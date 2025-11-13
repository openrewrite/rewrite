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
package org.openrewrite.maven.marketplace;

import org.apache.commons.lang3.StringUtils;
import org.intellij.lang.annotations.Language;
import org.openrewrite.ExecutionContext;
import org.openrewrite.marketplace.RecipeBundle;
import org.openrewrite.marketplace.RecipeBundleReader;
import org.openrewrite.marketplace.RecipeBundleResolver;
import org.openrewrite.marketplace.ThrowingRecipeBundleReader;
import org.openrewrite.maven.MavenParser;
import org.openrewrite.maven.tree.GroupArtifactVersion;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.maven.utilities.MavenArtifactDownloader;

import java.util.Optional;

public class MavenRecipeBundleResolver implements RecipeBundleResolver {
    private final ExecutionContext ctx;
    private final MavenArtifactDownloader downloader;
    private final RecipeClassLoaderFactory classLoaderFactory;

    public MavenRecipeBundleResolver(ExecutionContext ctx, MavenArtifactDownloader downloader, RecipeClassLoaderFactory classLoaderFactory) {
        this.ctx = ctx;
        this.downloader = downloader;
        this.classLoaderFactory = classLoaderFactory;
    }

    @Override
    public String getEcosystem() {
        return "maven";
    }

    @Override
    public RecipeBundleReader resolve(RecipeBundle bundle) {
        if (StringUtils.isBlank(bundle.getVersion())) {
            return new ThrowingRecipeBundleReader(bundle, new IllegalStateException("Unable to read a Maven recipe bundle that has no version"));
        }
        String[] ga = bundle.getPackageName().split(":");
        GroupArtifactVersion gav = new GroupArtifactVersion(ga[0], ga[1], bundle.getVersion());
        return resolveDependencies(gav)
                .map(mrr -> (RecipeBundleReader) new MavenRecipeBundleReader(bundle, mrr, downloader, classLoaderFactory))
                .orElseGet(() -> new ThrowingRecipeBundleReader(bundle, new IllegalStateException("Unable to resolve recipe " + gav)));
    }

    private Optional<MavenResolutionResult> resolveDependencies(GroupArtifactVersion gav) {
        @Language("xml") String pomXml =
                "<project>" +
                "    <groupId>io.moderne</groupId>" +
                "    <artifactId>recipe-downloader</artifactId>" +
                "    <version>1</version>" +
                "    <dependencies>" +
                "        <dependency>" +
                "            <groupId>" + gav.getGroupId() + "</groupId>" +
                "            <artifactId>" + gav.getArtifactId() + "</artifactId>" +
                "            <version>" + gav.getVersion() + "</version>" +
                "        </dependency>" +
                "    </dependencies>" +
                "</project>";
        return MavenParser.builder().build().parse(ctx, pomXml)
                .findFirst()
                .flatMap(sf -> sf.getMarkers().findFirst(MavenResolutionResult.class))
                .filter(mrr -> !mrr.getDependencies().isEmpty());
    }
}
