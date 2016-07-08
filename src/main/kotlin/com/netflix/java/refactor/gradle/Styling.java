package com.netflix.java.refactor.gradle;

import org.gradle.internal.logging.text.StyledTextOutput;

class Styling {
	static StyledTextOutput.Style Bold = StyledTextOutput.Style.UserInput;
    static StyledTextOutput.Style Green = StyledTextOutput.Style.Identifier;
    static StyledTextOutput.Style Yellow = StyledTextOutput.Style.Description;
    static StyledTextOutput.Style Red = StyledTextOutput.Style.Failure;
}
