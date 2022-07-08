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


	public StringWithOriginalPositions(String text, String originalCode, int[] originalPositions) {
		assert text.length() == originalPositions.length;
		this.preprocessedText = text;
		this.originalText = originalCode;
		this.originalPositions = originalPositions;
	}

	// Scaffolding, to be removed.
	public StringWithOriginalPositions(StringWithOriginalPositions code, String expandedText) {
		this.preprocessedText = expandedText;
		this.originalText = code.originalText;
		this.originalPositions = code.originalPositions; // XXX
//		this.originalPositions = new int[text.length()];
//		for(int i=0; i<originalPositions.length; i++) {
//			originalPositions[i] = i;
//		}
	}

	public String getPreprocessedText(int start, int stop) {
		return preprocessedText.substring(start, stop+1);
	}

	public String getOriginalText(int start, int stop) {
		return originalText.substring(originalPositions[start], originalPositions[stop]+1);
	}
}
