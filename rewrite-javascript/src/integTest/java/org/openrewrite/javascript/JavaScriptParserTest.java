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
package org.openrewrite.javascript;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.javascript.tree.JS;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class JavaScriptParserTest {

    private JavaScriptParser parser;

    @BeforeEach
    void before() {
        this.parser = JavaScriptParser.builder()
          .nodePath(Path.of("node"))
          .installationDir(Path.of("./rewrite/dist/src/rpc"))
          .build();
    }

    @Test
    void helloJavaScript() {
        @Language("js")
        String helloWorld = """
          console.info("Hello world!")
          """;
        Parser.Input input = Parser.Input.fromString(Paths.get("helloworld.js"), helloWorld);
        Optional<SourceFile> javascript = parser.parseInputs(List.of(input), null, new InMemoryExecutionContext()).findFirst();
        assertThat(javascript).containsInstanceOf(JS.CompilationUnit.class);
        assertThat(javascript.get()).satisfies(cu -> {
            assertThat(cu.printAll()).isEqualTo(helloWorld);
            assertThat(cu.getSourcePath()).isEqualTo(input.getPath());
        });
    }

    @Test
    void helloTypeScript() {
        @Language("ts")
        String helloWorld = """
          const message: string = "Hello world!";
          console.info(message);
          """;
        Parser.Input input = Parser.Input.fromString(Paths.get("helloworld.ts"), helloWorld);
        Optional<SourceFile> typescript = parser.parseInputs(List.of(input), null, new InMemoryExecutionContext()).findFirst();
        assertThat(typescript).containsInstanceOf(JS.CompilationUnit.class);
        assertThat(typescript.get()).satisfies(cu -> {
            assertThat(cu.printAll()).isEqualTo(helloWorld);
            assertThat(cu.getSourcePath()).isEqualTo(input.getPath());
        });
    }

    @Test
    void tsx() {
        @Language("tsx")
        String script = """
          import React from 'react';
          
          const JSXConstructsExample = () => {
            // Props object for spread attribute demonstration
            const buttonProps = {
              className: 'test-button',
              disabled: false,
              'data-testid': 'spread-button'
            };
          
            const linkProps = {
              href: 'https://example.com',
              target: '_blank',
              rel: 'noopener noreferrer'
            };
          
            return (
              <React.Fragment>
                {/* Fragment - wrapping multiple elements without extra DOM node */}
          
                {/* Basic JSX Element with attributes */}
                <div className="container" id="main-container" data-test="element-example">
                  <h1 title="Main heading">JSX Constructs Test</h1>
          
                  {/* Element with spread attributes */}
                  <button {...buttonProps} onClick={() => alert('Spread attributes work!')}>
                    Button with Spread Props
                  </button>
          
                  {/* Another spread attribute example */}
                  <a {...linkProps}>Link with Spread Props</a>
          
                  {/* Namespace example (commonly used with SVG) */}
                  <svg width="50" height="50" xmlns="http://www.w3.org/2000/svg">
                    <circle
                      cx="25"
                      cy="25"
                      r="20"
                      fill="blue"
                      xmlns:custom="http://example.com/custom"
                      custom:attribute="namespace-example"
                    />
                  </svg>
          
                  {/* Mixed attributes: regular, spread, and namespaced */}
                  <div
                    className="mixed-example"
                    {...{ 'data-spread': 'true', role: 'region' }}
                    aria:label="Mixed attributes example"
                    style={{ padding: '10px', border: '1px solid #ccc' }}
                  >
                    <p>This div uses regular attributes, spread attributes, and namespaced attributes</p>
                  </div>
                </div>
          
                {/* Short fragment syntax */}
                <>
                  <p>This paragraph is in a short fragment syntax</p>
                  <span>Along with this span</span>
                </>
              </React.Fragment>
            );
          };
          
          export default JSXConstructsExample;
          """;
        Parser.Input input = Parser.Input.fromString(Paths.get("helloworld.tsx"), script);
        Optional<SourceFile> typescript = parser.parseInputs(List.of(input), null, new InMemoryExecutionContext()).findFirst();
        assertThat(typescript).containsInstanceOf(JS.CompilationUnit.class);
        assertThat(typescript.get()).satisfies(cu -> {
//            assertThat(cu.printAll()).isEqualTo(helloWorld);
//            assertThat(cu.getSourcePath()).isEqualTo(input.getPath());
        });
    }

    @Test
    @Disabled
    void complexTypeScript() throws MalformedURLException {
        URL url = URI.create("https://raw.githubusercontent.com/sinclairzx81/typebox/f958156785350aa052c5f822bc2970d0945d887b/src/syntax/parser.ts").toURL();
        Parser.Input input = new Parser.Input(Paths.get("parser.ts"), null, () -> {
            try {
                return url.openStream();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        Optional<SourceFile> typescript = parser.parseInputs(List.of(input), null, new InMemoryExecutionContext()).findFirst();
        assertThat(typescript).containsInstanceOf(JS.CompilationUnit.class);
        assertThat(typescript.get()).satisfies(cu ->
            assertThat(cu.printAll()).isEqualTo(input.getSource(new InMemoryExecutionContext()).readFully()));
    }
}
