package org.openrewrite.service;


import lombok.Builder;
import lombok.Value;

/**
 * Tracks the beginning and ending of a given LST element in source code
 */
@Value
@Builder
public class Span {
    int startLine;
    int startColumn;
    int endLine;
    int endColumn;
}
