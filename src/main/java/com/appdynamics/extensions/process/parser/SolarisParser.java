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

import java.util.Arrays;
import java.util.List;
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
        Map<String, String> commands = (Map) config.get("commands");
        String cmd;
        if (commands != null && !Strings.isNullOrEmpty(commands.get("solarisProcess"))) {
            cmd = commands.get("solarisProcess");
        } else {
            cmd = MonitorConstants.SOLARIS_PROCESS_LIST_COMMAND;
        }
        return cmd;
    }

    private Map<String, ProcessData> parseProcesses(String cmd, List<String> processListOutput, List<Instance> instances) {
        Map<String, ProcessData> processesData = Maps.newHashMap();
        if (processListOutput != null && !processListOutput.isEmpty()) {
            // skip first 6 lines as they are not required
            processListOutput = processListOutput.subList(6, processListOutput.size());

            List<String> headerColumns = Arrays.asList(processListOutput.get(0).trim().split("\\s+"));

            ListMultimap<String, String> filteredProcessLines = ProcessUtil.filterProcessLinesFromCompleteList(processListOutput, instances, headerColumns);

            processesData = ProcessUtil.populateProcessesData(instances, filteredProcessLines);
        } else {
            logger.warn("Output from command " + cmd + " is null or empty " + processListOutput);
        }
        return processesData;
    }

    public String getProcessGroupName() {
        return "Solaris Processes";
    }
}
