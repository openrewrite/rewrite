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

import org.codehaus.groovy.GroovyBugError;
import org.codehaus.groovy.ast.*;
import org.jspecify.annotations.Nullable;
import org.openrewrite.java.JavaTypeMapping;
import org.openrewrite.java.internal.JavaReflectionTypeMapping;
import org.openrewrite.java.internal.JavaTypeCache;
import org.openrewrite.java.tree.Flag;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.openrewrite.java.tree.JavaType.GenericTypeVariable.Variance.CONTRAVARIANT;
import static org.openrewrite.java.tree.JavaType.GenericTypeVariable.Variance.COVARIANT;
import static org.openrewrite.java.tree.JavaType.GenericTypeVariable.Variance.INVARIANT;

class GroovyTypeMapping implements JavaTypeMapping<ASTNode> {
    private final GroovyAstTypeSignatureBuilder signatureBuilder = new GroovyAstTypeSignatureBuilder();

    private final JavaTypeCache typeCache;
    private final JavaReflectionTypeMapping reflectionTypeMapping;

    GroovyTypeMapping(JavaTypeCache typeCache) {
        this.typeCache = typeCache;
        this.reflectionTypeMapping = new JavaReflectionTypeMapping(typeCache);
    }

    @Override
    public JavaType type(@Nullable ASTNode type) {
        if (type == null) {
            return JavaType.Class.Unknown.getInstance();
        }

        String signature = signatureBuilder.signature(type);
        JavaType existing = typeCache.get(signature);
        if (existing != null) {
            return existing;
        }

        try {
            if (type instanceof ClassNode) {
                ClassNode clazz = (ClassNode) type;
                if (clazz.isArray()) {
                    return arrayType(clazz, signature);
                } else if (ClassHelper.isPrimitiveType(clazz)) {
                    //noinspection ConstantConditions
                    return JavaType.Primitive.fromKeyword(clazz.getName());
                } else if (clazz.isUsingGenerics() && clazz.getGenericsTypes() != null) {
                    return parameterizedType(clazz, signature);
                }
                return classType((ClassNode) type, signature);
            } else if (type instanceof GenericsType) {
                return genericType((GenericsType) type, signature);
            } else if (type instanceof MethodNode) {
                //noinspection ConstantConditions
                return methodType((MethodNode) type);
            } else if (type instanceof FieldNode) {
                //noinspection ConstantConditions
                return variableType((FieldNode) type);
            }
        } catch (NoClassDefFoundError e) {
            // e.getMessage() returns fully qualified name of type that couldn't be found on the classpath
            return JavaType.ShallowClass.build(e.getMessage());
        }

        throw new UnsupportedOperationException("Unknown type " + type.getClass().getName());
    }

    private JavaType.FullyQualified classType(ClassNode node, String signature) {
        try {
            JavaType type = reflectionTypeMapping.type(node.getTypeClass());
            return (JavaType.FullyQualified) (type instanceof JavaType.Parameterized ? ((JavaType.Parameterized) type).getType() : type);
        } catch (GroovyBugError | NoClassDefFoundError ignored1) {
            JavaType.Class clazz;
            clazz = new JavaType.Class(null, Flag.Public.getBitMask(), node.getName(), JavaType.Class.Kind.Class,
                    null, null, null, null, null, null, null);
            typeCache.put(signature, clazz);

            JavaType.FullyQualified supertype = TypeUtils.asFullyQualified(type(node.getSuperClass()));
            JavaType.FullyQualified owner = TypeUtils.asFullyQualified(type(node.getOuterClass()));

            List<JavaType.Variable> fields = null;
            if(!node.getFields().isEmpty()) {
                fields = new ArrayList<>(node.getFields().size());
                for (FieldNode field : node.getFields()) {
                    if (!field.isSynthetic()) {
                        fields.add(variableType(field));
                    }
                }
            }

            List<JavaType.Method> methods = null;
            if(!node.getMethods().isEmpty()) {
                methods = new ArrayList<>(node.getMethods().size());
                for (MethodNode method : node.getMethods()) {
                    if (!method.isSynthetic()) {
                        methods.add(methodType(method));
                    }
                }
            }

            List<JavaType.FullyQualified> interfaces = null;
            if (node.getInterfaces().length > 0) {
                interfaces = new ArrayList<>(node.getInterfaces().length);
                for (ClassNode iParam : node.getInterfaces()) {
                    JavaType.FullyQualified javaType = TypeUtils.asFullyQualified(type(iParam));
                    if (javaType != null) {
                        interfaces.add(javaType);
                    }
                }
            }

            List<JavaType.FullyQualified> annotations = getAnnotations(node);

            clazz.unsafeSet(null, supertype, owner, annotations, interfaces, fields, methods);
            return clazz;
        }

    }

    private JavaType parameterizedType(ClassNode type, String signature) {
        JavaType.Parameterized pt = new JavaType.Parameterized(null, null, null);
        typeCache.put(signature, pt);

        JavaType.FullyQualified clazz = classType(type, type.getPlainNodeReference().getName());

        List<JavaType> typeParameters = emptyList();
        if (type.getGenericsTypes() != null && type.getGenericsTypes().length > 0) {
            typeParameters = new ArrayList<>(type.getGenericsTypes().length);
            for (GenericsType g : type.getGenericsTypes()) {
                typeParameters.add(type(g));
            }
        }

        pt.unsafeSet(clazz, typeParameters);
        return pt;
    }

    private JavaType.Array arrayType(ClassNode array, String signature) {
        JavaType.Array arr = new JavaType.Array(null, null, null);
        typeCache.put(signature, arr);

        if (array.getComponentType().isUsingGenerics()) {
            arr.unsafeSet(type(array.getComponentType().getGenericsTypes()[0]), null);
        } else {
            arr.unsafeSet(type(array.getComponentType()), null);
        }

        return arr;
    }

    private JavaType genericType(GenericsType g, String signature) {
        if (!g.isPlaceholder() && !g.isWildcard()) {
            // this is a type name used in a parameterized type
            return type(g.getType());
        }

        JavaType.GenericTypeVariable.Variance variance = INVARIANT;

        JavaType.GenericTypeVariable gtv = new JavaType.GenericTypeVariable(null, g.getName(), variance, null);
        typeCache.put(signature, gtv);

        List<JavaType> bounds = null;

        if (g.getUpperBounds() != null) {
            for (ClassNode bound : g.getUpperBounds()) {
                JavaType.FullyQualified mappedBound = TypeUtils.asFullyQualified(type(bound));
                if (mappedBound != null && !"java.lang.Object".equals(mappedBound.getFullyQualifiedName())) {
                    if (bounds == null) {
                        bounds = new ArrayList<>(g.getUpperBounds().length);
                    }
                    bounds.add(mappedBound);
                    variance = COVARIANT;
                }
            }
        } else if (g.getLowerBound() != null) {
            JavaType.FullyQualified mappedBound = TypeUtils.asFullyQualified(type(g.getLowerBound()));
            if (mappedBound != null && !"java.lang.Object".equals(mappedBound.getFullyQualifiedName())) {
                bounds = singletonList(mappedBound);
                variance = CONTRAVARIANT;
            }
        }

        gtv.unsafeSet(gtv.getName(), variance, bounds);
        return gtv;
    }

    public JavaType.@Nullable Method methodType(@Nullable MethodNode node) {
        if (node == null) {
            return null;
        }

        String signature = signatureBuilder.methodSignature(node);
        JavaType.Method existing = typeCache.get(signature);
        if (existing != null) {
            return existing;
        }

        List<String> paramNames = null;
        if (node.getParameters().length > 0) {
            paramNames = new ArrayList<>(node.getParameters().length);
            for (org.codehaus.groovy.ast.Parameter parameter : node.getParameters()) {
                paramNames.add(parameter.getName());
            }
        }

        JavaType.Method method = new JavaType.Method(
                null,
                node.getModifiers(),
                null,
                node instanceof ConstructorNode ? "<constructor>" : node.getName(),
                null,
                paramNames,
                null, null, null,
                null
        );
        typeCache.put(signature, method);

        List<JavaType> parameterTypes = null;
        if (node.getParameters().length > 0) {
            parameterTypes = new ArrayList<>(node.getParameters().length);
            for (org.codehaus.groovy.ast.Parameter parameter : node.getParameters()) {
                parameterTypes.add(type(parameter.getOriginType()));
            }
        }

        List<JavaType> thrownExceptions = null;
        if(node.getExceptions() != null) {
            for (ClassNode e : node.getExceptions()) {
                thrownExceptions = new ArrayList<>(node.getExceptions().length);
                JavaType.FullyQualified qualified = TypeUtils.asFullyQualified(type(e));
                thrownExceptions.add(qualified);
            }
        }

        List<JavaType.FullyQualified> annotations = getAnnotations(node);

        method.unsafeSet(
                TypeUtils.asFullyQualified(type(node.getDeclaringClass())),
                type(node.getReturnType()),
                parameterTypes,
                thrownExceptions,
                annotations
        );

        return method;
    }

    public JavaType.@Nullable Variable variableType(@Nullable FieldNode node) {
        if (node == null) {
            return null;
        }

        String signature = signatureBuilder.variableSignature(node);
        JavaType.Variable existing = typeCache.get(signature);
        if (existing != null) {
            return existing;
        }

        JavaType.Variable variable = new JavaType.Variable(
                null,
                node.getModifiers(),
                node.getName(),
                null, null, null);

        typeCache.put(signature, variable);

        List<JavaType.FullyQualified> annotations = getAnnotations(node);

        variable.unsafeSet(type(node.getOwner()), type(node.getType()), annotations);

        return variable;
    }

    /**
     * With an undefined owner
     */
    public JavaType.@Nullable Variable variableType(String name, @Nullable ASTNode type) {
        return variableType(name, type(type));
    }

    public JavaType.@Nullable Variable variableType(String name, @Nullable JavaType type) {
        if (type == null) {
            return null;
        }

        String signature = signatureBuilder.variableSignature(name);
        JavaType.Variable existing = typeCache.get(signature);
        if (existing != null) {
            return existing;
        }

        JavaType.Variable variable = new JavaType.Variable(
                null,
                0,
                name,
                null, null, null);

        typeCache.put(signature, variable);

        variable.unsafeSet(JavaType.Unknown.getInstance(), type, (JavaType.FullyQualified[]) null);

        return variable;
    }

    private @Nullable List<JavaType.FullyQualified> getAnnotations(AnnotatedNode node) {
        List<JavaType.FullyQualified> annotations = null;
        for (AnnotationNode a : node.getAnnotations()) {
            annotations = new ArrayList<>(node.getAnnotations().size());
            JavaType.FullyQualified fullyQualified = (JavaType.FullyQualified) type(a.getClassNode());
            annotations.add(fullyQualified);
        }
        return annotations;
    }
}
