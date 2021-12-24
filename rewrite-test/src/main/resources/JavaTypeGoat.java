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

// Whenever this class is changed, make a corresponding change in JavaTypeGoat in the main java source set.
public interface JavaTypeGoat<T, S extends JavaTypeGoat<S, ? extends T> & C> {
    PT<String> parameterizedField = new PT<String>() {
    };

    void clazz(C n);
    void primitive(int n);
    void array(C[][] n);
    void parameterized(PT<C> n);
    void parameterizedRecursive(PT<PT<C>> n);
    void generic(PT<? extends C> n);
    void genericContravariant(PT<? super C> n);
    <U extends JavaTypeGoat<U, ?>> void genericRecursive(JavaTypeGoat<? extends U[], ?> n);
    <U> void genericUnbounded(PT<U> n);
    void genericArray(PT<C>[] n);
    void inner(C.Inner n);
}

interface C {
    class Inner {
    }
}

interface PT<T> {
}
