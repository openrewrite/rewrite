      *HEADER,COBOL,IC209A,SUBRTN,IC210A                                              
000100 IDENTIFICATION DIVISION.                                         IC2104.2
000200 PROGRAM-ID.                                                      IC2104.2
000300     IC210A.                                                      IC2104.2
000400****************************************************************  IC2104.2
000500*                                                              *  IC2104.2
000600*    VALIDATION FOR:-                                          *  IC2104.2
000700*                                                              *  IC2104.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IC2104.2
000900*                                                              *  IC2104.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IC2104.2
001100*                                                              *  IC2104.2
001200****************************************************************  IC2104.2
001300*                                                              *  IC2104.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  IC2104.2
001500*                                                              *  IC2104.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  IC2104.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  IC2104.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  IC2104.2
001900*                                                              *  IC2104.2
002000****************************************************************  IC2104.2
002100* 1  THIS IS SUBPROGRAM IC210.                                    IC2104.2
002200****************************************************************  IC2104.2
002300 ENVIRONMENT DIVISION.                                            IC2104.2
002400 CONFIGURATION SECTION.                                           IC2104.2
002500 SOURCE-COMPUTER.                                                 IC2104.2
002600     XXXXX082.                                                    IC2104.2
002700 OBJECT-COMPUTER.                                                 IC2104.2
002800     XXXXX083.                                                    IC2104.2
002900 DATA DIVISION.                                                   IC2104.2
003000 LINKAGE SECTION.                                                 IC2104.2
003100 01  TEST-AREA.                                                   IC2104.2
003200     02  TEST1                   PICTURE X.                       IC2104.2
003300     02  TEST2                   PICTURE X.                       IC2104.2
003400     02  TEST3                   PICTURE X.                       IC2104.2
003500     02  TEST4                   PICTURE X.                       IC2104.2
003600 PROCEDURE DIVISION USING TEST-AREA.                              IC2104.2
003700 CALL-TEST-2.                                                     IC2104.2
003800     MOVE "Y" TO TEST1.                                           IC2104.2
003900     CALL "IC211A" USING TEST-AREA.                               IC2104.2
004000     IF TEST2 = "Y" GO TO CALL-TEST-3.                            IC2104.2
004100     MOVE "N" TO TEST2.                                           IC2104.2
004200 CALL-TEST-3.                                                     IC2104.2
004300     CALL "IC212A" USING TEST-AREA.                               IC2104.2
004400     IF TEST3 = "Y" GO TO CANCEL-TEST-1.                          IC2104.2
004500     MOVE "N" TO TEST3.                                           IC2104.2
004600 CANCEL-TEST-1.                                                   IC2104.2
004700     CANCEL "IC211A".                                             IC2104.2
004800     MOVE "Y" TO TEST4.                                           IC2104.2
004900 IC210-EXIT.                                                      IC2104.2
005000     EXIT PROGRAM.                                                IC2104.2
      *END-OF,IC210A                                                                  
