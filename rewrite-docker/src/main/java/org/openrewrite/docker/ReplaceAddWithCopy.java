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
import org.openrewrite.docker.tree.Docker;
import org.openrewrite.internal.ListUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.openrewrite.Tree.randomId;

/**
 * Replaces ADD instructions with COPY instructions where appropriate.
 * <p>
 * ADD has two features that COPY does not:
 * <ol>
 *   <li>It can fetch URLs</li>
 *   <li>It automatically extracts tar archives</li>
 * </ol>
 * <p>
 * Using COPY instead of ADD is a best practice (CIS Docker Benchmark 4.9) because:
 * <ul>
 *   <li>COPY is more transparent - it only copies files</li>
 *   <li>ADD's auto-extraction can be unexpected and create security issues</li>
 *   <li>For URL fetching, using RUN with curl/wget is preferred for better caching</li>
 * </ul>
 */
@Value
@EqualsAndHashCode(callSuper = false)
public class ReplaceAddWithCopy extends Recipe {

    private static final Set<String> TAR_EXTENSIONS = new HashSet<>(Arrays.asList(
            ".tar", ".tar.gz", ".tgz", ".tar.bz2", ".tbz", ".tbz2",
            ".tar.xz", ".txz", ".tar.lz", ".tlz", ".tar.lzma", ".tar.zst"
    ));

    @Override
    public String getDisplayName() {
        return "Replace ADD with COPY";
    }

    @Override
    public String getDescription() {
        return "Replaces ADD instructions with COPY where appropriate. " +
                "ADD is only kept when the source is a URL or a tar archive that should be auto-extracted. " +
                "Using COPY is preferred for transparency (CIS Docker Benchmark 4.9).";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new DockerIsoVisitor<ExecutionContext>() {
            @Override
            public Docker.Add visitAdd(Docker.Add add, ExecutionContext ctx) {
                add = (Docker.Add) super.visitAdd(add, ctx);

                if (shouldKeepAsAdd(add)) {
                    return add;
                }

                // Convert ADD to COPY
                Docker.Copy copy = new Docker.Copy(
                        randomId(),
                        add.getPrefix(),
                        add.getMarkers(),
                        "COPY",
                        add.getFlags(),
                        add.getForm()
                );

                //noinspection DataFlowIssue - doAfterVisit accepts Docker types
                doAfterVisit(new ReplaceAddWithCopyVisitor(add, copy));
                return add;
            }

            private boolean shouldKeepAsAdd(Docker.Add add) {
                // Check source arguments
                Docker.CopyShellForm shellForm = add.getShellForm();
                if (shellForm != null) {
                    for (Docker.Argument source : shellForm.getSources()) {
                        String text = extractText(source);
                        if (text == null) {
                            // Contains environment variable - can't determine statically, be conservative
                            return true;
                        }
                        // Keep ADD for URLs
                        if (isUrl(text)) {
                            return true;
                        }
                        // Keep ADD for tar archives (auto-extraction is desired)
                        if (isTarArchive(text)) {
                            return true;
                        }
                    }
                    return false;
                }

                // For exec form and heredoc form, we can't easily detect URL/tar, be conservative
                return true;
            }

            private @Nullable String extractText(Docker.Argument arg) {
                StringBuilder builder = new StringBuilder();
                for (Docker.ArgumentContent content : arg.getContents()) {
                    if (content instanceof Docker.Literal) {
                        builder.append(((Docker.Literal) content).getText());
                    } else if (content instanceof Docker.EnvironmentVariable) {
                        // If there's an env var, we can't determine statically
                        return null;
                    }
                }
                return builder.toString();
            }

            private boolean isUrl(String source) {
                String lower = source.toLowerCase();
                return lower.startsWith("http://") ||
                        lower.startsWith("https://") ||
                        lower.startsWith("ftp://");
            }

            private boolean isTarArchive(String source) {
                String lower = source.toLowerCase();
                for (String ext : TAR_EXTENSIONS) {
                    if (lower.endsWith(ext)) {
                        return true;
                    }
                }
                return false;
            }
        };
    }

    private static class ReplaceAddWithCopyVisitor extends DockerIsoVisitor<ExecutionContext> {
        private final Docker.Add addToReplace;
        private final Docker.Copy copyReplacement;

        ReplaceAddWithCopyVisitor(Docker.Add addToReplace, Docker.Copy copyReplacement) {
            this.addToReplace = addToReplace;
            this.copyReplacement = copyReplacement;
        }

        @Override
        public Docker.Stage visitStage(Docker.Stage stage, ExecutionContext ctx) {
            return stage.withInstructions(
                    ListUtils.map(stage.getInstructions(),
                            inst -> inst == addToReplace ? copyReplacement : inst)
            );
        }
    }
}
