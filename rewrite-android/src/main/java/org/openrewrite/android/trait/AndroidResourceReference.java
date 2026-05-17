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
 * Matches Android resource references of the shape {@code R.<type>.<name>}
 * (e.g., {@code R.string.app_name}, {@code R.id.button_submit},
 * {@code R.layout.activity_main}, or {@code android.R.color.white}).
 * <p>
 * Cross-language: works for free against Kotlin source because Kotlin lowers
 * {@code R.string.app_name} into a {@link J.FieldAccess} just like Java.
 */
@Incubating(since = "8.65.0")
@Value
public class AndroidResourceReference implements Trait<J.FieldAccess> {
    Cursor cursor;

    /**
     * The resource type segment ({@code "string"}, {@code "layout"}, {@code "id"},
     * {@code "drawable"}, {@code "color"}, ...).
     */
    public String getResourceType() {
        J.FieldAccess outer = getTree();
        J.FieldAccess inner = (J.FieldAccess) outer.getTarget();
        return inner.getName().getSimpleName();
    }

    /**
     * The resource name segment — the identifier after the type
     * (e.g., {@code "app_name"} in {@code R.string.app_name}).
     */
    public String getResourceName() {
        return getTree().getName().getSimpleName();
    }

    public static class Matcher extends SimpleTraitMatcher<AndroidResourceReference> {

        @Override
        public <P> TreeVisitor<? extends Tree, P> asVisitor(VisitFunction2<AndroidResourceReference, P> visitor) {
            return new JavaVisitor<P>() {
                @Override
                public J visitFieldAccess(J.FieldAccess fieldAccess, P p) {
                    AndroidResourceReference ref = test(getCursor());
                    return ref != null ?
                            (J) visitor.visit(ref, p) :
                            super.visitFieldAccess(fieldAccess, p);
                }
            };
        }

        @Override
        protected @Nullable AndroidResourceReference test(Cursor cursor) {
            Object value = cursor.getValue();
            if (!(value instanceof J.FieldAccess)) {
                return null;
            }
            J.FieldAccess outer = (J.FieldAccess) value;
            // `R.string.class` is a class literal, not a resource reference.
            if ("class".equals(outer.getName().getSimpleName())) {
                return null;
            }
            // Outer access must have its target be `R.<type>` — itself a FieldAccess
            // whose target is an `R` identifier (or `<pkg>.R` chain ending in `R`).
            Expression outerTarget = outer.getTarget();
            if (!(outerTarget instanceof J.FieldAccess)) {
                return null;
            }
            J.FieldAccess inner = (J.FieldAccess) outerTarget;
            if (!isRReceiver(inner.getTarget())) {
                return null;
            }
            // The parent must NOT itself be a FieldAccess that uses this node as its
            // target — otherwise we'd also match the `R.string` intermediate in
            // `R.string.app_name`, which would produce duplicate matches.
            Object parent = cursor.getParentTreeCursor().getValue();
            if (parent instanceof J.FieldAccess && ((J.FieldAccess) parent).getTarget() == outer) {
                return null;
            }
            return new AndroidResourceReference(cursor);
        }

        private static boolean isRReceiver(Expression expr) {
            if (expr instanceof J.Identifier) {
                return "R".equals(((J.Identifier) expr).getSimpleName());
            }
            if (expr instanceof J.FieldAccess) {
                // Allow `android.R` or `com.example.R` style receivers.
                return "R".equals(((J.FieldAccess) expr).getName().getSimpleName());
            }
            return false;
        }
    }
}
