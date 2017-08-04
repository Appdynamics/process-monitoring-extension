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
import com.appdynamics.extensions.process.data.ProcessData;
import com.appdynamics.extensions.process.parser.Parser;
import com.appdynamics.extensions.process.parser.ParserFactory;
import org.apache.log4j.Logger;

import java.math.BigDecimal;
import java.util.Map;

public class ProcessMonitorTask implements Runnable {

    private static final Logger logger = Logger.getLogger(ProcessMonitorTask.class);
    private MonitorConfiguration monitorConfiguration;
    private String os;
    public int numberOfMetricsReported;

    public ProcessMonitorTask(MonitorConfiguration monitorConfiguration, String os) {
        this.monitorConfiguration = monitorConfiguration;
        this.os = os;
    }
    public void run() {
        Map<String, ?> config = monitorConfiguration.getConfigYml();
        Parser parser = ParserFactory.createParser(os);
        if (parser != null) {
        Map<String, ProcessData> processDataMap = parser.parseProcesses(config);
        printMetrics(parser, processDataMap);
        }
    }

    private void printMetrics(Parser parser, Map<String, ProcessData> processDataMap) {
        StringBuilder metricPath = new StringBuilder(monitorConfiguration.getMetricPrefix() + "|" + parser.getProcessGroupName()).append("|");
        for (Map.Entry<String, ProcessData> entry : processDataMap.entrySet()) {
            String processName = entry.getKey();
            Map<String, BigDecimal> processMetrics = entry.getValue().getProcessMetrics();
            for (Map.Entry<String, BigDecimal> metric : processMetrics.entrySet()) {
                printMetric(metricPath.toString() + processName + "|" + metric.getKey(), metric.getValue());
            }
        }
        logger.debug("Number of metrics reported by Process Monitor in this iteration " + numberOfMetricsReported);

    }

    protected void printMetric(String metricPath, BigDecimal metricValue) {
        if (metricValue != null) {
            logger.debug("Metric:" + metricPath + ", Raw Value:" + metricValue);
            monitorConfiguration.getMetricWriter().printMetric(metricPath, metricValue, "AVG.AVG.COL");
            numberOfMetricsReported++;
        }
    }
}
