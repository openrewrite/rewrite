      *HEADER,COBOL,ST115A,SUBPRG,ST116A                                              
000100 IDENTIFICATION DIVISION.                                         ST1164.2
000200 PROGRAM-ID.                                                      ST1164.2
000300     ST116A.                                                      ST1164.2
000400****************************************************************  ST1164.2
000500*                                                              *  ST1164.2
000600*    VALIDATION FOR:-                                          *  ST1164.2
000700*                                                              *  ST1164.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1164.2
000900*                                                              *  ST1164.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".ST1164.2
001100*                                                              *  ST1164.2
001200****************************************************************  ST1164.2
001300*                                                              *  ST1164.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  ST1164.2
001500*                                                              *  ST1164.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  ST1164.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  ST1164.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  ST1164.2
001900*        X-XXXD01  -  SQ-FS1             *                        ST1164.2
002000*        X-XXXP02  -  SQ-FS2             *                        ST1164.2
002100*        X-XXX027  -  SORT FILE ST-FS1   *                        ST1164.2
002200*                                                              *  ST1164.2
002300****************************************************************  ST1164.2
002400******************************************************************ST1164.2
002500*                                                                 ST1164.2
002600*                                                                 ST1164.2
002700*    ST116                                                        ST1164.2
002800*                                                                 ST1164.2
002900*                                                                 ST1164.2
003000*    OBJECTIVE -                                                  ST1164.2
003100*        ROUTINE ST116 IS A TEST OF THE SORT STATEMENT USING      ST1164.2
003200*    FIXED LENGTH RECORDS ( 507 CHARACTERS PER RECORD ).          ST1164.2
003300*                                                                 ST1164.2
003400*                                                                 ST1164.2
003500*    FEATURES TESTED -                                            ST1164.2
003600*    *   COLLATING SEQUENCE IS NATIVE.  NO COLLATING SEQUENCE     ST1164.2
003700*            STATEMENT IS USED IN THE ACTUAL SORT STATEMENT.      ST1164.2
003800*    *   FIXED LENGTH RECORDS                                     ST1164.2
003900*    *   OCCURS CLAUSES                                           ST1164.2
004000*    *   QUALIFIED ALPHANUMERIC AND NUMERIC SORT KEYS             ST1164.2
004100*                                                                 ST1164.2
004200*    *   SORT  SORT-FILE-NAME                                     ST1164.2
004300*        ON ASCENDING KEY KEY-1 OF DATA-NAME-1                    ST1164.2
004400*        ASCENDING KEY-2 OF DATA-NAME-2                           ST1164.2
004500*        USING FILE-NAME-1                                        ST1164.2
004600*        GIVING FILE-NAME-2.                                      ST1164.2
004700*                                                                 ST1164.2
004800*                                                                 ST1164.2
004900*    ANSI X3.23-1974 REFERENCES -                                 ST1164.2
005000*    *   SECTION 4.4  THE SORT STATEMENT  PAGE VII-14             ST1164.2
005100*                                                                 ST1164.2
005200*                                                                 ST1164.2
005300*    FILES USED -                                                 ST1164.2
005400*    *   FILES SQ-FS1 AND SQ-FS2 ON MAGNETIC TAPE OR MASS-STORAGE ST1164.2
005500*                                                                 ST1164.2
005600*    FILE SQ-FS1 IS CREATED IN ST115 AND PASSED TO ST116.  THE    ST1164.2
005700*    FILE SQ-FS2 IS PASSED TO ROUTINE ST117 FOR CHECKING.         ST1164.2
005800*                                                                 ST1164.2
005900*        SQ-FS1 -                                                 ST1164.2
006000*    NUMBER OF RECORDS IS SET BY THE INTEGER X-65.                ST1164.2
006100*    FIXED LENGTH ( 507 CHARACTERS PER RECORD )                   ST1164.2
006200*    BLOCKED 1                                                    ST1164.2
006300*    RESERVE 2 AREAS                                              ST1164.2
006400*                                                                 ST1164.2
006500*    SQ-FS1 IS SORTED GIVING SQ-FS2.                              ST1164.2
006600*                                                                 ST1164.2
006700*        SQ-FS2 -                                                 ST1164.2
006800*    SAME NUMBER OF RECORDS AS IN SQ-FS1  ( SET BY X-65 )         ST1164.2
006900*    FIXED LENGTH ( 507 CHARACTERS PER RECORD )                   ST1164.2
007000*    BLOCKED 2                                                    ST1164.2
007100*    RESERVE 4 AREAS                                              ST1164.2
007200*                                                                 ST1164.2
007300*                                                                 ST1164.2
007400*                                                                 ST1164.2
007500*                                                                 ST1164.2
007600*    OPTIONS RECOMMENDED -                                        ST1164.2
007700*        NOT APPLICABLE.                                          ST1164.2
007800*                                                                 ST1164.2
007900*                                                                 ST1164.2
008000*    TEST DESCRIPTIONS -                                          ST1164.2
008100*        NOT APPLICABLE.  ROUTINE ST116 ONLY CONTAINS THE SORT.   ST1164.2
008200*                                                                 ST1164.2
008300*                                                                 ST1164.2
008400*    ************************************************************ ST1164.2
008500 ENVIRONMENT DIVISION.                                            ST1164.2
008600 CONFIGURATION SECTION.                                           ST1164.2
008700 SOURCE-COMPUTER.                                                 ST1164.2
008800     XXXXX082.                                                    ST1164.2
008900 OBJECT-COMPUTER.                                                 ST1164.2
009000     XXXXX083.                                                    ST1164.2
009100 INPUT-OUTPUT SECTION.                                            ST1164.2
009200 FILE-CONTROL.                                                    ST1164.2
009300     SELECT SQ-FS1 ASSIGN TO                                      ST1164.2
009400     XXXXD001                                                     ST1164.2
009500     ORGANIZATION IS SEQUENTIAL                                   ST1164.2
009600     ACCESS MODE IS SEQUENTIAL.                                   ST1164.2
009700     SELECT SQ-FS2 ASSIGN TO                                      ST1164.2
009800     XXXXP002                                                     ST1164.2
009900     ORGANIZATION IS SEQUENTIAL                                   ST1164.2
010000     ACCESS MODE IS SEQUENTIAL.                                   ST1164.2
010100     SELECT ST-FS1 ASSIGN TO                                      ST1164.2
010200     XXXXX027.                                                    ST1164.2
010300 DATA DIVISION.                                                   ST1164.2
010400 FILE SECTION.                                                    ST1164.2
010500 FD  SQ-FS1                                                       ST1164.2
010600     LABEL RECORDS STANDARD                                       ST1164.2
010700C    VALUE OF                                                     ST1164.2
010800C    XXXXX074                                                     ST1164.2
010900C    IS                                                           ST1164.2
011000C    XXXXX075                                                     ST1164.2
011100*    XXXXX069                                                     ST1164.2
011200     BLOCK CONTAINS 1 RECORDS                                     ST1164.2
011300     RECORD CONTAINS 507 CHARACTERS                               ST1164.2
011400     DATA RECORD IS SQ-FS1R1-F-G-507.                             ST1164.2
011500 01  SQ-FS1R1-F-G-507.                                            ST1164.2
011600     10 REC-PREAMBLE PIC X(120).                                  ST1164.2
011700     10  LENGTH-1  PIC 999.                                       ST1164.2
011800     10  THE-THREE-KEYS.                                          ST1164.2
011900     20 KEY-1.                                                    ST1164.2
012000         30 ALPHAN-KEY PIC X.                                     ST1164.2
012100         30 NUM-KEY PIC 999.                                      ST1164.2
012200     20 KEY-2.                                                    ST1164.2
012300         30 ALPHAN-KEY PIC X.                                     ST1164.2
012400         30 NUM-KEY PIC 999.                                      ST1164.2
012500     20 KEY-3.                                                    ST1164.2
012600         30 ALPHAN-KEY PIC X.                                     ST1164.2
012700         30 NUM-KEY PIC 999.                                      ST1164.2
012800     10  STUFF-1 OCCURS                                           ST1164.2
012900             31 TIMES.                                            ST1164.2
013000         30  FILL-ME-UPS  PIC  X(12).                             ST1164.2
013100 FD  SQ-FS2                                                       ST1164.2
013200     LABEL RECORDS STANDARD                                       ST1164.2
013300C    VALUE OF                                                     ST1164.2
013400C    XXXXX074                                                     ST1164.2
013500C    IS                                                           ST1164.2
013600C    XXXXX076                                                     ST1164.2
013700*    XXXXX069                                                     ST1164.2
013800     BLOCK CONTAINS 2 RECORDS                                     ST1164.2
013900     RECORD CONTAINS 507 CHARACTERS                               ST1164.2
014000     DATA RECORD IS SQ-FS2R1-F-G-507.                             ST1164.2
014100 01  SQ-FS2R1-F-G-507.                                            ST1164.2
014200     10 REC-PRE-2 PIC X(120).                                     ST1164.2
014300     10  LENGTH-2  PIC 999.                                       ST1164.2
014400     10  THE-NEW-KEYS.                                            ST1164.2
014500     20 KEY-4.                                                    ST1164.2
014600         30 ALPHAN-KEY PIC X.                                     ST1164.2
014700         30 NUM-KEY PIC 999.                                      ST1164.2
014800     20 KEY-5.                                                    ST1164.2
014900         30 ALPHAN-KEY PIC X.                                     ST1164.2
015000         30 NUM-KEY PIC 999.                                      ST1164.2
015100     20 KEY-6.                                                    ST1164.2
015200         30 ALPHAN-KEY PIC X.                                     ST1164.2
015300         30 NUM-KEY PIC 999.                                      ST1164.2
015400     10  STUFF-2 OCCURS                                           ST1164.2
015500             31 TIMES.                                            ST1164.2
015600         30  FILLER PIC X(12).                                    ST1164.2
015700 SD  ST-FS1                                                       ST1164.2
015800     RECORD CONTAINS 507 CHARACTERS                               ST1164.2
015900     DATA RECORD IS ST-FS1R1-F-G-507.                             ST1164.2
016000 01  ST-FS1R1-F-G-507.                                            ST1164.2
016100     02 FILLER PIC X(120).                                        ST1164.2
016200     02 LENGTH-3 PIC 999.                                         ST1164.2
016300     02 NON-KEY-1.                                                ST1164.2
016400         03 A-KEY-NK1 PIC X.                                      ST1164.2
016500         03 N-KEY-NK1 PIC 999.                                    ST1164.2
016600     02 SORT-KEY.                                                 ST1164.2
016700         03 A-KEY-SK PIC X.                                       ST1164.2
016800         03 N-KEY-SK PIC 999.                                     ST1164.2
016900     02 NON-KEY-2.                                                ST1164.2
017000         03 A-KEY-NK2 PIC X.                                      ST1164.2
017100         03 N-KEY-NK2 PIC 999.                                    ST1164.2
017200     02  STUFF-3 OCCURS                                           ST1164.2
017300             31 TIMES.                                            ST1164.2
017400         03  FILLER PIC X(12).                                    ST1164.2
017500 PROCEDURE DIVISION.                                              ST1164.2
017600 SECT-ST116-0001 SECTION.                                         ST1164.2
017700 SRT-INIT-001.                                                    ST1164.2
017800     SORT ST-FS1                                                  ST1164.2
017900         ON ASCENDING KEY A-KEY-SK                                ST1164.2
018000         ASCENDING N-KEY-NK2                                      ST1164.2
018100         USING SQ-FS1                                             ST1164.2
018200         GIVING SQ-FS2.                                           ST1164.2
018300 STOP-THE-RUN.                                                    ST1164.2
018400     STOP RUN.                                                    ST1164.2
      *END-OF,ST116A                                                                  
