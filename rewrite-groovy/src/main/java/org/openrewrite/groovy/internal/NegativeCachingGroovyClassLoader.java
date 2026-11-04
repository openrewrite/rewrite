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
package org.openrewrite.groovy.internal;

import groovy.lang.GroovyClassLoader;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilerConfiguration;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A {@link GroovyClassLoader} that caches names that could not be resolved, so repeated lookups for the same
 * absent name skip re-scanning the whole classpath. Groovy's resolver probes many package-prefixes per
 * unresolved reference, and neither {@link GroovyClassLoader} nor {@code URLClassLoader} caches a
 * {@link ClassNotFoundException}, so each miss re-opens every classpath JAR before failing.
 * <p>
 * Caching misses is safe for {@code GroovyParser}'s usage: the classpath is fixed for the batch and the parser
 * never defines classes into the loader (it compiles only to {@code CANONICALIZATION}), so an absent name stays
 * absent. Only the {@code lookupScriptFiles == false} path is cached, since a {@code true} lookup could resolve
 * a name to a Groovy script source rather than a class.
 */
public class NegativeCachingGroovyClassLoader extends GroovyClassLoader {

    private final Set<String> absentNames = ConcurrentHashMap.newKeySet();

    public NegativeCachingGroovyClassLoader(ClassLoader parent, CompilerConfiguration config) {
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
