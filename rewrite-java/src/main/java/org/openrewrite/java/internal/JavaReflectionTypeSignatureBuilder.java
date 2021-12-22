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
import java.util.HashSet;
import java.util.Set;
import java.util.StringJoiner;

public class JavaReflectionTypeSignatureBuilder implements JavaTypeSignatureBuilder {
    // fully qualified names of base type of a class or interface bound (minus further parameterization)
    private Set<String> genericStack;

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
            StringBuilder s = new StringBuilder(typeVar.getName());

            if (typeVar.getBounds().length > 0) {
                String boundsStr = genericBounds(typeVar.getBounds());
                if (!boundsStr.isEmpty()) {
                    s.append(" extends ").append(boundsStr);
                }
            }

            return s.toString();
        } else if (type instanceof WildcardType) {
            WildcardType wildcard = (WildcardType) type;
            StringBuilder s = new StringBuilder("?");

            if (wildcard.getLowerBounds().length > 0) {
                String boundsStr = genericBounds(wildcard.getLowerBounds());
                if (!boundsStr.isEmpty()) {
                    s.append(" super ").append(boundsStr);
                }
            } else if (wildcard.getUpperBounds().length > 0) {
                String boundsStr = genericBounds(wildcard.getUpperBounds());
                if (!boundsStr.isEmpty()) {
                    s.append(" extends ").append(boundsStr);
                }
            }

            return s.toString();
        }

        throw new UnsupportedOperationException("Unexpected generic type " + type.getClass().getName());
    }

    private String genericBounds(Type[] bounds) {
        StringJoiner boundJoiner = new StringJoiner(" & ");
        for (Type bound : bounds) {
            if (!isRecursiveGenericBound(bound)) {
                String boundStr = signature(bound);
                if (!boundStr.equals("java.lang.Object")) {
                    boundJoiner.add(boundStr);
                }
                removeFromGenericStack(bound);
            } else {
                removeFromGenericStack(bound);
                // if one of a multi-bound covariant variable matches a type on the stack, short-circuit both
                return "";
            }
        }
        return boundJoiner.toString();
    }

    private void removeFromGenericStack(Type bound) {
        if (bound instanceof ParameterizedType) {
            genericStack.remove(((Class<?>) ((ParameterizedType) bound).getRawType()).getName());
        } else if (bound instanceof Class) {
            Class<?> clazz = (Class<?>) bound;
            if (clazz.isArray()) {
                removeFromGenericStack(clazz.getComponentType());
            }
            genericStack.remove(clazz.getName());
        }
    }

    private boolean isRecursiveGenericBound(Type bound) {
        String className;
        if (bound instanceof ParameterizedType) {
            className = ((Class<?>) ((ParameterizedType) bound).getRawType()).getName();
        } else if (bound instanceof Class) {
            Class<?> clazz = (Class<?>) bound;
            if (clazz.isArray()) {
                return isRecursiveGenericBound(clazz.getComponentType());
            }
            className = clazz.getName();
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
        ParameterizedType pt = (ParameterizedType) type;

        String className = ((Class<?>) pt.getRawType()).getName();
        StringBuilder s = new StringBuilder(className);

        if (genericStack == null) {
            genericStack = new HashSet<>();
        }

        genericStack.add(className);
        StringJoiner typeParameters = new StringJoiner(", ", "<", ">");
        for (Type typeArgument : pt.getActualTypeArguments()) {
            typeParameters.add(signature(typeArgument));
        }
        genericStack.remove(className);

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
                Type parameterizedType = parameter.getParameterizedType();
                argumentTypeSignatures.add(signature(parameterizedType == null ? parameter.getType() : parameterizedType));
            }
        }
        s.append(",resolved=").append(method.getReturnType().getName()).append('(').append(argumentTypeSignatures).append(')');
        s.append(",generic=").append(method.getReturnType().getName()).append('(').append(argumentTypeSignatures).append(')');
        s.append('}');

        return s.toString();
    }

    public String variableSignature(Field field) {
        // Formatted like com.MyThing{name=MY_FIELD,type=java.lang.String}
        return field.getDeclaringClass().getName() + "{name=" + field.getName() + '}';
    }
}
