package com.netflix.java.refactor.ast

import java.util.*

data class TypeCache private constructor(val key: String) {
    val packagePool = HashMap<String, Type.Package>()
    val classPool = HashMap<String, Type.Class>()

    companion object {
        private val caches = WeakHashMap<String, TypeCache>()
        val random = Random()

        fun of(key: String): TypeCache = caches.getOrPut(key) { TypeCache(key) }

        fun new(): TypeCache {
            val buffer = ByteArray(5)
            random.nextBytes(buffer)
            val uid = Base64.getEncoder().encodeToString(buffer)
            val cache = TypeCache(uid)
            caches.put(uid, cache)
            return cache
        }
    }

    fun reset() {
        packagePool.clear()
        classPool.clear()
    }
}