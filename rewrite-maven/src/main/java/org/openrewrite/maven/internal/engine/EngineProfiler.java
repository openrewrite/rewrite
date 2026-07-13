/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.maven.internal.engine;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Opt-in ({@code -Dengine.profile=true}) phase attribution for the maven engine: nanos and counts per hot phase, so the
 * integrated benchmark can split a warm reactor build into bootstrap / model / collect / project. Disabled it costs a
 * volatile read per site. Package-external only for the phase0-tools harness to snapshot.
 */
public final class EngineProfiler {

    public static final boolean ENABLED = Boolean.getBoolean("engine.profile");

    public static final AtomicLong bootstrapNanos = new AtomicLong();
    public static final AtomicLong effectiveNanos = new AtomicLong();   // whole buildEngineEffective (root)
    public static final AtomicLong collectNanos = new AtomicLong();     // collectDependencies
    public static final AtomicLong projectNanos = new AtomicLong();     // DependencyGraphMapper.map
    public static final AtomicLong modelBuildNanos = new AtomicLong();  // every EngineEffectivePom.build (root + descriptor)
    public static final AtomicLong modelBuilds = new AtomicLong();
    public static final AtomicLong descriptorReads = new AtomicLong();
    public static final AtomicLong modelCacheHits = new AtomicLong();
    public static final AtomicLong modelCacheMisses = new AtomicLong();
    public static final AtomicLong effectiveMemoHits = new AtomicLong();   // buildEngineEffective served from the memo
    public static final AtomicLong effectiveMemoMisses = new AtomicLong();

    private EngineProfiler() {
    }

    public static void reset() {
        bootstrapNanos.set(0);
        effectiveNanos.set(0);
        collectNanos.set(0);
        projectNanos.set(0);
        modelBuildNanos.set(0);
        modelBuilds.set(0);
        descriptorReads.set(0);
        modelCacheHits.set(0);
        modelCacheMisses.set(0);
        effectiveMemoHits.set(0);
        effectiveMemoMisses.set(0);
    }

    public static String report(int modules) {
        return "modules=" + modules +
                " effectiveMs=" + ms(effectiveNanos) +
                " collectMs=" + ms(collectNanos) +
                " projectMs=" + ms(projectNanos) +
                " modelBuildMs=" + ms(modelBuildNanos) +
                " bootstrapMs=" + ms(bootstrapNanos) +
                " modelBuilds=" + modelBuilds.get() +
                " descriptorReads=" + descriptorReads.get() +
                " modelCache(hit/miss)=" + modelCacheHits.get() + "/" + modelCacheMisses.get() +
                " effectiveMemo(hit/miss)=" + effectiveMemoHits.get() + "/" + effectiveMemoMisses.get();
    }

    private static long ms(AtomicLong nanos) {
        return nanos.get() / 1_000_000;
    }
}
