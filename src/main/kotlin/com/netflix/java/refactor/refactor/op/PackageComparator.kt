package com.netflix.java.refactor.refactor.op

import java.util.*

class PackageComparator: Comparator<String> {
    override fun compare(p1: String, p2: String): Int {
        val p1s = p1.split(".")
        val p2s = p2.split(".")

        p1s.forEachIndexed { i, fragment ->
            if(p2s.size < i + 1) return@compare 1
            if(fragment != p2s[i]) return@compare fragment.compareTo(p2s[i])
        }

        return if(p1s.size < p2s.size) -1 else 0
    }
}
