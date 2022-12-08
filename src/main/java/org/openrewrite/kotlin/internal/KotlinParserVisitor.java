/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.kotlin.internal;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.fir.FirElement;
import org.jetbrains.kotlin.fir.declarations.FirClass;
import org.jetbrains.kotlin.fir.declarations.FirFile;
import org.jetbrains.kotlin.fir.expressions.FirBlock;
import org.jetbrains.kotlin.fir.visitors.FirVisitor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.FileAttributes;
import org.openrewrite.internal.EncodingDetectingInputStream;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.internal.JavaTypeCache;
import org.openrewrite.kotlin.KotlinTypeMapping;
import org.openrewrite.kotlin.tree.K;

import java.nio.charset.Charset;
import java.nio.file.Path;

// todo: find visitor with type attribution and replace FirVisitor.
public class KotlinParserVisitor extends FirVisitor<K, ExecutionContext> {
    private final Path sourcePath;

    @Nullable
    private final FileAttributes fileAttributes;
    private final String source;
    private final Charset charset;
    private final boolean charsetBomMarked;
    private final KotlinTypeMapping typeMapping;
    private final ExecutionContext ctx;

    private int cursor = 0;

    public KotlinParserVisitor(Path sourcePath, @Nullable FileAttributes fileAttributes, EncodingDetectingInputStream source, JavaTypeCache typeCache, ExecutionContext ctx) {
        this.sourcePath = sourcePath;
        this.fileAttributes = fileAttributes;
        this.source = source.readFully();
        this.charset = source.getCharset();
        this.charsetBomMarked = source.isCharsetBomMarked();
        this.typeMapping = new KotlinTypeMapping(typeCache);
        this.ctx = ctx;
    }

    @Override
    public K visitFile(@NotNull FirFile file, ExecutionContext ctx) {
        return super.visitFile(file, ctx);
    }

    @Override
    public K visitBlock(@NotNull FirBlock block, ExecutionContext data) {
        return super.visitBlock(block, data);
    }

    @Override
    public K visitClass(@NotNull FirClass klass, ExecutionContext ctx) {
        return super.visitClass(klass, ctx);
    }

    @Override
    public K visitElement(@NotNull FirElement firElement, ExecutionContext ctx) {
        return null;
    }
}
