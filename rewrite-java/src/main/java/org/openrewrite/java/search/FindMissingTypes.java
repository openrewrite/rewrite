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
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavadocVisitor;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.SearchResult;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.joining;
import static org.openrewrite.java.tree.TypeUtils.isWellFormedType;

@Value
@EqualsAndHashCode(callSuper = false)
public class FindMissingTypes extends Recipe {

    @Option(displayName = "Check documentation",
            description = "When set to `true` any references in documentation (i.e. Javadoc for Java) will also be checked. Default is `false`.",
            required = false
    )
    boolean checkDocumentation;

    @Override
    public String getDisplayName() {
        return "Find missing type information on Java LSTs";
    }

    @Override
    public String getDescription() {
        return "This is a diagnostic recipe to highlight where LSTs are missing type attribution information.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new FindMissingTypesVisitor(checkDocumentation);
    }

    public static List<MissingTypeResult> findMissingTypes(J j, boolean checkDocumentation) {
        J j1 = new FindMissingTypesVisitor(checkDocumentation).visit(j, new InMemoryExecutionContext());
        List<MissingTypeResult> results = new ArrayList<>();
        if (j1 != j) {
            new JavaIsoVisitor<List<MissingTypeResult>>() {
                @Override
                public <M extends Marker> M visitMarker(Marker marker, List<MissingTypeResult> missingTypeResults) {
                    if (marker instanceof SearchResult) {
                        String message = ((SearchResult) marker).getDescription();
                        String path = getCursor()
                                .getPathAsStream(j -> j instanceof J || j instanceof Javadoc)
                                .map(t -> t.getClass().getSimpleName())
                                .collect(joining("->"));
                        J j = getCursor().firstEnclosing(J.class);
                        if (j != null) {
                            String printedTree;
                            if (getCursor().firstEnclosing(JavaSourceFile.class) != null) {
                                printedTree = j.printTrimmed(new InMemoryExecutionContext(), getCursor().getParentOrThrow());
                            } else {
                                printedTree = String.valueOf(j);
                            }
                            missingTypeResults.add(new MissingTypeResult(message, path, printedTree, j));
                        }
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
        @Nullable
        String message;

        String path;
        String printedTree;
        J j;
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    static class FindMissingTypesVisitor extends JavaIsoVisitor<ExecutionContext> {

        boolean checkDocumentation;
        Set<JavaType> seenTypes = new HashSet<>();

        @Override
        public J.Identifier visitIdentifier(J.Identifier identifier, ExecutionContext ctx) {
            // The non-nullability of J.Identifier.getType() in our AST is a white lie
            // J.Identifier.getType() is allowed to be null in places where the containing AST element fully specifies the type
            if (!isWellFormedType(identifier.getType(), seenTypes) && !isAllowedToHaveNullType(identifier)) {
                identifier = SearchResult.found(identifier, "Identifier type is missing or malformed");
            }
            if (identifier.getFieldType() != null && !identifier.getSimpleName().equals(identifier.getFieldType().getName())) {
                identifier = SearchResult.found(identifier, "type information has a different variable name '" + identifier.getFieldType().getName() + "'");
            }
            return identifier;
        }

        @Override
        public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, ExecutionContext ctx) {
            J.VariableDeclarations.NamedVariable v = super.visitVariable(variable, ctx);
            if (v == variable) {
                JavaType.Variable variableType = v.getVariableType();
                if (!isWellFormedType(variableType, seenTypes) && !isAllowedToHaveUnknownType()) {
                    v = SearchResult.found(v, "Variable type is missing or malformed");
                } else if (variableType != null && !variableType.getName().equals(v.getSimpleName())) {
                    v = SearchResult.found(v, "type information has a different variable name '" + variableType.getName() + "'");
                }
            }
            return v;
        }

        private boolean isAllowedToHaveUnknownType() {
            Cursor parent = getCursor().getParent();
            while (parent != null && parent.getParent() != null && !(parent.getParentTreeCursor().getValue() instanceof J.ClassDeclaration)) {
                parent = parent.getParentTreeCursor();
            }
            // If the variable is declared in a class initializer, then it's allowed to have unknown type
            return parent != null && parent.getValue() instanceof J.Block;
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
                if (mi.getName().getType() != null && type != null && type != mi.getName().getType()) {
                    // The MethodDeclaration#name#type and the methodType field should be the same object.
                    // A different object in one implies a type has changed, either in the method signature or deeper in the type tree.
                    mi = SearchResult.found(mi, "MethodInvocation#name#type is not the same instance as the MethodType of MethodInvocation.");
                }
                if (type != null) {
                    int argCount = 0;
                    for (Expression argument : mi.getArguments()) {
                        if (!(argument instanceof J.Empty)) {
                            argCount++;
                        }
                    }
                    int minCount = type.hasFlags(Flag.Varargs) ? type.getParameterTypes().size() - 1 : type.getParameterTypes().size();
                    if (argCount < minCount) {
                        mi = SearchResult.found(mi, "argument count mismatch: " + argCount + " != " + type.getParameterTypes().size());
                    }
                }
            }
            return mi;
        }

        @Override
        public J.MemberReference visitMemberReference(J.MemberReference memberRef, ExecutionContext ctx) {
            J.MemberReference mr = super.visitMemberReference(memberRef, ctx);
            JavaType.Method type = mr.getMethodType();
            if (type != null) {
                if (!isWellFormedType(type, seenTypes)) {
                    mr = SearchResult.found(mr, "MemberReference type is missing or malformed");
                } else if (!type.getName().equals(mr.getReference().getSimpleName()) && !type.isConstructor()) {
                    mr = SearchResult.found(mr, "type information has a different method name '" + type.getName() + "'");
                }
            } else {
                JavaType.Variable variableType = mr.getVariableType();
                if (!isWellFormedType(variableType, seenTypes)) {
                    mr = SearchResult.found(mr, "MemberReference type is missing or malformed");
                } else if (!variableType.getName().equals(mr.getReference().getSimpleName())) {
                    mr = SearchResult.found(mr, "type information has a different variable name '" + variableType.getName() + "'");
                }
            }
            return mr;
        }

        @Override
        public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
            J.MethodDeclaration md = method;
            JavaType.Method type = md.getMethodType();
            if (!isWellFormedType(type, seenTypes)) {
                md = SearchResult.found(md, "MethodDeclaration type is missing or malformed");
            } else if (!md.getSimpleName().equals(type.getName()) && !type.isConstructor()) {
                md = SearchResult.found(md, "type information has a different method name '" + type.getName() + "'");
            }
            if (md.getName().getType() != null && type != null && type != md.getName().getType()) {
                // The MethodDeclaration#name#type and the methodType field should be the same object.
                // A different object in one implies a type has changed, either in the method signature or deeper in the type tree.
                md = SearchResult.found(md, "MethodDeclaration#name#type is not the same instance as the MethodType of MethodDeclaration.");
            }
            return super.visitMethodDeclaration(md, ctx);
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
        public J.NewClass visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
            J.NewClass n = super.visitNewClass(newClass, ctx);
            if (n == newClass && !isWellFormedType(n.getType(), seenTypes)) {
                n = SearchResult.found(n, "NewClass type is missing or malformed");
            }
            if (n.getClazz() instanceof J.Identifier && n.getClazz().getType() != null &&
                    !(n.getClazz().getType() instanceof JavaType.Class || n.getClazz().getType() instanceof JavaType.Unknown)) {
                n = SearchResult.found(n, "NewClass#clazz is J.Identifier and the type is is not JavaType$Class.");
            }
            return n;
        }

        @Override
        public J.ParameterizedType visitParameterizedType(J.ParameterizedType type, ExecutionContext ctx) {
            J.ParameterizedType p = super.visitParameterizedType(type, ctx);
            if (p.getClazz() instanceof J.Identifier && p.getClazz().getType() != null &&
                    !(p.getClazz().getType() instanceof JavaType.Class || p.getClazz().getType() instanceof JavaType.Unknown)) {
                p = SearchResult.found(p, "ParameterizedType#clazz is J.Identifier and the type is is not JavaType$Class.");
            }
            return p;
        }

        @Override
        protected JavadocVisitor<ExecutionContext> getJavadocVisitor() {
            return new JavadocVisitor<ExecutionContext>(this) {
                @Override
                public @Nullable Javadoc visit(@Nullable Tree tree, ExecutionContext ctx) {
                    return checkDocumentation ? super.visit(tree, ctx) : (Javadoc) tree;
                }
            };
        }

        private boolean isAllowedToHaveNullType(J.Identifier ident) {
            return inPackageDeclaration() || inImport() || isClassName() ||
                    isMethodName() || isMethodInvocationName() || isFieldAccess(ident) || isBeingDeclared(ident) || isParameterizedType(ident) ||
                    isNewClass(ident) || isTypeParameter() || isMemberReference(ident) || isCaseLabel() || isLabel() || isAnnotationField(ident) ||
                    isInJavaDoc(ident);
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
            Tree value = getCursor().getParentTreeCursor().getValue();
            return value instanceof J.FieldAccess &&
                    (ident == ((J.FieldAccess) value).getName() ||
                        ident == ((J.FieldAccess) value).getTarget() && !"class".equals(((J.FieldAccess) value).getSimpleName()));
        }

        private boolean isBeingDeclared(J.Identifier ident) {
            Tree value = getCursor().getParentTreeCursor().getValue();
            return value instanceof J.VariableDeclarations.NamedVariable && ident == ((J.VariableDeclarations.NamedVariable) value).getName();
        }

        private boolean isParameterizedType(J.Identifier ident) {
            Tree value = getCursor().getParentTreeCursor().getValue();
            return value instanceof J.ParameterizedType && ident == ((J.ParameterizedType) value).getClazz();
        }

        private boolean isNewClass(J.Identifier ident) {
            Tree value = getCursor().getParentTreeCursor().getValue();
            return value instanceof J.NewClass && ident == ((J.NewClass) value).getClazz();
        }

        private boolean isTypeParameter() {
            return getCursor().getParent() != null &&
                    getCursor().getParent().getValue() instanceof J.TypeParameter;
        }

        private boolean isMemberReference(J.Identifier ident) {
            Tree value = getCursor().getParentTreeCursor().getValue();
            return value instanceof J.MemberReference &&
                   ident == ((J.MemberReference) value).getReference();
        }

        private boolean isInJavaDoc(J.Identifier ident) {
            Tree value = getCursor().getParentTreeCursor().getValue();
            return value instanceof Javadoc.Reference &&
                    ident == ((Javadoc.Reference) value).getTree();
        }

        private boolean isCaseLabel() {
            return getCursor().getParentTreeCursor().getValue() instanceof J.Case;
        }

        private boolean isLabel() {
            return getCursor().firstEnclosing(J.Label.class) != null;
        }

        private boolean isAnnotationField(J.Identifier ident) {
            Cursor parent = getCursor().getParent();
            return parent != null && parent.getValue() instanceof J.Assignment &&
                    (ident == ((J.Assignment) parent.getValue()).getVariable() && getCursor().firstEnclosing(J.Annotation.class) != null);
        }

    }
}
