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
import org.openrewrite.groovy.GroovyTemplate;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaCoordinates;
import org.openrewrite.java.tree.Statement;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Objects.requireNonNull;
import static org.openrewrite.gradle.GradleParser.requireParsed;

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
                    return method.withArguments(ListUtils.mapFirst(method.getArguments(), arg -> {
                        if (!(arg instanceof J.Lambda)) {
                            return arg;
                        }
                        J.Block body = (J.Block) ((J.Lambda) arg).getBody();
                        return addBuildCache(new Cursor(getCursor(), arg), body.getCoordinates().lastStatement(), ctx);
                    }));
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

    private Expression addBuildCache(Cursor scope, JavaCoordinates coordinates, ExecutionContext ctx) {
        StringBuilder template = new StringBuilder("buildCache {\n    remote(develocity.buildCache) {\n");
        List<Expression> settings = new ArrayList<>(2);
        if (!StringUtils.isBlank(remoteEnabled)) {
            template.append("        enabled = #{any()}\n");
            settings.add(parseExpression(remoteEnabled, ctx));
        }
        if (!StringUtils.isBlank(remotePushEnabled)) {
            template.append("        push = #{any()}\n");
            settings.add(parseExpression(remotePushEnabled, ctx));
        }
        template.append("    }\n}");
        return GroovyTemplate.builder(template.toString())
                .build()
                .apply(scope, coordinates, settings.toArray());
    }

    /**
     * Parses an option's value on its own, so that the template receives the user's expression as a tree. Were it
     * spliced into the template text instead, a value like {@code System.getenv("#{CI}") != null} would have its
     * {@code #{...}} claimed by the template's own placeholder syntax.
     */
    private static Expression parseExpression(String source, ExecutionContext ctx) {
        Statement statement = GradleParser.builder().build()
                .parse(ctx, source)
                .map(requireParsed(G.CompilationUnit.class))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Could not parse as Gradle: " + source))
                .getStatements()
                .get(0);
        // A script's trailing statement carries an implicit return
        return statement instanceof J.Return ?
                requireNonNull(((J.Return) statement).getExpression()) :
                (Expression) statement;
    }
}
