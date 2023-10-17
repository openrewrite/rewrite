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
package org.openrewrite.java.isolated;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaParser;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.tools.JavaFileObject;
import java.io.*;
import java.net.URI;
import java.nio.file.Path;

/**
 * So that {@link JavaParser} can ingest source files from {@link InputStream} sources
 * other than a file on disk.
 */
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ReloadableJava21ParserInputFileObject implements JavaFileObject {
    @EqualsAndHashCode.Include
    @Nullable
    private final Path path;

    @Getter
    private final Parser.Input input;

    private final ExecutionContext ctx;

    public ReloadableJava21ParserInputFileObject(Parser.Input input, ExecutionContext ctx) {
        this.input = input;
        this.path = input.getPath();
        this.ctx = ctx;
    }

    @Override
    public URI toUri() {
        if (path == null) {
            //noinspection ConstantConditions
            return null;
        }
        return path.toUri();
    }

    @Override
    public String getName() {
        if (path == null) {
            //noinspection ConstantConditions
            return null;
        }
        return path.toString();
    }

    @Override
    public InputStream openInputStream() {
        return input.getSource(ctx);
    }

    @Override
    public OutputStream openOutputStream() {
        throw new UnsupportedOperationException("Should be no need to write output to this file");
    }

    @Override
    public Reader openReader(boolean ignoreEncodingErrors) {
        return new InputStreamReader(input.getSource(ctx));
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) {
        return input.getSource(ctx).readFully();
    }

    @Override
    public Writer openWriter() {
        throw new UnsupportedOperationException("Should be no need to write output to this file");
    }

    @Override
    public long getLastModified() {
        return 0;
    }

    @Override
    public boolean delete() {
        return false;
    }

    @Override
    public Kind getKind() {
        return Kind.SOURCE;
    }

    @Override
    public boolean isNameCompatible(String simpleName, Kind kind) {
        String baseName = simpleName + kind.extension;
        return kind.equals(getKind())
                && path.getFileName().toString().equals(baseName);
    }

    @Override
    public NestingKind getNestingKind() {
        return NestingKind.TOP_LEVEL;
    }

    @Override
    public Modifier getAccessLevel() {
        return Modifier.PUBLIC;
    }
}
