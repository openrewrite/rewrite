package com.netflix.rewrite.ast.serialization

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.smile.SmileFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.netflix.rewrite.ast.Tr
import com.netflix.rewrite.parse.OracleJdkParser
import com.netflix.rewrite.parse.Parser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * Test that flyweights survive a serialization/deserialization cycle
 */
class TreeSerializationTest : Parser by OracleJdkParser() {

    val a = parse("""
            public class A {
                A a = foo();
                A a2 = foo();

                public A foo() { return null; }
            }
        """)

    @Test
    fun identFlyweights() {
        val mapper: ObjectMapper = ObjectMapper()
                .registerModule(KotlinModule())
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)

        val aStr = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(listOf(a))
        println(aStr)

        val aDesers: List<Tr.CompilationUnit> = mapper.readValue(aStr, object: TypeReference<List<Tr.CompilationUnit>>() {})
        val aDeser = aDesers[0]

        assertEquals(a, aDeser)
        assertTrue(a.classes[0].type === aDeser.classes[0].type)
        assertTrue((a.classes[0].fields() + aDeser.classes[0].fields())
                .map { it.vars[0].initializer?.type }
                .toSet()
                .size == 1)
    }

    /**
     * ~1,030 bytes serialized, original source is 160 bytes
     */
    @Test
    fun identFlyweightsSmileFormat() {
        val mapper: ObjectMapper = ObjectMapper(SmileFactory())
                .registerModule(KotlinModule())
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)

        val bos = ByteArrayOutputStream()
        GZIPOutputStream(bos).use { gz -> mapper.writeValue(gz, a) }

        val serialized = bos.toByteArray()
        println(serialized.size)

        val aDeser = mapper.readValue(GZIPInputStream(ByteArrayInputStream(serialized)), Tr.CompilationUnit::class.java)

        assertEquals(a, aDeser)
        assertTrue(a.classes[0].type === aDeser.classes[0].type)
        assertTrue((a.classes[0].fields() + aDeser.classes[0].fields())
                .map { it.vars[0].initializer?.type }
                .toSet()
                .size == 1)
    }
}