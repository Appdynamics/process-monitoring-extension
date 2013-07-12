# AppDynamics Processes Monitoring Extension

- [Use Case](processes-readme.md#use-case)
- [Installation](processes-readme.md#installation)
- [XML Example](processes-readme.md#xml-examples)
    - [monitor.xml](processes-readme.md#monitorxml)
    - [properties.xml](processes-readme.md#propertiesxml)
- [Custom Dashboard](processes-readme.md#customdashboard)
- [Metric Browser](processes-readme.md#metric-browser)
- [Content of processes.tar.gz](processes-readme.md#processes-content-of-processestargz)
- [Contributing](processes-readme.md#contributing)

##Use Case

The AppDynamics Process extension observes active processes on a Linux or Windows machine and displays them in the AppDynamics Metric Browser.

The Processes extension retrieves the following metrics of each process:

-   CPU utilization in %
-   Memory utilization in MB
-   Memory utilization in %
-   Number of running instances. (If there are, for example, 3 Java processes running, this monitor will summarize them to one single report)

XML files:

-   monitor.xml: This is used to execute the Java code which starts
    the extension. You might need to configure the path to the other xml
    file (see
    [monitor.xml](http://docs.appdynamics.com/display/ACE/Processes?sortBy=name#Processes-monitor.xml) (requires login)).
-   properties.xml: This file enables you to filter out
    which metrics are going to be reported and displayed on the metric
    browser (see
    [properties.xml](http://docs.appdynamics.com/display/ACE/Processes?sortBy=name#Processes-properties.xml) (requires login)).

##Contents of processes.tar.gz

| Directory/File | Description |
| --- | --- |
| bin | Contains class files |
| conf | Contains the monitor.xml and properties.xml |
| dist | Contains the distribution package (monitor.xml, properties.xml, the lib directory, and processes.jar) |
| lib | Contains Third-Party project references |
| src | Contains source code to this Process Monitor |
| build.xml | Ant build script to package the project (only required if changing java code) |

##Installation

1.  Go to the Machine Agent directory and create a new directory in ../machineagent/monitors (such as ProcessExtension).
2.  Copy over the contents in the 'dist' folder to the folder made in step 1.
3.  Open monitor.xml and configure the path to the properties.xml (if the directory from step 1 is named other than ProcessExtension)
4.  Optional. Open properties.xml and configure the filter values.
5.  Restart the Machine Agent.

The Metrics will be uploaded to "Custom Metrics| \<Windows/Linux\> Processes|\<Process name\>

![](images/emoticons/information.gif) If you are running Windows,  make sure that the file 'csv.xsl'
is in 'C:\Windows\System32' for 32bit or 'C:\Windows\SysWOW64' for 64bit OS versions.
If this file is not found, the process monitor will output an error to the log file (logs/machine-agent.log) .

##XML Examples

###monitor.xml

| Param | Description |
| --- | --- |
| properties\_path | Location of the properties.xml |

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
			    <classpath>processes.jar;lib/dom4j-2.0.0-ALPHA-2.jar</classpath>
			    <impl-class>com.appdynamics.monitors.processes.ProcessMonitor</impl-class>
		    </java-task>
		    <!-- CONFIGURE IF NECESSARY:
			 this is the path to the file properties.xml
			 i.e.: if you created a directory in 'monitors' named other than 'ProcessMonitor',
			 change the field 'default-value' to the appropriate directory.
		    -->
		    <task-arguments>
			    <argument name="properties-path" is-required="true" default-value="monitors/ProcessMonitor/properties.xml"/>
		    </task-arguments>
	    </monitor-run-task>
    </monitor>

###properties.xml

| Param | Description |
| --- | --- |
| \<exclude-processes\> | A comma-separated list of *names of processes* you want to exclude from the reported metrics |
| \<exclude-pids\> | A comma-separated list of *Process IDs (PIDs)* you want to exclude from the reported metrics |
| \<memory-threshold\> | (MB) - Processes with an aggregated absolute memory consumption of less than this number will be excluded from the reported metrics |



    <process-monitor> fill this tag with comma-separated names of processes you want to be filtered out of the reported metrics. (OPTIONAL)
     <memory-threshold></memory-threshold><process-monitor>
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

##Custom Dashboard

![](images/Screen Shot 2013-06-11 at 5.52.20 PM.png)

##Metric Browser

![](images/Screen Shot 2013-06-11 at 5.52.20 PM.png)

![](images/Screen Shot 2013-06-11 at 5.24.41 PM.png)



##Contributing

Always feel free to fork and contribute any changes directly via GitHub.


##Support

For any support questions, please contact ace@appdynamics.com.
