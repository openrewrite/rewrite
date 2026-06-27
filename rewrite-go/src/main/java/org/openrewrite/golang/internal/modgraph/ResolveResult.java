/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.golang.internal.modgraph;

import java.util.ArrayList;
import java.util.List;

/**
 * The resolved go1.17+ pruned module graph: the MVS-selected version of every
 * module ({@link #buildList}, mirroring {@code go list -m all}) and the require
 * edges between modules ({@link #graph}, mirroring {@code go mod graph}).
 * {@link #complete} is false when any dependency metadata could not be
 * read/fetched, making the result best-effort.
 */
public final class ResolveResult {

    final List<Module> buildList = new ArrayList<>();
    final List<Edge> graph = new ArrayList<>();
    boolean complete = true;

    public List<Module> buildList() {
        return buildList;
    }

    public List<Edge> graph() {
        return graph;
    }

    public boolean complete() {
        return complete;
    }

    /** One node of the build list at its MVS-selected version. */
    public static final class Module {
        public final String path;
        public final String version; // empty for the main module
        public final String goVersion; // the module's own `go` directive, "" if absent
        public final boolean main;

        public Module(String path, String version, String goVersion, boolean main) {
            this.path = path;
            this.version = version;
            this.goVersion = goVersion;
            this.main = main;
        }
    }

    /** A require edge from one module to another. */
    public static final class Edge {
        public final String fromPath;
        public final String fromVersion; // empty when From is the main module
        public final String toPath;
        public final String toVersion;
        public final boolean indirect;

        public Edge(String fromPath, String fromVersion, String toPath, String toVersion, boolean indirect) {
            this.fromPath = fromPath;
            this.fromVersion = fromVersion;
            this.toPath = toPath;
            this.toVersion = toVersion;
            this.indirect = indirect;
        }
    }
}
