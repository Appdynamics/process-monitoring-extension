# AppDynamics Process Monitoring Extension

## Version 2.3.5
* Fixed incorrect 0% CPU reporting for Windows processes
* Replaced Sigar library with OSHI for Windows process metrics collection
* CPU% on Windows is now computed as an interval measurement between consecutive polls instead of a cumulative approximation
* CPU% is skipped on the first poll cycle (no previous snapshot available) and when a PID is reused by a new process
* When a regex matches multiple processes, CPU% is summed only from processes with a valid previous snapshot

## Version 2.3.4
* Fix the issue with building the extension

## Version 2.3.3
* Add CPU & memory calculation for more processes via Regex on Windows

## Version 2.3.2
* Updated appd-exts-commons 2.2.13 to fix vulnerabilities.

## Version 2.3.1
* Fixed ErrorReader class issue

## Version 2.3
* Updated to appd-exts-commons 2.2.4

## Version 2.2
 * Updated to appd-exts-commons 2.2.3

## Version 2.1
 * Extension moved to 2.2 framework of Commons Library
 * Extension tested through the internal testing pipeline
 * Extension supports all features of the new framework like auto upload of dashboards, metric character replacer, etc.
 


## Version 2.0.2
* Moving extension to 2.0 framework.
* Minor bug fixes


## Version 1.1.1

* Main part and Windows part of the Process Monitor now have debug level logging. To see the debug level logs,
configure the log4j.xml file in your MachineAgent.
* Minor bug fixes


## Version 1.2.0

* Continued reporting of processes that fell below the memory threshold, even after restarting the machine agent. 


## Version 1.0.0

* Initial open source release


