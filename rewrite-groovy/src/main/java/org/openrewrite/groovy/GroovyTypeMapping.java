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
import org.codehaus.groovy.ast.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.Flag;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Collections.emptyList;

@RequiredArgsConstructor
class GroovyTypeMapping {
    private static final int KIND_BITMASK_INTERFACE = 1 << 9;
    private static final int KIND_BITMASK_ANNOTATION = 1 << 13;
    private static final int KIND_BITMASK_ENUM = 1 << 14;

    private final GroovyReflectionTypeSignatureBuilder signatureBuilder = new GroovyReflectionTypeSignatureBuilder();
    private final GroovyAstTypeSignatureBuilder astSignatureBuilder = new GroovyAstTypeSignatureBuilder();
    private final Map<Class<?>, JavaType.Class> classStack = new IdentityHashMap<>();

    private final Map<String, Object> typeBySignature;

    @Nullable
    public JavaType.Method type(@Nullable MethodNode node) {
        if (node == null) {
            return null;
        }

        StringJoiner argumentTypeSignatures = new StringJoiner(",");
        if (node.getParameters().length > 0) {
            for (org.codehaus.groovy.ast.Parameter parameter : node.getParameters()) {
                argumentTypeSignatures.add(parameter.getOriginType().getName());
            }
        }

        //noinspection ConstantConditions
        return (JavaType.Method) typeBySignature.computeIfAbsent(astSignatureBuilder.methodSignature(node), ignore -> {
            List<JavaType> parameterTypes = emptyList();
            if (node.getParameters().length > 0) {
                parameterTypes = new ArrayList<>(node.getParameters().length);
                for (org.codehaus.groovy.ast.Parameter p : node.getParameters()) {
                    JavaType paramType = _type(p.getOriginType());
                    if (paramType instanceof JavaType.Parameterized) {
                        return ((JavaType.Parameterized) paramType).getType();
                    }
                    parameterTypes.add(paramType);
                }
            }

            JavaType.Method.Signature signature = new JavaType.Method.Signature(
                    _type(node.getReturnType()),
                    parameterTypes
            );

            List<String> paramNames = null;
            if (node.getParameters().length > 0) {
                paramNames = new ArrayList<>(node.getParameters().length);
                for (org.codehaus.groovy.ast.Parameter parameter : node.getParameters()) {
                    paramNames.add(parameter.getName());
                }
            }

            List<JavaType.FullyQualified> thrownExceptions = null;
            for (ClassNode e : node.getExceptions()) {
                thrownExceptions = new ArrayList<>(node.getExceptions().length);
                JavaType.FullyQualified qualified = (JavaType.FullyQualified) _type(e);
                thrownExceptions.add(qualified);
            }

            List<JavaType.FullyQualified> annotations = null;
            for (AnnotationNode a : node.getAnnotations()) {
                annotations = new ArrayList<>(node.getAnnotations().size());
                JavaType.FullyQualified fullyQualified = (JavaType.FullyQualified) _type(a.getClassNode());
                annotations.add(fullyQualified);
            }

            //noinspection ConstantConditions
            return new JavaType.Method(
                    node.getModifiers(),
                    (JavaType.FullyQualified) _type(node.getDeclaringClass()),
                    node.getName(),
                    paramNames,
                    signature,
                    signature,
                    thrownExceptions,
                    annotations
            );
        });
    }

    @Nullable
    public JavaType type(@Nullable ClassNode node) {
        classStack.clear();
        return _type(node);
    }

    @Nullable
    private JavaType _type(@Nullable ClassNode node) {
        if (node == null) {
            return null;
        }

        if(node.isArray()) {
            return arrayType(node);
        } else if(ClassHelper.isPrimitiveType(node)) {
            return JavaType.Primitive.fromKeyword(node.getName());
        }

        JavaType.Class clazz = (JavaType.Class) typeBySignature.computeIfAbsent(astSignatureBuilder.signature(node.getTypeClass()), ignored -> {
            try {
                return classType(node.getTypeClass());
            } catch (GroovyBugError | NoClassDefFoundError ignored1) {
                return new JavaType.Class(null, Flag.Public.getBitMask(), node.getName(), JavaType.Class.Kind.Class,
                        null, null, null, null, null, null);
            }
        });

        if(node.isUsingGenerics()) {
            AtomicBoolean newlyCreated = new AtomicBoolean(false);

            JavaType.Parameterized parameterized = (JavaType.Parameterized) typeBySignature.computeIfAbsent(signatureBuilder.signature(node), ignored -> {
                newlyCreated.set(true);
                //noinspection ConstantConditions
                return new JavaType.Parameterized(null, null, null);
            });

            if (newlyCreated.get()) {
                List<JavaType> typeParameters = emptyList();
                if (node.getGenericsTypes().length > 0) {
                    typeParameters = new ArrayList<>(node.getGenericsTypes().length);
                    for (GenericsType g : node.getGenericsTypes()) {
                        typeParameters.add(genericType(g));
                    }
                }

                parameterized.unsafeSet((JavaType.FullyQualified) clazz, typeParameters);
            }

            return parameterized;
        }

        return clazz;
    }

    @SuppressWarnings("ConstantConditions")
    private JavaType.Array arrayType(ClassNode array) {
        return (JavaType.Array) typeBySignature.computeIfAbsent(astSignatureBuilder.signature(array), ignored ->
                new JavaType.Array(_type(array.getComponentType())));
    }

    private JavaType genericType(GenericsType g) {
        //noinspection ConstantConditions
        return (JavaType) typeBySignature.computeIfAbsent(astSignatureBuilder.signature(g), ignored -> {
            if (g.getUpperBounds() != null) {
                List<JavaType> bounds = new ArrayList<>(g.getUpperBounds().length);
                for (ClassNode bound : g.getUpperBounds()) {
                    bounds.add(TypeUtils.asFullyQualified(_type(bound)));
                }

                // FIXME how to tell variance type?
                return new JavaType.GenericTypeVariable(
                        null,
                        g.getName(),
                        JavaType.GenericTypeVariable.Variance.COVARIANT,
                        bounds
                );
            }

            return _type(g.getType());
        });
    }

    private <T extends JavaType> T classType(@Nullable Class<?> clazz) {
        if (clazz == null) {
            //noinspection ConstantConditions
            return null;
        }

        //noinspection unchecked
        return (T) typeBySignature.computeIfAbsent(signatureBuilder.signature(clazz), ignored -> {
            if (clazz.isArray()) {
                return new JavaType.Array(classType(clazz.getComponentType()));
            } else if (clazz.isPrimitive()) {
                return JavaType.Primitive.fromKeyword(clazz.getName());
            }
            return _classType(clazz);
        });
    }

    private JavaType _classType(Class<?> clazz) {
        JavaType.Class existingClass = classStack.get(clazz);
        if (existingClass != null) {
            return existingClass;
        }

        AtomicBoolean newlyCreated = new AtomicBoolean(false);

        JavaType.Class mappedClazz = (JavaType.Class) typeBySignature.computeIfAbsent(signatureBuilder.signature(clazz), ignored -> {
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
                    null,
                    clazz.getModifiers(),
                    clazz.getName(),
                    kind,
                    null, null, null, null, null, null
            );
        });

        if (newlyCreated.get()) {
            classStack.put(clazz, mappedClazz);

            JavaType.FullyQualified supertype = classType(clazz.getSuperclass());
            JavaType.FullyQualified owner = classType(clazz.getDeclaringClass());

            List<JavaType.FullyQualified> annotations = null;
            if (clazz.getDeclaredAnnotations().length > 0) {
                annotations = new ArrayList<>(clazz.getDeclaredAnnotations().length);
                for (Annotation a : clazz.getDeclaredAnnotations()) {
                    JavaType.FullyQualified type = classType(a.annotationType());
                    annotations.add(type);
                }
            }

            List<JavaType.FullyQualified> interfaces = null;
            if (clazz.getInterfaces().length > 0) {
                interfaces = new ArrayList<>(clazz.getInterfaces().length);
                for (Class<?> i : clazz.getInterfaces()) {
                    JavaType.FullyQualified type = classType(i);
                    interfaces.add(type);
                }
            }

            List<JavaType.Variable> members = null;
            if (clazz.getDeclaredFields().length > 0) {
                members = new ArrayList<>(clazz.getDeclaredFields().length);
                for (Field f : clazz.getDeclaredFields()) {
                    if (!clazz.getName().equals("java.lang.String") || !f.getName().equals("serialPersistentFields")) {
                        JavaType.Variable field = field(f);
                        members.add(field);
                    }
                }
            }

            List<JavaType.Method> methods = null;
            if (clazz.getDeclaredMethods().length > 0) {
                methods = new ArrayList<>(clazz.getDeclaredMethods().length);
                for (Method method : clazz.getDeclaredMethods()) {
                    JavaType.Method javaType = method(method);
                    methods.add(javaType);
                }
            }

            mappedClazz.unsafeSet(supertype, owner, annotations, interfaces, members, methods);

            classStack.remove(clazz);
        }

        return mappedClazz;
    }

    private JavaType.Variable field(Field field) {
        return (JavaType.Variable) typeBySignature.computeIfAbsent(signatureBuilder.variableSignature(field), ignored -> {
            List<JavaType.FullyQualified> annotations = null;
            if (field.getDeclaredAnnotations().length > 0) {
                annotations = new ArrayList<>(field.getDeclaredAnnotations().length);
                for (Annotation a : field.getDeclaredAnnotations()) {
                    JavaType.FullyQualified type = classType(a.annotationType());
                    annotations.add(type);
                }
            }

            return new JavaType.Variable(
                    field.getModifiers(),
                    field.getName(),
                    classType(field.getDeclaringClass()),
                    classType(field.getType()),
                    annotations
            );
        });
    }

    private JavaType.Method method(Method method) {
        return (JavaType.Method) typeBySignature.computeIfAbsent(signatureBuilder.methodSignature(method), ignored -> {
            List<String> paramNames = null;
            if (method.getParameters().length > 0) {
                paramNames = new ArrayList<>(method.getParameters().length);
                for (Parameter p : method.getParameters()) {
                    paramNames.add(p.getName());
                }
            }

            List<JavaType.FullyQualified> thrownExceptions = null;
            if (method.getExceptionTypes().length > 0) {
                thrownExceptions = new ArrayList<>(method.getExceptionTypes().length);
                for (Class<?> e : method.getExceptionTypes()) {
                    JavaType.FullyQualified fullyQualified = classType(e);
                    thrownExceptions.add(fullyQualified);
                }
            }

            List<JavaType.FullyQualified> annotations = new ArrayList<>();
            if (method.getDeclaredAnnotations().length > 0) {
                annotations = new ArrayList<>(method.getDeclaredAnnotations().length);
                for (Annotation a : method.getDeclaredAnnotations()) {
                    JavaType.FullyQualified fullyQualified = classType(a.annotationType());
                    annotations.add(fullyQualified);
                }
            }

            List<JavaType> resolvedArgumentTypes = emptyList();
            if (method.getParameters().length > 0) {
                resolvedArgumentTypes = new ArrayList<>(method.getParameters().length);
                for (Parameter parameter : method.getParameters()) {
                    resolvedArgumentTypes.add(classType(method.getDeclaringClass()));
                }
            }

            // FIXME method.getReturnType() can't result in a parameterized type?
            JavaType.Method.Signature signature = new JavaType.Method.Signature(
                    classType(method.getReturnType()),
                    resolvedArgumentTypes
            );

            return new JavaType.Method(
                    method.getModifiers(),
                    classType(method.getDeclaringClass()),
                    method.getName(),
                    paramNames,
                    signature,
                    signature,
                    thrownExceptions,
                    annotations
            );
        });
    }
}
