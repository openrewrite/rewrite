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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

// Whenever this class is changed, make a corresponding change in JavaTypeGoat in the main resources folder.
@AnnotationWithRuntimeRetention
@AnnotationWithSourceRetention
public abstract class JavaTypeGoat<T, S extends PT<S> & C> {
    public JavaTypeGoat() {
    }

    public static final PT<String> parameterizedField = new PT<String>() {
    };

    public static abstract class InheritedJavaTypeGoat<T, U extends PT<U> & C> extends JavaTypeGoat<T, U> {
        public InheritedJavaTypeGoat() {
            super();
        }
    }

    @AnnotationWithRuntimeRetention
    @AnnotationWithSourceRetention
    public abstract void clazz(C n);
    public abstract void primitive(int n);
    public abstract void array(C[][] n);
    public abstract PT<C> parameterized(PT<C> n);
    public abstract PT<PT<C>> parameterizedRecursive(PT<PT<C>> n);
    public abstract PT<? extends C> generic(PT<? extends C> n);
    public abstract PT<? super C> genericContravariant(PT<? super C> n);
    public abstract <U extends JavaTypeGoat<U, ?>> JavaTypeGoat<? extends U[], ?> genericRecursive(JavaTypeGoat<? extends U[], ?> n);
    public abstract <U> PT<U> genericUnbounded(PT<U> n);
    public abstract void genericArray(PT<C>[] n);
    public abstract void inner(C.Inner n);
    public abstract <U extends PT<U> & C> void inheritedJavaTypeGoat(InheritedJavaTypeGoat<T, U> inheritedJavaTypeGoat);
    public abstract T genericT(T n); // remove after signatures are common.
}

interface C {
    class Inner {
    }
}

interface PT<T> {
}

@Retention(RetentionPolicy.SOURCE)
@interface AnnotationWithSourceRetention {}

@Retention(RetentionPolicy.RUNTIME)
@interface AnnotationWithRuntimeRetention {}
