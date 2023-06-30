package org.openrewrite.remote;

import org.openrewrite.DelegatingExecutionContext;
import org.openrewrite.ExecutionContext;

import java.nio.file.Paths;

public class RemoteExecutionContextView extends DelegatingExecutionContext {
    private static final RemoteArtifactCache DEFAULT_ARTIFACT_CACHE = new LocalRemoteArtifactCache(Paths.get(System.getProperty("java.io.tmpdir")));

    private static final String REMOTE_ARTIFACT_CACHE = "org.openrewrite.remote.artifactCache";

    private RemoteExecutionContextView(ExecutionContext delegate) {
        super(delegate);
    }

    public static RemoteExecutionContextView view(ExecutionContext ctx) {
        if (ctx instanceof RemoteExecutionContextView) {
            return (RemoteExecutionContextView) ctx;
        }
        return new RemoteExecutionContextView(ctx);
    }

    public RemoteExecutionContextView setArtifactCache(RemoteArtifactCache artifactCache) {
        putMessage(REMOTE_ARTIFACT_CACHE, artifactCache);
        return this;
    }

    public RemoteArtifactCache getArtifactCache() {
        return getMessage(REMOTE_ARTIFACT_CACHE, DEFAULT_ARTIFACT_CACHE);
    }
}
