/**
 * Copyright 2015 AppDynamics
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

import com.appdynamics.extensions.process.common.CommandExecutorException;
import com.appdynamics.extensions.process.common.ProcessCommands;
import com.appdynamics.extensions.process.config.Configuration;
import com.appdynamics.extensions.process.processdata.ProcessData;
import com.appdynamics.extensions.process.processexception.ProcessMonitorException;
import com.google.common.base.Strings;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;


public class SolarisParser extends Parser {
    private int posPID = -1, posCPU = -1, posMem = -1, posProcName = -1;// used for parsing
    private static final Logger logger = Logger.getLogger(SolarisParser.class);

    public SolarisParser(Configuration config) {
        super(config);
        processGroupName = "Solaris Processes";
        processes = new HashMap<String, ProcessData>();
        includeProcesses = new HashSet<String>();
    }


    @Override
    public void parseProcesses() throws ProcessMonitorException, CommandExecutorException {
        String cmd = ProcessCommands.SOLARIS_PROCESS_LIST_COMMAND;
        Process p = commandExecutor.execute(cmd);
        BufferedReader input = null;
        try {
            input = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;

            for (int i = 0; i < 4; i++) {
                input.readLine();
            }
            processMemoryLine(input.readLine());
            input.readLine();
            processHeader(input.readLine());
            //logger.info("posPID: " + posPID);
            while (!Strings.isNullOrEmpty(input.readLine())) {
                line = input.readLine();
                String[] words = line.trim().split("\\s+");
                //logger.info(words[posPID]);
                //logger.info(line);
                // retrieve single process information
                int pid = Integer.parseInt(words[posPID].trim());

                String processName = words[posProcName].trim();

                BigDecimal cpu = toBigDecimal(words[posCPU].split("%")[0].trim());
                String memString = words[posMem].trim();
                BigDecimal mem = BigDecimal.ZERO;
                if (memString.endsWith("M")) {
                    mem = toBigDecimal(memString.split("M")[0].trim());
                } else if (memString.endsWith("K")) {
                    mem = toBigDecimal(memString.split("K")[0].trim()).divide(new BigDecimal(1024));
                }
                BigDecimal memPercent = (mem.divide(getTotalMemSizeMB(), BigDecimal.ROUND_HALF_UP)).multiply(new BigDecimal(100));

                if (processName != null) {
                    String procName = "";
                    StringBuilder sb = new StringBuilder(processName);
                    if (config.isDisplayByPid()) {
                        procName = sb.append(METRIC_SEPARATOR).append(pid).toString();
                    } else {
                        procName = sb.toString();
                    }
                    // check if user wants to exclude this process
                    if (!config.getExcludeProcesses().contains(procName) && !config.getExcludePIDs().contains(pid)) {
                        // update the processes Map
                        if (processes.containsKey(procName)) {
                            //logger.info("Process Name" + procName);
                            ProcessData procData = processes.get(procName);
                            procData.numOfInstances++;
                            procData.CPUPercent.add(cpu);
                            procData.memPercent.add(memPercent);
                            procData.absoluteMem.add(mem);
                        } else {
                            processes.put(procName, new ProcessData(procName, cpu, memPercent, mem));
                        }
                    }
                } else {
                    logger.warn("Could not retrieve the name of Process with pid " + pid);
                }
            }
        } catch (IOException e) {
            logger.error("Error in parsing the output of command " + cmd, e);
            throw new ProcessMonitorException("Error in parsing the output of command " + cmd, e);
        } catch (Exception e) {
            logger.error("Exception: ", e);
            throw new ProcessMonitorException("Exception: ", e);
        } finally {
            closeBufferedReader(input);
            cleanUpProcess(p, cmd);
        }
    }

    private void processMemoryLine(String line) throws ProcessMonitorException {
        String memoryToken = line.split(",")[0].trim().split(":")[1].trim().split("\\s+")[0].trim();
        if (memoryToken.endsWith("M")) {
            String memoryString = memoryToken.split("M")[0].trim();
            logger.info("Memory of Solaris: " + memoryString);
            setTotalMemSizeMB(new BigDecimal(unLocalizeStrValue(memoryString)));
        } else {
            logger.error("Couldn't parse " + line + " for memory retrieval");
            throw new ProcessMonitorException("Couldn't parse " + line + " for memory retrieval");
        }
    }

    private static Integer unLocalizeStrValue(String valueStr) {
        try {
            Locale loc = Locale.getDefault();
            return Integer.valueOf(NumberFormat.getInstance(loc).parse(valueStr).intValue());
        } catch (ParseException e) {
            logger.error("Exception while unlocalizing number string " + valueStr, e);
        }
        return null;
    }

    private void processHeader(String processLine) throws ProcessMonitorException {
        String[] words = processLine.trim().split("\\s+");
        for (int i = 0; i < words.length; i++) {
            if (words[i].equals("PID")) {
                posPID = i;
            } else if (words[i].equals("CPU")) {
                posCPU = i;
            } else if (words[i].equals("SIZE")) {
                posMem = i;
            } else if (words[i].equals("COMMAND")) {
                posProcName = i;
            }
        }
        if (posPID == -1 || posCPU == -1 || posMem == -1 || posProcName == -1) {
            throw new ProcessMonitorException("Can't find correct process stats from 'top' command. Terminating Process Monitor");
        }
    }
}
