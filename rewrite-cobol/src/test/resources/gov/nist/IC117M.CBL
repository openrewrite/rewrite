      *HEADER,COBOL,IC116M,SUBRTN,IC117M                                              
000100 IDENTIFICATION DIVISION.                                         IC1174.2
000200 PROGRAM-ID.                                                      IC1174.2
000300     IC117M.                                                      IC1174.2
000400****************************************************************  IC1174.2
000500*                                                              *  IC1174.2
000600*    VALIDATION FOR:-                                          *  IC1174.2
000700*                                                              *  IC1174.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IC1174.2
000900*                                                              *  IC1174.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IC1174.2
001100*                                                              *  IC1174.2
001200****************************************************************  IC1174.2
001300*                                                              *  IC1174.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  IC1174.2
001500*                                                              *  IC1174.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  IC1174.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  IC1174.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  IC1174.2
001900*                                                              *  IC1174.2
002000****************************************************************  IC1174.2
002100*                                                                 IC1174.2
002200*        THE SUBPROGRAM IC117 IS CALLED BY THE MAIN PROGRAM IC116.IC1174.2
002300*    THE SUBPROGRAM IC117 DOES NOT CONTAIN A LINKAGE SECTION OR   IC1174.2
002400*    AN USING PHRASE IN THE PROCEDURE DIVISION HEADER.  DISPLAY   IC1174.2
002500*    STATEMENTS ARE USED TO VERIFY THE PROGRAM EXECUTION SEQUENCE.IC1174.2
002600*    THE SUBPROGRAM IC118 IS CALLED BY THE SUBPROGRAM IC117 AND   IC1174.2
002700*    THE CALL STATEMENT IN THE SUBPROGRAM ALSO DOES NOT HAVE AN   IC1174.2
002800*    USING PHRASE.                                                IC1174.2
002900*                                                                 IC1174.2
003000*    REFERENCE - AMERICAN NATIONAL STANDARD PROGRAMMING LANGUAGE  IC1174.2
003100*                    COBOL, X3.23-1974                            IC1174.2
003200*                SECTION XII, INTER-PROGRAM COMMUNICATION MODULE. IC1174.2
003300*                                                                 IC1174.2
003400******************************************************************IC1174.2
003500 ENVIRONMENT DIVISION.                                            IC1174.2
003600 CONFIGURATION SECTION.                                           IC1174.2
003700 SOURCE-COMPUTER.                                                 IC1174.2
003800     XXXXX082.                                                    IC1174.2
003900 OBJECT-COMPUTER.                                                 IC1174.2
004000     XXXXX083.                                                    IC1174.2
004100 DATA DIVISION.                                                   IC1174.2
004200 WORKING-STORAGE SECTION.                                         IC1174.2
004300 01  IC117-TEMP1         PICTURE 9.                               IC1174.2
004400 01  ONE                 PICTURE 9    VALUE 1.                    IC1174.2
004500 01  IC117-TEMP2         PICTURE 9     VALUE 0.                   IC1174.2
004600 PROCEDURE DIVISION.                                              IC1174.2
004700*USNG-TEST-02.                                                    IC1174.2
004800*                                                                 IC1174.2
004900*    THIS TEST VERIFIES THAT A SUBPROGRAM PROCEDURE DIVISION      IC1174.2
005000*    HEADER IS NOT REQUIRED TO HAVE THE OPTIONAL USING PHRASE.    IC1174.2
005100*                                                                 IC1174.2
005200 USNG-VERIFY-02.                                                  IC1174.2
005300     MOVE 1 TO IC117-TEMP1.                                       IC1174.2
005400     ADD ONE TO IC117-TEMP2.                                      IC1174.2
005500*                                                                 IC1174.2
005600*    THE RESULTS OF THE ABOVE STATEMENTS ARE NOT TESTED.          IC1174.2
005700*                                                                 IC1174.2
005800 USNG-DISPLAY-02.                                                 IC1174.2
005900     DISPLAY "  ".                                                IC1174.2
006000     DISPLAY "IC117M CALLED".                                     IC1174.2
006100 USNG-TEST-03.                                                    IC1174.2
006200     CALL "IC118M".                                               IC1174.2
006300*                                                                 IC1174.2
006400*    THIS TEST CONTAINS A CALL STATEMENT WITHOUT THE OPTIONAL     IC1174.2
006500*    USING PHRASE.                                                IC1174.2
006600*    REFERENCE - X3.23-1995, PAGE X-27, 5.2, THE CALL STATEMENT.  IC1174.2
006700*                                                                 IC1174.2
006800 USNG-DISPLAY-03.                                                 IC1174.2
006900     DISPLAY "RETURNED TO IC117M".                                IC1174.2
007000 IC117-EXIT.                                                      IC1174.2
007100     EXIT PROGRAM.                                                IC1174.2
      *END-OF,IC117M                                                                  
