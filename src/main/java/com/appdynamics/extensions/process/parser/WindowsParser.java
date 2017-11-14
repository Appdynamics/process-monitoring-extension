/**
 * Copyright 2016 AppDynamics
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

import com.appdynamics.extensions.process.common.MonitorConstants;
import com.appdynamics.extensions.process.common.ProcessUtil;
import com.appdynamics.extensions.process.configuration.ConfigProcessor;
import com.appdynamics.extensions.process.configuration.Instance;
import com.appdynamics.extensions.process.data.ProcessData;
import com.google.common.base.Joiner;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.log4j.Logger;
import org.hyperic.sigar.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WindowsParser implements Parser {

    public static final Logger logger = Logger.getLogger(WindowsParser.class);

    public Map<String, ProcessData> parseProcesses(Map<String, ?> config) {
        List<Instance> instances = new ConfigProcessor().processConfig(config);
        // all process lines
        List<String> processListOutput = fetchProcessListFromSigar();
        // filter process lines based on configuration
        ListMultimap<String, String> filteredProcessLines = ProcessUtil.filterProcessLinesFromCompleteList(processListOutput, instances, buildHeaderInfo());
        // process the filtered lines and retrieve the data
        Map<String, ProcessData> processesData = populateProcessesData(instances, filteredProcessLines);
        return processesData;
    }

    public List<String> fetchProcessListFromSigar() {
        List<String> processLines = new ArrayList<String>();
        Sigar sigar = new Sigar();
        try {
            long [] pids = sigar.getProcList();
            for (long pid : pids) {
                String line = "";
                try {
                    String processName = sigar.getProcExe(pid).getName();
                    String processArgs = Joiner.on(" ").join(sigar.getProcArgs(pid));
                    line = pid + " " + processName + " " + processArgs;
                } catch (SigarPermissionDeniedException e) {
                    logger.trace("Unable to retrieve process name for pid " + pid + " " + e.getMessage());
                }
                processLines.add(line);
            }
        } catch (SigarException e) {
            logger.warn(e.getMessage());
        }
        return processLines;
    }

    public Map<String, ProcessData> populateProcessesData(List<Instance> instances, ListMultimap<String, String> filteredProcessLines) {
        Map<String, ProcessData> processesData = Maps.newHashMap();
        for (Instance instance : instances) {
            ProcessData processData = new ProcessData();
            Map<String, BigDecimal> processMetrics = Maps.newHashMap();
            List<String> processLines = filteredProcessLines.get(instance.getDisplayName());
            if (processLines.size() == 1) {
                String pid = processLines.get(0).trim().split("\\s+")[0];
                Sigar sigar = new Sigar();
                Double cpuPercent = getProcCPU(sigar, pid);
                Long residentMem = getProcMem(sigar, pid);
                if (cpuPercent != null) {
                    processMetrics.put("CPU Utilization %", new BigDecimal(cpuPercent));
                }
                if (residentMem != null) {
                    processMetrics.put("Resident Mem(MB)", new BigDecimal(residentMem).divide(new BigDecimal(1024*1024)));
                }
            }
            processMetrics.put(MonitorConstants.RUNNING_INSTANCES_COUNT, new BigDecimal(processLines.size()));
            processData.setProcessMetrics(processMetrics);
            processesData.put(instance.getDisplayName(), processData);
        }
        return processesData;
    }

    private Double getProcCPU(Sigar sigar, String pid) {
        try {
            ProcCpu procCpu = sigar.getProcCpu(pid);
            return procCpu.getPercent();
        } catch (SigarException e) {
            logger.error("Error while fetching cpu% for process " + pid, e);
        }
        return null;
    }

    private Long getProcMem(Sigar sigar, String pid) {
        try {
            ProcMem procMem = sigar.getProcMem(pid);
            return procMem.getResident();
        } catch (SigarException e) {
            logger.error("Error while fetching mem for process " + pid, e);
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
        return "Windows Processes";
    }
}
