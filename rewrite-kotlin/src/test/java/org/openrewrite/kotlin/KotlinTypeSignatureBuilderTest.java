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
package org.openrewrite.kotlin;

import org.jetbrains.kotlin.com.intellij.openapi.Disposable;
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer;
import org.jetbrains.kotlin.fir.declarations.*;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.kotlin.internal.CompiledSource;
import org.openrewrite.tree.ParsingExecutionContextView;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Objects;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

public class KotlinTypeSignatureBuilderTest {
    private static final String goat = StringUtils.readFully(KotlinTypeSignatureBuilderTest.class.getResourceAsStream("/KotlinTypeGoat.kt"));

    private static final Disposable disposable = Disposer.newDisposable();
    private static final CompiledSource compiledSource = KotlinParser.builder()
            .logCompilationWarningsAndErrors(true)
            .moduleName("test")
            .build()
            .parse(singletonList(new Parser.Input(Path.of("KotlinTypeGoat.kt"), () -> new ByteArrayInputStream(goat.getBytes(StandardCharsets.UTF_8)))), disposable,
                    new ParsingExecutionContextView(new InMemoryExecutionContext(Throwable::printStackTrace)));

    @AfterAll
    static void afterAll() {
        Disposer.dispose(disposable);
    }

    public KotlinTypeSignatureBuilder signatureBuilder() {
        return new KotlinTypeSignatureBuilder(compiledSource.getFirSession(), Objects.requireNonNull(compiledSource.getSources().iterator().next().getFirFile()));
    }

    private FirFile getCompiledSource() {
        FirFile file = compiledSource.getSources().iterator().next().getFirFile();
        assert file != null;
        return file;
    }

    public String constructorSignature() {
        return signatureBuilder().methodSignature(getCompiledSource().getDeclarations().stream()
                .filter(FirRegularClass.class::isInstance)
                .map(FirRegularClass.class::cast)
                .flatMap(it -> it.getDeclarations().stream())
                .filter(FirConstructor.class::isInstance)
                .map(FirFunction.class::cast)
                .findFirst()
                .orElseThrow(), getCompiledSource());
    }

    public Object innerClassSignature(String innerClassSimpleName) {
        return signatureBuilder().signature(getCompiledSource().getDeclarations().stream()
                .filter(FirRegularClass.class::isInstance)
                .map(FirRegularClass.class::cast)
                .flatMap(it -> it.getDeclarations().stream())
                .filter(FirRegularClass.class::isInstance)
                .map(FirRegularClass.class::cast)
                .filter(it -> innerClassSimpleName.equals(it.getName().asString()))
                .findFirst()
                .orElseThrow());
    }

    public String fieldSignature(String field) {
        return signatureBuilder().variableSignature(getCompiledSource().getDeclarations().stream()
                .filter(FirRegularClass.class::isInstance)
                .map(FirRegularClass.class::cast)
                .flatMap(it -> it.getDeclarations().stream())
                .filter(FirProperty.class::isInstance)
                .map(FirProperty.class::cast)
                .filter(it -> field.equals(it.getName().asString()))
                .findFirst()
                .orElseThrow(), getCompiledSource());
    }

    public @Nullable FirProperty getProperty(String field) {
        return getCompiledSource().getDeclarations().stream()
                .filter(FirRegularClass.class::isInstance)
                .map(FirRegularClass.class::cast)
                .flatMap(it -> it.getDeclarations().stream())
                .filter(FirProperty.class::isInstance)
                .map(FirProperty.class::cast)
                .filter(it -> field.equals(it.getName().asString()))
                .findFirst()
                .orElse(null);
    }

    public String fieldPropertyGetterSignature(String field) {
        FirProperty property = getProperty(field);
        if (property == null || property.getGetter() == null) {
            throw new UnsupportedOperationException("No filed or getter for " + field);
        }
        return signatureBuilder().methodSignature(property.getGetter(), getCompiledSource());
    }

    public String fieldPropertySetterSignature(String field) {
        FirProperty property = getProperty(field);
        if (property == null || property.getSetter() == null) {
            throw new UnsupportedOperationException("No filed or setter for " + field);
        }
        return signatureBuilder().methodSignature(property.getSetter(), getCompiledSource());
    }

    public Object firstMethodParameterSignature(String methodName) {
        return signatureBuilder().signature(getCompiledSource().getDeclarations().stream()
                .filter(FirRegularClass.class::isInstance)
                .map(FirRegularClass.class::cast)
                .flatMap(it -> it.getDeclarations().stream())
                .filter(FirSimpleFunction.class::isInstance)
                .map(FirSimpleFunction.class::cast)
                .filter(it -> methodName.equals(it.getName().asString()))
                .findFirst()
                .orElseThrow()
                .getValueParameters()
                .getFirst()
                .getReturnTypeRef());
    }

    public Object lastClassTypeParameter() {
        return signatureBuilder().signature(getCompiledSource().getDeclarations().stream()
                .filter(FirRegularClass.class::isInstance)
                .map(FirRegularClass.class::cast)
                .findFirst()
                .orElseThrow()
                .getTypeParameters()
                .get(1));
    }

    public String methodSignature(String methodName) {
        return signatureBuilder().methodSignature(getCompiledSource().getDeclarations().stream()
                .filter(FirRegularClass.class::isInstance)
                .map(FirRegularClass.class::cast)
                .flatMap(it -> it.getDeclarations().stream())
                .filter(FirSimpleFunction.class::isInstance)
                .map(FirSimpleFunction.class::cast)
                .filter(it -> methodName.equals(it.getName().asString()))
                .findFirst()
                .orElseThrow(), getCompiledSource());
    }

    @Test
    void fileField() {
        FirProperty firProperty = getCompiledSource().getDeclarations().stream()
          .filter(it -> it instanceof FirProperty fp && "field".equals(fp.getName().asString()))
          .map(it -> (FirProperty) it).findFirst().orElseThrow();
        assertThat(signatureBuilder().variableSignature(firProperty, getCompiledSource()))
          .isEqualTo("org.openrewrite.kotlin.KotlinTypeGoatKt{name=field,type=kotlin.Int}");
    }

    @Test
    void fileFunction() {
        FirSimpleFunction function = getCompiledSource().getDeclarations().stream()
          .filter(it -> it instanceof FirSimpleFunction fsf && "function".equals(fsf.getName().asString()))
          .map(it -> (FirSimpleFunction) it).findFirst().orElseThrow();
        assertThat(signatureBuilder().methodSignature(function, getCompiledSource()))
          .isEqualTo("org.openrewrite.kotlin.KotlinTypeGoatKt{name=function,return=kotlin.Unit,parameters=[org.openrewrite.kotlin.C]}");
    }

    @Test
    void constructor() {
        assertThat(constructorSignature())
                .isEqualTo("org.openrewrite.kotlin.KotlinTypeGoat{name=<constructor>,return=org.openrewrite.kotlin.KotlinTypeGoat<Generic{T}, Generic{S extends org.openrewrite.kotlin.PT<Generic{S}> & org.openrewrite.kotlin.C}>,parameters=[]}");
    }

    @Test
    void parameterizedField() {
        assertThat(fieldSignature("parameterizedField"))
                .isEqualTo("org.openrewrite.kotlin.KotlinTypeGoat{name=parameterizedField,type=org.openrewrite.kotlin.PT<org.openrewrite.kotlin.KotlinTypeGoat$TypeA>}");
    }

    @Test
    void fieldType() {
        assertThat(fieldSignature("field"))
            .isEqualTo("org.openrewrite.kotlin.KotlinTypeGoat{name=field,type=kotlin.Int}");
    }

    @Test
    void gettableField() {
        assertThat(fieldPropertyGetterSignature("field"))
                .isEqualTo("org.openrewrite.kotlin.KotlinTypeGoat{name=get,return=kotlin.Int,parameters=[]}");
    }

    @Test
    void settableField() {
        assertThat(fieldPropertySetterSignature("field"))
          .isEqualTo("org.openrewrite.kotlin.KotlinTypeGoat{name=set,return=kotlin.Unit,parameters=[kotlin.Int]}");
    }

    @Test
    void classSignature() {
        assertThat(firstMethodParameterSignature("clazz"))
                .isEqualTo("org.openrewrite.kotlin.C");
        assertThat(methodSignature("clazz"))
                .isEqualTo("org.openrewrite.kotlin.KotlinTypeGoat{name=clazz,return=kotlin.Unit,parameters=[org.openrewrite.kotlin.C]}");
    }

    @Test
    void parameterized() {
        assertThat(firstMethodParameterSignature("parameterized"))
                .isEqualTo("org.openrewrite.kotlin.PT<org.openrewrite.kotlin.C>");
        assertThat(methodSignature("parameterized"))
                .isEqualTo("org.openrewrite.kotlin.KotlinTypeGoat{name=parameterized,return=org.openrewrite.kotlin.PT<org.openrewrite.kotlin.C>,parameters=[org.openrewrite.kotlin.PT<org.openrewrite.kotlin.C>]}");
    }

    @Test
    void parameterizedRecursive() {
        assertThat(firstMethodParameterSignature("parameterizedRecursive"))
                .isEqualTo("org.openrewrite.kotlin.PT<org.openrewrite.kotlin.PT<org.openrewrite.kotlin.C>>");
        assertThat(methodSignature("parameterizedRecursive"))
                .isEqualTo("org.openrewrite.kotlin.KotlinTypeGoat{name=parameterizedRecursive,return=org.openrewrite.kotlin.PT<org.openrewrite.kotlin.PT<org.openrewrite.kotlin.C>>,parameters=[org.openrewrite.kotlin.PT<org.openrewrite.kotlin.PT<org.openrewrite.kotlin.C>>]}");
    }

    @Test
    void generic() {
        assertThat(firstMethodParameterSignature("generic"))
                .isEqualTo("org.openrewrite.kotlin.PT<Generic{? extends org.openrewrite.kotlin.C}>");
        assertThat(methodSignature("generic"))
                .isEqualTo("org.openrewrite.kotlin.KotlinTypeGoat{name=generic,return=org.openrewrite.kotlin.PT<Generic{? extends org.openrewrite.kotlin.C}>,parameters=[org.openrewrite.kotlin.PT<Generic{? extends org.openrewrite.kotlin.C}>]}");
    }

    @Test
    void genericT() {
        assertThat(firstMethodParameterSignature("genericT"))
                .isEqualTo("Generic{T}");
        assertThat(methodSignature("genericT"))
                .isEqualTo("org.openrewrite.kotlin.KotlinTypeGoat{name=genericT,return=Generic{T},parameters=[Generic{T}]}");
    }

    @Test
    void genericContravariant() {
        assertThat(firstMethodParameterSignature("genericContravariant"))
                .isEqualTo("org.openrewrite.kotlin.PT<Generic{? super org.openrewrite.kotlin.C}>");
        assertThat(methodSignature("genericContravariant"))
                .isEqualTo("org.openrewrite.kotlin.KotlinTypeGoat{name=genericContravariant,return=org.openrewrite.kotlin.PT<Generic{? super org.openrewrite.kotlin.C}>,parameters=[org.openrewrite.kotlin.PT<Generic{? super org.openrewrite.kotlin.C}>]}");
    }

    @Test
    void genericUnbounded() {
        assertThat(firstMethodParameterSignature("genericUnbounded"))
                .isEqualTo("org.openrewrite.kotlin.PT<Generic{U}>");
        assertThat(methodSignature("genericUnbounded"))
                .isEqualTo("org.openrewrite.kotlin.KotlinTypeGoat{name=genericUnbounded,return=org.openrewrite.kotlin.PT<Generic{U}>,parameters=[org.openrewrite.kotlin.PT<Generic{U}>]}");
    }

    @Test
    void innerClass() {
        assertThat(firstMethodParameterSignature("inner"))
                .isEqualTo("org.openrewrite.kotlin.C$Inner");
        assertThat(methodSignature("inner"))
                .isEqualTo("org.openrewrite.kotlin.KotlinTypeGoat{name=inner,return=kotlin.Unit,parameters=[org.openrewrite.kotlin.C$Inner]}");
    }

    @Test
    void inheritedKotlinTypeGoat() {
        assertThat(firstMethodParameterSignature("inheritedKotlinTypeGoat"))
                .isEqualTo("org.openrewrite.kotlin.KotlinTypeGoat$InheritedKotlinTypeGoat<Generic{T}, Generic{U extends org.openrewrite.kotlin.PT<Generic{U}> & org.openrewrite.kotlin.C}>");
        assertThat(methodSignature("inheritedKotlinTypeGoat"))
                .isEqualTo("org.openrewrite.kotlin.KotlinTypeGoat{name=inheritedKotlinTypeGoat,return=org.openrewrite.kotlin.KotlinTypeGoat$InheritedKotlinTypeGoat<Generic{T}, Generic{U extends org.openrewrite.kotlin.PT<Generic{U}> & org.openrewrite.kotlin.C}>,parameters=[org.openrewrite.kotlin.KotlinTypeGoat$InheritedKotlinTypeGoat<Generic{T}, Generic{U extends org.openrewrite.kotlin.PT<Generic{U}> & org.openrewrite.kotlin.C}>]}");
    }

    @Disabled("Requires reference of type params from parent class")
    @Test
    void extendsJavaTypeGoat() {
        assertThat(innerClassSignature("ExtendsKotlinTypeGoat"))
                .isEqualTo("org.openrewrite.kotlin.KotlinTypeGoat$ExtendsKotlinTypeGoat");
    }

    @Test
    void genericIntersection() {
        assertThat(firstMethodParameterSignature("genericIntersection"))
                .isEqualTo("Generic{U extends org.openrewrite.kotlin.KotlinTypeGoat$TypeA & org.openrewrite.kotlin.PT<Generic{U}> & org.openrewrite.kotlin.C}");
        assertThat(methodSignature("genericIntersection"))
                .isEqualTo("org.openrewrite.kotlin.KotlinTypeGoat{name=genericIntersection,return=Generic{U extends org.openrewrite.kotlin.KotlinTypeGoat$TypeA & org.openrewrite.kotlin.PT<Generic{U}> & org.openrewrite.kotlin.C},parameters=[Generic{U extends org.openrewrite.kotlin.KotlinTypeGoat$TypeA & org.openrewrite.kotlin.PT<Generic{U}> & org.openrewrite.kotlin.C}]}");
    }

    @Test
    void recursiveIntersection() {
        assertThat(firstMethodParameterSignature("recursiveIntersection"))
                .isEqualTo("Generic{U extends org.openrewrite.kotlin.KotlinTypeGoat$Extension<Generic{U}> & org.openrewrite.kotlin.Intersection<Generic{U}>}");
        assertThat(methodSignature("recursiveIntersection"))
                .isEqualTo("org.openrewrite.kotlin.KotlinTypeGoat{name=recursiveIntersection,return=kotlin.Unit,parameters=[Generic{U extends org.openrewrite.kotlin.KotlinTypeGoat$Extension<Generic{U}> & org.openrewrite.kotlin.Intersection<Generic{U}>}]}");
    }

    @Test
    void genericRecursiveInClassDefinition() {
        assertThat(lastClassTypeParameter())
                .isEqualTo("Generic{S extends org.openrewrite.kotlin.PT<Generic{S}> & org.openrewrite.kotlin.C}");
    }

    @Test
    void genericRecursiveInMethodDeclaration() {
        assertThat(firstMethodParameterSignature("genericRecursive"))
                .isEqualTo("org.openrewrite.kotlin.KotlinTypeGoat<Generic{? extends kotlin.Array<Generic{U extends org.openrewrite.kotlin.KotlinTypeGoat<Generic{U}, Generic{?}>}>}, Generic{?}>");
        assertThat(methodSignature("genericRecursive"))
                .isEqualTo("org.openrewrite.kotlin.KotlinTypeGoat{name=genericRecursive,return=org.openrewrite.kotlin.KotlinTypeGoat<Generic{? extends kotlin.Array<Generic{U extends org.openrewrite.kotlin.KotlinTypeGoat<Generic{U}, Generic{?}>}>}, Generic{?}>,parameters=[org.openrewrite.kotlin.KotlinTypeGoat<Generic{? extends kotlin.Array<Generic{U extends org.openrewrite.kotlin.KotlinTypeGoat<Generic{U}, Generic{?}>}>}, Generic{?}>]}");
    }

    @Test
    void javaReference() {
        assertThat(firstMethodParameterSignature("javaType"))
          .isEqualTo("java.lang.Object");
        assertThat(methodSignature("javaType"))
          .isEqualTo("org.openrewrite.kotlin.KotlinTypeGoat{name=javaType,return=kotlin.Unit,parameters=[java.lang.Object]}");
    }

    @Test
    void receiver() {
        assertThat(firstMethodParameterSignature("receiver"))
                .isEqualTo("org.openrewrite.kotlin.C");
        assertThat(methodSignature("receiver"))
                .isEqualTo("org.openrewrite.kotlin.KotlinTypeGoat{name=receiver,return=kotlin.Unit,parameters=[org.openrewrite.kotlin.KotlinTypeGoat$TypeA,org.openrewrite.kotlin.C]}");
    }
}
