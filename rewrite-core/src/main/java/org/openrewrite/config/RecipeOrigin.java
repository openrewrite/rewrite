package org.openrewrite.config;

import lombok.Value;

import java.net.URI;

@Value
public class RecipeOrigin {
    URI sourceLocation;
    License license;
}
