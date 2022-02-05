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
import org.openrewrite.java.tree.JavaType;

import java.util.*;

public class DefaultJavaTypeSignatureBuilder implements JavaTypeSignatureBuilder {
    public static final DefaultJavaTypeSignatureBuilder TO_STRING = new DefaultJavaTypeSignatureBuilder();

    @Nullable
    private Set<JavaType> typeVariableStack;

    @Nullable
    private Set<JavaType> parameterizedStack;

    @Override
    public String signature(@Nullable Object type) {
        if (type == null || type == JavaType.Unknown.getInstance()) {
            return "{undefined}";
        }

        if (type instanceof JavaType.Class) {
            return classSignature(type);
        } else if (type instanceof JavaType.Array) {
            return arraySignature(type);
        } else if (type instanceof JavaType.Parameterized) {
            return parameterizedSignature(type);
        } else if (type instanceof JavaType.GenericTypeVariable) {
            return genericSignature(type);
        } else if (type instanceof JavaType.Primitive) {
            return primitiveSignature(type);
        } else if(type instanceof JavaType.Method) {
            return methodSignature((JavaType.Method) type);
        } else if(type instanceof JavaType.Variable) {
            return variableSignature((JavaType.Variable) type);
        }

        throw new UnsupportedOperationException("Unexpected type " + type.getClass().getName());
    }

    @Override
    public String arraySignature(Object type) {
        return signature(((JavaType.Array) type).getElemType()) + "[]";
    }

    @Override
    public String classSignature(Object type) {
        return ((JavaType.Class) type).getFullyQualifiedName();
    }

    @Override
    public String genericSignature(Object type) {
        JavaType.GenericTypeVariable gtv = (JavaType.GenericTypeVariable) type;
        StringBuilder s = new StringBuilder("Generic{");

        if (typeVariableStack == null) {
            typeVariableStack = Collections.newSetFromMap(new IdentityHashMap<>());
        }

        if (!typeVariableStack.add(gtv)) {
            s.append('}');
            return s.toString();
        }

        switch (gtv.getVariance()) {
            case INVARIANT:
                break;
            case COVARIANT:
                s.append("extends ");
                break;
            case CONTRAVARIANT:
                s.append("super ");
                break;
        }

        StringJoiner bounds = new StringJoiner(" & ");
        for (JavaType bound : gtv.getBounds()) {
            if(parameterizedStack == null || !parameterizedStack.contains(bound)) {
                bounds.add(signature(bound));
            }
        }

        s.append(bounds).append('}');
        typeVariableStack.remove(gtv);

        return s.toString();
    }

    @Override
    public String parameterizedSignature(Object type) {
        JavaType.Parameterized pt = (JavaType.Parameterized) type;

        if(parameterizedStack == null) {
            parameterizedStack = Collections.newSetFromMap(new IdentityHashMap<>());
        }
        parameterizedStack.add(pt);

        String baseType = signature(pt.getType());
        StringBuilder s = new StringBuilder(baseType);

        StringJoiner typeParameters = new StringJoiner(", ", "<", ">");
        for (JavaType typeParameter : pt.getTypeParameters()) {
            typeParameters.add(signature(typeParameter));
        }
        s.append(typeParameters);

        parameterizedStack.remove(pt);
        return s.toString();
    }

    @Override
    public String primitiveSignature(Object type) {
        return ((JavaType.Primitive) type).getKeyword();
    }

    public String variableSignature(JavaType.Variable variable) {
        return signature(variable.getOwner()) + "{name=" + variable.getName() + '}';
    }

    public String methodSignature(JavaType.Method method) {
        StringBuilder s = new StringBuilder(signature(method.getDeclaringType()));
        s.append("{name=").append(method.getName());

        s.append(",return=").append(method.getReturnType());

        StringJoiner parameterTypes = new StringJoiner(",", "[", "]");
        for (JavaType paramType : method.getParameterTypes()) {
            parameterTypes.add(signature(paramType));
        }
        s.append(",parameters=").append(parameterTypes);

        s.append('}');

        return s.toString();
    }
}
