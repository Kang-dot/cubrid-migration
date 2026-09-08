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
package com.cubrid.cubridmigration.core.engine.exporter.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cubrid.cubridmigration.core.dbobject.Table;
import com.cubrid.cubridmigration.core.dbtype.DatabaseType;
import com.cubrid.cubridmigration.core.engine.config.MigrationConfiguration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Covers {@link JDBCExporter#combineCondition} and {@link JDBCExporter#isLatestPage}.
 *
 * <p>{@code isLatestPage} regression note: {@code OracleExportHelper}/{@code MySQLExportHelper}
 * #getPagedSelectSQL are no-op stubs (never actually implemented), so a single query execution
 * already returns everything it matches. The stopping condition must not depend on comparing the
 * exported count against the *whole table's* row count for these two DB types, since a
 * TableSplitPlanner range query only ever returns its own (smaller) subset - see the real 4x
 * duplication bug this exact scenario caused.
 */
@DisplayName("JDBCExporter")
class JDBCExporterTest {

    private final JDBCExporter exporter = new JDBCExporter();

    @Test
    @DisplayName("a blank extra condition leaves the SQL unchanged")
    void combineCondition_blankExtraCondition_returnsSqlUnchanged() {
        String sql = "SELECT id FROM orders";

        assertThat(exporter.combineCondition(sql, null)).isEqualTo(sql);
        assertThat(exporter.combineCondition(sql, "")).isEqualTo(sql);
        assertThat(exporter.combineCondition(sql, "   ")).isEqualTo(sql);
    }

    @Test
    @DisplayName("no existing WHERE clause: the extra condition is added with WHERE")
    void combineCondition_noExistingWhere_addsWhere() {
        String sql = "SELECT id FROM orders";

        String result = exporter.combineCondition(sql, "id BETWEEN 1 AND 1000");

        assertThat(result).isEqualTo("SELECT id FROM orders WHERE (id BETWEEN 1 AND 1000)");
    }

    @Test
    @DisplayName("an existing WHERE clause (from the table's own condition): the extra condition"
            + " is ANDed on instead of replacing it")
    void combineCondition_existingWhere_addsAnd() {
        String sql = "SELECT id FROM orders WHERE status = 'ACTIVE'";

        String result = exporter.combineCondition(sql, "id BETWEEN 1 AND 1000");

        assertThat(result)
                .isEqualTo(
                        "SELECT id FROM orders WHERE status = 'ACTIVE' AND (id BETWEEN 1 AND"
                                + " 1000)");
    }

    @Test
    @DisplayName("WHERE detection is case-insensitive")
    void combineCondition_lowercaseWhere_isDetected() {
        String sql = "select id from orders where status = 'ACTIVE'";

        String result = exporter.combineCondition(sql, "id BETWEEN 1 AND 1000");

        assertThat(result).contains(" AND (id BETWEEN 1 AND 1000)");
        assertThat(result).doesNotContain(" WHERE (id BETWEEN 1 AND 1000)");
    }

    @Test
    @DisplayName(
            "regression: Oracle stops after one page even when the query only matched a range"
                    + " smaller than the whole table (the 4x-duplication bug)")
    void isLatestPage_oracleRangeSmallerThanTable_stopsAfterOnePage() {
        MigrationConfiguration config = mock(MigrationConfiguration.class);
        when(config.getSourceDBType()).thenReturn(DatabaseType.ORACLE);
        when(config.isImplicitEstimate()).thenReturn(false); // the default a user would leave
        when(config.getPageFetchCount()).thenReturn(1000);
        exporter.setConfig(config);

        Table table = new Table();
        table.setName("orders");
        table.setTableRowCount(200_000); // whole table, not this range's ~50,000

        // One un-paginated execution of a range query already returned all 50,000 matching rows -
        // far more than pageFetchCount, and far less than the whole table's row count. The old
        // logic would keep looping (re-reading the same 50,000 rows) until exportedRecords caught
        // up to 200,000: exactly 4 repeats for a degree=4 split.
        boolean latest = exporter.isLatestPage(table, 50_000, 50_000);

        assertThat(latest).isTrue();
    }

    @Test
    @DisplayName("regression: same as Oracle, MySQL also stops after one page unconditionally")
    void isLatestPage_mysqlRangeSmallerThanTable_stopsAfterOnePage() {
        MigrationConfiguration config = mock(MigrationConfiguration.class);
        when(config.getSourceDBType()).thenReturn(DatabaseType.MYSQL);
        when(config.isImplicitEstimate()).thenReturn(false);
        when(config.getPageFetchCount()).thenReturn(1000);
        exporter.setConfig(config);

        Table table = new Table();
        table.setName("orders");
        table.setTableRowCount(200_000);

        assertThat(exporter.isLatestPage(table, 50_000, 50_000)).isTrue();
    }

    @Test
    @DisplayName(
            "a DB with real pagination (e.g. MSSQL) still uses the row-count-based stopping"
                    + " condition, unaffected by the Oracle/MySQL fix")
    void isLatestPage_otherDatabase_stillUsesRowCountCondition() {
        MigrationConfiguration config = mock(MigrationConfiguration.class);
        when(config.getSourceDBType()).thenReturn(DatabaseType.MSSQL);
        when(config.isImplicitEstimate()).thenReturn(false);
        when(config.getPageFetchCount()).thenReturn(1000);
        exporter.setConfig(config);

        Table table = new Table();
        table.setName("orders");
        table.setTableRowCount(200_000);

        // A full page (1000 rows) with plenty of the table still unread: not the last page.
        assertThat(exporter.isLatestPage(table, 1000, 1000)).isFalse();
        // Exported count reached the (whole-table) total: last page.
        assertThat(exporter.isLatestPage(table, 200_000, 1000)).isTrue();
        // A short page (fewer rows than a full page) always signals the last page.
        assertThat(exporter.isLatestPage(table, 500, 500)).isTrue();
    }
}
