package com.netflix.rewrite.ast

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIdentityInfo
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.ObjectIdGenerators
import com.koloboke.collect.map.hash.HashObjObjMaps

/**
 * The stylistic surroundings of a tree element
 */
@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator::class, property = "@ref")
@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@c")
sealed class Formatting {
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
                val matchingPrefixes = flyweights.getOrPut(prefix, { HashObjObjMaps.newMutableMap<String, Reified>(mapOf(suffix to Reified(prefix, suffix))) })
                return matchingPrefixes.getOrPut(suffix, { Reified(prefix, suffix) })
            }
        }
    }

    object None : Formatting()
}

fun format(prefix: String, suffix: String = "") = Formatting.Reified.build(prefix, suffix)