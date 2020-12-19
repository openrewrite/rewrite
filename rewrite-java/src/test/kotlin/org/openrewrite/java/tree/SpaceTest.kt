package org.openrewrite.java.tree

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SpaceTest {
    @Test
    fun singleLineComment() {
        val cf = Space.format("""
            // I'm a little // teapot
            // Short and stout //
                // Here is my handle
            \u00A0  \u00A0
        """.trimIndent())

        assertThat(cf.comments).hasSize(3)

        val (c1, c2, c3) = cf.comments

        assertThat(c1.text).isEqualTo(" I'm a little // teapot")
        assertThat(c2.text).isEqualTo(" Short and stout //")
        assertThat(c3.text).isEqualTo(" Here is my handle")

        assertThat(c1.prefix).isEmpty()
        assertThat(c2.prefix).isEqualTo("\n")
        assertThat(c3.prefix).isEqualTo("\n    ")

        assertThat(cf.whitespace).isEqualTo("""
            
            \u00A0  \u00A0""".trimIndent())
    }

    @Test
    fun multiLineComment() {
        val cf = Space.format("""
            /*   /*    Here is my spout     */
            /* When I get all steamed up */
            /* /*
            Here me shout
            */
            \u00A0  \u00A0
        """.trimIndent())

        assertThat(cf.comments).hasSize(3)

        val (c1, c2, c3) = cf.comments

        assertThat(c1.text).isEqualTo("   /*    Here is my spout     ")
        assertThat(c2.text).isEqualTo(" When I get all steamed up ")
        assertThat(c3.text).isEqualTo(" /*\nHere me shout\n")

        assertThat(c1.prefix).isEmpty()
        assertThat(c2.prefix).isEqualTo("\n")
        assertThat(c3.prefix).isEqualTo("\n")

        assertThat(cf.whitespace).isEqualTo("""
            
            \u00A0  \u00A0""".trimIndent())
    }

    @Test
    fun javadocComment() {
        val cf = Space.format("""
            /**
             * /** Tip me over and pour me out!
             */
            \u00A0  \u00A0
        """.trimIndent())

        assertThat(cf.comments).hasSize(1)
        assertThat(cf.comments.first().text).isEqualTo("\n * /** Tip me over and pour me out!\n ")

        assertThat(cf.whitespace).isEqualTo("""
            
            \u00A0  \u00A0""".trimIndent())
    }
}
