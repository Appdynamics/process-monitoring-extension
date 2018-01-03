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
import com.appdynamics.extensions.process.configuration.ConfigProcessor;
import com.appdynamics.extensions.process.configuration.Instance;
import com.appdynamics.extensions.process.data.ProcessData;
import com.appdynamics.extensions.util.AssertUtils;
import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.log4j.Logger;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public abstract class Parser {

    public static final Logger logger = Logger.getLogger(Parser.class);

    public abstract String getProcessGroupName();

    protected abstract Map<String, String> getCommands(Map<String, ?> config);

    public abstract Map<String, ProcessData> fetchMetrics(Map<String, ?> config);

    public Map<String, ProcessData> fetchMetrics(Map<String, ?> config, String cmd) {
        List<Instance> instances = new ConfigProcessor().processConfig(config);
        List<String> processListOutput = CommandExecutor.execute(cmd);
        AssertUtils.assertNotNull(processListOutput, "The output from " + cmd + " is null");
        List<String> headers = getHeaders(processListOutput, cmd);
        ListMultimap<String, String> filteredProcessLines = filterProcessLinesFromCompleteList(processListOutput.subList(1, processListOutput.size()), instances, headers);
        Map<String, ProcessData> processMetrics = populateProcessesData(instances, filteredProcessLines, headers);
        return processMetrics;

    }

    private List<String> getHeaders(List<String> processListOutput, String cmd) {
        List<String> headerColumns = Lists.newArrayList();
        if (!processListOutput.isEmpty()) {
            // first line in the output has the header
            String header = processListOutput.get(0).trim();
            // removes UTF8 BOM if exists (exists in solaris output causing indexoutofboundsexception)
            if (header.startsWith("\uFEFF")) {
                header = header.substring(1).trim();
            }
            headerColumns = Arrays.asList(header.split(MonitorConstants.SPACES));
        }
        return headerColumns;
    }

    protected ListMultimap<String, String> filterProcessLinesFromCompleteList(List<String> processOutputList, List<Instance> instances, List<String> headerColumns) {
        ListMultimap<String, String> filteredProcesses = ArrayListMultimap.create();
        logger.debug("Process list output is: " + processOutputList);
        for (String processLine : processOutputList) {
            if (!Strings.isNullOrEmpty(processLine)) {
                // Second argument limit in split is to prevent last header (command) from splitting in case of spaces and maintain the size of the array
                String [] processLineColumns = processLine.trim().split(MonitorConstants.SPACES, headerColumns.size());
                if (processLineColumns.length == headerColumns.size()) {
                    for (Instance instance : instances) {
                        String regex = instance.getRegex();
                        String pidToMatch = instance.getPid();
                        String displayName = instance.getDisplayName();
                        if (!Strings.isNullOrEmpty(regex)) {
                            String commandPath = processLineColumns[headerColumns.indexOf(MonitorConstants.COMMAND)].trim();
                            boolean matches = commandPath.matches(regex);
                            if (matches) {
                                filteredProcesses.put(displayName, processLine);
                                logger.debug("Found match for regex " + regex + " in " + commandPath);
                            }
                        } else if (!Strings.isNullOrEmpty(pidToMatch)){
                            String pid = processLineColumns[headerColumns.indexOf(MonitorConstants.PID)].trim();
                            if (pidToMatch.equals(pid)) {
                                filteredProcesses.put(displayName, processLine);
                                logger.debug("Found matching pid for " + pid);
                            }
                        } else {
                            logger.warn("regex/pid/pidFile not properly defined in config.yml for instance " + displayName);
                        }
                    }
                } else {
                    logger.warn("processLine and header columns not equal " + processLine + " " + Arrays.asList(headerColumns));
                }
            }
        }
        return filteredProcesses;
    }

    private Map<String, ProcessData> populateProcessesData(List<Instance> instances, ListMultimap<String, String> filteredProcessLines, List<String> headerColumns) {
        Map<String, ProcessData> processesData = Maps.newHashMap();
        for (Instance instance : instances) {
            ProcessData processData = new ProcessData();
            Map<String, String> processMetrics = Maps.newHashMap();
            List<String> processLines = filteredProcessLines.get(instance.getDisplayName());
            if (processLines.size() == 1) {
                String [] processLineColumns = processLines.get(0).trim().split(MonitorConstants.SPACES);
                // ignoring first and last columns (pid and command)
                for (int i = 1; i < headerColumns.size(); i++) {
                    processMetrics.put(headerColumns.get(i), processLineColumns[i]);
                }
            }
            processMetrics.put(MonitorConstants.RUNNING_INSTANCES_COUNT, String.valueOf(processLines.size()));
            processData.setProcessMetrics(processMetrics);
            processesData.put(instance.getDisplayName(), processData);
        }
        return processesData;
    }
}
