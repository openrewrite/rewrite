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
package org.openrewrite.android.trait;

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.Incubating;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.trait.SimpleTraitMatcher;
import org.openrewrite.trait.Trait;
import org.openrewrite.trait.VisitFunction2;

/**
 * Matches calls to {@code findViewById(int)} on any receiver (Activity, View,
 * Fragment-via-{@code requireView()}, generated ViewBinding, etc.). The receiver
 * type is intentionally not constrained — the method name and arity carry the
 * intent unambiguously across the Android API surface.
 * <p>
 * Cross-language: works for free against Kotlin (same {@code J.MethodInvocation}
 * shape).
 */
@Incubating(since = "8.65.0")
@Value
public class AndroidFindViewById implements Trait<J.MethodInvocation> {
    Cursor cursor;

    /**
     * The single argument expression (typically a {@code R.id.<name>} reference,
     * but could be any int expression).
     */
    public Expression getResourceIdExpression() {
        return getTree().getArguments().get(0);
    }

    public static class Matcher extends SimpleTraitMatcher<AndroidFindViewById> {

        @Override
        public <P> TreeVisitor<? extends Tree, P> asVisitor(VisitFunction2<AndroidFindViewById, P> visitor) {
            return new JavaVisitor<P>() {
                @Override
                public J visitMethodInvocation(J.MethodInvocation method, P p) {
                    AndroidFindViewById trait = test(getCursor());
                    return trait != null ?
                            (J) visitor.visit(trait, p) :
                            super.visitMethodInvocation(method, p);
                }
            };
        }

        @Override
        protected @Nullable AndroidFindViewById test(Cursor cursor) {
            Object value = cursor.getValue();
            if (!(value instanceof J.MethodInvocation)) {
                return null;
            }
            J.MethodInvocation mi = (J.MethodInvocation) value;
            if (!"findViewById".equals(mi.getSimpleName())) {
                return null;
            }
            if (mi.getArguments().size() != 1) {
                return null;
            }
            // Filter out cases where the single "argument" is actually an empty placeholder
            // (J.Empty stand-in for `findViewById()` with zero args, which J still represents
            // as a one-element list containing J.Empty).
            if (mi.getArguments().get(0) instanceof J.Empty) {
                return null;
            }
            return new AndroidFindViewById(cursor);
        }
    }
}
