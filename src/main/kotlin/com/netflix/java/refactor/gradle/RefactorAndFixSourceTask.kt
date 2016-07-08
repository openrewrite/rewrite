package com.netflix.java.refactor.gradle

import com.netflix.java.refactor.Refactor
import com.netflix.java.refactor.RefactorFix
import com.netflix.java.refactor.RefactorRule
import eu.infomas.annotation.AnnotationDetector
import org.gradle.api.DefaultTask
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.logging.text.StyledTextOutput
import org.gradle.internal.logging.text.StyledTextOutputFactory
import java.util.*
import javax.inject.Inject

class RefactorAndFixSourceTask : DefaultTask() {
    // see http://gradle.1045684.n5.nabble.com/injecting-dependencies-into-task-instances-td5712637.html
    @Inject
    fun getTextOutputFactory(): StyledTextOutputFactory? = null
    
    @TaskAction
    fun refactorSource() {
        val fixesByRule = HashMap<Refactor, List<RefactorFix>>()
        
        val reporter = object : AnnotationDetector.MethodReporter {
            override fun annotations() = arrayOf(Refactor::class.java)

            override fun reportMethodAnnotation(annotation: Class<out Annotation>, className: String, methodName: String) {
                // TODO do some signature checking
                val method = Class.forName(className).getMethod(methodName)
                val rule = method.invoke(null) as RefactorRule

                val fixes = project.convention.getPlugin(JavaPluginConvention::class.java).sourceSets.flatMap {
                    rule.refactorAndFix(*it.allJava.toList().toTypedArray())
                }
             
                if(fixes.isNotEmpty()) {
                    fixesByRule.put(method.getAnnotation(Refactor::class.java), fixes)
                }
            }

        }
        val cf = AnnotationDetector(reporter)
        cf.detect()
    }
    
    fun printReport(fixesByRule: Map<Refactor, List<RefactorFix>>) {
        val textOutput = getTextOutputFactory()!!.create(RefactorAndFixSourceTask::class.java)
        
        if(fixesByRule.isEmpty()) {
            textOutput.style(StyledTextOutput.Style.Identifier).println("Passed refactoring check with no changes necessary")
        } else {
            textOutput.withStyle(StyledTextOutput.Style.UserInput).text("Refactoring operations were performed on this project")
            textOutput.println("A complete listing my fixes follows. Please review and commit the changes.\n")
        }

        fixesByRule.forEach {
            val (rule, ruleFixes) = it
            
            ruleFixes.groupBy { it.source }.forEach { 
                val (file, fileFixes) = it
                val relativePath = project.rootDir.toURI().relativize(file.toURI()).toString()
                
                fileFixes.forEach { fix ->
                    textOutput.withStyle(Styling.Green).text("fixed".padEnd(15))
                    textOutput.text(rule.id.padEnd(35))
                    textOutput.withStyle(Styling.Yellow).println(rule.description)

                    textOutput.withStyle(Styling.Bold).println("$relativePath: ${fix.lineNumber}")
                    textOutput.println() // extra space between violations
                }
            }
        }

        textOutput.style(Styling.Green).println("Made ${fixesByRule.values.sumBy { it.size }} changes\n")
    }
}