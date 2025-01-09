/*
 * Copyright (C) 2016 CUBRID Corporation.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * - Neither the name of the <ORGANIZATION> nor the names of its contributors
 *   may be used to endorse or promote products derived from this software without
 *   specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY
 * OF SUCH DAMAGE.
 *
 */
package com.cubrid.cubridmigration.core.engine.config;

/**
 * SourceFunctionConfig Description
 *
 * @author Dongmin Kim
 */
public class SourcePlcsqlFunctionConfig extends SourceConfig {
    private final String owner;
    private final String targetOwner;
    private final String authid;
    private final boolean authidChanged;

    private final String sourceDDL;
    private final String headerDDL;
    private final String bodyDDL;
    private final String functionDDL;

    public SourcePlcsqlFunctionConfig(
            String owner,
            String targetOwner,
            String name,
            String targetName,
            String authid,
            boolean authidChanged,
            String sourceDDL,
            String headerDDL,
            String bodyDDL,
            String functionDDL) {
        this.owner = owner;
        this.targetOwner = targetOwner;
        this.setName(name);
        this.setTarget(targetName);
        this.authid = authid;
        this.authidChanged = authidChanged;
        this.sourceDDL = sourceDDL;
        this.headerDDL = headerDDL;
        this.bodyDDL = bodyDDL;
        this.functionDDL = functionDDL;
    }

    public String getOwner() {
        return owner;
    }

    public String getTargetOwner() {
        return targetOwner;
    }

    public String getAuthid() {
        return authid;
    }

    public boolean isAuthidChanged() {
        return authidChanged;
    }

    public String getSourceDDL() {
        return sourceDDL;
    }

    public String getHeaderDDL() {
        return headerDDL;
    }

    public String getBodyDDL() {
        return bodyDDL;
    }

    public String getFunctionDDL() {
        return functionDDL;
    }
}