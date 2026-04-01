/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.java.internal.parser;

import org.openrewrite.ExecutionContext;
import org.openrewrite.java.JavaParser;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * This class is used to find the caller of {@link JavaParser#dependenciesFromResources(ExecutionContext, String...)},
 * which is used to load classpath resources for {@link JavaParser}.
 */
class JavaParserCaller {
    private JavaParserCaller() {
    }

    public static Class<?> findCaller() {
        Class<?> caller;
        try {
            // StackWalker is only available in Java 15+, but right now we only use classloader isolated
            // recipe instances in Java 17 environments, so we can safely use StackWalker there.
            Class<?> options = Class.forName("java.lang.StackWalker$Option");
            Object retainOption = options.getDeclaredField("RETAIN_CLASS_REFERENCE").get(null);

            Class<?> walkerClass = Class.forName("java.lang.StackWalker");
            Method getInstance = walkerClass.getDeclaredMethod("getInstance", options);
            Object walker = getInstance.invoke(null, retainOption);
            Method getDeclaringClass = Class.forName("java.lang.StackWalker$StackFrame").getDeclaredMethod("getDeclaringClass");
            caller = (Class<?>) walkerClass.getMethod("walk", Function.class).invoke(walker, (Function<Stream<Object>, Object>) s -> s
                    .map(f -> {
                        try {
                            return (Class<?>) getDeclaringClass.invoke(f);
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    // Drop anything before the parser or builder class, as well as those classes themselves
                    .filter(new Predicate<Class<?>>() {
                        boolean parserOrBuilderFound = false;

                        @Override
                        public boolean test(Class<?> c1) {
                            if (c1.getName().equals(JavaParser.class.getName()) ||
                                c1.getName().equals(JavaParser.Builder.class.getName()) ||
                                // FIXME this is a hack
                                "org.openrewrite.gradle.GradleParser".equals(c1.getName()) ||
                                "org.openrewrite.gradle.GradleParser$Builder".equals(c1.getName())) {
                                parserOrBuilderFound = true;
                                return false;
                            }
                            return parserOrBuilderFound;
                        }
                    })
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Unable to find caller of JavaParser.dependenciesFromResources(..)")));
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException |
                 NoSuchMethodException | InvocationTargetException e) {
            caller = JavaParser.class;
        }
        return caller;
    }
}
