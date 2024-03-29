/*
 * Copyright 2020 AppDynamics LLC and its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.appdynamics.extensions.process.common;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;


public class CommandExecutorTest {

    private CommandExecutor commandExecutor;

    @Before
    public void setup() {
        commandExecutor = new CommandExecutor();
    }

    @Test
    public void testCommand2() {
        List<String> processLines = commandExecutor.execute("abc");
        Assert.assertNull(processLines);
    }


}
