package com.appdynamics.extensions.process.parser;

import com.appdynamics.extensions.process.common.MonitorConstants;
import com.appdynamics.extensions.process.configuration.ConfigProcessor;
import com.appdynamics.extensions.process.configuration.Instance;
import com.appdynamics.extensions.process.data.ProcessData;
import com.appdynamics.extensions.yml.YmlReader;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
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
        List<Instance> instances = new ConfigProcessor().processConfig(configArgs);
        Mockito.doReturn(processList).when(parser).fetchProcessListFromSigar();
        Map<String, ProcessData> processDataMap = parser.parseProcesses(configArgs);

        Map<String, BigDecimal> dockerProcessData = processDataMap.get("docker").getProcessMetrics();
        Assert.assertEquals(BigDecimal.ZERO, dockerProcessData.get(MonitorConstants.RUNNING_INSTANCES_COUNT));

        Map<String, BigDecimal> svchostProcessData = processDataMap.get("svchost").getProcessMetrics();
        Assert.assertEquals(BigDecimal.valueOf(11), svchostProcessData.get(MonitorConstants.RUNNING_INSTANCES_COUNT));

        Map<String, BigDecimal> svchostFilteredData = processDataMap.get("svchostFiltered").getProcessMetrics();
        Assert.assertEquals(BigDecimal.ONE, svchostFilteredData.get(MonitorConstants.RUNNING_INSTANCES_COUNT));

        Map<String, BigDecimal> javaProcessData = processDataMap.get("java").getProcessMetrics();
        Assert.assertEquals(BigDecimal.valueOf(2), javaProcessData.get(MonitorConstants.RUNNING_INSTANCES_COUNT));

        Map<String, BigDecimal> machineAgentProcessData = processDataMap.get("MachineAgent").getProcessMetrics();
        Assert.assertEquals(BigDecimal.ONE, machineAgentProcessData.get(MonitorConstants.RUNNING_INSTANCES_COUNT));

        Map<String, BigDecimal> notepadProcessData = processDataMap.get("Notepad").getProcessMetrics();
        Assert.assertEquals(BigDecimal.ONE, notepadProcessData.get(MonitorConstants.RUNNING_INSTANCES_COUNT));
    }
}
