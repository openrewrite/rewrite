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
package org.openrewrite.android.internal;

import lombok.Value;
import org.jspecify.annotations.Nullable;

/**
 * Classification of a site that supplies a value to an Android SDK assignment
 * (e.g. {@code compileSdk = 34}, {@code compileSdkVersion 'android-34'}).
 * <p>
 * Produced by {@link SdkVersionValueSourceResolver} during the scan phase of an
 * SDK upgrade recipe. The visit phase uses {@link Kind} to pick the right edit
 * (numeric literal vs string-form literal vs extra-property declaration vs
 * version-catalog TOML entry vs gradle.properties entry).
 */
@Value
public class SdkVersionValueSource {

    /**
     * The category of value source. The visit phase branches on this when
     * deciding what file to edit and what shape the edit takes.
     */
    public enum Kind {
        /** Inline integer literal: {@code compileSdk = 34}. Edit the literal. */
        LITERAL_INT,

        /** Inline string with embedded number: {@code compileSdkVersion 'android-34'}. Edit the number inside the string. */
        LITERAL_STRING,

        /**
         * Reference to an extra-property variable declared in the same build script
         * (Groovy DSL {@code ext.compileSdkVersion = 34} or
         * Kotlin DSL {@code val compileSdkVersion by extra(34)}).
         * Edit the extra-property declaration.
         */
        EXTRA_PROPERTY,

        /**
         * Reference to a version catalog entry
         * ({@code libs.versions.compileSdk.get().toInt()}).
         * Edit {@code gradle/libs.versions.toml}.
         */
        VERSION_CATALOG,

        /**
         * Reference to a {@code gradle.properties} entry
         * ({@code providers.gradleProperty("compileSdk").get().toInt()}).
         * Edit {@code gradle.properties}.
         */
        GRADLE_PROPERTIES,

        /**
         * Source pattern recognized as an SDK assignment, but the value source
         * could not be resolved (unknown property, dynamic expression, etc.).
         * The recipe must report this rather than silently no-op.
         */
        UNRESOLVED
    }

    Kind kind;

    /**
     * The current value as an integer, when statically resolvable. May be null
     * for {@link Kind#UNRESOLVED} or for forms whose value lives in another
     * file (catalog / properties) where the scan phase cannot resolve it
     * synchronously.
     */
    @Nullable
    Integer currentValue;

    /**
     * For {@link Kind#LITERAL_STRING}: the surrounding string template
     * (e.g. {@code "android-"}) used to reconstruct the new literal.
     * For {@link Kind#EXTRA_PROPERTY}: the simple property name to find at the
     * declaration site.
     * For {@link Kind#VERSION_CATALOG}: the catalog version key
     * (e.g. {@code compileSdk} for {@code libs.versions.compileSdk}).
     * For {@link Kind#GRADLE_PROPERTIES}: the gradle property key.
     * Null for {@link Kind#LITERAL_INT} and {@link Kind#UNRESOLVED}.
     */
    @Nullable
    String detail;

    /**
     * Human-readable summary of the source position when {@link Kind#UNRESOLVED},
     * used in the warning markup so users can locate the site that needs manual
     * attention. Null for all other kinds.
     */
    @Nullable
    String sourceDescription;

    public static SdkVersionValueSource literalInt(int currentValue) {
        return new SdkVersionValueSource(Kind.LITERAL_INT, currentValue, null, null);
    }

    public static SdkVersionValueSource literalString(int currentValue, String prefix) {
        return new SdkVersionValueSource(Kind.LITERAL_STRING, currentValue, prefix, null);
    }

    public static SdkVersionValueSource extraProperty(String propertyName) {
        return new SdkVersionValueSource(Kind.EXTRA_PROPERTY, null, propertyName, null);
    }

    public static SdkVersionValueSource versionCatalog(String catalogKey) {
        return new SdkVersionValueSource(Kind.VERSION_CATALOG, null, catalogKey, null);
    }

    public static SdkVersionValueSource gradleProperty(String propertyKey) {
        return new SdkVersionValueSource(Kind.GRADLE_PROPERTIES, null, propertyKey, null);
    }

    public static SdkVersionValueSource unresolved(String sourceDescription) {
        return new SdkVersionValueSource(Kind.UNRESOLVED, null, null, sourceDescription);
    }
}
