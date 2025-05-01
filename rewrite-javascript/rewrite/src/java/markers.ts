import {Marker} from "../markers";
import {J} from "./tree";

export const JavaMarkers = {
    Semicolon: "org.openrewrite.java.marker.Semicolon",
    TrailingComma: "org.openrewrite.java.marker.TrailingComma",
    OmitParentheses: "org.openrewrite.java.marker.OmitParentheses",
}

export interface Semicolon extends Marker {
    kind: typeof JavaMarkers.Semicolon
}

export interface TrailingComma extends Marker {
    kind: typeof JavaMarkers.TrailingComma;
    readonly suffix: J.Space;
}

export interface OmitParentheses extends Marker {
    kind: typeof JavaMarkers.OmitParentheses;
}
