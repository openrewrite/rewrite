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
package org.openrewrite.ipc.http;

import io.micrometer.core.instrument.util.StringUtils;
import io.micrometer.core.lang.Nullable;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.zip.GZIPOutputStream;

/**
 * A general-purpose interface for controlling how components perform HTTP calls for various purposes.
 * This interface can be used to inject more advanced customization like SSL verification, key loading,
 * etc. without requiring further additions to OpenRewrite components needing to make HTTP calls.
 */
@SuppressWarnings("unused")
public interface HttpSender {
    Response send(Request request);

    default Request.Builder post(String uri) {
        return newRequest(uri).withMethod(Method.POST);
    }

    default Request.Builder head(String uri) {
        return newRequest(uri).withMethod(Method.HEAD);
    }

    default Request.Builder put(String uri) {
        return newRequest(uri).withMethod(Method.PUT);
    }

    default Request.Builder get(String uri) {
        return newRequest(uri).withMethod(Method.GET);
    }

    default Request.Builder delete(String uri) {
        return newRequest(uri).withMethod(Method.DELETE);
    }

    default Request.Builder options(String uri) {
        return newRequest(uri).withMethod(Method.OPTIONS);
    }

    default Request.Builder newRequest(String uri) {
        return new Request.Builder(uri, this);
    }

    class Request {
        private final URL url;
        private final byte[] entity;
        private final Method method;
        private final Map<String, String> requestHeaders;

        public Request(URL url, byte[] entity, Method method, Map<String, String> requestHeaders) {
            this.url = url;
            this.entity = entity;
            this.method = method;
            this.requestHeaders = requestHeaders;
        }

        public URL getUrl() {
            return url;
        }

        public byte[] getEntity() {
            return entity;
        }

        public Method getMethod() {
            return method;
        }

        public Map<String, String> getRequestHeaders() {
            return requestHeaders;
        }

        public static Builder build(String uri, HttpSender sender) {
            return new Builder(uri, sender);
        }

        @Override
        public String toString() {
            StringBuilder printed = new StringBuilder(method.toString()).append(' ')
                    .append(url.toString()).append("\n");
            if (entity.length == 0) {
                printed.append("<no request body>");
            } else {
                printed.append(new String(entity));
            }
            return printed.toString();
        }

        @SuppressWarnings("UnusedReturnValue")
        public static class Builder {
            private static final String APPLICATION_JSON = "application/json";
            private static final String TEXT_PLAIN = "text/plain";

            private final HttpSender sender;
            private final Map<String, String> requestHeaders = new LinkedHashMap<>();

            private URL url;
            private byte[] entity = new byte[0];
            private Method method;

            Builder(String url, HttpSender sender) {
                try {
                    this.url = URI.create(url).toURL();
                } catch (MalformedURLException e) {
                    throw new UncheckedIOException(e);
                }
                this.sender = sender;
            }

            /**
             * Add a header to the request.
             *
             * @param name  The name of the header.
             * @param value The value of the header.
             * @return This request builder.
             */
            public final Builder withHeader(String name, String value) {
                requestHeaders.put(name, value);
                return this;
            }

            /**
             * If user and password are non-empty, set basic authentication on the request.
             *
             * @param user     A user name, if available.
             * @param password A password, if available.
             * @return This request builder.
             */
            public final Builder withBasicAuthentication(@Nullable String user, @Nullable String password) {
                if (StringUtils.isNotBlank(user)) {
                    String encoded = Base64.getEncoder().encodeToString((user.trim() + ":" + (password == null ? "" : password.trim()))
                            .getBytes(StandardCharsets.UTF_8));
                    withAuthentication("Basic", encoded);
                }
                return this;
            }

            /**
             * Configures the {@code Authorization} HTTP header with the given type and credentials.
             * The format will be:
             * <pre>{@code Authorization: <type> <credentials>}</pre>
             * No encoding will be performed on the {@code credentials}, so if the authentication scheme
             * expects {@code credentials} to be encoded, encode them before passing them to this method.
             *
             * @param type        authentication type
             * @param credentials authentication credentials
             * @return This request builder.
             * @since 1.8.0
             */
            public final Builder withAuthentication(String type, @Nullable String credentials) {
                if (StringUtils.isNotBlank(credentials)) {
                    withHeader("Authorization", type + " " + credentials);
                }
                return this;
            }

            /**
             * Set the request body as JSON content type.
             *
             * @param content The request body.
             * @return This request builder.
             */
            public final Builder withJsonContent(String content) {
                return withContent(APPLICATION_JSON, content);
            }

            /**
             * Set the request body as plain text content type.
             *
             * @param content The request body.
             * @return This request builder.
             */
            public final Builder withPlainText(String content) {
                return withContent(TEXT_PLAIN, content);
            }

            /**
             * Set the request body.
             *
             * @param type    The value of the "Content-Type" header to add.
             * @param content The request body.
             * @return This request builder.
             */
            public final Builder withContent(String type, String content) {
                return withContent(type, content.getBytes(StandardCharsets.UTF_8));
            }

            /**
             * Set the request body.
             *
             * @param type    The value of the "Content-Type" header to add.
             * @param content The request body.
             * @return This request builder.
             */
            public final Builder withContent(String type, byte[] content) {
                withHeader("Content-Type", type);
                entity = content;
                return this;
            }

            /**
             * Add header to accept {@code application/json} data.
             *
             * @return This request builder.
             */
            public Builder acceptJson() {
                return accept(APPLICATION_JSON);
            }

            /**
             * Add accept header.
             *
             * @param type The value of the "Accept" header to add.
             * @return This request builder.
             */
            public Builder accept(String type) {
                return withHeader("Accept", type);
            }

            /**
             * Set the request method.
             *
             * @param method An HTTP method.
             * @return This request builder.
             */
            public final Builder withMethod(Method method) {
                this.method = method;
                return this;
            }

            /**
             * Add a "Content-Encoding" header of "gzip" and compress the request body.
             *
             * @return This request builder.
             * @throws IOException If compression fails.
             */
            public final Builder compress() throws IOException {
                withHeader("Content-Encoding", "gzip");
                this.entity = gzip(entity);
                return this;
            }

            /**
             * Add a "Content-Encoding" header of "gzip" and compress the request body when the supplied
             * condition is true.
             *
             * @param when Condition that governs when to compress the request body.
             * @return This request builder.
             * @throws IOException If compression fails.
             */
            public final Builder compressWhen(Supplier<Boolean> when) throws IOException {
                if (when.get())
                    return compress();
                return this;
            }

            public final Builder withMultipartFile(File file) throws IOException {
                String boundary = UUID.randomUUID().toString();
                String contentType = "multipart/form-data; boundary=" + boundary;
                byte[] content = multipart(boundary, file);
                return withContent(contentType, content);
            }

            private byte[] multipart(String boundary, File file) throws IOException {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                String mimeType = Files.probeContentType(file.toPath());
                if (mimeType == null) {
                    mimeType = "application/octet-stream";
                }
                outputStream.write(("--" + boundary + "\r\n").getBytes());
                outputStream.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getName() + "\"\r\n").getBytes());
                outputStream.write(("Content-Type: " + mimeType + "\r\n").getBytes());
                outputStream.write(("\r\n--" + boundary + "--\r\n").getBytes());
                FileInputStream fileInputStream = new FileInputStream(file);
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                fileInputStream.close();
                return outputStream.toByteArray();
            }

            private static byte[] gzip(byte[] data) throws IOException {
                ByteArrayOutputStream bos = new ByteArrayOutputStream(data.length);
                try (GZIPOutputStream out = new GZIPOutputStream(bos)) {
                    out.write(data);
                }
                return bos.toByteArray();
            }

            public final Builder url(String url) {
                try {
                    this.url = URI.create(url).toURL();
                } catch (MalformedURLException e) {
                    throw new UncheckedIOException(e);
                }
                return this;
            }

            public Response send() {
                return sender.send(build());
            }

            public Request build() {
                return new Request(url, entity, method, requestHeaders);
            }
        }
    }

    class Response implements AutoCloseable {
        private final int code;
        private final InputStream body;
        private final Runnable onClose;

        public Response(int code, @Nullable InputStream body, Runnable onClose) {
            this.code = code;
            this.body = body;
            this.onClose = onClose;
        }

        public int getCode() {
            return code;
        }

        public InputStream getBody() {
            return new InputStream() {
                @Override
                public int read() throws IOException {
                    return body == null ? 0 : body.read();
                }

                @Override
                public void close() throws IOException {
                    if (body != null) {
                        body.close();
                    }
                    onClose.run();
                }
            };
        }

        public byte[] getBodyAsBytes() {
            if (body == null) {
                return new byte[0];
            }
            try (InputStream is = getBody()) {
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                int nRead;
                byte[] data = new byte[4];
                while ((nRead = is.read(data, 0, data.length)) != -1) {
                    buffer.write(data, 0, nRead);
                }
                buffer.flush();
                return buffer.toByteArray();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        public Response onSuccess(Consumer<Response> onSuccess) {
            switch (HttpStatusClass.valueOf(code)) {
                case INFORMATIONAL:
                case SUCCESS:
                    onSuccess.accept(this);
            }
            return this;
        }

        public Response onError(Consumer<Response> onError) {
            switch (HttpStatusClass.valueOf(code)) {
                case CLIENT_ERROR:
                case SERVER_ERROR:
                    onError.accept(this);
            }
            return this;
        }

        public boolean isSuccessful() {
            switch (HttpStatusClass.valueOf(code)) {
                case INFORMATIONAL:
                case SUCCESS:
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public void close() {
            onClose.run();
        }
    }

    enum Method {
        GET, HEAD, POST, PUT, DELETE, OPTIONS
    }
}
