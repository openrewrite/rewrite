/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.internal.lang.NonNull;
import org.openrewrite.internal.lang.Nullable;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Objects.requireNonNull;

@Value
@EqualsAndHashCode(callSuper = false)
public class CreateFileVisitor extends TreeVisitor<Tree, ExecutionContext> {

    @NonNull Path relativeFileName;

    @NonNull AtomicBoolean shouldCreate;

    @Override
    public Tree visit(@Nullable Tree tree, @NonNull ExecutionContext ctx) {
        SourceFile sourceFile = (SourceFile) requireNonNull(tree);
        if (relativeFileName.equals(sourceFile.getSourcePath())) {
            shouldCreate.set(false);
        }
        return sourceFile;
    }
}
