package com.netflix.java.refactor.op

import com.netflix.java.refactor.RefactorFix
import com.netflix.java.refactor.Refactorer
import com.sun.source.tree.MemberSelectTree
import com.sun.tools.javac.tree.JCTree
import com.sun.tools.javac.util.Context
import java.util.*

class Bookmark(val id: String, val rule: Refactorer): RefactorOperation {
    override fun scanner() = BookmarkScanner(this)
    
    var whereFieldType: String? = null
    
    fun findFieldWithType(clazz: Class<Any>) = findFieldWithType(clazz.name)
    fun findFieldWithType(clazz: String): Refactorer { 
        whereFieldType = clazz
        return rule
    }
}

class BookmarkScanner(val op: Bookmark): BaseRefactoringScanner() {
    override fun visitMemberSelect(node: MemberSelectTree?, p: Context?): List<RefactorFix> {
        return super.visitMemberSelect(node, p)
    }
}

class BookmarkTable {
    val bookmarks = HashMap<String, JCTree>()
    
    @Suppress("UNCHECKED_CAST")
    fun <T: JCTree> get(id: String): T? = bookmarks[id] as T?
    
    fun save(id: String, tree: JCTree) { 
        bookmarks[id] = tree
    }
}