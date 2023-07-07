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

import org.jetbrains.kotlin.*;
import org.jetbrains.kotlin.com.intellij.lang.ASTNode;
import org.jetbrains.kotlin.com.intellij.psi.PsiElement;
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement;
import org.jetbrains.kotlin.com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.kotlin.descriptors.ClassKind;
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget;
import org.jetbrains.kotlin.fir.*;
import org.jetbrains.kotlin.fir.contracts.*;
import org.jetbrains.kotlin.fir.declarations.*;
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyGetter;
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertySetter;
import org.jetbrains.kotlin.fir.declarations.impl.FirPrimaryConstructor;
import org.jetbrains.kotlin.fir.expressions.*;
import org.jetbrains.kotlin.fir.expressions.impl.*;
import org.jetbrains.kotlin.fir.references.*;
import org.jetbrains.kotlin.fir.resolve.LookupTagUtilsKt;
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag;
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol;
import org.jetbrains.kotlin.fir.symbols.impl.*;
import org.jetbrains.kotlin.fir.types.*;
import org.jetbrains.kotlin.fir.types.impl.FirImplicitNullableAnyTypeRef;
import org.jetbrains.kotlin.fir.types.impl.FirImplicitUnitTypeRef;
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitor;
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.psi.psiUtil.PsiUtilsKt;
import org.openrewrite.ExecutionContext;
import org.openrewrite.FileAttributes;
import org.openrewrite.ParseExceptionResult;
import org.openrewrite.internal.EncodingDetectingInputStream;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.internal.JavaTypeCache;
import org.openrewrite.java.marker.ImplicitReturn;
import org.openrewrite.java.marker.OmitParentheses;
import org.openrewrite.java.tree.*;
import org.openrewrite.kotlin.KotlinParser;
import org.openrewrite.kotlin.KotlinTypeMapping;
import org.openrewrite.kotlin.marker.*;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.marker.Markers;
import org.openrewrite.style.NamedStyles;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import static java.lang.Math.max;
import static java.util.Collections.*;
import static java.util.stream.Collectors.toList;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.internal.StringUtils.indexOfNextNonWhitespace;
import static org.openrewrite.java.tree.Space.EMPTY;
import static org.openrewrite.java.tree.Space.format;

@SuppressWarnings("DataFlowIssue")
public class KotlinParserVisitor extends FirDefaultVisitor<J, ExecutionContext> {
    private final Path sourcePath;

    @Nullable
    private final FileAttributes fileAttributes;
    private final String source;
    private final Charset charset;
    private final boolean charsetBomMarked;
    private final List<NamedStyles> styles;
    private final KotlinTypeMapping typeMapping;
    private final ExecutionContext ctx;
    private final FirSession firSession;
    private int cursor;

    private final Map<Integer, ASTNode> nodes;

    // Associate top-level function and property declarations to the file.
    @Nullable
    private FirFile currentFile;

    public KotlinParserVisitor(KotlinSource kotlinSource, @Nullable Path relativeTo, List<NamedStyles> styles, JavaTypeCache typeCache, FirSession firSession, ExecutionContext ctx) {
        this.sourcePath = kotlinSource.getInput().getRelativePath(relativeTo);
        this.fileAttributes = kotlinSource.getInput().getFileAttributes();
        EncodingDetectingInputStream is = kotlinSource.getInput().getSource(ctx);
        this.source = is.readFully();
        this.charset = is.getCharset();
        this.charsetBomMarked = is.isCharsetBomMarked();
        this.styles = styles;
        this.typeMapping = new KotlinTypeMapping(typeCache, firSession);
        this.ctx = ctx;
        this.firSession = firSession;
        this.nodes = kotlinSource.getNodes();
    }

    @Override
    public J visitFile(FirFile file, ExecutionContext ctx) {
        currentFile = file;

//        List<J.Annotation> annotations = null;
//        if (getCurrentPsiNode() instanceof KtFile) {
//            KtFileAnnotationList annotationList = PsiTreeUtil.findChildOfType(getCurrentPsiNode(), KtFileAnnotationList.class);
//            annotations = mapFileAnnotations(annotationList);
//        }
        List<J.Annotation> annotations = mapAnnotations(file.getAnnotations());

        JRightPadded<J.Package> paddedPkg = null;
        if (!file.getPackageDirective().getPackageFqName().isRoot()) {
            J.Package pkg;
            try {
                pkg = (J.Package) visitPackageDirective(file.getPackageDirective(), ctx);
            } catch (Exception e) {
                throw new KotlinParsingException("Failed to parse package directive", e);
            }
            paddedPkg = maybeSemicolon(pkg);
        }

        List<JRightPadded<J.Import>> imports = new ArrayList<>(file.getImports().size());
        for (FirImport anImport : file.getImports()) {
            J.Import importStatement;
            try {
                importStatement = (J.Import) visitImport(anImport, ctx);
            } catch (Exception e) {
                throw new KotlinParsingException("Failed to parse import", e);
            }
            imports.add(maybeSemicolon(importStatement));
        }

        List<JRightPadded<Statement>> statements = new ArrayList<>(file.getDeclarations().size());
        for (FirDeclaration declaration : file.getDeclarations()) {
            Statement statement;
            int savedCursor = cursor;
            try {
                statement = (Statement) visitElement(declaration, ctx);
            } catch (Exception e) {
                if (declaration.getSource() == null) {
                    throw new KotlinParsingException("Failed to parse declaration", e);
                }
                cursor = savedCursor;
                Space prefix = whitespace();
                String text = declaration.getSource().getLighterASTNode().toString();
                skip(text);
                statement = new J.Unknown(
                        randomId(),
                        prefix,
                        Markers.EMPTY,
                        new J.Unknown.Source(
                                randomId(),
                                Space.EMPTY,
                                Markers.build(singletonList(ParseExceptionResult.build(KotlinParser.class, e)
                                        .withTreeType(declaration.getSource().getKind().toString()))),
                                text
                        )
                );
            }
            statements.add(maybeSemicolon(statement));
        }

        return new K.CompilationUnit(
                randomId(),
                Space.EMPTY,
                Markers.build(styles),
                sourcePath,
                fileAttributes,
                charset.name(),
                charsetBomMarked,
                null,
                annotations,
                paddedPkg,
                imports,
                statements,
                format(source.substring(cursor)));
    }

    @Override
    public J visitErrorNamedReference(FirErrorNamedReference errorNamedReference, ExecutionContext ctx) {
        if (errorNamedReference.getSource() instanceof KtRealPsiSourceElement && ((KtRealPsiSourceElement) errorNamedReference.getSource()).getPsi() instanceof KtNameReferenceExpression) {
            KtNameReferenceExpression nameReferenceExpression = (KtNameReferenceExpression) ((KtRealPsiSourceElement) errorNamedReference.getSource()).getPsi();
            String name = nameReferenceExpression.getIdentifier() == null ? "{error}" : nameReferenceExpression.getIdentifier().getText();
            Space prefix = sourceBefore(name);
            return new J.Identifier(
                    randomId(),
                    prefix,
                    Markers.EMPTY,
                    name,
                    null,
                    null);
        } else if (errorNamedReference.getSource() instanceof KtLightSourceElement) {
            String fullName = errorNamedReference.getSource().getLighterASTNode().toString();
            Space prefix = whitespace();
            skip(fullName);
            return TypeTree.build(fullName).withPrefix(prefix);
        } else {
            throw new UnsupportedOperationException("Unsupported error name reference type.");
        }
    }

    @Override
    public J visitAnnotationCall(FirAnnotationCall annotationCall, ExecutionContext ctx) {
        Space prefix = whitespace();
        Markers markers = Markers.EMPTY;
        skip("@");
        if (annotationCall.getUseSiteTarget() == AnnotationUseSiteTarget.FILE) {
            skip("file");
            markers = markers.addIfAbsent(new AnnotationCallSite(randomId(), "file", sourceBefore(":")));
        }

        if (annotationCall.getUseSiteTarget() == AnnotationUseSiteTarget.PROPERTY_GETTER) {
            skip("get");
            markers = markers.addIfAbsent(new AnnotationCallSite(randomId(), "get", sourceBefore(":")));
        }

        J.Identifier name = (J.Identifier) visitElement(annotationCall.getCalleeReference(), ctx);
        JContainer<Expression> args = null;
        if (!annotationCall.getArgumentList().getArguments().isEmpty()) {
            Space argsPrefix = sourceBefore("(");
            List<JRightPadded<Expression>> expressions;
            if (annotationCall.getArgumentList().getArguments().size() == 1) {
                if (annotationCall.getArgumentList().getArguments().get(0) instanceof FirVarargArgumentsExpression) {
                    FirVarargArgumentsExpression varargArgumentsExpression = (FirVarargArgumentsExpression) annotationCall.getArgumentList().getArguments().get(0);
                    expressions = convertAll(varargArgumentsExpression.getArguments(), commaDelim, t -> sourceBefore(")"), ctx, true);
                } else {
                    FirExpression arg = annotationCall.getArgumentList().getArguments().get(0);
                    expressions = singletonList(convert(arg, t -> sourceBefore(")"), ctx));
                }
            } else {
                expressions = convertAll(annotationCall.getArgumentList().getArguments(), commaDelim, t -> sourceBefore(")"), ctx, true);
            }

            args = JContainer.build(argsPrefix, expressions, Markers.EMPTY);
        }

        return new J.Annotation(
                randomId(),
                prefix,
                markers,
                name,
                args);
    }

    @Override
    public J visitAnonymousFunction(FirAnonymousFunction anonymousFunction, ExecutionContext ctx) {
        Markers markers = Markers.EMPTY;

        J.Label label = null;
        if (anonymousFunction.getLabel() != null && anonymousFunction.getLabel().getSource() != null &&
                anonymousFunction.getLabel().getSource().getKind() != KtFakeSourceElementKind.GeneratedLambdaLabel.INSTANCE) {
            label = (J.Label) visitElement(anonymousFunction.getLabel(), ctx);
        }

        Space prefix = whitespace();
        boolean omitBraces = !source.startsWith("{", cursor);
        if (omitBraces) {
            markers = markers.addIfAbsent(new OmitBraces(randomId()));
        } else {
            skip("{");
        }

        JavaType closureType = null;

        boolean omitDestruct = false;
        List<JRightPadded<J>> paramExprs = new ArrayList<>(anonymousFunction.getValueParameters().size());
        if (!anonymousFunction.getValueParameters().isEmpty()) {
            List<FirValueParameter> parameters = anonymousFunction.getValueParameters();
            for (int i = 0; i < parameters.size(); i++) {
                FirValueParameter p = parameters.get(i);
                if (p.getSource() != null && p.getSource().getKind() instanceof KtFakeSourceElementKind.ItLambdaParameter) {
                    continue;
                }

                if ("<destruct>".equals(p.getName().asString())) {
                    omitDestruct = true;
                    Space destructPrefix = sourceBefore("(");
                    int saveCursor = cursor;
                    String params = sourceBefore(")").getWhitespace();
                    String[] paramNames = params.split(",");
                    List<JRightPadded<J>> destructParams = new ArrayList<>(paramNames.length);
                    cursor(saveCursor);

                    ConeTypeProjection[] typeArguments = null;
                    if (p.getReturnTypeRef() instanceof FirResolvedTypeRef && (p.getReturnTypeRef().getSource() == null || !(p.getReturnTypeRef().getSource().getKind() instanceof KtFakeSourceElementKind))) {
                        FirResolvedTypeRef typeRef = (FirResolvedTypeRef) p.getReturnTypeRef();
                        typeArguments = typeRef.getType().getTypeArguments();
                    }

                    for (int j = 0; j < paramNames.length; j++) {
                        String paramName = paramNames[j].trim();
                        JavaType type = typeArguments == null || j >= typeArguments.length ? null : typeMapping.type(typeArguments[j]);
                        J.Identifier param = createIdentifier(paramName, type, null);
                        JRightPadded<J> paramExpr = JRightPadded.build(param);
                        Space after = j < paramNames.length - 1 ? sourceBefore(",") : sourceBefore(")");
                        paramExpr = paramExpr.withAfter(after);
                        destructParams.add(paramExpr);
                    }

                    // Create a new J.Lambda.Parameters instance to represent the destructured parameters.
                    // { (a, b), c -> ... } // a destructured pair and another parameter
                    // { (a, b), (c, d) -> ... } // a destructured pair and another destructured pair
                    J.Lambda.Parameters destructParamsExpr = new J.Lambda.Parameters(randomId(), destructPrefix, Markers.EMPTY, true, destructParams);
                    paramExprs.add(JRightPadded.build(destructParamsExpr));
                } else {
                    J expr = visitElement(p, ctx);

                    JRightPadded<J> param = JRightPadded.build(expr);
                    if (i != parameters.size() - 1) {
                        param = param.withAfter(sourceBefore(","));
                    }
                    paramExprs.add(param);
                }
            }
        }

        J.Lambda.Parameters params = new J.Lambda.Parameters(randomId(), EMPTY, Markers.EMPTY, false, paramExprs);
        int saveCursor = cursor;
        Space arrowPrefix = whitespace();
        if (source.startsWith("->", cursor)) {
            skip("->");
            if (params.getParameters().isEmpty()) {
                params = params.getPadding().withParams(singletonList(JRightPadded
                        .build((J) new J.Empty(randomId(), EMPTY, Markers.EMPTY))
                        .withAfter(arrowPrefix)));
            } else {
                params = params.getPadding().withParams(
                        ListUtils.mapLast(params.getPadding().getParams(), param -> param.withAfter(arrowPrefix)));
            }
        } else {
            cursor(saveCursor);
        }

        Set<FirElement> skip = Collections.newSetFromMap(new IdentityHashMap<>());
        if (omitDestruct && anonymousFunction.getBody() != null) {
            // skip destructured property declarations.
            for (FirStatement statement : anonymousFunction.getBody().getStatements()) {
                if (statement instanceof FirProperty && ((FirProperty) statement).getInitializer() instanceof FirComponentCall &&
                        ((FirComponentCall) ((FirProperty) statement).getInitializer()).getDispatchReceiver() instanceof FirPropertyAccessExpression &&
                        ((FirPropertyAccessExpression) ((FirComponentCall) ((FirProperty) statement).getInitializer()).getDispatchReceiver()).getCalleeReference() instanceof FirResolvedNamedReference &&
                        "<destruct>".equals(((FirResolvedNamedReference) ((FirPropertyAccessExpression) ((FirComponentCall) ((FirProperty) statement).getInitializer()).getDispatchReceiver()).getCalleeReference()).getName().asString())) {
                    skip.add(statement);
                }
            }
        }

        J body = anonymousFunction.getBody() == null ? null : visitBlock(anonymousFunction.getBody(), skip, ctx);
        if (body instanceof J.Block && !omitBraces) {
            body = ((J.Block) body).withEnd(sourceBefore("}"));
        }

        if (body != null && anonymousFunction.getValueParameters().isEmpty()) {
            body = body.withMarkers(body.getMarkers().removeByType(OmitBraces.class));
        }

        if (body == null) {
            body = new J.Block(
                    randomId(),
                    EMPTY,
                    Markers.EMPTY,
                    new JRightPadded<>(false, EMPTY, Markers.EMPTY),
                    emptyList(),
                    EMPTY);
        }

        J.Lambda lambda = new J.Lambda(
                randomId(),
                prefix,
                markers,
                params,
                EMPTY,
                body,
                closureType);

        return label != null ? label.withStatement(lambda) : lambda;
    }

    @Override
    public J visitAnonymousFunctionExpression(FirAnonymousFunctionExpression anonymousFunctionExpression, ExecutionContext ctx) {
        if (!anonymousFunctionExpression.getAnonymousFunction().isLambda()) {
            throw new UnsupportedOperationException("Unsupported anonymous function expression.");
        }

        return visitAnonymousFunction(anonymousFunctionExpression.getAnonymousFunction(), ctx);
    }

    @Override
    public J visitAnonymousInitializer(FirAnonymousInitializer anonymousInitializer, ExecutionContext ctx) {
        Space prefix = sourceBefore("init");
        J.Block staticInit = (J.Block) visitElement(anonymousInitializer.getBody(), ctx);
        staticInit = staticInit.getPadding().withStatic(staticInit.getPadding().getStatic().withAfter(staticInit.getPrefix()));
        staticInit = staticInit.withPrefix(prefix);
        return staticInit.withStatic(true);
    }

    @Override
    public J visitAnonymousObject(FirAnonymousObject anonymousObject, ExecutionContext ctx) {
        Space objectPrefix = sourceBefore("object");
        Markers markers = Markers.EMPTY.addIfAbsent(new KObject(randomId(), objectPrefix));
        Space typeExpressionPrefix = sourceBefore(":");
        Space prefix = whitespace();

        TypeTree clazz = (TypeTree) visitElement(anonymousObject.getSuperTypeRefs().get(0), ctx);
        JContainer<Expression> args;

        int saveCursor = cursor;
        Space before = whitespace();
        if (source.startsWith("(", cursor)) {
            if (!anonymousObject.getDeclarations().isEmpty() &&
                    anonymousObject.getDeclarations().get(0) instanceof FirPrimaryConstructor &&
                    !((FirPrimaryConstructor) anonymousObject.getDeclarations().get(0)).getDelegatedConstructor().getArgumentList().getArguments().isEmpty()) {
                cursor(saveCursor);
                args = mapFunctionalCallArguments(((FirPrimaryConstructor) anonymousObject.getDeclarations().get(0)).getDelegatedConstructor().getArgumentList().getArguments());
            } else {
                skip("(");
                args = JContainer.build(before,
                        singletonList(padRight(new J.Empty(randomId(), sourceBefore(")"), Markers.EMPTY), EMPTY)), Markers.EMPTY);
            }
        } else {
            cursor(saveCursor);
            args = JContainer.<Expression>empty()
                    .withMarkers(Markers.build(singletonList(new OmitParentheses(randomId()))));
        }

        saveCursor = cursor;
        J.Block body = null;
        Space bodyPrefix = whitespace();

        if (source.startsWith("{", cursor)) {
            skip("{");
            List<FirElement> declarations = new ArrayList<>(anonymousObject.getDeclarations().size());
            for (FirDeclaration declaration : anonymousObject.getDeclarations()) {
                if (declaration.getSource() != null && declaration.getSource().getKind() instanceof KtFakeSourceElementKind) {
                    continue;
                }
                declarations.add(declaration);
            }

            List<JRightPadded<Statement>> statements = new ArrayList<>(declarations.size());
            for (FirElement element : declarations) {
                statements.add(JRightPadded.build((Statement) visitElement(element, ctx)));
            }

            body = new J.Block(
                    randomId(),
                    bodyPrefix,
                    Markers.EMPTY,
                    new JRightPadded<>(false, EMPTY, Markers.EMPTY),
                    statements,
                    sourceBefore("}"));
        } else {
            cursor(saveCursor);
        }

        return new J.NewClass(
                randomId(),
                prefix,
                markers,
                null,
                typeExpressionPrefix,
                clazz,
                args,
                body,
                null);
    }

    @Override
    public J visitAnonymousObjectExpression(FirAnonymousObjectExpression anonymousObjectExpression, ExecutionContext ctx) {
        // Pass through to the anonymous object since the `<anonymous>` typeRef on the expression is not necessary.
        return visitElement(anonymousObjectExpression.getAnonymousObject(), ctx);
    }

    @Override
    public J visitCallableReferenceAccess(FirCallableReferenceAccess callableReferenceAccess, ExecutionContext ctx) {
        Space prefix = whitespace();

        FirResolvedCallableReference reference = (FirResolvedCallableReference) callableReferenceAccess.getCalleeReference();
        JavaType.Method methodReferenceType = null;
        if (reference.getResolvedSymbol() instanceof FirNamedFunctionSymbol) {
            methodReferenceType = typeMapping.methodDeclarationType(((FirNamedFunctionSymbol) reference.getResolvedSymbol()).getFir(),
                    TypeUtils.asFullyQualified(typeMapping.type(callableReferenceAccess.getExplicitReceiver())), getCurrentFile());
        }
        JavaType.Variable fieldReferenceType = null;
        if (reference.getResolvedSymbol() instanceof FirPropertySymbol) {
            fieldReferenceType = typeMapping.variableType((FirVariableSymbol<? extends FirVariable>) reference.getResolvedSymbol(),
                    TypeUtils.asFullyQualified(typeMapping.type(callableReferenceAccess.getExplicitReceiver())), getCurrentFile());
        }

        return new J.MemberReference(
                randomId(),
                prefix,
                Markers.EMPTY,
                padRight(callableReferenceAccess.getExplicitReceiver() == null ?
                        new J.Empty(randomId(), EMPTY, Markers.EMPTY) :
                        (Expression) visitElement(callableReferenceAccess.getExplicitReceiver(), ctx), sourceBefore("::")),
                callableReferenceAccess.getTypeArguments().isEmpty() ? null : mapTypeArguments(callableReferenceAccess.getTypeArguments()),
                padLeft(whitespace(), (J.Identifier) visitElement(callableReferenceAccess.getCalleeReference(), ctx)),
                typeMapping.type(callableReferenceAccess.getCalleeReference()),
                methodReferenceType,
                fieldReferenceType);
    }

    @Override
    public J visitArrayOfCall(FirArrayOfCall arrayOfCall, ExecutionContext ctx) {
        return new K.ListLiteral(
                randomId(),
                sourceBefore("["),
                Markers.EMPTY,
                arrayOfCall.getArgumentList().getArguments().isEmpty() ?
                        JContainer.build(singletonList(new JRightPadded<>(new J.Empty(randomId(), EMPTY, Markers.EMPTY), sourceBefore("]"), Markers.EMPTY))) :
                        JContainer.build(EMPTY, convertAll(arrayOfCall.getArgumentList().getArguments(), commaDelim, t -> sourceBefore("]"), ctx, true), Markers.EMPTY),
                typeMapping.type(arrayOfCall));
    }

    @Override
    public J visitBackingFieldReference(FirBackingFieldReference backingFieldReference, ExecutionContext ctx) {
        String name = backingFieldReference.getName().asString().startsWith("$") ? backingFieldReference.getName().asString().substring(1) : backingFieldReference.getName().asString();
        return createIdentifier(name, backingFieldReference);
    }

    @Override
    public J visitBinaryLogicExpression(FirBinaryLogicExpression binaryLogicExpression, ExecutionContext ctx) {
        Space prefix = whitespace();

        Space beforeParens = EMPTY;
        boolean includeParentheses = false;
        if (source.startsWith("(", cursor)) {
            skip("(");
            beforeParens = prefix;
            prefix = whitespace();
            includeParentheses = true;
        }

        Expression left = (Expression) visitElement(binaryLogicExpression.getLeftOperand(), ctx);

        Markers markers = Markers.EMPTY;
        Space opPrefix = whitespace();
        J.Binary.Type op;
        if (LogicOperationKind.AND == binaryLogicExpression.getKind()) {
            skip("&&");
            op = J.Binary.Type.And;
        } else if (LogicOperationKind.OR == binaryLogicExpression.getKind()) {
            if (source.startsWith(",", cursor)) {
                skip(",");
                markers = Markers.build(singletonList(new LogicalComma(randomId())));
            } else {
                skip("||");
            }
            op = J.Binary.Type.Or;
        } else {
            throw new UnsupportedOperationException("Unsupported binary expression type " + binaryLogicExpression.getKind().name());
        }

        Expression right = (Expression) visitElement(binaryLogicExpression.getRightOperand(), ctx);

        J.Binary binary = new J.Binary(
                randomId(),
                prefix,
                markers,
                left,
                padLeft(opPrefix, op),
                right,
                typeMapping.type(binaryLogicExpression));

        return includeParentheses ? new J.Parentheses<>(randomId(), beforeParens, Markers.EMPTY, JRightPadded.build(binary).withAfter(sourceBefore(")"))) : binary;
    }

    @Override
    public J visitBlock(FirBlock block, ExecutionContext ctx) {
        return visitBlock(block, emptySet(), ctx);
    }

    /**
     * Map a FirBlock to a J.Block.
     *
     * @param block          target FirBlock.
     * @param skipStatements must use a {@link Set} constructed by {@link IdentityHashMap}. Kotlin uses FirBlocks to
     *                       represented certain AST elements. When an AST element is represented as a block, we need
     *                       to filter out statements that should not be processed.
     *                       <p>
     *                       I.E., a for loop in code will be a FirWhileLoop in the AST, and the body of the FirWhileLoop
     *                       (FirBlock) will contain statements that do not exist in code.
     *                       The additional statements contain AST information required to construct the J.ForLoop,
     *                       but should not be added as statements to the J.ForLoop#body.
     */
    private J visitBlock(FirBlock block, Set<FirElement> skipStatements, ExecutionContext ctx) {
        int saveCursor = cursor;
        Space prefix = whitespace();
        OmitBraces omitBraces = null;
        boolean isEmptyBody = !source.startsWith("{", cursor);
        if (isEmptyBody) {
            cursor(saveCursor);
            prefix = EMPTY;
            omitBraces = new OmitBraces(randomId());
        } else {
            skip("{");
        }

        List<FirStatement> firStatements = new ArrayList<>(block.getStatements().size());
        for (FirStatement s : block.getStatements()) {
            // Skip FirElements that should not be processed.
            if (!skipStatements.contains(s) &&
                    (s.getSource() == null || !(s.getSource().getKind() instanceof KtFakeSourceElementKind.ImplicitConstructor))) {
                firStatements.add(s);
            }
        }

        List<JRightPadded<Statement>> statements = new ArrayList<>(firStatements.size());
        for (int i = 0; i < firStatements.size(); i++) {
            FirElement firElement = firStatements.get(i);
            // Skip receiver of the unary post increment or decrement.
            if (firElement.getSource() != null && firElement.getSource().getKind() instanceof KtFakeSourceElementKind.DesugaredIncrementOrDecrement &&
                    !(firElement instanceof FirVariableAssignment)) {
                continue;
            }

            J expr = null;
            if (firElement instanceof FirBlock && ((FirBlock) firElement).getStatements().size() == 2) {
                // For loops are wrapped in a block and split into two FirElements.
                // The FirProperty at position 0 is the control of the for loop.
                // The FirWhileLoop at position 1 is the for loop, which is transformed to use an iterator.
                // So, the FirBlock is transformed to construct a J.ForEach that preserves source code.
                FirBlock check = (FirBlock) firElement;
                if (check.getStatements().get(0) instanceof FirProperty &&
                        "<iterator>".equals(((FirProperty) check.getStatements().get(0)).getName().asString()) &&
                        check.getStatements().get(1) instanceof FirWhileLoop) {
                    expr = mapForLoop(check);
                }
            }

            int skipImplicitDestructs = 0;
            if (firElement instanceof FirProperty && "<destruct>".equals(((FirProperty) firElement).getName().asString())) {
                // Destructs are represented as FirProperty with the name "<destruct>".
                // The destruct property does not the values declared in the destruct `(a, b, ..., c)`.
                // However, the property initialization IS a part of the FirProperty.
                // The Kotlin compiler adds the values `a, b, ..., c` in the FirBlock after the destruct property.
                // So, we need to skip the destruct values when mapping the FirProperty.
                saveCursor = cursor;
                sourceBefore("(");
                skipImplicitDestructs = sourceBefore(")").getWhitespace().split(",").length;
                cursor(saveCursor);
            }

            if (expr == null) {
                expr = visitElement(firElement, ctx);
                if (!(expr instanceof Statement)) {
                    if (expr instanceof Expression) {
                        expr = new K.ExpressionStatement(randomId(), (Expression) expr);
                    } else {
                        throw new IllegalStateException("Unexpected expression type " + expr.getClass().getSimpleName());
                    }
                }
                i += skipImplicitDestructs;
            }

            JRightPadded<Statement> stat = JRightPadded.build((Statement) expr);
            saveCursor = cursor;
            Space beforeSemicolon = whitespace();
            if (cursor < source.length() && source.charAt(cursor) == ';') {
                stat = stat
                        .withMarkers(stat.getMarkers().add(new Semicolon(randomId())))
                        .withAfter(beforeSemicolon);
                skip(";");
            } else {
                cursor(saveCursor);
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

        J.Identifier label = null;
        if (breakExpression.getTarget().getLabelName() != null) {
            skip("@");
            label = createIdentifier(breakExpression.getTarget().getLabelName());
        }

        return new J.Break(
                randomId(),
                prefix,
                Markers.EMPTY,
                label);
    }

    @Override
    public J visitCatch(FirCatch firCatch, ExecutionContext ctx) {
        Space prefix = whitespace();
        skip("catch");

        Space paramPrefix = sourceBefore("(");
        J.VariableDeclarations paramDecl = (J.VariableDeclarations) visitElement(firCatch.getParameter(), ctx);

        J.ControlParentheses<J.VariableDeclarations> param = new J.ControlParentheses<>(
                randomId(),
                paramPrefix,
                Markers.EMPTY,
                padRight(paramDecl, sourceBefore(")")));

        return new J.Try.Catch(
                randomId(),
                prefix,
                Markers.EMPTY,
                param,
                (J.Block) visitElement(firCatch.getBlock(), ctx));
    }

    @Override
    public J visitCheckNotNullCall(FirCheckNotNullCall checkNotNullCall, ExecutionContext ctx) {
        J j = visitElement(checkNotNullCall.getArgumentList().getArguments().get(0), ctx);
        return j.withMarkers(j.getMarkers().addIfAbsent(new CheckNotNull(randomId(), sourceBefore("!!"))));
    }

    @Override
    public J visitComparisonExpression(FirComparisonExpression comparisonExpression, ExecutionContext ctx) {
        Space prefix = whitespace();

        FirFunctionCall functionCall = comparisonExpression.getCompareToCall();

        FirElement receiver = functionCall.getExplicitReceiver() != null ? functionCall.getExplicitReceiver() : functionCall.getDispatchReceiver();
        J j = visitElement(receiver, ctx);
        Expression left = !(j instanceof Expression) && j instanceof Statement ? new K.StatementExpression(randomId(), (Statement) j) : (Expression) j;

        Space opPrefix = whitespace();
        J.Binary.Type op = mapOperation(comparisonExpression.getOperation());

        if (functionCall.getArgumentList().getArguments().size() != 1) {
            throw new UnsupportedOperationException("Unsupported FirComparisonExpression argument size");
        }

        j = visitElement(functionCall.getArgumentList().getArguments().get(0), ctx);
        Expression right = !(j instanceof Expression) && j instanceof Statement ? new K.StatementExpression(randomId(), (Statement) j) : (Expression) j;
        return new J.Binary(
                randomId(),
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
            throw new IllegalArgumentException("Unresolved primitive type.");
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
        FirElement right = equalityOperatorCall.getArgumentList().getArguments().get(1);
        if (left instanceof FirWhenSubjectExpression || right instanceof FirWhenSubjectExpression) {
            return (left instanceof FirWhenSubjectExpression) ? (Expression) visitElement(right, ctx) : (Expression) visitElement(left, ctx);
        }

        Space prefix = whitespace();
        J j = visitElement(left, ctx);
        Expression leftExpr = !(j instanceof Expression) && j instanceof Statement ? new K.StatementExpression(randomId(), (Statement) j) : (Expression) j;
        FirOperation op = equalityOperatorCall.getOperation();
        Space opPrefix = sourceBefore(op.getOperator());

        j = visitElement(right, ctx);
        Expression rightExpr = !(j instanceof Expression) && j instanceof Statement ? new K.StatementExpression(randomId(), (Statement) j) : (Expression) j;

        if (op == FirOperation.IDENTITY || op == FirOperation.NOT_IDENTITY) {
            return new K.Binary(
                    randomId(),
                    prefix,
                    Markers.EMPTY,
                    leftExpr,
                    padLeft(opPrefix, op == FirOperation.IDENTITY ? K.Binary.Type.IdentityEquals : K.Binary.Type.IdentityNotEquals),
                    rightExpr,
                    EMPTY,
                    typeMapping.type(equalityOperatorCall)
            );
        } else {
            return new J.Binary(
                    randomId(),
                    prefix,
                    Markers.EMPTY,
                    leftExpr,
                    padLeft(opPrefix, mapOperation(op)),
                    rightExpr,
                    typeMapping.type(equalityOperatorCall));
        }

    }

    @Override
    public J visitContinueExpression(FirContinueExpression continueExpression, ExecutionContext ctx) {
        Space prefix = sourceBefore("continue");

        J.Identifier label = null;
        if (continueExpression.getTarget().getLabelName() != null) {
            skip("@");
            label = createIdentifier(continueExpression.getTarget().getLabelName());
        }

        return new J.Continue(
                randomId(),
                prefix,
                Markers.EMPTY,
                label);
    }

    @Override
    public J visitDoWhileLoop(FirDoWhileLoop doWhileLoop, ExecutionContext ctx) {
        J.Label label = null;
        if (doWhileLoop.getLabel() != null) {
            label = (J.Label) visitElement(doWhileLoop.getLabel(), ctx);
        }

        Space prefix = whitespace();
        skip("do");
        J.DoWhileLoop statement = new J.DoWhileLoop(
                randomId(),
                prefix,
                Markers.EMPTY,
                JRightPadded.build((Statement) visitElement(doWhileLoop.getBlock(), ctx)),
                padLeft(sourceBefore("while"), mapControlParentheses(doWhileLoop.getCondition())));

        return label != null ? label.withStatement(statement) : statement;
    }

    @Override
    public J visitElvisExpression(FirElvisExpression elvisExpression, ExecutionContext ctx) {
        Space prefix = whitespace();
        J lhs = visitElement(elvisExpression.getLhs(), ctx);
        if (lhs instanceof Statement && !(lhs instanceof Expression)) {
            lhs = new K.StatementExpression(randomId(), (Statement) lhs);
        }
        Space before = sourceBefore("?:");
        J rhs = visitElement(elvisExpression.getRhs(), ctx);
        if (rhs instanceof Statement && !(rhs instanceof Expression)) {
            rhs = new K.StatementExpression(randomId(), (Statement) rhs);
        }
        return new J.Ternary(
                randomId(),
                prefix,
                Markers.EMPTY,
                new J.Empty(randomId(), EMPTY, Markers.EMPTY),
                padLeft(EMPTY, (Expression) lhs),
                padLeft(before, (Expression) rhs),
                typeMapping.type(elvisExpression));
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
                createIdentifier(enumEntry.getName().asString(), enumEntry),
                null);
    }

    @Override
    public J visitSuperReference(FirSuperReference superReference, ExecutionContext ctx) {
        Space prefix = sourceBefore("super");

        return new J.Identifier(
                randomId(),
                prefix,
                Markers.EMPTY,
                "super",
                null,
                null);
    }

    @Override
    public J visitFunctionCall(FirFunctionCall functionCall, ExecutionContext ctx) {
        FirFunctionCallOrigin origin = functionCall.getOrigin();

        J j;
        if (origin == FirFunctionCallOrigin.Operator && !(functionCall instanceof FirImplicitInvokeCall)) {
            String operatorName = functionCall.getCalleeReference().getName().asString();
            if (isUnaryOperation(operatorName)) {
                j = mapUnaryOperation(functionCall);
            } else if ("contains".equals(operatorName) || "rangeTo".equals(operatorName) || "get".equals(operatorName) || "set".equals(operatorName)) {
                j = mapKotlinBinaryOperation(functionCall);
            } else {
                j = mapBinaryOperation(functionCall);
            }
        } else {
            j = mapFunctionCall(functionCall, origin == FirFunctionCallOrigin.Infix);
        }

        return j;
    }

    private J mapFunctionCall(FirFunctionCall functionCall, boolean isInfix) {
        Space prefix = whitespace();

        FirNamedReference namedReference = functionCall.getCalleeReference();
        if (namedReference instanceof FirResolvedNamedReference &&
                ((FirResolvedNamedReference) namedReference).getResolvedSymbol() instanceof FirConstructorSymbol) {
            TypeTree name;
            if (functionCall.getExplicitReceiver() != null) {
                J j = visitElement(functionCall.getExplicitReceiver(), null);
                name = new J.FieldAccess(
                        randomId(),
                        EMPTY,
                        Markers.EMPTY,
                        (Expression) j,
                        padLeft(sourceBefore("."), createIdentifier(namedReference.getName().asString(), namedReference)),
                        typeMapping.type(functionCall, getCurrentFile()));
            } else {
                name = (J.Identifier) visitElement(namedReference, null);
            }

            int saveCursor = cursor;
            whitespace();

            if (source.startsWith("<", cursor) && !functionCall.getTypeArguments().isEmpty()) {
                cursor(saveCursor);
                name = new J.ParameterizedType(randomId(), EMPTY, Markers.EMPTY, name, mapTypeArguments(functionCall.getTypeArguments()), typeMapping.type(functionCall, getCurrentFile()));
            } else {
                cursor(saveCursor);
            }

            JContainer<Expression> args;
            List<FirExpression> arrExpressions = new ArrayList<>(functionCall.getArgumentList().getArguments().size());
            FirLambdaArgumentExpression init = null;
            if (functionCall.getArgumentList() instanceof FirResolvedArgumentList) {
                for (Map.Entry<FirExpression, FirValueParameter> entry : ((FirResolvedArgumentList) functionCall.getArgumentList()).getMapping().entrySet()) {
                    if (entry.getKey() instanceof FirLambdaArgumentExpression && "init".equals(entry.getValue().getName().asString())) {
                        init = (FirLambdaArgumentExpression) entry.getKey();
                    } else {
                        arrExpressions.add(entry.getKey());
                    }
                }
            } else {
                arrExpressions = functionCall.getArgumentList().getArguments();
            }

            args = mapFunctionalCallArguments(arrExpressions);

            J.Block body = null;
            if (init != null) {
                body = new J.Block(
                        randomId(),
                        sourceBefore("{"),
                        Markers.EMPTY,
                        new JRightPadded<>(false, EMPTY, Markers.EMPTY),
                        singletonList(padRight((Statement) visitElement(init, ctx), EMPTY)),
                        sourceBefore("}"));
            }

            return new J.NewClass(
                    randomId(),
                    prefix,
                    Markers.EMPTY,
                    null,
                    EMPTY,
                    name,
                    args,
                    body,
                    typeMapping.methodInvocationType(functionCall, getCurrentFile()));

        } else {
            Markers markers = Markers.EMPTY;
            JRightPadded<Expression> select = null;

            if (isInfix) {
                markers = markers.addIfAbsent(new ReceiverType(randomId()));
            }

            if (!(functionCall instanceof FirImplicitInvokeCall)) {
                FirElement receiver = getReceiver(functionCall.getExplicitReceiver());
                if (receiver == null) {
                    receiver = getReceiver(functionCall.getDispatchReceiver());
                }

                if (receiver != null) {
                    Expression selectExpr = (Expression) visitElement(receiver, ctx);
                    Space after = whitespace();
                    if (source.startsWith(".", cursor)) {
                        skip(".");
                    } else if (source.startsWith("?.", cursor)) {
                        skip("?.");
                        markers = markers.addIfAbsent(new IsNullable(randomId(), EMPTY));
                    }

                    select = JRightPadded.build(selectExpr)
                            .withAfter(after);
                }
            }

            J.Identifier name = (J.Identifier) visitElement(namedReference, null);

            JContainer<Expression> typeParams = null;
            if (!functionCall.getTypeArguments().isEmpty()) {
                int saveCursor = cursor;
                whitespace();
                boolean parseTypeArguments = source.startsWith("<", cursor);
                cursor(saveCursor);
                if (parseTypeArguments) {
                    typeParams = mapTypeArguments(functionCall.getTypeArguments());
                }
            }

            int saveCursor = cursor;
            whitespace();
            JContainer<Expression> args;
            if (source.startsWith("(", cursor)) {
                cursor(saveCursor);
                args = mapFunctionalCallArguments(functionCall.getArgumentList().getArguments());
            } else {
                cursor(saveCursor);
                markers = markers.addIfAbsent(new OmitParentheses(randomId()));

                List<JRightPadded<Expression>> arguments = new ArrayList<>(functionCall.getArgumentList().getArguments().size());
                for (FirExpression argument : functionCall.getArgumentList().getArguments()) {
                    J j = visitElement(argument, null);
                    if (j instanceof Statement && !(j instanceof Expression)) {
                        j = new K.StatementExpression(randomId(), (Statement) j);
                    }
                    JRightPadded<Expression> padded = JRightPadded.build((Expression) j);
                    arguments.add(padded);
                }
                args = JContainer.build(arguments);
            }

            FirBasedSymbol<?> owner = getCurrentFile();
            if (namedReference instanceof FirResolvedNamedReference) {
                FirBasedSymbol<?> symbol = ((FirResolvedNamedReference) namedReference).getResolvedSymbol();
                if (symbol instanceof FirNamedFunctionSymbol) {
                    FirNamedFunctionSymbol namedFunctionSymbol = (FirNamedFunctionSymbol) symbol;
                    ConeClassLikeLookupTag lookupTag = ClassMembersKt.containingClass(namedFunctionSymbol);
                    if (lookupTag != null) {
                        owner = LookupTagUtilsKt.toFirRegularClassSymbol(lookupTag, firSession);
                    }
                }
            }

            JavaType.Method type = typeMapping.methodInvocationType(functionCall, owner);
            return new J.MethodInvocation(
                    randomId(),
                    prefix,
                    markers,
                    select,
                    typeParams,
                    name.withType(type),
                    args,
                    type);
        }
    }

    @Nullable
    private FirElement getReceiver(@Nullable FirElement firElement) {
        if (firElement == null || firElement.getSource() == null) {
            return null;
        }

        FirElement receiver = null;
        if (firElement instanceof FirCheckedSafeCallSubject) {
            receiver = ((FirCheckedSafeCallSubject) firElement).getOriginalReceiverRef().getValue();
        } else if (firElement instanceof FirThisReceiverExpression) {
            receiver = firElement;
        } else if (!(firElement instanceof FirNoReceiverExpression)) {
            receiver = firElement;
        }

        return receiver;
    }

    @Nullable
    private List<J.Annotation> mapFileAnnotations(@Nullable KtFileAnnotationList annotationList) {
        if (annotationList == null) {
            return null;
        }

        System.out.println();
        List<J.Annotation> annotations = new ArrayList<>(annotationList.getChildren().length);
        for (PsiElement annotation : annotationList.getChildren()) {
            // convert KtAnnotation to J.Annotation
        }

        return annotations;
    }

    private JContainer<Expression> mapFunctionalCallArguments(List<FirExpression> firExpressions) {
        JContainer<Expression> args;
        if (firExpressions.size() == 1) {
            FirExpression firExpression = firExpressions.get(0);
            if (firExpression instanceof FirVarargArgumentsExpression) {
                FirVarargArgumentsExpression argumentsExpression = (FirVarargArgumentsExpression) firExpressions.get(0);
                args = JContainer.build(sourceBefore("("), argumentsExpression.getArguments().isEmpty() ?
                        singletonList(padRight(new J.Empty(randomId(), sourceBefore(")"), Markers.EMPTY), EMPTY)) :
                        convertAll(argumentsExpression.getArguments(), commaDelim, t -> sourceBefore(")"), ctx, true), Markers.EMPTY);
            } else {
                args = JContainer.build(sourceBefore("("), convertAll(singletonList(firExpression), commaDelim, t -> sourceBefore(")"), ctx, true), Markers.EMPTY);
            }
        } else {
            if (firExpressions.isEmpty()) {
                int saveCursor = cursor;
                Space before = whitespace();
                if (source.startsWith("{", cursor)) {
                    // function call arguments with no parens.
                    cursor(saveCursor);
                    args = JContainer.build(before,
                            singletonList(padRight(new J.Empty(randomId(), EMPTY, Markers.EMPTY), EMPTY)), Markers.EMPTY.addIfAbsent(new OmitParentheses(randomId())));
                } else {
                    skip("(");
                    args = JContainer.build(before,
                            singletonList(padRight(new J.Empty(randomId(), sourceBefore(")"), Markers.EMPTY), EMPTY)), Markers.EMPTY);
                }
            } else {
                Space containerPrefix = sourceBefore("(");
                List<FirExpression> flattenedExpressions = firExpressions.stream()
                        .map(e -> e instanceof FirVarargArgumentsExpression ? ((FirVarargArgumentsExpression) e).getArguments() : singletonList(e))
                        .flatMap(Collection::stream)
                        .collect(toList());

                List<JRightPadded<Expression>> expressions = new ArrayList<>(flattenedExpressions.size());
                boolean isTrailingComma = false;
                for (int i = 0; i < flattenedExpressions.size(); i++) {
                    FirExpression expression = flattenedExpressions.get(i);
                    J j = convert(expression, ctx);
                    if (j instanceof Statement && !(j instanceof Expression)) {
                        j = new K.StatementExpression(randomId(), (Statement) j);
                    }
                    Expression expr = (Expression) j;
                    if (isTrailingComma) {
                        expr = expr.withMarkers(expr.getMarkers().addIfAbsent(new TrailingLambdaArgument(randomId())));
                        expressions.add(padRight(expr, EMPTY));
                        break;
                    }

                    Space padding = whitespace();
                    if (i < flattenedExpressions.size() - 1) {
                        if (source.startsWith(",", cursor)) {
                            skip(",");
                        } else if (source.startsWith(")", cursor) && flattenedExpressions.get(i + 1) instanceof FirLambdaArgumentExpression) {
                            // Trailing comma: https://kotlinlang.org/docs/coding-conventions.html#trailing-commas
                            isTrailingComma = true;
                            skip(")");
                        }
                    } else {
                        skip(")");
                    }
                    expressions.add(padRight(expr, padding));
                }
                args = JContainer.build(containerPrefix, expressions, Markers.EMPTY);
            }
        }
        return args;
    }

    private JContainer<Expression> mapTypeArguments(List<? extends FirElement> types) {
        Space prefix = whitespace();
        if (source.startsWith("<", cursor)) {
            skip("<");
        }
        List<JRightPadded<Expression>> parameters = new ArrayList<>(types.size());

        for (int i = 0; i < types.size(); i++) {
            FirElement type = types.get(i);
            JRightPadded<Expression> padded = JRightPadded.build((Expression) visitElement(type, null))
                    .withAfter(i < types.size() - 1 ?
                            sourceBefore(",") :
                            whitespace());
            parameters.add(padded);
        }

        if (source.startsWith(">", cursor)) {
            skip(">");
        }
        return JContainer.build(prefix, parameters, Markers.EMPTY);
    }

    private J mapUnaryOperation(FirFunctionCall functionCall) {
        Space prefix = whitespace();
        String name = functionCall.getCalleeReference().getName().asString();

        JLeftPadded<J.Unary.Type> op;
        Expression expr;

        switch (name) {
            case "dec":
                if (source.startsWith("--", cursor)) {
                    skip("--");
                    op = padLeft(EMPTY, J.Unary.Type.PreDecrement);
                    expr = (Expression) visitElement(functionCall.getDispatchReceiver(), ctx);
                } else {
                    int saveCursor = cursor;
                    String opName = sourceBefore("--").getWhitespace().trim();
                    cursor(saveCursor);

                    expr = createIdentifier(opName);
                    op = padLeft(sourceBefore("--"), J.Unary.Type.PostDecrement);
                }
                break;
            case "inc":
                if (source.startsWith("++", cursor)) {
                    skip("++");
                    op = padLeft(EMPTY, J.Unary.Type.PreIncrement);
                    expr = (Expression) visitElement(functionCall.getDispatchReceiver(), ctx);
                } else {
                    int saveCursor = cursor;
                    String opName = sourceBefore("++").getWhitespace().trim();
                    cursor(saveCursor);

                    expr = createIdentifier(opName);
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

        return new J.Unary(
                randomId(),
                prefix,
                Markers.EMPTY,
                op,
                expr,
                typeMapping.type(functionCall));
    }

    private J mapKotlinBinaryOperation(FirFunctionCall functionCall) {
        Space prefix = whitespace();


        Expression left;
        Space opPrefix;
        K.Binary.Type kotlinBinaryType;
        Expression right;
        Space after = EMPTY;

        String name = functionCall.getCalleeReference().getName().asString();
        if ("contains".equals(name)) {
            // Prevent SOE of methods with an implicit LHS that refers to the subject of a when expression.
            if (functionCall.getArgumentList().getArguments().get(0) instanceof FirWhenSubjectExpression) {
                left = new J.Empty(randomId(), EMPTY, Markers.EMPTY);
            } else {
                left = (Expression) visitElement(functionCall.getArgumentList().getArguments().get(0), ctx);
            }

            // The `in` keyword is a function call to `contains` applied to a primitive range. I.E., `IntRange`, `LongRange`.
            opPrefix = sourceBefore("in");
            kotlinBinaryType = K.Binary.Type.Contains;

            right = (Expression) visitElement(functionCall.getExplicitReceiver(), ctx);

        } else if ("get".equals(name)) {
            // TODO: Check if the function call may be safely changed to a J.ArrayAccess. Note: must cover not null checks: `!!`.
            left = (Expression) visitElement(functionCall.getExplicitReceiver(), ctx);

            opPrefix = sourceBefore("[");
            kotlinBinaryType = K.Binary.Type.Get;

            right = (Expression) visitElement(functionCall.getArgumentList().getArguments().get(0), ctx);

            after = sourceBefore("]");
        } else if ("set".equals(name)) {
            // Note: the kotlin set function call is converted to a J.Assignment and may be an issue in Kotlin recipes in the future.
            left = (Expression) visitElement(functionCall.getExplicitReceiver(), ctx);
            left = new J.ArrayAccess(
                    randomId(),
                    left.getPrefix(),
                    Markers.EMPTY,
                    left.withPrefix(EMPTY),
                    new J.ArrayAccess.ArrayDimension(
                            randomId(),
                            sourceBefore("["),
                            Markers.EMPTY,
                            padRight((Expression) visitElement(functionCall.getArgumentList().getArguments().get(0), ctx), sourceBefore("]"))
                    ),
                    typeMapping.type(typeMapping.type(functionCall.getArgumentList().getArguments().get(1))));

            Space before = whitespace();
            if (source.startsWith("=", cursor)) {
                skip("=");
            } else {
                // Check for syntax de-sugaring.
                throw new UnsupportedOperationException("Unsupported set operator type.");
            }

            return new J.Assignment(
                    randomId(),
                    prefix,
                    Markers.EMPTY,
                    left,
                    padLeft(before, (Expression) visitElement(functionCall.getArgumentList().getArguments().get(1), ctx)),
                    typeMapping.type(functionCall.getArgumentList().getArguments().get(1))
            );
        } else {
            left = (Expression) visitElement(functionCall.getExplicitReceiver(), ctx);

            opPrefix = sourceBefore("..");
            kotlinBinaryType = K.Binary.Type.RangeTo;

            right = (Expression) visitElement(functionCall.getArgumentList().getArguments().get(0), ctx);
        }

        return new K.Binary(
                randomId(),
                prefix,
                Markers.EMPTY,
                left,
                padLeft(opPrefix, kotlinBinaryType),
                right,
                after,
                typeMapping.type(functionCall));
    }

    private J mapBinaryOperation(FirFunctionCall functionCall) {
        Space prefix = whitespace();
        Space binaryPrefix;

        boolean isParenthesized = false;
        if (!(functionCall.getDispatchReceiver() instanceof FirFunctionCall) && source.startsWith("(", cursor)) {
            isParenthesized = true;
            skip("(");
            // The next whitespace is prefix of the binary operation.
            binaryPrefix = whitespace();
        } else {
            binaryPrefix = prefix;
        }

        FirElement receiver = functionCall.getExplicitReceiver() != null ? functionCall.getExplicitReceiver() : functionCall.getDispatchReceiver();
        Expression left = (Expression) visitElement(receiver, ctx);

        Space opPrefix;
        J.Binary.Type javaBinaryType;

        String name = functionCall.getCalleeReference().getName().asString();
        switch (name) {
            case "div":
                javaBinaryType = J.Binary.Type.Division;
                opPrefix = sourceBefore("/");
                break;
            case "minus":
                javaBinaryType = J.Binary.Type.Subtraction;
                opPrefix = sourceBefore("-");
                break;
            case "plus":
                javaBinaryType = J.Binary.Type.Addition;
                opPrefix = sourceBefore("+");
                break;
            case "rem":
                javaBinaryType = J.Binary.Type.Modulo;
                opPrefix = sourceBefore("%");
                break;
            case "times":
                javaBinaryType = J.Binary.Type.Multiplication;
                opPrefix = sourceBefore("*");
                break;
            default:
                throw new UnsupportedOperationException("Unsupported binary operator type.");
        }
        Expression right = (Expression) visitElement(functionCall.getArgumentList().getArguments().get(0), ctx);

        J.Binary binary = new J.Binary(
                randomId(),
                binaryPrefix,
                Markers.EMPTY,
                left,
                padLeft(opPrefix, javaBinaryType),
                right,
                typeMapping.type(functionCall));
        return !isParenthesized ? binary : new J.Parentheses<Expression>(randomId(), prefix, Markers.EMPTY, padRight(binary, sourceBefore(")")));
    }

    @Override
    public J visitFunctionTypeRef(FirFunctionTypeRef functionTypeRef, ExecutionContext ctx) {

        J.Annotation annotation = null;
        if (functionTypeRef.isSuspend()) {
            Space suspendPrefix = whitespace();
            annotation = convertToAnnotation(mapModifier(suspendPrefix, emptyList()));
        } else if (source.startsWith("noinline", cursor)) {
            annotation = convertToAnnotation(mapModifier(EMPTY, emptyList()));
        } else if (source.startsWith("crossinline", cursor)) {
            annotation = convertToAnnotation(mapModifier(EMPTY, emptyList()));
        }

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
                    .build((J) new J.Empty(randomId(), EMPTY, Markers.EMPTY))
                    .withAfter(sourceBefore(")"))));
        }

        Space arrow = sourceBefore("->");
        int saveCursor = cursor;
        whitespace();
        boolean omitBraces = !source.startsWith("{");
        cursor(saveCursor);

        J body = visitElement(functionTypeRef.getReturnTypeRef(), ctx);
        if (body instanceof J.Block) {
            body = ((J.Block) body).withEnd(sourceBefore("}"));
        }

        if (functionTypeRef.getValueParameters().isEmpty()) {
            body = body.withMarkers(body.getMarkers().removeByType(OmitBraces.class));
        }

        J.Lambda lambda = new J.Lambda(
                randomId(),
                prefix,
                omitBraces ? Markers.EMPTY.addIfAbsent(new OmitBraces(randomId())) : Markers.EMPTY,
                params,
                arrow,
                body,
                closureType);

        return new K.FunctionType(
                randomId(),
                lambda,
                annotation,
                receiver);
    }

    @Override
    public J visitImport(FirImport firImport, ExecutionContext ctx) {
        Space prefix = sourceBefore("import");
        JLeftPadded<Boolean> static_ = padLeft(EMPTY, false);

        Space space = whitespace();
        String packageName = firImport.getImportedFqName() == null ? "" : firImport.isAllUnder() ?
                firImport.getImportedFqName().asString() + ".*" :
                firImport.getImportedFqName().asString();
        J.FieldAccess qualid = TypeTree.build(packageName).withPrefix(space);
        skip(qualid.toString());
        JLeftPadded<J.Identifier> alias = null;
        if (firImport.getAliasName() != null) {
            Space asPrefix = sourceBefore("as");
            Space aliasPrefix = whitespace();
            String aliasText = firImport.getAliasName().asString();
            skip(aliasText);
            // FirImport does not contain type attribution information, so we cannot use the type mapping here.
            J.Identifier aliasId = createIdentifier(aliasText)
                    .withPrefix(aliasPrefix);
            alias = padLeft(asPrefix, aliasId);
        }
        return new J.Import(
                randomId(),
                prefix,
                Markers.EMPTY,
                static_,
                qualid,
                alias);
    }

    @Override
    public J visitPackageDirective(FirPackageDirective packageDirective, ExecutionContext ctx) {
        Space pkgPrefix = whitespace();
        skip("package");

        Space pkgNamePrefix = whitespace();
        String packageName = packageDirective.getPackageFqName().asString();
        skip(packageName);
        return new J.Package(
                randomId(),
                pkgPrefix,
                Markers.EMPTY,
                TypeTree.build(packageName).withPrefix(pkgNamePrefix),
                emptyList());
    }

    @Override
    public J visitGetClassCall(FirGetClassCall getClassCall, ExecutionContext ctx) {
        return new J.MemberReference(
                randomId(),
                whitespace(),
                Markers.EMPTY,
                padRight((Expression) visitElement(getClassCall.getArgument(), ctx), sourceBefore("::")),
                null,
                padLeft(whitespace(), createIdentifier("class")),
                typeMapping.type(getClassCall),
                null,
                null);
    }

    @Override
    public J visitLabel(FirLabel label, ExecutionContext ctx) {
        return new J.Label(
                randomId(),
                whitespace(),
                Markers.EMPTY,
                padRight(createIdentifier(label.getName()), sourceBefore("@")),
                // The label exists on the FIR statement, and needs to be set in the statements visit.
                null);
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
        J.Identifier name = createIdentifier(namedArgumentExpression.getName().toString());
        Space exprPrefix = sourceBefore("=");
        J j = visitElement(namedArgumentExpression.getExpression(), ctx);
        if (j instanceof Statement && !(j instanceof Expression)) {
            j = new K.StatementExpression(randomId(), (Statement) j);
        }
        Expression expr = (Expression) j;
        return new J.Assignment(
                randomId(),
                prefix,
                Markers.EMPTY,
                name,
                padLeft(exprPrefix, expr),
                typeMapping.type(namedArgumentExpression.getTypeRef()));
    }

    @Override
    public J visitProperty(FirProperty property, ExecutionContext ctx) {
        Space prefix = whitespace();
        Markers markers = Markers.EMPTY;

        List<J> modifiers = emptyList();
        List<FirAnnotation> mapAnnotations = new ArrayList<>(property.getAnnotations());
        // Note: it might be possible to have annotations on both the property associated to the getter
        // AND the getter itself. In that case, we to use a single list to avoid duplicates and add
        // the target annotations to the appropriate LST element.
        if (property.getGetter() != null && !property.getGetter().getAnnotations().isEmpty()) {
            mapAnnotations.addAll(property.getGetter().getAnnotations());
        }

        List<J.Annotation> annotations = mapModifiers(mapAnnotations, property.getName().asString());

        J.VariableDeclarations receiver = null;
        if (property.getReceiverTypeRef() != null) {
            // Generates a VariableDeclaration to represent the receiver similar to how it is done in the Kotlin compiler.
            TypeTree receiverName = (TypeTree) visitElement(property.getReceiverTypeRef(), ctx);
            markers = markers.addIfAbsent(new ReceiverType(randomId()));
            J.VariableDeclarations.NamedVariable receiverVar = new J.VariableDeclarations.NamedVariable(
                    randomId(),
                    EMPTY,
                    Markers.EMPTY,
                    new J.Identifier(randomId(), EMPTY, Markers.EMPTY, "", null, null),
                    emptyList(),
                    null,
                    null
            );
            receiver = new J.VariableDeclarations(
                    randomId(),
                    EMPTY,
                    Markers.EMPTY,
                    emptyList(),
                    emptyList(),
                    receiverName,
                    null,
                    emptyList(),
                    singletonList(padRight(receiverVar, sourceBefore(".")))
            ).withMarkers(Markers.EMPTY.addIfAbsent(new Implicit(randomId())));
        }

        boolean isDestruct = "<destruct>".equals(property.getName().asString()) && property.getReturnTypeRef() instanceof FirResolvedTypeRef;
        int initialCapacity = isDestruct ?
                ((FirResolvedTypeRef) property.getReturnTypeRef()).getType().getTypeArguments().length : 1;
        List<JRightPadded<J.VariableDeclarations.NamedVariable>> variables = new ArrayList<>(initialCapacity);

        TypeTree typeExpression = null;
        if (isDestruct) {
            // Destructuring declarations do not fit well into the `J.VariableDeclarations` model.
            // So, the position of prefixes are slightly adjusted to preserve the prefix of `(` and the suffix of `)`.
            Space destructPrefix = sourceBefore("(");
            int saveCursor = cursor;
            String params = sourceBefore(")").getWhitespace();
            String[] paramNames = params.split(",");
            cursor(saveCursor);

            ConeTypeProjection[] typeArguments = null;
            if (property.getReturnTypeRef() instanceof FirResolvedTypeRef && (property.getReturnTypeRef().getSource() == null || !(property.getReturnTypeRef().getSource().getKind() instanceof KtFakeSourceElementKind))) {
                FirResolvedTypeRef typeRef = (FirResolvedTypeRef) property.getReturnTypeRef();
                typeArguments = typeRef.getType().getTypeArguments();
            }

            JRightPadded<J.VariableDeclarations.NamedVariable> namedVariable = null;
            for (int j = 0; j < paramNames.length; j++) {
                String paramName = paramNames[j].trim();
                JavaType type = typeArguments == null || j >= typeArguments.length ? null : typeMapping.type(typeArguments[j]);
                J.Identifier name = createIdentifier(paramName, type, null);
                namedVariable = JRightPadded.build(
                        new J.VariableDeclarations.NamedVariable(
                                randomId(),
                                destructPrefix == null ? EMPTY : destructPrefix,
                                Markers.EMPTY,
                                name,
                                emptyList(),
                                null,
                                typeMapping.variableType(property.getSymbol(), null, getCurrentFile())
                        ));
                if (j != paramNames.length - 1) {
                    namedVariable = namedVariable.withAfter(sourceBefore(","));
                    variables.add(namedVariable);
                } else {
                    // The variable is not added here, because an initializer might be present.
                    namedVariable = namedVariable.withAfter(sourceBefore(")"));
                }
                destructPrefix = null;
            }

            J expr = null;
            saveCursor = cursor;
            Space exprPrefix = whitespace();
            if (property.getInitializer() != null && source.startsWith("=", cursor)) {
                skip("=");
                expr = visitElement(property.getInitializer(), ctx);
                if (expr instanceof Statement && !(expr instanceof Expression)) {
                    expr = new K.StatementExpression(randomId(), (Statement) expr);
                }
            } else {
                exprPrefix = EMPTY;
                cursor(saveCursor);
            }
            assert namedVariable != null;
            namedVariable = namedVariable.withElement(namedVariable.getElement().getPadding().withInitializer(expr == null ? null : padLeft(exprPrefix, (Expression) expr)));
            variables.add(namedVariable);
        } else {
            Space namePrefix = whitespace();
            J.Identifier name = createIdentifier(property.getName().asString(), property);

            Space exprPrefix = EMPTY;
            J initializer;
            List<J> expressions = null;
            Markers initMarkers = Markers.EMPTY;
            if (property.getDelegate() != null) {
                if (property.getDelegate() instanceof FirFunctionCall && "lazy".equals(((FirFunctionCall) property.getDelegate()).getCalleeReference().getName().asString())) {
                    exprPrefix = whitespace();
                    String current = source.substring(cursor);
                    J.Identifier delegateName = createIdentifier(current.substring(0, current.indexOf(((FirFunctionCall) property.getDelegate()).getCalleeReference().getName().asString())).trim());
                    skip(delegateName.getSimpleName());

                    initializer = visitElement(property.getDelegate(), ctx);
                    initMarkers = initMarkers.addIfAbsent(new By(randomId()));
                    if (initializer instanceof Statement && !(initializer instanceof Expression)) {
                        initializer = new K.StatementExpression(randomId(), (Statement) initializer);
                    }
                    expressions = new ArrayList<>(1);
                    expressions.add(initializer);
                } else {
                    throw new UnsupportedOperationException(generateUnsupportedMessage("Unexpected property delegation. FirProperty#delegate for name: " +
                            ((FirFunctionCall) property.getDelegate()).getCalleeReference().getName().asString()));
                }
            } else if (property.getReturnTypeRef() instanceof FirResolvedTypeRef && (property.getReturnTypeRef().getSource() == null || !(property.getReturnTypeRef().getSource().getKind() instanceof KtFakeSourceElementKind))) {
                FirResolvedTypeRef typeRef = (FirResolvedTypeRef) property.getReturnTypeRef();
                if (typeRef.getDelegatedTypeRef() != null) {
                    Space delimiterPrefix = whitespace();
                    boolean addTypeReferencePrefix = source.startsWith(":", cursor);
                    skip(":");
                    if (addTypeReferencePrefix) {
                        markers = markers.addIfAbsent(new TypeReferencePrefix(randomId(), delimiterPrefix));
                    }
                    J j = visitElement(typeRef, ctx);
                    if (j instanceof TypeTree) {
                        typeExpression = (TypeTree) j;
                    } else {
                        typeExpression = new K.FunctionType(randomId(), (TypedTree) j, null, null);
                    }
                }
            }

            if (property.getInitializer() != null && source.substring(cursor).trim().startsWith("=")) {
                exprPrefix = whitespace();
                expressions = new ArrayList<>(1);
                skip("=");
                initializer = visitElement(property.getInitializer(), ctx);
                if (initializer instanceof Statement && !(initializer instanceof Expression)) {
                    initializer = new K.StatementExpression(randomId(), (Statement) initializer);
                }
                expressions.add(initializer);
            } else if (initMarkers.getMarkers().isEmpty()) {
                initMarkers = initMarkers.addIfAbsent(new OmitEquals(randomId()));
            }

            // getters and setter may be auto-generated by the compiler, so we need to check if they are valid.
            // ... the order of the getter and setter is not preserved by compiler, so we have to sort them out.
            J.MethodDeclaration getter;
            J.MethodDeclaration setter;
            if (isValidGetter(property.getGetter()) && isValidSetter(property.getSetter())) {
                if (expressions == null) {
                    expressions = new ArrayList<>(2);
                }

                int saveCursor = cursor;
                whitespace();

                String methodName = source.substring(cursor, cursor + 3);
                switch (methodName) {
                    case "get":
                        cursor(saveCursor);
                        if (isValidGetter(property.getGetter())) {
                            getter = (J.MethodDeclaration) visitElement(property.getGetter(), ctx);
                            if (receiver != null) {
                                getter = getter.withParameters(ListUtils.concat(receiver, getter.getParameters()));
                            }
                            expressions.add(getter);
                        }
                        break;
                    case "set":
                        cursor(saveCursor);
                        if (isValidSetter(property.getSetter())) {
                            setter = (J.MethodDeclaration) visitElement(property.getSetter(), ctx);
                            if (receiver != null) {
                                setter = setter.withParameters(ListUtils.concat(receiver, setter.getParameters()));
                            }
                            expressions.add(setter);
                        }
                        break;
                    default:
                        cursor(saveCursor);
                        break;
                }
                saveCursor = cursor;
                whitespace();

                methodName = source.substring(cursor, cursor + 3);
                switch (methodName) {
                    case "get":
                        cursor(saveCursor);
                        if (isValidGetter(property.getGetter())) {
                            getter = (J.MethodDeclaration) visitElement(property.getGetter(), ctx);
                            if (receiver != null) {
                                getter = getter.withParameters(ListUtils.concat(receiver, getter.getParameters()));
                            }
                            expressions.add(getter);
                        }
                        break;
                    case "set":
                        cursor(saveCursor);
                        if (isValidSetter(property.getSetter())) {
                            setter = (J.MethodDeclaration) visitElement(property.getSetter(), ctx);
                            if (receiver != null) {
                                setter = setter.withParameters(ListUtils.concat(receiver, setter.getParameters()));
                            }
                            expressions.add(setter);
                        }
                        break;
                    default:
                        cursor(saveCursor);
                        break;
                }
            } else if (isValidGetter(property.getGetter()) && !isValidSetter(property.getSetter())) {
                if (expressions == null) {
                    expressions = new ArrayList<>(2);
                }

                getter = (J.MethodDeclaration) visitElement(property.getGetter(), ctx);
                if (receiver != null) {
                    getter = getter.withParameters(ListUtils.concat(receiver, getter.getParameters()));

                    setter = createImplicitMethodDeclaration("set").withParameters(singletonList(receiver));
                    expressions.add(setter);
                }
                expressions.add(getter);
            } else if (!isValidGetter(property.getGetter()) && isValidSetter(property.getSetter())) {
                if (expressions == null) {
                    expressions = new ArrayList<>(2);
                }

                setter = (J.MethodDeclaration) visitElement(property.getSetter(), ctx);
                if (receiver != null) {
                    setter = setter.withParameters(ListUtils.concat(receiver, setter.getParameters()));

                    getter = createImplicitMethodDeclaration("get").withParameters(singletonList(receiver));
                    expressions.add(getter);
                }
                expressions.add(setter);
            } else if (receiver != null) {
                if (expressions == null) {
                    expressions = new ArrayList<>(2);
                }

                getter = createImplicitMethodDeclaration("get").withParameters(singletonList(receiver));
                expressions.add(getter);

                setter = createImplicitMethodDeclaration("set").withParameters(singletonList(receiver));
                expressions.add(setter);
            }

            Expression expr = null;
            if (expressions != null) {
                expr = new K.NamedVariableInitializer(
                        randomId(),
                        EMPTY,
                        initMarkers,
                        expressions
                );
            }

            JRightPadded<J.VariableDeclarations.NamedVariable> namedVariable = maybeSemicolon(
                    new J.VariableDeclarations.NamedVariable(
                            randomId(),
                            namePrefix,
                            Markers.EMPTY,
                            name,
                            emptyList(),
                            expr == null ? null : padLeft(exprPrefix, expr),
                            typeMapping.variableType(property.getSymbol(), null, getCurrentFile())
                    ));

            variables.add(namedVariable);
        }

        return new J.VariableDeclarations(
                randomId(),
                prefix,
                markers,
                annotations,
                emptyList(),
                typeExpression,
                null,
                emptyList(),
                variables);
    }

    private boolean isValidGetter(@Nullable FirPropertyAccessor getter) {
        return getter != null && !(getter instanceof FirDefaultPropertyGetter) &&
                (getter.getSource() == null || !(getter.getSource().getKind() instanceof KtFakeSourceElementKind));
    }

    private boolean isValidSetter(@Nullable FirPropertyAccessor setter) {
        return setter != null && !(setter instanceof FirDefaultPropertySetter) &&
                (setter.getSource() == null || !(setter.getSource().getKind() instanceof KtFakeSourceElementKind));
    }

    @Override
    public J visitPropertyAccessExpression(FirPropertyAccessExpression propertyAccessExpression, ExecutionContext ctx) {
        JavaType type = typeMapping.type(propertyAccessExpression);
        if (propertyAccessExpression.getExplicitReceiver() != null) {
            Space prefix = whitespace();
            Expression target = (Expression) visitElement(propertyAccessExpression.getExplicitReceiver(), ctx);
            Space before = whitespace();
            Markers markers = Markers.EMPTY;
            if (source.startsWith(".", cursor)) {
                skip(".");
            } else if (source.startsWith("?.", cursor)) {
                skip("?.");
                markers = markers.addIfAbsent(new IsNullable(randomId(), EMPTY));
            }

            JLeftPadded<J.Identifier> name = padLeft(before, (J.Identifier) visitElement(propertyAccessExpression.getCalleeReference(), ctx));
            return new J.FieldAccess(
                    randomId(),
                    prefix,
                    markers,
                    target,
                    name,
                    type);
        } else {
            return visitElement(propertyAccessExpression.getCalleeReference(), ctx);
        }
    }

    @Override
    public J visitPropertyAccessor(FirPropertyAccessor propertyAccessor, ExecutionContext ctx) {
        Space prefix = whitespace();
        if (propertyAccessor.isGetter() || propertyAccessor.isSetter()) {
            Markers markers = Markers.EMPTY;
            List<J> modifiers = emptyList();
            List<J.Annotation> annotations = emptyList();

            J.TypeParameters typeParameters = propertyAccessor.getTypeParameters().isEmpty() ? null :
                    new J.TypeParameters(randomId(), sourceBefore("<"), Markers.EMPTY,
                            emptyList(),
                            convertAll(propertyAccessor.getTypeParameters(), commaDelim, t -> sourceBefore(">"), ctx));

            String methodName = propertyAccessor.isGetter() ? "get" : "set";
            J.Identifier name = createIdentifier(methodName, propertyAccessor);

            JContainer<Statement> params = JContainer.build(sourceBefore("("), propertyAccessor.isGetter() ?
                    singletonList(padRight(new J.Empty(randomId(), sourceBefore(")"), Markers.EMPTY), EMPTY)) :
                    convertAll(propertyAccessor.getValueParameters(), commaDelim, t -> sourceBefore(")"), ctx, true), Markers.EMPTY);

            int saveCursor = cursor;
            Space nextPrefix = whitespace();
            TypeTree returnTypeExpression = null;
            // Only add the type reference if it exists in source code.
            if (!(propertyAccessor.getReturnTypeRef() instanceof FirImplicitUnitTypeRef) && source.startsWith(":", cursor)) {
                skip(":");
                markers = markers.addIfAbsent(new TypeReferencePrefix(randomId(), nextPrefix));
                returnTypeExpression = (TypeTree) visitElement(propertyAccessor.getReturnTypeRef(), ctx);
            } else {
                cursor(saveCursor);
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
                cursor(saveCursor);
                body = convertOrNull(propertyAccessor.getBody(), ctx);
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
                    typeMapping.methodDeclarationType(propertyAccessor, null, getCurrentFile()));
        }

        throw new UnsupportedOperationException("Unsupported property accessor.");
    }

    @Override
    public J visitResolvedNamedReference(FirResolvedNamedReference resolvedNamedReference, ExecutionContext ctx) {
        String name = resolvedNamedReference.getName().asString();
        return createIdentifier(name, resolvedNamedReference);
    }

    @Override
    public J visitReturnExpression(FirReturnExpression returnExpression, ExecutionContext ctx) {
        Space prefix = whitespace();
        J.Identifier label = null;
        boolean explicitReturn = source.startsWith("return", cursor);
        if (explicitReturn) {
            skip("return");
            if (source.startsWith("@", cursor)) {
                skip("@");
                label = createIdentifier(returnExpression.getTarget().getLabelName());
            }
        }

        J j = null;
        if (!(returnExpression.getResult() instanceof FirUnitExpression)) {
            j = visitElement(returnExpression.getResult(), ctx);
        }
        Expression returnExpr = j == null ? null : j instanceof Statement && !(j instanceof Expression) ? new K.StatementExpression(randomId(), (Statement) j) : (Expression) j;
        K.KReturn k = new K.KReturn(randomId(), new J.Return(randomId(), prefix, Markers.EMPTY, returnExpr), label);
        return explicitReturn ? k : k.withMarkers(k.getMarkers().addIfAbsent(new ImplicitReturn(randomId())));
    }

    @Override
    public J visitResolvedTypeRef(FirResolvedTypeRef resolvedTypeRef, ExecutionContext ctx) {
        if (resolvedTypeRef.getDelegatedTypeRef() != null) {
            J j = visitElement(resolvedTypeRef.getDelegatedTypeRef(), ctx);
            JavaType type = typeMapping.type(resolvedTypeRef);
            if (j instanceof TypeTree) {
                j = ((TypeTree) j).withType(type);
            }

            if (j instanceof J.ParameterizedType) {
                // The identifier on a parameterized type of the FIR does not contain type information and must be added separately.
                J.ParameterizedType parameterizedType = (J.ParameterizedType) j;
                j = parameterizedType.withClazz(parameterizedType.getClazz().withType(type));
            }
            return j;
        } else {
            // The type reference only exists in the source code if it is not a delegated type reference.
            // So, we use the name of the symbol to find the type reference in the source code.
            FirRegularClassSymbol symbol = TypeUtilsKt.toRegularClassSymbol(resolvedTypeRef.getType(), firSession);
            if (symbol != null) {
                Space prefix = whitespace();
                String name = symbol.getName().asString();
                int pos = source.substring(cursor).indexOf(name);
                String fullName = source.substring(cursor, cursor + pos + name.length());
                skip(fullName);
                TypeTree typeTree = TypeTree.build(fullName).withPrefix(prefix);
                int saveCursor = cursor;
                Space nextPrefix = whitespace();
                if (source.startsWith("?", cursor)) {
                    skip("?");
                    if (typeTree instanceof J.FieldAccess) {
                        J.FieldAccess fa = (J.FieldAccess) typeTree;
                        typeTree = fa.withName(fa.getName().withMarkers(fa.getName().getMarkers().addIfAbsent(new IsNullable(randomId(), nextPrefix))));
                    } else {
                        typeTree = typeTree.withMarkers(typeTree.getMarkers().addIfAbsent(new IsNullable(randomId(), nextPrefix)));
                    }
                } else {
                    cursor(saveCursor);
                }
                return typeTree.withType(typeMapping.type(resolvedTypeRef));
            }
        }
        throw new UnsupportedOperationException("Unsupported null delegated type reference.");
    }

    @Override
    public J visitResolvedReifiedParameterReference(FirResolvedReifiedParameterReference resolvedReifiedParameterReference, ExecutionContext ctx) {
        return createIdentifier(resolvedReifiedParameterReference.getSymbol().getFir().getName().asString(), typeMapping.type(resolvedReifiedParameterReference), null);
    }

    @Override
    public J visitResolvedQualifier(FirResolvedQualifier resolvedQualifier, ExecutionContext ctx) {
        String fieldAccess = resolvedQualifier.getPackageFqName().asString();
        String resolvedName = resolvedQualifier.getRelativeClassFqName() == null ? "" : "." + resolvedQualifier.getRelativeClassFqName().asString();
        String[] split = (fieldAccess + resolvedName).split("\\.");
        StringBuilder name = new StringBuilder();
        for (int i = 0; i < split.length; i++) {
            String part = split[i];
            name.append(whitespace().getWhitespace());
            if (source.startsWith(part, cursor)) {
                skip(part);
                name.append(part);
            }
            if (i < split.length - 1) {
                name.append(whitespace().getWhitespace());
                if (source.startsWith(".", cursor)) {
                    skip(".");
                    name.append(".");
                }
            }
        }

        TypeTree typeTree = TypeTree.build(name.toString());
        if (resolvedQualifier.getRelativeClassFqName() != null) {
            typeTree = typeTree.withType(typeMapping.type(resolvedQualifier));
        }
        return typeTree;
    }

    @Override
    public J visitSafeCallExpression(FirSafeCallExpression safeCallExpression, ExecutionContext ctx) {
        return visitElement(safeCallExpression.getSelector(), ctx);
    }

    @Override
    public J visitCheckedSafeCallSubject(FirCheckedSafeCallSubject checkedSafeCallSubject, ExecutionContext ctx) {
        return visitElement(checkedSafeCallSubject.getOriginalReceiverRef().getValue(), ctx);
    }

    @Override
    public J visitSimpleFunction(FirSimpleFunction simpleFunction, ExecutionContext ctx) {
        Space prefix = whitespace();
        Markers markers = Markers.EMPTY;
        List<J> modifiers = emptyList();
        List<J.Annotation> annotations = mapModifiers(simpleFunction.getAnnotations(), simpleFunction.getName().asString());

        J.TypeParameters typeParameters = simpleFunction.getTypeParameters().isEmpty() ? null :
                new J.TypeParameters(randomId(), sourceBefore("<"), Markers.EMPTY,
                        emptyList(),
                        convertAll(simpleFunction.getTypeParameters(), commaDelim, t -> sourceBefore(">"), ctx));

        JRightPadded<J.VariableDeclarations.NamedVariable> infixReceiver = null;
        if (simpleFunction.getReceiverTypeRef() != null) {
            // Infix functions are de-sugared during the backend phase of the compiler.
            // The de-sugaring process moves the infix receiver to the first position of the method declaration.
            // The infix receiver is added as to the `J.MethodInvocation` parameters, and marked to distinguish the parameter.
            markers = markers.addIfAbsent(new ReceiverType(randomId()));
            Expression receiver = (Expression) visitElement(simpleFunction.getReceiverTypeRef(), ctx);
            infixReceiver = JRightPadded.build(new J.VariableDeclarations.NamedVariable(
                            randomId(),
                            EMPTY,
                            Markers.EMPTY.addIfAbsent(new ReceiverType(randomId())),
                            new J.Identifier(randomId(), EMPTY, Markers.EMPTY, "<receiverType>", null, null),
                            emptyList(),
                            padLeft(EMPTY, receiver),
                            null))
                    .withAfter(sourceBefore("."));
        }

        String methodName;
        if ("<no name provided>".equals(simpleFunction.getName().asString())) {
            // Extract name from source.
            throw new IllegalStateException("Unresolved function.");
        } else {
            methodName = simpleFunction.getName().asString();
        }

        J.Identifier name = createIdentifier(methodName, simpleFunction);

        JContainer<Statement> params;
        Space before = sourceBefore("(");
        params = !simpleFunction.getValueParameters().isEmpty() ?
                JContainer.build(before, convertAll(simpleFunction.getValueParameters(), commaDelim, t -> sourceBefore(")"), ctx, true), Markers.EMPTY) :
                JContainer.build(before, singletonList(padRight(new J.Empty(randomId(), sourceBefore(")"), Markers.EMPTY), EMPTY)), Markers.EMPTY);

        if (simpleFunction.getReceiverTypeRef() != null) {
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

        int saveCursor = cursor;
        TypeTree returnTypeExpression = null;
        before = whitespace();
        if (source.startsWith(":", cursor)) {
            skip(":");
            markers = markers.addIfAbsent(new TypeReferencePrefix(randomId(), before));

            returnTypeExpression = (TypeTree) visitElement(simpleFunction.getReturnTypeRef(), ctx);

            saveCursor = cursor;
            before = whitespace();
            if (source.startsWith("?", cursor)) {
                returnTypeExpression = returnTypeExpression.withMarkers(
                        returnTypeExpression.getMarkers().addIfAbsent(new IsNullable(randomId(), before)));
            } else {
                cursor(saveCursor);
            }
        } else {
            cursor(saveCursor);
        }

        J.Block body;
        saveCursor = cursor;
        before = whitespace();
        if (simpleFunction.getBody() instanceof FirSingleExpressionBlock) {
            if (source.startsWith("=", cursor)) {
                skip("=");
                SingleExpressionBlock singleExpressionBlock = new SingleExpressionBlock(randomId());

                body = convertOrNull(simpleFunction.getBody(), ctx);
                body = body.withPrefix(before);
                body = body.withMarkers(body.getMarkers().addIfAbsent(singleExpressionBlock));
            } else {
                throw new IllegalStateException("Unexpected single block expression, cursor is likely at the wrong position.");
            }
        } else {
            cursor(saveCursor);
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
    public J visitSmartCastExpression(FirSmartCastExpression smartCastExpression, ExecutionContext ctx) {
        return visitElement(smartCastExpression.getOriginalExpression(), ctx);
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
                typeMapping.type(stringConcatenationCall));
    }

    @Nullable
    @Override
    public J visitThisReceiverExpression(FirThisReceiverExpression thisReceiverExpression, ExecutionContext ctx) {
        if (thisReceiverExpression.isImplicit()) {
            return null;
        }

        Space prefix = sourceBefore("this");
        J.Identifier label = null;
        if (thisReceiverExpression.getCalleeReference().getLabelName() != null) {
            skip("@");
            label = createIdentifier(thisReceiverExpression.getCalleeReference().getLabelName(), thisReceiverExpression.getCalleeReference().getBoundSymbol().getFir());
        }
        return new K.KThis(randomId(),
                prefix,
                Markers.EMPTY,
                label,
                typeMapping.type(thisReceiverExpression)
        );
    }

    @Override
    public J visitTypeAlias(FirTypeAlias typeAlias, ExecutionContext ctx) {
        Space prefix = whitespace();

        List<J> modifiers = emptyList();
        Markers markers = Markers.EMPTY;

        List<J.Annotation> annotations = mapModifiers(typeAlias.getAnnotations(), "typealias");
        Space aliasPrefix = whitespace();
        J.Annotation aliasAnnotation = new J.Annotation(
                randomId(),
                aliasPrefix,
                Markers.EMPTY.addIfAbsent(new Modifier(randomId())),
                createIdentifier("typealias"),
                JContainer.empty());
        annotations.add(aliasAnnotation);

        List<JRightPadded<J.VariableDeclarations.NamedVariable>> vars = new ArrayList<>(1); // adjust size if necessary
        Space namePrefix = EMPTY;
        String valueName = "";
        if ("<no name provided>".equals(typeAlias.getName().toString())) {
            KtSourceElement sourceElement = typeAlias.getSource();
            if (sourceElement == null) {
                throw new IllegalStateException("Unexpected null source.");
            }
        } else {
            valueName = typeAlias.getName().asString();
        }
        J.Identifier name = createIdentifier(valueName, typeMapping.type(typeAlias.getExpandedTypeRef()), null);

        TypeTree typeExpression = typeAlias.getTypeParameters().isEmpty() ? name : new J.ParameterizedType(
                randomId(),
                name.getPrefix(),
                Markers.EMPTY,
                name.withPrefix(EMPTY),
                JContainer.build(sourceBefore("<"), convertAll(typeAlias.getTypeParameters(), commaDelim, t -> sourceBefore(">"), ctx), Markers.EMPTY),
                null
        );

        // Dimensions do not exist in Kotlin, and array is declared based on the type. I.E., IntArray
        List<JLeftPadded<Space>> dimensionsAfterName = emptyList();

        Space initializerPrefix = sourceBefore("=");
        Expression expr = (Expression) visitElement(typeAlias.getExpandedTypeRef(), ctx);
        JRightPadded<J.VariableDeclarations.NamedVariable> namedVariable = maybeSemicolon(
                new J.VariableDeclarations.NamedVariable(
                        randomId(),
                        EMPTY,
                        Markers.EMPTY,
                        new J.Identifier(
                                randomId(),
                                EMPTY,
                                Markers.EMPTY,
                                "",
                                null,
                                null),
                        dimensionsAfterName,
                        padLeft(initializerPrefix, expr),
                        null
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
                null,
                vars);
    }

    @Override
    public J visitTypeOperatorCall(FirTypeOperatorCall typeOperatorCall, ExecutionContext ctx) {
        Space prefix = whitespace();
        FirExpression expression = typeOperatorCall.getArgumentList().getArguments().get(0);
        Markers markers = Markers.EMPTY;
        boolean includeParentheses = false;
        if (typeOperatorCall.getOperation() == FirOperation.AS && source.startsWith("(", cursor)) {
            skip("(");
            includeParentheses = true;
        }

        Expression element;
        // A when subject expression does not have a target because it's implicit
        if (expression instanceof FirWhenSubjectExpression) {
            element = new J.Empty(randomId(), EMPTY, Markers.EMPTY);
        } else {
            FirElement target = expression instanceof FirSmartCastExpression ?
                    ((FirSmartCastExpression) expression).getOriginalExpression() :
                    expression;
            J j = visitElement(target, ctx);
            if (j instanceof Statement) {
                element = new K.StatementExpression(randomId(), (Statement) j);
            } else {
                element = (Expression) j;
            }
        }

        Space after;
        switch (typeOperatorCall.getOperation()) {
            case IS:
                after = sourceBefore("is");
                break;
            case NOT_IS:
                after = sourceBefore("!is");
                markers = markers.addIfAbsent(new NotIs(randomId()));
                break;
            case AS:
                after = sourceBefore("as");
                break;
            case SAFE_AS:
                after = sourceBefore("as?");
                markers = markers.addIfAbsent(new IsNullable(randomId(), EMPTY));
                break;
            default:
                throw new UnsupportedOperationException("Unsupported type operator " + typeOperatorCall.getOperation().name());
        }

        if (typeOperatorCall.getOperation() == FirOperation.AS || typeOperatorCall.getOperation() == FirOperation.SAFE_AS) {
            if (!includeParentheses) {
                markers = markers.addIfAbsent(new OmitParentheses(randomId()));
            }
            return new J.TypeCast(
                    randomId(),
                    prefix,
                    markers,
                    new J.ControlParentheses<>(
                            randomId(),
                            after,
                            Markers.EMPTY,
                            JRightPadded.build((TypeTree) visitElement(typeOperatorCall.getConversionTypeRef(), ctx))
                                    .withAfter(includeParentheses ? sourceBefore(")") : EMPTY)),
                    element);
        } else {
            JRightPadded<Expression> expr = JRightPadded.build(element).withAfter(after);

            J clazz = visitElement(typeOperatorCall.getConversionTypeRef(), ctx);
            J pattern = null;
            return new J.InstanceOf(
                    randomId(),
                    prefix,
                    markers,
                    expr,
                    clazz,
                    pattern,
                    typeMapping.type(typeOperatorCall));
        }
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


        List<FirTypeRef> nonImplicitParams = new ArrayList<>(typeParameter.getBounds().size());
        boolean hasImplicitAny = false;
        for (FirTypeRef bound : typeParameter.getBounds()) {
            if (bound instanceof FirImplicitNullableAnyTypeRef) {
                hasImplicitAny = true;
            } else {
                nonImplicitParams.add(bound);
            }
        }

        // Generate a J.WildCard if there is an implicit any bound.
        if (hasImplicitAny && (source.startsWith("in", cursor) || source.startsWith("out", cursor))) {
            J.Wildcard.Bound bound;
            if (source.startsWith("in", cursor)) {
                skip("in");
                bound = J.Wildcard.Bound.Super;
            } else {
                skip("out");
                bound = J.Wildcard.Bound.Extends;
            }
            NameTree name = createIdentifier(typeParameter.getName().asString(), typeParameter);
            return new J.Wildcard(
                    randomId(),
                    prefix,
                    Markers.EMPTY,
                    padLeft(EMPTY, bound),
                    name
            );
        } else {
            Expression name = createIdentifier(typeParameter.getName().asString(), typeParameter);

            JContainer<TypeTree> bounds = null;
            if (nonImplicitParams.size() == 1) {
                bounds = JContainer.build(sourceBefore(":"),
                        convertAll(nonImplicitParams, t -> sourceBefore(","), noDelim, ctx, true), Markers.EMPTY);
            }

            return new J.TypeParameter(
                    randomId(),
                    prefix,
                    markers,
                    annotations,
                    name,
                    bounds);
        }
    }

    @Override
    public J visitTryExpression(FirTryExpression tryExpression, ExecutionContext ctx) {
        Space prefix = whitespace();
        skip("try");

        JContainer<J.Try.Resource> resources = null;
        J.Block block = (J.Block) visitElement(tryExpression.getTryBlock(), ctx);
        List<J.Try.Catch> catches = new ArrayList<>(tryExpression.getCatches().size());
        for (FirCatch aCatch : tryExpression.getCatches()) {
            catches.add((J.Try.Catch) visitElement(aCatch, ctx));
        }

        JLeftPadded<J.Block> finally_ = tryExpression.getFinallyBlock() == null ? null :
                padLeft(sourceBefore("finally"), (J.Block) visitElement(tryExpression.getFinallyBlock(), ctx));

        return new J.Try(randomId(),
                prefix,
                Markers.EMPTY,
                resources,
                block,
                catches,
                finally_);
    }

    @Override
    public J visitTypeProjectionWithVariance(FirTypeProjectionWithVariance typeProjectionWithVariance, ExecutionContext ctx) {
        return visitResolvedTypeRef((FirResolvedTypeRef) typeProjectionWithVariance.getTypeRef(), ctx);
    }

    @Override
    public J visitUserTypeRef(FirUserTypeRef userTypeRef, ExecutionContext ctx) {
        Space prefix = whitespace();
        Markers markers = Markers.EMPTY;
        StringBuilder name = new StringBuilder();
        List<FirQualifierPart> qualifier = userTypeRef.getQualifier();
        for (int i = 0; i < qualifier.size(); i++) {
            FirQualifierPart part = qualifier.get(i);
            Space whitespace = whitespace();
            name.append(whitespace.getWhitespace());
            name.append(part.getName().asString());
            skip(part.getName().asString());
            if (i < qualifier.size() - 1) {
                if (!part.getTypeArgumentList().getTypeArguments().isEmpty()) {
                    throw new IllegalArgumentException("Unsupported type parameters in user part " + part.getName());
                }
                name.append(whitespace().getWhitespace());
                name.append(".");
                skip(".");
            }
        }

        NameTree nameTree = TypeTree.build(name.toString());
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
                    JContainer.build(typeArgPrefix, parameters, Markers.EMPTY),
                    typeMapping.type(userTypeRef));
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
        Markers markers = Markers.EMPTY;

        List<J.Annotation> annotations = mapModifiers(valueParameter.getAnnotations(), valueParameter.getName().asString());

        List<JRightPadded<J.VariableDeclarations.NamedVariable>> vars = new ArrayList<>(1); // adjust size if necessary
        Space namePrefix = EMPTY;
        String valueName = "";
        if ("<no name provided>".equals(valueParameter.getName().toString())) {
            KtSourceElement sourceElement = valueParameter.getSource();
            if (sourceElement == null) {
                throw new IllegalStateException("Unexpected null source.");
            }
        } else if ("<unused var>".equals(valueParameter.getName().toString())) {
            valueName = "_";
            namePrefix = whitespace();
        } else {
            valueName = valueParameter.getName().asString();
            namePrefix = whitespace();
        }
        J.Identifier name = createIdentifier(valueName, valueParameter);

        TypeTree typeExpression = null;
        if (valueParameter.getReturnTypeRef() instanceof FirResolvedTypeRef && (valueParameter.getReturnTypeRef().getSource() == null || !(valueParameter.getReturnTypeRef().getSource().getKind() instanceof KtFakeSourceElementKind))) {
            FirResolvedTypeRef typeRef = (FirResolvedTypeRef) valueParameter.getReturnTypeRef();
            if (typeRef.getDelegatedTypeRef() != null) {
                Space delimiterPrefix = whitespace();
                boolean addTypeReferencePrefix = source.startsWith(":", cursor);
                skip(":");
                if (addTypeReferencePrefix) {
                    markers = markers.addIfAbsent(new TypeReferencePrefix(randomId(), delimiterPrefix));
                }
                J j = visitElement(typeRef, ctx);
                if (j instanceof TypeTree) {
                    typeExpression = (TypeTree) j;
                } else {
                    typeExpression = new K.FunctionType(randomId(), (TypedTree) j, null, null);
                }
            } else if ("_".equals(valueName)) {
                int savedCursor = cursor;
                Space delimiterPrefix = whitespace();
                if (source.startsWith(":", cursor)) {
                    skip(":");
                    markers = markers.addIfAbsent(new TypeReferencePrefix(randomId(), delimiterPrefix));
                    J j = visitElement(typeRef, ctx);
                    if (j instanceof TypeTree) {
                        typeExpression = (TypeTree) j;
                    } else {
                        typeExpression = new K.FunctionType(randomId(), (TypedTree) j, null, null);
                    }
                } else {
                    cursor = savedCursor;
                }
            }
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
                        initializer != null ? padLeft(sourceBefore("="), (Expression) visitElement(initializer, ctx)) : null,
                        typeMapping.variableType(valueParameter.getSymbol(), null, getCurrentFile())
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
    public J visitVariableAssignment(FirVariableAssignment variableAssignment, ExecutionContext ctx) {
        boolean unaryAssignment = variableAssignment.getRValue() instanceof FirFunctionCall &&
                ((FirFunctionCall) variableAssignment.getRValue()).getOrigin() == FirFunctionCallOrigin.Operator &&
                ((FirFunctionCall) variableAssignment.getRValue()).getCalleeReference() instanceof FirResolvedNamedReference &&
                isUnaryOperation((((FirFunctionCall) variableAssignment.getRValue()).getCalleeReference()).getName().asString());

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
                        typeMapping.type(variableAssignment, getCurrentFile()));
            } else {
                variable = convert(variableAssignment.getLValue(), ctx);
            }

            int saveCursor = cursor;
            whitespace();
            boolean isCompoundAssignment = source.startsWith("-=", cursor) ||
                    source.startsWith("+=", cursor) ||
                    source.startsWith("*=", cursor) ||
                    source.startsWith("/=", cursor);
            cursor(saveCursor);

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
                        ((FirFunctionCall) variableAssignment.getRValue()).getArgumentList().getArguments().size() != 1) {
                    throw new IllegalArgumentException("Unexpected compound assignment.");
                }

                FirElement rhs = ((FirFunctionCall) variableAssignment.getRValue()).getArgumentList().getArguments().get(0);
                return new J.AssignmentOperation(
                        randomId(),
                        prefix,
                        Markers.EMPTY,
                        variable,
                        padLeft(opPrefix, op),
                        (Expression) visitElement(rhs, ctx),
                        typeMapping.type(variableAssignment));
            } else {
                Space exprPrefix = sourceBefore("=");
                J j = convert(variableAssignment.getRValue(), ctx);
                if (j instanceof Statement && !(j instanceof Expression)) {
                    j = new K.StatementExpression(randomId(), (Statement) j);
                }
                Expression expr = (Expression) j;
                return new J.Assignment(
                        randomId(),
                        prefix,
                        Markers.EMPTY,
                        variable,
                        padLeft(exprPrefix, expr),
                        typeMapping.type(variableAssignment));
            }
        }
    }

    @Override
    public J visitWhenBranch(FirWhenBranch whenBranch, ExecutionContext ctx) {
        Space prefix = whitespace();
        if (source.substring(cursor).startsWith("if")) {
            skip("if");
        } else if (!(whenBranch.getCondition() instanceof FirElseIfTrueCondition ||
                whenBranch.getCondition() instanceof FirEqualityOperatorCall)) {
            throw new IllegalArgumentException("Unsupported condition type.");
        }

        boolean singleExpression = whenBranch.getResult() instanceof FirSingleExpressionBlock;
        if (whenBranch.getCondition() instanceof FirElseIfTrueCondition) {
            FirElement result = singleExpression ? ((FirSingleExpressionBlock) whenBranch.getResult()).getStatement() : whenBranch.getResult();
            J j = visitElement(result, ctx);
            return j.withPrefix(prefix);
        } else {
            J.ControlParentheses<Expression> controlParentheses = mapControlParentheses(whenBranch.getCondition());

            FirElement result = singleExpression ? ((FirSingleExpressionBlock) whenBranch.getResult()).getStatement() : whenBranch.getResult();
            J j = convert(result, ctx);
            if (!(j instanceof Statement) && j instanceof Expression) {
                j = new K.ExpressionStatement(randomId(), (Expression) j);
            }
            return new J.If(
                    randomId(),
                    prefix,
                    Markers.EMPTY,
                    controlParentheses,
                    JRightPadded.build((Statement) j),
                    null);
        }
    }

    @Override
    public J visitWhenExpression(FirWhenExpression whenExpression, ExecutionContext ctx) {
        int saveCursor = cursor;
        Space prefix = whitespace();
        if (source.startsWith("when", cursor)) {
            // Create the entire when expression here to simplify visiting `WhenBranch`, since `if` and `when` share the same data structure.
            skip("when");

            J.ControlParentheses<Expression> controlParentheses = null;
            if (whenExpression.getSubject() != null) {
                controlParentheses = new J.ControlParentheses<>(
                        randomId(),
                        sourceBefore("("),
                        Markers.EMPTY,
                        padRight((Expression) visitElement(whenExpression.getSubject(), ctx), sourceBefore(")"))
                );
            }

            Space bodyPrefix = sourceBefore("{");
            List<JRightPadded<Statement>> statements = new ArrayList<>(whenExpression.getBranches().size());

            for (FirWhenBranch whenBranch : whenExpression.getBranches()) {
                int exprSize = whenBranch.getCondition() instanceof FirEqualityOperatorCall ?
                        ((FirEqualityOperatorCall) whenBranch.getCondition()).getArgumentList().getArguments().size() - 1 : 1;
                List<JRightPadded<Expression>> expressions = new ArrayList<>(exprSize);

                if (whenBranch.getCondition() instanceof FirElseIfTrueCondition) {
                    expressions.add(padRight(createIdentifier("else"), sourceBefore("->")));
                } else if (whenBranch.getCondition() instanceof FirEqualityOperatorCall) {
                    List<FirExpression> arguments = new ArrayList<>(((FirEqualityOperatorCall) whenBranch.getCondition()).getArgumentList().getArguments().size());
                    for (FirExpression argument : ((FirEqualityOperatorCall) whenBranch.getCondition()).getArgumentList().getArguments()) {
                        if (!(argument instanceof FirWhenSubjectExpression)) {
                            arguments.add(argument);
                        }
                    }

                    if (arguments.size() == 1) {
                        expressions.add(padRight((Expression) visitElement(arguments.get(0), ctx), sourceBefore("->")));
                    } else {
                        Expression expr = (Expression) visitElement(whenBranch.getCondition(), ctx);
                        expressions.add(padRight(expr, sourceBefore("->")));
                    }
                } else {
                    expressions.add(padRight((Expression) visitElement(whenBranch.getCondition(), ctx), sourceBefore("->")));
                }

                JContainer<Expression> expressionContainer = JContainer.build(EMPTY, expressions, Markers.EMPTY);

                J body = visitElement(whenBranch.getResult(), ctx);
                K.WhenBranch branch = new K.WhenBranch(
                        randomId(),
                        EMPTY,
                        Markers.EMPTY,
                        expressionContainer,
                        padRight(body, EMPTY));

                statements.add(padRight(branch, EMPTY));
            }
            Space bodySuffix = sourceBefore("}");
            J.Block body = new J.Block(
                    randomId(),
                    bodyPrefix,
                    Markers.EMPTY,
                    new JRightPadded<>(false, EMPTY, Markers.EMPTY),
                    statements,
                    bodySuffix);

            return new K.When(
                    randomId(),
                    prefix,
                    Markers.EMPTY,
                    controlParentheses,
                    body,
                    typeMapping.type(whenExpression));
        }

        // Otherwise, create an if branch.
        cursor(saveCursor);

        FirWhenBranch whenBranch = whenExpression.getBranches().get(0);
        J firstElement = visitElement(whenBranch, ctx);
        if (!(firstElement instanceof J.If)) {
            throw new IllegalStateException("First element of when expression was not an if.");
        }

        J.If ifStatement = (J.If) firstElement;

        List<J> elses = new ArrayList<>(whenExpression.getBranches().size() - 1);
        List<FirWhenBranch> branches = whenExpression.getBranches();
        for (int i = 1; i < branches.size(); i++) {
            FirWhenBranch branch = branches.get(i);

            Space elsePrefix = sourceBefore("else");
            J j = visitWhenBranch(branch, ctx);
            if (!(j instanceof Statement) && j instanceof Expression) {
                j = new K.ExpressionStatement(randomId(), (Expression) j);
            }
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

        J.Label label = null;
        if (whileLoop.getLabel() != null) {
            label = (J.Label) visitElement(whileLoop.getLabel(), ctx);
        }

        Space prefix = whitespace();
        skip("while");
        J.ControlParentheses<Expression> controlParentheses = mapControlParentheses(whileLoop.getCondition());
        Statement body = (Statement) visitElement(whileLoop.getBlock(), ctx);
        J.WhileLoop statement = new J.WhileLoop(
                randomId(),
                prefix,
                Markers.EMPTY,
                controlParentheses,
                JRightPadded.build(body));

        return label != null ? label.withStatement(statement) : statement;
    }

    @Override
    public J visitArgumentList(FirArgumentList argumentList, ExecutionContext ctx) {
        throw new UnsupportedOperationException(generateUnsupportedMessage("FirArgumentList"));
    }

    @Override
    public J visitAugmentedArraySetCall(FirAugmentedArraySetCall augmentedArraySetCall, ExecutionContext ctx) {
        throw new UnsupportedOperationException(generateUnsupportedMessage("FirAugmentedArraySetCall"));
    }

    @Override
    public J visitAssignmentOperatorStatement(FirAssignmentOperatorStatement assignmentOperatorStatement, ExecutionContext ctx) {
        throw new UnsupportedOperationException(generateUnsupportedMessage("FirAssignmentOperatorStatement"));
    }

    @Override
    public J visitAnnotation(FirAnnotation annotation, ExecutionContext ctx) {
        throw new UnsupportedOperationException(generateUnsupportedMessage("FirAnnotation"));
    }

    @Override
    public J visitAnnotationContainer(FirAnnotationContainer annotationContainer, ExecutionContext ctx) {
        throw new UnsupportedOperationException(generateUnsupportedMessage("FirAnnotationContainer"));
    }

    @Override
    public J visitAnnotationArgumentMapping(FirAnnotationArgumentMapping annotationArgumentMapping, ExecutionContext ctx) {
        throw new UnsupportedOperationException(generateUnsupportedMessage("FirAnnotationArgumentMapping"));
    }

    @Override
    public J visitBackingField(FirBackingField backingField, ExecutionContext ctx) {
        throw new UnsupportedOperationException(generateUnsupportedMessage("FirBackingField"));
    }

    @Override
    public J visitContextReceiver(FirContextReceiver contextReceiver, ExecutionContext ctx) {
        throw new UnsupportedOperationException(generateUnsupportedMessage("FirContextReceiver"));
    }

    @Override
    public J visitConstructor(FirConstructor constructor, ExecutionContext ctx) {
        Space prefix = whitespace();
        Markers markers = Markers.EMPTY;
        List<J> modifiers = emptyList();
        List<J.Annotation> annotations = mapModifiers(constructor.getAnnotations(), "constructor");

        JRightPadded<J.VariableDeclarations.NamedVariable> infixReceiver = null;
        if (constructor.getReceiverTypeRef() != null) {
            // Infix functions are de-sugared during the backend phase of the compiler.
            // The de-sugaring process moves the infix receiver to the first position of the method declaration.
            // The infix receiver is added as to the `J.MethodInvocation` parameters, and marked to distinguish the parameter.
            markers = markers.addIfAbsent(new ReceiverType(randomId()));
            Expression receiver = (Expression) visitElement(constructor.getReceiverTypeRef(), ctx);
            infixReceiver = JRightPadded.build(new J.VariableDeclarations.NamedVariable(
                            randomId(),
                            EMPTY,
                            Markers.EMPTY.addIfAbsent(new ReceiverType(randomId())),
                            new J.Identifier(randomId(), EMPTY, Markers.EMPTY, "<receiverType>", null, null),
                            emptyList(),
                            padLeft(EMPTY, receiver),
                            null))
                    .withAfter(sourceBefore("."));
        }

        String methodName = "constructor";

        J.Identifier name = createIdentifier(methodName, constructor);

        JContainer<Statement> params;
        Space before = sourceBefore("(");
        params = !constructor.getValueParameters().isEmpty() ?
                JContainer.build(before, convertAll(constructor.getValueParameters(), commaDelim, t -> sourceBefore(")"), ctx, true), Markers.EMPTY) :
                JContainer.build(before, singletonList(padRight(new J.Empty(randomId(), sourceBefore(")"), Markers.EMPTY), EMPTY)), Markers.EMPTY);

        if (constructor.getReceiverTypeRef() != null) {
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

        int saveCursor = cursor;
        TypeTree returnTypeExpression = null;
        before = whitespace();
        if (source.startsWith(":", cursor)) {
            skip(":");
            markers = markers.addIfAbsent(new TypeReferencePrefix(randomId(), before));

            if (constructor.getDelegatedConstructor() != null &&
                    (constructor.getDelegatedConstructor().isThis() || constructor.getDelegatedConstructor().isSuper())) {
                Space thisPrefix = whitespace();
                // The delegate constructor call is de-sugared during the backend phase of the compiler.
                TypeTree delegateName = createIdentifier(constructor.getDelegatedConstructor().isThis() ? "this" : "super");
                JContainer<Expression> args = mapFunctionalCallArguments(constructor.getDelegatedConstructor().getArgumentList().getArguments()).withBefore(before);

                JavaType type = typeMapping.type(constructor);
                J.NewClass newClass = new J.NewClass(
                        randomId(),
                        thisPrefix,
                        Markers.EMPTY,
                        null,
                        EMPTY,
                        delegateName,
                        args,
                        null,
                        type instanceof JavaType.Method ? (JavaType.Method) type : null
                );
                returnTypeExpression = new K.FunctionType(randomId(), newClass, null, null);
            } else {
                returnTypeExpression = (TypeTree) visitElement(constructor.getReturnTypeRef(), ctx);
            }

            saveCursor = cursor;
            before = whitespace();
            if (source.startsWith("?", cursor)) {
                returnTypeExpression = returnTypeExpression.withMarkers(
                        returnTypeExpression.getMarkers().addIfAbsent(new IsNullable(randomId(), before)));
            } else {
                cursor(saveCursor);
            }
        } else {
            cursor(saveCursor);
        }

        J.Block body;
        saveCursor = cursor;
        before = whitespace();
        if (constructor.getBody() instanceof FirSingleExpressionBlock) {
            if (source.startsWith("=", cursor)) {
                skip("=");
                SingleExpressionBlock singleExpressionBlock = new SingleExpressionBlock(randomId());

                body = convertOrNull(constructor.getBody(), ctx);
                body = body.withPrefix(before);
                body = body.withMarkers(body.getMarkers().addIfAbsent(singleExpressionBlock));
            } else {
                throw new IllegalStateException("Unexpected single block expression.");
            }
        } else {
            cursor(saveCursor);
            body = convertOrNull(constructor.getBody(), ctx);
        }

        return new J.MethodDeclaration(
                randomId(),
                prefix,
                markers,
                annotations,
                emptyList(),
                null,
                returnTypeExpression,
                new J.MethodDeclaration.IdentifierWithAnnotations(name, emptyList()),
                params,
                null,
                body,
                null,
                typeMapping.methodDeclarationType(constructor, null, getCurrentFile()));
    }

    @Override
    public J visitComponentCall(FirComponentCall componentCall, ExecutionContext ctx) {
        throw new UnsupportedOperationException(generateUnsupportedMessage("FirComponentCall"));
    }

    @Override
    public J visitContractDescriptionOwner(FirContractDescriptionOwner contractDescriptionOwner, ExecutionContext ctx) {
        throw new UnsupportedOperationException(generateUnsupportedMessage("FirContractDescriptionOwner"));
    }

    @Override
    public J visitContextReceiverArgumentListOwner(FirContextReceiverArgumentListOwner contextReceiverArgumentListOwner, ExecutionContext ctx) {
        throw new UnsupportedOperationException(generateUnsupportedMessage("FirContextReceiverArgumentListOwner"));
    }

    @Override
    public J visitClassReferenceExpression(FirClassReferenceExpression classReferenceExpression, ExecutionContext ctx) {
        throw new UnsupportedOperationException(generateUnsupportedMessage("FirClassReferenceExpression"));
    }

    @Override
    public J visitClassLikeDeclaration(FirClassLikeDeclaration classLikeDeclaration, ExecutionContext ctx) {
        throw new UnsupportedOperationException(generateUnsupportedMessage("FirClassLikeDeclaration"));
    }

    @Override
    public J visitCall(FirCall call, ExecutionContext ctx) {
        throw new UnsupportedOperationException(generateUnsupportedMessage("FirCall"));
    }

    @Override
    public J visitCallableDeclaration(FirCallableDeclaration callableDeclaration, ExecutionContext ctx) {
        throw new UnsupportedOperationException(generateUnsupportedMessage("FirCallableDeclaration"));
    }

    @Override
    public J visitDelegatedConstructorCall(FirDelegatedConstructorCall delegatedConstructorCall, ExecutionContext ctx) {
        throw new UnsupportedOperationException(generateUnsupportedMessage("FirDelegatedConstructorCall"));
    }

    @Override
    public J visitDeclaration(FirDeclaration declaration, ExecutionContext ctx) {
        throw new UnsupportedOperationException(generateUnsupportedMessage("FirDeclaration"));
    }

    @Override
    public J visitDynamicTypeRef(FirDynamicTypeRef dynamicTypeRef, ExecutionContext ctx) {
        throw new UnsupportedOperationException(generateUnsupportedMessage("FirDynamicTypeRef"));
    }

    @Override
    public J visitDelegateFieldReference(FirDelegateFieldReference delegateFieldReference, ExecutionContext ctx) {
        throw new UnsupportedOperationException(generateUnsupportedMessage("FirDelegateFieldReference"));
    }

    @Override
    public J visitDeclarationStatus(FirDeclarationStatus declarationStatus, ExecutionContext ctx) {
        throw new UnsupportedOperationException(generateUnsupportedMessage("FirDeclarationStatus"));
    }

    @Override
    public J visitField(FirField field, ExecutionContext ctx) {
        throw new UnsupportedOperationException(generateUnsupportedMessage("FirField"));
    }

    @Override
    public J visitFunction(FirFunction function, ExecutionContext ctx) {
        throw new UnsupportedOperationException(generateUnsupportedMessage("FirFunction"));
    }

    @Override
    public J visitImplicitInvokeCall(FirImplicitInvokeCall implicitInvokeCall, ExecutionContext ctx) {
        throw new UnsupportedOperationException(generateUnsupportedMessage("FirImplicitInvokeCall"));
    }

    @Override
    public J visitImplicitTypeRef(FirImplicitTypeRef implicitTypeRef, ExecutionContext ctx) {
        throw new UnsupportedOperationException(generateUnsupportedMessage("FirImplicitTypeRef"));
    }

    @Override
    public J visitIntegerLiteralOperatorCall(FirIntegerLiteralOperatorCall integerLiteralOperatorCall, ExecutionContext ctx) {
        throw new UnsupportedOperationException(generateUnsupportedMessage("FirIntegerLiteralOperatorCall"));
    }

    @Override
    public J visitIntersectionTypeRef(FirIntersectionTypeRef intersectionTypeRef, ExecutionContext ctx) {
        throw new UnsupportedOperationException(generateUnsupportedMessage("FirIntersectionTypeRef"));
    }

    @Override
    public <E extends FirTargetElement> J visitJump(FirJump<E> jump, ExecutionContext ctx) {
        throw new UnsupportedOperationException(generateUnsupportedMessage("FirJump"));
    }

    @Override
    public J visitLoop(FirLoop loop, ExecutionContext ctx) {
        throw new UnsupportedOperationException(generateUnsupportedMessage("FirLoop"));
    }

    @Override
    public J visitLoopJump(FirLoopJump loopJump, ExecutionContext ctx) {
        throw new UnsupportedOperationException(generateUnsupportedMessage("FirLoopJump"));
    }

    @Override
    public J visitMemberDeclaration(FirMemberDeclaration memberDeclaration, ExecutionContext ctx) {
        throw new UnsupportedOperationException(generateUnsupportedMessage("FirMemberDeclaration"));
    }

    @Override
    public J visitNamedReference(FirNamedReference namedReference, ExecutionContext ctx) {
        throw new UnsupportedOperationException(generateUnsupportedMessage("FirNamedReference"));
    }

    @Override
    public J visitPlaceholderProjection(FirPlaceholderProjection placeholderProjection, ExecutionContext ctx) {
        throw new UnsupportedOperationException(generateUnsupportedMessage("FirPlaceholderProjection"));
    }

    @Override
    public J visitQualifiedAccess(FirQualifiedAccess qualifiedAccess, ExecutionContext ctx) {
        throw new UnsupportedOperationException(generateUnsupportedMessage("FirQualifiedAccess"));
    }

    @Override
    public J visitQualifiedAccessExpression(FirQualifiedAccessExpression qualifiedAccessExpression, ExecutionContext ctx) {
        throw new UnsupportedOperationException(generateUnsupportedMessage("FirQualifiedAccessExpression"));
    }

    @Override
    public J visitReference(FirReference reference, ExecutionContext ctx) {
        throw new UnsupportedOperationException(generateUnsupportedMessage("FirReference"));
    }

    @Override
    public J visitRegularClass(FirRegularClass regularClass, ExecutionContext ctx) {
        Space prefix = whitespace();
        Markers markers = Markers.EMPTY;

        List<J.Modifier> modifiers = emptyList();
        PsiElement currentNode = getCurrentPsiNode();
        if (currentNode instanceof KtAnnotationEntry) {
            currentNode = PsiTreeUtil.getParentOfType(currentNode, KtModifierList.class);
        }

        List<J.Annotation> kindAnnotations = new ArrayList<>();
        if (currentNode instanceof KtModifierList) {
            modifiers = mapModifierList((KtModifierList) currentNode, regularClass.getAnnotations(), kindAnnotations);
        }

        ClassKind classKind = regularClass.getClassKind();

        J.ClassDeclaration.Kind kind;
        if (ClassKind.INTERFACE == classKind) {
            kind = new J.ClassDeclaration.Kind(randomId(), sourceBefore("interface"), Markers.EMPTY, kindAnnotations, J.ClassDeclaration.Kind.Type.Interface);
        } else if (ClassKind.OBJECT == classKind) {
            markers = markers.addIfAbsent(new KObject(randomId(), EMPTY));
            kind = new J.ClassDeclaration.Kind(randomId(), sourceBefore("object"), Markers.EMPTY, kindAnnotations, J.ClassDeclaration.Kind.Type.Class);
        } else {
            // Enums and Interfaces are modifiers in kotlin and require the modifier prefix to preserve source code.
            kind = new J.ClassDeclaration.Kind(randomId(), sourceBefore("class"), Markers.EMPTY, kindAnnotations, J.ClassDeclaration.Kind.Type.Class);
        }

        J.Identifier name;
        if (ClassKind.OBJECT == classKind && kindAnnotations.stream().anyMatch(a -> a.getAnnotationType() instanceof J.Identifier && "companion".equals(((J.Identifier) a.getAnnotationType()).getSimpleName()))) {
            name = new J.Identifier(
                    randomId(),
                    EMPTY,
                    Markers.EMPTY,
                    "",
                    null,
                    null
            );
        } else {
            name = createIdentifier(regularClass.getName().asString(), regularClass);
        }

        // KotlinTypeParameters with multiple bounds are defined outside the TypeParameter container.
        // KotlinTypeGoat<T, S> where S: A, T: B, S: C, T: D.
        // The order the bounds exist in T and S will be based on the declaration order.
        // However, each bound may be declared in any order T -> S -> T -> S.
        JContainer<J.TypeParameter> typeParams = regularClass.getTypeParameters().isEmpty() ? null : JContainer.build(
                sourceBefore("<"),
                convertAll(regularClass.getTypeParameters(), commaDelim, t -> sourceBefore(">"), ctx),
                Markers.EMPTY);

        List<FirElement> membersMultiVariablesSeparated = new ArrayList<>(regularClass.getDeclarations().size());
        List<FirDeclaration> jcEnums = new ArrayList<>(regularClass.getDeclarations().size());
        FirPrimaryConstructor firPrimaryConstructor = null;
        for (FirDeclaration declaration : regularClass.getDeclarations()) {
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

        List<J.Annotation> leadingAnnotation = new ArrayList<>();
        int saveCursor = cursor;
        Space before = whitespace();
        JContainer<Statement> primaryConstructor = null;
        boolean inlineConstructor = (source.startsWith("internal", cursor) || source.startsWith("constructor", cursor) || source.startsWith("(", cursor)) && firPrimaryConstructor != null;
        if (inlineConstructor) {
            if (source.startsWith("internal", cursor)) {
                cursor = saveCursor;
                J.Annotation annotation = mapKModifierToAnnotation("internal");
                annotation = annotation.withMarkers(annotation.getMarkers().addIfAbsent(new ExplicitInlineConstructor(randomId())));
                annotation = annotation.withMarkers(annotation.getMarkers().addIfAbsent(new Modifier(randomId())));
                leadingAnnotation.add(annotation);
                before = whitespace();
            }

            if (source.startsWith("constructor", cursor)) {
                J.Annotation annotation = new J.Annotation(randomId(), before, Markers.EMPTY, createIdentifier("constructor", regularClass), null);
                annotation = annotation.withMarkers(annotation.getMarkers().addIfAbsent(new ExplicitInlineConstructor(randomId())));
                annotation = annotation.withMarkers(annotation.getMarkers().addIfAbsent(new Modifier(randomId())));
                leadingAnnotation.add(annotation);
                before = whitespace();
            }
            skip("(");
            primaryConstructor = JContainer.build(before,
                    firPrimaryConstructor.getValueParameters().isEmpty() ?
                            singletonList(padRight(new J.Empty(randomId(), sourceBefore(")"), Markers.EMPTY), EMPTY)) :
                            convertAll(firPrimaryConstructor.getValueParameters(), commaDelim, t -> sourceBefore(")"), ctx, true), Markers.EMPTY);
        } else {
            cursor(saveCursor);
        }

        JContainer<TypeTree> implementings = null;
        List<JRightPadded<TypeTree>> superTypes = null;

        saveCursor = cursor;
        before = whitespace();
        if (source.startsWith(":", cursor)) {
            skip(":");
        }

        // Kotlin declared super class and interfaces differently than java. All types declared after the `:` are added into implementings.
        // This should probably exist on a K.ClassDeclaration view where the getters return the appropriate types.
        // The J.ClassDeclaration should have the super type set in extending and the J.NewClass should be unwrapped.
        for (int i = 0; i < regularClass.getSuperTypeRefs().size(); i++) {
            FirTypeRef typeRef = regularClass.getSuperTypeRefs().get(i);
            FirRegularClassSymbol symbol = TypeUtilsKt.toRegularClassSymbol(FirTypeUtilsKt.getConeType(typeRef), firSession);
            // Filter out generated types.
            if (typeRef.getSource() != null && !(typeRef.getSource().getKind() instanceof KtFakeSourceElementKind)) {
                if (superTypes == null) {
                    superTypes = new ArrayList<>(regularClass.getSuperTypeRefs().size());
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
                    element = new K.FunctionType(randomId(), newClass, null, null);
                }
                superTypes.add(JRightPadded.build(element)
                        .withAfter(i == regularClass.getSuperTypeRefs().size() - 1 ? EMPTY : sourceBefore(",")));
            }
        }

        if (superTypes == null) {
            cursor(saveCursor);
        } else {
            implementings = JContainer.build(before, superTypes, Markers.EMPTY);
        }

        saveCursor = cursor;
        Space bodyPrefix = whitespace();

        OmitBraces omitBraces;
        J.Block body;
        if (source.substring(cursor).isEmpty() || !source.substring(cursor).startsWith("{")) {
            cursor(saveCursor);
            omitBraces = new OmitBraces(randomId());
            body = new J.Block(randomId(), bodyPrefix, Markers.EMPTY, new JRightPadded<>(false, EMPTY, Markers.EMPTY), emptyList(), Space.EMPTY);
            body = body.withMarkers(body.getMarkers().addIfAbsent(omitBraces));
        } else {
            skip("{");

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
                    if (firElement.getSource() != null && firElement.getSource().getKind() instanceof KtFakeSourceElementKind) {
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
                markers,
                leadingAnnotation,
                modifiers,
                kind,
                name,
                typeParams,
                primaryConstructor,
                null,
                implementings,
                null,
                body,
                (JavaType.FullyQualified) typeMapping.type(regularClass));
    }

    private List<J.Modifier> mapModifierList(KtModifierList currentNode, List<FirAnnotation> annotations, List<J.Annotation> lastAnnotations) {
        Map<Integer, FirAnnotation> annotationsMap = new HashMap<>();
        for (FirAnnotation annotation : annotations) {
            annotationsMap.put(annotation.getSource().getStartOffset(), annotation);
        }

        List<J.Modifier> modifiers = new ArrayList<>();
        List<J.Annotation> currentAnnotations = new ArrayList<>();
        Iterator<PsiElement> iterator = PsiUtilsKt.getAllChildren(currentNode).iterator();
        while (iterator.hasNext()) {
            PsiElement it = iterator.next();
            if (it instanceof LeafPsiElement && it.getNode().getElementType() instanceof KtModifierKeywordToken) {
                if (isJModifier(it.getText())) {
                    modifiers.add(mapToJModifier(it.getText(), currentAnnotations));
                    currentAnnotations = new ArrayList<>();
                } else {
                    currentAnnotations.add(mapKModifierToAnnotation(it.getText()));
                }
            } else if (it instanceof KtAnnotationEntry) {
                if (annotationsMap.containsKey(it.getTextRange().getStartOffset())) {
                    J.Annotation annotation = (J.Annotation) visitElement(annotationsMap.get(it.getTextRange().getStartOffset()), null);
                    currentAnnotations.add(annotation);
                } else {
                    throw new UnsupportedOperationException("Annotation not found");
                }
            }
        }

        if (!currentAnnotations.isEmpty()) {
            lastAnnotations.addAll(currentAnnotations);
        }

        return modifiers;
    }

    @Override
    public J visitResolvable(FirResolvable resolvable, ExecutionContext ctx) {
        throw new UnsupportedOperationException(generateUnsupportedMessage("FirResolvable"));
    }

    @Override
    public J visitResolvedCallableReference(FirResolvedCallableReference resolvedCallableReference, ExecutionContext ctx) {
        throw new UnsupportedOperationException(generateUnsupportedMessage("FirResolvedCallableReference"));
    }

    @Override
    public J visitResolvedDeclarationStatus(FirResolvedDeclarationStatus resolvedDeclarationStatus, ExecutionContext ctx) {
        throw new UnsupportedOperationException(generateUnsupportedMessage("FirResolvedDeclarationStatus"));
    }

    @Override
    public J visitResolvedImport(FirResolvedImport resolvedImport, ExecutionContext ctx) {
        throw new UnsupportedOperationException(generateUnsupportedMessage("FirResolvedImport"));
    }

    @Override
    public J visitSpreadArgumentExpression(FirSpreadArgumentExpression spreadArgumentExpression, ExecutionContext ctx) {
        if (!spreadArgumentExpression.isSpread()) {
            // A spread argument without a spread operator?
            throw new UnsupportedOperationException("Only spread arguments are supported");
        }
        Space prefix = whitespace();
        skip("*");
        J j = visitElement(spreadArgumentExpression.getExpression(), ctx);
        return j.withMarkers(j.getMarkers().addIfAbsent(new SpreadArgument(randomId(), prefix)));
    }

    @Override
    public J visitTypeRef(FirTypeRef typeRef, ExecutionContext ctx) {
        throw new UnsupportedOperationException(generateUnsupportedMessage("FirTypeRef"));
    }

    @Override
    public J visitTargetElement(FirTargetElement targetElement, ExecutionContext ctx) {
        throw new UnsupportedOperationException(generateUnsupportedMessage("FirTargetElement"));
    }

    @Override
    public J visitThisReference(FirThisReference thisReference, ExecutionContext ctx) {
        throw new UnsupportedOperationException(generateUnsupportedMessage("FirThisReference"));
    }

    @Override
    public J visitThrowExpression(FirThrowExpression throwExpression, ExecutionContext ctx) {
        Space prefix = whitespace();
        skip("throw");
        return new J.Throw(
                randomId(),
                prefix,
                Markers.EMPTY,
                (Expression) visitElement(throwExpression.getException(), ctx)
        );
    }

    @Override
    public J visitTypeParameterRef(FirTypeParameterRef typeParameterRef, ExecutionContext ctx) {
        throw new UnsupportedOperationException(generateUnsupportedMessage("FirTypeParameterRef"));
    }

    @Override
    public J visitTypeParameterRefsOwner(FirTypeParameterRefsOwner typeParameterRefsOwner, ExecutionContext ctx) {
        throw new UnsupportedOperationException(generateUnsupportedMessage("FirTypeParameterRefsOwner"));
    }

    @Override
    public J visitTypeParametersOwner(FirTypeParametersOwner typeParametersOwner, ExecutionContext ctx) {
        throw new UnsupportedOperationException(generateUnsupportedMessage("FirTypeParametersOwner"));
    }

    @Override
    public J visitTypeProjection(FirTypeProjection typeProjection, ExecutionContext ctx) {
        throw new UnsupportedOperationException(generateUnsupportedMessage("FirTypeProjection"));
    }

    @Override
    public J visitTypeRefWithNullability(FirTypeRefWithNullability typeRefWithNullability, ExecutionContext ctx) {
        throw new UnsupportedOperationException(generateUnsupportedMessage("FirTypeRefWithNullability"));
    }

    @Override
    public J visitVarargArgumentsExpression(FirVarargArgumentsExpression varargArgumentsExpression, ExecutionContext ctx) {
        throw new UnsupportedOperationException(generateUnsupportedMessage("FirVarargArgumentsExpression"));
    }

    @Override
    public J visitWhenSubjectExpression(FirWhenSubjectExpression whenSubjectExpression, ExecutionContext ctx) {
        throw new UnsupportedOperationException(generateUnsupportedMessage("FirWhenSubjectExpression"));
    }

    @Override
    public J visitWrappedArgumentExpression(FirWrappedArgumentExpression wrappedArgumentExpression, ExecutionContext ctx) {
        throw new UnsupportedOperationException(generateUnsupportedMessage("FirWrappedArgumentExpression"));
    }

    @Override
    public J visitWrappedDelegateExpression(FirWrappedDelegateExpression wrappedDelegateExpression, ExecutionContext ctx) {
        throw new UnsupportedOperationException(generateUnsupportedMessage("FirWrappedDelegateExpression"));
    }

    @Override
    public J visitWrappedExpression(FirWrappedExpression wrappedExpression, ExecutionContext ctx) {
        throw new UnsupportedOperationException(generateUnsupportedMessage("FirWrappedExpression"));
    }

    public J visitNoReceiverExpression(FirNoReceiverExpression noReceiverExpression, ExecutionContext ctx) {
        throw new UnsupportedOperationException(generateUnsupportedMessage("FirNoReceiverExpression"));
    }

    /* Error element visits */

    @Override
    public J visitErrorExpression(FirErrorExpression errorExpression, ExecutionContext ctx) {
        throw new UnsupportedOperationException(generateUnsupportedMessage("FirErrorExpression"));
    }

    @Override
    public J visitErrorFunction(FirErrorFunction errorFunction, ExecutionContext ctx) {
        throw new UnsupportedOperationException(generateUnsupportedMessage("FirErrorFunction"));
    }

    @Override
    public J visitErrorImport(FirErrorImport errorImport, ExecutionContext ctx) {
        throw new UnsupportedOperationException(generateUnsupportedMessage("FirErrorImport"));
    }

    @Override
    public J visitErrorLoop(FirErrorLoop errorLoop, ExecutionContext ctx) {
        throw new UnsupportedOperationException(generateUnsupportedMessage("FirErrorLoop"));
    }

    @Override
    public J visitErrorProperty(FirErrorProperty errorProperty, ExecutionContext ctx) {
        throw new UnsupportedOperationException(generateUnsupportedMessage("FirErrorProperty"));
    }

    @Override
    public J visitErrorResolvedQualifier(FirErrorResolvedQualifier errorResolvedQualifier, ExecutionContext ctx) {
        throw new UnsupportedOperationException(generateUnsupportedMessage("FirErrorResolvedQualifier"));
    }

    @Override
    public J visitErrorTypeRef(FirErrorTypeRef errorTypeRef, ExecutionContext ctx) {
        throw new UnsupportedOperationException(generateUnsupportedMessage("FirErrorTypeRef"));
    }

    private String generateUnsupportedMessage(String typeName) {
        StringBuilder msg = new StringBuilder(typeName);
        msg.append(" is not supported at cursor: ");
        msg.append(source, cursor, Math.min(source.length(), cursor + 30));
        if (currentFile != null) {
            msg.append("in file: ");
            msg.append(currentFile.getName());
        }
        return msg.toString();
    }

    /**
     * Delegate {@link FirElement} to the appropriate visit.
     *
     * @param firElement visited element.
     * @param ctx        N/A. The FirVisitor requires a second parameter that is generally used for DataFlow analysis.
     * @return {@link J}
     */
    @SuppressWarnings("UnstableApiUsage")
    @Nullable
    @Override
    public J visitElement(@Nullable FirElement firElement, ExecutionContext ctx) {
        if (firElement == null) {
            return null;
        }

        int saveCursor = cursor;
        Space prefix = whitespace();
        ASTNode node = nodes.get(cursor);
        if (node != null) {
            switch (node.getElementType().getDebugName()) {
                case "PARENTHESIZED":
                    if (node.getTextRange().getEndOffset() >= firElement.getSource().getEndOffset()) {
                        return wrapInParens(firElement, prefix, ctx);
                    }
                    break;
                case "REFERENCE_EXPRESSION":
                    if ("POSTFIX_EXPRESSION".equals(node.getTreeParent().getElementType().getDebugName()) && firElement instanceof FirBlock) {
                        firElement = ((FirBlock) firElement).getStatements().get(1);
                    }
                    break;
                case "OPERATION_REFERENCE":
                    if ("PREFIX_EXPRESSION".equals(node.getTreeParent().getElementType().getDebugName()) && firElement instanceof FirBlock) {
                        firElement = ((FirBlock) firElement).getStatements().get(0);
                    }
                    break;
                default:
                    break;
            }
        }

        cursor = saveCursor;
        // FIR error elements
        if (firElement instanceof FirErrorNamedReference) {
            return visitErrorNamedReference((FirErrorNamedReference) firElement, ctx);
        } else if (firElement instanceof FirErrorExpression) {
            return visitErrorExpression((FirErrorExpression) firElement, ctx);
        } else if (firElement instanceof FirErrorFunction) {
            return visitErrorFunction((FirErrorFunction) firElement, ctx);
        } else if (firElement instanceof FirErrorImport) {
            return visitErrorImport((FirErrorImport) firElement, ctx);
        } else if (firElement instanceof FirErrorLoop) {
            return visitErrorLoop((FirErrorLoop) firElement, ctx);
        } else if (firElement instanceof FirErrorProperty) {
            return visitErrorProperty((FirErrorProperty) firElement, ctx);
        } else if (firElement instanceof FirErrorResolvedQualifier) {
            return visitErrorResolvedQualifier((FirErrorResolvedQualifier) firElement, ctx);
        } else if (firElement instanceof FirErrorTypeRef) {
            return visitErrorTypeRef((FirErrorTypeRef) firElement, ctx);
        }

        else if (firElement instanceof FirAnnotationCall) {
            return visitAnnotationCall((FirAnnotationCall) firElement, ctx);
        } else if (firElement instanceof FirAnonymousFunction) {
            return visitAnonymousFunction((FirAnonymousFunction) firElement, ctx);
        } else if (firElement instanceof FirAnonymousFunctionExpression) {
            return visitAnonymousFunctionExpression((FirAnonymousFunctionExpression) firElement, ctx);
        } else if (firElement instanceof FirAnonymousObject) {
            return visitAnonymousObject((FirAnonymousObject) firElement, ctx);
        } else if (firElement instanceof FirAnonymousObjectExpression) {
            return visitAnonymousObjectExpression((FirAnonymousObjectExpression) firElement, ctx);
        } else if (firElement instanceof FirArrayOfCall) {
            return visitArrayOfCall((FirArrayOfCall) firElement, ctx);
        } else if (firElement instanceof FirBinaryLogicExpression) {
            return visitBinaryLogicExpression((FirBinaryLogicExpression) firElement, ctx);
        } else if (firElement instanceof FirBlock) {
            return visitBlock((FirBlock) firElement, ctx);
        } else if (firElement instanceof FirBreakExpression) {
            return visitBreakExpression((FirBreakExpression) firElement, ctx);
        } else if (firElement instanceof FirCallableReferenceAccess) {
            return visitCallableReferenceAccess((FirCallableReferenceAccess) firElement, ctx);
        } else if (firElement instanceof FirCatch) {
            return visitCatch((FirCatch) firElement, ctx);
        } else if (firElement instanceof FirCheckNotNullCall) {
            return visitCheckNotNullCall((FirCheckNotNullCall) firElement, ctx);
        } else if (firElement instanceof FirRegularClass) {
            return visitRegularClass((FirRegularClass) firElement, ctx);
        } else if (firElement instanceof FirComparisonExpression) {
            return visitComparisonExpression((FirComparisonExpression) firElement, ctx);
        } else if (firElement instanceof FirConstExpression) {
            return visitConstExpression((FirConstExpression<?>) firElement, ctx);
        } else if (firElement instanceof FirConstructor) {
            return visitConstructor((FirConstructor) firElement, ctx);
        } else if (firElement instanceof FirContinueExpression) {
            return visitContinueExpression((FirContinueExpression) firElement, ctx);
        } else if (firElement instanceof FirDoWhileLoop) {
            return visitDoWhileLoop((FirDoWhileLoop) firElement, ctx);
        } else if (firElement instanceof FirElvisExpression) {
            return visitElvisExpression((FirElvisExpression) firElement, ctx);
        } else if (firElement instanceof FirEnumEntry) {
            return visitEnumEntry((FirEnumEntry) firElement, ctx);
        } else if (firElement instanceof FirEqualityOperatorCall) {
            return visitEqualityOperatorCall((FirEqualityOperatorCall) firElement, ctx);
        } else if (firElement instanceof FirSuperReference) {
            return visitSuperReference((FirSuperReference) firElement, ctx);
        } else if (firElement instanceof FirFunctionCall) {
            return visitFunctionCall((FirFunctionCall) firElement, ctx);
        } else if (firElement instanceof FirFunctionTypeRef) {
            return visitFunctionTypeRef((FirFunctionTypeRef) firElement, ctx);
        } else if (firElement instanceof FirGetClassCall) {
            return visitGetClassCall((FirGetClassCall) firElement, ctx);
        } else if (firElement instanceof FirLabel) {
            return visitLabel((FirLabel) firElement, ctx);
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
        } else if (firElement instanceof FirBackingFieldReference) {
            return visitBackingFieldReference((FirBackingFieldReference) firElement, ctx);
        } else if (firElement instanceof FirResolvedNamedReference) {
            return visitResolvedNamedReference((FirResolvedNamedReference) firElement, ctx);
        } else if (firElement instanceof FirResolvedTypeRef) {
            return visitResolvedTypeRef((FirResolvedTypeRef) firElement, ctx);
        } else if (firElement instanceof FirResolvedQualifier) {
            return visitResolvedQualifier((FirResolvedQualifier) firElement, ctx);
        } else if (firElement instanceof FirReturnExpression) {
            return visitReturnExpression((FirReturnExpression) firElement, ctx);
        } else if (firElement instanceof FirSafeCallExpression) {
            return visitSafeCallExpression((FirSafeCallExpression) firElement, ctx);
        } else if (firElement instanceof FirCheckedSafeCallSubject) {
            return visitCheckedSafeCallSubject((FirCheckedSafeCallSubject) firElement, ctx);
        } else if (firElement instanceof FirSimpleFunction) {
            return visitSimpleFunction((FirSimpleFunction) firElement, ctx);
        } else if (firElement instanceof FirSmartCastExpression) {
            return visitSmartCastExpression((FirSmartCastExpression) firElement, ctx);
        } else if (firElement instanceof FirStarProjection) {
            return visitStarProjection((FirStarProjection) firElement, ctx);
        } else if (firElement instanceof FirStringConcatenationCall) {
            return visitStringConcatenationCall((FirStringConcatenationCall) firElement, ctx);
        } else if (firElement instanceof FirThisReceiverExpression) {
            return visitThisReceiverExpression((FirThisReceiverExpression) firElement, ctx);
        } else if (firElement instanceof FirThrowExpression) {
            return visitThrowExpression((FirThrowExpression) firElement, ctx);
        } else if (firElement instanceof FirTypeOperatorCall) {
            return visitTypeOperatorCall((FirTypeOperatorCall) firElement, ctx);
        } else if (firElement instanceof FirTypeParameter) {
            return visitTypeParameter((FirTypeParameter) firElement, ctx);
        } else if (firElement instanceof FirTryExpression) {
            return visitTryExpression((FirTryExpression) firElement, ctx);
        } else if (firElement instanceof FirTypeAlias) {
            return visitTypeAlias((FirTypeAlias) firElement, ctx);
        } else if (firElement instanceof FirTypeProjectionWithVariance) {
            return visitTypeProjectionWithVariance((FirTypeProjectionWithVariance) firElement, ctx);
        } else if (firElement instanceof FirUserTypeRef) {
            return visitUserTypeRef((FirUserTypeRef) firElement, ctx);
        } else if (firElement instanceof FirValueParameter) {
            return visitValueParameter((FirValueParameter) firElement, ctx);
        } else if (firElement instanceof FirVariableAssignment) {
            return visitVariableAssignment((FirVariableAssignment) firElement, ctx);
        } else if (firElement instanceof FirWhenBranch) {
            return visitWhenBranch((FirWhenBranch) firElement, ctx);
        } else if (firElement instanceof FirWhenExpression) {
            return visitWhenExpression((FirWhenExpression) firElement, ctx);
        } else if (firElement instanceof FirWhenSubjectExpression) {
            return visitWhenSubjectExpression((FirWhenSubjectExpression) firElement, ctx);
        } else if (firElement instanceof FirWhileLoop) {
            return visitWhileLoop((FirWhileLoop) firElement, ctx);
        }

        // Not implemented yet.
        else if (firElement instanceof FirArgumentList) {
            return visitArgumentList((FirArgumentList) firElement, ctx);
        } else if (firElement instanceof FirAugmentedArraySetCall) {
            return visitAugmentedArraySetCall((FirAugmentedArraySetCall) firElement, ctx);
        } else if (firElement instanceof FirAssignmentOperatorStatement) {
            return visitAssignmentOperatorStatement((FirAssignmentOperatorStatement) firElement, ctx);
        } else if (firElement instanceof FirAnonymousInitializer) {
            return visitAnonymousInitializer((FirAnonymousInitializer) firElement, ctx);
        } else if (firElement instanceof FirAnnotationArgumentMapping) {
            return visitAnnotationArgumentMapping((FirAnnotationArgumentMapping) firElement, ctx);
        } else if (firElement instanceof FirBackingField) {
            return visitBackingField((FirBackingField) firElement, ctx);
        } else if (firElement instanceof FirLegacyRawContractDescription) {
            return visitLegacyRawContractDescription((FirLegacyRawContractDescription) firElement, ctx);
        } else if (firElement instanceof FirRawContractDescription) {
            return visitRawContractDescription((FirRawContractDescription) firElement, ctx);
        } else if (firElement instanceof FirResolvedContractDescription) {
            return visitResolvedContractDescription((FirResolvedContractDescription) firElement, ctx);
        } else if (firElement instanceof FirContractDescription) {
            return visitContractDescription((FirContractDescription) firElement, ctx);
        } else if (firElement instanceof FirContextReceiver) {
            return visitContextReceiver((FirContextReceiver) firElement, ctx);
        } else if (firElement instanceof FirContractDescriptionOwner) {
            return visitContractDescriptionOwner((FirContractDescriptionOwner) firElement, ctx);
        } else if (firElement instanceof FirQualifiedAccessExpression) {
            return visitQualifiedAccessExpression((FirQualifiedAccessExpression) firElement, ctx);
        } else if (firElement instanceof FirQualifiedAccess) {
            return visitQualifiedAccess((FirQualifiedAccess) firElement, ctx);
        } else if (firElement instanceof FirContextReceiverArgumentListOwner) {
            return visitContextReceiverArgumentListOwner((FirContextReceiverArgumentListOwner) firElement, ctx);
        } else if (firElement instanceof FirClassReferenceExpression) {
            return visitClassReferenceExpression((FirClassReferenceExpression) firElement, ctx);
        } else if (firElement instanceof FirClassLikeDeclaration) {
            return visitClassLikeDeclaration((FirClassLikeDeclaration) firElement, ctx);
        } else if (firElement instanceof FirCall) {
            return visitCall((FirCall) firElement, ctx);
        } else if (firElement instanceof FirDynamicTypeRef) {
            return visitDynamicTypeRef((FirDynamicTypeRef) firElement, ctx);
        } else if (firElement instanceof FirResolvedDeclarationStatus) {
            return visitResolvedDeclarationStatus((FirResolvedDeclarationStatus) firElement, ctx);
        } else if (firElement instanceof FirDeclarationStatus) {
            return visitDeclarationStatus((FirDeclarationStatus) firElement, ctx);
        } else if (firElement instanceof FirEffectDeclaration) {
            return visitEffectDeclaration((FirEffectDeclaration) firElement, ctx);
        } else if (firElement instanceof FirField) {
            return visitField((FirField) firElement, ctx);
        } else if (firElement instanceof FirFunction) {
            return visitFunction((FirFunction) firElement, ctx);
        } else if (firElement instanceof FirImplicitTypeRef) {
            return visitImplicitTypeRef((FirImplicitTypeRef) firElement, ctx);
        } else if (firElement instanceof FirIntersectionTypeRef) {
            return visitIntersectionTypeRef((FirIntersectionTypeRef) firElement, ctx);
        } else if (firElement instanceof FirLoopJump) {
            return visitLoopJump((FirLoopJump) firElement, ctx);
        } else if (firElement instanceof FirJump) {
            return visitJump((FirJump<? extends FirTargetElement>) firElement, ctx);
        } else if (firElement instanceof FirNamedReference) {
            return visitNamedReference((FirNamedReference) firElement, ctx);
        } else if (firElement instanceof FirPlaceholderProjection) {
            return visitPlaceholderProjection((FirPlaceholderProjection) firElement, ctx);
        } else if (firElement instanceof FirThisReference) {
            return visitThisReference((FirThisReference) firElement, ctx);
        } else if (firElement instanceof FirReference) {
            return visitReference((FirReference) firElement, ctx);
        } else if (firElement instanceof FirResolvable) {
            return visitResolvable((FirResolvable) firElement, ctx);
        } else if (firElement instanceof FirResolvedImport) {
            return visitResolvedImport((FirResolvedImport) firElement, ctx);
        } else if (firElement instanceof FirResolvedReifiedParameterReference) {
            return visitResolvedReifiedParameterReference((FirResolvedReifiedParameterReference) firElement, ctx);
        } else if (firElement instanceof FirSpreadArgumentExpression) {
            return visitSpreadArgumentExpression((FirSpreadArgumentExpression) firElement, ctx);
        } else if (firElement instanceof FirTypeRefWithNullability) {
            return visitTypeRefWithNullability((FirTypeRefWithNullability) firElement, ctx);
        } else if (firElement instanceof FirTypeRef) {
            return visitTypeRef((FirTypeRef) firElement, ctx);
        } else if (firElement instanceof FirTypeParameterRef) {
            return visitTypeParameterRef((FirTypeParameterRef) firElement, ctx);
        } else if (firElement instanceof FirTypeParametersOwner) {
            return visitTypeParametersOwner((FirTypeParametersOwner) firElement, ctx);
        } else if (firElement instanceof FirTypeProjection) {
            return visitTypeProjection((FirTypeProjection) firElement, ctx);
        } else if (firElement instanceof FirVarargArgumentsExpression) {
            return visitVarargArgumentsExpression((FirVarargArgumentsExpression) firElement, ctx);
        } else if (firElement instanceof FirWrappedArgumentExpression) {
            return visitWrappedArgumentExpression((FirWrappedArgumentExpression) firElement, ctx);
        } else if (firElement instanceof FirWrappedExpression) {
            return visitWrappedExpression((FirWrappedExpression) firElement, ctx);
        } else if (firElement instanceof FirNoReceiverExpression) {
            return visitNoReceiverExpression((FirNoReceiverExpression) firElement, ctx);
        }

        throw new IllegalArgumentException("Unsupported FirElement " + firElement.getClass().getName());
    }

    @SuppressWarnings("unchecked")
    private <J2 extends J> J wrapInParens(FirElement firElement, Space prefix, ExecutionContext ctx) {
        skip("(");
        return new J.Parentheses<>(
                randomId(),
                prefix,
                Markers.EMPTY,
                (JRightPadded<J2>) padRight(visitElement(firElement, ctx), sourceBefore(")"))
        );
    }

    private J.Identifier createIdentifier(@Nullable String name) {
        return createIdentifier(name == null ? "" : name, null, null);
    }

    private J.Identifier createIdentifier(String name, FirElement firElement) {
        return createIdentifier(name, typeMapping.type(firElement, getCurrentFile()), null);
    }

    private J.Identifier createIdentifier(String name, FirResolvedNamedReference namedReference) {
        FirBasedSymbol<?> resolvedSymbol = namedReference.getResolvedSymbol();
        if (resolvedSymbol instanceof FirVariableSymbol) {
            FirVariableSymbol<?> propertySymbol = (FirVariableSymbol<?>) resolvedSymbol;
            JavaType.FullyQualified owner = null;
            ConeClassLikeLookupTag lookupTag = ClassMembersKt.containingClass(propertySymbol);
            if (lookupTag != null) {
                owner = (JavaType.FullyQualified) typeMapping.type(LookupTagUtilsKt.toFirRegularClassSymbol(lookupTag, firSession).getFir());
            }
            return createIdentifier(name, typeMapping.type(namedReference, getCurrentFile()),
                    typeMapping.variableType(propertySymbol, owner, getCurrentFile()));
        }
        return createIdentifier(name, (FirElement) namedReference);
    }

    @SuppressWarnings("SameParameterValue")
    private J.Identifier createIdentifier(String name, @Nullable JavaType type, @Nullable JavaType.Variable fieldType) {
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

    private J.MethodDeclaration createImplicitMethodDeclaration(String name) {
        return new J.MethodDeclaration(
                randomId(),
                EMPTY,
                Markers.EMPTY,
                emptyList(),
                emptyList(),
                null,
                null,
                new J.MethodDeclaration.IdentifierWithAnnotations(
                        new J.Identifier(
                                randomId(),
                                EMPTY,
                                Markers.EMPTY,
                                name,
                                null,
                                null
                        ),
                        emptyList()
                ),
                JContainer.empty(),
                null,
                null,
                null,
                null
        ).withMarkers(Markers.EMPTY.addIfAbsent(new Implicit(randomId())));
    }

    private J.Annotation convertToAnnotation(K.Modifier modifier) {
        J.Identifier name = new J.Identifier(randomId(),
                EMPTY,
                Markers.EMPTY,
                modifier.getType().name().toLowerCase(),
                null,
                null);

        return new J.Annotation(
                randomId(),
                modifier.getPrefix(),
                Markers.EMPTY.addIfAbsent(new Modifier(randomId())),
                name,
                JContainer.empty());
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
            throw new IllegalArgumentException("Unexpected empty list of FIR Annotations.");
        }

        FirAnnotation firAnnotation = firAnnotations.get(0);
        J.Annotation annotation = (J.Annotation) visitElement(firAnnotation, null);

        firAnnotations.remove(firAnnotation);
        return annotation;
    }

    private J.ControlParentheses<Expression> mapControlParentheses(FirElement firElement) {
        Space controlParenPrefix = whitespace();
        skip("(");
        return new J.ControlParentheses<>(randomId(), controlParenPrefix, Markers.EMPTY,
                convert(firElement, t -> sourceBefore(")"), ctx));
    }

    private J mapForLoop(FirBlock firBlock) {

        FirWhileLoop forLoop = (FirWhileLoop) firBlock.getStatements().get(1);
        FirProperty receiver = (FirProperty) forLoop.getBlock().getStatements().get(0);

        J.Label label = null;
        if (forLoop.getLabel() != null) {
            label = (J.Label) visitElement(forLoop.getLabel(), ctx);
        }

        Space prefix = whitespace();
        skip("for");
        Space controlPrefix = sourceBefore("(");

        J.VariableDeclarations variable;

        int additionalVariables = 0;
        if ("<destruct>".equals(receiver.getName().asString())) {
            additionalVariables = source.substring(cursor, cursor + source.substring(cursor).indexOf(")") + 1).split(",").length;

            Space variablePrefix = sourceBefore("(");
            List<FirStatement> statements = forLoop.getBlock().getStatements();
            List<JRightPadded<J.VariableDeclarations.NamedVariable>> variables = new ArrayList<>(additionalVariables);
            // Skip the first statement at position 0.
            for (int i = 1; i < additionalVariables + 1; i++) {
                FirStatement statement = statements.get(i);
                J.VariableDeclarations part = (J.VariableDeclarations) visitElement(statement, ctx);
                JRightPadded<J.VariableDeclarations.NamedVariable> named = part.getPadding().getVariables().get(0);
                named = named.withElement(named.getElement().withName(named.getElement().getName().withPrefix(part.getPrefix())));
                Space after = i == additionalVariables ? sourceBefore(")") : sourceBefore(",");
                named = named.withAfter(after);
                variables.add(named);
            }

            variable = new J.VariableDeclarations(
                    randomId(),
                    variablePrefix,
                    Markers.EMPTY,
                    emptyList(),
                    emptyList(),
                    null,
                    null,
                    emptyList(),
                    variables);
        } else {
            variable = (J.VariableDeclarations) visitElement(receiver, ctx);
        }

        Space afterVariable = sourceBefore("in");

        FirProperty loopCondition = (FirProperty) firBlock.getStatements().get(0);
        Expression expression;
        if (loopCondition.getInitializer() instanceof FirFunctionCall &&
                ((FirFunctionCall) loopCondition.getInitializer()).getExplicitReceiver() instanceof FirFunctionCall) {
            expression = (Expression) visitElement(((FirFunctionCall) loopCondition.getInitializer()).getExplicitReceiver(), ctx);
        } else {
            expression = (Expression) visitElement(((FirFunctionCall) loopCondition.getInitializer()).getExplicitReceiver(), ctx);
        }

        Space afterExpression = sourceBefore(")");
        J.ForEachLoop.Control control = new J.ForEachLoop.Control(
                randomId(),
                controlPrefix,
                Markers.EMPTY,
                padRight(variable, afterVariable),
                padRight(expression, afterExpression));

        JRightPadded<Statement> body = null;
        if (!forLoop.getBlock().getStatements().isEmpty()) {
            Set<FirElement> skip = Collections.newSetFromMap(new IdentityHashMap<>());
            List<FirStatement> statements = forLoop.getBlock().getStatements();
            for (int i = 0; i < 1 + additionalVariables; i++) {
                skip.add(statements.get(i));
            }

            Statement block = (Statement) visitBlock(forLoop.getBlock(), skip, ctx);
            body = padRight(block, EMPTY);
        }

        J.ForEachLoop statement = new J.ForEachLoop(
                randomId(),
                prefix,
                Markers.EMPTY,
                control,
                body);

        return label != null ? label.withStatement(statement) : statement;
    }

    private List<J.Annotation> mapModifiers(List<FirAnnotation> firAnnotations, String stopWord) {
        if ("<no name provided>".equals(stopWord)) {
            List<J.Annotation> annotations = mapAnnotations(firAnnotations);
            return annotations == null ? emptyList() : annotations;
        }

        List<FirAnnotation> findMatch = new ArrayList<>(firAnnotations.size());
        findMatch.addAll(firAnnotations);

        List<J.Annotation> modifiers = new ArrayList<>();
        int count = 0;
        while (count < 10) {
            int saveCursor = cursor;
            Space prefix = whitespace();

            if (cursor == source.length() - 1 ||
                    // Matched stop word.
                    (source.startsWith(stopWord, cursor) && (source.length() - 1 == cursor + stopWord.length() ||
                            (Character.isWhitespace(source.charAt(cursor + stopWord.length())) ||
                                    isDelimiter(source.charAt(cursor + stopWord.length())))))) {
                cursor(saveCursor);
                return modifiers;
            } else if (source.startsWith("@", cursor)) {
                cursor(saveCursor);
                J.Annotation annotation = mapAnnotation(findMatch);
                modifiers.add(annotation);
            } else if ((source.startsWith("val", cursor) || source.startsWith("var", cursor)) &&
                    (Character.isWhitespace(source.charAt(cursor + 3)) ||
                            isDelimiter(source.charAt(cursor + 3)))) {
                String word = source.startsWith("val", cursor) ? "val" : "var";

                J.Identifier name = createIdentifier(word);
                modifiers.add(new J.Annotation(
                        randomId(),
                        prefix,
                        Markers.EMPTY.addIfAbsent(new Modifier(randomId())),
                        name,
                        JContainer.empty()));
            } else {
                K.Modifier modifier = mapModifier(prefix, emptyList());
                if (modifier == null) {
                    cursor(saveCursor);
                    return modifiers;
                }
                J.Annotation annotation = convertToAnnotation(modifier);
                modifiers.add(annotation);
            }

            count++;
        }

        return modifiers;
    }

    private boolean isDelimiter(char c) {
        return c == '(' || c == ')' || c == '{' || c == '}' || c == '<' || c == '>' || c == ':' || c == '.' || c == ',';
    }

    private boolean isJModifier(String modifier) {
        switch (modifier) {
            case "public":
            case "protected":
            case "private":
            case "final":
            case "abstract":
                return true;
        }
        return false;
    }

    private J.Annotation mapKModifierToAnnotation(String kModifier) {
        Space prefix = whitespace();
        J.Identifier name = createIdentifier(kModifier);
        return new J.Annotation(
                randomId(),
                prefix,
                Markers.EMPTY.addIfAbsent(new Modifier(randomId())),
                name,
                JContainer.empty());
    }

    private J.Modifier mapToJModifier(String text, List<J.Annotation> annotations) {
        Space prefix = whitespace();

        J.Modifier.Type type;
        switch (text) {
            case "public":
                type = J.Modifier.Type.Public;
                break;
            case "protected":
                type = J.Modifier.Type.Protected;
                break;
            case "private":
                type = J.Modifier.Type.Private;
                break;
            case "final":
                type = J.Modifier.Type.Final;
                break;
            case "abstract":
                type = J.Modifier.Type.Abstract;
                break;
            default:
                throw new IllegalArgumentException("Unknown modifier " + text);
        }
        skip(text);
        return new J.Modifier(randomId(), prefix, Markers.EMPTY, type, annotations);
    }

    @Nullable
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
        } else if (source.startsWith("noinline", cursor)) {
            type = K.Modifier.Type.NoInline;
        } else if (source.startsWith("crossinline", cursor)) {
            type = K.Modifier.Type.CrossInline;
        } else if (source.startsWith("value", cursor)) {
            type = K.Modifier.Type.Value;
        } else if (source.startsWith("infix", cursor)) {
            type = K.Modifier.Type.Infix;
        } else if (source.startsWith("operator", cursor)) {
            type = K.Modifier.Type.Operator;
        } else if (source.startsWith("data", cursor)) {
            type = K.Modifier.Type.Data;
        } else {
            return null;
        }

        cursor += type.name().length();
        return new K.Modifier(randomId(), prefix, Markers.EMPTY, type, annotations);
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
                op = J.Binary.Type.LessThanOrEqual;
                break;
            default:
                throw new IllegalArgumentException("Unsupported FirOperation " + op.name());
        }

        return op;
    }

    private boolean isUnaryOperation(String name) {
        return "dec".equals(name) ||
                "inc".equals(name) ||
                "not".equals(name) ||
                "unaryMinus".equals(name) ||
                "unaryPlus".equals(name);
    }

    @Nullable
    private FirBasedSymbol<?> getCurrentFile() {
        return currentFile == null ? null : currentFile.getSymbol();
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
        if (t instanceof FirThisReceiverExpression) {
            return 0;
        } else if (t.getSource() == null) {
            throw new IllegalStateException("Unexpected null source ... fix me.");
        }
        return t.getSource().getEndOffset();
    }

    @Nullable
    private PsiElement getCurrentPsiNode() {
        if (nodes.containsKey(cursor)) {
            return nodes.get(cursor).getPsi();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private <J2 extends J> J2 convert(FirElement t, ExecutionContext ctx) {
        return (J2) visitElement(t, ctx);
    }

    @Nullable
    private <J2 extends J> JRightPadded<J2> convert(FirElement t, Function<FirElement, Space> suffix, ExecutionContext ctx) {
        //noinspection unchecked
        J2 j = (J2) visitElement(t, ctx);

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
        return convertAll(elements, innerSuffix, suffix, ctx, false);
    }

    @SuppressWarnings("unchecked")
    private <J2 extends J> List<JRightPadded<J2>> convertAll(List<? extends FirElement> elements,
                                                             Function<FirElement, Space> innerSuffix,
                                                             Function<FirElement, Space> suffix,
                                                             ExecutionContext ctx,
                                                             boolean withUnknown) {
        if (elements.isEmpty()) {
            return emptyList();
        }
        List<JRightPadded<J2>> converted = new ArrayList<>(elements.size());
        for (int i = 0; i < elements.size(); i++) {
            FirElement element = elements.get(i);
            J2 j;
            if (withUnknown && element.getSource() != null) {
                int saveCursor = cursor;
                try {
                    j = (J2) visitElement(element, ctx);
                } catch (Exception e) {
                    cursor = saveCursor;
                    Space prefix = whitespace();
                    String text = element.getSource().getLighterASTNode().toString();
                    skip(text);
                    j = (J2) new J.Unknown(
                            randomId(),
                            prefix,
                            Markers.EMPTY,
                            new J.Unknown.Source(
                                    randomId(),
                                    EMPTY,
                                    Markers.build(singletonList(ParseExceptionResult.build(KotlinParser.class, e)
                                            .withTreeType(element.getSource().getKind().toString()))),
                                    text));
                }
            } else {
                j = (J2) visitElement(element, ctx);
            }
            Space after = i == elements.size() - 1 ? suffix.apply(element) : innerSuffix.apply(element);
            if (j == null && i < elements.size() - 1) {
                continue;
            }
            if (j == null) {
                //noinspection unchecked
                j = (J2) new J.Empty(randomId(), EMPTY, Markers.EMPTY);
            }
            JRightPadded<J2> rightPadded = padRight(j, after);
            converted.add(rightPadded);
        }
        return converted.isEmpty() ? emptyList() : converted;
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
            cursor(saveCursor);
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

    private <T> JRightPadded<T> padRight(T tree, @Nullable Space right) {
        return new JRightPadded<>(tree, right == null ? EMPTY : right, Markers.EMPTY);
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
                    if (c1 == '/') {
                        if (c2 == '/') {
                            inSingleLineComment = true;
                            delimIndex++;
                        } else if (c2 == '*') {
                            inMultiLineComment = true;
                            delimIndex++;
                        }
                    } else if (c1 == '*') {
                        if (c2 == '/') {
                            inMultiLineComment = false;
                            delimIndex += 2;
                        }
                    }
                }

                if (!inMultiLineComment && !inSingleLineComment) {
                    if (stop != null && source.charAt(delimIndex) == stop) {
                        return -1;
                    } // reached stop word before finding the delimiter

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
