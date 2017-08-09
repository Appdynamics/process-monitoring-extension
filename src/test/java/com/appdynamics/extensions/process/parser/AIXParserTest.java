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
import java.math.BigDecimal;
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
    public void testParseProcesses() {
        Map<String, ?> configArgs = YmlReader.readFromFile(new File("src/test/resources/conf/config-aix.yml"));
        PowerMockito.when(CommandExecutor.execute(MonitorConstants.AIX_PROCESS_LIST_COMMAND)).thenReturn(processList);
        Map<String, ProcessData> processDataMap = parser.parseProcesses(configArgs);

        Map<String, BigDecimal> javaProcessData = processDataMap.get("java").getProcessMetrics();
        Assert.assertEquals(BigDecimal.valueOf(3), javaProcessData.get(MonitorConstants.RUNNING_INSTANCES_COUNT));

        Map<String, BigDecimal> sshProcessData = processDataMap.get("ssh").getProcessMetrics();
        Assert.assertEquals(BigDecimal.ONE, sshProcessData.get(MonitorConstants.RUNNING_INSTANCES_COUNT));

        Map<String, BigDecimal> testProcessData = processDataMap.get("test").getProcessMetrics();
        Assert.assertEquals(BigDecimal.ZERO, testProcessData.get(MonitorConstants.RUNNING_INSTANCES_COUNT));
    }
}
