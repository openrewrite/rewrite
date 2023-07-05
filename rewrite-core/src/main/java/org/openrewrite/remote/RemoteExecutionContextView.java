/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.remote;

import org.openrewrite.DelegatingExecutionContext;
import org.openrewrite.ExecutionContext;

import java.nio.file.Paths;

public class RemoteExecutionContextView extends DelegatingExecutionContext {
    private static final RemoteArtifactCache DEFAULT_ARTIFACT_CACHE = new LocalRemoteArtifactCache(
            Paths.get(System.getProperty("user.home") + "/.rewrite/remote"));

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
