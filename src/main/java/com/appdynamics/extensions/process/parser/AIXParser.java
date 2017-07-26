package com.appdynamics.extensions.process.parser;

import com.appdynamics.extensions.process.data.ProcessData;

import java.util.Map;

public class AIXParser implements Parser {
    public Map<String, ProcessData> parseProcesses(Map<String, ?> config) {
        return null;
    }

    public String getProcessGroupName() {
        return "AIX Processes";
    }
}
