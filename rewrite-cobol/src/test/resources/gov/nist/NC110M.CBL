      *HEADER,COBOL,NC110M                                                            
000100 IDENTIFICATION DIVISION.                                         NC1104.2
000200 PROGRAM-ID.                                                      NC1104.2
000300     NC110M.                                                      NC1104.2
000400****************************************************************  NC1104.2
000500*                                                              *  NC1104.2
000600*    VALIDATION FOR:-                                          *  NC1104.2
000700*                                                              *  NC1104.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1104.2
000900*                                                              *  NC1104.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC1104.2
001100*                                                              *  NC1104.2
001200****************************************************************  NC1104.2
001300*                                                              *  NC1104.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  NC1104.2
001500*                                                              *  NC1104.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  NC1104.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  NC1104.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  NC1104.2
001900*                                                              *  NC1104.2
002000****************************************************************  NC1104.2
002100*                                                                 NC1104.2
002200*    THE PROCEDURE DIVISION OF NC110M CONSISTS ENTIRELY OF        NC1104.2
002300*    PARAGRAPH NAMES AND "DISPLAY" LITERAL STATEMENTS.            NC1104.2
002400*                                                                 NC1104.2
002500 ENVIRONMENT DIVISION.                                            NC1104.2
002600 CONFIGURATION SECTION.                                           NC1104.2
002700 SOURCE-COMPUTER.                                                 NC1104.2
002800     XXXXX082.                                                    NC1104.2
002900 OBJECT-COMPUTER.                                                 NC1104.2
003000     XXXXX083.                                                    NC1104.2
003100 DATA DIVISION.                                                   NC1104.2
003200 PROCEDURE    DIVISION.                                           NC1104.2
003300 HEADER-PRINT.                                                    NC1104.2
003400     DISPLAY " ".                                                 NC1104.2
003500     DISPLAY                                                      NC1104.2
003600     " FOR OFFICIAL USE ONLY                                    ".NC1104.2
003700     DISPLAY                                                      NC1104.2
003800     "     OFFICIAL COBOL COMPILER TEST SYSTEM.     ".            NC1104.2
003900     DISPLAY                                                      NC1104.2
004000     "                TEST RESULTS SET- NC110M                  ".NC1104.2
004100     DISPLAY                                                      NC1104.2
004200     "                                                          ".NC1104.2
004300     DISPLAY                                                      NC1104.2
004400     " FOR OFFICIAL USE ONLY "                                    NC1104.2
004500     "                                                         ". NC1104.2
004600     DISPLAY                                                      NC1104.2
004700     "COPYRIGHT  1985".                                           NC1104.2
004800     DISPLAY                                                      NC1104.2
004900     "                                                          ".NC1104.2
005000     DISPLAY                                                      NC1104.2
005100     " FEATURE     RESULTS AND                                  ".NC1104.2
005200     DISPLAY                                                      NC1104.2
005300     " TESTED      REMARKS                                      ".NC1104.2
005400     DISPLAY                                                      NC1104.2
005500     "                                                          ".NC1104.2
005600     DISPLAY                                                      NC1104.2
005700     " ---------------------------------------------------------".NC1104.2
005800 GO-TEST.                                                         NC1104.2
005900     DISPLAY                                                      NC1104.2
006000     " GO TO       THIS TEST PASSES UNLESS FAIL APPEARS BELOW.  ".NC1104.2
006100     GO       TO PERFORM-TEST.                                    NC1104.2
006200 GO-FAIL.                                                         NC1104.2
006300     DISPLAY                                                      NC1104.2
006400     "             FAIL".                                         NC1104.2
006500 PERFORM-TEST.                                                    NC1104.2
006600     DISPLAY                                                      NC1104.2
006700     " PERFORM     THIS TEST FAILS UNLESS PASS APPEARS BELOW.   ".NC1104.2
006800     PERFORM  PASS.                                               NC1104.2
006900 ENDER-PRINT.                                                     NC1104.2
007000     DISPLAY                                                      NC1104.2
007100     " ---------------------------------------------------------".NC1104.2
007200     DISPLAY                                                      NC1104.2
007300     SPACE.                                                       NC1104.2
007400     DISPLAY                                                      NC1104.2
007500     "                  END OF TEST - NC110M                    ".NC1104.2
007600     DISPLAY                                                      NC1104.2
007700     "                  CHECK FOR ERRORS                        ".NC1104.2
007800     DISPLAY                                                      NC1104.2
007900     "                                                          ".NC1104.2
008000     DISPLAY                                                      NC1104.2
008100     " FOR OFFICIAL USE ONLY "                                    NC1104.2
008200     DISPLAY                                                      NC1104.2
008300     "COPYRIGHT  1985".                                           NC1104.2
008400     DISPLAY                                                      NC1104.2
008500     "       NTIS DISTRIBUTION COBOL 1985               ".        NC1104.2
008600     STOP RUN.                                                    NC1104.2
008700 PASS.                                                            NC1104.2
008800     DISPLAY                                                      NC1104.2
008900     "             PASS".                                         NC1104.2
      *END-OF,NC110M                                                                  
