package com.appdynamics.extensions.process.config;

import java.util.HashSet;
import java.util.Set;

public class Configuration {
	
	private int fetchesPerInterval;
	private Set<String> excludeProcesses = new HashSet<String>();
	private Set<Integer> excludePIDs = new HashSet<Integer>();
	private int memoryThreshold;
	private String monitoredProcessFilePath;
	private String metricPrefix;

	public int getFetchesPerInterval() {
		return fetchesPerInterval;
	}

	public void setFetchesPerInterval(int fetchesPerInterval) {
		this.fetchesPerInterval = fetchesPerInterval;
	}

	public Set<String> getExcludeProcesses() {
		return excludeProcesses;
	}

	public void setExcludeProcesses(Set<String> excludeProcesses) {
		this.excludeProcesses = excludeProcesses;
	}

	public Set<Integer> getExcludePIDs() {
		return excludePIDs;
	}

	public void setExcludePIDs(Set<Integer> excludePIDs) {
		this.excludePIDs = excludePIDs;
	}

	public int getMemoryThreshold() {
		return memoryThreshold;
	}

	public void setMemoryThreshold(int memoryThreshold) {
		this.memoryThreshold = memoryThreshold;
	}

	public String getMonitoredProcessFilePath() {
		return monitoredProcessFilePath;
	}

	public void setMonitoredProcessFilePath(String monitoredProcessFilePath) {
		this.monitoredProcessFilePath = monitoredProcessFilePath;
	}

	public String getMetricPrefix() {
		return metricPrefix;
	}

	public void setMetricPrefix(String metricPrefix) {
		this.metricPrefix = metricPrefix;
	}
	
}
