/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.java.internal.rpc;

import org.openrewrite.java.JavaTypeVisitor;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.rpc.RpcReceiveQueue;

import java.util.List;

import static org.openrewrite.internal.ListUtils.arrayOrNullIfEmpty;
import static org.openrewrite.java.tree.JavaType.EMPTY_FULLY_QUALIFIED_ARRAY;
import static org.openrewrite.java.tree.JavaType.EMPTY_JAVA_TYPE_ARRAY;

public class JavaTypeReceiver extends JavaTypeVisitor<RpcReceiveQueue> {
    private final String[] EMPTY_STRING_ARRAY = new String[0];

    @Override
    public JavaType visitAnnotation(JavaType.Annotation annotation, RpcReceiveQueue q) {
        JavaType.FullyQualified type = q.receive(annotation.getType(), v -> (JavaType.FullyQualified) visit(v, q));
        return annotation.unsafeSet(
                type,
                null
//                    arrayOrNullIfEmpty(q.receiveList(annotation.getValues(), v -> {
//                        if (v instanceof JavaType.Annotation.SingleElementValue) {
//                            JavaType.Annotation.SingleElementValue sev = (JavaType.Annotation.SingleElementValue) v;
//                            JavaType element = q.receive(sev.getElement(), null);
//                            Object constantValue = q.receive(sev.getConstantValue(), null);
//                            JavaType referenceValue = q.receive(sev.getReferenceValue(), null);
//                            return new JavaType.Annotation.SingleElementValue(element, constantValue, referenceValue);
//                        } else if (v instanceof JavaType.Annotation.ArrayElementValue) {
//                            JavaType.Annotation.ArrayElementValue aev = (JavaType.Annotation.ArrayElementValue) v;
//                            JavaType element = q.receive(aev.getElement(), null);
//                            List<Object> constantValues = q.receiveList(aev.getConstantValues() == null ? null : Arrays.asList(aev.getConstantValues()), null);
//                            List<JavaType> referenceValues = q.receiveList(aev.getReferenceValues() == null ? null : Arrays.asList(aev.getReferenceValues()), null);
//                            return new JavaType.Annotation.ArrayElementValue(
//                                    element,
//                                    constantValues == null ? null : constantValues.toArray(),
//                                    referenceValues == null ? null : referenceValues.toArray(EMPTY_JAVA_TYPE_ARRAY)
//                            );
//                        }
//                        return v;
//                    }), EMPTY_ANNOTATION_VALUE_ARRAY)
        );
    }

    @Override
    public JavaType visitMultiCatch(JavaType.MultiCatch multiCatch, RpcReceiveQueue q) {
        return multiCatch.withThrowableTypes(q.receiveList(multiCatch.getThrowableTypes(), v -> visit(v, q)));
    }

    @Override
    public JavaType visitIntersection(JavaType.Intersection intersection, RpcReceiveQueue q) {
        return intersection.withBounds(q.receiveList(intersection.getBounds(), v -> visit(v, q)));
    }

    @Override
    public JavaType visitClass(JavaType.Class aClass, RpcReceiveQueue q) {
        long flags = q.receive((Number) aClass.getFlagsBitMap()).longValue();
        JavaType.FullyQualified.Kind kind = q.receiveAndGet(aClass.getKind(), k -> JavaType.FullyQualified.Kind.valueOf(k.toString()));
        String fqn = q.receive(aClass.getFullyQualifiedName());
        List<JavaType> typeParameters = q.receiveList(aClass.getTypeParameters(), v -> visit(v, q));
        JavaType.FullyQualified supertype = q.receive(aClass.getSupertype(), v -> (JavaType.FullyQualified) visit(v, q));
        JavaType.FullyQualified owningClass = q.receive(aClass.getOwningClass(), v -> (JavaType.FullyQualified) visit(v, q));
        List<JavaType.FullyQualified> annotations = q.receiveList(aClass.getAnnotations(), v -> (JavaType.FullyQualified) visit(v, q));
        List<JavaType.FullyQualified> interfaces = q.receiveList(aClass.getInterfaces(), v -> (JavaType.FullyQualified) visit(v, q));
        List<JavaType.Variable> members = q.receiveList(aClass.getMembers(), v -> (JavaType.Variable) visit(v, q));
        List<JavaType.Method> methods = q.receiveList(aClass.getMethods(), v -> (JavaType.Method) visit(v, q));
        return aClass.unsafeSet(flags, kind, fqn, typeParameters, supertype, owningClass, annotations,
                interfaces, members, methods);
    }

    @Override
    public JavaType visitParameterized(JavaType.Parameterized parameterized, RpcReceiveQueue q) {
        JavaType.FullyQualified type = q.receive(parameterized.getType(), v -> (JavaType.FullyQualified) visit(v, q));
        List<JavaType> typeParameters = q.receiveList(parameterized.getTypeParameters(), v -> visit(v, q));
        return parameterized.unsafeSet(type, typeParameters);
    }

    @Override
    public JavaType visitGenericTypeVariable(JavaType.GenericTypeVariable generic, RpcReceiveQueue q) {
        String name = q.receive(generic.getName());
        JavaType.GenericTypeVariable.Variance variance = q.receiveAndGet(generic.getVariance(),
                v -> JavaType.GenericTypeVariable.Variance.valueOf(v.toString()));
        List<JavaType> bounds = q.receiveList(generic.getBounds(), v -> visit(v, q));
        return generic.unsafeSet(name, variance, bounds);
    }

    @Override
    public JavaType visitArray(JavaType.Array array, RpcReceiveQueue q) {
        JavaType elemType = q.receive(array.getElemType(), v -> visit(v, q));
        List<JavaType.FullyQualified> annotations = q.receiveList(array.getAnnotations(), v -> (JavaType.FullyQualified) visit(v, q));
        return array.unsafeSet(elemType, arrayOrNullIfEmpty(annotations, EMPTY_FULLY_QUALIFIED_ARRAY));
    }

    @Override
    public JavaType visitPrimitive(JavaType.Primitive primitive, RpcReceiveQueue q) {
        String keyword = q.receive(primitive.getKeyword());
        JavaType.Primitive p = JavaType.Primitive.fromKeyword(keyword);
        if (p == null) {
            throw new IllegalArgumentException("Unknown primitive type keyword: " + keyword);
        }
        return p;
    }

    @Override
    public JavaType visitMethod(JavaType.Method method, RpcReceiveQueue q) {
        JavaType.FullyQualified declaringType = q.receive(method.getDeclaringType(), v -> (JavaType.FullyQualified) visit(v, q));
        String name = q.receive(method.getName(), null);
        long flags = q.receive((Number) method.getFlagsBitMap()).longValue();
        JavaType returnType = q.receive(method.getReturnType(), v -> visit(v, q));
        List<String> parameterNames = q.receiveList(method.getParameterNames(), null);
        List<JavaType> parameterTypes = q.receiveList(method.getParameterTypes(), v -> visit(v, q));
        List<JavaType> thrownExceptions = q.receiveList(method.getThrownExceptions(), v -> visit(v, q));
        List<JavaType.FullyQualified> annotations = q.receiveList(method.getAnnotations(), v -> (JavaType.FullyQualified) visit(v, q));
        List<String> defaultValue = q.receiveList(method.getDefaultValue(), null);
        List<String> declaredFormalTypeNames = q.receiveList(method.getDeclaredFormalTypeNames(), null);
        method.unsafeSet(
                name, flags,
                declaringType,
                returnType,
                arrayOrNullIfEmpty(parameterNames, EMPTY_STRING_ARRAY),
                arrayOrNullIfEmpty(parameterTypes, EMPTY_JAVA_TYPE_ARRAY),
                arrayOrNullIfEmpty(thrownExceptions, EMPTY_JAVA_TYPE_ARRAY),
                arrayOrNullIfEmpty(annotations, EMPTY_FULLY_QUALIFIED_ARRAY),
                defaultValue,
                arrayOrNullIfEmpty(declaredFormalTypeNames, EMPTY_STRING_ARRAY)
        );
        return method;
    }

    @Override
    public JavaType visitVariable(JavaType.Variable variable, RpcReceiveQueue q) {
        String name = q.receive(variable.getName());
        JavaType owner = q.receive(variable.getOwner(), v -> visit(v, q));
        JavaType type = q.receive(variable.getType(), v -> visit(v, q));
        List<JavaType.FullyQualified> annotations = q.receiveList(variable.getAnnotations(), v -> (JavaType.FullyQualified) visit(v, q));
        return variable.unsafeSet(name, owner, type, arrayOrNullIfEmpty(annotations, EMPTY_FULLY_QUALIFIED_ARRAY));
    }
}
