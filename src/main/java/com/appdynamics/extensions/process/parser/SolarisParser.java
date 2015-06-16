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
import com.google.common.base.Strings;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;


public class SolarisParser extends Parser {
    private int posPID = -1, posCPU = -1, posMem = -1, posProcName = -1;// used for parsing
    private static final Logger logger = Logger.getLogger(SolarisParser.class);

    public SolarisParser(Configuration config) {
        super(config);
        processGroupName = "Solaris Processes";
        processes = new HashMap<String, ProcessData>();
        includeProcesses = new HashSet<String>();
    }


    @Override
    public void parseProcesses() throws ProcessMonitorException, CommandExecutorException {
        String cmd = ProcessCommands.SOLARIS_PROCESS_LIST_COMMAND;
        Process p = commandExecutor.execute(cmd);
        BufferedReader input = null;
        try {
            input = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            skipParsingLines(input, 4);
            processMemoryLine(input.readLine());
            skipParsingLines(input, 1);
            processHeader(input.readLine());

            while (!Strings.isNullOrEmpty(input.readLine())) {
                line = input.readLine();
                String[] words = line.trim().split("\\s+");
                int pid = Integer.parseInt(words[posPID].trim());
                String processName = words[posProcName].trim();

                BigDecimal cpuUtilizationInPercent = toBigDecimal(words[posCPU].split("%")[0].trim());
                BigDecimal absoluteMemUsed = parseMemoryString(words[posMem].trim());

                BigDecimal memUtilizationInPercent = (absoluteMemUsed.divide(getTotalMemSizeMB(), BigDecimal.ROUND_HALF_UP)).multiply(new BigDecimal(100));

                populateProcessData(processName, pid, cpuUtilizationInPercent, memUtilizationInPercent, absoluteMemUsed);
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

    private BigDecimal parseMemoryString(String memString) {
        BigDecimal mem = null;
        if (memString.endsWith("M")) {
            mem = toBigDecimal(memString.split("M")[0].trim());
        } else if (memString.endsWith("K")) {
            mem = toBigDecimal(memString.split("K")[0].trim()).divide(new BigDecimal(1024));
        }
        return mem;
    }

    private void processMemoryLine(String line) throws ProcessMonitorException {
        String memoryToken = line.split(",")[0].trim().split(":")[1].trim().split("\\s+")[0].trim();
        if (memoryToken.endsWith("M")) {
            String memoryString = memoryToken.split("M")[0].trim();
            setTotalMemSizeMB(new BigDecimal(unLocalizeStrValue(memoryString)));
        } else {
            logger.error("Couldn't parse " + line + " for memory retrieval");
            throw new ProcessMonitorException("Couldn't parse " + line + " for memory retrieval");
        }
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

    private void processHeader(String processLine) throws ProcessMonitorException {
        String[] words = processLine.trim().split("\\s+");
        for (int i = 0; i < words.length; i++) {
            if (words[i].equals("PID")) {
                posPID = i;
            } else if (words[i].equals("CPU")) {
                posCPU = i;
            } else if (words[i].equals("SIZE")) {
                posMem = i;
            } else if (words[i].equals("COMMAND")) {
                posProcName = i;
            }
        }
        if (posPID == -1 || posCPU == -1 || posMem == -1 || posProcName == -1) {
            throw new ProcessMonitorException("Can't find correct process stats from 'top' command. Terminating Process Monitor");
        }
    }
}
