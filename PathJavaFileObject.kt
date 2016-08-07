package com.netflix.java.refactor.ast

import java.nio.file.Files
import java.nio.file.Path
import javax.tools.JavaFileObject
import javax.tools.SimpleJavaFileObject

class PathJavaFileObject(val path: Path): 
        SimpleJavaFileObject(path.toUri(), JavaFileObject.Kind.SOURCE) {
    
    override fun getCharContent(ignoreEncodingErrors: Boolean) = String(Files.readAllBytes(path))
}
