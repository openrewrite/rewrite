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
import org.jspecify.annotations.Nullable;
import org.openrewrite.java.JavaTypeSignatureBuilder;

import java.util.HashSet;
import java.util.Set;
import java.util.StringJoiner;

class GroovyAstTypeSignatureBuilder implements JavaTypeSignatureBuilder {
    @Nullable
    private Set<String> typeVariableNameStack;

    @Override
    public String signature(@Nullable Object t) {
        if (t == null) {
            return "{undefined}";
        }

        try {
            ASTNode astNode = (ASTNode) t;
            if (astNode instanceof ClassNode) {
                ClassNode clazz = (ClassNode) astNode;
                if (clazz.isArray()) {
                    return arraySignature(clazz);
                } else if (ClassHelper.isPrimitiveType(clazz)) {
                    return primitiveSignature(clazz);
                } else if (clazz.isUsingGenerics()) {
                    return parameterizedSignature(clazz);
                }
                return classSignature(astNode);
            } else if (astNode instanceof GenericsType) {
                return genericSignature(astNode);
            } else if (astNode instanceof MethodNode) {
                return methodSignature((MethodNode) astNode);
            } else if (astNode instanceof FieldNode) {
                return variableSignature((FieldNode) astNode);
            }
        } catch (NoClassDefFoundError e) {
            // e.getMessage() returns fully qualified name of type that couldn't be found on the classpath
            return e.getMessage();
        }

        throw new UnsupportedOperationException("Unexpected type " + t.getClass().getName());
    }

    @Override
    public String arraySignature(Object type) {
        ClassNode clazz = (ClassNode) type;
        String component;
        if (clazz.getComponentType().isUsingGenerics()) {
            component = genericSignature(clazz.getComponentType().getGenericsTypes()[0]);
        } else {
            component = signature(clazz.getComponentType());
        }
        return component + "[]";
    }

    @Override
    public String classSignature(Object type) {
        ClassNode clazz = (ClassNode) type;
        return clazz.getName();
    }

    @Override
    public String genericSignature(Object type) {
        GenericsType g = (GenericsType) type;

        if (!g.isPlaceholder() && !g.isWildcard()) {
            // this is a type name used in a parameterized type
            return signature(g.getType());
        }

        String name = g.getName();

        if (typeVariableNameStack == null) {
            typeVariableNameStack = new HashSet<>();
        }

        if (!typeVariableNameStack.add(name)) {
            typeVariableNameStack.remove(name);
            return "Generic{" + name + "}";
        }

        StringBuilder s = new StringBuilder("Generic{" + name);


        StringJoiner bounds = new StringJoiner(" & ");

        if (g.getUpperBounds() != null) {
            s.append(" extends ");
            for (ClassNode bound : g.getUpperBounds()) {
                bounds.add(signature(bound));
            }
        } else if (g.getLowerBound() != null) {
            s.append(" super ");
            bounds.add(signature(g.getLowerBound()));
        }

        s.append(bounds);

        typeVariableNameStack.remove(name);

        return s.append("}").toString();
    }

    @Override
    public String parameterizedSignature(Object type) {
        ClassNode classNode = (ClassNode) type;

        StringBuilder s = new StringBuilder(classSignature(type));
        StringJoiner typeParameters = new StringJoiner(", ", "<", ">");
        if (classNode.getGenericsTypes() != null) {
            for (GenericsType genericsType : classNode.getGenericsTypes()) {
                typeParameters.add(signature(genericsType));
            }
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
        s.append("{name=").append(node instanceof ConstructorNode ? "<constructor>" : node.getName());

        s.append(",return=").append(node instanceof ConstructorNode ?
                node.getDeclaringClass().getName() :
                signature(node.getReturnType()));

        StringJoiner parameterTypes = new StringJoiner(",", "[", "]");
        if (node.getParameters().length > 0) {
            for (org.codehaus.groovy.ast.Parameter parameter : node.getParameters()) {
                parameterTypes.add(signature(parameter.getOriginType()));
            }
        }

        s.append(",parameters=").append(parameterTypes);
        s.append('}');

        return s.toString();
    }

    public String variableSignature(FieldNode declaredField) {
        String owner = signature(declaredField.getOwner());
        if (owner.contains("<")) {
            owner = owner.substring(0, owner.indexOf('<'));
        }
        return owner + "{name=" + declaredField.getName() + '}';
    }

    public String variableSignature(String name) {
        return "{undefined}{name=" + name + '}';
    }
}
