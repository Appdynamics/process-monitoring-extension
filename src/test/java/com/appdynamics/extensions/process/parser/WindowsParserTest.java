package com.appdynamics.extensions.process.parser;

import com.appdynamics.extensions.process.common.MonitorConstants;
import com.appdynamics.extensions.process.data.ProcessData;
import com.appdynamics.extensions.yml.YmlReader;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import org.hyperic.sigar.Sigar;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class WindowsParserTest {

    private WindowsParser parser;
    List<String> processList;

    @Before
    public void setup() throws IOException {
        parser = Mockito.spy(new WindowsParser());
        processList = Files.readLines(new File("src/test/resources/outputsamples/win/win_proc_out.txt"), Charsets.UTF_8);
    }

    @Test
    public void testParseProcesses() {
        Map<String, ?> configArgs = YmlReader.readFromFile(new File("src/test/resources/conf/config-windows.yml"));
        Mockito.doReturn(processList).when(parser).fetchProcessListFromSigar();
        Mockito.doReturn(20.0).when(parser).getProcCPU((Sigar) Mockito.anyObject(), Mockito.anyString());
        Mockito.doReturn(Long.valueOf(20)).when(parser).getProcMem((Sigar) Mockito.anyObject(), Mockito.anyString());
        Map<String, ProcessData> processDataMap = parser.fetchMetrics(configArgs);

        Map<String, String> dockerProcessData = processDataMap.get("docker").getProcessMetrics();
        Assert.assertEquals(String.valueOf(0), dockerProcessData.get(MonitorConstants.RUNNING_INSTANCES_COUNT));

        Map<String, String> svchostProcessData = processDataMap.get("svchost").getProcessMetrics();
        Assert.assertEquals(String.valueOf(11), svchostProcessData.get(MonitorConstants.RUNNING_INSTANCES_COUNT));

        Map<String, String> svchostFilteredData = processDataMap.get("svchostFiltered").getProcessMetrics();
        Assert.assertEquals(String.valueOf(1), svchostFilteredData.get(MonitorConstants.RUNNING_INSTANCES_COUNT));

        Map<String, String> javaProcessData = processDataMap.get("java").getProcessMetrics();
        Assert.assertEquals(String.valueOf(2), javaProcessData.get(MonitorConstants.RUNNING_INSTANCES_COUNT));

        Map<String, String> machineAgentProcessData = processDataMap.get("MachineAgent").getProcessMetrics();
        Assert.assertEquals(String.valueOf(1), machineAgentProcessData.get(MonitorConstants.RUNNING_INSTANCES_COUNT));

        Map<String, String> notepadProcessData = processDataMap.get("Notepad").getProcessMetrics();
        Assert.assertEquals(String.valueOf(1), notepadProcessData.get(MonitorConstants.RUNNING_INSTANCES_COUNT));
    }
}
