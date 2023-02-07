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
package org.openrewrite.java;

import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.tree.JavaType;

public class UnsafeJavaTypeVisitor<P> extends JavaTypeVisitor<P> {

    @Override
    public JavaType visitClass(JavaType.Class aClass, P p) {
        return aClass.unsafeSet(
                ListUtils.map(aClass.getTypeParameters(), t -> visit(t, p)),
                (JavaType.FullyQualified) visit(aClass.getSupertype(), p),
                (JavaType.FullyQualified) visit(aClass.getOwningClass(), p),
                ListUtils.map(aClass.getAnnotations(), a -> (JavaType.FullyQualified) visit(a, p)),
                ListUtils.map(aClass.getInterfaces(), i -> (JavaType.FullyQualified) visit(i, p)),
                ListUtils.map(aClass.getMembers(), m -> (JavaType.Variable) visit(m, p)),
                ListUtils.map(aClass.getMethods(), m -> (JavaType.Method) visit(m, p))
        );
    }

    @Override
    public JavaType visitArray(JavaType.Array array, P p) {
        return array.unsafeSet(visit(array.getElemType(), p));
    }

    @Override
    public JavaType visitParameterized(JavaType.Parameterized parameterized, P p) {
        return parameterized.unsafeSet(
                (JavaType.FullyQualified) visit(parameterized.getType(), p),
                ListUtils.map(parameterized.getTypeParameters(), t -> visit(t, p))
        );
    }

    @Override
    public JavaType visitGenericTypeVariable(JavaType.GenericTypeVariable generic, P p) {
        return generic.unsafeSet(
                generic.getName(),
                generic.getVariance(),
                ListUtils.map(generic.getBounds(), b -> visit(b, p))
        );
    }

    @Override
    public JavaType visitMethod(JavaType.Method method, P p) {
        return method.unsafeSet(
                (JavaType.FullyQualified) visit(method.getDeclaringType(), p),
                visit(method.getReturnType(), p),
                ListUtils.map(method.getParameterTypes(), pt -> visit(pt, p)),
                ListUtils.map(method.getThrownExceptions(), t -> (JavaType.FullyQualified) visit(t, p)),
                ListUtils.map(method.getAnnotations(), a -> (JavaType.FullyQualified) visit(a, p))
        );
    }

    @Override
    public JavaType visitVariable(JavaType.Variable variable, P p) {
        return variable.unsafeSet(
                visit(variable.getOwner(), p),
                visit(variable.getType(), p),
                ListUtils.map(variable.getAnnotations(), a -> (JavaType.FullyQualified) visit(a, p))
        );
    }

    @Override
    public JavaType visitMultiCatch(JavaType.MultiCatch multiCatch, P p) {
        return multiCatch.unsafeSet(ListUtils.map(multiCatch.getThrowableTypes(), t -> visit(t, p)));
    }
}
