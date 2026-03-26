/*
 * Copyright 2024 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.csharp.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.tree.JavaType;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CSharpTypeUtilsTest {

    @Test
    void nullableValueType() {
        JavaType.Class intType = JavaType.ShallowClass.build("System.Int32");
        JavaType.Parameterized nullableInt = new JavaType.Parameterized(
                null,
                JavaType.ShallowClass.build("System.Nullable"),
                List.of(intType)
        );
        assertThat(CSharpTypeUtils.isNullableType(nullableInt)).isTrue();
    }

    @Test
    void nonNullableParameterizedType() {
        JavaType.Class stringType = JavaType.ShallowClass.build("System.String");
        JavaType.Parameterized listOfString = new JavaType.Parameterized(
                null,
                JavaType.ShallowClass.build("System.Collections.Generic.List"),
                List.of(stringType)
        );
        assertThat(CSharpTypeUtils.isNullableType(listOfString)).isFalse();
    }

    @Test
    void plainClassType() {
        JavaType.Class stringType = JavaType.ShallowClass.build("System.String");
        assertThat(CSharpTypeUtils.isNullableType(stringType)).isFalse();
    }

    @Test
    void primitiveType() {
        assertThat(CSharpTypeUtils.isNullableType(JavaType.Primitive.Int)).isFalse();
    }

    @Test
    void nullType() {
        assertThat(CSharpTypeUtils.isNullableType(null)).isFalse();
    }
}
