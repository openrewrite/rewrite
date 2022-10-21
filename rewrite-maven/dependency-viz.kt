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
package org.openrewrite.maven

import guru.nidi.graphviz.engine.Format
import org.openrewrite.ExecutionContext
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.maven.tree.GraphvizResolutionEventListener
import org.openrewrite.maven.tree.MavenResolutionResult
import org.openrewrite.maven.tree.Scope
import java.net.URL
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists

fun <T> visualize(
    ctx: ExecutionContext = InMemoryExecutionContext { t -> throw t },
    scope: Scope = Scope.Compile,
    outputFileLocation: Path = Paths.get("diagrams"),
    outputFilePrefix: String = "maven",
    showProperties: Boolean = false,
    showManagedDependencies: Boolean = false,
    runnable: () -> T
) {
    val viz = GraphvizResolutionEventListener(scope, showProperties, showManagedDependencies)
    MavenExecutionContextView(ctx).setResolutionListener(viz)
    try {
        runnable()
    } finally {
        viz.graphviz().render(Format.DOT).toFile(outputFileLocation.resolve("${outputFilePrefix}.dot").toFile())
        viz.graphviz()
            .postProcessor { result, _, _ ->
                result.mapString { svg ->
                    svg.replace("font-family=\"Times,serif\" ", "")
                        .replace("a xlink:href", "a target=\"_blank\" xlink:href")
                }
            }
            .render(Format.SVG)
            .toFile(outputFileLocation.resolve("${outputFilePrefix}.svg").toFile())

        val panZoom = outputFileLocation.resolve("svg-pan-zoom.min.js")
        if (!panZoom.exists()) {
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
                        <style>
                            body {
                                font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif, "Apple Color Emoji", "Segoe UI Emoji", "Segoe UI Symbol";
                            }
                        </style>
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
    scope: Scope = Scope.Compile,
    showProperties: Boolean = false,
    showManagedDependencies: Boolean = false
) {
    visualize(ctx, scope, Paths.get("diagrams"), gav.replace(':', '_'), showProperties, showManagedDependencies) {
        parse(gav, ctx)
    }
}

fun parse(gav: String, ctx: ExecutionContext = InMemoryExecutionContext { t -> throw t }): MavenResolutionResult {
    val (group, artifact, version) = gav.split(":")
    val maven = MavenParser.builder().build().parse(
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
    )[0]

    return maven.mavenResolutionResult()
}
