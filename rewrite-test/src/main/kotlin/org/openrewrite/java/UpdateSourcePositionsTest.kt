/*
 * Copyright 2021 the original author or authors.
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
import org.openrewrite.PrintOutputCapture
import org.openrewrite.marker.Marker
import org.openrewrite.marker.Range

interface UpdateSourcePositionsTest : JavaRecipeTest {

    @Test
    fun updateSourcePositions(jp: JavaParser) {
        val cu = jp.parse(
            """ 
                class Test {
                    int n;
                    
                    void test() {
                    }
                }
            """.trimIndent()
        )

        val after = UpdateSourcePositions().run(cu)[0].after!!

        val withOffsetAndLength = after.print(
            object : JavaPrinter<Int>() {
                override fun <M : Marker> visitMarker(marker: Marker, p: PrintOutputCapture<Int>): M {
                    if (marker is Range) {
                        p.append("[${marker.start.offset},${marker.length()}]")
                    }
                    return super.visitMarker(marker, p)
                }
            }
        )

        assertThat(withOffsetAndLength).isEqualTo("""
            [0,54][0,54]class [6,4]Test [11,43]{
                [17,5][17,3]int [21,1][21,1]n;
                
                [33,19][33,4]void [38,4]test([43,0]) [45,7]{
                }
            }
        """.trimIndent())

        val withLineAndColumn = after.print(
            object : JavaPrinter<Int>() {
                override fun <M : Marker> visitMarker(marker: Marker, p: PrintOutputCapture<Int>): M {
                    if (marker is Range) {
                        p.append("[(${marker.start.line}, ${marker.start.column}), (${marker.end.line}, ${marker.end.column})]")
                    }
                    return super.visitMarker(marker, p)
                }
            }
        )

        assertThat(withLineAndColumn).isEqualTo("""
            [(0, 0), (4, 7)][(0, 0), (4, 7)]class [(0, 6), (0, 10)]Test [(0, 11), (4, 7)]{
                [(1, 4), (1, 9)][(1, 4), (1, 7)]int [(1, 8), (1, 9)][(1, 8), (1, 9)]n;
                
                [(3, 4), (4, 5)][(3, 4), (3, 8)]void [(3, 9), (3, 13)]test([(3, 14), (3, 14)]) [(3, 16), (4, 5)]{
                }
            }
        """.trimIndent())

    }
}
