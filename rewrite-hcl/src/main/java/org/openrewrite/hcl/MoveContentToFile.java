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
package org.openrewrite.hcl;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.hcl.tree.BodyContent;
import org.openrewrite.hcl.tree.Hcl;
import org.openrewrite.hcl.tree.Space;
import org.openrewrite.internal.ListUtils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Value
@EqualsAndHashCode(callSuper = false)
public class MoveContentToFile extends Recipe {
    @Option(
            displayName = "Content path",
            description = "A JSONPath expression specifying the block to delete.",
            example = "$.provider"
    )
    String contentPath;

    @Option(
            displayName = "From path",
            description = "The source path of the file from which content is being moved.",
            example = "from.tf"
    )
    String fromPath;

    @Option(
            displayName = "To path",
            description = "The source path of the file to move the content to.",
            example = "to.tf"
    )
    String destinationPath;

    @Override
    public String getDisplayName() {
        return "Move content to another file";
    }

    @Override
    public String getDescription() {
        return "Move content to another HCL file, deleting it in the original file.";
    }

    @Override
    protected List<SourceFile> visit(List<SourceFile> before, ExecutionContext ctx) {
        AtomicReference<BodyContent> toMove = new AtomicReference<>();
        JsonPathMatcher pathMatcher = new JsonPathMatcher(contentPath);

        Path from = Paths.get(fromPath);
        List<SourceFile> after = ListUtils.map(before, sourceFile ->
                sourceFile.getSourcePath().equals(from) ?
                        (SourceFile) new HclIsoVisitor<ExecutionContext>() {
                            @Override
                            public BodyContent visitBodyContent(BodyContent bodyContent, ExecutionContext ctx) {
                                BodyContent b = super.visitBodyContent(bodyContent, ctx);
                                if (pathMatcher.matches(getCursor())) {
                                    toMove.set(bodyContent);
                                    //noinspection ConstantConditions
                                    return null;
                                }
                                return b;
                            }
                        }.visit(sourceFile, ctx) : sourceFile
        );

        if (toMove.get() == null) {
            return before;
        }

        Path dest = Paths.get(destinationPath);
        if (after.stream().anyMatch(sourceFile -> sourceFile.getSourcePath().equals(dest))) {
            return ListUtils.map(after, sourceFile -> {
                if (sourceFile.getSourcePath().equals(dest)) {
                    return (SourceFile) new HclIsoVisitor<ExecutionContext>() {
                        @Override
                        public BodyContent visitBodyContent(BodyContent bodyContent, ExecutionContext ctx) {
                            BodyContent b = super.visitBodyContent(bodyContent, ctx);
                            if (pathMatcher.matches(getCursor())) {
                                return toMove.get();
                            }
                            return b;
                        }
                    }.visit(sourceFile, ctx);
                }
                return sourceFile;
            });
        }

        Hcl.ConfigFile configFile = HclParser.builder().build().parse("").get(0);
        configFile = configFile.withBody(Collections.singletonList(toMove.get().withPrefix(Space.EMPTY)))
                .withSourcePath(dest);
        return ListUtils.concat(after, configFile);
    }
}
