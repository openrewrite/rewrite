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
import org.openrewrite.quark.Quark;
import org.openrewrite.remote.Remote;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Value
@EqualsAndHashCode(callSuper = true)
public class Find extends Recipe {

    private static final String SR_PREFIX = "~~>(";
    private static final int SR_LENGTH = SR_PREFIX.length();

    @Override
    public String getDisplayName() {
        return "Find text";
    }

    @Override
    public String getDescription() {
        return "Search for text in a plain text file. Wraps the results in \"~~>( )\".";
    }

    @Option(displayName = "Find",
            description = "The text to find.",
            example = "blacklist")
    String find;

    @Option(displayName = "Regex",
            description = "Default false. If true, `find` will be interpreted as a Regular Expression.",
            required = false)
    @Nullable
    Boolean regex;

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree visitSourceFile(SourceFile sourceFile, ExecutionContext executionContext) {
                if(sourceFile instanceof Quark || sourceFile instanceof Remote || sourceFile instanceof Binary) {
                    return sourceFile;
                }
                PlainText plainText = PlainTextParser.convert(sourceFile);
                String searchStr = find;
                if(!Boolean.TRUE.equals(regex)) {
                    searchStr = Pattern.quote(searchStr);
                }
                Pattern pattern = Pattern.compile(searchStr);
                Matcher matcher = pattern.matcher(plainText.getText());
                String rawText = plainText.getText();
                StringBuilder result = new StringBuilder();
                boolean anyFound = false;
                int previousEnd = 0;
                while(matcher.find()) {
                    anyFound = true;
                    int matchStart = matcher.start();
                    result.append(rawText, previousEnd, matchStart);
                    if(matchStart >= SR_LENGTH && rawText.substring(matchStart - SR_LENGTH, matchStart).equals(SR_PREFIX)) {
                        result.append(rawText, matchStart, matcher.end());
                    } else {
                        result.append(SR_PREFIX).append(rawText, matchStart, matcher.end()).append(")");
                    }
                    previousEnd = matcher.end();
                }
                if(anyFound) {
                    result.append(rawText.substring(previousEnd));
                    String newText = result.toString();
                    if(!newText.equals(rawText)) {
                        plainText = plainText.withText(newText);
                    }
                    return plainText;
                }

                return sourceFile;
            }
        };
    }

}
