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
package org.openrewrite.maven.engine;

import org.openrewrite.ipc.http.HttpSender;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Wraps a real {@link HttpSender}, recording (method, path) of every request that flows through it. */
class RecordingHttpSender implements HttpSender {

    static final class Recorded {
        final Method method;
        final String path;

        Recorded(Method method, String path) {
            this.method = method;
            this.path = path;
        }

        @Override
        public String toString() {
            return method + " " + path;
        }
    }

    private final HttpSender delegate;
    private final List<Recorded> requests = new CopyOnWriteArrayList<>();

    RecordingHttpSender(HttpSender delegate) {
        this.delegate = delegate;
    }

    @Override
    public Response send(Request request) {
        requests.add(new Recorded(request.getMethod(), request.getUrl().getPath()));
        return delegate.send(request);
    }

    List<Recorded> requests() {
        return requests;
    }

    long count() {
        return requests.size();
    }

    long countPomGets() {
        return requests.stream()
                .filter(r -> r.method == Method.GET && r.path.endsWith(".pom"))
                .count();
    }

    boolean anyJarRequested() {
        return requests.stream().anyMatch(r -> r.path.endsWith(".jar"));
    }
}
