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
import java.lang.reflect.Modifier
import java.net.URLClassLoader
import java.util.*
import javax.inject.Inject

open class RefactorAndFixSourceTask : DefaultTask() {
    // see http://gradle.1045684.n5.nabble.com/injecting-dependencies-into-task-instances-td5712637.html
    @Inject
    open fun getTextOutputFactory(): StyledTextOutputFactory? = null
    
    @TaskAction
    fun refactorSource() {
        val fixesByRule = HashMap<Refactor, List<RefactorFix>>()

        val classpath = project.convention.getPlugin(JavaPluginConvention::class.java).sourceSets
                .flatMap { it.compileClasspath }.distinct()

        val classLoader = URLClassLoader(classpath.map { it.toURI().toURL() }.toTypedArray(), javaClass.classLoader)
        
        val reporter = object : AnnotationDetector.MethodReporter {
            override fun annotations() = arrayOf(Refactor::class.java)

            override fun reportMethodAnnotation(annotation: Class<out Annotation>, className: String, methodName: String) {
                val method = Class.forName(className, false, classLoader).getMethod(methodName)
                val refactor = method.getAnnotation(Refactor::class.java)
                
                if(method.returnType != RefactorRule::class.java) {
                    project.logger.warn("Rule ${refactor.value} must return RefactorRule, will not be applied")
                    return
                }
                
                if(!Modifier.isStatic(method.modifiers)) {
                    project.logger.warn("Rule ${refactor.value} must be static, will not be applied")
                    return
                }
                
                val rule = method.invoke(null) as RefactorRule

                val fixes = project.convention.getPlugin(JavaPluginConvention::class.java).sourceSets.flatMap {
                    rule.refactorAndFix(*it.allJava.toList().toTypedArray())
                }

                if (fixes.isNotEmpty()) {
                    fixesByRule.put(refactor, fixes)
                }
            }

        }
        AnnotationDetector(reporter).detect(*classpath.toTypedArray())
        
        printReport(fixesByRule)
    }
    
    fun printReport(fixesByRule: Map<Refactor, List<RefactorFix>>) {
        val textOutput = getTextOutputFactory()!!.create(RefactorAndFixSourceTask::class.java)
        
        if(fixesByRule.isEmpty()) {
            textOutput.style(StyledTextOutput.Style.Identifier).println("Passed refactoring check with no changes necessary")
        } else {
            textOutput.withStyle(StyledTextOutput.Style.UserInput).text("Refactoring operations were performed on this project. ")
            textOutput.println("A complete listing of fixes follows. Please review and commit the changes.\n")
        }

        fixesByRule.forEach {
            val (rule, ruleFixes) = it
            
            ruleFixes.groupBy { it.source }.forEach { 
                val (file, fileFixes) = it
                val relativePath = project.rootDir.toURI().relativize(file.toURI()).toString()
                
                fileFixes.forEach { fix ->
                    textOutput.withStyle(Styling.Green).text("fixed".padEnd(15))
                    textOutput.text(rule.value.padEnd(35))
                    textOutput.withStyle(Styling.Yellow).println(rule.description)

                    textOutput.withStyle(Styling.Bold).println("$relativePath: ${fix.lineNumber}")
                    textOutput.println() // extra space between violations
                }
            }
        }

        textOutput.style(Styling.Green).println("Made ${fixesByRule.values.sumBy { it.size }} changes\n")
    }
}