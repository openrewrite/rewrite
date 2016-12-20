package com.netflix.rewrite.ast.visitor

import com.netflix.rewrite.ast.Cursor
import com.netflix.rewrite.ast.Tree

class RetrieveCursorVisitor(val treeId: Long?) : AstVisitor<Cursor?>(null) {
    constructor(t: Tree?) : this(t?.id)

    override fun visitTree(t: Tree): Cursor? =
            if (treeId == t.id) cursor() else super.visitTree(t)
}