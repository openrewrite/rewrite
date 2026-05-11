/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.internal;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class PropertyPlaceholderHelperTest {

    @Test
    void nested() {
        var helper = new PropertyPlaceholderHelper("%%{", "}", null);
        var s = helper.replacePlaceholders("%%{%%{k1}}", k -> switch (k) {
            case "k1" -> "k2";
            case "k2" -> "jon";
            default -> throw new UnsupportedOperationException();
        });
        assertThat(s).isEqualTo("jon");
    }

    @Test
    void notOnlyPlaceholders() {
        var helper = new PropertyPlaceholderHelper("%%{", "}", null);
        var s = helper.replacePlaceholders("Oh, %%{k1} there %%{k2}!", k -> switch (k) {
            case "k1" -> "hi";
            case "k2" -> "jon";
            default -> throw new UnsupportedOperationException();
        });
        assertThat(s).isEqualTo("Oh, hi there jon!");
    }

    @Test
    void dashedSeparation() {
        var helper = new PropertyPlaceholderHelper("%%{", "}", null);
        var s = helper.replacePlaceholders("%%{k1}-%%{k2}", k -> switch (k) {
            case "k1" -> "hi";
            case "k2" -> "jon";
            default -> throw new UnsupportedOperationException();
        });
        assertThat(s).isEqualTo("hi-jon");
    }

    @Test
    void spaceSeparation() {
        var helper = new PropertyPlaceholderHelper("%%{", "}", null);
        var s = helper.replacePlaceholders("%%{k1} %%{k2}", k -> switch (k) {
            case "k1" -> "hi";
            case "k2" -> "jon";
            default -> throw new UnsupportedOperationException();
        });
        assertThat(s).isEqualTo("hi jon");
    }

    @Test
    void noSeparation() {
        var helper = new PropertyPlaceholderHelper("%%{", "}", null);
        var s = helper.replacePlaceholders("%%{k1}%%{k2}", k -> switch (k) {
            case "k1" -> "hi";
            case "k2" -> "jon";
            default -> throw new UnsupportedOperationException();
        });
        assertThat(s).isEqualTo("hijon");
    }

    @Test
    void withValueSeparatorAndValueReplacement() {
        var helper = new PropertyPlaceholderHelper("%%{", "}", ",");
        var s = helper.replacePlaceholders("%%{k1,oh} %%{k2}", k -> switch (k) {
            case "k1" -> "hi";
            case "k2" -> "jon";
            // Note: this needs to not throw an exception because there won't be a match for "k1,oh" as a placeholder
            default -> null;
        });
        assertThat(s).isEqualTo("hi jon");
    }

    @Test
    void escapedPlaceholder() {
        var helper = new PropertyPlaceholderHelper("${", "}", null);
        var s = helper.replacePlaceholders("\\${java.version}", k -> "should-not-resolve");
        assertThat(s).isEqualTo("${java.version}");
    }

    @Test
    void escapedPlaceholderWithOtherPrefix() {
        var helper = new PropertyPlaceholderHelper("%%{", "}", null);
        var s = helper.replacePlaceholders("\\%%{k1}", k -> "should-not-resolve");
        assertThat(s).isEqualTo("%%{k1}");
    }

    @Test
    void mixedEscapedAndResolvedPlaceholders() {
        var helper = new PropertyPlaceholderHelper("${", "}", null);
        var s = helper.replacePlaceholders("${greeting} \\${java.version}", k -> "greeting".equals(k) ? "hello" : null);
        assertThat(s).isEqualTo("hello ${java.version}");
    }

    @Test
    void unresolvedPlaceholderLeftAsIs() {
        var helper = new PropertyPlaceholderHelper("${", "}", null);
        var s = helper.replacePlaceholders("${unresolved}", k -> null);
        assertThat(s).isEqualTo("${unresolved}");
    }

    @Test
    void normalResolutionStillWorks() {
        var helper = new PropertyPlaceholderHelper("${", "}", null);
        var s = helper.replacePlaceholders("${greeting} ${name}", k -> switch (k) {
            case "greeting" -> "hello";
            case "name" -> "world";
            default -> null;
        });
        assertThat(s).isEqualTo("hello world");
    }

    @Test
    void backslashNotBeforePrefixIsPreserved() {
        var helper = new PropertyPlaceholderHelper("${", "}", null);
        var s = helper.replacePlaceholders("path\\to\\file", k -> null);
        assertThat(s).isEqualTo("path\\to\\file");
    }

    @Test
    void selfReferencingPropertyDoesNotStackOverflow() {
        var helper = new PropertyPlaceholderHelper("${", "}", null);
        var props = new java.util.HashMap<String, String>();
        props.put("revision", "${revision}");
        var s = helper.replacePlaceholders("${revision}", props::get);
        assertThat(s).isEqualTo("${revision}");
    }

    @Test
    void cyclicPropertyReferenceDoesNotStackOverflow() {
        var helper = new PropertyPlaceholderHelper("${", "}", null);
        var props = new java.util.HashMap<String, String>();
        props.put("revision", "${project.build.version}");
        props.put("project.build.version", "${revision}");
        var s = helper.replacePlaceholders("${revision}", props::get);
        // The cycle should be detected and the placeholder left unresolved
        assertThat(s).contains("${");
    }

    @Test
    void withValueSeparatorAndNullReplacement() {
        var helper = new PropertyPlaceholderHelper("%%{", "}", ",");
        var s = helper.replacePlaceholders("%%{k1,oh}%%{k2}", k -> switch (k) {
            case "k1" -> null;
            case "k2" -> "jon";
            // Note: this needs to not throw an exception because there won't be a match for "k1,oh" as a placeholder
            default -> null;
        });
        assertThat(s).isEqualTo("ohjon");
    }

    @Test
    void cachesParsedSegmentsAcrossCalls() {
        var helper = new PropertyPlaceholderHelper("#{", "}", null);
        helper.clearParsedTemplateCache();
        var template = "assertThat(#{any(java.lang.Object)}).isNotNull()";
        var resolver = new AtomicInteger();
        for (int i = 0; i < 1_000; i++) {
            var s = helper.replacePlaceholders(template, k -> "any(java.lang.Object)".equals(k) ?
                    "x" + resolver.incrementAndGet() :
                    null);
            assertThat(s).startsWith("assertThat(x").endsWith(").isNotNull()");
        }
        // The template string is parsed exactly once and its segments are
        // cached for subsequent calls.
        assertThat(helper.parsedTemplateCacheSize()).isEqualTo(1);
        assertThat(resolver.get()).isEqualTo(1_000);
    }

    @Test
    void cacheIsSharedAcrossInstancesWithSameConfiguration() {
        // Two separate helper instances with the same prefix/suffix share the
        // cache, so a template parsed by one instance is reused by the next.
        var first = new PropertyPlaceholderHelper("#{", "}", null);
        first.clearParsedTemplateCache();
        first.replacePlaceholders("hello #{name}!", k -> "world");
        assertThat(first.parsedTemplateCacheSize()).isEqualTo(1);

        var second = new PropertyPlaceholderHelper("#{", "}", null);
        assertThat(second.parsedTemplateCacheSize()).isEqualTo(1);
        assertThat(second.replacePlaceholders("hello #{name}!", k -> "Claude"))
                .isEqualTo("hello Claude!");
        assertThat(second.parsedTemplateCacheSize()).isEqualTo(1);
    }

    @Test
    void cachedAndUncachedAgreeOnComplexTemplates() {
        // Stress-test: a template with literal text, multiple placeholders,
        // an escaped placeholder, and a default-value placeholder. The result
        // should match expectations whether the template is parsed fresh or
        // served from the cache.
        var helper = new PropertyPlaceholderHelper("${", "}", ":");
        helper.clearParsedTemplateCache();
        var template = "head ${a} mid ${b:fallback} tail \\${literal} end";
        var first = helper.replacePlaceholders(template, k -> switch (k) {
            case "a" -> "ALPHA";
            case "b" -> null;
            default -> null;
        });
        var second = helper.replacePlaceholders(template, k -> switch (k) {
            case "a" -> "ALPHA";
            case "b" -> null;
            default -> null;
        });
        assertThat(first).isEqualTo("head ALPHA mid fallback tail ${literal} end");
        assertThat(second).isEqualTo(first);
    }
}
