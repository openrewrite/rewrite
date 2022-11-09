/*
 * Copyright 2022 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

interface CountLinesTest {

    @Test
    fun countsLines() {
        val jp = JavaParser.fromJavaVersion().build();
        val tree = jp.parse("""
            package com.whatever;
            
            import java.util.List;
            
            // comments don't count
            class A {
                
                
                List<String> foo() {
                }
            }
            
            // EOF doesn't count
        """.trimIndent())[0]
        val lines = CountLinesVisitor.countLines(tree)
        assertThat(lines).isEqualTo(4)
    }
}
