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
package org.openrewrite.java

import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

// Whenever this class is changed, make a corresponding change in JavaTypeGoat in the main java source set.
@AnnotationWithRuntimeRetention
@AnnotationWithSourceRetention
abstract class JavaTypeGoat<T, S extends PT<S> & C> {

    public static final PT<String> parameterizedField = new PT<String>() {
    }

    static abstract class InheritedJavaTypeGoat<T, U extends PT<U> & C> extends JavaTypeGoat<T, U> {
        InheritedJavaTypeGoat() {
            super()
        }
    }

    enum EnumTypeA {
    }

    enum EnumTypeB {
        FOO("bar")
        private String label
        EnumTypeB(String label) {
            this.label = label
        }
    }

    abstract class ExtendsJavaTypeGoat extends JavaTypeGoat<T, S> {
    }

    static abstract class Extension<U extends Extension<U>> {}

    static class TypeA {}
    static class TypeB {}

    @AnnotationWithRuntimeRetention
    @AnnotationWithSourceRetention
    abstract void clazz(C n);
    abstract void primitive(int n);
    abstract void array(C[][] n);
    abstract PT<C> parameterized(PT<C> n);
    abstract PT<PT<C>> parameterizedRecursive(PT<PT<C>> n);
    abstract PT<? extends C> generic(PT<? extends C> n);
    abstract PT<? super C> genericContravariant(PT<? super C> n);
    abstract <U extends JavaTypeGoat<U, ?>> JavaTypeGoat<? extends U[], ?> genericRecursive(JavaTypeGoat<? extends U[], ?> n);
    abstract <U> PT<U> genericUnbounded(PT<U> n);
    abstract void genericArray(PT<C>[] n);
    abstract void inner(C.Inner n);
    abstract void enumTypeA(EnumTypeA n);
    abstract void enumTypeB(EnumTypeB n);
    abstract <U extends PT<U> & C> InheritedJavaTypeGoat<T, U> inheritedJavaTypeGoat(InheritedJavaTypeGoat<T, U> n);
    abstract <U extends TypeA & PT<U> & C> U genericIntersection(U n);
    abstract T genericT(T n); // remove after signatures are common.
    abstract <U extends Extension<U> & Intersection<U>> void recursiveIntersection(U n);
}

interface C {
    class Inner {
    }
}

interface PT<T> {
}

interface Intersection<T extends JavaTypeGoat.Extension<T> & Intersection<T>> {
    T getIntersectionType();
}

@Retention(RetentionPolicy.SOURCE)
@interface AnnotationWithSourceRetention {}

@Retention(RetentionPolicy.RUNTIME)
@interface AnnotationWithRuntimeRetention {}