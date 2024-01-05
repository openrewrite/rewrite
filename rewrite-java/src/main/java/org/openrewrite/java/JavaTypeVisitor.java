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

import org.openrewrite.Cursor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.JavaType;

import java.util.List;

public class JavaTypeVisitor<P> {
    private Cursor cursor = new Cursor(null, "root");

    public Cursor getCursor() {
        return cursor;
    }

    public void setCursor(Cursor cursor) {
        this.cursor = cursor;
    }

    public @Nullable <JT extends JavaType> List<JT> visit(@Nullable List<JT> javaTypes, P p) {
        //noinspection unchecked
        return ListUtils.map(javaTypes, jt -> (JT) visit(jt, p));
    }

    @Nullable
    public JavaType preVisit(JavaType javaType, P p) {
        return javaType;
    }

    @Nullable
    public JavaType postVisit(JavaType javaType, P p) {
        return javaType;
    }

    /**
     * By calling this method, you are asserting that you know that the outcome will be non-null
     * when the compiler couldn't otherwise prove this to be the case. This method is a shortcut
     * for having to assert the non-nullability of the returned tree.
     *
     * @param javaType A non-null type.
     * @param p        A state object that passes through the visitor.
     * @return A non-null type.
     */
    public JavaType visitNonNull(JavaType javaType, P p) {
        JavaType t = visit(javaType, p);
        //noinspection ConstantConditions
        assert t != null;
        return t;
    }

    public JavaType visit(@Nullable JavaType javaType, P p) {
        if (javaType != null) {
            cursor = new Cursor(cursor, javaType);
            javaType = preVisit(javaType, p);

            if (javaType instanceof JavaType.Array) {
                javaType = visitArray((JavaType.Array) javaType, p);
            } else if (javaType instanceof JavaType.Class) {
                javaType = visitClass((JavaType.Class) javaType, p);
            } else if (javaType instanceof JavaType.GenericTypeVariable) {
                javaType = visitGenericTypeVariable((JavaType.GenericTypeVariable) javaType, p);
            } else if (javaType instanceof JavaType.Intersection) {
                javaType = visitIntersection((JavaType.Intersection) javaType, p);
            } else if (javaType instanceof JavaType.MultiCatch) {
                javaType = visitMultiCatch((JavaType.MultiCatch) javaType, p);
            } else if (javaType instanceof JavaType.Parameterized) {
                javaType = visitParameterized((JavaType.Parameterized) javaType, p);
            } else if (javaType instanceof JavaType.Primitive) {
                javaType = visitPrimitive((JavaType.Primitive) javaType, p);
            } else if (javaType instanceof JavaType.Method) {
                javaType = visitMethod((JavaType.Method) javaType, p);
            } else if (javaType instanceof JavaType.Variable) {
                javaType = visitVariable((JavaType.Variable) javaType, p);
            }

            if (javaType != null) {
                javaType = postVisit(javaType, p);
            }

            cursor = cursor.getParentOrThrow();

            //noinspection ConstantConditions
            return javaType;
        }

        //noinspection ConstantConditions
        return null;
    }

    public JavaType visitMultiCatch(JavaType.MultiCatch multiCatch, P p) {
        return multiCatch.withThrowableTypes(ListUtils.map(multiCatch.getThrowableTypes(), tt -> visit(tt, p)));
    }

    public JavaType visitArray(JavaType.Array array, P p) {
        JavaType.Array a = array;
        a = a.withElemType(visit(a.getElemType(), p));
        a = a.withAnnotations(visit(a.getAnnotations(), p));
        return a;
    }

    public JavaType visitClass(JavaType.Class aClass, P p) {
        JavaType.Class c = aClass;
        c = c.withSupertype((JavaType.FullyQualified) visit(c.getSupertype(), p));
        c = c.withOwningClass((JavaType.FullyQualified) visit(c.getOwningClass(), p));
        c = c.withAnnotations(ListUtils.map(c.getAnnotations(), a -> (JavaType.FullyQualified) visit(a, p)));
        c = c.withInterfaces(ListUtils.map(c.getInterfaces(), i -> (JavaType.FullyQualified) visit(i, p)));
        c = c.withMembers(ListUtils.map(c.getMembers(), m -> (JavaType.Variable) visit(m, p)));
        c = c.withMethods(ListUtils.map(c.getMethods(), m -> (JavaType.Method) visit(m, p)));
        c = c.withTypeParameters(ListUtils.map(c.getTypeParameters(), t -> visit(t, p)));
        return c;
    }

    public JavaType visitGenericTypeVariable(JavaType.GenericTypeVariable generic, P p) {
        JavaType.GenericTypeVariable g = generic;
        g = g.withBounds(ListUtils.map(g.getBounds(), bound -> visit(bound, p)));
        return g;
    }

    public JavaType visitIntersection(JavaType.Intersection intersection, P p) {
        JavaType.Intersection i = intersection;
        i = i.withBounds(ListUtils.map(i.getBounds(), bound -> visit(bound, p)));
        return i;
    }

    /**
     * This does not visit the declaring type to avoid a visitor cycle.
     *
     * @param method The method to visit
     * @param p      Visit context
     * @return A method
     */
    public JavaType visitMethod(JavaType.Method method, P p) {
        JavaType.Method m = method;
        m = m.withDeclaringType((JavaType.FullyQualified) visit(m.getDeclaringType(), p));
        m = m.withReturnType(visit(m.getReturnType(), p));
        m = m.withParameterTypes(ListUtils.map(m.getParameterTypes(), pt -> visit(pt, p)));
        m = m.withThrownExceptions(ListUtils.map(m.getThrownExceptions(), t -> (JavaType.FullyQualified) visit(t, p)));
        m = m.withAnnotations(ListUtils.map(m.getAnnotations(), a -> (JavaType.FullyQualified) visit(a, p)));
        return m;
    }

    public JavaType visitParameterized(JavaType.Parameterized parameterized, P p) {
        JavaType.Parameterized pa = parameterized;
        pa = pa.withType((JavaType.FullyQualified) visit(pa.getType(), p));
        pa = pa.withTypeParameters(ListUtils.map(pa.getTypeParameters(), t -> visit(t, p)));
        return pa;
    }

    public JavaType visitPrimitive(JavaType.Primitive primitive, P p) {
        return primitive;
    }

    /**
     * This does not visit the owner to avoid a visitor cycle.
     *
     * @param variable The variable to visit
     * @param p        Visit context
     * @return A variable
     */
    public JavaType visitVariable(JavaType.Variable variable, P p) {
        JavaType.Variable v = variable;
        v = v.withOwner(visit(v.getOwner(), p));
        v = v.withType(visit(variable.getType(), p));
        v = v.withAnnotations(ListUtils.map(v.getAnnotations(), a -> (JavaType.FullyQualified) visit(a, p)));
        return v;
    }
}
