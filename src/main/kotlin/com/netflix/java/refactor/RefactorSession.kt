package com.netflix.java.refactor

import eu.infomas.annotation.AnnotationDetector
import org.gradle.internal.impldep.com.google.common.collect.HashMultimap
import org.slf4j.LoggerFactory
import java.io.File
import java.lang.reflect.Modifier
import java.net.URLClassLoader

class RefactorSession(sources: Iterable<File>, val classpath: Iterable<File>) {
    val logger = LoggerFactory.getLogger(RefactorSession::class.java)
    val parser = AstParser()
    val compilationUnits = parser.parseFiles(sources.toList(), classpath)
    
    fun refactor(): Map<Refactor, Iterable<File>> {
        val results = HashMultimap.create<Refactor, File>()
        val classLoader = URLClassLoader(classpath.map { it.toURI().toURL() }.toTypedArray(), javaClass.classLoader)

        val reporter = object : AnnotationDetector.MethodReporter {
            override fun annotations() = arrayOf(Refactor::class.java)

            override fun reportMethodAnnotation(annotation: Class<out Annotation>, className: String, methodName: String) {
                val method = Class.forName(className, false, classLoader).getMethod(methodName)
                val refactor = method.getAnnotation(Refactor::class.java)

                if(method.returnType != Refactorer::class.java) {
                    logger.warn("Rule ${refactor.value} must return RefactorRule, will not be applied")
                    return
                }

                if(!Modifier.isStatic(method.modifiers)) {
                    logger.warn("Rule ${refactor.value} must be static, will not be applied")
                    return
                }

                compilationUnits.forEach { cu ->
                    val refactorer = Refactorer(cu, parser.context)
                    method.invoke(null, refactorer)
                    if(refactorer.changedFile)
                        results.put(refactor, refactorer.source)
                }
            }

        }
        AnnotationDetector(reporter).detect(*classpath.toList().toTypedArray())
        return results.asMap()
    }
}