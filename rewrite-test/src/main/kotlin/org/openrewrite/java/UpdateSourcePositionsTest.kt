package org.openrewrite.java

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.PrintOutputCapture
import org.openrewrite.marker.Marker
import org.openrewrite.marker.Position

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

        val withPositions = after.print(
            object : JavaPrinter<Int>() {
                override fun <M : Marker?> visitMarker(marker: Marker, p: PrintOutputCapture<Int>): M {
                    if (marker is Position) {
                        p.append("[${marker.startPosition},${marker.length}]")
                    }
                    return super.visitMarker(marker, p)
                }
            }
        )

        assertThat(withPositions).isEqualTo("""
          [0,54][0,54]class [6,4]Test [11,43]{
              [17,5][17,3]int [21,1][21,1]n;
              
              [33,19][33,4]void [38,4]test([43,0]) [45,7]{
              }
          }
        """.trimIndent())
    }
}
