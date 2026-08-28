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
package com.cubrid.cubridmigration.mysql.meta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.cubrid.cubridmigration.core.dbobject.Table;
import com.cubrid.cubridmigration.mysql.trans.MySQL2CUBRIDTranformHelper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * End-to-end check: MySQL's raw SHOW CREATE TABLE partition DDL (fetch stage) -&gt; CUBRID-fit
 * format (transform stage). Uses a real-world example with per-partition ENGINE clauses and a
 * MySQL version-comment wrapper, exactly as MySQL actually emits it.
 */
@DisplayName("MySQL partition DDL: source fetch -> CUBRID transform pipeline")
class MySQLPartitionDDLPipelineTest {

    // 실제 MySQL 서버의 SHOW CREATE TABLE 결과 형태 그대로 (버전 주석 + 파티션별 ENGINE 절 포함)
    private static final String REAL_MYSQL_DDL =
            "CREATE TABLE `mysql_partition_orders` (\n"
                    + "  `order_id` bigint NOT NULL,\n"
                    + "  `order_date` date NOT NULL,\n"
                    + "  `region` varchar(20) NOT NULL,\n"
                    + "  `amount` decimal(12,2) NOT NULL,\n"
                    + "  `memo` varchar(100) DEFAULT NULL,\n"
                    + "  PRIMARY KEY (`order_id`,`order_date`)\n"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb3\n"
                    + "/*!50500 PARTITION BY RANGE  COLUMNS(order_date)\n"
                    + "(PARTITION p2023 VALUES LESS THAN ('2024-01-01') ENGINE = InnoDB,\n"
                    + " PARTITION p2024 VALUES LESS THAN ('2025-01-01') ENGINE = InnoDB,\n"
                    + " PARTITION p2025 VALUES LESS THAN ('2026-01-01') ENGINE = InnoDB,\n"
                    + " PARTITION pmax VALUES LESS THAN (MAXVALUE) ENGINE = InnoDB) */";

    @Test
    @DisplayName(
            "raw MySQL partition DDL becomes CUBRID-compatible after fetch + transform stages")
    void mysqlPartitionDDL_becomesCubridCompatible_afterFetchAndTransform() {
        Table sourceTable = new Table();
        sourceTable.setName("mysql_partition_orders");
        sourceTable.setDDL(REAL_MYSQL_DDL);

        // 1단계: fetch 시점 - 소스 방언 그대로 "PARTITION BY ..." 부분만 추출 (ENGINE 절은 아직 안 지움)
        String sourceDialectDDL = new MySQLSchemaFetcher().getSourcePartitionDDL(sourceTable);

        // 2단계: transform 시점 - CUBRID에 맞게 정리 (여기서 ENGINE 절 제거, 백틱->큰따옴표)
        String cubridDDL =
                new MySQL2CUBRIDTranformHelper(null, null).getFitTargetFormatSQL(sourceDialectDDL);

        assertAll(
                () -> assertThat(sourceDialectDDL).contains("ENGINE = InnoDB"), // 1단계는 아직 안 지워짐(정상)
                () -> assertThat(cubridDDL).doesNotContain("ENGINE"), // 2단계 이후엔 지워져야 함
                () -> assertThat(cubridDDL).doesNotContain("`"),
                () -> assertThat(cubridDDL).doesNotContain("/*!"),
                () -> assertThat(cubridDDL).contains("PARTITION BY RANGE"),
                () -> assertThat(cubridDDL).contains("PARTITION p2023 VALUES LESS THAN ('2024-01-01')"),
                () -> assertThat(cubridDDL).contains("PARTITION p2024 VALUES LESS THAN ('2025-01-01')"),
                () -> assertThat(cubridDDL).contains("PARTITION p2025 VALUES LESS THAN ('2026-01-01')"),
                () -> assertThat(cubridDDL).contains("PARTITION pmax VALUES LESS THAN (MAXVALUE)"));
    }
}
