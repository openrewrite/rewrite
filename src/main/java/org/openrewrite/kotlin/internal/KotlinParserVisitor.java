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
import org.jetbrains.kotlin.com.intellij.lang.ASTNode;
import org.jetbrains.kotlin.com.intellij.psi.PsiElement;
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.*;
import org.jetbrains.kotlin.com.intellij.psi.tree.IElementType;
import org.jetbrains.kotlin.descriptors.ClassKind;
import org.jetbrains.kotlin.fir.FirElement;
import org.jetbrains.kotlin.fir.FirPackageDirective;
import org.jetbrains.kotlin.fir.declarations.*;
import org.jetbrains.kotlin.fir.expressions.FirConstExpression;
import org.jetbrains.kotlin.fir.types.FirQualifierPart;
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef;
import org.jetbrains.kotlin.fir.types.FirTypeRef;
import org.jetbrains.kotlin.fir.types.FirUserTypeRef;
import org.jetbrains.kotlin.fir.visitors.FirVisitor;
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.psi.psiUtil.PsiChildRange;
import org.jetbrains.kotlin.psi.psiUtil.PsiUtilsKt;
import org.jetbrains.kotlin.psi.stubs.elements.KtAnnotationEntryElementType;
import org.openrewrite.ExecutionContext;
import org.openrewrite.FileAttributes;
import org.openrewrite.internal.EncodingDetectingInputStream;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.internal.JavaTypeCache;
import org.openrewrite.java.tree.*;
import org.openrewrite.kotlin.KotlinTypeMapping;
import org.openrewrite.kotlin.marker.EmptyBody;
import org.openrewrite.kotlin.marker.VariableTypeConstraint;
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
    public J visitFile(@NotNull FirFile file, ExecutionContext executionContext) {
        JRightPadded<J.Package> pkg = null;
        if (!file.getPackageDirective().getPackageFqName().isRoot()) {
            pkg = maybeSemicolon((J.Package) visitPackageDirective(file.getPackageDirective(), executionContext));
        }

        List<JRightPadded<J.Import>> imports = file.getImports().stream()
                .map(it -> maybeSemicolon((J.Import) visitImport(it, executionContext)))
                .collect(Collectors.toList());


        List<JRightPadded<Statement>> statements = new ArrayList<>(file.getDeclarations().size());
        for (FirDeclaration declaration : file.getDeclarations()) {
            Statement statement = null;
            if (declaration instanceof FirClass) {
                statement = (Statement) visitClass((FirClass) declaration, ctx);
            } else if (declaration instanceof FirProperty) {
                statement = (Statement) visitProperty((FirProperty) declaration, ctx);
            } else {
                throw new IllegalStateException("Implement me.");
            }

            if (statement != null) {
                statements.add(JRightPadded.build(statement));
            }
        }

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
                statements,
                Space.EMPTY
        );
    }

    @Override
    public J visitClass(@NotNull FirClass klass, ExecutionContext executionContext) {
        if (!(klass instanceof FirRegularClass)) {
            throw new IllegalStateException("Implement me.");
        }

        FirRegularClass firRegularClass = (FirRegularClass) klass;
        Space prefix = whitespace();

        // Not used until it's possible to handle K.Modifiers.
        List<J> modifiers = emptyList();
        if (firRegularClass.getSource() != null) {
            PsiChildRange psiChildRange = PsiUtilsKt.getAllChildren(((KtRealPsiSourceElement) firRegularClass.getSource()).getPsi());
            if (psiChildRange.getFirst() instanceof KtDeclarationModifierList) {
                KtDeclarationModifierList modifierList = (KtDeclarationModifierList) psiChildRange.getFirst();
                modifiers = getModifiers(modifierList);
            }
        }

        List<J.Annotation> kindAnnotations = emptyList(); // TODO: the last annotations in modifiersAndAnnotations should be added to the class.

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
                emptyList(), // TODO: requires updates to handle kotlin specific modifiers.
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

    @Override
    public <T> J visitConstExpression(@NotNull FirConstExpression<T> constExpression, ExecutionContext data) {
        Space prefix = whitespace();
        Object value = constExpression.getValue();
        String valueSource = source.substring(constExpression.getSource().getStartOffset(), constExpression.getSource().getEndOffset());
        JavaType.Primitive type = null; // TODO: add type mapping.

        return new J.Literal(
                randomId(),
                prefix,
                Markers.EMPTY,
                value,
                valueSource,
                null,
                type);
    }

    @Override
    public J visitImport(@NotNull FirImport firImport, ExecutionContext executionContext) {
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
    public J visitPackageDirective(@NotNull FirPackageDirective packageDirective, ExecutionContext executionContext) {
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

    @Override
    public J visitProperty(@NotNull FirProperty property, ExecutionContext executionContext) {
        Space prefix = whitespace();

        List<J> modifiers = emptyList();
        if (property.getSource() != null) {
            PsiChildRange psiChildRange = PsiUtilsKt.getAllChildren(((KtRealPsiSourceElement) property.getSource()).getPsi());
            if (psiChildRange.getFirst() instanceof KtDeclarationModifierList) {
                KtDeclarationModifierList modifierList = (KtDeclarationModifierList) psiChildRange.getFirst();
                modifiers = getModifiers(modifierList);
            }
        }

        List<J.Annotation> annotations = emptyList(); // TODO: the last annotations in modifiers should be added.

        boolean isVal = property.isVal();
        J.Identifier typeExpression = new J.Identifier(
                randomId(),
                isVal ? sourceBefore("val") : sourceBefore("var"),
                Markers.EMPTY,
                isVal ? "val" : "var",
                null,
                null);

        List<JRightPadded<J.VariableDeclarations.NamedVariable>> vars = new ArrayList<>(1); // adjust size if necessary
        Space namePrefix = sourceBefore(property.getName().asString());

        J.Identifier name = new J.Identifier(
                randomId(),
                EMPTY,
                Markers.EMPTY,
                property.getName().asString(),
                null, // TODO: add type mapping and set type
                null); // TODO: add type mapping and set variable type

        TypeTree typeConstraint = null;
        if (property.getReturnTypeRef() instanceof FirResolvedTypeRef) {
            FirResolvedTypeRef typeRef = (FirResolvedTypeRef) property.getReturnTypeRef();
            if (typeRef.getDelegatedTypeRef() == null) {
                typeConstraint = null;
            } else {
                typeConstraint = (TypeTree) visitTypeRef(typeRef.getDelegatedTypeRef(), ctx);
            }
        } else {
            throw new IllegalStateException("Implement me.");
        }

        // Dimensions do not exist in Kotlin, and array is declared based on the type. I.E., IntArray
        List<JLeftPadded<Space>> dimensionsAfterName = emptyList();

        JRightPadded<J.VariableDeclarations.NamedVariable> namedVariable = maybeSemicolon(
                new J.VariableDeclarations.NamedVariable(
                        randomId(),
                        namePrefix,
                        typeConstraint == null ? Markers.EMPTY : Markers.EMPTY.addIfAbsent(new VariableTypeConstraint(randomId(), typeConstraint)),
                        name,
                        dimensionsAfterName,
                        property.getInitializer() != null ? padLeft(sourceBefore("="), (Expression) visitExpression(property.getInitializer(), ctx)) : null,
                        null // TODO: add type mapping
                )
        );
        vars.add(namedVariable);

        return new J.VariableDeclarations(
                randomId(),
                prefix,
                Markers.EMPTY,
                emptyList(),
                emptyList(), // TODO: requires updates to handle kotlin specific modifiers.
                typeExpression,
                null,
                dimensionsAfterName,
                vars);
    }

    @Override
    public J visitTypeRef(@NotNull FirTypeRef typeRef, ExecutionContext executionContext) {
        sourceBefore(":"); // increment passed the ":"

        JavaType type = null; // TODO: add type mapping. Note: typeRef does not contain a reference to the symbol. The symbol exists on the FIR element.
        if (typeRef instanceof FirUserTypeRef) {
            FirUserTypeRef userTypeRef = (FirUserTypeRef) typeRef;
            if (userTypeRef.getQualifier().size() == 1) {
                FirQualifierPart part = userTypeRef.getQualifier().get(0);
                Space prefix = sourceBefore(part.getName().asString());
                J.Identifier ident = new J.Identifier(randomId(),
                        EMPTY,
                        Markers.EMPTY,
                        part.getName().asString(),
                        type,
                        null);
                if (!part.getTypeArgumentList().getTypeArguments().isEmpty()) {
                    throw new IllegalStateException("Return parameterized type.");
                } else {
                    return ident.withPrefix(prefix);
                }
            } else {
                throw new IllegalStateException("Implement me.");
            }
        } else {
            throw new IllegalStateException("Implement me.");
        }
    }

    @Override
    public J visitElement(@NotNull FirElement firElement, ExecutionContext executionContext) {
        if (firElement instanceof FirConstExpression) {
            return visitConstExpression((FirConstExpression<? extends Object>) firElement, ctx);
        } else {
            throw new IllegalStateException("Implement me.");
        }
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


    // TODO: parse comments.
    private List<J> getModifiers(KtDeclarationModifierList modifierList) {
        List<J> modifiers = new ArrayList<>();
        PsiElement current = modifierList.getFirstChild();
        List<J.Annotation> annotations = new ArrayList<>();
        while (current != null) {
            IElementType elementType = current.getNode().getElementType();
            if (elementType instanceof KtModifierKeywordToken) {
                KtModifierKeywordToken token = (KtModifierKeywordToken) elementType;
                K.Modifier modifier = mapModifier(token, annotations);
                annotations = new ArrayList<>();
                modifiers.add(modifier);
            } else if (elementType instanceof KtAnnotationEntryElementType) {
                ASTNode astNode = current.getNode();
                if (astNode instanceof CompositeElement) {
                    J.Annotation annotation = mapAnnotation((CompositeElement) astNode);
                    annotations.add(annotation);
                } else {
                    throw new IllegalStateException("Implement me.");
                }
            }
            current = current.getNextSibling();
        }
        modifiers.addAll(annotations);
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

    // TODO: parse annotation composite and create J.Annotation.
    private J.Annotation mapAnnotation(CompositeElement compositeElement) {
        Space prefix = whitespace();
        return new J.Annotation(randomId(), prefix, Markers.EMPTY, null, JContainer.empty());
    }
}
