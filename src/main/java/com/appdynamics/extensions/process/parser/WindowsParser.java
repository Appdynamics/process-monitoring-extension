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
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import oshi.SystemInfo;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.appdynamics.extensions.process.common.MonitorConstants.*;

public class WindowsParser extends Parser {

    public static final Logger logger = ExtensionsLoggerFactory.getLogger(WindowsParser.class);

    // Previous OSProcess snapshots keyed by PID string; static so snapshots survive across
    // parser instances (a new WindowsParser is created on every poll cycle).
    private static final Map<String, OSProcess> previousSnapshots = new ConcurrentHashMap<>();

    // Current-cycle snapshots populated once by fetchProcessList(); reused by getProcCPU/getProcMem.
    private final Map<String, OSProcess> currentSnapshots = new java.util.HashMap<>();

    public Map<String, ProcessData> fetchMetrics(Map<String, ?> config) {
        List<Instance> instances = new ConfigProcessor().processConfig(config);
        List<String> processListOutput = fetchProcessList();
        List<String> headerInfo = buildHeaderInfo();
        ListMultimap<String, String> filteredProcessLines = filterProcessLinesFromCompleteList(processListOutput, instances, headerInfo);
        for (Instance instance : instances) {
            logger.debug("WindowsParser:: instance='{}' regex='{}' matched {} line(s)",
                    instance.getDisplayName(), instance.getRegex(),
                    filteredProcessLines.get(instance.getDisplayName()).size());
        }
        return populateProcessesData(instances, filteredProcessLines);
    }

    public List<String> fetchProcessList() {
        List<String> processLines = new ArrayList<>();
        try {
            OperatingSystem os = new SystemInfo().getOperatingSystem();
            List<OSProcess> processes = os.getProcesses();
            logger.debug("WindowsParser:: OSHI returned {} processes", processes.size());
            int skipped = 0;
            for (OSProcess proc : processes) {
                try {
                    String pid = String.valueOf(proc.getProcessID());
                    currentSnapshots.put(pid, proc);
                    String executablePath = proc.getPath();
                    String commandLine = proc.getCommandLine();
                    String name = (executablePath != null && !executablePath.isEmpty()) ? executablePath : proc.getName();
                    String args = (commandLine != null && !commandLine.isEmpty()) ? commandLine : "";
                    processLines.add(pid + " " + name + " " + args);
                } catch (Exception e) {
                    skipped++;
                    logger.debug("WindowsParser:: skipped pid={} due to exception: {}", proc.getProcessID(), e.toString());
                }
            }
            if (skipped > 0) {
                logger.debug("WindowsParser:: {} processes skipped due to errors", skipped);
            }
            previousSnapshots.keySet().retainAll(currentSnapshots.keySet());
        } catch (Exception e) {
            logger.error("WindowsParser:: exception while fetching process list via OSHI", e);
        }
        return processLines;
    }

    public Map<String, ProcessData> populateProcessesData(List<Instance> instances, ListMultimap<String, String> filteredProcessLines) {
        Map<String, ProcessData> processesData = Maps.newHashMap();
        for (Instance instance : instances) {
            ProcessData processData = new ProcessData();
            Map<String, String> processMetrics = Maps.newHashMap();
            List<String> processLines = filteredProcessLines.get(instance.getDisplayName());
            if (!processLines.isEmpty()) {
                Double cpuPercent = null;
                Long residentMem = null;
                for (String processLine : processLines) {
                    String pid = processLine.trim().split(MonitorConstants.SPACES)[0];
                    Double processCpuPercent = getProcCPU(pid);
                    if (processCpuPercent != null) {
                        cpuPercent = (cpuPercent == null ? 0.0 : cpuPercent) + processCpuPercent;
                    }
                    Long processResidentMem = getProcMem(pid);
                    if (processResidentMem != null) {
                        residentMem = (residentMem == null ? 0L : residentMem) + processResidentMem;
                    }
                }
                if (cpuPercent != null) {
                    processMetrics.put(CPU_PERCENT, String.valueOf(cpuPercent));
                }
                if (residentMem != null) {
                    processMetrics.put(RSS, String.valueOf(residentMem));
                }
            }
            processMetrics.put(MonitorConstants.RUNNING_INSTANCES_COUNT, String.valueOf(processLines.size()));
            processData.setProcessMetrics(processMetrics);
            processesData.put(instance.getDisplayName(), processData);
        }
        return processesData;
    }

    private OSProcess fetchValidProcess(String pid) {
        OSProcess current = currentSnapshots.get(pid);
        if (current == null) {
            return null;
        }
        OSProcess previous = previousSnapshots.get(pid);
        if (previous != null && previous.getStartTime() != current.getStartTime()) {
            logger.info("WindowsParser:: PID {} was reused (prev startTime={} current startTime={})",
                    pid, previous.getStartTime(), current.getStartTime());
            previousSnapshots.put(pid, current);
            return null;
        }
        return current;
    }

    protected Double getProcCPU(String pid) {
        try {
            OSProcess current = fetchValidProcess(pid);
            if (current == null) {
                return null;
            }
            OSProcess previous = previousSnapshots.put(pid, current);
            if (previous == null) {
                logger.debug("WindowsParser:: pid={} first sample — CPU% available on next poll", pid);
                return null;
            }
            double cpuPercent = current.getProcessCpuLoadBetweenTicks(previous) * 100.0;
            logger.debug("WindowsParser:: pid={} CPU%={}", pid, cpuPercent);
            return cpuPercent;
        } catch (Exception e) {
            logger.error("WindowsParser:: error fetching CPU% for pid={}", pid, e);
        }
        return null;
    }

    protected Long getProcMem(String pid) {
        try {
            OSProcess proc = fetchValidProcess(pid);
            if (proc == null) {
                return null;
            }
            return proc.getResidentSetSize() / 1024;
        } catch (Exception e) {
            logger.error("WindowsParser:: error fetching RSS for pid={}", pid, e);
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
