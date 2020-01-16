/**
 * Copyright 2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.rewrite.jackson

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.smile.SmileFactory
import com.fasterxml.jackson.dataformat.smile.SmileGenerator
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.netflix.rewrite.ast.Tr
import java.io.InputStream
import java.io.OutputStream

class TreeJacksonSerializer {

    companion object {
        val cuListType = object : TypeReference<List<Tr.CompilationUnit>>() {}
    }

    val mapper: ObjectMapper by lazy {
        val f = SmileFactory()
        f.configure(SmileGenerator.Feature.CHECK_SHARED_STRING_VALUES, true)

        ObjectMapper(f)
                .registerModule(KotlinModule())
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
    }

    fun Tr.CompilationUnit.writePretty(): String =
        ObjectMapper().registerModule(KotlinModule())
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(this)

    fun write(cus: List<Tr.CompilationUnit>, out: OutputStream) {
        mapper.writeValue(out, cus)
    }

    fun write(cus: List<Tr.CompilationUnit>): ByteArray = mapper.writeValueAsBytes(cus)

    fun write(cu: Tr.CompilationUnit, out: OutputStream) {
        mapper.writeValue(out, cu)
    }

    fun write(cu: Tr.CompilationUnit): ByteArray = mapper.writeValueAsBytes(cu)

    fun readList(input: InputStream): List<Tr.CompilationUnit> {
        return mapper.readValue<List<Tr.CompilationUnit>>(input, cuListType)
    }

    fun readList(bytes: ByteArray): List<Tr.CompilationUnit> =
            mapper.readValue<List<Tr.CompilationUnit>>(bytes, cuListType)

    fun read(input: InputStream): Tr.CompilationUnit = mapper.readValue(input, Tr.CompilationUnit::class.java)

    fun read(bytes: ByteArray): Tr.CompilationUnit = mapper.readValue(bytes, Tr.CompilationUnit::class.java)
}