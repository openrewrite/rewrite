/*
 * Copyright 2021 VMware, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 *
 * @author: Fabian Krüger
 */
package org.openrewrite.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.tree.J;

import static org.assertj.core.api.Assertions.assertThat;

public class AddOrReplaceAnnotationAttributeVisitorTest {


    @Test
    void addBooleanAttributeToAnnotationWithoutAttributes() {
        String code = "@Deprecated public class Foo {}";
        J.CompilationUnit compilationUnit = OpenRewriteTestSupport.createCompilationUnit(code);

        J.Annotation annotation = compilationUnit.getClasses().get(0).getLeadingAnnotations().get(0);
        JavaIsoVisitor<ExecutionContext> javaIsoVisitor = new AddOrReplaceAnnotationAttributeVisitor(annotation, "forRemoval", true, Boolean.class);

        String refactoredCu = javaIsoVisitor.visit(compilationUnit, new InMemoryExecutionContext()).print();

        assertThat(refactoredCu).isEqualTo("@Deprecated(forRemoval = true) public class Foo {}");
    }

    @Test
    void addStringAttributeToAnnotationWithoutAttributes() {
        String code = "@Deprecated public class Foo {}";
        J.CompilationUnit compilationUnit = OpenRewriteTestSupport.createCompilationUnit(code);

        J.Annotation annotation = compilationUnit.getClasses().get(0).getLeadingAnnotations().get(0);
        JavaIsoVisitor<ExecutionContext> javaIsoVisitor = new AddOrReplaceAnnotationAttributeVisitor(annotation, "since", "2020", String.class);
        String refactoredCu = javaIsoVisitor.visit(compilationUnit, new InMemoryExecutionContext()).print();

        assertThat(refactoredCu).isEqualTo("@Deprecated(since = \"2020\") public class Foo {}");
    }

    @Test
    void changeAnnotationAttributeValue() {
        String code = "@Deprecated(forRemoval = false) public class Foo {}";
        J.CompilationUnit compilationUnit = OpenRewriteTestSupport.createCompilationUnit(code);
        J.Annotation annotation = compilationUnit.getClasses().get(0).getLeadingAnnotations().get(0);
        JavaIsoVisitor<ExecutionContext> javaIsoVisitor = new AddOrReplaceAnnotationAttributeVisitor(annotation, "forRemoval", true, Boolean.class);
        String refactoredCu = javaIsoVisitor.visit(compilationUnit, new InMemoryExecutionContext()).print();
        assertThat(refactoredCu).isEqualTo("@Deprecated(forRemoval = true) public class Foo {}");
    }

    @Test
    void changeAnnotationAttributeValueOfAnnotationWithAttributes() {
        String code = "@Deprecated(forRemoval = false, since = \"2020\") public class Foo {}";
        J.CompilationUnit compilationUnit = OpenRewriteTestSupport.createCompilationUnit(code);
        J.Annotation annotation = compilationUnit.getClasses().get(0).getLeadingAnnotations().get(0);
        JavaIsoVisitor<ExecutionContext> javaIsoVisitor = new AddOrReplaceAnnotationAttributeVisitor(annotation, "forRemoval", true, Boolean.class);
        String refactoredCu = javaIsoVisitor.visit(compilationUnit, new InMemoryExecutionContext()).print();
        assertThat(refactoredCu).isEqualTo("@Deprecated(forRemoval = true, since = \"2020\") public class Foo {}");
    }

    @Test
    void changeAnnotationAttributeValueOfAnnotationWithAttributes2() {
        String code = "@Deprecated(since = \"2020\", forRemoval = false) public class Foo {}";
        J.CompilationUnit compilationUnit = OpenRewriteTestSupport.createCompilationUnit(code);
        J.Annotation annotation = compilationUnit.getClasses().get(0).getLeadingAnnotations().get(0);
        JavaIsoVisitor<ExecutionContext> javaIsoVisitor = new AddOrReplaceAnnotationAttributeVisitor(annotation, "forRemoval", true, Boolean.class);
        String refactoredCu = javaIsoVisitor.visit(compilationUnit, new InMemoryExecutionContext()).print();
        assertThat(refactoredCu).isEqualTo("@Deprecated(since = \"2020\", forRemoval = true) public class Foo {}");
    }

    @Test
    void addAttributeToAnnotationWithAttributes() {
        String code = "@Deprecated(forRemoval = true) public class Foo {}";
        J.CompilationUnit compilationUnit = OpenRewriteTestSupport.createCompilationUnit(code);
        J.Annotation annotation = compilationUnit.getClasses().get(0).getLeadingAnnotations().get(0);
        JavaIsoVisitor<ExecutionContext> javaIsoVisitor = new AddOrReplaceAnnotationAttributeVisitor(annotation, "since", "2020", String.class);
        String refactoredCu = javaIsoVisitor.visit(compilationUnit, new InMemoryExecutionContext()).print();
        assertThat(refactoredCu).isEqualTo("@Deprecated(forRemoval = true, since = \"2020\") public class Foo {}");
    }


}




