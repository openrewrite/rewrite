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
import org.apache.commons.lang3.StringUtils;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.search.UsesField;
import org.openrewrite.java.tree.*;

@EqualsAndHashCode(callSuper = true)
@Value
public class ChangeStaticFieldToMethod extends Recipe {

    @Option(displayName = "Old class name",
            description = "The fully qualified name of the class containing the field to replace.",
            example = "java.util.Collections")
    String oldClassName;

    @Option(displayName = "Old field name",
            description = "The simple name of the static field to replace.",
            example = "EMPTY_LIST")
    String oldFieldName;

    @Option(displayName = "New class name",
            description = "The fully qualified name of the class containing the method to use. Leave empty to keep the same class.",
            example = "java.util.List",
            required = false)
    @Nullable
    String newClassName;

    @Option(displayName = "New field target",
            description = "An optional method target that can be used to specify a static field within the new class.",
            example = "OK_RESPONSE",
            required = false)
    @Nullable
    String newTarget;

    @Option(displayName = "New method name",
            description = "The simple name of the method to use. The method must be static and have no arguments.",
            example = "of")
    String newMethodName;

    @Override
    public String getDisplayName() {
        return "Change static field access to static method access";
    }

    @Override
    public String getDescription() {
        return "Migrate accesses to a static field to invocations of a static method.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        return new UsesField<>(oldClassName, oldFieldName);
    }

    @Override
    public JavaVisitor<ExecutionContext> getVisitor() {
        return new JavaVisitor<ExecutionContext>() {

            @Override
            public J visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext context) {
                if(TypeUtils.isOfClassType(classDecl.getType(), oldClassName)) {
                    // Don't modify the class that declares the static field being replaced
                    return classDecl;
                }
                return super.visitClassDeclaration(classDecl, context);
            }

            @Override
            public J visitFieldAccess(J.FieldAccess fieldAccess, ExecutionContext ctx) {
                if (getCursor().firstEnclosing(J.Import.class) == null &&
                        TypeUtils.isOfClassType(fieldAccess.getTarget().getType(), oldClassName) &&
                        fieldAccess.getSimpleName().equals(oldFieldName)) {
                    return useNewMethod(fieldAccess);
                }
                return super.visitFieldAccess(fieldAccess, ctx);
            }

            @Override
            public J visitIdentifier(J.Identifier ident, ExecutionContext executionContext) {
                JavaType.Variable varType = ident.getFieldType();
                if (varType != null &&
                        TypeUtils.isOfClassType(varType.getOwner(), oldClassName) &&
                        varType.getName().equals(oldFieldName)) {
                    return useNewMethod(ident);
                }
                return ident;
            }

            @NotNull
            private J useNewMethod(TypeTree tree) {
                String newClass = newClassName == null ? oldClassName : newClassName;

                maybeRemoveImport(oldClassName);
                maybeAddImport(newClass);

                Cursor statementCursor = getCursor().dropParentUntil(Statement.class::isInstance);
                Statement statement = statementCursor.getValue();
                JavaTemplate template = makeNewMethod(newClass, statementCursor);
                J.Block block = statement.withTemplate(template, statement.getCoordinates().replace());
                J.MethodInvocation method = block.getStatements().get(0).withPrefix(tree.getPrefix());

                if (method.getMethodType() == null) {
                    throw new IllegalArgumentException("Error while changing a static field to a method. The generated template using a the new class ["
                            + newClass + "] and the method [" + newMethodName + "] resulted in a null method type.");
                }
                return tree.getType() == null ? method :
                        method.withMethodType(method.getMethodType().withReturnType(tree.getType()));
            }

            @NotNull
            private JavaTemplate makeNewMethod(String newClass, Cursor statementCursor) {

                String packageName = StringUtils.substringBeforeLast(newClass, ".");
                String simpleClassName = StringUtils.substringAfterLast(newClass, ".");
                String methodInvocationTemplate = "{" + simpleClassName + (newTarget != null ? "." + newTarget + "." : ".") + newMethodName + "();}";

                @Language("java") String methodStub;
                if (newTarget == null) {
                    methodStub = "package " + packageName + ";" +
                            " public class " + simpleClassName + " {" +
                            " public static void " + newMethodName + "() { return null; }" +
                            " }";
                } else {
                    methodStub = "package " + packageName + ";" +
                            " public class Target {" +
                            " public static void " + newMethodName + "() { return null; }" +
                            " }" +
                            " public class " + simpleClassName + " {public static Target " + newTarget + ";}";
                }
                return JavaTemplate
                        .builder(() -> statementCursor, methodInvocationTemplate)
                        .javaParser(() -> JavaParser.fromJavaVersion()
                                .dependsOn(methodStub)
                                .build())
                        .imports(newClass)
                        .build();
            }
        };
    }
}
