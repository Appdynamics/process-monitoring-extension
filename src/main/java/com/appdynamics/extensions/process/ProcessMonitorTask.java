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

import com.appdynamics.extensions.AMonitorTaskRunnable;
import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.conf.MonitorConfiguration;
import com.appdynamics.extensions.metrics.Metric;
import com.appdynamics.extensions.process.common.MonitorConstants;
import com.appdynamics.extensions.process.data.ProcessData;
import com.appdynamics.extensions.process.parser.Parser;
import com.appdynamics.extensions.process.parser.ParserFactory;
import com.google.common.collect.Lists;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.Map;

public class ProcessMonitorTask implements AMonitorTaskRunnable {

    private static final Logger logger = Logger.getLogger(ProcessMonitorTask.class);
    private MonitorConfiguration monitorConfiguration;
    private MetricWriteHelper metricWriteHelper;
    private String os;

    public ProcessMonitorTask(MonitorConfiguration monitorConfiguration, MetricWriteHelper metricWriteHelper, String os) {
        this.monitorConfiguration = monitorConfiguration;
        this.metricWriteHelper = metricWriteHelper;
        this.os = os.toLowerCase();
    }
    public void run() {
        try {
            logger.debug("ProcessMonitorTask::run Started");
            Map<String, ?> config = monitorConfiguration.getConfigYml();
            Parser parser = ParserFactory.createParser(os);
            if (parser != null) {
                Map<String, ProcessData> processDataMap = parser.fetchMetrics(config);
                String metricPrefix = new StringBuilder(monitorConfiguration.getMetricPrefix()).append(MonitorConstants.METRIC_SEPARATOR).append(parser.getProcessGroupName()).append(MonitorConstants.METRIC_SEPARATOR).toString();
                List<Metric> metrics = buildMetrics(metricPrefix, processDataMap, config);
                logger.debug("Metrics size is " + metrics.size());
                metricWriteHelper.transformAndPrintMetrics(metrics);
            } else {
                logger.debug("No matching parser found for this OS:" + os);
            }
        } catch (Exception e) {
            logger.error("Exception in ProcessMonitorTask: ", e);
        }
    }

    public List<Metric> buildMetrics(String metricPrefix, Map<String, ProcessData> processDataMap, Map<String, ?> config) {
        List<Metric> metrics = Lists.newArrayList();
        List<Map<String, ?>> metricsFromConfig = (List<Map<String, ?>>) config.get("metrics");
        // Iterate through fetched metrics
        for (Map.Entry<String, ProcessData> entry : processDataMap.entrySet()) {
            String processName = entry.getKey();
            Map<String, String> processMetrics = entry.getValue().getProcessMetrics();
            // iterate through metrics configuration in config.yml
            for (Map<String, ?> metricFromConfig : metricsFromConfig) {
                String metricNameFromConfig = metricFromConfig.entrySet().iterator().next().getKey();
                Map<String, ?> metricModifierMap = (Map<String, ?>) metricFromConfig.get(metricNameFromConfig);
                if (processMetrics.containsKey(metricNameFromConfig)) {
                    String finalMetricPath = metricPrefix + MonitorConstants.METRIC_SEPARATOR + processName + MonitorConstants.METRIC_SEPARATOR + metricNameFromConfig;
                    Metric metric;
                    if (metricModifierMap != null) {
                        metric = new Metric(metricNameFromConfig, processMetrics.get(metricNameFromConfig), finalMetricPath, metricModifierMap);
                    } else {
                        metric = new Metric(metricNameFromConfig, processMetrics.get(metricNameFromConfig), finalMetricPath);
                    }
                    metrics.add(metric);
                }
            }
        }
        return metrics;
    }

    public void onTaskComplete() {
        logger.info("Process Monitoring Extension Task completed");
    }
}
