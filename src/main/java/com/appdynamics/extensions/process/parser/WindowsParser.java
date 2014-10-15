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




package com.appdynamics.extensions.process.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.log4j.Logger;

import com.appdynamics.extensions.process.processdata.ProcessData;
import com.appdynamics.extensions.process.processexception.ProcessMonitorException;


public class WindowsParser extends Parser{

	private int posName = -1, posPID = -1, posMem = -1;
	private int report_interval_secs, fetches_per_interval;

	// for keeping track of the CPU load
	private Map<String, Long> oldDeltaCPUTime;
	private Map<String, Long> newDeltaCPUTime;

	public WindowsParser(Logger logger, int report_interval_secs, int fetches_per_interval){
		super(logger);
		processGroupName = "Windows Processes";
		processes = new HashMap<String, ProcessData>();
		includeProcesses = new HashSet<String>();
		excludeProcesses = new ArrayList<String>();
		excludePIDs = new ArrayList<Integer>();
		this.report_interval_secs = report_interval_secs;
		this.fetches_per_interval = fetches_per_interval;
		oldDeltaCPUTime = new HashMap<String, Long>();
		newDeltaCPUTime = new HashMap<String, Long>();
	}

	@Override
	public void initialize() throws ProcessMonitorException {
		BufferedReader input = null;
        Process p = null;
        try{
			String line;			
			p = Runtime.getRuntime().exec("tasklist /fo csv");
			input = new BufferedReader(new InputStreamReader(p.getInputStream()));
			//first line, parse positions of data (in case this will change over time)
			if((line = input.readLine()) != null){
				String[] words = line.replaceAll("\"", "").trim().split(",");

				for(int i = 0; i < words.length; i++){
					if(words[i].equals("Image Name")){
						posName = i;
					} else if(words[i].equals("PID")){
						posPID = i;
					} else if(words[i].equals("Mem Usage")){
						posMem = i;
					}
				}
			}
			
			cleanUpProcess(p);
            input.close();
			
			if(posName == -1 || posPID == -1 || posMem == -1){
				throw new ProcessMonitorException("Could not find correct header information of 'tasklist -fo csv'. Terminating Process Monitor");
			}

			p = Runtime.getRuntime().exec("wmic OS get TotalVisibleMemorySize");
			input = new BufferedReader(new InputStreamReader(p.getInputStream()));

			//skipping first lines
			input.readLine();
			input.readLine();
			// setting the total RAM size
			line = input.readLine();
			setTotalMemSizeMB(Integer.parseInt(line.trim()) / 1024);

			input.close();
		} catch(IOException e) {
			throw new ProcessMonitorException("Unable to read output from 'tasklist' or 'wmic' command. Terminating Process Monitor");
		} catch (NumberFormatException e){
			throw new ProcessMonitorException("Unable to retrieve total physical memory size (not a number: " + e.getMessage() +
					"). Terminating Process Monitor");
		} catch (NullPointerException e){
            throw new ProcessMonitorException("NullPointerException: " + e.getMessage());
		}
        finally {
            cleanUpProcess(p);
            closeBufferedReader(input);
        }
	}

	/**
	 * Parsing the 'tasklist' command and storing process level data
	 * @throws ProcessMonitorException 
	 */
	@Override
	public void parseProcesses() throws ProcessMonitorException {
		BufferedReader input = null;
        Process p = null;
        try{
			String processLine;			
			p = Runtime.getRuntime().exec("tasklist /fo csv");
			input = new BufferedReader(new InputStreamReader(p.getInputStream()));

			// skipping csv header
			input.readLine();

			while ((processLine = input.readLine()) != null) {
				String[] words = processLine.split("\",\"");
				words[0] = words[0].replaceAll("\"", "");
				words[words.length - 1] = words[words.length - 1].replaceAll("\"", "");

				// retrieve single process information
				int pid = Integer.parseInt(words[posPID]);
				String procName = words[posName];

				float memPercent = (Float.parseFloat(words[posMem].replaceAll("\\D*", "")) / 1024) / getTotalMemSizeMB() * 100;

				if(procName != null){
					// check if user wants to exclude this process
					if(!excludeProcesses.contains(procName) && !excludePIDs.contains(pid)){
						// update the processes Map
						if(processes.containsKey(procName)){
							ProcessData procData = processes.get(procName);
							procData.numOfInstances++;
							procData.memPercent += memPercent;
						} else {
							processes.put(procName, new ProcessData(procName, 0, memPercent));
						}
					}

				}
			}

			calcCPUTime();
		} catch (IOException e){
			throw new ProcessMonitorException("Unable to read output from 'tasklist' or 'wmic' command. Terminating Process Monitor");
		} catch (NumberFormatException e){
			throw new ProcessMonitorException("Unable to retrieve percentual memory consumption: " + e.getMessage() + 
					". This is not a number. Terminating Process Monitor");
		} catch (NullPointerException e){
            throw new ProcessMonitorException("NullPointerException: " + e.getMessage());
        }
        finally {
            cleanUpProcess(p);
            closeBufferedReader(input);
        }
	}

	/**
	 * calculates the cpu utilization in % for each process and updates the 'processes' hashmap
	 * @throws ProcessMonitorException 
	 */
	private void calcCPUTime() throws ProcessMonitorException{
		BufferedReader input = null;
        Process p = null;
        try{
			String cpudata;			
			int cpuPosName = -1, cpuPosUserModeTime = -1, cpuPosKernelModeTime = -1;
			p = Runtime.getRuntime().exec("wmic process get name,usermodetime,kernelmodetime /format:csv");
			input = new BufferedReader(new InputStreamReader(p.getInputStream()));

			//skipping first lines
			if(input.readLine().toLowerCase().contains("invalid xsl format")){
				logger.error("csv.xls not found. Cannot process information for CPU usage (value 0 will be repored)" +
						"Make sure csv.xsl is in C:\\Windows\\System32 or C:\\Windows\\SysWOW64 respectively.");
				return;
			}
			if(input.readLine().toLowerCase().contains("invalid xsl format")){
				logger.error("csv.xls not found. Cannot process information for CPU usage (value 0 will be repored)" +
						"Make sure csv.xsl is in C:\\Windows\\System32 or C:\\Windows\\SysWOW64 respectively.");
				return;
			}

			String header = input.readLine();
			if(header != null){
				String[] words = header.trim().split(",");
				for(int i = 0; i < words.length; i++){
					if(words[i].toLowerCase().equals("name")){
						cpuPosName = i;
					} else if(words[i].toLowerCase().equals("usermodetime")){
						cpuPosUserModeTime = i;
					} else if(words[i].toLowerCase().equals("kernelmodetime")){
						cpuPosKernelModeTime = i;
					}
				}
			}

			if(cpuPosName == -1 || cpuPosUserModeTime == -1 || cpuPosKernelModeTime == -1){
				input.close();
				throw new ProcessMonitorException("Could not find correct header information of 'wmic process get name," +
						"usermodetime,kernelmodetime /format:csv'. Terminating Process Monitor");
			}

			while ((cpudata = input.readLine()) != null) {
				String[] words = cpudata.trim().split(",");
				if(words.length < 4){
					continue;
				}

				// retrieve single process information
				String procName = words[cpuPosName];				
				long userModeTime = Long.parseLong(words[cpuPosUserModeTime]) / 10000; // divide by 10000 to convert to milliseconds
				long kernelModeTime = Long.parseLong(words[cpuPosKernelModeTime]) / 10000;

				//update hashmaps used for CPU load calculations
				if(processes.containsKey(procName)){
					if(newDeltaCPUTime.containsKey(procName)){
						newDeltaCPUTime.put(procName, newDeltaCPUTime.get(procName) + userModeTime + kernelModeTime);
					} else {
						newDeltaCPUTime.put(procName, userModeTime + kernelModeTime);
					}

				}

			}

			input.close();

			// update CPU data in processes hash-map
			for(String key : newDeltaCPUTime.keySet()){
				if(oldDeltaCPUTime.containsKey(key)){
					// calculations involving the period and interval
					float delta = newDeltaCPUTime.get(key) - oldDeltaCPUTime.get(key);
					float time = report_interval_secs / fetches_per_interval * 1000;
					ProcessData procData = processes.get(key);
					if(procData != null){
						procData.CPUPercent += delta/time * 100;
					}
				}
			}

			oldDeltaCPUTime = newDeltaCPUTime;
			newDeltaCPUTime = new HashMap<String, Long>();

		} catch(IOException e) {
			throw new ProcessMonitorException("Unable to read output from 'wmic' command. Terminating Process Monitor");
		} catch (NullPointerException e){
            throw new ProcessMonitorException("NullPointerException: " + e.getMessage());
        }
        finally {
           cleanUpProcess(p);
           closeBufferedReader(input);
        }
	}
}
