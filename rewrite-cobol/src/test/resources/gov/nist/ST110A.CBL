      *HEADER,COBOL,ST109A,SUBPRG,ST110A                                              
000100 IDENTIFICATION DIVISION.                                         ST1104.2
000200 PROGRAM-ID.                                                      ST1104.2
000300     ST110A.                                                      ST1104.2
000400****************************************************************  ST1104.2
000500*                                                              *  ST1104.2
000600*    VALIDATION FOR:-                                          *  ST1104.2
000700*                                                              *  ST1104.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1104.2
000900*                                                              *  ST1104.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".ST1104.2
001100*                                                              *  ST1104.2
001200****************************************************************  ST1104.2
001300*                                                              *  ST1104.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  ST1104.2
001500*                                                              *  ST1104.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  ST1104.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  ST1104.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  ST1104.2
001900*                                                              *  ST1104.2
002000****************************************************************  ST1104.2
002100 ENVIRONMENT DIVISION.                                            ST1104.2
002200 CONFIGURATION SECTION.                                           ST1104.2
002300 SOURCE-COMPUTER.                                                 ST1104.2
002400     XXXXX082.                                                    ST1104.2
002500 OBJECT-COMPUTER.                                                 ST1104.2
002600     XXXXX083.                                                    ST1104.2
002700 INPUT-OUTPUT SECTION.                                            ST1104.2
002800 FILE-CONTROL.                                                    ST1104.2
002900     SELECT PRINT-FILE ASSIGN TO                                  ST1104.2
003000     XXXXX055.                                                    ST1104.2
003100     SELECT   SORTIN-1J ASSIGN TO                                 ST1104.2
003200     XXXXD001.                                                    ST1104.2
003300     SELECT   SORTOUT-1J ASSIGN TO                                ST1104.2
003400     XXXXP002.                                                    ST1104.2
003500     SELECT   SORTFILE-1J ASSIGN TO                               ST1104.2
003600     XXXXX027.                                                    ST1104.2
003700 DATA DIVISION.                                                   ST1104.2
003800 FILE SECTION.                                                    ST1104.2
003900 FD  PRINT-FILE.                                                  ST1104.2
004000 01  PRINT-REC PICTURE X(120).                                    ST1104.2
004100 01  DUMMY-RECORD PICTURE X(120).                                 ST1104.2
004200 FD  SORTIN-1J                                                    ST1104.2
004300     LABEL RECORDS STANDARD                                       ST1104.2
004400C    VALUE OF                                                     ST1104.2
004500C    XXXXX074                                                     ST1104.2
004600C    IS                                                           ST1104.2
004700C    XXXXX075                                                     ST1104.2
004800*    XXXXX069                                                     ST1104.2
004900     RECORD CONTAINS 50 TO 100 CHARACTERS                         ST1104.2
005000     DATA RECORDS ARE SHORT-IN                                    ST1104.2
005100                      MEDIUM-IN                                   ST1104.2
005200                      LONG-IN.                                    ST1104.2
005300 01  SHORT-IN           PICTURE X(50).                            ST1104.2
005400 01  MEDIUM-IN          PICTURE X(75).                            ST1104.2
005500 01  LONG-IN.                                                     ST1104.2
005600     02 FALSE-LENGTH-1  PICTURE X(25).                            ST1104.2
005700     02 FALSE-LENGTH-2  PICTURE A(20).                            ST1104.2
005800     02 FALSE-LENGTH-3  PICTURE 9(15).                            ST1104.2
005900     02 FALSE-LENGTH-4  PICTURE X(40).                            ST1104.2
006000 FD  SORTOUT-1J                                                   ST1104.2
006100     LABEL RECORDS ARE STANDARD                                   ST1104.2
006200C    VALUE OF                                                     ST1104.2
006300C    XXXXX074                                                     ST1104.2
006400C    IS                                                           ST1104.2
006500C    XXXXX076                                                     ST1104.2
006600*    XXXXX069                                                     ST1104.2
006700     RECORD CONTAINS 50 TO 100 CHARACTERS                         ST1104.2
006800     DATA RECORD SHORT-OUT                                        ST1104.2
006900                 MEDIUM-OUT                                       ST1104.2
007000                 LONG-OUT.                                        ST1104.2
007100 01  SHORT-OUT.                                                   ST1104.2
007200     02 FAKE-LENGTH-1   PICTURE X(10).                            ST1104.2
007300     02 FAKE-LENGTH-2   PICTURE A(10).                            ST1104.2
007400     02 FAKE-LENGTH-3   PICTURE 9(10).                            ST1104.2
007500     02 FAKE-LENGTH-4   PICTURE X(20).                            ST1104.2
007600 01  MEDIUM-OUT         PICTURE X(75).                            ST1104.2
007700 01  LONG-OUT           PICTURE X(100).                           ST1104.2
007800 SD  SORTFILE-1J                                                  ST1104.2
007900     RECORD 50 TO 100                                             ST1104.2
008000     DATA RECORD SHORT-SORT                                       ST1104.2
008100                 MEDIUM-SORT                                      ST1104.2
008200                 LONG-SORT.                                       ST1104.2
008300 01  SHORT-SORT.                                                  ST1104.2
008400     02 SHORT-NON-KEY   PICTURE XX.                               ST1104.2
008500     02 SHORT-KEY-1     PICTURE X(10).                            ST1104.2
008600     02 SHORT-KEY-2     PICTURE X(38).                            ST1104.2
008700 01  MEDIUM-SORT.                                                 ST1104.2
008800     02 MEDIUM-NON-KEY  PICTURE XX.                               ST1104.2
008900     02 MEDIUM-KEY-1    PICTURE X(10).                            ST1104.2
009000     02 MEDIUM-KEY-2    PICTURE X(38).                            ST1104.2
009100     02 MEDIUM-FILLER   PICTURE X(25).                            ST1104.2
009200 01  LONG-SORT.                                                   ST1104.2
009300     02 LONG-NON-KEY    PICTURE XX.                               ST1104.2
009400     02 LONG-KEY-1      PICTURE X(10).                            ST1104.2
009500     02 LONG-KEY-2      PICTURE X(38).                            ST1104.2
009600     02 LONG-FILLER     PICTURE X(50).                            ST1104.2
009700 PROCEDURE    DIVISION.                                           ST1104.2
009800 SORT-PARAGRAPH.                                                  ST1104.2
009900     SORT     SORTFILE-1J                                         ST1104.2
010000              DESCENDING KEY                                      ST1104.2
010100              MEDIUM-KEY-1                                        ST1104.2
010200              MEDIUM-KEY-2                                        ST1104.2
010300              USING SORTIN-1J                                     ST1104.2
010400              GIVING SORTOUT-1J.                                  ST1104.2
010500     STOP     RUN.                                                ST1104.2
      *END-OF,ST110A                                                                  
