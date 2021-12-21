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
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

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
    private final Map<Class<?>, JavaType.Class> classStack = new IdentityHashMap<>();

    private final Map<String, Object> typeBySignature;

    @Override
    public JavaType type(@Nullable Type type) {
        if (type == null) {
            return null;
        }
        classStack.clear();

        return _type(type);
    }

    public JavaType _type(@Nullable Type type) {
        if (type == null) {
            //noinspection ConstantConditions
            return null;
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
                return new JavaType.Array(classType(clazz.getComponentType()));
            } else if (clazz.isPrimitive()) {
                return JavaType.Primitive.fromKeyword(clazz.getName());
            }
            mapped = classType(clazz);

            if (clazz.getTypeParameters().length > 0) {
                List<JavaType> typeParameters = new ArrayList<>(clazz.getTypeParameters().length);
                for (TypeVariable<?> typeParameter : clazz.getTypeParameters()) {
                    typeParameters.add(_type(typeParameter));
                }
                mapped = new JavaType.Parameterized(null, (JavaType.FullyQualified) mapped,
                        typeParameters);
            }
        } else if (type instanceof GenericArrayType) {
            throw new UnsupportedOperationException("Unknown type " + type.getClass().getName());
        } else if (type instanceof TypeVariable) {
            mapped = typeVariable((TypeVariable<?>) type);
        } else if (type instanceof WildcardType) {
            mapped = wildcard((WildcardType) type);
        } else if (type instanceof ParameterizedType) {
            mapped = parameterized((ParameterizedType) type);
        } else {
            throw new UnsupportedOperationException("Unknown type " + type.getClass().getName());
        }

        typeBySignature.put(signature, mapped);
        return mapped;
    }

    private JavaType classType(Class<?> clazz) {
        JavaType.Class existingClass = classStack.get(clazz);
        if (existingClass != null) {
            return existingClass;
        }

        AtomicBoolean newlyCreated = new AtomicBoolean(false);

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

        JavaType.Class mappedClazz = new JavaType.Class(
                null,
                clazz.getModifiers(),
                clazz.getName(),
                kind,
                null, null, null, null, null, null
        );

        classStack.put(clazz, mappedClazz);

        JavaType.FullyQualified supertype = (JavaType.FullyQualified) (
                clazz.getName().equals("java.lang.Object") ?
                        null :
                        (clazz.getSuperclass() == null ? _type(Object.class) : _type(clazz.getSuperclass())));
        JavaType.FullyQualified owner = (JavaType.FullyQualified) _type(clazz.getDeclaringClass());

        List<JavaType.FullyQualified> annotations = null;
        if (clazz.getDeclaredAnnotations().length > 0) {
            annotations = new ArrayList<>(clazz.getDeclaredAnnotations().length);
            for (Annotation a : clazz.getDeclaredAnnotations()) {
                JavaType.FullyQualified type = (JavaType.FullyQualified) _type(a.annotationType());
                annotations.add(type);
            }
        }

        List<JavaType.FullyQualified> interfaces = null;
        if (clazz.getInterfaces().length > 0) {
            interfaces = new ArrayList<>(clazz.getInterfaces().length);
            for (Class<?> i : clazz.getInterfaces()) {
                JavaType.FullyQualified type = (JavaType.FullyQualified) _type(i);
                interfaces.add(type);
            }
        }

        List<JavaType.Variable> members = null;
        if (clazz.getDeclaredFields().length > 0) {
            members = new ArrayList<>(clazz.getDeclaredFields().length);
            for (Field f : clazz.getDeclaredFields()) {
                if (!clazz.getName().equals("java.lang.String") || !f.getName().equals("serialPersistentFields")) {
                    JavaType.Variable field = _field(f);
                    members.add(field);
                }
            }
        }

        List<JavaType.Method> methods = null;
        if (clazz.getDeclaredMethods().length > 0) {
            methods = new ArrayList<>(clazz.getDeclaredMethods().length);
            for (Method method : clazz.getDeclaredMethods()) {
                JavaType.Method javaType = _method(method);
                methods.add(javaType);
            }
        }

        mappedClazz.unsafeSet(supertype, owner, annotations, interfaces, members, methods);

        classStack.remove(clazz);

        return mappedClazz;
    }

    private JavaType wildcard(WildcardType wildcard) {
        JavaType.GenericTypeVariable.Variance variance = INVARIANT;
        List<JavaType> bounds = null;

        if (wildcard.getLowerBounds().length > 0) {
            for (Type bound : wildcard.getLowerBounds()) {
                JavaType mappedBound = _type(bound);
                if (!((JavaType.FullyQualified) mappedBound).getFullyQualifiedName().equals("java.lang.Object")) {
                    if (bounds == null) {
                        bounds = new ArrayList<>(wildcard.getLowerBounds().length);
                    }
                    bounds.add(mappedBound);
                }
            }

            if (bounds != null) {
                variance = CONTRAVARIANT;
            }
        } else if (wildcard.getUpperBounds().length > 0) {
            for (Type bound : wildcard.getUpperBounds()) {
                JavaType mappedBound = _type(bound);
                if (!((JavaType.FullyQualified) mappedBound).getFullyQualifiedName().equals("java.lang.Object")) {
                    if (bounds == null) {
                        bounds = new ArrayList<>(wildcard.getLowerBounds().length);
                    }
                    bounds.add(mappedBound);
                }
            }

            if (bounds != null) {
                variance = COVARIANT;
            }
        }

        return new JavaType.GenericTypeVariable(null, "?", variance, bounds);
    }

    private JavaType parameterized(ParameterizedType type) {
        JavaType mapped;
        List<JavaType> typeParameters = new ArrayList<>(type.getActualTypeArguments().length);
        for (Type actualTypeArgument : type.getActualTypeArguments()) {
            typeParameters.add(_type(actualTypeArgument));
        }
        mapped = new JavaType.Parameterized(null, (JavaType.FullyQualified) _type(type.getRawType()), typeParameters);
        return mapped;
    }

    private JavaType typeVariable(TypeVariable<?> typeParameter) {
        List<JavaType> bounds = null;
        for (Type bound : typeParameter.getBounds()) {
            if (bound instanceof JavaType.Class && ((JavaType.Class) bound).getFullyQualifiedName().equals("java.lang.Object")) {
                continue;
            }
            if (bounds == null) {
                bounds = new ArrayList<>(typeParameter.getBounds().length);
            }
            bounds.add(_type(bound));
        }

        // FIXME how to determine contravariance?
        return new JavaType.GenericTypeVariable(null, typeParameter.getName(),
                bounds == null ? INVARIANT : COVARIANT, bounds);
    }
    private JavaType.Variable field(Field field) {
        classStack.clear();
        return _field(field);
    }

    private JavaType.Variable _field(Field field) {
        String signature = signatureBuilder.variableSignature(field);
        JavaType.Variable existing = (JavaType.Variable) typeBySignature.get(signature);
        if (existing != null) {
            return existing;
        }

        List<JavaType.FullyQualified> annotations = null;
        if (field.getDeclaredAnnotations().length > 0) {
            annotations = new ArrayList<>(field.getDeclaredAnnotations().length);
            for (Annotation a : field.getDeclaredAnnotations()) {
                JavaType.FullyQualified type = (JavaType.FullyQualified) _type(a.annotationType());
                annotations.add(type);
            }
        }

        JavaType.Variable mappedVariable = new JavaType.Variable(
                field.getModifiers(),
                field.getName(),
                (JavaType.FullyQualified) _type(field.getDeclaringClass()),
                _type(field.getType()),
                annotations
        );

        typeBySignature.put(signature, mappedVariable);

        return mappedVariable;
    }

    public JavaType.Method method(Method method) {
        classStack.clear();
        return _method(method);
    }

    private JavaType.Method _method(Method method) {
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

        List<JavaType.FullyQualified> thrownExceptions = null;
        if (method.getExceptionTypes().length > 0) {
            thrownExceptions = new ArrayList<>(method.getExceptionTypes().length);
            for (Class<?> e : method.getExceptionTypes()) {
                JavaType.FullyQualified fullyQualified = (JavaType.FullyQualified) _type(e);
                thrownExceptions.add(fullyQualified);
            }
        }

        List<JavaType.FullyQualified> annotations = new ArrayList<>();
        if (method.getDeclaredAnnotations().length > 0) {
            annotations = new ArrayList<>(method.getDeclaredAnnotations().length);
            for (Annotation a : method.getDeclaredAnnotations()) {
                JavaType.FullyQualified fullyQualified = (JavaType.FullyQualified) _type(a.annotationType());
                annotations.add(fullyQualified);
            }
        }

        List<JavaType> resolvedArgumentTypes = emptyList();
        if (method.getParameters().length > 0) {
            resolvedArgumentTypes = new ArrayList<>(method.getParameters().length);
            for (Parameter parameter : method.getParameters()) {
                resolvedArgumentTypes.add(_type(method.getDeclaringClass()));
            }
        }

        JavaType.Method.Signature resolvedSignature = new JavaType.Method.Signature(
                _type(method.getReturnType()),
                resolvedArgumentTypes
        );

        JavaType.Method mappedMethod = new JavaType.Method(
                method.getModifiers(),
                (JavaType.FullyQualified) _type(method.getDeclaringClass()),
                method.getName(),
                paramNames,
                resolvedSignature,
                resolvedSignature,
                thrownExceptions,
                annotations
        );

        typeBySignature.put(signature, mappedMethod);

        return mappedMethod;
    }
}
