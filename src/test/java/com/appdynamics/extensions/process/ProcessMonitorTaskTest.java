package com.appdynamics.extensions.process;

import com.appdynamics.extensions.conf.MonitorConfiguration;
import com.appdynamics.extensions.process.common.CommandExecutor;
import com.appdynamics.extensions.process.common.MonitorConstants;
import com.appdynamics.extensions.process.parser.LinuxParser;
import com.appdynamics.extensions.process.parser.ParserFactory;
import com.appdynamics.extensions.process.parser.SolarisParser;
import com.appdynamics.extensions.process.parser.WindowsParser;
import com.appdynamics.extensions.util.MetricWriteHelper;
import com.appdynamics.extensions.util.MetricWriteHelperFactory;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.powermock.api.mockito.PowerMockito.mockStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest({CommandExecutor.class, ParserFactory.class, LinuxParser.class})
public class ProcessMonitorTaskTest extends AManagedMonitor {

    private MetricWriteHelper writer;
    private MonitorConfiguration configuration;
    private ProcessMonitorTask task;

    @Before
    public void setup() {
        Runnable runnable = Mockito.mock(Runnable.class);
        writer = MetricWriteHelperFactory.create(this);
        writer = Mockito.spy(writer);
        configuration = new MonitorConfiguration("Test", runnable, writer);
        configuration = Mockito.spy(configuration);
        mockStatic(CommandExecutor.class);
    }

    @Test
    public void testLinuxParserTask() throws Exception {
        task = Mockito.spy(new ProcessMonitorTask(configuration, "linux"));
        configuration.setConfigYml("src/test/resources/conf/config-linux.yml");
        List<String> processList = Files.readLines(new File("src/test/resources/outputsamples/linux/ps.txt"), Charsets.UTF_8);

        int configuredProcessesSize = ((List<Map>)configuration.getConfigYml().get("instances")).size();

        PowerMockito.when(CommandExecutor.execute(MonitorConstants.LINUX_PROCESS_LIST_COMMAND)).thenReturn(processList);

        LinuxParser parser = (LinuxParser) Mockito.spy(ParserFactory.createParser("linux"));
        PowerMockito.whenNew(LinuxParser.class).withNoArguments().thenReturn(parser);

        task.run();

        Mockito.verify(writer, Mockito.times(configuredProcessesSize)).printMetric(Mockito.anyString(), Mockito.any(BigDecimal.class), Mockito.anyString());
    }

    @Test
    public void testWindowsParserTask() throws Exception {
        task = Mockito.spy(new ProcessMonitorTask(configuration, "win"));
        configuration.setConfigYml("src/test/resources/conf/config-windows.yml");
        List<String> processList = Files.readLines(new File("src/test/resources/outputsamples/win/win_proc_out.txt"), Charsets.UTF_8);
        int configuredProcessesSize = ((List<Map>)configuration.getConfigYml().get("instances")).size();

        WindowsParser parser = (WindowsParser) PowerMockito.spy(ParserFactory.createParser("win"));
        PowerMockito.whenNew(WindowsParser.class).withNoArguments().thenReturn(parser);

        Mockito.doReturn(processList).when(parser).fetchProcessListFromSigar();
        task.run();

        Mockito.verify(writer, Mockito.times(configuredProcessesSize)).printMetric(Mockito.anyString(), Mockito.any(BigDecimal.class), Mockito.anyString());
    }

    @Test
    public void testSolarisParserTask() throws Exception {
        task = Mockito.spy(new ProcessMonitorTask(configuration, "sunos"));
        configuration.setConfigYml("src/test/resources/conf/config-solaris.yml");
        List<String> processList = Files.readLines(new File("src/test/resources/outputsamples/solaris/topoutput.txt"), Charsets.UTF_8);

        PowerMockito.when(CommandExecutor.execute(MonitorConstants.SOLARIS_PROCESS_LIST_COMMAND)).thenReturn(processList);

        SolarisParser parser = (SolarisParser) PowerMockito.spy(ParserFactory.createParser("sunos"));
        PowerMockito.whenNew(SolarisParser.class).withNoArguments().thenReturn(parser);

        task.run();

        int configuredProcessesSize = ((List<Map>)configuration.getConfigYml().get("instances")).size();

        Mockito.verify(writer, Mockito.times(configuredProcessesSize)).printMetric(Mockito.anyString(), Mockito.any(BigDecimal.class), Mockito.anyString());
    }

    public TaskOutput execute(Map<String, String> map, TaskExecutionContext taskExecutionContext) throws TaskExecutionException {
        return null;
    }

}
