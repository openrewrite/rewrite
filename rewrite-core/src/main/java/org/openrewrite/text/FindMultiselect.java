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
import org.openrewrite.*;
import org.openrewrite.binary.Binary;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Markers;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.quark.Quark;
import org.openrewrite.remote.Remote;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toSet;

@Incubating(since = "8.2.0")
@Value
@EqualsAndHashCode(callSuper = false)
public class FindMultiselect extends Recipe {

    @Override
    public String getDisplayName() {
        return "Experimental find text with multiselect";
    }

    @Override
    public String getDescription() {
        return "Search for text, treating all textual sources as plain text. " +
               "This version of the recipe exists to experiment with multiselect recipe options.";
    }

    @Option(displayName = "Find",
            description = "The text to find. This snippet can be multiline.",
            example = "blacklist")
    String find;

    @Option(displayName = "Regex",
            description = "If true, `find` will be interpreted as a Regular Expression. Default `false`.",
            required = false)
    @Nullable
    Boolean regex;

    @Option(displayName = "Regex options",
            description = "Regex processing options. Multiple options may be specified. These options do nothing if `regex` mode is not enabled.\n" +
                          "* Case-sensitive - The search will be sensitive to letter case. " +
                          "* Multiline - Allows `^` and `$` to match the beginning and end of lines, respectively." +
                          "* Dot all - Allows `.` to match line terminators.",
            valid = {"Case-sensitive", "Multiline", "Dot all"},
            required = false)
    @Nullable
    Set<String> regexOptions;

    @Option(displayName = "File pattern",
            description = "A glob expression that can be used to constrain which directories or source files should be searched. " +
                          "Multiple patterns may be specified, separated by a semicolon `;`. " +
                          "If multiple patterns are supplied any of the patterns matching will be interpreted as a match. " +
                          "When not set, all source files are searched. ",
            example = "**/*.java")
    @Nullable
    String filePattern;

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        Boolean caseSensitive;
        Boolean multiline;
        Boolean dotAll;
        if (regexOptions != null) {
            Set<String> lowerCaseOptions = regexOptions.stream()
                    .map(String::toLowerCase)
                    .collect(toSet());
            caseSensitive = lowerCaseOptions.contains("Case-sensitive");
            multiline = lowerCaseOptions.contains("Multiline");
            dotAll = lowerCaseOptions.contains("Dot all");
        } else {
            caseSensitive = null;
            multiline = null;
            dotAll = null;
        }

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
                List<PlainText.Snippet> snippets = new ArrayList<>();
                int previousEnd = 0;
                while (matcher.find()) {
                    int matchStart = matcher.start();
                    snippets.add(snippet(rawText.substring(previousEnd, matchStart)));
                    snippets.add(SearchResult.found(snippet(rawText.substring(matchStart, matcher.end()))));
                    previousEnd = matcher.end();
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
