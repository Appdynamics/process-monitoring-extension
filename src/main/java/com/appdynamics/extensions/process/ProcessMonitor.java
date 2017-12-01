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
package com.appdynamics.extensions.process;

import com.appdynamics.extensions.ABaseMonitor;
import com.appdynamics.extensions.TasksExecutionServiceProvider;
import com.appdynamics.extensions.conf.MonitorConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessMonitor extends ABaseMonitor {

    private static final Logger logger = LoggerFactory.getLogger(ProcessMonitor.class);

    private String os;

    protected void doRun(TasksExecutionServiceProvider tasksExecutionServiceProvider) {
        ProcessMonitorTask processMonitorTask = new ProcessMonitorTask(configuration, tasksExecutionServiceProvider.getMetricWriteHelper(), os);
        tasksExecutionServiceProvider.submit(null, processMonitorTask);
    }

    @Override
    protected void initializeMoreStuff(MonitorConfiguration conf) {
        super.initializeMoreStuff(conf);
        determineOS();
    }

    public void determineOS() {
        os = System.getProperty("os.name").toLowerCase();
        if (!(os.contains("win") || os.contains("linux") || os.contains("sunos") || os.contains("aix") || os.contains("hp-ux"))) {
            logger.error("Your OS (" + os + ") is not supported by this extension");
        } else {
            logger.debug("OS of the System detected: " + os);
        }
    }

    protected String getDefaultMetricPrefix() {
        return "Custom Metrics|Process Monitor|";
    }

    public String getMonitorName() {
        return "Process Monitoring Extension";
    }

    protected int getTaskCount() {
        return 1;
    }
}
