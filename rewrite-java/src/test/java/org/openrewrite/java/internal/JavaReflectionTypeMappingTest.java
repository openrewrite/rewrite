/*
 * Copyright 2022 the original author or authors.
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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaTypeGoat;
import org.openrewrite.java.JavaTypeMappingTest;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

@SuppressWarnings("ConstantConditions")
class JavaReflectionTypeMappingTest implements JavaTypeMappingTest {
    JavaReflectionTypeMapping typeMapping = new JavaReflectionTypeMapping(new JavaTypeCache());
    JavaType.Parameterized goat = TypeUtils.asParameterized(typeMapping.type(JavaTypeGoat.class));

    @Override
    public JavaType.Parameterized goatType() {
        return goat;
    }

    @Override
    public JavaType.FullyQualified classType(String fqn) {
        try {
            return TypeUtils.asFullyQualified(typeMapping.type(Class.forName(fqn)));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    // Tests for enum supertypes are disabled in JavaReflection, because through reflection the supertype will be based on the byte code.
    // In byte code, the supertype of an `enum` instanceof Java.lang.Enum<E extends java.lang.Enum<E>>.
    // However, the Javac compiler will type attribute the generic type of `E`, which is more accurated.
    // I.E. From the Javac compiler, the JavaTypeGoat$EnumTypeA will have a supertype of `java.lang.Enum<org.openrewrite.java.JavaTypeGoat$EnumTypeA>`.
    @Disabled
    @Test
    @Override
    public void enumTypeA() {
    }

    @Disabled
    @Test
    @Override
    public void enumTypeB() {
    }
}
