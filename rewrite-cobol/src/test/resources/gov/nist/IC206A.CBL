      *HEADER,COBOL,IC203A,SUBRTN,IC206A                                              
000100 IDENTIFICATION DIVISION.                                         IC2064.2
000200 PROGRAM-ID.                                                      IC2064.2
000300     IC206A.                                                      IC2064.2
000400****************************************************************  IC2064.2
000500*                                                              *  IC2064.2
000600*    VALIDATION FOR:-                                          *  IC2064.2
000700*                                                              *  IC2064.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IC2064.2
000900*                                                              *  IC2064.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IC2064.2
001100*                                                              *  IC2064.2
001200****************************************************************  IC2064.2
001300*                                                              *  IC2064.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  IC2064.2
001500*                                                              *  IC2064.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  IC2064.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  IC2064.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  IC2064.2
001900*                                                              *  IC2064.2
002000****************************************************************  IC2064.2
002100*        THE SUBPROGRAM IC206 IS CALLED BY THE SUBPROGRAM         IC2064.2
002200*    IC205.  THE SUBPROGRAM IS THEN CANCELED AND CALLED           IC2064.2
002300*    AGAIN.  THE PROGRAM IC205 CHECKS IF IC206 WAS IN ITS         IC2064.2
002400*    INITIAL STATE ON THE FIRST CALL AFTER THE PROGRAM WAS        IC2064.2
002500*    CANCELED.                                                    IC2064.2
002600*        THE LINKAGE PARAMETER DN1 CONTAINS THE NUMBER OF         IC2064.2
002700*    TIMES IC206 HAS BEEN CALLED SINCE INITIALIZATION WHEN        IC2064.2
002800*    CONTROL IS RETURNED TO THE CALLING PROGRAM.                  IC2064.2
002900****************************************************************  IC2064.2
003000 ENVIRONMENT DIVISION.                                            IC2064.2
003100 CONFIGURATION SECTION.                                           IC2064.2
003200 SOURCE-COMPUTER.                                                 IC2064.2
003300     XXXXX082.                                                    IC2064.2
003400 OBJECT-COMPUTER.                                                 IC2064.2
003500     XXXXX083.                                                    IC2064.2
003600 INPUT-OUTPUT SECTION.                                            IC2064.2
003700 FILE-CONTROL.                                                    IC2064.2
003800     SELECT PRINT-FILE ASSIGN TO                                  IC2064.2
003900     XXXXX055.                                                    IC2064.2
004000 DATA DIVISION.                                                   IC2064.2
004100 FILE SECTION.                                                    IC2064.2
004200 FD  PRINT-FILE.                                                  IC2064.2
004300 01  PRINT-REC PICTURE X(120).                                    IC2064.2
004400 01  DUMMY-RECORD PICTURE X(120).                                 IC2064.2
004500 WORKING-STORAGE SECTION.                                         IC2064.2
004600 77  WS1 PICTURE S9(8) USAGE COMPUTATIONAL                        IC2064.2
004700         VALUE ZERO.                                              IC2064.2
004800 LINKAGE SECTION.                                                 IC2064.2
004900 01  DN1 PICTURE S9(8)  USAGE COMPUTATIONAL.                      IC2064.2
005000 PROCEDURE DIVISION USING DN1.                                    IC2064.2
005100 SECT-IC206-0001 SECTION.                                         IC2064.2
005200 TEST-PARAGRAPH.                                                  IC2064.2
005300     ADD 1 TO WS1.                                                IC2064.2
005400     MOVE WS1 TO DN1.                                             IC2064.2
005500 EXIT-IC206.                                                      IC2064.2
005600     EXIT PROGRAM.                                                IC2064.2
      *END-OF,IC206A                                                                  
