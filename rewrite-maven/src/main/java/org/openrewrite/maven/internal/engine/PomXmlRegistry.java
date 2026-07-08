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

import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.maven.tree.ResolvedGroupArtifactVersion;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static java.util.Collections.emptyMap;

/**
 * Per-context registry of the printed pom XML bytes the resolution engine reads (DESIGN §0 XML-first). {@code MavenParser}
 * populates it with each project pom's printed document bytes at parse time; {@code UpdateMavenModel} refreshes it with a
 * mutated document's bytes and bumps the reactor epoch on re-resolution. All state lives on the {@link ExecutionContext}
 * as internal messages — additive, no public API. Synthetic {@code Pom.builder()} graphs have no entry here and route
 * through {@link PomToModelConverter} at the facade.
 */
public final class PomXmlRegistry {

    private static final String BYTES_KEY = "org.openrewrite.maven.internal.engine.pomXmlBytes";
    private static final String EPOCH_KEY = "org.openrewrite.maven.internal.engine.reactorEpoch";
    private static final String INJECTED_PROPERTIES_KEY = "org.openrewrite.maven.internal.engine.injectedProperties";

    private PomXmlRegistry() {
    }

    @SuppressWarnings("unchecked")
    private static Map<String, byte[]> bytes(ExecutionContext ctx) {
        return ctx.computeMessageIfAbsent(BYTES_KEY, k -> new ConcurrentHashMap<String, byte[]>());
    }

    private static String pathKey(Path sourcePath) {
        return "path:" + sourcePath;
    }

    private static String gavKey(ResolvedGroupArtifactVersion gav) {
        return "gav:" + gav.getGroupId() + ":" + gav.getArtifactId() + ":" + gav.getVersion();
    }

    /** Record a project pom's printed XML, keyed by both its source path and its GAV so either lookup finds it. */
    public static void put(ExecutionContext ctx, @Nullable Path sourcePath, @Nullable ResolvedGroupArtifactVersion gav, byte[] xml) {
        Map<String, byte[]> bytes = bytes(ctx);
        if (sourcePath != null) {
            bytes.put(pathKey(sourcePath), xml);
        }
        if (gav != null) {
            bytes.put(gavKey(gav), xml);
        }
    }

    public static byte @Nullable [] get(ExecutionContext ctx, @Nullable Path sourcePath, @Nullable ResolvedGroupArtifactVersion gav) {
        Map<String, byte[]> bytes = bytes(ctx);
        byte[] found = sourcePath == null ? null : bytes.get(pathKey(sourcePath));
        if (found == null && gav != null) {
            found = bytes.get(gavKey(gav));
        }
        return found;
    }

    /** The {@code Function<Path, byte[]>} a {@link ReactorWorkspace} reads printed bytes through. */
    public static Function<Path, byte @Nullable []> pathSource(ExecutionContext ctx) {
        Map<String, byte[]> bytes = bytes(ctx);
        return path -> path == null ? null : bytes.get(pathKey(path));
    }

    private static AtomicInteger epochCounter(ExecutionContext ctx) {
        return ctx.computeMessageIfAbsent(EPOCH_KEY, k -> new AtomicInteger());
    }

    public static int epoch(ExecutionContext ctx) {
        return epochCounter(ctx).get();
    }

    /** Called on marker replacement (re-resolution): fresh workspaces seed their epoch from here. */
    public static int bumpEpoch(ExecutionContext ctx) {
        return epochCounter(ctx).incrementAndGet();
    }

    /** The parser/host {@code -D}-style properties overlaid onto the effective model (see {@code EffectivePomMapper}). */
    public static void setInjectedProperties(ExecutionContext ctx, Map<String, String> injectedProperties) {
        ctx.putMessage(INJECTED_PROPERTIES_KEY, injectedProperties);
    }

    public static Map<String, String> injectedProperties(ExecutionContext ctx) {
        Map<String, String> injected = ctx.getMessage(INJECTED_PROPERTIES_KEY);
        return injected == null ? emptyMap() : injected;
    }
}
