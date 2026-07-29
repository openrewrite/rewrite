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
package org.openrewrite.javascript.internal.semver;

import org.jspecify.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A single desugared comparator ({@code >=1.2.3}, {@code <2.0.0-0}, ...) as produced by
 * {@link NpmRange}'s pipeline. {@link #ANY} is the empty comparator matching everything.
 */
final class NpmComparator {

    static final NpmComparator ANY = new NpmComparator("", null);

    private static final Pattern COMPARATOR = Pattern.compile("^((?:<|>)?=?)\\s*(.*)$");

    private final String operator;
    private final @Nullable NpmVersion version;

    private NpmComparator(String operator, @Nullable NpmVersion version) {
        this.operator = operator;
        this.version = version;
    }

    static @Nullable NpmComparator parse(String comp) {
        Matcher m = COMPARATOR.matcher(comp.trim());
        if (!m.matches()) {
            return null;
        }
        String op = m.group(1);
        String rest = m.group(2);
        if (rest.isEmpty()) {
            return op.isEmpty() ? ANY : null;
        }
        NpmVersion v = NpmVersion.parse(rest);
        if (v == null) {
            return null;
        }
        return new NpmComparator("=".equals(op) ? "" : op, v);
    }

    @Nullable NpmVersion getVersion() {
        return version;
    }

    boolean test(NpmVersion candidate) {
        if (version == null) {
            return true;
        }
        int cmp = candidate.compareTo(version);
        switch (operator) {
            case "":
                return cmp == 0;
            case ">":
                return cmp > 0;
            case ">=":
                return cmp >= 0;
            case "<":
                return cmp < 0;
            case "<=":
                return cmp <= 0;
            default:
                return false;
        }
    }

    @Override
    public String toString() {
        return version == null ? "" : operator + version;
    }
}
