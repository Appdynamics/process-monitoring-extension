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


public class CommandHeaderInfo {
    private int posPID;
    private int posCPU;
    private int posMem;
    private int posProcessName;

    public int getPosPID() {
        return posPID;
    }

    public void setPosPID(int posPID) {
        this.posPID = posPID;
    }

    public int getPosCPU() {
        return posCPU;
    }

    public void setPosCPU(int posCPU) {
        this.posCPU = posCPU;
    }

    public int getPosMem() {
        return posMem;
    }

    public void setPosMem(int posMem) {
        this.posMem = posMem;
    }

    public int getPosProcessName() {
        return posProcessName;
    }

    public void setPosProcessName(int posProcessName) {
        this.posProcessName = posProcessName;
    }
}
