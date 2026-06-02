/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.scala;

import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.cleanup.UnnecessaryParenthesesVisitor;
import org.openrewrite.java.service.ImportService;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.scala.tree.S;

/**
 * Base class for Scala recipes defined as before/after template pairs.
 * <p>
 * Subclasses provide one or more "before" template patterns and a single "after" template.
 * The base class handles matching and replacement.
 * <p>
 * Template strings use the same {@code #{name:any(Type)}} placeholder syntax as
 * {@link ScalaTemplate}. Templates may use {@code #{any(Type)}} for matching but
 * the after template must be a constant (no parameter substitution).
 *
 * <pre>
 * public class ReplaceEqualsOne extends ScalaTemplateRecipe {
 *     &#064;Override public String getDisplayName() { return "Replace == 1"; }
 *     &#064;Override public String getDescription() { return "Replace x == 1 with x != 0."; }
 *     &#064;Override protected String[] getBeforeTemplates() {
 *         return new String[]{ "#{a:any(int)} == 1" };
 *     }
 *     &#064;Override protected String getAfterTemplate() { return "42"; }
 * }
 * </pre>
 */
public abstract class ScalaTemplateRecipe extends Recipe {

    /**
     * One or more before-template patterns. A match on any triggers replacement.
     */
    protected abstract String[] getBeforeTemplates();

    /**
     * The replacement template applied when a before-template matches.
     */
    protected abstract String getAfterTemplate();

    /**
     * Imports required by the templates. Override to add imports.
     */
    protected String[] getImports() {
        return new String[0];
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        String[] imports = getImports();

        ScalaTemplate.Builder afterBuilder = ScalaTemplate.builder(getAfterTemplate());
        for (String imp : imports) {
            afterBuilder.imports(imp);
        }
        ScalaTemplate after = afterBuilder.build();

        String[] beforeCodes = getBeforeTemplates();
        ScalaTemplate[] befores = new ScalaTemplate[beforeCodes.length];
        for (int i = 0; i < beforeCodes.length; i++) {
            ScalaTemplate.Builder beforeBuilder = ScalaTemplate.builder(beforeCodes[i]);
            for (String imp : imports) {
                beforeBuilder.imports(imp);
            }
            befores[i] = beforeBuilder.build();
        }

        return new ScalaVisitor<ExecutionContext>() {
            // Override visitCompilationUnit to not swallow template exceptions
            @Override
            public J visitCompilationUnit(S.CompilationUnit cu, ExecutionContext p) {
                S.CompilationUnit c = cu;
                c = c.withPrefix(visitSpace(c.getPrefix(), Space.Location.COMPILATION_UNIT_PREFIX, p));
                c = c.withMarkers(visitMarkers(c.getMarkers(), p));
                if (c.getPackageDeclaration() != null) {
                    c = c.withPackageDeclaration(visitAndCast(c.getPackageDeclaration(), p));
                }
                c = c.withImports(ListUtils.map(c.getImports(), i -> visitAndCast(i, p)));
                c = c.withStatements(ListUtils.map(c.getStatements(), s -> visitAndCast(s, p)));
                c = c.withEof(visitSpace(c.getEof(), Space.Location.COMPILATION_UNIT_EOF, p));
                return c;
            }

            @Override
            public J visitBinary(J.Binary elem, ExecutionContext ctx) {
                J.Binary b = (J.Binary) super.visitBinary(elem, ctx);
                return tryMatch(b, ctx);
            }

            @Override
            public J visitMethodInvocation(J.MethodInvocation elem, ExecutionContext ctx) {
                J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(elem, ctx);
                return tryMatch(m, ctx);
            }

            @Override
            public J visitUnary(J.Unary elem, ExecutionContext ctx) {
                J.Unary u = (J.Unary) super.visitUnary(elem, ctx);
                return tryMatch(u, ctx);
            }

            @Override
            public J visitTernary(J.Ternary elem, ExecutionContext ctx) {
                J.Ternary t = (J.Ternary) super.visitTernary(elem, ctx);
                return tryMatch(t, ctx);
            }

            @Override
            public J visitNewClass(J.NewClass elem, ExecutionContext ctx) {
                J.NewClass n = (J.NewClass) super.visitNewClass(elem, ctx);
                return tryMatch(n, ctx);
            }

            private <E extends Expression> J tryMatch(E elem, ExecutionContext ctx) {
                for (ScalaTemplate before : befores) {
                    if (before.matches(getCursor())) {
                        return embed(
                                after.apply(getCursor(), elem.getCoordinates().replace()),
                                getCursor(), ctx
                        );
                    }
                }
                return elem;
            }

            private J embed(J j, Cursor cursor, ExecutionContext ctx) {
                TreeVisitor<?, ExecutionContext> parensVisitor = new UnnecessaryParenthesesVisitor<>();
                if (!getAfterVisit().contains(parensVisitor)) {
                    doAfterVisit(parensVisitor);
                }
                doAfterVisit(service(ImportService.class).shortenFullyQualifiedTypeReferencesIn(j));
                return j;
            }
        };
    }
}
