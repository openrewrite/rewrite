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

import org.openrewrite.cobol.internal.preprocessor.sub.CobolLine;

import java.util.ArrayList;
import java.util.List;

public class StringBufferWithOriginalPositions {
    //StringWithOriginalPositions originalCodeWithPositions;
    String originalCode;
    List<CobolLine> lines;

    StringBuffer sb;
    List<Integer> originalPositions;
    int currentPositionInOriginalFile;

    public StringBufferWithOriginalPositions(String originalCode, List<CobolLine> lines) {
        //this.originalCodeWithPositions = null;
        this.originalCode = originalCode;
        this.lines = lines;

        this.sb = new StringBuffer();
        this.originalPositions = new ArrayList<>();
        this.currentPositionInOriginalFile = 0;
    }

    public StringBufferWithOriginalPositions(StringWithOriginalPositions originalCodeWithPositions) {
        //this.originalCodeWithPositions = originalCodeWithPositions;
        // This original text is the previous string's preprocessed text
        this.originalCode = originalCodeWithPositions.preprocessedText;

        this.sb = new StringBuffer();
        this.originalPositions = new ArrayList<>();
        this.currentPositionInOriginalFile = 0;
    }

    public void append(String s) {
        sb.append(s);
        for (int i = 0; i < s.length(); i++) {
            originalPositions.add(currentPositionInOriginalFile);
            currentPositionInOriginalFile++;
        }
    }

    public StringWithOriginalPositions toStringWithMarkers(int lineNumber) {
        return new StringWithOriginalPositions(lines, sb.toString(), originalCode, toIntArray(originalPositions));
    }

    private int[] toIntArray(List<Integer> list) {
        int size = list.size();
        int[] result = new int[size];
        for(int i=0; i<size; i++) {
            result[i] = list.get(i);
        }
        return result;
    }

    public void skip(String s) {
        skip(s.length());
    }

    public void skip(int i) {
        currentPositionInOriginalFile += i;
    }
}


