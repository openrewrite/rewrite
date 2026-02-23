/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.java.search;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.table.SymbolsTable;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.util.stream.Collectors;

/**
 * Lists all symbols (classes, interfaces, enums, records, methods, constructors, fields)
 * declared in the codebase and emits them into a {@link SymbolsTable} DataTable.
 */
@Value
@EqualsAndHashCode(callSuper = false)
public class FindSymbols extends Recipe {
    transient SymbolsTable symbolsTable = new SymbolsTable(this);

    @Option(displayName = "Source path",
            description = "Optional source path to limit the search to a single file.",
            example = "src/main/java/com/example/MyClass.java",
            required = false)
    @Nullable
    String sourcePath;

    String displayName = "Find symbols";

    String description = "Lists all symbols (classes, methods, fields, etc.) declared in the codebase. " +
               "Results are emitted into a data table with symbol kind, name, parent type, signature, and visibility.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {

            private String currentSourcePath() {
                return getCursor().firstEnclosingOrThrow(SourceFile.class)
                        .getSourcePath().toString();
            }

            private boolean shouldProcess() {
                if (sourcePath == null || sourcePath.isEmpty()) {
                    return true;
                }
                return currentSourcePath().equals(sourcePath);
            }

            @Nullable
            private String parentFqn() {
                J.ClassDeclaration enclosing = getCursor().firstEnclosing(J.ClassDeclaration.class);
                if (enclosing != null && enclosing.getType() != null) {
                    return enclosing.getType().getFullyQualifiedName();
                }
                return null;
            }

            private String classVisibility(J.ClassDeclaration cd) {
                if (cd.hasModifier(J.Modifier.Type.Public)) return "PUBLIC";
                if (cd.hasModifier(J.Modifier.Type.Protected)) return "PROTECTED";
                if (cd.hasModifier(J.Modifier.Type.Private)) return "PRIVATE";
                return "PACKAGE_PRIVATE";
            }

            private String methodVisibility(J.MethodDeclaration md) {
                if (md.hasModifier(J.Modifier.Type.Public)) return "PUBLIC";
                if (md.hasModifier(J.Modifier.Type.Protected)) return "PROTECTED";
                if (md.hasModifier(J.Modifier.Type.Private)) return "PRIVATE";
                return "PACKAGE_PRIVATE";
            }

            private String fieldVisibility(J.VariableDeclarations vd) {
                if (vd.hasModifier(J.Modifier.Type.Public)) return "PUBLIC";
                if (vd.hasModifier(J.Modifier.Type.Protected)) return "PROTECTED";
                if (vd.hasModifier(J.Modifier.Type.Private)) return "PRIVATE";
                return "PACKAGE_PRIVATE";
            }

            private String classKind(J.ClassDeclaration.Kind.Type kindType) {
                switch (kindType) {
                    case Class: return "CLASS";
                    case Interface: return "INTERFACE";
                    case Enum: return "ENUM";
                    case Annotation: return "ANNOTATION";
                    case Record: return "RECORD";
                    default: return kindType.name().toUpperCase();
                }
            }

            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl,
                                                            ExecutionContext ctx) {
                J.ClassDeclaration cd = super.visitClassDeclaration(classDecl, ctx);
                if (!shouldProcess()) return cd;

                JavaType.FullyQualified type = cd.getType();
                String name = cd.getSimpleName();
                String kind = classKind(cd.getKind());

                symbolsTable.insertRow(ctx, new SymbolsTable.Row(
                        currentSourcePath(),
                        kind,
                        name,
                        parentFqn(),
                        type != null ? type.getFullyQualifiedName() : null,
                        classVisibility(cd)
                ));
                return cd;
            }

            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method,
                                                              ExecutionContext ctx) {
                J.MethodDeclaration md = super.visitMethodDeclaration(method, ctx);
                if (!shouldProcess()) return md;

                boolean isConstructor = md.isConstructor();
                String name = md.getSimpleName();
                String kind = isConstructor ? "CONSTRUCTOR" : "METHOD";

                // Build signature: returnType name(paramType1, paramType2, ...)
                String signature = null;
                JavaType.Method methodType = md.getMethodType();
                if (methodType != null) {
                    String params = methodType.getParameterTypes().stream()
                            .map(TypeUtils::asFullyQualified)
                            .map(fq -> fq != null ? fq.getClassName() : "?")
                            .collect(Collectors.joining(", "));
                    JavaType returnJavaType = methodType.getReturnType();
                    JavaType.FullyQualified returnFq = TypeUtils.asFullyQualified(returnJavaType);
                    String returnType = returnFq != null
                            ? returnFq.getClassName()
                            : returnJavaType.toString();
                    signature = returnType + " " + name + "(" + params + ")";
                }

                symbolsTable.insertRow(ctx, new SymbolsTable.Row(
                        currentSourcePath(),
                        kind,
                        name,
                        parentFqn(),
                        signature,
                        methodVisibility(md)
                ));
                return md;
            }

            @Override
            public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable,
                                                                     ExecutionContext ctx) {
                J.VariableDeclarations vd = super.visitVariableDeclarations(multiVariable, ctx);
                if (!shouldProcess()) return vd;

                // Only emit fields (class-level variables), not local variables
                Object parentValue = getCursor().getParentTreeCursor().getValue();
                if (!(parentValue instanceof J.Block)) {
                    return vd;
                }
                Object blockParent = getCursor().getParentTreeCursor()
                        .getParentTreeCursor().getValue();
                if (!(blockParent instanceof J.ClassDeclaration)) {
                    return vd;
                }

                String fieldType = vd.getTypeExpression() != null
                        ? vd.getTypeExpression().print(getCursor())
                        : null;

                for (J.VariableDeclarations.NamedVariable var : vd.getVariables()) {
                    symbolsTable.insertRow(ctx, new SymbolsTable.Row(
                            currentSourcePath(),
                            "FIELD",
                            var.getSimpleName(),
                            parentFqn(),
                            fieldType,
                            fieldVisibility(vd)
                    ));
                }
                return vd;
            }
        };
    }
}
