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

import org.intellij.lang.annotations.Language;
import org.openrewrite.*;
import org.openrewrite.binary.BinaryVisitor;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.marker.Markers;

import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.UUID;

public interface Remote extends SourceFile {
    URI getUri();
    <R extends Remote> R withUri(URI uri);

    String getDescription();
    <R extends Remote> R withDescription(String description);

    InputStream getInputStream(HttpSender httpSender);

    @Override
    default <P> String printAll(P p) {
        return new String(printAllAsBytes(p));
    }

    @Override
    default <P> String printAllTrimmed(P p) {
        return StringUtils.trimIndentPreserveCRLF(printAll(p));
    }

    @Nullable
    default FileAttributes getFileAttributes() {
        throw new UnsupportedOperationException("Remote files do not have a file attributes.");
    }

    @Override
    default SourceFile withFileAttributes(@Nullable FileAttributes fileAttributes) {
        throw new UnsupportedOperationException("Remote files do not have a file attributes.");
    }

    @SuppressWarnings("unchecked")
    @Override
    default <R extends Tree, P> R accept(TreeVisitor<R, P> v, P p) {
        return (R) ((RemoteVisitor<P>) v).visitRemote(this, p);
    }

    @Override
    default <P> boolean isAcceptable(TreeVisitor<?, P> v, P p) {
        return v instanceof BinaryVisitor;
    }

    @Override
    default Charset getCharset() {
        throw new UnsupportedOperationException("Remote files do not have a character encoding.");
    }

    @Override
    default SourceFile withCharset(Charset charset) {
        throw new UnsupportedOperationException("Remote files do not have a character encoding.");
    }

    @Override
    default boolean isCharsetBomMarked() {
        throw new UnsupportedOperationException("Remote files do not have a character encoding.");
    }

    @Override
    default SourceFile withCharsetBomMarked(boolean marked) {
        throw new UnsupportedOperationException("Remote files do not have a character encoding.");
    }

    static Builder builder(SourceFile before, URI uri) {
        return new Builder(before.getId(), before.getSourcePath(), before.getMarkers(), uri);
    }

    static Builder builder(UUID id, Path sourcePath, Markers markers, URI uri) {
        return new Builder(id, sourcePath, markers, uri);
    }

    class Builder {
        protected final UUID id;
        protected final Path sourcePath;
        protected final Markers markers;
        protected final URI uri;

        @Nullable
        @Language("markdown")
        protected String description;

        @Nullable
        protected Checksum checksum;

        Builder(UUID id, Path sourcePath, Markers markers, URI uri) {
            this.id = id;
            this.sourcePath = sourcePath;
            this.markers = markers;
            this.uri = uri;
        }

        public Builder description(@Language("markdown") String description) {
            this.description = description;
            return this;
        }

        public Builder checksum(Checksum checksum) {
            this.checksum = checksum;
            return this;
        }

        public RemoteFile build() {
            return new RemoteFile(id, sourcePath, markers, uri, checksum, description);
        }

        public RemoteArchive build(Path path) {
            return new RemoteArchive(id, sourcePath, markers, uri, checksum, description, path);
        }
    }
}
