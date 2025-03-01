/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.javascript.tree;

import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.ExpectedToFail;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.javascript.Assertions.javaScript;

@SuppressWarnings({"JSFileReferences", "JSUnusedLocalSymbols", "TypeScriptCheckImport", "TypeScriptUnresolvedVariable"})
class ExportTest implements RewriteTest {

    @Test
    void exportDeclaration() {
        rewriteRun(
          javaScript(
            """
              class ZipCodeValidator {
                isAcceptable ( s : string ) {
                  return s . length === 5 ;
                }
              }
              export { ZipCodeValidator } ;
              """
          )
        );
    }

    @Test
    void fromClass() {
        rewriteRun(
          javaScript(
            """
              export * from "./f0.ts" ;
              """
          )
        );
    }

    @Test
    void exportAssignment() {
        rewriteRun(
          javaScript(
            """
              export default null;
              """
          )
        );
    }

    @Test
    void multiExport() {
        rewriteRun(
          javaScript(
            """
              export {
                  first ,
                  second ,
                  third
              }
              """
          )
        );
    }

    @Test
    void alias() {
        rewriteRun(
          javaScript(
            """
              export {
                  name as default
              }
              """
          )
        );
    }

    @Test
    void exportProperty() {
        rewriteRun(
          javaScript(
            """
              export const numberRegexp = /^[0-9]+$/ ;
              """
          )
        );
    }

    @ExpectedToFail
    @Test
    void exportInterface() {
        rewriteRun(
          javaScript(
            """
              export interface Foo {
                  ( value : any , defaultEncoder : ( value : any ) => any ) : any ;
              }
              """
          )
        );
    }

    @Test
    void exportInterfaceParameterizedAssignment() {
        rewriteRun(
          javaScript(
            """
              export interface Foo < D = any > {
                  url ? : string ;
                  method ? : Method | string ;
                  baseURL ? : string ;
              }
              """
          )
        );
    }

    @Test
    void exportInterfaceAndExtends() {
        rewriteRun(
          javaScript(
            """
              export interface Foo extends Bar {
                  encode ? : Baz ;
                  serialize ? : Buz ;
              }
              """
          )
        );
    }

    @Test
    void readOnlyProperty() {
        rewriteRun(
          javaScript(
            """
              export interface Foo {
                  readonly encode ? : string ;
              }
              """
          )
        );
    }

    @Test
    void exportFunction() {
        rewriteRun(
          javaScript(
            """
              export default function methodName() {
              }
              """
          )
        );
    }

    @Test
    void functionWithTypeParameter() {
        rewriteRun(
          javaScript(
            """
              export function spread < T , R > ( t : T , r : R ) { }
              """
          )
        );
    }

    @Test
    void exportType() {
        rewriteRun(
          javaScript(
            """
              export type Value = string | string[] | number | boolean | null;
              """
          )
        );
    }
}
