package com.appdynamics.extensions.process.parser;

import com.appdynamics.extensions.process.common.CommandExecutor;
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
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.powermock.api.mockito.PowerMockito.mockStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest({CommandExecutor.class})
public class SolarisParserTest {

    private SolarisParser parser;
    private List<String> processList;

    @Before
    public void setup() throws IOException {
        parser = Mockito.spy(new SolarisParser());
        processList = Files.readLines(new File("src/test/resources/outputsamples/solaris/topoutput.txt"), Charsets.UTF_8);
        mockStatic(CommandExecutor.class);
    }

    @Test
    public void testParseProcesses() {
        Map<String, ?> configArgs = YmlReader.readFromFile(new File("src/test/resources/conf/config-solaris.yml"));
        List<Instance> instances = new ConfigProcessor().processConfig(configArgs);
        PowerMockito.when(CommandExecutor.execute(MonitorConstants.SOLARIS_PROCESS_LIST_COMMAND)).thenReturn(processList);
        Map<String, ProcessData> processDataMap = parser.parseProcesses(configArgs);

        Map<String, BigDecimal> dockerProcessData = processDataMap.get("docker").getProcessMetrics();
        Assert.assertEquals(BigDecimal.ZERO, dockerProcessData.get(MonitorConstants.RUNNING_INSTANCES_COUNT));

        Map<String, BigDecimal> gnomeProcessData = processDataMap.get("gnome").getProcessMetrics();
        Assert.assertEquals(BigDecimal.valueOf(6), gnomeProcessData.get(MonitorConstants.RUNNING_INSTANCES_COUNT));

        Map<String, BigDecimal> javaProcessData = processDataMap.get("java").getProcessMetrics();
        Assert.assertEquals(BigDecimal.ONE, javaProcessData.get(MonitorConstants.RUNNING_INSTANCES_COUNT));

        Map<String, BigDecimal> java1ProcessData = processDataMap.get("java1").getProcessMetrics();
        Assert.assertEquals(BigDecimal.ZERO, java1ProcessData.get(MonitorConstants.RUNNING_INSTANCES_COUNT));

        Map<String, BigDecimal> hadoopProcessData = processDataMap.get("hadoop").getProcessMetrics();
        Assert.assertEquals(BigDecimal.ZERO, hadoopProcessData.get(MonitorConstants.RUNNING_INSTANCES_COUNT));
    }
}
