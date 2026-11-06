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
package org.openrewrite.coldfusion.style;

import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.coldfusion.ColdFusionParser;
import org.openrewrite.coldfusion.format.AutoFormatVisitor;
import org.openrewrite.coldfusion.tree.Cf;
import org.openrewrite.style.GeneralFormatStyle;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AutodetectTest {

    private static List<SourceFile> parse(String... pathsAndSources) {
        List<Parser.Input> inputs = new ArrayList<>();
        for (int i = 0; i < pathsAndSources.length; i += 2) {
            inputs.add(Parser.Input.fromString(Paths.get(pathsAndSources[i]), pathsAndSources[i + 1]));
        }
        List<SourceFile> parsed = new ArrayList<>();
        ColdFusionParser.builder().build().parseInputs(inputs, null, new InMemoryExecutionContext(t -> {
            throw new AssertionError(t);
        })).forEach(parsed::add);
        return parsed;
    }

    private static Autodetect detect(String... pathsAndSources) {
        Autodetect.Detector detector = Autodetect.detector();
        parse(pathsAndSources).forEach(detector::sample);
        return detector.build();
    }

    @Test
    void ortusStyle() {
        Autodetect autodetect = detect(
          "models/UserService.cfc",
          """
            component accessors="true" {
            \tproperty name="userDAO"    inject="UserDAO";
            \tproperty name="log"        inject="logbox:logger:{this}";

            \tfunction list( required string name, numeric max = 10 ){
            \t\tvar result = [];
            \t\tif ( arguments.max > 0 ){
            \t\t\tresult.append( userDAO.find( name = arguments.name ) );
            \t\t}
            \t\treturn result;
            \t}

            \tfunction count(){
            \t\tthis.title       = "Users";
            \t\tthis.author      = "Someone";
            \t\treturn arrayLen( list( "x" ) );
            \t}
            }
            """
        );
        TabsAndIndentsStyle tabs = autodetect.getStyle(TabsAndIndentsStyle.class);
        assertThat(tabs.getUseTabCharacter()).isTrue();
        SpacesStyle spaces = autodetect.getStyle(SpacesStyle.class);
        assertThat(spaces.getWithin().getFunctionCallParentheses()).isTrue();
        assertThat(spaces.getWithin().getFunctionDeclarationParentheses()).isTrue();
        assertThat(spaces.getWithin().getKeywordParentheses()).isTrue();
        assertThat(spaces.getOther().getBeforeFunctionBrace()).isFalse();
        assertThat(spaces.getOther().getBeforeStatementBrace()).isFalse();
        assertThat(spaces.getOther().getBeforeComponentBrace()).isTrue();
        assertThat(spaces.getOther().getKeepAlignedAssignments()).isTrue();
        assertThat(spaces.getTags().getKeepAlignedAttributes()).isTrue();
    }

    @Test
    void twoSpacesAndBracesOnTheirOwnLines() {
        Autodetect autodetect = detect(
          "Service.cfc",
          """
            component
            {
              function f(a)
              {
                if (a)
                {
                  return 1;
                }
                else
                {
                  return 2;
                }
              }
            }
            """
        );
        TabsAndIndentsStyle tabs = autodetect.getStyle(TabsAndIndentsStyle.class);
        assertThat(tabs.getUseTabCharacter()).isFalse();
        assertThat(tabs.getIndentSize()).isEqualTo(2);
        WrappingAndBracesStyle wrapping = autodetect.getStyle(WrappingAndBracesStyle.class);
        assertThat(wrapping.getBraces().getComponentOnNewLine()).isTrue();
        assertThat(wrapping.getBraces().getFunctionOnNewLine()).isTrue();
        assertThat(wrapping.getBraces().getStatementOnNewLine()).isTrue();
        assertThat(wrapping.getKeywordsAfterBlock().getElseOnNewLine()).isTrue();
    }

    @Test
    void tagConventions() {
        Autodetect autodetect = detect(
          "Greeter.cfc",
          """
            <cfcomponent output="false">
            <cffunction name="greet" output="false">
            \t<cftry>
            \t\t<cfset x = 1>
            \t<cfcatch>
            \t\t<cfset x = 2>
            \t</cfcatch>
            \t</cftry>
            \t<cfswitch expression="#x#">
            \t<cfcase value="1">
            \t\t<cfset y = 1>
            \t</cfcase>
            \t</cfswitch>
            </cffunction>
            </cfcomponent>
            """,
          "page.cfm",
          """
            <cfsilent>
            <cfset x = 1>
            <cfset y = 2>
            </cfsilent>
            <cfscript>
            \tx = 1;
            \ty = 2;
            </cfscript>
            """
        );
        TabsAndIndentsStyle.Tags tags = autodetect.getStyle(TabsAndIndentsStyle.class).getTags();
        assertThat(tags.getIndentComponentBody()).isFalse();
        assertThat(tags.getIndentCatchFromTry()).isFalse();
        assertThat(tags.getIndentCaseFromSwitch()).isFalse();
        assertThat(tags.getIndentSilentBody()).isFalse();
        assertThat(tags.getIndentScriptBody()).isTrue();
    }

    @Test
    void lineBreaks() {
        Autodetect autodetect = detect("a.cfm", "<cfset x = 1>\r\n<cfset y = 2>\r\n<cfset z = 3>\n");
        assertThat(autodetect.getStyle(GeneralFormatStyle.class).isUseCRLFNewLines()).isTrue();
    }

    @Test
    void noEvidenceLeavesCFFormatsDefaults() {
        Autodetect autodetect = detect("a.cfm", "<p>Hello</p>\n");
        assertThat(autodetect.getStyle(SpacesStyle.class)).isEqualTo(CFFormat.spaces());
        assertThat(autodetect.getStyle(TabsAndIndentsStyle.class)).isEqualTo(CFFormat.tabsAndIndents());
    }

    @Test
    void detectedStyleLeavesTheRepositorysOwnCodeAsItIs() {
        List<SourceFile> sources = parse(
          "models/UserService.cfc",
          """
            component accessors="true" {
            \tproperty name="userDAO"    inject="UserDAO";

            \tfunction list( required string name ){
            \t\tif ( len( arguments.name ) ){
            \t\t\treturn userDAO.find( name = arguments.name );
            \t\t}
            \t\treturn [];
            \t}
            }
            """
        );
        Autodetect.Detector detector = Autodetect.detector();
        sources.forEach(detector::sample);
        Autodetect autodetect = detector.build();
        Cf.CompilationUnit cu = (Cf.CompilationUnit) sources.get(0);
        Cf.CompilationUnit formatted = (Cf.CompilationUnit) new AutoFormatVisitor<Integer>()
          .visit(cu.withMarkers(cu.getMarkers().add(autodetect)), 0);
        assertThat(formatted.printAll()).isEqualTo(cu.printAll());
    }
}
