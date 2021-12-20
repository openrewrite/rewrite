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
package org.openrewrite.groovy;

import org.codehaus.groovy.ast.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.internal.JavaTypeSignatureBuilder;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.StringJoiner;

public class GroovyAstTypeSignatureBuilder implements JavaTypeSignatureBuilder {
    @Nullable
    private Set<ClassNode> typeStack;

    @Override
    public String signature(@Nullable Object t) {
        if (t == null) {
            return "{undefined}";
        }

        ASTNode astNode = (ASTNode) t;
        if(astNode instanceof ClassNode) {
            ClassNode clazz = (ClassNode) astNode;
            if(clazz.isArray()) {
                return arraySignature(clazz);
            } else if(ClassHelper.isPrimitiveType(clazz)) {
                return primitiveSignature(clazz);
            } else if(clazz.isUsingGenerics()) {
                return parameterizedSignature(clazz);
            }
            return classSignature(astNode);
        } else if(astNode instanceof GenericsType) {
            return genericSignature(astNode);
        }

        throw new UnsupportedOperationException("Unexpected type " + t.getClass().getName());
    }

    @Override
    public String arraySignature(Object type) {
        ClassNode clazz = (ClassNode) type;
        return signature(clazz.getComponentType()) + "[]";
    }

    @Override
    public String classSignature(Object type) {
        ClassNode clazz = (ClassNode) type;
        return ((ClassNode) type).getName();
    }

    @Override
    public String genericSignature(Object type) {
        GenericsType g = (GenericsType) type;

        StringBuilder s = new StringBuilder(g.getName());
        s.append(" extends ");

        StringJoiner bounds = new StringJoiner(" & ");
        for (ClassNode bound : g.getUpperBounds()) {
            bounds.add(genericBound(bound));
        }
        s.append(bounds);

        return s.toString();
    }

    private String genericBound(ClassNode bound) {
        if (typeStack != null && typeStack.contains(bound)) {
            return "(*)";
        }

        if (typeStack == null) {
            typeStack = Collections.newSetFromMap(new IdentityHashMap<>());
        }
        typeStack.add(bound);
        return signature(bound);
    }

    @Override
    public String parameterizedSignature(Object type) {
        ClassNode classNode = (ClassNode) type;

        StringBuilder s = new StringBuilder(classSignature(type));
        StringJoiner typeParameters = new StringJoiner(",", "<", ">");
        for (GenericsType genericsType : classNode.getGenericsTypes()) {
            typeParameters.add(signature(genericsType));
        }
        s.append(typeParameters);

        return s.toString();
    }

    @Override
    public String primitiveSignature(Object type) {
        ClassNode clazz = (ClassNode) type;
        return clazz.getName();
    }

    public String methodSignature(MethodNode node) {
        StringBuilder s = new StringBuilder(node.getDeclaringClass().getName());
        s.append("{name=").append(node.getName());

        StringJoiner argumentTypeSignatures = new StringJoiner(",");
        if (node.getParameters().length > 0) {
            for (org.codehaus.groovy.ast.Parameter parameter : node.getParameters()) {
                argumentTypeSignatures.add(parameter.getOriginType().getName());
            }
        }

        s.append(",resolved=").append(signature(node.getReturnType())).append('(').append(argumentTypeSignatures).append(')');

        // TODO how do we calculate the generic signature?
        s.append(",generic=").append(signature(node.getReturnType())).append('(').append(argumentTypeSignatures).append(')');
        s.append('}');

        return s.toString();
    }
}
