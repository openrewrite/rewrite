/**
 * Copyright 2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.rewrite.auto

import com.netflix.rewrite.Rewrite
import com.netflix.rewrite.refactor.Refactor
import eu.infomas.annotation.AnnotationDetector
import org.slf4j.LoggerFactory
import java.net.URLClassLoader
import java.nio.file.Path

class RewriteScanner(classpath: Iterable<Path>) {
    val logger = LoggerFactory.getLogger(RewriteScanner::class.java)

    val filteredClasspath = classpath.filter {
        val fn = it.fileName.toString()
        fn.endsWith(".jar") && !fn.endsWith("-javadoc.jar") && !fn.endsWith("-sources.jar")
    }

    fun rewriteRulesOnClasspath(): Map<Rewrite, Rule> {
        val scanners = HashMap<Rewrite, Rule>()
        val classLoader = URLClassLoader(filteredClasspath.map { it.toFile().toURI().toURL() }.toTypedArray(), this::class.java.classLoader)

        val reporter = object: AnnotationDetector.TypeReporter {
            override fun annotations() = arrayOf(Rewrite::class.java)

            override fun reportTypeAnnotation(annotation: Class<out Annotation>, className: String) {
                val clazz = Class.forName(className, false, classLoader)
                val refactor = clazz.getAnnotation(Rewrite::class.java)

                try {
                    val scanner = clazz.newInstance()
                    if(scanner is Rule) {
                        scanners.put(refactor, scanner)
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
}