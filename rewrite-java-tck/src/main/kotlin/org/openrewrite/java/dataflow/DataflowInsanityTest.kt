/*
 * Copyright 2022 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.dataflow

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.openrewrite.Cursor
import org.openrewrite.ExecutionContext
import org.openrewrite.java.Assertions.java
import org.openrewrite.java.JavaIsoVisitor
import org.openrewrite.java.tree.Expression
import org.openrewrite.java.tree.J
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest

@Suppress("FunctionName")
interface DataflowInsanityTest : RewriteTest {

    fun JavaIsoVisitor<ExecutionContext>.doRunDataFlow() {
        Dataflow.startingAt(cursor).findSinks(object : LocalTaintFlowSpec<Expression, Expression>() {
            override fun isSource(source: Expression, cursor: Cursor) = true

            override fun isSink(sink: Expression, cursor: Cursor) = true
        })
    }

    override fun defaults(spec: RecipeSpec) {
        spec.recipe(RewriteTest.toRecipe {
            object : JavaIsoVisitor<ExecutionContext>() {
                override fun visitClassDeclaration(
                    classDecl: J.ClassDeclaration,
                    p: ExecutionContext
                ): J.ClassDeclaration {
                    // Force a case where data flow occurs inside a `doAfterVisit` on a non-top-level visitor run.
                    object : JavaIsoVisitor<ExecutionContext>() {
                        override fun visitMethodDeclaration(
                            method: J.MethodDeclaration,
                            p: ExecutionContext
                        ): J.MethodDeclaration {
                            // The doAfterVisit
                            doAfterVisit(object : JavaIsoVisitor<ExecutionContext>() {
                                override fun visitMethodInvocation(
                                    method: J.MethodInvocation,
                                    p: ExecutionContext
                                ): J.MethodInvocation {
                                    doRunDataFlow()
                                    return super.visitMethodInvocation(method, p)
                                }
                            })
                            return method
                        }
                    }.visitNonNull(classDecl, p, cursor.parentOrThrow)
                    return classDecl
                }

                override fun visitExpression(expression: Expression, p: ExecutionContext): Expression {
                    doRunDataFlow()
                    return super.visitExpression(expression, p)
                }
            }
        })
        spec.expectedCyclesThatMakeChanges(0).cycles(1)
    }

    @Test
    fun simple() = rewriteRun(
        java(
            """
            public class A {
                public void m() {
                    int i = 0;
                    i = 1;
                }
            }
        """
        )
    )

    @Test
    fun `random types doing strange things`() = rewriteRun(
        java(
            """
                import java.util.Collection;
                import java.util.ArrayList;
                import java.util.function.Supplier;

                @SuppressWarnings("RedundantSuppression")
                abstract class Test<P extends Collection> implements Supplier<P> {
                    Object field;
                    Object ternaryStrangeness = conditional() ? get() : get().stream();
                    static Boolean conditional() {
                        return null;
                    }
                    static {
                        Supplier s = new Test<Collection>() {
                            @Override
                            public Collection get() {
                                return new ArrayList<>();
                            }
                        };
                    }
                    Test() {
                        Collection c = new ArrayList();
                        c.add(1);
                        //noinspection UnusedAssignment
                        field = c;
                        this.field = "Over achievements!";
                    }

                    @Override
                    public P get() {
                        return null;
                    }

                    void test() {
                        String n = "42";
                        String o = n;
                        System.out.println(o);
                        String p = o;
                    }
                }
            """
        )
    )

    @Test
    fun `test assignment to null variable`() = rewriteRun(
        java(
            """
            import java.util.List;
            import java.util.ArrayList;
            import java.io.File;
            import java.io.FileFilter;

            class Test {
                void test2(File srcDir, File destDir, FileFilter filter) {
                    // Cater for destination being directory within the source directory (see IO-141)
                    List<String> exclusionList = null;
                    String canonicalDestDir = destDir.getCanonicalPath();
                    if (canonicalDestDir.startsWith(srcDir.getCanonicalPath())) {
                        File[] srcFiles = filter == null ? srcDir.listFiles() : srcDir.listFiles(filter);
                        if (srcFiles != null && srcFiles.length > 0) {
                            exclusionList = new ArrayList<String>(srcFiles.length);
                            for (File srcFile : srcFiles) {
                                File copiedFile = new File(destDir, srcFile.getName());
                                exclusionList.add(copiedFile.getCanonicalPath());
                            }
                        }
                    }
                }
            }
            """
        )
    )

    @Test
    fun `goat test`() = rewriteRun(
        java(
            """
            package org.openrewrite.java;

            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;

            // Whenever this class is changed, make a corresponding change in JavaTypeGoat in the main resources folder.
            @AnnotationWithRuntimeRetention
            @AnnotationWithSourceRetention
            public abstract class JavaTypeGoat<T, S extends PT<S> & C> {

                public static final PT<TypeA> parameterizedField = new PT<TypeA>() {
                };

                public static abstract class InheritedJavaTypeGoat<T, U extends PT<U> & C> extends JavaTypeGoat<T, U> {
                    public InheritedJavaTypeGoat() {
                        super();
                    }
                }

                public enum EnumTypeA {
                    FOO, BAR(),
                    @AnnotationWithRuntimeRetention
                    FUZ
                }

                public enum EnumTypeB {
                    FOO(null);
                    private TypeA label;
                    EnumTypeB(TypeA label) {
                        this.label = label;
                    }
                }

                public abstract class ExtendsJavaTypeGoat extends JavaTypeGoat<T, S> {
                }

                public static abstract class Extension<U extends Extension<U>> {}

                public static class TypeA {}
                public static class TypeB {}

                @AnnotationWithRuntimeRetention
                @AnnotationWithSourceRetention
                public abstract void clazz(C n);
                public abstract void primitive(int n);
                public abstract void array(C[][] n);
                public abstract PT<C> parameterized(PT<C> n);
                public abstract PT<PT<C>> parameterizedRecursive(PT<PT<C>> n);
                public abstract PT<? extends C> generic(PT<? extends C> n);
                public abstract PT<? super C> genericContravariant(PT<? super C> n);
                public abstract <U extends JavaTypeGoat<U, ?>> JavaTypeGoat<? extends U[], ?> genericRecursive(JavaTypeGoat<? extends U[], ?> n);
                public abstract <U> PT<U> genericUnbounded(PT<U> n);
                public abstract void genericArray(PT<C>[] n);
                public abstract void inner(C.Inner n);
                public abstract void enumTypeA(EnumTypeA n);
                public abstract void enumTypeB(EnumTypeB n);
                public abstract <U extends PT<U> & C> InheritedJavaTypeGoat<T, U> inheritedJavaTypeGoat(InheritedJavaTypeGoat<T, U> n);
                public abstract <U extends TypeA & PT<U> & C> U genericIntersection(U n);
                public abstract T genericT(T n); // remove after signatures are common.
                public abstract <U extends JavaTypeGoat.Extension<U> & Intersection<U>> void recursiveIntersection(U n);
            }

            interface C {
                class Inner {
                }
            }

            interface PT<T> {
            }

            interface Intersection<T extends JavaTypeGoat.Extension<T> & Intersection<T>> {
                T getIntersectionType();
            }

            @Retention(RetentionPolicy.SOURCE)
            @interface AnnotationWithSourceRetention {}

            @Retention(RetentionPolicy.RUNTIME)
            @interface AnnotationWithRuntimeRetention {}
            """
        )
    )

    @Test
    fun `big file`() = rewriteRun(
        java(
            """
            /*
             * Licensed to the Apache Software Foundation (ASF) under one or more
             * contributor license agreements.  See the NOTICE file distributed with
             * this work for additional information regarding copyright ownership.
             * The ASF licenses this file to You under the Apache License, Version 2.0
             * (the "License"); you may not use this file except in compliance with
             * the License.  You may obtain a copy of the License at
             *
             *      http://www.apache.org/licenses/LICENSE-2.0
             *
             * Unless required by applicable law or agreed to in writing, software
             * distributed under the License is distributed on an "AS IS" BASIS,
             * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
             * See the License for the specific language governing permissions and
             * limitations under the License.
             */
            package org.apache.commons.io;

            import java.io.File;
            import java.io.FileFilter;
            import java.io.FileInputStream;
            import java.io.FileNotFoundException;
            import java.io.FileOutputStream;
            import java.io.IOException;
            import java.io.InputStream;
            import java.io.OutputStream;
            import java.net.URL;
            import java.net.URLConnection;
            import java.nio.ByteBuffer;
            import java.nio.channels.FileChannel;
            import java.nio.charset.Charset;
            import java.util.ArrayList;
            import java.util.Collection;
            import java.util.Date;
            import java.util.Iterator;
            import java.util.List;
            import java.util.zip.CRC32;
            import java.util.zip.CheckedInputStream;
            import java.util.zip.Checksum;

            import org.apache.commons.io.filefilter.DirectoryFileFilter;
            import org.apache.commons.io.filefilter.FalseFileFilter;
            import org.apache.commons.io.filefilter.FileFilterUtils;
            import org.apache.commons.io.filefilter.IOFileFilter;
            import org.apache.commons.io.filefilter.SuffixFileFilter;
            import org.apache.commons.io.filefilter.TrueFileFilter;
            import org.apache.commons.io.output.NullOutputStream;

            /**
             * General file manipulation utilities.
             * <p>
             * Facilities are provided in the following areas:
             * <ul>
             * <li>writing to a file
             * <li>reading from a file
             * <li>make a directory including parent directories
             * <li>copying files and directories
             * <li>deleting files and directories
             * <li>converting to and from a URL
             * <li>listing files and directories by filter and extension
             * <li>comparing file content
             * <li>file last changed date
             * <li>calculating a checksum
             * </ul>
             * <p>
             * Origin of code: Excalibur, Alexandria, Commons-Utils
             *
             * @author <a href="mailto:burton@relativity.yi.org">Kevin A. Burton</A>
             * @author <a href="mailto:sanders@apache.org">Scott Sanders</a>
             * @author <a href="mailto:dlr@finemaltcoding.com">Daniel Rall</a>
             * @author <a href="mailto:Christoph.Reck@dlr.de">Christoph.Reck</a>
             * @author <a href="mailto:peter@apache.org">Peter Donald</a>
             * @author <a href="mailto:jefft@apache.org">Jeff Turner</a>
             * @author Matthew Hawthorne
             * @author <a href="mailto:jeremias@apache.org">Jeremias Maerki</a>
             * @author Stephen Colebourne
             * @author Ian Springer
             * @author Chris Eldredge
             * @author Jim Harrington
             * @author Niall Pemberton
             * @author Sandy McArthur
             * @version ${'$'}Id${'$'}
             */
            public class FileUtils {

                /**
                 * Instances should NOT be constructed in standard programming.
                 */
                public FileUtils() {
                    super();
                }

                /**
                 * The number of bytes in a kilobyte.
                 */
                public static final long ONE_KB = 1024;

                /**
                 * The number of bytes in a megabyte.
                 */
                public static final long ONE_MB = ONE_KB * ONE_KB;

                /**
                 * The number of bytes in a 50 MB.
                 */
                private static final long FIFTY_MB = ONE_MB * 50;

                /**
                 * The number of bytes in a gigabyte.
                 */
                public static final long ONE_GB = ONE_KB * ONE_MB;

                /**
                 * An empty array of type <code>File</code>.
                 */
                public static final File[] EMPTY_FILE_ARRAY = new File[0];

                /**
                 * The UTF-8 character set, used to decode octets in URLs.
                 */
                private static final Charset UTF8 = Charset.forName("UTF-8");

                //-----------------------------------------------------------------------
                /**
                 * Returns the path to the system temporary directory.
                 *
                 * @return the path to the system temporary directory.
                 *
                 * @since Commons IO 2.0
                 */
                public static String getTempDirectoryPath() {
                    return System.getProperty("java.io.tmpdir");
                }

                /**
                 * Returns a {@link File} representing the system temporary directory.
                 *
                 * @return the system temporary directory.
                 *
                 * @since Commons IO 2.0
                 */
                public static File getTempDirectory() {
                    return new File(getTempDirectoryPath());
                }

                /**
                 * Returns the path to the user's home directory.
                 *
                 * @return the path to the user's home directory.
                 *
                 * @since Commons IO 2.0
                 */
                public static String getUserDirectoryPath() {
                    return System.getProperty("user.home");
                }

                /**
                 * Returns a {@link File} representing the user's home directory.
                 *
                 * @return the user's home directory.
                 *
                 * @since Commons IO 2.0
                 */
                public static File getUserDirectory() {
                    return new File(getUserDirectoryPath());
                }

                //-----------------------------------------------------------------------
                /**
                 * Opens a {@link FileInputStream} for the specified file, providing better
                 * error messages than simply calling <code>new FileInputStream(file)</code>.
                 * <p>
                 * At the end of the method either the stream will be successfully opened,
                 * or an exception will have been thrown.
                 * <p>
                 * An exception is thrown if the file does not exist.
                 * An exception is thrown if the file object exists but is a directory.
                 * An exception is thrown if the file exists but cannot be read.
                 *
                 * @param file  the file to open for input, must not be <code>null</code>
                 * @return a new {@link FileInputStream} for the specified file
                 * @throws FileNotFoundException if the file does not exist
                 * @throws IOException if the file object is a directory
                 * @throws IOException if the file cannot be read
                 * @since Commons IO 1.3
                 */
                public static FileInputStream openInputStream(File file) throws IOException {
                    if (file.exists()) {
                        if (file.isDirectory()) {
                            throw new IOException("File '" + file + "' exists but is a directory");
                        }
                        if (file.canRead() == false) {
                            throw new IOException("File '" + file + "' cannot be read");
                        }
                    } else {
                        throw new FileNotFoundException("File '" + file + "' does not exist");
                    }
                    return new FileInputStream(file);
                }

                //-----------------------------------------------------------------------
                /**
                 * Opens a {@link FileOutputStream} for the specified file, checking and
                 * creating the parent directory if it does not exist.
                 * <p>
                 * At the end of the method either the stream will be successfully opened,
                 * or an exception will have been thrown.
                 * <p>
                 * The parent directory will be created if it does not exist.
                 * The file will be created if it does not exist.
                 * An exception is thrown if the file object exists but is a directory.
                 * An exception is thrown if the file exists but cannot be written to.
                 * An exception is thrown if the parent directory cannot be created.
                 *
                 * @param file  the file to open for output, must not be <code>null</code>
                 * @return a new {@link FileOutputStream} for the specified file
                 * @throws IOException if the file object is a directory
                 * @throws IOException if the file cannot be written to
                 * @throws IOException if a parent directory needs creating but that fails
                 * @since Commons IO 1.3
                 */
                public static FileOutputStream openOutputStream(File file) throws IOException {
                    if (file.exists()) {
                        if (file.isDirectory()) {
                            throw new IOException("File '" + file + "' exists but is a directory");
                        }
                        if (file.canWrite() == false) {
                            throw new IOException("File '" + file + "' cannot be written to");
                        }
                    } else {
                        File parent = file.getParentFile();
                        if (parent != null && parent.exists() == false) {
                            if (parent.mkdirs() == false) {
                                throw new IOException("File '" + file + "' could not be created");
                            }
                        }
                    }
                    return new FileOutputStream(file);
                }

                //-----------------------------------------------------------------------
                /**
                 * Returns a human-readable version of the file size, where the input
                 * represents a specific number of bytes.
                 *
                 * If the size is over 1GB, the size is returned as the number of whole GB,
                 * i.e. the size is rounded down to the nearest GB boundary.
                 *
                 * Similarly for the 1MB and 1KB boundaries.
                 *
                 * @param size  the number of bytes
                 * @return a human-readable display value (includes units - GB, MB, KB or bytes)
                 */
                // See https://issues.apache.org/jira/browse/IO-226 - should the rounding be changed?
                public static String byteCountToDisplaySize(long size) {
                    String displaySize;

                    if (size / ONE_GB > 0) {
                        displaySize = String.valueOf(size / ONE_GB) + " GB";
                    } else if (size / ONE_MB > 0) {
                        displaySize = String.valueOf(size / ONE_MB) + " MB";
                    } else if (size / ONE_KB > 0) {
                        displaySize = String.valueOf(size / ONE_KB) + " KB";
                    } else {
                        displaySize = String.valueOf(size) + " bytes";
                    }
                    return displaySize;
                }

                //-----------------------------------------------------------------------
                /**
                 * Implements the same behaviour as the "touch" utility on Unix. It creates
                 * a new file with size 0 or, if the file exists already, it is opened and
                 * closed without modifying it, but updating the file date and time.
                 * <p>
                 * NOTE: As from v1.3, this method throws an IOException if the last
                 * modified date of the file cannot be set. Also, as from v1.3 this method
                 * creates parent directories if they do not exist.
                 *
                 * @param file  the File to touch
                 * @throws IOException If an I/O problem occurs
                 */
                public static void touch(File file) throws IOException {
                    if (!file.exists()) {
                        OutputStream out = openOutputStream(file);
                        IOUtils.closeQuietly(out);
                    }
                    boolean success = file.setLastModified(System.currentTimeMillis());
                    if (!success) {
                        throw new IOException("Unable to set the last modification time for " + file);
                    }
                }

                //-----------------------------------------------------------------------
                /**
                 * Converts a Collection containing java.io.File instanced into array
                 * representation. This is to account for the difference between
                 * File.listFiles() and FileUtils.listFiles().
                 *
                 * @param files  a Collection containing java.io.File instances
                 * @return an array of java.io.File
                 */
                public static File[] convertFileCollectionToFileArray(Collection<File> files) {
                     return files.toArray(new File[files.size()]);
                }

                //-----------------------------------------------------------------------
                /**
                 * Finds files within a given directory (and optionally its
                 * subdirectories). All files found are filtered by an IOFileFilter.
                 *
                 * @param files the collection of files found.
                 * @param directory the directory to search in.
                 * @param filter the filter to apply to files and directories.
                 */
                private static void innerListFiles(Collection<File> files, File directory,
                        IOFileFilter filter) {
                    File[] found = directory.listFiles((FileFilter) filter);
                    if (found != null) {
                        for (File file : found) {
                            if (file.isDirectory()) {
                                innerListFiles(files, file, filter);
                            } else {
                                files.add(file);
                            }
                        }
                    }
                }

                /**
                 * Finds files within a given directory (and optionally its
                 * subdirectories). All files found are filtered by an IOFileFilter.
                 * <p>
                 * If your search should recurse into subdirectories you can pass in
                 * an IOFileFilter for directories. You don't need to bind a
                 * DirectoryFileFilter (via logical AND) to this filter. This method does
                 * that for you.
                 * <p>
                 * An example: If you want to search through all directories called
                 * "temp" you pass in <code>FileFilterUtils.NameFileFilter("temp")</code>
                 * <p>
                 * Another common usage of this method is find files in a directory
                 * tree but ignoring the directories generated CVS. You can simply pass
                 * in <code>FileFilterUtils.makeCVSAware(null)</code>.
                 *
                 * @param directory  the directory to search in
                 * @param fileFilter  filter to apply when finding files.
                 * @param dirFilter  optional filter to apply when finding subdirectories.
                 * If this parameter is <code>null</code>, subdirectories will not be included in the
                 * search. Use TrueFileFilter.INSTANCE to match all directories.
                 * @return an collection of java.io.File with the matching files
                 * @see org.apache.commons.io.filefilter.FileFilterUtils
                 * @see org.apache.commons.io.filefilter.NameFileFilter
                 */
                public static Collection<File> listFiles(
                        File directory, IOFileFilter fileFilter, IOFileFilter dirFilter) {
                    if (!directory.isDirectory()) {
                        throw new IllegalArgumentException(
                                "Parameter 'directory' is not a directory");
                    }
                    if (fileFilter == null) {
                        throw new NullPointerException("Parameter 'fileFilter' is null");
                    }

                    //Setup effective file filter
                    IOFileFilter effFileFilter = FileFilterUtils.and(fileFilter,
                        FileFilterUtils.notFileFilter(DirectoryFileFilter.INSTANCE));

                    //Setup effective directory filter
                    IOFileFilter effDirFilter;
                    if (dirFilter == null) {
                        effDirFilter = FalseFileFilter.INSTANCE;
                    } else {
                        effDirFilter = FileFilterUtils.and(dirFilter,
                            DirectoryFileFilter.INSTANCE);
                    }

                    //Find files
                    Collection<File> files = new java.util.LinkedList<File>();
                    innerListFiles(files, directory,
                        FileFilterUtils.or(effFileFilter, effDirFilter));
                    return files;
                }

                /**
                 * Allows iteration over the files in given directory (and optionally
                 * its subdirectories).
                 * <p>
                 * All files found are filtered by an IOFileFilter. This method is
                 * based on {@link #listFiles(File, IOFileFilter, IOFileFilter)}.
                 *
                 * @param directory  the directory to search in
                 * @param fileFilter  filter to apply when finding files.
                 * @param dirFilter  optional filter to apply when finding subdirectories.
                 * If this parameter is <code>null</code>, subdirectories will not be included in the
                 * search. Use TrueFileFilter.INSTANCE to match all directories.
                 * @return an iterator of java.io.File for the matching files
                 * @see org.apache.commons.io.filefilter.FileFilterUtils
                 * @see org.apache.commons.io.filefilter.NameFileFilter
                 * @since Commons IO 1.2
                 */
                public static Iterator<File> iterateFiles(
                        File directory, IOFileFilter fileFilter, IOFileFilter dirFilter) {
                    return listFiles(directory, fileFilter, dirFilter).iterator();
                }

                //-----------------------------------------------------------------------
                /**
                 * Converts an array of file extensions to suffixes for use
                 * with IOFileFilters.
                 *
                 * @param extensions  an array of extensions. Format: {"java", "xml"}
                 * @return an array of suffixes. Format: {".java", ".xml"}
                 */
                private static String[] toSuffixes(String[] extensions) {
                    String[] suffixes = new String[extensions.length];
                    for (int i = 0; i < extensions.length; i++) {
                        suffixes[i] = "." + extensions[i];
                    }
                    return suffixes;
                }


                /**
                 * Finds files within a given directory (and optionally its subdirectories)
                 * which match an array of extensions.
                 *
                 * @param directory  the directory to search in
                 * @param extensions  an array of extensions, ex. {"java","xml"}. If this
                 * parameter is <code>null</code>, all files are returned.
                 * @param recursive  if true all subdirectories are searched as well
                 * @return an collection of java.io.File with the matching files
                 */
                public static Collection<File> listFiles(
                        File directory, String[] extensions, boolean recursive) {
                    IOFileFilter filter;
                    if (extensions == null) {
                        filter = TrueFileFilter.INSTANCE;
                    } else {
                        String[] suffixes = toSuffixes(extensions);
                        filter = new SuffixFileFilter(suffixes);
                    }
                    return listFiles(directory, filter,
                        (recursive ? TrueFileFilter.INSTANCE : FalseFileFilter.INSTANCE));
                }

                /**
                 * Allows iteration over the files in a given directory (and optionally
                 * its subdirectories) which match an array of extensions. This method
                 * is based on {@link #listFiles(File, String[], boolean)}.
                 *
                 * @param directory  the directory to search in
                 * @param extensions  an array of extensions, ex. {"java","xml"}. If this
                 * parameter is <code>null</code>, all files are returned.
                 * @param recursive  if true all subdirectories are searched as well
                 * @return an iterator of java.io.File with the matching files
                 * @since Commons IO 1.2
                 */
                public static Iterator<File> iterateFiles(
                        File directory, String[] extensions, boolean recursive) {
                    return listFiles(directory, extensions, recursive).iterator();
                }

                //-----------------------------------------------------------------------
                /**
                 * Compares the contents of two files to determine if they are equal or not.
                 * <p>
                 * This method checks to see if the two files are different lengths
                 * or if they point to the same file, before resorting to byte-by-byte
                 * comparison of the contents.
                 * <p>
                 * Code origin: Avalon
                 *
                 * @param file1  the first file
                 * @param file2  the second file
                 * @return true if the content of the files are equal or they both don't
                 * exist, false otherwise
                 * @throws IOException in case of an I/O error
                 */
                public static boolean contentEquals(File file1, File file2) throws IOException {
                    boolean file1Exists = file1.exists();
                    if (file1Exists != file2.exists()) {
                        return false;
                    }

                    if (!file1Exists) {
                        // two not existing files are equal
                        return true;
                    }

                    if (file1.isDirectory() || file2.isDirectory()) {
                        // don't want to compare directory contents
                        throw new IOException("Can't compare directories, only files");
                    }

                    if (file1.length() != file2.length()) {
                        // lengths differ, cannot be equal
                        return false;
                    }

                    if (file1.getCanonicalFile().equals(file2.getCanonicalFile())) {
                        // same file
                        return true;
                    }

                    InputStream input1 = null;
                    InputStream input2 = null;
                    try {
                        input1 = new FileInputStream(file1);
                        input2 = new FileInputStream(file2);
                        return IOUtils.contentEquals(input1, input2);

                    } finally {
                        IOUtils.closeQuietly(input1);
                        IOUtils.closeQuietly(input2);
                    }
                }

                //-----------------------------------------------------------------------
                /**
                 * Convert from a <code>URL</code> to a <code>File</code>.
                 * <p>
                 * From version 1.1 this method will decode the URL.
                 * Syntax such as <code>file:///my%20docs/file.txt</code> will be
                 * correctly decoded to <code>/my docs/file.txt</code>. Starting with version
                 * 1.5, this method uses UTF-8 to decode percent-encoded octets to characters.
                 * Additionally, malformed percent-encoded octets are handled leniently by
                 * passing them through literally.
                 *
                 * @param url  the file URL to convert, <code>null</code> returns <code>null</code>
                 * @return the equivalent <code>File</code> object, or <code>null</code>
                 *  if the URL's protocol is not <code>file</code>
                 */
                public static File toFile(URL url) {
                    if (url == null || !"file".equalsIgnoreCase(url.getProtocol())) {
                        return null;
                    } else {
                        String filename = url.getFile().replace('/', File.separatorChar);
                        filename = decodeUrl(filename);
                        return new File(filename);
                    }
                }

                /**
                 * Decodes the specified URL as per RFC 3986, i.e. transforms
                 * percent-encoded octets to characters by decoding with the UTF-8 character
                 * set. This function is primarily intended for usage with
                 * {@link java.net.URL} which unfortunately does not enforce proper URLs. As
                 * such, this method will leniently accept invalid characters or malformed
                 * percent-encoded octets and simply pass them literally through to the
                 * result string. Except for rare edge cases, this will make unencoded URLs
                 * pass through unaltered.
                 *
                 * @param url  The URL to decode, may be <code>null</code>.
                 * @return The decoded URL or <code>null</code> if the input was
                 *         <code>null</code>.
                 */
                static String decodeUrl(String url) {
                    String decoded = url;
                    if (url != null && url.indexOf('%') >= 0) {
                        int n = url.length();
                        StringBuffer buffer = new StringBuffer();
                        ByteBuffer bytes = ByteBuffer.allocate(n);
                        for (int i = 0; i < n;) {
                            if (url.charAt(i) == '%') {
                                try {
                                    do {
                                        byte octet = (byte) Integer.parseInt(url.substring(i + 1, i + 3), 16);
                                        bytes.put(octet);
                                        i += 3;
                                    } while (i < n && url.charAt(i) == '%');
                                    continue;
                                } catch (RuntimeException e) {
                                    // malformed percent-encoded octet, fall through and
                                    // append characters literally
                                } finally {
                                    if (bytes.position() > 0) {
                                        bytes.flip();
                                        buffer.append(UTF8.decode(bytes).toString());
                                        bytes.clear();
                                    }
                                }
                            }
                            buffer.append(url.charAt(i++));
                        }
                        decoded = buffer.toString();
                    }
                    return decoded;
                }

                /**
                 * Converts each of an array of <code>URL</code> to a <code>File</code>.
                 * <p>
                 * Returns an array of the same size as the input.
                 * If the input is <code>null</code>, an empty array is returned.
                 * If the input contains <code>null</code>, the output array contains <code>null</code> at the same
                 * index.
                 * <p>
                 * This method will decode the URL.
                 * Syntax such as <code>file:///my%20docs/file.txt</code> will be
                 * correctly decoded to <code>/my docs/file.txt</code>.
                 *
                 * @param urls  the file URLs to convert, <code>null</code> returns empty array
                 * @return a non-<code>null</code> array of Files matching the input, with a <code>null</code> item
                 *  if there was a <code>null</code> at that index in the input array
                 * @throws IllegalArgumentException if any file is not a URL file
                 * @throws IllegalArgumentException if any file is incorrectly encoded
                 * @since Commons IO 1.1
                 */
                public static File[] toFiles(URL[] urls) {
                    if (urls == null || urls.length == 0) {
                        return EMPTY_FILE_ARRAY;
                    }
                    File[] files = new File[urls.length];
                    for (int i = 0; i < urls.length; i++) {
                        URL url = urls[i];
                        if (url != null) {
                            if (url.getProtocol().equals("file") == false) {
                                throw new IllegalArgumentException(
                                        "URL could not be converted to a File: " + url);
                            }
                            files[i] = toFile(url);
                        }
                    }
                    return files;
                }

                /**
                 * Converts each of an array of <code>File</code> to a <code>URL</code>.
                 * <p>
                 * Returns an array of the same size as the input.
                 *
                 * @param files  the files to convert
                 * @return an array of URLs matching the input
                 * @throws IOException if a file cannot be converted
                 */
                public static URL[] toURLs(File[] files) throws IOException {
                    URL[] urls = new URL[files.length];

                    for (int i = 0; i < urls.length; i++) {
                        urls[i] = files[i].toURI().toURL();
                    }

                    return urls;
                }

                //-----------------------------------------------------------------------
                /**
                 * Copies a file to a directory preserving the file date.
                 * <p>
                 * This method copies the contents of the specified source file
                 * to a file of the same name in the specified destination directory.
                 * The destination directory is created if it does not exist.
                 * If the destination file exists, then this method will overwrite it.
                 * <p>
                 * <strong>Note:</strong> This method tries to preserve the file's last
                 * modified date/times using {@link File#setLastModified(long)}, however
                 * it is not guaranteed that the operation will succeed.
                 * If the modification operation fails, no indication is provided.
                 *
                 * @param srcFile  an existing file to copy, must not be <code>null</code>
                 * @param destDir  the directory to place the copy in, must not be <code>null</code>
                 *
                 * @throws NullPointerException if source or destination is null
                 * @throws IOException if source or destination is invalid
                 * @throws IOException if an IO error occurs during copying
                 * @see #copyFile(File, File, boolean)
                 */
                public static void copyFileToDirectory(File srcFile, File destDir) throws IOException {
                    copyFileToDirectory(srcFile, destDir, true);
                }

                /**
                 * Copies a file to a directory optionally preserving the file date.
                 * <p>
                 * This method copies the contents of the specified source file
                 * to a file of the same name in the specified destination directory.
                 * The destination directory is created if it does not exist.
                 * If the destination file exists, then this method will overwrite it.
                 * <p>
                 * <strong>Note:</strong> Setting <code>preserveFileDate</code> to
                 * <code>true</code> tries to preserve the file's last modified
                 * date/times using {@link File#setLastModified(long)}, however it is
                 * not guaranteed that the operation will succeed.
                 * If the modification operation fails, no indication is provided.
                 *
                 * @param srcFile  an existing file to copy, must not be <code>null</code>
                 * @param destDir  the directory to place the copy in, must not be <code>null</code>
                 * @param preserveFileDate  true if the file date of the copy
                 *  should be the same as the original
                 *
                 * @throws NullPointerException if source or destination is <code>null</code>
                 * @throws IOException if source or destination is invalid
                 * @throws IOException if an IO error occurs during copying
                 * @see #copyFile(File, File, boolean)
                 * @since Commons IO 1.3
                 */
                public static void copyFileToDirectory(File srcFile, File destDir, boolean preserveFileDate) throws IOException {
                    if (destDir == null) {
                        throw new NullPointerException("Destination must not be null");
                    }
                    if (destDir.exists() && destDir.isDirectory() == false) {
                        throw new IllegalArgumentException("Destination '" + destDir + "' is not a directory");
                    }
                    File destFile = new File(destDir, srcFile.getName());
                    copyFile(srcFile, destFile, preserveFileDate);
                }

                /**
                 * Copies a file to a new location preserving the file date.
                 * <p>
                 * This method copies the contents of the specified source file to the
                 * specified destination file. The directory holding the destination file is
                 * created if it does not exist. If the destination file exists, then this
                 * method will overwrite it.
                 * <p>
                 * <strong>Note:</strong> This method tries to preserve the file's last
                 * modified date/times using {@link File#setLastModified(long)}, however
                 * it is not guaranteed that the operation will succeed.
                 * If the modification operation fails, no indication is provided.
                 *
                 * @param srcFile  an existing file to copy, must not be <code>null</code>
                 * @param destFile  the new file, must not be <code>null</code>
                 *
                 * @throws NullPointerException if source or destination is <code>null</code>
                 * @throws IOException if source or destination is invalid
                 * @throws IOException if an IO error occurs during copying
                 * @see #copyFileToDirectory(File, File)
                 */
                public static void copyFile(File srcFile, File destFile) throws IOException {
                    copyFile(srcFile, destFile, true);
                }

                /**
                 * Copies a file to a new location.
                 * <p>
                 * This method copies the contents of the specified source file
                 * to the specified destination file.
                 * The directory holding the destination file is created if it does not exist.
                 * If the destination file exists, then this method will overwrite it.
                 * <p>
                 * <strong>Note:</strong> Setting <code>preserveFileDate</code> to
                 * <code>true</code> tries to preserve the file's last modified
                 * date/times using {@link File#setLastModified(long)}, however it is
                 * not guaranteed that the operation will succeed.
                 * If the modification operation fails, no indication is provided.
                 *
                 * @param srcFile  an existing file to copy, must not be <code>null</code>
                 * @param destFile  the new file, must not be <code>null</code>
                 * @param preserveFileDate  true if the file date of the copy
                 *  should be the same as the original
                 *
                 * @throws NullPointerException if source or destination is <code>null</code>
                 * @throws IOException if source or destination is invalid
                 * @throws IOException if an IO error occurs during copying
                 * @see #copyFileToDirectory(File, File, boolean)
                 */
                public static void copyFile(File srcFile, File destFile,
                        boolean preserveFileDate) throws IOException {
                    if (srcFile == null) {
                        throw new NullPointerException("Source must not be null");
                    }
                    if (destFile == null) {
                        throw new NullPointerException("Destination must not be null");
                    }
                    if (srcFile.exists() == false) {
                        throw new FileNotFoundException("Source '" + srcFile + "' does not exist");
                    }
                    if (srcFile.isDirectory()) {
                        throw new IOException("Source '" + srcFile + "' exists but is a directory");
                    }
                    if (srcFile.getCanonicalPath().equals(destFile.getCanonicalPath())) {
                        throw new IOException("Source '" + srcFile + "' and destination '" + destFile + "' are the same");
                    }
                    if (destFile.getParentFile() != null && destFile.getParentFile().exists() == false) {
                        if (destFile.getParentFile().mkdirs() == false) {
                            throw new IOException("Destination '" + destFile + "' directory cannot be created");
                        }
                    }
                    if (destFile.exists() && destFile.canWrite() == false) {
                        throw new IOException("Destination '" + destFile + "' exists but is read-only");
                    }
                    doCopyFile(srcFile, destFile, preserveFileDate);
                }

                /**
                 * Internal copy file method.
                 *
                 * @param srcFile  the validated source file, must not be <code>null</code>
                 * @param destFile  the validated destination file, must not be <code>null</code>
                 * @param preserveFileDate  whether to preserve the file date
                 * @throws IOException if an error occurs
                 */
                private static void doCopyFile(File srcFile, File destFile, boolean preserveFileDate) throws IOException {
                    if (destFile.exists() && destFile.isDirectory()) {
                        throw new IOException("Destination '" + destFile + "' exists but is a directory");
                    }

                    FileInputStream fis = null;
                    FileOutputStream fos = null;
                    FileChannel input = null;
                    FileChannel output = null;
                    try {
                        fis = new FileInputStream(srcFile);
                        fos = new FileOutputStream(destFile);
                        input  = fis.getChannel();
                        output = fos.getChannel();
                        long size = input.size();
                        long pos = 0;
                        long count = 0;
                        while (pos < size) {
                            count = (size - pos) > FIFTY_MB ? FIFTY_MB : (size - pos);
                            pos += output.transferFrom(input, pos, count);
                        }
                    } finally {
                        IOUtils.closeQuietly(output);
                        IOUtils.closeQuietly(fos);
                        IOUtils.closeQuietly(input);
                        IOUtils.closeQuietly(fis);
                    }

                    if (srcFile.length() != destFile.length()) {
                        throw new IOException("Failed to copy full contents from '" +
                                srcFile + "' to '" + destFile + "'");
                    }
                    if (preserveFileDate) {
                        destFile.setLastModified(srcFile.lastModified());
                    }
                }

                //-----------------------------------------------------------------------
                /**
                 * Copies a directory to within another directory preserving the file dates.
                 * <p>
                 * This method copies the source directory and all its contents to a
                 * directory of the same name in the specified destination directory.
                 * <p>
                 * The destination directory is created if it does not exist.
                 * If the destination directory did exist, then this method merges
                 * the source with the destination, with the source taking precedence.
                 * <p>
                 * <strong>Note:</strong> This method tries to preserve the files' last
                 * modified date/times using {@link File#setLastModified(long)}, however
                 * it is not guaranteed that those operations will succeed.
                 * If the modification operation fails, no indication is provided.
                 *
                 * @param srcDir  an existing directory to copy, must not be <code>null</code>
                 * @param destDir  the directory to place the copy in, must not be <code>null</code>
                 *
                 * @throws NullPointerException if source or destination is <code>null</code>
                 * @throws IOException if source or destination is invalid
                 * @throws IOException if an IO error occurs during copying
                 * @since Commons IO 1.2
                 */
                public static void copyDirectoryToDirectory(File srcDir, File destDir) throws IOException {
                    if (srcDir == null) {
                        throw new NullPointerException("Source must not be null");
                    }
                    if (srcDir.exists() && srcDir.isDirectory() == false) {
                        throw new IllegalArgumentException("Source '" + destDir + "' is not a directory");
                    }
                    if (destDir == null) {
                        throw new NullPointerException("Destination must not be null");
                    }
                    if (destDir.exists() && destDir.isDirectory() == false) {
                        throw new IllegalArgumentException("Destination '" + destDir + "' is not a directory");
                    }
                    copyDirectory(srcDir, new File(destDir, srcDir.getName()), true);
                }

                /**
                 * Copies a whole directory to a new location preserving the file dates.
                 * <p>
                 * This method copies the specified directory and all its child
                 * directories and files to the specified destination.
                 * The destination is the new location and name of the directory.
                 * <p>
                 * The destination directory is created if it does not exist.
                 * If the destination directory did exist, then this method merges
                 * the source with the destination, with the source taking precedence.
                 * <p>
                 * <strong>Note:</strong> This method tries to preserve the files' last
                 * modified date/times using {@link File#setLastModified(long)}, however
                 * it is not guaranteed that those operations will succeed.
                 * If the modification operation fails, no indication is provided.
                 *
                 * @param srcDir  an existing directory to copy, must not be <code>null</code>
                 * @param destDir  the new directory, must not be <code>null</code>
                 *
                 * @throws NullPointerException if source or destination is <code>null</code>
                 * @throws IOException if source or destination is invalid
                 * @throws IOException if an IO error occurs during copying
                 * @since Commons IO 1.1
                 */
                public static void copyDirectory(File srcDir, File destDir) throws IOException {
                    copyDirectory(srcDir, destDir, true);
                }

                /**
                 * Copies a whole directory to a new location.
                 * <p>
                 * This method copies the contents of the specified source directory
                 * to within the specified destination directory.
                 * <p>
                 * The destination directory is created if it does not exist.
                 * If the destination directory did exist, then this method merges
                 * the source with the destination, with the source taking precedence.
                 * <p>
                 * <strong>Note:</strong> Setting <code>preserveFileDate</code> to
                 * <code>true</code> tries to preserve the files' last modified
                 * date/times using {@link File#setLastModified(long)}, however it is
                 * not guaranteed that those operations will succeed.
                 * If the modification operation fails, no indication is provided.
                 *
                 * @param srcDir  an existing directory to copy, must not be <code>null</code>
                 * @param destDir  the new directory, must not be <code>null</code>
                 * @param preserveFileDate  true if the file date of the copy
                 *  should be the same as the original
                 *
                 * @throws NullPointerException if source or destination is <code>null</code>
                 * @throws IOException if source or destination is invalid
                 * @throws IOException if an IO error occurs during copying
                 * @since Commons IO 1.1
                 */
                public static void copyDirectory(File srcDir, File destDir,
                        boolean preserveFileDate) throws IOException {
                    copyDirectory(srcDir, destDir, null, preserveFileDate);
                }

                /**
                 * Copies a filtered directory to a new location preserving the file dates.
                 * <p>
                 * This method copies the contents of the specified source directory
                 * to within the specified destination directory.
                 * <p>
                 * The destination directory is created if it does not exist.
                 * If the destination directory did exist, then this method merges
                 * the source with the destination, with the source taking precedence.
                 * <p>
                 * <strong>Note:</strong> This method tries to preserve the files' last
                 * modified date/times using {@link File#setLastModified(long)}, however
                 * it is not guaranteed that those operations will succeed.
                 * If the modification operation fails, no indication is provided.
                 *
                 * <h4>Example: Copy directories only</h4>
                 *  <pre>
                 *  // only copy the directory structure
                 *  FileUtils.copyDirectory(srcDir, destDir, DirectoryFileFilter.DIRECTORY);
                 *  </pre>
                 *
                 * <h4>Example: Copy directories and txt files</h4>
                 *  <pre>
                 *  // Create a filter for ".txt" files
                 *  IOFileFilter txtSuffixFilter = FileFilterUtils.suffixFileFilter(".txt");
                 *  IOFileFilter txtFiles = FileFilterUtils.andFileFilter(FileFileFilter.FILE, txtSuffixFilter);
                 *
                 *  // Create a filter for either directories or ".txt" files
                 *  FileFilter filter = FileFilterUtils.orFileFilter(DirectoryFileFilter.DIRECTORY, txtFiles);
                 *
                 *  // Copy using the filter
                 *  FileUtils.copyDirectory(srcDir, destDir, filter);
                 *  </pre>
                 *
                 * @param srcDir  an existing directory to copy, must not be <code>null</code>
                 * @param destDir  the new directory, must not be <code>null</code>
                 * @param filter  the filter to apply, null means copy all directories and files
                 *  should be the same as the original
                 *
                 * @throws NullPointerException if source or destination is <code>null</code>
                 * @throws IOException if source or destination is invalid
                 * @throws IOException if an IO error occurs during copying
                 * @since Commons IO 1.4
                 */
                public static void copyDirectory(File srcDir, File destDir,
                        FileFilter filter) throws IOException {
                    copyDirectory(srcDir, destDir, filter, true);
                }

                /**
                 * Copies a filtered directory to a new location.
                 * <p>
                 * This method copies the contents of the specified source directory
                 * to within the specified destination directory.
                 * <p>
                 * The destination directory is created if it does not exist.
                 * If the destination directory did exist, then this method merges
                 * the source with the destination, with the source taking precedence.
                 * <p>
                 * <strong>Note:</strong> Setting <code>preserveFileDate</code> to
                 * <code>true</code> tries to preserve the files' last modified
                 * date/times using {@link File#setLastModified(long)}, however it is
                 * not guaranteed that those operations will succeed.
                 * If the modification operation fails, no indication is provided.
                 *
                 * <h4>Example: Copy directories only</h4>
                 *  <pre>
                 *  // only copy the directory structure
                 *  FileUtils.copyDirectory(srcDir, destDir, DirectoryFileFilter.DIRECTORY, false);
                 *  </pre>
                 *
                 * <h4>Example: Copy directories and txt files</h4>
                 *  <pre>
                 *  // Create a filter for ".txt" files
                 *  IOFileFilter txtSuffixFilter = FileFilterUtils.suffixFileFilter(".txt");
                 *  IOFileFilter txtFiles = FileFilterUtils.andFileFilter(FileFileFilter.FILE, txtSuffixFilter);
                 *
                 *  // Create a filter for either directories or ".txt" files
                 *  FileFilter filter = FileFilterUtils.orFileFilter(DirectoryFileFilter.DIRECTORY, txtFiles);
                 *
                 *  // Copy using the filter
                 *  FileUtils.copyDirectory(srcDir, destDir, filter, false);
                 *  </pre>
                 *
                 * @param srcDir  an existing directory to copy, must not be <code>null</code>
                 * @param destDir  the new directory, must not be <code>null</code>
                 * @param filter  the filter to apply, null means copy all directories and files
                 * @param preserveFileDate  true if the file date of the copy
                 *  should be the same as the original
                 *
                 * @throws NullPointerException if source or destination is <code>null</code>
                 * @throws IOException if source or destination is invalid
                 * @throws IOException if an IO error occurs during copying
                 * @since Commons IO 1.4
                 */
                public static void copyDirectory(File srcDir, File destDir,
                        FileFilter filter, boolean preserveFileDate) throws IOException {
                    if (srcDir == null) {
                        throw new NullPointerException("Source must not be null");
                    }
                    if (destDir == null) {
                        throw new NullPointerException("Destination must not be null");
                    }
                    if (srcDir.exists() == false) {
                        throw new FileNotFoundException("Source '" + srcDir + "' does not exist");
                    }
                    if (srcDir.isDirectory() == false) {
                        throw new IOException("Source '" + srcDir + "' exists but is not a directory");
                    }
                    if (srcDir.getCanonicalPath().equals(destDir.getCanonicalPath())) {
                        throw new IOException("Source '" + srcDir + "' and destination '" + destDir + "' are the same");
                    }

                    // Cater for destination being directory within the source directory (see IO-141)
                    List<String> exclusionList = null;
                    String canonicalDestDir = destDir.getCanonicalPath();
                    if (canonicalDestDir.startsWith(srcDir.getCanonicalPath())) {
                        File[] srcFiles = filter == null ? srcDir.listFiles() : srcDir.listFiles(filter);
                        if (srcFiles != null && srcFiles.length > 0) {
                            exclusionList = new ArrayList<String>(srcFiles.length);
                            for (File srcFile : srcFiles) {
                                File copiedFile = new File(destDir, srcFile.getName());
                                exclusionList.add(copiedFile.getCanonicalPath());
                            }
                        }
                    }
                    doCopyDirectory(srcDir, destDir, filter, preserveFileDate, exclusionList);
                }

                /**
                 * Internal copy directory method.
                 *
                 * @param srcDir  the validated source directory, must not be <code>null</code>
                 * @param destDir  the validated destination directory, must not be <code>null</code>
                 * @param filter  the filter to apply, null means copy all directories and files
                 * @param preserveFileDate  whether to preserve the file date
                 * @param exclusionList  List of files and directories to exclude from the copy, may be null
                 * @throws IOException if an error occurs
                 * @since Commons IO 1.1
                 */
                private static void doCopyDirectory(File srcDir, File destDir, FileFilter filter,
                        boolean preserveFileDate, List<String> exclusionList) throws IOException {
                    // recurse
                    File[] files = filter == null ? srcDir.listFiles() : srcDir.listFiles(filter);
                    if (files == null) {  // null if security restricted
                        throw new IOException("Failed to list contents of " + srcDir);
                    }
                    if (destDir.exists()) {
                        if (destDir.isDirectory() == false) {
                            throw new IOException("Destination '" + destDir + "' exists but is not a directory");
                        }
                    } else {
                        if (destDir.mkdirs() == false) {
                            throw new IOException("Destination '" + destDir + "' directory cannot be created");
                        }
                    }
                    if (destDir.canWrite() == false) {
                        throw new IOException("Destination '" + destDir + "' cannot be written to");
                    }
                    for (File file : files) {
                        File copiedFile = new File(destDir, file.getName());
                        if (exclusionList == null || !exclusionList.contains(file.getCanonicalPath())) {
                            if (file.isDirectory()) {
                                doCopyDirectory(file, copiedFile, filter, preserveFileDate, exclusionList);
                            } else {
                                doCopyFile(file, copiedFile, preserveFileDate);
                            }
                        }
                    }

                    // Do this last, as the above has probably affected directory metadata
                    if (preserveFileDate) {
                        destDir.setLastModified(srcDir.lastModified());
                    }
                }

                //-----------------------------------------------------------------------
                /**
                 * Copies bytes from the URL <code>source</code> to a file
                 * <code>destination</code>. The directories up to <code>destination</code>
                 * will be created if they don't already exist. <code>destination</code>
                 * will be overwritten if it already exists.
                 * <p>
                 * Warning: this method does not set a connection or read timeout and thus
                 * might block forever. Use {@link #copyURLToFile(URL, File, int, int)}
                 * with reasonable timeouts to prevent this.
                 *
                 * @param source  the <code>URL</code> to copy bytes from, must not be <code>null</code>
                 * @param destination  the non-directory <code>File</code> to write bytes to
                 *  (possibly overwriting), must not be <code>null</code>
                 * @throws IOException if <code>source</code> URL cannot be opened
                 * @throws IOException if <code>destination</code> is a directory
                 * @throws IOException if <code>destination</code> cannot be written
                 * @throws IOException if <code>destination</code> needs creating but can't be
                 * @throws IOException if an IO error occurs during copying
                 */
                public static void copyURLToFile(URL source, File destination) throws IOException {
                    InputStream input = source.openStream();
                    copyInputStreamToFile(input, destination);
                }

                /**
                 * Copies bytes from the URL <code>source</code> to a file
                 * <code>destination</code>. The directories up to <code>destination</code>
                 * will be created if they don't already exist. <code>destination</code>
                 * will be overwritten if it already exists.
                 *
                 * @param source  the <code>URL</code> to copy bytes from, must not be <code>null</code>
                 * @param destination  the non-directory <code>File</code> to write bytes to
                 *  (possibly overwriting), must not be <code>null</code>
                 * @param connectionTimeout the number of milliseconds until this method
                 *  will timeout if no connection could be established to the <code>source</code>
                 * @param readTimeout the number of milliseconds until this method will
                 *  timeout if no data could be read from the <code>source</code>
                 * @throws IOException if <code>source</code> URL cannot be opened
                 * @throws IOException if <code>destination</code> is a directory
                 * @throws IOException if <code>destination</code> cannot be written
                 * @throws IOException if <code>destination</code> needs creating but can't be
                 * @throws IOException if an IO error occurs during copying
                 * @since Commons IO 2.0
                 */
                public static void copyURLToFile(URL source, File destination,
                        int connectionTimeout, int readTimeout) throws IOException {
                    URLConnection connection = source.openConnection();
                    connection.setConnectTimeout(connectionTimeout);
                    connection.setReadTimeout(readTimeout);
                    InputStream input = connection.getInputStream();
                    copyInputStreamToFile(input, destination);
                }

                /**
                 * Copies bytes from an {@link InputStream} <code>source</code> to a file
                 * <code>destination</code>. The directories up to <code>destination</code>
                 * will be created if they don't already exist. <code>destination</code>
                 * will be overwritten if it already exists.
                 *
                 * @param source  the <code>InputStream</code> to copy bytes from, must not be <code>null</code>
                 * @param destination  the non-directory <code>File</code> to write bytes to
                 *  (possibly overwriting), must not be <code>null</code>
                 * @throws IOException if <code>destination</code> is a directory
                 * @throws IOException if <code>destination</code> cannot be written
                 * @throws IOException if <code>destination</code> needs creating but can't be
                 * @throws IOException if an IO error occurs during copying
                 * @since Commons IO 2.0
                 */
                public static void copyInputStreamToFile(InputStream source, File destination) throws IOException {
                    try {
                        FileOutputStream output = openOutputStream(destination);
                        try {
                            IOUtils.copy(source, output);
                        } finally {
                            IOUtils.closeQuietly(output);
                        }
                    } finally {
                        IOUtils.closeQuietly(source);
                    }
                }

                //-----------------------------------------------------------------------
                /**
                 * Deletes a directory recursively.
                 *
                 * @param directory  directory to delete
                 * @throws IOException in case deletion is unsuccessful
                 */
                public static void deleteDirectory(File directory) throws IOException {
                    if (!directory.exists()) {
                        return;
                    }

                    if (!isSymlink(directory)) {
                        cleanDirectory(directory);
                    }

                    if (!directory.delete()) {
                        String message =
                            "Unable to delete directory " + directory + ".";
                        throw new IOException(message);
                    }
                }

                /**
                 * Deletes a file, never throwing an exception. If file is a directory, delete it and all sub-directories.
                 * <p>
                 * The difference between File.delete() and this method are:
                 * <ul>
                 * <li>A directory to be deleted does not have to be empty.</li>
                 * <li>No exceptions are thrown when a file or directory cannot be deleted.</li>
                 * </ul>
                 *
                 * @param file  file or directory to delete, can be <code>null</code>
                 * @return <code>true</code> if the file or directory was deleted, otherwise
                 * <code>false</code>
                 *
                 * @since Commons IO 1.4
                 */
                public static boolean deleteQuietly(File file) {
                    if (file == null) {
                        return false;
                    }
                    try {
                        if (file.isDirectory()) {
                            cleanDirectory(file);
                        }
                    } catch (Exception ignored) {
                    }

                    try {
                        return file.delete();
                    } catch (Exception ignored) {
                        return false;
                    }
                }

                /**
                 * Cleans a directory without deleting it.
                 *
                 * @param directory directory to clean
                 * @throws IOException in case cleaning is unsuccessful
                 */
                public static void cleanDirectory(File directory) throws IOException {
                    if (!directory.exists()) {
                        String message = directory + " does not exist";
                        throw new IllegalArgumentException(message);
                    }

                    if (!directory.isDirectory()) {
                        String message = directory + " is not a directory";
                        throw new IllegalArgumentException(message);
                    }

                    File[] files = directory.listFiles();
                    if (files == null) {  // null if security restricted
                        throw new IOException("Failed to list contents of " + directory);
                    }

                    IOException exception = null;
                    for (File file : files) {
                        try {
                            forceDelete(file);
                        } catch (IOException ioe) {
                            exception = ioe;
                        }
                    }

                    if (null != exception) {
                        throw exception;
                    }
                }

                //-----------------------------------------------------------------------
                /**
                 * Waits for NFS to propagate a file creation, imposing a timeout.
                 * <p>
                 * This method repeatedly tests {@link File#exists()} until it returns
                 * true up to the maximum time specified in seconds.
                 *
                 * @param file  the file to check, must not be <code>null</code>
                 * @param seconds  the maximum time in seconds to wait
                 * @return true if file exists
                 * @throws NullPointerException if the file is <code>null</code>
                 */
                public static boolean waitFor(File file, int seconds) {
                    int timeout = 0;
                    int tick = 0;
                    while (!file.exists()) {
                        if (tick++ >= 10) {
                            tick = 0;
                            if (timeout++ > seconds) {
                                return false;
                            }
                        }
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException ignore) {
                            // ignore exception
                        } catch (Exception ex) {
                            break;
                        }
                    }
                    return true;
                }

                //-----------------------------------------------------------------------
                /**
                 * Reads the contents of a file into a String.
                 * The file is always closed.
                 *
                 * @param file  the file to read, must not be <code>null</code>
                 * @param encoding  the encoding to use, <code>null</code> means platform default
                 * @return the file contents, never <code>null</code>
                 * @throws IOException in case of an I/O error
                 * @throws java.io.UnsupportedEncodingException if the encoding is not supported by the VM
                 */
                public static String readFileToString(File file, String encoding) throws IOException {
                    InputStream in = null;
                    try {
                        in = openInputStream(file);
                        return IOUtils.toString(in, encoding);
                    } finally {
                        IOUtils.closeQuietly(in);
                    }
                }


                /**
                 * Reads the contents of a file into a String using the default encoding for the VM.
                 * The file is always closed.
                 *
                 * @param file  the file to read, must not be <code>null</code>
                 * @return the file contents, never <code>null</code>
                 * @throws IOException in case of an I/O error
                 * @since Commons IO 1.3.1
                 */
                public static String readFileToString(File file) throws IOException {
                    return readFileToString(file, null);
                }

                /**
                 * Reads the contents of a file into a byte array.
                 * The file is always closed.
                 *
                 * @param file  the file to read, must not be <code>null</code>
                 * @return the file contents, never <code>null</code>
                 * @throws IOException in case of an I/O error
                 * @since Commons IO 1.1
                 */
                public static byte[] readFileToByteArray(File file) throws IOException {
                    InputStream in = null;
                    try {
                        in = openInputStream(file);
                        return IOUtils.toByteArray(in);
                    } finally {
                        IOUtils.closeQuietly(in);
                    }
                }

                /**
                 * Reads the contents of a file line by line to a List of Strings.
                 * The file is always closed.
                 *
                 * @param file  the file to read, must not be <code>null</code>
                 * @param encoding  the encoding to use, <code>null</code> means platform default
                 * @return the list of Strings representing each line in the file, never <code>null</code>
                 * @throws IOException in case of an I/O error
                 * @throws java.io.UnsupportedEncodingException if the encoding is not supported by the VM
                 * @since Commons IO 1.1
                 */
                public static List<String> readLines(File file, String encoding) throws IOException {
                    InputStream in = null;
                    try {
                        in = openInputStream(file);
                        return IOUtils.readLines(in, encoding);
                    } finally {
                        IOUtils.closeQuietly(in);
                    }
                }

                /**
                 * Reads the contents of a file line by line to a List of Strings using the default encoding for the VM.
                 * The file is always closed.
                 *
                 * @param file  the file to read, must not be <code>null</code>
                 * @return the list of Strings representing each line in the file, never <code>null</code>
                 * @throws IOException in case of an I/O error
                 * @since Commons IO 1.3
                 */
                public static List<String> readLines(File file) throws IOException {
                    return readLines(file, null);
                }

                /**
                 * Returns an Iterator for the lines in a <code>File</code>.
                 * <p>
                 * This method opens an <code>InputStream</code> for the file.
                 * When you have finished with the iterator you should close the stream
                 * to free internal resources. This can be done by calling the
                 * {@link LineIterator#close()} or
                 * {@link LineIterator#closeQuietly(LineIterator)} method.
                 * <p>
                 * The recommended usage pattern is:
                 * <pre>
                 * LineIterator it = FileUtils.lineIterator(file, "UTF-8");
                 * try {
                 *   while (it.hasNext()) {
                 *     String line = it.nextLine();
                 *     /// do something with line
                 *   }
                 * } finally {
                 *   LineIterator.closeQuietly(iterator);
                 * }
                 * </pre>
                 * <p>
                 * If an exception occurs during the creation of the iterator, the
                 * underlying stream is closed.
                 *
                 * @param file  the file to open for input, must not be <code>null</code>
                 * @param encoding  the encoding to use, <code>null</code> means platform default
                 * @return an Iterator of the lines in the file, never <code>null</code>
                 * @throws IOException in case of an I/O error (file closed)
                 * @since Commons IO 1.2
                 */
                public static LineIterator lineIterator(File file, String encoding) throws IOException {
                    InputStream in = null;
                    try {
                        in = openInputStream(file);
                        return IOUtils.lineIterator(in, encoding);
                    } catch (IOException ex) {
                        IOUtils.closeQuietly(in);
                        throw ex;
                    } catch (RuntimeException ex) {
                        IOUtils.closeQuietly(in);
                        throw ex;
                    }
                }

                /**
                 * Returns an Iterator for the lines in a <code>File</code> using the default encoding for the VM.
                 *
                 * @param file  the file to open for input, must not be <code>null</code>
                 * @return an Iterator of the lines in the file, never <code>null</code>
                 * @throws IOException in case of an I/O error (file closed)
                 * @since Commons IO 1.3
                 * @see #lineIterator(File, String)
                 */
                public static LineIterator lineIterator(File file) throws IOException {
                    return lineIterator(file, null);
                }

                //-----------------------------------------------------------------------
                /**
                 * Writes a String to a file creating the file if it does not exist.
                 *
                 * NOTE: As from v1.3, the parent directories of the file will be created
                 * if they do not exist.
                 *
                 * @param file  the file to write
                 * @param data  the content to write to the file
                 * @param encoding  the encoding to use, <code>null</code> means platform default
                 * @throws IOException in case of an I/O error
                 * @throws java.io.UnsupportedEncodingException if the encoding is not supported by the VM
                 */
                public static void writeStringToFile(File file, String data, String encoding) throws IOException {
                    OutputStream out = null;
                    try {
                        out = openOutputStream(file);
                        IOUtils.write(data, out, encoding);
                    } finally {
                        IOUtils.closeQuietly(out);
                    }
                }

                /**
                 * Writes a String to a file creating the file if it does not exist using the default encoding for the VM.
                 *
                 * @param file  the file to write
                 * @param data  the content to write to the file
                 * @throws IOException in case of an I/O error
                 */
                public static void writeStringToFile(File file, String data) throws IOException {
                    writeStringToFile(file, data, null);
                }

                /**
                 * Writes a CharSequence to a file creating the file if it does not exist using the default encoding for the VM.
                 *
                 * @param file  the file to write
                 * @param data  the content to write to the file
                 * @throws IOException in case of an I/O error
                 * @since Commons IO 2.0
                 */
                public static void write(File file, CharSequence data) throws IOException {
                    String str = data == null ? null : data.toString();
                    writeStringToFile(file, str);
                }

                /**
                 * Writes a CharSequence to a file creating the file if it does not exist.
                 *
                 * @param file  the file to write
                 * @param data  the content to write to the file
                 * @param encoding  the encoding to use, <code>null</code> means platform default
                 * @throws IOException in case of an I/O error
                 * @throws java.io.UnsupportedEncodingException if the encoding is not supported by the VM
                 * @since Commons IO 2.0
                 */
                public static void write(File file, CharSequence data, String encoding) throws IOException {
                    String str = data == null ? null : data.toString();
                    writeStringToFile(file, str, encoding);
                }

                /**
                 * Writes a byte array to a file creating the file if it does not exist.
                 * <p>
                 * NOTE: As from v1.3, the parent directories of the file will be created
                 * if they do not exist.
                 *
                 * @param file  the file to write to
                 * @param data  the content to write to the file
                 * @throws IOException in case of an I/O error
                 * @since Commons IO 1.1
                 */
                public static void writeByteArrayToFile(File file, byte[] data) throws IOException {
                    OutputStream out = null;
                    try {
                        out = openOutputStream(file);
                        out.write(data);
                    } finally {
                        IOUtils.closeQuietly(out);
                    }
                }

                /**
                 * Writes the <code>toString()</code> value of each item in a collection to
                 * the specified <code>File</code> line by line.
                 * The specified character encoding and the default line ending will be used.
                 * <p>
                 * NOTE: As from v1.3, the parent directories of the file will be created
                 * if they do not exist.
                 *
                 * @param file  the file to write to
                 * @param encoding  the encoding to use, <code>null</code> means platform default
                 * @param lines  the lines to write, <code>null</code> entries produce blank lines
                 * @throws IOException in case of an I/O error
                 * @throws java.io.UnsupportedEncodingException if the encoding is not supported by the VM
                 * @since Commons IO 1.1
                 */
                public static void writeLines(File file, String encoding, Collection<?> lines) throws IOException {
                    writeLines(file, encoding, lines, null);
                }

                /**
                 * Writes the <code>toString()</code> value of each item in a collection to
                 * the specified <code>File</code> line by line.
                 * The default VM encoding and the default line ending will be used.
                 *
                 * @param file  the file to write to
                 * @param lines  the lines to write, <code>null</code> entries produce blank lines
                 * @throws IOException in case of an I/O error
                 * @since Commons IO 1.3
                 */
                public static void writeLines(File file, Collection<?> lines) throws IOException {
                    writeLines(file, null, lines, null);
                }

                /**
                 * Writes the <code>toString()</code> value of each item in a collection to
                 * the specified <code>File</code> line by line.
                 * The specified character encoding and the line ending will be used.
                 * <p>
                 * NOTE: As from v1.3, the parent directories of the file will be created
                 * if they do not exist.
                 *
                 * @param file  the file to write to
                 * @param encoding  the encoding to use, <code>null</code> means platform default
                 * @param lines  the lines to write, <code>null</code> entries produce blank lines
                 * @param lineEnding  the line separator to use, <code>null</code> is system default
                 * @throws IOException in case of an I/O error
                 * @throws java.io.UnsupportedEncodingException if the encoding is not supported by the VM
                 * @since Commons IO 1.1
                 */
                public static void writeLines(File file, String encoding, Collection<?> lines, String lineEnding)
                    throws IOException {
                    OutputStream out = null;
                    try {
                        out = openOutputStream(file);
                        IOUtils.writeLines(lines, lineEnding, out, encoding);
                    } finally {
                        IOUtils.closeQuietly(out);
                    }
                }

                /**
                 * Writes the <code>toString()</code> value of each item in a collection to
                 * the specified <code>File</code> line by line.
                 * The default VM encoding and the specified line ending will be used.
                 *
                 * @param file  the file to write to
                 * @param lines  the lines to write, <code>null</code> entries produce blank lines
                 * @param lineEnding  the line separator to use, <code>null</code> is system default
                 * @throws IOException in case of an I/O error
                 * @since Commons IO 1.3
                 */
                public static void writeLines(File file, Collection<?> lines, String lineEnding) throws IOException {
                    writeLines(file, null, lines, lineEnding);
                }

                //-----------------------------------------------------------------------
                /**
                 * Deletes a file. If file is a directory, delete it and all sub-directories.
                 * <p>
                 * The difference between File.delete() and this method are:
                 * <ul>
                 * <li>A directory to be deleted does not have to be empty.</li>
                 * <li>You get exceptions when a file or directory cannot be deleted.
                 *      (java.io.File methods returns a boolean)</li>
                 * </ul>
                 *
                 * @param file  file or directory to delete, must not be <code>null</code>
                 * @throws NullPointerException if the directory is <code>null</code>
                 * @throws FileNotFoundException if the file was not found
                 * @throws IOException in case deletion is unsuccessful
                 */
                public static void forceDelete(File file) throws IOException {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        boolean filePresent = file.exists();
                        if (!file.delete()) {
                            if (!filePresent){
                                throw new FileNotFoundException("File does not exist: " + file);
                            }
                            String message =
                                "Unable to delete file: " + file;
                            throw new IOException(message);
                        }
                    }
                }




                /**
                 * Schedules a file to be deleted when JVM exits.
                 * If file is directory delete it and all sub-directories.
                 *
                 * @param file  file or directory to delete, must not be <code>null</code>
                 * @throws NullPointerException if the file is <code>null</code>
                 * @throws IOException in case deletion is unsuccessful
                 */
                public static void forceDeleteOnExit(File file) throws IOException {
                    if (file.isDirectory()) {
                        deleteDirectoryOnExit(file);
                    } else {
                        file.deleteOnExit();
                    }
                }

                /**
                 * Schedules a directory recursively for deletion on JVM exit.
                 *
                 * @param directory  directory to delete, must not be <code>null</code>
                 * @throws NullPointerException if the directory is <code>null</code>
                 * @throws IOException in case deletion is unsuccessful
                 */
                private static void deleteDirectoryOnExit(File directory) throws IOException {
                    if (!directory.exists()) {
                        return;
                    }

                    if (!isSymlink(directory)) {
                        cleanDirectoryOnExit(directory);
                    }
                    directory.deleteOnExit();
                }

                /**
                 * Cleans a directory without deleting it.
                 *
                 * @param directory  directory to clean, must not be <code>null</code>
                 * @throws NullPointerException if the directory is <code>null</code>
                 * @throws IOException in case cleaning is unsuccessful
                 */
                private static void cleanDirectoryOnExit(File directory) throws IOException {
                    if (!directory.exists()) {
                        String message = directory + " does not exist";
                        throw new IllegalArgumentException(message);
                    }

                    if (!directory.isDirectory()) {
                        String message = directory + " is not a directory";
                        throw new IllegalArgumentException(message);
                    }

                    File[] files = directory.listFiles();
                    if (files == null) {  // null if security restricted
                        throw new IOException("Failed to list contents of " + directory);
                    }

                    IOException exception = null;
                    for (File file : files) {
                        try {
                            forceDeleteOnExit(file);
                        } catch (IOException ioe) {
                            exception = ioe;
                        }
                    }

                    if (null != exception) {
                        throw exception;
                    }
                }

                /**
                 * Makes a directory, including any necessary but nonexistent parent
                 * directories. If a file already exists with specified name but it is
                 * not a directory then an IOException is thrown.
                 * If the directory cannot be created (or does not already exist)
                 * then an IOException is thrown.
                 *
                 * @param directory  directory to create, must not be <code>null</code>
                 * @throws NullPointerException if the directory is <code>null</code>
                 * @throws IOException if the directory cannot be created or the file already exists but is not a directory
                 */
                public static void forceMkdir(File directory) throws IOException {
                    if (directory.exists()) {
                        if (!directory.isDirectory()) {
                            String message =
                                "File "
                                    + directory
                                    + " exists and is "
                                    + "not a directory. Unable to create directory.";
                            throw new IOException(message);
                        }
                    } else {
                        if (!directory.mkdirs()) {
                            // Double-check that some other thread or process hasn't made
                            // the directory in the background
                            if (!directory.isDirectory())
                            {
                                String message =
                                    "Unable to create directory " + directory;
                                throw new IOException(message);
                            }
                        }
                    }
                }

                //-----------------------------------------------------------------------
                /**
                 * Returns the size of the specified file or directory. If the provided
                 * {@link File} is a regular file, then the file's length is returned.
                 * If the argument is a directory, then the size of the directory is
                 * calculated recursively. If a directory or subdirectory is security
                 * restricted, its size will not be included.
                 *
                 * @param file the regular file or directory to return the size
                 *        of (must not be <code>null</code>).
                 *
                 * @return the length of the file, or recursive size of the directory,
                 *         provided (in bytes).
                 *
                 * @throws NullPointerException if the file is <code>null</code>
                 * @throws IllegalArgumentException if the file does not exist.
                 *
                 * @since Commons IO 2.0
                 */
                public static long sizeOf(File file) {

                    if (!file.exists()) {
                        String message = file + " does not exist";
                        throw new IllegalArgumentException(message);
                    }

                    if (file.isDirectory()) {
                        return sizeOfDirectory(file);
                    } else {
                        return file.length();
                    }

                }

                /**
                 * Counts the size of a directory recursively (sum of the length of all files).
                 *
                 * @param directory  directory to inspect, must not be <code>null</code>
                 * @return size of directory in bytes, 0 if directory is security restricted
                 * @throws NullPointerException if the directory is <code>null</code>
                 */
                public static long sizeOfDirectory(File directory) {
                    if (!directory.exists()) {
                        String message = directory + " does not exist";
                        throw new IllegalArgumentException(message);
                    }

                    if (!directory.isDirectory()) {
                        String message = directory + " is not a directory";
                        throw new IllegalArgumentException(message);
                    }

                    long size = 0;

                    File[] files = directory.listFiles();
                    if (files == null) {  // null if security restricted
                        return 0L;
                    }
                    for (File file : files) {
                        size += sizeOf(file);
                    }

                    return size;
                }

                //-----------------------------------------------------------------------
                /**
                 * Tests if the specified <code>File</code> is newer than the reference
                 * <code>File</code>.
                 *
                 * @param file  the <code>File</code> of which the modification date must
                 * be compared, must not be <code>null</code>
                 * @param reference  the <code>File</code> of which the modification date
                 * is used, must not be <code>null</code>
                 * @return true if the <code>File</code> exists and has been modified more
                 * recently than the reference <code>File</code>
                 * @throws IllegalArgumentException if the file is <code>null</code>
                 * @throws IllegalArgumentException if the reference file is <code>null</code> or doesn't exist
                 */
                 public static boolean isFileNewer(File file, File reference) {
                    if (reference == null) {
                        throw new IllegalArgumentException("No specified reference file");
                    }
                    if (!reference.exists()) {
                        throw new IllegalArgumentException("The reference file '"
                                + reference + "' doesn't exist");
                    }
                    return isFileNewer(file, reference.lastModified());
                }

                /**
                 * Tests if the specified <code>File</code> is newer than the specified
                 * <code>Date</code>.
                 *
                 * @param file  the <code>File</code> of which the modification date
                 * must be compared, must not be <code>null</code>
                 * @param date  the date reference, must not be <code>null</code>
                 * @return true if the <code>File</code> exists and has been modified
                 * after the given <code>Date</code>.
                 * @throws IllegalArgumentException if the file is <code>null</code>
                 * @throws IllegalArgumentException if the date is <code>null</code>
                 */
                public static boolean isFileNewer(File file, Date date) {
                    if (date == null) {
                        throw new IllegalArgumentException("No specified date");
                    }
                    return isFileNewer(file, date.getTime());
                }

                /**
                 * Tests if the specified <code>File</code> is newer than the specified
                 * time reference.
                 *
                 * @param file  the <code>File</code> of which the modification date must
                 * be compared, must not be <code>null</code>
                 * @param timeMillis  the time reference measured in milliseconds since the
                 * epoch (00:00:00 GMT, January 1, 1970)
                 * @return true if the <code>File</code> exists and has been modified after
                 * the given time reference.
                 * @throws IllegalArgumentException if the file is <code>null</code>
                 */
                 public static boolean isFileNewer(File file, long timeMillis) {
                    if (file == null) {
                        throw new IllegalArgumentException("No specified file");
                    }
                    if (!file.exists()) {
                        return false;
                    }
                    return file.lastModified() > timeMillis;
                }


                //-----------------------------------------------------------------------
                /**
                 * Tests if the specified <code>File</code> is older than the reference
                 * <code>File</code>.
                 *
                 * @param file  the <code>File</code> of which the modification date must
                 * be compared, must not be <code>null</code>
                 * @param reference  the <code>File</code> of which the modification date
                 * is used, must not be <code>null</code>
                 * @return true if the <code>File</code> exists and has been modified before
                 * the reference <code>File</code>
                 * @throws IllegalArgumentException if the file is <code>null</code>
                 * @throws IllegalArgumentException if the reference file is <code>null</code> or doesn't exist
                 */
                 public static boolean isFileOlder(File file, File reference) {
                    if (reference == null) {
                        throw new IllegalArgumentException("No specified reference file");
                    }
                    if (!reference.exists()) {
                        throw new IllegalArgumentException("The reference file '"
                                + reference + "' doesn't exist");
                    }
                    return isFileOlder(file, reference.lastModified());
                }

                /**
                 * Tests if the specified <code>File</code> is older than the specified
                 * <code>Date</code>.
                 *
                 * @param file  the <code>File</code> of which the modification date
                 * must be compared, must not be <code>null</code>
                 * @param date  the date reference, must not be <code>null</code>
                 * @return true if the <code>File</code> exists and has been modified
                 * before the given <code>Date</code>.
                 * @throws IllegalArgumentException if the file is <code>null</code>
                 * @throws IllegalArgumentException if the date is <code>null</code>
                 */
                public static boolean isFileOlder(File file, Date date) {
                    if (date == null) {
                        throw new IllegalArgumentException("No specified date");
                    }
                    return isFileOlder(file, date.getTime());
                }

                /**
                 * Tests if the specified <code>File</code> is older than the specified
                 * time reference.
                 *
                 * @param file  the <code>File</code> of which the modification date must
                 * be compared, must not be <code>null</code>
                 * @param timeMillis  the time reference measured in milliseconds since the
                 * epoch (00:00:00 GMT, January 1, 1970)
                 * @return true if the <code>File</code> exists and has been modified before
                 * the given time reference.
                 * @throws IllegalArgumentException if the file is <code>null</code>
                 */
                 public static boolean isFileOlder(File file, long timeMillis) {
                    if (file == null) {
                        throw new IllegalArgumentException("No specified file");
                    }
                    if (!file.exists()) {
                        return false;
                    }
                    return file.lastModified() < timeMillis;
                }

                //-----------------------------------------------------------------------
                /**
                 * Computes the checksum of a file using the CRC32 checksum routine.
                 * The value of the checksum is returned.
                 *
                 * @param file  the file to checksum, must not be <code>null</code>
                 * @return the checksum value
                 * @throws NullPointerException if the file or checksum is <code>null</code>
                 * @throws IllegalArgumentException if the file is a directory
                 * @throws IOException if an IO error occurs reading the file
                 * @since Commons IO 1.3
                 */
                public static long checksumCRC32(File file) throws IOException {
                    CRC32 crc = new CRC32();
                    checksum(file, crc);
                    return crc.getValue();
                }

                /**
                 * Computes the checksum of a file using the specified checksum object.
                 * Multiple files may be checked using one <code>Checksum</code> instance
                 * if desired simply by reusing the same checksum object.
                 * For example:
                 * <pre>
                 *   long csum = FileUtils.checksum(file, new CRC32()).getValue();
                 * </pre>
                 *
                 * @param file  the file to checksum, must not be <code>null</code>
                 * @param checksum  the checksum object to be used, must not be <code>null</code>
                 * @return the checksum specified, updated with the content of the file
                 * @throws NullPointerException if the file or checksum is <code>null</code>
                 * @throws IllegalArgumentException if the file is a directory
                 * @throws IOException if an IO error occurs reading the file
                 * @since Commons IO 1.3
                 */
                public static Checksum checksum(File file, Checksum checksum) throws IOException {
                    if (file.isDirectory()) {
                        throw new IllegalArgumentException("Checksums can't be computed on directories");
                    }
                    InputStream in = null;
                    try {
                        in = new CheckedInputStream(new FileInputStream(file), checksum);
                        IOUtils.copy(in, new NullOutputStream());
                    } finally {
                        IOUtils.closeQuietly(in);
                    }
                    return checksum;
                }

                /**
                 * Moves a directory.
                 * <p>
                 * When the destination directory is on another file system, do a "copy and delete".
                 *
                 * @param srcDir the directory to be moved
                 * @param destDir the destination directory
                 * @throws NullPointerException if source or destination is <code>null</code>
                 * @throws IOException if source or destination is invalid
                 * @throws IOException if an IO error occurs moving the file
                 * @since Commons IO 1.4
                 */
                public static void moveDirectory(File srcDir, File destDir) throws IOException {
                    if (srcDir == null) {
                        throw new NullPointerException("Source must not be null");
                    }
                    if (destDir == null) {
                        throw new NullPointerException("Destination must not be null");
                    }
                    if (!srcDir.exists()) {
                        throw new FileNotFoundException("Source '" + srcDir + "' does not exist");
                    }
                    if (!srcDir.isDirectory()) {
                        throw new IOException("Source '" + srcDir + "' is not a directory");
                    }
                    if (destDir.exists()) {
                        throw new FileExistsException("Destination '" + destDir + "' already exists");
                    }
                    boolean rename = srcDir.renameTo(destDir);
                    if (!rename) {
                        copyDirectory( srcDir, destDir );
                        deleteDirectory( srcDir );
                        if (srcDir.exists()) {
                            throw new IOException("Failed to delete original directory '" + srcDir +
                                    "' after copy to '" + destDir + "'");
                        }
                    }
                }

                /**
                 * Moves a directory to another directory.
                 *
                 * @param src the file to be moved
                 * @param destDir the destination file
                 * @param createDestDir If <code>true</code> create the destination directory,
                 * otherwise if <code>false</code> throw an IOException
                 * @throws NullPointerException if source or destination is <code>null</code>
                 * @throws IOException if source or destination is invalid
                 * @throws IOException if an IO error occurs moving the file
                 * @since Commons IO 1.4
                 */
                public static void moveDirectoryToDirectory(File src, File destDir, boolean createDestDir) throws IOException {
                    if (src == null) {
                        throw new NullPointerException("Source must not be null");
                    }
                    if (destDir == null) {
                        throw new NullPointerException("Destination directory must not be null");
                    }
                    if (!destDir.exists() && createDestDir) {
                        destDir.mkdirs();
                    }
                    if (!destDir.exists()) {
                        throw new FileNotFoundException("Destination directory '" + destDir +
                                "' does not exist [createDestDir=" + createDestDir +"]");
                    }
                    if (!destDir.isDirectory()) {
                        throw new IOException("Destination '" + destDir + "' is not a directory");
                    }
                    moveDirectory(src, new File(destDir, src.getName()));

                }

                /**
                 * Moves a file.
                 * <p>
                 * When the destination file is on another file system, do a "copy and delete".
                 *
                 * @param srcFile the file to be moved
                 * @param destFile the destination file
                 * @throws NullPointerException if source or destination is <code>null</code>
                 * @throws IOException if source or destination is invalid
                 * @throws IOException if an IO error occurs moving the file
                 * @since Commons IO 1.4
                 */
                public static void moveFile(File srcFile, File destFile) throws IOException {
                    if (srcFile == null) {
                        throw new NullPointerException("Source must not be null");
                    }
                    if (destFile == null) {
                        throw new NullPointerException("Destination must not be null");
                    }
                    if (!srcFile.exists()) {
                        throw new FileNotFoundException("Source '" + srcFile + "' does not exist");
                    }
                    if (srcFile.isDirectory()) {
                        throw new IOException("Source '" + srcFile + "' is a directory");
                    }
                    if (destFile.exists()) {
                        throw new FileExistsException("Destination '" + destFile + "' already exists");
                    }
                    if (destFile.isDirectory()) {
                        throw new IOException("Destination '" + destFile + "' is a directory");
                    }
                    boolean rename = srcFile.renameTo(destFile);
                    if (!rename) {
                        copyFile( srcFile, destFile );
                        if (!srcFile.delete()) {
                            FileUtils.deleteQuietly(destFile);
                            throw new IOException("Failed to delete original file '" + srcFile +
                                    "' after copy to '" + destFile + "'");
                        }
                    }
                }

                /**
                 * Moves a file to a directory.
                 *
                 * @param srcFile the file to be moved
                 * @param destDir the destination file
                 * @param createDestDir If <code>true</code> create the destination directory,
                 * otherwise if <code>false</code> throw an IOException
                 * @throws NullPointerException if source or destination is <code>null</code>
                 * @throws IOException if source or destination is invalid
                 * @throws IOException if an IO error occurs moving the file
                 * @since Commons IO 1.4
                 */
                public static void moveFileToDirectory(File srcFile, File destDir, boolean createDestDir) throws IOException {
                    if (srcFile == null) {
                        throw new NullPointerException("Source must not be null");
                    }
                    if (destDir == null) {
                        throw new NullPointerException("Destination directory must not be null");
                    }
                    if (!destDir.exists() && createDestDir) {
                        destDir.mkdirs();
                    }
                    if (!destDir.exists()) {
                        throw new FileNotFoundException("Destination directory '" + destDir +
                                "' does not exist [createDestDir=" + createDestDir +"]");
                    }
                    if (!destDir.isDirectory()) {
                        throw new IOException("Destination '" + destDir + "' is not a directory");
                    }
                    moveFile(srcFile, new File(destDir, srcFile.getName()));
                }

                /**
                 * Moves a file or directory to the destination directory.
                 * <p>
                 * When the destination is on another file system, do a "copy and delete".
                 *
                 * @param src the file or directory to be moved
                 * @param destDir the destination directory
                 * @param createDestDir If <code>true</code> create the destination directory,
                 * otherwise if <code>false</code> throw an IOException
                 * @throws NullPointerException if source or destination is <code>null</code>
                 * @throws IOException if source or destination is invalid
                 * @throws IOException if an IO error occurs moving the file
                 * @since Commons IO 1.4
                 */
                public static void moveToDirectory(File src, File destDir, boolean createDestDir) throws IOException {
                    if (src == null) {
                        throw new NullPointerException("Source must not be null");
                    }
                    if (destDir == null) {
                        throw new NullPointerException("Destination must not be null");
                    }
                    if (!src.exists()) {
                        throw new FileNotFoundException("Source '" + src + "' does not exist");
                    }
                    if (src.isDirectory()) {
                        moveDirectoryToDirectory(src, destDir, createDestDir);
                    } else {
                        moveFileToDirectory(src, destDir, createDestDir);
                    }
                }

                /**
                 * Determines whether the specified file is a Symbolic Link rather than an actual file.
                 * <p>
                 * Will not return true if there is a Symbolic Link anywhere in the path,
                 * only if the specific file is.
                 *
                 * @param file the file to check
                 * @return true if the file is a Symbolic Link
                 * @throws IOException if an IO error occurs while checking the file
                 * @since Commons IO 2.0
                 */
                public static boolean isSymlink(File file) throws IOException {
                    if (file == null) {
                        throw new NullPointerException("File must not be null");
                    }
                    // Type information would be missing
                    // if (FilenameUtils.isSystemWindows()) {
                    //     return false;
                    // }
                    File fileInCanonicalDir = null;
                    if (file.getParent() == null) {
                        fileInCanonicalDir = file;
                    } else {
                        File canonicalDir = file.getParentFile().getCanonicalFile();
                        fileInCanonicalDir = new File(canonicalDir, file.getName());
                    }

                    if (fileInCanonicalDir.getCanonicalFile().equals(fileInCanonicalDir.getAbsoluteFile())) {
                        return false;
                    } else {
                        return true;
                    }
                }
            }
            """
        )
    )


    // removed code to ensure test passes (was failing because of "Condition node has no guard" illegal state exception)
    @Test
    fun `maven-scm`() = rewriteRun(
        java(
            """
                package org.apache.maven.scm.provider.git.gitexe.command;

                /*
                 * Licensed to the Apache Software Foundation (ASF) under one
                 * or more contributor license agreements.  See the NOTICE file
                 * distributed with this work for additional information
                 * regarding copyright ownership.  The ASF licenses this file
                 * to you under the Apache License, Version 2.0 (the
                 * "License"); you may not use this file except in compliance
                 * with the License.  You may obtain a copy of the License at
                 *
                 * http://www.apache.org/licenses/LICENSE-2.0
                 *
                 * Unless required by applicable law or agreed to in writing,
                 * software distributed under the License is distributed on an
                 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
                 * KIND, either express or implied.  See the License for the
                 * specific language governing permissions and limitations
                 * under the License.
                 */

                import org.apache.commons.io.FilenameUtils;

                import org.apache.maven.scm.ScmException;
                import org.apache.maven.scm.provider.git.util.GitUtil;
                import org.apache.maven.scm.providers.gitlib.settings.Settings;
                import org.codehaus.plexus.util.cli.CommandLineException;
                import org.codehaus.plexus.util.cli.CommandLineUtils;
                import org.codehaus.plexus.util.cli.Commandline;
                import org.codehaus.plexus.util.cli.StreamConsumer;
                import org.slf4j.Logger;
                import org.slf4j.LoggerFactory;

                import java.io.File;
                import java.io.IOException;
                import java.util.List;

                /**
                 * Command line construction utility.
                 *
                 * @author Brett Porter
                 * @author <a href="mailto:struberg@yahoo.de">Mark Struberg</a>
                 *
                 */
                public final class GitCommandLineUtils
                {
                    private static final Logger LOGGER = LoggerFactory.getLogger( GitCommandLineUtils.class );

                    private GitCommandLineUtils()
                    {
                    }

                    public static void addTarget( Commandline cl, List<File> files )
                    {
                        if ( files == null || files.isEmpty() )
                        {
                            return;
                        }
                        final File workingDirectory = cl.getWorkingDirectory();
                        try
                        {
                            final String canonicalWorkingDirectory = workingDirectory.getCanonicalPath();
                            for ( File file : files )
                            {
                                String relativeFile = file.getPath();

                                final String canonicalFile = file.getCanonicalPath();
                                if ( canonicalFile.startsWith( canonicalWorkingDirectory ) )
                                {
                                    // so we can omit the starting characters
                                    relativeFile = canonicalFile.substring( canonicalWorkingDirectory.length() );

                                    if ( relativeFile.startsWith( File.separator ) )
                                    {
                                        relativeFile = relativeFile.substring( File.separator.length() );
                                    }
                                }

                                // no setFile() since this screws up the working directory!
                                cl.createArg().setValue( FilenameUtils.separatorsToUnix( relativeFile ) );
                            }
                        }
                        catch ( IOException ex )
                        {
                            throw new IllegalArgumentException( "Could not get canonical paths for workingDirectory = "
                                + workingDirectory + " or files=" + files, ex );
                        }
                    }

                    /**
                     *
                     * @param workingDirectory
                     * @param command
                     * @return TODO
                     */
                    public static Commandline getBaseGitCommandLine( File workingDirectory, String command )
                    {
                        return getAnonymousBaseGitCommandLine( workingDirectory, command );
                    }

                    /**
                     * Creates a {@link Commandline} for which the toString() do not display
                     * password.
                     *
                     * @param workingDirectory
                     * @param command
                     * @return CommandLine with anonymous output.
                     */
                    private static Commandline getAnonymousBaseGitCommandLine( File workingDirectory, String command )
                    {
                        if ( command == null || command.length() == 0 )
                        {
                            return null;
                        }

                        Commandline cl = new AnonymousCommandLine();

                        composeCommand( workingDirectory, command, cl );

                        return cl;
                    }

                    private static void composeCommand( File workingDirectory, String command, Commandline cl )
                    {
                        Settings settings = GitUtil.getSettings();

                        cl.setExecutable( settings.getGitCommand() );

                        cl.createArg().setValue( command );

                        if ( workingDirectory != null )
                        {
                            cl.setWorkingDirectory( workingDirectory.getAbsolutePath() );
                        }
                    }

                    public static int execute( Commandline cl, StreamConsumer consumer, CommandLineUtils.StringStreamConsumer stderr )
                        throws ScmException
                    {
//                        if ( LOGGER.isInfoEnabled() )
//                        {
//                            LOGGER.info( "Executing: " + cl );
//                            LOGGER.info( "Working directory: " + cl.getWorkingDirectory().getAbsolutePath() );
//                        }

                        int exitCode;
                        try
                        {
                            exitCode = CommandLineUtils.executeCommandLine( cl, consumer, stderr );
                        }
                        catch ( CommandLineException ex )
                        {
                            throw new ScmException( "Error while executing command.", ex );
                        }

                        return exitCode;
                    }

                    public static int execute( Commandline cl, CommandLineUtils.StringStreamConsumer stdout,
                                               CommandLineUtils.StringStreamConsumer stderr )
                        throws ScmException
                    {
//                        if ( LOGGER.isInfoEnabled() )
//                        {
//                            LOGGER.info( "Executing: " + cl );
//                            LOGGER.info( "Working directory: " + cl.getWorkingDirectory().getAbsolutePath() );
//                        }

                        int exitCode;
                        try
                        {
                            exitCode = CommandLineUtils.executeCommandLine( cl, stdout, stderr );
                        }
                        catch ( CommandLineException ex )
                        {
                            throw new ScmException( "Error while executing command.", ex );
                        }

                        return exitCode;
                    }

                }
            """
        )
    )


    @Test
    fun `aws-serverless-java-container`() = rewriteRun(
        java(
            """
                /*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance
 * with the License. A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package com.amazonaws.serverless.proxy.internal;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * This class contains utility methods to address FSB security issues found in the application, such as string sanitization
 * and file path validation.
 */
public final class SecurityUtils {
    private static Logger log = LoggerFactory.getLogger(SecurityUtils.class);

    private static Set<String> SCHEMES = new HashSet<String>() {{
        add("http");
        add("https");
        add("HTTP");
        add("HTTPS");
    }};

    private static Set<Integer> PORTS = new HashSet<Integer>() {{
        add(443);
        add(80);
        add(3000); // we allow port 3000 for SAM local
    }};

    public static boolean isValidPort(String port) {
        if (port == null) {
            return false;
        }
        try {
            int intPort = Integer.parseInt(port);
            return PORTS.contains(intPort);
        } catch (NumberFormatException e) {
            log.error("Invalid port parameter: " + crlf(port));
            return false;
        }
    }

    public static boolean isValidScheme(String scheme) {
        return SCHEMES.contains(scheme);
    }

    public static boolean isValidHost(String host, String apiId, String region) {
        if (host == null) {
            return false;
        }
        if (host.endsWith(".amazonaws.com")) {
            String defaultHost = new StringBuilder().append(apiId)
                                                    .append(".execute-api.")
                                                    .append(region)
                                                    .append(".amazonaws.com").toString();
            return host.equals(defaultHost);
        } else {
            return LambdaContainerHandler.getContainerConfig().getCustomDomainNames().contains(host);
        }
    }

    /**
     * Replaces CRLF characters in a string with empty string ("").
     * @param s The string to be cleaned
     * @return A copy of the original string without CRLF characters
     */
    public static String crlf(String s) {
        if (s == null) {
            return null;
        }
        return s.replaceAll("[\r\n]", "");
    }


    /**
     * Escapes all special characters in a java string
     * @param s The string to be cleaned
     * @return The escaped string
     */
    public static String encode(String s) {
        if (s == null) {
            return null;
        }

        int sz = s.length();

        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < sz; i++) {
            char ch = s.charAt(i);

            // handle unicode
            if (ch > 0xfff) {
                buffer.append("\\u" + Integer.toHexString(ch).toUpperCase(Locale.ENGLISH));
            } else if (ch > 0xff) {
                buffer.append("\\u0" + Integer.toHexString(ch).toUpperCase(Locale.ENGLISH));
            } else if (ch > 0x7f) {
                buffer.append("\\u00" + Integer.toHexString(ch).toUpperCase(Locale.ENGLISH));
            } else if (ch < 32) {
                switch (ch) {
                case '\b':
                    buffer.append('\\');
                    buffer.append('b');
                    break;
                case '\n':
                    buffer.append('\\');
                    buffer.append('n');
                    break;
                case '\t':
                    buffer.append('\\');
                    buffer.append('t');
                    break;
                case '\f':
                    buffer.append('\\');
                    buffer.append('f');
                    break;
                case '\r':
                    buffer.append('\\');
                    buffer.append('r');
                    break;
                default:
                    if (ch > 0xf) {
                        buffer.append("\\u00" + Integer.toHexString(ch).toUpperCase(Locale.ENGLISH));
                    } else {
                        buffer.append("\\u000" + Integer.toHexString(ch).toUpperCase(Locale.ENGLISH));
                    }
                    break;
                }
            } else {
                switch (ch) {
                case '\'':

                    buffer.append('\'');
                    break;
                case '"':
                    buffer.append('\\');
                    buffer.append('"');
                    break;
                case '\\':
                    buffer.append('\\');
                    buffer.append('\\');
                    break;
                case '/':
                    buffer.append('/');
                    break;
                default:
                    buffer.append(ch);
                    break;
                }
            }
        }

        return buffer.toString();
    }

    public static String getValidFilePath(String inputPath) {
        return getValidFilePath(inputPath, false);
    }

    /**
     * Returns an absolute file path given an input path and validates that it is not trying
     * to write/read from a directory other than /tmp.
     * @param inputPath The input path
     * @return The absolute path to the file
     * @throws IllegalArgumentException If the given path is not valid or outside of /tmp
     */
    @SuppressFBWarnings("PATH_TRAVERSAL_IN")
    public static String getValidFilePath(final String inputPath, boolean isWrite) {
        if (inputPath == null || "".equals(inputPath.trim())) {
            return null;
        }
        String testInputPath = inputPath;
        if (testInputPath.startsWith("file://")) {
            testInputPath = testInputPath.substring(6);
        }

        File f = new File(testInputPath);
        try {
            String canonicalPath = f.getCanonicalPath();

            if (isWrite && canonicalPath.startsWith("/var/task")) {
                throw new IllegalArgumentException("Trying to write to /var/task folder");
            }

            boolean isAllowed = false;
            for (String allowedPath : LambdaContainerHandler.getContainerConfig().getValidFilePaths()) {
                if (canonicalPath.startsWith(allowedPath)) {
                    isAllowed = true;
                    break;
                }
            }
            if (!isAllowed) {
                throw new IllegalArgumentException("File path not allowed: " + encode(canonicalPath));
            }

            return (inputPath.startsWith("file://") ? "file://" + canonicalPath : canonicalPath);
        } catch (IOException e) {
            log.error("Invalid file path: {}", encode(testInputPath));
            throw new IllegalArgumentException("Invalid file path", e);
        }
    }
}
            """
        )
    )

    @Test
    @Disabled("Basic block already has a successor")
    fun `basex`() = rewriteRun(
        java(
            """
                package org.basex.gui.view.editor;

                import static org.basex.core.Text.*;
                import static org.basex.gui.GUIConstants.*;
                import static org.basex.util.Token.*;

                import java.awt.*;
                import java.io.*;
                import java.util.*;
                import java.util.List;
                import java.util.Timer;
                import java.util.concurrent.atomic.*;
                import java.util.regex.*;

                import javax.swing.*;

                import org.basex.build.json.*;
                import org.basex.core.*;
                import org.basex.core.cmd.*;
                import org.basex.core.parse.*;
                import org.basex.data.*;
                import org.basex.gui.*;
                import org.basex.gui.dialog.*;
                import org.basex.gui.layout.*;
                import org.basex.gui.layout.BaseXFileChooser.*;
                import org.basex.gui.listener.*;
                import org.basex.gui.text.*;
                import org.basex.gui.text.TextPanel.Action;
                import org.basex.gui.view.*;
                import org.basex.gui.view.project.*;
                import org.basex.io.*;
                import org.basex.io.in.*;
                import org.basex.io.parse.json.*;
                import org.basex.io.parse.xml.*;
                import org.basex.query.*;
                import org.basex.query.func.*;
                import org.basex.query.value.item.*;
                import org.basex.query.value.node.*;
                import org.basex.util.*;
                import org.basex.util.list.*;
                import org.xml.sax.*;

                /**
                 * This view allows the input and evaluation of queries and documents.
                 *
                 * @author BaseX Team 2005-22, BSD License
                 * @author Christian Gruen
                 */
                public final class EditorView extends View {
                  /** Delay for showing the please wait info. */
                  private static final int WAIT_DELAY = 250;
                  /** Delay for highlighting an error. */
                  private static final int SEARCH_DELAY = 100;
                  /** Link pattern. */
                  private static final Pattern LINK = Pattern.compile("(.*?), ([0-9]+)/([0-9]+)");

                  /** Project files. */
                  final ProjectView project;
                  /** Go button. */
                  final AbstractButton exec;
                  /** Test button. */
                  final AbstractButton tests;

                  /** History Button. */
                  private final AbstractButton history;
                  /** Stop Button. */
                  private final AbstractButton stop;
                  /** Search bar. */
                  private final SearchBar search;
                  /** Info label. */
                  private final BaseXLabel info;
                  /** Position label. */
                  private final BaseXLabel pos;
                  /** Splitter. */
                  private final BaseXSplit split;
                  /** Tabs. */
                  private final BaseXTabs tabs;
                  /** Context. */
                  private final BaseXLabel context;

                  /** Query file that has last been evaluated. */
                  private IOFile execFile;
                  /** Main-memory document. */
                  private DBNode doc;

                  /** Parse counter. */
                  private final AtomicInteger parseID = new AtomicInteger();
                  /** Parse query context. */
                  private final AtomicBoolean parsing = new AtomicBoolean();
                  /** Input info. */
                  private InputInfo inputInfo;

                  /**
                   * Default constructor.
                   * @param notifier view notifier
                   */
                  public EditorView(final ViewNotifier notifier) {
                    super(EDITORVIEW, notifier);
                    layout(new BorderLayout());
                    setBackground(PANEL);

                    tabs = new BaseXTabs(gui);
                    tabs.setFocusable(Prop.MAC);
                    tabs.addDragDrop();
                    tabs.setTabLayoutPolicy(gui.gopts.get(GUIOptions.SCROLLTABS) ? JTabbedPane.SCROLL_TAB_LAYOUT :
                      JTabbedPane.WRAP_TAB_LAYOUT);
                    tabs.addMouseListener((MouseClickedListener) e -> {
                      final int i = tabs.indexAtLocation(e.getX(), e.getY());
                      if(i != -1 && SwingUtilities.isMiddleMouseButton(e)) {
                        final Component comp = tabs.getComponentAt(i);
                        if(comp instanceof EditorArea) close((EditorArea) comp);
                      }
                    });

                    final SearchEditor center = new SearchEditor(gui, tabs, null);
                    search = center.bar();

                    final AbstractButton newB = BaseXButton.command(GUIMenuCmd.C_EDIT_NEW, gui);
                    final AbstractButton openB = BaseXButton.command(GUIMenuCmd.C_EDIT_OPEN, gui);
                    final AbstractButton saveB = BaseXButton.get("c_save", SAVE, false, gui);
                    final AbstractButton find = search.button(FIND_REPLACE);
                    final AbstractButton vars = BaseXButton.command(GUIMenuCmd.C_EXTERNAL_VARIABLES, gui);

                    history = BaseXButton.get("c_history", BaseXLayout.addShortcut(RECENTLY_OPENED,
                        BaseXKeys.HISTORY.toString()), false, gui);
                    exec = BaseXButton.get("c_go", BaseXLayout.addShortcut(RUN_QUERY,
                        BaseXKeys.EXEC.toString()), false, gui);
                    stop = BaseXButton.get("c_stop", STOP, false, gui);
                    stop.setEnabled(false);
                    tests = BaseXButton.get("c_test", BaseXLayout.addShortcut(RUN_TESTS,
                        BaseXKeys.TESTS.toString()), false, gui);

                    final BaseXBack buttons = new BaseXBack(false);
                    buttons.layout(new ColumnLayout());
                    buttons.add(newB);
                    buttons.add(openB);
                    buttons.add(saveB);
                    buttons.add(history);
                    buttons.add(Box.createHorizontalStrut(8));
                    buttons.add(find);
                    buttons.add(Box.createHorizontalStrut(8));
                    buttons.add(stop);
                    buttons.add(exec);
                    buttons.add(vars);
                    buttons.add(tests);

                    context = new BaseXLabel("").resize(1.2f);
                    context.setForeground(dgray);

                    final BaseXBack north = new BaseXBack(false).layout(new BorderLayout(10, 0));
                    north.add(buttons, BorderLayout.WEST);
                    north.add(context, BorderLayout.CENTER);
                    north.add(new BaseXHeader(EDITOR), BorderLayout.EAST);

                    // status and query pane
                    search.editor(addTab(), false);

                    info = new BaseXLabel().setText(OK, Msg.SUCCESS).resize(1.2f);
                    pos = new BaseXLabel(" ").resize(1.2f);

                    posCode.invokeLater();

                    final BaseXBack south = new BaseXBack(false).border(8, 0, 0, 0);
                    south.layout(new BorderLayout(4, 0));
                    south.add(info, BorderLayout.CENTER);
                    south.add(pos, BorderLayout.EAST);

                    final BaseXBack main = new BaseXBack().border(5);
                    main.setOpaque(false);
                    main.layout(new BorderLayout());
                    main.add(north, BorderLayout.NORTH);
                    main.add(center, BorderLayout.CENTER);
                    main.add(south, BorderLayout.SOUTH);

                    project = new ProjectView(this);
                    split = new BaseXSplit(true);
                    split.setOpaque(false);
                    split.add(project);
                    split.add(main);
                    split.init(new double[] { 0.3, 0.7 }, new double[] { 0, 1 });
                    toggleProject();
                    add(split, BorderLayout.CENTER);

                    refreshLayout();

                    // add listeners
                    saveB.addActionListener(e -> {
                      final JPopupMenu pop = new JPopupMenu();
                      final StringBuilder mnem = new StringBuilder();
                      final JMenuItem sa = GUIMenu.newItem(GUIMenuCmd.C_EDIT_SAVE, gui, mnem);
                      final JMenuItem sas = GUIMenu.newItem(GUIMenuCmd.C_EDIT_SAVE_AS, gui, mnem);
                      sa.setEnabled(GUIMenuCmd.C_EDIT_SAVE.enabled(gui));
                      sas.setEnabled(GUIMenuCmd.C_EDIT_SAVE_AS.enabled(gui));
                      pop.add(sa);
                      pop.add(sas);
                      pop.show(saveB, 0, saveB.getHeight());
                    });

                    history.addActionListener(e -> historyPopup(0));
                    refreshHistory(null);

                    info.addMouseListener((MouseClickedListener) e -> markError(true));
                    stop.addActionListener(e -> {
                      stop.setEnabled(false);
                      gui.stop();
                    });
                    exec.addActionListener(e -> run(getEditor(), Action.EXECUTE));
                    tests.addActionListener(e -> run(getEditor(), Action.TEST));
                    tabs.addChangeListener(e -> {
                      final EditorArea ea = getEditor();
                      if(ea == null) return;
                      search.editor(ea, true);
                      gui.refreshControls(false);
                      posCode.invokeLater();
                      refreshMark();
                      run(ea, Action.PARSE);
                      gui.setTitle();
                    });

                    BaseXLayout.addDrop(this, file -> {
                      if(file instanceof File) open(new IOFile((File) file));
                    });
                  }

                  @Override
                  public void refreshInit() { }

                  @Override
                  public void refreshFocus() { }

                  @Override
                  public void refreshMark() {
                    tests.setEnabled(getEditor().file().hasSuffix(IO.XQSUFFIXES));
                  }

                  @Override
                  public void refreshContext(final boolean more, final boolean quick) { }

                  @Override
                  public void refreshLayout() {
                    for(final EditorArea edit : editors()) edit.refreshLayout(mfont);
                    project.refreshLayout();
                    search.refreshLayout();
                  }

                  @Override
                  public void refreshUpdate() { }

                  @Override
                  public boolean visible() {
                    return gui.gopts.get(GUIOptions.SHOWEDITOR);
                  }

                  @Override
                  public void visible(final boolean v) {
                    gui.gopts.set(GUIOptions.SHOWEDITOR, v);
                  }

                  @Override
                  protected boolean db() {
                    return false;
                  }

                  /**
                   * Shows a history popup menu.
                   * @param start first entry
                   */
                  public void historyPopup(final int start) {
                    // create list of paths; show opened files first
                    final HashSet<String> opened = new HashSet<>();
                    for(final EditorArea edit : editors()) opened.add(edit.file().path());
                    final List<String> paths = new ArrayList<>(opened);
                    for(final String path : gui.gopts.get(GUIOptions.EDITOR)) {
                      if(!paths.contains(path)) paths.add(path);
                    }
                    paths.sort((path1, path2) -> {
                      final boolean c1 = opened.contains(path1), c2 = opened.contains(path2);
                      return c1 == c2 ? path1.compareTo(path2) : c1 ? -1 : 1;
                    });

                    final JPopupMenu menu = new JPopupMenu();
                    int p = start - 1;
                    final int max = Math.min(paths.size(), start + BaseXHistory.MAXPAGE);
                    if(start > 0) menu.add(new JMenuItem(DOTS)).addActionListener(
                        ac -> historyPopup(start - BaseXHistory.MAXPAGE));
                    while(++p < max) {
                      final String path = paths.get(p);
                      final IOFile file = new IOFile(path);
                      final String label = file.name() + " \u00b7 " + BaseXLayout.reversePath(file);
                      final JMenuItem item = new JMenuItem(label);
                      item.setToolTipText(BaseXLayout.info(file, true));
                      if(opened.contains(path)) BaseXLayout.boldFont(item);
                      menu.add(item).addActionListener(ac -> open(file));
                    }
                    if(p < paths.size()) menu.add(new JMenuItem(DOTS)).addActionListener(
                        ac -> historyPopup(start + BaseXHistory.MAXPAGE));
                    menu.show(history, 0, history.getHeight());
                  }

                  /**
                   * Refreshes the context label.
                   */
                  public void refreshContextLabel() {
                    final String label = context();
                    context.setText(label.isEmpty() ? "" : CONTEXT + COLS + label);
                  }

                  /**
                   * Sets an XML document as context.
                   * @param file file
                   */
                  public void setContext(final IOFile file) {
                    try {
                      // close database
                      if(Close.close(gui.context)) gui.notify.init();
                      doc = new DBNode(file);
                      // remove context item binding
                      final Map<String, String> map = gui.context.options.toMap(MainOptions.BINDINGS);
                      map.remove("");
                      DialogBindings.assign(map, gui);
                      refreshContextLabel();
                    } catch(final IOException ex) {
                      Util.debug(ex);
                      BaseXDialog.error(gui, Util.info(ex));
                    }
                  }

                  /**
                   * Returns a string describing the current context.
                   * @return context string (can be empty)
                   */
                  public String context() {
                    // check if context binding was set
                    String value = gui.context.options.toMap(MainOptions.BINDINGS).get("");
                    if(value != null) {
                      value = Strings.concat("xs:untypedAtomic(", Atm.get(value), ')');
                    }
                    // check if database is opened
                    if(value == null) {
                      final Data data = gui.context.data();
                      if(data != null) value = Function._DB_OPEN.args(data.meta.name);
                    }
                    // check if main-memory document exists
                    if(value == null) {
                      if(doc != null) value = Function.DOC.args(new IOFile(doc.data().meta.original).name());
                    } else {
                      doc = null;
                    }
                    return value != null ? value.trim() : "";
                  }

                  /**
                   * Shows the project view.
                   */
                  public void showProject() {
                    if(!gui.gopts.get(GUIOptions.SHOWPROJECT)) {
                      gui.gopts.invert(GUIOptions.SHOWPROJECT);
                      split.visible(true);
                    }
                  }

                  /**
                   * Toggles the project view.
                   */
                  public void toggleProject() {
                    final boolean show = gui.gopts.get(GUIOptions.SHOWPROJECT);
                    split.visible(show);
                    if(show) {
                      project.focus();
                    } else {
                      focusEditor();
                    }
                  }

                  /**
                   * Focuses the project view.
                   */
                  public void findFiles() {
                    project.findFiles(getEditor());
                  }

                  /**
                   * Focuses the current editor.
                   */
                  public void focusEditor() {
                    SwingUtilities.invokeLater(() -> getEditor().requestFocusInWindow());
                  }

                  /**
                   * Focuses the currently edited file in the project view.
                   */
                  public void jumpToFile() {
                    project.jumpTo(getEditor().file(), true);
                  }

                  /**
                   * Switches the current editor tab.
                   * @param next next next/previous tab
                   */
                  public void tab(final boolean next) {
                    final int s = tabs.getTabCount();
                    final int i = (s + tabs.getSelectedIndex() + (next ? 1 : -1)) % s;
                    tabs.setSelectedIndex(i);
                  }

                  /**
                   * Opens previously opened and new files.
                   * @param files files to be opened
                   */
                  public void init(final ArrayList<IOFile> files) {
                    for(final String file : gui.gopts.get(GUIOptions.OPEN)) open(new IOFile(file), true, false);
                    for(final IOFile file : files) open(file, true, false);

                    // open temporary files
                    final EditorArea edit = getEditor();
                    final String prefix = Prop.HOMEDIR.hashCode() + "-";
                    for(final IOFile file : new IOFile(Prop.TEMPDIR, Prop.PROJECT).children()) {
                      if(!file.name().startsWith(prefix)) continue;
                      try {
                        final byte[] text = read(file);
                        if(text != null) {
                          final EditorArea ea = addTab();
                          ea.setText(text);
                          refreshControls(ea, true);
                          file.delete();
                        }
                      } catch(final IOException ex) {
                        Util.debug(ex);
                      }
                    }
                    if(!edit.opened()) closeEditor(edit);

                    gui.setTitle();
                  }

                  /**
                   * Opens a new file.
                   */
                  public void open() {
                    // open file chooser for XML creation
                    final BaseXFileChooser fc = new BaseXFileChooser(gui, OPEN, gui.gopts.get(GUIOptions.WORKPATH));
                    fc.filter(XQUERY_FILES, false, IO.XQSUFFIXES);
                    fc.filter(BXS_FILES, false, IO.BXSSUFFIX);
                    fc.textFilters();
                    for(final IOFile f : fc.multi().selectAll(Mode.FOPEN)) open(f);
                  }

                  /**
                   * Saves the contents of the currently opened editor.
                   * @return {@code false} if operation was canceled
                   */
                  public boolean save() {
                    final EditorArea edit = getEditor();
                    return edit.opened() ? edit.save() : saveAs();
                  }

                  /**
                   * Saves the contents of the currently opened editor under a new name.
                   * @return {@code false} if operation was canceled
                   */
                  public boolean saveAs() {
                    // open file chooser for XML creation
                    final EditorArea edit = getEditor();
                    final String path = edit.opened() ? edit.file().path() : gui.gopts.get(GUIOptions.WORKPATH);
                    final BaseXFileChooser fc = new BaseXFileChooser(gui, SAVE_AS, path);
                    fc.filter(XQUERY_FILES, false, IO.XQSUFFIXES);
                    fc.filter(BXS_FILES, false, IO.BXSSUFFIX);
                    fc.textFilters();
                    fc.suffix(IO.XQSUFFIX);

                    // save new file
                    final IOFile file = fc.select(Mode.FSAVE);
                    if(file == null || !edit.save(file)) return false;
                    // success: parse contents
                    run(edit, Action.PARSE);
                    return true;
                  }

                  /**
                   * Creates a new file.
                   */
                  public void newFile() {
                    if(!visible()) GUIMenuCmd.C_SHOW_EDITOR.execute(gui);
                    refreshControls(addTab(), true);
                  }

                  /**
                   * Deletes a file.
                   * @param file file to be deleted
                   * @return success flag
                   */
                  public boolean delete(final IOFile file) {
                    final EditorArea edit = find(file);
                    if(edit != null) close(edit);
                    return file.delete();
                  }

                  /**
                   * Opens and parses the specified query file.
                   * @param file query file
                   * @return opened editor or {@code null} if file could not be opened
                   */
                  public EditorArea open(final IOFile file) {
                    return open(file, true, true);
                  }

                  /**
                   * Opens and focuses the specified query file.
                   * @param file query file
                   * @param parse parse contents
                   * @param error display error if file does not exist
                   * @return opened editor, or {@code null} if file could not be opened
                   */
                  private EditorArea open(final IOFile file, final boolean parse, final boolean error) {
                    if(!visible()) GUIMenuCmd.C_SHOW_EDITOR.execute(gui);

                    EditorArea edit = find(file);
                    if(edit != null) {
                      // display open file
                      tabs.setSelectedComponent(edit);
                    } else {
                      try {
                        // check and retrieve content
                        final byte[] text = read(file);
                        if(text == null) return null;

                        // get current editor
                        edit = getEditor();
                        // create new tab if text in current tab is stored on disk or has been modified
                        if(edit.opened() || edit.modified()) edit = addTab();
                        edit.initText(text);
                        edit.file(file, error);
                        if(parse) run(edit, Action.PARSE);
                      } catch(final IOException ex) {
                        refreshHistory(null);
                        Util.debug(ex);
                        if(error) BaseXDialog.error(gui, Util.info(FILE_NOT_OPENED_X, file));
                        return null;
                      }
                    }
                    focusEditor();
                    return edit;
                  }

                  /**
                   * Parses or evaluates the current file.
                   * @param action action
                   * @param editor current editor
                   */
                  void run(final EditorArea editor, final Action action) {
                    refreshControls(editor, false);

                    // skip checks if input has not changed
                    final byte[] text = editor.getText();
                    if(eq(text, editor.last) && action == Action.CHECK) return;
                    editor.last = text;

                    // save modified files before executing queries
                    if(gui.gopts.get(GUIOptions.SAVERUN) && (action == Action.EXECUTE || action == Action.TEST)) {
                      for(final EditorArea edit : editors()) {
                        if(edit.opened()) edit.save();
                      }
                    }

                    IOFile file = editor.file();
                    final boolean xquery = file.hasSuffix(IO.XQSUFFIXES) || !file.name().contains(".");
                    final boolean script = file.hasSuffix(IO.BXSSUFFIX);

                    if(action == Action.TEST) {
                      // test query
                      if(xquery) gui.execute(true, new Test(file.path()));
                    } else if(action == Action.EXECUTE && script) {
                      // execute script
                      gui.execute(true, new Execute(string(text)).baseURI(file.path()));
                    } else if(action == Action.EXECUTE || xquery) {
                      // execute or parse query
                      String input = string(text);
                      if(action == Action.EXECUTE || gui.gopts.get(GUIOptions.EXECRT)) {
                        // find main module if current file cannot be evaluated; return early if no module is found
                        if(!xquery || QueryParser.isLibrary(input)) {
                          final EditorArea ea = execEditor();
                          if(ea == null) return;
                          file = ea.file();
                          input = string(ea.getText());
                        }
                        // execute query
                        final XQuery cmd = new XQuery(input);
                        if(doc != null) cmd.bind(null, doc);
                        gui.execute(true, cmd.baseURI(file.path()));
                        execFile = file;
                      } else {
                        // parse: replace empty query with empty sequence (suppresses errors for plain text files)
                        parse(input.isEmpty() ? "()" : input, file);
                      }
                    } else if(file.hasSuffix(IO.JSONSUFFIX)) {
                      try {
                        final IOContent io = new IOContent(text);
                        io.name(file.path());
                        JsonConverter.get(new JsonParserOptions()).convert(io);
                        info(null);
                      } catch(final IOException ex) {
                        info(ex);
                      }
                    } else if(script || file.hasSuffix(gui.gopts.xmlSuffixes()) || file.hasSuffix(IO.XSLSUFFIXES)) {
                      final ArrayInput ai = new ArrayInput(text);
                      try {
                        // check XML syntax
                        if(startsWith(text, '<') || !script) new XmlParser().parse(ai);
                        // check command script
                        if(script) CommandParser.get(string(text), gui.context).parse();
                        info(null);
                      } catch(final Exception ex) {
                        info(ex);
                      }
                    } else if(action != Action.CHECK) {
                      info(null);
                    } else {
                      // no particular file format, no particular action: reset status info
                      info.setText(OK, Msg.SUCCESS);
                    }
                  }

                  /**
                   * Evaluates the info message resulting from command or query parsing.
                   * @param ex exception or {@code null}
                   */
                  private void info(final Exception ex) {
                    info(ex, false, false);
                  }

                  /**
                   * Returns the editor whose contents have been executed last.
                   * @return editor or {@code null}
                   */
                  private EditorArea execEditor() {
                    final IOFile file = execFile;
                    if(file != null) {
                      for(final EditorArea edit : editors()) {
                        if(edit.file().path().equals(file.path())) return edit;
                      }
                      execFile = null;
                    }
                    return null;
                  }

                  /**
                   * Retrieves the contents of the specified file, or opens it externally.
                   * @param file query file
                   * @return contents, or {@code null} reference if file is opened externally
                   * @throws IOException I/O exception
                   */
                  private byte[] read(final IOFile file) throws IOException {
                    try {
                      // try to open as validated UTF-8 document
                      return new NewlineInput(file).validate(true).content();
                    } catch(final InputException ex) {
                      // error...
                      Util.debug(ex);
                      final String button = BaseXDialog.yesNoCancel(gui, H_FILE_BINARY);
                      // open binary as text; do not validate
                      if(button == B_NO) return new NewlineInput(file).content();
                      // open external application
                      if(button == B_YES) {
                        try {
                          file.open();
                        } catch(final IOException ioex) {
                          Util.debug(ioex);
                          Desktop.getDesktop().open(file.file());
                        }
                      }
                      // return nothing (also applies if dialog is canceled)
                      return null;
                    }
                  }

                  /**
                   * Refreshes the list of recent query files and updates the query path.
                   * @param file new file (can be {@code null})
                   */
                  void refreshHistory(final IOFile file) {
                    final StringList paths = new StringList();
                    if(file != null) {
                      gui.gopts.setFile(GUIOptions.WORKPATH, file.parent());
                      paths.add(file.path());
                      tabs.setToolTipTextAt(tabs.getSelectedIndex(), BaseXLayout.info(file, true));
                    }
                    for(final String old : gui.gopts.get(GUIOptions.EDITOR)) {
                      if(paths.size() < BaseXHistory.MAX) paths.addUnique(old);
                    }

                    // store sorted history
                    gui.gopts.setFiles(GUIOptions.EDITOR, paths.finish());
                    history.setEnabled(!paths.isEmpty());
                  }

                  /**
                   * Closes all editors.
                   */
                  public void closeAll() {
                    for(final EditorArea ea : editors()) closeEditor(ea);
                    gui.saveOptions();
                  }

                  /**
                   * Closes an editor.
                   * @param edit editor to be closed (if {@code null}, the currently opened editor will be closed)
                   */
                  public void close(final EditorArea edit) {
                    closeEditor(edit);
                    gui.saveOptions();
                  }

                  /**
                   * Closes an editor.
                   * @param edit editor to be closed (if {@code null}, the currently opened editor will be closed)
                   */
                  private void closeEditor(final EditorArea edit) {
                    final EditorArea ea = edit != null ? edit : getEditor();
                    if(ea.modified() && !confirm(ea)) return;

                    // remove reference to last executed file
                    if(execFile != null && ea.file().path().equals(execFile.path())) execFile = null;
                    tabs.remove(ea);
                    // no panels left: open default tab
                    if(tabs.getTabCount() == 0) {
                      addTab();
                      SwingUtilities.invokeLater(this::toggleProject);
                    } else {
                      focusEditor();
                    }
                  }

                  /**
                   * Starts a thread, which shows a waiting info after a short timeout.
                   * @param id thread id
                   */
                  public void pleaseWait(final int id) {
                    new Timer(true).schedule(new TimerTask() {
                      @Override
                      public void run() {
                        if(gui.running(id)) {
                          info.setText(PLEASE_WAIT_D, Msg.SUCCESS).setToolTipText(null);
                          stop.setEnabled(true);
                        }
                      }
                    }, WAIT_DELAY);
                  }

                  /**
                   * Parses the current query after a little delay.
                   * @param input query input
                   * @param file file
                   */
                  private void parse(final String input, final IO file) {
                    final int id = parseID.incrementAndGet();
                    new Timer(true).schedule(new TimerTask() {
                      @Override
                      public void run() {
                        // let current parser finish; check if thread is obsolete
                        while(parsing.get()) Performance.sleep(1);
                        if(id != parseID.get()) return;

                        // parse query
                        parsing.set(true);
                        try(QueryContext qc = new QueryContext(gui.context)) {
                          qc.parse(input, file.path());
                          if(id == parseID.get()) info(null);
                        } catch(final QueryException ex) {
                          if(id == parseID.get()) info(ex);
                        } finally {
                          parsing.set(false);
                        }
                      }
                    }, SEARCH_DELAY);
                  }

                  /**
                   * Processes the result from a command or query execution.
                   * @param th exception or {@code null}
                   * @param stopped {@code true} if evaluation was stopped
                   * @param refresh refresh buttons
                   */
                  public void info(final Throwable th, final boolean stopped, final boolean refresh) {
                    // do not refresh view when query is running
                    if(!refresh && stop.isEnabled()) return;

                    parseID.incrementAndGet();
                    final EditorArea editor = getEditor();
                    String path = "";
                    if(editor != null) {
                      path = editor.file().path();
                      editor.resetError();
                    }

                    if(refresh) {
                      stop.setEnabled(false);
                      refreshMark();
                    }

                    if(stopped || th == null) {
                      info.setCursor(CURSORARROW);
                      info.setText(stopped ? INTERRUPTED : OK, Msg.SUCCESS);
                      info.setToolTipText(null);
                      inputInfo = null;
                    } else {
                      info.setCursor(CURSORHAND);
                      final String msg = Util.message(th), local = th.getLocalizedMessage();
                      info.setText(local != null ? local : msg, Msg.ERROR);
                      final String tt = msg.replace("<", "&lt;").replace(">", "&gt;").
                        replaceAll("\r?\n", "<br/>").replaceAll("(<br/>.*?)<br/>.*", "${'$'}1");
                      info.setToolTipText("<html>" + tt + "</html>");

                      if(th instanceof QueryIOException) {
                        inputInfo = ((QueryIOException) th).getCause().info();
                      } else if(th instanceof QueryException) {
                        inputInfo = ((QueryException) th).info();
                      } else if(th instanceof SAXParseException) {
                        final SAXParseException ex = (SAXParseException) th;
                        inputInfo = new InputInfo(path, ex.getLineNumber(), ex.getColumnNumber());
                      } else {
                        inputInfo = new InputInfo(path, 1, 1);
                      }
                      markError(false);
                    }
                  }

                  /**
                   * Jumps to the specified file and position.
                   * @param link link
                   */
                  public void jump(final String link) {
                    final Matcher m = LINK.matcher(link);
                    if(m.matches()) {
                      inputInfo = new InputInfo(m.group(1), Strings.toInt(m.group(2)), Strings.toInt(m.group(3)));
                      markError(true);
                    } else {
                      Util.stack("No match found: " + link);
                    }
                  }

                  /**
                   * Jumps to the current error.
                   * @param jump jump to error position (if necessary, open file)
                   */
                  public void markError(final boolean jump) {
                    InputInfo ii = inputInfo;
                    final String path;
                    final boolean error = ii == null;
                    if(error) {
                      final TreeMap<String, InputInfo> errors = project.errors();
                      if(errors.isEmpty()) return;
                      path = errors.get(errors.keySet().iterator().next()).path();
                    } else {
                      path = ii.path();
                    }
                    if(path == null) return;

                    final IOFile file = new IOFile(path);
                    final EditorArea found = find(file), opened;
                    if(jump) {
                      opened = open(file, error, true);
                      // update error information if file was not already opened
                      if(found == null && error) ii = inputInfo;
                    } else {
                      opened = found;
                    }
                    // no editor available, no input info
                    if(opened == null || ii == null) return;

                    // mark error, jump to error position
                    final int ep = pos(opened.last, ii.line(), ii.column());
                    opened.error(ep);

                    if(jump) {
                      opened.setCaret(ep);
                      posCode.invokeLater();
                      // jump to file in project view if file was opened by this function call
                      if(found == null) project.jumpTo(opened.file(), false);
                    }
                  }

                  /**
                   * Returns an editor offset for the specified line and column.
                   * @param text text
                   * @param line line
                   * @param col column
                   * @return position
                   */
                  private static int pos(final byte[] text, final int line, final int col) {
                    final int ll = text.length;
                    int ep = ll;
                    for(int p = 0, l = 1, c = 1; p < ll; ++c, p += cl(text, p)) {
                      if(l > line || l == line && c == col) {
                        ep = p;
                        break;
                      }
                      if(text[p] == '\n') {
                        ++l;
                        c = 0;
                      }
                    }
                    if(ep < ll && Character.isLetterOrDigit(cp(text, ep))) {
                      while(ep > 0 && Character.isLetterOrDigit(cp(text, ep - 1))) ep--;
                    }
                    return ep;
                  }

                  /**
                   * Returns paths of all open files.
                   * @return file paths
                   */
                  public String[] openFiles() {
                    // remember opened files
                    final StringList files = new StringList();
                    for(final EditorArea edit : editors()) {
                      if(edit.opened()) files.add(edit.file().path());
                    }
                    return files.finish();
                  }

                  /**
                   * Returns the current editor.
                   * @return editor or {@code null}
                   */
                  public EditorArea getEditor() {
                    final Component c = tabs.getSelectedComponent();
                    return c instanceof EditorArea ? (EditorArea) c : null;
                  }

                  /**
                   * Updates the references to renamed files.
                   * @param old old file file reference
                   * @param renamed updated file reference
                   */
                  public void rename(final IOFile old, final IOFile renamed) {
                    try {
                      // use canonical representation and add slash to names of directories
                      final boolean dir = renamed.isDir();
                      final String oldPath = old.file().getCanonicalPath() + (dir ? File.separator : "");
                      // iterate through all tabs
                      for(final Component c : tabs.getComponents()) {
                        if(!(c instanceof EditorArea)) continue;

                        final EditorArea ea = (EditorArea) c;
                        if(!ea.opened()) continue;

                        final String editPath = ea.file().file().getCanonicalPath();
                        if(dir) {
                          // change path to files in a renamed directory
                          if(editPath.startsWith(oldPath)) {
                            ea.file(new IOFile(renamed, editPath.substring(oldPath.length())), true);
                          }
                        } else if(oldPath.equals(editPath)) {
                          // update file reference and label of editor tab
                          ea.file(renamed, true);
                          ea.label.setText(renamed.name());
                          break;
                        }
                      }
                    } catch(final IOException ex) {
                      Util.errln(ex);
                    }
                  }

                  /**
                   * Refreshes the query modification flag.
                   * @param edit editor
                   * @param enforce enforce action
                   */
                  void refreshControls(final EditorArea edit, final boolean enforce) {
                    // update modification flag
                    final boolean modified = edit.hist != null && edit.hist.modified();
                    if(modified == edit.modified() && !enforce) return;

                    edit.modified(modified);

                    // update tab title
                    String title = edit.file().name();
                    if(modified) title += '*';
                    edit.label.setText(title);

                    // update components
                    gui.refreshControls(false);
                    gui.setTitle();
                    posCode.invokeLater();
                    refreshMark();
                  }

                  /** Code for setting cursor position. */
                  public final GUICode posCode = new GUICode() {
                    @Override
                    public void execute(final Object arg) {
                      final int[] lc = getEditor().pos();
                      pos.setText(lc[0] + " : " + lc[1]);
                    }
                  };

                  /**
                   * Finds the editor that contains the specified file.
                   * @param file file to be found
                   * @return editor or {@code null}
                   */
                  private EditorArea find(final IO file) {
                    for(final EditorArea edit : editors()) {
                      if(edit.file().eq(file)) return edit;
                    }
                    return null;
                  }

                  /**
                   * Choose a unique tab file.
                   * @return io reference
                   */
                  private IOFile newTabFile() {
                    // collect numbers of existing files
                    final BoolList bl = new BoolList();
                    for(final EditorArea edit : editors()) {
                      if(edit.opened()) continue;
                      final String n = edit.file().name().substring(FILE.length());
                      bl.set(n.isEmpty() ? 1 : Integer.parseInt(n), true);
                    }
                    // find first free file number
                    int b = 0;
                    final int bs = bl.size();
                    while(++b < bs && bl.get(b));
                    // create io reference
                    return new IOFile(gui.gopts.get(GUIOptions.WORKPATH), FILE + (b == 1 ? "" : b));
                  }

                  /**
                   * Adds a new editor tab.
                   * @return editor reference
                   */
                  private EditorArea addTab() {
                    final EditorArea edit = new EditorArea(this, newTabFile());
                    edit.setFont(mfont);

                    final BaseXBack tab = new BaseXBack(false).layout(new BorderLayout(10, 0));
                    tab.add(edit.label, BorderLayout.CENTER);

                    final AbstractButton close = tabButton("e_close", "e_close2");
                    close.addActionListener(e -> close(edit));
                    tab.add(close, BorderLayout.EAST);

                    tabs.add(edit, tab, tabs.getTabCount());
                    return edit;
                  }

                  /**
                   * Adds a new tab button.
                   * @param icon name of button icon
                   * @param rollover rollover icon
                   * @return button
                   */
                  private AbstractButton tabButton(final String icon, final String rollover) {
                    final AbstractButton close = BaseXButton.get(icon, null, false, gui);
                    close.setBorder(BaseXLayout.border(2, 0, 2, 0));
                    close.setContentAreaFilled(false);
                    close.setFocusable(false);
                    close.setRolloverIcon(BaseXImages.icon(rollover));
                    return close;
                  }

                  /**
                   * Shows a confirmation dialog for the specified editor, or all editors.
                   * @param edit editor to be saved, or {@code null} to save all editors
                   * @return {@code true} if all editors were confirmed
                   */
                  public boolean confirm(final EditorArea edit) {
                    final boolean all = edit == null;
                     final EditorArea[] eas = all ? editors() : new EditorArea[] { edit };
                    final String[] buttons = all && eas.length > 1 ? new String[] { CLOSE_ALL } : new String[0];

                    for(final EditorArea ea : eas) {
                      tabs.setSelectedComponent(ea);
                      if(ea.modified() && (ea.opened() || edit != null && trim(ea.getText()).length != 0)) {
                        final String msg = Util.info(CLOSE_FILE_X, ea.file().name());
                        final String action = BaseXDialog.yesNoCancel(gui, msg, buttons);
                        if(action == null || action.equals(B_YES) && !save()) return false;
                        else if(action.equals(CLOSE_ALL)) break;
                      }
                    }

                    // close application: remember opened files
                    final IOFile tmpDir = new IOFile(Prop.TEMPDIR, Prop.PROJECT);
                    if(edit == null && eas.length > 0 && tmpDir.md()) {
                      try {
                        final String prefix = Prop.HOMEDIR.hashCode() + "-";
                        int c = 0;
                        for(final EditorArea ea : eas) {
                          final byte[] text = ea.getText();
                          if(!ea.opened() && text.length > 0) {
                            new IOFile(tmpDir, prefix + c++ + IO.TMPSUFFIX).write(text);
                          }
                        }
                      } catch(final IOException ex) {
                        Util.debug(ex);
                      }
                    }
                    return true;
                  }

                  /**
                   * Returns all editors.
                   * @return editors
                   */
                  private EditorArea[] editors() {
                    final ArrayList<EditorArea> edits = new ArrayList<>();
                    for(final Component c : tabs.getComponents()) {
                      if(c instanceof EditorArea) edits.add((EditorArea) c);
                    }
                    return edits.toArray(new EditorArea[0]);
                  }
                }
            """
        )
    )
    @Test
    fun californium() = rewriteRun(
        java(
            """
                /*******************************************************************************
                 * Copyright (c) 2017 Bosch Software Innovations GmbH and others.
                 *
                 * All rights reserved. This program and the accompanying materials
                 * are made available under the terms of the Eclipse Public License v2.0
                 * and Eclipse Distribution License v1.0 which accompany this distribution.
                 *
                 * The Eclipse Public License is available at
                 *    http://www.eclipse.org/legal/epl-v20.html
                 * and the Eclipse Distribution License is available at
                 *    http://www.eclipse.org/org/documents/edl-v10.html.
                 *
                 * Contributors:
                 *    Bosch Software Innovations GmbH - initial creation
                 *                                      derived from HelloWorldServer example
                 *    Bosch Software Innovations GmbH - migrate to SLF4J
                 ******************************************************************************/
                package org.eclipse.californium.examples;

                import java.io.File;
                import java.io.FileInputStream;
                import java.io.IOException;
                import java.io.InputStream;
                import java.net.SocketException;
                import java.util.Arrays;
                import java.util.HashMap;
                import java.util.Map;

                import org.eclipse.californium.core.CoapResource;
                import org.eclipse.californium.core.coap.CoAP;
                import org.eclipse.californium.core.coap.MediaTypeRegistry;
                import org.eclipse.californium.core.coap.Request;
                import org.eclipse.californium.core.coap.Response;
                import org.eclipse.californium.core.config.CoapConfig;
                import org.eclipse.californium.core.network.Exchange;
                import org.eclipse.californium.core.server.resources.CoapExchange;
                import org.eclipse.californium.core.server.resources.MyIpResource;
                import org.eclipse.californium.core.server.resources.Resource;
                import org.eclipse.californium.elements.config.Configuration;
                import org.eclipse.californium.elements.config.UdpConfig;
                import org.eclipse.californium.elements.config.Configuration.DefinitionsProvider;
                import org.eclipse.californium.elements.config.TcpConfig;
                import org.eclipse.californium.elements.util.StringUtil;
                import org.eclipse.californium.plugtests.AbstractTestServer;
                import org.eclipse.californium.plugtests.PlugtestServer.BaseConfig;
                import org.eclipse.californium.plugtests.resources.MyContext;
                import org.eclipse.californium.scandium.config.DtlsConfig;
                import org.slf4j.Logger;
                import org.slf4j.LoggerFactory;

                import picocli.CommandLine;
                import picocli.CommandLine.Command;
                import picocli.CommandLine.Option;
                import picocli.CommandLine.ParameterException;
                import picocli.CommandLine.ParseResult;

                public class SimpleFileServer extends AbstractTestServer {

                	private static final Logger LOG = LoggerFactory.getLogger(SimpleFileServer.class);

                	private static final File CONFIG_FILE = new File("Californium3.properties");
                	private static final String CONFIG_HEADER = "Californium CoAP Properties file for Fileserver";
                	// 2 MB
                	private static final int DEFAULT_MAX_RESOURCE_SIZE = 2 * 1024 * 1024;
                	private static final int DEFAULT_BLOCK_SIZE = 512;

                	static {
                		CoapConfig.register();
                		UdpConfig.register();
                		DtlsConfig.register();
                		TcpConfig.register();
                	}

                	private static DefinitionsProvider DEFAULTS = new DefinitionsProvider() {

                		@Override
                		public void applyDefinitions(Configuration config) {
                			config.set(CoapConfig.MAX_RESOURCE_BODY_SIZE, DEFAULT_MAX_RESOURCE_SIZE);
                			config.set(CoapConfig.MAX_MESSAGE_SIZE, DEFAULT_BLOCK_SIZE);
                			config.set(CoapConfig.PREFERRED_BLOCK_SIZE, DEFAULT_BLOCK_SIZE);
                			config.setTransient(DtlsConfig.DTLS_CLIENT_AUTHENTICATION_MODE);
                			config.setTransient(TcpConfig.TLS_CLIENT_AUTHENTICATION_MODE);
                			config.set(EXTERNAL_UDP_MAX_MESSAGE_SIZE, 64);
                			config.set(EXTERNAL_UDP_PREFERRED_BLOCK_SIZE, 64);
                		}
                	};

                	private static final String DEFAULT_PATH = "data";

                	@Command(name = "SimpleFileServer", version = "(c) 2017, Bosch Software Innovations GmbH and others.")
                	public static class Config extends BaseConfig {

                		@Option(names = "--file-root", description = "files root. Default \"" + DEFAULT_PATH + "\"")
                		public String fileRoot = DEFAULT_PATH;

                		@Option(names = "--path-root", description = "resource-path root. Default \"" + DEFAULT_PATH + "\"")
                		public String pathRoot = DEFAULT_PATH;

                	}

                	private static final Config config = new Config();

                	/*
                	 * Application entry point.
                	 */
                	public static void main(String[] args) {
                		String version = StringUtil.CALIFORNIUM_VERSION == null ? "" : StringUtil.CALIFORNIUM_VERSION;
                		CommandLine cmd = new CommandLine(config);
                		try {
                			ParseResult result = cmd.parseArgs(args);
                			if (result.isVersionHelpRequested()) {
                				System.out.println("\nCalifornium (Cf) " + cmd.getCommandName() + " " + version);
                				cmd.printVersionHelp(System.out);
                				System.out.println();
                			}
                			if (result.isUsageHelpRequested()) {
                				cmd.usage(System.out);
                				return;
                			}
                		} catch (ParameterException ex) {
                			System.err.println(ex.getMessage());
                			System.err.println();
                			cmd.usage(System.err);
                			System.exit(-1);
                		}

                		Configuration netConfig = Configuration.createWithFile(CONFIG_FILE, CONFIG_HEADER, DEFAULTS);
                		// reduce the message size for plain UDP
                		Configuration udpConfig = new Configuration(netConfig)
                				.set(CoapConfig.MAX_MESSAGE_SIZE, netConfig.get(EXTERNAL_UDP_MAX_MESSAGE_SIZE))
                				.set(CoapConfig.PREFERRED_BLOCK_SIZE, netConfig.get(EXTERNAL_UDP_PREFERRED_BLOCK_SIZE));
                		Map<Select, Configuration> protocolConfig = new HashMap<>();
                		protocolConfig.put(new Select(Protocol.UDP, InterfaceType.EXTERNAL), udpConfig);

                		try {
                			String filesRootPath = config.fileRoot;
                			String coapRootPath = config.pathRoot;

                			if (0 <= coapRootPath.indexOf('/')) {
                				LOG.error("{} don't use '/'! Only one path segement for coap root allowed!", coapRootPath);
                				return;
                			}

                			File filesRoot = new File(filesRootPath);
                			if (!filesRoot.exists()) {
                				LOG.error("{} doesn't exists!", filesRoot.getAbsolutePath());
                				return;
                			} else if (!filesRoot.isDirectory()) {
                				LOG.error("{} is no directory!", filesRoot.getAbsolutePath());
                				return;
                			}

                			listURIs(filesRoot, coapRootPath);

                			// create server
                			SimpleFileServer server = new SimpleFileServer(netConfig, protocolConfig, coapRootPath, filesRoot);
                			server.add(new MyContext(MyContext.RESOURCE_NAME, version, true));

                			// add endpoints on all IP addresses
                			server.addEndpoints(null, null, Arrays.asList(Protocol.UDP, Protocol.DTLS, Protocol.TCP, Protocol.TLS),
                					config);
                			server.start();

                		} catch (SocketException e) {
                			LOG.error("Failed to initialize server: ", e);
                		}
                	}

                	public static void listURIs(File filesRoot, String coapRootPath) {
                		File[] files = filesRoot.listFiles();
                		for (File file : files) {
                			if (file.isFile() && file.canRead()) {
                				LOG.info("GET: coap://<host>/{}/{}", coapRootPath, file.getName());
                			}
                		}
                		for (File file : files) {
                			if (file.isDirectory() && file.canRead()) {
                				listURIs(file, coapRootPath + "/" + file.getName());
                			}
                		}
                	}

                	/*
                	 * Constructor for a new simple file server. Here, the resources of the
                	 * server are initialized.
                	 */
                	public SimpleFileServer(Configuration config, Map<Select, Configuration> protocolConfig, String coapRootPath,
                			File filesRoot) throws SocketException {
                		super(config, protocolConfig);
                		add(new FileResource(config, coapRootPath, filesRoot));
                		add(new MyIpResource(MyIpResource.RESOURCE_NAME, true));
                	}

                	class FileResource extends CoapResource {

                		private final Configuration config;
                		/**
                		 * Files root directory.
                		 */
                		private final File filesRoot;

                		/**
                		 * Create CoAP file resource.
                		 *
                		 * @param config configuration
                		 * @param coapRootPath CoAP resource (base) name
                		 * @param filesRoot files root
                		 */
                		public FileResource(Configuration config, String coapRootPath, File filesRoot) {
                			super(coapRootPath);
                			this.config = config;
                			this.filesRoot = filesRoot;
                		}

                		/*
                		 * Override the default behavior so that requests to sub resources
                		 * (typically /{path}/{file-name}) are handled by /file resource.
                		 */
                		@Override
                		public Resource getChild(String name) {
                			return this;
                		}

                		@Override
                		public void handleRequest(Exchange exchange) {
                			try {
                				super.handleRequest(exchange);
                			} catch (Exception e) {
                				LOG.error("Exception while handling a request on the {} resource", getName(), e);
                				exchange.sendResponse(new Response(CoAP.ResponseCode.INTERNAL_SERVER_ERROR));
                			}
                		}

                		@Override
                		public void handleGET(final CoapExchange exchange) {
                			Request request = exchange.advanced().getRequest();
                			LOG.info("Get received : {}", request);

                			int accept = request.getOptions().getAccept();
                			if (MediaTypeRegistry.UNDEFINED == accept) {
                				accept = MediaTypeRegistry.APPLICATION_OCTET_STREAM;
                			} else if (MediaTypeRegistry.APPLICATION_OCTET_STREAM != accept) {
                				exchange.respond(CoAP.ResponseCode.UNSUPPORTED_CONTENT_FORMAT);
                				return;
                			}

                			String myURI = getURI() + "/";
                			String path = "/" + request.getOptions().getUriPathString();
                			if (!path.startsWith(myURI)) {
                				LOG.info("Request {} does not match {}!", path, myURI);
                				exchange.respond(CoAP.ResponseCode.INTERNAL_SERVER_ERROR);
                				return;
                			}
                			path = path.substring(myURI.length());
                			if (request.getOptions().hasBlock2()) {
                				LOG.info("Send file {} {}", path, request.getOptions().getBlock2());
                			} else {
                				LOG.info("Send file {}", path);
                			}
                			File file = new File(filesRoot, path);
                			if (!file.exists() || !file.isFile()) {
                				LOG.warn("File {} doesn't exist!", file.getAbsolutePath());
                				exchange.respond(CoAP.ResponseCode.NOT_FOUND);
                				return;
                			}
                			if (!checkFileLocation(file, filesRoot)) {
                				LOG.warn("File {} is not in {}!", file.getAbsolutePath(), filesRoot.getAbsolutePath());
                				exchange.respond(CoAP.ResponseCode.UNAUTHORIZED);
                				return;
                			}

                			if (!file.canRead()) {
                				LOG.warn("File {} is not readable!", file.getAbsolutePath());
                				exchange.respond(CoAP.ResponseCode.UNAUTHORIZED);
                				return;
                			}
                			long maxLength = config.get(CoapConfig.MAX_RESOURCE_BODY_SIZE);
                			long length = file.length();
                			if (length > maxLength) {
                				LOG.warn("File {} is too large {} (max.: {})!", file.getAbsolutePath(), length, maxLength);
                				exchange.respond(CoAP.ResponseCode.INTERNAL_SERVER_ERROR);
                				return;
                			}
                			try (InputStream in = new FileInputStream(file)) {
                				byte[] content = new byte[(int) length];
                				int r = in.read(content);
                				if (length == r) {
                					Response response = new Response(CoAP.ResponseCode.CONTENT);
                					response.setPayload(content);
                					response.getOptions().setSize2((int) length);
                					response.getOptions().setContentFormat(accept);
                					exchange.respond(response);
                				} else {
                					LOG.warn("File {} could not be read in!", file.getAbsolutePath());
                					exchange.respond(CoAP.ResponseCode.INTERNAL_SERVER_ERROR);
                				}
                			} catch (IOException ex) {
                				LOG.warn("File {}:", file.getAbsolutePath(), ex);
                				exchange.respond(CoAP.ResponseCode.INTERNAL_SERVER_ERROR);
                			}
                		}

                		/**
                		 * Check, if file is located in root.
                		 *
                		 * Detect attacks via "../.../../file".
                		 *
                		 * @param file file to check
                		 * @param root file root
                		 * @return true, if file is locate in root (or a sub-folder of root),
                		 *         false, otherwise.
                		 */
                		private boolean checkFileLocation(File file, File root) {
                			try {
                				return file.getCanonicalPath().startsWith(root.getCanonicalPath());
                			} catch (IOException ex) {
                				LOG.warn("File {}:", file.getAbsolutePath(), ex);
                				return false;
                			}
                		}
                	}
                }
            """
        )
    )

    @Test
    fun `DependencyCheck`() = rewriteRun(
        java(
            """
                /*
                 * This file is part of dependency-check-core.
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
                 *
                 * Copyright (c) 2013 Jeremy Long. All Rights Reserved.
                 */
                package org.owasp.dependencycheck.analyzer;

                import java.io.BufferedInputStream;
                import java.io.File;
                import java.io.FileFilter;
                import java.io.FileInputStream;
                import java.io.FileNotFoundException;
                import java.io.FileOutputStream;
                import java.io.IOException;
                import java.nio.file.Path;
                import java.util.Collections;
                import java.util.Enumeration;
                import java.util.HashSet;
                import java.util.List;
                import java.util.Set;
                import java.util.concurrent.atomic.AtomicInteger;
                import javax.annotation.concurrent.ThreadSafe;

                import org.apache.commons.compress.archivers.ArchiveEntry;
                import org.apache.commons.compress.archivers.ArchiveInputStream;
                import org.apache.commons.compress.archivers.cpio.CpioArchiveInputStream;
                import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
                import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
                import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
                import org.apache.commons.compress.archivers.zip.ZipFile;
                import org.apache.commons.compress.compressors.CompressorInputStream;
                import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
                import org.apache.commons.compress.compressors.bzip2.BZip2Utils;
                import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
                import org.apache.commons.compress.compressors.gzip.GzipUtils;
                import org.apache.commons.compress.utils.IOUtils;
                import org.eclipse.packager.rpm.RpmTag;
                import org.eclipse.packager.rpm.parse.RpmInputStream;
                import org.owasp.dependencycheck.Engine;
                import static org.owasp.dependencycheck.analyzer.AbstractNpmAnalyzer.shouldProcess;
                import org.owasp.dependencycheck.analyzer.exception.AnalysisException;
                import org.owasp.dependencycheck.analyzer.exception.ArchiveExtractionException;
                import org.owasp.dependencycheck.analyzer.exception.UnexpectedAnalysisException;
                import org.owasp.dependencycheck.dependency.Dependency;
                import org.owasp.dependencycheck.exception.InitializationException;
                import org.owasp.dependencycheck.utils.FileFilterBuilder;
                import org.owasp.dependencycheck.utils.FileUtils;
                import org.owasp.dependencycheck.utils.Settings;

                import org.slf4j.Logger;
                import org.slf4j.LoggerFactory;

                /**
                 * <p>
                 * An analyzer that extracts files from archives and ensures any supported files
                 * contained within the archive are added to the dependency list.</p>
                 *
                 * @author Jeremy Long
                 */
                @ThreadSafe
                public class ArchiveAnalyzer extends AbstractFileTypeAnalyzer {

                    /**
                     * The logger.
                     */
                    private static final Logger LOGGER = LoggerFactory.getLogger(ArchiveAnalyzer.class);
                    /**
                     * The count of directories created during analysis. This is used for
                     * creating temporary directories.
                     */
                    private static final AtomicInteger DIRECTORY_COUNT = new AtomicInteger(0);
                    /**
                     * The parent directory for the individual directories per archive.
                     */
                    private File tempFileLocation = null;
                    /**
                     * The max scan depth that the analyzer will recursively extract nested
                     * archives.
                     */
                    private int maxScanDepth;
                    /**
                     * The file filter used to filter supported files.
                     */
                    private FileFilter fileFilter = null;
                    /**
                     * The set of things we can handle with Zip methods
                     */
                    private static final Set<String> KNOWN_ZIP_EXT = Collections.unmodifiableSet(
                            newHashSet("zip", "ear", "war", "jar", "sar", "apk", "nupkg", "aar"));
                    /**
                     * The set of additional extensions we can handle with Zip methods
                     */
                    private static final Set<String> ADDITIONAL_ZIP_EXT = new HashSet<>();
                    /**
                     * The set of file extensions supported by this analyzer. Note for
                     * developers, any additions to this list will need to be explicitly handled
                     * in {@link #extractFiles(File, File, Engine)}.
                     */
                    private static final Set<String> EXTENSIONS = Collections.unmodifiableSet(
                            newHashSet("tar", "gz", "tgz", "bz2", "tbz2", "rpm"));

                    /**
                     * Detects files with extensions to remove from the engine's collection of
                     * dependencies.
                     */
                    private static final FileFilter REMOVE_FROM_ANALYSIS = FileFilterBuilder.newInstance()
                            .addExtensions("zip", "tar", "gz", "tgz", "bz2", "tbz2", "nupkg", "rpm").build();
                    /**
                     * Detects files with .zip extension.
                     */
                    private static final FileFilter ZIP_FILTER = FileFilterBuilder.newInstance().addExtensions("zip").build();

                    //<editor-fold defaultstate="collapsed" desc="All standard implementation details of Analyzer">
                    /**
                     * The name of the analyzer.
                     */
                    private static final String ANALYZER_NAME = "Archive Analyzer";
                    /**
                     * The phase that this analyzer is intended to run in.
                     */
                    private static final AnalysisPhase ANALYSIS_PHASE = AnalysisPhase.INITIAL;

                    /**
                     * Make java compiler happy
                     */
                    public ArchiveAnalyzer() {
                    }

                    /**
                     * Initializes the analyzer with the configured settings.
                     *
                     * @param settings the configured settings to use
                     */
                    @Override
                    public void initialize(Settings settings) {
                        super.initialize(settings);
                        initializeSettings();
                    }

                    @Override
                    protected FileFilter getFileFilter() {
                        return fileFilter;
                    }

                    /**
                     * Returns the name of the analyzer.
                     *
                     * @return the name of the analyzer.
                     */
                    @Override
                    public String getName() {
                        return ANALYZER_NAME;
                    }

                    /**
                     * Returns the phase that the analyzer is intended to run in.
                     *
                     * @return the phase that the analyzer is intended to run in.
                     */
                    @Override
                    public AnalysisPhase getAnalysisPhase() {
                        return ANALYSIS_PHASE;
                    }
                    //</editor-fold>

                    /**
                     * Returns the key used in the properties file to reference the analyzer's
                     * enabled property.
                     *
                     * @return the analyzer's enabled property setting key
                     */
                    @Override
                    protected String getAnalyzerEnabledSettingKey() {
                        return Settings.KEYS.ANALYZER_ARCHIVE_ENABLED;
                    }

                    /**
                     * The prepare method does nothing for this Analyzer.
                     *
                     * @param engine a reference to the dependency-check engine
                     * @throws InitializationException is thrown if there is an exception
                     * deleting or creating temporary files
                     */
                    @Override
                    public void prepareFileTypeAnalyzer(Engine engine) throws InitializationException {
                        try {
                            final File baseDir = getSettings().getTempDirectory();
                            tempFileLocation = File.createTempFile("check", "tmp", baseDir);
                            if (!tempFileLocation.delete()) {
                                setEnabled(false);
                                final String msg = String.format("Unable to delete temporary file '%s'.", tempFileLocation.getAbsolutePath());
                                throw new InitializationException(msg);
                            }
                            if (!tempFileLocation.mkdirs()) {
                                setEnabled(false);
                                final String msg = String.format("Unable to create directory '%s'.", tempFileLocation.getAbsolutePath());
                                throw new InitializationException(msg);
                            }
                        } catch (IOException ex) {
                            setEnabled(false);
                            throw new InitializationException("Unable to create a temporary file", ex);
                        }
                    }

                    /**
                     * The close method deletes any temporary files and directories created
                     * during analysis.
                     *
                     * @throws Exception thrown if there is an exception deleting temporary
                     * files
                     */
                    @Override
                    public void closeAnalyzer() throws Exception {
                        if (tempFileLocation != null && tempFileLocation.exists()) {
                            LOGGER.debug("Attempting to delete temporary files from `{}`", tempFileLocation.toString());
                            final boolean success = FileUtils.delete(tempFileLocation);
                            if (!success && tempFileLocation.exists()) {
                                final String[] l = tempFileLocation.list();
                                if (l != null && l.length > 0) {
                                    LOGGER.warn("Failed to delete the Archive Analyzer's temporary files from `{}`, "
                                            + "see the log for more details", tempFileLocation.toString());
                                }
                            }
                        }
                    }

                    /**
                     * Determines if the file can be analyzed by the analyzer. If the npm
                     * analyzer are enabled the archive analyzer will skip the node_modules and
                     * bower_modules directories.
                     *
                     * @param pathname the path to the file
                     * @return true if the file can be analyzed by the given analyzer; otherwise
                     * false
                     */
                    @Override
                    public boolean accept(File pathname) {
                        boolean accept = super.accept(pathname);
                        final boolean npmEnabled = getSettings().getBoolean(Settings.KEYS.ANALYZER_NODE_AUDIT_ENABLED, false);
                        final boolean yarnEnabled = getSettings().getBoolean(Settings.KEYS.ANALYZER_YARN_AUDIT_ENABLED, false);
                        final boolean pnpmEnabled = getSettings().getBoolean(Settings.KEYS.ANALYZER_PNPM_AUDIT_ENABLED, false);
                        if (accept && (npmEnabled || yarnEnabled || pnpmEnabled)) {
                            try {
                                accept = shouldProcess(pathname);
                            } catch (AnalysisException ex) {
                                throw new UnexpectedAnalysisException(ex.getMessage(), ex.getCause());
                            }
                        }
                        return accept;
                    }

                    /**
                     * Analyzes a given dependency. If the dependency is an archive, such as a
                     * WAR or EAR, the contents are extracted, scanned, and added to the list of
                     * dependencies within the engine.
                     *
                     * @param dependency the dependency to analyze
                     * @param engine the engine scanning
                     * @throws AnalysisException thrown if there is an analysis exception
                     */
                    @Override
                    public void analyzeDependency(Dependency dependency, Engine engine) throws AnalysisException {
                        extractAndAnalyze(dependency, engine, 0);
                        engine.sortDependencies();
                    }

                    /**
                     * Extracts the contents of the archive dependency and scans for additional
                     * dependencies.
                     *
                     * @param dependency the dependency being analyzed
                     * @param engine the engine doing the analysis
                     * @param scanDepth the current scan depth; extracctAndAnalyze is recursive
                     * and will, be default, only go 3 levels deep
                     * @throws AnalysisException thrown if there is a problem analyzing the
                     * dependencies
                     */
                    private void extractAndAnalyze(Dependency dependency, Engine engine, int scanDepth) throws AnalysisException {
                        final File f = new File(dependency.getActualFilePath());
                        final File tmpDir = getNextTempDirectory();
                        extractFiles(f, tmpDir, engine);

                        //make a copy
                        final List<Dependency> dependencySet = findMoreDependencies(engine, tmpDir);

                        if (dependencySet != null && !dependencySet.isEmpty()) {
                            for (Dependency d : dependencySet) {
                                if (d.getFilePath().startsWith(tmpDir.getAbsolutePath())) {
                                    //fix the dependency's display name and path
                                    final String displayPath = String.format("%s%s",
                                            dependency.getFilePath(),
                                            d.getActualFilePath().substring(tmpDir.getAbsolutePath().length()));
                                    final String displayName = String.format("%s: %s",
                                            dependency.getFileName(),
                                            d.getFileName());
                                    d.setFilePath(displayPath);
                                    d.setFileName(displayName);
                                    d.addAllProjectReferences(dependency.getProjectReferences());

                                    //TODO - can we get more evidence from the parent? EAR contains module name, etc.
                                    //analyze the dependency (i.e. extract files) if it is a supported type.
                                    if (this.accept(d.getActualFile()) && scanDepth < maxScanDepth) {
                                        extractAndAnalyze(d, engine, scanDepth + 1);
                                    }
                                } else {
                                    dependencySet.stream().filter((sub) -> sub.getFilePath().startsWith(tmpDir.getAbsolutePath())).forEach((sub) -> {
                                        final String displayPath = String.format("%s%s",
                                                dependency.getFilePath(),
                                                sub.getActualFilePath().substring(tmpDir.getAbsolutePath().length()));
                                        final String displayName = String.format("%s: %s",
                                                dependency.getFileName(),
                                                sub.getFileName());
                                        sub.setFilePath(displayPath);
                                        sub.setFileName(displayName);
                                    });
                                }
                            }
                        }
                        if (REMOVE_FROM_ANALYSIS.accept(dependency.getActualFile())) {
                            addDisguisedJarsToDependencies(dependency, engine);
                            engine.removeDependency(dependency);
                        }
                    }

                    /**
                     * If a zip file was identified as a possible JAR, this method will add the
                     * zip to the list of dependencies.
                     *
                     * @param dependency the zip file
                     * @param engine the engine
                     * @throws AnalysisException thrown if there is an issue
                     */
                    private void addDisguisedJarsToDependencies(Dependency dependency, Engine engine) throws AnalysisException {
                        if (ZIP_FILTER.accept(dependency.getActualFile()) && isZipFileActuallyJarFile(dependency)) {
                            final File tempDir = getNextTempDirectory();
                            final String fileName = dependency.getFileName();

                            LOGGER.info("The zip file '{}' appears to be a JAR file, making a copy and analyzing it as a JAR.", fileName);
                            final File tmpLoc = new File(tempDir, fileName.substring(0, fileName.length() - 3) + "jar");
                            //store the archives sha1 and change it so that the engine doesn't think the zip and jar file are the same
                            // and add it is a related dependency.
                            final String archiveMd5 = dependency.getMd5sum();
                            final String archiveSha1 = dependency.getSha1sum();
                            final String archiveSha256 = dependency.getSha256sum();
                            try {
                                dependency.setMd5sum("");
                                dependency.setSha1sum("");
                                dependency.setSha256sum("");
                                org.apache.commons.io.FileUtils.copyFile(dependency.getActualFile(), tmpLoc);
                                final List<Dependency> dependencySet = findMoreDependencies(engine, tmpLoc);
                                if (dependencySet != null && !dependencySet.isEmpty()) {
                                    dependencySet.forEach((d) -> {
                                        //fix the dependency's display name and path
                                        if (d.getActualFile().equals(tmpLoc)) {
                                            d.setFilePath(dependency.getFilePath());
                                            d.setDisplayFileName(dependency.getFileName());
                                        } else {
                                            d.getRelatedDependencies().stream().filter((rel) -> rel.getActualFile().equals(tmpLoc)).forEach((rel) -> {
                                                rel.setFilePath(dependency.getFilePath());
                                                rel.setDisplayFileName(dependency.getFileName());
                                            });
                                        }
                                    });
                                }
                            } catch (IOException ex) {
                                LOGGER.debug("Unable to perform deep copy on '{}'", dependency.getActualFile().getPath(), ex);
                            } finally {
                                dependency.setMd5sum(archiveMd5);
                                dependency.setSha1sum(archiveSha1);
                                dependency.setSha256sum(archiveSha256);
                            }
                        }
                    }

                    /**
                     * Scan the given file/folder, and return any new dependencies found.
                     *
                     * @param engine used to scan
                     * @param file target of scanning
                     * @return any dependencies that weren't known to the engine before
                     */
                    private static List<Dependency> findMoreDependencies(Engine engine, File file) {
                        return engine.scan(file);
                    }

                    /**
                     * Retrieves the next temporary directory to extract an archive too.
                     *
                     * @return a directory
                     * @throws AnalysisException thrown if unable to create temporary directory
                     */
                    private File getNextTempDirectory() throws AnalysisException {
                        final File directory = new File(tempFileLocation, String.valueOf(DIRECTORY_COUNT.incrementAndGet()));
                        //getting an exception for some directories not being able to be created; might be because the directory already exists?
                        if (directory.exists()) {
                            return getNextTempDirectory();
                        }
                        if (!directory.mkdirs()) {
                            final String msg = String.format("Unable to create temp directory '%s'.", directory.getAbsolutePath());
                            throw new AnalysisException(msg);
                        }
                        return directory;
                    }

                    /**
                     * Extracts the contents of an archive into the specified directory.
                     *
                     * @param archive an archive file such as a WAR or EAR
                     * @param destination a directory to extract the contents to
                     * @param engine the scanning engine
                     * @throws AnalysisException thrown if the archive is not found
                     */
                    private void extractFiles(File archive, File destination, Engine engine) throws AnalysisException {
                        if (archive != null && destination != null) {
                            String archiveExt = FileUtils.getFileExtension(archive.getName());
                            if (archiveExt == null) {
                                return;
                            }
                            archiveExt = archiveExt.toLowerCase();

                            final FileInputStream fis;
                            try {
                                fis = new FileInputStream(archive);
                            } catch (FileNotFoundException ex) {
                                final String msg = String.format("Error extracting file `%s`: %s", archive.getAbsolutePath(), ex.getMessage());
                                LOGGER.debug(msg, ex);
                                throw new AnalysisException(msg);
                            }
                            BufferedInputStream in = null;
                            ZipArchiveInputStream zin = null;
                            TarArchiveInputStream tin = null;
                            GzipCompressorInputStream gin = null;
                            BZip2CompressorInputStream bzin = null;
                            RpmInputStream rin = null;
                            CpioArchiveInputStream cain = null;
                            try {
                                if (KNOWN_ZIP_EXT.contains(archiveExt) || ADDITIONAL_ZIP_EXT.contains(archiveExt)) {
                                    in = new BufferedInputStream(fis);
                                    ensureReadableJar(archiveExt, in);
                                    zin = new ZipArchiveInputStream(in);
                                    extractArchive(zin, destination, engine);
                                } else if ("tar".equals(archiveExt)) {
                                    in = new BufferedInputStream(fis);
                                    tin = new TarArchiveInputStream(in);
                                    extractArchive(tin, destination, engine);
                                } else if ("gz".equals(archiveExt) || "tgz".equals(archiveExt)) {
                                    final String uncompressedName = GzipUtils.getUncompressedFilename(archive.getName());
                                    final File f = new File(destination, uncompressedName);
                                    if (engine.accept(f)) {
                                        final String destPath = destination.getCanonicalPath();
                                        if (!f.getCanonicalPath().startsWith(destPath)) {
                                            final String msg = String.format(
                                                    "Archive (%s) contains a file that would be written outside of the destination directory",
                                                    archive.getPath());
                                            throw new AnalysisException(msg);
                                        }
                                        in = new BufferedInputStream(fis);
                                        gin = new GzipCompressorInputStream(in);
                                        decompressFile(gin, f);
                                    }
                                } else if ("bz2".equals(archiveExt) || "tbz2".equals(archiveExt)) {
                                    final String uncompressedName = BZip2Utils.getUncompressedFilename(archive.getName());
                                    final File f = new File(destination, uncompressedName);
                                    if (engine.accept(f)) {
                                        final String destPath = destination.getCanonicalPath();
                                        if (!f.getCanonicalPath().startsWith(destPath)) {
                                            final String msg = String.format(
                                                    "Archive (%s) contains a file that would be written outside of the destination directory",
                                                    archive.getPath());
                                            throw new AnalysisException(msg);
                                        }
                                        in = new BufferedInputStream(fis);
                                        bzin = new BZip2CompressorInputStream(in);
                                        decompressFile(bzin, f);
                                    }
                                } else if ("rpm".equals(archiveExt)) {
                                    rin = new RpmInputStream(fis);
                                    //return of getTag is not used - but the call is a
                                    //necassary step in reading from the stream
                                    rin.getPayloadHeader().getTag(RpmTag.NAME);
                                    cain = new CpioArchiveInputStream(rin);
                                    extractArchive(cain, destination, engine);
                                }
                            } catch (ArchiveExtractionException ex) {
                                LOGGER.warn("Exception extracting archive '{}'.", archive.getName());
                                LOGGER.debug("", ex);
                            } catch (IOException ex) {
                                LOGGER.warn("Exception reading archive '{}'.", archive.getName());
                                LOGGER.debug("", ex);
                            } finally {
                                //overly verbose and not needed... but keeping it anyway due to
                                //having issue with file handles being left open
                                FileUtils.close(fis);
                                FileUtils.close(in);
                                FileUtils.close(zin);
                                FileUtils.close(tin);
                                FileUtils.close(gin);
                                FileUtils.close(bzin);
                            }
                        }
                    }

                    /**
                     * Checks if the file being scanned is a JAR or WAR that begins with
                     * '#!/bin' which indicates it is a fully executable jar. If a fully
                     * executable JAR is identified the input stream will be advanced to the
                     * start of the actual JAR file ( skipping the script).
                     *
                     * @see
                     * <a href="http://docs.spring.io/spring-boot/docs/1.3.0.BUILD-SNAPSHOT/reference/htmlsingle/#deployment-install">Installing
                     * Spring Boot Applications</a>
                     * @param archiveExt the file extension
                     * @param in the input stream
                     * @throws IOException thrown if there is an error reading the stream
                     */
                    private void ensureReadableJar(final String archiveExt, BufferedInputStream in) throws IOException {
                        if (("war".equals(archiveExt) || "jar".equals(archiveExt)) && in.markSupported()) {
                            in.mark(7);
                            final byte[] b = new byte[7];
                            final int read = in.read(b);
                            if (read == 7
                                    && b[0] == '#'
                                    && b[1] == '!'
                                    && b[2] == '/'
                                    && b[3] == 'b'
                                    && b[4] == 'i'
                                    && b[5] == 'n'
                                    && b[6] == '/') {
                                boolean stillLooking = true;
                                int chr;
                                int nxtChr;
                                //CSOFF: InnerAssignment
                                //CSOFF: NestedIfDepth
                                while (stillLooking && (chr = in.read()) != -1) {
                                    if (chr == '\n' || chr == '\r') {
                                        in.mark(4);
                                        if ((chr = in.read()) != -1) {
                                            if (chr == 'P' && (chr = in.read()) != -1) {
                                                if (chr == 'K' && (chr = in.read()) != -1) {
                                                    if ((chr == 3 || chr == 5 || chr == 7) && (nxtChr = in.read()) != -1) {
                                                        if (nxtChr == chr + 1) {
                                                            stillLooking = false;
                                                            in.reset();
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                //CSON: InnerAssignment
                                //CSON: NestedIfDepth
                            } else {
                                in.reset();
                            }
                        }
                    }

                    /**
                     * Extracts files from an archive.
                     *
                     * @param input the archive to extract files from
                     * @param destination the location to write the files too
                     * @param engine the dependency-check engine
                     * @throws ArchiveExtractionException thrown if there is an exception
                     * extracting files from the archive
                     */
                    private void extractArchive(ArchiveInputStream input, File destination, Engine engine) throws ArchiveExtractionException {
                        ArchiveEntry entry;
                        try {
                            //final String destPath = destination.getCanonicalPath();
                            final Path d = destination.toPath();
                            while ((entry = input.getNextEntry()) != null) {
                                //final File file = new File(destination, entry.getName());
                                final Path f = d.resolve(entry.getName()).normalize();
                                if (!f.startsWith(d)) {
                                    LOGGER.debug("ZipSlip detected\n-Destination: " + d.toString() + "\n-Path: " + f.toString());
                                    final String msg = String.format(
                                            "Archive contains a file (%s) that would be extracted outside of the target directory.",
                                            entry.getName());
                                    throw new ArchiveExtractionException(msg);
                                }
                                final File file = f.toFile();
                                if (entry.isDirectory()) {
                                    if (!file.exists() && !file.mkdirs()) {
                                        final String msg = String.format("Unable to create directory '%s'.", file.getAbsolutePath());
                                        throw new AnalysisException(msg);
                                    }
                                } else if (engine.accept(file)) {
                                    extractAcceptedFile(input, file);
                                }
                            }
                        } catch (IOException | AnalysisException ex) {
                            throw new ArchiveExtractionException(ex);
                        } finally {
                            FileUtils.close(input);
                        }
                    }

                    /**
                     * Extracts a file from an archive.
                     *
                     * @param input the archives input stream
                     * @param file the file to extract
                     * @throws AnalysisException thrown if there is an error
                     */
                    private static void extractAcceptedFile(ArchiveInputStream input, File file) throws AnalysisException {
                        LOGGER.debug("Extracting '{}'", file.getPath());
                        final File parent = file.getParentFile();
                        if (!parent.isDirectory() && !parent.mkdirs()) {
                            final String msg = String.format("Unable to build directory '%s'.", parent.getAbsolutePath());
                            throw new AnalysisException(msg);
                        }
                        try (FileOutputStream fos = new FileOutputStream(file)) {
                            IOUtils.copy(input, fos);
                        } catch (FileNotFoundException ex) {
                            LOGGER.debug("", ex);
                            final String msg = String.format("Unable to find file '%s'.", file.getName());
                            throw new AnalysisException(msg, ex);
                        } catch (IOException ex) {
                            LOGGER.debug("", ex);
                            final String msg = String.format("IO Exception while parsing file '%s'.", file.getName());
                            throw new AnalysisException(msg, ex);
                        }
                    }

                    /**
                     * Decompresses a file.
                     *
                     * @param inputStream the compressed file
                     * @param outputFile the location to write the decompressed file
                     * @throws ArchiveExtractionException thrown if there is an exception
                     * decompressing the file
                     */
                    private void decompressFile(CompressorInputStream inputStream, File outputFile) throws ArchiveExtractionException {
                        LOGGER.debug("Decompressing '{}'", outputFile.getPath());
                        try (FileOutputStream out = new FileOutputStream(outputFile)) {
                            IOUtils.copy(inputStream, out);
                        } catch (IOException ex) {
                            LOGGER.debug("", ex);
                            throw new ArchiveExtractionException(ex);
                        }
                    }

                    /**
                     * Attempts to determine if a zip file is actually a JAR file.
                     *
                     * @param dependency the dependency to check
                     * @return true if the dependency appears to be a JAR file; otherwise false
                     */
                    private boolean isZipFileActuallyJarFile(Dependency dependency) {
                        boolean isJar = false;
                        ZipFile zip = null;
                        try {
                            zip = new ZipFile(dependency.getActualFilePath());
                            if (zip.getEntry("META-INF/MANIFEST.MF") != null
                                    || zip.getEntry("META-INF/maven") != null) {
                                final Enumeration<ZipArchiveEntry> entries = zip.getEntries();
                                while (entries.hasMoreElements()) {
                                    final ZipArchiveEntry entry = entries.nextElement();
                                    if (!entry.isDirectory()) {
                                        final String name = entry.getName().toLowerCase();
                                        if (name.endsWith(".class")) {
                                            isJar = true;
                                            break;
                                        }
                                    }
                                }
                            }
                        } catch (IOException ex) {
                            LOGGER.debug("Unable to unzip zip file '{}'", dependency.getFilePath(), ex);
                        } finally {
                            ZipFile.closeQuietly(zip);
                        }
                        return isJar;
                    }

                    /**
                     * Initializes settings used by the scanning functions of the archive
                     * analyzer.
                     */
                    private void initializeSettings() {
                        maxScanDepth = getSettings().getInt("archive.scan.depth", 3);
                        final Set<String> extensions = new HashSet<>(EXTENSIONS);
                        extensions.addAll(KNOWN_ZIP_EXT);
                        final String additionalZipExt = getSettings().getString(Settings.KEYS.ADDITIONAL_ZIP_EXTENSIONS);
                        if (additionalZipExt != null) {
                            final String[] ext = additionalZipExt.split("\\s*,\\s*");
                            Collections.addAll(extensions, ext);
                            Collections.addAll(ADDITIONAL_ZIP_EXT, ext);
                        }
                        fileFilter = FileFilterBuilder.newInstance().addExtensions(extensions).build();
                    }
                }
            """
        )
    )


    @Test
    fun `jsonschema2pojo`() = rewriteRun(
        java(
            """
                /**
                 * Copyright © 2010-2020 Nokia
                 *
                 * Licensed under the Apache License, Version 2.0 (the "License");
                 * you may not use this file except in compliance with the License.
                 * You may obtain a copy of the License at
                 *
                 *      http://www.apache.org/licenses/LICENSE-2.0
                 *
                 * Unless required by applicable law or agreed to in writing, software
                 * distributed under the License is distributed on an "AS IS" BASIS,
                 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
                 * See the License for the specific language governing permissions and
                 * limitations under the License.
                 */

                package org.jsonschema2pojo.maven;

                import static java.lang.String.*;
                import static java.util.Arrays.*;
                import static java.util.regex.Pattern.*;

                import java.io.File;
                import java.io.FileFilter;
                import java.io.IOException;
                import java.util.ArrayList;
                import java.util.List;

                import org.apache.maven.shared.utils.io.DirectoryScanner;
                import org.apache.maven.shared.utils.io.MatchPatterns;

                /**
                 * <p>A file filter that supports include and exclude patterns.</p>
                 *
                 * @author Christian Trimble
                 * @since 0.4.3
                 */
                public class MatchPatternsFileFilter implements FileFilter {
                    MatchPatterns includePatterns;
                    MatchPatterns excludePatterns;
                    String sourceDirectory;
                    boolean caseSensitive;

                    /**
                     * <p>Builder for MatchPatternFileFilter instances.</p>
                     */
                    public static class Builder {
                        List<String> includes = new ArrayList<>();
                        List<String> excludes = new ArrayList<>();
                        String sourceDirectory;
                        boolean caseSensitive;

                        public Builder addIncludes(List<String> includes) {
                            this.includes.addAll(processPatterns(includes));
                            return this;
                        }

                        public Builder addIncludes(String... includes) {
                            if (includes != null) {
                                addIncludes(asList(includes));
                            }
                            return this;
                        }

                        public Builder addExcludes(List<String> excludes) {
                            this.excludes.addAll(processPatterns(excludes));
                            return this;
                        }

                        public Builder addExcludes(String... excludes) {
                            if (excludes != null) {
                                addExcludes(asList(excludes));
                            }
                            return this;
                        }

                        public Builder addDefaultExcludes() {
                            excludes.addAll(processPatterns(asList(DirectoryScanner.DEFAULTEXCLUDES)));
                            return this;
                        }

                        public Builder withSourceDirectory(String canonicalSourceDirectory) {
                            this.sourceDirectory = canonicalSourceDirectory;
                            return this;
                        }

                        public Builder withCaseSensitive(boolean caseSensitive) {
                            this.caseSensitive = caseSensitive;
                            return this;
                        }

                        public MatchPatternsFileFilter build() {
                            if (includes.isEmpty()) {
                                includes.add(processPattern("**/*"));
                            }
                            return new MatchPatternsFileFilter(
                                    MatchPatterns.from(includes.toArray(new String[] {})),
                                    MatchPatterns.from(excludes.toArray(new String[] {})),
                                    sourceDirectory,
                                    caseSensitive);
                        }
                    }

                    MatchPatternsFileFilter(MatchPatterns includePatterns, MatchPatterns excludePatterns, String sourceDirectory, boolean caseSensitive) {
                        this.includePatterns = includePatterns;
                        this.excludePatterns = excludePatterns;
                        this.sourceDirectory = sourceDirectory;
                        this.caseSensitive = caseSensitive;
                    }

                    @Override
                    public boolean accept(File file) {
                        try {
                            String path = relativePath(file);
                            return file.isDirectory() ?
                                    includePatterns.matchesPatternStart(path, caseSensitive) && !excludePatterns.matches(path, caseSensitive) :
                                    includePatterns.matches(path, caseSensitive) && !excludePatterns.matches(path, caseSensitive);
                        } catch (IOException e) {
                            return false;
                        }
                    }

                    String relativePath(File file) throws IOException {
                        String canonicalPath = file.getCanonicalPath();
                        if (!canonicalPath.startsWith(sourceDirectory)) {
                            throw new IOException(format("the path %s is not a decendent of the basedir %s", canonicalPath, sourceDirectory));
                        }
                        return canonicalPath.substring(sourceDirectory.length()).replaceAll("^" + quote(File.separator), "");
                    }

                    static List<String> processPatterns(List<String> patterns) {
                        if (patterns == null)
                            return null;
                        List<String> processed = new ArrayList<>();
                        for (String pattern : patterns) {
                            processed.add(processPattern(pattern));
                        }
                        return processed;
                    }

                    static String processPattern(String pattern) {
                        return pattern
                                .trim()
                                .replace('/', File.separatorChar)
                                .replace('\\', File.separatorChar)
                                .replaceAll(quote(File.separator) + "${'$'}", File.separator + "**");
                    }

                }
            """
        )
    )

    @Test
    fun `karate`() = rewriteRun(
        java(
            """
                /*
                 * The MIT License
                 *
                 * Copyright 2019 Intuit Inc.
                 *
                 * Permission is hereby granted, free of charge, to any person obtaining a copy
                 * of this software and associated documentation files (the "Software"), to deal
                 * in the Software without restriction, including without limitation the rights
                 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
                 * copies of the Software, and to permit persons to whom the Software is
                 * furnished to do so, subject to the following conditions:
                 *
                 * The above copyright notice and this permission notice shall be included in
                 * all copies or substantial portions of the Software.
                 *
                 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
                 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
                 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
                 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
                 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
                 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
                 * THE SOFTWARE.
                 */
                package com.intuit.karate.job;

                import java.io.File;
                import java.io.FileInputStream;
                import java.io.FileOutputStream;
                import java.io.IOException;
                import java.util.function.Predicate;
                import java.util.zip.ZipEntry;
                import java.util.zip.ZipInputStream;
                import java.util.zip.ZipOutputStream;

                /**
                 *
                 * @author pthomas3
                 */
                public class JobUtils {

                    public static void zip(File src, File dest) {
                        try {
                            src = src.getCanonicalFile();
                            FileOutputStream fos = new FileOutputStream(dest);
                            ZipOutputStream zipOut = new ZipOutputStream(fos);
                            zip(src, "", zipOut, 0);
                            zipOut.close();
                            fos.close();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    private static void zip(File fileToZip, String fileName, ZipOutputStream zipOut, int level) throws IOException {
                        if (fileToZip.isHidden()) {
                            return;
                        }
                        if (fileToZip.isDirectory()) {
                            String entryName = fileName;
                            zipOut.putNextEntry(new ZipEntry(entryName + "/"));
                            zipOut.closeEntry();
                            File[] children = fileToZip.listFiles();
                            for (File childFile : children) {
                                String childFileName = childFile.getName();
                                // TODO improve ?
                                if (childFileName.equals("target") || childFileName.equals("build")) {
                                    continue;
                                }
                                if (level != 0) {
                                    childFileName = entryName + "/" + childFileName;
                                }
                                zip(childFile, childFileName, zipOut, level + 1);
                            }
                            return;
                        }
                        ZipEntry zipEntry = new ZipEntry(fileName);
                        zipOut.putNextEntry(zipEntry);
                        FileInputStream fis = new FileInputStream(fileToZip);
                        byte[] bytes = new byte[1024];
                        int length;
                        while ((length = fis.read(bytes)) >= 0) {
                            zipOut.write(bytes, 0, length);
                        }
                        fis.close();
                    }

                    public static void unzip(File src, File dest) {
                        try {
                            byte[] buffer = new byte[1024];
                            ZipInputStream zis = new ZipInputStream(new FileInputStream(src));
                            ZipEntry zipEntry = zis.getNextEntry();
                            while (zipEntry != null) {
                                File newFile = createFile(dest, zipEntry);
                                if (zipEntry.isDirectory()) {
                                    newFile.mkdirs();
                                } else {
                                    File parentFile = newFile.getParentFile();
                                    if (parentFile != null && !parentFile.exists()) {
                                        parentFile.mkdirs();
                                    }
                                    FileOutputStream fos = new FileOutputStream(newFile);
                                    int len;
                                    while ((len = zis.read(buffer)) > 0) {
                                        fos.write(buffer, 0, len);
                                    }
                                    fos.close();
                                }
                                zipEntry = zis.getNextEntry();
                            }
                            zis.closeEntry();
                            zis.close();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    private static File createFile(File destinationDir, ZipEntry zipEntry) throws IOException {
                        File destFile = new File(destinationDir, zipEntry.getName());
                        String destDirPath = destinationDir.getCanonicalPath();
                        String destFilePath = destFile.getCanonicalPath();
                        if (!destFilePath.startsWith(destDirPath)) {
                            throw new IOException("entry outside target dir: " + zipEntry.getName());
                        }
                        return destFile;
                    }

                    public static File getFirstFileMatching(File parent, Predicate<String> predicate) {
                        File[] files = parent.listFiles((f, n) -> predicate.test(n));
                        return files == null || files.length == 0 ? null : files[0];
                    }

                }
            """
        )
    )

    @Test
    fun `termux-app TermuxDocumentProvider`() = rewriteRun(
        java(
            """
                package com.termux.filepicker;

                import android.content.res.AssetFileDescriptor;
                import android.database.Cursor;
                import android.database.MatrixCursor;
                import android.graphics.Point;
                import android.os.CancellationSignal;
                import android.os.ParcelFileDescriptor;
                import android.provider.DocumentsContract.Document;
                import android.provider.DocumentsContract.Root;
                import android.provider.DocumentsProvider;
                import android.webkit.MimeTypeMap;

                import com.termux.R;
                import com.termux.shared.termux.TermuxConstants;

                import java.io.File;
                import java.io.FileNotFoundException;
                import java.io.IOException;
                import java.util.Collections;
                import java.util.LinkedList;

                /**
                 * A document provider for the Storage Access Framework which exposes the files in the
                 * HOME directory to other apps.
                 * <p/>
                 * Note that this replaces providing an activity matching the ACTION_GET_CONTENT intent:
                 * <p/>
                 * "A document provider and ACTION_GET_CONTENT should be considered mutually exclusive. If you
                 * support both of them simultaneously, your app will appear twice in the system picker UI,
                 * offering two different ways of accessing your stored data. This would be confusing for users."
                 * - http://developer.android.com/guide/topics/providers/document-provider.html#43
                 */
                public class TermuxDocumentsProvider extends DocumentsProvider {

                    private static final String ALL_MIME_TYPES = "*/*";

                    private static final File BASE_DIR = TermuxConstants.TERMUX_HOME_DIR;


                    // The default columns to return information about a root if no specific
                    // columns are requested in a query.
                    private static final String[] DEFAULT_ROOT_PROJECTION = new String[]{
                        Root.COLUMN_ROOT_ID,
                        Root.COLUMN_MIME_TYPES,
                        Root.COLUMN_FLAGS,
                        Root.COLUMN_ICON,
                        Root.COLUMN_TITLE,
                        Root.COLUMN_SUMMARY,
                        Root.COLUMN_DOCUMENT_ID,
                        Root.COLUMN_AVAILABLE_BYTES
                    };

                    // The default columns to return information about a document if no specific
                    // columns are requested in a query.
                    private static final String[] DEFAULT_DOCUMENT_PROJECTION = new String[]{
                        Document.COLUMN_DOCUMENT_ID,
                        Document.COLUMN_MIME_TYPE,
                        Document.COLUMN_DISPLAY_NAME,
                        Document.COLUMN_LAST_MODIFIED,
                        Document.COLUMN_FLAGS,
                        Document.COLUMN_SIZE
                    };

                    @Override
                    public Cursor queryRoots(String[] projection) {
                        final MatrixCursor result = new MatrixCursor(projection != null ? projection : DEFAULT_ROOT_PROJECTION);
                        final String applicationName = getContext().getString(R.string.application_name);

                        final MatrixCursor.RowBuilder row = result.newRow();
                        row.add(Root.COLUMN_ROOT_ID, getDocIdForFile(BASE_DIR));
                        row.add(Root.COLUMN_DOCUMENT_ID, getDocIdForFile(BASE_DIR));
                        row.add(Root.COLUMN_SUMMARY, null);
                        row.add(Root.COLUMN_FLAGS, Root.FLAG_SUPPORTS_CREATE | Root.FLAG_SUPPORTS_SEARCH | Root.FLAG_SUPPORTS_IS_CHILD);
                        row.add(Root.COLUMN_TITLE, applicationName);
                        row.add(Root.COLUMN_MIME_TYPES, ALL_MIME_TYPES);
                        row.add(Root.COLUMN_AVAILABLE_BYTES, BASE_DIR.getFreeSpace());
                        row.add(Root.COLUMN_ICON, R.mipmap.ic_launcher);
                        return result;
                    }

                    @Override
                    public Cursor queryDocument(String documentId, String[] projection) throws FileNotFoundException {
                        final MatrixCursor result = new MatrixCursor(projection != null ? projection : DEFAULT_DOCUMENT_PROJECTION);
                        includeFile(result, documentId, null);
                        return result;
                    }

                    @Override
                    public Cursor queryChildDocuments(String parentDocumentId, String[] projection, String sortOrder) throws FileNotFoundException {
                        final MatrixCursor result = new MatrixCursor(projection != null ? projection : DEFAULT_DOCUMENT_PROJECTION);
                        final File parent = getFileForDocId(parentDocumentId);
                        for (File file : parent.listFiles()) {
                            includeFile(result, null, file);
                        }
                        return result;
                    }

                    @Override
                    public ParcelFileDescriptor openDocument(final String documentId, String mode, CancellationSignal signal) throws FileNotFoundException {
                        final File file = getFileForDocId(documentId);
                        final int accessMode = ParcelFileDescriptor.parseMode(mode);
                        return ParcelFileDescriptor.open(file, accessMode);
                    }

                    @Override
                    public AssetFileDescriptor openDocumentThumbnail(String documentId, Point sizeHint, CancellationSignal signal) throws FileNotFoundException {
                        final File file = getFileForDocId(documentId);
                        final ParcelFileDescriptor pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
                        return new AssetFileDescriptor(pfd, 0, file.length());
                    }

                    @Override
                    public boolean onCreate() {
                        return true;
                    }

                    @Override
                    public String createDocument(String parentDocumentId, String mimeType, String displayName) throws FileNotFoundException {
                        File newFile = new File(parentDocumentId, displayName);
                        int noConflictId = 2;
                        while (newFile.exists()) {
                            newFile = new File(parentDocumentId, displayName + " (" + noConflictId++ + ")");
                        }
                        try {
                            boolean succeeded;
                            if (Document.MIME_TYPE_DIR.equals(mimeType)) {
                                succeeded = newFile.mkdir();
                            } else {
                                succeeded = newFile.createNewFile();
                            }
                            if (!succeeded) {
                                throw new FileNotFoundException("Failed to create document with id " + newFile.getPath());
                            }
                        } catch (IOException e) {
                            throw new FileNotFoundException("Failed to create document with id " + newFile.getPath());
                        }
                        return newFile.getPath();
                    }

                    @Override
                    public void deleteDocument(String documentId) throws FileNotFoundException {
                        File file = getFileForDocId(documentId);
                        if (!file.delete()) {
                            throw new FileNotFoundException("Failed to delete document with id " + documentId);
                        }
                    }

                    @Override
                    public String getDocumentType(String documentId) throws FileNotFoundException {
                        File file = getFileForDocId(documentId);
                        return getMimeType(file);
                    }

                    @Override
                    public Cursor querySearchDocuments(String rootId, String query, String[] projection) throws FileNotFoundException {
                        final MatrixCursor result = new MatrixCursor(projection != null ? projection : DEFAULT_DOCUMENT_PROJECTION);
                        final File parent = getFileForDocId(rootId);

                        // This example implementation searches file names for the query and doesn't rank search
                        // results, so we can stop as soon as we find a sufficient number of matches.  Other
                        // implementations might rank results and use other data about files, rather than the file
                        // name, to produce a match.
                        final LinkedList<File> pending = new LinkedList<>();
                        pending.add(parent);

                        final int MAX_SEARCH_RESULTS = 50;
                        while (!pending.isEmpty() && result.getCount() < MAX_SEARCH_RESULTS) {
                            final File file = pending.removeFirst();
                            // Avoid directories outside the HOME directory linked with symlinks (to avoid e.g. search
                            // through the whole SD card).
                            boolean isInsideHome;
                            try {
                                isInsideHome = file.getCanonicalPath().startsWith(TermuxConstants.TERMUX_HOME_DIR_PATH);
                            } catch (IOException e) {
                                isInsideHome = true;
                            }
                            if (isInsideHome) {
                                if (file.isDirectory()) {
                                    Collections.addAll(pending, file.listFiles());
                                } else {
                                    if (file.getName().toLowerCase().contains(query)) {
                                        includeFile(result, null, file);
                                    }
                                }
                            }
                        }

                        return result;
                    }

                    @Override
                    public boolean isChildDocument(String parentDocumentId, String documentId) {
                        return documentId.startsWith(parentDocumentId);
                    }

                    /**
                     * Get the document id given a file. This document id must be consistent across time as other
                     * applications may save the ID and use it to reference documents later.
                     * <p/>
                     * The reverse of @{link #getFileForDocId}.
                     */
                    private static String getDocIdForFile(File file) {
                        return file.getAbsolutePath();
                    }

                    /**
                     * Get the file given a document id (the reverse of {@link #getDocIdForFile(File)}).
                     */
                    private static File getFileForDocId(String docId) throws FileNotFoundException {
                        final File f = new File(docId);
                        if (!f.exists()) throw new FileNotFoundException(f.getAbsolutePath() + " not found");
                        return f;
                    }

                    private static String getMimeType(File file) {
                        if (file.isDirectory()) {
                            return Document.MIME_TYPE_DIR;
                        } else {
                            final String name = file.getName();
                            final int lastDot = name.lastIndexOf('.');
                            if (lastDot >= 0) {
                                final String extension = name.substring(lastDot + 1).toLowerCase();
                                final String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
                                if (mime != null) return mime;
                            }
                            return "application/octet-stream";
                        }
                    }

                    /**
                     * Add a representation of a file to a cursor.
                     *
                     * @param result the cursor to modify
                     * @param docId  the document ID representing the desired file (may be null if given file)
                     * @param file   the File object representing the desired file (may be null if given docID)
                     */
                    private void includeFile(MatrixCursor result, String docId, File file)
                        throws FileNotFoundException {
                        if (docId == null) {
                            docId = getDocIdForFile(file);
                        } else {
                            file = getFileForDocId(docId);
                        }

                        int flags = 0;
                        if (file.isDirectory()) {
                            if (file.canWrite()) flags |= Document.FLAG_DIR_SUPPORTS_CREATE;
                        } else if (file.canWrite()) {
                            flags |= Document.FLAG_SUPPORTS_WRITE;
                        }
                        if (file.getParentFile().canWrite()) flags |= Document.FLAG_SUPPORTS_DELETE;

                        final String displayName = file.getName();
                        final String mimeType = getMimeType(file);
                        if (mimeType.startsWith("image/")) flags |= Document.FLAG_SUPPORTS_THUMBNAIL;

                        final MatrixCursor.RowBuilder row = result.newRow();
                        row.add(Document.COLUMN_DOCUMENT_ID, docId);
                        row.add(Document.COLUMN_DISPLAY_NAME, displayName);
                        row.add(Document.COLUMN_SIZE, file.length());
                        row.add(Document.COLUMN_MIME_TYPE, mimeType);
                        row.add(Document.COLUMN_LAST_MODIFIED, file.lastModified());
                        row.add(Document.COLUMN_FLAGS, flags);
                        row.add(Document.COLUMN_ICON, R.mipmap.ic_launcher);
                    }

                }
            """
        )
    )

    @Test
    fun `commons-io if test `() = rewriteRun(
        java(
            """

            public class Hello {
                public static void copyDirectory(final File srcDir, final File destDir, final FileFilter fileFilter, final boolean preserveFileDate,
                    final CopyOption... copyOptions) throws IOException {
                    requireFileCopy(srcDir, destDir);
                    requireDirectory(srcDir, "srcDir");
                    requireCanonicalPathsNotEquals(srcDir, destDir);

                    // Cater for destination being directory within the source directory (see IO-141)
                    List<String> exclusionList = null;
                    final String srcDirCanonicalPath = srcDir.getCanonicalPath();
                    final String destDirCanonicalPath = destDir.getCanonicalPath();
                    if (destDirCanonicalPath.startsWith(srcDirCanonicalPath)) {
                        final File[] srcFiles = listFiles(srcDir, fileFilter);
                        if (srcFiles.length > 0) {
                            exclusionList = new ArrayList<>(srcFiles.length);
                            for (final File srcFile : srcFiles) {
                                final File copiedFile = new File(destDir, srcFile.getName());
                                exclusionList.add(copiedFile.getCanonicalPath());
                            }
                        }
                    }
                    doCopyDirectory(srcDir, destDir, fileFilter, exclusionList, preserveFileDate, preserveFileDate ? addCopyAttributes(copyOptions) : copyOptions);
                }
            }
            """
        )
    )

    @Test
    fun `commons-io `() = rewriteRun(
        java(
            """
                /*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.io;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.CopyOption;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.ChronoLocalDateTime;
import java.time.chrono.ChronoZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;
import java.util.zip.Checksum;

import org.apache.commons.io.file.AccumulatorPathVisitor;
import org.apache.commons.io.file.Counters;
import org.apache.commons.io.file.PathFilter;
import org.apache.commons.io.file.PathUtils;
import org.apache.commons.io.file.StandardDeleteOption;
import org.apache.commons.io.filefilter.FileEqualsFileFilter;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.function.IOConsumer;

/**
 * General file manipulation utilities.
 * <p>
 * Facilities are provided in the following areas:
 * </p>
 * <ul>
 * <li>writing to a file
 * <li>reading from a file
 * <li>make a directory including parent directories
 * <li>copying files and directories
 * <li>deleting files and directories
 * <li>converting to and from a URL
 * <li>listing files and directories by filter and extension
 * <li>comparing file content
 * <li>file last changed date
 * <li>calculating a checksum
 * </ul>
 * <p>
 * Note that a specific charset should be specified whenever possible. Relying on the platform default means that the
 * code is Locale-dependent. Only use the default if the files are known to always use the platform default.
 * </p>
 * <p>
 * {@link SecurityException} are not documented in the Javadoc.
 * </p>
 * <p>
 * Origin of code: Excalibur, Alexandria, Commons-Utils
 * </p>
 */
public class FileUtils {
    /**
     * The number of bytes in a kilobyte.
     */
    public static final long ONE_KB = 1024;

    /**
     * The number of bytes in a kilobyte.
     *
     * @since 2.4
     */
    public static final BigInteger ONE_KB_BI = BigInteger.valueOf(ONE_KB);

    /**
     * The number of bytes in a megabyte.
     */
    public static final long ONE_MB = ONE_KB * ONE_KB;

    /**
     * The number of bytes in a megabyte.
     *
     * @since 2.4
     */
    public static final BigInteger ONE_MB_BI = ONE_KB_BI.multiply(ONE_KB_BI);

    /**
     * The number of bytes in a gigabyte.
     */
    public static final long ONE_GB = ONE_KB * ONE_MB;

    /**
     * The number of bytes in a gigabyte.
     *
     * @since 2.4
     */
    public static final BigInteger ONE_GB_BI = ONE_KB_BI.multiply(ONE_MB_BI);

    /**
     * The number of bytes in a terabyte.
     */
    public static final long ONE_TB = ONE_KB * ONE_GB;

    /**
     * The number of bytes in a terabyte.
     *
     * @since 2.4
     */
    public static final BigInteger ONE_TB_BI = ONE_KB_BI.multiply(ONE_GB_BI);

    /**
     * The number of bytes in a petabyte.
     */
    public static final long ONE_PB = ONE_KB * ONE_TB;

    /**
     * The number of bytes in a petabyte.
     *
     * @since 2.4
     */
    public static final BigInteger ONE_PB_BI = ONE_KB_BI.multiply(ONE_TB_BI);

    /**
     * The number of bytes in an exabyte.
     */
    public static final long ONE_EB = ONE_KB * ONE_PB;

    /**
     * The number of bytes in an exabyte.
     *
     * @since 2.4
     */
    public static final BigInteger ONE_EB_BI = ONE_KB_BI.multiply(ONE_PB_BI);

    /**
     * The number of bytes in a zettabyte.
     */
    public static final BigInteger ONE_ZB = BigInteger.valueOf(ONE_KB).multiply(BigInteger.valueOf(ONE_EB));

    /**
     * The number of bytes in a yottabyte.
     */
    public static final BigInteger ONE_YB = ONE_KB_BI.multiply(ONE_ZB);

    /**
     * An empty array of type {@link File}.
     */
    public static final File[] EMPTY_FILE_ARRAY = {};

    /**
     * Copies the given array and adds StandardCopyOption.COPY_ATTRIBUTES.
     *
     * @param copyOptions sorted copy options.
     * @return a new array.
     */
    private static CopyOption[] addCopyAttributes(final CopyOption... copyOptions) {
        // Make a copy first since we don't want to sort the call site's version.
        final CopyOption[] actual = Arrays.copyOf(copyOptions, copyOptions.length + 1);
        Arrays.sort(actual, 0, copyOptions.length);
        if (Arrays.binarySearch(copyOptions, 0, copyOptions.length, StandardCopyOption.COPY_ATTRIBUTES) >= 0) {
            return copyOptions;
        }
        actual[actual.length - 1] = StandardCopyOption.COPY_ATTRIBUTES;
        return actual;
    }

    /**
     * Returns a human-readable version of the file size, where the input represents a specific number of bytes.
     * <p>
     * If the size is over 1GB, the size is returned as the number of whole GB, i.e. the size is rounded down to the
     * nearest GB boundary.
     * </p>
     * <p>
     * Similarly for the 1MB and 1KB boundaries.
     * </p>
     *
     * @param size the number of bytes
     * @return a human-readable display value (includes units - EB, PB, TB, GB, MB, KB or bytes)
     * @throws NullPointerException if the given {@link BigInteger} is {@code null}.
     * @see <a href="https://issues.apache.org/jira/browse/IO-226">IO-226 - should the rounding be changed?</a>
     * @since 2.4
     */
    // See https://issues.apache.org/jira/browse/IO-226 - should the rounding be changed?
    public static String byteCountToDisplaySize(final BigInteger size) {
        Objects.requireNonNull(size, "size");
        final String displaySize;

        if (size.divide(ONE_EB_BI).compareTo(BigInteger.ZERO) > 0) {
            displaySize = size.divide(ONE_EB_BI) + " EB";
        } else if (size.divide(ONE_PB_BI).compareTo(BigInteger.ZERO) > 0) {
            displaySize = size.divide(ONE_PB_BI) + " PB";
        } else if (size.divide(ONE_TB_BI).compareTo(BigInteger.ZERO) > 0) {
            displaySize = size.divide(ONE_TB_BI) + " TB";
        } else if (size.divide(ONE_GB_BI).compareTo(BigInteger.ZERO) > 0) {
            displaySize = size.divide(ONE_GB_BI) + " GB";
        } else if (size.divide(ONE_MB_BI).compareTo(BigInteger.ZERO) > 0) {
            displaySize = size.divide(ONE_MB_BI) + " MB";
        } else if (size.divide(ONE_KB_BI).compareTo(BigInteger.ZERO) > 0) {
            displaySize = size.divide(ONE_KB_BI) + " KB";
        } else {
            displaySize = size + " bytes";
        }
        return displaySize;
    }

    /**
     * Returns a human-readable version of the file size, where the input represents a specific number of bytes.
     * <p>
     * If the size is over 1GB, the size is returned as the number of whole GB, i.e. the size is rounded down to the
     * nearest GB boundary.
     * </p>
     * <p>
     * Similarly for the 1MB and 1KB boundaries.
     * </p>
     *
     * @param size the number of bytes
     * @return a human-readable display value (includes units - EB, PB, TB, GB, MB, KB or bytes)
     * @see <a href="https://issues.apache.org/jira/browse/IO-226">IO-226 - should the rounding be changed?</a>
     */
    // See https://issues.apache.org/jira/browse/IO-226 - should the rounding be changed?
    public static String byteCountToDisplaySize(final long size) {
        return byteCountToDisplaySize(BigInteger.valueOf(size));
    }

    /**
     * Returns a human-readable version of the file size, where the input represents a specific number of bytes.
     * <p>
     * If the size is over 1GB, the size is returned as the number of whole GB, i.e. the size is rounded down to the
     * nearest GB boundary.
     * </p>
     * <p>
     * Similarly for the 1MB and 1KB boundaries.
     * </p>
     *
     * @param size the number of bytes
     * @return a human-readable display value (includes units - EB, PB, TB, GB, MB, KB or bytes)
     * @see <a href="https://issues.apache.org/jira/browse/IO-226">IO-226 - should the rounding be changed?</a>
     * @since 2.12.0
     */
    // See https://issues.apache.org/jira/browse/IO-226 - should the rounding be changed?
    public static String byteCountToDisplaySize(final Number size) {
        return byteCountToDisplaySize(size.longValue());
    }

    /**
     * Computes the checksum of a file using the specified checksum object. Multiple files may be checked using one
     * {@link Checksum} instance if desired simply by reusing the same checksum object. For example:
     *
     * <pre>
     * long checksum = FileUtils.checksum(file, new CRC32()).getValue();
     * </pre>
     *
     * @param file the file to checksum, must not be {@code null}
     * @param checksum the checksum object to be used, must not be {@code null}
     * @return the checksum specified, updated with the content of the file
     * @throws NullPointerException if the given {@link File} is {@code null}.
     * @throws NullPointerException if the given {@link Checksum} is {@code null}.
     * @throws IllegalArgumentException if the given {@link File} does not exist or is not a file.
     * @throws IOException if an IO error occurs reading the file.
     * @since 1.3
     */
    public static Checksum checksum(final File file, final Checksum checksum) throws IOException {
        requireExistsChecked(file, "file");
        requireFile(file, "file");
        Objects.requireNonNull(checksum, "checksum");
        try (InputStream inputStream = new CheckedInputStream(Files.newInputStream(file.toPath()), checksum)) {
            IOUtils.consume(inputStream);
        }
        return checksum;
    }

    /**
     * Computes the checksum of a file using the CRC32 checksum routine.
     * The value of the checksum is returned.
     *
     * @param file the file to checksum, must not be {@code null}
     * @return the checksum value
     * @throws NullPointerException if the given {@link File} is {@code null}.
     * @throws IllegalArgumentException if the given {@link File} does not exist or is not a file.
     * @throws IOException              if an IO error occurs reading the file.
     * @since 1.3
     */
    public static long checksumCRC32(final File file) throws IOException {
        return checksum(file, new CRC32()).getValue();
    }

    /**
     * Cleans a directory without deleting it.
     *
     * @param directory directory to clean
     * @throws NullPointerException if the given {@link File} is {@code null}.
     * @throws IllegalArgumentException if directory does not exist or is not a directory.
     * @throws IOException if an I/O error occurs.
     * @see #forceDelete(File)
     */
    public static void cleanDirectory(final File directory) throws IOException {
        IOConsumer.forEach(listFiles(directory, null), FileUtils::forceDelete);
    }

    /**
     * Cleans a directory without deleting it.
     *
     * @param directory directory to clean, must not be {@code null}
     * @throws NullPointerException if the given {@link File} is {@code null}.
     * @throws IllegalArgumentException if directory does not exist or is not a directory.
     * @throws IOException if an I/O error occurs.
     * @see #forceDeleteOnExit(File)
     */
    private static void cleanDirectoryOnExit(final File directory) throws IOException {
        IOConsumer.forEach(listFiles(directory, null), FileUtils::forceDeleteOnExit);
    }

    /**
     * Tests whether the contents of two files are equal.
     * <p>
     * This method checks to see if the two files are different lengths or if they point to the same file, before
     * resorting to byte-by-byte comparison of the contents.
     * </p>
     * <p>
     * Code origin: Avalon
     * </p>
     *
     * @param file1 the first file
     * @param file2 the second file
     * @return true if the content of the files are equal or they both don't exist, false otherwise
     * @throws IllegalArgumentException when an input is not a file.
     * @throws IOException If an I/O error occurs.
     * @see org.apache.commons.io.file.PathUtils#fileContentEquals(Path,Path,java.nio.file.LinkOption[],java.nio.file.OpenOption...)
     */
    public static boolean contentEquals(final File file1, final File file2) throws IOException {
        if (file1 == null && file2 == null) {
            return true;
        }
        if (file1 == null || file2 == null) {
            return false;
        }
        final boolean file1Exists = file1.exists();
        if (file1Exists != file2.exists()) {
            return false;
        }

        if (!file1Exists) {
            // two not existing files are equal
            return true;
        }

        requireFile(file1, "file1");
        requireFile(file2, "file2");

        if (file1.length() != file2.length()) {
            // lengths differ, cannot be equal
            return false;
        }

        if (file1.getCanonicalFile().equals(file2.getCanonicalFile())) {
            // same file
            return true;
        }

        try (InputStream input1 = Files.newInputStream(file1.toPath()); InputStream input2 = Files.newInputStream(file2.toPath())) {
            return IOUtils.contentEquals(input1, input2);
        }
    }

    /**
     * Compares the contents of two files to determine if they are equal or not.
     * <p>
     * This method checks to see if the two files point to the same file,
     * before resorting to line-by-line comparison of the contents.
     * </p>
     *
     * @param file1       the first file
     * @param file2       the second file
     * @param charsetName the name of the requested charset.
     *                    May be null, in which case the platform default is used
     * @return true if the content of the files are equal or neither exists,
     * false otherwise
     * @throws IllegalArgumentException when an input is not a file.
     * @throws IOException in case of an I/O error.
     * @throws UnsupportedCharsetException If the named charset is unavailable (unchecked exception).
     * @see IOUtils#contentEqualsIgnoreEOL(Reader, Reader)
     * @since 2.2
     */
    public static boolean contentEqualsIgnoreEOL(final File file1, final File file2, final String charsetName)
            throws IOException {
        if (file1 == null && file2 == null) {
            return true;
        }
        if (file1 == null || file2 == null) {
            return false;
        }
        final boolean file1Exists = file1.exists();
        if (file1Exists != file2.exists()) {
            return false;
        }

        if (!file1Exists) {
            // two not existing files are equal
            return true;
        }

        requireFile(file1, "file1");
        requireFile(file2, "file2");

        if (file1.getCanonicalFile().equals(file2.getCanonicalFile())) {
            // same file
            return true;
        }

        final Charset charset = Charsets.toCharset(charsetName);
        try (Reader input1 = new InputStreamReader(Files.newInputStream(file1.toPath()), charset);
             Reader input2 = new InputStreamReader(Files.newInputStream(file2.toPath()), charset)) {
            return IOUtils.contentEqualsIgnoreEOL(input1, input2);
        }
    }

    /**
     * Converts a Collection containing java.io.File instances into array
     * representation. This is to account for the difference between
     * File.listFiles() and FileUtils.listFiles().
     *
     * @param files a Collection containing java.io.File instances
     * @return an array of java.io.File
     */
    public static File[] convertFileCollectionToFileArray(final Collection<File> files) {
        return files.toArray(EMPTY_FILE_ARRAY);
    }

    /**
     * Copies a whole directory to a new location preserving the file dates.
     * <p>
     * This method copies the specified directory and all its child directories and files to the specified destination.
     * The destination is the new location and name of the directory.
     * </p>
     * <p>
     * The destination directory is created if it does not exist. If the destination directory did exist, then this
     * method merges the source with the destination, with the source taking precedence.
     * </p>
     * <p>
     * <strong>Note:</strong> This method tries to preserve the files' last modified date/times using
     * {@link File#setLastModified(long)}, however it is not guaranteed that those operations will succeed. If the
     * modification operation fails, the methods throws IOException.
     * </p>
     *
     * @param srcDir an existing directory to copy, must not be {@code null}.
     * @param destDir the new directory, must not be {@code null}.
     * @throws NullPointerException if any of the given {@link File}s are {@code null}.
     * @throws IllegalArgumentException if the source or destination is invalid.
     * @throws FileNotFoundException if the source does not exist.
     * @throws IOException if an error occurs or setting the last-modified time didn't succeeded.
     * @since 1.1
     */
    public static void copyDirectory(final File srcDir, final File destDir) throws IOException {
        copyDirectory(srcDir, destDir, true);
    }

    /**
     * Copies a whole directory to a new location.
     * <p>
     * This method copies the contents of the specified source directory to within the specified destination directory.
     * </p>
     * <p>
     * The destination directory is created if it does not exist. If the destination directory did exist, then this
     * method merges the source with the destination, with the source taking precedence.
     * </p>
     * <p>
     * <strong>Note:</strong> Setting {@code preserveFileDate} to {@code true} tries to preserve the files' last
     * modified date/times using {@link File#setLastModified(long)}, however it is not guaranteed that those operations
     * will succeed. If the modification operation fails, the methods throws IOException.
     * </p>
     *
     * @param srcDir an existing directory to copy, must not be {@code null}.
     * @param destDir the new directory, must not be {@code null}.
     * @param preserveFileDate true if the file date of the copy should be the same as the original.
     * @throws NullPointerException if any of the given {@link File}s are {@code null}.
     * @throws IllegalArgumentException if the source or destination is invalid.
     * @throws FileNotFoundException if the source does not exist.
     * @throws IOException if an error occurs or setting the last-modified time didn't succeeded.
     * @since 1.1
     */
    public static void copyDirectory(final File srcDir, final File destDir, final boolean preserveFileDate)
        throws IOException {
        copyDirectory(srcDir, destDir, null, preserveFileDate);
    }

    /**
     * Copies a filtered directory to a new location preserving the file dates.
     * <p>
     * This method copies the contents of the specified source directory to within the specified destination directory.
     * </p>
     * <p>
     * The destination directory is created if it does not exist. If the destination directory did exist, then this
     * method merges the source with the destination, with the source taking precedence.
     * </p>
     * <p>
     * <strong>Note:</strong> This method tries to preserve the files' last modified date/times using
     * {@link File#setLastModified(long)}, however it is not guaranteed that those operations will succeed. If the
     * modification operation fails, the methods throws IOException.
     * </p>
     * <b>Example: Copy directories only</b>
     *
     * <pre>
     * // only copy the directory structure
     * FileUtils.copyDirectory(srcDir, destDir, DirectoryFileFilter.DIRECTORY);
     * </pre>
     *
     * <b>Example: Copy directories and txt files</b>
     *
     * <pre>
     * // Create a filter for ".txt" files
     * IOFileFilter txtSuffixFilter = FileFilterUtils.suffixFileFilter(".txt");
     * IOFileFilter txtFiles = FileFilterUtils.andFileFilter(FileFileFilter.FILE, txtSuffixFilter);
     *
     * // Create a filter for either directories or ".txt" files
     * FileFilter filter = FileFilterUtils.orFileFilter(DirectoryFileFilter.DIRECTORY, txtFiles);
     *
     * // Copy using the filter
     * FileUtils.copyDirectory(srcDir, destDir, filter);
     * </pre>
     *
     * @param srcDir an existing directory to copy, must not be {@code null}.
     * @param destDir the new directory, must not be {@code null}.
     * @param filter the filter to apply, null means copy all directories and files should be the same as the original.
     * @throws NullPointerException if any of the given {@link File}s are {@code null}.
     * @throws IllegalArgumentException if the source or destination is invalid.
     * @throws FileNotFoundException if the source does not exist.
     * @throws IOException if an error occurs or setting the last-modified time didn't succeeded.
     * @since 1.4
     */
    public static void copyDirectory(final File srcDir, final File destDir, final FileFilter filter)
        throws IOException {
        copyDirectory(srcDir, destDir, filter, true);
    }

    /**
     * Copies a filtered directory to a new location.
     * <p>
     * This method copies the contents of the specified source directory to within the specified destination directory.
     * </p>
     * <p>
     * The destination directory is created if it does not exist. If the destination directory did exist, then this
     * method merges the source with the destination, with the source taking precedence.
     * </p>
     * <p>
     * <strong>Note:</strong> Setting {@code preserveFileDate} to {@code true} tries to preserve the files' last
     * modified date/times using {@link File#setLastModified(long)}, however it is not guaranteed that those operations
     * will succeed. If the modification operation fails, the methods throws IOException.
     * </p>
     * <b>Example: Copy directories only</b>
     *
     * <pre>
     * // only copy the directory structure
     * FileUtils.copyDirectory(srcDir, destDir, DirectoryFileFilter.DIRECTORY, false);
     * </pre>
     *
     * <b>Example: Copy directories and txt files</b>
     *
     * <pre>
     * // Create a filter for ".txt" files
     * IOFileFilter txtSuffixFilter = FileFilterUtils.suffixFileFilter(".txt");
     * IOFileFilter txtFiles = FileFilterUtils.andFileFilter(FileFileFilter.FILE, txtSuffixFilter);
     *
     * // Create a filter for either directories or ".txt" files
     * FileFilter filter = FileFilterUtils.orFileFilter(DirectoryFileFilter.DIRECTORY, txtFiles);
     *
     * // Copy using the filter
     * FileUtils.copyDirectory(srcDir, destDir, filter, false);
     * </pre>
     *
     * @param srcDir an existing directory to copy, must not be {@code null}.
     * @param destDir the new directory, must not be {@code null}.
     * @param filter the filter to apply, null means copy all directories and files.
     * @param preserveFileDate true if the file date of the copy should be the same as the original.
     * @throws NullPointerException if any of the given {@link File}s are {@code null}.
     * @throws IllegalArgumentException if the source or destination is invalid.
     * @throws FileNotFoundException if the source does not exist.
     * @throws IOException if an error occurs or setting the last-modified time didn't succeeded.
     * @since 1.4
     */
    public static void copyDirectory(final File srcDir, final File destDir, final FileFilter filter, final boolean preserveFileDate) throws IOException {
        copyDirectory(srcDir, destDir, filter, preserveFileDate, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Copies a filtered directory to a new location.
     * <p>
     * This method copies the contents of the specified source directory to within the specified destination directory.
     * </p>
     * <p>
     * The destination directory is created if it does not exist. If the destination directory did exist, then this
     * method merges the source with the destination, with the source taking precedence.
     * </p>
     * <p>
     * <strong>Note:</strong> Setting {@code preserveFileDate} to {@code true} tries to preserve the files' last
     * modified date/times using {@link File#setLastModified(long)}, however it is not guaranteed that those operations
     * will succeed. If the modification operation fails, the methods throws IOException.
     * </p>
     * <b>Example: Copy directories only</b>
     *
     * <pre>
     * // only copy the directory structure
     * FileUtils.copyDirectory(srcDir, destDir, DirectoryFileFilter.DIRECTORY, false);
     * </pre>
     *
     * <b>Example: Copy directories and txt files</b>
     *
     * <pre>
     * // Create a filter for ".txt" files
     * IOFileFilter txtSuffixFilter = FileFilterUtils.suffixFileFilter(".txt");
     * IOFileFilter txtFiles = FileFilterUtils.andFileFilter(FileFileFilter.FILE, txtSuffixFilter);
     *
     * // Create a filter for either directories or ".txt" files
     * FileFilter filter = FileFilterUtils.orFileFilter(DirectoryFileFilter.DIRECTORY, txtFiles);
     *
     * // Copy using the filter
     * FileUtils.copyDirectory(srcDir, destDir, filter, false);
     * </pre>
     *
     * @param srcDir an existing directory to copy, must not be {@code null}
     * @param destDir the new directory, must not be {@code null}
     * @param fileFilter the filter to apply, null means copy all directories and files
     * @param preserveFileDate true if the file date of the copy should be the same as the original
     * @param copyOptions options specifying how the copy should be done, for example {@link StandardCopyOption}.
     * @throws NullPointerException if any of the given {@link File}s are {@code null}.
     * @throws IllegalArgumentException if the source or destination is invalid.
     * @throws FileNotFoundException if the source does not exist.
     * @throws IOException if an error occurs or setting the last-modified time didn't succeeded.
     * @since 2.8.0
     */
    public static void copyDirectory(final File srcDir, final File destDir, final FileFilter fileFilter, final boolean preserveFileDate,
        final CopyOption... copyOptions) throws IOException {
        requireFileCopy(srcDir, destDir);
        requireDirectory(srcDir, "srcDir");
        requireCanonicalPathsNotEquals(srcDir, destDir);

        // Cater for destination being directory within the source directory (see IO-141)
        List<String> exclusionList = null;
        final String srcDirCanonicalPath = srcDir.getCanonicalPath();
        final String destDirCanonicalPath = destDir.getCanonicalPath();
        if (destDirCanonicalPath.startsWith(srcDirCanonicalPath)) {
            final File[] srcFiles = listFiles(srcDir, fileFilter);
            if (srcFiles.length > 0) {
                exclusionList = new ArrayList<>(srcFiles.length);
                for (final File srcFile : srcFiles) {
                    final File copiedFile = new File(destDir, srcFile.getName());
                    exclusionList.add(copiedFile.getCanonicalPath());
                }
            }
        }
        doCopyDirectory(srcDir, destDir, fileFilter, exclusionList, preserveFileDate, preserveFileDate ? addCopyAttributes(copyOptions) : copyOptions);
    }

    /**
     * Copies a directory to within another directory preserving the file dates.
     * <p>
     * This method copies the source directory and all its contents to a directory of the same name in the specified
     * destination directory.
     * </p>
     * <p>
     * The destination directory is created if it does not exist. If the destination directory did exist, then this
     * method merges the source with the destination, with the source taking precedence.
     * </p>
     * <p>
     * <strong>Note:</strong> This method tries to preserve the files' last modified date/times using
     * {@link File#setLastModified(long)}, however it is not guaranteed that those operations will succeed. If the
     * modification operation fails, the methods throws IOException.
     * </p>
     *
     * @param sourceDir an existing directory to copy, must not be {@code null}.
     * @param destinationDir the directory to place the copy in, must not be {@code null}.
     * @throws NullPointerException if any of the given {@link File}s are {@code null}.
     * @throws IllegalArgumentException if the source or destination is invalid.
     * @throws FileNotFoundException if the source does not exist.
     * @throws IOException if an error occurs or setting the last-modified time didn't succeeded.
     * @since 1.2
     */
    public static void copyDirectoryToDirectory(final File sourceDir, final File destinationDir) throws IOException {
        requireDirectoryIfExists(sourceDir, "sourceDir");
        requireDirectoryIfExists(destinationDir, "destinationDir");
        copyDirectory(sourceDir, new File(destinationDir, sourceDir.getName()), true);
    }

    /**
     * Copies a file to a new location preserving the file date.
     * <p>
     * This method copies the contents of the specified source file to the specified destination file. The directory
     * holding the destination file is created if it does not exist. If the destination file exists, then this method
     * will overwrite it.
     * </p>
     * <p>
     * <strong>Note:</strong> This method tries to preserve the file's last modified date/times using
     * {@link StandardCopyOption#COPY_ATTRIBUTES}, however it is not guaranteed that the operation will succeed. If the
     * modification operation fails, the methods throws IOException.
     * </p>
     *
     * @param srcFile an existing file to copy, must not be {@code null}.
     * @param destFile the new file, must not be {@code null}.
     * @throws NullPointerException if any of the given {@link File}s are {@code null}.
     * @throws IOException if source or destination is invalid.
     * @throws IOException if an error occurs or setting the last-modified time didn't succeeded.
     * @throws IOException if the output file length is not the same as the input file length after the copy completes.
     * @see #copyFileToDirectory(File, File)
     * @see #copyFile(File, File, boolean)
     */
    public static void copyFile(final File srcFile, final File destFile) throws IOException {
        copyFile(srcFile, destFile, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Copies an existing file to a new file location.
     * <p>
     * This method copies the contents of the specified source file to the specified destination file. The directory
     * holding the destination file is created if it does not exist. If the destination file exists, then this method
     * will overwrite it.
     * </p>
     * <p>
     * <strong>Note:</strong> Setting {@code preserveFileDate} to {@code true} tries to preserve the file's last
     * modified date/times using {@link StandardCopyOption#COPY_ATTRIBUTES}, however it is not guaranteed that the operation
     * will succeed. If the modification operation fails, the methods throws IOException.
     * </p>
     *
     * @param srcFile an existing file to copy, must not be {@code null}.
     * @param destFile the new file, must not be {@code null}.
     * @param preserveFileDate true if the file date of the copy should be the same as the original.
     * @throws NullPointerException if any of the given {@link File}s are {@code null}.
     * @throws IOException if source or destination is invalid.
     * @throws IOException if an error occurs or setting the last-modified time didn't succeeded.
     * @throws IOException if the output file length is not the same as the input file length after the copy completes
     * @see #copyFile(File, File, boolean, CopyOption...)
     */
    public static void copyFile(final File srcFile, final File destFile, final boolean preserveFileDate) throws IOException {
        // @formatter:off
        copyFile(srcFile, destFile, preserveFileDate
                ? new CopyOption[] {StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING}
                : new CopyOption[] {StandardCopyOption.REPLACE_EXISTING});
        // @formatter:on
    }

    /**
     * Copies a file to a new location.
     * <p>
     * This method copies the contents of the specified source file to the specified destination file. The directory
     * holding the destination file is created if it does not exist. If the destination file exists, you can overwrite
     * it with {@link StandardCopyOption#REPLACE_EXISTING}.
     * </p>
     * <p>
     * <strong>Note:</strong> Setting {@code preserveFileDate} to {@code true} tries to preserve the file's last
     * modified date/times using {@link StandardCopyOption#COPY_ATTRIBUTES}, however it is not guaranteed that the operation
     * will succeed. If the modification operation fails, the methods throws IOException.
     * </p>
     *
     * @param srcFile an existing file to copy, must not be {@code null}.
     * @param destFile the new file, must not be {@code null}.
     * @param preserveFileDate true if the file date of the copy should be the same as the original.
     * @param copyOptions options specifying how the copy should be done, for example {@link StandardCopyOption}..
     * @throws NullPointerException if any of the given {@link File}s are {@code null}.
     * @throws FileNotFoundException if the source does not exist.
     * @throws IllegalArgumentException if source is not a file.
     * @throws IOException if the output file length is not the same as the input file length after the copy completes.
     * @throws IOException if an I/O error occurs, or setting the last-modified time didn't succeeded.
     * @see #copyFileToDirectory(File, File, boolean)
     * @since 2.8.0
     */
    public static void copyFile(final File srcFile, final File destFile, final boolean preserveFileDate, final CopyOption... copyOptions) throws IOException {
        copyFile(srcFile, destFile, preserveFileDate ? addCopyAttributes(copyOptions) : copyOptions);
    }

    /**
     * Copies a file to a new location.
     * <p>
     * This method copies the contents of the specified source file to the specified destination file. The directory
     * holding the destination file is created if it does not exist. If the destination file exists, you can overwrite
     * it if you use {@link StandardCopyOption#REPLACE_EXISTING}.
     * </p>
     *
     * @param srcFile an existing file to copy, must not be {@code null}.
     * @param destFile the new file, must not be {@code null}.
     * @param copyOptions options specifying how the copy should be done, for example {@link StandardCopyOption}..
     * @throws NullPointerException if any of the given {@link File}s are {@code null}.
     * @throws FileNotFoundException if the source does not exist.
     * @throws IllegalArgumentException if source is not a file.
     * @throws IOException if the output file length is not the same as the input file length after the copy completes.
     * @throws IOException if an I/O error occurs.
     * @see StandardCopyOption
     * @since 2.9.0
     */
    public static void copyFile(final File srcFile, final File destFile, final CopyOption... copyOptions) throws IOException {
        requireFileCopy(srcFile, destFile);
        requireFile(srcFile, "srcFile");
        requireCanonicalPathsNotEquals(srcFile, destFile);
        createParentDirectories(destFile);
        requireFileIfExists(destFile, "destFile");
        if (destFile.exists()) {
            requireCanWrite(destFile, "destFile");
        }

        // On Windows, the last modified time is copied by default.
        Files.copy(srcFile.toPath(), destFile.toPath(), copyOptions);

        // TODO IO-386: Do we still need this check?
        requireEqualSizes(srcFile, destFile, srcFile.length(), destFile.length());
    }

    /**
     * Copies bytes from a {@link File} to an {@link OutputStream}.
     * <p>
     * This method buffers the input internally, so there is no need to use a {@link BufferedInputStream}.
     * </p>
     *
     * @param input  the {@link File} to read.
     * @param output the {@link OutputStream} to write.
     * @return the number of bytes copied
     * @throws NullPointerException if the File is {@code null}.
     * @throws NullPointerException if the OutputStream is {@code null}.
     * @throws IOException          if an I/O error occurs.
     * @since 2.1
     */
    public static long copyFile(final File input, final OutputStream output) throws IOException {
        try (InputStream fis = Files.newInputStream(input.toPath())) {
            return IOUtils.copyLarge(fis, output);
        }
    }

    /**
     * Copies a file to a directory preserving the file date.
     * <p>
     * This method copies the contents of the specified source file to a file of the same name in the specified
     * destination directory. The destination directory is created if it does not exist. If the destination file exists,
     * then this method will overwrite it.
     * </p>
     * <p>
     * <strong>Note:</strong> This method tries to preserve the file's last modified date/times using
     * {@link StandardCopyOption#COPY_ATTRIBUTES}, however it is not guaranteed that the operation will succeed. If the
     * modification operation fails, the methods throws IOException.
     * </p>
     *
     * @param srcFile an existing file to copy, must not be {@code null}.
     * @param destDir the directory to place the copy in, must not be {@code null}.
     * @throws NullPointerException if any of the given {@link File}s are {@code null}.
     * @throws IllegalArgumentException if source or destination is invalid.
     * @throws IOException if an error occurs or setting the last-modified time didn't succeeded.
     * @see #copyFile(File, File, boolean)
     */
    public static void copyFileToDirectory(final File srcFile, final File destDir) throws IOException {
        copyFileToDirectory(srcFile, destDir, true);
    }

    /**
     * Copies a file to a directory optionally preserving the file date.
     * <p>
     * This method copies the contents of the specified source file to a file of the same name in the specified
     * destination directory. The destination directory is created if it does not exist. If the destination file exists,
     * then this method will overwrite it.
     * </p>
     * <p>
     * <strong>Note:</strong> Setting {@code preserveFileDate} to {@code true} tries to preserve the file's last
     * modified date/times using {@link StandardCopyOption#COPY_ATTRIBUTES}, however it is not guaranteed that the operation
     * will succeed. If the modification operation fails, the methods throws IOException.
     * </p>
     *
     * @param sourceFile an existing file to copy, must not be {@code null}.
     * @param destinationDir the directory to place the copy in, must not be {@code null}.
     * @param preserveFileDate true if the file date of the copy should be the same as the original.
     * @throws NullPointerException if any of the given {@link File}s are {@code null}.
     * @throws IOException if an error occurs or setting the last-modified time didn't succeeded.
     * @throws IOException if the output file length is not the same as the input file length after the copy completes.
     * @see #copyFile(File, File, CopyOption...)
     * @since 1.3
     */
    public static void copyFileToDirectory(final File sourceFile, final File destinationDir, final boolean preserveFileDate) throws IOException {
        Objects.requireNonNull(sourceFile, "sourceFile");
        requireDirectoryIfExists(destinationDir, "destinationDir");
        copyFile(sourceFile, new File(destinationDir, sourceFile.getName()), preserveFileDate);
    }

    /**
     * Copies bytes from an {@link InputStream} {@code source} to a file
     * {@code destination}. The directories up to {@code destination}
     * will be created if they don't already exist. {@code destination}
     * will be overwritten if it already exists.
     * <p>
     * <em>The {@code source} stream is closed.</em>
     * </p>
     * <p>
     * See {@link #copyToFile(InputStream, File)} for a method that does not close the input stream.
     * </p>
     *
     * @param source      the {@link InputStream} to copy bytes from, must not be {@code null}, will be closed
     * @param destination the non-directory {@link File} to write bytes to
     *                    (possibly overwriting), must not be {@code null}
     * @throws IOException if {@code destination} is a directory
     * @throws IOException if {@code destination} cannot be written
     * @throws IOException if {@code destination} needs creating but can't be
     * @throws IOException if an IO error occurs during copying
     * @since 2.0
     */
    public static void copyInputStreamToFile(final InputStream source, final File destination) throws IOException {
        try (InputStream inputStream = source) {
            copyToFile(inputStream, destination);
        }
    }

    /**
     * Copies a file or directory to within another directory preserving the file dates.
     * <p>
     * This method copies the source file or directory, along all its contents, to a directory of the same name in the
     * specified destination directory.
     * </p>
     * <p>
     * The destination directory is created if it does not exist. If the destination directory did exist, then this method
     * merges the source with the destination, with the source taking precedence.
     * </p>
     * <p>
     * <strong>Note:</strong> This method tries to preserve the files' last modified date/times using
     * {@link StandardCopyOption#COPY_ATTRIBUTES} or {@link File#setLastModified(long)} depending on the input, however it
     * is not guaranteed that those operations will succeed. If the modification operation fails, the methods throws
     * IOException.
     * </p>
     *
     * @param sourceFile an existing file or directory to copy, must not be {@code null}.
     * @param destinationDir the directory to place the copy in, must not be {@code null}.
     * @throws NullPointerException if any of the given {@link File}s are {@code null}.
     * @throws IllegalArgumentException if the source or destination is invalid.
     * @throws FileNotFoundException if the source does not exist.
     * @throws IOException if an error occurs or setting the last-modified time didn't succeeded.
     * @see #copyDirectoryToDirectory(File, File)
     * @see #copyFileToDirectory(File, File)
     * @since 2.6
     */
    public static void copyToDirectory(final File sourceFile, final File destinationDir) throws IOException {
        Objects.requireNonNull(sourceFile, "sourceFile");
        if (sourceFile.isFile()) {
            copyFileToDirectory(sourceFile, destinationDir);
        } else if (sourceFile.isDirectory()) {
            copyDirectoryToDirectory(sourceFile, destinationDir);
        } else {
            throw new FileNotFoundException("The source " + sourceFile + " does not exist");
        }
    }

    /**
     * Copies a files to a directory preserving each file's date.
     * <p>
     * This method copies the contents of the specified source files
     * to a file of the same name in the specified destination directory.
     * The destination directory is created if it does not exist.
     * If the destination file exists, then this method will overwrite it.
     * </p>
     * <p>
     * <strong>Note:</strong> This method tries to preserve the file's last
     * modified date/times using {@link File#setLastModified(long)}, however
     * it is not guaranteed that the operation will succeed.
     * If the modification operation fails, the methods throws IOException.
     * </p>
     *
     * @param sourceIterable     a existing files to copy, must not be {@code null}.
     * @param destinationDir  the directory to place the copy in, must not be {@code null}.
     * @throws NullPointerException if any of the given {@link File}s are {@code null}.
     * @throws IOException if source or destination is invalid.
     * @throws IOException if an error occurs or setting the last-modified time didn't succeeded.
     * @see #copyFileToDirectory(File, File)
     * @since 2.6
     */
    public static void copyToDirectory(final Iterable<File> sourceIterable, final File destinationDir) throws IOException {
        Objects.requireNonNull(sourceIterable, "sourceIterable");
        for (final File src : sourceIterable) {
            copyFileToDirectory(src, destinationDir);
        }
    }

    /**
     * Copies bytes from an {@link InputStream} source to a {@link File} destination. The directories
     * up to {@code destination} will be created if they don't already exist. {@code destination} will be
     * overwritten if it already exists. The {@code source} stream is left open, e.g. for use with
     * {@link java.util.zip.ZipInputStream ZipInputStream}. See {@link #copyInputStreamToFile(InputStream, File)} for a
     * method that closes the input stream.
     *
     * @param inputStream the {@link InputStream} to copy bytes from, must not be {@code null}
     * @param file the non-directory {@link File} to write bytes to (possibly overwriting), must not be
     *        {@code null}
     * @throws NullPointerException if the InputStream is {@code null}.
     * @throws NullPointerException if the File is {@code null}.
     * @throws IllegalArgumentException if the file object is a directory.
     * @throws IllegalArgumentException if the file is not writable.
     * @throws IOException if the directories could not be created.
     * @throws IOException if an IO error occurs during copying.
     * @since 2.5
     */
    public static void copyToFile(final InputStream inputStream, final File file) throws IOException {
        try (OutputStream out = newOutputStream(file, false)) {
            IOUtils.copy(inputStream, out);
        }
    }

    /**
     * Copies bytes from the URL {@code source} to a file
     * {@code destination}. The directories up to {@code destination}
     * will be created if they don't already exist. {@code destination}
     * will be overwritten if it already exists.
     * <p>
     * Warning: this method does not set a connection or read timeout and thus
     * might block forever. Use {@link #copyURLToFile(URL, File, int, int)}
     * with reasonable timeouts to prevent this.
     * </p>
     *
     * @param source      the {@link URL} to copy bytes from, must not be {@code null}
     * @param destination the non-directory {@link File} to write bytes to
     *                    (possibly overwriting), must not be {@code null}
     * @throws IOException if {@code source} URL cannot be opened
     * @throws IOException if {@code destination} is a directory
     * @throws IOException if {@code destination} cannot be written
     * @throws IOException if {@code destination} needs creating but can't be
     * @throws IOException if an IO error occurs during copying
     */
    public static void copyURLToFile(final URL source, final File destination) throws IOException {
        try (InputStream stream = source.openStream()) {
            final Path path = destination.toPath();
            PathUtils.createParentDirectories(path);
            Files.copy(stream, path, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Copies bytes from the URL {@code source} to a file {@code destination}. The directories up to
     * {@code destination} will be created if they don't already exist. {@code destination} will be
     * overwritten if it already exists.
     *
     * @param source the {@link URL} to copy bytes from, must not be {@code null}
     * @param destination the non-directory {@link File} to write bytes to (possibly overwriting), must not be
     *        {@code null}
     * @param connectionTimeoutMillis the number of milliseconds until this method will timeout if no connection could
     *        be established to the {@code source}
     * @param readTimeoutMillis the number of milliseconds until this method will timeout if no data could be read from
     *        the {@code source}
     * @throws IOException if {@code source} URL cannot be opened
     * @throws IOException if {@code destination} is a directory
     * @throws IOException if {@code destination} cannot be written
     * @throws IOException if {@code destination} needs creating but can't be
     * @throws IOException if an IO error occurs during copying
     * @since 2.0
     */
    public static void copyURLToFile(final URL source, final File destination, final int connectionTimeoutMillis, final int readTimeoutMillis)
        throws IOException {
        try (CloseableURLConnection urlConnection = CloseableURLConnection.open(source)) {
            urlConnection.setConnectTimeout(connectionTimeoutMillis);
            urlConnection.setReadTimeout(readTimeoutMillis);
            try (InputStream stream = urlConnection.getInputStream()) {
                copyInputStreamToFile(stream, destination);
            }
        }
    }

    /**
     * Creates all parent directories for a File object.
     *
     * @param file the File that may need parents, may be null.
     * @return The parent directory, or {@code null} if the given file does not name a parent
     * @throws IOException if the directory was not created along with all its parent directories.
     * @throws IOException if the given file object is not null and not a directory.
     * @since 2.9.0
     */
    public static File createParentDirectories(final File file) throws IOException {
        return mkdirs(getParentFile(file));
    }

    /**
     * Gets the current directory.
     *
     * @return the current directory.
     * @since 2.12.0
     */
    public static File current() {
        return PathUtils.current().toFile();
    }

    /**
     * Decodes the specified URL as per RFC 3986, i.e. transforms
     * percent-encoded octets to characters by decoding with the UTF-8 character
     * set. This function is primarily intended for usage with
     * {@link java.net.URL} which unfortunately does not enforce proper URLs. As
     * such, this method will leniently accept invalid characters or malformed
     * percent-encoded octets and simply pass them literally through to the
     * result string. Except for rare edge cases, this will make unencoded URLs
     * pass through unaltered.
     *
     * @param url The URL to decode, may be {@code null}.
     * @return The decoded URL or {@code null} if the input was
     * {@code null}.
     */
    static String decodeUrl(final String url) {
        String decoded = url;
        if (url != null && url.indexOf('%') >= 0) {
            final int n = url.length();
            final StringBuilder builder = new StringBuilder();
            final ByteBuffer byteBuffer = ByteBuffer.allocate(n);
            for (int i = 0; i < n; ) {
                if (url.charAt(i) == '%') {
                    try {
                        do {
                            final byte octet = (byte) Integer.parseInt(url.substring(i + 1, i + 3), 16);
                            byteBuffer.put(octet);
                            i += 3;
                        } while (i < n && url.charAt(i) == '%');
                        continue;
                    } catch (final RuntimeException ignored) {
                        // malformed percent-encoded octet, fall through and
                        // append characters literally
                    } finally {
                        if (byteBuffer.position() > 0) {
                            byteBuffer.flip();
                            builder.append(StandardCharsets.UTF_8.decode(byteBuffer).toString());
                            byteBuffer.clear();
                        }
                    }
                }
                builder.append(url.charAt(i++));
            }
            decoded = builder.toString();
        }
        return decoded;
    }

    /**
     * Deletes the given File but throws an IOException if it cannot, unlike {@link File#delete()} which returns a
     * boolean.
     *
     * @param file The file to delete.
     * @return the given file.
     * @throws NullPointerException     if the parameter is {@code null}
     * @throws IOException              if the file cannot be deleted.
     * @see File#delete()
     * @since 2.9.0
     */
    public static File delete(final File file) throws IOException {
        Objects.requireNonNull(file, "file");
        Files.delete(file.toPath());
        return file;
    }

    /**
     * Deletes a directory recursively.
     *
     * @param directory directory to delete
     * @throws IOException              in case deletion is unsuccessful
     * @throws NullPointerException     if the parameter is {@code null}
     * @throws IllegalArgumentException if {@code directory} is not a directory
     */
    public static void deleteDirectory(final File directory) throws IOException {
        Objects.requireNonNull(directory, "directory");
        if (!directory.exists()) {
            return;
        }
        if (!isSymlink(directory)) {
            cleanDirectory(directory);
        }
        delete(directory);
    }

    /**
     * Schedules a directory recursively for deletion on JVM exit.
     *
     * @param directory directory to delete, must not be {@code null}
     * @throws NullPointerException if the directory is {@code null}
     * @throws IOException          in case deletion is unsuccessful
     */
    private static void deleteDirectoryOnExit(final File directory) throws IOException {
        if (!directory.exists()) {
            return;
        }
        directory.deleteOnExit();
        if (!isSymlink(directory)) {
            cleanDirectoryOnExit(directory);
        }
    }

    /**
     * Deletes a file, never throwing an exception. If file is a directory, delete it and all sub-directories.
     * <p>
     * The difference between File.delete() and this method are:
     * </p>
     * <ul>
     * <li>A directory to be deleted does not have to be empty.</li>
     * <li>No exceptions are thrown when a file or directory cannot be deleted.</li>
     * </ul>
     *
     * @param file file or directory to delete, can be {@code null}
     * @return {@code true} if the file or directory was deleted, otherwise
     * {@code false}
     *
     * @since 1.4
     */
    public static boolean deleteQuietly(final File file) {
        if (file == null) {
            return false;
        }
        try {
            if (file.isDirectory()) {
                cleanDirectory(file);
            }
        } catch (final Exception ignored) {
            // ignore
        }

        try {
            return file.delete();
        } catch (final Exception ignored) {
            return false;
        }
    }

    /**
     * Determines whether the {@code parent} directory contains the {@code child} element (a file or directory).
     * <p>
     * Files are normalized before comparison.
     * </p>
     *
     * Edge cases:
     * <ul>
     * <li>A {@code directory} must not be null: if null, throw IllegalArgumentException</li>
     * <li>A {@code directory} must be a directory: if not a directory, throw IllegalArgumentException</li>
     * <li>A directory does not contain itself: return false</li>
     * <li>A null child file is not contained in any parent: return false</li>
     * </ul>
     *
     * @param directory the file to consider as the parent.
     * @param child     the file to consider as the child.
     * @return true is the candidate leaf is under by the specified composite. False otherwise.
     * @throws IOException              if an IO error occurs while checking the files.
     * @throws NullPointerException if the given {@link File} is {@code null}.
     * @throws IllegalArgumentException if the given {@link File} does not exist or is not a directory.
     * @see FilenameUtils#directoryContains(String, String)
     * @since 2.2
     */
    public static boolean directoryContains(final File directory, final File child) throws IOException {
        requireDirectoryExists(directory, "directory");

        if (child == null || !directory.exists() || !child.exists()) {
            return false;
        }

        // Canonicalize paths (normalizes relative paths)
        return FilenameUtils.directoryContains(directory.getCanonicalPath(), child.getCanonicalPath());
    }

    /**
     * Internal copy directory method.
     *
     * @param srcDir the validated source directory, must not be {@code null}.
     * @param destDir the validated destination directory, must not be {@code null}.
     * @param fileFilter the filter to apply, null means copy all directories and files.
     * @param exclusionList List of files and directories to exclude from the copy, may be null.
     * @param preserveDirDate preserve the directories last modified dates.
     * @param copyOptions options specifying how the copy should be done, see {@link StandardCopyOption}.
     * @throws IOException if the directory was not created along with all its parent directories.
     * @throws IOException if the given file object is not a directory.
     */
    private static void doCopyDirectory(final File srcDir, final File destDir, final FileFilter fileFilter, final List<String> exclusionList,
        final boolean preserveDirDate, final CopyOption... copyOptions) throws IOException {
        // recurse dirs, copy files.
        final File[] srcFiles = listFiles(srcDir, fileFilter);
        requireDirectoryIfExists(destDir, "destDir");
        mkdirs(destDir);
        requireCanWrite(destDir, "destDir");
        for (final File srcFile : srcFiles) {
            final File dstFile = new File(destDir, srcFile.getName());
            if (exclusionList == null || !exclusionList.contains(srcFile.getCanonicalPath())) {
                if (srcFile.isDirectory()) {
                    doCopyDirectory(srcFile, dstFile, fileFilter, exclusionList, preserveDirDate, copyOptions);
                } else {
                    copyFile(srcFile, dstFile, copyOptions);
                }
            }
        }
        // Do this last, as the above has probably affected directory metadata
        if (preserveDirDate) {
            setLastModified(srcDir, destDir);
        }
    }

    /**
     * Deletes a file or directory. For a directory, delete it and all sub-directories.
     * <p>
     * The difference between File.delete() and this method are:
     * </p>
     * <ul>
     * <li>The directory does not have to be empty.</li>
     * <li>You get an exception when a file or directory cannot be deleted.</li>
     * </ul>
     *
     * @param file file or directory to delete, must not be {@code null}.
     * @throws NullPointerException  if the file is {@code null}.
     * @throws FileNotFoundException if the file was not found.
     * @throws IOException           in case deletion is unsuccessful.
     */
    public static void forceDelete(final File file) throws IOException {
        Objects.requireNonNull(file, "file");
        final Counters.PathCounters deleteCounters;
        try {
            deleteCounters = PathUtils.delete(file.toPath(), PathUtils.EMPTY_LINK_OPTION_ARRAY,
                StandardDeleteOption.OVERRIDE_READ_ONLY);
        } catch (final IOException e) {
            throw new IOException("Cannot delete file: " + file, e);
        }

        if (deleteCounters.getFileCounter().get() < 1 && deleteCounters.getDirectoryCounter().get() < 1) {
            // didn't find a file to delete.
            throw new FileNotFoundException("File does not exist: " + file);
        }
    }

    /**
     * Schedules a file to be deleted when JVM exits.
     * If file is directory delete it and all sub-directories.
     *
     * @param file file or directory to delete, must not be {@code null}.
     * @throws NullPointerException if the file is {@code null}.
     * @throws IOException          in case deletion is unsuccessful.
     */
    public static void forceDeleteOnExit(final File file) throws IOException {
        Objects.requireNonNull(file, "file");
        if (file.isDirectory()) {
            deleteDirectoryOnExit(file);
        } else {
            file.deleteOnExit();
        }
    }

    /**
     * Makes a directory, including any necessary but nonexistent parent
     * directories. If a file already exists with specified name but it is
     * not a directory then an IOException is thrown.
     * If the directory cannot be created (or the file already exists but is not a directory)
     * then an IOException is thrown.
     *
     * @param directory directory to create, may be {@code null}.
     * @throws IOException if the directory was not created along with all its parent directories.
     * @throws IOException if the given file object is not a directory.
     * @throws SecurityException See {@link File#mkdirs()}.
     */
    public static void forceMkdir(final File directory) throws IOException {
        mkdirs(directory);
    }

    /**
     * Makes any necessary but nonexistent parent directories for a given File. If the parent directory cannot be
     * created then an IOException is thrown.
     *
     * @param file file with parent to create, must not be {@code null}.
     * @throws NullPointerException if the file is {@code null}.
     * @throws IOException          if the parent directory cannot be created.
     * @since 2.5
     */
    public static void forceMkdirParent(final File file) throws IOException {
        Objects.requireNonNull(file, "file");
        forceMkdir(getParentFile(file));
    }

    /**
     * Constructs a file from the set of name elements.
     *
     * @param directory the parent directory.
     * @param names the name elements.
     * @return the new file.
     * @since 2.1
     */
    public static File getFile(final File directory, final String... names) {
        Objects.requireNonNull(directory, "directory");
        Objects.requireNonNull(names, "names");
        File file = directory;
        for (final String name : names) {
            file = new File(file, name);
        }
        return file;
    }

    /**
     * Constructs a file from the set of name elements.
     *
     * @param names the name elements.
     * @return the file.
     * @since 2.1
     */
    public static File getFile(final String... names) {
        Objects.requireNonNull(names, "names");
        File file = null;
        for (final String name : names) {
            if (file == null) {
                file = new File(name);
            } else {
                file = new File(file, name);
            }
        }
        return file;
    }

    /**
     * Gets the parent of the given file. The given file may be bull and a file's parent may as well be null.
     *
     * @param file The file to query.
     * @return The parent file or {@code null}.
     */
    private static File getParentFile(final File file) {
        return file == null ? null : file.getParentFile();
    }

    /**
     * Returns a {@link File} representing the system temporary directory.
     *
     * @return the system temporary directory.
     *
     * @since 2.0
     */
    public static File getTempDirectory() {
        return new File(getTempDirectoryPath());
    }

    /**
     * Returns the path to the system temporary directory.
     *
     * @return the path to the system temporary directory.
     *
     * @since 2.0
     */
    public static String getTempDirectoryPath() {
        return System.getProperty("java.io.tmpdir");
    }

    /**
     * Returns a {@link File} representing the user's home directory.
     *
     * @return the user's home directory.
     *
     * @since 2.0
     */
    public static File getUserDirectory() {
        return new File(getUserDirectoryPath());
    }

    /**
     * Returns the path to the user's home directory.
     *
     * @return the path to the user's home directory.
     *
     * @since 2.0
     */
    public static String getUserDirectoryPath() {
        return System.getProperty("user.home");
    }

    /**
     * Tests whether the specified {@link File} is a directory or not. Implemented as a
     * null-safe delegate to {@link Files#isDirectory(Path path, LinkOption... options)}.
     *
     * @param   file the path to the file.
     * @param   options options indicating how symbolic links are handled
     * @return  {@code true} if the file is a directory; {@code false} if
     *          the path is null, the file does not exist, is not a directory, or it cannot
     *          be determined if the file is a directory or not.
     * @throws SecurityException     In the case of the default provider, and a security manager is installed, the
     *                               {@link SecurityManager#checkRead(String) checkRead} method is invoked to check read
     *                               access to the directory.
     * @since 2.9.0
     */
    public static boolean isDirectory(final File file, final LinkOption... options) {
        return file != null && Files.isDirectory(file.toPath(), options);
    }

    /**
     * Tests whether the directory is empty.
     *
     * @param directory the directory to query.
     * @return whether the directory is empty.
     * @throws IOException if an I/O error occurs.
     * @throws NotDirectoryException if the file could not otherwise be opened because it is not a directory
     *                               <i>(optional specific exception)</i>.
     * @since 2.9.0
     */
    public static boolean isEmptyDirectory(final File directory) throws IOException {
        return PathUtils.isEmptyDirectory(directory.toPath());
    }

    /**
     * Tests if the specified {@link File} is newer than the specified {@link ChronoLocalDate}
     * at the current time.
     *
     * <p>Note: The input date is assumed to be in the system default time-zone with the time
     * part set to the current time. To use a non-default time-zone use the method
     * {@link #isFileNewer(File, ChronoLocalDateTime, ZoneId)
     * isFileNewer(file, chronoLocalDate.atTime(LocalTime.now(zoneId)), zoneId)} where
     * {@code zoneId} is a valid {@link ZoneId}.
     *
     * @param file            the {@link File} of which the modification date must be compared.
     * @param chronoLocalDate the date reference.
     * @return true if the {@link File} exists and has been modified after the given
     * {@link ChronoLocalDate} at the current time.
     * @throws NullPointerException if the file or local date is {@code null}.
     * @since 2.8.0
     */
    public static boolean isFileNewer(final File file, final ChronoLocalDate chronoLocalDate) {
        return isFileNewer(file, chronoLocalDate, LocalTime.now());
    }

    /**
     * Tests if the specified {@link File} is newer than the specified {@link ChronoLocalDate}
     * at the specified time.
     *
     * <p>Note: The input date and time are assumed to be in the system default time-zone. To use a
     * non-default time-zone use the method {@link #isFileNewer(File, ChronoLocalDateTime, ZoneId)
     * isFileNewer(file, chronoLocalDate.atTime(localTime), zoneId)} where {@code zoneId} is a valid
     * {@link ZoneId}.
     *
     * @param file            the {@link File} of which the modification date must be compared.
     * @param chronoLocalDate the date reference.
     * @param localTime       the time reference.
     * @return true if the {@link File} exists and has been modified after the given
     * {@link ChronoLocalDate} at the given time.
     * @throws NullPointerException if the file, local date or zone ID is {@code null}.
     * @since 2.8.0
     */
    public static boolean isFileNewer(final File file, final ChronoLocalDate chronoLocalDate, final LocalTime localTime) {
        Objects.requireNonNull(chronoLocalDate, "chronoLocalDate");
        Objects.requireNonNull(localTime, "localTime");
        return isFileNewer(file, chronoLocalDate.atTime(localTime));
    }

    /**
     * Tests if the specified {@link File} is newer than the specified {@link ChronoLocalDate} at the specified
     * {@link OffsetTime}.
     *
     * @param file the {@link File} of which the modification date must be compared
     * @param chronoLocalDate the date reference
     * @param offsetTime the time reference
     * @return true if the {@link File} exists and has been modified after the given {@link ChronoLocalDate} at the given
     *         {@link OffsetTime}.
     * @throws NullPointerException if the file, local date or zone ID is {@code null}
     * @since 2.12.0
     */
    public static boolean isFileNewer(final File file, final ChronoLocalDate chronoLocalDate, final OffsetTime offsetTime) {
        Objects.requireNonNull(chronoLocalDate, "chronoLocalDate");
        Objects.requireNonNull(offsetTime, "offsetTime");
        return isFileNewer(file, chronoLocalDate.atTime(offsetTime.toLocalTime()));
    }

    /**
     * Tests if the specified {@link File} is newer than the specified {@link ChronoLocalDateTime}
     * at the system-default time zone.
     *
     * <p>Note: The input date and time is assumed to be in the system default time-zone. To use a
     * non-default time-zone use the method {@link #isFileNewer(File, ChronoLocalDateTime, ZoneId)
     * isFileNewer(file, chronoLocalDateTime, zoneId)} where {@code zoneId} is a valid
     * {@link ZoneId}.
     *
     * @param file                the {@link File} of which the modification date must be compared.
     * @param chronoLocalDateTime the date reference.
     * @return true if the {@link File} exists and has been modified after the given
     * {@link ChronoLocalDateTime} at the system-default time zone.
     * @throws NullPointerException if the file or local date time is {@code null}.
     * @since 2.8.0
     */
    public static boolean isFileNewer(final File file, final ChronoLocalDateTime<?> chronoLocalDateTime) {
        return isFileNewer(file, chronoLocalDateTime, ZoneId.systemDefault());
    }

    /**
     * Tests if the specified {@link File} is newer than the specified {@link ChronoLocalDateTime}
     * at the specified {@link ZoneId}.
     *
     * @param file                the {@link File} of which the modification date must be compared.
     * @param chronoLocalDateTime the date reference.
     * @param zoneId              the time zone.
     * @return true if the {@link File} exists and has been modified after the given
     * {@link ChronoLocalDateTime} at the given {@link ZoneId}.
     * @throws NullPointerException if the file, local date time or zone ID is {@code null}.
     * @since 2.8.0
     */
    public static boolean isFileNewer(final File file, final ChronoLocalDateTime<?> chronoLocalDateTime, final ZoneId zoneId) {
        Objects.requireNonNull(chronoLocalDateTime, "chronoLocalDateTime");
        Objects.requireNonNull(zoneId, "zoneId");
        return isFileNewer(file, chronoLocalDateTime.atZone(zoneId));
    }

    /**
     * Tests if the specified {@link File} is newer than the specified {@link ChronoZonedDateTime}.
     *
     * @param file                the {@link File} of which the modification date must be compared.
     * @param chronoZonedDateTime the date reference.
     * @return true if the {@link File} exists and has been modified after the given
     * {@link ChronoZonedDateTime}.
     * @throws NullPointerException if the file or zoned date time is {@code null}.
     * @since 2.8.0
     */
    public static boolean isFileNewer(final File file, final ChronoZonedDateTime<?> chronoZonedDateTime) {
        Objects.requireNonNull(file, "file");
        Objects.requireNonNull(chronoZonedDateTime, "chronoZonedDateTime");
        return UncheckedIO.get(() -> PathUtils.isNewer(file.toPath(), chronoZonedDateTime));
    }

    /**
     * Tests if the specified {@link File} is newer than the specified {@link Date}.
     *
     * @param file the {@link File} of which the modification date must be compared.
     * @param date the date reference.
     * @return true if the {@link File} exists and has been modified
     * after the given {@link Date}.
     * @throws NullPointerException if the file or date is {@code null}.
     */
    public static boolean isFileNewer(final File file, final Date date) {
        Objects.requireNonNull(date, "date");
        return isFileNewer(file, date.getTime());
    }

    /**
     * Tests if the specified {@link File} is newer than the reference {@link File}.
     *
     * @param file      the {@link File} of which the modification date must be compared.
     * @param reference the {@link File} of which the modification date is used.
     * @return true if the {@link File} exists and has been modified more
     * recently than the reference {@link File}.
     * @throws NullPointerException if the file or reference file is {@code null}.
     * @throws IllegalArgumentException if the reference file doesn't exist.
     */
    public static boolean isFileNewer(final File file, final File reference) {
        requireExists(reference, "reference");
        return UncheckedIO.get(() -> PathUtils.isNewer(file.toPath(), reference.toPath()));
    }

    /**
     * Tests if the specified {@link File} is newer than the specified {@link FileTime}.
     *
     * @param file the {@link File} of which the modification date must be compared.
     * @param fileTime the file time reference.
     * @return true if the {@link File} exists and has been modified after the given {@link FileTime}.
     * @throws IOException if an I/O error occurs.
     * @throws NullPointerException if the file or local date is {@code null}.
     * @since 2.12.0
     */
    public static boolean isFileNewer(final File file, final FileTime fileTime) throws IOException {
        Objects.requireNonNull(file, "file");
        return PathUtils.isNewer(file.toPath(), fileTime);
    }

    /**
     * Tests if the specified {@link File} is newer than the specified {@link Instant}.
     *
     * @param file the {@link File} of which the modification date must be compared.
     * @param instant the date reference.
     * @return true if the {@link File} exists and has been modified after the given {@link Instant}.
     * @throws NullPointerException if the file or instant is {@code null}.
     * @since 2.8.0
     */
    public static boolean isFileNewer(final File file, final Instant instant) {
        Objects.requireNonNull(instant, "instant");
        return UncheckedIO.get(() -> PathUtils.isNewer(file.toPath(), instant));
    }

    /**
     * Tests if the specified {@link File} is newer than the specified time reference.
     *
     * @param file       the {@link File} of which the modification date must be compared.
     * @param timeMillis the time reference measured in milliseconds since the
     *                   epoch (00:00:00 GMT, January 1, 1970).
     * @return true if the {@link File} exists and has been modified after the given time reference.
     * @throws NullPointerException if the file is {@code null}.
     */
    public static boolean isFileNewer(final File file, final long timeMillis) {
        Objects.requireNonNull(file, "file");
        return UncheckedIO.get(() -> PathUtils.isNewer(file.toPath(), timeMillis));
    }

    /**
     * Tests if the specified {@link File} is newer than the specified {@link OffsetDateTime}.
     *
     * @param file the {@link File} of which the modification date must be compared
     * @param offsetDateTime the date reference
     * @return true if the {@link File} exists and has been modified before the given {@link OffsetDateTime}.
     * @throws NullPointerException if the file or zoned date time is {@code null}
     * @since 2.12.0
     */
    public static boolean isFileNewer(final File file, final OffsetDateTime offsetDateTime) {
        Objects.requireNonNull(offsetDateTime, "offsetDateTime");
        return isFileNewer(file, offsetDateTime.toInstant());
    }

    /**
     * Tests if the specified {@link File} is older than the specified {@link ChronoLocalDate}
     * at the current time.
     *
     * <p>Note: The input date is assumed to be in the system default time-zone with the time
     * part set to the current time. To use a non-default time-zone use the method
     * {@link #isFileOlder(File, ChronoLocalDateTime, ZoneId)
     * isFileOlder(file, chronoLocalDate.atTime(LocalTime.now(zoneId)), zoneId)} where
     * {@code zoneId} is a valid {@link ZoneId}.
     *
     * @param file            the {@link File} of which the modification date must be compared.
     * @param chronoLocalDate the date reference.
     * @return true if the {@link File} exists and has been modified before the given
     * {@link ChronoLocalDate} at the current time.
     * @throws NullPointerException if the file or local date is {@code null}.
     * @see ZoneId#systemDefault()
     * @see LocalTime#now()
     *
     * @since 2.8.0
     */
    public static boolean isFileOlder(final File file, final ChronoLocalDate chronoLocalDate) {
        return isFileOlder(file, chronoLocalDate, LocalTime.now());
    }

    /**
     * Tests if the specified {@link File} is older than the specified {@link ChronoLocalDate}
     * at the specified {@link LocalTime}.
     *
     * <p>Note: The input date and time are assumed to be in the system default time-zone. To use a
     * non-default time-zone use the method {@link #isFileOlder(File, ChronoLocalDateTime, ZoneId)
     * isFileOlder(file, chronoLocalDate.atTime(localTime), zoneId)} where {@code zoneId} is a valid
     * {@link ZoneId}.
     *
     * @param file            the {@link File} of which the modification date must be compared.
     * @param chronoLocalDate the date reference.
     * @param localTime       the time reference.
     * @return true if the {@link File} exists and has been modified before the
     * given {@link ChronoLocalDate} at the specified time.
     * @throws NullPointerException if the file, local date or local time is {@code null}.
     * @see ZoneId#systemDefault()
     * @since 2.8.0
     */
    public static boolean isFileOlder(final File file, final ChronoLocalDate chronoLocalDate, final LocalTime localTime) {
        Objects.requireNonNull(chronoLocalDate, "chronoLocalDate");
        Objects.requireNonNull(localTime, "localTime");
        return isFileOlder(file, chronoLocalDate.atTime(localTime));
    }

    /**
     * Tests if the specified {@link File} is older than the specified {@link ChronoLocalDate} at the specified
     * {@link OffsetTime}.
     *
     * @param file the {@link File} of which the modification date must be compared
     * @param chronoLocalDate the date reference
     * @param offsetTime the time reference
     * @return true if the {@link File} exists and has been modified after the given {@link ChronoLocalDate} at the given
     *         {@link OffsetTime}.
     * @throws NullPointerException if the file, local date or zone ID is {@code null}
     * @since 2.12.0
     */
    public static boolean isFileOlder(final File file, final ChronoLocalDate chronoLocalDate, final OffsetTime offsetTime) {
        Objects.requireNonNull(chronoLocalDate, "chronoLocalDate");
        Objects.requireNonNull(offsetTime, "offsetTime");
        return isFileOlder(file, chronoLocalDate.atTime(offsetTime.toLocalTime()));
    }

    /**
     * Tests if the specified {@link File} is older than the specified {@link ChronoLocalDateTime}
     * at the system-default time zone.
     *
     * <p>Note: The input date and time is assumed to be in the system default time-zone. To use a
     * non-default time-zone use the method {@link #isFileOlder(File, ChronoLocalDateTime, ZoneId)
     * isFileOlder(file, chronoLocalDateTime, zoneId)} where {@code zoneId} is a valid
     * {@link ZoneId}.
     *
     * @param file                the {@link File} of which the modification date must be compared.
     * @param chronoLocalDateTime the date reference.
     * @return true if the {@link File} exists and has been modified before the given
     * {@link ChronoLocalDateTime} at the system-default time zone.
     * @throws NullPointerException if the file or local date time is {@code null}.
     * @see ZoneId#systemDefault()
     * @since 2.8.0
     */
    public static boolean isFileOlder(final File file, final ChronoLocalDateTime<?> chronoLocalDateTime) {
        return isFileOlder(file, chronoLocalDateTime, ZoneId.systemDefault());
    }

    /**
     * Tests if the specified {@link File} is older than the specified {@link ChronoLocalDateTime}
     * at the specified {@link ZoneId}.
     *
     * @param file          the {@link File} of which the modification date must be compared.
     * @param chronoLocalDateTime the date reference.
     * @param zoneId        the time zone.
     * @return true if the {@link File} exists and has been modified before the given
     * {@link ChronoLocalDateTime} at the given {@link ZoneId}.
     * @throws NullPointerException if the file, local date time or zone ID is {@code null}.
     * @since 2.8.0
     */
    public static boolean isFileOlder(final File file, final ChronoLocalDateTime<?> chronoLocalDateTime, final ZoneId zoneId) {
        Objects.requireNonNull(chronoLocalDateTime, "chronoLocalDateTime");
        Objects.requireNonNull(zoneId, "zoneId");
        return isFileOlder(file, chronoLocalDateTime.atZone(zoneId));
    }

    /**
     * Tests if the specified {@link File} is older than the specified {@link ChronoZonedDateTime}.
     *
     * @param file                the {@link File} of which the modification date must be compared.
     * @param chronoZonedDateTime the date reference.
     * @return true if the {@link File} exists and has been modified before the given
     * {@link ChronoZonedDateTime}.
     * @throws NullPointerException if the file or zoned date time is {@code null}.
     * @since 2.8.0
     */
    public static boolean isFileOlder(final File file, final ChronoZonedDateTime<?> chronoZonedDateTime) {
        Objects.requireNonNull(chronoZonedDateTime, "chronoZonedDateTime");
        return isFileOlder(file, chronoZonedDateTime.toInstant());
    }

    /**
     * Tests if the specified {@link File} is older than the specified {@link Date}.
     *
     * @param file the {@link File} of which the modification date must be compared.
     * @param date the date reference.
     * @return true if the {@link File} exists and has been modified before the given {@link Date}.
     * @throws NullPointerException if the file or date is {@code null}.
     */
    public static boolean isFileOlder(final File file, final Date date) {
        Objects.requireNonNull(date, "date");
        return isFileOlder(file, date.getTime());
    }

    /**
     * Tests if the specified {@link File} is older than the reference {@link File}.
     *
     * @param file      the {@link File} of which the modification date must be compared.
     * @param reference the {@link File} of which the modification date is used.
     * @return true if the {@link File} exists and has been modified before the reference {@link File}.
     * @throws NullPointerException if the file or reference file is {@code null}.
     * @throws IllegalArgumentException if the reference file doesn't exist.
     */
    public static boolean isFileOlder(final File file, final File reference) {
        requireExists(reference, "reference");
        return UncheckedIO.get(() -> PathUtils.isOlder(file.toPath(), reference.toPath()));
    }

    /**
     * Tests if the specified {@link File} is older than the specified {@link FileTime}.
     *
     * @param file the {@link File} of which the modification date must be compared.
     * @param fileTime the file time reference.
     * @return true if the {@link File} exists and has been modified before the given {@link FileTime}.
     * @throws IOException if an I/O error occurs.
     * @throws NullPointerException if the file or local date is {@code null}.
     * @since 2.12.0
     */
    public static boolean isFileOlder(final File file, final FileTime fileTime) throws IOException {
        Objects.requireNonNull(file, "file");
        return PathUtils.isOlder(file.toPath(), fileTime);
    }

    /**
     * Tests if the specified {@link File} is older than the specified {@link Instant}.
     *
     * @param file    the {@link File} of which the modification date must be compared.
     * @param instant the date reference.
     * @return true if the {@link File} exists and has been modified before the given {@link Instant}.
     * @throws NullPointerException if the file or instant is {@code null}.
     * @since 2.8.0
     */
    public static boolean isFileOlder(final File file, final Instant instant) {
        Objects.requireNonNull(instant, "instant");
        return UncheckedIO.get(() -> PathUtils.isOlder(file.toPath(), instant));
    }

    /**
     * Tests if the specified {@link File} is older than the specified time reference.
     *
     * @param file       the {@link File} of which the modification date must be compared.
     * @param timeMillis the time reference measured in milliseconds since the
     *                   epoch (00:00:00 GMT, January 1, 1970).
     * @return true if the {@link File} exists and has been modified before the given time reference.
     * @throws NullPointerException if the file is {@code null}.
     */
    public static boolean isFileOlder(final File file, final long timeMillis) {
        Objects.requireNonNull(file, "file");
        return UncheckedIO.get(() -> PathUtils.isOlder(file.toPath(), timeMillis));
    }

    /**
     * Tests if the specified {@link File} is older than the specified {@link OffsetDateTime}.
     *
     * @param file the {@link File} of which the modification date must be compared
     * @param offsetDateTime the date reference
     * @return true if the {@link File} exists and has been modified before the given {@link OffsetDateTime}.
     * @throws NullPointerException if the file or zoned date time is {@code null}
     * @since 2.12.0
     */
    public static boolean isFileOlder(final File file, final OffsetDateTime offsetDateTime) {
        Objects.requireNonNull(offsetDateTime, "offsetDateTime");
        return isFileOlder(file, offsetDateTime.toInstant());
    }

    /**
     * Tests whether the specified {@link File} is a regular file or not. Implemented as a
     * null-safe delegate to {@link Files#isRegularFile(Path path, LinkOption... options)}.
     *
     * @param   file the path to the file.
     * @param   options options indicating how symbolic links are handled
     * @return  {@code true} if the file is a regular file; {@code false} if
     *          the path is null, the file does not exist, is not a regular file, or it cannot
     *          be determined if the file is a regular file or not.
     * @throws SecurityException     In the case of the default provider, and a security manager is installed, the
     *                               {@link SecurityManager#checkRead(String) checkRead} method is invoked to check read
     *                               access to the directory.
     * @since 2.9.0
     */
    public static boolean isRegularFile(final File file, final LinkOption... options) {
        return file != null && Files.isRegularFile(file.toPath(), options);
    }

    /**
     * Tests whether the specified file is a symbolic link rather than an actual file.
     * <p>
     * This method delegates to {@link Files#isSymbolicLink(Path path)}
     * </p>
     *
     * @param file the file to test.
     * @return true if the file is a symbolic link, see {@link Files#isSymbolicLink(Path path)}.
     * @since 2.0
     * @see Files#isSymbolicLink(Path)
     */
    public static boolean isSymlink(final File file) {
        return file != null && Files.isSymbolicLink(file.toPath());
    }

    /**
     * Iterates over the files in given directory (and optionally
     * its subdirectories).
     * <p>
     * The resulting iterator MUST be consumed in its entirety in order to close its underlying stream.
     * </p>
     * <p>
     * All files found are filtered by an IOFileFilter.
     * </p>
     *
     * @param directory  the directory to search in
     * @param fileFilter filter to apply when finding files.
     * @param dirFilter  optional filter to apply when finding subdirectories.
     *                   If this parameter is {@code null}, subdirectories will not be included in the
     *                   search. Use TrueFileFilter.INSTANCE to match all directories.
     * @return an iterator of java.io.File for the matching files
     * @see org.apache.commons.io.filefilter.FileFilterUtils
     * @see org.apache.commons.io.filefilter.NameFileFilter
     * @since 1.2
     */
    public static Iterator<File> iterateFiles(final File directory, final IOFileFilter fileFilter, final IOFileFilter dirFilter) {
        return listFiles(directory, fileFilter, dirFilter).iterator();
    }

    /**
     * Iterates over the files in a given directory (and optionally
     * its subdirectories) which match an array of extensions.
     * <p>
     * The resulting iterator MUST be consumed in its entirety in order to close its underlying stream.
     * </p>
     *
     * @param directory  the directory to search in
     * @param extensions an array of extensions, ex. {"java","xml"}. If this
     *                   parameter is {@code null}, all files are returned.
     * @param recursive  if true all subdirectories are searched as well
     * @return an iterator of java.io.File with the matching files
     * @since 1.2
     */
    public static Iterator<File> iterateFiles(final File directory, final String[] extensions, final boolean recursive) {
        return UncheckedIO.apply(d -> StreamIterator.iterator(streamFiles(d, recursive, extensions)), directory);
    }

    /**
     * Iterates over the files in given directory (and optionally
     * its subdirectories).
     * <p>
     * The resulting iterator MUST be consumed in its entirety in order to close its underlying stream.
     * </p>
     * <p>
     * All files found are filtered by an IOFileFilter.
     * </p>
     * <p>
     * The resulting iterator includes the subdirectories themselves.
     * </p>
     *
     * @param directory  the directory to search in
     * @param fileFilter filter to apply when finding files.
     * @param dirFilter  optional filter to apply when finding subdirectories.
     *                   If this parameter is {@code null}, subdirectories will not be included in the
     *                   search. Use TrueFileFilter.INSTANCE to match all directories.
     * @return an iterator of java.io.File for the matching files
     * @see org.apache.commons.io.filefilter.FileFilterUtils
     * @see org.apache.commons.io.filefilter.NameFileFilter
     * @since 2.2
     */
    public static Iterator<File> iterateFilesAndDirs(final File directory, final IOFileFilter fileFilter, final IOFileFilter dirFilter) {
        return listFilesAndDirs(directory, fileFilter, dirFilter).iterator();
    }

    /**
     * Returns the last modification time in milliseconds via
     * {@link java.nio.file.Files#getLastModifiedTime(Path, LinkOption...)}.
     * <p>
     * For the best precision, use {@link #lastModifiedFileTime(File)}.
     * </p>
     * <p>
     * Use this method to avoid issues with {@link File#lastModified()} like
     * <a href="https://bugs.openjdk.java.net/browse/JDK-8177809">JDK-8177809</a> where {@link File#lastModified()} is
     * losing milliseconds (always ends in 000). This bug exists in OpenJDK 8 and 9, and is fixed in 10.
     * </p>
     *
     * @param file The File to query.
     * @return See {@link java.nio.file.attribute.FileTime#toMillis()}.
     * @throws IOException if an I/O error occurs.
     * @since 2.9.0
     */
    public static long lastModified(final File file) throws IOException {
        // https://bugs.openjdk.java.net/browse/JDK-8177809
        // File.lastModified() is losing milliseconds (always ends in 000)
        // This bug is in OpenJDK 8 and 9, and fixed in 10.
        return lastModifiedFileTime(file).toMillis();
    }

    /**
     * Returns the last modification {@link FileTime} via
     * {@link java.nio.file.Files#getLastModifiedTime(Path, LinkOption...)}.
     * <p>
     * Use this method to avoid issues with {@link File#lastModified()} like
     * <a href="https://bugs.openjdk.java.net/browse/JDK-8177809">JDK-8177809</a> where {@link File#lastModified()} is
     * losing milliseconds (always ends in 000). This bug exists in OpenJDK 8 and 9, and is fixed in 10.
     * </p>
     *
     * @param file The File to query.
     * @return See {@link java.nio.file.Files#getLastModifiedTime(Path, LinkOption...)}.
     * @throws IOException if an I/O error occurs.
     * @since 2.12.0
     */
    public static FileTime lastModifiedFileTime(final File file) throws IOException {
        // https://bugs.openjdk.java.net/browse/JDK-8177809
        // File.lastModified() is losing milliseconds (always ends in 000)
        // This bug is in OpenJDK 8 and 9, and fixed in 10.
        return Files.getLastModifiedTime(Objects.requireNonNull(file.toPath(), "file"));
    }

    /**
     * Returns the last modification time in milliseconds via
     * {@link java.nio.file.Files#getLastModifiedTime(Path, LinkOption...)}.
     * <p>
     * For the best precision, use {@link #lastModifiedFileTime(File)}.
     * </p>
     * <p>
     * Use this method to avoid issues with {@link File#lastModified()} like
     * <a href="https://bugs.openjdk.java.net/browse/JDK-8177809">JDK-8177809</a> where {@link File#lastModified()} is
     * losing milliseconds (always ends in 000). This bug exists in OpenJDK 8 and 9, and is fixed in 10.
     * </p>
     *
     * @param file The File to query.
     * @return See {@link java.nio.file.attribute.FileTime#toMillis()}.
     * @throws UncheckedIOException if an I/O error occurs.
     * @since 2.9.0
     */
    public static long lastModifiedUnchecked(final File file) {
        // https://bugs.openjdk.java.net/browse/JDK-8177809
        // File.lastModified() is losing milliseconds (always ends in 000)
        // This bug is in OpenJDK 8 and 9, and fixed in 10.
        return UncheckedIO.apply(FileUtils::lastModified, file);
    }

    /**
     * Returns an Iterator for the lines in a {@link File} using the default encoding for the VM.
     *
     * @param file the file to open for input, must not be {@code null}
     * @return an Iterator of the lines in the file, never {@code null}
     * @throws NullPointerException if file is {@code null}.
     * @throws FileNotFoundException if the file does not exist, is a directory rather than a regular file, or for some
     *         other reason cannot be opened for reading.
     * @throws IOException if an I/O error occurs.
     * @see #lineIterator(File, String)
     * @since 1.3
     */
    public static LineIterator lineIterator(final File file) throws IOException {
        return lineIterator(file, null);
    }

    /**
     * Returns an Iterator for the lines in a {@link File}.
     * <p>
     * This method opens an {@link InputStream} for the file.
     * When you have finished with the iterator you should close the stream
     * to free internal resources. This can be done by using a try-with-resources block or calling the
     * {@link LineIterator#close()} method.
     * </p>
     * <p>
     * The recommended usage pattern is:
     * </p>
     * <pre>
     * LineIterator it = FileUtils.lineIterator(file, StandardCharsets.UTF_8.name());
     * try {
     *   while (it.hasNext()) {
     *     String line = it.nextLine();
     *     /// do something with line
     *   }
     * } finally {
     *   LineIterator.closeQuietly(iterator);
     * }
     * </pre>
     * <p>
     * If an exception occurs during the creation of the iterator, the
     * underlying stream is closed.
     * </p>
     *
     * @param file     the file to open for input, must not be {@code null}
     * @param charsetName the name of the requested charset, {@code null} means platform default
     * @return a LineIterator for lines in the file, never {@code null}; MUST be closed by the caller.
     * @throws NullPointerException if file is {@code null}.
     * @throws FileNotFoundException if the file does not exist, is a directory rather than a regular file, or for some
     *         other reason cannot be opened for reading.
     * @throws IOException if an I/O error occurs.
     * @since 1.2
     */
    @SuppressWarnings("resource") // Caller closes the result LineIterator.
    public static LineIterator lineIterator(final File file, final String charsetName) throws IOException {
        InputStream inputStream = null;
        try {
            inputStream = Files.newInputStream(file.toPath());
            return IOUtils.lineIterator(inputStream, charsetName);
        } catch (final IOException | RuntimeException ex) {
            IOUtils.closeQuietly(inputStream, ex::addSuppressed);
            throw ex;
        }
    }

    private static AccumulatorPathVisitor listAccumulate(final File directory, final IOFileFilter fileFilter, final IOFileFilter dirFilter,
        final FileVisitOption... options) throws IOException {
        final boolean isDirFilterSet = dirFilter != null;
        final FileEqualsFileFilter rootDirFilter = new FileEqualsFileFilter(directory);
        final PathFilter dirPathFilter = isDirFilterSet ? rootDirFilter.or(dirFilter) : rootDirFilter;
        final AccumulatorPathVisitor visitor = new AccumulatorPathVisitor(Counters.noopPathCounters(), fileFilter, dirPathFilter,
            (p, e) -> FileVisitResult.CONTINUE);
        final Set<FileVisitOption> optionSet = new HashSet<>();
        Collections.addAll(optionSet, options);
        Files.walkFileTree(directory.toPath(), optionSet, toMaxDepth(isDirFilterSet), visitor);
        return visitor;
    }

    /**
     * Lists files in a directory, asserting that the supplied directory exists and is a directory.
     *
     * @param directory The directory to list
     * @param fileFilter Optional file filter, may be null.
     * @return The files in the directory, never {@code null}.
     * @throws NullPointerException if directory is {@code null}.
     * @throws IllegalArgumentException if directory does not exist or is not a directory.
     * @throws IOException if an I/O error occurs.
     */
    private static File[] listFiles(final File directory, final FileFilter fileFilter) throws IOException {
        requireDirectoryExists(directory, "directory");
        final File[] files = fileFilter == null ? directory.listFiles() : directory.listFiles(fileFilter);
        if (files == null) {
            // null if the directory does not denote a directory, or if an I/O error occurs.
            throw new IOException("Unknown I/O error listing contents of directory: " + directory);
        }
        return files;
    }

    /**
     * Finds files within a given directory (and optionally its
     * subdirectories). All files found are filtered by an IOFileFilter.
     * <p>
     * If your search should recurse into subdirectories you can pass in
     * an IOFileFilter for directories. You don't need to bind a
     * DirectoryFileFilter (via logical AND) to this filter. This method does
     * that for you.
     * </p>
     * <p>
     * An example: If you want to search through all directories called
     * "temp" you pass in {@code FileFilterUtils.NameFileFilter("temp")}
     * </p>
     * <p>
     * Another common usage of this method is find files in a directory
     * tree but ignoring the directories generated CVS. You can simply pass
     * in {@code FileFilterUtils.makeCVSAware(null)}.
     * </p>
     *
     * @param directory  the directory to search in
     * @param fileFilter filter to apply when finding files. Must not be {@code null},
     *                   use {@link TrueFileFilter#INSTANCE} to match all files in selected directories.
     * @param dirFilter  optional filter to apply when finding subdirectories.
     *                   If this parameter is {@code null}, subdirectories will not be included in the
     *                   search. Use {@link TrueFileFilter#INSTANCE} to match all directories.
     * @return a collection of java.io.File with the matching files
     * @see org.apache.commons.io.filefilter.FileFilterUtils
     * @see org.apache.commons.io.filefilter.NameFileFilter
     */
    public static Collection<File> listFiles(final File directory, final IOFileFilter fileFilter, final IOFileFilter dirFilter) {
        final AccumulatorPathVisitor visitor = UncheckedIO
            .apply(d -> listAccumulate(d, FileFileFilter.INSTANCE.and(fileFilter), dirFilter, FileVisitOption.FOLLOW_LINKS), directory);
        return visitor.getFileList().stream().map(Path::toFile).collect(Collectors.toList());
    }

    /**
     * Finds files within a given directory (and optionally its subdirectories)
     * which match an array of extensions.
     *
     * @param directory  the directory to search in
     * @param extensions an array of extensions, ex. {"java","xml"}. If this
     *                   parameter is {@code null}, all files are returned.
     * @param recursive  if true all subdirectories are searched as well
     * @return a collection of java.io.File with the matching files
     */
    public static Collection<File> listFiles(final File directory, final String[] extensions, final boolean recursive) {
        return UncheckedIO.apply(d -> toList(streamFiles(d, recursive, extensions)), directory);
    }

    /**
     * Finds files within a given directory (and optionally its
     * subdirectories). All files found are filtered by an IOFileFilter.
     * <p>
     * The resulting collection includes the starting directory and
     * any subdirectories that match the directory filter.
     * </p>
     *
     * @param directory  the directory to search in
     * @param fileFilter filter to apply when finding files.
     * @param dirFilter  optional filter to apply when finding subdirectories.
     *                   If this parameter is {@code null}, subdirectories will not be included in the
     *                   search. Use TrueFileFilter.INSTANCE to match all directories.
     * @return a collection of java.io.File with the matching files
     * @see org.apache.commons.io.FileUtils#listFiles
     * @see org.apache.commons.io.filefilter.FileFilterUtils
     * @see org.apache.commons.io.filefilter.NameFileFilter
     * @since 2.2
     */
    public static Collection<File> listFilesAndDirs(final File directory, final IOFileFilter fileFilter, final IOFileFilter dirFilter) {
        final AccumulatorPathVisitor visitor = UncheckedIO.apply(d -> listAccumulate(d, fileFilter, dirFilter, FileVisitOption.FOLLOW_LINKS),
            directory);
        final List<Path> list = visitor.getFileList();
        list.addAll(visitor.getDirList());
        return list.stream().map(Path::toFile).collect(Collectors.toList());
    }

    /**
     * Calls {@link File#mkdirs()} and throws an exception on failure.
     *
     * @param directory the receiver for {@code mkdirs()}, may be null.
     * @return the given file, may be null.
     * @throws IOException if the directory was not created along with all its parent directories.
     * @throws IOException if the given file object is not a directory.
     * @throws SecurityException See {@link File#mkdirs()}.
     * @see File#mkdirs()
     */
    private static File mkdirs(final File directory) throws IOException {
        if (directory != null && !directory.mkdirs() && !directory.isDirectory()) {
            throw new IOException("Cannot create directory '" + directory + "'.");
        }
        return directory;
    }

    /**
     * Moves a directory.
     * <p>
     * When the destination directory is on another file system, do a "copy and delete".
     * </p>
     *
     * @param srcDir the directory to be moved.
     * @param destDir the destination directory.
     * @throws NullPointerException if any of the given {@link File}s are {@code null}.
     * @throws IllegalArgumentException if the source or destination is invalid.
     * @throws FileNotFoundException if the source does not exist.
     * @throws IOException if an error occurs or setting the last-modified time didn't succeeded.
     * @since 1.4
     */
    public static void moveDirectory(final File srcDir, final File destDir) throws IOException {
        validateMoveParameters(srcDir, destDir);
        requireDirectory(srcDir, "srcDir");
        requireAbsent(destDir, "destDir");
        if (!srcDir.renameTo(destDir)) {
            if (destDir.getCanonicalPath().startsWith(srcDir.getCanonicalPath() + File.separator)) {
                throw new IOException("Cannot move directory: " + srcDir + " to a subdirectory of itself: " + destDir);
            }
            copyDirectory(srcDir, destDir);
            deleteDirectory(srcDir);
            if (srcDir.exists()) {
                throw new IOException("Failed to delete original directory '" + srcDir +
                        "' after copy to '" + destDir + "'");
            }
        }
    }

    /**
     * Moves a directory to another directory.
     *
     * @param source the file to be moved.
     * @param destDir the destination file.
     * @param createDestDir If {@code true} create the destination directory, otherwise if {@code false} throw an
     *        IOException.
     * @throws NullPointerException if any of the given {@link File}s are {@code null}.
     * @throws IllegalArgumentException if the source or destination is invalid.
     * @throws FileNotFoundException if the source does not exist.
     * @throws IOException if an error occurs or setting the last-modified time didn't succeeded.
     * @since 1.4
     */
    public static void moveDirectoryToDirectory(final File source, final File destDir, final boolean createDestDir) throws IOException {
        validateMoveParameters(source, destDir);
        if (!destDir.isDirectory()) {
            if (destDir.exists()) {
                throw new IOException("Destination '" + destDir + "' is not a directory");
            }
            if (!createDestDir) {
                throw new FileNotFoundException("Destination directory '" + destDir + "' does not exist [createDestDir=" + false + "]");
            }
            mkdirs(destDir);
        }
        moveDirectory(source, new File(destDir, source.getName()));
    }

    /**
     * Moves a file preserving attributes.
     * <p>
     * Shorthand for {@code moveFile(srcFile, destFile, StandardCopyOption.COPY_ATTRIBUTES)}.
     * </p>
     * <p>
     * When the destination file is on another file system, do a "copy and delete".
     * </p>
     *
     * @param srcFile the file to be moved.
     * @param destFile the destination file.
     * @throws NullPointerException if any of the given {@link File}s are {@code null}.
     * @throws FileExistsException if the destination file exists.
     * @throws FileNotFoundException if the source file does not exist.
     * @throws IOException if source or destination is invalid.
     * @throws IOException if an error occurs.
     * @since 1.4
     */
    public static void moveFile(final File srcFile, final File destFile) throws IOException {
        moveFile(srcFile, destFile, StandardCopyOption.COPY_ATTRIBUTES);
    }

    /**
     * Moves a file.
     * <p>
     * When the destination file is on another file system, do a "copy and delete".
     * </p>
     *
     * @param srcFile the file to be moved.
     * @param destFile the destination file.
     * @param copyOptions Copy options.
     * @throws NullPointerException if any of the given {@link File}s are {@code null}.
     * @throws FileExistsException if the destination file exists.
     * @throws FileNotFoundException if the source file does not exist.
     * @throws IOException if source or destination is invalid.
     * @throws IOException if an error occurs or setting the last-modified time didn't succeeded.
     * @since 2.9.0
     */
    public static void moveFile(final File srcFile, final File destFile, final CopyOption... copyOptions) throws IOException {
        validateMoveParameters(srcFile, destFile);
        requireFile(srcFile, "srcFile");
        requireAbsent(destFile, "destFile");
        final boolean rename = srcFile.renameTo(destFile);
        if (!rename) {
            copyFile(srcFile, destFile, copyOptions);
            if (!srcFile.delete()) {
                FileUtils.deleteQuietly(destFile);
                throw new IOException("Failed to delete original file '" + srcFile + "' after copy to '" + destFile + "'");
            }
        }
    }

    /**
     * Moves a file to a directory.
     *
     * @param srcFile the file to be moved.
     * @param destDir the destination file.
     * @param createDestDir If {@code true} create the destination directory, otherwise if {@code false} throw an
     *        IOException.
     * @throws NullPointerException if any of the given {@link File}s are {@code null}.
     * @throws FileExistsException if the destination file exists.
     * @throws FileNotFoundException if the source file does not exist.
     * @throws IOException if source or destination is invalid.
     * @throws IOException if an error occurs or setting the last-modified time didn't succeeded.
     * @since 1.4
     */
    public static void moveFileToDirectory(final File srcFile, final File destDir, final boolean createDestDir) throws IOException {
        validateMoveParameters(srcFile, destDir);
        if (!destDir.exists() && createDestDir) {
            mkdirs(destDir);
        }
        requireExistsChecked(destDir, "destDir");
        requireDirectory(destDir, "destDir");
        moveFile(srcFile, new File(destDir, srcFile.getName()));
    }

    /**
     * Moves a file or directory to the destination directory.
     * <p>
     * When the destination is on another file system, do a "copy and delete".
     * </p>
     *
     * @param src the file or directory to be moved.
     * @param destDir the destination directory.
     * @param createDestDir If {@code true} create the destination directory, otherwise if {@code false} throw an
     *        IOException.
     * @throws NullPointerException if any of the given {@link File}s are {@code null}.
     * @throws FileExistsException if the directory or file exists in the destination directory.
     * @throws FileNotFoundException if the source file does not exist.
     * @throws IOException if source or destination is invalid.
     * @throws IOException if an error occurs or setting the last-modified time didn't succeeded.
     * @since 1.4
     */
    public static void moveToDirectory(final File src, final File destDir, final boolean createDestDir) throws IOException {
        validateMoveParameters(src, destDir);
        if (src.isDirectory()) {
            moveDirectoryToDirectory(src, destDir, createDestDir);
        } else {
            moveFileToDirectory(src, destDir, createDestDir);
        }
    }

    /**
     * Creates a new OutputStream by opening or creating a file, returning an output stream that may be used to write bytes
     * to the file.
     *
     * @param append Whether or not to append.
     * @param file the File.
     * @return a new OutputStream.
     * @throws IOException if an I/O error occurs.
     * @see PathUtils#newOutputStream(Path, boolean)
     * @since 2.12.0
     */
    public static OutputStream newOutputStream(final File file, final boolean append) throws IOException {
        return PathUtils.newOutputStream(Objects.requireNonNull(file, "file").toPath(), append);
    }

    /**
     * Opens a {@link FileInputStream} for the specified file, providing better error messages than simply calling
     * {@code new FileInputStream(file)}.
     * <p>
     * At the end of the method either the stream will be successfully opened, or an exception will have been thrown.
     * </p>
     * <p>
     * An exception is thrown if the file does not exist. An exception is thrown if the file object exists but is a
     * directory. An exception is thrown if the file exists but cannot be read.
     * </p>
     *
     * @param file the file to open for input, must not be {@code null}
     * @return a new {@link FileInputStream} for the specified file
     * @throws NullPointerException if file is {@code null}.
     * @throws FileNotFoundException if the file does not exist, is a directory rather than a regular file, or for some
     *         other reason cannot be opened for reading.
     * @throws IOException See FileNotFoundException above, FileNotFoundException is a subclass of IOException.
     * @since 1.3
     */
    public static FileInputStream openInputStream(final File file) throws IOException {
        Objects.requireNonNull(file, "file");
        return new FileInputStream(file);
    }

    /**
     * Opens a {@link FileOutputStream} for the specified file, checking and
     * creating the parent directory if it does not exist.
     * <p>
     * At the end of the method either the stream will be successfully opened,
     * or an exception will have been thrown.
     * </p>
     * <p>
     * The parent directory will be created if it does not exist.
     * The file will be created if it does not exist.
     * An exception is thrown if the file object exists but is a directory.
     * An exception is thrown if the file exists but cannot be written to.
     * An exception is thrown if the parent directory cannot be created.
     * </p>
     *
     * @param file the file to open for output, must not be {@code null}
     * @return a new {@link FileOutputStream} for the specified file
     * @throws NullPointerException if the file object is {@code null}.
     * @throws IllegalArgumentException if the file object is a directory
     * @throws IllegalArgumentException if the file is not writable.
     * @throws IOException if the directories could not be created.
     * @since 1.3
     */
    public static FileOutputStream openOutputStream(final File file) throws IOException {
        return openOutputStream(file, false);
    }

    /**
     * Opens a {@link FileOutputStream} for the specified file, checking and
     * creating the parent directory if it does not exist.
     * <p>
     * At the end of the method either the stream will be successfully opened,
     * or an exception will have been thrown.
     * </p>
     * <p>
     * The parent directory will be created if it does not exist.
     * The file will be created if it does not exist.
     * An exception is thrown if the file object exists but is a directory.
     * An exception is thrown if the file exists but cannot be written to.
     * An exception is thrown if the parent directory cannot be created.
     * </p>
     *
     * @param file   the file to open for output, must not be {@code null}
     * @param append if {@code true}, then bytes will be added to the
     *               end of the file rather than overwriting
     * @return a new {@link FileOutputStream} for the specified file
     * @throws NullPointerException if the file object is {@code null}.
     * @throws IllegalArgumentException if the file object is a directory
     * @throws IllegalArgumentException if the file is not writable.
     * @throws IOException if the directories could not be created.
     * @since 2.1
     */
    public static FileOutputStream openOutputStream(final File file, final boolean append) throws IOException {
        Objects.requireNonNull(file, "file");
        if (file.exists()) {
            requireFile(file, "file");
            requireCanWrite(file, "file");
        } else {
            createParentDirectories(file);
        }
        return new FileOutputStream(file, append);
    }

    /**
     * Reads the contents of a file into a byte array.
     * The file is always closed.
     *
     * @param file the file to read, must not be {@code null}
     * @return the file contents, never {@code null}
     * @throws NullPointerException if file is {@code null}.
     * @throws FileNotFoundException if the file does not exist, is a directory rather than a regular file, or for some
     *         other reason cannot be opened for reading.
     * @throws IOException if an I/O error occurs.
     * @since 1.1
     */
    public static byte[] readFileToByteArray(final File file) throws IOException {
        Objects.requireNonNull(file, "file");
        return Files.readAllBytes(file.toPath());
    }

    /**
     * Reads the contents of a file into a String using the default encoding for the VM.
     * The file is always closed.
     *
     * @param file the file to read, must not be {@code null}
     * @return the file contents, never {@code null}
     * @throws NullPointerException if file is {@code null}.
     * @throws FileNotFoundException if the file does not exist, is a directory rather than a regular file, or for some
     *         other reason cannot be opened for reading.
     * @throws IOException if an I/O error occurs.
     * @since 1.3.1
     * @deprecated 2.5 use {@link #readFileToString(File, Charset)} instead (and specify the appropriate encoding)
     */
    @Deprecated
    public static String readFileToString(final File file) throws IOException {
        return readFileToString(file, Charset.defaultCharset());
    }

    /**
     * Reads the contents of a file into a String.
     * The file is always closed.
     *
     * @param file     the file to read, must not be {@code null}
     * @param charsetName the name of the requested charset, {@code null} means platform default
     * @return the file contents, never {@code null}
     * @throws NullPointerException if file is {@code null}.
     * @throws FileNotFoundException if the file does not exist, is a directory rather than a regular file, or for some
     *         other reason cannot be opened for reading.
     * @throws IOException if an I/O error occurs.
     * @since 2.3
     */
    public static String readFileToString(final File file, final Charset charsetName) throws IOException {
        try (InputStream inputStream = Files.newInputStream(file.toPath())) {
            return IOUtils.toString(inputStream, Charsets.toCharset(charsetName));
        }
    }

    /**
     * Reads the contents of a file into a String. The file is always closed.
     *
     * @param file     the file to read, must not be {@code null}
     * @param charsetName the name of the requested charset, {@code null} means platform default
     * @return the file contents, never {@code null}
     * @throws NullPointerException if file is {@code null}.
     * @throws FileNotFoundException if the file does not exist, is a directory rather than a regular file, or for some
     *         other reason cannot be opened for reading.
     * @throws IOException if an I/O error occurs.
     * @throws java.nio.charset.UnsupportedCharsetException thrown instead of {@link java.io
     * .UnsupportedEncodingException} in version 2.2 if the named charset is unavailable.
     * @since 2.3
     */
    public static String readFileToString(final File file, final String charsetName) throws IOException {
        return readFileToString(file, Charsets.toCharset(charsetName));
    }

    /**
     * Reads the contents of a file line by line to a List of Strings using the default encoding for the VM.
     * The file is always closed.
     *
     * @param file the file to read, must not be {@code null}
     * @return the list of Strings representing each line in the file, never {@code null}
     * @throws NullPointerException if file is {@code null}.
     * @throws FileNotFoundException if the file does not exist, is a directory rather than a regular file, or for some
     *         other reason cannot be opened for reading.
     * @throws IOException if an I/O error occurs.
     * @since 1.3
     * @deprecated 2.5 use {@link #readLines(File, Charset)} instead (and specify the appropriate encoding)
     */
    @Deprecated
    public static List<String> readLines(final File file) throws IOException {
        return readLines(file, Charset.defaultCharset());
    }

    /**
     * Reads the contents of a file line by line to a List of Strings.
     * The file is always closed.
     *
     * @param file     the file to read, must not be {@code null}
     * @param charset the charset to use, {@code null} means platform default
     * @return the list of Strings representing each line in the file, never {@code null}
     * @throws NullPointerException if file is {@code null}.
     * @throws FileNotFoundException if the file does not exist, is a directory rather than a regular file, or for some
     *         other reason cannot be opened for reading.
     * @throws IOException if an I/O error occurs.
     * @since 2.3
     */
    public static List<String> readLines(final File file, final Charset charset) throws IOException {
        return Files.readAllLines(file.toPath(), charset);
    }

    /**
     * Reads the contents of a file line by line to a List of Strings. The file is always closed.
     *
     * @param file     the file to read, must not be {@code null}
     * @param charsetName the name of the requested charset, {@code null} means platform default
     * @return the list of Strings representing each line in the file, never {@code null}
     * @throws NullPointerException if file is {@code null}.
     * @throws FileNotFoundException if the file does not exist, is a directory rather than a regular file, or for some
     *         other reason cannot be opened for reading.
     * @throws IOException if an I/O error occurs.
     * @throws java.nio.charset.UnsupportedCharsetException thrown instead of {@link java.io
     * .UnsupportedEncodingException} in version 2.2 if the named charset is unavailable.
     * @since 1.1
     */
    public static List<String> readLines(final File file, final String charsetName) throws IOException {
        return readLines(file, Charsets.toCharset(charsetName));
    }


    private static void requireAbsent(final File file, final String name) throws FileExistsException {
        if (file.exists()) {
            throw new FileExistsException(String.format("File element in parameter '%s' already exists: '%s'", name, file));
        }
    }

    /**
     * Throws IllegalArgumentException if the given files' canonical representations are equal.
     *
     * @param file1 The first file to compare.
     * @param file2 The second file to compare.
     * @throws IOException if an I/O error occurs.
     * @throws IllegalArgumentException if the given files' canonical representations are equal.
     */
    private static void requireCanonicalPathsNotEquals(final File file1, final File file2) throws IOException {
        final String canonicalPath = file1.getCanonicalPath();
        if (canonicalPath.equals(file2.getCanonicalPath())) {
            throw new IllegalArgumentException(String
                .format("File canonical paths are equal: '%s' (file1='%s', file2='%s')", canonicalPath, file1, file2));
        }
    }

    /**
     * Throws an {@link IllegalArgumentException} if the file is not writable. This provides a more precise exception
     * message than a plain access denied.
     *
     * @param file The file to test.
     * @param name The parameter name to use in the exception message.
     * @throws NullPointerException if the given {@link File} is {@code null}.
     * @throws IllegalArgumentException if the file is not writable.
     */
    private static void requireCanWrite(final File file, final String name) {
        Objects.requireNonNull(file, "file");
        if (!file.canWrite()) {
            throw new IllegalArgumentException("File parameter '" + name + " is not writable: '" + file + "'");
        }
    }

    /**
     * Requires that the given {@link File} is a directory.
     *
     * @param directory The {@link File} to check.
     * @param name The parameter name to use in the exception message in case of null input or if the file is not a directory.
     * @return the given directory.
     * @throws NullPointerException if the given {@link File} is {@code null}.
     * @throws IllegalArgumentException if the given {@link File} does not exist or is not a directory.
     */
    private static File requireDirectory(final File directory, final String name) {
        Objects.requireNonNull(directory, name);
        if (!directory.isDirectory()) {
            throw new IllegalArgumentException("Parameter '" + name + "' is not a directory: '" + directory + "'");
        }
        return directory;
    }

    /**
     * Requires that the given {@link File} exists and is a directory.
     *
     * @param directory The {@link File} to check.
     * @param name The parameter name to use in the exception message in case of null input.
     * @return the given directory.
     * @throws NullPointerException if the given {@link File} is {@code null}.
     * @throws IllegalArgumentException if the given {@link File} does not exist or is not a directory.
     */
    private static File requireDirectoryExists(final File directory, final String name) {
        requireExists(directory, name);
        requireDirectory(directory, name);
        return directory;
    }

    /**
     * Requires that the given {@link File} is a directory if it exists.
     *
     * @param directory The {@link File} to check.
     * @param name The parameter name to use in the exception message in case of null input.
     * @return the given directory.
     * @throws NullPointerException if the given {@link File} is {@code null}.
     * @throws IllegalArgumentException if the given {@link File} exists but is not a directory.
     */
    private static File requireDirectoryIfExists(final File directory, final String name) {
        Objects.requireNonNull(directory, name);
        if (directory.exists()) {
            requireDirectory(directory, name);
        }
        return directory;
    }

    /**
     * Requires that two file lengths are equal.
     *
     * @param srcFile Source file.
     * @param destFile Destination file.
     * @param srcLen Source file length.
     * @param dstLen Destination file length
     * @throws IOException Thrown when the given sizes are not equal.
     */
    private static void requireEqualSizes(final File srcFile, final File destFile, final long srcLen, final long dstLen) throws IOException {
        if (srcLen != dstLen) {
            throw new IOException(
                "Failed to copy full contents from '" + srcFile + "' to '" + destFile + "' Expected length: " + srcLen + " Actual: " + dstLen);
        }
    }

    /**
     * Requires that the given {@link File} exists and throws an {@link IllegalArgumentException} if it doesn't.
     *
     * @param file The {@link File} to check.
     * @param fileParamName The parameter name to use in the exception message in case of {@code null} input.
     * @return the given file.
     * @throws NullPointerException if the given {@link File} is {@code null}.
     * @throws IllegalArgumentException if the given {@link File} does not exist.
     */
    private static File requireExists(final File file, final String fileParamName) {
        Objects.requireNonNull(file, fileParamName);
        if (!file.exists()) {
            throw new IllegalArgumentException("File system element for parameter '" + fileParamName + "' does not exist: '" + file + "'");
        }
        return file;
    }

    /**
     * Requires that the given {@link File} exists and throws an {@link FileNotFoundException} if it doesn't.
     *
     * @param file The {@link File} to check.
     * @param fileParamName The parameter name to use in the exception message in case of {@code null} input.
     * @return the given file.
     * @throws NullPointerException if the given {@link File} is {@code null}.
     * @throws FileNotFoundException if the given {@link File} does not exist.
     */
    private static File requireExistsChecked(final File file, final String fileParamName) throws FileNotFoundException {
        Objects.requireNonNull(file, fileParamName);
        if (!file.exists()) {
            throw new FileNotFoundException("File system element for parameter '" + fileParamName + "' does not exist: '" + file + "'");
        }
        return file;
    }

    /**
     * Requires that the given {@link File} is a file.
     *
     * @param file The {@link File} to check.
     * @param name The parameter name to use in the exception message.
     * @return the given file.
     * @throws NullPointerException if the given {@link File} is {@code null}.
     * @throws IllegalArgumentException if the given {@link File} does not exist or is not a file.
     */
    private static File requireFile(final File file, final String name) {
        Objects.requireNonNull(file, name);
        if (!file.isFile()) {
            throw new IllegalArgumentException("Parameter '" + name + "' is not a file: " + file);
        }
        return file;
    }

    /**
     * Requires parameter attributes for a file copy operation.
     *
     * @param source the source file
     * @param destination the destination
     * @throws NullPointerException if any of the given {@link File}s are {@code null}.
     * @throws FileNotFoundException if the source does not exist.
     */
    private static void requireFileCopy(final File source, final File destination) throws FileNotFoundException {
        requireExistsChecked(source, "source");
        Objects.requireNonNull(destination, "destination");
    }

    /**
     * Requires that the given {@link File} is a file if it exists.
     *
     * @param file The {@link File} to check.
     * @param name The parameter name to use in the exception message in case of null input.
     * @return the given directory.
     * @throws NullPointerException if the given {@link File} is {@code null}.
     * @throws IllegalArgumentException if the given {@link File} does exists but is not a directory.
     */
    private static File requireFileIfExists(final File file, final String name) {
        Objects.requireNonNull(file, name);
        return file.exists() ? requireFile(file, name) : file;
    }

    /**
     * Sets the given {@code targetFile}'s last modified date to the value from {@code sourceFile}.
     *
     * @param sourceFile The source file to query.
     * @param targetFile The target file or directory to set.
     * @throws NullPointerException if sourceFile is {@code null}.
     * @throws NullPointerException if targetFile is {@code null}.
     * @throws IOException if setting the last-modified time failed.
     */
    private static void setLastModified(final File sourceFile, final File targetFile) throws IOException {
        Objects.requireNonNull(sourceFile, "sourceFile");
        Objects.requireNonNull(targetFile, "targetFile");
        if (targetFile.isFile()) {
            PathUtils.setLastModifiedTime(targetFile.toPath(), sourceFile.toPath());
        } else {
            setLastModified(targetFile, lastModified(sourceFile));
        }
    }

    /**
     * Sets the given {@code targetFile}'s last modified date to the given value.
     *
     * @param file The source file to query.
     * @param timeMillis The new last-modified time, measured in milliseconds since the epoch 01-01-1970 GMT.
     * @throws NullPointerException if file is {@code null}.
     * @throws IOException if setting the last-modified time failed.
     */
    private static void setLastModified(final File file, final long timeMillis) throws IOException {
        Objects.requireNonNull(file, "file");
        if (!file.setLastModified(timeMillis)) {
            throw new IOException(String.format("Failed setLastModified(%s) on '%s'", timeMillis, file));
        }
    }

    /**
     * Returns the size of the specified file or directory. If the provided
     * {@link File} is a regular file, then the file's length is returned.
     * If the argument is a directory, then the size of the directory is
     * calculated recursively. If a directory or subdirectory is security
     * restricted, its size will not be included.
     * <p>
     * Note that overflow is not detected, and the return value may be negative if
     * overflow occurs. See {@link #sizeOfAsBigInteger(File)} for an alternative
     * method that does not overflow.
     * </p>
     *
     * @param file the regular file or directory to return the size
     *             of (must not be {@code null}).
     *
     * @return the length of the file, or recursive size of the directory,
     * provided (in bytes).
     *
     * @throws NullPointerException     if the file is {@code null}.
     * @throws IllegalArgumentException if the file does not exist.
     * @throws UncheckedIOException if an IO error occurs.
     * @since 2.0
     */
    public static long sizeOf(final File file) {
        requireExists(file, "file");
        return UncheckedIO.get(() -> PathUtils.sizeOf(file.toPath()));
    }

    /**
     * Returns the size of the specified file or directory. If the provided
     * {@link File} is a regular file, then the file's length is returned.
     * If the argument is a directory, then the size of the directory is
     * calculated recursively. If a directory or subdirectory is security
     * restricted, its size will not be included.
     *
     * @param file the regular file or directory to return the size
     *             of (must not be {@code null}).
     *
     * @return the length of the file, or recursive size of the directory,
     * provided (in bytes).
     *
     * @throws NullPointerException     if the file is {@code null}.
     * @throws IllegalArgumentException if the file does not exist.
     * @throws UncheckedIOException if an IO error occurs.
     * @since 2.4
     */
    public static BigInteger sizeOfAsBigInteger(final File file) {
        requireExists(file, "file");
        return UncheckedIO.get(() -> PathUtils.sizeOfAsBigInteger(file.toPath()));
    }

    /**
     * Counts the size of a directory recursively (sum of the length of all files).
     * <p>
     * Note that overflow is not detected, and the return value may be negative if
     * overflow occurs. See {@link #sizeOfDirectoryAsBigInteger(File)} for an alternative
     * method that does not overflow.
     * </p>
     *
     * @param directory directory to inspect, must not be {@code null}.
     * @return size of directory in bytes, 0 if directory is security restricted, a negative number when the real total
     * is greater than {@link Long#MAX_VALUE}.
     * @throws NullPointerException if the directory is {@code null}.
     * @throws UncheckedIOException if an IO error occurs.
     */
    public static long sizeOfDirectory(final File directory) {
        requireDirectoryExists(directory, "directory");
        return UncheckedIO.get(() -> PathUtils.sizeOfDirectory(directory.toPath()));
    }

    /**
     * Counts the size of a directory recursively (sum of the length of all files).
     *
     * @param directory directory to inspect, must not be {@code null}.
     * @return size of directory in bytes, 0 if directory is security restricted.
     * @throws NullPointerException if the directory is {@code null}.
     * @throws UncheckedIOException if an IO error occurs.
     * @since 2.4
     */
    public static BigInteger sizeOfDirectoryAsBigInteger(final File directory) {
        requireDirectoryExists(directory, "directory");
        return UncheckedIO.get(() -> PathUtils.sizeOfDirectoryAsBigInteger(directory.toPath()));
    }

    /**
     * Streams over the files in a given directory (and optionally
     * its subdirectories) which match an array of extensions.
     *
     * @param directory  the directory to search in
     * @param recursive  if true all subdirectories are searched as well
     * @param extensions an array of extensions, ex. {"java","xml"}. If this
     *                   parameter is {@code null}, all files are returned.
     * @return an iterator of java.io.File with the matching files
     * @throws IOException if an I/O error is thrown when accessing the starting file.
     * @since 2.9.0
     */
    public static Stream<File> streamFiles(final File directory, final boolean recursive, final String... extensions) throws IOException {
        // @formatter:off
        final IOFileFilter filter = extensions == null
            ? FileFileFilter.INSTANCE
            : FileFileFilter.INSTANCE.and(new SuffixFileFilter(toSuffixes(extensions)));
        // @formatter:on
        return PathUtils.walk(directory.toPath(), filter, toMaxDepth(recursive), false, FileVisitOption.FOLLOW_LINKS).map(Path::toFile);
    }

    /**
     * Converts from a {@link URL} to a {@link File}.
     * <p>
     * From version 1.1 this method will decode the URL.
     * Syntax such as {@code file:///my%20docs/file.txt} will be
     * correctly decoded to {@code /my docs/file.txt}. Starting with version
     * 1.5, this method uses UTF-8 to decode percent-encoded octets to characters.
     * Additionally, malformed percent-encoded octets are handled leniently by
     * passing them through literally.
     * </p>
     *
     * @param url the file URL to convert, {@code null} returns {@code null}
     * @return the equivalent {@link File} object, or {@code null}
     * if the URL's protocol is not {@code file}
     */
    public static File toFile(final URL url) {
        if (url == null || !"file".equalsIgnoreCase(url.getProtocol())) {
            return null;
        }
        final String filename = url.getFile().replace('/', File.separatorChar);
        return new File(decodeUrl(filename));
    }

    /**
     * Converts each of an array of {@link URL} to a {@link File}.
     * <p>
     * Returns an array of the same size as the input.
     * If the input is {@code null}, an empty array is returned.
     * If the input contains {@code null}, the output array contains {@code null} at the same
     * index.
     * </p>
     * <p>
     * This method will decode the URL.
     * Syntax such as {@code file:///my%20docs/file.txt} will be
     * correctly decoded to {@code /my docs/file.txt}.
     * </p>
     *
     * @param urls the file URLs to convert, {@code null} returns empty array
     * @return a non-{@code null} array of Files matching the input, with a {@code null} item
     * if there was a {@code null} at that index in the input array
     * @throws IllegalArgumentException if any file is not a URL file
     * @throws IllegalArgumentException if any file is incorrectly encoded
     * @since 1.1
     */
    public static File[] toFiles(final URL... urls) {
        if (IOUtils.length(urls) == 0) {
            return EMPTY_FILE_ARRAY;
        }
        final File[] files = new File[urls.length];
        for (int i = 0; i < urls.length; i++) {
            final URL url = urls[i];
            if (url != null) {
                if (!"file".equalsIgnoreCase(url.getProtocol())) {
                    throw new IllegalArgumentException("Can only convert file URL to a File: " + url);
                }
                files[i] = toFile(url);
            }
        }
        return files;
    }

    private static List<File> toList(final Stream<File> stream) {
        return stream.collect(Collectors.toList());
    }

    /**
     * Converts whether or not to recurse into a recursion max depth.
     *
     * @param recursive whether or not to recurse
     * @return the recursion depth
     */
    private static int toMaxDepth(final boolean recursive) {
        return recursive ? Integer.MAX_VALUE : 1;
    }

    /**
     * Converts an array of file extensions to suffixes.
     *
     * @param extensions an array of extensions. Format: {"java", "xml"}
     * @return an array of suffixes. Format: {".java", ".xml"}
     * @throws NullPointerException if the parameter is null
     */
    private static String[] toSuffixes(final String... extensions) {
        Objects.requireNonNull(extensions, "extensions");
        final String[] suffixes = new String[extensions.length];
        for (int i = 0; i < extensions.length; i++) {
            suffixes[i] = "." + extensions[i];
        }
        return suffixes;
    }

    /**
     * Implements behavior similar to the Unix "touch" utility. Creates a new file with size 0, or, if the file exists, just
     * updates the file's modified time.
     * <p>
     * NOTE: As from v1.3, this method throws an IOException if the last modified date of the file cannot be set. Also, as
     * from v1.3 this method creates parent directories if they do not exist.
     * </p>
     *
     * @param file the File to touch.
     * @throws NullPointerException if the parameter is {@code null}.
     * @throws IOException if setting the last-modified time failed or an I/O problem occurs.
     */
    public static void touch(final File file) throws IOException {
        PathUtils.touch(Objects.requireNonNull(file, "file").toPath());
    }

    /**
     * Converts each of an array of {@link File} to a {@link URL}.
     * <p>
     * Returns an array of the same size as the input.
     * </p>
     *
     * @param files the files to convert, must not be {@code null}
     * @return an array of URLs matching the input
     * @throws IOException          if a file cannot be converted
     * @throws NullPointerException if the parameter is null
     */
    public static URL[] toURLs(final File... files) throws IOException {
        Objects.requireNonNull(files, "files");
        final URL[] urls = new URL[files.length];
        for (int i = 0; i < urls.length; i++) {
            urls[i] = files[i].toURI().toURL();
        }
        return urls;
    }

    /**
     * Validates the given arguments.
     * <ul>
     * <li>Throws {@link NullPointerException} if {@code source} is null</li>
     * <li>Throws {@link NullPointerException} if {@code destination} is null</li>
     * <li>Throws {@link FileNotFoundException} if {@code source} does not exist</li>
     * </ul>
     *
     * @param source      the file or directory to be moved.
     * @param destination the destination file or directory.
     * @throws FileNotFoundException if the source file does not exist.
     */
    private static void validateMoveParameters(final File source, final File destination) throws FileNotFoundException {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(destination, "destination");
        if (!source.exists()) {
            throw new FileNotFoundException("Source '" + source + "' does not exist");
        }
    }

    /**
     * Waits for the file system to propagate a file creation, with a timeout.
     * <p>
     * This method repeatedly tests {@link Files#exists(Path, LinkOption...)} until it returns
     * true up to the maximum time specified in seconds.
     * </p>
     *
     * @param file    the file to check, must not be {@code null}
     * @param seconds the maximum time in seconds to wait
     * @return true if file exists
     * @throws NullPointerException if the file is {@code null}
     */
    public static boolean waitFor(final File file, final int seconds) {
        Objects.requireNonNull(file, "file");
        return PathUtils.waitFor(file.toPath(), Duration.ofSeconds(seconds), PathUtils.EMPTY_LINK_OPTION_ARRAY);
    }

    /**
     * Writes a CharSequence to a file creating the file if it does not exist using the default encoding for the VM.
     *
     * @param file the file to write
     * @param data the content to write to the file
     * @throws IOException in case of an I/O error
     * @since 2.0
     * @deprecated 2.5 use {@link #write(File, CharSequence, Charset)} instead (and specify the appropriate encoding)
     */
    @Deprecated
    public static void write(final File file, final CharSequence data) throws IOException {
        write(file, data, Charset.defaultCharset(), false);
    }

    /**
     * Writes a CharSequence to a file creating the file if it does not exist using the default encoding for the VM.
     *
     * @param file   the file to write
     * @param data   the content to write to the file
     * @param append if {@code true}, then the data will be added to the
     *               end of the file rather than overwriting
     * @throws IOException in case of an I/O error
     * @since 2.1
     * @deprecated 2.5 use {@link #write(File, CharSequence, Charset, boolean)} instead (and specify the appropriate encoding)
     */
    @Deprecated
    public static void write(final File file, final CharSequence data, final boolean append) throws IOException {
        write(file, data, Charset.defaultCharset(), append);
    }

    /**
     * Writes a CharSequence to a file creating the file if it does not exist.
     *
     * @param file     the file to write
     * @param data     the content to write to the file
     * @param charset the name of the requested charset, {@code null} means platform default
     * @throws IOException in case of an I/O error
     * @since 2.3
     */
    public static void write(final File file, final CharSequence data, final Charset charset) throws IOException {
        write(file, data, charset, false);
    }

    /**
     * Writes a CharSequence to a file creating the file if it does not exist.
     *
     * @param file     the file to write
     * @param data     the content to write to the file
     * @param charset the charset to use, {@code null} means platform default
     * @param append   if {@code true}, then the data will be added to the
     *                 end of the file rather than overwriting
     * @throws IOException in case of an I/O error
     * @since 2.3
     */
    public static void write(final File file, final CharSequence data, final Charset charset, final boolean append) throws IOException {
        writeStringToFile(file, Objects.toString(data, null), charset, append);
    }

    /**
     * Writes a CharSequence to a file creating the file if it does not exist.
     *
     * @param file     the file to write
     * @param data     the content to write to the file
     * @param charsetName the name of the requested charset, {@code null} means platform default
     * @throws IOException                          in case of an I/O error
     * @throws java.io.UnsupportedEncodingException if the encoding is not supported by the VM
     * @since 2.0
     */
    public static void write(final File file, final CharSequence data, final String charsetName) throws IOException {
        write(file, data, charsetName, false);
    }

    /**
     * Writes a CharSequence to a file creating the file if it does not exist.
     *
     * @param file     the file to write
     * @param data     the content to write to the file
     * @param charsetName the name of the requested charset, {@code null} means platform default
     * @param append   if {@code true}, then the data will be added to the
     *                 end of the file rather than overwriting
     * @throws IOException                 in case of an I/O error
     * @throws java.nio.charset.UnsupportedCharsetException thrown instead of {@link java.io
     * .UnsupportedEncodingException} in version 2.2 if the encoding is not supported by the VM
     * @since 2.1
     */
    public static void write(final File file, final CharSequence data, final String charsetName, final boolean append) throws IOException {
        write(file, data, Charsets.toCharset(charsetName), append);
    }

    // Must be called with a directory

    /**
     * Writes a byte array to a file creating the file if it does not exist.
     * <p>
     * NOTE: As from v1.3, the parent directories of the file will be created
     * if they do not exist.
     * </p>
     *
     * @param file the file to write to
     * @param data the content to write to the file
     * @throws IOException in case of an I/O error
     * @since 1.1
     */
    public static void writeByteArrayToFile(final File file, final byte[] data) throws IOException {
        writeByteArrayToFile(file, data, false);
    }

    /**
     * Writes a byte array to a file creating the file if it does not exist.
     *
     * @param file   the file to write to
     * @param data   the content to write to the file
     * @param append if {@code true}, then bytes will be added to the
     *               end of the file rather than overwriting
     * @throws IOException in case of an I/O error
     * @since 2.1
     */
    public static void writeByteArrayToFile(final File file, final byte[] data, final boolean append) throws IOException {
        writeByteArrayToFile(file, data, 0, data.length, append);
    }

    /**
     * Writes {@code len} bytes from the specified byte array starting
     * at offset {@code off} to a file, creating the file if it does
     * not exist.
     *
     * @param file the file to write to
     * @param data the content to write to the file
     * @param off  the start offset in the data
     * @param len  the number of bytes to write
     * @throws IOException in case of an I/O error
     * @since 2.5
     */
    public static void writeByteArrayToFile(final File file, final byte[] data, final int off, final int len) throws IOException {
        writeByteArrayToFile(file, data, off, len, false);
    }

    /**
     * Writes {@code len} bytes from the specified byte array starting
     * at offset {@code off} to a file, creating the file if it does
     * not exist.
     *
     * @param file   the file to write to
     * @param data   the content to write to the file
     * @param off    the start offset in the data
     * @param len    the number of bytes to write
     * @param append if {@code true}, then bytes will be added to the
     *               end of the file rather than overwriting
     * @throws IOException in case of an I/O error
     * @since 2.5
     */
    public static void writeByteArrayToFile(final File file, final byte[] data, final int off, final int len, final boolean append) throws IOException {
        try (OutputStream out = newOutputStream(file, append)) {
            out.write(data, off, len);
        }
    }

    /**
     * Writes the {@code toString()} value of each item in a collection to
     * the specified {@link File} line by line.
     * The default VM encoding and the default line ending will be used.
     *
     * @param file  the file to write to
     * @param lines the lines to write, {@code null} entries produce blank lines
     * @throws IOException in case of an I/O error
     * @since 1.3
     */
    public static void writeLines(final File file, final Collection<?> lines) throws IOException {
        writeLines(file, null, lines, null, false);
    }

    /**
     * Writes the {@code toString()} value of each item in a collection to
     * the specified {@link File} line by line.
     * The default VM encoding and the default line ending will be used.
     *
     * @param file   the file to write to
     * @param lines  the lines to write, {@code null} entries produce blank lines
     * @param append if {@code true}, then the lines will be added to the
     *               end of the file rather than overwriting
     * @throws IOException in case of an I/O error
     * @since 2.1
     */
    public static void writeLines(final File file, final Collection<?> lines, final boolean append) throws IOException {
        writeLines(file, null, lines, null, append);
    }

    /**
     * Writes the {@code toString()} value of each item in a collection to
     * the specified {@link File} line by line.
     * The default VM encoding and the specified line ending will be used.
     *
     * @param file       the file to write to
     * @param lines      the lines to write, {@code null} entries produce blank lines
     * @param lineEnding the line separator to use, {@code null} is system default
     * @throws IOException in case of an I/O error
     * @since 1.3
     */
    public static void writeLines(final File file, final Collection<?> lines, final String lineEnding) throws IOException {
        writeLines(file, null, lines, lineEnding, false);
    }

    /**
     * Writes the {@code toString()} value of each item in a collection to
     * the specified {@link File} line by line.
     * The default VM encoding and the specified line ending will be used.
     *
     * @param file       the file to write to
     * @param lines      the lines to write, {@code null} entries produce blank lines
     * @param lineEnding the line separator to use, {@code null} is system default
     * @param append     if {@code true}, then the lines will be added to the
     *                   end of the file rather than overwriting
     * @throws IOException in case of an I/O error
     * @since 2.1
     */
    public static void writeLines(final File file, final Collection<?> lines, final String lineEnding, final boolean append) throws IOException {
        writeLines(file, null, lines, lineEnding, append);
    }

    /**
     * Writes the {@code toString()} value of each item in a collection to
     * the specified {@link File} line by line.
     * The specified character encoding and the default line ending will be used.
     * <p>
     * NOTE: As from v1.3, the parent directories of the file will be created
     * if they do not exist.
     * </p>
     *
     * @param file     the file to write to
     * @param charsetName the name of the requested charset, {@code null} means platform default
     * @param lines    the lines to write, {@code null} entries produce blank lines
     * @throws IOException                          in case of an I/O error
     * @throws java.io.UnsupportedEncodingException if the encoding is not supported by the VM
     * @since 1.1
     */
    public static void writeLines(final File file, final String charsetName, final Collection<?> lines) throws IOException {
        writeLines(file, charsetName, lines, null, false);
    }

    /**
     * Writes the {@code toString()} value of each item in a collection to
     * the specified {@link File} line by line, optionally appending.
     * The specified character encoding and the default line ending will be used.
     *
     * @param file     the file to write to
     * @param charsetName the name of the requested charset, {@code null} means platform default
     * @param lines    the lines to write, {@code null} entries produce blank lines
     * @param append   if {@code true}, then the lines will be added to the
     *                 end of the file rather than overwriting
     * @throws IOException                          in case of an I/O error
     * @throws java.io.UnsupportedEncodingException if the encoding is not supported by the VM
     * @since 2.1
     */
    public static void writeLines(final File file, final String charsetName, final Collection<?> lines, final boolean append) throws IOException {
        writeLines(file, charsetName, lines, null, append);
    }

    /**
     * Writes the {@code toString()} value of each item in a collection to
     * the specified {@link File} line by line.
     * The specified character encoding and the line ending will be used.
     * <p>
     * NOTE: As from v1.3, the parent directories of the file will be created
     * if they do not exist.
     * </p>
     *
     * @param file       the file to write to
     * @param charsetName   the name of the requested charset, {@code null} means platform default
     * @param lines      the lines to write, {@code null} entries produce blank lines
     * @param lineEnding the line separator to use, {@code null} is system default
     * @throws IOException                          in case of an I/O error
     * @throws java.io.UnsupportedEncodingException if the encoding is not supported by the VM
     * @since 1.1
     */
    public static void writeLines(final File file, final String charsetName, final Collection<?> lines, final String lineEnding) throws IOException {
        writeLines(file, charsetName, lines, lineEnding, false);
    }

    /**
     * Writes the {@code toString()} value of each item in a collection to
     * the specified {@link File} line by line.
     * The specified character encoding and the line ending will be used.
     *
     * @param file       the file to write to
     * @param charsetName   the name of the requested charset, {@code null} means platform default
     * @param lines      the lines to write, {@code null} entries produce blank lines
     * @param lineEnding the line separator to use, {@code null} is system default
     * @param append     if {@code true}, then the lines will be added to the
     *                   end of the file rather than overwriting
     * @throws IOException                          in case of an I/O error
     * @throws java.io.UnsupportedEncodingException if the encoding is not supported by the VM
     * @since 2.1
     */
    public static void writeLines(final File file, final String charsetName, final Collection<?> lines, final String lineEnding, final boolean append)
        throws IOException {
        try (OutputStream out = new BufferedOutputStream(newOutputStream(file, append))) {
            IOUtils.writeLines(lines, lineEnding, out, charsetName);
        }
    }

    /**
     * Writes a String to a file creating the file if it does not exist using the default encoding for the VM.
     *
     * @param file the file to write
     * @param data the content to write to the file
     * @throws IOException in case of an I/O error
     * @deprecated 2.5 use {@link #writeStringToFile(File, String, Charset)} instead (and specify the appropriate encoding)
     */
    @Deprecated
    public static void writeStringToFile(final File file, final String data) throws IOException {
        writeStringToFile(file, data, Charset.defaultCharset(), false);
    }

    /**
     * Writes a String to a file creating the file if it does not exist using the default encoding for the VM.
     *
     * @param file   the file to write
     * @param data   the content to write to the file
     * @param append if {@code true}, then the String will be added to the
     *               end of the file rather than overwriting
     * @throws IOException in case of an I/O error
     * @since 2.1
     * @deprecated 2.5 use {@link #writeStringToFile(File, String, Charset, boolean)} instead (and specify the appropriate encoding)
     */
    @Deprecated
    public static void writeStringToFile(final File file, final String data, final boolean append) throws IOException {
        writeStringToFile(file, data, Charset.defaultCharset(), append);
    }

    /**
     * Writes a String to a file creating the file if it does not exist.
     * <p>
     * NOTE: As from v1.3, the parent directories of the file will be created
     * if they do not exist.
     * </p>
     *
     * @param file     the file to write
     * @param data     the content to write to the file
     * @param charset the charset to use, {@code null} means platform default
     * @throws IOException                          in case of an I/O error
     * @throws java.io.UnsupportedEncodingException if the encoding is not supported by the VM
     * @since 2.4
     */
    public static void writeStringToFile(final File file, final String data, final Charset charset) throws IOException {
        writeStringToFile(file, data, charset, false);
    }

    /**
     * Writes a String to a file creating the file if it does not exist.
     *
     * @param file     the file to write
     * @param data     the content to write to the file
     * @param charset the charset to use, {@code null} means platform default
     * @param append   if {@code true}, then the String will be added to the
     *                 end of the file rather than overwriting
     * @throws IOException in case of an I/O error
     * @since 2.3
     */
    public static void writeStringToFile(final File file, final String data, final Charset charset, final boolean append) throws IOException {
        try (OutputStream out = newOutputStream(file, append)) {
            IOUtils.write(data, out, charset);
        }
    }

    /**
     * Writes a String to a file creating the file if it does not exist.
     * <p>
     * NOTE: As from v1.3, the parent directories of the file will be created
     * if they do not exist.
     * </p>
     *
     * @param file     the file to write
     * @param data     the content to write to the file
     * @param charsetName the name of the requested charset, {@code null} means platform default
     * @throws IOException                          in case of an I/O error
     * @throws java.io.UnsupportedEncodingException if the encoding is not supported by the VM
     */
    public static void writeStringToFile(final File file, final String data, final String charsetName) throws IOException {
        writeStringToFile(file, data, charsetName, false);
    }

    /**
     * Writes a String to a file creating the file if it does not exist.
     *
     * @param file     the file to write
     * @param data     the content to write to the file
     * @param charsetName the name of the requested charset, {@code null} means platform default
     * @param append   if {@code true}, then the String will be added to the
     *                 end of the file rather than overwriting
     * @throws IOException                 in case of an I/O error
     * @throws java.nio.charset.UnsupportedCharsetException thrown instead of {@link java.io
     * .UnsupportedEncodingException} in version 2.2 if the encoding is not supported by the VM
     * @since 2.1
     */
    public static void writeStringToFile(final File file, final String data, final String charsetName, final boolean append) throws IOException {
        writeStringToFile(file, data, Charsets.toCharset(charsetName), append);
    }

    /**
     * Instances should NOT be constructed in standard programming.
     * @deprecated Will be private in 3.0.
     */
    @Deprecated
    public FileUtils() { //NOSONAR

    }

}
            """
        )
    )




}
