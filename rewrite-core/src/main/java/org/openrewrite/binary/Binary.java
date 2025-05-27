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
package org.openrewrite.binary;

import lombok.Value;
import lombok.With;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.marker.Markers;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.UUID;

@Value
@With
public class Binary implements SourceFile, Tree {
    UUID id;
    Path sourcePath;
    Markers markers;

    @Nullable
    FileAttributes fileAttributes;

    @Nullable
    Checksum checksum;

    byte[] bytes;

    @Override
    public <P> boolean isAcceptable(TreeVisitor<?, P> v, P p) {
        return v.isAdaptableTo(BinaryVisitor.class);
    }

    @Override
    public Charset getCharset() {
        throw new UnsupportedOperationException("Binary files do not have a character encoding.");
    }

    @Override
    public SourceFile withCharset(Charset charset) {
        throw new UnsupportedOperationException("Binary files do not have a character encoding.");
    }

    @Override
    public boolean isCharsetBomMarked() {
        throw new UnsupportedOperationException("Binary files do not have a character encoding.");
    }

    @Override
    public SourceFile withCharsetBomMarked(boolean marked) {
        throw new UnsupportedOperationException("Binary files do not have a character encoding.");
    }

    @Override
    public <P> byte[] printAllAsBytes(P p) {
        return bytes;
    }

    @Override
    public <P> String printAll(P p) {
        throw new UnsupportedOperationException("Cannot print a binary as a string.");
    }

    @Override
    public <P> String printAllTrimmed(P p) {
        throw new UnsupportedOperationException("Cannot print a binary as a string.");
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R extends Tree, P> R accept(TreeVisitor<R, P> v, P p) {
        return (R) v.adapt(BinaryVisitor.class).visitBinary(this, p);
    }
}
