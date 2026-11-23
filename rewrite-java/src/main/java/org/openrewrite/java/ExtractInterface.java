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
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.internal.PackageNameUtils;
import org.openrewrite.java.search.FindAnnotations;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.text.ParseException;
import java.text.RuleBasedCollator;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Set;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static org.openrewrite.Tree.randomId;

public class ExtractInterface {

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class CreateInterface extends JavaIsoVisitor<ExecutionContext> {
        String fullyQualifiedInterfaceName;

        @Override
        public J postVisit(J tree, ExecutionContext ctx) {
            if (tree instanceof JavaSourceFile) {
                JavaSourceFile c = (JavaSourceFile) requireNonNull(tree);

                String pkg = c.getPackageDeclaration() == null ? "" :
                        Arrays.stream(PackageNameUtils.getPackageName(c.getPackageDeclaration()).split("\\."))
                                .map(subpackage -> "..")
                                .collect(joining("/", "../", "/"));

                String interfacePkg = JavaType.ShallowClass.build(fullyQualifiedInterfaceName).getPackageName();
                if (!interfacePkg.isEmpty()) {
                    c = c.withPackageDeclaration(new J.Package(randomId(),
                            c.getPackageDeclaration() == null ? Space.EMPTY : c.getPackageDeclaration().getPrefix(),
                            Markers.EMPTY,
                            TypeTree.build(interfacePkg).withPrefix(Space.format(" ")),
                            c.getPackageDeclaration() == null ? emptyList() : c.getPackageDeclaration().getAnnotations()));
                }

                return (JavaSourceFile) c.withSourcePath(c.getSourcePath()
                        .resolve(pkg + fullyQualifiedInterfaceName.replace('.', '/') + ".java")
                        .normalize());
            }
            return super.postVisit(tree, ctx);
        }

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
            J.ClassDeclaration c = classDecl;

            c = c.withKind(J.ClassDeclaration.Kind.Type.Interface);
            c = c.withBody(c.getBody().withStatements(ListUtils.filter(c.getBody().getStatements(), s ->
                    s instanceof J.MethodDeclaration)));
            c = c.withName(c.getName().withSimpleName(fullyQualifiedInterfaceName.substring(
                    fullyQualifiedInterfaceName.lastIndexOf('.') + 1
            )).withType(interfaceType()));
            c = c.withType(interfaceType());

            return autoFormat(super.visitClassDeclaration(c, ctx), ctx);
        }

        @Override
        public  J.@Nullable MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
            if (method.hasModifier(J.Modifier.Type.Static) || !method.hasModifier(J.Modifier.Type.Public) ||
                method.isConstructor()) {
                //noinspection ConstantConditions
                return null;
            }

            J.MethodDeclaration m = method
                    .withModifiers(ListUtils.filter(method.getModifiers(), mod ->
                            !(mod.getType() == J.Modifier.Type.Public ||
                            mod.getType() == J.Modifier.Type.Final ||
                            mod.getType() == J.Modifier.Type.Native)))
                    .withBody(null)
                    .withMethodType(method.getMethodType() == null ? null : interfaceMethodType(method.getMethodType()));
            return m.getName().getType() == null ? m : m.withName(m.getName().withType(m.getMethodType()));
        }

        private JavaType.Method interfaceMethodType(JavaType.Method methodType) {
            // without its body and the modifiers an interface method can't have, it is abstract
            Set<Flag> flags = EnumSet.noneOf(Flag.class);
            flags.addAll(methodType.getFlags());
            flags.removeAll(Arrays.asList(Flag.Final, Flag.Native));
            flags.add(Flag.Abstract);
            return methodType.withDeclaringType(interfaceType()).withFlags(flags);
        }

        private JavaType.FullyQualified interfaceType() {
            return JavaType.ShallowClass.build(fullyQualifiedInterfaceName)
                    .withKind(JavaType.FullyQualified.Kind.Interface);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class ImplementAndAddOverrideAnnotations extends JavaIsoVisitor<ExecutionContext> {
        String fullyQualifiedInterfaceName;

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
            if (classDecl.getType() != null && classDecl.getType().getFullyQualifiedName().equals(fullyQualifiedInterfaceName)) {
                return classDecl;
            }

            maybeAddImport(fullyQualifiedInterfaceName);
            JavaType.ShallowClass type = JavaType.ShallowClass.build(fullyQualifiedInterfaceName);

            J.Block body = classDecl.getBody();
            Cursor parent = getCursor().getParent();
            assert parent != null;
            J.ClassDeclaration implementing = (J.ClassDeclaration) new ImplementInterface<>(classDecl, type).visitNonNull(classDecl, ctx, parent);

            return (J.ClassDeclaration) new JavaIsoVisitor<ExecutionContext>() {
                @Override
                public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                    if (getCursor().getParentTreeCursor().getValue() == body) {
                        try {
                            if (!method.hasModifier(J.Modifier.Type.Public) ||
                                method.hasModifier(J.Modifier.Type.Static) ||
                                method.isConstructor()) {
                                return method;
                            }

                            if (FindAnnotations.find(method, "@java.lang.Override").isEmpty()) {
                                return JavaTemplate.apply(
                                        "@Override",
                                        getCursor(),
                                        method.getCoordinates().addAnnotation(Comparator.comparing(
                                                J.Annotation::getSimpleName,
                                                new RuleBasedCollator("< Override")
                                        ))
                                );
                            }

                            return method;
                        } catch (ParseException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    return super.visitMethodDeclaration(method, ctx);
                }
            }.visitNonNull(implementing, ctx, getCursor().getParentOrThrow());
        }
    }
}
