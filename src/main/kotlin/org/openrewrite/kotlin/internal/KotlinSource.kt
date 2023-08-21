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

import lombok.Getter
import lombok.Setter
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.openrewrite.Parser
import java.util.*

@Getter
class KotlinSource(
    var input: Parser.Input,
    ktFile: KtFile
) {
    var nodes: Map<Int, ASTNode>

    @Setter
    var firFile: FirFile? = null

    init {
        nodes = map(ktFile)
    }

    private fun map(ktFile: KtFile): Map<Int, ASTNode> {
        val result: MutableMap<Int, ASTNode> = LinkedHashMap()
        val visited = Collections.newSetFromMap(IdentityHashMap<PsiElement, Boolean>())
        val v: PsiElementVisitor = object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                if (!visited.add(element)) {
                    return
                }
                result[element.textRange.startOffset] = element.node
                for (child in element.children) {
                    (child as? KtElement)?.let { visitElement(it) }
                }
                if (element.nextSibling is KtElement) {
                    visitElement(element.nextSibling)
                }
            }
        }
        v.visitElement(ktFile)
        return result
    }
}
