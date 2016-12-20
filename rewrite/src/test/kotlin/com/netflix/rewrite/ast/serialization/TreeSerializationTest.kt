package com.netflix.rewrite.ast.serialization

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.netflix.rewrite.ast.Tr
import com.netflix.rewrite.parse.OracleJdkParser
import com.netflix.rewrite.parse.Parser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Test that flyweights survive a serialization/deserialization cycle
 */
class TreeSerializationTest : Parser by OracleJdkParser() {

    val mapper: ObjectMapper = ObjectMapper()
            .registerModule(KotlinModule())
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)

    @Test
    fun identFlyweights() {
        val a = parse("""
            public class A {
                A a = foo();
                A a2 = foo();

                public A foo() { return null; }
            }
        """)

        val aStr = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(a)
        println(aStr)

        val aDeser = mapper.readValue(aStr, Tr.CompilationUnit::class.java)

        assertEquals(a, aDeser)
        assertTrue(a.classes[0].type === aDeser.classes[0].type)
        assertTrue((a.classes[0].fields() + aDeser.classes[0].fields())
                .map { it.vars[0].initializer?.type }
                .toSet()
                .size == 1)
    }
}