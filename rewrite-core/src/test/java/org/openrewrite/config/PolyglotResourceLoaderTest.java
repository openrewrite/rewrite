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

import org.graalvm.polyglot.Source;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graalvm.polyglot.Source.newBuilder;

class PolyglotResourceLoaderTest {

    static final String SHARED_LIB = "src/github.com/moderneinc/rewrite_rust/target/debug/deps/rewrite_rust.bc";

    @Disabled
    @Test
    void mustLoadLLVMModule() throws IOException {
        Source src = newBuilder("llvm", Paths.get(SHARED_LIB).toFile()).build();
        PolyglotResourceLoader loader = new PolyglotResourceLoader(src);
        assertThat(loader.listRecipes()).isNotEmpty();
    }

}