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

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.apache.log4j.Logger;

import com.appdynamics.extensions.PathResolver;
import com.appdynamics.extensions.process.config.ConfigUtil;
import com.appdynamics.extensions.process.config.Configuration;
import com.appdynamics.extensions.process.parser.LinuxParser;
import com.appdynamics.extensions.process.parser.Parser;
import com.appdynamics.extensions.process.parser.WindowsParser;
import com.appdynamics.extensions.process.processdata.ProcessData;
import com.appdynamics.extensions.process.processexception.ProcessMonitorException;
import com.google.common.base.Strings;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;

public class ProcessMonitor extends AManagedMonitor {

	private Parser parser;

	private final int REPORT_INTERVAL_SECS = 60;
	private int fetchesPerInterval;

	private static final Logger logger = Logger.getLogger("com.singularity.extensions.ProcessMonitor");
	public static final String CONFIG_ARG = "config-file";
	public static final String METRIC_SEPARATOR = "|";

	// To load the config files
	private final static ConfigUtil<Configuration> configUtil = new ConfigUtil<Configuration>();

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
				Configuration config = configUtil.readConfig(configFilename, Configuration.class);
				fetchesPerInterval = config.getFetchesPerInterval() == 0 ? 2 : config.getFetchesPerInterval();
				determineOS(config);

				parser.retrieveMemoryMetrics();

				printPropertiesInfo(config);

				// working with threads to ensure a more accurate sleep time.
				CountDownLatch latch = new CountDownLatch(fetchesPerInterval);
				logger.info("New round of metric collection started");
				for (int i = 0; i < fetchesPerInterval; i++) {
					Thread.sleep(REPORT_INTERVAL_SECS / (fetchesPerInterval + 2) * 1000);
					Thread parseThread = new ParseThread(latch);
					parseThread.start();
				}
				latch.await();
				printAllMetrics(config);
				logger.info("Process monitoring task completed successfully.");
				return new TaskOutput("Process monitoring task completed successfully.");
			} catch (FileNotFoundException e) {
				logger.error("Config file not found :: " + configFilename, e);
			} catch (Exception e) {
				logger.error("Process Monitoring Task has failed with exception: ", e);
			}
		}
		throw new TaskExecutionException("Process Monitor completed with failures");

	}

	private void determineOS(Configuration config) throws ProcessMonitorException {
		String os = System.getProperty("os.name").toLowerCase();
		if (os.contains("win")) {
			parser = new WindowsParser(config, REPORT_INTERVAL_SECS, fetchesPerInterval);
			logger.debug("OS System detected: Windows");
		} else if (os.contains("linux")) {
			parser = new LinuxParser(config);
			logger.debug("OS System detected: Linux");
		} else {
			logger.error("Your OS (" + os + ") is not supported. Quitting Process Monitor");
			throw new ProcessMonitorException("Your OS (" + os + ") is not supported. Quitting Process Monitor");
		}
	}

	/**
	 * prints info to logger about parsed properties.xml
	 * 
	 * @param config
	 */
	private void printPropertiesInfo(Configuration config) {
		String exclProcs = "";
		String exclPIDs = "";
		logger.info("Detected total RAM size: " + parser.getTotalMemSizeMB() + " MB");
		logger.debug("Memory Threshold:        " + parser.getMemoryThreshold() + " MB");
		for (String pr : config.getExcludeProcesses()) {
			exclProcs = exclProcs.concat(pr + ", ");
		}
		for (int pid : config.getExcludePIDs()) {
			exclPIDs = exclPIDs.concat(pid + ", ");
		}
		if (!"".equals(exclProcs)) {
			logger.debug("Ignoring processes:      " + exclProcs.substring(0, exclProcs.length() - 2));
		}
		if (!"".equals(exclPIDs)) {
			logger.debug("Ignoring pids:           " + exclPIDs.substring(0, exclPIDs.length() - 2));
		}
	}

	private void printAllMetrics(Configuration config) {

		logger.debug("This round of metric collection done. Starting to report metrics...");

		logger.debug("Reading in the set of monitored processes");
		parser.readProcsFromFile();
		for (ProcessData procData : parser.getProcesses().values()) {
			float absoluteMem = (procData.memPercent / 100 * parser.getTotalMemSizeMB()) / fetchesPerInterval;

			if (absoluteMem >= parser.getMemoryThreshold() || parser.getIncludeProcesses().contains(procData.name)) {

				parser.addIncludeProcesses(procData.name);

				int cpuPercent = (int) (procData.CPUPercent / fetchesPerInterval);
				int memPercent = (int) (procData.memPercent / fetchesPerInterval);
				int memAbsolute = (int) (Math.round(absoluteMem));
				//int numOfInst = procData.numOfInstances / fetchesPerInterval;

				StringBuilder metricPath = new StringBuilder(config.getMetricPrefix()).append(parser.processGroupName).append(METRIC_SEPARATOR);
				
				printMetric(metricPath.toString() + procData.name + "|CPU Utilization in Percent", cpuPercent);
				printMetric(metricPath.toString() + procData.name + "|Memory Utilization in Percent", memPercent);
				printMetric(metricPath.toString() + procData.name + "|Memory Utilization Absolute (MB)", memAbsolute); 
				//printMetric(metricPath.toString() + procData.name + "|Number of running instances", numOfInst);
			}
		}
		logger.debug("Writing monitored processes out to file");
		parser.writeProcsToFile();
		logger.debug("Finished reporting metrics");
	}

	/**
	 * Returns the metric to the AppDynamics Controller.
	 * 
	 * @param metricName
	 *            Name of the Metric
	 * @param metricValue
	 *            Value of the Metric
	 * @param aggregation
	 *            Average OR Observation OR Sum
	 * @param timeRollup
	 *            Average OR Current OR Sum
	 * @param cluster
	 *            Collective OR Individual
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

	private class ParseThread extends Thread {

		private CountDownLatch latch;

		public ParseThread(CountDownLatch latch) {
			this.latch = latch;
		}

		public void run() {
			try {
				parser.parseProcesses();
			} catch (Exception e) {
				logger.error(e);
			} finally {
				latch.countDown();
			}
		}
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
