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

import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.ipc.http.HttpSender;

import java.net.URI;
import java.util.Set;

/**
 * Sends the GET of a {@link Remote} download, adding authentication from a {@link RemoteAuthenticator} the way Apache
 * Maven's DeferredCredentialsProvider does: anonymously first, then with credentials once the server challenges the
 * anonymous request, and up front for endpoints that have already required credentials.
 */
final class RemoteDownload {

    private RemoteDownload() {
    }

    /**
     * @return The response, whose body the caller streams and closes.
     */
    static HttpSender.Response get(HttpSender httpSender, URI uri, ExecutionContext ctx) {
        RemoteExecutionContextView remoteCtx = RemoteExecutionContextView.view(ctx);
        RemoteAuthenticator authenticator = authenticatorFor(remoteCtx, uri);
        if (authenticator == null) {
            return httpSender.send(httpSender.get(uri.toString()).build());
        }

        String endpoint = endpointOrNull(uri);
        Set<String> authenticationRequiredEndpoints = remoteCtx.getAuthenticationRequiredEndpoints();
        if (endpoint != null && authenticationRequiredEndpoints.contains(endpoint)) {
            return httpSender.send(authenticator.authenticate(uri, httpSender.get(uri.toString())).build());
        }

        HttpSender.Response anonymous = httpSender.send(httpSender.get(uri.toString()).build());
        if (!isClientSideError(anonymous.getCode())) {
            return anonymous;
        }
        anonymous.close();

        HttpSender.Response authenticated = httpSender.send(authenticator.authenticate(uri, httpSender.get(uri.toString())).build());
        if (authenticated.isSuccessful() && endpoint != null) {
            authenticationRequiredEndpoints.add(endpoint);
        }
        return authenticated;
    }

    private static @Nullable RemoteAuthenticator authenticatorFor(RemoteExecutionContextView ctx, URI uri) {
        for (RemoteAuthenticator authenticator : ctx.getAuthenticators()) {
            if (authenticator.appliesTo(uri)) {
                return authenticator;
            }
        }
        return null;
    }

    private static @Nullable String endpointOrNull(URI uri) {
        String host = uri.getHost();
        return host == null ? null : host + ':' + uri.getPort();
    }

    /**
     * 408 (timeout), 425 (too early) and 429 (too many requests) are transient rather than credential rejections,
     * so retrying them with credentials is pointless.
     */
    private static boolean isClientSideError(int responseCode) {
        if (responseCode < 400 || responseCode > 499) {
            return false;
        }
        return responseCode != 408 && responseCode != 425 && responseCode != 429;
    }
}
