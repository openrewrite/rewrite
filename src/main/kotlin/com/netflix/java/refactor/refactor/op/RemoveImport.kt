package com.netflix.java.refactor.refactor.op

import com.netflix.java.refactor.ast.*
import com.netflix.java.refactor.refactor.RefactorVisitor
import com.netflix.java.refactor.search.MethodMatcher
import java.util.*

class RemoveImport(val cu: Tr.CompilationUnit, val clazz: String): RefactorVisitor() {
    val methodMatcher = MethodMatcher("$clazz *(..)")

    var namedImport: Tr.Import? = null
    var starImport: Tr.Import? = null

    var referencedTypes = HashSet<Type.Class>()
    var referencedMethods = HashSet<Tr.Ident>()

    var staticNamedImports = ArrayList<Tr.Import>()
    var staticStarImport: Tr.Import? = null

    val classType = Type.Class.build(cu.typeCache(), clazz)

    override fun visitImport(import: Tr.Import): List<AstTransform<*>> {
        if (import.static) {
            if (import.qualid.target.printTrimmed() == clazz) {
                if (import.qualid.name.name == "*")
                    staticStarImport = import
                else
                    staticNamedImports.add(import)
            }
        } else {
            if (import.qualid.printTrimmed() == clazz) {
                namedImport = import
            } else if (import.qualid.name.name == "*" && clazz.startsWith(import.qualid.target.printTrimmed())) {
                starImport = import
            }
        }

        return emptyList()
    }

    override fun visitIdentifier(ident: Tr.Ident): List<AstTransform<*>> {
        if(ident.type.asClass()?.packageOwner() == classType.packageOwner())
            ident.type.asClass()?.let { referencedTypes.add(it) }
        return emptyList()
    }

    override fun visitMethodInvocation(meth: Tr.MethodInvocation): List<AstTransform<*>> {
        if(methodMatcher.matches(meth)) {
            if(meth.declaringType?.fullyQualifiedName == clazz)
                referencedMethods.add(meth.name)
        }
        return super.visitMethodInvocation(meth)
    }

    override fun visitEnd(): List<AstTransform<*>> =
        classImportDeletions() + staticImportDeletions()

    private fun classImportDeletions() =
        if (namedImport is Tr.Import && referencedTypes.none { it == classType }) {
            listOf(namedImport!!.delete())
        } else if (starImport is Tr.Import && referencedTypes.isEmpty()) {
            listOf(starImport!!.delete())
        } else if (starImport is Tr.Import && referencedTypes.size == 1) {
            listOf(AstTransform<Tr.CompilationUnit>(cursor()) {
                copy(imports = imports.map {
                    if(it == starImport) {
                        val classImportField = TreeBuilder.buildName(cu.typeCache(), referencedTypes.first().fullyQualifiedName, Formatting.Reified(" ")) as Tr.FieldAccess
                        Tr.Import(classImportField, false, it.formatting)
                    }
                    else it
                })
            })
        } else emptyList()

    private fun staticImportDeletions(): ArrayList<AstTransform<*>> {
        val staticImportFixes = ArrayList<AstTransform<*>>()
        if(staticStarImport is Tr.Import && referencedMethods.isEmpty()) {
            staticImportFixes.add(staticStarImport!!.delete())
        }
        staticNamedImports.forEach { staticImport ->
            val method = staticImport.qualid.name.name
            if(referencedMethods.none { it.name == method })
                staticImportFixes.add(staticImport.delete())
        }
        return staticImportFixes
    }

    private fun Tr.Import.delete(): AstTransform<Tr.CompilationUnit> =
        AstTransform(cursor()) { copy(imports = imports - this@delete) }
}