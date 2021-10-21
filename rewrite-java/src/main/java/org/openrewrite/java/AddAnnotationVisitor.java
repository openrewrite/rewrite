/*
 * Copyright 2021 VMware, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 *
 * @author: Alex Boyko
 */
package org.openrewrite.java;

import org.jetbrains.annotations.NotNull;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.JavaTemplate.Builder;
import org.openrewrite.java.tree.J;

import java.util.function.Supplier;

/**
 * Adds <code>annotation</code> to <code>target</code> if it is part of <code>scope</code>
 */
public class AddAnnotationVisitor extends JavaIsoVisitor<ExecutionContext> {
    private final J target;
    private final String snippet;
    private final String[] imports;
    private final Supplier<JavaParser> javaParserSupplier;

    public AddAnnotationVisitor(JavaParser javaParserSupplier, J target, String snippet, String annotationImport, String... otherImports) {
        this(() -> javaParserSupplier, target, snippet, annotationImport, otherImports);
    }

    public AddAnnotationVisitor(Supplier<JavaParser> javaParserSupplier, J target, String snippet, String annotationImport, String... otherImports) {
        this.target = target;
        this.snippet = snippet;
        this.imports = otherImports == null
                ? new String[]{annotationImport}
                : concat(annotationImport, otherImports);
        this.javaParserSupplier = javaParserSupplier;
    }

    public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext p) {
        if (target.getId().equals(classDecl.getId())) {
            addImports(imports);
            JavaTemplate template = getJavaTemplate(p, snippet, imports);
            return classDecl.withTemplate(template, classDecl.getCoordinates().addAnnotation((o1, o2) -> 0));
        }
        return super.visitClassDeclaration(classDecl, p);
    }


    public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration methodDecl, ExecutionContext p) {
        if (target == methodDecl) {
            addImports(imports);
            JavaTemplate template = getJavaTemplate(p, snippet, imports);
            return methodDecl.withTemplate(template, methodDecl.getCoordinates().addAnnotation((o1, o2) -> 0));
        }
        return super.visitMethodDeclaration(methodDecl, p);
    }

    @Override
    public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext p) {
        if (target == multiVariable) {
            addImports(imports);
            JavaTemplate template = getJavaTemplate(p, snippet, imports);
            return multiVariable.withTemplate(template, multiVariable.getCoordinates().addAnnotation((o1,o2) -> o1.getSimpleName().compareTo(o2.getSimpleName())));
        }
        return super.visitVariableDeclarations(multiVariable, p);
    }

    private String[] concat(String annotationImport, String[] otherImports) {
        String[] result = new String[otherImports.length + 1];
        result[0] = annotationImport;
        System.arraycopy(otherImports, 0, result, 1, otherImports.length);
        return result;
    }

    @NotNull
    private JavaTemplate getJavaTemplate(ExecutionContext p, String snippet, String... imports) {
        Builder builder = JavaTemplate.builder(() -> getCursor(), snippet);
        builder.imports(imports);
        builder.javaParser(javaParserSupplier);
        JavaTemplate template = builder.build();
        return template;
    }

    private void addImports(String... imports) {
        for (String i : imports) {
            AddImport<ExecutionContext> op = new AddImport(i, (String)null, false);
            if (!super.getAfterVisit().contains(op)) {
                super.doAfterVisit(op);
            }
        }
    }
}
