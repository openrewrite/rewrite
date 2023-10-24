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

import org.jetbrains.kotlin.KtRealPsiSourceElement
import org.jetbrains.kotlin.com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.com.intellij.psi.PsiFile
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.backend.FirMetadataSource
import org.jetbrains.kotlin.fir.expressions.impl.FirElseIfTrueCondition
import org.jetbrains.kotlin.fir.expressions.impl.FirSingleExpressionBlock
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.psi.*
import org.openrewrite.java.tree.JavaType
import org.openrewrite.kotlin.KotlinIrTypeMapping

class PsiElementAssociations(val typeMapping: KotlinIrTypeMapping, private val psiFile: PsiFile, val file: IrFile) {

    private val notMapped: MutableList<IrElement> = ArrayList()
    private val psiMap: MutableMap<TextRange, MutableList<PsiElement>> = HashMap()
    private val elementMap: MutableMap<PsiElement, MutableList<IrInfo>> = HashMap()

    fun initialize() {
        var depth = 0
        object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                psiMap.computeIfAbsent(element.textRange) { ArrayList() } += element
                element.acceptChildren(this)
            }
        }.visitFile(psiFile)

        object : IrElementVisitor<Unit, MutableMap<PsiElement, MutableList<IrInfo>>> {
            override fun visitElement(element: IrElement, data: MutableMap<PsiElement, MutableList<IrInfo>>) {
                if (element is IrMetadataSourceOwner) {
                    if (element.metadata is FirMetadataSource) {
                        when (element.metadata) {
                            is FirMetadataSource.File -> {
                                val source = (element.metadata!! as FirMetadataSource.File).files[0].source
                                if (source is KtRealPsiSourceElement) {
                                    elementMap.computeIfAbsent(source.psi) { ArrayList() } += IrInfo(element, depth)
                                }
                            }

                            is FirMetadataSource.Class -> {
                                val source = (element.metadata!! as FirMetadataSource.Class).fir.source
                                if (source is KtRealPsiSourceElement) {
                                    elementMap.computeIfAbsent(source.psi) { ArrayList() } += IrInfo(element, depth)
                                }
                            }

                            is FirMetadataSource.Function -> {
                                val source = (element.metadata!! as FirMetadataSource.Function).fir.source
                                if (source is KtRealPsiSourceElement) {
                                    elementMap.computeIfAbsent(source.psi) { ArrayList() } += IrInfo(element, depth)
                                }
                            }

                            is FirMetadataSource.Property -> {
                                val source = (element.metadata!! as FirMetadataSource.Property).fir.source
                                if (source is KtRealPsiSourceElement) {
                                    elementMap.computeIfAbsent(source.psi) { ArrayList() } += IrInfo(element, depth)
                                }
                            }

                            is FirMetadataSource.Script -> {
                                val source = (element.metadata!! as FirMetadataSource.Script).fir.source
                                if (source is KtRealPsiSourceElement) {
                                    elementMap.computeIfAbsent(source.psi) { ArrayList() } += IrInfo(element, depth)
                                }
                            }
                        }
                    }
                } else if (element.startOffset >= 0 && element.endOffset >= 0) {
                    val textRange = TextRange.create(element.startOffset, element.endOffset)
                    if (psiMap.containsKey(textRange)) {
                        val psi = psiMap[textRange]
                        if (psi != null) {
                            if (psi.size == 1) {
                                elementMap.computeIfAbsent(psi.toList()[0]) { ArrayList() } += IrInfo(element, depth)
                            } else {
                                for (p in psi) {
                                    elementMap.computeIfAbsent(p) { ArrayList() } += IrInfo(element, depth)
                                }
                            }
                        }
                    }
                } else {
                    notMapped.add(element)
                }

                depth++
                element.acceptChildren(this, data)
                depth--
            }
        }.visitFile(file, elementMap)
        validate()
    }

    private fun validate() {
        // TODO
    }

    fun type(psiElement: PsiElement): JavaType? {
        val ir = primary(psiElement)
        return if (ir != null) typeMapping.type(ir) else null
    }

    fun all(psiElement: PsiElement): List<IrElement> {
        val elements = elementMap[psiElement]
        if (elements != null) {
            return elements.map { it.ir }.toList()
        }
        return emptyList()
    }

    fun primary(psiElement: PsiElement): IrElement? {
        val temp = all(psiElement) // TODO: remove
        val f: ((IrElement) -> Boolean)? = when (psiElement) {
            is KtFile -> {
                { it is IrFile }
            }

            is KtCallExpression -> {
                { it is IrConstructorCall || it is IrCall || it is IrTypeOperatorCall }
            }

            is KtClass, is KtObjectDeclaration -> {
                { it is IrClass }
            }

            is KtConstantExpression, is KtStringTemplateExpression -> {
                { it is IrConst<*> }
            }

            is KtClassLiteralExpression -> {
                { it is IrClassReference }
            }

            is KtFunction -> {
                { it is IrFunction }
            }

            is KtNameReferenceExpression -> {
                { it is IrPropertyReference || it is IrFunctionReference || it is IrGetValue || it is IrCall }
            }

            is KtParameter -> {
                {
                    it is IrValueParameter && (it.name.asString() == psiElement.name.toString() || (it.name.asString() == "<unused var>" && psiElement.name.toString() == "_")) ||
                            it is IrVariable && (it.name.asString() == psiElement.name.toString() || (it.name.asString() == "<unused var>" && psiElement.name.toString() == "_"))
                }
            }

            is KtPostfixExpression -> {
                { it is IrVariable }
            }

            is KtPrefixExpression -> {
                { it is IrVariable }
            }

            is KtProperty -> {
                { it is IrProperty || it is IrVariable }
            }

            is KtThisExpression -> {
                { it is IrGetValue }
            }

            is KtTypeParameter -> {
                { it is IrTypeParameter }
            }

            is KtVariableDeclaration -> {
                { it is IrVariable }
            }

            else -> {
                null
            }
        }

        if (f == null) {
            // TEMP: fix KotlinTreeParserVisitor.
            if (!(psiElement is KtNameReferenceExpression ||
                        psiElement is KtLiteralStringTemplateEntry ||
                        psiElement is KtUserType ||
                        psiElement is KtBinaryExpression ||
                        psiElement is KtDotQualifiedExpression)
            ) {
                // println()
            }
            return null
        } else {
            val m = elementMap[psiElement]
            if (m != null && m.stream().map { it.ir }.noneMatch(f)) {
                // println()
            }
        }
        return ir(psiElement, f)
    }

    fun ir(psi: PsiElement?, filter: (IrElement) -> Boolean): IrElement? {
        if (psi == null) {
            return null
        }
        val directIrInfos = all(psi).filter { filter.invoke(it) }
        return if (directIrInfos.isNotEmpty()) directIrInfos[0] else null
    }

    fun functionType(psiElement: PsiElement): IrFunction? {
        return ir(psiElement) { it is IrFunction } as? IrFunction ?: return null
    }

    fun functionCallType(psiElement: PsiElement): IrFunctionAccessExpression? {
        return ir(psiElement) { it is IrFunctionAccessExpression } as? IrFunctionAccessExpression ?: return null
    }

    fun propertyType(psiElement: PsiElement): IrProperty? {
        return ir(psiElement) { it is IrProperty } as? IrProperty ?: return null
    }

    fun variableType(psiElement: PsiElement): IrVariable? {
        return ir(psiElement) { it is IrVariable } as? IrVariable ?: return null
    }

    enum class ExpressionType {
        CONSTRUCTOR,
        METHOD_INVOCATION,
        RETURN_EXPRESSION
    }

    fun getFunctionCallType(psi: KtExpression): ExpressionType? {
        val ir = ir(psi) { it is IrFunctionAccessExpression } as? IrFunctionAccessExpression ?: return null
        return if (ir is IrConstructorCall) ExpressionType.CONSTRUCTOR else ExpressionType.METHOD_INVOCATION
    }

    private fun PsiElement.customToString(): String {
        return "PSI ${this.textRange} $this"
    }

    override fun toString(): String {
        val sb = StringBuilder()
//        elementMap.forEach{ (psi, firs) ->
//            sb.append(t).append("\n")
//            firs.forEach{ fir ->
//                sb.append("  - $fir\n")
//            }
//            sb.append("\n")
//        }
        return sb.toString()
    }

    private class IrInfo(
        val ir: IrElement,
        val depth: Int,
    ) {
        override fun toString(): String {
            val s = PsiTreePrinter.printIrElement(ir)
            return "FIR($depth, $s)"
        }
    }

    companion object {
        fun printElement(firElement: FirElement): String {
            if (firElement is FirSingleExpressionBlock) {
                return PsiTreePrinter.firElementToString(firElement.statement)
            } else if (firElement is FirElseIfTrueCondition) {
                return "true";
            }

            return "";
        }
    }
}