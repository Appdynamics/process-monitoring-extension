# AppDynamics Processes - Monitoring Extension

This extension works only with the standalone machine agent.

##Use Case
The AppDynamics Process Extension observes active processes on a Linux/Windows/Solaris/AIX/HP-UX machine and displays them in the AppDynamics Metric Browser.

The Processes extension retrieves the following metrics for each process/processes group:

-   CPU utilization in %
-   Memory utilization in MB
-   Memory utilization in %

There are two ways of reporting these metrics to controller by changing the flag `displayByPid` in config.yml

1. If false, the aggregate metrics (sum) are reported. For eg. If there are multiple processes with the same name (i.e. 3 "java.exe" processes), the aggregate metrics with process group name are reported with an additional metric (Number of running instances).
2. If true, each individual process metrics are reported under PID.

**Note**: If you are running Windows,  make sure that the file 'csv.xsl' is in 'C:\Windows\System32' for 32bit or 'C:\Windows\SysWOW64' or 'C:\\Windows\\SysWOW64\\webem\\en-US\\csv.xsl' for 64bit OS versions (standard under Windows Server 2003).
If this file is not found, the process monitor will output an error to the log file (logs/machine-agent.log).

##Installation
1. To build from source, clone this repository and run 'mvn clean install'. This will produce a ProcessMonitor-VERSION.zip in the target directory. Alternatively, download the latest release archive from [Github](https://github.com/Appdynamics/process-monitoring-extension/releases/latest).
2. Unzip as "ProcessMonitor" and copy the "ProcessMonitor" directory to `<MACHINE_AGENT_HOME>/monitors`
3. Configure the extension by referring to the below section.
4. Restart the Machine Agent.

In the AppDynamics Metric Browser, look for: Application Infrastructure Performance  | \<Tier\> | Custom Metrics | \<Windows/Linux\> Processes
or your specified path under Application Infrastructure Performance  | \<Tier\> |.

## Configuration ##
Note : Please make sure to not use tab (\t) while editing yaml files. You may want to validate the yaml file using a [yaml validator](http://yamllint.com/)

1. Configure the extension by editing the config.yml file in `<MACHINE_AGENT_HOME>/monitors/ProcessMonitor/`.

   For eg.
   ```
        # Option to view process metrics in MetricBrowser per PID OR as aggregated over all the processes with same name.
        # If false, all the processes with same name are grouped and collective metrics are reported to AppDynamics Metric Browser.
        # If true, each process with PID can be viewed but since PID changes with process restart, this might not be the best option.
        displayByPid: false

        # comma-separated names of processes you want to include and exclude respectively in the reported metrics.
        #Example Linux: java,bash,sshd
        #Example Windows: java.exe,chrome.exe
        # includeProcesses: If empty, all processes are monitored. If not empty, only those specified are monitored excluding others.

        includeProcesses: []
        excludeProcesses: []

        # Processes with an aggregated absolute memory consumption of LESS than this number
        # in Megabytes will be filtered out of the reported metrics. Default value is 100 [MB]
        memoryThreshold: 100

        # ONLY for OS - WINDOWS
        # csv.xsl file path - leave null for default location,
        # i.e. C:/Windows/SysWOW64/csv.xsl OR C:\\Windows\\SysWOW64\\webem\\en-US\\csv.xsl (for 64bit)
        # OR C:/Windows/System32/csv.xsl (32 bit)
        csvFilePath: ""

        # this is the path to the file .monitored-processes
        monitoredProcessFilePath: "monitors/ProcessMonitor/.monitored-processes"

        metricPrefix: "Custom Metrics|"

   ```

3. Configure the path to the config.yml file by editing the <task-arguments> in the monitor.xml file in the `<MACHINE_AGENT_HOME>/monitors/ProcessMonitor/` directory. Below is the sample

     ```
     <task-arguments>
         <!-- config file-->
         <argument name="config-file" is-required="true" default-value="monitors/ProcessMonitor/config.yml" />
          ....
     </task-arguments>
    ```


**Note** : By default, a Machine agent or a AppServer agent can send a fixed number of metrics to the controller. To change this limit, please follow the instructions mentioned [here](http://docs.appdynamics.com/display/PRO14S/Metrics+Limits).
For eg.  
```    
    java -Dappdynamics.agent.maxMetrics=2500 -jar machineagent.jar
```

##Custom Dashboard
![](http://appsphere.appdynamics.com/t5/image/serverpage/image-id/95i5C555106398901A2/image-size/original?v=mpbl-1&px=-1)

##Metric Browser
![](http://appsphere.appdynamics.com/t5/image/serverpage/image-id/93iED3BE531B3AE0FFC/image-size/original?v=mpbl-1&px=-1)

![](http://appsphere.appdynamics.com/t5/image/serverpage/image-id/97iCA9AA07958232EAD/image-size/original?v=mpbl-1&px=-1)


##Contributing
Always feel free to fork and contribute any changes directly here on GitHub.

##Community

Find out more in the [AppSphere](https://www.appdynamics.com/community/exchange/extension/process-monitoring-extension/) community.

##Support

For any questions or feature request, please contact [AppDynamics Support](mailto:help@appdynamics.com).
