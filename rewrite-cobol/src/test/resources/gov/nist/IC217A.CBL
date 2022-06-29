      *HEADER,COBOL,IC216A,SUBRTN,IC217A                                              
000100 IDENTIFICATION DIVISION.                                         IC2174.2
000200 PROGRAM-ID.                                                      IC2174.2
000300     IC217A.                                                      IC2174.2
000400****************************************************************  IC2174.2
000500*                                                              *  IC2174.2
000600*    VALIDATION FOR:-                                          *  IC2174.2
000700*                                                              *  IC2174.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IC2174.2
000900*                                                              *  IC2174.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IC2174.2
001100*                                                              *  IC2174.2
001200****************************************************************  IC2174.2
001300*                                                              *  IC2174.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  IC2174.2
001500*                                                              *  IC2174.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  IC2174.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  IC2174.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  IC2174.2
001900*                                                              *  IC2174.2
002000****************************************************************  IC2174.2
002100*    THIS IS SUBPROGRAM IC217.                                    IC2174.2
002200****************************************************************  IC2174.2
002300 ENVIRONMENT DIVISION.                                            IC2174.2
002400 CONFIGURATION SECTION.                                           IC2174.2
002500 SOURCE-COMPUTER.                                                 IC2174.2
002600     XXXXX082.                                                    IC2174.2
002700 OBJECT-COMPUTER.                                                 IC2174.2
002800     XXXXX083.                                                    IC2174.2
002900 DATA DIVISION.                                                   IC2174.2
003000 LINKAGE SECTION.                                                 IC2174.2
003100 01  DN1.                                                         IC2174.2
003200     02  DN2                     PICTURE X(5).                    IC2174.2
003300     02  DN3                     REDEFINES DN2   PICTURE 9(5).    IC2174.2
003400 01  DN4.                                                         IC2174.2
003500     02  DN5.                                                     IC2174.2
003600         03  DN6                 PICTURE X(3).                    IC2174.2
003700         03  DN7                 PICTURE X(3).                    IC2174.2
003800         03  DN8                 REDEFINES DN7   PICTURE 9(3).    IC2174.2
003900     02  DN9                     PICTURE XX.                      IC2174.2
004000 PROCEDURE DIVISION USING DN1, DN4.                               IC2174.2
004100 CALL-TEST-1.                                                     IC2174.2
004200     MOVE 12345 TO DN3.                                           IC2174.2
004300 CALL-TEST-2.                                                     IC2174.2
004400     MOVE "YES" TO DN6.                                           IC2174.2
004500     MOVE 987 TO DN8.                                             IC2174.2
004600     MOVE "NO" TO DN9.                                            IC2174.2
004700 IC217-EXIT.                                                      IC2174.2
004800     EXIT PROGRAM.                                                IC2174.2
      *END-OF,IC217A                                                                  
