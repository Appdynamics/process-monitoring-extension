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

import com.appdynamics.extensions.metrics.Metric;
import com.appdynamics.extensions.process.data.ProcessData;
import com.appdynamics.extensions.yml.YmlReader;
import com.google.common.collect.Maps;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class ProcessMonitorTaskTest {

    private ProcessMonitorTask classUnderTest;

    @Before
    public void setup() {
        classUnderTest = Mockito.spy(new ProcessMonitorTask(null, null, null));
    }

    @Test
    public void testBuildMetricsWithNoMatchingMetricsConfig() {
        Map<String, ?> config = YmlReader.readFromFile(new File("src/test/resources/conf/config.yml"));
        Map<String, ProcessData> processDataMap = Maps.newHashMap();
        ProcessData processData = new ProcessData();
        Map<String, String> processMetrics = Maps.newHashMap();
        processMetrics.put("CPU Utilization%", "10");
        processMetrics.put("Memory Utilization%", "30");
        processData.setProcessMetrics(processMetrics);
        processDataMap.put("Java", processData);

        List<Metric> metrics = classUnderTest.buildMetrics("Linux Processes", processDataMap, config);
        Assert.assertTrue(metrics.isEmpty());
    }

    @Test
    public void testBuildMetricsWithImproperMetricsConfig() {
        Map<String, ?> config = YmlReader.readFromFile(new File("src/test/resources/conf/config.yml"));
        Map<String, ProcessData> processDataMap = Maps.newHashMap();
        ProcessData processData = new ProcessData();
        Map<String, String> processMetrics = Maps.newHashMap();
        processMetrics.put("CPU%", "10");
        processMetrics.put("MEM%", "30");
        processData.setProcessMetrics(processMetrics);
        processDataMap.put("Java", processData);

        List<Metric> metrics = classUnderTest.buildMetrics("Linux Processes", processDataMap, config);
        Assert.assertEquals(1, metrics.size());
        Assert.assertEquals("Linux Processes|Java|CPU%", metrics.get(0).getMetricPath());
        Assert.assertEquals(String.valueOf(10), metrics.get(0).getMetricValue());
        Assert.assertEquals("INDIVIDUAL", metrics.get(0).getMetricProperties().getClusterRollUpType());
        Assert.assertEquals("SUM", metrics.get(0).getMetricProperties().getAggregationType());
        Assert.assertEquals(new BigDecimal(1), metrics.get(0).getMetricProperties().getMultiplier());
    }

    @Test
    public void testBuildMetricsWithProperMetricsConfig() {
        Map<String, ?> config = YmlReader.readFromFile(new File("src/test/resources/conf/config.yml"));
        Map<String, ProcessData> processDataMap = Maps.newHashMap();
        ProcessData processData = new ProcessData();
        Map<String, String> processMetrics = Maps.newHashMap();
        processMetrics.put("CPU%", "10");
        processMetrics.put("Memory%", "30");
        processMetrics.put("RSS", "30");
        processMetrics.put("Running Instances", "2");
        processData.setProcessMetrics(processMetrics);
        processDataMap.put("Java", processData);
        processDataMap.put("SQL", processData);

        List<Metric> metrics = classUnderTest.buildMetrics("Linux Processes", processDataMap, config);
        Assert.assertEquals(8, metrics.size());
    }

    @Test
    public void testBuildMetricsWithExcessMetricsThanMetricsConfig() {
        Map<String, ?> config = YmlReader.readFromFile(new File("src/test/resources/conf/config.yml"));
        Map<String, ProcessData> processDataMap = Maps.newHashMap();
        ProcessData processData = new ProcessData();
        Map<String, String> processMetrics = Maps.newHashMap();
        processMetrics.put("CPU%", "10");
        processMetrics.put("Memory%", "30");
        processMetrics.put("RSS", "30");
        processMetrics.put("Running Instances", "2");
        processMetrics.put("VSZ", "10");
        processData.setProcessMetrics(processMetrics);
        processDataMap.put("Java", processData);

        List<Metric> metrics = classUnderTest.buildMetrics("Linux Processes", processDataMap, config);
        Assert.assertEquals(4, metrics.size());
    }

}
