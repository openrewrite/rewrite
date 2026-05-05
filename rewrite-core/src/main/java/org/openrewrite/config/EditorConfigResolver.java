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

import org.openrewrite.PathUtils;
import org.openrewrite.internal.lang.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Resolves effective {@code .editorconfig} properties for a given file by walking
 * directories upward from the file's location to the project root, collecting and
 * merging matching sections. Child directories override parent directories.
 */
public class EditorConfigResolver {
    private static final String EDITOR_CONFIG_FILE = ".editorconfig";

    private final @Nullable Path projectRoot;
    private final EditorConfigParser parser = new EditorConfigParser();
    private final Map<Path, EditorConfigParser.EditorConfigFile> parsedFileCache = new HashMap<>();
    private final Map<Path, List<EditorConfigParser.EditorConfigFile>> configsPerDirectory = new HashMap<>();

    public EditorConfigResolver(@Nullable Path projectRoot) {
        this.projectRoot = projectRoot != null ? projectRoot.toAbsolutePath().normalize() : null;
    }

    /**
     * Resolve the effective editorconfig properties for the given file path.
     *
     * @param filePath absolute path to the file
     * @return merged properties map, or empty if no .editorconfig applies
     */
    public Map<String, String> resolve(Path filePath) {
        Path absPath = filePath.toAbsolutePath().normalize();
        Path dir = absPath.getParent();
        if (dir == null) {
            return Collections.emptyMap();
        }

        String fileName = absPath.getFileName().toString();
        List<EditorConfigParser.EditorConfigFile> configs = collectConfigs(dir);

        // Merge matching sections
        Map<String, String> merged = new LinkedHashMap<>();
        for (EditorConfigParser.EditorConfigFile config : configs) {
            for (EditorConfigParser.Section section : config.getSections()) {
                if (PathUtils.matchesGlob(fileName, section.getPattern())) {
                    merged.putAll(section.getProperties());
                }
            }
        }
        return merged;
    }

    private List<EditorConfigParser.EditorConfigFile> collectConfigs(Path dir) {
        List<EditorConfigParser.EditorConfigFile> cached = configsPerDirectory.get(dir);
        if (cached != null) {
            return cached;
        }

        List<EditorConfigParser.EditorConfigFile> configs = new ArrayList<>();
        Path current = dir;
        while (current != null) {
            Path ecFile = current.resolve(EDITOR_CONFIG_FILE);
            if (Files.isRegularFile(ecFile)) {
                EditorConfigParser.EditorConfigFile parsed = parseFile(ecFile);
                if (parsed != null) {
                    configs.add(parsed);
                    if (parsed.isRoot()) {
                        break;
                    }
                }
            }
            if (projectRoot != null && current.equals(projectRoot)) {
                break;
            }
            Path parent = current.getParent();
            if (parent != null && parent.equals(current)) {
                break;
            }
            current = parent;
        }

        // Reverse so parents come first, children override
        Collections.reverse(configs);
        configsPerDirectory.put(dir, configs);
        return configs;
    }

    private @Nullable EditorConfigParser.EditorConfigFile parseFile(Path ecFile) {
        return parsedFileCache.computeIfAbsent(ecFile, f -> {
            try {
                return parser.parse(f);
            } catch (IOException e) {
                return null;
            }
        });
    }
}
