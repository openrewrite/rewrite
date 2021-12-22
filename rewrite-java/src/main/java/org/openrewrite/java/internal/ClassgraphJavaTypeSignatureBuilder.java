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
import java.util.*;

public class ClassgraphJavaTypeSignatureBuilder implements JavaTypeSignatureBuilder {
    // fully qualified names of base type of a class or interface bound (minus further parameterization)
    private Set<String> genericStack;

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
        } else if (typeSignature instanceof TypeVariableSignature) {
            TypeVariableSignature typeVariableSignature = (TypeVariableSignature) typeSignature;
            try {
                // resolves to a TypeParameter
                return signature(typeVariableSignature.resolve());
            } catch (IllegalArgumentException ignored) {
                return typeVariableSignature.getName();
            }
        } else if (typeSignature instanceof TypeParameter) {
            return genericSignature(typeSignature);
        } else if (typeSignature instanceof BaseTypeSignature) {
            return primitiveSignature(typeSignature);
        } else if (typeSignature instanceof TypeArgument) {
            TypeArgument typeArgument = (TypeArgument) typeSignature;
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

        throw new UnsupportedOperationException("Unexpected signature type " + type.getClass().getName());
    }

    @Override
    public String arraySignature(Object type) {
        ArrayTypeSignature arrSignature = (ArrayTypeSignature) type;
        StringBuilder signature = new StringBuilder(signature(arrSignature.getElementTypeSignature()));
        for (int i = 0; i < arrSignature.getNumDimensions(); i++) {
            signature.append("[]");
        }
        return signature.toString();
    }

    @Override
    public String classSignature(Object type) {
        ClassRefTypeSignature classRefTypeSignature = (ClassRefTypeSignature) type;
        return classRefTypeSignature.getBaseClassName();
    }

    @Override
    public String genericSignature(Object type) {
        TypeParameter generic = (TypeParameter) type;

        StringBuilder bounds = new StringBuilder();

        if (generic.getClassBound() != null) {
            if (!isRecursiveGenericBound(generic.getClassBound())) {
                String bound = signature(generic.getClassBound());
                if (!bound.equals("java.lang.Object")) {
                    bounds.append(bound);
                }
            }
        } else if (generic.getInterfaceBounds() != null) {
            StringJoiner interfaceBounds = new StringJoiner(" & ");
            for (ReferenceTypeSignature interfaceBound : generic.getInterfaceBounds()) {
                if (!isRecursiveGenericBound(interfaceBound)) {
                    interfaceBounds.add(signature(interfaceBound));
                }
            }
            bounds.append(interfaceBounds);
        }

        String boundsStr = bounds.toString();
        return generic.getName() + (boundsStr.isEmpty() ? "" : " extends " + boundsStr);
    }

    private boolean isRecursiveGenericBound(Object bound) {
        String className;
        if (bound instanceof ClassTypeSignature) {
            className = className((ClassTypeSignature) bound);
        } else if (bound instanceof ClassRefTypeSignature) {
            className = ((ClassRefTypeSignature) bound).getBaseClassName();
        } else if (bound instanceof ArrayTypeSignature) {
            return isRecursiveGenericBound(((ArrayTypeSignature) bound).getElementTypeSignature());
        } else {
            return false;
        }

        if (genericStack == null) {
            genericStack = new HashSet<>();
        }
        return !genericStack.add(className);
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
                if (genericStack == null) {
                    genericStack = new HashSet<>();
                }
                genericStack.add(className);
                for (TypeParameter typeParameter : clazz.getTypeParameters()) {
                    typeParameters.add(signature(typeParameter));
                }
                genericStack.remove(className);
            }
        } else if (type instanceof ClassRefTypeSignature) {
            ClassRefTypeSignature clazz = (ClassRefTypeSignature) type;
            s.append(clazz.getBaseClassName());
            if (!clazz.getTypeArguments().isEmpty()) {
                if (genericStack == null) {
                    genericStack = new HashSet<>();
                }
                genericStack.add(clazz.getBaseClassName());
                for (TypeArgument typeArgument : clazz.getTypeArguments()) {
                    typeParameters.add(signature(typeArgument));
                }
                genericStack.remove(clazz.getBaseClassName());
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
