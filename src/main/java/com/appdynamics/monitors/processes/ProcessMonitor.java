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




package com.appdynamics.monitors.processes;

import java.util.Map;

import org.apache.log4j.Logger;
import org.dom4j.DocumentException;

import com.appdynamics.monitors.processes.parser.LinuxParser;
import com.appdynamics.monitors.processes.parser.Parser;
import com.appdynamics.monitors.processes.parser.WindowsParser;
import com.appdynamics.monitors.processes.processdata.ProcessData;
import com.appdynamics.monitors.processes.processexception.ProcessMonitorException;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;

public class ProcessMonitor extends AManagedMonitor{

	Parser parser;

	private final int REPORT_INTERVAL_SECS = 60;	
	private final int FETCHES_PER_INTERVAL = 10;
	
	private String metricPath = "Custom Metrics|";

	private boolean running;
	
	Logger logger;
	
	public static void main(String[] args){
		System.out.println("running");
	}


	/**
	 * prints info to logger about parsed properties.xml
	 */
	private void printPropertiesInfo(){
		String exclProcs = "";
		String exclPIDs = "";
		logger.info("Detected total RAM size: " + parser.getTotalMemSizeMB() + " MB");
		logger.info("Memory Threshold:        " + parser.getMemoryThreshold() + " MB");
		for(String pr : parser.getExcludeProcesses()){
			exclProcs = exclProcs.concat(pr + ", ");
		}
		for(int pid : parser.getExcludePIDs()){
			exclPIDs = exclPIDs.concat(pid + ", ");
		}
		if(!exclProcs.isEmpty()){
			logger.info("Ignoring processes:      " + exclProcs.substring(0, exclProcs.length() - 2));
		}
		if(!exclPIDs.isEmpty()){
			logger.info("Ignoring pids:           " + exclPIDs.substring(0, exclPIDs.length() - 2));
		}
	}


	@Override
	public TaskOutput execute(Map<String, String> taskArguments,
			TaskExecutionContext taskContext) throws TaskExecutionException {
		try {

			String os = System.getProperty("os.name").toLowerCase();
			logger = Logger.getLogger(ProcessMonitor.class);	
			running = true;
			
			logger.debug("Process Monitor in Debug mode started.");

			if(os.contains("win")){
				parser = new WindowsParser(logger, REPORT_INTERVAL_SECS, FETCHES_PER_INTERVAL);
				logger.debug("OS System detected: Windows");
			} else if(os.contains("linux")){
				parser = new LinuxParser(logger);
				logger.debug("OS System detected: Linux");
			} else {
				logger.error("Your OS (" + os + ") is not supported. Quitting Process Monitor");
				return null;
			}			

			if(!taskArguments.containsKey("properties-path")){
				logger.error("monitor.xml needs to contain a task-argument 'properties-path' describing" +
						"the path to the xml-file with the properties. Quitting Process Monitor");
				return null;
			}
			
			if(taskArguments.containsKey("metric-path") && !taskArguments.get("metric-path").equals("")){
				metricPath = taskArguments.get("metric-path");
				logger.debug("Metric path: " + metricPath);
				if(!metricPath.endsWith("|")){
					metricPath += "|";
				}
			}

			parser.initialize();
			try{
				parser.parseXML(taskArguments.get("properties-path"));
				logger.debug("finished reading properties.xml at " + taskArguments.get("properties-path"));
			} catch (DocumentException e) {
				logger.error("Unable to read " + taskArguments.get("properties-path") + ". setting properties to default values" +
						"\nError message: " + e.getMessage());
				parser.setMemoryThreshold(parser.getDefaultMemoryThreshold());
			}

			printPropertiesInfo();

			// working with threads to ensure a more accurate sleep time.
			while(running) {
				
				logger.debug("New round of metric collection started");
				for(int i = 0; i < FETCHES_PER_INTERVAL; i++){
					Thread.sleep(REPORT_INTERVAL_SECS / FETCHES_PER_INTERVAL * 1000);
					(new ParseThread()).start();					
				}
				
				(new PrintMetricsClearHashmapThread()).start();
				

			}

		} catch (InterruptedException e) {
			logger.error("Process Monitor interrupted. Quitting Monitor.");
			return null;
		} catch (ProcessMonitorException e) {
			logger.error(e.getMessage());
			return null;
		}
		
		return null;
	}

	private void printAllMetrics(){
		
		logger.debug("This round of metric collection done. Starting to report metrics...");
		
		for(ProcessData procData : parser.getProcesses().values()){

			float absoluteMem = (procData.memPercent/100 * parser.getTotalMemSizeMB()) / FETCHES_PER_INTERVAL;
	
			if(absoluteMem >= parser.getMemoryThreshold() || parser.getIncludeProcesses().contains(procData.name)){
				
				parser.addIncludeProcesses(procData.name);
				
				int cpuPercent = (int) (procData.CPUPercent / FETCHES_PER_INTERVAL);
				int memPercent = (int) (procData.memPercent / FETCHES_PER_INTERVAL);
				int memAbsolute = (int) (Math.round(absoluteMem));
				int numOfInst = procData.numOfInstances / FETCHES_PER_INTERVAL;

				printMetric(procData.name + "|CPU Utilization in Percent", cpuPercent,
						MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION,
						MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE,
						MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_INDIVIDUAL);
				
				logger.debug("Metric reported: " + procData.name + ", CPU Utilization in Percent: " + cpuPercent);

				printMetric(procData.name + "|Memory Utilization in Percent", memPercent,
						MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION,
						MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE,
						MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_INDIVIDUAL);

				logger.debug("Metric reported: " + procData.name + ", Memory Utilization in Percent: " + memPercent);
				
				printMetric(procData.name + "|Memory Utilization Absolute (MB)", memAbsolute, // divided through above
						MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION,
						MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE,
						MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_INDIVIDUAL);

				logger.debug("Metric reported: " + procData.name + ", Memory Utilization Absolute (MB): " + memAbsolute);
				
				printMetric(procData.name + "|Number of running instances", numOfInst,
						MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION,
						MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE,
						MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_INDIVIDUAL);
				
				logger.debug("Metric reported: " + procData.name + ", Number of running instances: " + numOfInst);


			}

		}
		
		logger.debug("Finished reporting metrics");
	}



	/**
	 * Returns the metric to the AppDynamics Controller.
	 * @param 	metricName		Name of the Metric
	 * @param 	metricValue		Value of the Metric
	 * @param 	aggregation		Average OR Observation OR Sum
	 * @param 	timeRollup		Average OR Current OR Sum
	 * @param 	cluster			Collective OR Individual
	 */
	public void printMetric(String metricName, Object metricValue, String aggregation, String timeRollup, String cluster)
	{
		MetricWriter metricWriter = getMetricWriter(metricPath + parser.processGroupName + "|" + metricName, 
				aggregation,
				timeRollup,
				cluster
				);

		metricWriter.printMetric(String.valueOf(metricValue));
	}

	private class ParseThread extends Thread{
		public void run(){
			try {
				parser.parseProcesses();
			}  catch (ProcessMonitorException e) {
				logger.error(e.getMessage());
				running = false;
			}				
		}
	}

	private class PrintMetricsClearHashmapThread extends Thread{
		public void run(){
			printAllMetrics();		
			
			parser.getProcesses().clear();
		}
	}

}
