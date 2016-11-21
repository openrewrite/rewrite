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
package com.netflix.java.refactor.ast

import org.junit.Assert.assertEquals
import org.junit.Test

class TypeTest {
    val cache = TypeCache.new()

    @Test
    fun innerClassType() {
        val t = Type.Class.build(cache, "com.foo.Foo.Bar")
        assertEquals("com.foo.Foo", t.owner.asClass()?.fullyQualifiedName)
        assertEquals("com.foo", t.owner.asClass()?.owner.asPackage()?.fullName)
    }

    @Test
    fun classType() {
        val t = Type.Class.build(cache, "com.foo.Foo")
        assertEquals("com.foo", t.owner.asPackage()?.fullName)
    }
}