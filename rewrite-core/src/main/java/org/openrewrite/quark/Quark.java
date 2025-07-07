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
package org.openrewrite.quark;

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
public class Quark implements SourceFile {
    UUID id;
    Path sourcePath;
    Markers markers;

    @Nullable
    Checksum checksum;

    @Nullable
    FileAttributes fileAttributes;

    @Override
    public <P> boolean isAcceptable(TreeVisitor<?, P> v, P p) {
        return v.isAdaptableTo(QuarkVisitor.class);
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public <R extends Tree, P> @Nullable R accept(TreeVisitor<R, P> v, P p) {
        //noinspection unchecked
        return (R) v.adapt(QuarkVisitor.class).visitQuark(this, p);
    }

    @Override
    public Charset getCharset() {
        throw new UnsupportedOperationException("The contents of a quark are unknown, so the charset is unknown.");
    }

    @SuppressWarnings("unchecked")
    @Override
    public SourceFile withCharset(Charset charset) {
        throw new UnsupportedOperationException("The contents of a quark are unknown, so the charset is unknown.");
    }

    @Override
    public boolean isCharsetBomMarked() {
        throw new UnsupportedOperationException("The contents of a quark are unknown, so the charset is unknown.");
    }

    @SuppressWarnings("unchecked")
    @Override
    public SourceFile withCharsetBomMarked(boolean marked) {
        throw new UnsupportedOperationException("The contents of a quark are unknown, so the charset is unknown.");
    }

    @Override
    public <P> TreeVisitor<?, PrintOutputCapture<P>> printer(Cursor cursor) {
        return new QuarkPrinter<>();
    }
}
