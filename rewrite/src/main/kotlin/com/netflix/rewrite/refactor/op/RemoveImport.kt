/**
 * Copyright 2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.rewrite.refactor.op

import com.netflix.rewrite.ast.*
import com.netflix.rewrite.refactor.RefactorVisitor
import com.netflix.rewrite.search.MethodMatcher
import java.util.*

class RemoveImport(val clazz: String, override val ruleName: String = "remove-import"):
        RefactorVisitor<Tr.CompilationUnit>() {

    val methodMatcher = MethodMatcher("$clazz *(..)")

    var namedImport: Tr.Import? = null
    var starImport: Tr.Import? = null

    val referencedTypes = HashSet<String>()
    val referencedMethods = HashSet<Tr.Ident>()

    val staticNamedImports = ArrayList<Tr.Import>()
    var staticStarImport: Tr.Import? = null

    val classType = Type.Class.build(clazz)

    override fun visitImport(import: Tr.Import): List<AstTransform<Tr.CompilationUnit>> {
        if (import.static) {
            if (import.qualid.target.printTrimmed() == clazz) {
                if (import.qualid.simpleName == "*")
                    staticStarImport = import
                else
                    staticNamedImports.add(import)
            }
        } else {
            if (import.qualid.printTrimmed() == clazz) {
                namedImport = import
            } else if (import.qualid.simpleName == "*" && clazz.startsWith(import.qualid.target.printTrimmed())) {
                starImport = import
            }
        }

        return emptyList()
    }

    override fun visitIdentifier(ident: Tr.Ident): List<AstTransform<Tr.CompilationUnit>> {
        if(ident.type.asClass()?.packageName() == classType.packageName())
            ident.type.asClass()?.let { referencedTypes.add(it.fullyQualifiedName) }
        return emptyList()
    }

    override fun visitMethodInvocation(meth: Tr.MethodInvocation): List<AstTransform<Tr.CompilationUnit>> {
        if(methodMatcher.matches(meth)) {
            if(meth.type?.declaringType?.fullyQualifiedName == clazz)
                referencedMethods.add(meth.name)
        }
        return super.visitMethodInvocation(meth)
    }

    override fun visitEnd(): List<AstTransform<Tr.CompilationUnit>> =
        classImportDeletions() + staticImportDeletions()

    private fun classImportDeletions(): List<AstTransform<Tr.CompilationUnit>> =
        if (namedImport is Tr.Import && referencedTypes.none { it == clazz }) {
            namedImport!!.delete()
        } else if (starImport is Tr.Import && referencedTypes.isEmpty()) {
            starImport!!.delete()
        } else if (starImport is Tr.Import && referencedTypes.size == 1) {
            transform {
                copy(imports = imports.map {
                    if(it == starImport) {
                        val classImportField = TreeBuilder.buildName(referencedTypes.first(), format(" ")) as Tr.FieldAccess
                        Tr.Import(classImportField, false, it.formatting)
                    }
                    else it
                })
            }
        } else emptyList()

    private fun staticImportDeletions(): ArrayList<AstTransform<Tr.CompilationUnit>> {
        val staticImportFixes = ArrayList<AstTransform<Tr.CompilationUnit>>()
        if(staticStarImport is Tr.Import && referencedMethods.isEmpty()) {
            staticImportFixes.addAll(staticStarImport!!.delete())
        }
        staticNamedImports.forEach { staticImport ->
            val method = staticImport.qualid.simpleName
            if(referencedMethods.none { it.simpleName == method })
                staticImportFixes.addAll(staticImport.delete())
        }
        return staticImportFixes
    }

    private fun Tr.Import.delete(): List<AstTransform<Tr.CompilationUnit>> =
        transform { copy(imports = imports - this@delete) }
}