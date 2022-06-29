      *HEADER,COBOL,IC101A,SUBRTN,IC102A                                              
000100 IDENTIFICATION DIVISION.                                         IC1024.2
000200 PROGRAM-ID.                                                      IC1024.2
000300     IC102A.                                                      IC1024.2
000400****************************************************************  IC1024.2
000500*                                                              *  IC1024.2
000600*    VALIDATION FOR:-                                          *  IC1024.2
000700*                                                              *  IC1024.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IC1024.2
000900*                                                              *  IC1024.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IC1024.2
001100*                                                              *  IC1024.2
001200****************************************************************  IC1024.2
001300*                                                              *  IC1024.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  IC1024.2
001500*                                                              *  IC1024.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  IC1024.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  IC1024.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  IC1024.2
001900*                                                              *  IC1024.2
002000****************************************************************  IC1024.2
002100*        THIS PROGRAM TESTS THE USE OF THE LINKAGE SECTION        IC1024.2
002200*    AND USING PHRASE IN THE PROCEDURE DIVISION HEADER.           IC1024.2
002300 ENVIRONMENT DIVISION.                                            IC1024.2
002400 CONFIGURATION SECTION.                                           IC1024.2
002500 SOURCE-COMPUTER.                                                 IC1024.2
002600     XXXXX082.                                                    IC1024.2
002700 OBJECT-COMPUTER.                                                 IC1024.2
002800     XXXXX083.                                                    IC1024.2
002900 INPUT-OUTPUT SECTION.                                            IC1024.2
003000 FILE-CONTROL.                                                    IC1024.2
003100     SELECT PRINT-FILE ASSIGN TO                                  IC1024.2
003200     XXXXX055.                                                    IC1024.2
003300 DATA DIVISION.                                                   IC1024.2
003400 FILE SECTION.                                                    IC1024.2
003500 FD  PRINT-FILE.                                                  IC1024.2
003600 01  PRINT-REC PICTURE X(120).                                    IC1024.2
003700 01  DUMMY-RECORD PICTURE X(120).                                 IC1024.2
003800 WORKING-STORAGE SECTION.                                         IC1024.2
003900 77  DN2 PICTURE S9  VALUE ZERO.                                  IC1024.2
004000 LINKAGE SECTION.                                                 IC1024.2
004100 77  DN1 PICTURE S9.                                              IC1024.2
004200 PROCEDURE DIVISION USING DN1.                                    IC1024.2
004300 SECT-IC102-0001 SECTION.                                         IC1024.2
004400 CALL-TEST-001.                                                   IC1024.2
004500     ADD 1 TO DN2.                                                IC1024.2
004600     MOVE DN2 TO DN1.                                             IC1024.2
004700 CALL-EXIT-001.                                                   IC1024.2
004800     EXIT PROGRAM.                                                IC1024.2
      *END-OF,IC102A                                                                  
