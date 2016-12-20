package com.netflix.rewrite.gradle

import com.netflix.rewrite.Rewrite
import com.netflix.rewrite.ast.Tr
import com.netflix.rewrite.auto.RewriteScanner
import com.netflix.rewrite.auto.Rule
import com.netflix.rewrite.parse.OracleJdkParser
import com.netflix.rewrite.refactor.rule.JUnitToAssertJ
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

class RewritePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.withType(JavaPlugin::class.java) {
            project.tasks.create("lintSource", RewriteTask::class.java)
            project.tasks.create("fixSourceLint", RewriteAndFixTask::class.java)
        }
    }
}

typealias RewriteStats = Map<Rewrite, Int>

abstract class AbstractRewriteTask : DefaultTask() {
    private val bundledRules = listOf(JUnitToAssertJ())
            .map { it::class.annotations.filterIsInstance<Rewrite>().first() to it }
            .toMap()

    fun run(op: (Rule, Tr.CompilationUnit) -> Int): RewriteStats {
        return project.convention.getPlugin(JavaPluginConvention::class.java).sourceSets.fold(mutableMapOf<Rewrite, Int>()) { stats, ss ->
            val classpath = ss.compileClasspath.map(File::toPath)
            val asts = OracleJdkParser(classpath).parse(ss.allJava.map(File::toPath))

            val rules = RewriteScanner(classpath).rewriteRulesOnClasspath() + bundledRules

            asts.forEach { cu ->
                rules.forEach { rewrite, rule ->
                    stats.merge(rewrite, op(rule, cu), Int::plus)
                }
            }

            stats
        }
    }
}

open class RewriteTask : AbstractRewriteTask() {

    @TaskAction
    fun refactorSourceStats() {
        val textOutput = StyledTextService(services)

        val stats = run { rule, cu ->
            val refactor = rule.refactor(cu)
            refactor.stats().values.sum()
        }

        if (stats.isNotEmpty()) {
            textOutput.withStyle(Styling.Red).println("\u2716 Your source code requires refactoring.")
            textOutput.text("Run").withStyle(Styling.Bold).text("./gradlew fixSourceLint")
            textOutput.println(" to automatically fix")

            stats.entries.forEachIndexed { i, (rewrite, count) ->
                textOutput.text("   ${i + 1}. ")
                textOutput.withStyle(Styling.Bold).text(rewrite.value)
                textOutput.println(" required $count changes to ${rewrite.description}")
            }

            throw GradleException("This project requires refactoring. Run ./gradlew fixSourceLint to automatically fix.")
        }
    }
}

open class RewriteAndFixTask : AbstractRewriteTask() {

    @TaskAction
    fun refactorSource() {
        val textOutput = StyledTextService(services)

        val stats = run { rule, cu ->
            val refactor = rule.refactor(cu)
            val changes = refactor.stats().values.sum()
            if (changes > 0) {
                Files.newBufferedWriter(Paths.get(cu.sourcePath)).use {
                    it.write(refactor.fix().print())
                }
            }
            changes
        }

        if (stats.isNotEmpty()) {
            textOutput.withStyle(Styling.Red).text("\u2716 Your source code requires refactoring. ")
            textOutput.text("Please run ")
            textOutput.withStyle(Styling.Bold).text("git diff")
            textOutput.println(", review, and commit changes.")
            stats.entries.forEachIndexed { i, (rewrite, count) ->
                textOutput.text("   ${i + 1}. ")
                textOutput.withStyle(Styling.Bold).text(rewrite.value)
                textOutput.println(" required $count changes to ${rewrite.description}")
            }

            throw GradleException("This project contains uncommitted refactoring changes.")
        }
    }
}