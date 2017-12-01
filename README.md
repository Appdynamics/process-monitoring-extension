# AppDynamics Process Monitoring Extension

This extension works with the AppDynamics Java Machine agent.

## Use Case
Process Monitoring Extension gathers process metrics from a Linux/Windows/Solaris/AIX machine and reports to AppDynamics Controller. It utilizes the ps command in Unix like systems and Sigar library in Windows to fetch basic process metrics.

This can also be used as a process checker (identify whether a process is running/not running) by its metric "Running Instances". If the configured process is not running, the "Running Instances" metric value reported is "ZERO".

Apart from the "Running Instances" metric, other process metrics are reported only if the extension finds a single instance of the process ("Running Instances" metric value is "ONE").

**Note**: If running on Windows, this extension has Sigar dependencies. Please make sure to copy Windows OS related Sigar files (sigar-*.jar, sigar-amd64-winnt.dll, sigar-x86-winnt.dll) from `<MachineAgent>\lib` to `<MachineAgent>\monitorLibs`

## Installation
1. To build from source, clone this repository and run 'mvn clean install'. This will produce a ProcessMonitor-VERSION.zip in the target directory. Alternatively, download the latest release archive from [Github](https://github.com/Appdynamics/process-monitoring-extension/releases/latest).
2. Unzip as "ProcessMonitor" and copy the "ProcessMonitor" directory to `<MACHINE_AGENT_HOME>/monitors`
3. Configure the extension by editing the config.yml. Refer to the Configuration section for details.
4. Verify the extension output in workbench mode and make sure desired metrics are reported. Check in WorkBench section for details.
5. Restart the Machine Agent

In the AppDynamics Metric Browser, look for: Application Infrastructure Performance  | \<Tier\> | Individual Nodes | <Node> | Custom Metrics | Process Monitor | \<OS\> Processes

## Configuration
**Note**: Please make sure to not use tab (\t) while editing yaml files. You may want to validate the yaml file using a [yaml validator](http://yamllint.com/)

Edit the config.yml file in `<MACHINE_AGENT_HOME>/monitors/ProcessMonitor/` to update the following.
 1. `metricPrefix`: If you wish to report metrics only to the tier which this MachineAgent is reporting to, please comment the second metricPrefix and update the "<Component-ID>" with TierID or TierName in the first metricPrefix.
   ```
   metricPrefix: "Server|Component:<Component-ID>|Custom Metrics|Process Monitor|"
   metricPrefix: "Custom Metrics|Process Monitor|"
   ```
 2. `instances`: process instances that are to be monitored. `displayName` which is mandatory is rendered on the metric browser and the process metrics are reported under this name. The process to be monitored can be configured in three ways: `regex`/`pid`/`pidFile`. 
   For process checker, in case of regex, please make sure the pattern uniquely identifies the process of interest.
   ```
    instances:
      - displayName: "machine agent"
        regex: ".* machineagent.jar"
    
      - displayName: "ssh"
        pid: "1056"
    
      - displayName: "mysql"
        pidFile: "/opt/mysql/db/mysql.pid"
   ```
 3. Commands (Optional). The underlying OS command can be modified to fetch any additional metrics if required. Please execute the command and make sure there is a valid output in a tabular format. Please check man pages for unix like systems. We do not support this option for Windows environment.
   ```
   linux:
     process: "ps -eo pid,%cpu=CPU%,%mem=Memory%,rsz=RSS,args"
      
   solaris:
     process: "ps -eo pid,pcpu=CPU%, -o pmem=Memory%, -o rss=RSS -o args"
      
   aix:
     process: "ps -eo pid,pcpu=CPU%,pmem=Memory%,rss=RSS,args"

   ```
 4. `metrics` (Optional). Existing metric properties can be modified in this section. If additional metrics are added in the commands, please configure them here with appropriate metric properties for extension to report these metrics. Supported properties are `alias`, `multiplier`, `delta`.
   ```
   metrics:
     - CPU%:
        multiplier: 1
     - Memory%:
        alias: "Memory%"
     - RSS:
        alias: "Resident Set Size"
     - Running Instances:
        alias: "Running Instances"
   ```
## Metrics
1. Running Instances: Count of the matched processes that are identified by regex.
2. CPU%, Memory%, Resident Set Size: Reported only if Running Instances metric value is ONE

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
1. For missing custom metrics, please refer to the KB article [here](https://community.appdynamics.com/t5/Knowledge-Base/How-to-troubleshoot-missing-custom-metrics-or-extensions-metrics/ta-p/28695)
2. Verify Machine Agent Data:Please start the Machine Agent without the extension and make sure that it reports data. Verify that the machine agent status is UP and it is reporting Machine Agent Availability Metric.
3. Metric Limit: Please start the machine agent with the argument -Dappdynamics.agent.maxMetrics=2000, if there is a metric limit reached error in the logs [Ref](http://docs.appdynamics.com/display/PRO14S/Metrics+Limits).
4. Collect Debug Logs: Edit the file, `<MachineAgent>/conf/logging/log4j.xml` and update the level of the appender "com.appdynamics" and "com.singularity" to debug.
5. In windows, if there is a java.lang.NoClassDefFoundError: org/hyperic/sigar/SigarException in machine-agent.log, please copy Windows OS related Sigar files (sigar-*.jar, sigar-amd64-winnt.dll, sigar-x86-winnt.dll) from `<MachineAgent>/lib` to `<MachineAgent>/monitorsLibs`.

##Contributing
Always feel free to fork and contribute any changes directly here on GitHub.

##Community

Find out more in the [AppSphere](https://www.appdynamics.com/community/exchange/extension/process-monitoring-extension/) community.

##Support

For any questions or feature request, please contact [AppDynamics Support](mailto:help@appdynamics.com).
