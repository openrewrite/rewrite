package org.openrewrite.maven;

import lombok.Builder;
import lombok.Value;
import lombok.With;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.tree.GroupArtifactVersion;

import java.util.Collections;
import java.util.Map;

/**
 * An easy to serialize representation of a failure to download an artifact from a maven repository.
 */
@Value
@With
@Builder
public class MavenDownloadingFailure {

    String message;

    @Nullable
    @Builder.Default
    GroupArtifactVersion root = null;

    GroupArtifactVersion failedOn;

    @Builder.Default
    Map<String, String> repositoryUriToResponse = Collections.emptyMap();

    @Builder.Default
    StackTraceElement[] stackTrace = new StackTraceElement[0];

    public static MavenDownloadingFailure fromException(MavenDownloadingException e) {
        return new MavenDownloadingFailure(e.getMessage(), e.getRoot(), e.getFailedOn(), e.getRepositoryUriToResponse(), e.getStackTrace());
    }

    public MavenDownloadingException asException() {
        MavenDownloadingException d = new MavenDownloadingException(message, null, failedOn)
                .setRoot(root)
                .setRepositoryUriToResponse(repositoryUriToResponse);
        d.setStackTrace(stackTrace);
        return d;
    }
}
