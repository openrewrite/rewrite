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
package org.openrewrite.java.tree;

import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaTypeSignatureBuilder;

import java.util.HashSet;
import java.util.Set;
import java.util.StringJoiner;

class DefaultJavaTypeSignatureBuilder implements JavaTypeSignatureBuilder {
    static final DefaultJavaTypeSignatureBuilder TO_STRING = new DefaultJavaTypeSignatureBuilder();

    @Nullable
    private Set<String> typeVariableNameStack;

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
        StringBuilder s = new StringBuilder("Generic{" + gtv.getName());

        if (typeVariableNameStack == null) {
            typeVariableNameStack = new HashSet<>();
        }
        if (!gtv.getName().equals("?") && !typeVariableNameStack.add(gtv.getName())) {
            typeVariableNameStack.remove(gtv.getName());
            s.append('}');
            return s.toString();
        }

        switch (gtv.getVariance()) {
            case INVARIANT:
                break;
            case COVARIANT:
                s.append(" extends ");
                break;
            case CONTRAVARIANT:
                s.append(" super ");
                break;
        }

        StringJoiner bounds = new StringJoiner(" & ");
        for (JavaType bound : gtv.getBounds()) {
            bounds.add(signature(bound));
        }

        s.append(bounds).append('}');
        typeVariableNameStack.remove(gtv.getName());

        return s.toString();
    }

    @Override
    public String parameterizedSignature(Object type) {
        JavaType.Parameterized pt = (JavaType.Parameterized) type;

        StringBuilder s = new StringBuilder(signature(pt.getType()));

        StringJoiner typeParameters = new StringJoiner(", ", "<", ">");
        for (JavaType typeParameter : pt.getTypeParameters()) {
            typeParameters.add(signature(typeParameter));
        }
        s.append(typeParameters);

        return s.toString();
    }

    @Override
    public String primitiveSignature(Object type) {
        return ((JavaType.Primitive) type).getKeyword();
    }

    public String variableSignature(JavaType.Variable variable) {
        return variable.getOwner().getFullyQualifiedName() + "{name=" + variable.getName() + '}';
    }

    public String methodSignature(JavaType.Method method) {
        StringBuilder s = new StringBuilder(signature(method.getDeclaringType()));
        s.append("{name=").append(method.getName());

        StringJoiner resolvedArgumentTypes = new StringJoiner(",");
        for (JavaType paramType : method.getResolvedSignature().getParamTypes()) {
            resolvedArgumentTypes.add(signature(paramType));
        }
        s.append(",resolved=");
        s.append(signature(method.getResolvedSignature().getReturnType()));
        s.append('(').append(resolvedArgumentTypes).append(')');

        StringJoiner genericArgumentTypes = new StringJoiner(",");
        for (JavaType paramType : method.getGenericSignature().getParamTypes()) {
            genericArgumentTypes.add(signature(paramType));
        }
        s.append(",generic=");
        s.append(signature(method.getGenericSignature().getReturnType()));
        s.append('(').append(genericArgumentTypes).append(')');

        s.append('}');

        return s.toString();
    }
}
