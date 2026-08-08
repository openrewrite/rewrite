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
using OpenRewrite.Core;
using OpenRewrite.CSharp.Rpc;

namespace OpenRewrite.Tests.Rpc;

/// <summary>
/// Verifies the marketplace row carries data tables (name, display metadata, and columns), bringing
/// the C# emitter in line with the other ecosystems rather than emitting an empty list.
/// </summary>
public class DataTableDescriptorDtoTest
{
    [Fact]
    public void FromDescriptor_MapsDataTableAndColumns()
    {
        var descriptor = new DataTableDescriptor(
            "my.table", "My Table", "A table.",
            [new ColumnDescriptor("col", "Column", "A column.")]);

        var dto = DataTableDescriptorDto.FromDescriptor(descriptor);

        Assert.Equal("my.table", dto.Name);
        Assert.Equal("My Table", dto.DisplayName);
        Assert.Equal("A table.", dto.Description);

        var column = Assert.Single(dto.Columns);
        Assert.Equal("col", column.Name);
        Assert.Equal("Column", column.DisplayName);
        Assert.Equal("A column.", column.Description);
        // C#'s ColumnDescriptor carries no type; the wire field is present but empty.
        Assert.Equal("", column.Type);
    }
}
