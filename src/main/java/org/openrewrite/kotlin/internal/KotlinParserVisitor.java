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

import org.jetbrains.kotlin.KtFakeSourceElementKind;
import org.jetbrains.kotlin.KtRealPsiSourceElement;
import org.jetbrains.kotlin.KtSourceElement;
import org.jetbrains.kotlin.descriptors.ClassKind;
import org.jetbrains.kotlin.fir.ClassMembersKt;
import org.jetbrains.kotlin.fir.FirElement;
import org.jetbrains.kotlin.fir.FirPackageDirective;
import org.jetbrains.kotlin.fir.FirSession;
import org.jetbrains.kotlin.fir.declarations.*;
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyGetter;
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertySetter;
import org.jetbrains.kotlin.fir.declarations.impl.FirPrimaryConstructor;
import org.jetbrains.kotlin.fir.expressions.*;
import org.jetbrains.kotlin.fir.expressions.impl.*;
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference;
import org.jetbrains.kotlin.fir.references.FirNamedReference;
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference;
import org.jetbrains.kotlin.fir.resolve.LookupTagUtilsKt;
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag;
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol;
import org.jetbrains.kotlin.fir.symbols.impl.*;
import org.jetbrains.kotlin.fir.types.*;
import org.jetbrains.kotlin.fir.types.impl.FirImplicitNullableAnyTypeRef;
import org.jetbrains.kotlin.fir.types.impl.FirImplicitUnitTypeRef;
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitor;
import org.jetbrains.kotlin.psi.KtNameReferenceExpression;
import org.openrewrite.ExecutionContext;
import org.openrewrite.FileAttributes;
import org.openrewrite.internal.EncodingDetectingInputStream;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.internal.JavaTypeCache;
import org.openrewrite.java.marker.ImplicitReturn;
import org.openrewrite.java.marker.OmitParentheses;
import org.openrewrite.java.tree.*;
import org.openrewrite.kotlin.KotlinTypeMapping;
import org.openrewrite.kotlin.marker.*;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.marker.Markers;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.lang.Math.max;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.internal.StringUtils.indexOfNextNonWhitespace;
import static org.openrewrite.java.tree.Space.EMPTY;
import static org.openrewrite.java.tree.Space.format;
import static org.openrewrite.kotlin.marker.PropertyClassifier.ClassifierType.VAL;
import static org.openrewrite.kotlin.marker.PropertyClassifier.ClassifierType.VAR;

@SuppressWarnings("DataFlowIssue")
public class KotlinParserVisitor extends FirDefaultVisitor<J, ExecutionContext> {
    private final Path sourcePath;

    @Nullable
    private final FileAttributes fileAttributes;
    private final String source;
    private final Charset charset;
    private final boolean charsetBomMarked;
    private final KotlinTypeMapping typeMapping;
    private final ExecutionContext ctx;
    private final FirSession firSession;
    private int cursor = 0;

    // Associate top-level function and property declarations to the file.
    @Nullable
    private FirFile currentFile = null;

    private static final Pattern whitespaceSuffixPattern = Pattern.compile("\\s*[^\\s]+(\\s*)");

    public KotlinParserVisitor(Path sourcePath, @Nullable FileAttributes fileAttributes, EncodingDetectingInputStream source, JavaTypeCache typeCache, FirSession firSession, ExecutionContext ctx) {
        this.sourcePath = sourcePath;
        this.fileAttributes = fileAttributes;
        this.source = source.readFully();
        this.charset = source.getCharset();
        this.charsetBomMarked = source.isCharsetBomMarked();
        this.typeMapping = new KotlinTypeMapping(typeCache, firSession);
        this.ctx = ctx;
        this.firSession = firSession;
    }

    @Override
    public J visitFile(FirFile file, ExecutionContext ctx) {
        currentFile = file;

        JRightPadded<J.Package> pkg = null;
        if (!file.getPackageDirective().getPackageFqName().isRoot()) {
            pkg = maybeSemicolon((J.Package) visitPackageDirective(file.getPackageDirective(), ctx));
        }

        List<JRightPadded<J.Import>> imports = file.getImports().stream()
                .map(it -> maybeSemicolon((J.Import) visitImport(it, ctx)))
                .collect(Collectors.toList());

        List<JRightPadded<Statement>> statements = new ArrayList<>(file.getDeclarations().size());
        for (FirDeclaration declaration : file.getDeclarations()) {
            Statement statement = (Statement) visitElement(declaration, ctx);
            statements.add(maybeSemicolon(statement));
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
                format(source.substring(cursor))
        );
    }

    @Override
    public J visitErrorNamedReference(FirErrorNamedReference errorNamedReference, ExecutionContext ctx) {
        if (!(errorNamedReference.getSource() instanceof KtRealPsiSourceElement)) {
            throw new IllegalStateException("Unexpected error reference source: " + errorNamedReference.getSource());
        }

        if (!(((KtRealPsiSourceElement) errorNamedReference.getSource()).getPsi() instanceof KtNameReferenceExpression)) {
            throw new IllegalStateException("Unexpected error reference source: " + errorNamedReference.getSource());
        }

        KtNameReferenceExpression nameReferenceExpression = (KtNameReferenceExpression) ((KtRealPsiSourceElement) errorNamedReference.getSource()).getPsi();
        String name = nameReferenceExpression.getIdentifier() == null ? "{error}" : nameReferenceExpression.getIdentifier().getText();
        Space prefix = sourceBefore(name);
        return new J.Identifier(
                randomId(),
                prefix,
                Markers.EMPTY,
                name,
                null,
                null
        );
    }

    @Override
    public J visitAnnotationCall(FirAnnotationCall annotationCall, ExecutionContext ctx) {
        Space prefix = whitespace();
        skip("@");
        J.Identifier name = (J.Identifier) visitElement(annotationCall.getCalleeReference(), ctx);
        JContainer<Expression> args = null;
        if (annotationCall.getArgumentList().getArguments().size() > 0) {
            Space argsPrefix = sourceBefore("(");
            List<JRightPadded<Expression>> expressions;
            if (annotationCall.getArgumentList().getArguments().size() == 1) {
                if (annotationCall.getArgumentList().getArguments().get(0) instanceof FirVarargArgumentsExpression) {
                    FirVarargArgumentsExpression varargArgumentsExpression = (FirVarargArgumentsExpression) annotationCall.getArgumentList().getArguments().get(0);
                    expressions = convertAll(varargArgumentsExpression.getArguments(), commaDelim, t -> sourceBefore(")"), ctx);
                } else {
                    FirExpression arg = annotationCall.getArgumentList().getArguments().get(0);
                    expressions = singletonList(convert(arg, t -> sourceBefore(")"), ctx));
                }
            } else {
                expressions = convertAll(annotationCall.getArgumentList().getArguments(), commaDelim, t -> sourceBefore(")"), ctx);
            }

            args = JContainer.build(argsPrefix, expressions, Markers.EMPTY);
        }

        return new J.Annotation(randomId(), prefix, Markers.EMPTY, name, args);
    }

    @Override
    public J visitAnonymousFunction(FirAnonymousFunction anonymousFunction, ExecutionContext ctx) {
        throw new IllegalStateException("Implement me.");
    }

    @Override
    public J visitAnonymousFunctionExpression(FirAnonymousFunctionExpression anonymousFunctionExpression, ExecutionContext ctx) {
        if (!anonymousFunctionExpression.getAnonymousFunction().isLambda()) {
            throw new IllegalStateException("Implement me.");
        }

        Space prefix = sourceBefore("{");
        JavaType closureType = null;

        FirAnonymousFunction anonymousFunction = anonymousFunctionExpression.getAnonymousFunction();
        List<JRightPadded<J>> paramExprs = new ArrayList<>(anonymousFunction.getValueParameters().size());
        if (!anonymousFunction.getValueParameters().isEmpty()) {
            List<FirValueParameter> parameters = anonymousFunction.getValueParameters();
            for (int i = 0; i < parameters.size(); i++) {
                FirValueParameter p = parameters.get(i);
                J expr = visitElement(p, ctx);
                JRightPadded<J> param = JRightPadded.build(expr);
                if (i != parameters.size() - 1) {
                    param = param.withAfter(sourceBefore(","));
                }
                paramExprs.add(param);
            }
        }

        J.Lambda.Parameters params = new J.Lambda.Parameters(randomId(), EMPTY, Markers.EMPTY, false, paramExprs);
        int saveCursor = cursor;
        Space arrowPrefix = whitespace();
        if (source.startsWith("->", cursor)) {
            cursor += "->".length();
            if (params.getParameters().isEmpty()) {
                params = params.getPadding().withParams(singletonList(JRightPadded
                        .build((J)new J.Empty(randomId(), EMPTY, Markers.EMPTY))
                        .withAfter(arrowPrefix)));
            } else {
                params = params.getPadding().withParams(
                        ListUtils.mapLast(params.getPadding().getParams(), param -> param.withAfter(arrowPrefix))
                );
            }
        } else {
            cursor = saveCursor;
        }

        J body = anonymousFunction.getBody() == null ? null : visitElement(anonymousFunction.getBody(), ctx);
        if (body instanceof J.Block) {
            body = ((J.Block) body).withEnd(sourceBefore("}"));
        }

        if (body != null && anonymousFunction.getValueParameters().isEmpty()) {
            body = body.withMarkers(body.getMarkers().removeByType(OmitBraces.class));
        }

        if (body == null) {
            body = new J.Block(randomId(),
                    EMPTY,
                    Markers.EMPTY,
                    new JRightPadded<>(false, EMPTY, Markers.EMPTY),
                    emptyList(),
                    EMPTY);
        }

        return new J.Lambda(
                randomId(),
                prefix,
                Markers.EMPTY,
                params,
                EMPTY,
                body,
                closureType);
    }

    @Override
    public J visitAnonymousObject(FirAnonymousObject anonymousObject, ExecutionContext ctx) {
        Space objectPrefix = sourceBefore("object");
        Markers markers = Markers.EMPTY.addIfAbsent(new AnonymousObjectPrefix(randomId(), objectPrefix));
        Space typeExpressionPrefix = sourceBefore(":");
        Space prefix = whitespace();

        TypeTree clazz = (TypeTree) visitElement(anonymousObject.getSuperTypeRefs().get(0), ctx);
        JContainer<Expression> args;
        if (positionOfNext("(", '{') > -1) {
            args = JContainer.build(sourceBefore("("),
                    singletonList(padRight(new J.Empty(randomId(), sourceBefore(")"), Markers.EMPTY), EMPTY)), Markers.EMPTY);
        } else {
            args = JContainer.<Expression>empty()
                    .withMarkers(Markers.build(singletonList(new OmitParentheses(randomId()))));
        }

        int saveCursor = cursor;
        J.Block body = null;
        Space bodyPrefix = whitespace();

        if (source.startsWith("{", cursor)) {
            skip("{");
            body = new J.Block(
                    randomId(),
                    bodyPrefix,
                    Markers.EMPTY,
                    new JRightPadded<>(false, EMPTY, Markers.EMPTY),
                    emptyList(),
                    sourceBefore("}"));
        } else {
            cursor = saveCursor;
        }

        return new J.NewClass(
                randomId(),
                prefix,
                markers,
                null, // TODO
                typeExpressionPrefix,
                clazz,
                args,
                body,
                null
        );
    }

    @Override
    public J visitAnonymousObjectExpression(FirAnonymousObjectExpression anonymousObjectExpression, ExecutionContext ctx) {
        // Pass through to the anonymous object since it doesn't seem necessary to use the typeRef.
        return visitElement(anonymousObjectExpression.getAnonymousObject(), ctx);
    }

    @Override
    public J visitBinaryLogicExpression(FirBinaryLogicExpression binaryLogicExpression, ExecutionContext ctx) {
        Space prefix = whitespace();
        Expression left = (Expression) visitElement(binaryLogicExpression.getLeftOperand(), ctx);

        Space opPrefix = whitespace();
        J.Binary.Type op;
        if (LogicOperationKind.AND == binaryLogicExpression.getKind()) {
            skip("&&");
            op = J.Binary.Type.And;
        } else if (LogicOperationKind.OR == binaryLogicExpression.getKind()) {
            skip("||");
            op = J.Binary.Type.Or;
        } else {
            throw new UnsupportedOperationException("Unsupported binary expression type " + binaryLogicExpression.getKind().name());
        }

        Expression right = (Expression) visitElement(binaryLogicExpression.getRightOperand(), ctx);

        return new J.Binary(
                randomId(),
                prefix,
                Markers.EMPTY,
                left,
                padLeft(opPrefix, op),
                right,
                typeMapping.type(binaryLogicExpression));
    }

    @Override
    public J visitBlock(FirBlock block, ExecutionContext ctx) {
        int saveCursor = cursor;
        Space prefix = whitespace();
        OmitBraces omitBraces = null;
        boolean isEmptyBody = !source.startsWith("{", cursor);
        if (isEmptyBody) {
            cursor = saveCursor;
            prefix = EMPTY;
            omitBraces = new OmitBraces(randomId());
        } else {
            skip("{");
        }

        List<FirStatement> firStatements = new ArrayList<>(block.getStatements().size());
        for (FirStatement s : block.getStatements()) {
            if (s.getSource() == null || !(s.getSource().getKind() instanceof KtFakeSourceElementKind.ImplicitConstructor)) {
                firStatements.add(s);
            }
        }

        List<JRightPadded<Statement>> statements = new ArrayList<>(firStatements.size());
        for (int i = 0; i < firStatements.size(); i++) {
            FirElement firElement = firStatements.get(i);
            // Skip receiver of the unary post increment or decrement.
            if (firElement instanceof FirProperty && ("<unary>".equals(((FirProperty) firElement).getName().asString()) ||
                    "<iterator>".equals(((FirProperty) firElement).getName().asString()))) {
                continue;
            }

            boolean explicitReturn = false;
            Space returnPrefix = EMPTY;
            if (i == firStatements.size() - 1) {
                // Skip the implicit return from a unary pre increment or decrement operation.
                if (!statements.isEmpty() && statements.get(statements.size() - 1).getElement() instanceof J.Unary) {
                    continue;
                }

                saveCursor = cursor;
                returnPrefix = whitespace();
                if (source.startsWith("return", cursor)) {
                    cursor += "return".length();
                    explicitReturn = true;
                } else {
                    cursor = saveCursor;
                }
            }

            J expr = visitElement(firElement, ctx);
            if (i == firStatements.size() - 1 && (expr instanceof Expression)) {
                if (!explicitReturn) {
                    returnPrefix = expr.getPrefix();
                    expr = expr.withPrefix(EMPTY);
                }
                expr = new J.Return(randomId(), returnPrefix, expr.getMarkers(), (Expression) expr);
                if (!explicitReturn) {
                    expr = expr.withMarkers(expr.getMarkers().add(new ImplicitReturn(randomId())));
                }
            } else if (i == firStatements.size() - 1 && expr instanceof J.Return && ((J.Return) expr).getExpression() == null) {
                expr = expr.withMarkers(expr.getMarkers().add(new ImplicitReturn(randomId())));
            }

            if (!(expr instanceof Statement)) {
                if (expr instanceof Expression) {
                    expr = new K.ExpressionStatement(randomId(), (Expression) expr);
                } else {
                    throw new UnsupportedOperationException("Unexpected statement type.");
                }
            }

            JRightPadded<Statement> stat = JRightPadded.build((Statement) expr);
            saveCursor = cursor;
            Space beforeSemicolon = whitespace();
            if (cursor < source.length() && source.charAt(cursor) == ';') {
                stat = stat
                        .withMarkers(stat.getMarkers().add(new Semicolon(randomId())))
                        .withAfter(beforeSemicolon);
                cursor++;
            } else {
                cursor = saveCursor;
            }
            statements.add(stat);
        }

        return new J.Block(
                randomId(),
                prefix,
                omitBraces == null ? Markers.EMPTY : Markers.EMPTY.addIfAbsent(omitBraces),
                JRightPadded.build(false),
                statements,
                isEmptyBody ? Space.EMPTY : sourceBefore("}"));
    }

    @Override
    public J visitBreakExpression(FirBreakExpression breakExpression, ExecutionContext ctx) {
        Space prefix = sourceBefore("break");

        J.Identifier label = breakExpression.getTarget().getLabelName() == null ? null : convertToIdentifier(breakExpression.getTarget().getLabelName());
        return new J.Break(randomId(), prefix, Markers.EMPTY, label);
    }

    @Override
    public J visitClass(FirClass klass, ExecutionContext ctx) {
        FirRegularClass firRegularClass = (FirRegularClass) klass;
        Space prefix = whitespace();

        // Not used until it's possible to handle K.Modifiers.
        List<J> modifiers = emptyList();

        ClassKind classKind = klass.getClassKind();
        ModifierScope modifierScope = null;
        if (ClassKind.INTERFACE == classKind) {
            modifierScope = ModifierScope.INTERFACE;
        } else {
            modifierScope = ModifierScope.CLASS;
        }

        List<J.Annotation> leadingAnnotation = mapModifiers(modifierScope, firRegularClass.getAnnotations());
        List<J.Annotation> kindAnnotations = emptyList();

        J.ClassDeclaration.Kind kind;
        if (ClassKind.INTERFACE == classKind) {
            kind = new J.ClassDeclaration.Kind(randomId(), sourceBefore("interface"), Markers.EMPTY, kindAnnotations, J.ClassDeclaration.Kind.Type.Interface);
        } else {
            // Enums and Interfaces are modifiers in kotlin and require the modifier prefix to preserve source code.
            kind = new J.ClassDeclaration.Kind(randomId(), sourceBefore("class"), Markers.EMPTY, kindAnnotations, J.ClassDeclaration.Kind.Type.Class);
        }

        J.Identifier name = convertToIdentifier(firRegularClass.getName().asString(), firRegularClass);

        // KotlinTypeParameters with multiple bounds are defined outside the TypeParameter container.
        // KotlinTypeGoat<T, S> where S: A, T: B, S: C, T: D.
        // The order the bounds exist in T and S will be based on the declaration order.
        // However, each bound may be declared in any order T -> S -> T -> S.
        JContainer<J.TypeParameter> typeParams = firRegularClass.getTypeParameters().isEmpty() ? null : JContainer.build(
                sourceBefore("<"),
                convertAll(firRegularClass.getTypeParameters(), commaDelim, t -> sourceBefore(">"), ctx),
                Markers.EMPTY);

        JContainer<TypeTree> implementings = null;
        List<JRightPadded<TypeTree>> superTypes = null;

        int saveCursor = cursor;
        Space before = whitespace();
        if (source.startsWith(":", cursor)) {
            skip(":");
        }

        // Kotlin declared super class and interfaces differently than java. All types declared after the `:` are added into implementings.
        // This should probably exist on a K.ClassDeclaration view where the getters return the appropriate types.
        // The J.ClassDeclaration should have the super type set in extending and the J.NewClass should be unwrapped.
        for (int i = 0; i < firRegularClass.getSuperTypeRefs().size(); i++) {
            FirTypeRef typeRef = firRegularClass.getSuperTypeRefs().get(i);
            FirRegularClassSymbol symbol = TypeUtilsKt.toRegularClassSymbol(FirTypeUtilsKt.getConeType(typeRef), firSession);
            // Filter out generated types.
            if (typeRef.getSource() != null && !(typeRef.getSource().getKind() instanceof KtFakeSourceElementKind)) {
                if (superTypes == null) {
                    superTypes = new ArrayList<>(firRegularClass.getSuperTypeRefs().size());
                }

                TypeTree element = (TypeTree) visitElement(typeRef, ctx);
                if (symbol != null && ClassKind.CLASS == symbol.getFir().getClassKind()) {
                    // Wrap the element in a J.NewClass to preserve the whitespace and container of `( )`
                    J.NewClass newClass = new J.NewClass(
                            randomId(),
                            element.getPrefix(),
                            Markers.EMPTY,
                            null,
                            EMPTY,
                            element.withPrefix(EMPTY),
                            JContainer.build(sourceBefore("("),
                                    singletonList(JRightPadded.build(new J.Empty(randomId(), sourceBefore(")"), Markers.EMPTY))),
                                    Markers.EMPTY),
                            null,
                            null
                    );
                    element = new K.FunctionType(randomId(), newClass, null);
                }
                superTypes.add(JRightPadded.build(element)
                        .withAfter(i == firRegularClass.getSuperTypeRefs().size() - 1 ? EMPTY : sourceBefore(",")));
            }
        }

        if (superTypes == null) {
            cursor = saveCursor;
        } else {
            implementings = JContainer.build(before, superTypes, Markers.EMPTY);
        }
        List<FirElement> membersMultiVariablesSeparated = new ArrayList<>(firRegularClass.getDeclarations().size());
        List<FirDeclaration> jcEnums = new ArrayList<>(klass.getDeclarations().size());
        FirPrimaryConstructor firPrimaryConstructor = null;
        for (FirDeclaration declaration : firRegularClass.getDeclarations()) {
            if (declaration instanceof FirEnumEntry) {
                jcEnums.add(declaration);
            } else if (declaration instanceof FirPrimaryConstructor) {
                firPrimaryConstructor = (FirPrimaryConstructor) declaration;
            } else {
                // We aren't interested in the generated values.
                if (ClassKind.ENUM_CLASS == classKind && declaration.getSource() != null &&
                        declaration.getSource().getKind() instanceof KtFakeSourceElementKind.PropertyFromParameter) {
                    continue;
                }
                membersMultiVariablesSeparated.add(declaration);
            }
        }

        saveCursor = cursor;
        Space bodyPrefix = whitespace();
        JContainer<Statement> primaryConstructor = null;
        boolean inlineConstructor = source.startsWith("(", cursor) && firPrimaryConstructor != null;
        if (inlineConstructor) {
            cursor = saveCursor;
            primaryConstructor = JContainer.build(
                    sourceBefore("("),
                    convertAll(firPrimaryConstructor.getValueParameters(), commaDelim, t -> sourceBefore(")"), ctx),
                    Markers.EMPTY
            );
        }

        if (inlineConstructor) {
            saveCursor = cursor;
            bodyPrefix = whitespace();
        }

        OmitBraces omitBraces;
        J.Block body;
        if (source.substring(cursor).isEmpty() || !source.substring(cursor).startsWith("{")) {
            cursor = saveCursor;
            omitBraces = new OmitBraces(randomId());
            body = new J.Block(randomId(), bodyPrefix, Markers.EMPTY, new JRightPadded<>(false, EMPTY, Markers.EMPTY), emptyList(), Space.EMPTY);
            body = body.withMarkers(body.getMarkers().addIfAbsent(omitBraces));
        } else {
            skip("{"); // Increment past the `{`

            JRightPadded<Statement> enumSet = null;
            if (!jcEnums.isEmpty()) {
                AtomicBoolean semicolonPresent = new AtomicBoolean(false);

                List<JRightPadded<J.EnumValue>> enumValues = convertAll(jcEnums, commaDelim, t -> {
                    // this semicolon is required when there are non-value members, but can still
                    // be present when there are not
                    semicolonPresent.set(positionOfNext(";", '}') > 0);
                    return semicolonPresent.get() ? sourceBefore(";", '}') : EMPTY;
                }, ctx);

                enumSet = padRight(
                        new J.EnumValueSet(
                                randomId(),
                                enumValues.get(0).getElement().getPrefix(),
                                Markers.EMPTY,
                                ListUtils.map(enumValues, (i, ev) -> i == 0 ? ev.withElement(ev.getElement().withPrefix(EMPTY)) : ev),
                                semicolonPresent.get()
                        ),
                        EMPTY
                );
            }

            List<JRightPadded<Statement>> members = new ArrayList<>(
                    membersMultiVariablesSeparated.size() + (enumSet == null ? 0 : 1));
            if (enumSet != null) {
                members.add(enumSet);
            }
            for (FirElement firElement : membersMultiVariablesSeparated) {
                if (!(firElement instanceof FirEnumEntry)) {
                    if (ClassKind.ENUM_CLASS == classKind && firElement.getSource() != null &&
                            firElement.getSource().getKind() instanceof KtFakeSourceElementKind.EnumGeneratedDeclaration) {
                        continue;
                    }
                    members.add(maybeSemicolon((Statement) visitElement(firElement, ctx)));
                }
            }

            body = new J.Block(randomId(), bodyPrefix, Markers.EMPTY, new JRightPadded<>(false, EMPTY, Markers.EMPTY),
                    members, sourceBefore("}"));
        }

        return new J.ClassDeclaration(
                randomId(),
                prefix,
                Markers.EMPTY,
                leadingAnnotation,
                emptyList(), // Requires a K view to add K specific modifiers. Modifiers specific to Kotlin are added as annotations for now.
                kind,
                name,
                typeParams,
                primaryConstructor,
                null,
                implementings,
                null,
                body,
                (JavaType.FullyQualified) typeMapping.type(firRegularClass)
        );
    }

    @Override
    public J visitComparisonExpression(FirComparisonExpression comparisonExpression, ExecutionContext ctx) {
        Space prefix = whitespace();

        FirFunctionCall functionCall = comparisonExpression.getCompareToCall();
        Expression left = (Expression) visitElement(functionCall.getExplicitReceiver(), ctx);

        Space opPrefix = whitespace();
        J.Binary.Type op = mapOperation(comparisonExpression.getOperation());

        if (functionCall.getArgumentList().getArguments().size() != 1) {
            throw new UnsupportedOperationException("Unsupported FirComparisonExpression argument size");
        }

        Expression right = (Expression) visitElement(functionCall.getArgumentList().getArguments().get(0), ctx);
        return new J.Binary(randomId(),
                prefix,
                Markers.EMPTY,
                left,
                padLeft(opPrefix, op),
                right,
                typeMapping.type(comparisonExpression));
    }

    @Override
    public <T> J visitConstExpression(FirConstExpression<T> constExpression, ExecutionContext ctx) {
        String valueSource = source.substring(constExpression.getSource().getStartOffset(), constExpression.getSource().getEndOffset());
        Space prefix = sourceBefore(valueSource);

        Object value = constExpression.getValue();
        JavaType.Primitive type;
        if (constExpression.getTypeRef() instanceof FirResolvedTypeRef &&
                ((FirResolvedTypeRef) constExpression.getTypeRef()).getType() instanceof ConeClassLikeType) {
            ConeClassLikeType coneClassLikeType = (ConeClassLikeType) ((FirResolvedTypeRef) constExpression.getTypeRef()).getType();
            type = typeMapping.primitive(coneClassLikeType);
        } else {
            throw new UnsupportedOperationException("Unresolved primitive type.");
        }

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
    public J visitEqualityOperatorCall(FirEqualityOperatorCall equalityOperatorCall, ExecutionContext ctx) {
        if (equalityOperatorCall.getArgumentList().getArguments().size() != 2) {
            throw new UnsupportedOperationException("Unsupported number of equality operator arguments.");
        }

        FirElement left = equalityOperatorCall.getArgumentList().getArguments().get(0);
        FirOperation op = equalityOperatorCall.getOperation();
        FirElement right = equalityOperatorCall.getArgumentList().getArguments().get(1);

        return new J.Binary(
                randomId(),
                whitespace(),
                Markers.EMPTY,
                (Expression) visitElement(left, ctx),
                padLeft(sourceBefore(op.getOperator()), mapOperation(op)),
                (Expression) visitElement(right, ctx),
                typeMapping.type(equalityOperatorCall));
    }

    @Override
    public J visitContinueExpression(FirContinueExpression continueExpression, ExecutionContext ctx) {
        Space prefix = sourceBefore("continue");
        J.Identifier label = continueExpression.getTarget().getLabelName() == null ? null : convertToIdentifier(continueExpression.getTarget().getLabelName());
        return new J.Continue(
                randomId(),
                prefix,
                Markers.EMPTY,
                label);
    }

    @Override
    public J visitDoWhileLoop(FirDoWhileLoop doWhileLoop, ExecutionContext ctx) {
        Space prefix = whitespace();

        skip("do");
        return new J.DoWhileLoop(
                randomId(),
                prefix,
                Markers.EMPTY,
                JRightPadded.build((Statement) visitElement(doWhileLoop.getBlock(), ctx)),
                padLeft(sourceBefore("while"), convertToControlParentheses(doWhileLoop.getCondition())));
    }

    @Override
    public J visitEnumEntry(FirEnumEntry enumEntry, ExecutionContext ctx) {
        Space prefix = whitespace();

        List<J.Annotation> annotations = mapAnnotations(enumEntry.getAnnotations());

        return new J.EnumValue(
                randomId(),
                prefix,
                Markers.EMPTY,
                annotations == null ? emptyList() : annotations,
                convertToIdentifier(enumEntry.getName().asString(), enumEntry),
                null
        );
    }

    @Override
    public J visitFunctionCall(FirFunctionCall functionCall, ExecutionContext ctx) {
        FirFunctionCallOrigin origin = functionCall.getOrigin();
        if (origin == FirFunctionCallOrigin.Operator && !(functionCall instanceof FirImplicitInvokeCall)) {
            return mapOperatorFunctionCall(functionCall);
        } else {
            return mapFunctionalCall(functionCall, origin == FirFunctionCallOrigin.Infix);
        }
    }

    private J mapFunctionalCall(FirFunctionCall functionCall, boolean isInfix) {
        Space prefix = whitespace();

        FirNamedReference namedReference = functionCall.getCalleeReference();
        if (namedReference instanceof FirResolvedNamedReference &&
                ((FirResolvedNamedReference) namedReference).getResolvedSymbol() instanceof FirConstructorSymbol) {
            TypeTree name = (J.Identifier) visitElement(namedReference, null);
            if (!functionCall.getTypeArguments().isEmpty()) {
                name = new J.ParameterizedType(randomId(), EMPTY, Markers.EMPTY, name, mapTypeArguments(functionCall.getTypeArguments()));
            }

            JContainer<Expression> args = mapFunctionalCallArguments(functionCall);
            return new J.NewClass(
                    randomId(),
                    prefix,
                    Markers.EMPTY,
                    null,
                    EMPTY,
                    name,
                    args,
                    null,
                    null);

        } else if (namedReference instanceof FirResolvedNamedReference) {
            FirBasedSymbol<?> symbol = ((FirResolvedNamedReference) namedReference).getResolvedSymbol();
            FirBasedSymbol<?> owner = null;
            if (symbol instanceof FirNamedFunctionSymbol) {
                FirNamedFunctionSymbol namedFunctionSymbol = (FirNamedFunctionSymbol) symbol;
                ConeClassLikeLookupTag lookupTag = ClassMembersKt.containingClass(namedFunctionSymbol);
                if (lookupTag != null) {
                    owner = LookupTagUtilsKt.toFirRegularClassSymbol(lookupTag, firSession);
                } else if (currentFile != null) {
                    owner = currentFile.getSymbol();
                }
            }

            JRightPadded<Expression> select = null;
            FirElement dispatchReceiver = functionCall.getDispatchReceiver();
            FirElement extensionReceiver = functionCall.getExtensionReceiver();
            if (!(functionCall instanceof FirImplicitInvokeCall) &&
                    (!(dispatchReceiver instanceof FirNoReceiverExpression || dispatchReceiver instanceof FirThisReceiverExpression)) ||
                    !(extensionReceiver instanceof FirNoReceiverExpression || extensionReceiver instanceof FirThisReceiverExpression)) {
                FirElement visit;
                if (dispatchReceiver instanceof FirFunctionCall || dispatchReceiver instanceof FirPropertyAccessExpression) {
                    visit = dispatchReceiver;
                } else if (extensionReceiver instanceof FirFunctionCall || extensionReceiver instanceof FirPropertyAccessExpression) {
                    visit = extensionReceiver;
                } else {
                    throw new UnsupportedOperationException("Unsupported FunctionCall selection type.");
                }

                Expression selectExpr = (Expression) visitElement(visit, ctx);
                Space after = whitespace();
                if (source.startsWith(".", cursor)) {
                    skip(".");
                }

                select = JRightPadded.build(selectExpr)
                        .withAfter(after);
            }
            Markers markers = Markers.EMPTY;
            if (isInfix) {
                markers = markers.addIfAbsent(new ReceiverType(randomId()));
            }

            J.Identifier name = (J.Identifier) visitElement(namedReference, null);

            JContainer<Expression> typeParams = null;
            if (!functionCall.getTypeArguments().isEmpty()) {
                int saveCursor = cursor;
                whitespace();
                boolean parseTypeArguments = source.startsWith("<", cursor);
                cursor = saveCursor;
                if (parseTypeArguments) {
                    typeParams = mapTypeArguments(functionCall.getTypeArguments());
                }
            }

            int saveCursor = cursor;
            whitespace();
            JContainer<Expression> args;
            if (source.startsWith("(", cursor)) {
                cursor = saveCursor;
                args = mapFunctionalCallArguments(functionCall);
            } else {
                cursor = saveCursor;
                markers = markers.addIfAbsent(new OmitParentheses(randomId()));

                List<JRightPadded<Expression>> arguments = new ArrayList<>(functionCall.getArgumentList().getArguments().size());
                for (FirExpression argument : functionCall.getArgumentList().getArguments()) {
                    Expression expression = (Expression) visitElement(argument, null);
                    JRightPadded<Expression> padded = JRightPadded.build(expression);
                    arguments.add(padded);
                }
                args = JContainer.build(arguments);
            }

            return new J.MethodInvocation(
                    randomId(),
                    prefix,
                    markers,
                    select,
                    typeParams,
                    name,
                    args,
                    typeMapping.methodInvocationType(functionCall, owner));
        }

        throw new UnsupportedOperationException("Unsupported function call.");
    }

    private JContainer<Expression> mapFunctionalCallArguments(FirFunctionCall functionCall) {
        JContainer<Expression> args;
        List<FirExpression> firExpressions = functionCall.getArgumentList().getArguments();
        if (firExpressions.size() == 1) {
            FirExpression firExpression = firExpressions.get(0);
            if (firExpression instanceof FirVarargArgumentsExpression) {
                FirVarargArgumentsExpression argumentsExpression = (FirVarargArgumentsExpression) firExpressions.get(0);
                args = JContainer.build(sourceBefore("("), argumentsExpression.getArguments().isEmpty() ?
                        singletonList(padRight(new J.Empty(randomId(), sourceBefore(")"), Markers.EMPTY), EMPTY)) :
                        convertAll(argumentsExpression.getArguments(), commaDelim, t -> sourceBefore(")"), ctx), Markers.EMPTY);
            } else if (firExpression instanceof FirConstExpression ||
                    firExpression instanceof FirEqualityOperatorCall ||
                    firExpression instanceof FirFunctionCall ||
                    firExpression instanceof FirNamedArgumentExpression ||
                    firExpression instanceof FirPropertyAccessExpression ||
                    firExpression instanceof FirStringConcatenationCall) {
                args = JContainer.build(sourceBefore("("), convertAll(singletonList(firExpression), commaDelim, t -> sourceBefore(")"), ctx), Markers.EMPTY);
            } else {
                throw new UnsupportedOperationException("Could not map function call arguments.");
            }
        } else {
            args = JContainer.build(sourceBefore("("), firExpressions.isEmpty() ?
                    singletonList(padRight(new J.Empty(randomId(), sourceBefore(")"), Markers.EMPTY), EMPTY)) :
                    convertAll(firExpressions, commaDelim, t -> sourceBefore(")"), ctx), Markers.EMPTY);
        }
        return args;
    }

    private JContainer<Expression> mapTypeArguments(List<? extends FirElement> types) {
        Space prefix = whitespace();
        if (source.startsWith("<", cursor)) {
            skip("<");
        }
        List<JRightPadded<Expression>> parameters = new ArrayList<>(types.size());;

        for (int i = 0; i < types.size(); i++) {
            FirElement type = types.get(i);
            JRightPadded<Expression> padded = JRightPadded.build((Expression) visitElement(type, null)).withAfter(
                    i < types.size() - 1 ?
                            sourceBefore(",") :
                            whitespace()
            );
            parameters.add(padded);
        }

        if (source.startsWith(">", cursor)) {
            skip(">");
        }
        return JContainer.build(prefix, parameters, Markers.EMPTY);
    }

    private J mapOperatorFunctionCall(FirFunctionCall functionCall) {
        Space prefix = whitespace();

        Space opPrefix;
        String name = functionCall.getCalleeReference().getName().asString();

        boolean unaryOperation = isUnaryOperator(name);
        if (unaryOperation) {
            JLeftPadded<J.Unary.Type> op;
            Expression expr;

            switch (name) {
                case "dec":
                    if (source.startsWith("--", cursor)) {
                        skip("--");
                        op = padLeft(EMPTY, J.Unary.Type.PreDecrement);
                        expr = (Expression) visitElement(functionCall.getDispatchReceiver(), ctx);
                    } else {
                        // Both pre and post unary operations exist in a block with multiple AST elements: the property and unary operation.
                        // The J.Unary objects are all created here instead of interpreting the statements in visitBlock.
                        // The PRE operations have access to the correct property name, but the POST operations are set to "<unary>".
                        // So, we extract the name from the source based on the post operator.
                        int saveCursor = cursor;
                        String opName = sourceBefore("--").getWhitespace().trim();
                        cursor = saveCursor;
                        expr = convertToIdentifier(opName);
                        op = padLeft(sourceBefore("--"), J.Unary.Type.PostDecrement);
                    }
                    break;
                case "inc":
                    if (source.startsWith("++", cursor)) {
                        skip("++");
                        op = padLeft(EMPTY, J.Unary.Type.PreIncrement);
                        expr = (Expression) visitElement(functionCall.getDispatchReceiver(), ctx);
                    } else {
                        // Both pre and post unary operations exist in a block with multiple AST elements: the property and unary operation.
                        // The J.Unary objects are all created here instead of interpreting the statements in visitBlock.
                        // The PRE operations have access to the correct property name, but the POST operations are set to "<unary>".
                        // So, we extract the name from the source based on the post operator.
                        int saveCursor = cursor;
                        String opName = sourceBefore("++").getWhitespace().trim();
                        cursor = saveCursor;
                        expr = convertToIdentifier(opName);
                        op = padLeft(sourceBefore("++"), J.Unary.Type.PostIncrement);
                    }
                    break;
                case "not":
                    skip("!");
                    op = padLeft(EMPTY, J.Unary.Type.Not);
                    expr = (Expression) visitElement(functionCall.getDispatchReceiver(), ctx);
                    break;
                case "unaryMinus":
                    skip("-");
                    op = padLeft(EMPTY, J.Unary.Type.Negative);
                    expr = (Expression) visitElement(functionCall.getDispatchReceiver(), ctx);
                    break;
                case "unaryPlus":
                    skip("+");
                    op = padLeft(EMPTY, J.Unary.Type.Positive);
                    expr = (Expression) visitElement(functionCall.getDispatchReceiver(), ctx);
                    break;
                default:
                    throw new UnsupportedOperationException("Unsupported unary operator type.");
            }

            return new J.Unary(randomId(), prefix, Markers.EMPTY, op, expr, typeMapping.type(functionCall));
        } else {
            J left = visitElement(functionCall.getDispatchReceiver(), ctx);

            J.Binary.Type type;
            switch (name) {
                case "div":
                    type = J.Binary.Type.Division;
                    opPrefix = sourceBefore("/");
                    break;
                case "minus":
                    type = J.Binary.Type.Subtraction;
                    opPrefix = sourceBefore("-");
                    break;
                case "plus":
                    type = J.Binary.Type.Addition;
                    opPrefix = sourceBefore("+");
                    break;
                case "times":
                    type = J.Binary.Type.Multiplication;
                    opPrefix = sourceBefore("*");
                    break;
                default:
                    throw new UnsupportedOperationException("Unsupported binary operator type.");
            }
            J right = visitElement(functionCall.getArgumentList().getArguments().get(0), ctx);

            return new J.Binary(
                    randomId(),
                    prefix,
                    Markers.EMPTY,
                    (Expression) left,
                    padLeft(opPrefix, type),
                    (Expression) right,
                    typeMapping.type(functionCall));
        }
    }

    @Override
    public J visitFunctionTypeRef(FirFunctionTypeRef functionTypeRef, ExecutionContext ctx) {
        List<JRightPadded<J>> paramExprs = new ArrayList<>(functionTypeRef.getValueParameters().size());
        JRightPadded<NameTree> receiver = null;
        if (functionTypeRef.getReceiverTypeRef() != null) {
            NameTree receiverName = (NameTree) visitElement(functionTypeRef.getReceiverTypeRef(), ctx);
            receiver = JRightPadded.build(receiverName)
                    .withAfter(whitespace());
            skip(".");
        }

        Space prefix = whitespace();
        boolean parenthesized = source.charAt(cursor) == '(';
        skip("(");

        JavaType closureType = typeMapping.type(functionTypeRef);
        if (!functionTypeRef.getValueParameters().isEmpty()) {
            List<FirValueParameter> parameters = functionTypeRef.getValueParameters();
            for (int i = 0; i < parameters.size(); i++) {
                FirValueParameter p = parameters.get(i);
                J expr = visitElement(p, ctx);
                JRightPadded<J> param = JRightPadded.build(expr);
                Space after = i < parameters.size() - 1 ? sourceBefore(",") : (parenthesized ? sourceBefore(")") : EMPTY);
                param = param.withAfter(after);
                paramExprs.add(param);
            }
        }

        J.Lambda.Parameters params = new J.Lambda.Parameters(randomId(), EMPTY, Markers.EMPTY, parenthesized, paramExprs);
        if (parenthesized && functionTypeRef.getValueParameters().isEmpty()) {
            params = params.getPadding().withParams(singletonList(JRightPadded
                    .build((J)new J.Empty(randomId(), EMPTY, Markers.EMPTY))
                    .withAfter(sourceBefore(")"))));
        }

        Space arrow = sourceBefore("->");
        int saveCursor = cursor;
        whitespace();
        boolean omitBraces = !source.startsWith("{");
        cursor = saveCursor;

        J body = visitElement(functionTypeRef.getReturnTypeRef(), ctx);
        if (body instanceof J.Block) {
            body = ((J.Block) body).withEnd(sourceBefore("}"));
        }

        if (functionTypeRef.getValueParameters().isEmpty()) {
            body = body.withMarkers(body.getMarkers().removeByType(OmitBraces.class));
        }

        J.Lambda lambda =  new J.Lambda(
                randomId(),
                prefix,
                omitBraces ? Markers.EMPTY.addIfAbsent(new OmitBraces(randomId())) : Markers.EMPTY,
                params,
                arrow,
                body,
                closureType);
        return new K.FunctionType(randomId(), lambda, receiver);
    }

    @Override
    public J visitImport(FirImport firImport, ExecutionContext ctx) {
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
            skip(qualid.toString());
        }
        return new J.Import(randomId(), prefix, Markers.EMPTY, statik, qualid);
    }

    @Override
    public J visitPackageDirective(FirPackageDirective packageDirective, ExecutionContext ctx) {
        Space pkgPrefix = whitespace();
        cursor += "package".length();
        Space space = whitespace();

        String packageName = packageDirective.getPackageFqName().asString();
        skip(packageName);
        return new J.Package(
                randomId(),
                pkgPrefix,
                Markers.EMPTY,
                TypeTree.build(packageName).withPrefix(space),
                emptyList());
    }

    @Override
    public J visitLambdaArgumentExpression(FirLambdaArgumentExpression lambdaArgumentExpression, ExecutionContext ctx) {
        Space prefix = whitespace();
        J j = visitElement(lambdaArgumentExpression.getExpression(), ctx);
        return j.withPrefix(prefix);
    }

    @Override
    public J visitNamedArgumentExpression(FirNamedArgumentExpression namedArgumentExpression, ExecutionContext ctx) {
        Space prefix = whitespace();
        return new J.Assignment(
                randomId(),
                prefix,
                Markers.EMPTY,
                convertToIdentifier(namedArgumentExpression.getName().toString()),
                padLeft(sourceBefore("="), convert(namedArgumentExpression.getExpression(), ctx)),
                typeMapping.type(namedArgumentExpression.getTypeRef()));
    }

    @Override
    public J visitProperty(FirProperty property, ExecutionContext ctx) {
        Space prefix = whitespace();

        List<J> modifiers = emptyList();
        boolean isVal = property.isVal();
        List<J.Annotation> annotations = mapModifiers(isVal ? ModifierScope.VAL : ModifierScope.VAR, property.getAnnotations());

        Markers markers = Markers.EMPTY;
        markers = markers.addIfAbsent(new PropertyClassifier(randomId(), isVal ? sourceBefore("val") : sourceBefore("var"), isVal ? VAL : VAR));

        JRightPadded<J.VariableDeclarations.NamedVariable> receiver = null;
        if (property.getReceiverTypeRef() != null) {
            markers = markers.addIfAbsent(new ReceiverType(randomId()));
            J.Identifier receiverName = (J.Identifier) visitElement(property.getReceiverTypeRef(), ctx);

            // Temporary wrapper to move forward ...
            // The property receiver should be placed in the getter / setter.
            receiver = JRightPadded.build(
                    new J.VariableDeclarations.NamedVariable(
                            randomId(),
                            receiverName.getPrefix(),
                            Markers.EMPTY,
                            receiverName.withPrefix(EMPTY),
                            emptyList(),
                            null,
                            null)
            ).withAfter(sourceBefore("."));
        }

        List<JRightPadded<J.VariableDeclarations.NamedVariable>> vars = new ArrayList<>(1 + (receiver == null ? 0 : 1)); // adjust size if necessary
        if (receiver != null) {
            vars.add(receiver);
        }

        Space namePrefix = whitespace();
        J.Identifier name = convertToIdentifier(property.getName().asString(), property);

        TypeTree typeExpression = null;
        if (property.getReturnTypeRef() instanceof FirResolvedTypeRef) {
            FirResolvedTypeRef typeRef = (FirResolvedTypeRef) property.getReturnTypeRef();
            if (typeRef.getDelegatedTypeRef() != null) {
                Space delimiterPrefix = whitespace();
                boolean addTypeReferencePrefix = source.startsWith(":", cursor);
                skip(":");
                if (addTypeReferencePrefix) {
                    markers = markers.addIfAbsent(new TypeReferencePrefix(randomId(), delimiterPrefix));
                }
                typeExpression = (TypeTree) visitElement(typeRef.getDelegatedTypeRef(), ctx);
            }
        } else {
            throw new IllegalStateException("Implement me.");
        }

        // Dimensions do not exist in Kotlin, and array is declared based on the type. I.E., IntArray
        List<JLeftPadded<Space>> dimensionsAfterName = emptyList();

        J expr = null;
        Space exprPrefix = EMPTY;
        if (property.getInitializer() != null) {
            exprPrefix = sourceBefore("=");
            expr = visitExpression(property.getInitializer(), ctx);
            if (expr instanceof Statement && !(expr instanceof Expression)) {
                expr = new K.StatementExpression(randomId(), (Statement) expr);
            }
        }

        if (property.getGetter() != null && !(property.getGetter() instanceof FirDefaultPropertyGetter)) {
            expr =  visitElement(property.getGetter(), ctx);
            if (expr instanceof Statement && !(expr instanceof Expression)) {
                expr = new K.StatementExpression(randomId(), (Statement) expr);
            }
        }

        if (property.getSetter() != null && !(property.getSetter() instanceof FirDefaultPropertySetter)) {
            throw new UnsupportedOperationException("Explicit setter initialization are not currently supported.");
        }

        JRightPadded<J.VariableDeclarations.NamedVariable> namedVariable = maybeSemicolon(
                new J.VariableDeclarations.NamedVariable(
                        randomId(),
                        namePrefix,
                        Markers.EMPTY,
                        name,
                        dimensionsAfterName,
                        expr == null ? null : padLeft(exprPrefix, (Expression) expr),
                        typeMapping.variableType(property.getSymbol(), null, getCurrentFile())
                )
        );
        vars.add(namedVariable);

        return new J.VariableDeclarations(
                randomId(),
                prefix,
                markers,
                annotations,
                emptyList(),
                typeExpression,
                null,
                dimensionsAfterName,
                vars);
    }

    @Override
    public J visitPropertyAccessExpression(FirPropertyAccessExpression propertyAccessExpression, ExecutionContext ctx) {
        JavaType type = typeMapping.type(propertyAccessExpression);
        if (propertyAccessExpression.getExplicitReceiver() != null) {
            Space prefix = whitespace();
            Expression target = (Expression) visitElement(propertyAccessExpression.getExplicitReceiver(), ctx);
            JLeftPadded<J.Identifier> name = padLeft(sourceBefore("."), (J.Identifier) visitElement(propertyAccessExpression.getCalleeReference(), ctx));
            return new J.FieldAccess(
                    randomId(),
                    prefix,
                    Markers.EMPTY,
                    target,
                    name,
                    type
            );
        } else {
            return visitElement(propertyAccessExpression.getCalleeReference(), ctx);
        }
    }

    @Override
    public J visitPropertyAccessor(FirPropertyAccessor propertyAccessor, ExecutionContext ctx) {
        Space prefix = whitespace();
        if (propertyAccessor.isGetter()) {
            Markers markers = Markers.EMPTY;
            List<J> modifiers = emptyList();
            List<J.Annotation> annotations = mapAnnotations(propertyAccessor.getAnnotations());

            JRightPadded<J.VariableDeclarations.NamedVariable> infixReceiver = null;

            J.TypeParameters typeParameters = propertyAccessor.getTypeParameters().isEmpty() ? null :
                    new J.TypeParameters(randomId(), sourceBefore("<"), Markers.EMPTY,
                            emptyList(),
                            convertAll(propertyAccessor.getTypeParameters(), commaDelim, t -> sourceBefore(">"), ctx));

            String methodName = "get";
            skip(methodName);
            J.Identifier name = new J.Identifier(
                    randomId(),
                    EMPTY,
                    Markers.EMPTY,
                    methodName,
                    typeMapping.type(propertyAccessor),
                    null);

            JContainer<Statement> params;
            Space paramFmt = sourceBefore("(");
            params = JContainer.build(paramFmt, singletonList(padRight(new J.Empty(randomId(), sourceBefore(")"), Markers.EMPTY), EMPTY)), Markers.EMPTY);

            int saveCursor = cursor;
            Space nextPrefix = whitespace();
            TypeTree returnTypeExpression = null;
            // Only add the type reference if it exists in source code.
            if (!(propertyAccessor.getReturnTypeRef() instanceof FirImplicitUnitTypeRef) && source.startsWith(":", cursor)) {
                skip(":");
                markers = markers.addIfAbsent(new TypeReferencePrefix(randomId(), nextPrefix));
                returnTypeExpression = (TypeTree) visitElement(propertyAccessor.getReturnTypeRef(), ctx);
            } else {
                cursor = saveCursor;
            }

            J.Block body = null;
            saveCursor = cursor;
            Space blockPrefix = whitespace();
            if (propertyAccessor.getBody() instanceof FirSingleExpressionBlock) {
                if (source.startsWith("=", cursor)) {
                    skip("=");
                    SingleExpressionBlock singleExpressionBlock = new SingleExpressionBlock(randomId());

                    body = convertOrNull(propertyAccessor.getBody(), ctx);
                    body = body.withPrefix(blockPrefix);
                    body = body.withMarkers(body.getMarkers().addIfAbsent(singleExpressionBlock));
                }
            } else {
                cursor = saveCursor;
                body = convertOrNull(propertyAccessor.getBody(), ctx);
            }

            return new J.MethodDeclaration(
                    randomId(),
                    prefix,
                    markers,
                    annotations == null ? emptyList() : annotations,
                    emptyList(),
                    typeParameters,
                    returnTypeExpression,
                    new J.MethodDeclaration.IdentifierWithAnnotations(name, emptyList()),
                    params,
                    null,
                    body,
                    null,
                    typeMapping.methodDeclarationType(propertyAccessor, null, getCurrentFile()));
        }

        throw new UnsupportedOperationException("Unsupported property accessor.");
    }

    @Override
    public J visitResolvedNamedReference(FirResolvedNamedReference resolvedNamedReference, ExecutionContext ctx) {
        String name = resolvedNamedReference.getName().asString();
        return convertToIdentifier(name, resolvedNamedReference);
    }

    @Override
    public J visitReturnExpression(FirReturnExpression returnExpression, ExecutionContext ctx) {
        if (returnExpression.getResult() instanceof FirUnitExpression) {
            Space prefix = whitespace();
            if (source.startsWith("return", cursor)) {
                throw new IllegalStateException("FirReturnExpression.");
            }

            return new K.Return(randomId(), prefix, Markers.EMPTY, null);
        }

        return visitElement(returnExpression.getResult(), ctx);
    }

    @Override
    public J visitResolvedTypeRef(FirResolvedTypeRef resolvedTypeRef, ExecutionContext ctx) {
        if (resolvedTypeRef.getDelegatedTypeRef() != null) {
            J j = visitElement(resolvedTypeRef.getDelegatedTypeRef(), ctx);
            if (j instanceof TypeTree) {
                j = ((TypeTree) j).withType(typeMapping.type(resolvedTypeRef));
            }
            return j;
        }
        throw new UnsupportedOperationException("Unsupported null delegated type reference.");
    }

    @Override
    public J visitResolvedQualifier(FirResolvedQualifier resolvedQualifier, ExecutionContext ctx) {
        return convertToIdentifier(resolvedQualifier.getRelativeClassFqName().asString(), resolvedQualifier);
    }

    @Override
    public J visitSimpleFunction(FirSimpleFunction simpleFunction, ExecutionContext ctx) {
        Space prefix = whitespace();
        Markers markers = Markers.EMPTY;
        List<J> modifiers = emptyList();
        List<J.Annotation> annotations = mapModifiers(ModifierScope.FUNCTION, simpleFunction.getAnnotations());

        boolean isInfix = annotations.stream()
                .filter(it -> it.getAnnotationType() instanceof J.Identifier)
                .map(it -> (J.Identifier) it.getAnnotationType())
                .anyMatch(it -> "infix".equals(it.getSimpleName()));

        JRightPadded<J.VariableDeclarations.NamedVariable> infixReceiver = null;
        if (isInfix && simpleFunction.getReceiverTypeRef() != null) {
            // Infix functions are de-sugared during the backend phase of the compiler.
            // The de-sugaring process moves the infix receiver to the first position of the method declaration.
            // The infix receiver is added as to the `J.MethodInvocation` parameters, and marked to distinguish the parameter.
            markers = markers.addIfAbsent(new ReceiverType(randomId()));
            J.Identifier receiver = (J.Identifier) visitElement(simpleFunction.getReceiverTypeRef(), ctx);
            infixReceiver = JRightPadded.build(new J.VariableDeclarations.NamedVariable(
                    randomId(),
                    EMPTY,
                    Markers.EMPTY,
                    receiver,
                    emptyList(),
                    null,
                    null))
                    .withAfter(sourceBefore("."));
        }

        J.TypeParameters typeParameters = simpleFunction.getTypeParameters().isEmpty() ? null :
                new J.TypeParameters(randomId(), sourceBefore("<"), Markers.EMPTY,
                emptyList(),
                convertAll(simpleFunction.getTypeParameters(), commaDelim, t -> sourceBefore(">"), ctx));

        String methodName = "";
        if ("<no name provided>".equals(simpleFunction.getName().asString())) {
            // Extract name from source.
            throw new IllegalStateException("Unresolved function.");
        } else {
            methodName = simpleFunction.getName().asString();
        }

        J.Identifier name = convertToIdentifier(methodName, simpleFunction);

        JContainer<Statement> params;
        Space paramFmt = sourceBefore("(");
        params = !simpleFunction.getValueParameters().isEmpty() ?
                JContainer.build(paramFmt, convertAll(simpleFunction.getValueParameters(), commaDelim, t -> sourceBefore(")"), ctx), Markers.EMPTY) :
                JContainer.build(paramFmt, singletonList(padRight(new J.Empty(randomId(), sourceBefore(")"), Markers.EMPTY), EMPTY)), Markers.EMPTY);

        if (isInfix) {
            // Insert the infix receiver to the list of parameters.
            J.VariableDeclarations implicitParam = new J.VariableDeclarations(
                    randomId(),
                    EMPTY,
                    Markers.EMPTY.addIfAbsent(new ReceiverType(randomId())),
                    emptyList(),
                    emptyList(),
                    null,
                    null,
                    emptyList(),
                    singletonList(infixReceiver));
            implicitParam = implicitParam.withMarkers(implicitParam.getMarkers().addIfAbsent(new TypeReferencePrefix(randomId(), EMPTY)));
            List<JRightPadded<Statement>> newStatements = new ArrayList<>(params.getElements().size() + 1);
            newStatements.add(JRightPadded.build(implicitParam));
            newStatements.addAll(params.getPadding().getElements());
            params = params.getPadding().withElements(newStatements);
        }

        TypeTree returnTypeExpression = null;
        if (!(simpleFunction.getReturnTypeRef() instanceof FirImplicitUnitTypeRef)) {
            Space delimiterPrefix = whitespace();
            boolean addTypeReferencePrefix = source.startsWith(":", cursor);
            skip(":");
            if (addTypeReferencePrefix) {
                markers = markers.addIfAbsent(new TypeReferencePrefix(randomId(), delimiterPrefix));
            }
            returnTypeExpression = (TypeTree) visitElement(simpleFunction.getReturnTypeRef(), ctx);
        }

        J.Block body;
        int saveCursor = cursor;
        Space blockPrefix = whitespace();
        if (simpleFunction.getBody() instanceof FirSingleExpressionBlock) {
            if (source.startsWith("=", cursor)) {
                skip("=");
                SingleExpressionBlock singleExpressionBlock = new SingleExpressionBlock(randomId());

                body = convertOrNull(simpleFunction.getBody(), ctx);
                body = body.withPrefix(blockPrefix);
                body = body.withMarkers(body.getMarkers().addIfAbsent(singleExpressionBlock));
            } else {
                throw new IllegalStateException("Implement me.");
            }
        } else {
            cursor = saveCursor;
            body = convertOrNull(simpleFunction.getBody(), ctx);
        }

        return new J.MethodDeclaration(
                randomId(),
                prefix,
                markers,
                annotations,
                emptyList(),
                typeParameters,
                returnTypeExpression,
                new J.MethodDeclaration.IdentifierWithAnnotations(name, emptyList()),
                params,
                null,
                body,
                null,
                typeMapping.methodDeclarationType(simpleFunction, null, getCurrentFile()));
    }

    @Override
    public J visitStarProjection(FirStarProjection starProjection, ExecutionContext ctx) {
        Space prefix = whitespace();
        skip("*");
        return new J.Wildcard(randomId(), prefix, Markers.EMPTY, null, null);
    }

    @Override
    public J visitStringConcatenationCall(FirStringConcatenationCall stringConcatenationCall, ExecutionContext ctx) {
        Space prefix = whitespace();
        String delimiter;
        if (source.startsWith("\"\"\"", cursor)) {
            delimiter = "\"\"\"";
        } else if (source.startsWith("$", cursor)) {
            delimiter = "$";
        } else {
            delimiter = "\"";
        }
        cursor += delimiter.length();
        List<J> values = new ArrayList<>(stringConcatenationCall.getArgumentList().getArguments().size());
        for (FirExpression e : stringConcatenationCall.getArgumentList().getArguments()) {
            if (source.startsWith("$", cursor)) {
                skip("$");
                boolean inBraces = source.startsWith("{", cursor);
                if (inBraces) {
                    skip("{");
                }
                values.add(new K.KString.Value(randomId(), Markers.EMPTY, visitElement(e, ctx), inBraces));
                if (inBraces) {
                    skip("}");
                }
            } else {
                values.add(visitElement(e, ctx));
            }
        }
        cursor += delimiter.length();
        return new K.KString(
                randomId(),
                prefix,
                Markers.EMPTY,
                delimiter,
                values,
                typeMapping.type(stringConcatenationCall)
        );
    }

    @Override
    public J visitThisReceiverExpression(FirThisReceiverExpression thisReceiverExpression, ExecutionContext ctx) {
        Space prefix = sourceBefore("this");

        return new J.Identifier(randomId(),
                prefix,
                Markers.EMPTY,
                "this",
                typeMapping.type(thisReceiverExpression),
                null);
    }

    @Override
    public J visitTypeParameter(FirTypeParameter typeParameter, ExecutionContext ctx) {
        if (!typeParameter.getAnnotations().isEmpty()) {
            throw new IllegalStateException("Implement me.");
        }

        Space prefix = whitespace();
        Markers markers = Markers.EMPTY;
        List<J.Annotation> annotations = new ArrayList<>(typeParameter.getAnnotations().size() + (typeParameter.isReified() ? 1 : 0));

        if (typeParameter.isReified()) {
            // Add reified as an annotation to preserve whitespace.
            J.Identifier name = new J.Identifier(
                    randomId(),
                    EMPTY,
                    Markers.EMPTY,
                    "reified",
                    null,
                    null
            );

            J.Annotation reified = new J.Annotation(randomId(), sourceBefore("reified"), Markers.EMPTY.addIfAbsent(new Modifier(randomId())), name, JContainer.empty());
            annotations.add(reified);
        }

        Expression name = convertToIdentifier(typeParameter.getName().asString(), typeParameter);

        List<FirTypeRef> nonImplicitParams = typeParameter.getBounds().stream().filter(it -> !(it instanceof FirImplicitNullableAnyTypeRef)).collect(Collectors.toList());
        JContainer<TypeTree> bounds = null;
        if (nonImplicitParams.size() == 1) {
            bounds = JContainer.build(sourceBefore(":"),
                    convertAll(nonImplicitParams, t -> sourceBefore(","), noDelim, ctx), Markers.EMPTY);
        }

        return new J.TypeParameter(randomId(), prefix, markers, annotations, name, bounds);
    }

    @Override
    public J visitTypeProjectionWithVariance(FirTypeProjectionWithVariance typeProjectionWithVariance, ExecutionContext ctx) {
        return visitResolvedTypeRef((FirResolvedTypeRef) typeProjectionWithVariance.getTypeRef(), ctx);
    }

    @Override
    public J visitUserTypeRef(FirUserTypeRef userTypeRef, ExecutionContext ctx) {
        Markers markers = Markers.EMPTY;
        StringBuilder name = new StringBuilder();
        List<FirQualifierPart> qualifier = userTypeRef.getQualifier();
        for (int i = 0; i < qualifier.size(); i++) {
            FirQualifierPart part = qualifier.get(i);
            name.append(part.getName().asString());
            if (i < qualifier.size() - 1) {
                if (!part.getTypeArgumentList().getTypeArguments().isEmpty()) {
                    throw new UnsupportedOperationException("Unsupported type parameters in user part " + part.getName());
                }
                name.append(".");
            }
        }

        Space prefix = whitespace();
        NameTree nameTree = TypeTree.build(name.toString());
        skip(name.toString());
        FirQualifierPart part = userTypeRef.getQualifier().get(userTypeRef.getQualifier().size() - 1);
        if (!part.getTypeArgumentList().getTypeArguments().isEmpty()) {
            Space typeArgPrefix = sourceBefore("<");
            List<JRightPadded<Expression>> parameters = new ArrayList<>(part.getTypeArgumentList().getTypeArguments().size());
            List<FirTypeProjection> typeArguments = part.getTypeArgumentList().getTypeArguments();
            for (int i = 0; i < typeArguments.size(); i++) {
                FirTypeProjection typeArgument = typeArguments.get(i);
                parameters.add(JRightPadded.build((Expression) visitElement(typeArgument, ctx))
                        .withAfter(
                        i < typeArguments.size() - 1 ?
                                sourceBefore(",") :
                                sourceBefore(">")
                ));
            }

            if (userTypeRef.isMarkedNullable()) {
                markers = markers.addIfAbsent(new IsNullable(randomId(), sourceBefore("?")));
            }

            return new J.ParameterizedType(
                    randomId(),
                    prefix,
                    markers,
                    nameTree,
                    JContainer.build(typeArgPrefix, parameters, Markers.EMPTY)
            );
        } else {
            if (userTypeRef.isMarkedNullable()) {
                markers = markers.addIfAbsent(new IsNullable(randomId(), sourceBefore("?")));
            }

            return nameTree.withPrefix(prefix)
                        .withMarkers(markers);
        }
    }

    // Merge visitValueParameter and visitProperty
    @Override
    public J visitValueParameter(FirValueParameter valueParameter, ExecutionContext ctx) {
        Space prefix = whitespace();

        List<J> modifiers = emptyList();
        List<J.Annotation> annotations = mapAnnotations(valueParameter.getAnnotations());
        Markers markers = Markers.EMPTY;

        List<JRightPadded<J.VariableDeclarations.NamedVariable>> vars = new ArrayList<>(1); // adjust size if necessary
        Space namePrefix = EMPTY;
        String valueName = "";
        if ("<no name provided>".equals(valueParameter.getName().toString())) {
            KtSourceElement sourceElement = valueParameter.getSource();
            if (sourceElement == null) {
                throw new IllegalStateException("Unexpected null source.");
            }
        } else {
            valueName = valueParameter.getName().asString();
            namePrefix = whitespace();
        }
        J.Identifier name = convertToIdentifier(valueName, valueParameter);

        TypeTree typeExpression = null;
        if (valueParameter.getReturnTypeRef() instanceof FirResolvedTypeRef) {
            FirResolvedTypeRef typeRef = (FirResolvedTypeRef) valueParameter.getReturnTypeRef();
            if (typeRef.getDelegatedTypeRef() != null) {
                Space delimiterPrefix = whitespace();
                boolean addTypeReferencePrefix = source.startsWith(":", cursor);
                skip(":");
                if (addTypeReferencePrefix) {
                    markers = markers.addIfAbsent(new TypeReferencePrefix(randomId(), delimiterPrefix));
                }
                J j = visitElement(typeRef.getDelegatedTypeRef(), ctx);
                if (j instanceof TypeTree) {
                    typeExpression = (TypeTree) j;
                } else {
                    typeExpression = new K.FunctionType(randomId(), (TypedTree) j, null);
                }
            }
        } else {
            throw new IllegalStateException("Implement me.");
        }

        // Dimensions do not exist in Kotlin, and array is declared based on the type. I.E., IntArray
        List<JLeftPadded<Space>> dimensionsAfterName = emptyList();

        FirExpression initializer = valueParameter.getInitializer() != null ? valueParameter.getInitializer() : valueParameter.getDefaultValue() != null ? valueParameter.getDefaultValue() : null;
        JRightPadded<J.VariableDeclarations.NamedVariable> namedVariable = maybeSemicolon(
                new J.VariableDeclarations.NamedVariable(
                        randomId(),
                        namePrefix,
                        Markers.EMPTY,
                        name,
                        dimensionsAfterName,
                        initializer != null ? padLeft(sourceBefore("="), (Expression) visitExpression(initializer, ctx)) : null,
                        typeMapping.variableType(valueParameter.getSymbol(), null, getCurrentFile())
                )
        );
        vars.add(namedVariable);

        return new J.VariableDeclarations(
                randomId(),
                prefix,
                markers,
                annotations == null ? emptyList() : annotations,
                emptyList(),
                typeExpression,
                null,
                dimensionsAfterName,
                vars);
    }

    @Override
    public J visitVariableAssignment(FirVariableAssignment variableAssignment, ExecutionContext ctx) {
        boolean unaryAssignment = variableAssignment.getRValue() instanceof FirFunctionCall &&
                ((FirFunctionCall) variableAssignment.getRValue()).getOrigin() == FirFunctionCallOrigin.Operator &&
                ((FirFunctionCall) variableAssignment.getRValue()).getCalleeReference() instanceof FirResolvedNamedReference &&
                isUnaryOperator((((FirFunctionCall) variableAssignment.getRValue()).getCalleeReference()).getName().asString());

        if (unaryAssignment) {
            return visitElement(variableAssignment.getRValue(), ctx);
        } else {
            Space prefix = whitespace();

            Expression variable;
            if (variableAssignment.getExplicitReceiver() != null) {
                Expression target = (Expression) visitElement(variableAssignment.getExplicitReceiver(), ctx);
                JLeftPadded<J.Identifier> name = padLeft(sourceBefore("."), (J.Identifier) visitElement(variableAssignment.getLValue(), ctx));
                variable = new J.FieldAccess(
                        randomId(),
                        EMPTY,
                        Markers.EMPTY,
                        target,
                        name,
                        typeMapping.type(variableAssignment, currentFile.getSymbol()));
            } else {
                variable = convert(variableAssignment.getLValue(), ctx);
            }

            int saveCursor = cursor;
            whitespace();
            boolean isCompoundAssignment = source.startsWith("-=", cursor) ||
                    source.startsWith("+=", cursor) ||
                    source.startsWith("*=", cursor) ||
                    source.startsWith("/=", cursor);
            cursor = saveCursor;

            if (isCompoundAssignment) {
                Space opPrefix = whitespace();
                J.AssignmentOperation.Type op;
                if (source.startsWith("-=", cursor)) {
                    skip("-=");
                    op = J.AssignmentOperation.Type.Subtraction;
                } else if (source.startsWith("+=", cursor)) {
                    skip("+=");
                    op = J.AssignmentOperation.Type.Addition;
                } else if (source.startsWith("*=", cursor)) {
                    skip("*=");
                    op = J.AssignmentOperation.Type.Multiplication;
                } else if (source.startsWith("/=", cursor)) {
                    skip("/=");
                    op = J.AssignmentOperation.Type.Division;
                } else {
                    throw new IllegalArgumentException("Unexpected compound assignment.");
                }

                if (!(variableAssignment.getRValue() instanceof FirFunctionCall) ||
                        (((FirFunctionCall) variableAssignment.getRValue())).getArgumentList().getArguments().size() != 1) {
                    throw new IllegalArgumentException("Unexpected compound assignment.");
                }

                FirElement rhs = (((FirFunctionCall) variableAssignment.getRValue())).getArgumentList().getArguments().get(0);
                return new J.AssignmentOperation(
                        randomId(),
                        prefix,
                        Markers.EMPTY,
                        variable,
                        padLeft(opPrefix, op),
                        (Expression) visitElement(rhs, ctx),
                        typeMapping.type(variableAssignment));
            } else {
                return new J.Assignment(
                        randomId(),
                        prefix,
                        Markers.EMPTY,
                        variable,
                        padLeft(sourceBefore("="), convert(variableAssignment.getRValue(), ctx)),
                        typeMapping.type(variableAssignment));
            }
        }
    }

    @Override
    public J visitWhenBranch(FirWhenBranch whenBranch, ExecutionContext ctx) {
        Space prefix = whitespace();
        if (source.substring(cursor).startsWith("when")) {
            throw new UnsupportedOperationException("When conditions are currently unsupported.");
        } else if (source.substring(cursor).startsWith("if")) {
            skip("if");
        } else if (!(whenBranch.getCondition() instanceof FirElseIfTrueCondition ||
                whenBranch.getCondition() instanceof FirEqualityOperatorCall)) {
            throw new UnsupportedOperationException("Unsupported condition type.");
        }

        boolean singleExpression = whenBranch.getResult() instanceof FirSingleExpressionBlock;
        if (whenBranch.getCondition() instanceof FirElseIfTrueCondition) {
            FirElement result = singleExpression ? ((FirSingleExpressionBlock) whenBranch.getResult()).getStatement() : whenBranch.getResult();
            J j = visitElement(result, ctx);
            return j.withPrefix(prefix);
        } else {
            J.ControlParentheses<Expression> controlParentheses = convertToControlParentheses(whenBranch.getCondition());

            FirElement result = singleExpression ? ((FirSingleExpressionBlock) whenBranch.getResult()).getStatement() : whenBranch.getResult();
            return new J.If(
                    randomId(),
                    prefix,
                    Markers.EMPTY,
                    controlParentheses,
                    JRightPadded.build(convert(result, ctx)),
                    null
            );
        }
    }

    @Override
    public J visitWhenExpression(FirWhenExpression whenExpression, ExecutionContext ctx) {
        FirWhenBranch whenBranch = whenExpression.getBranches().get(0);
        J firstElement = visitElement(whenBranch, ctx);
        if (!(firstElement instanceof J.If)) {
            throw new IllegalStateException("Implement me.");
        }

        J.If ifStatement = (J.If) firstElement;

        List<J> elses = new ArrayList<>(whenExpression.getBranches().size() - 1);
        List<FirWhenBranch> branches = whenExpression.getBranches();
        for (int i = 1; i < branches.size(); i++) {
            FirWhenBranch branch = branches.get(i);

            Space elsePrefix = sourceBefore("else");
            J j = visitWhenBranch(branch, ctx);
            J.If.Else ifElse = new J.If.Else(
                    randomId(),
                    elsePrefix,
                    Markers.EMPTY,
                    JRightPadded.build((Statement) j)
            );
            elses.add(ifElse);
        }

        elses.add(0, ifStatement);
        J.If.Else ifElse = null;
        for (int i = elses.size() - 1; i >= 0; i--) {
            J j = elses.get(i);
            if (j instanceof J.If.Else) {
                if (((J.If.Else) j).getBody() instanceof J.If) {
                    J.If addElse = (J.If) ((J.If.Else) j).getBody();
                    addElse = addElse.withElsePart(ifElse);
                    j = ((J.If.Else) j).withBody(addElse);
                }
                ifElse = (J.If.Else) j;
            } else if (j instanceof J.If) {
                ifStatement = ((J.If) j).withElsePart(ifElse);
            }
        }

        return ifStatement;
    }

    @Override
    public J visitWhileLoop(FirWhileLoop whileLoop, ExecutionContext ctx) {
        Space prefix = whitespace();

        if (source.startsWith("while", cursor)) {
            skip("while");
            J.ControlParentheses<Expression> controlParentheses = convertToControlParentheses(whileLoop.getCondition());
            Statement body = (Statement) visitElement(whileLoop.getBlock(), ctx);
            return new J.WhileLoop(
                    randomId(),
                    prefix,
                    Markers.EMPTY,
                    controlParentheses,
                    JRightPadded.build(body));
        } else if (source.startsWith("for", cursor)) {
            // The structure of the for only exists in the PSI.
            skip("for");

//            Statement body = (Statement) visitElement(whileLoop.getBlock(), ctx);
//            return new J.ForEachLoop(
//                    randomId(),
//                    prefix,
//                    Markers.EMPTY,
//                    null,
//                    JRightPadded.build(body));
            throw new UnsupportedOperationException("For loop is not supported");
        } else {
            throw new UnsupportedOperationException("Unsupported loop starts with " + source.substring(cursor, cursor + 10));
        }
    }

    @Override
    public J visitElement(FirElement firElement, ExecutionContext ctx) {
        if (firElement instanceof FirErrorNamedReference) {
            return visitErrorNamedReference((FirErrorNamedReference) firElement, ctx);
        } else if (firElement instanceof FirAnnotationCall) {
            return visitAnnotationCall((FirAnnotationCall) firElement, ctx);
        } else if (firElement instanceof FirAnonymousFunction) {
            return visitAnonymousFunction((FirAnonymousFunction) firElement, ctx);
        } else if (firElement instanceof FirAnonymousFunctionExpression) {
            return visitAnonymousFunctionExpression((FirAnonymousFunctionExpression) firElement, ctx);
        } else if (firElement instanceof FirAnonymousObject) {
            return visitAnonymousObject((FirAnonymousObject) firElement, ctx);
        }  else if (firElement instanceof FirAnonymousObjectExpression) {
            return visitAnonymousObjectExpression((FirAnonymousObjectExpression) firElement, ctx);
        } else if (firElement instanceof FirBinaryLogicExpression) {
            return visitBinaryLogicExpression((FirBinaryLogicExpression) firElement, ctx);
        } else if (firElement instanceof FirBlock) {
            return visitBlock((FirBlock) firElement, ctx);
        } else if (firElement instanceof FirBreakExpression) {
            return visitBreakExpression((FirBreakExpression) firElement, ctx);
        } else if (firElement instanceof FirClass) {
            return visitClass((FirClass) firElement, ctx);
        } else if (firElement instanceof FirComparisonExpression) {
            return visitComparisonExpression((FirComparisonExpression) firElement, ctx);
        } else if (firElement instanceof FirConstExpression) {
            return visitConstExpression((FirConstExpression<?>) firElement, ctx);
        } else if (firElement instanceof FirContinueExpression) {
            return visitContinueExpression((FirContinueExpression) firElement, ctx);
        } else if (firElement instanceof FirDoWhileLoop) {
            return visitDoWhileLoop((FirDoWhileLoop) firElement, ctx);
        } else if (firElement instanceof FirEnumEntry) {
            return visitEnumEntry((FirEnumEntry) firElement, ctx);
        } else if (firElement instanceof FirEqualityOperatorCall) {
            return visitEqualityOperatorCall((FirEqualityOperatorCall) firElement, ctx);
        } else if (firElement instanceof FirFunctionCall) {
            return visitFunctionCall((FirFunctionCall) firElement, ctx);
        } else if (firElement instanceof FirFunctionTypeRef) {
            return visitFunctionTypeRef((FirFunctionTypeRef) firElement, ctx);
        } else if (firElement instanceof FirLambdaArgumentExpression) {
            return visitLambdaArgumentExpression((FirLambdaArgumentExpression) firElement, ctx);
        } else if (firElement instanceof FirNamedArgumentExpression) {
            return visitNamedArgumentExpression((FirNamedArgumentExpression) firElement, ctx);
        } else if (firElement instanceof FirProperty) {
            return visitProperty((FirProperty) firElement, ctx);
        } else if (firElement instanceof FirPropertyAccessExpression) {
            return visitPropertyAccessExpression((FirPropertyAccessExpression) firElement, ctx);
        } else if (firElement instanceof FirPropertyAccessor) {
            return visitPropertyAccessor((FirPropertyAccessor) firElement, ctx);
        } else if (firElement instanceof FirResolvedNamedReference) {
            return visitResolvedNamedReference((FirResolvedNamedReference) firElement, ctx);
        } else if (firElement instanceof FirResolvedTypeRef) {
            return visitResolvedTypeRef((FirResolvedTypeRef) firElement, ctx);
        } else if (firElement instanceof FirResolvedQualifier) {
            return visitResolvedQualifier((FirResolvedQualifier) firElement, ctx);
        } else if (firElement instanceof FirReturnExpression) {
            return visitReturnExpression((FirReturnExpression) firElement, ctx);
        } else if (firElement instanceof FirSimpleFunction) {
            return visitSimpleFunction((FirSimpleFunction) firElement, ctx);
        } else if (firElement instanceof FirStarProjection) {
            return visitStarProjection((FirStarProjection) firElement, ctx);
        } else if (firElement instanceof FirStringConcatenationCall) {
            return visitStringConcatenationCall((FirStringConcatenationCall) firElement, ctx);
        } else if (firElement instanceof FirThisReceiverExpression) {
            return visitThisReceiverExpression((FirThisReceiverExpression) firElement, ctx);
        } else if (firElement instanceof FirTypeParameter) {
            return visitTypeParameter((FirTypeParameter) firElement, ctx);
        } else if (firElement instanceof FirTypeProjectionWithVariance) {
            return visitTypeProjectionWithVariance((FirTypeProjectionWithVariance) firElement, ctx);
        } else if (firElement instanceof FirUserTypeRef) {
            return visitUserTypeRef((FirUserTypeRef) firElement, ctx);
        } else if (firElement instanceof FirValueParameter) {
            return visitValueParameter((FirValueParameter) firElement, ctx);
        } else if (firElement instanceof FirVariableAssignment) {
            return visitVariableAssignment((FirVariableAssignment) firElement, ctx);
        }  else if (firElement instanceof FirWhenBranch) {
            return visitWhenBranch((FirWhenBranch) firElement, ctx);
        } else if (firElement instanceof FirWhenExpression) {
            return visitWhenExpression((FirWhenExpression) firElement, ctx);
        } else if (firElement instanceof FirWhileLoop) {
            return visitWhileLoop((FirWhileLoop) firElement, ctx);
        }

        throw new IllegalStateException("Implement me.");
    }

    private final Function<FirElement, Space> commaDelim = ignored -> sourceBefore(",");
    private final Function<FirElement, Space> noDelim = ignored -> EMPTY;

    private void skip(@Nullable String token) {
        if (token != null && source.startsWith(token, cursor)) {
            cursor += token.length();
        }
    }

    private void cursor(int n) {
        cursor = n;
    }

    private int endPos(FirElement t) {
        if (t.getSource() == null) {
            throw new IllegalStateException("Unexpected null source ... fix me.");
        }
        return t.getSource().getEndOffset();
    }

    @SuppressWarnings("unchecked")
    private <J2 extends J> J2 convert(FirElement t, ExecutionContext ctx) {
        return (J2) visitElement(t, ctx);
    }

    @SuppressWarnings("unchecked")
    private <J2 extends J> JRightPadded<J2> convert(FirElement t, Function<FirElement, Space> suffix, ExecutionContext ctx) {
        J2 j = (J2) visitElement(t, ctx);
        @SuppressWarnings("ConstantConditions")
        JRightPadded<J2> rightPadded = j == null ? null :
                new JRightPadded<>(j, suffix.apply(t), Markers.EMPTY);
        cursor(max(endPos(t), cursor)); // if there is a non-empty suffix, the cursor may have already moved past it
        return rightPadded;
    }

    @Nullable
    private <T extends J> T convertOrNull(@Nullable FirElement t, ExecutionContext ctx) {
        return t == null ? null : convert(t, ctx);
    }

    private <J2 extends J> List<JRightPadded<J2>> convertAll(List<? extends FirElement> elements,
                                                             Function<FirElement, Space> innerSuffix,
                                                             Function<FirElement, Space> suffix,
                                                             ExecutionContext ctx) {
        if (elements.isEmpty()) {
            return emptyList();
        }
        List<JRightPadded<J2>> converted = new ArrayList<>(elements.size());
        for (int i = 0; i < elements.size(); i++) {
            converted.add(convert(elements.get(i), i == elements.size() - 1 ? suffix : innerSuffix, ctx));
        }
        return converted;
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

    @SuppressWarnings("SameParameterValue")
    private <T> JRightPadded<T> padRight(T tree, @Nullable Space right) {
        return new JRightPadded<>(tree, right == null ? EMPTY : right, Markers.EMPTY);
    }

    private enum ModifierScope {
        CLASS("class"), FUNCTION("fun"), INTERFACE("interface"), VAL("val"), VAR("var");

        private final String keyword;
        ModifierScope(String keyword) {
            this.keyword = keyword;
        }

        public String getKeyword() {
            return keyword;
        }
    }

    private List<J.Annotation> mapModifiers(ModifierScope modifierScope, List<FirAnnotation> firAnnotations) {
        List<FirAnnotation> findMatch = new ArrayList<>(firAnnotations.size());
        findMatch.addAll(firAnnotations);

        List<J.Annotation> modifiers = new ArrayList<>();
        int count = 0;
        while (count < 10) {
            int saveCursor = cursor;
            Space prefix = whitespace();

            if (source.startsWith(modifierScope.getKeyword() + " ", cursor)) {
                if (ModifierScope.FUNCTION == modifierScope) {
                    K.Modifier modifier = mapModifier(prefix, emptyList());
                    J.Annotation annotation = convertToAnnotation(modifier);
                    modifiers.add(annotation);
                } else {
                    cursor = saveCursor;
                }
                return modifiers;
            } else if (source.startsWith("@", cursor)) {
                cursor = saveCursor;
                J.Annotation annotation = mapAnnotation(findMatch);
                modifiers.add(annotation);
            } else {
                K.Modifier modifier = mapModifier(prefix, emptyList());
                J.Annotation annotation = convertToAnnotation(modifier);
                modifiers.add(annotation);
            }

            count++;
        }

        return modifiers;
    }


    private J.Binary.Type mapOperation(FirOperation firOp) {
        J.Binary.Type op = null;
        switch (firOp) {
            case EQ:
                skip("=");
                op = J.Binary.Type.Equal;
                break;
            case NOT_EQ:
                skip("!=");
                op = J.Binary.Type.NotEqual;
                break;
            case GT:
                skip(">");
                op = J.Binary.Type.GreaterThan;
                break;
            case GT_EQ:
                skip(">=");
                op = J.Binary.Type.GreaterThanOrEqual;
                break;
            case LT:
                skip("<");
                op = J.Binary.Type.LessThan;
                break;
            case LT_EQ:
                skip("<=");
                op = J.Binary.Type.LessThan;
                break;
            default:
                throw new UnsupportedOperationException("Unsupported FirOperation " + op.name());
        }

        return op;
    }

    private boolean isUnaryOperator(String name) {
        return "dec".equals(name) ||
                "inc".equals(name) ||
                "not".equals(name) ||
                "unaryMinus".equals(name) ||
                "unaryPlus".equals(name);
    }

    private J.ControlParentheses<Expression> convertToControlParentheses(FirElement firElement) {
        Space controlParenPrefix = whitespace();
        skip("(");
        return new J.ControlParentheses<>(randomId(), controlParenPrefix, Markers.EMPTY,
                convert(firElement, t -> sourceBefore(")"), ctx));
    }

    private J.Identifier convertToIdentifier(String name) {
        return convertToIdentifier(name, null, null);
    }

    private J.Identifier convertToIdentifier(String name, FirElement firElement) {
        return convertToIdentifier(name, typeMapping.type(firElement, getCurrentFile()), null);
    }

    @SuppressWarnings("SameParameterValue")
    private J.Identifier convertToIdentifier(String name, @Nullable JavaType type, @Nullable JavaType.Variable fieldType) {
        Space prefix = whitespace();
        boolean isQuotedSymbol = source.startsWith("`", cursor);
        String value;
        if (isQuotedSymbol) {
            skip("`");
            value = source.substring(cursor, cursor + source.substring(cursor).indexOf('`'));
            skip(value);
            skip("`");
            value = "`" + value + "`";
        } else {
            value = name;
            skip(value);
        }

        return new J.Identifier(
                randomId(),
                prefix,
                Markers.EMPTY,
                value,
                type,
                fieldType
        );
    }

    private J.Annotation convertToAnnotation(K.Modifier modifier) {
        J.Identifier name = new J.Identifier(randomId(),
                EMPTY,
                Markers.EMPTY,
                modifier.getType().name().toLowerCase(),
                null,
                null);
        return new J.Annotation(randomId(), modifier.getPrefix(), Markers.EMPTY.addIfAbsent(new Modifier(randomId())), name, JContainer.empty());
    }

    @Nullable
    private List<J.Annotation> mapAnnotations(List<FirAnnotation> firAnnotations) {
        if (firAnnotations.isEmpty()) {
            return null;
        }

        List<J.Annotation> annotations = new ArrayList<>(firAnnotations.size());
        for (FirAnnotation annotation : firAnnotations) {
            J.Annotation a = (J.Annotation) visitElement(annotation, null);
            annotations.add(a);
        }
        return annotations;
    }

    private J.Annotation mapAnnotation(List<FirAnnotation> firAnnotations) {
        if (firAnnotations.isEmpty()) {
            throw new UnsupportedOperationException("Unexpected empty list of FIR Annotations.");
        }

        FirAnnotation firAnnotation = firAnnotations.get(0);
        J.Annotation annotation = (J.Annotation) visitElement(firAnnotation, null);

        firAnnotations.remove(firAnnotation);
        return annotation;
    }

    private K.Modifier mapModifier(Space prefix, List<J.Annotation> annotations) {
        K.Modifier.Type type;
        // Ordered based on kotlin requirements.
        if (source.startsWith("public", cursor)) {
            type = K.Modifier.Type.Public;
        } else if (source.startsWith("protected", cursor)) {
            type = K.Modifier.Type.Protected;
        } else if (source.startsWith("private", cursor)) {
            type = K.Modifier.Type.Private;
        } else if (source.startsWith("internal", cursor)) {
            type = K.Modifier.Type.Internal;
        } else if (source.startsWith("expect", cursor)) {
            type = K.Modifier.Type.Expect;
        } else if (source.startsWith("actual", cursor)) {
            type = K.Modifier.Type.Actual;
        } else if (source.startsWith("final", cursor)) {
            type = K.Modifier.Type.Final;
        } else if (source.startsWith("open", cursor)) {
            type = K.Modifier.Type.Open;
        } else if (source.startsWith("abstract", cursor)) {
            type = K.Modifier.Type.Abstract;
        } else if (source.startsWith("sealed", cursor)) {
            type = K.Modifier.Type.Sealed;
        } else if (source.startsWith("const", cursor)) {
            type = K.Modifier.Type.Const;
        } else if (source.startsWith("external", cursor)) {
            type = K.Modifier.Type.External;
        } else if (source.startsWith("override", cursor)) {
            type = K.Modifier.Type.Override;
        } else if (source.startsWith("lateinit", cursor)) {
            type = K.Modifier.Type.LateInit;
        } else if (source.startsWith("tailrec", cursor)) {
            type = K.Modifier.Type.TailRec;
        } else if (source.startsWith("vararg", cursor)) {
            type = K.Modifier.Type.Vararg;
        } else if (source.startsWith("suspend", cursor)) {
            type = K.Modifier.Type.Suspend;
        } else if (source.startsWith("inner", cursor)) {
            type = K.Modifier.Type.Inner;
        } else if (source.startsWith("enum", cursor)) {
            type = K.Modifier.Type.Enum;
        } else if (source.startsWith("annotation", cursor)) {
            type = K.Modifier.Type.Annotation;
        } else if (source.startsWith("fun", cursor)) {
            type = K.Modifier.Type.Fun;
        } else if (source.startsWith("companion", cursor)) {
            type = K.Modifier.Type.Companion;
        } else if (source.startsWith("inline", cursor)) {
            type = K.Modifier.Type.Inline;
        } else if (source.startsWith("value", cursor)) {
            type = K.Modifier.Type.Value;
        } else if (source.startsWith("infix", cursor)) {
            type = K.Modifier.Type.Infix;
        } else if (source.startsWith("operator", cursor)) {
            type = K.Modifier.Type.Operator;
        } else if (source.startsWith("data", cursor)) {
            type = K.Modifier.Type.Data;
        } else {
            throw new IllegalArgumentException("Source starts with " + source.substring(cursor, cursor + 30) + " at cursor pos " + cursor);
        }

        skip(type.name().toLowerCase());
        return new K.Modifier(randomId(), prefix, Markers.EMPTY, type, annotations);
    }

    @Nullable
    private FirBasedSymbol<?> getCurrentFile() {
        return currentFile == null ? null : currentFile.getSymbol();
    }

    private int positionOfNext(String untilDelim) {
        return positionOfNext(untilDelim, null);
    }

    private int positionOfNext(String untilDelim, @Nullable Character stop) {
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
                    char c1 = source.charAt(delimIndex);
                    char c2 = source.charAt(delimIndex + 1);
                    switch (c1) {
                        case '/':
                            switch (c2) {
                                case '/':
                                    inSingleLineComment = true;
                                    delimIndex++;
                                    break;
                                case '*':
                                    inMultiLineComment = true;
                                    delimIndex++;
                                    break;
                            }
                            break;
                        case '*':
                            if (c2 == '/') {
                                inMultiLineComment = false;
                                delimIndex += 2;
                            }
                            break;
                    }
                }

                if (!inMultiLineComment && !inSingleLineComment) {
                    if (stop != null && source.charAt(delimIndex) == stop)
                        return -1; // reached stop word before finding the delimiter

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

    /**
     * @return Source from <code>cursor</code> to next occurrence of <code>untilDelim</code>,
     * and if not found in the remaining source, the empty String. If <code>stop</code> is reached before
     * <code>untilDelim</code> return the empty String.
     */
    @SuppressWarnings("SameParameterValue")
    private Space sourceBefore(String untilDelim, @Nullable Character stop) {
        int delimIndex = positionOfNext(untilDelim, stop);
        if (delimIndex < 0) {
            return EMPTY; // unable to find this delimiter
        }

        if (delimIndex == cursor) {
            cursor += untilDelim.length();
            return EMPTY;
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
