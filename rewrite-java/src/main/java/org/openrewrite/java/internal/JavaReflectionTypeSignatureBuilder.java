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

import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaTypeSignatureBuilder;

import java.lang.reflect.*;
import java.util.HashSet;
import java.util.Set;
import java.util.StringJoiner;

public class JavaReflectionTypeSignatureBuilder implements JavaTypeSignatureBuilder {
    private Set<String> typeVariableNameStack;

    @Override
    public String signature(@Nullable Object t) {
        if (t == null) {
            return "{undefined}";
        }

        if (t instanceof Class) {
            Class<?> clazz = (Class<?>) t;
            if (clazz.isArray()) {
                return arraySignature(clazz);
            } else if (clazz.isPrimitive()) {
                return primitiveSignature(clazz);
            }

            StringBuilder s = new StringBuilder(classSignature(clazz));

            if (clazz.getTypeParameters().length > 0) {
                StringJoiner typeParams = new StringJoiner(", ", "<", ">");
                for (TypeVariable<?> typeParameter : clazz.getTypeParameters()) {
                    typeParams.add(signature(typeParameter));
                }
                s.append(typeParams);
            }

            return s.toString();
        } else if (t instanceof ParameterizedType) {
            return parameterizedSignature(t);
        } else if (t instanceof WildcardType) {
            return genericSignature(t);
        } else if (t instanceof TypeVariable) {
            return genericSignature(t);
        } else if (t instanceof GenericArrayType) {
            return arraySignature(t);
        }

        throw new UnsupportedOperationException("Unknown type " + t.getClass().getName());
    }

    @Override
    public String arraySignature(Object type) {
        if (type instanceof GenericArrayType) {
            return signature(((GenericArrayType) type).getGenericComponentType()) + "[]";
        } else if (type instanceof Class) {
            Class<?> array = (Class<?>) type;
            return signature(array.getComponentType()) + "[]";
        }

        throw new UnsupportedOperationException("Unknown array type " + type.getClass().getName());
    }

    @Override
    public String classSignature(Object type) {
        Class<?> clazz = (Class<?>) type;
        return clazz.getName();
    }

    @Override
    public String genericSignature(Object type) {
        if (type instanceof TypeVariable) {
            TypeVariable<?> typeVar = (TypeVariable<?>) type;
            String name = typeVar.getName();

            if (typeVariableNameStack == null) {
                typeVariableNameStack = new HashSet<>();
            }

            StringBuilder s = new StringBuilder("Generic{");
            if (typeVariableNameStack.add(name)) {
                if (typeVar.getBounds().length > 0) {
                    String boundsStr = genericBounds(typeVar.getBounds());
                    if (!boundsStr.isEmpty()) {
                        s.append("extends ").append(boundsStr);
                    }
                }
            }

            s.append('}');

            typeVariableNameStack.remove(name);
            return s.toString();
        } else if (type instanceof WildcardType) {
            WildcardType wildcard = (WildcardType) type;
            StringBuilder s = new StringBuilder("Generic{");

            if (wildcard.getLowerBounds().length > 0) {
                String boundsStr = genericBounds(wildcard.getLowerBounds());
                if (!boundsStr.isEmpty()) {
                    s.append("super ").append(boundsStr);
                }
            } else if (wildcard.getUpperBounds().length > 0) {
                String boundsStr = genericBounds(wildcard.getUpperBounds());
                if (!boundsStr.isEmpty()) {
                    s.append("extends ").append(boundsStr);
                }
            }

            s.append('}');

            return s.toString();
        }

        throw new UnsupportedOperationException("Unexpected generic type " + type.getClass().getName());
    }

    private String genericBounds(Type[] bounds) {
        StringJoiner boundJoiner = new StringJoiner(" & ");
        for (Type bound : bounds) {
            String boundStr = signature(bound);
            if (!boundStr.equals("java.lang.Object")) {
                boundJoiner.add(boundStr);
            }
        }
        return boundJoiner.toString();
    }

    @Override
    public String parameterizedSignature(Object type) {
        ParameterizedType pt = (ParameterizedType) type;

        String className = ((Class<?>) pt.getRawType()).getName();
        StringBuilder s = new StringBuilder(className);

        StringJoiner typeParameters = new StringJoiner(", ", "<", ">");
        for (Type typeArgument : pt.getActualTypeArguments()) {
            typeParameters.add(signature(typeArgument));
        }

        s.append(typeParameters);

        return s.toString();
    }

    @Override
    public String primitiveSignature(Object type) {
        return ((Class<?>) type).getName();
    }

    public String methodSignature(Constructor<?> ctor, String declaringTypeName) {
        StringBuilder s = new StringBuilder(declaringTypeName);

        s.append("{name=").append("<constructor>");
        s.append(",return=").append(declaringTypeName);

        StringJoiner parameterTypeSignatures = new StringJoiner(",", "[", "]");
        if (ctor.getParameters().length > 0) {
            for (Parameter parameter : ctor.getParameters()) {
                Type parameterizedType = parameter.getParameterizedType();
                parameterTypeSignatures.add(signature(parameterizedType == null ? parameter.getType() : parameterizedType));
            }
        }
        s.append(",parameters=").append(parameterTypeSignatures);

        s.append('}');

        return s.toString();
    }

    public String methodSignature(Method method, String declaringTypeName) {
        StringBuilder s = new StringBuilder(declaringTypeName);

        s.append("{name=").append(method.getName());
        s.append(",return=").append(signature(method.getGenericReturnType()));

        StringJoiner parameterTypeSignatures = new StringJoiner(",", "[", "]");
        if (method.getParameters().length > 0) {
            for (Parameter parameter : method.getParameters()) {
                Type parameterizedType = parameter.getParameterizedType();
                parameterTypeSignatures.add(signature(parameterizedType == null ? parameter.getType() : parameterizedType));
            }
        }
        s.append(",parameters=").append(parameterTypeSignatures);

        s.append('}');

        return s.toString();
    }

    public String variableSignature(Field field) {
        return field.getDeclaringClass().getName() + "{name=" + field.getName() + '}';
    }
}
