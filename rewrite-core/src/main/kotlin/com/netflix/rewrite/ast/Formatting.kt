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
package com.netflix.rewrite.ast

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIdentityInfo
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.ObjectIdGenerators
import com.koloboke.collect.map.hash.HashObjObjMaps
import java.io.Serializable

/**
 * The stylistic surroundings of a tree element
 */
@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator::class, property = "@ref")
@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@c")
sealed class Formatting: Serializable {
    companion object {
        val Empty by lazy { format("") }
    }

    open fun withPrefix(prefix: String): Formatting = when(this) {
        is Infer -> format(prefix)
        is Reified -> format(prefix, suffix)
        is None -> format(prefix)
    }

    open fun withSuffix(suffix: String): Formatting = when(this) {
        is Infer -> format("", suffix)
        is Reified -> format(prefix, suffix)
        is None -> format("", suffix)
    }

    /**
     * Formatting should be inferred and reified from surrounding context
     */
    object Infer : Formatting()

    class Reified private constructor(val prefix: String, val suffix: String = "") : Formatting() {
        operator fun component1() = prefix
        operator fun component2() = suffix

        companion object {
            // suffixes are uncommon, so we'll treat them as a secondary index
            val flyweights = HashObjObjMaps.newMutableMap<String, MutableMap<String, Reified>>()

            @JvmStatic @JsonCreator
            fun build(prefix: String, suffix: String): Reified {
                return synchronized(flyweights) {
                    flyweights
                        .getOrPut(prefix, { HashObjObjMaps.newMutableMap<String, Reified>(mapOf(suffix to Reified(prefix, suffix))) })
                        .getOrPut(suffix, { Reified(prefix, suffix) })
                }
            }
        }
    }

    object None : Formatting()
}

fun format(prefix: String, suffix: String = "") = Formatting.Reified.build(prefix, suffix)