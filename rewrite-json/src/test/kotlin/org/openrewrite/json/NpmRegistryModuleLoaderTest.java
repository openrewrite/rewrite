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

package org.openrewrite.json;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Result;
import org.openrewrite.config.Environment;
import org.openrewrite.scheduling.DirectScheduler;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

class NpmRegistryModuleLoaderTest {

    @Test
    void mustLoadModuleFromNpmjsOrg() {
        Environment env = Environment.builder()
                .scanNpmModules("https://registry.npmjs.org/", "@openrewrite/ts-recipes")
                .build();
        Collection<Recipe> recipes = env.listRecipes();
        assertThat(recipes).isNotEmpty();

        Recipe r = recipes.iterator().next();
        List<Result> results = r.run(new JsonParser().parse("{}"));

        System.out.println("results: " + results);
    }

    @Disabled
    @Test
    void mustLoadModuleFromLocal() throws Exception {
        String registry = Paths.get(System.getProperty("user.home"), "src", "github.com", "openrewrite").toString();
        Environment env = Environment.builder()
                .scanNpmModules(registry, "ts-recipes")
                .build();
        assertThat(env.listRecipes()).isNotEmpty();

        Recipe r = env.activateRecipes("org.openrewrite.typescript.ChangeLicenseRecipe");
        ExecutorService x = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(1);
        Future<?> f1 = x.submit(() -> {
            latch.await();
            return r.getDisplayName();
        });
        Future<?> f2 = x.submit(() -> {
            latch.countDown();
            return r.getDisplayName();
        });
        f2.get();

        List<Result> results = r.run(new JsonParser().parse(Files.readString(Paths.get(registry, "ts-recipes", "package.json"))),
                new InMemoryExecutionContext(Throwable::printStackTrace),
                DirectScheduler.common(),
                3,
                1);

        assertThat(results).isNotEmpty();
        System.out.println("results: " + results);
    }

}
