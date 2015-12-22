package com.appdynamics.extensions.process;

import com.google.common.collect.Maps;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.junit.Test;

import java.util.Map;

/**
 * Created by balakrishnav on 22/12/15.
 */
public class ProcessMonitorTest {

    public static final String CONFIG_ARG = "config-file";

    @Test
    public void testProcessMonitor() throws TaskExecutionException {
        Map<String, String> taskArgs = Maps.newHashMap();
        taskArgs.put(CONFIG_ARG, "src/test/resources/conf/config.yml");

        ProcessMonitor monitor = new ProcessMonitor();
        monitor.execute(taskArgs, null);
    }
}
