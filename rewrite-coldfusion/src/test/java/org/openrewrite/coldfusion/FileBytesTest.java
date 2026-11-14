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
package org.openrewrite.coldfusion;

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.coldfusion.tree.Cf;
import org.openrewrite.tree.ParseError;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * A file prints back its own bytes, its byte order mark among them, and bytes that are no text fail to parse.
 */
class FileBytesTest {
    private static final byte[] BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    private final ExecutionContext ctx = new InMemoryExecutionContext(t -> {
    });

    private static byte[] bytes(Object... parts) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (Object part : parts) {
            byte[] b = part instanceof String ? ((String) part).getBytes(StandardCharsets.UTF_8) : (byte[]) part;
            out.write(b, 0, b.length);
        }
        return out.toByteArray();
    }

    private static Parser.Input input(String path, byte[] bytes) {
        return new Parser.Input(Paths.get(path), () -> new ByteArrayInputStream(bytes));
    }

    private SourceFile parse(String path, byte[] bytes) {
        return ColdFusionParser.builder().build().parseInputs(singletonList(input(path, bytes)), null, ctx)
          .findFirst().orElseThrow(IllegalStateException::new);
    }

    @Test
    void aByteOrderMarkPrintsBack() {
        for (byte[] bytes : new byte[][]{
          bytes(BOM, "<p>Hello</p>"),
          // a file of nothing but a byte order mark
          BOM,
          // a second one is a U+FEFF in the text
          bytes(BOM, BOM, "<cfsilent>\r\n</cfsilent>")}) {
            SourceFile parsed = parse("index.cfm", bytes);
            assertThat(parsed).isInstanceOf(Cf.CompilationUnit.class);
            assertThat(parsed.isCharsetBomMarked()).isTrue();
            assertThat(parsed.printAllAsBytes()).isEqualTo(bytes);
        }
        for (byte[] bytes : new byte[][]{bytes(BOM, "component {}"), bytes(BOM, BOM, "component {}")}) {
            SourceFile parsed = parse("models/User.cfc", bytes);
            assertThat(parsed).isInstanceOf(Cf.CompilationUnit.class);
            assertThat(parsed.printAllAsBytes()).isEqualTo(bytes);
        }
    }

    @Test
    void printingTheSameTextIsNotPrintingTheSameBytes() {
        byte[] bytes = bytes(BOM, "<p>Hello</p>");
        Cf.CompilationUnit cu = (Cf.CompilationUnit) parse("index.cfm", bytes);
        assertThat(cu.printEqualsInput(input("index.cfm", bytes), ctx)).isTrue();
        // the text printed is the same without the byte order mark, but the bytes are not
        assertThat(cu.withCharsetBomMarked(false).printEqualsInput(input("index.cfm", bytes), ctx)).isFalse();
    }

    @Test
    void bytesThatAreNoTextFailToParse() {
        // so no recipe, AutoFormat among them, rewrites a zip archive or a template cfencode encrypted
        byte[] zip = bytes("PK", new byte[]{3, 4, 10, 0, 0, 0, 0, 0, (byte) 0x8D, 0x60}, "assets/");
        byte[] encoded = bytes("Allaire Cold Fusion Template\nHeader Size: New Version\n",
          new byte[]{(byte) 0x81, 0x00, (byte) 0xD3, 0x17, (byte) 0x9D, 0x00});
        assertThat(parse("theme.zip.cfm", zip)).isInstanceOf(ParseError.class);
        assertThat(parse("Captcha.cfc", encoded)).isInstanceOf(ParseError.class);
        // text in Windows-1252 is text
        byte[] latin = bytes("<p>Caf", new byte[]{(byte) 0xE9}, "</p>");
        SourceFile parsed = parse("index.cfm", latin);
        assertThat(parsed).isInstanceOf(Cf.CompilationUnit.class);
        assertThat(parsed.printAllAsBytes()).isEqualTo(latin);
    }
}
