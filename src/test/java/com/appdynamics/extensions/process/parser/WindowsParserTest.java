package com.appdynamics.extensions.process.parser;

import com.appdynamics.extensions.process.common.MetricConstants;
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
        Mockito.doReturn(processList).when(parser).fetchProcessListFromSigar();
        Map<String, ProcessData> processDataMap = parser.parseProcesses(configArgs);

        Map<String, BigDecimal> javaProcessData = processDataMap.get("java").getProcessMetrics();
        Assert.assertEquals(javaProcessData.get(MetricConstants.NUMBER_OF_RUNNING_INSTANCES), new BigDecimal(1));

        Map<String, BigDecimal> dockerProcessData = processDataMap.get("docker").getProcessMetrics();
        Assert.assertEquals(dockerProcessData.get(MetricConstants.NUMBER_OF_RUNNING_INSTANCES), new BigDecimal(0));

        Map<String, BigDecimal> svchostProcessData = processDataMap.get("svchost.exe").getProcessMetrics();
        Assert.assertEquals(svchostProcessData.get(MetricConstants.NUMBER_OF_RUNNING_INSTANCES), new BigDecimal(12));
    }
}
