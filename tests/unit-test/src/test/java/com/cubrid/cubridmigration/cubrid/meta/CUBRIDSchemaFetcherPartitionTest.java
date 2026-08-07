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
package com.cubrid.cubridmigration.cubrid.meta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cubrid.cubridmigration.core.dbobject.Catalog;
import com.cubrid.cubridmigration.core.dbobject.Column;
import com.cubrid.cubridmigration.core.dbobject.PartitionInfo;
import com.cubrid.cubridmigration.core.dbobject.Schema;
import com.cubrid.cubridmigration.core.dbobject.Table;
import com.cubrid.cubridmigration.core.dbtype.DatabaseType;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

@DisplayName("CUBRIDSchemaFetcher partition metadata")
class CUBRIDSchemaFetcherPartitionTest {

    private static final String TODO_SCHEMA_NAME = "test1";
    private static final String TODO_OTHER_SCHEMA_NAME = "test2";
    private static final String TODO_TABLE_NAME = "tbl1";
    private static final String TODO_COLUMN_NAME = "col1";
    private static final String TODO_COLUMN_TYPE = "INTEGER";
    private static final String TODO_PARTITION_NAME = "UNDER_2000";
    private static final String TODO_PARTITION_METHOD = "RANGE";
    private static final String TODO_PARTITION_EXPR = "col1";
    private static final String SQL_GET_PARTITIONS =
            "SELECT class_name, partition_name, partition_class_name,"
                    + " partition_type, partition_expr, partition_values"
                    + " FROM db_partition";

    @Test
    @DisplayName("buildPartitions() maps db_partition rows to table PartitionInfo")
    void buildPartitions_mapsDbPartitionRowsToTablePartitionInfo() throws Exception {
        CUBRIDSchemaFetcher fetcher = new CUBRIDSchemaFetcher();
        Catalog catalog = createCatalog();
        Schema schema = createSchema(TODO_SCHEMA_NAME);
        Table table = createTable(TODO_TABLE_NAME, TODO_COLUMN_NAME, TODO_COLUMN_TYPE);
        catalog.addSchema(schema);
        schema.addTable(table);

        Connection conn = mockConnectionWithPartitionRow(TODO_TABLE_NAME);

        fetcher.buildPartitions(conn, catalog, schema, null);

        PartitionInfo partitionInfo = table.getPartitionInfo();

        assertAll(
                () -> assertThat(partitionInfo).isNotNull(),
                () -> assertThat(partitionInfo.getPartitionMethod()).isEqualTo(TODO_PARTITION_METHOD),
                () -> assertThat(partitionInfo.getPartitionExp()).isEqualTo(TODO_PARTITION_EXPR),
                () ->
                        assertThat(partitionInfo.getPartitionColumns())
                                .extracting(Column::getName)
                                .containsExactly(TODO_COLUMN_NAME),
                () -> assertThat(partitionInfo.getPartitionCount()).isEqualTo(1),
                () -> assertThat(partitionInfo.getPartitions()).hasSize(1),
                () ->
                        assertThat(partitionInfo.getPartitions().get(0).getPartitionName())
                                .isEqualTo(TODO_PARTITION_NAME),
                () ->
                        assertThat(partitionInfo.getPartitions().get(0).getPartitionDesc())
                                .isEqualTo("2000"),
                () -> assertThat(partitionInfo.getDDL()).contains("PARTITION BY RANGE"));
    }

    @Test
    @DisplayName("buildPartitions() skips tables outside the schema being built")
    void buildPartitions_skipsTablesOutsideCurrentSchema() throws Exception {
        CUBRIDSchemaFetcher fetcher = new CUBRIDSchemaFetcher();
        Catalog catalog = createCatalog();
        Schema schema = createSchema(TODO_SCHEMA_NAME);
        Schema otherSchema = createSchema(TODO_OTHER_SCHEMA_NAME);
        Table otherTable = createTable(TODO_TABLE_NAME, TODO_COLUMN_NAME, TODO_COLUMN_TYPE);
        catalog.addSchema(schema);
        catalog.addSchema(otherSchema);
        otherSchema.addTable(otherTable);

        Connection conn = mockConnectionWithPartitionRow(TODO_TABLE_NAME);

        fetcher.buildPartitions(conn, catalog, schema, null);

        assertThat(otherTable.getPartitionInfo()).isNull();
    }

    private static Connection mockConnectionWithPartitionRow(String tableName) throws Exception {
        Connection conn = mock(Connection.class);
        Statement stmt = mock(Statement.class);
        ResultSet rs = mock(ResultSet.class);

        when(conn.createStatement()).thenReturn(stmt);
        when(stmt.executeQuery(SQL_GET_PARTITIONS)).thenReturn(rs);
        when(rs.next()).thenReturn(true, false);
        when(rs.getString("class_name")).thenReturn(tableName);
        when(rs.getString("partition_type")).thenReturn(TODO_PARTITION_METHOD);
        when(rs.getString("partition_expr")).thenReturn(TODO_PARTITION_EXPR);
        when(rs.getObject("partition_values")).thenReturn(new Object[] {"0", "2000"});
        when(rs.getString("partition_name")).thenReturn(TODO_PARTITION_NAME);

        return conn;
    }

    private static Catalog createCatalog() {
        Catalog catalog = new Catalog();
        catalog.setName("TODO_CATALOG");
        catalog.setDatabaseType(DatabaseType.CUBRID);
        return catalog;
    }

    private static Schema createSchema(String schemaName) {
        Schema schema = new Schema();
        schema.setName(schemaName);
        return schema;
    }

    private static Table createTable(String tableName, String columnName, String columnType) {
        Table table = new Table();
        table.setName(tableName);

        Column column = new Column();
        column.setName(columnName);
        column.setDataType(columnType);
        table.addColumn(column);

        return table;
    }
}
