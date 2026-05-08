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
package org.openrewrite.java.internal;

import org.jspecify.annotations.Nullable;
import org.openrewrite.java.JavaTypeMapping;
import org.openrewrite.java.tree.JavaType;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.openrewrite.java.tree.JavaType.GenericTypeVariable.Variance.*;

/**
 * Type mapping from type attribution given from {@link java.lang.reflect} types.
 */
public class JavaReflectionTypeMapping implements JavaTypeMapping<Type> {
    private static final int KIND_BITMASK_INTERFACE = 1 << 9;
    private static final int KIND_BITMASK_ANNOTATION = 1 << 13;
    private static final int KIND_BITMASK_ENUM = 1 << 14;

    private final JavaReflectionTypeSignatureBuilder signatureBuilder = new JavaReflectionTypeSignatureBuilder();

    private final JavaTypeFactory typeFactory;

    /**
     * Backward-compatible constructor: wraps the provided cache in a {@link DefaultJavaTypeFactory}.
     */
    public JavaReflectionTypeMapping(JavaTypeCache typeCache) {
        this(new DefaultJavaTypeFactory(typeCache));
    }

    public JavaReflectionTypeMapping(JavaTypeFactory typeFactory) {
        this.typeFactory = typeFactory;
    }

    @Override
    public JavaType type(@Nullable Type type) {
        if (type == null) {
            return JavaType.Unknown.getInstance();
        }

        String signature = signatureBuilder.signature(type);
        JavaType existing = typeFactory.get(signature);
        if (existing != null) {
            return existing;
        }

        if (type instanceof Class) {
            Class<?> clazz = (Class<?>) type;
            if (clazz.isArray()) {
                return array(clazz, signature);
            } else if (clazz.isPrimitive()) {
                //noinspection ConstantConditions
                return JavaType.Primitive.fromKeyword(clazz.getName());
            }
            return classType((Class<?>) type, signature);
        } else if (type instanceof GenericArrayType) {
            return array((GenericArrayType) type, signature);
        } else if (type instanceof TypeVariable) {
            return generic((TypeVariable<?>) type, signature);
        } else if (type instanceof WildcardType) {
            return generic((WildcardType) type, signature);
        } else if (type instanceof ParameterizedType) {
            return parameterized((ParameterizedType) type, signature);
        }

        throw new UnsupportedOperationException("Unknown type " + type.getClass().getName());
    }

    private JavaType.Array array(Class<?> clazz, String signature) {
        return typeFactory.computeArray(signature, clazz, arr ->
                arr.unsafeSet(type(clazz.getComponentType()), null));
    }

    private JavaType.Array array(GenericArrayType type, String signature) {
        return typeFactory.computeArray(signature, type, arr ->
                arr.unsafeSet(type(type.getGenericComponentType()), null));
    }

    private JavaType classType(Class<?> clazz, String signature) {
        JavaType.FullyQualified mappedClazz = classTypeWithoutParameters(clazz);

        if (clazz.getTypeParameters().length > 0) {
            return typeFactory.computeParameterized(signature, clazz, pt -> {
                List<JavaType> typeParameters = new ArrayList<>(clazz.getTypeParameters().length);
                for (TypeVariable<?> typeParameter : clazz.getTypeParameters()) {
                    typeParameters.add(type(typeParameter));
                }
                pt.unsafeSet(mappedClazz, typeParameters);
            });
        }

        return mappedClazz;
    }

    private JavaType.FullyQualified classTypeWithoutParameters(Class<?> clazz) {
        String className = clazz.getName();
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

        return typeFactory.computeClass(className, className, clazz.getModifiers(), kind, clazz, mappedClazz -> {
            JavaType.FullyQualified supertype = (JavaType.FullyQualified) (
                    "java.lang.Object".equals(clazz.getName()) ?
                            null :
                            (clazz.getSuperclass() == null ? type(Object.class) : type(clazz.getSuperclass())));
            JavaType.FullyQualified owner = (
                    "java.lang.Object".equals(clazz.getName()) ?
                            null :
                            (clazz.getDeclaringClass() == null ? null : (JavaType.FullyQualified) type(clazz.getDeclaringClass())));
            List<JavaType.FullyQualified> annotations = null;
            if (clazz.getDeclaredAnnotations().length > 0) {
                annotations = new ArrayList<>(clazz.getDeclaredAnnotations().length);
                for (Annotation a : clazz.getDeclaredAnnotations()) {
                    JavaType.FullyQualified type = (JavaType.FullyQualified) type(a.annotationType());
                    annotations.add(new JavaType.Annotation(type, emptyList()));
                }
            }

            List<JavaType.FullyQualified> interfaces = null;
            if (clazz.getInterfaces().length > 0) {
                interfaces = new ArrayList<>(clazz.getInterfaces().length);
                for (Class<?> i : clazz.getInterfaces()) {
                    JavaType.FullyQualified type = (JavaType.FullyQualified) type(i);
                    interfaces.add(type);
                }
            }

            List<JavaType.Variable> members = null;
            if (clazz.getDeclaredFields().length > 0) {
                members = new ArrayList<>(clazz.getDeclaredFields().length);
                for (Field f : clazz.getDeclaredFields()) {
                    if (!f.isSynthetic()) {
                        if (!"java.lang.String".equals(clazz.getName()) || !"serialPersistentFields".equals(f.getName())) {
                            JavaType.Variable field = field(f);
                            members.add(field);
                        }
                    }
                }
            }

            List<JavaType.Method> methods = null;
            if (clazz.getDeclaredMethods().length > 0) {
                methods = new ArrayList<>(clazz.getDeclaredMethods().length + clazz.getDeclaredConstructors().length);
                for (Method method : clazz.getDeclaredMethods()) {
                    if (!(method.isBridge() || method.isSynthetic())) {
                        methods.add(method(method, mappedClazz));
                    }
                }
            }

            if (clazz.getDeclaredConstructors().length > 0) {
                if (methods == null) {
                    methods = new ArrayList<>(clazz.getDeclaredConstructors().length);
                }
                for (Constructor<?> ctor : clazz.getDeclaredConstructors()) {
                    if (!ctor.isSynthetic()) {
                        methods.add(method(ctor, mappedClazz));
                    }
                }

            }

            List<JavaType> typeParameters = null;
            if (clazz.getTypeParameters().length > 0) {
                typeParameters = new ArrayList<>(clazz.getTypeParameters().length);
                for (Type tParam : clazz.getTypeParameters()) {
                    typeParameters.add(type(tParam));
                }
            }
            mappedClazz.unsafeSet(typeParameters, supertype, owner, annotations, interfaces, members, methods);
        });
    }

    private JavaType generic(TypeVariable<?> typeParameter, String signature) {
        return typeFactory.computeGenericTypeVariable(signature, typeParameter.getName(), INVARIANT, typeParameter, gtv -> {
            List<JavaType> bounds = genericBounds(typeParameter.getBounds());
            gtv.unsafeSet(gtv.getName(), bounds == null ? INVARIANT : COVARIANT, bounds);
        });
    }

    private JavaType.GenericTypeVariable generic(WildcardType wildcard, String signature) {
        return typeFactory.computeGenericTypeVariable(signature, "?", INVARIANT, wildcard, gtv -> {
            JavaType.GenericTypeVariable.Variance variance = INVARIANT;
            List<JavaType> bounds = null;

            if (wildcard.getLowerBounds().length > 0) {
                bounds = genericBounds(wildcard.getLowerBounds());
                if (bounds != null) {
                    variance = CONTRAVARIANT;
                }
            } else if (wildcard.getUpperBounds().length > 0) {
                bounds = genericBounds(wildcard.getUpperBounds());
                if (bounds != null) {
                    variance = COVARIANT;
                }
            }

            gtv.unsafeSet(gtv.getName(), variance, bounds);
        });
    }

    private @Nullable List<JavaType> genericBounds(Type[] bounds) {
        List<JavaType> mappedBounds = null;

        for (Type bound : bounds) {
            JavaType mappedBound = type(bound);
            if (!(mappedBound instanceof JavaType.FullyQualified) || !"java.lang.Object".equals(((JavaType.FullyQualified) mappedBound).getFullyQualifiedName())) {
                if (mappedBounds == null) {
                    mappedBounds = new ArrayList<>(bounds.length);
                }
                mappedBounds.add(mappedBound);
            }
        }

        return mappedBounds;
    }

    private JavaType parameterized(ParameterizedType type, String signature) {
        return typeFactory.computeParameterized(signature, type, pt -> {
            List<JavaType> typeParameters = new ArrayList<>(type.getActualTypeArguments().length);
            for (Type actualTypeArgument : type.getActualTypeArguments()) {
                typeParameters.add(type(actualTypeArgument));
            }

            JavaType.FullyQualified baseType = classTypeWithoutParameters((Class<?>) type.getRawType());
            pt.unsafeSet(baseType, typeParameters);
        });
    }

    private JavaType.Variable field(Field field) {
        String signature = signatureBuilder.variableSignature(field);
        return typeFactory.computeVariable(signature, field.getModifiers(), field.getName(), field, mappedVariable -> {
            List<JavaType.FullyQualified> annotations = null;
            if (field.getDeclaredAnnotations().length > 0) {
                annotations = new ArrayList<>(field.getDeclaredAnnotations().length);
                for (Annotation a : field.getDeclaredAnnotations()) {
                    JavaType.FullyQualified type = (JavaType.FullyQualified) type(a.annotationType());
                    annotations.add(new JavaType.Annotation(type, emptyList()));
                }
            }

            mappedVariable.unsafeSet(type(field.getDeclaringClass()), type(field.getGenericType()), annotations);
        });
    }

    public JavaType.Method method(Method method) {
        JavaType.FullyQualified type = (JavaType.FullyQualified) type(method.getDeclaringClass());
        if (type instanceof JavaType.Parameterized) {
            type = ((JavaType.Parameterized) type).getType();
        }
        return method(method, type);
    }

    private JavaType.Method method(Constructor<?> method, JavaType.FullyQualified declaringType) {
        String signature = signatureBuilder.methodSignature(method, declaringType.getFullyQualifiedName());

        String[] paramNames = null;
        if (method.getParameters().length > 0) {
            paramNames = new String[method.getParameters().length];
            Parameter[] parameters = method.getParameters();
            for (int i = 0; i < parameters.length; i++) {
                Parameter p = parameters[i];
                if (!p.isSynthetic()) {
                    paramNames[i] = p.getName();
                }
            }
        }

        return typeFactory.computeMethod(signature, method.getModifiers(), "<constructor>", paramNames, null, null, method, mappedMethod -> {
            List<JavaType> thrownExceptions = null;
            if (method.getExceptionTypes().length > 0) {
                thrownExceptions = new ArrayList<>(method.getExceptionTypes().length);
                for (Class<?> e : method.getExceptionTypes()) {
                    JavaType.FullyQualified fullyQualified = (JavaType.FullyQualified) type(e);
                    thrownExceptions.add(fullyQualified);
                }
            }

            List<JavaType.FullyQualified> annotations = new ArrayList<>();
            if (method.getDeclaredAnnotations().length > 0) {
                annotations = new ArrayList<>(method.getDeclaredAnnotations().length);
                for (Annotation a : method.getDeclaredAnnotations()) {
                    JavaType.FullyQualified fullyQualified = (JavaType.FullyQualified) type(a.annotationType());
                    annotations.add(new JavaType.Annotation(fullyQualified, emptyList()));
                }
            }

            List<JavaType> parameterTypes = emptyList();
            if (method.getParameters().length > 0) {
                parameterTypes = new ArrayList<>(method.getParameters().length);
                for (Parameter parameter : method.getParameters()) {
                    if (!parameter.isSynthetic()) {
                        Type parameterizedType = parameter.getParameterizedType();
                        parameterTypes.add(type(parameterizedType == null ? parameter.getType() : parameterizedType));
                    }
                }
            }

            mappedMethod.unsafeSet(declaringType, declaringType, parameterTypes, thrownExceptions, annotations);
        });
    }

    private JavaType.Method method(Method method, JavaType.FullyQualified declaringType) {
        String signature = signatureBuilder.methodSignature(method, declaringType.getFullyQualifiedName());

        String[] paramNames = null;
        if (method.getParameters().length > 0) {
            paramNames = new String[method.getParameters().length];
            Parameter[] parameters = method.getParameters();
            for (int i = 0; i < parameters.length; i++) {
                Parameter p = parameters[i];
                paramNames[i] = p.getName();
            }
        }

        List<String> defaultValues = null;
        if (method.getDefaultValue() != null) {
            Class<?> valueClass = method.getDefaultValue().getClass();
            if (valueClass.isArray()) {
                defaultValues = new ArrayList<>();
                Class<?> elementType = valueClass.getComponentType();
                if (elementType == int.class) {
                    for (int v : ((int[]) method.getDefaultValue())) {
                        defaultValues.add(String.valueOf(v));
                    }
                } else if (elementType == long.class) {
                    for (long v : ((long[]) method.getDefaultValue())) {
                        defaultValues.add(String.valueOf(v));
                    }
                } else if (elementType == byte.class) {
                    for (byte v : ((byte[]) method.getDefaultValue())) {
                        defaultValues.add(String.valueOf(v));
                    }
                } else if (elementType == boolean.class) {
                    for (boolean v : ((boolean[]) method.getDefaultValue())) {
                        defaultValues.add(String.valueOf(v));
                    }
                } else if (elementType == short.class) {
                    for (short v : ((short[]) method.getDefaultValue())) {
                        defaultValues.add(String.valueOf(v));
                    }
                } else if (elementType == double.class) {
                    for (double v : ((double[]) method.getDefaultValue())) {
                        defaultValues.add(String.valueOf(v));
                    }
                } else if (elementType == float.class) {
                    for (float v : ((float[]) method.getDefaultValue())) {
                        defaultValues.add(String.valueOf(v));
                    }
                } else {
                    for (Object v : ((Object[]) method.getDefaultValue())) {
                        defaultValues.add(String.valueOf(v));
                    }
                }
            } else {
                defaultValues = singletonList(method.getDefaultValue().toString());
            }
        }

        List<String> declaredFormalTypeNames = null;
        for (TypeVariable<?> typeVariable : method.getTypeParameters()) {
            if (typeVariable.getGenericDeclaration() == method) {
                if (declaredFormalTypeNames == null) {
                    declaredFormalTypeNames = new ArrayList<>();
                }
                declaredFormalTypeNames.add(typeVariable.getName());
            }
        }

        return typeFactory.computeMethod(
                signature,
                method.getModifiers(),
                method.getName(),
                paramNames,
                defaultValues,
                declaredFormalTypeNames == null ? null : declaredFormalTypeNames.toArray(new String[0]),
                method,
                mappedMethod -> {
                    List<JavaType> thrownExceptions = null;
                    if (method.getExceptionTypes().length > 0) {
                        thrownExceptions = new ArrayList<>(method.getExceptionTypes().length);
                        for (Type e : method.getGenericExceptionTypes()) {
                            thrownExceptions.add(type(e));
                        }
                    }

                    List<JavaType.FullyQualified> annotations = new ArrayList<>();
                    if (method.getDeclaredAnnotations().length > 0) {
                        annotations = new ArrayList<>(method.getDeclaredAnnotations().length);
                        for (Annotation a : method.getDeclaredAnnotations()) {
                            JavaType.FullyQualified fullyQualified = (JavaType.FullyQualified) type(a.annotationType());
                            annotations.add(new JavaType.Annotation(fullyQualified, emptyList()));
                        }
                    }

                    List<JavaType> parameterTypes = emptyList();
                    if (method.getParameters().length > 0) {
                        parameterTypes = new ArrayList<>(method.getParameters().length);
                        for (Type parameter : method.getGenericParameterTypes()) {
                            parameterTypes.add(type(parameter));
                        }
                    }

                    JavaType returnType = type(method.getGenericReturnType());
                    mappedMethod.unsafeSet(declaringType, returnType, parameterTypes, thrownExceptions, annotations);
                });
    }
}
