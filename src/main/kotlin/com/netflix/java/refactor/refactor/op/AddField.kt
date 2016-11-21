package com.netflix.java.refactor.refactor.op

import com.netflix.java.refactor.ast.AstTransform
import com.netflix.java.refactor.ast.Formatting
import com.netflix.java.refactor.ast.Tr
import com.netflix.java.refactor.ast.Type
import com.netflix.java.refactor.refactor.RefactorVisitor

class AddField(val cu: Tr.CompilationUnit,
               val modifiers: List<Tr.VariableDecls.Modifier>,
               val target: Tr.ClassDecl,
               val clazz: String,
               val name: String,
               val init: String?): RefactorVisitor() {

    private val classType = Type.Class.build(cu.typeCache(), clazz)

    override fun visitClassDecl(classDecl: Tr.ClassDecl): List<AstTransform<*>> =
        if(classDecl.id == target.id) {
            val newField = Tr.VariableDecls(
                    emptyList(),
                    modifiers,
                    Tr.Ident(classType.className(), classType, Formatting.Reified.Empty),
                    null,
                    emptyList(),
                    listOf(Tr.VariableDecls.NamedVar(
                            Tr.Ident(name, null, Formatting.Reified("", if(init != null) " " else "")),
                            emptyList(),
                            init?.let { Tr.UnparsedSource(it, Formatting.Reified(" ")) },
                            classType,
                            Formatting.Reified(" ")
                    )),
                    Formatting.Infer
            )

            listOf(AstTransform<Tr.Block<*>>(cursor().plus(classDecl.body)) {
                copy(statements = listOf(newField) + statements)
            })
        }
        else super.visitClassDecl(classDecl)
}