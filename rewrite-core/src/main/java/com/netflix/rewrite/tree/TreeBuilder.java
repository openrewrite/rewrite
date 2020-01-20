/*
 * Copyright 2020 the original authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.rewrite.tree;

import com.netflix.rewrite.internal.lang.NonNullApi;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.netflix.rewrite.tree.Formatting.format;
import static com.netflix.rewrite.tree.Tr.randomId;

@NonNullApi
public class TreeBuilder {
    private static final Pattern whitespacePrefixPattern = Pattern.compile("^\\s*");
    private static final Pattern whitespaceSuffixPattern = Pattern.compile("\\s*[^\\s]+(\\s*)");

    private TreeBuilder() {
    }

    public static NameTree buildName(String fullyQualifiedName) {
        return buildName(fullyQualifiedName, Formatting.EMPTY);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static NameTree buildName(String fullyQualifiedName, Formatting fmt) {
        String[] parts = fullyQualifiedName.split("\\.");

        String fullName = "";
        Expression expr = null;
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (i == 0) {
                fullName = part;
                expr = Tr.Ident.build(randomId(), part, null, Formatting.EMPTY);
            } else {
                fullName += "." + part;

                Matcher whitespacePrefix = whitespacePrefixPattern.matcher(part);
                var identFmt = whitespacePrefix.matches() ? format(whitespacePrefix.group(0)) : Formatting.EMPTY;

                Matcher whitespaceSuffix = whitespaceSuffixPattern.matcher(part);
                whitespaceSuffix.matches();
                var partFmt = i == parts.length - 1 ? Formatting.EMPTY : format("", whitespaceSuffix.group(1));

                expr = new Tr.FieldAccess(randomId(),
                        expr,
                        Tr.Ident.build(randomId(), part.trim(), null, identFmt),
                        (Character.isUpperCase(part.charAt(0)) || i == parts.length - 1) ?
                                Type.Class.build(fullName) :
                                null,
                        partFmt
                );
            }
        }

        //noinspection ConstantConditions
        return expr.withFormatting(fmt);
    }
}
