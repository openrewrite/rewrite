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
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.trait.Reference;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Objects.requireNonNull;

@Value
@EqualsAndHashCode(callSuper = false)
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
            required = false)
    @Nullable
    Boolean ignoreDefinition;

    String displayName = "Change type";

    @Override
    public String getInstanceNameSuffix() {
        // Defensively guard against null values when recipes are first classloaded. This
        // is a temporary workaround until releases of workers/CLI that include the defensive
        // coding in Recipe.
        //noinspection ConstantValue
        if (oldFullyQualifiedTypeName == null || newFullyQualifiedTypeName == null) {
            return getDisplayName();
        }

        String oldShort = oldFullyQualifiedTypeName.substring(oldFullyQualifiedTypeName.lastIndexOf('.') + 1);
        String newShort = newFullyQualifiedTypeName.substring(newFullyQualifiedTypeName.lastIndexOf('.') + 1);
        if (oldShort.equals(newShort)) {
            return String.format("`%s` to `%s`",
                    oldFullyQualifiedTypeName,
                    newFullyQualifiedTypeName);
        } else {
            return String.format("`%s` to `%s`",
                    oldShort, newShort);
        }
    }

    String description = "Change a given type to another.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        TreeVisitor<?, ExecutionContext> condition = new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree preVisit(@Nullable Tree tree, ExecutionContext ctx) {
                stopAfterPreVisit();
                if (tree instanceof JavaSourceFile) {
                    JavaSourceFile cu = (JavaSourceFile) tree;
                    if (!Boolean.TRUE.equals(ignoreDefinition) && containsClassDefinition(cu, oldFullyQualifiedTypeName)) {
                        return SearchResult.found(cu);
                    }
                    return new UsesType<>(oldFullyQualifiedTypeName, true).visitNonNull(cu, ctx);
                } else if (tree instanceof SourceFileWithReferences) {
                    SourceFileWithReferences cu = (SourceFileWithReferences) tree;
                    return new UsesType<>(oldFullyQualifiedTypeName, true).visitNonNull(cu, ctx);
                }
                return tree;
            }
        };

        return Preconditions.check(condition, new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public boolean isAcceptable(SourceFile sourceFile, ExecutionContext ctx) {
                return sourceFile instanceof JavaSourceFile || sourceFile instanceof SourceFileWithReferences;
            }

            @Override
            public @Nullable Tree preVisit(@Nullable Tree tree, ExecutionContext ctx) {
                stopAfterPreVisit();
                if (tree instanceof J) {
                    return new JavaChangeTypeVisitor(oldFullyQualifiedTypeName, newFullyQualifiedTypeName, ignoreDefinition).visit(tree, ctx, requireNonNull(getCursor().getParent()));
                } else if (tree instanceof SourceFileWithReferences) {
                    SourceFileWithReferences sourceFile = (SourceFileWithReferences) tree;
                    SourceFileWithReferences.References references = sourceFile.getReferences();
                    TypeMatcher matcher = new TypeMatcher(oldFullyQualifiedTypeName);
                    Map<Tree, Reference> matches = new HashMap<>();
                    for (Reference ref : references.findMatches(matcher)) {
                        matches.put(ref.getTree(), ref);
                    }
                    return new ReferenceChangeTypeVisitor(matches, matcher.createRenamer(newFullyQualifiedTypeName)).visit(tree, ctx, requireNonNull(getCursor().getParent()));
                }
                return tree;
            }
        });
    }

    private static class JavaChangeTypeVisitor extends JavaVisitor<ExecutionContext> {
        private final JavaType.Class originalType;
        private final JavaType targetType;


        private J.@Nullable Identifier importAlias;

        @Nullable
        private final Boolean ignoreDefinition;

        private final Map<JavaType, JavaType> oldNameToChangedType = new IdentityHashMap<>();
        private final Set<String> topLevelClassnames = new HashSet<>();

        private JavaChangeTypeVisitor(String oldFullyQualifiedTypeName, String newFullyQualifiedTypeName, @Nullable Boolean ignoreDefinition) {
            this.originalType = JavaType.ShallowClass.build(oldFullyQualifiedTypeName);
            this.targetType = JavaType.buildType(newFullyQualifiedTypeName);
            this.ignoreDefinition = ignoreDefinition;
            importAlias = null;
        }

        @Override
        public J visit(@Nullable Tree tree, ExecutionContext ctx) {
            if (tree instanceof JavaSourceFile) {
                JavaSourceFile cu = (JavaSourceFile) tree;
                if (!Boolean.TRUE.equals(ignoreDefinition)) {
                    JavaType.FullyQualified fq = TypeUtils.asFullyQualified(targetType);
                    if (fq != null) {
                        ChangeClassDefinition changeClassDefinition = new ChangeClassDefinition(originalType.getFullyQualifiedName(), fq.getFullyQualifiedName());
                        cu = (JavaSourceFile) changeClassDefinition.visitNonNull(cu, ctx);
                    }
                }
                return super.visit(cu, ctx);
            }
            return super.visit(tree, ctx);
        }

        @Override
        public J visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
            J.ClassDeclaration cd = (J.ClassDeclaration) super.visitClassDeclaration(classDecl, ctx);
            if (cd.getType() != null) {
                topLevelClassnames.add(getTopLevelClassName(cd.getType()).getFullyQualifiedName());
            }
            return cd;
        }

        @Override
        public J visitImport(J.Import import_, ExecutionContext ctx) {
            // Collect alias import information here
            // If there is an existing import with an alias, we need to add a target import with an alias accordingly.
            // If there is an existing import without an alias, we need to add a target import with an alias accordingly.
            if (hasSameFQN(import_, originalType)) {
                if (import_.getAlias() != null) {
                    importAlias = import_.getAlias();
                }
            }

            // visitCompilationUnit() handles changing the imports.
            // If we call super.visitImport() then visitFieldAccess() will change the imports before AddImport/RemoveImport see them.
            // visitFieldAccess() doesn't have the import-specific formatting logic that AddImport/RemoveImport do.
            return import_;
        }

        @Override
        public @Nullable JavaType visitType(@Nullable JavaType javaType, ExecutionContext ctx) {
            return updateType(javaType);
        }

        private void addImport(JavaType.FullyQualified owningClass) {
            if (importAlias != null) {
                maybeAddImport(owningClass.getPackageName(), owningClass.getClassName(), null, importAlias.getSimpleName(), true);
            }

            maybeAddImport(owningClass.getPackageName(), owningClass.getClassName(), null, null, true);
        }

        @Override
        public @Nullable J postVisit(J tree, ExecutionContext ctx) {
            J j = super.postVisit(tree, ctx);
            if (j instanceof J.ArrayType) {
                J.ArrayType arrayType = (J.ArrayType) j;
                JavaType type = updateType(arrayType.getType());
                j = arrayType.withType(type);
            } else if (j instanceof J.MethodDeclaration) {
                J.MethodDeclaration method = (J.MethodDeclaration) j;
                JavaType.Method mt = updateType(method.getMethodType());
                j = method.withMethodType(mt)
                        .withName(method.getName().withType(mt));
            } else if (j instanceof J.MethodInvocation) {
                J.MethodInvocation method = (J.MethodInvocation) j;
                JavaType.Method mt = updateType(method.getMethodType());
                j = method.withMethodType(mt)
                        .withName(method.getName().withType(mt));
            } else if (j instanceof J.NewClass) {
                J.NewClass n = (J.NewClass) j;
                j = n.withConstructorType(updateType(n.getConstructorType()));
            } else if (tree instanceof TypedTree) {
                j = ((TypedTree) tree).withType(updateType(((TypedTree) tree).getType()));
            } else if (tree instanceof JavaSourceFile) {
                JavaSourceFile sf = (JavaSourceFile) tree;
                if (targetType instanceof JavaType.FullyQualified) {
                    for (J.Import anImport : sf.getImports()) {
                        if (anImport.isStatic()) {
                            continue;
                        }

                        JavaType maybeType = anImport.getQualid().getType();
                        if (maybeType instanceof JavaType.FullyQualified) {
                            JavaType.FullyQualified type = (JavaType.FullyQualified) maybeType;
                            if (originalType.getFullyQualifiedName().equals(type.getFullyQualifiedName())) {
                                sf = (JavaSourceFile) new RemoveImport<ExecutionContext>(originalType.getFullyQualifiedName()).visitNonNull(sf, ctx, getCursor().getParentOrThrow());
                            } else if (originalType.getOwningClass() != null && originalType.getOwningClass().getFullyQualifiedName().equals(type.getFullyQualifiedName())) {
                                sf = (JavaSourceFile) new RemoveImport<ExecutionContext>(originalType.getOwningClass().getFullyQualifiedName()).visitNonNull(sf, ctx, getCursor().getParentOrThrow());
                            }
                        }
                    }
                }

                JavaType.FullyQualified fullyQualifiedTarget = TypeUtils.asFullyQualified(targetType);
                if (fullyQualifiedTarget != null) {
                    JavaType.FullyQualified owningClass = fullyQualifiedTarget.getOwningClass();
                    if (!topLevelClassnames.contains(getTopLevelClassName(fullyQualifiedTarget).getFullyQualifiedName())) {
                        if (hasNoConflictingImport(sf)) {
                            if (owningClass != null && !"java.lang".equals(fullyQualifiedTarget.getPackageName())) {
                                addImport(owningClass);
                            }
                            if (!"java.lang".equals(fullyQualifiedTarget.getPackageName())) {
                                addImport(fullyQualifiedTarget);
                            }
                        }
                    }
                }

                j = sf.withImports(ListUtils.map(sf.getImports(), i -> {
                    Cursor cursor = getCursor();
                    setCursor(new Cursor(cursor, i));
                    try {
                        return visitAndCast(i, ctx, super::visitImport);
                    } finally {
                        setCursor(cursor);
                    }
                }));
            }

            return j;
        }

        @Override
        public J visitFieldAccess(J.FieldAccess fieldAccess, ExecutionContext ctx) {
            if (fieldAccess.isFullyQualifiedClassReference(originalType.getFullyQualifiedName())) {
                if (targetType instanceof JavaType.FullyQualified) {
                    J.FieldAccess fa = (J.FieldAccess) updateOuterClassTypes(TypeTree.build(((JavaType.FullyQualified) targetType).getFullyQualifiedName())
                            .withPrefix(fieldAccess.getPrefix()));
                    if (getCursor().firstEnclosing(J.Import.class) == null) {
                        // don't shorten qualified names in imports
                        fa = fa.withName(fa.getName().withType(targetType));
                        doAfterVisit(ShortenFullyQualifiedTypeReferences.modifyOnly(fa));
                    }
                    return fa;
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
                } else if (maybeClass.toString().equals(oldType.getFullyQualifiedName().replace('$', '.'))) {
                    maybeRemoveImport(oldType.getOwningClass());
                    return updateOuterClassTypes(TypeTree.build(((JavaType.FullyQualified) targetType).getFullyQualifiedName())
                            .withPrefix(fieldAccess.getPrefix()));
                }
            }
            return super.visitFieldAccess(fieldAccess, ctx);
        }

        @Override
        public J visitIdentifier(J.Identifier ident, ExecutionContext ctx) {
            // Do not modify the identifier if it's on a inner class definition.
            if (Boolean.TRUE.equals(ignoreDefinition) && getCursor().getParent() != null &&
                    getCursor().getParent().getValue() instanceof J.ClassDeclaration) {
                return super.visitIdentifier(ident, ctx);
            }
            // if the ident's type is equal to the type we're looking for, and the classname of the type we're looking for is equal to the ident's string representation
            // Then transform it, otherwise leave it alone
            if (TypeUtils.isOfClassType(ident.getType(), originalType.getFullyQualifiedName())) {
                String className = originalType.getClassName();
                JavaType.FullyQualified iType = TypeUtils.asFullyQualified(ident.getType());
                if (iType != null && iType.getOwningClass() != null) {
                    className = originalType.getFullyQualifiedName().substring(iType.getOwningClass().getFullyQualifiedName().length() + 1);
                }

                JavaSourceFile sf = getCursor().firstEnclosing(JavaSourceFile.class);
                if (ident.getSimpleName().equals(className)) {
                    if (targetType instanceof JavaType.FullyQualified) {
                        if (((JavaType.FullyQualified) targetType).getOwningClass() != null) {
                            return updateOuterClassTypes(TypeTree.build(((JavaType.FullyQualified) targetType).getClassName())
                                    .withType(null)
                                    .withPrefix(ident.getPrefix()));
                        } else {
                            if (hasNoConflictingImport(sf)) {
                                ident = ident.withSimpleName(((JavaType.FullyQualified) targetType).getClassName());
                            } else {
                                ident = ident.withSimpleName(((JavaType.FullyQualified) targetType).getFullyQualifiedName());
                            }
                        }
                    } else if (targetType instanceof JavaType.Primitive) {
                        ident = ident.withSimpleName(((JavaType.Primitive) targetType).getKeyword());
                    }
                }

                // Recreate any static imports as needed
                if (sf != null) {
                    for (J.Import anImport : sf.getImports()) {
                        if (anImport.isStatic() && anImport.getQualid().getTarget().getType() != null) {
                            JavaType.FullyQualified fqn = TypeUtils.asFullyQualified(anImport.getQualid().getTarget().getType());
                            if (fqn != null && TypeUtils.isOfClassType(fqn, originalType.getFullyQualifiedName()) &&
                                    ident.getSimpleName().equals(anImport.getQualid().getSimpleName())) {
                                JavaType.FullyQualified targetFqn = (JavaType.FullyQualified) targetType;
                                maybeAddImport((targetFqn).getFullyQualifiedName(), ident.getSimpleName());
                                break;
                            }
                        }
                    }
                }
            }
            ident = ident.withType(updateType(ident.getType()));
            return visitAndCast(ident, ctx, super::visitIdentifier);
        }

        @Override
        public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            if (method.getMethodType() != null && method.getMethodType().hasFlags(Flag.Static)) {
                if (method.getMethodType().getDeclaringType().isAssignableFrom(originalType)) {
                    JavaSourceFile cu = getCursor().firstEnclosingOrThrow(JavaSourceFile.class);

                    for (J.Import anImport : cu.getImports()) {
                        if (anImport.isStatic() && anImport.getQualid().getTarget().getType() != null) {
                            JavaType.FullyQualified fqn = TypeUtils.asFullyQualified(anImport.getQualid().getTarget().getType());
                            if (fqn != null && TypeUtils.isOfClassType(fqn, originalType.getFullyQualifiedName()) &&
                                    method.getSimpleName().equals(anImport.getQualid().getSimpleName())) {
                                JavaType.FullyQualified targetFqn = (JavaType.FullyQualified) targetType;

                                addImport(targetFqn);
                                maybeAddImport((targetFqn).getFullyQualifiedName(), method.getName().getSimpleName());
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
                    return typeTree.withType(updateType(targetType));
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

        private @Nullable JavaType updateType(@Nullable JavaType oldType) {
            if (oldType == null || oldType instanceof JavaType.Unknown) {
                return oldType;
            }

            JavaType type = oldNameToChangedType.get(oldType);
            if (type != null) {
                return type;
            }

            if (oldType instanceof JavaType.Parameterized) {
                JavaType.Parameterized pt = (JavaType.Parameterized) oldType;
                pt = pt.withTypeParameters(ListUtils.map(pt.getTypeParameters(), tp -> {
                    if (tp instanceof JavaType.FullyQualified) {
                        JavaType.FullyQualified tpFq = (JavaType.FullyQualified) tp;
                        if (isTargetFullyQualifiedType(tpFq) || isNestedTypeOfOriginal(tpFq)) {
                            return updateType(tpFq);
                        }
                    }
                    return tp;
                }));

                if (isTargetFullyQualifiedType(pt)) {
                    pt = pt.withType((JavaType.FullyQualified) updateType(pt.getType()));
                } else if (isNestedTypeOfOriginal(pt)) {
                    pt = pt.withType(updateNestedType(pt.getType()));
                }
                oldNameToChangedType.put(oldType, pt);
                oldNameToChangedType.put(pt, pt);
                return pt;
            } else if (oldType instanceof JavaType.FullyQualified) {
                JavaType.FullyQualified original = TypeUtils.asFullyQualified(oldType);
                if (isTargetFullyQualifiedType(original)) {
                    oldNameToChangedType.put(oldType, targetType);
                    return targetType;
                } else if (isNestedTypeOfOriginal(original)) {
                    JavaType.FullyQualified updatedNestedType = updateNestedType(original);
                    oldNameToChangedType.put(oldType, updatedNestedType);
                    return updatedNestedType;
                } else if (oldType instanceof JavaType.Class) {
                    JavaType.Class clazz = ((JavaType.Class) oldType)
                            .withInterfaces(ListUtils.map(original.getInterfaces(), t -> (JavaType.FullyQualified) updateType(t)))
                            .withSupertype((JavaType.FullyQualified) updateType(original.getSupertype()));
                    oldNameToChangedType.put(oldType, clazz);
                    oldNameToChangedType.put(clazz, clazz);
                    return clazz;
                }
            } else if (oldType instanceof JavaType.GenericTypeVariable) {
                JavaType.GenericTypeVariable gtv = (JavaType.GenericTypeVariable) oldType;
                gtv = gtv.withBounds(ListUtils.map(gtv.getBounds(), b -> {
                    if (b instanceof JavaType.FullyQualified) {
                        JavaType.FullyQualified fq = (JavaType.FullyQualified) b;
                        if (isTargetFullyQualifiedType(fq) || isNestedTypeOfOriginal(fq)) {
                            return updateType(b);
                        }
                    }
                    return b;
                }));

                oldNameToChangedType.put(oldType, gtv);
                oldNameToChangedType.put(gtv, gtv);
                return gtv;
            } else if (oldType instanceof JavaType.Variable) {
                JavaType.Variable variable = (JavaType.Variable) oldType;
                variable = variable.withOwner(updateType(variable.getOwner()));
                variable = variable.withType(updateType(variable.getType()));
                oldNameToChangedType.put(oldType, variable);
                oldNameToChangedType.put(variable, variable);
                return variable;
            } else if (oldType instanceof JavaType.Array) {
                JavaType.Array array = (JavaType.Array) oldType;
                array = array.withElemType(updateType(array.getElemType()));
                oldNameToChangedType.put(oldType, array);
                oldNameToChangedType.put(array, array);
                return array;
            }

            return oldType;
        }

        private JavaType.@Nullable Method updateType(JavaType.@Nullable Method oldMethodType) {
            if (oldMethodType != null) {
                JavaType.Method method = (JavaType.Method) oldNameToChangedType.get(oldMethodType);
                if (method != null) {
                    return method;
                }

                method = oldMethodType;
                method = method.withDeclaringType((JavaType.FullyQualified) updateType(method.getDeclaringType()))
                        .withReturnType(updateType(method.getReturnType()))
                        .withParameterTypes(ListUtils.map(method.getParameterTypes(), this::updateType));
                oldNameToChangedType.put(oldMethodType, method);
                oldNameToChangedType.put(method, method);
                return method;
            }
            return null;
        }

        private boolean isTargetFullyQualifiedType(JavaType.@Nullable FullyQualified fq) {
            return fq != null && TypeUtils.isOfClassType(fq, originalType.getFullyQualifiedName()) && targetType instanceof JavaType.FullyQualified;
        }

        private boolean isNestedTypeOfOriginal(JavaType.@Nullable FullyQualified fq) {
            if (fq == null || !(targetType instanceof JavaType.FullyQualified)) {
                return false;
            }
            String fqn = fq.getFullyQualifiedName();
            String originalFqn = originalType.getFullyQualifiedName();
            // Check if fq is a nested class of the original type (e.g., a.b.c.A$Nested when original is a.b.c.A)
            return fqn.startsWith(originalFqn + "$");
        }

        private JavaType.FullyQualified updateNestedType(JavaType.FullyQualified nestedType) {
            String nestedFqn = nestedType.getFullyQualifiedName();
            String originalFqn = originalType.getFullyQualifiedName();
            String targetFqn = ((JavaType.FullyQualified) targetType).getFullyQualifiedName();
            // Replace the original type prefix with the target type prefix
            String newNestedFqn = targetFqn + nestedFqn.substring(originalFqn.length());
            return JavaType.ShallowClass.build(newNestedFqn);
        }

        private boolean hasNoConflictingImport(@Nullable JavaSourceFile sf) {
            JavaType.FullyQualified oldType = TypeUtils.asFullyQualified(originalType);
            JavaType.FullyQualified newType = TypeUtils.asFullyQualified(targetType);
            if (sf == null || oldType == null || newType == null) {
                return true; // No way to be sure so we retain previous behaviour
            }
            for (J.Import anImport : sf.getImports()) {
                JavaType.FullyQualified currType = TypeUtils.asFullyQualified(anImport.getQualid().getType());
                if (currType != null &&
                        !TypeUtils.isOfType(currType, oldType) &&
                        !TypeUtils.isOfType(currType, newType) &&
                        currType.getClassName().equals(newType.getClassName())) {
                    return false;
                }
            }
            return true;
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    private static class ReferenceChangeTypeVisitor extends TreeVisitor<Tree, ExecutionContext> {
        Map<Tree, Reference> matches;
        Reference.Renamer renamer;

        @Override
        public Tree postVisit(Tree tree, ExecutionContext ctx) {
            Reference reference = matches.get(tree);
            if (reference != null && reference.supportsRename()) {
                return reference.rename(renamer, getCursor(), ctx);
            }
            return tree;
        }
    }

    private static class ChangeClassDefinition extends JavaIsoVisitor<ExecutionContext> {
        private final JavaType.Class originalType;
        private final JavaType.Class targetType;
        private final MethodMatcher originalConstructor;

        private ChangeClassDefinition(String oldFullyQualifiedTypeName, String newFullyQualifiedTypeName) {
            this.originalType = JavaType.ShallowClass.build(oldFullyQualifiedTypeName);
            this.targetType = JavaType.ShallowClass.build(newFullyQualifiedTypeName);
            this.originalConstructor = new MethodMatcher(oldFullyQualifiedTypeName + " <constructor>(..)");
        }

        @Override
        public J visit(@Nullable Tree tree, ExecutionContext ctx) {
            if (tree instanceof JavaSourceFile) {
                JavaSourceFile cu = (JavaSourceFile) tree;
                for (J.ClassDeclaration declaration : cu.getClasses()) {
                    // Check the class name instead of source path, as it might differ
                    String fqn = declaration.getType().getFullyQualifiedName();
                    if (fqn.equals(originalType.getFullyQualifiedName())) {
                        getCursor().putMessage("UPDATE_PACKAGE", true);
                        break;
                    }
                }

                String oldPath = cu.getSourcePath().toString().replace('\\', '/');
                String oldFqn = fqnToPath(originalType.getFullyQualifiedName());
                String newFqn = fqnToPath(targetType.getFullyQualifiedName());
                int lastDot = oldPath.lastIndexOf('.');
                String extension = lastDot >= 0 ? oldPath.substring(lastDot) : "";
                String newPathStr = oldPath;
                if (!extension.isEmpty() && oldPath.endsWith(oldFqn + extension)) {
                    newPathStr = oldPath.substring(0, oldPath.length() - (oldFqn + extension).length()) + newFqn + extension;
                }
                Path newPath = Paths.get(newPathStr);
                if (updatePath(cu, oldPath, newPath.toString())) {
                    cu = cu.withSourcePath(newPath);
                }
                return super.visit(cu, ctx);
            }
            return super.visit(tree, ctx);
        }

        private String fqnToPath(String fullyQualifiedName) {
            int index = fullyQualifiedName.indexOf("$");
            String topLevelClassName = index == -1 ? fullyQualifiedName : fullyQualifiedName.substring(0, index);
            return topLevelClassName.replace('.', '/');
        }

        private boolean updatePath(JavaSourceFile sf, String oldPath, String newPath) {
            return !oldPath.equals(newPath) && sf.getClasses().stream()
                    .anyMatch(o -> !o.hasModifier(J.Modifier.Type.Private) &&
                            o.getType() != null && !o.getType().getFullyQualifiedName().contains("$") &&
                            TypeUtils.isOfClassType(o.getType(), getTopLevelClassName(originalType).getFullyQualifiedName()));
        }

        @Override
        public J.@Nullable Package visitPackage(J.Package pkg, ExecutionContext ctx) {
            Boolean updatePackage = getCursor().pollNearestMessage("UPDATE_PACKAGE");
            if (updatePackage != null && updatePackage) {
                String original = pkg.getExpression().printTrimmed(getCursor()).replaceAll("\\s", "");
                if (original.equals(originalType.getPackageName())) {
                    JavaType.FullyQualified fq = TypeUtils.asFullyQualified(targetType);
                    if (fq != null) {
                        if (fq.getPackageName().isEmpty()) {
                            getCursor().putMessageOnFirstEnclosing(J.CompilationUnit.class, "UPDATE_PREFIX", true);
                            //noinspection DataFlowIssue
                            return null;
                        } else {
                            String newPkg = targetType.getPackageName();
                            return JavaTemplate.builder(newPkg).contextSensitive().build().apply(getCursor(), pkg.getCoordinates().replace());
                        }
                    }
                }
            }
            //noinspection ConstantConditions
            return pkg;
        }

        @Override
        public J.Import visitImport(J.Import _import, ExecutionContext ctx) {
            Boolean updatePrefix = getCursor().pollNearestMessage("UPDATE_PREFIX");
            if (updatePrefix != null && updatePrefix) {
                _import = _import.withPrefix(Space.EMPTY);
            }
            return super.visitImport(_import, ctx);
        }

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
            J.ClassDeclaration cd = super.visitClassDeclaration(classDecl, ctx);
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

        @Override
        public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
            if (method.isConstructor() && originalConstructor.matches(method.getMethodType())) {
                method = method.withName(method.getName().withSimpleName(targetType.getClassName()));
                method = method.withMethodType(updateType(method.getMethodType()));
            }
            return super.visitMethodDeclaration(method, ctx);
        }

        private String getNewClassName(JavaType.FullyQualified fq) {
            return fq.getOwningClass() == null ? fq.getClassName() :
                    fq.getFullyQualifiedName().substring(fq.getOwningClass().getFullyQualifiedName().length() + 1);
        }

        private JavaType updateType(@Nullable JavaType oldType) {
            if (oldType instanceof JavaType.FullyQualified) {
                JavaType.FullyQualified original = TypeUtils.asFullyQualified(oldType);
                if (isTargetFullyQualifiedType(original)) {
                    return targetType;
                } else if (isNestedTypeOfOriginal(original)) {
                    return updateNestedType(original);
                }
            }

            //noinspection ConstantConditions
            return oldType;
        }

        private JavaType.@Nullable Method updateType(JavaType.@Nullable Method mt) {
            if (mt != null) {
                return mt.withDeclaringType((JavaType.FullyQualified) updateType(mt.getDeclaringType()))
                        .withReturnType(updateType(mt.getReturnType()))
                        .withParameterTypes(ListUtils.map(mt.getParameterTypes(), this::updateType));
            }
            return null;
        }

        private boolean isTargetFullyQualifiedType(JavaType.@Nullable FullyQualified fq) {
            return fq != null && TypeUtils.isOfClassType(fq, originalType.getFullyQualifiedName());
        }

        private boolean isNestedTypeOfOriginal(JavaType.@Nullable FullyQualified fq) {
            if (fq == null) {
                return false;
            }
            String fqn = fq.getFullyQualifiedName();
            String originalFqn = originalType.getFullyQualifiedName();
            // Check if fq is a nested class of the original type (e.g., a.b.c.A$Nested when original is a.b.c.A)
            return fqn.startsWith(originalFqn + "$");
        }

        private JavaType.FullyQualified updateNestedType(JavaType.FullyQualified nestedType) {
            String nestedFqn = nestedType.getFullyQualifiedName();
            String originalFqn = originalType.getFullyQualifiedName();
            String targetFqn = targetType.getFullyQualifiedName();
            // Replace the original type prefix with the target type prefix
            String newNestedFqn = targetFqn + nestedFqn.substring(originalFqn.length());
            return JavaType.ShallowClass.build(newNestedFqn);
        }
    }

    public static boolean containsClassDefinition(JavaSourceFile sourceFile, String fullyQualifiedTypeName) {
        AtomicBoolean found = new AtomicBoolean(false);
        JavaIsoVisitor<AtomicBoolean> visitor = new JavaIsoVisitor<AtomicBoolean>() {

            @Override
            public @Nullable J visit(@Nullable Tree tree, AtomicBoolean found) {
                if (found.get()) {
                    return (J) tree;
                }
                return super.visit(tree, found);
            }

            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, AtomicBoolean found) {
                if (found.get()) {
                    return classDecl;
                }

                if (classDecl.getType() != null && TypeUtils.isOfClassType(classDecl.getType(), fullyQualifiedTypeName)) {
                    found.set(true);
                    return classDecl;
                }
                return super.visitClassDeclaration(classDecl, found);
            }
        };
        visitor.visit(sourceFile, found);
        return found.get();
    }

    public static JavaType.FullyQualified getTopLevelClassName(JavaType.FullyQualified classType) {
        if (classType.getOwningClass() == null || TypeUtils.fullyQualifiedNamesAreEqual(classType.getFullyQualifiedName(),
                classType.getOwningClass().getFullyQualifiedName())) {
            return classType;
        }
        return getTopLevelClassName(classType.getOwningClass());
    }

    private static boolean hasSameFQN(J.Import import_, JavaType targetType) {
        JavaType.FullyQualified type = TypeUtils.asFullyQualified(targetType);
        String fqn = type != null ? type.getFullyQualifiedName() : null;

        JavaType.FullyQualified curType = TypeUtils.asFullyQualified(Optional.ofNullable(import_.getQualid()).map(J.FieldAccess::getType).orElse(null));
        String curFqn = curType != null ? curType.getFullyQualifiedName() : null;

        return fqn != null && fqn.equals(curFqn);
    }
}
