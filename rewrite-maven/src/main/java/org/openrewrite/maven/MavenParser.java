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

import org.intellij.lang.annotations.Language;
import org.openrewrite.ExecutionContext;
import org.openrewrite.HttpSenderExecutionContextView;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.maven.internal.MavenPomDownloader;
import org.openrewrite.maven.internal.RawPom;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.maven.tree.Parent;
import org.openrewrite.maven.tree.Pom;
import org.openrewrite.maven.tree.ResolvedPom;
import org.openrewrite.tree.ParsingExecutionContextView;
import org.openrewrite.xml.XmlParser;
import org.openrewrite.xml.tree.Xml;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Collections.*;
import static org.openrewrite.Tree.randomId;

public class MavenParser implements Parser<Xml.Document> {
    @SuppressWarnings("DeprecatedIsStillUsed")
    @Deprecated
    @Nullable
    private final HttpSender httpSender;

    private final Collection<String> activeProfiles;

    /**
     * @param activeProfiles The maven profile names set to be active. Profiles are typically defined in the settings.xml
     */
    private MavenParser(@Nullable HttpSender httpSender, Collection<String> activeProfiles) {
        this.httpSender = httpSender;
        this.activeProfiles = activeProfiles;
    }

    @Override
    public List<Xml.Document> parse(@Language("xml") String... sources) {
        return parse(new InMemoryExecutionContext(), sources);
    }

    @Override
    public List<Xml.Document> parse(ExecutionContext ctx, @Language("xml") String... sources) {
        return Parser.super.parse(ctx, sources);
    }

    private static class MavenXmlParser extends XmlParser {
        @Override
        public boolean accept(Path path) {
            return super.accept(path) || path.toString().endsWith(".pom");
        }
    }

    @Override
    public List<Xml.Document> parseInputs(Iterable<Input> sources, @Nullable Path relativeTo,
                                          ExecutionContext ctx) {
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

                Xml.Document xml = new MavenXmlParser()
                        .parseInputs(singletonList(source), relativeTo, ctx)
                        .iterator().next();

                projectPoms.put(xml, pom);
                projectPomsByPath.put(pomPath, pom);
            } catch (Throwable t) {
                ParsingExecutionContextView.view(ctx).parseFailure(source, relativeTo, this, t);
                ctx.getOnError().accept(t);
            }
        }

        List<Xml.Document> parsed = new ArrayList<>();

        if (httpSender != null) {
            ctx = HttpSenderExecutionContextView.view(ctx).setHttpSender(httpSender);
        }
        MavenPomDownloader downloader = new MavenPomDownloader(projectPomsByPath, ctx);

        MavenExecutionContextView mavenCtx = MavenExecutionContextView.view(ctx);
        MavenSettings sanitizedSettings = mavenCtx.getSettings() == null ? null : mavenCtx.getSettings()
                .withServers(null);

        for (Map.Entry<Xml.Document, Pom> docToPom : projectPoms.entrySet()) {
            try {
                ResolvedPom resolvedPom = docToPom.getValue().resolve(activeProfiles, downloader, ctx);
                MavenResolutionResult model = new MavenResolutionResult(randomId(), null, resolvedPom, emptyList(), null, emptyMap(), sanitizedSettings, mavenCtx.getActiveProfiles())
                        .resolveDependencies(downloader, ctx);
                parsed.add(docToPom.getKey().withMarkers(docToPom.getKey().getMarkers().compute(model, (old, n) -> n)));
            } catch (MavenDownloadingExceptions e) {
                throw new UncheckedMavenDownloadingException(docToPom.getKey(), e);
            } catch (MavenDownloadingException e) {
                throw new UncheckedMavenDownloadingException(docToPom.getKey(), e);
            }
        }

        for (int i = 0; i < parsed.size(); i++) {
            Xml.Document maven = parsed.get(i);
            MavenResolutionResult resolutionResult = maven.getMarkers().findFirst(MavenResolutionResult.class)
                    .orElseThrow(() -> new IllegalStateException("Expected to find a maven resolution marker"));

            List<MavenResolutionResult> modules = new ArrayList<>(0);
            for (Xml.Document possibleModule : parsed) {
                if (possibleModule == maven) {
                    continue;
                }
                MavenResolutionResult moduleResolutionResult = possibleModule.getMarkers().findFirst(MavenResolutionResult.class)
                        .orElseThrow(() -> new IllegalStateException("Expected to find a maven resolution marker"));
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

        return parsed;
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

        @SuppressWarnings("DeprecatedIsStillUsed")
        @Deprecated
        @Nullable
        private HttpSender httpSender;

        public Builder() {
            super(Xml.Document.class);
        }

        public Builder activeProfiles(@Nullable String... profiles) {
            //noinspection ConstantConditions
            if (profiles != null) {
                Collections.addAll(this.activeProfiles, profiles);
            }
            return this;
        }

        /**
         * @param httpSender The HTTP sender to use.
         * @return This builder
         * @deprecated Use {@link org.openrewrite.HttpSenderExecutionContextView#setHttpSender(HttpSender)} instead.
         */
        @Deprecated
        public Builder httpSender(HttpSender httpSender) {
            this.httpSender = httpSender;
            return this;
        }

        public Builder mavenConfig(@Nullable Path mavenConfig) {
            if (mavenConfig != null && mavenConfig.toFile().exists()) {
                try {
                    String mavenConfigText = new String(Files.readAllBytes(mavenConfig));
                    Matcher matcher = Pattern.compile("(?:$|\\s)-P\\s+([^\\s]+)").matcher(mavenConfigText);
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
            return new MavenParser(httpSender, activeProfiles);
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
