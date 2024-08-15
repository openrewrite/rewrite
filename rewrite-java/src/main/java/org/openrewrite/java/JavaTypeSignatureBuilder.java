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

import org.jspecify.annotations.Nullable;

/**
 * In addition to the signature formats described below, implementations should provide a way of retrieving method
 * and variable signatures, but they may have different numbers of input arguments depending on the implementation.
 * <p/>
 * Method signatures should be formatted like {@code com.MyThing{name=add,return=void,parameters=[Integer]}},
 * where <code>MyThing</code> is the declaring type, <code>void</code> is the return type, and <code>Integer</code> is a parameter.
 * <p/>
 * Variable signatures should be formatted like <code>com.MyThing{name=MY_FIELD}</code>.
 */
public interface JavaTypeSignatureBuilder {
    /**
     * @param type A type object.
     * @return The type signature. If <code>t</code> is null, the signature is <code>{undefined}</code>.
     */
    String signature(@Nullable Object type);
    
    /**
     * @param type An array type.
     * @return Formatted like <code>Integer[]</code>.
     */
    String arraySignature(Object type);

    /**
     * @param type A class type.
     * @return Formatted like <code>java.util.List</code>.
     */
    String classSignature(Object type);

    /**
     * When generic type variables are cyclic, like {@code U extends Cyclic<? extends U>}, represent the cycle with the
     * bound name, like {@code Generic{U extends Cyclic<? extends U>}}.
     * <p/>
     * When the bound is {@link Object} (regardless of whether
     * that bound is implicit or explicit in the source code),the type variable is considered invariant and the
     * bound is omitted. So {@code Generic{List<?>}} is favored over {@code Generic{List<? extends java.lang.Object>}}.
     *
     * @param type A generic type.
     * @return Formatted like <code>Generic{U extends java.lang.Comparable}</code> (covariant) or
     * <code>Generic{U super java.lang.Comparable}</code> (contravariant).
     */
    String genericSignature(Object type);

    /**
     * @param type A parameterized type.
     * @return Formatted like {@code java.util.List<java.util.List<Integer>>}.
     */
    String parameterizedSignature(Object type);

    /**
     * @param type A primitive type.
     * @return Formatted like <code>Integer</code>.
     */
    String primitiveSignature(Object type);
}
