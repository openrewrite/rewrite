      *HEADER,COBOL,IC201A,SUBRTN,IC202A                                              
000100 IDENTIFICATION DIVISION.                                         IC2024.2
000200 PROGRAM-ID.                                                      IC2024.2
000300     IC202A.                                                      IC2024.2
000400****************************************************************  IC2024.2
000500*                                                              *  IC2024.2
000600*    VALIDATION FOR:-                                          *  IC2024.2
000700*                                                              *  IC2024.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IC2024.2
000900*                                                              *  IC2024.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IC2024.2
001100*                                                              *  IC2024.2
001200****************************************************************  IC2024.2
001300*                                                              *  IC2024.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  IC2024.2
001500*                                                              *  IC2024.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  IC2024.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  IC2024.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  IC2024.2
001900*                                                              *  IC2024.2
002000****************************************************************  IC2024.2
002100*        THE SUBPROGRAM IC202 IS CALLED BY THE PROGRAM IC201.  *  IC2024.2
002200*    THE SUBPROGRAM HAS FOUR OPERANDS IN THE USING PHRASE      *  IC2024.2
002300*    OF THE PROCEDURE DIVISION HEADER.                         *  IC2024.2
002400****************************************************************  IC2024.2
002500 ENVIRONMENT DIVISION.                                            IC2024.2
002600 CONFIGURATION SECTION.                                           IC2024.2
002700 SOURCE-COMPUTER.                                                 IC2024.2
002800     XXXXX082.                                                    IC2024.2
002900 OBJECT-COMPUTER.                                                 IC2024.2
003000     XXXXX083.                                                    IC2024.2
003100 INPUT-OUTPUT SECTION.                                            IC2024.2
003200 FILE-CONTROL.                                                    IC2024.2
003300     SELECT PRINT-FILE ASSIGN TO                                  IC2024.2
003400     XXXXX055.                                                    IC2024.2
003500 DATA DIVISION.                                                   IC2024.2
003600 FILE SECTION.                                                    IC2024.2
003700 FD  PRINT-FILE.                                                  IC2024.2
003800 01  PRINT-REC PICTURE X(120).                                    IC2024.2
003900 01  DUMMY-RECORD PICTURE X(120).                                 IC2024.2
004000 WORKING-STORAGE SECTION.                                         IC2024.2
004100 77  WS1 PICTURE S999.                                            IC2024.2
004200 77  WS2 PICTURE S999                                             IC2024.2
004300         USAGE COMPUTATIONAL, VALUE ZERO.                         IC2024.2
004400 LINKAGE SECTION.                                                 IC2024.2
004500 77  DN1 PICTURE S99.                                             IC2024.2
004600 77  DN2 PICTURE S99 USAGE COMPUTATIONAL.                         IC2024.2
004700 77  DN3 PICTURE S99.                                             IC2024.2
004800 77  DN4 PICTURE S99 USAGE COMPUTATIONAL.                         IC2024.2
004900 PROCEDURE DIVISION USING DN1, DN2, DN3, DN4.                     IC2024.2
005000 SECT-IC202-0001 SECTION.                                         IC2024.2
005100 CALL-TEST-001.                                                   IC2024.2
005200     MOVE DN1 TO WS1.                                             IC2024.2
005300     ADD 1 TO WS1.                                                IC2024.2
005400     ADD 1 TO WS2.                                                IC2024.2
005500     MOVE WS1 TO DN3.                                             IC2024.2
005600     MOVE WS2 TO DN4.                                             IC2024.2
005700 CALL-EXIT-001.                                                   IC2024.2
005800     EXIT PROGRAM.                                                IC2024.2
      *END-OF,IC202A                                                                  
