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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.HttpSenderExecutionContextView;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Issue;
import org.openrewrite.ipc.http.HttpUrlConnectionSender;
import org.openrewrite.marketplace.RecipeBundle;
import org.openrewrite.marketplace.RecipeBundleReader;
import org.openrewrite.marketplace.RecipeClassLoader;
import org.openrewrite.marketplace.RecipeMarketplace;
import org.openrewrite.maven.MavenExecutionContextView;
import org.openrewrite.maven.cache.LocalMavenArtifactCache;
import org.openrewrite.maven.utilities.MavenArtifactDownloader;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class MavenRecipeBundleReaderTest {
    @Issue("https://github.com/openrewrite/rewrite/issues/6487")
    @Test
    void canInstallRewriteCore(@TempDir Path tempDir) throws Exception {
        InMemoryExecutionContext ctx = new InMemoryExecutionContext(Throwable::printStackTrace);
        HttpSenderExecutionContextView.view(ctx).setHttpSender(new HttpUrlConnectionSender());
        MavenExecutionContextView mavenCtx = MavenExecutionContextView.view(ctx);
        mavenCtx.setAddCentralRepository(true);

        LocalMavenArtifactCache artifactCache = new LocalMavenArtifactCache(tempDir.resolve("artifacts"));

        MavenArtifactDownloader downloader = new MavenArtifactDownloader(
          artifactCache,
          null,
          new HttpUrlConnectionSender(),
          Throwable::printStackTrace
        );

        try (MavenRecipeBundleResolver resolver = new MavenRecipeBundleResolver(
          ctx,
          downloader,
          RecipeClassLoader::new
        )) {
            RecipeBundle bundle = new RecipeBundle("maven", "org.openrewrite:rewrite-core", "8.70.0", null, null);
            RecipeBundleReader reader = resolver.resolve(bundle);

            RecipeMarketplace marketplace = reader.read();
            assertThat(marketplace.getAllRecipes())
              .isNotEmpty()
              .as("rewrite-core should install and successfully list recipes")
              .anyMatch(r -> r.getName().contains(org.openrewrite.text.Find.class.getName()));
        }
    }
}
