/*
 * Copyright 2024 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.internal;

import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Diagnostic, opt-in detector for {@link org.openrewrite.Cursor} objects that survive
 * past the top-level visit (one recipe applied to one source file) that created them.
 * <p>
 * A cursor is created for every visited tree node and is logically dead once its
 * top-level visit completes — unless a recipe stashes it into longer-lived state
 * (an {@code ExecutionContext} message, a {@code ScanningRecipe} accumulator, an
 * instance field, …). Such retention is what a per-source-file cursor pool would
 * have to forbid. This class measures whether it actually happens, without changing
 * allocation behavior: it weakly tracks a sample of cursors, advances an epoch per
 * top-level visit, and periodically forces a GC and reports any sampled cursor that
 * is still strongly reachable several epochs after its own visit ended.
 * <p>
 * Entirely disabled (zero overhead) unless {@code -Dorg.openrewrite.cursorEscapeCheck=true}.
 * Tuning: {@code cursorEscapeSample} (1/N sampling, default 16),
 * {@code cursorEscapeCheckEvery} (visits between sweeps, default 100),
 * {@code cursorEscapeGrace} (epochs of grace before a survivor counts as leaked, default 3).
 */
public final class CursorEscapeDetector {

    public static final boolean ENABLED = Boolean.getBoolean("org.openrewrite.cursorEscapeCheck");

    private static int SAMPLE = Integer.getInteger("org.openrewrite.cursorEscapeSample", 16);
    private static int CHECK_EVERY = Integer.getInteger("org.openrewrite.cursorEscapeCheckEvery", 100);
    private static long GRACE = Integer.getInteger("org.openrewrite.cursorEscapeGrace", 3);

    /** Capture a creation stack for cursors whose value type (lowercased) contains one of these tokens. */
    private static final java.util.Set<String> STACK_FILTER = parseFilter(System.getProperty("org.openrewrite.cursorEscapeStackFilter", ""));
    private static final int STACK_FRAMES = Integer.getInteger("org.openrewrite.cursorEscapeStackFrames", 40);
    private static final int LEAK_STACK_CAP = 12;

    private static java.util.Set<String> parseFilter(String s) {
        java.util.Set<String> set = new java.util.HashSet<>();
        for (String tok : s.split(",")) {
            tok = tok.trim().toLowerCase();
            if (!tok.isEmpty()) {
                set.add(tok);
            }
        }
        return set;
    }

    // Per-thread epoch: a cursor is aged against its OWN thread's progress, not a global counter. The
    // run is parallel across source files (a ForkJoinPool), so a long file-visit on one worker must not
    // be "aged out" by other workers racing a shared counter while its cursors are still legitimately live.
    private static final ThreadLocal<long[]> threadEpoch = ThreadLocal.withInitial(() -> new long[1]);
    private static final Map<Long, long[]> allThreadEpochs = new ConcurrentHashMap<>();
    private static final AtomicLong createdCount = new AtomicLong();
    private static final AtomicLong sampledCount = new AtomicLong();
    private static final AtomicLong visitsSinceSweep = new AtomicLong();
    private static final AtomicLong leakedCount = new AtomicLong();

    private static final Queue<Tracked> tracked = new ConcurrentLinkedQueue<>();
    private static final Map<String, Long> leaksByType = new ConcurrentHashMap<>();
    private static final Queue<String> leakStacks = new ConcurrentLinkedQueue<>();

    /** Per-thread stack of the class names of currently-active top-level visitors. */
    private static final ThreadLocal<ArrayDeque<String>> creatorStack = ThreadLocal.withInitial(ArrayDeque::new);

    private static final class Tracked extends WeakReference<Object> {
        final long threadId;
        final long epoch;
        final String valueType;
        final String creator;
        final String stack;

        Tracked(Object cursor, long threadId, long epoch, String valueType, String creator, String stack) {
            super(cursor);
            this.threadId = threadId;
            this.epoch = epoch;
            this.valueType = valueType;
            this.creator = creator;
            this.stack = stack;
        }
    }

    /** The current thread's epoch counter, registered so the sweep (on any thread) can read it. */
    private static long[] myEpoch() {
        long[] te = threadEpoch.get();
        allThreadEpochs.putIfAbsent(Thread.currentThread().getId(), te);
        return te;
    }

    /** Called at the start of a top-level visit so created cursors can be attributed to the visitor. */
    public static void pushVisitor(String visitorClass) {
        creatorStack.get().push(visitorClass);
    }

    /** Called at the end of a top-level visit. */
    public static void popVisitor() {
        ArrayDeque<String> d = creatorStack.get();
        if (!d.isEmpty()) {
            d.pop();
        }
    }

    static {
        if (ENABLED) {
            Runtime.getRuntime().addShutdownHook(new Thread(CursorEscapeDetector::report, "cursor-escape-report"));
        }
    }

    private CursorEscapeDetector() {
    }

    /** Test-only: set tunables and clear all state for an isolated run. */
    public static void configureForTest(int sample, int checkEvery, long grace) {
        SAMPLE = sample;
        CHECK_EVERY = checkEvery;
        GRACE = grace;
        threadEpoch.remove();
        allThreadEpochs.clear();
        createdCount.set(0);
        sampledCount.set(0);
        visitsSinceSweep.set(0);
        leakedCount.set(0);
        tracked.clear();
        leaksByType.clear();
        leakStacks.clear();
    }

    /** Registers a sampled cursor against the current epoch. Called right after each {@code new Cursor(...)}. */
    public static void onCursorCreated(Object cursor, Object value) {
        if (createdCount.incrementAndGet() % SAMPLE != 0) {
            return;
        }
        sampledCount.incrementAndGet();
        String creator = creatorStack.get().peek();
        String valueType = value == null ? "null" : value.getClass().getName();
        String stack = null;
        if (!STACK_FILTER.isEmpty()) {
            String vt = valueType.toLowerCase();
            for (String f : STACK_FILTER) {
                if (vt.contains(f)) {
                    stack = captureStack();
                    break;
                }
            }
        }
        tracked.add(new Tracked(cursor, Thread.currentThread().getId(), myEpoch()[0], valueType,
                creator == null ? "<none>" : creator, stack));
    }

    private static String captureStack() {
        StackTraceElement[] els = new Throwable().getStackTrace();
        StringBuilder sb = new StringBuilder();
        int shown = 0;
        for (StackTraceElement e : els) {
            if (CursorEscapeDetector.class.getName().equals(e.getClassName())) {
                continue; // skip the detector's own frames
            }
            sb.append("        at ").append(e.getClassName()).append('.').append(e.getMethodName()).append('\n');
            if (++shown >= STACK_FRAMES) {
                break;
            }
        }
        return sb.toString();
    }

    /** Advances the epoch and periodically sweeps for survivors. Called at the end of each top-level visit. */
    public static void onTopLevelEnd() {
        myEpoch()[0]++;
        if (visitsSinceSweep.incrementAndGet() < CHECK_EVERY) {
            return;
        }
        visitsSinceSweep.set(0);
        sweep(false);
    }

    private static void sweep(boolean finalSweep) {
        // Aggressive reclaim so a survivor is provably retained, not just GC-timing lag: multiple full
        // GCs + finalization with a pause between, on the assumption truly-dead cursors get collected.
        for (int i = 0; i < 3; i++) {
            System.gc();
            System.runFinalization();
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        for (Iterator<Tracked> it = tracked.iterator(); it.hasNext(); ) {
            Tracked t = it.next();
            Object c = t.get();
            if (c == null) {
                it.remove(); // collected on schedule — not retained
                continue;
            }
            // age against the cursor's OWN thread's current epoch, not a global one
            long[] te = allThreadEpochs.get(t.threadId);
            long threadNow = te == null ? Long.MAX_VALUE : te[0];
            if (finalSweep || threadNow - t.epoch >= GRACE) {
                // still strongly reachable many of its own thread's visits after its visit ended => retained
                leakedCount.incrementAndGet();
                leaksByType.merge(t.creator + "  ::  " + t.valueType, 1L, Long::sum);
                if (t.stack != null && leakStacks.size() < LEAK_STACK_CAP) {
                    leakStacks.add("LEAK  " + t.creator + "  ::  " + t.valueType +
                            "  (created epoch " + t.epoch + " on thread " + t.threadId + ", thread now " + threadNow + ")\n" + t.stack);
                }
                it.remove();
            }
        }
    }

    /** Number of sampled cursors confirmed retained past their visit (for tests). */
    public static long leakCount() {
        return leakedCount.get();
    }

    public static void report() {
        sweep(true); // final sweep: every visit is done, so anything still alive is genuinely retained
        StringBuilder sb = new StringBuilder("\n[CursorEscapeDetector] sampling 1/").append(SAMPLE)
                .append(" — created(sampled)=").append(sampledCount.get())
                .append(" leaked(sampled)=").append(leakedCount.get())
                .append(" estimatedTotalLeaked≈").append(leakedCount.get() * SAMPLE).append('\n');
        if (leaksByType.isEmpty()) {
            sb.append("   no cursors retained past their top-level visit — contract holds for this run\n");
        } else {
            sb.append("   count  creator-visitor  ::  retained-node-type\n");
            leaksByType.entrySet().stream()
                    .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                    .limit(40)
                    .forEach(e -> sb.append("   ").append(e.getValue()).append("  ").append(e.getKey()).append('\n'));
        }
        if (!leakStacks.isEmpty()) {
            sb.append("   --- creation stacks of leaked cursors (").append(leakStacks.size()).append(") ---\n");
            for (String s : leakStacks) {
                sb.append("   ").append(s).append('\n');
            }
        }
        System.err.print(sb);
    }
}
