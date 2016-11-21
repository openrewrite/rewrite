package com.netflix.java.refactor.search

import com.netflix.java.refactor.ast.visitor.AstVisitor
import com.netflix.java.refactor.ast.Cursor
import com.netflix.java.refactor.ast.Tr

class HasImport(val clazz: String): AstVisitor<Boolean>(false) {
    override fun visitImport(import: Tr.Import): Boolean = 
            import.matches(clazz)
}