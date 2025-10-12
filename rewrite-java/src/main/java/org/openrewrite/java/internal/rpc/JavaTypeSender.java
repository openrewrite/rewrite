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

import org.openrewrite.java.JavaTypeSignatureBuilder;
import org.openrewrite.java.JavaTypeVisitor;
import org.openrewrite.java.internal.DefaultJavaTypeSignatureBuilder;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.rpc.Reference;
import org.openrewrite.rpc.RpcSendQueue;

import static org.openrewrite.rpc.Reference.asRef;

public class JavaTypeSender extends JavaTypeVisitor<RpcSendQueue> {
    private final JavaTypeSignatureBuilder sig = new DefaultJavaTypeSignatureBuilder();

    @Override
    public JavaType visitAnnotation(JavaType.Annotation annotation, RpcSendQueue q) {
        q.getAndSend(annotation, a -> asRef(a.getType()), t -> visit(Reference.<JavaType>getValueNonNull(t), q));
        // TODO examine serialization of element types when they are raw Objects
//            q.getAndSendListAsRef(annotation, JavaType.Annotation::getValues, v ->
//                    sig.signature(v.getElement()) + ":" + v.getValue(), v -> {
//                q.getAndSend(v, e -> asRef(e.getElement()));
//                if (v instanceof JavaType.Annotation.SingleElementValue) {
//                    JavaType.Annotation.SingleElementValue sev = (JavaType.Annotation.SingleElementValue) v;
//                    q.getAndSend(sev, JavaType.Annotation.SingleElementValue::getConstantValue);
//                    q.getAndSend(sev, e -> asRef(e.getReferenceValue()));
//                } else if (v instanceof JavaType.Annotation.ArrayElementValue) {
//                    JavaType.Annotation.ArrayElementValue aev = (JavaType.Annotation.ArrayElementValue) v;
//                    q.getAndSendList(aev, e -> e.getConstantValues() == null ? null : Arrays.asList(e.getConstantValues()),
//                            Object::toString, null);
//                    q.getAndSendListAsRef(aev, e -> e.getReferenceValues() == null ? null : Arrays.asList(e.getReferenceValues()),
//                            sig::signature, null);
//                }
//            });
        return annotation;
    }

    @Override
    public JavaType visitMultiCatch(JavaType.MultiCatch multiCatch, RpcSendQueue q) {
        q.getAndSendListAsRef(multiCatch, JavaType.MultiCatch::getThrowableTypes,
                sig::signature, t -> visit(t, q));
        return multiCatch;
    }

    @Override
    public JavaType visitIntersection(JavaType.Intersection intersection, RpcSendQueue q) {
        q.getAndSendListAsRef(intersection, JavaType.Intersection::getBounds,
                sig::signature, t -> visit(t, q));
        return intersection;
    }

    @Override
    public JavaType visitClass(JavaType.Class aClass, RpcSendQueue q) {
        q.getAndSend(aClass, JavaType.Class::getFlagsBitMap);
        q.getAndSend(aClass, JavaType.Class::getKind);
        q.getAndSend(aClass, JavaType.Class::getFullyQualifiedName);
        q.getAndSendListAsRef(aClass, JavaType.Class::getTypeParameters, sig::signature, t -> visit(t, q));
        q.getAndSend(aClass, c -> asRef(c.getSupertype()), t -> visit(Reference.<JavaType>getValueNonNull(t), q));
        q.getAndSend(aClass, c -> asRef(c.getOwningClass()), t -> visit(Reference.<JavaType>getValueNonNull(t), q));
        q.getAndSendListAsRef(aClass, JavaType.Class::getAnnotations, sig::signature, t -> visit(t, q));
        q.getAndSendListAsRef(aClass, JavaType.Class::getInterfaces, sig::signature, t -> visit(t, q));
        q.getAndSendListAsRef(aClass, JavaType.Class::getMembers, sig::signature, t -> visit(t, q));
        q.getAndSendListAsRef(aClass, JavaType.Class::getMethods, sig::signature, t -> visit(t, q));
        return aClass;
    }

    @Override
    public JavaType visitParameterized(JavaType.Parameterized parameterized, RpcSendQueue q) {
        q.getAndSend(parameterized, p -> asRef(p.getType()), t -> visit(Reference.<JavaType>getValueNonNull(t), q));
        q.getAndSendListAsRef(parameterized, JavaType.Parameterized::getTypeParameters, sig::signature, t -> visit(t, q));
        return parameterized;
    }

    @Override
    public JavaType visitGenericTypeVariable(JavaType.GenericTypeVariable generic, RpcSendQueue q) {
        q.getAndSend(generic, JavaType.GenericTypeVariable::getName);
        q.getAndSend(generic, JavaType.GenericTypeVariable::getVariance);
        q.getAndSendListAsRef(generic, JavaType.GenericTypeVariable::getBounds, sig::signature, t -> visit(t, q));
        return generic;
    }

    @Override
    public JavaType visitArray(JavaType.Array array, RpcSendQueue q) {
        q.getAndSend(array, a -> asRef(a.getElemType()), t -> visit(Reference.<JavaType>getValueNonNull(t), q));
        q.getAndSendListAsRef(array, JavaType.Array::getAnnotations, sig::signature, t -> visit(t, q));
        return array;
    }

    @Override
    public JavaType visitPrimitive(JavaType.Primitive primitive, RpcSendQueue q) {
        q.getAndSend(primitive, JavaType.Primitive::getKeyword);
        return primitive;
    }

    @Override
    public JavaType visitMethod(JavaType.Method method, RpcSendQueue q) {
        q.getAndSend(method, m -> asRef(m.getDeclaringType()), t -> visit(Reference.<JavaType>getValueNonNull(t), q));
        q.getAndSend(method, JavaType.Method::getName);
        q.getAndSend(method, JavaType.Method::getFlagsBitMap);
        q.getAndSend(method, m -> asRef(m.getReturnType()), t -> visit(Reference.<JavaType>getValueNonNull(t), q));
        q.getAndSendList(method, JavaType.Method::getParameterNames, String::toString, null);
        q.getAndSendListAsRef(method, JavaType.Method::getParameterTypes, sig::signature, t -> visit(t, q));
        q.getAndSendListAsRef(method, JavaType.Method::getThrownExceptions, sig::signature, t -> visit(t, q));
        q.getAndSendListAsRef(method, JavaType.Method::getAnnotations, sig::signature, t -> visit(t, q));
        q.getAndSendList(method, JavaType.Method::getDefaultValue, String::toString, null);
        q.getAndSendList(method, JavaType.Method::getDeclaredFormalTypeNames, String::toString, null);
        return method;
    }

    @Override
    public JavaType visitVariable(JavaType.Variable variable, RpcSendQueue q) {
        q.getAndSend(variable, JavaType.Variable::getName);
        q.getAndSend(variable, v -> asRef(v.getOwner()), t -> visit(Reference.<JavaType>getValueNonNull(t), q));
        q.getAndSend(variable, v -> asRef(v.getType()), t -> visit(Reference.<JavaType>getValueNonNull(t), q));
        q.getAndSendListAsRef(variable, JavaType.Variable::getAnnotations, sig::signature, t -> visit(t, q));
        return variable;
    }
}
