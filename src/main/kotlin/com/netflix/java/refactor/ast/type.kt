package com.netflix.java.refactor.ast

import com.sun.tools.javac.code.Type
import com.sun.tools.javac.tree.JCTree

fun packageOwner(fullyQualifiedClassName: String) =
    fullyQualifiedClassName.split('.').dropLastWhile { it[0].isUpperCase() }.joinToString(".")

fun className(fullyQualifiedClassName: String) =
    fullyQualifiedClassName.split('.').dropWhile { it[0].isLowerCase() }.joinToString(".")

fun Type.matches(fullyQualifiedClassName: String?) = matches(toString(), fullyQualifiedClassName)
fun JCTree.matches(fullyQualifiedClassName: String?) = matches(toString(), fullyQualifiedClassName)

private fun matches(javacTypeSerialization: String, fullyQualifiedClassName: String?) =
    if(fullyQualifiedClassName is String)
        javacTypeSerialization == "${packageOwner(fullyQualifiedClassName)}.${className(fullyQualifiedClassName).replace(".", "\$")}"
    else false