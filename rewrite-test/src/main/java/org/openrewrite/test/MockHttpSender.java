/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.test;

import org.openrewrite.ipc.http.HttpSender;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyMap;

/**
 * Issues no HTTP requests, instead returning the supplied input stream whenever send() is invoked.
 * Convenient for writing tests that do not depend on the availability of external services.
 */
public class MockHttpSender implements HttpSender {
    final UncheckedSupplier<InputStream> is;
    int responseCode = 200;
    Map<String, List<String>> responseHeaders = emptyMap();

    public MockHttpSender(UncheckedSupplier<InputStream> is) {
        this.is = is;
    }

    public MockHttpSender(int responseCode) {
        this.is = null;
        this.responseCode = responseCode;
    }

    public MockHttpSender withResponseHeaders(Map<String, List<String>> headers) {
        this.responseHeaders = headers;
        return this;
    }

    @Override
    public Response send(Request request) {
        if (responseCode != 200) {
            return new Response(responseCode, null, responseHeaders, () -> {
            });
        } else {
            return new Response(responseCode, is.get(), responseHeaders, () -> {
            });
        }
    }
}
