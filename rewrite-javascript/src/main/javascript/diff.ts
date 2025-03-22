import {PathLike} from "fs";
import {createTwoFilesPatch} from "diff";

export function diff(before: string, after: string, path: PathLike): string {
    const beforeFileName = path.toString();
    const afterFileName = path.toString();

    // Generate the unified diff
    return createTwoFilesPatch(
        beforeFileName, // fromFile
        afterFileName,  // toFile
        before,         // fromString
        after,          // toString
        "",             // fromFileHeader (optional)
        "",             // toFileHeader (optional)
        {context: 3}  // options (e.g., number of context lines)
    );
}
