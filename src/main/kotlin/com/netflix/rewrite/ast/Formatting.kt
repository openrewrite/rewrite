package com.netflix.rewrite.ast

import com.koloboke.collect.map.hash.HashObjObjMaps

/**
 * The stylistic surroundings of a tree element
 */
sealed class Formatting {
    companion object {
        val Empty = Reified.build("")
    }

    open fun withPrefix(prefix: String): Formatting = when(this) {
        is Infer -> Reified.build(prefix, "")
        is Reified -> Reified.build(prefix, suffix)
        is None -> Reified.build(prefix, "")
    }

    /**
     * Formatting should be inferred and reified from surrounding context
     */
    object Infer : Formatting()

    data class Reified private constructor(val prefix: String, val suffix: String = "") : Formatting() {
        companion object {
            // suffixes are uncommon, so we'll treat them as a secondary index
            private val flyweights = HashObjObjMaps.newMutableMap<String, MutableMap<String, Reified>>()

            fun build(prefix: String, suffix: String = ""): Reified =
                flyweights
                        .getOrPut(prefix, { HashObjObjMaps.newMutableMap<String, Reified>(mapOf(suffix to Reified(prefix, suffix))) })
                        .getOrPut(suffix, { Reified(prefix, suffix) })
        }
    }

    object None : Formatting()
}

fun format(prefix: String, suffix: String = "") = Formatting.Reified.build(prefix, suffix)