      *HEADER,COBOL,ST119A,SUBPRG,ST120A                                              
000100 IDENTIFICATION DIVISION.                                         ST1204.2
000200 PROGRAM-ID.                                                      ST1204.2
000300     ST120A.                                                      ST1204.2
000400****************************************************************  ST1204.2
000500*                                                              *  ST1204.2
000600*    VALIDATION FOR:-                                          *  ST1204.2
000700*                                                              *  ST1204.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1204.2
000900*                                                              *  ST1204.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".ST1204.2
001100*                                                              *  ST1204.2
001200****************************************************************  ST1204.2
001300*                                                              *  ST1204.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  ST1204.2
001500*                                                              *  ST1204.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  ST1204.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  ST1204.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  ST1204.2
001900*                                                              *  ST1204.2
002000****************************************************************  ST1204.2
002100 ENVIRONMENT DIVISION.                                            ST1204.2
002200 CONFIGURATION SECTION.                                           ST1204.2
002300 SOURCE-COMPUTER.                                                 ST1204.2
002400     XXXXX082.                                                    ST1204.2
002500 OBJECT-COMPUTER.                                                 ST1204.2
002600     XXXXX083.                                                    ST1204.2
002700 INPUT-OUTPUT SECTION.                                            ST1204.2
002800 FILE-CONTROL.                                                    ST1204.2
002900     SELECT SORTFILE-1B ASSIGN TO                                 ST1204.2
003000     XXXXX027.                                                    ST1204.2
003100     SELECT SORTIN-1B ASSIGN TO                                   ST1204.2
003200     XXXXD001.                                                    ST1204.2
003300     SELECT SORTOUT-1B ASSIGN TO                                  ST1204.2
003400     XXXXP002.                                                    ST1204.2
003500 DATA DIVISION.                                                   ST1204.2
003600 FILE SECTION.                                                    ST1204.2
003700 SD  SORTFILE-1B                                                  ST1204.2
003800     RECORD CONTAINS 120 CHARACTERS                               ST1204.2
003900     DATA RECORD S-RECORD.                                        ST1204.2
004000 01  S-RECORD.                                                    ST1204.2
004100     02  KEYS-GROUP.                                              ST1204.2
004200         03  KEY-1 PICTURE 9.                                     ST1204.2
004300         03  KEY-2 PICTURE 99.                                    ST1204.2
004400         03  KEY-3 PICTURE 999.                                   ST1204.2
004500         03  KEY-4 PICTURE 9999.                                  ST1204.2
004600         03  KEY-5 PICTURE 9(5).                                  ST1204.2
004700     02  FILLER  PICTURE X(105).                                  ST1204.2
004800 FD  SORTIN-1B                                                    ST1204.2
004900     BLOCK CONTAINS 10 RECORDS                                    ST1204.2
005000     LABEL RECORDS ARE STANDARD                                   ST1204.2
005100C    VALUE OF                                                     ST1204.2
005200C    XXXXX074                                                     ST1204.2
005300C    IS                                                           ST1204.2
005400C    XXXXX075                                                     ST1204.2
005500*    XXXXX069                                                     ST1204.2
005600     DATA RECORD IS INSORT.                                       ST1204.2
005700 01  INSORT PICTURE X(120).                                       ST1204.2
005800 FD  SORTOUT-1B                                                   ST1204.2
005900     BLOCK CONTAINS 10 RECORDS                                    ST1204.2
006000     LABEL RECORD STANDARD                                        ST1204.2
006100C    VALUE OF                                                     ST1204.2
006200C    XXXXX074                                                     ST1204.2
006300C    IS                                                           ST1204.2
006400C    XXXXX076                                                     ST1204.2
006500*    XXXXX069                                                     ST1204.2
006600     DATA RECORD OUTSORT.                                         ST1204.2
006700 01  OUTSORT PICTURE X(120).                                      ST1204.2
006800 PROCEDURE DIVISION.                                              ST1204.2
006900 SORT-STATEMENT.                                                  ST1204.2
007000     SORT SORTFILE-1B                                             ST1204.2
007100         ON DESCENDING KEY KEY-1                                  ST1204.2
007200         ON ASCENDING KEY KEY-2                                   ST1204.2
007300         ON DESCENDING KEY KEY-3                                  ST1204.2
007400         ASCENDING KEY-4 KEY-5                                    ST1204.2
007500       USING SORTIN-1B                                            ST1204.2
007600       GIVING SORTOUT-1B.                                         ST1204.2
007700 STOP-RUN-STATEMENT.                                              ST1204.2
007800     STOP RUN.                                                    ST1204.2
      *END-OF,ST120A                                                                  
