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

import org.openrewrite.java.JavaTypeGoat;
import org.openrewrite.java.JavaTypeSignatureBuilderTest;

import java.lang.reflect.TypeVariable;
import java.util.Arrays;

class JavaReflectionTypeSignatureBuilderTest implements JavaTypeSignatureBuilderTest {

    @Override
    public String fieldSignature(String field) {
        try {
            return signatureBuilder().variableSignature(JavaTypeGoat.class.getDeclaredField(field));
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String methodSignature(String methodName) {
        return signatureBuilder().methodSignature(Arrays.stream(JavaTypeGoat.class.getDeclaredMethods())
                .filter(it -> it.getName().equals(methodName)).findAny().orElseThrow(), "org.openrewrite.java.JavaTypeGoat");
    }

    @Override
    public String constructorSignature() {
        return signatureBuilder().methodSignature(JavaTypeGoat.class.getDeclaredConstructors()[0], "org.openrewrite.java.JavaTypeGoat");
    }

    @Override
    public Object firstMethodParameter(String methodName) {
        return Arrays.stream(JavaTypeGoat.class.getDeclaredMethods())
                .filter(it -> it.getName().equals(methodName))
                .findAny()
                .orElseThrow()
                .getGenericParameterTypes()[0];
    }

    @Override
    public Object innerClassSignature(String innerClassSimpleName) {
        return Arrays.stream(JavaTypeGoat.class.getDeclaredClasses())
                .filter(c -> c.getSimpleName().equals(innerClassSimpleName))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("Unable to find class " + innerClassSimpleName));
    }

    @Override
    public Object lastClassTypeParameter() {
        @SuppressWarnings("rawtypes") TypeVariable<Class<JavaTypeGoat>>[] tp =
                JavaTypeGoat.class.getTypeParameters();
        return tp[tp.length - 1];
    }

    @Override
    public JavaReflectionTypeSignatureBuilder signatureBuilder() {
        return new JavaReflectionTypeSignatureBuilder();
    }
}
