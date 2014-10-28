/**
 * Copyright 2013 AppDynamics
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

package com.appdynamics.extensions.process.parser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.appdynamics.extensions.process.ProcessMonitor;
import com.appdynamics.extensions.process.config.Configuration;
import com.appdynamics.extensions.process.processdata.ProcessData;
import com.appdynamics.extensions.process.processexception.ProcessMonitorException;

public abstract class Parser {

	private final int DEFAULT_MEM_THRESHOLD = 100;
	protected Set<String> includeProcesses = new HashSet<String>();
	protected Map<String, ProcessData> processes = new HashMap<String, ProcessData>();
	private int totalMemSizeMB;
	private static final Logger logger = Logger.getLogger("com.singularity.extensions.Parser");

	public String processGroupName;
	protected Configuration config;
	private String monitoredProcessFilePath;

	public Parser(Configuration config) {
		this.config = config;
		this.monitoredProcessFilePath = ProcessMonitor.getConfigFilename(this.config.getMonitoredProcessFilePath());
	}

	public abstract void retrieveMemoryMetrics() throws ProcessMonitorException;

	public abstract void parseProcesses() throws ProcessMonitorException;

	public void readProcsFromFile() {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(monitoredProcessFilePath));
			String line;
			while ((line = br.readLine()) != null) {
				includeProcesses.add(line);
			}
		} catch (FileNotFoundException e) {
			logger.warn("the file .monitoredProcesses.txt could not be found. " + "This might be the first time trying to read in from the file, "
					+ "and the set of monitored processes is set to be empty.");
		} catch (IOException e) {
			logger.warn("A problem occurred reading from the .monitoredProcesses file.");
		} finally {
			if (br != null)
				closeBufferedReader(br);
		}
	}

	public void writeProcsToFile() {
		BufferedWriter wr = null;
		try {
			wr = new BufferedWriter(new FileWriter(monitoredProcessFilePath));
			wr.write("");
			wr.close();

			wr = new BufferedWriter(new FileWriter(monitoredProcessFilePath));
			for (String process : includeProcesses) {
				wr.write(process);
				wr.newLine();
			}
		} catch (IOException e) {
			logger.warn("Can't write process names to/create file '.monitored-processes' in ProcessMonitor directory.", e);
		} catch (Exception e) {
			logger.error("Exception: ", e);
		} finally {
			closeBufferedWriter(wr);
		}
	}

	public int getMemoryThreshold() {
		if (config.getMemoryThreshold() == 0) {
			return DEFAULT_MEM_THRESHOLD;
		} else {
			return config.getMemoryThreshold();
		}
	}

	public Map<String, ProcessData> getProcesses() {
		return processes;
	}

	public void setProcesses(Map<String, ProcessData> processes) {
		this.processes = processes;
	}

	public int getTotalMemSizeMB() {
		return totalMemSizeMB;
	}

	public void setTotalMemSizeMB(int totalMemSizeMB) {
		this.totalMemSizeMB = totalMemSizeMB;
	}

	public Set<String> getIncludeProcesses() {
		return includeProcesses;
	}

	public void setIncludeProcesses(Set<String> includeProcesses) {
		this.includeProcesses = includeProcesses;
	}

	public void addIncludeProcesses(String name) {
		if (this.includeProcesses.add(name)) {
			logger.debug("New Process added to the list of metrics to be permanently" + "reported, even if falling back below threshold: " + name);
		}
	}

	protected class ErrorHandler extends Thread {
		InputStream is;
		StringBuilder sb = new StringBuilder();
		BufferedReader br = null;

		public ErrorHandler(InputStream is) {
			this.is = is;
		}

		public void run() {
			br = new BufferedReader(new InputStreamReader(is));
			String line = null;
			try {
				while ((line = br.readLine()) != null) {
					sb.append(line).append("\n");
				}
			} catch (IOException e) {
				logger.error("Error in accessing the error stream " + e);
			}
		}
	}

	protected void handleErrorsIfAny(InputStream is) throws ProcessMonitorException {
		ErrorHandler error = new ErrorHandler(is);
		error.start();
		StringBuilder sb = error.sb;
		if (sb.length() != 0) {
			logger.error(sb.toString());
			throw new ProcessMonitorException(sb.toString());
		}
	}

	protected void parseErrorsIfAny(BufferedReader error) throws ProcessMonitorException {
		String line;
		StringBuilder sb = new StringBuilder();
		// read any errors from the attempted command
		try {
			while ((line = error.readLine()) != null) {
				sb.append(line).append("\n");
			}
		} catch (IOException e) {
			logger.error("Error in accessing the error stream " + e);
		}
		if (sb.length() != 0) {
			logger.error(sb.toString());
			throw new ProcessMonitorException(sb.toString());
		}
	}

	protected void cleanUpProcess(Process p, String cmd) {
		try {
			if (p != null) {
				int exitValue = p.waitFor();
				if (exitValue != 0) {
					logger.warn("Unable to terminate the command " + cmd + " normally. ExitValue = " + exitValue);
				}
				p.destroy();
			}
		} catch (InterruptedException e) {
			logger.error("Execution of command " + cmd + " got interrupted ", e);
		}
	}

	protected void closeBufferedReader(BufferedReader reader) {
		if (reader != null) {
			try {
				reader.close();
			} catch (IOException e) {
				logger.error("Exception while closing the reader: ", e);
			}
		}
	}

	protected void closeBufferedWriter(BufferedWriter writer) {
		if (writer != null) {
			try {
				writer.close();
			} catch (IOException e) {
				logger.error("Exception while closing the writer: ", e);
			}
		}
	}

	public void executeCommand(String command) throws ProcessMonitorException {
		Runtime rt = Runtime.getRuntime();
		Process p = null;
		try {
			p = rt.exec(command);
			process(p);
			int exitValue = p.waitFor();
			if (exitValue != 0) {
				logger.error("Unable to terminate the command normally. ExitValue = " + exitValue);
			}
			p.destroy();
		} catch (IOException e) {
			logger.error("Error in executing the command " + command, e);
			throw new ProcessMonitorException("Error in executing the command " + command, e);
		} catch (InterruptedException e) {
			logger.error("Execution of command " + command + " got interrupted ", e);
			throw new ProcessMonitorException("Execution of command " + command + " got interrupted ", e);
		}
	}

	private void process(Process p) throws ProcessMonitorException {
		BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
		BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));

		String s;
		StringBuilder sb = new StringBuilder();
		// read any errors from the attempted command
		try {
			while ((s = stdError.readLine()) != null) {
				sb.append(s).append("\n");
			}
		} catch (IOException e) {
			logger.error("Error in accessing the error stream " + e);
		}
		if (sb.length() != 0) {
			logger.error(sb.toString());
			throw new ProcessMonitorException(sb.toString());
		}
		try {
			if ((s = stdInput.readLine()) != null) {
				// System.out.println(s);
			}
		} catch (IOException e) {
			logger.error("Error in accessing the stdInput stream " + e);
		}
		closeBufferedReader(stdInput);
		closeBufferedReader(stdError);
	}
}
