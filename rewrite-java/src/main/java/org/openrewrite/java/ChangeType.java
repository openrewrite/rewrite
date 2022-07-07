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
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Stack;

@Value
@EqualsAndHashCode(callSuper = true)
public class ChangeType extends Recipe {

    @Option(displayName = "Old fully-qualified type name",
            description = "Fully-qualified class name of the original type.",
            example = "org.junit.Assume")
    String oldFullyQualifiedTypeName;

    @Option(displayName = "New fully-qualified type name",
            description = "Fully-qualified class name of the replacement type, or the name of a primitive such as \"int\". The `OuterClassName$NestedClassName` naming convention should be used for nested classes.",
            example = "org.junit.jupiter.api.Assumptions")
    String newFullyQualifiedTypeName;

    @Option(displayName = "Ignore type definition",
            description = "When set to `true` the definition of the old type will be left untouched. " +
                    "This is useful when you're replacing usage of a class but don't want to rename it.",
            example = "true",
            required = false)
    @Nullable
    Boolean ignoreDefinition;

    @Override
    public String getDisplayName() {
        return "Change type";
    }

    @Override
    public String getDescription() {
        return "Change a given type to another.";
    }

    @Override
    public boolean causesAnotherCycle() {
        return true;
    }

    @Override
    protected JavaVisitor<ExecutionContext> getSingleSourceApplicableTest() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public JavaSourceFile visitJavaSourceFile(JavaSourceFile cu, ExecutionContext executionContext) {
                JavaSourceFile sf = super.visitJavaSourceFile(cu, executionContext);
                if (!Boolean.TRUE.equals(ignoreDefinition)) {
                    Boolean visit = executionContext.getMessage("TARGET_CLASS");
                    if (visit != null && visit) {
                        return sf.withMarkers(sf.getMarkers().searchResult());
                    }
                }
                doAfterVisit(new UsesType<>(oldFullyQualifiedTypeName));
                return sf;
            }

            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext executionContext) {
                J.ClassDeclaration cd = super.visitClassDeclaration(classDecl, executionContext);
                if (!Boolean.TRUE.equals(ignoreDefinition) &&
                        TypeUtils.isOfClassType(cd.getType(), oldFullyQualifiedTypeName)) {
                    executionContext.putMessage("TARGET_CLASS", true);
                }
                return cd;
            }
        };
    }

    @Override
    public JavaVisitor<ExecutionContext> getVisitor() {
        return new ChangeTypeVisitor(oldFullyQualifiedTypeName, newFullyQualifiedTypeName, ignoreDefinition);
    }

    private static class ChangeTypeVisitor extends JavaVisitor<ExecutionContext> {
        private final JavaType.Class originalType;
        private final JavaType targetType;

        @Nullable
        private final Boolean ignoreDefinition;

        private ChangeTypeVisitor(String oldFullyQualifiedTypeName, String newFullyQualifiedTypeName, @Nullable Boolean ignoreDefinition) {
            this.originalType = JavaType.ShallowClass.build(oldFullyQualifiedTypeName);
            this.targetType = JavaType.buildType(newFullyQualifiedTypeName);
            this.ignoreDefinition = ignoreDefinition;
        }

        @Override
        public J visitJavaSourceFile(JavaSourceFile cu, ExecutionContext executionContext) {
            if (!Boolean.TRUE.equals(ignoreDefinition)) {
                JavaType.FullyQualified fq = TypeUtils.asFullyQualified(targetType);
                if (fq != null) {
                    ChangeClassDefinition changeClassDefinition = new ChangeClassDefinition(originalType.getFullyQualifiedName(), fq.getFullyQualifiedName());
                    cu = (JavaSourceFile) changeClassDefinition.visit(cu, executionContext);
                    assert cu != null;
                }
            }
            return super.visitJavaSourceFile(cu, executionContext);
        }

        @SuppressWarnings({"unchecked", "rawtypes", "ConstantConditions"})
        @Override
        public J visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {

            J.CompilationUnit c = visitAndCast(cu, ctx, super::visitCompilationUnit);
            c = (J.CompilationUnit) new RemoveImport<>(originalType.getFullyQualifiedName()).visit(c, ctx);
            if (originalType.getOwningClass() != null) {
                c = (J.CompilationUnit) new RemoveImport(originalType.getOwningClass().getFullyQualifiedName()).visit(c, ctx);
            }
            JavaType.FullyQualified fullyQualifiedTarget = TypeUtils.asFullyQualified(targetType);
            if (fullyQualifiedTarget != null) {
                if (fullyQualifiedTarget.getOwningClass() != null && !"java.lang".equals(fullyQualifiedTarget.getPackageName())) {
                    c = (J.CompilationUnit) new AddImport(fullyQualifiedTarget.getOwningClass().getFullyQualifiedName(), null, true).visit(c, ctx);
                }
                if (!"java.lang".equals(fullyQualifiedTarget.getPackageName())) {
                    c = (J.CompilationUnit) new AddImport(fullyQualifiedTarget.getFullyQualifiedName(), null, true).visit(c, ctx);
                }
            }
            if (c != null) {
                c = c.withImports(ListUtils.map(c.getImports(), i -> visitAndCast(i, ctx, super::visitImport)));
            }
            return c;
        }

        @Override
        public J visitImport(J.Import impoort, ExecutionContext executionContext) {
            // visitCompilationUnit() handles changing the imports.
            // If we call super.visitImport() then visitFieldAccess() will change the imports before AddImport/RemoveImport see them.
            // visitFieldAccess() doesn't have the import-specific formatting logic that AddImport/RemoveImport do.
            return impoort;
        }

        @Override
        public @Nullable J postVisit(J tree, ExecutionContext executionContext) {
            if (tree instanceof J.MethodDeclaration) {
                J.MethodDeclaration method = (J.MethodDeclaration) tree;
                tree = method.withMethodType(updateType(method.getMethodType()));
            } else if (tree instanceof J.MethodInvocation) {
                J.MethodInvocation method = (J.MethodInvocation) tree;
                tree = method.withMethodType(updateType(method.getMethodType()));
            }

            if (tree instanceof TypedTree) {
                if (tree instanceof J.MethodDeclaration) {
                    J.MethodDeclaration m = (J.MethodDeclaration) tree;
                    return m.withMethodType(updateType(m.getMethodType()));
                } else if (tree instanceof J.MethodInvocation) {
                    J.MethodInvocation m = (J.MethodInvocation) tree;
                    return m.withMethodType(updateType(m.getMethodType()));
                } else if (tree instanceof J.NewClass) {
                    J.NewClass n = (J.NewClass) tree;
                    return n.withConstructorType(updateType(n.getConstructorType()));
                }
                return ((TypedTree) tree).withType(updateType(((TypedTree) tree).getType()));
            }
            return tree;
        }

        @Override
        public J visitFieldAccess(J.FieldAccess fieldAccess, ExecutionContext ctx) {
            if (fieldAccess.isFullyQualifiedClassReference(originalType.getFullyQualifiedName())) {
                if (targetType instanceof JavaType.FullyQualified) {
                    return updateOuterClassTypes(TypeTree.build(((JavaType.FullyQualified) targetType).getFullyQualifiedName())
                            .withPrefix(fieldAccess.getPrefix()));
                } else if (targetType instanceof JavaType.Primitive) {
                    return new J.Primitive(
                            fieldAccess.getId(),
                            fieldAccess.getPrefix(),
                            Markers.EMPTY,
                            (JavaType.Primitive) targetType
                    );
                }
            } else {
                StringBuilder maybeClass = new StringBuilder();
                for (Expression target = fieldAccess; target != null; ) {
                    if (target instanceof J.FieldAccess) {
                        J.FieldAccess fa = (J.FieldAccess) target;
                        maybeClass.insert(0, fa.getSimpleName()).insert(0, '.');
                        target = fa.getTarget();
                    } else if (target instanceof J.Identifier) {
                        maybeClass.insert(0, ((J.Identifier) target).getSimpleName());
                        target = null;
                    } else {
                        maybeClass = new StringBuilder("__NOT_IT__");
                        break;
                    }
                }
                JavaType.Class oldType = JavaType.ShallowClass.build(originalType.getFullyQualifiedName());
                if (maybeClass.toString().equals(oldType.getClassName())) {
                    maybeRemoveImport(oldType.getOwningClass());
                    Expression e = updateOuterClassTypes(TypeTree.build(((JavaType.FullyQualified) targetType).getClassName())
                            .withPrefix(fieldAccess.getPrefix()));
                    // If a FieldAccess like Map.Entry has been replaced with an Identifier, ensure that identifier has the correct type
                    if (e instanceof J.Identifier && e.getType() == null) {
                        J.Identifier i = (J.Identifier) e;
                        e = i.withType(targetType);
                    }
                    return e;
                }
            }
            return super.visitFieldAccess(fieldAccess, ctx);
        }

        @Override
        public J visitIdentifier(J.Identifier ident, ExecutionContext ctx) {
            // if the ident's type is equal to the type we're looking for, and the classname of the type we're looking for is equal to the ident's string representation
            // Then transform it, otherwise leave it alone
            J.Identifier i = visitAndCast(ident, ctx, super::visitIdentifier);

            if (TypeUtils.isOfClassType(i.getType(), originalType.getFullyQualifiedName())) {
                String className = originalType.getClassName();
                JavaType.FullyQualified iType = TypeUtils.asFullyQualified(i.getType());
                if (iType != null && iType.getOwningClass() != null) {
                    className = originalType.getFullyQualifiedName().substring(iType.getOwningClass().getFullyQualifiedName().length() + 1);
                }

                if (i.getSimpleName().equals(className)) {
                    if (targetType instanceof JavaType.FullyQualified) {
                        if (((JavaType.FullyQualified) targetType).getOwningClass() != null) {
                            return updateOuterClassTypes(TypeTree.build(((JavaType.FullyQualified) targetType).getClassName())
                                    .withType(null)
                                    .withPrefix(i.getPrefix()));
                        } else {
                            i = i.withSimpleName(((JavaType.FullyQualified) targetType).getClassName());
                        }
                    } else if (targetType instanceof JavaType.Primitive) {
                        i = i.withSimpleName(((JavaType.Primitive) targetType).getKeyword());
                    }
                }
            }
            return i.withType(updateType(i.getType()));
        }

        @Override
        public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            if (method.getMethodType() != null && method.getMethodType().hasFlags(Flag.Static)) {
                if (method.getMethodType().getDeclaringType().isAssignableFrom(originalType)) {
                    J.CompilationUnit cu = getCursor().firstEnclosingOrThrow(J.CompilationUnit.class);

                    for (J.Import anImport : cu.getImports()) {
                        if (anImport.isStatic() && anImport.getQualid().getTarget().getType() != null) {
                            JavaType.FullyQualified fqn = TypeUtils.asFullyQualified(anImport.getQualid().getTarget().getType());
                            if (fqn != null && TypeUtils.isOfClassType(fqn, originalType.getFullyQualifiedName()) &&
                                    method.getSimpleName().equals(anImport.getQualid().getSimpleName())) {
                                maybeAddImport(((JavaType.FullyQualified) targetType).getFullyQualifiedName(), method.getName().getSimpleName());
                                break;
                            }
                        }
                    }
                }
            }
            return super.visitMethodInvocation(method, ctx);
        }

        private Expression updateOuterClassTypes(Expression typeTree) {
            if (typeTree instanceof J.FieldAccess) {
                JavaType.FullyQualified type = (JavaType.FullyQualified) targetType;

                if (type.getOwningClass() == null) {
                    // just a performance shortcut when this isn't an inner class
                    typeTree.withType(updateType(targetType));
                }

                Stack<Expression> typeStack = new Stack<>();
                typeStack.push(typeTree);

                Stack<JavaType.FullyQualified> attrStack = new Stack<>();
                attrStack.push(type);

                for (Expression t = ((J.FieldAccess) typeTree).getTarget(); ; ) {
                    typeStack.push(t);
                    if (t instanceof J.FieldAccess) {
                        if (Character.isUpperCase(((J.FieldAccess) t).getSimpleName().charAt(0))) {
                            if (attrStack.peek().getOwningClass() != null) {
                                attrStack.push(attrStack.peek().getOwningClass());
                            }
                        }
                        t = ((J.FieldAccess) t).getTarget();
                    } else if (t instanceof J.Identifier) {
                        if (Character.isUpperCase(((J.Identifier) t).getSimpleName().charAt(0))) {
                            if (attrStack.peek().getOwningClass() != null) {
                                attrStack.push(attrStack.peek().getOwningClass());
                            }
                        }
                        break;
                    }
                }

                Expression attributed = null;
                for (Expression e = typeStack.pop(); ; e = typeStack.pop()) {
                    if (e instanceof J.Identifier) {
                        if (attrStack.size() == typeStack.size() + 1) {
                            attributed = ((J.Identifier) e).withType(attrStack.pop());
                        } else {
                            attributed = e;
                        }
                    } else if (e instanceof J.FieldAccess) {
                        if (attrStack.size() == typeStack.size() + 1) {
                            attributed = ((J.FieldAccess) e).withTarget(attributed)
                                    .withType(attrStack.pop());
                        } else {
                            attributed = ((J.FieldAccess) e).withTarget(attributed);
                        }
                    }
                    if (typeStack.isEmpty()) {
                        break;
                    }
                }

                assert attributed != null;
                return attributed;
            }
            return typeTree;
        }

        private JavaType updateType(@Nullable JavaType type) {
            JavaType.GenericTypeVariable gtv = TypeUtils.asGeneric(type);
            if (gtv != null) {
                return gtv.withBounds(ListUtils.map(gtv.getBounds(), bound -> {
                    JavaType.FullyQualified fq = TypeUtils.asFullyQualified(bound);
                    if (fq != null) {
                        if (fq.getFullyQualifiedName().equals(originalType.getFullyQualifiedName()) && targetType instanceof JavaType.FullyQualified) {
                            return targetType;
                        }
                    }
                    return bound;
                }));
            }

            JavaType.FullyQualified fqt = TypeUtils.asFullyQualified(type);
            if (fqt != null && fqt.getFullyQualifiedName().equals(originalType.getFullyQualifiedName())) {
                return targetType;
            }

            //noinspection ConstantConditions
            return type;
        }

        @Nullable
        private JavaType.Method updateType(@Nullable JavaType.Method mt) {
            if (mt != null) {
                return mt.withDeclaringType((JavaType.FullyQualified) updateType(mt.getDeclaringType()))
                        .withReturnType(updateType(mt.getReturnType()))
                        .withParameterTypes(ListUtils.map(mt.getParameterTypes(), this::updateType));
            }
            return null;
        }
    }

    private static class ChangeClassDefinition extends JavaIsoVisitor<ExecutionContext> {
        private final JavaType.Class originalType;
        private final JavaType.Class targetType;

        private ChangeClassDefinition(String oldFullyQualifiedTypeName, String newFullyQualifiedTypeName) {
            this.originalType = JavaType.ShallowClass.build(oldFullyQualifiedTypeName);
            this.targetType = JavaType.ShallowClass.build(newFullyQualifiedTypeName);
        }

        @Override
        public JavaSourceFile visitJavaSourceFile(JavaSourceFile sf, ExecutionContext ctx) {
            String oldPath = ((SourceFile) sf).getSourcePath().toString().replace('\\', '/');
            // The old FQN must exist in the path.
            String oldFqn = fqnToPath(originalType.getFullyQualifiedName());
            String newFqn = fqnToPath(targetType.getFullyQualifiedName());

            Path newPath = Paths.get(oldPath.replaceFirst(oldFqn, newFqn));
            if (updatePath(sf, oldPath, newPath.toString())) {
                sf = ((SourceFile) sf).withSourcePath(newPath);
            }
            return super.visitJavaSourceFile(sf, ctx);
        }

        private String fqnToPath(String fullyQualifiedName) {
            int index = fullyQualifiedName.indexOf("$");
            String topLevelClassName = index == -1 ? fullyQualifiedName : fullyQualifiedName.substring(0, index);
            return topLevelClassName.replace('.', '/');
        }

        private boolean updatePath(JavaSourceFile sf, String oldPath, String newPath) {
            return !oldPath.equals(newPath) && sf.getClasses().stream()
                    .anyMatch(o -> J.Modifier.hasModifier(o.getModifiers(), J.Modifier.Type.Public) &&
                            o.getType() != null && !o.getType().getFullyQualifiedName().contains("$") &&
                            TypeUtils.isOfClassType(o.getType(), getTopLevelClassName(originalType)));
        }

        private String getTopLevelClassName(JavaType.FullyQualified classType) {
            if (classType.getOwningClass() == null) {
                return classType.getFullyQualifiedName();
            }
            return getTopLevelClassName(classType.getOwningClass());
        }

        @Override
        public J.Package visitPackage(J.Package pkg, ExecutionContext executionContext) {
            J.Package p = super.visitPackage(pkg, executionContext);
            String original = p.getExpression().printTrimmed(getCursor()).replaceAll("\\s", "");
            if (original.equals(originalType.getPackageName())) {
                JavaType.FullyQualified fq = TypeUtils.asFullyQualified(targetType);
                if (fq != null) {
                    if (fq.getPackageName().isEmpty()) {
                        getCursor().putMessageOnFirstEnclosing(J.CompilationUnit.class, "UPDATE_PREFIX", true);
                        p = null;
                    } else {
                        String newPkg = targetType.getPackageName();
                        p = p.withTemplate(JavaTemplate.builder(this::getCursor, newPkg).build(), p.getCoordinates().replace());
                    }
                }
            }
            //noinspection ConstantConditions
            return p;
        }

        @Override
        public J.Import visitImport(J.Import _import, ExecutionContext executionContext) {
            Boolean updatePrefix = getCursor().pollNearestMessage("UPDATE_PREFIX");
            if (updatePrefix != null && updatePrefix) {
                _import = _import.withPrefix(Space.EMPTY);
            }
            return super.visitImport(_import, executionContext);
        }

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext executionContext) {
            J.ClassDeclaration cd = super.visitClassDeclaration(classDecl, executionContext);
            Boolean updatePrefix = getCursor().pollNearestMessage("UPDATE_PREFIX");
            if (updatePrefix != null && updatePrefix) {
                cd = cd.withPrefix(Space.EMPTY);
            }

            if (TypeUtils.isOfClassType(classDecl.getType(), originalType.getFullyQualifiedName())) {
                String newClassName = getNewClassName(targetType);
                cd = cd.withName(cd.getName().withSimpleName(newClassName));
                cd = cd.withType(updateType(cd.getType()));
            }
            return cd;
        }

        private String getNewClassName(JavaType.FullyQualified fq) {
            return fq.getOwningClass() == null ? fq.getClassName() :
                    fq.getFullyQualifiedName().substring(fq.getOwningClass().getFullyQualifiedName().length() + 1);
        }

        private JavaType updateType(@Nullable JavaType type) {
            JavaType.GenericTypeVariable gtv = TypeUtils.asGeneric(type);
            if (gtv != null) {
                return gtv.withBounds(ListUtils.map(gtv.getBounds(), bound -> {
                    JavaType.FullyQualified fq = TypeUtils.asFullyQualified(bound);
                    if (fq != null) {
                        if (fq.getFullyQualifiedName().equals(originalType.getFullyQualifiedName())) {
                            return targetType;
                        }
                    }
                    return bound;
                }));
            }

            JavaType.FullyQualified fqt = TypeUtils.asFullyQualified(type);
            if (fqt != null && fqt.getFullyQualifiedName().equals(originalType.getFullyQualifiedName())) {
                return targetType;
            }

            //noinspection ConstantConditions
            return type;
        }
    }
}
