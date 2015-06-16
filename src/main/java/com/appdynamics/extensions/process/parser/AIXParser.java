/**
 * Copyright 2015 AppDynamics
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

import com.appdynamics.extensions.process.common.CommandExecutorException;
import com.appdynamics.extensions.process.common.ProcessCommands;
import com.appdynamics.extensions.process.config.Configuration;
import com.appdynamics.extensions.process.processdata.ProcessData;
import com.appdynamics.extensions.process.processexception.ProcessMonitorException;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;


public class AIXParser extends Parser {
    private static final Logger logger = Logger.getLogger(AIXParser.class);
    private int posPID = -1, posCPU = -1, posMem = -1, posCommand = -1; // used for parsing

    public AIXParser(Configuration config) {
        super(config);
        processGroupName = "AIX Processes";
        processes = new HashMap<String, ProcessData>();
        includeProcesses = new HashSet<String>();
    }


    @Override
    public void parseProcesses() throws ProcessMonitorException, CommandExecutorException {
        fetchMemorySize();

        String processListCommand = ProcessCommands.AIX_PROCESS_LIST_COMMAND;
        Process p = commandExecutor.execute(processListCommand);
        BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
        try {
            String line = input.readLine().trim();
            processHeader(line);
            while ((line = input.readLine()) != null) {
                String[] words = line.trim().split("\\s+");
                int pid = Integer.parseInt(words[posPID].trim());

                BigDecimal cpuUtilizationInPercent = toBigDecimal(words[posCPU].trim());
                BigDecimal memUtilizationInPercent = toBigDecimal(words[posMem].trim());
                String processName = words[posCommand].trim();
                BigDecimal absoluteMem = (memUtilizationInPercent.divide(new BigDecimal(100)).multiply(getTotalMemSizeMB()));
                populateProcessData(processName, pid, cpuUtilizationInPercent, memUtilizationInPercent, absoluteMem);
            }
        } catch (IOException e) {
            logger.error("Error in parsing the output of command " + processListCommand, e);
            throw new ProcessMonitorException("Error in parsing the output of command " + processListCommand, e);
        } finally {
            closeBufferedReader(input);
            cleanUpProcess(p, processListCommand);
        }


    }

    private void fetchMemorySize() throws CommandExecutorException, ProcessMonitorException {
        String cmd = ProcessCommands.AIX_MEMORY_COMMAND;
        Process p = commandExecutor.execute(cmd);
        BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line;
        try {
            if ((line = input.readLine()) != null) {
                setTotalMemSizeMB(toBigDecimal(line.trim()).divide(new BigDecimal(1024)));
            }
        } catch (IOException e) {
            logger.error("Error in parsing the output of command to fetch Memory " + cmd, e);
            throw new ProcessMonitorException("Error in parsing the output of command to fetch Memory " + cmd, e);
        } finally {
            closeBufferedReader(input);
            cleanUpProcess(p, cmd);
        }
    }

    private void processHeader(String processLine) throws ProcessMonitorException {
        String[] words = processLine.split("\\s+");
        for (int i = 0; i < words.length; i++) {
            if (words[i].equals("PID")) {
                posPID = i;
            } else if (words[i].equals("%CPU")) {
                posCPU = i;
            } else if (words[i].equals("%MEM")) {
                posMem = i;
            } else if (words[i].equals("COMMAND")) {
                posCommand = i;
            }
        }
        if (posMem == -1 || posCPU == -1 || posMem == -1 || posCommand == -1) {
            throw new ProcessMonitorException("Can't find correct process stats from 'ps -eo pid,pcpu,pmem,command' command. Terminating Process Monitor");
        }
    }
}
