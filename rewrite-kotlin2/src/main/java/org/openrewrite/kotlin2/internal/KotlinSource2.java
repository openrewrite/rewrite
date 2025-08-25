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
import org.jetbrains.kotlin.fir.declarations.FirFile;
import org.jetbrains.kotlin.psi.KtFile;
import org.openrewrite.Parser;

/**
 * Represents a single Kotlin 2 source file with its PSI and FIR representations.
 */
@Getter
public class KotlinSource2 {
    private final Parser.Input input;
    private final KtFile ktFile;
    private final FirFile firFile;
    private final String source;

    public KotlinSource2(Parser.Input input, KtFile ktFile, FirFile firFile, String source) {
        this.input = input;
        this.ktFile = ktFile;
        this.firFile = firFile;
        this.source = source;
    }
}