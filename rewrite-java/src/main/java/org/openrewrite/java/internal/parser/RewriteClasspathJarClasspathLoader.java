/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.java.internal.parser;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.Resource;
import io.github.classgraph.ResourceList;
import io.github.classgraph.ScanResult;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaParserExecutionContextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import static java.util.Collections.sort;
import static java.util.Objects.requireNonNull;
import static org.openrewrite.java.internal.parser.JavaParserCaller.findCaller;

/**
 * As of 6.1.0, we are using {@link TypeTable} to include {@link JavaParser} dependencies
 * as resources in the classpath, because including the whole JAR caused bloat in the size of recipe JARs
 * and would often get blocked by security scanners that weren't able to recognize that these JAR resources
 * were only used to parse templats and never executed and so didn't represent a security threat.
 * <p>
 * Provided that the type table concept works well, this class and the technique it enables
 * will be removed in a future release.
 */
public class RewriteClasspathJarClasspathLoader implements JavaParserClasspathLoader, AutoCloseable {
    private final Class<?> caller = findCaller();
    private final ScanResult result;
    private final ResourceList resources;
    private final ExecutionContext ctx;

    public RewriteClasspathJarClasspathLoader(ExecutionContext ctx) {
        this.ctx = ctx;
        result = new ClassGraph().acceptPaths("META-INF/rewrite/classpath").scan();
        resources = result.getResourcesWithExtension(".jar");
    }

    @Override
    public @Nullable Path load(String artifactName) {
        Pattern jarPattern = Pattern.compile(artifactName + "-?.*\\.jar$");
        for (Resource resource : resources) {
            String fileName = Paths.get(resource.getPath()).getFileName().toString();
            if (jarPattern.matcher(fileName).matches()) {
                try {
                    Path artifact = getJarsFolder(ctx).resolve(Paths.get(resource.getPath()).getFileName());
                    if (!Files.exists(artifact)) {
                        try {
                            InputStream resourceAsStream = requireNonNull(
                                    caller.getResourceAsStream("/" + resource.getPath()),
                                    caller.getCanonicalName() + " resource not found: " + resource.getPath());
                            Files.copy(resourceAsStream, artifact);
                        } catch (FileAlreadyExistsException ignore) {
                            // can happen when tests run in parallel, for example
                        }
                    }
                    return artifact;
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        }
        return null;
    }

    @Override
    public Collection<String> availableArtifacts() {
        List<String> available = new ArrayList<>(resources.size());
        for (Resource resource : resources) {
            String fileName = Paths.get(resource.getPath()).getFileName().toString();
            if (fileName.endsWith(".jar")) {
                available.add(fileName.substring(0, fileName.length() - 4));
            }
        }
        sort(available);
        return available;
    }

    /**
     * The /.jars folder will contain JARs that were packed into /META-INF/rewrite/classpath as
     * part of the original integration to pack parser classpath resources into recipe JARs. Those
     * JARs existed in /META-INF/rewrite/classpath with just file names of the form
     * [ARTIFACT]-[VERSION].jar, and so there was always the possibility of collision on JARs
     * with the same artifact name and different group IDs with this mechanism. As a result, we'll
     * continue to write those JARs out to the /jars folder, distinct from the way we write out
     * parser classpath resources from {@link TypeTable} going forward, so that there is no possibility
     * that we pick up a JAR written from /META-INF/rewrite/classpath that has a more accurate GAV
     * coordinate fully specified in a type table.
     *
     * @param ctx An execution context from which we can determine the root of the JARs folder
     * @return The path to the /.jars folder
     */
    private static Path getJarsFolder(ExecutionContext ctx) {
        Path jarsFolder = JavaParserExecutionContextView.view(ctx)
                .getParserClasspathDownloadTarget().toPath().resolve(".jars");
        if (!Files.exists(jarsFolder) && !jarsFolder.toFile().mkdirs()) {
            throw new UncheckedIOException(new IOException("Failed to create directory " + jarsFolder));
        }
        return jarsFolder;
    }

    @Override
    public void close() {
        resources.close();
        result.close();
    }
}
