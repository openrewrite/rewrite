/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.config;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Contributor;
import org.openrewrite.Recipe;
import org.openrewrite.internal.MetricsHelper;
import org.openrewrite.internal.RecipeIntrospectionUtils;
import org.openrewrite.internal.RecipeLoader;
import org.openrewrite.style.NamedStyles;

import java.io.*;

import org.objectweb.asm.ClassReader;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

public class ClasspathScanningLoader implements ResourceLoader {

    private final LinkedHashMap<String, Recipe> recipes = new LinkedHashMap<>();
    private final List<NamedStyles> styles = new ArrayList<>();

    private final LinkedHashSet<RecipeDescriptor> recipeDescriptors = new LinkedHashSet<>();
    private final List<CategoryDescriptor> categoryDescriptors = new ArrayList<>();

    private final Map<String, List<RecipeExample>> recipeExamples = new HashMap<>();

    private final ClassLoader classLoader;
    private final RecipeLoader recipeLoader;
    private @Nullable Runnable performScan;

    /**
     * Construct a ClasspathScanningLoader that scans the runtime classpath of the current java process for recipes,
     * mostly for use in tests.
     *
     * @param properties     Yaml placeholder properties
     * @param acceptPackages Limit scan to specified packages
     */
    public ClasspathScanningLoader(@Nullable Properties properties, String[] acceptPackages) {
        this.classLoader = ClasspathScanningLoader.class.getClassLoader();
        this.recipeLoader = new RecipeLoader(classLoader);
        Set<String> packagePrefixes = new HashSet<>();
        for (String pkg : acceptPackages) {
            packagePrefixes.add(pkg.replace('.', '/'));
        }
        this.performScan = () -> {
            Map<String, String> superclassMap = buildSuperclassMapFromClassLoader(classLoader);
            if (!packagePrefixes.isEmpty()) {
                superclassMap.keySet().removeIf(name -> {
                    String path = name.replace('.', '/');
                    for (String prefix : packagePrefixes) {
                        if (path.startsWith(prefix)) {
                            return false;
                        }
                    }
                    return true;
                });
            }
            configureRecipesAndStyles(superclassMap, classLoader);
            List<YamlResource> yaml = listYamlResourcesFromClassLoader(classLoader);
            scanYaml(yaml, properties, emptyList(), null);
        };
    }

    /**
     * Construct a ClasspathScanningLoader scans the provided classloader for recipes.
     *
     * @param properties  YAML placeholder properties
     * @param classLoader Limit scan to classes loadable by this classloader
     */
    public ClasspathScanningLoader(@Nullable Properties properties, ClassLoader classLoader) {
        this.classLoader = classLoader;
        this.recipeLoader = new RecipeLoader(classLoader);
        this.performScan = () -> {
            configureRecipesAndStyles(buildSuperclassMapFromClassLoader(classLoader), classLoader);
            scanYaml(listYamlResourcesFromClassLoader(classLoader), properties, emptyList(), classLoader);
        };
    }

    /**
     * Construct a ClasspathScanningLoader as used from {@code Environment.scanJar} for
     * {@code MavenRecipeBundleReader.marketplaceFromClasspathScan}.
     * Supports both jar files and directories containing class files.
     * <p>
     * The classLoader is used to resolve class hierarchies through intermediate classes
     * in dependency JARs (e.g. a recipe extending a base class in a dependency that
     * itself extends ScanningRecipe).
     */
    public ClasspathScanningLoader(Path jar, @Nullable Properties properties, Collection<? extends ResourceLoader> dependencyResourceLoaders, ClassLoader classLoader) {
        this(jar, properties, dependencyResourceLoaders, classLoader, new ClassLoaderBackedSuperclassMap(new HashMap<>(), classLoader));
    }

    ClasspathScanningLoader(Path jar, @Nullable Properties properties, Collection<? extends ResourceLoader> dependencyResourceLoaders, ClassLoader classLoader, ClassLoaderBackedSuperclassMap sharedSuperclassMap) {
        this.classLoader = classLoader;
        this.recipeLoader = new RecipeLoader(classLoader);

        this.performScan = () -> {
            Map<String, String> jarClasses = new HashMap<>();
            buildSuperclassMapFromPath(jar, jarClasses);
            sharedSuperclassMap.putAll(jarClasses);
            configureRecipesAndStyles(jarClasses, sharedSuperclassMap, classLoader);
            scanYaml(listYamlResourcesFromPath(jar), properties, dependencyResourceLoaders, classLoader);
        };
    }

    /**
     * Construct a ClasspathScanningLoader to load Yaml categories and recipes from the runtime classpath, as part of
     * running tests or inferring local recipe categories.
     */
    public static ClasspathScanningLoader onlyYaml(@Nullable Properties properties) {
        ClasspathScanningLoader loader = new ClasspathScanningLoader();
        loader.scanYaml(
                listYamlResourcesFromClassLoader(ClasspathScanningLoader.class.getClassLoader()),
                properties,
                emptyList(),
                null);
        return loader;
    }

    /**
     * Construct a ClasspathScanningLoader to load categories from the provided dependencies only, as part of migration
     * in the CLI.
     */
    public static ClasspathScanningLoader onlyYaml(@Nullable Properties properties, Collection<Path> dependencies) {
        ClasspathScanningLoader loader = new ClasspathScanningLoader();
        List<YamlResource> yamlResources = new ArrayList<>();
        for (Path dep : dependencies) {
            yamlResources.addAll(listYamlResourcesFromPath(dep));
        }
        loader.scanYaml(yamlResources, properties, emptyList(), null);
        return loader;
    }

    private ClasspathScanningLoader() {
        this.classLoader = ClasspathScanningLoader.class.getClassLoader();
        this.recipeLoader = new RecipeLoader(classLoader);
    }

    // ---- Class hierarchy scanning via ASM ----

    /**
     * Builds a map of className -> superClassName by reading class bytecode headers from
     * all URLs in a classloader. Only reads the first few bytes of each class file via ASM's
     * ClassReader to extract the superclass — no full parsing or class loading required.
     */
    private static Map<String, String> buildSuperclassMapFromClassLoader(ClassLoader classLoader) {
        Map<String, String> superclassMap = new HashMap<>();
        for (Path path : classpathEntriesOf(classLoader)) {
            buildSuperclassMapFromPath(path, superclassMap);
        }
        return superclassMap;
    }

    private static List<Path> classpathEntriesOf(ClassLoader classLoader) {
        // Use URLClassLoader URLs when available (e.g. RecipeClassLoader),
        // otherwise fall back to java.class.path
        if (classLoader instanceof URLClassLoader) {
            List<Path> paths = new ArrayList<>();
            for (URL url : ((URLClassLoader) classLoader).getURLs()) {
                try {
                    paths.add(Paths.get(url.toURI()));
                } catch (Exception ignored) {
                }
            }
            if (!paths.isEmpty()) {
                return paths;
            }
        }
        String classpath = System.getProperty("java.class.path");
        if (classpath != null) {
            List<Path> paths = new ArrayList<>();
            for (String entry : classpath.split(File.pathSeparator)) {
                paths.add(Paths.get(entry));
            }
            return paths;
        }
        return emptyList();
    }

    /**
     * Builds a map of className -> superClassName from a JAR file or directory.
     */
    private static void buildSuperclassMapFromPath(Path path, Map<String, String> superclassMap) {
        if (Files.isDirectory(path)) {
            try {
                Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        if (file.toString().endsWith(".class") && !file.toString().contains("module-info")) {
                            try (InputStream is = Files.newInputStream(file)) {
                                ClassReader cr = new ClassReader(is);
                                String className = cr.getClassName().replace('/', '.');
                                String superName = cr.getSuperName();
                                if (superName != null) {
                                    superclassMap.put(className, superName.replace('/', '.'));
                                }
                            } catch (IOException | IllegalArgumentException ignored) {
                            }
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException ignored) {
            }
        } else if (Files.isRegularFile(path)) {
            try (JarFile jarFile = new JarFile(path.toFile())) {
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if (name.endsWith(".class") && !name.contains("module-info") && !entry.isDirectory()) {
                        try (InputStream is = jarFile.getInputStream(entry)) {
                            ClassReader cr = new ClassReader(is);
                            String className = cr.getClassName().replace('/', '.');
                            String superName = cr.getSuperName();
                            if (superName != null) {
                                superclassMap.put(className, superName.replace('/', '.'));
                            }
                        } catch (IOException | IllegalArgumentException ignored) {
                        }
                    }
                }
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * A map that lazily resolves superclass names by reading class bytecode from the classloader.
     * Entries from the seed map are returned directly; missing entries are resolved on demand
     * by finding the .class resource via the classloader and reading its header with ASM.
     */
    static class ClassLoaderBackedSuperclassMap extends HashMap<String, String> {
        private static final String NOT_FOUND = "\0";
        private final ClassLoader classLoader;

        ClassLoaderBackedSuperclassMap(Map<String, String> seed, ClassLoader classLoader) {
            super(seed);
            this.classLoader = classLoader;
        }

        @Override
        public @Nullable String get(Object key) {
            String result = super.get(key);
            if (result != null) {
                return NOT_FOUND.equals(result) ? null : result;
            }
            if (key instanceof String) {
                String className = (String) key;
                String resolved = resolveSuperclass(className);
                // Cache both positive and negative results to avoid repeated lookups
                put(className, resolved != null ? resolved : NOT_FOUND);
                return resolved;
            }
            return null;
        }

        private @Nullable String resolveSuperclass(String className) {
            String resourceName = className.replace('.', '/') + ".class";
            try (InputStream is = classLoader.getResourceAsStream(resourceName)) {
                if (is != null) {
                    ClassReader cr = new ClassReader(is);
                    String superName = cr.getSuperName();
                    return superName != null ? superName.replace('/', '.') : null;
                }
            } catch (IOException | IllegalArgumentException ignored) {
            }
            return null;
        }
    }

    private static final Set<String> RECIPE_SUPERCLASS_NAMES = new HashSet<>(Arrays.asList(
            Recipe.class.getName(),
            "org.openrewrite.ScanningRecipe"
    ));

    private static final String NAMED_STYLES_NAME = NamedStyles.class.getName();

    /**
     * Walk the superclass chain in the map to determine if a class transitively
     * extends one of the target superclasses.
     */
    private static boolean isSubclassOf(String className, Set<String> targetSuperclasses, Map<String, String> superclassMap) {
        String current = className;
        while (current != null) {
            if (targetSuperclasses.contains(current)) {
                return true;
            }
            if (current.startsWith("java.") || current.startsWith("com.sun.") ||
                    current.startsWith("sun.") || current.startsWith("jdk.")) {
                return false;
            }
            current = superclassMap.get(current);
        }
        return false;
    }



    // ---- YAML resource enumeration ----

    private static class YamlResource {
        final URI uri;
        final InputStreamSupplier inputStreamSupplier;

        YamlResource(URI uri, InputStreamSupplier inputStreamSupplier) {
            this.uri = uri;
            this.inputStreamSupplier = inputStreamSupplier;
        }

        @FunctionalInterface
        interface InputStreamSupplier {
            InputStream get() throws IOException;
        }
    }

    private static List<YamlResource> listYamlResourcesFromClassLoader(ClassLoader classLoader) {
        List<YamlResource> resources = new ArrayList<>();
        for (Path entry : classpathEntriesOf(classLoader)) {
            resources.addAll(listYamlResourcesFromPath(entry));
        }
        return resources;
    }

    private static List<YamlResource> listYamlResourcesFromPath(Path path) {
        List<YamlResource> resources = new ArrayList<>();
        if (Files.isDirectory(path)) {
            Path rewriteDir = path.resolve("META-INF/rewrite");
            if (Files.isDirectory(rewriteDir)) {
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(rewriteDir, "*.{yml,yaml}")) {
                    for (Path file : stream) {
                        resources.add(new YamlResource(file.toUri(), () -> Files.newInputStream(file)));
                    }
                } catch (IOException ignored) {
                }
            }
        } else if (Files.isRegularFile(path)) {
            addYamlResourcesFromJar(resources, path);
        }
        return resources;
    }

    private static void addYamlResourcesFromJar(List<YamlResource> resources, Path jarPath) {
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (name.startsWith("META-INF/rewrite/") && !entry.isDirectory() &&
                        (name.endsWith(".yml") || name.endsWith(".yaml"))) {
                    // Read the content eagerly since the JarFile will be closed
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    try (InputStream is = jarFile.getInputStream(entry)) {
                        byte[] buf = new byte[8192];
                        int n;
                        while ((n = is.read(buf)) != -1) {
                            baos.write(buf, 0, n);
                        }
                    }
                    byte[] content = baos.toByteArray();
                    URI uri = URI.create("jar:" + jarPath.toUri() + "!/" + name);
                    resources.add(new YamlResource(uri, () -> new ByteArrayInputStream(content)));
                }
            }
        } catch (IOException ignored) {
        }
    }

    // ---- Recipe, style, and YAML configuration ----

    /**
     * Configure recipes and styles where candidates and hierarchy are in the same map.
     */
    private void configureRecipesAndStyles(Map<String, String> superclassMap, ClassLoader classLoader) {
        configureRecipesAndStyles(superclassMap, superclassMap, classLoader);
    }

    /**
     * Configure recipes and styles. Candidates are the classes to consider; the hierarchy
     * map is used to trace superclass chains (may include classes from dependencies).
     */
    private void configureRecipesAndStyles(Map<String, String> candidates, Map<String, String> hierarchyMap, ClassLoader classLoader) {
        Set<String> configured = new HashSet<>();

        for (String name : candidates.keySet()) {
            if (isSubclassOf(name, RECIPE_SUPERCLASS_NAMES, hierarchyMap)) {
                try {
                    Class<?> cls = classLoader.loadClass(name);
                    if (!cls.getName().equals(DeclarativeRecipe.class.getName()) &&
                            (cls.getModifiers() & Modifier.PUBLIC) != 0 &&
                            (cls.getModifiers() & Modifier.ABSTRACT) == 0) {
                        configureRecipe(cls, configured);
                    }
                } catch (ClassNotFoundException | LinkageError ignored) {
                }
            } else if (isSubclassOf(name, Collections.singleton(NAMED_STYLES_NAME), hierarchyMap)) {
                try {
                    Class<?> cls = classLoader.loadClass(name);
                    if ((cls.getModifiers() & Modifier.PUBLIC) != 0 &&
                            (cls.getModifiers() & Modifier.ABSTRACT) == 0) {
                        Constructor<?> constructor = RecipeIntrospectionUtils.getZeroArgsConstructor(cls);
                        if (constructor != null) {
                            constructor.setAccessible(true);
                            styles.add((NamedStyles) constructor.newInstance());
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        }
    }

    private void configureRecipe(Class<?> recipeClass, Set<String> configured) {
        if (!configured.add(recipeClass.getName())) {
            return;
        }
        Timer.Builder builder = Timer.builder("rewrite.scan.configure.recipe");
        Timer.Sample sample = Timer.start();
        try {
            Recipe recipe = recipeLoader.load(recipeClass, emptyMap());
            recipeDescriptors.add(recipe.getDescriptor());
            recipes.put(recipe.getName(), recipe);
            MetricsHelper.successTags(builder.tags("recipe", "elided"));
        } catch (Throwable e) {
            MetricsHelper.errorTags(builder.tags("recipe", recipeClass.getName()), e);
        } finally {
            sample.stop(builder.register(Metrics.globalRegistry));
        }
    }

    /**
     * This must be called _after_ configureRecipesAndStyles or the descriptors of declarative recipes will be missing
     * any non-declarative recipes they depend on that would be discovered by class scanning.
     */
    private void scanYaml(List<YamlResource> yamlResources, @Nullable Properties properties,
                          Collection<? extends ResourceLoader> dependencyResourceLoaders,
                          @Nullable ClassLoader classLoader) {
        List<YamlResourceLoader> yamlResourceLoaders = new ArrayList<>();
        for (YamlResource resource : yamlResources) {
            try (InputStream input = resource.inputStreamSupplier.get()) {
                yamlResourceLoaders.add(new YamlResourceLoader(input, resource.uri, properties, classLoader, dependencyResourceLoaders));
            } catch (IOException ignored) {
            }
        }
        // Extract in two passes so that the full list of recipes from all sources are known when computing recipe descriptors
        // Otherwise recipes which include recipes from other sources in their recipeList will have incomplete descriptors
        for (YamlResourceLoader resourceLoader : yamlResourceLoaders) {
            for (Recipe recipe : resourceLoader.listRecipes()) {
                recipes.put(recipe.getName(), recipe);
            }
            categoryDescriptors.addAll(resourceLoader.listCategoryDescriptors());
            styles.addAll(resourceLoader.listStyles());
            recipeExamples.putAll(resourceLoader.listRecipeExamples());
        }
        for (YamlResourceLoader resourceLoader : yamlResourceLoaders) {
            recipeDescriptors.addAll(resourceLoader.listRecipeDescriptors(recipes.values(), recipeExamples));
        }
    }

    // ---- ResourceLoader interface ----

    @Override
    public @Nullable Recipe loadRecipe(String recipeName, RecipeDetail... details) {
        if (performScan != null) {
            try {
                return recipeLoader.load(recipeName, null);
            } catch (NoClassDefFoundError | IllegalArgumentException ignored) {
                // it's probably declarative
            }
        }
        ensureScanned();
        return recipes.get(recipeName);
    }

    @Override
    public Collection<Recipe> listRecipes() {
        ensureScanned();
        return recipes.values();
    }

    private void ensureScanned() {
        if (performScan != null) {
            Runnable scan = performScan;
            performScan = null;
            scan.run();
        }
    }

    @Override
    public Collection<RecipeDescriptor> listRecipeDescriptors() {
        ensureScanned();
        return recipeDescriptors;
    }

    @Override
    public Collection<CategoryDescriptor> listCategoryDescriptors() {
        ensureScanned();
        return categoryDescriptors;
    }

    @Override
    public Collection<NamedStyles> listStyles() {
        ensureScanned();
        return styles;
    }

    @Override
    public Map<String, List<RecipeExample>> listRecipeExamples() {
        ensureScanned();
        return recipeExamples;
    }

    @Override
    public Map<String, List<Contributor>> listContributors() {
        return emptyMap();
    }
}
