/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.python.internal;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.python.PipfileParser;
import org.openrewrite.python.PyProjectTomlParser;
import org.openrewrite.python.RequirementsTxtParser;
import org.openrewrite.python.internal.pep440.PythonVersion;
import org.openrewrite.python.internal.pep440.PythonVersionSpecifierSet;
import org.openrewrite.python.internal.pep508.Pep508Requirement;
import org.openrewrite.python.marker.PythonResolutionResult;
import org.openrewrite.python.marker.PythonResolutionResult.Dependency;
import org.openrewrite.python.marker.PythonResolutionResult.ResolvedDependency;
import org.openrewrite.python.marker.PythonResolutionResult.SourceIndex;
import org.openrewrite.toml.TomlParser;
import org.openrewrite.toml.tree.Toml;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.util.Collections.*;
import static java.util.stream.Collectors.toList;

/**
 * The manifest that declares a Python project's dependencies. {@link #read} takes the requirements a
 * dependency environment is installed from out of the same parse that computes the manifest's marker.
 */
public enum DependencyManifest {
    PyProjectToml("pyproject.toml"),
    Pipfile("Pipfile"),
    RequirementsFiles("requirements files"),
    SetupPy("setup.py"),
    SetupCfg("setup.cfg");

    private static final Pattern REQUIREMENTS_OPTION = Pattern.compile(
            "(?<option>-[rcef]|--requirement|--constraint|--editable|--find-links)(?:\\s*=\\s*|\\s+)?(?<value>\\S+).*");
    private static final Pattern VCS = Pattern.compile("(?:git|hg|svn|bzr)\\+.*");
    private static final Pattern EXTRAS = Pattern.compile("\\[(?<extras>[^]]*)]$");
    private static final Pattern VARIABLE = Pattern.compile("\\$\\{(\\w+)}");
    private static final Pattern INDEX_NAME = Pattern.compile("[A-Za-z0-9_-]+");
    private static final List<String> PIPFILE_LOCAL_KEYS = Arrays.asList("path", "file", "git", "hg", "svn", "bzr");

    /**
     * The env's Python is the lowest from 3.12 up that the project allows, since a project's pins more often
     * lack wheels for the newest Pythons than for 3.12. When the project excludes all of those, the newest
     * Python it allows below 3.12 is taken.
     */
    private static final int FIRST_MINOR = 12;
    /**
     * The oldest CPython uv downloads. typeshed's stubs have no branch for a Python below 3.10, so the standard
     * library of a 3.8 or 3.9 env is typed approximately. The env's third-party packages resolve as installed.
     */
    private static final int OLDEST_MINOR = 8;
    private static final int NEWEST_MINOR = 20;

    /**
     * How a host names the manifest in a log line, such as "requirements files".
     */
    @Getter
    private final String description;

    DependencyManifest(String description) {
        this.description = description;
    }

    /**
     * Reads the requirements a setup.py declares, with the extras named, which takes running it.
     */
    @FunctionalInterface
    public interface SetupPyReader {
        List<String> read(Path projectDir, List<String> extras);
    }

    /**
     * The input of one uv install into a dependency environment.
     */
    @Value
    @Builder
    public static class Requirements {
        DependencyManifest manifest;

        /**
         * The lines of the requirements file the env is installed from.
         */
        @Singular
        List<String> roots;

        /**
         * The lock's pins, as {@code name==version}, and the constraints the project declares.
         */
        @Singular
        List<String> constraints;

        @Singular
        List<String> overrides;

        /**
         * The canonical names of packages the env goes without, because they come from a path, a
         * repository or an index the env is not installed from.
         */
        @Singular
        Set<String> excludes;

        /**
         * The project's package indexes as uv names them in {@code UV_INDEX}, which may carry credentials.
         */
        @Singular
        List<String> indexes;

        /**
         * The version specifiers the env's Python has to satisfy.
         */
        @Singular("requiresPython")
        List<String> requiresPython;

        // declared so that javadoc, which reads the source before Lombok, finds it
        public static class RequirementsBuilder {
        }
    }

    /**
     * A pyproject.toml comes first when it lists dependencies. Requirements files come before setup.py,
     * since reading them runs no project code. A {@code [project]} table with no other manifest still gets
     * an env without roots, while tool settings alone get none.
     */
    public static @Nullable DependencyManifest choose(Path projectDir) {
        Toml.Document pyproject = parse(new TomlParser(), projectDir.resolve("pyproject.toml"), null);
        PythonResolutionResult marker = pyproject == null ? null : PythonDependencyParser.createMarker(pyproject, null);
        if (marker != null && !pyprojectRoots(marker, new UvSettings(pyproject), emptyList()).isEmpty()) {
            return PyProjectToml;
        }
        if (Files.isRegularFile(projectDir.resolve("Pipfile"))) {
            return Pipfile;
        }
        if (!requirementsFiles(projectDir).isEmpty()) {
            return RequirementsFiles;
        }
        if (Files.isRegularFile(projectDir.resolve("setup.py"))) {
            return SetupPy;
        }
        if (Files.isRegularFile(projectDir.resolve("setup.cfg"))) {
            return SetupCfg;
        }
        return marker != null ? PyProjectToml : null;
    }

    /**
     * @param boundary    where the search for a lock above the project stops, as for the dependency markers
     * @param environment the environment that fills the variables in an index's URL
     */
    public Requirements read(Path projectDir, @Nullable Path boundary, Map<String, String> environment,
                             SetupPyReader setupPy) {
        return read(projectDir, boundary, environment, setupPy, emptyList());
    }

    private Requirements read(Path projectDir, @Nullable Path boundary, Map<String, String> environment,
                              SetupPyReader setupPy, List<String> extras) {
        switch (this) {
            case PyProjectToml:
                return pyproject(projectDir, boundary, environment, extras);
            case Pipfile:
                return pipfile(projectDir, boundary, environment);
            case RequirementsFiles:
                Requirements.RequirementsBuilder requirements = Requirements.builder().manifest(RequirementsFiles);
                Set<Path> visited = new HashSet<>();
                for (Path file : requirementsFiles(projectDir)) {
                    Path absolute = file.toAbsolutePath().normalize();
                    if (visited.add(absolute)) {
                        flatten(absolute, readString(absolute), requirements, visited, environment, setupPy);
                    }
                }
                return requirements.build();
            case SetupPy:
                return Requirements.builder().manifest(SetupPy).roots(setupPy.read(projectDir, extras)).build();
            case SetupCfg:
            default:
                Path setupCfg = projectDir.resolve("setup.cfg");
                return ofSetupCfg(Files.isRegularFile(setupCfg) ? readString(setupCfg) : "", projectDir);
        }
    }

    /**
     * The requirements of a single requirements file, read as {@link #RequirementsFiles} reads each of a project's.
     *
     * @param file where the file sits on disk, which its includes and local paths are relative to
     */
    public static Requirements ofRequirementsFile(String content, @Nullable Path file, Map<String, String> environment,
                                                  SetupPyReader setupPy) {
        Requirements.RequirementsBuilder requirements = Requirements.builder().manifest(RequirementsFiles);
        Set<Path> visited = new HashSet<>();
        Path absolute = file == null ? null : file.toAbsolutePath().normalize();
        if (absolute != null) {
            visited.add(absolute);
        }
        flatten(absolute, content, requirements, visited, environment, setupPy);
        return requirements.build();
    }

    /**
     * The requirements a setup.cfg's {@code install_requires} declares. They are static, so a project
     * without a setup.py, which uv cannot build, is read without running anything.
     *
     * @param projectDir where the project sits on disk, which an {@code install_requires = file:} is
     *                   relative to, or null when it has no files
     */
    public static Requirements ofSetupCfg(String content, @Nullable Path projectDir) {
        Map<String, String> options = readIni(content).getOrDefault("options", emptyMap());
        List<String> roots = new ArrayList<>();
        String installRequires = options.get("install_requires");
        if (installRequires != null) {
            String value = installRequires.trim();
            if (value.startsWith("file:")) {
                for (String file : value.substring("file:".length()).split(",")) {
                    Path requirements = projectDir == null ? null : projectDir.resolve(file.trim());
                    if (requirements != null && Files.isRegularFile(requirements)) {
                        addSetuptoolsList(readString(requirements), roots);
                    }
                }
            } else {
                addSetuptoolsList(installRequires, roots);
            }
        }
        roots.removeIf(root -> localPath(root) != null);
        Requirements.RequirementsBuilder requirements = Requirements.builder().manifest(SetupCfg).roots(roots);
        String pythonRequires = options.get("python_requires");
        if (pythonRequires != null) {
            requirements.requiresPython(pythonRequires.trim());
        }
        return requirements.build();
    }

    /**
     * The Python minor version, such as {@code 3.12}, to build an env with that satisfies all of
     * {@code requiresPython}, or null when none from 3.8 up does.
     */
    public static @Nullable String pythonVersion(List<String> requiresPython) {
        List<PythonVersionSpecifierSet> specifiers = new ArrayList<>();
        for (String specifier : requiresPython) {
            PythonVersionSpecifierSet parsed = PythonVersionSpecifierSet.parse(specifier);
            if (parsed != null) {
                specifiers.add(parsed);
            }
        }
        for (int minor = FIRST_MINOR; minor <= NEWEST_MINOR; minor++) {
            if (allows(specifiers, minor)) {
                return "3." + minor;
            }
        }
        for (int minor = FIRST_MINOR - 1; minor >= OLDEST_MINOR; minor--) {
            if (allows(specifiers, minor)) {
                return "3." + minor;
            }
        }
        return null;
    }

    /**
     * Whether some patch release of {@code 3.<minor>} satisfies all specifiers, judged by the first and a
     * late patch since specifiers rarely bound a patch from both sides.
     */
    private static boolean allows(List<PythonVersionSpecifierSet> specifiers, int minor) {
        for (String patch : Arrays.asList("0", "99")) {
            PythonVersion version = PythonVersion.parse("3." + minor + "." + patch);
            if (version != null && specifiers.stream().allMatch(s -> s.contains(version))) {
                return true;
            }
        }
        return false;
    }

    private static List<Path> requirementsFiles(Path projectDir) {
        if (!Files.isDirectory(projectDir)) {
            return emptyList();
        }
        try (Stream<Path> files = Files.list(projectDir)) {
            return files.filter(RequirementsTxtParser::isRequirementsFile)
                    .filter(Files::isRegularFile)
                    .sorted()
                    .collect(toList());
        } catch (IOException e) {
            return emptyList();
        }
    }

    /**
     * The {@code [tool.uv]} settings that decide which of a pyproject's dependencies the env installs.
     */
    private static final class UvSettings {
        /**
         * The dependency groups uv installs by default, or null for all of them.
         */
        final @Nullable List<String> defaultGroups;

        /**
         * The packages {@code [tool.uv.sources]} takes from a path, a repository or an explicit index.
         */
        final Set<String> excludes = new LinkedHashSet<>();

        UvSettings(Toml.Document pyproject) {
            Map<String, Object> uv = map(map(PyprojectData.toNestedMap(pyproject).get("tool")).get("uv"));
            Object groups = uv.get("default-groups");
            defaultGroups = "all".equals(groups) ? null :
                    groups instanceof List ? strings(groups) : singletonList("dev");
            Set<String> explicitIndexes = new HashSet<>();
            for (Object index : list(uv.get("index"))) {
                if (Boolean.TRUE.equals(map(index).get("explicit"))) {
                    explicitIndexes.add(String.valueOf(map(index).get("name")));
                }
            }
            for (Map.Entry<String, Object> source : map(uv.get("sources")).entrySet()) {
                Object value = source.getValue();
                for (Object table : value instanceof List ? (List<?>) value : singletonList(value)) {
                    String index = string(map(table).get("index"));
                    if (index == null || explicitIndexes.contains(index)) {
                        excludes.add(Pep508Requirement.canonicalize(source.getKey()));
                    }
                }
            }
        }
    }

    /**
     * The project's dependencies plus the dependency groups uv installs by default, since a parse covers
     * test code too, and the optional dependencies of the extras named.
     */
    private static List<String> pyprojectRoots(PythonResolutionResult marker, UvSettings settings,
                                               List<String> extras) {
        List<Dependency> declared = new ArrayList<>(marker.getDependencies());
        Collection<String> groups = settings.defaultGroups == null ? marker.getDependencyGroups().keySet() :
                settings.defaultGroups;
        for (String group : groups) {
            declared.addAll(marker.getDependencyGroups().getOrDefault(group, emptyList()));
        }
        for (String extra : extras) {
            declared.addAll(marker.getOptionalDependencies().getOrDefault(extra, emptyList()));
        }
        return roots(declared, settings.excludes);
    }

    private static Requirements pyproject(Path projectDir, @Nullable Path boundary, Map<String, String> environment,
                                          List<String> extras) {
        Toml.Document pyproject = parse(new PyProjectTomlParser(), projectDir.resolve("pyproject.toml"), boundary);
        PythonResolutionResult marker = marker(pyproject);
        Requirements.RequirementsBuilder requirements = Requirements.builder().manifest(PyProjectToml);
        if (pyproject == null || marker == null) {
            return requirements.build();
        }
        UvSettings settings = new UvSettings(pyproject);
        requirements.roots(pyprojectRoots(marker, settings, extras))
                .excludes(settings.excludes)
                .constraints(pins(marker.getResolvedDependencies()))
                .constraints(requirementLines(marker.getConstraintDependencies()))
                .overrides(requirementLines(marker.getOverrideDependencies()))
                .indexes(indexes(marker.getSourceIndexes(), environment));
        if (marker.getRequiresPython() != null) {
            requirements.requiresPython(marker.getRequiresPython());
        }
        return requirements.build();
    }

    private static Requirements pipfile(Path projectDir, @Nullable Path boundary, Map<String, String> environment) {
        Toml.Document pipfile = parse(new PipfileParser(), projectDir.resolve("Pipfile"), boundary);
        PythonResolutionResult marker = marker(pipfile);
        Requirements.RequirementsBuilder requirements = Requirements.builder().manifest(Pipfile);
        if (pipfile == null || marker == null) {
            return requirements.build();
        }
        Set<String> excludes = new LinkedHashSet<>();
        Map<String, Object> sections = PyprojectData.toNestedMap(pipfile);
        for (String section : Arrays.asList("packages", "dev-packages")) {
            for (Map.Entry<String, Object> pkg : map(sections.get(section)).entrySet()) {
                if (PIPFILE_LOCAL_KEYS.stream().anyMatch(map(pkg.getValue())::containsKey)) {
                    excludes.add(Pep508Requirement.canonicalize(pkg.getKey()));
                }
            }
        }
        List<Dependency> declared = new ArrayList<>(marker.getDependencies());
        declared.addAll(marker.getOptionalDependencies().getOrDefault("dev-packages", emptyList()));
        requirements.roots(roots(declared, excludes))
                .excludes(excludes)
                .constraints(pins(marker.getResolvedDependencies()))
                .indexes(indexes(marker.getSourceIndexes(), environment));
        if (marker.getRequiresPython() != null) {
            requirements.requiresPython("==" + marker.getRequiresPython() + ".*");
        }
        return requirements.build();
    }

    private static List<String> roots(List<Dependency> declared, Set<String> excludes) {
        List<String> roots = new ArrayList<>();
        for (Dependency dependency : declared) {
            String requirement = requirement(dependency);
            if (!excludes.contains(Pep508Requirement.canonicalize(dependency.getName())) &&
                localPath(requirement) == null) {
                roots.add(requirement);
            }
        }
        return roots;
    }

    /**
     * A package a lock resolves to more than one version, one per fork, is left unpinned, since a
     * constraint cannot express that.
     */
    private static List<String> pins(List<ResolvedDependency> resolved) {
        Map<String, Set<String>> versions = new LinkedHashMap<>();
        for (ResolvedDependency dependency : resolved) {
            versions.computeIfAbsent(Pep508Requirement.canonicalize(dependency.getName()), n -> new LinkedHashSet<>())
                    .add(dependency.getVersion());
        }
        List<String> pins = new ArrayList<>();
        for (Map.Entry<String, Set<String>> entry : versions.entrySet()) {
            if (entry.getValue().size() == 1) {
                pins.add(entry.getKey() + "==" + entry.getValue().iterator().next());
            }
        }
        return pins;
    }

    /**
     * The indexes in uv's {@code name=url} form, leaving out PyPI, which the default index already stands
     * for. An index whose URL needs a variable the environment lacks fails the read, since installing
     * from the default index instead could take a public package for a private one of the same name.
     */
    private static List<String> indexes(@Nullable List<SourceIndex> sourceIndexes, Map<String, String> environment) {
        List<String> indexes = new ArrayList<>();
        for (SourceIndex index : sourceIndexes == null ? Collections.<SourceIndex>emptyList() : sourceIndexes) {
            String host = host(index.getUrl());
            if (host == null || "pypi.org".equals(host) || "pypi.python.org".equals(host)) {
                continue;
            }
            Matcher variable = VARIABLE.matcher(index.getUrl());
            StringBuffer url = new StringBuffer();
            while (variable.find()) {
                String value = environment.containsKey(variable.group(1)) ? environment.get(variable.group(1)) :
                        System.getenv(variable.group(1));
                if (value == null) {
                    throw new IllegalStateException("The package index " + index.getName() + " needs ${" +
                                                    variable.group(1) + "}, which the environment does not set");
                }
                variable.appendReplacement(url, Matcher.quoteReplacement(value));
            }
            variable.appendTail(url);
            indexes.add(INDEX_NAME.matcher(index.getName()).matches() ? index.getName() + "=" + url : url.toString());
        }
        return indexes;
    }

    private static @Nullable String host(String url) {
        try {
            return URI.create(VARIABLE.matcher(url).replaceAll("x")).getHost();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Writes the requirements of {@code content} into {@code requirements}. Includes are inlined, and the
     * paths of constraints and wheel directories resolved, since they are relative to the file naming them.
     * A line installing a local project is replaced by that project's own requirements, so a {@code -e .}
     * brings in what setup.py declares.
     */
    private static void flatten(@Nullable Path file, String content, Requirements.RequirementsBuilder requirements,
                                Set<Path> visited, Map<String, String> environment, SetupPyReader setupPy) {
        Path base = file == null ? null : file.getParent();
        for (String rawLine : content.split("\\r?\\n")) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            Matcher option = REQUIREMENTS_OPTION.matcher(line);
            if (option.matches()) {
                String value = option.group("value");
                Path path = resolve(base, value);
                switch (option.group("option")) {
                    case "-r":
                    case "--requirement":
                        if (path != null && Files.isRegularFile(path) && visited.add(path)) {
                            flatten(path, readString(path), requirements, visited, environment, setupPy);
                        }
                        break;
                    case "-c":
                    case "--constraint":
                        if (path != null && Files.isRegularFile(path)) {
                            for (String constraint : readString(path).split("\\r?\\n")) {
                                if (!constraint.trim().isEmpty() && !constraint.trim().startsWith("#")) {
                                    requirements.constraint(constraint.trim());
                                }
                            }
                        }
                        break;
                    case "-f":
                    case "--find-links":
                        if (value.contains("://")) {
                            requirements.root(line);
                        } else if (path != null && Files.isDirectory(path)) {
                            requirements.root("--find-links " + path);
                        }
                        break;
                    default:
                        if (VCS.matcher(value).matches()) {
                            // uv refuses an editable install of a repository, so it goes in as a regular one
                            requirements.root(value);
                        } else {
                            addLocalProject(value, base, requirements, visited, environment, setupPy);
                        }
                }
            } else if (line.startsWith("-")) {
                requirements.root(line);
            } else {
                String path = localPath(line);
                if (path == null) {
                    requirements.root(line);
                } else {
                    addLocalProject(path, base, requirements, visited, environment, setupPy);
                }
            }
        }
    }

    private static void addLocalProject(String target, @Nullable Path base,
                                        Requirements.RequirementsBuilder requirements, Set<Path> visited,
                                        Map<String, String> environment, SetupPyReader setupPy) {
        Path projectDir = resolve(base, target);
        if (projectDir == null || !Files.isDirectory(projectDir) || !visited.add(projectDir)) {
            return;
        }
        Toml.Document pyproject = parse(new TomlParser(), projectDir.resolve("pyproject.toml"), null);
        PythonResolutionResult marker = pyproject == null ? null : PythonDependencyParser.createMarker(pyproject, null);
        DependencyManifest manifest = marker != null && !marker.getDependencies().isEmpty() ? PyProjectToml :
                Files.isRegularFile(projectDir.resolve("setup.py")) ? SetupPy :
                        Files.isRegularFile(projectDir.resolve("setup.cfg")) ? SetupCfg : null;
        if (manifest == null) {
            return;
        }
        Matcher extras = EXTRAS.matcher(target.replaceFirst("#.*$", ""));
        Requirements local = manifest.read(projectDir, projectDir, environment, setupPy, extras.find() ?
                Arrays.stream(extras.group("extras").split(",")).map(String::trim).collect(toList()) : emptyList());
        requirements.roots(local.getRoots())
                .constraints(local.getConstraints())
                .overrides(local.getOverrides())
                .excludes(local.getExcludes())
                .indexes(local.getIndexes());
    }

    /**
     * The path a requirement installs from, such as {@code .}, {@code ./lib} or the one in
     * {@code name @ file:///lib}, or null when it names a distribution or a remote URL.
     */
    static @Nullable String localPath(String requirement) {
        Pep508Requirement parsed = Pep508Requirement.parse(requirement);
        String target = parsed != null && parsed.getUrl() != null ? parsed.getUrl() :
                requirement.trim().split("[\\s;]", 2)[0];
        if (target.startsWith("file:")) {
            return target;
        }
        if (parsed != null && parsed.getUrl() == null || target.contains("://") || VCS.matcher(target).matches()) {
            return null;
        }
        return target.startsWith(".") || target.contains("/") || target.contains("\\") ? target : null;
    }

    private static @Nullable Path resolve(@Nullable Path base, String value) {
        String path = value.replaceFirst("#.*$", "").replaceFirst("\\[[^]]*]$", "");
        try {
            if (path.startsWith("file:")) {
                return Paths.get(URI.create(path.startsWith("file://") ? path : "file://" +
                        (base == null ? "" : base + "/") + path.substring("file:".length()))).normalize();
            }
            Path resolved = Paths.get(path);
            if (resolved.isAbsolute()) {
                return resolved.normalize();
            }
            return base == null ? null : base.resolve(resolved).normalize();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Splits a value the way setuptools splits a list of requirements: one per line, or by
     * semicolons when it is all on one line.
     */
    private static void addSetuptoolsList(String value, List<String> roots) {
        String trimmed = value.trim();
        for (String item : trimmed.contains("\n") ? trimmed.split("\\r?\\n") : trimmed.split(";")) {
            String requirement = item.trim();
            if (!requirement.isEmpty() && !requirement.startsWith("#")) {
                roots.add(requirement);
            }
        }
    }

    private static Map<String, Map<String, String>> readIni(String content) {
        Map<String, Map<String, String>> sections = new LinkedHashMap<>();
        Map<String, String> section = null;
        String key = null;
        for (String line : content.split("\\r?\\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith(";")) {
                continue;
            }
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                section = sections.computeIfAbsent(trimmed.substring(1, trimmed.length() - 1).trim(),
                        s -> new LinkedHashMap<>());
                key = null;
            } else if (section != null && Character.isWhitespace(line.charAt(0)) && key != null) {
                section.merge(key, trimmed, (a, b) -> a.isEmpty() ? b : a + "\n" + b);
            } else if (section != null) {
                int equals = trimmed.indexOf('=');
                int colon = trimmed.indexOf(':');
                int separator = equals < 0 ? colon : colon < 0 ? equals : Math.min(equals, colon);
                if (separator > 0) {
                    key = trimmed.substring(0, separator).trim();
                    section.put(key, trimmed.substring(separator + 1).trim());
                }
            }
        }
        return sections;
    }

    private static List<String> requirementLines(List<Dependency> dependencies) {
        List<String> lines = new ArrayList<>(dependencies.size());
        for (Dependency dependency : dependencies) {
            lines.add(requirement(dependency));
        }
        return lines;
    }

    private static String requirement(Dependency dependency) {
        StringBuilder requirement = new StringBuilder(dependency.getName());
        if (dependency.getExtras() != null && !dependency.getExtras().isEmpty()) {
            requirement.append('[').append(String.join(",", dependency.getExtras())).append(']');
        }
        if (dependency.getVersionConstraint() != null) {
            requirement.append(dependency.getVersionConstraint());
        }
        if (dependency.getMarker() != null) {
            requirement.append(" ; ").append(dependency.getMarker());
        }
        return requirement.toString();
    }

    private static @Nullable PythonResolutionResult marker(Toml.@Nullable Document manifest) {
        return manifest == null ? null : manifest.getMarkers().findFirst(PythonResolutionResult.class).orElse(null);
    }

    private static Toml.@Nullable Document parse(Parser parser, Path file, @Nullable Path relativeTo) {
        if (!Files.isRegularFile(file)) {
            return null;
        }
        return parser.parseInputs(singletonList(Parser.Input.fromFile(file)), relativeTo,
                        new InMemoryExecutionContext())
                .findFirst()
                .filter(Toml.Document.class::isInstance)
                .map(Toml.Document.class::cast)
                .orElse(null);
    }

    private static String readString(Path file) {
        try {
            return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(@Nullable Object value) {
        return value instanceof Map ? (Map<String, Object>) value : emptyMap();
    }

    private static List<?> list(@Nullable Object value) {
        return value instanceof List ? (List<?>) value : emptyList();
    }

    private static List<String> strings(@Nullable Object value) {
        List<String> strings = new ArrayList<>();
        for (Object item : list(value)) {
            if (item instanceof String) {
                strings.add((String) item);
            }
        }
        return strings;
    }

    private static @Nullable String string(@Nullable Object value) {
        return value instanceof String ? (String) value : null;
    }
}
