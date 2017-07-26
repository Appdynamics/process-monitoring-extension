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

import com.appdynamics.extensions.process.common.MetricConstants;
import com.appdynamics.extensions.process.common.ProcessUtil;
import com.appdynamics.extensions.process.data.ProcessData;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import org.apache.log4j.Logger;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.SigarPermissionDeniedException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WindowsParser implements Parser {

    public static final Logger logger = Logger.getLogger(WindowsParser.class);

    public Map<String, ProcessData> parseProcesses(Map<String, ?> config) {
        List<Map> processesToBeMonitored = (List) config.get("processesToBeMonitored");
        List<String> processListOutput = fetchProcessListFromSigar();
        ListMultimap<String, String> filteredProcessLines = ProcessUtil.filterProcessesToBeMonitoredFromCompleteList(processListOutput, processesToBeMonitored, 1, ",", 0);

        Map<String, ProcessData> processesData = populateProcessesData(processesToBeMonitored, filteredProcessLines);
        return processesData;
    }

    private Map<String, ProcessData> populateProcessesData(List<Map> processesToBeMonitored, ListMultimap<String, String> filteredProcessLines) {
        Map<String, ProcessData> processesData = Maps.newHashMap();
        for (Map processToBeMonitored : processesToBeMonitored) {
            ProcessData processData = new ProcessData();
            Map<String, BigDecimal> processMetrics = Maps.newHashMap();
            String displayName = (String) processToBeMonitored.get("displayName");
            List<String> processLines = filteredProcessLines.get(displayName);
            processMetrics.put(MetricConstants.NUMBER_OF_RUNNING_INSTANCES, new BigDecimal(processLines.size()));
            processData.setProcessMetrics(processMetrics);
            processesData.put(displayName, processData);
        }
        return processesData;
    }

    public List<String> fetchProcessListFromSigar() {
        List<String> processLines = new ArrayList<String>();
        Sigar sigar = new Sigar();
        try {
            long [] pids = sigar.getProcList();
            for (long pid : pids) {
                String line = "";
                if (pid == 4)
                    continue;
                try {
                    String processName = sigar.getProcExe(pid).getName();
                    line = pid + "," + processName;
                } catch (SigarPermissionDeniedException e) {
                    logger.warn("Unable to retrieve process name for pid " + pid + " " + e.getMessage());
                }
                processLines.add(line);
            }
        } catch (SigarException e) {
            logger.warn(e.getMessage());
        }
        return processLines;
    }

    public String getProcessGroupName() {
        return "Windows Processes";
    }
}
