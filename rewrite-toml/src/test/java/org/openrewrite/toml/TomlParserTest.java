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
package org.openrewrite.toml;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.toml.tree.Toml;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.toml.Assertions.toml;

class TomlParserTest implements RewriteTest {
    @Test
    void keyValueString() {
        rewriteRun(
          toml(
            """
              str = "I'm a string. \\"You can quote me\\". Name\\tJos\\u00E9\\nLocation\\tSF."
              """
          )
        );
    }

    @Test
    void keyValueInteger() {
        rewriteRun(
          toml(
            """
              int1 = +99
              int2 = 42
              int3 = 0
              int4 = -17
              int5 = 1_000

              # hexadecimal with prefix `0x`
              hex1 = 0xDEADBEEF
              hex2 = 0xdeadbeef
              hex3 = 0xdead_beef
              # octal with prefix `0o`
              oct1 = 0o01234567
              oct2 = 0o755 # useful for Unix file permissions

              # binary with prefix `0b`
              bin1 = 0b11010110
              """
          )
        );
    }

    @Test
    void keyValueFloat() {
        rewriteRun(
          toml(
            """
              # fractional
              flt1 = +1.0
              flt2 = 3.1415
              flt3 = -0.01

              # exponent
              flt4 = 5e+22
              flt5 = 1e06
              flt6 = -2E-2

              # both
              flt7 = 6.626e-34

              flt8 = 224_617.445_991_228

              # infinity
              sf1 = inf  # positive infinity
              sf2 = +inf # positive infinity
              sf3 = -inf # negative infinity

              # not a number
              sf4 = nan  # actual sNaN/qNaN encoding is implementation-specific
              sf5 = +nan # same as `nan`
              sf6 = -nan # valid, actual encoding is implementation-specific
              """
          )
        );
    }

    @Test
    void keyValueBool() {
        rewriteRun(
          toml(
            """
              bool1 = true
              bool2 = false
              """
          )
        );
    }

    @Test
    void keyValueOffsetDateTime() {
        rewriteRun(
          toml(
            """
              odt1 = 1979-05-27T07:32:00Z
              odt2 = 1979-05-27T00:32:00-07:00
              odt3 = 1979-05-27T00:32:00.999999-07:00
              odt4 = 1979-05-27 07:32:00Z
              """
          )
        );
    }

    @Test
    void keyValueLocalDateTime() {
        rewriteRun(
          toml(
            """
              ldt1 = 1979-05-27T07:32:00
              ldt2 = 1979-05-27T00:32:00.999999
              """
          )
        );
    }

    @Test
    void keyValueLocalDate() {
        rewriteRun(
          toml(
            """
              ld1 = 1979-05-27
              """
          )
        );
    }

    @Test
    void keyValueLocalTime() {
        rewriteRun(
          toml(
            """
              lt1 = 07:32:00
              lt2 = 00:32:00.999999
              """
          )
        );
    }

    @Test
    void keyValueArray() {
        rewriteRun(
          toml(
            """
              integers = [ 1, 2, 3 ]
              colors = [ "red", "yellow", "green" ]
              nested_arrays_of_ints = [ [ 1, 2 ], [3, 4, 5] ]
              nested_mixed_array = [ [ 1, 2 ], ["a", "b", "c"] ]
              string_array = [ "all", 'strings', ""\"are the same""\", '''type''' ]

              # Mixed-type arrays are allowed
              numbers = [ 0.1, 0.2, 0.5, 1, 2, 5 ]
              contributors = [
                "Foo Bar <foo@example.com>",
                { name = "Baz Qux", email = "bazqux@example.com", url = "https://example.com/bazqux" }
              ]
              integers2 = [
                1, 2, 3
              ]

              integers3 = [
                1,
                2, # this is ok
              ]
              """
          )
        );
    }

    @Test
    void table() {
        rewriteRun(
          toml(
            """
              [table-1]
              key1 = "some string"
              key2 = 123

              [table-2]
              key1 = "another string"
              key2 = 456

              [dog."tater.man"]
              type.name = "pug"
              """,
            spec -> spec.afterRecipe(doc -> {
                assertThat(doc.getValues()).hasSize(3);
                assertThat(doc.getValues()).allSatisfy(
                  v -> assertThat(v).isInstanceOf(Toml.Table.class)
                );
            })
          )
        );
    }

    @Test
    void arrayTable() {
        rewriteRun(
          toml(
            """
              [[products]]
              name = "Hammer"
              sku = 738594937

              [[products]]  # empty table within the array

              [[products]]
              name = "Nail"
              sku = 284758393

              color = "gray"
              """
          )
        );
    }

    @Test
    void bareKeys() {
        rewriteRun(
          toml(
            """
              key = "value"
              bare_key = "value"
              bare-key = "value"
              1234 = "value"
              """
          )
        );
    }

    @Test
    void quotedKeys() {
        rewriteRun(
          toml(
            """
              "127.0.0.1" = "value"
              "character encoding" = "value"
              "ʎǝʞ" = "value"
              'key2' = "value"
              'quoted "value"' = "value"
              """
          )
        );
    }

    @Test
    void dottedKeys() {
        rewriteRun(
          toml(
            """
              physical.color = "orange"
              physical.shape = "round"
              site."google.com" = true
              """
          )
        );
    }

    @Test
    void extraWhitespaceDottedKeys() {
        rewriteRun(
          toml(
            """
              fruit.name = "banana"      # this is best practice
              fruit. color = "yellow"    # same as fruit.color
              fruit . flavor = "banana"  # same as fruit.flavor
              """
          )
        );
    }

    @Test
    void extraWhitespaceTable() {
        rewriteRun(
          toml(
            """
              [a.b.c]            # this is best practice
              [ d.e.f ]          # same as [d.e.f]
              [ g .  h  . i ]    # same as [g.h.i]
              [ j . "ʞ" . 'l' ]  # same as [j."ʞ".'l']
              """
          )
        );
    }

    @Test
    void extraWhitespaceArrayTable() {
        rewriteRun(
          toml(
            """
              [[a.b.c]]            # this is best practice
              [[ d.e.f ]]          # same as [[d.e.f]]
              [[ g .  h  . i ]]    # same as [[g.h.i]]
              [[ j . "ʞ" . 'l' ]]  # same as [[j."ʞ".'l']]
              """
          )
        );
    }

    @Test
    void empty() {
        rewriteRun(
          toml(
            ""
          )
        );
    }

    @Test
    void trailingComment() {
        rewriteRun(
          toml(
            """
              str = "I'm a string. \\"You can quote me\\". Name\\tJos\\u00E9\\nLocation\\tSF."
              # trailing comment
              """
          )
        );
    }

    @Test
    void trailingEmptyComment() {
        rewriteRun(
          toml(
              """
              str = "I'm a string. \\"You can quote me\\". Name\\tJos\\u00E9\\nLocation\\tSF."
              #
              """
          )
        );
    }

    @Test
    void multiBytesUnicode() {
        rewriteRun(
          toml(
            """
              robot.name = "r2d2" # 🤖
              """
          )
        );
    }
}
