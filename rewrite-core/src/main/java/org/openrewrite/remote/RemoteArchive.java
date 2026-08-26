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
package org.openrewrite.remote;

import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.With;
import org.intellij.lang.annotations.Language;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Checksum;
import org.openrewrite.ExecutionContext;
import org.openrewrite.FileAttributes;
import org.openrewrite.HttpSenderExecutionContextView;
import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.marker.Markers;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Represents a source to be extracted from within an archive hosted at a remote URI.
 * If you want to download and retain the entire archive, use {@link RemoteFile}.
 * Useful when a Recipe wishes to create a SourceFile based on something specific from within a remote archive, but not
 * the entire archive.
 * <p>
 * Downloading and extracting the correct file from within the archive are not handled during recipe execution.
 * Post-processing of recipe results by a build plugin or other caller of OpenRewrite is responsible for this.
 */
@Value
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@With
public class RemoteArchive implements Remote {
    @EqualsAndHashCode.Include
    UUID id;

    Path sourcePath;
    Markers markers;
    URI uri;

    @Nullable
    Charset charset;

    boolean charsetBomMarked;

    @Nullable
    FileAttributes fileAttributes;

    @Language("markdown")
    @Nullable
    String description;

    /**
     * A set of regular expressions that match consecutively nested paths within an archive, starting
     * with the path of the topmost archive itself. For example:
     * <p/>
     * <pre>
     *     gradle-[^\/]+\/(?:.*\/)+gradle-wrapper-(?!shared).*\.jar
     *     gradle-wrapper\.jar
     * </pre>
     */
    List<String> paths;

    @Nullable
    Checksum checksum;

    @Override
    public InputStream getInputStream(ExecutionContext ctx) {
        HttpSender httpSender = HttpSenderExecutionContextView.view(ctx).getLargeFileHttpSender();
        RemoteArtifactCache cache = RemoteExecutionContextView.view(ctx).getArtifactCache();

        Path extracted = extractedFile(cache, httpSender, ctx);
        if (extracted == null) {
            throw new IllegalStateException("Failed to download " + uri + " to artifact cache");
        }

        try {
            return Files.newInputStream(extracted);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to download " + uri + " to file", e);
        }
    }

    /**
     * Returns the cache path of the file extracted from within the archive, streaming the archive on a cache miss so
     * that only the small extracted entry is persisted rather than the entire (potentially very large) archive.
     */
    private @Nullable Path extractedFile(RemoteArtifactCache cache, HttpSender httpSender, ExecutionContext ctx) {
        URI cacheKey = extractedFileCacheKey();
        Path extracted = cache.get(cacheKey);
        if (extracted == null) {
            InputStream archive;
            try {
                archive = getArchiveInputStream(httpSender);
            } catch (Exception e) {
                ctx.getOnError().accept(e);
                throw new IllegalStateException("Failed to download " + uri + " to artifact cache");
            }
            try {
                InputStream inner = readIntoArchive(archive, paths, 0);
                if (inner == null) {
                    throw new IllegalArgumentException("Unable to find path " + paths + " in zip file " + uri);
                }
                extracted = cache.put(cacheKey, inner, ctx.getOnError());
            } catch (RuntimeException e) {
                closeQuietly(archive);
                throw e;
            }
        }
        return extracted;
    }

    private static void closeQuietly(InputStream is) {
        try {
            is.close();
        } catch (IOException ignored) {
            // Suppress
        }
    }

    /**
     * A cache key that identifies the file extracted from within the archive rather than the archive itself, so that
     * extracting different entries from the same archive URI does not collide and only the extracted entry is stored.
     */
    private URI extractedFileCacheKey() {
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        // Encode each part separately and join with '!', which never appears in base64url output, so a regex path
        // containing '!' (such as the "(?!shared)" lookahead) cannot be confused with the separator between parts.
        StringBuilder key = new StringBuilder(encoder.encodeToString(uri.toString().getBytes(StandardCharsets.UTF_8)));
        for (String path : paths) {
            key.append('!').append(encoder.encodeToString(path.getBytes(StandardCharsets.UTF_8)));
        }
        return URI.create("archive:" + key);
    }

    private InputStream getArchiveInputStream(HttpSender httpSender) throws IOException {
        if ("file".equals(uri.getScheme())) {
            return Files.newInputStream(Paths.get(uri));
        }
        //noinspection resource
        HttpSender.Response response = httpSender.send(httpSender.get(uri.toString()).build());
        if (!response.isSuccessful()) {
            throw new IllegalStateException("Failed to download " + uri + " to artifact cache got an " + response.getCode());
        }
        InputStream body = response.getBody();
        if (!response.getHeaders().containsKey("Content-Length")) {
            return body;
        }
        long contentLength = Long.parseLong(response.getHeaders().get("Content-Length").get(0));
        return new FilterInputStream(body) {
            private long count;

            @Override
            public int read() throws IOException {
                int i = super.read();
                if (i != -1) {
                    if (++count > contentLength) {
                        throw new IOException("Too much data received");
                    }
                } else if (count < contentLength) {
                    throw new IOException("Unexpected end of stream");
                }
                return i;
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                int bytesRead = super.read(b, off, len);
                if (bytesRead > 0) {
                    count += bytesRead;
                    if (count > contentLength) {
                        throw new IOException("Too much data received");
                    }
                } else if (bytesRead == -1 && count < contentLength) {
                    throw new IOException("Unexpected end of stream");
                }
                return bytesRead;
            }
        };
    }

    private @Nullable InputStream readIntoArchive(InputStream body, List<String> paths, int index) {
        ZipInputStream zis = new ZipInputStream(body);
        Pattern pattern = Pattern.compile(paths.get(index));

        try {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (pattern.matcher(entry.getName()).matches()) {
                    if (paths.size() == index + 1) {
                        return new InputStream() {
                            @Override
                            public int read() throws IOException {
                                return zis.read();
                            }

                            @Override
                            public int read(byte[] b, int off, int len) throws IOException {
                                return zis.read(b, off, len);
                            }

                            @Override
                            public void close() throws IOException {
                                zis.closeEntry();
                                zis.close();
                            }
                        };
                    } else {
                        InputStream maybeInputStream = readIntoArchive(zis, paths, index + 1);
                        if (maybeInputStream != null) {
                            return maybeInputStream;
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to load path " + paths + " in zip file " + uri, e);
        }
        return null;
    }
}
