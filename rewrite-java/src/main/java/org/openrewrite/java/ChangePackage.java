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
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.*;

import java.nio.file.Paths;

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
                            .printTrimmed(getCursor()).replaceAll("\\s", "");
                    if (original.startsWith(oldPackageName)) {
                        return cu.withMarkers(cu.getMarkers().searchResult());
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

                for (J.Import anImport : c.getImports()) {
                    if (anImport.getPackageName().equals(newPackageName)) {
                        c = new RemoveImport<ExecutionContext>(anImport.getTypeName(),true).visitCompilationUnit(c, ctx);
                    }
                }
            }
            return c;
        }

        @Override
        public @Nullable J postVisit(J tree, ExecutionContext executionContext) {
            J j = super.postVisit(tree, executionContext);
            if (j instanceof TypedTree) {
                JavaType.FullyQualified fq = TypeUtils.asFullyQualified(((TypedTree) j).getType());
                if (fq != null && fq.getPackageName().equals(oldPackageName) && !fq.getClassName().isEmpty()) {
                    j = ((TypedTree) j).withType(buildNewType(fq.getClassName()));
                }
            }
            return j;
        }

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
            J.ClassDeclaration c = super.visitClassDeclaration(classDecl, ctx);

            String changingTo = getCursor().getNearestMessage(RENAME_TO_KEY);
            if (changingTo != null && classDecl.getType() != null) {
                JavaType.FullyQualified type = c.getType();
                if(type != null) {
                    c = c.withType(type.withFullyQualifiedName(changingTo + "." + c.getType().getClassName()));
                }
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
            }
            return f;
        }

        @Override
        public J.Package visitPackage(J.Package pkg, ExecutionContext context) {
            String original = pkg.getExpression().printTrimmed(getCursor()).replaceAll("\\s", "");
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

        private JavaType updateType(@Nullable JavaType type) {
            JavaType.GenericTypeVariable gtv = TypeUtils.asGeneric(type);
            if (gtv != null && gtv.getBound() != null
                    && gtv.getBound().getPackageName().equals(oldPackageName)) {
                return gtv.withBound(TypeUtils.asFullyQualified(buildNewType(gtv.getBound().getClassName())));
            }

            JavaType.FullyQualified fqt = TypeUtils.asFullyQualified(type);
            if (fqt != null && fqt.getPackageName().equals(oldPackageName)) {
                return buildNewType(fqt.getClassName());
            }

            JavaType.Method mt = TypeUtils.asMethod(type);
            if (mt != null) {
                return mt.withDeclaringType((JavaType.FullyQualified) updateType(mt.getDeclaringType()))
                        .withResolvedSignature(updateSignature(mt.getResolvedSignature()))
                        .withGenericSignature(updateSignature(mt.getGenericSignature()));
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
