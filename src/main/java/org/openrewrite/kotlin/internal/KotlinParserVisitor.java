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
package org.openrewrite.kotlin.internal;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.fir.FirElement;
import org.jetbrains.kotlin.fir.declarations.FirFile;
import org.jetbrains.kotlin.fir.declarations.FirImport;
import org.jetbrains.kotlin.fir.declarations.FirResolvedImport;
import org.jetbrains.kotlin.fir.visitors.FirVisitor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.FileAttributes;
import org.openrewrite.internal.EncodingDetectingInputStream;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.internal.JavaTypeCache;
import org.openrewrite.java.tree.*;
import org.openrewrite.kotlin.KotlinTypeMapping;
import org.openrewrite.kotlin.marker.Semicolon;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.marker.Markers;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.internal.StringUtils.indexOfNextNonWhitespace;
import static org.openrewrite.java.tree.Space.EMPTY;
import static org.openrewrite.java.tree.Space.format;

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
    public K visitElement(@NotNull FirElement firElement, ExecutionContext ctx) {
        if (firElement instanceof FirImport) {
            return visitImport((FirImport) firElement, ctx);
        } else {
            throw new IllegalStateException("Implement me.");
        }
    }

    @Override
    public K visitFile(@NotNull FirFile file, ExecutionContext ctx) {
        JRightPadded<J.Package> pkg = null;
        if (!file.getPackageDirective().getPackageFqName().isRoot()) {
            Space prefix = whitespace();
            cursor += "package".length();
            Space space = whitespace();
            pkg = JRightPadded.build(new J.Package(
                    randomId(),
                    prefix,
                    Markers.EMPTY,
                    TypeTree.build(file.getPackageDirective().getPackageFqName().asString())
                            .withPrefix(space),
                    emptyList()));
        }

        List<JRightPadded<Statement>> statements = Stream.of(file.getImports(), file.getDeclarations())
                .flatMap(Collection::stream)
                .sorted(Comparator.comparingInt(it -> it.getSource().getStartOffset()))
                .map(this::convertTopLevelStatement)
                .collect(Collectors.toList());

        return new K.CompilationUnit(
                randomId(),
                null,
                Space.EMPTY,
                Markers.EMPTY,
                sourcePath,
                fileAttributes,
                charset.name(),
                charsetBomMarked,
                null,
                pkg,
                statements,
                Space.EMPTY
        );
    }

    private JRightPadded<Statement> convertTopLevelStatement(FirElement element) {
        Statement statement = null;
        if (element instanceof FirResolvedImport) {
            FirResolvedImport firResolvedImport = (FirResolvedImport) element;

            Space prefix = sourceBefore("import");
            JLeftPadded<Boolean> statik = padLeft(EMPTY, false);

            J.FieldAccess qualid;
            if (firResolvedImport.getImportedFqName() == null) {
                throw new IllegalStateException("implement me.");
            } else {
                Space space = whitespace();
                String packageName = firResolvedImport.isAllUnder() ?
                        firResolvedImport.getImportedFqName().asString() + ".*" :
                        firResolvedImport.getImportedFqName().asString();
                qualid = TypeTree.build(packageName).withPrefix(space);
            }
            statement = new J.Import(randomId(), prefix, Markers.EMPTY, statik, qualid);
        }

        if (statement == null) {
            throw new IllegalStateException("implement me.");
        }

        return maybeSemicolon(statement);
    }

    private <K2 extends J> JRightPadded<K2> maybeSemicolon(K2 k) {
        int saveCursor = cursor;
        Space beforeSemi = whitespace();
        Semicolon semicolon = null;
        if (cursor < source.length() && source.charAt(cursor) == ';') {
            semicolon = new Semicolon(randomId());
            cursor++;
        } else {
            beforeSemi = EMPTY;
            cursor = saveCursor;
        }

        JRightPadded<K2> padded = JRightPadded.build(k).withAfter(beforeSemi);
        if (semicolon != null) {
            padded = padded.withMarkers(padded.getMarkers().add(semicolon));
        }

        return padded;
    }

    private <T> JLeftPadded<T> padLeft(Space left, T tree) {
        return new JLeftPadded<>(left, tree, Markers.EMPTY);
    }

    private int positionOfNext(String untilDelim) {
        boolean inMultiLineComment = false;
        boolean inSingleLineComment = false;

        int delimIndex = cursor;
        for (; delimIndex < source.length() - untilDelim.length() + 1; delimIndex++) {
            if (inSingleLineComment) {
                if (source.charAt(delimIndex) == '\n') {
                    inSingleLineComment = false;
                }
            } else {
                if (source.length() - untilDelim.length() > delimIndex + 1) {
                    switch (source.substring(delimIndex, delimIndex + 2)) {
                        case "//":
                            inSingleLineComment = true;
                            delimIndex++;
                            break;
                        case "/*":
                            inMultiLineComment = true;
                            delimIndex++;
                            break;
                        case "*/":
                            inMultiLineComment = false;
                            delimIndex = delimIndex + 2;
                            break;
                    }
                }

                if (!inMultiLineComment && !inSingleLineComment) {
                    if (source.startsWith(untilDelim, delimIndex)) {
                        break; // found it!
                    }
                }
            }
        }

        return delimIndex > source.length() - untilDelim.length() ? -1 : delimIndex;
    }

    private Space sourceBefore(String untilDelim) {
        int delimIndex = positionOfNext(untilDelim);
        if (delimIndex < 0) {
            return EMPTY; // unable to find this delimiter
        }

        String prefix = source.substring(cursor, delimIndex);
        cursor += prefix.length() + untilDelim.length(); // advance past the delimiter
        return Space.format(prefix);
    }

    private Space whitespace() {
        String prefix = source.substring(cursor, indexOfNextNonWhitespace(cursor, source));
        cursor += prefix.length();
        return format(prefix);
    }
}
