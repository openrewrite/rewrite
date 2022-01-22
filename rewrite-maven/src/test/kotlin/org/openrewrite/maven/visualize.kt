package org.openrewrite.maven

import guru.nidi.graphviz.engine.Format
import guru.nidi.graphviz.engine.Rasterizer
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.openrewrite.ExecutionContext
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.maven.tree.GraphvizResolutionEventListener
import org.openrewrite.maven.tree.Scope
import java.awt.Toolkit
import java.net.URL
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists

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
        viz.graphviz().render(Format.SVG).toFile(outputFileLocation.resolve("${outputFilePrefix}.svg").toFile())

        val panZoom = outputFileLocation.resolve("svg-pan-zoom.min.js")
        if(!panZoom.exists()) {
            panZoom.toFile().writeText(
                URL("https://raw.githubusercontent.com/bumbu/svg-pan-zoom/master/dist/svg-pan-zoom.min.js")
                    .openStream().bufferedReader().readText()
            )
        }

        outputFileLocation.resolve("${outputFilePrefix}.html").toFile().writeText(
            //language=html
            """
                <html>
                    <head>
                        <script src="svg-pan-zoom.min.js"></script>
                    </head>
                    <body>
                        <embed id="resolution" type="image/svg+xml" style="width: 100%; height: 100%; border:1px solid black; " src="${outputFilePrefix}.svg">
                        <script>
                          window.onload = function() {
                            svgPanZoom('#resolution', {
                              zoomEnabled: true,
                              controlIconsEnabled: true,
                              mouseWheelZoomEnabled: true,
                              fit: false
                            });
                          };
                        </script>
                    </body>
                </html>
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
