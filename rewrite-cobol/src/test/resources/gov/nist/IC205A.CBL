      *HEADER,COBOL,IC203A,SUBRTN,IC205A                                              
000100 IDENTIFICATION DIVISION.                                         IC2054.2
000200 PROGRAM-ID.                                                      IC2054.2
000300     IC205A.                                                      IC2054.2
000400****************************************************************  IC2054.2
000500*                                                              *  IC2054.2
000600*    VALIDATION FOR:-                                          *  IC2054.2
000700*                                                              *  IC2054.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IC2054.2
000900*                                                              *  IC2054.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IC2054.2
001100*                                                              *  IC2054.2
001200****************************************************************  IC2054.2
001300*                                                              *  IC2054.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  IC2054.2
001500*                                                              *  IC2054.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  IC2054.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  IC2054.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  IC2054.2
001900*                                                              *  IC2054.2
002000****************************************************************  IC2054.2
002100*        THE SUBPROGRAM IC205 TESTS THE USE OF THE CANCEL      *  IC2054.2
002200*    STATEMENT WITHIN A SUBPROGRAM.  THIS SUBPROGRAM IS        *  IC2054.2
002300*    CALLED BY IC203 AND CALLS THE SUBPROGRAMS IC204 AND IC206.*  IC2054.2
002400****************************************************************  IC2054.2
002500 ENVIRONMENT DIVISION.                                            IC2054.2
002600 CONFIGURATION SECTION.                                           IC2054.2
002700 SOURCE-COMPUTER.                                                 IC2054.2
002800     XXXXX082.                                                    IC2054.2
002900 OBJECT-COMPUTER.                                                 IC2054.2
003000     XXXXX083.                                                    IC2054.2
003100 INPUT-OUTPUT SECTION.                                            IC2054.2
003200 FILE-CONTROL.                                                    IC2054.2
003300     SELECT PRINT-FILE ASSIGN TO                                  IC2054.2
003400     XXXXX055.                                                    IC2054.2
003500 DATA DIVISION.                                                   IC2054.2
003600 FILE SECTION.                                                    IC2054.2
003700 FD  PRINT-FILE.                                                  IC2054.2
003800 01  PRINT-REC PICTURE X(120).                                    IC2054.2
003900 01  DUMMY-RECORD PICTURE X(120).                                 IC2054.2
004000 WORKING-STORAGE SECTION.                                         IC2054.2
004100 77  ID1 PICTURE X(6) VALUE "IC204A".                             IC2054.2
004200 77  DN2 PICTURE S9(8) USAGE COMP VALUE ZERO.                     IC2054.2
004300 LINKAGE SECTION.                                                 IC2054.2
004400 01  TABLE-1.                                                     IC2054.2
004500     02  T-DN1 PIC XXX.                                           IC2054.2
004600     02  T-DN2 PIC 99.                                            IC2054.2
004700     02  T-DN3 PIC X(5).                                          IC2054.2
004800 77  DN1 PICTURE S999.                                            IC2054.2
004900 01  TABLE-2.                                                     IC2054.2
005000     02  TV-1 PIC X.                                              IC2054.2
005100     02  TV-2 PIC X.                                              IC2054.2
005200 PROCEDURE DIVISION USING TABLE-1, TABLE-2, DN1.                  IC2054.2
005300 CNCL-TEST-05.                                                    IC2054.2
005400     CALL "IC206A" USING DN2.                                     IC2054.2
005500     CALL "IC206A" USING DN2.                                     IC2054.2
005600     CALL "IC206A" USING DN2.                                     IC2054.2
005700     MOVE "X" TO TV-1.                                            IC2054.2
005800     IF DN2 EQUAL TO 3                                            IC2054.2
005900         MOVE "A" TO TV-1.                                        IC2054.2
006000     CANCEL "IC206A".                                             IC2054.2
006100     MOVE ZERO TO DN2.                                            IC2054.2
006200     CALL "IC206A" USING DN2.                                     IC2054.2
006300     IF DN2 NOT EQUAL TO 1                                        IC2054.2
006400         MOVE "Y" TO TV-2,                                        IC2054.2
006500         GO TO CNCL-TEST-06.                                      IC2054.2
006600     MOVE "B" TO TV-2.                                            IC2054.2
006700 CNCL-TEST-06.                                                    IC2054.2
006800     CANCEL ID1.                                                  IC2054.2
006900 EXIT-IC205.                                                      IC2054.2
007000     EXIT PROGRAM.                                                IC2054.2
      *END-OF,IC205A                                                                  
