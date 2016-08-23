package com.netflix.java.refactor

import com.netflix.java.refactor.ast.AstParser
import eu.infomas.annotation.AnnotationDetector
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URLClassLoader
import java.nio.file.Path
import java.util.*

data class SourceSet(val allSourceFiles: Iterable<Path>, val classpath: Iterable<Path>) {
    val logger: Logger = LoggerFactory.getLogger(SourceSet::class.java)
    val filteredClasspath = classpath.filter {
        val fn = it.fileName.toString()
        fn.endsWith(".jar") && !fn.endsWith("-javadoc.jar") && !fn.endsWith("-sources.jar")
    }
    
    private val parser = AstParser(filteredClasspath)
    private val compilationUnits by lazy { 
        parser.parseFiles(allSourceFiles.filter { it.fileName.toString().endsWith(".java") }.toList()).map { CompilationUnit(it, parser) }
    }
    
    fun allJava() = compilationUnits.map { cu -> JavaSource(cu) }

    /**
     * Find all classes annotated with @AutoRefactor on the classpath that implement JavaSourceScanner.
     * Does not work on virtual file systems at this time.
     */
    fun allAutoRefactorsOnClasspath(): Map<AutoRefactor, JavaSourceScanner<*>> {
        val scanners = HashMap<AutoRefactor, JavaSourceScanner<*>>()
        val classLoader = URLClassLoader(filteredClasspath.map { it.toFile().toURI().toURL() }.toTypedArray(), javaClass.classLoader)

        val reporter = object: AnnotationDetector.TypeReporter {
            override fun annotations() = arrayOf(AutoRefactor::class.java)

            override fun reportTypeAnnotation(annotation: Class<out Annotation>, className: String) {
                val clazz = Class.forName(className, false, classLoader)
                val refactor = clazz.getAnnotation(AutoRefactor::class.java)

                try {
                    val scanner = clazz.newInstance()
                    if(scanner is JavaSourceScanner<*>) {
                        scanners.put(refactor, scanner)
                    }
                    else {
                        logger.warn("To be useable, an @AutoRefactor must implement JavaSourceScanner or extend JavaSourceVisitor")
                    }
                } catch(ignored: ReflectiveOperationException) {
                    logger.warn("Unable to construct @AutoRefactor with id '${refactor.value}'. It must extend implement JavaSourceScanner or extend JavaSourceVisitor and have a zero argument public constructor.")
                }
            }
        }
        
        AnnotationDetector(reporter).detect(*filteredClasspath.map { it.toFile() }.toTypedArray())
        return scanners
    }

    fun <T> scan(scanner: JavaSourceScanner<T>): List<T> = allJava().map { scanner.scan(it) }.filter { it != null }

    fun scanForClasses(predicate: (JavaSource) -> Boolean): List<String> {
        return scan(object : JavaSourceScanner<List<String>> {
            override fun scan(source: JavaSource): List<String> {
                return if (predicate.invoke(source))
                    source.classes()
                else emptyList<String>()
            }
        }).flatten()
    }
}
