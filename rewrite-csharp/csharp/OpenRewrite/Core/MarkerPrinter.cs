/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
namespace OpenRewrite.Core;

/// <summary>
/// Controls how markers are printed in output. Called at three points during printing:
/// before prefix, before syntax, and after syntax.
/// </summary>
public interface IMarkerPrinter
{
    string BeforePrefix(Marker marker, Cursor cursor, Func<string, string> commentWrapper) => "";

    string BeforeSyntax(Marker marker, Cursor cursor, Func<string, string> commentWrapper) => "";

    string AfterSyntax(Marker marker, Cursor cursor, Func<string, string> commentWrapper) => "";
}

public static class MarkerPrinter
{
    /// <summary>
    /// Default marker printer that prints SearchResult and Markup markers before syntax.
    /// </summary>
    public static readonly IMarkerPrinter Default = new DefaultMarkerPrinter();

    /// <summary>
    /// Only prints SearchResult markers, ignoring Markup and other markers.
    /// </summary>
    public static readonly IMarkerPrinter SearchMarkersOnly = new SearchMarkersOnlyPrinter();

    /// <summary>
    /// Wraps SearchResult and Markup markers with fenced {{id}} delimiters.
    /// </summary>
    public static readonly IMarkerPrinter Fenced = new FencedMarkerPrinter();

    /// <summary>
    /// Suppresses all marker output.
    /// </summary>
    public static readonly IMarkerPrinter Sanitized = new SanitizedMarkerPrinter();

    private class DefaultMarkerPrinter : IMarkerPrinter
    {
        public string BeforeSyntax(Marker marker, Cursor cursor, Func<string, string> commentWrapper)
        {
            return marker switch
            {
                SearchResult sr => commentWrapper(sr.Description == null ? "" : $"({sr.Description})"),
                Markup m => commentWrapper(m.Detail != null ? $"({m.Message}: {m.Detail})" : $"({m.Message})"),
                _ => ""
            };
        }
    }

    private class SearchMarkersOnlyPrinter : IMarkerPrinter
    {
        public string BeforeSyntax(Marker marker, Cursor cursor, Func<string, string> commentWrapper)
        {
            return marker is SearchResult sr
                ? commentWrapper(sr.Description == null ? "" : $"({sr.Description})")
                : "";
        }
    }

    private class FencedMarkerPrinter : IMarkerPrinter
    {
        public string BeforeSyntax(Marker marker, Cursor cursor, Func<string, string> commentWrapper)
        {
            return marker is SearchResult or Markup ? $"{{{{{marker.Id}}}}}" : "";
        }

        public string AfterSyntax(Marker marker, Cursor cursor, Func<string, string> commentWrapper)
        {
            return marker is SearchResult or Markup ? $"{{{{{marker.Id}}}}}" : "";
        }
    }

    private class SanitizedMarkerPrinter : IMarkerPrinter
    {
    }
}
