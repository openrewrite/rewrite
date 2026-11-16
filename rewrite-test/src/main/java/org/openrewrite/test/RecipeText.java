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
package org.openrewrite.test;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Finds what a recipe's display name and description write as prose that the docs, which render both as Markdown,
 * show better otherwise.
 */
final class RecipeText {

    private static final Pattern NOT_PROSE = Pattern.compile(
            "```.*?```" +
            "|`[^`]*`" +
            "|\\]\\([^)]*\\)" +
            "|<[^>\\s]+>" +
            "|https?://\\S+",
            Pattern.DOTALL);

    private static final Pattern CODE = Pattern.compile(
            "(?<![\\w.$/-])(?:[a-z_][a-z0-9_]*\\.){2,}[A-Z][A-Za-z0-9_$]*(?:#[a-z]\\w*)?(?:\\(\\))?(?![\\w$])" +
            "|(?<![\\w@.{])@[A-Z][A-Za-z0-9_]*(?:\\.[A-Z][A-Za-z0-9_]*)*(?![\\w])" +
            "|(?<![\\w.#/])[A-Z][A-Za-z0-9_]*#[a-z]\\w*(?:\\(\\))?");

    private static final Set<String> MINOR_WORDS = new HashSet<>(Arrays.asList(
            "A", "An", "And", "As", "At", "By", "For", "From", "In", "Into", "Of", "On", "Or", "The", "To", "Via",
            "With"));

    private RecipeText() {
    }

    /**
     * @return The type names, annotations and {@code Type#method} references in {@code markdown} that no code span
     * holds.
     */
    static List<String> codeOutsideCodeSpans(String markdown) {
        List<String> code = new ArrayList<>();
        Matcher matcher = CODE.matcher(prose(markdown));
        while (matcher.find()) {
            code.add(matcher.group());
        }
        return code;
    }

    /**
     * @return The words after the first that title case capitalizes and sentence case doesn't, such as "To" in
     * "Migrate To Spring Boot 3".
     */
    static List<String> titleCaseWords(String displayName) {
        List<String> words = new ArrayList<>();
        String[] split = prose(displayName).trim().split("\\s+");
        for (int i = 1; i < split.length; i++) {
            if (MINOR_WORDS.contains(split[i])) {
                words.add(split[i]);
            }
        }
        return words;
    }

    private static String prose(String markdown) {
        return NOT_PROSE.matcher(markdown).replaceAll(" ");
    }
}
