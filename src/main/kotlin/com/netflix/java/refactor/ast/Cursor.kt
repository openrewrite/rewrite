package com.netflix.java.refactor.ast

data class Cursor(val path: List<Tree>) {
    fun plus(t: Tree) = copy(path + t)
    operator fun plus(cursor: Cursor) = copy(path + cursor.path)

    fun parent() = copy(path.dropLast(1))
    fun last() = path.last()
    
    companion object {
        val Empty = Cursor(emptyList())
    }

    override fun equals(other: Any?): Boolean = if(other is Cursor && path.size == other.path.size) {
        path.forEachIndexed { i, tree ->
            if(other.path[i] != path[i])
                return@equals false
        }
        true
    } else false

    override fun hashCode(): Int = path.hashCode()
}
