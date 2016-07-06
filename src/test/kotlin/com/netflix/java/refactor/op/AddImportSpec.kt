package com.netflix.java.refactor.op

import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.test.fail

class AddImportSpec {
    @JvmField @Rule
    val temp = TemporaryFolder()
    
    @Test
    fun addNamedImport() {
        fail()
    }
    
    @Test
    fun doNotAddImportIfAlreadyExists() {
        fail()
    }
    
    @Test
    fun doNotAddImportIfCoveredByStarImport() {
        fail()
    }
    
    @Test
    fun addNamedImportIfStarStaticImportExists() {
        fail()
    }
}
