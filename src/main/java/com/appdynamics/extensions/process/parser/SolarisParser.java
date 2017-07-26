package com.appdynamics.extensions.process.parser;

import com.appdynamics.extensions.process.common.*;
import com.appdynamics.extensions.process.data.ProcessData;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import org.apache.log4j.Logger;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class SolarisParser implements Parser {
    public static final Logger logger = Logger.getLogger(LinuxParser.class);

    public Map<String, ProcessData> parseProcesses(Map<String, ?> config) {
        List<Map> processesToBeMonitored = (List) config.get("processesToBeMonitored");
        String cmd = ProcessCommands.SOLARIS_PROCESS_LIST_COMMAND;
        List<String> processListOutput = CommandExecutor.execute(cmd);
        Map<String, ProcessData> processMetrics = parseProcesses(cmd, processListOutput, processesToBeMonitored);
        return processMetrics;
    }

    private Map<String, ProcessData> parseProcesses(String cmd, List<String> processListOutput, List<Map> processesToBeMonitored) {
        Map<String, ProcessData> processesData = Maps.newHashMap();
        if (processListOutput != null && !processListOutput.isEmpty()) {
            List<String> truncatedProcessList = processListOutput.subList(6, processListOutput.size());
            String [] headerArray = {CmdOutHeaderConstants.SOLARIS_PID, CmdOutHeaderConstants.SOLARIS_CPU, CmdOutHeaderConstants.SOLARIS_MEM, CmdOutHeaderConstants.SOLARIS_PROC_NAME};
            Map<String, Integer> headerPositions = ProcessUtil.processHeaderLine(cmd, truncatedProcessList.get(0), headerArray, "\\s+");

            ListMultimap<String, String> filteredProcessLines = ProcessUtil.filterProcessesToBeMonitoredFromCompleteList(truncatedProcessList, processesToBeMonitored, headerPositions.get(CmdOutHeaderConstants.COMMAND), "\\s+", 0);

            populateProcessesData(processesToBeMonitored, processesData, filteredProcessLines);
        } else {
            logger.warn("Output from command " + cmd + " is null or empty " + processListOutput);
        }
        return processesData;
    }

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
        return "Solaris Processes";
    }
}
