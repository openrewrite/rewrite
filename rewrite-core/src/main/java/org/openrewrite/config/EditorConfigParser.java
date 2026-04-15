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
package org.openrewrite.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Parses a single {@code .editorconfig} file into a structured representation.
 */
public class EditorConfigParser {

    public static class EditorConfigFile {
        private final boolean root;
        private final List<Section> sections;

        public EditorConfigFile(boolean root, List<Section> sections) {
            this.root = root;
            this.sections = sections;
        }

        public boolean isRoot() {
            return root;
        }

        public List<Section> getSections() {
            return sections;
        }
    }

    public static class Section {
        private final String pattern;
        private final Map<String, String> properties;

        public Section(String pattern, Map<String, String> properties) {
            this.pattern = pattern;
            this.properties = properties;
        }

        public String getPattern() {
            return pattern;
        }

        public Map<String, String> getProperties() {
            return properties;
        }
    }

    public EditorConfigFile parse(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path);
        return parse(lines);
    }

    public EditorConfigFile parse(List<String> lines) {
        boolean root = false;
        List<Section> sections = new ArrayList<>();
        String currentPattern = null;
        Map<String, String> currentProperties = null;

        for (String rawLine : lines) {
            String line = rawLine.trim();

            // Skip empty lines and comments
            if (line.isEmpty() || line.charAt(0) == '#' || line.charAt(0) == ';') {
                continue;
            }

            // Section header
            if (line.startsWith("[") && line.endsWith("]")) {
                if (currentPattern != null && currentProperties != null) {
                    sections.add(new Section(currentPattern, currentProperties));
                }
                currentPattern = line.substring(1, line.length() - 1).trim();
                currentProperties = new LinkedHashMap<>();
                continue;
            }

            // Key-value pair
            int eqIdx = line.indexOf('=');
            if (eqIdx < 0) {
                continue;
            }
            String key = line.substring(0, eqIdx).trim().toLowerCase(Locale.ENGLISH);
            String value = line.substring(eqIdx + 1).trim().toLowerCase(Locale.ENGLISH);

            if (currentPattern == null) {
                // Preamble (before any section)
                if ("root".equals(key) && "true".equals(value)) {
                    root = true;
                }
            } else {
                currentProperties.put(key, value);
            }
        }

        // Flush last section
        if (currentPattern != null && currentProperties != null) {
            sections.add(new Section(currentPattern, currentProperties));
        }

        return new EditorConfigFile(root, sections);
    }
}
