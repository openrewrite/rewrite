package com.netflix.java.refactor

@Target(AnnotationTarget.FUNCTION)
annotation class Refactor(val id: String, val description: String = "")