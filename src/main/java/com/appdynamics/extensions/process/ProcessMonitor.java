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
package com.appdynamics.extensions.process;

import com.appdynamics.extensions.ABaseMonitor;
import com.appdynamics.extensions.TasksExecutionServiceProvider;
import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.appdynamics.extensions.util.AssertUtils;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.appdynamics.extensions.process.common.MonitorConstants.*;

public class ProcessMonitor extends ABaseMonitor {

    private static final Logger logger = ExtensionsLoggerFactory.getLogger(ProcessMonitor.class);

    private String os;

    @Override
    protected void doRun(TasksExecutionServiceProvider tasksExecutionServiceProvider) {
        if (determineValidOS()) {
            logger.debug("OS of the System detected: " + os);
            ProcessMonitorTask processMonitorTask = new ProcessMonitorTask(getContextConfiguration(), tasksExecutionServiceProvider.getMetricWriteHelper(), os);
            tasksExecutionServiceProvider.submit(null, processMonitorTask);
        } else {
            logger.error("Your OS (" + os + ") is not supported by this extension");
        }
    }

    private boolean determineValidOS() {
        os = System.getProperty(OS_NAME).trim().toLowerCase();
        if (!(os.contains(WINDOWS) || os.contains(LINUX) || os.contains(SUNOS) || os.contains(AIX) || os.contains(HP_UX))) {
            return false;
        }
        return true;
    }

    @Override
    protected String getDefaultMetricPrefix() {
        return CUSTOM_METRICS + METRIC_SEPARATOR + MONITOR_NAME;
    }

    @Override
    public String getMonitorName() {
        return MONITOR_NAME;
    }

    protected List<Map<String, ?>> getServers() {
        List<Map<String, ?>> servers = (List<Map<String, ?>>) getContextConfiguration().getConfigYml().get(INSTANCES);
        List<Map<String, ?>> oneServer = new ArrayList<>();
        oneServer.add(servers.get(0));
        AssertUtils.assertNotNull(oneServer, "The 'instances' section in config.yml is not initialised");
        return oneServer;
    }
}
