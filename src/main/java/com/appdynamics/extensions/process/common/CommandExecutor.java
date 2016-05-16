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

import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.List;


public class CommandExecutor {
    private static Logger logger = Logger.getLogger(CommandExecutor.class);

    public Process execute(String command) throws CommandExecutorException {
        Runtime rt = Runtime.getRuntime();
        Process p;
        try {
            p = rt.exec(command);
            logger.debug("Executed command " + command);
        } catch (IOException e) {
            logger.error("Error in executing the command " + e);
            throw new CommandExecutorException("Execution failed with message " + e.getMessage(), e);
        }
        return p;
    }

    public Process execute(String command, List<String> env) throws CommandExecutorException {
        Runtime rt = Runtime.getRuntime();
        Process p;
        try {
            p = rt.exec(command, env.toArray(new String[1]));
        } catch (IOException e) {
            logger.error("Error in executing the command " + e);
            throw new CommandExecutorException("Execution failed with message " + e.getMessage(), e);
        }
        return p;
    }
}
