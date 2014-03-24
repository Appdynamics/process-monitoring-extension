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


package com.appdynamics.monitors.processes.parser;

import com.appdynamics.monitors.processes.processdata.ProcessData;
import com.appdynamics.monitors.processes.processexception.ProcessMonitorException;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.*;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class Parser {

	private final int DEFAULT_MEM_THRESHOLD = 100;
	private String properties;
	private int memoryThreshold;
	
	protected Set<String> includeProcesses;
	protected List<String> excludeProcesses;
	protected List<Integer> excludePIDs;
	protected Map<String, ProcessData> processes;	
	private int totalMemSizeMB;
	Logger logger;

	public String processGroupName;
	public String monitoredProcessFilePath;

	public abstract void initialize() throws ProcessMonitorException;

	public abstract void parseProcesses() throws ProcessMonitorException;	

	public Parser(Logger logger){
		this.logger = logger;
	}
	/**
	 * Parses the properties file for the Process Monitor
	 * @param xml: The path of the xml file
	 * @throws DocumentException
	 */
	public void parseXML(String xml) throws DocumentException {
		properties = xml;
		SAXReader reader = new SAXReader();
		Document document = reader.read(xml);
		Element root = document.getRootElement();
		String text;
		// might be altered further down:
		memoryThreshold = DEFAULT_MEM_THRESHOLD;
		for (Iterator<Element> i = root.elementIterator(); i.hasNext();) {
			Element element = (Element)i.next();

			if (element.getName().equals("exclude-processes")){
				if(!(text = element.getText()).equals("")) {					
					String[] procs = text.split(",");
					for(String proc : procs){
						excludeProcesses.add(proc);
					}
				}

			} else if (element.getName().equals("exclude-pids")){
				if(!(text = element.getText()).equals("")) {					

					String[] pids = text.split(",");
					for(String pidword : pids){
						try{
							excludePIDs.add(Integer.parseInt(pidword));
						} catch (NumberFormatException e){
							logger.error(properties + ": You can only provide a whole number as a pid! " +
									"Ignoring entry: " + pidword);						
						}
					}
				}

			} else if(element.getName().equals("memory-threshold")) {
				if(!(text = element.getText().trim()).equals("")){
					try{
						memoryThreshold = Integer.parseInt(text);					
						if(memoryThreshold < 0){
							logger.error(properties + "You can only provide a non-negative whole number as a memory threshold! " +
									"Threshold set to default (" + DEFAULT_MEM_THRESHOLD + "MB)");
							memoryThreshold = DEFAULT_MEM_THRESHOLD;
						} else {
							logger.info("Memory threshold set to " + memoryThreshold + " MB");
						}
					} catch (NumberFormatException e){
						logger.error(properties + "You can only provide a non-negative whole number as a memory threshold! " +
								"Threshold set to default (" + DEFAULT_MEM_THRESHOLD + " MB)");
						memoryThreshold = DEFAULT_MEM_THRESHOLD;
					}
				}

			} else {
				logger.warn("Unknown Element " + element.getName() + " found in " + properties);
			}
		}
	}
	
	public void readProcsFromFile(){
		BufferedReader br = null;
        try {
			br = new BufferedReader(new FileReader(monitoredProcessFilePath));
			String line;
			includeProcesses.clear();
			while((line = br.readLine()) != null){
				includeProcesses.add(line);
			}
		} catch (FileNotFoundException e) {
			logger.warn("the file .monitoredProcesses.txt could not be found. " +
					"This might be the first time trying to read in from the file, " +
					"and the set of monitored processes is set to be empty.");
		} catch (IOException e) {
			logger.warn("A problem occurred reading from the .monitoredProcesses file.");
		}
        finally {
        	if(br !=null)
            closeBufferedReader(br);
        }
	}
	
	public void writeProcsToFile(){
		BufferedWriter wr = null;
		try {
			wr = new BufferedWriter(new FileWriter(monitoredProcessFilePath));
			wr.write("");
			wr.close();
			
			wr = new BufferedWriter(new FileWriter(monitoredProcessFilePath));
			for(String process : includeProcesses){
				wr.write(process);
				wr.newLine();
			}

		} catch (IOException e) {
			logger.warn("Can't write process names to/create file '.monitored-processes' in ProcessMonitor directory.");
		}
        catch(NullPointerException e) {
            logger.error("NullPointerException: ", e);
        }
        finally {
            closeBufferedWriter(wr);
        }
	}

	public int getDefaultMemoryThreshold() {
		return DEFAULT_MEM_THRESHOLD;
	}

	public int getMemoryThreshold() {
		return memoryThreshold;
	}

	public void setMemoryThreshold(int memoryThreshold) {
		this.memoryThreshold = memoryThreshold;
	}

	public List<String> getExcludeProcesses() {
		return excludeProcesses;
	}

	public void setExcludeProcesses(List<String> excludeProcesses) {
		this.excludeProcesses = excludeProcesses;
	}

	public List<Integer> getExcludePIDs() {
		return excludePIDs;
	}

	public void setExcludePIDs(List<Integer> excludePIDs) {
		this.excludePIDs = excludePIDs;
	}

	public Map<String, ProcessData> getProcesses() {
		return processes;
	}

	public void setProcesses(Map<String, ProcessData> processes) {
		this.processes = processes;
	}

	public Logger getLogger() {
		return logger;
	}

	public void setLogger(Logger logger) {
		this.logger = logger;
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

	public void addIncludeProcesses(String name){
		if(this.includeProcesses.add(name)){
			logger.debug("New Process added to the list of metrics to be permanently" +
					"reported, even if falling back below threshold: " + name);
		}
	}

    protected void cleanUpProcess(Process p) {
        try {
            p.waitFor();
            p.getInputStream().close();
            p.getOutputStream().close();
            p.getErrorStream().close();
            p.destroy();
        } catch(IOException e) {
            logger.error("IOException: ", e);
        } catch(InterruptedException e) {
            logger.error("InterruptedException: ", e);
        } catch(NullPointerException e) {
            logger.error("NullPointerException: ", e);
        } catch(Exception e) {
            logger.error("Exception: ", e);
        }
    }
    protected void closeBufferedReader(BufferedReader reader) {
        try {
            reader.close();
        } catch (IOException e) {
            logger.error("IOException: ", e);
        } catch(NullPointerException e) {
            logger.error("NullPointerException: ", e);
        }
    }

    protected void closeBufferedWriter(BufferedWriter writer) {
        try {
            writer.close();
        } catch (IOException e) {
            logger.error("IOException: ", e);
        } catch(NullPointerException e) {
            logger.error("NullPointerException: ", e);
        }
    }
}
