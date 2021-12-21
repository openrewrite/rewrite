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
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaTypeMapping;
import org.openrewrite.java.internal.JavaReflectionTypeMapping;
import org.openrewrite.java.tree.Flag;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Collections.emptyList;

class GroovyTypeMapping implements JavaTypeMapping<ClassNode> {
    private final GroovyAstTypeSignatureBuilder signatureBuilder = new GroovyAstTypeSignatureBuilder();

    private final Map<String, Object> typeBySignature;
    private final JavaReflectionTypeMapping reflectionTypeMapping;

    GroovyTypeMapping(Map<String, Object> typeBySignature) {
        this.typeBySignature = typeBySignature;
        this.reflectionTypeMapping = new JavaReflectionTypeMapping(typeBySignature);
    }

    @Nullable
    public JavaType type(@Nullable ClassNode node) {
        return _type(node);
    }

    @Nullable
    private JavaType _type(@Nullable ClassNode node) {
        if (node == null) {
            return null;
        }

        if(node.isArray()) {
            return arrayType(node);
        } else if(ClassHelper.isPrimitiveType(node)) {
            return JavaType.Primitive.fromKeyword(node.getName());
        }

        JavaType.Class clazz = (JavaType.Class) typeBySignature.computeIfAbsent(signatureBuilder.signature(node.getTypeClass()), ignored -> {
            try {
                return reflectionTypeMapping.type(node.getTypeClass());
            } catch (GroovyBugError | NoClassDefFoundError ignored1) {
                return new JavaType.Class(null, Flag.Public.getBitMask(), node.getName(), JavaType.Class.Kind.Class,
                        null, null, null, null, null, null);
            }
        });

        if(node.isUsingGenerics()) {
            AtomicBoolean newlyCreated = new AtomicBoolean(false);

            JavaType.Parameterized parameterized = (JavaType.Parameterized) typeBySignature.computeIfAbsent(signatureBuilder.signature(node), ignored -> {
                newlyCreated.set(true);
                //noinspection ConstantConditions
                return new JavaType.Parameterized(null, null, null);
            });

            if (newlyCreated.get()) {
                List<JavaType> typeParameters = emptyList();
                if (node.getGenericsTypes().length > 0) {
                    typeParameters = new ArrayList<>(node.getGenericsTypes().length);
                    for (GenericsType g : node.getGenericsTypes()) {
                        typeParameters.add(genericType(g));
                    }
                }

                assert clazz != null;
                parameterized.unsafeSet(clazz, typeParameters);
            }

            return parameterized;
        }

        return clazz;
    }

    @Nullable
    public JavaType.Method type(@Nullable MethodNode node) {
        if (node == null) {
            return null;
        }

        StringJoiner argumentTypeSignatures = new StringJoiner(",");
        if (node.getParameters().length > 0) {
            for (org.codehaus.groovy.ast.Parameter parameter : node.getParameters()) {
                argumentTypeSignatures.add(parameter.getOriginType().getName());
            }
        }

        //noinspection ConstantConditions
        return (JavaType.Method) typeBySignature.computeIfAbsent(signatureBuilder.methodSignature(node), ignore -> {
            List<JavaType> parameterTypes = emptyList();
            if (node.getParameters().length > 0) {
                parameterTypes = new ArrayList<>(node.getParameters().length);
                for (org.codehaus.groovy.ast.Parameter p : node.getParameters()) {
                    JavaType paramType = _type(p.getOriginType());
                    if (paramType instanceof JavaType.Parameterized) {
                        return ((JavaType.Parameterized) paramType).getType();
                    }
                    parameterTypes.add(paramType);
                }
            }

            JavaType.Method.Signature signature = new JavaType.Method.Signature(
                    _type(node.getReturnType()),
                    parameterTypes
            );

            List<String> paramNames = null;
            if (node.getParameters().length > 0) {
                paramNames = new ArrayList<>(node.getParameters().length);
                for (org.codehaus.groovy.ast.Parameter parameter : node.getParameters()) {
                    paramNames.add(parameter.getName());
                }
            }

            List<JavaType.FullyQualified> thrownExceptions = null;
            for (ClassNode e : node.getExceptions()) {
                thrownExceptions = new ArrayList<>(node.getExceptions().length);
                JavaType.FullyQualified qualified = (JavaType.FullyQualified) _type(e);
                thrownExceptions.add(qualified);
            }

            List<JavaType.FullyQualified> annotations = null;
            for (AnnotationNode a : node.getAnnotations()) {
                annotations = new ArrayList<>(node.getAnnotations().size());
                JavaType.FullyQualified fullyQualified = (JavaType.FullyQualified) _type(a.getClassNode());
                annotations.add(fullyQualified);
            }

            //noinspection ConstantConditions
            return new JavaType.Method(
                    node.getModifiers(),
                    (JavaType.FullyQualified) _type(node.getDeclaringClass()),
                    node.getName(),
                    paramNames,
                    signature,
                    signature,
                    thrownExceptions,
                    annotations
            );
        });
    }

    @SuppressWarnings("ConstantConditions")
    private JavaType.Array arrayType(ClassNode array) {
        return (JavaType.Array) typeBySignature.computeIfAbsent(signatureBuilder.signature(array), ignored ->
                new JavaType.Array(_type(array.getComponentType())));
    }

    private JavaType genericType(GenericsType g) {
        //noinspection ConstantConditions
        return (JavaType) typeBySignature.computeIfAbsent(signatureBuilder.signature(g), ignored -> {
            if (g.getUpperBounds() != null) {
                List<JavaType> bounds = new ArrayList<>(g.getUpperBounds().length);
                for (ClassNode bound : g.getUpperBounds()) {
                    bounds.add(TypeUtils.asFullyQualified(_type(bound)));
                }

                // FIXME how to tell variance type?
                return new JavaType.GenericTypeVariable(
                        null,
                        g.getName(),
                        JavaType.GenericTypeVariable.Variance.COVARIANT,
                        bounds
                );
            }

            return _type(g.getType());
        });
    }
}
