/*
 * Copyright 2022 the original author or authors.
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

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.binary.Binary;
import org.openrewrite.internal.lang.NonNull;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.AlreadyReplaced;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.quark.Quark;
import org.openrewrite.remote.Remote;

import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;
import static org.openrewrite.Tree.randomId;

@Value
@EqualsAndHashCode(callSuper = false)
public class FindAndReplace extends Recipe {

    @Override
    public String getDisplayName() {
        return "Find and replace";
    }

    @Override
    public String getDescription() {
        return "Textual find and replace, optionally interpreting the search query as a Regular Expression (regex). " +
               "When operating on source files that are language-specific Lossless Semantic " +
               "Tree, such as Java or XML, this operation converts the source file to plain text for the rest of the recipe run. " +
               "So if you are combining this recipe with language-specific recipes in a single recipe run put all the language-specific recipes before this recipe.";
    }

    @Option(displayName = "Find",
            description = "The text to find (and replace). This snippet can be multiline.",
            example = "blacklist")
    String find;

    @Option(displayName = "Replace",
            description = "The replacement text for `find`. This snippet can be multiline.",
            example = "denylist",
            required = false)
    @Nullable
    String replace;

    @Option(displayName = "Regex",
            description = "Default false. If true, `find` will be interpreted as a [Regular Expression](https://en.wikipedia.org/wiki/Regular_expression), and capture group contents will be available in `replace`.",
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

    @Option(displayName = "Plaintext only", description = "Only alter files that are parsed as plaintext to prevent language-specific LST information loss. Defaults to false.",
            required = false)
    @Nullable
    Boolean plaintextOnly;

    @Deprecated
    public FindAndReplace(final String find, @Nullable final String replace, @Nullable final Boolean regex,
            @Nullable final Boolean caseSensitive, @Nullable final Boolean multiline, @Nullable final Boolean dotAll,
            @Nullable final String filePattern) {
        this.find = find;
        this.replace = replace;
        this.regex = regex;
        this.caseSensitive = caseSensitive;
        this.multiline = multiline;
        this.dotAll = dotAll;
        this.filePattern = filePattern;
        this.plaintextOnly = null;
    }

    @JsonCreator
    public FindAndReplace(final String find, @Nullable final String replace, @Nullable final Boolean regex,
            @Nullable final Boolean caseSensitive, @Nullable final Boolean multiline, @Nullable final Boolean dotAll,
            @Nullable final String filePattern, @Nullable final Boolean plaintextOnly) {
        this.find = find;
        this.replace = replace;
        this.regex = regex;
        this.caseSensitive = caseSensitive;
        this.multiline = multiline;
        this.dotAll = dotAll;
        this.filePattern = filePattern;
        this.plaintextOnly = plaintextOnly;
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
                for (Marker marker : sourceFile.getMarkers().getMarkers()) {
                    if (marker instanceof AlreadyReplaced) {
                        AlreadyReplaced alreadyReplaced = (AlreadyReplaced) marker;
                        if (Objects.equals(find, alreadyReplaced.getFind()) && Objects.equals(replace, alreadyReplaced.getReplace())) {
                            return sourceFile;
                        }
                    }
                }
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
                PlainText plainText = PlainTextParser.convert(sourceFile);
                Pattern pattern = Pattern.compile(searchStr, patternOptions);
                Matcher matcher = pattern.matcher(plainText.getText());

                if (!matcher.find()) {
                    return sourceFile;
                }
                String replacement = replace == null ? "" : replace;
                if (!Boolean.TRUE.equals(regex)) {
                    replacement = replacement.replace("$", "\\$");
                }
                String newText = matcher.replaceAll(replacement);
                return plainText.withText(newText)
                        .withMarkers(sourceFile.getMarkers().add(new AlreadyReplaced(randomId(), find, replace)));
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

        if (Boolean.TRUE.equals(plaintextOnly)) {
            visitor = Preconditions.check(new PlainTextVisitor<ExecutionContext>(){
                @Override
                public @NonNull PlainText visitText(@NonNull PlainText text, @NonNull ExecutionContext ctx) {
                    return SearchResult.found(text);
                }
            }, visitor);
        }
        return visitor;
    }

}
