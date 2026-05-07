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
package org.openrewrite.gradle.gradle9;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.gradle.GradleParser;
import org.openrewrite.gradle.IsBuildGradle;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Value
@EqualsAndHashCode(callSuper = false)
public class UseJavaExtensionBlock extends Recipe {

    private static final String SOURCE = "sourceCompatibility";
    private static final String TARGET = "targetCompatibility";
    private static final String VERSIONS_KEY = "USE_JAVA_EXTENSION_BLOCK_VERSIONS";

    @Override
    public String getDisplayName() {
        return "Move `sourceCompatibility` and `targetCompatibility` into the `java { }` extension block";
    }

    @Override
    public String getDescription() {
        return "Gradle 9 removed the `JavaPluginConvention` (deprecated in 8.2). Top-level `sourceCompatibility` and " +
                "`targetCompatibility` assignments in a Groovy build script previously delegated to that convention object " +
                "and stop working in Gradle 9. Move them into the `java { }` extension block, normalizing values to " +
                "`JavaVersion.VERSION_<n>` and adding the missing counterpart so both properties are set explicitly. " +
                "See the [Gradle upgrade guide](https://docs.gradle.org/9.0.0/userguide/upgrading_major_version_9.html) for more information.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new IsBuildGradle<>(), new JavaVisitor<ExecutionContext>() {

            @Override
            public @Nullable J visit(@Nullable Tree tree, ExecutionContext ctx) {
                J visited = super.visit(tree, ctx);
                Map<String, Expression> versionsToMove = getCursor().pollMessage(VERSIONS_KEY);
                if (!(visited instanceof G.CompilationUnit) || versionsToMove == null || versionsToMove.isEmpty()) {
                    return visited;
                }
                G.CompilationUnit cu = (G.CompilationUnit) visited;

                Expression srcVal = versionsToMove.get(SOURCE);
                Expression tgtVal = versionsToMove.get(TARGET);
                if (srcVal != null && tgtVal == null) {
                    versionsToMove.put(TARGET, srcVal);
                } else if (tgtVal != null && srcVal == null) {
                    versionsToMove.put(SOURCE, tgtVal);
                }

                J.MethodInvocation incomingBlock = (J.MethodInvocation) buildJavaBlock(versionsToMove, ctx);

                List<Statement> originalStatements = cu.getStatements();
                List<Statement> mapped = ListUtils.map(originalStatements, s ->
                        isJavaBlock(s) ? mergeIntoExistingJavaBlock((J.MethodInvocation) s, incomingBlock) : s);
                if (mapped == originalStatements) {
                    mapped = ListUtils.concat(originalStatements, incomingBlock.withPrefix(Space.format("\n\n")));
                }
                return cu.withStatements(mapped);
            }

            @Override
            public @Nullable J visitAssignment(J.Assignment assignment, ExecutionContext ctx) {
                if (!(getCursor().getParentTreeCursor().getValue() instanceof G.CompilationUnit)) {
                    return assignment;
                }
                String name = compatibilityName(assignment);
                if (name == null) {
                    return assignment;
                }
                getCursor().getRoot().<Map<String, Expression>>computeMessageIfAbsent(
                        VERSIONS_KEY, k -> new LinkedHashMap<>()
                ).put(name, assignment.getAssignment());
                return null;
            }
        });
    }

    private static boolean isJavaBlock(Statement s) {
        if (!(s instanceof J.MethodInvocation)) {
            return false;
        }
        J.MethodInvocation m = (J.MethodInvocation) s;
        return "java".equals(m.getSimpleName()) &&
                m.getArguments().size() == 1 &&
                m.getArguments().get(0) instanceof J.Lambda;
    }

    private static J.MethodInvocation mergeIntoExistingJavaBlock(J.MethodInvocation existing, J.MethodInvocation incoming) {
        J.Lambda existingLambda = (J.Lambda) existing.getArguments().get(0);
        if (!(existingLambda.getBody() instanceof J.Block)) {
            return existing;
        }
        J.Block existingBody = (J.Block) existingLambda.getBody();

        Set<String> existingNames = new HashSet<>();
        for (Statement bs : existingBody.getStatements()) {
            String n = compatibilityName(bs);
            if (n != null) {
                existingNames.add(n);
            }
        }

        J.Lambda incomingLambda = (J.Lambda) incoming.getArguments().get(0);
        List<Statement> incomingStatements = ((J.Block) incomingLambda.getBody()).getStatements();
        List<Statement> incomingToAdd = ListUtils.map(incomingStatements, s -> {
            String n = compatibilityName(s);
            return n != null && existingNames.contains(n) ? null : s;
        });
        if (incomingToAdd.isEmpty()) {
            return existing;
        }
        List<Statement> merged = ListUtils.concatAll(existingBody.getStatements(), incomingToAdd);
        return existing.withArguments(Collections.singletonList(
                existingLambda.withBody(existingBody.withStatements(merged))));
    }

    private static Statement buildJavaBlock(Map<String, Expression> entries, ExecutionContext ctx) {
        StringBuilder snippet = new StringBuilder("\njava {\n");
        Set<String> needsReplacement = new HashSet<>();
        appendEntry(snippet, SOURCE, entries, needsReplacement);
        appendEntry(snippet, TARGET, entries, needsReplacement);
        snippet.append("}\n");

        G.CompilationUnit parsed = (G.CompilationUnit) GradleParser.builder().build()
                .parse(ctx, snippet.toString())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Unable to parse `java { }` block"));
        if (parsed.getStatements().isEmpty()) {
            throw new IllegalStateException("Parsed `java { }` block is empty");
        }
        Statement template = parsed.getStatements().get(0);
        if (needsReplacement.isEmpty()) {
            return template;
        }
        return (Statement) new JavaIsoVisitor<Integer>() {
            @Override
            public J.Assignment visitAssignment(J.Assignment a, Integer i) {
                String name = compatibilityName(a);
                if (name == null || !needsReplacement.contains(name)) {
                    return super.visitAssignment(a, i);
                }
                return a.withAssignment(entries.get(name).withPrefix(a.getAssignment().getPrefix()));
            }
        }.visitNonNull(template, 0);
    }

    private static void appendEntry(StringBuilder snippet, String name, Map<String, Expression> entries, Set<String> needsReplacement) {
        if (!entries.containsKey(name)) {
            return;
        }
        Integer version = extractVersion(entries.get(name));
        if (version != null) {
            snippet.append("    ").append(name).append(" = ").append(enumForm(version)).append("\n");
        } else {
            snippet.append("    ").append(name).append(" = JavaVersion.VERSION_17\n");
            needsReplacement.add(name);
        }
    }

    private static String enumForm(int version) {
        return version <= 8 ? "JavaVersion.VERSION_1_" + version : "JavaVersion.VERSION_" + version;
    }

    private static @Nullable String compatibilityName(Statement s) {
        J.Assignment a = asAssignment(s);
        if (a == null) {
            return null;
        }
        Expression v = a.getVariable();
        String name = null;
        if (v instanceof J.Identifier) {
            name = ((J.Identifier) v).getSimpleName();
        } else if (v instanceof J.FieldAccess) {
            name = ((J.FieldAccess) v).getSimpleName();
        }
        if (SOURCE.equals(name) || TARGET.equals(name)) {
            return name;
        }
        return null;
    }

    private static J.@Nullable Assignment asAssignment(Statement s) {
        if (s instanceof J.Assignment) {
            return (J.Assignment) s;
        }
        if (s instanceof J.Return) {
            Expression e = ((J.Return) s).getExpression();
            if (e instanceof J.Assignment) {
                return (J.Assignment) e;
            }
        }
        return null;
    }

    private static @Nullable Integer extractVersion(Expression e) {
        if (e instanceof J.Literal) {
            J.Literal lit = (J.Literal) e;
            JavaType.Primitive type = lit.getType();
            Object value = lit.getValue();
            if (type == JavaType.Primitive.String && value instanceof String) {
                return parseMajor((String) value);
            }
            if (type == JavaType.Primitive.Int && value instanceof Integer) {
                return (Integer) value;
            }
            if (type == JavaType.Primitive.Double && value != null) {
                return parseMajor(value.toString());
            }
        } else if (e instanceof J.FieldAccess) {
            J.FieldAccess fa = (J.FieldAccess) e;
            return parseEnumName(fa.getName().getSimpleName());
        } else if (e instanceof J.MethodInvocation) {
            J.MethodInvocation m = (J.MethodInvocation) e;
            if ("toVersion".equals(m.getSimpleName()) && m.getArguments().size() == 1) {
                return extractVersion(m.getArguments().get(0));
            }
        }
        return null;
    }

    private static @Nullable Integer parseMajor(@Nullable String version) {
        if (version == null) {
            return null;
        }
        String v = version.replace("\"", "").replace("'", "");
        if (v.startsWith("1.")) {
            v = v.substring(2);
        }
        if (v.contains("_")) {
            v = v.substring(v.lastIndexOf("_") + 1);
        }
        if (v.contains(".")) {
            v = v.substring(0, v.indexOf("."));
        }
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static @Nullable Integer parseEnumName(String name) {
        if (!name.startsWith("VERSION_")) {
            return null;
        }
        String rest = name.substring("VERSION_".length());
        if (rest.startsWith("1_")) {
            rest = rest.substring(2);
        }
        try {
            return Integer.parseInt(rest);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
