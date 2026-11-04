/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.python.internal.index;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class DistFilenameTest {

    @ParameterizedTest
    @CsvSource({
      "requests-2.31.0-py3-none-any.whl, requests, 2.31.0, WHEEL",
      "typing_extensions-4.8.0-py3-none-any.whl, typing_extensions, 4.8.0, WHEEL",
      "numpy-1.26.0-1-cp311-cp311-manylinux_2_17_x86_64.manylinux2014_x86_64.whl, numpy, 1.26.0, WHEEL",
      "cryptography-41.0.7-cp37-abi3-macosx_10_12_universal2.whl, cryptography, 41.0.7, WHEEL",
      "requests-2.31.0.tar.gz, requests, 2.31.0, SDIST",
      "zope.interface-5.4.0.tar.gz, zope.interface, 5.4.0, SDIST",
      "pyyaml-5.4.1.tar.bz2, pyyaml, 5.4.1, OTHER",
      "flask-2.0.tgz, flask, 2.0, SDIST",
      "foo-bar-1.0.zip, foo-bar, 1.0, SDIST",
      "setuptools-65.0-py3.9.egg, setuptools, 65.0, OTHER",
    })
    void parses(String filename, String distribution, String version, DistFilename.Type type) {
        DistFilename parsed = DistFilename.parse(filename);
        assertThat(parsed).isNotNull();
        assertThat(parsed.getDistribution()).isEqualTo(distribution);
        assertThat(parsed.getVersion()).isEqualTo(version);
        assertThat(parsed.getType()).isEqualTo(type);
    }

    @ParameterizedTest
    @ValueSource(strings = {
      "METADATA",
      "not-an-archive.txt",
      "malformed.whl",
      "requests-2.31.0.whl",
      "noversion.tar.gz",
      "-1.0.tar.gz"
    })
    void unparseable(String filename) {
        assertThat(DistFilename.parse(filename)).isNull();
    }
}
