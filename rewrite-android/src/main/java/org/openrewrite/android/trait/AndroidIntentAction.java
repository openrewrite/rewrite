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
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.trait.SimpleTraitMatcher;
import org.openrewrite.trait.Trait;
import org.openrewrite.trait.VisitFunction2;

import java.util.List;

/**
 * Matches string-literal Intent action arguments — i.e., the literal passed to
 * a {@code new android.content.Intent(String)} constructor or
 * {@code intent.setAction(String)}. The trait sits on the {@link J.Literal}
 * itself, not on the surrounding call.
 * <p>
 * Cross-language: works for free against Kotlin (same {@code J.Literal} shape).
 */
@Incubating(since = "8.65.0")
@Value
public class AndroidIntentAction implements Trait<J.Literal> {
    Cursor cursor;

    /**
     * The action string value (e.g., {@code "android.intent.action.VIEW"}).
     */
    public String getAction() {
        Object v = getTree().getValue();
        return v == null ? "" : v.toString();
    }

    public static class Matcher extends SimpleTraitMatcher<AndroidIntentAction> {
        private static final String INTENT_FQN = "android.content.Intent";
        // Accept any single-String arg (java.lang.String, kotlin.String). The receiver
        // type carries the intent unambiguously across Java and Kotlin call sites.
        private static final MethodMatcher SET_ACTION =
                new MethodMatcher("android.content.Intent setAction(..)", true);

        @Override
        public <P> TreeVisitor<? extends Tree, P> asVisitor(VisitFunction2<AndroidIntentAction, P> visitor) {
            return new JavaVisitor<P>() {
                @Override
                public J visitLiteral(J.Literal literal, P p) {
                    AndroidIntentAction action = test(getCursor());
                    return action != null ?
                            (J) visitor.visit(action, p) :
                            super.visitLiteral(literal, p);
                }
            };
        }

        @Override
        protected @Nullable AndroidIntentAction test(Cursor cursor) {
            Object value = cursor.getValue();
            if (!(value instanceof J.Literal)) {
                return null;
            }
            J.Literal literal = (J.Literal) value;
            if (!(literal.getValue() instanceof String)) {
                return null;
            }
            Object parent = cursor.getParentTreeCursor().getValue();
            if (parent instanceof J.NewClass) {
                J.NewClass nc = (J.NewClass) parent;
                if (isIntentConstructor(nc) && containsAsArg(nc.getArguments(), literal)) {
                    return new AndroidIntentAction(cursor);
                }
            } else if (parent instanceof J.MethodInvocation) {
                J.MethodInvocation mi = (J.MethodInvocation) parent;
                if (SET_ACTION.matches(mi) && containsAsArg(mi.getArguments(), literal)) {
                    return new AndroidIntentAction(cursor);
                }
            }
            return null;
        }

        private static boolean isIntentConstructor(J.NewClass nc) {
            if (nc.getClazz() == null) {
                return false;
            }
            return TypeUtils.isOfClassType(nc.getClazz().getType(), INTENT_FQN);
        }

        private static boolean containsAsArg(@Nullable List<Expression> args, J.Literal literal) {
            if (args == null) {
                return false;
            }
            for (Expression arg : args) {
                if (arg == literal) {
                    return true;
                }
            }
            return false;
        }
    }
}
