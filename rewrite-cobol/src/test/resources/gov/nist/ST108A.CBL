      *HEADER,COBOL,ST108A                                                            
000100 IDENTIFICATION DIVISION.                                         ST1084.2
000200 PROGRAM-ID.                                                      ST1084.2
000300     ST108A.                                                      ST1084.2
000400****************************************************************  ST1084.2
000500*                                                              *  ST1084.2
000600*    VALIDATION FOR:-                                          *  ST1084.2
000700*                                                              *  ST1084.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1084.2
000900*                                                              *  ST1084.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".ST1084.2
001100*                                                              *  ST1084.2
001200****************************************************************  ST1084.2
001300*                                                              *  ST1084.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  ST1084.2
001500*                                                              *  ST1084.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  ST1084.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  ST1084.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  ST1084.2
001900*                                                              *  ST1084.2
002000****************************************************************  ST1084.2
002100*    ST108 IS A COMPLETELY SELF-CONTAINED PROGRAM.  THE INPUT     ST1084.2
002200*    PROCEDURE BUILDS THE EIGHT-RECORD FILE SHOWN BELOW. THE      ST1084.2
002300*    OUTPUT PROCEDURE CHECKS THE SORTED FILE AND GENERATES THE    ST1084.2
002400*    REPORT.                                                      ST1084.2
002500*    SORT    SORT    SORT    SORT    SORT   SORT     SORT    SORT ST1084.2
002600*    KEY-1   KEY-2   KEY-3   KEY-4   KEY-5  KEY-6    KEY-7   KEY-8ST1084.2
002700*    S9(6)   A(5)    SV9(16) X(10)   A(20)  X(10)    999     S99  ST1084.2
002800*    USAGE   JUST            JUST                            USAGEST1084.2
002900*    COMP    RIGHT           RIGHT                           COMP ST1084.2
003000*                                                                 ST1084.2
003100*    +123456     BBB -.1234   BBBBBB A      AAAAAAAA 501     +99  ST1084.2
003200*    -054321       X -.1234   BBBBBB A      AAAAAAAA 501     +99  ST1084.2
003300*    -054321     BBB +.6      BBBBBB A      AAAAAAAA 501     +99  ST1084.2
003400*    -054321     BBB -.1234        X A      AAAAAAAA 501     +99  ST1084.2
003500*    -054321     BBB -.1234   BBBBBB Z      AAAAAAAA 501     +99  ST1084.2
003600*    -054321     BBB -.1234   BBBBBB A      Z        501     +99  ST1084.2
003700*    -054321     BBB -.1234   BBBBBB A      AAAAAAAA 418     +99  ST1084.2
003800*    -054321     BBB -.1234   BBBBBB A      AAAAAAAA 501     -14  ST1084.2
003900*                                                                 ST1084.2
004000*    THIS PROGRAM CHECKS THE COMPILER"S ABILITY TO HANDLE EIGHT   ST1084.2
004100*    ASCENDING KEYS IN ONE FILE.                                  ST1084.2
004200                                                                  ST1084.2
004300 ENVIRONMENT DIVISION.                                            ST1084.2
004400 CONFIGURATION SECTION.                                           ST1084.2
004500 SOURCE-COMPUTER.                                                 ST1084.2
004600     XXXXX082.                                                    ST1084.2
004700 OBJECT-COMPUTER.                                                 ST1084.2
004800     XXXXX083.                                                    ST1084.2
004900 INPUT-OUTPUT SECTION.                                            ST1084.2
005000 FILE-CONTROL.                                                    ST1084.2
005100     SELECT PRINT-FILE ASSIGN TO                                  ST1084.2
005200     XXXXX055.                                                    ST1084.2
005300     SELECT   SORTFILE-1H ASSIGN TO                               ST1084.2
005400     XXXXX027.                                                    ST1084.2
005500 DATA DIVISION.                                                   ST1084.2
005600 FILE SECTION.                                                    ST1084.2
005700 FD  PRINT-FILE.                                                  ST1084.2
005800 01  PRINT-REC PICTURE X(120).                                    ST1084.2
005900 01  DUMMY-RECORD PICTURE X(120).                                 ST1084.2
006000 SD  SORTFILE-1H                                                  ST1084.2
006100     DATA RECORD IS SORTFILE-REC.                                 ST1084.2
006200 01  SORTFILE-REC.                                                ST1084.2
006300     02 SORTKEY-8       PICTURE S99 COMPUTATIONAL.                ST1084.2
006400     02 SORTKEY-1       PICTURE S9(6) COMPUTATIONAL.              ST1084.2
006500     02 SORTKEY-7       PICTURE 999.                              ST1084.2
006600     02 SORTKEY-3       PICTURE SV9(16).                          ST1084.2
006700     02 FILLER          PICTURE XX.                               ST1084.2
006800     02 SORTKEY-4       PICTURE X(10) JUSTIFIED RIGHT.            ST1084.2
006900     02 SORTKEY-6       PICTURE X(10).                            ST1084.2
007000     02 SORTKEY-2       PICTURE A(05) JUSTIFIED RIGHT.            ST1084.2
007100     02 SORTKEY-5       PICTURE A(20).                            ST1084.2
007200     02 FILLER          PICTURE XXX.                              ST1084.2
007300 WORKING-STORAGE SECTION.                                         ST1084.2
007400 77  UTIL-CTR           PICTURE S99999.                           ST1084.2
007500 77  SPAC-E             PICTURE X VALUE " ".                      ST1084.2
007600 01  TEST-RESULTS.                                                ST1084.2
007700     02 FILLER                   PIC X      VALUE SPACE.          ST1084.2
007800     02 FEATURE                  PIC X(20)  VALUE SPACE.          ST1084.2
007900     02 FILLER                   PIC X      VALUE SPACE.          ST1084.2
008000     02 P-OR-F                   PIC X(5)   VALUE SPACE.          ST1084.2
008100     02 FILLER                   PIC X      VALUE SPACE.          ST1084.2
008200     02  PAR-NAME.                                                ST1084.2
008300       03 FILLER                 PIC X(19)  VALUE SPACE.          ST1084.2
008400       03  PARDOT-X              PIC X      VALUE SPACE.          ST1084.2
008500       03 DOTVALUE               PIC 99     VALUE ZERO.           ST1084.2
008600     02 FILLER                   PIC X(8)   VALUE SPACE.          ST1084.2
008700     02 RE-MARK                  PIC X(61).                       ST1084.2
008800 01  TEST-COMPUTED.                                               ST1084.2
008900     02 FILLER                   PIC X(30)  VALUE SPACE.          ST1084.2
009000     02 FILLER                   PIC X(17)  VALUE                 ST1084.2
009100            "       COMPUTED=".                                   ST1084.2
009200     02 COMPUTED-X.                                               ST1084.2
009300     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          ST1084.2
009400     03 COMPUTED-N               REDEFINES COMPUTED-A             ST1084.2
009500                                 PIC -9(9).9(9).                  ST1084.2
009600     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         ST1084.2
009700     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     ST1084.2
009800     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     ST1084.2
009900     03       CM-18V0 REDEFINES COMPUTED-A.                       ST1084.2
010000         04 COMPUTED-18V0                    PIC -9(18).          ST1084.2
010100         04 FILLER                           PIC X.               ST1084.2
010200     03 FILLER PIC X(50) VALUE SPACE.                             ST1084.2
010300 01  TEST-CORRECT.                                                ST1084.2
010400     02 FILLER PIC X(30) VALUE SPACE.                             ST1084.2
010500     02 FILLER PIC X(17) VALUE "       CORRECT =".                ST1084.2
010600     02 CORRECT-X.                                                ST1084.2
010700     03 CORRECT-A                  PIC X(20) VALUE SPACE.         ST1084.2
010800     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      ST1084.2
010900     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         ST1084.2
011000     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     ST1084.2
011100     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     ST1084.2
011200     03      CR-18V0 REDEFINES CORRECT-A.                         ST1084.2
011300         04 CORRECT-18V0                     PIC -9(18).          ST1084.2
011400         04 FILLER                           PIC X.               ST1084.2
011500     03 FILLER PIC X(2) VALUE SPACE.                              ST1084.2
011600     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     ST1084.2
011700 01  CCVS-C-1.                                                    ST1084.2
011800     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAST1084.2
011900-    "SS  PARAGRAPH-NAME                                          ST1084.2
012000-    "       REMARKS".                                            ST1084.2
012100     02 FILLER                     PIC X(20)    VALUE SPACE.      ST1084.2
012200 01  CCVS-C-2.                                                    ST1084.2
012300     02 FILLER                     PIC X        VALUE SPACE.      ST1084.2
012400     02 FILLER                     PIC X(6)     VALUE "TESTED".   ST1084.2
012500     02 FILLER                     PIC X(15)    VALUE SPACE.      ST1084.2
012600     02 FILLER                     PIC X(4)     VALUE "FAIL".     ST1084.2
012700     02 FILLER                     PIC X(94)    VALUE SPACE.      ST1084.2
012800 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       ST1084.2
012900 01  REC-CT                        PIC 99       VALUE ZERO.       ST1084.2
013000 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       ST1084.2
013100 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       ST1084.2
013200 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       ST1084.2
013300 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       ST1084.2
013400 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       ST1084.2
013500 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       ST1084.2
013600 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      ST1084.2
013700 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       ST1084.2
013800 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     ST1084.2
013900 01  CCVS-H-1.                                                    ST1084.2
014000     02  FILLER                    PIC X(39)    VALUE SPACES.     ST1084.2
014100     02  FILLER                    PIC X(42)    VALUE             ST1084.2
014200     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 ST1084.2
014300     02  FILLER                    PIC X(39)    VALUE SPACES.     ST1084.2
014400 01  CCVS-H-2A.                                                   ST1084.2
014500   02  FILLER                        PIC X(40)  VALUE SPACE.      ST1084.2
014600   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  ST1084.2
014700   02  FILLER                        PIC XXXX   VALUE             ST1084.2
014800     "4.2 ".                                                      ST1084.2
014900   02  FILLER                        PIC X(28)  VALUE             ST1084.2
015000            " COPY - NOT FOR DISTRIBUTION".                       ST1084.2
015100   02  FILLER                        PIC X(41)  VALUE SPACE.      ST1084.2
015200                                                                  ST1084.2
015300 01  CCVS-H-2B.                                                   ST1084.2
015400   02  FILLER                        PIC X(15)  VALUE             ST1084.2
015500            "TEST RESULT OF ".                                    ST1084.2
015600   02  TEST-ID                       PIC X(9).                    ST1084.2
015700   02  FILLER                        PIC X(4)   VALUE             ST1084.2
015800            " IN ".                                               ST1084.2
015900   02  FILLER                        PIC X(12)  VALUE             ST1084.2
016000     " HIGH       ".                                              ST1084.2
016100   02  FILLER                        PIC X(22)  VALUE             ST1084.2
016200            " LEVEL VALIDATION FOR ".                             ST1084.2
016300   02  FILLER                        PIC X(58)  VALUE             ST1084.2
016400     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1084.2
016500 01  CCVS-H-3.                                                    ST1084.2
016600     02  FILLER                      PIC X(34)  VALUE             ST1084.2
016700            " FOR OFFICIAL USE ONLY    ".                         ST1084.2
016800     02  FILLER                      PIC X(58)  VALUE             ST1084.2
016900     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".ST1084.2
017000     02  FILLER                      PIC X(28)  VALUE             ST1084.2
017100            "  COPYRIGHT   1985 ".                                ST1084.2
017200 01  CCVS-E-1.                                                    ST1084.2
017300     02 FILLER                       PIC X(52)  VALUE SPACE.      ST1084.2
017400     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              ST1084.2
017500     02 ID-AGAIN                     PIC X(9).                    ST1084.2
017600     02 FILLER                       PIC X(45)  VALUE SPACES.     ST1084.2
017700 01  CCVS-E-2.                                                    ST1084.2
017800     02  FILLER                      PIC X(31)  VALUE SPACE.      ST1084.2
017900     02  FILLER                      PIC X(21)  VALUE SPACE.      ST1084.2
018000     02 CCVS-E-2-2.                                               ST1084.2
018100         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      ST1084.2
018200         03 FILLER                   PIC X      VALUE SPACE.      ST1084.2
018300         03 ENDER-DESC               PIC X(44)  VALUE             ST1084.2
018400            "ERRORS ENCOUNTERED".                                 ST1084.2
018500 01  CCVS-E-3.                                                    ST1084.2
018600     02  FILLER                      PIC X(22)  VALUE             ST1084.2
018700            " FOR OFFICIAL USE ONLY".                             ST1084.2
018800     02  FILLER                      PIC X(12)  VALUE SPACE.      ST1084.2
018900     02  FILLER                      PIC X(58)  VALUE             ST1084.2
019000     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1084.2
019100     02  FILLER                      PIC X(13)  VALUE SPACE.      ST1084.2
019200     02 FILLER                       PIC X(15)  VALUE             ST1084.2
019300             " COPYRIGHT 1985".                                   ST1084.2
019400 01  CCVS-E-4.                                                    ST1084.2
019500     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      ST1084.2
019600     02 FILLER                       PIC X(4)   VALUE " OF ".     ST1084.2
019700     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      ST1084.2
019800     02 FILLER                       PIC X(40)  VALUE             ST1084.2
019900      "  TESTS WERE EXECUTED SUCCESSFULLY".                       ST1084.2
020000 01  XXINFO.                                                      ST1084.2
020100     02 FILLER                       PIC X(19)  VALUE             ST1084.2
020200            "*** INFORMATION ***".                                ST1084.2
020300     02 INFO-TEXT.                                                ST1084.2
020400       04 FILLER                     PIC X(8)   VALUE SPACE.      ST1084.2
020500       04 XXCOMPUTED                 PIC X(20).                   ST1084.2
020600       04 FILLER                     PIC X(5)   VALUE SPACE.      ST1084.2
020700       04 XXCORRECT                  PIC X(20).                   ST1084.2
020800     02 INF-ANSI-REFERENCE           PIC X(48).                   ST1084.2
020900 01  HYPHEN-LINE.                                                 ST1084.2
021000     02 FILLER  PIC IS X VALUE IS SPACE.                          ST1084.2
021100     02 FILLER  PIC IS X(65)    VALUE IS "************************ST1084.2
021200-    "*****************************************".                 ST1084.2
021300     02 FILLER  PIC IS X(54)    VALUE IS "************************ST1084.2
021400-    "******************************".                            ST1084.2
021500 01  CCVS-PGM-ID                     PIC X(9)   VALUE             ST1084.2
021600     "ST108A".                                                    ST1084.2
021700 PROCEDURE DIVISION.                                              ST1084.2
021800 SORT-PARA SECTION.                                               ST1084.2
021900 SORT-PARAGRAPH.                                                  ST1084.2
022000     SORT     SORTFILE-1H ON                                      ST1084.2
022100              ASCENDING KEY SORTKEY-1                             ST1084.2
022200              ASCENDING SORTKEY-2                                 ST1084.2
022300              ASCENDING SORTKEY-3                                 ST1084.2
022400              ASCENDING SORTKEY-4                                 ST1084.2
022500              ASCENDING SORTKEY-5                                 ST1084.2
022600              ASCENDING SORTKEY-6                                 ST1084.2
022700              ASCENDING SORTKEY-7                                 ST1084.2
022800              ASCENDING SORTKEY-8                                 ST1084.2
022900              INPUT PROCEDURE INPROC                              ST1084.2
023000              OUTPUT PROCEDURE OUTPROC THRU OUTPROC-EXIT.         ST1084.2
023100     STOP     RUN.                                                ST1084.2
023200 INPROC SECTION.                                                  ST1084.2
023300 BUILD-FILE.                                                      ST1084.2
023400     PERFORM  BUILD-RECORD.                                       ST1084.2
023500     MOVE     +123456             TO SORTKEY-1.                   ST1084.2
023600     PERFORM  RELEASE-RECORD.                                     ST1084.2
023700     PERFORM  BUILD-RECORD.                                       ST1084.2
023800     MOVE     "X"                 TO SORTKEY-2.                   ST1084.2
023900     PERFORM  RELEASE-RECORD.                                     ST1084.2
024000     PERFORM  BUILD-RECORD.                                       ST1084.2
024100     MOVE     +.6                 TO SORTKEY-3.                   ST1084.2
024200     PERFORM  RELEASE-RECORD.                                     ST1084.2
024300     PERFORM  BUILD-RECORD.                                       ST1084.2
024400     MOVE     "X"                 TO SORTKEY-4.                   ST1084.2
024500     PERFORM  RELEASE-RECORD.                                     ST1084.2
024600     PERFORM  BUILD-RECORD.                                       ST1084.2
024700     MOVE     "Z"                 TO SORTKEY-5.                   ST1084.2
024800     PERFORM  RELEASE-RECORD.                                     ST1084.2
024900     PERFORM  BUILD-RECORD.                                       ST1084.2
025000     MOVE     "Z"                 TO SORTKEY-6.                   ST1084.2
025100     PERFORM  RELEASE-RECORD.                                     ST1084.2
025200     PERFORM  BUILD-RECORD.                                       ST1084.2
025300     MOVE     +418                TO SORTKEY-7.                   ST1084.2
025400     PERFORM  RELEASE-RECORD.                                     ST1084.2
025500     PERFORM  BUILD-RECORD.                                       ST1084.2
025600     MOVE     -14                 TO SORTKEY-8.                   ST1084.2
025700     PERFORM  RELEASE-RECORD.                                     ST1084.2
025800     GO       TO BUILD-EXIT.                                      ST1084.2
025900 BUILD-RECORD.                                                    ST1084.2
026000     MOVE     -054321             TO SORTKEY-1.                   ST1084.2
026100     MOVE     "BBB"               TO SORTKEY-2.                   ST1084.2
026200     MOVE     -.1234567890123456  TO SORTKEY-3.                   ST1084.2
026300     MOVE     "BBBBBB"            TO SORTKEY-4.                   ST1084.2
026400     MOVE     "A"                 TO SORTKEY-5.                   ST1084.2
026500     MOVE     "AAAAAAAA"          TO SORTKEY-6.                   ST1084.2
026600     MOVE     -501                TO SORTKEY-7.                   ST1084.2
026700*       NOTE THIS ITEM IS INTENTIONALLY MOVED TO AN UNSIGNED      ST1084.2
026800*             FIELD.                                              ST1084.2
026900     MOVE     +99                 TO SORTKEY-8.                   ST1084.2
027000 RELEASE-RECORD.                                                  ST1084.2
027100     RELEASE  SORTFILE-REC.                                       ST1084.2
027200 BUILD-EXIT.                                                      ST1084.2
027300     EXIT.                                                        ST1084.2
027400 OUTPROC SECTION.                                                 ST1084.2
027500 OPEN-FILES.                                                      ST1084.2
027600     OPEN     OUTPUT PRINT-FILE.                                  ST1084.2
027700     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   ST1084.2
027800     MOVE    SPACE TO TEST-RESULTS.                               ST1084.2
027900     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             ST1084.2
028000     IF       SPAC-E IS LESS THAN "B"                             ST1084.2
028100              GO TO SPACE-IS-LESS-THAN-B.                         ST1084.2
028200 B-IS-LESS-THAN-SPACE SECTION.                                    ST1084.2
028300 SORT-INIT-A.                                                     ST1084.2
028400     MOVE     "SORT - 8 ASC. KEYS" TO FEATURE.                    ST1084.2
028500*        NOTE THE RECORDS SHOULD BE SORTED INTO THE FOLLOWING     ST1084.2
028600*             ORDER --- 8 7 6 5 4 3 1 2 --- THAT IS,              ST1084.2
028700*             THE 8TH RECORD SORTS UP TO THE 1ST POSITION,        ST1084.2
028800*             THE 7TH RECORD SORTS UP TO THE 2ND POSITION, ETC.   ST1084.2
028900 SORT-TEST-1.                                                     ST1084.2
029000     RETURN   SORTFILE-1H AT END GO TO RETURN-ERROR.              ST1084.2
029100     IF       SORTKEY-7 EQUAL TO 418                              ST1084.2
029200              PERFORM PASS GO TO SORT-WRITE-1.                    ST1084.2
029300 SORT-FAIL-1.                                                     ST1084.2
029400     PERFORM  FAIL.                                               ST1084.2
029500     MOVE     SORTKEY-7 TO COMPUTED-N.                            ST1084.2
029600     MOVE     418 TO CORRECT-N.                                   ST1084.2
029700 SORT-WRITE-1.                                                    ST1084.2
029800     MOVE     "SORT-TEST-1 " TO PAR-NAME.                         ST1084.2
029900     PERFORM  PRINT-DETAIL.                                       ST1084.2
030000 SORT-TEST-2.                                                     ST1084.2
030100     RETURN   SORTFILE-1H AT END GO TO RETURN-ERROR.              ST1084.2
030200     IF       SORTKEY-8 EQUAL TO -14                              ST1084.2
030300              PERFORM PASS GO TO SORT-WRITE-2.                    ST1084.2
030400 SORT-FAIL-2.                                                     ST1084.2
030500     PERFORM  FAIL.                                               ST1084.2
030600     MOVE     SORTKEY-8 TO COMPUTED-N.                            ST1084.2
030700     MOVE     -14 TO CORRECT-N.                                   ST1084.2
030800 SORT-WRITE-2.                                                    ST1084.2
030900     MOVE     "SORT-TEST-2 " TO PAR-NAME.                         ST1084.2
031000     PERFORM  PRINT-DETAIL.                                       ST1084.2
031100 SORT-TEST-3.                                                     ST1084.2
031200     RETURN   SORTFILE-1H AT END GO TO RETURN-ERROR.              ST1084.2
031300     IF       SORTKEY-6 EQUAL TO "Z         "                     ST1084.2
031400              PERFORM PASS GO TO SORT-WRITE-3.                    ST1084.2
031500 SORT-FAIL-3.                                                     ST1084.2
031600     PERFORM  FAIL.                                               ST1084.2
031700     MOVE     SORTKEY-6 TO COMPUTED-A.                            ST1084.2
031800     MOVE     "Z         "           TO CORRECT-A.                ST1084.2
031900 SORT-WRITE-3.                                                    ST1084.2
032000     MOVE     "SORT-TEST-3 " TO PAR-NAME.                         ST1084.2
032100     PERFORM  PRINT-DETAIL.                                       ST1084.2
032200 SORT-TEST-4.                                                     ST1084.2
032300     RETURN   SORTFILE-1H AT END GO TO RETURN-ERROR.              ST1084.2
032400     IF       SORTKEY-5 EQUAL TO "Z                   "           ST1084.2
032500              PERFORM PASS GO TO SORT-WRITE-4.                    ST1084.2
032600 SORT-FAIL-4.                                                     ST1084.2
032700     PERFORM  FAIL.                                               ST1084.2
032800     MOVE     SORTKEY-5 TO COMPUTED-A.                            ST1084.2
032900     MOVE     "Z                   " TO CORRECT-A.                ST1084.2
033000 SORT-WRITE-4.                                                    ST1084.2
033100     MOVE     "SORT-TEST-4 " TO PAR-NAME.                         ST1084.2
033200     PERFORM  PRINT-DETAIL.                                       ST1084.2
033300 SORT-TEST-5.                                                     ST1084.2
033400     RETURN   SORTFILE-1H AT END GO TO RETURN-ERROR.              ST1084.2
033500     IF       SORTKEY-4 EQUAL TO "         X"                     ST1084.2
033600              PERFORM PASS GO TO SORT-WRITE-5.                    ST1084.2
033700 SORT-FAIL-5.                                                     ST1084.2
033800     PERFORM  FAIL.                                               ST1084.2
033900     MOVE     SORTKEY-4 TO COMPUTED-A.                            ST1084.2
034000     MOVE     "         X"           TO CORRECT-A.                ST1084.2
034100 SORT-WRITE-5.                                                    ST1084.2
034200     MOVE     "SORT-TEST-5 " TO PAR-NAME.                         ST1084.2
034300     PERFORM  PRINT-DETAIL.                                       ST1084.2
034400 SORT-TEST-6.                                                     ST1084.2
034500     RETURN   SORTFILE-1H AT END GO TO RETURN-ERROR.              ST1084.2
034600     IF       SORTKEY-3 EQUAL TO +.6000000000000000               ST1084.2
034700              PERFORM PASS GO TO SORT-WRITE-6.                    ST1084.2
034800 SORT-FAIL-6.                                                     ST1084.2
034900     PERFORM  FAIL.                                               ST1084.2
035000     MOVE     SORTKEY-3 TO COMPUTED-0V18.                         ST1084.2
035100     MOVE     +.6000000000000000     TO CORRECT-0V18.             ST1084.2
035200 SORT-WRITE-6.                                                    ST1084.2
035300     MOVE     "SORT-TEST-6 " TO PAR-NAME.                         ST1084.2
035400     PERFORM  PRINT-DETAIL.                                       ST1084.2
035500 SORT-TEST-7.                                                     ST1084.2
035600     RETURN   SORTFILE-1H AT END GO TO RETURN-ERROR.              ST1084.2
035700     IF       SORTKEY-2 EQUAL TO "    X"                          ST1084.2
035800              PERFORM PASS GO TO SORT-WRITE-7.                    ST1084.2
035900 SORT-FAIL-7.                                                     ST1084.2
036000     PERFORM  FAIL.                                               ST1084.2
036100     MOVE     SORTKEY-2 TO COMPUTED-A.                            ST1084.2
036200     MOVE     "    X" TO CORRECT-A.                               ST1084.2
036300 SORT-WRITE-7.                                                    ST1084.2
036400     MOVE     "SORT-TEST-7 " TO PAR-NAME.                         ST1084.2
036500     PERFORM  PRINT-DETAIL.                                       ST1084.2
036600 SORT-TEST-8.                                                     ST1084.2
036700     RETURN   SORTFILE-1H AT END GO TO RETURN-ERROR.              ST1084.2
036800     IF       SORTKEY-1 EQUAL TO +123456                          ST1084.2
036900              PERFORM PASS GO TO SORT-WRITE-8.                    ST1084.2
037000 SORT-FAIL-8.                                                     ST1084.2
037100     PERFORM  FAIL.                                               ST1084.2
037200     MOVE     SORTKEY-1 TO COMPUTED-N.                            ST1084.2
037300     MOVE     +123456 TO CORRECT-N.                               ST1084.2
037400 SORT-WRITE-8.                                                    ST1084.2
037500     MOVE     "SORT-TEST-8 " TO PAR-NAME.                         ST1084.2
037600     PERFORM  PRINT-DETAIL.                                       ST1084.2
037700 SORT-REMARK-A.                                                   ST1084.2
037800     MOVE     SPACE TO FEATURE.                                   ST1084.2
037900     MOVE     "THE COLLATING SEQUENCE" TO RE-MARK.                ST1084.2
038000     PERFORM  PRINT-DETAIL.                                       ST1084.2
038100     MOVE     "RENDERS TESTS 9 THRU 16" TO RE-MARK.               ST1084.2
038200     PERFORM  PRINT-DETAIL.                                       ST1084.2
038300     MOVE     "UNNECESSARY." TO RE-MARK.                          ST1084.2
038400     PERFORM  PRINT-DETAIL.                                       ST1084.2
038500     MOVE     "SORT - 8 ASC. KEYS" TO FEATURE.                    ST1084.2
038600     GO       TO CONTINUE-TESTING.                                ST1084.2
038700 SPACE-IS-LESS-THAN-B SECTION.                                    ST1084.2
038800 SORT-REMARK-B.                                                   ST1084.2
038900     MOVE     "THE COLLATING SEQUENCE" TO RE-MARK.                ST1084.2
039000     PERFORM  PRINT-DETAIL.                                       ST1084.2
039100     MOVE     "RENDERS TESTS 1 THRU 8" TO RE-MARK.                ST1084.2
039200     PERFORM  PRINT-DETAIL.                                       ST1084.2
039300     MOVE     "UNNECESSARY." TO RE-MARK.                          ST1084.2
039400     PERFORM  PRINT-DETAIL.                                       ST1084.2
039500     MOVE     "SORT - 8 ASC. KEYS" TO FEATURE.                    ST1084.2
039600*        NOTE THE RECORDS SHOULD BE SORTED INTO THE FOLLOWING     ST1084.2
039700*             ORDER --- 8 1 7 2 6 5 3 4 --- THAT IS,              ST1084.2
039800*             THE 1ST RECORD IS SORTED DOWN TO THE 8TH POSITION,  ST1084.2
039900*             THE 2ND RECORD SORTS UP TO THE 1ST POSITION, ETC.   ST1084.2
040000 SORT-TEST-9.                                                     ST1084.2
040100     RETURN   SORTFILE-1H AT END GO TO RETURN-ERROR.              ST1084.2
040200     IF       SORTKEY-2 EQUAL TO "    X"                          ST1084.2
040300              PERFORM PASS GO TO SORT-WRITE-9.                    ST1084.2
040400 SORT-FAIL-9.                                                     ST1084.2
040500     PERFORM  FAIL.                                               ST1084.2
040600     MOVE     SORTKEY-2 TO COMPUTED-A.                            ST1084.2
040700     MOVE     "    X" TO CORRECT-A.                               ST1084.2
040800 SORT-WRITE-9.                                                    ST1084.2
040900     MOVE     "SORT-TEST-9 " TO PAR-NAME.                         ST1084.2
041000     PERFORM  PRINT-DETAIL.                                       ST1084.2
041100 SORT-TEST-10.                                                    ST1084.2
041200     RETURN   SORTFILE-1H AT END GO TO RETURN-ERROR.              ST1084.2
041300     IF       SORTKEY-4 EQUAL TO "         X"                     ST1084.2
041400              PERFORM PASS GO TO SORT-WRITE-10.                   ST1084.2
041500 SORT-FAIL-10.                                                    ST1084.2
041600     PERFORM  FAIL.                                               ST1084.2
041700     MOVE     SORTKEY-4 TO COMPUTED-A.                            ST1084.2
041800     MOVE     "         X" TO CORRECT-A.                          ST1084.2
041900 SORT-WRITE-10.                                                   ST1084.2
042000     MOVE     "SORT-TEST-10" TO PAR-NAME.                         ST1084.2
042100     PERFORM  PRINT-DETAIL.                                       ST1084.2
042200 SORT-TEST-11.                                                    ST1084.2
042300     RETURN   SORTFILE-1H AT END GO TO RETURN-ERROR.              ST1084.2
042400     IF       SORTKEY-7 EQUAL TO 418                              ST1084.2
042500              PERFORM PASS GO TO SORT-WRITE-11.                   ST1084.2
042600 SORT-FAIL-11.                                                    ST1084.2
042700     PERFORM  FAIL.                                               ST1084.2
042800     MOVE     SORTKEY-7 TO COMPUTED-N                             ST1084.2
042900     MOVE     418 TO CORRECT-N.                                   ST1084.2
043000 SORT-WRITE-11.                                                   ST1084.2
043100     MOVE     "SORT-TEST-11" TO PAR-NAME.                         ST1084.2
043200     PERFORM  PRINT-DETAIL.                                       ST1084.2
043300 SORT-TEST-12.                                                    ST1084.2
043400     RETURN   SORTFILE-1H AT END GO TO RETURN-ERROR.              ST1084.2
043500     IF       SORTKEY-8 EQUAL TO -14                              ST1084.2
043600              PERFORM PASS GO TO SORT-WRITE-12.                   ST1084.2
043700 SORT-FAIL-12.                                                    ST1084.2
043800     PERFORM  FAIL.                                               ST1084.2
043900     MOVE     SORTKEY-8 TO COMPUTED-N.                            ST1084.2
044000     MOVE     -14 TO CORRECT-N.                                   ST1084.2
044100 SORT-WRITE-12.                                                   ST1084.2
044200     MOVE     "SORT-TEST-12" TO PAR-NAME.                         ST1084.2
044300     PERFORM  PRINT-DETAIL.                                       ST1084.2
044400 SORT-TEST-13.                                                    ST1084.2
044500     RETURN   SORTFILE-1H AT END GO TO RETURN-ERROR.              ST1084.2
044600     IF       SORTKEY-6 EQUAL TO "Z         "                     ST1084.2
044700              PERFORM PASS GO TO SORT-WRITE-13.                   ST1084.2
044800 SORT-FAIL-13.                                                    ST1084.2
044900     PERFORM  FAIL.                                               ST1084.2
045000     MOVE     SORTKEY-6 TO COMPUTED-A.                            ST1084.2
045100     MOVE     "Z         "           TO CORRECT-A.                ST1084.2
045200 SORT-WRITE-13.                                                   ST1084.2
045300     MOVE     "SORT-TEST-13" TO PAR-NAME.                         ST1084.2
045400     PERFORM  PRINT-DETAIL.                                       ST1084.2
045500 SORT-TEST-14.                                                    ST1084.2
045600     RETURN   SORTFILE-1H AT END GO TO RETURN-ERROR.              ST1084.2
045700     IF       SORTKEY-5 EQUAL TO "Z                   "           ST1084.2
045800              PERFORM PASS GO TO SORT-WRITE-14.                   ST1084.2
045900 SORT-FAIL-14.                                                    ST1084.2
046000     PERFORM  FAIL.                                               ST1084.2
046100     MOVE     SORTKEY-5 TO COMPUTED-A.                            ST1084.2
046200     MOVE     "Z                   " TO CORRECT-A.                ST1084.2
046300 SORT-WRITE-14.                                                   ST1084.2
046400     MOVE     "SORT-TEST-14" TO PAR-NAME.                         ST1084.2
046500     PERFORM  PRINT-DETAIL.                                       ST1084.2
046600 SORT-TEST-15.                                                    ST1084.2
046700     RETURN   SORTFILE-1H AT END GO TO RETURN-ERROR.              ST1084.2
046800     IF       SORTKEY-3 EQUAL TO +.6000000000000000               ST1084.2
046900              PERFORM PASS GO TO SORT-WRITE-15.                   ST1084.2
047000 SORT-FAIL-15.                                                    ST1084.2
047100     PERFORM  FAIL.                                               ST1084.2
047200     MOVE     SORTKEY-3 TO COMPUTED-0V18.                         ST1084.2
047300     MOVE     +.6000000000000000     TO CORRECT-0V18.             ST1084.2
047400 SORT-WRITE-15.                                                   ST1084.2
047500     MOVE     "SORT-TEST-15" TO PAR-NAME.                         ST1084.2
047600     PERFORM  PRINT-DETAIL.                                       ST1084.2
047700 SORT-TEST-16.                                                    ST1084.2
047800     RETURN   SORTFILE-1H AT END GO TO RETURN-ERROR.              ST1084.2
047900     IF       SORTKEY-1 EQUAL TO +123456                          ST1084.2
048000              PERFORM PASS GO TO SORT-WRITE-16.                   ST1084.2
048100 SORT-FAIL-16.                                                    ST1084.2
048200     PERFORM  FAIL.                                               ST1084.2
048300     MOVE     SORTKEY-1 TO COMPUTED-N.                            ST1084.2
048400     MOVE     +123456 TO CORRECT-N.                               ST1084.2
048500 SORT-WRITE-16.                                                   ST1084.2
048600     MOVE     "SORT-TEST-16" TO PAR-NAME.                         ST1084.2
048700     PERFORM  PRINT-DETAIL.                                       ST1084.2
048800 CONTINUE-TESTING SECTION.                                        ST1084.2
048900 SORT-TEST-17.                                                    ST1084.2
049000     RETURN   SORTFILE-1H AT END                                  ST1084.2
049100              PERFORM PASS GO TO SORT-WRITE-17.                   ST1084.2
049200 SORT-FAIL-17.                                                    ST1084.2
049300     MOVE     "END OF FILE NOT FOUND" TO RE-MARK.                 ST1084.2
049400     PERFORM  FAIL.                                               ST1084.2
049500 SORT-WRITE-17.                                                   ST1084.2
049600     MOVE     "SORT-TEST-17" TO PAR-NAME.                         ST1084.2
049700     PERFORM  PRINT-DETAIL.                                       ST1084.2
049800     GO       TO OUTPROC-EXIT.                                    ST1084.2
049900 RETURN-ERROR.                                                    ST1084.2
050000     MOVE     "RETURN-ERROR" TO PAR-NAME.                         ST1084.2
050100     PERFORM  FAIL.                                               ST1084.2
050200     MOVE     "EOF PREMATURELY FOUND" TO RE-MARK.                 ST1084.2
050300     PERFORM  PRINT-DETAIL.                                       ST1084.2
050400     GO TO CCVS1-EXIT.                                            ST1084.2
050500 CLOSE-FILES.                                                     ST1084.2
050600     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   ST1084.2
050700 TERMINATE-CCVS.                                                  ST1084.2
050800*    EXIT PROGRAM.                                                ST1084.2
050900*TERMINATE-CALL.                                                  ST1084.2
051000     STOP     RUN.                                                ST1084.2
051100 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         ST1084.2
051200 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           ST1084.2
051300 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          ST1084.2
051400 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      ST1084.2
051500     MOVE "****TEST DELETED****" TO RE-MARK.                      ST1084.2
051600 PRINT-DETAIL.                                                    ST1084.2
051700     IF REC-CT NOT EQUAL TO ZERO                                  ST1084.2
051800             MOVE "." TO PARDOT-X                                 ST1084.2
051900             MOVE REC-CT TO DOTVALUE.                             ST1084.2
052000     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      ST1084.2
052100     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               ST1084.2
052200        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 ST1084.2
052300          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 ST1084.2
052400     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              ST1084.2
052500     MOVE SPACE TO CORRECT-X.                                     ST1084.2
052600     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         ST1084.2
052700     MOVE     SPACE TO RE-MARK.                                   ST1084.2
052800 HEAD-ROUTINE.                                                    ST1084.2
052900     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  ST1084.2
053000     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  ST1084.2
053100     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  ST1084.2
053200     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  ST1084.2
053300 COLUMN-NAMES-ROUTINE.                                            ST1084.2
053400     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           ST1084.2
053500     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1084.2
053600     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        ST1084.2
053700 END-ROUTINE.                                                     ST1084.2
053800     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.ST1084.2
053900 END-RTN-EXIT.                                                    ST1084.2
054000     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1084.2
054100 END-ROUTINE-1.                                                   ST1084.2
054200      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      ST1084.2
054300      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               ST1084.2
054400      ADD PASS-COUNTER TO ERROR-HOLD.                             ST1084.2
054500*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   ST1084.2
054600      MOVE PASS-COUNTER TO CCVS-E-4-1.                            ST1084.2
054700      MOVE ERROR-HOLD TO CCVS-E-4-2.                              ST1084.2
054800      MOVE CCVS-E-4 TO CCVS-E-2-2.                                ST1084.2
054900      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           ST1084.2
055000  END-ROUTINE-12.                                                 ST1084.2
055100      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        ST1084.2
055200     IF       ERROR-COUNTER IS EQUAL TO ZERO                      ST1084.2
055300         MOVE "NO " TO ERROR-TOTAL                                ST1084.2
055400         ELSE                                                     ST1084.2
055500         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       ST1084.2
055600     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           ST1084.2
055700     PERFORM WRITE-LINE.                                          ST1084.2
055800 END-ROUTINE-13.                                                  ST1084.2
055900     IF DELETE-COUNTER IS EQUAL TO ZERO                           ST1084.2
056000         MOVE "NO " TO ERROR-TOTAL  ELSE                          ST1084.2
056100         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      ST1084.2
056200     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   ST1084.2
056300     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           ST1084.2
056400      IF   INSPECT-COUNTER EQUAL TO ZERO                          ST1084.2
056500          MOVE "NO " TO ERROR-TOTAL                               ST1084.2
056600      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   ST1084.2
056700      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            ST1084.2
056800      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          ST1084.2
056900     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           ST1084.2
057000 WRITE-LINE.                                                      ST1084.2
057100     ADD 1 TO RECORD-COUNT.                                       ST1084.2
057200*    IF RECORD-COUNT GREATER 42                                   ST1084.2
057300*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          ST1084.2
057400*        MOVE SPACE TO DUMMY-RECORD                               ST1084.2
057500*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  ST1084.2
057600*        MOVE CCVS-H-1  TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    ST1084.2
057700*        MOVE CCVS-H-2A TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    ST1084.2
057800*        MOVE CCVS-H-2B TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    ST1084.2
057900*        MOVE CCVS-H-3  TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    ST1084.2
058000*        MOVE CCVS-C-1  TO DUMMY-RECORD PERFORM WRT-LN            ST1084.2
058100*        MOVE CCVS-C-2  TO DUMMY-RECORD PERFORM WRT-LN            ST1084.2
058200*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          ST1084.2
058300*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          ST1084.2
058400*        MOVE ZERO TO RECORD-COUNT.                               ST1084.2
058500     PERFORM WRT-LN.                                              ST1084.2
058600 WRT-LN.                                                          ST1084.2
058700     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               ST1084.2
058800     MOVE SPACE TO DUMMY-RECORD.                                  ST1084.2
058900 BLANK-LINE-PRINT.                                                ST1084.2
059000     PERFORM WRT-LN.                                              ST1084.2
059100 FAIL-ROUTINE.                                                    ST1084.2
059200     IF     COMPUTED-X NOT EQUAL TO SPACE                         ST1084.2
059300            GO TO   FAIL-ROUTINE-WRITE.                           ST1084.2
059400     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.ST1084.2
059500     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 ST1084.2
059600     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   ST1084.2
059700     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1084.2
059800     MOVE   SPACES TO INF-ANSI-REFERENCE.                         ST1084.2
059900     GO TO  FAIL-ROUTINE-EX.                                      ST1084.2
060000 FAIL-ROUTINE-WRITE.                                              ST1084.2
060100     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         ST1084.2
060200     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 ST1084.2
060300     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. ST1084.2
060400     MOVE   SPACES TO COR-ANSI-REFERENCE.                         ST1084.2
060500 FAIL-ROUTINE-EX. EXIT.                                           ST1084.2
060600 BAIL-OUT.                                                        ST1084.2
060700     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   ST1084.2
060800     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           ST1084.2
060900 BAIL-OUT-WRITE.                                                  ST1084.2
061000     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  ST1084.2
061100     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 ST1084.2
061200     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1084.2
061300     MOVE   SPACES TO INF-ANSI-REFERENCE.                         ST1084.2
061400 BAIL-OUT-EX. EXIT.                                               ST1084.2
061500 CCVS1-EXIT.                                                      ST1084.2
061600     EXIT.                                                        ST1084.2
061700 OUTPROC-EXIT SECTION.                                            ST1084.2
061800 EXIT-ONLY.                                                       ST1084.2
061900     PERFORM  CLOSE-FILES.                                        ST1084.2
      *END-OF,ST108A                                                                  
