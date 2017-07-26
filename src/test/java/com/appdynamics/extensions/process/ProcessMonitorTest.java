package com.appdynamics.extensions.process;

import com.appdynamics.extensions.process.processexception.ProcessMonitorException;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashMap;

public class ProcessMonitorTest {

    private ProcessMonitor classUnderTest;

    @Before
    public void setup() {
        classUnderTest = Mockito.spy(new ProcessMonitor());
    }

    @Test(expected=TaskExecutionException.class)
    public void testWithNoArgs() throws TaskExecutionException {
        classUnderTest.execute(null, null);
    }

    @Test(expected=TaskExecutionException.class)
    public void testWithEmptyArgs() throws TaskExecutionException {
        classUnderTest.execute(new HashMap<String, String>(), null);
    }

    @Test(expected=ProcessMonitorException.class)
    public void testWithMacOS() throws ProcessMonitorException {
        Mockito.when(classUnderTest.getOSFromSystemProperty()).thenReturn("mac");
        classUnderTest.determineOS();
    }
}
