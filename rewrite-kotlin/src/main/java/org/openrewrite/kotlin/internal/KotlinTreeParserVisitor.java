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

import kotlin.Pair;
import org.jetbrains.kotlin.KtNodeTypes;
import org.jetbrains.kotlin.com.intellij.lang.ASTNode;
import org.jetbrains.kotlin.com.intellij.openapi.util.TextRange;
import org.jetbrains.kotlin.com.intellij.psi.*;
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement;
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.PsiErrorElementImpl;
import org.jetbrains.kotlin.com.intellij.psi.tree.IElementType;
import org.jetbrains.kotlin.com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.kotlin.fir.FirElement;
import org.jetbrains.kotlin.fir.declarations.FirResolvedImport;
import org.jetbrains.kotlin.fir.expressions.FirCallableReferenceAccess;
import org.jetbrains.kotlin.fir.references.FirResolvedCallableReference;
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol;
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol;
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol;
import org.jetbrains.kotlin.kdoc.psi.api.KDoc;
import org.jetbrains.kotlin.kdoc.psi.impl.KDocSection;
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.parsing.ParseUtilsKt;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.psi.psiUtil.PsiUtilsKt;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.FileAttributes;
import org.openrewrite.ParseExceptionResult;
import org.openrewrite.Tree;
import org.openrewrite.internal.EncodingDetectingInputStream;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.marker.ImplicitReturn;
import org.openrewrite.java.marker.OmitParentheses;
import org.openrewrite.java.marker.Quoted;
import org.openrewrite.java.marker.TrailingComma;
import org.openrewrite.java.tree.*;
import org.openrewrite.kotlin.KotlinParser;
import org.openrewrite.kotlin.marker.*;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.marker.Markers;
import org.openrewrite.style.NamedStyles;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Predicate;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static org.openrewrite.Tree.randomId;

/**
 * PSI based parser
 */
@SuppressWarnings("ConstantValue")
public class KotlinTreeParserVisitor extends KtVisitor<J, ExecutionContext> {
    private final KotlinSource kotlinSource;
    private final PsiElementAssociations psiElementAssociations;
    private final List<NamedStyles> styles;
    private final Path sourcePath;

    @Nullable
    private final FileAttributes fileAttributes;

    private final Charset charset;
    private final Boolean charsetBomMarked;
    private final Stack<KtElement> ownerStack = new Stack<>();
    private final ExecutionContext executionContext;
    private final List<Integer> cRLFLocations;

    public KotlinTreeParserVisitor(KotlinSource kotlinSource,
                                   PsiElementAssociations psiElementAssociations,
                                   List<NamedStyles> styles,
                                   @Nullable Path relativeTo,
                                   ExecutionContext ctx) {
        this.kotlinSource = kotlinSource;
        this.psiElementAssociations = psiElementAssociations;
        this.styles = styles;
        sourcePath = kotlinSource.getInput().getRelativePath(relativeTo);
        fileAttributes = kotlinSource.getInput().getFileAttributes();
        EncodingDetectingInputStream stream = kotlinSource.getInput().getSource(ctx);
        charset = stream.getCharset();
        charsetBomMarked = stream.isCharsetBomMarked();
        ownerStack.push(kotlinSource.getKtFile());
        executionContext = ctx;
        cRLFLocations = kotlinSource.getCRLFLocations();
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
                deepPrefix(expression),
                Markers.EMPTY,
                padRight(expression.getExpression().accept(this, data), prefix(rPar))
        );
    }

    @Override
    public J visitForExpression(KtForExpression expression, ExecutionContext data) {
        return new J.ForEachLoop(
                randomId(),
                deepPrefix(expression),
                Markers.EMPTY,
                new J.ForEachLoop.Control(
                        randomId(),
                        prefix(expression.getLeftParenthesis()),
                        Markers.EMPTY,
                        padRight((J.VariableDeclarations) requireNonNull(expression.getLoopParameter()).accept(this, data), suffix(expression.getLoopParameter())),
                        padRight(requireNonNull(expression.getLoopRange()).accept(this, data)
                                .withPrefix(prefix(expression.getLoopRange().getParent())), suffix(expression.getLoopRange().getParent()))
                ),
                padRight(requireNonNull(expression.getBody()).accept(this, data)
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
                convertToExpression(requireNonNull(expression.getBaseExpression()).accept(this, data))
        );
    }

    @Override
    public J visitAnnotationUseSiteTarget(KtAnnotationUseSiteTarget annotationTarget, ExecutionContext data) {
        return createIdentifier(annotationTarget, type(annotationTarget));
    }

    @Override
    public J visitAnonymousInitializer(KtAnonymousInitializer initializer, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitArrayAccessExpression(KtArrayAccessExpression expression, ExecutionContext data) {
        Markers markers = Markers.EMPTY;
        Expression selectExpr = convertToExpression(requireNonNull(expression.getArrayExpression()).accept(this, data));
        JRightPadded<Expression> select = padRight(selectExpr, suffix(expression.getArrayExpression()));
        JavaType.Method type = methodInvocationType(expression);
        J.Identifier name = createIdentifier("<get>", Space.EMPTY, type);

        markers = markers.addIfAbsent(new IndexedAccess(randomId()));
        List<KtExpression> indexExpressions = expression.getIndexExpressions();
        List<JRightPadded<Expression>> expressions = new ArrayList<>();

        for (int i = 0; i < indexExpressions.size(); i++) {
            KtExpression indexExp = indexExpressions.get(i);
            JRightPadded<Expression> rp = padRight(convertToExpression(indexExp.accept(this, data)), suffix(indexExp));
            expressions.add(maybeTrailingComma(indexExp, rp, i == indexExpressions.size() - 1));
        }

        JContainer<Expression> args = JContainer.build(Space.EMPTY, expressions, markers);
        return mapType(new J.MethodInvocation(
                randomId(),
                prefix(expression),
                markers,
                select,
                null,
                name,
                args,
                type
        ));
    }

    @Override
    public J visitBackingField(KtBackingField accessor, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitBinaryWithTypeRHSExpression(KtBinaryExpressionWithTypeRHS expression, ExecutionContext data) {
        IElementType type = expression.getOperationReference().getReferencedNameElementType();

        if (type == KtTokens.AS_KEYWORD || type == KtTokens.AS_SAFE) {
            TypeTree clazz = (TypeTree) (requireNonNull(expression.getRight()).accept(this, data));
            Markers markers = Markers.EMPTY;
            if (type == KtTokens.AS_SAFE) {
                markers = markers.addIfAbsent(new IsNullSafe(randomId()));
            }

            return new J.TypeCast(
                    randomId(),
                    deepPrefix(expression),
                    markers,
                    new J.ControlParentheses<>(
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
        J tree = requireNonNull(entry.getExpression()).accept(this, data);
        boolean inBraces = true;

        return new K.StringTemplate.Expression(
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
                deepPrefix(expression),
                Markers.EMPTY,
                expression.getTargetLabel() != null ? createIdentifier(requireNonNull(expression.getTargetLabel().getIdentifier()), null) : null
        );
    }

    @Override
    public J visitCallableReferenceExpression(KtCallableReferenceExpression expression, ExecutionContext data) {
        FirElement firElement = psiElementAssociations.primary(expression.getCallableReference());
        if (!(firElement instanceof FirResolvedCallableReference || firElement instanceof FirCallableReferenceAccess)) {
            throw new UnsupportedOperationException(java.lang.String.format("Unsupported callable reference: fir class: %s, fir: %s, psi class: %s.",
                    firElement == null ? "null" : firElement.getClass().getName(),
                    PsiTreePrinter.print(psiElementAssociations.primary(expression)),
                    expression.getClass().getName()));
        }
        JavaType.Method methodReferenceType = null;
        JavaType.Variable fieldReferenceType = null;
        if (firElement instanceof FirResolvedCallableReference) {
            FirResolvedCallableReference reference = (FirResolvedCallableReference) psiElementAssociations.primary(expression.getCallableReference());
            if (reference != null && reference.getResolvedSymbol() instanceof FirNamedFunctionSymbol) {
                methodReferenceType = psiElementAssociations.getTypeMapping().methodDeclarationType(
                        ((FirNamedFunctionSymbol) reference.getResolvedSymbol()).getFir(),
                        expression.getReceiverExpression()
                );
            }
            if (reference != null && reference.getResolvedSymbol() instanceof FirConstructorSymbol) {
                methodReferenceType = psiElementAssociations.getTypeMapping().methodDeclarationType(
                        ((FirConstructorSymbol) reference.getResolvedSymbol()).getFir(),
                        expression.getReceiverExpression()
                );
            }
            if (reference != null && reference.getResolvedSymbol() instanceof FirPropertySymbol) {
                fieldReferenceType = psiElementAssociations.getTypeMapping().variableType(
                        ((FirPropertySymbol) reference.getResolvedSymbol()).getFir(),
                        expression.getReceiverExpression()
                );
            }
        }

        JRightPadded<Expression> receiver;
        if (expression.getReceiverExpression() == null) {
            receiver = padRight(new J.Empty(randomId(), Space.EMPTY, Markers.EMPTY),
                    prefix(expression.findColonColon()));
        } else {
            Expression receiverExp = convertToExpression(expression.getReceiverExpression().accept(this, data));
            if (expression.getHasQuestionMarks()) {
                PsiElement questionMark = PsiTreeUtil.findSiblingForward(expression.getFirstChild(), KtTokens.QUEST, null);

                receiverExp = new J.NullableType(randomId(),
                        receiverExp.getPrefix(),
                        Markers.EMPTY,
                        emptyList(),
                        padRight(receiverExp.withPrefix(Space.EMPTY), prefix(questionMark))
                );
            }
            receiver = padRight(receiverExp, prefix(expression.findColonColon()));
        }

        return new J.MemberReference(
                randomId(),
                deepPrefix(expression),
                Markers.EMPTY,
                receiver,
                null,
                padLeft(prefix(expression.getLastChild()), expression.getCallableReference().accept(this, data).withPrefix(Space.EMPTY)),
                type(expression),
                methodReferenceType,
                fieldReferenceType
        );
    }

    @Override
    public J visitCatchSection(KtCatchClause catchClause, ExecutionContext data) {
        J.VariableDeclarations paramDecl = (J.VariableDeclarations) requireNonNull(catchClause.getCatchParameter()).accept(this, data);
        J.Block body = (J.Block) requireNonNull(catchClause.getCatchBody()).accept(this, data);
        return new J.Try.Catch(
                randomId(),
                deepPrefix(catchClause),
                Markers.EMPTY,
                new J.ControlParentheses<>(
                        randomId(),
                        prefix(catchClause.getParameterList()),
                        Markers.EMPTY,
                        padRight(paramDecl, endFixAndSuffix(catchClause.getCatchParameter()))
                ),
                body
        );
    }

    @Override
    public J visitClassInitializer(KtClassInitializer initializer, ExecutionContext data) {
        J.Block staticInit = requireNonNull(initializer.getBody()).accept(this, data).withPrefix(deepPrefix(initializer));
        return staticInit.getPadding().withStatic(padRight(true, prefix(initializer.getBody())));
    }

    @Override
    public J visitClassLiteralExpression(KtClassLiteralExpression expression, ExecutionContext data) {
        return new J.MemberReference(
                randomId(),
                deepPrefix(expression),
                Markers.EMPTY,
                padRight(convertToExpression(requireNonNull(expression.getReceiverExpression()).accept(this, data)),
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
        // Should never happen, as both `visitClass()` and `visitObjectDeclaration()` are implemented
        throw new IllegalArgumentException("Unsupported declaration: " + classOrObject.getText());
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
                deepPrefix(expression),
                Markers.EMPTY,
                elements,
                type(expression)
        );
    }

    @Override
    public J visitConstructorCalleeExpression(KtConstructorCalleeExpression constructorCalleeExpression, ExecutionContext data) {
        J j = requireNonNull(constructorCalleeExpression.getTypeReference()).accept(this, data);
        return j.withPrefix(merge(j.getPrefix(), deepPrefix(constructorCalleeExpression)));
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
                deepPrefix(expression),
                Markers.EMPTY,
                expression.getTargetLabel() != null ? createIdentifier(requireNonNull(expression.getTargetLabel().getIdentifier()), null) : null
        );
    }

    @Override
    public J visitDeclaration(KtDeclaration dcl, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitDelegatedSuperTypeEntry(KtDelegatedSuperTypeEntry specifier, ExecutionContext data) {
        TypeTree element = (TypeTree) requireNonNull(specifier.getTypeReference()).accept(this, data);
        Expression expr = convertToExpression(requireNonNull(specifier.getDelegateExpression()).accept(this, data));
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
        JRightPadded<Statement> body;
        if (expression.getBody() != null) {
            body = JRightPadded.build(requireNonNull(expression.getBody()).accept(this, data)
                    .withPrefix(prefix(expression.getBody().getParent())));
        } else {
            J.Block emptyBlock = new J.Block(
                    randomId(),
                    Space.EMPTY,
                    Markers.EMPTY.add(new OmitBraces(randomId())),
                    new JRightPadded<>(false, Space.EMPTY, Markers.EMPTY),
                    emptyList(),
                    Space.EMPTY
            );
            body = padRight(emptyBlock, Space.EMPTY);
        }

        return new J.DoWhileLoop(
                randomId(),
                deepPrefix(expression),
                Markers.EMPTY,
                body,
                padLeft(prefix(expression.getWhileKeyword()), mapControlParentheses(requireNonNull(expression.getCondition()), data).withPrefix(prefix(expression.getLeftParenthesis())))
        );
    }

    private J.ControlParentheses<Expression> mapControlParentheses(KtExpression expression, ExecutionContext data) {
        return new J.ControlParentheses<>(
                randomId(),
                deepPrefix(expression.getParent()),
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
            mapModifiers(enumEntry.getModifierList(), annotations, emptyList(), data);
        }

        Set<PsiElement> consumedSpaces = preConsumedInfix(enumEntry);

        J.Identifier name = createIdentifier(requireNonNull(enumEntry.getNameIdentifier()), type(enumEntry), consumedSpaces);
        J.NewClass initializer = null;

        JavaType.Method mt = methodDeclarationType(enumEntry);
        if (enumEntry.getInitializerList() != null) {
            initializer = (J.NewClass) enumEntry.getInitializerList().accept(this, data);
            initializer = initializer.withMethodType(mt);
        }

        if (enumEntry.getBody() != null) {
            J.Block body = (J.Block) enumEntry.getBody().accept(this, data);

            if (initializer != null) {
                initializer = initializer.withBody(body);
            } else {
                Markers markers = Markers.EMPTY.addIfAbsent(new Implicit(randomId()));
                JContainer<Expression> args = JContainer.empty();
                args = args.withMarkers(Markers.build(singletonList(new OmitParentheses(randomId()))));
                initializer = new J.NewClass(
                        randomId(),
                        Space.EMPTY,
                        markers,
                        null,
                        Space.EMPTY,
                        null,
                        args,
                        body,
                        mt
                );
            }
        }

        return new J.EnumValue(
                randomId(),
                deepPrefix(enumEntry),
                Markers.EMPTY,
                annotations,
                name,
                initializer
        );
    }

    @Override
    public J visitEscapeStringTemplateEntry(KtEscapeStringTemplateEntry entry, ExecutionContext data) {
        return new J.Literal(
                randomId(),
                Space.EMPTY,
                Markers.EMPTY,
                entry.getText(),
                entry.getText(),
                null,
                JavaType.Primitive.String
        ).withPrefix(deepPrefix(entry));
    }

    @Override
    public J visitExpression(KtExpression expression, ExecutionContext data) {
        if (expression instanceof KtFunctionLiteral) {
            KtFunctionLiteral ktFunctionLiteral = (KtFunctionLiteral) expression;
            Markers markers = Markers.EMPTY;
            ktFunctionLiteral.getLBrace();

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

            J.Block body = (J.Block) requireNonNull(ktFunctionLiteral.getBodyExpression()).accept(this, data);
            body = body.withEnd(endFixAndSuffix(ktFunctionLiteral.getBodyExpression()));

            return new J.Lambda(
                    randomId(),
                    deepPrefix(expression),
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
        Set<PsiElement> consumedSpaces = new HashSet<>();

        if (type.getParameters().isEmpty()) {
            params = singletonList(JRightPadded
                    .<TypeTree>build(new J.Empty(randomId(), prefix(requireNonNull(requireNonNull(type.getParameterList()).getNode().findChildByType(KtTokens.RPAR)).getPsi()), Markers.EMPTY))
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
                            createIdentifier(ktParameter.getNameIdentifier(), type(ktParameter.getTypeReference())),
                            (TypeTree) requireNonNull(ktParameter.getTypeReference()).accept(this, data)
                    ).withPrefix(prefix(ktParameter));
                } else {
                    typeTree = (TypeTree) requireNonNull(ktParameter.getTypeReference()).accept(this, data);
                    if (typeTree instanceof J.Identifier) {
                        typeTree = mergePrefix(prefix(ktParameter), (J.Identifier) typeTree);
                    } else {
                        typeTree = typeTree.withPrefix(prefix(ktParameter));
                    }
                }
                params.add(maybeTrailingComma(ktParameter, padRight(typeTree, endFixAndSuffix(ktParameter)), i == parameters.size() - 1));
            }
        }

        JContainer<TypeTree> parameters = JContainer.build(prefix(type.getParameterList()), params, Markers.EMPTY);
        if (type.getFirstChild() == type.getParameterList()) {
            parameters = parameters.withBefore(prefix(type));
            consumedSpaces.add(findFirstPrefixSpace(type));
        }

        return new K.FunctionType(
                randomId(),
                prefix(type, consumedSpaces),
                Markers.EMPTY,
                emptyList(), // TODO
                emptyList(), // TODO
                type.getReceiver() != null ? padRight((NameTree) requireNonNull(type.getReceiverTypeReference()).accept(this, data), suffix(type.getReceiver())) : null,
                parameters,
                suffix(type.getParameterList()),
                padRight((TypedTree) requireNonNull(type.getReturnTypeReference()).accept(this, data), suffix(type))
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

        Markers markers = Markers.EMPTY.addIfAbsent(new Implicit(randomId()));

        KtSuperTypeCallEntry superTypeCallEntry = (KtSuperTypeCallEntry) entries.get(0);
        JContainer<Expression> args;

        if (!superTypeCallEntry.getValueArguments().isEmpty()) {
            args = mapValueArguments(superTypeCallEntry.getValueArgumentList(), data);
        } else {
            KtValueArgumentList ktArgList = superTypeCallEntry.getValueArgumentList();
            args = JContainer.build(
                    prefix(ktArgList),
                    singletonList(padRight(new J.Empty(randomId(), ktArgList != null ? prefix(ktArgList.getRightParenthesis()) : Space.EMPTY, Markers.EMPTY), Space.EMPTY)
                    ),
                    markers
            );
        }

        return new J.NewClass(
                randomId(),
                deepPrefix(list),
                markers,
                null,
                Space.EMPTY,
                null,
                args,
                null,
                null // TODO
        );
    }

    @Override
    public J visitIntersectionType(KtIntersectionType definitelyNotNullType, ExecutionContext data) {
        List<JRightPadded<TypeTree>> rps = new ArrayList<>(2);
        TypeTree left = (TypeTree) requireNonNull(definitelyNotNullType.getLeftTypeRef()).accept(this, data);
        TypeTree right = (TypeTree) requireNonNull(definitelyNotNullType.getRightTypeRef()).accept(this, data);
        rps.add(padRight(left, suffix(definitelyNotNullType.getLeftTypeRef())));
        rps.add(padRight(right, Space.EMPTY));
        JContainer<TypeTree> bounds = JContainer.build(
                Space.EMPTY,
                rps,
                Markers.EMPTY
        );

        return new J.IntersectionType(randomId(), prefix(definitelyNotNullType), Markers.EMPTY, bounds);
    }

    @Override
    public J visitIsExpression(KtIsExpression expression, ExecutionContext data) {
        Markers markers = Markers.EMPTY;

        Expression element = convertToExpression(expression.getLeftHandSide().accept(this, data));

        if (expression.getOperationReference().getReferencedNameElementType() == KtTokens.NOT_IS) {
            markers = markers.addIfAbsent(new NotIs(randomId()));
        }

        J clazz = requireNonNull(expression.getTypeReference()).accept(this, data);

        return new J.InstanceOf(
                randomId(),
                deepPrefix(expression),
                markers,
                padRight(element, prefix(expression.getOperationReference())),
                clazz,
                null,
                type(expression)
        );
    }

    @Override
    public J visitLabeledExpression(KtLabeledExpression expression, ExecutionContext data) {
        J j = requireNonNull(expression.getBaseExpression()).accept(this, data);
        return new J.Label(
                randomId(),
                deepPrefix(expression),
                Markers.EMPTY,
                padRight(createIdentifier(requireNonNull(expression.getNameIdentifier()), null),
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
        if (!(entry.getFirstChild() instanceof LeafPsiElement)) {
            throw new UnsupportedOperationException("Unsupported KtStringTemplateEntry child");
        }

        String value = maybeAdjustCRLF(entry);
        boolean quoted = entry.getPrevSibling().getNode().getElementType() == KtTokens.OPEN_QUOTE &&
                         entry.getNextSibling().getNode().getElementType() == KtTokens.CLOSING_QUOTE;

        String valueSource = quoted ? "\"" + value + "\"" : value;

        return new J.Literal(
                Tree.randomId(),
                Space.EMPTY,
                Markers.EMPTY,
                value,
                valueSource,
                null,
                JavaType.Primitive.String
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
        KtTypeElement innerType = nullableType.getInnerType();
        if (innerType == null) {
            throw new UnsupportedOperationException("This should never happen");
        }

        TypeTree typeTree = (TypeTree) requireNonNull(innerType).accept(this, data);
        Set<PsiElement> consumedSpaces = new HashSet<>();
        if (innerType.getNextSibling() != null &&
            isSpace(innerType.getNextSibling().getNode()) &&
            !(innerType instanceof KtNullableType)) {
            consumedSpaces.add(innerType.getNextSibling());
        }

        if (typeTree instanceof K.FunctionType && nullableType.getModifierList() != null) {
            List<J.Annotation> leadingAnnotations = new ArrayList<>();
            List<J.Modifier> modifiers = mapModifiers(nullableType.getModifierList(), leadingAnnotations, emptyList(), data);

            if (!leadingAnnotations.isEmpty()) {
                leadingAnnotations = ListUtils.mapFirst(leadingAnnotations, anno -> anno.withPrefix(merge(deepPrefix(nullableType.getModifierList()), anno.getPrefix())));
            } else if (!modifiers.isEmpty()) {
                modifiers = ListUtils.mapFirst(modifiers, mod -> mod.withPrefix(merge(deepPrefix(nullableType.getModifierList()), mod.getPrefix())));
            }

            typeTree = ((K.FunctionType) typeTree).withModifiers(modifiers).withLeadingAnnotations(leadingAnnotations);
        }

        // Handle parentheses or potential nested parentheses
        Stack<Pair<Integer, Integer>> parenPairs = new Stack<>();
        List<PsiElement> allChildren = getAllChildren(nullableType);

        TypeTree j = typeTree;
        int l = 0;
        int r = allChildren.size() - 1;
        while (l < r) {
            l = findFirstLPAR(allChildren, l);
            r = findLastRPAR(allChildren, r);
            if (l * r < 0) {
                throw new UnsupportedOperationException("Unpaired parentheses!");
            }
            if (l < 0 || r < 0) {
                break;
            }
            parenPairs.add(new Pair<>(l++, r--));
        }

        while (!parenPairs.empty()) {
            Pair<Integer, Integer> parenPair = parenPairs.pop();
            PsiElement lPAR = allChildren.get(parenPair.getFirst());
            PsiElement rPAR = allChildren.get(parenPair.getSecond());
            j = new J.ParenthesizedTypeTree(randomId(),
                    Space.EMPTY,
                    Markers.EMPTY,
                    emptyList(),
                    new J.Parentheses<>(randomId(), prefix(lPAR), Markers.EMPTY, padRight(j, prefix(rPAR, consumedSpaces)))
            );
        }

        return new J.NullableType(randomId(),
                merge(deepPrefix(nullableType), j.getPrefix()),
                Markers.EMPTY,
                emptyList(),
                padRight(j, prefix(findFirstChild(nullableType, c -> c.getNode().getElementType() == KtTokens.QUEST)))
        );
    }

    @Override
    public J visitParameter(KtParameter parameter, ExecutionContext data) {
        Markers markers = Markers.EMPTY;
        List<J.Annotation> leadingAnnotations = new ArrayList<>();
        List<J.Annotation> lastAnnotations = new ArrayList<>();
        List<J.Modifier> modifiers = new ArrayList<>();
        TypeTree typeExpression = null;
        List<JRightPadded<J.VariableDeclarations.NamedVariable>> vars = new ArrayList<>(1);
        Set<PsiElement> consumedSpaces = preConsumedInfix(parameter);

        // todo, simplify this logic
        int valOrVarOffset = parameter.getValOrVarKeyword() != null ? parameter.getValOrVarKeyword().getTextOffset() : -1;
        int modifierOffset = parameter.getModifierList() != null ? parameter.getModifierList().getTextOffset() : -1;

        if (valOrVarOffset < modifierOffset) {
            if (parameter.getValOrVarKeyword() != null) {
                modifiers.add(mapModifier(parameter.getValOrVarKeyword(), emptyList(), consumedSpaces));
            }

            if (parameter.getModifierList() != null) {
                modifiers.addAll(mapModifiers(parameter.getModifierList(), leadingAnnotations, lastAnnotations, data));
            }
        } else {
            if (parameter.getModifierList() != null) {
                modifiers.addAll(mapModifiers(parameter.getModifierList(), leadingAnnotations, lastAnnotations, data));
            }

            if (parameter.getValOrVarKeyword() != null) {
                modifiers.add(mapModifier(parameter.getValOrVarKeyword(), lastAnnotations, consumedSpaces));
            }
        }

        if (parameter.getDestructuringDeclaration() != null) {
            return mapDestructuringDeclaration(parameter.getDestructuringDeclaration(), data)
                    .withPrefix(prefix(parameter));
        }

        JavaType.Variable vt = variableType(parameter, owner(parameter));
        J.Identifier name = createIdentifier(requireNonNull(parameter.getNameIdentifier()), vt, consumedSpaces);

        if (parameter.getTypeReference() != null) {
            typeExpression = (TypeTree) parameter.getTypeReference().accept(this, data);
            // TODO: get type from IR of KtProperty.
        }

        JLeftPadded<Expression> initializer =
                parameter.getDefaultValue() != null ? padLeft(prefix(parameter.getEqualsToken()), convertToExpression(parameter.getDefaultValue().accept(this, data))) : null;

        J.VariableDeclarations.NamedVariable namedVariable = new J.VariableDeclarations.NamedVariable(
                randomId(),
                Space.EMPTY,
                Markers.EMPTY,
                name,
                emptyList(),
                initializer,
                vt
        );

        vars.add(padRight(namedVariable, prefix(parameter.getColon())));

        return new J.VariableDeclarations(
                randomId(),
                deepPrefix(parameter),
                markers,
                leadingAnnotations,
                modifiers,
                typeExpression,
                null,
                vars
        );
    }

    @Override
    public J visitParameterList(KtParameterList list, ExecutionContext data) {
        throw new UnsupportedOperationException("Unsupported, use mapParameters() instead");
    }

    @Override
    public J visitPrimaryConstructor(KtPrimaryConstructor constructor, ExecutionContext data) {
        if (constructor.getBodyExpression() != null) {
            throw new UnsupportedOperationException("TODO");
        }

        List<J.Annotation> leadingAnnotations = new ArrayList<>();
        List<J.Modifier> modifiers = new ArrayList<>();
        Set<PsiElement> consumedSpaces = preConsumedInfix(constructor);

        if (constructor.getModifierList() != null) {
            KtModifierList ktModifierList = constructor.getModifierList();
            modifiers.addAll(mapModifiers(ktModifierList, leadingAnnotations, emptyList(), data));
        }

        if (constructor.getConstructorKeyword() != null) {
            modifiers.add(mapModifier(constructor.getConstructorKeyword(), emptyList(), consumedSpaces));
        }

        JavaType.Method type = methodDeclarationType(constructor);
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
            params = JContainer.build(
                    prefix(constructor.getValueParameterList(), preConsumedInfix(constructor)),
                    mapParameters(constructor.getValueParameterList(), data),
                    Markers.EMPTY);
        } else {
            throw new UnsupportedOperationException("TODO");
        }

        return mapType(new J.MethodDeclaration(
                randomId(),
                deepPrefix(constructor),
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
                type
        ));
    }

    @Override
    public J visitPropertyAccessor(KtPropertyAccessor accessor, ExecutionContext data) {
        Markers markers = Markers.EMPTY;
        List<J.Annotation> leadingAnnotations = new ArrayList<>();
        List<J.Annotation> lastAnnotations = new ArrayList<>();
        List<J.Modifier> modifiers = mapModifiers(accessor.getModifierList(), leadingAnnotations, lastAnnotations, data);
        TypeTree returnTypeExpression = null;
        JContainer<Statement> params;
        J.Block body = null;
        Set<PsiElement> consumedSpaces = preConsumedInfix(accessor);

        JavaType.Method type = methodDeclarationType(accessor);
        J.Identifier name = createIdentifier(accessor.getNamePlaceholder().getText(),
                prefix(accessor.getNamePlaceholder(), consumedSpaces),
                type);

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
            params = JContainer.empty();
            params = params.withBefore(Space.EMPTY)
                    .withMarkers(Markers.EMPTY.addIfAbsent(new OmitParentheses(randomId())));
        }

        return mapType(new J.MethodDeclaration(
                randomId(),
                deepPrefix(accessor),
                markers,
                leadingAnnotations,
                modifiers,
                null,
                returnTypeExpression,
                new J.MethodDeclaration.IdentifierWithAnnotations(
                        name,
                        lastAnnotations
                ),
                params,
                null,
                body,
                null,
                type
        ));
    }

    @Override
    public J visitQualifiedExpression(KtQualifiedExpression expression, ExecutionContext data) {
        Expression receiver = convertToExpression(expression.getReceiverExpression().accept(this, data));
        Expression selector = convertToExpression(requireNonNull(expression.getSelectorExpression()).accept(this, data));
        if (selector instanceof J.MethodInvocation) {
            J.MethodInvocation methodInvocation = (J.MethodInvocation) selector;
            return methodInvocation.getPadding()
                    .withSelect(padRight(receiver, suffix(expression.getReceiverExpression())))
                    .withName(methodInvocation.getName().withPrefix(prefix(expression.getSelectorExpression())))
                    .withPrefix(endFixPrefixAndInfix(expression));
        } else {
            J.Identifier identifier = (J.Identifier) selector;
            return mapType(new J.FieldAccess(
                    randomId(),
                    deepPrefix(expression),
                    Markers.EMPTY,
                    receiver,
                    padLeft(suffix(expression.getReceiverExpression()), identifier),
                    type(expression)
            ));
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
        return new K.Return(
                randomId(),
                new J.Return(
                        randomId(),
                        deepPrefix(expression),
                        Markers.EMPTY,
                        returnExpr
                ),
                expression.getTargetLabel() != null ? createIdentifier(requireNonNull(expression.getTargetLabel().getIdentifier()), null) : null
        );
    }

    @Override
    public J visitSafeQualifiedExpression(KtSafeQualifiedExpression expression, ExecutionContext data) {
        J j = visitQualifiedExpression(expression, data);
        return j.withMarkers(j.getMarkers().addIfAbsent(new IsNullSafe(randomId())));
    }

    @Override
    public J visitScript(KtScript script, ExecutionContext data) {
        return script.getBlockExpression().accept(this, data);
    }

    @Override
    public J visitScriptInitializer(KtScriptInitializer initializer, ExecutionContext data) {
        J j = requireNonNull(initializer.getBody()).accept(this, data);
        return j.withPrefix(merge(deepPrefix(initializer), j.getPrefix()));
    }

    @Override
    public J visitSecondaryConstructor(KtSecondaryConstructor constructor, ExecutionContext data) {
        Markers markers = Markers.EMPTY;
        List<J.Annotation> leadingAnnotations = new ArrayList<>();
        List<J.Annotation> lastAnnotations = new ArrayList<>();
        List<J.Modifier> modifiers = mapModifiers(constructor.getModifierList(), leadingAnnotations, lastAnnotations, data);
        modifiers.add(mapModifier(constructor.getConstructorKeyword(), lastAnnotations, preConsumedInfix(constructor)));

        JavaType.Method type = methodDeclarationType(constructor);
        J.Identifier name = createIdentifier(requireNonNull(constructor.getName()), prefix(constructor.getConstructorKeyword()), type)
                .withMarkers(Markers.EMPTY.addIfAbsent(new Implicit(randomId())));
        List<JRightPadded<Statement>> statements = mapParameters(constructor.getValueParameterList(), data);
        JContainer<Statement> params = JContainer.build(
                prefix(constructor.getValueParameterList()),
                statements,
                Markers.EMPTY
        );

        K.ConstructorInvocation delegationCall = constructor.getDelegationCall().isImplicit() ? null : new K.ConstructorInvocation(
                randomId(),
                prefix(constructor.getDelegationCall()),
                Markers.EMPTY,
                createIdentifier(requireNonNull(constructor.getDelegationCall().getCalleeExpression()), type(constructor.getDelegationCall().getCalleeExpression())),
                mapValueArguments(constructor.getDelegationCall().getValueArgumentList(), data)
        );

        J.Block body = null;
        if (constructor.getBodyExpression() != null) {
            body = (J.Block) constructor.getBodyExpression().accept(this, data);
        }

        J.MethodDeclaration methodDeclaration = mapType(new J.MethodDeclaration(
                randomId(),
                deepPrefix(constructor),
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
                type
        ));

        return delegationCall != null ? new K.Constructor(randomId(), Markers.EMPTY, methodDeclaration, padLeft(prefix(constructor.getColon()), delegationCall)) :
                methodDeclaration;
    }

    @Override
    public J visitSelfType(KtSelfType type, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitSimpleNameStringTemplateEntry(KtSimpleNameStringTemplateEntry entry, ExecutionContext data) {
        return new K.StringTemplate.Expression(
                randomId(),
                Space.EMPTY,
                Markers.EMPTY,
                requireNonNull(entry.getExpression()).accept(this, data),
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
        J j = requireNonNull(specifier.getTypeReference()).accept(this, data);
        if (j instanceof J.Identifier) {
            J.Identifier ident = (J.Identifier) j;
            if (!ident.getAnnotations().isEmpty()) {
                j = ident.withAnnotations(ListUtils.mapFirst(ident.getAnnotations(), a -> a.withPrefix(prefix(specifier.getParent()))));
            } else {
                j = ident.withPrefix(prefix(specifier));
            }
            return j;
        }
        return j.withPrefix(merge(deepPrefix(specifier), j.getPrefix()));
    }

    @Override
    public J visitSuperTypeListEntry(KtSuperTypeListEntry specifier, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitThisExpression(KtThisExpression expression, ExecutionContext data) {
        return new K.This(
                randomId(),
                deepPrefix(expression),
                Markers.EMPTY,
                expression.getTargetLabel() != null ? createIdentifier(requireNonNull(expression.getTargetLabel().getIdentifier()), null) : null,
                type(expression)
        );
    }

    @Override
    public J visitThrowExpression(KtThrowExpression expression, ExecutionContext data) {
        return new J.Throw(
                randomId(),
                prefix(expression),
                Markers.EMPTY,
                convertToExpression(requireNonNull(expression.getThrownExpression()).accept(this, data))
        );
    }

    @Override
    public J visitTryExpression(KtTryExpression expression, ExecutionContext data) {
        List<KtCatchClause> ktCatchClauses = expression.getCatchClauses();
        J.Block block = (J.Block) expression.getTryBlock().accept(this, data);
        List<J.Try.Catch> catches = new ArrayList<>(ktCatchClauses.size());
        JLeftPadded<J.Block> finallyBlock = null;
        for (KtCatchClause catchClause : ktCatchClauses) {
            catches.add((J.Try.Catch) catchClause.accept(this, data));
        }

        if (expression.getFinallyBlock() != null) {
            finallyBlock = padLeft(prefix(expression.getFinallyBlock()), (J.Block) expression.getFinallyBlock().accept(this, data));
        }

        return new J.Try(
                randomId(),
                deepPrefix(expression),
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
        List<J.Annotation> leadingAnnotations = new ArrayList<>();
        List<J.Annotation> lastAnnotations = new ArrayList<>();
        Set<PsiElement> consumedSpaces = preConsumedInfix(typeAlias);

        List<J.Modifier> modifiers = mapModifiers(typeAlias.getModifierList(), leadingAnnotations, lastAnnotations, data);
        modifiers.add(new J.Modifier(randomId(), prefix(typeAlias.getTypeAliasKeyword(), consumedSpaces), markers, "typealias", J.Modifier.Type.LanguageExtension, emptyList()));

        if (typeAlias.getIdentifyingElement() == null) {
            throw new UnsupportedOperationException("TODO");
        }

        J.Identifier name = createIdentifier(typeAlias.getIdentifyingElement(), type(typeAlias.getTypeReference()));

        JContainer<J.TypeParameter> typeParams = null;
        if (typeAlias.getTypeParameterList() != null) {
            typeParams = JContainer.build(prefix(typeAlias.getTypeParameterList()), mapTypeParameters(typeAlias.getTypeParameterList(), data), Markers.EMPTY);
        }

        Expression expr = convertToExpression(typeAlias.getTypeReference().accept(this, data));

        ASTNode node = typeAlias.getNode().findChildByType(KtTokens.EQ);
        Space prefix = node != null ? prefix(node.getPsi()) : Space.EMPTY;

        return new K.TypeAlias(
                randomId(),
                deepPrefix(typeAlias),
                markers,
                leadingAnnotations,
                modifiers,
                name,
                typeParams,
                padLeft(prefix, expr),
                type(typeAlias.getTypeReference())
        );
    }

    @Override
    public J visitTypeArgumentList(KtTypeArgumentList typeArgumentList, ExecutionContext data) {
        throw new UnsupportedOperationException("use mapTypeArguments instead");
    }

    @Override
    public J visitTypeConstraint(KtTypeConstraint constraint, ExecutionContext data) {
        List<J.Annotation> annotations = new ArrayList<>();
        J.Identifier typeParamName = (J.Identifier) requireNonNull(constraint.getSubjectTypeParameterName()).accept(this, data);
        PsiElement ref = PsiTreeUtil.getChildOfType(constraint, KtTypeReference.class);
        typeParamName = typeParamName.withType(psiElementAssociations.type(ref, owner(constraint)));
        TypeTree typeTree = (TypeTree) requireNonNull(constraint.getBoundTypeReference()).accept(this, data);

        return new J.TypeParameter(
                randomId(),
                deepPrefix(constraint),
                Markers.EMPTY.addIfAbsent(new TypeReferencePrefix(randomId(), suffix(constraint.getSubjectTypeParameterName()))),
                annotations,
                emptyList(),
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
            params.add(padRight((J.TypeParameter) ktTypeConstraint.accept(this, data), suffix(ktTypeConstraint)));
        }

        return new K.TypeConstraints(
                randomId(),
                Markers.EMPTY,
                JContainer.build(deepPrefix(list), params, Markers.EMPTY)
        );
    }

    @Override
    public J visitTypeParameter(KtTypeParameter parameter, ExecutionContext data) {
        Markers markers = Markers.EMPTY;
        List<J.Annotation> annotations = new ArrayList<>();

        JContainer<TypeTree> bounds = null;
        if (parameter.getNameIdentifier() == null) {
            throw new UnsupportedOperationException("This should never happen");
        }

        if (parameter.getExtendsBound() != null) {
            bounds = JContainer.build(suffix(parameter.getNameIdentifier()),
                    singletonList(padRight((TypeTree) parameter.getExtendsBound().accept(this, data),
                            Space.EMPTY)),
                    Markers.EMPTY);
            markers = markers.addIfAbsent(new TypeReferencePrefix(randomId(), Space.EMPTY));
        }

        return new J.TypeParameter(
                randomId(),
                deepPrefix(parameter),
                markers,
                annotations,
                mapModifiers(parameter.getModifierList(), annotations, emptyList(), data),
                createIdentifier(parameter.getNameIdentifier(), type(parameter)),
                bounds
        );
    }

    @Override
    public J visitTypeParameterList(KtTypeParameterList list, ExecutionContext data) {
        List<KtTypeParameter> ktTypeParameters = list.getParameters();
        List<JRightPadded<Expression>> expressions = new ArrayList<>(ktTypeParameters.size());

        for (KtTypeParameter ktTypeParameter : ktTypeParameters) {
            J.Identifier name = createIdentifier(ktTypeParameter, type(ktTypeParameter));
            expressions.add(padRight(name, suffix(ktTypeParameter)));
        }

        JContainer<Expression> typeParameters = JContainer.build(
                deepPrefix(list),
                expressions,
                Markers.EMPTY
        );

        return new J.ParameterizedType(
                randomId(),
                Space.EMPTY,
                Markers.EMPTY,
                null,
                typeParameters,
                null
        );
    }

    @Override
    public J visitTypeProjection(KtTypeProjection typeProjection, ExecutionContext data) {
        Markers markers = Markers.EMPTY;
        JContainer<TypeTree> bounds = null;
        Expression name = null;
        List<J.Annotation> leadingAnnotations = new ArrayList<>();
        List<J.Modifier> modifiers = mapModifiers(typeProjection.getModifierList(), leadingAnnotations, emptyList(), data);

        switch (typeProjection.getProjectionKind()) {
            case IN:
            case OUT: {
                bounds = JContainer.build(
                        prefix(typeProjection.getProjectionToken()),
                        singletonList(padRight(requireNonNull(typeProjection.getTypeReference()).accept(this, data)
                                .withPrefix(prefix(typeProjection.getTypeReference())), Space.EMPTY)),
                        Markers.EMPTY
                );
                break;
            }
            case STAR: {
                return new J.Wildcard(randomId(), prefix(typeProjection), Markers.EMPTY, null, null);
            }
            default: {
                name = convertToExpression(requireNonNull(typeProjection.getTypeReference()).accept(this, data));
                Space prefix = deepPrefix(typeProjection);
                if (name instanceof J.Identifier) {
                    name = addPrefixInFront((J.Identifier) name, prefix);
                } else {
                    name = name.withPrefix(merge(prefix, name.getPrefix()));
                }
            }
        }

        if (name != null) {
            return name;
        }

        return new K.TypeParameterExpression(randomId(), new J.TypeParameter(
                randomId(),
                deepPrefix(typeProjection),
                markers,
                leadingAnnotations,
                modifiers,
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
        return new K.Binary(randomId(),
                deepPrefix(condition),
                Markers.EMPTY,
                left,
                padLeft(Space.EMPTY, operator),
                convertToExpression(requireNonNull(condition.getRangeExpression()).accept(this, data)),
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
        J clazz = requireNonNull(condition.getTypeReference()).accept(this, data);

        return new J.InstanceOf(
                randomId(),
                deepPrefix(condition),
                markers,
                expr,
                clazz,
                null,
                type(condition)
        );
    }

    @Override
    public J visitWhenConditionWithExpression(KtWhenConditionWithExpression condition, ExecutionContext data) {
        return requireNonNull(condition.getExpression()).accept(this, data)
                .withPrefix(deepPrefix(condition));
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
        J body = requireNonNull(ktWhenEntry.getExpression()).accept(this, data);

        return new K.WhenBranch(
                randomId(),
                deepPrefix(ktWhenEntry),
                Markers.EMPTY,
                expressionContainer,
                padRight(body, Space.EMPTY)
        );
    }

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
            statements.add(maybeTrailingSemicolon(whenBranch, whenEntry));
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
                deepPrefix(expression),
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
                deepPrefix(expression),
                Markers.EMPTY,
                mapControlParentheses(requireNonNull(expression.getCondition()), data).withPrefix(prefix(expression.getLeftParenthesis())),
                expression.getBody() == null ? JRightPadded.build(new J.Empty(randomId(), suffix(expression.getRightParenthesis()), Markers.EMPTY)) :
                        JRightPadded.build(requireNonNull(expression.getBody()).accept(this, data).withPrefix(prefix(expression.getBody().getParent())))
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

    @Override
    public J visitKtFile(KtFile file, ExecutionContext data) {
        List<J.Annotation> annotations = file.getFileAnnotationList() != null ? mapAnnotations(file.getAnnotationEntries(), data) : emptyList();
        Set<PsiElement> consumedSpaces = new HashSet<>();
        Space eof = endFixAndSuffix(file);

        String shebang = null;
        Space spaceAfterShebang = null;
        PsiElement maybeShebang = file.getFirstChild();
        if (maybeShebang instanceof PsiComment && maybeShebang.getNode().getElementType() == KtTokens.SHEBANG_COMMENT) {
            shebang = maybeShebang.getText();
            spaceAfterShebang = suffix(maybeShebang);
        }

        JRightPadded<J.Package> pkg = null;
        if (!file.getPackageFqName().isRoot()) {
            pkg = maybeTrailingSemicolon((J.Package) requireNonNull(file.getPackageDirective()).accept(this, data), file.getPackageDirective());
            spaceAfterShebang = null;
            consumedSpaces.add(findFirstPrefixSpace(file.getPackageDirective()));
        }

        List<JRightPadded<J.Import>> imports = new ArrayList<>(file.getImportDirectives().size());
        if (!file.getImportDirectives().isEmpty()) {
            List<KtImportDirective> importDirectives = file.getImportDirectives();
            for (int i = 0; i < importDirectives.size(); i++) {
                KtImportDirective importDirective = importDirectives.get(i);
                J.Import anImport = (J.Import) importDirective.accept(this, data);
                if (i == 0) {
                    anImport = anImport.withPrefix(merge(prefix(file.getImportList()), anImport.getPrefix()));

                    if (spaceAfterShebang != null) {
                        anImport = anImport.withPrefix(merge(spaceAfterShebang, anImport.getPrefix()));
                        spaceAfterShebang = null;
                    }
                }

                imports.add(maybeTrailingSemicolon(anImport, importDirective));
            }
        }

        List<JRightPadded<Statement>> statements = new ArrayList<>(file.getDeclarations().size());
        List<KtDeclaration> declarations = file.getDeclarations();
        for (KtDeclaration declaration : declarations) {
            Statement statement;
            try {
                statement = convertToStatement(declaration.accept(this, data));
                if (spaceAfterShebang != null) {
                    statement = statement.withPrefix(merge(spaceAfterShebang, statement.getPrefix()));
                    spaceAfterShebang = null;
                }
            } catch (Exception e) {
                statement = new J.Unknown(
                        randomId(),
                        deepPrefix(declaration),
                        Markers.EMPTY,
                        new J.Unknown.Source(
                                randomId(),
                                Space.EMPTY,
                                Markers.build(singletonList(ParseExceptionResult.build(KotlinParser.builder().build(), e)
                                        .withTreeType(declaration.getClass().getName()))),
                                file.getText().substring(PsiUtilsKt.getStartOffsetSkippingComments(declaration),
                                        declaration.getTextRange().getEndOffset())));
            }

            statements.add(maybeTrailingSemicolon(statement, declaration));
        }

        return new K.CompilationUnit(
                Tree.randomId(),
                shebang,
                prefixAndInfix(file, consumedSpaces),
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
                eof
        );
    }

    @Override
    public J visitAnnotation(KtAnnotation annotation, ExecutionContext data) {
        Expression target;

        if (annotation.getUseSiteTarget() != null) {
            target = (J.Identifier) annotation.getUseSiteTarget().accept(this, data);
        } else {
            target = new J.Empty(randomId(), Space.EMPTY, Markers.EMPTY);
        }

        List<KtAnnotationEntry> annotationEntries = annotation.getEntries();
        List<JRightPadded<J.Annotation>> rpAnnotations = new ArrayList<>(annotationEntries.size());
        J.Annotation anno = null;
        for (KtAnnotationEntry ktAnnotationEntry : annotationEntries) {
            anno = (J.Annotation) ktAnnotationEntry.accept(this, data);
            anno = anno.withMarkers(anno.getMarkers().addIfAbsent(new AnnotationConstructor(randomId())));
            rpAnnotations.add(padRight(anno, Space.EMPTY));
        }

        PsiElement maybeLBracket = findFirstChild(annotation, an -> an.getNode().getElementType() == KtTokens.LBRACKET);
        boolean isImplicitBracket = maybeLBracket == null;
        Space beforeLBracket = isImplicitBracket ? Space.EMPTY : prefix(maybeLBracket);

        if (!isImplicitBracket) {
            rpAnnotations = ListUtils.mapLast(rpAnnotations,
                    rp -> rp.withAfter(prefix(findFirstChild(annotation, an -> an.getNode().getElementType() == KtTokens.RBRACKET))));
        }

        NameTree annotationType;
        if (isImplicitBracket) {
            annotationType = new K.AnnotationType(randomId(), Space.EMPTY, Markers.EMPTY, padRight(target, suffix(annotation.getUseSiteTarget())), anno);
        } else {
            annotationType = new K.MultiAnnotationType(randomId(), Space.EMPTY, Markers.EMPTY, padRight(target, suffix(annotation.getUseSiteTarget())), JContainer.build(beforeLBracket, rpAnnotations, Markers.EMPTY));
        }

        return mapType(new J.Annotation(randomId(),
                deepPrefix(annotation),
                Markers.EMPTY,
                annotationType,
                null
        ));
    }

    @Override
    public J visitAnnotationEntry(KtAnnotationEntry annotationEntry, ExecutionContext data) {
        Markers markers = Markers.EMPTY;
        NameTree nameTree;
        JContainer<Expression> args = null;

        boolean isUseSite = annotationEntry.getUseSiteTarget() != null;
        if (isUseSite) {
            isUseSite = findFirstChild(annotationEntry, c -> c == annotationEntry.getUseSiteTarget()) != null;
        }

        if (isUseSite) {
            J.Annotation callee = new J.Annotation(
                    randomId(),
                    Space.EMPTY,
                    Markers.EMPTY.addIfAbsent(new AnnotationConstructor(randomId())),
                    (NameTree) requireNonNull(annotationEntry.getCalleeExpression()).accept(this, data),
                    annotationEntry.getValueArgumentList() != null ? mapValueArguments(annotationEntry.getValueArgumentList(), data) : null
            );

            nameTree = new K.AnnotationType(randomId(),
                    Space.EMPTY,
                    Markers.EMPTY,
                    padRight(convertToExpression(annotationEntry.getUseSiteTarget().accept(this, data)),
                            prefix(findFirstChild(annotationEntry, p -> p.getNode().getElementType() == KtTokens.COLON))),
                    callee);
        } else {
            nameTree = (NameTree) requireNonNull(annotationEntry.getCalleeExpression()).accept(this, data);
            if (annotationEntry.getValueArgumentList() != null) {
                args = mapValueArguments(annotationEntry.getValueArgumentList(), data);
            }
        }

        return mapType(new J.Annotation(
                randomId(),
                deepPrefix(annotationEntry),
                markers,
                nameTree,
                args
        ));
    }

    @Override
    public J visitArgument(KtValueArgument argument, ExecutionContext data) {
        if (argument.getArgumentExpression() == null) {
            throw new UnsupportedOperationException("TODO");
        } else if (argument.isNamed()) {
            J.Identifier name = createIdentifier(requireNonNull(argument.getArgumentName()), type(argument.getArgumentName()));
            Expression expr = convertToExpression(argument.getArgumentExpression().accept(this, data));
            if (argument.isSpread()) {
                expr = new K.SpreadArgument(
                        randomId(),
                        prefix(findFirstChild(argument, c -> c.getNode().getElementType() == KtTokens.MUL)),
                        Markers.EMPTY,
                        expr
                );
            }
            return mapType(new J.Assignment(
                    randomId(),
                    deepPrefix(argument),
                    Markers.EMPTY,
                    name,
                    padLeft(suffix(argument.getArgumentName()), expr),
                    type(argument.getArgumentExpression())
            ));
        } else if (argument.isSpread()) {
            Expression j = (Expression) argument.getArgumentExpression().accept(this, data);
            return new K.SpreadArgument(
                    randomId(),
                    deepPrefix(argument),
                    Markers.EMPTY,
                    j
            );
        }

        J j = argument.getArgumentExpression().accept(this, data).withPrefix(deepPrefix(argument));
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
        // FIXME: This requires detection of infix overrides and operator overloads.
        if (javaBinaryType != null) {
            return mapType(new J.Binary(
                    randomId(),
                    deepPrefix(expression),
                    Markers.EMPTY,
                    left,
                    padLeft(prefix(operationReference), javaBinaryType),
                    right,
                    type
            ));
        } else if (operationReference.getOperationSignTokenType() == KtTokens.EQ) {
            return mapType(new J.Assignment(
                    randomId(),
                    deepPrefix(expression),
                    Markers.EMPTY,
                    left,
                    padLeft(suffix(expression.getLeft()), right),
                    type
            ));
        } else if (assignmentOperationType != null) {
            return mapType(new J.AssignmentOperation(
                    randomId(),
                    deepPrefix(expression),
                    Markers.EMPTY,
                    left,
                    padLeft(prefix(operationReference), assignmentOperationType),
                    right,
                    type
            ));
        } else if (kotlinBinaryType != null) {
            return mapType(new K.Binary(
                    randomId(),
                    deepPrefix(expression),
                    Markers.EMPTY,
                    left,
                    padLeft(prefix(operationReference), kotlinBinaryType),
                    right,
                    Space.EMPTY,
                    type
            ));
        }

        return mapFunctionCall(expression, data);
    }

    private J.AssignmentOperation.@Nullable Type mapAssignmentOperationType(KtOperationReferenceExpression operationReference) {
        IElementType elementType = operationReference.getOperationSignTokenType();

        if (elementType == KtTokens.PLUSEQ) {
            return J.AssignmentOperation.Type.Addition;
        } else if (elementType == KtTokens.MINUSEQ) {
            return J.AssignmentOperation.Type.Subtraction;
        } else if (elementType == KtTokens.MULTEQ) {
            return J.AssignmentOperation.Type.Multiplication;
        } else if (elementType == KtTokens.DIVEQ) {
            return J.AssignmentOperation.Type.Division;
        } else if (elementType == KtTokens.PERCEQ) {
            return J.AssignmentOperation.Type.Modulo;
        } else
            return null;
    }

    @Override
    public J visitBlockExpression(KtBlockExpression expression, ExecutionContext data) {
        List<JRightPadded<Statement>> statements = new ArrayList<>();
        for (KtExpression stmt : expression.getStatements()) {
            J exp = stmt.accept(this, data);
            Statement statement = convertToStatement(exp).withPrefix(endFixPrefixAndInfix(stmt));
            JRightPadded<Statement> build = maybeTrailingSemicolon(statement, stmt);
            statements.add(build);
        }

        boolean hasBraces = expression.getLBrace() != null;
        Space end = hasBraces ? deepPrefix(expression.getRBrace()) : Space.EMPTY;

        Space prefix = prefix(expression);
        Space blockPrefix = prefix;
        if (!hasBraces && !statements.isEmpty()) {
            statements = ListUtils.mapFirst(statements, s -> s.withElement(s.getElement().withPrefix(merge(prefix, s.getElement().getPrefix()))));
            blockPrefix = Space.EMPTY;
        }

        return new J.Block(
                randomId(),
                blockPrefix,
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
        PsiElementAssociations.ExpressionType type = psiElementAssociations.getCallType(expression);
        if (type == PsiElementAssociations.ExpressionType.CONSTRUCTOR) {
            JavaType.Method mt = methodInvocationType(expression);

            TypeTree name = (J.Identifier) expression.getCalleeExpression().accept(this, data);
            name = name.withType(mt != null ? mt.getReturnType() : JavaType.Unknown.getInstance());
            if (!expression.getTypeArguments().isEmpty()) {
                List<JRightPadded<Expression>> parameters = new ArrayList<>(expression.getTypeArguments().size());
                for (KtTypeProjection ktTypeProjection : expression.getTypeArguments()) {
                    parameters.add(padRight(convertToExpression(ktTypeProjection.accept(this, data)), suffix(ktTypeProjection)));
                }

                name = mapType(new J.ParameterizedType(
                        randomId(),
                        name.getPrefix(),
                        Markers.EMPTY,
                        name.withPrefix(Space.EMPTY),
                        JContainer.build(prefix(expression.getTypeArgumentList()), parameters, Markers.EMPTY),
                        type(expression)
                ));
            }

            return isAnnotationConstructor(mt) ?
                    mapType(new J.Annotation(
                            randomId(),
                            deepPrefix(expression),
                            Markers.EMPTY.addIfAbsent(new AnnotationConstructor(randomId())),
                            name,
                            mapValueArgumentsMaybeWithTrailingLambda(expression.getValueArgumentList(), expression.getValueArguments(), data)
                    )) :
                    mapType(new J.NewClass(
                            randomId(),
                            deepPrefix(expression),
                            Markers.EMPTY,
                            null,
                            Space.EMPTY,
                            name,
                            mapValueArgumentsMaybeWithTrailingLambda(expression.getValueArgumentList(), expression.getValueArguments(), data),
                            null,
                            mt
                    ));
        } else if (type == null || type == PsiElementAssociations.ExpressionType.METHOD_INVOCATION) {
            J j = expression.getCalleeExpression().accept(this, data);
            JRightPadded<Expression> select = null;
            J.Identifier name;
            if (j instanceof J.Identifier) {
                name = (J.Identifier) j;
            } else {
                select = padRight(convertToExpression(j), Space.EMPTY);
                name = createIdentifier("<empty>", Space.EMPTY, null, null)
                        .withMarkers(Markers.EMPTY.addIfAbsent(new Implicit(randomId())));
            }

            JContainer<Expression> typeParams = mapTypeArguments(expression.getTypeArgumentList(), data);
            JContainer<Expression> args = mapValueArgumentsMaybeWithTrailingLambda(expression.getValueArgumentList(), expression.getValueArguments(), data);

            if (expression.getValueArgumentList() == null) {
                args = args.withMarkers(args.getMarkers().addIfAbsent(new OmitParentheses(randomId())));
            }

            return mapType(new J.MethodInvocation(
                    randomId(),
                    deepPrefix(expression),
                    Markers.EMPTY,
                    select,
                    typeParams,
                    name,
                    args,
                    methodInvocationType(expression)
            ));
        } else if (type == PsiElementAssociations.ExpressionType.QUALIFIER) {
            TypeTree typeTree = (TypeTree) expression.getCalleeExpression().accept(this, data);
            JContainer<Expression> typeParams = mapTypeArguments(expression.getTypeArgumentList(), data);

            return mapType(new J.ParameterizedType(
                    randomId(),
                    deepPrefix(expression),
                    Markers.EMPTY,
                    typeTree,
                    typeParams,
                    type(expression)
            ));
        } else {
            throw new UnsupportedOperationException("ExpressionType not found: " + expression.getCalleeExpression().getText());
        }
    }

    private boolean isAnnotationConstructor(@Nullable JavaType type) {
        if (type instanceof JavaType.Method && !(((JavaType.Method) type).getDeclaringType() instanceof JavaType.Unknown)) {
            return isAnnotationConstructor(((JavaType.Method) type).getDeclaringType());
        }
        if (type instanceof JavaType.Parameterized) {
            return isAnnotationConstructor(((JavaType.Parameterized) type).getType());
        }
        if (type instanceof JavaType.Class) {
            return ((JavaType.Class) type).getKind() == JavaType.FullyQualified.Kind.Annotation;
        }
        return false;
    }

    @Nullable
    JContainer<Expression> mapTypeArguments(@Nullable KtTypeArgumentList ktTypeArgumentList, ExecutionContext data) {
        if (ktTypeArgumentList == null) {
            return null;
        }

        List<KtTypeProjection> ktTypeProjections = ktTypeArgumentList.getArguments();
        List<JRightPadded<Expression>> parameters = new ArrayList<>(ktTypeProjections.size());
        for (int i = 0; i < ktTypeProjections.size(); i++) {
            KtTypeProjection ktTypeProjection = ktTypeProjections.get(i);
            parameters.add(maybeTrailingComma(ktTypeProjection,
                    padRight(convertToExpression(ktTypeProjection.accept(this, data)), suffix(ktTypeProjection)), i == ktTypeProjections.size() - 1));
        }

        return JContainer.build(deepPrefix(ktTypeArgumentList), parameters, Markers.EMPTY);
    }

    @Override
    public J visitConstantExpression(KtConstantExpression expression, ExecutionContext data) {
        IElementType elementType = expression.getElementType();
        JavaType.Primitive type = primitiveType(expression);
        Object value;
        if (elementType == KtNodeTypes.INTEGER_CONSTANT || elementType == KtNodeTypes.FLOAT_CONSTANT) {
            value = ParseUtilsKt.parseNumericLiteral(expression.getText(), elementType);
            if (type == JavaType.Primitive.Int && value instanceof Long) {
                value = ((Long) value).intValue();
            }
        } else if (elementType == KtNodeTypes.BOOLEAN_CONSTANT) {
            value = ParseUtilsKt.parseBoolean(expression.getText());
        } else if (elementType == KtNodeTypes.CHARACTER_CONSTANT) {
            value = unescape(expression.getText().substring(1, expression.getText().length() - 1));
        } else if (elementType == KtNodeTypes.NULL) {
            value = null;
        } else {
            throw new UnsupportedOperationException("Unsupported constant expression elementType : " + elementType);
        }
        return new J.Literal(
                Tree.randomId(),
                deepPrefix(expression),
                Markers.EMPTY,
                value,
                expression.getText(),
                null,
                type
        );
    }

    private Object unescape(String str) {
        int length = str.length();
        if (length == 1) {
            return str.charAt(0);
        } else if (length == 2 && str.charAt(0) == '\\') {
            switch (str.charAt(1)) {
                case 't':
                    return '\t';
                case 'b':
                    return '\b';
                case 'r':
                    return '\r';
                case 'n':
                    return '\n';
                case '\'':
                    return '\'';
                default:
                    return str.charAt(1);
            }
        } else if (length == 6 && str.startsWith("\\u")) {
            return (char) Integer.parseInt(str.substring(2), 16);
        }
        return str;
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

    private J visitClass0(KtClass klass, ExecutionContext data) {
        List<J.Annotation> leadingAnnotations = new ArrayList<>();
        List<J.Annotation> lastAnnotations = new ArrayList<>();
        JContainer<J.TypeParameter> typeParams = null;
        JContainer<TypeTree> implementings = null;
        Markers markers = Markers.EMPTY;
        J.MethodDeclaration primaryConstructor;
        Set<PsiElement> prefixConsumedSet = preConsumedInfix(klass);

        List<J.Modifier> modifiers = mapModifiers(klass.getModifierList(), leadingAnnotations, lastAnnotations, data);

        if (!klass.hasModifier(KtTokens.OPEN_KEYWORD)) {
            modifiers.add(buildFinalModifier());
        }

        J.ClassDeclaration.Kind kind;
        if (klass.getClassKeyword() != null) {
            J.ClassDeclaration.Kind.Type classType = J.ClassDeclaration.Kind.Type.Class;

            for (J.Modifier mod : modifiers) {
                if ("annotation".equals(mod.getKeyword())) {
                    classType = J.ClassDeclaration.Kind.Type.Annotation;
                    break;
                } else if ("enum".equals(mod.getKeyword())) {
                    classType = J.ClassDeclaration.Kind.Type.Enum;
                    break;
                }
            }

            kind = new J.ClassDeclaration.Kind(
                    randomId(),
                    prefix(klass.getClassKeyword(), prefixConsumedSet),
                    Markers.EMPTY,
                    lastAnnotations,
                    classType
            );

        } else if (klass.getClassOrInterfaceKeyword() != null) {
            kind = new J.ClassDeclaration.Kind(
                    randomId(),
                    prefix(klass.getClassOrInterfaceKeyword(), prefixConsumedSet),
                    Markers.EMPTY,
                    emptyList(),
                    J.ClassDeclaration.Kind.Type.Interface
            );
        } else {
            throw new UnsupportedOperationException("TODO");
        }

        J.Identifier name = createIdentifier(requireNonNull(klass.getIdentifyingElement()), type(klass));

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

        if (klass.getSuperTypeList() != null) {
            implementings = mapSuperTypeList(klass.getSuperTypeList(), data);
            implementings = implementings != null ? implementings.withBefore(prefix(klass.getColon())) : null;
        }

        if (klass.getTypeParameterList() != null) {
            typeParams = JContainer.build(prefix(klass.getTypeParameterList()), mapTypeParameters(klass.getTypeParameterList(), data), Markers.EMPTY);
        }

        K.TypeConstraints typeConstraints = null;
        if (klass.getTypeConstraintList() != null) {
            typeConstraints = (K.TypeConstraints) klass.getTypeConstraintList().accept(this, data);
            PsiElement whereKeyword = requireNonNull(klass.getNode().findChildByType(KtTokens.WHERE_KEYWORD)).getPsi();
            typeConstraints = typeConstraints.withConstraints(ListUtils.mapFirst(typeConstraints.getConstraints(), constraint -> constraint.withPrefix(suffix(whereKeyword))))
                    .withPrefix(prefix(whereKeyword));
        }

        J.ClassDeclaration classDeclaration = new J.ClassDeclaration(
                randomId(),
                deepPrefix(klass),
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
                (JavaType.FullyQualified) type(klass)
        );

        return (typeConstraints != null) ? new K.ClassDeclaration(randomId(), Markers.EMPTY, classDeclaration, typeConstraints) : classDeclaration;
    }

    @Override
    public J visitClassBody(KtClassBody classBody, ExecutionContext data) {
        List<JRightPadded<Statement>> list = new ArrayList<>();

        Space after = endFixPrefixAndInfix(classBody.getRBrace());

        if (!classBody.getEnumEntries().isEmpty()) {
            List<JRightPadded<J.EnumValue>> enumValues = new ArrayList<>(classBody.getEnumEntries().size());
            boolean terminatedWithSemicolon = false;

            for (int i = 0; i < classBody.getEnumEntries().size(); i++) {
                KtEnumEntry ktEnumEntry = classBody.getEnumEntries().get(i);
                PsiElement comma = PsiTreeUtil.findSiblingForward(requireNonNull(ktEnumEntry.getIdentifyingElement()), KtTokens.COMMA, null);
                PsiElement semicolon = PsiTreeUtil.findSiblingForward(ktEnumEntry.getIdentifyingElement(), KtTokens.SEMICOLON, null);
                JRightPadded<J.EnumValue> rp = padRight((J.EnumValue) ktEnumEntry.accept(this, data), Space.EMPTY);

                if (i == classBody.getEnumEntries().size() - 1) {
                    if (semicolon != null) {
                        terminatedWithSemicolon = true;
                    }

                    if (comma != null) {
                        rp = rp.withAfter(prefix(comma));
                        // Space afterComma will be handled by others as a prefix or else, except there is a trailing semicolon
                        Space afterComma = Space.EMPTY;
                        if (semicolon != null) {
                            afterComma = suffix(comma);
                        }

                        rp = rp.withMarkers(rp.getMarkers().addIfAbsent(new TrailingComma(randomId(), afterComma)));
                    } else {
                        if (semicolon != null) {
                            rp = rp.withAfter(prefix(semicolon));
                        }
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
            Statement statement = convertToStatement(d.accept(this, data));
            list.add(maybeTrailingSemicolon(statement, d));
        }

        return new J.Block(
                randomId(),
                deepPrefix(classBody),
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
        List<JRightPadded<Statement>> destructVars = new ArrayList<>();

        JLeftPadded<Expression> paddedInitializer = null;

        J.Modifier modifier = new J.Modifier(
                Tree.randomId(),
                prefix(multiDeclaration.getValOrVarKeyword(), preConsumedInfix(multiDeclaration)),
                Markers.EMPTY,
                multiDeclaration.isVar() ? "var" : null,
                multiDeclaration.isVar() ? J.Modifier.Type.LanguageExtension : J.Modifier.Type.Final,
                emptyList()
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

            JavaType.Variable vt = variableType(entry, owner(entry));

            if (entry.getName() == null) {
                throw new UnsupportedOperationException("KtDestructuringDeclarationEntry has empty name, this should never happen");
            }

            J.Identifier nameVar = createIdentifier(requireNonNull(entry.getNameIdentifier()), vt);
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

            TypeTree typeExpression = null;
            if (entry.getTypeReference() != null) {
                typeExpression = (TypeTree) entry.getTypeReference().accept(this, data);
            }

            J.VariableDeclarations variableDeclarations = new J.VariableDeclarations(randomId(),
                    Space.EMPTY,
                    Markers.EMPTY,
                    emptyList(),
                    emptyList(),
                    typeExpression,
                    null,
                    singletonList(padRight(namedVariable, prefix(entry.getColon())))
            );

            destructVars.add(maybeTrailingComma(entry, padRight(variableDeclarations, suffix(entry)), i == entries.size() - 1));
        }

        JavaType.Variable vt = variableType(multiDeclaration, owner(multiDeclaration));
        J.VariableDeclarations.NamedVariable emptyWithInitializer = new J.VariableDeclarations.NamedVariable(
                randomId(),
                Space.EMPTY,
                Markers.EMPTY,
                createIdentifier("<destruct>", Space.SINGLE_SPACE, vt),
                emptyList(),
                paddedInitializer,
                vt
        );

        J.VariableDeclarations variableDeclarations = new J.VariableDeclarations(
                randomId(),
                Space.EMPTY,
                Markers.EMPTY,
                leadingAnnotations,
                modifiers,
                null,
                null,
                singletonList(padRight(emptyWithInitializer, Space.EMPTY))
        );

        return new K.DestructuringDeclaration(
                randomId(),
                deepPrefix(multiDeclaration),
                Markers.EMPTY,
                variableDeclarations,
                JContainer.build(prefix(multiDeclaration.getLPar()), destructVars, Markers.EMPTY)
        );
    }

    @Override
    public J visitDotQualifiedExpression(KtDotQualifiedExpression expression, ExecutionContext data) {
        assert expression.getSelectorExpression() != null;
        Space prefix = deepPrefix(expression);
        if (expression.getSelectorExpression() instanceof KtCallExpression) {
            KtCallExpression callExpression = (KtCallExpression) expression.getSelectorExpression();
            Space callExpressionPrefix = prefix(callExpression);
            Expression receiver = convertToExpression(expression.getReceiverExpression().accept(this, data));

            J j = callExpression.accept(this, data);
            if (j instanceof J.Annotation) {
                J.Annotation a = (J.Annotation) j;
                a = a.withAnnotationType(a.getAnnotationType().withPrefix(callExpressionPrefix));
                J.FieldAccess newName = mapType(new J.FieldAccess(
                        randomId(),
                        receiver.getPrefix(),
                        Markers.EMPTY,
                        receiver.withPrefix(Space.EMPTY),
                        padLeft(suffix(expression.getReceiverExpression()), (J.Identifier) a.getAnnotationType()),
                        a.getType()
                ));
                return a.withAnnotationType(newName).withPrefix(prefix);
            } else if (j instanceof J.ParameterizedType) {
                J.ParameterizedType pt = (J.ParameterizedType) j;
                pt = pt.withClazz(pt.getClazz().withPrefix(callExpressionPrefix));
                J.FieldAccess newName = mapType(new J.FieldAccess(
                        randomId(),
                        receiver.getPrefix(),
                        Markers.EMPTY,
                        receiver.withPrefix(Space.EMPTY),
                        padLeft(suffix(expression.getReceiverExpression()), (J.Identifier) pt.getClazz()),
                        pt.getType()
                ));
                return mapType(pt.withClazz(newName).withPrefix(prefix));
            } else if (j instanceof J.MethodInvocation) {
                J.MethodInvocation m = (J.MethodInvocation) j;
                return m.getPadding().withSelect(padRight(receiver, suffix(expression.getReceiverExpression())))
                        .withName(m.getName().withPrefix(callExpressionPrefix))
                        .withPrefix(prefix);
            } else if (j instanceof J.NewClass) {
                J.NewClass n = (J.NewClass) j;
                if (receiver instanceof J.FieldAccess || receiver instanceof J.Identifier || receiver instanceof J.NewClass || receiver instanceof K.This) {
                    n = n.withPrefix(prefix);
                    if (n.getClazz() instanceof J.ParameterizedType) {
                        J.ParameterizedType pt = (J.ParameterizedType) n.getClazz();
                        if (pt != null) {
                            pt = pt.withClazz(pt.getClazz().withPrefix(callExpressionPrefix));
                            J.FieldAccess newName = mapType(new J.FieldAccess(
                                    randomId(),
                                    receiver.getPrefix(),
                                    Markers.EMPTY,
                                    receiver.withPrefix(Space.EMPTY),
                                    padLeft(suffix(expression.getReceiverExpression()), (J.Identifier) pt.getClazz()),
                                    pt.getType()
                            ));
                            pt = pt.withClazz(newName);
                            pt = mapType(pt);
                            n = n.withClazz(pt);
                        }
                    } else {
                        J.Identifier id = (J.Identifier) n.getClazz();
                        if (id != null) {
                            id = id.withPrefix(callExpressionPrefix);
                            J.FieldAccess newName = mapType(new J.FieldAccess(
                                    randomId(),
                                    receiver.getPrefix(),
                                    Markers.EMPTY,
                                    receiver.withPrefix(Space.EMPTY),
                                    padLeft(suffix(expression.getReceiverExpression()), id),
                                    id.getType()
                            ));
                            n = n.withClazz(newName).withPrefix(prefix);
                        }
                    }
                }
                return n;
            }
            throw new UnsupportedOperationException("Unsupported call expression " + j.getClass().getName());
        } else if (expression.getSelectorExpression() instanceof KtNameReferenceExpression) {
            // Maybe need to type check before creating a field access.
            return mapType(new J.FieldAccess(
                    randomId(),
                    prefix,
                    Markers.EMPTY,
                    convertToExpression(expression.getReceiverExpression().accept(this, data).withPrefix(Space.EMPTY)),
                    padLeft(suffix(expression.getReceiverExpression()), (J.Identifier) expression.getSelectorExpression().accept(this, data)),
                    type(expression.getSelectorExpression())
            ));
        } else {
            throw new UnsupportedOperationException("Unsupported dot qualified selector: " + expression.getSelectorExpression().getClass());
        }
    }

    @Override
    public J visitIfExpression(KtIfExpression expression, ExecutionContext data) {
        return new J.If(
                randomId(),
                deepPrefix(expression),
                Markers.EMPTY,
                buildIfCondition(expression),
                buildIfThenPart(expression),
                buildIfElsePart(expression)
        );
    }

    @Override
    public J visitImportDirective(KtImportDirective importDirective, ExecutionContext data) {
        FirResolvedImport resolvedImport = getResolvedImport(importDirective);
        boolean isStaticImport = resolvedImport != null && resolvedImport.getResolvedParentClassId() != null;
        JLeftPadded<Boolean> rpStatic = padLeft(Space.EMPTY, isStaticImport);
        KtImportAlias alias = importDirective.getAlias();

        ASTNode node = importDirective.getNode().findChildByType(KtTokens.IMPORT_KEYWORD);
        LeafPsiElement importPsi = (LeafPsiElement) node;

        PsiElement first = PsiTreeUtil.skipWhitespacesAndCommentsForward(importPsi);
        PsiElement last = findLastChild(importDirective, psi -> !(psi instanceof KtImportAlias) &&
                                                                !isSpace(psi.getNode()) &&
                                                                psi.getNode().getElementType() != KtTokens.SEMICOLON);

        String text = nodeRangeText(getNodeOrNull(first), getNodeOrNull(last));
        TypeTree reference = TypeTree.build(text, '`');
        reference = reference.withPrefix(suffix(importPsi));

        JavaType jt = type(importDirective);
        if (jt instanceof JavaType.Parameterized) {
            jt = ((JavaType.Parameterized) jt).getType();
        }
        if (jt != null) {
            reference = reference.withType(jt);
        }
        if (reference instanceof J.Identifier) {
            reference = mapType(new J.FieldAccess(
                    randomId(),
                    suffix(importPsi),
                    Markers.EMPTY,
                    new J.Empty(randomId(), Space.EMPTY, Markers.EMPTY),
                    padLeft(Space.EMPTY, (J.Identifier) reference),
                    jt
            ));
        }

        return new J.Import(
                randomId(),
                deepPrefix(importDirective),
                Markers.EMPTY,
                rpStatic,
                (J.FieldAccess) reference,
                // Aliases contain Kotlin `Name` and do not resolve to a type. The aliases type is the import directive, so we set the type to match the import.
                alias != null ? padLeft(prefix(alias), createIdentifier(requireNonNull(alias.getNameIdentifier()), jt)) : null
        );
    }

    private @Nullable FirResolvedImport getResolvedImport(KtImportDirective importDirective) {
        FirElement primary = psiElementAssociations.primary(importDirective);
        if (primary instanceof FirResolvedImport) {
            return (FirResolvedImport) primary;
        }
        return null;
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

    private J visitNamedFunction0(KtNamedFunction function, ExecutionContext data) {
        Markers markers = Markers.EMPTY;
        List<J.Annotation> leadingAnnotations = new ArrayList<>();
        List<J.Annotation> lastAnnotations = new ArrayList<>();
        List<J.Modifier> modifiers = mapModifiers(function.getModifierList(), leadingAnnotations, lastAnnotations, data);
        J.TypeParameters typeParameters = null;
        TypeTree returnTypeExpression = null;

        Set<PsiElement> prefixConsumedSet = preConsumedInfix(function);

        if (function.getTypeParameterList() != null) {
            typeParameters = new J.TypeParameters(
                    randomId(),
                    prefix(function.getTypeParameterList()),
                    Markers.EMPTY,
                    emptyList(),
                    mapTypeParameters(function.getTypeParameterList(), data)
            );
        }

        boolean isOpen = function.hasModifier(KtTokens.OPEN_KEYWORD);
        if (!isOpen) {
            modifiers.add(buildFinalModifier().withPrefix(Space.EMPTY));
        }

        modifiers.add(new J.Modifier(randomId(), prefix(function.getFunKeyword(), prefixConsumedSet), Markers.EMPTY, "fun", J.Modifier.Type.LanguageExtension, lastAnnotations));
        J.Identifier name;

        JavaType.Method type = methodDeclarationType(function);
        if (function.getNameIdentifier() == null) {
            name = createIdentifier("", Space.EMPTY, type);
        } else {
            name = createIdentifier(function.getNameIdentifier(), type);
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
                                    prefix(function.getValueParameterList().getRightParenthesis()),
                                    Markers.EMPTY),
                            Space.EMPTY)
                    ), Markers.EMPTY
            );
        } else {
            params = JContainer.build(prefix(function.getValueParameterList()), mapParameters(function.getValueParameterList(), data), Markers.EMPTY);
        }

        if (function.getReceiverTypeReference() != null) {
            markers = markers.addIfAbsent(new Extension(randomId()));
            Expression receiver = convertToExpression(function.getReceiverTypeReference().accept(this, data));
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
            returnTypeExpression = (TypeTree) function.getTypeReference().accept(this, data);
        }

        K.TypeConstraints typeConstraints = null;
        if (function.getTypeConstraintList() != null) {
            typeConstraints = (K.TypeConstraints) function.getTypeConstraintList().accept(this, data);
            PsiElement whereKeyword = requireNonNull(function.getNode().findChildByType(KtTokens.WHERE_KEYWORD)).getPsi();
            typeConstraints = typeConstraints.withConstraints(ListUtils.mapFirst(typeConstraints.getConstraints(), constraint -> constraint.withPrefix(suffix(whereKeyword))))
                    .withPrefix(prefix(whereKeyword));
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

        J.MethodDeclaration methodDeclaration = mapType(new J.MethodDeclaration(
                randomId(),
                deepPrefix(function),
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
                type
        ));

        return (typeConstraints == null) ? methodDeclaration : new K.MethodDeclaration(randomId(), Markers.EMPTY, methodDeclaration, typeConstraints);
    }

    private List<JRightPadded<J.TypeParameter>> mapTypeParameters(KtTypeParameterList list, ExecutionContext data) {
        List<KtTypeParameter> ktTypeParameters = list.getParameters();
        List<JRightPadded<J.TypeParameter>> params = new ArrayList<>(ktTypeParameters.size());

        for (int i = 0; i < ktTypeParameters.size(); i++) {
            KtTypeParameter ktTypeParameter = ktTypeParameters.get(i);
            J.TypeParameter typeParameter = (J.TypeParameter) ktTypeParameter.accept(this, data);
            params.add(maybeTrailingComma(ktTypeParameter, padRight(typeParameter, suffix(ktTypeParameter)), i == ktTypeParameters.size() - 1));
        }

        return params;
    }

    @Override
    public J visitObjectLiteralExpression(KtObjectLiteralExpression expression, ExecutionContext data) {
        J j = expression.getObjectDeclaration().accept(this, data);
        return j.withPrefix(merge(deepPrefix(expression), j.getPrefix()));
    }

    @Override
    public J visitObjectDeclaration(KtObjectDeclaration declaration, ExecutionContext data) {
        Markers markers = Markers.EMPTY;
        List<J.Modifier> modifiers = new ArrayList<>();
        JContainer<TypeTree> implementings = null;
        Set<PsiElement> consumedSpaces = preConsumedInfix(declaration);

        List<J.Annotation> leadingAnnotations = new ArrayList<>();
        List<J.Annotation> lastAnnotations = new ArrayList<>();
        if (declaration.getModifierList() != null) {
            modifiers.addAll(mapModifiers(declaration.getModifierList(), leadingAnnotations, lastAnnotations, data));
        }

        modifiers.add(buildFinalModifier());

        JContainer<J.TypeParameter> typeParameters = declaration.getTypeParameterList() == null ? null :
                JContainer.build(prefix(declaration.getTypeParameterList()), mapTypeParameters(declaration.getTypeParameterList(), data), Markers.EMPTY);

        if (declaration.getSuperTypeList() != null) {
            implementings = mapSuperTypeList(declaration.getSuperTypeList(), data);
            implementings = requireNonNull(implementings).withBefore(prefix(declaration.getColon()));
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
            markers = markers.add(new KObject(randomId()));
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
                deepPrefix(declaration),
                markers,
                leadingAnnotations,
                modifiers,
                new J.ClassDeclaration.Kind(
                        randomId(),
                        prefix(declaration.getObjectKeyword(), consumedSpaces),
                        Markers.EMPTY,
                        lastAnnotations,
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
    public @Nullable J visitPackageDirective(KtPackageDirective directive, ExecutionContext data) {
        if (directive.getPackageNameExpression() == null) {
            return null;
        }
        return new J.Package(
                randomId(),
                deepPrefix(directive),
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
        // FIXME: Add detection of overloads and return the appropriate trees when it is not equivalent to a J.Unary.
        //        Returning the base type only applies when the expression is equivalent to a J.Binary.
        JavaType javaType = type(expression);
        return mapType(new J.Unary(
                randomId(),
                deepPrefix(expression),
                Markers.EMPTY,
                padLeft(prefix(expression.getOperationReference()), type),
                expression.getBaseExpression().accept(this, data).withPrefix(suffix(expression.getOperationReference())),
                javaType
        ));
    }

    @Override
    public J visitPostfixExpression(KtPostfixExpression expression, ExecutionContext data) {
        // FIXME: Add detection of overloads and return the appropriate trees when it is not equivalent to a J.Unary.
        //        Returning the base type only applies when the expression is equivalent to a J.Binary.
        JavaType type = type(expression);
        if (type instanceof JavaType.Method) {
            type = ((JavaType.Method) type).getReturnType();
        } else if (type instanceof JavaType.Variable) {
            type = ((JavaType.Variable) type).getType();
        }
        J j = convertToExpression(requireNonNull(expression.getBaseExpression()).accept(this, data));
        IElementType referencedNameElementType = expression.getOperationReference().getReferencedNameElementType();
        if (referencedNameElementType == KtTokens.EXCLEXCL) {
            j = mapType(new K.Unary(randomId(), deepPrefix(expression), Markers.EMPTY, padLeft(prefix(expression.getOperationReference()), K.Unary.Type.NotNull), (Expression) j, type));
        } else if (referencedNameElementType == KtTokens.PLUSPLUS) {
            j = mapType(new J.Unary(randomId(), deepPrefix(expression), Markers.EMPTY, padLeft(prefix(expression.getOperationReference()), J.Unary.Type.PostIncrement), (Expression) j, type));
        } else if (referencedNameElementType == KtTokens.MINUSMINUS) {
            j = mapType(new J.Unary(randomId(), deepPrefix(expression), Markers.EMPTY, padLeft(prefix(expression.getOperationReference()), J.Unary.Type.PostDecrement), (Expression) j, type));
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
        JRightPadded<Expression> receiver = null;
        JContainer<J.TypeParameter> typeParameters = property.getTypeParameterList() != null ?
                JContainer.build(prefix(property.getTypeParameterList()), mapTypeParameters(property.getTypeParameterList(), data), Markers.EMPTY) : null;
        K.TypeConstraints typeConstraints = null;
        Set<PsiElement> prefixConsumedSet = preConsumedInfix(property);

        modifiers.add(new J.Modifier(
                Tree.randomId(),
                prefix(property.getValOrVarKeyword(), prefixConsumedSet),
                Markers.EMPTY,
                property.isVar() ? "var" : null,
                property.isVar() ? J.Modifier.Type.LanguageExtension : J.Modifier.Type.Final,
                lastAnnotations
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
        JavaType.Variable vt = variableType(property, owner(property));
        J.VariableDeclarations.NamedVariable namedVariable =
                new J.VariableDeclarations.NamedVariable(
                        randomId(),
                        prefix(property.getNameIdentifier()),
                        Markers.EMPTY,
                        createIdentifier(requireNonNull(property.getNameIdentifier()), vt).withPrefix(Space.EMPTY),
                        emptyList(),
                        initializer,
                        vt
                );

        variables.add(padRight(namedVariable, prefix(property.getColon())).withMarkers(rpMarker));

        if (property.getColon() != null) {
            typeExpression = (TypeTree) requireNonNull(property.getTypeReference()).accept(this, data);
        }

        J.VariableDeclarations variableDeclarations = new J.VariableDeclarations(
                Tree.randomId(),
                endFixPrefixAndInfix(property), // overlaps with right-padding of previous statement
                markers,
                leadingAnnotations,
                modifiers,
                typeExpression,
                null,
                variables
        );

        if (property.getTypeConstraintList() != null) {
            typeConstraints = (K.TypeConstraints) property.getTypeConstraintList().accept(this, data);
            PsiElement whereKeyword = requireNonNull(property.getNode().findChildByType(KtTokens.WHERE_KEYWORD)).getPsi();
            typeConstraints = typeConstraints.withConstraints(ListUtils.mapFirst(typeConstraints.getConstraints(), constraint -> constraint.withPrefix(suffix(whereKeyword))))
                    .withPrefix(prefix(whereKeyword));
        }

        List<KtPropertyAccessor> ktPropertyAccessors = property.getAccessors();
        if (!ktPropertyAccessors.isEmpty() || receiver != null || typeConstraints != null) {
            List<JRightPadded<J.MethodDeclaration>> accessors = new ArrayList<>(ktPropertyAccessors.size());

            Space beforeSemiColon = Space.EMPTY;
            Markers rpMarkers = Markers.EMPTY;
            for (int i = 0; i < ktPropertyAccessors.size(); i++) {
                KtPropertyAccessor ktPropertyAccessor = ktPropertyAccessors.get(i);

                if (i == 0) {
                    PsiElement maybeSemiColon = PsiTreeUtil.findSiblingBackward(ktPropertyAccessor, KtTokens.SEMICOLON, null);
                    if (maybeSemiColon != null) {
                        beforeSemiColon = prefix(maybeSemiColon);
                        rpMarkers = rpMarkers.addIfAbsent(new Semicolon(randomId()));
                    }
                }

                J.MethodDeclaration accessor = (J.MethodDeclaration) ktPropertyAccessor.accept(this, data);
                accessors.add(maybeTrailingSemicolonInternal(accessor, ktPropertyAccessor));
            }

            return new K.Property(
                    randomId(),
                    deepPrefix(property),
                    markers,
                    typeParameters,
                    padRight(variableDeclarations.withPrefix(Space.EMPTY), beforeSemiColon, rpMarkers),
                    typeConstraints,
                    JContainer.build(accessors),
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
        boolean isLeadingAnnotation = true;
        ArrayList<J.Modifier> modifiers = new ArrayList<>();
        List<J.Annotation> annotations = new ArrayList<>();
        if (modifierList == null) {
            return modifiers;
        }

        // don't use iterator of `PsiTreeUtil.firstChild` and `getNextSibling`, since it could skip one layer, example test "paramAnnotation"
        // also don't use `modifierList.getChildren()` since it could miss some element
        for (Iterator<PsiElement> it = PsiUtilsKt.getAllChildren(modifierList).iterator(); it.hasNext(); ) {
            PsiElement child = it.next();
            if (isSpace(child.getNode())) {
                continue;
            }

            boolean isAnnotationEntry = child instanceof KtAnnotationEntry;
            boolean isAnnotation = child instanceof KtAnnotation;
            boolean isKeyword = child instanceof LeafPsiElement && child.getNode().getElementType() instanceof KtModifierKeywordToken;

            if (isAnnotationEntry) {
                KtAnnotationEntry ktAnnotationEntry = (KtAnnotationEntry) child;
                J.Annotation annotation = (J.Annotation) ktAnnotationEntry.accept(this, data);
                if (isLeadingAnnotation) {
                    leadingAnnotations.add(annotation);
                } else {
                    annotations.add(annotation);
                }
            } else if (isAnnotation) {
                KtAnnotation ktAnnotation = (KtAnnotation) child;
                J.Annotation annotation = (J.Annotation) ktAnnotation.accept(this, data);
                if (isLeadingAnnotation) {
                    leadingAnnotations.add(annotation);
                } else {
                    annotations.add(annotation);
                }
            } else if (isKeyword) {
                isLeadingAnnotation = false;
                modifiers.add(mapModifier(child, new ArrayList<>(annotations), null));
                annotations.clear();
            }
        }

        if (!annotations.isEmpty()) {
            lastAnnotations.addAll(annotations);
        }

        return modifiers;
    }

    @Override
    public J visitPropertyDelegate(KtPropertyDelegate delegate, ExecutionContext data) {
        if (delegate.getExpression() == null) {
            throw new UnsupportedOperationException("TODO");
        }
        // Markers initMarkers = Markers.EMPTY.addIfAbsent(new By(randomId()));
        return delegate.getExpression().accept(this, data)
                .withPrefix(deepPrefix(delegate));
    }

    @Override
    public J visitSimpleNameExpression(KtSimpleNameExpression expression, ExecutionContext data) {
        // The correct type cannot consistently be associated to the expression due to the relationship between the PSI and FIR.
        // The parent tree should use the associated FIR to fix mis-mapped types.
        // I.E. MethodInvocations, MethodDeclarations, VariableNames should be set manually through `createIdentifier(psi, type)`
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
            List<J> values = new ArrayList<>(entries.length);

            for (KtStringTemplateEntry entry : entries) {
                values.add(entry.accept(this, data));
            }

            return new K.StringTemplate(
                    randomId(),
                    deepPrefix(expression),
                    Markers.EMPTY,
                    delimiter,
                    values,
                    type(expression)
            );
        }

        StringBuilder valueSb = new StringBuilder();
        Arrays.stream(entries).forEach(entry -> valueSb.append(maybeAdjustCRLF(entry))
        );

        String valueSource = getString(expression, valueSb);

        return new J.Literal(
                randomId(),
                Space.EMPTY,
                Markers.EMPTY,
                valueSb.toString(),
                valueSource,
                null,
                JavaType.Primitive.String
        ).withPrefix(deepPrefix(expression));
    }

    private static String getString(KtStringTemplateExpression expression, StringBuilder valueSb) {
        PsiElement openQuote = expression.getFirstChild();
        PsiElement closingQuota = expression.getLastChild();
        if (openQuote == null || closingQuota == null ||
            openQuote.getNode().getElementType() != KtTokens.OPEN_QUOTE ||
            closingQuota.getNode().getElementType() != KtTokens.CLOSING_QUOTE) {
            throw new UnsupportedOperationException("This should never happen");
        }

        return openQuote.getText() + valueSb + closingQuota.getText();
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
        throw new UnsupportedOperationException("Unsupported, call mapSuperTypeList instead");
    }

    @Override
    public J visitSuperTypeCallEntry(KtSuperTypeCallEntry call, ExecutionContext data) {
        return requireNonNull(call.getTypeReference()).accept(this, data);
    }

    @Override
    public J visitTypeReference(KtTypeReference typeReference, ExecutionContext data) {
        List<J.Annotation> leadingAnnotations = new ArrayList<>();
        List<J.Annotation> lastAnnotations = new ArrayList<>();
        Set<PsiElement> consumedSpaces = preConsumedInfix(typeReference);

        List<J.Modifier> modifiers = mapModifiers(typeReference.getModifierList(), leadingAnnotations, lastAnnotations, data);
        if (!leadingAnnotations.isEmpty()) {
            leadingAnnotations = ListUtils.mapFirst(leadingAnnotations, anno -> anno.withPrefix(prefix(typeReference)));
            consumedSpaces.add(findFirstPrefixSpace(typeReference));
        } else if (!modifiers.isEmpty()) {
            PsiElement first = findFirstNonSpaceChild(typeReference);
            if (first != null) {
                if (first.getNode().getElementType() != KtTokens.LPAR) {
                    modifiers = ListUtils.mapFirst(modifiers, mod -> mod.withPrefix(prefix(typeReference)));
                    consumedSpaces.add(findFirstPrefixSpace(typeReference));
                } else {
                    // handle redundant parentheses
                    modifiers = ListUtils.mapFirst(modifiers, mod -> mod.withPrefix(merge(prefix(typeReference.getModifierList()), mod.getPrefix())));
                }
            }
        }

        J j = requireNonNull(typeReference.getTypeElement()).accept(this, data);
        consumedSpaces.add(findFirstPrefixSpace(typeReference.getTypeElement()));

        if (j instanceof K.FunctionType && typeReference.getModifierList() != null) {
            K.FunctionType functionType = (K.FunctionType) j;
            functionType = functionType.withModifiers(modifiers).withLeadingAnnotations(leadingAnnotations);

            if (functionType.getReceiver() != null) {
                functionType = functionType.withReceiver(
                        functionType.getReceiver().withElement(functionType.getReceiver().getElement().withPrefix(functionType.getPrefix()))
                );

                functionType = functionType.withPrefix(Space.EMPTY);
            }

            j = functionType;
        } else if (j instanceof J.Identifier) {
            j = ((J.Identifier) j).withAnnotations(leadingAnnotations);
        } else if (j instanceof J.ParameterizedType || j instanceof J.IntersectionType) {
            if (!leadingAnnotations.isEmpty()) {
                j = new J.AnnotatedType(randomId(), Space.EMPTY, Markers.EMPTY, leadingAnnotations, (TypeTree) j);
            }
        } else if (j instanceof J.NullableType) {
            j = ((J.NullableType) j).withAnnotations(leadingAnnotations);
        }

        // Handle potential redundant nested parentheses
        Stack<Pair<Integer, Integer>> parenPairs = new Stack<>();
        List<PsiElement> allChildren = getAllChildren(typeReference);

        int l = 0;
        int r = allChildren.size() - 1;
        while (l < r) {
            l = findFirstLPAR(allChildren, l);
            r = findLastRPAR(allChildren, r);
            if (l * r < 0) {
                throw new UnsupportedOperationException("Unpaired parentheses!");
            }
            if (l < 0 || r < 0) {
                break;
            }
            parenPairs.add(new Pair<>(l++, r--));
        }

        while (!parenPairs.empty()) {
            Pair<Integer, Integer> parenPair = parenPairs.pop();
            PsiElement lPAR = allChildren.get(parenPair.getFirst());
            PsiElement rPAR = allChildren.get(parenPair.getSecond());
            TypeTree typeTree = j instanceof K.FunctionType ? ((K.FunctionType) j).withReturnType(((K.FunctionType) j).getReturnType().withAfter(Space.EMPTY)) : (TypeTree) j;
            j = new J.ParenthesizedTypeTree(randomId(),
                    Space.EMPTY,
                    Markers.EMPTY,
                    emptyList(),
                    new J.Parentheses<>(randomId(), prefix(lPAR), Markers.EMPTY, padRight(typeTree, prefix(rPAR)))
            );
        }

        return j.withPrefix(merge(prefix(typeReference, consumedSpaces), j.getPrefix()));
    }

    @Override
    public J visitUserType(KtUserType type, ExecutionContext data) {
        J.Identifier name = (J.Identifier) requireNonNull(type.getReferenceExpression()).accept(this, data);

        if (type.getFirstChild() == type.getReferenceExpression()) {
            name = name.withPrefix(merge(deepPrefix(type), name.getPrefix()));
        }

        NameTree nameTree = name;

        if (type.getQualifier() != null) {
            Expression select = convertToExpression(type.getQualifier().accept(this, data)).withPrefix(prefix(type.getQualifier()));
            nameTree = mapType(new J.FieldAccess(randomId(), Space.EMPTY, Markers.EMPTY, select, padLeft(suffix(type.getQualifier()), name), name.getType()));
        }

        if (type.getTypeArgumentList() != null) {
            JContainer<Expression> args = mapTypeArguments(type.getTypeArgumentList(), data);

            JavaType javaType = type(type);
            if (javaType instanceof JavaType.Unknown) {
                javaType = new JavaType.Parameterized(null, JavaType.Unknown.getInstance(), null);
            } else if (!(javaType instanceof JavaType.Parameterized)) {
                throw new UnsupportedOperationException("java type is not a Parameterized: " + type.getText());
            }

            JavaType.Parameterized pt = (JavaType.Parameterized) javaType;
            return mapType(new J.ParameterizedType(
                    randomId(),
                    Space.EMPTY,
                    Markers.EMPTY,
                    nameTree,
                    args,
                    pt == null ? JavaType.Unknown.getInstance() : pt
            ));
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
    private K.Binary.@Nullable Type mapKBinaryType(KtOperationReferenceExpression operationReference) {
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

    private J.Binary.@Nullable Type mapJBinaryType(KtOperationReferenceExpression operationReference) {
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
            return J.Binary.Type.Equal;
        else if (elementType == KtTokens.EXCLEQ)
            return J.Binary.Type.NotEqual;
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

    private J.Unary.@Nullable Type mapJUnaryType(KtSimpleNameExpression operationReference) {
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

    private J.MethodInvocation mapFunctionCall(KtBinaryExpression expression, ExecutionContext data) {
        Markers markers = Markers.EMPTY
                .addIfAbsent(new Infix(randomId()))
                .addIfAbsent(new Extension(randomId()));

        Expression selectExp = convertToExpression(requireNonNull(expression.getLeft()).accept(this, data).withPrefix(prefix(expression.getLeft())));
        JRightPadded<Expression> select = padRight(selectExp, Space.EMPTY);
        J.Identifier name = (J.Identifier) expression.getOperationReference().accept(this, data); // createIdentifier(operation, Space.EMPTY, methodInvocationType(expression));

        List<JRightPadded<Expression>> expressions = new ArrayList<>(1);
        Markers paramMarkers = markers.addIfAbsent(new OmitParentheses(randomId()));
        Expression rightExp = convertToExpression(requireNonNull(expression.getRight()).accept(this, data).withPrefix(prefix(expression.getRight())));
        JRightPadded<Expression> padded = padRight(rightExp, suffix(expression.getRight()));
        expressions.add(padded);
        JContainer<Expression> args = JContainer.build(Space.EMPTY, expressions, paramMarkers);
        JavaType.Method methodType = methodInvocationType(expression);

        return mapType(new J.MethodInvocation(
                randomId(),
                prefix(expression),
                markers,
                select,
                null,
                name,
                args,
                methodType
        ));
    }

    private J.ControlParentheses<Expression> buildIfCondition(KtIfExpression expression) {
        return new J.ControlParentheses<>(randomId(),
                prefix(expression.getLeftParenthesis()),
                Markers.EMPTY,
                padRight(convertToExpression(requireNonNull(expression.getCondition()).accept(this, executionContext))
                                .withPrefix(suffix(expression.getLeftParenthesis())),
                        prefix(expression.getRightParenthesis()))
        );
    }

    private JRightPadded<Statement> buildIfThenPart(KtIfExpression expression) {
        // TODO: fix NPE.
        return padRight(convertToStatement(requireNonNull(expression.getThen()).accept(this, executionContext))
                        .withPrefix(prefix(expression.getThen().getParent())),
                Space.EMPTY);
    }

    private J.If.@Nullable Else buildIfElsePart(KtIfExpression expression) {
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

    /**
     * Aligns types between the PSI and FIR to the Java compiler for use in recipes and type utilities.
     * <p>
     * The types associated from the PSI to the FIR do not always align with the expected types on the
     * J tree. An example is binary operations; In Kotlin, binary operations are method invocations where
     * the source code appears to be a binary expression like `1 + 1`, but the FIR is a function call `1.plus(1)`.
     * <p>
     * The function call will be mapped to a `JavaType$Method`, which is only necessary if the function is not overloaded.
     * Otherwise, a J.Binary is created, which should have a type field of `JavaType.Class`. This method will assign
     * the appropriate JavaType based on the tree and the trees fields.
     * <p>
     * The expected types on fields of `J` trees varies. An example is the name of J.MethodInvocation should
     * have a type of `JavaType$Method` whereas the name of an Annotation should be a `JavaType.Class`.
     * <p>
     * Finally, we can identify and fix mis-mapped types by detecting unexpected types, marking the elements,
     * and finding the elements through data-tables.
     *
     * @param tree J tree to align types on.
     * @return Tree with updated types.
     */
    @SuppressWarnings("unchecked")
    private <J2 extends J> J2 mapType(J2 tree) {
        J2 updated = tree;
        if (tree instanceof J.Annotation) {
            updated = (J2) mapType((J.Annotation) tree);
        } else if (tree instanceof J.Assignment) {
            updated = (J2) mapType((J.Assignment) tree);
        } else if (tree instanceof J.AssignmentOperation) {
            updated = (J2) mapType((J.AssignmentOperation) tree);
        } else if (tree instanceof J.Binary) {
            updated = (J2) mapType((J.Binary) tree);
        } else if (tree instanceof J.FieldAccess) {
            updated = (J2) mapType((J.FieldAccess) tree);
        } else if (tree instanceof J.MethodDeclaration) {
            updated = (J2) mapType((J.MethodDeclaration) tree);
        } else if (tree instanceof J.MethodInvocation) {
            updated = (J2) mapType((J.MethodInvocation) tree);
        } else if (tree instanceof J.NewClass) {
            updated = (J2) mapType((J.NewClass) tree);
        } else if (tree instanceof J.ParameterizedType) {
            updated = (J2) mapType((J.ParameterizedType) tree);
        } else if (tree instanceof J.Unary) {
            updated = (J2) mapType((J.Unary) tree);
        } else if (tree instanceof K.Binary) {
            updated = (J2) mapType((K.Binary) tree);
        }
        return updated;
    }

    private J.Annotation mapType(J.Annotation tree) {
        J.Annotation a = tree;
        if (isNotFullyQualified(tree.getAnnotationType().getType())) {
            if (a.getAnnotationType().getType() instanceof JavaType.Method) {
                a = a.withAnnotationType(a.getAnnotationType().withType(((JavaType.Method) a.getAnnotationType().getType()).getReturnType()));
            }
        }
        return a;
    }

    private J.Assignment mapType(J.Assignment tree) {
        J.Assignment a = tree;
        if (isNotFullyQualified(tree.getType())) {
            if (a.getType() instanceof JavaType.Method) {
                a = a.withType(((JavaType.Method) a.getType()).getReturnType());
            } else if (a.getType() instanceof JavaType.Variable) {
                a = a.withType(((JavaType.Variable) a.getType()).getType());
            }
        }
        return a;
    }

    private J.AssignmentOperation mapType(J.AssignmentOperation tree) {
        J.AssignmentOperation a = tree;
        if (isNotFullyQualified(tree.getType())) {
            if (a.getType() instanceof JavaType.Method) {
                a = a.withType(((JavaType.Method) a.getType()).getReturnType());
            } else if (a.getType() instanceof JavaType.Variable) {
                a = a.withType(((JavaType.Variable) a.getType()).getType());
            }
        }
        return a;
    }

    private J.Binary mapType(J.Binary tree) {
        J.Binary b = tree;
        if (isNotFullyQualified(tree.getType())) {
            if (b.getType() instanceof JavaType.Method) {
                b = b.withType(((JavaType.Method) b.getType()).getReturnType());
            }
        }
        return b;
    }

    private J.FieldAccess mapType(J.FieldAccess tree) {
        J.FieldAccess f = tree;
        if (isNotFullyQualified(tree.getType())) {
            if (f.getType() instanceof JavaType.Method) {
                f = f.withType(((JavaType.Method) f.getType()).getReturnType());
            } else if (f.getType() instanceof JavaType.Variable) {
                f = f.withType(((JavaType.Variable) f.getType()).getType());
            }
            return f;
        }
        return f;
    }

    private J.MethodDeclaration mapType(J.MethodDeclaration tree) {
        return tree.withName(tree.getName().withType(tree.getMethodType()));
    }

    private J.MethodInvocation mapType(J.MethodInvocation tree) {
        return tree.withName(tree.getName().withType(tree.getMethodType()));
    }

    private J.NewClass mapType(J.NewClass tree) {
        J.NewClass n = tree;
        if (n.getClazz() instanceof J.Identifier) {
            if (n.getClazz().getType() instanceof JavaType.Parameterized) {
                J.Identifier clazz = (J.Identifier) n.getClazz();
                n = n.withClazz(clazz.withType(((JavaType.Parameterized) clazz.getType()).getType()));
            }
        }
        return n;
    }

    private J.ParameterizedType mapType(J.ParameterizedType tree) {
        J.ParameterizedType p = tree;
        if (p.getType() != null && !(p.getType() instanceof JavaType.Parameterized)) {
            if (p.getType() instanceof JavaType.Method) {
                if (((JavaType.Method) p.getType()).getReturnType() instanceof JavaType.Parameterized) {
                    p = p.withType(((JavaType.Method) p.getType()).getReturnType());
                }
            }
        }
        if (p.getClazz() != null && p.getClazz().getType() instanceof JavaType.Parameterized) {
            p = p.withClazz(p.getClazz().withType(((JavaType.Parameterized) p.getClazz().getType()).getType()));
        }
        return p;
    }

    private J.Unary mapType(J.Unary tree) {
        J.Unary u = tree;
        if (isNotFullyQualified(tree.getType())) {
            if (u.getType() instanceof JavaType.Method) {
                u = u.withType(((JavaType.Method) u.getType()).getReturnType());
            }
        }
        return u;
    }

    private K.Binary mapType(K.Binary tree) {
        K.Binary b = tree;
        if (isNotFullyQualified(b.getType())) {
            if (b.getType() instanceof JavaType.Method) {
                b = b.withType(((JavaType.Method) b.getType()).getReturnType());
            }
        }
        return b;
    }

    private boolean isNotFullyQualified(@Nullable JavaType type) {
        return type != null && !(type instanceof JavaType.FullyQualified);
    }

    private @Nullable JavaType type(@Nullable KtElement psi) {
        if (psi == null) {
            return JavaType.Unknown.getInstance();
        }
        return psiElementAssociations.type(psi, owner(psi));
    }

    private JavaType.Primitive primitiveType(PsiElement psi) {
        return psiElementAssociations.primitiveType(psi);
    }

    private JavaType.@Nullable Variable variableType(PsiElement psi, @Nullable FirElement parent) {
        return psiElementAssociations.variableType(psi, parent);
    }

    private JavaType.@Nullable Method methodDeclarationType(PsiElement psi) {
        return psiElementAssociations.methodDeclarationType(psi);
    }

    private JavaType.@Nullable Method methodInvocationType(PsiElement psi) {
        return psiElementAssociations.methodInvocationType(psi);
    }

    /*====================================================================
     * Other helper methods
     * ====================================================================*/
    private J.Identifier createIdentifier(PsiElement name, @Nullable JavaType type) {
        return createIdentifier(name, type, null);
    }

    private J.Identifier createIdentifier(PsiElement name, @Nullable JavaType type, @Nullable Set<PsiElement> consumedSpaces) {
        return createIdentifier(name.getNode().getText(), prefix(name, consumedSpaces), type);
    }

    private J.Identifier createIdentifier(String name, Space prefix,
                                          @Nullable JavaType type) {
        return createIdentifier(name, prefix,
                type instanceof JavaType.Variable ? ((JavaType.Variable) type).getType() : type,
                type instanceof JavaType.Variable ? (JavaType.Variable) type : null);
    }

    private J.Identifier createIdentifier(String name, Space prefix,
                                          @Nullable JavaType type,
                                          JavaType.@Nullable Variable fieldType) {
        Markers markers = Markers.EMPTY;
        String updated = name;
        if (name.startsWith("`")) {
            updated = updated.substring(1, updated.length() - 1);
            markers = markers.addIfAbsent(new Quoted(randomId()));
        }
        return new J.Identifier(
                randomId(),
                prefix,
                markers,
                emptyList(),
                updated,
                type instanceof JavaType.Unknown ? null : type,
                fieldType
        );
    }

    private static J.Identifier addPrefixInFront(J.Identifier id, Space prefix) {
        if (!id.getAnnotations().isEmpty()) {
            id = id.withAnnotations(ListUtils.mapFirst(id.getAnnotations(), anno -> anno.withPrefix(merge(prefix, anno.getPrefix()))));
        } else {
            id = id.withPrefix(merge(prefix, id.getPrefix()));
        }
        return id;
    }

    private @Nullable FirElement owner(PsiElement element) {
        KtElement owner = ownerStack.peek() == element ? ownerStack.get(ownerStack.size() - 2) : ownerStack.peek();
        if (owner instanceof KtDeclaration) {
            return psiElementAssociations.primary(owner);
        } else if (owner instanceof KtFile) {
            return psiElementAssociations.primary(owner);
        }
        return null;
    }

    private J.Block convertToBlock(KtExpression ktExpression, ExecutionContext data) {
        Expression returnExpr = convertToExpression(ktExpression.accept(this, data)).withPrefix(Space.EMPTY);
        K.Return return_ = new K.Return(randomId(), new J.Return(randomId(), prefix(ktExpression), Markers.EMPTY.addIfAbsent(new ImplicitReturn(randomId())), returnExpr), null);
        return new J.Block(
                randomId(),
                Space.EMPTY,
                Markers.EMPTY.addIfAbsent(new OmitBraces(randomId()))
                        .addIfAbsent(new SingleExpressionBlock(randomId())),
                JRightPadded.build(false),
                singletonList(JRightPadded.build(return_)),
                Space.EMPTY
        );
    }

    // Handle trailing semicolon inside the input `element` only.
    private <J2 extends J> JRightPadded<J2> maybeTrailingSemicolonInternal(J2 j, KtElement element) {
        PsiElement maybeSemicolon = findLastNotSpaceChild(element);
        if (isSemicolon(maybeSemicolon)) {
            return new JRightPadded<>(j, prefix(maybeSemicolon), Markers.EMPTY.add(new Semicolon(randomId())));
        }
        return padRight(j, Space.EMPTY);
    }

    // Handle trailing semicolon maybe inside the input `element` or following the input `element`
    private <J2 extends J> JRightPadded<J2> maybeTrailingSemicolon(J2 j, KtElement element) {
        // maybe trailing semicolon at the end of the `element`
        PsiElement maybeSemicolon = findLastNotSpaceChild(element);
        if (isSemicolon(maybeSemicolon)) {
            return new JRightPadded<>(j, prefix(maybeSemicolon), Markers.EMPTY.add(new Semicolon(randomId())));
        }

        // maybe following trailing semicolon of the `element`
        maybeSemicolon = PsiTreeUtil.skipWhitespacesAndCommentsForward(element);
        if (isSemicolon(maybeSemicolon)) {
            return new JRightPadded<>(j, deepPrefix(maybeSemicolon), Markers.EMPTY.add(new Semicolon(randomId())));
        }

        return padRight(j, Space.EMPTY);
    }

    private boolean isSemicolon(@Nullable PsiElement element) {
        if (element == null) {
            return false;
        }
        return element instanceof LeafPsiElement && ((LeafPsiElement) element).getElementType() == KtTokens.SEMICOLON;
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
        return prefix(element, null);
    }

    private Space prefix(@Nullable PsiElement element, @Nullable Set<PsiElement> consumedSpaces) {
        if (element == null) {
            return Space.EMPTY;
        }

        PsiElement first = findFirstPrefixSpace(element);
        if (first == null) {
            return Space.EMPTY;
        }

        if (consumedSpaces != null && consumedSpaces.contains(first)) {
            return Space.EMPTY;
        }

        return space(first);
    }

    private static List<PsiElement> getAllChildren(PsiElement psiElement) {
        List<PsiElement> allChildren = new ArrayList<>();

        Iterator<PsiElement> iterator = PsiUtilsKt.getAllChildren(psiElement).iterator();
        while (iterator.hasNext()) {
            PsiElement it = iterator.next();
            allChildren.add(it);
        }
        return allChildren;
    }

    private @Nullable PsiElement findFirstPrefixSpace(@Nullable PsiElement element) {
        if (element == null) {
            return null;
        }

        PsiElement pre = element.getPrevSibling();
        if (pre == null || !isSpace(pre.getNode())) {
            return null;
        }

        while (pre.getPrevSibling() != null && isSpace(pre.getPrevSibling().getNode())) {
            pre = pre.getPrevSibling();
        }

        return pre;
    }

    private static @Nullable PsiElement getFirstSpaceChildOrNull(@Nullable PsiElement element) {
        if (element == null) {
            return null;
        }

        Iterator<PsiElement> iterator = PsiUtilsKt.getAllChildren(element).iterator();
        PsiElement first = iterator.next();

        if (first != null) {
            return isSpace(first.getNode()) ? first : null;
        }

        return null;
    }

    private Space suffix(@Nullable PsiElement element) {
        if (element == null) {
            return Space.EMPTY;
        }

        PsiElement whitespace = element.getNextSibling();
        if (whitespace == null || !isSpace(whitespace.getNode())) {
            return Space.EMPTY;
        }
        return space(whitespace);
    }

    private Space infix(@Nullable PsiElement element, @Nullable Set<PsiElement> consumedSpaces) {
        if (element == null) {
            return Space.EMPTY;
        }

        PsiElement first = element.getFirstChild();
        if (first == null || !isSpace(first.getNode())) {
            return Space.EMPTY;
        }

        if (consumedSpaces != null && consumedSpaces.contains(first)) {
            return Space.EMPTY;
        }

        return space(element.getFirstChild());
    }

    private Space endFix(@Nullable PsiElement element) {
        if (element == null) {
            return Space.EMPTY;
        }

        Space end = endFix(findLastNotSpaceChild(element));
        PsiElement lastSpace = findLastSpaceChild(element);

        return merge(end, space(lastSpace));
    }

    private Space endFixPrefixAndInfix(@Nullable PsiElement element) {
        return merge(endFix(PsiTreeUtil.skipWhitespacesAndCommentsBackward(element)), prefixAndInfix(element, null));
    }

    private Space deepPrefix(@Nullable PsiElement element) {
        return endFixPrefixAndInfix(element);
    }

    private Space endFixAndSuffix(@Nullable PsiElement element) {
        return merge(endFix(element), suffix(element));
    }

    private Space prefixAndInfix(@Nullable PsiElement element, @Nullable Set<PsiElement> consumedSpaces) {
        if (element == null) {
            return Space.EMPTY;
        }

        return merge(prefix(element, consumedSpaces), infix(element, consumedSpaces));
    }

    private static boolean isSpace(ASTNode node) {
        IElementType elementType = node.getElementType();
        return elementType == KtTokens.WHITE_SPACE ||
               elementType == KtTokens.BLOCK_COMMENT ||
               elementType == KtTokens.EOL_COMMENT ||
               elementType == KtTokens.DOC_COMMENT ||
               isCRLF(node);
    }

    private static boolean isLPAR(PsiElement element) {
        return element instanceof LeafPsiElement && ((LeafPsiElement) element).getElementType() == KtTokens.LPAR;
    }

    private static boolean isRPAR(PsiElement element) {
        return element instanceof LeafPsiElement && ((LeafPsiElement) element).getElementType() == KtTokens.RPAR;
    }

    private static int findFirstLPAR(List<PsiElement> elements, int start) {
        int ret = -1;
        for (int i = start; i < elements.size(); i++) {
            if (isLPAR(elements.get(i))) {
                return i;
            }
        }
        return ret;
    }

    private static int findLastRPAR(List<PsiElement> elements, int end) {
        int ret = -1;
        for (int i = end; i >= 0; i--) {
            if (isRPAR(elements.get(i))) {
                return i;
            }
        }
        return ret;
    }

    private static boolean isCRLF(ASTNode node) {
        return node instanceof PsiErrorElementImpl && "\r".equals(node.getText());
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
                .collect(toList());
    }

    private J mapDestructuringDeclaration(KtDestructuringDeclaration ktDestructuringDeclaration, ExecutionContext data) {
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
                    variableType(ktDestructuringDeclarationEntry, owner(ktDestructuringDeclarationEntry))
            );
            variables.add(padRight(namedVariable, suffix(ktDestructuringDeclarationEntry)));
        }

        J j = new J.VariableDeclarations(
                randomId(),
                prefix(ktDestructuringDeclaration),
                Markers.EMPTY.addIfAbsent(new OmitEquals(randomId())),
                emptyList(),
                emptyList(),
                null,
                null,
                variables
        );

        if (entries.size() == 1) {
            // Handle potential redundant parentheses
            List<PsiElement> allChildren = getAllChildren(ktDestructuringDeclaration);
            int l = findFirstLPAR(allChildren, 0);
            int r = findLastRPAR(allChildren, allChildren.size() - 1);
            if (l >= 0 && l < r) {
                j = new J.Parentheses<>(randomId(), Space.EMPTY, Markers.EMPTY, padRight(j, Space.EMPTY));
            }
        }
        return j;
    }

    private J.Modifier mapModifier(PsiElement modifier, List<J.Annotation> annotations, @Nullable Set<PsiElement> consumedSpaces) {
        Space prefix = prefix(modifier, consumedSpaces);
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
            statements.add(maybeTrailingComma(ktParameter, padRight(statement, endFixAndSuffix(ktParameter)), i == ktParameters.size() - 1));
        }

        if (ktParameters.isEmpty()) {
            Statement param = new J.Empty(randomId(), prefix(list.getRightParenthesis()), Markers.EMPTY);
            statements.add(padRight(param, Space.EMPTY));
        }

        return statements;
    }

    private @Nullable JContainer<TypeTree> mapSuperTypeList(@Nullable KtSuperTypeList ktSuperTypeList, ExecutionContext data) {
        if (ktSuperTypeList == null) {
            return null;
        }

        List<KtSuperTypeListEntry> ktSuperTypeListEntries = ktSuperTypeList.getEntries();
        List<JRightPadded<TypeTree>> superTypes = new ArrayList<>(ktSuperTypeListEntries.size());
        Markers markers = Markers.EMPTY;

        for (int i = 0; i < ktSuperTypeListEntries.size(); i++) {
            KtSuperTypeListEntry superTypeListEntry = ktSuperTypeListEntries.get(i);

            if (superTypeListEntry instanceof KtSuperTypeCallEntry) {
                KtSuperTypeCallEntry superTypeCallEntry = (KtSuperTypeCallEntry) superTypeListEntry;
                TypeTree typeTree = (TypeTree) superTypeCallEntry.getCalleeExpression().accept(this, data);
                JContainer<Expression> args;

                if (!superTypeCallEntry.getValueArguments().isEmpty()) {
                    args = mapValueArguments(superTypeCallEntry.getValueArgumentList(), data);
                } else {
                    KtValueArgumentList ktArgList = superTypeCallEntry.getValueArgumentList();
                    args = JContainer.build(
                            prefix(ktArgList),
                            singletonList(padRight(new J.Empty(randomId(), prefix(requireNonNull(ktArgList).getRightParenthesis()), Markers.EMPTY), Space.EMPTY)),
                            markers
                    );
                }

                if (i == 0) {
                    if (typeTree instanceof J.Identifier && !((J.Identifier) typeTree).getAnnotations().isEmpty()) {
                        J.Identifier ident = (J.Identifier) typeTree;
                        typeTree = ident.withAnnotations(ListUtils.mapFirst(ident.getAnnotations(), a -> a.withPrefix(prefix(superTypeListEntry.getParent()))));
                    } else {
                        typeTree = typeTree.withPrefix(prefix(superTypeListEntry.getParent()));
                    }
                }

                K.ConstructorInvocation delegationCall = new K.ConstructorInvocation(
                        randomId(),
                        prefix(superTypeListEntry),
                        Markers.EMPTY,
                        typeTree,
                        args
                );
                superTypes.add(padRight(delegationCall, suffix(superTypeCallEntry)));
            } else if (superTypeListEntry instanceof KtSuperTypeEntry ||
                       superTypeListEntry instanceof KtDelegatedSuperTypeEntry) {
                TypeTree typeTree = (TypeTree) superTypeListEntry.accept(this, data);

                if (i == 0) {
                    if (typeTree instanceof J.Identifier && !((J.Identifier) typeTree).getAnnotations().isEmpty()) {
                        J.Identifier ident = (J.Identifier) typeTree;
                        typeTree = ident.withAnnotations(ListUtils.mapFirst(ident.getAnnotations(), a -> a.withPrefix(prefix(superTypeListEntry.getParent()))));
                    } else {
                        typeTree = typeTree.withPrefix(prefix(superTypeListEntry.getParent()));
                    }
                }

                superTypes.add(padRight(typeTree, suffix(superTypeListEntry)));
            } else {
                throw new UnsupportedOperationException("TODO");
            }
        }

        return JContainer.build(Space.EMPTY, superTypes, Markers.EMPTY);
    }

    /**
     * Compared to the other mapValueArguments method, this method supports trailing lambda
     */
    private JContainer<Expression> mapValueArgumentsMaybeWithTrailingLambda(@Nullable KtValueArgumentList
                                                                                    valueArgumentList,
                                                                            List<KtValueArgument> ktValueArguments,
                                                                            ExecutionContext data) {
        List<JRightPadded<Expression>> expressions = new ArrayList<>(ktValueArguments.size());
        Markers markers = Markers.EMPTY;

        if (valueArgumentList != null && valueArgumentList.getArguments().isEmpty()) {
            Expression arg = new J.Empty(randomId(), prefix(valueArgumentList.getRightParenthesis()), Markers.EMPTY);
            expressions.add(padRight(arg, Space.EMPTY));
        }

        for (int i = 0; i < ktValueArguments.size(); i++) {
            KtValueArgument ktValueArgument = ktValueArguments.get(i);
            Expression expr = convertToExpression(ktValueArgument.accept(this, data));

            Markers rpMarkers = Markers.EMPTY;
            if (valueArgumentList != null && i == valueArgumentList.getArguments().size() - 1) {
                PsiElement maybeTrailingComma = findTrailingComma(ktValueArgument);
                if (maybeTrailingComma != null) {
                    rpMarkers = rpMarkers.addIfAbsent(new TrailingComma(randomId(), suffix(maybeTrailingComma)));
                }
            }

            expressions.add(padRight(expr, suffix(ktValueArgument), rpMarkers));
        }

        if (valueArgumentList == null) {
            markers = markers.addIfAbsent(new OmitParentheses(randomId()));
        }

        Space prefix = valueArgumentList != null ? prefix(valueArgumentList) : Space.EMPTY;
        return JContainer.build(prefix, expressions, markers);
    }

    private JContainer<Expression> mapValueArguments(@Nullable KtValueArgumentList argumentList, ExecutionContext
            data) {
        if (argumentList == null) {
            return JContainer.empty();
        }

        List<KtValueArgument> ktValueArguments = argumentList.getArguments();
        List<JRightPadded<Expression>> expressions = new ArrayList<>(ktValueArguments.size());

        if (ktValueArguments.isEmpty()) {
            Expression arg = new J.Empty(randomId(), prefix(argumentList.getRightParenthesis()), Markers.EMPTY);
            expressions.add(padRight(arg, Space.EMPTY));
        } else {
            for (int i = 0; i < ktValueArguments.size(); i++) {
                KtValueArgument ktValueArgument = ktValueArguments.get(i);
                Expression expr = convertToExpression(ktValueArgument.accept(this, data));
                expressions.add(maybeTrailingComma(ktValueArgument, padRight(expr, suffix(ktValueArgument)), i == ktValueArguments.size() - 1));
            }
        }

        return JContainer.build(prefix(argumentList), expressions, Markers.EMPTY);
    }

    private Space toSpace(@Nullable PsiElement element) {
        if (element == null || !isSpace(element.getNode())) {
            return Space.EMPTY;
        }

        IElementType elementType = element.getNode().getElementType();
        if (elementType == KtTokens.WHITE_SPACE) {
            return Space.build(maybeAdjustCRLF(element), emptyList());
        } else if (elementType == KtTokens.EOL_COMMENT ||
                   elementType == KtTokens.BLOCK_COMMENT) {
            String nodeText = maybeAdjustCRLF(element);
            boolean isBlockComment = ((PsiComment) element).getTokenType() == KtTokens.BLOCK_COMMENT;
            String comment = isBlockComment ? nodeText.substring(2, nodeText.length() - 2) : nodeText.substring(2);
            List<Comment> comments = new ArrayList<>(1);
            comments.add(new TextComment(isBlockComment, comment, "", Markers.EMPTY));
            return Space.build("", comments);
        } else if (elementType == KtTokens.DOC_COMMENT) {
            return kdocToSpace((KDoc) element);
        }

        return Space.EMPTY;
    }

    // replace `\n` to CRLF back if it's CRLF in the source
    private String maybeAdjustCRLF(PsiElement element) {
        String text = element.getText();
        boolean isStringTemplateEntry = element instanceof KtLiteralStringTemplateEntry;
        if (!isSpace(element.getNode()) && !isStringTemplateEntry) {
            return text;
        }

        TextRange range = element.getTextRange();
        int left = findFirstGreaterOrEqual(cRLFLocations, range.getStartOffset());
        int right = left != -1 ? findFirstLessOrEqual(cRLFLocations, range.getEndOffset(), left) : -1;
        boolean hasCRLF = left != -1 && left <= right;
        if (hasCRLF) {
            for (int i = right; i >= left; i--) {
                text = replaceNewLineWithCRLF(text, cRLFLocations.get(i) - range.getStartOffset());
            }
        }
        return text;
    }

    private Space kdocToSpace(KDoc kDoc) {
        StringBuilder sb = new StringBuilder();

        Iterator<PsiElement> iterator = PsiUtilsKt.getAllChildren(kDoc).iterator();
        while (iterator.hasNext()) {
            PsiElement it = iterator.next();
            String text = it.getText();
            if (it instanceof PsiWhiteSpace) {
                text = replaceCRLF((PsiWhiteSpace) it);
            } else if (it instanceof KDocSection) {
                text = KDocSectionToString((KDocSection) it);
            }

            sb.append(text);
        }

        String source = sb.toString();
        String comment = source.substring(2, source.length() - 2);
        List<Comment> comments = new ArrayList<>(1);
        comments.add(new TextComment(true, comment, "", Markers.EMPTY));
        return Space.build("", comments);
    }

    private String KDocSectionToString(KDocSection kDocSection) {
        StringBuilder sb = new StringBuilder();
        Iterator<PsiElement> iterator = PsiUtilsKt.getAllChildren(kDocSection).iterator();
        while (iterator.hasNext()) {
            PsiElement it = iterator.next();
            String text = it.getText();
            if (it instanceof PsiWhiteSpace) {
                text = replaceCRLF((PsiWhiteSpace) it);
            }
            sb.append(text);
        }

        return sb.toString();
    }

    // replace `\n` to CRLF back if it's CRLF in the source
    private String replaceCRLF(PsiWhiteSpace wp) {
        String text = wp.getText();
        TextRange range = wp.getTextRange();
        int left = findFirstGreaterOrEqual(cRLFLocations, range.getStartOffset());
        int right = left != -1 ? findFirstLessOrEqual(cRLFLocations, range.getEndOffset(), left) : -1;
        boolean hasCRLF = left != -1 && left <= right;

        if (hasCRLF) {
            for (int i = right; i >= left; i--) {
                text = replaceNewLineWithCRLF(text, cRLFLocations.get(i) - range.getStartOffset());
            }
        }
        return text;
    }

    private Space space(@Nullable PsiElement node) {
        Space space = Space.EMPTY;
        while (node != null) {
            if (isSpace(node.getNode())) {
                space = merge(space, toSpace(node));
            } else {
                break;
            }
            node = node.getNextSibling();
        }
        return space;
    }

    public static Space merge(@Nullable Space s1, @Nullable Space s2) {
        if (s1 == null || s1.isEmpty()) {
            return s2 != null ? s2 : Space.EMPTY;
        } else if (s2 == null || s2.isEmpty()) {
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

    private static J.Identifier mergePrefix(Space prefix, J.Identifier id) {
        return id.getAnnotations().isEmpty() ? id.withPrefix(merge(prefix, id.getPrefix())) :
                id.withAnnotations(ListUtils.mapFirst(id.getAnnotations(), ann -> ann.withPrefix(merge(prefix, ann.getPrefix()))));
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

    private @Nullable PsiElement findLastNotSpaceChild(@Nullable PsiElement parent) {
        return findLastChild(parent, psi -> !isSpace(psi.getNode()));
    }

    private static @Nullable PsiElement findFirstChild(@Nullable PsiElement parent, Predicate<PsiElement> condition) {
        if (parent == null) {
            return null;
        }

        for (PsiElement child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (condition.test(child)) {
                return child;
            }
        }

        return null;
    }

    private static @Nullable PsiElement findFirstNonSpaceChild(@Nullable PsiElement parent) {
        return findFirstChild(parent, child -> !isSpace(child.getNode()));
    }

    private static @Nullable PsiElement findLastChild(@Nullable PsiElement parent, Predicate<PsiElement> condition) {
        if (parent == null) {
            return null;
        }

        for (PsiElement child = parent.getLastChild(); child != null; child = child.getPrevSibling()) {
            if (condition.test(child)) {
                return child;
            }
        }

        return null;
    }

    private @Nullable PsiElement findLastSpaceChild(@Nullable PsiElement parent) {
        if (parent == null) {
            return null;
        }

        PsiElement ret = null;
        for (PsiElement child = parent.getLastChild(); child != null; child = child.getPrevSibling()) {
            if (isSpace(child.getNode())) {
                ret = child;
            } else {
                break;
            }
        }
        return ret;
    }

    private static <E> void addIfNotNull(Set<E> set, @Nullable E element) {
        if (element != null) {
            set.add(element);
        }
    }

    private static Set<PsiElement> preConsumedInfix(PsiElement element) {
        Set<PsiElement> consumed = new HashSet<>();
        addIfNotNull(consumed, getFirstSpaceChildOrNull(element));
        return consumed;
    }

    private @Nullable PsiElement findTrailingComma(PsiElement element) {
        PsiElement nextSibling = element.getNextSibling();
        while (nextSibling != null) {
            if (nextSibling.getNode().getElementType() == KtTokens.COMMA) {
                return nextSibling;
            }
            nextSibling = nextSibling.getNextSibling();
        }
        return null;
    }

    private int findFirstGreaterOrEqual(List<Integer> sequence, int target) {
        int left = 0;
        int right = sequence.size() - 1;
        int resultIndex = -1;

        while (left <= right) {
            int mid = left + (right - left) / 2;
            int midValue = sequence.get(mid);

            if (midValue >= target) {
                resultIndex = mid;
                right = mid - 1;
            } else {
                left = mid + 1;
            }
        }

        return resultIndex;
    }

    private int findFirstLessOrEqual(List<Integer> sequence, int target, int left) {
        int right = sequence.size() - 1;
        int resultIndex = -1;

        while (left <= right) {
            int mid = left + (right - left) / 2;
            int midValue = sequence.get(mid);

            if (midValue <= target) {
                resultIndex = mid;
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }

        return resultIndex;
    }

    private String replaceNewLineWithCRLF(String source, int index) {
        if (index < 0 || index >= source.length()) {
            return source;
        }

        StringBuilder sb = new StringBuilder(source);
        char charAtIndex = source.charAt(index);
        if (charAtIndex != '\n') {
            return source;
        }

        sb.setCharAt(index, '\r');
        sb.insert(index + 1, '\n');

        return sb.toString();
    }

    private static @Nullable ASTNode getNodeOrNull(@Nullable PsiElement psiElement) {
        return psiElement != null ? psiElement.getNode() : null;
    }
}
