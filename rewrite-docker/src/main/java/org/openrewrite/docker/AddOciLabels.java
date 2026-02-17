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
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.Validated;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Adds standard OCI (Open Container Initiative) image labels to a Dockerfile.
 * <p>
 * OCI defines a standard set of labels for container images that provide metadata
 * such as the image title, description, version, creation time, source repository,
 * and license information.
 * <p>
 * The standard labels are prefixed with {@code org.opencontainers.image.} and include:
 * <ul>
 *   <li>{@code title} - Human-readable title of the image</li>
 *   <li>{@code description} - Human-readable description of the image</li>
 *   <li>{@code version} - Version of the packaged software</li>
 *   <li>{@code created} - Date and time the image was built (RFC 3339)</li>
 *   <li>{@code revision} - Source control revision identifier</li>
 *   <li>{@code source} - URL to get source code for building the image</li>
 *   <li>{@code url} - URL to find more information about the image</li>
 *   <li>{@code vendor} - Name of the distributing entity, organization, or individual</li>
 *   <li>{@code licenses} - License(s) under which contained software is distributed</li>
 *   <li>{@code authors} - Contact details of the people or organization responsible</li>
 * </ul>
 *
 * @see <a href="https://github.com/opencontainers/image-spec/blob/main/annotations.md">OCI Image Annotations</a>
 */
@Value
@EqualsAndHashCode(callSuper = false)
public class AddOciLabels extends Recipe {

    @Option(displayName = "Title",
            description = "Human-readable title of the image (org.opencontainers.image.title).",
            example = "My Application",
            required = false)
    @Nullable
    String title;

    @Option(displayName = "Description",
            description = "Human-readable description of the image (org.opencontainers.image.description).",
            example = "A containerized web application",
            required = false)
    @Nullable
    String description;

    @Option(displayName = "Version",
            description = "Version of the packaged software (org.opencontainers.image.version).",
            example = "1.0.0",
            required = false)
    @Nullable
    String version;

    @Option(displayName = "Created timestamp",
            description = "Date and time the image was built in RFC 3339 format. If 'now', uses the current time.",
            example = "2024-01-15T10:30:00Z",
            required = false)
    @Nullable
    String created;

    @Option(displayName = "Revision",
            description = "Source control revision identifier for the packaged software (org.opencontainers.image.revision).",
            example = "abc123def456",
            required = false)
    @Nullable
    String revision;

    @Option(displayName = "Source URL",
            description = "URL to get source code for building the image (org.opencontainers.image.source).",
            example = "https://github.com/myorg/myapp",
            required = false)
    @Nullable
    String source;

    @Option(displayName = "Documentation URL",
            description = "URL to find more information about the image (org.opencontainers.image.url).",
            example = "https://myapp.example.com",
            required = false)
    @Nullable
    String url;

    @Option(displayName = "Vendor",
            description = "Name of the distributing entity, organization, or individual (org.opencontainers.image.vendor).",
            example = "My Organization",
            required = false)
    @Nullable
    String vendor;

    @Option(displayName = "Licenses",
            description = "License(s) under which contained software is distributed as a SPDX License Expression (org.opencontainers.image.licenses).",
            example = "Apache-2.0",
            required = false)
    @Nullable
    String licenses;

    @Option(displayName = "Authors",
            description = "Contact details of the people or organization responsible (org.opencontainers.image.authors).",
            example = "maintainers@example.com",
            required = false)
    @Nullable
    String authors;

    @Override
    public String getDisplayName() {
        return "Add OCI image labels";
    }

    @Override
    public String getDescription() {
        return "Adds standard OCI (Open Container Initiative) image labels to a Dockerfile. " +
                "These labels provide metadata about the image such as title, version, source, and license information. " +
                "See https://github.com/opencontainers/image-spec/blob/main/annotations.md for the specification.";
    }

    @Override
    public Validated<Object> validate() {
        return super.validate().and(
                Validated.test("labels", "At least one label must be specified",
                        this, r ->
                                (r.title != null && !r.title.isEmpty()) ||
                                (r.description != null && !r.description.isEmpty()) ||
                                (r.version != null && !r.version.isEmpty()) ||
                                (r.created != null && !r.created.isEmpty()) ||
                                (r.revision != null && !r.revision.isEmpty()) ||
                                (r.source != null && !r.source.isEmpty()) ||
                                (r.url != null && !r.url.isEmpty()) ||
                                (r.vendor != null && !r.vendor.isEmpty()) ||
                                (r.licenses != null && !r.licenses.isEmpty()) ||
                                (r.authors != null && !r.authors.isEmpty())
                )
        );
    }

    private static final String OCI_PREFIX = "org.opencontainers.image.";

    @Override
    public List<Recipe> getRecipeList() {
        List<Recipe> recipes = new ArrayList<>();

        if (title != null && !title.isEmpty()) {
            recipes.add(new AddOrUpdateLabel(OCI_PREFIX + "title", title, true, null));
        }
        if (description != null && !description.isEmpty()) {
            recipes.add(new AddOrUpdateLabel(OCI_PREFIX + "description", description, true, null));
        }
        if (version != null && !version.isEmpty()) {
            recipes.add(new AddOrUpdateLabel(OCI_PREFIX + "version", version, true, null));
        }
        if (created != null && !created.isEmpty()) {
            String timestamp = "now".equalsIgnoreCase(created) ?
                    DateTimeFormatter.ISO_INSTANT.format(Instant.now()) :
                    created;
            recipes.add(new AddOrUpdateLabel(OCI_PREFIX + "created", timestamp, true, null));
        }
        if (revision != null && !revision.isEmpty()) {
            recipes.add(new AddOrUpdateLabel(OCI_PREFIX + "revision", revision, true, null));
        }
        if (source != null && !source.isEmpty()) {
            recipes.add(new AddOrUpdateLabel(OCI_PREFIX + "source", source, true, null));
        }
        if (url != null && !url.isEmpty()) {
            recipes.add(new AddOrUpdateLabel(OCI_PREFIX + "url", url, true, null));
        }
        if (vendor != null && !vendor.isEmpty()) {
            recipes.add(new AddOrUpdateLabel(OCI_PREFIX + "vendor", vendor, true, null));
        }
        if (licenses != null && !licenses.isEmpty()) {
            recipes.add(new AddOrUpdateLabel(OCI_PREFIX + "licenses", licenses, true, null));
        }
        if (authors != null && !authors.isEmpty()) {
            recipes.add(new AddOrUpdateLabel(OCI_PREFIX + "authors", authors, true, null));
        }

        return recipes;
    }
}
