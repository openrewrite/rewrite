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
package org.openrewrite.kotlin.internal;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.KtNodeTypes;
import org.jetbrains.kotlin.com.intellij.lang.ASTNode;
import org.jetbrains.kotlin.com.intellij.psi.*;
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement;
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.PsiErrorElementImpl;
import org.jetbrains.kotlin.com.intellij.psi.tree.IElementType;
import org.jetbrains.kotlin.com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.kotlin.fir.ClassMembersKt;
import org.jetbrains.kotlin.fir.FirElement;
import org.jetbrains.kotlin.fir.FirSession;
import org.jetbrains.kotlin.fir.declarations.FirFile;
import org.jetbrains.kotlin.fir.declarations.FirFunction;
import org.jetbrains.kotlin.fir.declarations.FirResolvedImport;
import org.jetbrains.kotlin.fir.declarations.FirVariable;
import org.jetbrains.kotlin.fir.expressions.FirConstExpression;
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall;
import org.jetbrains.kotlin.fir.expressions.FirStringConcatenationCall;
import org.jetbrains.kotlin.fir.references.FirResolvedCallableReference;
import org.jetbrains.kotlin.fir.resolve.LookupTagUtilsKt;
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag;
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol;
import org.jetbrains.kotlin.fir.symbols.impl.*;
import org.jetbrains.kotlin.fir.types.ConeClassLikeType;
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef;
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.parsing.ParseUtilsKt;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.psi.psiUtil.PsiUtilsKt;
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes;
import org.jetbrains.kotlin.types.Variance;
import org.openrewrite.ExecutionContext;
import org.openrewrite.FileAttributes;
import org.openrewrite.Tree;
import org.openrewrite.internal.EncodingDetectingInputStream;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.NonNull;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.marker.ImplicitReturn;
import org.openrewrite.java.marker.OmitParentheses;
import org.openrewrite.java.marker.TrailingComma;
import org.openrewrite.java.tree.*;
import org.openrewrite.kotlin.KotlinTypeMapping;
import org.openrewrite.kotlin.marker.*;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.marker.Markers;
import org.openrewrite.style.NamedStyles;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.openrewrite.Tree.randomId;

/**
 * PSI based parser
 */
public class KotlinTreeParserVisitor extends KtVisitor<J, ExecutionContext> {
    private final KotlinSource kotlinSource;
    private final FirSession firSession;
    private final KotlinTypeMapping typeMapping;
    private final PsiElementAssociations psiElementAssociations;
    private final PsiElementAssociations2 psiElementAssociations2;
    private final List<NamedStyles> styles;
    private final Path sourcePath;

    @Nullable
    private final FileAttributes fileAttributes;

    private final Charset charset;
    private final Boolean charsetBomMarked;
    private final Stack<KtElement> ownerStack = new Stack<>();
    private final ExecutionContext executionContext;

    public KotlinTreeParserVisitor(KotlinSource kotlinSource,
                                   FirSession firSession,
                                   KotlinTypeMapping typeMapping,
                                   PsiElementAssociations psiElementAssociations,
                                   PsiElementAssociations2 psiElementAssociations2,
                                   List<NamedStyles> styles,
                                   @Nullable Path relativeTo,
                                   ExecutionContext ctx) {
        this.kotlinSource = kotlinSource;
        this.firSession = firSession;
        this.typeMapping = typeMapping;
        this.psiElementAssociations = psiElementAssociations;
        this.psiElementAssociations2 = psiElementAssociations2;
        this.styles = styles;
        sourcePath = kotlinSource.getInput().getRelativePath(relativeTo);
        fileAttributes = kotlinSource.getInput().getFileAttributes();
        EncodingDetectingInputStream stream = kotlinSource.getInput().getSource(ctx);
        charset = stream.getCharset();
        charsetBomMarked = stream.isCharsetBomMarked();
        ownerStack.push(kotlinSource.getKtFile());
        executionContext = ctx;
    }

    public K.CompilationUnit parse() {
        return (K.CompilationUnit) visitKtFile(kotlinSource.getKtFile(), executionContext);
    }

    @Override
    public J visitParenthesizedExpression(KtParenthesizedExpression expression, ExecutionContext data) {
        assert expression.getExpression() != null;

        PsiElement rPar = expression.getLastChild();
        if (rPar == null || !(")".equals(rPar.getText()))) {
            throw new UnsupportedOperationException("TODO");
        }

        return new J.Parentheses<>(
                randomId(),
                prefix(expression),
                Markers.EMPTY,
                padRight(expression.getExpression().accept(this, data), prefix(rPar))
        );
    }

    @Override
    public J visitForExpression(KtForExpression expression, ExecutionContext data) {
        return new J.ForEachLoop(
                randomId(),
                prefix(expression),
                Markers.EMPTY,
                new J.ForEachLoop.Control(
                        randomId(),
                        prefix(expression.getLeftParenthesis()),
                        Markers.EMPTY,
                        padRight((J.VariableDeclarations) expression.getLoopParameter().accept(this, data), suffix(expression.getLoopParameter())),
                        padRight(expression.getLoopRange().accept(this, data)
                                .withPrefix(prefix(expression.getLoopRange().getParent())), suffix(expression.getLoopRange().getParent()))
                ),
                padRight(expression.getBody().accept(this, data)
                        .withPrefix(prefix(expression.getBody().getParent())), suffix(expression.getBody()))
        );
    }

    @Override
    public J visitAnnotatedExpression(KtAnnotatedExpression expression, ExecutionContext data) {
        List<KtAnnotationEntry> ktAnnotations = expression.getAnnotationEntries();
        List<J.Annotation> annotations = new ArrayList<>(ktAnnotations.size());

        for (int i = 0; i < ktAnnotations.size(); i++) {
            KtAnnotationEntry ktAnnotation = ktAnnotations.get(i);
            J.Annotation anno = (J.Annotation) ktAnnotation.accept(this, data);
            if (i == 0) {
                anno = anno.withPrefix(merge(prefix(expression), anno.getPrefix()));
            }
            annotations.add(anno);
        }

        return new K.AnnotatedExpression(
                randomId(),
                Markers.EMPTY,
                annotations,
                convertToExpression(expression.getBaseExpression().accept(this, data))
        );
    }

    @Override
    public J visitAnnotationUseSiteTarget(KtAnnotationUseSiteTarget annotationTarget, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitAnonymousInitializer(KtAnonymousInitializer initializer, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitArrayAccessExpression(KtArrayAccessExpression expression, ExecutionContext data) {
        Markers markers = Markers.EMPTY;
        boolean hasExplicitReceiver = false;
        boolean implicitExtensionFunction = false;
        Expression selectExpr = convertToExpression(expression.getArrayExpression().accept(this, data));
        JRightPadded<Expression> select = padRight(selectExpr, suffix(expression.getArrayExpression()));
        JContainer<Expression> typeParams = null;
        J.Identifier name = createIdentifier("get", Space.EMPTY, methodInvocationType(expression));

        markers = markers.addIfAbsent(new IndexedAccess(randomId()));

        List<KtExpression> indexExpressions = expression.getIndexExpressions();
        if (indexExpressions.size() != 1) {
            throw new UnsupportedOperationException("TODO");
        }

        List<JRightPadded<Expression>> expressions = new ArrayList<>();
        KtExpression indexExp = indexExpressions.get(0);
        expressions.add(padRight(convertToExpression(indexExp.accept(this, data)), suffix(indexExp)));
        JContainer<Expression> args = JContainer.build(Space.EMPTY, expressions, markers); // expression.getIndicesNode().accept(this, data).withPrefix(Space.EMPTY);

        return new J.MethodInvocation(
                randomId(),
                Space.EMPTY,
                markers,
                select,
                typeParams,
                name,
                args,
                methodInvocationType(expression)
        );
    }

    @Override
    public J visitBackingField(KtBackingField accessor, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitBinaryWithTypeRHSExpression(KtBinaryExpressionWithTypeRHS expression, ExecutionContext data) {
        IElementType type = expression.getOperationReference().getReferencedNameElementType();

        if (type == KtTokens.AS_KEYWORD || type == KtTokens.AS_SAFE) {
            J clazz = expression.getRight().accept(this, data).withPrefix(prefix(expression.getRight()));
            Markers markers = Markers.EMPTY;
            if (type == KtTokens.AS_SAFE) {
                markers = markers.addIfAbsent(new IsNullSafe(randomId(), Space.EMPTY));
            }

            return new J.TypeCast(
                    randomId(),
                    prefix(expression),
                    markers,
                    new J.ControlParentheses(
                            randomId(),
                            suffix(expression.getLeft()),
                            Markers.EMPTY,
                            JRightPadded.build(clazz)
                    ),
                    convertToExpression(expression.getLeft().accept(this, data))
            );
        }

        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitBlockStringTemplateEntry(KtBlockStringTemplateEntry entry, ExecutionContext data) {
        J tree = entry.getExpression().accept(this, data);
        boolean inBraces = true;

        return new K.KString.Value(
                randomId(),
                Space.EMPTY,
                Markers.EMPTY,
                tree,
                suffix(entry.getExpression()),
                inBraces
        );
    }

    @Override
    public J visitBreakExpression(KtBreakExpression expression, ExecutionContext data) {
        return new J.Break(
                randomId(),
                prefix(expression),
                Markers.EMPTY,
                expression.getTargetLabel() != null ? createIdentifier(expression.getTargetLabel().getIdentifier(), null) : null
        );
    }

    @Override
    public J visitCallableReferenceExpression(KtCallableReferenceExpression expression, ExecutionContext data) {
        FirResolvedCallableReference reference = (FirResolvedCallableReference) psiElementAssociations.primary(expression.getCallableReference());
        JavaType.Method methodReferenceType = null;
        if (reference.getResolvedSymbol() instanceof FirNamedFunctionSymbol) {
            methodReferenceType = typeMapping.methodDeclarationType(
                    ((FirNamedFunctionSymbol) reference.getResolvedSymbol()).getFir(),
                    TypeUtils.asFullyQualified(type(expression.getReceiverExpression())),
                    owner(expression)
            );
        }
        JavaType.Variable fieldReferenceType = null;
        if (reference.getResolvedSymbol() instanceof FirPropertySymbol) {
            fieldReferenceType = typeMapping.variableType(
                    (FirVariableSymbol<FirVariable>) reference.getResolvedSymbol(),
                    TypeUtils.asFullyQualified(type(expression.getReceiverExpression())),
                    owner(expression)
            );
        }

        JRightPadded<Expression> receiver;
        if (expression.getReceiverExpression() == null) {
            receiver = padRight(new J.Empty(randomId(), Space.EMPTY, Markers.EMPTY),
                    prefix(expression.findColonColon()));
        } else {
            receiver = padRight(convertToExpression(expression.getReceiverExpression().accept(this, data)),
                    prefix(expression.findColonColon()));
        }

        return new J.MemberReference(
                randomId(),
                prefix(expression),
                Markers.EMPTY,
                receiver,
                null,
                padLeft(prefix(expression.getLastChild()), expression.getCallableReference().accept(this, data).withPrefix(Space.EMPTY)),
                type(expression.getCallableReference()),
                methodReferenceType,
                fieldReferenceType
        );
    }

    private FirBasedSymbol<?> owner(PsiElement element) {
        KtElement owner = ownerStack.peek() == element ? ownerStack.get(ownerStack.size() - 2) : ownerStack.peek();
        if (owner instanceof KtDeclaration) {
            return psiElementAssociations.symbol(((KtDeclaration) owner));
        } else if (owner instanceof KtFile) {
            return ((FirFile) psiElementAssociations.primary(owner)).getSymbol();
        }
        return null;
    }

    @Override
    public J visitCatchSection(KtCatchClause catchClause, ExecutionContext data) {
        J.VariableDeclarations paramDecl = (J.VariableDeclarations) catchClause.getCatchParameter().accept(this, data);
        J.ControlParentheses param = new J.ControlParentheses<>(
                randomId(),
                prefix(catchClause.getParameterList()),
                Markers.EMPTY,
                padRight(paramDecl, prefix(catchClause.getParameterList().getRightParenthesis()))
        );

        J.Block body = (J.Block) catchClause.getCatchBody().accept(this, data);
        return new J.Try.Catch(
                randomId(),
                prefix(catchClause),
                Markers.EMPTY,
                param,
                body
        );
    }

    @Override
    public J visitClassInitializer(KtClassInitializer initializer, ExecutionContext data) {
        J.Block staticInit = (J.Block) initializer.getBody().accept(this, data).withPrefix(prefix(initializer));
        staticInit = staticInit.getPadding().withStatic(padRight(true, prefix(initializer.getBody())));
        return staticInit;
    }

    @Override
    public J visitClassLiteralExpression(KtClassLiteralExpression expression, ExecutionContext data) {
        return new J.MemberReference(
                randomId(),
                prefix(expression),
                Markers.EMPTY,
                padRight(convertToExpression(expression.getReceiverExpression().accept(this, data)),
                        prefix(expression.findColonColon())),
                null,
                padLeft(prefix(expression.getLastChild()), createIdentifier("class", Space.EMPTY, null)),
                type(expression),
                null,
                null
        );
    }

    @Override
    public J visitClassOrObject(KtClassOrObject classOrObject, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitCollectionLiteralExpression(KtCollectionLiteralExpression expression, ExecutionContext data) {
        JContainer<Expression> elements;
        if (expression.getInnerExpressions().isEmpty()) {
            elements = JContainer.build(singletonList(
                    padRight(new J.Empty(randomId(), Space.EMPTY, Markers.EMPTY), prefix(expression.getRightBracket()))));
        } else {
            List<JRightPadded<Expression>> rps = new ArrayList<>(expression.getInnerExpressions().size());
            for (KtExpression ktExpression : expression.getInnerExpressions()) {
                rps.add(padRight(convertToExpression(ktExpression.accept(this, data)), suffix(ktExpression)));
            }

            if (expression.getTrailingComma() != null) {
                rps = ListUtils.mapLast(rps, rp -> rp.withMarkers(rp.getMarkers()
                        .addIfAbsent(new TrailingComma(randomId(), suffix(expression.getTrailingComma())))));
            }

            elements = JContainer.build(Space.EMPTY, rps, Markers.EMPTY);
        }

        return new K.ListLiteral(
                randomId(),
                prefix(expression),
                Markers.EMPTY,
                elements,
                type(expression)
        );
    }

    @Override
    public J visitConstructorCalleeExpression(KtConstructorCalleeExpression constructorCalleeExpression, ExecutionContext data) {
        J j = constructorCalleeExpression.getTypeReference().accept(this, data);
        return j.withPrefix(merge(j.getPrefix(), prefix(constructorCalleeExpression)));
    }

    @Override
    public J visitConstructorDelegationCall(KtConstructorDelegationCall call, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitContextReceiverList(KtContextReceiverList contextReceiverList, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitContinueExpression(KtContinueExpression expression, ExecutionContext data) {
        return new J.Continue(
                randomId(),
                prefix(expression.getParent()),
                Markers.EMPTY,
                expression.getTargetLabel() != null ? createIdentifier(expression.getTargetLabel().getIdentifier(), null) : null
        );
    }

    @Override
    public J visitDeclaration(KtDeclaration dcl, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitDelegatedSuperTypeEntry(KtDelegatedSuperTypeEntry specifier, ExecutionContext data) {
        TypeTree element = (TypeTree) specifier.getTypeReference().accept(this, data);
        Expression expr = convertToExpression(specifier.getDelegateExpression().accept(this, data));
        return new K.DelegatedSuperType(randomId(), Markers.EMPTY, element, suffix(specifier.getTypeReference()), expr).withPrefix(prefix(specifier));
    }

    @Override
    public J visitDestructuringDeclarationEntry(KtDestructuringDeclarationEntry multiDeclarationEntry, ExecutionContext data) {
        return createIdentifier(multiDeclarationEntry, type(multiDeclarationEntry)).withPrefix(prefix(multiDeclarationEntry));
    }

    @Override
    public J visitDoubleColonExpression(KtDoubleColonExpression expression, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitDoWhileExpression(KtDoWhileExpression expression, ExecutionContext data) {
        return new J.DoWhileLoop(
                randomId(),
                prefix(expression),
                Markers.EMPTY,
                // FIXME NPE if no body
                JRightPadded.build(expression.getBody().accept(this, data)
                        .withPrefix(prefix(expression.getBody().getParent()))
                ),
                padLeft(prefix(expression.getWhileKeyword()), mapControlParentheses(expression.getCondition(), data).withPrefix(prefix(expression.getLeftParenthesis())))
        );
    }

    private J.ControlParentheses<Expression> mapControlParentheses(KtExpression expression, ExecutionContext data) {
        return new J.ControlParentheses<>(
                randomId(),
                prefix(expression.getParent()),
                Markers.EMPTY,
                padRight(convertToExpression(expression.accept(this, data))
                        .withPrefix(prefix(expression.getParent())), suffix(expression.getParent())
                )
        );
    }

    @Override
    public J visitDynamicType(KtDynamicType type, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitEnumEntry(KtEnumEntry enumEntry, ExecutionContext data) {
        List<J.Annotation> annotations = new ArrayList<>();
        if (!enumEntry.getAnnotationEntries().isEmpty()) {
            throw new UnsupportedOperationException("TODO");
        }

        J.Identifier name = createIdentifier(enumEntry.getNameIdentifier(), type(enumEntry));
        J.NewClass initializer = null;

        if (enumEntry.getInitializerList() != null) {
            initializer = (J.NewClass) enumEntry.getInitializerList().accept(this, data);
        }

        if (enumEntry.getBody() != null) {
            Markers markers = Markers.EMPTY.addIfAbsent(new Implicit(randomId()));
            JContainer<Expression> args = JContainer.empty();
            args = args.withMarkers(Markers.build(singletonList(new OmitParentheses(randomId()))));
            J.Block body = (J.Block) enumEntry.getBody().accept(this, data).withPrefix(Space.EMPTY);

            initializer = new J.NewClass(
                    randomId(),
                    prefix(enumEntry.getBody()),
                    markers,
                    null,
                    Space.EMPTY,
                    null,
                    args,
                    body,
                    null
            );
        }

        return new J.EnumValue(
                randomId(),
                prefix(enumEntry),
                Markers.EMPTY,
                annotations,
                name,
                initializer
        );
    }

    @Override
    public J visitEscapeStringTemplateEntry(KtEscapeStringTemplateEntry entry, ExecutionContext data) {
        throw new UnsupportedOperationException("Not required");
    }

    @Override
    public J visitExpression(KtExpression expression, ExecutionContext data) {
        if (expression instanceof KtFunctionLiteral) {
            KtFunctionLiteral ktFunctionLiteral = (KtFunctionLiteral) expression;
            Markers markers = Markers.EMPTY;
            ktFunctionLiteral.getLBrace();
            boolean hasBraces = true;
            boolean omitDestruct = false;

            List<KtParameter> valueParameters = ktFunctionLiteral.getValueParameters();
            List<JRightPadded<J>> valueParams = new ArrayList<>(valueParameters.size());

            if (!valueParameters.isEmpty()) {
                for (int i = 0; i < valueParameters.size(); i++) {
                    KtParameter ktParameter = valueParameters.get(i);
                    J expr = ktParameter.accept(this, data).withPrefix(prefix(ktParameter));
                    valueParams.add(maybeTrailingComma(ktParameter, padRight(expr, suffix(ktParameter)), i == valueParameters.size() - 1));
                }
            } else if (ktFunctionLiteral.getArrow() != null) {
                valueParams.add(padRight(new J.Empty(randomId(), Space.EMPTY, Markers.EMPTY), Space.EMPTY));
            }

            J.Lambda.Parameters params = new J.Lambda.Parameters(randomId(), prefix(ktFunctionLiteral.getValueParameterList()), Markers.EMPTY, false, valueParams);

            J.Block body = ktFunctionLiteral.getBodyExpression().accept(this, data)
                    .withPrefix(prefix(ktFunctionLiteral.getBodyExpression()));
            body = body.withEnd(prefix(ktFunctionLiteral.getRBrace()));

            return new J.Lambda(
                    randomId(),
                    prefix(expression),
                    markers,
                    params,
                    prefix(ktFunctionLiteral.getArrow()),
                    body,
                    null
            );
        }

        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitExpressionWithLabel(KtExpressionWithLabel expression, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitFileAnnotationList(KtFileAnnotationList fileAnnotationList, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitFinallySection(KtFinallySection finallySection, ExecutionContext data) {
        return finallySection.getFinalExpression().accept(this, data);
    }

    @Override
    public J visitFunctionType(KtFunctionType type, ExecutionContext data) {
        List<JRightPadded<TypeTree>> params;
        if (type.getParameters().isEmpty()) {
            params = singletonList(JRightPadded
                    .<TypeTree>build(new J.Empty(randomId(), prefix(type.getParameterList().getNode().findChildByType(KtTokens.RPAR).getPsi()), Markers.EMPTY))
                    .withAfter(Space.EMPTY));
        } else {
            params = new ArrayList<>();
            List<KtParameter> parameters = type.getParameters();
            for (int i = 0; i < parameters.size(); i++) {
                KtParameter ktParameter = parameters.get(i);
                TypeTree typeTree;
                if (ktParameter.getNameIdentifier() != null) {
                    typeTree = new K.FunctionType.Parameter(
                            randomId(),
                            Markers.EMPTY.addIfAbsent(new TypeReferencePrefix(randomId(), prefix(ktParameter.getColon()))),
                            createIdentifier(ktParameter.getNameIdentifier(), null),
                            (TypeTree) ktParameter.getTypeReference().accept(this, data)
                    );
                } else {
                    typeTree = (TypeTree) ktParameter.getTypeReference().accept(this, data);
                }
                params.add(maybeTrailingComma(ktParameter, padRight(typeTree.withPrefix(prefix(ktParameter)), suffix(ktParameter)), i == parameters.size() - 1));
            }
        }

        JContainer<TypeTree> parameters = JContainer.build(prefix(type.getParameterList()), params, Markers.EMPTY);
        if (type.getFirstChild() == type.getParameterList()) {
            parameters = parameters.withBefore(prefix(type));
        }

        return new K.FunctionType(
                randomId(),
                prefix(type),
                Markers.EMPTY, //.addIfAbsent(new IsNullable(randomId(), Space.EMPTY)), // TODO
                emptyList(), // TODO
                emptyList(), // TODO
                type.getReceiver() != null ? padRight((NameTree) type.getReceiverTypeReference().accept(this, data), suffix(type.getReceiver())) : null,
                parameters,
                suffix(type.getParameterList()),
                type.getReturnTypeReference().accept(this, data).withPrefix(prefix(type.getReturnTypeReference()))
        );
    }

    @Override
    public J visitImportAlias(KtImportAlias importAlias, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitImportList(KtImportList importList, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitInitializerList(KtInitializerList list, ExecutionContext data) {
        List<KtSuperTypeListEntry> entries = list.getInitializers();
        if (entries.size() > 1) {
            throw new UnsupportedOperationException("TODO");
        }

        if (!(entries.get(0) instanceof KtSuperTypeCallEntry)) {
            throw new UnsupportedOperationException("TODO");
        }

        TypeTree clazz = null;
        Markers markers = Markers.EMPTY.addIfAbsent(new Implicit(randomId()));

        KtSuperTypeCallEntry superTypeCallEntry = (KtSuperTypeCallEntry) entries.get(0);
        J.Identifier typeTree = (J.Identifier) superTypeCallEntry.getCalleeExpression().accept(this, data);
        JContainer<Expression> args;

        if (!superTypeCallEntry.getValueArguments().isEmpty()) {
            List<JRightPadded<Expression>> expressions = new ArrayList<>(superTypeCallEntry.getValueArguments().size());

            for (ValueArgument valueArgument : superTypeCallEntry.getValueArguments()) {
                if (!(valueArgument instanceof KtValueArgument)) {
                    throw new UnsupportedOperationException("TODO");
                }

                KtValueArgument ktValueArgument = (KtValueArgument) valueArgument;
                expressions.add(padRight(convertToExpression(ktValueArgument.accept(this, data)), suffix(ktValueArgument)));
            }

            args = JContainer.build(prefix(superTypeCallEntry.getValueArgumentList()), expressions, Markers.EMPTY);
        } else {
            KtValueArgumentList ktArgList = superTypeCallEntry.getValueArgumentList();
            args = JContainer.build(
                    prefix(ktArgList),
                    singletonList(padRight(new J.Empty(randomId(), prefix(ktArgList.getRightParenthesis()), Markers.EMPTY), Space.EMPTY)
                    ),
                    markers
            );
        }

        return new J.NewClass(
                randomId(),
                prefix(list),
                markers,
                null,
                Space.EMPTY,
                clazz,
                args,
                null,
                null
        );
    }

    @Override
    public J visitIntersectionType(KtIntersectionType definitelyNotNullType, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitIsExpression(KtIsExpression expression, ExecutionContext data) {
        Markers markers = Markers.EMPTY;

        Expression element = convertToExpression(expression.getLeftHandSide().accept(this, data));

        if (expression.getOperationReference().getReferencedNameElementType() == KtTokens.NOT_IS) {
            markers = markers.addIfAbsent(new NotIs(randomId()));
        }

        J clazz = expression.getTypeReference().accept(this, data);

        return new J.InstanceOf(
                randomId(),
                prefix(expression),
                markers,
                padRight(element, prefix(expression.getOperationReference())),
                clazz,
                null,
                type(expression)
        );
    }

    @Override
    public J visitLabeledExpression(KtLabeledExpression expression, ExecutionContext data) {
        J j = expression.getBaseExpression().accept(this, data);
        return new J.Label(
                randomId(),
                prefix(expression),
                Markers.EMPTY,
                padRight(
                        createIdentifier(expression.getNameIdentifier(), null),
                        suffix(expression.getNameIdentifier())
                ),
                convertToStatement(j)
        );
    }

    @Override
    public J visitLambdaExpression(KtLambdaExpression expression, ExecutionContext data) {
        KtFunctionLiteral functionLiteral = expression.getFunctionLiteral();
        return functionLiteral.accept(this, data).withPrefix(prefix(expression));
    }

    @Override
    public J visitLiteralStringTemplateEntry(KtLiteralStringTemplateEntry entry, ExecutionContext data) {
        PsiElement leaf = entry.getFirstChild();
        if (!(leaf instanceof LeafPsiElement)) {
            throw new UnsupportedOperationException("Unsupported KtStringTemplateEntry child");
        }

        boolean quoted = entry.getPrevSibling().getNode().getElementType() == KtTokens.OPEN_QUOTE &&
                entry.getNextSibling().getNode().getElementType() == KtTokens.CLOSING_QUOTE;

        String valueSource = quoted ? "\"" + leaf.getText() + "\"" : leaf.getText();
        return new J.Literal(
                Tree.randomId(),
                Space.EMPTY,
                Markers.EMPTY,
                leaf.getText(),
                valueSource, // todo, support text block
                null,
                primitiveType(entry)
        );
    }

    @Override
    public J visitLoopExpression(KtLoopExpression loopExpression, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitModifierList(KtModifierList list, ExecutionContext data) {
        throw new UnsupportedOperationException("Use mapModifiers instead");
    }

    @Override
    public J visitNamedDeclaration(KtNamedDeclaration declaration, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitNullableType(KtNullableType nullableType, ExecutionContext data) {
        J j = nullableType.getInnerType().accept(this, data);
        return j.withMarkers(j.getMarkers().addIfAbsent(new IsNullable(randomId(), suffix(nullableType.getInnerType()))));
    }

    @Override
    public J visitParameter(KtParameter parameter, ExecutionContext data) {
        Markers markers = Markers.EMPTY;
        List<J.Annotation> leadingAnnotations = new ArrayList<>();
        List<J.Annotation> lastAnnotations = new ArrayList<>();
        List<J.Modifier> modifiers = new ArrayList<>();
        TypeTree typeExpression = null;
        List<JRightPadded<J.VariableDeclarations.NamedVariable>> vars = new ArrayList<>(1);

        if (!parameter.getAnnotations().isEmpty()) {
            throw new UnsupportedOperationException("TODO");
        }

        if (parameter.getValOrVarKeyword() != null) {
            modifiers.add(mapModifier(parameter.getValOrVarKeyword(), Collections.emptyList()));
        }

        if (parameter.getModifierList() != null) {
            modifiers.addAll(mapModifiers(parameter.getModifierList(), leadingAnnotations, lastAnnotations, data));
        }

        if (parameter.getDestructuringDeclaration() != null) {
            return mapDestructuringDeclaration(parameter.getDestructuringDeclaration(), data)
                    .withPrefix(prefix(parameter));
        }

        J.Identifier name = createIdentifier(parameter.getNameIdentifier(), type(parameter));

        if (parameter.getTypeReference() != null) {
            markers = markers.addIfAbsent(new TypeReferencePrefix(randomId(), prefix(parameter.getColon())));
            typeExpression = parameter.getTypeReference().accept(this, data).withPrefix(prefix(parameter.getTypeReference()));
        }

        JLeftPadded<Expression> initializer =
                parameter.getDefaultValue() != null ? padLeft(prefix(parameter.getEqualsToken()), (Expression) parameter.getDefaultValue().accept(this, data)) : null;

        J.VariableDeclarations.NamedVariable namedVariable = new J.VariableDeclarations.NamedVariable(
                randomId(),
                Space.EMPTY,
                Markers.EMPTY,
                name,
                emptyList(),
                initializer,
                name.getFieldType()
        );

        vars.add(padRight(namedVariable, Space.EMPTY));

        return new J.VariableDeclarations(
                randomId(),
                prefix(parameter),
                markers,
                leadingAnnotations,
                modifiers,
                typeExpression,
                null,
                emptyList(),
                vars
        );
    }

    @Override
    public J visitParameterList(KtParameterList list, ExecutionContext data) {
        throw new UnsupportedOperationException("Unsupport, use mapParameters() instead");
    }

    @Override
    public J visitPrimaryConstructor(KtPrimaryConstructor constructor, ExecutionContext data) {
        if (constructor.getBodyExpression() != null) {
            throw new UnsupportedOperationException("TODO");
        }

        List<J.Annotation> leadingAnnotations = new ArrayList<>();
        List<J.Modifier> modifiers = new ArrayList<>();

        if (constructor.getModifierList() != null) {
            KtModifierList ktModifierList = constructor.getModifierList();
            modifiers.addAll(mapModifiers(ktModifierList, emptyList(), emptyList(), data));
        }

        if (constructor.getConstructorKeyword() != null) {
            modifiers.add(mapModifier(constructor.getConstructorKeyword(), Collections.emptyList()));
        }

        JavaType type = type(constructor);

        J.Identifier name = new J.Identifier(
                randomId(),
                Space.EMPTY,
                Markers.EMPTY,
                emptyList(),
                "<constructor>",
                type,
                null
        );

        JContainer<Statement> params;

        if (constructor.getValueParameterList() != null) {
            // constructor.getValueParameterList().accept(this, data);

            //  todo. move to constructor.getValueParameterList().accept(this, data);
            List<KtParameter> ktParameters = constructor.getValueParameters();
            List<JRightPadded<Statement>> statements = new ArrayList<>(ktParameters.size());

            for (int i = 0; i < ktParameters.size(); i++) {
                KtParameter ktParameter = ktParameters.get(i);
                Statement statement = convertToStatement(ktParameter.accept(this, data));
                statements.add(maybeTrailingComma(ktParameter, padRight(statement, suffix(ktParameter)), i == ktParameters.size() - 1));
            }

            if (ktParameters.isEmpty()) {
                Statement param = new J.Empty(randomId(), prefix(constructor.getValueParameterList().getRightParenthesis()), Markers.EMPTY);
                statements.add(padRight(param, Space.EMPTY));
            }

            params = JContainer.build(
                    prefix(constructor.getValueParameterList()),
                    statements,
                    Markers.EMPTY);
        } else {
            throw new UnsupportedOperationException("TODO");
        }

        return new J.MethodDeclaration(
                randomId(),
                prefix(constructor),
                Markers.build(singletonList(new PrimaryConstructor(randomId()))),
                leadingAnnotations,
                modifiers,
                null,
                null,
                new J.MethodDeclaration.IdentifierWithAnnotations(
                        name.withMarkers(name.getMarkers().addIfAbsent(new Implicit(randomId()))),
                        emptyList()
                ),
                params,
                null,
                null,
                null,
                methodDeclarationType(constructor)
        );
    }

    @Override
    public J visitPropertyAccessor(KtPropertyAccessor accessor, ExecutionContext data) {
        Markers markers = Markers.EMPTY;
        List<J.Annotation> leadingAnnotations = mapAnnotations(accessor.getAnnotationEntries(), data);
        List<J.Modifier> modifiers = new ArrayList<>();
        J.TypeParameters typeParameters = null;
        TypeTree returnTypeExpression = null;
        List<J.Annotation> lastAnnotations = new ArrayList<>();
        J.Identifier name = null;
        JContainer<Statement> params = null;
        J.Block body = null;

        name = createIdentifier(accessor.getNamePlaceholder().getText(), prefix(accessor.getNamePlaceholder()), type(accessor));

        List<KtParameter> ktParameters = accessor.getValueParameters();
        if (!ktParameters.isEmpty()) {
            if (ktParameters.size() != 1) {
                throw new UnsupportedOperationException("TODO");
            }

            List<JRightPadded<Statement>> parameters = new ArrayList<>();
            for (KtParameter ktParameter : ktParameters) {
                Statement stmt = convertToStatement(ktParameter.accept(this, data).withPrefix(prefix(ktParameter.getParent())));
                parameters.add(padRight(stmt, prefix(accessor.getRightParenthesis())));
            }

            params = JContainer.build(prefix(accessor.getLeftParenthesis()), parameters, Markers.EMPTY);
        } else {
            params = JContainer.build(
                    prefix(accessor.getLeftParenthesis()),
                    singletonList(padRight(new J.Empty(randomId(), prefix(accessor.getRightParenthesis()), Markers.EMPTY), Space.EMPTY)),
                    Markers.EMPTY
            );
        }

        if (accessor.getReturnTypeReference() != null) {
            markers = markers.addIfAbsent(new TypeReferencePrefix(randomId(), suffix(accessor.getRightParenthesis())));
            returnTypeExpression = accessor.getReturnTypeReference().accept(this, data).withPrefix(prefix(accessor.getReturnTypeReference()));
        }

        if (accessor.getBodyBlockExpression() != null) {
            body = accessor.getBodyBlockExpression().accept(this, data).withPrefix(prefix(accessor.getBodyBlockExpression()));
        } else if (accessor.getBodyExpression() != null) {
            body = convertToBlock(accessor.getBodyExpression(), data).withPrefix(prefix(accessor.getEqualsToken()));
        } else {
            throw new UnsupportedOperationException("TODO");
        }

        return new J.MethodDeclaration(
                randomId(),
                prefix(accessor),
                markers,
                leadingAnnotations,
                modifiers,
                typeParameters,
                returnTypeExpression,
                new J.MethodDeclaration.IdentifierWithAnnotations(
                        name,
                        lastAnnotations
                ),
                params,
                null,
                body,
                null,
                methodDeclarationType(accessor)
        );
    }

    @Override
    public J visitQualifiedExpression(KtQualifiedExpression expression, ExecutionContext data) {
        Expression receiver = (Expression) expression.getReceiverExpression().accept(this, data);
        Expression selector = (Expression) expression.getSelectorExpression().accept(this, data);
        if (selector instanceof J.MethodInvocation) {
            J.MethodInvocation methodInvocation = (J.MethodInvocation) selector;
            return methodInvocation.getPadding()
                    .withSelect(padRight(receiver, suffix(expression.getReceiverExpression())))
                    .withName(methodInvocation.getName().withPrefix(prefix(expression.getSelectorExpression())))
                    ;
        } else {
            J.Identifier identifier = (J.Identifier) selector;
            return new J.FieldAccess(
                    randomId(),
                    prefix(expression),
                    Markers.EMPTY,
                    receiver,
                    padLeft(suffix(expression.getReceiverExpression()), identifier),
                    type(expression)
            );
        }
    }

    @Override
    public J visitReferenceExpression(KtReferenceExpression expression, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitReturnExpression(KtReturnExpression expression, ExecutionContext data) {
        KtExpression returnedExpression = expression.getReturnedExpression();
        Expression returnExpr = returnedExpression != null ?
                convertToExpression(returnedExpression.accept(this, data).withPrefix(prefix(returnedExpression))) :
                null;
        return new K.KReturn(
                randomId(),
                new J.Return(
                        randomId(),
                        prefix(expression),
                        Markers.EMPTY,
                        returnExpr
                ),
                expression.getTargetLabel() != null ? createIdentifier(expression.getTargetLabel().getIdentifier(), null) : null
        );
    }

    @Override
    public J visitSafeQualifiedExpression(KtSafeQualifiedExpression expression, ExecutionContext data) {
        J j = visitQualifiedExpression(expression, data);
        ASTNode safeAccess = expression.getNode().findChildByType(KtTokens.SAFE_ACCESS);
        return j.withMarkers(j.getMarkers().addIfAbsent(new IsNullSafe(randomId(), prefix(safeAccess.getPsi()))));
    }

    @Override
    public J visitScript(KtScript script, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitScriptInitializer(KtScriptInitializer initializer, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitSecondaryConstructor(KtSecondaryConstructor constructor, ExecutionContext data) {
        Markers markers = Markers.EMPTY;
        List<J.Annotation> leadingAnnotations = new ArrayList<>();
        List<J.Annotation> lastAnnotations = new ArrayList<>();
        List<J.Modifier> modifiers = mapModifiers(constructor.getModifierList(), leadingAnnotations, lastAnnotations, data);
        modifiers.add(mapModifier(constructor.getConstructorKeyword(), emptyList()));

        J.Identifier name = createIdentifier(constructor.getName(), Space.EMPTY, type(constructor));
        List<JRightPadded<Statement>> statements = mapParameters(constructor.getValueParameterList(), data);
        JContainer<Statement> params = JContainer.build(
                prefix(constructor.getValueParameterList()),
                statements,
                Markers.EMPTY
        );

        J.Identifier delegateName = createIdentifier(constructor.getDelegationCall().getCalleeExpression(), type(constructor.getDelegationCall().getCalleeExpression()));
        JContainer<Expression> args = mapFunctionCallArguments(constructor.getDelegationCall().getValueArgumentList(), data);

        K.ConstructorInvocation delegationCall = new K.ConstructorInvocation(
                randomId(),
                prefix(constructor.getDelegationCall()),
                Markers.EMPTY,
                delegateName,
                args
        );


        J.Block body = null;
        if (constructor.getBodyExpression() != null) {
            body = (J.Block) constructor.getBodyExpression().accept(this, data);
        }

        J.MethodDeclaration methodDeclaration = new J.MethodDeclaration(
                randomId(),
                prefix(constructor),
                markers,
                leadingAnnotations,
                modifiers,
                null,
                null,
                new J.MethodDeclaration.IdentifierWithAnnotations(name, emptyList()),
                params,
                null,
                body,
                null,
                methodDeclarationType(constructor)
        );

        return new K.Constructor(randomId(), Markers.EMPTY, methodDeclaration, prefix(constructor.getColon()), delegationCall);
    }

    @Override
    public J visitSelfType(KtSelfType type, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitSimpleNameStringTemplateEntry(KtSimpleNameStringTemplateEntry entry, ExecutionContext data) {
        return new K.KString.Value(
                randomId(),
                Space.EMPTY,
                Markers.EMPTY,
                entry.getExpression().accept(this, data),
                suffix(entry.getExpression()),
                false
        );
    }

    @Override
    public J visitStringTemplateEntryWithExpression(KtStringTemplateEntryWithExpression entry, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitSuperExpression(KtSuperExpression expression, ExecutionContext data) {
        return createIdentifier(expression, type(expression)).withPrefix(prefix(expression));
    }

    @Override
    public J visitSuperTypeEntry(KtSuperTypeEntry specifier, ExecutionContext data) {
        J j = specifier.getTypeReference().accept(this, data);
        return j.withPrefix(merge(prefix(specifier), j.getPrefix()));
    }

    @Override
    public J visitSuperTypeListEntry(KtSuperTypeListEntry specifier, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitThisExpression(KtThisExpression expression, ExecutionContext data) {
        return new K.KThis(
                randomId(),
                prefix(expression),
                Markers.EMPTY,
                expression.getTargetLabel() != null ? createIdentifier(expression.getTargetLabel().getIdentifier(), null) : null,
                type(expression)
        );
    }

    @Override
    public J visitThrowExpression(KtThrowExpression expression, ExecutionContext data) {
        return new J.Throw(
                randomId(),
                prefix(expression),
                Markers.EMPTY,
                convertToExpression(expression.getThrownExpression().accept(this, data))
        );
    }

    @Override
    public J visitTryExpression(KtTryExpression expression, ExecutionContext data) {
        List<KtCatchClause> ktCatchClauses = expression.getCatchClauses();
        J.Block block = (J.Block) expression.getTryBlock().accept(this, data);
        List<J.Try.Catch> catches = new ArrayList(ktCatchClauses.size());
        JLeftPadded<J.Block> finallyBlock = null;
        for (KtCatchClause catchClause : ktCatchClauses) {
            catches.add((J.Try.Catch) catchClause.accept(this, data));
        }

        if (expression.getFinallyBlock() != null) {
            finallyBlock = padLeft(prefix(expression.getFinallyBlock()), (J.Block) expression.getFinallyBlock().accept(this, data));
        }

        return new J.Try(
                randomId(),
                prefix(expression),
                Markers.EMPTY,
                null,
                block,
                catches,
                finallyBlock
        );
    }

    @Override
    public J visitTypeAlias(KtTypeAlias typeAlias, ExecutionContext data) {
        Markers markers = Markers.EMPTY;
        List<J.Modifier> modifiers = new ArrayList<>();
        List<J.Annotation> leadingAnnotations = new ArrayList<>();
        List<J.Annotation> lastAnnotations = new ArrayList<>();
        modifiers.add(new J.Modifier(randomId(), prefix(typeAlias.getTypeAliasKeyword()), markers, "typealias", J.Modifier.Type.LanguageExtension, emptyList()));

        mapModifiers(typeAlias.getModifierList(), leadingAnnotations, lastAnnotations, data);

        if (typeAlias.getIdentifyingElement() == null) {
            throw new UnsupportedOperationException("TODO");
        }

        J.Identifier name = createIdentifier(typeAlias.getIdentifyingElement(), type(typeAlias.getTypeReference()));// typeAlias.getIdentifyingElement().accept(this, data);
        Expression expr = convertToExpression(typeAlias.getTypeReference().accept(this, data));

        JRightPadded<J.VariableDeclarations.NamedVariable> namedVariable = padRight(
                new J.VariableDeclarations.NamedVariable(
                        randomId(),
                        Space.EMPTY,
                        Markers.EMPTY,
                        // typealias does not have a name.
                        createIdentifier("", Space.EMPTY, null, null),
                        emptyList(),
                        padLeft(suffix(typeAlias.getIdentifyingElement()), expr),
                        null
                ), Space.EMPTY
        );

        List<JRightPadded<J.VariableDeclarations.NamedVariable>> vars = singletonList(namedVariable);

        return new J.VariableDeclarations(
                randomId(),
                prefix(typeAlias),
                markers,
                leadingAnnotations,
                modifiers,
                name,
                null,
                emptyList(),
                vars
        );
    }

    @Override
    public J visitTypeArgumentList(KtTypeArgumentList typeArgumentList, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitTypeConstraint(KtTypeConstraint constraint, ExecutionContext data) {
        List<J.Annotation> annotations = new ArrayList<>();
        J.Identifier typeParamName = (J.Identifier) constraint.getSubjectTypeParameterName().accept(this, data);
        TypeTree typeTree = (TypeTree) constraint.getBoundTypeReference().accept(this, data);

        return new J.TypeParameter(
                randomId(),
                prefix(constraint),
                Markers.EMPTY.addIfAbsent(new TypeReferencePrefix(randomId(), suffix(constraint.getSubjectTypeParameterName()))),
                annotations,
                typeParamName,
                JContainer.build(
                        Space.EMPTY,
                        singletonList(padRight(typeTree, null)),
                        Markers.EMPTY
                )
        );
    }

    @Override
    public J visitTypeConstraintList(KtTypeConstraintList list, ExecutionContext data) {
        List<KtTypeConstraint> ktTypeConstraints = list.getConstraints();
        List<JRightPadded<J.TypeParameter>> params = new ArrayList<>(ktTypeConstraints.size());

        for (KtTypeConstraint ktTypeConstraint : ktTypeConstraints) {
            params.add(padRight((J.TypeParameter) ktTypeConstraint.accept(this, data), Space.EMPTY));
        }

        return new K.TypeConstraints(
                randomId(),
                Markers.EMPTY,
                JContainer.build(prefix(list), params, Markers.EMPTY)
        );
    }

    @Override
    public J visitTypeParameter(KtTypeParameter parameter, ExecutionContext data) {
        Markers markers = Markers.EMPTY;
        List<J.Annotation> annotations = new ArrayList<>();
        J.Identifier name = null;
        JContainer<TypeTree> bounds = null;
        if (parameter.getNameIdentifier() == null) {
            throw new UnsupportedOperationException("TODO");
        }

        mapModifiers(parameter.getModifierList(), annotations, emptyList(), data);

        if (parameter.getVariance() == Variance.INVARIANT) {
            PsiElement reifiedKeyword = findLeafElement(parameter.getModifierList(), "reified");
            if (reifiedKeyword != null) {
                J.Annotation reified = new J.Annotation(randomId(),
                        prefix(reifiedKeyword),
                        Markers.EMPTY.addIfAbsent(new Modifier(randomId())),
                        createIdentifier("reified", Space.EMPTY, null, null),
                        JContainer.empty()
                );
                annotations.add(reified);
            }

            name = createIdentifier(parameter.getNameIdentifier(), type(parameter));

            if (parameter.getExtendsBound() != null) {
                bounds = JContainer.build(suffix(parameter.getNameIdentifier()),
                        singletonList(padRight(parameter.getExtendsBound().accept(this, data).withPrefix(prefix(parameter.getExtendsBound())),
                                Space.EMPTY)),
                        Markers.EMPTY);
            }
        } else if (parameter.getVariance() == Variance.IN_VARIANCE ||
                parameter.getVariance() == Variance.OUT_VARIANCE) {
            GenericType.Variance variance = parameter.getVariance() == Variance.IN_VARIANCE ?
                    GenericType.Variance.CONTRAVARIANT : GenericType.Variance.COVARIANT;
            markers = markers.addIfAbsent(new GenericType(randomId(), variance));
            name = createIdentifier("<Any>", Space.EMPTY, null).withMarkers(Markers.build(singletonList(new Implicit(randomId()))));

            String varianceKeyword = parameter.getVariance() == Variance.IN_VARIANCE ? "in" : "out";
            PsiElement varianceKeywordPsi = findLeafElement(parameter.getModifierList(), varianceKeyword);

            bounds = JContainer.build(
                    prefix(varianceKeywordPsi),
                    singletonList(padRight(
                            createIdentifier(parameter.getNameIdentifier(), type(parameter)), Space.EMPTY)),
                    Markers.EMPTY);
        } else {
            throw new UnsupportedOperationException("TODO");
        }

        return new J.TypeParameter(
                randomId(),
                prefix(parameter),
                markers,
                annotations,
                name,
                bounds
        );
    }

    @Override
    public J visitTypeParameterList(KtTypeParameterList list, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitTypeProjection(KtTypeProjection typeProjection, ExecutionContext data) {
        Markers markers = Markers.EMPTY;
        JContainer<TypeTree> bounds = null;
        Expression name = null;
        switch (typeProjection.getProjectionKind()) {
            case IN: {
                markers = markers.addIfAbsent(new GenericType(randomId(), GenericType.Variance.CONTRAVARIANT));
                bounds = JContainer.build(
                        prefix(typeProjection.getProjectionToken()),
                        singletonList(padRight(typeProjection.getTypeReference().accept(this, data)
                                .withPrefix(prefix(typeProjection.getTypeReference())), Space.EMPTY)),
                        Markers.EMPTY
                );
                break;
            }
            case OUT: {
                markers = markers.addIfAbsent(new GenericType(randomId(), GenericType.Variance.COVARIANT));
                bounds = JContainer.build(
                        prefix(typeProjection.getProjectionToken()),
                        singletonList(padRight(typeProjection.getTypeReference().accept(this, data)
                                .withPrefix(prefix(typeProjection.getTypeReference())), Space.EMPTY)),
                        Markers.EMPTY
                );
                break;
            }
            case STAR: {
                return new J.Wildcard(randomId(), prefix(typeProjection), Markers.EMPTY, null, null);

            }
            default: {
                name = typeProjection.getTypeReference().accept(this, data).withPrefix(prefix(typeProjection));
            }
        }

        return name != null ? name :
                new K.TypeParameterExpression(randomId(), new J.TypeParameter(
                        randomId(),
                        prefix(typeProjection),
                        markers,
                        emptyList(),
                        new J.Identifier(
                                randomId(),
                                Space.EMPTY,
                                Markers.build(singletonList(new Implicit(randomId()))),
                                emptyList(),
                                "Any",
                                null,
                                null
                        ),
                        bounds
                ));
    }

    @Override
    public J visitUnaryExpression(KtUnaryExpression expression, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitWhenConditionInRange(KtWhenConditionInRange condition, ExecutionContext data) {
        K.Binary.Type operator = condition.isNegated() ? K.Binary.Type.NotContains : K.Binary.Type.Contains;
        Expression left = new J.Empty(randomId(), Space.EMPTY, Markers.EMPTY);
        return new K.Binary(randomId(), prefix(condition), Markers.EMPTY, left,
                padLeft(Space.EMPTY, operator),
                convertToExpression(condition.getRangeExpression().accept(this, data)),
                Space.EMPTY,
                type(condition)
        );
    }

    @Override
    public J visitWhenConditionIsPattern(KtWhenConditionIsPattern condition, ExecutionContext data) {
        Markers markers = Markers.EMPTY;
        if (condition.isNegated()) {
            markers = markers.addIfAbsent(new NotIs(randomId()));
        }

        Expression element = new J.Empty(randomId(), Space.EMPTY, Markers.EMPTY);
        JRightPadded<Expression> expr = padRight(element, Space.EMPTY);
        J clazz = condition.getTypeReference().accept(this, data);

        return new J.InstanceOf(
                randomId(),
                prefix(condition),
                markers,
                expr,
                clazz,
                null,
                type(condition)
        );
    }

    @Override
    public J visitWhenConditionWithExpression(KtWhenConditionWithExpression condition, ExecutionContext data) {
        return condition.getExpression().accept(this, data).withPrefix(prefix(condition));
    }

    @Override
    public J visitWhenEntry(KtWhenEntry ktWhenEntry, ExecutionContext data) {
        List<JRightPadded<Expression>> expressions = new ArrayList<>(1);

        if (ktWhenEntry.getElseKeyword() != null) {
            expressions.add(padRight(createIdentifier("else", Space.EMPTY, null, null), prefix(ktWhenEntry.getArrow())));
        } else {
            KtWhenCondition[] ktWhenConditions = ktWhenEntry.getConditions();
            for (int i = 0; i < ktWhenConditions.length; i++) {
                KtWhenCondition ktWhenCondition = ktWhenConditions[i];
                Expression expr = convertToExpression(ktWhenCondition.accept(this, data));
                expressions.add(maybeTrailingComma(ktWhenCondition, padRight(expr, suffix(ktWhenCondition)), i == ktWhenConditions.length - 1));
            }
        }

        JContainer<Expression> expressionContainer = JContainer.build(Space.EMPTY, expressions, Markers.EMPTY);
        J body = ktWhenEntry.getExpression().accept(this, data);

        return new K.WhenBranch(
                randomId(),
                prefix(ktWhenEntry),
                Markers.EMPTY,
                expressionContainer,
                padRight(body, Space.EMPTY)
        );
    }

    @NotNull
    private <T> JRightPadded<T> maybeTrailingComma(KtElement element, JRightPadded<T> padded, boolean last) {
        if (!last) {
            return padded;
        }
        PsiElement maybeComma = PsiTreeUtil.findSiblingForward(element, KtTokens.COMMA, null);
        if (maybeComma != null && maybeComma.getNode().getElementType() == KtTokens.COMMA) {
            padded = padded.withMarkers(padded.getMarkers().addIfAbsent(new TrailingComma(randomId(), suffix(maybeComma))));
        }
        return padded;
    }

    @Override
    public J visitWhenExpression(KtWhenExpression expression, ExecutionContext data) {
        J.ControlParentheses<J> controlParentheses = null;

        if (expression.getSubjectExpression() != null) {
            J subject = expression.getSubjectExpression().accept(this, data);
            controlParentheses = new J.ControlParentheses<>(
                    randomId(),
                    prefix(expression.getLeftParenthesis()),
                    Markers.EMPTY,
                    padRight(subject, prefix(expression.getRightParenthesis()))
            );
        }

        List<KtWhenEntry> whenEntries = expression.getEntries();
        List<JRightPadded<Statement>> statements = new ArrayList<>(whenEntries.size());

        for (KtWhenEntry whenEntry : whenEntries) {
            K.WhenBranch whenBranch = (K.WhenBranch) whenEntry.accept(this, data);
            statements.add(padRight(whenBranch, Space.EMPTY));
        }

        J.Block body = new J.Block(
                randomId(),
                prefix(expression.getOpenBrace()),
                Markers.EMPTY,
                new JRightPadded<>(false, Space.EMPTY, Markers.EMPTY),
                statements,
                prefix(expression.getCloseBrace())
        );

        return new K.When(
                randomId(),
                prefix(expression),
                Markers.EMPTY,
                controlParentheses,
                body,
                type(expression)
        );
    }

    @Override
    public J visitWhileExpression(KtWhileExpression expression, ExecutionContext data) {
        return new J.WhileLoop(
                randomId(),
                prefix(expression),
                Markers.EMPTY,
                mapControlParentheses(expression.getCondition(), data).withPrefix(prefix(expression.getLeftParenthesis())),
                JRightPadded.build(expression.getBody().accept(this, data).withPrefix(prefix(expression.getBody().getParent())))
        );
    }

    @Override
    public void visitBinaryFile(PsiBinaryFile file) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public void visitComment(PsiComment comment) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public void visitDirectory(PsiDirectory dir) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitKtElement(KtElement element, ExecutionContext data) {
        throw new UnsupportedOperationException("Should never call this, if this is called, means something wrong");
        // return element.accept(this, data);
    }

    /*====================================================================
     * PSI to J tree mapping methods
     * ====================================================================*/
    @Override
    public J visitKtFile(KtFile file, ExecutionContext data) {
        List<J.Annotation> annotations = file.getFileAnnotationList() != null ? mapAnnotations(file.getAnnotationEntries(), data) : emptyList();

        JRightPadded<J.Package> pkg = null;
        if (!file.getPackageFqName().isRoot()) {
            pkg = maybeSemicolon((J.Package) file.getPackageDirective().accept(this, data), file.getPackageDirective());
        }

        List<JRightPadded<J.Import>> imports = file.getImportDirectives().isEmpty() ? emptyList() : new ArrayList<>(file.getImportDirectives().size());
        if (!file.getImportDirectives().isEmpty()) {
            List<KtImportDirective> importDirectives = file.getImportDirectives();
            for (int i = 0; i < importDirectives.size(); i++) {
                KtImportDirective importDirective = importDirectives.get(i);
                J.Import anImport = (J.Import) importDirective.accept(this, data);
                if (i == 0) {
                    anImport = anImport.withPrefix(prefix(file.getImportList()));
                }
                imports.add(maybeSemicolon(anImport, importDirective));
            }
        }

        List<JRightPadded<Statement>> statements = file.getDeclarations().isEmpty() ? emptyList() : new ArrayList<>(file.getDeclarations().size());
        List<KtDeclaration> declarations = file.getDeclarations();
        for (int i = 0; i < declarations.size(); i++) {
            boolean last = i == declarations.size() - 1;
            KtDeclaration declaration = declarations.get(i);
            Statement statement = convertToStatement(declaration.accept(this, data));
            statements.add(padRight(statement, last ? suffix(declaration) : Space.EMPTY));
        }

        return new K.CompilationUnit(
                Tree.randomId(),
                Space.EMPTY,
                Markers.build(styles),
                sourcePath,
                fileAttributes,
                charset.name(),
                charsetBomMarked,
                null,
                annotations,
                pkg,
                imports,
                statements,
                Space.EMPTY
        );
    }

    @Override
    public J visitAnnotation(KtAnnotation annotation, ExecutionContext data) {
        throw new UnsupportedOperationException("KtAnnotation");
    }

    @Override
    public J visitAnnotationEntry(@NotNull KtAnnotationEntry annotationEntry, ExecutionContext data) {
        Markers markers = Markers.EMPTY;
        JContainer<Expression> args = null;

        if (annotationEntry.getUseSiteTarget() != null) {
            markers = markers.addIfAbsent(new AnnotationCallSite(
                    randomId(),
                    annotationEntry.getUseSiteTarget().getText(),
                    suffix(annotationEntry.getUseSiteTarget())
            ));
        }

        if (annotationEntry.getValueArgumentList() != null) {
            if (annotationEntry.getValueArguments().isEmpty()) {
                args = JContainer.build(prefix(annotationEntry.getValueArgumentList()),
                        singletonList(padRight(new J.Empty(randomId(), prefix(annotationEntry.getValueArgumentList().getRightParenthesis()), Markers.EMPTY), Space.EMPTY)
                        ), Markers.EMPTY);
            } else {
                List<JRightPadded<Expression>> expressions = new ArrayList<>(annotationEntry.getValueArguments().size());
                for (ValueArgument valueArgument : annotationEntry.getValueArguments()) {
                    KtValueArgument ktValueArgument = (KtValueArgument) valueArgument;
                    expressions.add(padRight(convertToExpression(ktValueArgument.accept(this, data).withPrefix(prefix(ktValueArgument))), suffix(ktValueArgument)));
                }
                args = JContainer.build(prefix(annotationEntry.getValueArgumentList()), expressions, Markers.EMPTY);
            }
        }

        return new J.Annotation(
                randomId(),
                prefix(annotationEntry),
                markers,
                (NameTree) annotationEntry.getCalleeExpression().accept(this, data),
                args
        );
    }

    @Override
    public J visitArgument(KtValueArgument argument, ExecutionContext data) {
        if (argument.getArgumentExpression() == null) {
            throw new UnsupportedOperationException("TODO");
        } else if (argument.isNamed()) {
            J.Identifier name = createIdentifier(argument.getArgumentName(), type(argument.getArgumentName()));
            Expression expr = convertToExpression(argument.getArgumentExpression().accept(this, data));
            return new J.Assignment(
                    randomId(),
                    prefix(argument.getArgumentName()),
                    Markers.EMPTY,
                    name,
                    padLeft(suffix(argument.getArgumentName()), expr),
                    type(argument.getArgumentExpression())
            );
        } else if (argument.isSpread()) {
            throw new UnsupportedOperationException("TODO");
        }

        J j = argument.getArgumentExpression().accept(this, data).withPrefix(prefix(argument));
        return argument instanceof KtLambdaArgument ? j.withMarkers(j.getMarkers().addIfAbsent(new TrailingLambdaArgument(randomId()))) : j;
    }

    @Override
    public J visitBinaryExpression(KtBinaryExpression expression, ExecutionContext data) {
        assert expression.getLeft() != null;
        assert expression.getRight() != null;

        KtOperationReferenceExpression operationReference = expression.getOperationReference();
        J.Binary.Type javaBinaryType = mapJBinaryType(operationReference);
        J.AssignmentOperation.Type assignmentOperationType = javaBinaryType == null ? mapAssignmentOperationType(operationReference) : null;
        K.Binary.Type kotlinBinaryType = javaBinaryType == null && assignmentOperationType == null ? mapKBinaryType(operationReference) : null;

        Expression left = convertToExpression(expression.getLeft().accept(this, data)).withPrefix(Space.EMPTY);
        Expression right = convertToExpression((expression.getRight()).accept(this, data))
                .withPrefix(prefix(expression.getRight()));
        JavaType type = type(expression);

        if (javaBinaryType != null) {
            return new J.Binary(
                    randomId(),
                    prefix(expression),
                    Markers.EMPTY,
                    left,
                    padLeft(prefix(operationReference), javaBinaryType),
                    right,
                    type
            );
        } else if (operationReference.getOperationSignTokenType() == KtTokens.EQ) {
            return new J.Assignment(
                    randomId(),
                    prefix(expression),
                    Markers.EMPTY,
                    left,
                    padLeft(suffix(expression.getLeft()), right),
                    type
            );
        } else if (assignmentOperationType != null) {
            return new J.AssignmentOperation(
                    randomId(),
                    prefix(expression),
                    Markers.EMPTY,
                    left,
                    padLeft(prefix(operationReference), assignmentOperationType),
                    right,
                    type
            );
        } else if (kotlinBinaryType != null) {
            return new K.Binary(
                    randomId(),
                    prefix(expression),
                    Markers.EMPTY,
                    left,
                    padLeft(prefix(operationReference), kotlinBinaryType),
                    right,
                    Space.EMPTY,
                    type
            );
        } else if (operationReference.getIdentifier() != null) {
            String operation = operationReference.getIdentifier().getText();
            if ("to".equals(operation) || "downTo".equals(operation) || "step".equals(operation)) {
                Markers markers = Markers.EMPTY
                        .addIfAbsent(new Infix(randomId()))
                        .addIfAbsent(new Extension(randomId()));

                Expression selectExp = convertToExpression(expression.getLeft().accept(this, data).withPrefix(prefix(expression.getLeft())));
                JRightPadded<Expression> select = padRight(selectExp, suffix(expression.getLeft()));
                J.Identifier name = createIdentifier(operation, Space.EMPTY, methodInvocationType(expression));
                JContainer<Expression> typeParams = null;

                List<JRightPadded<Expression>> expressions = new ArrayList<>(1);
                Markers paramMarkers = markers.addIfAbsent(new OmitParentheses(randomId()));
                Expression rightExp = convertToExpression(expression.getRight().accept(this, data).withPrefix(prefix(expression.getRight())));
                JRightPadded<Expression> padded = padRight(rightExp, suffix(expression.getRight()));
                expressions.add(padded);
                JContainer<Expression> args = JContainer.build(Space.EMPTY, expressions, paramMarkers);
                JavaType.Method methodType = methodInvocationType(expression);

                return new J.MethodInvocation(
                        randomId(),
                        prefix(expression),
                        markers,
                        select,
                        typeParams,
                        name,
                        args,
                        methodType
                );
            }
        }

        throw new UnsupportedOperationException("TODO");
    }

    @Nullable
    private J.AssignmentOperation.Type mapAssignmentOperationType(KtOperationReferenceExpression operationReference) {
        IElementType elementType = operationReference.getOperationSignTokenType();

        if (elementType == KtTokens.PLUSEQ)
            return J.AssignmentOperation.Type.Addition;
        if (elementType == KtTokens.MINUSEQ)
            return J.AssignmentOperation.Type.Subtraction;
        if (elementType == KtTokens.MULTEQ)
            return J.AssignmentOperation.Type.Multiplication;
        if (elementType == KtTokens.DIVEQ)
            return J.AssignmentOperation.Type.Division;
        else
            return null;
    }

    @Override
    public J visitBlockExpression(KtBlockExpression expression, ExecutionContext data) {
        List<JRightPadded<Statement>> statements = new ArrayList<>();
        for (KtExpression stmt : expression.getStatements()) {
            J exp = stmt.accept(this, data);

            Statement statement = convertToStatement(exp);

//            PsiElementAssociations.ExpressionType expressionType = psiElementAssociations.getExpressionType(stmt);
//            if (expressionType == PsiElementAssociations.ExpressionType.RETURN_EXPRESSION) {
//                boolean explicitReturn = false;
//                Markers markers = Markers.EMPTY;
//                if (!explicitReturn) {
//                    markers = markers.addIfAbsent(new ImplicitReturn(randomId()));
//                }
//                statement = new K.KReturn(randomId(), new J.Return(randomId(), prefix(stmt), markers, convertToExpression(exp).withPrefix(Space.EMPTY)), null);
//            } else {
//                statement = convertToStatement(exp);
//            }

            JRightPadded<Statement> build = maybeSemicolon(statement, stmt);
            statements.add(build);
        }

        boolean hasBraces = expression.getLBrace() != null;
        Space end = expression.getLBrace() != null ? prefix(expression.getRBrace()) : suffix(expression);

        return new J.Block(
                randomId(),
                prefix(expression),
                hasBraces ? Markers.EMPTY : Markers.EMPTY.addIfAbsent(new OmitBraces(randomId())),
                JRightPadded.build(false),
                statements,
                end
        );
    }

    @Override
    public J visitCallExpression(KtCallExpression expression, ExecutionContext data) {
        if (expression.getCalleeExpression() == null) {
            throw new UnsupportedOperationException("TODO");
        }

        PsiElementAssociations.ExpressionType type = psiElementAssociations.getFunctionType(expression);
        if (type == PsiElementAssociations.ExpressionType.CONSTRUCTOR) {
            TypeTree name = (J.Identifier) expression.getCalleeExpression().accept(this, data);
            if (!expression.getTypeArguments().isEmpty()) {
                // FIXME: create a list.
                Expression expr = (Expression) expression.getTypeArguments().get(0).accept(this, data);
                name = new J.ParameterizedType(
                        randomId(),
                        name.getPrefix(),
                        Markers.EMPTY,
                        name.withPrefix(Space.EMPTY),
                        JContainer.build(prefix(expression.getTypeArgumentList()), singletonList(padRight(expr, suffix(expression.getTypeArgumentList()))), Markers.EMPTY),
                        type(expression)
                );
            }

            // createIdentifier(expression.getCalleeExpression(), type(expression));
            List<KtValueArgument> arguments = expression.getValueArguments();

            JContainer<Expression> args;
            if (arguments.isEmpty()) {
                args = JContainer.build(singletonList(padRight(new J.Empty(randomId(), prefix(expression.getValueArgumentList().getRightParenthesis()), Markers.EMPTY), Space.EMPTY)));
                args = args.withBefore(prefix(expression.getValueArgumentList()));
            } else {
                List<JRightPadded<Expression>> expressions = new ArrayList<>(arguments.size());
                Markers markers = Markers.EMPTY;

                for (KtValueArgument arg : arguments) {
                    expressions.add(padRight(convertToExpression(arg.accept(this, data)).withPrefix(prefix(arg)), suffix(arg)));
                }
                args = JContainer.build(prefix(expression.getValueArgumentList()), expressions, markers);
            }
            return new J.NewClass(
                    randomId(),
                    prefix(expression),
                    Markers.EMPTY,
                    null,
                    Space.EMPTY,
                    name,
                    args,
                    null,
                    methodInvocationType(expression)
            );
        } else if (type == null || type == PsiElementAssociations.ExpressionType.METHOD_INVOCATION) {
            J.Identifier name = (J.Identifier) expression.getCalleeExpression().accept(this, data);
            JContainer<Expression> typeParams = null;

            if (!expression.getTypeArguments().isEmpty()) {
                List<JRightPadded<Expression>> parameters = new ArrayList<>(expression.getTypeArguments().size());
                for (KtTypeProjection ktTypeProjection : expression.getTypeArguments()) {
                    parameters.add(padRight(convertToExpression(ktTypeProjection.accept(this, data)), suffix(ktTypeProjection)));
                }

                typeParams = JContainer.build(prefix(expression.getTypeArgumentList()), parameters, Markers.EMPTY);
            }

            // createIdentifier(expression.getCalleeExpression(), type(expression));
            List<KtValueArgument> arguments = expression.getValueArguments();

            List<JRightPadded<Expression>> expressions = new ArrayList<>(arguments.size());
            Markers markers = Markers.EMPTY;
            if (expression.getValueArgumentList() == null) {
                markers = markers.addIfAbsent(new OmitParentheses(randomId()));
            }

            if (!arguments.isEmpty()) {
                for (int i = 0; i < arguments.size(); i++) {
                    KtValueArgument arg = arguments.get(i);
                    Expression expr = convertToExpression(arg.accept(this, data)).withPrefix(prefix(arg));
                    if (expr.getMarkers().findFirst(TrailingLambdaArgument.class).isPresent() && !expressions.isEmpty()) {
                        expressions.set(expressions.size() - 1, maybeTrailingComma(arguments.get(i - 1), expressions.get(expressions.size() - 1), true));
                    }
                    expressions.add(maybeTrailingComma(arg, padRight(expr, suffix(arg)), i == arguments.size() - 1));
                }
            } else {
                expressions.add(padRight(new J.Empty(randomId(), prefix(expression.getValueArgumentList().getRightParenthesis()), Markers.EMPTY), Space.EMPTY));
            }

            JContainer<Expression> args = JContainer.build(prefix(expression.getValueArgumentList()), expressions, markers);

            JavaType.Method methodType = methodInvocationType(expression);
            return new J.MethodInvocation(
                    randomId(),
                    prefix(expression),
                    Markers.EMPTY,
                    null,
                    typeParams,
                    name.withType(methodType),
                    args,
                    methodType
            );
        } else {
            throw new UnsupportedOperationException("TODO");
        }
    }

    @Override
    public J visitConstantExpression(KtConstantExpression expression, ExecutionContext data) {
        IElementType elementType = expression.getElementType();
        Object value;
        if (elementType == KtNodeTypes.INTEGER_CONSTANT || elementType == KtNodeTypes.FLOAT_CONSTANT) {
            value = ParseUtilsKt.parseNumericLiteral(expression.getText(), elementType);
        } else if (elementType == KtNodeTypes.BOOLEAN_CONSTANT) {
            value = ParseUtilsKt.parseBoolean(expression.getText());
        } else if (elementType == KtNodeTypes.CHARACTER_CONSTANT) {
            value = expression.getText().charAt(0);
        } else if (elementType == KtNodeTypes.NULL) {
            value = null;
        } else {
            throw new UnsupportedOperationException("Unsupported constant expression elementType : " + elementType);
        }
        return new J.Literal(
                Tree.randomId(),
                prefix(expression),
                Markers.EMPTY,
                value,
                expression.getText(),
                null,
                primitiveType(expression)
        );
    }

    @Override
    public J visitClass(KtClass klass, ExecutionContext data) {
        ownerStack.push(klass);
        try {
            return visitClass0(klass, data);
        } finally {
            ownerStack.pop();
        }
    }

    @NotNull
    private J.ClassDeclaration visitClass0(KtClass klass, ExecutionContext data) {
        List<J.Annotation> leadingAnnotations = new ArrayList<>();
        List<J.Annotation> lastAnnotations = new ArrayList<>();
        JContainer<J.TypeParameter> typeParams = null;
        JContainer<TypeTree> implementings = null;
        Markers markers = Markers.EMPTY;
        J.MethodDeclaration primaryConstructor;

        List<J.Modifier> modifiers = mapModifiers(klass.getModifierList(), leadingAnnotations, lastAnnotations, data);

        if (!klass.hasModifier(KtTokens.OPEN_KEYWORD)) {
            modifiers.add(buildFinalModifier());
        }

        J.ClassDeclaration.Kind kind;
        if (klass.getClassKeyword() != null) {
            kind = new J.ClassDeclaration.Kind(
                    randomId(),
                    prefix(klass.getClassKeyword()),
                    Markers.EMPTY,
                    emptyList(),
                    J.ClassDeclaration.Kind.Type.Class
            );
        } else if (klass.getClassOrInterfaceKeyword() != null) {
            kind = new J.ClassDeclaration.Kind(
                    randomId(),
                    prefix(klass.getClassOrInterfaceKeyword()),
                    Markers.EMPTY,
                    emptyList(),
                    J.ClassDeclaration.Kind.Type.Interface
            );
        } else {
            throw new UnsupportedOperationException("TODO");
        }

        // TODO: fix NPE.
        J.Identifier name = createIdentifier(klass.getIdentifyingElement(), type(klass));

        J.Block body;
        if (klass.getBody() != null) {
            body = (J.Block) klass.getBody().accept(this, data);
        } else {
            body = new J.Block(
                    randomId(),
                    Space.EMPTY,
                    Markers.EMPTY.add(new OmitBraces(randomId())),
                    padRight(false, Space.EMPTY),
                    emptyList(),
                    Space.EMPTY
            );
        }

        if (klass.getPrimaryConstructor() != null) {
            primaryConstructor = (J.MethodDeclaration) klass.getPrimaryConstructor().accept(this, data);
            body = body.withStatements(ListUtils.concat(primaryConstructor, body.getStatements()));
            markers = markers.addIfAbsent(new PrimaryConstructor(randomId()));
        }

        if (!klass.getSuperTypeListEntries().isEmpty()) {
            List<JRightPadded<TypeTree>> superTypes = new ArrayList<>(klass.getSuperTypeListEntries().size());

            for (int i = 0; i < klass.getSuperTypeListEntries().size(); i++) {
                KtSuperTypeListEntry superTypeListEntry = klass.getSuperTypeListEntries().get(i);

                if (superTypeListEntry instanceof KtSuperTypeCallEntry) {
                    KtSuperTypeCallEntry superTypeCallEntry = (KtSuperTypeCallEntry) superTypeListEntry;
                    TypeTree typeTree = (TypeTree) superTypeCallEntry.getCalleeExpression().accept(this, data);
                    JContainer<Expression> args;

                    if (!superTypeCallEntry.getValueArguments().isEmpty()) {
                        args = mapFunctionCallArguments(superTypeCallEntry.getValueArgumentList(), data);
                    } else {
                        KtValueArgumentList ktArgList = superTypeCallEntry.getValueArgumentList();
                        args = JContainer.build(
                                prefix(ktArgList),
                                singletonList(padRight(new J.Empty(randomId(), prefix(ktArgList.getRightParenthesis()), Markers.EMPTY), Space.EMPTY)),
                                markers
                        );
                    }

                    K.ConstructorInvocation delegationCall = new K.ConstructorInvocation(
                            randomId(),
                            prefix(klass.getSuperTypeList()),
                            Markers.EMPTY,
                            typeTree,
                            args
                    );
                    superTypes.add(padRight(delegationCall, suffix(superTypeCallEntry)));
                } else if (superTypeListEntry instanceof KtSuperTypeEntry ||
                        superTypeListEntry instanceof KtDelegatedSuperTypeEntry) {
                    TypeTree typeTree = (TypeTree) superTypeListEntry.accept(this, data);

                    if (i == 0) {
                        typeTree = typeTree.withPrefix(prefix(klass.getSuperTypeList()));
                    }

                    superTypes.add(padRight(typeTree, suffix(superTypeListEntry)));
                } else {
                    throw new UnsupportedOperationException("TODO");
                }
            }

            implementings = JContainer.build(prefix(klass.getColon()), superTypes, Markers.EMPTY);
        }

        if (!klass.getTypeParameters().isEmpty()) {
            typeParams = JContainer.build(prefix(klass.getTypeParameterList()), mapTypeParameters(klass, data), Markers.EMPTY);
        }

        return new J.ClassDeclaration(
                randomId(),
                merge(prefix(klass), infix(klass)),
                markers,
                leadingAnnotations,
                modifiers,
                kind,
                name,
                typeParams,
                null,
                null,
                implementings,
                null,
                body,
                (JavaType.FullyQualified) type2(klass)
        );
    }

    @Override
    public J visitClassBody(KtClassBody classBody, ExecutionContext data) {
        List<JRightPadded<Statement>> list = new ArrayList<>();

        Space after = prefix(classBody.getRBrace());

        if (!classBody.getEnumEntries().isEmpty()) {
            List<JRightPadded<J.EnumValue>> enumValues = new ArrayList(classBody.getEnumEntries().size());
            boolean terminatedWithSemicolon = false;

            for (int i = 0; i < classBody.getEnumEntries().size(); i++) {
                KtEnumEntry ktEnumEntry = classBody.getEnumEntries().get(i);
                PsiElement comma = PsiTreeUtil.findSiblingForward(ktEnumEntry.getIdentifyingElement(), KtTokens.COMMA, null);
                PsiElement semicolon = PsiTreeUtil.findSiblingForward(ktEnumEntry.getIdentifyingElement(), KtTokens.SEMICOLON, null);
                JRightPadded<J.EnumValue> rp = padRight((J.EnumValue) ktEnumEntry.accept(this, data), Space.EMPTY);

                if (i == classBody.getEnumEntries().size() - 1) {
                    List<PsiElement> allChildren = getAllChildren(ktEnumEntry);
                    IElementType lastElementType = allChildren.get(allChildren.size() - 1).getNode().getElementType();

                    if (comma != null) {
                        rp = rp.withAfter(prefix(comma));
                        Space afterComma = suffix(comma);
                        rp = rp.withMarkers(rp.getMarkers().addIfAbsent(new TrailingComma(randomId(), afterComma)));
                    } else {
                        if (semicolon != null) {
                            rp = rp.withAfter(prefix(semicolon));
                        }
                    }

                    if (semicolon != null) {
                        // rp = rp.withAfter(prefix(semicolon));
                        terminatedWithSemicolon = true;
                        after = merge(suffix(semicolon), after);
                    }
                } else {
                    rp = rp.withAfter(prefix(comma));
                }
                enumValues.add(rp);
            }

            JRightPadded<Statement> enumSet = padRight(
                    new J.EnumValueSet(
                            randomId(),
                            Space.EMPTY,
                            Markers.EMPTY,
                            enumValues,
                            terminatedWithSemicolon
                    ),
                    Space.EMPTY
            );
            list.add(enumSet);
        }

        for (KtDeclaration d : classBody.getDeclarations()) {
            if (d instanceof KtEnumEntry) {
                continue;
            }
            list.add(maybeSemicolon(convertToStatement(d.accept(this, data)), d));
        }

        return new J.Block(
                randomId(),
                prefix(classBody),
                Markers.EMPTY,
                padRight(false, Space.EMPTY),
                list,
                after
        );
    }

    @Override
    public J visitDestructuringDeclaration(KtDestructuringDeclaration multiDeclaration, ExecutionContext data) {
        List<J.Modifier> modifiers = new ArrayList<>();
        List<J.Annotation> leadingAnnotations = new ArrayList<>();
        List<JRightPadded<J.VariableDeclarations.NamedVariable>> vars = new ArrayList<>();
        JLeftPadded<Expression> paddedInitializer = null;

        J.Modifier modifier = new J.Modifier(
                Tree.randomId(),
                prefix(multiDeclaration.getValOrVarKeyword()),
                Markers.EMPTY,
                multiDeclaration.isVar() ? "var" : null,
                multiDeclaration.isVar() ? J.Modifier.Type.LanguageExtension : J.Modifier.Type.Final,
                Collections.emptyList()
        );
        modifiers.add(modifier);

        if (multiDeclaration.getInitializer() != null) {
            paddedInitializer = padLeft(suffix(multiDeclaration.getRPar()),
                    convertToExpression(multiDeclaration.getInitializer().accept(this, data))
                            .withPrefix(prefix(multiDeclaration.getInitializer())));
        }

        List<KtDestructuringDeclarationEntry> entries = multiDeclaration.getEntries();
        for (int i = 0; i < entries.size(); i++) {
            KtDestructuringDeclarationEntry entry = entries.get(i);
            Space beforeEntry = prefix(entry);
            List<J.Annotation> annotations = new ArrayList<>();

            if (entry.getModifierList() != null) {
                mapModifiers(entry.getModifierList(), annotations, emptyList(), data);
                if (!annotations.isEmpty()) {
                    annotations = ListUtils.mapFirst(annotations, anno -> anno.withPrefix(beforeEntry));
                }
            }

            JavaType.Variable vt = variableType(entry);

            if (entry.getName() == null) {
                throw new UnsupportedOperationException();
            }

            J.Identifier nameVar = createIdentifier(entry.getNameIdentifier(), vt != null ? vt.getType() : null, vt);
            if (!annotations.isEmpty()) {
                nameVar = nameVar.withAnnotations(annotations);
            } else {
                nameVar = nameVar.withPrefix(beforeEntry);
            }

            J.VariableDeclarations.NamedVariable namedVariable = new J.VariableDeclarations.NamedVariable(
                    randomId(),
                    Space.EMPTY,
                    Markers.EMPTY,
                    nameVar,
                    emptyList(),
                    null,
                    vt
            );

            JavaType.Method methodType = null;
            if (paddedInitializer != null && paddedInitializer.getElement() instanceof J.NewClass) {
                // TODO: fix what is NC for?
                J.NewClass nc = (J.NewClass) paddedInitializer.getElement();
                methodType = methodInvocationType(entry);
            }

            J.MethodInvocation initializer = buildSyntheticDestructInitializer(i + 1)
                    .withMethodType(methodType);
            namedVariable = namedVariable.getPadding().withInitializer(padLeft(Space.SINGLE_SPACE, initializer));
            vars.add(padRight(namedVariable, suffix(entry)));
        }

        J.VariableDeclarations.NamedVariable emptyWithInitializer = new J.VariableDeclarations.NamedVariable(
                randomId(),
                Space.EMPTY,
                Markers.EMPTY,
                new J.Identifier(
                        randomId(),
                        Space.SINGLE_SPACE,
                        Markers.EMPTY,
                        emptyList(),
                        "<destruct>",
                        null,
                        null
                ),
                emptyList(),
                paddedInitializer,
                null
        );

        J.VariableDeclarations variableDeclarations = new J.VariableDeclarations(
                randomId(),
                Space.EMPTY,
                Markers.EMPTY,
                leadingAnnotations,
                modifiers,
                null,
                null,
                emptyList(),
                singletonList(padRight(emptyWithInitializer, Space.EMPTY)
                )
        );

        return new K.DestructuringDeclaration(
                randomId(),
                prefix(multiDeclaration),
                Markers.EMPTY,
                variableDeclarations,
                JContainer.build(prefix(multiDeclaration.getLPar()), vars, Markers.EMPTY)
        );
    }

    private J.MethodInvocation buildSyntheticDestructInitializer(int id) {
        J.Identifier name = new J.Identifier(
                randomId(),
                Space.SINGLE_SPACE,
                Markers.EMPTY,
                emptyList(),
                "component" + id,
                null,
                null
        );

        return new J.MethodInvocation(
                randomId(),
                Space.EMPTY,
                Markers.EMPTY,
                null,
                null,
                name,
                JContainer.empty(),
                null
        );
    }

    @Override
    public J visitDotQualifiedExpression(KtDotQualifiedExpression expression, ExecutionContext data) {
        assert expression.getSelectorExpression() != null;
        if (expression.getSelectorExpression() instanceof KtCallExpression) {
            KtCallExpression callExpression = (KtCallExpression) expression.getSelectorExpression();
            MethodCall methodInvocation = (MethodCall) callExpression.accept(this, data);

            if (expression.getReceiverExpression() != null) {
                Expression receiver = convertToExpression(expression.getReceiverExpression().accept(this, data));
                if (methodInvocation instanceof J.MethodInvocation) {
                    methodInvocation = ((J.MethodInvocation) methodInvocation).getPadding().withSelect(padRight(receiver, suffix(expression.getReceiverExpression())))
                            .withName(((J.MethodInvocation) methodInvocation).getName().withPrefix(prefix(callExpression)))
                            .withPrefix(prefix(expression));
                } else if (methodInvocation instanceof J.NewClass) {
                    methodInvocation = ((J.NewClass) methodInvocation).getPadding().withEnclosing(padRight(receiver, suffix(expression.getReceiverExpression())))
                            .withClazz(((J.NewClass) methodInvocation).getClazz().withPrefix(prefix(callExpression)))
                            .withPrefix(prefix(expression));
                }
            }

            return methodInvocation;

//
//            if (!callExpression.getTypeArguments().isEmpty()) {
//                throw new UnsupportedOperationException("TODO");
//            }
//            J j = expression.getSelectorExpression().accept(this, data);
//            if (j instanceof J.MethodInvocation) {
//                J.MethodInvocation methodInvocation = (J.MethodInvocation) j;
//                methodInvocation = methodInvocation.getPadding()
//                        .withSelect(
//                                padRight(expression.getReceiverExpression().accept(this, data)
//                                                .withPrefix(prefix(expression.getReceiverExpression())),
//                                        suffix(expression.getReceiverExpression())
//                                )
//                        )
//                        .withName(methodInvocation.getName().withPrefix(methodInvocation.getPrefix()))
//                        .withPrefix(prefix(expression))
//                ;
//                return methodInvocation;
//            }
//            return new J.MethodInvocation(
//                    randomId(),
//                    prefix(expression),
//                    Markers.EMPTY,
//                    padRight(expression.getReceiverExpression().accept(this, data).withPrefix(Space.EMPTY), Space.EMPTY),
//                    null,
//                    expression.getSelectorExpression().accept(this, data).withPrefix(Space.EMPTY),
//                    JContainer.empty(),
//                    methodInvocationType(expression) // TODO: fix for constructors of inner classes
//            );
        } else {
            // Maybe need to type check before creating a field access.
            return new J.FieldAccess(
                    randomId(),
                    prefix(expression),
                    Markers.EMPTY,
                    expression.getReceiverExpression().accept(this, data).withPrefix(Space.EMPTY),
                    padLeft(suffix(expression.getReceiverExpression()), (J.Identifier) expression.getSelectorExpression().accept(this, data)),
                    type(expression)
            );
        }
    }

    @Override
    public J visitIfExpression(KtIfExpression expression, ExecutionContext data) {
        return new J.If(
                randomId(),
                prefix(expression),
                Markers.EMPTY,
                buildIfCondition(expression),
                buildIfThenPart(expression),
                buildIfElsePart(expression)
        );
    }

    @Override
    public J visitImportDirective(KtImportDirective importDirective, ExecutionContext data) {
        FirElement firElement = psiElementAssociations.primary(importDirective);
        boolean hasParentClassId = firElement instanceof FirResolvedImport && ((FirResolvedImport) firElement).getResolvedParentClassId() != null;
        JLeftPadded<Boolean> statik = padLeft(Space.EMPTY, hasParentClassId);
        KtImportAlias alias = importDirective.getAlias();
        String text = nodeRangeText(
                importDirective.getNode().findChildByType(KtTokens.WHITE_SPACE),
                importDirective.isAllUnder() ? importDirective.getNode().findChildByType(KtTokens.MUL)
                        : importDirective.getNode().findChildByType(KtNodeTypes.DOT_QUALIFIED_EXPRESSION));
        J reference = TypeTree.build(text);
//        J reference = importDirective.getImportedReference().accept(this, data);
//        if (importDirective.isAllUnder()) {
//            reference = new J.FieldAccess(
//                    randomId(),
//                    Space.EMPTY,
//                    Markers.EMPTY,
//                    (Expression) reference,
//                    padLeft(Space.EMPTY, createIdentifier(
//                            "*",
//                            prefix((PsiElement) importDirective.getNode().findChildByType(KtTokens.MUL)),
//                            null
//                    )),
//                    null
//            );
//        }
        if (reference instanceof J.Identifier) {
            reference = new J.FieldAccess(
                    randomId(),
                    Space.EMPTY,
                    Markers.EMPTY,
                    new J.Empty(randomId(), Space.EMPTY, Markers.EMPTY),
                    padLeft(Space.EMPTY, (J.Identifier) reference),
                    null
            );
        }

        return new J.Import(
                randomId(),
                prefix(importDirective),
                Markers.EMPTY,
                statik,
                (J.FieldAccess) reference,
                // TODO: fix NPE.
                alias != null ? padLeft(prefix(alias), createIdentifier(alias.getNameIdentifier(), null)) : null
        );
    }

    @Override
    public J visitNamedFunction(KtNamedFunction function, ExecutionContext data) {
        ownerStack.push(function);
        try {
            return visitNamedFunction0(function, data);
        } finally {
            ownerStack.pop();
        }
    }

    @NotNull
    private J visitNamedFunction0(KtNamedFunction function, ExecutionContext data) {
        Markers markers = Markers.EMPTY;
        List<J.Annotation> leadingAnnotations = new ArrayList<>();
        List<J.Annotation> lastAnnotations = new ArrayList<>();
        List<J.Modifier> modifiers = mapModifiers(function.getModifierList(), leadingAnnotations, lastAnnotations, data);
        J.TypeParameters typeParameters = null;
        TypeTree returnTypeExpression = null;

        if (function.getTypeParameterList() != null) {
            typeParameters = new J.TypeParameters(
                    randomId(),
                    prefix(function.getTypeParameterList()),
                    Markers.EMPTY,
                    emptyList(),
                    mapTypeParameters(function, data)
            );
        }

        boolean isOpen = function.hasModifier(KtTokens.OPEN_KEYWORD);
        if (!isOpen) {
            modifiers.add(buildFinalModifier().withPrefix(prefix(function.getFunKeyword())));
        }

        modifiers.add(new J.Modifier(randomId(), Space.EMPTY, Markers.EMPTY, "fun", J.Modifier.Type.LanguageExtension, emptyList()));
        J.Identifier name = null;

        if (function.getNameIdentifier() == null) {
            name = createIdentifier("", Space.EMPTY, type(function));
        } else {
            name = createIdentifier(function.getNameIdentifier(), type(function));
        }

        // parameters
        JContainer<Statement> params;
        List<KtParameter> ktParameters = function.getValueParameters();

        if (function.getValueParameterList() == null) {
            throw new UnsupportedOperationException("TODO");
        }

        if (ktParameters.isEmpty()) {
            params = JContainer.build(prefix(function.getValueParameterList()),
                    singletonList(padRight(new J.Empty(randomId(),
                                    // TODO: fix NPE.
                                    prefix(function.getValueParameterList().getRightParenthesis()),
                                    Markers.EMPTY),
                            Space.EMPTY)
                    ), Markers.EMPTY
            );
        } else {
            List<JRightPadded<Statement>> rps = new ArrayList<>();
            for (KtParameter param : ktParameters) {
                rps.add(padRight(convertToStatement(param.accept(this, data)), suffix(param)));
            }
            params = JContainer.build(prefix(function.getValueParameterList()), rps, Markers.EMPTY);
        }

        if (function.getReceiverTypeReference() != null) {
            markers = markers.addIfAbsent(new Extension(randomId()));
            Expression receiver = convertToExpression(function.getReceiverTypeReference().accept(this, data)).withPrefix(prefix(function.getReceiverTypeReference()));
            JRightPadded<J.VariableDeclarations.NamedVariable> infixReceiver = JRightPadded.build(
                            new J.VariableDeclarations.NamedVariable(
                                    randomId(),
                                    Space.EMPTY,
                                    Markers.EMPTY.addIfAbsent(new Extension(randomId())),
                                    createIdentifier("<receiverType>", Space.EMPTY, null, null),
                                    emptyList(),
                                    padLeft(Space.EMPTY, receiver),
                                    null
                            )
                    )
                    .withAfter(suffix(function.getReceiverTypeReference()));

            J.VariableDeclarations implicitParam = new J.VariableDeclarations(
                    randomId(),
                    Space.EMPTY,
                    Markers.EMPTY.addIfAbsent(new Extension(randomId())),
                    emptyList(),
                    emptyList(),
                    null,
                    null,
                    emptyList(),
                    singletonList(infixReceiver)
            );
            implicitParam = implicitParam.withMarkers(implicitParam.getMarkers().addIfAbsent(new TypeReferencePrefix(randomId(), Space.EMPTY)));

            List<JRightPadded<Statement>> newStatements = new ArrayList<>(params.getElements().size() + 1);
            newStatements.add(JRightPadded.build(implicitParam));
            newStatements.addAll(params.getPadding().getElements());
            params = params.getPadding().withElements(newStatements);

        }

        if (function.getTypeReference() != null) {
            markers = markers.addIfAbsent(new TypeReferencePrefix(randomId(), prefix(function.getColon())));
            returnTypeExpression = function.getTypeReference().accept(this, data).withPrefix(prefix(function.getTypeReference()));
        }

        K.TypeConstraints typeConstraints = null;
        if (function.getTypeConstraintList() != null) {
            // todo, find `where` keyword, and use prefix(whereKeyWord)
            // PsiTreeUtil.findSiblingForward(function.getTypeConstraintList(), KtTokens.WHERE_KEYWORD, null);
            typeConstraints = (K.TypeConstraints) function.getTypeConstraintList().accept(this, data);
            Space afterWhere = prefix(function.getTypeConstraintList());
            typeConstraints = typeConstraints.withConstraints(ListUtils.mapFirst(typeConstraints.getConstraints(), constraint -> constraint.withPrefix(afterWhere)))
                    .withPrefix(suffix(function.getTypeReference()));
        }

        J.Block body;
        if (function.getBodyBlockExpression() != null) {
            body = function.getBodyBlockExpression().accept(this, data)
                    .withPrefix(prefix(function.getBodyBlockExpression()));
        } else if (function.getBodyExpression() != null) {
            body = convertToBlock(function.getBodyExpression(), data).withPrefix(prefix(function.getEqualsToken()));
        } else {
            body = null;
        }

        J.MethodDeclaration methodDeclaration = new J.MethodDeclaration(
                randomId(),
                merge(prefix(function), infix(function)),
                markers,
                leadingAnnotations,
                modifiers,
                typeParameters,
                returnTypeExpression,
                new J.MethodDeclaration.IdentifierWithAnnotations(name, emptyList()),
                params,
                null,
                body,
                null,
                methodDeclarationType(function)
        );

        return (typeConstraints == null) ? methodDeclaration : new K.MethodDeclaration(randomId(), Markers.EMPTY, methodDeclaration, typeConstraints);
    }

    @NotNull
    private List<JRightPadded<J.TypeParameter>> mapTypeParameters(KtTypeParameterListOwner owner, ExecutionContext data) {
        List<KtTypeParameter> ktTypeParameters = owner.getTypeParameters();
        List<JRightPadded<J.TypeParameter>> params = new ArrayList<>(ktTypeParameters.size());

        for (KtTypeParameter ktTypeParameter : ktTypeParameters) {
            J.TypeParameter typeParameter = ktTypeParameter.accept(this, data).withPrefix(prefix(ktTypeParameter));
            params.add(padRight(typeParameter, suffix(ktTypeParameter)));
        }

        return params;
    }

    @Override
    public J visitObjectLiteralExpression(KtObjectLiteralExpression expression, ExecutionContext data) {
        KtObjectDeclaration declaration = expression.getObjectDeclaration();
        TypeTree clazz = null;
        Markers markers = Markers.EMPTY;
        JContainer<Expression> args;

        if (declaration.getSuperTypeList() == null) {
            args = JContainer.empty();
            args = args.withMarkers(Markers.build(singletonList(new OmitParentheses(randomId()))));
        } else {
            KtValueArgumentList ktArgs = declaration.getSuperTypeList().getEntries().get(0).getStubOrPsiChild(KtStubElementTypes.VALUE_ARGUMENT_LIST);
            if (ktArgs != null && ktArgs.getArguments().isEmpty()) {
                args = JContainer.build(
                        prefix(ktArgs),
                        singletonList(padRight(new J.Empty(randomId(), prefix(ktArgs.getRightParenthesis()), Markers.EMPTY), Space.EMPTY)
                        ), Markers.EMPTY
                );
            } else {
                throw new UnsupportedOperationException("TODO, support multiple ObjectDeclaration arguments");
            }

            clazz = declaration.getSuperTypeList().accept(this, data).withPrefix(Space.EMPTY);
        }

        // TODO: fix NPE.
        J.Block body = (J.Block) declaration.getBody().accept(this, data);

        if (declaration.getObjectKeyword() != null) {
            markers = markers.add(new KObject(randomId(), Space.EMPTY));
            markers = markers.add(new TypeReferencePrefix(randomId(), prefix(declaration.getColon())));
        }

        return new J.NewClass(
                randomId(),
                prefix(declaration),
                markers,
                null,
                suffix(declaration.getColon()),
                clazz,
                args,
                body,
                null
        );
    }

    @Override
    public J visitObjectDeclaration(KtObjectDeclaration declaration, ExecutionContext data) {
        Markers markers = Markers.EMPTY;
        List<J.Modifier> modifiers = new ArrayList<>();
        List<JRightPadded<TypeTree>> superTypes = new ArrayList<>();
        JContainer<TypeTree> implementings = null;

        if (declaration.getModifierList() != null) {
            modifiers.addAll(mapModifiers(declaration.getModifierList(), emptyList(), emptyList(), data));
        }

        modifiers.add(buildFinalModifier());

        if (!declaration.getAnnotationEntries().isEmpty()) {
            throw new UnsupportedOperationException("TODO");
        }

        JContainer<J.TypeParameter> typeParameters = declaration.getTypeParameterList() == null ? null :
                JContainer.build(prefix(declaration.getTypeParameterList()), mapTypeParameters(declaration, data), Markers.EMPTY);

        if (declaration.getSuperTypeList() != null) {
            TypeTree clazz = (TypeTree) declaration.getSuperTypeList().accept(this, data)
                    .withPrefix(prefix(declaration.getSuperTypeList()));
            superTypes.add(padRight(clazz, Space.EMPTY));
            implementings = JContainer.build(prefix(declaration.getColon()), superTypes, Markers.EMPTY);
        }

        J.Block body;
        if (declaration.getBody() == null) {
            body = new J.Block(
                    randomId(),
                    Space.EMPTY,
                    Markers.EMPTY,
                    padRight(false, Space.EMPTY),
                    emptyList(),
                    Space.EMPTY
            );
            body = body.withMarkers(body.getMarkers().addIfAbsent(new OmitBraces(randomId())));
        } else {
            body = (J.Block) declaration.getBody().accept(this, data);
        }

        if (declaration.getObjectKeyword() != null) {
            markers = markers.add(new KObject(randomId(), Space.EMPTY));
            markers = markers.add(new TypeReferencePrefix(randomId(), prefix(declaration.getColon())));
        }

        J.Identifier name;
        if (declaration.getNameIdentifier() != null) {
            name = createIdentifier(declaration.getNameIdentifier(), type(declaration));
        } else {
            name = createIdentifier(declaration.isCompanion() ? "<companion>" : "", Space.EMPTY, type(declaration))
                    .withMarkers(Markers.EMPTY.addIfAbsent(new Implicit(randomId())));
        }


        return new J.ClassDeclaration(
                randomId(),
                prefix(declaration),
                markers,
                emptyList(),
                modifiers,
                new J.ClassDeclaration.Kind(
                        randomId(),
                        prefix(declaration.getObjectKeyword()),
                        Markers.EMPTY,
                        emptyList(), // TODO
                        J.ClassDeclaration.Kind.Type.Class
                ),
                name,
                typeParameters,
                null,
                null,
                implementings,
                null,
                body,
                (JavaType.FullyQualified) type(declaration)
        );
    }

    @Override
    @Nullable
    public J visitPackageDirective(KtPackageDirective directive, ExecutionContext data) {
        if (directive.getPackageNameExpression() == null) {
            return null;
        }
        return new J.Package(
                randomId(),
                prefix(directive),
                Markers.EMPTY,
                (Expression) directive.getPackageNameExpression().accept(this, data),
                emptyList()
        );
    }

    @Override
    public J visitPrefixExpression(KtPrefixExpression expression, ExecutionContext data) {
        assert expression.getBaseExpression() != null;
        J.Unary.Type type = mapJUnaryType(expression.getOperationReference());
        if (type == null) {
            throw new UnsupportedOperationException("TODO");
        }
        return new J.Unary(
                randomId(),
                prefix(expression),
                Markers.EMPTY,
                padLeft(prefix(expression.getOperationReference()), type),
                expression.getBaseExpression().accept(this, data).withPrefix(suffix(expression.getOperationReference())),
                type(expression)
        );
    }

    @Override
    public J visitPostfixExpression(KtPostfixExpression expression, ExecutionContext data) {
        // TODO: fix NPE.
        J j = expression.getBaseExpression().accept(this, data);
        IElementType referencedNameElementType = expression.getOperationReference().getReferencedNameElementType();
        if (referencedNameElementType == KtTokens.EXCLEXCL) {
            j = j.withMarkers(j.getMarkers().addIfAbsent(new CheckNotNull(randomId(), prefix(expression.getOperationReference()))));
        } else if (referencedNameElementType == KtTokens.PLUSPLUS) {
            j = new J.Unary(randomId(), prefix(expression), Markers.EMPTY, padLeft(prefix(expression.getOperationReference()), J.Unary.Type.PostIncrement), (Expression) j, type(expression));
        } else if (referencedNameElementType == KtTokens.MINUSMINUS) {
            j = new J.Unary(randomId(), prefix(expression), Markers.EMPTY, padLeft(prefix(expression.getOperationReference()), J.Unary.Type.PostDecrement), (Expression) j, type(expression));
        } else {
            throw new UnsupportedOperationException("TODO");
        }
        return j;
    }

    @Override
    public J visitProperty(KtProperty property, ExecutionContext data) {
        Markers markers = Markers.EMPTY;
        List<J.Annotation> leadingAnnotations = new ArrayList<>();
        List<J.Annotation> lastAnnotations = new ArrayList<>();
        List<J.Modifier> modifiers = mapModifiers(property.getModifierList(), leadingAnnotations, lastAnnotations, data);
        TypeTree typeExpression = null;
        List<JRightPadded<J.VariableDeclarations.NamedVariable>> variables = new ArrayList<>();
        J.MethodDeclaration getter = null;
        J.MethodDeclaration setter = null;
        JRightPadded<Expression> receiver = null;
        JContainer<J.TypeParameter> typeParameters = property.getTypeParameterList() != null ?
                JContainer.build(prefix(property.getTypeParameterList()), mapTypeParameters(property, data), Markers.EMPTY) : null;
        K.TypeConstraints typeConstraints = null;
        boolean isSetterFirst = false;

        modifiers.add(new J.Modifier(
                Tree.randomId(),
                prefix(property.getValOrVarKeyword()),
                Markers.EMPTY,
                property.isVar() ? "var" : null,
                property.isVar() ? J.Modifier.Type.LanguageExtension : J.Modifier.Type.Final,
                Collections.emptyList() // FIXME
        ));

        // Receiver
        if (property.getReceiverTypeReference() != null) {
            Expression receiverExp = convertToExpression(property.getReceiverTypeReference().accept(this, data).withPrefix(prefix(property.getReceiverTypeReference())));
            receiver = padRight(receiverExp, suffix(property.getReceiverTypeReference()));
            markers = markers.addIfAbsent(new Extension(randomId()));
        }

        JLeftPadded<Expression> initializer = null;
        if (property.getInitializer() != null) {
            initializer = padLeft(prefix(property.getEqualsToken()),
                    convertToExpression(property.getInitializer().accept(this, data)
                            .withPrefix(prefix(property.getInitializer()))));
        } else if (property.getDelegate() != null) {
            Space afterByKeyword = prefix(property.getDelegate().getExpression());
            Expression initializerExp = convertToExpression(property.getDelegate().accept(this, data)).withPrefix(afterByKeyword);
            initializer = padLeft(prefix(property.getDelegate()), initializerExp);
            markers = markers.addIfAbsent(new By(randomId()));
        }

        Markers rpMarker = Markers.EMPTY;
        Space maybeBeforeSemicolon = Space.EMPTY;
        if (property.getLastChild().getNode().getElementType() == KtTokens.SEMICOLON) {
            rpMarker = rpMarker.addIfAbsent(new Semicolon(randomId()));
            maybeBeforeSemicolon = prefix(property.getLastChild());
        }

        J.VariableDeclarations.NamedVariable namedVariable =
                new J.VariableDeclarations.NamedVariable(
                        randomId(),
                        prefix(property.getNameIdentifier()),
                        Markers.EMPTY,
                        // TODO: fix NPE.
                        createIdentifier(property.getNameIdentifier(), type(property)).withPrefix(Space.EMPTY),
                        emptyList(),
                        initializer,
                        variableType(property)
                );

        variables.add(padRight(namedVariable, maybeBeforeSemicolon).withMarkers(rpMarker));

        if (property.getColon() != null) {
            markers = markers.addIfAbsent(new TypeReferencePrefix(randomId(), prefix(property.getColon())));
            // TODO: fix NPE.
            typeExpression = (TypeTree) property.getTypeReference().accept(this, data);
            typeExpression = typeExpression.withPrefix(suffix(property.getColon()));
        }

        if (property.getGetter() != null) {
            getter = (J.MethodDeclaration) property.getGetter().accept(this, data);
        }

        if (property.getSetter() != null) {
            setter = (J.MethodDeclaration) property.getSetter().accept(this, data);
        }

        if (getter != null && setter != null) {
            isSetterFirst = property.getSetter().getTextRange().getStartOffset() < property.getGetter().getTextRange().getStartOffset();
        }

        J.VariableDeclarations variableDeclarations = new J.VariableDeclarations(
                Tree.randomId(),
                merge(prefix(property), infix(property)), // overlaps with right-padding of previous statement
                markers,
                leadingAnnotations,
                modifiers,
                typeExpression,
                null,
                Collections.emptyList(),
                variables
        );

        if (getter != null || setter != null || receiver != null) {
            return new K.Property(
                    randomId(),
                    prefix(property),
                    markers,
                    typeParameters,
                    variableDeclarations.withPrefix(Space.EMPTY),
                    typeConstraints,
                    getter,
                    setter,
                    isSetterFirst,
                    receiver
            );
        } else {
            return variableDeclarations;
        }

    }

    private List<J.Modifier> mapModifiers(@Nullable KtModifierList modifierList,
                                          @NonNull List<J.Annotation> leadingAnnotations,
                                          @NonNull List<J.Annotation> lastAnnotations,
                                          ExecutionContext data) {
        ArrayList<J.Modifier> modifiers = new ArrayList<>();
        if (modifierList == null) {
            return modifiers;
        }

        boolean isLeadingAnnotation = true;

        // don't use iterator of `PsiTreeUtil.firstChild` and `getNextSibling`, since it could skip one layer, example test "paramAnnotation"
        // also don't use `modifierList.getChildren()` since it could miss some element
        List<PsiElement> children = getAllChildren(modifierList);

        for (PsiElement child : children) {
            boolean isAnnotation = child instanceof KtAnnotationEntry;

            if (isLeadingAnnotation && isAnnotation) {
                KtAnnotationEntry ktAnnotationEntry = (KtAnnotationEntry) child;
                leadingAnnotations.add((J.Annotation) ktAnnotationEntry.accept(this, data));
            } else {
                if (child instanceof LeafPsiElement && child.getNode().getElementType() instanceof KtModifierKeywordToken) {
                    isLeadingAnnotation = false;
                    modifiers.add(mapModifier(child, emptyList()));
                } else if (isAnnotation) {
                    List<J.Annotation> annotations = new ArrayList<>();
                    annotations.add((J.Annotation) ((KtAnnotationEntry) child).accept(this, data));
                    modifiers.add(new J.Modifier(randomId(), Space.EMPTY, Markers.EMPTY, "", J.Modifier.Type.LanguageExtension, annotations));
                }
            }
        }

        // TODO. handle lastAnnotations
        return modifiers;
    }


    @Override
    public J visitPropertyDelegate(KtPropertyDelegate delegate, ExecutionContext data) {
        if (delegate.getExpression() == null) {
            throw new UnsupportedOperationException("TODO");
        }
        // Markers initMarkers = Markers.EMPTY.addIfAbsent(new By(randomId()));
        return delegate.getExpression().accept(this, data)
                .withPrefix(prefix(delegate));
    }

    @Override
    public J visitSimpleNameExpression(KtSimpleNameExpression expression, ExecutionContext data) {
        FirBasedSymbol<?> symbol = psiElementAssociations.symbol(expression);
        if (symbol instanceof FirVariableSymbol<?>) {
            JavaType.FullyQualified owner = null;
            ConeClassLikeLookupTag lookupTag = ClassMembersKt.containingClassLookupTag((FirCallableSymbol<?>) symbol);
            if (lookupTag != null && !(LookupTagUtilsKt.toSymbol(lookupTag, firSession) instanceof FirAnonymousObjectSymbol)) {
                // TODO check type attribution for `FirAnonymousObjectSymbol` case
                owner =
                        // TODO: fix NPE.
                        (JavaType.FullyQualified) typeMapping.type(LookupTagUtilsKt.toFirRegularClassSymbol(lookupTag, firSession).getFir());
            }
            return createIdentifier(
                    expression,
                    // TODO: fix NPE.
                    typeMapping.variableType((FirVariableSymbol<?>) symbol, owner, psiElementAssociations.getFile().getSymbol())
            );
        }
        return createIdentifier(expression, type(expression));
    }

    @Override
    public J visitStringTemplateExpression(KtStringTemplateExpression expression, ExecutionContext data) {
        KtStringTemplateEntry[] entries = expression.getEntries();
        boolean hasStringTemplateEntry = Arrays.stream(entries).anyMatch(x ->
                x instanceof KtBlockStringTemplateEntry ||
                        x instanceof KtSimpleNameStringTemplateEntry);

        if (hasStringTemplateEntry) {
            String delimiter = expression.getFirstChild().getText();
            List<J> values = new ArrayList<>();

            for (KtStringTemplateEntry entry : entries) {
                values.add(entry.accept(this, data));
            }

            return new K.KString(
                    randomId(),
                    prefix(expression),
                    Markers.EMPTY,
                    delimiter,
                    values,
                    type(expression)
            );
        }

        String valueSource = expression.getText();
        StringBuilder valueSb = new StringBuilder();
        Arrays.stream(entries).forEach(x -> valueSb.append(x.getText()));

        return new J.Literal(
                randomId(),
                Space.EMPTY,
                Markers.EMPTY,
                valueSb.toString(),
                valueSource,
                null,
                primitiveType(expression)
        ).withPrefix(prefix(expression));
    }

    @Override
    public J visitStringTemplateEntry(KtStringTemplateEntry entry, ExecutionContext data) {
        PsiElement leaf = entry.getFirstChild();
        if (!(leaf instanceof LeafPsiElement)) {
            throw new UnsupportedOperationException("Unsupported KtStringTemplateEntry child");
        }

        return new J.Literal(
                Tree.randomId(),
                Space.EMPTY,
                Markers.EMPTY,
                leaf.getText(),
                "\"" + leaf.getText() + "\"", // todo, support text block
                null,
                primitiveType(entry)
        );
    }

    @Override
    public J visitSuperTypeList(KtSuperTypeList list, ExecutionContext data) {
        List<KtSuperTypeListEntry> typeListEntries = list.getEntries();

        if (typeListEntries.size() > 1) {
            throw new UnsupportedOperationException("KtSuperTypeList size is bigger than 1, TODO");
        }
        return typeListEntries.get(0).accept(this, data);
    }

    @Override
    public J visitSuperTypeCallEntry(KtSuperTypeCallEntry call, ExecutionContext data) {
        // TODO: fix NPE.
        return call.getTypeReference().accept(this, data);
    }

    @Override
    public J visitTypeReference(KtTypeReference typeReference, ExecutionContext data) {
        List<J.Annotation> leadingAnnotations = new ArrayList<>();
        List<J.Annotation> lastAnnotations = new ArrayList<>();
        boolean prefixConsumed = false;

        mapModifiers(typeReference.getModifierList(), leadingAnnotations, lastAnnotations, data);
        if (!leadingAnnotations.isEmpty()) {
            leadingAnnotations = ListUtils.mapFirst(leadingAnnotations, anno -> anno.withPrefix(prefix(typeReference)));
            prefixConsumed = true;
        }

        J j = typeReference.getTypeElement().accept(this, data)
                .withPrefix(prefixConsumed ? prefix(typeReference.getTypeElement()) : merge(prefix(typeReference), prefix(typeReference.getTypeElement())));
        if (j instanceof K.FunctionType &&
                typeReference.getModifierList() != null &&
                typeReference.getModifierList().hasModifier(KtTokens.SUSPEND_KEYWORD)) {
            j = ((K.FunctionType) j).withModifiers(singletonList(mapModifier(typeReference.getModifierList().getModifier(KtTokens.SUSPEND_KEYWORD), emptyList())))
                    .withLeadingAnnotations(leadingAnnotations);
            if (((K.FunctionType) j).getReceiver() != null) {
                // TODO check if we can simplify this
                j = ((K.FunctionType) j).withReceiver(
                        ((K.FunctionType) j).getReceiver().withElement(
                                ((K.FunctionType) j).getReceiver().getElement().withPrefix(
                                        prefix(typeReference.getTypeElement())
                                )
                        )
                );
            }
        } else if (j instanceof J.Identifier) {
            j = ((J.Identifier) j).withAnnotations(leadingAnnotations);
        }
        return j;
    }

    @Override
    public J visitUserType(KtUserType type, ExecutionContext data) {
        J.Identifier name = (J.Identifier) type.getReferenceExpression().accept(this, data);
        NameTree nameTree = name;

        if (type.getQualifier() != null) {
            Expression select = convertToExpression(type.getQualifier().accept(this, data)).withPrefix(prefix(type.getQualifier()));
            nameTree = new J.FieldAccess(randomId(), Space.EMPTY, Markers.EMPTY, select, padLeft(suffix(type.getQualifier()), name), null);
        }

        List<KtTypeProjection> typeArguments = type.getTypeArguments();
        List<JRightPadded<Expression>> parameters = new ArrayList<>(typeArguments.size());
        if (!typeArguments.isEmpty()) {
            for (KtTypeProjection typeProjection : typeArguments) {
                parameters.add(padRight(convertToExpression(typeProjection.accept(this, data)), suffix(typeProjection)));
            }

            return new J.ParameterizedType(
                    randomId(),
                    Space.EMPTY,
                    Markers.EMPTY,
                    nameTree,
                    JContainer.build(prefix(type.getTypeArgumentList()), parameters, Markers.EMPTY),
                    type(type)
            );
        }

        return nameTree;
    }

    @Override
    public J visitValueArgumentList(KtValueArgumentList list, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    /*====================================================================
     * Mapping methods
     * ====================================================================*/
    @Nullable
    private K.Binary.Type mapKBinaryType(KtOperationReferenceExpression operationReference) {
        IElementType elementType = operationReference.getOperationSignTokenType();
        if (elementType == null) {
            return null;
        }

        if (elementType == KtTokens.NOT_IN) {
            return K.Binary.Type.NotContains;
        } else if (elementType == KtTokens.RANGE) {
            return K.Binary.Type.RangeTo;
        } else if (elementType == KtTokens.RANGE_UNTIL) {
            return K.Binary.Type.RangeUntil;
        } else if (elementType == KtTokens.IN_KEYWORD) {
            return K.Binary.Type.Contains;
        } else if (elementType == KtTokens.EQEQEQ) {
            return K.Binary.Type.IdentityEquals;
        } else if (elementType == KtTokens.EXCLEQEQEQ) {
            return K.Binary.Type.IdentityNotEquals;
        } else if (elementType == KtTokens.ELVIS) {
            return K.Binary.Type.Elvis;
        } else if (elementType == KtTokens.EQ) {
            return null;
        } else {
            throw new UnsupportedOperationException("Unsupported OPERATION_REFERENCE type: " + elementType);
        }
    }

    @Nullable
    private J.Binary.Type mapJBinaryType(KtOperationReferenceExpression operationReference) {
        IElementType elementType = operationReference.getOperationSignTokenType();

        if (elementType == null) {
            String operator = operationReference.getText();
            if ("and".equals(operator)) {
                return J.Binary.Type.BitAnd;
            } else if ("or".equals(operator)) {
                return J.Binary.Type.BitOr;
            } else if ("xor".equals(operator)) {
                return J.Binary.Type.BitXor;
            } else if ("shl".equals(operator)) {
                return J.Binary.Type.LeftShift;
            } else if ("shr".equals(operator)) {
                return J.Binary.Type.RightShift;
            } else if ("ushr".equals(operator)) {
                return J.Binary.Type.UnsignedRightShift;
            }
        }

        if (elementType == KtTokens.PLUS)
            return J.Binary.Type.Addition;
        else if (elementType == KtTokens.MINUS)
            return J.Binary.Type.Subtraction;
        else if (elementType == KtTokens.MUL)
            return J.Binary.Type.Multiplication;
        else if (elementType == KtTokens.DIV)
            return J.Binary.Type.Division;
        else if (elementType == KtTokens.EQEQ)
            return J.Binary.Type.Equal; // TODO should this not be mapped to `Object#equals(Object)`?
        else if (elementType == KtTokens.EXCLEQ)
            return J.Binary.Type.NotEqual; // TODO should this not be mapped to `!Object#equals(Object)`?
        else if (elementType == KtTokens.GT)
            return J.Binary.Type.GreaterThan;
        else if (elementType == KtTokens.GTEQ)
            return J.Binary.Type.GreaterThanOrEqual;
        else if (elementType == KtTokens.LT)
            return J.Binary.Type.LessThan;
        else if (elementType == KtTokens.LTEQ)
            return J.Binary.Type.LessThanOrEqual;
        else if (elementType == KtTokens.PERC)
            return J.Binary.Type.Modulo;
        else if (elementType == KtTokens.ANDAND)
            return J.Binary.Type.And;
        else if (elementType == KtTokens.OROR)
            return J.Binary.Type.Or;
        else
            return null;
    }

    @Nullable
    private J.Unary.Type mapJUnaryType(KtSimpleNameExpression operationReference) {
        IElementType elementType = operationReference.getReferencedNameElementType();

        if (elementType == KtTokens.EXCL) {
            return J.Unary.Type.Not;
        } else if (elementType == KtTokens.MINUSMINUS) {
            return J.Unary.Type.PreDecrement;
        } else if (elementType == KtTokens.PLUSPLUS) {
            return J.Unary.Type.PreIncrement;
        } else if (elementType == KtTokens.MINUS) {
            return J.Unary.Type.Negative;
        } else if (elementType == KtTokens.PLUSEQ) {
            return J.Unary.Type.Positive;
        } else if (elementType == KtTokens.PLUS) {
            return J.Unary.Type.Positive;
        } else {
            return null;
        }
    }

    private J.Modifier.Type mapModifierType(PsiElement modifier) {
        switch (modifier.getText()) {
            case "public":
                return J.Modifier.Type.Public;
            case "private":
                return J.Modifier.Type.Private;
            case "sealed":
                return J.Modifier.Type.Sealed;
            case "annotation":
            case "data":
            case "enum":
            case "open":
            case "inner":
            case "value":
                return J.Modifier.Type.LanguageExtension;
            case "abstract":
                return J.Modifier.Type.Abstract;
            default:
                throw new UnsupportedOperationException("Unsupported ModifierType : " + modifier);
        }
    }

    private J.ControlParentheses<Expression> buildIfCondition(KtIfExpression expression) {
        return new J.ControlParentheses<>(randomId(),
                prefix(expression.getLeftParenthesis()),
                Markers.EMPTY,
                // TODO: fix NPE.
                padRight(convertToExpression(expression.getCondition().accept(this, executionContext))
                                .withPrefix(suffix(expression.getLeftParenthesis())),
                        prefix(expression.getRightParenthesis()))
        );
    }

    private JRightPadded<Statement> buildIfThenPart(KtIfExpression expression) {
        // TODO: fix NPE.
        return padRight(convertToStatement(expression.getThen().accept(this, executionContext))
                        .withPrefix(prefix(expression.getThen().getParent())),
                Space.EMPTY);
    }

    @Nullable
    private J.If.Else buildIfElsePart(KtIfExpression expression) {
        if (expression.getElse() == null) {
            return null;
        }

        return new J.If.Else(
                randomId(),
                prefix(expression.getElseKeyword()),
                Markers.EMPTY,
                padRight(convertToStatement(expression.getElse().accept(this, executionContext))
                        .withPrefix(suffix(expression.getElseKeyword())), Space.EMPTY)
        );
    }

    /*====================================================================
     * Type related methods
     * ====================================================================*/
    @Nullable
    private JavaType type(@Nullable KtElement psi) {
        if (psi == null) {
            return JavaType.Unknown.getInstance();
        }
        return psiElementAssociations.type(psi, owner(psi));
    }

    // rename
    @Nullable
    private JavaType type2(@Nullable KtElement psi) {
        if (psi == null) {
            return JavaType.Unknown.getInstance();
        }
        return psiElementAssociations2.type(psi);
    }

    private JavaType.Primitive primitiveType(PsiElement expression) {
        // TODO: fix NPE.
        FirElement firElement = psiElementAssociations.primary(expression);
        if (firElement instanceof FirConstExpression) {
            return typeMapping.primitive((ConeClassLikeType) ((FirResolvedTypeRef) ((FirConstExpression<?>) firElement).getTypeRef()).getType());
        }

        if (firElement instanceof FirStringConcatenationCall) {
            return JavaType.Primitive.String;
        }

        return null;
    }

    @Nullable
    private JavaType.Variable variableType(PsiElement psi) {
        if (psi instanceof KtDeclaration) {
            FirBasedSymbol<?> basedSymbol = psiElementAssociations.symbol((KtDeclaration) psi);
            if (basedSymbol instanceof FirVariableSymbol) {
                return (JavaType.Variable) typeMapping.type(basedSymbol.getFir(), owner(psi));
            }
        }
        return null;
    }

    @Nullable
    private JavaType.Method methodDeclarationType(PsiElement psi) {
        if (psi instanceof KtDeclaration) {
            FirBasedSymbol<?> basedSymbol = psiElementAssociations.symbol((KtDeclaration) psi);
            if (basedSymbol != null && basedSymbol.getFir() instanceof FirFunction) {
                return psiElementAssociations.getTypeMapping().methodDeclarationType((FirFunction) basedSymbol.getFir(), null, psiElementAssociations.getFile().getSymbol());
            }
        }
//        if (psi instanceof KtNamedFunction) {
//            FirBasedSymbol<?> basedSymbol = psiElementAssociations.symbol((KtNamedFunction) psi);
//            if (basedSymbol instanceof FirNamedFunctionSymbol) {
//                FirNamedFunctionSymbol functionSymbol = (FirNamedFunctionSymbol) basedSymbol;
//                return psiElementAssociations.getTypeMapping().methodDeclarationType(functionSymbol.getFir(), null, psiElementAssociations.getFile().getSymbol());
//            }
//        } else if (psi instanceof KtPropertyAccessor) {
//            // todo, more generic logic
//            FirBasedSymbol<?> basedSymbol = psiElementAssociations.symbol((KtDeclaration) psi);
//            if (basedSymbol instanceof FirPropertyAccessorSymbol) {
//                FirPropertyAccessorSymbol propertyAccessorSymbol = (FirPropertyAccessorSymbol) basedSymbol;
//                return psiElementAssociations.getTypeMapping().methodDeclarationType(propertyAccessorSymbol.getFir(), null, psiElementAssociations.getFile().getSymbol());
//            }
//        }
        return null;
    }

    @Nullable
    private JavaType.Method methodInvocationType(PsiElement psi) {
        FirElement firElement = psiElementAssociations.component(psi);
        if (firElement == null) {
            // TODO analyze why this is required (example `MethodReferenceTest#noReceiver()`)
            firElement = psiElementAssociations.component(psi.getParent());
        }
        if (firElement instanceof FirFunctionCall) {
            return typeMapping.methodInvocationType((FirFunctionCall) firElement, psiElementAssociations.getFile().getSymbol());
        }
        return null;
    }

    /*====================================================================
     * Other helper methods
     * ====================================================================*/
    private J.Identifier createIdentifier(PsiElement name, @Nullable JavaType type, @Nullable JavaType.Variable fieldType) {
        return createIdentifier(name.getNode().getText(), prefix(name), type, fieldType);
    }

    private J.Identifier createIdentifier(PsiElement name, @Nullable JavaType type) {
        return createIdentifier(name.getNode().getText(), prefix(name), type);
    }

    private J.Identifier createIdentifier(String name, Space prefix,
                                          @Nullable JavaType type) {
        return createIdentifier(name, prefix,
                type instanceof JavaType.Variable ? ((JavaType.Variable) type).getType() : type,
                type instanceof JavaType.Variable ? (JavaType.Variable) type : null);
    }

    private J.Identifier createIdentifier(String name, Space prefix,
                                          @Nullable JavaType type,
                                          @Nullable JavaType.Variable fieldType) {
        return new J.Identifier(
                randomId(),
                prefix,
                Markers.EMPTY,
                emptyList(),
                name,
                type,
                fieldType
        );
    }

    private J.Block convertToBlock(KtExpression ktExpression, ExecutionContext data) {
        Expression returnExpr = convertToExpression(ktExpression.accept(this, data)).withPrefix(Space.EMPTY);
        K.KReturn kreturn = new K.KReturn(randomId(), new J.Return(randomId(), prefix(ktExpression), Markers.EMPTY.addIfAbsent(new ImplicitReturn(randomId())), returnExpr), null);
        return new J.Block(
                randomId(),
                Space.EMPTY,
                Markers.EMPTY.addIfAbsent(new OmitBraces(randomId()))
                        .addIfAbsent(new SingleExpressionBlock(randomId())),
                JRightPadded.build(false),
                singletonList(JRightPadded.build(kreturn)),
                Space.EMPTY
        );
    }

    private <J2 extends J> JRightPadded<J2> maybeSemicolon(J2 j, KtElement element) {
        PsiElement maybeSemicolon = element.getLastChild();
        boolean hasSemicolon = maybeSemicolon instanceof LeafPsiElement && ((LeafPsiElement) maybeSemicolon).getElementType() == KtTokens.SEMICOLON;
        return hasSemicolon ? new JRightPadded<>(j, prefix(maybeSemicolon), Markers.EMPTY.add(new Semicolon(randomId())))
                : JRightPadded.build(j);
    }

    private <T> JLeftPadded<T> padLeft(Space left, T tree) {
        return new JLeftPadded<>(left, tree, Markers.EMPTY);
    }

    private <T> JRightPadded<T> padRight(T tree, @Nullable Space right) {
        return padRight(tree, right, Markers.EMPTY);
    }

    private <T> JRightPadded<T> padRight(T tree, @Nullable Space right, Markers markers) {
        return new JRightPadded<>(tree, right == null ? Space.EMPTY : right, markers);
    }

    @SuppressWarnings("unchecked")
    private <J2 extends J> J2 convertToExpression(J j) {
        if (j instanceof Statement && !(j instanceof Expression)) {
            j = new K.StatementExpression(randomId(), (Statement) j);
        }
        return (J2) j;
    }

    private Statement convertToStatement(J j) {
        if (!(j instanceof Statement) && j instanceof Expression) {
            j = new K.ExpressionStatement(randomId(), (Expression) j);
        }
        if (!(j instanceof Statement)) {
            throw new UnsupportedOperationException("TODO");
        }
        return (Statement) j;
    }

    private Space prefix(@Nullable PsiElement element) {
        return prefix(element, false);
    }

    @NotNull
    private Space prefix(@Nullable PsiElement element, boolean checkParent) {
        if (element == null) {
            return Space.EMPTY;
        }

        PsiElement whitespace = element.getPrevSibling();
        while (checkParent && (whitespace == null || !isSpace(whitespace.getNode()))) {
            element = element.getParent();
            whitespace = element.getPrevSibling();
        }
        if (whitespace == null || !isSpace(whitespace.getNode())) {
            return Space.EMPTY;
        }
        while (whitespace.getPrevSibling() != null && isSpace(whitespace.getPrevSibling().getNode())) {
            whitespace = whitespace.getPrevSibling();
        }
        return space(whitespace);
    }

    private Space openPrefix(@Nullable PsiElement element) {
        if (element == null) {
            return Space.EMPTY;
        }

        PsiElement whitespace = prev(element);
        if (whitespace == null || !isSpace(whitespace.getNode())) {
            return Space.EMPTY;
        }
        PsiElement prev;
        while ((prev = prev(whitespace)) != null && isSpace(prev.getNode())) {
            whitespace = prev;
        }
        return space(whitespace);
    }

    private Space suffix(@Nullable PsiElement element) {
        if (element == null) {
            return Space.EMPTY;
        }

        PsiElement whitespace = element.getLastChild();
        if (whitespace == null || !isSpace(whitespace.getNode())) {
            whitespace = element.getNextSibling();
        } else {
            while (whitespace.getPrevSibling() != null && isSpace(whitespace.getPrevSibling().getNode())) {
                whitespace = whitespace.getPrevSibling();
            }
        }
        if (whitespace == null || !isSpace(whitespace.getNode())) {
            return Space.EMPTY;
        }
        return space(whitespace);
    }

    private Space infix(@Nullable PsiElement element) {
        if (element == null) {
            return Space.EMPTY;
        }
        List<PsiElement> children = getAllChildren(element);

        for (PsiElement child : children) {
            if (!isSpace(child.getNode())) {
                return prefix(child);
            }
        }

        return Space.EMPTY;
    }

    private boolean isSpace(ASTNode node) {
        IElementType elementType = node.getElementType();
        return elementType == KtTokens.WHITE_SPACE ||
                elementType == KtTokens.BLOCK_COMMENT ||
                elementType == KtTokens.EOL_COMMENT ||
                elementType == KtTokens.DOC_COMMENT ||
                isCRLF(node);
    }

    private boolean isWhiteSpace(@Nullable PsiElement node) {
        if (node == null) {
            return false;
        }
        return node instanceof PsiWhiteSpace || isCRLF(node.getNode());
    }

    private boolean isCRLF(ASTNode node) {
        return node instanceof PsiErrorElementImpl && node.getText().equals("\r");
    }

    private String nodeRangeText(@Nullable ASTNode first, @Nullable ASTNode last) {
        StringBuilder builder = new StringBuilder();
        while (first != null) {
            builder.append(first.getText());
            if (first == last) {
                break;
            }
            first = first.getTreeNext();
        }
        return builder.toString();
    }


    private List<J.Annotation> mapAnnotations(List<KtAnnotationEntry> ktAnnotationEntries, ExecutionContext data) {
        return ktAnnotationEntries.stream()
                .map(annotation -> (J.Annotation) annotation.accept(this, data))
                .collect(Collectors.toList());
    }

    private J.VariableDeclarations mapDestructuringDeclaration(KtDestructuringDeclaration ktDestructuringDeclaration, ExecutionContext data) {
        List<KtDestructuringDeclarationEntry> entries = ktDestructuringDeclaration.getEntries();
        List<JRightPadded<J.VariableDeclarations.NamedVariable>> variables = new ArrayList<>(entries.size());

        for (KtDestructuringDeclarationEntry ktDestructuringDeclarationEntry : entries) {
            J.Identifier name = (J.Identifier) ktDestructuringDeclarationEntry.accept(this, data);

            J.VariableDeclarations.NamedVariable namedVariable = new J.VariableDeclarations.NamedVariable(
                    randomId(),
                    Space.EMPTY,
                    Markers.EMPTY,
                    name,
                    emptyList(),
                    null,
                    variableType(ktDestructuringDeclaration)
            );
            variables.add(padRight(namedVariable, suffix(ktDestructuringDeclarationEntry)));
        }

        return new J.VariableDeclarations(
                randomId(),
                prefix(ktDestructuringDeclaration),
                Markers.EMPTY.addIfAbsent(new OmitEquals(randomId())),
                emptyList(),
                emptyList(),
                null,
                null,
                emptyList(),
                variables
        );
    }

    private J.Modifier mapModifier(PsiElement modifier, List<J.Annotation> annotations) {
        Space prefix = prefix(modifier);
        J.Modifier.Type type;
        String keyword = null;
        switch (modifier.getText()) {
            case "public":
                type = J.Modifier.Type.Public;
                break;
            case "protected":
                type = J.Modifier.Type.Protected;
                break;
            case "private":
                type = J.Modifier.Type.Private;
                break;
            case "abstract":
                type = J.Modifier.Type.Abstract;
                break;
            case "val":
                type = J.Modifier.Type.Final;
                break;
            default:
                type = J.Modifier.Type.LanguageExtension;
                keyword = modifier.getText();
                break;
        }
        return new J.Modifier(
                randomId(),
                prefix,
                Markers.EMPTY,
                keyword,
                type,
                annotations
        );
    }

    private List<JRightPadded<Statement>> mapParameters(@Nullable KtParameterList list, ExecutionContext data) {
        if (list == null) {
            return emptyList();
        }

        List<KtParameter> ktParameters = list.getParameters();
        List<JRightPadded<Statement>> statements = new ArrayList<>(ktParameters.size());

        for (int i = 0; i < ktParameters.size(); i++) {
            KtParameter ktParameter = ktParameters.get(i);
            Statement statement = convertToStatement(ktParameter.accept(this, data));
            statements.add(maybeTrailingComma(ktParameter, padRight(statement, suffix(ktParameter)), i == ktParameters.size() - 1));
        }

        if (ktParameters.isEmpty()) {
            Statement param = new J.Empty(randomId(), prefix(list.getRightParenthesis()), Markers.EMPTY);
            statements.add(padRight(param, Space.EMPTY));
        }

        return statements;
    }

    @Nullable
    private JContainer<Expression> mapFunctionCallArguments(@Nullable KtValueArgumentList argumentList, ExecutionContext data) {
        if (argumentList == null) {
            return null;
        }

        List<KtValueArgument> ktValueArguments = argumentList.getArguments();
        List<JRightPadded<Expression>> expressions = new ArrayList<>(ktValueArguments.size());

        for (int i = 0; i < ktValueArguments.size(); i++) {
            KtValueArgument ktValueArgument = ktValueArguments.get(i);
            Expression expr = convertToExpression(ktValueArgument.accept(this, data));
            expressions.add(maybeTrailingComma(ktValueArgument, padRight(expr, suffix(ktValueArgument)), i == ktValueArguments.size() - 1));
        }

        if (ktValueArguments.isEmpty()) {
            Expression arg = new J.Empty(randomId(), prefix(argumentList.getRightParenthesis()), Markers.EMPTY);
            expressions.add(padRight(arg, Space.EMPTY));
        }

        return JContainer.build(prefix(argumentList), expressions, Markers.EMPTY);
    }


    private Space space(PsiElement node) {
        Space space = null;
        PsiElement preNode = null;

        for (; node != null; node = next(node)) {
            PsiElement finalNode = node;
            if (isWhiteSpace(node)) {
                if (space == null) {
                    space = Space.build(node.getText(), emptyList());
                } else {
                    if (isWhiteSpace(preNode)) {
                        // merge space
                        space = space.withWhitespace(space.getWhitespace() + node.getText());
                    } else {
                        space = space.withComments(ListUtils.mapLast(space.getComments(), c -> c.withSuffix(finalNode.getText())));
                    }
                }
            } else if (node instanceof PsiComment) {
                if (space == null) {
                    space = Space.EMPTY;
                }
                String nodeText = node.getText();
                boolean isBlockComment = ((PsiComment) node).getTokenType() == KtTokens.BLOCK_COMMENT;
                String comment = isBlockComment ? nodeText.substring(2, nodeText.length() - 2) : nodeText.substring(2);
                space = space.withComments(ListUtils.concat(space.getComments(), new TextComment(isBlockComment, comment, "", Markers.EMPTY)));
            } else {
                break;
            }

            preNode = node;
        }
        return space == null ? Space.EMPTY : space;
    }

    @Nullable
    private PsiElement prev(PsiElement node) {
        return node.getPrevSibling();
    }

    @Nullable
    private PsiElement next(PsiElement node) {
        return node.getNextSibling();
    }

    private Space merge(Space s1, Space s2) {
        if (s1.isEmpty()) {
            return s2;
        } else if (s2.isEmpty()) {
            return s1;
        }

        if (s1.getComments().isEmpty()) {
            return Space.build(s1.getWhitespace() + s2.getWhitespace(), s2.getComments());
        } else {
            List<Comment> newComments = ListUtils.mapLast(s1.getComments(), c -> c.withSuffix(c.getSuffix() + s2.getWhitespace()));
            newComments.addAll(s2.getComments());
            return Space.build(s1.getWhitespace(), newComments);
        }
    }

    private J.Modifier buildFinalModifier() {
        return new J.Modifier(
                randomId(),
                Space.EMPTY,
                Markers.EMPTY,
                null,
                J.Modifier.Type.Final,
                emptyList()
        );
    }

    private List<PsiElement> getAllChildren(PsiElement parent) {
        List<PsiElement> children = new ArrayList<>();
        Iterator<PsiElement> iterator = PsiUtilsKt.getAllChildren(parent).iterator();
        while (iterator.hasNext()) {
            PsiElement it = iterator.next();
            children.add(it);
        }
        return children;
    }

    @Nullable
    private PsiElement findLeafElement(@Nullable PsiElement parent, String text) {
        if (parent == null) {
            return null;
        }

        List<PsiElement> children = getAllChildren(parent);
        for (PsiElement child : children) {
            if (child instanceof LeafPsiElement && child.getText().equals(text)) {
                return child;
            }
        }

        return null;
    }

}
