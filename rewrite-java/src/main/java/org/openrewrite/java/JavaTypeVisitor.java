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

import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.tree.JavaType;

import java.util.List;

import static org.openrewrite.internal.ListUtils.arrayOrNullIfEmpty;

@Getter
@Setter
public class JavaTypeVisitor<P> {
    private Cursor cursor = new Cursor(null, "root");

    public @Nullable <JT extends JavaType> List<JT> visit(@Nullable List<JT> javaTypes, P p) {
        //noinspection unchecked
        return ListUtils.map(javaTypes, jt -> (JT) visit(jt, p));
    }

    @SuppressWarnings("unused")
    public @Nullable JavaType preVisit(JavaType javaType, P p) {
        return javaType;
    }

    @SuppressWarnings("unused")
    public @Nullable JavaType postVisit(JavaType javaType, P p) {
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
            } else if (javaType instanceof JavaType.Annotation) {
                javaType = visitAnnotation((JavaType.Annotation) javaType, p);
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
            } else if (javaType instanceof JavaType.Unknown) {
                javaType = visitUnknown((JavaType.Unknown) javaType, p);
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
        multiCatch.unsafeSet(
                visit(multiCatch.getThrowableTypes(), p)
        );
        return multiCatch;
    }

    public JavaType visitAnnotation(JavaType.Annotation annotation, P p) {
        annotation.unsafeSet(
                (JavaType.FullyQualified) visit(annotation.getType(), p),
                arrayOrNullIfEmpty(annotation.getValues(), JavaType.EMPTY_ANNOTATION_VALUE_ARRAY)
        );
        return annotation;
    }

    public JavaType visitArray(JavaType.Array array, P p) {
        array.unsafeSet(
                visit(array.getElemType(), p),
                arrayOrNullIfEmpty(visit(array.getAnnotations(), p), JavaType.EMPTY_FULLY_QUALIFIED_ARRAY)
        );
        return array;
    }

    public JavaType visitClass(JavaType.Class aClass, P p) {
        aClass.unsafeSet(
                visit(aClass.getTypeParameters(), p),
                (JavaType.FullyQualified) visit(aClass.getSupertype(), p),
                (JavaType.FullyQualified) visit(aClass.getOwningClass(), p),
                ListUtils.map(aClass.getAnnotations(), a -> (JavaType.FullyQualified) visit(a, p)),
                ListUtils.map(aClass.getInterfaces(), i -> (JavaType.FullyQualified) visit(i, p)),
                ListUtils.map(aClass.getMembers(), m -> (JavaType.Variable) visit(m, p)),
                ListUtils.map(aClass.getMethods(), m -> (JavaType.Method) visit(m, p))
        );
        return aClass;
    }

    public JavaType visitGenericTypeVariable(JavaType.GenericTypeVariable generic, P p) {
        generic.unsafeSet(
                generic.getName(),
                generic.getVariance(),
                visit(generic.getBounds(), p)
        );
        return generic;
    }

    public JavaType visitIntersection(JavaType.Intersection intersection, P p) {
        intersection.unsafeSet(
                visit(intersection.getBounds(), p)
        );
        return intersection;
    }

    /**
     * This does not visit the declaring type to avoid a visitor cycle.
     *
     * @param method The method to visit
     * @param p      Visit context
     * @return A method
     */
    public JavaType visitMethod(JavaType.Method method, P p) {
        method.unsafeSet(
                (JavaType.FullyQualified) visit(method.getDeclaringType(), p),
                visit(method.getReturnType(), p),
                visit(method.getParameterTypes(), p),
                visit(method.getThrownExceptions(), p),
                ListUtils.map(method.getAnnotations(), a -> (JavaType.FullyQualified) visit(a, p))
        );
        return method;
    }

    public JavaType visitParameterized(JavaType.Parameterized parameterized, P p) {
        parameterized.unsafeSet(
                (JavaType.FullyQualified) visit(parameterized.getType(), p),
                visit(parameterized.getTypeParameters(), p)
        );
        return parameterized;
    }

    public JavaType visitPrimitive(JavaType.Primitive primitive, P p) {
        return primitive;
    }

    public JavaType visitUnknown(JavaType.Unknown unknown, P p) {
        return unknown;
    }

    /**
     * This does not visit the owner to avoid a visitor cycle.
     *
     * @param variable The variable to visit
     * @param p        Visit context
     * @return A variable
     */
    public JavaType visitVariable(JavaType.Variable variable, P p) {
        variable.unsafeSet(
                visit(variable.getOwner(), p),
                visit(variable.getType(), p),
                ListUtils.map(variable.getAnnotations(), a -> (JavaType.FullyQualified) visit(a, p))
        );
        return variable;
    }
}
