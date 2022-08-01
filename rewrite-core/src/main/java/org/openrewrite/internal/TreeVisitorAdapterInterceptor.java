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
package org.openrewrite.internal;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.NamedElement;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static net.bytebuddy.matcher.ElementMatchers.*;

public class TreeVisitorAdapterInterceptor<T> {
    private final T delegate;
    private final Map<Method, Method> interceptedMethods;

    TreeVisitorAdapterInterceptor(T delegate) {
        this.delegate = Objects.requireNonNull(delegate);
        this.interceptedMethods = new HashMap<>(delegate.getClass().getDeclaredMethods().length);
    }

    @RuntimeType
    public Object intercept(@Origin Method method, @AllArguments Object[] args) throws Throwable {
        Method intercepted = interceptedMethods.computeIfAbsent(method, m -> {
            nextMethod:
            for (Method candidateMethod : delegate.getClass().getMethods()) {
                if (candidateMethod.getName().equals(method.getName())) {
                    if (candidateMethod.getParameterCount() == method.getParameterCount()) {
                        for (int i = 0; i < candidateMethod.getParameterTypes().length; i++) {
                            Class<?> candidateParameter = candidateMethod.getParameterTypes()[i];
                            if (!candidateParameter.isAssignableFrom(method.getParameterTypes()[i])) {
                                continue nextMethod;
                            }
                        }
                        candidateMethod.setAccessible(true);
                        return candidateMethod;
                    }
                }
            }
            throw new IllegalStateException("Expected to find matching method for " + method);
        });

        return intercepted.invoke(delegate, args);
    }

    public static <T, Adapted> Adapted adapt(T delegate, Class<Adapted> adaptTo) {
        ElementMatcher.Junction<NamedElement> methodNames = null;
        for (Method declaredMethod : delegate.getClass().getDeclaredMethods()) {
            if ((declaredMethod.getModifiers() & Modifier.PUBLIC) > 0 &&
                    (declaredMethod.getName().startsWith("visit") || declaredMethod.getName().endsWith("Visit"))) {
                methodNames = methodNames == null ? named(declaredMethod.getName()) :
                        methodNames.or(named(declaredMethod.getName()));
            }
        }

        if (methodNames == null) {
            methodNames = ElementMatchers.none();
        }

        DynamicType.Builder<Adapted> classBuilder = new ByteBuddy().subclass(adaptTo);

        if (!delegate.getClass().isAnonymousClass()) {
            classBuilder = classBuilder.name(delegate.getClass().getSimpleName().trim() + "_" + adaptTo.getSimpleName());
        }

        DynamicType.Unloaded<?> unloaded = classBuilder
                .method(isPublic().and(methodNames))
                .intercept(MethodDelegation.to(new TreeVisitorAdapterInterceptor<>(delegate)))
                .make();

        // for debugging class generation issues
//        try {
//            java.io.File adapted = new java.io.File("adapted");
//            unloaded.saveIn(adapted);
//        } catch (java.io.IOException e) {
//            throw new RuntimeException(e);
//        }

        Class<?> adapted = unloaded.load(delegate.getClass().getClassLoader()).getLoaded();

        try {
            //noinspection unchecked
            return (Adapted) adapted.getDeclaredConstructor().newInstance();
        } catch (InvocationTargetException | NoSuchMethodException | InstantiationException |
                 IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
