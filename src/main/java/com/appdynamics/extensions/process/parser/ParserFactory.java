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
package com.appdynamics.extensions.process.parser;

import org.apache.log4j.Logger;

public class ParserFactory {

    public static final Logger logger = Logger.getLogger(ParserFactory.class);

    public static Parser createParser(String os) {

        if (os.contains("linux")) {
            return new LinuxParser();
        } else if (os.contains("win")) {
            return new WindowsParser();
        } else if (os.contains("sunos")){
            return new SolarisParser();
        } else if (os.contains("aix")) {
            return new AIXParser();
        } else if (os.contains("hp-ux")) {
            return new HPUXParser();
        } else {
            logger.error("Error in initializing Parser for " + os);
            return null;
        }

    }
}
