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
package org.openrewrite.remote;

import org.openrewrite.ipc.http.HttpSender;

import java.net.URI;

/**
 * Adds authentication to a {@link Remote} download, for the URIs it is responsible for.
 * <p>
 * Authenticators are registered on the {@link org.openrewrite.ExecutionContext} through
 * {@link RemoteExecutionContextView#addAuthenticator(RemoteAuthenticator)} and are never stored on an LST element, so
 * credentials are not serialized with the {@link Remote}. A download is sent anonymously first and is only retried
 * through {@link #authenticate(URI, HttpSender.Request.Builder)} when the server answers with a client error; once an
 * endpoint has required authentication, later downloads from it authenticate up front.
 * <p>
 * Implementations should define {@code equals} so that registering the same authenticator twice is a no-op.
 */
public interface RemoteAuthenticator {

    /**
     * @param uri The URI about to be downloaded.
     * @return Whether this authenticator holds credentials for {@code uri}.
     */
    boolean appliesTo(URI uri);

    /**
     * @param uri     The URI being downloaded, for which {@link #appliesTo(URI)} returned {@code true}.
     * @param request The request to add authentication to.
     * @return The request with authentication added.
     */
    HttpSender.Request.Builder authenticate(URI uri, HttpSender.Request.Builder request);
}
