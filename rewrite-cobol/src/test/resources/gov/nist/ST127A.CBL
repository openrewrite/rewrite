      *HEADER,COBOL,ST127A                                                            
000100 IDENTIFICATION DIVISION.                                         ST1274.2
000200 PROGRAM-ID.                                                      ST1274.2
000300     ST127A.                                                      ST1274.2
000400****************************************************************  ST1274.2
000500*                                                              *  ST1274.2
000600*    VALIDATION FOR:-                                          *  ST1274.2
000700*                                                              *  ST1274.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1274.2
000900*                                                              *  ST1274.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".ST1274.2
001100*                                                              *  ST1274.2
001200****************************************************************  ST1274.2
001300*                                                              *  ST1274.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  ST1274.2
001500*                                                              *  ST1274.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  ST1274.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  ST1274.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  ST1274.2
001900*        X-27  - SORT-FILE-NAME-1                              *  ST1274.2
002000*                                                              *  ST1274.2
002100****************************************************************  ST1274.2
002200*    ST127A IS A COMPLETELY SELF-CONTAINED PROGRAM.  THE INPUT    ST1274.2
002300*    PROCEDURE BUILDS THE 17-RECORD FILE SHOWN BELOW. THE         ST1274.2
002400*    OUTPUT PROCEDURE CHECKS THE SORTED FILE AND GENERATES THE    ST1274.2
002500*    REPORT.                                                      ST1274.2
002600*    SORT    SORT   SORT    SORT   SORT   SORT   SORT  SORT  LAST ST1274.2
002700*    KEY-1   KEY-2  KEY-3   KEY-4  KEY-5  KEY-6  KEY-7 KEY-8 CHAR ST1274.2
002800*    S9(6)   A(5)   SV9(16) X(10)  A(20)  X(10)  999   S99   PIC XST1274.2
002900*    USAGE   JUST           JUST                       USAGE      ST1274.2
003000*    COMP    RIGHT          RIGHT                      COMP       ST1274.2
003100*                                                                 ST1274.2
003200*    -054321  BBB  -.1234  BBBBBB A     AAAAAAAA  501  +99     1  ST1274.2
003300*    +123456  BBB  -.1234  BBBBBB A     AAAAAAAA  501  +99        ST1274.2
003400*    -054321  BBB  -.1234  BBBBBB A     AAAAAAAA  501  +99     2  ST1274.2
003500*    -054321    X  -.1234  BBBBBB A     AAAAAAAA  501  +99        ST1274.2
003600*    -054321  BBB  -.1234  BBBBBB A     AAAAAAAA  501  +99     3  ST1274.2
003700*    -054321  BBB  +.6     BBBBBB A     AAAAAAAA  501  +99        ST1274.2
003800*    -054321  BBB  -.1234  BBBBBB A     AAAAAAAA  501  +99     4  ST1274.2
003900*    -054321  BBB  -.1234       X A     AAAAAAAA  501  +99        ST1274.2
004000*    -054321  BBB  -.1234  BBBBBB A     AAAAAAAA  501  +99     5  ST1274.2
004100*    -054321  BBB  -.1234  BBBBBB Z     AAAAAAAA  501  +99        ST1274.2
004200*    -054321  BBB  -.1234  BBBBBB A     AAAAAAAA  501  +99     6  ST1274.2
004300*    -054321  BBB  -.1234  BBBBBB A     Z         501  +99        ST1274.2
004400*    -054321  BBB  -.1234  BBBBBB A     AAAAAAAA  501  +99     7  ST1274.2
004500*    -054321  BBB  -.1234  BBBBBB A     AAAAAAAA  418  +99        ST1274.2
004600*    -054321  BBB  -.1234  BBBBBB A     AAAAAAAA  501  +99     8  ST1274.2
004700*    -054321  BBB  -.1234  BBBBBB A     AAAAAAAA  501  -14        ST1274.2
004800*    -054321  BBB  -.1234  BBBBBB A     AAAAAAAA  501  +99     9  ST1274.2
004900*                                                                 ST1274.2
005000*    THIS PROGRAM TESTS THE USE OF THE "DUPLICATES" PHRASE OF     ST1274.2
005100*    THE  "SORT" STATEMENT.  THE ORDER OF RECORDS HAVING          ST1274.2
005200*    DUPLICATE KEYS AFTER THE EXECUTION OF A "SORT" STATEMENT     ST1274.2
005300*    MUST BE THE SAME AS THE ORDER OF THOSE RECORDS ON INPUT      ST1274.2
005400*    TO THE "SORT" STATEMENT.                                     ST1274.2
005500                                                                  ST1274.2
005600 ENVIRONMENT DIVISION.                                            ST1274.2
005700 CONFIGURATION SECTION.                                           ST1274.2
005800 SOURCE-COMPUTER.                                                 ST1274.2
005900     XXXXX082.                                                    ST1274.2
006000 OBJECT-COMPUTER.                                                 ST1274.2
006100     XXXXX083.                                                    ST1274.2
006200 INPUT-OUTPUT SECTION.                                            ST1274.2
006300 FILE-CONTROL.                                                    ST1274.2
006400     SELECT PRINT-FILE ASSIGN TO                                  ST1274.2
006500     XXXXX055.                                                    ST1274.2
006600     SELECT   SORTFILE-1H ASSIGN TO                               ST1274.2
006700     XXXXX027.                                                    ST1274.2
006800 DATA DIVISION.                                                   ST1274.2
006900 FILE SECTION.                                                    ST1274.2
007000 FD  PRINT-FILE.                                                  ST1274.2
007100 01  PRINT-REC PICTURE X(120).                                    ST1274.2
007200 01  DUMMY-RECORD PICTURE X(120).                                 ST1274.2
007300 SD  SORTFILE-1H                                                  ST1274.2
007400     DATA RECORD IS SORTFILE-REC.                                 ST1274.2
007500 01  SORTFILE-REC.                                                ST1274.2
007600   05 SORT-1.                                                     ST1274.2
007700     10 SORTKEY-8       PICTURE S99 COMPUTATIONAL.                ST1274.2
007800     10 SORTKEY-1       PICTURE S9(6) COMPUTATIONAL.              ST1274.2
007900     10 SORTKEY-7       PICTURE 999.                              ST1274.2
008000     10 SORTKEY-3       PICTURE SV9(16).                          ST1274.2
008100     10 FILLER          PICTURE XX.                               ST1274.2
008200     10 SORTKEY-4       PICTURE X(10) JUSTIFIED RIGHT.            ST1274.2
008300     10 SORTKEY-6       PICTURE X(10).                            ST1274.2
008400     10 SORTKEY-2       PICTURE A(05) JUSTIFIED RIGHT.            ST1274.2
008500     10 SORTKEY-5       PICTURE A(20).                            ST1274.2
008600     10 FILLER          PICTURE XXX.                              ST1274.2
008700  05    SORT-IDENTIFIER PICTURE X.                                ST1274.2
008800 WORKING-STORAGE SECTION.                                         ST1274.2
008900 77  WS-IDENTIFIER      PIC 9.                                    ST1274.2
009000 01  WS-SORTFILE-REC.                                             ST1274.2
009100     02 WS-8            PICTURE S99 COMPUTATIONAL.                ST1274.2
009200     02 WS-1            PICTURE S9(6) COMPUTATIONAL.              ST1274.2
009300     02 WS-7            PICTURE 999.                              ST1274.2
009400     02 WS-3            PICTURE SV9(16).                          ST1274.2
009500     02 FILLER          PICTURE XX.                               ST1274.2
009600     02 WS-4            PICTURE X(10) JUSTIFIED RIGHT.            ST1274.2
009700     02 WS-6            PICTURE X(10).                            ST1274.2
009800     02 WS-2            PICTURE A(05) JUSTIFIED RIGHT.            ST1274.2
009900     02 WS-5            PICTURE A(20).                            ST1274.2
010000     02 FILLER          PICTURE XXX.                              ST1274.2
010100 77  UTIL-CTR           PICTURE S99999.                           ST1274.2
010200 77  SPAC-E             PICTURE X VALUE " ".                      ST1274.2
010300 01  TEST-RESULTS.                                                ST1274.2
010400     02 FILLER                   PIC X      VALUE SPACE.          ST1274.2
010500     02 FEATURE                  PIC X(20)  VALUE SPACE.          ST1274.2
010600     02 FILLER                   PIC X      VALUE SPACE.          ST1274.2
010700     02 P-OR-F                   PIC X(5)   VALUE SPACE.          ST1274.2
010800     02 FILLER                   PIC X      VALUE SPACE.          ST1274.2
010900     02  PAR-NAME.                                                ST1274.2
011000       03 FILLER                 PIC X(19)  VALUE SPACE.          ST1274.2
011100       03  PARDOT-X              PIC X      VALUE SPACE.          ST1274.2
011200       03 DOTVALUE               PIC 99     VALUE ZERO.           ST1274.2
011300     02 FILLER                   PIC X(8)   VALUE SPACE.          ST1274.2
011400     02 RE-MARK                  PIC X(61).                       ST1274.2
011500 01  TEST-COMPUTED.                                               ST1274.2
011600     02 FILLER                   PIC X(30)  VALUE SPACE.          ST1274.2
011700     02 FILLER                   PIC X(17)  VALUE                 ST1274.2
011800            "       COMPUTED=".                                   ST1274.2
011900     02 COMPUTED-X.                                               ST1274.2
012000     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          ST1274.2
012100     03 COMPUTED-N               REDEFINES COMPUTED-A             ST1274.2
012200                                 PIC -9(9).9(9).                  ST1274.2
012300     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         ST1274.2
012400     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     ST1274.2
012500     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     ST1274.2
012600     03       CM-18V0 REDEFINES COMPUTED-A.                       ST1274.2
012700         04 COMPUTED-18V0                    PIC -9(18).          ST1274.2
012800         04 FILLER                           PIC X.               ST1274.2
012900     03 FILLER PIC X(50) VALUE SPACE.                             ST1274.2
013000 01  TEST-CORRECT.                                                ST1274.2
013100     02 FILLER PIC X(30) VALUE SPACE.                             ST1274.2
013200     02 FILLER PIC X(17) VALUE "       CORRECT =".                ST1274.2
013300     02 CORRECT-X.                                                ST1274.2
013400     03 CORRECT-A                  PIC X(20) VALUE SPACE.         ST1274.2
013500     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      ST1274.2
013600     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         ST1274.2
013700     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     ST1274.2
013800     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     ST1274.2
013900     03      CR-18V0 REDEFINES CORRECT-A.                         ST1274.2
014000         04 CORRECT-18V0                     PIC -9(18).          ST1274.2
014100         04 FILLER                           PIC X.               ST1274.2
014200     03 FILLER PIC X(2) VALUE SPACE.                              ST1274.2
014300     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     ST1274.2
014400 01  CCVS-C-1.                                                    ST1274.2
014500     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAST1274.2
014600-    "SS  PARAGRAPH-NAME                                          ST1274.2
014700-    "       REMARKS".                                            ST1274.2
014800     02 FILLER                     PIC X(20)    VALUE SPACE.      ST1274.2
014900 01  CCVS-C-2.                                                    ST1274.2
015000     02 FILLER                     PIC X        VALUE SPACE.      ST1274.2
015100     02 FILLER                     PIC X(6)     VALUE "TESTED".   ST1274.2
015200     02 FILLER                     PIC X(15)    VALUE SPACE.      ST1274.2
015300     02 FILLER                     PIC X(4)     VALUE "FAIL".     ST1274.2
015400     02 FILLER                     PIC X(94)    VALUE SPACE.      ST1274.2
015500 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       ST1274.2
015600 01  REC-CT                        PIC 99       VALUE ZERO.       ST1274.2
015700 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       ST1274.2
015800 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       ST1274.2
015900 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       ST1274.2
016000 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       ST1274.2
016100 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       ST1274.2
016200 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       ST1274.2
016300 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      ST1274.2
016400 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       ST1274.2
016500 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     ST1274.2
016600 01  CCVS-H-1.                                                    ST1274.2
016700     02  FILLER                    PIC X(39)    VALUE SPACES.     ST1274.2
016800     02  FILLER                    PIC X(42)    VALUE             ST1274.2
016900     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 ST1274.2
017000     02  FILLER                    PIC X(39)    VALUE SPACES.     ST1274.2
017100 01  CCVS-H-2A.                                                   ST1274.2
017200   02  FILLER                        PIC X(40)  VALUE SPACE.      ST1274.2
017300   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  ST1274.2
017400   02  FILLER                        PIC XXXX   VALUE             ST1274.2
017500     "4.2 ".                                                      ST1274.2
017600   02  FILLER                        PIC X(28)  VALUE             ST1274.2
017700            " COPY - NOT FOR DISTRIBUTION".                       ST1274.2
017800   02  FILLER                        PIC X(41)  VALUE SPACE.      ST1274.2
017900                                                                  ST1274.2
018000 01  CCVS-H-2B.                                                   ST1274.2
018100   02  FILLER                        PIC X(15)  VALUE             ST1274.2
018200            "TEST RESULT OF ".                                    ST1274.2
018300   02  TEST-ID                       PIC X(9).                    ST1274.2
018400   02  FILLER                        PIC X(4)   VALUE             ST1274.2
018500            " IN ".                                               ST1274.2
018600   02  FILLER                        PIC X(12)  VALUE             ST1274.2
018700     " HIGH       ".                                              ST1274.2
018800   02  FILLER                        PIC X(22)  VALUE             ST1274.2
018900            " LEVEL VALIDATION FOR ".                             ST1274.2
019000   02  FILLER                        PIC X(58)  VALUE             ST1274.2
019100     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1274.2
019200 01  CCVS-H-3.                                                    ST1274.2
019300     02  FILLER                      PIC X(34)  VALUE             ST1274.2
019400            " FOR OFFICIAL USE ONLY    ".                         ST1274.2
019500     02  FILLER                      PIC X(58)  VALUE             ST1274.2
019600     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".ST1274.2
019700     02  FILLER                      PIC X(28)  VALUE             ST1274.2
019800            "  COPYRIGHT   1985 ".                                ST1274.2
019900 01  CCVS-E-1.                                                    ST1274.2
020000     02 FILLER                       PIC X(52)  VALUE SPACE.      ST1274.2
020100     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              ST1274.2
020200     02 ID-AGAIN                     PIC X(9).                    ST1274.2
020300     02 FILLER                       PIC X(45)  VALUE SPACES.     ST1274.2
020400 01  CCVS-E-2.                                                    ST1274.2
020500     02  FILLER                      PIC X(31)  VALUE SPACE.      ST1274.2
020600     02  FILLER                      PIC X(21)  VALUE SPACE.      ST1274.2
020700     02 CCVS-E-2-2.                                               ST1274.2
020800         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      ST1274.2
020900         03 FILLER                   PIC X      VALUE SPACE.      ST1274.2
021000         03 ENDER-DESC               PIC X(44)  VALUE             ST1274.2
021100            "ERRORS ENCOUNTERED".                                 ST1274.2
021200 01  CCVS-E-3.                                                    ST1274.2
021300     02  FILLER                      PIC X(22)  VALUE             ST1274.2
021400            " FOR OFFICIAL USE ONLY".                             ST1274.2
021500     02  FILLER                      PIC X(12)  VALUE SPACE.      ST1274.2
021600     02  FILLER                      PIC X(58)  VALUE             ST1274.2
021700     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1274.2
021800     02  FILLER                      PIC X(13)  VALUE SPACE.      ST1274.2
021900     02 FILLER                       PIC X(15)  VALUE             ST1274.2
022000             " COPYRIGHT 1985".                                   ST1274.2
022100 01  CCVS-E-4.                                                    ST1274.2
022200     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      ST1274.2
022300     02 FILLER                       PIC X(4)   VALUE " OF ".     ST1274.2
022400     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      ST1274.2
022500     02 FILLER                       PIC X(40)  VALUE             ST1274.2
022600      "  TESTS WERE EXECUTED SUCCESSFULLY".                       ST1274.2
022700 01  XXINFO.                                                      ST1274.2
022800     02 FILLER                       PIC X(19)  VALUE             ST1274.2
022900            "*** INFORMATION ***".                                ST1274.2
023000     02 INFO-TEXT.                                                ST1274.2
023100       04 FILLER                     PIC X(8)   VALUE SPACE.      ST1274.2
023200       04 XXCOMPUTED                 PIC X(20).                   ST1274.2
023300       04 FILLER                     PIC X(5)   VALUE SPACE.      ST1274.2
023400       04 XXCORRECT                  PIC X(20).                   ST1274.2
023500     02 INF-ANSI-REFERENCE           PIC X(48).                   ST1274.2
023600 01  HYPHEN-LINE.                                                 ST1274.2
023700     02 FILLER  PIC IS X VALUE IS SPACE.                          ST1274.2
023800     02 FILLER  PIC IS X(65)    VALUE IS "************************ST1274.2
023900-    "*****************************************".                 ST1274.2
024000     02 FILLER  PIC IS X(54)    VALUE IS "************************ST1274.2
024100-    "******************************".                            ST1274.2
024200 01  CCVS-PGM-ID                     PIC X(9)   VALUE             ST1274.2
024300     "ST127A".                                                    ST1274.2
024400 PROCEDURE DIVISION.                                              ST1274.2
024500 SORT-PARA SECTION.                                               ST1274.2
024600 SORT-PARAGRAPH.                                                  ST1274.2
024700     MOVE    "XI-18 4.4.4 GR(3)b" TO ANSI-REFERENCE.              ST1274.2
024800     SORT     SORTFILE-1H ON                                      ST1274.2
024900              ASCENDING KEY SORTKEY-1                             ST1274.2
025000              ASCENDING SORTKEY-2                                 ST1274.2
025100              ASCENDING SORTKEY-3                                 ST1274.2
025200              ASCENDING SORTKEY-4                                 ST1274.2
025300              ASCENDING SORTKEY-5                                 ST1274.2
025400              ASCENDING SORTKEY-6                                 ST1274.2
025500              ASCENDING SORTKEY-7                                 ST1274.2
025600              ASCENDING SORTKEY-8                                 ST1274.2
025700              WITH DUPLICATES IN ORDER                            ST1274.2
025800              INPUT PROCEDURE INPROC                              ST1274.2
025900              OUTPUT PROCEDURE OUTPROC THRU OUTPROC-EXIT.         ST1274.2
026000     STOP     RUN.                                                ST1274.2
026100 INPROC SECTION.                                                  ST1274.2
026200 BUILD-FILE.                                                      ST1274.2
026300     MOVE     ZERO TO WS-IDENTIFIER.                              ST1274.2
026400     PERFORM  BUILD-RECORD.                                       ST1274.2
026500     MOVE     SORT-1              TO WS-SORTFILE-REC.             ST1274.2
026600     ADD      1 TO WS-IDENTIFIER.                                 ST1274.2
026700     MOVE     WS-IDENTIFIER       TO SORT-IDENTIFIER.             ST1274.2
026800     PERFORM  RELEASE-RECORD.                                     ST1274.2
026900     MOVE     +123456             TO SORTKEY-1.                   ST1274.2
027000     PERFORM  RELEASE-RECORD.                                     ST1274.2
027100     MOVE     WS-SORTFILE-REC     TO SORTFILE-REC.                ST1274.2
027200     ADD      1 TO WS-IDENTIFIER.                                 ST1274.2
027300     MOVE     WS-IDENTIFIER       TO SORT-IDENTIFIER.             ST1274.2
027400     PERFORM  RELEASE-RECORD.                                     ST1274.2
027500     PERFORM  BUILD-RECORD.                                       ST1274.2
027600     MOVE     "X"                 TO SORTKEY-2.                   ST1274.2
027700     PERFORM  RELEASE-RECORD.                                     ST1274.2
027800     MOVE     WS-SORTFILE-REC     TO SORTFILE-REC.                ST1274.2
027900     ADD      1 TO WS-IDENTIFIER.                                 ST1274.2
028000     MOVE     WS-IDENTIFIER       TO SORT-IDENTIFIER.             ST1274.2
028100     PERFORM  RELEASE-RECORD.                                     ST1274.2
028200     PERFORM  BUILD-RECORD.                                       ST1274.2
028300     MOVE     +.6                 TO SORTKEY-3.                   ST1274.2
028400     PERFORM  RELEASE-RECORD.                                     ST1274.2
028500     MOVE     WS-SORTFILE-REC     TO SORTFILE-REC.                ST1274.2
028600     ADD      1 TO WS-IDENTIFIER.                                 ST1274.2
028700     MOVE     WS-IDENTIFIER       TO SORT-IDENTIFIER.             ST1274.2
028800     PERFORM  RELEASE-RECORD.                                     ST1274.2
028900     PERFORM  BUILD-RECORD.                                       ST1274.2
029000     MOVE     "X"                 TO SORTKEY-4.                   ST1274.2
029100     PERFORM  RELEASE-RECORD.                                     ST1274.2
029200     MOVE     WS-SORTFILE-REC     TO SORTFILE-REC.                ST1274.2
029300     ADD      1 TO WS-IDENTIFIER.                                 ST1274.2
029400     MOVE     WS-IDENTIFIER       TO SORT-IDENTIFIER.             ST1274.2
029500     PERFORM  RELEASE-RECORD.                                     ST1274.2
029600     PERFORM  BUILD-RECORD.                                       ST1274.2
029700     MOVE     "Z"                 TO SORTKEY-5.                   ST1274.2
029800     PERFORM  RELEASE-RECORD.                                     ST1274.2
029900     MOVE     WS-SORTFILE-REC     TO SORTFILE-REC.                ST1274.2
030000     ADD      1 TO WS-IDENTIFIER.                                 ST1274.2
030100     MOVE     WS-IDENTIFIER       TO SORT-IDENTIFIER.             ST1274.2
030200     PERFORM  RELEASE-RECORD.                                     ST1274.2
030300     PERFORM  BUILD-RECORD.                                       ST1274.2
030400     MOVE     "Z"                 TO SORTKEY-6.                   ST1274.2
030500     PERFORM  RELEASE-RECORD.                                     ST1274.2
030600     MOVE     WS-SORTFILE-REC     TO SORTFILE-REC.                ST1274.2
030700     ADD      1 TO WS-IDENTIFIER.                                 ST1274.2
030800     MOVE     WS-IDENTIFIER       TO SORT-IDENTIFIER.             ST1274.2
030900     PERFORM  RELEASE-RECORD.                                     ST1274.2
031000     PERFORM  BUILD-RECORD.                                       ST1274.2
031100     MOVE     +418                TO SORTKEY-7.                   ST1274.2
031200     PERFORM  RELEASE-RECORD.                                     ST1274.2
031300     MOVE     WS-SORTFILE-REC     TO SORTFILE-REC.                ST1274.2
031400     ADD      1 TO WS-IDENTIFIER.                                 ST1274.2
031500     MOVE     WS-IDENTIFIER       TO SORT-IDENTIFIER.             ST1274.2
031600     PERFORM  RELEASE-RECORD.                                     ST1274.2
031700     PERFORM  BUILD-RECORD.                                       ST1274.2
031800     MOVE     -14                 TO SORTKEY-8.                   ST1274.2
031900     PERFORM  RELEASE-RECORD.                                     ST1274.2
032000     MOVE     WS-SORTFILE-REC     TO SORTFILE-REC.                ST1274.2
032100     ADD      1 TO WS-IDENTIFIER.                                 ST1274.2
032200     MOVE     WS-IDENTIFIER       TO SORT-IDENTIFIER.             ST1274.2
032300     PERFORM  RELEASE-RECORD.                                     ST1274.2
032400     GO       TO BUILD-EXIT.                                      ST1274.2
032500 BUILD-RECORD.                                                    ST1274.2
032600     MOVE     -054321             TO SORTKEY-1.                   ST1274.2
032700     MOVE     "BBB"               TO SORTKEY-2.                   ST1274.2
032800     MOVE     -.1234567890123456  TO SORTKEY-3.                   ST1274.2
032900     MOVE     "BBBBBB"            TO SORTKEY-4.                   ST1274.2
033000     MOVE     "A"                 TO SORTKEY-5.                   ST1274.2
033100     MOVE     "AAAAAAAA"          TO SORTKEY-6.                   ST1274.2
033200     MOVE     -501                TO SORTKEY-7.                   ST1274.2
033300*       NOTE THIS ITEM IS INTENTIONALLY MOVED TO AN UNSIGNED      ST1274.2
033400*             FIELD.                                              ST1274.2
033500     MOVE     +99                 TO SORTKEY-8.                   ST1274.2
033600     MOVE      SPACE              TO SORT-IDENTIFIER.             ST1274.2
033700 RELEASE-RECORD.                                                  ST1274.2
033800     RELEASE  SORTFILE-REC.                                       ST1274.2
033900 BUILD-EXIT.                                                      ST1274.2
034000     EXIT.                                                        ST1274.2
034100 OUTPROC SECTION.                                                 ST1274.2
034200 OPEN-FILES.                                                      ST1274.2
034300     OPEN     OUTPUT PRINT-FILE.                                  ST1274.2
034400     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   ST1274.2
034500     MOVE    SPACE TO TEST-RESULTS.                               ST1274.2
034600     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             ST1274.2
034700     IF       SPAC-E IS LESS THAN "B"                             ST1274.2
034800              GO TO SPACE-IS-LESS-THAN-B.                         ST1274.2
034900 B-IS-LESS-THAN-SPACE SECTION.                                    ST1274.2
035000 SORT-INIT-A.                                                     ST1274.2
035100     MOVE     "SORT - 8 ASC. KEYS" TO FEATURE.                    ST1274.2
035200*        NOTE THE RECORDS SHOULD BE SORTED INTO THE FOLLOWING     ST1274.2
035300*             ORDER --- 8 7 6 5 4 3 1 2 --- THAT IS,              ST1274.2
035400*             THE 8TH RECORD SORTS UP TO THE 1ST POSITION,        ST1274.2
035500*             THE 7TH RECORD SORTS UP TO THE 2ND POSITION, ETC.   ST1274.2
035600 SORT-TEST-1.                                                     ST1274.2
035700     RETURN   SORTFILE-1H AT END GO TO RETURN-ERROR.              ST1274.2
035800     IF       SORTKEY-7 EQUAL TO 418                              ST1274.2
035900              PERFORM PASS GO TO SORT-WRITE-1.                    ST1274.2
036000 SORT-FAIL-1.                                                     ST1274.2
036100     PERFORM  FAIL.                                               ST1274.2
036200     MOVE     SORTKEY-7 TO COMPUTED-N.                            ST1274.2
036300     MOVE     418 TO CORRECT-N.                                   ST1274.2
036400 SORT-WRITE-1.                                                    ST1274.2
036500     MOVE     "SORT-TEST-1 " TO PAR-NAME.                         ST1274.2
036600     PERFORM  PRINT-DETAIL.                                       ST1274.2
036700 SORT-TEST-2.                                                     ST1274.2
036800     RETURN   SORTFILE-1H AT END GO TO RETURN-ERROR.              ST1274.2
036900     IF       SORTKEY-8 EQUAL TO -14                              ST1274.2
037000              PERFORM PASS GO TO SORT-WRITE-2.                    ST1274.2
037100 SORT-FAIL-2.                                                     ST1274.2
037200     PERFORM  FAIL.                                               ST1274.2
037300     MOVE     SORTKEY-8 TO COMPUTED-N.                            ST1274.2
037400     MOVE     -14 TO CORRECT-N.                                   ST1274.2
037500 SORT-WRITE-2.                                                    ST1274.2
037600     MOVE     "SORT-TEST-2 " TO PAR-NAME.                         ST1274.2
037700     PERFORM  PRINT-DETAIL.                                       ST1274.2
037800 SORT-TEST-3.                                                     ST1274.2
037900     RETURN   SORTFILE-1H AT END GO TO RETURN-ERROR.              ST1274.2
038000     IF       SORTKEY-6 EQUAL TO "Z         "                     ST1274.2
038100              PERFORM PASS GO TO SORT-WRITE-3.                    ST1274.2
038200 SORT-FAIL-3.                                                     ST1274.2
038300     PERFORM  FAIL.                                               ST1274.2
038400     MOVE     SORTKEY-6 TO COMPUTED-A.                            ST1274.2
038500     MOVE     "Z         "           TO CORRECT-A.                ST1274.2
038600 SORT-WRITE-3.                                                    ST1274.2
038700     MOVE     "SORT-TEST-3 " TO PAR-NAME.                         ST1274.2
038800     PERFORM  PRINT-DETAIL.                                       ST1274.2
038900 SORT-TEST-4.                                                     ST1274.2
039000     RETURN   SORTFILE-1H AT END GO TO RETURN-ERROR.              ST1274.2
039100     IF       SORTKEY-5 EQUAL TO "Z                   "           ST1274.2
039200              PERFORM PASS GO TO SORT-WRITE-4.                    ST1274.2
039300 SORT-FAIL-4.                                                     ST1274.2
039400     PERFORM  FAIL.                                               ST1274.2
039500     MOVE     SORTKEY-5 TO COMPUTED-A.                            ST1274.2
039600     MOVE     "Z                   " TO CORRECT-A.                ST1274.2
039700 SORT-WRITE-4.                                                    ST1274.2
039800     MOVE     "SORT-TEST-4 " TO PAR-NAME.                         ST1274.2
039900     PERFORM  PRINT-DETAIL.                                       ST1274.2
040000 SORT-TEST-5.                                                     ST1274.2
040100     RETURN   SORTFILE-1H AT END GO TO RETURN-ERROR.              ST1274.2
040200     IF       SORTKEY-4 EQUAL TO "         X"                     ST1274.2
040300              PERFORM PASS GO TO SORT-WRITE-5.                    ST1274.2
040400 SORT-FAIL-5.                                                     ST1274.2
040500     PERFORM  FAIL.                                               ST1274.2
040600     MOVE     SORTKEY-4 TO COMPUTED-A.                            ST1274.2
040700     MOVE     "         X"           TO CORRECT-A.                ST1274.2
040800 SORT-WRITE-5.                                                    ST1274.2
040900     MOVE     "SORT-TEST-5 " TO PAR-NAME.                         ST1274.2
041000     PERFORM  PRINT-DETAIL.                                       ST1274.2
041100 SORT-TEST-6.                                                     ST1274.2
041200     RETURN   SORTFILE-1H AT END GO TO RETURN-ERROR.              ST1274.2
041300     IF       SORTKEY-3 EQUAL TO +.6000000000000000               ST1274.2
041400              PERFORM PASS GO TO SORT-WRITE-6.                    ST1274.2
041500 SORT-FAIL-6.                                                     ST1274.2
041600     PERFORM  FAIL.                                               ST1274.2
041700     MOVE     SORTKEY-3 TO COMPUTED-0V18.                         ST1274.2
041800     MOVE     +.6000000000000000     TO CORRECT-0V18.             ST1274.2
041900 SORT-WRITE-6.                                                    ST1274.2
042000     MOVE     "SORT-TEST-6 " TO PAR-NAME.                         ST1274.2
042100     PERFORM  PRINT-DETAIL.                                       ST1274.2
042200 SORT-TEST-7.                                                     ST1274.2
042300     RETURN   SORTFILE-1H AT END GO TO RETURN-ERROR.              ST1274.2
042400     IF       SORTKEY-2 EQUAL TO "    X"                          ST1274.2
042500              PERFORM PASS GO TO SORT-WRITE-7.                    ST1274.2
042600 SORT-FAIL-7.                                                     ST1274.2
042700     PERFORM  FAIL.                                               ST1274.2
042800     MOVE     SORTKEY-2 TO COMPUTED-A.                            ST1274.2
042900     MOVE     "    X" TO CORRECT-A.                               ST1274.2
043000 SORT-WRITE-7.                                                    ST1274.2
043100     MOVE     "SORT-TEST-7 " TO PAR-NAME.                         ST1274.2
043200     PERFORM  PRINT-DETAIL.                                       ST1274.2
043300 SORT-TEST-8.                                                     ST1274.2
043400     RETURN   SORTFILE-1H AT END GO TO RETURN-ERROR.              ST1274.2
043500     IF       SORTKEY-1 EQUAL TO +123456                          ST1274.2
043600              PERFORM PASS GO TO SORT-WRITE-8.                    ST1274.2
043700 SORT-FAIL-8.                                                     ST1274.2
043800     PERFORM  FAIL.                                               ST1274.2
043900     MOVE     SORTKEY-1 TO COMPUTED-N.                            ST1274.2
044000     MOVE     +123456 TO CORRECT-N.                               ST1274.2
044100 SORT-WRITE-8.                                                    ST1274.2
044200     MOVE     "SORT-TEST-8 " TO PAR-NAME.                         ST1274.2
044300     PERFORM  PRINT-DETAIL.                                       ST1274.2
044400 SORT-REMARK-A.                                                   ST1274.2
044500     MOVE     SPACE TO FEATURE.                                   ST1274.2
044600     MOVE     "THE COLLATING SEQUENCE" TO RE-MARK.                ST1274.2
044700     PERFORM  PRINT-DETAIL.                                       ST1274.2
044800     MOVE     "RENDERS TESTS 9 THRU 16" TO RE-MARK.               ST1274.2
044900     PERFORM  PRINT-DETAIL.                                       ST1274.2
045000     MOVE     "UNNECESSARY." TO RE-MARK.                          ST1274.2
045100     PERFORM  PRINT-DETAIL.                                       ST1274.2
045200     MOVE     "SORT - 8 ASC. KEYS" TO FEATURE.                    ST1274.2
045300     GO       TO CONTINUE-TESTING.                                ST1274.2
045400 SPACE-IS-LESS-THAN-B SECTION.                                    ST1274.2
045500 SORT-REMARK-B.                                                   ST1274.2
045600     MOVE     "THE COLLATING SEQUENCE" TO RE-MARK.                ST1274.2
045700     PERFORM  PRINT-DETAIL.                                       ST1274.2
045800     MOVE     "RENDERS TESTS 1 THRU 8" TO RE-MARK.                ST1274.2
045900     PERFORM  PRINT-DETAIL.                                       ST1274.2
046000     MOVE     "UNNECESSARY." TO RE-MARK.                          ST1274.2
046100     PERFORM  PRINT-DETAIL.                                       ST1274.2
046200     MOVE     "SORT - 8 ASC. KEYS" TO FEATURE.                    ST1274.2
046300*        NOTE THE RECORDS SHOULD BE SORTED INTO THE FOLLOWING     ST1274.2
046400*             ORDER --- 8 1 7 2 6 5 3 4 --- THAT IS,              ST1274.2
046500*             THE 1ST RECORD IS SORTED DOWN TO THE 8TH POSITION,  ST1274.2
046600*             THE 2ND RECORD SORTS UP TO THE 1ST POSITION, ETC.   ST1274.2
046700 SORT-TEST-9.                                                     ST1274.2
046800     RETURN   SORTFILE-1H AT END GO TO RETURN-ERROR.              ST1274.2
046900     IF       SORTKEY-2 EQUAL TO "    X"                          ST1274.2
047000              PERFORM PASS GO TO SORT-WRITE-9.                    ST1274.2
047100 SORT-FAIL-9.                                                     ST1274.2
047200     PERFORM  FAIL.                                               ST1274.2
047300     MOVE     SORTKEY-2 TO COMPUTED-A.                            ST1274.2
047400     MOVE     "    X" TO CORRECT-A.                               ST1274.2
047500 SORT-WRITE-9.                                                    ST1274.2
047600     MOVE     "SORT-TEST-9 " TO PAR-NAME.                         ST1274.2
047700     PERFORM  PRINT-DETAIL.                                       ST1274.2
047800*                                                                 ST1274.2
047900*    PERFORM  RETURN-DUPLICATE-RECORDS.                           ST1274.2
048000*                                                                 ST1274.2
048100 SORT-TEST-10.                                                    ST1274.2
048200     RETURN   SORTFILE-1H AT END GO TO RETURN-ERROR.              ST1274.2
048300     IF       SORTKEY-4 EQUAL TO "         X"                     ST1274.2
048400              PERFORM PASS GO TO SORT-WRITE-10.                   ST1274.2
048500 SORT-FAIL-10.                                                    ST1274.2
048600     PERFORM  FAIL.                                               ST1274.2
048700     MOVE     SORTKEY-4 TO COMPUTED-A.                            ST1274.2
048800     MOVE     "         X" TO CORRECT-A.                          ST1274.2
048900 SORT-WRITE-10.                                                   ST1274.2
049000     MOVE     "SORT-TEST-10" TO PAR-NAME.                         ST1274.2
049100     PERFORM  PRINT-DETAIL.                                       ST1274.2
049200 SORT-TEST-11.                                                    ST1274.2
049300     RETURN   SORTFILE-1H AT END GO TO RETURN-ERROR.              ST1274.2
049400     IF       SORTKEY-7 EQUAL TO 418                              ST1274.2
049500              PERFORM PASS GO TO SORT-WRITE-11.                   ST1274.2
049600 SORT-FAIL-11.                                                    ST1274.2
049700     PERFORM  FAIL.                                               ST1274.2
049800     MOVE     SORTKEY-7 TO COMPUTED-N                             ST1274.2
049900     MOVE     418 TO CORRECT-N.                                   ST1274.2
050000 SORT-WRITE-11.                                                   ST1274.2
050100     MOVE     "SORT-TEST-11" TO PAR-NAME.                         ST1274.2
050200     PERFORM  PRINT-DETAIL.                                       ST1274.2
050300 SORT-TEST-12.                                                    ST1274.2
050400     RETURN   SORTFILE-1H AT END GO TO RETURN-ERROR.              ST1274.2
050500     IF       SORTKEY-8 EQUAL TO -14                              ST1274.2
050600              PERFORM PASS GO TO SORT-WRITE-12.                   ST1274.2
050700 SORT-FAIL-12.                                                    ST1274.2
050800     PERFORM  FAIL.                                               ST1274.2
050900     MOVE     SORTKEY-8 TO COMPUTED-N.                            ST1274.2
051000     MOVE     -14 TO CORRECT-N.                                   ST1274.2
051100 SORT-WRITE-12.                                                   ST1274.2
051200     MOVE     "SORT-TEST-12" TO PAR-NAME.                         ST1274.2
051300     PERFORM  PRINT-DETAIL.                                       ST1274.2
051400     PERFORM  RETURN-DUPLICATE-RECORDS.                           ST1274.2
051500 SORT-TEST-13.                                                    ST1274.2
051600     RETURN   SORTFILE-1H AT END GO TO RETURN-ERROR.              ST1274.2
051700     IF       SORTKEY-6 EQUAL TO "Z         "                     ST1274.2
051800              PERFORM PASS GO TO SORT-WRITE-13.                   ST1274.2
051900 SORT-FAIL-13.                                                    ST1274.2
052000     PERFORM  FAIL.                                               ST1274.2
052100     MOVE     SORTKEY-6 TO COMPUTED-A.                            ST1274.2
052200     MOVE     "Z         "           TO CORRECT-A.                ST1274.2
052300 SORT-WRITE-13.                                                   ST1274.2
052400     MOVE     "SORT-TEST-13" TO PAR-NAME.                         ST1274.2
052500     PERFORM  PRINT-DETAIL.                                       ST1274.2
052600 SORT-TEST-14.                                                    ST1274.2
052700     RETURN   SORTFILE-1H AT END GO TO RETURN-ERROR.              ST1274.2
052800     IF       SORTKEY-5 EQUAL TO "Z                   "           ST1274.2
052900              PERFORM PASS GO TO SORT-WRITE-14.                   ST1274.2
053000 SORT-FAIL-14.                                                    ST1274.2
053100     PERFORM  FAIL.                                               ST1274.2
053200     MOVE     SORTKEY-5 TO COMPUTED-A.                            ST1274.2
053300     MOVE     "Z                   " TO CORRECT-A.                ST1274.2
053400 SORT-WRITE-14.                                                   ST1274.2
053500     MOVE     "SORT-TEST-14" TO PAR-NAME.                         ST1274.2
053600     PERFORM  PRINT-DETAIL.                                       ST1274.2
053700 SORT-TEST-15.                                                    ST1274.2
053800     RETURN   SORTFILE-1H AT END GO TO RETURN-ERROR.              ST1274.2
053900     IF       SORTKEY-3 EQUAL TO +.6000000000000000               ST1274.2
054000              PERFORM PASS GO TO SORT-WRITE-15.                   ST1274.2
054100 SORT-FAIL-15.                                                    ST1274.2
054200     PERFORM  FAIL.                                               ST1274.2
054300     MOVE     SORTKEY-3 TO COMPUTED-0V18.                         ST1274.2
054400     MOVE     +.6000000000000000     TO CORRECT-0V18.             ST1274.2
054500 SORT-WRITE-15.                                                   ST1274.2
054600     MOVE     "SORT-TEST-15" TO PAR-NAME.                         ST1274.2
054700     PERFORM  PRINT-DETAIL.                                       ST1274.2
054800 SORT-TEST-16.                                                    ST1274.2
054900     RETURN   SORTFILE-1H AT END GO TO RETURN-ERROR.              ST1274.2
055000     IF       SORTKEY-1 EQUAL TO +123456                          ST1274.2
055100              PERFORM PASS GO TO SORT-WRITE-16.                   ST1274.2
055200 SORT-FAIL-16.                                                    ST1274.2
055300     PERFORM  FAIL.                                               ST1274.2
055400     MOVE     SORTKEY-1 TO COMPUTED-N.                            ST1274.2
055500     MOVE     +123456 TO CORRECT-N.                               ST1274.2
055600 SORT-WRITE-16.                                                   ST1274.2
055700     MOVE     "SORT-TEST-16" TO PAR-NAME.                         ST1274.2
055800     PERFORM  PRINT-DETAIL.                                       ST1274.2
055900 CONTINUE-TESTING SECTION.                                        ST1274.2
056000 SORT-TEST-17.                                                    ST1274.2
056100     RETURN   SORTFILE-1H AT END                                  ST1274.2
056200              PERFORM PASS GO TO SORT-WRITE-17.                   ST1274.2
056300 SORT-FAIL-17.                                                    ST1274.2
056400     MOVE     "END OF FILE NOT FOUND" TO RE-MARK.                 ST1274.2
056500     PERFORM  FAIL.                                               ST1274.2
056600 SORT-WRITE-17.                                                   ST1274.2
056700     MOVE     "SORT-TEST-17" TO PAR-NAME.                         ST1274.2
056800     PERFORM  PRINT-DETAIL.                                       ST1274.2
056900     GO       TO OUTPROC-EXIT.                                    ST1274.2
057000 RETURN-ERROR.                                                    ST1274.2
057100     MOVE     "RETURN-ERROR" TO PAR-NAME.                         ST1274.2
057200     PERFORM  FAIL.                                               ST1274.2
057300     MOVE     "EOF PREMATURELY FOUND" TO RE-MARK.                 ST1274.2
057400     PERFORM  PRINT-DETAIL.                                       ST1274.2
057500     GO TO CCVS1-EXIT.                                            ST1274.2
057600 CLOSE-FILES.                                                     ST1274.2
057700     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   ST1274.2
057800 TERMINATE-CCVS.                                                  ST1274.2
057900*    EXIT PROGRAM.                                                ST1274.2
058000*TERMINATE-CALL.                                                  ST1274.2
058100     STOP     RUN.                                                ST1274.2
058200 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         ST1274.2
058300 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           ST1274.2
058400 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          ST1274.2
058500 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      ST1274.2
058600     MOVE "****TEST DELETED****" TO RE-MARK.                      ST1274.2
058700 PRINT-DETAIL.                                                    ST1274.2
058800     IF REC-CT NOT EQUAL TO ZERO                                  ST1274.2
058900             MOVE "." TO PARDOT-X                                 ST1274.2
059000             MOVE REC-CT TO DOTVALUE.                             ST1274.2
059100     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      ST1274.2
059200     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               ST1274.2
059300        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 ST1274.2
059400          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 ST1274.2
059500     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              ST1274.2
059600     MOVE SPACE TO CORRECT-X.                                     ST1274.2
059700     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         ST1274.2
059800     MOVE     SPACE TO RE-MARK.                                   ST1274.2
059900 HEAD-ROUTINE.                                                    ST1274.2
060000     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  ST1274.2
060100     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  ST1274.2
060200     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  ST1274.2
060300     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  ST1274.2
060400 COLUMN-NAMES-ROUTINE.                                            ST1274.2
060500     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           ST1274.2
060600     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1274.2
060700     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        ST1274.2
060800 END-ROUTINE.                                                     ST1274.2
060900     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.ST1274.2
061000 END-RTN-EXIT.                                                    ST1274.2
061100     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1274.2
061200 END-ROUTINE-1.                                                   ST1274.2
061300      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      ST1274.2
061400      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               ST1274.2
061500      ADD PASS-COUNTER TO ERROR-HOLD.                             ST1274.2
061600*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   ST1274.2
061700      MOVE PASS-COUNTER TO CCVS-E-4-1.                            ST1274.2
061800      MOVE ERROR-HOLD TO CCVS-E-4-2.                              ST1274.2
061900      MOVE CCVS-E-4 TO CCVS-E-2-2.                                ST1274.2
062000      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           ST1274.2
062100  END-ROUTINE-12.                                                 ST1274.2
062200      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        ST1274.2
062300     IF       ERROR-COUNTER IS EQUAL TO ZERO                      ST1274.2
062400         MOVE "NO " TO ERROR-TOTAL                                ST1274.2
062500         ELSE                                                     ST1274.2
062600         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       ST1274.2
062700     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           ST1274.2
062800     PERFORM WRITE-LINE.                                          ST1274.2
062900 END-ROUTINE-13.                                                  ST1274.2
063000     IF DELETE-COUNTER IS EQUAL TO ZERO                           ST1274.2
063100         MOVE "NO " TO ERROR-TOTAL  ELSE                          ST1274.2
063200         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      ST1274.2
063300     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   ST1274.2
063400     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           ST1274.2
063500      IF   INSPECT-COUNTER EQUAL TO ZERO                          ST1274.2
063600          MOVE "NO " TO ERROR-TOTAL                               ST1274.2
063700      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   ST1274.2
063800      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            ST1274.2
063900      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          ST1274.2
064000     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           ST1274.2
064100 WRITE-LINE.                                                      ST1274.2
064200     ADD 1 TO RECORD-COUNT.                                       ST1274.2
064300*    IF RECORD-COUNT GREATER 42                                   ST1274.2
064400*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          ST1274.2
064500*        MOVE SPACE TO DUMMY-RECORD                               ST1274.2
064600*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  ST1274.2
064700*        MOVE CCVS-H-1  TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    ST1274.2
064800*        MOVE CCVS-H-2A TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    ST1274.2
064900*        MOVE CCVS-H-2B TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    ST1274.2
065000*        MOVE CCVS-H-3  TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    ST1274.2
065100*        MOVE CCVS-C-1  TO DUMMY-RECORD PERFORM WRT-LN            ST1274.2
065200*        MOVE CCVS-C-2  TO DUMMY-RECORD PERFORM WRT-LN            ST1274.2
065300*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          ST1274.2
065400*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          ST1274.2
065500*        MOVE ZERO TO RECORD-COUNT.                               ST1274.2
065600     PERFORM WRT-LN.                                              ST1274.2
065700 WRT-LN.                                                          ST1274.2
065800     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               ST1274.2
065900     MOVE SPACE TO DUMMY-RECORD.                                  ST1274.2
066000 BLANK-LINE-PRINT.                                                ST1274.2
066100     PERFORM WRT-LN.                                              ST1274.2
066200 FAIL-ROUTINE.                                                    ST1274.2
066300     IF     COMPUTED-X NOT EQUAL TO SPACE                         ST1274.2
066400            GO TO   FAIL-ROUTINE-WRITE.                           ST1274.2
066500     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.ST1274.2
066600     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 ST1274.2
066700     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   ST1274.2
066800     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1274.2
066900     MOVE   SPACES TO INF-ANSI-REFERENCE.                         ST1274.2
067000     GO TO  FAIL-ROUTINE-EX.                                      ST1274.2
067100 FAIL-ROUTINE-WRITE.                                              ST1274.2
067200     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         ST1274.2
067300     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 ST1274.2
067400     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. ST1274.2
067500     MOVE   SPACES TO COR-ANSI-REFERENCE.                         ST1274.2
067600 FAIL-ROUTINE-EX. EXIT.                                           ST1274.2
067700 BAIL-OUT.                                                        ST1274.2
067800     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   ST1274.2
067900     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           ST1274.2
068000 BAIL-OUT-WRITE.                                                  ST1274.2
068100     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  ST1274.2
068200     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 ST1274.2
068300     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1274.2
068400     MOVE   SPACES TO INF-ANSI-REFERENCE.                         ST1274.2
068500 BAIL-OUT-EX. EXIT.                                               ST1274.2
068600 CCVS1-EXIT.                                                      ST1274.2
068700     EXIT.                                                        ST1274.2
068800*                                                                 ST1274.2
068900 OUTPROC-EXIT SECTION.                                            ST1274.2
069000 EXIT-ONLY.                                                       ST1274.2
069100     PERFORM  CLOSE-FILES.                                        ST1274.2
069200*                                                                 ST1274.2
069300 RETURN-DUPLICATE-RECORDS SECTION.                                ST1274.2
069400*================================                                 ST1274.2
069500 SORT-INIT-18.                                                    ST1274.2
069600     MOVE     "DUPLICATE KEYS" TO FEATURE.                        ST1274.2
069700*        NOTE THE RECORDS SHOULD BE SORTED INTO THE FOLLOWING     ST1274.2
069800*             ORDER OF THE LAST CHARACTER OF THE RECORD:          ST1274.2
069900*             ---- 1 2 3 4 5 6 7 8 9 ---                          ST1274.2
070000 SORT-TEST-18-1.                                                  ST1274.2
070100     MOVE    "SORT-TEST-18-1" TO PAR-NAME.                        ST1274.2
070200     RETURN   SORTFILE-1H AT END GO TO RETURN-ERROR.              ST1274.2
070300     IF       SORT-1 = WS-SORTFILE-REC                            ST1274.2
070400              PERFORM PASS                                        ST1274.2
070500              PERFORM PRINT-DETAIL                                ST1274.2
070600              GO TO   SORT-TEST-18-2.                             ST1274.2
070700 SORT-FAIL-18-1.                                                  ST1274.2
070800     PERFORM  FAIL.                                               ST1274.2
070900     PERFORM  CHECK-KEYS.                                         ST1274.2
071000 SORT-TEST-18-2.                                                  ST1274.2
071100     MOVE    "SORT-TEST-18-2" TO PAR-NAME.                        ST1274.2
071200     IF       SORT-IDENTIFIER = "1"                               ST1274.2
071300              PERFORM PASS                                        ST1274.2
071400              PERFORM PRINT-DETAIL                                ST1274.2
071500              GO TO   SORT-TEST-19-1.                             ST1274.2
071600 SORT-FAIL-18-2.                                                  ST1274.2
071700     PERFORM  FAIL.                                               ST1274.2
071800     MOVE     SORT-IDENTIFIER TO COMPUTED-X.                      ST1274.2
071900     MOVE    "1" TO CORRECT-X.                                    ST1274.2
072000     PERFORM PRINT-DETAIL.                                        ST1274.2
072100*                                                                 ST1274.2
072200 SORT-TEST-19-1.                                                  ST1274.2
072300     MOVE    "SORT-TEST-19-1" TO PAR-NAME.                        ST1274.2
072400     RETURN   SORTFILE-1H AT END GO TO RETURN-ERROR.              ST1274.2
072500     IF       SORT-1 = WS-SORTFILE-REC                            ST1274.2
072600              PERFORM PASS                                        ST1274.2
072700              PERFORM PRINT-DETAIL                                ST1274.2
072800              GO TO   SORT-TEST-19-2.                             ST1274.2
072900 SORT-FAIL-19-1.                                                  ST1274.2
073000     PERFORM  FAIL.                                               ST1274.2
073100     PERFORM  CHECK-KEYS.                                         ST1274.2
073200 SORT-TEST-19-2.                                                  ST1274.2
073300     MOVE    "SORT-TEST-19-2" TO PAR-NAME.                        ST1274.2
073400     IF       SORT-IDENTIFIER = "2"                               ST1274.2
073500              PERFORM PASS                                        ST1274.2
073600              PERFORM PRINT-DETAIL                                ST1274.2
073700              GO TO   SORT-TEST-20-1.                             ST1274.2
073800 SORT-FAIL-19-2.                                                  ST1274.2
073900     PERFORM  FAIL.                                               ST1274.2
074000     MOVE     SORT-IDENTIFIER TO COMPUTED-X.                      ST1274.2
074100     MOVE    "2" TO CORRECT-X.                                    ST1274.2
074200     PERFORM PRINT-DETAIL.                                        ST1274.2
074300*                                                                 ST1274.2
074400 SORT-TEST-20-1.                                                  ST1274.2
074500     MOVE    "SORT-TEST-20-1" TO PAR-NAME.                        ST1274.2
074600     RETURN   SORTFILE-1H AT END GO TO RETURN-ERROR.              ST1274.2
074700     IF       SORT-1 = WS-SORTFILE-REC                            ST1274.2
074800              PERFORM PASS                                        ST1274.2
074900              PERFORM PRINT-DETAIL                                ST1274.2
075000              GO TO   SORT-TEST-20-2.                             ST1274.2
075100 SORT-FAIL-20-1.                                                  ST1274.2
075200     PERFORM  FAIL.                                               ST1274.2
075300     PERFORM  CHECK-KEYS.                                         ST1274.2
075400 SORT-TEST-20-2.                                                  ST1274.2
075500     MOVE    "SORT-TEST-20-2" TO PAR-NAME.                        ST1274.2
075600     IF       SORT-IDENTIFIER = "3"                               ST1274.2
075700              PERFORM PASS                                        ST1274.2
075800              PERFORM PRINT-DETAIL                                ST1274.2
075900              GO TO   SORT-TEST-21-1.                             ST1274.2
076000 SORT-FAIL-20-2.                                                  ST1274.2
076100     PERFORM  FAIL.                                               ST1274.2
076200     MOVE     SORT-IDENTIFIER TO COMPUTED-X.                      ST1274.2
076300     MOVE    "3" TO CORRECT-X.                                    ST1274.2
076400     PERFORM PRINT-DETAIL.                                        ST1274.2
076500*                                                                 ST1274.2
076600 SORT-TEST-21-1.                                                  ST1274.2
076700     MOVE    "SORT-TEST-21-1" TO PAR-NAME.                        ST1274.2
076800     RETURN   SORTFILE-1H AT END GO TO RETURN-ERROR.              ST1274.2
076900     IF       SORT-1 = WS-SORTFILE-REC                            ST1274.2
077000              PERFORM PASS                                        ST1274.2
077100              PERFORM PRINT-DETAIL                                ST1274.2
077200              GO TO   SORT-TEST-21-2.                             ST1274.2
077300 SORT-FAIL-21-1.                                                  ST1274.2
077400     PERFORM  FAIL.                                               ST1274.2
077500     PERFORM  CHECK-KEYS.                                         ST1274.2
077600 SORT-TEST-21-2.                                                  ST1274.2
077700     MOVE    "SORT-TEST-21-2" TO PAR-NAME.                        ST1274.2
077800     IF       SORT-IDENTIFIER = "4"                               ST1274.2
077900              PERFORM PASS                                        ST1274.2
078000              PERFORM PRINT-DETAIL                                ST1274.2
078100              GO TO   SORT-TEST-22-1.                             ST1274.2
078200 SORT-FAIL-21-2.                                                  ST1274.2
078300     PERFORM  FAIL.                                               ST1274.2
078400     MOVE     SORT-IDENTIFIER TO COMPUTED-X.                      ST1274.2
078500     MOVE    "4" TO CORRECT-X.                                    ST1274.2
078600     PERFORM PRINT-DETAIL.                                        ST1274.2
078700*                                                                 ST1274.2
078800 SORT-TEST-22-1.                                                  ST1274.2
078900     MOVE    "SORT-TEST-22-1" TO PAR-NAME.                        ST1274.2
079000     RETURN   SORTFILE-1H AT END GO TO RETURN-ERROR.              ST1274.2
079100     IF       SORT-1 = WS-SORTFILE-REC                            ST1274.2
079200              PERFORM PASS                                        ST1274.2
079300              PERFORM PRINT-DETAIL                                ST1274.2
079400              GO TO   SORT-TEST-22-2.                             ST1274.2
079500 SORT-FAIL-22-1.                                                  ST1274.2
079600     PERFORM  FAIL.                                               ST1274.2
079700     PERFORM  CHECK-KEYS.                                         ST1274.2
079800 SORT-TEST-22-2.                                                  ST1274.2
079900     MOVE    "SORT-TEST-22-2" TO PAR-NAME.                        ST1274.2
080000     IF       SORT-IDENTIFIER = "5"                               ST1274.2
080100              PERFORM PASS                                        ST1274.2
080200              PERFORM PRINT-DETAIL                                ST1274.2
080300              GO TO   SORT-TEST-23-1.                             ST1274.2
080400 SORT-FAIL-22-2.                                                  ST1274.2
080500     PERFORM  FAIL.                                               ST1274.2
080600     MOVE     SORT-IDENTIFIER TO COMPUTED-X.                      ST1274.2
080700     MOVE    "5" TO CORRECT-X.                                    ST1274.2
080800     PERFORM PRINT-DETAIL.                                        ST1274.2
080900*                                                                 ST1274.2
081000 SORT-TEST-23-1.                                                  ST1274.2
081100     MOVE    "SORT-TEST-23-1" TO PAR-NAME.                        ST1274.2
081200     RETURN   SORTFILE-1H AT END GO TO RETURN-ERROR.              ST1274.2
081300     IF       SORT-1 = WS-SORTFILE-REC                            ST1274.2
081400              PERFORM PASS                                        ST1274.2
081500              PERFORM PRINT-DETAIL                                ST1274.2
081600              GO TO   SORT-TEST-23-2.                             ST1274.2
081700 SORT-FAIL-23-1.                                                  ST1274.2
081800     PERFORM  FAIL.                                               ST1274.2
081900     PERFORM  CHECK-KEYS.                                         ST1274.2
082000 SORT-TEST-23-2.                                                  ST1274.2
082100     MOVE    "SORT-TEST-23-2" TO PAR-NAME.                        ST1274.2
082200     IF       SORT-IDENTIFIER = "6"                               ST1274.2
082300              PERFORM PASS                                        ST1274.2
082400              PERFORM PRINT-DETAIL                                ST1274.2
082500              GO TO   SORT-TEST-24-1.                             ST1274.2
082600 SORT-FAIL-23-2.                                                  ST1274.2
082700     PERFORM  FAIL.                                               ST1274.2
082800     MOVE     SORT-IDENTIFIER TO COMPUTED-X.                      ST1274.2
082900     MOVE    "6" TO CORRECT-X.                                    ST1274.2
083000     PERFORM PRINT-DETAIL.                                        ST1274.2
083100*                                                                 ST1274.2
083200 SORT-TEST-24-1.                                                  ST1274.2
083300     MOVE    "SORT-TEST-24-1" TO PAR-NAME.                        ST1274.2
083400     RETURN   SORTFILE-1H AT END GO TO RETURN-ERROR.              ST1274.2
083500     IF       SORT-1 = WS-SORTFILE-REC                            ST1274.2
083600              PERFORM PASS                                        ST1274.2
083700              PERFORM PRINT-DETAIL                                ST1274.2
083800              GO TO   SORT-TEST-24-2.                             ST1274.2
083900 SORT-FAIL-24-1.                                                  ST1274.2
084000     PERFORM  FAIL.                                               ST1274.2
084100     PERFORM  CHECK-KEYS.                                         ST1274.2
084200 SORT-TEST-24-2.                                                  ST1274.2
084300     MOVE    "SORT-TEST-24-2" TO PAR-NAME.                        ST1274.2
084400     IF       SORT-IDENTIFIER = "7"                               ST1274.2
084500              PERFORM PASS                                        ST1274.2
084600              PERFORM PRINT-DETAIL                                ST1274.2
084700              GO TO   SORT-TEST-25-1.                             ST1274.2
084800 SORT-FAIL-24-2.                                                  ST1274.2
084900     PERFORM  FAIL.                                               ST1274.2
085000     MOVE     SORT-IDENTIFIER TO COMPUTED-X.                      ST1274.2
085100     MOVE    "7" TO CORRECT-X.                                    ST1274.2
085200     PERFORM PRINT-DETAIL.                                        ST1274.2
085300*                                                                 ST1274.2
085400 SORT-TEST-25-1.                                                  ST1274.2
085500     MOVE    "SORT-TEST-25-1" TO PAR-NAME.                        ST1274.2
085600     RETURN   SORTFILE-1H AT END GO TO RETURN-ERROR.              ST1274.2
085700     IF       SORT-1 = WS-SORTFILE-REC                            ST1274.2
085800              PERFORM PASS                                        ST1274.2
085900              PERFORM PRINT-DETAIL                                ST1274.2
086000              GO TO   SORT-TEST-25-2.                             ST1274.2
086100 SORT-FAIL-25-1.                                                  ST1274.2
086200     PERFORM  FAIL.                                               ST1274.2
086300     PERFORM  CHECK-KEYS.                                         ST1274.2
086400 SORT-TEST-25-2.                                                  ST1274.2
086500     MOVE    "SORT-TEST-25-2" TO PAR-NAME.                        ST1274.2
086600     IF       SORT-IDENTIFIER = "8"                               ST1274.2
086700              PERFORM PASS                                        ST1274.2
086800              PERFORM PRINT-DETAIL                                ST1274.2
086900              GO TO   SORT-TEST-26-1.                             ST1274.2
087000 SORT-FAIL-25-2.                                                  ST1274.2
087100     PERFORM  FAIL.                                               ST1274.2
087200     MOVE     SORT-IDENTIFIER TO COMPUTED-X.                      ST1274.2
087300     MOVE    "8" TO CORRECT-X.                                    ST1274.2
087400     PERFORM PRINT-DETAIL.                                        ST1274.2
087500*                                                                 ST1274.2
087600 SORT-TEST-26-1.                                                  ST1274.2
087700     MOVE    "SORT-TEST-26-1" TO PAR-NAME.                        ST1274.2
087800     RETURN   SORTFILE-1H AT END GO TO RETURN-ERROR.              ST1274.2
087900     IF       SORT-1 = WS-SORTFILE-REC                            ST1274.2
088000              PERFORM PASS                                        ST1274.2
088100              PERFORM PRINT-DETAIL                                ST1274.2
088200              GO TO   SORT-TEST-26-2.                             ST1274.2
088300 SORT-FAIL-26-1.                                                  ST1274.2
088400     PERFORM  FAIL.                                               ST1274.2
088500     PERFORM  CHECK-KEYS.                                         ST1274.2
088600 SORT-TEST-26-2.                                                  ST1274.2
088700     MOVE    "SORT-TEST-26-2" TO PAR-NAME.                        ST1274.2
088800     IF       SORT-IDENTIFIER = "9"                               ST1274.2
088900              PERFORM PASS                                        ST1274.2
089000              PERFORM PRINT-DETAIL                                ST1274.2
089100              GO TO   DUPLICATE-RECORD-EXIT.                      ST1274.2
089200 SORT-FAIL-26-2.                                                  ST1274.2
089300     PERFORM  FAIL.                                               ST1274.2
089400     MOVE     SORT-IDENTIFIER TO COMPUTED-X.                      ST1274.2
089500     MOVE    "9" TO CORRECT-X.                                    ST1274.2
089600     PERFORM PRINT-DETAIL.                                        ST1274.2
089700                                                                  ST1274.2
089800*                                                                 ST1274.2
089900 DUPLICATE-RECORD-EXIT.                                           ST1274.2
090000     EXIT.                                                        ST1274.2
090100*                                                                 ST1274.2
090200*                                                                 ST1274.2
090300 CHECK-KEYS SECTION.                                              ST1274.2
090400*==================                                               ST1274.2
090500 CHK-TEST-1.                                                      ST1274.2
090600     MOVE    "CHK-TEST-1" TO PAR-NAME.                            ST1274.2
090700     IF       SORTKEY-1 = WS-1                                    ST1274.2
090800              PERFORM PASS                                        ST1274.2
090900              PERFORM PRINT-DETAIL                                ST1274.2
091000              GO TO   CHK-TEST-2.                                 ST1274.2
091100 CHK-FAIL-1.                                                      ST1274.2
091200     PERFORM  FAIL.                                               ST1274.2
091300     MOVE     SORTKEY-1 TO COMPUTED-N.                            ST1274.2
091400     MOVE     WS-1      TO CORRECT-N.                             ST1274.2
091500     PERFORM PRINT-DETAIL.                                        ST1274.2
091600*                                                                 ST1274.2
091700 CHK-TEST-2.                                                      ST1274.2
091800     MOVE    "CHK-TEST-2" TO PAR-NAME.                            ST1274.2
091900     IF       SORTKEY-2 = WS-2                                    ST1274.2
092000              PERFORM PASS                                        ST1274.2
092100              PERFORM PRINT-DETAIL                                ST1274.2
092200              GO TO   CHK-TEST-3.                                 ST1274.2
092300 CHK-FAIL-2.                                                      ST1274.2
092400     PERFORM  FAIL.                                               ST1274.2
092500     MOVE     SORTKEY-1 TO COMPUTED-N.                            ST1274.2
092600     MOVE     WS-1      TO CORRECT-N.                             ST1274.2
092700     PERFORM PRINT-DETAIL.                                        ST1274.2
092800*                                                                 ST1274.2
092900 CHK-TEST-3.                                                      ST1274.2
093000     MOVE    "CHK-TEST-3" TO PAR-NAME.                            ST1274.2
093100     IF       SORTKEY-3 = WS-3                                    ST1274.2
093200              PERFORM PASS                                        ST1274.2
093300              PERFORM PRINT-DETAIL                                ST1274.2
093400              GO TO   CHK-TEST-4.                                 ST1274.2
093500 CHK-FAIL-3.                                                      ST1274.2
093600     PERFORM  FAIL.                                               ST1274.2
093700     MOVE     SORTKEY-3 TO COMPUTED-X.                            ST1274.2
093800     MOVE     WS-1      TO CORRECT-X.                             ST1274.2
093900     PERFORM PRINT-DETAIL.                                        ST1274.2
094000*                                                                 ST1274.2
094100 CHK-TEST-4.                                                      ST1274.2
094200     MOVE    "CHK-TEST-4" TO PAR-NAME.                            ST1274.2
094300     IF       SORTKEY-4 = WS-4                                    ST1274.2
094400              PERFORM PASS                                        ST1274.2
094500              PERFORM PRINT-DETAIL                                ST1274.2
094600              GO TO   CHK-TEST-5.                                 ST1274.2
094700 CHK-FAIL-4.                                                      ST1274.2
094800     PERFORM  FAIL.                                               ST1274.2
094900     MOVE     SORTKEY-4 TO COMPUTED-X.                            ST1274.2
095000     MOVE     WS-4      TO CORRECT-X.                             ST1274.2
095100     PERFORM PRINT-DETAIL.                                        ST1274.2
095200                                                                  ST1274.2
095300 CHK-TEST-5.                                                      ST1274.2
095400     MOVE    "CHK-TEST-5" TO PAR-NAME.                            ST1274.2
095500     IF       SORTKEY-5 = WS-5                                    ST1274.2
095600              PERFORM PASS                                        ST1274.2
095700              PERFORM PRINT-DETAIL                                ST1274.2
095800              GO TO   CHK-TEST-6.                                 ST1274.2
095900 CHK-FAIL-5.                                                      ST1274.2
096000     PERFORM  FAIL.                                               ST1274.2
096100     MOVE     SORTKEY-5 TO COMPUTED-X.                            ST1274.2
096200     MOVE     WS-5      TO CORRECT-X.                             ST1274.2
096300     PERFORM PRINT-DETAIL.                                        ST1274.2
096400*                                                                 ST1274.2
096500 CHK-TEST-6.                                                      ST1274.2
096600     MOVE    "CHK-TEST-6" TO PAR-NAME.                            ST1274.2
096700     IF       SORTKEY-6 = WS-6                                    ST1274.2
096800              PERFORM PASS                                        ST1274.2
096900              PERFORM PRINT-DETAIL                                ST1274.2
097000              GO TO   CHK-TEST-7.                                 ST1274.2
097100 CHK-FAIL-6.                                                      ST1274.2
097200     PERFORM  FAIL.                                               ST1274.2
097300     MOVE     SORTKEY-6 TO COMPUTED-X.                            ST1274.2
097400     MOVE     WS-6      TO CORRECT-X.                             ST1274.2
097500     PERFORM PRINT-DETAIL.                                        ST1274.2
097600*                                                                 ST1274.2
097700 CHK-TEST-7.                                                      ST1274.2
097800     MOVE    "CHK-TEST-7" TO PAR-NAME.                            ST1274.2
097900     IF       SORTKEY-7 = WS-7                                    ST1274.2
098000              PERFORM PASS                                        ST1274.2
098100              PERFORM PRINT-DETAIL                                ST1274.2
098200              GO TO   CHK-TEST-8.                                 ST1274.2
098300 CHK-FAIL-7.                                                      ST1274.2
098400     PERFORM  FAIL.                                               ST1274.2
098500     MOVE     SORTKEY-7 TO COMPUTED-X.                            ST1274.2
098600     MOVE     WS-7      TO CORRECT-X.                             ST1274.2
098700     PERFORM PRINT-DETAIL.                                        ST1274.2
098800*                                                                 ST1274.2
098900 CHK-TEST-8.                                                      ST1274.2
099000     MOVE    "CHK-TEST-8" TO PAR-NAME.                            ST1274.2
099100     IF       SORTKEY-8 = WS-8                                    ST1274.2
099200              PERFORM PASS                                        ST1274.2
099300              PERFORM PRINT-DETAIL                                ST1274.2
099400              GO TO   CHECK-KEYS-EXIT.                            ST1274.2
099500 CHK-FAIL-8.                                                      ST1274.2
099600     PERFORM  FAIL.                                               ST1274.2
099700     MOVE     SORTKEY-8 TO COMPUTED-N.                            ST1274.2
099800     MOVE     WS-8      TO CORRECT-N.                             ST1274.2
099900     PERFORM PRINT-DETAIL.                                        ST1274.2
100000 CHECK-KEYS-EXIT.                                                 ST1274.2
100100     EXIT.                                                        ST1274.2
      *END-OF,ST127A                                                                  
