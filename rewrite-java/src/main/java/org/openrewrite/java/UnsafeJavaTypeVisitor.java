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

import org.openrewrite.java.tree.JavaType;

import java.util.function.UnaryOperator;

import static org.openrewrite.java.tree.JavaType.*;

public class UnsafeJavaTypeVisitor<P> extends JavaTypeVisitor<P> {

    @Override
    public JavaType visitClass(JavaType.Class aClass, P p) {
        return aClass.unsafeSet(
                mapInPlace(aClass.getTypeParameters().toArray(EMPTY_JAVA_TYPE_ARRAY), t -> visit(t, p)),
                (JavaType.FullyQualified) visit(aClass.getSupertype(), p),
                (JavaType.FullyQualified) visit(aClass.getOwningClass(), p),
                mapInPlace(aClass.getAnnotations().toArray(EMPTY_FULLY_QUALIFIED_ARRAY), a -> (JavaType.FullyQualified) visit(a, p)),
                mapInPlace(aClass.getInterfaces().toArray(EMPTY_FULLY_QUALIFIED_ARRAY), i -> (JavaType.FullyQualified) visit(i, p)),
                mapInPlace(aClass.getMembers().toArray(EMPTY_VARIABLE_ARRAY), m -> (JavaType.Variable) visit(m, p)),
                mapInPlace(aClass.getMethods().toArray(EMPTY_METHOD_ARRAY), m -> (JavaType.Method) visit(m, p))
        );
    }

    @Override
    public JavaType visitArray(JavaType.Array array, P p) {
        return array.unsafeSet(visit(array.getElemType(), p), mapInPlace(array.getAnnotations().toArray(EMPTY_FULLY_QUALIFIED_ARRAY), a -> (JavaType.FullyQualified) visit(a, p)));
    }

    @Override
    public JavaType visitParameterized(JavaType.Parameterized parameterized, P p) {
        return parameterized.unsafeSet(
                (JavaType.FullyQualified) visit(parameterized.getType(), p),
                mapInPlace(parameterized.getTypeParameters().toArray(EMPTY_JAVA_TYPE_ARRAY), t -> visit(t, p))
        );
    }

    @Override
    public JavaType visitGenericTypeVariable(JavaType.GenericTypeVariable generic, P p) {
        return generic.unsafeSet(
                generic.getName(),
                generic.getVariance(),
                mapInPlace(generic.getBounds().toArray(EMPTY_JAVA_TYPE_ARRAY), b -> visit(b, p))
        );
    }

    @Override
    public JavaType visitIntersection(Intersection intersection, P p) {
        return intersection.unsafeSet(mapInPlace(intersection.getBounds().toArray(EMPTY_JAVA_TYPE_ARRAY), t -> visit(t, p)));
    }

    @Override
    public JavaType visitMethod(JavaType.Method method, P p) {
        return method.unsafeSet(
                (JavaType.FullyQualified) visit(method.getDeclaringType(), p),
                visit(method.getReturnType(), p),
                mapInPlace(method.getParameterTypes().toArray(EMPTY_JAVA_TYPE_ARRAY), pt -> visit(pt, p)),
                mapInPlace(method.getThrownExceptions().toArray(EMPTY_FULLY_QUALIFIED_ARRAY), t -> (JavaType.FullyQualified) visit(t, p)),
                mapInPlace(method.getAnnotations().toArray(EMPTY_FULLY_QUALIFIED_ARRAY), a -> (JavaType.FullyQualified) visit(a, p))
        );
    }

    @Override
    public JavaType visitMultiCatch(JavaType.MultiCatch multiCatch, P p) {
        return multiCatch.unsafeSet(mapInPlace(multiCatch.getThrowableTypes().toArray(EMPTY_JAVA_TYPE_ARRAY), t -> visit(t, p)));
    }

    @Override
    public JavaType visitVariable(JavaType.Variable variable, P p) {
        return variable.unsafeSet(
                visit(variable.getOwner(), p),
                visit(variable.getType(), p),
                mapInPlace(variable.getAnnotations().toArray(EMPTY_FULLY_QUALIFIED_ARRAY), a -> (JavaType.FullyQualified) visit(a, p))
        );
    }

    private static <T extends JavaType> T[] mapInPlace(T[] ls, UnaryOperator<T> map) {
        for (int i = 0; i < ls.length; i++) {
            ls[i] = map.apply(ls[i]);
        }
        return ls;
    }
}
