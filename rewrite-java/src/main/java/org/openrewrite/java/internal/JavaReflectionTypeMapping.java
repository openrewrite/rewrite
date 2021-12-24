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

import lombok.RequiredArgsConstructor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaTypeMapping;
import org.openrewrite.java.tree.JavaType;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static org.openrewrite.java.tree.JavaType.GenericTypeVariable.Variance.*;

/**
 * Type mapping from type attribution given from {@link java.lang.reflect} types.
 */
@RequiredArgsConstructor
public class JavaReflectionTypeMapping implements JavaTypeMapping<Type> {
    private static final int KIND_BITMASK_INTERFACE = 1 << 9;
    private static final int KIND_BITMASK_ANNOTATION = 1 << 13;
    private static final int KIND_BITMASK_ENUM = 1 << 14;

    private final JavaReflectionTypeSignatureBuilder signatureBuilder = new JavaReflectionTypeSignatureBuilder();

    private final Map<String, Object> typeBySignature;

    @Override
    public JavaType type(@Nullable Type type) {
        if (type == null) {
            return JavaType.Unknown.getInstance();
        }

        String signature = signatureBuilder.signature(type);
        JavaType existing = (JavaType) typeBySignature.get(signature);
        if (existing != null) {
            return existing;
        }

        JavaType mapped;
        if (type instanceof Class) {
            Class<?> clazz = (Class<?>) type;
            if (clazz.isArray()) {
                return array(clazz, signature);
            } else if (clazz.isPrimitive()) {
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
        JavaType.Array arr = new JavaType.Array(null);
        typeBySignature.put(signature, arr);
        arr.unsafeSet(type(clazz.getComponentType()));
        return arr;
    }

    private JavaType.Array array(GenericArrayType type, String signature) {
        JavaType.Array arr = new JavaType.Array(null);
        typeBySignature.put(signature, arr);
        arr.unsafeSet(type(type.getGenericComponentType()));
        return arr;
    }

    private JavaType classType(Class<?> clazz, String signature) {
        JavaType.FullyQualified mappedClazz = classTypeWithoutParameters(clazz);

        if (clazz.getTypeParameters().length > 0) {
            JavaType existing = (JavaType) typeBySignature.get(signature);
            if(existing != null) {
                return existing;
            }

            JavaType.Parameterized pt = new JavaType.Parameterized(null, null, null);
            typeBySignature.put(signature, pt);

            List<JavaType> typeParameters = new ArrayList<>(clazz.getTypeParameters().length);
            for (TypeVariable<?> typeParameter : clazz.getTypeParameters()) {
                typeParameters.add(type(typeParameter));
            }

            pt.unsafeSet(mappedClazz, typeParameters);
            return pt;
        }

        return mappedClazz;
    }

    private JavaType.FullyQualified classTypeWithoutParameters(Class<?> clazz) {
        String className = clazz.getName();
        JavaType.Class mappedClazz = (JavaType.Class) typeBySignature.get(className);

        if (mappedClazz == null) {
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

            mappedClazz = new JavaType.Class(
                    null,
                    clazz.getModifiers(),
                    className,
                    kind,
                    null, null, null, null, null, null
            );

            typeBySignature.put(className, mappedClazz);

            JavaType.FullyQualified supertype = (JavaType.FullyQualified) (
                    clazz.getName().equals("java.lang.Object") ?
                            null :
                            (clazz.getSuperclass() == null ? type(Object.class) : type(clazz.getSuperclass())));
            JavaType.FullyQualified owner = (JavaType.FullyQualified) type(clazz.getDeclaringClass());

            List<JavaType.FullyQualified> annotations = null;
            if (clazz.getDeclaredAnnotations().length > 0) {
                annotations = new ArrayList<>(clazz.getDeclaredAnnotations().length);
                for (Annotation a : clazz.getDeclaredAnnotations()) {
                    JavaType.FullyQualified type = (JavaType.FullyQualified) type(a.annotationType());
                    annotations.add(type);
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
                    if(!f.isSynthetic()) {
                        if (!clazz.getName().equals("java.lang.String") || !f.getName().equals("serialPersistentFields")) {
                            JavaType.Variable field = field(f);
                            members.add(field);
                        }
                    }
                }
            }

            List<JavaType.Method> methods = null;
            if (clazz.getDeclaredMethods().length > 0) {
                methods = new ArrayList<>(clazz.getDeclaredMethods().length);
                for (Method method : clazz.getDeclaredMethods()) {
                    if(!(method.isBridge() || method.isSynthetic())) {
                        methods.add(method(method));
                    }
                }
            }

            mappedClazz.unsafeSet(supertype, owner, annotations, interfaces, members, methods);
        }

        return mappedClazz;
    }

    private JavaType generic(TypeVariable<?> typeParameter, String signature) {
        JavaType.GenericTypeVariable gtv = new JavaType.GenericTypeVariable(null, typeParameter.getName(),
                INVARIANT, null);
        typeBySignature.put(signature, gtv);

        List<JavaType> bounds = genericBounds(typeParameter.getBounds());
        gtv.unsafeSet(bounds == null ? INVARIANT : COVARIANT, bounds);
        return gtv;
    }

    private JavaType.GenericTypeVariable generic(WildcardType wildcard, String signature) {
        JavaType.GenericTypeVariable gtv = new JavaType.GenericTypeVariable(null, "?",
                INVARIANT, null);
        typeBySignature.put(signature, gtv);

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

        gtv.unsafeSet(variance, bounds);
        return gtv;
    }

    @Nullable
    private List<JavaType> genericBounds(Type[] bounds) {
        List<JavaType> mappedBounds = null;

        for (Type bound : bounds) {
            JavaType mappedBound = type(bound);
            if (!(mappedBound instanceof JavaType.FullyQualified) || !((JavaType.FullyQualified) mappedBound).getFullyQualifiedName().equals("java.lang.Object")) {
                if (mappedBounds == null) {
                    mappedBounds = new ArrayList<>(bounds.length);
                }
                mappedBounds.add(mappedBound);
            }
        }

        return mappedBounds;
    }

    private JavaType parameterized(ParameterizedType type, String signature) {
        JavaType.Parameterized pt = new JavaType.Parameterized(null, null, null);
        typeBySignature.put(signature, pt);

        List<JavaType> typeParameters = new ArrayList<>(type.getActualTypeArguments().length);
        for (Type actualTypeArgument : type.getActualTypeArguments()) {
            typeParameters.add(type(actualTypeArgument));
        }

        JavaType.FullyQualified baseType = classTypeWithoutParameters((Class<?>) type.getRawType());
        pt.unsafeSet(baseType, typeParameters);
        return pt;
    }

    private JavaType.Variable field(Field field) {
        String signature = signatureBuilder.variableSignature(field);
        JavaType.Variable existing = (JavaType.Variable) typeBySignature.get(signature);
        if (existing != null) {
            return existing;
        }

        JavaType.Variable mappedVariable = new JavaType.Variable(
                field.getModifiers(),
                field.getName(),
                null, null, null
        );
        typeBySignature.put(signature, mappedVariable);

        List<JavaType.FullyQualified> annotations = null;
        if (field.getDeclaredAnnotations().length > 0) {
            annotations = new ArrayList<>(field.getDeclaredAnnotations().length);
            for (Annotation a : field.getDeclaredAnnotations()) {
                JavaType.FullyQualified type = (JavaType.FullyQualified) type(a.annotationType());
                annotations.add(type);
            }
        }

        mappedVariable.unsafeSet((JavaType.FullyQualified) type(field.getDeclaringClass()), type(field.getType()), annotations);
        return mappedVariable;
    }

    public JavaType.Method method(Method method) {
        String signature = signatureBuilder.methodSignature(method);

        JavaType.Method existing = (JavaType.Method) typeBySignature.get(signature);
        if (existing != null) {
            return existing;
        }

        List<String> paramNames = null;
        if (method.getParameters().length > 0) {
            paramNames = new ArrayList<>(method.getParameters().length);
            for (Parameter p : method.getParameters()) {
                paramNames.add(p.getName());
            }
        }

        JavaType.Method mappedMethod = new JavaType.Method(
                method.getModifiers(),
                null,
                method.getName(),
                paramNames,
                null, null, null, null
        );
        typeBySignature.put(signature, mappedMethod);

        List<JavaType.FullyQualified> thrownExceptions = null;
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
                annotations.add(fullyQualified);
            }
        }

        List<JavaType> resolvedArgumentTypes = emptyList();
        if (method.getParameters().length > 0) {
            resolvedArgumentTypes = new ArrayList<>(method.getParameters().length);
            for (Parameter parameter : method.getParameters()) {
                Type parameterizedType = parameter.getParameterizedType();
                resolvedArgumentTypes.add(type(parameterizedType == null ? parameter.getType() : parameterizedType));
            }
        }

        JavaType.Method.Signature resolvedSignature = new JavaType.Method.Signature(
                type(method.getReturnType()),
                resolvedArgumentTypes
        );

        mappedMethod.unsafeSet((JavaType.FullyQualified) type(method.getDeclaringClass()), resolvedSignature, resolvedSignature, thrownExceptions, annotations);
        return mappedMethod;
    }
}
