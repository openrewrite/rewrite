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
package org.openrewrite.kotlin;

import org.junit.jupiter.api.Test;
import org.openrewrite.SourceFile;
import org.openrewrite.java.service.AutoFormatService;
import org.openrewrite.java.service.ImportService;
import org.openrewrite.kotlin.service.KotlinAutoFormatService;
import org.openrewrite.kotlin.service.KotlinImportService;

import java.net.URL;
import java.net.URLClassLoader;

import static org.assertj.core.api.Assertions.assertThat;

class KotlinServiceLoaderTest {

    @Test
    void servicesResolveWithoutTheRequestedInterfacesClassLoader() throws Exception {
        SourceFile cu = KotlinParser.builder().build()
          .parse("class A")
          .findFirst()
          .orElseThrow(() -> new AssertionError("no source file parsed"));

        URL rewriteJava = ImportService.class.getProtectionDomain().getCodeSource().getLocation();
        // Stands in for a recipe classloader carrying rewrite-java but no Kotlin module, so the
        // service has to resolve through this tree's own loader.
        try (URLClassLoader isolated = new URLClassLoader(new URL[]{rewriteJava},
          ClassLoader.getSystemClassLoader().getParent())) {
            Class<?> importService = isolated.loadClass(ImportService.class.getName());
            Class<?> autoFormatService = isolated.loadClass(AutoFormatService.class.getName());
            assertThat(importService).isNotSameAs(ImportService.class);

            assertThat((Object) cu.service(importService)).isInstanceOf(KotlinImportService.class);
            assertThat((Object) cu.service(autoFormatService)).isInstanceOf(KotlinAutoFormatService.class);
        }
    }

    @Test
    void theKotlinTypeCanBeRequestedDirectly() {
        SourceFile cu = KotlinParser.builder().build()
          .parse("class A")
          .findFirst()
          .orElseThrow(() -> new AssertionError("no source file parsed"));

        assertThat((Object) cu.service(KotlinImportService.class)).isInstanceOf(KotlinImportService.class);
        assertThat((Object) cu.service(KotlinAutoFormatService.class)).isInstanceOf(KotlinAutoFormatService.class);
    }
}
