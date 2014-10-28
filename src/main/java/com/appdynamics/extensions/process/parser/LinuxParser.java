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
import java.util.HashMap;
import java.util.HashSet;

import org.apache.log4j.Logger;

import com.appdynamics.extensions.process.config.Configuration;
import com.appdynamics.extensions.process.parser.Parser.ErrorHandler;
import com.appdynamics.extensions.process.processdata.ProcessData;
import com.appdynamics.extensions.process.processexception.ProcessMonitorException;

public class LinuxParser extends Parser {

	private static final String MEMORY_COMMAND = "cat /proc/meminfo";
	private int posPID = -1, posCPU = -1, posMem = -1; // used for parsing
	private static final Logger logger = Logger.getLogger("com.singularity.extensions.LinuxParser");

	public LinuxParser(Configuration config) {
		super(config);
		processGroupName = "Linux Processes";
		processes = new HashMap<String, ProcessData>();
		includeProcesses = new HashSet<String>();
	}

	public void retrieveMemoryMetrics() throws ProcessMonitorException {
		Runtime rt = Runtime.getRuntime();
		Process p = null;
		BufferedReader input = null;
		try {
			p = rt.exec(MEMORY_COMMAND);
			//handleErrorsIfAny(p.getErrorStream());
			
			input = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line;
			if ((line = input.readLine()) != null) {
				String[] words = line.split("\\s+");
				setTotalMemSizeMB(Integer.parseInt(words[1]) / 1024);
			}
		} catch (IOException e) {
			logger.error("Error in executing the command " + MEMORY_COMMAND, e);
			throw new ProcessMonitorException("Error in executing the command " + MEMORY_COMMAND, e);
		} catch (NumberFormatException e) {
			logger.error("Unable to retrieve total physical memory size (not a number) ", e);
			throw new ProcessMonitorException("Unable to retrieve total physical memory size (not a number) ", e);
		} finally {
			closeBufferedReader(input);
			cleanUpProcess(p, MEMORY_COMMAND);
		}
	}

	public String getNameOfProcess(int pid) {
		Runtime rt = Runtime.getRuntime();
		Process p = null;
		String cmd = "cat /proc/" + pid + "/status";
		BufferedReader input = null;
		try {
			p = rt.exec(cmd);
			//handleErrorsIfAny(p.getErrorStream());
			input = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line;
			if ((line = input.readLine()) != null) {
				String[] words = line.split("\\s+");
				return words[1];
			}
		} catch (IOException e) {
			logger.error("Error in executing the command " + cmd, e);
		} catch (Exception e) {
			logger.error(e);
		} finally {
			closeBufferedReader(input);
			cleanUpProcess(p, cmd);
		}
		return null;
	}

	/**
	 * Parsing the 'ps aux' command and gathering process information data.
	 * 
	 * @throws NumberFormatException
	 * @throws ProcessMonitorException
	 */
	public void parseProcesses() throws NumberFormatException, ProcessMonitorException {
		Runtime rt = Runtime.getRuntime();
		Process p = null;
		String cmd = "ps aux";
		BufferedReader input = null;
		try {
			p = rt.exec(cmd);
			//handleErrorsIfAny(p.getErrorStream());
			input = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line;
			// there seems to be a single process, probably ps aux itself,
			// for which information can't be retrieved after it is terminated.
			// this is used for logging errors on retrieving single process
			// information
			boolean hasReadOwn = false;
			int i = 0;
			while ((line = input.readLine()) != null) {
				if (i == 0) {
					processHeader(line);
				} else {
					String[] words = line.split("\\s+");
					// retrieve single process information
					int pid = Integer.parseInt(words[posPID]);

					String procName = null;
					String processName = getNameOfProcess(pid);
					if (processName != null) {
						StringBuilder sb = new StringBuilder(processName);
						procName = sb.append("|PID|").append(pid).toString();
					}
					float cpu = Float.parseFloat(words[posCPU]);
					float mem = Float.parseFloat(words[posMem]);

					if (procName != null) {
						// check if user wants to exclude this process
						if (!config.getExcludeProcesses().contains(procName) && !config.getExcludePIDs().contains(pid)) {
							// update the processes Map
							if (processes.containsKey(procName)) {
								ProcessData procData = processes.get(procName);
								procData.numOfInstances++;
								procData.CPUPercent += cpu;
								procData.memPercent += mem;
							} else {
								processes.put(procName, new ProcessData(procName, cpu, mem));
							}
						}
					} else {
						// see description for hasReadOwn above
						if (hasReadOwn) {
							logger.warn("Could not retrieve the name of Process with pid " + pid);
						}
						hasReadOwn = true;
					}
				}
				i++;
			}
		} catch (IOException e) {
			logger.error("Error in executing the command " + cmd, e);
			throw new ProcessMonitorException("Error in executing the command " + cmd, e);
		} catch (Exception e) {
			logger.error("Exception: ", e);
			throw new ProcessMonitorException("Exception: ", e);
		} finally {
			closeBufferedReader(input);
			cleanUpProcess(p, cmd);
		}
	}

	private void processHeader(String processLine) throws ProcessMonitorException {
		String[] words = processLine.split("\\s+");
		for (int i = 0; i < words.length; i++) {
			if (words[i].equals("PID")) {
				posPID = i;
			} else if (words[i].equals("%CPU")) {
				posCPU = i;
			} else if (words[i].equals("%MEM")) {
				posMem = i;
			}
		}
		if (posMem == -1 || posCPU == -1 || posMem == -1) {
			throw new ProcessMonitorException("Can't find correct process stats from 'ps aux' command. Terminating Process Monitor");
		}
	}
}
