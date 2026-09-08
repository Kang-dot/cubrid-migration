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
package com.cubrid.cubridmigration.core.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Regression coverage for the export-completion tracking in {@link MigrationStatusManager}: it
 * used to be a plain boolean flag that assumed exactly one export task per table, which silently
 * breaks once a table is split into N parallel range tasks (the first range to finish would mark
 * the whole table done while the others are still exporting - see TableSplitPlanner and
 * LoadFileImporter's use of getExpFlag() to trigger the final file merge).
 */
@DisplayName("MigrationStatusManager export completion tracking")
class MigrationStatusManagerTest {

    private static final String OWNER = "owner";
    private static final String TABLE = "orders";

    private final MigrationStatusManager statusManager = new MigrationStatusManager();

    @Test
    @DisplayName(
            "an unregistered source (the common, non-split case) finishes after a single"
                    + " setExpFinished() call - same as the old one-flag behavior")
    void setExpFinished_withoutRegistration_finishesAfterOneCall() {
        assertThat(statusManager.getExpFlag(OWNER, TABLE)).isFalse();

        statusManager.setExpFinished(OWNER, TABLE);

        assertThat(statusManager.getExpFlag(OWNER, TABLE)).isTrue();
    }

    @Test
    @DisplayName("a source registered with 3 ranges is not done until all 3 have finished")
    void setExpFinished_withThreeRegisteredRanges_finishesOnlyAfterAllThree() {
        statusManager.registerExportRangeCount(OWNER, TABLE, 3);

        statusManager.setExpFinished(OWNER, TABLE);
        assertThat(statusManager.getExpFlag(OWNER, TABLE)).isFalse();

        statusManager.setExpFinished(OWNER, TABLE);
        assertThat(statusManager.getExpFlag(OWNER, TABLE)).isFalse();

        statusManager.setExpFinished(OWNER, TABLE);
        assertThat(statusManager.getExpFlag(OWNER, TABLE)).isTrue();
    }

    @Test
    @DisplayName(
            "the first range finishing does not prematurely mark a multi-range table as done"
                    + " (the exact bug a boolean flag had)")
    void setExpFinished_firstOfMultipleRanges_doesNotMarkTableDone() {
        statusManager.registerExportRangeCount(OWNER, TABLE, 4);

        statusManager.setExpFinished(OWNER, TABLE);

        assertThat(statusManager.getExpFlag(OWNER, TABLE)).isFalse();
    }

    @Test
    @DisplayName("registering the range count after construction still works if done before any"
            + " range reports finished")
    void registerExportRangeCount_beforeAnyFinish_isRespected() {
        statusManager.registerExportRangeCount(OWNER, TABLE, 2);

        statusManager.setExpFinished(OWNER, TABLE);
        statusManager.setExpFinished(OWNER, TABLE);

        assertThat(statusManager.getExpFlag(OWNER, TABLE)).isTrue();
    }

    @Test
    @DisplayName("a source that was never touched is not considered finished")
    void getExpFlag_unknownSource_isFalse() {
        assertThat(statusManager.getExpFlag(OWNER, "never_seen_table")).isFalse();
    }

    @Test
    @DisplayName("registering fewer than 1 range is rejected")
    void registerExportRangeCount_lessThanOne_throws() {
        assertThatThrownBy(() -> statusManager.registerExportRangeCount(OWNER, TABLE, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("export completion tracking for one table doesn't affect another")
    void setExpFinished_isPerSource_doesNotLeakAcrossTables() {
        statusManager.registerExportRangeCount(OWNER, "table_a", 2);
        statusManager.registerExportRangeCount(OWNER, "table_b", 2);

        statusManager.setExpFinished(OWNER, "table_a");
        statusManager.setExpFinished(OWNER, "table_a");

        assertThat(statusManager.getExpFlag(OWNER, "table_a")).isTrue();
        assertThat(statusManager.getExpFlag(OWNER, "table_b")).isFalse();
    }

    @Test
    @DisplayName("addExpCount/getExpCount accumulate independently of the completion flag")
    void addExpCount_accumulatesAcrossMultipleRangeCalls() {
        statusManager.registerExportRangeCount(OWNER, TABLE, 2);

        statusManager.addExpCount(OWNER, TABLE, 100);
        statusManager.addExpCount(OWNER, TABLE, 50);

        assertThat(statusManager.getExpCount(OWNER, TABLE)).isEqualTo(150);
        // Row counting and range-completion tracking are independent: no range has reported
        // finished yet even though records were counted.
        assertThat(statusManager.getExpFlag(OWNER, TABLE)).isFalse();
    }
}
