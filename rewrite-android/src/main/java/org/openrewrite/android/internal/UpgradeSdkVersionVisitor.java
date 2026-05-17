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
package org.openrewrite.android.internal;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.marker.Markup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.IntPredicate;

/**
 * Visitor scaffolding shared by {@code UpgradeCompileSdkVersion},
 * {@code UpgradeTargetSdkVersion}, and {@code UpgradeMinSdkVersion}.
 * <p>
 * Discovers SDK assignment sites inside an {@code android { }} block (or its
 * {@code defaultConfig { }} child) in either Groovy or Kotlin DSL build scripts,
 * classifies each value source via {@link SdkVersionValueSourceResolver}, and
 * rewrites literal/string/extra-property forms in-place. Catalog and
 * gradle.properties forms are reported to the caller via {@link #findings} so a
 * companion visitor can update the appropriate sibling file.
 */
public class UpgradeSdkVersionVisitor extends JavaIsoVisitor<ExecutionContext> {

    public static class Finding {
        public final SdkVersionValueSource source;
        public final int from;
        public final int to;

        public Finding(SdkVersionValueSource source, int from, int to) {
            this.source = source;
            this.from = from;
            this.to = to;
        }
    }

    private final Set<String> sdkAssignmentNames;
    private final int newValue;
    private final IntPredicate currentValueAcceptable;
    private final boolean mutate;

    private final List<Finding> findings = new ArrayList<>();
    private boolean inAndroidScope;

    /**
     * @param sdkAssignmentNames    LHS identifier names recognized as SDK assignments (e.g. {"compileSdk", "compileSdkVersion"}).
     * @param newValue              the target SDK integer.
     * @param currentValueAcceptable returns {@code true} if the current literal value should trigger an upgrade.
     *                               Use {@code current -> current < newValue} for the standard "don't downgrade" rule;
     *                               for targetSdk with floor, pre-bake the floor into this predicate.
     * @param mutate                when {@code true}, in-place literal/string forms are rewritten and UNRESOLVED sites get Markup.warn.
     *                               When {@code false}, the visitor only collects findings without changing the tree.
     */
    public UpgradeSdkVersionVisitor(Set<String> sdkAssignmentNames, int newValue, IntPredicate currentValueAcceptable, boolean mutate) {
        this.sdkAssignmentNames = sdkAssignmentNames;
        this.newValue = newValue;
        this.currentValueAcceptable = currentValueAcceptable;
        this.mutate = mutate;
    }

    public List<Finding> getFindings() {
        return Collections.unmodifiableList(findings);
    }

    public static boolean isBuildGradle(@Nullable SourceFile sf) {
        if (sf == null) {
            return false;
        }
        String path = sf.getSourcePath().toString();
        boolean groovy = sf instanceof G.CompilationUnit && path.endsWith(".gradle");
        boolean kotlin = sf instanceof K.CompilationUnit && path.endsWith(".gradle.kts");
        return (groovy || kotlin) && !path.endsWith("settings.gradle") && !path.endsWith("settings.gradle.kts");
    }

    @Override
    public @Nullable J visit(@Nullable Tree tree, ExecutionContext ctx) {
        if (tree instanceof JavaSourceFile && !isBuildGradle((SourceFile) tree)) {
            return (J) tree;
        }
        return super.visit(tree, ctx);
    }

    @Override
    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
        // Track whether we're inside `android { ... }` (or a nested config block within it).
        String name = method.getSimpleName();
        boolean enteredAndroidScope = false;
        if (!inAndroidScope && "android".equals(name) && isClosure(method)) {
            inAndroidScope = true;
            enteredAndroidScope = true;
        }
        try {
            // String form: `compileSdkVersion 'android-34'` (Groovy) or `compileSdkVersion("android-34")` (Kotlin)
            if (inAndroidScope && sdkAssignmentNames.contains(name) &&
                    method.getArguments().size() == 1 &&
                    !(method.getArguments().get(0) instanceof J.Empty)) {
                Expression arg = method.getArguments().get(0);
                SdkVersionValueSource source = SdkVersionValueSourceResolver.resolve(arg);
                Finding finding = recordAndDispatch(source, getCursor());
                if (mutate && finding != null && finding.source.getKind() == SdkVersionValueSource.Kind.LITERAL_INT) {
                    J.Literal newLit = ((J.Literal) arg)
                            .withValue(newValue)
                            .withValueSource(String.valueOf(newValue));
                    return method.withArguments(Collections.singletonList(newLit));
                }
                if (mutate && finding != null && finding.source.getKind() == SdkVersionValueSource.Kind.LITERAL_STRING) {
                    J.Literal lit = (J.Literal) arg;
                    String prefix = finding.source.getDetail() == null ? "" : finding.source.getDetail();
                    String newStr = prefix + newValue;
                    String src = lit.getValueSource();
                    String quote = src == null || src.isEmpty() ? "\"" : src.substring(0, 1);
                    J.Literal newLit = lit.withValue(newStr).withValueSource(quote + newStr + quote);
                    return method.withArguments(Collections.singletonList(newLit));
                }
                if (mutate && finding != null && finding.source.getKind() == SdkVersionValueSource.Kind.UNRESOLVED) {
                    return Markup.warn(method, new IllegalStateException(
                            "could not locate value source for " + name + ": " + finding.source.getSourceDescription()));
                }
                // EXTRA_PROPERTY / VERSION_CATALOG / GRADLE_PROPERTIES: leave call site, sibling visitor rewrites the source.
                return super.visitMethodInvocation(method, ctx);
            }
            return super.visitMethodInvocation(method, ctx);
        } finally {
            if (enteredAndroidScope) {
                inAndroidScope = false;
            }
        }
    }

    @Override
    public J.Assignment visitAssignment(J.Assignment assignment, ExecutionContext ctx) {
        J.Assignment a = super.visitAssignment(assignment, ctx);
        if (!inAndroidScope) {
            return a;
        }
        String lhsName = lhsName(a.getVariable());
        if (lhsName == null || !sdkAssignmentNames.contains(lhsName)) {
            return a;
        }
        SdkVersionValueSource source = SdkVersionValueSourceResolver.resolve(a.getAssignment());
        Finding finding = recordAndDispatch(source, getCursor());
        if (finding == null) {
            return a;
        }
        if (mutate && finding.source.getKind() == SdkVersionValueSource.Kind.LITERAL_INT) {
            J.Literal lit = (J.Literal) a.getAssignment();
            return a.withAssignment(lit.withValue(newValue).withValueSource(String.valueOf(newValue)));
        }
        if (mutate && finding.source.getKind() == SdkVersionValueSource.Kind.LITERAL_STRING) {
            J.Literal lit = (J.Literal) a.getAssignment();
            String prefix = finding.source.getDetail() == null ? "" : finding.source.getDetail();
            String newStr = prefix + newValue;
            String src = lit.getValueSource();
            String quote = src == null || src.isEmpty() ? "\"" : src.substring(0, 1);
            return a.withAssignment(lit.withValue(newStr).withValueSource(quote + newStr + quote));
        }
        if (mutate && finding.source.getKind() == SdkVersionValueSource.Kind.UNRESOLVED) {
            return Markup.warn(a, new IllegalStateException(
                    "could not locate value source for " + lhsName + ": " + finding.source.getSourceDescription()));
        }
        // EXTRA_PROPERTY / VERSION_CATALOG / GRADLE_PROPERTIES: hand off to the relevant secondary visitor via findings.
        return a;
    }

    private @Nullable Finding recordAndDispatch(SdkVersionValueSource source, Cursor cursor) {
        Integer current = source.getCurrentValue();
        if (current != null && !currentValueAcceptable.test(current)) {
            // Already at or above target. No-op; do not record (so the recipe knows nothing was changed).
            return null;
        }
        Finding f = new Finding(source, current == null ? -1 : current, newValue);
        findings.add(f);
        return f;
    }

    private static @Nullable String lhsName(Expression lhs) {
        if (lhs instanceof J.Identifier) {
            return ((J.Identifier) lhs).getSimpleName();
        }
        if (lhs instanceof J.FieldAccess) {
            return ((J.FieldAccess) lhs).getSimpleName();
        }
        return null;
    }

    private static boolean isClosure(J.MethodInvocation m) {
        if (m.getArguments().isEmpty()) {
            return false;
        }
        Expression last = m.getArguments().get(m.getArguments().size() - 1);
        return last instanceof J.Lambda;
    }

    /**
     * Returns true if a literal int/string represents the same logical SDK value as {@code newValue}.
     * Useful for the "already at target" no-op check from outside this visitor (e.g. by the
     * caller deciding whether to also run secondary visitors).
     */
    public static boolean isAlreadyAtTarget(Expression expr, int newValue) {
        if (expr instanceof J.Literal) {
            J.Literal lit = (J.Literal) expr;
            if (lit.getType() == JavaType.Primitive.Int && lit.getValue() instanceof Integer) {
                return (Integer) lit.getValue() == newValue;
            }
            if (lit.getType() == JavaType.Primitive.String && lit.getValue() instanceof String) {
                String s = (String) lit.getValue();
                try {
                    int n = Integer.parseInt(s.startsWith("android-") ? s.substring("android-".length()) : s);
                    return n == newValue;
                } catch (NumberFormatException ignored) {
                    return false;
                }
            }
        }
        return false;
    }
}
