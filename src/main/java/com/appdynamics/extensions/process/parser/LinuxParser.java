/**
 * Copyright 2013 AppDynamics
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

import com.appdynamics.extensions.process.common.CmdOutHeaderConstants;
import com.appdynamics.extensions.process.common.CommandExecutorException;
import com.appdynamics.extensions.process.common.ProcessCommands;
import com.appdynamics.extensions.process.config.Configuration;
import com.appdynamics.extensions.process.processdata.ProcessData;
import com.appdynamics.extensions.process.processexception.ProcessMonitorException;
import com.google.common.collect.Maps;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class LinuxParser extends Parser {

    private static final Logger logger = Logger.getLogger(LinuxParser.class);

    public LinuxParser(Configuration config) {
        super(config);
        processGroupName = "Linux Processes";
        processes = new HashMap<String, ProcessData>();
        includeProcesses = new HashSet<String>();
    }

    public void retrieveMemoryMetrics() throws ProcessMonitorException, CommandExecutorException {
        Process p = commandExecutor.execute(ProcessCommands.LINUX_MEMORY_COMMAND);
        BufferedReader input = null;
        try {
            input = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            if ((line = input.readLine()) != null) {
                String[] words = line.trim().split("\\s+");
                setTotalMemSizeMB(toBigDecimal(words[1].trim()).divide(new BigDecimal(1024)));
            }
        } catch (IOException e) {
            logger.error("Error in parsing the output of command " + ProcessCommands.LINUX_MEMORY_COMMAND, e);
            throw new ProcessMonitorException("Error in parsing the output of command " + ProcessCommands.LINUX_MEMORY_COMMAND, e);
        } catch (NumberFormatException e) {
            logger.error("Unable to retrieve total physical memory size (not a number) ", e);
            throw new ProcessMonitorException("Unable to retrieve total physical memory size (not a number) ", e);
        } finally {
            closeBufferedReader(input);
            cleanUpProcess(p, ProcessCommands.LINUX_MEMORY_COMMAND);
        }
    }

    public String getNameOfProcess(int pid) throws CommandExecutorException {
        String cmd = String.format(ProcessCommands.LINUX_PROCESS_NAME_COMMAD, pid);
        Process p = commandExecutor.execute(cmd);
        BufferedReader input = null;
        try {
            input = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            if ((line = input.readLine()) != null) {
                String[] words = line.trim().split("\\s+");
                return words[1];
            }
        } catch (IOException e) {
            logger.error("Error in parsing the output of command " + cmd, e);
        } catch (Exception e) {
            logger.error(e);
        } finally {
            closeBufferedReader(input);
            cleanUpProcess(p, cmd);
        }
        return null;
    }

    /**
     * Parsing the 'ps aux' command and gathering process information data.
     *
     * @throws NumberFormatException
     * @throws ProcessMonitorException
     */
    public void parseProcesses() throws NumberFormatException, ProcessMonitorException, CommandExecutorException {
        this.retrieveMemoryMetrics();

        String cmd = ProcessCommands.LINUX_PROCESS_LIST_COMMAND;
        Process p = commandExecutor.execute(cmd);
        BufferedReader input = null;
        try {
            input = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            int i = 0;
            Map<String, Integer> headerPositions = Maps.newHashMap();
            while ((line = input.readLine()) != null) {
                if (i == 0) {
                    String [] headerArray = {CmdOutHeaderConstants.LINUX_PID, CmdOutHeaderConstants.LINUX_CPU_PERCENT, CmdOutHeaderConstants.LINUX_MEM_PERCENT};
                    headerPositions = processHeaderLine(cmd, line, headerArray, "\\s+");
                } else {
                    String[] words = line.split("\\s+");

                    int pid = Integer.parseInt(words[headerPositions.get(CmdOutHeaderConstants.LINUX_PID)].trim());

                    BigDecimal cpuUtilizationInPercent = toBigDecimal(words[headerPositions.get(CmdOutHeaderConstants.LINUX_CPU_PERCENT)]);
                    BigDecimal memUtilizationInPercent = toBigDecimal(words[headerPositions.get(CmdOutHeaderConstants.LINUX_MEM_PERCENT)]);
                    BigDecimal absoluteMem = (memUtilizationInPercent.divide(new BigDecimal(100)).multiply(getTotalMemSizeMB()));

                    String processName = getNameOfProcess(pid);

                    populateProcessData(processName, pid, cpuUtilizationInPercent, memUtilizationInPercent, absoluteMem);
                }
                i++;
            }
        } catch (IOException e) {
            logger.error("Error in parsing the output of command " + cmd, e);
            throw new ProcessMonitorException("Error in parsing the output of command " + cmd, e);
        } catch (Exception e) {
            logger.error("Exception: ", e);
            throw new ProcessMonitorException("Exception: ", e);
        } finally {
            closeBufferedReader(input);
            cleanUpProcess(p, cmd);
        }
    }
}
