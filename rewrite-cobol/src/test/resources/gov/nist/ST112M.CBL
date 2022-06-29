      *HEADER,COBOL,ST112M                                                            
000100 IDENTIFICATION DIVISION.                                         ST1124.2
000200 PROGRAM-ID.                                                      ST1124.2
000300     ST112M.                                                      ST1124.2
000400****************************************************************  ST1124.2
000500*                                                              *  ST1124.2
000600*    VALIDATION FOR:-                                          *  ST1124.2
000700*                                                              *  ST1124.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1124.2
000900*                                                              *  ST1124.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".ST1124.2
001100*                                                              *  ST1124.2
001200****************************************************************  ST1124.2
001300*                                                              *  ST1124.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  ST1124.2
001500*                                                              *  ST1124.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  ST1124.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  ST1124.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  ST1124.2
001900*                                                              *  ST1124.2
002000****************************************************************  ST1124.2
002100 ENVIRONMENT DIVISION.                                            ST1124.2
002200 CONFIGURATION SECTION.                                           ST1124.2
002300 SOURCE-COMPUTER.                                                 ST1124.2
002400     XXXXX082.                                                    ST1124.2
002500 OBJECT-COMPUTER.                                                 ST1124.2
002600     XXXXX083.                                                    ST1124.2
002700 INPUT-OUTPUT SECTION.                                            ST1124.2
002800 FILE-CONTROL.                                                    ST1124.2
002900     SELECT PRINT-FILE ASSIGN TO                                  ST1124.2
003000     XXXXX055.                                                    ST1124.2
003100     SELECT   SORTOUT-1L ASSIGN TO                                ST1124.2
003200     XXXXD006.                                                    ST1124.2
003300 DATA DIVISION.                                                   ST1124.2
003400 FILE SECTION.                                                    ST1124.2
003500 FD  PRINT-FILE.                                                  ST1124.2
003600 01  PRINT-REC PICTURE X(120).                                    ST1124.2
003700 01  DUMMY-RECORD PICTURE X(120).                                 ST1124.2
003800 FD  SORTOUT-1L                                                   ST1124.2
003900     LABEL RECORDS STANDARD                                       ST1124.2
004000C    VALUE OF                                                     ST1124.2
004100C    XXXXX074                                                     ST1124.2
004200C    IS                                                           ST1124.2
004300C    XXXXX079                                                     ST1124.2
004400*    XXXXX069                                                     ST1124.2
004500     DATA RECORD IS SORT-KEY.                                     ST1124.2
004600 01  SORT-KEY                    PIC X(33).                       ST1124.2
004700 WORKING-STORAGE SECTION.                                         ST1124.2
004800 77  UTIL-CTR                    PIC S99999 VALUE ZERO.           ST1124.2
004900 77  COMMENT-SENTENCE  PICTURE X(118) VALUE "ST112M HAS CREATED A ST1124.2
005000-    "3-REEL FILE WHICH WILL BE PASSED TO ST113 FOR SORTING. THIS ST1124.2
005100-    "COMMENT IS THE ONLY OUTPUT FOR ST112".                      ST1124.2
005200 01  TEST-RESULTS.                                                ST1124.2
005300     02 FILLER                   PIC X      VALUE SPACE.          ST1124.2
005400     02 FEATURE                  PIC X(20)  VALUE SPACE.          ST1124.2
005500     02 FILLER                   PIC X      VALUE SPACE.          ST1124.2
005600     02 P-OR-F                   PIC X(5)   VALUE SPACE.          ST1124.2
005700     02 FILLER                   PIC X      VALUE SPACE.          ST1124.2
005800     02  PAR-NAME.                                                ST1124.2
005900       03 FILLER                 PIC X(19)  VALUE SPACE.          ST1124.2
006000       03  PARDOT-X              PIC X      VALUE SPACE.          ST1124.2
006100       03 DOTVALUE               PIC 99     VALUE ZERO.           ST1124.2
006200     02 FILLER                   PIC X(8)   VALUE SPACE.          ST1124.2
006300     02 RE-MARK                  PIC X(61).                       ST1124.2
006400 01  TEST-COMPUTED.                                               ST1124.2
006500     02 FILLER                   PIC X(30)  VALUE SPACE.          ST1124.2
006600     02 FILLER                   PIC X(17)  VALUE                 ST1124.2
006700            "       COMPUTED=".                                   ST1124.2
006800     02 COMPUTED-X.                                               ST1124.2
006900     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          ST1124.2
007000     03 COMPUTED-N               REDEFINES COMPUTED-A             ST1124.2
007100                                 PIC -9(9).9(9).                  ST1124.2
007200     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         ST1124.2
007300     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     ST1124.2
007400     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     ST1124.2
007500     03       CM-18V0 REDEFINES COMPUTED-A.                       ST1124.2
007600         04 COMPUTED-18V0                    PIC -9(18).          ST1124.2
007700         04 FILLER                           PIC X.               ST1124.2
007800     03 FILLER PIC X(50) VALUE SPACE.                             ST1124.2
007900 01  TEST-CORRECT.                                                ST1124.2
008000     02 FILLER PIC X(30) VALUE SPACE.                             ST1124.2
008100     02 FILLER PIC X(17) VALUE "       CORRECT =".                ST1124.2
008200     02 CORRECT-X.                                                ST1124.2
008300     03 CORRECT-A                  PIC X(20) VALUE SPACE.         ST1124.2
008400     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      ST1124.2
008500     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         ST1124.2
008600     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     ST1124.2
008700     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     ST1124.2
008800     03      CR-18V0 REDEFINES CORRECT-A.                         ST1124.2
008900         04 CORRECT-18V0                     PIC -9(18).          ST1124.2
009000         04 FILLER                           PIC X.               ST1124.2
009100     03 FILLER PIC X(2) VALUE SPACE.                              ST1124.2
009200     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     ST1124.2
009300 01  CCVS-C-1.                                                    ST1124.2
009400     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAST1124.2
009500-    "SS  PARAGRAPH-NAME                                          ST1124.2
009600-    "       REMARKS".                                            ST1124.2
009700     02 FILLER                     PIC X(20)    VALUE SPACE.      ST1124.2
009800 01  CCVS-C-2.                                                    ST1124.2
009900     02 FILLER                     PIC X        VALUE SPACE.      ST1124.2
010000     02 FILLER                     PIC X(6)     VALUE "TESTED".   ST1124.2
010100     02 FILLER                     PIC X(15)    VALUE SPACE.      ST1124.2
010200     02 FILLER                     PIC X(4)     VALUE "FAIL".     ST1124.2
010300     02 FILLER                     PIC X(94)    VALUE SPACE.      ST1124.2
010400 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       ST1124.2
010500 01  REC-CT                        PIC 99       VALUE ZERO.       ST1124.2
010600 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       ST1124.2
010700 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       ST1124.2
010800 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       ST1124.2
010900 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       ST1124.2
011000 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       ST1124.2
011100 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       ST1124.2
011200 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      ST1124.2
011300 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       ST1124.2
011400 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     ST1124.2
011500 01  CCVS-H-1.                                                    ST1124.2
011600     02  FILLER                    PIC X(39)    VALUE SPACES.     ST1124.2
011700     02  FILLER                    PIC X(42)    VALUE             ST1124.2
011800     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 ST1124.2
011900     02  FILLER                    PIC X(39)    VALUE SPACES.     ST1124.2
012000 01  CCVS-H-2A.                                                   ST1124.2
012100   02  FILLER                        PIC X(40)  VALUE SPACE.      ST1124.2
012200   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  ST1124.2
012300   02  FILLER                        PIC XXXX   VALUE             ST1124.2
012400     "4.2 ".                                                      ST1124.2
012500   02  FILLER                        PIC X(28)  VALUE             ST1124.2
012600            " COPY - NOT FOR DISTRIBUTION".                       ST1124.2
012700   02  FILLER                        PIC X(41)  VALUE SPACE.      ST1124.2
012800                                                                  ST1124.2
012900 01  CCVS-H-2B.                                                   ST1124.2
013000   02  FILLER                        PIC X(15)  VALUE             ST1124.2
013100            "TEST RESULT OF ".                                    ST1124.2
013200   02  TEST-ID                       PIC X(9).                    ST1124.2
013300   02  FILLER                        PIC X(4)   VALUE             ST1124.2
013400            " IN ".                                               ST1124.2
013500   02  FILLER                        PIC X(12)  VALUE             ST1124.2
013600     " HIGH       ".                                              ST1124.2
013700   02  FILLER                        PIC X(22)  VALUE             ST1124.2
013800            " LEVEL VALIDATION FOR ".                             ST1124.2
013900   02  FILLER                        PIC X(58)  VALUE             ST1124.2
014000     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1124.2
014100 01  CCVS-H-3.                                                    ST1124.2
014200     02  FILLER                      PIC X(34)  VALUE             ST1124.2
014300            " FOR OFFICIAL USE ONLY    ".                         ST1124.2
014400     02  FILLER                      PIC X(58)  VALUE             ST1124.2
014500     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".ST1124.2
014600     02  FILLER                      PIC X(28)  VALUE             ST1124.2
014700            "  COPYRIGHT   1985 ".                                ST1124.2
014800 01  CCVS-E-1.                                                    ST1124.2
014900     02 FILLER                       PIC X(52)  VALUE SPACE.      ST1124.2
015000     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              ST1124.2
015100     02 ID-AGAIN                     PIC X(9).                    ST1124.2
015200     02 FILLER                       PIC X(45)  VALUE SPACES.     ST1124.2
015300 01  CCVS-E-2.                                                    ST1124.2
015400     02  FILLER                      PIC X(31)  VALUE SPACE.      ST1124.2
015500     02  FILLER                      PIC X(21)  VALUE SPACE.      ST1124.2
015600     02 CCVS-E-2-2.                                               ST1124.2
015700         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      ST1124.2
015800         03 FILLER                   PIC X      VALUE SPACE.      ST1124.2
015900         03 ENDER-DESC               PIC X(44)  VALUE             ST1124.2
016000            "ERRORS ENCOUNTERED".                                 ST1124.2
016100 01  CCVS-E-3.                                                    ST1124.2
016200     02  FILLER                      PIC X(22)  VALUE             ST1124.2
016300            " FOR OFFICIAL USE ONLY".                             ST1124.2
016400     02  FILLER                      PIC X(12)  VALUE SPACE.      ST1124.2
016500     02  FILLER                      PIC X(58)  VALUE             ST1124.2
016600     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1124.2
016700     02  FILLER                      PIC X(13)  VALUE SPACE.      ST1124.2
016800     02 FILLER                       PIC X(15)  VALUE             ST1124.2
016900             " COPYRIGHT 1985".                                   ST1124.2
017000 01  CCVS-E-4.                                                    ST1124.2
017100     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      ST1124.2
017200     02 FILLER                       PIC X(4)   VALUE " OF ".     ST1124.2
017300     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      ST1124.2
017400     02 FILLER                       PIC X(40)  VALUE             ST1124.2
017500      "  TESTS WERE EXECUTED SUCCESSFULLY".                       ST1124.2
017600 01  XXINFO.                                                      ST1124.2
017700     02 FILLER                       PIC X(19)  VALUE             ST1124.2
017800            "*** INFORMATION ***".                                ST1124.2
017900     02 INFO-TEXT.                                                ST1124.2
018000       04 FILLER                     PIC X(8)   VALUE SPACE.      ST1124.2
018100       04 XXCOMPUTED                 PIC X(20).                   ST1124.2
018200       04 FILLER                     PIC X(5)   VALUE SPACE.      ST1124.2
018300       04 XXCORRECT                  PIC X(20).                   ST1124.2
018400     02 INF-ANSI-REFERENCE           PIC X(48).                   ST1124.2
018500 01  HYPHEN-LINE.                                                 ST1124.2
018600     02 FILLER  PIC IS X VALUE IS SPACE.                          ST1124.2
018700     02 FILLER  PIC IS X(65)    VALUE IS "************************ST1124.2
018800-    "*****************************************".                 ST1124.2
018900     02 FILLER  PIC IS X(54)    VALUE IS "************************ST1124.2
019000-    "******************************".                            ST1124.2
019100 01  CCVS-PGM-ID                     PIC X(9)   VALUE             ST1124.2
019200     "ST112M".                                                    ST1124.2
019300 PROCEDURE    DIVISION.                                           ST1124.2
019400 CCVS1 SECTION.                                                   ST1124.2
019500 OPEN-FILES.                                                      ST1124.2
019600     OPEN     OUTPUT PRINT-FILE.                                  ST1124.2
019700     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   ST1124.2
019800     MOVE    SPACE TO TEST-RESULTS.                               ST1124.2
019900     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             ST1124.2
020000     GO TO CCVS1-EXIT.                                            ST1124.2
020100 CLOSE-FILES.                                                     ST1124.2
020200     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   ST1124.2
020300 TERMINATE-CCVS.                                                  ST1124.2
020400*    EXIT PROGRAM.                                                ST1124.2
020500*TERMINATE-CALL.                                                  ST1124.2
020600     STOP     RUN.                                                ST1124.2
020700 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         ST1124.2
020800 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           ST1124.2
020900 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          ST1124.2
021000 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      ST1124.2
021100     MOVE "****TEST DELETED****" TO RE-MARK.                      ST1124.2
021200 PRINT-DETAIL.                                                    ST1124.2
021300     IF REC-CT NOT EQUAL TO ZERO                                  ST1124.2
021400             MOVE "." TO PARDOT-X                                 ST1124.2
021500             MOVE REC-CT TO DOTVALUE.                             ST1124.2
021600     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      ST1124.2
021700     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               ST1124.2
021800        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 ST1124.2
021900          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 ST1124.2
022000     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              ST1124.2
022100     MOVE SPACE TO CORRECT-X.                                     ST1124.2
022200     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         ST1124.2
022300     MOVE     SPACE TO RE-MARK.                                   ST1124.2
022400 HEAD-ROUTINE.                                                    ST1124.2
022500     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  ST1124.2
022600     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  ST1124.2
022700     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  ST1124.2
022800     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  ST1124.2
022900 COLUMN-NAMES-ROUTINE.                                            ST1124.2
023000     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           ST1124.2
023100     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1124.2
023200     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        ST1124.2
023300 END-ROUTINE.                                                     ST1124.2
023400     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.ST1124.2
023500 END-RTN-EXIT.                                                    ST1124.2
023600     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1124.2
023700 END-ROUTINE-1.                                                   ST1124.2
023800      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      ST1124.2
023900      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               ST1124.2
024000      ADD PASS-COUNTER TO ERROR-HOLD.                             ST1124.2
024100*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   ST1124.2
024200      MOVE PASS-COUNTER TO CCVS-E-4-1.                            ST1124.2
024300      MOVE ERROR-HOLD TO CCVS-E-4-2.                              ST1124.2
024400      MOVE CCVS-E-4 TO CCVS-E-2-2.                                ST1124.2
024500      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           ST1124.2
024600  END-ROUTINE-12.                                                 ST1124.2
024700      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        ST1124.2
024800     IF       ERROR-COUNTER IS EQUAL TO ZERO                      ST1124.2
024900         MOVE "NO " TO ERROR-TOTAL                                ST1124.2
025000         ELSE                                                     ST1124.2
025100         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       ST1124.2
025200     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           ST1124.2
025300     PERFORM WRITE-LINE.                                          ST1124.2
025400 END-ROUTINE-13.                                                  ST1124.2
025500     IF DELETE-COUNTER IS EQUAL TO ZERO                           ST1124.2
025600         MOVE "NO " TO ERROR-TOTAL  ELSE                          ST1124.2
025700         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      ST1124.2
025800     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   ST1124.2
025900     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           ST1124.2
026000      IF   INSPECT-COUNTER EQUAL TO ZERO                          ST1124.2
026100          MOVE "NO " TO ERROR-TOTAL                               ST1124.2
026200      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   ST1124.2
026300      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            ST1124.2
026400      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          ST1124.2
026500     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           ST1124.2
026600 WRITE-LINE.                                                      ST1124.2
026700     ADD 1 TO RECORD-COUNT.                                       ST1124.2
026800*    IF RECORD-COUNT GREATER 42                                   ST1124.2
026900*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          ST1124.2
027000*        MOVE SPACE TO DUMMY-RECORD                               ST1124.2
027100*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  ST1124.2
027200*        MOVE CCVS-H-1  TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    ST1124.2
027300*        MOVE CCVS-H-2A TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    ST1124.2
027400*        MOVE CCVS-H-2B TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    ST1124.2
027500*        MOVE CCVS-H-3  TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    ST1124.2
027600*        MOVE CCVS-C-1  TO DUMMY-RECORD PERFORM WRT-LN            ST1124.2
027700*        MOVE CCVS-C-2  TO DUMMY-RECORD PERFORM WRT-LN            ST1124.2
027800*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          ST1124.2
027900*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          ST1124.2
028000*        MOVE ZERO TO RECORD-COUNT.                               ST1124.2
028100     PERFORM WRT-LN.                                              ST1124.2
028200 WRT-LN.                                                          ST1124.2
028300     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               ST1124.2
028400     MOVE SPACE TO DUMMY-RECORD.                                  ST1124.2
028500 BLANK-LINE-PRINT.                                                ST1124.2
028600     PERFORM WRT-LN.                                              ST1124.2
028700 FAIL-ROUTINE.                                                    ST1124.2
028800     IF     COMPUTED-X NOT EQUAL TO SPACE                         ST1124.2
028900            GO TO   FAIL-ROUTINE-WRITE.                           ST1124.2
029000     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.ST1124.2
029100     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 ST1124.2
029200     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   ST1124.2
029300     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1124.2
029400     MOVE   SPACES TO INF-ANSI-REFERENCE.                         ST1124.2
029500     GO TO  FAIL-ROUTINE-EX.                                      ST1124.2
029600 FAIL-ROUTINE-WRITE.                                              ST1124.2
029700     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         ST1124.2
029800     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 ST1124.2
029900     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. ST1124.2
030000     MOVE   SPACES TO COR-ANSI-REFERENCE.                         ST1124.2
030100 FAIL-ROUTINE-EX. EXIT.                                           ST1124.2
030200 BAIL-OUT.                                                        ST1124.2
030300     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   ST1124.2
030400     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           ST1124.2
030500 BAIL-OUT-WRITE.                                                  ST1124.2
030600     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  ST1124.2
030700     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 ST1124.2
030800     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1124.2
030900     MOVE   SPACES TO INF-ANSI-REFERENCE.                         ST1124.2
031000 BAIL-OUT-EX. EXIT.                                               ST1124.2
031100 CCVS1-EXIT.                                                      ST1124.2
031200     EXIT.                                                        ST1124.2
031300 SECT-ST112M-001 SECTION.                                         ST1124.2
031400 ST112M-001-01.                                                   ST1124.2
031500     OPEN     OUTPUT  SORTOUT-1L.                                 ST1124.2
031600 BUILD-REEL.                                                      ST1124.2
031700     MOVE    ALL "A" TO SORT-KEY.                                 ST1124.2
031800     PERFORM WRITE-SORT-KEY.                                      ST1124.2
031900     MOVE    ALL "B" TO SORT-KEY.                                 ST1124.2
032000     PERFORM WRITE-SORT-KEY.                                      ST1124.2
032100     MOVE    ALL "C" TO SORT-KEY.                                 ST1124.2
032200     PERFORM WRITE-SORT-KEY.                                      ST1124.2
032300     MOVE    ALL "D" TO SORT-KEY.                                 ST1124.2
032400     PERFORM WRITE-SORT-KEY.                                      ST1124.2
032500     MOVE    ALL "E" TO SORT-KEY.                                 ST1124.2
032600     PERFORM WRITE-SORT-KEY.                                      ST1124.2
032700     MOVE    ALL "F" TO SORT-KEY.                                 ST1124.2
032800     PERFORM WRITE-SORT-KEY.                                      ST1124.2
032900     MOVE    ALL "G" TO SORT-KEY.                                 ST1124.2
033000     PERFORM WRITE-SORT-KEY.                                      ST1124.2
033100     MOVE    ALL "H" TO SORT-KEY.                                 ST1124.2
033200     PERFORM WRITE-SORT-KEY.                                      ST1124.2
033300     MOVE    ALL "I" TO SORT-KEY.                                 ST1124.2
033400     PERFORM WRITE-SORT-KEY.                                      ST1124.2
033500     MOVE    ALL "J" TO SORT-KEY.                                 ST1124.2
033600     PERFORM WRITE-SORT-KEY.                                      ST1124.2
033700     MOVE    ALL "K" TO SORT-KEY.                                 ST1124.2
033800     PERFORM WRITE-SORT-KEY.                                      ST1124.2
033900     MOVE    ALL "L" TO SORT-KEY.                                 ST1124.2
034000     PERFORM WRITE-SORT-KEY.                                      ST1124.2
034100     MOVE    ALL "M" TO SORT-KEY.                                 ST1124.2
034200     PERFORM WRITE-SORT-KEY.                                      ST1124.2
034300     MOVE    ALL "N" TO SORT-KEY.                                 ST1124.2
034400     PERFORM WRITE-SORT-KEY.                                      ST1124.2
034500     MOVE    ALL "O" TO SORT-KEY.                                 ST1124.2
034600     PERFORM WRITE-SORT-KEY.                                      ST1124.2
034700     MOVE    ALL "P" TO SORT-KEY.                                 ST1124.2
034800     PERFORM WRITE-SORT-KEY.                                      ST1124.2
034900     MOVE    ALL "Q" TO SORT-KEY.                                 ST1124.2
035000     PERFORM WRITE-SORT-KEY.                                      ST1124.2
035100     MOVE    ALL "R" TO SORT-KEY.                                 ST1124.2
035200     PERFORM WRITE-SORT-KEY.                                      ST1124.2
035300     MOVE    ALL "S" TO SORT-KEY.                                 ST1124.2
035400     PERFORM WRITE-SORT-KEY.                                      ST1124.2
035500     MOVE    ALL "T" TO SORT-KEY.                                 ST1124.2
035600     PERFORM WRITE-SORT-KEY.                                      ST1124.2
035700     MOVE    ALL "U" TO SORT-KEY.                                 ST1124.2
035800     PERFORM WRITE-SORT-KEY.                                      ST1124.2
035900     MOVE    ALL "V" TO SORT-KEY.                                 ST1124.2
036000     PERFORM WRITE-SORT-KEY.                                      ST1124.2
036100     MOVE    ALL "W" TO SORT-KEY.                                 ST1124.2
036200     PERFORM WRITE-SORT-KEY.                                      ST1124.2
036300     MOVE    ALL "X" TO SORT-KEY.                                 ST1124.2
036400     PERFORM WRITE-SORT-KEY.                                      ST1124.2
036500     MOVE    ALL "Y" TO SORT-KEY.                                 ST1124.2
036600     PERFORM WRITE-SORT-KEY.                                      ST1124.2
036700     MOVE    ALL "Z" TO SORT-KEY.                                 ST1124.2
036800     PERFORM WRITE-SORT-KEY.                                      ST1124.2
036900 CLOSE-REEL.                                                      ST1124.2
037000     ADD     1 TO UTIL-CTR.                                       ST1124.2
037100     IF      UTIL-CTR = 3                                         ST1124.2
037200             GO TO   ST112M-002-01.                               ST1124.2
037300*    CLOSE   SORTOUT-1L REEL.                                     ST1124.2
037400*    THE FOLLOWING OPTION CARDS APPEAR ONLY FOR *OPT3 = I (NO     ST1124.2
037500*    CLOSE REEL)                                                  ST1124.2
037600     GO TO   BUILD-REEL.                                          ST1124.2
037700 ST112M-002-01.                                                   ST1124.2
037800     MOVE    COMMENT-SENTENCE TO PRINT-REC.                       ST1124.2
037900     PERFORM WRITE-LINE.                                          ST1124.2
038000     CLOSE   SORTOUT-1L.                                          ST1124.2
038100     GO TO   CCVS-EXIT.                                           ST1124.2
038200 WRITE-SORT-KEY.                                                  ST1124.2
038300     WRITE   SORT-KEY.                                            ST1124.2
038400 CCVS-EXIT SECTION.                                               ST1124.2
038500 CCVS-999999.                                                     ST1124.2
038600     GO TO   CLOSE-FILES.                                         ST1124.2
      *END-OF,ST112M                                                                  
