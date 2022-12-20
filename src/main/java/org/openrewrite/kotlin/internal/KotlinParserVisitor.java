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
import org.jetbrains.kotlin.KtRealPsiSourceElement;
import org.jetbrains.kotlin.com.intellij.psi.PsiElement;
import org.jetbrains.kotlin.descriptors.ClassKind;
import org.jetbrains.kotlin.fir.FirElement;
import org.jetbrains.kotlin.fir.FirPackageDirective;
import org.jetbrains.kotlin.fir.declarations.FirClass;
import org.jetbrains.kotlin.fir.declarations.FirFile;
import org.jetbrains.kotlin.fir.declarations.FirImport;
import org.jetbrains.kotlin.fir.declarations.FirRegularClass;
import org.jetbrains.kotlin.fir.visitors.FirVisitor;
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken;
import org.jetbrains.kotlin.psi.KtDeclarationModifierList;
import org.jetbrains.kotlin.psi.psiUtil.PsiChildRange;
import org.jetbrains.kotlin.psi.psiUtil.PsiUtilsKt;
import org.openrewrite.ExecutionContext;
import org.openrewrite.FileAttributes;
import org.openrewrite.internal.EncodingDetectingInputStream;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.internal.JavaTypeCache;
import org.openrewrite.java.tree.*;
import org.openrewrite.kotlin.KotlinTypeMapping;
import org.openrewrite.kotlin.marker.EmptyBody;
import org.openrewrite.kotlin.marker.Semicolon;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.marker.Markers;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.internal.StringUtils.indexOfNextNonWhitespace;
import static org.openrewrite.java.tree.Space.EMPTY;
import static org.openrewrite.java.tree.Space.format;

public class KotlinParserVisitor extends FirVisitor<J, ExecutionContext> {
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
    public J visitElement(@NotNull FirElement firElement, ExecutionContext ctx) {
        throw new IllegalStateException("Implement me.");
    }

    @Override
    public J visitFile(@NotNull FirFile file, ExecutionContext ctx) {
        JRightPadded<J.Package> pkg = null;
        if (!file.getPackageDirective().getPackageFqName().isRoot()) {
            pkg = maybeSemicolon((J.Package) visitPackageDirective(file.getPackageDirective(), ctx));
        }

        List<JRightPadded<J.Import>> imports = file.getImports().stream()
                .map(it -> maybeSemicolon((J.Import) visitImport(it, ctx)))
                .collect(Collectors.toList());

        List<J.ClassDeclaration> classes = file.getDeclarations().stream()
                .filter(it -> it instanceof FirClass)
                .map(it -> (J.ClassDeclaration) visitClass((FirClass) it, ctx))
                .collect(Collectors.toList());

        return new K.CompilationUnit(
                randomId(),
                Space.EMPTY,
                Markers.EMPTY,
                sourcePath,
                fileAttributes,
                charset.name(),
                charsetBomMarked,
                null,
                pkg,
                imports,
                classes,
                Space.EMPTY
        );
    }

    @Override
    public J visitClass(@NotNull FirClass klass, ExecutionContext data) {
        if (!(klass instanceof FirRegularClass)) {
            throw new IllegalStateException("Implement me.");
        }

        FirRegularClass firRegularClass = (FirRegularClass) klass;
        Space prefix = whitespace();

        // Not used until it's possible to handle K.Modifiers.
        List<K.Modifier> modifiers = emptyList();
        if (((KtRealPsiSourceElement) firRegularClass.getSource()) != null) {
            PsiChildRange psiChildRange = PsiUtilsKt.getAllChildren(((KtRealPsiSourceElement) firRegularClass.getSource()).getPsi());
            if (psiChildRange.getFirst() instanceof KtDeclarationModifierList) {
                KtDeclarationModifierList modifierList = (KtDeclarationModifierList) psiChildRange.getFirst();
                modifiers = getModifiers(modifierList);
            }
        }

        List<J.Annotation> kindAnnotations = emptyList(); // TODO

        J.ClassDeclaration.Kind kind;
        ClassKind classKind = klass.getClassKind();
        if (ClassKind.INTERFACE == classKind) {
            kind = new J.ClassDeclaration.Kind(randomId(), sourceBefore("interface"), Markers.EMPTY, kindAnnotations, J.ClassDeclaration.Kind.Type.Interface);
        } else if (ClassKind.CLASS == classKind || ClassKind.ENUM_CLASS == classKind || ClassKind.ANNOTATION_CLASS == classKind) {
            // Enums and Interfaces are modifiers in kotlin and require the modifier prefix to preserve source code.
            kind = new J.ClassDeclaration.Kind(randomId(), sourceBefore("class"), Markers.EMPTY, kindAnnotations, J.ClassDeclaration.Kind.Type.Class);
        } else {
            throw new IllegalStateException("Implement me.");
        }

        // TODO: add type mapping
        J.Identifier name = new J.Identifier(randomId(), sourceBefore(firRegularClass.getName().asString()),
                Markers.EMPTY, firRegularClass.getName().asString(), null, null);

        // TODO: fix: super type references are resolved as error kind.
        JLeftPadded<TypeTree> extendings = null;

        // TODO: fix: super type references are resolved as error kind.
        JContainer<TypeTree> implementings = null;

        Space bodyPrefix = whitespace();
        EmptyBody emptyBody = null;
        if (source.substring(cursor).isEmpty() || !source.substring(cursor).startsWith("{")) {
            emptyBody = new EmptyBody(randomId());
        } else {
            cursor++; // Increment past the `{`
        }

        J.Block body = new J.Block(randomId(), bodyPrefix, Markers.EMPTY, new JRightPadded<>(false, EMPTY, Markers.EMPTY),
                emptyList(), sourceBefore("}"));

        if (emptyBody != null) {
            body = body.withMarkers(body.getMarkers().addIfAbsent(emptyBody));
        }

        return new J.ClassDeclaration(
                randomId(),
                prefix,
                Markers.EMPTY,
                emptyList(), // TODO
                emptyList(), // TODO
                kind,
                name, // TODO
                null, // TODO
                null, // TODO
                extendings, // TODO
                implementings,
                body, // TODO
                null // TODO
        );
    }

    private List<K.Modifier> getModifiers(KtDeclarationModifierList modifierList) {
        List<K.Modifier> modifiers = new ArrayList<>();
        PsiElement current = modifierList.getFirstChild();
        while (current != null) {
            if (current.getNode().getElementType() instanceof KtModifierKeywordToken) {
                KtModifierKeywordToken token = (KtModifierKeywordToken) current.getNode().getElementType();
                List<J.Annotation> annotations = new ArrayList<>(); // TODO
                K.Modifier modifier = mapModifier(token, annotations);
                modifiers.add(modifier);
            }
            current = current.getNextSibling();
        }
        return modifiers;
    }

    // TODO: confirm this works for all types of kotlin modifiers.
    private K.Modifier mapModifier(KtModifierKeywordToken mod, List<J.Annotation> annotations) {
        Space modFormat = whitespace();
        cursor += mod.getValue().length();
        K.Modifier.Type type;
        // Ordered based on kotlin requirements.
        switch (mod.getValue()) {
            case "public":
                type = K.Modifier.Type.Public;
                break;
            case "protected":
                type = K.Modifier.Type.Protected;
                break;
            case "private":
                type = K.Modifier.Type.Private;
                break;
            case "internal":
                type = K.Modifier.Type.Internal;
                break;
            case "expect":
                type = K.Modifier.Type.Expect;
                break;
            case "actual":
                type = K.Modifier.Type.Actual;
                break;
            case "final":
                type = K.Modifier.Type.Final;
                break;
            case "open":
                type = K.Modifier.Type.Open;
                break;
            case "abstract":
                type = K.Modifier.Type.Abstract;
                break;
            case "sealed":
                type = K.Modifier.Type.Sealed;
                break;
            case "const":
                type = K.Modifier.Type.Const;
                break;
            case "external":
                type = K.Modifier.Type.External;
                break;
            case "override":
                type = K.Modifier.Type.Override;
                break;
            case "lateinit":
                type = K.Modifier.Type.LateInit;
                break;
            case "tailrec":
                type = K.Modifier.Type.TailRec;
                break;
            case "vararg":
                type = K.Modifier.Type.Vararg;
                break;
            case "suspend":
                type = K.Modifier.Type.Suspend;
                break;
            case "inner":
                type = K.Modifier.Type.Inner;
                break;
            case "enum":
                type = K.Modifier.Type.Enum;
                break;
            case "annotation":
                type = K.Modifier.Type.Annotation;
                break;
            case "fun":
                type = K.Modifier.Type.Fun;
                break;
            case "companion":
                type = K.Modifier.Type.Companion;
                break;
            case "inline":
                type = K.Modifier.Type.Inline;
                break;
            case "value":
                type = K.Modifier.Type.Value;
                break;
            case "infix":
                type = K.Modifier.Type.Infix;
                break;
            case "operator":
                type = K.Modifier.Type.Operator;
                break;
            case "data":
                type = K.Modifier.Type.Data;
                break;
            default:
                throw new IllegalArgumentException("Unexpected modifier " + mod);
        }
        return new K.Modifier(randomId(), modFormat, Markers.EMPTY, type, annotations);
    }

    @Override
    public J visitImport(@NotNull FirImport firImport, ExecutionContext data) {
        Space prefix = sourceBefore("import");
        JLeftPadded<Boolean> statik = padLeft(EMPTY, false);

        J.FieldAccess qualid;
        if (firImport.getImportedFqName() == null) {
            throw new IllegalStateException("implement me.");
        } else {
            Space space = whitespace();
            String packageName = firImport.isAllUnder() ?
                    firImport.getImportedFqName().asString() + ".*" :
                    firImport.getImportedFqName().asString();
            qualid = TypeTree.build(packageName).withPrefix(space);
        }
        return new J.Import(randomId(), prefix, Markers.EMPTY, statik, qualid);
    }

    @Override
    public J visitPackageDirective(@NotNull FirPackageDirective packageDirective, ExecutionContext data) {
        Space pkgPrefix = whitespace();
        cursor += "package".length();
        Space space = whitespace();

        return new J.Package(
                randomId(),
                pkgPrefix,
                Markers.EMPTY,
                TypeTree.build(packageDirective.getPackageFqName().asString())
                        .withPrefix(space),
                emptyList());
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

    private <T> JRightPadded<T> padRight(T tree, Space right) {
        return new JRightPadded<>(tree, right, Markers.EMPTY);
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
