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

import java.util.List;

/**
 * Matches Android permission checks: instance-style
 * {@code context.checkSelfPermission(String)} (Context / Activity) and static-style
 * {@code ContextCompat.checkSelfPermission(context, String)} (AndroidX compat).
 * <p>
 * Cross-language: works for free against Kotlin (same {@code J.MethodInvocation}
 * shape).
 */
@Incubating(since = "8.65.0")
@Value
public class AndroidPermissionCheck implements Trait<J.MethodInvocation> {
    Cursor cursor;

    /**
     * The permission string expression (typically a {@code Manifest.permission.*}
     * constant or a string literal). For the static {@code ContextCompat} form, this
     * is the second argument; for the instance form, the first.
     */
    public Expression getPermissionExpression() {
        J.MethodInvocation mi = getTree();
        List<Expression> args = mi.getArguments();
        return args.size() == 2 ? args.get(1) : args.get(0);
    }

    public static class Matcher extends SimpleTraitMatcher<AndroidPermissionCheck> {
        private static final String CONTEXT_COMPAT_FQN = "androidx.core.content.ContextCompat";
        private static final String SUPPORT_CONTEXT_COMPAT_FQN = "android.support.v4.content.ContextCompat";

        @Override
        public <P> TreeVisitor<? extends Tree, P> asVisitor(VisitFunction2<AndroidPermissionCheck, P> visitor) {
            return new JavaVisitor<P>() {
                @Override
                public J visitMethodInvocation(J.MethodInvocation method, P p) {
                    AndroidPermissionCheck check = test(getCursor());
                    return check != null ?
                            (J) visitor.visit(check, p) :
                            super.visitMethodInvocation(method, p);
                }
            };
        }

        @Override
        protected @Nullable AndroidPermissionCheck test(Cursor cursor) {
            Object value = cursor.getValue();
            if (!(value instanceof J.MethodInvocation)) {
                return null;
            }
            J.MethodInvocation mi = (J.MethodInvocation) value;
            if (!"checkSelfPermission".equals(mi.getSimpleName())) {
                return null;
            }
            List<Expression> args = mi.getArguments();
            if (args.isEmpty() || args.get(0) instanceof J.Empty) {
                return null;
            }
            // Instance-style: receiver is a Context-ish expression, single permission arg.
            if (args.size() == 1) {
                return new AndroidPermissionCheck(cursor);
            }
            // Static-style: ContextCompat.checkSelfPermission(context, permission).
            if (args.size() == 2 && isContextCompatReceiver(mi.getSelect())) {
                return new AndroidPermissionCheck(cursor);
            }
            return null;
        }

        private static boolean isContextCompatReceiver(@Nullable Expression select) {
            if (select instanceof J.Identifier) {
                String name = ((J.Identifier) select).getSimpleName();
                return "ContextCompat".equals(name);
            }
            if (select instanceof J.FieldAccess) {
                J.FieldAccess fa = (J.FieldAccess) select;
                return "ContextCompat".equals(fa.getName().getSimpleName()) &&
                        (fa.isFullyQualifiedClassReference(CONTEXT_COMPAT_FQN) ||
                                fa.isFullyQualifiedClassReference(SUPPORT_CONTEXT_COMPAT_FQN));
            }
            return false;
        }
    }
}
