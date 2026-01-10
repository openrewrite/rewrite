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
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.docker.tree.Docker;
import org.openrewrite.docker.tree.Space;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.marker.Markers;

import java.util.ArrayList;
import java.util.List;

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
            description = "If true, overwrite the label if it already exists. If false, skip if exists.",
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
        return "Adds or updates a LABEL instruction in a Dockerfile. " +
               "By default, adds to the final stage only.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        boolean shouldOverwrite = overwriteExisting == null || overwriteExisting;

        return new DockerIsoVisitor<ExecutionContext>() {

            @Override
            public Docker.File visitFile(Docker.File file, ExecutionContext ctx) {
                Docker.File f = super.visitFile(file, ctx);

                if (f.getStages().isEmpty()) {
                    return f;
                }

                // Determine which stages to modify
                List<Docker.Stage> newStages = new ArrayList<>(f.getStages());
                boolean modified = false;

                for (int i = 0; i < newStages.size(); i++) {
                    Docker.Stage stage = newStages.get(i);
                    boolean isFinalStage = (i == newStages.size() - 1);

                    // Check if we should target this stage
                    if (stageName != null) {
                        String currentStageName = getStageName(stage);
                        if (!stageName.equals(currentStageName)) {
                            continue; // Skip this stage
                        }
                    } else if (!isFinalStage) {
                        // By default, only modify final stage
                        continue;
                    }

                    Docker.Stage modifiedStage = addOrUpdateLabel(stage, shouldOverwrite);
                    if (modifiedStage != stage) {
                        newStages.set(i, modifiedStage);
                        modified = true;
                    }
                }

                if (modified) {
                    return f.withStages(newStages);
                }
                return f;
            }

            private @Nullable String getStageName(Docker.Stage stage) {
                if (stage.getFrom().getAs() != null) {
                    return extractText(stage.getFrom().getAs().getName());
                }
                return null;
            }

            private Docker.Stage addOrUpdateLabel(Docker.Stage stage, boolean shouldOverwrite) {
                // Look for existing LABEL instruction with this key
                Docker.Label existingLabel = null;
                int existingLabelIndex = -1;
                String existingValue = null;

                for (int i = 0; i < stage.getInstructions().size(); i++) {
                    Docker.Instruction inst = stage.getInstructions().get(i);
                    if (inst instanceof Docker.Label) {
                        Docker.Label label = (Docker.Label) inst;
                        for (Docker.Label.LabelPair pair : label.getPairs()) {
                            String pairKey = extractText(pair.getKey());
                            if (key.equals(pairKey)) {
                                existingLabel = label;
                                existingLabelIndex = i;
                                existingValue = extractText(pair.getValue());
                                break;
                            }
                        }
                    }
                    if (existingLabel != null) {
                        break;
                    }
                }

                if (existingLabel != null) {
                    // Label exists - check if value already matches
                    if (value.equals(existingValue)) {
                        return stage; // Already has the correct value, no change needed
                    }
                    if (!shouldOverwrite) {
                        return stage; // Skip, label already exists with different value
                    }
                    // Update existing label
                    return updateExistingLabel(stage, existingLabel, existingLabelIndex);
                } else {
                    // Add new LABEL instruction
                    return addNewLabel(stage);
                }
            }

            private Docker.Stage updateExistingLabel(Docker.Stage stage,
                                                      Docker.Label existingLabel,
                                                      int index) {
                // Find and update the specific pair
                List<Docker.Label.LabelPair> newPairs = new ArrayList<>();

                for (Docker.Label.LabelPair pair : existingLabel.getPairs()) {
                    String pairKey = extractText(pair.getKey());
                    if (key.equals(pairKey)) {
                        // Update the value
                        Docker.Argument newValue = createArgument(value, pair.getValue());
                        newPairs.add(pair.withValue(newValue));
                    } else {
                        newPairs.add(pair);
                    }
                }

                Docker.Label updatedLabel = existingLabel.withPairs(newPairs);

                List<Docker.Instruction> newInstructions = new ArrayList<>(stage.getInstructions());
                newInstructions.set(index, updatedLabel);

                return stage.withInstructions(newInstructions);
            }

            private Docker.Stage addNewLabel(Docker.Stage stage) {
                // Create new LABEL instruction
                Docker.Label.LabelPair pair = createLabelPair(key, value, Space.SINGLE_SPACE);

                Docker.Label newLabel = new Docker.Label(
                        randomId(),
                        Space.format("\n"),
                        Markers.EMPTY,
                        "LABEL",
                        singletonList(pair)
                );

                // Find the best position to insert (after FROM and other LABELs, before CMD/ENTRYPOINT)
                int insertIndex = findLabelInsertPosition(stage);

                List<Docker.Instruction> newInstructions = ListUtils.insert(
                        stage.getInstructions(), newLabel, insertIndex);

                return stage.withInstructions(newInstructions);
            }

            private int findLabelInsertPosition(Docker.Stage stage) {
                // Insert after other LABEL instructions, or at beginning, before CMD/ENTRYPOINT
                int lastLabelIndex = -1;
                int cmdEntrypointIndex = stage.getInstructions().size();

                for (int i = 0; i < stage.getInstructions().size(); i++) {
                    Docker.Instruction inst = stage.getInstructions().get(i);
                    if (inst instanceof Docker.Label) {
                        lastLabelIndex = i;
                    } else if (inst instanceof Docker.Cmd || inst instanceof Docker.Entrypoint) {
                        cmdEntrypointIndex = i;
                        break;
                    }
                }

                // Prefer after last LABEL, then at beginning but before CMD
                if (lastLabelIndex >= 0) {
                    return lastLabelIndex + 1;
                }
                return Math.min(0, cmdEntrypointIndex);
            }

            private Docker.Label.LabelPair createLabelPair(String labelKey, String labelValue, Space prefix) {
                return new Docker.Label.LabelPair(
                        randomId(),
                        prefix,
                        Markers.EMPTY,
                        createArgument(labelKey, null),
                        true,  // hasEquals - use modern format with equals sign
                        createArgument(labelValue, null)
                );
            }

            private Docker.Argument createArgument(String text, Docker.@Nullable Argument original) {
                // Quote if contains spaces or special characters
                boolean needsQuotes = text.contains(" ") || text.contains("=");
                Docker.ArgumentContent content;

                if (needsQuotes) {
                    // Preserve quote style from original if available
                    Docker.QuotedString.QuoteStyle quoteStyle = Docker.QuotedString.QuoteStyle.DOUBLE;
                    if (original != null) {
                        for (Docker.ArgumentContent c : original.getContents()) {
                            if (c instanceof Docker.QuotedString) {
                                quoteStyle = ((Docker.QuotedString) c).getQuoteStyle();
                                break;
                            }
                        }
                    }
                    content = new Docker.QuotedString(
                            randomId(),
                            Space.EMPTY,
                            Markers.EMPTY,
                            text,
                            quoteStyle
                    );
                } else {
                    content = new Docker.PlainText(
                            randomId(),
                            Space.EMPTY,
                            Markers.EMPTY,
                            text
                    );
                }

                return new Docker.Argument(
                        randomId(),
                        Space.EMPTY,
                        Markers.EMPTY,
                        singletonList(content)
                );
            }

            private @Nullable String extractText(Docker.@Nullable Argument arg) {
                if (arg == null) {
                    return null;
                }
                StringBuilder builder = new StringBuilder();
                for (Docker.ArgumentContent content : arg.getContents()) {
                    if (content instanceof Docker.PlainText) {
                        builder.append(((Docker.PlainText) content).getText());
                    } else if (content instanceof Docker.QuotedString) {
                        builder.append(((Docker.QuotedString) content).getValue());
                    } else if (content instanceof Docker.EnvironmentVariable) {
                        return null;
                    }
                }
                return builder.toString();
            }
        };
    }
}
