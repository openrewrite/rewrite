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
import org.openrewrite.*;
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
            public JavaSourceFile visitJavaSourceFile(JavaSourceFile cu, ExecutionContext executionContext) {
                if (cu.getPackageDeclaration() != null) {
                    String original = cu.getPackageDeclaration().getExpression()
                            .printTrimmed(getCursor()).replaceAll("\\s", "");
                    if (original.startsWith(oldPackageName)) {
                        return cu.withMarkers(cu.getMarkers().searchResult());
                    }
                }
                if (recursive != null && recursive) {
                    doAfterVisit(new UsesType<>(oldPackageName + "..*"));
                } else {
                    doAfterVisit(new UsesType<>(oldPackageName + ".*"));
                }
                return cu;
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new ChangePackageVisitor();
    }

    private class ChangePackageVisitor extends JavaIsoVisitor<ExecutionContext> {
        private static final String RENAME_TO_KEY = "renameTo";
        private static final String RENAME_FROM_KEY = "renameFrom";

        private final JavaType.Class newPackageType = JavaType.ShallowClass.build(newPackageName);

        @Override
        public JavaSourceFile visitJavaSourceFile(JavaSourceFile cu, ExecutionContext ctx) {
            JavaSourceFile c = super.visitJavaSourceFile(cu, ctx);

            String changingTo = getCursor().getMessage(RENAME_TO_KEY);
            if (changingTo != null) {
                String path = ((SourceFile) c).getSourcePath().toString().replace('\\', '/');
                String changingFrom = getCursor().getMessage(RENAME_FROM_KEY);
                assert changingFrom != null;
                c = ((SourceFile) c).withSourcePath(Paths.get(path.replaceFirst(
                        changingFrom.replace('.', '/'),
                        changingTo.replace('.', '/')
                )));

                for (J.Import anImport : c.getImports()) {
                    if (anImport.getPackageName().equals(newPackageName)) {
                        c = new RemoveImport<ExecutionContext>(anImport.getTypeName(), true).visitJavaSourceFile(c, ctx);
                    }
                }
            }
            return c;
        }

        @Override
        public J postVisit(J tree, ExecutionContext executionContext) {
            J j = super.postVisit(tree, executionContext);
            if (j instanceof J.MethodDeclaration) {
                J.MethodDeclaration m = (J.MethodDeclaration) j;
                return m.withMethodType(updateType(m.getMethodType()));
            } else if (j instanceof J.MethodInvocation) {
                J.MethodInvocation m = (J.MethodInvocation) j;
                return m.withMethodType(updateType(m.getMethodType()));
            } else if (j instanceof J.NewClass) {
                J.NewClass n = (J.NewClass) j;
                return n.withConstructorType(updateType(n.getConstructorType()));
            } else if (j instanceof TypedTree) {
                return ((TypedTree) j).withType(updateType(((TypedTree) j).getType()));
            }
            return j;
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
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
            J.ClassDeclaration c = super.visitClassDeclaration(classDecl, ctx);

            Boolean updatePrefix = getCursor().pollNearestMessage("UPDATE_PREFIX");
            if (updatePrefix != null && updatePrefix) {
                c = c.withPrefix(Space.EMPTY);
            }

            String changingTo = getCursor().getNearestMessage(RENAME_TO_KEY);
            if (changingTo != null && classDecl.getType() != null) {
                JavaType.FullyQualified type = c.getType();
                if (type != null) {
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
                if (newPackageName.contains(".")) {
                    pkg = pkg.withTemplate(JavaTemplate.builder(this::getCursor, newPackageName).build(), pkg.getCoordinates().replace());
                } else {
                    getCursor().putMessageOnFirstEnclosing(J.CompilationUnit.class, "UPDATE_PREFIX", true);
                    pkg = null;
                }
            } else if ((recursive == null || recursive)
                    && original.startsWith(oldPackageName) && !original.startsWith(newPackageName)) {
                String changingTo = newPackageName + original.substring(oldPackageName.length());
                getCursor().putMessageOnFirstEnclosing(J.CompilationUnit.class, RENAME_TO_KEY, changingTo);
                pkg = pkg.withTemplate(JavaTemplate.builder(this::getCursor, changingTo).build(), pkg.getCoordinates().replace());
            }
            //noinspection ConstantConditions
            return pkg;
        }

        @Nullable
        private JavaType updateType(@Nullable JavaType javaType) {
            JavaType.FullyQualified fq = TypeUtils.asFullyQualified(javaType);
            if (fq != null && fq.getPackageName().equals(oldPackageName) && !fq.getClassName().isEmpty()) {
                return TypeUtils.asFullyQualified(JavaType.buildType(newPackageName + "." + fq.getClassName()));
            }
            return javaType;
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
}
