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
import org.openrewrite.docker.trait.DockerFrom;
import org.openrewrite.docker.tree.Docker;
import org.openrewrite.docker.tree.Space;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.marker.Markers;

import java.util.List;
import java.util.Objects;

import static java.util.Collections.singletonList;
import static org.openrewrite.Tree.randomId;

@Value
@EqualsAndHashCode(callSuper = false)
public class ChangeFrom extends Recipe {

    @Option(displayName = "Old image name",
            description = "Glob pattern to match image names (without tag/digest).",
            example = "ubuntu")
    String oldImageName;

    @Option(displayName = "Old tag",
            description = "Only match images with tags matching this glob pattern. If null, matches any tag or no tag.",
            example = "20.*",
            required = false)
    @Nullable
    String oldTag;

    @Option(displayName = "Old digest",
            description = "Only match images with digests matching this glob pattern. If null, matches any digest or no digest.",
            example = "sha256:*",
            required = false)
    @Nullable
    String oldDigest;

    @Option(displayName = "Old platform",
            description = "Only change images with this platform. If null, matches any platform.",
            example = "linux/amd64",
            required = false)
    @Nullable
    String oldPlatform;

    @Option(displayName = "New image name",
            description = "The new image name. If null, preserves the existing name.",
            example = "ubuntu",
            required = false)
    @Nullable
    String newImageName;

    @Option(displayName = "New tag",
            description = "The new tag. If null, preserves the existing tag. If empty, removes the tag.",
            example = "22.04",
            required = false)
    @Nullable
    String newTag;

    @Option(displayName = "New digest",
            description = "The new digest. If null, preserves the existing digest. If empty, removes the digest.",
            example = "sha256:abc123...",
            required = false)
    @Nullable
    String newDigest;

    @Option(displayName = "New platform",
            description = "The new platform. If null, preserves the existing platform. If empty, removes the platform flag.",
            example = "linux/arm64",
            required = false)
    @Nullable
    String newPlatform;

    @Override
    public String getDisplayName() {
        return "Change Docker FROM";
    }

    @Override
    public String getDescription() {
        return "Change the base image in a Dockerfile FROM instruction.";
    }

    @Override
    public Validated<Object> validate() {
        Validated<Object> validated = super.validate();
        if (newImageName == null && newTag == null && newDigest == null && newPlatform == null) {
            validated = validated.and(Validated.invalid("options", null,
                    "At least one of newImageName, newTag, newDigest, or newPlatform must be specified"));
        }
        if (newImageName != null && newImageName.isEmpty()) {
            validated = validated.and(Validated.invalid("newImageName", newImageName,
                    "newImageName cannot be empty; omit to preserve the existing name"));
        }
        return validated;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        DockerFrom.Matcher matcher = new DockerFrom.Matcher()
                .imageName(oldImageName);
        if (oldTag != null) {
            matcher.tag(oldTag);
        }
        if (oldDigest != null) {
            matcher.digest(oldDigest);
        }
        if (oldPlatform != null) {
            matcher.platform(oldPlatform);
        }

        return matcher.asVisitor((image, ctx) -> {
            Docker.From f = image.getTree();

            // Check if any change is needed
            String currentImageName = image.getImageName();
            String currentTag = image.getTag();
            String currentDigest = image.getDigest();
            String currentPlatform = image.getPlatform();

            boolean imageNameChanged = newImageName != null && !currentImageName.equals(newImageName);
            boolean tagChanged = newTag != null && !newTag.equals(currentTag == null ? "" : currentTag);
            boolean digestChanged = newDigest != null && !newDigest.equals(currentDigest == null ? "" : currentDigest);
            boolean platformChanged = newPlatform != null && !Objects.equals(
                    currentPlatform == null ? "" : currentPlatform,
                    newPlatform);

            if (!imageNameChanged && !tagChanged && !digestChanged && !platformChanged) {
                return f;
            }

            Docker.From result = f;

            // Update platform flag if needed
            if (platformChanged) {
                if (newPlatform.isEmpty()) {
                    result = updatePlatformFlag(result, null);
                } else {
                    result = updatePlatformFlag(result, newPlatform);
                }
            }

            // Get quote style from original
            Docker.Literal.QuoteStyle quoteStyle = image.getQuoteStyle();

            // Check if the original was a single content item (e.g., a single quoted string)
            boolean wasSingleContent = f.getImageName().getContents().size() == 1 &&
                    f.getTag() == null && f.getDigest() == null;

            if (wasSingleContent) {
                // Keep as a single content item (don't split)
                String imagePart = newImageName != null ? newImageName : currentImageName;
                StringBuilder sb = new StringBuilder(imagePart);
                // For tag: null=keep existing, ""=remove, value=set
                if (newTag != null) {
                    if (!newTag.isEmpty()) {
                        sb.append(":").append(newTag);
                    }
                } else if (currentTag != null) {
                    sb.append(":").append(currentTag);
                }
                // For digest: null=keep existing, ""=remove, value=set
                if (newDigest != null) {
                    if (!newDigest.isEmpty()) {
                        sb.append("@").append(newDigest);
                    }
                } else if (currentDigest != null) {
                    sb.append("@").append(currentDigest);
                }
                Docker.ArgumentContent newContent = createContent(sb.toString(), quoteStyle);
                Docker.Argument newImageArg = f.getImageName().withContents(singletonList(newContent));
                return result.withImageName(newImageArg);
            }

            // Update image name: null=keep, value=set
            if (newImageName != null) {
                Docker.ArgumentContent newImageContent = createContent(newImageName, quoteStyle);
                Docker.Argument newImageArg = f.getImageName().withContents(singletonList(newImageContent));
                result = result.withImageName(newImageArg);
            }

            // Update tag: null=keep, ""=remove, value=set
            if (newTag != null) {
                if (newTag.isEmpty()) {
                    result = result.withTag(null);
                } else {
                    Docker.ArgumentContent newTagContent = createContent(newTag, quoteStyle);
                    Docker.Argument newTagArg = new Docker.Argument(randomId(), Space.EMPTY, Markers.EMPTY, singletonList(newTagContent));
                    result = result.withTag(newTagArg);
                }
            }

            // Update digest: null=keep, ""=remove, value=set
            if (newDigest != null) {
                if (newDigest.isEmpty()) {
                    result = result.withDigest(null);
                } else {
                    Docker.ArgumentContent newDigestContent = createContent(newDigest, quoteStyle);
                    Docker.Argument newDigestArg = new Docker.Argument(randomId(), Space.EMPTY, Markers.EMPTY, singletonList(newDigestContent));
                    result = result.withDigest(newDigestArg);
                }
            }

            return result;
        });
    }

    private Docker.ArgumentContent createContent(String text, Docker.Literal.@Nullable QuoteStyle quoteStyle) {
        return new Docker.Literal(randomId(), Space.EMPTY, Markers.EMPTY, text, quoteStyle);
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
            if (firstContent instanceof Docker.Literal) {
                Docker.Literal updated = ((Docker.Literal) firstContent).withText(platform);
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
                singletonList(new Docker.Literal(
                        randomId(),
                        Space.EMPTY,
                        Markers.EMPTY,
                        platform,
                        null
                ))
        );
    }
}
