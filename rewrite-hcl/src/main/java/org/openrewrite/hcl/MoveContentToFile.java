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

    @Override
    public String getDisplayName() {
        return "Move content to another file";
    }

    @Override
    public String getDescription() {
        return "Move content to another HCL file, deleting it in the original file.";
    }

    static class Scanned {
        @Nullable
        BodyContent toMove;

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
                    acc.toMove = bodyContent;
                    //noinspection ConstantConditions
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
        Hcl.ConfigFile configFile = HclParser.builder().build().parse("")
                .findFirst()
                .map(Hcl.ConfigFile.class::cast)
                .orElseThrow(() -> new IllegalArgumentException("Could not parse as HCL"));
        configFile = configFile.withBody(singletonList(acc.toMove.withPrefix(Space.EMPTY)))
                .withSourcePath(Paths.get(destinationPath));
        return singletonList(configFile);
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
                    return acc.toMove;
                }
                return b;
            }
        });
    }
}
