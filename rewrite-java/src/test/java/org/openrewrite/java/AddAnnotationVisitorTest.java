/*
 * Copyright 2021 VMware, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 *
 * @author: fkrueger
 */
package org.openrewrite.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Result;
import org.openrewrite.java.tree.J;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AddAnnotationVisitorTest {

    AddAnnotationVisitor sut;

    @Test
    void visitClassDeclaration() {
        String javaCode = "public class Foo {}";

        // FIXME: requires download
        JavaParser javaParser = JavaParser.fromJavaVersion()
                .classpath(List.of(Path.of(System.getProperty("user.home")).resolve(".m2/repository/javax/ejb/javax.ejb-api/3.2/javax.ejb-api-3.2.jar")))
                .build();

        J.CompilationUnit compilationUnit1 = javaParser
                .parse(javaCode)
                .get(0);

        String annotationImport = "javax.ejb.Stateless";

        J target = compilationUnit1.getClasses().get(0);
        String snippet = "@Stateless(name = \"test\")";

        sut = new AddAnnotationVisitor(() -> javaParser, target, snippet, annotationImport);
        J.CompilationUnit afterVisit = (J.CompilationUnit) sut.visit(compilationUnit1, new InMemoryExecutionContext());

        assertThat(afterVisit.print()).isEqualTo(
                "import javax.ejb.Stateless;\n" +
                        "\n" +
                        snippet + "\n" +
                        "public class Foo {}"
        );

        J.Annotation annotation = afterVisit.getClasses().get(0).getLeadingAnnotations().get(0);
        assertThat(annotation.getAnnotationType().getType()).isNotNull();
    }

    @Test
    void visitClassDeclarationWithGenericRewriteRecipe() {
        String javaCode = "public class Foo {}";

        // FIXME: needs to exist
        JavaParser javaParser = JavaParser.fromJavaVersion()
                .classpath(List.of(Path.of(System.getProperty("user.home")).resolve(".m2/repository/javax/ejb/javax.ejb-api/3.2/javax.ejb-api-3.2.jar")))
                .build();

        J.CompilationUnit compilationUnit1 = javaParser
                .parse(javaCode)
                .get(0);

        InMemoryExecutionContext executionContext = new InMemoryExecutionContext();
        executionContext.putMessage("java-parser", javaParser); // required otherwise type is not resolved
        String annotationImport = "javax.ejb.Stateless";

        J target = compilationUnit1.getClasses().get(0);
        String snippet = "@Stateless(name = \"test\")";

        sut = new AddAnnotationVisitor(() -> javaParser, target, snippet, annotationImport);
        List<Result> run = new GenericOpenRewriteRecipe<>(sut).run(List.of(compilationUnit1), executionContext);
        J.CompilationUnit afterVisit = (J.CompilationUnit) run.get(0).getAfter();

        assertThat(afterVisit.print()).isEqualTo(
                "import javax.ejb.Stateless;\n" +
                        "\n" +
                        snippet + "\n" +
                        "public class Foo {}"
        );

        J.Annotation annotation = afterVisit.getClasses().get(0).getLeadingAnnotations().get(0);
        assertThat(annotation.getAnnotationType().getType()).isNotNull();
    }

    @Test
    void visitMethodDeclaration() {
    }

    @Test
    void visitVariableDeclarations() {
    }
}