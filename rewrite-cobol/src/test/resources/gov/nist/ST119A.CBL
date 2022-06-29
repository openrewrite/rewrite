      *HEADER,COBOL,ST119A                                                            
000100 IDENTIFICATION DIVISION.                                         ST1194.2
000200 PROGRAM-ID.                                                      ST1194.2
000300     ST119A.                                                      ST1194.2
000400****************************************************************  ST1194.2
000500*                                                              *  ST1194.2
000600*    VALIDATION FOR:-                                          *  ST1194.2
000700*                                                              *  ST1194.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1194.2
000900*                                                              *  ST1194.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".ST1194.2
001100*                                                              *  ST1194.2
001200****************************************************************  ST1194.2
001300*                                                              *  ST1194.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  ST1194.2
001500*                                                              *  ST1194.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  ST1194.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  ST1194.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  ST1194.2
001900*                                                              *  ST1194.2
002000****************************************************************  ST1194.2
002100 ENVIRONMENT DIVISION.                                            ST1194.2
002200 CONFIGURATION SECTION.                                           ST1194.2
002300 SOURCE-COMPUTER.                                                 ST1194.2
002400     XXXXX082.                                                    ST1194.2
002500 OBJECT-COMPUTER.                                                 ST1194.2
002600     XXXXX083.                                                    ST1194.2
002700 INPUT-OUTPUT SECTION.                                            ST1194.2
002800 FILE-CONTROL.                                                    ST1194.2
002900     SELECT PRINT-FILE ASSIGN TO                                  ST1194.2
003000     XXXXX055.                                                    ST1194.2
003100     SELECT SORTFILE-1A ASSIGN TO                                 ST1194.2
003200     XXXXX027.                                                    ST1194.2
003300     SELECT SORTOUT-1A ASSIGN TO                                  ST1194.2
003400     XXXXP001.                                                    ST1194.2
003500 DATA DIVISION.                                                   ST1194.2
003600 FILE SECTION.                                                    ST1194.2
003700 FD  PRINT-FILE.                                                  ST1194.2
003800 01  PRINT-REC PICTURE X(120).                                    ST1194.2
003900 01  DUMMY-RECORD PICTURE X(120).                                 ST1194.2
004000 SD  SORTFILE-1A                                                  ST1194.2
004100     DATA RECORD IS S-RECORD.                                     ST1194.2
004200 01  S-RECORD.                                                    ST1194.2
004300     02  KEYS-GROUP.                                              ST1194.2
004400         03  KEY-1 PICTURE 9.                                     ST1194.2
004500         03  KEY-2 PICTURE 99.                                    ST1194.2
004600         03  KEY-3 PICTURE 999.                                   ST1194.2
004700         03  KEY-4 PICTURE 9999.                                  ST1194.2
004800         03  KEY-5 PICTURE 9(5).                                  ST1194.2
004900     02  RDF-KEYS REDEFINES KEYS-GROUP PICTURE 9(15).             ST1194.2
005000     02  FILLER PICTURE X(105).                                   ST1194.2
005100 FD  SORTOUT-1A                                                   ST1194.2
005200     BLOCK CONTAINS 10 RECORDS                                    ST1194.2
005300     LABEL RECORDS ARE STANDARD                                   ST1194.2
005400C    VALUE OF                                                     ST1194.2
005500C    XXXXX074                                                     ST1194.2
005600C    IS                                                           ST1194.2
005700C    XXXXX075                                                     ST1194.2
005800*    XXXXX069                                                     ST1194.2
005900     DATA RECORD IS SORTED.                                       ST1194.2
006000 01  SORTED PICTURE X(120).                                       ST1194.2
006100 WORKING-STORAGE SECTION.                                         ST1194.2
006200 77  COMMENT-SENTENCE PIC X(120) VALUE " THE FILE BUILT IN ST119A ST1194.2
006300-    "IS SORTED IN ST120A.  ST120A DOES NOT PRODUCE A REPORT - THEST1194.2
006400-    " RESULTS ARE CHECKED IN ST121A.".                           ST1194.2
006500 77  WRK-XN-00001-1  PIC X.                                       ST1194.2
006600 77  WRK-XN-00001-2  PIC X.                                       ST1194.2
006700 77  WRK-XN-00001-3  PIC X.                                       ST1194.2
006800 77  C0 PICTURE 9 VALUE 0.                                        ST1194.2
006900 77  C1 PICTURE 9 VALUE 1.                                        ST1194.2
007000 77  C2 PICTURE 9 VALUE 2.                                        ST1194.2
007100 77  C6 PICTURE 9 VALUE 6.                                        ST1194.2
007200 77  C3 PICTURE 9 VALUE 3.                                        ST1194.2
007300 01  WKEYS-GROUP.                                                 ST1194.2
007400     02  WKEY-1 PICTURE 9.                                        ST1194.2
007500     02  WKEY-2 PICTURE 99.                                       ST1194.2
007600     02  WKEY-3 PICTURE 999.                                      ST1194.2
007700     02  WKEY-4 PICTURE 9999.                                     ST1194.2
007800     02  WKEY-5 PICTURE 9(5).                                     ST1194.2
007900 01  WKEYS-RDF REDEFINES WKEYS-GROUP PICTURE 9(15).               ST1194.2
008000 01  TEST-RESULTS.                                                ST1194.2
008100     02 FILLER                   PIC X      VALUE SPACE.          ST1194.2
008200     02 FEATURE                  PIC X(20)  VALUE SPACE.          ST1194.2
008300     02 FILLER                   PIC X      VALUE SPACE.          ST1194.2
008400     02 P-OR-F                   PIC X(5)   VALUE SPACE.          ST1194.2
008500     02 FILLER                   PIC X      VALUE SPACE.          ST1194.2
008600     02  PAR-NAME.                                                ST1194.2
008700       03 FILLER                 PIC X(19)  VALUE SPACE.          ST1194.2
008800       03  PARDOT-X              PIC X      VALUE SPACE.          ST1194.2
008900       03 DOTVALUE               PIC 99     VALUE ZERO.           ST1194.2
009000     02 FILLER                   PIC X(8)   VALUE SPACE.          ST1194.2
009100     02 RE-MARK                  PIC X(61).                       ST1194.2
009200 01  TEST-COMPUTED.                                               ST1194.2
009300     02 FILLER                   PIC X(30)  VALUE SPACE.          ST1194.2
009400     02 FILLER                   PIC X(17)  VALUE                 ST1194.2
009500            "       COMPUTED=".                                   ST1194.2
009600     02 COMPUTED-X.                                               ST1194.2
009700     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          ST1194.2
009800     03 COMPUTED-N               REDEFINES COMPUTED-A             ST1194.2
009900                                 PIC -9(9).9(9).                  ST1194.2
010000     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         ST1194.2
010100     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     ST1194.2
010200     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     ST1194.2
010300     03       CM-18V0 REDEFINES COMPUTED-A.                       ST1194.2
010400         04 COMPUTED-18V0                    PIC -9(18).          ST1194.2
010500         04 FILLER                           PIC X.               ST1194.2
010600     03 FILLER PIC X(50) VALUE SPACE.                             ST1194.2
010700 01  TEST-CORRECT.                                                ST1194.2
010800     02 FILLER PIC X(30) VALUE SPACE.                             ST1194.2
010900     02 FILLER PIC X(17) VALUE "       CORRECT =".                ST1194.2
011000     02 CORRECT-X.                                                ST1194.2
011100     03 CORRECT-A                  PIC X(20) VALUE SPACE.         ST1194.2
011200     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      ST1194.2
011300     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         ST1194.2
011400     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     ST1194.2
011500     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     ST1194.2
011600     03      CR-18V0 REDEFINES CORRECT-A.                         ST1194.2
011700         04 CORRECT-18V0                     PIC -9(18).          ST1194.2
011800         04 FILLER                           PIC X.               ST1194.2
011900     03 FILLER PIC X(2) VALUE SPACE.                              ST1194.2
012000     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     ST1194.2
012100 01  CCVS-C-1.                                                    ST1194.2
012200     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAST1194.2
012300-    "SS  PARAGRAPH-NAME                                          ST1194.2
012400-    "       REMARKS".                                            ST1194.2
012500     02 FILLER                     PIC X(20)    VALUE SPACE.      ST1194.2
012600 01  CCVS-C-2.                                                    ST1194.2
012700     02 FILLER                     PIC X        VALUE SPACE.      ST1194.2
012800     02 FILLER                     PIC X(6)     VALUE "TESTED".   ST1194.2
012900     02 FILLER                     PIC X(15)    VALUE SPACE.      ST1194.2
013000     02 FILLER                     PIC X(4)     VALUE "FAIL".     ST1194.2
013100     02 FILLER                     PIC X(94)    VALUE SPACE.      ST1194.2
013200 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       ST1194.2
013300 01  REC-CT                        PIC 99       VALUE ZERO.       ST1194.2
013400 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       ST1194.2
013500 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       ST1194.2
013600 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       ST1194.2
013700 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       ST1194.2
013800 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       ST1194.2
013900 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       ST1194.2
014000 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      ST1194.2
014100 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       ST1194.2
014200 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     ST1194.2
014300 01  CCVS-H-1.                                                    ST1194.2
014400     02  FILLER                    PIC X(39)    VALUE SPACES.     ST1194.2
014500     02  FILLER                    PIC X(42)    VALUE             ST1194.2
014600     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 ST1194.2
014700     02  FILLER                    PIC X(39)    VALUE SPACES.     ST1194.2
014800 01  CCVS-H-2A.                                                   ST1194.2
014900   02  FILLER                        PIC X(40)  VALUE SPACE.      ST1194.2
015000   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  ST1194.2
015100   02  FILLER                        PIC XXXX   VALUE             ST1194.2
015200     "4.2 ".                                                      ST1194.2
015300   02  FILLER                        PIC X(28)  VALUE             ST1194.2
015400            " COPY - NOT FOR DISTRIBUTION".                       ST1194.2
015500   02  FILLER                        PIC X(41)  VALUE SPACE.      ST1194.2
015600                                                                  ST1194.2
015700 01  CCVS-H-2B.                                                   ST1194.2
015800   02  FILLER                        PIC X(15)  VALUE             ST1194.2
015900            "TEST RESULT OF ".                                    ST1194.2
016000   02  TEST-ID                       PIC X(9).                    ST1194.2
016100   02  FILLER                        PIC X(4)   VALUE             ST1194.2
016200            " IN ".                                               ST1194.2
016300   02  FILLER                        PIC X(12)  VALUE             ST1194.2
016400     " HIGH       ".                                              ST1194.2
016500   02  FILLER                        PIC X(22)  VALUE             ST1194.2
016600            " LEVEL VALIDATION FOR ".                             ST1194.2
016700   02  FILLER                        PIC X(58)  VALUE             ST1194.2
016800     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1194.2
016900 01  CCVS-H-3.                                                    ST1194.2
017000     02  FILLER                      PIC X(34)  VALUE             ST1194.2
017100            " FOR OFFICIAL USE ONLY    ".                         ST1194.2
017200     02  FILLER                      PIC X(58)  VALUE             ST1194.2
017300     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".ST1194.2
017400     02  FILLER                      PIC X(28)  VALUE             ST1194.2
017500            "  COPYRIGHT   1985 ".                                ST1194.2
017600 01  CCVS-E-1.                                                    ST1194.2
017700     02 FILLER                       PIC X(52)  VALUE SPACE.      ST1194.2
017800     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              ST1194.2
017900     02 ID-AGAIN                     PIC X(9).                    ST1194.2
018000     02 FILLER                       PIC X(45)  VALUE SPACES.     ST1194.2
018100 01  CCVS-E-2.                                                    ST1194.2
018200     02  FILLER                      PIC X(31)  VALUE SPACE.      ST1194.2
018300     02  FILLER                      PIC X(21)  VALUE SPACE.      ST1194.2
018400     02 CCVS-E-2-2.                                               ST1194.2
018500         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      ST1194.2
018600         03 FILLER                   PIC X      VALUE SPACE.      ST1194.2
018700         03 ENDER-DESC               PIC X(44)  VALUE             ST1194.2
018800            "ERRORS ENCOUNTERED".                                 ST1194.2
018900 01  CCVS-E-3.                                                    ST1194.2
019000     02  FILLER                      PIC X(22)  VALUE             ST1194.2
019100            " FOR OFFICIAL USE ONLY".                             ST1194.2
019200     02  FILLER                      PIC X(12)  VALUE SPACE.      ST1194.2
019300     02  FILLER                      PIC X(58)  VALUE             ST1194.2
019400     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1194.2
019500     02  FILLER                      PIC X(13)  VALUE SPACE.      ST1194.2
019600     02 FILLER                       PIC X(15)  VALUE             ST1194.2
019700             " COPYRIGHT 1985".                                   ST1194.2
019800 01  CCVS-E-4.                                                    ST1194.2
019900     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      ST1194.2
020000     02 FILLER                       PIC X(4)   VALUE " OF ".     ST1194.2
020100     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      ST1194.2
020200     02 FILLER                       PIC X(40)  VALUE             ST1194.2
020300      "  TESTS WERE EXECUTED SUCCESSFULLY".                       ST1194.2
020400 01  XXINFO.                                                      ST1194.2
020500     02 FILLER                       PIC X(19)  VALUE             ST1194.2
020600            "*** INFORMATION ***".                                ST1194.2
020700     02 INFO-TEXT.                                                ST1194.2
020800       04 FILLER                     PIC X(8)   VALUE SPACE.      ST1194.2
020900       04 XXCOMPUTED                 PIC X(20).                   ST1194.2
021000       04 FILLER                     PIC X(5)   VALUE SPACE.      ST1194.2
021100       04 XXCORRECT                  PIC X(20).                   ST1194.2
021200     02 INF-ANSI-REFERENCE           PIC X(48).                   ST1194.2
021300 01  HYPHEN-LINE.                                                 ST1194.2
021400     02 FILLER  PIC IS X VALUE IS SPACE.                          ST1194.2
021500     02 FILLER  PIC IS X(65)    VALUE IS "************************ST1194.2
021600-    "*****************************************".                 ST1194.2
021700     02 FILLER  PIC IS X(54)    VALUE IS "************************ST1194.2
021800-    "******************************".                            ST1194.2
021900 01  CCVS-PGM-ID                     PIC X(9)   VALUE             ST1194.2
022000     "ST119A".                                                    ST1194.2
022100 PROCEDURE DIVISION.                                              ST1194.2
022200 CCVS1 SECTION.                                                   ST1194.2
022300 OPEN-FILES.                                                      ST1194.2
022400     OPEN     OUTPUT PRINT-FILE.                                  ST1194.2
022500     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   ST1194.2
022600     MOVE    SPACE TO TEST-RESULTS.                               ST1194.2
022700     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             ST1194.2
022800     GO TO CCVS1-EXIT.                                            ST1194.2
022900 CLOSE-FILES.                                                     ST1194.2
023000     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   ST1194.2
023100 TERMINATE-CCVS.                                                  ST1194.2
023200*    EXIT PROGRAM.                                                ST1194.2
023300*TERMINATE-CALL.                                                  ST1194.2
023400     STOP     RUN.                                                ST1194.2
023500 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         ST1194.2
023600 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           ST1194.2
023700 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          ST1194.2
023800 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      ST1194.2
023900     MOVE "****TEST DELETED****" TO RE-MARK.                      ST1194.2
024000 PRINT-DETAIL.                                                    ST1194.2
024100     IF REC-CT NOT EQUAL TO ZERO                                  ST1194.2
024200             MOVE "." TO PARDOT-X                                 ST1194.2
024300             MOVE REC-CT TO DOTVALUE.                             ST1194.2
024400     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      ST1194.2
024500     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               ST1194.2
024600        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 ST1194.2
024700          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 ST1194.2
024800     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              ST1194.2
024900     MOVE SPACE TO CORRECT-X.                                     ST1194.2
025000     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         ST1194.2
025100     MOVE     SPACE TO RE-MARK.                                   ST1194.2
025200 HEAD-ROUTINE.                                                    ST1194.2
025300     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  ST1194.2
025400     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  ST1194.2
025500     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  ST1194.2
025600     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  ST1194.2
025700 COLUMN-NAMES-ROUTINE.                                            ST1194.2
025800     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           ST1194.2
025900     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1194.2
026000     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        ST1194.2
026100 END-ROUTINE.                                                     ST1194.2
026200     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.ST1194.2
026300 END-RTN-EXIT.                                                    ST1194.2
026400     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1194.2
026500 END-ROUTINE-1.                                                   ST1194.2
026600      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      ST1194.2
026700      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               ST1194.2
026800      ADD PASS-COUNTER TO ERROR-HOLD.                             ST1194.2
026900*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   ST1194.2
027000      MOVE PASS-COUNTER TO CCVS-E-4-1.                            ST1194.2
027100      MOVE ERROR-HOLD TO CCVS-E-4-2.                              ST1194.2
027200      MOVE CCVS-E-4 TO CCVS-E-2-2.                                ST1194.2
027300      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           ST1194.2
027400  END-ROUTINE-12.                                                 ST1194.2
027500      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        ST1194.2
027600     IF       ERROR-COUNTER IS EQUAL TO ZERO                      ST1194.2
027700         MOVE "NO " TO ERROR-TOTAL                                ST1194.2
027800         ELSE                                                     ST1194.2
027900         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       ST1194.2
028000     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           ST1194.2
028100     PERFORM WRITE-LINE.                                          ST1194.2
028200 END-ROUTINE-13.                                                  ST1194.2
028300     IF DELETE-COUNTER IS EQUAL TO ZERO                           ST1194.2
028400         MOVE "NO " TO ERROR-TOTAL  ELSE                          ST1194.2
028500         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      ST1194.2
028600     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   ST1194.2
028700     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           ST1194.2
028800      IF   INSPECT-COUNTER EQUAL TO ZERO                          ST1194.2
028900          MOVE "NO " TO ERROR-TOTAL                               ST1194.2
029000      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   ST1194.2
029100      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            ST1194.2
029200      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          ST1194.2
029300     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           ST1194.2
029400 WRITE-LINE.                                                      ST1194.2
029500     ADD 1 TO RECORD-COUNT.                                       ST1194.2
029600*    IF RECORD-COUNT GREATER 42                                   ST1194.2
029700*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          ST1194.2
029800*        MOVE SPACE TO DUMMY-RECORD                               ST1194.2
029900*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  ST1194.2
030000*        MOVE CCVS-H-1  TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    ST1194.2
030100*        MOVE CCVS-H-2A TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    ST1194.2
030200*        MOVE CCVS-H-2B TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    ST1194.2
030300*        MOVE CCVS-H-3  TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    ST1194.2
030400*        MOVE CCVS-C-1  TO DUMMY-RECORD PERFORM WRT-LN            ST1194.2
030500*        MOVE CCVS-C-2  TO DUMMY-RECORD PERFORM WRT-LN            ST1194.2
030600*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          ST1194.2
030700*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          ST1194.2
030800*        MOVE ZERO TO RECORD-COUNT.                               ST1194.2
030900     PERFORM WRT-LN.                                              ST1194.2
031000 WRT-LN.                                                          ST1194.2
031100     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               ST1194.2
031200     MOVE SPACE TO DUMMY-RECORD.                                  ST1194.2
031300 BLANK-LINE-PRINT.                                                ST1194.2
031400     PERFORM WRT-LN.                                              ST1194.2
031500 FAIL-ROUTINE.                                                    ST1194.2
031600     IF     COMPUTED-X NOT EQUAL TO SPACE                         ST1194.2
031700            GO TO   FAIL-ROUTINE-WRITE.                           ST1194.2
031800     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.ST1194.2
031900     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 ST1194.2
032000     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   ST1194.2
032100     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1194.2
032200     MOVE   SPACES TO INF-ANSI-REFERENCE.                         ST1194.2
032300     GO TO  FAIL-ROUTINE-EX.                                      ST1194.2
032400 FAIL-ROUTINE-WRITE.                                              ST1194.2
032500     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         ST1194.2
032600     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 ST1194.2
032700     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. ST1194.2
032800     MOVE   SPACES TO COR-ANSI-REFERENCE.                         ST1194.2
032900 FAIL-ROUTINE-EX. EXIT.                                           ST1194.2
033000 BAIL-OUT.                                                        ST1194.2
033100     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   ST1194.2
033200     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           ST1194.2
033300 BAIL-OUT-WRITE.                                                  ST1194.2
033400     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  ST1194.2
033500     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 ST1194.2
033600     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1194.2
033700     MOVE   SPACES TO INF-ANSI-REFERENCE.                         ST1194.2
033800 BAIL-OUT-EX. EXIT.                                               ST1194.2
033900 CCVS1-EXIT.                                                      ST1194.2
034000     EXIT.                                                        ST1194.2
034100                                                                  ST1194.2
034200 SORT-INIT SECTION.                                               ST1194.2
034300 MAIN-SORT-PARAGRAPH.                                             ST1194.2
034400     SORT SORTFILE-1A                                             ST1194.2
034500     ON ASCENDING KEY KEY-1                                       ST1194.2
034600     ON DESCENDING KEY KEY-2                                      ST1194.2
034700     ON ASCENDING KEY KEY-3                                       ST1194.2
034800     DESCENDING KEY-4 KEY-5                                       ST1194.2
034900     INPUT PROCEDURE IS IN-1 THROUGH IN-EXIT                      ST1194.2
035000     OUTPUT PROCEDURE IS OUT-PROC-1 THRU SORT-END.                ST1194.2
035100                                                                  ST1194.2
035200 INTERNAL-OUTPUT-PROC-CODE SECTION.                               ST1194.2
035300*=================================                                ST1194.2
035400 INT-INIT-1.                                                      ST1194.2
035500*    ===--> ACCESSING OF CODE WITHIN THE OUTPUT PROCEDURE <--===  ST1194.2
035600     MOVE   "XI-19 4.4.4 GR(10)" TO ANSI-REFERENCE.               ST1194.2
035700     MOVE   "INT-TEST-1"       TO PAR-NAME.                       ST1194.2
035800     MOVE   "INTNL CODE PERFORMED" TO FEATURE.                    ST1194.2
035900     MOVE    SPACE TO WRK-XN-00001-1.                             ST1194.2
036000     PERFORM INTERNAL-CODE-1.                                     ST1194.2
036100     GO TO   INT-TEST-1.                                          ST1194.2
036200 INT-DELETE-1.                                                    ST1194.2
036300     PERFORM DE-LETE.                                             ST1194.2
036400     PERFORM PRINT-DETAIL.                                        ST1194.2
036500     GO TO   INT-INIT-2.                                          ST1194.2
036600 INT-TEST-1.                                                      ST1194.2
036700     IF      WRK-XN-00001-1 = "C"                                 ST1194.2
036800             PERFORM PASS                                         ST1194.2
036900             PERFORM PRINT-DETAIL                                 ST1194.2
037000     ELSE                                                         ST1194.2
037100             MOVE   "INTERNAL OUTPUT PROC CODE NOT PERFORMED"     ST1194.2
037200                  TO RE-MARK                                      ST1194.2
037300             MOVE   "C" TO CORRECT-X                              ST1194.2
037400             MOVE    WRK-XN-00001-1 TO COMPUTED-X                 ST1194.2
037500             PERFORM FAIL                                         ST1194.2
037600             PERFORM PRINT-DETAIL.                                ST1194.2
037700                                                                  ST1194.2
037800 INT-INIT-2.                                                      ST1194.2
037900*    ===--> ACCESSING OF CODE WITHIN THE OUTPUT PROCEDURE <--===  ST1194.2
038000     MOVE   "XI-19 4.4.4 GR(10)"  TO ANSI-REFERENCE.              ST1194.2
038100     MOVE   "INT-TEST-2-1"      TO PAR-NAME.                      ST1194.2
038200     MOVE   "GO TO INTERNAL CODE" TO FEATURE.                     ST1194.2
038300     MOVE    SPACE TO WRK-XN-00001-1.                             ST1194.2
038400     MOVE   "Y"    TO WRK-XN-00001-2.                             ST1194.2
038500     MOVE    1 TO REC-CT.                                         ST1194.2
038600     GO TO   INTERNAL-CODE-2.                                     ST1194.2
038700 INT-FAIL-2-1.                                                    ST1194.2
038800     MOVE   "X"    TO WRK-XN-00001-2.                             ST1194.2
038900     GO TO   INT-TEST-2-2.                                        ST1194.2
039000 INT-DELETE-2-1.                                                  ST1194.2
039100     PERFORM DE-LETE.                                             ST1194.2
039200     PERFORM PRINT-DETAIL.                                        ST1194.2
039300     GO TO   INT-INIT-3.                                          ST1194.2
039400 INT-TEST-2-1.                                                    ST1194.2
039500     IF      WRK-XN-00001-2 = "Y"                                 ST1194.2
039600             PERFORM PASS                                         ST1194.2
039700             PERFORM PRINT-DETAIL                                 ST1194.2
039800             GO TO   INT-TEST-2-2.                                ST1194.2
039900     MOVE   "GO TO INTERNAL CODE ERROR OR RETURN FROM CODE ERROR" ST1194.2
040000          TO RE-MARK.                                             ST1194.2
040100     MOVE   "Y" TO CORRECT-X.                                     ST1194.2
040200     MOVE    WRK-XN-00001-2 TO COMPUTED-X.                        ST1194.2
040300     PERFORM FAIL.                                                ST1194.2
040400     PERFORM PRINT-DETAIL.                                        ST1194.2
040500 INT-TEST-2-2.                                                    ST1194.2
040600     MOVE   "INT-TEST-2-1"      TO PAR-NAME.                      ST1194.2
040700     ADD     1 TO REC-CT.                                         ST1194.2
040800     IF      WRK-XN-00001-1 = "D"                                 ST1194.2
040900             PERFORM PASS                                         ST1194.2
041000             PERFORM PRINT-DETAIL                                 ST1194.2
041100     ELSE                                                         ST1194.2
041200             MOVE   "GO TO INTERNAL OUTPUT PROC. CODE ERROR"      ST1194.2
041300                  TO RE-MARK                                      ST1194.2
041400             MOVE   "D" TO CORRECT-X                              ST1194.2
041500             MOVE    WRK-XN-00001-1 TO COMPUTED-X                 ST1194.2
041600             PERFORM FAIL                                         ST1194.2
041700             PERFORM PRINT-DETAIL.                                ST1194.2
041800                                                                  ST1194.2
041900 INTERNAL-INPUT-PROC-CODE SECTION.                                ST1194.2
042000*================================                                 ST1194.2
042100 INT-INIT-3.                                                      ST1194.2
042200*    ===--> ACCESSING OF CODE WITHIN THE INPUT PROCEDURE <--===   ST1194.2
042300     MOVE   "XI-19 4.4.4 GR(7)" TO ANSI-REFERENCE.                ST1194.2
042400     MOVE   "INT-TEST-3"       TO PAR-NAME.                       ST1194.2
042500     MOVE   "INTNL CODE PERFORMED" TO FEATURE.                    ST1194.2
042600     MOVE    SPACE TO WRK-XN-00001-1.                             ST1194.2
042700     PERFORM INTERNAL-CODE-3.                                     ST1194.2
042800     GO TO   INT-TEST-3.                                          ST1194.2
042900 INT-DELETE-3.                                                    ST1194.2
043000     PERFORM DE-LETE.                                             ST1194.2
043100     PERFORM PRINT-DETAIL.                                        ST1194.2
043200     GO TO   INT-INIT-4.                                          ST1194.2
043300 INT-TEST-3.                                                      ST1194.2
043400     IF      WRK-XN-00001-1 = "L"                                 ST1194.2
043500             PERFORM PASS                                         ST1194.2
043600             PERFORM PRINT-DETAIL                                 ST1194.2
043700     ELSE                                                         ST1194.2
043800             MOVE   "INTERNAL INPUT PROC CODE NOT PERFORMED"      ST1194.2
043900                  TO RE-MARK                                      ST1194.2
044000             MOVE   "L" TO CORRECT-X                              ST1194.2
044100             MOVE    WRK-XN-00001-1 TO COMPUTED-X                 ST1194.2
044200             PERFORM FAIL                                         ST1194.2
044300             PERFORM PRINT-DETAIL.                                ST1194.2
044400                                                                  ST1194.2
044500 INT-INIT-4.                                                      ST1194.2
044600*    ===--> ACCESSING OF CODE WITHIN THE OUTPUT PROCEDURE <--===  ST1194.2
044700     MOVE   "XI-19 4.4.4 GR(7)"  TO ANSI-REFERENCE.               ST1194.2
044800     MOVE   "INT-TEST-4-1"      TO PAR-NAME.                      ST1194.2
044900     MOVE   "GO TO INTERNAL CODE" TO FEATURE.                     ST1194.2
045000     MOVE    SPACE TO WRK-XN-00001-1.                             ST1194.2
045100     MOVE   "N"    TO WRK-XN-00001-2.                             ST1194.2
045200     MOVE    1 TO REC-CT.                                         ST1194.2
045300     GO TO   INTERNAL-CODE-4.                                     ST1194.2
045400 INT-FAIL-4-1.                                                    ST1194.2
045500     MOVE   "O"    TO WRK-XN-00001-2.                             ST1194.2
045600     GO TO   INT-TEST-4-1.                                        ST1194.2
045700 INT-DELETE-4.                                                    ST1194.2
045800     PERFORM DE-LETE.                                             ST1194.2
045900     PERFORM PRINT-DETAIL.                                        ST1194.2
046000     GO TO   I-2.                                                 ST1194.2
046100 INT-TEST-4-1.                                                    ST1194.2
046200     IF      WRK-XN-00001-2 = "N"                                 ST1194.2
046300             PERFORM PASS                                         ST1194.2
046400             PERFORM PRINT-DETAIL                                 ST1194.2
046500             GO TO   INT-TEST-4-2.                                ST1194.2
046600     MOVE   "GO TO INTERNAL CODE ERROR OR RETURN FROM CODE ERROR" ST1194.2
046700          TO RE-MARK.                                             ST1194.2
046800     MOVE   "N" TO CORRECT-X.                                     ST1194.2
046900     MOVE    WRK-XN-00001-2 TO COMPUTED-X.                        ST1194.2
047000     PERFORM FAIL.                                                ST1194.2
047100     PERFORM PRINT-DETAIL.                                        ST1194.2
047200 INT-TEST-4-2.                                                    ST1194.2
047300     MOVE   "INT-TEST-4-2"      TO PAR-NAME.                      ST1194.2
047400     ADD     1 TO REC-CT.                                         ST1194.2
047500     IF      WRK-XN-00001-1 = "M"                                 ST1194.2
047600             PERFORM PASS                                         ST1194.2
047700             PERFORM PRINT-DETAIL                                 ST1194.2
047800     ELSE                                                         ST1194.2
047900             MOVE   "GO TO INTERNAL OUTPUT PROC. CODE ERROR"      ST1194.2
048000                  TO RE-MARK                                      ST1194.2
048100             MOVE   "M" TO CORRECT-X                              ST1194.2
048200             MOVE    WRK-XN-00001-1 TO COMPUTED-X                 ST1194.2
048300             PERFORM FAIL                                         ST1194.2
048400             PERFORM PRINT-DETAIL.                                ST1194.2
048500                                                                  ST1194.2
048600 I-2.                                                             ST1194.2
048700     GO TO   CCVS-EXIT.                                           ST1194.2
048800                                                                  ST1194.2
048900 IN-1.                                                            ST1194.2
049000 INPT-INIT-1.                                                     ST1194.2
049100*    ===--> ACCESSING OF CODE OUTSIDE THE INPUT PROCEDURE <--===  ST1194.2
049200     MOVE   "XI-19 4.4.4 GR(7)" TO ANSI-REFERENCE.                ST1194.2
049300     MOVE   "INPT-TEST-1"       TO PAR-NAME.                      ST1194.2
049400     MOVE   "PERFORM EXTNL CODE" TO FEATURE.                      ST1194.2
049500     MOVE    SPACE TO WRK-XN-00001-1.                             ST1194.2
049600     PERFORM EXTERNAL-CODE-3.                                     ST1194.2
049700     GO TO   INPT-TEST-1.                                         ST1194.2
049800 INPT-DELETE-1.                                                   ST1194.2
049900     PERFORM DE-LETE.                                             ST1194.2
050000     PERFORM PRINT-DETAIL.                                        ST1194.2
050100     GO TO   INPT-INIT-2.                                         ST1194.2
050200 INPT-TEST-1.                                                     ST1194.2
050300     IF      WRK-XN-00001-1 = "J"                                 ST1194.2
050400             PERFORM PASS                                         ST1194.2
050500             PERFORM PRINT-DETAIL                                 ST1194.2
050600     ELSE                                                         ST1194.2
050700             MOVE   "EXTERNAL CODE NOT PERFORMED FROM INPUT PROC" ST1194.2
050800                  TO RE-MARK                                      ST1194.2
050900             MOVE   "J" TO CORRECT-X                              ST1194.2
051000             MOVE    WRK-XN-00001-1 TO COMPUTED-X                 ST1194.2
051100             PERFORM FAIL                                         ST1194.2
051200             PERFORM PRINT-DETAIL.                                ST1194.2
051300                                                                  ST1194.2
051400 INPT-INIT-2.                                                     ST1194.2
051500     MOVE   "XI-19 4.4.4 GR(7)"  TO ANSI-REFERENCE.               ST1194.2
051600     MOVE   "INPT-TEST-2-1"      TO PAR-NAME.                     ST1194.2
051700     MOVE   "GO TO EXTERNAL CODE" TO FEATURE.                     ST1194.2
051800     MOVE    SPACE TO WRK-XN-00001-1.                             ST1194.2
051900     MOVE   "S"    TO WRK-XN-00001-2.                             ST1194.2
052000     MOVE    1 TO REC-CT.                                         ST1194.2
052100     GO TO   EXTERNAL-CODE-4.                                     ST1194.2
052200 INPT-FAIL-2-1.                                                   ST1194.2
052300     MOVE   "W"    TO WRK-XN-00001-1.                             ST1194.2
052400     GO TO   INPT-TEST-2-1.                                       ST1194.2
052500 INPT-DELETE-2.                                                   ST1194.2
052600     PERFORM DE-LETE.                                             ST1194.2
052700     PERFORM PRINT-DETAIL.                                        ST1194.2
052800     GO TO   IN-2.                                                ST1194.2
052900 INPT-TEST-2-1.                                                   ST1194.2
053000     IF      WRK-XN-00001-2 = "S"                                 ST1194.2
053100             PERFORM PASS                                         ST1194.2
053200             PERFORM PRINT-DETAIL                                 ST1194.2
053300             GO TO   INPT-TEST-2-2.                               ST1194.2
053400     MOVE   "GO TO EXTERNAL CODE ERROR OR RETURN FROM CODE ERROR" ST1194.2
053500          TO RE-MARK.                                             ST1194.2
053600     MOVE   "S" TO CORRECT-X.                                     ST1194.2
053700     MOVE    WRK-XN-00001-2 TO COMPUTED-X.                        ST1194.2
053800     PERFORM FAIL.                                                ST1194.2
053900     PERFORM PRINT-DETAIL.                                        ST1194.2
054000 INPT-TEST-2-2.                                                   ST1194.2
054100     MOVE   "INPT-TEST-2-1"      TO PAR-NAME.                     ST1194.2
054200     ADD     1 TO REC-CT.                                         ST1194.2
054300     IF      WRK-XN-00001-1 = "K"                                 ST1194.2
054400             PERFORM PASS                                         ST1194.2
054500             PERFORM PRINT-DETAIL                                 ST1194.2
054600     ELSE                                                         ST1194.2
054700             MOVE   "GO TO EXTERNAL CODE ERROR"                   ST1194.2
054800                  TO RE-MARK                                      ST1194.2
054900             MOVE   "K" TO CORRECT-X                              ST1194.2
055000             MOVE    WRK-XN-00001-1 TO COMPUTED-X                 ST1194.2
055100             PERFORM FAIL                                         ST1194.2
055200             PERFORM PRINT-DETAIL.                                ST1194.2
055300     GO TO   IN-2.                                                ST1194.2
055400*                                                                 ST1194.2
055500*    THE FOLLOWING CODE IS ACCESSED FROM OUTSIDE THE INPUT        ST1194.2
055600*    PROCEEDURE:                                                  ST1194.2
055700*                                                                 ST1194.2
055800 INTERNAL-CODE-3.                                                 ST1194.2
055900     MOVE   "L" TO WRK-XN-00001-1.                                ST1194.2
056000 INTERNAL-CODE-4.                                                 ST1194.2
056100     MOVE   "M" TO WRK-XN-00001-1.                                ST1194.2
056200     GO TO   INT-TEST-4-1.                                        ST1194.2
056300*                                                                 ST1194.2
056400*    NOTE.                                                        ST1194.2
056500*       KEYS 1 AND 3 THRU 5 WILL VARY IN VALUE BETWEEN 1 AND 2.   ST1194.2
056600*       KEY 2 VARIES FROM 1 THRU 6. THUS 96 RECORDS ARE CREATED   ST1194.2
056700*       IN REVERSE SEQUENCE OF SORTING ORDER. TWO RECORDS ARE     ST1194.2
056800*       ADDED TO EACH END OF THE SORTED STRING FOR HI-LOW CONTROL.ST1194.2
056900*       THE SORT STATEMENT TESTS THE SERIES AND THRU OPTIONS WITH ST1194.2
057000*       INCLUSION AND OMISSION OF OPTIONAL WORDS. THE SORT        ST1194.2
057100*       STATEMENT REPRESENTS BASIC SORTING PERMITTED BY LEVEL 1 OFST1194.2
057200*       THE SORT MODULE.                                          ST1194.2
057300 IN-2.                                                            ST1194.2
057400     MOVE 900009000000000 TO RDF-KEYS.                            ST1194.2
057500     RELEASE S-RECORD.                                            ST1194.2
057600     MOVE 009000000900009 TO RDF-KEYS.                            ST1194.2
057700     RELEASE S-RECORD.                                            ST1194.2
057800     MOVE 900008000000000 TO RDF-KEYS.                            ST1194.2
057900     RELEASE S-RECORD.                                            ST1194.2
058000     MOVE 009000000900008 TO RDF-KEYS.                            ST1194.2
058100     RELEASE S-RECORD.                                            ST1194.2
058200*    NOTE HI-LOW CONTROL RECORDS DONE.                            ST1194.2
058300     MOVE 300003000000000 TO WKEYS-RDF.                           ST1194.2
058400 IN-3.                                                            ST1194.2
058500     PERFORM IN-4 2 TIMES.                                        ST1194.2
058600     GO TO IN-EXIT.                                               ST1194.2
058700 IN-4.                                                            ST1194.2
058800     SUBTRACT C1 FROM WKEY-1.                                     ST1194.2
058900     PERFORM IN-5 6 TIMES.                                        ST1194.2
059000 IN-5.                                                            ST1194.2
059100     IF WKEY-2 IS EQUAL TO C6                                     ST1194.2
059200         MOVE C0 TO WKEY-2.                                       ST1194.2
059300     ADD C1 TO WKEY-2.                                            ST1194.2
059400     PERFORM IN-6 2 TIMES.                                        ST1194.2
059500 IN-6.                                                            ST1194.2
059600     IF WKEY-3 IS EQUAL TO C1                                     ST1194.2
059700         MOVE C3 TO WKEY-3.                                       ST1194.2
059800     SUBTRACT C1 FROM WKEY-3.                                     ST1194.2
059900     PERFORM IN-7 2 TIMES.                                        ST1194.2
060000 IN-7.                                                            ST1194.2
060100     IF WKEY-4 IS EQUAL TO C2                                     ST1194.2
060200         MOVE C0 TO WKEY-4.                                       ST1194.2
060300     ADD C1 TO WKEY-4.                                            ST1194.2
060400     PERFORM IN-8 2 TIMES.                                        ST1194.2
060500 IN-8.                                                            ST1194.2
060600     IF WKEY-5 IS EQUAL TO C2                                     ST1194.2
060700         MOVE C0 TO WKEY-5.                                       ST1194.2
060800     ADD C1 TO WKEY-5.                                            ST1194.2
060900     MOVE WKEYS-RDF TO RDF-KEYS.                                  ST1194.2
061000     RELEASE S-RECORD.                                            ST1194.2
061100 IN-EXIT.                                                         ST1194.2
061200     EXIT.                                                        ST1194.2
061300                                                                  ST1194.2
061400 OUT-PROC-1.                                                      ST1194.2
061500     MOVE    SPACES TO PAR-NAME.                                  ST1194.2
061600     MOVE    SPACES TO FEATURE.                                   ST1194.2
061700     MOVE    ZERO TO REC-CT.                                      ST1194.2
061800     MOVE   "XI-19 4.4.4 GR(7)" TO ANSI-REFERENCE.                ST1194.2
061900     MOVE   "ST119 GENERATES OUTPUT" TO RE-MARK.                  ST1194.2
062000     PERFORM PRINT-DETAIL.                                        ST1194.2
062100     MOVE   "WHICH AFFECTS PROGRAMS" TO RE-MARK.                  ST1194.2
062200     PERFORM PRINT-DETAIL.                                        ST1194.2
062300     MOVE   "ST120 AND ST121."       TO RE-MARK.                  ST1194.2
062400     PERFORM PRINT-DETAIL.                                        ST1194.2
062500     MOVE   "SORT --- FIVE KEYS" TO FEATURE.                      ST1194.2
062600     OPEN    OUTPUT SORTOUT-1A.                                   ST1194.2
062700 SORT-TEST-1.                                                     ST1194.2
062800     PERFORM  RET-1.                                              ST1194.2
062900     IF       RDF-KEYS EQUAL TO 009000000900009                   ST1194.2
063000              PERFORM PASS GO TO SORT-WRITE-1.                    ST1194.2
063100     GO       TO SORT-FAIL-1.                                     ST1194.2
063200 SORT-DELETE-1.                                                   ST1194.2
063300     PERFORM  DE-LETE.                                            ST1194.2
063400     GO       TO SORT-WRITE-1.                                    ST1194.2
063500 SORT-FAIL-1.                                                     ST1194.2
063600     MOVE     RDF-KEYS TO COMPUTED-18V0.                          ST1194.2
063700     MOVE     009000000900009 TO CORRECT-18V0.                    ST1194.2
063800     PERFORM  FAIL.                                               ST1194.2
063900 SORT-WRITE-1.                                                    ST1194.2
064000     MOVE     "SORT-TEST-1 " TO PAR-NAME.                         ST1194.2
064100     PERFORM  PRINT-DETAIL.                                       ST1194.2
064200 SORT-TEST-2.                                                     ST1194.2
064300     PERFORM  RET-1.                                              ST1194.2
064400     IF       RDF-KEYS EQUAL TO 009000000900008                   ST1194.2
064500              PERFORM PASS GO TO SORT-WRITE-2.                    ST1194.2
064600     GO       TO SORT-FAIL-2.                                     ST1194.2
064700 SORT-DELETE-2.                                                   ST1194.2
064800     PERFORM  DE-LETE.                                            ST1194.2
064900     GO       TO SORT-WRITE-2.                                    ST1194.2
065000 SORT-FAIL-2.                                                     ST1194.2
065100     MOVE     RDF-KEYS TO COMPUTED-18V0.                          ST1194.2
065200     MOVE     009000000900009 TO CORRECT-18V0.                    ST1194.2
065300     PERFORM  FAIL.                                               ST1194.2
065400 SORT-WRITE-2.                                                    ST1194.2
065500     MOVE     "SORT-TEST-2 " TO PAR-NAME.                         ST1194.2
065600     PERFORM  PRINT-DETAIL.                                       ST1194.2
065700 SORT-TEST-3.                                                     ST1194.2
065800     PERFORM  RET-1.                                              ST1194.2
065900     IF       RDF-KEYS EQUAL TO 106001000200002                   ST1194.2
066000              PERFORM PASS GO TO SORT-WRITE-3.                    ST1194.2
066100     GO       TO SORT-FAIL-3.                                     ST1194.2
066200 SORT-DELETE-3.                                                   ST1194.2
066300     PERFORM  DE-LETE.                                            ST1194.2
066400     GO       TO SORT-WRITE-3.                                    ST1194.2
066500 SORT-FAIL-3.                                                     ST1194.2
066600     MOVE     RDF-KEYS TO COMPUTED-18V0.                          ST1194.2
066700     MOVE     106001000200002  TO CORRECT-18V0.                   ST1194.2
066800     PERFORM  FAIL.                                               ST1194.2
066900 SORT-WRITE-3.                                                    ST1194.2
067000     MOVE     "SORT-TEST-3 " TO PAR-NAME.                         ST1194.2
067100     PERFORM  PRINT-DETAIL.                                       ST1194.2
067200                                                                  ST1194.2
067300 SORT-TEST-4.                                                     ST1194.2
067400     PERFORM  RET-2 48 TIMES.                                     ST1194.2
067500     IF       RDF-KEYS EQUAL TO 206001000200002                   ST1194.2
067600              PERFORM PASS GO TO SORT-WRITE-4.                    ST1194.2
067700     GO       TO SORT-FAIL-4.                                     ST1194.2
067800 SORT-DELETE-4.                                                   ST1194.2
067900     PERFORM  DE-LETE.                                            ST1194.2
068000     GO       TO SORT-WRITE-4.                                    ST1194.2
068100 SORT-FAIL-4.                                                     ST1194.2
068200     MOVE     RDF-KEYS TO COMPUTED-18V0.                          ST1194.2
068300     MOVE    206001000200002 TO CORRECT-18V0.                     ST1194.2
068400     PERFORM  FAIL.                                               ST1194.2
068500 SORT-WRITE-4.                                                    ST1194.2
068600     MOVE     "SORT-TEST-4 " TO PAR-NAME.                         ST1194.2
068700     PERFORM  PRINT-DETAIL.                                       ST1194.2
068800 SORT-TEST-5.                                                     ST1194.2
068900     PERFORM  RET-2 40 TIMES.                                     ST1194.2
069000     IF       RDF-KEYS EQUAL TO 201001000200002                   ST1194.2
069100              PERFORM PASS GO TO SORT-WRITE-5.                    ST1194.2
069200     GO       TO SORT-FAIL-5.                                     ST1194.2
069300 SORT-DELETE-5.                                                   ST1194.2
069400     PERFORM  DE-LETE.                                            ST1194.2
069500     GO       TO SORT-WRITE-5.                                    ST1194.2
069600 SORT-FAIL-5.                                                     ST1194.2
069700     MOVE     RDF-KEYS TO COMPUTED-18V0.                          ST1194.2
069800     MOVE     201001000200002 TO CORRECT-18V0.                    ST1194.2
069900     PERFORM  FAIL.                                               ST1194.2
070000 SORT-WRITE-5.                                                    ST1194.2
070100     MOVE     "SORT-TEST-5 " TO PAR-NAME.                         ST1194.2
070200     PERFORM  PRINT-DETAIL.                                       ST1194.2
070300 SORT-TEST-6.                                                     ST1194.2
070400     PERFORM  RET-2.                                              ST1194.2
070500     PERFORM  RET-3 THRU RET-3-EXIT.                              ST1194.2
070600     PERFORM  RET-4 THRU RET-4-EXIT.                              ST1194.2
070700     PERFORM  RET-5 THRU RET-5-EXIT.                              ST1194.2
070800     PERFORM  RET-6 THRU RET-6-EXIT.                              ST1194.2
070900     PERFORM  RET-7 THRU RET-7-EXIT.                              ST1194.2
071000     PERFORM  RET-8 THRU RET-8-EXIT.                              ST1194.2
071100     IF       RDF-KEYS EQUAL TO 201002000100001                   ST1194.2
071200              PERFORM PASS GO TO SORT-WRITE-6.                    ST1194.2
071300     GO       TO SORT-FAIL-6.                                     ST1194.2
071400 SORT-DELETE-6.                                                   ST1194.2
071500     PERFORM  DE-LETE.                                            ST1194.2
071600     GO       TO SORT-WRITE-6.                                    ST1194.2
071700 SORT-FAIL-6.                                                     ST1194.2
071800     MOVE     RDF-KEYS TO COMPUTED-18V0.                          ST1194.2
071900     MOVE     201002000100001 TO CORRECT-18V0.                    ST1194.2
072000     PERFORM  FAIL.                                               ST1194.2
072100 SORT-WRITE-6.                                                    ST1194.2
072200     MOVE     "SORT-TEST-6 " TO PAR-NAME.                         ST1194.2
072300     PERFORM  PRINT-DETAIL.                                       ST1194.2
072400 SORT-TEST-7.                                                     ST1194.2
072500     PERFORM  RET-2.                                              ST1194.2
072600     IF       RDF-KEYS EQUAL TO 900008000000000                   ST1194.2
072700              PERFORM PASS GO TO SORT-WRITE-7.                    ST1194.2
072800     GO       TO SORT-FAIL-7.                                     ST1194.2
072900 SORT-DELETE-7.                                                   ST1194.2
073000     PERFORM  DE-LETE.                                            ST1194.2
073100     GO       TO SORT-WRITE-7.                                    ST1194.2
073200 SORT-FAIL-7.                                                     ST1194.2
073300     MOVE     RDF-KEYS TO COMPUTED-18V0.                          ST1194.2
073400     MOVE     900008000000000 TO CORRECT-18V0.                    ST1194.2
073500     PERFORM  FAIL.                                               ST1194.2
073600 SORT-WRITE-7.                                                    ST1194.2
073700     MOVE     "SORT-TEST-7 " TO PAR-NAME.                         ST1194.2
073800     PERFORM  PRINT-DETAIL.                                       ST1194.2
073900 SORT-TEST-8.                                                     ST1194.2
074000     PERFORM  RET-2.                                              ST1194.2
074100     IF       RDF-KEYS EQUAL TO 900009000000000                   ST1194.2
074200              PERFORM PASS GO TO SORT-WRITE-8.                    ST1194.2
074300     GO       TO SORT-FAIL-8.                                     ST1194.2
074400 SORT-DELETE-8.                                                   ST1194.2
074500     PERFORM  DE-LETE.                                            ST1194.2
074600     GO       TO SORT-WRITE-8.                                    ST1194.2
074700 SORT-FAIL-8.                                                     ST1194.2
074800     MOVE     RDF-KEYS TO COMPUTED-18V0.                          ST1194.2
074900     MOVE     900009000000000 TO CORRECT-18V0.                    ST1194.2
075000     PERFORM  FAIL.                                               ST1194.2
075100 SORT-WRITE-8.                                                    ST1194.2
075200     MOVE     "SORT-TEST-8 " TO PAR-NAME.                         ST1194.2
075300     PERFORM  PRINT-DETAIL.                                       ST1194.2
075400 SORT-TEST-9.                                                     ST1194.2
075500     RETURN   SORTFILE-1A AT END                                  ST1194.2
075600              PERFORM PASS GO TO SORT-WRITE-9.                    ST1194.2
075700     GO       TO SORT-FAIL-9.                                     ST1194.2
075800 SORT-DELETE-9.                                                   ST1194.2
075900     PERFORM  DE-LETE.                                            ST1194.2
076000     GO       TO SORT-WRITE-9.                                    ST1194.2
076100 SORT-FAIL-9.                                                     ST1194.2
076200     MOVE     RDF-KEYS TO COMPUTED-18V0.                          ST1194.2
076300     MOVE     "END OF FILE NOT FOUND" TO RE-MARK.                 ST1194.2
076400     PERFORM  FAIL.                                               ST1194.2
076500 SORT-WRITE-9.                                                    ST1194.2
076600     MOVE     "SORT-TEST-9 " TO PAR-NAME.                         ST1194.2
076700     PERFORM  PRINT-DETAIL.                                       ST1194.2
076800 SORT-INIT-10.                                                    ST1194.2
076900     MOVE   "XI-19 4.4.4 GR(10)" TO ANSI-REFERENCE.               ST1194.2
077000     MOVE   "SORT-TEST-10"       TO PAR-NAME.                     ST1194.2
077100     MOVE   "PERFORM EXTNL CODE" TO FEATURE.                      ST1194.2
077200     MOVE    SPACE TO WRK-XN-00001-1.                             ST1194.2
077300     PERFORM EXTERNAL-CODE-1.                                     ST1194.2
077400     GO TO   SORT-TEST-10.                                        ST1194.2
077500 SORT-DELETE-10.                                                  ST1194.2
077600     PERFORM DE-LETE.                                             ST1194.2
077700     PERFORM PRINT-DETAIL.                                        ST1194.2
077800     GO TO   SORT-INIT-11.                                        ST1194.2
077900 SORT-TEST-10.                                                    ST1194.2
078000     IF      WRK-XN-00001-1 = "A"                                 ST1194.2
078100             PERFORM PASS                                         ST1194.2
078200             PERFORM PRINT-DETAIL                                 ST1194.2
078300     ELSE                                                         ST1194.2
078400             MOVE   "EXTERNAL CODE NOT PERFORMED FROM OUTPUT PROC"ST1194.2
078500                  TO RE-MARK                                      ST1194.2
078600             MOVE   "A" TO CORRECT-X                              ST1194.2
078700             MOVE    WRK-XN-00001-1 TO COMPUTED-X                 ST1194.2
078800             PERFORM FAIL                                         ST1194.2
078900             PERFORM PRINT-DETAIL.                                ST1194.2
079000                                                                  ST1194.2
079100 SORT-INIT-11.                                                    ST1194.2
079200     MOVE   "XI-19 4.4.4 GR(10)"  TO ANSI-REFERENCE.              ST1194.2
079300     MOVE   "SORT-TEST-11-1"      TO PAR-NAME.                    ST1194.2
079400     MOVE   "GO TO EXTERNAL CODE" TO FEATURE.                     ST1194.2
079500     MOVE    SPACE TO WRK-XN-00001-1.                             ST1194.2
079600     MOVE   "D"    TO WRK-XN-00001-2.                             ST1194.2
079700     MOVE    1 TO REC-CT.                                         ST1194.2
079800     GO TO   EXTERNAL-CODE-2.                                     ST1194.2
079900 SORT-FAIL-11-1.                                                  ST1194.2
080000     MOVE   "Z"    TO WRK-XN-00001-2.                             ST1194.2
080100     GO TO   SORT-TEST-11-1.                                      ST1194.2
080200 SORT-DELETE-11.                                                  ST1194.2
080300     PERFORM DE-LETE.                                             ST1194.2
080400     PERFORM PRINT-DETAIL.                                        ST1194.2
080500     GO TO   SORT-INIT-11.                                        ST1194.2
080600 SORT-TEST-11-1.                                                  ST1194.2
080700     IF      WRK-XN-00001-2 = "D"                                 ST1194.2
080800             PERFORM PASS                                         ST1194.2
080900             PERFORM PRINT-DETAIL                                 ST1194.2
081000             GO TO   SORT-TEST-11-2.                              ST1194.2
081100     MOVE   "GO TO EXTERNAL CODE ERROR OR RETURN FROM CODE ERROR" ST1194.2
081200          TO RE-MARK.                                             ST1194.2
081300     MOVE   "D" TO CORRECT-X.                                     ST1194.2
081400     MOVE    WRK-XN-00001-2 TO COMPUTED-X.                        ST1194.2
081500     PERFORM FAIL.                                                ST1194.2
081600     PERFORM PRINT-DETAIL.                                        ST1194.2
081700 SORT-TEST-11-2.                                                  ST1194.2
081800     MOVE   "SORT-TEST-11-2"      TO PAR-NAME.                    ST1194.2
081900     ADD     1 TO REC-CT.                                         ST1194.2
082000     IF      WRK-XN-00001-1 = "B"                                 ST1194.2
082100             PERFORM PASS                                         ST1194.2
082200             PERFORM PRINT-DETAIL                                 ST1194.2
082300     ELSE                                                         ST1194.2
082400             MOVE   "GO TO EXTERNAL CODE ERROR"                   ST1194.2
082500                  TO RE-MARK                                      ST1194.2
082600             MOVE   "B" TO CORRECT-X                              ST1194.2
082700             MOVE    WRK-XN-00001-1 TO COMPUTED-X                 ST1194.2
082800             PERFORM FAIL                                         ST1194.2
082900             PERFORM PRINT-DETAIL.                                ST1194.2
083000     GO TO   SORT-END.                                            ST1194.2
083100*                                                                 ST1194.2
083200*    THE FOLLOWING CODE IS ACCESSED FROM OUTSIDE THE OUTPUT       ST1194.2
083300*    PROCEEDURE:                                                  ST1194.2
083400*                                                                 ST1194.2
083500 INTERNAL-CODE-1.                                                 ST1194.2
083600     MOVE  "C" TO WRK-XN-00001-1.                                 ST1194.2
083700 INTERNAL-CODE-2.                                                 ST1194.2
083800     MOVE  "D" TO WRK-XN-00001-1.                                 ST1194.2
083900     GO TO  INT-TEST-2-1.                                         ST1194.2
084000                                                                  ST1194.2
084100 SORT-END.                                                        ST1194.2
084200     CLOSE  SORTOUT-1A.                                           ST1194.2
084300                                                                  ST1194.2
084400 BAD-FILE.                                                        ST1194.2
084500     MOVE   "BAD-FILE" TO PAR-NAME.                               ST1194.2
084600     PERFORM FAIL.                                                ST1194.2
084700     MOVE   "END OF FILE PREMATURELY" TO RE-MARK.                 ST1194.2
084800     PERFORM PRINT-DETAIL.                                        ST1194.2
084900     MOVE   "REACHED, PREVIOUS TEST WAS" TO RE-MARK.              ST1194.2
085000     PERFORM PRINT-DETAIL.                                        ST1194.2
085100     MOVE   "THE LAST SUCCESSFUL TEST." TO RE-MARK.               ST1194.2
085200     PERFORM PRINT-DETAIL.                                        ST1194.2
085300     MOVE    SPACE TO FEATURE.                                    ST1194.2
085400     GO TO   CCVS-EXIT.                                           ST1194.2
085500 RET-1.                                                           ST1194.2
085600     RETURN  SORTFILE-1A RECORD AT END GO TO BAD-FILE.            ST1194.2
085700     MOVE     S-RECORD TO SORTED.                                 ST1194.2
085800     WRITE    SORTED.                                             ST1194.2
085900*    NOTE     THE RETURN VERB WITH ALL OPTIONAL WORDS.            ST1194.2
086000 RET-2.                                                           ST1194.2
086100     RETURN  SORTFILE-1A           END GO TO BAD-FILE.            ST1194.2
086200     MOVE    S-RECORD TO SORTED.                                  ST1194.2
086300     WRITE   SORTED.                                              ST1194.2
086400*    NOTE    THE RETURN VERB WITHOUT OPTIONAL WORDS.              ST1194.2
086500 RET-3.                                                           ST1194.2
086600     MOVE   "XI-14 4.3.4 GR(2) & (4)" TO ANSI-REFERENCE.          ST1194.2
086700     MOVE   "RET-3" TO PAR-NAME.                                  ST1194.2
086800     RETURN  SORTFILE-1A                                          ST1194.2
086900             AT END  GO TO BAD-FILE                               ST1194.2
087000         NOT AT END                                               ST1194.2
087100             PERFORM PASS                                         ST1194.2
087200             PERFORM PRINT-DETAIL                                 ST1194.2
087300             MOVE    S-RECORD TO SORTED                           ST1194.2
087400             WRITE   SORTED                                       ST1194.2
087500             GO TO RET-3-EXIT.                                    ST1194.2
087600 RET-3-EXIT.                                                      ST1194.2
087700     EXIT.                                                        ST1194.2
087800 RET-4.                                                           ST1194.2
087900     MOVE   "XI-14 4.3.4 GR(2) & (4)" TO ANSI-REFERENCE.          ST1194.2
088000     MOVE   "RET-4" TO PAR-NAME.                                  ST1194.2
088100     MOVE   "A"     TO WRK-XN-00001-3.                            ST1194.2
088200     RETURN  SORTFILE-1A                                          ST1194.2
088300             AT END  GO TO BAD-FILE                               ST1194.2
088400     END-RETURN                                                   ST1194.2
088500     MOVE   "S" TO WRK-XN-00001-3.                                ST1194.2
088600     MOVE    S-RECORD TO SORTED.                                  ST1194.2
088700     WRITE   SORTED.                                              ST1194.2
088800     IF      WRK-XN-00001-3 = "S"                                 ST1194.2
088900             PERFORM PASS                                         ST1194.2
089000             PERFORM PRINT-DETAIL                                 ST1194.2
089100     ELSE                                                         ST1194.2
089200             MOVE   "SCOPE DELIMITER IGNORED" TO RE-MARK          ST1194.2
089300             MOVE   "S" TO CORRECT-X                              ST1194.2
089400             MOVE    WRK-XN-00001-3 TO COMPUTED-X                 ST1194.2
089500             PERFORM FAIL                                         ST1194.2
089600             PERFORM PRINT-DETAIL.                                ST1194.2
089700 RET-4-EXIT.                                                      ST1194.2
089800     EXIT.                                                        ST1194.2
089900 RET-5.                                                           ST1194.2
090000     MOVE   "XI-14 4.3.4 GR(2) & (4)" TO ANSI-REFERENCE.          ST1194.2
090100     MOVE   "RET-5" TO PAR-NAME.                                  ST1194.2
090200     RETURN  SORTFILE-1A                                          ST1194.2
090300             AT END                                               ST1194.2
090400             PERFORM FAIL                                         ST1194.2
090500             PERFORM PRINT-DETAIL                                 ST1194.2
090600             GO TO   BAD-FILE                                     ST1194.2
090700         NOT AT END                                               ST1194.2
090800             PERFORM PASS                                         ST1194.2
090900             PERFORM PRINT-DETAIL                                 ST1194.2
091000             MOVE    S-RECORD TO SORTED                           ST1194.2
091100             WRITE   SORTED.                                      ST1194.2
091200 RET-5-EXIT.                                                      ST1194.2
091300     EXIT.                                                        ST1194.2
091400 RET-6.                                                           ST1194.2
091500     MOVE   "XI-14 4.3.4 GR(2) & (4)" TO ANSI-REFERENCE.          ST1194.2
091600     MOVE   "RET-6" TO PAR-NAME.                                  ST1194.2
091700     MOVE   "V" TO WRK-XN-00001-3.                                ST1194.2
091800     RETURN  SORTFILE-1A                                          ST1194.2
091900             AT END                                               ST1194.2
092000             GO TO BAD-FILE                                       ST1194.2
092100     END-RETURN                                                   ST1194.2
092200     MOVE   "W"     TO WRK-XN-00001-3.                            ST1194.2
092300     MOVE    S-RECORD TO SORTED.                                  ST1194.2
092400     WRITE   SORTED.                                              ST1194.2
092500     IF      WRK-XN-00001-3 = "W"                                 ST1194.2
092600             PERFORM PASS                                         ST1194.2
092700             PERFORM PRINT-DETAIL                                 ST1194.2
092800     ELSE                                                         ST1194.2
092900             MOVE   "SCOPE DELIMITER IGNORED" TO RE-MARK          ST1194.2
093000             MOVE   "W" TO CORRECT-X                              ST1194.2
093100             MOVE    WRK-XN-00001-3 TO COMPUTED-X                 ST1194.2
093200             PERFORM FAIL                                         ST1194.2
093300             PERFORM PRINT-DETAIL.                                ST1194.2
093400 RET-6-EXIT.                                                      ST1194.2
093500     EXIT.                                                        ST1194.2
093600 RET-7.                                                           ST1194.2
093700     MOVE   "XI-14 4.3.4 GR(2) & (4)" TO ANSI-REFERENCE.          ST1194.2
093800     MOVE   "RET-7" TO PAR-NAME.                                  ST1194.2
093900     MOVE   "G" TO WRK-XN-00001-3.                                ST1194.2
094000     RETURN  SORTFILE-1A                                          ST1194.2
094100             AT END  GO TO BAD-FILE                               ST1194.2
094200             NOT AT END                                           ST1194.2
094300             MOVE    S-RECORD TO SORTED                           ST1194.2
094400             WRITE   SORTED                                       ST1194.2
094500     END-RETURN                                                   ST1194.2
094600     MOVE   "K"     TO WRK-XN-00001-3.                            ST1194.2
094700     IF      WRK-XN-00001-3 = "K"                                 ST1194.2
094800             PERFORM PASS                                         ST1194.2
094900             PERFORM PRINT-DETAIL                                 ST1194.2
095000     ELSE                                                         ST1194.2
095100             MOVE   "SCOPE DELIMITER IGNORED" TO RE-MARK          ST1194.2
095200             MOVE   "K" TO CORRECT-X                              ST1194.2
095300             MOVE    WRK-XN-00001-3 TO COMPUTED-X                 ST1194.2
095400             PERFORM FAIL                                         ST1194.2
095500             PERFORM PRINT-DETAIL.                                ST1194.2
095600 RET-7-EXIT.                                                      ST1194.2
095700     EXIT.                                                        ST1194.2
095800 RET-8.                                                           ST1194.2
095900     MOVE   "XI-14 4.3.4 GR(2) & (4)" TO ANSI-REFERENCE.          ST1194.2
096000     MOVE   "RET-8" TO PAR-NAME.                                  ST1194.2
096100     MOVE   "X" TO WRK-XN-00001-3.                                ST1194.2
096200     RETURN  SORTFILE-1A                                          ST1194.2
096300             AT END                                               ST1194.2
096400             GO TO BAD-FILE                                       ST1194.2
096500         NOT AT END                                               ST1194.2
096600             MOVE    S-RECORD TO SORTED                           ST1194.2
096700             WRITE   SORTED                                       ST1194.2
096800     END-RETURN                                                   ST1194.2
096900     MOVE   "T"     TO WRK-XN-00001-3.                            ST1194.2
097000     IF      WRK-XN-00001-3 = "T"                                 ST1194.2
097100             PERFORM PASS                                         ST1194.2
097200             PERFORM PRINT-DETAIL                                 ST1194.2
097300     ELSE                                                         ST1194.2
097400             MOVE   "SCOPE DELIMITER IGNORED" TO RE-MARK          ST1194.2
097500             MOVE   "T" TO CORRECT-X                              ST1194.2
097600             MOVE    WRK-XN-00001-3 TO COMPUTED-X                 ST1194.2
097700             PERFORM FAIL                                         ST1194.2
097800             PERFORM PRINT-DETAIL.                                ST1194.2
097900     MOVE SPACES TO TEST-RESULTS.                                 ST1194.2
098000     MOVE COMMENT-SENTENCE TO TEST-RESULTS.                       ST1194.2
098100     PERFORM PRINT-DETAIL.                                        ST1194.2
098200     MOVE SPACES TO TEST-RESULTS.                                 ST1194.2
098300 RET-8-EXIT.                                                      ST1194.2
098400     EXIT.                                                        ST1194.2
098500                                                                  ST1194.2
098600 EXTERNAL-CODE-1.                                                 ST1194.2
098700     MOVE   "A" TO WRK-XN-00001-1.                                ST1194.2
098800 EXTERNAL-CODE-2.                                                 ST1194.2
098900     MOVE   "B" TO WRK-XN-00001-1.                                ST1194.2
099000     GO TO   SORT-TEST-11-1.                                      ST1194.2
099100 EXTERNAL-CODE-3.                                                 ST1194.2
099200     MOVE   "J" TO WRK-XN-00001-1.                                ST1194.2
099300 EXTERNAL-CODE-4.                                                 ST1194.2
099400     MOVE   "K" TO WRK-XN-00001-1.                                ST1194.2
099500     GO TO   INPT-TEST-2-1.                                       ST1194.2
099600                                                                  ST1194.2
099700 CCVS-EXIT SECTION.                                               ST1194.2
099800 CCVS-9999.                                                       ST1194.2
099900     GO TO   CLOSE-FILES.                                         ST1194.2
      *END-OF,ST119A                                                                  
