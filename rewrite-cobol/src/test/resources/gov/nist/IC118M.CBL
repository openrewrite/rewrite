      *HEADER,COBOL,IC116M,SUBRTN,IC118M                                              
000100 IDENTIFICATION DIVISION.                                         IC1184.2
000200 PROGRAM-ID.                                                      IC1184.2
000300     IC118M.                                                      IC1184.2
000400****************************************************************  IC1184.2
000500*                                                              *  IC1184.2
000600*    VALIDATION FOR:-                                          *  IC1184.2
000700*                                                              *  IC1184.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IC1184.2
000900*                                                              *  IC1184.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IC1184.2
001100*                                                              *  IC1184.2
001200****************************************************************  IC1184.2
001300*                                                              *  IC1184.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  IC1184.2
001500*                                                              *  IC1184.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  IC1184.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  IC1184.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  IC1184.2
001900*                                                              *  IC1184.2
002000****************************************************************  IC1184.2
002100*                                                                 IC1184.2
002200*        THE SUBPROGRAM IC118 IS CALLED BY THE SUBPROGRAM IC117.  IC1184.2
002300*    THE SUBPROGRAM IC118 DOES NOT CONTAIN A LINKAGE SECTION OR   IC1184.2
002400*    AN USING PHRASE IN THE PROCEDURE DIVISION HEADER.  A DISPLAY IC1184.2
002500*    STATEMENT IS EXECUTED TO VERIFY THAT THIS SUBPROGRAM WAS     IC1184.2
002600*    EXECUTED.                                                    IC1184.2
002700*                                                                 IC1184.2
002800******************************************************************IC1184.2
002900 ENVIRONMENT DIVISION.                                            IC1184.2
003000 CONFIGURATION SECTION.                                           IC1184.2
003100 SOURCE-COMPUTER.                                                 IC1184.2
003200     XXXXX082.                                                    IC1184.2
003300 OBJECT-COMPUTER.                                                 IC1184.2
003400     XXXXX083.                                                    IC1184.2
003500 DATA DIVISION.                                                   IC1184.2
003600 WORKING-STORAGE SECTION.                                         IC1184.2
003700 01  IC118-TEMP1         PICTURE 9.                               IC1184.2
003800 01  TWO                 PICTURE 9   VALUE 2.                     IC1184.2
003900 01  IC118-TEMP2         PICTURE 99   VALUE 97.                   IC1184.2
004000 PROCEDURE DIVISION.                                              IC1184.2
004100*USNG-TEST-04.                                                    IC1184.2
004200*                                                                 IC1184.2
004300*    THIS TEST VERIFIES THAT A SUBPROGRAM PROCEDURE DIVISION      IC1184.2
004400*    HEADER IS NOT REQUIRED TO HAVE THE OPTIONAL USING PHRASE.    IC1184.2
004500*                                                                 IC1184.2
004600 USNG-VERIFY-04.                                                  IC1184.2
004700     MOVE 2 TO IC118-TEMP1.                                       IC1184.2
004800     ADD TWO TO IC118-TEMP2.                                      IC1184.2
004900*                                                                 IC1184.2
005000*    THE RESULTS OF THE ABOVE STATEMENTS ARE NOT TESTED.          IC1184.2
005100*                                                                 IC1184.2
005200 USNG-DISPLAY-04.                                                 IC1184.2
005300     DISPLAY "IC118M CALLED".                                     IC1184.2
005400 IC118-EXIT.                                                      IC1184.2
005500     EXIT PROGRAM.                                                IC1184.2
      *END-OF,IC118M                                                                  
