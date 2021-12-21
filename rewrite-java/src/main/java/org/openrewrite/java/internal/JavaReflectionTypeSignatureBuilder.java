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

import java.lang.reflect.*;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.StringJoiner;

public class JavaReflectionTypeSignatureBuilder implements JavaTypeSignatureBuilder {
    @Nullable
    private Set<Object> typeStack;

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
        }

        throw new UnsupportedOperationException("Unknown type " + t.getClass().getName());
    }

    @Override
    public String arraySignature(Object type) {
        Class<?> array = (Class<?>) type;
        return signature(array.getComponentType()) + "[]";
    }

    @Override
    public String classSignature(Object type) {
        Class<?> clazz = (Class<?>) type;
        return clazz.getName();
    }

    @Override
    public String genericSignature(Object type) {
        if (typeStack == null) {
            typeStack = Collections.newSetFromMap(new IdentityHashMap<>());
        }

        boolean unique = typeStack.add(type);

        if (type instanceof TypeVariable) {
            TypeVariable<?> typeVar = (TypeVariable<?>) type;

            if (!unique) {
                return typeVar.getTypeName();
            }

            StringBuilder s = new StringBuilder(typeVar.getName());

            if (typeVar.getBounds().length > 0) {
                StringJoiner bounds = new StringJoiner(" & ");
                for (Type bound : typeVar.getBounds()) {
                    String boundStr = signature(bound);
                    if (!boundStr.equals("java.lang.Object")) {
                        bounds.add(boundStr);
                    }
                }

                String boundsStr = bounds.toString();
                if (!boundsStr.isEmpty()) {
                    s.append(" extends ").append(boundsStr);
                }
            }

            typeStack.remove(type);
            return s.toString();
        } else if (type instanceof WildcardType) {
            WildcardType wildcard = (WildcardType) type;

            StringBuilder s = new StringBuilder("?");
            if (wildcard.getLowerBounds().length > 0) {
                StringJoiner bounds = new StringJoiner(" & ");
                for (Type bound : wildcard.getLowerBounds()) {
                    String boundStr = signature(bound);
                    if (!boundStr.equals("java.lang.Object")) {
                        bounds.add(boundStr);
                    }
                }

                String boundsStr = bounds.toString();
                if (!boundsStr.isEmpty()) {
                    s.append(" super ").append(boundsStr);
                }
            } else if (wildcard.getUpperBounds().length > 0) {
                StringJoiner bounds = new StringJoiner(" & ");
                for (Type bound : wildcard.getUpperBounds()) {
                    String boundStr = signature(bound);
                    if (!boundStr.equals("java.lang.Object")) {
                        bounds.add(boundStr);
                    }
                }

                String boundsStr = bounds.toString();
                if (!boundsStr.isEmpty()) {
                    s.append(" extends ").append(boundsStr);
                }
            }

            typeStack.remove(type);
            return s.toString();
        }

        throw new UnsupportedOperationException("Unexpected generic type " + type.getClass().getName());
    }

    @Override
    public String parameterizedSignature(Object type) {
        ParameterizedType pt = (ParameterizedType) type;

        StringBuilder s = new StringBuilder(((Class<?>) pt.getRawType()).getName());

        if(s.toString().equals("java.util.stream.BaseStream")) {
            System.out.println("here");
        }

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

    public String methodSignature(Method method) {
        // Formatted like com.MyThing{name=add,resolved=Thing(Integer),generic=Thing<?>(Integer)}
        StringBuilder s = new StringBuilder(method.getDeclaringClass().getName());
        s.append("{name=").append(method.getName());

        StringJoiner argumentTypeSignatures = new StringJoiner(",");
        if (method.getParameters().length > 0) {
            for (Parameter parameter : method.getParameters()) {
                argumentTypeSignatures.add(method.getDeclaringClass().getName());
            }
        }
        s.append(",resolved=").append(method.getReturnType().getName()).append('(').append(argumentTypeSignatures).append(')');

        // TODO how do we calculate the generic signature?
        s.append(",generic=").append(method.getReturnType().getName()).append('(').append(argumentTypeSignatures).append(')');
        s.append('}');

        return s.toString();
    }

    public String variableSignature(Field field) {
        // Formatted like com.MyThing{name=MY_FIELD,type=java.lang.String}
        return field.getDeclaringClass().getName() + "{name=" + field.getName() + '}';
    }
}
