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
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class AIXParser implements Parser {
    public static final Logger logger = Logger.getLogger(AIXParser.class);

    public Map<String, ProcessData> parseProcesses(Map<String, ?> config) {
        List<Instance> instances = new ConfigProcessor().processConfig(config);
        Map<String, String> commands = getCommands(config);
        List<String> memoryOutput = CommandExecutor.execute(getMemoryCommand(commands));

        List<String> processListOutput = CommandExecutor.execute(getProcessListCommand(commands));
        Map<String, ProcessData> processMetrics = parseProcesses(getProcessListCommand(commands), getTotalMemory(memoryOutput), processListOutput, instances);
        return processMetrics;

    }

    private Map<String, String> getCommands(Map<String, ?> config) {
        return (Map) config.get("aixCommands");
    }

    private String getProcessListCommand(Map<String, String> commands) {
        String cmd;
        if (commands != null && !Strings.isNullOrEmpty(commands.get("process"))) {
            cmd = commands.get("process");
        } else {
            cmd = MonitorConstants.AIX_PROCESS_LIST_COMMAND;
        }
        return cmd;
    }

    private String getMemoryCommand(Map<String, String> commands) {
        String cmd;
        if (commands != null && !Strings.isNullOrEmpty(commands.get("memory"))) {
            cmd = commands.get("memory");
        } else {
            cmd = MonitorConstants.AIX_MEMORY_COMMAND;
        }
        return cmd;
    }

    private BigDecimal getTotalMemory(List<String> memoryOutput) {
        BigDecimal memory = null;
        try {
            if (memoryOutput != null) {
                String memoryLine = memoryOutput.get(0);
                memory = ProcessUtil.toBigDecimal(memoryLine).divide(new BigDecimal(1024));
                logger.debug("Memory: " + memory);
            }
        } catch (Exception e) {
            logger.error(e);
        }
        return memory;
    }

    private Map<String, ProcessData> parseProcesses(String cmd, BigDecimal memory, List<String> processListOutput, List<Instance> instances) {
        Map<String, ProcessData> processesData = Maps.newHashMap();
        if (processListOutput != null && !processListOutput.isEmpty()) {
            List<String> headerColumns = Arrays.asList(processListOutput.get(0).trim().split("\\s+"));

            ListMultimap<String, String> filteredProcessLines = ProcessUtil.filterProcessLinesFromCompleteList(processListOutput, instances, headerColumns);

            processesData = populateProcessesData(instances, memory, filteredProcessLines, headerColumns);

        } else {
            logger.warn("Output from command " + cmd + " is null or empty " + processListOutput);
        }
        return processesData;
    }

    public static Map<String, ProcessData> populateProcessesData(List<Instance> instances, BigDecimal memory, ListMultimap<String, String> filteredProcessLines, List<String> headerColumns) {
        Map<String, ProcessData> processesData = Maps.newHashMap();
        for (Instance instance : instances) {
            ProcessData processData = new ProcessData();
            Map<String, BigDecimal> processMetrics = Maps.newHashMap();
            List<String> processLines = filteredProcessLines.get(instance.getDisplayName());
            if (processLines.size() == 1) {
                String [] processLineColumns = processLines.get(0).trim().split("\\s+");
                BigDecimal cpuPercent = ProcessUtil.toBigDecimal(processLineColumns[headerColumns.indexOf(MonitorConstants.AIX_CPU_PERCENT)]);
                BigDecimal memPercent = ProcessUtil.toBigDecimal(processLineColumns[headerColumns.indexOf(MonitorConstants.AIX_MEM_PERCENT)]);
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

    public String getProcessGroupName() {
        return "AIX Processes";
    }
}
