# AppDynamics Processes - Monitoring Extension

This extension works only with the standalone machine agent.

##Use Case
The AppDynamics Process extension observes active processes on a Linux or Windows machine and displays them in the AppDynamics Metric Browser.

The Processes extension retrieves the following metrics of each process:

-   CPU utilization in %
-   Memory utilization in MB
-   Memory utilization in %
-   Number of running instances. (If there are, for example, 3 Java processes running, this monitor will report them individually)

**Note**: If you are running Windows,  make sure that the file 'csv.xsl' is in 'C:\Windows\System32' for 32bit or 'C:\Windows\SysWOW64' for 64bit OS versions (standard under Windows Server 2003).
If this file is not found, the process monitor will output an error to the log file (logs/machine-agent.log) .


##Installation
1. Run 'mvn clean install' from the process-monitoring-extension directory and find the ProcessMonitor.zip in the "target" folder.
2. Unzip as "ProcessMonitor" and copy the "ProcessMonitor" directory to `<MACHINE_AGENT_HOME>/monitors`
3. Configure the extension by referring to the below section.
4. Restart the Machine Agent.

In the AppDynamics Metric Browser, look for: Application Infrastructure Performance  | \<Tier\> | Custom Metrics | \<Windows/Linux\> Processes
or your specified path under Application Infrastructure Performance  | \<Tier\> |.

NOTE: If there are multiple processes with the same name (i.e. 3 "java.exe" processes), they will be identified by their PID in the Metric Browser.

## Configuration ##
Note : Please make sure to not use tab (\t) while editing yaml files. You may want to validate the yaml file using a [yaml validator](http://yamllint.com/)

1. Configure the extension by editing the config.yml file in `<MACHINE_AGENT_HOME>/monitors/ProcessMonitor/`.

   For eg.
   ```
        # No of times the commands are executed to fetch metrics in 1 min
        fetchesPerInterval: 2

        # comma-separated names of processes you want to exclude out of the reported metrics.
        #Example Linux  : java,bash,sshd
        #Example Windows: java.exe,chrome.exe
        excludeProcesses: [ ]

        # comma-separated Process IDs (pids) to be excluded out of the reported metrics.
        # Example: 2,343,1235,34
        excludePIDs: [ ]

        # Processes with an aggregated absolute memory consumption of LESS than this number
        # in Megabytes will be filtered out of the reported metrics. Default value is 100 [MB]
        memoryThreshold: 100

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

Find out more in the [AppSphere](http://appsphere.appdynamics.com/t5/Extensions/Process-Monitoring-Extension/idi-p/1069) community.

##Support

For any questions or feature request, please contact [AppDynamics Support](mailto:help@appdynamics.com).
