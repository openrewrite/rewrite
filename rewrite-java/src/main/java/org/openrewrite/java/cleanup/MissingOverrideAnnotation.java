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
package org.openrewrite.java.cleanup;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.AnnotationMatcher;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeUtils;

import java.time.Duration;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;

@Value
@EqualsAndHashCode(callSuper = true)
public class MissingOverrideAnnotation extends Recipe {
    @Option(displayName = "Ignore methods in anonymous classes",
            description = "When enabled, ignore missing annotations on methods which override methods when the class definition is within an anonymous class.",
            required = false)
    @Nullable
    Boolean ignoreAnonymousClassMethods;

    @Override
    public String getDisplayName() {
        return "Add missing `@Override` to overriding and implementing methods";
    }

    @Override
    public String getDescription() {
        return "Adds `@Override` to methods overriding superclass methods or implementing interface methods. " +
                "Annotating methods improves readability by showing the author's intent to override. " +
                "Additionally, when annotated, the compiler will emit an error when a signature of the overridden method does not match the superclass method.";
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("RSPEC-1161");
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(5);
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MissingOverrideAnnotationVisitor();
    }

    private class MissingOverrideAnnotationVisitor extends JavaIsoVisitor<ExecutionContext> {
        private final AnnotationMatcher OVERRIDE_ANNOTATION = new AnnotationMatcher("@java.lang.Override");

        private Cursor getCursorToParentScope(Cursor cursor) {
            return cursor.dropParentUntil(is -> is instanceof J.NewClass || is instanceof J.ClassDeclaration);
        }

        @Override
        public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
            if (!method.hasModifier(J.Modifier.Type.Static)
                    && method.getAllAnnotations().stream().noneMatch(OVERRIDE_ANNOTATION::matches)
                    && TypeUtils.isOverride(method.getMethodType())
                    && !(Boolean.TRUE.equals(ignoreAnonymousClassMethods)
                    && getCursorToParentScope(getCursor()).getValue() instanceof J.NewClass)) {

                method = method.withTemplate(
                        JavaTemplate.builder(this::getCursor, "@Override").build(),
                        method.getCoordinates().addAnnotation(Comparator.comparing(J.Annotation::getSimpleName)));
            }
            return super.visitMethodDeclaration(method, ctx);
        }
    }
}
