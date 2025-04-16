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
package org.openrewrite.java;

import lombok.experimental.UtilityClass;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

// Intentionally package private, at least for now, to limit the publicly exposed API surface
@UtilityClass
final class JavaKeywordUtils {

    private static final String[] RESERVED_KEYWORDS = new String[]{
          "abstract",
          "assert",
          "boolean",
          "break",
          "byte",
          "case",
          "catch",
          "char",
          "class",
          "const",
          "continue",
          "default",
          "do",
          "double",
          "else",
          "enum",
          "extends",
          "final",
          "finally",
          "float",
          "for",
          "goto",
          "if",
          "implements",
          "import",
          "instanceof",
          "int",
          "interface",
          "long",
          "native",
          "new",
          "package",
          "private",
          "protected",
          "public",
          "return",
          "short",
          "static",
          "strictfp",
          "super",
          "switch",
          "synchronized",
          "this",
          "throw",
          "throws",
          "transient",
          "try",
          "void",
          "volatile",
          "while",
    };

    private static final String[] RESERVED_IDENTIFIERS = new String[]{
          "exports",
          "module",
          "non-sealed",
          "open",
          "opens",
          "permits",
          "provides",
          "record",
          "requires",
          "sealed",
          "to",
          "transitive",
          "uses",
          "var",
          "when",
          "with",
          "yield"
    };

    private static final String[] RESERVED_LITERALS = new String[]{
          "true",
          "false",
          "null"
    };

    private static final Set<String> RESERVED_KEYWORDS_SET = new HashSet<>(Arrays.asList(RESERVED_KEYWORDS));
    private static final Set<String> RESERVED_IDENTIFIERS_SET = new HashSet<>(Arrays.asList(RESERVED_IDENTIFIERS));
    private static final Set<String> RESERVED_LITERALS_SET = new HashSet<>(Arrays.asList(RESERVED_LITERALS));

    static boolean isReservedKeyword(String word) {
        return RESERVED_KEYWORDS_SET.contains(word);
    }

    static boolean isReservedIdentifier(String word) {
        return RESERVED_IDENTIFIERS_SET.contains(word);
    }

    static boolean isReservedLiteral(String word) {
        return RESERVED_LITERALS_SET.contains(word);
    }
}
