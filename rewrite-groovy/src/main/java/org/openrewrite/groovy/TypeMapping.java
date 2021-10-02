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
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;

@RequiredArgsConstructor
class TypeMapping {
    private static final int KIND_BITMASK_INTERFACE = 1 << 9;
    private static final int KIND_BITMASK_ANNOTATION = 1 << 13;
    private static final int KIND_BITMASK_ENUM = 1 << 14;

    private final Map<String, JavaType.Class> sharedClassTypes;

    @Nullable
    public JavaType.Method type(@Nullable MethodNode node) {
        if (node == null) {
            return null;
        }

        JavaType.Method.Signature signature = new JavaType.Method.Signature(
                type(node.getReturnType(), emptyList()),
                Arrays.stream(node.getParameters())
                        .map(p -> {
                            JavaType.FullyQualified fqn = (JavaType.FullyQualified) type(p.getOriginType(), emptyList());
                            if(fqn instanceof JavaType.Parameterized) {
                                return ((JavaType.Parameterized) fqn).getType();
                            }
                            return fqn;
                        })
                        .collect(Collectors.toList())
        );

        //noinspection ConstantConditions
        return JavaType.Method.build(
                node.getModifiers(),
                (JavaType.FullyQualified) type(node.getDeclaringClass(), emptyList()),
                node.getName(),
                signature,
                signature,
                Arrays.stream(node.getParameters())
                        .map(org.codehaus.groovy.ast.Parameter::getName)
                        .collect(Collectors.toList()),
                Arrays.stream(node.getExceptions())
                        .map(e -> (JavaType.FullyQualified) type(e, emptyList()))
                        .collect(Collectors.toList()),
                node.getAnnotations().stream()
                        .map(a -> (JavaType.FullyQualified) type(a.getClassNode(), emptyList()))
                        .collect(Collectors.toList())
        );
    }

    @Nullable
    public JavaType type(@Nullable ClassNode node) {
        return type(node, emptyList());
    }

    @Nullable
    public JavaType type(@Nullable ClassNode node, List<Class<?>> stack) {
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
        } catch (GroovyBugError ignored) {
            return null;
        }
    }

    private JavaType parameterizedType(Class<?> clazz, GenericsType[] generics, List<Class<?>> stack) {
        if (stack.contains(clazz)) {
            return new JavaType.Cyclic(clazz.getName());
        }

        List<Class<?>> stackWithSym = new ArrayList<>(stack);
        stackWithSym.add(clazz);

        return JavaType.Parameterized.build(
                type(clazz, stackWithSym),
                Arrays.stream(generics)
                        .map(g -> {
                            if (g.getUpperBounds() != null) {
                                return new JavaType.GenericTypeVariable(
                                        g.getName(),
                                        TypeUtils.asFullyQualified(type(g.getUpperBounds()[0], stackWithSym)));
                            }
                            return type(g.getType(), stackWithSym);
                        })
                        .collect(Collectors.toList())
        );
    }

    @SuppressWarnings("unchecked")
    private <T extends JavaType> T type(@Nullable Class<?> clazz, List<Class<?>> stack) {
        if (clazz == null) {
            //noinspection ConstantConditions
            return null;
        }

        if (clazz.getName().equals("java.lang.Object")) {
            return (T) JavaType.Class.OBJECT;
        } else if (clazz.getName().equals("java.lang.Class")) {
            return (T) JavaType.Class.build("java.lang.Class");
        }

        JavaType.Primitive primitiveType = JavaType.Primitive.fromKeyword(clazz.getName());
        if (primitiveType != null) {
            return (T) primitiveType;
        }

        if (stack.contains(clazz)) {
            return (T) new JavaType.Cyclic(clazz.getName());
        }

        JavaType.Class sharedClass = sharedClassTypes.get(clazz.getName());
        if (sharedClass != null) {
            return (T) sharedClass;
        }

        JavaType.Class cached = JavaType.Class.find(clazz.getName());
        if(cached != null) {
            return (T) cached;
        }


        List<Class<?>> stackWithSym = new ArrayList<>(stack);
        stackWithSym.add(clazz);

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

        return (T) JavaType.Class.build(
                clazz.getModifiers(),
                clazz.getName(),
                kind,
                Arrays.stream(clazz.getDeclaredFields())
                        .map(f -> field(f, stackWithSym))
                        .collect(Collectors.toList()),
                Arrays.stream(clazz.getInterfaces())
                        .map(i -> (JavaType.FullyQualified) type(i, stackWithSym))
                        .collect(Collectors.toList()),
                Arrays.stream(clazz.getDeclaredMethods())
                        .map(m -> method(m, stackWithSym))
                        .collect(Collectors.toList()),
                type(clazz.getSuperclass(), stackWithSym),
                type(clazz.getDeclaringClass(), stackWithSym),
                Arrays.stream(clazz.getDeclaredAnnotations())
                        .map(a -> (JavaType.FullyQualified) type(a.annotationType(), stackWithSym))
                        .collect(Collectors.toList()),
                null,
                false
        );
    }

    private JavaType.Variable field(Field field, List<Class<?>> stack) {
        return JavaType.Variable.build(
                field.getName(),
                type(field.getDeclaringClass(), stack),
                type(field.getType(), stack),
                Arrays.stream(field.getDeclaredAnnotations())
                        .map(a -> (JavaType.FullyQualified) type(a.annotationType(), stack))
                        .collect(Collectors.toList()),
                field.getModifiers()
        );
    }

    private JavaType.Method method(Method method, List<Class<?>> stack) {
        return JavaType.Method.build(
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
        );
    }
}
