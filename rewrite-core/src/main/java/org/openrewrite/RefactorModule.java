/*
 * Copyright 2020 the original authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite;

import java.util.*;

import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

public interface RefactorModule<S extends SourceFile, T extends Tree> {
    List<SourceVisitor<T>> getVisitors();

    /**
     * @return A list of outputs that will certainly be affected by this module, in addition to source file inputs
     * provided to {@link RefactorModule#plan}. These outputs may represent files that need to change but aren't an input
     * or new source files to be generated.
     */
    default List<S> getDeclaredOutputs() {
        return emptyList();
    }

    static <S extends SourceFile, T extends Tree> List<Refactor<S, T>> plan(
            Collection<S> sources,
            RefactorModule<S, T>... modules) {

        Set<S> allSources = new HashSet<>(sources);

        for (RefactorModule<S, T> module : modules) {
            // Declared outputs from each module can add to the set of transformable sources. We will apply
            // visitors from all modules to the combined set of transformable sources contributed by each module.
            // In this way, for example, a module can cause a new source file to be generated, and formatting modules
            // can format the new generated source in a manner that best suits the project being operated on.
            allSources.addAll(module.getDeclaredOutputs());
        }

        return allSources.stream().map(s -> {
            Refactor<S, T> refactor = new Refactor<>(s);
            stream(modules).forEach(mod -> mod.getVisitors().forEach(refactor::visit));
            return refactor;
        }).collect(toList());
    }
}
