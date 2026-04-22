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
package org.openrewrite.maven.utilities;

import org.openrewrite.ExecutionContext;
import org.openrewrite.HttpSenderExecutionContextView;
import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.java.marker.JavaSourceSet;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.maven.MavenExecutionContextView;
import org.openrewrite.maven.tree.Dependency;
import org.openrewrite.maven.tree.GroupArtifactVersion;
import org.openrewrite.maven.tree.MavenRepository;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.maven.tree.ResolvedGroupArtifactVersion;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Updates {@link JavaSourceSet} markers to reflect dependency changes made by recipes.
 * <p>
 * When dependency-modifying recipes (ChangeDependency, AddDependency) change a project's
 * dependencies, the JavaSourceSet marker on Java source files becomes stale — it still
 * reflects the pre-change classpath. This utility downloads the new dependency's JAR,
 * scans it for type names, and updates the JavaSourceSet accordingly.
 */
public class JavaSourceSetUpdater {
    static final String TYPE_CACHE_KEY = "org.openrewrite.maven.jarTypeCache";

    private final MavenArtifactDownloader downloader;
    private final Map<String, List<JavaType.FullyQualified>> typeCache;

    @SuppressWarnings("unchecked")
    public JavaSourceSetUpdater(ExecutionContext ctx) {
        MavenExecutionContextView mctx = MavenExecutionContextView.view(ctx);
        HttpSender httpSender = HttpSenderExecutionContextView.view(ctx).getHttpSender();
        // Download failures are not fatal for JavaSourceSet updates — degrade gracefully
        Consumer<Throwable> onError = t -> {};
        this.downloader = new MavenArtifactDownloader(mctx.getArtifactCache(), mctx.getSettings(), httpSender, onError);
        this.typeCache = (Map<String, List<JavaType.FullyQualified>>) ctx.getMessages()
                .computeIfAbsent(TYPE_CACHE_KEY, k -> new ConcurrentHashMap<>());
    }

    /**
     * Update a JavaSourceSet to reflect a dependency coordinate change.
     * Currently a no-op: JAR downloading is disabled while the classpath-update
     * approach is being reconsidered.
     */
    public JavaSourceSet changeDependency(JavaSourceSet sourceSet,
                                          ResolvedDependency oldDep,
                                          ResolvedDependency newDep) {
        return sourceSet;
    }

    /**
     * Update a JavaSourceSet to reflect a newly added dependency.
     * Currently a no-op: JAR downloading is disabled while the classpath-update
     * approach is being reconsidered.
     */
    public JavaSourceSet addDependency(JavaSourceSet sourceSet,
                                       String groupId, String artifactId, String version,
                                       List<MavenRepository> repositories) {
        return sourceSet;
    }

    private List<JavaType.FullyQualified> downloadAndScanTypes(ResolvedDependency dep) {
        return Collections.emptyList();
    }

    private static String gavKey(ResolvedDependency dep) {
        return dep.getGroupId() + ":" + dep.getArtifactId() + ":" + dep.getVersion();
    }
}
