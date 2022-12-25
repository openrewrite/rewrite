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
import org.openrewrite.Tree;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.search.FindAnnotations;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.text.ParseException;
import java.text.RuleBasedCollator;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static org.openrewrite.Tree.randomId;

public class ExtractInterface {
    public static List<JavaSourceFile> extract(JavaSourceFile cu, String fullyQualifiedInterfaceName) {
        return Arrays.asList(
                (JavaSourceFile) new CreateInterface(fullyQualifiedInterfaceName).visitNonNull(cu, 0),
                (JavaSourceFile) new ImplementAndAddOverrideAnnotations(fullyQualifiedInterfaceName).visitNonNull(cu, 0)
        );
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    private static class CreateInterface extends JavaIsoVisitor<Integer> {
        String fullyQualifiedInterfaceName;

        @Override
        public JavaSourceFile visitJavaSourceFile(JavaSourceFile cu, Integer p) {
            String pkg = cu.getPackageDeclaration() == null ? "" :
                    Arrays.stream(cu.getPackageDeclaration().getExpression().printTrimmed(getCursor()).split("\\."))
                            .map(subpackage -> "..")
                            .collect(Collectors.joining("/", "../", "/"));

            JavaSourceFile c = super.visitJavaSourceFile(cu, p)
                    .withId(Tree.randomId());

            String interfacePkg = JavaType.ShallowClass.build(fullyQualifiedInterfaceName).getPackageName();
            if (!interfacePkg.isEmpty()) {
                c = c.withPackageDeclaration(new J.Package(randomId(),
                        cu.getPackageDeclaration() == null ? Space.EMPTY : cu.getPackageDeclaration().getPrefix(),
                        Markers.EMPTY,
                        TypeTree.build(interfacePkg).withPrefix(Space.format(" ")),
                        cu.getPackageDeclaration() == null ? emptyList() : cu.getPackageDeclaration().getAnnotations()));
            }

            c = (JavaSourceFile) c.withSourcePath(cu.getSourcePath()
                    .resolve(pkg + fullyQualifiedInterfaceName.replace('.', '/') + ".java")
                    .normalize());

            return c;
        }

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, Integer p) {
            J.ClassDeclaration c = classDecl;

            c = c.withKind(J.ClassDeclaration.Kind.Type.Interface);
            c = c.withBody(c.getBody().withStatements(ListUtils.map(c.getBody().getStatements(), s ->
                    s instanceof J.MethodDeclaration ? s : null)));
            c = c.withName(c.getName().withSimpleName(fullyQualifiedInterfaceName.substring(
                    fullyQualifiedInterfaceName.lastIndexOf('.') + 1
            )));

            return autoFormat(super.visitClassDeclaration(c, p), p);
        }

        @Override
        public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, Integer p) {
            if (method.hasModifier(J.Modifier.Type.Static) || !method.hasModifier(J.Modifier.Type.Public) ||
                    method.isConstructor()) {
                //noinspection ConstantConditions
                return null;
            }

            return method
                    .withModifiers(ListUtils.map(method.getModifiers(), m ->
                            m.getType() == J.Modifier.Type.Public ||
                                    m.getType() == J.Modifier.Type.Final ||
                                    m.getType() == J.Modifier.Type.Native ? null : m))
                    .withBody(null);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    private static class ImplementAndAddOverrideAnnotations extends JavaIsoVisitor<Integer> {
        String fullyQualifiedInterfaceName;

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, Integer p) {
            maybeAddImport(fullyQualifiedInterfaceName);
            JavaType.ShallowClass type = JavaType.ShallowClass.build(fullyQualifiedInterfaceName);

            J.Block body = classDecl.getBody();
            return ((J.ClassDeclaration) new ImplementInterface<>(classDecl, type).visitNonNull(classDecl, p))
                    .withBody(body.withStatements(ListUtils.map(body.getStatements(), s -> {
                        if (s instanceof J.MethodDeclaration) {
                            J.MethodDeclaration methodDeclaration = (J.MethodDeclaration) s;
                            try {
                                if (!methodDeclaration.hasModifier(J.Modifier.Type.Public) ||
                                        methodDeclaration.hasModifier(J.Modifier.Type.Static) ||
                                        methodDeclaration.isConstructor()) {
                                    return s;
                                }

                                if(FindAnnotations.find(methodDeclaration, "@java.lang.Override").isEmpty()) {
                                    return methodDeclaration.withTemplate(
                                            JavaTemplate.builder(this::getCursor, "@Override").build(),
                                            methodDeclaration.getCoordinates().addAnnotation(
                                                    Comparator
                                                            .comparing(
                                                                    J.Annotation::getSimpleName,
                                                                    new RuleBasedCollator("< Override")
                                                            ))
                                    );
                                }

                                return methodDeclaration;
                            } catch (ParseException e) {
                                throw new RuntimeException(e);
                            }
                        }
                        return s;
                    })));
        }
    }
}
