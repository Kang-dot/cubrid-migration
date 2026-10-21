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
package com.cubrid.cubridmigration.core.engine.executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.cubrid.cubridmigration.core.engine.task.ITableKeyedTask;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Regression coverage for {@link MultiQueueExecutor}'s per-key queue assignment: it used to key
 * every queue by {@code Thread.currentThread().getId()}, which was harmless while one table's
 * export was always exactly one thread, but silently multiplies a split table's import thread count
 * by its split degree now that a table can be exported by several parallel range threads (see
 * {@code RecordImportTask#getTableKey()}, {@code ImportDataTaskDecorator#getTableKey()}).
 *
 * <p>Each test proves queue sharing/independence through causality - a blocked first task can only
 * let a second one observe its release if they run on the very same single-thread queue - rather
 * than through wall-clock timing, so they stay deterministic regardless of machine speed.
 */
@DisplayName("MultiQueueExecutor")
class MultiQueueExecutorTest {

    private static final long AWAIT_SECONDS = 5L;

    @Test
    @DisplayName(
            "same table key from two different threads -> tasks share one single-thread queue and"
                    + " run one at a time")
    void sameTableKeyFromDifferentThreads_sharesOneQueueAndSerializes()
            throws InterruptedException {
        // Submitted from two distinct java.lang.Thread instances - this is what actually
        // distinguishes the fix from the old behavior. TableRecordRangeExportTask runs each split
        // range on its own thread, so the regression only shows up when different *callers*
        // (not just different calls) share a table key.
        MultiQueueExecutor executor = new MultiQueueExecutor(1, false);
        CountDownLatch firstStarted = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        AtomicBoolean firstReleased = new AtomicBoolean(false);
        AtomicBoolean secondSawFirstReleased = new AtomicBoolean(false);
        CountDownLatch secondDone = new CountDownLatch(1);

        try {
            Thread rangeThread1 =
                    new Thread(
                            () ->
                                    executor.execute(
                                            keyedTask(
                                                    "OWNER.T",
                                                    () -> {
                                                        firstStarted.countDown();
                                                        awaitLatch(releaseFirst);
                                                        firstReleased.set(true);
                                                    })));
            rangeThread1.start();
            assertThat(firstStarted.await(AWAIT_SECONDS, TimeUnit.SECONDS)).isTrue();

            Thread rangeThread2 =
                    new Thread(
                            () ->
                                    executor.execute(
                                            keyedTask(
                                                    "OWNER.T",
                                                    () -> {
                                                        secondSawFirstReleased.set(
                                                                firstReleased.get());
                                                        secondDone.countDown();
                                                    })));
            rangeThread2.start();
            // Block until the submission itself (the enqueue onto whatever queue "OWNER.T"
            // resolves to) has definitely happened, so the next check isn't racing it.
            rangeThread2.join(AWAIT_SECONDS * 1000);

            // With a pool of size 1, a second task sharing the first one's queue cannot even
            // start while the first is still blocked - so it must not have finished yet. Proving
            // this *before* releasing (rather than trusting timing after releasing) is what
            // catches the old per-calling-thread key, under which range thread 2 would get its
            // own independent queue and finish here immediately.
            assertThat(secondDone.await(200, TimeUnit.MILLISECONDS)).isFalse();

            releaseFirst.countDown();
            assertThat(secondDone.await(AWAIT_SECONDS, TimeUnit.SECONDS)).isTrue();
            // The second task could only run (on a pool of size 1) after the first one - sharing
            // the same table key even though a different thread submitted it - had already
            // finished and released.
            assertThat(secondSawFirstReleased.get()).isTrue();
            rangeThread1.join(AWAIT_SECONDS * 1000);
        } finally {
            releaseFirst.countDown();
            executor.dispose();
        }
    }

    @Test
    @DisplayName("different table keys -> independent queues that run concurrently")
    void differentTableKeys_getIndependentConcurrentQueues() throws InterruptedException {
        MultiQueueExecutor executor = new MultiQueueExecutor(1, false);
        CountDownLatch firstStarted = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        AtomicBoolean firstReleased = new AtomicBoolean(false);
        AtomicBoolean secondSawFirstReleased = new AtomicBoolean(false);
        CountDownLatch secondDone = new CountDownLatch(1);

        try {
            executor.execute(
                    keyedTask(
                            "OWNER.T1",
                            () -> {
                                firstStarted.countDown();
                                awaitLatch(releaseFirst);
                                firstReleased.set(true);
                            }));
            assertThat(firstStarted.await(AWAIT_SECONDS, TimeUnit.SECONDS)).isTrue();

            executor.execute(
                    keyedTask(
                            "OWNER.T2",
                            () -> {
                                secondSawFirstReleased.set(firstReleased.get());
                                secondDone.countDown();
                            }));

            // T2's queue has its own dedicated thread, so it can finish while T1 is still blocked.
            assertThat(secondDone.await(AWAIT_SECONDS, TimeUnit.SECONDS)).isTrue();
            assertThat(secondSawFirstReleased.get()).isFalse();
        } finally {
            releaseFirst.countDown();
            executor.dispose();
        }
    }

    @Test
    @DisplayName("a null table key falls back to the original per-calling-thread queue")
    void nullTableKey_fallsBackToPerCallingThreadQueue() throws InterruptedException {
        MultiQueueExecutor executor = new MultiQueueExecutor(1, false);
        CountDownLatch firstStarted = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        AtomicBoolean firstReleased = new AtomicBoolean(false);
        AtomicBoolean secondSawFirstReleased = new AtomicBoolean(false);
        CountDownLatch secondDone = new CountDownLatch(1);

        try {
            // Both submitted from this same test thread, so the thread-id fallback groups them
            // exactly as it did before ITableKeyedTask existed.
            executor.execute(
                    keyedTask(
                            null,
                            () -> {
                                firstStarted.countDown();
                                awaitLatch(releaseFirst);
                                firstReleased.set(true);
                            }));
            assertThat(firstStarted.await(AWAIT_SECONDS, TimeUnit.SECONDS)).isTrue();

            executor.execute(
                    keyedTask(
                            null,
                            () -> {
                                secondSawFirstReleased.set(firstReleased.get());
                                secondDone.countDown();
                            }));

            releaseFirst.countDown();
            assertThat(secondDone.await(AWAIT_SECONDS, TimeUnit.SECONDS)).isTrue();
            assertThat(secondSawFirstReleased.get()).isTrue();
        } finally {
            releaseFirst.countDown();
            executor.dispose();
        }
    }

    @Test
    @DisplayName(
            "releaseKey() removes the key's queue but lets an in-flight task under it finish"
                    + " normally")
    void releaseKey_removesQueueButLetsInFlightTaskFinish() throws InterruptedException {
        MultiQueueExecutor executor = new MultiQueueExecutor(1, false);
        CountDownLatch firstStarted = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        CountDownLatch firstDone = new CountDownLatch(1);
        AtomicBoolean firstCompleted = new AtomicBoolean(false);

        try {
            executor.execute(
                    keyedTask(
                            "OWNER.T",
                            () -> {
                                firstStarted.countDown();
                                awaitLatch(releaseFirst);
                                firstCompleted.set(true);
                                firstDone.countDown();
                            }));
            assertThat(firstStarted.await(AWAIT_SECONDS, TimeUnit.SECONDS)).isTrue();
            assertThat(executor.queueCount()).isEqualTo(1);

            // Release the key while the first task is still blocked/in-flight - this is exactly
            // the moment TableRecordExportTask#endExportTable calls it, since a table's export
            // finishing doesn't mean the last batch it submitted has finished importing yet.
            executor.releaseKey("OWNER.T");
            // The map entry is gone immediately - this is the fix for the unbounded per-table
            // growth: without it, this count would only ever go up for the rest of the migration.
            assertThat(executor.queueCount()).isEqualTo(0);

            releaseFirst.countDown();
            assertThat(firstDone.await(AWAIT_SECONDS, TimeUnit.SECONDS)).isTrue();
            // A graceful dispose() (not interrupt()) must let the in-flight task finish normally.
            assertThat(firstCompleted.get()).isTrue();

            // A later submission under the same key must still work, on a freshly created queue -
            // proving the key isn't permanently poisoned by having been released once.
            CountDownLatch secondDone = new CountDownLatch(1);
            AtomicBoolean secondCompleted = new AtomicBoolean(false);
            executor.execute(
                    keyedTask(
                            "OWNER.T",
                            () -> {
                                secondCompleted.set(true);
                                secondDone.countDown();
                            }));
            assertThat(secondDone.await(AWAIT_SECONDS, TimeUnit.SECONDS)).isTrue();
            assertThat(secondCompleted.get()).isTrue();
            assertThat(executor.queueCount()).isEqualTo(1);
        } finally {
            releaseFirst.countDown();
            executor.dispose();
        }
    }

    @Test
    @DisplayName("releaseKey() for a key with no queue yet is a harmless no-op")
    void releaseKey_forUnknownKeyIsNoOp() {
        MultiQueueExecutor executor = new MultiQueueExecutor(1, false);
        try {
            assertThatCode(() -> executor.releaseKey("OWNER.NEVER_SUBMITTED"))
                    .doesNotThrowAnyException();
        } finally {
            executor.dispose();
        }
    }

    private static void awaitLatch(CountDownLatch latch) {
        try {
            latch.await(AWAIT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static Runnable keyedTask(String tableKey, Runnable body) {
        return new KeyedRunnable(tableKey, body);
    }

    /** Minimal {@link ITableKeyedTask} stand-in - production tasks are {@code ImportTask}s. */
    private static final class KeyedRunnable implements Runnable, ITableKeyedTask {
        private final String tableKey;
        private final Runnable body;

        KeyedRunnable(String tableKey, Runnable body) {
            this.tableKey = tableKey;
            this.body = body;
        }

        @Override
        public void run() {
            body.run();
        }

        @Override
        public String getTableKey() {
            return tableKey;
        }
    }
}
