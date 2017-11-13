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

import com.appdynamics.extensions.process.common.MonitorConstants;
import com.appdynamics.extensions.process.common.ProcessUtil;
import com.appdynamics.extensions.process.configuration.ConfigProcessor;
import com.appdynamics.extensions.process.configuration.Instance;
import com.appdynamics.extensions.process.data.ProcessData;
import com.google.common.base.Joiner;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
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
        List<Instance> instances = new ConfigProcessor().processConfig(config);
        // all process lines
        List<String> processListOutput = fetchProcessListFromSigar();
        // filter process lines based on configuration
        ListMultimap<String, String> filteredProcessLines = ProcessUtil.filterProcessLinesFromCompleteList(processListOutput, instances, buildHeaderInfo());
        // process the filtered lines and retrieve the data
        //TODO
        Map<String, ProcessData> processesData = ProcessUtil.populateProcessesData(instances, BigDecimal.ZERO, filteredProcessLines);
        return processesData;
    }

    public List<String> fetchProcessListFromSigar() {
        List<String> processLines = new ArrayList<String>();
        Sigar sigar = new Sigar();
        try {
            long [] pids = sigar.getProcList();
            for (long pid : pids) {
                String line = "";
                try {
                    String processName = sigar.getProcExe(pid).getName();
                    String processArgs = Joiner.on(" ").join(sigar.getProcArgs(pid));
                    line = pid + " " + processName + " " + processArgs;
                } catch (SigarPermissionDeniedException e) {
                    logger.trace("Unable to retrieve process name for pid " + pid + " " + e.getMessage());
                }
                processLines.add(line);
            }
        } catch (SigarException e) {
            logger.warn(e.getMessage());
        }
        return processLines;
    }

    private List<String> buildHeaderInfo() {
        List<String> headerInfo = Lists.newArrayList();
        headerInfo.add(MonitorConstants.PID);
        headerInfo.add(MonitorConstants.COMMAND);

        return headerInfo;
    }

    public String getProcessGroupName() {
        return "Windows Processes";
    }
}
