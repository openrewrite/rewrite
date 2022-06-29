      *HEADER,COBOL,NC139A                                                            
000100 IDENTIFICATION DIVISION.                                         NC1394.2
000200 PROGRAM-ID.                                                      NC1394.2
000300     NC139A.                                                      NC1394.2
000400****************************************************************  NC1394.2
000500*                                                              *  NC1394.2
000600*    VALIDATION FOR:-                                          *  NC1394.2
000700*                                                              *  NC1394.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1394.2
000900*                                                              *  NC1394.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC1394.2
001100*                                                              *  NC1394.2
001200****************************************************************  NC1394.2
001300*                                                              *  NC1394.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  NC1394.2
001500*                                                              *  NC1394.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  NC1394.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  NC1394.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  NC1394.2
001900*                                                              *  NC1394.2
002000****************************************************************  NC1394.2
002100*                                                                 NC1394.2
002200*    PROGRAM NC139A TESTS THE USE OF NUMERIC LITERALS WITH        NC1394.2
002300*    RELATIVE INDEXING WHEN ACCESSING 2 AND 3 DIMENSIONAL         NC1394.2
002400*    TABLES.                                                      NC1394.2
002500*    THE USE OF INDEXES AND SUBSCRIPTS TOGETHER IS ALSO TESTED.   NC1394.2
002600*                                                                 NC1394.2
002700 ENVIRONMENT DIVISION.                                            NC1394.2
002800 CONFIGURATION SECTION.                                           NC1394.2
002900 SOURCE-COMPUTER.                                                 NC1394.2
003000     XXXXX082.                                                    NC1394.2
003100 OBJECT-COMPUTER.                                                 NC1394.2
003200     XXXXX083.                                                    NC1394.2
003300 INPUT-OUTPUT SECTION.                                            NC1394.2
003400 FILE-CONTROL.                                                    NC1394.2
003500     SELECT PRINT-FILE ASSIGN TO                                  NC1394.2
003600     XXXXX055.                                                    NC1394.2
003700 DATA DIVISION.                                                   NC1394.2
003800 FILE SECTION.                                                    NC1394.2
003900 FD  PRINT-FILE.                                                  NC1394.2
004000 01  PRINT-REC PICTURE X(120).                                    NC1394.2
004100 01  DUMMY-RECORD PICTURE X(120).                                 NC1394.2
004200 WORKING-STORAGE SECTION.                                         NC1394.2
004300*         LITERALS AND INDEX-NAMES MAY BE MIXED IN A TABLE        NC1394.2
004400*    REFERENCE.  THE LITERALS MAY BE SIGNED BUT MUST BE POSITIVE. NC1394.2
004500*    RELATIVE INDEXING WITH BOTH POSITIVE AND NEGATIVE INTEGERS   NC1394.2
004600*    IS PERMITTED.                                                NC1394.2
004700 01  WS-2      PIC 9.                                             NC1394.2
004800 01  WS-PLUS-2 PIC S9.                                            NC1394.2
004900 01  WS-4      PIC 9.                                             NC1394.2
005000 01  WS-PLUS-4 PIC S9.                                            NC1394.2
005100 77  TEMP PIC XXX.                                                NC1394.2
005200 77  EXPECTED-VALUE  PIC XXX.                                     NC1394.2
005300*        TWO DIMENSIONAL TABLE, 6X4, WITH INDEXES.                NC1394.2
005400 01  GRP-TAB1.                                                    NC1394.2
005500     02  GRP-1  OCCURS 6 TIMES                                    NC1394.2
005600                INDEXED BY IN1.                                   NC1394.2
005700         03  ELEM1  PIC  XXX                                      NC1394.2
005800                OCCURS 4 TIMES                                    NC1394.2
005900                INDEXED BY IN2.                                   NC1394.2
006000*       THREE DIMENSIONAL TABLE, 3X2X4, WITH INDEXES.             NC1394.2
006100 01  GRP-TAB2.                                                    NC1394.2
006200     02  GRP-2   OCCURS 3 TIMES                                   NC1394.2
006300                 INDEXED BY INDEX1.                               NC1394.2
006400         03  GRP-3  OCCURS 2 TIMES                                NC1394.2
006500                    INDEXED BY INDEX2.                            NC1394.2
006600             04  ELEM2 PIC XXX                                    NC1394.2
006700                       OCCURS 4 TIMES                             NC1394.2
006800                       INDEXED BY INDEX3.                         NC1394.2
006900 01  TABLE-VALUES.                                                NC1394.2
007000     02  VALUES-1   PIC  X(12)                                    NC1394.2
007100             VALUE "AAABBBCCCDDD".                                NC1394.2
007200     02  VALUES-2   PIC  X(12)                                    NC1394.2
007300             VALUE "EEEFFFGGGHHH".                                NC1394.2
007400     02  VALUES-3   PIC  X(12)                                    NC1394.2
007500             VALUE "IIIJJJKKKLLL".                                NC1394.2
007600     02  VALUES-4   PIC  X(12)                                    NC1394.2
007700             VALUE "MMMNNNOOOPPP".                                NC1394.2
007800     02  VALUES-5   PIC  X(12)                                    NC1394.2
007900             VALUE "QQQRRRSSSTTT".                                NC1394.2
008000     02  VALUES-6   PIC  X(12)                                    NC1394.2
008100             VALUE "UUUVVVWWWXXX".                                NC1394.2
008200 01  TEST-RESULTS.                                                NC1394.2
008300     02 FILLER                   PIC X      VALUE SPACE.          NC1394.2
008400     02 FEATURE                  PIC X(20)  VALUE SPACE.          NC1394.2
008500     02 FILLER                   PIC X      VALUE SPACE.          NC1394.2
008600     02 P-OR-F                   PIC X(5)   VALUE SPACE.          NC1394.2
008700     02 FILLER                   PIC X      VALUE SPACE.          NC1394.2
008800     02  PAR-NAME.                                                NC1394.2
008900       03 FILLER                 PIC X(19)  VALUE SPACE.          NC1394.2
009000       03  PARDOT-X              PIC X      VALUE SPACE.          NC1394.2
009100       03 DOTVALUE               PIC 99     VALUE ZERO.           NC1394.2
009200     02 FILLER                   PIC X(8)   VALUE SPACE.          NC1394.2
009300     02 RE-MARK                  PIC X(61).                       NC1394.2
009400 01  TEST-COMPUTED.                                               NC1394.2
009500     02 FILLER                   PIC X(30)  VALUE SPACE.          NC1394.2
009600     02 FILLER                   PIC X(17)  VALUE                 NC1394.2
009700            "       COMPUTED=".                                   NC1394.2
009800     02 COMPUTED-X.                                               NC1394.2
009900     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          NC1394.2
010000     03 COMPUTED-N               REDEFINES COMPUTED-A             NC1394.2
010100                                 PIC -9(9).9(9).                  NC1394.2
010200     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         NC1394.2
010300     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     NC1394.2
010400     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     NC1394.2
010500     03       CM-18V0 REDEFINES COMPUTED-A.                       NC1394.2
010600         04 COMPUTED-18V0                    PIC -9(18).          NC1394.2
010700         04 FILLER                           PIC X.               NC1394.2
010800     03 FILLER PIC X(50) VALUE SPACE.                             NC1394.2
010900 01  TEST-CORRECT.                                                NC1394.2
011000     02 FILLER PIC X(30) VALUE SPACE.                             NC1394.2
011100     02 FILLER PIC X(17) VALUE "       CORRECT =".                NC1394.2
011200     02 CORRECT-X.                                                NC1394.2
011300     03 CORRECT-A                  PIC X(20) VALUE SPACE.         NC1394.2
011400     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      NC1394.2
011500     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         NC1394.2
011600     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     NC1394.2
011700     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     NC1394.2
011800     03      CR-18V0 REDEFINES CORRECT-A.                         NC1394.2
011900         04 CORRECT-18V0                     PIC -9(18).          NC1394.2
012000         04 FILLER                           PIC X.               NC1394.2
012100     03 FILLER PIC X(2) VALUE SPACE.                              NC1394.2
012200     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     NC1394.2
012300 01  CCVS-C-1.                                                    NC1394.2
012400     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PANC1394.2
012500-    "SS  PARAGRAPH-NAME                                          NC1394.2
012600-    "       REMARKS".                                            NC1394.2
012700     02 FILLER                     PIC X(20)    VALUE SPACE.      NC1394.2
012800 01  CCVS-C-2.                                                    NC1394.2
012900     02 FILLER                     PIC X        VALUE SPACE.      NC1394.2
013000     02 FILLER                     PIC X(6)     VALUE "TESTED".   NC1394.2
013100     02 FILLER                     PIC X(15)    VALUE SPACE.      NC1394.2
013200     02 FILLER                     PIC X(4)     VALUE "FAIL".     NC1394.2
013300     02 FILLER                     PIC X(94)    VALUE SPACE.      NC1394.2
013400 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       NC1394.2
013500 01  REC-CT                        PIC 99       VALUE ZERO.       NC1394.2
013600 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       NC1394.2
013700 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       NC1394.2
013800 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       NC1394.2
013900 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       NC1394.2
014000 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       NC1394.2
014100 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       NC1394.2
014200 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      NC1394.2
014300 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       NC1394.2
014400 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     NC1394.2
014500 01  CCVS-H-1.                                                    NC1394.2
014600     02  FILLER                    PIC X(39)    VALUE SPACES.     NC1394.2
014700     02  FILLER                    PIC X(42)    VALUE             NC1394.2
014800     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 NC1394.2
014900     02  FILLER                    PIC X(39)    VALUE SPACES.     NC1394.2
015000 01  CCVS-H-2A.                                                   NC1394.2
015100   02  FILLER                        PIC X(40)  VALUE SPACE.      NC1394.2
015200   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  NC1394.2
015300   02  FILLER                        PIC XXXX   VALUE             NC1394.2
015400     "4.2 ".                                                      NC1394.2
015500   02  FILLER                        PIC X(28)  VALUE             NC1394.2
015600            " COPY - NOT FOR DISTRIBUTION".                       NC1394.2
015700   02  FILLER                        PIC X(41)  VALUE SPACE.      NC1394.2
015800                                                                  NC1394.2
015900 01  CCVS-H-2B.                                                   NC1394.2
016000   02  FILLER                        PIC X(15)  VALUE             NC1394.2
016100            "TEST RESULT OF ".                                    NC1394.2
016200   02  TEST-ID                       PIC X(9).                    NC1394.2
016300   02  FILLER                        PIC X(4)   VALUE             NC1394.2
016400            " IN ".                                               NC1394.2
016500   02  FILLER                        PIC X(12)  VALUE             NC1394.2
016600     " HIGH       ".                                              NC1394.2
016700   02  FILLER                        PIC X(22)  VALUE             NC1394.2
016800            " LEVEL VALIDATION FOR ".                             NC1394.2
016900   02  FILLER                        PIC X(58)  VALUE             NC1394.2
017000     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1394.2
017100 01  CCVS-H-3.                                                    NC1394.2
017200     02  FILLER                      PIC X(34)  VALUE             NC1394.2
017300            " FOR OFFICIAL USE ONLY    ".                         NC1394.2
017400     02  FILLER                      PIC X(58)  VALUE             NC1394.2
017500     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC1394.2
017600     02  FILLER                      PIC X(28)  VALUE             NC1394.2
017700            "  COPYRIGHT   1985 ".                                NC1394.2
017800 01  CCVS-E-1.                                                    NC1394.2
017900     02 FILLER                       PIC X(52)  VALUE SPACE.      NC1394.2
018000     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              NC1394.2
018100     02 ID-AGAIN                     PIC X(9).                    NC1394.2
018200     02 FILLER                       PIC X(45)  VALUE SPACES.     NC1394.2
018300 01  CCVS-E-2.                                                    NC1394.2
018400     02  FILLER                      PIC X(31)  VALUE SPACE.      NC1394.2
018500     02  FILLER                      PIC X(21)  VALUE SPACE.      NC1394.2
018600     02 CCVS-E-2-2.                                               NC1394.2
018700         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      NC1394.2
018800         03 FILLER                   PIC X      VALUE SPACE.      NC1394.2
018900         03 ENDER-DESC               PIC X(44)  VALUE             NC1394.2
019000            "ERRORS ENCOUNTERED".                                 NC1394.2
019100 01  CCVS-E-3.                                                    NC1394.2
019200     02  FILLER                      PIC X(22)  VALUE             NC1394.2
019300            " FOR OFFICIAL USE ONLY".                             NC1394.2
019400     02  FILLER                      PIC X(12)  VALUE SPACE.      NC1394.2
019500     02  FILLER                      PIC X(58)  VALUE             NC1394.2
019600     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1394.2
019700     02  FILLER                      PIC X(13)  VALUE SPACE.      NC1394.2
019800     02 FILLER                       PIC X(15)  VALUE             NC1394.2
019900             " COPYRIGHT 1985".                                   NC1394.2
020000 01  CCVS-E-4.                                                    NC1394.2
020100     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      NC1394.2
020200     02 FILLER                       PIC X(4)   VALUE " OF ".     NC1394.2
020300     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      NC1394.2
020400     02 FILLER                       PIC X(40)  VALUE             NC1394.2
020500      "  TESTS WERE EXECUTED SUCCESSFULLY".                       NC1394.2
020600 01  XXINFO.                                                      NC1394.2
020700     02 FILLER                       PIC X(19)  VALUE             NC1394.2
020800            "*** INFORMATION ***".                                NC1394.2
020900     02 INFO-TEXT.                                                NC1394.2
021000       04 FILLER                     PIC X(8)   VALUE SPACE.      NC1394.2
021100       04 XXCOMPUTED                 PIC X(20).                   NC1394.2
021200       04 FILLER                     PIC X(5)   VALUE SPACE.      NC1394.2
021300       04 XXCORRECT                  PIC X(20).                   NC1394.2
021400     02 INF-ANSI-REFERENCE           PIC X(48).                   NC1394.2
021500 01  HYPHEN-LINE.                                                 NC1394.2
021600     02 FILLER  PIC IS X VALUE IS SPACE.                          NC1394.2
021700     02 FILLER  PIC IS X(65)    VALUE IS "************************NC1394.2
021800-    "*****************************************".                 NC1394.2
021900     02 FILLER  PIC IS X(54)    VALUE IS "************************NC1394.2
022000-    "******************************".                            NC1394.2
022100 01  CCVS-PGM-ID                     PIC X(9)   VALUE             NC1394.2
022200     "NC139A".                                                    NC1394.2
022300 PROCEDURE DIVISION.                                              NC1394.2
022400 CCVS1 SECTION.                                                   NC1394.2
022500 OPEN-FILES.                                                      NC1394.2
022600     OPEN     OUTPUT PRINT-FILE.                                  NC1394.2
022700     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   NC1394.2
022800     MOVE    SPACE TO TEST-RESULTS.                               NC1394.2
022900     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             NC1394.2
023000     GO TO CCVS1-EXIT.                                            NC1394.2
023100 CLOSE-FILES.                                                     NC1394.2
023200     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   NC1394.2
023300 TERMINATE-CCVS.                                                  NC1394.2
023400*    EXIT PROGRAM.                                                NC1394.2
023500*TERMINATE-CALL.                                                  NC1394.2
023600     STOP     RUN.                                                NC1394.2
023700 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         NC1394.2
023800 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           NC1394.2
023900 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          NC1394.2
024000 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      NC1394.2
024100     MOVE "****TEST DELETED****" TO RE-MARK.                      NC1394.2
024200 PRINT-DETAIL.                                                    NC1394.2
024300     IF REC-CT NOT EQUAL TO ZERO                                  NC1394.2
024400             MOVE "." TO PARDOT-X                                 NC1394.2
024500             MOVE REC-CT TO DOTVALUE.                             NC1394.2
024600     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      NC1394.2
024700     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               NC1394.2
024800        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 NC1394.2
024900          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 NC1394.2
025000     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              NC1394.2
025100     MOVE SPACE TO CORRECT-X.                                     NC1394.2
025200     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         NC1394.2
025300     MOVE     SPACE TO RE-MARK.                                   NC1394.2
025400 HEAD-ROUTINE.                                                    NC1394.2
025500     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC1394.2
025600     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC1394.2
025700     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC1394.2
025800     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC1394.2
025900 COLUMN-NAMES-ROUTINE.                                            NC1394.2
026000     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1394.2
026100     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1394.2
026200     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        NC1394.2
026300 END-ROUTINE.                                                     NC1394.2
026400     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.NC1394.2
026500 END-RTN-EXIT.                                                    NC1394.2
026600     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1394.2
026700 END-ROUTINE-1.                                                   NC1394.2
026800      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      NC1394.2
026900      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               NC1394.2
027000      ADD PASS-COUNTER TO ERROR-HOLD.                             NC1394.2
027100*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   NC1394.2
027200      MOVE PASS-COUNTER TO CCVS-E-4-1.                            NC1394.2
027300      MOVE ERROR-HOLD TO CCVS-E-4-2.                              NC1394.2
027400      MOVE CCVS-E-4 TO CCVS-E-2-2.                                NC1394.2
027500      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           NC1394.2
027600  END-ROUTINE-12.                                                 NC1394.2
027700      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        NC1394.2
027800     IF       ERROR-COUNTER IS EQUAL TO ZERO                      NC1394.2
027900         MOVE "NO " TO ERROR-TOTAL                                NC1394.2
028000         ELSE                                                     NC1394.2
028100         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       NC1394.2
028200     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           NC1394.2
028300     PERFORM WRITE-LINE.                                          NC1394.2
028400 END-ROUTINE-13.                                                  NC1394.2
028500     IF DELETE-COUNTER IS EQUAL TO ZERO                           NC1394.2
028600         MOVE "NO " TO ERROR-TOTAL  ELSE                          NC1394.2
028700         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      NC1394.2
028800     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   NC1394.2
028900     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1394.2
029000      IF   INSPECT-COUNTER EQUAL TO ZERO                          NC1394.2
029100          MOVE "NO " TO ERROR-TOTAL                               NC1394.2
029200      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   NC1394.2
029300      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            NC1394.2
029400      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          NC1394.2
029500     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1394.2
029600 WRITE-LINE.                                                      NC1394.2
029700     ADD 1 TO RECORD-COUNT.                                       NC1394.2
029800*    IF RECORD-COUNT GREATER 42                                   NC1394.2
029900*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          NC1394.2
030000*        MOVE SPACE TO DUMMY-RECORD                               NC1394.2
030100*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  NC1394.2
030200*        MOVE CCVS-H-1  TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   NC1394.2
030300*        MOVE CCVS-H-2A TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   NC1394.2
030400*        MOVE CCVS-H-2B TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   NC1394.2
030500*        MOVE CCVS-H-3  TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   NC1394.2
030600*        MOVE CCVS-C-1  TO DUMMY-RECORD  PERFORM WRT-LN           NC1394.2
030700*        MOVE CCVS-C-2  TO DUMMY-RECORD  PERFORM WRT-LN           NC1394.2
030800*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          NC1394.2
030900*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          NC1394.2
031000*        MOVE ZERO TO RECORD-COUNT.                               NC1394.2
031100     PERFORM WRT-LN.                                              NC1394.2
031200 WRT-LN.                                                          NC1394.2
031300     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               NC1394.2
031400     MOVE SPACE TO DUMMY-RECORD.                                  NC1394.2
031500 BLANK-LINE-PRINT.                                                NC1394.2
031600     PERFORM WRT-LN.                                              NC1394.2
031700 FAIL-ROUTINE.                                                    NC1394.2
031800     IF     COMPUTED-X NOT EQUAL TO SPACE                         NC1394.2
031900            GO TO FAIL-ROUTINE-WRITE.                             NC1394.2
032000     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.NC1394.2
032100     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC1394.2
032200     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   NC1394.2
032300     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1394.2
032400     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC1394.2
032500     GO TO  FAIL-ROUTINE-EX.                                      NC1394.2
032600 FAIL-ROUTINE-WRITE.                                              NC1394.2
032700     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         NC1394.2
032800     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 NC1394.2
032900     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. NC1394.2
033000     MOVE   SPACES TO COR-ANSI-REFERENCE.                         NC1394.2
033100 FAIL-ROUTINE-EX. EXIT.                                           NC1394.2
033200 BAIL-OUT.                                                        NC1394.2
033300     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   NC1394.2
033400     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           NC1394.2
033500 BAIL-OUT-WRITE.                                                  NC1394.2
033600     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  NC1394.2
033700     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC1394.2
033800     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1394.2
033900     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC1394.2
034000 BAIL-OUT-EX. EXIT.                                               NC1394.2
034100 CCVS1-EXIT.                                                      NC1394.2
034200     EXIT.                                                        NC1394.2
034300*********************************                                 NC1394.2
034400*        STATEMENT DELETION INSTRUCTIONS                          NC1394.2
034500*         IF THE COMPILER REJECTS ANY OF THE TABLE REFERENCES IN  NC1394.2
034600*    THIS ROUTINE, DELETE THAT LINE OF CODE BY PLACING AN * IN    NC1394.2
034700*    COLUMN 7.  LEAVE THE PERFORM...THRU STATEMENT.  THE TEST     NC1394.2
034800*    ELEMENT DELETED APPEARS AS A FAILURE ON THE OUTPUT REPORT.   NC1394.2
034900*****************************************                         NC1394.2
035000 SECT-NC139A-001 SECTION.                                         NC1394.2
035100 NC139A-001.                                                      NC1394.2
035200     MOVE "IV-21 4.3.8.2" TO ANSI-REFERENCE.                      NC1394.2
035300*        THIS SECTION STORES AAA THRU XXX IN THE TWO TABLES       NC1394.2
035400*    REFERENCED IN THE TESTS.                                     NC1394.2
035500 BUILD-TABLE.                                                     NC1394.2
035600     SET IN1 TO 1.                                                NC1394.2
035700     MOVE VALUES-1 TO GRP-1 (IN1).                                NC1394.2
035800     MOVE VALUES-2 TO GRP-1 (IN1 + 1).                            NC1394.2
035900     MOVE VALUES-3 TO GRP-1 (IN1 + 2).                            NC1394.2
036000     MOVE VALUES-4 TO GRP-1 (IN1 + 3).                            NC1394.2
036100     MOVE VALUES-5 TO GRP-1 (IN1 + 4).                            NC1394.2
036200     MOVE VALUES-6 TO GRP-1 (IN1 + 5).                            NC1394.2
036300     MOVE GRP-TAB1 TO GRP-TAB2.                                   NC1394.2
036400 SECT-TH110-0002 SECTION.                                         NC1394.2
036500*       THIS SECTION CONTAINS THE TESTS WHICH VALIDATE            NC1394.2
036600*    THE HANDLING OF LITERALS MIXED WITH INDEX-NAMES              NC1394.2
036700*    IN REFERENCING TWO AND THREE DIMENSIONAL TABLES.             NC1394.2
036800 IND-INIT-001.                                                    NC1394.2
036900*        THIS TEST MIXES UNSIGNED LITERALS WITH INDEX-NAMES.      NC1394.2
037000     SET IN1  IN2  TO 1.                                          NC1394.2
037100     SET INDEX1  INDEX2  INDEX3  TO 1.                            NC1394.2
037200     MOVE "AAA" TO EXPECTED-VALUE.                                NC1394.2
037300     MOVE SPACE TO TEMP.                                          NC1394.2
037400     MOVE ZERO TO REC-CT.                                         NC1394.2
037500     MOVE "IND-TEST-001" TO PAR-NAME.                             NC1394.2
037600     MOVE "INDEXES AND LITERALS" TO FEATURE.                      NC1394.2
037700 IND-TEST-001-01.                                                 NC1394.2
037800     MOVE ELEM1 (IN1, 1) TO TEMP.                                 NC1394.2
037900     PERFORM SYNTAX-CHECK THRU SYNTAX-CHECK-WRITE.                NC1394.2
038000 IND-TEST-001-02.                                                 NC1394.2
038100     MOVE ELEM1(1 IN2) TO TEMP.                                   NC1394.2
038200     PERFORM SYNTAX-CHECK THRU SYNTAX-CHECK-WRITE.                NC1394.2
038300 IND-TEST-001-03.                                                 NC1394.2
038400     MOVE ELEM1(1, IN2) TO TEMP.                                  NC1394.2
038500     PERFORM SYNTAX-CHECK THRU SYNTAX-CHECK-WRITE.                NC1394.2
038600 IND-TEST-001-04.                                                 NC1394.2
038700     MOVE ELEM2 (1 INDEX2 1)  TO TEMP.                            NC1394.2
038800     PERFORM SYNTAX-CHECK THRU SYNTAX-CHECK-WRITE.                NC1394.2
038900 IND-TEST-001-05.                                                 NC1394.2
039000     MOVE ELEM2(INDEX1 INDEX2 1) TO TEMP.                         NC1394.2
039100     PERFORM SYNTAX-CHECK THRU SYNTAX-CHECK-WRITE.                NC1394.2
039200 IND-TEST-001-06.                                                 NC1394.2
039300     MOVE ELEM2 (INDEX1, 1  INDEX3) TO TEMP.                      NC1394.2
039400     PERFORM SYNTAX-CHECK THRU SYNTAX-CHECK-WRITE.                NC1394.2
039500 IND-TEST-001-07.                                                 NC1394.2
039600     MOVE ELEM2 (1 1 INDEX3) TO TEMP.                             NC1394.2
039700     PERFORM SYNTAX-CHECK THRU SYNTAX-CHECK-WRITE.                NC1394.2
039800     GO TO IND-INIT-002.                                          NC1394.2
039900 IND-DELETE-001.                                                  NC1394.2
040000     PERFORM DE-LETE.                                             NC1394.2
040100     PERFORM SYNTAX-CHECK-WRITE.                                  NC1394.2
040200 IND-INIT-002.                                                    NC1394.2
040300*       THIS TEST MIXES SIGNED LITERALS WITH INDEX-NAMES.         NC1394.2
040400     MOVE ZERO TO REC-CT.                                         NC1394.2
040500     MOVE SPACE TO TEMP.                                          NC1394.2
040600     MOVE "GGG" TO EXPECTED-VALUE.                                NC1394.2
040700     MOVE "IND-TEST-002" TO PAR-NAME.                             NC1394.2
040800     SET INDEX1 TO 1.                                             NC1394.2
040900     SET IN1 INDEX2 TO 2.                                         NC1394.2
041000     SET IN2 INDEX3 TO 3.                                         NC1394.2
041100 IND-TEST-002-01.                                                 NC1394.2
041200     MOVE ELEM1(IN1, +3) TO TEMP.                                 NC1394.2
041300     PERFORM SYNTAX-CHECK THRU SYNTAX-CHECK-WRITE.                NC1394.2
041400 IND-TEST-002-02.                                                 NC1394.2
041500     MOVE ELEM1(+2, IN2) TO TEMP.                                 NC1394.2
041600     PERFORM SYNTAX-CHECK THRU SYNTAX-CHECK-WRITE.                NC1394.2
041700 IND-TEST-002-03.                                                 NC1394.2
041800     IF ELEM1 (+2 IN2) EQUAL TO "GGG"                             NC1394.2
041900         MOVE "GGG" TO TEMP.                                      NC1394.2
042000     PERFORM SYNTAX-CHECK THRU SYNTAX-CHECK-WRITE.                NC1394.2
042100 IND-TEST-002-04.                                                 NC1394.2
042200     IF ELEM1 (IN1  +3) IS EQUAL TO EXPECTED-VALUE                NC1394.2
042300          MOVE "GGG" TO TEMP.                                     NC1394.2
042400     PERFORM SYNTAX-CHECK THRU SYNTAX-CHECK-WRITE.                NC1394.2
042500 IND-TEST-002-05.                                                 NC1394.2
042600     MOVE ELEM2(+1, INDEX2, +3)  TO TEMP.                         NC1394.2
042700     PERFORM SYNTAX-CHECK THRU SYNTAX-CHECK-WRITE.                NC1394.2
042800 IND-TEST-002-06.                                                 NC1394.2
042900     MOVE ELEM2(+1 INDEX2 +3) TO TEMP.                            NC1394.2
043000     PERFORM SYNTAX-CHECK THRU SYNTAX-CHECK-WRITE.                NC1394.2
043100 IND-TEST-002-07.                                                 NC1394.2
043200     MOVE ELEM2 (INDEX1  +2, +3) TO TEMP.                         NC1394.2
043300     PERFORM SYNTAX-CHECK THRU SYNTAX-CHECK-WRITE.                NC1394.2
043400 IND-TEST-002-08.                                                 NC1394.2
043500     MOVE ELEM2 (INDEX1  INDEX2  +3)  TO  TEMP.                   NC1394.2
043600     PERFORM SYNTAX-CHECK THRU SYNTAX-CHECK-WRITE.                NC1394.2
043700     GO TO IND-INIT-003.                                          NC1394.2
043800 IND-DELETE-002.                                                  NC1394.2
043900     PERFORM DE-LETE.                                             NC1394.2
044000     PERFORM SYNTAX-CHECK-WRITE.                                  NC1394.2
044100 IND-INIT-003.                                                    NC1394.2
044200*       THIS TEST MIXES UNSIGNED LITERALS WITH RELATIVE INDEXING. NC1394.2
044300     MOVE ZERO TO REC-CT.                                         NC1394.2
044400     MOVE SPACE TO TEMP.                                          NC1394.2
044500     MOVE "SSS" TO EXPECTED-VALUE.                                NC1394.2
044600     MOVE "IND-TEST-003" TO PAR-NAME.                             NC1394.2
044700     SET IN1 TO 6.                                                NC1394.2
044800     SET INDEX3 TO 4.                                             NC1394.2
044900     SET INDEX2 TO 1.                                             NC1394.2
045000     SET IN2 INDEX1 TO 2.                                         NC1394.2
045100 IND-TEST-003-01.                                                 NC1394.2
045200     MOVE ELEM1(IN1 - 1, 3) TO TEMP.                              NC1394.2
045300     PERFORM SYNTAX-CHECK THRU SYNTAX-CHECK-WRITE.                NC1394.2
045400 IND-TEST-003-02.                                                 NC1394.2
045500     MOVE ELEM1 ( 5, IN2 + 1) TO TEMP.                            NC1394.2
045600     PERFORM SYNTAX-CHECK THRU SYNTAX-CHECK-WRITE.                NC1394.2
045700 IND-TEST-003-03.                                                 NC1394.2
045800     MOVE ELEM1(IN1 - 1 3) TO TEMP.                               NC1394.2
045900     PERFORM SYNTAX-CHECK THRU SYNTAX-CHECK-WRITE.                NC1394.2
046000 IND-TEST-003-04.                                                 NC1394.2
046100     MOVE ELEM1 (5 IN2 + 1) TO TEMP.                              NC1394.2
046200     PERFORM SYNTAX-CHECK THRU SYNTAX-CHECK-WRITE.                NC1394.2
046300 IND-TEST-003-05.                                                 NC1394.2
046400     MOVE ELEM2 (3, INDEX2, INDEX3 - 1) TO TEMP.                  NC1394.2
046500     PERFORM SYNTAX-CHECK THRU SYNTAX-CHECK-WRITE.                NC1394.2
046600 IND-TEST-003-06.                                                 NC1394.2
046700     MOVE ELEM2 (3 INDEX2 INDEX3 - 1) TO TEMP.                    NC1394.2
046800     PERFORM SYNTAX-CHECK THRU SYNTAX-CHECK-WRITE.                NC1394.2
046900 IND-TEST-003-07.                                                 NC1394.2
047000     MOVE ELEM2(INDEX1 + 1, 1, 3) TO TEMP.                        NC1394.2
047100     PERFORM SYNTAX-CHECK THRU SYNTAX-CHECK-WRITE.                NC1394.2
047200 IND-TEST-003-08.                                                 NC1394.2
047300     IF ELEM2(INDEX1 + 1 1 3) IS EQUAL TO "SSS"                   NC1394.2
047400              MOVE "SSS" TO TEMP.                                 NC1394.2
047500     PERFORM SYNTAX-CHECK THRU SYNTAX-CHECK-WRITE.                NC1394.2
047600 IND-TEST-003-09.                                                 NC1394.2
047700     MOVE ELEM2 (INDEX1 + 1 INDEX2 3) TO TEMP.                    NC1394.2
047800     PERFORM SYNTAX-CHECK THRU SYNTAX-CHECK-WRITE.                NC1394.2
047900 IND-TEST-003-10.                                                 NC1394.2
048000     MOVE ELEM2 (3 1 INDEX3 - 1) TO TEMP.                         NC1394.2
048100     PERFORM SYNTAX-CHECK THRU SYNTAX-CHECK-WRITE.                NC1394.2
048200 IND-TEST-003-11.                                                 NC1394.2
048300     MOVE ELEM2(INDEX1 + 1 1 INDEX3 - 1) TO TEMP.                 NC1394.2
048400     PERFORM SYNTAX-CHECK THRU SYNTAX-CHECK-WRITE.                NC1394.2
048500     GO TO IND-INIT-004.                                          NC1394.2
048600 IND-DELETE-003.                                                  NC1394.2
048700     PERFORM DE-LETE.                                             NC1394.2
048800     PERFORM SYNTAX-CHECK-WRITE.                                  NC1394.2
048900 IND-INIT-004.                                                    NC1394.2
049000*        THIS TEST MIXES SIGNED LITERALS WITH RELATIVE INDEXING.  NC1394.2
049100     MOVE ZERO TO REC-CT.                                         NC1394.2
049200     MOVE SPACE TO TEMP.                                          NC1394.2
049300     MOVE "VVV" TO EXPECTED-VALUE.                                NC1394.2
049400     MOVE "IND-TEST-004" TO PAR-NAME.                             NC1394.2
049500     SET IN1 TO 2.                                                NC1394.2
049600     SET IN2 TO 4.                                                NC1394.2
049700     SET INDEX1 TO 2.                                             NC1394.2
049800     SET INDEX2 TO 1.                                             NC1394.2
049900     SET INDEX3 TO 4.                                             NC1394.2
050000 IND-TEST-004-01.                                                 NC1394.2
050100     MOVE ELEM1(IN1 + 4, +2) TO TEMP.                             NC1394.2
050200     PERFORM SYNTAX-CHECK THRU SYNTAX-CHECK-WRITE.                NC1394.2
050300 IND-TEST-004-02.                                                 NC1394.2
050400     MOVE ELEM1 (IN1 + 4 +2) TO TEMP.                             NC1394.2
050500     PERFORM SYNTAX-CHECK THRU SYNTAX-CHECK-WRITE.                NC1394.2
050600 IND-TEST-004-03.                                                 NC1394.2
050700     MOVE ELEM1 (+6, IN2 - 2) TO TEMP.                            NC1394.2
050800     PERFORM SYNTAX-CHECK THRU SYNTAX-CHECK-WRITE.                NC1394.2
050900 IND-TEST-004-04.                                                 NC1394.2
051000     IF ELEM1(+6 IN2 - 2) IS EQUAL TO "VVV"                       NC1394.2
051100              MOVE "VVV" TO TEMP.                                 NC1394.2
051200     PERFORM SYNTAX-CHECK THRU SYNTAX-CHECK-WRITE.                NC1394.2
051300 IND-TEST-004-05.                                                 NC1394.2
051400     IF ELEM2 (INDEX1 + 1, +2, INDEX3 - 2)                        NC1394.2
051500         IS EQUAL TO EXPECTED-VALUE                               NC1394.2
051600              MOVE "VVV" TO TEMP.                                 NC1394.2
051700     PERFORM SYNTAX-CHECK THRU SYNTAX-CHECK-WRITE.                NC1394.2
051800 IND-TEST-004-06.                                                 NC1394.2
051900     MOVE ELEM2(INDEX1 + 1 +2 INDEX3 - 2)   TO  TEMP.             NC1394.2
052000     PERFORM SYNTAX-CHECK THRU SYNTAX-CHECK-WRITE.                NC1394.2
052100 IND-TEST-004-07.                                                 NC1394.2
052200     MOVE ELEM2 (+3 +2 INDEX3 - 2)  TO TEMP.                      NC1394.2
052300     PERFORM SYNTAX-CHECK THRU SYNTAX-CHECK-WRITE.                NC1394.2
052400 IND-TEST-004-08.                                                 NC1394.2
052500     MOVE ELEM2 (INDEX1 + 1 +2 +2) TO TEMP.                       NC1394.2
052600     PERFORM SYNTAX-CHECK THRU SYNTAX-CHECK-WRITE.                NC1394.2
052700 IND-TEST-004-09.                                                 NC1394.2
052800     MOVE ELEM2(INDEX1 + 1, INDEX2 + 1, +2) TO TEMP.              NC1394.2
052900     PERFORM SYNTAX-CHECK THRU SYNTAX-CHECK-WRITE.                NC1394.2
053000 IND-TEST-004-10.                                                 NC1394.2
053100     MOVE ELEM2 (+3 INDEX2 + 1 +2) TO TEMP.                       NC1394.2
053200     PERFORM SYNTAX-CHECK THRU SYNTAX-CHECK-WRITE.                NC1394.2
053300     GO TO IND-INIT-005.                                          NC1394.2
053400 IND-DELETE-004.                                                  NC1394.2
053500     PERFORM DE-LETE.                                             NC1394.2
053600     PERFORM SYNTAX-CHECK-WRITE.                                  NC1394.2
053700 IND-INIT-005.                                                    NC1394.2
053800*        THIS TEST MIXES SIGNED AND UNSIGNED LITERALS             NC1394.2
053900*    WITH RELATIVE INDEXING.                                      NC1394.2
054000     MOVE ZERO TO REC-CT.                                         NC1394.2
054100     MOVE SPACE TO TEMP.                                          NC1394.2
054200     MOVE "PPP" TO EXPECTED-VALUE.                                NC1394.2
054300     MOVE "IND-TEST-005" TO PAR-NAME.                             NC1394.2
054400     SET INDEX1 TO 3.                                             NC1394.2
054500     SET INDEX2 TO 1.                                             NC1394.2
054600     SET INDEX3 TO 2.                                             NC1394.2
054700 IND-TEST-005-01.                                                 NC1394.2
054800     MOVE ELEM2 (+2, INDEX2 + 1, 4)  TO  TEMP.                    NC1394.2
054900     PERFORM SYNTAX-CHECK THRU SYNTAX-CHECK-WRITE.                NC1394.2
055000 IND-TEST-005-02.                                                 NC1394.2
055100     MOVE ELEM2(+2 INDEX2 + 1 4) TO TEMP.                         NC1394.2
055200     PERFORM SYNTAX-CHECK THRU SYNTAX-CHECK-WRITE.                NC1394.2
055300 IND-TEST-005-03.                                                 NC1394.2
055400     MOVE ELEM2 (2 +2 INDEX3 + 2) TO TEMP.                        NC1394.2
055500     PERFORM SYNTAX-CHECK THRU SYNTAX-CHECK-WRITE.                NC1394.2
055600 IND-TEST-005-04.                                                 NC1394.2
055700     IF ELEM2 (INDEX1 - 1, 2  +4) IS EQUAL TO EXPECTED-VALUE      NC1394.2
055800              MOVE "PPP" TO TEMP.                                 NC1394.2
055900     PERFORM SYNTAX-CHECK THRU SYNTAX-CHECK-WRITE.                NC1394.2
056000 IND-TEST-005-05.                                                 NC1394.2
056100     MOVE ELEM2(+2 2 INDEX3 + 2) TO TEMP.                         NC1394.2
056200     PERFORM SYNTAX-CHECK THRU SYNTAX-CHECK-WRITE.                NC1394.2
056300     GO TO CCVS-EXIT.                                             NC1394.2
056400 IND-DELETE-005.                                                  NC1394.2
056500     PERFORM DE-LETE.                                             NC1394.2
056600     PERFORM SYNTAX-CHECK-WRITE.                                  NC1394.2
056700 IND-INIT-006.                                                    NC1394.2
056800     MOVE "IV-21 4.3.8.2" TO ANSI-REFERENCE.                      NC1394.2
056900*        THIS TEST MIXES DATA-NAMES WITH RELATIVE INDEXING.       NC1394.2
057000     MOVE ZERO TO REC-CT.                                         NC1394.2
057100     MOVE SPACE TO TEMP.                                          NC1394.2
057200     MOVE "PPP" TO EXPECTED-VALUE.                                NC1394.2
057300     MOVE "IND-TEST-006" TO PAR-NAME.                             NC1394.2
057400     SET INDEX1 TO 3.                                             NC1394.2
057500     SET INDEX2 TO 1.                                             NC1394.2
057600     SET INDEX3 TO 2.                                             NC1394.2
057700     MOVE    2 TO WS-2.                                           NC1394.2
057800     MOVE   +2 TO WS-PLUS-2.                                      NC1394.2
057900     MOVE    4 TO WS-4.                                           NC1394.2
058000     MOVE   +4 TO WS-PLUS-4.                                      NC1394.2
058100 IND-TEST-006-01.                                                 NC1394.2
058200     MOVE ELEM2 (WS-PLUS-2, INDEX2 + 1,  WS-4)  TO  TEMP.         NC1394.2
058300     PERFORM SYNTAX-CHECK THRU SYNTAX-CHECK-WRITE.                NC1394.2
058400 IND-TEST-006-02.                                                 NC1394.2
058500     MOVE ELEM2(WS-PLUS-2 INDEX2 + 1  WS-4) TO TEMP.              NC1394.2
058600     PERFORM SYNTAX-CHECK THRU SYNTAX-CHECK-WRITE.                NC1394.2
058700 IND-TEST-006-03.                                                 NC1394.2
058800     MOVE ELEM2 ( WS-2 WS-PLUS-2 INDEX3 + 2) TO TEMP.             NC1394.2
058900     PERFORM SYNTAX-CHECK THRU SYNTAX-CHECK-WRITE.                NC1394.2
059000 IND-TEST-006-04.                                                 NC1394.2
059100     IF ELEM2 (INDEX1 - 1,  WS-2   WS-PLUS-4) = EXPECTED-VALUE    NC1394.2
059200              MOVE "PPP" TO TEMP.                                 NC1394.2
059300     PERFORM SYNTAX-CHECK THRU SYNTAX-CHECK-WRITE.                NC1394.2
059400 IND-TEST-006-05.                                                 NC1394.2
059500     MOVE ELEM2(WS-PLUS-2  WS-2 INDEX3 + 2) TO TEMP.              NC1394.2
059600     PERFORM SYNTAX-CHECK THRU SYNTAX-CHECK-WRITE.                NC1394.2
059700     GO TO CCVS-EXIT.                                             NC1394.2
059800 IND-DELETE-006.                                                  NC1394.2
059900     PERFORM DE-LETE.                                             NC1394.2
060000     PERFORM SYNTAX-CHECK-WRITE.                                  NC1394.2
060100     GO TO CCVS-EXIT.                                             NC1394.2
060200 SECT-TH110-0003 SECTION.                                         NC1394.2
060300 SYNTAX-CHECK.                                                    NC1394.2
060400     ADD 1 TO REC-CT.                                             NC1394.2
060500     IF TEMP IS EQUAL TO EXPECTED-VALUE                           NC1394.2
060600              PERFORM PASS                                        NC1394.2
060700              GO TO SYNTAX-CHECK-WRITE.                           NC1394.2
060800 SYNTAX-FAIL.                                                     NC1394.2
060900     PERFORM FAIL.                                                NC1394.2
061000     MOVE TEMP TO COMPUTED-A.                                     NC1394.2
061100     MOVE EXPECTED-VALUE TO CORRECT-A.                            NC1394.2
061200 SYNTAX-CHECK-WRITE.                                              NC1394.2
061300     PERFORM PRINT-DETAIL.                                        NC1394.2
061400     MOVE SPACE TO TEMP.                                          NC1394.2
061500 CCVS-EXIT SECTION.                                               NC1394.2
061600 CCVS-999999.                                                     NC1394.2
061700     GO TO CLOSE-FILES.                                           NC1394.2
      *END-OF,NC139A                                                                  
