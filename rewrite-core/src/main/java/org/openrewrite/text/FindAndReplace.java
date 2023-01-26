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
import org.openrewrite.ExecutionContext;
import org.openrewrite.HasSourcePath;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;

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
    @Option(displayName = "Optional file Matcher",
            description = "Matching files will be modified. This is a glob expression.",
            example = "foo/bar/baz.txt",
            required = false)
    @Nullable
    String fileMatcher;

    @Override
    public String getDisplayName() {
        return "Find and Replace";
    }

    @Override
    public String getDescription() {
        return "Replaces content inside a plaintext file. Will not affect files which are parsed as a more-specific type (eg .yml, .java).";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        return new HasSourcePath<>(fileMatcher);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new PlainTextVisitor<ExecutionContext>() {
            @Override
            public PlainText visitText(PlainText text, ExecutionContext executionContext) {
                String newText = Boolean.TRUE.equals(regex)
                        ? text.getText().replaceAll(find, replace)
                        : text.getText().replace(find, replace);
                return text.getText().equals(newText) ? text : text.withText(newText);
            }
        };
    }
}
