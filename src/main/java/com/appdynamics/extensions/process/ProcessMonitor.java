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
package com.appdynamics.extensions.process;

import com.appdynamics.extensions.PathResolver;
import com.appdynamics.extensions.process.config.Configuration;
import com.appdynamics.extensions.process.parser.AIXParser;
import com.appdynamics.extensions.process.parser.HPUXParser;
import com.appdynamics.extensions.process.parser.LinuxParser;
import com.appdynamics.extensions.process.parser.Parser;
import com.appdynamics.extensions.process.parser.SolarisParser;
import com.appdynamics.extensions.process.parser.WindowsParser;
import com.appdynamics.extensions.process.processdata.ProcessData;
import com.appdynamics.extensions.process.processexception.ProcessMonitorException;
import com.appdynamics.extensions.yml.YmlReader;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.apache.log4j.Logger;

import java.io.File;
import java.math.BigDecimal;
import java.util.Map;

public class ProcessMonitor extends AManagedMonitor {

    private Parser parser;

    private static final Logger logger = Logger.getLogger(ProcessMonitor.class);
    public static final String CONFIG_ARG = "config-file";
    public static final String METRIC_SEPARATOR = "|";

    public ProcessMonitor() {
        String msg = "Using Monitor Version [" + getImplementationVersion() + "]";
        logger.info(msg);
        System.out.println(msg);
    }

    public TaskOutput execute(Map<String, String> taskArguments, TaskExecutionContext taskContext) throws TaskExecutionException {
        if (taskArguments != null) {
            logger.info("Starting the Process Monitoring task.");
            String configFilename = getConfigFilename(taskArguments.get(CONFIG_ARG));
            try {
                Configuration config = YmlReader.readFromFile(configFilename, Configuration.class);
                if (logger.isDebugEnabled()) {
                    logConfigInfo(config);
                }
                determineOS(config);
                parser.parseProcesses();
                printAllMetrics(config);
                logger.info("Process monitoring task completed successfully.");
                return new TaskOutput("Process monitoring task completed successfully.");
            } catch (Exception e) {
                logger.error("Process Monitoring Task has failed with exception: ", e);
            }
        }
        throw new TaskExecutionException("Process Monitor completed with failures");

    }

    private void determineOS(Configuration config) throws ProcessMonitorException {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            parser = new WindowsParser(config);
            logger.debug("OS System detected: Windows");
        } else if (os.contains("linux")) {
            parser = new LinuxParser(config);
            logger.debug("OS System detected: Linux");
        } else if (os.contains("sunos")) {
            parser = new SolarisParser(config);
            logger.debug("OS System detected: Solaris");
        } else if (os.contains("aix")) {
            parser = new AIXParser(config);
            logger.debug("OS System detected: IBM AIX");
        } else if (os.contains("hp-ux")) {
            parser = new HPUXParser(config);
            logger.debug("OS System detected: HP-UX");
        } else {
            logger.error("Your OS (" + os + ") is not supported. Quitting Process Monitor");
            throw new ProcessMonitorException("Your OS (" + os + ") is not supported. Quitting Process Monitor");
        }

    }

    private void logConfigInfo(Configuration config) {
        String exclProcs = "";
        String exclPIDs = "";
        logger.debug("Display by PID " + config.isDisplayByPid());
        logger.debug("Memory Threshold:        " + config.getMemoryThreshold() + " MB");
        for (String pr : config.getExcludeProcesses()) {
            exclProcs = exclProcs.concat(pr + ", ");
        }
        for (int pid : config.getExcludePIDs()) {
            exclPIDs = exclPIDs.concat(pid + ", ");
        }
        if (!"".equals(exclProcs)) {
            logger.debug("Ignoring processes: " + exclProcs.substring(0, exclProcs.length() - 2));
        }
        if (!"".equals(exclPIDs)) {
            logger.debug("Ignoring pids: " + exclPIDs.substring(0, exclPIDs.length() - 2));
        }
    }

    private void printAllMetrics(Configuration config) {

        logger.debug("This round of metric collection done. Starting to report metrics...");

        logger.debug("Reading in the set of monitored processes");
        parser.readProcsFromFile();
        for (ProcessData procData : parser.getProcesses().values()) {

            if (procData.absoluteMem.doubleValue() >= parser.getMemoryThreshold() || parser.getIncludeProcesses().contains(procData.name)) {

                parser.addIncludeProcesses(procData.name);

                StringBuilder metricPath = new StringBuilder(config.getMetricPrefix()).append(parser.processGroupName).append(METRIC_SEPARATOR);
                metricPath.append(procData.name).append(METRIC_SEPARATOR);

                printMetric(metricPath.toString() + "CPU Utilization in Percent", convertBigDecimalToString(procData.CPUPercent));
                printMetric(metricPath.toString() + "Memory Utilization in Percent", convertBigDecimalToString(procData.memPercent));
                printMetric(metricPath.toString() + "Memory Utilization Absolute (MB)", convertBigDecimalToString(procData.absoluteMem));
                if (!config.isDisplayByPid()) {
                    printMetric(metricPath.toString() + "Number of running instances", procData.numOfInstances);
                }

            }
        }
        logger.debug("Writing monitored processes out to file");
        parser.writeProcsToFile();
        logger.debug("Finished reporting metrics");
    }

    /**
     * Returns the metric to the AppDynamics Controller.
     *
     * @param metricName  Name of the Metric
     * @param metricValue Value of the Metric
     */
    public void printMetric(String metricName, Object metricValue) {
        MetricWriter metricWriter = getMetricWriter(metricName, MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION,
                MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE, MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_INDIVIDUAL);
        if (metricValue != null) {
            metricWriter.printMetric(String.valueOf(metricValue));
            if (logger.isDebugEnabled()) {
                logger.debug(metricName + " = " + metricValue);
            }
        }
    }

    private String convertBigDecimalToString(BigDecimal value) {
        return value.setScale(0, BigDecimal.ROUND_HALF_UP).toString();
    }

    public static String getConfigFilename(String filename) {
        if (filename == null) {
            return "";
        }
        // for absolute paths
        if (new File(filename).exists()) {
            return filename;
        }
        // for relative paths
        File jarPath = PathResolver.resolveDirectory(AManagedMonitor.class);
        String configFileName = "";
        if (!Strings.isNullOrEmpty(filename)) {
            configFileName = jarPath + File.separator + filename;
        }
        return configFileName;
    }

    private static String getImplementationVersion() {
        return ProcessMonitor.class.getPackage().getImplementationTitle();
    }
}
