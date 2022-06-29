      *HEADER,COBOL,ST125A                                                            
000100 IDENTIFICATION DIVISION.                                         ST1254.2
000200 PROGRAM-ID.                                                      ST1254.2
000300     ST125A.                                                      ST1254.2
000400****************************************************************  ST1254.2
000500*                                                              *  ST1254.2
000600*    VALIDATION FOR:-                                          *  ST1254.2
000700*                                                              *  ST1254.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1254.2
000900*                                                              *  ST1254.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".ST1254.2
001100*                                                              *  ST1254.2
001200****************************************************************  ST1254.2
001300*                                                              *  ST1254.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  ST1254.2
001500*                                                              *  ST1254.2
001600*        X-01                                                  *  ST1254.2
001700*        X-02                                                  *  ST1254.2
001800*        X-03                                                  *  ST1254.2
001900*        X-27                                                  *  ST1254.2
002000*        X-55  - SYSTEM PRINTER NAME.                          *  ST1254.2
002100*        X-69                                                  *  ST1254.2
002200*        X-74                                                  *  ST1254.2
002300*        X-75                                                  *  ST1254.2
002400*        X-82  - SOURCE COMPUTER NAME.                         *  ST1254.2
002500*        X-83  - OBJECT COMPUTER NAME.                         *  ST1254.2
002600*                                                              *  ST1254.2
002700****************************************************************  ST1254.2
002800*                                                                 ST1254.2
002900*    THIS PROGRAM TESTS THE FACILITY OF MULTIPLE FILES IN THE     ST1254.2
003000*    "GIVING" PHRASE OF THE "SORT" STATEMENT.                     ST1254.2
003100*    THE CONTENT OF THE 3 OUTPUT FILES WILL BE VERIFIED IN        ST1254.2
003200*    PROGRAM ST126A.                                              ST1254.2
003300*    THIS PROGRAM BUILDS A FILE OF NINE RECORDS. EACH RECORD HAS  ST1254.2
003400*    THREE KEYS, AND THE VALUES OF THE RECORDS ARE SHOWN BELOW-   ST1254.2
003500*             S                                                   ST1254.2
003600*             O                                                   ST1254.2
003700*             R               SORT                                ST1254.2
003800*             T  SORT         KEY                                 ST1254.2
003900*             K  KEY           -2                                 ST1254.2
004000*             E   -1           ..                                 ST1254.2
004100*             Y   ..         .    .                               ST1254.2
004200*             -  .  .      .        .                             ST1254.2
004300*             3 .    .   .            .                           ST1254.2
004400*             ..      ..                .                         ST1254.2
004500*              11111112888888888888888888                         ST1254.2
004600*              11111112999999999999999999                         ST1254.2
004700*              11111112999999999999999999                         ST1254.2
004800*              00000001999999999999999999                         ST1254.2
004900*             000000001999999999999999999                         ST1254.2
005000*             000000001999999999999999999                         ST1254.2
005100*             000000001999999999999999999                         ST1254.2
005200*             000000001999999999999999999                         ST1254.2
005300*             000000001999999999999999999                         ST1254.2
005400*    THERE IS AN ASSUMED DECIMAL POINT BETWEEN THE FIRST AND      ST1254.2
005500*    SECOND COLUMNS OF SORTKEY-1.                                 ST1254.2
005600*                                                                 ST1254.2
005700 ENVIRONMENT DIVISION.                                            ST1254.2
005800 CONFIGURATION SECTION.                                           ST1254.2
005900 SOURCE-COMPUTER.                                                 ST1254.2
006000     XXXXX082.                                                    ST1254.2
006100 OBJECT-COMPUTER.                                                 ST1254.2
006200     XXXXX083.                                                    ST1254.2
006300 INPUT-OUTPUT SECTION.                                            ST1254.2
006400 FILE-CONTROL.                                                    ST1254.2
006500     SELECT PRINT-FILE ASSIGN TO                                  ST1254.2
006600     XXXXX055.                                                    ST1254.2
006700     SELECT SORTFILE-1F ASSIGN TO                                 ST1254.2
006800     XXXXX027.                                                    ST1254.2
006900     SELECT SORTOUT-1F ASSIGN TO                                  ST1254.2
007000     XXXXP001.                                                    ST1254.2
007100     SELECT SORTOUT-2F ASSIGN TO                                  ST1254.2
007200     XXXXP002.                                                    ST1254.2
007300     SELECT SORTOUT-3F ASSIGN TO                                  ST1254.2
007400     XXXXP003.                                                    ST1254.2
007500 DATA DIVISION.                                                   ST1254.2
007600 FILE SECTION.                                                    ST1254.2
007700 FD  PRINT-FILE.                                                  ST1254.2
007800 01  PRINT-REC PICTURE X(120).                                    ST1254.2
007900 01  DUMMY-RECORD PICTURE X(120).                                 ST1254.2
008000 FD  SORTOUT-1F                                                   ST1254.2
008100     LABEL RECORDS STANDARD                                       ST1254.2
008200C    VALUE OF                                                     ST1254.2
008300C    XXXXX074                                                     ST1254.2
008400C    IS                                                           ST1254.2
008500C    XXXXX075                                                     ST1254.2
008600*    XXXXX069                                                     ST1254.2
008700     RECORD CONTAINS 27 CHARACTERS.                               ST1254.2
008800 01  SORTOUT-REC-1.                                               ST1254.2
008900     02 FILLER          PICTURE X(27).                            ST1254.2
009000 FD  SORTOUT-2F                                                   ST1254.2
009100     LABEL RECORDS STANDARD                                       ST1254.2
009200C    VALUE OF                                                     ST1254.2
009300C    XXXXX074                                                     ST1254.2
009400C    IS                                                           ST1254.2
009500C    XXXXX075                                                     ST1254.2
009600*    XXXXX069                                                     ST1254.2
009700     RECORD CONTAINS 27 CHARACTERS.                               ST1254.2
009800 01  SORTOUT-REC-2.                                               ST1254.2
009900     02 FILLER          PICTURE X(27).                            ST1254.2
010000 FD  SORTOUT-3F                                                   ST1254.2
010100     LABEL RECORDS STANDARD                                       ST1254.2
010200C    VALUE OF                                                     ST1254.2
010300C    XXXXX074                                                     ST1254.2
010400C    IS                                                           ST1254.2
010500C    XXXXX075                                                     ST1254.2
010600*    XXXXX069                                                     ST1254.2
010700     RECORD CONTAINS 27 CHARACTERS.                               ST1254.2
010800 01  SORTOUT-REC-3.                                               ST1254.2
010900     02 FILLER          PICTURE X(27).                            ST1254.2
011000 SD  SORTFILE-1F                                                  ST1254.2
011100     RECORD  CONTAINS 27 CHARACTERS.                              ST1254.2
011200 01  SORT-GROUP.                                                  ST1254.2
011300     02 SORTKEY-3       PICTURE X.                                ST1254.2
011400     02 SORTKEY-1       PICTURE S9V9(7).                          ST1254.2
011500     02 SORTKEY-2       PICTURE 9(18).                            ST1254.2
011600 WORKING-STORAGE SECTION.                                         ST1254.2
011700 77  UTIL-CTR           PICTURE S99999 VALUE ZERO.                ST1254.2
011800 77  UTILITY-1          PICTURE S9V9(7) VALUE +1.1111112.         ST1254.2
011900 77  UTILITY-2          PICTURE 9(018) VALUE 888888888888888888.  ST1254.2
012000 77  UTILITY-3          PICTURE X VALUE SPACE.                    ST1254.2
012100 01  TEST-RESULTS.                                                ST1254.2
012200     02 FILLER                   PIC X      VALUE SPACE.          ST1254.2
012300     02 FEATURE                  PIC X(20)  VALUE SPACE.          ST1254.2
012400     02 FILLER                   PIC X      VALUE SPACE.          ST1254.2
012500     02 P-OR-F                   PIC X(5)   VALUE SPACE.          ST1254.2
012600     02 FILLER                   PIC X      VALUE SPACE.          ST1254.2
012700     02  PAR-NAME.                                                ST1254.2
012800       03 FILLER                 PIC X(19)  VALUE SPACE.          ST1254.2
012900       03  PARDOT-X              PIC X      VALUE SPACE.          ST1254.2
013000       03 DOTVALUE               PIC 99     VALUE ZERO.           ST1254.2
013100     02 FILLER                   PIC X(8)   VALUE SPACE.          ST1254.2
013200     02 RE-MARK                  PIC X(61).                       ST1254.2
013300 01  TEST-COMPUTED.                                               ST1254.2
013400     02 FILLER                   PIC X(30)  VALUE SPACE.          ST1254.2
013500     02 FILLER                   PIC X(17)  VALUE                 ST1254.2
013600            "       COMPUTED=".                                   ST1254.2
013700     02 COMPUTED-X.                                               ST1254.2
013800     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          ST1254.2
013900     03 COMPUTED-N               REDEFINES COMPUTED-A             ST1254.2
014000                                 PIC -9(9).9(9).                  ST1254.2
014100     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         ST1254.2
014200     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     ST1254.2
014300     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     ST1254.2
014400     03       CM-18V0 REDEFINES COMPUTED-A.                       ST1254.2
014500         04 COMPUTED-18V0                    PIC -9(18).          ST1254.2
014600         04 FILLER                           PIC X.               ST1254.2
014700     03 FILLER PIC X(50) VALUE SPACE.                             ST1254.2
014800 01  TEST-CORRECT.                                                ST1254.2
014900     02 FILLER PIC X(30) VALUE SPACE.                             ST1254.2
015000     02 FILLER PIC X(17) VALUE "       CORRECT =".                ST1254.2
015100     02 CORRECT-X.                                                ST1254.2
015200     03 CORRECT-A                  PIC X(20) VALUE SPACE.         ST1254.2
015300     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      ST1254.2
015400     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         ST1254.2
015500     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     ST1254.2
015600     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     ST1254.2
015700     03      CR-18V0 REDEFINES CORRECT-A.                         ST1254.2
015800         04 CORRECT-18V0                     PIC -9(18).          ST1254.2
015900         04 FILLER                           PIC X.               ST1254.2
016000     03 FILLER PIC X(2) VALUE SPACE.                              ST1254.2
016100     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     ST1254.2
016200 01  CCVS-C-1.                                                    ST1254.2
016300     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAST1254.2
016400-    "SS  PARAGRAPH-NAME                                          ST1254.2
016500-    "       REMARKS".                                            ST1254.2
016600     02 FILLER                     PIC X(20)    VALUE SPACE.      ST1254.2
016700 01  CCVS-C-2.                                                    ST1254.2
016800     02 FILLER                     PIC X        VALUE SPACE.      ST1254.2
016900     02 FILLER                     PIC X(6)     VALUE "TESTED".   ST1254.2
017000     02 FILLER                     PIC X(15)    VALUE SPACE.      ST1254.2
017100     02 FILLER                     PIC X(4)     VALUE "FAIL".     ST1254.2
017200     02 FILLER                     PIC X(94)    VALUE SPACE.      ST1254.2
017300 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       ST1254.2
017400 01  REC-CT                        PIC 99       VALUE ZERO.       ST1254.2
017500 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       ST1254.2
017600 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       ST1254.2
017700 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       ST1254.2
017800 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       ST1254.2
017900 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       ST1254.2
018000 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       ST1254.2
018100 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      ST1254.2
018200 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       ST1254.2
018300 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     ST1254.2
018400 01  CCVS-H-1.                                                    ST1254.2
018500     02  FILLER                    PIC X(39)    VALUE SPACES.     ST1254.2
018600     02  FILLER                    PIC X(42)    VALUE             ST1254.2
018700     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 ST1254.2
018800     02  FILLER                    PIC X(39)    VALUE SPACES.     ST1254.2
018900 01  CCVS-H-2A.                                                   ST1254.2
019000   02  FILLER                        PIC X(40)  VALUE SPACE.      ST1254.2
019100   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  ST1254.2
019200   02  FILLER                        PIC XXXX   VALUE             ST1254.2
019300     "4.2 ".                                                      ST1254.2
019400   02  FILLER                        PIC X(28)  VALUE             ST1254.2
019500            " COPY - NOT FOR DISTRIBUTION".                       ST1254.2
019600   02  FILLER                        PIC X(41)  VALUE SPACE.      ST1254.2
019700                                                                  ST1254.2
019800 01  CCVS-H-2B.                                                   ST1254.2
019900   02  FILLER                        PIC X(15)  VALUE             ST1254.2
020000            "TEST RESULT OF ".                                    ST1254.2
020100   02  TEST-ID                       PIC X(9).                    ST1254.2
020200   02  FILLER                        PIC X(4)   VALUE             ST1254.2
020300            " IN ".                                               ST1254.2
020400   02  FILLER                        PIC X(12)  VALUE             ST1254.2
020500     " HIGH       ".                                              ST1254.2
020600   02  FILLER                        PIC X(22)  VALUE             ST1254.2
020700            " LEVEL VALIDATION FOR ".                             ST1254.2
020800   02  FILLER                        PIC X(58)  VALUE             ST1254.2
020900     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1254.2
021000 01  CCVS-H-3.                                                    ST1254.2
021100     02  FILLER                      PIC X(34)  VALUE             ST1254.2
021200            " FOR OFFICIAL USE ONLY    ".                         ST1254.2
021300     02  FILLER                      PIC X(58)  VALUE             ST1254.2
021400     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".ST1254.2
021500     02  FILLER                      PIC X(28)  VALUE             ST1254.2
021600            "  COPYRIGHT   1985 ".                                ST1254.2
021700 01  CCVS-E-1.                                                    ST1254.2
021800     02 FILLER                       PIC X(52)  VALUE SPACE.      ST1254.2
021900     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              ST1254.2
022000     02 ID-AGAIN                     PIC X(9).                    ST1254.2
022100     02 FILLER                       PIC X(45)  VALUE SPACES.     ST1254.2
022200 01  CCVS-E-2.                                                    ST1254.2
022300     02  FILLER                      PIC X(31)  VALUE SPACE.      ST1254.2
022400     02  FILLER                      PIC X(21)  VALUE SPACE.      ST1254.2
022500     02 CCVS-E-2-2.                                               ST1254.2
022600         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      ST1254.2
022700         03 FILLER                   PIC X      VALUE SPACE.      ST1254.2
022800         03 ENDER-DESC               PIC X(44)  VALUE             ST1254.2
022900            "ERRORS ENCOUNTERED".                                 ST1254.2
023000 01  CCVS-E-3.                                                    ST1254.2
023100     02  FILLER                      PIC X(22)  VALUE             ST1254.2
023200            " FOR OFFICIAL USE ONLY".                             ST1254.2
023300     02  FILLER                      PIC X(12)  VALUE SPACE.      ST1254.2
023400     02  FILLER                      PIC X(58)  VALUE             ST1254.2
023500     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1254.2
023600     02  FILLER                      PIC X(13)  VALUE SPACE.      ST1254.2
023700     02 FILLER                       PIC X(15)  VALUE             ST1254.2
023800             " COPYRIGHT 1985".                                   ST1254.2
023900 01  CCVS-E-4.                                                    ST1254.2
024000     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      ST1254.2
024100     02 FILLER                       PIC X(4)   VALUE " OF ".     ST1254.2
024200     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      ST1254.2
024300     02 FILLER                       PIC X(40)  VALUE             ST1254.2
024400      "  TESTS WERE EXECUTED SUCCESSFULLY".                       ST1254.2
024500 01  XXINFO.                                                      ST1254.2
024600     02 FILLER                       PIC X(19)  VALUE             ST1254.2
024700            "*** INFORMATION ***".                                ST1254.2
024800     02 INFO-TEXT.                                                ST1254.2
024900       04 FILLER                     PIC X(8)   VALUE SPACE.      ST1254.2
025000       04 XXCOMPUTED                 PIC X(20).                   ST1254.2
025100       04 FILLER                     PIC X(5)   VALUE SPACE.      ST1254.2
025200       04 XXCORRECT                  PIC X(20).                   ST1254.2
025300     02 INF-ANSI-REFERENCE           PIC X(48).                   ST1254.2
025400 01  HYPHEN-LINE.                                                 ST1254.2
025500     02 FILLER  PIC IS X VALUE IS SPACE.                          ST1254.2
025600     02 FILLER  PIC IS X(65)    VALUE IS "************************ST1254.2
025700-    "*****************************************".                 ST1254.2
025800     02 FILLER  PIC IS X(54)    VALUE IS "************************ST1254.2
025900-    "******************************".                            ST1254.2
026000 01  CCVS-PGM-ID                     PIC X(9)   VALUE             ST1254.2
026100     "ST125A".                                                    ST1254.2
026200 PROCEDURE DIVISION.                                              ST1254.2
026300 SORTPARA SECTION.                                                ST1254.2
026400 SORT-PARAGRAPH.                                                  ST1254.2
026500     SORT     SORTFILE-1F ON                                      ST1254.2
026600              ASCENDING SORTKEY-1                                 ST1254.2
026700              DESCENDING SORTKEY-2                                ST1254.2
026800              ASCENDING SORTKEY-3                                 ST1254.2
026900              INPUT PROCEDURE INPROC THRU INPROC-EXIT             ST1254.2
027000              GIVING SORTOUT-1F                                   ST1254.2
027100                     SORTOUT-2F                                   ST1254.2
027200                     SORTOUT-3F.                                  ST1254.2
027300     STOP     RUN.                                                ST1254.2
027400 INPROC SECTION.                                                  ST1254.2
027500 OPEN-FILES.                                                      ST1254.2
027600     OPEN     OUTPUT PRINT-FILE.                                  ST1254.2
027700     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   ST1254.2
027800     MOVE    SPACE TO TEST-RESULTS.                               ST1254.2
027900     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             ST1254.2
028000     GO TO CCVS1-EXIT.                                            ST1254.2
028100 CLOSE-FILES.                                                     ST1254.2
028200     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   ST1254.2
028300 TERMINATE-CCVS.                                                  ST1254.2
028400*    EXIT PROGRAM.                                                ST1254.2
028500*TERMINATE-CALL.                                                  ST1254.2
028600     STOP     RUN.                                                ST1254.2
028700 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         ST1254.2
028800 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           ST1254.2
028900 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          ST1254.2
029000 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      ST1254.2
029100     MOVE "****TEST DELETED****" TO RE-MARK.                      ST1254.2
029200 PRINT-DETAIL.                                                    ST1254.2
029300     IF REC-CT NOT EQUAL TO ZERO                                  ST1254.2
029400             MOVE "." TO PARDOT-X                                 ST1254.2
029500             MOVE REC-CT TO DOTVALUE.                             ST1254.2
029600     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      ST1254.2
029700     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               ST1254.2
029800        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 ST1254.2
029900          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 ST1254.2
030000     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              ST1254.2
030100     MOVE SPACE TO CORRECT-X.                                     ST1254.2
030200     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         ST1254.2
030300     MOVE     SPACE TO RE-MARK.                                   ST1254.2
030400 HEAD-ROUTINE.                                                    ST1254.2
030500     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  ST1254.2
030600     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  ST1254.2
030700     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  ST1254.2
030800     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  ST1254.2
030900 COLUMN-NAMES-ROUTINE.                                            ST1254.2
031000     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           ST1254.2
031100     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1254.2
031200     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        ST1254.2
031300 END-ROUTINE.                                                     ST1254.2
031400     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.ST1254.2
031500 END-RTN-EXIT.                                                    ST1254.2
031600     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1254.2
031700 END-ROUTINE-1.                                                   ST1254.2
031800      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      ST1254.2
031900      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               ST1254.2
032000      ADD PASS-COUNTER TO ERROR-HOLD.                             ST1254.2
032100*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   ST1254.2
032200      MOVE PASS-COUNTER TO CCVS-E-4-1.                            ST1254.2
032300      MOVE ERROR-HOLD TO CCVS-E-4-2.                              ST1254.2
032400      MOVE CCVS-E-4 TO CCVS-E-2-2.                                ST1254.2
032500      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           ST1254.2
032600  END-ROUTINE-12.                                                 ST1254.2
032700      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        ST1254.2
032800     IF       ERROR-COUNTER IS EQUAL TO ZERO                      ST1254.2
032900         MOVE "NO " TO ERROR-TOTAL                                ST1254.2
033000         ELSE                                                     ST1254.2
033100         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       ST1254.2
033200     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           ST1254.2
033300     PERFORM WRITE-LINE.                                          ST1254.2
033400 END-ROUTINE-13.                                                  ST1254.2
033500     IF DELETE-COUNTER IS EQUAL TO ZERO                           ST1254.2
033600         MOVE "NO " TO ERROR-TOTAL  ELSE                          ST1254.2
033700         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      ST1254.2
033800     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   ST1254.2
033900     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           ST1254.2
034000      IF   INSPECT-COUNTER EQUAL TO ZERO                          ST1254.2
034100          MOVE "NO " TO ERROR-TOTAL                               ST1254.2
034200      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   ST1254.2
034300      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            ST1254.2
034400      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          ST1254.2
034500     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           ST1254.2
034600 WRITE-LINE.                                                      ST1254.2
034700     ADD 1 TO RECORD-COUNT.                                       ST1254.2
034800*    IF RECORD-COUNT GREATER 42                                   ST1254.2
034900*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          ST1254.2
035000*        MOVE SPACE TO DUMMY-RECORD                               ST1254.2
035100*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  ST1254.2
035200*        MOVE CCVS-H-1  TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    ST1254.2
035300*        MOVE CCVS-H-2A TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    ST1254.2
035400*        MOVE CCVS-H-2B TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    ST1254.2
035500*        MOVE CCVS-H-3  TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    ST1254.2
035600*        MOVE CCVS-C-1  TO DUMMY-RECORD PERFORM WRT-LN            ST1254.2
035700*        MOVE CCVS-C-2  TO DUMMY-RECORD PERFORM WRT-LN            ST1254.2
035800*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          ST1254.2
035900*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          ST1254.2
036000*        MOVE ZERO TO RECORD-COUNT.                               ST1254.2
036100     PERFORM WRT-LN.                                              ST1254.2
036200 WRT-LN.                                                          ST1254.2
036300     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               ST1254.2
036400     MOVE SPACE TO DUMMY-RECORD.                                  ST1254.2
036500 BLANK-LINE-PRINT.                                                ST1254.2
036600     PERFORM WRT-LN.                                              ST1254.2
036700 FAIL-ROUTINE.                                                    ST1254.2
036800     IF     COMPUTED-X NOT EQUAL TO SPACE                         ST1254.2
036900            GO TO   FAIL-ROUTINE-WRITE.                           ST1254.2
037000     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.ST1254.2
037100     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 ST1254.2
037200     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   ST1254.2
037300     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1254.2
037400     MOVE   SPACES TO INF-ANSI-REFERENCE.                         ST1254.2
037500     GO TO  FAIL-ROUTINE-EX.                                      ST1254.2
037600 FAIL-ROUTINE-WRITE.                                              ST1254.2
037700     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         ST1254.2
037800     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 ST1254.2
037900     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. ST1254.2
038000     MOVE   SPACES TO COR-ANSI-REFERENCE.                         ST1254.2
038100 FAIL-ROUTINE-EX. EXIT.                                           ST1254.2
038200 BAIL-OUT.                                                        ST1254.2
038300     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   ST1254.2
038400     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           ST1254.2
038500 BAIL-OUT-WRITE.                                                  ST1254.2
038600     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  ST1254.2
038700     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 ST1254.2
038800     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1254.2
038900     MOVE   SPACES TO INF-ANSI-REFERENCE.                         ST1254.2
039000 BAIL-OUT-EX. EXIT.                                               ST1254.2
039100 CCVS1-EXIT.                                                      ST1254.2
039200     EXIT.                                                        ST1254.2
039300 ST125A-001-01.                                                   ST1254.2
039400     MOVE   "XI-20 4.4.4 GR(12)" TO ANSI-REFERENCE.               ST1254.2
039500     OPEN     OUTPUT   SORTOUT-1F.                                ST1254.2
039600     OPEN     OUTPUT   SORTOUT-2F.                                ST1254.2
039700     OPEN     OUTPUT   SORTOUT-3F.                                ST1254.2
039800     MOVE     "THIS PROGRAM BUILDS AND" TO RE-MARK.               ST1254.2
039900     PERFORM  PRINT-DETAIL.                                       ST1254.2
040000     MOVE     "SORTS 3 FILES AND PASSES" TO RE-MARK.              ST1254.2
040100     PERFORM  PRINT-DETAIL.                                       ST1254.2
040200     MOVE      "THE OUTPUT TO ST126A." TO RE-MARK.                ST1254.2
040300     PERFORM  PRINT-DETAIL.                                       ST1254.2
040400 BUILD-FILE.                                                      ST1254.2
040500     ADD      1 TO UTIL-CTR                                       ST1254.2
040600     IF       UTIL-CTR EQUAL TO 2                                 ST1254.2
040700              MOVE 999999999999999999 TO UTILITY-2.               ST1254.2
040800     IF       UTIL-CTR EQUAL TO 4                                 ST1254.2
040900              ADD -1.1111111 TO UTILITY-1.                        ST1254.2
041000     IF       UTIL-CTR EQUAL TO 5                                 ST1254.2
041100              MOVE ZERO TO UTILITY-3.                             ST1254.2
041200     MOVE     UTILITY-1 TO SORTKEY-1.                             ST1254.2
041300     MOVE     UTILITY-3 TO SORTKEY-3.                             ST1254.2
041400     MOVE     UTILITY-2 TO SORTKEY-2.                             ST1254.2
041500     RELEASE  SORT-GROUP.                                         ST1254.2
041600     IF       UTIL-CTR LESS THAN 9 GO TO BUILD-FILE.              ST1254.2
041700 BUILD-FILE-TEST.                                                 ST1254.2
041800     IF       UTIL-CTR EQUAL TO 9                                 ST1254.2
041900              PERFORM PASS GO TO BUILD-FILE-WRITE.                ST1254.2
042000 BUILD-FILE-FAIL.                                                 ST1254.2
042100     MOVE     UTIL-CTR TO COMPUTED-N.                             ST1254.2
042200     MOVE     9 TO CORRECT-N.                                     ST1254.2
042300     PERFORM  FAIL.                                               ST1254.2
042400 BUILD-FILE-WRITE.                                                ST1254.2
042500     MOVE     "CREATE A FILE" TO FEATURE.                         ST1254.2
042600     MOVE     "BUILD-FILE-TEST" TO PAR-NAME.                      ST1254.2
042700     PERFORM  PRINT-DETAIL.                                       ST1254.2
042800     CLOSE    SORTOUT-1F.                                         ST1254.2
042900     CLOSE    SORTOUT-2F.                                         ST1254.2
043000     CLOSE    SORTOUT-3F.                                         ST1254.2
043100                                                                  ST1254.2
043200 INPROC-EXIT SECTION.                                             ST1254.2
043300 EXITPARA.                                                        ST1254.2
043400     PERFORM CLOSE-FILES.                                         ST1254.2
      *END-OF,ST125A                                                                  
