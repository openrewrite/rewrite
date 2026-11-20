/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.java;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.tree.*;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

@Value
@EqualsAndHashCode(callSuper = true)
public class RemoveAnnotationVisitor extends JavaIsoVisitor<ExecutionContext> {
    AnnotationMatcher annotationMatcher;

    private static final String REMOVED_ANNOTATIONS = "annotationRemoved";

    @Override
    public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
        J.ClassDeclaration c = super.visitClassDeclaration(classDecl, ctx);
        Set<J.Annotation> removed = getCursor().pollMessage(REMOVED_ANNOTATIONS);
        if (removed != null && !classDecl.getLeadingAnnotations().isEmpty()) {
            J.Annotation first = classDecl.getLeadingAnnotations().get(0);
            if (removed.contains(first)) {
                c = startWithPrefix(c, first.getPrefix());
            }
            // The manual prefix surgery above writes to whatever modifier or
            // kind happens to be first in the list — on languages like Kotlin,
            // the parser inserts synthetic modifiers that print to nothing,
            // so the cleanup lands on an invisible slot and stranded leading
            // whitespace remains. Run the language's BlankLines pass on the
            // touched declaration to absorb such cases without paying for
            // full auto-format (which would also touch braces, annotation
            // wrapping, etc. and change unrelated formatting).
            c = maybeRemoveBlankLines(classDecl, c, ctx);
        }
        return c;
    }

    @Override
    public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
        J.MethodDeclaration m = super.visitMethodDeclaration(method, ctx);
        Set<J.Annotation> removed = getCursor().pollMessage(REMOVED_ANNOTATIONS);
        if (removed != null && !method.getLeadingAnnotations().isEmpty()) {
            J.Annotation first = method.getLeadingAnnotations().get(0);
            if (removed.contains(first)) {
                m = startWithPrefix(m, first.getPrefix());
            }
            m = maybeRemoveBlankLines(method, m, ctx);
        }
        return m;
    }

    @Override
    public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
        J.VariableDeclarations v = super.visitVariableDeclarations(multiVariable, ctx);
        Set<J.Annotation> removed = getCursor().pollMessage(REMOVED_ANNOTATIONS);
        if (removed != null && !multiVariable.getLeadingAnnotations().isEmpty()) {
            J.Annotation first = multiVariable.getLeadingAnnotations().get(0);
            if (removed.contains(first)) {
                v = startWithPrefix(v, first.getPrefix());
            }
            v = maybeRemoveBlankLines(multiVariable, v, ctx);
        }
        return v;
    }

    @Override
    public  J.@Nullable Annotation visitAnnotation(J.Annotation annotation, ExecutionContext ctx) {
        if (annotationMatcher.matches(annotation)) {
            getCursor().getParentOrThrow()
                    .<Set<J.Annotation>>computeMessageIfAbsent(REMOVED_ANNOTATIONS, k -> Collections.newSetFromMap(new IdentityHashMap<>()))
                    .add(annotation);
            RemoveImport.namesReferencedBy(annotation).forEach(this::maybeRemoveImport);
            //noinspection ConstantConditions
            return null;
        }
        return super.visitAnnotation(annotation, ctx);
    }

    private static J.ClassDeclaration startWithPrefix(J.ClassDeclaration c, Space prefix) {
        if (!c.getLeadingAnnotations().isEmpty()) {
            return c.withLeadingAnnotations(withFirstPrefix(c.getLeadingAnnotations(), prefix));
        } else if (!c.getModifiers().isEmpty()) {
            return c.withModifiers(withFirstWhitespace(c.getModifiers(), prefix));
        }
        J.ClassDeclaration.Kind kind = c.getPadding().getKind();
        return c.getPadding().withKind(kind.withPrefix(kind.getPrefix().withWhitespace(prefix.getWhitespace())));
    }

    private static J.MethodDeclaration startWithPrefix(J.MethodDeclaration m, Space prefix) {
        if (!m.getLeadingAnnotations().isEmpty()) {
            return m.withLeadingAnnotations(withFirstPrefix(m.getLeadingAnnotations(), prefix));
        } else if (!m.getModifiers().isEmpty()) {
            return m.withModifiers(withFirstWhitespace(m.getModifiers(), prefix));
        }
        J.TypeParameters typeParameters = m.getPadding().getTypeParameters();
        if (typeParameters != null) {
            return m.getPadding().withTypeParameters(typeParameters.withPrefix(typeParameters.getPrefix().withWhitespace(prefix.getWhitespace())));
        } else if (m.getReturnTypeExpression() != null) {
            return m.withReturnTypeExpression(m.getReturnTypeExpression().withPrefix(m.getReturnTypeExpression().getPrefix().withWhitespace(prefix.getWhitespace())));
        }
        return m.withName(m.getName().withPrefix(m.getName().getPrefix().withWhitespace(prefix.getWhitespace())));
    }

    private static J.VariableDeclarations startWithPrefix(J.VariableDeclarations v, Space prefix) {
        if (!v.getLeadingAnnotations().isEmpty()) {
            return v.withLeadingAnnotations(withFirstPrefix(v.getLeadingAnnotations(), prefix));
        } else if (!v.getModifiers().isEmpty()) {
            return v.withModifiers(withFirstWhitespace(v.getModifiers(), prefix));
        } else if (v.getTypeExpression() != null) {
            return v.withTypeExpression(v.getTypeExpression().withPrefix(v.getTypeExpression().getPrefix().withWhitespace(prefix.getWhitespace())));
        }
        return v;
    }

    private static List<J.Annotation> withFirstPrefix(List<J.Annotation> leadingAnnotations, Space prefix) {
        return ListUtils.mapFirst(leadingAnnotations, a -> a.withPrefix(prefix));
    }

    private static List<J.Modifier> withFirstWhitespace(List<J.Modifier> modifiers, Space prefix) {
        return Space.formatFirstPrefix(modifiers, Space.firstPrefix(modifiers).withWhitespace(prefix.getWhitespace()));
    }
}
