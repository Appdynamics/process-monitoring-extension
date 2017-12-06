package com.appdynamics.extensions.process.parser;

import com.appdynamics.extensions.process.data.ProcessData;

import java.util.Map;

public class HPUXParser extends Parser {
    public Map<String, ProcessData> fetchMetrics(Map<String, ?> config) {
        return null;
    }

    public String getProcessGroupName() {
        return "HP-UX Processes";
    }

    protected Map<String, String> getCommands(Map<String, ?> config) {
        return null;
    }
}
