/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.python.internal.pep508;

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.python.internal.pep440.PythonVersionSpecifierSet;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * A parsed PEP 508 dependency specifier, ported from pypa/packaging's
 * {@code requirements.Requirement}.
 */
@Value
public class Pep508Requirement {
    /**
     * The name as written.
     */
    String name;

    /**
     * The PEP 503 canonical form of the name.
     */
    String canonicalName;

    /**
     * Extras as written, in declaration order.
     */
    Set<String> extras;

    /**
     * Null when the requirement carries no version specifier.
     */
    @Nullable
    PythonVersionSpecifierSet specifiers;

    /**
     * The URL of a {@code name @ url} requirement.
     */
    @Nullable
    String url;

    @Nullable
    Marker marker;

    /**
     * The original requirement string.
     */
    String value;

    public static @Nullable Pep508Requirement parse(@Nullable String requirement) {
        if (requirement == null) {
            return null;
        }
        Pep508Parser.ParsedRequirement parsed;
        try {
            parsed = Pep508Parser.parseRequirement(requirement);
        } catch (Pep508Parser.SyntaxException e) {
            return null;
        }
        PythonVersionSpecifierSet specifiers = parsed.specifier.isEmpty() ?
                null : PythonVersionSpecifierSet.parse(parsed.specifier);
        if (!parsed.specifier.isEmpty() && specifiers == null) {
            return null;
        }
        return new Pep508Requirement(
                parsed.name,
                canonicalize(parsed.name),
                new LinkedHashSet<>(parsed.extras),
                specifiers,
                parsed.url.isEmpty() ? null : parsed.url,
                parsed.marker == null ? null : new Marker(parsed.marker),
                requirement);
    }

    /**
     * PEP 503 name normalization: lowercase with runs of {@code -}, {@code _} and {@code .}
     * collapsed to a single {@code -}.
     */
    public static String canonicalize(String name) {
        String value = name.toLowerCase(Locale.ROOT).replace('_', '-').replace('.', '-');
        while (value.contains("--")) {
            value = value.replace("--", "-");
        }
        return value;
    }
}
