/**
 * Copyright 2016 AppDynamics
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.appdynamics.extensions.process.common;

public class MonitorConstants {

    public static final String METRIC_SEPARATOR = "|";
    public static final String SPACES = "\\s+";

    // Metric Constants
    public static final String RUNNING_INSTANCES_COUNT = "Running Instances";

    // Process Line Header Constants
    public static final String PID = "PID";
    public static final String COMMAND = "COMMAND";


    // Commands
    //Linux
    public static final String LINUX_PROCESS_LIST_COMMAND = "ps -eo pid,%cpu=CPU%,%mem=Memory%,rsz=RSS,args";

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
