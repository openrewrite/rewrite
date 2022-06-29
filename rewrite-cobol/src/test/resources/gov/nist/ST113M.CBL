      *HEADER,COBOL,ST112M,SUBPRG,ST113M                                              
000100 IDENTIFICATION DIVISION.                                         ST1134.2
000200 PROGRAM-ID.                                                      ST1134.2
000300     ST113M.                                                      ST1134.2
000400****************************************************************  ST1134.2
000500*                                                              *  ST1134.2
000600*    VALIDATION FOR:-                                          *  ST1134.2
000700*                                                              *  ST1134.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1134.2
000900*                                                              *  ST1134.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".ST1134.2
001100*                                                              *  ST1134.2
001200****************************************************************  ST1134.2
001300*                                                              *  ST1134.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  ST1134.2
001500*                                                              *  ST1134.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  ST1134.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  ST1134.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  ST1134.2
001900*                                                              *  ST1134.2
002000****************************************************************  ST1134.2
002100 ENVIRONMENT DIVISION.                                            ST1134.2
002200 CONFIGURATION SECTION.                                           ST1134.2
002300 SOURCE-COMPUTER.                                                 ST1134.2
002400     XXXXX082.                                                    ST1134.2
002500 OBJECT-COMPUTER.                                                 ST1134.2
002600     XXXXX083.                                                    ST1134.2
002700 INPUT-OUTPUT SECTION.                                            ST1134.2
002800 FILE-CONTROL.                                                    ST1134.2
002900     SELECT PRINT-FILE ASSIGN TO                                  ST1134.2
003000     XXXXX055.                                                    ST1134.2
003100     SELECT   SORTIN-1M ASSIGN TO                                 ST1134.2
003200     XXXXP006.                                                    ST1134.2
003300     SELECT   SORTOUT-1M ASSIGN TO                                ST1134.2
003400     XXXXP001.                                                    ST1134.2
003500     SELECT   SORTFILE-1M ASSIGN TO                               ST1134.2
003600     XXXXX027.                                                    ST1134.2
003700 DATA DIVISION.                                                   ST1134.2
003800 FILE SECTION.                                                    ST1134.2
003900 FD  PRINT-FILE.                                                  ST1134.2
004000 01  PRINT-REC PICTURE X(120).                                    ST1134.2
004100 01  DUMMY-RECORD PICTURE X(120).                                 ST1134.2
004200 FD  SORTIN-1M                                                    ST1134.2
004300C    VALUE OF                                                     ST1134.2
004400C    XXXXX074                                                     ST1134.2
004500C    IS                                                           ST1134.2
004600C    XXXXX079                                                     ST1134.2
004700*    XXXXX069                                                     ST1134.2
004800     .                                                            ST1134.2
004900 01  SORT-KEY-IN        PICTURE X(33).                            ST1134.2
005000 FD  SORTOUT-1M                                                   ST1134.2
005100C    VALUE OF                                                     ST1134.2
005200C    XXXXX074                                                     ST1134.2
005300C    IS                                                           ST1134.2
005400C    XXXXX075                                                     ST1134.2
005500*    XXXXX069                                                     ST1134.2
005600     .                                                            ST1134.2
005700 01  SORT-KEY-OUT       PICTURE X(33).                            ST1134.2
005800 SD  SORTFILE-1M.                                                 ST1134.2
005900 01  SORT-KEY           PICTURE X(33).                            ST1134.2
006000 PROCEDURE    DIVISION.                                           ST1134.2
006100 SORT-PARA SECTION.                                               ST1134.2
006200 SORT-PARAGRAPH.                                                  ST1134.2
006300     SORT     SORTFILE-1M DESCENDING                              ST1134.2
006400              SORT-KEY                                            ST1134.2
006500              USING SORTIN-1M                                     ST1134.2
006600              GIVING SORTOUT-1M.                                  ST1134.2
006700     STOP     RUN.                                                ST1134.2
      *END-OF,ST113M                                                                  
