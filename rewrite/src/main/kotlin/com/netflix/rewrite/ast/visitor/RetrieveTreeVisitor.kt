package com.netflix.rewrite.ast.visitor

import com.netflix.rewrite.ast.Tree

class RetrieveTreeVisitor(val treeId: Long?) : AstVisitor<Tree?>(null) {

    override fun visitTree(t: Tree): Tree? =
            if (treeId == t.id) t else super.visitTree(t)
}