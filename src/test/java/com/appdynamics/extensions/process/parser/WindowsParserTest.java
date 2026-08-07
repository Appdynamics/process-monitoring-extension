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

import com.appdynamics.extensions.process.common.MonitorConstants;
import com.appdynamics.extensions.process.configuration.Instance;
import com.appdynamics.extensions.process.data.ProcessData;
import com.appdynamics.extensions.yml.YmlReader;
import com.google.common.base.Charsets;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.io.Files;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import oshi.software.os.OSProcess;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WindowsParserTest {

    List<String> processList;
    private WindowsParser parser;

    @Before
    public void setup() throws IOException {
        parser = Mockito.spy(new WindowsParser());
        processList = Files.readLines(new File("src/test/resources/outputsamples/win/win_proc_out.txt"), Charsets.UTF_8);
    }

    @Test
    public void testParseProcesses() {
        Map<String, ?> configArgs = YmlReader.readFromFile(new File("src/test/resources/conf/config-windows.yml"));
        Mockito.doReturn(processList).when(parser).fetchProcessList();
        Mockito.doReturn(20.0).when(parser).getProcCPU(Mockito.anyString());
        Mockito.doReturn(Long.valueOf(20)).when(parser).getProcMem(Mockito.anyString());
        Map<String, ProcessData> processDataMap = parser.fetchMetrics(configArgs);

        Map<String, String> dockerProcessData = processDataMap.get("docker").getProcessMetrics();
        Assert.assertEquals(String.valueOf(0), dockerProcessData.get(MonitorConstants.RUNNING_INSTANCES_COUNT));

        Map<String, String> svchostProcessData = processDataMap.get("svchost").getProcessMetrics();
        Assert.assertEquals(String.valueOf(11), svchostProcessData.get(MonitorConstants.RUNNING_INSTANCES_COUNT));

        Map<String, String> svchostFilteredData = processDataMap.get("svchostFiltered").getProcessMetrics();
        Assert.assertEquals(String.valueOf(1), svchostFilteredData.get(MonitorConstants.RUNNING_INSTANCES_COUNT));

        Map<String, String> javaProcessData = processDataMap.get("java").getProcessMetrics();
        Assert.assertEquals(String.valueOf(3), javaProcessData.get(MonitorConstants.RUNNING_INSTANCES_COUNT));

        Map<String, String> machineAgentProcessData = processDataMap.get("MachineAgent").getProcessMetrics();
        Assert.assertEquals(String.valueOf(1), machineAgentProcessData.get(MonitorConstants.RUNNING_INSTANCES_COUNT));

        Map<String, String> notepadProcessData = processDataMap.get("Notepad").getProcessMetrics();
        Assert.assertEquals(String.valueOf(1), notepadProcessData.get(MonitorConstants.RUNNING_INSTANCES_COUNT));

        Map<String, String> serviceProcessData = processDataMap.get("TryTryAgain").getProcessMetrics();
        Assert.assertEquals(String.valueOf(1), serviceProcessData.get(MonitorConstants.RUNNING_INSTANCES_COUNT));

        Map<String, String> wmiprvse = processDataMap.get("wmiprvse").getProcessMetrics();
        Assert.assertEquals(String.valueOf(0), wmiprvse.get(MonitorConstants.RUNNING_INSTANCES_COUNT));
    }

    /**
     * Regression test: a PID matched by two overlapping rules (e.g. "java" and "MachineAgent")
     * must report the same interval-based CPU% in both instances.
     */
    @Test
    public void testOverlappingRulesProduceSameCpuForSharedPid() throws Exception {
        // Build two instances whose regexes both match the same PID "4736"
        // (java.exe machineagent.jar in win_proc_out.txt).
        Instance javaInstance = new Instance();
        javaInstance.setDisplayName("java");
        javaInstance.setRegex(".*java.exe.*");

        Instance machineAgentInstance = new Instance();
        machineAgentInstance.setDisplayName("MachineAgent");
        machineAgentInstance.setRegex(".*java.exe .* machineagent.jar");

        List<Instance> instances = new ArrayList<>();
        instances.add(javaInstance);
        instances.add(machineAgentInstance);

        // Shared line for PID 4736 — matches both rules.
        String sharedLine = "4736 C:\\Windows\\system32\\java.exe java -Dappdynamics.agent.maxMetrics=2000 -jar machineagent.jar";
        ListMultimap<String, String> filteredLines = ArrayListMultimap.create();
        filteredLines.put("java", sharedLine);
        filteredLines.put("MachineAgent", sharedLine);

        // Seed previousSnapshots via reflection so getProcCPU sees a valid previous snapshot.
        OSProcess prevProc = Mockito.mock(OSProcess.class);
        Mockito.when(prevProc.getStartTime()).thenReturn(1000L);
        java.lang.reflect.Field prevField = WindowsParser.class.getDeclaredField("previousSnapshots");
        prevField.setAccessible(true);
        Map<String, OSProcess> prevMap = (ConcurrentHashMap<String, OSProcess>) prevField.get(null);
        prevMap.put("4736", prevProc);

        // Seed currentSnapshots so fetchValidProcess finds the process.
        OSProcess currentProc = Mockito.mock(OSProcess.class);
        Mockito.when(currentProc.getStartTime()).thenReturn(1000L);
        // Interval CPU should be 30%; cumulative fallback would differ.
        Mockito.when(currentProc.getProcessCpuLoadBetweenTicks(prevProc)).thenReturn(0.30);
        java.lang.reflect.Field currField = WindowsParser.class.getDeclaredField("currentSnapshots");
        currField.setAccessible(true);
        Map<String, OSProcess> currMap = (Map<String, OSProcess>) currField.get(parser);
        currMap.put("4736", currentProc);

        Map<String, ProcessData> result = parser.populateProcessesData(instances, filteredLines);

        // Both instances must report the same CPU% — interval-based, not cumulative.
        String javaCpu = result.get("java").getProcessMetrics().get(MonitorConstants.CPU_PERCENT);
        String maCpu = result.get("MachineAgent").getProcessMetrics().get(MonitorConstants.CPU_PERCENT);
        Assert.assertNotNull("java CPU% must not be null", javaCpu);
        Assert.assertNotNull("MachineAgent CPU% must not be null", maCpu);
        Assert.assertEquals("Overlapping rules must report the same CPU% for a shared PID", javaCpu, maCpu);
        Assert.assertEquals("30.0", javaCpu);
    }
}
