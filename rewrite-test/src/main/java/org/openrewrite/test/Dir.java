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
package org.openrewrite.test;

import org.openrewrite.SourceFile;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

public class Dir implements Iterable<SourceSpec<?>>, SourceSpecs {
    private final String dir;
    private final Consumer<SourceSpec<SourceFile>> spec;
    private final SourceSpecs[] sourceSpecs;

    public Dir(String dir, Consumer<SourceSpec<SourceFile>> spec, SourceSpecs... sourceSpecs) {
        // Prevent invalid paths such as `<project>` passed into `mavenProject(...)` where `pomXml` should be used
        if (!dir.matches("[- \\w/\\\\]+")) {
            throw new IllegalArgumentException("Invalid directory: " + dir);
        }
        this.dir = dir;
        this.spec = spec;
        this.sourceSpecs = sourceSpecs;
    }

    @Override
    public Iterator<SourceSpec<?>> iterator() {
        List<SourceSpec<?>> asList = new ArrayList<>();
        for (SourceSpecs many : sourceSpecs) {
            for (SourceSpec<?> sourceSpec : many) {
                //noinspection unchecked
                SourceSpec<SourceFile> boxed = (SourceSpec<SourceFile>) sourceSpec;
                spec.accept(boxed);
                boxed.dir = Paths.get(dir).resolve(boxed.dir);
                asList.add(boxed);
            }
        }
        return asList.iterator();
    }
}
