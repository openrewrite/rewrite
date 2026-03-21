/*
 * Copyright 2025 the original author or authors.
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

import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.binary.Binary;
import org.openrewrite.quark.Quark;
import org.openrewrite.remote.Remote;

import static java.util.Objects.requireNonNull;

public class RemoveByteOrderMark extends Recipe {

    @Getter
    final String displayName = "Remove byte order mark (BOM)";

    @Getter
    final String description = "Removes UTF-8 byte order marks from the beginning of files.\n\n" +
        "The BOM character (U+FEFF) is generally unnecessary in UTF-8 files and can cause issues with some tools.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                SourceFile sourceFile = (SourceFile) requireNonNull(tree);
                if (sourceFile instanceof Quark || sourceFile instanceof Remote || sourceFile instanceof Binary) {
                    return sourceFile;
                }

                try {
                    if (sourceFile.isCharsetBomMarked()) {
                        return sourceFile.withCharsetBomMarked(false);
                    }
                } catch (UnsupportedOperationException e) {
                    // Defensive fallback for any other SourceFile implementations that don't support charset operations
                }

                return sourceFile;
            }
        };
    }
}
