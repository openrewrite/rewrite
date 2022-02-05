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
package org.openrewrite.java;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public interface JavaTypeSignatureBuilderTest {
    String fieldSignature(String field);

    String methodSignature(String methodName);

    String constructorSignature();

    /**
     * @param methodName The type of the first parameter of the method with this name.
     */
    Object firstMethodParameter(String methodName);

    Object innerClassSignature(String innerClassSimpleName);

    /**
     * The type of the type variable of the last type parameter of {@link JavaTypeGoat}.
     */
    Object lastClassTypeParameter();

    JavaTypeSignatureBuilder signatureBuilder();

    @Test
    default void constructor() {
        assertThat(constructorSignature())
                .isEqualTo("org.openrewrite.java.JavaTypeGoat{name=<constructor>,return=org.openrewrite.java.JavaTypeGoat,parameters=[]}");
    }

    @Test
    default void parameterizedField() {
        assertThat(fieldSignature("parameterizedField"))
                .isEqualTo("org.openrewrite.java.JavaTypeGoat{name=parameterizedField}");
    }

    @Test
    default void array() {
        assertThat(signatureBuilder().signature(firstMethodParameter("array")))
                .isEqualTo("org.openrewrite.java.C[][]");
        assertThat(methodSignature("array"))
                .isEqualTo("org.openrewrite.java.JavaTypeGoat{name=array,return=org.openrewrite.java.C[][],parameters=[org.openrewrite.java.C[][]]}");
    }

    @Test
    default void classSignature() {
        assertThat(signatureBuilder().signature(firstMethodParameter("clazz")))
                .isEqualTo("org.openrewrite.java.C");
        assertThat(methodSignature("clazz"))
                .isEqualTo("org.openrewrite.java.JavaTypeGoat{name=clazz,return=org.openrewrite.java.C,parameters=[org.openrewrite.java.C]}");
    }

    @Test
    default void primitive() {
        assertThat(signatureBuilder().signature(firstMethodParameter("primitive")))
                .isEqualTo("int");
        assertThat(methodSignature("primitive"))
                .isEqualTo("org.openrewrite.java.JavaTypeGoat{name=primitive,return=int,parameters=[int]}");
    }

    @Test
    default void parameterized() {
        assertThat(signatureBuilder().signature(firstMethodParameter("parameterized")))
                .isEqualTo("org.openrewrite.java.PT<org.openrewrite.java.C>");
        assertThat(methodSignature("parameterized"))
                .isEqualTo("org.openrewrite.java.JavaTypeGoat{name=parameterized,return=org.openrewrite.java.PT<org.openrewrite.java.C>,parameters=[org.openrewrite.java.PT<org.openrewrite.java.C>]}");
    }

    @Test
    default void parameterizedRecursive() {
        assertThat(signatureBuilder().signature(firstMethodParameter("parameterizedRecursive")))
                .isEqualTo("org.openrewrite.java.PT<org.openrewrite.java.PT<org.openrewrite.java.C>>");
        assertThat(methodSignature("parameterizedRecursive"))
                .isEqualTo("org.openrewrite.java.JavaTypeGoat{name=parameterizedRecursive,return=org.openrewrite.java.PT<org.openrewrite.java.PT<org.openrewrite.java.C>>,parameters=[org.openrewrite.java.PT<org.openrewrite.java.PT<org.openrewrite.java.C>>]}");
    }

    @Test
    default void generic() {
        assertThat(signatureBuilder().signature(firstMethodParameter("generic")))
                .isEqualTo("org.openrewrite.java.PT<Generic{extends org.openrewrite.java.C}>");
        assertThat(methodSignature("generic"))
                .isEqualTo("org.openrewrite.java.JavaTypeGoat{name=generic,return=org.openrewrite.java.PT<Generic{extends org.openrewrite.java.C}>,parameters=[org.openrewrite.java.PT<Generic{extends org.openrewrite.java.C}>]}");
    }

    // Remove test after signatures are working.
    // This test is to highlight that JavaTypeReflection returns java.lang.Object for unbound generics.
    @Test
    default void genericT() {
        assertThat(signatureBuilder().signature(firstMethodParameter("genericT")))
                .isEqualTo("Generic{}");
        assertThat(methodSignature("genericT"))
                .isEqualTo("org.openrewrite.java.JavaTypeGoat{name=genericT,return=Generic{},parameters=[Generic{}]}");
    }

    @Test
    default void genericContravariant() {
        assertThat(signatureBuilder().signature(firstMethodParameter("genericContravariant")))
            .isEqualTo("org.openrewrite.java.PT<Generic{super org.openrewrite.java.C}>");
        assertThat(methodSignature("genericContravariant"))
                .isEqualTo("org.openrewrite.java.JavaTypeGoat{name=genericContravariant,return=org.openrewrite.java.PT<Generic{super org.openrewrite.java.C}>,parameters=[org.openrewrite.java.PT<Generic{super org.openrewrite.java.C}>]}");
    }

    @Test
    default void genericRecursiveInClassDefinition() {
        assertThat(signatureBuilder().signature(lastClassTypeParameter()))
                .isEqualTo("Generic{extends org.openrewrite.java.PT<Generic{}> & org.openrewrite.java.C}");
    }

    @Test
    default void genericRecursiveInMethodDeclaration() {
        assertThat(signatureBuilder().signature(firstMethodParameter("genericRecursive")))
                .isEqualTo("org.openrewrite.java.JavaTypeGoat<Generic{extends Generic{extends org.openrewrite.java.JavaTypeGoat<Generic{}, Generic{}>}[]}, Generic{}>");
        assertThat(methodSignature("genericRecursive"))
                .isEqualTo("org.openrewrite.java.JavaTypeGoat{name=genericRecursive,return=org.openrewrite.java.JavaTypeGoat<Generic{extends Generic{extends org.openrewrite.java.JavaTypeGoat<Generic{}, Generic{}>}[]}, Generic{}>,parameters=[org.openrewrite.java.JavaTypeGoat<Generic{extends Generic{extends org.openrewrite.java.JavaTypeGoat<Generic{}, Generic{}>}[]}, Generic{}>]}");
    }

    @Test
    default void genericUnbounded() {
        assertThat(signatureBuilder().signature(firstMethodParameter("genericUnbounded")))
            .isEqualTo("org.openrewrite.java.PT<Generic{}>");
        assertThat(methodSignature("genericUnbounded"))
                .isEqualTo("org.openrewrite.java.JavaTypeGoat{name=genericUnbounded,return=org.openrewrite.java.PT<Generic{}>,parameters=[org.openrewrite.java.PT<Generic{}>]}");
    }

    @Test
    default void innerClass() {
        assertThat(signatureBuilder().signature(firstMethodParameter("inner")))
                .isEqualTo("org.openrewrite.java.C$Inner");
        assertThat(methodSignature("inner"))
                .isEqualTo("org.openrewrite.java.JavaTypeGoat{name=inner,return=org.openrewrite.java.C$Inner,parameters=[org.openrewrite.java.C$Inner]}");
    }

    @Disabled("Disabled until Classgraph is removed.")
    @Test
    default void inheritedJavaTypeGoat() {
        assertThat(signatureBuilder().signature(firstMethodParameter("inheritedJavaTypeGoat")))
                .isEqualTo("org.openrewrite.java.JavaTypeGoat$InheritedJavaTypeGoat<Generic{}, Generic{extends org.openrewrite.java.PT<Generic{}> & org.openrewrite.java.C}>");
        assertThat(methodSignature("inheritedJavaTypeGoat"))
                .isEqualTo("org.openrewrite.java.JavaTypeGoat{name=inheritedJavaTypeGoat,return=org.openrewrite.java.JavaTypeGoat$InheritedJavaTypeGoat<Generic{}, Generic{extends org.openrewrite.java.PT<Generic{}> & org.openrewrite.java.C}>,parameters=[org.openrewrite.java.JavaTypeGoat$InheritedJavaTypeGoat<Generic{}, Generic{extends org.openrewrite.java.PT<Generic{}> & org.openrewrite.java.C}>]}");
    }

    @Disabled("Disabled until Classgraph is removed.")
    @Test
    default void extendsJavaTypeGoat() {
        assertThat(signatureBuilder().signature(innerClassSignature("ExtendsJavaTypeGoat")))
                .isEqualTo("org.openrewrite.java.JavaTypeGoat$ExtendsJavaTypeGoat");
    }

    @Disabled("Disabled until Classgraph is removed.")
    @Test
    default void genericIntersection() {
        assertThat(signatureBuilder().signature(firstMethodParameter("genericIntersection")))
                .isEqualTo("Generic{extends org.openrewrite.java.JavaTypeGoat$TypeA & org.openrewrite.java.PT<Generic{}> & org.openrewrite.java.C}");
        assertThat(methodSignature("genericIntersection"))
                .isEqualTo("org.openrewrite.java.JavaTypeGoat{name=genericIntersection,return=Generic{extends org.openrewrite.java.JavaTypeGoat$TypeA & org.openrewrite.java.PT<Generic{}> & org.openrewrite.java.C},parameters=[Generic{extends org.openrewrite.java.JavaTypeGoat$TypeA & org.openrewrite.java.PT<Generic{}> & org.openrewrite.java.C}]}");
    }
}
