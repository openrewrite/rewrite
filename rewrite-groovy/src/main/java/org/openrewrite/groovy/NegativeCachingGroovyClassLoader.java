/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.groovy;

import groovy.lang.GroovyClassLoader;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilerConfiguration;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A {@link GroovyClassLoader} that remembers which names could <em>not</em> be resolved, so repeated lookups
 * for the same absent name do not re-scan the whole classpath.
 * <p>
 * Groovy resolves types by loading classes, and its resolver tries many candidate package-prefixes for every
 * unresolved reference (e.g. {@code Foo} is probed as {@code Foo}, {@code <pkg>.Foo}, {@code java.lang.Foo},
 * one per star-import, ...). Neither {@link GroovyClassLoader} nor the underlying {@code URLClassLoader} caches
 * a {@link ClassNotFoundException}, so each of those fruitless probes walks the full delegation chain and
 * re-opens every classpath JAR before failing — paid over and over within a file and, when this loader is
 * reused across the files of a parse batch, across files too. On a large classpath these never-resolving scans
 * dominate the remaining type-resolution I/O.
 * <p>
 * Caching the misses is safe in {@link GroovyParser}'s usage because the classpath is fixed for the batch and
 * the parser never defines new classes into the loader (it compiles only to
 * {@link org.codehaus.groovy.control.Phases#CANONICALIZATION} and never calls {@code parseClass}), so a name
 * that is absent once stays absent for the lifetime of this loader.
 * <p>
 * Only the {@code lookupScriptFiles == false} path is cached — the path Groovy's {@code ClassNodeResolver}
 * uses for type resolution. A {@code lookupScriptFiles == true} call could in principle resolve a name to a
 * Groovy script source rather than a class, so it is neither served from nor written to the cache.
 */
class NegativeCachingGroovyClassLoader extends GroovyClassLoader {

    private final Set<String> absentNames = ConcurrentHashMap.newKeySet();

    NegativeCachingGroovyClassLoader(ClassLoader parent, CompilerConfiguration config) {
        super(parent, config, true);
    }

    @Override
    public Class loadClass(String name, boolean lookupScriptFiles, boolean preferClassOverScript, boolean resolve)
            throws ClassNotFoundException, CompilationFailedException {
        if (!lookupScriptFiles && absentNames.contains(name)) {
            throw new ClassNotFoundException(name);
        }
        try {
            return super.loadClass(name, lookupScriptFiles, preferClassOverScript, resolve);
        } catch (ClassNotFoundException e) {
            if (!lookupScriptFiles) {
                absentNames.add(name);
            }
            throw e;
        }
    }
}
