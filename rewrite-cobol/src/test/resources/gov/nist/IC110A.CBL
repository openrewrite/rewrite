      *HEADER,COBOL,IC108A,SUBRTN,IC110A                                              
000100 IDENTIFICATION DIVISION.                                         IC1104.2
000200 PROGRAM-ID.                                                      IC1104.2
000300     IC110A.                                                      IC1104.2
000400****************************************************************  IC1104.2
000500*                                                              *  IC1104.2
000600*    VALIDATION FOR:-                                          *  IC1104.2
000700*                                                              *  IC1104.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IC1104.2
000900*                                                              *  IC1104.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IC1104.2
001100*                                                              *  IC1104.2
001200****************************************************************  IC1104.2
001300*                                                              *  IC1104.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  IC1104.2
001500*                                                              *  IC1104.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  IC1104.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  IC1104.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  IC1104.2
001900*                                                              *  IC1104.2
002000****************************************************************  IC1104.2
002100*        THE SUBPROGRAM IC110 IS THE SECOND SUBPROGRAM IN A       IC1104.2
002200*    SEQUENCE OF CALLS WHICH START IN THE MAIN PROGRAM IC108.     IC1104.2
002300*    THIS SUBPROGRAM CALLS IC111 WITH OPERANDS IN THE LINKAGE     IC1104.2
002400*    SECTION AND IN THE WORKING-STORAGE SECTION.  THE SUBPROGRAM  IC1104.2
002500*    IC110 IS CALLED BY IC109.                                    IC1104.2
002600 ENVIRONMENT DIVISION.                                            IC1104.2
002700 CONFIGURATION SECTION.                                           IC1104.2
002800 SOURCE-COMPUTER.                                                 IC1104.2
002900     XXXXX082.                                                    IC1104.2
003000 OBJECT-COMPUTER.                                                 IC1104.2
003100     XXXXX083.                                                    IC1104.2
003200 INPUT-OUTPUT SECTION.                                            IC1104.2
003300 FILE-CONTROL.                                                    IC1104.2
003400     SELECT PRINT-FILE ASSIGN TO                                  IC1104.2
003500     XXXXX055.                                                    IC1104.2
003600 DATA DIVISION.                                                   IC1104.2
003700 FILE SECTION.                                                    IC1104.2
003800 FD  PRINT-FILE.                                                  IC1104.2
003900 01  PRINT-REC PICTURE X(120).                                    IC1104.2
004000 01  DUMMY-RECORD PICTURE X(120).                                 IC1104.2
004100 WORKING-STORAGE SECTION.                                         IC1104.2
004200 77  WS2 PICTURE X.                                               IC1104.2
004300 LINKAGE SECTION.                                                 IC1104.2
004400 01  GRP-01.                                                      IC1104.2
004500     02  SUB-CALLED.                                              IC1104.2
004600         03  DN1 PICTURE X(6).                                    IC1104.2
004700         03  DN2 PICTURE X(6).                                    IC1104.2
004800         03  DN3 PICTURE X(6).                                    IC1104.2
004900     02  TIMES-CALLED.                                            IC1104.2
005000         03  DN4 PICTURE S999.                                    IC1104.2
005100         03  DN5 PICTURE S999.                                    IC1104.2
005200         03  DN6 PICTURE S999.                                    IC1104.2
005300     02  SPECIAL-FLAGS.                                           IC1104.2
005400         03  DN7 PICTURE X.                                       IC1104.2
005500         03  DN8 PICTURE X.                                       IC1104.2
005600         03  DN9 PICTURE X.                                       IC1104.2
005700 01  LS1 PICTURE X.                                               IC1104.2
005800 PROCEDURE DIVISION USING LS1 GRP-01.                             IC1104.2
005900 SECT-IC110-0001 SECTION.                                         IC1104.2
006000 PARA-IC110.                                                      IC1104.2
006100     MOVE "IC110A" TO DN2.                                        IC1104.2
006200     MOVE SPACE TO WS2.                                           IC1104.2
006300     CALL "IC111A" USING LS1 GRP-01 WS2.                          IC1104.2
006400     MOVE WS2 TO DN7.                                             IC1104.2
006500     MOVE LS1 TO DN8.                                             IC1104.2
006600     ADD 1 TO DN5.                                                IC1104.2
006700     MOVE "B" TO LS1.                                             IC1104.2
006800 EXIT-IC110.                                                      IC1104.2
006900     EXIT PROGRAM.                                                IC1104.2
      *END-OF,IC110A                                                                  
