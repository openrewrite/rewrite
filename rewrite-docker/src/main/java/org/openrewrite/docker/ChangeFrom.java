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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.util.Collections.emptyList;
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
        return "Change the base image in a Dockerfile FROM instruction. " +
                "Each `*` in an `old*` glob is a positional capture; `$N` in the paired `new*` substitutes capture N. " +
                "`$0` substitutes the full original value; `\\$` is a literal dollar.";
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
        validated = validateBackrefs(validated, "newImageName", newImageName, oldImageName);
        validated = validateBackrefs(validated, "newTag", newTag, oldTag);
        validated = validateBackrefs(validated, "newDigest", newDigest, oldDigest);
        validated = validateBackrefs(validated, "newPlatform", newPlatform, oldPlatform);
        return validated;
    }

    private static Validated<Object> validateBackrefs(Validated<Object> validated, String field,
                                                     @Nullable String template, @Nullable String oldPattern) {
        int highest = highestBackref(template);
        if (highest > 0) {
            int captures = countCaptures(oldPattern);
            if (highest > captures) {
                validated = validated.and(Validated.invalid(field, template,
                        String.format("%s references $%d but the paired old-field pattern has only %d capture group(s).",
                                field, highest, captures)));
            }
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

            // Resolve $N backrefs against captures from the paired old-field glob
            String resolvedNewImageName = resolve(newImageName, currentImageName, oldImageName);
            String resolvedNewTag = resolve(newTag, currentTag, oldTag);
            String resolvedNewDigest = resolve(newDigest, currentDigest, oldDigest);
            String resolvedNewPlatform = resolve(newPlatform, currentPlatform, oldPlatform);

            boolean imageNameChanged = resolvedNewImageName != null && !currentImageName.equals(resolvedNewImageName);
            boolean tagChanged = resolvedNewTag != null && !resolvedNewTag.equals(currentTag == null ? "" : currentTag);
            boolean digestChanged = resolvedNewDigest != null && !resolvedNewDigest.equals(currentDigest == null ? "" : currentDigest);
            boolean platformChanged = resolvedNewPlatform != null && !Objects.equals(
                    currentPlatform == null ? "" : currentPlatform,
                    resolvedNewPlatform);

            if (!imageNameChanged && !tagChanged && !digestChanged && !platformChanged) {
                return f;
            }

            Docker.From result = f;

            // Update platform flag if needed
            if (platformChanged) {
                if (resolvedNewPlatform.isEmpty()) {
                    result = updatePlatformFlag(result, null);
                } else {
                    result = updatePlatformFlag(result, resolvedNewPlatform);
                }
            }

            // Get quote style from original
            Docker.Literal.QuoteStyle quoteStyle = image.getQuoteStyle();

            // Check if the original was a single content item (e.g., a single quoted string)
            boolean wasSingleContent = f.getImageName().getContents().size() == 1 &&
                    f.getTag() == null && f.getDigest() == null;

            if (wasSingleContent) {
                // Keep as a single content item (don't split)
                String imagePart = resolvedNewImageName != null ? resolvedNewImageName : currentImageName;
                StringBuilder sb = new StringBuilder(imagePart);
                // For tag: null=keep existing, ""=remove, value=set
                if (resolvedNewTag != null) {
                    if (!resolvedNewTag.isEmpty()) {
                        sb.append(":").append(resolvedNewTag);
                    }
                } else if (currentTag != null) {
                    sb.append(":").append(currentTag);
                }
                // For digest: null=keep existing, ""=remove, value=set
                if (resolvedNewDigest != null) {
                    if (!resolvedNewDigest.isEmpty()) {
                        sb.append("@").append(resolvedNewDigest);
                    }
                } else if (currentDigest != null) {
                    sb.append("@").append(currentDigest);
                }
                Docker.ArgumentContent newContent = createContent(sb.toString(), quoteStyle);
                Docker.Argument newImageArg = f.getImageName().withContents(singletonList(newContent));
                return result.withImageName(newImageArg);
            }

            // Update image name: null=keep, value=set
            if (resolvedNewImageName != null) {
                Docker.ArgumentContent newImageContent = createContent(resolvedNewImageName, quoteStyle);
                Docker.Argument newImageArg = f.getImageName().withContents(singletonList(newImageContent));
                result = result.withImageName(newImageArg);
            }

            // Update tag: null=keep, ""=remove, value=set
            if (resolvedNewTag != null) {
                if (resolvedNewTag.isEmpty()) {
                    result = result.withTag(null);
                } else {
                    Docker.ArgumentContent newTagContent = createContent(resolvedNewTag, quoteStyle);
                    Docker.Argument newTagArg = new Docker.Argument(randomId(), Space.EMPTY, Markers.EMPTY, singletonList(newTagContent));
                    result = result.withTag(newTagArg);
                }
            }

            // Update digest: null=keep, ""=remove, value=set
            if (resolvedNewDigest != null) {
                if (resolvedNewDigest.isEmpty()) {
                    result = result.withDigest(null);
                } else {
                    Docker.ArgumentContent newDigestContent = createContent(resolvedNewDigest, quoteStyle);
                    Docker.Argument newDigestArg = new Docker.Argument(randomId(), Space.EMPTY, Markers.EMPTY, singletonList(newDigestContent));
                    result = result.withDigest(newDigestArg);
                }
            }

            return result;
        });
    }

    private static @Nullable String resolve(@Nullable String template, @Nullable String originalText, @Nullable String oldPattern) {
        if (template == null) {
            return null;
        }
        List<String> captures = extractCaptures(originalText, oldPattern);
        return applyBackrefs(template, originalText == null ? "" : originalText, captures);
    }

    private static List<String> extractCaptures(@Nullable String text, @Nullable String pattern) {
        if (text == null || pattern == null || pattern.indexOf('*') < 0) {
            return emptyList();
        }
        java.util.regex.Matcher m = globToRegex(pattern).matcher(text);
        if (!m.matches()) {
            return emptyList();
        }
        List<String> captures = new ArrayList<>(m.groupCount());
        for (int i = 1; i <= m.groupCount(); i++) {
            String g = m.group(i);
            captures.add(g == null ? "" : g);
        }
        return captures;
    }

    private static java.util.regex.Pattern globToRegex(String glob) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < glob.length(); i++) {
            char c = glob.charAt(i);
            if (c == '*') {
                sb.append("(.*)");
            } else if (c == '?') {
                sb.append(".");
            } else if ("\\.^$|()[]{}+".indexOf(c) >= 0) {
                sb.append('\\').append(c);
            } else {
                sb.append(c);
            }
        }
        return java.util.regex.Pattern.compile(sb.toString(), java.util.regex.Pattern.DOTALL);
    }

    private static String applyBackrefs(String template, String originalText, List<String> captures) {
        if (template.indexOf('$') < 0 && template.indexOf('\\') < 0) {
            return template;
        }
        StringBuilder sb = new StringBuilder(template.length());
        int i = 0;
        while (i < template.length()) {
            char c = template.charAt(i);
            if (c == '\\' && i + 1 < template.length() && template.charAt(i + 1) == '$') {
                sb.append('$');
                i += 2;
            } else if (c == '$' && i + 1 < template.length() && Character.isDigit(template.charAt(i + 1))) {
                int n = template.charAt(i + 1) - '0';
                if (n == 0) {
                    sb.append(originalText);
                } else if (n <= captures.size()) {
                    sb.append(captures.get(n - 1));
                }
                i += 2;
            } else {
                sb.append(c);
                i++;
            }
        }
        return sb.toString();
    }

    private static int countCaptures(@Nullable String glob) {
        if (glob == null) {
            return 0;
        }
        int count = 0;
        for (int i = 0; i < glob.length(); i++) {
            if (glob.charAt(i) == '*') {
                count++;
            }
        }
        return count;
    }

    private static int highestBackref(@Nullable String template) {
        if (template == null) {
            return -1;
        }
        int highest = -1;
        int i = 0;
        while (i < template.length()) {
            char c = template.charAt(i);
            if (c == '\\' && i + 1 < template.length() && template.charAt(i + 1) == '$') {
                i += 2;
            } else if (c == '$' && i + 1 < template.length() && Character.isDigit(template.charAt(i + 1))) {
                int n = template.charAt(i + 1) - '0';
                if (n > highest) {
                    highest = n;
                }
                i += 2;
            } else {
                i++;
            }
        }
        return highest;
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
