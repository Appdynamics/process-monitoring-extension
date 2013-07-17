# AppDynamics Processes Monitoring Extension


##Use Case

The AppDynamics Process extension observes active processes on a Linux or Windows machine and displays them in the AppDynamics Metric Browser.

The Processes extension retrieves the following metrics of each process:

-   CPU utilization in %
-   Memory utilization in MB
-   Memory utilization in %
-   Number of running instances. (If there are, for example, 3 Java processes running, this monitor will summarize them to one single report)

XML files:

-   monitor.xml: This is used to execute the Java code which starts
    the extension.
-   properties.xml: This file enables you to filter out
    which metrics are going to be reported and displayed on the metric
    browser.

##Files

Files/Folders Included:

<table><tbody>
<tr>
<th align = 'left'> Directory/File </th>
<th align = 'left'> Description </th>
</tr>
<tr>
<td class='confluenceTd'> conf </td>
<td class='confluenceTd'> Contains the monitor.xml and properties.xml </td>
</tr>
<tr>
<td class='confluenceTd'> lib </td>
<td class='confluenceTd'> Contains third-party project references </td>
</tr>
<tr>
<td class='confluenceTd'> src </td>
<td class='confluenceTd'> Contains source code to the Process Monitoring Extension </td>
</tr>
<tr>
<td class='confluenceTd'> dist </td>
<td class='confluenceTd'> Only obtained when using ant. Run 'ant build' to get binaries. Run 'ant package' to get the distributable .zip file </td>
</tr>
<tr>
<td class='confluenceTd'> build.xml </td>
<td class='confluenceTd'> Ant build script to package the project (required only if changing Java code) </td>
</tr>
</tbody>
</table>

##Installation

1. Run 'ant package' from the process-monitoring-extension directory
2. Download the file ProcessMonitor.zip located in the 'dist' directory into \<machineagent install dir\>/monitors/
3. Unzip the downloaded file
4. In \<machineagent install dir\>/monitors/ProcessMonitor/, open monitor.xml and configure the path to the properties.xml.
5. Optional but recommended. Configure a custom metric path (in monitor.xml).
6. Optional. Open properties.xml and configure the filter values.
7. Restart the Machine Agent.

In the AppDynamics Metric Browser, look for: Application Infrastructure Performance  | \<Tier\> | Custom Metrics | \<Windows/Linux\> Processes
or your specified path under Application Infrastructure Performance  | \<Tier\> |.

![](images/emoticons/information.gif) If you are running Windows,  make sure that the file 'csv.xsl' is in 'C:\Windows\System32' for 32bit or 'C:\Windows\SysWOW64' for 64bit OS versions (standard under Windows Server 2003).
If this file is not found, the process monitor will output an error to the log file (logs/machine-agent.log) .

##XML Examples

###monitor.xml

| Param | Description |
| --- | --- |
| properties\_path | Location of the properties.xml |
| metric\_path | Configuring this will limit the report of the metrics to one single tier |

~~~~
<monitor>
        <name>ProcessMonitor</name>
        <type>managed</type>
        <description>Processes monitor</description>
        <monitor-configuration></monitor-configuration>
        <monitor-run-task>
                <execution-style>continuous</execution-style>
                <name>Processes Monitor Run Task</name>
                <display-name>Processes Monitor Task</display-name>
                <description>Processes Monitor Task</description>
                <type>java</type>
                <java-task>
                        <classpath>ProcessMonitor.jar;lib/dom4j-2.0.0-ALPHA-2.jar</classpath>
                        <impl-class>main.java.com.appdynamics.monitors.processes.ProcessMonitor</impl-class>
                </java-task>
                
                <task-arguments>
               		<!-- CONFIGURE IF NECESSARY:
                		this is the path to the file properties.xml
                		i.e.: if you created a directory in 'monitors' named other than 'ProcessMonitor',
                	 	change the field 'default-value' to the appropriate directory.
                	-->
                    <argument name="properties-path" is-required="true" default-value="monitors/ProcessMonitor/properties.xml"/>
                    
                	<!-- CONFIGURE METRIC PATH (OPTIONAL):
                     		You can configure a metric path, such that only one tier is going to receive
                     		metrics from this monitor. The pattern is: Server|Component:<id or name>
                     		Component id or name is the id or name of the tier.
                     		Default (if default-value="") is "Custom Metrics|<Windows/Linux> Processes" under 
                     		Application Infrastructure Performance in every tier
                	-->
                    <argument name="metric-path" is-required="false" default-value=""/>
                </task-arguments>
        </monitor-run-task>
</monitor>
~~~~

###properties.xml

| Param | Description |
| --- | --- |
| \<exclude-processes\> | A comma-separated list of *names of processes* you want to exclude from the reported metrics |
| \<exclude-pids\> | A comma-separated list of *Process IDs (PIDs)* you want to exclude from the reported metrics |
| \<memory-threshold\> | (MB) - Processes with an aggregated absolute memory consumption of less than this number will be excluded from the reported metrics |


~~~~
<process-monitor>
	<!-- fill this tag with comma-separated names of processes you want to
		 be filtered out of the reported metrics. (OPTIONAL)
		 Example Linux  : <exclude-processes>java,bash,sshd</exclude-processes>
		 Example Windows: <exclude-processes>java.exe,chrome.exe</exclude-processes>
		 DO NOT include spaces or quotes in this tag! 
	-->
	<exclude-processes></exclude-processes>
	
	<!-- fill this tag with comma-separated Process IDs (pids) you want to
		 be filtered out of the reported metrics. (OPTIONAL)
		 Example: <exclude-processes>2,343,1235,34</exclude-processes>
		 DO NOT include spaces or quotes in this tag! 
	-->
	<exclude-pids></exclude-pids>
	
	<!-- fill this tag with a non-negative whole number. (OPTIONAL)
		 Processes with an aggregated absolute memory consumption of LESS
		 than this number in Megabytes will be filtered out of the
		 reported metrics.
		 (Fill with 0 to turn off filtering. Default value is 100 [MB])
		 Example: <memory-threshold>250</memory-threshold> 
	-->
	<memory-threshold></memory-threshold>
</process-monitor>
~~~~

##Custom Dashboard

![](images/Screen Shot 2013-06-11 at 5.52.20 PM.png)

##Metric Browser

![](images/Screen Shot 2013-06-11 at 5.52.20 PM.png)

![](images/Screen Shot 2013-06-11 at 5.24.41 PM.png)

##Metrics

###For each process the monitor is reporting for:


<table><tbody>
<tr>
<th align = 'left'> Metric Name </th>
<th align = 'left'> Description </th>
</tr>
<tr>
<td class='confluenceTd'> CPU utilization in Percent </td>
<td class='confluenceTd'></td>
</tr>
<tr>
<td class='confluenceTd'> Memory Utilization Absolute (MB) </td>
<td class='confluenceTd'></td>
</tr>
<tr>
<td class='confluenceTd'> Memory Utilization in Percent </td>
<td class='confluenceTd'></td>
</tr>
<tr>
<td class='confluenceTd'> Number of running instances </td>
<td class='confluenceTd'> Often there are multiple processes running under the same name, and this is their count. </td>
</tr>
</tbody>
</table>

##Contributing

Always feel free to fork and contribute any changes directly via GitHub.


##Support

For any support questions, please contact ace@appdynamics.com.
