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
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.docker.trait.DockerImageReference;
import org.openrewrite.docker.trait.ImageName;
import org.openrewrite.docker.tree.Docker;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Normalizes Docker Hub image names to their canonical short form.
 * <p>
 * This recipe removes redundant Docker Hub registry prefixes from image names:
 * <ul>
 *   <li>{@code docker.io/library/ubuntu} → {@code ubuntu}</li>
 *   <li>{@code docker.io/myuser/myimage} → {@code myuser/myimage}</li>
 *   <li>{@code index.docker.io/library/ubuntu} → {@code ubuntu}</li>
 *   <li>{@code registry.hub.docker.com/library/ubuntu} → {@code ubuntu}</li>
 *   <li>{@code library/ubuntu} → {@code ubuntu}</li>
 * </ul>
 */
@Value
@EqualsAndHashCode(callSuper = false)
public class NormalizeDockerHubImageName extends Recipe {

    String displayName = "Normalize Docker Hub image names";
    String description = "Normalizes Docker Hub image names to their canonical short form by removing " +
            "redundant registry prefixes like `docker.io/library/` or `index.docker.io/`.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new DockerImageReference.Matcher()
                .excludeScratch()
                .asVisitor(image -> {
                    Docker instruction = image.getTree();
                    Docker.Argument nameArgument = image.getImageNameArgument();
                    Optional<ImageName> parsed = image.getImage();
                    if (nameArgument == null || !parsed.isPresent()) {
                        return instruction;
                    }

                    ImageName imageName = parsed.get();
                    int redundant = imageName.toString().length() - imageName.getFamiliar().length();
                    if (redundant <= 0) {
                        return instruction;
                    }

                    List<Docker.ArgumentContent> contents = dropLeading(nameArgument.getContents(), redundant);
                    return contents == null ? instruction :
                            image.withImageNameArgument(nameArgument.withContents(contents));
                });
    }

    /// Leaves the contents that follow the dropped characters as they are, so that a variable
    /// reference survives the change. Null where the prefix to drop ends part-way through one,
    /// which cannot be split.
    private static @Nullable List<Docker.ArgumentContent> dropLeading(List<Docker.ArgumentContent> contents, int characters) {
        List<Docker.ArgumentContent> remaining = new ArrayList<>(contents.size());
        int toDrop = characters;
        for (Docker.ArgumentContent content : contents) {
            if (toDrop == 0) {
                remaining.add(content);
                continue;
            }
            int length = renderedLength(content);
            if (toDrop >= length) {
                toDrop -= length;
            } else if (content instanceof Docker.Literal) {
                Docker.Literal literal = (Docker.Literal) content;
                remaining.add(literal.withText(literal.getText().substring(toDrop)));
                toDrop = 0;
            } else {
                return null;
            }
        }
        return toDrop == 0 ? remaining : null;
    }

    private static int renderedLength(Docker.ArgumentContent content) {
        if (content instanceof Docker.Literal) {
            return ((Docker.Literal) content).getText().length();
        }
        Docker.EnvironmentVariable variable = (Docker.EnvironmentVariable) content;
        return variable.getName().length() + (variable.isBraced() ? 3 : 1);
    }
}
