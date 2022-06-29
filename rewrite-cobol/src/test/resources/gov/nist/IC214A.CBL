      *HEADER,COBOL,IC213A,SUBRTN,IC214A                                              
000100 IDENTIFICATION DIVISION.                                         IC2144.2
000200 PROGRAM-ID.                                                      IC2144.2
000300     IC214A.                                                      IC2144.2
000400****************************************************************  IC2144.2
000500*                                                              *  IC2144.2
000600*    VALIDATION FOR:-                                          *  IC2144.2
000700*                                                              *  IC2144.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IC2144.2
000900*                                                              *  IC2144.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IC2144.2
001100*                                                              *  IC2144.2
001200****************************************************************  IC2144.2
001300*                                                              *  IC2144.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  IC2144.2
001500*                                                              *  IC2144.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  IC2144.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  IC2144.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  IC2144.2
001900*                                                              *  IC2144.2
002000****************************************************************  IC2144.2
002100*    THIS IS SUBPROGRAM IC214.                                    IC2144.2
002200****************************************************************  IC2144.2
002300 ENVIRONMENT DIVISION.                                            IC2144.2
002400 CONFIGURATION SECTION.                                           IC2144.2
002500 SOURCE-COMPUTER.                                                 IC2144.2
002600     XXXXX082.                                                    IC2144.2
002700 OBJECT-COMPUTER.                                                 IC2144.2
002800     XXXXX083.                                                    IC2144.2
002900 DATA DIVISION.                                                   IC2144.2
003000 LINKAGE SECTION.                                                 IC2144.2
003100 01  DN1                         PICTURE S9.                      IC2144.2
003200 PROCEDURE DIVISION USING DN1.                                    IC2144.2
003300 CALL-TEST-1.                                                     IC2144.2
003400     MOVE 1 TO DN1.                                               IC2144.2
003500 IC214-EXIT.                                                      IC2144.2
003600     EXIT PROGRAM.                                                IC2144.2
      *END-OF,IC214A                                                                  
