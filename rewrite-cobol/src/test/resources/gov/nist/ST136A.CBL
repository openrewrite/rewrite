      *HEADER,COBOL,ST136A                                                            
000100 IDENTIFICATION DIVISION.                                         ST1364.2
000200 PROGRAM-ID.                                                      ST1364.2
000300     ST136A.                                                      ST1364.2
000400                                                                  ST1364.2
000500****************************************************************  ST1364.2
000600*                                                              *  ST1364.2
000700*    VALIDATION FOR:-                                          *  ST1364.2
000800*                                                              *  ST1364.2
000900*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1364.2
001000*                                                              *  ST1364.2
001100*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".ST1364.2
001200*                                                              *  ST1364.2
001300****************************************************************  ST1364.2
001400*                                                              *  ST1364.2
001500*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  ST1364.2
001600*                                                              *  ST1364.2
001700*        X-55  - SYSTEM PRINTER NAME.                          *  ST1364.2
001800*        X-82  - SOURCE COMPUTER NAME.                         *  ST1364.2
001900*        X-83  - OBJECT COMPUTER NAME.                         *  ST1364.2
002000*                                                              *  ST1364.2
002100****************************************************************  ST1364.2
002200*    ST206 TESTS THE FROM OPTION OF THE RELEASE VERB. TEN RECORDS ST1364.2
002300*    ARE CREATED WITH THE NUMERICAL KEY DESCENDING FROM 10 THRU 1.ST1364.2
002400*    THE RECORDS ARE THEN SORTED INTO ASCENDING SEQUENCE AND THE  ST1364.2
002500*    RESULTS ARE SPOT-CHECKED.                                    ST1364.2
002600*                                                                 ST1364.2
002700 ENVIRONMENT DIVISION.                                            ST1364.2
002800 CONFIGURATION SECTION.                                           ST1364.2
002900 SOURCE-COMPUTER.                                                 ST1364.2
003000     XXXXX082.                                                    ST1364.2
003100 OBJECT-COMPUTER.                                                 ST1364.2
003200     XXXXX083.                                                    ST1364.2
003300 INPUT-OUTPUT SECTION.                                            ST1364.2
003400 FILE-CONTROL.                                                    ST1364.2
003500     SELECT PRINT-FILE ASSIGN TO                                  ST1364.2
003600     XXXXX055.                                                    ST1364.2
003700     SELECT   SORTOUT-2F ASSIGN TO                                ST1364.2
003800     XXXXX001.                                                    ST1364.2
003900     SELECT   SORTFILE-2F ASSIGN TO                               ST1364.2
004000     XXXXX027.                                                    ST1364.2
004100 DATA DIVISION.                                                   ST1364.2
004200 FILE SECTION.                                                    ST1364.2
004300 FD  PRINT-FILE.                                                  ST1364.2
004400 01  PRINT-REC PICTURE X(120).                                    ST1364.2
004500 01  DUMMY-RECORD PICTURE X(120).                                 ST1364.2
004600 FD  SORTOUT-2F                                                   ST1364.2
004700     LABEL RECORDS STANDARD                                       ST1364.2
004800C    VALUE OF                                                     ST1364.2
004900C    XXXXX074                                                     ST1364.2
005000C    IS                                                           ST1364.2
005100C    XXXXX075                                                     ST1364.2
005200*    XXXXX069                                                     ST1364.2
005300     DATA RECORD IS SORTOUT-REC.                                  ST1364.2
005400 01  SORTOUT-REC.                                                 ST1364.2
005500     02 SORTOUT-NON-KEY-1         PICTURE A(12).                  ST1364.2
005600     02 SORTOUT-KEY               PICTURE S9(8).                  ST1364.2
005700     02 SORTOUT-NON-KEY-2         PICTURE A(60).                  ST1364.2
005800 SD  SORTFILE-2F                                                  ST1364.2
005900     DATA RECORD IS SORTFILE-REC.                                 ST1364.2
006000 01  SORTFILE-REC.                                                ST1364.2
006100     02 SORTFILE-NON-KEY-1        PICTURE X(12).                  ST1364.2
006200     02 SORTFILE-KEY              PICTURE S9(8).                  ST1364.2
006300     02 SORTFILE-NON-KEY-2        PICTURE X(60).                  ST1364.2
006400 WORKING-STORAGE SECTION.                                         ST1364.2
006500 77  LITERAL-B          PICTURE X(12) VALUE "B           ".       ST1364.2
006600 77  UTIL-CTR           PICTURE S99999.                           ST1364.2
006700 77  COMMENT-1 PICTURE X(27) VALUE "KEY AREA                   ". ST1364.2
006800 77  COMMENT-2 PICTURE X(27) VALUE "FIRST 20 OF 60-CHAR FIELD  ". ST1364.2
006900 77  COMMENT-3 PICTURE X(27) VALUE "SECOND 20 OF 60-CHAR FIELD ". ST1364.2
007000 77  COMMENT-4 PICTURE X(27) VALUE "THIRD 20 OF 60-CHAR FIELD  ". ST1364.2
007100 77  ALL-A              PICTURE X(60) VALUE ALL "A".              ST1364.2
007200 77  ALL-X              PICTURE X(60) VALUE ALL "X".              ST1364.2
007300 77  ALL-Z              PICTURE X(60) VALUE ALL "Z".              ST1364.2
007400 01  WORK-REC.                                                    ST1364.2
007500     02 WORK-NON-KEY-1  PICTURE X(12).                            ST1364.2
007600     02 WORK-KEY        PICTURE S9(8).                            ST1364.2
007700     02 WORK-NON-KEY-2  PICTURE X(60).                            ST1364.2
007800 01  COMPUTED-BREAKDOWN.                                          ST1364.2
007900     02 FIRST-20CM      PICTURE X(20).                            ST1364.2
008000     02 SECOND-20CM     PICTURE X(20).                            ST1364.2
008100     02 THIRD-20CM      PICTURE X(20).                            ST1364.2
008200 01  CORRECT-BREAKDOWN.                                           ST1364.2
008300     02 FIRST-20CR      PICTURE X(20).                            ST1364.2
008400     02 SECOND-20CR     PICTURE X(20).                            ST1364.2
008500     02 THIRD-20CR      PICTURE X(20).                            ST1364.2
008600 01  HOLD-REC.                                                    ST1364.2
008700     02 HOLD-NON-KEY-1  PICTURE X(12).                            ST1364.2
008800     02 HOLD-KEY        PICTURE S9(8).                            ST1364.2
008900     02 HOLD-NON-KEY-2  PICTURE X(60).                            ST1364.2
009000 01  TEST-RESULTS.                                                ST1364.2
009100     02 FILLER                   PIC X      VALUE SPACE.          ST1364.2
009200     02 FEATURE                  PIC X(20)  VALUE SPACE.          ST1364.2
009300     02 FILLER                   PIC X      VALUE SPACE.          ST1364.2
009400     02 P-OR-F                   PIC X(5)   VALUE SPACE.          ST1364.2
009500     02 FILLER                   PIC X      VALUE SPACE.          ST1364.2
009600     02  PAR-NAME.                                                ST1364.2
009700       03 FILLER                 PIC X(19)  VALUE SPACE.          ST1364.2
009800       03  PARDOT-X              PIC X      VALUE SPACE.          ST1364.2
009900       03 DOTVALUE               PIC 99     VALUE ZERO.           ST1364.2
010000     02 FILLER                   PIC X(8)   VALUE SPACE.          ST1364.2
010100     02 RE-MARK                  PIC X(61).                       ST1364.2
010200 01  TEST-COMPUTED.                                               ST1364.2
010300     02 FILLER                   PIC X(30)  VALUE SPACE.          ST1364.2
010400     02 FILLER                   PIC X(17)  VALUE                 ST1364.2
010500            "       COMPUTED=".                                   ST1364.2
010600     02 COMPUTED-X.                                               ST1364.2
010700     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          ST1364.2
010800     03 COMPUTED-N               REDEFINES COMPUTED-A             ST1364.2
010900                                 PIC -9(9).9(9).                  ST1364.2
011000     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         ST1364.2
011100     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     ST1364.2
011200     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     ST1364.2
011300     03       CM-18V0 REDEFINES COMPUTED-A.                       ST1364.2
011400         04 COMPUTED-18V0                    PIC -9(18).          ST1364.2
011500         04 FILLER                           PIC X.               ST1364.2
011600     03 FILLER PIC X(50) VALUE SPACE.                             ST1364.2
011700 01  TEST-CORRECT.                                                ST1364.2
011800     02 FILLER PIC X(30) VALUE SPACE.                             ST1364.2
011900     02 FILLER PIC X(17) VALUE "       CORRECT =".                ST1364.2
012000     02 CORRECT-X.                                                ST1364.2
012100     03 CORRECT-A                  PIC X(20) VALUE SPACE.         ST1364.2
012200     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      ST1364.2
012300     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         ST1364.2
012400     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     ST1364.2
012500     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     ST1364.2
012600     03      CR-18V0 REDEFINES CORRECT-A.                         ST1364.2
012700         04 CORRECT-18V0                     PIC -9(18).          ST1364.2
012800         04 FILLER                           PIC X.               ST1364.2
012900     03 FILLER PIC X(2) VALUE SPACE.                              ST1364.2
013000     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     ST1364.2
013100 01  CCVS-C-1.                                                    ST1364.2
013200     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAST1364.2
013300-    "SS  PARAGRAPH-NAME                                          ST1364.2
013400-    "       REMARKS".                                            ST1364.2
013500     02 FILLER                     PIC X(20)    VALUE SPACE.      ST1364.2
013600 01  CCVS-C-2.                                                    ST1364.2
013700     02 FILLER                     PIC X        VALUE SPACE.      ST1364.2
013800     02 FILLER                     PIC X(6)     VALUE "TESTED".   ST1364.2
013900     02 FILLER                     PIC X(15)    VALUE SPACE.      ST1364.2
014000     02 FILLER                     PIC X(4)     VALUE "FAIL".     ST1364.2
014100     02 FILLER                     PIC X(94)    VALUE SPACE.      ST1364.2
014200 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       ST1364.2
014300 01  REC-CT                        PIC 99       VALUE ZERO.       ST1364.2
014400 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       ST1364.2
014500 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       ST1364.2
014600 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       ST1364.2
014700 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       ST1364.2
014800 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       ST1364.2
014900 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       ST1364.2
015000 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      ST1364.2
015100 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       ST1364.2
015200 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     ST1364.2
015300 01  CCVS-H-1.                                                    ST1364.2
015400     02  FILLER                    PIC X(39)    VALUE SPACES.     ST1364.2
015500     02  FILLER                    PIC X(42)    VALUE             ST1364.2
015600     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 ST1364.2
015700     02  FILLER                    PIC X(39)    VALUE SPACES.     ST1364.2
015800 01  CCVS-H-2A.                                                   ST1364.2
015900   02  FILLER                        PIC X(40)  VALUE SPACE.      ST1364.2
016000   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  ST1364.2
016100   02  FILLER                        PIC XXXX   VALUE             ST1364.2
016200     "4.2 ".                                                      ST1364.2
016300   02  FILLER                        PIC X(28)  VALUE             ST1364.2
016400            " COPY - NOT FOR DISTRIBUTION".                       ST1364.2
016500   02  FILLER                        PIC X(41)  VALUE SPACE.      ST1364.2
016600                                                                  ST1364.2
016700 01  CCVS-H-2B.                                                   ST1364.2
016800   02  FILLER                        PIC X(15)  VALUE             ST1364.2
016900            "TEST RESULT OF ".                                    ST1364.2
017000   02  TEST-ID                       PIC X(9).                    ST1364.2
017100   02  FILLER                        PIC X(4)   VALUE             ST1364.2
017200            " IN ".                                               ST1364.2
017300   02  FILLER                        PIC X(12)  VALUE             ST1364.2
017400     " HIGH       ".                                              ST1364.2
017500   02  FILLER                        PIC X(22)  VALUE             ST1364.2
017600            " LEVEL VALIDATION FOR ".                             ST1364.2
017700   02  FILLER                        PIC X(58)  VALUE             ST1364.2
017800     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1364.2
017900 01  CCVS-H-3.                                                    ST1364.2
018000     02  FILLER                      PIC X(34)  VALUE             ST1364.2
018100            " FOR OFFICIAL USE ONLY    ".                         ST1364.2
018200     02  FILLER                      PIC X(58)  VALUE             ST1364.2
018300     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".ST1364.2
018400     02  FILLER                      PIC X(28)  VALUE             ST1364.2
018500            "  COPYRIGHT   1985 ".                                ST1364.2
018600 01  CCVS-E-1.                                                    ST1364.2
018700     02 FILLER                       PIC X(52)  VALUE SPACE.      ST1364.2
018800     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              ST1364.2
018900     02 ID-AGAIN                     PIC X(9).                    ST1364.2
019000     02 FILLER                       PIC X(45)  VALUE SPACES.     ST1364.2
019100 01  CCVS-E-2.                                                    ST1364.2
019200     02  FILLER                      PIC X(31)  VALUE SPACE.      ST1364.2
019300     02  FILLER                      PIC X(21)  VALUE SPACE.      ST1364.2
019400     02 CCVS-E-2-2.                                               ST1364.2
019500         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      ST1364.2
019600         03 FILLER                   PIC X      VALUE SPACE.      ST1364.2
019700         03 ENDER-DESC               PIC X(44)  VALUE             ST1364.2
019800            "ERRORS ENCOUNTERED".                                 ST1364.2
019900 01  CCVS-E-3.                                                    ST1364.2
020000     02  FILLER                      PIC X(22)  VALUE             ST1364.2
020100            " FOR OFFICIAL USE ONLY".                             ST1364.2
020200     02  FILLER                      PIC X(12)  VALUE SPACE.      ST1364.2
020300     02  FILLER                      PIC X(58)  VALUE             ST1364.2
020400     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1364.2
020500     02  FILLER                      PIC X(13)  VALUE SPACE.      ST1364.2
020600     02 FILLER                       PIC X(15)  VALUE             ST1364.2
020700             " COPYRIGHT 1985".                                   ST1364.2
020800 01  CCVS-E-4.                                                    ST1364.2
020900     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      ST1364.2
021000     02 FILLER                       PIC X(4)   VALUE " OF ".     ST1364.2
021100     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      ST1364.2
021200     02 FILLER                       PIC X(40)  VALUE             ST1364.2
021300      "  TESTS WERE EXECUTED SUCCESSFULLY".                       ST1364.2
021400 01  XXINFO.                                                      ST1364.2
021500     02 FILLER                       PIC X(19)  VALUE             ST1364.2
021600            "*** INFORMATION ***".                                ST1364.2
021700     02 INFO-TEXT.                                                ST1364.2
021800       04 FILLER                     PIC X(8)   VALUE SPACE.      ST1364.2
021900       04 XXCOMPUTED                 PIC X(20).                   ST1364.2
022000       04 FILLER                     PIC X(5)   VALUE SPACE.      ST1364.2
022100       04 XXCORRECT                  PIC X(20).                   ST1364.2
022200     02 INF-ANSI-REFERENCE           PIC X(48).                   ST1364.2
022300 01  HYPHEN-LINE.                                                 ST1364.2
022400     02 FILLER  PIC IS X VALUE IS SPACE.                          ST1364.2
022500     02 FILLER  PIC IS X(65)    VALUE IS "************************ST1364.2
022600-    "*****************************************".                 ST1364.2
022700     02 FILLER  PIC IS X(54)    VALUE IS "************************ST1364.2
022800-    "******************************".                            ST1364.2
022900 01  CCVS-PGM-ID                     PIC X(9)   VALUE             ST1364.2
023000     "ST136A".                                                    ST1364.2
023100 PROCEDURE DIVISION.                                              ST1364.2
023200 CCVS1 SECTION.                                                   ST1364.2
023300 OPEN-FILES.                                                      ST1364.2
023400     OPEN     OUTPUT PRINT-FILE.                                  ST1364.2
023500     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   ST1364.2
023600     MOVE    SPACE TO TEST-RESULTS.                               ST1364.2
023700     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             ST1364.2
023800     GO TO CCVS1-EXIT.                                            ST1364.2
023900 CLOSE-FILES.                                                     ST1364.2
024000     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   ST1364.2
024100 TERMINATE-CCVS.                                                  ST1364.2
024200*    EXIT PROGRAM.                                                ST1364.2
024300*TERMINATE-CALL.                                                  ST1364.2
024400     STOP     RUN.                                                ST1364.2
024500 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         ST1364.2
024600 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           ST1364.2
024700 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          ST1364.2
024800 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      ST1364.2
024900     MOVE "****TEST DELETED****" TO RE-MARK.                      ST1364.2
025000 PRINT-DETAIL.                                                    ST1364.2
025100     IF REC-CT NOT EQUAL TO ZERO                                  ST1364.2
025200             MOVE "." TO PARDOT-X                                 ST1364.2
025300             MOVE REC-CT TO DOTVALUE.                             ST1364.2
025400     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      ST1364.2
025500     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               ST1364.2
025600        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 ST1364.2
025700          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 ST1364.2
025800     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              ST1364.2
025900     MOVE SPACE TO CORRECT-X.                                     ST1364.2
026000     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         ST1364.2
026100     MOVE     SPACE TO RE-MARK.                                   ST1364.2
026200 HEAD-ROUTINE.                                                    ST1364.2
026300     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  ST1364.2
026400     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  ST1364.2
026500     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  ST1364.2
026600     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  ST1364.2
026700 COLUMN-NAMES-ROUTINE.                                            ST1364.2
026800     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           ST1364.2
026900     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1364.2
027000     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        ST1364.2
027100 END-ROUTINE.                                                     ST1364.2
027200     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.ST1364.2
027300 END-RTN-EXIT.                                                    ST1364.2
027400     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1364.2
027500 END-ROUTINE-1.                                                   ST1364.2
027600      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      ST1364.2
027700      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               ST1364.2
027800      ADD PASS-COUNTER TO ERROR-HOLD.                             ST1364.2
027900*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   ST1364.2
028000      MOVE PASS-COUNTER TO CCVS-E-4-1.                            ST1364.2
028100      MOVE ERROR-HOLD TO CCVS-E-4-2.                              ST1364.2
028200      MOVE CCVS-E-4 TO CCVS-E-2-2.                                ST1364.2
028300      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           ST1364.2
028400  END-ROUTINE-12.                                                 ST1364.2
028500      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        ST1364.2
028600     IF       ERROR-COUNTER IS EQUAL TO ZERO                      ST1364.2
028700         MOVE "NO " TO ERROR-TOTAL                                ST1364.2
028800         ELSE                                                     ST1364.2
028900         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       ST1364.2
029000     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           ST1364.2
029100     PERFORM WRITE-LINE.                                          ST1364.2
029200 END-ROUTINE-13.                                                  ST1364.2
029300     IF DELETE-COUNTER IS EQUAL TO ZERO                           ST1364.2
029400         MOVE "NO " TO ERROR-TOTAL  ELSE                          ST1364.2
029500         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      ST1364.2
029600     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   ST1364.2
029700     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           ST1364.2
029800      IF   INSPECT-COUNTER EQUAL TO ZERO                          ST1364.2
029900          MOVE "NO " TO ERROR-TOTAL                               ST1364.2
030000      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   ST1364.2
030100      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            ST1364.2
030200      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          ST1364.2
030300     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           ST1364.2
030400 WRITE-LINE.                                                      ST1364.2
030500     ADD 1 TO RECORD-COUNT.                                       ST1364.2
030600*    IF RECORD-COUNT GREATER 42                                   ST1364.2
030700*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          ST1364.2
030800*        MOVE SPACE TO DUMMY-RECORD                               ST1364.2
030900*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  ST1364.2
031000*        MOVE CCVS-H-1  TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    ST1364.2
031100*        MOVE CCVS-H-2A TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    ST1364.2
031200*        MOVE CCVS-H-2B TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    ST1364.2
031300*        MOVE CCVS-H-3  TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    ST1364.2
031400*        MOVE CCVS-C-1  TO DUMMY-RECORD PERFORM WRT-LN            ST1364.2
031500*        MOVE CCVS-C-2  TO DUMMY-RECORD PERFORM WRT-LN            ST1364.2
031600*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          ST1364.2
031700*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          ST1364.2
031800*        MOVE ZERO TO RECORD-COUNT.                               ST1364.2
031900     PERFORM WRT-LN.                                              ST1364.2
032000 WRT-LN.                                                          ST1364.2
032100     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               ST1364.2
032200     MOVE SPACE TO DUMMY-RECORD.                                  ST1364.2
032300 BLANK-LINE-PRINT.                                                ST1364.2
032400     PERFORM WRT-LN.                                              ST1364.2
032500 FAIL-ROUTINE.                                                    ST1364.2
032600     IF     COMPUTED-X NOT EQUAL TO SPACE                         ST1364.2
032700            GO TO   FAIL-ROUTINE-WRITE.                           ST1364.2
032800     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.ST1364.2
032900     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 ST1364.2
033000     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   ST1364.2
033100     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1364.2
033200     MOVE   SPACES TO INF-ANSI-REFERENCE.                         ST1364.2
033300     GO TO  FAIL-ROUTINE-EX.                                      ST1364.2
033400 FAIL-ROUTINE-WRITE.                                              ST1364.2
033500     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         ST1364.2
033600     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 ST1364.2
033700     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. ST1364.2
033800     MOVE   SPACES TO COR-ANSI-REFERENCE.                         ST1364.2
033900 FAIL-ROUTINE-EX. EXIT.                                           ST1364.2
034000 BAIL-OUT.                                                        ST1364.2
034100     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   ST1364.2
034200     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           ST1364.2
034300 BAIL-OUT-WRITE.                                                  ST1364.2
034400     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  ST1364.2
034500     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 ST1364.2
034600     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1364.2
034700     MOVE   SPACES TO INF-ANSI-REFERENCE.                         ST1364.2
034800 BAIL-OUT-EX. EXIT.                                               ST1364.2
034900 CCVS1-EXIT.                                                      ST1364.2
035000     EXIT.                                                        ST1364.2
035100 SORT-PARA SECTION.                                               ST1364.2
035200 SORT-PARAGRAPH.                                                  ST1364.2
035300     SORT     SORTFILE-2F ON ASCENDING KEY                        ST1364.2
035400              SORTFILE-KEY                                        ST1364.2
035500              INPUT PROCEDURE INPROC                              ST1364.2
035600              GIVING SORTOUT-2F.                                  ST1364.2
035700     GO       TO SORT-TESTS.                                      ST1364.2
035800 INPROC SECTION.                                                  ST1364.2
035900  INPROC-SYSIN.                                                   ST1364.2
036000     MOVE     "RELEASE FROM" TO FEATURE.                          ST1364.2
036100     MOVE     10 TO UTIL-CTR.                                     ST1364.2
036200 SORT-TEST-1.                                                     ST1364.2
036300     MOVE     "SORT-TEST-1" TO PAR-NAME.                          ST1364.2
036400     PERFORM  RELEASE-SORTFILE-REC.                               ST1364.2
036500     IF       WORK-REC EQUAL TO HOLD-REC                          ST1364.2
036600              PERFORM PASS-1 GO TO SORT-WRITE-1.                  ST1364.2
036700 SORT-FAIL-1.                                                     ST1364.2
036800     MOVE     WORK-NON-KEY-1 TO COMPUTED-A.                       ST1364.2
036900     MOVE     HOLD-NON-KEY-1 TO CORRECT-A.                        ST1364.2
037000     PERFORM  FAIL-1.                                             ST1364.2
037100     PERFORM  PRINT-DETAIL-1.                                     ST1364.2
037200     MOVE     WORK-KEY TO COMPUTED-N.                             ST1364.2
037300     MOVE     HOLD-KEY TO CORRECT-N.                              ST1364.2
037400     MOVE     COMMENT-1 TO RE-MARK.                               ST1364.2
037500     PERFORM  PRINT-DETAIL-1.                                     ST1364.2
037600     MOVE     WORK-NON-KEY-2 TO COMPUTED-BREAKDOWN.               ST1364.2
037700     MOVE     HOLD-NON-KEY-2 TO CORRECT-BREAKDOWN.                ST1364.2
037800     MOVE     FIRST-20CM TO COMPUTED-A.                           ST1364.2
037900     MOVE     FIRST-20CR TO CORRECT-A.                            ST1364.2
038000     MOVE     COMMENT-2 TO RE-MARK.                               ST1364.2
038100     PERFORM  PRINT-DETAIL-1.                                     ST1364.2
038200     MOVE     SECOND-20CM TO COMPUTED-A.                          ST1364.2
038300     MOVE     SECOND-20CR TO CORRECT-A.                           ST1364.2
038400     MOVE     COMMENT-3 TO RE-MARK.                               ST1364.2
038500     PERFORM  PRINT-DETAIL-1.                                     ST1364.2
038600     MOVE     THIRD-20CM TO COMPUTED-A.                           ST1364.2
038700     MOVE     THIRD-20CR TO CORRECT-A.                            ST1364.2
038800     MOVE     COMMENT-4 TO RE-MARK.                               ST1364.2
038900 SORT-WRITE-1.                                                    ST1364.2
039000     PERFORM  PRINT-DETAIL-1.                                     ST1364.2
039100 INPROC-CONTINUE.                                                 ST1364.2
039200     PERFORM  RELEASE-SORTFILE-REC 9 TIMES.                       ST1364.2
039300     GO       TO INPROC-EXIT.                                     ST1364.2
039400 RELEASE-SORTFILE-REC.                                            ST1364.2
039500     MOVE     ALL-A TO WORK-NON-KEY-2.                            ST1364.2
039600     MOVE     UTIL-CTR TO WORK-KEY.                               ST1364.2
039700     MOVE     "B" TO WORK-NON-KEY-1.                              ST1364.2
039800     MOVE     ALL-Z TO SORTFILE-NON-KEY-1.                        ST1364.2
039900     MOVE     -12345 TO SORTFILE-KEY.                             ST1364.2
040000     MOVE     ALL-X TO SORTFILE-NON-KEY-2.                        ST1364.2
040100*        NOTE A FALSE RECORD HAS BEEN MOVED TO SORTFILE-REC ---   ST1364.2
040200*             THE RELEASE STATEMENT WHICH FOLLOWS SHOULD CLOBBER  ST1364.2
040300*             IT COMPLETELY.                                      ST1364.2
040400     MOVE     WORK-REC TO HOLD-REC.                               ST1364.2
040500     RELEASE  SORTFILE-REC FROM WORK-REC.                         ST1364.2
040600     SUBTRACT 1 FROM UTIL-CTR.                                    ST1364.2
040700 INSPT-1. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.       ST1364.2
040800 PASS-1.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.         ST1364.2
040900 FAIL-1.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.        ST1364.2
041000 DE-LETE-1.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.    ST1364.2
041100     MOVE "****TEST DELETED****" TO RE-MARK.                      ST1364.2
041200 PRINT-DETAIL-1.                                                  ST1364.2
041300     IF REC-CT NOT EQUAL TO ZERO                                  ST1364.2
041400             MOVE "." TO PARDOT-X                                 ST1364.2
041500             MOVE REC-CT TO DOTVALUE.                             ST1364.2
041600     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE-1.    ST1364.2
041700     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE-1             ST1364.2
041800        PERFORM FAIL-ROUTINE-1 THRU FAIL-ROUTINE-EX-1             ST1364.2
041900          ELSE PERFORM BAIL-OUT-1 THRU BAIL-OUT-EX-1.             ST1364.2
042000     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              ST1364.2
042100     MOVE SPACE TO CORRECT-X.                                     ST1364.2
042200     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         ST1364.2
042300     MOVE     SPACE TO RE-MARK.                                   ST1364.2
042400 WRITE-LINE-1.                                                    ST1364.2
042500     ADD 1 TO RECORD-COUNT.                                       ST1364.2
042600*    IF RECORD-COUNT GREATER 50                                   ST1364.2
042700*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          ST1364.2
042800*        MOVE SPACE TO DUMMY-RECORD                               ST1364.2
042900*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  ST1364.2
043000*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN-1           ST1364.2
043100*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN-1 2 TIMES   ST1364.2
043200*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN-1        ST1364.2
043300*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          ST1364.2
043400*        MOVE ZERO TO RECORD-COUNT.                               ST1364.2
043500     PERFORM WRT-LN-1.                                            ST1364.2
043600 WRT-LN-1.                                                        ST1364.2
043700     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               ST1364.2
043800     MOVE SPACE TO DUMMY-RECORD.                                  ST1364.2
043900 BLANK-LINE-PRINT-1.                                              ST1364.2
044000     PERFORM WRT-LN-1.                                            ST1364.2
044100 FAIL-ROUTINE-1.                                                  ST1364.2
044200     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-RTN-WRITE-1.     ST1364.2
044300     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-RTN-WRITE-1.      ST1364.2
044400     MOVE "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.    ST1364.2
044500     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE-1 2 TIMES.   ST1364.2
044600     GO TO FAIL-ROUTINE-EX-1.                                     ST1364.2
044700 FAIL-RTN-WRITE-1.                                                ST1364.2
044800     MOVE TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE-1         ST1364.2
044900     MOVE TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE-1 2 TIMES. ST1364.2
045000 FAIL-ROUTINE-EX-1. EXIT.                                         ST1364.2
045100 BAIL-OUT-1.                                                      ST1364.2
045200     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE-1.     ST1364.2
045300     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX-1.             ST1364.2
045400 BAIL-OUT-WRITE-1.                                                ST1364.2
045500     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  ST1364.2
045600     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE-1 2 TIMES.   ST1364.2
045700 BAIL-OUT-EX-1. EXIT.                                             ST1364.2
045800 INPROC-EXIT.                                                     ST1364.2
045900     EXIT.                                                        ST1364.2
046000 SORT-TESTS SECTION.                                              ST1364.2
046100 SORT-INIT.                                                       ST1364.2
046200     OPEN     INPUT SORTOUT-2F.                                   ST1364.2
046300     MOVE     ZERO TO UTIL-CTR.                                   ST1364.2
046400 SORT-TEST-2.                                                     ST1364.2
046500     MOVE     "SORT-TEST-2" TO PAR-NAME.                          ST1364.2
046600     PERFORM  READ-SORTOUT.                                       ST1364.2
046700     IF       SORTOUT-NON-KEY-1 NOT EQUAL TO LITERAL-B            ST1364.2
046800              GO TO SORT-FAIL-2.                                  ST1364.2
046900     IF       SORTOUT-KEY NOT EQUAL TO 1                          ST1364.2
047000              GO TO SORT-FAIL-2.                                  ST1364.2
047100     IF       SORTOUT-NON-KEY-2 EQUAL TO ALL-A                    ST1364.2
047200              PERFORM PASS GO TO SORT-WRITE-2.                    ST1364.2
047300 SORT-FAIL-2.                                                     ST1364.2
047400     MOVE     1 TO CORRECT-N.                                     ST1364.2
047500     PERFORM  BREAKDOWN-PARA.                                     ST1364.2
047600 SORT-WRITE-2.                                                    ST1364.2
047700     PERFORM  PRINT-DETAIL.                                       ST1364.2
047800 SORT-TEST-3.                                                     ST1364.2
047900     MOVE     "SORT-TEST-3" TO PAR-NAME.                          ST1364.2
048000     PERFORM  READ-SORTOUT 6 TIMES.                               ST1364.2
048100     IF       SORTOUT-NON-KEY-1 NOT EQUAL TO LITERAL-B            ST1364.2
048200              GO TO SORT-FAIL-3.                                  ST1364.2
048300     IF       SORTOUT-KEY NOT EQUAL TO 7                          ST1364.2
048400              GO TO SORT-FAIL-3.                                  ST1364.2
048500     IF       SORTOUT-NON-KEY-2 EQUAL TO ALL-A                    ST1364.2
048600              PERFORM PASS GO TO SORT-WRITE-3.                    ST1364.2
048700 SORT-FAIL-3.                                                     ST1364.2
048800     MOVE     7 TO CORRECT-N.                                     ST1364.2
048900     PERFORM  BREAKDOWN-PARA.                                     ST1364.2
049000 SORT-WRITE-3.                                                    ST1364.2
049100     PERFORM  PRINT-DETAIL.                                       ST1364.2
049200 SORT-TEST-4.                                                     ST1364.2
049300     MOVE     "SORT-TEST-4" TO PAR-NAME.                          ST1364.2
049400     PERFORM  READ-SORTOUT 3 TIMES.                               ST1364.2
049500     IF       SORTOUT-NON-KEY-1 NOT EQUAL TO LITERAL-B            ST1364.2
049600              GO TO SORT-FAIL-4.                                  ST1364.2
049700     IF       SORTOUT-KEY NOT EQUAL TO 10                         ST1364.2
049800              GO TO SORT-FAIL-4.                                  ST1364.2
049900     IF       SORTOUT-NON-KEY-2 EQUAL TO ALL-A                    ST1364.2
050000              PERFORM PASS GO TO SORT-WRITE-4.                    ST1364.2
050100 SORT-FAIL-4.                                                     ST1364.2
050200     MOVE     10 TO CORRECT-N.                                    ST1364.2
050300     PERFORM  BREAKDOWN-PARA.                                     ST1364.2
050400 SORT-WRITE-4.                                                    ST1364.2
050500     PERFORM  PRINT-DETAIL.                                       ST1364.2
050600 SORT-TEST-5.                                                     ST1364.2
050700     MOVE     "SORT-TEST-5" TO PAR-NAME.                          ST1364.2
050800     READ     SORTOUT-2F AT END                                   ST1364.2
050900              PERFORM PASS GO TO SORT-WRITE-5.                    ST1364.2
051000 SORT-FAIL-5.                                                     ST1364.2
051100     MOVE     SPACE TO ALL-A.                                     ST1364.2
051200     MOVE     SPACE TO LITERAL-B                                  ST1364.2
051300     PERFORM  BREAKDOWN-PARA.                                     ST1364.2
051400     PERFORM  PRINT-DETAIL.                                       ST1364.2
051500     MOVE     "END OF FILE NOT FOUND" TO RE-MARK.                 ST1364.2
051600 SORT-WRITE-5.                                                    ST1364.2
051700     PERFORM  PRINT-DETAIL.                                       ST1364.2
051800 CLOSE-SORT-FILES.                                                ST1364.2
051900     CLOSE    SORTOUT-2F.                                         ST1364.2
052000     GO TO     CCVS-EXIT.                                         ST1364.2
052100 BREAKDOWN-PARA.                                                  ST1364.2
052200     MOVE     SORTOUT-KEY TO COMPUTED-N.                          ST1364.2
052300     MOVE     COMMENT-1 TO RE-MARK.                               ST1364.2
052400     PERFORM  FAIL.                                               ST1364.2
052500     PERFORM  PRINT-DETAIL.                                       ST1364.2
052600     MOVE     SORTOUT-NON-KEY-1 TO COMPUTED-A.                    ST1364.2
052700     MOVE     LITERAL-B TO CORRECT-A.                             ST1364.2
052800     PERFORM  PRINT-DETAIL.                                       ST1364.2
052900     MOVE     SORTOUT-NON-KEY-2 TO COMPUTED-BREAKDOWN.            ST1364.2
053000     MOVE     FIRST-20CM TO COMPUTED-A.                           ST1364.2
053100     MOVE     ALL-A TO CORRECT-A.                                 ST1364.2
053200     MOVE     COMMENT-2 TO RE-MARK.                               ST1364.2
053300     PERFORM  PRINT-DETAIL.                                       ST1364.2
053400     MOVE     SECOND-20CM TO COMPUTED-A.                          ST1364.2
053500     MOVE     ALL-A TO CORRECT-A.                                 ST1364.2
053600     MOVE     COMMENT-3 TO RE-MARK.                               ST1364.2
053700     PERFORM  PRINT-DETAIL.                                       ST1364.2
053800     MOVE     THIRD-20CM TO COMPUTED-A.                           ST1364.2
053900     MOVE     ALL-A TO CORRECT-A.                                 ST1364.2
054000     MOVE     COMMENT-4 TO RE-MARK.                               ST1364.2
054100 READ-SORTOUT.                                                    ST1364.2
054200     READ     SORTOUT-2F AT END GO TO READ-ERROR.                 ST1364.2
054300     ADD      1 TO UTIL-CTR.                                      ST1364.2
054400 READ-ERROR.                                                      ST1364.2
054500     MOVE     UTIL-CTR TO COMPUTED-N.                             ST1364.2
054600     MOVE     10 TO CORRECT-N.                                    ST1364.2
054700     MOVE     "TOO FEW RECORDS IN FILE" TO RE-MARK.               ST1364.2
054800     PERFORM  FAIL.                                               ST1364.2
054900     MOVE     "READ-ERROR" TO PAR-NAME                            ST1364.2
055000     PERFORM  PRINT-DETAIL.                                       ST1364.2
055100     GO       TO CLOSE-SORT-FILES.                                ST1364.2
055200 CCVS-EXIT SECTION.                                               ST1364.2
055300 CCVS-999999.                                                     ST1364.2
055400     GO TO CLOSE-FILES.                                           ST1364.2
      *END-OF,ST136A                                                                  
