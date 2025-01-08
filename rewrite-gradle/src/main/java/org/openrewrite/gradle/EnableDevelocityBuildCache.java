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
package org.openrewrite.gradle;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.groovy.GroovyIsoVisitor;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.tree.J;

import java.nio.file.Paths;

import static java.util.Collections.singletonList;

@Value
@EqualsAndHashCode(callSuper = false)
public class EnableDevelocityBuildCache extends Recipe {

    @Override
    public String getDisplayName() {
        return "Enable Develocity build cache";
    }

    @Override
    public String getDescription() {
        return "Add configuration to enable Develocity build cache, the recipe requires `develocity` " +
               "configuration without `buildCache` configuration to be present. Only work for Groovy DSL.";
    }

    @Option(displayName = "Enable remote build cache",
            description = "Value for `//develocity/buildCache/remote/enabled`.",
            example = "true",
            required = false)
    @Nullable
    String remoteEnabled;

    @Option(displayName = "Enable remote build cache push",
            description = "Value for `//develocity/buildCache/remote/storeEnabled`.",
            example = "true",
            required = false)
    @Nullable
    String remotePushEnabled;

    @Override
    public Validated<Object> validate(ExecutionContext ctx) {
        return super.validate(ctx)
                .or(Validated.notBlank("remoteEnabled", remoteEnabled)
                        .or(Validated.notBlank("remotePushEnabled", remotePushEnabled)));
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new IsSettingsGradle<>(), new GroovyIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);

                if ("buildCache".equals(method.getSimpleName())) {
                    try {
                        Cursor parent = getCursor().dropParentUntil(v -> v instanceof J.MethodInvocation && "develocity".equals(((J.MethodInvocation) v).getSimpleName()));
                        parent.putMessage("hasBuildCacheConfig", true);
                    } catch (IllegalStateException e) {
                        // ignore, this means we're not in a develocity block
                    }
                    return m;
                }

                if ("develocity".equals(method.getSimpleName()) && getCursor().pollMessage("hasBuildCacheConfig") == null) {
                    J.MethodInvocation buildCache = develocityBuildCacheTemplate("    ", ctx);
                    return maybeAutoFormat(m, m.withArguments(ListUtils.mapFirst(m.getArguments(), arg -> {
                        if (arg instanceof J.Lambda) {
                            J.Lambda lambda = (J.Lambda) arg;
                            J.Block block = (J.Block) lambda.getBody();
                            return lambda.withBody(block.withStatements(ListUtils.concat(block.getStatements(), buildCache)));
                        }
                        return arg;
                    })), ctx);
                }
                return m;
            }
        });
    }

    private J.@Nullable MethodInvocation develocityBuildCacheTemplate(String indent, ExecutionContext ctx) {
        StringBuilder ge = new StringBuilder("\ndevelocity {\n");
        ge.append(indent).append("buildCache {\n");

        ge.append(indent).append(indent).append("remote(develocity.buildCache) {\n");
        if (!StringUtils.isBlank(remoteEnabled)) {
            ge.append(indent).append(indent).append(indent).append("enabled = ").append(remoteEnabled).append("\n");
        }

        if (!StringUtils.isBlank(remotePushEnabled)) {
            ge.append(indent).append(indent).append(indent).append("push = ").append(remotePushEnabled).append("\n");
        }
        ge.append(indent).append(indent).append("}\n");
        ge.append(indent).append("}\n");
        ge.append("}\n");

        G.CompilationUnit cu = GradleParser.builder().build()
                .parseInputs(singletonList(
                        Parser.Input.fromString(Paths.get("settings.gradle"), ge.toString())), null, ctx)
                .map(G.CompilationUnit.class::cast)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Could not parse as Gradle"));

        J.MethodInvocation develocity = (J.MethodInvocation) cu.getStatements().get(0);
        return (J.MethodInvocation) ((J.Return) ((J.Block) ((J.Lambda) develocity.getArguments().get(0)).getBody()).getStatements().get(0)).getExpression();
    }
}
