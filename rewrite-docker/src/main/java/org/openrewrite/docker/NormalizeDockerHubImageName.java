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
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.docker.trait.DockerFrom;
import org.openrewrite.docker.tree.Docker;
import org.openrewrite.docker.tree.Space;
import org.openrewrite.marker.Markers;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;
import static java.lang.String.join;
import static java.util.Collections.singletonList;

/**
 * Normalizes Docker Hub image names to their canonical short form.
 * <p>
 * This recipe removes redundant Docker Hub registry prefixes from image names:
 * <ul>
 *   <li>{@code docker.io/library/ubuntu} → {@code ubuntu}</li>
 *   <li>{@code docker.io/myuser/myimage} → {@code myuser/myimage}</li>
 *   <li>{@code index.docker.io/library/ubuntu} → {@code ubuntu}</li>
 *   <li>{@code registry.hub.docker.com/library/ubuntu} → {@code ubuntu}</li>
 * </ul>
 */
@Value
@EqualsAndHashCode(callSuper = false)
public class NormalizeDockerHubImageName extends Recipe {

    private static final List<String> DOCKER_HUB_HOSTS = Arrays.asList(
            "docker.io",
            "index.docker.io",
            "registry.hub.docker.com",
            "registry-1.docker.io");

    /**
     * Pattern to match Docker Hub image references.
     * Groups: 1=host, 2=library/ (optional), 3=image name
     */
    private static final Pattern DOCKER_HUB_PATTERN = Pattern.compile(
            format("^(%s)/(library/)?(.+)$", join("|", DOCKER_HUB_HOSTS).replace(".", "\\.")));

    String displayName = "Normalize Docker Hub image names";
    String description = "Normalizes Docker Hub image names to their canonical short form by removing " +
            "redundant registry prefixes like `docker.io/library/` or `index.docker.io/`.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new DockerFrom.Matcher()
                .excludeScratch()
                .asVisitor(image -> {
                    if (image.getImageName() == null) {
                        return image.getTree();
                    }

                    Matcher matcher = DOCKER_HUB_PATTERN.matcher(image.getImageName());
                    if (!matcher.matches()) {
                        return image.getTree();
                    }

                    Docker.From f = image.getTree();
                    return f.withImageName(f.getImageName().withContents(singletonList(new Docker.Literal(
                            Tree.randomId(), Space.EMPTY, Markers.EMPTY, matcher.group(3), image.getQuoteStyle()))));
                });
    }

}
