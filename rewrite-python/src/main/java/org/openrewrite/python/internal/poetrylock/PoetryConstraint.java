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
package org.openrewrite.python.internal.poetrylock;

import org.jspecify.annotations.Nullable;
import org.openrewrite.python.internal.pep440.PythonVersionSpecifierSet;

/**
 * Translates a poetry dependency version constraint ({@code [tool.poetry.dependencies]} syntax) into
 * a PEP 440 {@link PythonVersionSpecifierSet}: a bare version is an exact pin, {@code ^} is a caret
 * range and {@code ~} a tilde range; PEP 440 operators pass through. Returns {@code null} for forms
 * the engine will not attempt (e.g. {@code ||} unions), which callers treat as needing resolution.
 */
final class PoetryConstraint {

    private PoetryConstraint() {
    }

    static @Nullable PythonVersionSpecifierSet toSpecifierSet(String constraint) {
        String s = constraint.trim();
        if (s.isEmpty() || "*".equals(s) || s.contains("||") || s.contains("*")) {
            return null;
        }
        String pep440;
        if (s.charAt(0) == '^') {
            pep440 = caret(s.substring(1).trim());
        } else if (s.charAt(0) == '~') {
            pep440 = tilde(s.substring(1).trim());
        } else if (startsWithOperator(s)) {
            pep440 = s;
        } else {
            pep440 = "==" + s;
        }
        return pep440 == null ? null : PythonVersionSpecifierSet.parse(pep440);
    }

    private static boolean startsWithOperator(String s) {
        char c = s.charAt(0);
        return c == '>' || c == '<' || c == '=' || c == '!' || c == '~';
    }

    /**
     * Caret: {@code >=v} up to the next increment of the most significant non-zero component.
     */
    private static @Nullable String caret(String version) {
        int[] parts = numericParts(version);
        if (parts == null) {
            return null;
        }
        int major = parts[0];
        int minor = parts.length > 1 ? parts[1] : 0;
        int patch = parts.length > 2 ? parts[2] : 0;
        String upper;
        if (major > 0 || parts.length == 1) {
            upper = (major + 1) + ".0.0";
        } else if (minor > 0 || parts.length == 2) {
            upper = "0." + (minor + 1) + ".0";
        } else {
            upper = "0.0." + (patch + 1);
        }
        return ">=" + version + ",<" + upper;
    }

    /**
     * Tilde: {@code >=v} up to the next increment of the second component (or major when only one
     * component is given).
     */
    private static @Nullable String tilde(String version) {
        int[] parts = numericParts(version);
        if (parts == null) {
            return null;
        }
        String upper = parts.length == 1 ? (parts[0] + 1) + ".0.0" : parts[0] + "." + (parts[1] + 1) + ".0";
        return ">=" + version + ",<" + upper;
    }

    private static int @Nullable [] numericParts(String version) {
        String[] raw = version.split("\\.");
        int[] parts = new int[raw.length];
        for (int i = 0; i < raw.length; i++) {
            try {
                parts[i] = Integer.parseInt(raw[i].trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return parts.length == 0 ? null : parts;
    }
}
