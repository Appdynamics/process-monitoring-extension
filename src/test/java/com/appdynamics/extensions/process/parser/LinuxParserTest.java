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

package com.appdynamics.extensions.process.parser;

import com.appdynamics.extensions.process.common.CommandExecutor;
import com.appdynamics.extensions.process.common.MonitorConstants;
import com.appdynamics.extensions.process.configuration.Instance;
import com.appdynamics.extensions.process.data.ProcessData;
import com.appdynamics.extensions.yml.YmlReader;
import com.google.common.base.Charsets;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.powermock.api.mockito.PowerMockito.mockStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest({CommandExecutor.class})
public class LinuxParserTest {

    private LinuxParser parser;
    private List<String> processList;

    @Before
    public void setup() throws IOException {
        parser = Mockito.spy(new LinuxParser());
        processList = Files.readLines(new File("src/test/resources/outputsamples/linux/ps.txt"), Charsets.UTF_8);
        mockStatic(CommandExecutor.class);
    }

    @Test
    public void testFetchMetrics() {
        Map<String, ?> configArgs = YmlReader.readFromFile(new File("src/test/resources/conf/config-linux.yml"));
        PowerMockito.when(CommandExecutor.execute(MonitorConstants.LINUX_PROCESS_LIST_COMMAND)).thenReturn(processList);
        Map<String, ProcessData> processDataMap = parser.fetchMetrics(configArgs);

        Map<String, String> dockerProcessData = processDataMap.get("docker").getProcessMetrics();
        Assert.assertEquals(String.valueOf(2), dockerProcessData.get(MonitorConstants.RUNNING_INSTANCES_COUNT));

        Map<String, String> biosetProcessData = processDataMap.get("bioset").getProcessMetrics();
        Assert.assertEquals(String.valueOf(10), biosetProcessData.get(MonitorConstants.RUNNING_INSTANCES_COUNT));
        // If instance count is > 1, no metrics will be reported
        Assert.assertNull(biosetProcessData.get("CPU%"));

        Map<String, String> javaProcessData = processDataMap.get("java").getProcessMetrics();
        Assert.assertEquals(String.valueOf(1), javaProcessData.get(MonitorConstants.RUNNING_INSTANCES_COUNT));
        Assert.assertEquals(String.valueOf(264708), javaProcessData.get("RSS"));

        Map<String, String> hadoopProcessData = processDataMap.get("hadoop").getProcessMetrics();
        Assert.assertEquals(String.valueOf(0), hadoopProcessData.get(MonitorConstants.RUNNING_INSTANCES_COUNT));
    }

    @Test
    public void testFilterProcessLinesFromCompleteList() throws IOException {
        List<Instance> instances = Lists.newArrayList();

        Instance instance = new Instance();
        instance.setDisplayName("java");
        instance.setRegex("java.exe");
        instances.add(instance);

        Instance instance1 = new Instance();
        instance.setDisplayName("ssh");
        instance.setRegex(".*sshd.*");
        instances.add(instance1);

        List<String> headerColumns = Lists.newArrayList();
        headerColumns.add("PID");
        headerColumns.add("CPU%");
        headerColumns.add("Memory%");
        headerColumns.add("RSS");
        headerColumns.add("COMMAND");

        ListMultimap<String, String> filteredLines = parser.filterProcessLinesFromCompleteList(processList, instances, headerColumns);
        Assert.assertEquals(0, filteredLines.get("java").size());
        Assert.assertEquals(3, filteredLines.get("ssh").size());
    }

    @Test
    public void testStringSplitWithLimit() {
        String string = "  0.0   0.0 /sbin/init splash";
        String[] columns = string.trim().split("\\s+");
        Assert.assertNotEquals(3, columns.length);
        Assert.assertEquals("/sbin/init", columns[2]);

        String[] columns1 = string.trim().split("\\s+", 3);
        Assert.assertEquals(3, columns1.length);
        Assert.assertEquals("/sbin/init splash", columns1[2]);
    }

    @Test
    public void testRegexMatch() {
        String str = "/opt/push-jobs-client/bin/ruby /opt/push-jobs-client/bin/pushy-client -c /etc/push-jobs-clientb";
        Assert.assertTrue(str.matches(".*pushy-client.*"));
        Assert.assertTrue(str.matches(".*pushy-client .*"));
        Assert.assertFalse(str.matches(".*pushy-client  .*"));
        Assert.assertFalse(str.matches(".* pushy-client.*"));
    }
}
