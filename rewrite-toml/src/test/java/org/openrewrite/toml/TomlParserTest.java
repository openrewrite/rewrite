package org.openrewrite.toml;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.Recipe;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.toml.Assertions.toml;

public class TomlParserTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(TomlParser.builder())
                .recipe(Recipe.noop());
    }

    @Test
    void GradleVersionCatalog() {
        rewriteRun(toml("""
                [versions]
                groovy = "3.0.5" # Check with team before updating
                checkstyle = "8.37"
                
                [libraries]
                groovy-core = { module = "org.codehaus.groovy:groovy", version.ref = "groovy" }
                groovy-json = { module = "org.codehaus.groovy:groovy-json", version.ref = "groovy" }
                groovy-nio = { module = "org.codehaus.groovy:groovy-nio", version.ref = "groovy" }
                # Used by module 1
                commons-lang3 = { group = "org.apache.commons", name = "commons-lang3", version = { strictly = "[3.8, 4.0[", prefer="3.9" } }
                
                [bundles]
                groovy = ["groovy-core", "groovy-json", "groovy-nio"]
                
                [plugins]
                versions = { id = "com.github.ben-manes.versions", version = "0.45.0" }
                """));
    }

    @Test
    void Comment() {
        rewriteRun(toml("""
                # This is a full-line comment
                key = "value"  # This is a comment at the end of a line
                another = "# This is not a comment"
                """));
    }

    @Test
    void BareKey() {
        rewriteRun(toml("""
                key = "value"
                bare_key = "value"
                bare-key = "value"
                1234 = "value"
                """));
    }

    @Test
    void QuotedKey() {
        rewriteRun(toml("""
                "127.0.0.1" = "value"
                "character encoding" = "value"
                "ʎǝʞ" = "value"
                'key2' = "value"
                'quoted "value"' = "value"
                """));
    }

    @Test
    void DottedKey() {
        rewriteRun(toml("""
                name = "Orange"
                physical.color = "orange"
                physical.shape = "round"
                site."google.com" = true
                3.14159 = "pi"
                """));
    }

    @Disabled("Not yet implemented")
    @Test
    void DottedKey_With_Extra_Whitespace() {
        rewriteRun(toml("""
                fruit.name = "banana"
                fruit. color = "yellow"
                fruit . flavor = "banana"
                """));
    }

    @Test
    void BasicString() {
        rewriteRun(toml("""
                str1 = "hello \\"world\\""
                """));
    }

    @Test
    void MultilineBasicString1() {
        rewriteRun(toml("""
                str2 = ""\"
                Roses are red
                Violets are blue""\"
                """));
    }

    @Test
    void MultilineBasicString2() {
        rewriteRun(toml("""
                str3 = ""\"
                The quick brown \\
                
                
                  fox jumps over \\
                    the lazy dog.""\"
                """));
    }

    @Test
    void MultilineBasicString3() {
        rewriteRun(toml("""
                str4 = ""\"\\
                      The quick brown \\
                      fox jumps over \\
                      the lazy dog.\\
                      ""\"
                """));
    }

    @Test
    void MultilineBasicString4() {
        rewriteRun(toml("""
                str5 = ""\"Here is an in-line multiline string.""\"
                """));
    }

    @Test
    void LiteralString() {
        rewriteRun(toml("""
                winpath = 'C:\\Users\\nodejs\\templates'
                winpath2 = '\\\\ServerX\\admin$\\system32\\'
                quoted = 'Tom "Dubs" Preston-Werner'
                regex = '<\\i\\c*\\s*>'
                """));
    }

    @Test
    void MultilineLiteralString() {
        rewriteRun(toml("""
                regex2 = '''I [dw]on't need \\d{2} apples'''
                lines  = '''
                The first newline is
                trimmed in raw strings.
                   All other whitespace
                   is preserved.
                '''
                """));
    }

    @Test
    void Integer() {
        rewriteRun(toml("""
                int1 = +99
                int2 = 42
                int3 = 0
                int4 = -17
                """));
    }

    @Test
    void Integer_With_Separators() {
        rewriteRun(toml("""
                int5 = 1_000
                int6 = 5_349_221
                int7 = 53_49_221
                int8 = 1_2_3_4_5
                """));
    }

    @Test
    void Integer_From_Hexadecimal() {
        rewriteRun(toml("""
                hex1 = 0xDEADBEEF
                hex2 = 0xdeadbeef
                hex3 = 0xdead_beef
                """));
    }

    @Test
    void Integer_From_Octal() {
        rewriteRun(toml("""
                hex1 = 0xDEADBEEF
                hex2 = 0xdeadbeef
                hex3 = 0xdead_beef
                """));
    }

    @Test
    void Integer_From_Binary() {
        rewriteRun(toml("""
                bin1 = 0b11010110
                """));
    }

    @Test
    void Float_Fractional() {
        rewriteRun(toml("""
                flt1 = +1.0
                flt2 = 3.1415
                flt3 = -0.01
                """));
    }

    @Test
    void Float_Exponent() {
        rewriteRun(toml("""
                flt4 = 5e+22
                flt5 = 1e06
                flt6 = -2E-2
                """));
    }

    @Test
    void Float_Fractional_And_Exponent() {
        rewriteRun(toml("""
                flt7 = 6.626e-34
                """));
    }

    @Test
    void Float_With_Separators() {
        rewriteRun(toml("""
                flt8 = 224_617.445_991_228
                """));
    }

    @Test
    void Float_Using_Inf() {
        rewriteRun(toml("""
                sf1 = inf
                sf2 = +inf
                sf3 = -inf
                """));
    }

    @Test
    void Float_Using_Nan() {
        rewriteRun(toml("""
                sf4 = nan
                sf5 = +nan
                sf6 = -nan
                """));
    }

    @Test
    void Boolean() {
        rewriteRun(toml("""
                bool1 = true
                bool2 = false
                """));
    }

    @Test
    void OffsetDateTime() {
        rewriteRun(toml("""
                odt1 = 1979-05-27T07:32:00Z
                odt2 = 1979-05-27T00:32:00-07:00
                odt3 = 1979-05-27T00:32:00.999999-07:00
                odt4 = 1979-05-27 07:32:00Z
                """));
    }

    @Test
    void LocalDateTime() {
        rewriteRun(toml("""
                ldt1 = 1979-05-27T07:32:00
                ldt2 = 1979-05-27T00:32:00.999999
                """));
    }

    @Test
    void LocalDate() {
        rewriteRun(toml("""
                ld1 = 1979-05-27
                """));
    }

    @Test
    void LocalTime() {
        rewriteRun(toml("""
                lt1 = 07:32:00
                lt2 = 00:32:00.999999
                """));
    }

    @Test
    void Array() {
        rewriteRun(toml("""
                integers = [ 1, 2, 3 ]
                colors = [ "red", "yellow", "green" ]
                string_array = [ "all", 'strings', ""\"are the same""\", '''type''' ]
                """));
    }

    @Test
    void Array_Containing_Nested_Arrays() {
        rewriteRun(toml("""
                nested_arrays_of_ints = [ [ 1, 2 ], [3, 4, 5] ]
                nested_mixed_array = [ [ 1, 2 ], ["a", "b", "c"] ]
                """));
    }

    @Test
    void Array_Using_Mixed_Types() {
        rewriteRun(toml("""
                numbers = [ 0.1, 0.2, 0.5, 1, 2, 5 ]
                contributors = [
                  "Foo Bar <foo@example.com>",
                  { name = "Baz Qux", email = "bazqux@example.com", url = "https://example.com/bazqux" }
                ]
                """));
    }

    @Test
    void Array_Multiline() {
        rewriteRun(toml("""
                integers2 = [
                  1, 2, 3
                ]
                integers3 = [
                  1,
                  2,
                ]
                """));
    }

    @Test
    void Table() {
        rewriteRun(toml("""
                [table-1]
                key1 = "some string"
                key2 = 123
                
                [table-2]
                key1 = "another string"
                key2 = 456
                """));
    }

    @Test
    void Table_Empty() {
        rewriteRun(toml("""
                [table]
                """));
    }

    @Test
    void Table_With_Dotted_And_Quoted_Name() {
        rewriteRun(toml("""
                [dog."tater.man"]
                type.name = "pug"
                """));
    }

    @Test
    void Table_With_Extra_Whitespace_In_Name() {
        rewriteRun(toml("""
                [a.b.c]
                [ d.e.f ]
                [ g .  h  . i ]
                [ j . "ʞ" . 'l' ]
                """));
    }

    @Test
    void Table_With_SuperTable_Declared_After_Dotted_Table() {
        rewriteRun(toml("""
                [x.y.z.w]
                [x]
                """));
    }

    @Test
    void Table_With_SubTable_Added_Later() {
        rewriteRun(toml("""
                [fruit]
                apple.color = "red"
                apple.taste.sweet = true
                
                [fruit.apple.texture]
                smooth = true
                """));
    }

    @Test
    void InlineTable() {
        rewriteRun(toml("""
                name = { first = "Tom", last = "Preston-Werner" }
                point = { x = 1, y = 2 }
                animal = { type.name = "pug" }
                """));
    }

    @Test
    void ArrayOfTables() {
        rewriteRun(toml("""
                [[products]]
                name = "Hammer"
                sku = 738594937
                
                [[products]]
                
                [[products]]
                name = "Nail"
                sku = 284758393
                color = "gray"
                """));
    }

    @Test
    void ArrayOfTables_With_SubTables_And_SubArrays() {
        rewriteRun(toml("""
                [[fruits]]
                name = "apple"
                
                [fruits.physical]  # subtable
                color = "red"
                shape = "round"
                
                [[fruits.varieties]]  # nested array of tables
                name = "red delicious"
                
                [[fruits.varieties]]
                name = "granny smith"
                
                
                [[fruits]]
                name = "banana"
                
                [[fruits.varieties]]
                name = "plantain"
                """));
    }

    @Test
    void ArrayOfTables_From_InlineTables() {
        rewriteRun(toml("""
                points = [ { x = 1, y = 2, z = 3 },
                           { x = 7, y = 8, z = 9 },
                           { x = 2, y = 4, z = 8 } ]
                """));
    }
}
