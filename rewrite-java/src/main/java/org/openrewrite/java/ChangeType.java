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
import org.openrewrite.java.internal.PackageNameUtils;
import org.openrewrite.java.internal.TypesInUse;
import org.openrewrite.java.marker.JavaSourceSet;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.service.ImportService;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.trait.Reference;

import java.lang.reflect.Method;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

@Value
@EqualsAndHashCode(callSuper = false)
public class ChangeType extends Recipe implements RenamingRecipe {

    /**
     * The kind of name ChangeType and ChangePackage rename, which shares one namespace of dotted names.
     */
    static final String JAVA_TYPES_AND_PACKAGES = "Java type or package";

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
    public String kindOfName() {
        return JAVA_TYPES_AND_PACKAGES;
    }

    @Override
    public String oldName() {
        return oldFullyQualifiedTypeName;
    }

    @Override
    public String rename(String name) {
        // the types nested in the old type move with it
        String oldName = oldFullyQualifiedTypeName.replace('$', '.');
        String dotted = name.replace('$', '.');
        if (dotted.equals(oldName)) {
            return newFullyQualifiedTypeName;
        } else if (dotted.startsWith(oldName + ".")) {
            return newFullyQualifiedTypeName.replace('$', '.') + dotted.substring(oldName.length());
        }
        return name;
    }

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
        // Normalize nested-class separators ($ -> .) so the package pre-filter can prefix-match the
        // file's package declaration regardless of which form the old FQN was given in.
        String normalizedOldName = oldFullyQualifiedTypeName.replace('$', '.');
        TreeVisitor<?, ExecutionContext> condition = new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree preVisit(@Nullable Tree tree, ExecutionContext ctx) {
                stopAfterPreVisit();
                if (tree instanceof JavaSourceFile) {
                    JavaSourceFile cu = (JavaSourceFile) tree;
                    // UsesType is an O(1) lookup against the cached type index; check it first and
                    // only fall back to the definition scan when the type isn't used.
                    Tree used = new UsesType<>(oldFullyQualifiedTypeName, true).visitNonNull(cu, ctx);
                    if (used != cu) {
                        return used;
                    }
                    // the types nested in the old type move with it
                    if (importsMemberOf(cu, normalizedOldName)) {
                        return SearchResult.found(cu);
                    }
                    // Fully qualified documentation-comment references are excluded from UsesType's
                    // import-retention view, but they still need to be retargeted.
                    if (hasDocReference(cu.getTypesInUse(), oldFullyQualifiedTypeName)) {
                        return SearchResult.found(cu);
                    }
                    if (!Boolean.TRUE.equals(ignoreDefinition) &&
                            mayContainDefinition(cu, normalizedOldName) &&
                            containsClassDefinition(cu, oldFullyQualifiedTypeName) &&
                            !declaresNestedTypeThatStays(cu, oldFullyQualifiedTypeName, newFullyQualifiedTypeName)) {
                        return SearchResult.found(cu);
                    }
                    return cu;
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
                    Map<Tree, List<Reference>> matches = new HashMap<>();
                    for (Reference ref : references.findMatches(matcher)) {
                        matches.computeIfAbsent(ref.getTree(), k -> new java.util.ArrayList<>()).add(ref);
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

        private final String newFullyQualifiedTypeName;
        private final boolean ignoreDefinition;

        /**
         * Whether the visited file's definition of the original type stays as it is, where asked to or where renaming
         * it in place can't make it the target type.
         */
        private boolean keepsDefinition;

        private final TypeReplacer types = new TypeReplacer(this::replacement);
        private final Set<String> declaredClassNames = new HashSet<>();
        private @Nullable Set<String> otherTypeNames;

        private JavaChangeTypeVisitor(String oldFullyQualifiedTypeName, String newFullyQualifiedTypeName, @Nullable Boolean ignoreDefinition) {
            this.originalType = JavaType.ShallowClass.build(oldFullyQualifiedTypeName);
            this.targetType = JavaType.buildType(newFullyQualifiedTypeName);
            this.newFullyQualifiedTypeName = newFullyQualifiedTypeName;
            this.ignoreDefinition = Boolean.TRUE.equals(ignoreDefinition);
            importAlias = null;
        }

        @Override
        public J visit(@Nullable Tree tree, ExecutionContext ctx) {
            if (tree instanceof JavaSourceFile) {
                JavaSourceFile cu = (JavaSourceFile) tree;
                keepsDefinition = ignoreDefinition ||
                        declaresNestedTypeThatStays(cu, originalType.getFullyQualifiedName(), newFullyQualifiedTypeName);
                if (!keepsDefinition) {
                    JavaType.FullyQualified fq = TypeUtils.asFullyQualified(targetType);
                    if (fq != null) {
                        ChangeClassDefinition changeClassDefinition = new ChangeClassDefinition(originalType.getFullyQualifiedName(), fq.getFullyQualifiedName());
                        JavaSourceFile changed = (JavaSourceFile) changeClassDefinition.visitNonNull(cu, ctx);
                        String oldPackage = packageName(cu);
                        String newPackage = packageName(changed);
                        if (!oldPackage.equals(newPackage)) {
                            // the other types the file declares move to its new package too
                            for (J.ClassDeclaration other : changed.getClasses()) {
                                JavaType.FullyQualified otherType = other.getType();
                                if (otherType != null && oldPackage.equals(otherType.getPackageName())) {
                                    changed = (JavaSourceFile) new JavaChangeTypeVisitor(otherType.getFullyQualifiedName(),
                                            (newPackage.isEmpty() ? "" : newPackage + ".") + otherType.getClassName(), false)
                                            .visitNonNull(changed, ctx);
                                }
                            }
                        }
                        cu = changed;
                    }
                }
                return super.visit(cu, ctx);
            }
            return super.visit(tree, ctx);
        }

        @Override
        public J visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
            if (classDecl.getType() != null) {
                declaredClassNames.add(classDecl.getType().getFullyQualifiedName().replace('$', '.'));
            }
            if (isKeptDefinition(classDecl)) {
                // a definition that stays is left untouched, with its references to itself and the types of its members
                return classDecl;
            }
            return super.visitClassDeclaration(classDecl, ctx);
        }

        private boolean isKeptDefinition(J.ClassDeclaration classDecl) {
            return keepsDefinition && TypeUtils.isOfClassType(classDecl.getType(), originalType.getFullyQualifiedName());
        }

        @Override
        public J visitImport(J.Import import_, ExecutionContext ctx) {
            // An alias on the original type has to be reused for the target import, or references to
            // it stop resolving. Imports nested in a block (a function body, an `if TYPE_CHECKING:`)
            // bind a narrower scope than the file-level import that would be added in their place,
            // so only file-level ones are considered.
            if (import_.getAlias() != null && getCursor().firstEnclosing(J.Block.class) == null &&
                    hasSameFQN(import_, originalType)) {
                importAlias = import_.getAlias();
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

        /**
         * A nested target is written through its outermost class, the only one that is imported. An alias names the
         * target itself.
         */
        private void addImport(JavaType.FullyQualified target) {
            if (importAlias != null) {
                maybeAddImport(target.getPackageName(), target.getClassName(), null, importAlias.getSimpleName(), true);
            }
            JavaType.FullyQualified outermost = getTopLevelClassName(target);
            maybeAddImport(outermost.getPackageName(), outermost.getClassName(), null, null, true);
        }

        @Override
        public @Nullable J postVisit(J tree, ExecutionContext ctx) {
            J j = super.postVisit(tree, ctx);
            if (j instanceof J.ClassDeclaration && isKeptDefinition((J.ClassDeclaration) j)) {
                // a definition that stays keeps its type, as visitClassDeclaration leaves it untouched
                return j;
            }
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
            } else if (j instanceof MethodCall) {
                MethodCall call = (MethodCall) j;
                j = (J) call.withMethodType(updateType(call.getMethodType()));
            } else if (tree instanceof TypedTree) {
                j = ((TypedTree) tree).withType(updateType(((TypedTree) tree).getType()));
            } else if (tree instanceof JavaSourceFile) {
                JavaSourceFile sf = (JavaSourceFile) tree;
                if (targetType instanceof JavaType.FullyQualified) {
                    // The members a static star import of the original type brings in are now those of the target type,
                    // so it is changed here already. Otherwise removing an import of the original type below would also
                    // remove the static star import, as none of the members of the original type are used anymore.
                    // Where it does, it also removes a static import of a single member, which is added again for the
                    // target.
                    sf = sf.withImports(ListUtils.map(sf.getImports(), i -> {
                        if (!i.isStatic() ||
                                !TypeUtils.fullyQualifiedNamesAreEqual(i.getTypeName(), originalType.getFullyQualifiedName())) {
                            return i;
                        }
                        if (!"*".equals(i.getQualid().getSimpleName())) {
                            // AddImport recognises a static import of a nested type only by its `$` name
                            maybeAddImport(((JavaType.FullyQualified) targetType).getFullyQualifiedName(),
                                    i.getQualid().getSimpleName());
                            return i;
                        }
                        return visitImportInPlace(i, ctx);
                    }));
                    for (J.Import anImport : sf.getImports()) {
                        if (anImport.isStatic()) {
                            continue;
                        }

                        if ("*".equals(anImport.getClassName()) &&
                                anImport.getPackageName().equals(originalType.getPackageName()) &&
                                !originalType.getPackageName().equals(((JavaType.FullyQualified) targetType).getPackageName())) {
                            sf = (JavaSourceFile) new RemoveImport<ExecutionContext>(originalType.getFullyQualifiedName()).visitNonNull(sf, ctx, getCursor().getParentOrThrow());
                        } else {
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
                }

                JavaType.FullyQualified fullyQualifiedTarget = TypeUtils.asFullyQualified(targetType);
                if (fullyQualifiedTarget != null) {
                    if (!declaredClassNames.contains(getTopLevelClassName(fullyQualifiedTarget).getFullyQualifiedName().replace('$', '.')) &&
                            hasNoConflictingName(sf) && !"java.lang".equals(fullyQualifiedTarget.getPackageName())) {
                        addImport(fullyQualifiedTarget);
                    }
                }

                if (targetType instanceof JavaType.FullyQualified && service(ImportService.class).usesStatementBasedImports()) {
                    // Queued after the addition so the import service sees the name already bound by
                    // the new import, which is what makes the old one safe to retire.
                    maybeRemoveImport(originalType.getFullyQualifiedName());
                }
                j = sf.withImports(ListUtils.map(sf.getImports(), i -> visitImportInPlace(i, ctx)));

                // If the new type is covered by a star import and another star import
                // provides a type with the same simple name, add an explicit import
                if (fullyQualifiedTarget != null) {
                    j = maybeAddExplicitImportForAmbiguity((JavaSourceFile) j, fullyQualifiedTarget, ctx);
                }
            }

            return j;
        }

        @Override
        public J visitFieldAccess(J.FieldAccess fieldAccess, ExecutionContext ctx) {
            if (fieldAccess.getName().getFieldType() != null) {
                // a field can share the name of its type, so the text of a read can look like a type reference
                return super.visitFieldAccess(fieldAccess, ctx);
            }
            if (fieldAccess.isFullyQualifiedClassReference(originalType.getFullyQualifiedName())) {
                if (targetType instanceof JavaType.FullyQualified) {
                    JavaType.FullyQualified fqTarget = (JavaType.FullyQualified) targetType;
                    if (hasParameterizedTarget(fieldAccess)) {
                        if (fqTarget.getOwningClass() != null) {
                            J.FieldAccess fa = updateParameterizedFieldAccess(fieldAccess, originalType, fqTarget, ctx);
                            if (getCursor().firstEnclosing(J.Import.class) == null &&
                                    getCursor().firstEnclosing(Javadoc.class) == null) {
                                doAfterVisit(ShortenFullyQualifiedTypeReferences.modifyOnly(fa));
                            }
                            return fa;
                        }
                        maybeRemoveImport(originalType.getOwningClass());
                    }
                    Expression qualified = qualifiedName(fqTarget, fieldAccess.getPrefix());
                    if (!(qualified instanceof J.FieldAccess)) {
                        // a type in the default package has no qualifier to shorten, and no import can name it
                        return getCursor().firstEnclosing(J.Import.class) == null ? qualified : fieldAccess;
                    }
                    if (getCursor().firstEnclosing(J.Import.class) == null &&
                            getCursor().firstEnclosing(Javadoc.class) == null) {
                        doAfterVisit(ShortenFullyQualifiedTypeReferences.modifyOnly(qualified));
                    }
                    return qualified;
                } else if (targetType instanceof JavaType.Primitive) {
                    return new J.Primitive(
                            fieldAccess.getId(),
                            fieldAccess.getPrefix(),
                            Markers.EMPTY,
                            (JavaType.Primitive) targetType
                    );
                }
            } else if (fieldAccess.getType() == null || fieldAccess.getType() instanceof JavaType.Unknown ||
                       TypeUtils.isOfClassType(fieldAccess.getType(), originalType.getFullyQualifiedName())) {
                // a name qualified by its outer class, such as `Map.Entry`, which another outer class of the same
                // simple name can declare too, so where it has a type, that has to be the original
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
                    JavaType.FullyQualified target = (JavaType.FullyQualified) targetType;
                    return name(target.getClassName(), target, fieldAccess.getPrefix());
                }
            }
            return super.visitFieldAccess(fieldAccess, ctx);
        }

        @Override
        public J visitIdentifier(J.Identifier ident, ExecutionContext ctx) {
            // if the ident's type is equal to the type we're looking for, and the classname of the type we're looking for is equal to the ident's string representation
            // Then transform it, otherwise leave it alone
            if (TypeUtils.isOfClassType(ident.getType(), originalType.getFullyQualifiedName())) {
                String className = originalType.getClassName();
                JavaType.FullyQualified iType = TypeUtils.asFullyQualified(ident.getType());
                if (iType != null && iType.getOwningClass() != null) {
                    className = originalType.getFullyQualifiedName().substring(iType.getOwningClass().getFullyQualifiedName().length() + 1);
                }

                JavaSourceFile sf = getCursor().firstEnclosing(JavaSourceFile.class);
                Object parent = getCursor().getParentTreeCursor().getValue();
                // an identifier keeps its annotations, which a new qualified name or primitive would drop
                boolean annotated = !ident.getAnnotations().isEmpty();
                // a variable of the type can share its name and isn't a reference to it. An enum constant's name has
                // no field type.
                if (ident.getFieldType() == null && !(parent instanceof J.EnumValue) &&
                    ident.getSimpleName().equals(className)) {
                    if (isDeclarationName(parent, ident)) {
                        // a declaration's own name is a simple name, even where a reference would be qualified
                        if (targetType instanceof JavaType.FullyQualified) {
                            String name = ((JavaType.FullyQualified) targetType).getClassName();
                            ident = ident.withSimpleName(name.substring(name.lastIndexOf('.') + 1));
                        } else if (targetType instanceof JavaType.Primitive) {
                            ident = ident.withSimpleName(((JavaType.Primitive) targetType).getKeyword());
                        }
                    } else if (targetType instanceof JavaType.FullyQualified) {
                        if (((JavaType.FullyQualified) targetType).getOwningClass() != null) {
                            JavaType.FullyQualified target = (JavaType.FullyQualified) targetType;
                            return hasNoConflictingName(sf) || annotated ?
                                    name(target.getClassName(), target, ident.getPrefix()) :
                                    qualifiedName(target, ident.getPrefix());
                        } else {
                            if (hasNoConflictingName(sf)) {
                                ident = ident.withSimpleName(((JavaType.FullyQualified) targetType).getClassName());
                            } else if (annotated) {
                                ident = ident.withSimpleName(((JavaType.FullyQualified) targetType).getFullyQualifiedName());
                            } else {
                                return FullyQualifyTypeReference.qualifiedName((JavaType.FullyQualified) targetType,
                                        ident.getPrefix(), emptyList());
                            }
                        }
                    } else if (targetType instanceof JavaType.Primitive) {
                        // the parser makes `String` an identifier, not a primitive
                        if (!annotated && targetType != JavaType.Primitive.String) {
                            return new J.Primitive(ident.getId(), ident.getPrefix(), ident.getMarkers(),
                                    (JavaType.Primitive) targetType);
                        }
                        ident = ident.withSimpleName(((JavaType.Primitive) targetType).getKeyword());
                    }
                }
            }
            ident = ident.withType(updateType(ident.getType()));
            return visitAndCast(ident, ctx, super::visitIdentifier);
        }

        private static boolean isDeclarationName(Object parent, J.Identifier ident) {
            return parent instanceof J.ClassDeclaration && ((J.ClassDeclaration) parent).getName().getId().equals(ident.getId()) ||
                   parent instanceof J.MethodDeclaration && ((J.MethodDeclaration) parent).getName().getId().equals(ident.getId());
        }

        private static String packageName(JavaSourceFile cu) {
            J.Package pkg = cu.getPackageDeclaration();
            return pkg == null ? "" : PackageNameUtils.getPackageName(pkg);
        }

        private static boolean hasParameterizedTarget(J.FieldAccess fieldAccess) {
            Expression target = fieldAccess.getTarget();
            while (target instanceof J.FieldAccess) {
                target = ((J.FieldAccess) target).getTarget();
            }
            return target instanceof J.ParameterizedType;
        }

        private J.FieldAccess updateParameterizedFieldAccess(
                J.FieldAccess fieldAccess,
                JavaType.FullyQualified origType,
                JavaType.FullyQualified targetFq,
                ExecutionContext ctx
        ) {
            JavaType.FullyQualified origOwner = origType.getOwningClass() != null ? origType.getOwningClass() : origType;
            JavaType.FullyQualified targetOwner = targetFq.getOwningClass();
            assert targetOwner != null;

            if (!TypeUtils.fullyQualifiedNamesAreEqual(origOwner.getFullyQualifiedName(), targetOwner.getFullyQualifiedName())) {
                maybeRemoveImport(origOwner);
            }

            Expression target = fieldAccess.getTarget();
            if (target instanceof J.ParameterizedType) {
                J.ParameterizedType pt = (J.ParameterizedType) visitAndCast((J.ParameterizedType) target, ctx, super::visitParameterizedType);
                NameTree clazz = pt.getClazz();
                if (clazz instanceof J.FieldAccess && hasParameterizedTarget((J.FieldAccess) clazz) &&
                        targetOwner.getOwningClass() != null) {
                    clazz = updateParameterizedFieldAccess((J.FieldAccess) clazz, origOwner, targetOwner, ctx);
                } else {
                    clazz = (NameTree) qualifiedName(targetOwner, clazz.getPrefix());
                }
                JavaType ptType = updateType(pt.getType());
                if (ptType instanceof JavaType.Parameterized) {
                    ptType = ((JavaType.Parameterized) ptType).withType(targetOwner);
                    types.replaceWith(pt.getType() != null ? pt.getType() : ptType, ptType);
                }
                target = pt.withClazz(clazz).withType(ptType);
            } else if (target instanceof J.FieldAccess && hasParameterizedTarget((J.FieldAccess) target) &&
                    targetOwner.getOwningClass() != null) {
                target = updateParameterizedFieldAccess((J.FieldAccess) target, origOwner, targetOwner, ctx);
            }

            JavaType updatedFieldAccessType = updateType(fieldAccess.getType());
            if (updatedFieldAccessType instanceof JavaType.Parameterized) {
                updatedFieldAccessType = ((JavaType.Parameterized) updatedFieldAccessType).withType(targetFq);
            } else if (TypeUtils.isOfClassType(fieldAccess.getType(), origType.getFullyQualifiedName())) {
                updatedFieldAccessType = targetFq;
            }
            if (fieldAccess.getType() != null && updatedFieldAccessType != null) {
                types.replaceWith(fieldAccess.getType(), updatedFieldAccessType);
            }

            String newSimpleName = targetFq.getFullyQualifiedName().substring(targetOwner.getFullyQualifiedName().length() + 1);
            return fieldAccess
                    .withTarget(target)
                    .withName(fieldAccess.getName().withSimpleName(newSimpleName).withType(targetFq))
                    .withType(updatedFieldAccessType);
        }

        /**
         * The fully qualified name of a class. In an import or Javadoc its names have no types. AddImport takes a
         * typed name in Javadoc for a use of the class that needs an import.
         */
        private Expression qualifiedName(JavaType.FullyQualified type, Space prefix) {
            Expression qualified = name(type.getFullyQualifiedName().replace('$', '.'), type, prefix);
            if (qualified instanceof J.FieldAccess && (getCursor().firstEnclosing(J.Import.class) != null ||
                    getCursor().firstEnclosing(Javadoc.class) != null)) {
                return withoutNameTypes((J.FieldAccess) qualified);
            }
            return qualified;
        }

        private static J.FieldAccess withoutNameTypes(J.FieldAccess qualified) {
            J.FieldAccess fa = qualified.withName(qualified.getName().withType(null));
            return fa.getTarget() instanceof J.FieldAccess ?
                    fa.withTarget(withoutNameTypes((J.FieldAccess) fa.getTarget())) : fa;
        }

        /**
         * The dotted name of a class. The last segment is the class and those before it are its outer classes, as
         * far as it has them, and then its package.
         */
        private static Expression name(String dotted, JavaType.FullyQualified type, Space prefix) {
            String[] segments = dotted.split("\\.");
            JavaType.FullyQualified[] types = new JavaType.FullyQualified[segments.length];
            JavaType.FullyQualified segmentType = type;
            for (int i = segments.length - 1; i >= 0 && segmentType != null; i--) {
                types[i] = segmentType;
                segmentType = segmentType.getOwningClass();
            }
            Expression name = new J.Identifier(Tree.randomId(), Space.EMPTY, Markers.EMPTY, emptyList(), segments[0],
                    types[0], null);
            for (int i = 1; i < segments.length; i++) {
                J.Identifier segment = new J.Identifier(Tree.randomId(), Space.EMPTY, Markers.EMPTY, emptyList(),
                        segments[i], types[i], null);
                name = new J.FieldAccess(Tree.randomId(), Space.EMPTY, Markers.EMPTY, name, JLeftPadded.build(segment),
                        types[i]);
            }
            return name.withPrefix(prefix);
        }

        private @Nullable JavaType updateType(@Nullable JavaType oldType) {
            return types.replace(oldType);
        }

        private JavaType.@Nullable Method updateType(JavaType.@Nullable Method oldMethodType) {
            return types.replace(oldMethodType);
        }

        private JavaType.@Nullable FullyQualified replacement(JavaType.FullyQualified fq) {
            if (isTargetFullyQualifiedType(fq)) {
                return (JavaType.FullyQualified) targetType;
            }
            return isNestedTypeOfOriginal(fq) ? updateNestedType(fq) : null;
        }

        private boolean isTargetFullyQualifiedType(JavaType.@Nullable FullyQualified fq) {
            return fq != null && TypeUtils.isOfClassType(fq, originalType.getFullyQualifiedName()) && targetType instanceof JavaType.FullyQualified;
        }

        private boolean isNestedTypeOfOriginal(JavaType.@Nullable FullyQualified fq) {
            if (fq == null || !(targetType instanceof JavaType.FullyQualified)) {
                return false;
            }
            return isNestedIn(fq.getFullyQualifiedName(), originalType.getFullyQualifiedName());
        }

        /**
         * Whether {@code fullyQualifiedName}, such as {@code a.Outer$Inner$Deep}, names a type nested in {@code outer},
         * which can name its own outer types with dots, as {@code a.Outer.Inner}.
         */
        private static boolean isNestedIn(String fullyQualifiedName, String outer) {
            int length = outer.length();
            return fullyQualifiedName.length() > length && fullyQualifiedName.charAt(length) == '$' &&
                   TypeUtils.fullyQualifiedNamesAreEqual(fullyQualifiedName.substring(0, length), outer);
        }

        private JavaType.FullyQualified updateNestedType(JavaType.FullyQualified nestedType) {
            String nestedFqn = nestedType.getFullyQualifiedName();
            String originalFqn = originalType.getFullyQualifiedName();
            String targetFqn = ((JavaType.FullyQualified) targetType).getFullyQualifiedName();
            // Replace the original type prefix with the target type prefix
            String newNestedFqn = targetFqn + nestedFqn.substring(originalFqn.length());
            return JavaType.ShallowClass.build(newNestedFqn).withKind(nestedType.getKind());
        }

        /**
         * When the new type is provided by a star import and another star import, or {@code java.lang},
         * provides a type with the same simple name, add an explicit import to disambiguate.
         */
        private JavaSourceFile maybeAddExplicitImportForAmbiguity(JavaSourceFile sf, JavaType.FullyQualified newType, ExecutionContext ctx) {
            String newPkg = newType.getPackageName();
            String simpleName = newType.getClassName();

            // Check if the new type is covered by a star import
            boolean coveredByStar = false;
            Set<String> otherStarPackages = new LinkedHashSet<>();
            for (J.Import anImport : sf.getImports()) {
                if (anImport.isStatic() || !"*".equals(anImport.getQualid().getSimpleName())) {
                    // Check if there's already an explicit import for this type
                    if (!anImport.isStatic() && anImport.getTypeName().replace('$', '.').equals(newType.getFullyQualifiedName())) {
                        return sf; // Already has explicit import, no ambiguity possible
                    }
                    continue;
                }
                if (anImport.getPackageName().equals(newPkg)) {
                    coveredByStar = true;
                } else {
                    otherStarPackages.add(anImport.getPackageName());
                }
            }

            if (!coveredByStar) {
                return sf;
            }

            // Check if any other star-imported package has a type with the same simple name
            boolean ambiguous;
            if (!otherStarPackages.isEmpty() && JavaSourceSet.isDirty(ctx, sf)) {
                // An earlier dependency mutation in this run means the classpath cannot be trusted to
                // prove non-ambiguity; fall back to the safe path and add the explicit import.
                ambiguous = true;
            } else {
                Optional<JavaSourceSet> sourceSet = sf.getMarkers().findFirst(JavaSourceSet.class);
                if (!sourceSet.isPresent()) {
                    return sf;
                }
                // Every compilation unit imports java.lang on demand. No dependency change alters it.
                if (!"java.lang".equals(newPkg)) {
                    otherStarPackages.add("java.lang");
                }
                ambiguous = false;
                for (JavaType.FullyQualified fq : sourceSet.get().getClasspath()) {
                    if (fq.getClassName().equals(simpleName) && otherStarPackages.contains(fq.getPackageName())) {
                        ambiguous = true;
                        break;
                    }
                }
            }

            if (!ambiguous) {
                return sf;
            }

            // Add an explicit import to resolve the ambiguity
            J.Import explicitImport = new J.Import(Tree.randomId(),
                    Space.EMPTY,
                    Markers.EMPTY,
                    new JLeftPadded<>(Space.EMPTY, false, Markers.EMPTY),
                    TypeTree.build(newType.getFullyQualifiedName()).withPrefix(Space.SINGLE_SPACE),
                    null);

            List<JRightPadded<J.Import>> imports = new ArrayList<>(sf.getPadding().getImports());
            // Insert the explicit import right before the star import for its package
            for (int i = 0; i < imports.size(); i++) {
                J.Import imp = imports.get(i).getElement();
                if (!imp.isStatic() && "*".equals(imp.getQualid().getSimpleName()) && imp.getPackageName().equals(newPkg)) {
                    // Give the explicit import the star import's prefix (which has the leading newline)
                    JRightPadded<J.Import> padded = JRightPadded.build(explicitImport.withPrefix(imp.getPrefix()));
                    // Give the star import a fresh newline prefix
                    imports.set(i, imports.get(i).map(starImp -> starImp.withPrefix(Space.format("\n"))));
                    imports.add(i, padded);
                    break;
                }
            }
            return sf.getPadding().withImports(imports);
        }

        private J.Import visitImportInPlace(J.Import anImport, ExecutionContext ctx) {
            Cursor cursor = getCursor();
            setCursor(new Cursor(cursor, anImport));
            try {
                return visitAndCast(anImport, ctx, super::visitImport);
            } finally {
                setCursor(cursor);
            }
        }

        private boolean hasNoConflictingName(@Nullable JavaSourceFile sf) {
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
            Set<String> otherTypeNames = otherTypeNames(sf, newType);
            // a nested target is written through its outermost class, so that name has to be free as well
            return !otherTypeNames.contains(newType.getClassName()) &&
                   !otherTypeNames.contains(getTopLevelClassName(newType).getClassName());
        }

        /**
         * The simple names that the file declares for types other than the target and its outermost class, or writes
         * unqualified for types other than those and the original. Uses of the original are renamed, while a
         * declaration of it stays. Shortening the target to one of these names would bind that name to two types.
         */
        private Set<String> otherTypeNames(JavaSourceFile sf, JavaType.FullyQualified newType) {
            if (otherTypeNames == null) {
                JavaType.FullyQualified outermost = getTopLevelClassName(newType);
                Map<String, String> importedTypeNames = new HashMap<>();
                for (J.Import anImport : sf.getImports()) {
                    if (!anImport.isStatic() && anImport.getAlias() == null &&
                            !"*".equals(anImport.getQualid().getSimpleName())) {
                        importedTypeNames.put(anImport.getQualid().getSimpleName(), anImport.getTypeName());
                    }
                }
                otherTypeNames = new JavaIsoVisitor<Set<String>>() {
                    private boolean isTarget(@Nullable JavaType type) {
                        return TypeUtils.isOfClassType(type, newType.getFullyQualifiedName()) ||
                               TypeUtils.isOfClassType(type, outermost.getFullyQualifiedName());
                    }

                    @Override
                    public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, Set<String> names) {
                        if (!isTarget(classDecl.getType())) {
                            names.add(classDecl.getSimpleName());
                        }
                        return super.visitClassDeclaration(classDecl, names);
                    }

                    @Override
                    public J.TypeParameter visitTypeParameter(J.TypeParameter typeParam, Set<String> names) {
                        if (typeParam.getName() instanceof J.Identifier) {
                            names.add(((J.Identifier) typeParam.getName()).getSimpleName());
                        }
                        return super.visitTypeParameter(typeParam, names);
                    }

                    @Override
                    public J.Identifier visitIdentifier(J.Identifier identifier, Set<String> names) {
                        JavaType type = identifier.getType();
                        String importedTypeName = importedTypeNames.get(identifier.getSimpleName());
                        if (type instanceof JavaType.Unknown && importedTypeName != null) {
                            // a name of unknown type refers to the type the file imports by that name
                            type = JavaType.ShallowClass.build(importedTypeName);
                        }
                        if (identifier.getFieldType() == null && type instanceof JavaType.FullyQualified && !isTarget(type) &&
                                !TypeUtils.isOfClassType(type, originalType.getFullyQualifiedName())) {
                            names.add(identifier.getSimpleName());
                        }
                        return super.visitIdentifier(identifier, names);
                    }

                    @Override
                    public J.FieldAccess visitFieldAccess(J.FieldAccess fieldAccess, Set<String> names) {
                        visit(fieldAccess.getTarget(), names);
                        return fieldAccess;
                    }
                }.reduce(sf, new HashSet<>());
            }
            return otherTypeNames;
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    private static class ReferenceChangeTypeVisitor extends TreeVisitor<Tree, ExecutionContext> {
        Map<Tree, List<Reference>> matches;
        Reference.Renamer renamer;

        @Override
        public Tree postVisit(Tree tree, ExecutionContext ctx) {
            List<Reference> refs = matches.get(tree);
            if (refs != null) {
                for (Reference ref : refs) {
                    if (ref.supportsRename()) {
                        tree = ref.rename(renamer, new Cursor(getCursor().getParent(), tree), ctx);
                    }
                }
            }
            return tree;
        }
    }

    private static class ChangeClassDefinition extends JavaIsoVisitor<ExecutionContext> {
        private final JavaType.Class originalType;
        private final JavaType.Class targetType;
        private final MethodMatcher originalConstructor;
        private final TypeReplacer types = new TypeReplacer(this::replacement);

        private ChangeClassDefinition(String oldFullyQualifiedTypeName, String newFullyQualifiedTypeName) {
            this.originalType = JavaType.ShallowClass.build(oldFullyQualifiedTypeName);
            this.targetType = JavaType.ShallowClass.build(newFullyQualifiedTypeName);
            this.originalConstructor = new MethodMatcher(oldFullyQualifiedTypeName + " <constructor>(..)");
        }

        @Override
        public J visit(@Nullable Tree tree, ExecutionContext ctx) {
            if (tree instanceof JavaSourceFile) {
                JavaSourceFile cu = (JavaSourceFile) tree;
                boolean declaresOriginal = false;
                for (J.ClassDeclaration declaration : cu.getClasses()) {
                    // Check the class name instead of source path, as it might differ.
                    // Non-Java (e.g. Python) class declarations may lack type attribution.
                    JavaType.FullyQualified declarationType = declaration.getType();
                    if (declarationType == null) {
                        continue;
                    }
                    String fqn = declarationType.getFullyQualifiedName();
                    if (fqn.equals(originalType.getFullyQualifiedName())) {
                        getCursor().putMessage("UPDATE_PACKAGE", true);
                        declaresOriginal = true;
                        break;
                    }
                }

                String oldPath = cu.getSourcePath().toString().replace('\\', '/');
                String newPath = movedPath(oldPath);
                if (newPath != null && updatePath(cu, oldPath, newPath)) {
                    cu = cu.withSourcePath(Paths.get(newPath));
                }
                J visited = super.visit(cu, ctx);
                if (declaresOriginal && visited instanceof J.CompilationUnit &&
                    ((J.CompilationUnit) visited).getPackageDeclaration() == null && !targetType.getPackageName().isEmpty()) {
                    visited = addPackage((J.CompilationUnit) visited, targetType.getPackageName());
                }
                return visited;
            }
            return super.visit(tree, ctx);
        }

        /**
         * The path of a file whose name and directories end in the old type's top-level class and package, with
         * those replaced by the target's.
         */
        private @Nullable String movedPath(String oldPath) {
            String oldTypePath = fqnToPath(originalType.getFullyQualifiedName());
            int extension = oldPath.lastIndexOf('.');
            String withoutExtension = extension > oldPath.lastIndexOf('/') ? oldPath.substring(0, extension) : oldPath;
            if (!withoutExtension.equals(oldTypePath) && !withoutExtension.endsWith("/" + oldTypePath)) {
                return null;
            }
            return withoutExtension.substring(0, withoutExtension.length() - oldTypePath.length()) +
                   fqnToPath(targetType.getFullyQualifiedName()) + oldPath.substring(withoutExtension.length());
        }

        private String fqnToPath(String fullyQualifiedName) {
            int index = fullyQualifiedName.indexOf("$");
            String topLevelClassName = index == -1 ? fullyQualifiedName : fullyQualifiedName.substring(0, index);
            return topLevelClassName.replace('.', '/');
        }

        /**
         * Adds the package above the first import or type. The comments in front of that, such as a license header, go
         * above the package, except for a type's Javadoc and what follows it.
         */
        private static J.CompilationUnit addPackage(J.CompilationUnit cu, String packageName) {
            Space prefix = cu.getImports().isEmpty() ? cu.getClasses().get(0).getPrefix() : cu.getImports().get(0).getPrefix();
            List<Comment> comments = prefix.getComments();
            int javadoc = 0;
            while (javadoc < comments.size() && !(comments.get(javadoc) instanceof Javadoc.DocComment)) {
                javadoc++;
            }
            Space rest = Space.build("\n\n", new ArrayList<>(comments.subList(javadoc, comments.size())));
            cu = cu.getImports().isEmpty() ?
                    cu.withClasses(ListUtils.mapFirst(cu.getClasses(), cd -> cd.withPrefix(rest))) :
                    cu.withImports(ListUtils.mapFirst(cu.getImports(), i -> i.withPrefix(rest)));
            return cu.withPackageDeclaration(new J.Package(Tree.randomId(),
                    prefix.withComments(new ArrayList<>(comments.subList(0, javadoc))), Markers.EMPTY,
                    TypeTree.build(packageName).withPrefix(Space.SINGLE_SPACE), emptyList()));
        }

        private boolean updatePath(JavaSourceFile sf, String oldPath, String newPath) {
            return !oldPath.equals(newPath) && sf.getClasses().stream()
                    .anyMatch(o -> !o.hasModifier(J.Modifier.Type.Private) &&
                            o.getType() != null && !o.getType().getFullyQualifiedName().contains("$") &&
                            TypeUtils.isOfClassType(o.getType(), originalType.getFullyQualifiedName()));
        }

        @Override
        public J.@Nullable Package visitPackage(J.Package pkg, ExecutionContext ctx) {
            Boolean updatePackage = getCursor().pollNearestMessage("UPDATE_PACKAGE");
            if (updatePackage != null && updatePackage) {
                String original = PackageNameUtils.getPackageName(pkg);
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
            return super.visitPackage(pkg, ctx);
        }

        @Override
        public J.Import visitImport(J.Import _import, ExecutionContext ctx) {
            Boolean updatePrefix = getCursor().pollNearestMessage("UPDATE_PREFIX");
            if (updatePrefix != null && updatePrefix) {
                _import = _import.withPrefix(_import.getPrefix().withWhitespace(""));
            }
            return super.visitImport(_import, ctx);
        }

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
            // polled before the body, as a type nested in this one doesn't follow the removed package
            Boolean updatePrefix = getCursor().pollNearestMessage("UPDATE_PREFIX");
            J.ClassDeclaration cd = super.visitClassDeclaration(classDecl, ctx);
            if (updatePrefix != null && updatePrefix) {
                cd = cd.withPrefix(cd.getPrefix().withWhitespace(""));
            }

            if (TypeUtils.isOfClassType(classDecl.getType(), originalType.getFullyQualifiedName())) {
                String newClassName = getNewClassName(targetType);
                cd = cd.withName(cd.getName().withSimpleName(newClassName));
                // the target type is built from its name alone, which makes it a class
                cd = cd.withType(targetType.withKind(classDecl.getType().getKind()));
            }
            return cd;
        }

        @Override
        public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
            if (method.isConstructor() && originalConstructor.matches(method.getMethodType())) {
                method = method.withName(method.getName().withSimpleName(getNewClassName(targetType)));
                method = method.withMethodType(types.replace(method.getMethodType()));
            }
            return super.visitMethodDeclaration(method, ctx);
        }

        private String getNewClassName(JavaType.FullyQualified fq) {
            return fq.getOwningClass() == null ? fq.getClassName() :
                    fq.getFullyQualifiedName().substring(fq.getOwningClass().getFullyQualifiedName().length() + 1);
        }

        private JavaType.@Nullable FullyQualified replacement(JavaType.FullyQualified fq) {
            String fqn = fq.getFullyQualifiedName();
            String originalFqn = originalType.getFullyQualifiedName();
            if (TypeUtils.isOfClassType(fq, originalFqn)) {
                return targetType;
            }
            // a type nested in the original follows it
            if (!JavaChangeTypeVisitor.isNestedIn(fqn, originalFqn)) {
                return null;
            }
            return JavaType.ShallowClass.build(targetType.getFullyQualifiedName() + fqn.substring(originalFqn.length()))
                    .withKind(fq.getKind());
        }
    }

    /**
     * Cheap pre-filter for {@link #containsClassDefinition}: a Java file can only declare a type
     * whose package equals the file's package declaration, so a file whose package is not a prefix
     * of the old type's name can skip the (potentially full-tree) definition scan entirely. This
     * invariant is Java-specific, so it is only applied to {@link J.CompilationUnit}; other
     * {@link JavaSourceFile} dialects (e.g. {@code K.CompilationUnit}, {@code G.CompilationUnit})
     * always fall through to the scan.
     *
     * @param normalizedOldName the old fully qualified type name with {@code $} normalized to {@code .}
     */
    // Looked up reflectively because recipe modules built against this ChangeType may run alongside an
    // older TypesInUse that predates the method, which would otherwise fail with NoSuchMethodError.
    private static final @Nullable Method HAS_DOC_REFERENCE = findHasDocReference();

    private static @Nullable Method findHasDocReference() {
        try {
            return TypesInUse.class.getMethod("hasDocReference", String.class);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private static boolean hasDocReference(TypesInUse typesInUse, String fullyQualifiedTypeName) {
        if (HAS_DOC_REFERENCE == null) {
            return false;
        }
        try {
            return (Boolean) HAS_DOC_REFERENCE.invoke(typesInUse, fullyQualifiedTypeName);
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    /**
     * Whether an import names something inside the type, such as a nested type, which the file can then use without
     * naming the type.
     */
    private static boolean importsMemberOf(JavaSourceFile sourceFile, String normalizedOldName) {
        for (J.Import anImport : sourceFile.getImports()) {
            for (Expression qualifier = anImport.getQualid().getTarget(); qualifier instanceof J.FieldAccess;
                 qualifier = ((J.FieldAccess) qualifier).getTarget()) {
                if (((J.FieldAccess) qualifier).isFullyQualifiedClassReference(normalizedOldName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean mayContainDefinition(JavaSourceFile sourceFile, String normalizedOldName) {
        if (!(sourceFile instanceof J.CompilationUnit)) {
            return true;
        }
        J.Package packageDeclaration = sourceFile.getPackageDeclaration();
        if (packageDeclaration == null) {
            // Default package: can't cheaply rule out a declaration here, so scan.
            return true;
        }
        // The declared type's package equals the file's, so the old name must begin with the file's
        // package. The prefix check never rejects a file that actually declares the type.
        return PackageNameUtils.isPrefixOf(packageDeclaration, normalizedOldName);
    }

    /**
     * Whether the source file declares the old type nested in an outer class that doesn't declare the new type. A
     * definition is renamed where it is declared, so that definition stays.
     */
    private static boolean declaresNestedTypeThatStays(JavaSourceFile sourceFile, String oldFullyQualifiedTypeName,
                                                       String newFullyQualifiedTypeName) {
        for (J.ClassDeclaration classDecl : sourceFile.getClasses()) {
            JavaType.FullyQualified outer = outerClassDeclaring(classDecl, oldFullyQualifiedTypeName);
            if (outer != null) {
                int lastSeparator = Math.max(newFullyQualifiedTypeName.lastIndexOf('.'), newFullyQualifiedTypeName.lastIndexOf('$'));
                return lastSeparator < 0 || !TypeUtils.fullyQualifiedNamesAreEqual(
                        newFullyQualifiedTypeName.substring(0, lastSeparator), outer.getFullyQualifiedName());
            }
        }
        return false;
    }

    private static JavaType.@Nullable FullyQualified outerClassDeclaring(J.ClassDeclaration classDecl, String fullyQualifiedTypeName) {
        for (Statement member : classDecl.getBody().getStatements()) {
            if (member instanceof J.ClassDeclaration) {
                J.ClassDeclaration nested = (J.ClassDeclaration) member;
                if (TypeUtils.isOfClassType(nested.getType(), fullyQualifiedTypeName)) {
                    return classDecl.getType();
                }
                JavaType.FullyQualified outer = outerClassDeclaring(nested, fullyQualifiedTypeName);
                if (outer != null) {
                    return outer;
                }
            }
        }
        return null;
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
