/*
 * Copyright 2016. AppDynamics LLC and its affiliates.
 *  All Rights Reserved.
 *  This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.process.parser;

import com.appdynamics.extensions.process.common.MonitorConstants;
import com.appdynamics.extensions.process.data.ProcessData;
import com.google.common.base.Strings;
import org.apache.log4j.Logger;

import java.util.Map;

public class SolarisParser extends Parser {
    public static final Logger logger = Logger.getLogger(LinuxParser.class);

    public Map<String, ProcessData> fetchMetrics(Map<String, ?> config) {
        return fetchMetrics(config, getProcessListCommand(getCommands(config)), null);
    }

    private String getProcessListCommand(Map<String, String> commands) {
        String cmd;
        if (commands != null && !Strings.isNullOrEmpty(commands.get("process"))) {
            cmd = commands.get("process");
        } else {
            cmd = MonitorConstants.SOLARIS_PROCESS_LIST_COMMAND;
        }
        return cmd;
    }

    public String getProcessGroupName() {
        return "Solaris Processes";
    }

    protected Map<String, String> getCommands(Map<String, ?> config) {
        return (Map) config.get("solaris");
    }
}
