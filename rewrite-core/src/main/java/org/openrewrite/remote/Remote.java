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

import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.openrewrite.Checksum;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.binary.BinaryVisitor;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.ipc.http.HttpUrlConnectionSender;
import org.openrewrite.marker.Markers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.UUID;

@ToString
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@RequiredArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Remote implements SourceFile {
    @With
    @Getter
    @EqualsAndHashCode.Include
    UUID id;

    @With
    @Getter
    Path sourcePath;

    @With
    @Getter
    Markers markers;

    @With
    @Getter
    URI uri;

    @Getter
    @With
    @Nullable
    URI checksumUri;

    @Getter
    @With
    @Nullable
    String checksumAlgorithm;

    @NonFinal
    @Nullable
    transient Checksum checksum;

    @Override
    @Nullable
    public Checksum getChecksum() {
        return getChecksum(new HttpUrlConnectionSender());
    }

    @Nullable
    public Checksum getChecksum(HttpSender httpSender) {
        if (checksum == null) {
            if (checksumUri != null && checksumAlgorithm != null) {
                //noinspection resource
                checksum = Checksum.fromHex(checksumAlgorithm, new String(
                        httpSender.get(checksumUri.toString()).send().getBodyAsBytes()));
            }
        }
        return checksum;
    }

    @Override
    public SourceFile withChecksum(@Nullable Checksum checksum) {
        throw new UnsupportedOperationException("A remote file's checksum cannot be changed, since it is determined by the remote source.");
    }

    @Override
    public <P> boolean isAcceptable(TreeVisitor<?, P> v, P p) {
        return v instanceof BinaryVisitor;
    }

    @Override
    public Charset getCharset() {
        throw new UnsupportedOperationException("Remote files do not have a character encoding.");
    }

    @Override
    public SourceFile withCharset(Charset charset) {
        throw new UnsupportedOperationException("Remote files do not have a character encoding.");
    }

    @Override
    public boolean isCharsetBomMarked() {
        throw new UnsupportedOperationException("Remote files do not have a character encoding.");
    }

    @Override
    public SourceFile withCharsetBomMarked(boolean marked) {
        throw new UnsupportedOperationException("Remote files do not have a character encoding.");
    }

    @Override
    public <P> byte[] printAllAsBytes(P p) {
        //noinspection resource
        return new HttpUrlConnectionSender().get(uri.toString()).send().getBodyAsBytes();
    }

    public InputStream getBytes(HttpSender httpSender) {
        //noinspection resource
        return httpSender.get(uri.toString()).send().getBody();
    }

    @Override
    public <P> String printAll(P p) {
        return new String(printAllAsBytes(p));
    }

    @Override
    public <P> String printAllTrimmed(P p) {
        return StringUtils.trimIndentPreserveCRLF(printAll(p));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R extends Tree, P> R accept(TreeVisitor<R, P> v, P p) {
        return (R) ((RemoteVisitor<P>) v).visitRemote(this, p);
    }

    public static Builder builder(SourceFile before, URI uri) {
        return new Builder(before.getId(), before.getSourcePath(), before.getMarkers(), uri);
    }

    public static Builder builder(UUID id, Path sourcePath, Markers markers, URI uri) {
        return new Builder(id, sourcePath, markers, uri);
    }

    public static class Builder {
        private final UUID id;
        private final Path sourcePath;
        private final Markers markers;
        private final URI uri;

        @Nullable
        private URI checksumUri;

        @Nullable
        private String checksumAlgorithm;

        Builder(UUID id, Path sourcePath, Markers markers, URI uri) {
            this.id = id;
            this.sourcePath = sourcePath;
            this.markers = markers;
            this.uri = uri;
        }

        public Builder checksum(String algorithm, URI uri) {
            this.checksumUri = uri;
            this.checksumAlgorithm = algorithm;
            return this;
        }

        public Remote build() {
            return new Remote(id, sourcePath, markers, uri, checksumUri, checksumAlgorithm);
        }
    }
}
