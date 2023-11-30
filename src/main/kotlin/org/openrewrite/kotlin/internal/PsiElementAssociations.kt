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
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirPackageDirective
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertySetter
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirElseIfTrueCondition
import org.jetbrains.kotlin.fir.expressions.impl.FirSingleExpressionBlock
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.resolved
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitor
import org.jetbrains.kotlin.psi
import org.jetbrains.kotlin.psi.*
import org.openrewrite.java.tree.JavaType
import org.openrewrite.kotlin.KotlinTypeMapping

class PsiElementAssociations(val typeMapping: KotlinTypeMapping, val file: FirFile) {

    private val elementMap: MutableMap<PsiElement, MutableList<FirInfo>> = HashMap()
    private val typeMap: MutableMap<PsiElement, ConeTypeProjection> = HashMap()

    fun initialize() {
        var depth = 0
        object : FirDefaultVisitor<Unit, MutableMap<PsiElement, MutableList<FirInfo>>>() {
            override fun visitElement(element: FirElement, data: MutableMap<PsiElement, MutableList<FirInfo>>) {
                if (element.source != null && element.source.psi != null) {
                    val psiElement = element.source!!.psi!!
                    val firInfo = FirInfo(element, depth)
                    data.computeIfAbsent(psiElement) { ArrayList() } += firInfo
                }
                depth++
                element.acceptChildren(this, data)
                if (element is FirResolvedTypeRef) {
                    // Do not visit FirUserTypeRef, since it's not mappable to a type.
                    if (element.delegatedTypeRef != null && element.delegatedTypeRef !is FirUserTypeRef) {
                        // not sure why this isn't taken care of by `FirResolvedTypeRefImpl#acceptChildren()`
                        element.delegatedTypeRef?.accept(this, data)
                    }
                }
                depth--
            }

            override fun visitResolvedTypeRef(
                resolvedTypeRef: FirResolvedTypeRef,
                data: MutableMap<PsiElement, MutableList<FirInfo>>
            ) {
                super.visitResolvedTypeRef(resolvedTypeRef, data)
                if (resolvedTypeRef.type is ConeClassLikeType && resolvedTypeRef.type.typeArguments.isNotEmpty() && resolvedTypeRef.psi is KtTypeReference) {
                    visitType(resolvedTypeRef.type, resolvedTypeRef.psi as KtTypeReference)
                }
            }

            private fun visitType(firType: ConeTypeProjection, psiType: KtTypeReference) {
                if (firType is ConeClassLikeType) {
                    val psiTypeArguments = psiType.typeElement!!.typeArgumentsAsTypes
                    if (psiTypeArguments.size != firType.typeArguments.size) {
                        // TODO check why this is happening
                        return
                    }
                    for ((index, typeArgument) in firType.typeArguments.withIndex()) {
                        val psiTypeArgument = psiTypeArguments[index] ?: continue
                        visitType(typeArgument, psiTypeArgument)
                        typeMap[psiTypeArgument] = typeArgument
                    }
                }
            }
        }.visitFile(file, elementMap)
    }

    fun type(psiElement: PsiElement, owner: FirElement?): JavaType? {
        if (typeMap.isNotEmpty() && psiElement is KtNameReferenceExpression) {
            // TODO can / should we make this more generic?
            val type = typeMap[psiElement.parent.parent]
            if (type != null) {
                return typeMapping.type(type, owner)
            }
        }
        val fir = primary(psiElement)
        return if (fir != null) typeMapping.type(fir, owner) else null
    }

    fun primary(psiElement: PsiElement) =
        fir(psiElement) { it.source is KtRealPsiSourceElement }

    fun methodDeclarationType(psi: PsiElement): JavaType.Method? {
        return when (val fir = primary(psi)) {
            is FirFunction -> typeMapping.methodDeclarationType(fir, null)
            is FirAnonymousFunctionExpression -> typeMapping.methodDeclarationType(fir.anonymousFunction, null)
            else -> {
                null
            }
        }
    }

    @OptIn(SymbolInternals::class)
    fun methodInvocationType(psi: PsiElement): JavaType.Method? {
        return when (psi) {
            is KtDestructuringDeclarationEntry -> {
                val fir = fir(psi) { it is FirComponentCall }
                when (fir) {
                    is FirFunctionCall -> typeMapping.methodInvocationType(fir)
                    else -> {
                        null
                    }
                }
            }
            else -> {
                when (val fir = primary(psi)) {
                    is FirResolvedNamedReference -> {
                        when (val sym = fir.resolvedSymbol) {
                            is FirFunctionSymbol<*> -> typeMapping.methodDeclarationType(sym.fir, null)
                            else -> {
                                null
                            }
                        }
                    }

                    is FirFunctionCall -> {
                        typeMapping.methodInvocationType(fir)
                    }

                    is FirSafeCallExpression -> {
                        when (val selector = fir.selector) {
                            is FirFunctionCall -> typeMapping.methodInvocationType(selector)
                            else -> {
                                null
                            }
                        }
                    }

                    is FirArrayOfCall -> {
                        // `FirArrayOfCall` is not a `FirFunctionCall`, so a `JavaType$Method` is impossible.
                        // The expression contains a type ref of the parameterized type, but cannot be added to a `MethodInvocation`.
                        null
                    }
                    else -> {
                        null
                    }
                }
            }
        }
    }

    fun primitiveType(psi: PsiElement): JavaType.Primitive {
        return when (val fir = primary(psi)) {
            is FirConstExpression<*> -> {
                typeMapping.primitive(fir)
            }
            else -> JavaType.Primitive.None
        }
    }

    @OptIn(SymbolInternals::class)
    fun variableType(psi: PsiElement, parent: FirElement?): JavaType.Variable? {
        return when (val fir = primary(psi)) {
            is FirVariable -> typeMapping.variableType(fir, parent)
            is FirResolvedNamedReference -> {
                when (val sym = fir.resolvedSymbol) {
                    is FirVariableSymbol<*> -> typeMapping.variableType(sym.fir, null)
                    else -> null
                }
            }
            is FirErrorNamedReference, is FirPackageDirective -> null
            else -> null
        }
    }

    fun fir(psi: PsiElement?, filter: (FirElement) -> Boolean) : FirElement? {
        var p = psi
        while (p != null && !elementMap.containsKey(p)) {
            p = p.parent
        }

        if (p == null || p is KtPackageDirective) {
            return null
        }

        val allFirInfos = elementMap[p]!!
        val directFirInfos = allFirInfos.filter { filter.invoke(it.fir) }
        return if (directFirInfos.isNotEmpty())
            // It might be more reliable to have explicit mappings in case something changes.
            return when {
                directFirInfos.size == 1 -> directFirInfos[0].fir
                else -> {
                    return when (p) {
                        is KtConstantExpression -> {
                            directFirInfos.firstOrNull { it.fir is FirConstExpression<*> }?.fir
                        }
                        is KtImportDirective -> {
                            directFirInfos.firstOrNull { it.fir is FirImport && it.fir !is FirErrorImport }?.fir
                        }
                        is KtNamedFunction -> {
                            val found = directFirInfos.firstOrNull { it.fir is FirFunction }?.fir
//                            if (found == null) {
//                                // Review how to expose unmatched types without causing an error.
//                            }
                            found
                        }
                        is KtNameReferenceExpression, is KtTypeReference -> {
                            val found = directFirInfos.firstOrNull { it.fir is FirResolvedTypeRef || it.fir is FirResolvedNamedReference }?.fir
//                            if (found == null) {
//                                // Review how to expose unmatched types without causing an error.
//                            }
                            found
                        }
                        is KtPropertyAccessor -> {
                            val found = directFirInfos.firstOrNull { it.fir is FirDefaultPropertySetter }?.fir
//                            if (found == null) {
//                                // Review how to expose unmatched types without causing an error.
//                            }
                            found
                        }
                        else -> {
                            directFirInfos[0].fir
                        }
                    }
                }
            }
        else if (allFirInfos.isNotEmpty()) {
            return when {
                allFirInfos.size == 1 -> allFirInfos[0].fir
                // There isn't a RealPsiElement associated to the KT, so, we find the associated FIR element.
                p is KtArrayAccessExpression -> allFirInfos.firstOrNull { it.fir is FirResolvedNamedReference && (it.fir.name.asString() == "get" || it.fir.name.asString() == "set") }?.fir
                p is KtPrefixExpression -> allFirInfos.firstOrNull { it.fir is FirVariableAssignment }?.fir
                p is KtPostfixExpression -> allFirInfos.firstOrNull { it.fir is FirResolvedTypeRef || it.fir is FirFunctionCall }?.fir
                p is KtTypeReference -> allFirInfos.firstOrNull { it.fir is FirResolvedTypeRef }?.fir
                p is KtWhenConditionInRange || p is KtBinaryExpression -> allFirInfos.firstOrNull { it.fir is FirFunctionCall }?.fir
                else -> {
                    throw IllegalStateException("Unable to determine the FIR element associated to the PSI." + if (psi == null) "null element" else "original PSI: ${psi.javaClass.name}, mapped PSI: ${p.javaClass.name}")
                }
            }
        }
        else
            null
    }

    enum class ExpressionType {
        CONSTRUCTOR,
        METHOD_INVOCATION,
        QUALIFIER
    }

    fun getCallType(psi: KtExpression): ExpressionType? {
        val fir = primary(psi) ?: return null
        return when (fir) {
            is FirResolvedQualifier -> ExpressionType.QUALIFIER
            is FirArrayOfCall -> ExpressionType.METHOD_INVOCATION
            is FirFunctionCall -> {
                if (fir.calleeReference is FirErrorNamedReference)
                    return null

                when (fir.calleeReference.resolved?.resolvedSymbol) {
                    is FirConstructorSymbol -> ExpressionType.CONSTRUCTOR
                    is FirNamedFunctionSymbol -> ExpressionType.METHOD_INVOCATION
                    else -> throw UnsupportedOperationException("Unsupported resolved symbol: ${fir.calleeReference.resolved?.resolvedSymbol?.javaClass}")
                }
            }
            is FirSafeCallExpression -> {
                return when (fir.selector) {
                    is FirFunctionCall -> when (fir.selector.calleeReference?.resolved?.resolvedSymbol) {
                        is FirConstructorSymbol -> ExpressionType.CONSTRUCTOR
                        is FirNamedFunctionSymbol -> ExpressionType.METHOD_INVOCATION
                        else -> null
                    }

                    else -> null
                }
            }
            else -> throw UnsupportedOperationException("Unsupported call type: ${fir.javaClass}")
        }
    }

    private fun PsiElement.customToString(): String {
        return "PSI ${this.textRange} $this"
    }

    override fun toString(): String {
        val sb = StringBuilder()
        elementMap.forEach{ (psi, firs) ->
            sb.append(psi.customToString()).append("\n")
            firs.forEach{ fir ->
                sb.append("  - $fir\n")
            }
            sb.append("\n")
        }
        return sb.toString()
    }

    private class FirInfo(
        val fir: FirElement,
        val depth: Int,
    ) {
        override fun toString(): String {
            val s = PsiTreePrinter.printFirElement(fir)
            return "FIR($depth, $s)"
        }
    }

    companion object {
        fun printElement(firElement: FirElement) : String {
            if (firElement is FirSingleExpressionBlock) {
                return PsiTreePrinter.firElementToString(firElement.statement) ?: ""
            } else  if (firElement is FirElseIfTrueCondition) {
                return "true"
            }

            return ""
        }
    }
}