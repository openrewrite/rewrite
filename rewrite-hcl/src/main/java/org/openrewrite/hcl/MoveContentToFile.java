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
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.hcl.tree.BodyContent;
import org.openrewrite.hcl.tree.Hcl;
import org.openrewrite.hcl.tree.Space;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

@Value
@EqualsAndHashCode(callSuper = false)
public class MoveContentToFile extends ScanningRecipe<MoveContentToFile.Scanned> {
    @Option(displayName = "Content path",
            description = "A JSONPath expression specifying the block to move.",
            example = "$.provider")
    String contentPath;

    @Option(displayName = "From path",
            description = "The source path of the file from which content is being moved.",
            example = "from.tf")
    String fromPath;

    @Option(displayName = "To path",
            description = "The source path of the file to move the content to.",
            example = "to.tf")
    String destinationPath;

    String displayName = "Move content to another file";

    String description = "Move content to another HCL file, deleting it in the original file.";

    static class Scanned {
        /**
         * The block to move, printed without the whitespace before it.
         */
        @Nullable
        String toMove;

        Space toMovePrefix = Space.EMPTY;

        boolean destinationExists;
    }

    @Override
    public Scanned getInitialValue(ExecutionContext ctx) {
        return new Scanned();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Scanned acc) {
        JsonPathMatcher pathMatcher = new JsonPathMatcher(contentPath);
        Path from = Paths.get(fromPath);
        Path dest = Paths.get(destinationPath);

        return new HclIsoVisitor<ExecutionContext>() {
            @Override
            public Hcl.ConfigFile visitConfigFile(Hcl.ConfigFile configFile, ExecutionContext ctx) {
                if (configFile.getSourcePath().equals(from)) {
                    return super.visitConfigFile(configFile, ctx);
                } else if (configFile.getSourcePath().equals(dest)) {
                    acc.destinationExists = true;
                }
                return configFile;
            }

            @Override
            public BodyContent visitBodyContent(BodyContent bodyContent, ExecutionContext ctx) {
                BodyContent b = super.visitBodyContent(bodyContent, ctx);
                if (pathMatcher.matches(getCursor())) {
                    acc.toMove = bodyContent.withPrefix(Space.EMPTY).print(getCursor().getParentOrThrow());
                    acc.toMovePrefix = bodyContent.getPrefix();
                }
                return b;
            }
        };
    }

    @Override
    public Collection<Hcl.ConfigFile> generate(Scanned acc, ExecutionContext ctx) {
        if (acc.destinationExists || acc.toMove == null) {
            return emptyList();
        }
        return singletonList(parse(acc.toMove).withSourcePath(Paths.get(destinationPath)));
    }

    private static Hcl.ConfigFile parse(String hcl) {
        return HclParser.builder().build().parse(hcl)
                .findFirst()
                .map(Hcl.ConfigFile.class::cast)
                .orElseThrow(() -> new IllegalArgumentException("Could not parse as HCL"));
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Scanned acc) {
        JsonPathMatcher pathMatcher = new JsonPathMatcher(contentPath);
        Path from = Paths.get(fromPath);
        Path dest = Paths.get(destinationPath);

        return Preconditions.check(acc.toMove != null, new HclIsoVisitor<ExecutionContext>() {

            @Override
            public @Nullable BodyContent visitBodyContent(BodyContent bodyContent, ExecutionContext ctx) {
                BodyContent b = super.visitBodyContent(bodyContent, ctx);
                Path sourcePath = getCursor().firstEnclosingOrThrow(Hcl.ConfigFile.class).getSourcePath();
                if (sourcePath.equals(from) && pathMatcher.matches(getCursor())) {
                    // delete the block from the original file
                    //noinspection ConstantConditions
                    return null;
                } else if (sourcePath.equals(dest) && pathMatcher.matches(getCursor())) {
                    return parse(requireNonNull(acc.toMove)).getBody().get(0).withPrefix(acc.toMovePrefix);
                }
                return b;
            }
        });
    }
}
