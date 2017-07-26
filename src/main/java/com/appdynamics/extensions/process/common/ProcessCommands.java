/**
 * Copyright 2015 AppDynamics
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


public class ProcessCommands {
    //Linux
    public static final String LINUX_MEMORY_COMMAND = "cat /proc/meminfo";
    public static final String LINUX_PROCESS_NAME_COMMAD = "cat /proc/%s/status";
    public static final String LINUX_PROCESS_LIST_COMMAND = "ps -eo pcpu:5,pmem:5,command";

    // Windows
    public static final String WINDOWS_MEMORY_COMMAND = "wmic OS get TotalVisibleMemorySize";
    public static final String WINDOWS_CPU_COMMAD = "wmic process get name,processid,usermodetime,kernelmodetime /format:";
    public static final String WINDOWS_PROCESS_LIST_COMMAND = "tasklist /fo csv";

    // Solaris
    public static final String SOLARIS_PROCESS_LIST_COMMAND = "top -b";

    // AIX
    public static final String AIX_MEMORY_COMMAND = "getconf REAL_MEMORY";
    public static final String AIX_PROCESS_LIST_COMMAND = "ps -eo pid,pcpu,pmem,command";

    // HP-UX
    // For Memory machinfo | grep -i memory doesn't work on HP-UX 11.1x, so parsing top command
    public static final String HPUX_TOP_COMMAND = "top -d 1";
    // UNIX95= ps -eo pid,pcpu,vsz,args
    public static final String HPUX_PROCESS_COMMAND = "ps -eo pid,pcpu,vsz,args";
}
