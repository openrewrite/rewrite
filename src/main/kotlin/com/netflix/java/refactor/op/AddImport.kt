package com.netflix.java.refactor.op

class AddImport(val clazz: String): RefactorOperation {
    override val scanner = AddImportScanner(this)
}

class AddImportScanner(val op: AddImport): BaseRefactoringScanner() {
}