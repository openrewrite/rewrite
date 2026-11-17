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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Issue;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.toml.tree.Toml;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static java.util.Collections.singletonList;
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
    void multiLineString() {
        rewriteRun(
          toml(
            """
              # Multi-line string with preserved newlines
              description = ""\"
              This is a
              multi-line string
              with preserved newlines""\"
              
              # Multi-line string with escaped newline (line-ending backslash)
              one-line = ""\"
              The quick brown \\
              fox jumps over \\
              the lazy dog.\\
              ""\"
              
              # Multi-line string containing quotes
              quotes = ""\"
              This has a single " quote
              This has double "" quotes
              This has escaped \\" quote""\"
              
              # Multi-line string with leading newline trimmed
              trimmed = ""\"
              First line after opening quotes""\"
              
              # Multi-line literal string (no escapes processed)
              regex = '''
              \\d{2}-\\d{2}-\\d{4}
              Line with \\ backslash
              Line with ' single quote
              Line with '' two quotes'''
              
              # Multi-line literal string with various quotes
              literal-quotes = '''
              Single ' quote
              Double '' quotes
              Almost ''\\' but not quite'''
              
              # Multi-line basic string with escape sequences
              escaped = ""\"
              Tab:\\there
              Newline:\\nhere
              Quote:\\" Unicode:\\u00E9
              Backslash:\\\\""\"
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
    void quotedKeyNameHoldsTheTextTheEscapesStandFor() {
        rewriteRun(
          toml(
            """
              "a\\tb\\u00e9" = 1
              'a\\tb' = 2
              """,
            spec -> spec.afterRecipe(doc -> assertThat(doc.getValues())
              .extracting(kv -> ((Toml.Identifier) ((Toml.KeyValue) kv).getKey()).getName())
              .containsExactly("a\tbé", "a\\tb"))
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
              """,
            spec -> spec.afterRecipe(doc -> {
                // Quoted-key identifier exposes the unquoted form as `name` and the
                // verbatim source (with quotes) as `source`. The printer uses
                // `source` so round-tripping preserves the original syntax.
                assertThat(doc.getValues()).hasSize(5);
                Toml.Identifier ipKey = (Toml.Identifier) ((Toml.KeyValue) doc.getValues().get(0)).getKey();
                assertThat(ipKey.getName()).isEqualTo("127.0.0.1");
                assertThat(ipKey.getSource()).isEqualTo("\"127.0.0.1\"");

                Toml.Identifier literalKey = (Toml.Identifier) ((Toml.KeyValue) doc.getValues().get(3)).getKey();
                assertThat(literalKey.getName()).isEqualTo("key2");
                assertThat(literalKey.getSource()).isEqualTo("'key2'");
            })
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

    @Issue("https://github.com/openrewrite/rewrite/issues/5988")
    @Test
    void arrayWithTrailingComma() {
        rewriteRun(
          toml(
            """
              # Single element with trailing comma
              tsfresh = [
                  "featuretools-tsfresh-primitives >= 1.0.0",
              ]
              
              # Multiple elements with trailing comma
              dependencies = [
                  "package1",
                  "package2",
                  "package3",
              ]
              
              # Inline array with trailing comma  
              inline = ["value1", "value2",]
              """
          )
        );
    }

    @Test
    void inlineTableWithTrailingComma() {
        rewriteRun(
          toml(
            """
              # Inline table with trailing comma
              point = { x = 1, y = 2, }
              
              # Another inline table with trailing comma
              person = { name = "John", age = 30, }
              """
          )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
      "\"hello\"",
      "'hello'",
      "\"\"\"hello\"\"\"",
      "'''hello'''"
    })
    void stringValueStrippedForAnyQuoteStyle(String quoted) {
        rewriteRun(
          toml(
            "greeting = " + quoted,
            spec -> spec.afterRecipe(doc ->
              assertThat(doc.getValues())
                .extracting(Toml.KeyValue.class::cast)
                .extracting(keyValue -> ((Toml.Literal) keyValue.getValue()).getValue())
                .containsExactly("hello"))
          )
        );
    }

    /**
     * A basic string's value is the decoded text, so a marker such as
     * {@code python_version < "3.11"} written with escaped quotes reads back with the quotes
     * themselves; a literal string has no escapes and keeps its backslashes.
     */
    @Test
    void basicStringEscapesAreDecoded() {
        rewriteRun(
          toml(
            """
              basic = "tomli; python_version < \\"3.11\\"\\tx\\\\y\\u00e9"
              literal = 'a\\"b'
              multiline = \"""
              first\\
                  second\"""
              beyondUnicode = "\\U00110000"
              """,
            spec -> spec.afterRecipe(doc ->
              assertThat(doc.getValues())
                .extracting(Toml.KeyValue.class::cast)
                .extracting(keyValue -> ((Toml.Literal) keyValue.getValue()).getValue())
                .containsExactly("tomli; python_version < \"3.11\"\tx\\yé", "a\\\"b", "firstsecond", "\\U00110000"))
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

    @Test
    void multilineEmptySequence() {
        rewriteRun(
          toml(
            """
            dependencies = [
            ]
            """
          )
        );
    }

    @Test
    void multilineEmptySequenceMultipleEmptyLines() {
        rewriteRun(
          toml(
            """
            dependencies = [
            
            
            ]
            """
          )
        );
    }

    @Test
    void singleLineEmptySequenceWithSpaces() {
        rewriteRun(
          toml(
            """
            dependencies = [    ]
            """
          )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"config.toml", "uv.lock", "poetry.lock", "pdm.lock", "Pipfile", ".xmake.cfg"})
    void acceptsTomlFiles(String fileName) {
        assertThat(new TomlParser().accept(Paths.get("project", fileName))).isTrue();
    }

    @Test
    void localDateTimeWithSpaceOrLowercaseDelimiter() {
        rewriteRun(
          toml(
            """
              a = 2017-02-25 12:00:01.123456789
              b = 1979-05-27t07:32:00
              c = 1979-05-27t07:32:00Z
              d = 1979-05-27 07:32:00+01:00
              """,
            spec -> spec.afterRecipe(doc -> assertThat(doc.getValues())
              .extracting(kv -> ((Toml.Literal) ((Toml.KeyValue) kv).getValue()).getValue())
              .containsExactly(
                LocalDateTime.of(2017, 2, 25, 12, 0, 1, 123456789),
                LocalDateTime.of(1979, 5, 27, 7, 32),
                OffsetDateTime.of(1979, 5, 27, 7, 32, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(1979, 5, 27, 7, 32, 0, 0, ZoneOffset.ofHours(1))))
          )
        );
    }

    @Test
    void fractionalSecondsBeyondNanosecondsAreTruncated() {
        rewriteRun(
          toml(
            """
              a = 1979-05-27T07:32:00.1234567899Z
              b = 1979-05-27 07:32:00.1234567899
              c = 07:32:00.1234567899
              """,
            spec -> spec.afterRecipe(doc -> assertThat(doc.getValues())
              .extracting(kv -> ((Toml.Literal) ((Toml.KeyValue) kv).getValue()).getValue())
              .containsExactly(
                OffsetDateTime.of(1979, 5, 27, 7, 32, 0, 123456789, ZoneOffset.UTC),
                LocalDateTime.of(1979, 5, 27, 7, 32, 0, 123456789),
                LocalTime.of(7, 32, 0, 123456789)))
          )
        );
    }

    @Test
    void emptyInlineTableKeepsItsSpace() {
        rewriteRun(
          toml(
            """
              dev-dependencies = { }
              coverageThreshold = {}
              headers = {  }
              """
          )
        );
    }

    @Test
    void integerBeyondSixtyFourBits() {
        rewriteRun(
          toml(
            """
              hex = 0x3D8660C0CB2D57B189C3D7995572A552F75E48B5
              dec = 228097355113300204138531148905234651262148041026195375645000724271212049151994375092458297304264351187709081232384
              max = 9223372036854775807
              min = -9223372036854775808
              """,
            spec -> spec.afterRecipe(doc -> assertThat(doc.getValues())
              .extracting(kv -> ((Toml.Literal) ((Toml.KeyValue) kv).getValue()).getValue())
              .containsExactly(
                new BigInteger("3D8660C0CB2D57B189C3D7995572A552F75E48B5", 16),
                new BigInteger("228097355113300204138531148905234651262148041026195375645000724271212049151994375092458297304264351187709081232384"),
                Long.MAX_VALUE,
                Long.MIN_VALUE))
          )
        );
    }

    @Test
    void fileNotInUtf8() {
        String source = "[default]\nencoded_variable=\"accents like ç and ã é\"\n";
        Parser.Input input = new Parser.Input(Paths.get("settings.toml"),
          () -> new ByteArrayInputStream(source.getBytes(StandardCharsets.ISO_8859_1)));

        SourceFile parsed = new TomlParser().parseInputs(singletonList(input), null, new InMemoryExecutionContext())
          .findFirst().orElseThrow();

        assertThat(parsed).isInstanceOf(Toml.Document.class);
        assertThat(parsed.printAll()).isEqualTo(source);
    }

    @Test
    void dottedKeyWithCharactersOutsideTheBasicMultilingualPlane() {
        rewriteRun(
          toml(
            """
              ['😀'.sub]
              a = 1
              ["𝐭𝐛𝐥".b."😀"]
              c = 2
              """
          )
        );
    }

    @Test
    void inlineTableSpanningLinesWithComments() {
        // TOML 1.1
        rewriteRun(
          toml(
            """
              tbl = {
                  key      = "a string",
                  # comments
                  moar-tbl =  {
                      key = 1,
                  }, # trailing comma
              }
              ProcessConfigsByRound = [
                  {
                      EnableRound = 0,
                      MaxRoundsWithoutNewBlockReceived = 10
                  }
              ]
              """
          )
        );
    }

    @Test
    void multilineBasicStringWithQuoteBeforeEscape() {
        rewriteRun(
          toml(
            """
              query = ""\"(
              (#match? @ternary_block "\\\\\\\\?[\\\\\\\\n]*nil:")
              )""\"
              quotes = ""\"Here are two quotation marks: "". Simple enough.""\"
              ends-in-quotes = ""\"a "quote\"""\"\"
              """,
            spec -> spec.afterRecipe(doc -> assertThat(doc.getValues())
              .extracting(kv -> ((Toml.Literal) ((Toml.KeyValue) kv).getValue()).getValue())
              .containsExactly(
                "(\n(#match? @ternary_block \"\\\\?[\\\\n]*nil:\")\n)",
                "Here are two quotation marks: \"\". Simple enough.",
                "a \"quote\"\""))
          )
        );
    }

    @Test
    void offsetDateTimeWithLowercaseZ() {
        rewriteRun(
          toml(
            """
              d = 1979-05-27T07:32:00z
              """,
            spec -> spec.afterRecipe(doc -> assertThat(((Toml.Literal) ((Toml.KeyValue) doc.getValues().getFirst()).getValue()).getValue())
              .isEqualTo(OffsetDateTime.of(1979, 5, 27, 7, 32, 0, 0, ZoneOffset.UTC)))
          )
        );
    }

    @Test
    void multilineLiteralStringEndingInQuotes() {
        rewriteRun(
          toml(
            """
              str = '''That,' she said, 'is still pointless.''''
              this-str-has-apostrophes='''' there's one already
              '' two more
              '''''
              """,
            spec -> spec.afterRecipe(doc -> assertThat(doc.getValues())
              .extracting(kv -> ((Toml.Literal) ((Toml.KeyValue) kv).getValue()).getValue())
              .containsExactly("That,' she said, 'is still pointless.'", "' there's one already\n'' two more\n''"))
          )
        );
    }

    @Test
    void arrayWithCommaAfterNewlineOrComment() {
        rewriteRun(
          toml(
            """
              arr = [
                1 # one
                , 2
                ,#3
                3
                , # trailing
              ]
              """,
            spec -> spec.afterRecipe(doc -> assertThat(((Toml.Array) ((Toml.KeyValue) doc.getValues().getFirst()).getValue()).getValues())
              .filteredOn(Toml.Literal.class::isInstance)
              .extracting(v -> ((Toml.Literal) v).getValue())
              .containsExactly(1L, 2L, 3L))
          )
        );
    }

    @Test
    void timesWithoutSeconds() {
        // TOML 1.1
        rewriteRun(
          toml(
            """
              odt = 1979-05-27 07:32Z
              odt-offset = 1979-05-27T07:32-07:00
              ldt = 1979-05-27T07:32
              lt = 07:32
              """,
            spec -> spec.afterRecipe(doc -> assertThat(doc.getValues())
              .extracting(kv -> ((Toml.Literal) ((Toml.KeyValue) kv).getValue()).getValue())
              .containsExactly(
                OffsetDateTime.of(1979, 5, 27, 7, 32, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(1979, 5, 27, 7, 32, 0, 0, ZoneOffset.ofHours(-7)),
                LocalDateTime.of(1979, 5, 27, 7, 32),
                LocalTime.of(7, 32)))
          )
        );
    }

    @Test
    void escapesOfToml11() {
        rewriteRun(
          toml(
            """
              esc = "\\e[0m"
              hex = "tab\\x09 e-acute\\xE9"
              ml = ""\"\\e and \\x41""\"
              """,
            spec -> spec.afterRecipe(doc -> assertThat(doc.getValues())
              .extracting(kv -> ((Toml.Literal) ((Toml.KeyValue) kv).getValue()).getValue())
              .containsExactly("\u001b[0m", "tab\t e-acute\u00e9", "\u001b and A"))
          )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"Pipfile.lock", "yarn.lock", "package-lock.json"})
    void rejectsNonTomlFiles(String fileName) {
        assertThat(new TomlParser().accept(Paths.get("project", fileName))).isFalse();
    }
}
