/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.java.internal;

import org.intellij.lang.annotations.Language;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTypeSignatureBuilderTest;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static java.util.Objects.requireNonNull;

class DefaultJavaTypeSignatureBuilderTest implements JavaTypeSignatureBuilderTest {

    @Language("java")
    private final String goat = StringUtils.readFully(
            requireNonNull(DefaultJavaTypeSignatureBuilderTest.class.getResourceAsStream("/JavaTypeGoat.java")), StandardCharsets.UTF_8);

    private final J.CompilationUnit goatCu = JavaParser.fromJavaVersion()
            .build()
            .parse(goat)
      .findFirst()
      .orElseThrow(() -> new IllegalArgumentException("Could not parse as Java"));

    private final JavaType.Parameterized goatCuType = requireNonNull(TypeUtils.asParameterized(goatCu
            .getClasses()
            .get(0)
            .getType()));

    @Override
    public String fieldSignature(String field) {
        return signatureBuilder().variableSignature(goatCuType.getType().getMembers().stream()
                .filter(m -> m.getName().equals(field))
                .findAny()
                .orElseThrow());
    }

    @Override
    public String methodSignature(String methodName) {
        return signatureBuilder().methodSignature(goatCuType.getType().getMethods().stream()
                .filter(m -> m.getName().equals(methodName))
                .findAny()
                .orElseThrow());
    }

    @Override
    public String constructorSignature() {
        return signatureBuilder().methodSignature(goatCuType.getType().getMethods().stream()
                .filter(m -> m.getName().equals("<constructor>"))
                .findAny()
                .orElseThrow());
    }

    @Override
    public JavaType firstMethodParameter(String methodName) {
        return goatCuType.getType().getMethods().stream()
                .filter(m -> m.getName().equals(methodName))
                .map(m -> m.getParameterTypes().get(0))
                .findAny()
                .orElseThrow();
    }

    @Override
    public JavaType innerClassSignature(String innerClassSimpleName) {
        return requireNonNull(goatCu.getClasses().get(0).getBody().getStatements()
                .stream()
                .filter(it -> it instanceof J.ClassDeclaration)
                .map(it -> (J.ClassDeclaration) it)
                .filter(cd -> requireNonNull(cd.getType()).getFullyQualifiedName().endsWith("$" + innerClassSimpleName))
                .findAny()
                .orElseThrow()
                .getType());
    }

    @Override
    public JavaType lastClassTypeParameter() {
        List<JavaType> typeParameters = goatCuType.getTypeParameters();
        return typeParameters.get(typeParameters.size() - 1);
    }

    @Override
    public DefaultJavaTypeSignatureBuilder signatureBuilder() {
        return new DefaultJavaTypeSignatureBuilder();
    }
}
