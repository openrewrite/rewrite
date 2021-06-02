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

import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.With;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.marker.JavaSearchResult;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.nio.file.Paths;

import static org.openrewrite.Tree.randomId;

/**
 * A recipe that will rename a package name in package statements, imports, and fully-qualified types (see: NOTE).
 * <p>
 * NOTE: Does not currently transform all possible type references, and accomplishing this would be non-trivial.
 * For example, a method invocation select might refer to field `A a` whose type has now changed to `A2`, and so the type
 * on the select should change as well. But how do we identify the set of all method selects which refer to `a`? Suppose
 * it were prefixed like `this.a`, or `MyClass.this.a`, or indirectly via a separate method call like `getA()` where `getA()`
 * is defined on the super class.
 */
@Value
@EqualsAndHashCode(callSuper = true)
public class ChangePackage extends Recipe {
    @Option(displayName = "Old package name",
            description = "The package name to replace.")
    String oldPackageName;

    @Option(displayName = "New package name",
            description = "New package name to replace the old package name with.")
    String newPackageName;

    @With
    @Option(displayName = "Recursive",
            description = "Recursively change subpackage names",
            required = false)
    @Nullable
    Boolean recursive;

    @Override
    public String getDisplayName() {
        return "Rename package name";
    }

    @Override
    public String getDescription() {
        return "A recipe that will rename a package name in package statements, imports, and fully-qualified types.";
    }

    @Override
    protected JavaVisitor<ExecutionContext> getSingleSourceApplicableTest() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext executionContext) {
                if (cu.getPackageDeclaration() != null) {
                    String original = cu.getPackageDeclaration().getExpression()
                            .printTrimmed().replaceAll("\\s", "");
                    if (original.startsWith(oldPackageName)) {
                        return cu.withMarkers(cu.getMarkers().addIfAbsent(new JavaSearchResult(randomId(), ChangePackage.this)));
                    }
                }
                doAfterVisit(new UsesType<>(oldPackageName + ".*"));
                return cu;
            }
        };
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new ChangePackageVisitor();
    }

    private class ChangePackageVisitor extends JavaIsoVisitor<ExecutionContext> {
        private static final String RENAME_TO_KEY = "renameTo";
        private static final String RENAME_FROM_KEY = "renameFrom";

        private final JavaType.Class newPackageType = JavaType.Class.build(newPackageName);
        private final JavaType.Class oldPackageType = JavaType.Class.build(oldPackageName);

        @Override
        public J.Annotation visitAnnotation(J.Annotation annotation, ExecutionContext ctx) {
            J.Annotation a = super.visitAnnotation(annotation, ctx);
            if (a.getAnnotationType() instanceof J.FieldAccess
                    && ((J.FieldAccess) a.getAnnotationType()).isFullyQualifiedClassReference(oldPackageName)) {

                a = a.withAnnotationType(transformName(a.getAnnotationType()));
            }
            return a;
        }

        @Override
        public J.ArrayType visitArrayType(J.ArrayType arrayType, ExecutionContext ctx) {
            J.ArrayType a = super.visitArrayType(arrayType, ctx);
            return a.withElementType(transformName(a.getElementType()));
        }

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
            J.ClassDeclaration c = super.visitClassDeclaration(classDecl, ctx);

            String changingTo = getCursor().getNearestMessage(RENAME_TO_KEY);
            if (changingTo != null && classDecl.getType() != null) {
                if (c.getExtends() != null) {
                    c = c.withExtends(transformName(c.getExtends()));
                }

                if (c.getImplements() != null) {
                    c = c.withImplements(ListUtils.map(c.getImplements(), this::transformName));
                }
            }
            return c;
        }

        @Override
        public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
            J.CompilationUnit c = super.visitCompilationUnit(cu, ctx);

            String changingTo = getCursor().getMessage(RENAME_TO_KEY);
            if (changingTo != null) {
                String path = c.getSourcePath().toString().replace('\\', '/');
                String changingFrom = getCursor().getMessage(RENAME_FROM_KEY);
                assert changingFrom != null;
                c = c.withSourcePath(Paths.get(path.replaceFirst(
                        changingFrom.replace('.', '/'),
                        changingTo.replace('.', '/')
                )));
            }
            return c;
        }

        @Override
        public J.Import visitImport(J.Import impoort, ExecutionContext executionContext) {
            return super.visitImport(impoort, executionContext);
        }

        @Override
        public J.FieldAccess visitFieldAccess(J.FieldAccess fieldAccess, ExecutionContext ctx) {
            J.FieldAccess f = super.visitFieldAccess(fieldAccess, ctx);

            if (f.isFullyQualifiedClassReference(oldPackageName)) {
                if (getCursor().getParent() != null
                        // Ensure the parent isn't a J.FieldAccess OR the parent doesn't match the target package name.
                        && (!(getCursor().getParent().getValue() instanceof J.FieldAccess)
                        || (!(((J.FieldAccess) getCursor().getParent().getValue())
                        .isFullyQualifiedClassReference(newPackageName))))) {

                    f = TypeTree.build(((JavaType.FullyQualified) newPackageType).getFullyQualifiedName())
                            .withPrefix(f.getPrefix());
                }
            }
            return f;
        }

        @Override
        public J.Identifier visitIdentifier(J.Identifier ident, ExecutionContext ctx) {
            // If the J.Identifier's type is equal to the type we're looking for,
            // and the classname of the type we're looking for is equal to the J.Identifiers string representation
            // Then transform it, otherwise leave it alone
            J.Identifier i = super.visitIdentifier(ident, ctx);

            if (TypeUtils.isOfClassType(i.getType(), oldPackageName)
                    && i.getSimpleName().equals(oldPackageType.getClassName())) {

                i = i.withName((newPackageType).getClassName())
                        .withType(newPackageType);
            }
            return i;
        }

        @Override
        public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
            J.MethodDeclaration m = super.visitMethodDeclaration(method, ctx);
            m = m.withReturnTypeExpression(transformName(m.getReturnTypeExpression()));
            return m.withThrows(ListUtils.map(m.getThrows(), this::transformName));
        }

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            J.MethodInvocation m = super.visitMethodInvocation(method, ctx);

            if (m.getSelect() instanceof NameTree
                    && m.getType() != null && m.getType().hasFlags(Flag.Static)) {
                m = m.withSelect(transformName(m.getSelect()));
            }

            if (m.getSelect() != null) {
                JavaType.FullyQualified selectType = TypeUtils.asFullyQualified(m.getSelect().getType());
                if (selectType != null && selectType.getFullyQualifiedName().equals(oldPackageName)) {
                    m = m.withSelect(m.getSelect()
                            .withType(newPackageType));
                }
            }

            if (m.getType() != null) {
                if (m.getType().getDeclaringType().getFullyQualifiedName().equals(oldPackageName)) {
                    m = m.withDeclaringType(newPackageType);
                }
            }
            return m;
        }

        @Override
        public J.MultiCatch visitMultiCatch(J.MultiCatch multiCatch, ExecutionContext ctx) {
            J.MultiCatch m = super.visitMultiCatch(multiCatch, ctx);
            return m.withAlternatives(ListUtils.map(m.getAlternatives(), this::transformName));
        }

        @Override
        public J.NewArray visitNewArray(J.NewArray newArray, ExecutionContext ctx) {
            J.NewArray n = super.visitNewArray(newArray, ctx);
            return n.withTypeExpression(transformName(n.getTypeExpression()));
        }

        @Override
        public J.NewClass visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
            J.NewClass n = super.visitNewClass(newClass, ctx);
            return n.withClazz(transformName(n.getClazz()));
        }

        @Override
        public J.Package visitPackage(J.Package pkg, ExecutionContext context) {
            String original = pkg.getExpression().printTrimmed().replaceAll("\\s", "");
            getCursor().putMessageOnFirstEnclosing(J.CompilationUnit.class, RENAME_FROM_KEY, original);

            if (original.equals(oldPackageName)) {
                getCursor().putMessageOnFirstEnclosing(J.CompilationUnit.class, RENAME_TO_KEY, newPackageName);
                pkg = pkg.withTemplate(template(newPackageName).build(), pkg.getCoordinates().replace());
            } else if ((recursive == null || recursive)
                    && original.startsWith(oldPackageName) && !original.startsWith(newPackageName)) {
                String changingTo = newPackageName + original.substring(oldPackageName.length());
                getCursor().putMessageOnFirstEnclosing(J.CompilationUnit.class, RENAME_TO_KEY, changingTo);
                pkg = pkg.withTemplate(template(changingTo).build(), pkg.getCoordinates().replace());
            }
            return pkg;
        }

        @Override
        public J.TypeCast visitTypeCast(J.TypeCast typeCast, ExecutionContext ctx) {
            J.TypeCast t = super.visitTypeCast(typeCast, ctx);
            return t.withClazz(t.getClazz().withTree(transformName(t.getClazz().getTree())));
        }

        @Override
        public <N extends NameTree> N visitTypeName(N name, ExecutionContext ctx) {
            N n = super.visitTypeName(name, ctx);

            JavaType.FullyQualified oldType = TypeUtils.asFullyQualified(name.getType());
            if (!(name instanceof TypeTree)
                    && oldType != null && oldType.getFullyQualifiedName().equals(oldPackageName)) {
                n = n.withType(newPackageType);
            }
            return n;
        }

        @Override
        public J.TypeParameter visitTypeParameter(J.TypeParameter typeParam, ExecutionContext ctx) {
            J.TypeParameter t = super.visitTypeParameter(typeParam, ctx);
            t = t.withBounds(t.getBounds() == null ? null : ListUtils.map(t.getBounds(), this::transformName));
            return t.withName(transformName(t.getName()));
        }

        @Override
        public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, ExecutionContext ctx) {
            J.VariableDeclarations.NamedVariable v = super.visitVariable(variable, ctx);

            JavaType.FullyQualified varType = TypeUtils.asFullyQualified(variable.getType());
            if (varType != null && varType.getFullyQualifiedName().equals(oldPackageName)) {
                v = v.withType(newPackageType)
                        .withName(v.getName().withType(newPackageType));
            }
            return v;
        }

        @Override
        public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
            J.VariableDeclarations m = super.visitVariableDeclarations(multiVariable, ctx);
            if (!(multiVariable.getTypeExpression() instanceof J.MultiCatch)) {
                m = m.withTypeExpression(transformName(m.getTypeExpression()));
            }
            return m;
        }

        @Override
        public J.Wildcard visitWildcard(J.Wildcard wildcard, ExecutionContext ctx) {
            J.Wildcard w = super.visitWildcard(wildcard, ctx);
            return w.withBoundedType(transformName(w.getBoundedType()));
        }

        @SuppressWarnings({"unchecked", "ConstantConditions"})
        private <T extends J> T transformName(@Nullable T nameField) {
            if (nameField instanceof NameTree) {
                JavaType.FullyQualified nameTree = TypeUtils.asFullyQualified(((NameTree) nameField).getType());
                String name = newPackageType.getClassName();

                if (nameTree != null && nameTree.getFullyQualifiedName().equals(oldPackageName)) {
                    return (T) J.Identifier.build(randomId(),
                            nameField.getPrefix(),
                            Markers.EMPTY,
                            name,
                            newPackageType
                    );
                }
            }
            return nameField;
        }
    }
}
