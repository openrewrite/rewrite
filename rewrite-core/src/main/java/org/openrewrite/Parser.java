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
package org.openrewrite;

import org.openrewrite.internal.lang.Nullable;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static java.util.Collections.singletonList;

public interface Parser<S extends SourceFile> {
    List<S> parse(List<Path> sourceFiles, @Nullable Path relativeTo);

    default S parse(Path sourceFile, @Nullable Path relativeTo) {
        return parse(singletonList(sourceFile), relativeTo).iterator().next();
    }

    default List<S> parse(String... sources) {
        return parse(Arrays.asList(sources));
    }

    List<S> parse(List<String> sources);

    default Parser<S> reset() {
        return this;
    }
}
