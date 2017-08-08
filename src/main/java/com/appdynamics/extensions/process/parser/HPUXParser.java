package com.appdynamics.extensions.process.parser;

import com.appdynamics.extensions.process.data.ProcessData;

import java.util.Map;

public class HPUXParser implements Parser {
    public Map<String, ProcessData> parseProcesses(Map<String, ?> config) {
        return null;
    }

    public String getProcessGroupName() {
        return "HP-UX Processes";
    }
}
