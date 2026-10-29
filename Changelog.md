# AppDynamics Process Monitoring Extension

## Version 2.3.6
* Updated appd-exts-commons 2.2.13 to 2.2.21 to fix vulnerabilities (Jetty 12.1.12, commons-lang3 3.20.0)
* Pinned Jackson to 2.22.3 to fix CVE-2026-89407, CVE-2026-89425, CVE-2026-91776, CVE-2026-91777
* Build now targets Java 17; the Machine Agent must run on Java 17 or later
* Updated test dependencies (PowerMock 2.0.9, Mockito 3.12.4, JUnit 4.13.2) for Java 17

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


