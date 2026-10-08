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
import org.openrewrite.ipc.http.HttpSender;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.util.Collections.emptyMap;

/**
 * Serves one body to requests carrying the expected {@code Authorization} header, answers 401 to any other, and
 * records the {@code Authorization} header of every request it receives.
 */
class AuthenticatingHttpSender implements HttpSender {
    static final String AUTHORIZATION = "Bearer token";

    private final byte[] body;
    final List<@Nullable String> authorizations = new CopyOnWriteArrayList<>();

    AuthenticatingHttpSender(byte[] body) {
        this.body = body;
    }

    @Override
    public Response send(Request request) {
        String authorization = request.getRequestHeaders().get("Authorization");
        authorizations.add(authorization);
        if (!AUTHORIZATION.equals(authorization)) {
            return new Response(401, new ByteArrayInputStream(new byte[0]), emptyMap(), () -> {
            });
        }
        return new Response(200, new ByteArrayInputStream(body), emptyMap(), () -> {
        });
    }

    static RemoteAuthenticator authenticatorFor(String baseUri) {
        return new RemoteAuthenticator() {
            @Override
            public boolean appliesTo(URI uri) {
                return uri.toString().startsWith(baseUri);
            }

            @Override
            public Request.Builder authenticate(URI uri, Request.Builder request) {
                return request.withHeader("Authorization", AUTHORIZATION);
            }
        };
    }
}
