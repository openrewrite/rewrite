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

import io.github.classgraph.*;
import lombok.RequiredArgsConstructor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaTypeSignatureBuilder;
import org.openrewrite.java.tree.JavaType;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

@RequiredArgsConstructor
public class ClassgraphJavaTypeSignatureBuilder implements JavaTypeSignatureBuilder {
    private Set<String> typeVariableNameStack;

    private final Map<String, JavaType.FullyQualified> jvmTypes;

    @Override
    public String signature(@Nullable Object type) {
        if (type == null) {
            return "{undefined}";
        }

        HierarchicalTypeSignature typeSignature = (HierarchicalTypeSignature) type;

        if (typeSignature instanceof ClassRefTypeSignature) {
            ClassRefTypeSignature classRef = (ClassRefTypeSignature) typeSignature;
            if (!classRef.getTypeArguments().isEmpty()) {
                return parameterizedSignature(typeSignature);
            }
            return classSignature(typeSignature);
        } else if (typeSignature instanceof ClassTypeSignature) {
            return parameterizedSignature(typeSignature);
        } else if (typeSignature instanceof ArrayTypeSignature) {
            return arraySignature(typeSignature);
        } else if (typeSignature instanceof TypeVariableSignature ||
                typeSignature instanceof TypeParameter ||
                typeSignature instanceof TypeArgument) {
            return genericSignature(typeSignature);
        } else if (typeSignature instanceof BaseTypeSignature) {
            return primitiveSignature(typeSignature);
        }

        throw new UnsupportedOperationException("Unexpected signature type " + type.getClass().getName());
    }

    @Override
    public String arraySignature(Object type) {
        ArrayTypeSignature arrSignature = (ArrayTypeSignature) type;
        return signature(arrSignature.getNestedType()) + "[]";
    }

    @Override
    public String classSignature(Object type) {
        ClassInfo classInfo;
        String className;

        if (type instanceof ClassTypeSignature) {
            try {
                Method getClassInfo = ClassTypeSignature.class.getDeclaredMethod("getClassInfo");
                getClassInfo.setAccessible(true);
                classInfo = (ClassInfo) getClassInfo.invoke(type);

                Method getClassName = ClassTypeSignature.class.getDeclaredMethod("getClassName");
                getClassName.setAccessible(true);
                className = (String) getClassName.invoke(type);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        } else if (type instanceof ClassRefTypeSignature) {
            ClassRefTypeSignature clazz = (ClassRefTypeSignature) type;
            classInfo = clazz.getClassInfo();
            className = clazz.getFullyQualifiedClassName();
        } else {
            throw new UnsupportedOperationException("Unknown class type " + type.getClass().getName());
        }

        if (classInfo == null) {
            JavaType.FullyQualified fallback = jvmTypes.get(className);
            if (fallback != null) {
                return fallback.getFullyQualifiedName();
            } else if (className.equals("java.lang.Object")) {
                return className;
            } else {
                return "{undefined}";
            }
        }

        return className;
    }

    @Override
    public String genericSignature(Object type) {
        if (type instanceof TypeVariableSignature) {
            TypeVariableSignature typeVariableSignature = (TypeVariableSignature) type;
            try {
                return signature(typeVariableSignature.resolve()); // resolves to a TypeParameter
            } catch (IllegalArgumentException ignored) {
                return "Generic{}";
            }
        } else if (type instanceof TypeArgument) {
            return generic((TypeArgument) type);
        } else if (type instanceof TypeParameter) {
            return generic((TypeParameter) type);
        }

        throw new UnsupportedOperationException("Unexpected generic type " + type.getClass().getName());
    }

    private String generic(TypeArgument typeArgument) {
        StringBuilder s = new StringBuilder();
        switch (typeArgument.getWildcard()) {
            case NONE:
                s.append(signature(typeArgument.getTypeSignature()));
                break;
            case EXTENDS: {
                String bound = signature(typeArgument.getTypeSignature());
                if (bound.equals("java.lang.Object")) {
                    s.append("Generic{}");
                } else {
                    s.append("Generic{extends ").append(bound).append('}');
                }
                break;
            }
            case SUPER: {
                String bound = signature(typeArgument.getTypeSignature());
                if (bound.equals("java.lang.Object")) {
                    s.append("Generic{}");
                } else {
                    s.append("Generic{super ").append(bound).append('}');
                }
                break;
            }
            case ANY:
                s.append("Generic{}");
        }

        return s.toString();
    }

    public String generic(TypeParameter typeParameter) {
        String name = typeParameter.getName();
        if (typeVariableNameStack == null) {
            typeVariableNameStack = new HashSet<>();
        }

        if (typeVariableNameStack.add(name)) {
            StringBuilder bounds = new StringBuilder();

            if (typeParameter.getClassBound() != null) {
                String bound = signature(typeParameter.getClassBound());
                if (!bound.equals("java.lang.Object")) {
                    bounds.append(bound);
                }
            } else if (typeParameter.getInterfaceBounds() != null) {
                StringJoiner interfaceBounds = new StringJoiner(" & ");
                for (ReferenceTypeSignature interfaceBound : typeParameter.getInterfaceBounds()) {
                    interfaceBounds.add(signature(interfaceBound));
                }
                bounds.append(interfaceBounds);
            }

            typeVariableNameStack.remove(name);

            String boundsStr = bounds.toString();
            if (boundsStr.isEmpty()) {
                return "Generic{}";
            }
            return "Generic{extends " + boundsStr + "}";
        }

        return "Generic{}";
    }

    @Override
    public String parameterizedSignature(Object type) {
        String baseClass = classSignature(type);
        if (baseClass.equals("{undefined}")) {
            return baseClass;
        }

        StringBuilder s = new StringBuilder(baseClass);

        StringJoiner typeParameters = new StringJoiner(", ", "<", ">");
        if (type instanceof ClassTypeSignature) {
            ClassTypeSignature clazz = (ClassTypeSignature) type;
            if (!clazz.getTypeParameters().isEmpty()) {
                for (TypeParameter typeParameter : clazz.getTypeParameters()) {
                    typeParameters.add(signature(typeParameter));
                }
            }
        } else if (type instanceof ClassRefTypeSignature) {
            ClassRefTypeSignature clazz = (ClassRefTypeSignature) type;
            if (!clazz.getTypeArguments().isEmpty()) {
                for (TypeArgument typeArgument : clazz.getTypeArguments()) {
                    typeParameters.add(signature(typeArgument));
                }
            }
        } else {
            throw new UnsupportedOperationException("Unexpected parameterized type " + type.getClass().getName());
        }

        s.append(typeParameters);

        return s.toString();
    }

    @Override
    public String primitiveSignature(Object type) {
        BaseTypeSignature baseTypeSignature = (BaseTypeSignature) type;
        return baseTypeSignature.getTypeStr();
    }

    public String methodSignature(MethodInfo methodInfo) {
        StringBuilder s = new StringBuilder(methodInfo.getClassName());
        s.append("{name=").append(methodInfo.isConstructor() ? "<constructor>" : methodInfo.getName());

        s.append(",return=");
        if (methodInfo.isConstructor()) {
            s.append(methodInfo.getClassName());
        } else {
            s.append(methodInfo.getTypeSignature() == null ?
                    signature(methodInfo.getTypeDescriptor().getResultType()) :
                    signature(methodInfo.getTypeSignature().getResultType()));
        }

        StringJoiner parameterTypes = new StringJoiner(",", "[", "]");
        for (MethodParameterInfo methodParameterInfo : methodInfo.getParameterInfo()) {
            parameterTypes.add(methodParameterInfo.getTypeSignature() == null ?
                    signature(methodParameterInfo.getTypeDescriptor()) :
                    signature(methodParameterInfo.getTypeSignature()));
        }
        s.append(",parameters=").append(parameterTypes);

        s.append('}');

        return s.toString();
    }

    public String variableSignature(FieldInfo fieldInfo) {
        return fieldInfo.getClassName() + "{name=" + fieldInfo.getName() + '}';
    }
}
