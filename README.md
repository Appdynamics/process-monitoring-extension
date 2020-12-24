# AppDynamics Process Monitoring Extension
An AppDynamics extension to be used with a standalone Java Machine Agent to provide metrics about the Processes on a machine.

## Use Case
Process Monitoring Extension gathers process metrics from a Linux/Windows/Solaris/AIX machine and reports to AppDynamics Controller.
 It utilizes the ``ps`` command in Unix-like systems and Sigar library in Windows to fetch basic process metrics.

This can also be used as a process checker (identify whether a process is running/not running) by its metric "Running Instances". 
If the configured process is not running, the "Running Instances" metric value reported is "ZERO".

Apart from the "Running Instances" metric, other process metrics are reported ONLY if the extension detects a single instance of the 
process running ("Running Instances" metric value is "ONE").

## Prerequisites
1. Before the extension is installed, the prerequisites mentioned [here](https://community.appdynamics.com/t5/Knowledge-Base/Extensions-Prerequisites-Guide/ta-p/35213) need to be met. Please do not proceed with the extension installation if the specified prerequisites are not met.
2.  If running on Windows, this extension has Sigar dependencies. 
    Please make sure to copy Windows OS related Sigar files (sigar-*.jar, sigar-amd64-winnt.dll, sigar-x86-winnt.dll) from `<MachineAgent>\lib` to `<MachineAgent>\monitorsLibs`


## Installation
1. To build from source, clone this repository and run 'mvn clean install'. This will produce a ProcessMonitor-VERSION.zip in the target directory. Alternatively, download the latest release archive from [Github](https://github.com/Appdynamics/process-monitoring-extension/releases/latest).
2. Unzip as "ProcessMonitor" and copy the "ProcessMonitor" directory to `<MACHINE_AGENT_HOME>/monitors`
3. Please place the extension in the "monitors" directory of your Machine Agent installation directory. 
Do not place the extension in the "extensions" directory of your Machine Agent installation directory.
4.  Edit the config.yml file. An example config.yml file follows these installation instructions.
5. Verify the extension output in workbench mode and make sure desired metrics are reported. Check in WorkBench section for details.
6.  Restart the Machine Agent.


## Configuration


In the AppDynamics Metric Browser, look for: Application Infrastructure Performance  | \<Tier\> | Individual Nodes | \<Node\> | Custom Metrics | Process Monitor | \<OS\> Processes

**Note**: Please make sure to not use tab (\t) while editing yaml files. You may want to validate the yaml file using a [yaml validator](http://yamllint.com/). 

**Note**: Please use workbench mode to verify the metrics while experimenting with various configurations in config.yml to arrive at the desired result.

Edit the config.yml file in `<MACHINE_AGENT_HOME>/monitors/ProcessMonitor/` to update the following.
 1. `metricPrefix`: If you wish to report metrics only to the tier which this MachineAgent is reporting to, please comment the second metricPrefix and update the "<Component-ID>" with TierID or TierName in the first metricPrefix.
   ```
   metricPrefix: "Server|Component:<Component-ID>|Custom Metrics|Process Monitor|"
   metricPrefix: "Custom Metrics|Process Monitor|"
   ```
 2. `instances`: process instances that are to be monitored. `displayName` which is mandatory is used to render the process name on the metric browser and all the process metrics are reported under this name. The process to be monitored can be configured in three ways: `regex`/`pid`/`pidFile`. regex is a regular expression used to match the process and is built based on the command line path to the process.
   For process checker, in case of regex, please make sure the pattern uniquely identifies the process of interest.
   
   For help picking the regex in various OS's please refer to the Troubleshooting section Step 3.
   ```
    instances:
      - displayName: "machine agent"
        regex: ".* machineagent.jar"
    
      - displayName: "ssh"
        pid: "1056"
    
      - displayName: "mysql"
        pidFile: "/opt/mysql/db/mysql.pid"
   ```
   While using regex, it is advisable to use any of the online regex validators (eg: [regextester](https://www.regextester.com/)) to match the regex against the process command line path to verify if it matches.
 
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
### Configuring additional Metrics
Additional metrics can be configured in unix like systems by adding them to the respective commands in config.yml. For example if Virtual Memory Size of a Linux process is needed, the linux command can be modified to the following
        ```
         linux:
              process: "ps -eo pid,%cpu=CPU%,%mem=Memory%,rsz=RSS,vsz=VSZ,args"
        ```
and the metric properties to 
        ```
         metrics:
           - VSZ:
              alias: "Virtual Memory Size"
        ```

## Metrics
The following metrics are returned from the extension: 
    1. Running Instances: Count of the matched processes that are identified by regex. The following metrics are reported only if this metric value is ONE.
    2. CPU%
    3. Resident Set Size
    4. Memory% (Not reported for Windows)

 There are several properties that are associated with each metric. They are: 
    * alias
    * aggregationType
    * timeRollUpType
    * clusterRollUpType
    * multiplier
    * convert
    * delta
   
   This format enables you to change some of the metric properties from what the default configurations are.

    In Order to use them for each metric, please use the following example.
    ```
            metrics:
              - CPU%:
                multiplier: 1
                alias: "CPU Percentage"
                clusterRollUpType: "AVERAGE"
                timeRollUpType: "SUM"
                aggregationType: "SUM"
    ```


### metricPathReplacements
Please visit [this](https://community.appdynamics.com/t5/Knowledge-Base/Metric-Path-CharSequence-Replacements-in-Extensions/ta-p/35412) page to get detailed instructions on configuring Metric Path Character sequence replacements in Extensions.
    

### Credentials Encryption

Please visit [this page](https://community.appdynamics.com/t5/Knowledge-Base/How-to-use-Password-Encryption-with-Extensions/ta-p/29397) to get detailed instructions on password encryption. The steps in this document will guide you through the whole process.

### Extensions Workbench
Workbench is an inbuilt feature provided with each extension in order to assist you to fine tune the extension setup before you actually deploy it on the controller. Please review the following document on [How to use the Extensions WorkBench](https://community.appdynamics.com/t5/Knowledge-Base/How-to-use-the-Extensions-WorkBench/ta-p/30130)

### Troubleshooting
1. For missing custom metrics, please refer to the KB article [here](https://community.appdynamics.com/t5/Knowledge-Base/How-to-troubleshoot-missing-custom-metrics-or-extensions-metrics/ta-p/28695)
2. In windows, if there is a java.lang.NoClassDefFoundError: org/hyperic/sigar/SigarException in machine-agent.log, please copy Windows OS related Sigar files (sigar-*.jar, sigar-amd64-winnt.dll, sigar-x86-winnt.dll) from `<MachineAgent>/lib` to `<MachineAgent>/monitorsLibs`.
3. Retrieving command line path and building regex for a process:
   
   regex in config.yml is a regular expression used to match the process and is built on the command line path to the process. 
   
   Command line of the process can be retrieved by executing ps/wmic command on a terminal in *nix/windows systems.
   
   **Unix-like systems**
   
   The args option in the command used (in config.yml) to fetch process statistics gives the command line path to the process.
   
   Linux command:
   ```
    ps -eo pid,%cpu=CPU%,%mem=Memory%,rsz=RSS,args | grep "java"
    Output: 
    2877 57.3     1.6    264708 java -jar machineagent.jar
   ```   
   command line here is "java -jar machineagent.jar" and regex could be ".*machineagent.jar"
   
   **Windows**
   
   The command line path to the process can be figured out either using Task Manager or by using wmic command.
   
   Task Manager:
   Open Task Manager and in the Processes tab, check if "Command Line" column is already displayed. If not, click on View -> Select Columns in the menu bar and checkbox the "Command Line" column to appear on the Processes page. Pick the Command Line of the process of interest.
   
   Command: 
   ```
    wmic process get /format:list | findstr java
    Output:
    Caption=java.exe
    CommandLine=jre\bin\java.exe  -jar machineagent.jar
    Description=java.exe
    ExecutablePath=C:\machineagent-bundle-32bit-windows-4.3.3.8\jre\bin\java.exe
   ```
   command line here is "C:\machineagent-bundle-32bit-windows-4.3.3.8\jre\bin\java.exe -jar machineagent.jar" and regex could be ".*java.exe -jar machineagent.jar"
4. For regex, some examples and their outcomes:
```
 process command line: "/opt/push-jobs-client/bin/ruby /opt/push-jobs-client/bin/pushy-client -c /etc/push-jobs-clientb"
   regex                         Matches
   ".*pushy-client.*"            True
   ".*pushy-client .*"           True
   ".*pushy-client  .*"          False
   ".* pushy-client.*"           False
```
Please test your regex using any of the online validators (eg: [regextester](https://www.regextester.com/)).

### Support Tickets
If after going through the [Troubleshooting Document](https://community.appdynamics.com/t5/Knowledge-Base/How-to-troubleshoot-missing-custom-metrics-or-extensions-metrics/ta-p/28695) you have not been able to get your extension working, please file a ticket and add the following information.

Please provide the following in order for us to assist you better.

    1. Stop the running machine agent.
    2. Delete all existing logs under <MachineAgent>/logs.
    3. Please enable debug logging by editing the file <MachineAgent>/conf/logging/log4j.xml. Change the level value of the following <logger> elements to debug.
        <logger name="com.singularity">
        <logger name="com.appdynamics">
    4. Start the machine agent and please let it run for 10 mins. Then zip and upload all the logs in the directory <MachineAgent>/logs/*.
    5. Attach the zipped <MachineAgent>/conf/* directory here.
    6. Attach the zipped <MachineAgent>/monitors/ExtensionFolderYouAreHavingIssuesWith directory here.

For any support related questions, you can also contact help@appdynamics.com.



### Contributing
Always feel free to fork and contribute any changes directly here on [GitHub](https://www.appdynamics.com/community/exchange/extension/process-monitoring-extension/).

### Version
|          Name            |  Version   |
|--------------------------|------------|
|Extension Version         |2.3       |
|Controller Compatibility  |4.5+|
|Agent Compatibility  |4.5.13+|
|Product Tested On         |Linux, Windows 10|
|Last Update               |21/12/2020|
