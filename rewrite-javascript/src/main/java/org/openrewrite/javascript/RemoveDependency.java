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
package org.openrewrite.javascript;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.java.tree.J;
import org.openrewrite.javascript.internal.NodeDependencyEdits;
import org.openrewrite.javascript.internal.NodeDependencyScan;
import org.openrewrite.javascript.internal.PackageJsonHelper;
import org.openrewrite.javascript.table.NodeDependencyEditsDeclined;
import org.openrewrite.javascript.table.NodeLockRegenerationFailures;
import org.openrewrite.javascript.tree.JS;
import org.openrewrite.text.PlainText;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@EqualsAndHashCode(callSuper = false)
@Value
public class RemoveDependency extends ScanningRecipe<RemoveDependency.Accumulator> {

    // the specifiers of import and export declarations, require() and import() calls, in a file not parsed as code
    private static final Pattern SPECIFIER = Pattern.compile(
            "(?:\\bfrom|\\bimport|\\brequire\\s*\\()\\s*\\(?\\s*(['\"`])([^'\"`\\r\\n]+)\\1");

    private static final Set<String> SOURCE_EXTENSIONS = new HashSet<>(Arrays.asList(
            "js", "jsx", "mjs", "cjs", "ts", "tsx", "mts", "cts"));

    transient NodeLockRegenerationFailures lockRegenerationFailures = new NodeLockRegenerationFailures(this);
    transient NodeDependencyEditsDeclined editsDeclined = new NodeDependencyEditsDeclined(this);

    @Option(displayName = "Package name",
            description = "The name of the npm package to remove (e.g., `lodash`, `@types/node`).",
            example = "lodash")
    String packageName;

    @Option(displayName = "Scope",
            description = "The dependency scope to remove from: `dependencies`, `devDependencies`, " +
                    "`peerDependencies`, `optionalDependencies`, or `bundledDependencies`. If not specified, removes " +
                    "from all scopes.",
            valid = {"dependencies", "devDependencies", "peerDependencies", "optionalDependencies",
                    "bundledDependencies"},
            example = "dependencies",
            required = false)
    @Nullable String scope;

    @Option(displayName = "Unless importing",
            description = "Keep the dependency in a `package.json` while a JavaScript or TypeScript source still " +
                    "imports a package matching one of these glob patterns, by `import`, `export ... from`, " +
                    "`require()` or `import()`. A pattern matches the package's name, so `lodash` covers " +
                    "`lodash/fp`. The `package.json` kept is the one Node resolves the dependency from for that " +
                    "source: the nearest one above it that declares the dependency. Imports are read as they stand " +
                    "before the recipes in a cycle edit them, so a migration that rewrites the imports and removes " +
                    "the dependency in one run causes another cycle, in which the dependency goes where no import " +
                    "is left. Leave unset to remove the dependency regardless of imports.",
            example = "lodash",
            required = false)
    @Nullable List<String> unlessImporting;

    public RemoveDependency(String packageName, @Nullable String scope) {
        this(packageName, scope, null);
    }

    @JsonCreator
    public RemoveDependency(String packageName, @Nullable String scope, @Nullable List<String> unlessImporting) {
        this.packageName = packageName;
        this.scope = scope;
        this.unlessImporting = unlessImporting;
    }

    @Override public String getDisplayName() { return "Remove npm dependency"; }
    @Override public String getInstanceNameSuffix() { return String.format("`%s`", packageName); }

    @Override public String getDescription() {
        return "Remove an npm dependency from `package.json` and regenerate the lock file natively, without " +
                "executing the package manager. A pnpm workspace with `cleanupUnusedCatalogs` also loses the catalog " +
                "entries no project references any more. If the dependency does not exist in any scope, the recipe " +
                "is a no-op.";
    }

    public static final class Accumulator {
        final NodeDependencyEdits edits;

        // the sources, as scanned, that import a package matching unlessImporting
        final Set<Path> importing = new HashSet<>();

        // the manifests those sources resolve the dependency from, once the scan is done
        @Nullable Set<Path> kept;

        Accumulator(NodeDependencyEdits edits) {
            this.edits = edits;
        }
    }

    @Override
    public Accumulator getInitialValue(ExecutionContext ctx) {
        return new Accumulator(new NodeDependencyEdits(NodeDependencyEdits.rows(lockRegenerationFailures,
                editsDeclined, packageName)));
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
        List<Pattern> imported = importedPatterns();
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override public Tree preVisit(Tree tree, ExecutionContext ctx) {
                stopAfterPreVisit();
                if (tree instanceof SourceFile) {
                    SourceFile sf = (SourceFile) tree;
                    acc.edits.scan(sf);
                    if (!imported.isEmpty() && !inNodeModules(sf.getSourcePath()) &&
                        imports(sf, imported)) {
                        acc.importing.add(sf.getSourcePath());
                    }
                }
                return tree;
            }
        };
    }

    @Override
    public Collection<? extends SourceFile> generate(Accumulator acc, ExecutionContext ctx) {
        return acc.edits.decideAll(ctx, (decision, c) -> decide(acc, decision, c));
    }

    private void decide(Accumulator acc, NodeDependencyEdits.Decision decision, ExecutionContext ctx) {
        if (kept(acc).contains(decision.getPath())) {
            return;
        }
        Set<String> scopes = new LinkedHashSet<>();
        for (Map.Entry<String, Map<String, String>> declared :
                PackageJsonHelper.declaredDependencies(decision.getManifest()).entrySet()) {
            if (declared.getValue().containsKey(packageName) && (scope == null || scope.equals(declared.getKey()))) {
                scopes.add(declared.getKey());
            }
        }
        if (!scopes.isEmpty()) {
            decision.editManifest(doc -> PackageJsonHelper.removeDependency(doc, packageName, scopes));
        }
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        return acc.edits.visitor((decision, ctx) -> decide(acc, decision, ctx));
    }

    private List<Pattern> importedPatterns() {
        if (unlessImporting == null) {
            return Collections.emptyList();
        }
        List<Pattern> patterns = new ArrayList<>(unlessImporting.size());
        for (String glob : unlessImporting) {
            if (!glob.trim().isEmpty()) {
                patterns.add(PackageJsonHelper.compileGlobPattern(glob.trim()));
            }
        }
        return patterns;
    }

    // the manifest each importing source resolves the dependency from: the nearest above it declaring it
    private Set<Path> kept(Accumulator acc) {
        if (acc.kept == null) {
            Set<Path> kept = new HashSet<>();
            for (Path source : acc.importing) {
                Path declaring = declaring(acc, source);
                if (declaring != null) {
                    kept.add(declaring);
                }
            }
            acc.kept = kept;
        }
        return acc.kept;
    }

    private @Nullable Path declaring(Accumulator acc, Path source) {
        for (Path dir = source.getParent(); ; dir = dir.getParent()) {
            Path manifest = dir == null ? Paths.get("package.json") : dir.resolve("package.json");
            NodeDependencyScan.ProjectState ps = acc.edits.scan.projects.get(manifest);
            if (ps != null && ps.capturedPackageJson != null) {
                for (Map<String, String> declared : ps.capturedPackageJson.getDeclared().values()) {
                    if (declared.containsKey(packageName)) {
                        return manifest;
                    }
                }
            }
            if (dir == null) {
                return null;
            }
        }
    }

    private static boolean inNodeModules(Path path) {
        for (Path segment : path) {
            if ("node_modules".equals(segment.toString())) {
                return true;
            }
        }
        return false;
    }

    private static boolean imports(SourceFile sf, List<Pattern> imported) {
        if (sf instanceof JS.CompilationUnit) {
            Set<String> specifiers = new HashSet<>();
            new JavaScriptIsoVisitor<Set<String>>() {
                @Override
                public JS.Import visitImportDeclaration(JS.Import jsImport, Set<String> s) {
                    addSpecifier(jsImport.getModuleSpecifier(), s);
                    return super.visitImportDeclaration(jsImport, s);
                }

                @Override
                public JS.ExportDeclaration visitExportDeclaration(JS.ExportDeclaration export, Set<String> s) {
                    addSpecifier(export.getModuleSpecifier(), s);
                    return super.visitExportDeclaration(export, s);
                }

                @Override
                public JS.ImportType visitImportType(JS.ImportType importType, Set<String> s) {
                    List<J> args = importType.getArgumentAndAttributes();
                    if (!args.isEmpty()) {
                        addSpecifier(args.get(0), s);
                    }
                    return super.visitImportType(importType, s);
                }

                // require('m') and import m = require('m')
                @Override
                public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, Set<String> s) {
                    if (method.getSelect() == null && "require".equals(method.getSimpleName()) &&
                        !method.getArguments().isEmpty()) {
                        addSpecifier(method.getArguments().get(0), s);
                    }
                    return super.visitMethodInvocation(method, s);
                }

                // import('m'), whose callee is the keyword
                @Override
                public JS.FunctionCall visitFunctionCall(JS.FunctionCall call, Set<String> s) {
                    if (call.getFunction() instanceof J.Identifier &&
                        "import".equals(((J.Identifier) call.getFunction()).getSimpleName()) &&
                        !call.getArguments().isEmpty()) {
                        addSpecifier(call.getArguments().get(0), s);
                    }
                    return super.visitFunctionCall(call, s);
                }
            }.visit(sf, specifiers);
            for (String specifier : specifiers) {
                if (matches(specifier, imported)) {
                    return true;
                }
            }
            return false;
        }
        // a source the parser could not read as code: its specifiers as written
        if (sf instanceof PlainText && SOURCE_EXTENSIONS.contains(extension(sf.getSourcePath()))) {
            Matcher m = SPECIFIER.matcher(((PlainText) sf).getText());
            while (m.find()) {
                if (matches(m.group(2), imported)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void addSpecifier(@Nullable J specifier, Set<String> specifiers) {
        if (specifier instanceof JS.LiteralType) {
            specifier = ((JS.LiteralType) specifier).getLiteral();
        }
        if (specifier instanceof J.Literal && ((J.Literal) specifier).getValue() instanceof String) {
            specifiers.add((String) ((J.Literal) specifier).getValue());
        }
    }

    private static boolean matches(String specifier, List<Pattern> imported) {
        String name = packageName(specifier);
        if (name == null) {
            return false;
        }
        for (Pattern pattern : imported) {
            if (pattern.matcher(name).matches()) {
                return true;
            }
        }
        return false;
    }

    // the package a bare specifier names: `@scope/name/sub` is `@scope/name`, `name/sub` is `name`
    private static @Nullable String packageName(String specifier) {
        if (specifier.isEmpty() || specifier.startsWith(".") || specifier.startsWith("/") ||
            specifier.contains(":")) {
            return null;
        }
        String[] parts = specifier.split("/", -1);
        if (specifier.startsWith("@")) {
            return parts.length < 2 || parts[1].isEmpty() ? null : parts[0] + "/" + parts[1];
        }
        return parts[0];
    }

    private static String extension(Path path) {
        String name = String.valueOf(path.getFileName());
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1);
    }
}
