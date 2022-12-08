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
package org.openrewrite.properties

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.Issue
import org.openrewrite.PrintOutputCapture
import org.openrewrite.properties.internal.PropertiesPrinter
import org.openrewrite.properties.tree.Properties

@Suppress("UnusedProperty")
class PropertiesParserTest {
    @Test
    fun noEndOfLine() {
        val props = PropertiesParser().parse(
            """
            key=value
        """.trimIndent()
        )[0]

        val entries = props.content.map { it as Properties.Entry }
        assertThat(entries).hasSize(1)
        val entry = entries[0]
        assertThat(entry.key).isEqualTo("key")
        assertThat(entry.value.text).isEqualTo("value")
        assertThat(props.eof).isEqualTo("")
    }

    @Test
    fun endOfLine() {
        val props = PropertiesParser().parse("key=value\n")[0]

        val entries = props.content.map { it as Properties.Entry }
        assertThat(entries).hasSize(1)
        val entry = entries[0]
        assertThat(entry.key).isEqualTo("key")
        assertThat(entry.value.text).isEqualTo("value")
        assertThat(props.eof).isEqualTo("\n")
    }

    @Test
    fun endOfFile() {
        val props = PropertiesParser().parse("key=value\n\n")[0]
        val entries = props.content.map { it as Properties.Entry }
        assertThat(entries).hasSize(1)
        val entry = entries[0]
        assertThat(entry.key).isEqualTo("key")
        assertThat(entry.value.text).isEqualTo("value")
        assertThat(props.eof).isEqualTo("\n\n")
    }

    @Test
    @Suppress("WrongPropertyKeyValueDelimiter")
    fun garbageEndOfFile() {
        val props = PropertiesParser().parse("key=value\nasdf\n")[0]
        val entries = props.content.map { it as Properties.Entry }
        assertThat(entries).hasSize(1)
        val entry = entries[0]
        assertThat(entry.key).isEqualTo("key")
        assertThat(entry.value.text).isEqualTo("value")
        assertThat(props.eof).isEqualTo("\nasdf\n")
    }

    @Test
    fun commentThenEntry() {
        val props = PropertiesParser().parse(
            """
            # this is a comment
            key=value
        """.trimIndent()
        )[0]

        val comment = props.content[0] as Properties.Comment
        assertThat(comment.message).isEqualTo(" this is a comment")
        val entry = props.content[1] as Properties.Entry
        assertThat(entry.prefix).isEqualTo("\n")
        assertThat(entry.key).isEqualTo("key")
        assertThat(entry.value.text).isEqualTo("value")
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2499")
    @Test
    fun commentThenEntryByExclamationMark() {
        val props = PropertiesParser().parse(
            """
            ! this is a comment
            key=value
        """.trimIndent()
        )[0]

        val comment = props.content[0] as Properties.Comment
        assertThat(comment.message).isEqualTo(" this is a comment")
        val entry = props.content[1] as Properties.Entry
        assertThat(entry.prefix).isEqualTo("\n")
        assertThat(entry.key).isEqualTo("key")
        assertThat(entry.value.text).isEqualTo("value")
    }

    @Test
    fun entryCommentEntry() {
        val props = PropertiesParser().parse(
            """
            key1=value1
            # comment
            key2=value2
        """.trimIndent()
        )[0]

        assertThat(props.content).hasSize(3)
        val entry1 = props.content[0] as Properties.Entry
        assertThat(entry1.key).isEqualTo("key1")
        assertThat(entry1.value.text).isEqualTo("value1")
        assertThat(entry1.prefix).isEqualTo("")
        val comment = props.content[1] as Properties.Comment
        assertThat(comment.prefix).isEqualTo("\n")
        assertThat(comment.message).isEqualTo(" comment")
        val entry2 = props.content[2] as Properties.Entry
        assertThat(entry2.prefix).isEqualTo("\n")
        assertThat(entry2.key).isEqualTo("key2")
        assertThat(entry2.value.text).isEqualTo("value2")
    }

    @Test
    fun multipleEntries() {
        val props = PropertiesParser().parse(
            """
            key=value
            key2 = value2
        """.trimIndent()
        )[0]

        assertThat(props.content.map { it as Properties.Entry }.map { it.key })
            .hasSize(2).containsExactly("key", "key2")
        assertThat(props.content.map { it as Properties.Entry }.map { it.value.text })
            .hasSize(2).containsExactly("value", "value2")
    }

    @Suppress("WrongPropertyKeyValueDelimiter", "TrailingSpacesInProperty")
    @Issue("https://github.com/openrewrite/rewrite/issues/2473")
    @Test
    fun escapedNewLines() {
        val source = """
                \
                k\
                 e\
                  y \
                   = \
                    v\
                     al\
                      ue\
                
                key2=value2
            """.trimIndent()
        val props = PropertiesParser().parse(source)[0] as Properties.File

        assertThat(props.content.map { it as Properties.Entry }.map { it.key })
            .hasSize(2)
            .containsExactly("key", "key2")
        assertThat(props.content.map { it as Properties.Entry }.map { it.value.text })
            .hasSize(2)
            .containsExactly("value", "value2")

        val outputCapture = PrintOutputCapture(InMemoryExecutionContext())
        val printer = PropertiesPrinter<InMemoryExecutionContext>()
        printer.visitFile(props, outputCapture)
        assertThat(outputCapture.out.toString()).isEqualTo(source)
    }

    @Disabled("Requires support for CRLF")
    @Suppress("WrongPropertyKeyValueDelimiter", "TrailingSpacesInProperty")
    @Issue("https://github.com/openrewrite/rewrite/issues/2473")
    @Test
    fun escapedNewLinesCRLF() {
        val source = "" +
                "\\\r\n" +
                "k\\\r\n" +
                " e\\\r\n" +
                "  y \\\r\n" +
                "   = \\\r\n" +
                "    v\\\r\n" +
                "     al\\\r\n" +
                "      ue\\\r\n" +
                "\r\n" +
                "key2=value2"

        val props = PropertiesParser().parse(source)[0] as Properties.File

        assertThat(props.content.map { it as Properties.Entry }.map { it.key })
            .hasSize(2)
            .containsExactly("key", "key2")
        assertThat(props.content.map { it as Properties.Entry }.map { it.value.text })
            .hasSize(2)
            .containsExactly("value", "value2")

        val outputCapture = PrintOutputCapture(InMemoryExecutionContext())
        val printer = PropertiesPrinter<InMemoryExecutionContext>()
        printer.visitFile(props, outputCapture)
        assertThat(outputCapture.out.toString()).isEqualTo(source)
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2411")
    @Test
    fun commentsWithMultipleDelimiters() {
        val props = PropertiesParser().parse(
            """
            ########################
            #
            ########################
            
            key1=value1
            
            !!!!!!!!!!!!!!!!!!!!!!!
            !
            !!!!!!!!!!!!!!!!!!!!!!!
            
            key2=value2

        """.trimIndent()
        )[0]

        val comment1 = props.content[0] as Properties.Comment
        assertThat(comment1.message).isEqualTo("#######################")

        val comment2 = props.content[1] as Properties.Comment
        assertThat(comment2.message).isEqualTo("")

        val comment3 = props.content[2] as Properties.Comment
        assertThat(comment3.message).isEqualTo("#######################")

        val entry1 = props.content[3] as Properties.Entry
        assertThat(entry1.key).isEqualTo("key1")
        assertThat(entry1.value.text).isEqualTo("value1")

        val comment4 = props.content[4] as Properties.Comment
        assertThat(comment4.message).isEqualTo("!!!!!!!!!!!!!!!!!!!!!!")

        val comment5 = props.content[5] as Properties.Comment
        assertThat(comment5.message).isEqualTo("")

        val comment6 = props.content[6] as Properties.Comment
        assertThat(comment6.message).isEqualTo("!!!!!!!!!!!!!!!!!!!!!!")

        val entry2 = props.content[7] as Properties.Entry
        assertThat(entry2.key).isEqualTo("key2")
        assertThat(entry2.value.text).isEqualTo("value2")
    }

    @Suppress("WrongPropertyKeyValueDelimiter")
    @Issue("https://github.com/openrewrite/rewrite/issues/2501")
    @Test
    fun delimitedByWhitespace() {
        val props = PropertiesParser().parse(
            """
            key1         value1
            key2:value2
        """.trimIndent()
        )[0]

        val entries = props.content.map { it as Properties.Entry }
        assertThat(entries).hasSize(2)
        val entry = entries[0]
        assertThat(entry.key).isEqualTo("key1")
        assertThat(entry.value.text).isEqualTo("value1")
        val entry2 = entries[1]
        assertThat(entry2.key).isEqualTo("key2")
        assertThat(entry2.value.text).isEqualTo("value2")
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2501")
    @Test
    fun escapedEntryDelimiters() {
        val props = PropertiesParser().parse(
            """
            ke\=y=value
            key\:2=value2
            key3=val\=ue3
            key4=val\:ue4
        """.trimIndent()
        )[0]

        assertThat(props.content.map { it as Properties.Entry }.map { it.key })
            .hasSize(4).containsExactly("ke\\=y", "key\\:2", "key3", "key4")
        assertThat(props.content.map { it as Properties.Entry }.map { it.value.text })
            .hasSize(4).containsExactly("value", "value2", "val\\=ue3", "val\\:ue4")
    }

    @Suppress("WrongPropertyKeyValueDelimiter", "TrailingSpacesInProperty")
    @Issue("https://github.com/openrewrite/rewrite/issues/2473")
    @Test
    fun continuedKey() {
        //language=properties
        val source = """
                  ke\
                      y=va\
                  lue
        """.trimIndent()

        val props = PropertiesParser().parse(source)[0] as Properties.File

        assertThat(props.content.map { it as Properties.Entry }.map { it.key })
            .hasSize(1)
            .containsExactly("key")

        assertThat(props.content.map { it as Properties.Entry }.map { it.value.text })
            .hasSize(1)
            .containsExactly("value")

        val outputCapture = PrintOutputCapture(InMemoryExecutionContext())
        val printer = PropertiesPrinter<InMemoryExecutionContext>()

        val entry = props.content[0] as Properties.Entry
        val sameLength = entry.withKey("yes")
        printer.visitEntry(sameLength, outputCapture)

        //language=properties
        val result = """
                  yes=va\
                  lue
        """.trimIndent()

        assertThat(outputCapture.out.toString()).isEqualTo(result)

        outputCapture.out.setLength(0)
        val diffLength = entry.withKey("newKey")
        printer.visitEntry(diffLength, outputCapture)

        //language=properties
        val result2 = """
                  newKey=va\
                  lue
        """.trimIndent()
        assertThat(outputCapture.out.toString()).isEqualTo(result2)

        outputCapture.out.setLength(0)
        val containsContinue = entry.withKey("new\\\n    Key")
        printer.visitEntry(containsContinue, outputCapture)

        //language=properties
        val result3 = """
                  new\
                      Key=va\
                  lue
        """.trimIndent()
        assertThat(outputCapture.out.toString()).isEqualTo(result3)
        println()
    }

    @Suppress("WrongPropertyKeyValueDelimiter", "TrailingSpacesInProperty")
    @Issue("https://github.com/openrewrite/rewrite/issues/2473")
    @Test
    fun continuedBeforeEquals() {
        //language=properties
        val source = """
                  key  \
                      = va\
                  lue
        """.trimIndent()

        val props = PropertiesParser().parse(source)[0] as Properties.File

        assertThat(props.content.map { it as Properties.Entry }.map { it.key })
            .hasSize(1)
            .containsExactly("key")

        assertThat(props.content.map { it as Properties.Entry }.map { it.value.text })
            .hasSize(1)
            .containsExactly("value")

        val outputCapture = PrintOutputCapture(InMemoryExecutionContext())
        val printer = PropertiesPrinter<InMemoryExecutionContext>()

        val entry = props.content[0] as Properties.Entry
        val sameLength = entry.withBeforeEquals(" *")
        printer.visitEntry(sameLength, outputCapture)

        //language=properties
        val result = """
                  key *= va\
                  lue
        """.trimIndent()

        assertThat(outputCapture.out.toString()).isEqualTo(result)

        outputCapture.out.setLength(0)
        val diffLength = entry.withBeforeEquals("   ")
        printer.visitEntry(diffLength, outputCapture)

        //language=properties
        val result2 = """
                  key   = va\
                  lue
        """.trimIndent()
        assertThat(outputCapture.out.toString()).isEqualTo(result2)

        outputCapture.out.setLength(0)
        val containsContinue = entry.withBeforeEquals("  \\\n")
        printer.visitEntry(containsContinue, outputCapture)

        //language=properties
        val result3 = """
                  key  \
                  = va\
                  lue
        """.trimIndent()
        assertThat(outputCapture.out.toString()).isEqualTo(result3)
        println()
    }

    @Disabled
    @Suppress("WrongPropertyKeyValueDelimiter", "TrailingSpacesInProperty")
    @Issue("https://github.com/openrewrite/rewrite/issues/2473")
    @Test
    fun continuedValue() {
        //language=properties
        val source = """
                  key=va\
                  lue
        """.trimIndent()

        val props = PropertiesParser().parse(source)[0] as Properties.File

        assertThat(props.content.map { it as Properties.Entry }.map { it.key })
            .hasSize(1)
            .containsExactly("key")

        assertThat(props.content.map { it as Properties.Entry }.map { it.value.text })
            .hasSize(1)
            .containsExactly("value")

        val outputCapture = PrintOutputCapture(InMemoryExecutionContext())
        val printer = PropertiesPrinter<InMemoryExecutionContext>()

        val entry = props.content[0] as Properties.Entry
        val sameLength = entry.withValue(entry.value.withText("words"))
        printer.visitEntry(sameLength, outputCapture)

        //language=properties
        val result = "key=words"

        assertThat(outputCapture.out.toString()).isEqualTo(result)

        outputCapture.out.setLength(0)
        val diffLength = entry.withValue(entry.value.withText("more words"))
        printer.visitEntry(diffLength, outputCapture)

        //language=properties
        val result2 = "key=more words"
        assertThat(outputCapture.out.toString()).isEqualTo(result2)

        outputCapture.out.setLength(0)
        val containsContinue = entry.withValue(entry.value.withText("wor\\\n  ds"))
        printer.visitEntry(containsContinue, outputCapture)

        //language=properties
        val result3 = """
                  key=wor\
                    ds
        """.trimIndent()
        assertThat(outputCapture.out.toString()).isEqualTo(result3)
        println()
    }
}
