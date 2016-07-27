package com.netflix.java.refactor.fix

import com.netflix.java.refactor.RefactorFix
import com.netflix.java.refactor.ast.*
import com.sun.source.tree.ClassTree
import com.sun.tools.javac.tree.JCTree
import com.sun.tools.javac.util.Context

class AddField(val clazz: String, val name: String, val init: String?): RefactoringAstScannerBuilder {
    override fun scanner() = CompositeScanner(AddFieldScanner(this), AddImport(clazz).scanner())
}

class AddFieldScanner(val op: AddField): FixingScanner() {
    override fun visitClass(node: ClassTree?, p: Context?): List<RefactorFix> {
        val decl = node as JCTree.JCClassDecl
        val assignment = if(op.init is String) " = ${op.init}" else ""
        
        return listOf(insertAfter(sourceText.indexOf('{', decl.startPosition) + 1, "\n   ${className(op.clazz)} ${op.name}$assignment;"))
    }
}