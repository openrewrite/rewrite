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
package com.netflix.java.refactor.search

import org.junit.Assert.assertTrue
import org.junit.Test

class MethodMatcherTest {
    
    @Test
    fun matchesMethodTargetType() {
        val typeRegex = { signature: String -> MethodMatcher(signature).targetTypePattern }

        assertTrue(typeRegex("*..MyClass foo()").matches("com.bar.MyClass"))
        assertTrue(typeRegex("MyClass foo()").matches("MyClass"))
        assertTrue(typeRegex("com.bar.MyClass foo()").matches("com.bar.MyClass"))
        assertTrue(typeRegex("com.*.MyClass foo()").matches("com.bar.MyClass"))
    }

    @Test
    fun matchesMethodName() {
        val nameRegex = { signature: String -> MethodMatcher(signature).methodNamePattern }

        assertTrue(nameRegex("A foo()").matches("foo"))
        assertTrue(nameRegex("A *()").matches("foo"))
        assertTrue(nameRegex("A fo*()").matches("foo"))
        assertTrue(nameRegex("A *oo()").matches("foo"))
    }

    val argRegex = { signature: String -> MethodMatcher(signature).argumentPattern }

    @Test
    fun matchesArguments() {
        assertTrue(argRegex("A foo()").matches(""))
        assertTrue(argRegex("A foo(int)").matches("int"))
        assertTrue(argRegex("A foo(java.util.Map)").matches("java.util.Map"))
    }

    @Test
    fun matchesUnqualifiedJavaLangArguments() {
        assertTrue(argRegex("A foo(String)").matches("java.lang.String"))
    }

    @Test
    fun matchesArgumentsWithWildcards() {
        assertTrue(argRegex("A foo(java.util.*)").matches("java.util.Map"))
        assertTrue(argRegex("A foo(java..*)").matches("java.util.Map"))
    }

    @Test
    fun matchesArgumentsWithDotDot() {
        assertTrue(argRegex("A foo(.., int)").matches("int"))
        assertTrue(argRegex("A foo(.., int)").matches("int,int"))

        assertTrue(argRegex("A foo(int, ..)").matches("int"))
        assertTrue(argRegex("A foo(int, ..)").matches("int,int"))

        assertTrue(argRegex("A foo(..)").matches(""))
        assertTrue(argRegex("A foo(..)").matches("int"))
        assertTrue(argRegex("A foo(..)").matches("int,int"))
    }

    @Test
    fun matchesMethodSymbolsWithVarargs() {
        argRegex("A foo(String, Object...)").matches("String,Object[]")
    }

    @Test
    fun dotDotMatchesArrayArgs() {
        argRegex("A foo(..)").matches("String,Object[]")
    }

    @Test
    fun matchesArrayArguments() {
        assertTrue(argRegex("A foo(String[])").matches("java.lang.String[]"))
    }
}