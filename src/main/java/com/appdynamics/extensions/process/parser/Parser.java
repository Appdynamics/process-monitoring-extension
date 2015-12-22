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

import com.appdynamics.extensions.process.ProcessMonitor;
import com.appdynamics.extensions.process.common.CommandExecutor;
import com.appdynamics.extensions.process.common.CommandExecutorException;
import com.appdynamics.extensions.process.config.Configuration;
import com.appdynamics.extensions.process.processdata.ProcessData;
import com.appdynamics.extensions.process.processexception.ProcessMonitorException;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class Parser {

    private final int DEFAULT_MEM_THRESHOLD = 100;
    public final BigDecimal BYTES_CONVERSION_FACTOR = new BigDecimal(1024);
    protected Set<String> includeProcesses = new HashSet<String>();
    protected Map<String, ProcessData> processes = new HashMap<String, ProcessData>();
    private BigDecimal totalMemSizeMB;
    private static final Logger logger = Logger.getLogger(Parser.class);
    public static final String METRIC_SEPARATOR = "|";

    public String processGroupName;
    protected Configuration config;
    private String monitoredProcessFilePath;
    public CommandExecutor commandExecutor;

    public Parser(Configuration config) {
        this.config = config;
        this.monitoredProcessFilePath = ProcessMonitor.getConfigFilename(this.config.getMonitoredProcessFilePath());
        this.commandExecutor = new CommandExecutor();
    }

    public abstract void parseProcesses() throws ProcessMonitorException, CommandExecutorException;

    public void readProcsFromFile() {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(monitoredProcessFilePath));
            String line;
            while ((line = br.readLine()) != null) {
                includeProcesses.add(line);
            }
        } catch (FileNotFoundException e) {
            logger.warn("the file .monitoredProcesses.txt could not be found. " + "This might be the first time trying to read in from the file, "
                    + "and the set of monitored processes is set to be empty.");
        } catch (IOException e) {
            logger.warn("A problem occurred reading from the .monitoredProcesses file.");
        } finally {
            if (br != null)
                closeBufferedReader(br);
        }
    }

    public static BigDecimal toBigDecimal(String valueStr) {
        if (!Strings.isNullOrEmpty(valueStr.trim())) {
            try {
                return new BigDecimal(valueStr.trim());
            } catch (NumberFormatException e) {
                logger.warn("Cannot convert the value " + valueStr + " to string ");
            }
        }
        return null;
    }

    public void writeProcsToFile() {
        BufferedWriter wr = null;
        try {
            wr = new BufferedWriter(new FileWriter(monitoredProcessFilePath));
            wr.write("");
            wr.close();

            wr = new BufferedWriter(new FileWriter(monitoredProcessFilePath));
            for (String process : includeProcesses) {
                wr.write(process);
                wr.newLine();
            }
        } catch (IOException e) {
            logger.warn("Can't write process names to/create file '.monitored-processes' in ProcessMonitor directory.", e);
        } catch (Exception e) {
            logger.error("Exception: ", e);
        } finally {
            closeBufferedWriter(wr);
        }
    }

    public int getMemoryThreshold() {
        if (config.getMemoryThreshold() == 0) {
            return DEFAULT_MEM_THRESHOLD;
        } else {
            return config.getMemoryThreshold();
        }
    }

    protected void populateProcessData(String processName, int pid, BigDecimal cpuUtilizationInPercent, BigDecimal memUtilizationInPercent, BigDecimal absoluteMemUsed) {
        // check if user wants to exclude this process
        if (processName != null) {
            if(checkFromConfigIfProcessNeedsToBeReported(processName)) {
                // update the processes Map
                StringBuilder sb = new StringBuilder(processName);
                if (config.isDisplayByPid()) {
                    processName = sb.append(METRIC_SEPARATOR).append(pid).toString();
                } else {
                    processName = sb.toString();
                }
                if (processes.containsKey(processName)) {
                    ProcessData procData = processes.get(processName);
                    procData.numOfInstances++;
                    procData.CPUPercent = procData.CPUPercent.add(cpuUtilizationInPercent);
                    procData.memPercent = procData.memPercent.add(memUtilizationInPercent);
                    procData.absoluteMem = procData.absoluteMem.add(absoluteMemUsed);
                } else {
                    processes.put(processName, new ProcessData(processName, cpuUtilizationInPercent, memUtilizationInPercent, absoluteMemUsed));
                }
            }
        } else {
            logger.warn("Could not retrieve the name of Process with pid " + pid);
        }
    }

    public boolean checkFromConfigIfProcessNeedsToBeReported(String processName) {
        boolean processNeedsToBeReported = false;
        Set<String> includeProcessesSet = config.getIncludeProcesses();
        if(includeProcessesSet.isEmpty()) {
            if (!config.getExcludeProcesses().contains(processName)) {
                processNeedsToBeReported = true;
            }
        } else {
            processNeedsToBeReported = includeProcessesSet.contains(processName) ? true : false;
        }
        return processNeedsToBeReported;
    }

    public Map<String, ProcessData> getProcesses() {
        return processes;
    }

    public void setProcesses(Map<String, ProcessData> processes) {
        this.processes = processes;
    }

    public BigDecimal getTotalMemSizeMB() {
        return totalMemSizeMB;
    }

    public void setTotalMemSizeMB(BigDecimal totalMemSizeMB) {
        this.totalMemSizeMB = totalMemSizeMB;
    }

    public Set<String> getIncludeProcesses() {
        return includeProcesses;
    }

    public void setIncludeProcesses(Set<String> includeProcesses) {
        this.includeProcesses = includeProcesses;
    }

    public void addIncludeProcesses(String name) {
        if (this.includeProcesses.add(name)) {
            logger.debug("New Process added to the list of metrics to be permanently" + "reported, even if falling back below threshold: " + name);
        }
    }

    protected void skipParsingLines(BufferedReader input, int count) throws IOException {
        if (input != null) {
            for (int i = 0; i < count; i++) {
                input.readLine();
            }
        }
    }


    protected void cleanUpProcess(Process p, String cmd) {
        try {
            if (p != null) {
                int exitValue = p.waitFor();
                if (exitValue != 0) {
                    logger.warn("Unable to terminate the command " + cmd + " normally. ExitValue = " + exitValue);
                }
                p.destroy();
            }
        } catch (InterruptedException e) {
            logger.error("Execution of command " + cmd + " got interrupted ", e);
        }
    }

    protected void closeBufferedReader(BufferedReader reader) {
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException e) {
                logger.error("Exception while closing the reader: ", e);
            }
        }
    }

    protected void closeBufferedWriter(BufferedWriter writer) {
        if (writer != null) {
            try {
                writer.close();
            } catch (IOException e) {
                logger.error("Exception while closing the writer: ", e);
            }
        }
    }

    protected Map<String, Integer> processHeaderLine(String command, String headerLine, String [] headerStrings, String delimiter) throws ProcessMonitorException {
        List<String> headersList = Arrays.asList(headerLine.trim().split(delimiter));
        Map<String, Integer> headerInfo = Maps.newHashMap();
        for(String headerString : headerStrings) {
            if(headersList.contains(headerString)) {
                headerInfo.put(headerString, headersList.indexOf(headerString));
            } else {
                logger.error("Could not find correct header information for " + headerString + " while executing command " + command);
                throw new ProcessMonitorException("Could not find correct header information for " + headerString + " while executing command " + command);
            }
        }
        return headerInfo;
    }
}
