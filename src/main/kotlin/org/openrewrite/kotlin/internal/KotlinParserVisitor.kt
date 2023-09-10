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
package org.openrewrite.kotlin.internal

import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.KtFakeSourceElementKind.GeneratedLambdaLabel
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.lang.LighterASTNode
import org.jetbrains.kotlin.com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.contracts.description.LogicOperationKind
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.contracts.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyGetter
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertySetter
import org.jetbrains.kotlin.fir.declarations.impl.FirPrimaryConstructor
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirElseIfTrueCondition
import org.jetbrains.kotlin.fir.expressions.impl.FirNoReceiverExpression
import org.jetbrains.kotlin.fir.expressions.impl.FirSingleExpressionBlock
import org.jetbrains.kotlin.fir.expressions.impl.FirUnitExpression
import org.jetbrains.kotlin.fir.references.*
import org.jetbrains.kotlin.fir.resolve.toFirRegularClassSymbol
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.FirImplicitNullableAnyTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitUnitTypeRef
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitor
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.allChildren
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.types.ConstantValueKind
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.util.getChildren
import org.openrewrite.Cursor
import org.openrewrite.ExecutionContext
import org.openrewrite.FileAttributes
import org.openrewrite.ParseExceptionResult
import org.openrewrite.Tree.randomId
import org.openrewrite.internal.ListUtils
import org.openrewrite.internal.StringUtils
import org.openrewrite.java.internal.JavaTypeCache
import org.openrewrite.java.marker.ImplicitReturn
import org.openrewrite.java.marker.OmitParentheses
import org.openrewrite.java.marker.TrailingComma
import org.openrewrite.java.tree.*
import org.openrewrite.java.tree.TypeTree.build
import org.openrewrite.kotlin.KotlinParser
import org.openrewrite.kotlin.KotlinTypeMapping
import org.openrewrite.kotlin.marker.*
import org.openrewrite.kotlin.tree.K
import org.openrewrite.marker.Markers
import org.openrewrite.style.NamedStyles
import java.nio.charset.Charset
import java.nio.file.Path
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Consumer
import java.util.function.Function
import java.util.stream.Collectors
import kotlin.math.max
import kotlin.math.min


class KotlinParserVisitor(
    kotlinSource: KotlinSource,
    relativeTo: Path?,
    styles: List<NamedStyles>,
    typeCache: JavaTypeCache,
    firSession: FirSession,
    data: ExecutionContext
) :
    FirDefaultVisitor<J?, ExecutionContext>() {
    private val sourcePath: Path
    private val fileAttributes: FileAttributes?
    private val source: String
    private val charset: Charset
    private val charsetBomMarked: Boolean
    private val styles: List<NamedStyles>
    private val typeMapping: KotlinTypeMapping
    private val data: ExecutionContext
    private val firSession: FirSession
    private var cursor = 0
    private var nodes: Map<Int, ASTNode>
    private val generatedFirProperties: MutableMap<TextRange, FirProperty>

    // Associate top-level function and property declarations to the file.
    private var currentFile: FirFile? = null
    private var aliasImportMap: MutableMap<String, String>

    init {
        sourcePath = kotlinSource.input.getRelativePath(relativeTo)
        fileAttributes = kotlinSource.input.fileAttributes
        val `is` = kotlinSource.input.getSource(data)
        source = `is`.readFully()
        charset = `is`.charset
        charsetBomMarked = `is`.isCharsetBomMarked
        this.styles = styles
        typeMapping = KotlinTypeMapping(typeCache, firSession)
        this.data = data
        this.firSession = firSession
        this.nodes = kotlinSource.nodes
        generatedFirProperties = HashMap()
        aliasImportMap = HashMap()
    }

    override fun visitFile(file: FirFile, data: ExecutionContext): J {
        currentFile = file
        generatedFirProperties.clear()
        var annotations: List<J.Annotation>? = null
        val annotationList = PsiTreeUtil.findChildOfType(
            getRealPsiElement(file),
            KtFileAnnotationList::class.java
        )
        if (annotationList != null) {
            annotations = mapFileAnnotations(annotationList, file.annotations)
        }
        var paddedPkg: JRightPadded<J.Package>? = null
        if (!file.packageDirective.packageFqName.isRoot) {
            val pkg: J.Package = try {
                visitPackageDirective(file.packageDirective, data) as J.Package
            } catch (e: Exception) {
                throw KotlinParsingException("Failed to parse package directive", e)
            }
            paddedPkg = maybeSemicolon(pkg)
        }
        val imports: MutableList<JRightPadded<J.Import>> = ArrayList(file.imports.size)
        for (anImport in file.imports) {
            val importStatement: J.Import = try {
                visitImport(anImport, data) as J.Import
            } catch (e: Exception) {
                throw KotlinParsingException("Failed to parse import", e)
            }
            imports.add(maybeSemicolon(importStatement))
        }
        val statements: MutableList<JRightPadded<Statement>> = ArrayList(file.declarations.size)
        for (declaration in file.declarations) {
            var statement: Statement?
            val savedCursor = cursor
            try {
                statement = visitElement(declaration, data) as Statement
            } catch (e: Exception) {
                if (declaration.source == null || getRealPsiElement(declaration) == null) {
                    throw KotlinParsingException("Failed to parse declaration", e)
                }
                cursor = savedCursor
                val prefix = whitespace()
                var text = getRealPsiElement(declaration)!!.text
                if (prefix.comments.isNotEmpty()) {
                    val lastComment = prefix.comments[prefix.comments.size - 1]
                    val prefixText = lastComment.printComment(Cursor(null, lastComment)) + lastComment.suffix
                    text = text.substring(prefixText.length)
                }
                skip(text)
                statement = J.Unknown(
                    randomId(),
                    prefix,
                    Markers.EMPTY,
                    J.Unknown.Source(
                        randomId(),
                        Space.EMPTY,
                        Markers.build(
                            listOf(
                                ParseExceptionResult.build(
                                    KotlinParser::class.java, e
                                )
                                    .withTreeType(declaration.source!!.kind.toString())
                            )
                        ),
                        text
                    )
                )
            }
            statements.add(maybeSemicolon(statement!!))
        }
        return K.CompilationUnit(
            randomId(),
            Space.EMPTY,
            Markers.build(styles),
            sourcePath,
            fileAttributes,
            charset.name(),
            charsetBomMarked,
            null,
            annotations ?: emptyList<J.Annotation>(),
            paddedPkg,
            imports,
            statements,
            Space.format(source, cursor, source.length)
        )
    }

    override fun visitErrorNamedReference(errorNamedReference: FirErrorNamedReference, data: ExecutionContext): J {
        return if (errorNamedReference.source is KtRealPsiSourceElement && (errorNamedReference.source as KtRealPsiSourceElement).psi is KtNameReferenceExpression) {
            val nameReferenceExpression =
                (errorNamedReference.source as KtRealPsiSourceElement).psi as KtNameReferenceExpression
            val name =
                if (nameReferenceExpression.getIdentifier() == null) "{error}" else nameReferenceExpression.getIdentifier()!!.text
            val prefix = sourceBefore(name)
            J.Identifier(
                randomId(),
                prefix,
                Markers.EMPTY,
                emptyList<J.Annotation>(),
                name,
                null,
                null
            )
        } else if (errorNamedReference.source is KtLightSourceElement) {
            val fullName: String = errorNamedReference.source!!.lighterASTNode.toString()
            val prefix = whitespace()
            skip(fullName)
            (build(fullName) as J).withPrefix(prefix)
        } else if (errorNamedReference.source is KtFakeSourceElement) {
            val psi = (errorNamedReference.source as KtFakeSourceElement).psi as KtNameReferenceExpression
            skip(psi.getReferencedName())
            build(psi.getReferencedName()) as J
        } else {
            throw UnsupportedOperationException("Unsupported error name reference type.")
        }
    }

    override fun visitAnnotationCall(annotationCall: FirAnnotationCall, data: ExecutionContext): J {
        val psiAnnotation = getRealPsiElement(annotationCall)
        val psiAnnotationChildren = psiAnnotation?.children
        val hasValueArgumentList = if (psiAnnotationChildren != null) {
            psiAnnotationChildren.any { it is KtValueArgumentList}
        } else false

        val prefix = whitespace()
        var markers = Markers.EMPTY
        skip("@")
        when (annotationCall.useSiteTarget) {
            AnnotationUseSiteTarget.FILE -> {
                skip("file")
                markers = markers.addIfAbsent(AnnotationCallSite(randomId(), "file", sourceBefore(":")))
            }

            AnnotationUseSiteTarget.PROPERTY_GETTER -> {
                skip("get")
                markers = markers.addIfAbsent(AnnotationCallSite(randomId(), "get", sourceBefore(":")))
            }

            AnnotationUseSiteTarget.PROPERTY_SETTER -> {
                skip("set")
                markers = markers.addIfAbsent(AnnotationCallSite(randomId(), "set", sourceBefore(":")))
            }

            AnnotationUseSiteTarget.CONSTRUCTOR_PARAMETER -> {
                skip("param")
                markers = markers.addIfAbsent(AnnotationCallSite(randomId(), "param", sourceBefore(":")))
            }

            AnnotationUseSiteTarget.FIELD -> {
                skip("field")
                markers = markers.addIfAbsent(AnnotationCallSite(randomId(), "field", sourceBefore(":")))
            }

            AnnotationUseSiteTarget.RECEIVER -> {
                skip("receiver")
                markers = markers.addIfAbsent(AnnotationCallSite(randomId(), "receiver", sourceBefore(":")))
            }

            AnnotationUseSiteTarget.SETTER_PARAMETER -> {
                skip("setparam")
                markers = markers.addIfAbsent(AnnotationCallSite(randomId(), "setparam", sourceBefore(":")))
            }

            AnnotationUseSiteTarget.PROPERTY -> {
                skip("property")
                markers = markers.addIfAbsent(AnnotationCallSite(randomId(), "property", sourceBefore(":")))
            }

            AnnotationUseSiteTarget.PROPERTY_DELEGATE_FIELD -> {
                skip("delegate")
                markers = markers.addIfAbsent(AnnotationCallSite(randomId(), "delegate", sourceBefore(":")))
            }

            null -> {
            }
        }
        val name = visitElement(annotationCall.annotationTypeRef, data) as NameTree
        var args: JContainer<Expression>? = null
        if (hasValueArgumentList || annotationCall.argumentList.arguments.isNotEmpty()) {
            val argsPrefix = sourceBefore("(")
            val expressions: List<JRightPadded<Expression>> = if (annotationCall.argumentList.arguments.size == 1) {
                if (annotationCall.argumentList.arguments[0] is FirVarargArgumentsExpression) {
                    val varargArgumentsExpression =
                        annotationCall.argumentList.arguments[0] as FirVarargArgumentsExpression
                    convertAllToExpressions(
                        varargArgumentsExpression.arguments, ",", ")", data
                    )
                } else {
                    val arg = annotationCall.argumentList.arguments[0]
                    listOf(
                        convertToExpression(
                            arg,
                            { sourceBefore(")") },
                            data
                        )!!
                    )
                }
            } else {
                convertAllToExpressions(
                    annotationCall.argumentList.arguments, ",", ")", data
                )
            }
            args = JContainer.build(argsPrefix, expressions, Markers.EMPTY)
        }
        return J.Annotation(
            randomId(),
            prefix,
            markers,
            name,
            args
        )
    }

    override fun visitAnonymousFunction(anonymousFunction: FirAnonymousFunction, data: ExecutionContext): J {
        var markers = Markers.EMPTY
        var label: J.Label? = null
        if (anonymousFunction.label != null &&
            anonymousFunction.label!!.source != null &&
            anonymousFunction.label?.source?.kind !== GeneratedLambdaLabel
        ) {
            label = visitElement(anonymousFunction.label!!, data) as J.Label?
        }
        val prefix = whitespace()
        val hasBraces = source[cursor] == '{'
        if (hasBraces) {
            skip("{")
        } else {
            markers = markers.addIfAbsent(OmitBraces(randomId()))
        }
        var omitDestruct = false
        val valueParams: MutableList<JRightPadded<J>> = ArrayList(anonymousFunction.valueParameters.size)
        if (anonymousFunction.valueParameters.isNotEmpty()) {
            val parameters = anonymousFunction.valueParameters
            for (i in parameters.indices) {
                val p = parameters[i]
                if (p.source?.kind is KtFakeSourceElementKind.ItLambdaParameter) {
                    continue
                }
                // FIXME: replace with destruct declaration
                if ("<destruct>" == p.name.asString()) {
                    omitDestruct = true
                    val destructPrefix = sourceBefore("(")
                    val saveCursor = cursor
                    val params = sourceBefore(")").whitespace
                    val paramNames = params.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    val destructParams: MutableList<JRightPadded<J>> = ArrayList(paramNames.size)
                    cursor(saveCursor)
                    var typeArguments: Array<out ConeTypeProjection>? = null
                    if (p.returnTypeRef is FirResolvedTypeRef && (p.returnTypeRef.source == null || p.returnTypeRef.source!!.kind !is KtFakeSourceElementKind)) {
                        val typeRef = p.returnTypeRef as FirResolvedTypeRef
                        typeArguments = typeRef.type.typeArguments
                    }
                    for (j in paramNames.indices) {
                        val paramName = paramNames[j].trim { it <= ' ' }
                        val type = if (typeArguments == null || j >= typeArguments.size) null else typeMapping.type(
                            typeArguments[j]
                        )
                        val param = createIdentifier(paramName, type, null)
                        var paramExpr = JRightPadded.build<J>(param)
                        val after = if (j < paramNames.size - 1) sourceBefore(",") else sourceBefore(")")
                        paramExpr = paramExpr.withAfter(after)
                        destructParams.add(paramExpr)
                    }

                    // Create a new J.Lambda.Parameters instance to represent the destructured parameters.
                    // { (a, b), c -> ... } // a destructured pair and another parameter
                    // { (a, b), (c, d) -> ... } // a destructured pair and another destructured pair
                    val destructParamsExpr =
                        J.Lambda.Parameters(randomId(), destructPrefix, Markers.EMPTY, true, destructParams)
                    valueParams.add(JRightPadded.build(destructParamsExpr))
                } else {
                    val lambda = visitElement(p, data)
                    if (lambda != null) {
                        val param: JRightPadded<J> =
                            if (i != parameters.size - 1) {
                                JRightPadded(lambda, sourceBefore(","), Markers.EMPTY)
                            } else {
                                maybeTrailingComma(lambda)
                            }
                        valueParams.add(param)
                    }
                }
            }
        }
        var params = J.Lambda.Parameters(randomId(), Space.EMPTY, Markers.EMPTY, false, valueParams)
        val saveCursor = cursor
        val arrowPrefix = whitespace()
        if (skip("->")) {
            params = if (params.parameters.isEmpty()) {
                params.padding.withParams(
                    listOf(
                        JRightPadded
                            .build(J.Empty(randomId(), Space.EMPTY, Markers.EMPTY) as J)
                            .withAfter(arrowPrefix)
                    )
                )
            } else {
                params.padding.withParams(
                    ListUtils.mapLast(
                        params.padding.params
                    ) { param: JRightPadded<J> ->
                        param.withAfter(
                            arrowPrefix
                        )
                    })
            }
        } else {
            cursor(saveCursor)
        }
        val skip = Collections.newSetFromMap(IdentityHashMap<FirElement, Boolean>())
        if (omitDestruct && anonymousFunction.body != null) {
            // FIXME: replace with destruct declaration
            // skip destructured property declarations.
            for (statement in anonymousFunction.body!!.statements) {
                if (statement is FirProperty && statement.initializer is FirComponentCall &&
                    (statement.initializer as FirComponentCall).explicitReceiver is FirPropertyAccessExpression &&
                    ((statement.initializer as FirComponentCall).explicitReceiver as FirPropertyAccessExpression).calleeReference is FirResolvedNamedReference &&
                    "<destruct>" == (((statement.initializer as FirComponentCall).explicitReceiver as FirPropertyAccessExpression).calleeReference as FirResolvedNamedReference).name.asString()
                ) {
                    skip.add(statement)
                }
            }
        }
        var body = if (anonymousFunction.body == null) null else visitBlock0(anonymousFunction.body!!, skip, false, data)
        if (hasBraces && body is J.Block) {
            body = body.withEnd(sourceBefore("}"))
        }
        if (body == null) {
            body = J.Block(
                randomId(),
                Space.EMPTY,
                Markers.EMPTY,
                JRightPadded(false, Space.EMPTY, Markers.EMPTY),
                emptyList(),
                Space.EMPTY
            )
        }
        val lambda = J.Lambda(
            randomId(),
            prefix,
            markers,
            params,
            Space.EMPTY,
            body,
            null
        )
        return if (label != null) label.withStatement(lambda) else lambda
    }

    override fun visitAnonymousFunctionExpression(
        anonymousFunctionExpression: FirAnonymousFunctionExpression,
        data: ExecutionContext
    ): J {
        val anonymousFunction = anonymousFunctionExpression.anonymousFunction
        if (anonymousFunction.isLambda) {
            return visitAnonymousFunction(anonymousFunction, data)
        } else {
            val prefix = sourceBefore("fun")
            val before = sourceBefore("(")
            val params: List<JRightPadded<J>> = if (anonymousFunction.valueParameters.isNotEmpty())
                convertAll(
                    anonymousFunction.valueParameters, ",", ")", data
                )
            else
                listOf(
                    padRight(
                        J.Empty(randomId(), sourceBefore(")"), Markers.EMPTY), Space.EMPTY
                    )
                )

            val body = mapFunctionBody(anonymousFunction, data)!!
            return J.Lambda(
                randomId(),
                prefix,
                Markers.EMPTY.addIfAbsent(AnonymousFunction(randomId())),
                J.Lambda.Parameters(randomId(), before, Markers.EMPTY, true, params),
                Space.EMPTY,
                body,
                null
            )
        }
    }

    private fun mapFunctionBody(function: FirFunction, data: ExecutionContext): J.Block? {
        val saveCursor = cursor
        val before = whitespace()
        return if (function.body is FirSingleExpressionBlock) {
            if (skip("=")) {
                convertToBlock(function.body as FirSingleExpressionBlock, data).withPrefix(before)
            } else {
                throw IllegalStateException("Unexpected single block expression, cursor is likely at the wrong position.")
            }
        } else if (function.body is FirBlock) {
            cursor(saveCursor)
            visitElement(function.body!!, data) as J.Block?
        } else if (function.body == null) {
            cursor(saveCursor)
            null
        } else {
            throw IllegalStateException("Unexpected function body.")
        }
    }

    override fun visitAnonymousInitializer(anonymousInitializer: FirAnonymousInitializer, data: ExecutionContext): J {
        val prefix = sourceBefore("init")
        var staticInit = visitElement(anonymousInitializer.body!!, data) as J.Block
        staticInit = staticInit.padding.withStatic(staticInit.padding.static.withAfter(staticInit.prefix))
        staticInit = staticInit.withPrefix(prefix)
        return staticInit.withStatic(true)
    }

    override fun visitAnonymousObject(anonymousObject: FirAnonymousObject, data: ExecutionContext): J {
        var saveCursor = cursor
        val objectPrefix = whitespace()
        var markers = Markers.EMPTY
        var typeExpressionPrefix = Space.EMPTY
        var prefix = Space.EMPTY
        var clazz: TypeTree? = null
        if (skip("object")) {
            markers = markers.addIfAbsent(KObject(randomId(), objectPrefix))
            prefix = whitespace()
            if (skip(":")) {
                typeExpressionPrefix = prefix
                prefix = whitespace()
                clazz = visitElement(anonymousObject.superTypeRefs[0], data) as TypeTree?
            }
        } else {
            cursor(saveCursor)
        }
        val args: JContainer<Expression>
        saveCursor = cursor
        val before = whitespace()
        args = if (source[cursor] == '(') {
            if (anonymousObject.declarations.isNotEmpty() &&
                anonymousObject.declarations[0] is FirPrimaryConstructor &&
                (anonymousObject.declarations[0] as FirPrimaryConstructor).delegatedConstructor != null &&
                (anonymousObject.declarations[0] as FirPrimaryConstructor).delegatedConstructor!!.argumentList.arguments.isNotEmpty()
            ) {
                cursor(saveCursor)
                val delegatedConstructor =
                    (anonymousObject.declarations[0] as FirPrimaryConstructor).delegatedConstructor!!
                mapFunctionalCallArguments(delegatedConstructor)
            } else {
                skip("(")
                JContainer.build(
                    before,
                    listOf(
                        padRight(
                            J.Empty(randomId(), sourceBefore(")"), Markers.EMPTY), Space.EMPTY
                        )
                    ), Markers.EMPTY
                )
            }
        } else {
            cursor(saveCursor)
            JContainer.empty<Expression>()
                .withMarkers(Markers.build(listOf(OmitParentheses(randomId()))))
        }
        saveCursor = cursor
        var body: J.Block? = null
        val bodyPrefix = whitespace()
        if (skip("{")) {
            val declarations: MutableList<FirElement> = ArrayList(anonymousObject.declarations.size)
            for (declaration in anonymousObject.declarations) {
                if (declaration.source != null && declaration.source!!.kind is KtFakeSourceElementKind) {
                    continue
                }
                declarations.add(declaration)
            }
            val statements: MutableList<JRightPadded<Statement>> = ArrayList(declarations.size)
            for (element in declarations) {
                val s = visitElement(element, data) as Statement?
                if (s != null) {
                    statements.add(JRightPadded.build(s))
                }
            }
            body = J.Block(
                randomId(),
                bodyPrefix,
                Markers.EMPTY,
                JRightPadded(false, Space.EMPTY, Markers.EMPTY),
                statements,
                sourceBefore("}")
            )
        } else {
            cursor(saveCursor)
        }
        return J.NewClass(
            randomId(),
            prefix,
            markers,
            null,
            typeExpressionPrefix,
            clazz,
            args,
            body,
            null
        )
    }

    override fun visitAnonymousObjectExpression(
        anonymousObjectExpression: FirAnonymousObjectExpression,
        data: ExecutionContext
    ): J {
        // Pass through to the anonymous object since the `<anonymous>` typeRef on the expression is not necessary.
        return visitElement(anonymousObjectExpression.anonymousObject, data)!!
    }

    @OptIn(SymbolInternals::class)
    override fun visitCallableReferenceAccess(
        callableReferenceAccess: FirCallableReferenceAccess,
        data: ExecutionContext
    ): J {
        val prefix = whitespace()
        val reference = callableReferenceAccess.calleeReference as FirResolvedCallableReference
        var methodReferenceType: JavaType.Method? = null
        if (reference.resolvedSymbol is FirNamedFunctionSymbol) {
            methodReferenceType = typeMapping.methodDeclarationType(
                (reference.resolvedSymbol as FirNamedFunctionSymbol).fir,
                TypeUtils.asFullyQualified(typeMapping.type(callableReferenceAccess.explicitReceiver)), getCurrentFile()
            )
        }
        var fieldReferenceType: JavaType.Variable? = null
        if (reference.resolvedSymbol is FirPropertySymbol) {
            fieldReferenceType = typeMapping.variableType(
                reference.resolvedSymbol as FirVariableSymbol<out FirVariable>,
                TypeUtils.asFullyQualified(typeMapping.type(callableReferenceAccess.explicitReceiver)), getCurrentFile()
            )
        }

        val receiverExpr = callableReferenceAccess.explicitReceiver?.let {
            convertToExpression<Expression>(callableReferenceAccess.explicitReceiver!!, data)!!
        }

        val paddedExpr: JRightPadded<Expression> = if (receiverExpr == null) {
            padRight(J.Empty(randomId(), Space.EMPTY, Markers.EMPTY), sourceBefore("::"))
        } else {
            padRight(receiverExpr, sourceBefore("::"))
        }

        return J.MemberReference(
            randomId(),
            prefix,
            Markers.EMPTY,
            paddedExpr,
            null,
            padLeft(whitespace(), visitElement(callableReferenceAccess.calleeReference, data) as J.Identifier),
            typeMapping.type(callableReferenceAccess.calleeReference),
            methodReferenceType,
            fieldReferenceType
        )
    }

    override fun visitArrayOfCall(arrayOfCall: FirArrayOfCall, data: ExecutionContext): J {
        return K.ListLiteral(
            randomId(),
            sourceBefore("["),
            Markers.EMPTY,
            if (arrayOfCall.argumentList.arguments.isEmpty()) JContainer.build(
                listOf(
                    JRightPadded(
                        J.Empty(randomId(), Space.EMPTY, Markers.EMPTY), sourceBefore("]"), Markers.EMPTY
                    )
                )
            ) else JContainer.build(
                Space.EMPTY, convertAllToExpressions(
                    arrayOfCall.argumentList.arguments, ",", "]", data), Markers.EMPTY
            ),
            typeMapping.type(arrayOfCall)
        )
    }

    override fun visitBackingFieldReference(
        backingFieldReference: FirBackingFieldReference,
        data: ExecutionContext
    ): J {
        val name = if (backingFieldReference.name.asString().startsWith("$")) backingFieldReference.name.asString()
            .substring(1) else backingFieldReference.name.asString()
        return createIdentifier(name, backingFieldReference)
    }

    override fun visitBinaryLogicExpression(
        binaryLogicExpression: FirBinaryLogicExpression,
        data: ExecutionContext
    ): J {
        val prefix = whitespace()
        val left =
            convertToExpression<Expression>(binaryLogicExpression.leftOperand, data)!!
        var markers = Markers.EMPTY
        val opPrefix = whitespace()
        val op: J.Binary.Type
        if (LogicOperationKind.AND == binaryLogicExpression.kind) {
            skip("&&")
            op = J.Binary.Type.And
        } else if (LogicOperationKind.OR == binaryLogicExpression.kind) {
            if (skip(",")) {
                markers = Markers.build(listOf(LogicalComma(randomId())))
            } else {
                skip("||")
            }
            op = J.Binary.Type.Or
        } else {
            throw UnsupportedOperationException("Unsupported binary expression type " + binaryLogicExpression.kind.name)
        }
        val right =
            convertToExpression<Expression>(binaryLogicExpression.rightOperand, data)!!
        return J.Binary(
            randomId(),
            prefix,
            markers,
            left,
            padLeft(opPrefix, op),
            right,
            typeMapping.type(binaryLogicExpression)
        )
    }

    override fun visitBlock(block: FirBlock, data: ExecutionContext): J {
        return if (block is FirSingleExpressionBlock) visitSingleExpressionBlock(block, data) else visitBlock(block, emptySet(), data)
    }

    private fun visitSingleExpressionBlock(block: FirSingleExpressionBlock, data: ExecutionContext): J {
        return visitElement(block.statement, data)!!
    }

    /**
     * Map a FirBlock to a J.Block.
     *
     * @param block          target FirBlock.
     * @param skipStatements must use a [Set] constructed by [IdentityHashMap]. Kotlin uses FirBlocks to
     * represented certain AST elements. When an AST element is represented as a block, we need
     * to filter out statements that should not be processed.
     *
     *
     * I.E., a for loop in code will be a FirWhileLoop in the AST, and the body of the FirWhileLoop
     * (FirBlock) will contain statements that do not exist in code.
     * The additional statements contain AST information required to construct the J.ForLoop,
     * but should not be added as statements to the J.ForLoop#body.
     */
    private fun visitBlock(block: FirBlock, skipStatements: Set<FirElement>, data: ExecutionContext): J {
        return visitBlock0(block, skipStatements, true, data)
    }

    private fun visitBlock0(block: FirBlock, skipStatements: Set<FirElement>, consumeBraces: Boolean, data: ExecutionContext): J.Block {
        var saveCursor = cursor
        var prefix: Space = whitespace()
        val hasBraces = consumeBraces && skip("{")
        if (!hasBraces) {
            cursor(saveCursor)
            prefix = Space.EMPTY
        }

        val firStatements: MutableList<FirStatement> = ArrayList(block.statements.size)
        for (s in block.statements) {
            // Skip FirElements that should not be processed.
            if (!skipStatements.contains(s) &&
                    (s.source == null || s.source!!.kind !is KtFakeSourceElementKind.ImplicitConstructor)
            ) {
                firStatements.add(s)
            }
        }
        val statements: MutableList<JRightPadded<Statement>> = ArrayList(firStatements.size)
        var i = 0
        while (i < firStatements.size) {
            val firElement: FirElement = firStatements[i]
            val element = getRealPsiElement(firElement)
            var j: J? = null
            if (firElement is FirBlock && firElement.statements.size == 2) {
                // For loops are wrapped in a block and split into two FirElements.
                // The FirProperty at position 0 is the control of the for loop.
                // The FirWhileLoop at position 1 is the for loop, which is transformed to use an iterator.
                // So, the FirBlock is transformed to construct a J.ForEach that preserves source code.
                if (firElement.statements[0] is FirProperty && "<iterator>" == (firElement.statements[0] as FirProperty).name.asString() &&
                        firElement.statements[1] is FirWhileLoop
                ) {
                    j = mapForLoop(firElement)
                }
            }
            var skipImplicitDestructs = 0
            if (element is KtDestructuringDeclaration) {
                val destructEntries = element.children.filterIsInstance<KtDestructuringDeclarationEntry>()
                val psiFirPairs: MutableList<Pair<PsiElement, FirStatement?>> = mutableListOf()

                psiFirPairs.add(Pair(element, firElement as FirStatement))
                for (destructEntry in destructEntries) {
                    destructEntry.endOffset
                    var fe: FirStatement? = null
                    for (fs in firStatements) {
                        if (fs.source != null &&
                                fs.source!!.startOffset == destructEntry.startOffset &&
                                fs.source!!.endOffset == destructEntry.endOffset) {
                            fe = fs
                            break
                        }
                    }
                    psiFirPairs.add(Pair(destructEntry, fe))
                }

                j = mapDestructProperty(psiFirPairs)
                skipImplicitDestructs = element.entries.size
            }
            if (j == null) {
                j = visitElement(firElement, data)
                if (j !is Statement && j is Expression) {
                    j = K.ExpressionStatement(randomId(), j)
                }
            }
            i += skipImplicitDestructs
            var stat = JRightPadded.build(j as Statement)
            saveCursor = cursor
            val beforeSemicolon = whitespace()
            if (cursor < source.length && skip(";")) {
                stat = stat.withMarkers(stat.markers.add(Semicolon(randomId())))
                        .withAfter(beforeSemicolon)
            } else {
                cursor(saveCursor)
            }
            statements.add(stat)
            i++
        }
        return J.Block(
                randomId(),
                prefix,
                if (hasBraces) Markers.EMPTY else Markers.EMPTY.addIfAbsent(OmitBraces(randomId())),
                JRightPadded.build(false),
                statements,
                if (hasBraces) sourceBefore("}") else Space.EMPTY
        )
    }

    override fun visitBreakExpression(breakExpression: FirBreakExpression, data: ExecutionContext): J {
        val prefix = sourceBefore("break")
        var label: J.Identifier? = null
        if (breakExpression.target.labelName != null) {
            skip("@")
            label = createIdentifier(breakExpression.target.labelName)
        }
        return J.Break(
            randomId(),
            prefix,
            Markers.EMPTY,
            label
        )
    }

    override fun visitCatch(catch: FirCatch, data: ExecutionContext): J {
        val prefix = whitespace()
        skip("catch")
        val paramPrefix = sourceBefore("(")
        val paramDecl = visitElement(catch.parameter, data) as J.VariableDeclarations
        val param = J.ControlParentheses(
            randomId(),
            paramPrefix,
            Markers.EMPTY,
            padRight(paramDecl, sourceBefore(")"))
        )
        return J.Try.Catch(
            randomId(),
            prefix,
            Markers.EMPTY,
            param,
            visitElement(catch.block, data) as J.Block
        )
    }

    override fun visitCheckNotNullCall(checkNotNullCall: FirCheckNotNullCall, data: ExecutionContext): J {
        val j: J = visitElement(checkNotNullCall.argumentList.arguments[0], data)!!
        return j.withMarkers(j.markers.addIfAbsent(CheckNotNull(randomId(), sourceBefore("!!"))))
    }

    override fun visitComparisonExpression(comparisonExpression: FirComparisonExpression, data: ExecutionContext): J {
        val prefix = whitespace()
        val functionCall = comparisonExpression.compareToCall
        val receiver: FirElement =
            if (functionCall.explicitReceiver != null) functionCall.explicitReceiver!! else functionCall.dispatchReceiver
        val left =
            convertToExpression<Expression>(receiver, data)!!
        val opPrefix = sourceBefore(comparisonExpression.operation.operator)
        val op = mapOperation(comparisonExpression.operation)
        if (functionCall.argumentList.arguments.size != 1) {
            throw UnsupportedOperationException("Unsupported FirComparisonExpression argument size")
        }
        val right =
            convertToExpression<Expression>(functionCall.argumentList.arguments[0], data)!!
        return J.Binary(
            randomId(),
            prefix,
            Markers.EMPTY,
            left,
            padLeft(opPrefix, op),
            right,
            typeMapping.type(comparisonExpression)
        )
    }

    override fun <T> visitConstExpression(constExpression: FirConstExpression<T>, data: ExecutionContext): J {
        var prefix = Space.EMPTY
        val psiElement = getRealPsiElement(constExpression)
        if (constExpression.kind !is ConstantValueKind.String || ((psiElement != null) && psiElement.text.contains("\""))) {
            prefix = whitespace()
        }
        val valueSource =
            source.substring(cursor, cursor + constExpression.source!!.endOffset - constExpression.source!!.startOffset)
        cursor += valueSource.length
        val value: T = constExpression.value
        val type: JavaType.Primitive = if (constExpression.typeRef is FirResolvedTypeRef &&
            (constExpression.typeRef as FirResolvedTypeRef).type is ConeClassLikeType
        ) {
            val coneClassLikeType = (constExpression.typeRef as FirResolvedTypeRef).type as ConeClassLikeType
            typeMapping.primitive(coneClassLikeType)
        } else {
            throw IllegalArgumentException("Unresolved primitive type.")
        }
        return J.Literal(
            randomId(),
            prefix,
            Markers.EMPTY,
            value,
            valueSource,
            null,
            type
        )
    }

    override fun visitEqualityOperatorCall(equalityOperatorCall: FirEqualityOperatorCall, data: ExecutionContext): J {
        if (equalityOperatorCall.argumentList.arguments.size != 2) {
            throw UnsupportedOperationException("Unsupported number of equality operator arguments.")
        }
        val left: FirElement = equalityOperatorCall.argumentList.arguments[0]
        val right: FirElement = equalityOperatorCall.argumentList.arguments[1]
        if (left is FirWhenSubjectExpression || right is FirWhenSubjectExpression) {
            return if (left is FirWhenSubjectExpression) convertToExpression(
                right,
                data
            )!! else convertToExpression(left, data)!!
        }
        val prefix = whitespace()
        val leftExpr =
            convertToExpression<Expression>(left, data)!!
        val op = equalityOperatorCall.operation
        val opPrefix = sourceBefore(op.operator)
        val rightExpr =
            convertToExpression<Expression>(right, data)!!
        return if (op == FirOperation.IDENTITY || op == FirOperation.NOT_IDENTITY) {
            K.Binary(
                randomId(),
                prefix,
                Markers.EMPTY,
                leftExpr,
                padLeft(
                    opPrefix,
                    if (op == FirOperation.IDENTITY) K.Binary.Type.IdentityEquals else K.Binary.Type.IdentityNotEquals
                ),
                rightExpr,
                Space.EMPTY,
                typeMapping.type(equalityOperatorCall)
            )
        } else {
            J.Binary(
                randomId(),
                prefix,
                Markers.EMPTY,
                leftExpr,
                padLeft(opPrefix, mapOperation(op)),
                rightExpr,
                typeMapping.type(equalityOperatorCall)
            )
        }
    }

    override fun visitContinueExpression(continueExpression: FirContinueExpression, data: ExecutionContext): J {
        val prefix = sourceBefore("continue")
        var label: J.Identifier? = null
        if (continueExpression.target.labelName != null) {
            skip("@")
            label = createIdentifier(continueExpression.target.labelName)
        }
        return J.Continue(
            randomId(),
            prefix,
            Markers.EMPTY,
            label
        )
    }

    override fun visitDoWhileLoop(doWhileLoop: FirDoWhileLoop, data: ExecutionContext): J {
        var label: J.Label? = null
        if (doWhileLoop.label != null) {
            label = visitElement(doWhileLoop.label!!, data) as J.Label?
        }
        val prefix = whitespace()
        skip("do")
        val statement = J.DoWhileLoop(
            randomId(),
            prefix,
            Markers.EMPTY,
            JRightPadded.build(visitElement(doWhileLoop.block, data) as Statement),
            padLeft(sourceBefore("while"), mapControlParentheses(doWhileLoop.condition))
        )
        return if (label != null) label.withStatement(statement) else statement
    }

    override fun visitElvisExpression(elvisExpression: FirElvisExpression, data: ExecutionContext): J {
        val prefix = whitespace()
        val lhs =
            convertToExpression<Expression>(elvisExpression.lhs, data)!!
        val before = sourceBefore("?:")
        val rhs =
            convertToExpression<Expression>(elvisExpression.rhs, data)!!
        return J.Ternary(
            randomId(),
            prefix,
            Markers.EMPTY,
            J.Empty(randomId(), Space.EMPTY, Markers.EMPTY),
            padLeft(Space.EMPTY, lhs),
            padLeft(before, rhs),
            typeMapping.type(elvisExpression)
        )
    }

    override fun visitEnumEntry(enumEntry: FirEnumEntry, data: ExecutionContext): J {
        val prefix = whitespace()
        val annotations: List<J.Annotation?>? = mapAnnotations(enumEntry.annotations)
        return J.EnumValue(
            randomId(),
            prefix,
            Markers.EMPTY,
            annotations ?: emptyList(),
            createIdentifier(enumEntry.name.asString(), enumEntry),
            if (enumEntry.initializer is FirAnonymousObjectExpression) {
                visitElement((enumEntry.initializer as FirAnonymousObjectExpression).anonymousObject, data) as J.NewClass?
            } else {
                null
            }
        )
    }

    override fun visitSuperReference(superReference: FirSuperReference, data: ExecutionContext): J {
        val prefix = sourceBefore("super")
        return J.Identifier(
            randomId(),
            prefix,
            Markers.EMPTY,
            emptyList(),
            "super",
            null,
            null
        )
    }

    override fun visitFunctionCall(functionCall: FirFunctionCall, data: ExecutionContext): J {
        val origin = functionCall.origin
        val j: J = if (origin == FirFunctionCallOrigin.Operator && functionCall !is FirImplicitInvokeCall) {
            val operatorName = functionCall.calleeReference.name.asString()
            if (isUnaryOperation(operatorName)) {
                mapUnaryOperation(functionCall)
            } else if ("get" == operatorName) {
                val indexedAccess = mapFunctionCall(functionCall, false, data)
                indexedAccess.withMarkers(indexedAccess.markers.addIfAbsent(IndexedAccess(randomId())))
            } else if ("contains" == operatorName || "rangeTo" == operatorName || "set" == operatorName || "rangeUntil" == operatorName) {
                mapKotlinBinaryOperation(functionCall)
            } else if (operatorName in augmentedAssignOperators) {
                mapAugmentedAssign(functionCall)
            } else if ("provideDelegate" == operatorName) {
                // TODO should we really just entirely skip the `provideDelegate` call in the LST?
                visitElement(functionCall.explicitReceiver!!, data)!!
            } else {
                mapBinaryOperation(functionCall)
            }
        } else {
            mapFunctionCall(functionCall, origin == FirFunctionCallOrigin.Infix, data)
        }
        return j
    }

    private fun mapFunctionCall(functionCall: FirFunctionCall, isInfix: Boolean, data: ExecutionContext): J {
        val prefix = whitespace()
        val namedReference = functionCall.calleeReference
        return if (namedReference is FirResolvedNamedReference &&
            namedReference.resolvedSymbol is FirConstructorSymbol
        ) {
            var name: TypeTree = if (functionCall.explicitReceiver != null) {
                val expr =
                    convertToExpression<Expression>(functionCall.explicitReceiver!!, data)!!
                J.FieldAccess(
                    randomId(),
                    Space.EMPTY,
                    Markers.EMPTY,
                    expr,
                    padLeft(sourceBefore("."), createIdentifier(namedReference.name.asString(), namedReference)),
                    typeMapping.type(functionCall, getCurrentFile())
                )
            } else {
                visitElement(namedReference, data) as J.Identifier
            }
            val saveCursor = cursor
            whitespace()
            if (source[cursor] == '<' && functionCall.typeArguments.isNotEmpty()) {
                cursor(saveCursor)
                name = J.ParameterizedType(
                    randomId(),
                    Space.EMPTY,
                    Markers.EMPTY,
                    name,
                    mapTypeArguments(functionCall.typeArguments, data),
                    typeMapping.type(functionCall, getCurrentFile())
                )
            } else {
                cursor(saveCursor)
            }
            val args = mapFunctionalCallArguments(functionCall)
            J.NewClass(
                randomId(),
                prefix,
                Markers.EMPTY,
                null,
                Space.EMPTY,
                name,
                args,
                null,
                typeMapping.methodInvocationType(functionCall, getCurrentFile())
            )
        } else {
            var markers = Markers.EMPTY
            var select: JRightPadded<Expression>? = null
            if (isInfix) {
                markers = markers.addIfAbsent(Infix(randomId()))
                markers = markers.addIfAbsent(Extension(randomId()))
            }

            val implicitExtensionFunction = functionCall is FirImplicitInvokeCall
                    && functionCall.arguments.isNotEmpty()
                    && functionCall.source != null
                    && functionCall.source!!.startOffset < functionCall.calleeReference.source!!.startOffset
            if (functionCall !is FirImplicitInvokeCall || implicitExtensionFunction) {
                val receiver = if (implicitExtensionFunction) functionCall.arguments[0] else getReceiver(functionCall.explicitReceiver)
                if (receiver != null) {
                    val selectExpr = convertToExpression<Expression>(receiver, data)
                    if (selectExpr != null) {
                        val after = whitespace()
                        if (skip("?")) {
                            markers = markers.addIfAbsent(IsNullSafe(randomId(), Space.EMPTY))
                        }
                        skip(".")
                        select = JRightPadded.build(selectExpr).withAfter(after)
                    }
                }
            }

            val name = visitElement(namedReference, data) as J.Identifier
            var typeParams: JContainer<Expression>? = null
            if (functionCall.typeArguments.isNotEmpty()) {
                val saveCursor = cursor
                whitespace()
                val parseTypeArguments = source[cursor] == '<'
                cursor(saveCursor)
                if (parseTypeArguments) {
                    typeParams = mapTypeArguments(functionCall.typeArguments, data)
                }
            }

            val args = mapFunctionalCallArguments(functionCall, implicitExtensionFunction)

            var owner = getCurrentFile()
            if (namedReference is FirResolvedNamedReference) {
                val symbol = namedReference.resolvedSymbol
                if (symbol is FirNamedFunctionSymbol) {
                    val lookupTag: ConeClassLikeLookupTag? = symbol.containingClassLookupTag()
                    if (lookupTag != null) {
                        owner = lookupTag.toFirRegularClassSymbol(firSession)
                    }
                }
            }
            val type = typeMapping.methodInvocationType(functionCall, owner)
            J.MethodInvocation(
                randomId(),
                prefix,
                markers,
                select,
                typeParams,
                name.withType(type),
                args,
                type
            )
        }
    }

    private fun getReceiver(firElement: FirElement?): FirElement? {
        if (firElement?.source == null) {
            return null
        }
        var receiver: FirElement? = null
        when (firElement) {
            is FirCheckedSafeCallSubject -> {
                receiver = firElement.originalReceiverRef.value
            }

            is FirThisReceiverExpression -> {
                receiver = firElement
            }

            !is FirNoReceiverExpression -> {
                receiver = firElement
            }
        }
        return receiver
    }

    private fun mapDestructProperty(propertiesPairs: List<Pair<PsiElement, FirStatement?>>): K.DestructuringDeclaration {
        val prefix = whitespace()
        val initializer = propertiesPairs[0].second as FirProperty
        val destructuringDeclaration = getRealPsiElement(initializer) as KtDestructuringDeclaration?
        var modifiers: MutableList<J.Modifier> = ArrayList()
        val leadingAnnotations: MutableList<J.Annotation> = mutableListOf()
        var trailingAnnotations: MutableList<J.Annotation>? = mutableListOf()
        val modifierList = getModifierList(destructuringDeclaration)
        if (modifierList != null) {
            modifiers =
                mapModifierList(modifierList, initializer.annotations, leadingAnnotations, trailingAnnotations!!)
        }
        val keyword = destructuringDeclaration!!.valOrVarKeyword
        if ("val" == keyword!!.text) {
            modifiers.add(
                J.Modifier(
                    randomId(),
                    sourceBefore("val"),
                    Markers.EMPTY,
                    null,
                    J.Modifier.Type.Final,
                    trailingAnnotations!!
                )
            )
            trailingAnnotations = null
        } else if ("var" == keyword.text) {
            modifiers.add(mapToJModifier("var", trailingAnnotations!!))
            trailingAnnotations = null
        }
        val before = sourceBefore("(")
        val vars: MutableList<JRightPadded<J.VariableDeclarations.NamedVariable>> = ArrayList(propertiesPairs.size - 1)
        var paddedInitializer: JLeftPadded<Expression>? = null
        for (i in 1 until propertiesPairs.size) {
            val property = propertiesPairs[i].second as FirProperty?

            if (property == null) {
                val maybeBeforeUnderscore = whitespace()
                if (source[cursor] == '_') {
                    val nameVar = createIdentifier("_", null, null).withPrefix(maybeBeforeUnderscore)
                    val namedVariable = J.VariableDeclarations.NamedVariable(
                        randomId(),
                        Space.EMPTY,
                        Markers.EMPTY,
                        nameVar,
                        emptyList(),
                        null,
                        null
                    )

                    val paddedVariable = padRight(namedVariable, sourceBefore(","))
                    vars.add(paddedVariable)
                }

                continue
            }

            val entry = getRealPsiElement(property) as KtDestructuringDeclarationEntry
            val annotations: MutableList<J.Annotation> = mutableListOf()
            val propNode = getRealPsiElement(property)
            val modifierListVar = getModifierList(propNode)
            if (modifierListVar != null) {
                mapModifierList(modifierListVar, property.annotations, annotations, mutableListOf())
            }
            val vt = typeMapping.type(property, getCurrentFile()) as JavaType.Variable?
            var nameVar = createIdentifier(entry.name!!, vt?.type, vt).withAnnotations(annotations)
            nameVar = if (trailingAnnotations == null) nameVar else nameVar.withAnnotations(trailingAnnotations)
            var namedVariable = J.VariableDeclarations.NamedVariable(
                randomId(),
                Space.EMPTY,
                Markers.EMPTY,
                nameVar,
                emptyList(),
                null,
                vt
            )
            trailingAnnotations = null
            var j = visitComponentCall(property.initializer as FirComponentCall, data, true)
            if (j !is Expression && j is Statement) {
                j = K.StatementExpression(randomId(), j)
            }
            namedVariable =
                namedVariable.padding.withInitializer(padLeft(Space.build(" ", emptyList()), j as Expression))
            var paddedVariable: JRightPadded<J.VariableDeclarations.NamedVariable>
            if (i == propertiesPairs.size - 1 && initializer.initializer != null) {
                val after = sourceBefore(")")
                paddedInitializer = padLeft(sourceBefore("="), convertToExpression(initializer.initializer!!, data)!!)
                paddedVariable = padRight(namedVariable, after)
            } else {
                paddedVariable = padRight(namedVariable, sourceBefore(","))
            }
            vars.add(paddedVariable)
        }
        val variableDeclarations = J.VariableDeclarations(
            randomId(),
            Space.EMPTY,
            Markers.EMPTY,
            if (leadingAnnotations.isEmpty()) emptyList() else leadingAnnotations,
            modifiers,
            null,
            null,
            emptyList(),
            listOf(
                padRight(
                    J.VariableDeclarations.NamedVariable(
                        randomId(),
                        Space.EMPTY,
                        Markers.EMPTY,
                        J.Identifier(
                            randomId(),
                            Space.build(" ", emptyList()),
                            Markers.EMPTY,
                            emptyList(),
                            initializer.name.asString(),
                            null,
                            null
                        ),
                        emptyList(),
                        paddedInitializer,
                        null
                    ), Space.EMPTY
                )
            )
        )
        return K.DestructuringDeclaration(
            randomId(),
            prefix,
            Markers.EMPTY,
            variableDeclarations,
            JContainer.build(before, vars, Markers.EMPTY)
        )
    }

    private fun mapFileAnnotations(
        annotationList: KtFileAnnotationList,
        firAnnotations: List<FirAnnotation>
    ): MutableList<J.Annotation> {
        val annotationsMap: MutableMap<Int, FirAnnotation> = HashMap()
        for (annotation in firAnnotations) {
            if (annotation.source != null) {
                annotationsMap[annotation.source!!.startOffset] = annotation
            }
        }

        val annotations: MutableList<J.Annotation> = ArrayList(annotationList.children.size)
        for (annotation in annotationList.children) {
            // convert KtAnnotation to J.Annotation
            if (annotationsMap.containsKey(annotation.textRange.startOffset)) {
                val ann = visitElement(annotationsMap[annotation.textRange.startOffset]!!, data) as J.Annotation
                annotations.add(ann)
            } else {
                throw UnsupportedOperationException("Unexpected missing annotation.")
            }
        }
        return annotations
    }

    private fun mapFunctionalCallArguments(firCall: FirCall, skipFirstArgument: Boolean = false): JContainer<Expression> {
        var callPsi = getPsiElement(firCall)!!
        callPsi = if (callPsi is KtDotQualifiedExpression) callPsi.lastChild else callPsi
        val firArguments = if (skipFirstArgument) firCall.argumentList.arguments.subList(1, firCall.argumentList.arguments.size) else firCall.argumentList.arguments
        val flattenedExpressions = firArguments.stream()
            .map { e -> if (e is FirVarargArgumentsExpression) e.arguments else listOf(e) }
            .flatMap { it.stream() }
            .collect(Collectors.toList())
        val argumentCount = flattenedExpressions.size
        // Trailing lambda: https://kotlinlang.org/docs/lambdas.html#passing-trailing-lambdas
        val hasTrailingLambda = argumentCount > 0 && callPsi.lastChild is KtLambdaArgument
        val expressions: MutableList<JRightPadded<Expression>> = ArrayList(flattenedExpressions.size)
        val isLastArgumentLambda = flattenedExpressions.isNotEmpty() && flattenedExpressions[argumentCount - 1] is FirLambdaArgumentExpression
        val isInfix = firCall is FirFunctionCall && firCall.origin == FirFunctionCallOrigin.Infix

        var markers = Markers.EMPTY
        val args: JContainer<Expression>
        var saveCursor = cursor
        var containerPrefix = whitespace()
        var parenOrBrace = source[cursor++]
        val closing = when (parenOrBrace) { '[' -> "]" else -> ")" }
        if (parenOrBrace == '[') {
            markers = markers.addIfAbsent(IndexedAccess(randomId()))
        }
        val isCloseParen = parenOrBrace == '(' && source[cursor] == ')'
        if (isCloseParen && isLastArgumentLambda) {
            cursor++
            saveCursor = cursor
            parenOrBrace = source[cursor]
        } else if ((parenOrBrace != '(' && parenOrBrace != '[') || isInfix) {
            cursor(saveCursor)
            containerPrefix = Space.EMPTY
            markers = markers.addIfAbsent(OmitParentheses(randomId()))
        }

        if (firArguments.isEmpty()) {
            args = if (parenOrBrace == '{') {
                // function call arguments with no parens.
                cursor(saveCursor)
                JContainer.build(
                    containerPrefix,
                    listOf(padRight(J.Empty(randomId(), Space.EMPTY, Markers.EMPTY), Space.EMPTY)),
                    markers
                )
            } else {
                JContainer.build(
                    containerPrefix,
                    listOf(padRight(J.Empty(randomId(), sourceBefore(closing), Markers.EMPTY), Space.EMPTY)
                    ),
                    markers
                )
            }
        } else {
            var isTrailingLambda = false
            for (i in flattenedExpressions.indices) {
                isTrailingLambda = hasTrailingLambda && i == argumentCount - 1
                val expression = flattenedExpressions[i]
                var expr = convertToExpression<Expression>(expression, data)!!
                if (isTrailingLambda && expr !is J.Empty) {
                    expr = expr.withMarkers(expr.markers.addIfAbsent(TrailingLambdaArgument(randomId())))
                    expressions.add(padRight(expr, Space.EMPTY))
                    break
                }
                val padding = whitespace()
                var trailingComma: TrailingComma? = null
                if (!isInfix) {
                    if (isLastArgumentLambda && i == argumentCount - 2) {
                        trailingComma = if (skip(",")) TrailingComma(randomId(), whitespace()) else null
                        skip(closing)
                    } else if (i == argumentCount - 1) {
                        trailingComma = if (skip(",")) TrailingComma(randomId(), whitespace()) else null
                    } else {
                        skip(",")
                    }
                }
                var padded = padRight(expr, padding)
                padded =
                    if (trailingComma != null) padded.withMarkers(padded.markers.addIfAbsent(trailingComma)) else padded
                expressions.add(padded)
            }
            if (!isTrailingLambda && !isInfix) {
                skip(closing)
            }
            args = JContainer.build(containerPrefix, expressions, markers)
        }
        return args
    }

    private fun mapTypeArguments(types: List<FirElement>, data: ExecutionContext): JContainer<Expression> {
        val prefix = whitespace()
        skip("<")
        val parameters: MutableList<JRightPadded<Expression>> = ArrayList(types.size)
        for (i in types.indices) {
            val type = types[i]
            val padded = JRightPadded.build(convertToExpression<J>(type, data) as Expression)
                .withAfter(if (i < types.size - 1) sourceBefore(",") else whitespace())
            parameters.add(padded)
        }
        skip(">")
        return JContainer.build(prefix, parameters, Markers.EMPTY)
    }

    private fun mapUnaryOperation(functionCall: FirFunctionCall): J {
        val prefix = whitespace()
        val name = functionCall.calleeReference.name.asString()
        val op: JLeftPadded<J.Unary.Type>
        val expr: Expression
        when (name) {
            "dec" -> if (skip("--")) {
                op = padLeft(Space.EMPTY, J.Unary.Type.PreDecrement)
                expr = convertToExpression(functionCall.dispatchReceiver, data)!!
            } else {
                val saveCursor = cursor
                val opName = sourceBefore("--").whitespace.trim { it <= ' ' }
                cursor(saveCursor)
                expr = createIdentifier(opName)
                op = padLeft(sourceBefore("--"), J.Unary.Type.PostDecrement)
            }

            "inc" -> if (skip("++")) {
                op = padLeft(Space.EMPTY, J.Unary.Type.PreIncrement)
                expr = convertToExpression(functionCall.dispatchReceiver, data)!!
            } else {
                val saveCursor = cursor
                val opName = sourceBefore("++").whitespace.trim { it <= ' ' }
                cursor(saveCursor)
                expr = createIdentifier(opName)
                op = padLeft(sourceBefore("++"), J.Unary.Type.PostIncrement)
            }

            "not" -> {
                skip("!")
                op = padLeft(Space.EMPTY, J.Unary.Type.Not)
                expr = convertToExpression(functionCall.dispatchReceiver, data)!!
            }

            "unaryMinus" -> {
                skip("-")
                op = padLeft(Space.EMPTY, J.Unary.Type.Negative)
                expr = convertToExpression(functionCall.dispatchReceiver, data)!!
            }

            "unaryPlus" -> {
                skip("+")
                op = padLeft(Space.EMPTY, J.Unary.Type.Positive)
                expr = convertToExpression(functionCall.dispatchReceiver, data)!!
            }

            else -> throw UnsupportedOperationException("Unsupported unary operator type.")
        }
        return J.Unary(
            randomId(),
            prefix,
            Markers.EMPTY,
            op,
            expr,
            typeMapping.type(functionCall)
        )
    }

    private fun mapKotlinBinaryOperation(functionCall: FirFunctionCall): J {
        val prefix = whitespace()
        var left: Expression
        val opPrefix: Space
        val kotlinBinaryType: K.Binary.Type
        val right: Expression
        var after = Space.EMPTY
        when (functionCall.calleeReference.name.asString()) {
            "contains" -> {
                // Prevent SOE of methods with an implicit LHS that refers to the subject of a when expression.
                left = if (functionCall.argumentList.arguments[0] is FirWhenSubjectExpression) {
                    J.Empty(randomId(), Space.EMPTY, Markers.EMPTY)
                } else {
                    convertToExpression(functionCall.argumentList.arguments[0], data)!!
                }

                // The `in` keyword is a function call to `contains` applied to a primitive range. I.E., `IntRange`, `LongRange`.
                opPrefix = whitespace()
                kotlinBinaryType = if (skip("!")) K.Binary.Type.NotContains else K.Binary.Type.Contains
                skip("in")
                val rhs: FirExpression =
                    if (functionCall.explicitReceiver != null) functionCall.explicitReceiver!! else functionCall.dispatchReceiver
                right = convertToExpression(rhs, data)!!
            }

            "set" -> {
                // Note: the kotlin set function call is converted to a J.Assignment and may be an issue in Kotlin recipes in the future.
                left = convertToExpression(functionCall.explicitReceiver!!, data)!!
                left = J.ArrayAccess(
                    randomId(),
                    left.prefix,
                    Markers.EMPTY,
                    left.withPrefix(Space.EMPTY),
                    J.ArrayDimension(
                        randomId(),
                        sourceBefore("["),
                        Markers.EMPTY,
                        padRight(
                            convertToExpression(
                                functionCall.argumentList.arguments[0], data
                            )!!, sourceBefore("]")
                        )
                    ),
                    typeMapping.type(typeMapping.type(functionCall.argumentList.arguments[1]))
                )
                val before = whitespace()
                @Suppress("ControlFlowWithEmptyBody")
                if (skip("=")) {
                } else {
                    // Check for syntax de-sugaring.
                    throw UnsupportedOperationException("Unsupported set operator type.")
                }
                return J.Assignment(
                    randomId(),
                    prefix,
                    Markers.EMPTY,
                    left,
                    padLeft(
                        before, convertToExpression(
                            functionCall.argumentList.arguments[1], data
                        )!!
                    ),
                    typeMapping.type(functionCall.argumentList.arguments[1])
                )
            }

            "rangeUntil" -> {
                val lhs =
                    if (functionCall.explicitReceiver != null) functionCall.explicitReceiver else functionCall.dispatchReceiver
                left = convertToExpression(lhs as FirElement, data)!!
                opPrefix = sourceBefore("..<")
                kotlinBinaryType = K.Binary.Type.RangeUntil
                right = convertToExpression(functionCall.argumentList.arguments[0], data)!!
            }

            else -> {
                val lhs =
                    if (functionCall.explicitReceiver != null) functionCall.explicitReceiver else functionCall.dispatchReceiver
                left = convertToExpression(lhs as FirElement, data)!!
                opPrefix = sourceBefore("..")
                kotlinBinaryType = K.Binary.Type.RangeTo
                right = convertToExpression(functionCall.argumentList.arguments[0], data)!!
            }
        }
        return K.Binary(
            randomId(),
            prefix,
            Markers.EMPTY,
            left,
            padLeft(opPrefix, kotlinBinaryType),
            right,
            after,
            typeMapping.type(functionCall)
        )
    }

    private fun mapBinaryOperation(functionCall: FirFunctionCall): J {
        val prefix = whitespace()
        val receiver: FirElement =
            if (functionCall.explicitReceiver != null) functionCall.explicitReceiver!! else functionCall.dispatchReceiver
        val left =
            convertToExpression<Expression>(receiver, data)!!
        val opPrefix: Space
        val javaBinaryType: J.Binary.Type
        when (functionCall.calleeReference.name.asString()) {
            "div" -> {
                javaBinaryType = J.Binary.Type.Division
                opPrefix = sourceBefore("/")
            }

            "minus" -> {
                javaBinaryType = J.Binary.Type.Subtraction
                opPrefix = sourceBefore("-")
            }

            "plus" -> {
                javaBinaryType = J.Binary.Type.Addition
                opPrefix = sourceBefore("+")
            }

            "rem" -> {
                javaBinaryType = J.Binary.Type.Modulo
                opPrefix = sourceBefore("%")
            }

            "times" -> {
                javaBinaryType = J.Binary.Type.Multiplication
                opPrefix = sourceBefore("*")
            }

            else -> throw UnsupportedOperationException("Unsupported binary operator type.")
        }
        val right =
            convertToExpression<Expression>(functionCall.argumentList.arguments[0], data)!!
        return J.Binary(
            randomId(),
            prefix,
            Markers.EMPTY,
            left,
            padLeft(opPrefix, javaBinaryType),
            right,
            typeMapping.type(functionCall)
        )
    }
    private fun mapAugmentedAssign(functionCall: FirFunctionCall): J {
        val prefix = whitespace()
        val receiver: FirElement =
            if (functionCall.explicitReceiver != null) functionCall.explicitReceiver!! else functionCall.dispatchReceiver
        val left =
            convertToExpression<Expression>(receiver, data)!!
        val opPrefix: Space
        val javaBinaryType: J.AssignmentOperation.Type
        when (functionCall.calleeReference.name.asString()) {
            "divAssign" -> {
                javaBinaryType = J.AssignmentOperation.Type.Division
                opPrefix = sourceBefore("/=")
            }

            "minusAssign" -> {
                javaBinaryType = J.AssignmentOperation.Type.Subtraction
                opPrefix = sourceBefore("-=")
            }

            "plusAssign" -> {
                javaBinaryType = J.AssignmentOperation.Type.Addition
                opPrefix = sourceBefore("+=")
            }

            "remAssign" -> {
                javaBinaryType = J.AssignmentOperation.Type.Modulo
                opPrefix = sourceBefore("%=")
            }

            "timesAssign" -> {
                javaBinaryType = J.AssignmentOperation.Type.Multiplication
                opPrefix = sourceBefore("*=")
            }

            else -> throw UnsupportedOperationException("Unsupported assignment operator type.")
        }
        val right =
            convertToExpression<Expression>(functionCall.argumentList.arguments[0], data)!!
        return J.AssignmentOperation(
            randomId(),
            prefix,
            Markers.EMPTY,
            left,
            padLeft(opPrefix, javaBinaryType),
            right,
            typeMapping.type(functionCall)
        )
    }

    override fun visitFunctionTypeRef(functionTypeRef: FirFunctionTypeRef, data: ExecutionContext): J {
        val prefix = whitespace()
        if (functionTypeRef.isMarkedNullable) {
            skip("(")
            whitespace() // FIXME add to LST
        }
        var modifiers: List<J.Modifier> = ArrayList()
        val leadingAnnotations: MutableList<J.Annotation> = mutableListOf()
        val node = getRealPsiElement(functionTypeRef)
        val modifierList = getModifierList(node)
        if (modifierList != null) {
            modifiers = mapModifierList(
                modifierList,
                functionTypeRef.annotations,
                leadingAnnotations,
                mutableListOf()
            )
        }
        var receiver: JRightPadded<NameTree>? = null
        if (functionTypeRef.receiverTypeRef != null) {
            val receiverName = visitElement(functionTypeRef.receiverTypeRef!!, data) as NameTree
            receiver = JRightPadded.build(receiverName)
                .withAfter(whitespace())
            skip(".")
        }
        val before = sourceBefore("(")
        val refParams: MutableList<JRightPadded<out TypeTree>> = ArrayList(functionTypeRef.parameters.size)
        if (functionTypeRef.parameters.isNotEmpty()) {
            val parameters = functionTypeRef.parameters
            for (i in parameters.indices) {
                val p = parameters[i]
                val expr = visitElement(p, data) as K.FunctionType.Parameter?
                if (expr != null) {
                    var param: JRightPadded<out TypeTree>
                    if (i < parameters.size - 1) {
                        param = JRightPadded.build(expr).withAfter(whitespace())
                        skip(",")
                    } else {
                        val after = whitespace()
                        param = JRightPadded.build(expr).withAfter(after)
                        if (skip(",")) {
                            param = param.withMarkers(Markers.build(listOf(TrailingComma(randomId(), whitespace()))))
                        }
                    }
                    refParams.add(param)
                }
            }
            skip(")")
        } else {
            refParams +=
                    JRightPadded
                        .build(J.Empty(randomId(), Space.EMPTY, Markers.EMPTY))
                        .withAfter(sourceBefore(")"))
        }

        val arrow = sourceBefore("->")
        val returnType: TypeTree = visitElement(functionTypeRef.returnTypeRef, data) as TypeTree

        val nullablePrefix: Space?
        if (functionTypeRef.isMarkedNullable) {
            whitespace() // FIXME add to LST
            skip(")")
            nullablePrefix = whitespace()
            skip("?")
        } else {
            nullablePrefix = null
        }

        return K.FunctionType(
            randomId(),
            prefix,
            if (functionTypeRef.isMarkedNullable) Markers.EMPTY.addIfAbsent(IsNullable(randomId(), nullablePrefix!!)) else Markers.EMPTY,
            leadingAnnotations,
            modifiers,
            receiver,
            JContainer.build(before, refParams as List<JRightPadded<TypeTree>>, Markers.EMPTY),
            arrow,
            returnType
        )
    }

    override fun visitImport(import: FirImport, data: ExecutionContext): J {
        val prefix = sourceBefore("import")
        val hasParentClassId = (import is FirResolvedImport && import.resolvedParentClassId != null)
        val static = padLeft(Space.EMPTY, hasParentClassId)
        val space = whitespace()
        val importName: String =
            if (import.importedFqName == null) {
                ""
            } else {
                val importNodes: List<LighterASTNode> =
                    import.source!!.lighterASTNode.getChildren(import.source!!.treeStructure)
                // KtStubElementTypes.DOT_QUALIFIED_EXPRESSION
                val importNameNode = importNodes[2]
                source.substring(
                    importNameNode.startOffset,
                    if (import.isAllUnder) importNodes[importNodes.size - 1].endOffset else importNameNode.endOffset
                )
            }
        val qualid = if (importName.contains(".")) (build(importName) as J)
            .withPrefix(space) else
            // Kotlin allows methods to be imported directly, so we need to create a fake field access to fit into J.Import.
            J.FieldAccess(
                randomId(),
                Space.EMPTY,
                Markers.EMPTY,
                J.Empty(randomId(), Space.EMPTY, Markers.EMPTY),
                padLeft(Space.EMPTY, createIdentifier(importName).withPrefix(space)),
                null
            )
        skip(importName)
        var alias: JLeftPadded<J.Identifier>? = null
        if (import.aliasName != null) {
            val asPrefix = sourceBefore("as")
            val aliasText = import.aliasName!!.asString()
            skip(aliasText)
            // FirImport does not contain type attribution information, so we cannot use the type mapping here.
            val aliasId = createIdentifier(aliasText)
            alias = padLeft(asPrefix, aliasId)
            aliasImportMap[importName] = aliasText
        }

        return J.Import(
            randomId(),
            prefix,
            Markers.EMPTY,
            static,
            qualid,
            alias
        )
    }

    override fun visitPackageDirective(packageDirective: FirPackageDirective, data: ExecutionContext): J {
        val pkgPrefix = whitespace()
        skip("package")
        val pkgNamePrefix = whitespace()
        val packageNameNode =
            packageDirective.source!!.lighterASTNode.getChildren(packageDirective.source!!.treeStructure)[2]
        val packageName = source.substring(packageNameNode.startOffset, packageNameNode.endOffset)
        skip(packageName)
        return J.Package(
            randomId(),
            pkgPrefix,
            Markers.EMPTY,
            (build(packageName) as J).withPrefix(pkgNamePrefix),
            emptyList()
        )
    }

    override fun visitGetClassCall(getClassCall: FirGetClassCall, data: ExecutionContext): J {
        return J.MemberReference(
            randomId(),
            whitespace(),
            Markers.EMPTY,
            padRight(convertToExpression(getClassCall.argument, data)!!, sourceBefore("::")),
            null,
            padLeft(whitespace(), createIdentifier("class")),
            typeMapping.type(getClassCall),
            null,
            null
        )
    }

    override fun visitLabel(label: FirLabel, data: ExecutionContext): J {
        return J.Label(
            randomId(),
            whitespace(),
            Markers.EMPTY,
            padRight(
                createIdentifier(label.name),
                sourceBefore("@")
            ),
            // The label exists on the FIR statement, and needs to be set in the statements visit.
            J.Empty(randomId(), Space.EMPTY, Markers.EMPTY)
        )
    }

    override fun visitLambdaArgumentExpression(
        lambdaArgumentExpression: FirLambdaArgumentExpression,
        data: ExecutionContext
    ): J {
        val prefix = whitespace()
        val j: J = visitElement(lambdaArgumentExpression.expression, data)!!
        return j.withPrefix(prefix)
    }

    override fun visitNamedArgumentExpression(
        namedArgumentExpression: FirNamedArgumentExpression,
        data: ExecutionContext
    ): J {
        val prefix = whitespace()
        val name = createIdentifier(namedArgumentExpression.name.toString())
        val exprPrefix = sourceBefore("=")
        val expr =
            convertToExpression<Expression>(namedArgumentExpression.expression, data)!!
        return J.Assignment(
            randomId(),
            prefix,
            Markers.EMPTY,
            name,
            padLeft(exprPrefix, expr),
            typeMapping.type(namedArgumentExpression.typeRef)
        )
    }

    override fun visitProperty(property: FirProperty, data: ExecutionContext): J {
        val prefix = whitespace()
        var markers = Markers.EMPTY
        val node = getRealPsiElement(property)
        val modifierList = getModifierList(node)
        var modifiers: MutableList<J.Modifier> = ArrayList()
        val leadingAnnotations: MutableList<J.Annotation> = mutableListOf()
        val lastAnnotations: MutableList<J.Annotation> = mutableListOf()
        if (modifierList != null) {
            modifiers =
                mapModifierList(modifierList, collectFirAnnotations(property), leadingAnnotations, lastAnnotations)
        }
        val varOrVar = if (node == null) null else (node as KtValVarKeywordOwner).valOrVarKeyword
        if (varOrVar != null) {
            if ("val" == varOrVar.text) {
                modifiers.add(
                    J.Modifier(
                        randomId(),
                        sourceBefore("val"),
                        Markers.EMPTY,
                        null,
                        J.Modifier.Type.Final,
                        lastAnnotations
                    )
                )
            } else {
                modifiers.add(
                    J.Modifier(
                        randomId(),
                        sourceBefore("var"),
                        Markers.EMPTY,
                        "var",
                        J.Modifier.Type.LanguageExtension,
                        lastAnnotations
                    )
                )
            }
        }

        var typeParameters: JContainer<J.TypeParameter>? = null
        if (property.typeParameters.isNotEmpty()) {
            val before = sourceBefore("<")
            val params: MutableList<JRightPadded<J.TypeParameter>> = ArrayList(property.typeParameters.size)
            val parameters = property.typeParameters
            for (i in parameters.indices) {
                val typeParameter = parameters[i]
                val j: J = visitElement(typeParameter, data)!!
                params.add(
                    padRight(
                        j as J.TypeParameter,
                        if (i == parameters.size - 1) sourceBefore(">") else sourceBefore(",")
                    )
                )
            }
            typeParameters = JContainer.build(
                before,
                params,
                Markers.EMPTY
            )
        }

        var receiver: JRightPadded<Statement>? = null
        if (property.receiverParameter != null) {
            // Generates a VariableDeclaration to represent the receiver similar to how it is done in the Kotlin compiler.
            val receiverName = visitElement(property.receiverParameter!!, data) as TypeTree
            markers = markers.addIfAbsent(Extension(randomId()))
            receiver = padRight(
                J.VariableDeclarations(
                    randomId(),
                    Space.EMPTY,
                    Markers.EMPTY,
                    emptyList(),
                    emptyList(),
                    receiverName,
                    null,
                    emptyList(),
                    emptyList()
                ), sourceBefore(".")
            )
        }
        val variables: MutableList<JRightPadded<J.VariableDeclarations.NamedVariable>>
        var getter: J.MethodDeclaration? = null
        var setter: J.MethodDeclaration? = null
        var isSetterFirst = false
        var typeExpression: TypeTree? = null
        val namePrefix = whitespace()

        val propertyName = property.name.asString()
        val variableName = when (propertyName) {
            "<unused var>" -> "_"
            "<no name provided>" -> ""
            else -> propertyName
        }

        val name = createIdentifier(variableName, property)
        var initializer: JLeftPadded<Expression>? = null
        if (node != null) {
            var initMarkers = Markers.EMPTY
            if (property.delegate != null) {
                if (property.delegate is FirFunctionCall || property.delegate is FirPropertyAccessExpression || property.delegate is FirCallableReferenceAccess) {
                    val prev = PsiTreeUtil.skipWhitespacesAndCommentsBackward(getPsiElement(property.delegate))
                    val typeMarkersPair = mapTypeExpression(property, markers)
                    if (typeMarkersPair.first != null) {
                        typeExpression = typeMarkersPair.first
                        markers = typeMarkersPair.second
                    }

                    val before = if (prev != null) sourceBefore(prev.text) else Space.EMPTY
                    initMarkers = initMarkers.addIfAbsent(By(randomId()))
                    initializer = padLeft(before, convertToExpression(property.delegate!!, data)!!)
                } else {
                    throw UnsupportedOperationException(
                        generateUnsupportedMessage(
                            "Unexpected property delegation. FirProperty#delegate for name: " +
                                    (property.delegate as FirFunctionCall?)!!.calleeReference.name.asString()
                        )
                    )
                }
            } else {
                val typeMarkersPair = mapTypeExpression(property, markers)
                if (typeMarkersPair.first != null) {
                    typeExpression = typeMarkersPair.first
                    markers = typeMarkersPair.second
                }
            }
            var equals: PsiElement? = null
            var propertyNode: KtProperty? = null
            if (node is KtProperty) {
                propertyNode = node
                equals = propertyNode.equalsToken
            } else if (node is KtParameter) {
                equals = node.equalsToken
            }
            if (equals != null) {
                // If an equals token exists, then we have an initializer
                initializer = padLeft(sourceBefore("="), convertToExpression(property.initializer!!, data)!!)
            } else if (initMarkers.markers.isEmpty()) {
                initMarkers = initMarkers.addIfAbsent(OmitEquals(randomId()))
            }
            for (marker in initMarkers.markers) {
                markers = markers.addIfAbsent(marker)
            }
            val accessors = propertyNode?.accessors
            isSetterFirst = !accessors.isNullOrEmpty() && accessors[0].isSetter
            if (isSetterFirst) {
                if (isValidSetter(property.setter)) {
                    setter = visitElement(property.setter!!, data) as J.MethodDeclaration
                }
                if (isValidGetter(property.getter)) {
                    getter = visitElement(property.getter!!, data) as J.MethodDeclaration
                }
            } else {
                if (isValidGetter(property.getter)) {
                    getter = visitElement(property.getter!!, data) as J.MethodDeclaration
                }
                if (isValidSetter(property.setter)) {
                    setter = visitElement(property.setter!!, data) as J.MethodDeclaration
                }
            }
            if (receiver != null) {
                if (getter == null) {
                    getter = createImplicitMethodDeclaration("get")
                }
                getter = getter.padding.withParameters(getter.padding.parameters.padding.withElements(listOf(receiver)))
                if (setter == null) {
                    setter = createImplicitMethodDeclaration("set")
                }
                setter = setter.padding.withParameters(setter.padding.parameters.padding.withElements(listOf(receiver)))
            }
        }
        val namedVariable = maybeSemicolon(
            J.VariableDeclarations.NamedVariable(
                randomId(),
                namePrefix,
                Markers.EMPTY,
                name,
                emptyList(),
                initializer,
                typeMapping.variableType(property.symbol, null, getCurrentFile())
            )
        )
        variables = ArrayList(1)
        variables.add(namedVariable)
        val variableDeclarations = J.VariableDeclarations(
            randomId(),
            prefix,
            markers,
            leadingAnnotations,
            modifiers,
            typeExpression,
            null,
            emptyList(),
            variables
        )
        return if (getter == null && setter == null) variableDeclarations else K.Property(
            randomId(),
            variableDeclarations.prefix,
            Markers.EMPTY,
            typeParameters,
            variableDeclarations.withPrefix(Space.EMPTY),
            getter,
            setter,
            isSetterFirst
        )
    }

    private fun mapTypeExpression(property: FirProperty, markers: Markers) : Pair<TypeTree?, Markers> {
        var typeExpression : TypeTree? = null
        var updatedMarkers = markers
        if (property.returnTypeRef is FirResolvedTypeRef &&
                (property.returnTypeRef.source == null || property.returnTypeRef.source?.kind !is KtFakeSourceElementKind)
        ) {
            val typeRef = property.returnTypeRef as FirResolvedTypeRef
            if (typeRef.delegatedTypeRef != null) {
                val prev =
                        PsiTreeUtil.skipWhitespacesAndCommentsBackward(getRealPsiElement(typeRef.delegatedTypeRef))
                val addTypeReferencePrefix = prev is LeafPsiElement && prev.elementType === KtTokens.COLON
                if (addTypeReferencePrefix) {
                    updatedMarkers = markers.addIfAbsent(TypeReferencePrefix(randomId(), sourceBefore(":")))
                }
                val j: J = visitElement(typeRef, data)!!
                typeExpression = if (j is TypeTree) {
                    j
                } else {
                    throw IllegalStateException("Unexpected type expression: " + j.javaClass.name)
                }
            }
        }

        return Pair(typeExpression, updatedMarkers)
    }

    private fun isValidGetter(getter: FirPropertyAccessor?): Boolean {
        return getter != null && getter !is FirDefaultPropertyGetter &&
                (getter.source == null || getter.source!!.kind !is KtFakeSourceElementKind)
    }

    private fun isValidSetter(setter: FirPropertyAccessor?): Boolean {
        return setter != null && setter !is FirDefaultPropertySetter &&
                (setter.source == null || setter.source!!.kind !is KtFakeSourceElementKind)
    }

    private fun collectFirAnnotations(property: FirProperty): List<FirAnnotation> {
        val firAnnotations: MutableList<FirAnnotation> = ArrayList(property.annotations.size + 3)
        firAnnotations.addAll(property.annotations)
        if (property.getter != null) {
            firAnnotations.addAll(property.getter!!.annotations)
        }
        if (property.setter != null) {
            val setter = property.setter
            if (setter != null) {
                firAnnotations.addAll(setter.annotations)
                if (setter.valueParameters.isNotEmpty()) {
                    setter.valueParameters.forEach(
                        Consumer { vp: FirValueParameter -> firAnnotations.addAll(vp.annotations) }
                    )
                }
            }
        }
        if (property.backingField != null) {
            firAnnotations.addAll(property.backingField!!.annotations)
        }
        return firAnnotations
    }

    override fun visitPropertyAccessExpression(
        propertyAccessExpression: FirPropertyAccessExpression,
        data: ExecutionContext
    ): J {
        val type = typeMapping.type(propertyAccessExpression)
        return if (propertyAccessExpression.explicitReceiver != null) {
            val prefix = whitespace()
            val target = convertToExpression<Expression>(propertyAccessExpression.explicitReceiver!!, data)!!
            val before = whitespace()
            var markers = Markers.EMPTY
            @Suppress("ControlFlowWithEmptyBody")
            if (skip(".")) {
            } else if (skip("?.")) {
                markers = markers.addIfAbsent(IsNullSafe(randomId(), Space.EMPTY))
            }
            val name = padLeft(before, visitElement(propertyAccessExpression.calleeReference, data) as J.Identifier)
            J.FieldAccess(
                randomId(),
                prefix,
                markers,
                target,
                name,
                type
            )
        } else {
            visitElement(propertyAccessExpression.calleeReference, data)!!
        }
    }

    override fun visitPropertyAccessor(propertyAccessor: FirPropertyAccessor, data: ExecutionContext): J {
        if (propertyAccessor.isGetter || propertyAccessor.isSetter) {
            val prefix = whitespace()
            var markers = Markers.EMPTY
            val accessorNode = getRealPsiElement(propertyAccessor)
            val leadingAnnotations: MutableList<J.Annotation> = mutableListOf()
            var lastAnnotations: MutableList<J.Annotation>? = mutableListOf()
            var modifiers = emptyList<J.Modifier>()
            val modifierList = getModifierList(accessorNode)
            if (modifierList != null) {
                modifiers =
                    mapModifierList(modifierList, propertyAccessor.annotations, leadingAnnotations, lastAnnotations!!)
            }
            var typeParameters: J.TypeParameters? = null
            if (propertyAccessor.typeParameters.isNotEmpty()
            ) {
                val before = sourceBefore("<")
                val params: MutableList<JRightPadded<J.TypeParameter?>> =
                    ArrayList(propertyAccessor.typeParameters.size)
                val parameters = propertyAccessor.typeParameters
                for (i in parameters.indices) {
                    val typeParameter = parameters[i]
                    val j: J = visitElement(typeParameter, data)!!
                    params.add(
                        padRight(
                            j as J.TypeParameter,
                            if (i == parameters.size - 1) sourceBefore(">") else sourceBefore(",")
                        )
                    )
                }
                typeParameters = J.TypeParameters(
                    randomId(),
                    before,
                    Markers.EMPTY,
                    if (lastAnnotations!!.isEmpty()) emptyList() else lastAnnotations,
                    params
                )
                lastAnnotations = null
            }
            val methodName = if (propertyAccessor.isGetter) "get" else "set"
            val name = createIdentifier(methodName, propertyAccessor)
            val params: JContainer<Statement?>
            val before = sourceBefore("(")
            if (propertyAccessor.isGetter) {
                params = JContainer.build(
                    before,
                    listOf(padRight(J.Empty(randomId(), sourceBefore(")"), Markers.EMPTY), Space.EMPTY)),
                    Markers.EMPTY
                )
            } else {
                val parameters: MutableList<JRightPadded<Statement?>> = ArrayList(propertyAccessor.valueParameters.size)
                val valueParameters = propertyAccessor.valueParameters
                for (i in valueParameters.indices) {
                    var j: J = visitElement(valueParameters[i], data)!!
                    if (j is Expression && j !is Statement) {
                        j = K.ExpressionStatement(randomId(), j)
                    }
                    if (i == valueParameters.size - 1) {
                        parameters.add(padRight(j as Statement, sourceBefore(")")))
                    } else {
                        parameters.add(padRight(j as Statement, sourceBefore(",")))
                    }
                }
                params = JContainer.build(before, parameters, Markers.EMPTY)
            }
            var saveCursor = cursor
            val nextPrefix = whitespace()
            var returnTypeExpression: TypeTree? = null
            // Only add the type reference if it exists in source code.
            if (propertyAccessor.returnTypeRef !is FirImplicitUnitTypeRef && skip(":")) {
                markers = markers.addIfAbsent(TypeReferencePrefix(randomId(), nextPrefix))
                returnTypeExpression = visitElement(propertyAccessor.returnTypeRef, data) as TypeTree?
            } else {
                cursor(saveCursor)
            }
            val body = mapFunctionBody(propertyAccessor, data)
            return J.MethodDeclaration(
                randomId(),
                prefix,
                markers,
                if (leadingAnnotations.isEmpty()) emptyList() else leadingAnnotations,
                modifiers,
                typeParameters,
                returnTypeExpression,
                J.MethodDeclaration.IdentifierWithAnnotations(
                    name,
                    lastAnnotations ?: emptyList()
                ),
                params,
                null,
                body,
                null,
                typeMapping.methodDeclarationType(propertyAccessor, null, getCurrentFile())
            )
        }
        throw UnsupportedOperationException("Unsupported property accessor.")
    }

    override fun visitReceiverParameter(receiverParameter: FirReceiverParameter, data: ExecutionContext): J {
        val annotations = mapAnnotations(receiverParameter.annotations)
        var j = visitElement(receiverParameter.typeRef, data)!!
        if (j is J.Identifier) {
            j = j.withAnnotations(ListUtils.concatAll(annotations, j.annotations))
        } else if (j is J.ParameterizedType) {
            if (j.clazz is J.Identifier) {
                j = j.withClazz(
                    (j.clazz as J.Identifier).withAnnotations(
                        ListUtils.concatAll(
                            annotations,
                            (j.clazz as J.Identifier).annotations
                        )
                    )
                )
            }
        }
        return j
    }

    override fun visitResolvedNamedReference(
        resolvedNamedReference: FirResolvedNamedReference,
        data: ExecutionContext
    ): J {
        val name = resolvedNamedReference.name.asString()
        return createIdentifier(name, resolvedNamedReference)
    }

    override fun visitReturnExpression(returnExpression: FirReturnExpression, data: ExecutionContext): J {
        var label: J.Identifier? = null
        val node = getRealPsiElement(returnExpression) as KtReturnExpression?
        val explicitReturn = node != null
        var prefix = Space.EMPTY
        if (explicitReturn) {
            prefix = sourceBefore("return")
            if (node!!.labeledExpression != null) {
                skip("@")
                label = createIdentifier(returnExpression.target.labelName)
            }
        }
        var returnExpr: Expression? = null
        if (returnExpression.result !is FirUnitExpression) {
            returnExpr = convertToExpression(returnExpression.result, data)
        }

        val markers = if (explicitReturn) Markers.EMPTY else Markers.EMPTY.addIfAbsent(ImplicitReturn(randomId()))
        return K.KReturn(randomId(), J.Return(randomId(), prefix, markers, returnExpr), label)
    }

    override fun visitResolvedTypeRef(resolvedTypeRef: FirResolvedTypeRef, data: ExecutionContext): J {
        if (resolvedTypeRef.delegatedTypeRef != null) {
            val annotations: List<J.Annotation?>
            if (resolvedTypeRef.delegatedTypeRef!!.annotations.isEmpty()) {
                annotations = emptyList<J.Annotation>()
            } else {
                annotations = ArrayList(resolvedTypeRef.delegatedTypeRef!!.annotations.size)
                for (annotation in resolvedTypeRef.delegatedTypeRef!!.annotations) {
                    annotations.add(visitElement(annotation, data) as J.Annotation?)
                }
            }
            var j = visitElement(resolvedTypeRef.delegatedTypeRef!!, data)
            val type = typeMapping.type(resolvedTypeRef)
            if (j is TypeTree) {
                j = j.withType(type)
            }
            if (j is J.Identifier) {
                j = j.withAnnotations(annotations)
            }
            if (j is J.ParameterizedType) {
                // The identifier on a parameterized type of the FIR does not contain type information and must be added separately.
                val parameterizedType = j
                j = parameterizedType.withClazz(parameterizedType.clazz.withType(type))
            }
            return j!!
        } else {
            // The type reference only exists in the source code if it is not a delegated type reference.
            // So, we use the name of the symbol to find the type reference in the source code.
            val symbol = resolvedTypeRef.type.toRegularClassSymbol(firSession)
            if (symbol != null) {
                val prefix = whitespace()
                val name = symbol.name.asString()
                val pos = source.indexOf(name, cursor)
                val fullName = source.substring(cursor, cursor + pos + name.length)
                cursor += fullName.length
                var typeTree: TypeTree = (build(fullName) as J).withPrefix(prefix)
                val saveCursor = cursor
                val nextPrefix = whitespace()
                if (skip("?")) {
                    typeTree = if (typeTree is J.FieldAccess) {
                        val fa = typeTree
                        fa.withName(
                            fa.name.withMarkers(
                                fa.name.markers.addIfAbsent(
                                    IsNullable(
                                        randomId(),
                                        nextPrefix
                                    )
                                )
                            )
                        )
                    } else {
                        typeTree.withMarkers(typeTree.markers.addIfAbsent(IsNullable(randomId(), nextPrefix)))
                    }
                } else {
                    cursor(saveCursor)
                }
                return typeTree.withType(typeMapping.type(resolvedTypeRef))
            }
        }
        throw UnsupportedOperationException("Unsupported null delegated type reference.")
    }

    @OptIn(SymbolInternals::class)
    override fun visitResolvedReifiedParameterReference(
        resolvedReifiedParameterReference: FirResolvedReifiedParameterReference,
        data: ExecutionContext
    ): J {
        return createIdentifier(
            resolvedReifiedParameterReference.symbol.fir.name.asString(),
            typeMapping.type(resolvedReifiedParameterReference),
            null
        )
    }

    override fun visitResolvedQualifier(resolvedQualifier: FirResolvedQualifier, data: ExecutionContext): J? {
        val fieldAccess = resolvedQualifier.packageFqName.asString()
        val resolvedName =
            if (resolvedQualifier.relativeClassFqName == null) "" else "." + resolvedQualifier.relativeClassFqName!!.asString()
        val fullName = fieldAccess + resolvedName
        val alias = aliasImportMap[fullName]

        val name = StringBuilder()
        if (alias != null && skip(alias)) {
            name.append(alias)
        } else {
            val split = fullName.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()
            for (i in split.indices) {
                val part = split[i]
                name.append(whitespace().whitespace)
                if (skip(part)) {
                    name.append(part)
                }
                if (i < split.size - 1) {
                    name.append(whitespace().whitespace)
                    if (skip(".")) {
                        name.append(".")
                    }
                }
            }
        }

        if (name.isEmpty())
            return null

        var typeTree: TypeTree = build(name.toString())
        if (resolvedQualifier.relativeClassFqName != null) {
            typeTree = typeTree.withType(typeMapping.type(resolvedQualifier))
        }
        if (resolvedQualifier.typeArguments.isNotEmpty()) {
            val typeArgs = mapTypeArguments(resolvedQualifier.typeArguments, data)
            typeTree = J.ParameterizedType(
                randomId(),
                Space.EMPTY,
                Markers.EMPTY,
                typeTree,
                typeArgs,
                typeMapping.type(resolvedQualifier)
            )
        }
        return typeTree
    }

    override fun visitSafeCallExpression(safeCallExpression: FirSafeCallExpression, data: ExecutionContext): J {
        return visitElement(safeCallExpression.selector, data)!!
    }

    override fun visitCheckedSafeCallSubject(
        checkedSafeCallSubject: FirCheckedSafeCallSubject,
        data: ExecutionContext
    ): J {
        return visitElement(checkedSafeCallSubject.originalReceiverRef.value, data)!!
    }

    override fun visitSimpleFunction(simpleFunction: FirSimpleFunction, data: ExecutionContext): J {
        val prefix = whitespace()
        var markers = Markers.EMPTY
        val functionNode = getRealPsiElement(simpleFunction)
        var modifiers: MutableList<J.Modifier> = ArrayList(1)
        val leadingAnnotations: MutableList<J.Annotation> = mutableListOf()
        val lastAnnotations: MutableList<J.Annotation> = mutableListOf()
        val modifierList = getModifierList(functionNode)
        if (modifierList != null) {
            modifiers = mapModifierList(modifierList, simpleFunction.annotations, leadingAnnotations, lastAnnotations)
        }
        var isOpen = false
        for (modifier in modifiers) {
            if (modifier.type == J.Modifier.Type.LanguageExtension && "open" == modifier.keyword) {
                isOpen = true
                break
            }
        }
        if (!isOpen) {
            modifiers.add(
                J.Modifier(
                    randomId(),
                    Space.EMPTY,
                    Markers.EMPTY,
                    null,
                    J.Modifier.Type.Final,
                    emptyList()
                )
            )
        }
        modifiers.add(
            J.Modifier(
                randomId(),
                sourceBefore("fun"),
                Markers.EMPTY,
                "fun",
                J.Modifier.Type.LanguageExtension,
                lastAnnotations
            )
        )
        var typeParameters: J.TypeParameters? = null
        if (simpleFunction.typeParameters.isNotEmpty()) {
            val before = sourceBefore("<")
            val params: MutableList<JRightPadded<J.TypeParameter?>> = ArrayList(simpleFunction.typeParameters.size)
            val parameters = simpleFunction.typeParameters
            for (i in parameters.indices) {
                val typeParameter = parameters[i]
                val j: J = visitElement(typeParameter, data)!!
                params.add(
                    padRight(
                        j as J.TypeParameter,
                        if (i == parameters.size - 1) sourceBefore(">") else sourceBefore(",")
                    )
                )
            }
            typeParameters = J.TypeParameters(
                randomId(),
                before,
                Markers.EMPTY,
                emptyList(),
                params
            )
        }
        var infixReceiver: JRightPadded<J.VariableDeclarations.NamedVariable>? = null
        if (simpleFunction.receiverParameter != null) {
            // Infix functions are de-sugared during the backend phase of the compiler.
            // The de-sugaring process moves the infix receiver to the first position of the method declaration.
            // The infix receiver is added as to the `J.MethodInvocation` parameters, and marked to distinguish the parameter.
            markers = markers.addIfAbsent(Extension(randomId()))
            val receiver =
                convertToExpression<Expression>(simpleFunction.receiverParameter!!, data)!!
            infixReceiver = JRightPadded.build(
                J.VariableDeclarations.NamedVariable(
                    randomId(),
                    Space.EMPTY,
                    Markers.EMPTY.addIfAbsent(Extension(randomId())),
                    J.Identifier(
                        randomId(),
                        Space.EMPTY,
                        Markers.EMPTY,
                        emptyList(),
                        "<receiverType>",
                        null,
                        null
                    ),
                    emptyList(),
                    padLeft(Space.EMPTY, receiver),
                    null
                )
            )
                .withAfter(sourceBefore("."))
        }
        val methodName: String = if ("<no name provided>" == simpleFunction.name.asString()) {
            // Extract name from source.
            throw IllegalStateException("Unresolved function.")
        } else {
            simpleFunction.name.asString()
        }
        val name = createIdentifier(methodName, simpleFunction)
        var before = sourceBefore("(")
        var params = if (simpleFunction.valueParameters.isNotEmpty()) JContainer.build(
            before,
            convertAll<Statement>(simpleFunction.valueParameters, ",", ")", data),
            Markers.EMPTY
        ) else JContainer.build(
            before, listOf(
                padRight<Statement>(
                    J.Empty(randomId(), sourceBefore(")"), Markers.EMPTY), Space.EMPTY
                )
            ), Markers.EMPTY
        )
        if (simpleFunction.receiverParameter != null) {
            // Insert the infix receiver to the list of parameters.
            var implicitParam = J.VariableDeclarations(
                randomId(),
                Space.EMPTY,
                Markers.EMPTY.addIfAbsent(Extension(randomId())),
                emptyList(),
                emptyList(),
                null,
                null,
                emptyList(),
                listOf(infixReceiver)
            )
            implicitParam = implicitParam.withMarkers(
                implicitParam.markers.addIfAbsent(
                    TypeReferencePrefix(
                        randomId(),
                        Space.EMPTY
                    )
                )
            )
            val newStatements: MutableList<JRightPadded<Statement>> = ArrayList(params.elements.size + 1)
            newStatements.add(JRightPadded.build(implicitParam))
            newStatements.addAll(params.padding.elements)
            params = params.padding.withElements(newStatements)
        }
        var saveCursor = cursor
        var returnTypeExpression: TypeTree? = null
        before = whitespace()
        if (skip(":")) {
            markers = markers.addIfAbsent(TypeReferencePrefix(randomId(), before))
            returnTypeExpression = visitElement(simpleFunction.returnTypeRef, data) as TypeTree?
            saveCursor = cursor
            before = whitespace()
            if (source[cursor] == '?') {
                returnTypeExpression = returnTypeExpression!!.withMarkers(
                    returnTypeExpression.markers.addIfAbsent(IsNullable(randomId(), before))
                )
            } else {
                cursor(saveCursor)
            }
        } else {
            cursor(saveCursor)
        }
        val body = mapFunctionBody(simpleFunction, data)

        return J.MethodDeclaration(
            randomId(),
            prefix,
            markers,
            if (leadingAnnotations.isEmpty()) emptyList() else leadingAnnotations,
            if (modifiers.isEmpty()) emptyList() else modifiers,
            typeParameters,
            returnTypeExpression,
            J.MethodDeclaration.IdentifierWithAnnotations(name, emptyList()),
            params,
            null,
            body,
            null,
            typeMapping.methodDeclarationType(simpleFunction, null, getCurrentFile())
        )
    }

    override fun visitSmartCastExpression(smartCastExpression: FirSmartCastExpression, data: ExecutionContext): J {
        return visitElement(smartCastExpression.originalExpression, data)!!
    }

    override fun visitStarProjection(starProjection: FirStarProjection, data: ExecutionContext): J {
        val prefix = whitespace()
        skip("*")
        return J.Wildcard(randomId(), prefix, Markers.EMPTY, null, null)
    }

    override fun visitStringConcatenationCall(
        stringConcatenationCall: FirStringConcatenationCall,
        data: ExecutionContext
    ): J {
        val prefix = whitespace()
        val delimiter: String = if (source.startsWith("\"\"\"", cursor)) {
            "\"\"\""
        } else if (source[cursor] == '$') {
            "$"
        } else {
            "\""
        }
        cursor += delimiter.length
        val values: MutableList<J?> = ArrayList(stringConcatenationCall.argumentList.arguments.size)
        val arguments = stringConcatenationCall.argumentList.arguments
        var i = 0
        while (i < arguments.size) {
            val e = arguments[i]
            val savedCursor = cursor
            val before = whitespace()
            val isDollar = e is FirConstExpression<*> && e.value == "$"
            if (cursor < e.source!!.endOffset && !isDollar && skip("$")) {
                val inBraces = skip("{")
                values.add(
                    K.KString.Value(
                        randomId(),
                        before,
                        Markers.EMPTY,
                        visitElement(e, data)!!,
                        if (inBraces) sourceBefore("}") else Space.EMPTY,
                        inBraces
                    )
                )
            } else {
                cursor = savedCursor
                values.add(visitElement(e, data))
            }
            i++
        }
        cursor += delimiter.length
        return K.KString(
            randomId(),
            prefix,
            Markers.EMPTY,
            delimiter,
            values,
            typeMapping.type(stringConcatenationCall)
        )
    }

    @OptIn(SymbolInternals::class)
    override fun visitThisReceiverExpression(
        thisReceiverExpression: FirThisReceiverExpression,
        data: ExecutionContext
    ): J {
        if (thisReceiverExpression.isImplicit) {
            return J.Empty(randomId(), Space.EMPTY, Markers.EMPTY)
        }
        val prefix = sourceBefore("this")
        var label: J.Identifier? = null
        if (thisReceiverExpression.calleeReference.labelName != null) {
            skip("@")
            label = createIdentifier(
                thisReceiverExpression.calleeReference.labelName!!,
                thisReceiverExpression.calleeReference.boundSymbol!!.fir
            )
        }
        return K.KThis(
            randomId(),
            prefix,
            Markers.EMPTY,
            label,
            typeMapping.type(thisReceiverExpression)
        )
    }

    override fun visitTypeAlias(typeAlias: FirTypeAlias, data: ExecutionContext): J {
        val prefix = whitespace()
        val markers = Markers.EMPTY
        val aliasNode = getRealPsiElement(typeAlias)
        var modifiers: MutableList<J.Modifier> = ArrayList()
        val leadingAnnotations: MutableList<J.Annotation> = mutableListOf()
        val lastAnnotations: MutableList<J.Annotation> = mutableListOf()
        val modifierList = getModifierList(aliasNode)
        if (modifierList != null) {
            modifiers = mapModifierList(modifierList, typeAlias.annotations, leadingAnnotations, lastAnnotations)
        }
        modifiers.add(
            J.Modifier(
                randomId(),
                sourceBefore("typealias"),
                markers,
                "typealias",
                J.Modifier.Type.LanguageExtension,
                lastAnnotations
            )
        )
        val name = createIdentifier(typeAlias.name.asString(), typeMapping.type(typeAlias.expandedTypeRef), null)
        val typeExpression: TypeTree = if (typeAlias.typeParameters.isEmpty()) name else J.ParameterizedType(
            randomId(),
            name.prefix,
            Markers.EMPTY,
            name.withPrefix(Space.EMPTY),
            JContainer.build(
                sourceBefore("<"),
                convertAllToExpressions(typeAlias.typeParameters, ",", ">", data),
                Markers.EMPTY
            ),
            name.type
        )
        val initializerPrefix = sourceBefore("=")
        val expr = convertToExpression<Expression>(typeAlias.expandedTypeRef, data)!!
        val namedVariable = padRight(
            J.VariableDeclarations.NamedVariable(
                randomId(),
                Space.EMPTY,
                Markers.EMPTY,  // typealias does not have a name.
                J.Identifier(
                    randomId(),
                    Space.EMPTY,
                    Markers.EMPTY,
                    emptyList(),
                    "",
                    null,
                    null
                ),
                emptyList(),
                padLeft(initializerPrefix, expr),
                null
            ), Space.EMPTY
        )
        val vars: MutableList<JRightPadded<J.VariableDeclarations.NamedVariable>> = ArrayList(1)
        vars.add(namedVariable)
        return J.VariableDeclarations(
            randomId(),
            prefix,
            markers,
            if (leadingAnnotations.isEmpty()) emptyList() else leadingAnnotations,
            modifiers,
            typeExpression,
            null,
            emptyList(),
            vars
        )
    }

    override fun visitTypeOperatorCall(typeOperatorCall: FirTypeOperatorCall, data: ExecutionContext): J {
        val prefix = whitespace()
        val expression = typeOperatorCall.argumentList.arguments[0]
        var markers = Markers.EMPTY
        // A when subject expression does not have a target because it's implicit
        val element: Expression = if (expression is FirWhenSubjectExpression) {
            J.Empty(randomId(), Space.EMPTY, Markers.EMPTY)
        } else {
            val target: FirElement =
                if (expression is FirSmartCastExpression) expression.originalExpression else expression
            convertToExpression(target, data)!!
        }
        val after: Space
        when (typeOperatorCall.operation) {
            FirOperation.IS -> after = sourceBefore("is")
            FirOperation.NOT_IS -> {
                after = sourceBefore("!is")
                markers = markers.addIfAbsent(NotIs(randomId()))
            }

            FirOperation.AS -> after = sourceBefore("as")
            FirOperation.SAFE_AS -> {
                after = sourceBefore("as?")
                markers = markers.addIfAbsent(IsNullSafe(randomId(), Space.EMPTY))
            }

            else -> throw UnsupportedOperationException("Unsupported type operator " + typeOperatorCall.operation.name)
        }
        return if (typeOperatorCall.operation == FirOperation.AS || typeOperatorCall.operation == FirOperation.SAFE_AS) {
            J.TypeCast(
                randomId(),
                prefix,
                markers,
                J.ControlParentheses(
                    randomId(),
                    after,
                    Markers.EMPTY,
                    JRightPadded.build(visitElement(typeOperatorCall.conversionTypeRef, data) as TypeTree)
                ),
                element
            )
        } else {
            val expr = JRightPadded.build(element).withAfter(after)
            val clazz: J = visitElement(typeOperatorCall.conversionTypeRef, data)!!
            J.InstanceOf(
                randomId(),
                prefix,
                markers,
                expr,
                clazz,
                null,
                typeMapping.type(typeOperatorCall)
            )
        }
    }

    override fun visitTypeParameter(typeParameter: FirTypeParameter, data: ExecutionContext): J {
        val prefix = whitespace()
        var markers = Markers.EMPTY
        val annotations: MutableList<J.Annotation?> = ArrayList(typeParameter.annotations.size)
        for (annotation in typeParameter.annotations) {
            annotations.add(visitElement(annotation, data) as J.Annotation?)
        }
        if (typeParameter.isReified) {
            // Add reified as an annotation to preserve whitespace.
            val name = J.Identifier(
                randomId(),
                Space.EMPTY,
                Markers.EMPTY,
                emptyList(),
                "reified",
                null,
                null
            )
            val reified = J.Annotation(
                randomId(), sourceBefore("reified"), Markers.EMPTY.addIfAbsent(
                    Modifier(randomId())
                ), name, JContainer.empty()
            )
            annotations.add(reified)
        }
        val nonImplicitParams: MutableList<FirTypeRef> = ArrayList(typeParameter.bounds.size)
        for (bound in typeParameter.bounds) {
            if (bound !is FirImplicitNullableAnyTypeRef) {
                nonImplicitParams.add(bound)
            }
        }
        val variance = typeParameter.variance
        val name: Expression
        var bounds: JContainer<TypeTree>? = null
        if (variance == Variance.IN_VARIANCE) {
            markers = markers.addIfAbsent(GenericType(randomId(), GenericType.Variance.CONTRAVARIANT))
            name = J.Identifier(
                randomId(),
                Space.EMPTY,
                Markers.build(listOf(Implicit(randomId()))),
                emptyList(),
                "Any",
                null,
                null
            )
            bounds = JContainer.build(
                sourceBefore("in"),
                listOf(padRight(createIdentifier(typeParameter.name.asString(), typeParameter), Space.EMPTY)),
                Markers.EMPTY
            )
        } else if (variance == Variance.OUT_VARIANCE) {
            markers = markers.addIfAbsent(GenericType(randomId(), GenericType.Variance.COVARIANT))
            name = J.Identifier(
                randomId(),
                Space.EMPTY,
                Markers.build(listOf(Implicit(randomId()))),
                emptyList(),
                "Any",
                null,
                null
            )
            bounds = JContainer.build(
                sourceBefore("out"),
                listOf(padRight(createIdentifier(typeParameter.name.asString(), typeParameter), Space.EMPTY)),
                Markers.EMPTY
            )
        } else {
            name = createIdentifier(typeParameter.name.asString(), typeParameter)
            if (nonImplicitParams.size == 1) {
                val saveCursor = cursor
                whitespace()
                if (source[cursor] == ':') {
                    cursor(saveCursor)
                    bounds = JContainer.build(
                        sourceBefore(":"), listOf(
                            padRight(
                                visitElement(
                                    nonImplicitParams[0], data
                                ) as TypeTree, Space.EMPTY
                            )
                        ), Markers.EMPTY
                    )
                } else {
                    cursor(saveCursor)
                }
            }
        }
        return J.TypeParameter(
            randomId(),
            prefix,
            markers,
            annotations,
            name,
            bounds
        )
    }

    override fun visitTryExpression(tryExpression: FirTryExpression, data: ExecutionContext): J {
        val prefix = whitespace()
        skip("try")
        val block = visitElement(tryExpression.tryBlock, data) as J.Block
        val catches: MutableList<J.Try.Catch?> = ArrayList(tryExpression.catches.size)
        for (aCatch in tryExpression.catches) {
            catches.add(visitElement(aCatch, data) as J.Try.Catch?)
        }
        val finally: JLeftPadded<J.Block>? = if (tryExpression.finallyBlock == null) null else padLeft(
            sourceBefore("finally"),
            visitElement(tryExpression.finallyBlock!!, data) as J.Block
        )
        return J.Try(
            randomId(),
            prefix,
            Markers.EMPTY,
            null,
            block,
            catches,
            finally
        )
    }

    override fun visitTypeProjectionWithVariance(
        typeProjectionWithVariance: FirTypeProjectionWithVariance,
        data: ExecutionContext
    ): J {
        var markers = Markers.EMPTY
        var bounds: JContainer<TypeTree>? = null
        var name: Expression? = null
        when (typeProjectionWithVariance.variance) {
            Variance.IN_VARIANCE -> {
                markers = markers.addIfAbsent(GenericType(randomId(), GenericType.Variance.CONTRAVARIANT))

                bounds = JContainer.build(
                    sourceBefore("in"),
                    listOf(padRight(visitResolvedTypeRef(typeProjectionWithVariance.typeRef as FirResolvedTypeRef, data) as TypeTree, Space.EMPTY)),
                    Markers.EMPTY
                )
            }
            Variance.OUT_VARIANCE -> {
                markers = markers.addIfAbsent(GenericType(randomId(), GenericType.Variance.COVARIANT))

                bounds = JContainer.build(
                    sourceBefore("out"),
                    listOf(padRight(visitResolvedTypeRef(typeProjectionWithVariance.typeRef as FirResolvedTypeRef, data) as TypeTree, Space.EMPTY)),
                    Markers.EMPTY
                )
            }
            else -> {
                name = visitResolvedTypeRef(typeProjectionWithVariance.typeRef as FirResolvedTypeRef, data) as Expression?
            }
        }

        return name
            ?: K.TypeParameterExpression(randomId(), J.TypeParameter(
                randomId(),
                Space.EMPTY,
                markers,
                emptyList(),
                J.Identifier(
                    randomId(),
                    Space.EMPTY,
                    Markers.build(listOf(Implicit(randomId()))),
                    emptyList(),
                    "Any",
                    null,
                    null
                ),
                bounds
            ))
    }

    override fun visitUserTypeRef(userTypeRef: FirUserTypeRef, data: ExecutionContext): J {
        val prefix = whitespace()
        var markers = Markers.EMPTY
        val name = StringBuilder()
        val qualifier = userTypeRef.qualifier
        for (i in qualifier.indices) {
            val part = qualifier[i]
            val whitespace = whitespace()
            name.append(whitespace.whitespace)
            name.append(part.name.asString())
            skip(part.name.asString())
            if (i < qualifier.size - 1) {
                require(part.typeArgumentList.typeArguments.isEmpty()) { "Unsupported type parameters in user part " + part.name }
                name.append(whitespace().whitespace)
                name.append(".")
                skip(".")
            }
        }
        val nameTree: NameTree = build(name.toString())
        val part = userTypeRef.qualifier[userTypeRef.qualifier.size - 1]
        return if (part.typeArgumentList.typeArguments.isNotEmpty()) {
            val typeArgPrefix = sourceBefore("<")
            val parameters: MutableList<JRightPadded<Expression>> = ArrayList(part.typeArgumentList.typeArguments.size)
            val typeArguments = part.typeArgumentList.typeArguments
            for (i in typeArguments.indices) {
                val typeArgument = typeArguments[i]
                parameters.add(
                    JRightPadded.build(convertToExpression<J>(typeArgument, data) as Expression)
                        .withAfter(
                            if (i < typeArguments.size - 1) sourceBefore(",") else sourceBefore(">")
                        )
                )
            }
            if (userTypeRef.isMarkedNullable) {
                markers = markers.addIfAbsent(IsNullable(randomId(), sourceBefore("?")))
            }
            J.ParameterizedType(
                randomId(),
                prefix,
                markers,
                nameTree,
                JContainer.build(typeArgPrefix, parameters, Markers.EMPTY),
                typeMapping.type(userTypeRef)
            )
        } else {
            if (userTypeRef.isMarkedNullable) {
                markers = markers.addIfAbsent(IsNullable(randomId(), sourceBefore("?")))
            }
            nameTree.withPrefix<J>(prefix)
                .withMarkers(markers)
        }
    }

    override fun visitValueParameter(valueParameter: FirValueParameter, data: ExecutionContext): J {
        val prefix = whitespace()
        var markers = Markers.EMPTY
        val range = TextRange(valueParameter.source!!.startOffset, valueParameter.source!!.endOffset)
        val firAnnotations: MutableList<FirAnnotation> = mutableListOf()
        firAnnotations.addAll(valueParameter.annotations)
        if (generatedFirProperties.containsKey(range)) {
            firAnnotations.addAll(collectFirAnnotations(generatedFirProperties[range]!!))
        }
        var modifiers: MutableList<J.Modifier> = ArrayList()
        val leadingAnnotations: MutableList<J.Annotation> = mutableListOf()
        var lastAnnotations: MutableList<J.Annotation>? = mutableListOf()
        val node = getRealPsiElement(valueParameter) as KtParameter?
        val modifierList = getModifierList(node)
        if (modifierList != null) {
            modifiers = mapModifierList(modifierList, firAnnotations, leadingAnnotations, lastAnnotations!!)
        }
        checkNotNull(node) { "No node found for $valueParameter" }
        val varOrVar = (node as KtValVarKeywordOwner).valOrVarKeyword
        if (varOrVar != null) {
            modifiers.add(mapToJModifier(varOrVar.text, lastAnnotations!!))
            lastAnnotations = null
        }
        var valueName = ""
        if ("<unused var>" == valueParameter.name.toString()) {
            valueName = "_"
        } else if ("<no name provided>" != valueParameter.name.toString()) {
            valueName = valueParameter.name.asString()
        }
        var name = createIdentifier(valueName, valueParameter)
        if (lastAnnotations != null) {
            name = name.withAnnotations(lastAnnotations)
        }
        var typeExpression: TypeTree? = null
        if (valueParameter.returnTypeRef is FirResolvedTypeRef && (valueParameter.returnTypeRef.source == null || valueParameter.returnTypeRef.source!!.kind !is KtFakeSourceElementKind)) {
            val typeRef = valueParameter.returnTypeRef as FirResolvedTypeRef
            if (typeRef.delegatedTypeRef != null) {
                val delimiterPrefix = whitespace()
                val addTypeReferencePrefix = skip(":")
                if (addTypeReferencePrefix) {
                    markers = markers.addIfAbsent(TypeReferencePrefix(randomId(), delimiterPrefix))
                }
                val j: J = visitElement(typeRef, data)!!
                typeExpression = if (j is TypeTree) {
                    j
                } else {
                    throw IllegalStateException("Unexpected type expression: " + j.javaClass.name)
                }
            } else if ("_" == valueName) {
                val savedCursor = cursor
                val delimiterPrefix = whitespace()
                if (skip(":")) {
                    markers = markers.addIfAbsent(TypeReferencePrefix(randomId(), delimiterPrefix))
                    val j: J = visitElement(typeRef, data)!!
                    typeExpression = if (j is TypeTree) {
                        j
                    } else {
                        throw IllegalStateException("Unexpected type expression: " + j.javaClass.name)
                    }
                } else {
                    cursor = savedCursor
                }
            }
        }
        val initializer =
            if (valueParameter.initializer != null) valueParameter.initializer else if (valueParameter.defaultValue != null) valueParameter.defaultValue else null
        val namedVariable = maybeSemicolon(
            J.VariableDeclarations.NamedVariable(
                randomId(),
                Space.EMPTY,
                Markers.EMPTY,
                name,
                emptyList(),
                if (initializer != null) padLeft(sourceBefore("="), convertToExpression(initializer, data)!!) else null,
                typeMapping.variableType(valueParameter.symbol, null, getCurrentFile())
            )
        )
        val vars: MutableList<JRightPadded<J.VariableDeclarations.NamedVariable>> = ArrayList(1)
        vars.add(namedVariable)
        return J.VariableDeclarations(
            randomId(),
            prefix,
            markers,
            if (leadingAnnotations.isEmpty()) emptyList() else leadingAnnotations,
            if (modifiers.isEmpty()) emptyList() else modifiers,
            typeExpression,
            null,
            emptyList(),
            vars
        )
    }

    override fun visitVariableAssignment(variableAssignment: FirVariableAssignment, data: ExecutionContext): J {
        val unaryAssignment =
            variableAssignment.rValue is FirFunctionCall && (variableAssignment.rValue as FirFunctionCall).origin == FirFunctionCallOrigin.Operator &&
                    (variableAssignment.rValue as FirFunctionCall).calleeReference is FirResolvedNamedReference &&
                    isUnaryOperation((variableAssignment.rValue as FirFunctionCall).calleeReference.name.asString())
        val node = getRealPsiElement(variableAssignment) as KtBinaryExpression?
        if (unaryAssignment && node == null) {
            return visitElement(variableAssignment.rValue, data)!!
        }
        val prefix = whitespace()
        val variable =
            if (variableAssignment.lValue is FirDesugaredAssignmentValueReferenceExpression) {
                convertToExpression(
                    (variableAssignment.lValue as FirDesugaredAssignmentValueReferenceExpression).expressionRef.value,
                    data
                )!!
            } else {
                convertToExpression<Expression>(variableAssignment.lValue, data)!!
            }
        val opText = node!!.operationReference.node.text
        val isCompoundAssignment = opText == "-=" || opText == "+=" || opText == "*=" || opText == "/="
        return if (isCompoundAssignment) {
            val opPrefix = whitespace()
            val op: J.AssignmentOperation.Type = when (opText) {
                "-=" -> {
                    skip("-=")
                    J.AssignmentOperation.Type.Subtraction
                }

                "+=" -> {
                    skip("+=")
                    J.AssignmentOperation.Type.Addition
                }

                "*=" -> {
                    skip("*=")
                    J.AssignmentOperation.Type.Multiplication
                }

                "/=" -> {
                    skip("/=")
                    J.AssignmentOperation.Type.Division
                }

                else -> throw IllegalArgumentException("Unexpected compound assignment.")
            }
            require(
                !(variableAssignment.rValue !is FirFunctionCall ||
                        (variableAssignment.rValue as FirFunctionCall).argumentList.arguments.size != 1)
            ) { "Unexpected compound assignment." }
            val rhs: FirElement = (variableAssignment.rValue as FirFunctionCall).argumentList.arguments[0]
            J.AssignmentOperation(
                randomId(),
                prefix,
                Markers.EMPTY,
                variable,
                padLeft(opPrefix, op),
                convertToExpression(rhs, data)!!,
                typeMapping.type(variableAssignment)
            )
        } else {
            val exprPrefix = sourceBefore("=")
            val expr =
                convertToExpression<Expression>(variableAssignment.rValue, data)!!
            J.Assignment(
                randomId(),
                prefix,
                Markers.EMPTY,
                variable,
                padLeft(exprPrefix, expr),
                typeMapping.type(variableAssignment)
            )
        }
    }

    override fun visitWhenBranch(whenBranch: FirWhenBranch, data: ExecutionContext): J {
        val prefix = whitespace()
        @Suppress("ControlFlowWithEmptyBody")
        if (skip("if")) {
        } else require(
            whenBranch.condition is FirElseIfTrueCondition ||
                    whenBranch.condition is FirEqualityOperatorCall
        ) { "Unsupported condition type." }
        val singleExpression = whenBranch.result is FirSingleExpressionBlock
        return if (whenBranch.condition is FirElseIfTrueCondition) {
            val result: FirElement =
                if (singleExpression) (whenBranch.result as FirSingleExpressionBlock).statement else whenBranch.result
            val j: J = visitElement(result, data)!!
            j.withPrefix(prefix)
        } else {
            val controlParentheses = mapControlParentheses(whenBranch.condition)
            val result: FirElement =
                if (singleExpression) (whenBranch.result as FirSingleExpressionBlock).statement else whenBranch.result
            var j: J = visitElement(result, data)!!
            if (j !is Statement && j is Expression) {
                j = K.ExpressionStatement(randomId(), j)
            }
            J.If(
                randomId(),
                prefix,
                Markers.EMPTY,
                controlParentheses,
                JRightPadded.build(j as Statement),
                null
            )
        }
    }

    override fun visitWhenExpression(whenExpression: FirWhenExpression, data: ExecutionContext): J {
        val saveCursor = cursor
        val prefix = whitespace()
        if (skip("when")) {
            // Create the entire when expression here to simplify visiting `WhenBranch`, since `if` and `when` share the same data structure.
            var controlParentheses: J.ControlParentheses<J>? = null
            if (whenExpression.subjectVariable != null) {
                controlParentheses = J.ControlParentheses(
                    randomId(),
                    sourceBefore("("),
                    Markers.EMPTY,
                    padRight(visitElement(whenExpression.subjectVariable!!, data)!!, sourceBefore(")"))
                )
            } else if (whenExpression.subject != null) {
                controlParentheses = J.ControlParentheses(
                    randomId(),
                    sourceBefore("("),
                    Markers.EMPTY,
                    padRight(convertToExpression(whenExpression.subject!!, data)!!, sourceBefore(")"))
                )
            }
            val bodyPrefix = sourceBefore("{")
            val statements: MutableList<JRightPadded<Statement>> = ArrayList(whenExpression.branches.size)
            for (whenBranch in whenExpression.branches) {
                val exprSize =
                    if (whenBranch.condition is FirEqualityOperatorCall) (whenBranch.condition as FirEqualityOperatorCall).argumentList.arguments.size - 1 else 1
                val expressions: MutableList<JRightPadded<Expression>> = ArrayList(exprSize)
                val branchPrefix = whitespace()
                if (whenBranch.condition is FirElseIfTrueCondition) {
                    expressions.add(padRight(createIdentifier("else"), sourceBefore("->")))
                } else if (whenBranch.condition is FirEqualityOperatorCall) {
                    val arguments: MutableList<FirExpression> =
                        ArrayList((whenBranch.condition as FirEqualityOperatorCall).argumentList.arguments.size)
                    for (argument in (whenBranch.condition as FirEqualityOperatorCall).argumentList.arguments) {
                        if (argument !is FirWhenSubjectExpression) {
                            arguments.add(argument)
                        }
                    }
                    if (arguments.size == 1) {
                        expressions.add(
                            padRight(
                                convertToExpression(
                                    arguments[0], data
                                )!!, sourceBefore("->")
                            )
                        )
                    } else {
                        val expr =
                            convertToExpression<Expression>(whenBranch.condition, data)!!
                        expressions.add(padRight(expr, sourceBefore("->")))
                    }
                } else {
                    val expr: Expression = convertToExpression(whenBranch.condition, data)!!
                    var padded = maybeTrailingComma(expr)
                    if (padded.markers.markers.isEmpty()) {
                        padded = padded.withAfter(sourceBefore("->"))
                    } else {
                        skip("->")
                    }
                    expressions.add(padded)
                }
                val expressionContainer = JContainer.build(Space.EMPTY, expressions, Markers.EMPTY)
                val body: J = visitElement(whenBranch.result, data)!!
                val branch = K.WhenBranch(
                    randomId(),
                    branchPrefix,
                    Markers.EMPTY,
                    expressionContainer,
                    padRight(body, Space.EMPTY)
                )
                statements.add(padRight(branch, Space.EMPTY))
            }
            val bodySuffix = sourceBefore("}")
            val body = J.Block(
                randomId(),
                bodyPrefix,
                Markers.EMPTY,
                JRightPadded(false, Space.EMPTY, Markers.EMPTY),
                statements,
                bodySuffix
            )
            return K.When(
                randomId(),
                prefix,
                Markers.EMPTY,
                controlParentheses,
                body,
                typeMapping.type(whenExpression)
            )
        }

        // Otherwise, create an if branch.
        cursor(saveCursor)
        val whenBranch = whenExpression.branches[0]
        val firstElement: J = visitElement(whenBranch, data)!!
        check(firstElement is J.If) { "First element of when expression was not an if." }
        var ifStatement = firstElement
        val elseClauses: MutableList<J> = ArrayList(whenExpression.branches.size - 1)
        val branches = whenExpression.branches
        for (i in 1 until branches.size) {
            val branch = branches[i]
            val elsePrefix = sourceBefore("else")
            var j = visitWhenBranch(branch, data)
            if (j !is Statement && j is Expression) {
                j = K.ExpressionStatement(randomId(), j)
            }
            val ifElse = J.If.Else(
                randomId(),
                elsePrefix,
                Markers.EMPTY,
                JRightPadded.build(j as Statement)
            )
            elseClauses.add(ifElse)
        }
        elseClauses.add(0, ifStatement)
        var ifElse: J.If.Else? = null
        for (i in elseClauses.indices.reversed()) {
            var j: J? = elseClauses[i]
            if (j is J.If.Else) {
                if (j.body is J.If) {
                    var addElse = j.body as J.If
                    addElse = addElse.withElsePart(ifElse)
                    j = j.withBody(addElse)
                }
                ifElse = j as J.If.Else?
            } else if (j is J.If) {
                ifStatement = j.withElsePart(ifElse)
            }
        }
        return ifStatement
    }

    override fun visitWhileLoop(whileLoop: FirWhileLoop, data: ExecutionContext): J {
        var label: J.Label? = null
        if (whileLoop.label != null) {
            label = visitElement(whileLoop.label!!, data) as J.Label?
        }
        val prefix = whitespace()
        skip("while")
        val controlParentheses = mapControlParentheses(whileLoop.condition)
        val body = visitElement(whileLoop.block, data) as Statement
        val statement = J.WhileLoop(
            randomId(),
            prefix,
            Markers.EMPTY,
            controlParentheses,
            JRightPadded.build(body)
        )
        return if (label != null) label.withStatement(statement) else statement
    }

    override fun visitArgumentList(argumentList: FirArgumentList, data: ExecutionContext): J {
        throw UnsupportedOperationException(generateUnsupportedMessage("FirArgumentList"))
    }

    override fun visitAugmentedArraySetCall(
        augmentedArraySetCall: FirAugmentedArraySetCall,
        data: ExecutionContext
    ): J {
        throw UnsupportedOperationException(generateUnsupportedMessage("FirAugmentedArraySetCall"))
    }

    override fun visitAssignmentOperatorStatement(
        assignmentOperatorStatement: FirAssignmentOperatorStatement,
        data: ExecutionContext
    ): J {
        throw UnsupportedOperationException(generateUnsupportedMessage("FirAssignmentOperatorStatement"))
    }

    override fun visitAnnotation(annotation: FirAnnotation, data: ExecutionContext): J {
        throw UnsupportedOperationException(generateUnsupportedMessage("FirAnnotation"))
    }

    override fun visitAnnotationContainer(annotationContainer: FirAnnotationContainer, data: ExecutionContext): J {
        throw UnsupportedOperationException(generateUnsupportedMessage("FirAnnotationContainer"))
    }

    override fun visitAnnotationArgumentMapping(
        annotationArgumentMapping: FirAnnotationArgumentMapping,
        data: ExecutionContext
    ): J {
        throw UnsupportedOperationException(generateUnsupportedMessage("FirAnnotationArgumentMapping"))
    }

    override fun visitBackingField(backingField: FirBackingField, data: ExecutionContext): J {
        throw UnsupportedOperationException(generateUnsupportedMessage("FirBackingField"))
    }

    override fun visitContextReceiver(contextReceiver: FirContextReceiver, data: ExecutionContext): J {
        throw UnsupportedOperationException(generateUnsupportedMessage("FirContextReceiver"))
    }

    override fun visitConstructor(constructor: FirConstructor, data: ExecutionContext): J {
        val prefix = whitespace()
        var markers = Markers.EMPTY
        var modifiers: MutableList<J.Modifier> = ArrayList()
        val leadingAnnotations: MutableList<J.Annotation> = mutableListOf()
        val lastAnnotations: MutableList<J.Annotation> = mutableListOf()
        val node = getRealPsiElement(constructor) as KtSecondaryConstructor?
        val modifierList = getModifierList(node)
        if (modifierList != null) {
            modifiers = mapModifierList(modifierList, constructor.annotations, leadingAnnotations, lastAnnotations)
        }
        modifiers.add(mapToJModifier("constructor", lastAnnotations))
        var infixReceiver: JRightPadded<J.VariableDeclarations.NamedVariable>? = null
        if (constructor.receiverParameter != null) {
            // Infix functions are de-sugared during the backend phase of the compiler.
            // The de-sugaring process moves the infix receiver to the first position of the method declaration.
            // The infix receiver is added as to the `J.MethodInvocation` parameters, and marked to distinguish the parameter.
            markers = markers.addIfAbsent(Extension(randomId()))
            val receiver =
                convertToExpression<Expression>(constructor.receiverParameter!!, data)!!
            infixReceiver = JRightPadded.build(
                J.VariableDeclarations.NamedVariable(
                    randomId(),
                    Space.EMPTY,
                    Markers.EMPTY.addIfAbsent(Extension(randomId())),
                    J.Identifier(
                        randomId(),
                        Space.EMPTY,
                        Markers.EMPTY,
                        emptyList(),
                        "<receiverType>",
                        null,
                        null
                    ),
                    emptyList(),
                    padLeft(Space.EMPTY, receiver),
                    null
                )
            )
                .withAfter(sourceBefore("."))
        }
        var saveCursor = cursor
        val name = createIdentifier(node!!.name, constructor).withMarkers(
            Markers.build(
                listOf(
                    Implicit(randomId())
                )
            )
        )
        cursor = saveCursor
        var params: JContainer<Statement>
        var before = sourceBefore("(")
        params = if (constructor.valueParameters.isNotEmpty()) JContainer.build(
            before,
            convertAll(
                constructor.valueParameters, ",", ")", data
            ),
            Markers.EMPTY
        ) else JContainer.build(
            before, listOf(
                padRight(
                    J.Empty(randomId(), sourceBefore(")"), Markers.EMPTY), Space.EMPTY
                )
            ), Markers.EMPTY
        )
        if (constructor.receiverParameter != null) {
            // Insert the infix receiver to the list of parameters.
            var implicitParam = J.VariableDeclarations(
                randomId(),
                Space.EMPTY,
                Markers.EMPTY.addIfAbsent(Extension(randomId())),
                emptyList(),
                emptyList(),
                null,
                null,
                emptyList(),
                listOf(infixReceiver)
            )
            implicitParam = implicitParam.withMarkers(
                implicitParam.markers.addIfAbsent(
                    TypeReferencePrefix(
                        randomId(),
                        Space.EMPTY
                    )
                )
            )
            val newStatements: MutableList<JRightPadded<Statement>> = ArrayList(params.elements.size + 1)
            newStatements.add(JRightPadded.build(implicitParam))
            newStatements.addAll(params.padding.elements)
            params = params.padding.withElements(newStatements)
        }

        saveCursor = cursor
        var delegationCall: J.MethodInvocation? = null
        before = whitespace()
        if (skip(":") && constructor.delegatedConstructor != null) {
            val thisPrefix = whitespace()
            // The delegate constructor call is de-sugared during the backend phase of the compiler.
            val delegateName =
                createIdentifier(if (constructor.delegatedConstructor!!.isThis) "this" else "super")
            val argsPrefix = whitespace()
            val args = mapFunctionalCallArguments(constructor.delegatedConstructor!!).withBefore(argsPrefix)
            delegationCall = J.MethodInvocation(
                randomId(),
                thisPrefix,
                Markers.EMPTY.addIfAbsent(ConstructorDelegation(randomId(), before)).addIfAbsent(Implicit(randomId())),
                null,
                null,
                delegateName,
                args,
                typeMapping.type(constructor) as JavaType.Method
            )
        } else {
            cursor(saveCursor)
        }

        var body: J.Block? = null
        saveCursor = cursor
        before = whitespace()
        if (constructor.body is FirSingleExpressionBlock) {
            if (skip("=")) {
                body = convertToBlock(constructor.body as FirSingleExpressionBlock, data).withPrefix(before)
            } else {
                throw IllegalStateException("Unexpected single block expression.")
            }
        } else if (constructor.body is FirBlock) {
            cursor(saveCursor)
            body = visitElement(constructor.body!!, data) as J.Block?
        } else if (constructor.body == null) {
            cursor(saveCursor)
        } else {
            throw IllegalStateException("Unexpected constructor body.")
        }

        if (delegationCall != null) {
            body = if (body == null) {
                J.Block(
                    randomId(),
                    Space.EMPTY,
                    Markers.EMPTY.addIfAbsent(OmitBraces(randomId())),
                    JRightPadded(false, Space.EMPTY, Markers.EMPTY),
                    listOf(JRightPadded.build(delegationCall)),
                    Space.EMPTY
                )
            } else {
                body.withStatements(ListUtils.insert(body.statements, delegationCall, 0))
            }
        }

        return J.MethodDeclaration(
            randomId(),
            prefix,
            markers,
            if (leadingAnnotations.isEmpty()) emptyList() else leadingAnnotations,
            modifiers,
            null,
            null,
            J.MethodDeclaration.IdentifierWithAnnotations(name, emptyList()),
            params,
            null,
            body,
            null,
            typeMapping.methodDeclarationType(constructor, null, getCurrentFile())
        )
    }

    override fun visitComponentCall(componentCall: FirComponentCall, data: ExecutionContext): J {
        return visitComponentCall(componentCall, data, false)
    }

    private fun visitComponentCall(
        componentCall: FirComponentCall,
        data: ExecutionContext,
        synthetic: Boolean
    ): J {
        val prefix: Space
        val receiver: JRightPadded<Expression>?
        val name: J.Identifier
        val type = typeMapping.methodInvocationType(componentCall, getCurrentFile())
        if (synthetic) {
            prefix = Space.build(" ", emptyList())
            receiver = null
            name = J.Identifier(
                randomId(),
                Space.EMPTY,
                Markers.EMPTY,
                emptyList(),
                componentCall.calleeReference.name.asString(),
                null,
                null
            )
        } else {
            prefix = whitespace()
            receiver = padRight(
                convertToExpression(
                    componentCall.explicitReceiver, data
                )!!, sourceBefore(".")
            )
            name = createIdentifier(componentCall.calleeReference.name.asString(), type, null)
        }
        return J.MethodInvocation(
            randomId(),
            prefix,
            Markers.EMPTY,
            receiver,
            null,
            name,
            JContainer.empty(),
            type
        )
    }

    override fun visitContractDescriptionOwner(
        contractDescriptionOwner: FirContractDescriptionOwner,
        data: ExecutionContext
    ): J {
        throw UnsupportedOperationException(generateUnsupportedMessage("FirContractDescriptionOwner"))
    }

    override fun visitContextReceiverArgumentListOwner(
        contextReceiverArgumentListOwner: FirContextReceiverArgumentListOwner,
        data: ExecutionContext
    ): J {
        throw UnsupportedOperationException(generateUnsupportedMessage("FirContextReceiverArgumentListOwner"))
    }

    override fun visitClassReferenceExpression(
        classReferenceExpression: FirClassReferenceExpression,
        data: ExecutionContext
    ): J {
        throw UnsupportedOperationException(generateUnsupportedMessage("FirClassReferenceExpression"))
    }

    override fun visitClassLikeDeclaration(classLikeDeclaration: FirClassLikeDeclaration, data: ExecutionContext): J {
        throw UnsupportedOperationException(generateUnsupportedMessage("FirClassLikeDeclaration"))
    }

    override fun visitCall(call: FirCall, data: ExecutionContext): J {
        throw UnsupportedOperationException(generateUnsupportedMessage("FirCall"))
    }

    override fun visitCallableDeclaration(callableDeclaration: FirCallableDeclaration, data: ExecutionContext): J {
        throw UnsupportedOperationException(generateUnsupportedMessage("FirCallableDeclaration"))
    }

    override fun visitDelegatedConstructorCall(
        delegatedConstructorCall: FirDelegatedConstructorCall,
        data: ExecutionContext
    ): J {
        throw UnsupportedOperationException(generateUnsupportedMessage("FirDelegatedConstructorCall"))
    }

    override fun visitDeclaration(declaration: FirDeclaration, data: ExecutionContext): J {
        throw UnsupportedOperationException(generateUnsupportedMessage("FirDeclaration"))
    }

    override fun visitDynamicTypeRef(dynamicTypeRef: FirDynamicTypeRef, data: ExecutionContext): J {
        throw UnsupportedOperationException(generateUnsupportedMessage("FirDynamicTypeRef"))
    }

    override fun visitDelegateFieldReference(
        delegateFieldReference: FirDelegateFieldReference,
        data: ExecutionContext
    ): J {
        throw UnsupportedOperationException(generateUnsupportedMessage("FirDelegateFieldReference"))
    }

    override fun visitDeclarationStatus(declarationStatus: FirDeclarationStatus, data: ExecutionContext): J {
        throw UnsupportedOperationException(generateUnsupportedMessage("FirDeclarationStatus"))
    }

    override fun visitField(field: FirField, data: ExecutionContext): J {
        throw UnsupportedOperationException(generateUnsupportedMessage("FirField"))
    }

    override fun visitFunction(function: FirFunction, data: ExecutionContext): J {
        throw UnsupportedOperationException(generateUnsupportedMessage("FirFunction"))
    }

    override fun visitFunctionTypeParameter(
        functionTypeParameter: FirFunctionTypeParameter,
        data: ExecutionContext
    ): J {
        val name: J.Identifier?
        var colon: Space? = null
        if (functionTypeParameter.name != null) {
            name = createIdentifier(functionTypeParameter.name!!.asString())
            colon = whitespace()
            skip(":")
        } else name = null
        return K.FunctionType.Parameter(
            randomId(),
            Markers.EMPTY,
            name,
            colon,
            visitElement(functionTypeParameter.returnTypeRef, data) as TypeTree
        )
    }

    override fun visitImplicitInvokeCall(implicitInvokeCall: FirImplicitInvokeCall, data: ExecutionContext): J {
        throw UnsupportedOperationException(generateUnsupportedMessage("FirImplicitInvokeCall"))
    }

    override fun visitImplicitTypeRef(implicitTypeRef: FirImplicitTypeRef, data: ExecutionContext): J {
        throw UnsupportedOperationException(generateUnsupportedMessage("FirImplicitTypeRef"))
    }

    override fun visitIntegerLiteralOperatorCall(
        integerLiteralOperatorCall: FirIntegerLiteralOperatorCall,
        data: ExecutionContext
    ): J {
        throw UnsupportedOperationException(generateUnsupportedMessage("FirIntegerLiteralOperatorCall"))
    }

    override fun visitIntersectionTypeRef(intersectionTypeRef: FirIntersectionTypeRef, data: ExecutionContext): J {
        throw UnsupportedOperationException(generateUnsupportedMessage("FirIntersectionTypeRef"))
    }

    override fun <E : FirTargetElement> visitJump(jump: FirJump<E>, data: ExecutionContext): J {
        throw UnsupportedOperationException(generateUnsupportedMessage("FirJump"))
    }

    override fun visitLoop(loop: FirLoop, data: ExecutionContext): J {
        throw UnsupportedOperationException(generateUnsupportedMessage("FirLoop"))
    }

    override fun visitLoopJump(loopJump: FirLoopJump, data: ExecutionContext): J {
        throw UnsupportedOperationException(generateUnsupportedMessage("FirLoopJump"))
    }

    override fun visitMemberDeclaration(memberDeclaration: FirMemberDeclaration, data: ExecutionContext): J {
        throw UnsupportedOperationException(generateUnsupportedMessage("FirMemberDeclaration"))
    }

    override fun visitNamedReference(namedReference: FirNamedReference, data: ExecutionContext): J {
        throw UnsupportedOperationException(generateUnsupportedMessage("FirNamedReference"))
    }

    override fun visitPlaceholderProjection(
        placeholderProjection: FirPlaceholderProjection,
        data: ExecutionContext
    ): J {
        throw UnsupportedOperationException(generateUnsupportedMessage("FirPlaceholderProjection"))
    }

    override fun visitQualifiedAccessExpression(
        qualifiedAccessExpression: FirQualifiedAccessExpression,
        data: ExecutionContext
    ): J {
        throw UnsupportedOperationException(generateUnsupportedMessage("FirQualifiedAccessExpression"))
    }

    override fun visitReference(reference: FirReference, data: ExecutionContext): J {
        throw UnsupportedOperationException(generateUnsupportedMessage("FirReference"))
    }

    @OptIn(SymbolInternals::class)
    override fun visitRegularClass(regularClass: FirRegularClass, data: ExecutionContext): J {
        val prefix = whitespace()
        var markers = Markers.EMPTY
        val node = getRealPsiElement(regularClass)
        val modifierList = getModifierList(node)
        var modifiers: MutableList<J.Modifier> = ArrayList()
        val leadingAnnotations: MutableList<J.Annotation> = mutableListOf()
        val kindAnnotations: MutableList<J.Annotation> = mutableListOf()
        if (modifierList != null) {
            modifiers = mapModifierList(modifierList, regularClass.annotations, leadingAnnotations, kindAnnotations)
        }
        var isOpen = false
        for (modifier in modifiers) {
            if (modifier.type == J.Modifier.Type.LanguageExtension && "open" == modifier.keyword) {
                isOpen = true
                break
            }
        }
        if (!isOpen) {
            modifiers.add(
                J.Modifier(
                    randomId(),
                    Space.EMPTY,
                    Markers.EMPTY,
                    null,
                    J.Modifier.Type.Final,
                    emptyList()
                )
            )
        }
        val classKind = regularClass.classKind
        val kind: J.ClassDeclaration.Kind
        if (ClassKind.INTERFACE == classKind) {
            kind = J.ClassDeclaration.Kind(
                randomId(),
                sourceBefore("interface"),
                Markers.EMPTY,
                kindAnnotations,
                J.ClassDeclaration.Kind.Type.Interface
            )
        } else if (ClassKind.OBJECT == classKind) {
            markers = markers.addIfAbsent(KObject(randomId(), Space.EMPTY))
            kind = J.ClassDeclaration.Kind(
                randomId(),
                sourceBefore("object"),
                Markers.EMPTY,
                kindAnnotations,
                J.ClassDeclaration.Kind.Type.Class
            )
        } else {
            // Enums and Interfaces are modifiers in kotlin and require the modifier prefix to preserve source code.
            kind = J.ClassDeclaration.Kind(
                randomId(),
                sourceBefore("class"),
                Markers.EMPTY,
                kindAnnotations,
                if (ClassKind.ENUM_CLASS == classKind) J.ClassDeclaration.Kind.Type.Enum else J.ClassDeclaration.Kind.Type.Class
            )
        }
        var name: J.Identifier
        if (classKind != ClassKind.OBJECT || (node as KtObjectDeclaration?)!!.nameIdentifier != null) {
            name = createIdentifier(regularClass.name.asString(), regularClass)
        } else {
            val saveCursor = cursor
            name = createIdentifier("", regularClass)
            name = name
                .withSimpleName(regularClass.name.asString())
                .withPrefix(Space.EMPTY)
                .withMarkers(name.markers.addIfAbsent(Implicit(randomId())))
            cursor = saveCursor
        }

        // KotlinTypeParameters with multiple bounds are defined outside the TypeParameter container.
        // KotlinTypeGoat<T, S> where S: A, T: B, S: C, T: D.
        // The order the bounds exist in T and S will be based on the declaration order.
        // However, each bound may be declared in any order T -> S -> T -> S.
        var typeParams: JContainer<J.TypeParameter?>? = null
        if (regularClass.typeParameters.isNotEmpty()) {
            val before = sourceBefore("<")
            val typeParameters: MutableList<JRightPadded<J.TypeParameter?>> =
                ArrayList(regularClass.typeParameters.size)
            val parameters = regularClass.typeParameters
            for (i in parameters.indices) {
                val j: J = visitElement(parameters[i], data)!!
                typeParameters.add(
                    padRight(
                        j as J.TypeParameter,
                        if (i == parameters.size - 1) sourceBefore(">") else sourceBefore(",")
                    )
                )
            }
            typeParams = JContainer.build(before, typeParameters, Markers.EMPTY)
        }
        val membersMultiVariablesSeparated: MutableList<FirElement> = ArrayList(regularClass.declarations.size)
        val jcEnums: MutableList<FirDeclaration> = ArrayList(regularClass.declarations.size)
        var firPrimaryConstructor: FirPrimaryConstructor? = null
        for (declaration in regularClass.declarations) {
            if (declaration is FirEnumEntry) {
                jcEnums.add(declaration)
            } else if (declaration is FirPrimaryConstructor) {
                firPrimaryConstructor = declaration
            } else if (declaration is FirProperty && declaration.source?.kind is KtFakeSourceElementKind.PropertyFromParameter) {
                val range = TextRange(declaration.source!!.startOffset, declaration.source!!.endOffset)
                generatedFirProperties[range] = declaration
            } else {
                // We aren't interested in the generated values.
                if (ClassKind.ENUM_CLASS == classKind && declaration.source != null) {
                    continue
                }
                membersMultiVariablesSeparated.add(declaration)
            }
        }
        var primaryConstructor: J.MethodDeclaration? = null
        if ((node as KtClassOrObject?)!!.primaryConstructor != null) {
            markers = markers.addIfAbsent(PrimaryConstructor(randomId()))
            primaryConstructor = mapPrimaryConstructor(firPrimaryConstructor)
        }
        var implementings: JContainer<TypeTree>? = null
        val superTypes: MutableList<JRightPadded<TypeTree>> = ArrayList(regularClass.superTypeRefs.size)
        var saveCursor = cursor
        val before = whitespace()
        skip(":")

        // Kotlin declared super class and interfaces differently than java. All types declared after the `:` are added into implementings.
        // This should probably exist on a K.ClassDeclaration view where the getters return the appropriate types.
        // The J.ClassDeclaration should have the super type set in extending and the J.NewClass should be unwrapped.

        // Filter out generated types.
        val realSuperTypes = regularClass.superTypeRefs.filter { it.source != null && it.source!!.kind !is KtFakeSourceElementKind }.toList()
        for (i in realSuperTypes.indices) {
            val typeRef = realSuperTypes[i]
            val symbol = typeRef.coneType.toRegularClassSymbol(firSession)
            var element = visitElement(typeRef, data) as TypeTree
            if (firPrimaryConstructor != null && symbol != null && ClassKind.CLASS == symbol.fir.classKind) {
                // Wrap the element in a J.NewClass to preserve the whitespace and container of `( )`
                val args = mapFunctionalCallArguments(firPrimaryConstructor.delegatedConstructor!!)
                val delegationCall = J.MethodInvocation(
                    randomId(),
                    element.prefix,
                    Markers.EMPTY.addIfAbsent(ConstructorDelegation(randomId(), before)).addIfAbsent(Implicit(randomId())),
                    null,
                    null,
                    J.Identifier(
                        randomId(),
                        prefix,
                        Markers.EMPTY,
                        emptyList(),
                        if (firPrimaryConstructor.delegatedConstructor!!.isThis) "this" else "super",
                        typeMapping.type(typeRef),
                        null
                    ),
                    args,
                    typeMapping.type(firPrimaryConstructor.delegatedConstructor!!.calleeReference.resolved!!.resolvedSymbol) as? JavaType.Method
                )
                if (primaryConstructor == null) {
                    primaryConstructor = J.MethodDeclaration(
                        randomId(),
                        Space.EMPTY,
                        Markers.build(
                            listOf(
                                PrimaryConstructor(randomId()),
                                Implicit(randomId())
                            )
                        ),
                        emptyList(), // TODO annotations
                        emptyList(), // TODO modifiers
                        null,
                        null,
                        J.MethodDeclaration.IdentifierWithAnnotations(
                            name.withMarkers(name.markers.addIfAbsent(Implicit(randomId()))),
                            emptyList()
                        ),
                        JContainer.empty(),
                        null,
                        J.Block(
                            randomId(),
                            Space.EMPTY,
                            Markers.EMPTY.addIfAbsent(OmitBraces(randomId())),
                            JRightPadded(false, Space.EMPTY, Markers.EMPTY),
                            listOf(JRightPadded.build(delegationCall)),
                            Space.EMPTY
                        ),
                        null,
                        null // TODO type
                    )
                } else {
                    primaryConstructor = primaryConstructor.withBody(J.Block(
                        randomId(),
                        Space.EMPTY,
                        Markers.EMPTY.addIfAbsent(OmitBraces(randomId())),
                        JRightPadded(false, Space.EMPTY, Markers.EMPTY),
                        listOf(JRightPadded.build(delegationCall)),
                        Space.EMPTY
                    ))
                }
                markers = markers.addIfAbsent(PrimaryConstructor(randomId()))
                element = element.withMarkers(element.markers.addIfAbsent(ConstructorDelegation(randomId(), Space.EMPTY)))
            }
            superTypes.add(
                JRightPadded.build(element)
                    .withAfter(if (i == realSuperTypes.size - 1) Space.EMPTY else sourceBefore(","))
            )
        }
        if (superTypes.isEmpty()) {
            cursor(saveCursor)
        } else {
            implementings = JContainer.build(before, superTypes, Markers.EMPTY)
        }
        saveCursor = cursor
        val bodyPrefix = whitespace()
        val omitBraces: OmitBraces
        var body: J.Block
        if (cursor == source.length || source[cursor] != '{') {
            cursor(saveCursor)
            omitBraces = OmitBraces(randomId())
            body = J.Block(
                randomId(),
                bodyPrefix,
                Markers.EMPTY,
                JRightPadded(false, Space.EMPTY, Markers.EMPTY),
                emptyList(),
                Space.EMPTY
            )
            body = body.withMarkers(body.markers.addIfAbsent(omitBraces))
        } else {
            skip("{")
            var enumSet: JRightPadded<Statement>? = null
            if (jcEnums.isNotEmpty()) {
                val semicolonPresent = AtomicBoolean(false)
                val enumValues: MutableList<JRightPadded<J.EnumValue>> = ArrayList(jcEnums.size)
                for (i in jcEnums.indices) {
                    val jcEnum = jcEnums[i]
                    val enumValue = visitElement(jcEnum, data) as J.EnumValue
                    var paddedEnumValue: JRightPadded<J.EnumValue>
                    if (i == jcEnums.size - 1) {
                        // special whitespace handling for last enum constant, as it can have a trailing comma, semicolon, both, or neither...
                        // further, any trailing whitespace is expected to be saved as the `BLOCK_END` location on the block
                        var saveCursor1 = cursor
                        val padding1 = whitespace()
                        val trailingComma = skip(",")
                        saveCursor1 = if (trailingComma) cursor else saveCursor1
                        val padding2 = if (trailingComma) whitespace() else Space.EMPTY
                        val trailingSemicolon = skip(";")
                        saveCursor1 = if (trailingSemicolon) cursor else saveCursor1
                        paddedEnumValue = JRightPadded(
                            enumValue,
                            if (trailingComma || trailingSemicolon) padding1 else Space.EMPTY,
                            if (trailingComma) Markers.build(
                                listOf(
                                    TrailingComma(
                                        randomId(),
                                        if (trailingSemicolon) padding2 else Space.EMPTY
                                    )
                                )
                            ) else Markers.EMPTY
                        )
                        semicolonPresent.set(trailingSemicolon)
                        cursor(saveCursor1)
                    } else {
                        paddedEnumValue = padRight(enumValue, sourceBefore(","))
                    }
                    enumValues.add(paddedEnumValue)
                }
                enumSet = padRight(
                    J.EnumValueSet(
                        randomId(),
                        enumValues[0].element.prefix ?: Space.EMPTY,
                        Markers.EMPTY,
                        ListUtils.map(
                            enumValues
                        ) { i: Int, ev: JRightPadded<J.EnumValue> ->
                            if (i == 0) ev.withElement(
                                ev.element.withPrefix(Space.EMPTY) ?: ev.element
                            ) else ev
                        },
                        semicolonPresent.get()
                    ),
                    Space.EMPTY
                )
            }
            val members: MutableList<JRightPadded<Statement>> = ArrayList(
                membersMultiVariablesSeparated.size + if (enumSet == null) 0 else 1
            )
            if (enumSet != null) {
                members.add(enumSet)
            }
            for (firElement in membersMultiVariablesSeparated) {
                if (firElement !is FirEnumEntry) {
                    if (firElement.source != null && firElement.source!!.kind is KtFakeSourceElementKind) {
                        continue
                    }
                    members.add(maybeSemicolon(visitElement(firElement, data) as Statement))
                }
            }
            val after = sourceBefore("}")
            body = J.Block(
                randomId(), bodyPrefix, Markers.EMPTY, JRightPadded(false, Space.EMPTY, Markers.EMPTY),
                members, after
            )
        }
        if (primaryConstructor != null) {
            body = body.withStatements(
                ListUtils.concat(
                    primaryConstructor,
                    body.statements
                )
            )
        }
        return J.ClassDeclaration(
            randomId(),
            prefix,
            markers,
            if (leadingAnnotations.isEmpty()) emptyList() else leadingAnnotations,
            if (modifiers.isEmpty()) emptyList() else modifiers,
            kind,
            name,
            typeParams,
            null,
            null,
            implementings,
            null,
            body,
            typeMapping.type(regularClass) as JavaType.FullyQualified?
        )
    }

    private fun mapPrimaryConstructor(primaryConstructor: FirPrimaryConstructor?): J.MethodDeclaration {
        val prefix = whitespace()
        var modifiers: MutableList<J.Modifier> = ArrayList()
        var leadingAnnotations: MutableList<J.Annotation> = mutableListOf()
        var lastAnnotations: MutableList<J.Annotation> = mutableListOf()
        val node = getRealPsiElement(primaryConstructor) as KtPrimaryConstructor?
        val modifierList = getModifierList(node)
        if (modifierList != null) {
            leadingAnnotations = ArrayList()
            lastAnnotations = ArrayList()
            modifiers =
                mapModifierList(modifierList, primaryConstructor!!.annotations, leadingAnnotations, lastAnnotations)
        }
        val cKeyword = node!!.getConstructorKeyword()
        if (cKeyword != null) {
            modifiers.add(mapToJModifier("constructor", lastAnnotations))
        }
        val type = typeMapping.type(primaryConstructor)
        val name = J.Identifier(
            randomId(),
            Space.EMPTY,
            Markers.EMPTY,
            emptyList(),
            node.name!!,
            type as? JavaType.Method,
            null
        )
        val before = sourceBefore("(")
        val params = if (primaryConstructor!!.valueParameters.isNotEmpty()) JContainer.build(
            before,
            convertAll<Statement>(
                primaryConstructor.valueParameters, ",", ")", data
            ),
            Markers.EMPTY
        ) else JContainer.build(
            before, listOf(
                padRight<Statement>(
                    J.Empty(randomId(), sourceBefore(")"), Markers.EMPTY), Space.EMPTY
                )
            ), Markers.EMPTY
        )
        return J.MethodDeclaration(
            randomId(),
            prefix,
            Markers.build(listOf(PrimaryConstructor(randomId()))),
            leadingAnnotations,
            if (modifiers.isEmpty()) emptyList() else modifiers,
            null,
            null,
            J.MethodDeclaration.IdentifierWithAnnotations(
                name.withMarkers(name.markers.addIfAbsent(Implicit(randomId()))),
                emptyList()
            ),
            params,
            null,
            null,
            null,
            type as? JavaType.Method
        )
    }

    private fun mapModifierList(
        currentNode: KtModifierList, annotations: List<FirAnnotation>,
        leadingAnnotations: MutableList<J.Annotation>, lastAnnotations: MutableList<J.Annotation>
    ): MutableList<J.Modifier> {
        val annotationsMap: MutableMap<Int, FirAnnotation> = HashMap()
        for (annotation in annotations) {
            annotationsMap[annotation.source!!.startOffset] = annotation
        }
        val modifiers: MutableList<J.Modifier> = ArrayList()
        var currentAnnotations: MutableList<J.Annotation> = ArrayList()
        val iterator: Iterator<PsiElement> = currentNode.allChildren.iterator()
        var leading = true
        while (iterator.hasNext()) {
            val it = iterator.next()
            if (it is LeafPsiElement && it.getNode().elementType is KtModifierKeywordToken) {
                if (leading) {
                    leading = false
                    if (currentAnnotations.isNotEmpty()) {
                        leadingAnnotations.addAll(currentAnnotations)
                        currentAnnotations = ArrayList()
                    }
                }
                modifiers.add(mapToJModifier(it.text, currentAnnotations))
                currentAnnotations = ArrayList()
            } else if (it is KtAnnotationEntry) {
                if (annotationsMap.containsKey(it.getTextRange().startOffset)) {
                    val annotation = visitElement(annotationsMap[it.getTextRange().startOffset]!!, data) as J.Annotation
                    currentAnnotations.add(annotation)
                } else {
                    throw UnsupportedOperationException("Annotation not found")
                }
            }
        }
        if (currentAnnotations.isNotEmpty()) {
            if (leading) {
                leadingAnnotations.addAll(currentAnnotations)
            } else {
                lastAnnotations.addAll(currentAnnotations)
            }
        }
        return modifiers
    }

    override fun visitResolvable(resolvable: FirResolvable, data: ExecutionContext): J {
        throw UnsupportedOperationException(generateUnsupportedMessage("FirResolvable"))
    }

    override fun visitResolvedCallableReference(
        resolvedCallableReference: FirResolvedCallableReference,
        data: ExecutionContext
    ): J {
        throw UnsupportedOperationException(generateUnsupportedMessage("FirResolvedCallableReference"))
    }

    override fun visitResolvedDeclarationStatus(
        resolvedDeclarationStatus: FirResolvedDeclarationStatus,
        data: ExecutionContext
    ): J {
        throw UnsupportedOperationException(generateUnsupportedMessage("FirResolvedDeclarationStatus"))
    }

    override fun visitResolvedImport(resolvedImport: FirResolvedImport, data: ExecutionContext): J {
        throw UnsupportedOperationException(generateUnsupportedMessage("FirResolvedImport"))
    }

    override fun visitSpreadArgumentExpression(
        spreadArgumentExpression: FirSpreadArgumentExpression,
        data: ExecutionContext
    ): J {
        if (!spreadArgumentExpression.isSpread) {
            // A spread argument without a spread operator?
            throw UnsupportedOperationException("Only spread arguments are supported")
        }
        val prefix = whitespace()
        skip("*")
        val j: J = visitElement(spreadArgumentExpression.expression, data)!!
        return j.withMarkers(j.markers.addIfAbsent(SpreadArgument(randomId(), prefix)))
    }

    override fun visitTypeRef(typeRef: FirTypeRef, data: ExecutionContext): J {
        throw UnsupportedOperationException(generateUnsupportedMessage("FirTypeRef"))
    }

    override fun visitTargetElement(targetElement: FirTargetElement, data: ExecutionContext): J {
        throw UnsupportedOperationException(generateUnsupportedMessage("FirTargetElement"))
    }

    override fun visitThisReference(thisReference: FirThisReference, data: ExecutionContext): J {
        throw UnsupportedOperationException(generateUnsupportedMessage("FirThisReference"))
    }

    override fun visitThrowExpression(throwExpression: FirThrowExpression, data: ExecutionContext): J {
        val prefix = whitespace()
        skip("throw")
        return J.Throw(
            randomId(),
            prefix,
            Markers.EMPTY,
            convertToExpression(throwExpression.exception, data)!!
        )
    }

    override fun visitTypeParameterRef(typeParameterRef: FirTypeParameterRef, data: ExecutionContext): J {
        throw UnsupportedOperationException(generateUnsupportedMessage("FirTypeParameterRef"))
    }

    override fun visitTypeParameterRefsOwner(
        typeParameterRefsOwner: FirTypeParameterRefsOwner,
        data: ExecutionContext
    ): J {
        throw UnsupportedOperationException(generateUnsupportedMessage("FirTypeParameterRefsOwner"))
    }

    override fun visitTypeParametersOwner(typeParametersOwner: FirTypeParametersOwner, data: ExecutionContext): J {
        throw UnsupportedOperationException(generateUnsupportedMessage("FirTypeParametersOwner"))
    }

    override fun visitTypeProjection(typeProjection: FirTypeProjection, data: ExecutionContext): J {
        throw UnsupportedOperationException(generateUnsupportedMessage("FirTypeProjection"))
    }

    override fun visitTypeRefWithNullability(
        typeRefWithNullability: FirTypeRefWithNullability,
        data: ExecutionContext
    ): J {
        throw UnsupportedOperationException(generateUnsupportedMessage("FirTypeRefWithNullability"))
    }

    override fun visitVarargArgumentsExpression(
        varargArgumentsExpression: FirVarargArgumentsExpression,
        data: ExecutionContext
    ): J {
        throw UnsupportedOperationException(generateUnsupportedMessage("FirVarargArgumentsExpression"))
    }

    override fun visitWhenSubjectExpression(
        whenSubjectExpression: FirWhenSubjectExpression,
        data: ExecutionContext
    ): J {
        throw UnsupportedOperationException(generateUnsupportedMessage("FirWhenSubjectExpression"))
    }

    override fun visitWrappedArgumentExpression(
        wrappedArgumentExpression: FirWrappedArgumentExpression,
        data: ExecutionContext
    ): J {
        throw UnsupportedOperationException(generateUnsupportedMessage("FirWrappedArgumentExpression"))
    }

    override fun visitWrappedDelegateExpression(
        wrappedDelegateExpression: FirWrappedDelegateExpression,
        data: ExecutionContext
    ): J {
        throw UnsupportedOperationException(generateUnsupportedMessage("FirWrappedDelegateExpression"))
    }

    override fun visitWrappedExpression(wrappedExpression: FirWrappedExpression, data: ExecutionContext): J {
        throw UnsupportedOperationException(generateUnsupportedMessage("FirWrappedExpression"))
    }

    private fun visitNoReceiverExpression(): J {
        throw UnsupportedOperationException(generateUnsupportedMessage("FirNoReceiverExpression"))
    }

    /* Error element visits */
    override fun visitErrorExpression(errorExpression: FirErrorExpression, data: ExecutionContext): J {
        throw UnsupportedOperationException(generateUnsupportedMessage("FirErrorExpression"))
    }

    override fun visitErrorFunction(errorFunction: FirErrorFunction, data: ExecutionContext): J {
        throw UnsupportedOperationException(generateUnsupportedMessage("FirErrorFunction"))
    }

    override fun visitErrorImport(errorImport: FirErrorImport, data: ExecutionContext): J {
        throw UnsupportedOperationException(generateUnsupportedMessage("FirErrorImport"))
    }

    override fun visitErrorLoop(errorLoop: FirErrorLoop, data: ExecutionContext): J {
        throw UnsupportedOperationException(generateUnsupportedMessage("FirErrorLoop"))
    }

    override fun visitErrorProperty(errorProperty: FirErrorProperty, data: ExecutionContext): J {
        throw UnsupportedOperationException(generateUnsupportedMessage("FirErrorProperty"))
    }

    override fun visitErrorResolvedQualifier(
        errorResolvedQualifier: FirErrorResolvedQualifier,
        data: ExecutionContext
    ): J {
        throw UnsupportedOperationException(generateUnsupportedMessage("FirErrorResolvedQualifier"))
    }

    override fun visitErrorTypeRef(errorTypeRef: FirErrorTypeRef, data: ExecutionContext): J {
        throw UnsupportedOperationException(generateUnsupportedMessage("FirErrorTypeRef"))
    }

    private fun generateUnsupportedMessage(typeName: String): String {
        val msg = StringBuilder(typeName)
        msg.append(" is not supported at cursor: ")
        msg.append(source, cursor, min(source.length, cursor + 30))
        if (currentFile != null) {
            msg.append("in file: ")
            msg.append(currentFile!!.name)
        }
        return msg.toString()
    }

    override fun visitElement(element: FirElement, data: ExecutionContext): J? {
        var firElement = element
        val saveCursor = cursor
        whitespace()
        val node = nodes[cursor]
        cursor = saveCursor
        if (node != null) {
            when (node.elementType) {
                KtNodeTypes.PARENTHESIZED -> if (node.textRange.endOffset >= firElement.source!!.endOffset)
                    return wrapInParens<J>(firElement, data)

                KtNodeTypes.REFERENCE_EXPRESSION -> if (KtNodeTypes.POSTFIX_EXPRESSION == node.treeParent.elementType && firElement is FirBlock)
                    firElement = firElement.statements[1]

                KtNodeTypes.OPERATION_REFERENCE -> if (KtNodeTypes.PREFIX_EXPRESSION == node.treeParent.elementType && firElement is FirBlock)
                    firElement = firElement.statements[0]
            }
        }

        if (firElement is FirExpression && firElement.annotations.isNotEmpty()) {
            val annotations = ArrayList<J.Annotation>(firElement.annotations.size)
            for (annotation in firElement.annotations) {
                annotations.add(visitElement(annotation, data) as J.Annotation)
            }
            return K.AnnotatedExpression(
                randomId(),
                Markers.EMPTY,
                annotations,
                visitElement0(firElement, data) as Expression
            )
        }
        return visitElement0(firElement, data)
    }

    private fun visitElement0(firElement: FirElement, data: ExecutionContext): J? {
        return when (firElement) {
            // FIR error elements
            is FirErrorNamedReference -> visitErrorNamedReference(firElement, data)
            is FirErrorExpression -> visitErrorExpression(firElement, data)
            is FirErrorFunction -> visitErrorFunction(firElement, data)
            is FirErrorImport -> visitErrorImport(firElement, data)
            is FirErrorLoop -> visitErrorLoop(firElement, data)
            is FirErrorProperty -> visitErrorProperty(firElement, data)
            is FirErrorResolvedQualifier -> visitErrorResolvedQualifier(firElement, data)
            is FirErrorTypeRef -> visitErrorTypeRef(firElement, data)

            // FIR trees
            is FirAnnotationCall -> visitAnnotationCall(firElement, data)
            is FirAnonymousFunction -> visitAnonymousFunction(firElement, data)
            is FirAnonymousFunctionExpression -> visitAnonymousFunctionExpression(firElement, data)
            is FirAnonymousObject -> visitAnonymousObject(firElement, data)
            is FirAnonymousObjectExpression -> visitAnonymousObjectExpression(firElement, data)
            is FirArrayOfCall -> visitArrayOfCall(firElement, data)
            is FirBackingFieldReference -> visitBackingFieldReference(firElement, data)
            is FirBinaryLogicExpression -> visitBinaryLogicExpression(firElement, data)
            is FirBlock -> visitBlock(firElement, data)
            is FirBreakExpression -> visitBreakExpression(firElement, data)
            is FirCallableReferenceAccess -> visitCallableReferenceAccess(firElement, data)
            is FirCatch -> visitCatch(firElement, data)
            is FirCheckNotNullCall -> visitCheckNotNullCall(firElement, data)
            is FirComparisonExpression -> visitComparisonExpression(firElement, data)
            is FirConstExpression<*> -> visitConstExpression(firElement, data)
            is FirConstructor -> visitConstructor(firElement, data)
            is FirContinueExpression -> visitContinueExpression(firElement, data)
            is FirDoWhileLoop -> visitDoWhileLoop(firElement, data)
            is FirElvisExpression -> visitElvisExpression(firElement, data)
            is FirEnumEntry -> visitEnumEntry(firElement, data)
            is FirEqualityOperatorCall -> visitEqualityOperatorCall(firElement, data)
            is FirFunctionCall -> visitFunctionCall(firElement, data)
            is FirFunctionTypeRef -> visitFunctionTypeRef(firElement, data)
            is FirGetClassCall -> visitGetClassCall(firElement, data)
            is FirLabel -> visitLabel(firElement, data)
            is FirLambdaArgumentExpression -> visitLambdaArgumentExpression(firElement, data)
            is FirNamedArgumentExpression -> visitNamedArgumentExpression(firElement, data)
            is FirProperty -> visitProperty(firElement, data)
            is FirPropertyAccessExpression -> visitPropertyAccessExpression(firElement, data)
            is FirPropertyAccessor -> visitPropertyAccessor(firElement, data)
            is FirReceiverParameter -> visitReceiverParameter(firElement, data)
            is FirRegularClass -> visitRegularClass(firElement, data)
            is FirResolvedNamedReference -> visitResolvedNamedReference(firElement, data)
            is FirResolvedTypeRef -> visitResolvedTypeRef(firElement, data)
            is FirResolvedQualifier -> visitResolvedQualifier(firElement, data)
            is FirReturnExpression -> visitReturnExpression(firElement, data)
            is FirSafeCallExpression -> visitSafeCallExpression(firElement, data)
            is FirCheckedSafeCallSubject -> visitCheckedSafeCallSubject(firElement, data)
            is FirSimpleFunction -> visitSimpleFunction(firElement, data)
            is FirSmartCastExpression -> visitSmartCastExpression(firElement, data)
            is FirStarProjection -> visitStarProjection(firElement, data)
            is FirStringConcatenationCall -> visitStringConcatenationCall(firElement, data)
            is FirSuperReference -> visitSuperReference(firElement, data)
            is FirThisReceiverExpression -> visitThisReceiverExpression(firElement, data)
            is FirThrowExpression -> visitThrowExpression(firElement, data)
            is FirTypeOperatorCall -> visitTypeOperatorCall(firElement, data)
            is FirTypeParameter -> visitTypeParameter(firElement, data)
            is FirTryExpression -> visitTryExpression(firElement, data)
            is FirTypeAlias -> visitTypeAlias(firElement, data)
            is FirTypeProjectionWithVariance -> visitTypeProjectionWithVariance(firElement, data)
            is FirUserTypeRef -> visitUserTypeRef(firElement, data)
            is FirValueParameter -> visitValueParameter(firElement, data)
            is FirVariableAssignment -> visitVariableAssignment(firElement, data)
            is FirWhenBranch -> visitWhenBranch(firElement, data)
            is FirWhenExpression -> visitWhenExpression(firElement, data)
            is FirWhenSubjectExpression -> visitWhenSubjectExpression(firElement, data)
            is FirWhileLoop -> visitWhileLoop(firElement, data)
            is FirArgumentList -> visitArgumentList(firElement, data)
            is FirAugmentedArraySetCall -> visitAugmentedArraySetCall(firElement, data)
            is FirAssignmentOperatorStatement -> visitAssignmentOperatorStatement(firElement, data)
            is FirAnonymousInitializer -> visitAnonymousInitializer(firElement, data)
            is FirAnnotationArgumentMapping -> visitAnnotationArgumentMapping(firElement, data)
            is FirBackingField -> visitBackingField(firElement, data)
            is FirLegacyRawContractDescription -> visitLegacyRawContractDescription(firElement, data)
            is FirRawContractDescription -> visitRawContractDescription(firElement, data)
            is FirResolvedContractDescription -> visitResolvedContractDescription(firElement, data)
            is FirContractDescription -> visitContractDescription(firElement, data)
            is FirContextReceiver -> visitContextReceiver(firElement, data)
            is FirContractDescriptionOwner -> visitContractDescriptionOwner(firElement, data)
            is FirQualifiedAccessExpression -> visitQualifiedAccessExpression(firElement, data)
            is FirContextReceiverArgumentListOwner -> visitContextReceiverArgumentListOwner(firElement, data)
            is FirClassReferenceExpression -> visitClassReferenceExpression(firElement, data)
            is FirClassLikeDeclaration -> visitClassLikeDeclaration(firElement, data)
            is FirCall -> visitCall(firElement, data)
            is FirDynamicTypeRef -> visitDynamicTypeRef(firElement, data)
            is FirResolvedDeclarationStatus -> visitResolvedDeclarationStatus(firElement, data)
            is FirDeclarationStatus -> visitDeclarationStatus(firElement, data)
            is FirEffectDeclaration -> visitEffectDeclaration(firElement, data)
            is FirField -> visitField(firElement, data)
            is FirFunction -> visitFunction(firElement, data)
            is FirFunctionTypeParameter -> visitFunctionTypeParameter(firElement, data)
            is FirImplicitTypeRef -> visitImplicitTypeRef(firElement, data)
            is FirIntersectionTypeRef -> visitIntersectionTypeRef(firElement, data)
            is FirLoopJump -> visitLoopJump(firElement, data)
            is FirJump<*> -> visitJump(firElement as FirJump<out FirTargetElement>, data)
            is FirNamedReference -> visitNamedReference(firElement, data)
            is FirPlaceholderProjection -> visitPlaceholderProjection(firElement, data)
            is FirThisReference -> visitThisReference(firElement, data)
            is FirReference -> visitReference(firElement, data)
            is FirResolvable -> visitResolvable(firElement, data)
            is FirResolvedImport -> visitResolvedImport(firElement, data)
            is FirResolvedReifiedParameterReference -> visitResolvedReifiedParameterReference(firElement, data)
            is FirSpreadArgumentExpression -> visitSpreadArgumentExpression(firElement, data)
            is FirTypeRefWithNullability -> visitTypeRefWithNullability(firElement, data)
            is FirTypeRef -> visitTypeRef(firElement, data)
            is FirTypeParameterRef -> visitTypeParameterRef(firElement, data)
            is FirTypeParametersOwner -> visitTypeParametersOwner(firElement, data)
            is FirTypeProjection -> visitTypeProjection(firElement, data)
            is FirVarargArgumentsExpression -> visitVarargArgumentsExpression(firElement, data)
            is FirWrappedArgumentExpression -> visitWrappedArgumentExpression(firElement, data)
            is FirWrappedExpression -> visitWrappedExpression(firElement, data)
            is FirNoReceiverExpression -> visitNoReceiverExpression()

            else -> throw IllegalArgumentException("Unsupported FirElement " + firElement.javaClass.name)
        }
    }

    private fun <J2 : J?> wrapInParens(firElement: FirElement, data: ExecutionContext): J {
        val prefix = sourceBefore("(")
        @Suppress("UNCHECKED_CAST")
        return J.Parentheses(
            randomId(),
            prefix,
            Markers.EMPTY,
            padRight(visitElement(firElement, data)!!, sourceBefore(")")) as JRightPadded<J2>
        )
    }

    private fun createIdentifier(name: String?): J.Identifier {
        return createIdentifier(name ?: "", null, null)
    }

    private fun createIdentifier(name: String?, firElement: FirElement): J.Identifier {
        val type = typeMapping.type(firElement, getCurrentFile())
        return createIdentifier(
            name ?: "",
            if (type is JavaType.Variable) type.type else type,
            if (type is JavaType.Variable) type else null
        )
    }

    @OptIn(SymbolInternals::class)
    private fun createIdentifier(name: String, namedReference: FirResolvedNamedReference): J.Identifier {
        val resolvedSymbol = namedReference.resolvedSymbol
        if (resolvedSymbol is FirVariableSymbol<*>) {
            var owner: JavaType.FullyQualified? = null
            val lookupTag: ConeClassLikeLookupTag? = resolvedSymbol.containingClassLookupTag()
            if (lookupTag != null && lookupTag.toSymbol(firSession) !is FirAnonymousObjectSymbol) {
                // TODO check type attribution for `FirAnonymousObjectSymbol` case
                owner =
                    typeMapping.type(lookupTag.toFirRegularClassSymbol(firSession)!!.fir) as JavaType.FullyQualified?
            }
            return createIdentifier(
                name, typeMapping.type(namedReference, getCurrentFile()),
                typeMapping.variableType(resolvedSymbol, owner, getCurrentFile())
            )
        }
        return createIdentifier(name, namedReference as FirElement)
    }

    private fun createIdentifier(
        name: String,
        type: JavaType?,
        fieldType: JavaType.Variable?
    ): J.Identifier {
        val prefix = whitespace()
        val isQuotedSymbol = source[cursor] == '`'
        val value: String
        if (isQuotedSymbol) {
            val closingQuoteIdx = source.indexOf('`', cursor + 1)
            value = source.substring(cursor, closingQuoteIdx + 1)
            cursor += value.length
        } else {
            value = name
            skip(value)
        }
        return J.Identifier(
            randomId(),
            prefix,
            Markers.EMPTY,
            emptyList(),
            value,
            type,
            fieldType
        )
    }

    private fun createImplicitMethodDeclaration(name: String): J.MethodDeclaration {
        return J.MethodDeclaration(
            randomId(),
            Space.EMPTY,
            Markers.EMPTY,
            emptyList(),
            emptyList(),
            null,
            null,
            J.MethodDeclaration.IdentifierWithAnnotations(
                J.Identifier(
                    randomId(),
                    Space.EMPTY,
                    Markers.EMPTY,
                    emptyList(),
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
        ).withMarkers(Markers.EMPTY.addIfAbsent(Implicit(randomId())))
    }

    private fun mapAnnotations(firAnnotations: List<FirAnnotation>): MutableList<J.Annotation>? {
        if (firAnnotations.isEmpty()) {
            return null
        }
        val annotations: MutableList<J.Annotation> = ArrayList(firAnnotations.size)
        for (annotation in firAnnotations) {
            val a = visitElement(annotation, data) as J.Annotation?
            if (a != null) {
                annotations.add(a)
            }
        }
        return annotations
    }

    private fun mapControlParentheses(firElement: FirElement): J.ControlParentheses<Expression> {
        val controlParenPrefix = whitespace()
        skip("(")
        return J.ControlParentheses(
            randomId(), controlParenPrefix, Markers.EMPTY,
            convertToExpression(
                firElement,
                { sourceBefore(")") }, data
            )!!
        )
    }

    private fun mapForLoop(firBlock: FirBlock): J {
        val forLoop = firBlock.statements[1] as FirWhileLoop
        val receiver = forLoop.block.statements[0] as FirProperty
        var label: J.Label? = null
        if (forLoop.label != null) {
            label = visitElement(forLoop.label!!, data) as J.Label
        }
        val prefix = whitespace()
        skip("for")
        val controlPrefix = sourceBefore("(")
        val variable: J.VariableDeclarations?
        val additionalVariables: Int
        if ("<destruct>" == receiver.name.asString()) {
            additionalVariables =
                source.substring(cursor, source.indexOf(')', cursor) + 1).split(",".toRegex())
                    .dropLastWhile { it.isEmpty() }
                    .toTypedArray().size
            val variablePrefix = sourceBefore("(")
            val statements = forLoop.block.statements
            val variables: MutableList<JRightPadded<J.VariableDeclarations.NamedVariable>> =
                ArrayList(additionalVariables)
            // Skip the first statement at position 0.
            for (i in 1 until additionalVariables + 1) {
                val statement = statements[i]
                val part = visitElement(statement, data) as J.VariableDeclarations
                var named = part.padding.variables[0]
                named = named.withElement(named.element.withName(named.element.name.withPrefix(part.prefix)))
                val after = if (i == additionalVariables) sourceBefore(")") else sourceBefore(",")
                named = named.withAfter(after)
                variables.add(named)
            }
            variable = J.VariableDeclarations(
                randomId(),
                variablePrefix,
                Markers.EMPTY,
                emptyList(),
                emptyList(),
                null,
                null,
                emptyList(),
                variables
            )
        } else {
            variable = visitElement(receiver, data) as J.VariableDeclarations
        }
        val afterVariable = sourceBefore("in")
        val loopCondition = firBlock.statements[0] as FirProperty
        val expression: Expression = convertToExpression(
            (loopCondition.initializer as FirFunctionCall?)!!.explicitReceiver!!,
            data
        )!!
        val afterExpression = sourceBefore(")")
        val control = J.ForEachLoop.Control(
            randomId(),
            controlPrefix,
            Markers.EMPTY,
            padRight(variable, afterVariable),
            padRight(expression, afterExpression)
        )
        val body: JRightPadded<Statement> = if (forLoop.block.statements.isNotEmpty()) {
            // actual loop body is contained as a nested block of the last statement
            val block = visitBlock(forLoop.block.statements[forLoop.block.statements.size - 1] as FirBlock, emptySet(), data) as Statement
            padRight(block, Space.EMPTY)
        } else {
            padRight(J.Empty(randomId(), Space.EMPTY, Markers.EMPTY), Space.EMPTY)
        }

        val statement = J.ForEachLoop(
            randomId(),
            prefix,
            Markers.EMPTY,
            control,
            body
        )
        return if (label != null) label.withStatement(statement) else statement
    }

    private fun mapToJModifier(text: String, annotations: List<J.Annotation>): J.Modifier {
        val prefix = whitespace()
        val type: J.Modifier.Type
        var keyword: String? = null
        when (text) {
            "public" -> type = J.Modifier.Type.Public
            "protected" -> type = J.Modifier.Type.Protected
            "private" -> type = J.Modifier.Type.Private
            "abstract" -> type = J.Modifier.Type.Abstract
            "val" -> type = J.Modifier.Type.Final
            else -> {
                type = J.Modifier.Type.LanguageExtension
                keyword = text
            }
        }
        skip(text)
        return J.Modifier(randomId(), prefix, Markers.EMPTY, keyword, type, annotations)
    }

    private fun mapOperation(firOp: FirOperation): J.Binary.Type {
        val op: J.Binary.Type = when (firOp) {
            FirOperation.EQ -> J.Binary.Type.Equal
            FirOperation.NOT_EQ -> J.Binary.Type.NotEqual
            FirOperation.GT -> J.Binary.Type.GreaterThan
            FirOperation.GT_EQ -> J.Binary.Type.GreaterThanOrEqual
            FirOperation.LT -> J.Binary.Type.LessThan
            FirOperation.LT_EQ -> J.Binary.Type.LessThanOrEqual
            else -> throw IllegalArgumentException("Unsupported FirOperation does not map to J.Binary.Type: " + firOp.name)
        }
        return op
    }

    private fun isUnaryOperation(name: String): Boolean {
        return "dec" == name || "inc" == name || "not" == name || "unaryMinus" == name || "unaryPlus" == name
    }

    private fun getCurrentFile(): FirBasedSymbol<*>? {
        return if (currentFile == null) null else currentFile!!.symbol
    }

    private fun skip(token: String?): Boolean {
        if (token != null && source.startsWith(token, cursor)) {
            cursor += token.length
            return true
        }
        return false
    }

    private fun cursor(n: Int) {
        cursor = n
    }

    private fun endPos(t: FirElement): Int {
        if (t is FirThisReceiverExpression) {
            return 0
        } else checkNotNull(t.source) { "Unexpected null source ... fix me." }
        return t.source!!.endOffset
    }

    private fun getModifierList(element: PsiElement?): KtModifierList? {
        if (element == null) {
            return null
        }
        val modifierList = PsiTreeUtil.findChildOfType(element, KtModifierList::class.java)
        if (modifierList != null) {
            // There may be multiple modifier lists in the element, and we only want the modifier list for the element.
            for (child in element.children) {
                // The element's start offset will be at the last leading comment.
                if (child is KtModifierList && modifierList.textRange.startOffset == child.getTextRange().startOffset) {
                    return modifierList
                }
            }
        }
        return null
    }

    private fun getRealPsiElement(element: FirElement?): PsiElement? {
        return if ((element?.source == null) || (element.source is KtFakeSourceElement)) {
            null
        } else {
            (element.source as KtRealPsiSourceElement?)?.psi
        }
    }

    private fun getPsiElement(element: FirElement?): PsiElement? {
        return if (element?.source == null) {
            null
        } else if (element.source is KtFakeSourceElement) {
            (element.source as KtFakeSourceElement).psi
        } else {
            (element.source as KtRealPsiSourceElement?)?.psi
        }
    }

    private fun convertToBlock(t: FirSingleExpressionBlock, data: ExecutionContext): J.Block {
        var j: J = visitElement(t, data)!!
        if (j !is Statement && j is Expression) {
            j = K.ExpressionStatement(randomId(), j)
        }
        return J.Block(
            randomId(),
            Space.EMPTY,
            Markers.EMPTY.addIfAbsent(OmitBraces(randomId())).addIfAbsent(SingleExpressionBlock(randomId())),
            JRightPadded.build(false),
            listOf(JRightPadded.build(j as Statement)),
            Space.EMPTY
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun <J2 : J> convertToExpression(t: FirElement, data: ExecutionContext): J2? {
        var j: J? = visitElement(t, data)
        if (j is Statement && j !is Expression) {
            j = K.StatementExpression(randomId(), j)
        }
        return j as J2?
    }

    @Suppress("UNCHECKED_CAST")
    private fun <J2 : J> convertToExpression(
        t: FirElement,
        suffix: Function<FirElement, Space>,
        data: ExecutionContext
    ): JRightPadded<J2>? {
        val j: J? = visitElement(t, data)
        var j2: J2? = null
        if (j is Statement && j !is Expression) {
            j2 = K.StatementExpression(randomId(), j) as J2
        } else if (j != null) {
            j2 = j as J2
        }
        val rightPadded = if (j2 == null) null else JRightPadded(j2, suffix.apply(t), Markers.EMPTY)
        cursor(max(endPos(t), cursor)) // if there is a non-empty suffix, the cursor may have already moved past it
        return rightPadded
    }

    @Suppress("SameParameterValue")
    private fun <J2 : J> convertAll(
        elements: List<FirElement>,
        innerDelim: String,
        delim: String,
        data: ExecutionContext
    ): MutableList<JRightPadded<J2>> {
        return convertAll0(elements, data, innerDelim, delim) {
            @Suppress("UNCHECKED_CAST")
            it as J2
        }
    }

    @Suppress("SameParameterValue")
    private fun <J2 : J> convertAllToExpressions(
        elements: List<FirElement>,
        innerDelim: String,
        delim: String,
        data: ExecutionContext
    ): MutableList<JRightPadded<J2>> {
        return convertAll0(elements, data, innerDelim, delim) {
            if (it is Statement && it !is Expression) {
                K.StatementExpression(randomId(), it)
            }
            @Suppress("UNCHECKED_CAST")
            it as J2?
        }
    }

    private fun <J2 : J> KotlinParserVisitor.convertAll0(
        elements: List<FirElement>,
        data: ExecutionContext,
        innerDelim: String,
        delim: String,
        map: (J?) -> J2?
    ): MutableList<JRightPadded<J2>> {
        val elementCount = elements.size
        val converted: MutableList<JRightPadded<J2>> = ArrayList(elementCount)

        var j: J2? = null
        var rightPadded: JRightPadded<J2>

        for (i in elements.indices) {
            val element = elements[i]

            if (element.source != null) {
                val saveCursor = cursor
                try {
                    j = map(visitElement(element, data))
                } catch (e: Exception) {
                    if (element.source == null || getRealPsiElement(element) == null) {
                        throw KotlinParsingException("Failed to parse declaration", e)
                    }
                    cursor = saveCursor
                    val prefix = whitespace()
                    var text = getRealPsiElement(element)!!.text
                    if (prefix.comments.isNotEmpty()) {
                        val lastComment = prefix.comments[prefix.comments.size - 1]
                        val prefixText = lastComment.printComment(Cursor(null, lastComment)) + lastComment.suffix
                        text = text.substring(prefixText.length)
                    }
                    @Suppress("UNCHECKED_CAST")
                    j = J.Unknown(
                        randomId(),
                        prefix,
                        Markers.EMPTY,
                        J.Unknown.Source(
                            randomId(),
                            Space.EMPTY,
                            Markers.build(
                                listOf(
                                    ParseExceptionResult.build(
                                        KotlinParser::class.java, e
                                    )
                                        .withTreeType(element.source!!.kind.toString())
                                )
                            ),
                            text
                        )
                    ) as J2
                    skip(text)
                }
            } else {
                j = map(visitElement(element, data))
            }

            if (i < elementCount - 1) {
                rightPadded = if (j != null) {
                    padRight(j, sourceBefore(innerDelim))
                } else {
                    continue
                }

                converted.add(rightPadded)
            }
        }

        // handle last element
        val space = whitespace()
        if (j != null) {
            rightPadded = if (skip(",")) padRight(j, space).withMarkers(
                    Markers.build(
                            listOf(
                                    TrailingComma(
                                            randomId(), whitespace()
                                    )
                            )
                    )
            ) else padRight(j, space)
        } else {
            @Suppress("UNCHECKED_CAST")
            j = J.Empty(randomId(), Space.EMPTY, Markers.EMPTY) as J2
            rightPadded = padRight(j, space)
        }
        skip(delim)
        converted.add(rightPadded)

        return if (converted.isEmpty()) mutableListOf() else converted
    }

    private fun <K2 : J> maybeSemicolon(k: K2): JRightPadded<K2> {
        val saveCursor = cursor
        var beforeSemi: Space = whitespace()
        var semicolon: Semicolon? = null
        if (cursor < source.length && skip(";")) {
            semicolon = Semicolon(randomId())
        } else {
            beforeSemi = Space.EMPTY
            cursor(saveCursor)
        }
        var padded = JRightPadded.build(k).withAfter(beforeSemi)
        if (semicolon != null) {
            padded = padded.withMarkers(padded.markers.add(semicolon))
        }
        return padded
    }

    private fun <K2 : J> maybeTrailingComma(k: K2): JRightPadded<K2> {
        val saveCursor = cursor
        var beforeComma: Space = whitespace()
        var comma: TrailingComma? = null
        if (cursor < source.length && skip(",")) {
            comma = TrailingComma(randomId(), whitespace())
        } else {
            beforeComma = Space.EMPTY
            cursor(saveCursor)
        }
        var padded = JRightPadded.build(k).withAfter(beforeComma)
        if (comma != null) {
            padded = padded.withMarkers(padded.markers.add(comma))
        }
        return padded
    }

    private fun <T> padLeft(left: Space, tree: T & Any): JLeftPadded<T> {
        return JLeftPadded(left, tree, Markers.EMPTY)
    }

    private fun <T> padRight(tree: T & Any, right: Space?): JRightPadded<T> {
        return JRightPadded(tree, right ?: Space.EMPTY, Markers.EMPTY)
    }

    private fun positionOfNext(untilDelim: String): Int {
        var inMultiLineComment = false
        var inSingleLineComment = false
        var delimIndex = cursor
        while (delimIndex < source.length - untilDelim.length + 1) {
            if (inSingleLineComment) {
                if (source[delimIndex] == '\n') {
                    inSingleLineComment = false
                }
            } else {
                if (source.length - untilDelim.length > delimIndex + 1) {
                    val c1 = source[delimIndex]
                    val c2 = source[delimIndex + 1]
                    if (c1 == '/') {
                        if (c2 == '/') {
                            inSingleLineComment = true
                            delimIndex++
                        } else if (c2 == '*') {
                            inMultiLineComment = true
                            delimIndex++
                        }
                    } else if (c1 == '*') {
                        if (c2 == '/') {
                            inMultiLineComment = false
                            delimIndex += 2
                        }
                    }
                }
                if (!inMultiLineComment && !inSingleLineComment) {
                    if (source.startsWith(untilDelim, delimIndex)) {
                        break // found it!
                    }
                }
            }
            delimIndex++
        }
        return if (delimIndex > source.length - untilDelim.length) -1 else delimIndex
    }

    private fun sourceBefore(untilDelim: String): Space {
        val delimIndex = positionOfNext(untilDelim)
        if (delimIndex < 0) {
            return Space.EMPTY // unable to find this delimiter
        }
        val space = Space.format(source, cursor, delimIndex)
        cursor = delimIndex + untilDelim.length // advance past the delimiter
        return space
    }

    private fun whitespace(): Space {
        val nextNonWhitespace = StringUtils.indexOfNextNonWhitespace(cursor, source)
        if (nextNonWhitespace == cursor) {
            return Space.EMPTY
        }
        val space = Space.format(source, cursor, nextNonWhitespace)
        cursor = nextNonWhitespace
        return space
    }

    companion object {
        private val augmentedAssignOperators = setOf("plusAssign", "minusAssign", "timesAssign", "divAssign", "remAssign")
    }
}
