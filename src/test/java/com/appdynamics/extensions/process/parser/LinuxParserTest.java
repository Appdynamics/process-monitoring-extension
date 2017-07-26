package com.appdynamics.extensions.process.parser;

import com.appdynamics.extensions.process.common.CommandExecutor;
import com.appdynamics.extensions.process.common.MetricConstants;
import com.appdynamics.extensions.process.common.ProcessCommands;
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
    public void testParseProcesses() {
        Map<String, ?> configArgs = YmlReader.readFromFile(new File("src/test/resources/conf/config-linux.yml"));
        PowerMockito.when(CommandExecutor.execute(ProcessCommands.LINUX_PROCESS_LIST_COMMAND)).thenReturn(processList);
        Map<String, ProcessData> processDataMap = parser.parseProcesses(configArgs);

        Map<String, BigDecimal> dockerProcessData = processDataMap.get("docker").getProcessMetrics();
        Assert.assertEquals(dockerProcessData.get(MetricConstants.NUMBER_OF_RUNNING_INSTANCES), new BigDecimal(2));

        Map<String, BigDecimal> biosetProcessData = processDataMap.get("bioset").getProcessMetrics();
        Assert.assertEquals(biosetProcessData.get(MetricConstants.NUMBER_OF_RUNNING_INSTANCES), new BigDecimal(26));

        Map<String, BigDecimal> javaProcessData = processDataMap.get("java").getProcessMetrics();
        Assert.assertEquals(javaProcessData.get(MetricConstants.NUMBER_OF_RUNNING_INSTANCES), new BigDecimal(1));

        Map<String, BigDecimal> hadoopProcessData = processDataMap.get("hadoop").getProcessMetrics();
        Assert.assertEquals(hadoopProcessData.get(MetricConstants.NUMBER_OF_RUNNING_INSTANCES), new BigDecimal(0));
    }
}
