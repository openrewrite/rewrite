      *HEADER,COBOL,IC213A,SUBRTN,IC215A                                              
000100 IDENTIFICATION DIVISION.                                         IC2154.2
000200 PROGRAM-ID.                                                      IC2154.2
000300     IC215A.                                                      IC2154.2
000400****************************************************************  IC2154.2
000500*                                                              *  IC2154.2
000600*    VALIDATION FOR:-                                          *  IC2154.2
000700*                                                              *  IC2154.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IC2154.2
000900*                                                              *  IC2154.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IC2154.2
001100*                                                              *  IC2154.2
001200****************************************************************  IC2154.2
001300*                                                              *  IC2154.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  IC2154.2
001500*                                                              *  IC2154.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  IC2154.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  IC2154.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  IC2154.2
001900*                                                              *  IC2154.2
002000****************************************************************  IC2154.2
002100*    THIS IS SUBPROGRAM IC215.                                    IC2154.2
002200****************************************************************  IC2154.2
002300 ENVIRONMENT DIVISION.                                            IC2154.2
002400 CONFIGURATION SECTION.                                           IC2154.2
002500 SOURCE-COMPUTER.                                                 IC2154.2
002600     XXXXX082.                                                    IC2154.2
002700 OBJECT-COMPUTER.                                                 IC2154.2
002800     XXXXX083.                                                    IC2154.2
002900 DATA DIVISION.                                                   IC2154.2
003000 LINKAGE SECTION.                                                 IC2154.2
003100 01  DN2                         PICTURE S9.                      IC2154.2
003200 01  DN3                         PICTURE S9.                      IC2154.2
003300 PROCEDURE DIVISION USING DN2, DN3.                               IC2154.2
003400 CALL-TEST-2.                                                     IC2154.2
003500     MOVE 1 TO DN2.                                               IC2154.2
003600 CANCEL-TEST-1.                                                   IC2154.2
003700     CANCEL "IC214A".                                             IC2154.2
003800     MOVE 1 TO DN3.                                               IC2154.2
003900 IC215-EXIT.                                                      IC2154.2
004000     EXIT PROGRAM.                                                IC2154.2
      *END-OF,IC215A                                                                  
