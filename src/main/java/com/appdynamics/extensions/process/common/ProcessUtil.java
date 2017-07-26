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

import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import org.apache.log4j.Logger;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ProcessUtil {

    private static final Logger logger = Logger.getLogger(ProcessUtil.class);

    public static Map<String, Integer> processHeaderLine(String command, String headerLine, String [] headerStrings, String delimiter) {
        List<String> headersList = Arrays.asList(headerLine.trim().split(delimiter));
        Map<String, Integer> headerInfo = Maps.newHashMap();
        for(String headerString : headerStrings) {
            if(headersList.contains(headerString)) {
                Integer pos = headersList.indexOf(headerString);
                headerInfo.put(headerString, pos);
                logger.debug(headerString + " Position in header: " + pos);
            } else {
                logger.error("Could not find correct header information for " + headerString + " while executing command " + command);
            }
        }
        return headerInfo;
    }

    public static Map isMatchProcessName(List<Map> processes, String processName) {
        for (Map regexProcess : processes) {
            String pattern = (String) regexProcess.get("pattern");
            if(!Strings.isNullOrEmpty(pattern)) {
                boolean matches = processName.matches(pattern);
                if (matches) {
                    logger.debug(processName + " matches with " + regexProcess.get("displayName"));
                    return regexProcess;
                }
            }
        }
        return null;
    }

    public static ListMultimap<String, String> filterProcessesToBeMonitoredFromCompleteList(List<String> processOutputList, List<Map> processesToBeMonitored, Integer commandPos, String splitRegex, int splitLimit) {
        ListMultimap<String, String> filteredProcesses = ArrayListMultimap.create();
        for (String processLine : processOutputList) {
            if (!Strings.isNullOrEmpty(processLine)) {
                String [] words = processLine.trim().split(splitRegex, splitLimit);
                String command = words[commandPos].trim();
                Map<String, String> processNeedsToBeReported = isMatchProcessName(processesToBeMonitored, command);
                if (processNeedsToBeReported != null) {
                    filteredProcesses.put(processNeedsToBeReported.get("displayName"), processLine);
                }
            }
        }
        return filteredProcesses;
    }

    private static Integer unLocalizeStrValue(String valueStr) {
        try {
            Locale loc = Locale.getDefault();
            return Integer.valueOf(NumberFormat.getInstance(loc).parse(valueStr).intValue());
        } catch (ParseException e) {
            logger.error("Exception while unlocalizing number string " + valueStr, e);
        }
        return null;
    }
}
