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
package org.openrewrite.java.internal.template;

import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.internal.ForwardingJavaParser;
import org.openrewrite.java.internal.JavaTypeCache;
import org.openrewrite.java.tree.J;

import java.nio.file.Path;
import java.util.List;
import java.util.StringJoiner;

class JavaTypeCacheSharingJavaParser extends ForwardingJavaParser {
    private final JavaParser delegate;
    private final Cursor cursor;
    private final JavaTypeCache typeCache;
    private final String typeCacheKey;

    public JavaTypeCacheSharingJavaParser(Builder<?, ?> parserBuilder, Cursor cursor) {
        StringJoiner joiner = new StringJoiner(":", "type-cache-key(classpath=", ")");
        parserBuilder.classpath().forEach(p -> joiner.add(p.toString()));
        String typeCacheKey = joiner.toString();
        JavaTypeCache typeCache = cursor.getRoot().getMessage(typeCacheKey);
        JavaTypeCache finalTypeCache = typeCache == null ? new JavaTypeCache() : typeCache.clone();
        parserBuilder.typeCache(finalTypeCache);

        this.delegate = parserBuilder.build();
        this.cursor = cursor;
        this.typeCache = finalTypeCache;
        this.typeCacheKey = typeCacheKey;
    }

    @Override
    protected JavaParser delegate() {
        return delegate;
    }

    @Override
    public List<J.CompilationUnit> parseInputs(Iterable<Input> sources, @Nullable Path relativeTo, ExecutionContext ctx) {
        try {
            return delegate.parseInputs(sources, relativeTo, ctx);
        } finally {
            typeCache.clear();
            // TODO This is not perfect. Ideally the part from JavaTypeCache which gets loaded from classpath would itself be thread-safe and not need copying.
            cursor.putMessage(typeCacheKey, typeCache);
        }
    }
}
