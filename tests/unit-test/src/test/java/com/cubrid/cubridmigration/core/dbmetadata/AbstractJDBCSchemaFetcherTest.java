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
 *   and/or other materials provided with the distribution
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
package com.cubrid.cubridmigration.core.dbmetadata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.cubrid.cubridmigration.core.dbobject.Catalog;
import com.cubrid.cubridmigration.core.dbobject.DBObjectFactory;
import com.cubrid.cubridmigration.core.dbobject.Schema;
import com.cubrid.cubridmigration.core.dbobject.SchemaCatalog;
import com.cubrid.cubridmigration.core.dbobject.SchemaEntry;
import com.cubrid.cubridmigration.core.dbobject.Table;
import com.cubrid.cubridmigration.core.dbobject.Version;
import com.cubrid.cubridmigration.core.dbtype.DatabaseType;
import com.cubrid.cubridmigration.core.export.DBExportHelper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@DisplayName("AbstractJDBCSchemaFetcher partition lifecycle")
class AbstractJDBCSchemaFetcherTest {

    private static final String TODO_CATALOG_NAME = "dba";
    private static final String TODO_SCHEMA_NAME = "test1";
    private static final String TODO_SKIPPED_SCHEMA_NAME = "test2";
    private static final String TODO_TABLE_NAME = "tbl1";

    @Test
    @DisplayName("buildSchemaObjects() invokes buildPartitions after schema objects are built")
    void buildSchemaObjects_invokesBuildPartitionsAfterSchemaObjects() throws Exception {
        TestSchemaFetcher fetcher = new TestSchemaFetcher();
        SchemaCatalog schemaCatalog = createSchemaCatalog();

        Catalog catalog =
                fetcher.buildSchemaObjects(
                        mock(Connection.class),
                        schemaCatalog,
                        Collections.singletonList(TODO_SCHEMA_NAME),
                        null);

        Schema schema = catalog.getSchemaByName(TODO_SCHEMA_NAME);

        assertThat(catalog.getSchemaByName(TODO_SKIPPED_SCHEMA_NAME)).isNull();
        assertThat(schema).isNotNull();
        assertThat(schema.getTableByName(TODO_TABLE_NAME)).isNotNull();
        assertThat(fetcher.invokedSchemaNames).containsExactly(TODO_SCHEMA_NAME);
        assertThat(fetcher.phases)
                .containsExactly(
                        "buildTables",
                        "buildViews",
                        "buildProcedures",
                        "buildTriggers",
                        "buildSequence",
                        "buildSynonym",
                        "buildGrant",
                        "buildPartitions");
    }

    private static SchemaCatalog createSchemaCatalog() {
        SchemaCatalog schemaCatalog =
                new SchemaCatalog(
                        TODO_CATALOG_NAME,
                        DatabaseType.CUBRID,
                        null,
                        new Version(),
                        Collections.emptyMap());
        schemaCatalog.getSchemas().add(new SchemaEntry(TODO_SCHEMA_NAME, false));
        schemaCatalog.getSchemas().add(new SchemaEntry(TODO_SKIPPED_SCHEMA_NAME, true));
        return schemaCatalog;
    }

    private static final class TestSchemaFetcher extends AbstractJDBCSchemaFetcher {
        private final List<String> phases = new ArrayList<String>();
        private final List<String> invokedSchemaNames = new ArrayList<String>();

        private TestSchemaFetcher() {
            factory = new DBObjectFactory();
        }

        @Override
        protected void buildTables(
                Connection conn, Catalog catalog, Schema schema, IBuildSchemaFilter filter)
                throws SQLException {
            phases.add("buildTables");
            Table table = factory.createTable();
            table.setName(TODO_TABLE_NAME);
            schema.addTable(table);
        }

        @Override
        protected void buildViews(
                Connection conn, Catalog catalog, Schema schema, IBuildSchemaFilter filter)
                throws SQLException {
            phases.add("buildViews");
        }

        @Override
        protected void buildProcedures(
                Connection conn, Catalog catalog, Schema schema, IBuildSchemaFilter filter)
                throws SQLException {
            phases.add("buildProcedures");
        }

        @Override
        protected void buildTriggers(
                Connection conn, Catalog catalog, Schema schema, IBuildSchemaFilter filter)
                throws SQLException {
            phases.add("buildTriggers");
        }

        @Override
        protected void buildSequence(
                Connection conn, Catalog catalog, Schema schema, IBuildSchemaFilter filter)
                throws SQLException {
            phases.add("buildSequence");
        }

        @Override
        protected void buildSynonym(
                Connection conn, Catalog catalog, Schema schema, IBuildSchemaFilter filter)
                throws SQLException {
            phases.add("buildSynonym");
        }

        @Override
        protected void buildGrant(
                Connection conn, Catalog catalog, Schema schema, IBuildSchemaFilter filter)
                throws SQLException {
            phases.add("buildGrant");
        }

        @Override
        protected void buildPartitions(
                Connection conn, Catalog catalog, Schema schema, IBuildSchemaFilter filter)
                throws SQLException {
            phases.add("buildPartitions");
            invokedSchemaNames.add(schema.getName());
            assertThat(schema.getTableByName(TODO_TABLE_NAME)).isNotNull();
        }

        @Override
        public DatabaseType getDBType() {
            return DatabaseType.CUBRID;
        }

        @Override
        protected DBExportHelper getExportHelper() {
            return null;
        }

        @Override
        protected String getTableComment(Connection conn, String schemaName, String tableName)
                throws SQLException {
            return null;
        }

        @Override
        protected String getViewComment(Connection conn, String schemaName, String viewName)
                throws SQLException {
            return null;
        }
    }
}
