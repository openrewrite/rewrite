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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.binary.Binary;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.quark.Quark;
import org.openrewrite.remote.Remote;

@Value
@EqualsAndHashCode(callSuper = true)
public class FindAndReplace extends Recipe {

    @Option(displayName = "Find",
            description = "The text to find (and replace).",
            example = "blacklist")
    String find;

    @Option(displayName = "Replace",
            description = "The replacement text for `find`.",
            example = "denylist")
    String replace;

    @Option(displayName = "Regex",
            description = "Default false. If true, `find` will be interpreted as a Regular Expression, and capture group contents will be available in `replace`.",
            required = false)
    @Nullable
    Boolean regex;

    /**
     * @deprecated Use {@link Recipe#addSingleSourceApplicableTest(TreeVisitor)} instead.
     */
    @SuppressWarnings("DeprecatedIsStillUsed")
    @Option(displayName = "Optional file Matcher",
            description = "Matching files will be modified. This is a glob expression.",
            example = "foo/bar/baz.txt",
            required = false)
    @Nullable
    @Deprecated
    String fileMatcher;

    @Override
    public String getDisplayName() {
        return "Find and replace";
    }

    @Override
    public String getDescription() {
        return "Simple text find and replace. When the original source file is a language-specific Lossless Semantic Tree, this operation " +
               "irreversibly converts the source file to a plain text file. Subsequent recipes will not be able to operate on language-specific type.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        return new HasSourcePath<>(fileMatcher);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visitSourceFile(SourceFile sourceFile, ExecutionContext executionContext) {
                if(sourceFile instanceof Quark || sourceFile instanceof Remote || sourceFile instanceof Binary) {
                    return sourceFile;
                }
                PlainText text = PlainTextParser.convert(sourceFile);
                String newText = Boolean.TRUE.equals(regex) ?
                        text.getText().replaceAll(find, replace) :
                        text.getText().replace(find, replace);
                return text.getText().equals(newText) ? text : text.withText(newText);
            }
        };
    }
}
