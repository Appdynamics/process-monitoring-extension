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
import com.appdynamics.extensions.process.data.ProcessData;
import com.appdynamics.extensions.yml.YmlReader;
import com.google.common.base.Charsets;
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
public class AIXParserTest {

    private AIXParser parser;
    private List<String> processList;

    @Before
    public void setup() throws IOException {
        parser = Mockito.spy(new AIXParser());
        processList = Files.readLines(new File("src/test/resources/outputsamples/aix/psout.txt"), Charsets.UTF_8);
        mockStatic(CommandExecutor.class);
    }

    @Test
    public void testFetchMetrics() {
        Map<String, ?> configArgs = YmlReader.readFromFile(new File("src/test/resources/conf/config-aix.yml"));
        PowerMockito.when(CommandExecutor.execute(MonitorConstants.AIX_PROCESS_LIST_COMMAND, null)).thenReturn(processList);
        Map<String, ProcessData> processDataMap = parser.fetchMetrics(configArgs);

        Map<String, String> javaProcessData = processDataMap.get("java").getProcessMetrics();
        Assert.assertEquals(String.valueOf(3), javaProcessData.get(MonitorConstants.RUNNING_INSTANCES_COUNT));
        Assert.assertNull(javaProcessData.get("CPU%"));

        Map<String, String> sshProcessData = processDataMap.get("ssh").getProcessMetrics();
        Assert.assertEquals(String.valueOf(1), sshProcessData.get(MonitorConstants.RUNNING_INSTANCES_COUNT));
        Assert.assertEquals(String.valueOf(1548), sshProcessData.get("RSS"));

        Map<String, String> testProcessData = processDataMap.get("test").getProcessMetrics();
        Assert.assertEquals(String.valueOf(0), testProcessData.get(MonitorConstants.RUNNING_INSTANCES_COUNT));
    }
}
