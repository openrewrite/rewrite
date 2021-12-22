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
import org.openrewrite.internal.lang.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.StringJoiner;

public class ClassgraphJavaTypeSignatureBuilder implements JavaTypeSignatureBuilder {
    private Set<String> typeVariableNameStack;

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
        StringBuilder s = new StringBuilder(signature(arrSignature.getNestedType()));
        for (int i = 0; i < arrSignature.getNumDimensions(); i++) {
            s.append("[]");
        }
        return s.toString();
    }

    @Override
    public String classSignature(Object type) {
        ClassRefTypeSignature classRefTypeSignature = (ClassRefTypeSignature) type;
        return classRefTypeSignature.getBaseClassName();
    }

    @Override
    public String genericSignature(Object type) {
        if (type instanceof TypeVariableSignature) {
            TypeVariableSignature typeVariableSignature = (TypeVariableSignature) type;
            try {
                return signature(typeVariableSignature.resolve()); // resolves to a TypeParameter
            } catch (IllegalArgumentException ignored) {
                return typeVariableSignature.getName();
            }
        } else if (type instanceof TypeArgument) {
            return generic((TypeArgument) type);
        } else if (type instanceof TypeParameter) {
            if (typeVariableNameStack == null) {
                typeVariableNameStack = new HashSet<>();
            }
            String name = ((TypeParameter) type).getName();
            if (typeVariableNameStack.add(name)) {
                String s = generic((TypeParameter) type);
                typeVariableNameStack.remove(name);
                return s;
            }
            return name;
        }

        throw new UnsupportedOperationException("Unexpected generic type " + type.getClass().getName());
    }

    private String generic(TypeArgument typeArgument) {
        StringBuilder s = new StringBuilder();
        switch (typeArgument.getWildcard()) {
            case NONE:
                s.append(signature(typeArgument.getTypeSignature()));
                break;
            case EXTENDS:
                s.append("? extends ").append(signature(typeArgument.getTypeSignature()));
                break;
            case SUPER:
                s.append("? super ").append(signature(typeArgument.getTypeSignature()));
                break;
            case ANY:
                s.append("?");
        }

        return s.toString();
    }

    public String generic(TypeParameter typeParameter) {
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

        String boundsStr = bounds.toString();
        return typeParameter.getName() + (boundsStr.isEmpty() ? "" : " extends " + boundsStr);
    }

    @Override
    public String parameterizedSignature(Object type) {
        StringBuilder s = new StringBuilder();
        StringJoiner typeParameters = new StringJoiner(", ", "<", ">");

        if (type instanceof ClassTypeSignature) {
            ClassTypeSignature clazz = (ClassTypeSignature) type;
            String className = className(clazz);
            s.append(className);
            if (!clazz.getTypeParameters().isEmpty()) {
                for (TypeParameter typeParameter : clazz.getTypeParameters()) {
                    typeParameters.add(signature(typeParameter));
                }
            }
        } else if (type instanceof ClassRefTypeSignature) {
            ClassRefTypeSignature clazz = (ClassRefTypeSignature) type;
            s.append(clazz.getBaseClassName());
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

    private String className(ClassTypeSignature clazz) {
        try {
            Method getClassName = ClassTypeSignature.class.getDeclaredMethod("getClassName");
            getClassName.setAccessible(true);
            return (String) getClassName.invoke(clazz);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String primitiveSignature(Object type) {
        BaseTypeSignature baseTypeSignature = (BaseTypeSignature) type;
        return baseTypeSignature.getTypeStr();
    }

    public String methodSignature(MethodInfo methodInfo) {
        StringBuilder s = new StringBuilder(signature(methodInfo.getClassInfo().getTypeSignature()));
        s.append("{name=").append(methodInfo.getName());

        StringJoiner resolvedArgumentTypes = new StringJoiner(",");
        for (MethodParameterInfo methodParameterInfo : methodInfo.getParameterInfo()) {
            resolvedArgumentTypes.add(signature(methodParameterInfo.getTypeDescriptor()));
        }
        s.append(",resolved=");
        s.append(signature(methodInfo.getTypeDescriptor().getResultType()));
        s.append('(').append(resolvedArgumentTypes).append(')');

        StringJoiner genericArgumentTypes = new StringJoiner(",");
        for (MethodParameterInfo methodParameterInfo : methodInfo.getParameterInfo()) {
            genericArgumentTypes.add(methodParameterInfo.getTypeSignature() == null ?
                    signature(methodParameterInfo.getTypeDescriptor()) :
                    signature(methodParameterInfo.getTypeSignature()));
        }
        s.append(",generic=");
        s.append(methodInfo.getTypeSignature() == null ?
                signature(methodInfo.getTypeDescriptor().getResultType()) :
                signature(methodInfo.getTypeSignature().getResultType()));
        s.append('(').append(genericArgumentTypes).append(')');

        s.append('}');

        return s.toString();
    }

    public String variableSignature(FieldInfo fieldInfo) {
        // Formatted like com.MyThing{name=MY_FIELD}
        return fieldInfo.getClassName() + "{name=" + fieldInfo.getName() + '}';
    }
}
