/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.java.internal;

import org.openrewrite.Incubating;
import org.openrewrite.java.JavaTypeSignatureBuilder;
import org.openrewrite.java.tree.JavaType;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Factory for constructing {@link JavaType} instances during parsing.
 * <p>
 * Two method families correspond to two construction shapes:
 * <ol>
 *   <li><b>{@code compute*} &mdash; stub-then-initialize.</b> The factory
 *       constructs a stub, registers it in the cache, then runs a
 *       {@link Consumer} that populates the stub via {@code unsafeSet}.
 *       Registering before the initializer runs is what makes recursive
 *       resolution during the initializer safe &mdash; reserved for types
 *       whose construction graph can recurse back to the same key:
 *       <ul>
 *         <li>{@link #computeClass} &mdash; Java's class graph is mutually
 *             recursive ({@code Foo extends Bar; Bar.field is Foo}).</li>
 *         <li>{@link #computeParameterized} &mdash; F-bounded polymorphism
 *             ({@code Comparable<T extends Comparable<T>>}) and Kotlin
 *             parameterized self-refs.</li>
 *         <li>{@link #computeGenericTypeVariable} &mdash; type variable
 *             bounds can reference the variable itself
 *             ({@code <T extends Comparable<T>>}).</li>
 *       </ul></li>
 *   <li><b>{@code *For} &mdash; atomic build.</b> The caller supplies a
 *       {@link Supplier} that returns a fully-constructed instance; the
 *       factory caches and returns it. No half-built object is ever
 *       observable through the cache. Used for variants without self-recursion
 *       on the same key: {@link #methodFor}, {@link #variableFor},
 *       {@link #arrayFor}, {@link #intersectionFor}.</li>
 * </ol>
 * <p>
 * Parser code that needs a canonical identity-stable {@link JavaType.Class}
 * for an FQN that has no Symbol/AST node (synthesized JVM facades, library
 * placeholders, etc.) should call {@link #computeClass} with an empty
 * initializer. There is no separate "lookup" surface &mdash; the absence of
 * richer attribution is signaled at the call site by an empty initializer.
 *
 * <h2>Key formats</h2>
 *
 * Two distinct keying schemes coexist; mixing them within one factory lifetime
 * is a caller bug.
 * <ul>
 *   <li><b>FQN keys</b> &mdash; {@link #computeClass} is keyed on the
 *       fully-qualified name (e.g. {@code java.util.List},
 *       {@code com.acme.Outer$Inner} for nested types). Universal across
 *       languages: every parser maps a {@link JavaType.Class} to its FQN the
 *       same way. The {@code fqn} value is exactly what
 *       {@link JavaType.FullyQualified#getFullyQualifiedName()} returns.</li>
 *   <li><b>Signature keys</b> &mdash; every other {@code compute*} and
 *       {@code *For} method accepts an opaque {@code signature} string. The
 *       signature must uniquely identify the type within a single factory
 *       lifetime, and callers must obtain it from a
 *       {@link JavaTypeSignatureBuilder} consistent with the rest of the
 *       parser session. Different parsers produce different signature shapes
 *       (e.g. {@link JvmsTypeSignatureBuilder} for javac-driven Java parsing
 *       vs. per-language builders for Groovy/Kotlin/Scala/reflection); the
 *       factory does not inspect the format, but signatures from different
 *       builders are NOT interchangeable across one parser session's cache.
 *       The canonical reference shapes are documented on
 *       {@link JavaTypeSignatureBuilder}: e.g. methods
 *       {@code com.MyThing{name=add,return=void,parameters=[Integer]}},
 *       variables {@code com.MyThing{name=MY_FIELD}}, generics
 *       {@code Generic{U extends java.lang.Comparable}}, parameterizeds
 *       {@code java.util.List<java.util.List<Integer>>}, arrays
 *       {@code Integer[]}.</li>
 * </ul>
 */
@Incubating(since = "8.82.0")
public interface JavaTypeFactory {

    // ---------------------------------------------------------------------
    // Stub-then-initialize ({@code compute*})
    //
    // For types whose construction graph can recurse back to the same key.
    // The factory registers a stub in the cache BEFORE the initializer runs
    // so recursive lookups during initialization see the partial instance
    // instead of looping. The initializer typically ends by calling
    // {@code unsafeSet} to populate the stub's remaining fields.
    // ---------------------------------------------------------------------

    /**
     * Cache-or-build for a fully-attributed {@link JavaType.Class}.
     * <p>
     * If a Class is already cached for {@code fqn}, the cached instance is
     * returned and {@code initializer} does not run. Otherwise a stub is
     * constructed with {@code flags} and {@code kind} populated, registered in
     * the cache, and the initializer runs to populate the remaining fields
     * (typically via {@link JavaType.Class#unsafeSet}).
     *
     * @param fqn the fully-qualified name of the class, in the form
     *            {@link JavaType.FullyQualified#getFullyQualifiedName()} returns.
     */
    JavaType.Class computeClass(String fqn, long flags,
                                JavaType.FullyQualified.Kind kind,
                                Consumer<JavaType.Class> initializer);

    /**
     * Cache-or-build for {@link JavaType.Parameterized}. The initializer's
     * recursive {@code type(...)} calls on type arguments can re-encounter the
     * same signature (e.g. F-bounded polymorphism, Kotlin parameterized
     * self-refs). The pre-registered stub lets those recursive lookups
     * observe a usable partially-populated instance rather than re-entering
     * the builder and looping. The initializer typically seeds the stub with
     * its raw class via {@link JavaType.Parameterized#unsafeSet} before
     * resolving type arguments.
     *
     * @param signature an opaque per-factory key from
     *                  {@link JavaTypeSignatureBuilder#parameterizedSignature}
     *                  &mdash; canonical shape
     *                  {@code java.util.List<java.util.List<Integer>>}.
     */
    JavaType.Parameterized computeParameterized(String signature,
                                                Consumer<JavaType.Parameterized> initializer);

    /**
     * Cache-or-build for {@link JavaType.GenericTypeVariable}. Type variable
     * bounds can reference the variable itself &mdash; e.g.
     * {@code <T extends Comparable<T>>} where {@code T}'s upper bound is a
     * {@link JavaType.Parameterized} that references {@code T} itself.
     * Pre-registering the stub before the initializer runs makes the
     * recursive lookup find the stub instead of looping.
     *
     * @param signature an opaque per-factory key from
     *                  {@link JavaTypeSignatureBuilder#genericSignature}
     *                  &mdash; canonical shape
     *                  {@code Generic{U extends java.lang.Comparable}}
     *                  (covariant) or {@code Generic{U super java.lang.Comparable}}
     *                  (contravariant).
     */
    JavaType.GenericTypeVariable computeGenericTypeVariable(
            String signature, String name,
            JavaType.GenericTypeVariable.Variance variance,
            Consumer<JavaType.GenericTypeVariable> initializer);

    // ---------------------------------------------------------------------
    // Atomic build ({@code *For})
    //
    // For types without self-recursion on the same key. The supplier
    // constructs the instance fully before the factory caches it; no
    // half-built object is ever observable through the cache.
    // ---------------------------------------------------------------------

    /**
     * Atomic build for {@link JavaType.Method}.
     *
     * @param signature an opaque per-factory key from a {@link JavaTypeSignatureBuilder}
     *                  &mdash; canonical shape
     *                  {@code com.MyThing{name=add,return=void,parameters=[Integer]}}.
     */
    JavaType.Method methodFor(String signature, Supplier<JavaType.Method> builder);

    /**
     * Atomic build for {@link JavaType.Variable}.
     *
     * @param signature an opaque per-factory key from a {@link JavaTypeSignatureBuilder}
     *                  &mdash; canonical shape {@code com.MyThing{name=MY_FIELD}}.
     */
    JavaType.Variable variableFor(String signature, Supplier<JavaType.Variable> builder);

    /**
     * Atomic build for {@link JavaType.Array}.
     *
     * @param signature an opaque per-factory key from
     *                  {@link JavaTypeSignatureBuilder#arraySignature}
     *                  &mdash; canonical shape {@code Integer[]}.
     */
    JavaType.Array arrayFor(String signature, Supplier<JavaType.Array> builder);

    /**
     * Atomic build for {@link JavaType.Intersection}.
     *
     * @param signature an opaque per-factory key from a {@link JavaTypeSignatureBuilder}
     *                  built from the intersection's component bounds.
     */
    JavaType.Intersection intersectionFor(String signature,
                                          Supplier<JavaType.Intersection> builder);
}
