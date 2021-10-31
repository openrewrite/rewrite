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
package org.openrewrite.groovy;

import lombok.RequiredArgsConstructor;
import org.codehaus.groovy.GroovyBugError;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.GenericsType;
import org.codehaus.groovy.ast.MethodNode;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.cache.JavaTypeCache;
import org.openrewrite.java.tree.Flag;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

@RequiredArgsConstructor
class TypeMapping {
    private static final int KIND_BITMASK_INTERFACE = 1 << 9;
    private static final int KIND_BITMASK_ANNOTATION = 1 << 13;
    private static final int KIND_BITMASK_ENUM = 1 << 14;

    private final JavaTypeCache typeCache;

    @Nullable
    public JavaType.Method type(@Nullable MethodNode node) {
        if (node == null) {
            return null;
        }

        List<String> argumentTypeSignatures = emptyList();
        if (node.getParameters().length > 0) {
            argumentTypeSignatures = new ArrayList<>(node.getParameters().length);
            for (org.codehaus.groovy.ast.Parameter parameter : node.getParameters()) {
                argumentTypeSignatures.add(parameter.getOriginType().getName());
            }
        }

        return typeCache.computeMethod(
                Paths.get("dontknow"),
                node.getDeclaringClass().getName(),
                node.getName(),
                node.getReturnType().getName(),
                argumentTypeSignatures,
                () -> {
                    JavaType.Method.Signature signature = new JavaType.Method.Signature(
                            type(node.getReturnType(), emptyMap()),
                            Arrays.stream(node.getParameters())
                                    .map(p -> {
                                        JavaType paramType = type(p.getOriginType(), emptyMap());
                                        if (paramType instanceof JavaType.Parameterized) {
                                            return ((JavaType.Parameterized) paramType).getType();
                                        }
                                        return paramType;
                                    })
                                    .collect(Collectors.toList())
                    );

                    return new JavaType.Method(
                            node.getModifiers(),
                            (JavaType.FullyQualified) type(node.getDeclaringClass(), emptyMap()),
                            node.getName(),
                            signature,
                            signature,
                            Arrays.stream(node.getParameters())
                                    .map(org.codehaus.groovy.ast.Parameter::getName)
                                    .collect(Collectors.toList()),
                            Arrays.stream(node.getExceptions())
                                    .map(e -> (JavaType.FullyQualified) type(e, emptyMap()))
                                    .collect(Collectors.toList()),
                            node.getAnnotations().stream()
                                    .map(a -> (JavaType.FullyQualified) type(a.getClassNode(), emptyMap()))
                                    .collect(Collectors.toList())
                    );
                }
        );
    }

    @Nullable
    public JavaType type(@Nullable ClassNode node) {
        return type(node, emptyMap());
    }

    @Nullable
    public JavaType type(@Nullable ClassNode node, Map<String, JavaType.Class> stack) {
        if (node == null) {
            return null;
        }

        JavaType.Primitive primitiveType = JavaType.Primitive.fromKeyword(node.getUnresolvedName());
        if (primitiveType != null) {
            return primitiveType;
        }

        try {
            return node.getGenericsTypes() != null && node.getGenericsTypes().length > 0 ?
                    parameterizedType(node.getTypeClass(), node.getGenericsTypes(), stack) :
                    type(node.getTypeClass(), stack);
        } catch (GroovyBugError | NoClassDefFoundError ignored) {
            return new JavaType.Class(Flag.Public.getBitMask(), node.getName(), JavaType.Class.Kind.Class,
                    null, null, null, null, null, null);
        }
    }

    private JavaType parameterizedType(Class<?> clazz, GenericsType[] generics, Map<String, JavaType.Class> stack) {
        List<String> genericSignatures = new ArrayList<>();
        for (GenericsType generic : generics) {
            genericSignatures.add(generic.getType().getName());
        }

        AtomicBoolean newlyCreated = new AtomicBoolean(false);

        JavaType.Parameterized parameterized = typeCache.computeParameterized(
                Paths.get("dontknow"),
                clazz.getName(),
                genericSignatures,
                () -> new JavaType.Parameterized(type(clazz, stack), emptyList())
        );

        if (newlyCreated.get()) {
            parameterized.unsafeSet(
                    Arrays.stream(generics)
                            .map(g -> {
                                if (g.getUpperBounds() != null) {
                                    return new JavaType.GenericTypeVariable(
                                            g.getName(),
                                            TypeUtils.asFullyQualified(type(g.getUpperBounds()[0], stack)));
                                }
                                return type(g.getType(), stack);
                            })
                            .collect(Collectors.toList())
            );
        }

        return parameterized;
    }

    @SuppressWarnings("unchecked")
    private <T extends JavaType> T type(@Nullable Class<?> clazz, Map<String, JavaType.Class> stack) {
        if (clazz == null) {
            //noinspection ConstantConditions
            return null;
        }

        if ("java.lang.Class".equals(clazz.getName())) {
            return (T) JavaType.Class.CLASS;
        } else if ("java.lang.Enum".equals(clazz.getName())) {
            return (T) JavaType.Class.ENUM;
        }

        JavaType.Primitive primitiveType = JavaType.Primitive.fromKeyword(clazz.getName());
        if (primitiveType != null) {
            return (T) primitiveType;
        }

        AtomicBoolean newlyCreated = new AtomicBoolean(false);

        JavaType.Class mappedClazz = typeCache.computeClass(Paths.get("dontknow"), clazz.getName(), () -> {
            JavaType.Class.Kind kind;
            if ((clazz.getModifiers() & KIND_BITMASK_ENUM) != 0) {
                kind = JavaType.Class.Kind.Enum;
            } else if ((clazz.getModifiers() & KIND_BITMASK_ANNOTATION) != 0) {
                kind = JavaType.Class.Kind.Annotation;
            } else if ((clazz.getModifiers() & KIND_BITMASK_INTERFACE) != 0) {
                kind = JavaType.Class.Kind.Interface;
            } else {
                kind = JavaType.Class.Kind.Class;
            }

            return new JavaType.Class(
                    clazz.getModifiers(),
                    clazz.getName(),
                    kind,
                    null, null, null, null,
                    type(clazz.getSuperclass(), stack),
                    type(clazz.getDeclaringClass(), stack)
            );
        });

        if (newlyCreated.get()) {
            Map<String, JavaType.Class> stackWithSym = new HashMap<>(stack);
            stackWithSym.put(clazz.getName(), mappedClazz);

            mappedClazz.unsafeSet(
                    Arrays.stream(clazz.getDeclaredAnnotations())
                            .map(a -> (JavaType.FullyQualified) type(a.annotationType(), stackWithSym))
                            .collect(Collectors.toList()),
                    Arrays.stream(clazz.getInterfaces())
                            .map(i -> (JavaType.FullyQualified) type(i, stackWithSym))
                            .collect(Collectors.toList()),
                    Arrays.stream(clazz.getDeclaredFields())
                            .filter(f -> !clazz.getName().equals("java.lang.String") || !f.getName().equals("serialPersistentFields"))
                            .map(f -> field(f, stackWithSym))
                            .collect(Collectors.toList()),
                    Arrays.stream(clazz.getDeclaredMethods())
                            .map(m -> method(m, stackWithSym))
                            .collect(Collectors.toList())
            );
        }

        return (T) mappedClazz;
    }

    private JavaType.Variable field(Field field, Map<String, JavaType.Class> stack) {
        return typeCache.computeVariable(
                Paths.get("dontknow"),
                field.getDeclaringClass().getName(),
                field.getName(),
                () -> new JavaType.Variable(
                        field.getModifiers(),
                        type(field.getDeclaringClass(), stack),
                        field.getName(),
                        type(field.getType(), stack),
                        Arrays.stream(field.getDeclaredAnnotations())
                                .map(a -> (JavaType.FullyQualified) type(a.annotationType(), stack))
                                .collect(Collectors.toList())
                )
        );
    }

    private JavaType.Method method(java.lang.reflect.Method method, Map<String, JavaType.Class> stack) {
        List<String> argumentTypeSignatures = emptyList();
        if (method.getParameters().length > 0) {
            argumentTypeSignatures = new ArrayList<>(method.getParameters().length);
            for (Parameter parameter : method.getParameters()) {
                argumentTypeSignatures.add(method.getDeclaringClass().getName());
            }
        }

        return typeCache.computeMethod(
                Paths.get("dontknow"),
                method.getDeclaringClass().getName(),
                method.getName(),
                method.getReturnType().getName(),
                argumentTypeSignatures,
                () -> new JavaType.Method(
                        method.getModifiers(),
                        type(method.getDeclaringClass(), stack),
                        method.getName(),
                        null,
                        null,
                        Arrays.stream(method.getParameters())
                                .map(Parameter::getName)
                                .collect(Collectors.toList()),
                        Arrays.stream(method.getExceptionTypes())
                                .map(e -> (JavaType.FullyQualified) type(e, stack))
                                .collect(Collectors.toList()),
                        Arrays.stream(method.getDeclaredAnnotations())
                                .map(a -> (JavaType.FullyQualified) type(a.annotationType(), stack))
                                .collect(Collectors.toList())
                )
        );
    }
}
