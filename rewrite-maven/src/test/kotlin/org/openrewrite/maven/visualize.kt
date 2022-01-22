package org.openrewrite.maven

import guru.nidi.graphviz.engine.Format
import guru.nidi.graphviz.engine.Rasterizer
import org.openrewrite.ExecutionContext
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.maven.tree.GraphvizResolutionEventListener
import org.openrewrite.maven.tree.Scope
import java.awt.Toolkit
import java.nio.file.Path
import java.nio.file.Paths

fun <T> visualize(
    ctx: ExecutionContext = InMemoryExecutionContext { t -> throw t },
    scope: Scope = Scope.Compile,
    outputFileLocation: Path = Paths.get("diagrams"),
    outputFilePrefix: String = "maven",
    runnable: () -> T
) {
    val viz = GraphvizResolutionEventListener(scope)
    MavenExecutionContextView(ctx).setResoutionListener(viz)
    try {
        runnable()
    } finally {
        viz.graphviz().render(Format.DOT).toFile(outputFileLocation.resolve("${outputFilePrefix}.dot").toFile())
        viz.graphviz().height(500).render(Format.PNG).toFile(outputFileLocation.resolve("${outputFilePrefix}.png").toFile())
        outputFileLocation.resolve("${outputFilePrefix}.html").toFile().writeText(
            """
                <img src="${outputFilePrefix}.png" usemap="#${GraphvizResolutionEventListener.GRAPH_NAME}"/>
                ${viz.graphviz().height(500).render(Format.CMAPX)}
            """.trimIndent()
        )
    }
}

fun visualize(
    gav: String,
    ctx: ExecutionContext = InMemoryExecutionContext { t -> throw t },
    scope: Scope = Scope.Compile
) {
    visualize(ctx, scope, Paths.get("diagrams"), gav.replace(':', '_')) {
        val (group, artifact, version) = gav.split(":")
        MavenParser.builder().build().parse(
            ctx, """
                    <project>
                        <groupId>org.openrewrite</groupId>
                        <artifactId>dependency-viz</artifactId>
                        <version>0.0.1</version>
                        <dependencies>
                            <dependency>
                                <groupId>${group}</groupId>
                                <artifactId>${artifact}</artifactId>
                                <version>${version}</version>
                            </dependency>
                        </dependencies>
                    </project>
                """
        )
    }
}
