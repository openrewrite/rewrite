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
import org.openrewrite.maven.tree.Pom;
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
 * <p>
 * Each entry remembers the {@link Pom} its bytes were printed from, so {@link #get(ExecutionContext, Pom)} serves them
 * only while the {@code requested} pom still matches. A recipe that mutates the requested pom and re-resolves without
 * refreshing this registry (e.g. {@code ChangeParentPom}'s scanner, which re-resolves a {@code withParent} pom directly)
 * therefore does not get stale bytes; the facade falls back to {@link PomToModelConverter} on the mutated pom.
 */
public final class PomXmlRegistry {

    private static final String BYTES_KEY = "org.openrewrite.maven.internal.engine.pomXmlBytes";
    private static final String EPOCH_KEY = "org.openrewrite.maven.internal.engine.reactorEpoch";
    private static final String INJECTED_PROPERTIES_KEY = "org.openrewrite.maven.internal.engine.injectedProperties";

    private PomXmlRegistry() {
    }

    private static final class Entry {
        final byte[] xml;
        final Pom source;

        Entry(byte[] xml, Pom source) {
            this.xml = xml;
            this.source = source;
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Entry> entries(ExecutionContext ctx) {
        return ctx.computeMessageIfAbsent(BYTES_KEY, k -> new ConcurrentHashMap<String, Entry>());
    }

    private static String pathKey(Path sourcePath) {
        return "path:" + sourcePath;
    }

    private static String gavKey(ResolvedGroupArtifactVersion gav) {
        return "gav:" + gav.getGroupId() + ":" + gav.getArtifactId() + ":" + gav.getVersion();
    }

    /** Record a project pom's printed XML, keyed by both its source path and its GAV so either lookup finds it. */
    public static void put(ExecutionContext ctx, Pom source, byte[] xml) {
        Map<String, Entry> entries = entries(ctx);
        Entry entry = new Entry(xml, source);
        if (source.getSourcePath() != null) {
            entries.put(pathKey(source.getSourcePath()), entry);
        }
        if (source.getGav() != null) {
            entries.put(gavKey(source.getGav()), entry);
        }
    }

    /**
     * The printed XML for {@code requested}, or {@code null} if no entry matches it. An entry matches only when the pom
     * its bytes were printed from still equals {@code requested}, so a mutated re-resolution never reads stale bytes.
     */
    public static byte @Nullable [] get(ExecutionContext ctx, Pom requested) {
        Map<String, Entry> entries = entries(ctx);
        Entry entry = requested.getSourcePath() == null ? null : entries.get(pathKey(requested.getSourcePath()));
        if ((entry == null || !entry.source.equals(requested)) && requested.getGav() != null) {
            entry = entries.get(gavKey(requested.getGav()));
        }
        return entry != null && entry.source.equals(requested) ? entry.xml : null;
    }

    /** The {@code Function<Path, byte[]>} a {@link ReactorWorkspace} reads printed bytes through. */
    public static Function<Path, byte @Nullable []> pathSource(ExecutionContext ctx) {
        Map<String, Entry> entries = entries(ctx);
        return path -> {
            if (path == null) {
                return null;
            }
            Entry entry = entries.get(pathKey(path));
            return entry == null ? null : entry.xml;
        };
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
