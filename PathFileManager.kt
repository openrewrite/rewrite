/*
 * Copyright (c) 2005, 2012, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.netflix.java.refactor

import java.net.MalformedURLException
import java.net.URI
import java.net.URISyntaxException
import java.net.URL
import java.nio.CharBuffer
import java.nio.charset.Charset
import java.util.ArrayList
import java.util.Arrays
import java.util.Collections
import java.util.Comparator
import java.util.EnumSet
import java.util.HashMap
import java.util.zip.ZipFile

import javax.lang.model.SourceVersion
import javax.tools.FileObject
import javax.tools.JavaFileManager
import javax.tools.JavaFileObject
import javax.tools.StandardJavaFileManager

import com.sun.tools.javac.file.RelativePath.RelativeFile
import com.sun.tools.javac.file.RelativePath.RelativeDirectory
import com.sun.tools.javac.util.BaseFileManager
import com.sun.tools.javac.util.Context
import com.sun.tools.javac.util.List
import com.sun.tools.javac.util.ListBuffer
import java.io.*
import java.nio.file.Path

import javax.tools.StandardLocation.*

class PathFileManager(context: Context) : 
        BaseFileManager(charset), StandardJavaFileManager {

    private var fsInfo: com.sun.tools.javac.file.FSInfo? = null
    private var zipFileIndexCache: com.sun.tools.javac.file.ZipFileIndexCache? = null
    private val sourceOrClass = EnumSet.of(JavaFileObject.Kind.SOURCE, JavaFileObject.Kind.CLASS)

    init {
        context.put(JavaFileManager::class.java, this)
        setContext(context)
    }

    /**
     * Set the context for JavacFileManager.
     */
    override fun setContext(context: Context) {
        super.setContext(context)

        fsInfo = com.sun.tools.javac.file.FSInfo.instance(context)

        contextUseOptimizedZip = options.getBoolean("useOptimizedZip", true)
        if (contextUseOptimizedZip)
            zipFileIndexCache = com.sun.tools.javac.file.ZipFileIndexCache.getSharedInstance()

        symbolFileEnabled = !options.isSet("ignore.symbol.file")
    }

    override fun isDefaultBootClassPath() = locations.isDefaultBootClassPath

    fun getFileForInput(path: String) = 
            PathJavaFileObject(path)

    /**
     * Insert all files in subdirectory subdirectory of directory directory
     * which match fileKinds into resultList
     */
    private fun listDirectory(directory: File,
                              subdirectory: RelativeDirectory,
                              fileKinds: Set<JavaFileObject.Kind>,
                              recurse: Boolean,
                              resultList: ListBuffer<JavaFileObject>) {
        val d = subdirectory.getFile(directory)
        if (!caseMapCheck(d, subdirectory))
            return

        val files = d.listFiles() ?: return

        for (f in files) {
            val fname = f.name
            if (f.isDirectory) {
                if (recurse && SourceVersion.isIdentifier(fname)) {
                    listDirectory(directory,
                            RelativeDirectory(subdirectory, fname),
                            fileKinds,
                            recurse,
                            resultList)
                }
            } else {
                if (isValidFile(fname, fileKinds)) {
                    val fe = com.sun.tools.javac.file.RegularFileObject(this, fname, File(d, fname))
                    resultList.append(fe)
                }
            }
        }
    }

    /**
     * Insert all files in subdirectory subdirectory of archive archive
     * which match fileKinds into resultList
     */
    private fun listArchive(archive: Archive,
                            subdirectory: RelativeDirectory,
                            fileKinds: Set<JavaFileObject.Kind>,
                            recurse: Boolean,
                            resultList: ListBuffer<JavaFileObject>) {
        // Get the files directly in the subdir
        var files: List<String>? = archive.getFiles(subdirectory)
        if (files != null) {
            while (!files!!.isEmpty()) {
                val file = files.head
                if (isValidFile(file, fileKinds)) {
                    resultList.append(archive.getFileObject(subdirectory, file))
                }
                files = files.tail
            }
        }
        if (recurse) {
            for (s in archive.subdirectories) {
                if (subdirectory.contains(s)) {
                    // Because the archive map is a flat list of directories,
                    // the enclosing loop will pick up all child subdirectories.
                    // Therefore, there is no need to recurse deeper.
                    listArchive(archive, s, fileKinds, false, resultList)
                }
            }
        }
    }

    /**
     * container is a directory, a zip file, or a non-existant path.
     * Insert all files in subdirectory subdirectory of container which
     * match fileKinds into resultList
     */
    private fun listContainer(container: File,
                              subdirectory: RelativeDirectory,
                              fileKinds: Set<JavaFileObject.Kind>,
                              recurse: Boolean,
                              resultList: ListBuffer<JavaFileObject>) {
        var archive: Archive? = archives[container]
        if (archive == null) {
            // archives are not created for directories.
            if (fsInfo!!.isDirectory(container)) {
                listDirectory(container,
                        subdirectory,
                        fileKinds,
                        recurse,
                        resultList)
                return
            }

            // Not a directory; either a file or non-existant, create the archive
            try {
                archive = openArchive(container)
            } catch (ex: IOException) {
                log.error("error.reading.file",
                        container, getMessage(ex))
                return
            }

        }
        listArchive(archive,
                subdirectory,
                fileKinds,
                recurse,
                resultList)
    }

    private fun isValidFile(s: String, fileKinds: Set<JavaFileObject.Kind>): Boolean {
        val kind = getKind(s)
        return fileKinds.contains(kind)
    }

    /** Hack to make Windows case sensitive. Test whether given path
     * ends in a string of characters with the same case as given name.
     * Ignore file separators in both path and name.
     */
    private fun caseMapCheck(f: File, name: com.sun.tools.javac.file.RelativePath): Boolean {
        if (fileSystemIsCaseSensitive) return true
        // Note that getCanonicalPath() returns the case-sensitive
        // spelled file name.
        val path: String
        try {
            path = f.canonicalPath
        } catch (ex: IOException) {
            return false
        }

        val pcs = path.toCharArray()
        val ncs = name.path.toCharArray()
        var i = pcs.size - 1
        var j = ncs.size - 1
        while (i >= 0 && j >= 0) {
            while (i >= 0 && pcs[i] == File.separatorChar) i--
            while (j >= 0 && ncs[j] == '/') j--
            if (i >= 0 && j >= 0) {
                if (pcs[i] != ncs[j]) return false
                i--
                j--
            }
        }
        return j < 0
    }

    /**
     * An archive provides a flat directory structure of a ZipFile by
     * mapping directory names to lists of files (basenames).
     */
    interface Archive {
        @Throws(IOException::class)
        fun close()

        operator fun contains(name: com.sun.tools.javac.file.RelativePath): Boolean

        fun getFileObject(subdirectory: RelativeDirectory, file: String): JavaFileObject

        fun getFiles(subdirectory: RelativeDirectory): List<String>

        val subdirectories: Set<RelativeDirectory>
    }

    inner class MissingArchive(internal val zipFileName: File) : Archive {
        fun contains(name: com.sun.tools.javac.file.RelativePath): Boolean {
            return false
        }

        fun close() {
        }

        fun getFileObject(subdirectory: RelativeDirectory, file: String): JavaFileObject? {
            return null
        }

        fun getFiles(subdirectory: RelativeDirectory): List<String> {
            return List.nil<String>()
        }

        val subdirectories: Set<RelativeDirectory>
            get() = emptySet()

        override fun toString(): String {
            return "MissingArchive[$zipFileName]"
        }
    }

    /** A directory of zip files already opened.
     */
    internal var archives: MutableMap<File, Archive> = HashMap()

    /*
     * This method looks for a ZipFormatException and takes appropriate
     * evasive action. If there is a failure in the fast mode then we
     * fail over to the platform zip, and allow it to deal with a potentially
     * non compliant zip file.
     */
    @Throws(IOException::class)
    protected fun openArchive(zipFilename: File): Archive {
        try {
            return openArchive(zipFilename, contextUseOptimizedZip)
        } catch (ioe: IOException) {
            if (ioe is com.sun.tools.javac.file.ZipFileIndex.ZipFormatException) {
                return openArchive(zipFilename, false)
            } else {
                throw ioe
            }
        }

    }

    /** Open a new zip file directory, and cache it.
     */
    @Throws(IOException::class)
    private fun openArchive(zipFileName: File, useOptimizedZip: Boolean): Archive {
        var zipFileName = zipFileName
        val origZipFileName = zipFileName
        if (symbolFileEnabled && locations.isDefaultBootClassPathRtJar(zipFileName)) {
            var file = zipFileName.parentFile.parentFile // ${java.home}
            if (File(file.name) == File("jre"))
                file = file.parentFile
            // file == ${jdk.home}
            for (name in symbolFileLocation)
                file = File(file, name)
            // file == ${jdk.home}/lib/ct.sym
            if (file.exists())
                zipFileName = file
        }

        val archive: Archive
        try {

            var zdir: ZipFile? = null

            var usePreindexedCache = false
            var preindexCacheLocation: String? = null

            if (!useOptimizedZip) {
                zdir = ZipFile(zipFileName)
            } else {
                usePreindexedCache = options.isSet("usezipindex")
                preindexCacheLocation = options.get("java.io.tmpdir")
                var optCacheLoc: String? = options.get("cachezipindexdir")

                if (optCacheLoc != null && optCacheLoc.length != 0) {
                    if (optCacheLoc.startsWith("\"")) {
                        if (optCacheLoc.endsWith("\"")) {
                            optCacheLoc = optCacheLoc.substring(1, optCacheLoc.length - 1)
                        } else {
                            optCacheLoc = optCacheLoc.substring(1)
                        }
                    }

                    val cacheDir = File(optCacheLoc)
                    if (cacheDir.exists() && cacheDir.canWrite()) {
                        preindexCacheLocation = optCacheLoc
                        if (!preindexCacheLocation.endsWith("/") && !preindexCacheLocation.endsWith(File.separator)) {
                            preindexCacheLocation += File.separator
                        }
                    }
                }
            }

            if (origZipFileName === zipFileName) {
                if (!useOptimizedZip) {
                    archive = com.sun.tools.javac.file.ZipArchive(this, zdir)
                } else {
                    archive = com.sun.tools.javac.file.ZipFileIndexArchive(this,
                            zipFileIndexCache!!.getZipFileIndex(zipFileName,
                                    null,
                                    usePreindexedCache,
                                    preindexCacheLocation,
                                    options.isSet("writezipindexfiles")))
                }
            } else {
                if (!useOptimizedZip) {
                    archive = com.sun.tools.javac.file.SymbolArchive(this, origZipFileName, zdir, symbolFilePrefix)
                } else {
                    archive = com.sun.tools.javac.file.ZipFileIndexArchive(this,
                            zipFileIndexCache!!.getZipFileIndex(zipFileName,
                                    symbolFilePrefix,
                                    usePreindexedCache,
                                    preindexCacheLocation,
                                    options.isSet("writezipindexfiles")))
                }
            }
        } catch (ex: FileNotFoundException) {
            archive = MissingArchive(zipFileName)
        } catch (zfe: com.sun.tools.javac.file.ZipFileIndex.ZipFormatException) {
            throw zfe
        } catch (ex: IOException) {
            if (zipFileName.exists())
                log.error("error.reading.file", zipFileName, getMessage(ex))
            archive = MissingArchive(zipFileName)
        }

        archives.put(origZipFileName, archive)
        return archive
    }

    /** Flush any output resources.
     */
    override fun flush() {
        contentCache.clear()
    }

    /**
     * Close the JavaFileManager, releasing resources.
     */
    override fun close() {
        val i = archives.values.iterator()
        while (i.hasNext()) {
            val a = i.next()
            i.remove()
            try {
                a.close()
            } catch (e: IOException) {
            }

        }
    }

    private var defaultEncodingName: String? = null
    private fun getDefaultEncodingName(): String {
        if (defaultEncodingName == null) {
            defaultEncodingName = OutputStreamWriter(ByteArrayOutputStream()).encoding
        }
        return defaultEncodingName
    }

    override fun getClassLoader(location: JavaFileManager.Location): ClassLoader? {
        nullCheck(location)
        val path = getLocation(location) ?: return null
        val lb = ListBuffer<URL>()
        for (f in path) {
            try {
                lb.append(f.toURI().toURL())
            } catch (e: MalformedURLException) {
                throw AssertionError(e)
            }

        }

        return getClassLoader(lb.toTypedArray())
    }

    @Throws(IOException::class)
    override fun list(location: JavaFileManager.Location,
                      packageName: String,
                      kinds: Set<JavaFileObject.Kind>,
                      recurse: Boolean): Iterable<JavaFileObject> {
        // validatePackageName(packageName);
        nullCheck(packageName)
        nullCheck(kinds)

        val path = getLocation(location) ?: return List.nil<JavaFileObject>()
        val subdirectory = RelativeDirectory.forPackage(packageName)
        val results = ListBuffer<JavaFileObject>()

        for (directory in path)
            listContainer(directory, subdirectory, kinds, recurse, results)
        return results.toList()
    }

    override fun inferBinaryName(location: JavaFileManager.Location, file: JavaFileObject): String? {
        file.javaClass // null check
        location.javaClass // null check
        // Need to match the path semantics of list(location, ...)
        val path = getLocation(location) ?: return null

        if (file is com.sun.tools.javac.file.BaseFileObject) {
            return file.inferBinaryName(path)
        } else
            throw IllegalArgumentException(file.javaClass.name)
    }

    override fun isSameFile(a: FileObject, b: FileObject): Boolean {
        nullCheck(a)
        nullCheck(b)
        if (a !is com.sun.tools.javac.file.BaseFileObject)
            throw IllegalArgumentException("Not supported: " + a)
        if (b !is com.sun.tools.javac.file.BaseFileObject)
            throw IllegalArgumentException("Not supported: " + b)
        return a == b
    }

    override fun hasLocation(location: JavaFileManager.Location): Boolean {
        return getLocation(location) != null
    }

    @Throws(IOException::class)
    override fun getJavaFileForInput(location: JavaFileManager.Location,
                                     className: String,
                                     kind: JavaFileObject.Kind): JavaFileObject {
        nullCheck(location)
        // validateClassName(className);
        nullCheck(className)
        nullCheck(kind)
        if (!sourceOrClass.contains(kind))
            throw IllegalArgumentException("Invalid kind: " + kind)
        return getFileForInput(location, RelativeFile.forClass(className, kind))
    }

    @Throws(IOException::class)
    override fun getFileForInput(location: JavaFileManager.Location,
                                 packageName: String,
                                 relativeName: String): FileObject {
        nullCheck(location)
        // validatePackageName(packageName);
        nullCheck(packageName)
        if (!isRelativeUri(relativeName))
            throw IllegalArgumentException("Invalid relative name: " + relativeName)
        val name = if (packageName.length == 0)
            RelativeFile(relativeName)
        else
            RelativeFile(RelativeDirectory.forPackage(packageName), relativeName)
        return getFileForInput(location, name)
    }

    @Throws(IOException::class)
    private fun getFileForInput(location: JavaFileManager.Location, name: RelativeFile): JavaFileObject? {
        val path = getLocation(location) ?: return null

        for (dir in path) {
            var a: Archive? = archives[dir]
            if (a == null) {
                if (fsInfo!!.isDirectory(dir)) {
                    val f = name.getFile(dir)
                    if (f.exists())
                        return com.sun.tools.javac.file.RegularFileObject(this, f)
                    continue
                }
                // Not a directory, create the archive
                a = openArchive(dir)
            }
            // Process the archive
            if (a.contains(name)) {
                return a.getFileObject(name.dirname(), name.basename())
            }
        }
        return null
    }
    
    override fun getJavaFileObjectsFromFiles(
            files: Iterable<File>): Iterable<JavaFileObject> {
        val result: ArrayList<com.sun.tools.javac.file.RegularFileObject>
        if (files is Collection<*>)
            result = ArrayList<com.sun.tools.javac.file.RegularFileObject>(files.size)
        else
            result = ArrayList<com.sun.tools.javac.file.RegularFileObject>()
        for (f in files)
            result.add(com.sun.tools.javac.file.RegularFileObject(this, nullCheck(f)))
        return result
    }

    override fun getJavaFileObjects(vararg files: File): Iterable<JavaFileObject> {
        return getJavaFileObjectsFromFiles(Arrays.asList<File>(*nullCheck<Array<File>>(files)))
    }

    @Throws(IOException::class)
    override fun setLocation(location: JavaFileManager.Location,
                             path: Iterable<File>) {
        nullCheck(location)
        locations.setLocation(location, path)
    }

    override fun getLocation(location: JavaFileManager.Location): Iterable<File>? {
        nullCheck(location)
        return locations.getLocation(location)
    }

    private val classOutDir: File?
        get() = locations.getOutputLocation(CLASS_OUTPUT)

    private val sourceOutDir: File?
        get() = locations.getOutputLocation(SOURCE_OUTPUT)

    companion object {

        fun toArray(buffer: CharBuffer): CharArray {
            if (buffer.hasArray())
                return (buffer.compact().flip() as CharBuffer).array()
            else
                return buffer.toString().toCharArray()
        }

        /**
         * Register a Context.Factory to create a JavacFileManager.
         */
        fun preRegister(context: Context) {
            context.put(JavaFileManager::class.java, Context.Factory<JavaFileManager> { c -> com.sun.tools.javac.file.JavacFileManager(c, true, null) })
        }

        private fun isValidName(name: String): Boolean {
            // Arguably, isValidName should reject keywords (such as in SourceVersion.isName() ),
            // but the set of keywords depends on the source level, and we don't want
            // impls of JavaFileManager to have to be dependent on the source level.
            // Therefore we simply check that the argument is a sequence of identifiers
            // separated by ".".
            for (s in name.split("\\.".toRegex()).toTypedArray()) {
                if (!SourceVersion.isIdentifier(s))
                    return false
            }
            return true
        }

        private fun validateClassName(className: String) {
            if (!isValidName(className))
                throw IllegalArgumentException("Invalid class name: " + className)
        }

        private fun validatePackageName(packageName: String) {
            if (packageName.length > 0 && !isValidName(packageName))
                throw IllegalArgumentException("Invalid packageName name: " + packageName)
        }

        fun testName(name: String,
                     isValidPackageName: Boolean,
                     isValidClassName: Boolean) {
            try {
                validatePackageName(name)
                if (!isValidPackageName)
                    throw AssertionError("Invalid package name accepted: " + name)
                printAscii("Valid package name: \"%s\"", name)
            } catch (e: IllegalArgumentException) {
                if (isValidPackageName)
                    throw AssertionError("Valid package name rejected: " + name)
                printAscii("Invalid package name: \"%s\"", name)
            }

            try {
                validateClassName(name)
                if (!isValidClassName)
                    throw AssertionError("Invalid class name accepted: " + name)
                printAscii("Valid class name: \"%s\"", name)
            } catch (e: IllegalArgumentException) {
                if (isValidClassName)
                    throw AssertionError("Valid class name rejected: " + name)
                printAscii("Invalid class name: \"%s\"", name)
            }

        }

        private fun printAscii(format: String, vararg args: Any) {
            val message: String
            try {
                val ascii = "US-ASCII"
                message = String(String.format(null, format, *args).toByteArray(charset(ascii)), ascii)
            } catch (ex: java.io.UnsupportedEncodingException) {
                throw AssertionError(ex)
            }

            println(message)
        }

        private val fileSystemIsCaseSensitive = File.separatorChar == '/'

        private val symbolFileLocation = arrayOf("lib", "ct.sym")
        private val symbolFilePrefix = RelativeDirectory("META-INF/sym/rt.jar/")

        /**
         * Enforces the specification of a "relative" name as used in
         * [ getFileForInput][.getFileForInput].  This method must follow the rules defined in
         * that method, do not make any changes without consulting the
         * specification.
         */
        protected fun isRelativeUri(uri: URI): Boolean {
            if (uri.isAbsolute)
                return false
            val path = uri.normalize().path
            if (path.length == 0 /* isEmpty() is mustang API */)
                return false
            if (path != uri.path)
            // implicitly checks for embedded . and ..
                return false
            if (path.startsWith("/") || path.startsWith("./") || path.startsWith("../"))
                return false
            return true
        }

        // Convenience method
        protected fun isRelativeUri(u: String): Boolean {
            try {
                return isRelativeUri(URI(u))
            } catch (e: URISyntaxException) {
                return false
            }

        }

        /**
         * Converts a relative file name to a relative URI.  This is
         * different from File.toURI as this method does not canonicalize
         * the file before creating the URI.  Furthermore, no schema is
         * used.
         * @param file a relative file name
         * *
         * @return a relative URI
         * *
         * @throws IllegalArgumentException if the file name is not
         * * relative according to the definition given in [ ][javax.tools.JavaFileManager.getFileForInput]
         */
        fun getRelativeName(file: File): String {
            if (!file.isAbsolute) {
                val result = file.path.replace(File.separatorChar, '/')
                if (isRelativeUri(result))
                    return result
            }
            throw IllegalArgumentException("Invalid relative path: " + file)
        }

        /**
         * Get a detail message from an IOException.
         * Most, but not all, instances of IOException provide a non-null result
         * for getLocalizedMessage().  But some instances return null: in these
         * cases, fallover to getMessage(), and if even that is null, return the
         * name of the exception itself.
         * @param e an IOException
         * *
         * @return a string to include in a compiler diagnostic
         */
        fun getMessage(e: IOException): String {
            var s: String? = e.localizedMessage
            if (s != null)
                return s
            s = e.message
            if (s != null)
                return s
            return e.toString()
        }
    }
}
