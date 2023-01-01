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

import org.jetbrains.kotlin.KtLightSourceElement;
import org.jetbrains.kotlin.KtRealPsiSourceElement;
import org.jetbrains.kotlin.KtSourceElement;
import org.jetbrains.kotlin.com.intellij.lang.ASTNode;
import org.jetbrains.kotlin.com.intellij.psi.PsiElement;
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.*;
import org.jetbrains.kotlin.com.intellij.psi.tree.IElementType;
import org.jetbrains.kotlin.descriptors.ClassKind;
import org.jetbrains.kotlin.fir.FirElement;
import org.jetbrains.kotlin.fir.FirPackageDirective;
import org.jetbrains.kotlin.fir.declarations.*;
import org.jetbrains.kotlin.fir.declarations.impl.FirPrimaryConstructor;
import org.jetbrains.kotlin.fir.expressions.*;
import org.jetbrains.kotlin.fir.expressions.impl.FirElseIfTrueCondition;
import org.jetbrains.kotlin.fir.expressions.impl.FirNoReceiverExpression;
import org.jetbrains.kotlin.fir.expressions.impl.FirSingleExpressionBlock;
import org.jetbrains.kotlin.fir.expressions.impl.FirUnitExpression;
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference;
import org.jetbrains.kotlin.fir.references.FirNamedReference;
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference;
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol;
import org.jetbrains.kotlin.fir.types.*;
import org.jetbrains.kotlin.fir.types.impl.FirImplicitUnitTypeRef;
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitor;
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.psi.psiUtil.PsiChildRange;
import org.jetbrains.kotlin.psi.psiUtil.PsiUtilsKt;
import org.jetbrains.kotlin.psi.stubs.elements.KtAnnotationEntryElementType;
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
import java.util.function.Function;
import java.util.regex.Matcher;
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

public class KotlinParserVisitor extends FirDefaultVisitor<J, ExecutionContext> {
    private final Path sourcePath;

    @Nullable
    private final FileAttributes fileAttributes;
    private final String source;
    private final Charset charset;
    private final boolean charsetBomMarked;
    private final KotlinTypeMapping typeMapping;
    private final ExecutionContext ctx;

    private int cursor = 0;

    private static final Pattern whitespaceSuffixPattern = Pattern.compile("\\s*[^\\s]+(\\s*)");

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
    public J visitFile(FirFile file, ExecutionContext ctx) {
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
            statements.add(JRightPadded.build(statement));
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
    public J visitErrorNamedReference(FirErrorNamedReference errorNamedReference, ExecutionContext ctx) {
        if (!(errorNamedReference.getSource() instanceof KtRealPsiSourceElement)) {
            throw new IllegalStateException("Unexpected error reference source: " + errorNamedReference.getSource());
        }

        if (!(((KtRealPsiSourceElement) errorNamedReference.getSource()).getPsi() instanceof KtNameReferenceExpression)) {
            throw new IllegalStateException("Unexpected error reference source: " + errorNamedReference.getSource());
        }

        KtNameReferenceExpression nameReferenceExpression = (KtNameReferenceExpression) ((KtRealPsiSourceElement) errorNamedReference.getSource()).getPsi();
        String name = nameReferenceExpression.getIdentifier().getText();
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
    public J visitAnonymousFunction(FirAnonymousFunction anonymousFunction, ExecutionContext ctx) {
        throw new IllegalStateException("Implement me.");
    }

    @Override
    public J visitAnonymousFunctionExpression(FirAnonymousFunctionExpression anonymousFunctionExpression, ExecutionContext ctx) {
        if (!anonymousFunctionExpression.getAnonymousFunction().isLambda()) {
            throw new IllegalStateException("Implement me.");
        }

        Space prefix = sourceBefore("{");
        JavaType closureType = null; // TODO

        FirAnonymousFunction anonymousFunction = anonymousFunctionExpression.getAnonymousFunction();
        List<JRightPadded<J>> paramExprs = new ArrayList<>(anonymousFunction.getValueParameters().size());
        if (!anonymousFunction.getValueParameters().isEmpty()) {
            List<FirValueParameter> parameters = anonymousFunction.getValueParameters();
            for (int i = 0; i < parameters.size(); i++) {
                FirValueParameter p = parameters.get(i);
                JavaType type = null; // TODO
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
            // TODO: filter out synthetic statements.
            firStatements.add(s);
        }

        List<JRightPadded<Statement>> statements = new ArrayList<>(firStatements.size());
        for (int i = 0; i < firStatements.size(); i++) {
            FirElement firElement = firStatements.get(i);
            boolean explicitReturn = false;
            Space returnPrefix = EMPTY;
            if (i == firStatements.size() - 1) {
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
    public J visitClass(FirClass klass, ExecutionContext ctx) {
        if (!(klass instanceof FirRegularClass)) {
            throw new IllegalStateException("Implement me.");
        }

        FirRegularClass firRegularClass = (FirRegularClass) klass;
        Space prefix = whitespace();

        // Not used until it's possible to handle K.Modifiers.
        List<J> modifiers = emptyList();
//        if (firRegularClass.getSource() != null) {
//            PsiChildRange psiChildRange = PsiUtilsKt.getAllChildren(((KtRealPsiSourceElement) firRegularClass.getSource()).getPsi());
//            if (psiChildRange.getFirst() instanceof KtDeclarationModifierList) {
//                KtDeclarationModifierList modifierList = (KtDeclarationModifierList) psiChildRange.getFirst();
//                modifiers = getModifiers(modifierList);
//            }
//        }

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

        JContainer<J.TypeParameter> typeParams = firRegularClass.getTypeParameters().isEmpty() ? null : JContainer.build(
                sourceBefore("<"),
                convertAll(firRegularClass.getTypeParameters(), commaDelim, t -> sourceBefore(">"), ctx),
                Markers.EMPTY);

        // TODO: fix: super type references are resolved as error kind.
        JLeftPadded<TypeTree> extendings = null;

        // TODO: fix: super type references are resolved as error kind.
        JContainer<TypeTree> implementings = null;

        int saveCursor = cursor;
        Space bodyPrefix = whitespace();
        OmitBraces omitBraces = null;
        J.Block body;
        if (source.substring(cursor).isEmpty() || !source.substring(cursor).startsWith("{")) {
            cursor = saveCursor;
            omitBraces = new OmitBraces(randomId());
            body = new J.Block(randomId(), bodyPrefix, Markers.EMPTY, new JRightPadded<>(false, EMPTY, Markers.EMPTY), emptyList(), Space.EMPTY);
            body = body.withMarkers(body.getMarkers().addIfAbsent(omitBraces));
        } else {
            skip("{"); // Increment past the `{`
            List<FirElement> membersMultiVariablesSeparated = new ArrayList<>(firRegularClass.getDeclarations().size());
            for (FirDeclaration declaration : firRegularClass.getDeclarations()) {
                if (declaration instanceof FirPrimaryConstructor) {
                    FirPrimaryConstructor primaryConstructor = (FirPrimaryConstructor) declaration;
                    // Note: the generated constructor contain flags generated = false and from source = true ...
                    continue;
                }
                membersMultiVariablesSeparated.add(declaration);
            }

            List<JRightPadded<Statement>> members = new ArrayList<>(membersMultiVariablesSeparated.size());
            for (FirElement firElement : membersMultiVariablesSeparated) {
                members.add(maybeSemicolon((Statement) visitElement(firElement, ctx)));
            }

            body = new J.Block(randomId(), bodyPrefix, Markers.EMPTY, new JRightPadded<>(false, EMPTY, Markers.EMPTY),
                    members, sourceBefore("}"));
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
                null,
                body, // TODO
                null // TODO
        );
    }

    @Override
    public <T> J visitConstExpression(FirConstExpression<T> constExpression, ExecutionContext ctx) {
        Space prefix = whitespace();
        String valueSource = null;
        if (constExpression.getSource() != null) {
            valueSource = source.substring(constExpression.getSource().getStartOffset(), constExpression.getSource().getEndOffset());
            cursor += valueSource.length();
        }

        Object value = constExpression.getValue();
        JavaType.Primitive type;
        if (constExpression.getTypeRef() instanceof FirResolvedTypeRef &&
                ((FirResolvedTypeRef) constExpression.getTypeRef()).getType() instanceof ConeClassLikeType) {
            ConeClassLikeType coneClassLikeType = (ConeClassLikeType) ((FirResolvedTypeRef) constExpression.getTypeRef()).getType();
            type = typeMapping.primitive(coneClassLikeType);
        } else {
            throw new IllegalStateException("Implement me.");
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
            throw new IllegalStateException("Implement me.");
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
                null); // TODO
    }

    private J.Binary.Type mapOperation(FirOperation op) {
        switch (op) {
            case EQ:
                return J.Binary.Type.Equal;
            case NOT_EQ:
                return J.Binary.Type.NotEqual;
            default:
                throw new IllegalStateException("Implement me.");
        }
    }

    @Override
    public J visitEnumEntry(FirEnumEntry enumEntry, ExecutionContext ctx) {
        throw new IllegalStateException("Implement me.");
    }

    @Override
    public J visitFunctionCall(FirFunctionCall functionCall, ExecutionContext ctx) {
        FirFunctionCallOrigin origin = functionCall.getOrigin();
        if (origin == FirFunctionCallOrigin.Regular || functionCall instanceof FirImplicitInvokeCall) {
            return mapRegularFunctionCall(functionCall);
        } else if (origin == FirFunctionCallOrigin.Infix) {
            throw new IllegalStateException("Implement me.");
        } else if (origin == FirFunctionCallOrigin.Operator) {
            return mapOperatorFunctionCall(functionCall);
        }

        throw new IllegalStateException("Implement me.");
    }

    private J mapRegularFunctionCall(FirFunctionCall functionCall) {
        Space prefix = whitespace();
        FirNamedReference namedReference = functionCall.getCalleeReference();
        TypeTree name = (J.Identifier) visitElement(namedReference, null);

        if (namedReference instanceof FirResolvedNamedReference &&
                ((FirResolvedNamedReference) namedReference).getResolvedSymbol() instanceof FirConstructorSymbol) {
            if (!functionCall.getTypeArguments().isEmpty()) {
                name = new J.ParameterizedType(randomId(), EMPTY, Markers.EMPTY, name, mapTypeArguments(functionCall.getTypeArguments()));
            }

            JContainer<Expression> args = JContainer.empty();
            J.Block body = null; // TODO
            return new J.NewClass(
                    randomId(),
                    prefix,
                    Markers.EMPTY,
                    null,
                    EMPTY,
                    name,
                    args,
                    body,
                    null);
        } else if (namedReference instanceof FirResolvedNamedReference) {
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
            JContainer<Expression> args = JContainer.empty();
            Markers markers = Markers.EMPTY;
            if (source.startsWith("(", cursor)) {
                cursor = saveCursor;
                List<FirExpression> firExpressions = functionCall.getArgumentList().getArguments();
                if (firExpressions.size() == 1) {
                    FirExpression firExpression = firExpressions.get(0);
                    if (firExpression instanceof FirVarargArgumentsExpression) {
                        FirVarargArgumentsExpression argumentsExpression = (FirVarargArgumentsExpression) firExpressions.get(0);
                        args = JContainer.build(sourceBefore("("), argumentsExpression.getArguments().isEmpty() ?
                                singletonList(padRight(new J.Empty(randomId(), sourceBefore(")"), Markers.EMPTY), EMPTY)) :
                                convertAll(argumentsExpression.getArguments(), commaDelim, t -> sourceBefore(")"), ctx), Markers.EMPTY);
                    } else if (firExpression instanceof FirImplicitInvokeCall || firExpression instanceof FirConstExpression) {
                        args = JContainer.build(sourceBefore("("), convertAll(singletonList(firExpression), commaDelim, t -> sourceBefore(")"), ctx), Markers.EMPTY);
                    } else {
                        throw new IllegalStateException("Implement me.");
                    }
                } else {
                    args = JContainer.build(sourceBefore("("), firExpressions.isEmpty() ?
                            singletonList(padRight(new J.Empty(randomId(), sourceBefore(")"), Markers.EMPTY), EMPTY)) :
                            convertAll(firExpressions, commaDelim, t -> sourceBefore(")"), ctx), Markers.EMPTY);
                }
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
                    null, // TODO
                    typeParams,
                    (J.Identifier) name,
                    args,
                    null); // TODO
        }

        throw new IllegalStateException("Implement me.");
    }

    private JContainer<Expression> mapTypeArguments(List<FirTypeProjection> types) {
        Space prefix = whitespace();
        if (source.startsWith("<", cursor)) {
            skip("<");
        }
        List<JRightPadded<Expression>> parameters = new ArrayList<>(types.size());;

        for (int i = 0; i < types.size(); i++) {
            FirTypeProjection type = types.get(i);
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
        J.Binary.Type type = mapFunctionalCallOperator(functionCall);
        if (type != null) {
            Space prefix = whitespace();
            J left = visitElement(functionCall.getDispatchReceiver(), ctx);
            if (functionCall.getArgumentList().getArguments().size() != 1) {
                throw new IllegalStateException("Implement me.");
            }

            Space opPrefix = EMPTY;
            if (type == J.Binary.Type.Multiplication) {
                opPrefix = sourceBefore("*");
            } else if (type == J.Binary.Type.Subtraction) {
                opPrefix = sourceBefore("-");
            }

            J right = visitElement(functionCall.getArgumentList().getArguments().get(0), ctx);
            return new J.Binary(
                    randomId(),
                    prefix,
                    Markers.EMPTY,
                    (Expression) left,
                    padLeft(opPrefix, type),
                    (Expression) right,
                    null);
        }

        throw new IllegalStateException("Implement me.");
    }

    @Nullable
    private J.Binary.Type mapFunctionalCallOperator(FirFunctionCall functionCall) {
        String resolvedName = functionCall.getCalleeReference().getName().asString();
        J.Binary.Type op = null;
        if ("times".equals(resolvedName)) {
            op = J.Binary.Type.Multiplication;
        } else if ("minus".equals(resolvedName)) {
            op = J.Binary.Type.Subtraction;
        }

        return op;
    }

    @Override
    public J visitFunctionTypeRef(FirFunctionTypeRef functionTypeRef, ExecutionContext ctx) {
        Space prefix = whitespace();
        boolean parenthesized = source.charAt(cursor) == '(';
        skip("(");

        JavaType closureType = null; // TODO
        List<JRightPadded<J>> paramExprs = new ArrayList<>(functionTypeRef.getValueParameters().size());
        if (!functionTypeRef.getValueParameters().isEmpty()) {
            List<FirValueParameter> parameters = functionTypeRef.getValueParameters();
            for (int i = 0; i < parameters.size(); i++) {
                FirValueParameter p = parameters.get(i);
                JavaType type = null; // TODO
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

        return new J.Lambda(
                randomId(),
                prefix,
                omitBraces ? Markers.EMPTY.addIfAbsent(new OmitBraces(randomId())) : Markers.EMPTY,
                params,
                arrow,
                body,
                closureType);
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
        }
        return new J.Import(randomId(), prefix, Markers.EMPTY, statik, qualid);
    }

    @Override
    public J visitPackageDirective(FirPackageDirective packageDirective, ExecutionContext ctx) {
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
    public J visitLambdaArgumentExpression(FirLambdaArgumentExpression lambdaArgumentExpression, ExecutionContext ctx) {
        Space prefix = whitespace();
        J j = visitElement(lambdaArgumentExpression.getExpression(), ctx);
        return j.withPrefix(prefix);
    }

    @Override
    public J visitProperty(FirProperty property, ExecutionContext ctx) {
        Space prefix = whitespace();

        List<J> modifiers = emptyList();
        if (property.getSource() != null) {
//            PsiChildRange psiChildRange = PsiUtilsKt.getAllChildren(((KtRealPsiSourceElement) property.getSource()).getPsi());
//            if (psiChildRange.getFirst() instanceof KtDeclarationModifierList) {
//                KtDeclarationModifierList modifierList = (KtDeclarationModifierList) psiChildRange.getFirst();
//                modifiers = getModifiers(modifierList);
//            }
        }

        List<J.Annotation> annotations = emptyList(); // TODO: the last annotations in modifiers should be added.
        Markers markers = Markers.EMPTY;

        boolean isVal = property.isVal();
        markers = markers.addIfAbsent(new PropertyClassifier(randomId(), isVal ? sourceBefore("val") : sourceBefore("var"), isVal ? VAL : VAR));

        List<JRightPadded<J.VariableDeclarations.NamedVariable>> vars = new ArrayList<>(1); // adjust size if necessary
        Space namePrefix = sourceBefore(property.getName().asString());

        J.Identifier name = new J.Identifier(
                randomId(),
                EMPTY,
                Markers.EMPTY,
                property.getName().asString(),
                null, // TODO: add type mapping and set type
                null); // TODO: add type mapping and set variable type

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

        JRightPadded<J.VariableDeclarations.NamedVariable> namedVariable = maybeSemicolon(
                new J.VariableDeclarations.NamedVariable(
                        randomId(),
                        namePrefix,
                        Markers.EMPTY,
                        name,
                        dimensionsAfterName,
                        expr == null ? null : padLeft(exprPrefix, (Expression) expr),
                        null // TODO: add type mapping
                )
        );
        vars.add(namedVariable);

        return new J.VariableDeclarations(
                randomId(),
                prefix,
                markers,
                emptyList(),
                emptyList(), // TODO: requires updates to handle kotlin specific modifiers.
                typeExpression,
                null,
                dimensionsAfterName,
                vars);
    }

    @Override
    public J visitPropertyAccessExpression(FirPropertyAccessExpression propertyAccessExpression, ExecutionContext ctx) {
        if (!propertyAccessExpression.getAnnotations().isEmpty() ||
                !propertyAccessExpression.getTypeArguments().isEmpty() ||
                propertyAccessExpression.getExplicitReceiver() != null ||
                !(propertyAccessExpression.getDispatchReceiver() instanceof FirNoReceiverExpression) ||
                !(propertyAccessExpression.getExtensionReceiver() instanceof FirNoReceiverExpression)) {
            throw new IllegalStateException("Implement me.");
        }

        return convert(propertyAccessExpression.getCalleeReference(), ctx);
    }

    @Override
    public J visitResolvedNamedReference(FirResolvedNamedReference resolvedNamedReference, ExecutionContext ctx) {
        return convertToIdentifier(resolvedNamedReference.getName());
    }

    @Override
    public J visitReturnExpression(FirReturnExpression returnExpression, ExecutionContext ctx) {
        if (returnExpression.getResult() instanceof FirUnitExpression) {
            Space prefix = whitespace();
            if (source.startsWith("return", cursor)) {
                throw new IllegalStateException("Implement me.");
            }

            return new K.Return(randomId(), prefix, Markers.EMPTY, null);
        }

        return visitElement(returnExpression.getResult(), ctx);
    }

    @Override
    public J visitResolvedTypeRef(FirResolvedTypeRef resolvedTypeRef, ExecutionContext ctx) {
        if (resolvedTypeRef.getDelegatedTypeRef() != null) {
            return visitElement(resolvedTypeRef.getDelegatedTypeRef(), ctx);
        }
        throw new IllegalStateException("Implement me.");
    }

    @Override
    public J visitSimpleFunction(FirSimpleFunction simpleFunction, ExecutionContext ctx) {
        Space prefix = whitespace();
        Markers markers = Markers.EMPTY;
        // Not used until it's possible to handle K.Modifiers.
        List<J> modifiers = emptyList();
        if (simpleFunction.getSource() != null) {
//            PsiChildRange psiChildRange = PsiUtilsKt.getAllChildren(((KtRealPsiSourceElement) simpleFunction.getSource()).getPsi());
//            if (psiChildRange.getFirst() instanceof KtDeclarationModifierList) {
//                KtDeclarationModifierList modifierList = (KtDeclarationModifierList) psiChildRange.getFirst();
//                modifiers = getModifiers(modifierList);
//            }
        }

        List<J.Annotation> kindAnnotations = emptyList(); // TODO: the last annotations in modifiersAndAnnotations should be added to the fun.

        markers = markers.addIfAbsent(new MethodClassifier(randomId(), sourceBefore("fun")));
        String methodName = "";
        if ("<no name provided>".equals(simpleFunction.getName().asString())) {
            // Extract name from source.
            throw new IllegalStateException("Implement me.");
        } else {
            methodName = simpleFunction.getName().asString();
        }

        J.Identifier name = new J.Identifier(
                randomId(),
                sourceBefore(simpleFunction.getName().asString()),
                Markers.EMPTY,
                methodName,
                null,
                null);

        JContainer<Statement> params;
        Space paramFmt = sourceBefore("(");
        params = !simpleFunction.getValueParameters().isEmpty() ?
                JContainer.build(paramFmt, convertAll(simpleFunction.getValueParameters(), commaDelim, t -> sourceBefore(")"), ctx), Markers.EMPTY) :
                JContainer.build(paramFmt, singletonList(padRight(new J.Empty(randomId(), sourceBefore(")"), Markers.EMPTY), EMPTY)), Markers.EMPTY);

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
        J.Block body = convertOrNull(simpleFunction.getBody(), ctx);

        return new J.MethodDeclaration(
                randomId(),
                prefix,
                markers,
                emptyList(), // TODO
                emptyList(), // TODO
                null, // TODO
                returnTypeExpression,
                new J.MethodDeclaration.IdentifierWithAnnotations(name, emptyList()),
                params, // TODO
                null, // TODO
                body,
                null,
                null); // TODO
    }

    @Override
    public J visitTypeParameter(FirTypeParameter typeParameter, ExecutionContext ctx) {
        if (!typeParameter.getAnnotations().isEmpty()) {
            throw new IllegalStateException("Implement me.");
        }

        Space prefix = whitespace();
        List<J.Annotation> annotations = emptyList();

        Expression name = buildName(typeParameter.getName().asString())
                .withPrefix(sourceBefore(typeParameter.getName().asString()));

        // TODO: add support for bounds. Bounds often exist regardless of if bounds are specified.
        JContainer<TypeTree> bounds = JContainer.empty();
//        JContainer<TypeTree> bounds = typeParameter.getBounds().isEmpty() ? null :
//                JContainer.build(whitespace(),
//                        convertAll(typeParameter.getBounds(), t -> sourceBefore(","), noDelim, ctx), Markers.EMPTY);

        return new J.TypeParameter(randomId(), prefix, Markers.EMPTY, annotations, name, bounds);
    }

    @Override
    public J visitTypeProjectionWithVariance(FirTypeProjectionWithVariance typeProjectionWithVariance, ExecutionContext ctx) {
        // TODO: Temp. sort out how type references work and why FirTypeProjectionWithVariance contain variance even when not specified in code. I.E., Int.
        return visitResolvedTypeRef((FirResolvedTypeRef) typeProjectionWithVariance.getTypeRef(), ctx);
    }

    @Override
    public J visitUserTypeRef(FirUserTypeRef userTypeRef, ExecutionContext ctx) {
        JavaType type = null; // TODO: add type mapping. Note: typeRef does not contain a reference to the symbol. The symbol exists on the FIR element.
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

                return new J.ParameterizedType(
                        randomId(),
                        prefix,
                        Markers.EMPTY,
                        ident,
                        JContainer.build(typeArgPrefix, parameters, Markers.EMPTY)
                );
            } else {
                return ident.withPrefix(prefix);
            }
        } else {
            throw new IllegalStateException("Implement me.");
        }
    }

    /**
     * Note: both ValueParameter and Property implement FirVariable.
     * Most of the code is the same, but the visits are separate until we can figure out how to handle the differences.
     */
    @Override
    public J visitValueParameter(FirValueParameter valueParameter, ExecutionContext ctx) {
        Space prefix = whitespace();

        List<J> modifiers = emptyList();
        if (valueParameter.getSource() != null) {
//            PsiChildRange psiChildRange = PsiUtilsKt.getAllChildren(((KtRealPsiSourceElement) valueParameter.getSource()).getPsi());
//            if (psiChildRange.getFirst() instanceof KtDeclarationModifierList) {
//                KtDeclarationModifierList modifierList = (KtDeclarationModifierList) psiChildRange.getFirst();
//                modifiers = getModifiers(modifierList);
//            }
        }

        List<J.Annotation> annotations = emptyList(); // TODO: the last annotations in modifiers should be added.
        Markers markers = Markers.EMPTY;

        List<JRightPadded<J.VariableDeclarations.NamedVariable>> vars = new ArrayList<>(1); // adjust size if necessary
        Space namePrefix = EMPTY;
        String valueName = "";
        if ("<no name provided>".equals(valueParameter.getName().toString())) {
            KtSourceElement sourceElement = valueParameter.getSource();
            if (sourceElement == null) {
                throw new IllegalStateException("Unexpected null source.");
            } else {
                KtLightSourceElement element = (KtLightSourceElement) sourceElement;
            }
        } else {
            valueName = valueParameter.getName().asString();
            namePrefix = sourceBefore(valueName);
        }

        J.Identifier name = new J.Identifier(
                randomId(),
                EMPTY,
                Markers.EMPTY,
                valueName,
                null, // TODO: add type mapping and set type
                null); // TODO: add type mapping and set variable type

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
                    typeExpression = new K.FunctionType(randomId(), (TypedTree) j);
                }
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
                        Markers.EMPTY,
                        name,
                        dimensionsAfterName,
                        valueParameter.getInitializer() != null ? padLeft(sourceBefore("="), (Expression) visitExpression(valueParameter.getInitializer(), ctx)) : null,
                        null // TODO: add type mapping
                )
        );
        vars.add(namedVariable);

        return new J.VariableDeclarations(
                randomId(),
                prefix,
                markers,
                emptyList(),
                emptyList(), // TODO: requires updates to handle kotlin specific modifiers.
                typeExpression,
                null,
                dimensionsAfterName,
                vars);
    }

    @Override
    public J visitVariableAssignment(FirVariableAssignment variableAssignment, ExecutionContext ctx) {
        Space prefix = whitespace();
        return new J.Assignment(
                randomId(),
                prefix,
                Markers.EMPTY,
                convert(variableAssignment.getLValue(), ctx),
                padLeft(sourceBefore("="), convert(variableAssignment.getRValue(), ctx)),
                typeMapping.type(variableAssignment));
    }

    @Override
    public J visitWhenBranch(FirWhenBranch whenBranch, ExecutionContext ctx) {
        Space prefix = whitespace();
        if (source.substring(cursor).startsWith("when")) {
            throw new IllegalStateException("Implement me.");
        } else if (source.substring(cursor).startsWith("if")) {
            skip("if");
        } else if (!(whenBranch.getCondition() instanceof FirElseIfTrueCondition ||
                whenBranch.getCondition() instanceof FirEqualityOperatorCall)) {
            throw new IllegalStateException("Implement me.");
        }

        boolean singleExpression = whenBranch.getResult() instanceof FirSingleExpressionBlock;
        if (whenBranch.getCondition() instanceof FirElseIfTrueCondition) {
            FirElement result = singleExpression ? ((FirSingleExpressionBlock) whenBranch.getResult()).getStatement() : whenBranch.getResult();
            J j = visitElement(result, ctx);
            return j.withPrefix(prefix);
        } else {
            Space controlParenPrefix = whitespace();
            skip("(");
            J.ControlParentheses<Expression> controlParentheses = new J.ControlParentheses<Expression>(randomId(), controlParenPrefix, Markers.EMPTY,
                        convert(whenBranch.getCondition(), t -> sourceBefore(")"), ctx));

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
        // TODO: implement when expressions.
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

    /**
     *
     * @param firElement
     * @param ctx
     * @return
     */
    @Override
    public J visitElement(FirElement firElement, ExecutionContext ctx) {
        if (firElement instanceof FirErrorNamedReference) {
            return visitErrorNamedReference((FirErrorNamedReference) firElement, ctx);
        } else if (firElement instanceof FirAnonymousFunction) {
            return visitAnonymousFunction((FirAnonymousFunction) firElement, ctx);
        } else if (firElement instanceof FirAnonymousFunctionExpression) {
            return visitAnonymousFunctionExpression((FirAnonymousFunctionExpression) firElement, ctx);
        } else if (firElement instanceof FirBlock) {
            return visitBlock((FirBlock) firElement, ctx);
        }  else if (firElement instanceof FirClass) {
            return visitClass((FirClass) firElement, ctx);
        } else if (firElement instanceof FirConstExpression) {
            return visitConstExpression((FirConstExpression<? extends Object>) firElement, ctx);
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
        } else if (firElement instanceof FirProperty) {
            return visitProperty((FirProperty) firElement, ctx);
        } else if (firElement instanceof FirPropertyAccessExpression) {
            return visitPropertyAccessExpression((FirPropertyAccessExpression) firElement, ctx);
        } else if (firElement instanceof FirResolvedNamedReference) {
            return visitResolvedNamedReference((FirResolvedNamedReference) firElement, ctx);
        } else if (firElement instanceof FirResolvedTypeRef) {
            return visitResolvedTypeRef((FirResolvedTypeRef) firElement, ctx);
        } else if (firElement instanceof FirReturnExpression) {
            return visitReturnExpression((FirReturnExpression) firElement, ctx);
        } else if (firElement instanceof FirSimpleFunction) {
            return visitSimpleFunction((FirSimpleFunction) firElement, ctx);
        } else if (firElement instanceof FirTypeParameter) {
            return visitTypeParameter((FirTypeParameter) firElement, ctx);
        } else if (firElement instanceof FirTypeProjectionWithVariance) {
            return visitTypeProjectionWithVariance((FirTypeProjectionWithVariance) firElement, ctx);
        } else if (firElement instanceof FirUserTypeRef) {
            return visitUserTypeRef((FirUserTypeRef) firElement, ctx);
        } else if (firElement instanceof FirValueParameter) {
            return visitValueParameter((FirValueParameter) firElement, ctx);
        } else if (firElement instanceof FirVarargArgumentsExpression) {
            return visitVarargArgumentsExpression((FirVarargArgumentsExpression) firElement, ctx);
        } else if (firElement instanceof FirVariableAssignment) {
            return visitVariableAssignment((FirVariableAssignment) firElement, ctx);
        }  else if (firElement instanceof FirWhenBranch) {
            return visitWhenBranch((FirWhenBranch) firElement, ctx);
        } else if (firElement instanceof FirWhenExpression) {
            return visitWhenExpression((FirWhenExpression) firElement, ctx);
        }

        throw new IllegalStateException("Implement me.");
    }

    private final Function<FirElement, Space> commaDelim = ignored -> sourceBefore(",");
    private final Function<FirElement, Space> noDelim = ignored -> EMPTY;

    private void skip(@Nullable String token) {
        if (source.startsWith(token, cursor)) {
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

    private <T extends TypeTree & Expression> T buildName(String fullyQualifiedName) {
        String[] parts = fullyQualifiedName.split("\\.");

        String fullName = "";
        Expression expr = null;
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (i == 0) {
                fullName = part;
                expr = new J.Identifier(randomId(), EMPTY, Markers.EMPTY, part, null, null);
            } else {
                fullName += "." + part;

                int endOfPrefix = indexOfNextNonWhitespace(0, part);
                Space identFmt = endOfPrefix > 0 ? format(part.substring(0, endOfPrefix)) : EMPTY;

                Matcher whitespaceSuffix = whitespaceSuffixPattern.matcher(part);
                //noinspection ResultOfMethodCallIgnored
                whitespaceSuffix.matches();
                Space namePrefix = i == parts.length - 1 ? Space.EMPTY : format(whitespaceSuffix.group(1));

                expr = new J.FieldAccess(
                        randomId(),
                        EMPTY,
                        Markers.EMPTY,
                        expr,
                        padLeft(namePrefix, new J.Identifier(randomId(), identFmt, Markers.EMPTY, part.trim(), null, null)),
                        (Character.isUpperCase(part.charAt(0)) || i == parts.length - 1) ?
                                JavaType.ShallowClass.build(fullName) :
                                null
                );
            }
        }

        //noinspection unchecked,ConstantConditions
        return (T) expr;
    }

    @SuppressWarnings("unchecked")
    private <J2 extends J> J2 convert(FirElement t, ExecutionContext ctx) {
        return (J2) visitElement(t, ctx);
    }

    @SuppressWarnings("unchecked")
    private <J2 extends J> JRightPadded<J2> convert(FirElement t, Function<FirElement, Space> suffix, ExecutionContext ctx) {
        J2 j = (J2) visitElement(t, ctx);
        @SuppressWarnings("ConstantConditions") JRightPadded<J2> rightPadded = j == null ? null :
                new JRightPadded<>(j, suffix.apply(t), Markers.EMPTY);
        cursor(max(endPos(t), cursor)); // if there is a non-empty suffix, the cursor may have already moved past it
        return rightPadded;
    }

    @Nullable
    private <T extends J> T convertOrNull(@Nullable FirElement t, ExecutionContext ctx) {
        return t == null ? null : convert(t, ctx);
    }

    @Nullable
    private <J2 extends J> JRightPadded<J2> convertOrNull(@Nullable FirElement t, Function<FirElement, Space> suffix, ExecutionContext ctx) {
        return t == null ? null : convert(t, suffix, ctx);
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

    private J.Identifier convertToIdentifier(Name name) {
        Space prefix = sourceBefore(name.asString());

        JavaType type = null; // TODO: add type mapping. Note: typeRef does not contain a reference to the symbol. The symbol exists on the FIR element.
        return new J.Identifier(randomId(),
                prefix,
                Markers.EMPTY,
                name.asString(),
                type,
                null);
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

    // TODO: parse annotation composite and create J.Annotation.
    private J.Annotation mapAnnotation(CompositeElement compositeElement) {
        Space prefix = whitespace();
        return new J.Annotation(randomId(), prefix, Markers.EMPTY, null, JContainer.empty());
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

    private J mapSourceElement(KtSourceElement sourceElement) {
        if (sourceElement instanceof KtRealPsiSourceElement) {
            PsiElement psiElement = ((KtRealPsiSourceElement) sourceElement).getPsi();
            if (psiElement instanceof KtTypeReference) {
                return mapTypeReference((KtTypeReference) psiElement);
            }
        }
        throw new IllegalStateException("Implement me.");
    }

    private J mapNameReferenceExpression(KtNameReferenceExpression nameReferenceExpression) {
        Space prefix = sourceBefore(nameReferenceExpression.getReferencedName());
        String name = nameReferenceExpression.getReferencedName();
        return new J.Identifier(randomId(), prefix, Markers.EMPTY, name, null, null); // TODO add types.
    }

    private J mapTypeReference(KtTypeReference typeReference) {
        PsiChildRange childRange = PsiUtilsKt.getAllChildren(typeReference);
        PsiElement firstChild = childRange.getFirst();
        if (firstChild instanceof KtFunctionType) {
            return mapFunctionType((KtFunctionType) firstChild);
        }
        throw new IllegalStateException("Implement me.");
    }

    private J mapFunctionType(KtFunctionType functionType) {
        throw new IllegalStateException("Implement me.");
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
