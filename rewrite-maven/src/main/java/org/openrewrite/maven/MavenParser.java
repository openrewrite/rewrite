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
package org.openrewrite.maven;

import lombok.RequiredArgsConstructor;
import org.intellij.lang.annotations.Language;
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.internal.MavenPomDownloader;
import org.openrewrite.maven.internal.RawPom;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.maven.tree.Parent;
import org.openrewrite.maven.tree.Pom;
import org.openrewrite.maven.tree.ResolvedPom;
import org.openrewrite.tree.ParseError;
import org.openrewrite.xml.XmlParser;
import org.openrewrite.xml.tree.Xml;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.util.Collections.*;
import static org.openrewrite.Tree.randomId;

@RequiredArgsConstructor
public class MavenParser implements Parser {

    private final Collection<String> activeProfiles;
    private final boolean skipDependencyResolution;

    @Override
    public Stream<SourceFile> parse(@Language("xml") String... sources) {
        return parse(new InMemoryExecutionContext(), sources);
    }

    @Override
    public Stream<SourceFile> parse(ExecutionContext ctx, @Language("xml") String... sources) {
        return Parser.super.parse(ctx, sources);
    }

    private static class MavenXmlParser extends XmlParser {
        @Override
        public boolean accept(Path path) {
            return super.accept(path) || path.toString().endsWith(".pom");
        }
    }

    @Override
    public Stream<SourceFile> parseInputs(Iterable<Input> sources, @Nullable Path relativeTo,
                                          ExecutionContext ctx) {
        List<SourceFile> parsed = new ArrayList<>();

        Map<Xml.Document, Pom> projectPoms = new LinkedHashMap<>();
        Map<Path, Pom> projectPomsByPath = new HashMap<>();
        for (Input source : sources) {
            Path pomPath = source.getRelativePath(relativeTo);
            try {
                Pom pom = RawPom.parse(source.getSource(ctx), null)
                        .toPom(pomPath, null);

                if (pom.getProperties() == null || pom.getProperties().isEmpty()) {
                    pom = pom.withProperties(new LinkedHashMap<>());
                }
                String baseDir = pomPath.toAbsolutePath().getParent().toString();
                pom.getProperties().put("project.basedir", baseDir);
                pom.getProperties().put("basedir", baseDir);

                SourceFile sourceFile = new MavenXmlParser()
                        .parseInputs(singletonList(source), relativeTo, ctx)
                        .iterator().next();

                if (sourceFile instanceof Xml.Document) {
                    Xml.Document xml = (Xml.Document) sourceFile;

                    projectPoms.put(xml, pom);
                    projectPomsByPath.put(pomPath, pom);
                } else {
                    parsed.add(sourceFile);
                }
            } catch (Throwable t) {
                ctx.getOnError().accept(t);
                parsed.add(ParseError.build(this, source, relativeTo, ctx, t));
            }
        }

        MavenPomDownloader downloader = new MavenPomDownloader(projectPomsByPath, ctx);

        MavenExecutionContextView mavenCtx = MavenExecutionContextView.view(ctx);
        MavenSettings sanitizedSettings = mavenCtx.getSettings() == null ? null : mavenCtx.getSettings()
                .withServers(null);

        for (Map.Entry<Xml.Document, Pom> docToPom : projectPoms.entrySet()) {
            try {
                ResolvedPom resolvedPom = docToPom.getValue().resolve(activeProfiles, downloader, ctx);
                MavenResolutionResult model = new MavenResolutionResult(randomId(), null, resolvedPom, emptyList(), null, emptyMap(), sanitizedSettings, mavenCtx.getActiveProfiles());
                if (!skipDependencyResolution) {
                    model = model.resolveDependencies(downloader, ctx);
                }
                parsed.add(docToPom.getKey().withMarkers(docToPom.getKey().getMarkers().compute(model, (old, n) -> n)));
            } catch (MavenDownloadingExceptions e) {
                ParseExceptionResult parseExceptionResult = new ParseExceptionResult(
                        randomId(),
                        MavenParser.class.getSimpleName(),
                        e.getClass().getSimpleName(),
                        e.warn(docToPom.getKey()).printAll(), // Shows any underlying MavenDownloadingException
                        null);
                parsed.add(docToPom.getKey().withMarkers(docToPom.getKey().getMarkers().add(parseExceptionResult)));
                ctx.getOnError().accept(e);
            } catch (MavenDownloadingException | UncheckedIOException e) {
                parsed.add(docToPom.getKey().withMarkers(docToPom.getKey().getMarkers().add(ParseExceptionResult.build(this, e))));
                ctx.getOnError().accept(e);
            }
        }

        for (int i = 0; i < parsed.size(); i++) {
            SourceFile maven = parsed.get(i);
            Optional<MavenResolutionResult> maybeResolutionResult = maven.getMarkers().findFirst(MavenResolutionResult.class);
            if(!maybeResolutionResult.isPresent()) {
                continue;
            }
            MavenResolutionResult resolutionResult = maybeResolutionResult.get();
            List<MavenResolutionResult> modules = new ArrayList<>(0);
            for (SourceFile possibleModule : parsed) {
                if (possibleModule == maven) {
                    continue;
                }
                Optional<MavenResolutionResult> maybeModuleResolutionResult = possibleModule.getMarkers().findFirst(MavenResolutionResult.class);
                if(!maybeModuleResolutionResult.isPresent()) {
                    continue;
                }
                MavenResolutionResult moduleResolutionResult = maybeModuleResolutionResult.get();
                Parent parent = moduleResolutionResult.getPom().getRequested().getParent();
                if (parent != null &&
                    resolutionResult.getPom().getGroupId().equals(resolutionResult.getPom().getValue(parent.getGroupId())) &&
                    resolutionResult.getPom().getArtifactId().equals(resolutionResult.getPom().getValue(parent.getArtifactId())) &&
                    resolutionResult.getPom().getVersion().equals(resolutionResult.getPom().getValue(parent.getVersion()))) {
                    moduleResolutionResult.unsafeSetParent(resolutionResult);
                    modules.add(moduleResolutionResult);
                }
            }

            if (!modules.isEmpty()) {
                resolutionResult.unsafeSetModules(modules);
            }
        }

        return parsed.stream();
    }

    @Override
    public boolean accept(Path path) {
        return "pom.xml".equals(path.toString()) || path.toString().endsWith(".pom");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends Parser.Builder {
        private final Collection<String> activeProfiles = new HashSet<>();
        private boolean skipDependencyResolution;

        public Builder() {
            super(Xml.Document.class);
        }

        public Builder skipDependencyResolution(boolean skip) {
            skipDependencyResolution = skip;
            return this;
        }

        public Builder activeProfiles(@Nullable String... profiles) {
            //noinspection ConstantConditions
            if (profiles != null) {
                Collections.addAll(this.activeProfiles, profiles);
            }
            return this;
        }

        @SuppressWarnings("unused") // Used in `MavenMojoProjectParser.parseMaven(..)`
        public Builder mavenConfig(@Nullable Path mavenConfig) {
            if (mavenConfig != null && mavenConfig.toFile().exists()) {
                try {
                    String mavenConfigText = new String(Files.readAllBytes(mavenConfig));
                    Matcher matcher = Pattern.compile("(?:$|\\s)-P\\s+(\\S+)").matcher(mavenConfigText);
                    if (matcher.find()) {
                        String[] profiles = matcher.group(1).split(",");
                        return activeProfiles(profiles);
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
            return this;
        }

        public MavenParser build() {
            return new MavenParser(activeProfiles, skipDependencyResolution);
        }

        @Override
        public String getDslName() {
            return "maven";
        }
    }

    @Override
    public Path sourcePathFromSourceText(Path prefix, String sourceCode) {
        return prefix.resolve("pom.xml");
    }
}
