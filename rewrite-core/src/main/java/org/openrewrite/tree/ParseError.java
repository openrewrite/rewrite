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
package org.openrewrite.tree;

import lombok.*;
import org.openrewrite.*;
import org.openrewrite.internal.EncodingDetectingInputStream;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Markers;
import org.openrewrite.service.ParseErrorSourceFileService;
import org.openrewrite.service.SourceFileService;

import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.UUID;

import static java.util.Collections.singletonList;

@Value
@With
public class ParseError implements SourceFile {
    @EqualsAndHashCode.Include
    UUID id;

    Markers markers;
    Path sourcePath;

    @Nullable
    FileAttributes fileAttributes;

    @Nullable // for backwards compatibility
    @With(AccessLevel.PRIVATE)
    String charsetName;

    @Override
    public Charset getCharset() {
        return charsetName == null ? StandardCharsets.UTF_8 : Charset.forName(charsetName);
    }

    @Override
    public <P> TreeVisitor<?, PrintOutputCapture<P>> printer(Cursor cursor) {
        return new ParseErrorPrinter<>();
    }

    @SuppressWarnings("unchecked")
    @Override
    public SourceFile withCharset(Charset charset) {
        return withCharsetName(charset.name());
    }

    boolean charsetBomMarked;

    @Nullable
    Checksum checksum;

    String text;

    /**
     * A parsed LST that was determined at parsing time to be erroneous, for
     * example if it doesn't faithfully produce the original source text at
     * printing time.
     */
    @Nullable
    SourceFile erroneous;

    @Override
    public <P> boolean isAcceptable(TreeVisitor<?, P> v, P p) {
        return v.isAdaptableTo(ParseErrorVisitor.class);
    }

    @Override
    public <R extends Tree, P> R accept(TreeVisitor<R, P> v, P p) {
        //noinspection unchecked
        return (R) v.adapt(ParseErrorVisitor.class).visitParseError(this, p);
    }

    public static ParseError build(Parser parser,
                                   Parser.Input input,
                                   @Nullable Path relativeTo,
                                   ExecutionContext ctx,
                                   Throwable t) {
        EncodingDetectingInputStream is = input.getSource(ctx);
        return new ParseError(
                Tree.randomId(),
                new Markers(Tree.randomId(), singletonList(ParseExceptionResult.build(parser, t))),
                input.getRelativePath(relativeTo),
                input.getFileAttributes(),
                parser.getCharset(ctx).name(),
                is.isCharsetBomMarked(),
                null,
                is.readFully(),
                null
        );
    }

    @Override
    public <S> S service(Class<S> service) {
        try {
            // use name indirection due to possibility of multiple class loaders being used
            String serviceName = service.getName();
            if (SourceFileService.class.getName().equals(serviceName)) {
                @SuppressWarnings("unchecked")
                Class<S> serviceClass = (Class<S>) service.getClassLoader().loadClass(ParseErrorSourceFileService.class.getName());
                return serviceClass.getConstructor().newInstance();
            } else {
                return SourceFile.super.service(service);
            }
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException |
                 ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
