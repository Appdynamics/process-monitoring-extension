/*
 * Copyright 2020 AppDynamics LLC and its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.appdynamics.extensions.process.parser;

import com.appdynamics.extensions.process.common.MonitorConstants;
import com.appdynamics.extensions.process.data.ProcessData;
import com.google.common.base.Strings;

import java.util.Map;

import static com.appdynamics.extensions.process.common.MonitorConstants.HP_UX;
import static com.appdynamics.extensions.process.common.MonitorConstants.PROCESS;

public class HPUXParser extends Parser {
    public Map<String, ProcessData> fetchMetrics(Map<String, ?> config) {
        String[] envp = {"UNIX95= "};
        return fetchMetrics(config, getProcessListCommand(getCommands(config)), envp);
    }

    public String getProcessGroupName() {
        return "HP-UX Processes";
    }

    private String getProcessListCommand(Map<String, String> commands) {
        String cmd;
        if (commands != null && !Strings.isNullOrEmpty(commands.get(PROCESS))) {
            cmd = commands.get(PROCESS);
        } else {
            cmd = MonitorConstants.HP_UX_PROCESS_LIST_COMMAND;
        }
        return cmd;
    }
    protected Map<String, String> getCommands(Map<String, ?> config) {
        return (Map) config.get(HP_UX);
    }
}
