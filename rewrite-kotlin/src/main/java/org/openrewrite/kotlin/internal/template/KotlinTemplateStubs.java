/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.kotlin.internal.template;

import org.openrewrite.java.internal.template.TemplateStubs;
import org.openrewrite.java.tree.*;
import org.openrewrite.kotlin.tree.K;

import java.util.List;

import static java.util.Collections.singletonList;
import static org.openrewrite.java.internal.template.TemplateStubs.Imports.NONE;
import static org.openrewrite.java.internal.template.TemplateStubs.Imports.TEMPLATE;

/**
 * Kotlin stubs.
 * <p>
 * Two differences from {@link org.openrewrite.java.internal.template.JavaTemplateStubs} drive the shapes here.
 * Kotlin allows declarations at file scope, so most stubs need no wrapper class and are read straight off
 * {@link K.CompilationUnit#getStatements()}. And {@code $} is not a legal Kotlin identifier character, so the
 * synthetic names use {@code __Template__} / {@code __template__} rather than Java's {@code $Template}.
 */
public class KotlinTemplateStubs implements TemplateStubs {

    @Override
    public Stub<Statement> parameters() {
        return Stub.of("fun __template__(#{}) {}", TEMPLATE,
                cu -> asMethodDeclaration(firstStatement(cu)).getParameters());
    }

    @Override
    public Stub<J.Lambda.Parameters> lambdaParameters() {
        return Stub.ofOne("val __o__ = { #{} -> Unit }", TEMPLATE, cu -> {
            J.VariableDeclarations v = asVariableDeclarations(firstStatement(cu));
            J.Lambda l = (J.Lambda) v.getVariables().get(0).getInitializer();
            assert l != null;
            return l.getParameters();
        });
    }

    /**
     * Kotlin has no {@code extends}: the parser puts every supertype into the implements container and the
     * printer renders them all after a single {@code :}. Both coordinates therefore share one stub, and
     * {@code parseExtends} taking the first element lands on Kotlin's superclass position.
     */
    @Override
    public Stub<TypeTree> anExtends() {
        return supertypes();
    }

    @Override
    public Stub<TypeTree> anImplements() {
        return supertypes();
    }

    private Stub<TypeTree> supertypes() {
        return Stub.of("class __Template__ : #{}", TEMPLATE, cu -> {
            List<TypeTree> supertypes = asClassDeclaration(firstStatement(cu)).getImplements();
            assert supertypes != null;
            return supertypes;
        });
    }

    @Override
    public Stub<NameTree> checkedExceptions() {
        throw new UnsupportedOperationException(
                "Kotlin has no checked exceptions and no `throws` clause, so there is nothing for this " +
                "coordinate to replace. Remove the throws coordinate from this recipe.");
    }

    @Override
    public Stub<J.TypeParameter> typeParameters() {
        return Stub.of("class __Template__<#{}>", TEMPLATE, cu -> {
            List<J.TypeParameter> tps = asClassDeclaration(firstStatement(cu)).getTypeParameters();
            assert tps != null;
            return tps;
        });
    }

    @Override
    public Stub<Expression> packageDeclaration() {
        //noinspection ConstantConditions
        return Stub.ofOne("package #{}", NONE, cu -> cu.getPackageDeclaration().getExpression());
    }

    /**
     * Top-level declarations are plain statements on {@link K.CompilationUnit}; there is no synthetic wrapper
     * class to descend through as there is in Java. Note {@code getClasses()} cannot be used here because it
     * filters to {@link J.ClassDeclaration} and so misses {@link K.ClassDeclaration}.
     */
    private static Statement firstStatement(JavaSourceFile cu) {
        return ((K.CompilationUnit) cu).getStatements().get(0);
    }

    // The K.* wrappers below appear only when a `where` clause or context parameters are present, so every
    // extractor has to accept both the wrapped and the bare form.

    private static J.MethodDeclaration asMethodDeclaration(Statement s) {
        return s instanceof K.MethodDeclaration ? ((K.MethodDeclaration) s).getMethodDeclaration() : (J.MethodDeclaration) s;
    }

    private static J.ClassDeclaration asClassDeclaration(Statement s) {
        return s instanceof K.ClassDeclaration ? ((K.ClassDeclaration) s).getClassDeclaration() : (J.ClassDeclaration) s;
    }

    private static J.VariableDeclarations asVariableDeclarations(Statement s) {
        return s instanceof K.Property ? ((K.Property) s).getVariableDeclarations() : (J.VariableDeclarations) s;
    }
}
