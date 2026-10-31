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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Translates a poetry dependency version constraint ({@code [tool.poetry.dependencies]} syntax) into
 * PEP 440 the way poetry-core does: a bare version is an exact pin, {@code ^} is a caret range and
 * {@code ~} a tilde range, each keeping the precision it is written with; PEP 440 operators pass
 * through, and comma- or space-separated parts all apply. Returns {@code null} for forms with no
 * single PEP 440 equivalent (e.g. {@code ||} unions).
 */
public final class PoetryConstraint {

    private static final Pattern OPERATOR_SPACE = Pattern.compile("([<>=!~^])\\s+");
    private static final Pattern PART_SEPARATOR = Pattern.compile("[,\\s]+");

    private PoetryConstraint() {
    }

    /**
     * As {@link #toPep440}, except that a wildcard, which callers treat as needing resolution, is null too.
     */
    static @Nullable PythonVersionSpecifierSet toSpecifierSet(String constraint) {
        if (constraint.contains("*")) {
            return null;
        }
        String pep440 = toPep440(constraint);
        return pep440 == null ? null : PythonVersionSpecifierSet.parse(pep440);
    }

    public static @Nullable String toPep440(String constraint) {
        String s = constraint.trim();
        if (s.isEmpty() || "*".equals(s) || s.contains("|")) {
            return null;
        }
        if (PythonVersionSpecifierSet.parse(s) != null) {
            return s;
        }
        List<String> parts = new ArrayList<>();
        for (String part : PART_SEPARATOR.split(OPERATOR_SPACE.matcher(s).replaceAll("$1"))) {
            String pep440 = part.isEmpty() ? null : partToPep440(part);
            if (pep440 == null) {
                return null;
            }
            parts.add(pep440);
        }
        String pep440 = String.join(",", parts);
        return PythonVersionSpecifierSet.parse(pep440) == null ? null : pep440;
    }

    private static @Nullable String partToPep440(String part) {
        if (part.startsWith("^")) {
            return caret(part.substring(1));
        } else if (part.startsWith("~") && !part.startsWith("~=")) {
            return tilde(part.substring(1));
        } else if (startsWithOperator(part)) {
            return part;
        }
        return "==" + part;
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
        int bumped = 0;
        while (bumped < parts.length - 1 && parts[bumped] == 0) {
            bumped++;
        }
        return ">=" + version + ",<" + bump(parts, bumped);
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
        return ">=" + version + ",<" + bump(parts, Math.min(1, parts.length - 1));
    }

    private static String bump(int[] parts, int index) {
        StringBuilder upper = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                upper.append('.');
            }
            upper.append(i < index ? parts[i] : i == index ? parts[i] + 1 : 0);
        }
        return upper.toString();
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
