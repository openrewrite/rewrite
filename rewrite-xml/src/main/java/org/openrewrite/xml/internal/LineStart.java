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
package org.openrewrite.xml.internal;

import static org.openrewrite.internal.StringUtils.repeat;

/**
 * The start of a line whose element was changed or replaced in place: a formatter gives such an element the
 * indentation of a document formatted from scratch, but the line keeps the indentation it had.
 */
public final class LineStart {

    private LineStart() {
    }

    /**
     * The prefix for an element that took the place of an element whose prefix was {@code original}, given
     * {@code given} and then formatted to {@code formatted}. When the element was given the original's indentation, so
     * that it stays where the original was, and the formatted element still starts its line, the line keeps that
     * indentation. An element given another indentation keeps the formatter's. Blank lines above the line, when the
     * element was given them as they were, keep their bytes: how many there are and the whitespace on them. Above a
     * line that had none, the formatter chooses the blank lines.
     */
    public static String keep(String formatted, String original, String given) {
        if (original.indexOf('\n') < 0 || given.indexOf('\n') < 0 || formatted.indexOf('\n') < 0 ||
            !afterLastBreak(given).equals(afterLastBreak(original))) {
            return formatted;
        }
        if (original.indexOf('\n') != original.lastIndexOf('\n') && original.equals(given)) {
            // blank lines the element was given as they were are the document's, not the recipe's
            return original;
        }
        String lineBreak = original.contains("\r\n") ? "\r\n" : "\n";
        int breaks = (int) formatted.chars().filter(c -> c == '\n').count();
        return repeat(lineBreak, breaks) + afterLastBreak(original);
    }

    private static String afterLastBreak(String whitespace) {
        return whitespace.substring(whitespace.lastIndexOf('\n') + 1);
    }
}
