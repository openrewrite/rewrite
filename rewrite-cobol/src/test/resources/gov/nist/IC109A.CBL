      *HEADER,COBOL,IC108A,SUBRTN,IC109A                                              
000100 IDENTIFICATION DIVISION.                                         IC1094.2
000200 PROGRAM-ID.                                                      IC1094.2
000300     IC109A.                                                      IC1094.2
000400****************************************************************  IC1094.2
000500*                                                              *  IC1094.2
000600*    VALIDATION FOR:-                                          *  IC1094.2
000700*                                                              *  IC1094.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IC1094.2
000900*                                                              *  IC1094.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IC1094.2
001100*                                                              *  IC1094.2
001200****************************************************************  IC1094.2
001300*                                                              *  IC1094.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  IC1094.2
001500*                                                              *  IC1094.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  IC1094.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  IC1094.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  IC1094.2
001900*                                                              *  IC1094.2
002000****************************************************************  IC1094.2
002100*        THE SUBPROGRAM IC109 IS THE FIRST SUBPROGRAM IN A        IC1094.2
002200*    SEQUENCE OF CALLS WHICH START IN THE MAIN PROGRAM IC108.     IC1094.2
002300*    IC109 CALLS IC110 WITH ONE OPERAND IN THE WORKING-STORAGE    IC1094.2
002400*    SECTION AND ONE OPERAND IN THE LINKAGE SECTION.              IC1094.2
002500 ENVIRONMENT DIVISION.                                            IC1094.2
002600 CONFIGURATION SECTION.                                           IC1094.2
002700 SOURCE-COMPUTER.                                                 IC1094.2
002800     XXXXX082.                                                    IC1094.2
002900 OBJECT-COMPUTER.                                                 IC1094.2
003000     XXXXX083.                                                    IC1094.2
003100 INPUT-OUTPUT SECTION.                                            IC1094.2
003200 FILE-CONTROL.                                                    IC1094.2
003300     SELECT PRINT-FILE ASSIGN TO                                  IC1094.2
003400     XXXXX055.                                                    IC1094.2
003500 DATA DIVISION.                                                   IC1094.2
003600 FILE SECTION.                                                    IC1094.2
003700 FD  PRINT-FILE.                                                  IC1094.2
003800 01  PRINT-REC PICTURE X(120).                                    IC1094.2
003900 01  DUMMY-RECORD PICTURE X(120).                                 IC1094.2
004000 WORKING-STORAGE SECTION.                                         IC1094.2
004100 77  WS1 PICTURE X.                                               IC1094.2
004200 LINKAGE SECTION.                                                 IC1094.2
004300 01  GRP-01.                                                      IC1094.2
004400     02  SUB-CALLED.                                              IC1094.2
004500         03  DN1  PICTURE X(6).                                   IC1094.2
004600         03  DN2  PICTURE X(6).                                   IC1094.2
004700         03  DN3  PICTURE X(6).                                   IC1094.2
004800     02  TIMES-CALLED.                                            IC1094.2
004900         03  DN4  PICTURE S999.                                   IC1094.2
005000         03  DN5  PICTURE S999.                                   IC1094.2
005100         03  DN6  PICTURE S999.                                   IC1094.2
005200     02  SPECIAL-FLAGS.                                           IC1094.2
005300         03  DN7 PICTURE X.                                       IC1094.2
005400         03  DN8 PICTURE X.                                       IC1094.2
005500         03  DN9 PICTURE X.                                       IC1094.2
005600 PROCEDURE DIVISION USING GRP-01.                                 IC1094.2
005700 SECT-IC109-0001 SECTION.                                         IC1094.2
005800 PARA-IC109.                                                      IC1094.2
005900     MOVE "IC109A" TO DN1.                                        IC1094.2
006000     MOVE SPACE TO WS1.                                           IC1094.2
006100     CALL "IC110A" USING WS1 GRP-01.                              IC1094.2
006200     ADD 1 TO DN4.                                                IC1094.2
006300     MOVE WS1 TO DN9.                                             IC1094.2
006400 EXIT-IC109.                                                      IC1094.2
006500     EXIT PROGRAM.                                                IC1094.2
      *END-OF,IC109A                                                                  
