/*
 * Copyright 2022 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openrewrite.cobol.internal;

import org.openrewrite.cobol.ProgramImpl;
import org.openrewrite.cobol.internal.params.CobolParserParams;
import org.openrewrite.cobol.internal.params.impl.CobolParserParamsImpl;
import org.openrewrite.cobol.internal.preprocessor.CobolPreprocessor;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class Main {

	public static void main(String[] args) throws IOException {
			//new File("src/test/resources/io/proleap/cobol/asg/HelloWorld.cbl");
			//new File("src/test/resources/io/proleap/cobol/preprocessor/fixed/LineContinuation.cbl");

		String DIR;
		File inputFile;
		
		String testName = "copyinws";
		
		switch(testName) {
		case "copyinws":
			DIR = "src/test/resources/io/proleap/cobol/preprocessor/copy/copyinws";
			inputFile = new File(DIR + "/SM201A.CBL");
			break;
		case "CobolWorld":
			DIR = "src/test/resources/io/proleap/cobol/preprocessor/copy/cobolword/variable";
			inputFile = new File(DIR + "/CopyCblWord.cbl");
			break;
		case "CopyReplace":
			DIR = "src/test/resources/io/proleap/cobol/preprocessor/copy/copyreplace/variable";
			inputFile = new File(DIR + "/CopyReplace.cbl");;
			break;
		default:
			return;
		}
		
		CobolPreprocessor.CobolSourceFormatEnum format = CobolPreprocessor.CobolSourceFormatEnum.FIXED;

		CobolParserRunnerImpl parser = new CobolParserRunnerImpl();
		CobolParserParams params = new CobolParserParamsImpl();
		
		params.setFormat(format);
		final File copyBooksDirectory = new File(DIR + "/copybooks");
		params.setCopyBookDirectories(Arrays.asList(copyBooksDirectory));

		//Program program = parser.analyzeFile(inputFile, params);
		final Program program = new ProgramImpl();
		parser.parseFile(inputFile, program, params);

		System.out.println();
		throw new Error("Stop here!");

		// navigate on ASG
//		Printer printer = new Printer(System.out);
//		for(CompilationUnit compilationUnit : program.getCompilationUnits()) {
//			System.out.println("Printing compilation unit " + compilationUnit.getName());
//			printer.print(compilationUnit.getCtx(), parser.tokens.getTokens());
//		}
		/*
		CompilationUnit compilationUnit = program.getCompilationUnit("copycblword");
		ProgramUnit programUnit = compilationUnit.getProgramUnit();
		DataDivision dataDivision = programUnit.getDataDivision();

		DataDescriptionEntry dataDescriptionEntry = dataDivision.getWorkingStorageSection().getDataDescriptionEntry("ITEMS");
		Integer levelNumber = dataDescriptionEntry.getLevelNumber();
		*/
	}
}

