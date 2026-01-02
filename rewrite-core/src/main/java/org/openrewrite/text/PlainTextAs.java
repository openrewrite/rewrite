/*
 * Copyright 2025 the original author or authors.
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

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;

/**
 * Abstract base recipe for converting plain text files matching a pattern into a specific typed source file.
 * <p>
 * <strong>When to use this recipe:</strong>
 * This recipe is designed for files with proprietary or non-standard extensions/names that are not
 * automatically recognized by OpenRewrite's default parsers. For files with standard extensions
 * (e.g., {@code *.xml}, {@code *.yaml}, {@code *.properties}, {@code *.groovy}), configure the
 * appropriate parser during LST build time instead.
 * <p>
 * <strong>Example use cases:</strong>
 * <ul>
 *   <li>Files named {@code Jenkinsfile} (no extension) that contain Groovy code</li>
 *   <li>Proprietary config files like {@code *.config} or {@code *.dat} that are actually XML</li>
 *   <li>Custom extensions like {@code *.myapp} that contain YAML content</li>
 * </ul>
 * <p>
 * The file pattern is required because this recipe specifically targets files that would otherwise
 * be parsed as plain text due to their non-standard naming.
 *
 * @param <T> The type of source file to convert to
 */
@RequiredArgsConstructor
public abstract class PlainTextAs<T extends SourceFile> extends Recipe {

    /**
     * @return The file pattern (glob) to match against source paths. This is required.
     */
    protected abstract String getFilePattern();

    /**
     * @return The parser builder to use for converting plain text to the target type
     */
    protected abstract Parser.Builder getParserBuilder();

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        String paths = getFilePattern();
        Parser parser = getParserBuilder().build();

        return Preconditions.check(new FindSourceFiles(paths), new PlainTextVisitor<ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof PlainText) {
                    PlainText pt = (PlainText) tree;
                    return parser.parse(pt.getText())
                            .findFirst()
                            .map(sourceFile -> sourceFile
                                    .<SourceFile>withId(pt.getId())
                                    .<SourceFile>withMarkers(pt.getMarkers())
                                    .<SourceFile>withSourcePath(pt.getSourcePath()))
                            .orElse(pt);
                }
                return super.visit(tree, ctx);
            }
        });
    }
}
