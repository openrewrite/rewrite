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
import org.openrewrite.marker.Markers;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.quark.Quark;
import org.openrewrite.remote.Remote;
import org.openrewrite.table.TextMatches;

import java.util.ArrayList;
import java.util.List;
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
                Matcher matcher = pattern.matcher(plainText.getText());
                String rawText = plainText.getText();
                if (!matcher.find()) {
                    return sourceFile;
                }

                String sourceFilePath = sourceFile.getSourcePath().toString();

                List<PlainText.Snippet> snippets = new ArrayList<>();
                int previousEnd = 0;

                int lastNewLineIndex = -1;
                int nextNewLineIndex = -1;
                boolean isFirstMatch = true;

                do {
                    int matchStart = matcher.start();
                    snippets.add(snippet(rawText.substring(previousEnd, matchStart)));
                    String text = rawText.substring(matchStart, matcher.end());
                    snippets.add(SearchResult.found(snippet(text), Boolean.TRUE.equals(description) ? text : null));
                    previousEnd = matcher.end();

                    // For the first match, search backwards
                    if (isFirstMatch) {
                        lastNewLineIndex = rawText.lastIndexOf('\n', matchStart);
                        nextNewLineIndex = rawText.indexOf('\n', lastNewLineIndex + 1);
                        isFirstMatch = false;
                    } else if (nextNewLineIndex != -1 && nextNewLineIndex < matchStart) {
                        // Advance lastNewLineIndex while before match start
                        while (nextNewLineIndex != -1 && nextNewLineIndex < matchStart) {
                            lastNewLineIndex = nextNewLineIndex;
                            nextNewLineIndex = rawText.indexOf('\n', lastNewLineIndex + 1);
                        }
                    }

                    int startLine = lastNewLineIndex + 1;
                    int endLine = nextNewLineIndex > matcher.end() ? nextNewLineIndex : rawText.indexOf('\n', matcher.end());
                    if (endLine == -1) {
                        endLine = rawText.length();
                    }

                    String context = truncateContext(endLine, startLine, matcher, matchStart, rawText);

                    textMatches.insertRow(ctx, new TextMatches.Row(sourceFilePath, context));
                } while (matcher.find());
                snippets.add(snippet(rawText.substring(previousEnd)));
                return plainText.withText("").withSnippets(snippets);
            }

            private String truncateContext(int endLine, int startLine, Matcher matcher, int matchStart, String rawText) {
                String matchText = matcher.group();
                int contextLength = contextSize == null ? 0 : contextSize;
                int contextStart = contextLength == -1 ? startLine : matchStart - contextLength;
                int contextEnd = contextLength == -1 ? endLine : matchStart + matchText.length() + contextLength;

                StringBuilder sb = new StringBuilder();

                if (contextStart > startLine) {
                    sb.append("...");
                }

                sb.append(rawText, Math.max(contextStart, startLine), matchStart)
                        .append("~~>")
                        .append(rawText, matchStart, Math.min(contextEnd, endLine));

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
