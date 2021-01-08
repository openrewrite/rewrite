package org.openrewrite.xml

import org.assertj.core.api.Assertions
import org.openrewrite.ExecutionContext
import org.openrewrite.TreePrinter
import org.openrewrite.xml.tree.Xml

open class XmlProcessorTest {

    private val parser = XmlParser()

    fun assertChanged(
            processorMapped: (Xml.Document) -> XmlProcessor<ExecutionContext>,
            before: String,
            after: String
    ) {
        val source = parser.parse(*(arrayOf(before.trimIndent()))).first()
        val result = processorMapped(source).visit(source,
                ExecutionContext.builder()
                        .maxCycles(2)
                        .doOnError { t: Throwable? -> Assertions.fail<Any>("Visitor threw an exception", t) }
                        .build())
        Assertions.assertThat(result).isNotNull
        Assertions.assertThat(result!!.printTrimmed(TreePrinter.identity<Any>())).isEqualTo(after.trimIndent())
    }
}