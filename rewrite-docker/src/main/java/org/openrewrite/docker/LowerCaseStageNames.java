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
import org.openrewrite.TreeVisitor;
import org.openrewrite.docker.internal.ArgumentContents;
import org.openrewrite.docker.trait.DockerCopyFrom;
import org.openrewrite.docker.tree.Docker;
import org.openrewrite.internal.ListUtils;

import java.util.*;

@Value
@EqualsAndHashCode(callSuper = false)
public class LowerCaseStageNames extends Recipe {

    String displayName = "Name Dockerfile build stages in lowercase";

    String description = "BuildKit's `StageNameCasing` check reports a build stage whose name is not lowercase. Renaming a " +
            "stage means renaming every reference to it, so the `FROM` instructions, `COPY --from` flags and " +
            "`RUN --mount` sources that name the stage are updated in the same pass.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new DockerIsoVisitor<ExecutionContext>() {
            @Override
            public Docker.File visitFile(Docker.File file, ExecutionContext ctx) {
                Map<String, String> renames = renames(file);
                if (renames.isEmpty()) {
                    return file;
                }
                return (Docker.File) new Rename(renames, referencingFroms(file, renames)).visitNonNull(file, ctx);
            }
        };
    }

    private static Map<String, String> renames(Docker.File file) {
        Map<String, String> renames = new LinkedHashMap<>();
        for (Docker.Stage stage : file.getStages()) {
            Docker.From.As as = stage.getFrom().getAs();
            if (as == null) {
                continue;
            }
            String name = as.getName().getText();
            String lower = name.toLowerCase(Locale.ROOT);
            if (!name.equals(lower)) {
                renames.put(name, lower);
            }
        }
        return renames;
    }

    /// The `FROM` instructions whose image name is a reference to a renamed stage rather than an image of that
    /// name. Only a stage declared earlier in the file can be built on, so a name that no stage before this one
    /// carries is an image.
    private static Set<UUID> referencingFroms(Docker.File file, Map<String, String> renames) {
        Set<UUID> referencing = new HashSet<>();
        Set<String> declared = new HashSet<>();
        for (Docker.Stage stage : file.getStages()) {
            Docker.From from = stage.getFrom();
            String imageName = from.getTag() == null && from.getDigest() == null ?
                    ArgumentContents.text(from.getImageName()) : null;
            if (imageName != null && declared.contains(imageName) && renames.containsKey(imageName)) {
                referencing.add(from.getId());
            }
            Docker.From.As as = from.getAs();
            if (as != null) {
                declared.add(as.getName().getText());
            }
        }
        return referencing;
    }

    private static class Rename extends DockerIsoVisitor<ExecutionContext> {
        private final Map<String, String> renames;
        private final Set<UUID> referencingFroms;

        private Rename(Map<String, String> renames, Set<UUID> referencingFroms) {
            this.renames = renames;
            this.referencingFroms = referencingFroms;
        }

        @Override
        public Docker.From visitFrom(Docker.From from, ExecutionContext ctx) {
            Docker.From f = super.visitFrom(from, ctx);
            if (referencingFroms.contains(f.getId())) {
                String renamed = renames.get(ArgumentContents.text(f.getImageName()));
                if (renamed != null) {
                    f = f.withImageName(f.getImageName().withContents(ArgumentContents.of(renamed, null)));
                }
            }
            Docker.From.As as = f.getAs();
            if (as != null) {
                String renamed = renames.get(as.getName().getText());
                if (renamed != null) {
                    f = f.withAs(as.withName(as.getName().withText(renamed)));
                }
            }
            return f;
        }

        /// A `RUN` mounts an earlier stage with `--mount=type=bind,from=<stage>`, so the name it uses has to be
        /// renamed along with the rest. The flag holds the `=` and `,` of an option list as contents of their
        /// own, which leaves the name a content too, renamed where it sits rather than by rebuilding the value.
        @Override
        public Docker.Run visitRun(Docker.Run run, ExecutionContext ctx) {
            Docker.Run r = super.visitRun(run, ctx);
            if (r.getFlags() == null) {
                return r;
            }
            return r.withFlags(ListUtils.map(r.getFlags(), flag ->
                    "mount".equals(flag.getName()) && flag.getValue() != null ?
                            flag.withValue(flag.getValue().withContents(
                                    renameMountFrom(flag.getValue().getContents()))) :
                            flag));
        }

        private List<Docker.ArgumentContent> renameMountFrom(List<Docker.ArgumentContent> contents) {
            return ListUtils.map(contents, (i, content) -> {
                if (i < 2 || !(content instanceof Docker.Literal) ||
                        !reads(contents.get(i - 1), "=") || !reads(contents.get(i - 2), "from")) {
                    return content;
                }
                Docker.Literal literal = (Docker.Literal) content;
                String renamed = renames.get(literal.getText());
                return renamed == null ? content : literal.withText(renamed);
            });
        }

        private static boolean reads(Docker.ArgumentContent content, String text) {
            return content instanceof Docker.Literal && text.equals(((Docker.Literal) content).getText());
        }

        @Override
        public Docker.Copy visitCopy(Docker.Copy copy, ExecutionContext ctx) {
            Docker.Copy c = super.visitCopy(copy, ctx);
            DockerCopyFrom copyFrom = new DockerCopyFrom(getCursor());
            if (!copyFrom.isStageReference()) {
                return c;
            }
            String renamed = renames.get(copyFrom.getFromValue().orElse(null));
            if (renamed == null) {
                return c;
            }
            return c.withFlags(ListUtils.map(c.getFlags(), flag ->
                    "from".equals(flag.getName()) && flag.getValue() != null ?
                            flag.withValue(flag.getValue().withContents(ArgumentContents.of(renamed, null))) : flag));
        }
    }
}
