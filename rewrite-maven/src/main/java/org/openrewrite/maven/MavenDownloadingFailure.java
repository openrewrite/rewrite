package org.openrewrite.maven;

import lombok.Value;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.tree.GroupArtifactVersion;

import java.util.Map;

/**
 * An easy to serialize representation of a failure to download an artifact from a maven repository.
 */
@Value
public class MavenDownloadingFailure {

    @Nullable
    GroupArtifactVersion root;

    GroupArtifactVersion failedOn;

    Map<String, String> repositoryUriToResponse;

    StackTraceElement[] stackTrace;

    public static MavenDownloadingFailure fromException(MavenDownloadingException e) {
        return new MavenDownloadingFailure(e.getRoot(), e.getFailedOn(), e.getRepositoryUriToResponse(), e.getStackTrace());
    }
}
