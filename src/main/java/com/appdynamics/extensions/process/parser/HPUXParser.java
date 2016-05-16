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

import com.appdynamics.extensions.process.common.CmdOutHeaderConstants;
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
import java.util.*;


public class HPUXParser extends Parser {
    private static final Logger logger = Logger.getLogger(AIXParser.class);

    public HPUXParser(Configuration config) {
        super(config);
        processGroupName = "HP-UX Processes";
        processes = new HashMap<String, ProcessData>();
        includeProcesses = new HashSet<String>();
    }

    @Override
    public void parseProcesses() throws ProcessMonitorException, CommandExecutorException {
        fetchMemory();
        String cmd = ProcessCommands.HPUX_PROCESS_COMMAND;
        // XPG4 behaviour, for details check man ps
        List<String> env = new ArrayList<String>();
        env.add("UNIX95=\"\"");
        Process p = commandExecutor.execute(cmd, env);
        BufferedReader input = null;
        try {
            input = new BufferedReader(new InputStreamReader(p.getInputStream()));

            String [] headerArray = {CmdOutHeaderConstants.HPUX_PID, CmdOutHeaderConstants.HPUX_CPU, CmdOutHeaderConstants.HPUX_SIZE, CmdOutHeaderConstants.HPUX_COMMAND};
            Map<String, Integer> headerPositions = processHeaderLine(cmd, input.readLine(), headerArray, "\\s+");
            String line;
            while ((line = input.readLine()) != null) {
                String[] words = line.trim().split("\\s+");
                if(words.length == headerPositions.size()) {
                    int pid = Integer.parseInt(words[headerPositions.get(CmdOutHeaderConstants.HPUX_PID)].trim());
                    String processName = deriveProcessNameFromCommand(words[headerPositions.get(CmdOutHeaderConstants.HPUX_COMMAND)].trim());
                    BigDecimal cpuUtilizationInPercent = toBigDecimal(words[headerPositions.get(CmdOutHeaderConstants.HPUX_CPU)].trim());
                    BigDecimal absoluteMemUsedInMB = toBigDecimal(words[headerPositions.get(CmdOutHeaderConstants.HPUX_SIZE)].trim()).divide(BYTES_CONVERSION_FACTOR);
                    BigDecimal memUtilizationInPercent = (absoluteMemUsedInMB.divide(getTotalMemSizeMB(), BigDecimal.ROUND_HALF_UP)).multiply(new BigDecimal(100));
                    populateProcessData(processName, pid, cpuUtilizationInPercent, memUtilizationInPercent, absoluteMemUsedInMB);
                }
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

    private String deriveProcessNameFromCommand(String commandString) {
        String[] array = commandString.split("/");
        return array [array.length - 1];
    }

    public void fetchMemory() throws ProcessMonitorException, CommandExecutorException {
        String cmd = ProcessCommands.HPUX_TOP_COMMAND;
        Process p = commandExecutor.execute(cmd);
        BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
        try {
            input = new BufferedReader(new InputStreamReader(p.getInputStream()));
            skipParsingLines(input, 6);
            processMemoryLine(input.readLine());
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

    public BigDecimal parseMemoryString(String valueStr) {
        if (!Strings.isNullOrEmpty(valueStr)) {
            String strippedValueStr = "";
            try {
                if (valueStr.endsWith("K")) {
                    strippedValueStr = valueStr.split("K")[0].trim();
                    return toBigDecimal(unLocalizeStrValue(strippedValueStr)).divide(BYTES_CONVERSION_FACTOR);
                } else if (valueStr.contains("M")) {
                    strippedValueStr = valueStr.split("M")[0].trim();
                    return toBigDecimal(unLocalizeStrValue(strippedValueStr));
                } else if (valueStr.contains("G")) {
                    strippedValueStr = valueStr.split("G")[0].trim();
                    return toBigDecimal(unLocalizeStrValue(strippedValueStr)).multiply(BYTES_CONVERSION_FACTOR);
                }
            } catch (Exception e) {
                logger.error("Unrecognized string format: " + valueStr);
            }
        }
        return null;
    }

    private void processMemoryLine(String line) throws ProcessMonitorException {
        try {
            String memoryToken = line.trim().split(",")[0].trim().split(":")[1].trim().split("\\s+")[0].trim();
            BigDecimal memory = parseMemoryString(memoryToken);
            setTotalMemSizeMB(memory);
        } catch (Exception e) {
            logger.error("Couldn't parse " + line + " for memory retrieval");
            throw new ProcessMonitorException("Couldn't parse " + line + " for memory retrieval");
        }
    }

    private static String unLocalizeStrValue(String valueStr) {
        try {
            Locale loc = Locale.getDefault();
            return NumberFormat.getInstance(loc).parse(valueStr).toString();
        } catch (ParseException e) {
            logger.error("Exception while unlocalizing number string " + valueStr, e);
        }
        return null;
    }
}
