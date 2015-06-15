/**
 * Copyright 2015 AppDynamics
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
package com.appdynamics.extensions.process.config;

import java.util.HashSet;
import java.util.Set;

public class Configuration {

    public static final String DEFAULT_CSV_FILE_PATH = "csv";

    private boolean displayByPid;
    private Set<String> excludeProcesses = new HashSet<String>();
    private Set<Integer> excludePIDs = new HashSet<Integer>();
    private int memoryThreshold;
    private String csvFilePath;
    private String monitoredProcessFilePath;
    private String metricPrefix;

    public boolean isDisplayByPid() {
        return displayByPid;
    }

    public void setDisplayByPid(boolean displayByPid) {
        this.displayByPid = displayByPid;
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

    public String getCsvFilePath() {
        if (this.csvFilePath == null || this.csvFilePath.equals("")) {
            return DEFAULT_CSV_FILE_PATH;
        }

        return csvFilePath;
    }

    public void setCsvFilePath(String csvFilePath) {
        this.csvFilePath = csvFilePath != null ? csvFilePath.trim() : csvFilePath;
    }

}
