package com.netflix.java.refactor.gradle

import org.gradle.logging.StyledTextOutput

object Styling {
	val Bold = StyledTextOutput.Style.UserInput
    val Green = StyledTextOutput.Style.Identifier
    val Yellow = StyledTextOutput.Style.Description
    val Red = StyledTextOutput.Style.Failure
}
