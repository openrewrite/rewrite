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
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.trait.Reference;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;

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
@EqualsAndHashCode(callSuper = false)
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
            required = false)
    @Nullable
    Boolean recursive;

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s` to `%s`", oldPackageName, newPackageName);
    }

    String displayName = "Rename package name";

    String description = "A recipe that will rename a package name in package statements, imports, and fully-qualified types.";

    @Override
    public Validated<Object> validate() {
        return Validated.none()
                .and(Validated.notBlank("oldPackageName", oldPackageName))
                .and(Validated.required("newPackageName", newPackageName));
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        TreeVisitor<?, ExecutionContext> condition = new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree preVisit(@Nullable Tree tree, ExecutionContext ctx) {
                stopAfterPreVisit();
                if (tree instanceof JavaSourceFile) {
                    JavaSourceFile cu = (JavaSourceFile) tree;
                    if (cu.getPackageDeclaration() != null) {
                        String original = cu.getPackageDeclaration().getExpression()
                                .printTrimmed(getCursor()).replaceAll("\\s", "");
                        if (original.startsWith(oldPackageName)) {
                            return SearchResult.found(cu);
                        }
                    }
                    boolean recursive = Boolean.TRUE.equals(ChangePackage.this.recursive);
                    String recursivePackageNamePrefix = oldPackageName + ".";
                    for (J.Import anImport : cu.getImports()) {
                        String importedPackage = anImport.getPackageName();
                        if (importedPackage.equals(oldPackageName) || recursive && importedPackage.startsWith(recursivePackageNamePrefix)) {
                            return SearchResult.found(cu);
                        }
                    }
                    for (JavaType type : cu.getTypesInUse().getTypesInUse()) {
                        if (type instanceof JavaType.FullyQualified) {
                            String packageName = ((JavaType.FullyQualified) type).getPackageName();
                            if (packageName.equals(oldPackageName) || recursive && packageName.startsWith(recursivePackageNamePrefix)) {
                                return SearchResult.found(cu);
                            }
                        }
                    }
                } else if (tree instanceof SourceFileWithReferences) {
                    SourceFileWithReferences cu = (SourceFileWithReferences) tree;
                    boolean recursive = Boolean.TRUE.equals(ChangePackage.this.recursive);
                    String recursivePackageNamePrefix = oldPackageName + ".";
                    for (Reference ref : cu.getReferences().getReferences()) {
                        if (ref.getValue().equals(oldPackageName) || recursive && ref.getValue().startsWith(recursivePackageNamePrefix)) {
                            return SearchResult.found(cu);
                        }
                    }
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
                if (tree instanceof JavaSourceFile) {
                    return new JavaChangePackageVisitor().visit(tree, ctx, requireNonNull(getCursor().getParent()));
                } else if (tree instanceof SourceFileWithReferences) {
                    SourceFileWithReferences sourceFile = (SourceFileWithReferences) tree;
                    SourceFileWithReferences.References references = sourceFile.getReferences();
                    boolean recursive = Boolean.TRUE.equals(ChangePackage.this.recursive);
                    PackageMatcher matcher = new PackageMatcher(oldPackageName, recursive);
                    Map<Tree, Reference> matches = new HashMap<>();
                    for (Reference ref : references.findMatches(matcher)) {
                        matches.put(ref.getTree(), ref);
                    }
                    return new ReferenceChangePackageVisitor(matches, matcher.createRenamer(newPackageName)).visit(tree, ctx, requireNonNull(getCursor().getParent()));
                }
                return tree;
            }
        });
    }

    private class JavaChangePackageVisitor extends JavaVisitor<ExecutionContext> {
        private static final String RENAME_TO_KEY = "renameTo";
        private static final String RENAME_FROM_KEY = "renameFrom";

        private final Map<JavaType, JavaType> oldNameToChangedType = new IdentityHashMap<>();
        private final JavaType.Class newPackageType = JavaType.ShallowClass.build(newPackageName);

        @Override
        public J visitFieldAccess(J.FieldAccess fieldAccess, ExecutionContext ctx) {
            J f = super.visitFieldAccess(fieldAccess, ctx);

            if (((J.FieldAccess) f).isFullyQualifiedClassReference(oldPackageName)) {
                Cursor parent = getCursor().getParent();
                if (parent != null &&
                    // Ensure the parent isn't a J.FieldAccess OR the parent doesn't match the target package name.
                    (!(parent.getValue() instanceof J.FieldAccess) ||
                     (!(((J.FieldAccess) parent.getValue()).isFullyQualifiedClassReference(newPackageName))))) {

                    f = TypeTree.build(((JavaType.FullyQualified) newPackageType).getFullyQualifiedName())
                            .withPrefix(f.getPrefix());
                }
            }
            return f;
        }

        @Override
        public J visitPackage(J.Package pkg, ExecutionContext ctx) {
            String original = pkg.getExpression().printTrimmed(getCursor()).replaceAll("\\s", "");
            getCursor().putMessageOnFirstEnclosing(JavaSourceFile.class, RENAME_FROM_KEY, original);

            pkg = pkg.withAnnotations(ListUtils.map(pkg.getAnnotations(), a -> visitAndCast(a, ctx)));

            if (original.equals(oldPackageName)) {
                getCursor().putMessageOnFirstEnclosing(JavaSourceFile.class, RENAME_TO_KEY, newPackageName);

                if (!newPackageName.isEmpty()) {
                    pkg = JavaTemplate.builder(newPackageName).contextSensitive().build().apply(getCursor(), pkg.getCoordinates().replace());
                } else {
                    // Covers unlikely scenario where the package is removed.
                    getCursor().putMessageOnFirstEnclosing(JavaSourceFile.class, "UPDATE_PREFIX", true);
                    pkg = null;
                }
            } else if (isTargetRecursivePackageName(original)) {
                String changingTo = getNewPackageName(original);
                getCursor().putMessageOnFirstEnclosing(JavaSourceFile.class, RENAME_TO_KEY, changingTo);
                pkg = JavaTemplate.builder(changingTo)
                        .contextSensitive()
                        .build()
                        .apply(getCursor(), pkg.getCoordinates().replace());
            }
            //noinspection ConstantConditions
            return pkg;
        }

        @Override
        public J visitImport(J.Import _import, ExecutionContext ctx) {
            // Polls message before calling super to change the prefix of the first import if applicable.
            Boolean updatePrefix = getCursor().pollNearestMessage("UPDATE_PREFIX");
            if (updatePrefix != null && updatePrefix) {
                _import = _import.withPrefix(Space.EMPTY);
            }
            return super.visitImport(_import, ctx);
        }

        @Override
        public J visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
            J c = super.visitClassDeclaration(classDecl, ctx);

            Boolean updatePrefix = getCursor().pollNearestMessage("UPDATE_PREFIX");
            if (updatePrefix != null && updatePrefix) {
                c = c.withPrefix(Space.EMPTY);
            }
            return c;
        }

        @Override
        public @Nullable JavaType visitType(@Nullable JavaType javaType, ExecutionContext ctx) {
            return updateType(javaType);
        }

        @Override
        public J postVisit(J tree, ExecutionContext ctx) {
            J j = super.postVisit(tree, ctx);
            if (j instanceof J.MethodDeclaration) {
                J.MethodDeclaration m = (J.MethodDeclaration) j;
                JavaType.Method mt = updateType(m.getMethodType());
                return m.withMethodType(mt).withName(m.getName().withType(mt));
            } else if (j instanceof J.MethodInvocation) {
                J.MethodInvocation m = (J.MethodInvocation) j;
                JavaType.Method mt = updateType(m.getMethodType());
                return m.withMethodType(mt).withName(m.getName().withType(mt));
            } else if (j instanceof J.NewClass) {
                J.NewClass n = (J.NewClass) j;
                return n.withConstructorType(updateType(n.getConstructorType()));
            } else if (j instanceof TypedTree) {
                return ((TypedTree) j).withType(updateType(((TypedTree) j).getType()));
            } else if (j instanceof JavaSourceFile) {
                JavaSourceFile sf = (JavaSourceFile) j;

                String changingTo = getCursor().getNearestMessage(RENAME_TO_KEY);
                if (changingTo != null) {
                    String path = ((SourceFile) sf).getSourcePath().toString().replace('\\', '/');
                    String changingFrom = getCursor().getMessage(RENAME_FROM_KEY);
                    assert changingFrom != null;
                    String oldPathPart = changingFrom.replace('.', '/');
                    String newPathPart = changingTo.replace('.', '/');
                    String newPath;
                    if (path.startsWith(oldPathPart)) {
                        newPath = newPathPart + path.substring(oldPathPart.length());
                    } else {
                        int dirIdx = path.indexOf('/');
                        String prefix = dirIdx == -1 ? null : path.substring(0, dirIdx);
                        String pathNoPrefix = dirIdx == -1 ? path : path.substring(dirIdx);
                        String newPath1 = pathNoPrefix.replaceFirst(
                                    oldPathPart,
                                    newPathPart
                                );
                        newPath = prefix == null ? newPath1 : prefix + newPath1;
                    }
                    sf = ((SourceFile) sf).withSourcePath(Paths.get(newPath));

                    for (J.Import anImport : sf.getImports()) {
                        if (anImport.getPackageName().equals(changingTo) && !anImport.isStatic()) {
                            // Skip removal for nested class imports - they still need the import even in the same package
                            JavaType.FullyQualified fq = TypeUtils.asFullyQualified(anImport.getQualid().getType());
                            if (fq != null && fq.getOwningClass() != null) {
                                continue;
                            }
                            sf = (JavaSourceFile) new RemoveImport<ExecutionContext>(anImport.getTypeName(), true)
                                    .visitNonNull(sf, ctx, getCursor().getParentTreeCursor());
                        }
                    }
                }

                j = sf;
            }
            //noinspection DataFlowIssue
            return j;
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
                        if (isTargetFullyQualifiedType(tpFq)) {
                            return updateType(tpFq);
                        }
                    }
                    return tp;
                }));

                if (isTargetFullyQualifiedType(pt)) {
                    //noinspection DataFlowIssue
                    pt = pt.withType((JavaType.FullyQualified) updateType(pt.getType()));
                }

                oldNameToChangedType.put(oldType, pt);
                return pt;
            } else if (oldType instanceof JavaType.FullyQualified) {
                JavaType.FullyQualified original = TypeUtils.asFullyQualified(oldType);
                if (isTargetFullyQualifiedType(original)) {
                    JavaType.FullyQualified fq = TypeUtils.asFullyQualified(JavaType.buildType(getNewPackageName(original.getPackageName()) + "." + original.getRawClassName()));
                    //noinspection DataFlowIssue
                    oldNameToChangedType.put(oldType, fq);
                    oldNameToChangedType.put(fq, fq);
                    return fq;
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
                    if (b instanceof JavaType.FullyQualified && isTargetFullyQualifiedType((JavaType.FullyQualified) b)) {
                        return updateType(b);
                    }
                    return b;
                }));

                oldNameToChangedType.put(oldType, gtv);
                oldNameToChangedType.put(gtv, gtv);
                return gtv;
            } else if (oldType instanceof JavaType.Variable) {
                JavaType.Variable variable = (JavaType.Variable) oldType;
                variable = variable.withOwner(updateType(variable.getOwner()));
                //noinspection DataFlowIssue
                variable = variable.withType(updateType(variable.getType()));
                oldNameToChangedType.put(oldType, variable);
                oldNameToChangedType.put(variable, variable);
                return variable;
            } else if (oldType instanceof JavaType.Array) {
                JavaType.Array array = (JavaType.Array) oldType;
                //noinspection DataFlowIssue
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
                //noinspection DataFlowIssue
                method = method.withDeclaringType((JavaType.FullyQualified) updateType(method.getDeclaringType()))
                        .withReturnType(updateType(method.getReturnType()))
                        .withParameterTypes(ListUtils.map(method.getParameterTypes(), this::updateType));
                oldNameToChangedType.put(oldMethodType, method);
                oldNameToChangedType.put(method, method);
                return method;
            }
            return null;
        }

        private String getNewPackageName(String packageName) {
            return (recursive == null || recursive) && !newPackageName.endsWith(packageName.substring(oldPackageName.length())) ?
                    newPackageName + packageName.substring(oldPackageName.length()) : newPackageName;
        }

        private boolean isTargetFullyQualifiedType(JavaType.@Nullable FullyQualified fq) {
            return fq != null &&
                   (fq.getPackageName().equals(oldPackageName) && !fq.getClassName().isEmpty() ||
                    isTargetRecursivePackageName(fq.getPackageName()));
        }

        private boolean isTargetRecursivePackageName(String packageName) {
            return (recursive == null || recursive) &&
                   packageName.startsWith(oldPackageName + ".") &&
                   !packageName.startsWith(newPackageName);
        }

    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    private static class ReferenceChangePackageVisitor extends TreeVisitor<Tree, ExecutionContext> {
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

}
