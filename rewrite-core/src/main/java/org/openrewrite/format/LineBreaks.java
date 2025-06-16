/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.format;

public class LineBreaks {
    public static String normalizeNewLines(String text, boolean useCrlf) {
        if (!text.contains("\n")) {
            return text;
        }
        StringBuilder normalized = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (useCrlf && c == '\n' && (i == 0 || text.charAt(i - 1) != '\r')) {
                normalized.append('\r').append('\n');
            } else if (useCrlf || c != '\r') {
                normalized.append(c);
            }
        }
        return normalized.toString();
    }
}
