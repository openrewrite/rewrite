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

import org.jspecify.annotations.Nullable;
import org.openrewrite.java.JavaTypeSignatureBuilder;
import org.openrewrite.java.tree.JavaType;

import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

import static java.util.Collections.newSetFromMap;

public class DefaultJavaTypeSignatureBuilder implements JavaTypeSignatureBuilder {
    @Nullable
    private Set<String> typeVariableNameStack;

    @Nullable
    private Set<JavaType> parameterizedStack;

    @Override
    public String signature(@Nullable Object type) {
        if (type == null || type instanceof JavaType.Unknown) {
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
        } else if (type instanceof JavaType.Method) {
            return methodSignature((JavaType.Method) type);
        } else if (type instanceof JavaType.Variable) {
            return variableSignature((JavaType.Variable) type);
        } else if (type instanceof JavaType.Intersection) {
            return intersectionSignature(type);
        } else if (type instanceof JavaType.Annotation) {
            return annotationSignature(type);
        } else if (type instanceof JavaType.MultiCatch) {
            return multiCatchSignature(type);
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
            typeVariableNameStack = new LinkedHashSet<>();
        }
        if (!"?".equals(gtv.getName()) && !typeVariableNameStack.add(gtv.getName())) {
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
            if (parameterizedStack == null || !parameterizedStack.contains(bound)) {
                bounds.add(signature(bound));
            }
        }

        s.append(bounds).append('}');
        typeVariableNameStack.remove(gtv.getName());

        return s.toString();
    }

    private String intersectionSignature(Object type) {
        JavaType.Intersection it = (JavaType.Intersection) type;
        StringJoiner bounds = new StringJoiner(" & ");
        for (JavaType bound : it.getBounds()) {
            bounds.add(signature(bound));
        }
        return bounds.toString();
    }

    @Override
    public String parameterizedSignature(Object type) {
        JavaType.Parameterized pt = (JavaType.Parameterized) type;

        if (parameterizedStack == null) {
            parameterizedStack = newSetFromMap(new IdentityHashMap<>());
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
        return signature(variable.getOwner()) + "{name=" + variable.getName() + ",type=" + signature(variable.getType()) + '}';
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

    private String annotationSignature(Object type) {
        JavaType.Annotation annotation = (JavaType.Annotation) type;
        StringBuilder s = new StringBuilder("@");
        s.append(signature(annotation.getType()));
        List<JavaType.Annotation.ElementValue> values = annotation.getValues();
        if (!values.isEmpty()) {
            s.append('(');
            for (int i = 0; i < values.size(); i++) {
                if (i > 0) {
                    s.append(',');
                }
                JavaType.Annotation.ElementValue value = values.get(i);
                s.append(signature(value.getElement())).append('=').append(value.getValue());
            }
            s.append(')');
        }
        return s.toString();
    }

    private String multiCatchSignature(Object type) {
        JavaType.MultiCatch multiCatch = (JavaType.MultiCatch) type;
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < multiCatch.getThrowableTypes().size(); i++) {
            if (i > 0) {
                s.append('|');
            }
            s.append(signature(multiCatch.getThrowableTypes().get(i)));
        }
        return s.toString();
    }
}
