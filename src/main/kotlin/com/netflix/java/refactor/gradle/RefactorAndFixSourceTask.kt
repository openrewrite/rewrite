package com.netflix.java.refactor.gradle

import com.netflix.java.refactor.Refactor
import com.netflix.java.refactor.RefactorFix
import com.netflix.java.refactor.Refactorer
import eu.infomas.annotation.AnnotationDetector
import org.gradle.api.DefaultTask
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.TaskAction
import org.gradle.logging.StyledTextOutputFactory
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
                
                if(method.returnType != Refactorer::class.java) {
                    project.logger.warn("Rule ${refactor.value} must return RefactorRule, will not be applied")
                    return
                }
                
                if(!Modifier.isStatic(method.modifiers)) {
                    project.logger.warn("Rule ${refactor.value} must be static, will not be applied")
                    return
                }
                
                val rule = method.invoke(null) as Refactorer

                val fixes = project.convention.getPlugin(JavaPluginConvention::class.java).sourceSets.flatMap {
                    rule.refactorAndFix(it.allJava, it.compileClasspath)
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
            textOutput.style(Styling.Green).println("Passed refactoring check with no changes necessary")
        } else {
            textOutput.text("Refactoring operations were performed on this project. ")
                    .withStyle(Styling.Bold).println("Please review the changes and commit.\n")
            
            fixesByRule.entries.forEachIndexed { i, entry ->
                val (rule, ruleFixes) = entry
                textOutput.withStyle(Styling.Bold).text("${"${i+1}.".padEnd(2)} ${rule.value}")
                textOutput.text(" (${ruleFixes.size} fixes) - ")
                textOutput.withStyle(Styling.Yellow).println(rule.description)
            }
        }
    }
}