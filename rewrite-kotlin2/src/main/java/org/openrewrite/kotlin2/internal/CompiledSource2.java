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
package org.openrewrite.kotlin2.internal;

import lombok.Getter;
import org.jetbrains.kotlin.fir.FirSession;
import org.openrewrite.SourceFile;

import java.util.List;
import java.util.stream.Stream;

/**
 * Represents compiled Kotlin 2 source units with their FIR session.
 */
@Getter
public class CompiledSource2 {
    private final FirSession firSession;
    private final List<KotlinSource2> sources;
    private final Stream<SourceFile> compiledInputs;

    public CompiledSource2(FirSession firSession, List<KotlinSource2> sources, Stream<SourceFile> compiledInputs) {
        this.firSession = firSession;
        this.sources = sources;
        this.compiledInputs = compiledInputs;
    }
}