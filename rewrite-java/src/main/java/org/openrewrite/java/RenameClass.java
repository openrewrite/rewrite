/*
 * Copyright 2022 the original author or authors.
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
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.*;

import java.nio.file.Path;
import java.nio.file.Paths;

@Value
@EqualsAndHashCode(callSuper = true)
public class RenameClass extends Recipe {

    @Option(displayName = "Old fully-qualified type name",
            description = "Fully-qualified class name of the original type.",
            example = "org.junit.Assume")
    String oldFullyQualifiedTypeName;

    @Option(displayName = "New fully-qualified type name",
            description = "Fully-qualified class name of the replacement type.",
            example = "org.junit.jupiter.api.Assumptions")
    String newFullyQualifiedTypeName;

    @Override
    public String getDisplayName() {
        return "Rename a class declaration that matches the provided fully qualified name";
    }

    @Override
    public String getDescription() {
        return "`RenameClass` will rename a single class declaration and update the source file path when possible. " +
                "The source file path will only be updated if the path matches the old FQN and the target FQN matches a top-level public class. I.E. `/a/b/C` and `a.b.C` where `C` is public." +
                "The last class in the provided FQN will be renamed. Multiple class renames are not supported like `a.b.C$D$E` to `a.b.X$Y$Z`.";
    }

    @Override
    protected JavaVisitor<ExecutionContext> getSingleSourceApplicableTest() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext executionContext) {
                J.ClassDeclaration cd = super.visitClassDeclaration(classDecl, executionContext);
                if (TypeUtils.isOfClassType(cd.getType(), oldFullyQualifiedTypeName)) {
                    return cd.withMarkers(cd.getMarkers().searchResult());
                }
                return cd;
            }
        };
    }

    @Override
    public JavaVisitor<ExecutionContext> getVisitor() {
        return new RenameClassVisitor(oldFullyQualifiedTypeName, newFullyQualifiedTypeName);
    }

    private class RenameClassVisitor extends JavaIsoVisitor<ExecutionContext> {
        private final JavaType.Class oldType;
        private final JavaType newType;

        private RenameClassVisitor(String oldFullyQualifiedTypeName, String newFullyQualifiedTypeName) {
            this.oldType = JavaType.ShallowClass.build(oldFullyQualifiedTypeName);
            this.newType = JavaType.buildType(newFullyQualifiedTypeName);
        }

        @Override
        public JavaSourceFile visitJavaSourceFile(JavaSourceFile sf, ExecutionContext ctx) {
            String oldPath = ((SourceFile) sf).getSourcePath().toString().replace('\\', '/');
            // The old FQN must exist in the path.
            String oldFqn = fqnToPath(oldType.getFullyQualifiedName());
            String newFqn = fqnToPath(newFullyQualifiedTypeName);

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
                            TypeUtils.isOfClassType(o.getType(), getTopLevelClassName(oldType)));
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
            String oldPkg = oldType.getFullyQualifiedName().substring(0, oldType.getFullyQualifiedName().lastIndexOf('.'));
            if (original.equals(oldPkg)) {
                if (newFullyQualifiedTypeName.contains(".")) {
                    String newPkg = newFullyQualifiedTypeName.substring(0, newFullyQualifiedTypeName.lastIndexOf('.'));
                    p = p.withTemplate(JavaTemplate.builder(this::getCursor, newPkg).build(), p.getCoordinates().replace());
                } else {
                    getCursor().putMessageOnFirstEnclosing(J.CompilationUnit.class, "UPDATE_PREFIX", true);
                    p = null;
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

            if (TypeUtils.isOfClassType(classDecl.getType(), oldType.getFullyQualifiedName())) {
                JavaType.FullyQualified fq = JavaType.FullyQualified.ShallowClass.build(newFullyQualifiedTypeName);
                String newClassName = getNewClassName(fq);
                cd = cd.withName(cd.getName().withSimpleName(newClassName));
                cd = cd.withType(updateType(cd.getType()));
                doNext(new ChangeType(oldType.getFullyQualifiedName(), newFullyQualifiedTypeName, true));
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
                        if (fq.getFullyQualifiedName().equals(oldType.getFullyQualifiedName()) && newType instanceof JavaType.FullyQualified) {
                            return newType;
                        }
                    }
                    return bound;
                }));
            }

            JavaType.FullyQualified fqt = TypeUtils.asFullyQualified(type);
            if (fqt != null && fqt.getFullyQualifiedName().equals(oldType.getFullyQualifiedName())) {
                return newType;
            }

            //noinspection ConstantConditions
            return type;
        }
    }
}
