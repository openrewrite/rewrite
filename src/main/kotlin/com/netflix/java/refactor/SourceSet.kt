package com.netflix.java.refactor

import com.netflix.java.refactor.ast.AstParser
import eu.infomas.annotation.AnnotationDetector
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URLClassLoader
import java.nio.file.Path
import java.util.*

data class SourceSet<P>(val allSourceInputs: Iterable<SourceInput<P>>, val classpath: Iterable<Path>) {
    
    val logger: Logger = LoggerFactory.getLogger(SourceSet::class.java)
    val filteredClasspath = classpath.filter {
        val fn = it.fileName.toString()
        fn.endsWith(".jar") && !fn.endsWith("-javadoc.jar") && !fn.endsWith("-sources.jar")
    }
    
    private val parser = AstParser(filteredClasspath)
    
    private val compilationUnits: List<JavaSource<P>> by lazy {
        val javaFiles = allSourceInputs.filter { it.path.fileName.toString().endsWith(".java") }
        val parsedFiles = parser.parseFiles(javaFiles.map { it.path })
                
        parsedFiles.zip(javaFiles.map { it.datum }).map { 
            val (parsed, datum) = it
            JavaSource(CompilationUnit(parsed, parser), datum) 
        }
    }
    
    fun allJava(): List<JavaSource<P>> = compilationUnits

    /**
     * Find all classes annotated with @AutoRefactor on the classpath that implement JavaSourceScanner.
     * Does not work on virtual file systems at this time.
     */
    fun allAutoRefactorsOnClasspath(): Map<AutoRefactor, JavaSourceScanner<Any, *>> {
        val scanners = HashMap<AutoRefactor, JavaSourceScanner<Any, *>>()
        val classLoader = URLClassLoader(filteredClasspath.map { it.toFile().toURI().toURL() }.toTypedArray(), javaClass.classLoader)

        val reporter = object: AnnotationDetector.TypeReporter {
            override fun annotations() = arrayOf(AutoRefactor::class.java)

            override fun reportTypeAnnotation(annotation: Class<out Annotation>, className: String) {
                val clazz = Class.forName(className, false, classLoader)
                val refactor = clazz.getAnnotation(AutoRefactor::class.java)

                try {
                    val scanner = clazz.newInstance()
                    if(scanner is JavaSourceScanner<*, *>) {
                        scanners.put(refactor, scanner as JavaSourceScanner<Any, *>)
                    }
                    else {
                        logger.warn("To be useable, an @AutoRefactor must implement JavaSourceScanner")
                    }
                } catch(ignored: ReflectiveOperationException) {
                    logger.warn("Unable to construct @AutoRefactor with id '${refactor.value}'. It must extend implement JavaSourceScanner or extend JavaSourceVisitor and have a zero argument public constructor.")
                }
            }
        }
        
        AnnotationDetector(reporter).detect(*filteredClasspath.map { it.toFile() }.toTypedArray())
        return scanners
    }

    fun <R> scan(scanner: JavaSourceScanner<P, R>): List<R> = allJava().map { scanner.scan(it) }.filter { it != null }

    fun scanForClasses(predicate: (JavaSource<P>) -> Boolean): List<String> {
        return scan(object : JavaSourceScanner<P, List<String>> {
            override fun scan(source: JavaSource<P>): List<String> {
                return if (predicate.invoke(source))
                    source.classes()
                else emptyList<String>()
            }
        }).flatten()
    }
}
