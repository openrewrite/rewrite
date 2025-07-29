/*
 * Copyright 2020 the original author or authors.
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

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.java.search.DeclaresMethod;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.Flag;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Javadoc;

@ToString
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Getter
@RequiredArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class UseStaticImport extends Recipe {

    /**
     * A method pattern that is used to find matching method invocations.
     * See {@link  MethodMatcher} for details on the expression's syntax.
     */
    @Option(displayName = "Method pattern",
            description = MethodMatcher.METHOD_PATTERN_DESCRIPTION,
            example = "java.util.Collections emptyList()")
    String methodPattern;

    @Override
    public String getDisplayName() {
        return "Use static import";
    }

    @Override
    public String getDescription() {
        return "Removes unnecessary receiver types from static method invocations. For example, `Collections.emptyList()` becomes `emptyList()`.";
    }

    @Override
    public Validated<Object> validate() {
        return super.validate().and(MethodMatcher.validate(methodPattern));
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        TreeVisitor<?, ExecutionContext> preconditions = new UsesMethod<>(methodPattern);
        if (!methodPattern.contains(" *(")) {
            int indexSpace = Math.max(methodPattern.indexOf(' '), methodPattern.indexOf('#'));
            int indexBrace = methodPattern.indexOf('(', indexSpace);
            String methodNameMatcher = methodPattern.substring(indexSpace, indexBrace);
            preconditions = Preconditions.and(preconditions,
                    Preconditions.not(new DeclaresMethod<>("*..* " + methodNameMatcher + "(..)")));
        }
        return Preconditions.check(preconditions, new UseStaticImportVisitor());
    }

    private class UseStaticImportVisitor extends JavaIsoVisitor<ExecutionContext> {
        final MethodMatcher methodMatcher = new MethodMatcher(methodPattern);

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
            if (methodMatcher.matches(m)) {
                if (m.getTypeParameters() != null && !m.getTypeParameters().isEmpty()) {
                    return m;
                }

                if (m.getMethodType() == null ||
                        !m.getMethodType().hasFlags(Flag.Static) ||
                        hasConflictingImport(m.getMethodType().getDeclaringType().getFullyQualifiedName(), m.getSimpleName(), getCursor()) ||
                        hasConflictingMethod(m.getSimpleName(), getCursor())) {
                    return m;
                }

                JavaType.FullyQualified receiverType = m.getMethodType().getDeclaringType();
                maybeRemoveImport(receiverType);
                maybeAddImport(receiverType.getFullyQualifiedName(), m.getSimpleName(), false);

                if (m.getSelect() != null) {
                    return m.withSelect(null).withName(m.getName().withPrefix(m.getSelect().getPrefix()));
                }
            }
            return m;
        }

        @Override
        protected JavadocVisitor<ExecutionContext> getJavadocVisitor() {
            return new JavadocVisitor<ExecutionContext>(this) {
                /**
                 * Do not visit the method referenced from the Javadoc.
                 * Otherwise, the Javadoc method reference would eventually be refactored to static import, which is not valid for Javadoc.
                 */
                @Override
                public Javadoc visitReference(Javadoc.Reference reference, ExecutionContext ctx) {
                    return reference;
                }
            };
        }
    }

    private static boolean hasConflictingImport(String typeName, String methodName, Cursor cursor) {
        J.CompilationUnit cu = cursor.firstEnclosing(J.CompilationUnit.class);
        if (cu != null) {
            for (J.Import imp : cu.getImports()) {
                if (imp.isStatic() &&
                        methodName.equals(imp.getQualid().getSimpleName()) &&
                        !typeName.equals(imp.getTypeName())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasConflictingMethod(String methodName, Cursor cursor) {
        Cursor cdCursor = cursor.dropParentUntil(it -> it instanceof J.ClassDeclaration || it == Cursor.ROOT_VALUE);
        Object maybeCd = cdCursor.getValue();
        if (!(maybeCd instanceof J.ClassDeclaration)) {
            return false;
        }
        return hasConflictingMethod(methodName, ((J.ClassDeclaration) maybeCd).getType()) ||
                hasConflictingMethod(methodName, cdCursor);
    }

    private static boolean hasConflictingMethod(String methodName, JavaType.@Nullable FullyQualified ct) {
        if (ct == null) {
            return false;
        }
        for (JavaType.Method method : ct.getMethods()) {
            if (method.getName().equals(methodName)) {
                return true;
            }
        }
        if (hasConflictingMethod(methodName, ct.getSupertype())) {
            return true;
        }
        for (JavaType.FullyQualified intf : ct.getInterfaces()) {
            if (hasConflictingMethod(methodName, intf)) {
                return true;
            }
        }
        return false;
    }
}
