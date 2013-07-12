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




package main.java.com.appdynamics.monitors.processes.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import main.java.com.appdynamics.monitors.processes.processdata.ProcessData;
import main.java.com.appdynamics.monitors.processes.processexception.ProcessMonitorException;

import org.apache.log4j.Logger;


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
		try{
			String line;
			Process p = Runtime.getRuntime().exec("ps aux");
			BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
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
				input.close();
				throw new ProcessMonitorException("Can't find correct process stats from 'ps aux' command. Terminating Process Monitor");
	
			}
			input.close();


			p = Runtime.getRuntime().exec("cat /proc/meminfo");
			input = new BufferedReader(new InputStreamReader(p.getInputStream()));
			if((line = input.readLine()) != null){
				String[] words = line.split("\\s+");
				setTotalMemSizeMB(Integer.parseInt(words[1]) / 1024);
			}		
			input.close();
		} catch (IOException e) {
			throw new ProcessMonitorException("Unable to read output from ps aux command");
		} catch (NumberFormatException e){
			throw new ProcessMonitorException("Unable to retrieve total physical memory size (not a number: " + e.getMessage() + "). Terminating Process Monitor");
		}
	}

	public String getNameOfProcess(int pid) throws IOException{
		String line;
		Process allProcs = Runtime.getRuntime().exec("cat /proc/" + pid + "/status");
		BufferedReader input = new BufferedReader(new InputStreamReader(allProcs.getInputStream()));
		if((line = input.readLine()) != null){
			String[] words = line.split("\\s+");
			input.close();
			return words[1];
		}
		input.close();
		return null;
	}

	/**
	 * Parsing the 'ps aux' command and gathering process information data.
	 * @throws NumberFormatException
	 */
	public void parseProcesses() throws NumberFormatException{
		try{
			String processLine;
			Process allProcs = Runtime.getRuntime().exec("ps aux");
			BufferedReader input = new BufferedReader(new InputStreamReader(allProcs.getInputStream()));

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
				String procName = getNameOfProcess(pid);
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
			input.close();
			
		} catch (IOException e) {
			logger.error("Unable to read output from ps aux command");
		}
	}

}
