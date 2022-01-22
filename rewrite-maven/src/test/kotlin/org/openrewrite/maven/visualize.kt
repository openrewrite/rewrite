package org.openrewrite.maven

import guru.nidi.graphviz.engine.Format
import org.openrewrite.ExecutionContext
import org.openrewrite.maven.tree.GraphvizResolutionEventListener
import org.openrewrite.maven.tree.Scope
import java.io.File

fun <T> visualize(ctx: ExecutionContext, outputFilePrefix: String = "maven-resolution", runnable: () -> T) {
    val viz = GraphvizResolutionEventListener(Scope.Compile)
    MavenExecutionContextView(ctx).setResoutionListener(viz)
    try {
        runnable()
    } finally {
        viz.graphviz().render(Format.DOT).toFile(File("${outputFilePrefix}.dot"));
        viz.graphviz().height(500).render(Format.PNG).toFile(File("${outputFilePrefix}.png"));
    }
}
