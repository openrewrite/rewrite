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
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget;
import org.jetbrains.kotlin.fir.*;
import org.jetbrains.kotlin.fir.contracts.*;
import org.jetbrains.kotlin.fir.declarations.*;
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyGetter;
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertySetter;
import org.jetbrains.kotlin.fir.declarations.impl.FirPrimaryConstructor;
import org.jetbrains.kotlin.fir.diagnostics.FirDiagnosticHolder;
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
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.regex.Pattern;

import static java.lang.Math.max;
import static java.util.Collections.*;
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
    private final KotlinTypeMapping typeMapping;
    private final ExecutionContext ctx;
    private final FirSession firSession;
    private int cursor;

    // Associate top-level function and property declarations to the file.
    @Nullable
    private FirFile currentFile;

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
            try {
                statement = (Statement) visitElement(declaration, ctx);
            } catch (Exception e) {
                throw new KotlinParsingException("Failed to parse declaration", e);
            }
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
                annotations,
                paddedPkg,
                imports,
                statements,
                format(source.substring(cursor)));
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
                null);
    }

    @Override
    public J visitAnnotationCall(FirAnnotationCall annotationCall, ExecutionContext ctx) {
        Space prefix = whitespace();
        Markers markers = Markers.EMPTY;
        skip("@");
        if (annotationCall.getUseSiteTarget() == AnnotationUseSiteTarget.FILE) {
            skip("file");
            markers = markers.addIfAbsent(new FileSuffix(randomId(), sourceBefore(":")));
        }

        J.Identifier name = (J.Identifier) visitElement(annotationCall.getCalleeReference(), ctx);
        JContainer<Expression> args = null;
        if (!annotationCall.getArgumentList().getArguments().isEmpty()) {
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
                    if (p.getReturnTypeRef() instanceof FirResolvedTypeRef) {
                        FirResolvedTypeRef typeRef = (FirResolvedTypeRef) p.getReturnTypeRef();
                        typeArguments = typeRef.getType().getTypeArguments();
                    }

                    for (int j = 0; j < paramNames.length; j++) {
                        String paramName = paramNames[j].trim();
                        JavaType type = typeArguments == null ? null : typeMapping.type(typeArguments[j]);
                        J.Identifier param = createIdentifier(paramName, type, null);
                        JRightPadded<J> paramExpr = JRightPadded.build(param);
                        if (j != paramNames.length - 1) {
                            paramExpr = paramExpr.withAfter(sourceBefore(","));
                        } else {
                            paramExpr = paramExpr.withAfter(sourceBefore(")"));
                        }
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
            body = new J.Block(randomId(),
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
            throw new IllegalArgumentException("Unsupported anonymous function expression.");
        }

        return visitAnonymousFunction(anonymousFunctionExpression.getAnonymousFunction(), ctx);
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
                padRight((Expression) visitElement(callableReferenceAccess.getExplicitReceiver(), ctx), sourceBefore("::")),
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
                        JContainer.build(EMPTY, convertAll(arrayOfCall.getArgumentList().getArguments(), commaDelim, t -> sourceBefore("]"), ctx), Markers.EMPTY),
                typeMapping.type(arrayOfCall));
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
            throw new IllegalArgumentException("Unsupported binary expression type " + binaryLogicExpression.getKind().name());
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
        for (FirElement firElement : firStatements) {
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

            if (expr == null) {
                Space returnPrefix = EMPTY;
                boolean explicitReturn = false;

                if (firElement instanceof FirReturnExpression) {
                    saveCursor = cursor;
                    returnPrefix = whitespace();
                    if (source.startsWith("return", cursor)) {
                        skip("return");
                        explicitReturn = true;
                    } else {
                        returnPrefix = EMPTY;
                        cursor(saveCursor);
                    }
                }

                if (!(firElement instanceof FirReturnExpression && ((FirReturnExpression) firElement).getResult() instanceof FirUnitExpression)) {
                    expr = visitElement(firElement, ctx);
                }

                if (firElement instanceof FirReturnExpression) {
                    if (expr == null) {
                        expr = new K.KReturn(randomId(), new J.Return(randomId(), EMPTY, Markers.EMPTY, null), null);
                    } else if (expr instanceof Statement && !(expr instanceof Expression)) {
                        expr = new K.KReturn(randomId(), new J.Return(randomId(), EMPTY, Markers.EMPTY, new K.StatementExpression(randomId(), (Statement) expr)), null);
                    } else if (expr instanceof Expression) {
                        expr = new K.KReturn(randomId(), new J.Return(randomId(), EMPTY, Markers.EMPTY, (Expression) expr), null);
                    }

                    if (explicitReturn) {
                        expr = expr.withPrefix(returnPrefix);
                    } else {
                        expr = expr.withMarkers(expr.getMarkers().addIfAbsent(new ImplicitReturn(randomId())));
                    }
                }

                if (!(expr instanceof Statement)) {
                    if (expr instanceof Expression) {
                        expr = new K.ExpressionStatement(randomId(), (Expression) expr);
                    } else {
                        throw new IllegalArgumentException("Unexpected statement type.");
                    }
                }
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

        Expression left = (Expression) visitElement(functionCall.getExplicitReceiver(), ctx);

        Space opPrefix = whitespace();
        J.Binary.Type op = mapOperation(comparisonExpression.getOperation());

        if (functionCall.getArgumentList().getArguments().size() != 1) {
            throw new IllegalArgumentException("Unsupported FirComparisonExpression argument size");
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
            throw new IllegalArgumentException("Unsupported number of equality operator arguments.");
        }

        FirElement left = equalityOperatorCall.getArgumentList().getArguments().get(0);
        FirOperation op = equalityOperatorCall.getOperation();
        FirElement right = equalityOperatorCall.getArgumentList().getArguments().get(1);

        if (op == FirOperation.IDENTITY || op == FirOperation.NOT_IDENTITY) {
            return new K.Binary(
                    randomId(),
                    whitespace(),
                    Markers.EMPTY,
                    (Expression) visitElement(left, ctx),
                    padLeft(sourceBefore(op.getOperator()), op == FirOperation.IDENTITY ? K.Binary.Type.IdentityEquals : K.Binary.Type.IdentityNotEquals),
                    (Expression) visitElement(right, ctx),
                    EMPTY,
                    typeMapping.type(equalityOperatorCall)
            );
        } else {
            return new J.Binary(
                    randomId(),
                    whitespace(),
                    Markers.EMPTY,
                    (Expression) visitElement(left, ctx),
                    padLeft(sourceBefore(op.getOperator()), mapOperation(op)),
                    (Expression) visitElement(right, ctx),
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
        return new J.Ternary(randomId(),
                prefix,
                Markers.EMPTY,
                new J.Empty(randomId(), EMPTY, Markers.EMPTY),
                padLeft(EMPTY, (Expression) visitElement(elvisExpression.getLhs(), ctx)),
                padLeft(sourceBefore("?:"), (Expression) visitElement(elvisExpression.getRhs(), ctx)),
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

        return new J.Identifier(randomId(),
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
            } else if ("contains".equals(operatorName) || "rangeTo".equals(operatorName) || "get".equals(operatorName)) {
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
        if (namedReference instanceof FirErrorNamedReference) {
            throw new IllegalStateException("Unresolved name reference: " + ((FirErrorNamedReference) namedReference).getDiagnostic());
        }

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

        } else if (namedReference instanceof FirResolvedNamedReference) {
            Markers markers = Markers.EMPTY;
            JRightPadded<Expression> select = null;

            if (!(functionCall instanceof FirImplicitInvokeCall)) {
                FirElement visit = getReceiver(functionCall.getDispatchReceiver());
                if (visit == null) {
                    visit = getReceiver(functionCall.getExtensionReceiver());
                }
                if (visit == null) {
                    visit = getReceiver(functionCall.getExplicitReceiver());
                }

                if (visit != null) {
                    Expression selectExpr = (Expression) visitElement(visit, ctx);
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

            if (isInfix) {
                markers = markers.addIfAbsent(new ReceiverType(randomId()));
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

            FirBasedSymbol<?> symbol = ((FirResolvedNamedReference) namedReference).getResolvedSymbol();
            FirBasedSymbol<?> owner = null;
            if (symbol instanceof FirNamedFunctionSymbol) {
                FirNamedFunctionSymbol namedFunctionSymbol = (FirNamedFunctionSymbol) symbol;
                ConeClassLikeLookupTag lookupTag = ClassMembersKt.containingClass(namedFunctionSymbol);
                if (lookupTag != null) {
                    owner = LookupTagUtilsKt.toFirRegularClassSymbol(lookupTag, firSession);
                } else if (currentFile != null) {
                    owner = getCurrentFile();
                }
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

        throw new IllegalArgumentException("Unsupported function call.");
    }

    @Nullable
    private FirElement getReceiver(FirElement firElement) {
        FirElement receiver = null;
        if (firElement instanceof FirFunctionCall ||
                firElement instanceof FirPropertyAccessExpression ||
                firElement instanceof FirConstExpression ||
                firElement instanceof FirResolvedQualifier) {
            receiver = firElement;
        } else if (firElement instanceof FirCheckedSafeCallSubject) {
            receiver = ((FirCheckedSafeCallSubject) firElement).getOriginalReceiverRef().getValue();
        }

        return receiver;
    }

    private JContainer<Expression> mapFunctionalCallArguments(List<FirExpression> firExpressions) {
        JContainer<Expression> args;
        if (firExpressions.size() == 1) {
            FirExpression firExpression = firExpressions.get(0);
            if (firExpression instanceof FirVarargArgumentsExpression) {
                FirVarargArgumentsExpression argumentsExpression = (FirVarargArgumentsExpression) firExpressions.get(0);
                args = JContainer.build(sourceBefore("("), argumentsExpression.getArguments().isEmpty() ?
                        singletonList(padRight(new J.Empty(randomId(), sourceBefore(")"), Markers.EMPTY), EMPTY)) :
                        convertAll(argumentsExpression.getArguments(), commaDelim, t -> sourceBefore(")"), ctx), Markers.EMPTY);
            } else {
                args = JContainer.build(sourceBefore("("), convertAll(singletonList(firExpression), commaDelim, t -> sourceBefore(")"), ctx), Markers.EMPTY);
            }
        } else {
            if (firExpressions.isEmpty()) {
                args = JContainer.build(sourceBefore("("),
                        singletonList(padRight(new J.Empty(randomId(), sourceBefore(")"), Markers.EMPTY), EMPTY)), Markers.EMPTY);
            } else {
                Space containerPrefix = sourceBefore("(");
                List<JRightPadded<Expression>> expressions = new ArrayList<>(firExpressions.size());
                for (int i = 0; i < firExpressions.size(); i++) {
                    FirExpression expression = firExpressions.get(i);
                    if (i == firExpressions.size() - 1 && expression instanceof FirLambdaArgumentExpression) {
                        int saveCursor = cursor;
                        Space space = whitespace();
                        // Trailing lambda argument: https://kotlinlang.org/docs/lambdas.html#passing-trailing-lambdas
                        if (source.startsWith(")", cursor)) {
                            skip(")");
                            Expression expr = convert(expression, ctx);
                            expr = expr.withMarkers(expr.getMarkers().addIfAbsent(new TrailingLambdaArgument(randomId())));
                            expressions.add(padRight(expr, space));
                            break;
                        } else {
                            cursor(saveCursor);
                        }
                    }

                    Expression expr = convert(expression, ctx);
                    Space padding = i < firExpressions.size() - 1 ? sourceBefore(",") : sourceBefore(")");
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
                    // Both pre and post unary operations exist in a block with multiple AST elements: the property and unary operation.
                    // The J.Unary objects are all created here instead of interpreting the statements in visitBlock.
                    // The PRE operations have access to the correct property name, but the POST operations are set to "<unary>".
                    // So, we extract the name from the source based on the post operator.
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
                    // Both pre and post unary operations exist in a block with multiple AST elements: the property and unary operation.
                    // The J.Unary objects are all created here instead of interpreting the statements in visitBlock.
                    // The PRE operations have access to the correct property name, but the POST operations are set to "<unary>".
                    // So, we extract the name from the source based on the post operator.
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
                throw new IllegalArgumentException("Unsupported unary operator type.");
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
            left = (Expression) visitElement(functionCall.getExplicitReceiver(), ctx);

            opPrefix = sourceBefore("[");
            kotlinBinaryType = K.Binary.Type.Get;

            right = (Expression) visitElement(functionCall.getArgumentList().getArguments().get(0), ctx);

            after = sourceBefore("]");
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

        Expression left = (Expression) visitElement(functionCall.getDispatchReceiver(), ctx);

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
            case "times":
                javaBinaryType = J.Binary.Type.Multiplication;
                opPrefix = sourceBefore("*");
                break;
            default:
                throw new IllegalArgumentException("Unsupported binary operator type.");
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
                receiver);
    }

    @Override
    public J visitImport(FirImport firImport, ExecutionContext ctx) {
        Space prefix = sourceBefore("import");
        JLeftPadded<Boolean> statik = padLeft(EMPTY, false);

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
            cursor += aliasText.length();
            // This feels not quite right, could probably record type information here
            J.Identifier aliasId = createIdentifier(aliasText)
                    .withPrefix(aliasPrefix);
            alias = padLeft(asPrefix, aliasId);
        }
        return new J.Import(
                randomId(),
                prefix,
                Markers.EMPTY,
                statik,
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
        return new J.Assignment(
                randomId(),
                prefix,
                Markers.EMPTY,
                createIdentifier(namedArgumentExpression.getName().toString()),
                padLeft(sourceBefore("="), convert(namedArgumentExpression.getExpression(), ctx)),
                typeMapping.type(namedArgumentExpression.getTypeRef()));
    }

    @Override
    public J visitProperty(FirProperty property, ExecutionContext ctx) {
        Space prefix = whitespace();
        Markers markers = Markers.EMPTY;

        List<J> modifiers = emptyList();
        List<J.Annotation> annotations = mapModifiers(property.getAnnotations(), property.getName().asString());

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
        J.Identifier name = createIdentifier(property.getName().asString(), property);

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
                J j = visitElement(typeRef, ctx);
                if (j instanceof TypeTree) {
                    typeExpression = (TypeTree) j;
                } else {
                    typeExpression = new K.FunctionType(randomId(), (TypedTree) j, null);
                }
            }
        }

        // Dimensions do not exist in Kotlin, and array is declared based on the type. I.E., IntArray
        List<JLeftPadded<Space>> dimensionsAfterName = emptyList();

        J expr = null;
        int saveCursor = cursor;
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

        if (property.getGetter() != null && !(property.getGetter() instanceof FirDefaultPropertyGetter)) {
            expr = visitElement(property.getGetter(), ctx);
            if (expr instanceof Statement && !(expr instanceof Expression)) {
                expr = new K.StatementExpression(randomId(), (Statement) expr);
            }
        }

        if (property.getSetter() != null && !(property.getSetter() instanceof FirDefaultPropertySetter)) {
            throw new IllegalArgumentException("Explicit setter initialization are not currently supported.");
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
                ));
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
            J.Identifier name = createIdentifier(methodName, propertyAccessor);

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

        throw new IllegalArgumentException("Unsupported property accessor.");
    }

    @Override
    public J visitResolvedNamedReference(FirResolvedNamedReference resolvedNamedReference, ExecutionContext ctx) {
        String name = resolvedNamedReference.getName().asString();
        return createIdentifier(name, resolvedNamedReference);
    }

    @Override
    public J visitReturnExpression(FirReturnExpression returnExpression, ExecutionContext ctx) {
        if (returnExpression.getResult() instanceof FirUnitExpression) {
            Space prefix = whitespace();
            J.Identifier label = null;
            if (source.startsWith("return@", cursor)) {
                skip("return@");
                label = createIdentifier(returnExpression.getTarget().getLabelName());
            }

            return new K.KReturn(randomId(), new J.Return(randomId(), prefix, Markers.EMPTY, null), label);
        }

        return visitElement(returnExpression.getResult(), ctx);
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
        }
        throw new IllegalArgumentException("Unsupported null delegated type reference.");
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
                JContainer.build(before, convertAll(simpleFunction.getValueParameters(), commaDelim, t -> sourceBefore(")"), ctx), Markers.EMPTY) :
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
                throw new IllegalStateException("Unexpected single block expression.");
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
    public J visitTypeOperatorCall(FirTypeOperatorCall typeOperatorCall, ExecutionContext ctx) {
        Space prefix = whitespace();
        FirElement target = typeOperatorCall.getArgumentList().getArguments().get(0) instanceof FirSmartCastExpression ?
                ((FirSmartCastExpression) typeOperatorCall.getArgumentList().getArguments().get(0)).getOriginalExpression() :
                typeOperatorCall.getArgumentList().getArguments().get(0);
        Expression element = (Expression) visitElement(target, ctx);

        Space after;
        Markers markers = Markers.EMPTY;
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
                throw new IllegalArgumentException("Unsupported type operator " + typeOperatorCall.getOperation().name());
        }

        if (typeOperatorCall.getOperation() == FirOperation.AS || typeOperatorCall.getOperation() == FirOperation.SAFE_AS) {
            return new J.TypeCast(
                    randomId(),
                    prefix,
                    markers,
                    new J.ControlParentheses<>(
                            randomId(),
                            after,
                            Markers.EMPTY,
                            JRightPadded.build((TypeTree) visitElement(typeOperatorCall.getConversionTypeRef(), ctx))),
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
                        convertAll(nonImplicitParams, t -> sourceBefore(","), noDelim, ctx), Markers.EMPTY);
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

        JLeftPadded<J.Block> finallyy = tryExpression.getFinallyBlock() == null ? null :
                padLeft(sourceBefore("finally"), (J.Block) visitElement(tryExpression.getFinallyBlock(), ctx));

        return new J.Try(randomId(),
                prefix,
                Markers.EMPTY,
                resources,
                block,
                catches,
                finallyy);
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
        } else {
            valueName = valueParameter.getName().asString();
            namePrefix = whitespace();
        }
        J.Identifier name = createIdentifier(valueName, valueParameter);

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
                J j = visitElement(typeRef, ctx);
                if (j instanceof TypeTree) {
                    typeExpression = (TypeTree) j;
                } else {
                    typeExpression = new K.FunctionType(randomId(), (TypedTree) j, null);
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
                } else if (whenBranch.getCondition() instanceof FirBinaryLogicExpression) {
                    mapBinaryExpressions((FirBinaryLogicExpression) whenBranch.getCondition(), expressions);
                } else if (whenBranch.getCondition() instanceof FirFunctionCall) {
                    expressions.add(padRight((Expression) visitElement(whenBranch.getCondition(), ctx), sourceBefore("->")));
                } else {
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
            throw new IllegalStateException("Implement me.");
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

    private void mapBinaryExpressions(FirBinaryLogicExpression logicExpression, List<JRightPadded<Expression>> expressions) {
        if (logicExpression.getLeftOperand() instanceof FirBinaryLogicExpression) {
            mapBinaryExpressions((FirBinaryLogicExpression) logicExpression.getLeftOperand(), expressions);
        } else if (logicExpression.getLeftOperand() instanceof FirEqualityOperatorCall) {
            FirEqualityOperatorCall lhs = (FirEqualityOperatorCall) logicExpression.getLeftOperand();
            Expression left = (Expression) visitElement(lhs.getArgumentList().getArguments().get(1), ctx);
            expressions.add(padRight(left, sourceBefore(",")));
        } else {
            throw new IllegalArgumentException("Unsupported logical operator from when expression.");
        }

        FirEqualityOperatorCall rhs = (FirEqualityOperatorCall) logicExpression.getRightOperand();
        Expression right = (Expression) visitElement(rhs.getArgumentList().getArguments().get(1), ctx);
        Space after = whitespace();
        if (source.startsWith(",", cursor)) {
            skip(",");
        } else {
            skip("->");
        }
        expressions.add(padRight(right, after));
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
        throw new UnsupportedOperationException("FirArgumentList is not supported at cursor: " + source.substring(cursor, Math.min(source.length(), cursor + 20)));
    }

    @Override
    public J visitAugmentedArraySetCall(FirAugmentedArraySetCall augmentedArraySetCall, ExecutionContext ctx) {
        throw new UnsupportedOperationException("FirAugmentedArraySetCall is not supported at cursor: " + source.substring(cursor, Math.min(source.length(), cursor + 20)));
    }

    @Override
    public J visitAssignmentOperatorStatement(FirAssignmentOperatorStatement assignmentOperatorStatement, ExecutionContext ctx) {
        throw new UnsupportedOperationException("FirAssignmentOperatorStatement is not supported at cursor: " + source.substring(cursor, Math.min(source.length(), cursor + 20)));
    }

    @Override
    public J visitAnonymousInitializer(FirAnonymousInitializer anonymousInitializer, ExecutionContext ctx) {
        throw new UnsupportedOperationException("FirAnonymousInitializer is not supported at cursor: " + source.substring(cursor, Math.min(source.length(), cursor + 20)));
    }

    @Override
    public J visitAnnotation(FirAnnotation annotation, ExecutionContext ctx) {
        throw new UnsupportedOperationException("FirAnnotation is not supported at cursor: " + source.substring(cursor, Math.min(source.length(), cursor + 20)));
    }

    @Override
    public J visitAnnotationContainer(FirAnnotationContainer annotationContainer, ExecutionContext ctx) {
        throw new UnsupportedOperationException("FirAnnotationContainer is not supported at cursor: " + source.substring(cursor, Math.min(source.length(), cursor + 20)));
    }

    @Override
    public J visitAnnotationArgumentMapping(FirAnnotationArgumentMapping annotationArgumentMapping, ExecutionContext ctx) {
        throw new UnsupportedOperationException("FirAnnotationArgumentMapping is not supported at cursor: " + source.substring(cursor, Math.min(source.length(), cursor + 20)));
    }

    @Override
    public J visitBackingField(FirBackingField backingField, ExecutionContext ctx) {
        throw new UnsupportedOperationException("FirBackingField is not supported at cursor: " + source.substring(cursor, Math.min(source.length(), cursor + 20)));
    }

    @Override
    public J visitBackingFieldReference(FirBackingFieldReference backingFieldReference, ExecutionContext ctx) {
        throw new UnsupportedOperationException("FirBackingFieldReference is not supported at cursor: " + source.substring(cursor, Math.min(source.length(), cursor + 20)));
    }

    @Override
    public J visitContextReceiver(FirContextReceiver contextReceiver, ExecutionContext ctx) {
        throw new UnsupportedOperationException("FirContextReceiver is not supported at cursor: " + source.substring(cursor, Math.min(source.length(), cursor + 20)));
    }

    @Override
    public J visitConstructor(FirConstructor constructor, ExecutionContext ctx) {
        Space prefix = whitespace();
        Markers markers = Markers.EMPTY;
        List<J> modifiers = emptyList();
        List<J.Annotation> annotations = mapModifiers(constructor.getAnnotations(), "constructor");

        J.TypeParameters typeParameters = constructor.getTypeParameters().isEmpty() ? null :
                new J.TypeParameters(randomId(), sourceBefore("<"), Markers.EMPTY,
                        emptyList(),
                        convertAll(constructor.getTypeParameters(), commaDelim, t -> sourceBefore(">"), ctx));

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
                JContainer.build(before, convertAll(constructor.getValueParameters(), commaDelim, t -> sourceBefore(")"), ctx), Markers.EMPTY) :
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

            returnTypeExpression = (TypeTree) visitElement(constructor.getReturnTypeRef(), ctx);

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
                typeParameters,
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
        throw new UnsupportedOperationException("FirComponentCall is not supported at cursor: " + source.substring(cursor, Math.min(source.length(), cursor + 20)));
    }

    @Override
    public J visitContractDescriptionOwner(FirContractDescriptionOwner contractDescriptionOwner, ExecutionContext ctx) {
        throw new UnsupportedOperationException("FirContractDescriptionOwner is not supported at cursor: " + source.substring(cursor, Math.min(source.length(), cursor + 20)));
    }

    @Override
    public J visitContextReceiverArgumentListOwner(FirContextReceiverArgumentListOwner contextReceiverArgumentListOwner, ExecutionContext ctx) {
        throw new UnsupportedOperationException("FirContextReceiverArgumentListOwner is not supported at cursor: " + source.substring(cursor, Math.min(source.length(), cursor + 20)));
    }

    @Override
    public J visitClassReferenceExpression(FirClassReferenceExpression classReferenceExpression, ExecutionContext ctx) {
        throw new UnsupportedOperationException("FirClassReferenceExpression is not supported at cursor: " + source.substring(cursor, Math.min(source.length(), cursor + 20)));
    }

    @Override
    public J visitClassLikeDeclaration(FirClassLikeDeclaration classLikeDeclaration, ExecutionContext ctx) {
        throw new UnsupportedOperationException("FirClassLikeDeclaration is not supported at cursor: " + source.substring(cursor, Math.min(source.length(), cursor + 20)));
    }

    @Override
    public J visitCall(FirCall call, ExecutionContext ctx) {
        throw new UnsupportedOperationException("FirCall is not supported at cursor: " + source.substring(cursor, Math.min(source.length(), cursor + 20)));
    }

    @Override
    public J visitCallableDeclaration(FirCallableDeclaration callableDeclaration, ExecutionContext ctx) {
        throw new UnsupportedOperationException("FirCallableDeclaration is not supported at cursor: " + source.substring(cursor, Math.min(source.length(), cursor + 20)));
    }

    @Override
    public J visitDelegatedConstructorCall(FirDelegatedConstructorCall delegatedConstructorCall, ExecutionContext ctx) {
        throw new UnsupportedOperationException("FirDelegatedConstructorCall is not supported at cursor: " + source.substring(cursor, Math.min(source.length(), cursor + 20)));
    }

    @Override
    public J visitDeclaration(FirDeclaration declaration, ExecutionContext ctx) {
        throw new UnsupportedOperationException("FirDeclaration is not supported at cursor: " + source.substring(cursor, Math.min(source.length(), cursor + 20)));
    }

    @Override
    public J visitDynamicTypeRef(FirDynamicTypeRef dynamicTypeRef, ExecutionContext ctx) {
        throw new UnsupportedOperationException("FirDynamicTypeRef is not supported at cursor: " + source.substring(cursor, Math.min(source.length(), cursor + 20)));
    }

    @Override
    public J visitDelegateFieldReference(FirDelegateFieldReference delegateFieldReference, ExecutionContext ctx) {
        throw new UnsupportedOperationException("FirDelegateFieldReference is not supported at cursor: " + source.substring(cursor, Math.min(source.length(), cursor + 20)));
    }

    @Override
    public J visitDeclarationStatus(FirDeclarationStatus declarationStatus, ExecutionContext ctx) {
        throw new UnsupportedOperationException("FirDeclarationStatus is not supported at cursor: " + source.substring(cursor, Math.min(source.length(), cursor + 20)));
    }

    @Override
    public J visitField(FirField field, ExecutionContext ctx) {
        throw new UnsupportedOperationException("FirField is not supported at cursor: " + source.substring(cursor, Math.min(source.length(), cursor + 20)));
    }

    @Override
    public J visitFunction(FirFunction function, ExecutionContext ctx) {
        throw new UnsupportedOperationException("FirFunction is not supported at cursor: " + source.substring(cursor, Math.min(source.length(), cursor + 20)));
    }

    @Override
    public J visitImplicitInvokeCall(FirImplicitInvokeCall implicitInvokeCall, ExecutionContext ctx) {
        throw new UnsupportedOperationException("FirImplicitInvokeCall is not supported at cursor: " + source.substring(cursor, Math.min(source.length(), cursor + 20)));
    }

    @Override
    public J visitImplicitTypeRef(FirImplicitTypeRef implicitTypeRef, ExecutionContext ctx) {
        throw new UnsupportedOperationException("FirImplicitTypeRef is not supported at cursor: " + source.substring(cursor, Math.min(source.length(), cursor + 20)));
    }

    @Override
    public J visitIntegerLiteralOperatorCall(FirIntegerLiteralOperatorCall integerLiteralOperatorCall, ExecutionContext ctx) {
        throw new UnsupportedOperationException("FirIntegerLiteralOperatorCall is not supported at cursor: " + source.substring(cursor, Math.min(source.length(), cursor + 20)));
    }

    @Override
    public J visitIntersectionTypeRef(FirIntersectionTypeRef intersectionTypeRef, ExecutionContext ctx) {
        throw new UnsupportedOperationException("FirIntersectionTypeRef is not supported at cursor: " + source.substring(cursor, Math.min(source.length(), cursor + 20)));
    }

    @Override
    public <E extends FirTargetElement> J visitJump(FirJump<E> jump, ExecutionContext ctx) {
        throw new UnsupportedOperationException("FirJump is not supported at cursor: " + source.substring(cursor, Math.min(source.length(), cursor + 20)));
    }

    @Override
    public J visitLoop(FirLoop loop, ExecutionContext ctx) {
        throw new UnsupportedOperationException("FirLoop is not supported at cursor: " + source.substring(cursor, Math.min(source.length(), cursor + 20)));
    }

    @Override
    public J visitLoopJump(FirLoopJump loopJump, ExecutionContext ctx) {
        throw new UnsupportedOperationException("FirLoopJump is not supported at cursor: " + source.substring(cursor, Math.min(source.length(), cursor + 20)));
    }

    @Override
    public J visitMemberDeclaration(FirMemberDeclaration memberDeclaration, ExecutionContext ctx) {
        throw new UnsupportedOperationException("FirMemberDeclaration is not supported at cursor: " + source.substring(cursor, Math.min(source.length(), cursor + 20)));
    }

    @Override
    public J visitNamedReference(FirNamedReference namedReference, ExecutionContext ctx) {
        throw new UnsupportedOperationException("FirNamedReference is not supported at cursor: " + source.substring(cursor, Math.min(source.length(), cursor + 20)));
    }

    @Override
    public J visitPlaceholderProjection(FirPlaceholderProjection placeholderProjection, ExecutionContext ctx) {
        throw new UnsupportedOperationException("FirPlaceholderProjection is not supported at cursor: " + source.substring(cursor, Math.min(source.length(), cursor + 20)));
    }

    @Override
    public J visitQualifiedAccess(FirQualifiedAccess qualifiedAccess, ExecutionContext ctx) {
        throw new UnsupportedOperationException("FirQualifiedAccess is not supported at cursor: " + source.substring(cursor, Math.min(source.length(), cursor + 20)));
    }

    @Override
    public J visitQualifiedAccessExpression(FirQualifiedAccessExpression qualifiedAccessExpression, ExecutionContext ctx) {
        throw new UnsupportedOperationException("FirQualifiedAccessExpression is not supported at cursor: " + source.substring(cursor, Math.min(source.length(), cursor + 20)));
    }

    @Override
    public J visitReference(FirReference reference, ExecutionContext ctx) {
        throw new UnsupportedOperationException("FirReference is not supported at cursor: " + source.substring(cursor, Math.min(source.length(), cursor + 20)));
    }

    @Override
    public J visitRegularClass(FirRegularClass regularClass, ExecutionContext ctx) {
        Space prefix = whitespace();
        Markers markers = Markers.EMPTY;

        // Not used until it's possible to handle K.Modifiers.
        List<J> modifiers = emptyList();

        ClassKind classKind = regularClass.getClassKind();
        String stopWord;
        if (ClassKind.INTERFACE == classKind) {
            stopWord = "interface";
        } else if (ClassKind.OBJECT == classKind) {
            stopWord = "object";
        } else {
            stopWord = "class";
        }

        List<J.Annotation> leadingAnnotation = mapModifiers(regularClass.getAnnotations(), stopWord);
        List<J.Annotation> kindAnnotations = emptyList();

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
        if (ClassKind.OBJECT == classKind && leadingAnnotation.stream().anyMatch(a -> a.getAnnotationType() instanceof J.Identifier && "companion".equals(((J.Identifier) a.getAnnotationType()).getSimpleName()))) {
            name = new J.Identifier(
                    randomId(),
                    prefix,
                    Markers.EMPTY,
                    regularClass.getName().asString(),
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

        int saveCursor = cursor;
        Space before = whitespace();
        JContainer<Statement> primaryConstructor = null;
        boolean inlineConstructor = source.startsWith("(", cursor) && firPrimaryConstructor != null;
        if (inlineConstructor) {
            skip("(");
            primaryConstructor = JContainer.build(before,
                    firPrimaryConstructor.getValueParameters().isEmpty() ?
                            singletonList(padRight(new J.Empty(randomId(), sourceBefore(")"), Markers.EMPTY), EMPTY)) :
                            convertAll(firPrimaryConstructor.getValueParameters(), commaDelim, t -> sourceBefore(")"), ctx), Markers.EMPTY);
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
                    element = new K.FunctionType(randomId(), newClass, null);
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
                    if (firElement.getSource() != null && (ClassKind.ENUM_CLASS == classKind &&
                            firElement.getSource().getKind() instanceof KtFakeSourceElementKind.EnumGeneratedDeclaration ||
                            firElement.getSource().getKind() instanceof KtFakeSourceElementKind.PropertyFromParameter)) {
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
                emptyList(), // Requires a K view to add K specific modifiers. Modifiers specific to Kotlin are added as annotations for now.
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

    @Override
    public J visitResolvable(FirResolvable resolvable, ExecutionContext ctx) {
        throw new UnsupportedOperationException("FirResolvable is not supported at cursor: " + source.substring(cursor, Math.min(source.length(), cursor + 20)));
    }

    @Override
    public J visitResolvedCallableReference(FirResolvedCallableReference resolvedCallableReference, ExecutionContext ctx) {
        throw new UnsupportedOperationException("FirResolvedCallableReference is not supported at cursor: " + source.substring(cursor, Math.min(source.length(), cursor + 20)));
    }

    @Override
    public J visitResolvedDeclarationStatus(FirResolvedDeclarationStatus resolvedDeclarationStatus, ExecutionContext ctx) {
        throw new UnsupportedOperationException("FirResolvedDeclarationStatus is not supported at cursor: " + source.substring(cursor, Math.min(source.length(), cursor + 20)));
    }

    @Override
    public J visitResolvedImport(FirResolvedImport resolvedImport, ExecutionContext ctx) {
        throw new UnsupportedOperationException("FirResolvedImport is not supported at cursor: " + source.substring(cursor, Math.min(source.length(), cursor + 20)));
    }

    @Override
    public J visitResolvedReifiedParameterReference(FirResolvedReifiedParameterReference resolvedReifiedParameterReference, ExecutionContext ctx) {
        throw new UnsupportedOperationException("FirResolvedReifiedParameterReference is not supported at cursor: " + source.substring(cursor, Math.min(source.length(), cursor + 20)));
    }

    @Override
    public J visitSmartCastExpression(FirSmartCastExpression smartCastExpression, ExecutionContext ctx) {
        throw new UnsupportedOperationException("FirSmartCastExpression is not supported at cursor: " + source.substring(cursor, Math.min(source.length(), cursor + 20)));
    }

    @Override
    public J visitSpreadArgumentExpression(FirSpreadArgumentExpression spreadArgumentExpression, ExecutionContext ctx) {
        throw new UnsupportedOperationException("FirSpreadArgumentExpression is not supported at cursor: " + source.substring(cursor, Math.min(source.length(), cursor + 20)));
    }

    @Override
    public J visitTypeRef(FirTypeRef typeRef, ExecutionContext ctx) {
        throw new UnsupportedOperationException("FirTypeRef is not supported at cursor: " + source.substring(cursor, Math.min(source.length(), cursor + 20)));
    }

    @Override
    public J visitTargetElement(FirTargetElement targetElement, ExecutionContext ctx) {
        throw new UnsupportedOperationException("FirTargetElement is not supported at cursor: " + source.substring(cursor, Math.min(source.length(), cursor + 20)));
    }

    @Override
    public J visitThisReference(FirThisReference thisReference, ExecutionContext ctx) {
        throw new UnsupportedOperationException("FirThisReference is not supported at cursor: " + source.substring(cursor, Math.min(source.length(), cursor + 20)));
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
            namePrefix = whitespace();
        }
        J.Identifier name = createIdentifier(valueName, typeMapping.type(typeAlias.getExpandedTypeRef()), null);

        // Dimensions do not exist in Kotlin, and array is declared based on the type. I.E., IntArray
        List<JLeftPadded<Space>> dimensionsAfterName = emptyList();

        Space initializerPrefix = sourceBefore("=");
        Expression expr = (Expression) visitElement(typeAlias.getExpandedTypeRef(), ctx);
        JRightPadded<J.VariableDeclarations.NamedVariable> namedVariable = maybeSemicolon(
                new J.VariableDeclarations.NamedVariable(
                        randomId(),
                        namePrefix,
                        Markers.EMPTY,
                        name,
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
                null,
                null,
                null,
                vars);
    }

    @Override
    public J visitTypeParameterRef(FirTypeParameterRef typeParameterRef, ExecutionContext ctx) {
        throw new UnsupportedOperationException("FirTypeParameterRef is not supported at cursor: " + source.substring(cursor, Math.min(source.length(), cursor + 20)));
    }

    @Override
    public J visitTypeParameterRefsOwner(FirTypeParameterRefsOwner typeParameterRefsOwner, ExecutionContext ctx) {
        throw new UnsupportedOperationException("FirTypeParameterRefsOwner is not supported at cursor: " + source.substring(cursor, Math.min(source.length(), cursor + 20)));
    }

    @Override
    public J visitTypeParametersOwner(FirTypeParametersOwner typeParametersOwner, ExecutionContext ctx) {
        throw new UnsupportedOperationException("FirTypeParametersOwner is not supported at cursor: " + source.substring(cursor, Math.min(source.length(), cursor + 20)));
    }

    @Override
    public J visitTypeProjection(FirTypeProjection typeProjection, ExecutionContext ctx) {
        throw new UnsupportedOperationException("FirTypeProjection is not supported at cursor: " + source.substring(cursor, Math.min(source.length(), cursor + 20)));
    }

    @Override
    public J visitTypeRefWithNullability(FirTypeRefWithNullability typeRefWithNullability, ExecutionContext ctx) {
        throw new UnsupportedOperationException("FirTypeRefWithNullability is not supported at cursor: " + source.substring(cursor, Math.min(source.length(), cursor + 20)));
    }

    @Override
    public J visitVarargArgumentsExpression(FirVarargArgumentsExpression varargArgumentsExpression, ExecutionContext ctx) {
        throw new UnsupportedOperationException("FirVarargArgumentsExpression is not supported at cursor: " + source.substring(cursor, Math.min(source.length(), cursor + 20)));
    }

    @Override
    public J visitWhenSubjectExpression(FirWhenSubjectExpression whenSubjectExpression, ExecutionContext ctx) {
        throw new UnsupportedOperationException("FirWhenSubjectExpression is not supported at cursor: " + source.substring(cursor, Math.min(source.length(), cursor + 20)));
    }

    @Override
    public J visitWrappedArgumentExpression(FirWrappedArgumentExpression wrappedArgumentExpression, ExecutionContext ctx) {
        throw new UnsupportedOperationException("FirWrappedArgumentExpression is not supported at cursor: " + source.substring(cursor, Math.min(source.length(), cursor + 20)));
    }

    @Override
    public J visitWrappedDelegateExpression(FirWrappedDelegateExpression wrappedDelegateExpression, ExecutionContext ctx) {
        throw new UnsupportedOperationException("FirWrappedDelegateExpression is not supported at cursor: " + source.substring(cursor, Math.min(source.length(), cursor + 20)));
    }

    @Override
    public J visitWrappedExpression(FirWrappedExpression wrappedExpression, ExecutionContext ctx) {
        throw new UnsupportedOperationException("FirWrappedExpression is not supported at cursor: " + source.substring(cursor, Math.min(source.length(), cursor + 20)));
    }

    /* Error element visits */

    @Override
    public J visitErrorExpression(FirErrorExpression errorExpression, ExecutionContext ctx) {
        throw new UnsupportedOperationException("FirErrorExpression should not be visited at cursor: " + source.substring(cursor, Math.min(source.length(), cursor + 20)));
    }

    @Override
    public J visitErrorFunction(FirErrorFunction errorFunction, ExecutionContext ctx) {
        throw new UnsupportedOperationException("FirErrorFunction should not be visited at cursor: " + source.substring(cursor, Math.min(source.length(), cursor + 20)));
    }

    @Override
    public J visitErrorImport(FirErrorImport errorImport, ExecutionContext ctx) {
        throw new UnsupportedOperationException("FirErrorImport should not be visited at cursor: " + source.substring(cursor, Math.min(source.length(), cursor + 20)));
    }

    @Override
    public J visitErrorLoop(FirErrorLoop errorLoop, ExecutionContext ctx) {
        throw new UnsupportedOperationException("FirErrorLoop should not be visited at cursor: " + source.substring(cursor, Math.min(source.length(), cursor + 20)));
    }

    @Override
    public J visitErrorProperty(FirErrorProperty errorProperty, ExecutionContext ctx) {
        throw new UnsupportedOperationException("FirErrorProperty should not be visited at cursor: " + source.substring(cursor, Math.min(source.length(), cursor + 20)));
    }

    @Override
    public J visitErrorResolvedQualifier(FirErrorResolvedQualifier errorResolvedQualifier, ExecutionContext ctx) {
        throw new UnsupportedOperationException("FirErrorResolvedQualifier should not be visited at cursor: " + source.substring(cursor, Math.min(source.length(), cursor + 20)));
    }

    @Override
    public J visitErrorTypeRef(FirErrorTypeRef errorTypeRef, ExecutionContext ctx) {
        throw new UnsupportedOperationException("FirErrorTypeRef should not be visited at cursor: " + source.substring(cursor, Math.min(source.length(), cursor + 20)));
    }

    /* Visits to parent classes ... these should never be called */
    @Override
    public J visitExpression(FirExpression expression, ExecutionContext ctx) {
        throw new UnsupportedOperationException("visitExpression should not be called.");
    }

    @Override
    public J visitStatement(FirStatement statement, ExecutionContext ctx) {
        throw new UnsupportedOperationException("visitStatement should not be called.");
    }

    @Override
    public J visitVariable(FirVariable variable, ExecutionContext ctx) {
        throw new UnsupportedOperationException("visitVariable should not be called.");
    }

    /* Visits to control flow ... these should never be called */
    @Override
    public J visitControlFlowGraphOwner(FirControlFlowGraphOwner controlFlowGraphOwner, ExecutionContext ctx) {
        throw new UnsupportedOperationException("visitControlFlowGraphOwner should not be called.");
    }

    @Override
    public J visitControlFlowGraphReference(FirControlFlowGraphReference controlFlowGraphReference, ExecutionContext ctx) {
        throw new UnsupportedOperationException("visitControlFlowGraphReference should not be called.");
    }

    /**
     * Delegate {@link FirElement} to the appropriate visit.
     *
     * @param firElement visited element.
     * @param ctx        N/A. The FirVisitor requires a second parameter that is generally used for DataFlow analysis.
     * @return {@link J}
     */
    @Override
    public J visitElement(FirElement firElement, ExecutionContext ctx) {
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
        } else if (firElement instanceof FirStarProjection) {
            return visitStarProjection((FirStarProjection) firElement, ctx);
        } else if (firElement instanceof FirStringConcatenationCall) {
            return visitStringConcatenationCall((FirStringConcatenationCall) firElement, ctx);
        } else if (firElement instanceof FirThisReceiverExpression) {
            return visitThisReceiverExpression((FirThisReceiverExpression) firElement, ctx);
        } else if (firElement instanceof FirTypeOperatorCall) {
            return visitTypeOperatorCall((FirTypeOperatorCall) firElement, ctx);
        } else if (firElement instanceof FirTypeParameter) {
            return visitTypeParameter((FirTypeParameter) firElement, ctx);
        } else if (firElement instanceof FirTryExpression) {
            return visitTryExpression((FirTryExpression) firElement, ctx);
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
        } else if (firElement instanceof FirConstructor) {
            return visitConstructor((FirConstructor) firElement, ctx);
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
        } else if (firElement instanceof FirTypeAlias) {
            return visitTypeAlias((FirTypeAlias) firElement, ctx);
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
        } else if (firElement instanceof FirSmartCastExpression) {
            return visitSmartCastExpression((FirSmartCastExpression) firElement, ctx);
        } else if (firElement instanceof FirSpreadArgumentExpression) {
            return visitSpreadArgumentExpression((FirSpreadArgumentExpression) firElement, ctx);
        } else if (firElement instanceof FirTypeRefWithNullability) {
            return visitTypeRefWithNullability((FirTypeRefWithNullability) firElement, ctx);
        } else if (firElement instanceof FirTypeRef) {
            return visitTypeRef((FirTypeRef) firElement, ctx);
        } else if (firElement instanceof FirThrowExpression) {
            return visitThrowExpression((FirThrowExpression) firElement, ctx);
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
        }

        // Visits to parent classes that should not occur.
        else if (firElement instanceof FirAnnotation) {
            return visitAnnotation((FirAnnotation) firElement, ctx);
        } else if (firElement instanceof FirExpression) {
            return visitExpression((FirExpression) firElement, ctx);
        } else if (firElement instanceof FirVariable) {
            return visitVariable((FirVariable) firElement, ctx);
        } else if (firElement instanceof FirCallableDeclaration) {
            return visitCallableDeclaration((FirCallableDeclaration) firElement, ctx);
        } else if (firElement instanceof FirMemberDeclaration) {
            return visitMemberDeclaration((FirMemberDeclaration) firElement, ctx);
        } else if (firElement instanceof FirTypeParameterRefsOwner) {
            return visitTypeParameterRefsOwner((FirTypeParameterRefsOwner) firElement, ctx);
        } else if (firElement instanceof FirLoop) {
            return visitLoop((FirLoop) firElement, ctx);
        } else if (firElement instanceof FirTargetElement) {
            return visitTargetElement((FirTargetElement) firElement, ctx);
        } else if (firElement instanceof FirDeclaration) {
            return visitDeclaration((FirDeclaration) firElement, ctx);
        } else if (firElement instanceof FirStatement) {
            return visitStatement((FirStatement) firElement, ctx);
        } else if (firElement instanceof FirAnnotationContainer) {
            return visitAnnotationContainer((FirAnnotationContainer) firElement, ctx);
        } else if (firElement instanceof FirDiagnosticHolder) {
            return visitDiagnosticHolder((FirDiagnosticHolder) firElement, ctx);
        }

        throw new IllegalArgumentException("Unsupported FirElement.");
    }

    private J.Identifier createIdentifier(@Nullable String name) {
        return createIdentifier(name == null ? "" : name, null, null);
    }

    private J.Identifier createIdentifier(String name, FirElement firElement) {
        return createIdentifier(name, typeMapping.type(firElement, getCurrentFile()), null);
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
                named = named.withElement(named.getElement().withPrefix(part.getPrefix()));
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
            return mapAnnotations(firAnnotations);
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
                op = J.Binary.Type.LessThan;
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

    @SuppressWarnings("SameParameterValue")
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
