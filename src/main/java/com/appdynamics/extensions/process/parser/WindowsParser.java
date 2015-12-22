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

import com.appdynamics.extensions.process.common.CmdOutHeaderConstants;
import com.appdynamics.extensions.process.common.CommandExecutorException;
import com.appdynamics.extensions.process.common.ProcessCommands;
import com.appdynamics.extensions.process.config.Configuration;
import com.appdynamics.extensions.process.processdata.ProcessData;
import com.appdynamics.extensions.process.processexception.ProcessMonitorException;
import com.google.common.collect.Maps;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class WindowsParser extends Parser {

    private static final Logger logger = Logger.getLogger(WindowsParser.class);
    private int reportIntervalSecs = 60;

    // for keeping track of the CPU load
    private Map<String, Long> oldDeltaCPUTime;
    private Map<String, Long> newDeltaCPUTime;

    public WindowsParser(Configuration config) {
        super(config);
        processGroupName = "Windows Processes";
        processes = new HashMap<String, ProcessData>();
        includeProcesses = new HashSet<String>();
        oldDeltaCPUTime = new HashMap<String, Long>();
        newDeltaCPUTime = new HashMap<String, Long>();
    }


    public void retrieveMemoryMetrics() throws ProcessMonitorException, CommandExecutorException {
        String cmd = ProcessCommands.WINDOWS_MEMORY_COMMAND;
        Process p = commandExecutor.execute(cmd);
        BufferedReader input = null;
        try {
            closeOutputStream(p);
            input = new BufferedReader(new InputStreamReader(p.getInputStream()));
            // skipping two lines
            skipParsingLines(input, 2);
            String line = input.readLine();
            setTotalMemSizeMB(toBigDecimal(line.trim()).divide(new BigDecimal(1024)));
        } catch (IOException e) {
            logger.error("Error in parsing the output of command " + cmd, e);
            throw new ProcessMonitorException("Error in parsing the output of command " + cmd, e);
        } catch (NumberFormatException e) {
            logger.error("Unable to retrieve total physical memory size (not a number) ", e);
            throw new ProcessMonitorException("Unable to retrieve total physical memory size (not a number) ", e);
        } catch(Exception e) {
            logger.error(e);
        } finally {
            closeBufferedReader(input);
            cleanUpProcess(p, cmd);
        }
    }

    /**
     * Parsing the 'tasklist' command and storing process level data
     *
     * @throws ProcessMonitorException
     */
    @Override
    public void parseProcesses() throws ProcessMonitorException, CommandExecutorException {
        this.retrieveMemoryMetrics();
        String cmd = ProcessCommands.WINDOWS_PROCESS_LIST_COMMAND;
        Process p = commandExecutor.execute(cmd);
        BufferedReader input = null;
        try {
            closeOutputStream(p);
            input = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String [] headerArray = {CmdOutHeaderConstants.WIN_PROC_NAME, CmdOutHeaderConstants.WIN_PID, CmdOutHeaderConstants.WIN_MEM};
            String headerLine = input.readLine().replaceAll("\"", "").trim();
            Map<String, Integer> headerPositions = processHeaderLine(cmd, headerLine, headerArray, ",");
            String line;
            while ((line = input.readLine()) != null) {
                String[] words = line.split("\",\"");
                words[0] = words[0].replaceAll("\"", "");
                words[words.length - 1] = words[words.length - 1].replaceAll("\"", "");

                String processName = words[headerPositions.get(CmdOutHeaderConstants.WIN_PROC_NAME)];
                if (processName != null) {
                    if(checkFromConfigIfProcessNeedsToBeReported(processName)) {
                        BigDecimal absoluteMem = toBigDecimal(words[headerPositions.get(CmdOutHeaderConstants.WIN_MEM)].replaceAll("\\D*", "")).divide(new BigDecimal(1024));
                        BigDecimal memPercent = (absoluteMem.divide(getTotalMemSizeMB(), BigDecimal.ROUND_HALF_UP)).multiply(new BigDecimal(100));
                        int pid = Integer.parseInt(words[headerPositions.get(CmdOutHeaderConstants.WIN_PID)]);
                        StringBuilder sb = new StringBuilder(processName);
                        if (config.isDisplayByPid()) {
                            processName = sb.append(METRIC_SEPARATOR).append(pid).toString();
                        } else {
                            processName = sb.toString();
                        }
                        if (processes.containsKey(processName)) {
                            ProcessData procData = processes.get(processName);
                            procData.numOfInstances++;
                            procData.memPercent = procData.memPercent.add(memPercent);
                            procData.absoluteMem = procData.absoluteMem.add(absoluteMem);
                        } else {
                            processes.put(processName, new ProcessData(processName, BigDecimal.ZERO, memPercent, absoluteMem));
                        }
                    }
                }
            }
            calcCPUTime();
        } catch (IOException e) {
            logger.error("Error in parsing the output of command " + cmd, e);
            throw new ProcessMonitorException("Error in parsing the output of command " + cmd, e);
        } catch (Exception e) {
            logger.error("Exception: " + e);
            throw new ProcessMonitorException("Exception: " + e);
        } finally {
            closeBufferedReader(input);
            cleanUpProcess(p, cmd);
        }
    }

    /**
     * calculates the cpu utilization in % for each process and updates the
     * 'processes' hashmap
     *
     * @throws ProcessMonitorException
     */
    private void calcCPUTime() throws ProcessMonitorException {
        Runtime rt = Runtime.getRuntime();
        Process p = null;

        String cmd = getCommand();
        BufferedReader input = null;
        try {
            p = rt.exec(cmd);
            closeOutputStream(p);
            input = new BufferedReader(new InputStreamReader(p.getInputStream()));
            if (input.readLine() == null) {
                closeBufferedReader(input);
                input = new BufferedReader(new InputStreamReader(p.getErrorStream()));
                String errorString = input.readLine();
                if (errorString.toLowerCase().contains("invalid xsl format")) {
                    StringBuilder msg = new StringBuilder(errorString).append(" ");

                    if (Configuration.DEFAULT_CSV_FILE_PATH.equals(config.getCsvFilePath())) {
                        msg.append("csv.xls not found in C:\\Windows\\System32 or C:\\Windows\\SysWOW64 respectively.");
                    } else {
                        msg.append(config.getCsvFilePath()).append(" not found.");
                    }

                    msg.append(" Cannot process information for CPU usage (value 0 will be reported).");
                    logger.warn(msg.toString());
                    return;
                }
            }

            String cpudata;
            String header = input.readLine();

            // sometimes the first line is empty, so need to cater for this
            while (header != null && header.trim().equals("")) {
                if (logger.isDebugEnabled()) {
                    logger.debug("header is empty, checking the next line...");
                }
                header = input.readLine();
            }

            Map<String, Integer> headerPositions = Maps.newHashMap();
            if (header != null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("header found!");
                }
                String [] headerArray = {CmdOutHeaderConstants.WIN_CPU_PROC_NAME, CmdOutHeaderConstants.WIN_USER_MODETIME, CmdOutHeaderConstants.WIN_KERNEL_MODETIME, CmdOutHeaderConstants.WIN_CPU_PROC_ID};
                headerPositions = processHeaderLine(cmd, header.trim(), headerArray, ",");
            }

            while ((cpudata = input.readLine()) != null) {
                String[] words = cpudata.trim().split(",");
                if (words.length < 5) {
                    continue;
                }
                // retrieve single process information
                String procName = words[headerPositions.get(CmdOutHeaderConstants.WIN_CPU_PROC_NAME)];
                // divide by 10000 to convert to milliseconds
                long userModeTime = Long.parseLong(words[headerPositions.get(CmdOutHeaderConstants.WIN_USER_MODETIME)]) / 10000;
                long kernelModeTime = Long.parseLong(words[headerPositions.get(CmdOutHeaderConstants.WIN_KERNEL_MODETIME)]) / 10000;
                int pid = Integer.parseInt(words[headerPositions.get(CmdOutHeaderConstants.WIN_CPU_PROC_ID)]);

                StringBuilder sb = new StringBuilder(procName);
                if (config.isDisplayByPid()) {
                    procName = sb.append(METRIC_SEPARATOR).append(pid).toString();
                } else {
                    procName = sb.toString();
                }

                // update hashmaps used for CPU load calculations
                if (processes.containsKey(procName)) {
                    if (newDeltaCPUTime.containsKey(procName)) {
                        newDeltaCPUTime.put(procName, newDeltaCPUTime.get(procName) + userModeTime + kernelModeTime);
                    } else {
                        newDeltaCPUTime.put(procName, userModeTime + kernelModeTime);
                    }
                }
            }
            // update CPU data in processes hash-map
            for (String key : newDeltaCPUTime.keySet()) {
                if (oldDeltaCPUTime.containsKey(key)) {
                    // calculations involving the period and interval
                    float delta = newDeltaCPUTime.get(key) - oldDeltaCPUTime.get(key);
                    float time = reportIntervalSecs * 1000;
                    ProcessData procData = processes.get(key);
                    if (procData != null) {
                        procData.CPUPercent = procData.CPUPercent.add((new BigDecimal(delta).divide(new BigDecimal(time))).multiply(new BigDecimal(100)));
                    }
                }
            }
            oldDeltaCPUTime = newDeltaCPUTime;
            newDeltaCPUTime = new HashMap<String, Long>();
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

    private void closeOutputStream(Process p) {
        // hack to work in win 2003, wmic hangs everytime
        // fetching and closing the output stream before reading the input stream
        try {
            p.getOutputStream().close();
        } catch (IOException e) {
            logger.info("Exception while closing the output stream: ", e);
        }
    }

    private String getCommand() {
        StringBuilder cmdBuilder = new StringBuilder(ProcessCommands.WINDOWS_CPU_COMMAD);

        if (Configuration.DEFAULT_CSV_FILE_PATH.equals(config.getCsvFilePath())) {
            cmdBuilder.append(config.getCsvFilePath());
        } else {
            cmdBuilder.append("\"").append(config.getCsvFilePath()).append("\"");
        }

        return cmdBuilder.toString();
    }
}
