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

import com.appdynamics.extensions.conf.MonitorConfiguration;
import com.appdynamics.extensions.process.processexception.ProcessMonitorException;
import com.appdynamics.extensions.util.MetricWriteHelper;
import com.appdynamics.extensions.util.MetricWriteHelperFactory;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.apache.log4j.Logger;

import java.util.Map;

public class ProcessMonitor extends AManagedMonitor {

    private static final Logger logger = Logger.getLogger(ProcessMonitor.class);

    private static final String CONFIG_ARG = "config-file";
    private static final String METRIC_PREFIX = "Custom Metrics|Process Monitor|";
    private boolean initialized;
    private MonitorConfiguration configuration;
    private String os;

    public ProcessMonitor() {
        System.out.println(logVersion());
    }

    private void initialize(Map<String, String> argsMap) throws ProcessMonitorException {
        determineOS();
        MetricWriteHelper metricWriteHelper = MetricWriteHelperFactory.create(this);
        MonitorConfiguration conf = new MonitorConfiguration(METRIC_PREFIX, new TaskRunnable(), metricWriteHelper);
        conf.setConfigYml(argsMap.get(CONFIG_ARG));
        conf.checkIfInitialized(MonitorConfiguration.ConfItem.METRIC_PREFIX, MonitorConfiguration.ConfItem.CONFIG_YML, MonitorConfiguration.ConfItem.METRIC_WRITE_HELPER, MonitorConfiguration.ConfItem.EXECUTOR_SERVICE);
        this.configuration = conf;
        initialized = true;
    }

    public TaskOutput execute(Map<String, String> taskArguments, TaskExecutionContext taskExecutionContext) throws TaskExecutionException {
        logger.info(logVersion());
        logger.debug("The task arguments in monitor.xml are " + taskArguments);
        if (taskArguments != null) {
            try {
                if (!initialized) {
                    initialize(taskArguments);
                }
                configuration.executeTask();
                logger.info("Process monitoring task completed successfully.");
                return new TaskOutput("Process monitoring task completed successfully.");
            } catch (Exception e) {
                logger.error("Process Monitoring Task has failed with exception: ", e);
            }
        }
        throw new TaskExecutionException("Process Monitor completed with failures");
    }

    private class TaskRunnable implements Runnable {

        public void run() {
            ProcessMonitorTask task = new ProcessMonitorTask(configuration, os);
            configuration.getExecutorService().execute(task);
        }
    }

    public void determineOS() throws ProcessMonitorException {
        os = getOSFromSystemProperty();
        if (!(os.contains("win") || os.contains("linux") || os.contains("sunos") || os.contains("aix") || os.contains("hp-ux"))) {
            logger.error("Your OS (" + os + ") is not supported. Quitting Process Monitor");
            throw new ProcessMonitorException("Your OS (" + os + ") is not supported. Quitting Process Monitor");
        }
        logger.debug("OS of the System detected: " + os);
    }

    public String getOSFromSystemProperty() {
        return System.getProperty("os.name").toLowerCase();
    }

    private static String getImplementationVersion() {
        return ProcessMonitor.class.getPackage().getImplementationTitle();
    }

    private String logVersion() {
        String msg = "Using Monitor Version [" + getImplementationVersion() + "]";
        return msg;
    }
}
