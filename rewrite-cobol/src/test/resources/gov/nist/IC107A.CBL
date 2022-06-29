      *HEADER,COBOL,IC106A,SUBRTN,IC107A                                              
000100 IDENTIFICATION DIVISION.                                         IC1074.2
000200 PROGRAM-ID.                                                      IC1074.2
000300     IC107A.                                                      IC1074.2
000400****************************************************************  IC1074.2
000500*                                                              *  IC1074.2
000600*    VALIDATION FOR:-                                          *  IC1074.2
000700*                                                              *  IC1074.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IC1074.2
000900*                                                              *  IC1074.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IC1074.2
001100*                                                              *  IC1074.2
001200****************************************************************  IC1074.2
001300*                                                              *  IC1074.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  IC1074.2
001500*                                                              *  IC1074.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  IC1074.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  IC1074.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  IC1074.2
001900*                                                              *  IC1074.2
002000****************************************************************  IC1074.2
002100*        THE SUBPROGRAM IC107 CONTAINS TABLES AND AN INDEX        IC1074.2
002200*    DATA ITEM WHICH ARE DEFINED IN THE LINKAGE SECTION AND       IC1074.2
002300*    NAMED AS OPERANDS IN THE USING PHRASE OF THE PROCEDURE       IC1074.2
002400*    DIVISION HEADER.  ONE OF THE TABLES HAS AN INDEX DEFINED     IC1074.2
002500*    FOR IT.  THIS INDEX SHOULD BE SEPARATE FROM THE INDEX        IC1074.2
002600*    DEFINED FOR THE SAME TABLE IN THE MAIN PROGRAM IC106,        IC1074.2
002700*    BUT NO SPACE SHOULD BE ALLOCATED FOR THE TABLES DEFINED      IC1074.2
002800*    IN THE LINKAGE SECTION.  THE INDEX DATA ITEM IS SET IN       IC1074.2
002900*    THE MAIN PROGRAM PRIOR TO CALLING IC107, AND IT IS USED      IC1074.2
003000*    IN THIS SUBPROGRAM TO SET AN INDEX FOR REFERENCING THE       IC1074.2
003100*    TABLE IN THE SUBPROGRAM.                                     IC1074.2
003200 ENVIRONMENT DIVISION.                                            IC1074.2
003300 CONFIGURATION SECTION.                                           IC1074.2
003400 SOURCE-COMPUTER.                                                 IC1074.2
003500     XXXXX082.                                                    IC1074.2
003600 OBJECT-COMPUTER.                                                 IC1074.2
003700     XXXXX083.                                                    IC1074.2
003800 INPUT-OUTPUT SECTION.                                            IC1074.2
003900 FILE-CONTROL.                                                    IC1074.2
004000     SELECT PRINT-FILE ASSIGN TO                                  IC1074.2
004100     XXXXX055.                                                    IC1074.2
004200 DATA DIVISION.                                                   IC1074.2
004300 FILE SECTION.                                                    IC1074.2
004400 FD  PRINT-FILE.                                                  IC1074.2
004500 01  PRINT-REC PICTURE X(120).                                    IC1074.2
004600 01  DUMMY-RECORD PICTURE X(120).                                 IC1074.2
004700 WORKING-STORAGE SECTION.                                         IC1074.2
004800 77  IDN3  USAGE IS INDEX.                                        IC1074.2
004900 77  S1  PICTURE 99.                                              IC1074.2
005000 77  AL-CON PICTURE XXX  VALUE "XYZ".                             IC1074.2
005100 LINKAGE SECTION.                                                 IC1074.2
005200 77  IDN2  USAGE IS INDEX.                                        IC1074.2
005300 01  GROUP-1.                                                     IC1074.2
005400     02  DN1 PICTURE X OCCURS 10 TIMES                            IC1074.2
005500                       INDEXED BY IN3.                            IC1074.2
005600 01  GROUP-2.                                                     IC1074.2
005700     02    GROUP-21.                                              IC1074.2
005800         06 DN2 PIC X OCCURS 10 TIMES.                            IC1074.2
005900     02     GROUP-2-1 REDEFINES GROUP-21.                         IC1074.2
006000         03  FILLER  PICTURE X(7).                                IC1074.2
006100         03  DN3     PICTURE XXX.                                 IC1074.2
006200 PROCEDURE DIVISION USING IDN2 GROUP-1 GROUP-2.                   IC1074.2
006300 SECT-IC107-0001 SECTION.                                         IC1074.2
006400 LINK-TEST-02-01.                                                 IC1074.2
006500     SET IN3 TO IDN2.                                             IC1074.2
006600     IF DN1 (IN3) EQUAL TO "F"                                    IC1074.2
006700         MOVE "G" TO DN2 (7).                                     IC1074.2
006800 LINK-TEST-02-02.                                                 IC1074.2
006900     SET IDN3 TO IDN2.                                            IC1074.2
007000     SET IN3 TO IDN3.                                             IC1074.2
007100     IF IN3 EQUAL TO 6                                            IC1074.2
007200         MOVE "F" TO DN2 (6).                                     IC1074.2
007300*        THE TESTS IN LINK-TEST-02 USE THE INDEX DATA ITEM        IC1074.2
007400*    WHICH IS DEFINED IN THE LINKAGE SECTION AND WAS SET IN       IC1074.2
007500*    THE MAIN PROGRAM BEFORE THIS SUBPROGRAM WAS CALLED.          IC1074.2
007600 LINK-TEST-03-01.                                                 IC1074.2
007700     SET IN3 TO 1.                                                IC1074.2
007800     MOVE 1 TO S1.                                                IC1074.2
007900     MOVE DN1 (IN3) TO DN2 (S1).                                  IC1074.2
008000 LINK-TEST-03-02.                                                 IC1074.2
008100     SET IN3 UP BY 1.                                             IC1074.2
008200     ADD 1 TO S1.                                                 IC1074.2
008300     MOVE DN1 (IN3) TO DN2 (S1).                                  IC1074.2
008400*        THE TESTS IN LINK-TEST-03 SET THE INDEX DEFINED IN THE   IC1074.2
008500*    TABLE IN THE LINKAGE SECTION AND USE THE INDEX TO REFERENCE  IC1074.2
008600*    THE TABLE ITEMS.                                             IC1074.2
008700 LINK-TEST-04-01.                                                 IC1074.2
008800     SET IN3 TO 3.                                                IC1074.2
008900     SET IDN2 TO IN3.                                             IC1074.2
009000 LINK-TEST-04-02.                                                 IC1074.2
009100     IF IDN2 IS EQUAL TO IN3                                      IC1074.2
009200         MOVE "C" TO DN2 (3).                                     IC1074.2
009300*        THE TESTS IN LINK-TEST-04 RESET THE INDEX DATA ITEM      IC1074.2
009400*    TO CORRESPOND TO TABLE POSITION 3, AND COMPARES THE          IC1074.2
009500*    INDEX DATA ITEM TO THE INDEX.                                IC1074.2
009600 LINK-TEST-05-01.                                                 IC1074.2
009700     MOVE 4 TO S1.                                                IC1074.2
009800     SET IN3 TO S1.                                               IC1074.2
009900     MOVE DN1 (IN3) TO DN2 (S1).                                  IC1074.2
010000 LINK-TEST-05-02.                                                 IC1074.2
010100     MOVE DN1 (5) TO DN2 (5).                                     IC1074.2
010200*        THE TESTS IN LINK-TEST-05 MOVE DATA FROM THE FIRST       IC1074.2
010300*    TABLE DEFINED IN THE LINKAGE SECTION TO THE SECOND TABLE.    IC1074.2
010400 LINK-TEST-06.                                                    IC1074.2
010500     MOVE AL-CON TO DN3.                                          IC1074.2
010600*        THIS TEST MOVES DATA TO THE REDEFINED FIELD IN A TABLE   IC1074.2
010700*    IN THE LINKAGE SECTION.                                      IC1074.2
010800 EXIT-IC107.                                                      IC1074.2
010900     EXIT PROGRAM.                                                IC1074.2
      *END-OF,IC107A                                                                  
