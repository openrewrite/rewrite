package com.netflix.java.refactor.refactor.op

//data class ChangeType(val from: String, val to: String): RefactorTreeVisitor() {
////    override fun scanner() = IfThenScanner(
////            ifFixesResultFrom = ChangeTypeScanner(this),
////            then = arrayOf(
////                RemoveImport(from).scanner(),
////                AddImport(to).scanner()
////            )
////    )
//
//    override fun visitIdentifier(ident: Tr.Ident): List<RefactorFix> =
//        if(ident.type.asClass()?.fullyQualifiedName == from)
//            listOf(ident.replace(className(to)))
//        else emptyList()
//}