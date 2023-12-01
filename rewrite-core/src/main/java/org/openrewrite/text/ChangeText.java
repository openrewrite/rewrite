/*
 * Copyright 2020 the original author or authors.
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
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.Set;

import static java.util.Collections.emptyList;

@Value
@EqualsAndHashCode(callSuper = false)
public class ChangeText extends Recipe {
    @Option(displayName = "Relative File Name",
            description = "File name, using a relative path. If a non-plaintext file already exists at this location, then this recipe will do nothing.",
            example = "foo/bar/baz.txt",
            required = false)
    String relativeFileName;

    @Option(displayName = "Text after change",
            description = "The text file will have only this text after the change.",
            example = "Some text.")
    String toText;

    @Override
    public Set<String> getTags() {
        return Collections.singleton("plain text");
    }

    @Override
    public String getDisplayName() {
        return "Change text";
    }

    @Override
    public String getDescription() {
        return "Completely replaces the contents of the text file with other text.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new PlainTextVisitor<ExecutionContext>() {
            @Override
            public PlainText visitText(PlainText text, ExecutionContext ctx) {
                if (relativeFileName == null || text.getSourcePath().equals(Paths.get(relativeFileName))) {
                    return text
                            .withSnippets(emptyList())
                            .withText(toText);
                }
                return super.visitText(text, ctx);
            }
        };
    }
}
