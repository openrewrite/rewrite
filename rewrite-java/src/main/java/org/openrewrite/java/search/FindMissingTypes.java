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
package org.openrewrite.java.search;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.SearchResult;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.openrewrite.java.tree.TypeUtils.isWellFormedType;

public class FindMissingTypes extends Recipe {
    @Override
    public String getDisplayName() {
        return "Find missing type information on Java ASTs";
    }

    @Override
    public String getDescription() {
        return "This is a diagnostic recipe to highlight where ASTs are missing type attribution information.";
    }

    @Override
    public JavaVisitor<ExecutionContext> getVisitor() {
        return new FindMissingTypesVisitor();
    }

    public static List<MissingTypeResult> findMissingTypes(J j) {
        J j1 = new FindMissingTypesVisitor().visit(j, new InMemoryExecutionContext());
        List<MissingTypeResult> results = new ArrayList<>();
        if (j1 != j) {
            new JavaIsoVisitor<List<MissingTypeResult>>() {
                @Override
                public <M extends Marker> M visitMarker(Marker marker, List<MissingTypeResult> missingTypeResults) {
                    if (marker instanceof SearchResult) {
                        String message = ((SearchResult) marker).getDescription();
                        String path = getCursor().getPathAsStream().filter(J.class::isInstance).map(t -> t.getClass().getSimpleName()).collect(Collectors.joining("->"));
                        J j = getCursor().firstEnclosing(J.class);
                        String printedTree;
                        if (getCursor().firstEnclosing(JavaSourceFile.class) != null) {
                            printedTree = j != null ? j.printTrimmed(new InMemoryExecutionContext(), getCursor().getParentOrThrow()) : "";
                        } else {
                            printedTree = String.valueOf(j);
                        }
                        missingTypeResults.add(new MissingTypeResult(message, path, printedTree, j));
                    }
                    return super.visitMarker(marker, missingTypeResults);
                }
            }.visit(j1, results);
        }
        return results;
    }

    @Getter
    @AllArgsConstructor
    public static class MissingTypeResult {
        String message;
        String path;
        String printedTree;
        J j;
    }

    static class FindMissingTypesVisitor extends JavaIsoVisitor<ExecutionContext> {

        private final Set<JavaType> seenTypes = new HashSet<>();

        @Override
        public J.Identifier visitIdentifier(J.Identifier identifier, ExecutionContext ctx) {
            // The non-nullability of J.Identifier.getType() in our AST is a white lie
            // J.Identifier.getType() is allowed to be null in places where the containing AST element fully specifies the type
            if (!isWellFormedType(identifier.getType(), seenTypes) && !isAllowedToHaveNullType(identifier)) {
                identifier = SearchResult.found(identifier, "Identifier type is missing or malformed");
            }
            return identifier;
        }

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);
            // If one of the method's arguments or type parameters is missing type, then the invocation very likely will too
            // Avoid over-reporting the same problem by checking the invocation only when its elements are well-formed
            if (mi == method) {
                JavaType.Method type = mi.getMethodType();
                if (!isWellFormedType(type, seenTypes)) {
                    mi = SearchResult.found(mi, "MethodInvocation type is missing or malformed");
                } else if (!type.getName().equals(mi.getSimpleName()) && !type.isConstructor()) {
                    mi = SearchResult.found(mi, "type information has a different method name '" + type.getName() + "'");
                }
            }
            return mi;
        }

        @Override
        public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
            J.MethodDeclaration md = super.visitMethodDeclaration(method, ctx);
            JavaType.Method type = md.getMethodType();
            if (!isWellFormedType(type, seenTypes)) {
                md = SearchResult.found(md, "MethodDeclaration type is missing or malformed");
            } else if (!md.getSimpleName().equals(type.getName()) && !type.isConstructor()) {
                md = SearchResult.found(md, "type information has a different method name '" + type.getName() + "'");
            }
            return md;
        }

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
            J.ClassDeclaration cd = super.visitClassDeclaration(classDecl, ctx);
            JavaType.FullyQualified t = cd.getType();
            if (!isWellFormedType(t, seenTypes)) {
                return SearchResult.found(cd, "ClassDeclaration type is missing or malformed");
            }
            if (!cd.getKind().name().equals(t.getKind().name())) {
                cd = SearchResult.found(cd,
                        " J.ClassDeclaration kind " + cd.getKind() + " does not match the kind in its type information " + t.getKind());
            }
            J.CompilationUnit jc = getCursor().firstEnclosing(J.CompilationUnit.class);
            if (jc != null) {
                J.Package pkg = jc.getPackageDeclaration();
                if (pkg != null && t.getPackageName().equals(pkg.printTrimmed(getCursor()))) {
                    cd = SearchResult.found(cd,
                            " J.ClassDeclaration package " + pkg + " does not match the package in its type information " + pkg.printTrimmed(getCursor()));
                }
            }
            return cd;
        }

        @Override
        public J.NewClass visitNewClass(J.NewClass newClass, ExecutionContext executionContext) {
            J.NewClass n = super.visitNewClass(newClass, executionContext);
            if (n == newClass && !isWellFormedType(n.getType(), seenTypes)) {
                n = SearchResult.found(n, "NewClass type is missing or malformed");
            }
            return n;
        }

        private boolean isAllowedToHaveNullType(J.Identifier ident) {
            return inPackageDeclaration() || inImport() || isClassName()
                    || isMethodName() || isMethodInvocationName() || isFieldAccess(ident) || isBeingDeclared(ident) || isParameterizedType(ident)
                    || isNewClass(ident) || isTypeParameter() || isMemberReference() || isCaseLabel() || isLabel() || isAnnotationField(ident);
        }

        private boolean inPackageDeclaration() {
            return getCursor().firstEnclosing(J.Package.class) != null;
        }

        private boolean inImport() {
            return getCursor().firstEnclosing(J.Import.class) != null;
        }

        private boolean isClassName() {
            Cursor parent = getCursor().getParent();
            return parent != null && parent.getValue() instanceof J.ClassDeclaration;
        }

        private boolean isMethodName() {
            Cursor parent = getCursor().getParent();
            return parent != null && parent.getValue() instanceof J.MethodDeclaration;
        }

        private boolean isMethodInvocationName() {
            Cursor parent = getCursor().getParent();
            return parent != null && parent.getValue() instanceof J.MethodInvocation;
        }

        private boolean isFieldAccess(J.Identifier ident) {
            J.FieldAccess parent = getCursor().firstEnclosing(J.FieldAccess.class);
            return parent != null
                    && (parent.getName().equals(ident) || parent.getTarget().equals(ident));
        }

        private boolean isBeingDeclared(J.Identifier ident) {
            J.VariableDeclarations.NamedVariable parent = getCursor().firstEnclosing(J.VariableDeclarations.NamedVariable.class);
            return parent != null && parent.getName().equals(ident);
        }

        private boolean isParameterizedType(J.Identifier ident) {
            J.ParameterizedType parent = getCursor().firstEnclosing(J.ParameterizedType.class);
            return parent != null && ident.equals(parent.getClazz());
        }

        private boolean isNewClass(J.Identifier ident) {
            J.NewClass parent = getCursor().firstEnclosing(J.NewClass.class);
            return parent != null && ident.equals(parent.getClazz());
        }

        private boolean isTypeParameter() {
            return getCursor().getParent() != null
                    && getCursor().getParent().getValue() instanceof J.TypeParameter;
        }

        private boolean isMemberReference() {
            return getCursor().firstEnclosing(J.MemberReference.class) != null;
        }

        private boolean isCaseLabel() {
            return getCursor().getParentTreeCursor().getValue() instanceof J.Case;
        }

        private boolean isLabel() {
            return getCursor().firstEnclosing(J.Label.class) != null;
        }

        private boolean isAnnotationField(J.Identifier ident) {
            Cursor parent = getCursor().getParent();
            return parent != null && parent.getValue() instanceof J.Assignment
                    && (ident.equals(((J.Assignment) parent.getValue()).getVariable()) && getCursor().firstEnclosing(J.Annotation.class) != null);
        }

    }
}
