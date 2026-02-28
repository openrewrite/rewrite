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
package org.openrewrite.java;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class JavaUnrestrictedClassLoaderTest {

    private static boolean hasModuleSystem() {
        try {
            Class.forName("java.lang.Module");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Verifies that when a class from a named module (jdk.compiler) is passed to
     * addExportIfNeeded, the module export is added so that classes in the
     * classloader's unnamed module can access classes in that package.
     * <p>
     * This tests the fix for the JDK 25 IllegalAccessError where a module split
     * (some classes in unnamed module, some in jdk.compiler) caused access failures.
     */
    @Test
    void addExportIfNeededExportsPackageFromNamedModule() throws Throwable {
        assumeTrue(hasModuleSystem(), "Requires JDK 9+ module system");

        JavaUnrestrictedClassLoader loader = new JavaUnrestrictedClassLoader(
                getClass().getClassLoader());

        try {
            Class<?> moduleClass = Class.forName("java.lang.Module");
            Method getModule = Class.class.getMethod("getModule");
            Method getUnnamedModule = ClassLoader.class.getMethod("getUnnamedModule");
            Method isExported = moduleClass.getMethod("isExported", String.class, moduleClass);

            // Load a class that lives in jdk.compiler module via the system classloader.
            // Class.forName succeeds for non-exported packages; the class object is in
            // jdk.compiler module regardless of export status.
            Class<?> tokensClass = Class.forName(
                    "com.sun.tools.javac.parser.Tokens",
                    false,
                    ClassLoader.getSystemClassLoader());

            Object jdkCompilerModule = getModule.invoke(tokensClass);
            Object unnamedModule = getUnnamedModule.invoke(loader);
            String packageName = "com.sun.tools.javac.parser";

            // Call addExportIfNeeded via reflection
            Method addExportIfNeeded = JavaUnrestrictedClassLoader.class
                    .getDeclaredMethod("addExportIfNeeded", Class.class);
            addExportIfNeeded.setAccessible(true);
            addExportIfNeeded.invoke(loader, tokensClass);

            // After the call, jdk.compiler should export the package to our unnamed module
            boolean exported = (boolean) isExported.invoke(
                    jdkCompilerModule, packageName, unnamedModule);
            assertThat(exported)
                    .as("jdk.compiler should export com.sun.tools.javac.parser to the classloader's unnamed module")
                    .isTrue();
        } finally {
            loader.close();
        }
    }

    /**
     * Verifies that addExportIfNeeded is a no-op for classes in the unnamed module
     * (i.e., classes defined by our own classloader via defineClass).
     */
    @Test
    void addExportIfNeededSkipsUnnamedModuleClasses() throws Throwable {
        assumeTrue(hasModuleSystem(), "Requires JDK 9+ module system");

        JavaUnrestrictedClassLoader loader = new JavaUnrestrictedClassLoader(
                getClass().getClassLoader());

        try {
            // Load a class through our classloader's defineClass path (unnamed module)
            Class<?> contextClass = loader.loadClass("com.sun.tools.javac.util.Context");

            Method getModule = Class.class.getMethod("getModule");
            Object module = getModule.invoke(contextClass);

            Class<?> moduleClass = Class.forName("java.lang.Module");
            Method isNamed = moduleClass.getMethod("isNamed");

            // The class should be in our unnamed module (loaded via defineClass)
            assertThat((boolean) isNamed.invoke(module))
                    .as("Class loaded via defineClass should be in unnamed module")
                    .isFalse();

            // addExportIfNeeded should silently return without error
            Method addExportIfNeeded = JavaUnrestrictedClassLoader.class
                    .getDeclaredMethod("addExportIfNeeded", Class.class);
            addExportIfNeeded.setAccessible(true);
            addExportIfNeeded.invoke(loader, contextClass);
            // No exception = success
        } finally {
            loader.close();
        }
    }

    /**
     * Verifies that addExportIfNeeded is idempotent - calling it multiple times
     * for classes in the same package does not fail.
     */
    @Test
    void addExportIfNeededIsIdempotent() throws Throwable {
        assumeTrue(hasModuleSystem(), "Requires JDK 9+ module system");

        JavaUnrestrictedClassLoader loader = new JavaUnrestrictedClassLoader(
                getClass().getClassLoader());

        try {
            Class<?> tokensClass = Class.forName(
                    "com.sun.tools.javac.parser.Tokens",
                    false,
                    ClassLoader.getSystemClassLoader());
            Class<?> tokenKindClass = Class.forName(
                    "com.sun.tools.javac.parser.Tokens$TokenKind",
                    false,
                    ClassLoader.getSystemClassLoader());

            Method addExportIfNeeded = JavaUnrestrictedClassLoader.class
                    .getDeclaredMethod("addExportIfNeeded", Class.class);
            addExportIfNeeded.setAccessible(true);

            // Call multiple times for classes in the same package - should not throw
            addExportIfNeeded.invoke(loader, tokensClass);
            addExportIfNeeded.invoke(loader, tokenKindClass);
            addExportIfNeeded.invoke(loader, tokensClass);
        } finally {
            loader.close();
        }
    }
}
