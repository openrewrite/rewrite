package com.netflix.java.refactor.ast

data class AstTransform<T: Tree>(val cursor: Cursor, val mutation: T.() -> T)