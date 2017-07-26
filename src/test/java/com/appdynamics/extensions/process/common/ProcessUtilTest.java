package com.appdynamics.extensions.process.common;

import com.appdynamics.extensions.yml.YmlReader;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.List;
import java.util.Map;

public class ProcessUtilTest {

    @Test
    public void testIsMatchProcessName() {
        Map<String, ?> configArgs = YmlReader.readFromFile(new File("src/test/resources/conf/config.yml"));
        List<Map> processToBeMonitoredList = (List<Map>) configArgs.get("processesToBeMonitored");

        Map processToBeMonitored = ProcessUtil.isMatchProcessName(processToBeMonitoredList, "java -jar machine-agent.jar");
        Assert.assertNotNull(processToBeMonitored);


        processToBeMonitored = ProcessUtil.isMatchProcessName(processToBeMonitoredList, "gedit");
        Assert.assertNull(processToBeMonitored);

        processToBeMonitored = ProcessUtil.isMatchProcessName(processToBeMonitoredList, "[bioset]");
        Assert.assertNotNull(processToBeMonitored);

        processToBeMonitored = ProcessUtil.isMatchProcessName(processToBeMonitoredList, "java");
        Assert.assertNull(processToBeMonitored);
    }
}
