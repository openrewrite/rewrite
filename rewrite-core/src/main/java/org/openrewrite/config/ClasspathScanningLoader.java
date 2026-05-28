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
import org.openrewrite.AbstractRecipe;
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
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

public class ClasspathScanningLoader implements ResourceLoader {

    private final LinkedHashMap<String, Recipe> recipes = new LinkedHashMap<>();
    private final List<NamedStyles> styles = new ArrayList<>();

    private final LinkedHashSet<RecipeDescriptor> recipeDescriptors = new LinkedHashSet<>();
    private final List<CategoryDescriptor> categoryDescriptors = new ArrayList<>();

    private final Map<String, List<RecipeExample>> recipeExamples = new HashMap<>();

    private final List<YamlResourceLoader> yamlResourceLoaders = new ArrayList<>();
    // Index of the next YAML loader to drain into the shared maps via progressive scan.
    // Each loader is parsed at most once; subsequent loadRecipe calls hit the shared
    // recipes map directly for any already-scanned name.
    private int yamlScanIndex;
    // Recipe descriptors require cross-loader recipe references to resolve, so they
    // can only be computed after every YAML loader has been drained. This flag
    // guards against duplicate descriptor extraction.
    private boolean recipeDescriptorsExtracted;

    private final ClassLoader classLoader;
    private final RecipeLoader recipeLoader;

    // Scanning is split into independently triggerable phases so single-recipe
    // activation pays only for what it needs:
    //  - performClassScan: walks class bytecode to find Recipe subclasses and
    //    instantiate them (most expensive — ~1 s for a large bundle).
    //  - performYamlListing: enumerates META-INF/rewrite/*.yml and constructs
    //    one YamlResourceLoader per file (cheap; no YAML parsing yet).
    // YAML parsing happens progressively in loadRecipe — one file at a time —
    // until either the requested recipe is found or all loaders are drained.
    private @Nullable Runnable performClassScan;
    private @Nullable Runnable performYamlListing;

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
        this.performClassScan = () -> {
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
        };
        this.performYamlListing = () -> listYamlLoaders(listYamlResourcesFromClassLoader(classLoader), properties, emptyList(), null);
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
        this.performClassScan = () -> configureRecipesAndStyles(buildSuperclassMapFromClassLoader(classLoader), classLoader);
        this.performYamlListing = () -> listYamlLoaders(listYamlResourcesFromClassLoader(classLoader), properties, emptyList(), classLoader);
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

        this.performClassScan = () -> configureRecipesAndStyles(sharedSuperclassMap.scan(jar), classLoader);
        this.performYamlListing = () -> listYamlLoaders(listYamlResourcesFromPath(jar), properties, dependencyResourceLoaders, classLoader);
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

    private static final Set<String> SKIP_CLASS_PREFIXES = new HashSet<>(Arrays.asList(
            "java/", "javax/", "com/sun/", "sun/", "jdk/", "org/w3c/", "org/xml/", "org/ietf/"
    ));

    private static boolean shouldSkipClassEntry(String entryName) {
        for (String prefix : SKIP_CLASS_PREFIXES) {
            if (entryName.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Builds a map of className -> superClassName by reading class bytecode headers from
     * all URLs in a classloader. Only reads the first few bytes of each class file via ASM's
     * ClassReader to extract the superclass — no full parsing or class loading required.
     */
    private static Map<String, String> buildSuperclassMapFromClassLoader(ClassLoader classLoader) {
        Set<String> scannedPaths = new HashSet<>();
        Map<String, String> superclassMap = new HashMap<>();
        for (Path path : classpathEntriesOf(classLoader)) {
            buildSuperclassMapFromPath(path, superclassMap, scannedPaths);
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
        buildSuperclassMapFromPath(path, superclassMap, null);
    }

    private static void buildSuperclassMapFromPath(Path path, Map<String, String> superclassMap, @Nullable Set<String> scannedPaths) {
        try {
            String canonicalPath = path.toAbsolutePath().normalize().toString();
            if (scannedPaths != null && !scannedPaths.add(canonicalPath)) {
                return;
            }
        } catch (Exception ignored) {
        }
        if (Files.isDirectory(path)) {
            try {
                Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        String fileName = file.toString();
                        if (fileName.endsWith(".class") && !fileName.contains("module-info")) {
                            String relative = path.relativize(file).toString();
                            if (shouldSkipClassEntry(relative)) {
                                return FileVisitResult.CONTINUE;
                            }
                            try (InputStream is = Files.newInputStream(file)) {
                                readSuperclass(is, superclassMap);
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
                    if (name.endsWith(".class") && !name.contains("module-info") && !entry.isDirectory()
                            && !shouldSkipClassEntry(name)) {
                        try (InputStream is = jarFile.getInputStream(entry)) {
                            readSuperclass(is, superclassMap);
                        } catch (IOException | IllegalArgumentException ignored) {
                        }
                    }
                }
            } catch (IOException ignored) {
            }
        }
    }

    private static void readSuperclass(InputStream is, Map<String, String> superclassMap) throws IOException {
        ClassReader cr = new ClassReader(is);
        String superName = cr.getSuperName();
        if (superName != null) {
            String superClassName = superName.replace('/', '.');
            // Don't store entries whose superclass is a JDK terminal — isSubclassOf
            // will short-circuit on these prefixes anyway, so they just waste map space.
            if (!superClassName.startsWith("java.") && !superClassName.startsWith("javax.") &&
                    !superClassName.startsWith("com.sun.") && !superClassName.startsWith("sun.") &&
                    !superClassName.startsWith("jdk.")) {
                superclassMap.put(cr.getClassName().replace('/', '.'), superClassName);
            }
        }
    }

    /**
     * A map that lazily resolves superclass names by reading class bytecode from the classloader.
     * Entries from the seed map are returned directly; missing entries are resolved on demand
     * by finding the .class resource via the classloader and reading its header with ASM.
     */
    static class ClassLoaderBackedSuperclassMap {
        private static final String NOT_FOUND = "\0";
        private final Map<String, String> delegate = new HashMap<>();
        private final ClassLoader classLoader;
        private final Set<String> scannedPaths = new HashSet<>();

        ClassLoaderBackedSuperclassMap(Map<String, String> seed, ClassLoader classLoader) {
            delegate.putAll(seed);
            this.classLoader = classLoader;
        }

        public @Nullable String get(String key) {
            String result = delegate.get(key);
            if (result != null) {
                return NOT_FOUND.equals(result) ? null : result;
            }
            String resolved = resolveSuperclass(key);
            // Cache both positive and negative results to avoid repeated lookups
            delegate.put(key, resolved != null ? resolved : NOT_FOUND);
            return resolved;
        }

        public void putAll(Map<String, String> m) {
            delegate.putAll(m);
        }

        /**
         * Scan a path for class files, merge the results into this map, and track
         * the path to avoid duplicate scanning.
         *
         * @return a {@link Scan} bundling the classes found in this specific path
         * (the candidates) together with this hierarchy map.
         */
        public Scan scan(Path path) {
            Map<String, String> classes = new HashMap<>();
            buildSuperclassMapFromPath(path, classes, scannedPaths);
            delegate.putAll(classes);
            return new Scan(classes, this);
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

    /**
     * The result of scanning a path: the classes discovered in that path (candidates for
     * recipe/style registration) together with the hierarchy map used to trace superclass
     * chains across all previously scanned paths.
     */
    static class Scan {
        final Map<String, String> candidates;
        final ClassLoaderBackedSuperclassMap hierarchy;

        Scan(Map<String, String> candidates, ClassLoaderBackedSuperclassMap hierarchy) {
            this.candidates = candidates;
            this.hierarchy = hierarchy;
        }
    }

    private static final String RECIPE_SUPERCLASS_NAME = Recipe.class.getName();

    private static final String NAMED_STYLES_NAME = NamedStyles.class.getName();

    /**
     * Walk the superclass chain in the map to determine if a class transitively
     * extends one of the target superclasses.
     */
    private static boolean isSubclassOf(String className, String targetSuperclass, ClassLoaderBackedSuperclassMap superclassMap) {
        String current = className;
        while (current != null) {
            if (targetSuperclass.equals(current)) {
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
                try (Stream<Path> stream = Files.walk(rewriteDir)) {
                    stream.filter(Files::isRegularFile)
                          .filter(p -> {
                              String name = p.getFileName().toString();
                              return name.endsWith(".yml") || name.endsWith(".yaml");
                          })
                          .forEach(file -> resources.add(new YamlResource(file.toUri(), () -> Files.newInputStream(file))));
                } catch (IOException | java.io.UncheckedIOException ignored) {
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
        configureRecipesAndStyles(new Scan(superclassMap, new ClassLoaderBackedSuperclassMap(superclassMap, classLoader)), classLoader);
    }

    /**
     * Configure recipes and styles. The scan provides both the candidate classes to
     * consider and the hierarchy map used to trace superclass chains (which may
     * include classes from dependencies).
     */
    private void configureRecipesAndStyles(Scan scan, ClassLoader classLoader) {
        Set<String> configured = new HashSet<>();

        for (String name : scan.candidates.keySet()) {
            if (isSubclassOf(name, RECIPE_SUPERCLASS_NAME, scan.hierarchy)) {
                try {
                    Class<?> cls = classLoader.loadClass(name);
                    if (!cls.getName().equals(DeclarativeRecipe.class.getName()) &&
                            (cls.getModifiers() & Modifier.PUBLIC) != 0 &&
                            (cls.getModifiers() & Modifier.ABSTRACT) == 0 &&
                            !cls.isAnnotationPresent(AbstractRecipe.class)) {
                        configureRecipe(cls, configured);
                    }
                } catch (ClassNotFoundException | LinkageError ignored) {
                }
            } else if (isSubclassOf(name, NAMED_STYLES_NAME, scan.hierarchy)) {
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
     * Convenience used by the {@code onlyYaml} factories that bypass the lazy scan
     * machinery. Lists every YAML loader and drains them all into the shared maps.
     * <p>
     * This must be called _after_ configureRecipesAndStyles or the descriptors of declarative recipes will be missing
     * any non-declarative recipes they depend on that would be discovered by class scanning.
     */
    private void scanYaml(List<YamlResource> yamlResources, @Nullable Properties properties,
                          Collection<? extends ResourceLoader> dependencyResourceLoaders,
                          @Nullable ClassLoader classLoader) {
        listYamlLoaders(yamlResources, properties, dependencyResourceLoaders, classLoader);
        drainAllYamlLoaders();
    }

    /**
     * Enumerate the YAML files and construct one {@link YamlResourceLoader} per
     * file. The loader constructor only consumes the YAML input stream into a
     * byte buffer; no YAML parsing happens here, so this remains cheap even for
     * bundles with hundreds of files.
     */
    private void listYamlLoaders(List<YamlResource> yamlResources, @Nullable Properties properties,
                                 Collection<? extends ResourceLoader> dependencyResourceLoaders,
                                 @Nullable ClassLoader classLoader) {
        for (YamlResource resource : yamlResources) {
            try (InputStream input = resource.inputStreamSupplier.get()) {
                yamlResourceLoaders.add(new YamlResourceLoader(input, resource.uri, properties, classLoader, dependencyResourceLoaders));
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * Drain the next unscanned YAML loader into the shared maps. Each loader is
     * parsed at most once across the lifetime of this instance.
     *
     * @return true if a loader was drained; false if every loader has already been processed.
     */
    private boolean drainNextYamlLoader() {
        if (yamlScanIndex >= yamlResourceLoaders.size()) {
            return false;
        }
        YamlResourceLoader loader = yamlResourceLoaders.get(yamlScanIndex++);
        for (Recipe recipe : loader.listRecipes()) {
            recipes.put(recipe.getName(), recipe);
        }
        categoryDescriptors.addAll(loader.listCategoryDescriptors());
        styles.addAll(loader.listStyles());
        recipeExamples.putAll(loader.listRecipeExamples());
        return true;
    }

    /**
     * Drain every remaining YAML loader and compute recipe descriptors. Descriptors
     * require the complete recipe set to resolve cross-loader references, so the
     * descriptor pass runs after all loaders are drained.
     */
    private void drainAllYamlLoaders() {
        while (drainNextYamlLoader()) {
        }
        if (!recipeDescriptorsExtracted) {
            recipeDescriptorsExtracted = true;
            for (YamlResourceLoader loader : yamlResourceLoaders) {
                recipeDescriptors.addAll(loader.listRecipeDescriptors(recipes.values(), recipeExamples));
            }
        }
    }

    // ---- ResourceLoader interface ----

    @Override
    public @Nullable Recipe loadRecipe(String recipeName, RecipeDetail... details) {
        // Imperative fast path: try to load by FQCN directly. This succeeds for any
        // imperative recipe whose name matches its class, without triggering any scan.
        if (performClassScan != null) {
            try {
                return recipeLoader.load(recipeName, null);
            } catch (NoClassDefFoundError | IllegalArgumentException ignored) {
                // it's probably declarative
            }
        }
        // Progressive YAML scan: parse YAML files one at a time until the recipe
        // turns up in the shared map or every loader has been drained. Subsequent
        // lookups for already-scanned names hit the map directly without parsing
        // any additional files.
        Recipe recipe = recipes.get(recipeName);
        if (recipe != null) {
            return recipe;
        }
        ensureYamlListed();
        while (drainNextYamlLoader()) {
            recipe = recipes.get(recipeName);
            if (recipe != null) {
                return recipe;
            }
        }
        return null;
    }

    @Override
    public Collection<Recipe> listRecipes() {
        ensureScanned();
        return recipes.values();
    }

    private void ensureClassesScanned() {
        if (performClassScan != null) {
            Runnable scan = performClassScan;
            performClassScan = null;
            scan.run();
        }
    }

    private void ensureYamlListed() {
        if (performYamlListing != null) {
            Runnable listing = performYamlListing;
            performYamlListing = null;
            listing.run();
        }
    }

    private void ensureYamlScanned() {
        ensureYamlListed();
        drainAllYamlLoaders();
    }

    private void ensureScanned() {
        ensureClassesScanned();
        ensureYamlScanned();
    }

    // ---- Package-private introspection for tests ----

    boolean classScanTriggered() {
        return performClassScan == null;
    }

    boolean yamlListingTriggered() {
        return performYamlListing == null;
    }

    int yamlLoadersDrained() {
        return yamlScanIndex;
    }

    int yamlLoadersListed() {
        return yamlResourceLoaders.size();
    }

    @Override
    public Collection<RecipeDescriptor> listRecipeDescriptors() {
        ensureScanned();
        return recipeDescriptors;
    }

    @Override
    public Collection<CategoryDescriptor> listCategoryDescriptors() {
        // Categories come from YAML only; no need to walk class bytecode.
        ensureYamlScanned();
        return categoryDescriptors;
    }

    @Override
    public Collection<NamedStyles> listStyles() {
        // Styles can come from either imperative classes or YAML, so both phases are needed.
        ensureScanned();
        return styles;
    }

    @Override
    public Map<String, List<RecipeExample>> listRecipeExamples() {
        // Examples are a YAML-only concept; the class scan does not populate this map.
        ensureYamlScanned();
        return recipeExamples;
    }

    @Override
    public Map<String, List<Contributor>> listContributors() {
        return emptyMap();
    }
}
