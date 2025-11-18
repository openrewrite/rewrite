/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.text;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.binary.Binary;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.marker.Markers;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.quark.Quark;
import org.openrewrite.remote.Remote;
import org.openrewrite.table.TextMatches;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;

@Value
@EqualsAndHashCode(callSuper = false)
public class Find extends Recipe {
    transient TextMatches textMatches = new TextMatches(this);

    @Override
    public String getDisplayName() {
        return "Find text";
    }

    @Override
    public String getDescription() {
        return "Textual search, optionally using Regular Expression (regex) to query.";
    }

    @Option(displayName = "Find",
            description = "The text to find. This snippet can be multiline.",
            example = "blacklist")
    String find;

    @Option(displayName = "Regex",
            description = "If true, `find` will be interpreted as a [Regular Expression](https://en.wikipedia.org/wiki/Regular_expression). Default `false`.",
            required = false)
    @Nullable
    Boolean regex;

    @Option(displayName = "Case sensitive",
            description = "If `true` the search will be sensitive to case. Default `false`.",
            required = false)
    @Nullable
    Boolean caseSensitive;

    @Option(displayName = "Regex multiline mode",
            description = "When performing a regex search setting this to `true` allows \"^\" and \"$\" to match the beginning and end of lines, respectively. " +
                          "When performing a regex search when this is `false` \"^\" and \"$\" will match only the beginning and ending of the entire source file, respectively." +
                          "Has no effect when not performing a regex search. Default `false`.",
            required = false)
    @Nullable
    Boolean multiline;

    @Option(displayName = "Regex dot all",
            description = "When performing a regex search setting this to `true` allows \".\" to match line terminators." +
                          "Has no effect when not performing a regex search. Default `false`.",
            required = false)
    @Nullable
    Boolean dotAll;

    @Option(displayName = "File pattern",
            description = "A glob expression that can be used to constrain which directories or source files should be searched. " +
                          "Multiple patterns may be specified, separated by a semicolon `;`. " +
                          "If multiple patterns are supplied any of the patterns matching will be interpreted as a match. " +
                          "When not set, all source files are searched.",
            required = false,
            example = "**/*.java")
    @Nullable
    String filePattern;

    @Option(displayName = "Description",
            description = "Add the matched value(s) as description on the search result marker.  Default `false`.",
            required = false)
    @Nullable
    Boolean description;

    @Option(displayName = "Context size for Datatable",
            description = "The number of characters to include in the datatable before and after the match. Default `0`, " +
                          "`-1` indicates that the whole text should be used.",
            required = false,
            example = "50")
    @Nullable
    Integer contextSize;

    @Override
    public String getInstanceName() {
        return String.format("Find text `%s`", find);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {

        TreeVisitor<?, ExecutionContext> visitor = new TreeVisitor<Tree, ExecutionContext>() {

            @Override
            public Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                SourceFile sourceFile = (SourceFile) requireNonNull(tree);
                if (sourceFile instanceof Quark || sourceFile instanceof Remote || sourceFile instanceof Binary) {
                    return sourceFile;
                }
                PlainText plainText = PlainTextParser.convert(sourceFile);

                String searchStr = find;
                if (!Boolean.TRUE.equals(regex)) {
                    searchStr = Pattern.quote(searchStr);
                }
                int patternOptions = 0;
                if (!Boolean.TRUE.equals(caseSensitive)) {
                    patternOptions |= Pattern.CASE_INSENSITIVE;
                }
                if (Boolean.TRUE.equals(multiline)) {
                    patternOptions |= Pattern.MULTILINE;
                }
                if (Boolean.TRUE.equals(dotAll)) {
                    patternOptions |= Pattern.DOTALL;
                }
                Pattern pattern = Pattern.compile(searchStr, patternOptions);

                List<PlainText.Snippet> inputSnippets = ListUtils.concat(snippet(plainText.getText()), plainText.getSnippets());
                List<PlainText.Snippet> newSnippets = new ArrayList<>();
                boolean foundAnyMatch = false;
                StringBuilder fullTextContext = new StringBuilder();

                for (PlainText.Snippet snippet : inputSnippets) {
                    // Skip snippets that already have a SearchResult marker - they've been processed by a previous Find
                    if (snippet.getMarkers().findFirst(SearchResult.class).isPresent()) {
                        newSnippets.add(snippet);
                        fullTextContext.append(snippet.getText());
                        continue;
                    }

                    String snippetText = snippet.getText();
                    Matcher matcher = pattern.matcher(snippetText);

                    if (!matcher.find()) {
                        // No match in this snippet, keep it as is
                        newSnippets.add(snippet);
                        fullTextContext.append(snippetText);
                        continue;
                    }

                    foundAnyMatch = true;
                    String sourceFilePath = sourceFile.getSourcePath().toString();

                    // Calculate offset of current snippet in the full text
                    int snippetOffset = fullTextContext.length();
                    String fullText = fullTextContext + snippetText;

                    AtomicInteger lastNewLineIndex = new AtomicInteger(-1);
                    AtomicInteger nextNewLineIndex = new AtomicInteger(-1);
                    AtomicBoolean isFirstMatch = new AtomicBoolean(true);

                    List<PlainText.Snippet> matchedSnippets = processMatches(snippetText, matcher, sourceFilePath, ctx,
                            (text) -> {
                                int matchStart = matcher.start() + snippetOffset;
                                int matchEnd = matcher.end() + snippetOffset;

                                // For the first match, search backwards
                                if (isFirstMatch.get()) {
                                    lastNewLineIndex.set(fullText.lastIndexOf('\n', matchStart));
                                    nextNewLineIndex.set(fullText.indexOf('\n', lastNewLineIndex.get() + 1));
                                    isFirstMatch.set(false);
                                } else if (nextNewLineIndex.get() != -1 && nextNewLineIndex.get() < matchStart) {
                                    // Advance lastNewLineIndex while before match start
                                    while (nextNewLineIndex.get() != -1 && nextNewLineIndex.get() < matchStart) {
                                        lastNewLineIndex.set(nextNewLineIndex.get());
                                        nextNewLineIndex.set(fullText.indexOf('\n', lastNewLineIndex.get() + 1));
                                    }
                                }

                                int startLine = lastNewLineIndex.get() + 1;
                                int endLine = nextNewLineIndex.get() > matchEnd ? nextNewLineIndex.get() : fullText.indexOf('\n', matchEnd);
                                if (endLine == -1) {
                                    endLine = fullText.length();
                                }

                                return truncateContext(endLine, startLine, matchStart, matchEnd, fullText);
                            });

                    newSnippets.addAll(matchedSnippets);
                    fullTextContext.append(snippetText);
                }

                if (!foundAnyMatch) {
                    return sourceFile;
                }

                return plainText.withText("").withSnippets(newSnippets);
            }

            /**
             * Process all matches in the given text and create snippets with SearchResult markers.
             *
             * @param text The text to search within
             * @param matcher The matcher positioned at the first match (after find() has been called)
             * @param sourceFilePath The path to the source file (for data table)
             * @param ctx The execution context
             * @param contextProvider Function to generate context string for data table from matched text
             * @return A new list of snippets with matches marked
             */
            private List<PlainText.Snippet> processMatches(
                    String text, Matcher matcher, String sourceFilePath, ExecutionContext ctx,
                    java.util.function.Function<String, String> contextProvider) {
                List<PlainText.Snippet> snippets = new ArrayList<>();
                int previousEnd = 0;
                do {
                    int matchStart = matcher.start();
                    if (matchStart > previousEnd) {
                        snippets.add(snippet(text.substring(previousEnd, matchStart)));
                    }
                    String matchedText = text.substring(matchStart, matcher.end());
                    snippets.add(SearchResult.found(snippet(matchedText), Boolean.TRUE.equals(description) ? matchedText : null));
                    previousEnd = matcher.end();
                    String context = contextProvider.apply(matchedText);
                    textMatches.insertRow(ctx, new TextMatches.Row(sourceFilePath, context));
                } while (matcher.find());

                if (previousEnd < text.length()) {
                    snippets.add(snippet(text.substring(previousEnd)));
                }
                return snippets;
            }

            private String truncateContext(int endLine, int startLine, int matchStart, int matchEnd, String fullText) {
                int contextLength = contextSize == null ? 0 : contextSize;
                int contextStart = contextLength == -1 ? startLine : matchStart - contextLength;
                int contextEnd = contextLength == -1 ? endLine : matchEnd + contextLength;

                StringBuilder sb = new StringBuilder();

                if (contextStart > startLine) {
                    sb.append("...");
                }

                sb.append(fullText, Math.max(contextStart, startLine), matchStart)
                        .append("~~>")
                        .append(fullText, matchStart, Math.min(contextEnd, endLine));

                if (contextEnd < endLine) {
                    sb.append("...");
                }

                return sb.toString();
            }
        };
        if (filePattern != null) {
            visitor = Preconditions.check(new FindSourceFiles(filePattern), visitor);
        }
        return visitor;
    }

    private static PlainText.Snippet snippet(String text) {
        return new PlainText.Snippet(Tree.randomId(), Markers.EMPTY, text);
    }

}
