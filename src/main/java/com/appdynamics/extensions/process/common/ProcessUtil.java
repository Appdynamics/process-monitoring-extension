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
package com.appdynamics.extensions.process.common;

import com.appdynamics.extensions.process.configuration.Instance;
import com.appdynamics.extensions.process.data.ProcessData;
import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import org.apache.log4j.Logger;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class ProcessUtil {

    private static final Logger logger = Logger.getLogger(ProcessUtil.class);

    public static ListMultimap<String, String> filterProcessLinesFromCompleteList(List<String> processOutputList, List<Instance> instances, List<String> headerColumns) {
        ListMultimap<String, String> filteredProcesses = ArrayListMultimap.create();
        for (String processLine : processOutputList) {
            if (!Strings.isNullOrEmpty(processLine)) {
                // limit is to prevent command from splitting and maintain the size of the array
                String [] processLineColumns = processLine.trim().split("\\s+", headerColumns.size());
                for (Instance instance : instances) {
                    String regex = instance.getRegex();
                    String pidToMatch = String.valueOf(instance.getPid());
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
                    }
                }
            }
        }
        return filteredProcesses;
    }

    public static BigDecimal toBigDecimal(String valueStr) {
        if (!Strings.isNullOrEmpty(valueStr.trim())) {
            try {
                return new BigDecimal(valueStr.trim());
            } catch (NumberFormatException e) {
                logger.warn("Cannot convert the value " + valueStr + " to string ");
            }
        }
        return null;
    }
}
