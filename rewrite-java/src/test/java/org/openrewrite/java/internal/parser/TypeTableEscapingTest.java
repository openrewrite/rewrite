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
package org.openrewrite.java.internal.parser;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests to verify that the TypeTable escaping and splitting logic correctly handles edge cases.
 */
class TypeTableEscapingTest {

    @Test
    void testSplitUnescapedBasicCases() {
        // Basic case - should split on unescaped pipes
        String[] result = TsvEscapeUtils.splitAnnotationList("value1|value2|value3", '|');
        assertThat(result).containsExactly("value1", "value2", "value3");

        // Should NOT split on escaped pipes, and should unescape them
        result = TsvEscapeUtils.splitAnnotationList("value1\\|with\\|pipes", '|');
        assertThat(result).containsExactly("value1|with|pipes");

        // Mixed case - some escaped, some not - unescape the escaped ones
        result = TsvEscapeUtils.splitAnnotationList("value1\\|escaped|value2|value3\\|also\\|escaped", '|');
        assertThat(result).containsExactly("value1|escaped", "value2", "value3|also|escaped");
    }

    @Test
    void testSplitUnescapedComplexEscaping() {
        // Escaped backslash followed by pipe - should split
        // \\| means: escaped backslash (\) + unescaped pipe (|) - should split
        String[] result = TsvEscapeUtils.splitAnnotationList("value1\\\\|value2", '|');
        assertThat(result).containsExactly("value1\\", "value2");
        
        // Multiple consecutive backslashes before pipe - even number means last pipe is unescaped
        result = TsvEscapeUtils.splitAnnotationList("value1\\\\\\\\|value2", '|');
        assertThat(result).containsExactly("value1\\\\", "value2");

        // Escaped backslash followed by escaped pipe - should NOT split
        // \\\| means: escaped backslash (\) + escaped pipe (\|) - should NOT split
        result = TsvEscapeUtils.splitAnnotationList("value1\\\\\\|value2", '|');
        assertThat(result).containsExactly("value1\\|value2");
        
        // Test edge case: single backslash at end
        result = TsvEscapeUtils.splitAnnotationList("value1\\\\", '|');
        assertThat(result).containsExactly("value1\\");
        
        // Test edge case: multiple delimiters
        result = TsvEscapeUtils.splitAnnotationList("a\\|b|c\\\\|d|e\\\\\\|f", '|');
        assertThat(result).containsExactly("a|b", "c\\", "d", "e\\|f");
    }

    @Test
    void testRealWorldAnnotationCases() {
        // Real annotation strings that might appear in TypeTable
        // Format: @Annotation(value="string with pipes")  
        String annotationList = "@Value(\"Hello\\|World\")|@RequestMapping(value=\"/api\\|v1\",method=\"GET\")";
        
        String[] result = TsvEscapeUtils.splitAnnotationList(annotationList, '|');
        
        // What we expect: should split on the middle |, but not on the \\| inside strings
        // The escaped pipes inside the strings should be unescaped
        assertThat(result).hasSize(2);
        assertThat(result[0]).isEqualTo("@Value(\"Hello|World\")");
        assertThat(result[1]).isEqualTo("@RequestMapping(value=\"/api|v1\",method=\"GET\")");
        
        // Test case with multiple annotations containing escaped pipes
        String complexAnnotationList = "@Value(\"Config\\|File\")|@Pattern(regex=\"test\\|regex\")|@Description(\"Some\\|description\")";
        result = TsvEscapeUtils.splitAnnotationList(complexAnnotationList, '|');
        
        assertThat(result).hasSize(3);
        assertThat(result[0]).isEqualTo("@Value(\"Config|File\")");
        assertThat(result[1]).isEqualTo("@Pattern(regex=\"test|regex\")");
        assertThat(result[2]).isEqualTo("@Description(\"Some|description\")");
    }

    @Test
    void testEscapingRoundTrip() {
        // Test that escaping and unescaping works correctly
        String original = "Hello|World\"With'Quotes";
        
        // Escape using the serializer
        String escaped = AnnotationSerializer.escapeDelimiters(original);
        System.out.println("Original: " + original);
        System.out.println("Escaped: " + escaped);
        
        // Unescape using the serializer
        String unescaped = AnnotationSerializer.unescapeDelimiters(escaped);
        System.out.println("Unescaped: " + unescaped);
        
        assertThat(unescaped).isEqualTo(original);
    }
}
