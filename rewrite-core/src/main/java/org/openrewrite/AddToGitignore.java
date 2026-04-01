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
package org.openrewrite;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextParser;
import org.openrewrite.text.PlainTextVisitor;

import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.openrewrite.internal.StringUtils.isBlank;

@Value
@EqualsAndHashCode(callSuper = false)
public class AddToGitignore extends ScanningRecipe<AtomicBoolean> {

    @Option(displayName = "Entries",
            description = "Multiline text containing gitignore entries to add, each on a separate line. Comments and blank lines are preserved.",
            example = "*.tmp\n.DS_Store\ntarget/")
    String entries;

    @Option(displayName = "File pattern",
            description = "A glob pattern to match `.gitignore` files to update. Defaults to only the root `.gitignore` file. " +
                    "Use `**/.gitignore` to update all `.gitignore` files in the repository, or specify a specific path like `src/.gitignore`.",
            required = false,
            example = ".gitignore")
    @Nullable
    String filePattern;

    String displayName = "Add entries to `.gitignore`";

    String description = "Adds entries to the project's `.gitignore` file. If no `.gitignore` file exists, one will be created. " +
                "Existing entries that match will not be duplicated.";

    @Override
    public AtomicBoolean getInitialValue(ExecutionContext ctx) {
        return new AtomicBoolean(true);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(AtomicBoolean shouldCreate) {
        String pattern = getEffectiveFilePattern();
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof SourceFile) {
                    String sourcePath = PathUtils.separatorsToUnix(((SourceFile) tree).getSourcePath().toString());
                    // Check if this source file matches our target pattern
                    if (sourcePath.endsWith(".gitignore") && shouldNotCreate(sourcePath, pattern)) {
                        shouldCreate.set(false);
                    }
                }
                return tree;
            }

            private boolean shouldNotCreate(String sourcePath, String pattern) {
                return pattern.contains("*") || pattern.contains("?") || sourcePath.equals(pattern);
            }
        };
    }

    @Override
    public Collection<? extends SourceFile> generate(AtomicBoolean shouldCreate, ExecutionContext ctx) {
        if (shouldCreate.get()) {
            String pattern = getEffectiveFilePattern();
            String path = patternToPathToCreate(pattern);
            return PlainTextParser.builder().build()
                    .parse("")
                    .map(text -> (SourceFile) text.withSourcePath(Paths.get(path)))
                    .collect(toList());
        }
        return emptyList();
    }

    private String getEffectiveFilePattern() {
        return isBlank(filePattern) ? ".gitignore" : filePattern;
    }

    private String patternToPathToCreate(String pattern) {
        // Extract a concrete path from the pattern for file generation
        if (!pattern.contains("*") && !pattern.contains("?")) {
            // It's already a concrete path
            return pattern;
        }
        if ("**/.gitignore".equals(pattern)) {
            // Default to root for wildcard patterns
            return ".gitignore";
        }
        if (pattern.endsWith("/.gitignore")) {
            // Specific directory pattern like "src/.gitignore"
            return pattern;
        }
        // Default to root for complex patterns
        return ".gitignore";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(AtomicBoolean shouldCreate) {
        String pattern = getEffectiveFilePattern();
        return Preconditions.check(new FindSourceFiles(pattern), new PlainTextVisitor<ExecutionContext>() {
            @Override
            public PlainText visitText(PlainText text, ExecutionContext ctx) {
                return text.withText(mergeGitignoreEntries(text.getText(), entries));
            }

            private String mergeGitignoreEntries(String existing, String newEntries) {
                String separator = existing.contains("\r\n") ? "\r\n" : "\n";
                boolean endsWithNewline = existing.isEmpty() || existing.endsWith("\n");

                Set<String> existingRules = new LinkedHashSet<>();
                Set<String> existingWildcardPatterns = new LinkedHashSet<>();
                Set<String> existingComments = new LinkedHashSet<>();
                List<String> existingLines = new ArrayList<>();

                if (!StringUtils.isBlank(existing)) {
                    for (String line : existing.split("\r?\n")) {
                        existingLines.add(line);
                        String trimmed = line.trim();
                        if (!StringUtils.isBlank(trimmed)) {
                            if (trimmed.startsWith("#")) {
                                existingComments.add(trimmed);
                            } else {
                                existingRules.add(normalizeRule(trimmed));
                                // Track wildcard patterns and directory patterns for superfluous entry checking
                                if (trimmed.endsWith("/")) {
                                    existingWildcardPatterns.add(trimmed + "**");
                                } else if (trimmed.contains("*")) {
                                    existingWildcardPatterns.add(trimmed);
                                }
                            }
                        }
                    }
                }

                List<String> linesToAdd = new ArrayList<>();

                for (String entry : newEntries.split("\r?\n")) {
                    String trimmed = entry.trim();
                    if (StringUtils.isBlank(trimmed)) {
                        continue;
                    }

                    if (trimmed.startsWith("#")) {
                        if (existingComments.add(trimmed)) {
                            linesToAdd.add(entry);
                        }
                    } else if (!isRedundantEntry(trimmed, existingWildcardPatterns) && existingRules.add(normalizeRule(trimmed))) {
                        linesToAdd.add(entry);
                    }
                }

                if (linesToAdd.isEmpty()) {
                    return existing;
                }

                List<String> result = new ArrayList<>(existingLines);

                if (!existingLines.isEmpty() && !StringUtils.isBlank(existingLines.get(existingLines.size() - 1))) {
                    result.add("");
                }

                result.addAll(linesToAdd);

                String joined = String.join(separator, result);
                return endsWithNewline ? joined + separator : joined;
            }

            private String normalizeRule(String rule) {
                String normalized = rule.trim();

                if (normalized.startsWith("!")) {
                    normalized = normalized.substring(1).trim();
                }
                if (normalized.endsWith("/")) {
                    normalized = normalized.substring(0, normalized.length() - 1);
                }
                if (normalized.startsWith("/")) {
                    normalized = normalized.substring(1);
                }

                return normalized;
            }

            private boolean isRedundantEntry(String entry, Set<String> existingWildcardPatterns) {
                for (String pattern : existingWildcardPatterns) {
                    if (PathUtils.matchesGlob(entry, pattern)) {
                        return true;
                    }
                }
                return false;
            }
        });
    }
}
