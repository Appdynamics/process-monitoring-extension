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

import com.appdynamics.extensions.controller.apiservices.CustomDashboardAPIService;
import com.appdynamics.extensions.controller.apiservices.MetricAPIService;
import com.appdynamics.extensions.util.JsonUtils;
import org.codehaus.jackson.JsonNode;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class MetricCheckIT {
    private MetricAPIService metricAPIService;
    private CustomDashboardAPIService customDashboardAPIService;

    @Before
    public void setup() {
        metricAPIService = IntegrationTestUtils.initializeMetricAPIService();
        customDashboardAPIService = IntegrationTestUtils.initializeCustomDashboardAPIService();
    }

    @Test
    public void whenInstanceIsUpThenHeartBeatIs1ForServer() {
        JsonNode jsonNode = null;
        if (metricAPIService != null) {
            jsonNode = metricAPIService.getMetricData("",
                    "Server%20&%20Infrastructure%20Monitoring/metric-data?metric-path=Application%20Infrastructure%20Performance%7CRoot%7CCustom%20Metrics%7CProcess%20Monitor%7CHeart%20Beat&time-range-type=BEFORE_NOW&duration-in-mins=60&output=JSON");
        }
        Assert.assertNotNull("Cannot connect to controller API", jsonNode);
        if (jsonNode != null) {
            JsonNode valueNode = JsonUtils.getNestedObject(jsonNode, "*", "metricValues", "*", "value");
            int heartBeat = (valueNode == null) ? 0 : valueNode.get(0).asInt();
            Assert.assertEquals("heartbeat is 0", heartBeat, 1);
        }
    }


    @Test
    public void checkTotalNumberOfMetricsReportedIsGreaterThan1() {
        JsonNode jsonNode = null;
        if (metricAPIService != null) {
            jsonNode = metricAPIService.getMetricData("",
                    "Server%20&%20Infrastructure%20Monitoring/metric-data?metric-path=Application%20Infrastructure%20Performance%7CRoot%7CCustom%20Metrics%7CProcess%20Monitor%7CMetrics%20Uploaded&time-range-type=BEFORE_NOW&duration-in-mins=15&output=JSON");
        }
        Assert.assertNotNull("Cannot connect to controller API", jsonNode);
        if (jsonNode != null) {
            JsonNode valueNode = JsonUtils.getNestedObject(jsonNode, "*", "metricValues", "*", "value");
            int totalNumberOfMetricsReported = (valueNode == null) ? 0 : valueNode.get(0).asInt();
            Assert.assertTrue(totalNumberOfMetricsReported > 1);
        }
    }

    @Test
    public void whenAliasIsAppliedThenCheckMetricName() {
        JsonNode jsonNode = null;
        if (metricAPIService != null) {
            jsonNode = metricAPIService.getMetricData("",
                    "Server%20&%20Infrastructure%20Monitoring/metric-data?metric-path=Application%20Infrastructure%20Performance%7CRoot%7CCustom%20Metrics%7CProcess%20Monitor%7CLinux%20Processes%7Cmachine%20agent%7CResident%20Set%20Size&time-range-type=BEFORE_NOW&duration-in-mins=60&output=JSON");
        }
        Assert.assertNotNull("Cannot connect to controller API", jsonNode);
        if (jsonNode != null) {
            JsonNode valueNode = JsonUtils.getNestedObject(jsonNode, "metricName");
            String metricName = (valueNode == null) ? "" : valueNode.get(0).toString();
            int metricValue = (valueNode == null) ? 0 : valueNode.get(0).asInt();
            Assert.assertEquals("Metric alias is invalid", "\"Custom Metrics|Process Monitor|Linux Processes|machine agent|Resident Set Size\"", metricName);
            Assert.assertNotNull("Metric Value is  null in last 15min, maybe a stale metric ", metricValue);
        }
    }

    @Test
    public void checkDashboardsUploaded() {
        boolean dashboardPresent = false;
        if (customDashboardAPIService != null) {
            JsonNode allDashboardsNode = customDashboardAPIService.getAllDashboards();
            dashboardPresent = IntegrationTestUtils.isDashboardPresent("Process Monitor SIM Dashboard", allDashboardsNode);
            Assert.assertTrue(dashboardPresent);
        } else {
            Assert.assertTrue(false);
        }
    }

    @Test
    public void checkMetricCharReplaced() {
        JsonNode jsonNode = null;
        if (metricAPIService != null) {
            jsonNode = metricAPIService.getMetricData("",
                    "Server%20&%20Infrastructure%20Monitoring/metric-data?metric-path=Application%20Infrastructure%20Performance%7CRoot%7CCustom%20Metrics%7CProcess%20Monitor%7CLinux%20Processes%7Cmachine%20agent%7CResident%20Set%20Size&time-range-type=BEFORE_NOW&duration-in-mins=60&output=JSON");
        }
        Assert.assertNotNull("Cannot connect to controller API", jsonNode);
        if (jsonNode != null) {
            JsonNode valueNode = JsonUtils.getNestedObject(jsonNode, "metricName");
            String metricName = (valueNode == null) ? "" : valueNode.get(0).toString();
            int metricValue = (valueNode == null) ? 0 : valueNode.get(0).asInt();
            Assert.assertEquals("Metric char replacement is not done", "\"Custom Metrics|Process Monitor|Linux Processes|machine agent|Resident Set Size\"", metricName);
            Assert.assertNotNull("Metric Value is  null in last 15min, maybe a stale metric ", metricValue);
        }
    }

}
