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
            description = "The package name to replace.",
            example = "com.yourorg.foo")
    String oldPackageName;

    @Option(displayName = "New package name",
            description = "New package name to replace the old package name with.",
            example = "com.yourorg.bar")
    String newPackageName;

    @With
    @Option(displayName = "Recursive",
            description = "Recursively change subpackage names",
            required = false,
            example = "true")
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

        @Override
        public J.Assignment visitAssignment(J.Assignment assignment, ExecutionContext executionContext) {
            J.Assignment a = super.visitAssignment(assignment, executionContext);
            a = a.withAssignment(updateType(a.getAssignment()));
            a = a.withType(updateType(a.getType()));
            return a;
        }

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

                JavaType.FullyQualified type = c.getType();
                if(type != null) {
                    c = c.withType(type.withFullyQualifiedName(changingTo + "." + newPackageType.getClassName()));
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
            } else if (f.getType() != null) {
                f = f.withType(updateType(f.getType()));
            }
            return f;
        }

        @Override
        public J.Identifier visitIdentifier(J.Identifier ident, ExecutionContext ctx) {
            // If the J.Identifier's type is equal to the type we're looking for,
            // and the classname of the type we're looking for is equal to the J.Identifiers string representation
            // Then transform it, otherwise leave it alone
            J.Identifier i = super.visitIdentifier(ident, ctx);
            return i.withType(updateType(i.getType()));
        }

        @Override
        public J.Lambda visitLambda(J.Lambda lambda, ExecutionContext executionContext) {
            J.Lambda l = super.visitLambda(lambda, executionContext);
            return l.withType(updateType(l.getType()));
        }

        @Override
        public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
            J.MethodDeclaration m = super.visitMethodDeclaration(method, ctx);
            m = m.withReturnTypeExpression(transformName(m.getReturnTypeExpression()));
            if (m.getReturnTypeExpression() != null) {
                m = m.withReturnTypeExpression(m.getReturnTypeExpression().withType(updateType(m.getReturnTypeExpression().getType())));
            }
            m = m.withType(updateType(m.getType()));
            return m.withThrows(m.getThrows() == null ? null : ListUtils.map(m.getThrows(), this::transformName));
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
                if (selectType != null && selectType.getPackageName().equals(oldPackageName)) {
                    m = m.withSelect(m.getSelect().withType(updateType(m.getSelect().getType())));
                }
            }

            if (m.getType() != null) {
                if (m.getType().getDeclaringType().getPackageName().equals(oldPackageName)) {
                    //noinspection ConstantConditions
                    m = m.withDeclaringType(TypeUtils.asFullyQualified(buildNewType(m.getType().getDeclaringType().getClassName())));
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
            n = n.withType(updateType(n.getType()));
            n = n.withTypeExpression(transformName(n.getTypeExpression()));
            return n;
        }

        @Override
        public J.ArrayAccess visitArrayAccess(J.ArrayAccess arrayAccess, ExecutionContext executionContext) {
            J.ArrayAccess aa = super.visitArrayAccess(arrayAccess, executionContext);
            aa = aa.withType(updateType(aa.getType()));
            return aa;
        }

        @Override
        public J.NewClass visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
            J.NewClass n = super.visitNewClass(newClass, ctx);
            n = n.withClazz(transformName(n.getClazz()));
            return n.withType(updateType(n.getType()));
        }

        @Override
        public J.Package visitPackage(J.Package pkg, ExecutionContext context) {
            String original = pkg.getExpression().printTrimmed().replaceAll("\\s", "");
            getCursor().putMessageOnFirstEnclosing(J.CompilationUnit.class, RENAME_FROM_KEY, original);

            if (original.equals(oldPackageName)) {
                getCursor().putMessageOnFirstEnclosing(J.CompilationUnit.class, RENAME_TO_KEY, newPackageName);
                pkg = pkg.withTemplate(JavaTemplate.builder(this::getCursor, newPackageName).build(), pkg.getCoordinates().replace());
            } else if ((recursive == null || recursive)
                    && original.startsWith(oldPackageName) && !original.startsWith(newPackageName)) {
                String changingTo = newPackageName + original.substring(oldPackageName.length());
                getCursor().putMessageOnFirstEnclosing(J.CompilationUnit.class, RENAME_TO_KEY, changingTo);
                pkg = pkg.withTemplate(JavaTemplate.builder(this::getCursor, changingTo).build(), pkg.getCoordinates().replace());
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
            if (varType != null && varType.getPackageName().equals(oldPackageName)) {
                v = v.withType(updateType(v.getType()))
                        .withName(v.getName().withType(updateType(v.getName().getType())));
            }
            return v;
        }

        @Override
        public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
            J.VariableDeclarations m = super.visitVariableDeclarations(multiVariable, ctx);
            if (!(multiVariable.getTypeExpression() instanceof J.MultiCatch)) {
                m = m.withTypeExpression(transformName(m.getTypeExpression()));
                m = m.withType(updateType(m.getType()));
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

        private Expression updateType(@Nullable Expression typeTree) {
            if (typeTree == null) {
                // updateType/updateSignature are always used to swap things in-place
                // The true nullability is that the return has the same nullability as the input
                // Because it's always an in-place operation it isn't problematic to tell a white lie about the nullability of the return value

                //noinspection ConstantConditions
                return null;
            }

            return typeTree.withType(updateType(typeTree.getType()));
        }

        private JavaType updateType(@Nullable JavaType type) {
            JavaType.FullyQualified fqt = TypeUtils.asFullyQualified(type);
            if (fqt != null && fqt.getPackageName().equals(oldPackageName)) {
                return buildNewType(fqt.getClassName());
            }
            JavaType.GenericTypeVariable gtv = TypeUtils.asGeneric(type);
            if (gtv != null && gtv.getBound() != null
                    && gtv.getBound().getPackageName().equals(oldPackageName)) {
                return gtv.withBound(TypeUtils.asFullyQualified(buildNewType(gtv.getBound().getClassName())));
            }
            JavaType.Method mt = TypeUtils.asMethod(type);
            if (mt != null) {
                return mt.withDeclaringType((JavaType.FullyQualified) updateType(mt.getDeclaringType()))
                        .withResolvedSignature(updateSignature(mt.getResolvedSignature()))
                        .withGenericSignature(updateSignature(mt.getGenericSignature()));
            }
            JavaType.Array at = TypeUtils.asArray(type);
            if (at != null && at.getElemType() != null) {
                fqt = TypeUtils.asFullyQualified(at.getElemType());
                if (fqt != null && fqt.getPackageName().equals(oldPackageName)) {
                    return new JavaType.Array(buildNewType(fqt.getClassName()));
                }
            }
            //noinspection ConstantConditions
            return type;
        }

        private JavaType.Method.Signature updateSignature(@Nullable JavaType.Method.Signature signature) {
            if (signature == null) {
                //noinspection ConstantConditions
                return signature;
            }

            return signature.withReturnType(updateType(signature.getReturnType()))
                    .withParamTypes(ListUtils.map(signature.getParamTypes(), this::updateType));
        }

        private JavaType buildNewType(String className) {
            //noinspection ConstantConditions
            return TypeUtils.asFullyQualified(JavaType.buildType(newPackageName + "." + className));
        }

    }
}
