/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.java.trait;

import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.AnnotationMatcher;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.trait.SimpleTraitMatcher;
import org.openrewrite.trait.Trait;
import org.openrewrite.trait.VisitFunction2;

import java.util.Optional;

@Value
public class Annotated implements Trait<J.Annotation> {
    Cursor cursor;

    /**
     * @param defaultAlias The name of the annotation attribute that is aliased to
     *                     "value", if any.
     * @return The attribute value.
     */
    public Optional<Literal> getDefaultAttribute(@Nullable String defaultAlias) {
        if (getTree().getArguments() == null) {
            return Optional.empty();
        }
        for (Expression argument : getTree().getArguments()) {
            if (!(argument instanceof J.Assignment)) {
                return new Literal.Matcher().get(argument, cursor);
            }
        }
        Optional<Literal> valueAttr = getAttribute("value");
        if (valueAttr.isPresent()) {
            return valueAttr;
        }
        return defaultAlias != null ?
                getAttribute(defaultAlias) :
                Optional.empty();
    }

    public Optional<Literal> getAttribute(String attribute) {
        if (getTree().getArguments() == null) {
            return Optional.empty();
        }
        for (Expression argument : getTree().getArguments()) {
            if (argument instanceof J.Assignment) {
                J.Assignment assignment = (J.Assignment) argument;
                if (assignment.getVariable() instanceof J.Identifier) {
                    J.Identifier identifier = (J.Identifier) assignment.getVariable();
                    if (identifier.getSimpleName().equals(attribute)) {
                        return new Literal.Matcher().get(
                                assignment.getAssignment(),
                                new Cursor(cursor, argument)
                        );
                    }
                }
            }
        }
        return Optional.empty();
    }

    @RequiredArgsConstructor
    public static class Matcher extends SimpleTraitMatcher<Annotated> {
        private final AnnotationMatcher matcher;

        public Matcher(String signature) {
            this.matcher = new AnnotationMatcher(signature);
        }

        public Matcher(Class<?> annotationType) {
            this.matcher = new AnnotationMatcher(annotationType);
        }

        @Override
        public <P> TreeVisitor<? extends Tree, P> asVisitor(VisitFunction2<Annotated, P> visitor) {
            return new JavaVisitor<P>() {
                @Override
                public J visitAnnotation(J.Annotation annotation, P p) {
                    Annotated annotated = test(getCursor());
                    return annotated != null ?
                            (J) visitor.visit(annotated, p) :
                            super.visitAnnotation(annotation, p);
                }
            };
        }

        @Override
        protected @Nullable Annotated test(Cursor cursor) {
            Object value = cursor.getValue();
            if (value instanceof J.Annotation) {
                J.Annotation annotation = (J.Annotation) value;
                if (matcher.matches(annotation)) {
                    return new Annotated(cursor);
                }
            }
            return null;
        }
    }
}
