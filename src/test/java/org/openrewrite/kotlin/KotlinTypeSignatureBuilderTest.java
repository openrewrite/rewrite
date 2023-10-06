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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.kotlin.internal.CompiledSource;
import org.openrewrite.tree.ParsingExecutionContextView;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

public class KotlinTypeSignatureBuilderTest {
    private static final String goat = StringUtils.readFully(KotlinTypeSignatureBuilderTest.class.getResourceAsStream("/KotlinTypeGoat.kt"));

    private static final Disposable disposable = Disposer.newDisposable();
    private static final CompiledSource compiledSource = KotlinParser.builder()
            .logCompilationWarningsAndErrors(true)
            .moduleName("test")
            .build()
            .parse(singletonList(new Parser.Input(Paths.get("KotlinTypeGoat.kt"), () -> new ByteArrayInputStream(goat.getBytes(StandardCharsets.UTF_8)))), disposable,
                    new ParsingExecutionContextView(new InMemoryExecutionContext(Throwable::printStackTrace)));;

    @AfterAll
    static void afterAll() {
        Disposer.dispose(disposable);
    }

    public KotlinTypeSignatureBuilder signatureBuilder() {
        return new KotlinTypeSignatureBuilder(compiledSource.getFirSession());
    }

    private FirFile getCompiledSource() {
        FirFile file = compiledSource.getSources().iterator().next().getFirFile();
        assert file != null;
        return file;
    }

    public String constructorSignature() {
        return signatureBuilder().methodDeclarationSignature(getCompiledSource().getDeclarations().stream()
                .map(FirRegularClass.class::cast)
                .flatMap(it -> it.getDeclarations().stream())
                .filter(FirConstructor.class::isInstance)
                .map(FirFunction.class::cast)
                .findFirst()
                .orElseThrow()
                .getSymbol(), null);
    }

    public Object innerClassSignature(String innerClassSimpleName) {
        return signatureBuilder().signature(getCompiledSource().getDeclarations().stream()
                .map(FirRegularClass.class::cast)
                .flatMap(it -> it.getDeclarations().stream())
                .filter(FirRegularClass.class::isInstance)
                .map(FirRegularClass.class::cast)
                .filter(it -> innerClassSimpleName.equals(it.getName().asString()))
                .findFirst()
                .orElseThrow()
                .getSymbol());
    }

    public String fieldSignature(String field) {
        return signatureBuilder().variableSignature(getCompiledSource().getDeclarations().stream()
                .map(FirRegularClass.class::cast)
                .flatMap(it -> it.getDeclarations().stream())
                .filter(FirProperty.class::isInstance)
                .map(FirProperty.class::cast)
                .filter(it -> field.equals(it.getName().asString()))
                .findFirst()
                .orElseThrow()
                .getSymbol(), null);
    }

    @Nullable
    public FirProperty getProperty(String field) {
        return getCompiledSource().getDeclarations().stream()
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
        return signatureBuilder().methodDeclarationSignature(property.getGetter().getSymbol(), getCompiledSource().getSymbol());
    }

    public String fieldPropertySetterSignature(String field) {
        FirProperty property = getProperty(field);
        if (property == null || property.getSetter() == null) {
            throw new UnsupportedOperationException("No filed or setter for " + field);
        }
        return signatureBuilder().methodDeclarationSignature(property.getSetter().getSymbol(), getCompiledSource().getSymbol());
    }

    public Object firstMethodParameterSignature(String methodName) {
        return signatureBuilder().signature(getCompiledSource().getDeclarations().stream()
                .map(FirRegularClass.class::cast)
                .flatMap(it -> it.getDeclarations().stream())
                .filter(FirSimpleFunction.class::isInstance)
                .map(FirSimpleFunction.class::cast)
                .filter(it -> methodName.equals(it.getName().asString()))
                .findFirst()
                .orElseThrow()
                .getValueParameters()
                .get(0)
                .getReturnTypeRef());
    }

    public Object lastClassTypeParameter() {
        return signatureBuilder().signature(getCompiledSource().getDeclarations().stream()
                .map(FirRegularClass.class::cast)
                .findFirst()
                .orElseThrow()
                .getTypeParameters()
                .get(1));
    }

    public String methodSignature(String methodName) {
        return signatureBuilder().methodDeclarationSignature(getCompiledSource().getDeclarations().stream()
                .map(FirRegularClass.class::cast)
                .flatMap(it -> it.getDeclarations().stream())
                .filter(FirSimpleFunction.class::isInstance)
                .map(FirSimpleFunction.class::cast)
                .filter(it -> methodName.equals(it.getName().asString()))
                .findFirst()
                .orElseThrow()
                .getSymbol(), null);
    }

    @Test
    void constructor() {
        assertThat(constructorSignature())
                .isEqualTo("org.openrewrite.kotlin.KotlinTypeGoat{name=<constructor>,return=org.openrewrite.kotlin.KotlinTypeGoat,parameters=[]}");
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
        assertThat(fieldPropertyGetterSignature("gettableField"))
                .isEqualTo("org.openrewrite.kotlin.KotlinTypeGoat{name=accessor,return=kotlin.Int,parameters=[]}");
    }

    @Test
    void settableField() {
        assertThat(fieldPropertySetterSignature("settableField"))
          .isEqualTo("org.openrewrite.kotlin.KotlinTypeGoat{name=accessor,return=kotlin.Unit,parameters=[kotlin.String]}");
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
                .isEqualTo("org.openrewrite.kotlin.PT<Generic{out org.openrewrite.kotlin.C}>");
        assertThat(methodSignature("generic"))
                .isEqualTo("org.openrewrite.kotlin.KotlinTypeGoat{name=generic,return=org.openrewrite.kotlin.PT<Generic{out org.openrewrite.kotlin.C}>,parameters=[org.openrewrite.kotlin.PT<Generic{out org.openrewrite.kotlin.C}>]}");
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
                .isEqualTo("org.openrewrite.kotlin.PT<Generic{in org.openrewrite.kotlin.C}>");
        assertThat(methodSignature("genericContravariant"))
                .isEqualTo("org.openrewrite.kotlin.KotlinTypeGoat{name=genericContravariant,return=org.openrewrite.kotlin.PT<Generic{in org.openrewrite.kotlin.C}>,parameters=[org.openrewrite.kotlin.PT<Generic{in org.openrewrite.kotlin.C}>]}");
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

    @Disabled("Requires parsing intersection types")
    @Test
    void inheritedJavaTypeGoat() {
        assertThat(firstMethodParameterSignature("inheritedJavaTypeGoat"))
                .isEqualTo("org.openrewrite.java.JavaTypeGoat$InheritedJavaTypeGoat<Generic{T}, Generic{U extends org.openrewrite.java.PT<Generic{U}> & org.openrewrite.java.C}>");
        assertThat(methodSignature("inheritedJavaTypeGoat"))
                .isEqualTo("org.openrewrite.java.JavaTypeGoat{name=inheritedJavaTypeGoat,return=org.openrewrite.java.JavaTypeGoat$InheritedJavaTypeGoat<Generic{T}, Generic{U extends org.openrewrite.java.PT<Generic{U}> & org.openrewrite.java.C}>,parameters=[org.openrewrite.java.JavaTypeGoat$InheritedJavaTypeGoat<Generic{T}, Generic{U extends org.openrewrite.java.PT<Generic{U}> & org.openrewrite.java.C}>]}");
    }

    @Disabled("Requires reference of type params from parent class")
    @Test
    void extendsJavaTypeGoat() {
        assertThat(innerClassSignature("ExtendsKotlinTypeGoat"))
                .isEqualTo("org.openrewrite.kotlin.KotlinTypeGoat$ExtendsKotlinTypeGoat");
    }

    @Disabled("Requires parsing intersection types")
    @Test
    void genericIntersection() {
        assertThat(firstMethodParameterSignature("genericIntersection"))
                .isEqualTo("Generic{U extends org.openrewrite.java.JavaTypeGoat$TypeA & org.openrewrite.java.PT<Generic{U}> & org.openrewrite.java.C}");
        assertThat(methodSignature("genericIntersection"))
                .isEqualTo("org.openrewrite.java.JavaTypeGoat{name=genericIntersection,return=Generic{U extends org.openrewrite.java.JavaTypeGoat$TypeA & org.openrewrite.java.PT<Generic{U}> & org.openrewrite.java.C},parameters=[Generic{U extends org.openrewrite.java.JavaTypeGoat$TypeA & org.openrewrite.java.PT<Generic{U}> & org.openrewrite.java.C}]}");
    }

    @Disabled("Requires parsing intersection types")
    @Test
    void recursiveIntersection() {
        assertThat(firstMethodParameterSignature("recursiveIntersection"))
                .isEqualTo("Generic{U extends org.openrewrite.java.JavaTypeGoat$Extension<Generic{U}> & org.openrewrite.java.Intersection<Generic{U}>}");
        assertThat(methodSignature("recursiveIntersection"))
                .isEqualTo("org.openrewrite.java.JavaTypeGoat{name=recursiveIntersection,return=void,parameters=[Generic{U extends org.openrewrite.java.JavaTypeGoat$Extension<Generic{U}> & org.openrewrite.java.Intersection<Generic{U}>}]}");
    }

    @Disabled
    @Test
    void genericRecursiveInClassDefinition() {
        assertThat(lastClassTypeParameter())
                .isEqualTo("Generic{S in org.openrewrite.kotlin.PT<Generic{S}> & org.openrewrite.kotlin.C}");
    }

    @Disabled
    @Test
    void genericRecursiveInMethodDeclaration() {
        // <U : KotlinTypeGoat<U, *>> genericRecursive(n: KotlinTypeGoat<out Array<U>, *>): KotlinTypeGoat<out Array<U>, *>
        assertThat(firstMethodParameterSignature("genericRecursive"))
                .isEqualTo("org.openrewrite.kotlin.KotlinTypeGoat<Generic{ extends kotlin.Array<Generic{U}>, Generic{*}>");
        assertThat(methodSignature("genericRecursive"))
                .isEqualTo("org.openrewrite.kotlin.KotlinTypeGoat{name=genericRecursive,return=org.openrewrite.kotlin.KotlinTypeGoat<Generic{? extends Generic{U extends org.openrewrite.kotlin.KotlinTypeGoat<Generic{U}, Generic{?}>}[]}, Generic{?}>,parameters=[org.openrewrite.kotlin.KotlinTypeGoat<Generic{? extends Generic{U extends org.openrewrite.kotlin.KotlinTypeGoat<Generic{U}, Generic{?}>}[]}, Generic{?}>]}");
    }

    @Test
    void javaReference() {
        assertThat(firstMethodParameterSignature("javaType"))
          .isEqualTo("java.lang.Object");
        assertThat(methodSignature("javaType"))
          .isEqualTo("org.openrewrite.kotlin.KotlinTypeGoat{name=javaType,return=kotlin.Unit,parameters=[java.lang.Object]}");
    }
}
