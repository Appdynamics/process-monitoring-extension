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

public class LinuxParser implements Parser {

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
        if (commands != null && !Strings.isNullOrEmpty(commands.get("linuxProcess"))) {
            cmd = commands.get("linuxProcess");
        } else {
            cmd = MonitorConstants.LINUX_PROCESS_LIST_COMMAND;
        }
        return cmd;
    }

    private Map<String, ProcessData> parseProcesses(String cmd, List<String> processListOutput, List<Instance> instances) {
        Map<String, ProcessData> processesData = Maps.newHashMap();
        if (processListOutput != null && !processListOutput.isEmpty()) {
            List<String> headerColumns = Arrays.asList(processListOutput.get(0).trim().split("\\s+"));

            ListMultimap<String, String> filteredProcessLines = ProcessUtil.filterProcessLinesFromCompleteList(processListOutput, instances, headerColumns);

            processesData = ProcessUtil.populateProcessesData(instances, filteredProcessLines);

        } else {
            logger.warn("Output from command " + cmd + " is null or empty " + processListOutput);
        }
        return processesData;
    }

    public String getProcessGroupName() {
        return "Linux Processes";
    }
}
