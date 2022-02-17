/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.text;

import lombok.Value;
import lombok.With;
import org.openrewrite.*;
import org.openrewrite.marker.Markers;

import java.nio.file.Path;
import java.util.UUID;

/**
 * The simplest of all ASTs representing nothing more than just unstructured text.
 */
@Value
public class PlainText implements SourceFile, Tree {
    UUID id;

    @With
    Path sourcePath;

    @With
    Markers markers;

    @With
    String text;

    @Override
    public <P> boolean isAcceptable(TreeVisitor<?, P> v, P p) {
        return v instanceof PlainTextVisitor && v.isAcceptable(this, p);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R extends Tree, P> R accept(TreeVisitor<R, P> v, P p) {
        return (R) ((PlainTextVisitor<P>) v).visitText(this, p);
    }

    @Override
    public <P> TreeVisitor<?, PrintOutputCapture<P>> printer(Cursor cursor) {
        return new PlainTextPrinter<>();
    }
}
