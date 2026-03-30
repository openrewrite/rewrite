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

import java.util.concurrent.atomic.AtomicBoolean;

@Value
@EqualsAndHashCode(callSuper = false)
public class EnableDevelocityBuildCache extends Recipe {

    String displayName = "Enable Develocity build cache";

    String description = "Adds `buildCache` configuration to `develocity` where not yet present.";

    @Option(displayName = "Enable remote build cache",
            description = "Value for `//develocity/buildCache/remote/enabled`.",
            example = "true",
            required = false)
    @Nullable
    String remoteEnabled;

    @Option(displayName = "Enable remote build cache push",
            description = "Value for `//develocity/buildCache/remote/storeEnabled`.",
            example = "System.getenv(\"CI\") != null",
            required = false)
    @Nullable
    String remotePushEnabled;

    @Override
    public Validated<Object> validate(ExecutionContext ctx) {
        return super.validate(ctx)
                .and(Validated.notBlank("remoteEnabled", remoteEnabled)
                        .or(Validated.notBlank("remotePushEnabled", remotePushEnabled)));
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new IsSettingsGradle<>(), new GroovyIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                if ("develocity".equals(method.getSimpleName()) && !hasBuildCache(method)) {
                    J.MethodInvocation buildCache = createBuildCache(ctx);
                    return maybeAutoFormat(method, method.withArguments(ListUtils.mapFirst(method.getArguments(), arg -> {
                        if (arg instanceof J.Lambda) {
                            J.Lambda lambda = (J.Lambda) arg;
                            J.Block block = (J.Block) lambda.getBody();
                            return lambda.withBody(block.withStatements(ListUtils.concat(block.getStatements(), buildCache)));
                        }
                        return arg;
                    })), ctx);
                }
                return method;
            }

            private boolean hasBuildCache(J.MethodInvocation m) {
                return new GroovyIsoVisitor<AtomicBoolean>() {
                    @Override
                    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, AtomicBoolean atomicBoolean) {
                        if ("buildCache".equals(method.getSimpleName())) {
                            atomicBoolean.set(true);
                            return method;
                        }
                        return super.visitMethodInvocation(method, atomicBoolean);
                    }
                }.reduce(m, new AtomicBoolean(false), getCursor().getParentTreeCursor()).get();
            }
        });
    }

    private J.MethodInvocation createBuildCache(ExecutionContext ctx) {
        String conf = "buildCache {\n" +
                "    remote(develocity.buildCache) {\n";
        if (!StringUtils.isBlank(remoteEnabled)) {
            conf += "        enabled = " + remoteEnabled + "\n";
        }
        if (!StringUtils.isBlank(remotePushEnabled)) {
            conf += "        push = " + remotePushEnabled + "\n";
        }
        conf += "    }" +
                "}";
        return (J.MethodInvocation) GradleParser.builder().build()
                .parse(ctx, conf)
                .map(G.CompilationUnit.class::cast)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Could not parse as Gradle"))
                .getStatements()
                .get(0);
    }
}
