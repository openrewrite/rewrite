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
import org.openrewrite.internal.StringUtils;
import org.openrewrite.marker.Markers;

import java.util.List;
import java.util.Objects;

import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static org.openrewrite.Tree.randomId;

@Value
@EqualsAndHashCode(callSuper = false)
public class ChangeBaseImage extends Recipe {

    @Option(displayName = "Old image name",
            description = "The old image name to replace. Supports glob patterns.",
            example = "ubuntu:20.04")
    String oldImageName;

    @Option(displayName = "New image name",
            description = "The new image name to use.",
            example = "ubuntu:22.04")
    String newImageName;

    @Option(displayName = "Old platform",
            description = "Only change images with this platform. If null, matches any platform.",
            example = "linux/amd64",
            required = false)
    @Nullable
    String oldPlatform;

    @Option(displayName = "New platform",
            description = "Set the platform to this value. If null and oldPlatform is specified, removes the platform flag from matched images. If both oldPlatform and newPlatform are null, platform flags are preserved.",
            example = "linux/arm64",
            required = false)
    @Nullable
    String newPlatform;

    @Override
    public String getDisplayName() {
        return "Change Docker base image";
    }

    @Override
    public String getDescription() {
        return "Change the base image in a Dockerfile FROM instruction.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new DockerfileIsoVisitor<ExecutionContext>() {
            @Override
            public Docker.From visitFrom(Docker.From from, ExecutionContext ctx) {
                // Visit children first
                Docker.From f = super.visitFrom(from, ctx);

                // Reconstruct the full image name from imageName, tag, and digest
                StringBuilder imageTextBuilder = new StringBuilder();

                // Add image name
                for (Docker.ArgumentContent content : f.getImageName().getContents()) {
                    if (content instanceof Docker.PlainText) {
                        imageTextBuilder.append(((Docker.PlainText) content).getText());
                    } else if (content instanceof Docker.QuotedString) {
                        imageTextBuilder.append(((Docker.QuotedString) content).getValue());
                    } else if (content instanceof Docker.EnvironmentVariable) {
                        // For environment variables, we can't know the actual value, so skip matching
                        return f;
                    }
                }

                // Add tag or digest
                if (f.getTag() != null) {
                    imageTextBuilder.append(":");
                    for (Docker.ArgumentContent content : f.getTag().getContents()) {
                        if (content instanceof Docker.PlainText) {
                            imageTextBuilder.append(((Docker.PlainText) content).getText());
                        } else if (content instanceof Docker.QuotedString) {
                            imageTextBuilder.append(((Docker.QuotedString) content).getValue());
                        } else if (content instanceof Docker.EnvironmentVariable) {
                            return f;
                        }
                    }
                } else if (f.getDigest() != null) {
                    imageTextBuilder.append("@");
                    for (Docker.ArgumentContent content : f.getDigest().getContents()) {
                        if (content instanceof Docker.PlainText) {
                            imageTextBuilder.append(((Docker.PlainText) content).getText());
                        } else if (content instanceof Docker.QuotedString) {
                            imageTextBuilder.append(((Docker.QuotedString) content).getValue());
                        } else if (content instanceof Docker.EnvironmentVariable) {
                            return f;
                        }
                    }
                }

                String imageText = imageTextBuilder.toString();

                if (!StringUtils.matchesGlob(imageText, oldImageName)) {
                    return f;
                }

                // Get the current platform flag value, if any
                String currentPlatform = getPlatformFlag(f);

                // Check if oldPlatform is specified and matches
                if (oldPlatform != null && !oldPlatform.equals(currentPlatform)) {
                    return f;
                }

                boolean imageChanged = !imageText.equals(newImageName);
                // Only consider platform changed if oldPlatform or newPlatform was explicitly set
                boolean shouldChangePlatform = oldPlatform != null || newPlatform != null;
                boolean platformChanged = shouldChangePlatform && !Objects.equals(currentPlatform, newPlatform);

                if (!imageChanged && !platformChanged) {
                    return f;
                }

                Docker.From result = f;

                // Update platform flag if needed
                if (platformChanged) {
                    result = updatePlatformFlag(result, newPlatform);
                }

                // Update image if needed
                if (imageChanged) {
                    // Check if the original was a single content item (e.g., a single quoted string)
                    boolean wasSingleContent = f.getImageName().getContents().size() == 1 &&
                            f.getTag() == null && f.getDigest() == null;

                    if (wasSingleContent) {
                        // Keep as a single content item (don't split)
                        boolean wasQuoted = hasQuotedString(f.getImageName());
                        Docker.ArgumentContent newContent = createContent(newImageName, wasQuoted, f.getImageName());
                        Docker.Argument newImageArg = f.getImageName().withContents(singletonList(newContent));
                        return result.withImageName(newImageArg);
                    }

                    // Split into components
                    @Nullable String[] parts = parseNewImageName(newImageName);
                    String newImage = requireNonNull(parts[0]);
                    String newTag = parts[1];
                    String newDigest = parts[2];

                    // Check if the original used quotes
                    boolean wasQuoted = hasQuotedString(f.getImageName());

                    // Create new image name argument
                    Docker.ArgumentContent newImageContent = createContent(newImage, wasQuoted, f.getImageName());
                    Docker.Argument newImageArg = f.getImageName().withContents(singletonList(newImageContent));
                    result = result.withImageName(newImageArg);

                    // Create new tag argument if present
                    if (newTag != null) {
                        Docker.ArgumentContent newTagContent = createContent(newTag, wasQuoted, f.getTag());
                        Docker.Argument newTagArg = new Docker.Argument(randomId(), Space.EMPTY, Markers.EMPTY, singletonList(newTagContent));
                        return result.withTag(newTagArg).withDigest(null);
                    }
                    if (newDigest != null) {
                        Docker.ArgumentContent newDigestContent = createContent(newDigest, wasQuoted, f.getDigest());
                        Docker.Argument newDigestArg = new Docker.Argument(randomId(), Space.EMPTY, Markers.EMPTY, singletonList(newDigestContent));
                        return result.withDigest(newDigestArg).withTag(null);
                    }
                    return result.withTag(null).withDigest(null);
                }

                return result;
            }
        };
    }

    private @Nullable String [] parseNewImageName(String fullImageName) {
        String imageName;
        String tag = null;
        String digest = null;

        // Check for digest first (takes precedence)
        int atIndex = fullImageName.indexOf('@');
        if (atIndex > 0) {
            imageName = fullImageName.substring(0, atIndex);
            digest = fullImageName.substring(atIndex + 1);
        } else {
            // Check for tag
            int colonIndex = fullImageName.lastIndexOf(':');
            if (colonIndex > 0) {
                imageName = fullImageName.substring(0, colonIndex);
                tag = fullImageName.substring(colonIndex + 1);
            } else {
                imageName = fullImageName;
            }
        }

        return new @Nullable String[]{imageName, tag, digest};
    }

    private boolean hasQuotedString(Docker.Argument arg) {
        return arg.getContents().stream()
                .anyMatch(content -> content instanceof Docker.QuotedString);
    }

    private Docker.ArgumentContent createContent(String text, boolean quoted, Docker.@Nullable Argument original) {
		if (quoted) {
			// Preserve the quote style from the original
			Docker.QuotedString.QuoteStyle quoteStyle = Docker.QuotedString.QuoteStyle.DOUBLE;
			if (original != null) {
				for (Docker.ArgumentContent content : original.getContents()) {
					if (content instanceof Docker.QuotedString) {
						quoteStyle = ((Docker.QuotedString)content).getQuoteStyle();
						break;
					}
				}
			}
			return new Docker.QuotedString(randomId(), Space.EMPTY, Markers.EMPTY, text, quoteStyle);
		}
		return new Docker.PlainText(randomId(), Space.EMPTY, Markers.EMPTY, text);
	}

    private @Nullable String getPlatformFlag(Docker.From from) {
        if (from.getFlags() == null) {
            return null;
        }

        for (Docker.Flag flag : from.getFlags()) {
            if ("platform".equals(flag.getName()) && flag.getValue() != null) {
                for (Docker.ArgumentContent content : flag.getValue().getContents()) {
                    if (content instanceof Docker.PlainText) {
                        return ((Docker.PlainText) content).getText();
                    }
                    if (content instanceof Docker.QuotedString) {
                        return ((Docker.QuotedString) content).getValue();
                    }
                }
            }
        }
        return null;
    }

    private Docker.From updatePlatformFlag(Docker.From from, @Nullable String platform) {
        List<Docker.Flag> oldFlags = from.getFlags();

        // Update or remove existing platform flag
        List<Docker.Flag> newFlags = ListUtils.map(oldFlags, flag -> {
            if ("platform".equals(flag.getName())) {
                if (platform != null) {
                    // Update existing platform flag using withers
                    return flag.withValue(updatePlatformValue(flag.getValue(), platform));
                }
                // If platform is null, return null to remove this flag
                return null;
            }
            return flag;
        });
        if (oldFlags != newFlags || platform == null) {
            return from.withFlags(newFlags);
        }

        // Add new platform flag if it wasn't found and platform is not null
        Docker.Flag platformFlag = new Docker.Flag(
                randomId(),
                oldFlags != null && !oldFlags.isEmpty() ? oldFlags.get(0).getPrefix() : Space.SINGLE_SPACE,
                from.getMarkers(),
                "platform",
                createPlatformValue(platform, Space.EMPTY)
        );
        return from.withFlags(ListUtils.concat(platformFlag, newFlags));
    }

    private Docker.Argument updatePlatformValue(Docker.@Nullable Argument existingValue, String platform) {
        if (existingValue != null && !existingValue.getContents().isEmpty()) {
            // Update existing value using withers
            Docker.ArgumentContent firstContent = existingValue.getContents().get(0);
            if (firstContent instanceof Docker.PlainText) {
                Docker.PlainText updated = ((Docker.PlainText) firstContent).withText(platform);
                return existingValue.withContents(singletonList(updated));
            }
            if (firstContent instanceof Docker.QuotedString) {
                Docker.QuotedString updated = ((Docker.QuotedString) firstContent).withValue(platform);
                return existingValue.withContents(singletonList(updated));
            }
        }
        // Fallback: create new value
        return createPlatformValue(platform, existingValue != null ? existingValue.getPrefix() : Space.EMPTY);
    }

    private Docker.Argument createPlatformValue(String platform, Space prefix) {
        return new Docker.Argument(
                randomId(),
                prefix,
                Markers.EMPTY,
                singletonList(new Docker.PlainText(
                        randomId(),
                        Space.EMPTY,
                        Markers.EMPTY,
                        platform
                ))
        );
    }
}
