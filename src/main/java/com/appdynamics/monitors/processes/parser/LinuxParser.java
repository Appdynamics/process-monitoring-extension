/**
 * Copyright 2013 AppDynamics
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.appdynamics.monitors.processes.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.log4j.Logger;

import com.appdynamics.monitors.processes.processdata.ProcessData;
import com.appdynamics.monitors.processes.processexception.ProcessMonitorException;

public class LinuxParser extends Parser {

	private int posPID = -1, posCPU = -1, posMem = -1; // used for parsing 

	public LinuxParser(Logger logger){
		super(logger);
		processGroupName = "Linux Processes";
		processes = new HashMap<String, ProcessData>();
		includeProcesses = new HashSet<String>();
		excludeProcesses = new ArrayList<String>();
		excludePIDs = new ArrayList<Integer>();
	}

	/**
	 * determines the position of the PID, %CPU, %MEM in the 'ps aux' output
	 * @throws ProcessMonitorException 
	 */
	public void initialize() throws ProcessMonitorException{		
		BufferedReader input = null;
        Process p = null;
        try{
			String line;
			p = Runtime.getRuntime().exec("ps aux");
			input = new BufferedReader(new InputStreamReader(p.getInputStream()));
			//first line, parse positions of data (in case this will change over time)
			if((line = input.readLine()) != null){
				String[] words = line.split("\\s+");
				for(int i = 0; i < words.length; i++){
					if(words[i].equals("PID")){
						posPID = i;
					} else if(words[i].equals("%CPU")){
						posCPU = i;
					} else if(words[i].equals("%MEM")){
						posMem = i;
					}
				}
			}

			if(posMem == -1 || posCPU == -1 || posMem == -1){
				p.destroy();
                input.close();
				throw new ProcessMonitorException("Can't find correct process stats from 'ps aux' command. Terminating Process Monitor");
	
			}
            cleanUpProcess(p);
            input.close();

			p = Runtime.getRuntime().exec("cat /proc/meminfo");
			input = new BufferedReader(new InputStreamReader(p.getInputStream()));
			if((line = input.readLine()) != null){
				String[] words = line.split("\\s+");
				setTotalMemSizeMB(Integer.parseInt(words[1]) / 1024);
			}		
		} catch (IOException e) {
			throw new ProcessMonitorException("Unable to read output from ps aux command");
		} catch (NumberFormatException e){
			throw new ProcessMonitorException("Unable to retrieve total physical memory size (not a number: " + e.getMessage() + "). Terminating Process Monitor");
		}  catch(NullPointerException e) {
            throw new NullPointerException("NullPointerException " + e.getMessage());
        }
        finally {
            cleanUpProcess(p);
            closeBufferedReader(input);
        }
	}

	public String getNameOfProcess(int pid) {
		BufferedReader input = null;
        Process allProcs = null;
        try {
            String line;
            allProcs = Runtime.getRuntime().exec("cat /proc/" + pid + "/status");
            input = new BufferedReader(new InputStreamReader(allProcs.getInputStream()));
            if((line = input.readLine()) != null){
                String[] words = line.split("\\s+");
                return words[1];
            }
        }
        catch(IOException e) {
            logger.error("IOException: ", e);
        } catch(NullPointerException e) {
            logger.error("NullPointerException: ", e);
        }
        finally {
            cleanUpProcess(allProcs);
            closeBufferedReader(input);
        }
		return null;
	}

	/**
	 * Parsing the 'ps aux' command and gathering process information data.
	 * @throws NumberFormatException
	 */
	public void parseProcesses() throws NumberFormatException{
        BufferedReader input = null;
        Process allProcs = null;
        try{
			String processLine;
			allProcs = Runtime.getRuntime().exec("ps aux");
			input = new BufferedReader(new InputStreamReader(allProcs.getInputStream()));

			// there seems to be a single process, probably ps aux itself,
			// for which information can't be retrieved after it is terminated.
			// this is used for logging errors on retrieving single process information
			boolean hasReadOwn = false;

			// skip header line
			input.readLine();

			// parse all remaining lines
			while ((processLine = input.readLine()) != null) {
				String[] words = processLine.split("\\s+");

				// retrieve single process information
				int pid = Integer.parseInt(words[posPID]);

                //logger.info("Process name: " + new StringBuilder(getNameOfProcess(pid)).append(" (PID: ").append(pid).append(")").toString());
                String procName = null;
                if (getNameOfProcess(pid) != null) {
                    StringBuilder sb = new StringBuilder(getNameOfProcess(pid));
                    procName = sb.append("|PID|").append(pid).toString();
                }
                float cpu = Float.parseFloat(words[posCPU]);
				float mem = Float.parseFloat(words[posMem]);

				if(procName != null){
					// check if user wants to exclude this process
					if(!excludeProcesses.contains(procName) && !excludePIDs.contains(pid)){
						// update the processes Map
						if(processes.containsKey(procName)){
							ProcessData procData = processes.get(procName);
							procData.numOfInstances++;
							procData.CPUPercent += cpu;
							procData.memPercent += mem;
						} else {
							processes.put(procName, new ProcessData(procName, cpu, mem));
						}
					}
				} else {
					if(hasReadOwn){ // see description for hasReadOwn above
						logger.warn("Could not retrieve the name of Process with pid " + pid);
					}
					hasReadOwn = true;
				}
			}
		} catch (IOException e) {
			logger.error("Unable to read output from ps aux command", e);
		} catch (NullPointerException e) {
            logger.error("NullPointerException: ", e);
        }
        finally {
            cleanUpProcess(allProcs);
            closeBufferedReader(input);
        }
	}
}
