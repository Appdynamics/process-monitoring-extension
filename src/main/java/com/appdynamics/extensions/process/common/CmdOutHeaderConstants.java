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

public class CmdOutHeaderConstants {
    public static final String LINUX_PID = "PID";
    public static final String LINUX_CPU_PERCENT = "%CPU";
    public static final String LINUX_MEM_PERCENT = "%MEM";
    public static final String COMMAND = "COMMAND";

    public static final String SOLARIS_PID = "PID";
    public static final String SOLARIS_CPU = "CPU";
    public static final String SOLARIS_MEM = "SIZE";
    public static final String SOLARIS_PROC_NAME = "COMMAND";

    public static final String AIX_PID = "PID";
    public static final String AIX_CPU_PERCENT = "%CPU";
    public static final String AIX_MEM_PERCENT = "%MEM";
    public static final String AIX_PROC_NAME = "COMMAND";

    public static final String WIN_PID = "PID";
    public static final String WIN_MEM = "Mem Usage";
    public static final String WIN_PROC_NAME = "Image Name";
    public static final String WIN_CPU_PROC_NAME = "Name";
    public static final String WIN_USER_MODETIME = "UserModeTime";
    public static final String WIN_KERNEL_MODETIME = "KernelModeTime";
    public static final String WIN_CPU_PROC_ID = "ProcessId";

    public static final String HPUX_PID = "PID";
    public static final String HPUX_SIZE = "VSZ";
    public static final String HPUX_CPU = "%CPU";
    public static final String HPUX_COMMAND = "COMMAND";

}
