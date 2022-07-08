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

import java.util.ArrayList;
import java.util.List;

public class StringBufferWithOriginalPositions {

	StringWithOriginalPositions originalCodeWithPositions;
	String originalCode;

	StringBuffer sb;
	List<Integer> originalPositions;
	int currentPositionInOriginalFile;
	
	public StringBufferWithOriginalPositions(String originalCode)
	{
		this.originalCodeWithPositions = null;
		this.originalCode = originalCode;

		this.sb = new StringBuffer();
		this.originalPositions = new ArrayList<>();
		this.currentPositionInOriginalFile = 0;
	}
	
	public StringBufferWithOriginalPositions(StringWithOriginalPositions originalCodeWithPositions)
	{
		this.originalCodeWithPositions = originalCodeWithPositions;
		// This original text is the previous string's preprocessed text
		this.originalCode = originalCodeWithPositions.preprocessedText;

		this.sb = new StringBuffer();
		this.originalPositions = new ArrayList<>();
		this.currentPositionInOriginalFile = 0;
	}
	
	public void append(String s) {
		sb.append(s);
		for(int i=0; i<s.length(); i++) {
			if(currentPositionInOriginalFile == 51) {
				System.out.println();
			}
			originalPositions.add(currentPositionInOriginalFile);
			currentPositionInOriginalFile++;
		}
	}

	public StringWithOriginalPositions toStringWithMarkers() {
		return new StringWithOriginalPositions(sb.toString(), originalCode, originalPositions.stream().mapToInt(Integer::intValue).toArray());
	}

	public void skip(String s) {
		skip(s.length());
	}

	public void skip(int i) {
		currentPositionInOriginalFile += i;
	}
}


