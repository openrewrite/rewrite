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
package org.openrewrite.kotlin.tree;

import org.jspecify.annotations.Nullable;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Kotlin-aware counterpart to {@link TypeUtils}.
 * <p>
 * {@link org.openrewrite.kotlin.KotlinTypeMapping} produces {@link JavaType} instances
 * that use JVM fully-qualified names (so {@code kotlin.Any} becomes {@code java.lang.Object},
 * {@code kotlin.String} becomes {@code java.lang.String}, and so on). That means Java-authored
 * recipes matching on {@code java.lang.Object} / {@code java.lang.String} work uniformly
 * over Kotlin sources.
 * <p>
 * Recipes written from the Kotlin author's perspective — e.g. ones that refer to
 * {@code kotlin.collections.List} or {@code kotlin.Int} — should use the methods in this
 * class so the Kotlin FQN is accepted as an alias for the JVM FQN that the type model
 * actually carries.
 */
public final class KotlinTypeUtils {

    /**
     * Kotlin → JVM FQN aliases. The keys are the Kotlin-world FQNs a recipe author may
     * reasonably type; the values are the JVM FQNs the type model actually uses.
     */
    private static final Map<String, String> KOTLIN_TO_JVM_FQN = buildKotlinToJvmFqnMap();

    /**
     * Inverse mapping: JVM → canonical Kotlin FQN. Only populated for JVM types that
     * have a natural Kotlin alias.
     */
    private static final Map<String, String> JVM_TO_KOTLIN_FQN = buildJvmToKotlinFqnMap();

    private KotlinTypeUtils() {
    }

    private static Map<String, String> buildKotlinToJvmFqnMap() {
        Map<String, String> m = new HashMap<>();
        // Core built-in classes.
        m.put("kotlin.Any", "java.lang.Object");
        m.put("kotlin.Annotation", "java.lang.annotation.Annotation");
        m.put("kotlin.CharSequence", "java.lang.CharSequence");
        m.put("kotlin.Comparable", "java.lang.Comparable");
        m.put("kotlin.Enum", "java.lang.Enum");
        m.put("kotlin.Number", "java.lang.Number");
        m.put("kotlin.String", "java.lang.String");
        m.put("kotlin.Throwable", "java.lang.Throwable");

        // Meta-annotations.
        m.put("kotlin.annotation.MustBeDocumented", "java.lang.annotation.Documented");
        m.put("kotlin.annotation.Repeatable", "java.lang.annotation.Repeatable");
        m.put("kotlin.annotation.Retention", "java.lang.annotation.Retention");
        m.put("kotlin.annotation.Target", "java.lang.annotation.Target");

        // Kotlin primitives compile to JVM primitives (non-nullable) or boxed classes
        // (nullable). The parser produces JVM primitives for non-nullable positions, so
        // a recipe asking for "kotlin.Int" should also match `int` / `java.lang.Integer`.
        m.put("kotlin.Int", "java.lang.Integer");
        m.put("kotlin.Long", "java.lang.Long");
        m.put("kotlin.Short", "java.lang.Short");
        m.put("kotlin.Byte", "java.lang.Byte");
        m.put("kotlin.Float", "java.lang.Float");
        m.put("kotlin.Double", "java.lang.Double");
        m.put("kotlin.Boolean", "java.lang.Boolean");
        m.put("kotlin.Char", "java.lang.Character");
        m.put("kotlin.Unit", "java.lang.Void");

        // Kotlin collection interfaces compile to their java.util counterparts at the JVM level.
        m.put("kotlin.collections.Collection", "java.util.Collection");
        m.put("kotlin.collections.Iterable", "java.lang.Iterable");
        m.put("kotlin.collections.Iterator", "java.util.Iterator");
        m.put("kotlin.collections.List", "java.util.List");
        m.put("kotlin.collections.ListIterator", "java.util.ListIterator");
        m.put("kotlin.collections.Map", "java.util.Map");
        m.put("kotlin.collections.Map.Entry", "java.util.Map$Entry");
        m.put("kotlin.collections.MutableCollection", "java.util.Collection");
        m.put("kotlin.collections.MutableIterable", "java.lang.Iterable");
        m.put("kotlin.collections.MutableIterator", "java.util.Iterator");
        m.put("kotlin.collections.MutableList", "java.util.List");
        m.put("kotlin.collections.MutableListIterator", "java.util.ListIterator");
        m.put("kotlin.collections.MutableMap", "java.util.Map");
        m.put("kotlin.collections.MutableMap.Entry", "java.util.Map$Entry");
        m.put("kotlin.collections.MutableSet", "java.util.Set");
        m.put("kotlin.collections.Set", "java.util.Set");
        return m;
    }

    private static Map<String, String> buildJvmToKotlinFqnMap() {
        Map<String, String> m = new HashMap<>();
        m.put("java.lang.Object", "kotlin.Any");
        m.put("java.lang.annotation.Annotation", "kotlin.Annotation");
        m.put("java.lang.CharSequence", "kotlin.CharSequence");
        m.put("java.lang.Comparable", "kotlin.Comparable");
        m.put("java.lang.Enum", "kotlin.Enum");
        m.put("java.lang.Number", "kotlin.Number");
        m.put("java.lang.String", "kotlin.String");
        m.put("java.lang.Throwable", "kotlin.Throwable");
        return m;
    }

    /**
     * If {@code fqn} is a recognised Kotlin built-in name (e.g. {@code kotlin.Any}),
     * returns the JVM FQN that the type model uses for it (e.g. {@code java.lang.Object}).
     * Otherwise returns {@code fqn} unchanged.
     */
    public static String toJvmFqn(String fqn) {
        String jvm = KOTLIN_TO_JVM_FQN.get(fqn);
        return jvm != null ? jvm : fqn;
    }

    /**
     * If {@code fqn} has a canonical Kotlin alias (e.g. {@code java.lang.Object}),
     * returns that alias (e.g. {@code kotlin.Any}). Otherwise returns {@code fqn}
     * unchanged.
     */
    public static String toKotlinFqn(String fqn) {
        String kotlin = JVM_TO_KOTLIN_FQN.get(fqn);
        return kotlin != null ? kotlin : fqn;
    }

    /**
     * {@link TypeUtils#isOfClassType(JavaType, String)} that also recognises Kotlin
     * built-in FQNs. Callers may pass either the Kotlin name or the JVM name; both
     * forms match types that carry the JVM representation. For Kotlin primitive FQNs
     * (e.g. {@code kotlin.Int}) the match also accepts the JVM primitive (e.g. {@code int}),
     * since the parser unboxes non-nullable primitive uses.
     */
    public static boolean isOfClassType(@Nullable JavaType type, String fqn) {
        if (TypeUtils.isOfClassType(type, fqn)) {
            return true;
        }
        // Kotlin primitive aliases also accept the JVM primitive form.
        if (type instanceof JavaType.Primitive) {
            JavaType.Primitive expected = kotlinFqnToPrimitive(fqn);
            if (expected != null && type == expected) {
                return true;
            }
        }
        String jvm = KOTLIN_TO_JVM_FQN.get(fqn);
        return jvm != null && TypeUtils.isOfClassType(type, jvm);
    }

    private static JavaType.@Nullable Primitive kotlinFqnToPrimitive(String fqn) {
        switch (fqn) {
            case "kotlin.Int": return JavaType.Primitive.Int;
            case "kotlin.Long": return JavaType.Primitive.Long;
            case "kotlin.Short": return JavaType.Primitive.Short;
            case "kotlin.Byte": return JavaType.Primitive.Byte;
            case "kotlin.Float": return JavaType.Primitive.Float;
            case "kotlin.Double": return JavaType.Primitive.Double;
            case "kotlin.Boolean": return JavaType.Primitive.Boolean;
            case "kotlin.Char": return JavaType.Primitive.Char;
            case "kotlin.Unit": return JavaType.Primitive.Void;
            default: return null;
        }
    }

    /**
     * {@link TypeUtils#isAssignableTo(String, JavaType)} that also recognises Kotlin
     * built-in FQNs. Callers may pass either form for {@code to}; both are treated as
     * equivalent when checking assignability against the JVM-oriented type model.
     */
    public static boolean isAssignableTo(String to, @Nullable JavaType from) {
        if (TypeUtils.isAssignableTo(to, from)) {
            return true;
        }
        String jvm = KOTLIN_TO_JVM_FQN.get(to);
        return jvm != null && TypeUtils.isAssignableTo(jvm, from);
    }

    /**
     * True when {@code type} corresponds to Kotlin's {@code Int} — either as the
     * JVM primitive {@code int} or as the boxed {@code java.lang.Integer}.
     */
    public static boolean isKotlinInt(@Nullable JavaType type) {
        return type == JavaType.Primitive.Int || TypeUtils.isOfClassType(type, "java.lang.Integer");
    }

    /**
     * True when {@code type} corresponds to Kotlin's {@code Long} — either as the
     * JVM primitive {@code long} or as the boxed {@code java.lang.Long}.
     */
    public static boolean isKotlinLong(@Nullable JavaType type) {
        return type == JavaType.Primitive.Long || TypeUtils.isOfClassType(type, "java.lang.Long");
    }

    /**
     * True when {@code type} corresponds to Kotlin's {@code Short} — either as the
     * JVM primitive {@code short} or as the boxed {@code java.lang.Short}.
     */
    public static boolean isKotlinShort(@Nullable JavaType type) {
        return type == JavaType.Primitive.Short || TypeUtils.isOfClassType(type, "java.lang.Short");
    }

    /**
     * True when {@code type} corresponds to Kotlin's {@code Byte} — either as the
     * JVM primitive {@code byte} or as the boxed {@code java.lang.Byte}.
     */
    public static boolean isKotlinByte(@Nullable JavaType type) {
        return type == JavaType.Primitive.Byte || TypeUtils.isOfClassType(type, "java.lang.Byte");
    }

    /**
     * True when {@code type} corresponds to Kotlin's {@code Float} — either as the
     * JVM primitive {@code float} or as the boxed {@code java.lang.Float}.
     */
    public static boolean isKotlinFloat(@Nullable JavaType type) {
        return type == JavaType.Primitive.Float || TypeUtils.isOfClassType(type, "java.lang.Float");
    }

    /**
     * True when {@code type} corresponds to Kotlin's {@code Double} — either as the
     * JVM primitive {@code double} or as the boxed {@code java.lang.Double}.
     */
    public static boolean isKotlinDouble(@Nullable JavaType type) {
        return type == JavaType.Primitive.Double || TypeUtils.isOfClassType(type, "java.lang.Double");
    }

    /**
     * True when {@code type} corresponds to Kotlin's {@code Boolean} — either as the
     * JVM primitive {@code boolean} or as the boxed {@code java.lang.Boolean}.
     */
    public static boolean isKotlinBoolean(@Nullable JavaType type) {
        return type == JavaType.Primitive.Boolean || TypeUtils.isOfClassType(type, "java.lang.Boolean");
    }

    /**
     * True when {@code type} corresponds to Kotlin's {@code Char} — either as the
     * JVM primitive {@code char} or as the boxed {@code java.lang.Character}.
     */
    public static boolean isKotlinChar(@Nullable JavaType type) {
        return type == JavaType.Primitive.Char || TypeUtils.isOfClassType(type, "java.lang.Character");
    }

    /**
     * True when {@code type} is Kotlin's {@code Unit} or the JVM's {@code void}.
     * The Kotlin type model keeps {@code kotlin.Unit} as a class in non-return positions;
     * in return positions Kotlin's compiler normalises to JVM {@code void}.
     */
    public static boolean isKotlinUnit(@Nullable JavaType type) {
        return type == JavaType.Primitive.Void || TypeUtils.isOfClassType(type, "kotlin.Unit");
    }

    /**
     * True when {@code type} is {@code kotlin.Any} / {@code java.lang.Object}.
     */
    public static boolean isAny(@Nullable JavaType type) {
        return TypeUtils.isObject(type);
    }

    /**
     * Render a type parameter list (including angle brackets and an optional {@code where}
     * clause for multi-bound variables) in Kotlin syntax from a collection of
     * {@link JavaType.GenericTypeVariable}. Returns an empty string when there are no
     * nameable type variables.
     */
    public static String toKotlinTypeParameters(Collection<JavaType.GenericTypeVariable> typeVariables) {
        if (typeVariables.isEmpty()) {
            return "";
        }
        StringBuilder params = new StringBuilder("<");
        StringBuilder where = new StringBuilder();
        boolean firstParam = true;
        for (JavaType.GenericTypeVariable tv : typeVariables) {
            if ("?".equals(tv.getName())) {
                continue;
            }
            if (!firstParam) {
                params.append(", ");
            }
            firstParam = false;
            params.append(tv.getName());
            List<JavaType> bounds = tv.getBounds();
            if (tv.getVariance() == JavaType.GenericTypeVariable.Variance.COVARIANT && !bounds.isEmpty()) {
                if (bounds.size() == 1) {
                    params.append(" : ").append(TypeUtils.toString(bounds.get(0)));
                } else {
                    for (JavaType bound : bounds) {
                        if (where.length() > 0) {
                            where.append(", ");
                        }
                        where.append(tv.getName()).append(" : ").append(TypeUtils.toString(bound));
                    }
                }
            }
        }
        if (firstParam) {
            return "";
        }
        params.append(">");
        if (where.length() > 0) {
            params.append(" where ").append(where);
        }
        return params.toString();
    }
}
