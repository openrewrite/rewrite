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

import org.jetbrains.kotlin.com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.com.intellij.psi.PsiFile
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.expressions.impl.FirElseIfTrueCondition
import org.jetbrains.kotlin.fir.expressions.impl.FirSingleExpressionBlock
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.psi.KtClass
import org.openrewrite.java.tree.JavaType
import org.openrewrite.kotlin.KotlinIrTypeMapping

// Temp: will replace PsiElementAssociations
class PsiElementAssociations2(private val typeMapping: KotlinIrTypeMapping, private val psiFile: PsiFile, val file: IrFile) {

    private val psiMap: MutableMap<TextRange, MutableSet<PsiElement>> = HashMap()
    private val elementMap: MutableMap<PsiElement, MutableSet<IrInfo>> = HashMap()

    fun initialize() {
        var depth = 0
        object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                psiMap.computeIfAbsent(element.textRange) { HashSet() } += element
                element.acceptChildren(this)
            }
        }.visitFile(psiFile)

        object : IrElementVisitor<Unit, MutableMap<PsiElement, MutableSet<IrInfo>>> {
            override fun visitElement(element: IrElement, data: MutableMap<PsiElement, MutableSet<IrInfo>>) {
                try {
                    if (element !is IrBlockBody && element !is IrExpressionBody) {
                        val textRange = TextRange.create(element.startOffset, element.endOffset)
                        val psiElements = psiMap[textRange]
                        if (psiElements != null) {
                            for (psi in psiElements) {
                                data.computeIfAbsent(psi) { HashSet() } += IrInfo(element, depth)
                            }
                        }
                    }
                } catch (ignored: Exception) {
                    // Generated elements like `equals` and `toString` have illegal start and end offsets,
                    // but are accessible through the real IR element.
                }

                depth++
                element.acceptChildren(this, data)
                depth--
            }
        }.visitFile(file, elementMap)
        validate()
    }

    private fun validate() {
//        var found1ToNMapping = false
//        elementMap.forEach { (psi, firList) ->
//            var fakeCount = 0
//            var realCount = 0
//            var otherCount = 0
//            for (firElement in firList) {
//                if (firElement.fir.source is KtRealPsiSourceElement) {
//                    realCount++
//                } else if (firElement.fir.source is KtFakeSourceElement) {
//                    fakeCount++
//                } else {
//                    otherCount++
//                }
//            }
//
//            // print out logs, debug purpose only, to be removed after complete parser
//            if (false) {
//                found1ToNMapping = realCount > 1
//
//                println("---------")
//                println("PSI: $psi")
//                println("FIR: $firList")
//
//                println("Found 1 to $realCount Real mapping!")
//                println("    types from $realCount Real elements:")
//                var firstUnknown = false
//                var hasNonUnknown = false
//                for ((index, firElement) in firList.withIndex()) {
//                    if (firElement.fir.source is KtRealPsiSourceElement) {
//                        val type = typeMapping.type(firElement.fir).toString()
//                        if (index == 0 && type.equals("Unknown")) {
//                            firstUnknown = true
//                        }
//
//                        if (!type.equals("Unknown")) {
//                            hasNonUnknown = true
//                        }
//
//                        val padded = "        -$type".padEnd(30, ' ')
//                        println("$padded - $firElement")
//                    }
//                }
//
//                if (firstUnknown && hasNonUnknown) {
//                    throw IllegalArgumentException("First type is Unknown!")
//                }
//
//                println("    types from $fakeCount Fake elements:")
//                for (firElement in firList) {
//                    if (firElement.fir.source is KtFakeSourceElement) {
//                        val type = typeMapping.type(firElement.fir).toString()
//                        val padded = "        -$type".padEnd(30, ' ')
//                        println("$padded - $firElement")
//
//                    }
//                }
//            }
//        }
//
//        if (found1ToNMapping) {
//            // throw IllegalArgumentException("Found 1 to N real mapping!")
//        }
    }

    fun type(psiElement: PsiElement): JavaType? {
        val ir = primary(psiElement)
        return if (ir != null) typeMapping.type(ir) else null
    }

//    fun symbol(psi: KtDeclaration?): FirBasedSymbol<*>? {
//        val fir = fir(psi) { it is FirDeclaration }
//        return if (fir != null) (fir as FirDeclaration).symbol else null
//    }
//
//    fun symbol(psi: KtExpression?): FirBasedSymbol<*>? {
//        val fir = fir(psi) { it is FirResolvedNamedReference }
//        return if (fir is FirResolvedNamedReference) fir.resolvedSymbol else null
//    }
//
    fun primary(psiElement: PsiElement) =
        ir(psiElement) {
            when (psiElement) {
                is KtClass -> {
                    it is IrClass
                }

                else -> {
                    it is IrElement
                }
            }
        }

//    fun component(psiElement: PsiElement) =
//        fir(psiElement) { it is FirFunctionCall}

    fun ir(psi: PsiElement?, filter: (IrElement) -> Boolean) : IrElement? {
        var p = psi
        while (p != null && !elementMap.containsKey(p)) {
            p = p.parent
        }

        if (p == null) {
            return null
        }

        val allIrInfos = elementMap[p]!!
        val directIrInfos = allIrInfos.filter { filter.invoke(it.ir) }
        return if (directIrInfos.isNotEmpty())
            directIrInfos[0].ir
        else if (directIrInfos.isNotEmpty())
            directIrInfos[0].ir
        else
            null
    }

    enum class ExpressionType {
        CONSTRUCTOR,
        METHOD_INVOCATION,
        RETURN_EXPRESSION
    }

//    fun getFunctionType(psi: KtExpression): ExpressionType? {
//        val fir = fir(psi) { it is FirFunctionCall } as? FirFunctionCall
//
//        if (fir == null) {
//            return null
//        }
//
//        return if (fir.calleeReference.resolved != null) {
//            when (fir.calleeReference.resolved!!.resolvedSymbol) {
//                is FirConstructorSymbol -> ExpressionType.CONSTRUCTOR
//                else -> ExpressionType.METHOD_INVOCATION
//            }
//        } else {
//            throw UnsupportedOperationException("Null resolved symbol on FirFunctionCall: $psi")
//        }
//    }

//    @OptIn(DfaInternals::class)
//    fun getExpressionType(psi: KtExpression): ExpressionType? {
//        val fir = fir(psi) { it is FirExpression }
//        return if (fir is FirReturnExpression) {
//            ExpressionType.RETURN_EXPRESSION
//        } else {
//            // TODO, other expression type if needed
//            null
//        }
//    }

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
        fun printElement(firElement: FirElement) : String {
            if (firElement is FirSingleExpressionBlock) {
                return PsiTreePrinter.firElementToString(firElement.statement)
            } else  if (firElement is FirElseIfTrueCondition) {
                return "true";
            }

            return "";
        }
    }
}