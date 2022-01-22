package org.openrewrite.maven

import guru.nidi.graphviz.engine.Format
import org.openrewrite.ExecutionContext
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.maven.tree.GraphvizResolutionEventListener
import org.openrewrite.maven.tree.Scope
import java.io.File

fun <T> visualize(
    ctx: ExecutionContext = InMemoryExecutionContext { t -> throw t },
    scope: Scope = Scope.Compile,
    outputFilePrefix: String = "diagrams/maven",
    runnable: () -> T
) {
    val viz = GraphvizResolutionEventListener(scope)
    MavenExecutionContextView(ctx).setResoutionListener(viz)
    try {
        runnable()
    } finally {
        viz.graphviz().render(Format.DOT).toFile(File("${outputFilePrefix}.dot"));
        viz.graphviz().height(500).render(Format.PNG).toFile(File("${outputFilePrefix}.png"));
    }
}

fun visualize(
    gav: String,
    ctx: ExecutionContext = InMemoryExecutionContext { t -> throw t },
    scope: Scope = Scope.Compile
) {
    visualize(ctx, scope, "diagrams/${gav}") {
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
