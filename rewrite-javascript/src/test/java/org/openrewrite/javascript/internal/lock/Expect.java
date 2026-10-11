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
package org.openrewrite.javascript.internal.lock;

import lombok.Value;
import org.jspecify.annotations.Nullable;

/**
 * A scenario's {@code expect} line: its first word, the kind ({@code exact} without a line), and the rest.
 */
@Value
public class Expect {

    public static final String EXACT = "exact";

    /**
     * The engine does not reproduce what the package manager did, for the reason the line gives; the edit is skipped.
     */
    public static final String NOT_REPRODUCED = "not-reproduced";

    /**
     * The package manager crashes or never ends on the edit.
     */
    public static final String PM_FAILS = "pm-fails";

    @Nullable
    String line;

    String kind;
    String rest;

    public static Expect of(@Nullable String line) {
        if (line == null || line.trim().isEmpty()) {
            return new Expect(null, EXACT, "");
        }
        String trimmed = line.trim();
        String kind = trimmed.split("\\s+")[0];
        return new Expect(trimmed, kind, trimmed.substring(kind.length()).trim());
    }

    public boolean isExact() {
        return EXACT.equals(kind);
    }

    /**
     * The words of the line, the kind first.
     */
    public String[] words() {
        return line == null ? new String[0] : line.split("\\s+");
    }

    @Override
    public String toString() {
        return String.valueOf(line);
    }
}
