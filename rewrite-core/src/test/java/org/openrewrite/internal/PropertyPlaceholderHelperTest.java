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
        var s = helper.replacePlaceholders("${greeting} \\${java.version}", k -> {
            if ("greeting".equals(k)) return "hello";
            return null;
        });
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
}
