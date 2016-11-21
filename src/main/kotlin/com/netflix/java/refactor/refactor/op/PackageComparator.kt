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
