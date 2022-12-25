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

import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.java.tree.Flag;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.util.List;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.tree.JavaType.GenericTypeVariable.Variance.*;

/**
 * Based on type attribution mappings of [JavaTypeGoat].
 */
@SuppressWarnings("ConstantConditions")
public interface JavaTypeMappingTest {
    JavaType.FullyQualified classType(String fqn);

    default JavaType.Parameterized goatType() {
        return requireNonNull(TypeUtils.asParameterized(classType("org.openrewrite.java.JavaTypeGoat")));
    }

    default JavaType.Method methodType(String methodName) {
        JavaType.Method type = goatType().getMethods().stream()
                .filter(m -> m.getName().equals(methodName))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Expected to find matching method named " + methodName));
        assertThat(type.getDeclaringType().toString()).isEqualTo("org.openrewrite.java.JavaTypeGoat");
        return type;
    }

    default JavaType firstMethodParameter(String methodName) {
        return methodType(methodName).getParameterTypes().get(0);
    }

    @Test
    default void javaLangObjectHasNoSupertype() {
        assertThat(goatType().getSupertype().getSupertype()).isNull();
    }

    @Test
    default void interfacesContainImplicitAbstractFlag() {
        JavaType.Class clazz = (JavaType.Class) firstMethodParameter("clazz");
        JavaType.Method methodType = methodType("clazz");
        assertThat(clazz.getFlags()).contains(Flag.Abstract);
        assertThat(methodType.getFlags()).contains(Flag.Abstract);
    }

    @Test
    default void extendsJavaLangObject() {
        // even though it is implicit in the source code...
        assertThat(goatType().getSupertype().getFullyQualifiedName()).isEqualTo("java.lang.Object");
    }

    @Test
    default void constructor() {
        JavaType.Method ctor = methodType("<constructor>");
        assertThat(ctor.getDeclaringType().getFullyQualifiedName()).isEqualTo("org.openrewrite.java.JavaTypeGoat");
    }

    @Test
    default void array() {
        JavaType.Array arr = (JavaType.Array) firstMethodParameter("array");
        assertThat(TypeUtils.asArray(arr.getElemType())).isNotNull();
        assertThat(TypeUtils.asFullyQualified(TypeUtils.asArray(arr.getElemType()).getElemType()).getFullyQualifiedName()).isEqualTo("org.openrewrite.java.C");
    }

    @Test
    default void className() {
        JavaType.Class clazz = (JavaType.Class) firstMethodParameter("clazz");
        assertThat(TypeUtils.asFullyQualified(clazz).getFullyQualifiedName()).isEqualTo("org.openrewrite.java.C");
    }

    @Test
    default void primitive() {
        JavaType.Primitive primitive = (JavaType.Primitive) firstMethodParameter("primitive");
        assertThat(primitive).isSameAs(JavaType.Primitive.Int);
    }

    @Test
    default void parameterized() {
        JavaType.Parameterized parameterized = (JavaType.Parameterized) firstMethodParameter("parameterized");
        assertThat(parameterized.getType().getFullyQualifiedName()).isEqualTo("org.openrewrite.java.PT");
        assertThat(TypeUtils.asFullyQualified(parameterized.getTypeParameters().get(0)).getFullyQualifiedName()).isEqualTo("org.openrewrite.java.C");
    }

    @Test
    default void generic() {
        JavaType.GenericTypeVariable generic = (JavaType.GenericTypeVariable) TypeUtils.asParameterized(firstMethodParameter("generic")).getTypeParameters().get(0);
        assertThat(generic.getName()).isEqualTo("?");
        assertThat(generic.getVariance()).isEqualTo(COVARIANT);
        assertThat(TypeUtils.asFullyQualified(generic.getBounds().get(0)).getFullyQualifiedName()).isEqualTo("org.openrewrite.java.C");
    }

    @Test
    default void genericContravariant() {
        JavaType.GenericTypeVariable generic = (JavaType.GenericTypeVariable) TypeUtils.asParameterized(firstMethodParameter("genericContravariant")).getTypeParameters().get(0);
        assertThat(generic.getName()).isEqualTo("?");
        assertThat(generic.getVariance()).isEqualTo(CONTRAVARIANT);
        assertThat(TypeUtils.asFullyQualified(generic.getBounds().get(0)).getFullyQualifiedName()).
                isEqualTo("org.openrewrite.java.C");
    }

    @Test
    default void genericMultipleBounds() {
        List<JavaType> typeParameters = goatType().getTypeParameters();
        JavaType.GenericTypeVariable generic = (JavaType.GenericTypeVariable) typeParameters.get(typeParameters.size() - 1);
        assertThat(generic.getName()).isEqualTo("S");
        assertThat(generic.getVariance()).isEqualTo(COVARIANT);
        assertThat(TypeUtils.asFullyQualified(generic.getBounds().get(0)).getFullyQualifiedName()).isEqualTo("org.openrewrite.java.PT");
        assertThat(TypeUtils.asFullyQualified(generic.getBounds().get(1)).getFullyQualifiedName()).
                isEqualTo("org.openrewrite.java.C");
    }

    @Test
    default void genericUnbounded() {
        JavaType.GenericTypeVariable generic = (JavaType.GenericTypeVariable) TypeUtils.asParameterized(firstMethodParameter("genericUnbounded")).getTypeParameters().get(0);
        assertThat(generic.getName()).isEqualTo("U");
        assertThat(generic.getVariance()).isEqualTo(INVARIANT);
        assertThat(generic.getBounds()).isEmpty();
    }

    @Test
    default void genericRecursive() {
        JavaType.Parameterized param = (JavaType.Parameterized) firstMethodParameter("genericRecursive");
        JavaType typeParam = param.getTypeParameters().get(0);
        JavaType.GenericTypeVariable generic = (JavaType.GenericTypeVariable) typeParam;
        assertThat(generic.getName()).isEqualTo("?");
        assertThat(generic.getVariance()).isEqualTo(COVARIANT);
        assertThat(TypeUtils.asArray(generic.getBounds().get(0))).isNotNull();

        JavaType.GenericTypeVariable elemType = (JavaType.GenericTypeVariable) TypeUtils.asArray(generic.getBounds().get(0)).getElemType();
        assertThat(elemType.getName()).isEqualTo("U");
        assertThat(elemType.getVariance()).isEqualTo(COVARIANT);
        assertThat(elemType.getBounds()).hasSize(1);
    }

    @Test
    default void genericArray() {
        JavaType.Array arr = (JavaType.Array) firstMethodParameter("genericArray");
        JavaType.Parameterized parameterized = (JavaType.Parameterized) arr.getElemType();

        assertThat(parameterized).isNotNull();
        assertThat(parameterized.getType().getFullyQualifiedName()).isEqualTo("org.openrewrite.java.PT");
        assertThat(TypeUtils.asFullyQualified(parameterized.getTypeParameters().get(0)).getFullyQualifiedName()).isEqualTo("org.openrewrite.java.C");
    }

    @Test
    default void innerClass() {
        JavaType.FullyQualified clazz = TypeUtils.asFullyQualified(firstMethodParameter("inner"));
        assertThat(clazz.getFullyQualifiedName()).isEqualTo("org.openrewrite.java.C$Inner");
    }

    @Test
    default void inheritedJavaTypeGoat() {
        JavaType.Parameterized clazz = (JavaType.Parameterized) firstMethodParameter("inheritedJavaTypeGoat");
        assertThat(clazz.getTypeParameters().get(0).toString()).isEqualTo("Generic{T}");
        assertThat(clazz.getTypeParameters().get(1).toString()).isEqualTo("Generic{U extends org.openrewrite.java.PT<Generic{U}> & org.openrewrite.java.C}");
        assertThat(clazz.toString()).isEqualTo("org.openrewrite.java.JavaTypeGoat$InheritedJavaTypeGoat<Generic{T}, Generic{U extends org.openrewrite.java.PT<Generic{U}> & org.openrewrite.java.C}>");
    }

    @Issue("https://github.com/openrewrite/rewrite/pull/1365")
    @Test
    default void genericIntersectionType() {
        JavaType.GenericTypeVariable clazz = (JavaType.GenericTypeVariable) firstMethodParameter("genericIntersection");
        assertThat(clazz.getBounds().get(0).toString()).isEqualTo("org.openrewrite.java.JavaTypeGoat$TypeA");
        assertThat(clazz.getBounds().get(1).toString()).isEqualTo("org.openrewrite.java.PT<Generic{U extends org.openrewrite.java.JavaTypeGoat$TypeA & org.openrewrite.java.C}>");
        assertThat(clazz.getBounds().get(2).toString()).isEqualTo("org.openrewrite.java.C");
        assertThat(clazz.toString()).isEqualTo("Generic{U extends org.openrewrite.java.JavaTypeGoat$TypeA & org.openrewrite.java.PT<Generic{U}> & org.openrewrite.java.C}");
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1349")
    @Test
    default void enumTypeA() {
        JavaType.Class clazz = (JavaType.Class) firstMethodParameter("enumTypeA");
        JavaType.Method type = clazz.getMethods().stream()
                .filter(m -> m.getName().equals("<constructor>"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No constructor found"));
        assertThat(type.toString()).isEqualTo("org.openrewrite.java.JavaTypeGoat$EnumTypeA{name=<constructor>,return=org.openrewrite.java.JavaTypeGoat$EnumTypeA,parameters=[]}");

        JavaType.FullyQualified supertype = clazz.getSupertype();
        assertThat(supertype).isNotNull();
        assertThat(supertype.toString()).isEqualTo("java.lang.Enum<org.openrewrite.java.JavaTypeGoat$EnumTypeA>");
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Issue("https://github.com/openrewrite/rewrite/pull/1453")
    @Test
    default void enumTypeB() {
        JavaType.Class clazz = (JavaType.Class) firstMethodParameter("enumTypeB");
        JavaType.Method type = clazz.getMethods().stream()
                .filter(m -> m.getName().equals("<constructor>"))
                .findFirst()
                .get();
        assertThat(type.toString()).isEqualTo("org.openrewrite.java.JavaTypeGoat$EnumTypeB{name=<constructor>,return=org.openrewrite.java.JavaTypeGoat$EnumTypeB,parameters=[org.openrewrite.java.JavaTypeGoat$TypeA]}");

        JavaType.FullyQualified supertype = clazz.getSupertype();
        assertThat(supertype).isNotNull();
        assertThat(supertype.toString()).isEqualTo("java.lang.Enum<org.openrewrite.java.JavaTypeGoat$EnumTypeB>");
    }

    @Test
    default void ignoreSourceRetentionAnnotations() {
        JavaType.Parameterized goat = goatType();
        assertThat(goat.getAnnotations().size()).isEqualTo(1);
        assertThat(goat.getAnnotations().get(0).getClassName()).isEqualTo("AnnotationWithRuntimeRetention");

        JavaType.Method clazzMethod = methodType("clazz");
        assertThat(clazzMethod.getAnnotations().size()).isEqualTo(1);
        assertThat(clazzMethod.getAnnotations().get(0).getClassName()).isEqualTo("AnnotationWithRuntimeRetention");
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1367")
    @Test
    default void recursiveIntersection() {
        JavaType.GenericTypeVariable clazz = TypeUtils.asGeneric(firstMethodParameter("recursiveIntersection"));
        assertThat(clazz.toString()).isEqualTo("Generic{U extends org.openrewrite.java.JavaTypeGoat$Extension<Generic{U}> & org.openrewrite.java.Intersection<Generic{U}>}");
    }
}
