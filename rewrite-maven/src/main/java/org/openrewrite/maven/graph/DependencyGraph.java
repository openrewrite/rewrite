/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.maven.graph;

import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.maven.tree.ResolvedGroupArtifactVersion;

import java.util.List;

public class DependencyGraph {
    public static String render(String scopeOrConfig, List<ResolvedDependency> dependencyPath) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < dependencyPath.size(); i++) {
            appendIndentedLine(sb, i, formatDependency(dependencyPath.get(i).getGav()));
        }
        appendIndentedLine(sb, dependencyPath.size(), scopeOrConfig);
        return sb.toString();
    }

    private static void appendIndentedLine(StringBuilder tree, int depth, String content) {
        tree.append("\n");
        for (int i = 0; i < depth; i++) {
            tree.append("     ");
        }
        tree.append("\\--- ").append(content);
    }

    private static String formatDependency(ResolvedGroupArtifactVersion gav) {
        return gav.getGroupId() + ":" + gav.getArtifactId() + ":" + gav.getVersion();
    }
}
