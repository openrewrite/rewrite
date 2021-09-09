/*
 *  Copyright 2021 the original author or authors.
 *  <p>
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  <p>
 *  https://www.apache.org/licenses/LICENSE-2.0
 *  <p>
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.openrewrite.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.TypeLiteral;
import org.graalvm.polyglot.Value;
import org.openrewrite.Recipe;
import org.openrewrite.polyglot.PolyglotRecipe;
import org.openrewrite.style.NamedStyles;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.zip.GZIPInputStream;

import static java.lang.Math.toIntExact;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyList;
import static org.openrewrite.polyglot.PolyglotUtils.invokeMember;

public class NpmRegistryModuleLoader implements ResourceLoader {

    private static final String JS = "js";

    private final List<Recipe> recipes = new ArrayList<>();
    private final List<NamedStyles> styles = new ArrayList<>();

    private final List<RecipeDescriptor> recipeDescriptors = new ArrayList<>();
    private final List<CategoryDescriptor> categoryDescriptors = new ArrayList<>();
    private final List<RecipeExample> recipeExamples = new ArrayList<>();

    public NpmRegistryModuleLoader(String registry, String... modules) {
        if (!registry.endsWith("/")) {
            registry += "/";
        }

        Context context = Context.newBuilder(JS)
                .allowAllAccess(true)
                .build();

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        for (String module : modules) {
            try {
                if (!registry.startsWith("http")) {
                    Path path = Paths.get(registry, module, "package.json");
                    PackageDescriptor packageDescriptor = mapper.readValue(path.toFile(), PackageDescriptor.class);
                    String main = packageDescriptor.getMain();

                    evalRecipe(context,
                            module + "@latest",
                            packageDescriptor.getDescription(),
                            path.toString(),
                            path.toUri(),
                            Files.readAllBytes(Paths.get(registry, module, main)));
                } else {
                    URI srcUri = URI.create(registry + module);
                    PackagesDescriptor packages = mapper.readValue(srcUri.toURL(), PackagesDescriptor.class);
                    String latestVersion = packages.getDistTags().get("latest");
                    PackageDescriptor packageDescriptor = packages.getVersions().get(latestVersion);
                    String main = packageDescriptor.getMain();

                    String tarball = (String) packageDescriptor.getDist().get("tarball");
                    URI tarballUri = URI.create(tarball);
                    try (TarArchiveInputStream tgzIn = new TarArchiveInputStream(new GZIPInputStream(tarballUri.toURL().openStream()))) {
                        for (TarArchiveEntry e; (e = tgzIn.getNextTarEntry()) != null; ) {
                            String name = e.getName();
                            if (!name.endsWith(main) || !name.endsWith(JS) || !tgzIn.canReadEntryData(e)) {
                                continue;
                            }

                            byte[] buff = new byte[toIntExact(e.getSize())];
                            //noinspection StatementWithEmptyBody
                            for (int i = 0; i < buff.length; i += tgzIn.read(buff, i, tgzIn.getRecordSize())) {
                            }
                            evalRecipe(context,
                                    module + "@" + latestVersion,
                                    packageDescriptor.getDescription(),
                                    e.getName(),
                                    tarballUri,
                                    buff);
                        }
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public Collection<Recipe> listRecipes() {
        return recipes;
    }

    @Override
    public Collection<RecipeDescriptor> listRecipeDescriptors() {
        return recipeDescriptors;
    }

    @Override
    public Collection<NamedStyles> listStyles() {
        return styles;
    }

    @Override
    public Collection<CategoryDescriptor> listCategoryDescriptors() {
        return categoryDescriptors;
    }

    @Override
    public Collection<RecipeExample> listRecipeExamples() {
        return recipeExamples;
    }

    private void evalRecipe(Context context,
                            String moduleName,
                            String description,
                            String jsFileName,
                            URI srcUri,
                            byte[] jsFile) throws IOException {
        Value bindings = context.getBindings(JS);

        Source src = Source.newBuilder(JS, new String(jsFile, UTF_8), jsFileName).build();
        context.eval(src);

        String recipeName = bindings.getMemberKeys().iterator().next();
        Value recipeVal = bindings.getMember(recipeName);
        Value opts = recipeVal.getMember("Options").newInstance();

        Recipe r = new PolyglotRecipe(opts, recipeVal.getMember("default"));

        recipes.add(r);
        RecipeDescriptor descriptor = new RecipeDescriptor(
                moduleName,
                moduleName,
                description,
                Collections.emptySet(),
                invokeMember(opts, "getOptionsDescriptors")
                        .map(descs -> descs.as(new TypeLiteral<List<OptionDescriptor>>() {
                        }))
                        .orElse(emptyList()),
                Collections.singletonList(JS),
                emptyList(),
                srcUri);
        recipeDescriptors.add(descriptor);
    }

    @Data
    private static class PackagesDescriptor {
        @JsonProperty("dist-tags")
        Map<String, String> distTags;
        @JsonProperty("versions")
        Map<String, PackageDescriptor> versions;
    }

    @Data
    private static class PackageDescriptor {
        @JsonProperty("description")
        String description;
        @JsonProperty("dist")
        Map<String, Object> dist;
        @JsonProperty("main")
        String main;
    }

}
