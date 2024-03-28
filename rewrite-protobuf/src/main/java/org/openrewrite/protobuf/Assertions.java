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
package org.openrewrite.protobuf;

import org.intellij.lang.annotations.Language;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.protobuf.tree.Proto;
import org.openrewrite.test.SourceSpec;
import org.openrewrite.test.SourceSpecs;

import java.util.function.Consumer;

public final class Assertions {

    private Assertions() {
    }

    public static SourceSpecs proto(@Language("protobuf") @Nullable String before) {
        return proto(before, s -> {
        });
    }

    public static SourceSpecs proto(@Language("protobuf") @Nullable String before, Consumer<SourceSpec<Proto.Document>> spec) {
        SourceSpec<Proto.Document> proto = new SourceSpec<>(Proto.Document.class, null, ProtoParser.builder(), before, null);
        spec.accept(proto);
        return proto;
    }

    public static SourceSpecs proto(@Language("protobuf") @Nullable String before, @Language("protobuf") @Nullable String after) {
        return proto(before, after, s -> {
        });
    }

    public static SourceSpecs proto(@Language("protobuf") @Nullable String before, @Language("protobuf") @Nullable String after,
                              Consumer<SourceSpec<Proto.Document>> spec) {
        SourceSpec<Proto.Document> proto = new SourceSpec<>(Proto.Document.class, null, ProtoParser.builder(), before, s -> after);
        spec.accept(proto);
        return proto;
    }
}
