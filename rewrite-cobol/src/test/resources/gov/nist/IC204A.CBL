      *HEADER,COBOL,IC203A,SUBRTN,IC204A                                              
000100 IDENTIFICATION DIVISION.                                         IC2044.2
000200 PROGRAM-ID.                                                      IC2044.2
000300     IC204A.                                                      IC2044.2
000400****************************************************************  IC2044.2
000500*                                                              *  IC2044.2
000600*    VALIDATION FOR:-                                          *  IC2044.2
000700*                                                              *  IC2044.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IC2044.2
000900*                                                              *  IC2044.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IC2044.2
001100*                                                              *  IC2044.2
001200****************************************************************  IC2044.2
001300*                                                              *  IC2044.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  IC2044.2
001500*                                                              *  IC2044.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  IC2044.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  IC2044.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  IC2044.2
001900*                                                              *  IC2044.2
002000****************************************************************  IC2044.2
002100*        THE SUBPROGRAM IC204 HAS TWO VARIABLES IN WORKING-       IC2044.2
002200*    STORAGE WHICH ARE INITIALIZED BY A VALUE STATEMENT.  THE     IC2044.2
002300*    DATA CONTENTS OF THESE VARIABLES ARE MODIFIED DURING         IC2044.2
002400*    EXECUTION OF THE SUBPROGRAM.  INDICATORS ARE SET FOR         IC2044.2
002500*    VARIABLES IN THE LINKAGE SECTION WHICH RELATE HOW MANY       IC2044.2
002600*    TIMES THE SUBPROGRAM HAS BEEN CALLED SINCE IT WAS            IC2044.2
002700*    INITIALIZED, AND WHETHER OR NOT THE SUBPROGRAM IS IN         IC2044.2
002800*    ITS INITIAL STATE.                                           IC2044.2
002900****************************************************************  IC2044.2
003000 ENVIRONMENT DIVISION.                                            IC2044.2
003100 CONFIGURATION SECTION.                                           IC2044.2
003200 SOURCE-COMPUTER.                                                 IC2044.2
003300     XXXXX082.                                                    IC2044.2
003400 OBJECT-COMPUTER.                                                 IC2044.2
003500     XXXXX083.                                                    IC2044.2
003600 INPUT-OUTPUT SECTION.                                            IC2044.2
003700 FILE-CONTROL.                                                    IC2044.2
003800     SELECT PRINT-FILE ASSIGN TO                                  IC2044.2
003900     XXXXX055.                                                    IC2044.2
004000 DATA DIVISION.                                                   IC2044.2
004100 FILE SECTION.                                                    IC2044.2
004200 FD  PRINT-FILE.                                                  IC2044.2
004300 01  PRINT-REC PICTURE X(120).                                    IC2044.2
004400 01  DUMMY-RECORD PICTURE X(120).                                 IC2044.2
004500 WORKING-STORAGE SECTION.                                         IC2044.2
004600 77  WS1  PICTURE 99 VALUE ZERO.                                  IC2044.2
004700 77  WS2 PICTURE X(5)   VALUE "FIRST".                            IC2044.2
004800 LINKAGE SECTION.                                                 IC2044.2
004900 77  SUB-DN1 PICTURE S999.                                        IC2044.2
005000 01  SUB-TABLE-1.                                                 IC2044.2
005100     02  SUB-DN2 PIC XXX.                                         IC2044.2
005200     02  SUB-DN3 PIC 99.                                          IC2044.2
005300     02  SUB-DN4 PIC X(5).                                        IC2044.2
005400 PROCEDURE DIVISION USING SUB-TABLE-1, SUB-DN1.                   IC2044.2
005500 SECT-IC204-0001 SECTION.                                         IC2044.2
005600 CNCL-TEST-01.                                                    IC2044.2
005700     ADD 1 TO WS1.                                                IC2044.2
005800     MOVE WS1 TO SUB-DN1.                                         IC2044.2
005900 CNCL-TEST-02.                                                    IC2044.2
006000     MOVE "NO" TO SUB-DN2.                                        IC2044.2
006100     IF WS2 EQUAL TO "FIRST"                                      IC2044.2
006200         MOVE SPACE TO WS2                                        IC2044.2
006300         MOVE "YES" TO SUB-DN2.                                   IC2044.2
006400 CNCL-TEST-03.                                                    IC2044.2
006500     MOVE SPACE TO SUB-DN4.                                       IC2044.2
006600     IF WS1 EQUAL TO SUB-DN3                                      IC2044.2
006700         MOVE "EQUAL" TO SUB-DN4.                                 IC2044.2
006800 IC204-EXIT.                                                      IC2044.2
006900     EXIT PROGRAM.                                                IC2044.2
007000*        THE PARAMETER SUB-DN3 IS SET IN THE CALLING PROGRAM      IC2044.2
007100*    EQUAL TO THE NUMBER OF TIMES THE SUBPROGRAM HAS BEEN         IC2044.2
007200*    CALLED SINCE BEING INITIALIZED, EITHER BY THE FIRST CALL     IC2044.2
007300*    OR THE FIRST CALL AFTER A CANCEL STATEMENT.                  IC2044.2
      *END-OF,IC204A                                                                  
