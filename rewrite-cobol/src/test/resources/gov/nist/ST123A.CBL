      *HEADER,COBOL,ST122A,SUBPRG,ST123A                                              
000100 IDENTIFICATION DIVISION.                                         ST1234.2
000200 PROGRAM-ID.                                                      ST1234.2
000300     ST123A.                                                      ST1234.2
000400****************************************************************  ST1234.2
000500*                                                              *  ST1234.2
000600*    VALIDATION FOR:-                                          *  ST1234.2
000700*                                                              *  ST1234.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1234.2
000900*                                                              *  ST1234.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".ST1234.2
001100*                                                              *  ST1234.2
001200****************************************************************  ST1234.2
001300*                                                              *  ST1234.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  ST1234.2
001500*                                                              *  ST1234.2
001600*        X-01                                                  *  ST1234.2
001700*        X-02                                                  *  ST1234.2
001800*        X-27                                                  *  ST1234.2
001900*        X-55  - SYSTEM PRINTER NAME.                          *  ST1234.2
002000*        X-69                                                  *  ST1234.2
002100*        X-74                                                  *  ST1234.2
002200*        X-75                                                  *  ST1234.2
002300*        X-76                                                  *  ST1234.2
002400*        X-82  - SOURCE COMPUTER NAME.                         *  ST1234.2
002500*        X-83  - OBJECT COMPUTER NAME.                         *  ST1234.2
002600*                                                              *  ST1234.2
002700****************************************************************  ST1234.2
002800*                                                              *  ST1234.2
002900*    PROGRAM ST123A TESTS THE SORTING OF VARIABLE LENGTH       *  ST1234.2
003000*    RECORDS.  THIS PROGRAM CAN BE USED ONLY IF LEVEL 2 OF THE *  ST1234.2
003100*    SEQUENTIAL I-O MODULE IS SUPPORTED AS THE                 *  ST1234.2
003200*    "RECORD IS VARYING IN SIZE" CLAUSE IS USED IN THE SD      *  ST1234.2
003300*    ENTRY.  (ST123A WILL BE RUN AS PART OF THE SET ST122A,    *  ST1234.2
003400*    ST123A, ST124A).                                          *  ST1234.2
003500*                                                              *  ST1234.2
003600****************************************************************  ST1234.2
003700 ENVIRONMENT DIVISION.                                            ST1234.2
003800 CONFIGURATION SECTION.                                           ST1234.2
003900 SOURCE-COMPUTER.                                                 ST1234.2
004000     XXXXX082.                                                    ST1234.2
004100 OBJECT-COMPUTER.                                                 ST1234.2
004200     XXXXX083.                                                    ST1234.2
004300 INPUT-OUTPUT SECTION.                                            ST1234.2
004400 FILE-CONTROL.                                                    ST1234.2
004500     SELECT PRINT-FILE ASSIGN TO                                  ST1234.2
004600     XXXXX055.                                                    ST1234.2
004700     SELECT   SORTIN-1J ASSIGN TO                                 ST1234.2
004800     XXXXD001.                                                    ST1234.2
004900     SELECT   SORTOUT-1J ASSIGN TO                                ST1234.2
005000     XXXXP002.                                                    ST1234.2
005100     SELECT   SORTFILE-1J ASSIGN TO                               ST1234.2
005200     XXXXX027.                                                    ST1234.2
005300 DATA DIVISION.                                                   ST1234.2
005400 FILE SECTION.                                                    ST1234.2
005500 FD  PRINT-FILE.                                                  ST1234.2
005600 01  PRINT-REC PICTURE X(120).                                    ST1234.2
005700 01  DUMMY-RECORD PICTURE X(120).                                 ST1234.2
005800 FD  SORTIN-1J                                                    ST1234.2
005900     LABEL RECORDS STANDARD                                       ST1234.2
006000C    VALUE OF                                                     ST1234.2
006100C    XXXXX074                                                     ST1234.2
006200C    IS                                                           ST1234.2
006300C    XXXXX075                                                     ST1234.2
006400*    XXXXX069                                                     ST1234.2
006500     RECORD IS VARYING IN SIZE                                    ST1234.2
006600     DATA RECORDS ARE SHORT-IN                                    ST1234.2
006700                      MEDIUM-IN                                   ST1234.2
006800                      LONG-IN.                                    ST1234.2
006900 01  SHORT-IN           PICTURE X(50).                            ST1234.2
007000 01  MEDIUM-IN          PICTURE X(75).                            ST1234.2
007100 01  LONG-IN.                                                     ST1234.2
007200     02 FALSE-LENGTH-1  PICTURE X(25).                            ST1234.2
007300     02 FALSE-LENGTH-2  PICTURE A(20).                            ST1234.2
007400     02 FALSE-LENGTH-3  PICTURE 9(15).                            ST1234.2
007500     02 FALSE-LENGTH-4  PICTURE X(40).                            ST1234.2
007600 FD  SORTOUT-1J                                                   ST1234.2
007700     LABEL RECORDS ARE STANDARD                                   ST1234.2
007800C    VALUE OF                                                     ST1234.2
007900C    XXXXX074                                                     ST1234.2
008000C    IS                                                           ST1234.2
008100C    XXXXX076                                                     ST1234.2
008200*    XXXXX069                                                     ST1234.2
008300     RECORD IS VARYING IN SIZE                                    ST1234.2
008400     DATA RECORD SHORT-OUT                                        ST1234.2
008500                 MEDIUM-OUT                                       ST1234.2
008600                 LONG-OUT.                                        ST1234.2
008700 01  SHORT-OUT.                                                   ST1234.2
008800     02 FAKE-LENGTH-1   PICTURE X(10).                            ST1234.2
008900     02 FAKE-LENGTH-2   PICTURE A(10).                            ST1234.2
009000     02 FAKE-LENGTH-3   PICTURE 9(10).                            ST1234.2
009100     02 FAKE-LENGTH-4   PICTURE X(20).                            ST1234.2
009200 01  MEDIUM-OUT         PICTURE X(75).                            ST1234.2
009300 01  LONG-OUT           PICTURE X(100).                           ST1234.2
009400 SD  SORTFILE-1J                                                  ST1234.2
009500     RECORD IS VARYING IN SIZE                                    ST1234.2
009600     DATA RECORD SHORT-SORT                                       ST1234.2
009700                 MEDIUM-SORT                                      ST1234.2
009800                 LONG-SORT.                                       ST1234.2
009900 01  SHORT-SORT.                                                  ST1234.2
010000     02 SHORT-NON-KEY   PICTURE XX.                               ST1234.2
010100     02 SHORT-KEY-1     PICTURE X(10).                            ST1234.2
010200     02 SHORT-KEY-2     PICTURE X(38).                            ST1234.2
010300 01  MEDIUM-SORT.                                                 ST1234.2
010400     02 MEDIUM-NON-KEY  PICTURE XX.                               ST1234.2
010500     02 MEDIUM-KEY-1    PICTURE X(10).                            ST1234.2
010600     02 MEDIUM-KEY-2    PICTURE X(38).                            ST1234.2
010700     02 MEDIUM-FILLER   PICTURE X(25).                            ST1234.2
010800 01  LONG-SORT.                                                   ST1234.2
010900     02 LONG-NON-KEY    PICTURE XX.                               ST1234.2
011000     02 LONG-KEY-1      PICTURE X(10).                            ST1234.2
011100     02 LONG-KEY-2      PICTURE X(38).                            ST1234.2
011200     02 LONG-FILLER     PICTURE X(50).                            ST1234.2
011300 PROCEDURE    DIVISION.                                           ST1234.2
011400 SORT-PARAGRAPH.                                                  ST1234.2
011500     SORT     SORTFILE-1J                                         ST1234.2
011600              DESCENDING KEY                                      ST1234.2
011700              MEDIUM-KEY-1                                        ST1234.2
011800              MEDIUM-KEY-2                                        ST1234.2
011900              USING SORTIN-1J                                     ST1234.2
012000              GIVING SORTOUT-1J.                                  ST1234.2
012100     STOP     RUN.                                                ST1234.2
      *END-OF,ST123A                                                                  
