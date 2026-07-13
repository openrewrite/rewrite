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
package org.openrewrite.maven.internal.engine;

import org.jspecify.annotations.Nullable;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.engine.shaded.org.apache.maven.model.building.ModelBuildingException;
import org.openrewrite.maven.engine.shaded.org.apache.maven.model.building.ModelProblem;
import org.openrewrite.maven.engine.shaded.org.apache.maven.model.resolution.UnresolvableModelException;
import org.openrewrite.maven.internal.MavenParsingException;
import org.openrewrite.maven.tree.GroupArtifactVersion;
import org.openrewrite.maven.tree.MavenRepository;

import java.util.Map;
import java.util.StringJoiner;

/**
 * Maps a {@link ModelBuildingException} to rewrite's exception, splitting exactly the way the legacy engine did:
 * a parent/BOM that could not be <em>downloaded</em> surfaces as a {@link MavenDownloadingException} (as
 * {@code MavenPomDownloader.download} threw), while a Maven-<em>fatal validation</em> problem — a parent cycle, a
 * relativePath cycle, a self-parent, a non-constant parent version — surfaces as a {@link MavenParsingException} (as the
 * legacy {@code resolveParentPom} threw for a missing/unresolvable parent version). Parity policy (DESIGN §0): these
 * fail Maven-identically with no cycle-breaking leniency; the captured problem text is preserved in the message.
 */
public final class ModelParityErrorMapper {

    private ModelParityErrorMapper() {
    }

    public static Throwable map(ModelBuildingException exception, CacheBridge bridge) {
        UnresolvableModelException unresolvable = findUnresolvable(exception);
        if (unresolvable != null) {
            GroupArtifactVersion failedOn = new GroupArtifactVersion(
                    unresolvable.getGroupId(), unresolvable.getArtifactId(), unresolvable.getVersion());
            MavenDownloadingException downloading =
                    new MavenDownloadingException(summarize(exception), unresolvable, failedOn);
            Map<MavenRepository, String> responses = bridge.responsesFor(failedOn);
            if (responses != null && !responses.isEmpty()) {
                downloading.setRepositoryResponses(responses);
            }
            return downloading;
        }
        return new MavenParsingException(summarize(exception));
    }

    private static @Nullable UnresolvableModelException findUnresolvable(ModelBuildingException exception) {
        for (ModelProblem problem : exception.getProblems()) {
            for (Throwable t = problem.getException(); t != null; t = t.getCause()) {
                if (t instanceof UnresolvableModelException) {
                    return (UnresolvableModelException) t;
                }
            }
        }
        return null;
    }

    /** The fatal/error problem messages verbatim (cycle text, self-parent text), falling back to the exception message. */
    private static String summarize(ModelBuildingException exception) {
        StringJoiner joiner = new StringJoiner("; ");
        for (ModelProblem problem : exception.getProblems()) {
            if (problem.getSeverity() == ModelProblem.Severity.FATAL ||
                problem.getSeverity() == ModelProblem.Severity.ERROR) {
                joiner.add(problem.getMessage());
            }
        }
        String summary = joiner.toString();
        return summary.isEmpty() ? exception.getMessage() : summary;
    }
}
