/*
 * Copyright 2020 AppDynamics LLC and its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.appdynamics.extensions.process.parser;

import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.appdynamics.extensions.process.common.MonitorConstants;
import com.appdynamics.extensions.process.configuration.ConfigProcessor;
import com.appdynamics.extensions.process.configuration.Instance;
import com.appdynamics.extensions.process.data.ProcessData;
import com.google.common.base.Joiner;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.hyperic.sigar.ProcCpu;
import org.hyperic.sigar.ProcMem;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.appdynamics.extensions.process.common.MonitorConstants.*;

public class WindowsParser extends Parser {

    public static final Logger logger = ExtensionsLoggerFactory.getLogger(WindowsParser.class);

    public Map<String, ProcessData> fetchMetrics(Map<String, ?> config) {
        logger.debug("WindowsParser:: In fetchMetrics method");
        List<Instance> instances = new ConfigProcessor().processConfig(config);
        // all process lines
        List<String> processListOutput = fetchProcessListFromSigar();
        // filter process lines based on configuration
        ListMultimap<String, String> filteredProcessLines = filterProcessLinesFromCompleteList(processListOutput, instances, buildHeaderInfo());
        // process the filtered lines and retrieve the data
        Map<String, ProcessData> processesData = populateProcessesData(instances, filteredProcessLines);
        return processesData;
    }

    public List<String> fetchProcessListFromSigar() {
        List<String> processLines = new ArrayList<String>();
        logger.debug("WindowsParser:: In fetchProcessListFromSigar method ");
        Sigar sigar = null;
        try {
            sigar = new Sigar();
            logger.debug("Fetching process list from Sigar");
            long[] pids = sigar.getProcList();
            for (long pid : pids) {
                String line = "";
                try {
                    String processName = sigar.getProcExe(pid).getName();
                    String processArgs = Joiner.on(" ").join(sigar.getProcArgs(pid));
                    line = pid + " " + processName + " " + processArgs;
                } catch (Exception e) {
                    logger.debug("Unable to retrieve process info for pid " + pid, e);
                }
                processLines.add(line);
            }
        } catch (Exception e) {
            logger.error("Exception while fetching metrics from Sigar", e);
        } finally {
            if (sigar != null) {
                sigar.close();
            }
        }
        return processLines;
    }

    public Map<String, ProcessData> populateProcessesData(List<Instance> instances, ListMultimap<String, String> filteredProcessLines) {
        Map<String, ProcessData> processesData = Maps.newHashMap();
        for (Instance instance : instances) {
            ProcessData processData = new ProcessData();
            Map<String, String> processMetrics = Maps.newHashMap();
            List<String> processLines = filteredProcessLines.get(instance.getDisplayName());
            if (processLines.size() == 1) {
                String pid = processLines.get(0).trim().split(MonitorConstants.SPACES)[0];
                Double cpuPercent = getProcCPU(pid);
                Long residentMem = getProcMem(pid);
                if (cpuPercent != null) {
                    processMetrics.put(CPU_PERCENT, String.valueOf(cpuPercent));
                }
                if (residentMem != null) {
                    processMetrics.put(RSS, String.valueOf(residentMem));
                }
            } else if (processLines.size() > 1) {
                Double cpuPercent = 0.0;
                Double processCpuPercent;
                Long residentMem = 0L;
                Long processResidentMem;
                for (String processLine : processLines) {
                    String pid = processLine.trim().split(MonitorConstants.SPACES)[0];
                    processCpuPercent = getProcCPU(pid);
                    if (processCpuPercent != null) {
                        cpuPercent += getProcCPU(pid);
                    }
                    processResidentMem = getProcMem(pid);
                    if (processResidentMem != null) {
                        residentMem += getProcMem(pid);
                    }
                }
                processMetrics.put(CPU_PERCENT, String.valueOf(cpuPercent));
                processMetrics.put(RSS, String.valueOf(residentMem));
            }
            processMetrics.put(MonitorConstants.RUNNING_INSTANCES_COUNT, String.valueOf(processLines.size()));
            processData.setProcessMetrics(processMetrics);
            processesData.put(instance.getDisplayName(), processData);
        }
        return processesData;
    }

    protected Double getProcCPU(String pid) {
        Sigar sigar = null;
        try {
            sigar = new Sigar();
            ProcCpu procCpu = sigar.getProcCpu(pid);
            Double cpuPercent = procCpu.getPercent();
            logger.debug("CPU% returned from Sigar for {} is {}", pid, cpuPercent);
            return cpuPercent;
        } catch (SigarException e) {
            logger.error("Error while fetching cpu% for process " + pid, e);
        } finally {
            if (sigar != null) {
                sigar.close();
            }
        }
        return null;
    }

    protected Long getProcMem(String pid) {
        Sigar sigar = null;
        try {
            sigar = new Sigar();
            ProcMem procMem = sigar.getProcMem(pid);
            return procMem.getResident();
        } catch (SigarException e) {
            logger.error("Error while fetching mem for process " + pid, e);
        } finally {
            if (sigar != null) {
                sigar.close();
            }
        }
        return null;
    }

    private List<String> buildHeaderInfo() {
        List<String> headerInfo = Lists.newArrayList();
        headerInfo.add(MonitorConstants.PID);
        headerInfo.add(MonitorConstants.COMMAND);
        return headerInfo;
    }

    public String getProcessGroupName() {
        return WINDOWS_PROCESSES;
    }

    protected Map<String, String> getCommands(Map<String, ?> config) {
        return null;
    }
}
