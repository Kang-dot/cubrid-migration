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
package com.cubrid.cubridmigration.core.engine.task.imp;

import com.cubrid.cubridmigration.core.common.CUBRIDIOUtils;
import com.cubrid.cubridmigration.core.common.log.LogUtil;
import com.cubrid.cubridmigration.core.dbobject.PlcsqlFunction;
import com.cubrid.cubridmigration.core.engine.config.MigrationConfiguration;
import com.cubrid.cubridmigration.core.engine.task.ImportTask;
import com.cubrid.cubridmigration.cubrid.CUBRIDSQLHelper;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;

/**
 * Output original and drop SQL for procedure
 *
 * @author Dongmin Kim
 */
public class AllPlcsqlFunctionHeaderDDLTask extends ImportTask {

    private static final Logger LOG = LogUtil.getLogger(AllPlcsqlFunctionHeaderDDLTask.class);
    private final MigrationConfiguration config;

    public AllPlcsqlFunctionHeaderDDLTask(MigrationConfiguration config) {
        this.config = config;
    }

    @Override
    protected void executeImport() {
        List<PlcsqlFunction> targetFunctions = config.getTargetPlcsqlFunctionSchema();
        Map<String, String> functionFiles = config.getTargetAllPlcsqlFunctionHeaderFileName();

        for (PlcsqlFunction function : targetFunctions) {
            String owner = function.getOwner();

            CUBRIDSQLHelper sqlHelper = CUBRIDSQLHelper.getInstance(null);

            String ddl =
                    sqlHelper.getPlcsqlFunctionHeaderDDL(function, config.isAddUserSchema())
                            + ";\n";
            File functionFile = new File(functionFiles.get(owner));

            try {
                CUBRIDIOUtils.writeLines(functionFile, new String[] {ddl}, true);
            } catch (IOException e) {
                LOG.error("AllPlcsqlFunctionHeaderDDLTask");
            }
        }
    }
}