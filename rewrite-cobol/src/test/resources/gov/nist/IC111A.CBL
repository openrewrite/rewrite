      *HEADER,COBOL,IC108A,SUBRTN,IC111A                                              
000100 IDENTIFICATION DIVISION.                                         IC1114.2
000200 PROGRAM-ID.                                                      IC1114.2
000300     IC111A.                                                      IC1114.2
000400****************************************************************  IC1114.2
000500*                                                              *  IC1114.2
000600*    VALIDATION FOR:-                                          *  IC1114.2
000700*                                                              *  IC1114.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IC1114.2
000900*                                                              *  IC1114.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IC1114.2
001100*                                                              *  IC1114.2
001200****************************************************************  IC1114.2
001300*                                                              *  IC1114.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  IC1114.2
001500*                                                              *  IC1114.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  IC1114.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  IC1114.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  IC1114.2
001900*                                                              *  IC1114.2
002000****************************************************************  IC1114.2
002100*        THE SUBPROGRAM IC111 IS THE LAST SUBPROGRAM CALLED       IC1114.2
002200*    IN A SEQUENCE OF SUBPROGRAM CALLS WHICH IS STARTED IN        IC1114.2
002300*    MAIN PROGRAM IC108.  THE SUBPROGRAM IC111 IS CALLED BY       IC1114.2
002400*    THE SUBPROGRAM IC110.                                        IC1114.2
002500 ENVIRONMENT DIVISION.                                            IC1114.2
002600 CONFIGURATION SECTION.                                           IC1114.2
002700 SOURCE-COMPUTER.                                                 IC1114.2
002800     XXXXX082.                                                    IC1114.2
002900 OBJECT-COMPUTER.                                                 IC1114.2
003000     XXXXX083.                                                    IC1114.2
003100 DATA DIVISION.                                                   IC1114.2
003200 LINKAGE SECTION.                                                 IC1114.2
003300 77  LS1 PICTURE X.                                               IC1114.2
003400 77  LS2 PICTURE X.                                               IC1114.2
003500 01  GRP-01.                                                      IC1114.2
003600     02  SUB-CALLED.                                              IC1114.2
003700         03  DN1 PICTURE X(6).                                    IC1114.2
003800         03  DN2 PICTURE X(6).                                    IC1114.2
003900         03  DN3 PICTURE X(6).                                    IC1114.2
004000     02  TIMES-CALLED.                                            IC1114.2
004100         03  DN4 PICTURE S999.                                    IC1114.2
004200         03  DN5 PICTURE S999.                                    IC1114.2
004300         03  DN6 PICTURE S999.                                    IC1114.2
004400     02  SPECIAL-FLAGS.                                           IC1114.2
004500         03  DN7 PICTURE X.                                       IC1114.2
004600         03  DN8 PICTURE X.                                       IC1114.2
004700         03  DN9 PICTURE X.                                       IC1114.2
004800 PROCEDURE DIVISION USING LS1 GRP-01 LS2.                         IC1114.2
004900 SECT-IC111-0001 SECTION.                                         IC1114.2
005000 PARA-IC111.                                                      IC1114.2
005100     MOVE "IC111A" TO DN3.                                        IC1114.2
005200     ADD 1 TO DN6.                                                IC1114.2
005300     MOVE "A" TO LS2.                                             IC1114.2
005400     MOVE "A" TO LS1.                                             IC1114.2
005500 EXIT-IC111.                                                      IC1114.2
005600     EXIT PROGRAM.                                                IC1114.2
      *END-OF,IC111A                                                                  
