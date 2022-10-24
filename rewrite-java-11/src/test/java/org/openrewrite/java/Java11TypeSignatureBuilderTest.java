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
package org.openrewrite.java;

import org.junit.jupiter.api.BeforeAll;
import org.openrewrite.internal.lang.Nullable;

import java.lang.reflect.Constructor;

@SuppressWarnings("ConstantConditions")
public class Java11TypeSignatureBuilderTest implements JavaTypeSignatureBuilderTest {

    @Nullable
    private static JavaTypeSignatureBuilderTest delegate;

    @BeforeAll
    static void setup() {
        ClassLoader moduleClassLoader = new Java11UnrestrictedClassLoader(Java11TypeSignatureBuilderTest.class.getClassLoader());

        try {
            Class<?> typeGoatBuilder = Class.forName("org.openrewrite.java.isolated.Java11TypeSignatureBuilderTestDelegate", true, moduleClassLoader);
            Constructor<?> typeGoatBuilderConstructor = typeGoatBuilder.getDeclaredConstructor();
            typeGoatBuilderConstructor.setAccessible(true);
            delegate = (JavaTypeSignatureBuilderTest) typeGoatBuilderConstructor.newInstance();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to construct JavaTypeGoatBuilder.", e);
        }
    }

    @Override
    public String fieldSignature(String field) {
        return delegate.fieldSignature(field);
    }

    @Override
    public String methodSignature(String methodName) {
        return delegate.methodSignature(methodName);
    }

    @Override
    public String constructorSignature() {
        return delegate.constructorSignature();
    }

    @Override
    public Object firstMethodParameter(String methodName) {
        return delegate.firstMethodParameter(methodName);
    }

    @Override
    public Object innerClassSignature(String innerClassSimpleName) {
        return delegate.innerClassSignature(innerClassSimpleName);
    }

    @Override
    public Object lastClassTypeParameter() {
        return delegate.lastClassTypeParameter();
    }

    @Override
    public JavaTypeSignatureBuilder signatureBuilder() {
        return delegate.signatureBuilder();
    }
}
