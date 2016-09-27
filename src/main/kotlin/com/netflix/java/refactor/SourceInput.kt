package com.netflix.java.refactor

import java.nio.file.Path

data class SourceInput<out D>(val path: Path, val datum: D)