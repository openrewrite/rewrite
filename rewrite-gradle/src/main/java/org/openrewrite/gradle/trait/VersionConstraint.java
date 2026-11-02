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
package org.openrewrite.gradle.trait;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.openrewrite.toml.TomlTableValue;
import org.openrewrite.toml.tree.Toml;

import java.util.Arrays;
import java.util.List;

/**
 * A catalog version written as a table of {@code strictly}, {@code require}, {@code prefer},
 * {@code reject} and {@code ref} rather than as a plain string, as in
 * {@code version = { require = "1.0", reject = ["1.1"] } }. Every method here also accepts the
 * plain string form, so callers need not know which one they have.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class VersionConstraint {
    private static final List<String> VERSION_KEYS = Arrays.asList("strictly", "require", "prefer");

    /**
     * The version an entry is at, {@code strictly} winning over {@code require} over {@code prefer},
     * as Gradle resolves them. A version that only refers to a declaration has none of its own.
     * {@code reject} is a list of exclusions, never the version.
     */
    static @Nullable String getVersion(Toml.Table table, String key) {
        String version = TomlTableValue.getString(table, key);
        if (version != null) {
            return version;
        }
        String versionKey = versionKey(table, key);
        return versionKey == null ? null : TomlTableValue.getString(table, key, versionKey);
    }

    /**
     * The declaration an entry refers to, written either as the dotted {@code version.ref} or as
     * {@code ref} within a version constraint.
     */
    static @Nullable String getVersionRef(Toml.Table table, String key) {
        String versionRef = TomlTableValue.getString(table, key + ".ref");
        return versionRef == null ? TomlTableValue.getString(table, key, "ref") : versionRef;
    }

    /**
     * Writes {@code newVersion} at whichever key {@link #getVersion} read it from, so that a
     * constraint keeps its shape and its {@code reject} list.
     */
    static Toml.Table withVersion(Toml.Table table, String key, String newVersion) {
        String versionKey = versionKey(table, key);
        return versionKey == null ?
                TomlTableValue.withString(table, key, newVersion) :
                TomlTableValue.withString(table, key, versionKey, newVersion);
    }

    /**
     * Writes {@code newVersion} as {@link #withVersion} does, adding a plain string version when
     * the entry has none at all.
     */
    static Toml.Table withVersionOrAdd(Toml.Table table, String key, String newVersion) {
        return versionKey(table, key) == null ?
                TomlTableValue.withStringOrAdd(table, key, newVersion) :
                withVersion(table, key, newVersion);
    }

    /**
     * Replaces a reference to a declaration with {@code newVersion} written at the entry itself,
     * a version constraint keeping its table so that its other keys survive.
     */
    static Toml.Table withDetachedVersion(Toml.Table table, String key, String newVersion) {
        if (TomlTableValue.getString(table, key, "ref") != null) {
            return TomlTableValue.withString(TomlTableValue.withKey(table, key, "ref", "require"), key, "require", newVersion);
        }
        return TomlTableValue.withString(TomlTableValue.withKey(table, key + ".ref", key), key, newVersion);
    }

    /**
     * Whichever of {@code strictly}, {@code require} or {@code prefer} carries the version.
     */
    private static @Nullable String versionKey(Toml.Table table, String key) {
        for (String versionKey : VERSION_KEYS) {
            if (TomlTableValue.getString(table, key, versionKey) != null) {
                return versionKey;
            }
        }
        return null;
    }
}
