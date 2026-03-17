/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.csharp.rpc;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.rpc.RpcRecipe;
import org.openrewrite.xml.XmlParser;
import org.openrewrite.xml.tree.Xml;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for XML LST round-tripping through the Java ↔ C# RPC bridge.
 * <p>
 * Tests the full flow: Java parses XML → sends to C# → C# visitor modifies tree →
 * modified tree shipped back to Java → Java verifies the modifications.
 * Exercises XmlSender and XmlReceiver on both sides.
 */
@Timeout(value = 120, unit = TimeUnit.SECONDS)
class XmlRpcBridgeTest {

    private CSharpRewriteRpc rpc;

    @BeforeEach
    void setUp() {
        Path csharpServerEntry = findCSharpServerEntry();
        CSharpRewriteRpc.setFactory(
                CSharpRewriteRpc.builder()
                        .csharpServerEntry(csharpServerEntry)
                        .timeout(Duration.ofSeconds(120))
                        .traceRpcMessages(true)
                        .log(Paths.get("/tmp/xml-rpc-bridge.log"))
        );
        rpc = CSharpRewriteRpc.getOrStart();
    }

    @AfterEach
    void tearDown() {
        CSharpRewriteRpc.shutdownCurrent();
    }

    private static Path findCSharpServerEntry() {
        Path basePath = Paths.get(System.getProperty("user.dir"));
        Path[] searchPaths = {
                basePath.resolve("csharp"),
                basePath.resolve("rewrite-csharp/csharp"),
        };
        for (Path searchPath : searchPaths) {
            Path csproj = searchPath.resolve("OpenRewrite.Tool/OpenRewrite.Tool.csproj");
            if (csproj.toFile().exists()) {
                return csproj.toAbsolutePath().normalize();
            }
        }
        throw new IllegalStateException("Could not find C# Rewrite project");
    }

    private Xml.Document parseXml(String source) {
        return (Xml.Document) new XmlParser().parse(source).findFirst().orElseThrow();
    }

    /**
     * Applies a C#-side recipe to an XML document and returns the modified result.
     */
    private Xml.Document visitOnCSharp(Xml.Document doc, String recipeName, Map<String, Object> options) {
        RpcRecipe recipe = rpc.prepareRecipe(recipeName, options);
        InMemoryExecutionContext ctx = new InMemoryExecutionContext();
        Tree result = rpc.visit(doc, recipe.getEditVisitor(), ctx);
        assertThat(result).isNotNull().isInstanceOf(Xml.Document.class);
        return (Xml.Document) result;
    }

    @Test
    void changeCharDataText() {
        String source = """
                <root>
                    <child>original</child>
                </root>
                """;

        Xml.Document doc = parseXml(source);
        Xml.Document result = visitOnCSharp(doc,
                "OpenRewrite.Xml.Recipes.ChangeXmlCharData",
                Map.of("OldText", "original", "NewText", "modified"));

        // Verify the modification came back from C#
        assertThat(result).isNotSameAs(doc);
        Xml.Tag root = result.getRoot();
        Xml.Tag child = (Xml.Tag) root.getContent().get(0);
        Xml.CharData charData = (Xml.CharData) child.getContent().get(0);
        assertThat(charData.getText()).isEqualTo("modified");
    }

    @Test
    void changeCharDataPreservesStructure() {
        String source = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <groupId>org.example</groupId>
                    <artifactId>my-lib</artifactId>
                    <version>1.0.0</version>
                </project>
                """;

        Xml.Document doc = parseXml(source);
        Xml.Document result = visitOnCSharp(doc,
                "OpenRewrite.Xml.Recipes.ChangeXmlCharData",
                Map.of("OldText", "1.0.0", "NewText", "2.0.0"));

        // Version should be changed
        Xml.Tag version = result.getRoot().getChild("version").orElseThrow();
        assertThat(version.getValue()).hasValue("2.0.0");

        // Other tags should be unchanged
        Xml.Tag groupId = result.getRoot().getChild("groupId").orElseThrow();
        assertThat(groupId.getValue()).hasValue("org.example");

        Xml.Tag artifactId = result.getRoot().getChild("artifactId").orElseThrow();
        assertThat(artifactId.getValue()).hasValue("my-lib");

        // Prolog should survive round-trip
        assertThat(result.getProlog()).isNotNull();
        assertThat(result.getProlog().getXmlDecl()).isNotNull();
    }

    @Test
    void changeAttributeValue() {
        String source = """
                <root attr="old" other="keep">
                    <child key="data"/>
                </root>
                """;

        Xml.Document doc = parseXml(source);
        Xml.Document result = visitOnCSharp(doc,
                "OpenRewrite.Xml.Recipes.ChangeXmlAttribute",
                Map.of("AttrName", "attr", "NewValue", "new"));

        // "attr" should be changed
        Xml.Attribute attr = result.getRoot().getAttributes().get(0);
        assertThat(attr.getKey().getName()).isEqualTo("attr");
        assertThat(attr.getValueAsString()).isEqualTo("new");

        // "other" should be unchanged
        Xml.Attribute other = result.getRoot().getAttributes().get(1);
        assertThat(other.getKey().getName()).isEqualTo("other");
        assertThat(other.getValueAsString()).isEqualTo("keep");
    }

    @Test
    void noChangeReturnsOriginal() {
        String source = """
                <root>
                    <child>hello</child>
                </root>
                """;

        Xml.Document doc = parseXml(source);
        Xml.Document result = visitOnCSharp(doc,
                "OpenRewrite.Xml.Recipes.ChangeXmlCharData",
                Map.of("OldText", "nonexistent", "NewText", "whatever"));

        // No matching text, so tree should be unchanged
        assertThat(result).isSameAs(doc);
    }

    @Test
    void changeCharDataInNestedStructure() {
        String source = """
                <dependencies>
                    <dependency>
                        <groupId>com.example</groupId>
                        <artifactId>lib-a</artifactId>
                        <version>1.0</version>
                    </dependency>
                    <dependency>
                        <groupId>com.example</groupId>
                        <artifactId>lib-b</artifactId>
                        <version>1.0</version>
                    </dependency>
                </dependencies>
                """;

        Xml.Document doc = parseXml(source);
        Xml.Document result = visitOnCSharp(doc,
                "OpenRewrite.Xml.Recipes.ChangeXmlCharData",
                Map.of("OldText", "1.0", "NewText", "2.0"));

        // Both version elements should be changed
        for (Xml.Tag dep : result.getRoot().getChildren("dependency")) {
            Xml.Tag version = dep.getChild("version").orElseThrow();
            assertThat(version.getValue()).hasValue("2.0");
        }
    }

    @Test
    void printRoundTripAfterModification() {
        String source = """
                <root>
                    <child>original</child>
                </root>
                """;

        Xml.Document doc = parseXml(source);
        Xml.Document result = visitOnCSharp(doc,
                "OpenRewrite.Xml.Recipes.ChangeXmlCharData",
                Map.of("OldText", "original", "NewText", "changed"));

        // Print on C# side should reflect the modification
        String printed = rpc.print(result);
        String expected = """
                <root>
                    <child>changed</child>
                </root>
                """;
        assertThat(printed).isEqualTo(expected);
    }
}
