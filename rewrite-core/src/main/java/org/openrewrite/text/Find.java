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

import java.util.*;
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
                          "When not set, all source files are searched. ",
            required = false,
            example = "**/*.java")
    @Nullable
    String filePattern;

    private static Deque<Integer> findAllNewLineIndexes(String input, int offset) {
        ArrayDeque<Integer> indexes = new ArrayDeque<>();
        int index = input.lastIndexOf('\n', offset); // Find the first occurrence
        if (index != -1) {
            indexes.add(index);
        }

        index = input.indexOf('\n', offset); // Find occurrence after the offset
        while (index != -1) {
            indexes.add(index); // Add the index to the list
            index = input.indexOf('\n', index + 1); // Find the next occurrence
        }

        return indexes;
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
                matcher.reset();

                String sourceFilePath = sourceFile.getSourcePath().toString();

                List<PlainText.Snippet> snippets = new ArrayList<>();
                int previousEnd = 0;

                Deque<Integer> newlineIndexes = null;
                int lastNewLineIndex = -1;

                while (matcher.find()) {
                    if (newlineIndexes == null) {
                        newlineIndexes = findAllNewLineIndexes(rawText, matcher.start());
                    }

                    int matchStart = matcher.start();
                    snippets.add(snippet(rawText.substring(previousEnd, matchStart)));
                    snippets.add(SearchResult.found(snippet(rawText.substring(matchStart, matcher.end()))));
                    previousEnd = matcher.end();

                    while (!newlineIndexes.isEmpty() && newlineIndexes.peek() < matchStart) {
                        lastNewLineIndex = newlineIndexes.pop();
                    }
                    int startLine = Math.max(0, lastNewLineIndex + 1);

                    int endLine = rawText.indexOf('\n', matcher.end());
                    if (endLine == -1) {
                        endLine = rawText.length();
                    }

                    textMatches.insertRow(ctx, new TextMatches.Row(
                            sourceFilePath,
                            new StringBuilder(endLine - startLine + 3)
                                    .append(rawText, startLine, matcher.start())
                                    .append("~~>")
                                    .append(rawText, matcher.start(), endLine)
                                    .toString()
                    ));
                }
                snippets.add(snippet(rawText.substring(previousEnd)));
                return plainText.withText("").withSnippets(snippets);
            }
        };
        //noinspection DuplicatedCode
        if (filePattern != null) {
            //noinspection unchecked
            TreeVisitor<?, ExecutionContext> check = Preconditions.or(Arrays.stream(filePattern.split(";"))
                    .map(FindSourceFiles::new)
                    .map(Recipe::getVisitor)
                    .toArray(TreeVisitor[]::new));

            visitor = Preconditions.check(check, visitor);
        }
        return visitor;
    }

    private static PlainText.Snippet snippet(String text) {
        return new PlainText.Snippet(Tree.randomId(), Markers.EMPTY, text);
    }

}
