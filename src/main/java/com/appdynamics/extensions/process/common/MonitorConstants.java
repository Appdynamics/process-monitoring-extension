/*
 * Copyright 2020 AppDynamics LLC and its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.appdynamics.extensions.process.common;

public class MonitorConstants {
    public static final String AIX = "aix";
    public static final String AIX_PROCESSES = "AIX Processes";
    public static final String CPU_PERCENT = "CPU%";
    public static final String CUSTOM_METRICS = "Custom Metrics";
    public static final String DISPLAY_NAME = "displayName";
    public static final String HP_UX = "hp-ux";
    public static final String INSTANCES = "instances";
    public static final String LINUX = "linux";
    public static final String LINUX_PROCESSES = "Linux Processes";
    public static final String METRICS = "metrics";
    public static final String METRIC_SEPARATOR = "|";
    public static final String MONITOR_NAME = "Process Monitor";
    public static final String OS_NAME = "os.name";
    public static final String PID_FILE = "pidFile";
    public static final String PID_LOWERCASE = "pid";
    public static final String PROCESS = "process";
    public static final String REGEX = "regex";
    public static final String RSS = "RSS";
    public static final String SOLARIS = "solaris";
    public static final String SOLARIS_PROCESSES = "Solaris Processes";
    public static final String SPACES = "\\s+";
    public static final String SUNOS = "sunos";
    public static final String WINDOWS = "win";
    public static final String WINDOWS_PROCESSES = "Windows Processes";

    // Metric Constants
    public static final String RUNNING_INSTANCES_COUNT = "Running Instances";

    // Process Line Header Constants
    public static final String PID = "PID";
    public static final String COMMAND = "COMMAND";

    // Commands
    //Linux
    public static final String LINUX_PROCESS_LIST_COMMAND = "ps -eo pid,%cpu=CPU%,%mem=Memory%,rss=RSS,args";

    // Solaris
    public static final String SOLARIS_PROCESS_LIST_COMMAND = "ps -eo pid,pcpu=CPU% -o pmem=Memory% -o rss=RSS -o args";

    // AIX
    public static final String AIX_PROCESS_LIST_COMMAND = "ps -eo pid,pcpu=CPU%,pmem=Memory%,rss=RSS,args";

    // HP-UX
    // For Memory machinfo | grep -i memory doesn't work on HP-UX 11.1x, so parsing top command
    public static final String HPUX_TOP_COMMAND = "top -d 1";
    // UNIX95= ps -eo pid,pcpu,vsz,args
    public static final String HPUX_PROCESS_COMMAND = "ps -eo pid,pcpu,vsz,args";
}
