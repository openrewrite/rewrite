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
import org.openrewrite.Checksum;
import org.openrewrite.ExecutionContext;
import org.openrewrite.FileAttributes;
import org.openrewrite.HttpSenderExecutionContextView;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.marker.Markers;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
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
 * Downloading and extracting the correct file from within the archive are not handled during Recipe execution.
 * Post-processing of Recipe results by a build plugin or other caller of OpenRewrite is responsible for this.
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
        try {
            Path localArchive = cache.compute(uri, () -> {
                //noinspection resource
                HttpSender.Response response = httpSender.send(httpSender.get(uri.toString()).build());
                return response.getBody();
            }, ctx.getOnError());

            if (localArchive == null) {
                throw new IllegalStateException("Failed to download " + uri + " to artifact cache");
            }

            InputStream body = Files.newInputStream(localArchive);
            InputStream inner = readIntoArchive(body, paths, 0);
            if (inner == null) {
                throw new IllegalArgumentException("Unable to find path " + paths + " in zip file " + uri);
            }
            return inner;
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to download " + uri + " to file", e);
        }
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
