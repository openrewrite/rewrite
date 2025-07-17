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
package org.openrewrite.maven.http;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import org.openrewrite.ipc.http.HttpSender;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OkHttp-based {@link HttpSender}.
 */
public class OkHttpSender implements HttpSender {
    private static final MediaType MEDIA_TYPE_APPLICATION_JSON = MediaType.get("application/json; charset=utf-8");
    private static final MediaType MEDIA_TYPE_TEXT_PLAIN = MediaType.get("text/plain; charset=utf-8");

    private final OkHttpClient client;

    public OkHttpSender(OkHttpClient client) {
        this.client = client;
    }

    public OkHttpSender() {
        this(new OkHttpClient());
    }

    @Override
    public Response send(Request request) {
        okhttp3.Request.Builder requestBuilder = new okhttp3.Request.Builder().url(request.getUrl());

        for (Map.Entry<String, String> requestHeader : request.getRequestHeaders().entrySet()) {
            requestBuilder.addHeader(requestHeader.getKey(), requestHeader.getValue());
        }

        byte[] entity = request.getEntity();
        Method method = request.getMethod();
        String methodValue = method.toString();
        if (entity.length > 0) {
            String contentType = request.getRequestHeaders().get("Content-Type");
            MediaType mediaType = contentType != null ?
              MediaType.get(contentType + "; charset=utf-8") :
              MEDIA_TYPE_APPLICATION_JSON;
            RequestBody body = RequestBody.create(entity, mediaType);
            requestBuilder.method(methodValue, body);
        } else {
            if (requiresRequestBody(method)) {
                RequestBody body = RequestBody.create(entity, MEDIA_TYPE_TEXT_PLAIN);
                requestBuilder.method(methodValue, body);
            } else {
                requestBuilder.method(methodValue, null);
            }
        }

        try {
            //noinspection resource
            okhttp3.Response response = client.newCall(requestBuilder.build()).execute();
            ResponseBody body = response.body();

            return new Response(response.code(), body == null ? null : body.byteStream(),
              response.headers().toMultimap(), response::close);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static boolean requiresRequestBody(Method method) {
        switch (method) {
            case POST:
            case PUT:
                return true;

            default:
                return false;
        }
    }
}
