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
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;

@Value
@EqualsAndHashCode(callSuper = true)
public class Find extends Recipe {

    @Override
    public String getDisplayName() {
        return "Find text";
    }

    @Override
    public String getDescription() {
        return "Search for text, treating all textual sources as plain text.";
    }

    @Option(displayName = "Find",
            description = "The text to find.",
            example = "blacklist")
    String find;

    @Option(displayName = "Regex",
            description = "If true, `find` will be interpreted as a Regular Expression. Default false.",
            required = false)
    @Nullable
    Boolean regex;

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new TreeVisitor<Tree, ExecutionContext>() {
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
                Pattern pattern = Pattern.compile(searchStr);
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
    }


    private static PlainText.Snippet snippet(String text) {
        return new PlainText.Snippet(Tree.randomId(), Markers.EMPTY, text);
    }
}
