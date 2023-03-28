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
package org.openrewrite.java.internal;

import org.openrewrite.ExecutionContext;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.marker.JavaSourceSet;
import org.openrewrite.java.tree.J;

import java.net.URI;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

public abstract class ForwardingJavaParser implements JavaParser {

    protected ForwardingJavaParser() {
    }

    protected abstract JavaParser delegate();

    @Override
    public List<J.CompilationUnit> parseInputs(Iterable<Input> sources, @Nullable Path relativeTo, ExecutionContext ctx) {
        return delegate().parseInputs(sources, relativeTo, ctx);
    }

    @Override
    public JavaParser reset() {
        delegate().reset();
        return this;
    }

    @Override
    public JavaParser reset(Collection<URI> uris) {
        delegate().reset(uris);
        return this;
    }

    @Override
    public void setClasspath(Collection<Path> classpath) {
        delegate().setClasspath(classpath);
    }

    @Override
    public void setSourceSet(String sourceSet) {
        delegate().setSourceSet(sourceSet);
    }

    @Override
    public JavaSourceSet getSourceSet(ExecutionContext ctx) {
        return delegate().getSourceSet(ctx);
    }
}
