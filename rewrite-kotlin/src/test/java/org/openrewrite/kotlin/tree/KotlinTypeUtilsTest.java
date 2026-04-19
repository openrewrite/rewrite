/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.kotlin.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import static org.assertj.core.api.Assertions.assertThat;

class KotlinTypeUtilsTest {

    @Test
    void jvmFqnFromKotlinFqn() {
        assertThat(KotlinTypeUtils.toJvmFqn("kotlin.Any")).isEqualTo("java.lang.Object");
        assertThat(KotlinTypeUtils.toJvmFqn("kotlin.String")).isEqualTo("java.lang.String");
        assertThat(KotlinTypeUtils.toJvmFqn("kotlin.collections.List")).isEqualTo("java.util.List");
        assertThat(KotlinTypeUtils.toJvmFqn("kotlin.annotation.MustBeDocumented"))
          .isEqualTo("java.lang.annotation.Documented");
        // Passes through unrecognised names unchanged.
        assertThat(KotlinTypeUtils.toJvmFqn("com.acme.MyType")).isEqualTo("com.acme.MyType");
    }

    @Test
    void kotlinFqnFromJvmFqn() {
        assertThat(KotlinTypeUtils.toKotlinFqn("java.lang.Object")).isEqualTo("kotlin.Any");
        assertThat(KotlinTypeUtils.toKotlinFqn("java.lang.String")).isEqualTo("kotlin.String");
        assertThat(KotlinTypeUtils.toKotlinFqn("com.acme.MyType")).isEqualTo("com.acme.MyType");
    }

    /**
     * After {@link org.openrewrite.kotlin.KotlinTypeMapping} remaps {@code kotlin.String}
     * to {@code java.lang.String}, a recipe author reasoning in Kotlin terms can still
     * match by passing {@code "kotlin.String"} — the utility recognises it as an alias.
     */
    @Test
    void isOfClassTypeAcceptsKotlinAlias() {
        JavaType stringType = JavaType.ShallowClass.build("java.lang.String");
        // Baseline: the type carries the JVM FQN.
        assertThat(TypeUtils.isOfClassType(stringType, "java.lang.String")).isTrue();
        // Kotlin FQN works as an alias.
        assertThat(KotlinTypeUtils.isOfClassType(stringType, "kotlin.String")).isTrue();
        assertThat(KotlinTypeUtils.isOfClassType(stringType, "java.lang.String")).isTrue();
        // Unrelated types don't match either alias.
        assertThat(KotlinTypeUtils.isOfClassType(stringType, "kotlin.Int")).isFalse();
        assertThat(KotlinTypeUtils.isOfClassType(stringType, "java.util.List")).isFalse();
    }

    @Test
    void isOfClassTypeOnNullIsFalse() {
        assertThat(KotlinTypeUtils.isOfClassType(null, "kotlin.String")).isFalse();
        assertThat(KotlinTypeUtils.isOfClassType(null, "java.lang.String")).isFalse();
    }

    @Test
    void kotlinPrimitiveHelpersMatchBothPrimitiveAndBoxed() {
        assertThat(KotlinTypeUtils.isKotlinInt(JavaType.Primitive.Int)).isTrue();
        assertThat(KotlinTypeUtils.isKotlinInt(JavaType.ShallowClass.build("java.lang.Integer"))).isTrue();
        assertThat(KotlinTypeUtils.isKotlinInt(JavaType.Primitive.Long)).isFalse();
        assertThat(KotlinTypeUtils.isKotlinInt(null)).isFalse();

        assertThat(KotlinTypeUtils.isKotlinBoolean(JavaType.Primitive.Boolean)).isTrue();
        assertThat(KotlinTypeUtils.isKotlinBoolean(JavaType.ShallowClass.build("java.lang.Boolean"))).isTrue();

        assertThat(KotlinTypeUtils.isKotlinLong(JavaType.Primitive.Long)).isTrue();
        assertThat(KotlinTypeUtils.isKotlinLong(JavaType.ShallowClass.build("java.lang.Long"))).isTrue();

        assertThat(KotlinTypeUtils.isKotlinShort(JavaType.Primitive.Short)).isTrue();
        assertThat(KotlinTypeUtils.isKotlinByte(JavaType.Primitive.Byte)).isTrue();
        assertThat(KotlinTypeUtils.isKotlinFloat(JavaType.Primitive.Float)).isTrue();
        assertThat(KotlinTypeUtils.isKotlinDouble(JavaType.Primitive.Double)).isTrue();

        assertThat(KotlinTypeUtils.isKotlinChar(JavaType.Primitive.Char)).isTrue();
        assertThat(KotlinTypeUtils.isKotlinChar(JavaType.ShallowClass.build("java.lang.Character"))).isTrue();

        // Unit returned from a function maps to JVM void.
        assertThat(KotlinTypeUtils.isKotlinUnit(JavaType.Primitive.Void)).isTrue();
    }

    @Test
    void isAnyMatchesObjectRegardlessOfSpelling() {
        assertThat(KotlinTypeUtils.isAny(JavaType.ShallowClass.build("java.lang.Object"))).isTrue();
        assertThat(KotlinTypeUtils.isAny(JavaType.ShallowClass.build("java.lang.String"))).isFalse();
        assertThat(KotlinTypeUtils.isAny(null)).isFalse();
    }
}
