package com.netflix.java.refactor.refactor.op

import com.netflix.java.refactor.ast.*
import java.util.ArrayList
import com.netflix.java.refactor.refactor.RefactorVisitor

class AddImport(val cu: Tr.CompilationUnit, val clazz: String, val staticMethod: String? = null): RefactorVisitor() {
    private var coveredByExistingImport = false
    private val packageComparator = PackageComparator()
    private val classType = Type.Class.build(cu.typeCache(), clazz)

    override fun visitImport(import: Tr.Import): List<AstTransform<*>> {
        val importedType = import.qualid.name.name

        if (addingStaticImport()) {
            if (import.matches(clazz) && import.static && (importedType == staticMethod || importedType == "*")) {
                coveredByExistingImport = true
            }
        }
        else {
            if (import.matches(clazz)) {
                coveredByExistingImport = true
            } else if (import.qualid.target.printTrimmed() == classType.packageOwner() && importedType == "*") {
                coveredByExistingImport = true
            }
        }

        return emptyList()
    }

    override fun visitEnd(): List<AstTransform<*>> {
        if(classType.packageOwner().isEmpty())
            return emptyList()

        val lastPrior = lastPriorImport()
        val classImportField = TreeBuilder.buildName(cu.typeCache(), clazz, Formatting.Reified(" ")) as Tr.FieldAccess

        val importStatementToAdd = if(addingStaticImport()) {
            Tr.Import(Tr.FieldAccess(classImportField, Tr.Ident(staticMethod!!, null, Formatting.Reified.Empty), null, Formatting.Reified.Empty), true, Formatting.Infer)
        } else Tr.Import(classImportField, false, Formatting.Infer)

        return if(coveredByExistingImport) {
            emptyList()
        }
        else if(lastPrior == null) {
            listOf(AstTransform<Tr.CompilationUnit>(cursor()) {
                copy(imports = listOf(importStatementToAdd) + cu.imports)
            })
        }
        else {
            listOf(AstTransform<Tr.CompilationUnit>(cursor()) {
                copy(imports = cu.imports.takeWhile { it !== lastPrior } + listOf(lastPrior, importStatementToAdd) +
                        cu.imports.takeLastWhile { it !== lastPrior })
            })
        }
    }

    fun lastPriorImport(): Tr.Import? {
        return cu.imports.lastOrNull { import ->
            // static imports go after all non-static imports
            if(addingStaticImport() && !import.static)
                return@lastOrNull true

            // non-static imports should always go before static imports
            if(!addingStaticImport() && import.static)
                return@lastOrNull false

            val comp = packageComparator.compare(import.qualid.target.printTrimmed(),
                    if(addingStaticImport()) clazz else classType.packageOwner())
            if(comp == 0) {
                if(import.qualid.name.name < if(addingStaticImport()) staticMethod!! else classType.className()) {
                    true
                }
                else false
            }
            else if(comp < 0) true
            else false
        }
    }

    fun addingStaticImport() = staticMethod is String
}