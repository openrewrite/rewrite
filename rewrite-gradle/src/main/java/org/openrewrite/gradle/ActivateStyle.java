/*
 * Copyright 2022 the original author or authors.
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
import org.openrewrite.*;
import org.openrewrite.groovy.GroovyVisitor;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;
import org.openrewrite.properties.PropertiesParser;
import org.openrewrite.properties.PropertiesVisitor;
import org.openrewrite.properties.tree.Properties;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static org.openrewrite.Tree.randomId;

@Value
@EqualsAndHashCode(callSuper = true)
public class ActivateStyle extends Recipe {
    @Override
    public String getDisplayName() {
        return "Activate Style in Gradle Project";
    }

    @Override
    public String getDescription() {
        return "Sets the specified style as active. Once the style has been set, future recipes will use the specified " +
                "style for any changes they make. This recipe does not reformat anything on its own. " +
                "Prefers to set the `activeStyle()` method in the `rewrite` DSL in a build.gradle." +
                "If no `rewrite` DSL can be found to update, will instead place a \"systemProp.rewrite.activeStyles\" " +
                "entry within the project's gradle.properties. " +
                "Styles can be provided by rewrite itself, defined in a rewrite.yml, or provided by recipe modules.";
    }

    @Option(displayName = "Fully qualified style name",
            description = "The name of the style to activate.",
            example = "org.openrewrite.java.IntelliJ")
    String fullyQualifiedStyleName;

    @Option(displayName = "Overwrite existing styles",
            description = "When set to `true` this Recipe will clear all existing active styles. " +
                    "When `false` the new style will be added after existing styles.",
            required = false,
            example = "false")
    boolean overwriteExistingStyles;

    private static final String STYLE_PRESENT = "org.openrewrite.gradle.ActivateStyle.STYLE_PRESENT";

    @SuppressWarnings({"WrongPropertyKeyValueDelimiter", "UnusedProperty"})
    private static final String STYLE_KEY = "systemProp.rewrite.activeStyles";

    @Override
    protected List<SourceFile> visit(List<SourceFile> before, ExecutionContext ctx) {
        AtomicBoolean applicable = new AtomicBoolean();
        AtomicBoolean gradlePropsExists = new AtomicBoolean();
        List<SourceFile> after = ListUtils.map(before, sourceFile -> {
            String sourcePath = sourceFile.getSourcePath().toString();
            if(!applicable.get() && (sourcePath.endsWith(".gradle") || sourcePath.endsWith(".gradle.kts"))) {
                // Make sure at least one file that ends with ".gradle" or ".gradle.kts" exists, so we don't add a gradle.properties to a maven project
                applicable.set(true);
            }
            if (sourceFile instanceof Properties.File && "gradle.properties".equals(sourcePath)) {
                gradlePropsExists.set(true);
            }
            if (!(sourceFile instanceof G.CompilationUnit) || !sourcePath.endsWith(".gradle")) {
                return sourceFile;
            }
            return (G.CompilationUnit) new ActivateStyleBuildGradle().visit(sourceFile, ctx);
        });

        // Unable to apply style via the rewrite DSL, add it to gradle.properties (which may or may not already exist) instead
        if (applicable.get() && !ctx.pollMessage(STYLE_PRESENT, false)) {
            if (gradlePropsExists.get()) {
                after = ListUtils.map(after, sourceFile -> {
                    if (sourceFile instanceof Properties.File && "gradle.properties".equals(sourceFile.getSourcePath().toString())) {
                        return (SourceFile) new ActivateStyleGradleProperties().visit(sourceFile, ctx);
                    }
                    return sourceFile;
                });
            } else {
                after = ListUtils.concat(after, new PropertiesParser()
                        .parse(STYLE_KEY + "=" + fullyQualifiedStyleName).get(0)
                        .withSourcePath(Paths.get("gradle.properties")));
            }
        }

        return after;
    }

    private class ActivateStyleGradleProperties extends PropertiesVisitor<ExecutionContext> {
        @Override
        public Properties visitFile(Properties.File file, ExecutionContext executionContext) {
            Properties.File p = (Properties.File) super.visitFile(file, executionContext);
            AtomicBoolean keyExists = new AtomicBoolean();
            p = p.withContent(ListUtils.map(p.getContent(), content -> {
                if(!(content instanceof Properties.Entry)) {
                    return content;
                }
                Properties.Entry entry = (Properties.Entry) content;
                if(STYLE_KEY.equals(entry.getKey())) {
                    keyExists.set(true);
                    if(overwriteExistingStyles) {
                        if(!fullyQualifiedStyleName.equals(entry.getValue().getText())) {
                            entry = entry.withValue(entry.getValue().withText(fullyQualifiedStyleName));
                        }
                    } else {
                        List<String> activeStyles = Arrays.stream(entry.getValue().getText().split(",")).collect(Collectors.toList());
                        if (activeStyles.contains(fullyQualifiedStyleName)) {
                            return entry;
                        }
                        activeStyles.add(fullyQualifiedStyleName);
                        entry = entry.withValue(entry.getValue().withText(String.join(",", activeStyles)));
                    }
                }
                return entry;
            }));
            if(!keyExists.get()) {
                // Existing key not found, need to add one
                p = p.withContent(ListUtils.concat(p.getContent(),
                        new Properties.Entry(randomId(), "\n", Markers.EMPTY, STYLE_KEY, "", Properties.Entry.Delimiter.EQUALS,
                                new Properties.Value(randomId(), "", Markers.EMPTY, fullyQualifiedStyleName))));
            }
            return p;
        }
    }

    private class ActivateStyleBuildGradle extends GroovyVisitor<ExecutionContext> {
        @Override
        public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);
            if ("rewrite".equals(m.getSimpleName()) && m.getArguments().size() == 1 && m.getArguments().get(0) instanceof J.Lambda) {
                if (activeStyleAlreadySet(m)) {
                    ctx.putMessage(STYLE_PRESENT, true);
                    return m;
                }
                J.MethodInvocation maybeModified = (J.MethodInvocation) new ModifyActiveStyleInvocation().visitNonNull(m, ctx, getCursor().getParentOrThrow());
                if (maybeModified != m) {
                    ctx.putMessage(STYLE_PRESENT, true);
                    return maybeModified;
                }
                // No existing activeStyle() invocation to modify, need to add a new one
                J.Lambda rewriteConfigBlock = (J.Lambda) m.getArguments().get(0);
                if (!(rewriteConfigBlock.getBody() instanceof J.Block)) {
                    return m;
                }
                J.Block body = (J.Block) rewriteConfigBlock.getBody();
                J.MethodInvocation activeStyleInvocation = new J.MethodInvocation(randomId(), Space.format("\n"), Markers.EMPTY, null, null,
                        new J.Identifier(randomId(), Space.EMPTY, Markers.EMPTY, "activeStyle", null, null),
                        JContainer.build(Collections.singletonList(JRightPadded.build(
                                new J.Literal(randomId(), Space.EMPTY, Markers.EMPTY, fullyQualifiedStyleName,
                                        "\"" + fullyQualifiedStyleName + "\"", null, JavaType.Primitive.String)))),
                        null);
                activeStyleInvocation = autoFormat(activeStyleInvocation, ctx,
                        // Pass cursor indicating actual nesting level
                        new Cursor(getCursor(), body));
                body = body.withStatements(ListUtils.concat(body.getStatements(), activeStyleInvocation));
                m = m.withArguments(Collections.singletonList(rewriteConfigBlock.withBody(body)));
                ctx.putMessage(STYLE_PRESENT, true);
            }
            return m;
        }
    }

    private class ModifyActiveStyleInvocation extends GroovyVisitor<ExecutionContext> {
        @Override
        public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
            J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, executionContext);
            if ("activeStyle".equals(m.getSimpleName())) {
                J.Literal newEntry = new J.Literal(randomId(), Space.EMPTY, Markers.EMPTY, fullyQualifiedStyleName,
                        "\"" + fullyQualifiedStyleName + "\"", null, JavaType.Primitive.String);
                if(overwriteExistingStyles) {
                    m = m.withArguments(Collections.singletonList(newEntry));
                } else {
                    m = m.withArguments(ListUtils.concat(m.getArguments(), newEntry));
                }
                m = autoFormat(m, executionContext);
            }
            return m;
        }
    }

    private boolean activeStyleAlreadySet(J tree) {
        AtomicBoolean b = new AtomicBoolean();
        new FindActiveStyleInvocation().visit(tree, b);
        return b.get();
    }

    private class FindActiveStyleInvocation extends GroovyVisitor<AtomicBoolean> {
        @Override
        public J visitMethodInvocation(J.MethodInvocation m, AtomicBoolean found) {
            if ("activeStyle".equals(m.getSimpleName())) {
                for (Expression arg : m.getArguments()) {
                    if (!(arg instanceof J.Literal)) {
                        continue;
                    }
                    J.Literal a = (J.Literal) arg;
                    if (fullyQualifiedStyleName.equals(a.getValue())) {
                        found.set(true);
                        break;
                    }
                }
                return m;
            }
            return super.visitMethodInvocation(m, found);
        }
    }
}
