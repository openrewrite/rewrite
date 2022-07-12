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
package org.openrewrite.cobol.internal.preprocessor.sub.line.reader;

public class CobolScanner {
    private final String text;
    private int index;

    private StringBuilder sb = new StringBuilder();

    public CobolScanner(String text) {
        this.text = text;
        this.index = 0;
    }

    public boolean hasNextLine() {
        return index < text.length();
    }

    public Line nextLine() {
        if(!(index < text.length())) {
            System.out.println();
        }

        int startIndex = index;

        sb.setLength(0);
        while(index < text.length() && !isNewLine(text.charAt(index))) {
            sb.append(text.charAt(index));
            index++;
        }
        String s = sb.toString();

        sb.setLength(0);
        while(index < text.length() && isNewLine(text.charAt(index))) {
            sb.append(text.charAt(index));
            index++;
        }
        String newLine = sb.toString();

        if(index == startIndex) {
            System.out.println();
        }

        return new Line(s, newLine);
    }

    private boolean isNewLine(char c) {
        return c == '\n' || c == '\r';
    }

    public void close() {
    }
}
