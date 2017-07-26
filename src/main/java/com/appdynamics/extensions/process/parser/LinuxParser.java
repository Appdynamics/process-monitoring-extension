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

import com.appdynamics.extensions.process.common.*;
import com.appdynamics.extensions.process.data.ProcessData;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import org.apache.log4j.Logger;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class LinuxParser implements Parser {

    public static final Logger logger = Logger.getLogger(LinuxParser.class);

    public Map<String, ProcessData> parseProcesses(Map<String, ?> config) {
        List<Map> processesToBeMonitored = (List) config.get("processesToBeMonitored");
        String cmd = ProcessCommands.LINUX_PROCESS_LIST_COMMAND;
        List<String> processListOutput = CommandExecutor.execute(cmd);
        Map<String, ProcessData> processMetrics = parseProcesses(cmd, processListOutput, processesToBeMonitored);
        return processMetrics;

    }

    private Map<String, ProcessData> parseProcesses(String cmd, List<String> processListOutput, List<Map> processesToBeMonitored) {
        Map<String, ProcessData> processesData = Maps.newHashMap();
        if (processListOutput != null && !processListOutput.isEmpty()) {
            String [] headerArray = {CmdOutHeaderConstants.LINUX_CPU_PERCENT, CmdOutHeaderConstants.LINUX_MEM_PERCENT, CmdOutHeaderConstants.COMMAND};
            Map<String, Integer> headerPositions = ProcessUtil.processHeaderLine(cmd, processListOutput.get(0), headerArray, "\\s+");

            ListMultimap<String, String> filteredProcessLines = ProcessUtil.filterProcessesToBeMonitoredFromCompleteList(processListOutput, processesToBeMonitored, headerPositions.get(CmdOutHeaderConstants.COMMAND), "\\s+", 3);

            populateProcessesData(processesToBeMonitored, processesData, filteredProcessLines);
        } else {
            logger.warn("Output from command " + cmd + " is null or empty " + processListOutput);
        }
        return processesData;
    }

    // specific to parser to which data to populate
    private void populateProcessesData(List<Map> processesToBeMonitored, Map<String, ProcessData> processesData, ListMultimap<String, String> filteredProcessLines) {
        for (Map processToBeMonitored : processesToBeMonitored) {
            ProcessData processData = new ProcessData();
            Map<String, BigDecimal> processMetrics = Maps.newHashMap();
            String displayName = (String) processToBeMonitored.get("displayName");
            List<String> processLines = filteredProcessLines.get(displayName);
            processMetrics.put(MetricConstants.NUMBER_OF_RUNNING_INSTANCES, new BigDecimal(processLines.size()));
            processData.setProcessMetrics(processMetrics);
            processesData.put(displayName, processData);
        }
    }

    public String getProcessGroupName() {
        return "Linux Processes";
    }
}
