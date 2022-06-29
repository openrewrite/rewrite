      *HEADER,COBOL,ST118A                                                            
000100 IDENTIFICATION DIVISION.                                         ST1184.2
000200 PROGRAM-ID.                                                      ST1184.2
000300     ST118A.                                                      ST1184.2
000400****************************************************************  ST1184.2
000500*                                                              *  ST1184.2
000600*    VALIDATION FOR:-                                          *  ST1184.2
000700*                                                              *  ST1184.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1184.2
000900*                                                              *  ST1184.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".ST1184.2
001100*                                                              *  ST1184.2
001200****************************************************************  ST1184.2
001300*                                                              *  ST1184.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  ST1184.2
001500*                                                              *  ST1184.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  ST1184.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  ST1184.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  ST1184.2
001900*                                                              *  ST1184.2
002000****************************************************************  ST1184.2
002100*    ST118 IS A COMPLETELY SELF-CONTAINED PROGRAM.  THE INPUT     ST1184.2
002200*    PROCEDURE BUILDS THE EIGHT-RECORD FILE SHOWN BELOW. THE      ST1184.2
002300*    OUTPUT PROCEDURE CHECKS THE SORTED FILE AND GENERATES THE    ST1184.2
002400*    REPORT.                                                      ST1184.2
002500*    SORT    SORT    SORT    SORT    SORT   SORT     SORT    SORT ST1184.2
002600*    KEY-1   KEY-2   KEY-3   KEY-4   KEY-5  KEY-6    KEY-7   KEY-8ST1184.2
002700*    S9(6)   A(5)    SV9(16) X(10)   A(20)  X(10)    999     S99  ST1184.2
002800*    SIGN    JUST    SIGN    JUST                           SIGN  ST1184.2
002900*    LEADING RIGHT   TRAILIN RIGHT                          TRAIL ST1184.2
003000*    SEPARAT         SEPARAT                                      ST1184.2
003100*                                                                 ST1184.2
003200*    +123456     BBB -.1234   BBBBBB A      AAAAAAAA 501     +99  ST1184.2
003300*    -054321       X -.1234   BBBBBB A      AAAAAAAA 501     +99  ST1184.2
003400*    -054321     BBB +.6      BBBBBB A      AAAAAAAA 501     +99  ST1184.2
003500*    -054321     BBB -.1234        X A      AAAAAAAA 501     +99  ST1184.2
003600*    -054321     BBB -.1234   BBBBBB Z      AAAAAAAA 501     +99  ST1184.2
003700*    -054321     BBB -.1234   BBBBBB A      Z        501     +99  ST1184.2
003800*    -054321     BBB -.1234   BBBBBB A      AAAAAAAA 418     +99  ST1184.2
003900*    -054321     BBB -.1234   BBBBBB A      AAAAAAAA 501     -14  ST1184.2
004000*                                                                 ST1184.2
004100*    THIS PROGRAM CHECKS THE COMPILER"S ABILITY TO HANDLE EIGHT   ST1184.2
004200*    ASCENDING KEYS IN ONE FILE.                                  ST1184.2
004300*                                                                 ST1184.2
004400*    ASCENDING KEYS IN ONE FILE.  EACH OF THE KEYS IDENTIFIED     ST1184.2
004500*    IN THE SORT STATEMENT ARE ELEMENTARY DATA ITEMS AND USE      ST1184.2
004600*    VARIOUS COMBINATIONS OF PICTURE CHARACTER-STRING SYMBOLS AND ST1184.2
004700*    CLAUSES FOR DESCRIBING THE GENERAL CHARACTERISTICS OF THE    ST1184.2
004800*    DATA ITEM.                                                   ST1184.2
004900*    THIS PROGRAM IS A REWRITE OF ST108.  THE PURPOSE OF THIS     ST1184.2
005000*    PROGRAM IS TO VERIFY THAT RECORDS ARE PROPERLY SORTED WHEN   ST1184.2
005100*    THE SORT KEYS OF THE SORT STATEMENT USE DATA DEFINITIONS     ST1184.2
005200*    WHICH INCLUDE THE SIGN CLAUSE.                               ST1184.2
005300 ENVIRONMENT DIVISION.                                            ST1184.2
005400 CONFIGURATION SECTION.                                           ST1184.2
005500 SOURCE-COMPUTER.                                                 ST1184.2
005600     XXXXX082.                                                    ST1184.2
005700 OBJECT-COMPUTER.                                                 ST1184.2
005800     XXXXX083.                                                    ST1184.2
005900 INPUT-OUTPUT SECTION.                                            ST1184.2
006000 FILE-CONTROL.                                                    ST1184.2
006100     SELECT PRINT-FILE ASSIGN TO                                  ST1184.2
006200     XXXXX055.                                                    ST1184.2
006300     SELECT   SORTFILE-1H ASSIGN TO                               ST1184.2
006400     XXXXX027.                                                    ST1184.2
006500 DATA DIVISION.                                                   ST1184.2
006600 FILE SECTION.                                                    ST1184.2
006700 FD  PRINT-FILE.                                                  ST1184.2
006800 01  PRINT-REC PICTURE X(120).                                    ST1184.2
006900 01  DUMMY-RECORD PICTURE X(120).                                 ST1184.2
007000 SD  SORTFILE-1H                                                  ST1184.2
007100     DATA RECORD IS SORTFILE-REC.                                 ST1184.2
007200 01  SORTFILE-REC.                                                ST1184.2
007300     02 SORTKEY-8       PICTURE S99 SIGN IS TRAILING.             ST1184.2
007400     02 SORTKEY-1       PICTURE S9(6)  SIGN IS LEADING SEPARATE.  ST1184.2
007500     02 SORTKEY-7       PICTURE 999.                              ST1184.2
007600     02 SORTKEY-3   PICTURE SV9(16)  SIGN IS TRAILING SEPARATE.   ST1184.2
007700     02 FILLER          PICTURE XX.                               ST1184.2
007800     02 SORTKEY-4       PICTURE X(10) JUSTIFIED RIGHT.            ST1184.2
007900     02 SORTKEY-6       PICTURE X(10).                            ST1184.2
008000     02 SORTKEY-2       PICTURE A(05) JUSTIFIED RIGHT.            ST1184.2
008100     02 SORTKEY-5       PICTURE A(20).                            ST1184.2
008200     02 FILLER          PICTURE XXX.                              ST1184.2
008300 WORKING-STORAGE SECTION.                                         ST1184.2
008400 77  UTIL-CTR           PICTURE S99999.                           ST1184.2
008500 77  SPAC-E             PICTURE X VALUE " ".                      ST1184.2
008600 01  TEST-RESULTS.                                                ST1184.2
008700     02 FILLER                   PIC X      VALUE SPACE.          ST1184.2
008800     02 FEATURE                  PIC X(20)  VALUE SPACE.          ST1184.2
008900     02 FILLER                   PIC X      VALUE SPACE.          ST1184.2
009000     02 P-OR-F                   PIC X(5)   VALUE SPACE.          ST1184.2
009100     02 FILLER                   PIC X      VALUE SPACE.          ST1184.2
009200     02  PAR-NAME.                                                ST1184.2
009300       03 FILLER                 PIC X(19)  VALUE SPACE.          ST1184.2
009400       03  PARDOT-X              PIC X      VALUE SPACE.          ST1184.2
009500       03 DOTVALUE               PIC 99     VALUE ZERO.           ST1184.2
009600     02 FILLER                   PIC X(8)   VALUE SPACE.          ST1184.2
009700     02 RE-MARK                  PIC X(61).                       ST1184.2
009800 01  TEST-COMPUTED.                                               ST1184.2
009900     02 FILLER                   PIC X(30)  VALUE SPACE.          ST1184.2
010000     02 FILLER                   PIC X(17)  VALUE                 ST1184.2
010100            "       COMPUTED=".                                   ST1184.2
010200     02 COMPUTED-X.                                               ST1184.2
010300     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          ST1184.2
010400     03 COMPUTED-N               REDEFINES COMPUTED-A             ST1184.2
010500                                 PIC -9(9).9(9).                  ST1184.2
010600     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         ST1184.2
010700     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     ST1184.2
010800     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     ST1184.2
010900     03       CM-18V0 REDEFINES COMPUTED-A.                       ST1184.2
011000         04 COMPUTED-18V0                    PIC -9(18).          ST1184.2
011100         04 FILLER                           PIC X.               ST1184.2
011200     03 FILLER PIC X(50) VALUE SPACE.                             ST1184.2
011300 01  TEST-CORRECT.                                                ST1184.2
011400     02 FILLER PIC X(30) VALUE SPACE.                             ST1184.2
011500     02 FILLER PIC X(17) VALUE "       CORRECT =".                ST1184.2
011600     02 CORRECT-X.                                                ST1184.2
011700     03 CORRECT-A                  PIC X(20) VALUE SPACE.         ST1184.2
011800     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      ST1184.2
011900     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         ST1184.2
012000     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     ST1184.2
012100     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     ST1184.2
012200     03      CR-18V0 REDEFINES CORRECT-A.                         ST1184.2
012300         04 CORRECT-18V0                     PIC -9(18).          ST1184.2
012400         04 FILLER                           PIC X.               ST1184.2
012500     03 FILLER PIC X(2) VALUE SPACE.                              ST1184.2
012600     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     ST1184.2
012700 01  CCVS-C-1.                                                    ST1184.2
012800     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAST1184.2
012900-    "SS  PARAGRAPH-NAME                                          ST1184.2
013000-    "       REMARKS".                                            ST1184.2
013100     02 FILLER                     PIC X(20)    VALUE SPACE.      ST1184.2
013200 01  CCVS-C-2.                                                    ST1184.2
013300     02 FILLER                     PIC X        VALUE SPACE.      ST1184.2
013400     02 FILLER                     PIC X(6)     VALUE "TESTED".   ST1184.2
013500     02 FILLER                     PIC X(15)    VALUE SPACE.      ST1184.2
013600     02 FILLER                     PIC X(4)     VALUE "FAIL".     ST1184.2
013700     02 FILLER                     PIC X(94)    VALUE SPACE.      ST1184.2
013800 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       ST1184.2
013900 01  REC-CT                        PIC 99       VALUE ZERO.       ST1184.2
014000 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       ST1184.2
014100 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       ST1184.2
014200 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       ST1184.2
014300 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       ST1184.2
014400 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       ST1184.2
014500 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       ST1184.2
014600 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      ST1184.2
014700 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       ST1184.2
014800 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     ST1184.2
014900 01  CCVS-H-1.                                                    ST1184.2
015000     02  FILLER                    PIC X(39)    VALUE SPACES.     ST1184.2
015100     02  FILLER                    PIC X(42)    VALUE             ST1184.2
015200     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 ST1184.2
015300     02  FILLER                    PIC X(39)    VALUE SPACES.     ST1184.2
015400 01  CCVS-H-2A.                                                   ST1184.2
015500   02  FILLER                        PIC X(40)  VALUE SPACE.      ST1184.2
015600   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  ST1184.2
015700   02  FILLER                        PIC XXXX   VALUE             ST1184.2
015800     "4.2 ".                                                      ST1184.2
015900   02  FILLER                        PIC X(28)  VALUE             ST1184.2
016000            " COPY - NOT FOR DISTRIBUTION".                       ST1184.2
016100   02  FILLER                        PIC X(41)  VALUE SPACE.      ST1184.2
016200                                                                  ST1184.2
016300 01  CCVS-H-2B.                                                   ST1184.2
016400   02  FILLER                        PIC X(15)  VALUE             ST1184.2
016500            "TEST RESULT OF ".                                    ST1184.2
016600   02  TEST-ID                       PIC X(9).                    ST1184.2
016700   02  FILLER                        PIC X(4)   VALUE             ST1184.2
016800            " IN ".                                               ST1184.2
016900   02  FILLER                        PIC X(12)  VALUE             ST1184.2
017000     " HIGH       ".                                              ST1184.2
017100   02  FILLER                        PIC X(22)  VALUE             ST1184.2
017200            " LEVEL VALIDATION FOR ".                             ST1184.2
017300   02  FILLER                        PIC X(58)  VALUE             ST1184.2
017400     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1184.2
017500 01  CCVS-H-3.                                                    ST1184.2
017600     02  FILLER                      PIC X(34)  VALUE             ST1184.2
017700            " FOR OFFICIAL USE ONLY    ".                         ST1184.2
017800     02  FILLER                      PIC X(58)  VALUE             ST1184.2
017900     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".ST1184.2
018000     02  FILLER                      PIC X(28)  VALUE             ST1184.2
018100            "  COPYRIGHT   1985 ".                                ST1184.2
018200 01  CCVS-E-1.                                                    ST1184.2
018300     02 FILLER                       PIC X(52)  VALUE SPACE.      ST1184.2
018400     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              ST1184.2
018500     02 ID-AGAIN                     PIC X(9).                    ST1184.2
018600     02 FILLER                       PIC X(45)  VALUE SPACES.     ST1184.2
018700 01  CCVS-E-2.                                                    ST1184.2
018800     02  FILLER                      PIC X(31)  VALUE SPACE.      ST1184.2
018900     02  FILLER                      PIC X(21)  VALUE SPACE.      ST1184.2
019000     02 CCVS-E-2-2.                                               ST1184.2
019100         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      ST1184.2
019200         03 FILLER                   PIC X      VALUE SPACE.      ST1184.2
019300         03 ENDER-DESC               PIC X(44)  VALUE             ST1184.2
019400            "ERRORS ENCOUNTERED".                                 ST1184.2
019500 01  CCVS-E-3.                                                    ST1184.2
019600     02  FILLER                      PIC X(22)  VALUE             ST1184.2
019700            " FOR OFFICIAL USE ONLY".                             ST1184.2
019800     02  FILLER                      PIC X(12)  VALUE SPACE.      ST1184.2
019900     02  FILLER                      PIC X(58)  VALUE             ST1184.2
020000     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1184.2
020100     02  FILLER                      PIC X(13)  VALUE SPACE.      ST1184.2
020200     02 FILLER                       PIC X(15)  VALUE             ST1184.2
020300             " COPYRIGHT 1985".                                   ST1184.2
020400 01  CCVS-E-4.                                                    ST1184.2
020500     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      ST1184.2
020600     02 FILLER                       PIC X(4)   VALUE " OF ".     ST1184.2
020700     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      ST1184.2
020800     02 FILLER                       PIC X(40)  VALUE             ST1184.2
020900      "  TESTS WERE EXECUTED SUCCESSFULLY".                       ST1184.2
021000 01  XXINFO.                                                      ST1184.2
021100     02 FILLER                       PIC X(19)  VALUE             ST1184.2
021200            "*** INFORMATION ***".                                ST1184.2
021300     02 INFO-TEXT.                                                ST1184.2
021400       04 FILLER                     PIC X(8)   VALUE SPACE.      ST1184.2
021500       04 XXCOMPUTED                 PIC X(20).                   ST1184.2
021600       04 FILLER                     PIC X(5)   VALUE SPACE.      ST1184.2
021700       04 XXCORRECT                  PIC X(20).                   ST1184.2
021800     02 INF-ANSI-REFERENCE           PIC X(48).                   ST1184.2
021900 01  HYPHEN-LINE.                                                 ST1184.2
022000     02 FILLER  PIC IS X VALUE IS SPACE.                          ST1184.2
022100     02 FILLER  PIC IS X(65)    VALUE IS "************************ST1184.2
022200-    "*****************************************".                 ST1184.2
022300     02 FILLER  PIC IS X(54)    VALUE IS "************************ST1184.2
022400-    "******************************".                            ST1184.2
022500 01  CCVS-PGM-ID                     PIC X(9)   VALUE             ST1184.2
022600     "ST118A".                                                    ST1184.2
022700 PROCEDURE DIVISION.                                              ST1184.2
022800 SORT-PARA SECTION.                                               ST1184.2
022900 SORT-PARAGRAPH.                                                  ST1184.2
023000     SORT     SORTFILE-1H ON                                      ST1184.2
023100              ASCENDING KEY SORTKEY-1                             ST1184.2
023200              ASCENDING SORTKEY-2                                 ST1184.2
023300              ASCENDING SORTKEY-3                                 ST1184.2
023400              ASCENDING SORTKEY-4                                 ST1184.2
023500              ASCENDING SORTKEY-5                                 ST1184.2
023600              ASCENDING SORTKEY-6                                 ST1184.2
023700              ASCENDING SORTKEY-7                                 ST1184.2
023800              ASCENDING SORTKEY-8                                 ST1184.2
023900              INPUT PROCEDURE INPROC                              ST1184.2
024000              OUTPUT PROCEDURE OUTPROC THRU OUTPROC-EXIT.         ST1184.2
024100     STOP     RUN.                                                ST1184.2
024200 INPROC SECTION.                                                  ST1184.2
024300 BUILD-FILE.                                                      ST1184.2
024400     PERFORM  BUILD-RECORD.                                       ST1184.2
024500     MOVE     +123456             TO SORTKEY-1.                   ST1184.2
024600     PERFORM  RELEASE-RECORD.                                     ST1184.2
024700     PERFORM  BUILD-RECORD.                                       ST1184.2
024800     MOVE     "X"                 TO SORTKEY-2.                   ST1184.2
024900     PERFORM  RELEASE-RECORD.                                     ST1184.2
025000     PERFORM  BUILD-RECORD.                                       ST1184.2
025100     MOVE     +.6                 TO SORTKEY-3.                   ST1184.2
025200     PERFORM  RELEASE-RECORD.                                     ST1184.2
025300     PERFORM  BUILD-RECORD.                                       ST1184.2
025400     MOVE     "X"                 TO SORTKEY-4.                   ST1184.2
025500     PERFORM  RELEASE-RECORD.                                     ST1184.2
025600     PERFORM  BUILD-RECORD.                                       ST1184.2
025700     MOVE     "Z"                 TO SORTKEY-5.                   ST1184.2
025800     PERFORM  RELEASE-RECORD.                                     ST1184.2
025900     PERFORM  BUILD-RECORD.                                       ST1184.2
026000     MOVE     "Z"                 TO SORTKEY-6.                   ST1184.2
026100     PERFORM  RELEASE-RECORD.                                     ST1184.2
026200     PERFORM  BUILD-RECORD.                                       ST1184.2
026300     MOVE     +418                TO SORTKEY-7.                   ST1184.2
026400     PERFORM  RELEASE-RECORD.                                     ST1184.2
026500     PERFORM  BUILD-RECORD.                                       ST1184.2
026600     MOVE     -14                 TO SORTKEY-8.                   ST1184.2
026700     PERFORM  RELEASE-RECORD.                                     ST1184.2
026800     GO       TO BUILD-EXIT.                                      ST1184.2
026900 BUILD-RECORD.                                                    ST1184.2
027000     MOVE     -054321             TO SORTKEY-1.                   ST1184.2
027100     MOVE     "BBB"               TO SORTKEY-2.                   ST1184.2
027200     MOVE     -.1234567890123456  TO SORTKEY-3.                   ST1184.2
027300     MOVE     "BBBBBB"            TO SORTKEY-4.                   ST1184.2
027400     MOVE     "A"                 TO SORTKEY-5.                   ST1184.2
027500     MOVE     "AAAAAAAA"          TO SORTKEY-6.                   ST1184.2
027600     MOVE     -501                TO SORTKEY-7.                   ST1184.2
027700*       NOTE THIS ITEM IS INTENTIONALLY MOVED TO AN UNSIGNED      ST1184.2
027800*             FIELD.                                              ST1184.2
027900     MOVE     +99                 TO SORTKEY-8.                   ST1184.2
028000 RELEASE-RECORD.                                                  ST1184.2
028100     RELEASE  SORTFILE-REC.                                       ST1184.2
028200 BUILD-EXIT.                                                      ST1184.2
028300     EXIT.                                                        ST1184.2
028400 OUTPROC SECTION.                                                 ST1184.2
028500 OPEN-FILES.                                                      ST1184.2
028600     OPEN     OUTPUT PRINT-FILE.                                  ST1184.2
028700     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   ST1184.2
028800     MOVE    SPACE TO TEST-RESULTS.                               ST1184.2
028900     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             ST1184.2
029000     IF       SPAC-E IS LESS THAN "B"                             ST1184.2
029100              GO TO SPACE-IS-LESS-THAN-B.                         ST1184.2
029200 B-IS-LESS-THAN-SPACE SECTION.                                    ST1184.2
029300 SORT-INIT-A.                                                     ST1184.2
029400     MOVE     "SORT - 8 ASC. KEYS" TO FEATURE.                    ST1184.2
029500*        NOTE THE RECORDS SHOULD BE SORTED INTO THE FOLLOWING     ST1184.2
029600*             ORDER --- 8 7 6 5 4 3 1 2 --- THAT IS,              ST1184.2
029700*             THE 8TH RECORD SORTS UP TO THE 1ST POSITION,        ST1184.2
029800*             THE 7TH RECORD SORTS UP TO THE 2ND POSITION, ETC.   ST1184.2
029900 SORT-TEST-1.                                                     ST1184.2
030000     RETURN   SORTFILE-1H AT END GO TO RETURN-ERROR.              ST1184.2
030100     IF       SORTKEY-7 EQUAL TO 418                              ST1184.2
030200              PERFORM PASS GO TO SORT-WRITE-1.                    ST1184.2
030300 SORT-FAIL-1.                                                     ST1184.2
030400     PERFORM  FAIL.                                               ST1184.2
030500     MOVE     SORTKEY-7 TO COMPUTED-N.                            ST1184.2
030600     MOVE     418 TO CORRECT-N.                                   ST1184.2
030700 SORT-WRITE-1.                                                    ST1184.2
030800     MOVE     "SORT-TEST-1 " TO PAR-NAME.                         ST1184.2
030900     PERFORM  PRINT-DETAIL.                                       ST1184.2
031000 SORT-TEST-2.                                                     ST1184.2
031100     RETURN   SORTFILE-1H AT END GO TO RETURN-ERROR.              ST1184.2
031200     IF       SORTKEY-8 EQUAL TO -14                              ST1184.2
031300              PERFORM PASS GO TO SORT-WRITE-2.                    ST1184.2
031400 SORT-FAIL-2.                                                     ST1184.2
031500     PERFORM  FAIL.                                               ST1184.2
031600     MOVE     SORTKEY-8 TO COMPUTED-N.                            ST1184.2
031700     MOVE     -14 TO CORRECT-N.                                   ST1184.2
031800 SORT-WRITE-2.                                                    ST1184.2
031900     MOVE     "SORT-TEST-2 " TO PAR-NAME.                         ST1184.2
032000     PERFORM  PRINT-DETAIL.                                       ST1184.2
032100 SORT-TEST-3.                                                     ST1184.2
032200     RETURN   SORTFILE-1H AT END GO TO RETURN-ERROR.              ST1184.2
032300     IF       SORTKEY-6 EQUAL TO "Z         "                     ST1184.2
032400              PERFORM PASS GO TO SORT-WRITE-3.                    ST1184.2
032500 SORT-FAIL-3.                                                     ST1184.2
032600     PERFORM  FAIL.                                               ST1184.2
032700     MOVE     SORTKEY-6 TO COMPUTED-A.                            ST1184.2
032800     MOVE     "Z         "           TO CORRECT-A.                ST1184.2
032900 SORT-WRITE-3.                                                    ST1184.2
033000     MOVE     "SORT-TEST-3 " TO PAR-NAME.                         ST1184.2
033100     PERFORM  PRINT-DETAIL.                                       ST1184.2
033200 SORT-TEST-4.                                                     ST1184.2
033300     RETURN   SORTFILE-1H AT END GO TO RETURN-ERROR.              ST1184.2
033400     IF       SORTKEY-5 EQUAL TO "Z                   "           ST1184.2
033500              PERFORM PASS GO TO SORT-WRITE-4.                    ST1184.2
033600 SORT-FAIL-4.                                                     ST1184.2
033700     PERFORM  FAIL.                                               ST1184.2
033800     MOVE     SORTKEY-5 TO COMPUTED-A.                            ST1184.2
033900     MOVE     "Z                   " TO CORRECT-A.                ST1184.2
034000 SORT-WRITE-4.                                                    ST1184.2
034100     MOVE     "SORT-TEST-4 " TO PAR-NAME.                         ST1184.2
034200     PERFORM  PRINT-DETAIL.                                       ST1184.2
034300 SORT-TEST-5.                                                     ST1184.2
034400     RETURN   SORTFILE-1H AT END GO TO RETURN-ERROR.              ST1184.2
034500     IF       SORTKEY-4 EQUAL TO "         X"                     ST1184.2
034600              PERFORM PASS GO TO SORT-WRITE-5.                    ST1184.2
034700 SORT-FAIL-5.                                                     ST1184.2
034800     PERFORM  FAIL.                                               ST1184.2
034900     MOVE     SORTKEY-4 TO COMPUTED-A.                            ST1184.2
035000     MOVE     "         X"           TO CORRECT-A.                ST1184.2
035100 SORT-WRITE-5.                                                    ST1184.2
035200     MOVE     "SORT-TEST-5 " TO PAR-NAME.                         ST1184.2
035300     PERFORM  PRINT-DETAIL.                                       ST1184.2
035400 SORT-TEST-6.                                                     ST1184.2
035500     RETURN   SORTFILE-1H AT END GO TO RETURN-ERROR.              ST1184.2
035600     IF       SORTKEY-3 EQUAL TO +.6000000000000000               ST1184.2
035700              PERFORM PASS GO TO SORT-WRITE-6.                    ST1184.2
035800 SORT-FAIL-6.                                                     ST1184.2
035900     PERFORM  FAIL.                                               ST1184.2
036000     MOVE     SORTKEY-3 TO COMPUTED-0V18.                         ST1184.2
036100     MOVE     +.6000000000000000     TO CORRECT-0V18.             ST1184.2
036200 SORT-WRITE-6.                                                    ST1184.2
036300     MOVE     "SORT-TEST-6 " TO PAR-NAME.                         ST1184.2
036400     PERFORM  PRINT-DETAIL.                                       ST1184.2
036500 SORT-TEST-7.                                                     ST1184.2
036600     RETURN   SORTFILE-1H AT END GO TO RETURN-ERROR.              ST1184.2
036700     IF       SORTKEY-2 EQUAL TO "    X"                          ST1184.2
036800              PERFORM PASS GO TO SORT-WRITE-7.                    ST1184.2
036900 SORT-FAIL-7.                                                     ST1184.2
037000     PERFORM  FAIL.                                               ST1184.2
037100     MOVE     SORTKEY-2 TO COMPUTED-A.                            ST1184.2
037200     MOVE     "    X" TO CORRECT-A.                               ST1184.2
037300 SORT-WRITE-7.                                                    ST1184.2
037400     MOVE     "SORT-TEST-7 " TO PAR-NAME.                         ST1184.2
037500     PERFORM  PRINT-DETAIL.                                       ST1184.2
037600 SORT-TEST-8.                                                     ST1184.2
037700     RETURN   SORTFILE-1H AT END GO TO RETURN-ERROR.              ST1184.2
037800     IF       SORTKEY-1 EQUAL TO +123456                          ST1184.2
037900              PERFORM PASS GO TO SORT-WRITE-8.                    ST1184.2
038000 SORT-FAIL-8.                                                     ST1184.2
038100     PERFORM  FAIL.                                               ST1184.2
038200     MOVE     SORTKEY-1 TO COMPUTED-N.                            ST1184.2
038300     MOVE     +123456 TO CORRECT-N.                               ST1184.2
038400 SORT-WRITE-8.                                                    ST1184.2
038500     MOVE     "SORT-TEST-8 " TO PAR-NAME.                         ST1184.2
038600     PERFORM  PRINT-DETAIL.                                       ST1184.2
038700 SORT-REMARK-A.                                                   ST1184.2
038800     MOVE     SPACE TO FEATURE.                                   ST1184.2
038900     MOVE     "THE COLLATING SEQUENCE" TO RE-MARK.                ST1184.2
039000     PERFORM  PRINT-DETAIL.                                       ST1184.2
039100     MOVE     "RENDERS TESTS 9 THRU 16" TO RE-MARK.               ST1184.2
039200     PERFORM  PRINT-DETAIL.                                       ST1184.2
039300     MOVE     "UNNECESSARY." TO RE-MARK.                          ST1184.2
039400     PERFORM  PRINT-DETAIL.                                       ST1184.2
039500     MOVE     "SORT - 8 ASC. KEYS" TO FEATURE.                    ST1184.2
039600     GO       TO CONTINUE-TESTING.                                ST1184.2
039700 SPACE-IS-LESS-THAN-B SECTION.                                    ST1184.2
039800 SORT-REMARK-B.                                                   ST1184.2
039900     MOVE     "THE COLLATING SEQUENCE" TO RE-MARK.                ST1184.2
040000     PERFORM  PRINT-DETAIL.                                       ST1184.2
040100     MOVE     "RENDERS TESTS 1 THRU 8" TO RE-MARK.                ST1184.2
040200     PERFORM  PRINT-DETAIL.                                       ST1184.2
040300     MOVE     "UNNECESSARY." TO RE-MARK.                          ST1184.2
040400     PERFORM  PRINT-DETAIL.                                       ST1184.2
040500     MOVE     "SORT - 8 ASC. KEYS" TO FEATURE.                    ST1184.2
040600*        NOTE THE RECORDS SHOULD BE SORTED INTO THE FOLLOWING     ST1184.2
040700*             ORDER --- 8 1 7 2 6 5 3 4 --- THAT IS,              ST1184.2
040800*             THE 1ST RECORD IS SORTED DOWN TO THE 8TH POSITION,  ST1184.2
040900*             THE 2ND RECORD SORTS UP TO THE 1ST POSITION, ETC.   ST1184.2
041000 SORT-TEST-9.                                                     ST1184.2
041100     RETURN   SORTFILE-1H AT END GO TO RETURN-ERROR.              ST1184.2
041200     IF       SORTKEY-2 EQUAL TO "    X"                          ST1184.2
041300              PERFORM PASS GO TO SORT-WRITE-9.                    ST1184.2
041400 SORT-FAIL-9.                                                     ST1184.2
041500     PERFORM  FAIL.                                               ST1184.2
041600     MOVE     SORTKEY-2 TO COMPUTED-A.                            ST1184.2
041700     MOVE     "    X" TO CORRECT-A.                               ST1184.2
041800 SORT-WRITE-9.                                                    ST1184.2
041900     MOVE     "SORT-TEST-9 " TO PAR-NAME.                         ST1184.2
042000     PERFORM  PRINT-DETAIL.                                       ST1184.2
042100 SORT-TEST-10.                                                    ST1184.2
042200     RETURN   SORTFILE-1H AT END GO TO RETURN-ERROR.              ST1184.2
042300     IF       SORTKEY-4 EQUAL TO "         X"                     ST1184.2
042400              PERFORM PASS GO TO SORT-WRITE-10.                   ST1184.2
042500 SORT-FAIL-10.                                                    ST1184.2
042600     PERFORM  FAIL.                                               ST1184.2
042700     MOVE     SORTKEY-4 TO COMPUTED-A.                            ST1184.2
042800     MOVE     "         X" TO CORRECT-A.                          ST1184.2
042900 SORT-WRITE-10.                                                   ST1184.2
043000     MOVE     "SORT-TEST-10" TO PAR-NAME.                         ST1184.2
043100     PERFORM  PRINT-DETAIL.                                       ST1184.2
043200 SORT-TEST-11.                                                    ST1184.2
043300     RETURN   SORTFILE-1H AT END GO TO RETURN-ERROR.              ST1184.2
043400     IF       SORTKEY-7 EQUAL TO 418                              ST1184.2
043500              PERFORM PASS GO TO SORT-WRITE-11.                   ST1184.2
043600 SORT-FAIL-11.                                                    ST1184.2
043700     PERFORM  FAIL.                                               ST1184.2
043800     MOVE     SORTKEY-7 TO COMPUTED-N                             ST1184.2
043900     MOVE     418 TO CORRECT-N.                                   ST1184.2
044000 SORT-WRITE-11.                                                   ST1184.2
044100     MOVE     "SORT-TEST-11" TO PAR-NAME.                         ST1184.2
044200     PERFORM  PRINT-DETAIL.                                       ST1184.2
044300 SORT-TEST-12.                                                    ST1184.2
044400     RETURN   SORTFILE-1H AT END GO TO RETURN-ERROR.              ST1184.2
044500     IF       SORTKEY-8 EQUAL TO -14                              ST1184.2
044600              PERFORM PASS GO TO SORT-WRITE-12.                   ST1184.2
044700 SORT-FAIL-12.                                                    ST1184.2
044800     PERFORM  FAIL.                                               ST1184.2
044900     MOVE     SORTKEY-8 TO COMPUTED-N.                            ST1184.2
045000     MOVE     -14 TO CORRECT-N.                                   ST1184.2
045100 SORT-WRITE-12.                                                   ST1184.2
045200     MOVE     "SORT-TEST-12" TO PAR-NAME.                         ST1184.2
045300     PERFORM  PRINT-DETAIL.                                       ST1184.2
045400 SORT-TEST-13.                                                    ST1184.2
045500     RETURN   SORTFILE-1H AT END GO TO RETURN-ERROR.              ST1184.2
045600     IF       SORTKEY-6 EQUAL TO "Z         "                     ST1184.2
045700              PERFORM PASS GO TO SORT-WRITE-13.                   ST1184.2
045800 SORT-FAIL-13.                                                    ST1184.2
045900     PERFORM  FAIL.                                               ST1184.2
046000     MOVE     SORTKEY-6 TO COMPUTED-A.                            ST1184.2
046100     MOVE     "Z         "           TO CORRECT-A.                ST1184.2
046200 SORT-WRITE-13.                                                   ST1184.2
046300     MOVE     "SORT-TEST-13" TO PAR-NAME.                         ST1184.2
046400     PERFORM  PRINT-DETAIL.                                       ST1184.2
046500 SORT-TEST-14.                                                    ST1184.2
046600     RETURN   SORTFILE-1H AT END GO TO RETURN-ERROR.              ST1184.2
046700     IF       SORTKEY-5 EQUAL TO "Z                   "           ST1184.2
046800              PERFORM PASS GO TO SORT-WRITE-14.                   ST1184.2
046900 SORT-FAIL-14.                                                    ST1184.2
047000     PERFORM  FAIL.                                               ST1184.2
047100     MOVE     SORTKEY-5 TO COMPUTED-A.                            ST1184.2
047200     MOVE     "Z                   " TO CORRECT-A.                ST1184.2
047300 SORT-WRITE-14.                                                   ST1184.2
047400     MOVE     "SORT-TEST-14" TO PAR-NAME.                         ST1184.2
047500     PERFORM  PRINT-DETAIL.                                       ST1184.2
047600 SORT-TEST-15.                                                    ST1184.2
047700     RETURN   SORTFILE-1H AT END GO TO RETURN-ERROR.              ST1184.2
047800     IF       SORTKEY-3 EQUAL TO +.6000000000000000               ST1184.2
047900              PERFORM PASS GO TO SORT-WRITE-15.                   ST1184.2
048000 SORT-FAIL-15.                                                    ST1184.2
048100     PERFORM  FAIL.                                               ST1184.2
048200     MOVE     SORTKEY-3 TO COMPUTED-0V18.                         ST1184.2
048300     MOVE     +.6000000000000000     TO CORRECT-0V18.             ST1184.2
048400 SORT-WRITE-15.                                                   ST1184.2
048500     MOVE     "SORT-TEST-15" TO PAR-NAME.                         ST1184.2
048600     PERFORM  PRINT-DETAIL.                                       ST1184.2
048700 SORT-TEST-16.                                                    ST1184.2
048800     RETURN   SORTFILE-1H AT END GO TO RETURN-ERROR.              ST1184.2
048900     IF       SORTKEY-1 EQUAL TO +123456                          ST1184.2
049000              PERFORM PASS GO TO SORT-WRITE-16.                   ST1184.2
049100 SORT-FAIL-16.                                                    ST1184.2
049200     PERFORM  FAIL.                                               ST1184.2
049300     MOVE     SORTKEY-1 TO COMPUTED-N.                            ST1184.2
049400     MOVE     +123456 TO CORRECT-N.                               ST1184.2
049500 SORT-WRITE-16.                                                   ST1184.2
049600     MOVE     "SORT-TEST-16" TO PAR-NAME.                         ST1184.2
049700     PERFORM  PRINT-DETAIL.                                       ST1184.2
049800 CONTINUE-TESTING SECTION.                                        ST1184.2
049900 SORT-TEST-17.                                                    ST1184.2
050000     RETURN   SORTFILE-1H AT END                                  ST1184.2
050100              PERFORM PASS GO TO SORT-WRITE-17.                   ST1184.2
050200 SORT-FAIL-17.                                                    ST1184.2
050300     MOVE     "END OF FILE NOT FOUND" TO RE-MARK.                 ST1184.2
050400     PERFORM  FAIL.                                               ST1184.2
050500 SORT-WRITE-17.                                                   ST1184.2
050600     MOVE     "SORT-TEST-17" TO PAR-NAME.                         ST1184.2
050700     PERFORM  PRINT-DETAIL.                                       ST1184.2
050800     GO       TO OUTPROC-EXIT.                                    ST1184.2
050900 RETURN-ERROR.                                                    ST1184.2
051000     MOVE     "RETURN-ERROR" TO PAR-NAME.                         ST1184.2
051100     PERFORM  FAIL.                                               ST1184.2
051200     MOVE     "EOF PREMATURELY FOUND" TO RE-MARK.                 ST1184.2
051300     PERFORM  PRINT-DETAIL.                                       ST1184.2
051400     GO TO CCVS1-EXIT.                                            ST1184.2
051500 CLOSE-FILES.                                                     ST1184.2
051600     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   ST1184.2
051700 TERMINATE-CCVS.                                                  ST1184.2
051800*    EXIT PROGRAM.                                                ST1184.2
051900*TERMINATE-CALL.                                                  ST1184.2
052000     STOP     RUN.                                                ST1184.2
052100 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         ST1184.2
052200 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           ST1184.2
052300 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          ST1184.2
052400 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      ST1184.2
052500     MOVE "****TEST DELETED****" TO RE-MARK.                      ST1184.2
052600 PRINT-DETAIL.                                                    ST1184.2
052700     IF REC-CT NOT EQUAL TO ZERO                                  ST1184.2
052800             MOVE "." TO PARDOT-X                                 ST1184.2
052900             MOVE REC-CT TO DOTVALUE.                             ST1184.2
053000     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      ST1184.2
053100     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               ST1184.2
053200        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 ST1184.2
053300          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 ST1184.2
053400     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              ST1184.2
053500     MOVE SPACE TO CORRECT-X.                                     ST1184.2
053600     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         ST1184.2
053700     MOVE     SPACE TO RE-MARK.                                   ST1184.2
053800 HEAD-ROUTINE.                                                    ST1184.2
053900     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  ST1184.2
054000     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  ST1184.2
054100     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  ST1184.2
054200     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  ST1184.2
054300 COLUMN-NAMES-ROUTINE.                                            ST1184.2
054400     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           ST1184.2
054500     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1184.2
054600     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        ST1184.2
054700 END-ROUTINE.                                                     ST1184.2
054800     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.ST1184.2
054900 END-RTN-EXIT.                                                    ST1184.2
055000     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1184.2
055100 END-ROUTINE-1.                                                   ST1184.2
055200      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      ST1184.2
055300      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               ST1184.2
055400      ADD PASS-COUNTER TO ERROR-HOLD.                             ST1184.2
055500*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   ST1184.2
055600      MOVE PASS-COUNTER TO CCVS-E-4-1.                            ST1184.2
055700      MOVE ERROR-HOLD TO CCVS-E-4-2.                              ST1184.2
055800      MOVE CCVS-E-4 TO CCVS-E-2-2.                                ST1184.2
055900      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           ST1184.2
056000  END-ROUTINE-12.                                                 ST1184.2
056100      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        ST1184.2
056200     IF       ERROR-COUNTER IS EQUAL TO ZERO                      ST1184.2
056300         MOVE "NO " TO ERROR-TOTAL                                ST1184.2
056400         ELSE                                                     ST1184.2
056500         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       ST1184.2
056600     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           ST1184.2
056700     PERFORM WRITE-LINE.                                          ST1184.2
056800 END-ROUTINE-13.                                                  ST1184.2
056900     IF DELETE-COUNTER IS EQUAL TO ZERO                           ST1184.2
057000         MOVE "NO " TO ERROR-TOTAL  ELSE                          ST1184.2
057100         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      ST1184.2
057200     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   ST1184.2
057300     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           ST1184.2
057400      IF   INSPECT-COUNTER EQUAL TO ZERO                          ST1184.2
057500          MOVE "NO " TO ERROR-TOTAL                               ST1184.2
057600      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   ST1184.2
057700      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            ST1184.2
057800      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          ST1184.2
057900     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           ST1184.2
058000 WRITE-LINE.                                                      ST1184.2
058100     ADD 1 TO RECORD-COUNT.                                       ST1184.2
058200*    IF RECORD-COUNT GREATER 42                                   ST1184.2
058300*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          ST1184.2
058400*        MOVE SPACE TO DUMMY-RECORD                               ST1184.2
058500*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  ST1184.2
058600*        MOVE CCVS-H-1  TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    ST1184.2
058700*        MOVE CCVS-H-2A TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    ST1184.2
058800*        MOVE CCVS-H-2B TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    ST1184.2
058900*        MOVE CCVS-H-3  TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    ST1184.2
059000*        MOVE CCVS-C-1  TO DUMMY-RECORD PERFORM WRT-LN            ST1184.2
059100*        MOVE CCVS-C-2  TO DUMMY-RECORD PERFORM WRT-LN            ST1184.2
059200*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          ST1184.2
059300*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          ST1184.2
059400*        MOVE ZERO TO RECORD-COUNT.                               ST1184.2
059500     PERFORM WRT-LN.                                              ST1184.2
059600 WRT-LN.                                                          ST1184.2
059700     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               ST1184.2
059800     MOVE SPACE TO DUMMY-RECORD.                                  ST1184.2
059900 BLANK-LINE-PRINT.                                                ST1184.2
060000     PERFORM WRT-LN.                                              ST1184.2
060100 FAIL-ROUTINE.                                                    ST1184.2
060200     IF     COMPUTED-X NOT EQUAL TO SPACE                         ST1184.2
060300            GO TO   FAIL-ROUTINE-WRITE.                           ST1184.2
060400     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.ST1184.2
060500     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 ST1184.2
060600     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   ST1184.2
060700     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1184.2
060800     MOVE   SPACES TO INF-ANSI-REFERENCE.                         ST1184.2
060900     GO TO  FAIL-ROUTINE-EX.                                      ST1184.2
061000 FAIL-ROUTINE-WRITE.                                              ST1184.2
061100     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         ST1184.2
061200     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 ST1184.2
061300     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. ST1184.2
061400     MOVE   SPACES TO COR-ANSI-REFERENCE.                         ST1184.2
061500 FAIL-ROUTINE-EX. EXIT.                                           ST1184.2
061600 BAIL-OUT.                                                        ST1184.2
061700     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   ST1184.2
061800     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           ST1184.2
061900 BAIL-OUT-WRITE.                                                  ST1184.2
062000     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  ST1184.2
062100     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 ST1184.2
062200     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1184.2
062300     MOVE   SPACES TO INF-ANSI-REFERENCE.                         ST1184.2
062400 BAIL-OUT-EX. EXIT.                                               ST1184.2
062500 CCVS1-EXIT.                                                      ST1184.2
062600     EXIT.                                                        ST1184.2
062700 OUTPROC-EXIT SECTION.                                            ST1184.2
062800 EXIT-ONLY.                                                       ST1184.2
062900     PERFORM  CLOSE-FILES.                                        ST1184.2
      *END-OF,ST118A                                                                  
