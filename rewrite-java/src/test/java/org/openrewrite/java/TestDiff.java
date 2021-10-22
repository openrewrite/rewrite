/*
 * Copyright 2021 VMware, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 *
 * @author: Fabian Krüger
 */
package org.openrewrite.java;

import org.assertj.core.util.diff.DiffUtils;
import org.assertj.core.util.diff.Patch;

import java.util.List;
import java.util.stream.Collectors;

public class TestDiff {

    public static String of(String actual, String expected) {
        List<String> originalLines = actual.lines().collect(Collectors.toList());
        Patch<String> diff = DiffUtils.diff(originalLines, expected.lines().collect(Collectors.toList()));
        List<String> strings = DiffUtils.generateUnifiedDiff(actual, expected, originalLines, diff, 1000);
        String theDiff = strings.stream().collect(Collectors.joining(System.lineSeparator()));
        String headline = "\n\nHere's the diff between 1. (---) actual and 2. (+++) expected:\n\n";
        return headline + theDiff;
    }
}
