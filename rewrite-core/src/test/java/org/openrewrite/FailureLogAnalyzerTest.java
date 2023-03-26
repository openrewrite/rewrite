/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FailureLogAnalyzerTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
      BUG! exception in phase 'semantic analysis' in source unit '_BuildScript_' Unsupported class file major version 61 | 8
      error: Source option 6 is no longer supported. Use 7 or later. | 6
      error: source option 6 is no longer supported. Use 7 or later. | 6
      error: Target option 6 is no longer supported. Use 7 or later. | 6
      Error:PARSE ERROR: Error:unsupported class file version 53.0 | 8
      DefaultCodeFormatter has been compiled by a more recent version of the Java Runtime (class file version 55.0), | 11
      Fatal error compiling: invalid target release: 17 -> [Help 1] | 17
      Fatal error compiling: invalid target release: 11 -> [Help 1] | 11
      Fatal error compiling: error: release version 17 not supported | 17
      javac: invalid target release: 11 | 11
      error: invalid source release: 17 | 17
      Fatal error compiling: invalid flag: --release | 11
      Unrecognized option: --add-exports | 11
      javac: invalid flag: --module-path | 11
      [ERROR] jdk [ version='1.8' ] | 8
      [WARNING] : bad option '-target:11' was ignored | 11
      [ERROR] warning: [options] source value 1.5 is obsolete and will be removed in a future release | 5
      [ERROR] warning: [options] target value 1.5 is obsolete and will be removed in a future release | 5
      (use -source 7 or higher to enable diamond operator) | 7
      (use -source 8 or higher to enable lambda expressions) | 8
      Incompatible because this component declares a component compatible with Java 11 and the consumer needed a component compatible with Java 8 | 11
      """)
    void determineRequiredClassFileVersion(String logFileContents, String expected) {
        assertEquals(expected, FailureLogAnalyzer.requiredJavaVersion(logFileContents));
    }
}