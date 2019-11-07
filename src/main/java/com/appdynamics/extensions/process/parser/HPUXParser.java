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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HPUXParser extends Parser {
    public Map<String, ProcessData> fetchMetrics(Map<String, ?> config) {
        List<String> env = new ArrayList<String>();
        env.add("UNIX95=\"\"");
        return fetchMetrics(config, getProcessListCommand(getCommands(config)), env);
    }

    private String getProcessListCommand(Map<String, String> commands) {
        String cmd;
        if (commands != null && !Strings.isNullOrEmpty(commands.get("process"))) {
            cmd = commands.get("process");
        } else {
            cmd = MonitorConstants.HPUX_PROCESS_COMMAND;
        }
        return cmd;
    }

    public String getProcessGroupName() {
        return "HP-UX Processes";
    }

    protected Map<String, String> getCommands(Map<String, ?> config) {
        return (Map) config.get("hpux");
    }
}
