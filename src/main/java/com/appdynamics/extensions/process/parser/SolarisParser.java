package com.appdynamics.extensions.process.parser;

import com.appdynamics.extensions.process.common.CommandExecutor;
import com.appdynamics.extensions.process.common.MonitorConstants;
import com.appdynamics.extensions.process.common.ProcessUtil;
import com.appdynamics.extensions.process.configuration.ConfigProcessor;
import com.appdynamics.extensions.process.configuration.Instance;
import com.appdynamics.extensions.process.data.ProcessData;
import com.google.common.base.Strings;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import org.apache.log4j.Logger;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SolarisParser implements Parser {
    public static final Logger logger = Logger.getLogger(LinuxParser.class);

    public Map<String, ProcessData> parseProcesses(Map<String, ?> config) {
        List<Instance> instances = new ConfigProcessor().processConfig(config);
        String cmd = getCommand(config);
        List<String> processListOutput = CommandExecutor.execute(cmd);
        Map<String, ProcessData> processMetrics = parseProcesses(cmd, processListOutput, instances);
        return processMetrics;
    }

    private String getCommand(Map<String, ?> config) {
        Map<String, String> commands = (Map) config.get("solarisCommands");
        String cmd;
        if (commands != null && !Strings.isNullOrEmpty(commands.get("process"))) {
            cmd = commands.get("process");
        } else {
            cmd = MonitorConstants.SOLARIS_PROCESS_LIST_COMMAND;
        }
        return cmd;
    }

    private Map<String, ProcessData> parseProcesses(String cmd, List<String> processListOutput, List<Instance> instances) {
        Map<String, ProcessData> processesData = Maps.newHashMap();
        if (processListOutput != null && !processListOutput.isEmpty()) {

            BigDecimal memory = processMemoryLine(processListOutput.get(4));

            // skip first 6 lines as they are not required
            processListOutput = processListOutput.subList(6, processListOutput.size());

            List<String> headerColumns = Arrays.asList(processListOutput.get(0).trim().split("\\s+"));

            ListMultimap<String, String> filteredProcessLines = ProcessUtil.filterProcessLinesFromCompleteList(processListOutput, instances, headerColumns);

            processesData = populateProcessesData(instances, memory, filteredProcessLines, headerColumns);
        } else {
            logger.warn("Output from command " + cmd + " is null or empty " + processListOutput);
        }
        return processesData;
    }

    public Map<String, ProcessData> populateProcessesData(List<Instance> instances, BigDecimal memory, ListMultimap<String, String> filteredProcessLines, List<String> headerColumns) {
        Map<String, ProcessData> processesData = Maps.newHashMap();
        for (Instance instance : instances) {
            ProcessData processData = new ProcessData();
            Map<String, BigDecimal> processMetrics = Maps.newHashMap();
            List<String> processLines = filteredProcessLines.get(instance.getDisplayName());
            if (processLines.size() == 1) {
                String [] processLineColumns = processLines.get(0).trim().split("\\s+");
                BigDecimal cpuPercent = ProcessUtil.toBigDecimal(processLineColumns[headerColumns.indexOf(MonitorConstants.SOLARIS_CPU)].split("%")[0].trim());
                BigDecimal memPercent = ProcessUtil.toBigDecimal(processLineColumns[headerColumns.indexOf(MonitorConstants.SOLARIS_MEM)]);
                BigDecimal absoluteMemUsed = memPercent.divide(new BigDecimal(100)).multiply(memory);

                processMetrics.put("CPU Utilization %", cpuPercent);
                processMetrics.put("Memory Utilization %", memPercent);
                processMetrics.put("Memory Utilization (MB)", absoluteMemUsed);
            }
            processMetrics.put(MonitorConstants.RUNNING_INSTANCES_COUNT, new BigDecimal(processLines.size()));
            processData.setProcessMetrics(processMetrics);
            processesData.put(instance.getDisplayName(), processData);
        }
        return processesData;
    }

    private BigDecimal processMemoryLine(String line) {
        String memoryToken = line.split(",")[0].trim().split(":")[1].trim().split("\\s+")[0].trim();
        if (memoryToken.endsWith("M")) {
            String memoryString = memoryToken.split("M")[0].trim();
            return new BigDecimal(memoryString);
        } else {
            logger.error("Couldn't parse " + line + " for memory retrieval");
        }
        return null;
    }

    public String getProcessGroupName() {
        return "Solaris Processes";
    }
}
