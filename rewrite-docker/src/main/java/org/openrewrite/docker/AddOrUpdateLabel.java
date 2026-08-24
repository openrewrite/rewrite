/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.docker;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.docker.tree.Docker;
import org.openrewrite.docker.tree.Space;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.marker.Markers;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Collections.singletonList;
import static org.openrewrite.Tree.randomId;

@Value
@EqualsAndHashCode(callSuper = false)
public class AddOrUpdateLabel extends Recipe {

    @Option(displayName = "Label key",
            description = "The key of the label to add.",
            example = "org.opencontainers.image.version")
    String key;

    @Option(displayName = "Label value",
            description = "The value of the label.",
            example = "1.0.0")
    String value;

    @Option(displayName = "Overwrite existing",
            description = "If true, overwrite the label if it already exists. If false, skip if exists. Defaults to true.",
            required = false)
    @Nullable
    Boolean overwriteExisting;

    @Option(displayName = "Stage name",
            description = "Only add the label to this build stage. If null, adds to the final stage only.",
            example = "final",
            required = false)
    @Nullable
    String stageName;

    @Override
    public String getDisplayName() {
        return "Add Docker LABEL instruction";
    }

    @Override
    public String getDescription() {
        return "Adds or updates a LABEL instruction in a Dockerfile. By default, adds to the final stage only.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.or(new UpdateLabelVisitor(), new AddLabelVisitor());
    }

    /**
     * Only update existing labels.
     */
    private class UpdateLabelVisitor extends DockerIsoVisitor<ExecutionContext> {
        @Override
        public Docker.Stage visitStage(Docker.Stage stage, ExecutionContext ctx) {
            if (stageName != null && !stageName.equals(stage.getFrom().getAs() != null ? stage.getFrom().getAs().getName().getText() : null)) {
                return stage; // Skip this stage
            }
            return super.visitStage(stage, ctx); // Update existing labels anywhere
        }

        @Override
        public Docker.Label.LabelPair visitLabelPair(Docker.Label.LabelPair pair, ExecutionContext ctx) {
            if (key.equals(extractText(pair.getKey()))) {
                boolean shouldOverwrite = overwriteExisting == null || overwriteExisting;
                return shouldOverwrite && !value.equals(extractText(pair.getValue())) ?
                        pair.withValue(createArgument(value, pair.getValue())) : pair;
            }
            return super.visitLabelPair(pair, ctx);
        }
    }

    /**
     * Only add new labels.
     */
    private class AddLabelVisitor extends DockerIsoVisitor<ExecutionContext> {
        @Override
        public Docker.Stage visitStage(Docker.Stage stage, ExecutionContext ctx) {
            if (stageName == null) { // Only modify final stage by default
                List<Docker.Stage> stages = getCursor().getParentTreeCursor().<Docker.File>getValue().getStages();
                return stage == stages.get(stages.size() - 1) ? addNewLabel(stage) : stage;
            }
            Docker.From.As as = stage.getFrom().getAs();
            return stageName.equals(as != null ? as.getName().getText() : null) ? addNewLabel(stage) : stage;
        }

        private Docker.Stage addNewLabel(Docker.Stage stage) {
            if (hasLabel(stage)) {
                return stage; // Prevent adding duplicate label
            }
            return stage.withInstructions(ListUtils.insert(
                    stage.getInstructions(),
                    createLabel(),
                    findLabelInsertPosition(stage)));
        }

        private boolean hasLabel(Docker.Stage stage) {
            return new DockerIsoVisitor<AtomicBoolean>() {
                @Override
                public Docker.Label.LabelPair visitLabelPair(Docker.Label.LabelPair pair, AtomicBoolean matchFound) {
                    if (!matchFound.get() && key.equals(extractText(pair.getKey()))) {
                        matchFound.set(true);
                    }
                    return pair;
                }
            }.reduce(stage, new AtomicBoolean(false)).get();
        }

        private Docker.Label createLabel() {
            Docker.Label.LabelPair pair = new Docker.Label.LabelPair(
                    Tree.randomId(),
                    Space.SINGLE_SPACE,
                    Markers.EMPTY,
                    createArgument(key, null),
                    true,  // hasEquals - use modern format with equals sign
                    createArgument(value, null)
            );
            return new Docker.Label(randomId(), Space.format("\n"), Markers.EMPTY, "LABEL", singletonList(pair));
        }

        private int findLabelInsertPosition(Docker.Stage stage) {
            int lastLabelIndex = -1; // Insert at start if no labels found
            for (int i = 0; i < stage.getInstructions().size(); i++) {
                Docker.Instruction inst = stage.getInstructions().get(i);
                if (inst instanceof Docker.Label) {
                    lastLabelIndex = i; // After any existing labels
                }
            }
            return lastLabelIndex + 1;
        }
    }

    private static @Nullable String extractText(Docker.@Nullable Argument arg) {
        if (arg == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        for (Docker.ArgumentContent content : arg.getContents()) {
            if (content instanceof Docker.Literal) {
                builder.append(((Docker.Literal) content).getText());
            } else if (content instanceof Docker.EnvironmentVariable) {
                Docker.EnvironmentVariable env = (Docker.EnvironmentVariable) content;
                // Include the variable reference as-is (e.g., ${VAR} or $VAR)
                if (env.isBraced()) {
                    builder.append("${").append(env.getName()).append("}");
                } else {
                    builder.append("$").append(env.getName());
                }
            }
        }
        return builder.toString();
    }

    private static Docker.Argument createArgument(String text, Docker.@Nullable Argument original) {
        // Quote if contains spaces or special characters
        boolean needsQuotes = text.contains(" ") || text.contains("=");
        Docker.ArgumentContent content;
        if (needsQuotes) {
            // Preserve quote style from original if available
            Docker.Literal.QuoteStyle quoteStyle = Docker.Literal.QuoteStyle.DOUBLE;
            if (original != null) {
                for (Docker.ArgumentContent c : original.getContents()) {
                    if (c instanceof Docker.Literal && ((Docker.Literal) c).isQuoted()) {
                        quoteStyle = ((Docker.Literal) c).getQuoteStyle();
                        break;
                    }
                }
            }
            content = new Docker.Literal(randomId(), Space.EMPTY, Markers.EMPTY, text, quoteStyle);
        } else {
            content = new Docker.Literal(randomId(), Space.EMPTY, Markers.EMPTY, text, null);
        }
        return new Docker.Argument(randomId(), Space.EMPTY, Markers.EMPTY, singletonList(content));
    }
}
