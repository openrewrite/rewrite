/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.cobol.internal;

public class StringWithOriginalPositions {
    public String originalText;
    public String[] originalFileName;

    // preprocessedText[i] == originalText[originalPositions[i]]
    // or originalPositions[i] == -1 if preprocessedText[i] does not correspond to an original
    public int[] originalPositions;

    public String preprocessedText;

    public StringWithOriginalPositions(String text, String originalText, int[] originalPositions) {
        assert text.length() == originalPositions.length;
        this.preprocessedText = text;
        this.originalText = originalText;
        this.originalPositions = originalPositions;

        // preprocessedText[i] == originalText[originalPositions[i]]
        // or originalPositions[i] == -1 if preprocessedText[i] does not correspond to an original

        StringBuffer sb = new StringBuffer();
        for(int i=0; i<preprocessedText.length(); i++) {
            int pos = originalPositions[i];
            if(pos != -1) {
                if(pos < originalText.length()) {
                    char c = originalText.charAt(pos);
                    sb.append(c);
                } else {
                    sb.append("%");
                }
            }
        }

        System.out.println(quote(preprocessedText));
        System.out.println(quote(sb.toString()));
        System.out.println();
    }

    // Scaffolding, to be removed.
    public StringWithOriginalPositions(StringWithOriginalPositions code, String expandedText) {
        this.preprocessedText = expandedText;
        this.originalText = code.originalText;
        this.originalPositions = code.originalPositions; // XXX
    }

    public String getPreprocessedText(int start, int stop) {
        return preprocessedText.substring(start, stop + 1);
    }

    public String getOriginalText(int start, int stop) {
        return originalText.substring(originalPositions[start], originalPositions[stop] + 1);
    }

    public static String quote(CharSequence str) {
        if (str == null)
            return "null";
        int len = str.length();
        StringBuilder buf = new StringBuilder(len + 10);
        buf.append("\"");
        for (int i = 0; i < len; i++) {
            char c = str.charAt(i);
            switch (c) {
                case '\n':
                    buf.append("\\n");
                    break;
                case '\r':
                    buf.append("\\r");
                    break;
                case '\t':
                    buf.append("\\t");
                    break;
                case '\"':
                    buf.append("\\\"");
                    break;
                case '\\':
                    buf.append("\\\\");
                    break;
                default:
                    if (c >= ' ' && c < 65000) {
                        // ASCII
                        buf.append(c);
                    } else {
                        // Unicode
                        buf.append(String.format("\\u%04x", (int) c));
                    }
            }
        }
        buf.append("\"");
        return buf.toString();
    }
}
