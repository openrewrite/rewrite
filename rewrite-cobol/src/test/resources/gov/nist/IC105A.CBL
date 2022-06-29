      *HEADER,COBOL,IC103A,SUBRTN,IC105A                                              
000100 IDENTIFICATION DIVISION.                                         IC1054.2
000200 PROGRAM-ID.                                                      IC1054.2
000300     IC105A.                                                      IC1054.2
000400****************************************************************  IC1054.2
000500*                                                              *  IC1054.2
000600*    VALIDATION FOR:-                                          *  IC1054.2
000700*                                                              *  IC1054.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IC1054.2
000900*                                                              *  IC1054.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IC1054.2
001100*                                                              *  IC1054.2
001200****************************************************************  IC1054.2
001300*                                                              *  IC1054.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  IC1054.2
001500*                                                              *  IC1054.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  IC1054.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  IC1054.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  IC1054.2
001900*                                                              *  IC1054.2
002000****************************************************************  IC1054.2
002100*        THE SUBPROGRAM IC105 HAS TWO OPERANDS IN THE             IC1054.2
002200*    PROCEDURE DIVISION HEADER AND THE ROUTINE CONTAINS           IC1054.2
002300*    FOUR EXIT PROGRAM STATEMENTS.  THE CALLING PROGRAM           IC1054.2
002400*    IS IC103.                                                    IC1054.2
002500 ENVIRONMENT DIVISION.                                            IC1054.2
002600 CONFIGURATION SECTION.                                           IC1054.2
002700 SOURCE-COMPUTER.                                                 IC1054.2
002800     XXXXX082.                                                    IC1054.2
002900 OBJECT-COMPUTER.                                                 IC1054.2
003000     XXXXX083.                                                    IC1054.2
003100 DATA DIVISION.                                                   IC1054.2
003200 LINKAGE SECTION.                                                 IC1054.2
003300 77  DN1 PICTURE 999.                                             IC1054.2
003400 77  DN2 PICTURE S99 COMPUTATIONAL.                               IC1054.2
003500 PROCEDURE DIVISION  USING DN1 DN2.                               IC1054.2
003600*        THIS SUBPROGRAM CONTANS FOUR EXIT PROGRAM STATEMENTS.    IC1054.2
003700*    REFERENCE  X3.23-1974, SECTION XII, 3.4.                     IC1054.2
003800 SECT-IC105-0001 SECTION.                                         IC1054.2
003900 EXIT-TEST-001.                                                   IC1054.2
004000     IF DN1 IS NOT EQUAL TO 1                                     IC1054.2
004100          GO TO EXIT-TEST-002.                                    IC1054.2
004200     MOVE 1 TO DN2.                                               IC1054.2
004300 EXIT-STATEMENT-001.                                              IC1054.2
004400     EXIT PROGRAM.                                                IC1054.2
004500 EXIT-TEST-002.                                                   IC1054.2
004600     IF DN1 IS NOT EQUAL TO 2                                     IC1054.2
004700          GO TO EXIT-TEST-003.                                    IC1054.2
004800     MOVE 2 TO DN2.                                               IC1054.2
004900 EXIT-STATEMENT-002.                                              IC1054.2
005000     EXIT PROGRAM.                                                IC1054.2
005100 EXIT-TEST-003.                                                   IC1054.2
005200     IF DN1 NOT EQUAL TO 3                                        IC1054.2
005300          GO TO EXIT-TEST-004.                                    IC1054.2
005400     MOVE 3 TO DN2.                                               IC1054.2
005500 EXIT-STATEMENT-003.                                              IC1054.2
005600     EXIT PROGRAM.                                                IC1054.2
005700 EXIT-TEST-004.                                                   IC1054.2
005800     MOVE 4 TO DN2.                                               IC1054.2
005900     GO TO EXIT-STATEMENT-004.                                    IC1054.2
006000 EXTRANEOUS-PARAGRAPH.                                            IC1054.2
006100*        THIS PARAGRAPH IS NEVER EXECUTED.                        IC1054.2
006200     MOVE 5 TO DN2.                                               IC1054.2
006300 EXIT-STATEMENT-004.                                              IC1054.2
006400     EXIT PROGRAM.                                                IC1054.2
      *END-OF,IC105A                                                                  
