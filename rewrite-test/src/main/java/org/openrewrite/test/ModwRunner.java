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
package org.openrewrite.test;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import lombok.Builder;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Assumptions;
import org.openrewrite.Changeset;
import org.openrewrite.DataTable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.PrintOutputCapture;
import org.openrewrite.Recipe;
import org.openrewrite.RecipeRun;
import org.openrewrite.Result;
import org.openrewrite.SourceFile;
import org.openrewrite.DataTableStore;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.lang.reflect.Constructor;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * A {@link RewriteRunner} that drives recipe execution through the moderne-cli
 * ({@code mod}). Stages the test's source files to a temp directory, invokes the
 * pinned CLI version via its {@code active.recipe} hook (the same path the
 * Moderne IntelliJ plugin uses for in-IDE recipe development), then translates
 * the CLI's output back into a {@link RecipeRun} so the surrounding
 * {@code rewriteRun} validation works unchanged.
 * <p>
 * The test case is responsible for providing whatever build files (pom.xml,
 * settings.xml, build.gradle, etc.) the CLI needs to load the staged project.
 * Source files are written at their declared {@code sourcePath}.
 * <p>
 * <h3>Construction</h3>
 * Built with the generated Lombok builder; every field is optional and defaulted,
 * so the common case is simply {@code ModwRunner.builder().build()}. Defaults live
 * in one place ({@link #DEFAULT_CLI_VERSION}, {@link #DEFAULT_ORIGIN_URL}) so the
 * same values apply across every recipe library; bump them there to move all
 * consumers at once, or override per runner via the builder
 * ({@code ModwRunner.builder().cliVersion("4.3.0").build()}).
 *
 * <h3>Choosing the CLI version</h3>
 * The effective version resolves as: the {@code moderne.cli.regression.version}
 * system property &gt; the {@code MODERNE_CLI_REGRESSION_VERSION} environment
 * variable &gt; the builder's {@code cliVersion} (default {@link #DEFAULT_CLI_VERSION}).
 * Local developers leave the overrides unset and get the committed default; CI sets
 * the env var once to bump the regression target across all recipe modules.
 *
 * <h3>Silencing known failures</h3>
 * Setting the version to one that is not yet published on Maven Central is treated
 * as a deliberate <em>skip</em>: when the {@code modw} wrapper download returns
 * HTTP&nbsp;404 the test is aborted (reported skipped) rather than failed. This is
 * the "look-ahead bump" workflow — point the version at the not-yet-released CLI
 * that will carry the fix to silence a known compatibility break; once that CLI
 * ships, the check resumes automatically.
 *
 * <h3>Artifact sourcing</h3>
 * The {@code modw} wrapper and the heavy platform distribution are fetched from
 * Moderne's Artifactory cache ({@value #ARTIFACTORY_CACHE}) in preference to Maven
 * Central, which rate-limits parallel CI downloads. Provide credentials via
 * {@link #WRAPPER_DIST_USERNAME_ENV}/{@link #WRAPPER_DIST_PASSWORD_ENV} (or
 * {@link #WRAPPER_DIST_TOKEN_ENV}); these are also read by the wrapper for the
 * distribution download. Authenticated, Artifactory proxies on demand and is
 * authoritative (a 404 means "not published" → skip). Unauthenticated access only
 * reads already-cached artifacts, so a 401 (or 5xx / I/O error) falls back to Maven
 * Central, whose 404 is then the authoritative "not published" signal.
 *
 * <h3>V1 limitations</h3>
 * <ul>
 *     <li>Modified files are re-parsed using the original {@code SourceSpec}'s
 *         parser. If the parser can't be invoked standalone (no JVM dep, RPC-based,
 *         etc.), the run fails.</li>
 *     <li>The recipe's runtime classpath is taken from the test JVM's
 *         {@code java.class.path}. Tests run from a JVM whose classpath doesn't
 *         include the recipe (extreme edge case) need to override.</li>
 * </ul>
 */
@Builder
public class ModwRunner implements RewriteRunner {

    private static final String MAVEN_CENTRAL = "https://repo1.maven.org/maven2";

    /**
     * Moderne's Artifactory cache, used in preference to Maven Central for CLI
     * artifacts because Maven Central rate-limits parallel CI downloads. It proxies
     * the same artifacts (anonymously readable) and is authoritative for the skip
     * decision; Maven Central is consulted only if Artifactory is unavailable.
     */
    private static final String ARTIFACTORY_CACHE =
            "https://artifactory.moderne.ninja/artifactory/moderne-cache-3";

    /**
     * Credentials for the Artifactory cache. Shared with the {@code modw} wrapper's
     * own distribution download (it reads the same variables), so a single set covers
     * both the wrapper fetch and the heavy platform distribution. When set, Artifactory
     * proxies on demand and becomes authoritative (a real 404 for missing artifacts);
     * when unset, access is anonymous (cached artifacts only) and a 401 falls back to
     * Maven Central. A token takes precedence over username/password.
     */
    public static final String WRAPPER_DIST_USERNAME_ENV = "MODERNE_WRAPPER_DISTRIBUTION_USERNAME";
    public static final String WRAPPER_DIST_PASSWORD_ENV = "MODERNE_WRAPPER_DISTRIBUTION_PASSWORD";
    public static final String WRAPPER_DIST_TOKEN_ENV = "MODERNE_WRAPPER_DISTRIBUTION_TOKEN";

    /** Environment variable CI sets to override the committed default CLI version. */
    public static final String VERSION_ENV = "MODERNE_CLI_REGRESSION_VERSION";

    /** System property equivalent of {@link #VERSION_ENV}; takes precedence over it. */
    public static final String VERSION_PROPERTY = "moderne.cli.regression.version";

    /**
     * Default moderne-cli version every recipe library tests against. The single
     * place to bump the floor for all consumers; override per runner via the builder
     * or at runtime via {@link #VERSION_ENV} / {@link #VERSION_PROPERTY}.
     */
    public static final String DEFAULT_CLI_VERSION = "4.2.8";

    /**
     * Environment variable holding a moderne-cli license key. When set, the runner
     * configures it via {@code modw config license edit} before building, which lets
     * the CLI run recipes on private repositories without a Moderne tenant. When
     * unset, the runner proceeds without a license — recipes still run against the
     * open-source fixture repositories these tests use.
     */
    public static final String LICENSE_KEY_ENV = "MODERNE_LICENSE_KEY";

    /**
     * Origin URL given to the staged fixture repo by default. A real public GitHub
     * URL so the CLI's no-license open-source check passes; override per runner via
     * the builder's {@code originUrl} when a different origin is needed.
     */
    public static final String DEFAULT_ORIGIN_URL = "https://github.com/openrewrite/rewrite.git";

    /** Configured CLI version; {@link #resolveCliVersion} applies env/property overrides at run time. */
    @Builder.Default
    private final String cliVersion = DEFAULT_CLI_VERSION;

    @Builder.Default
    private final String originUrl = DEFAULT_ORIGIN_URL;

    /**
     * Resolve the CLI version to run against. Precedence: system property
     * {@value #VERSION_PROPERTY} &gt; environment variable {@value #VERSION_ENV} &gt;
     * the {@code configuredVersion} from the builder (default {@link #DEFAULT_CLI_VERSION}).
     */
    static String resolveCliVersion(String configuredVersion) {
        String fromProperty = System.getProperty(VERSION_PROPERTY);
        if (fromProperty != null && !fromProperty.trim().isEmpty()) {
            return fromProperty.trim();
        }
        String fromEnv = System.getenv(VERSION_ENV);
        if (fromEnv != null && !fromEnv.trim().isEmpty()) {
            return fromEnv.trim();
        }
        return configuredVersion;
    }

    @Override
    public RecipeRun run(Recipe recipe, Context context) {
        try {
            return doRun(recipe, context);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private RecipeRun doRun(Recipe recipe, Context context) throws Exception {
        Path workDir = Files.createTempDirectory("rewrite-modw-");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                deleteRecursively(workDir);
            } catch (IOException ignored) {
            }
        }));

        Path repoDir = workDir.resolve("repo");
        Path cliHome = workDir.resolve("cli-home");
        Path modwScript = workDir.resolve("modw");
        Files.createDirectories(repoDir);
        Files.createDirectories(cliHome);

        // Stage every input source at its declared sourcePath. The test is
        // responsible for including pom.xml / build.gradle / etc. as source specs.
        List<SourceFile> beforeSources = context.getParsedSources();
        Map<Path, SourceFile> beforeByPath = new LinkedHashMap<>();
        for (SourceFile sf : beforeSources) {
            Path target = repoDir.resolve(sf.getSourcePath().toString());
            Files.createDirectories(target.getParent() == null ? repoDir : target.getParent());
            String content = sf.printAll(new PrintOutputCapture<>(new InMemoryExecutionContext()));
            Files.write(target, content.getBytes(StandardCharsets.UTF_8));
            beforeByPath.put(sf.getSourcePath(), sf);
        }

        String version = resolveCliVersion(cliVersion);
        initGitRepo(repoDir, originUrl);
        System.out.println("[modw] CLI regression target version: " + version +
                ", fixture origin: " + originUrl);
        if (!downloadModw(version, modwScript)) {
            // The targeted CLI isn't published yet: interpret as a deliberate skip so
            // a known compatibility break can be silenced by pointing at the
            // not-yet-released CLI that will carry the fix. Aborts (skips) the test.
            Assumptions.abort("[modw] moderne-cli " + version + " is not published on Maven Central; " +
                    "treating this CLI compatibility check as a known/silenced failure (skipped). " +
                    "It resumes automatically once that CLI version is released.");
        }
        writeActiveRecipe(cliHome, recipe.getClass().getName(),
                System.getProperty("java.class.path"));

        Map<String, String> env = new HashMap<>();
        env.put("MODERNE_CLI_HOME", cliHome.toAbsolutePath().toString());
        env.put("MODERNE_WRAPPER_VERSION", version);

        // Source the heavy platform distribution (~150MB+) from Artifactory too, when
        // it serves it; otherwise leave the wrapper on its Maven Central default.
        String distributionUrl = resolveArtifactoryDistributionUrl(version);
        if (distributionUrl != null) {
            env.put("MODERNE_WRAPPER_DISTRIBUTION_URL", distributionUrl);
            // Forward the Artifactory credentials so the wrapper authenticates for the
            // distribution download (the wrapper reads these same variable names).
            forwardIfPresent(env, WRAPPER_DIST_TOKEN_ENV);
            forwardIfPresent(env, WRAPPER_DIST_USERNAME_ENV);
            forwardIfPresent(env, WRAPPER_DIST_PASSWORD_ENV);
            System.out.println("[modw] CLI distribution source: " + distributionUrl);
        }

        configureLicenseIfPresent(repoDir, env, modwScript);

        exec(repoDir, env, modwScript.toString(), "build", ".");
        exec(repoDir, env, modwScript.toString(), "run", ".", "--active-recipe");

        Path runDir = mostRecentSubdir(repoDir.resolve(".moderne").resolve("run"));

        List<Result> results = buildResults(runDir, beforeByPath, context, recipe);
        DataTableStore store = buildDataTableStore(runDir, recipe);

        return new RecipeRun(new ListChangeset(results), store);
    }

    // --- staging / git / CLI invocation ---------------------------------------

    private void initGitRepo(Path repo, String originUrl) throws IOException, InterruptedException {
        execIn(repo, "git", "init", "-q", "-b", "main");
        execIn(repo, "git", "config", "user.email", "modw-runner@openrewrite.org");
        execIn(repo, "git", "config", "user.name", "modw-runner");
        execIn(repo, "git", "add", "-A");
        execIn(repo, "git", "commit", "-q", "-m", "modw-runner-fixture");
        execIn(repo, "git", "remote", "add", "origin", originUrl);
    }

    private void writeActiveRecipe(Path cliHome, String recipeFqn, String classpath) throws IOException {
        // Active-recipe format consumed by io.moderne.cli.recipe.ActiveRecipe.load —
        // classpath is comma-separated paths, first entry treated as the recipe jar.
        java.util.Properties props = new java.util.Properties();
        props.setProperty("recipe", recipeFqn);
        String[] entries = classpath.split(java.io.File.pathSeparator);
        props.setProperty("classpath", String.join(",", entries));
        try (java.io.OutputStream out = Files.newOutputStream(cliHome.resolve("active.recipe"))) {
            props.store(out, "Written by org.openrewrite.test.ModwRunner");
        }
    }

    /**
     * Download the pinned {@code modw} wrapper into {@code destination} from the
     * Moderne Artifactory cache, falling back to Maven Central when Artifactory can't
     * serve it definitively.
     *
     * @return {@code true} if the wrapper was downloaded (or already cached);
     *         {@code false} if the version is not published, the signal callers use to
     *         skip the check.
     */
    private boolean downloadModw(String version, Path destination) throws IOException {
        String relPath = "io/moderne/moderne-cli/" + version + "/moderne-cli-" + version + "-modw.sh";
        Path cacheDir = Paths.get(System.getProperty("user.home"), ".gradle", "caches", "rewrite-modw");
        Path cache = cacheDir.resolve("moderne-cli-" + version + "-modw.sh");
        if (!Files.exists(cache)) {
            Files.createDirectories(cache.getParent());
            if (!fetchPreferringArtifactory(relPath, cache)) {
                return false;
            }
        }
        Files.copy(cache, destination, REPLACE_EXISTING);
        makeExecutable(destination);
        return true;
    }

    /**
     * Fetch {@code relPath} into {@code dest} from the Artifactory cache (less prone to
     * Maven Central rate-limiting), with Maven Central as fallback. A definitive
     * Artifactory 404 means "not published" and short-circuits with no Maven Central
     * consultation. Maven Central is consulted when Artifactory can't answer
     * definitively: a 401 (anonymous access can't read an uncached artifact and can't
     * trigger a remote proxy-fetch — so this means "not cached", not "not published"),
     * a 5xx, or an I/O error. Maven Central's 404 is then the authoritative "not
     * published" signal.
     *
     * @return {@code true} if downloaded; {@code false} if confirmed not published.
     */
    private static boolean fetchPreferringArtifactory(String relPath, Path dest) throws IOException {
        try {
            HttpURLConnection conn = openConnection(ARTIFACTORY_CACHE + "/" + relPath);
            int code = conn.getResponseCode();
            if (code == HttpURLConnection.HTTP_OK) {
                try (InputStream in = conn.getInputStream()) {
                    Files.copy(in, dest, REPLACE_EXISTING);
                }
                return true;
            }
            if (code == HttpURLConnection.HTTP_NOT_FOUND) {
                return false;
            }
            // 401 (uncached / proxy-fetch needs auth), 5xx, etc. — not a definitive
            // answer; consult Maven Central.
        } catch (IOException artifactoryUnavailable) {
            // Artifactory unreachable — try Maven Central.
        }
        return downloadFrom(MAVEN_CENTRAL + "/" + relPath, dest);
    }

    /** Download a single URL, returning {@code false} on 404 and throwing on other non-200s. */
    private static boolean downloadFrom(String url, Path dest) throws IOException {
        HttpURLConnection conn = openConnection(url);
        int code = conn.getResponseCode();
        if (code == HttpURLConnection.HTTP_NOT_FOUND) {
            return false;
        }
        if (code != HttpURLConnection.HTTP_OK) {
            throw new IOException("Failed to download " + url + " (HTTP " + code + ")");
        }
        try (InputStream in = conn.getInputStream()) {
            Files.copy(in, dest, REPLACE_EXISTING);
        }
        return true;
    }

    private static HttpURLConnection openConnection(String url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(30_000);
        conn.setReadTimeout(60_000);
        if (url.startsWith(ARTIFACTORY_CACHE)) {
            applyArtifactoryAuth(conn);
        }
        return conn;
    }

    /**
     * Add an {@code Authorization} header from the configured Artifactory credentials,
     * preferring a bearer token over basic username/password. No-op when none are set
     * (anonymous access — limited to already-cached artifacts).
     */
    private static void applyArtifactoryAuth(HttpURLConnection conn) {
        String token = trimToNull(System.getenv(WRAPPER_DIST_TOKEN_ENV));
        if (token != null) {
            conn.setRequestProperty("Authorization", "Bearer " + token);
            return;
        }
        String username = trimToNull(System.getenv(WRAPPER_DIST_USERNAME_ENV));
        String password = System.getenv(WRAPPER_DIST_PASSWORD_ENV);
        if (username != null && password != null) {
            String encoded = Base64.getEncoder().encodeToString(
                    (username + ":" + password).getBytes(StandardCharsets.UTF_8));
            conn.setRequestProperty("Authorization", "Basic " + encoded);
        }
    }

    private static @Nullable String trimToNull(@Nullable String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    /** Copy an environment variable from this JVM into the child process env when set. */
    private static void forwardIfPresent(Map<String, String> env, String name) {
        String value = System.getenv(name);
        if (value != null && !value.isEmpty()) {
            env.put(name, value);
        }
    }

    private static void makeExecutable(Path destination) throws IOException {
        try {
            Set<PosixFilePermission> perms = EnumSet.of(
                    PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE,
                    PosixFilePermission.OWNER_EXECUTE,
                    PosixFilePermission.GROUP_READ, PosixFilePermission.GROUP_EXECUTE,
                    PosixFilePermission.OTHERS_READ, PosixFilePermission.OTHERS_EXECUTE);
            Files.setPosixFilePermissions(destination, perms);
        } catch (UnsupportedOperationException ignored) {
        }
    }

    /**
     * Resolve the Artifactory URL for the platform-specific CLI <em>distribution</em>
     * (the ~150MB+ self-extracting archive the {@code modw} wrapper downloads on first
     * use), passed to the wrapper via {@code MODERNE_WRAPPER_DISTRIBUTION_URL} so the
     * heavy artifact is also sourced from Artifactory rather than Maven Central. Returns
     * {@code null} — letting the wrapper use its Maven Central default — for snapshot
     * versions (different layout), unknown platforms, or when Artifactory doesn't (yet)
     * serve the artifact, so the wrapper's own fallback still applies.
     */
    private static @Nullable String resolveArtifactoryDistributionUrl(String version) {
        if (version.endsWith("-SNAPSHOT")) {
            return null;
        }
        String artifactId = distributionArtifactId();
        if (artifactId == null) {
            return null;
        }
        String url = ARTIFACTORY_CACHE + "/io/moderne/" + artifactId + "/" + version +
                "/" + artifactId + "-" + version + ".sh";
        return httpHeadOk(url) ? url : null;
    }

    /**
     * The distribution artifactId for the current OS/arch, mirroring the {@code modw}
     * wrapper's own platform detection. {@code null} for platforms we don't map.
     */
    private static @Nullable String distributionArtifactId() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        if (os.contains("linux")) {
            if (arch.equals("amd64") || arch.equals("x86_64")) {
                return "moderne-cli-linux-x64";
            }
            if (arch.equals("aarch64") || arch.equals("arm64")) {
                return "moderne-cli-linux-aarch64";
            }
            return null;
        }
        if (os.contains("mac") || os.contains("darwin")) {
            return "moderne-cli-osx";
        }
        if (os.contains("windows")) {
            return "moderne-cli-windows";
        }
        return null;
    }

    private static boolean httpHeadOk(String url) {
        try {
            HttpURLConnection conn = openConnection(url);
            conn.setRequestMethod("HEAD");
            return conn.getResponseCode() == HttpURLConnection.HTTP_OK;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Configure a moderne-cli license key from {@link #LICENSE_KEY_ENV} when present.
     * No-op when the variable is unset or blank — the CLI runs unlicensed, which is
     * sufficient for the open-source fixture repositories these tests stage.
     */
    private void configureLicenseIfPresent(Path repoDir, Map<String, String> env, Path modwScript)
            throws IOException, InterruptedException {
        String licenseKey = licenseKey();
        if (licenseKey == null) {
            return;
        }
        System.out.println("[modw] Configuring CLI license key from $" + LICENSE_KEY_ENV);
        // Redact the key from any failure output so it never reaches CI logs. The
        // license is persisted into the per-test MODERNE_CLI_HOME config.
        exec(repoDir, env, Collections.singleton(licenseKey),
                modwScript.toString(), "config", "license", "edit", licenseKey);
    }

    /** The configured license key (trimmed), or {@code null} when unset/blank. */
    private static @Nullable String licenseKey() {
        String licenseKey = System.getenv(LICENSE_KEY_ENV);
        return licenseKey == null || licenseKey.trim().isEmpty() ? null : licenseKey.trim();
    }

    private void execIn(Path cwd, String... command) throws IOException, InterruptedException {
        exec(cwd, Collections.emptyMap(), Collections.emptySet(), command);
    }

    private void exec(Path cwd, Map<String, String> env, String... command)
            throws IOException, InterruptedException {
        exec(cwd, env, Collections.emptySet(), command);
    }

    private void exec(Path cwd, Map<String, String> env, Set<String> redactions, String... command)
            throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command).directory(cwd.toFile()).redirectErrorStream(true);
        pb.environment().putAll(env);
        Process p = pb.start();
        StringBuilder out = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(),
                StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                out.append(line).append('\n');
            }
        }
        int exit = p.waitFor();
        if (exit != 0) {
            throw new AssertionError("[modw] Command failed (exit " + exit + "): " +
                    redact(String.join(" ", command), redactions) +
                    "\nOutput:\n" + redact(out.toString(), redactions));
        }
    }

    private static String redact(String s, Set<String> redactions) {
        for (String secret : redactions) {
            if (!secret.isEmpty()) {
                s = s.replace(secret, "***");
            }
        }
        return s;
    }

    private static Path mostRecentSubdir(Path parent) throws IOException {
        if (!Files.isDirectory(parent)) {
            throw new AssertionError("[modw] Expected directory " + parent +
                    " to exist after `mod run`, but the CLI did not create it.");
        }
        try (Stream<Path> stream = Files.list(parent)) {
            return stream
                    .filter(Files::isDirectory)
                    .max(Comparator.comparing((Path p) -> {
                        try {
                            return Files.getLastModifiedTime(p);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    }))
                    .orElseThrow(() -> new AssertionError("[modw] No run directory in " + parent));
        }
    }

    // --- source-change translation --------------------------------------------

    private List<Result> buildResults(Path runDir, Map<Path, SourceFile> beforeByPath,
                                      Context context, Recipe recipe) throws IOException {
        Path afterFenced = runDir.resolve("after-fenced");
        if (!Files.isDirectory(afterFenced)) {
            return Collections.emptyList();
        }
        Map<Path, SourceSpec<?>> specByPath = new HashMap<>();
        for (SourceSpec<?> spec : context.getSourceSpecs()) {
            if (spec.getSourcePath() != null) {
                specByPath.put(spec.getSourcePath(), spec);
            }
        }
        // Fall back: index parsers by sourcePath of the parsed before files.
        // SourceSpec.sourcePath isn't always set explicitly; it's derived during
        // parsing and copied onto the SourceFile.
        List<SourceSpec<?>> specs = new ArrayList<>(context.getSourceSpecs());
        Map<Path, SourceSpec<?>> beforePathToSpec = matchSpecsToBefores(beforeByPath, specs);
        Map<Path, SourceSpec<?>> specByDerivedPath = new HashMap<>(beforePathToSpec);

        List<Result> results = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(afterFenced)) {
            for (Path after : (Iterable<Path>) stream::iterator) {
                if (!Files.isRegularFile(after)) continue;
                Path relative = afterFenced.relativize(after);
                SourceFile beforeLst = beforeByPath.get(relative);
                SourceSpec<?> spec = specByPath.getOrDefault(relative, specByDerivedPath.get(relative));
                String afterContent = new String(Files.readAllBytes(after), StandardCharsets.UTF_8);
                SourceFile afterLst = reparse(spec, beforeLst, relative, afterContent);
                results.add(new Result(beforeLst, afterLst,
                        Collections.singletonList(Collections.singletonList(recipe))));
            }
        }
        return results;
    }

    private static Map<Path, SourceSpec<?>> matchSpecsToBefores(Map<Path, SourceFile> beforeByPath,
                                                                List<SourceSpec<?>> specs) {
        // Best-effort: pair spec ordering with beforeByPath ordering. Spec.sourcePath
        // is normally null until the parser fills it in via SourceFile.getSourcePath(),
        // so we lean on the assumption that specs and beforeSources line up by index.
        Map<Path, SourceSpec<?>> out = new HashMap<>();
        Path[] paths = beforeByPath.keySet().toArray(new Path[0]);
        for (int i = 0; i < paths.length && i < specs.size(); i++) {
            out.put(paths[i], specs.get(i));
        }
        return out;
    }

    private SourceFile reparse(@Nullable SourceSpec<?> spec, @Nullable SourceFile before,
                               Path sourcePath, String content) {
        Parser parser = spec != null ? spec.getParser().build() :
                // No matching SourceSpec — usually means the recipe created a new file.
                // Fall back to PlainText so the test still sees a Result; assertions
                // that only care about source-change count or content equality work.
                // Tests that need typed-LST assertions on a generated file should
                // declare an explicit SourceSpec for the new path.
                new org.openrewrite.text.PlainTextParser();
        Parser.Input input = new Parser.Input(sourcePath,
                () -> new java.io.ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
        java.util.Optional<SourceFile> parsed = parser.parseInputs(
                        Collections.singletonList(input), null, new InMemoryExecutionContext())
                .findFirst();
        return parsed.orElseThrow(() -> new AssertionError(
                "[modw] Parser produced no result for " + sourcePath));
    }

    // --- data-table translation ------------------------------------------------

    private static final String DATATABLES = "datatables";

    private DataTableStore buildDataTableStore(Path runDir, Recipe recipe) throws IOException {
        ModwDataTableStore store = new ModwDataTableStore();
        Path datatables = runDir.resolve(DATATABLES);
        if (!Files.isDirectory(datatables)) {
            return store;
        }
        CsvMapper mapper = new CsvMapper();
        mapper.registerModule(new ParameterNamesModule());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try (Stream<Path> stream = Files.list(datatables)) {
            for (Path csv : (Iterable<Path>) stream::iterator) {
                String name = csv.getFileName().toString();
                if (!name.endsWith(".csv.gz")) continue;
                String fqn = name.substring(0, name.length() - ".csv.gz".length());
                Class<?> dtClass;
                try {
                    dtClass = Class.forName(fqn, false, recipe.getClass().getClassLoader());
                } catch (ClassNotFoundException e) {
                    // CLI emitted a data table whose class isn't on the test classpath.
                    // Common for OpenRewrite framework tables like RecipeRunStats —
                    // record raw rows under the FQN so spec.dataTable() still finds
                    // them if the user happens to depend on them.
                    store.putRaw(fqn, readRawRows(csv));
                    continue;
                }
                if (!DataTable.class.isAssignableFrom(dtClass)) continue;
                DataTable<?> dt = instantiateDataTable(dtClass, recipe);
                Class<?> rowType = dt.getType();
                List<Object> rows = readTypedRows(csv, rowType, mapper);
                store.put(dt, rows);
            }
        }
        return store;
    }

    private static DataTable<?> instantiateDataTable(Class<?> dtClass, Recipe recipe) {
        try {
            Constructor<?> ctor = dtClass.getDeclaredConstructor(Recipe.class);
            ctor.setAccessible(true);
            return (DataTable<?>) ctor.newInstance(recipe);
        } catch (NoSuchMethodException e) {
            throw new AssertionError("[modw] DataTable " + dtClass.getName() +
                    " does not have a (Recipe) constructor; cannot reconstruct it from the CLI run.", e);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("[modw] Failed to instantiate DataTable " + dtClass.getName(), e);
        }
    }

    private static List<Object> readTypedRows(Path csvGz, Class<?> rowType, CsvMapper mapper) throws IOException {
        String csv = readUndecoratedCsv(csvGz);
        if (csv.isEmpty()) return Collections.emptyList();
        CsvSchema schema = CsvSchema.emptySchema().withHeader();
        try (MappingIterator<?> it = mapper.readerFor(rowType).with(schema).readValues(csv)) {
            List<Object> out = new ArrayList<>();
            while (it.hasNext()) {
                out.add(it.next());
            }
            return out;
        }
    }

    private static List<Object> readRawRows(Path csvGz) throws IOException {
        // Fallback when we can't load the DataTable class. Each row is a
        // Map<String, String> keyed by column name.
        String csv = readUndecoratedCsv(csvGz);
        if (csv.isEmpty()) return Collections.emptyList();
        CsvMapper mapper = new CsvMapper();
        CsvSchema schema = CsvSchema.emptySchema().withHeader();
        try (MappingIterator<Map<String, String>> it = mapper
                .readerFor(Map.class).with(schema).readValues(csv)) {
            List<Object> out = new ArrayList<>();
            while (it.hasNext()) {
                out.add(it.next());
            }
            return out;
        }
    }

    private static String readUndecoratedCsv(Path csvGz) throws IOException {
        StringBuilder csv = new StringBuilder();
        try (InputStream raw = Files.newInputStream(csvGz);
             GZIPInputStream gz = new GZIPInputStream(raw);
             BufferedReader r = new BufferedReader(new InputStreamReader(gz, StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                if (line.startsWith("#")) continue;
                csv.append(line).append('\n');
            }
        }
        return csv.toString();
    }

    private static void deleteRecursively(Path path) throws IOException {
        if (!Files.exists(path)) return;
        try (Stream<Path> stream = Files.walk(path)) {
            stream.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }
    }

    private static final class ListChangeset implements Changeset {
        private final List<Result> results;

        ListChangeset(List<Result> results) {
            this.results = results;
        }

        @Override
        public int size() {
            return results.size();
        }

        @Override
        public List<Result> getPage(int start, int count) {
            return results.subList(start, Math.min(start + count, results.size()));
        }
    }

    private static final class ModwDataTableStore implements DataTableStore {
        private final List<DataTable<?>> tables = new ArrayList<>();
        private final Map<String, List<Object>> rowsByName = new LinkedHashMap<>();

        void put(DataTable<?> dt, List<Object> rows) {
            tables.add(dt);
            rowsByName.put(dt.getName(), rows);
        }

        void putRaw(String name, List<Object> rows) {
            rowsByName.put(name, rows);
        }

        @Override
        public <Row> void insertRow(DataTable<Row> dataTable, ExecutionContext ctx, Row row) {
            throw new UnsupportedOperationException(
                    "[modw] ModwDataTableStore is read-only; rows are loaded from the CLI run output.");
        }

        @Override
        public Stream<?> getRows(String dataTableName, @Nullable String group) {
            return rowsByName.getOrDefault(dataTableName, Collections.emptyList()).stream();
        }

        @Override
        public Collection<DataTable<?>> getDataTables() {
            return Collections.unmodifiableList(tables);
        }
    }
}
