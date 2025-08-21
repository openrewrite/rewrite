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
            description = "A glob pattern to match .gitignore files to update. Defaults to only the root .gitignore file. " +
                    "Use `**/.gitignore` to update all .gitignore files in the repository, or specify a specific path like `src/.gitignore`.",
            required = false,
            example = ".gitignore")
    @Nullable
    String filePattern;

    @Override
    public String getDisplayName() {
        return "Add entries to .gitignore";
    }

    @Override
    public String getDescription() {
        return "Adds entries to the project's .gitignore file. If no .gitignore file exists, one will be created. " +
                "Existing entries that match will not be duplicated.";
    }

    @Override
    public AtomicBoolean getInitialValue(ExecutionContext ctx) {
        return new AtomicBoolean(true);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(AtomicBoolean shouldCreate) {
        String pattern = getEffectiveFilePattern();
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree visit(Tree tree, ExecutionContext ctx) {
                if (tree instanceof SourceFile) {
                    SourceFile sourceFile = (SourceFile) tree;
                    String sourcePath = sourceFile.getSourcePath().toString();
                    // Check if this source file matches our target pattern
                    if (sourcePath.endsWith(".gitignore") && matchesPattern(sourcePath, pattern)) {
                        shouldCreate.set(false);
                    }
                }
                return tree;
            }
        };
    }

    @Override
    public Collection<? extends SourceFile> generate(AtomicBoolean shouldCreate, ExecutionContext ctx) {
        if (shouldCreate.get()) {
            String pattern = getEffectiveFilePattern();
            // Extract the path from the pattern for simple cases
            String path = extractPathFromPattern(pattern);
            return PlainTextParser.builder().build()
                    .parse(entries)
                    .map(text -> (SourceFile) text.withSourcePath(Paths.get(path)))
                    .collect(toList());
        }
        return emptyList();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(AtomicBoolean acc) {
        String pattern = getEffectiveFilePattern();
        // Use the pattern directly for FindSourceFiles
        return Preconditions.check(new FindSourceFiles(pattern), new PlainTextVisitor<ExecutionContext>() {
            @Override
            public PlainText visitText(PlainText text, ExecutionContext ctx) {
                String existingContent = text.getText();
                String mergedContent = mergeGitignoreEntries(existingContent, entries);

                if (!existingContent.equals(mergedContent)) {
                    return text.withText(mergedContent);
                }

                return text;
            }
        });
    }

    private String mergeGitignoreEntries(String existing, String newEntries) {
        String separator = existing.contains("\r\n") ? "\r\n" : "\n";

        Set<String> existingRules = new LinkedHashSet<>();
        Set<String> existingWildcardPatterns = new LinkedHashSet<>();
        Set<String> existingComments = new LinkedHashSet<>();
        List<String> existingLines = new ArrayList<>();

        if (!StringUtils.isBlank(existing)) {
            String[] lines = existing.split("\r?\n");
            for (String line : lines) {
                existingLines.add(line);
                String trimmed = line.trim();
                if (!StringUtils.isBlank(trimmed)) {
                    if (trimmed.startsWith("#")) {
                        existingComments.add(trimmed);
                    } else {
                        existingRules.add(normalizeRule(trimmed));
                        // Track wildcard patterns and directory patterns for superfluous entry checking
                        if (trimmed.contains("*") || trimmed.endsWith("/")) {
                            existingWildcardPatterns.add(trimmed);
                        }
                    }
                }
            }
        }

        List<String> newLines = new ArrayList<>();
        String[] entriesToAdd = newEntries.split("\r?\n");

        for (String entry : entriesToAdd) {
            String trimmed = entry.trim();
            if (StringUtils.isBlank(trimmed)) {
                continue;
            }

            if (trimmed.startsWith("#")) {
                if (!existingComments.contains(trimmed)) {
                    newLines.add(entry);
                    existingComments.add(trimmed);
                }
            } else {
                String normalized = normalizeRule(trimmed);
                if (!existingRules.contains(normalized) && !isSuperfluousEntry(trimmed, existingWildcardPatterns)) {
                    newLines.add(entry);
                    existingRules.add(normalized);
                }
            }
        }

        if (newLines.isEmpty()) {
            return existing;
        }

        List<String> result = new ArrayList<>(existingLines);

        if (!existingLines.isEmpty() && !StringUtils.isBlank(existingLines.get(existingLines.size() - 1))) {
            result.add("");
        }

        result.addAll(newLines);

        return String.join(separator, result);
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

        return normalized.toLowerCase();
    }

    private boolean isSuperfluousEntry(String entry, Set<String> existingWildcardPatterns) {
        // Check if this entry would be redundant given existing wildcard patterns
        for (String pattern : existingWildcardPatterns) {
            if (matchesGitignorePattern(entry, pattern)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesGitignorePattern(String path, String pattern) {
        // Simple gitignore pattern matching
        // This is a simplified implementation - gitignore patterns can be complex

        // Remove leading slashes for comparison
        String normalizedPath = path.trim();
        String normalizedPattern = pattern.trim();

        if (normalizedPath.startsWith("/")) {
            normalizedPath = normalizedPath.substring(1);
        }
        if (normalizedPattern.startsWith("/")) {
            normalizedPattern = normalizedPattern.substring(1);
        }

        // Handle directory patterns (ending with /)
        boolean isDirectoryPattern = normalizedPattern.endsWith("/");
        if (isDirectoryPattern) {
            normalizedPattern = normalizedPattern.substring(0, normalizedPattern.length() - 1);
            // If the path is also a directory pattern, remove trailing slash
            if (normalizedPath.endsWith("/")) {
                normalizedPath = normalizedPath.substring(0, normalizedPath.length() - 1);
            }
            // Check if the path is under this directory
            if (normalizedPath.equals(normalizedPattern) ||
                    normalizedPath.startsWith(normalizedPattern + "/")) {
                return true;
            }
        }

        // Convert gitignore pattern to regex for wildcard matching
        String regex = normalizedPattern
                .replace(".", "\\.")
                .replace("**", "§§§")  // Temporary placeholder for **
                .replace("*", "[^/]*")  // * matches anything except /
                .replace("§§§", ".*")   // ** matches anything including /
                .replace("?", "[^/]");  // ? matches single character except /

        // Check if the path matches the pattern
        return normalizedPath.matches(regex);
    }

    private String getEffectiveFilePattern() {
        return isBlank(filePattern) ? ".gitignore" : filePattern;
    }

    private boolean matchesPattern(String sourcePath, String pattern) {
        // Simple pattern matching for common cases
        if (pattern.equals(".gitignore")) {
            // Root .gitignore only
            return sourcePath.equals(".gitignore");
        } else if (pattern.equals("**/.gitignore")) {
            // All .gitignore files
            return true;
        } else if (!pattern.contains("*") && !pattern.contains("?")) {
            // Exact path match
            return sourcePath.equals(pattern);
        } else {
            // For more complex patterns, we'll rely on FindSourceFiles
            // This is a simplified check just for the scanner
            return true;
        }
    }

    private String extractPathFromPattern(String pattern) {
        // Extract a concrete path from the pattern for file generation
        if (!pattern.contains("*") && !pattern.contains("?")) {
            // It's already a concrete path
            return pattern;
        } else if ("**/.gitignore".equals(pattern)) {
            // Default to root for wildcard patterns
            return ".gitignore";
        } else if (pattern.endsWith("/.gitignore")) {
            // Specific directory pattern like "src/.gitignore"
            return pattern;
        } else {
            // Default to root for complex patterns
            return ".gitignore";
        }
    }
}
