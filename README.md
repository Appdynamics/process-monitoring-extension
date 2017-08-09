# AppDynamics Processes - Monitoring Extension

This extension works only with the standalone machine agent.

##Use Case
This version of AppDynamics Process Extension reports the availability of a process on a Linux/Windows/Solaris/AIX machine and displays it in the AppDynamics Metric Browser.

The metric "Number of running instances" reports the available process count and can be used to identify the state of a process (running / not running).

**Note**: If running on Windows, this extension has Sigar dependencies. Please make sure to copy sigar-*.jar, sigar-amd64-winnt.dll, sigar-x86-winnt.dll from MachineAgent\lib to MachineAgent\monitorLibs

##Installation
1. To build from source, clone this repository and run 'mvn clean install'. This will produce a ProcessMonitor-VERSION.zip in the target directory. Alternatively, download the latest release archive from [Github](https://github.com/Appdynamics/process-monitoring-extension/releases/latest).
2. Unzip as "ProcessMonitor" and copy the "ProcessMonitor" directory to `<MACHINE_AGENT_HOME>/monitors`
3. Configure the extension by referring to the below section.
4. Restart the Machine Agent. Before restarting the Machine Agent, you could verify the extension output in workbench mode. Check in WorkBench section for details.

In the AppDynamics Metric Browser, look for: Application Infrastructure Performance  | \<Tier\> | Individual Nodes | Custom Metrics | \<OS\> Processes

## Configuration and Metric ##
Note : Please make sure to not use tab (\t) while editing yaml files. You may want to validate the yaml file using a [yaml validator](http://yamllint.com/)

   Configure the extension by editing the config.yml file in `<MACHINE_AGENT_HOME>/monitors/ProcessMonitor/`.

   For eg.
   ```
    metricPrefix: "Server|Component:<Component-ID>|Custom Metrics|Process Monitor|"
    # metricPrefix: Custom Metrics|Process Monitor
    
    # displayName: required - Metrics to be reported under this name in Controller's Metric Browser
    # regex OR pid - process is fetched using this field
    instances:
      - displayName: "machine agent"
        regex: ".* machineagent.jar"
    
      - displayName: "ssh"
        pid: 1056

   ```
   
   instances: process instances that are to be monitored
   
   displayName: (mandatory) the name that is displayed on AppDynamics Metric Browser
   
   regex OR pid: pattern or pid of the process that is monitored
   
   When using regex, in order to monitor a particular process, please make sure that the pattern uniquely identifies the process of interest. 
   
   Metric Reported: Number of running instances (count of the matching processes defined by regex or pid)

## WorkBench
Workbench is a feature that lets you preview the metrics before registering it with the controller. This is useful if you want to fine tune the configurations. Workbench is embedded into the extension jar.
To use the workbench

1. Deploy the extension and make all tne necessary configurations.
2. Start the workbench with the command
`java -jar /path/to/MachineAgent/monitors/ProcessMonitor/process-monitoring-extension.jar`
This starts an http server at `http://host:9090/`. This can be accessed from the browser.
3. If the server is not accessible from outside/browser, you can use the following end points to see the list of registered metrics and errors. #Get the stats `curl http://localhost:9090/api/stats` #Get the registered metrics `curl http://localhost:9090/api/metric-paths`
4. You can make the changes to config.yml and validate it from the browser or the API
5. Once the configuration is complete, you can kill the workbench and start the Machine Agent


## Troubleshooting 
1. Verify Machine Agent Data:Please start the Machine Agent without the extension and make sure that it reports data. Verify that the machine agent status is UP and it is reporting Machine Agent Availability Metric.
2. Metric Limit: Please start the machine agent with the argument -Dappdynamics.agent.maxMetrics=2000, if there is a metric limit reached error in the logs [Ref](http://docs.appdynamics.com/display/PRO14S/Metrics+Limits).
3. Collect Debug Logs: Edit the file, `<MachineAgent>/conf/logging/log4j.xml` and update the level of the appender "com.appdynamics" and "com.singularity" to debug.
4. In windows, if there is a java.lang.NoClassDefFoundError: org/hyperic/sigar/SigarException in machine-agent.log, please copy sigar-*.jar, sigar-amd64-winnt.dll, sigar-x86-winnt.dll from MachineAgent_Home/lib to MachineAgent_Home/monitorsLibs.

##Contributing
Always feel free to fork and contribute any changes directly here on GitHub.

##Community

Find out more in the [AppSphere](https://www.appdynamics.com/community/exchange/extension/process-monitoring-extension/) community.

##Support

For any questions or feature request, please contact [AppDynamics Support](mailto:help@appdynamics.com).
