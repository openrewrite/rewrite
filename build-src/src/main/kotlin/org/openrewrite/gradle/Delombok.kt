package org.openrewrite.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectories
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.withGroovyBuilder
import javax.inject.Inject

/**
 * Delomboks the source sets so that parsers like Javadoc generation can occur.
 */
@CacheableTask
abstract class Delombok @Inject constructor(
    objectFactory: ObjectFactory
): DefaultTask() {
    init {
        description = "Delomboks the entire source code tree"
    }

    private var sourceFiles: ConfigurableFileCollection = objectFactory.fileCollection()

    @get:IgnoreEmptyDirectories
    @get:SkipWhenEmpty
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFiles
    val source: FileCollection =
        objectFactory.fileCollection().from({ sourceFiles.asFileTree })

    @get:Classpath
    abstract val compileClasspath: ConfigurableFileCollection

    @get:OutputDirectories
    abstract val outputDirectory: DirectoryProperty

    /**
     * Adds some source to this task.
     *
     * @param sources given source objects will be evaluated as per [org.gradle.api.Project.files].
     */
    fun source(vararg sources: Any): Delombok {
        sourceFiles.from(sources)
        return this
    }

    @TaskAction
    fun delombok() {
        ant.withGroovyBuilder {
            "taskdef"(
                "name" to "delombok",
                "classname" to "lombok.delombok.ant.Tasks${'$'}Delombok",
                "classpath" to compileClasspath.asPath
            )

            "delombok"(
                "to" to outputDirectory.asFile.get().absolutePath,
                "classpath" to compileClasspath.asPath
            ) {
                sourceFiles.addToAntBuilder(this, "fileset", FileCollection.AntType.FileSet)
            }
        }
    }
}
