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
package com.appdynamics.extensions.process.configuration;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class ConfigProcessor {

    private static final Logger logger = Logger.getLogger(ConfigProcessor.class);

    public List<Instance> processConfig(Map<String, ?> config) {
        List<Map> configuredProcesses = (List) config.get("instances");
        List<Instance> instances = Lists.newArrayList();
        for (Map configuredProcess : configuredProcesses) {
            Instance instance = new Instance();
            String displayName = (String) configuredProcess.get("displayName");
            String regex = (String) configuredProcess.get("regex");
            String pid = (String) configuredProcess.get("pid");
            String pidFile = (String) configuredProcess.get("pidFile");

            if (!Strings.isNullOrEmpty(displayName)) {
                instance.setDisplayName(displayName);
            } else {
                logger.error("displayName null or empty, skipping ");
                break;
            }
            instance.setRegex(regex);
            String pidStr = getPid(pid, pidFile);
            instance.setPid(pidStr);

            instances.add(instance);
        }
        return instances;
    }

    private String getPid(String pid, String pidFile) {
        if (!Strings.isNullOrEmpty(pid)) {
            return pid;
        } else if (!Strings.isNullOrEmpty(pidFile)) {
            File file = new File(pidFile);
            try {
                String fileOutputString = FileUtils.readFileToString(file);
                logger.debug("Contents of pidFile at " + pidFile + " is " + fileOutputString);
                return fileOutputString.trim();
            } catch (IOException e) {
                logger.error("Error while reading pidFile " + pidFile, e);
            }
        }
        return null;
    }

}
