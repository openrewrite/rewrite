      *HEADER,COBOL,NC133A                                                            
000100 IDENTIFICATION DIVISION.                                         NC1334.2
000200 PROGRAM-ID.                                                      NC1334.2
000300     NC133A.                                                      NC1334.2
000400****************************************************************  NC1334.2
000500*                                                              *  NC1334.2
000600*    VALIDATION FOR:-                                          *  NC1334.2
000700*                                                              *  NC1334.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1334.2
000900*                                                              *  NC1334.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC1334.2
001100*                                                              *  NC1334.2
001200****************************************************************  NC1334.2
001300*                                                              *  NC1334.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  NC1334.2
001500*                                                              *  NC1334.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  NC1334.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  NC1334.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  NC1334.2
001900*                                                              *  NC1334.2
002000****************************************************************  NC1334.2
002100*                                                                 NC1334.2
002200*     PROGRAM NC133A TESTS THE USE OF FORMAT 1 OF THE SET         NC1334.2
002300*     STATEMENT USING VARIOUS INTEGERS, INDEX-NAMES AND           NC1334.2
002400*     IDENTIFIERS.                                                NC1334.2
002500*     REDEFINED SINGLE LEVEL TABLES ARE USED.                     NC1334.2
002600*                                                                 NC1334.2
002700 ENVIRONMENT DIVISION.                                            NC1334.2
002800 CONFIGURATION SECTION.                                           NC1334.2
002900 SOURCE-COMPUTER.                                                 NC1334.2
003000     XXXXX082.                                                    NC1334.2
003100 OBJECT-COMPUTER.                                                 NC1334.2
003200     XXXXX083.                                                    NC1334.2
003300 INPUT-OUTPUT SECTION.                                            NC1334.2
003400 FILE-CONTROL.                                                    NC1334.2
003500     SELECT PRINT-FILE ASSIGN TO                                  NC1334.2
003600     XXXXX055.                                                    NC1334.2
003700 DATA DIVISION.                                                   NC1334.2
003800 FILE SECTION.                                                    NC1334.2
003900 FD  PRINT-FILE.                                                  NC1334.2
004000 01  PRINT-REC PICTURE X(120).                                    NC1334.2
004100 01  DUMMY-RECORD PICTURE X(120).                                 NC1334.2
004200 WORKING-STORAGE SECTION.                                         NC1334.2
004300 77  SGN-IDX                     PIC S9(18) VALUE ZERO.           NC1334.2
004400 77  UNSGN-IDX                   PIC 9(18)  VALUE ZERO.           NC1334.2
004500 77  USE-IDX                     USAGE INDEX.                     NC1334.2
004600 77  COMP-U-IDX18  PICTURE 9(18) COMPUTATIONAL VALUE ZERO.        NC1334.2
004700 77  COMP-S-IDX18 PICTURE S9(18) COMPUTATIONAL VALUE ZERO.        NC1334.2
004800 77  COMP-U-IDX1   PICTURE 9 COMPUTATIONAL VALUE ZERO.            NC1334.2
004900 77  COMP-S-IDX1  PICTURE S9 COMPUTATIONAL VALUE ZERO.            NC1334.2
005000 01   INDEX-VALUE  PIC 9999.                                      NC1334.2
005100 01  TABLE-A-VALUES PIC X(20) VALUE "11122233344415263748".       NC1334.2
005200 01  TABLE-A.                                                     NC1334.2
005300     02  ENTRY-A-1 PICTURE XXX OCCURS 4 TIMES INDEXED IDX-1.      NC1334.2
005400     02  ENTRY-A-2  OCCURS 4 TIMES INDEXED BY IDX-2.              NC1334.2
005500         03  ENTRY-A-3  PIC X.                                    NC1334.2
005600         03  ENTRY-A-4  PIC X.                                    NC1334.2
005700 01  TABLE-A1 REDEFINES TABLE-A.                                  NC1334.2
005800     02  ENTRY-A-5  PICTURE XXX OCCURS 4 TIMES.                   NC1334.2
005900     02  ENTRY-A-6 OCCURS 4 TIMES INDEXED BY IDX-X2.              NC1334.2
006000         03  ENTRY-A-7 PIC X.                                     NC1334.2
006100         03  ENTRY-A-8 PIC X.                                     NC1334.2
006200 01  TABLE-B.                                                     NC1334.2
006300     02  ENTRY-B-1  PIC X(4) VALUE "1234".                        NC1334.2
006400     02  ENTRY-B-2 REDEFINES ENTRY-B-1 PIC 9 OCCURS 4 INDEXED     NC1334.2
006500              BY  IDX-3.                                          NC1334.2
006600 01  TEST-RESULTS.                                                NC1334.2
006700     02 FILLER                   PIC X      VALUE SPACE.          NC1334.2
006800     02 FEATURE                  PIC X(20)  VALUE SPACE.          NC1334.2
006900     02 FILLER                   PIC X      VALUE SPACE.          NC1334.2
007000     02 P-OR-F                   PIC X(5)   VALUE SPACE.          NC1334.2
007100     02 FILLER                   PIC X      VALUE SPACE.          NC1334.2
007200     02  PAR-NAME.                                                NC1334.2
007300       03 FILLER                 PIC X(19)  VALUE SPACE.          NC1334.2
007400       03  PARDOT-X              PIC X      VALUE SPACE.          NC1334.2
007500       03 DOTVALUE               PIC 99     VALUE ZERO.           NC1334.2
007600     02 FILLER                   PIC X(8)   VALUE SPACE.          NC1334.2
007700     02 RE-MARK                  PIC X(61).                       NC1334.2
007800 01  TEST-COMPUTED.                                               NC1334.2
007900     02 FILLER                   PIC X(30)  VALUE SPACE.          NC1334.2
008000     02 FILLER                   PIC X(17)  VALUE                 NC1334.2
008100            "       COMPUTED=".                                   NC1334.2
008200     02 COMPUTED-X.                                               NC1334.2
008300     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          NC1334.2
008400     03 COMPUTED-N               REDEFINES COMPUTED-A             NC1334.2
008500                                 PIC -9(9).9(9).                  NC1334.2
008600     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         NC1334.2
008700     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     NC1334.2
008800     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     NC1334.2
008900     03       CM-18V0 REDEFINES COMPUTED-A.                       NC1334.2
009000         04 COMPUTED-18V0                    PIC -9(18).          NC1334.2
009100         04 FILLER                           PIC X.               NC1334.2
009200     03 FILLER PIC X(50) VALUE SPACE.                             NC1334.2
009300 01  TEST-CORRECT.                                                NC1334.2
009400     02 FILLER PIC X(30) VALUE SPACE.                             NC1334.2
009500     02 FILLER PIC X(17) VALUE "       CORRECT =".                NC1334.2
009600     02 CORRECT-X.                                                NC1334.2
009700     03 CORRECT-A                  PIC X(20) VALUE SPACE.         NC1334.2
009800     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      NC1334.2
009900     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         NC1334.2
010000     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     NC1334.2
010100     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     NC1334.2
010200     03      CR-18V0 REDEFINES CORRECT-A.                         NC1334.2
010300         04 CORRECT-18V0                     PIC -9(18).          NC1334.2
010400         04 FILLER                           PIC X.               NC1334.2
010500     03 FILLER PIC X(2) VALUE SPACE.                              NC1334.2
010600     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     NC1334.2
010700 01  CCVS-C-1.                                                    NC1334.2
010800     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PANC1334.2
010900-    "SS  PARAGRAPH-NAME                                          NC1334.2
011000-    "       REMARKS".                                            NC1334.2
011100     02 FILLER                     PIC X(20)    VALUE SPACE.      NC1334.2
011200 01  CCVS-C-2.                                                    NC1334.2
011300     02 FILLER                     PIC X        VALUE SPACE.      NC1334.2
011400     02 FILLER                     PIC X(6)     VALUE "TESTED".   NC1334.2
011500     02 FILLER                     PIC X(15)    VALUE SPACE.      NC1334.2
011600     02 FILLER                     PIC X(4)     VALUE "FAIL".     NC1334.2
011700     02 FILLER                     PIC X(94)    VALUE SPACE.      NC1334.2
011800 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       NC1334.2
011900 01  REC-CT                        PIC 99       VALUE ZERO.       NC1334.2
012000 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       NC1334.2
012100 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       NC1334.2
012200 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       NC1334.2
012300 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       NC1334.2
012400 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       NC1334.2
012500 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       NC1334.2
012600 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      NC1334.2
012700 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       NC1334.2
012800 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     NC1334.2
012900 01  CCVS-H-1.                                                    NC1334.2
013000     02  FILLER                    PIC X(39)    VALUE SPACES.     NC1334.2
013100     02  FILLER                    PIC X(42)    VALUE             NC1334.2
013200     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 NC1334.2
013300     02  FILLER                    PIC X(39)    VALUE SPACES.     NC1334.2
013400 01  CCVS-H-2A.                                                   NC1334.2
013500   02  FILLER                        PIC X(40)  VALUE SPACE.      NC1334.2
013600   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  NC1334.2
013700   02  FILLER                        PIC XXXX   VALUE             NC1334.2
013800     "4.2 ".                                                      NC1334.2
013900   02  FILLER                        PIC X(28)  VALUE             NC1334.2
014000            " COPY - NOT FOR DISTRIBUTION".                       NC1334.2
014100   02  FILLER                        PIC X(41)  VALUE SPACE.      NC1334.2
014200                                                                  NC1334.2
014300 01  CCVS-H-2B.                                                   NC1334.2
014400   02  FILLER                        PIC X(15)  VALUE             NC1334.2
014500            "TEST RESULT OF ".                                    NC1334.2
014600   02  TEST-ID                       PIC X(9).                    NC1334.2
014700   02  FILLER                        PIC X(4)   VALUE             NC1334.2
014800            " IN ".                                               NC1334.2
014900   02  FILLER                        PIC X(12)  VALUE             NC1334.2
015000     " HIGH       ".                                              NC1334.2
015100   02  FILLER                        PIC X(22)  VALUE             NC1334.2
015200            " LEVEL VALIDATION FOR ".                             NC1334.2
015300   02  FILLER                        PIC X(58)  VALUE             NC1334.2
015400     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1334.2
015500 01  CCVS-H-3.                                                    NC1334.2
015600     02  FILLER                      PIC X(34)  VALUE             NC1334.2
015700            " FOR OFFICIAL USE ONLY    ".                         NC1334.2
015800     02  FILLER                      PIC X(58)  VALUE             NC1334.2
015900     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC1334.2
016000     02  FILLER                      PIC X(28)  VALUE             NC1334.2
016100            "  COPYRIGHT   1985 ".                                NC1334.2
016200 01  CCVS-E-1.                                                    NC1334.2
016300     02 FILLER                       PIC X(52)  VALUE SPACE.      NC1334.2
016400     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              NC1334.2
016500     02 ID-AGAIN                     PIC X(9).                    NC1334.2
016600     02 FILLER                       PIC X(45)  VALUE SPACES.     NC1334.2
016700 01  CCVS-E-2.                                                    NC1334.2
016800     02  FILLER                      PIC X(31)  VALUE SPACE.      NC1334.2
016900     02  FILLER                      PIC X(21)  VALUE SPACE.      NC1334.2
017000     02 CCVS-E-2-2.                                               NC1334.2
017100         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      NC1334.2
017200         03 FILLER                   PIC X      VALUE SPACE.      NC1334.2
017300         03 ENDER-DESC               PIC X(44)  VALUE             NC1334.2
017400            "ERRORS ENCOUNTERED".                                 NC1334.2
017500 01  CCVS-E-3.                                                    NC1334.2
017600     02  FILLER                      PIC X(22)  VALUE             NC1334.2
017700            " FOR OFFICIAL USE ONLY".                             NC1334.2
017800     02  FILLER                      PIC X(12)  VALUE SPACE.      NC1334.2
017900     02  FILLER                      PIC X(58)  VALUE             NC1334.2
018000     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1334.2
018100     02  FILLER                      PIC X(13)  VALUE SPACE.      NC1334.2
018200     02 FILLER                       PIC X(15)  VALUE             NC1334.2
018300             " COPYRIGHT 1985".                                   NC1334.2
018400 01  CCVS-E-4.                                                    NC1334.2
018500     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      NC1334.2
018600     02 FILLER                       PIC X(4)   VALUE " OF ".     NC1334.2
018700     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      NC1334.2
018800     02 FILLER                       PIC X(40)  VALUE             NC1334.2
018900      "  TESTS WERE EXECUTED SUCCESSFULLY".                       NC1334.2
019000 01  XXINFO.                                                      NC1334.2
019100     02 FILLER                       PIC X(19)  VALUE             NC1334.2
019200            "*** INFORMATION ***".                                NC1334.2
019300     02 INFO-TEXT.                                                NC1334.2
019400       04 FILLER                     PIC X(8)   VALUE SPACE.      NC1334.2
019500       04 XXCOMPUTED                 PIC X(20).                   NC1334.2
019600       04 FILLER                     PIC X(5)   VALUE SPACE.      NC1334.2
019700       04 XXCORRECT                  PIC X(20).                   NC1334.2
019800     02 INF-ANSI-REFERENCE           PIC X(48).                   NC1334.2
019900 01  HYPHEN-LINE.                                                 NC1334.2
020000     02 FILLER  PIC IS X VALUE IS SPACE.                          NC1334.2
020100     02 FILLER  PIC IS X(65)    VALUE IS "************************NC1334.2
020200-    "*****************************************".                 NC1334.2
020300     02 FILLER  PIC IS X(54)    VALUE IS "************************NC1334.2
020400-    "******************************".                            NC1334.2
020500 01  CCVS-PGM-ID                     PIC X(9)   VALUE             NC1334.2
020600     "NC133A".                                                    NC1334.2
020700 PROCEDURE DIVISION.                                              NC1334.2
020800 CCVS1 SECTION.                                                   NC1334.2
020900 OPEN-FILES.                                                      NC1334.2
021000     OPEN     OUTPUT PRINT-FILE.                                  NC1334.2
021100     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   NC1334.2
021200     MOVE    SPACE TO TEST-RESULTS.                               NC1334.2
021300     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             NC1334.2
021400     GO TO CCVS1-EXIT.                                            NC1334.2
021500 CLOSE-FILES.                                                     NC1334.2
021600     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   NC1334.2
021700 TERMINATE-CCVS.                                                  NC1334.2
021800*    EXIT PROGRAM.                                                NC1334.2
021900*TERMINATE-CALL.                                                  NC1334.2
022000     STOP     RUN.                                                NC1334.2
022100 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         NC1334.2
022200 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           NC1334.2
022300 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          NC1334.2
022400 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      NC1334.2
022500     MOVE "****TEST DELETED****" TO RE-MARK.                      NC1334.2
022600 PRINT-DETAIL.                                                    NC1334.2
022700     IF REC-CT NOT EQUAL TO ZERO                                  NC1334.2
022800             MOVE "." TO PARDOT-X                                 NC1334.2
022900             MOVE REC-CT TO DOTVALUE.                             NC1334.2
023000     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      NC1334.2
023100     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               NC1334.2
023200        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 NC1334.2
023300          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 NC1334.2
023400     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              NC1334.2
023500     MOVE SPACE TO CORRECT-X.                                     NC1334.2
023600     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         NC1334.2
023700     MOVE     SPACE TO RE-MARK.                                   NC1334.2
023800 HEAD-ROUTINE.                                                    NC1334.2
023900     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC1334.2
024000     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC1334.2
024100     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC1334.2
024200     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC1334.2
024300 COLUMN-NAMES-ROUTINE.                                            NC1334.2
024400     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1334.2
024500     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1334.2
024600     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        NC1334.2
024700 END-ROUTINE.                                                     NC1334.2
024800     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.NC1334.2
024900 END-RTN-EXIT.                                                    NC1334.2
025000     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1334.2
025100 END-ROUTINE-1.                                                   NC1334.2
025200      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      NC1334.2
025300      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               NC1334.2
025400      ADD PASS-COUNTER TO ERROR-HOLD.                             NC1334.2
025500*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   NC1334.2
025600      MOVE PASS-COUNTER TO CCVS-E-4-1.                            NC1334.2
025700      MOVE ERROR-HOLD TO CCVS-E-4-2.                              NC1334.2
025800      MOVE CCVS-E-4 TO CCVS-E-2-2.                                NC1334.2
025900      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           NC1334.2
026000  END-ROUTINE-12.                                                 NC1334.2
026100      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        NC1334.2
026200     IF       ERROR-COUNTER IS EQUAL TO ZERO                      NC1334.2
026300         MOVE "NO " TO ERROR-TOTAL                                NC1334.2
026400         ELSE                                                     NC1334.2
026500         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       NC1334.2
026600     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           NC1334.2
026700     PERFORM WRITE-LINE.                                          NC1334.2
026800 END-ROUTINE-13.                                                  NC1334.2
026900     IF DELETE-COUNTER IS EQUAL TO ZERO                           NC1334.2
027000         MOVE "NO " TO ERROR-TOTAL  ELSE                          NC1334.2
027100         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      NC1334.2
027200     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   NC1334.2
027300     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1334.2
027400      IF   INSPECT-COUNTER EQUAL TO ZERO                          NC1334.2
027500          MOVE "NO " TO ERROR-TOTAL                               NC1334.2
027600      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   NC1334.2
027700      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            NC1334.2
027800      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          NC1334.2
027900     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1334.2
028000 WRITE-LINE.                                                      NC1334.2
028100     ADD 1 TO RECORD-COUNT.                                       NC1334.2
028200*    IF RECORD-COUNT GREATER 42                                   NC1334.2
028300*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          NC1334.2
028400*        MOVE SPACE TO DUMMY-RECORD                               NC1334.2
028500*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  NC1334.2
028600*        MOVE CCVS-H-1  TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   NC1334.2
028700*        MOVE CCVS-H-2A TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   NC1334.2
028800*        MOVE CCVS-H-2B TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   NC1334.2
028900*        MOVE CCVS-H-3  TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   NC1334.2
029000*        MOVE CCVS-C-1  TO DUMMY-RECORD  PERFORM WRT-LN           NC1334.2
029100*        MOVE CCVS-C-2  TO DUMMY-RECORD  PERFORM WRT-LN           NC1334.2
029200*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          NC1334.2
029300*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          NC1334.2
029400*        MOVE ZERO TO RECORD-COUNT.                               NC1334.2
029500     PERFORM WRT-LN.                                              NC1334.2
029600 WRT-LN.                                                          NC1334.2
029700     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               NC1334.2
029800     MOVE SPACE TO DUMMY-RECORD.                                  NC1334.2
029900 BLANK-LINE-PRINT.                                                NC1334.2
030000     PERFORM WRT-LN.                                              NC1334.2
030100 FAIL-ROUTINE.                                                    NC1334.2
030200     IF     COMPUTED-X NOT EQUAL TO SPACE                         NC1334.2
030300            GO TO FAIL-ROUTINE-WRITE.                             NC1334.2
030400     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.NC1334.2
030500     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC1334.2
030600     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   NC1334.2
030700     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1334.2
030800     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC1334.2
030900     GO TO  FAIL-ROUTINE-EX.                                      NC1334.2
031000 FAIL-ROUTINE-WRITE.                                              NC1334.2
031100     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         NC1334.2
031200     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 NC1334.2
031300     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. NC1334.2
031400     MOVE   SPACES TO COR-ANSI-REFERENCE.                         NC1334.2
031500 FAIL-ROUTINE-EX. EXIT.                                           NC1334.2
031600 BAIL-OUT.                                                        NC1334.2
031700     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   NC1334.2
031800     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           NC1334.2
031900 BAIL-OUT-WRITE.                                                  NC1334.2
032000     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  NC1334.2
032100     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC1334.2
032200     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1334.2
032300     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC1334.2
032400 BAIL-OUT-EX. EXIT.                                               NC1334.2
032500 CCVS1-EXIT.                                                      NC1334.2
032600     EXIT.                                                        NC1334.2
032700 SECT-TH133A-001 SECTION.                                         NC1334.2
032800 TH-04-001.                                                       NC1334.2
032900 IDX-INIT-A.                                                      NC1334.2
033000     MOVE "INDEXING " TO FEATURE.                                 NC1334.2
033100     MOVE TABLE-A-VALUES TO TABLE-A.                              NC1334.2
033200     IF TABLE-A EQUAL TO TABLE-A-VALUES                           NC1334.2
033300         PERFORM PASS                                             NC1334.2
033400         MOVE "TABLE CREATED CORRECTLY" TO RE-MARK                NC1334.2
033500         GO TO INIT-WRITE.                                        NC1334.2
033600     MOVE "TABLE CREATED INCORRECTLY" TO RE-MARK.                 NC1334.2
033700     PERFORM FAIL.                                                NC1334.2
033800     PERFORM INIT-WRITE.                                          NC1334.2
033900     GO TO   CCVS-EXIT.                                           NC1334.2
034000 INIT-WRITE.                                                      NC1334.2
034100     MOVE     "TABLE BUILD" TO PAR-NAME.                          NC1334.2
034200     PERFORM PRINT-DETAIL.                                        NC1334.2
034300 IDX-TEST-1.                                                      NC1334.2
034400     SET IDX-1 TO 3.                                              NC1334.2
034500     IF ENTRY-A-1 (IDX-1) EQUAL TO "333"                          NC1334.2
034600         PERFORM PASS                                             NC1334.2
034700         GO TO IDX-WRITE-1.                                       NC1334.2
034800     MOVE ENTRY-A-1 (IDX-1) TO COMPUTED-A.                        NC1334.2
034900     MOVE "333" TO CORRECT-A.                                     NC1334.2
035000     PERFORM FAIL.                                                NC1334.2
035100     GO TO IDX-WRITE-1.                                           NC1334.2
035200 IDX-DELETE-1.                                                    NC1334.2
035300     PERFORM DE-LETE.                                             NC1334.2
035400 IDX-WRITE-1.                                                     NC1334.2
035500     MOVE "IDX-TEST-1 " TO PAR-NAME.                              NC1334.2
035600     PERFORM PRINT-DETAIL.                                        NC1334.2
035700 IDX-TEST-2.                                                      NC1334.2
035800     SET IDX-1 TO 2.                                              NC1334.2
035900     IF ENTRY-A-1 (IDX-1) EQUAL TO "222"                          NC1334.2
036000         PERFORM PASS                                             NC1334.2
036100         GO TO IDX-WRITE-2.                                       NC1334.2
036200     MOVE 222 TO CORRECT-A.                                       NC1334.2
036300     MOVE ENTRY-A-1 (IDX-1) TO COMPUTED-A.                        NC1334.2
036400     PERFORM FAIL.                                                NC1334.2
036500     GO TO IDX-WRITE-2.                                           NC1334.2
036600 IDX-DELETE-2.                                                    NC1334.2
036700     PERFORM DE-LETE.                                             NC1334.2
036800 IDX-WRITE-2.                                                     NC1334.2
036900     MOVE "IDX-TEST-2" TO PAR-NAME.                               NC1334.2
037000     PERFORM PRINT-DETAIL.                                        NC1334.2
037100 IDX-TEST-3.                                                      NC1334.2
037200     SET IDX-1 TO 000001.                                         NC1334.2
037300     IF ENTRY-A-1 (IDX-1) EQUAL TO "111"                          NC1334.2
037400         PERFORM PASS                                             NC1334.2
037500         GO TO IDX-WRITE-3.                                       NC1334.2
037600     MOVE ENTRY-A-1 (IDX-1) TO COMPUTED-A.                        NC1334.2
037700     MOVE 111 TO CORRECT-A.                                       NC1334.2
037800     PERFORM FAIL.                                                NC1334.2
037900     GO TO IDX-WRITE-3.                                           NC1334.2
038000 IDX-DELETE-3.                                                    NC1334.2
038100     PERFORM DE-LETE.                                             NC1334.2
038200 IDX-WRITE-3.                                                     NC1334.2
038300     MOVE "IDX-TEST-3" TO PAR-NAME.                               NC1334.2
038400     PERFORM PRINT-DETAIL.                                        NC1334.2
038500 IDX-TEST-4.                                                      NC1334.2
038600     SET IDX-1 TO 000000000000000004.                             NC1334.2
038700     IF ENTRY-A-1 (IDX-1) EQUAL TO "444"                          NC1334.2
038800         PERFORM PASS                                             NC1334.2
038900         GO TO IDX-WRITE-4.                                       NC1334.2
039000     MOVE ENTRY-A-1 (IDX-1) TO COMPUTED-A.                        NC1334.2
039100     MOVE 444 TO CORRECT-A.                                       NC1334.2
039200     PERFORM FAIL.                                                NC1334.2
039300     GO TO IDX-WRITE-4.                                           NC1334.2
039400 IDX-DELETE-4.                                                    NC1334.2
039500     PERFORM DE-LETE.                                             NC1334.2
039600 IDX-WRITE-4.                                                     NC1334.2
039700     MOVE "IDX-TEST-4" TO PAR-NAME.                               NC1334.2
039800     PERFORM PRINT-DETAIL.                                        NC1334.2
039900 IDX-TEST-5.                                                      NC1334.2
040000     MOVE 3 TO SGN-IDX.                                           NC1334.2
040100     SET IDX-2 TO SGN-IDX.                                        NC1334.2
040200     IF ENTRY-A-4 (IDX-2) EQUAL TO "7"                            NC1334.2
040300         PERFORM PASS                                             NC1334.2
040400         GO TO IDX-WRITE-5.                                       NC1334.2
040500     MOVE ENTRY-A-4 (IDX-2) TO COMPUTED-A.                        NC1334.2
040600     MOVE 7 TO CORRECT-A.                                         NC1334.2
040700     PERFORM FAIL.                                                NC1334.2
040800     GO TO IDX-WRITE-5.                                           NC1334.2
040900 IDX-DELETE-5.                                                    NC1334.2
041000     PERFORM DE-LETE.                                             NC1334.2
041100 IDX-WRITE-5.                                                     NC1334.2
041200     MOVE "IDX-TEST-5" TO PAR-NAME.                               NC1334.2
041300     PERFORM PRINT-DETAIL.                                        NC1334.2
041400     MOVE "SET STATEMENT" TO FEATURE.                             NC1334.2
041500 IDX-TEST-6.                                                      NC1334.2
041600     MOVE 1 TO UNSGN-IDX.                                         NC1334.2
041700     SET IDX-2 TO UNSGN-IDX.                                      NC1334.2
041800     IF ENTRY-A-2 (IDX-2) EQUAL TO "15"                           NC1334.2
041900         PERFORM PASS                                             NC1334.2
042000         GO TO IDX-WRITE-6.                                       NC1334.2
042100     MOVE ENTRY-A-2 (IDX-2) TO COMPUTED-A.                        NC1334.2
042200     MOVE 15 TO CORRECT-A.                                        NC1334.2
042300     PERFORM FAIL.                                                NC1334.2
042400     GO TO IDX-WRITE-6.                                           NC1334.2
042500 IDX-DELETE-6.                                                    NC1334.2
042600     PERFORM DE-LETE.                                             NC1334.2
042700 IDX-WRITE-6.                                                     NC1334.2
042800     MOVE "IDX-TEST-6" TO PAR-NAME.                               NC1334.2
042900     PERFORM PRINT-DETAIL.                                        NC1334.2
043000 IDX-TEST-7.                                                      NC1334.2
043100     SET IDX-1 TO 4.                                              NC1334.2
043200     SET IDX-2 TO IDX-1.                                          NC1334.2
043300     IF IDX-2 EQUAL TO 4                                          NC1334.2
043400         PERFORM PASS                                             NC1334.2
043500         GO TO IDX-WRITE-7.                                       NC1334.2
043600     MOVE 4 TO CORRECT-A.                                         NC1334.2
043700     SET      INDEX-VALUE TO IDX-2.                               NC1334.2
043800     MOVE     INDEX-VALUE TO COMPUTED-18V0.                       NC1334.2
043900     PERFORM FAIL.                                                NC1334.2
044000     GO TO IDX-WRITE-7.                                           NC1334.2
044100 IDX-DELETE-7.                                                    NC1334.2
044200     PERFORM DE-LETE.                                             NC1334.2
044300 IDX-WRITE-7.                                                     NC1334.2
044400     MOVE "IDX-TEST-7" TO PAR-NAME.                               NC1334.2
044500     PERFORM PRINT-DETAIL.                                        NC1334.2
044600 IDX-TEST-8.                                                      NC1334.2
044700     SET IDX-1 TO 4.                                              NC1334.2
044800     SET IDX-2 TO IDX-1.                                          NC1334.2
044900     IF ENTRY-A-4 (IDX-2) EQUAL TO "8"                            NC1334.2
045000         PERFORM PASS                                             NC1334.2
045100         GO TO IDX-WRITE-8.                                       NC1334.2
045200     MOVE 8 TO CORRECT-A.                                         NC1334.2
045300     MOVE     ENTRY-A-4 (IDX-2) TO COMPUTED-A.                    NC1334.2
045400     PERFORM FAIL.                                                NC1334.2
045500     GO TO IDX-WRITE-8.                                           NC1334.2
045600 IDX-DELETE-8.                                                    NC1334.2
045700     PERFORM DE-LETE.                                             NC1334.2
045800 IDX-WRITE-8.                                                     NC1334.2
045900     MOVE "IDX-TEST-8" TO PAR-NAME.                               NC1334.2
046000     PERFORM PRINT-DETAIL.                                        NC1334.2
046100 IDX-TEST-9.                                                      NC1334.2
046200     SET IDX-X2 TO 02.                                            NC1334.2
046300     SET USE-IDX TO IDX-X2.                                       NC1334.2
046400     SET IDX-2 TO USE-IDX.                                        NC1334.2
046500     IF IDX-2 EQUAL TO 2                                          NC1334.2
046600         PERFORM PASS                                             NC1334.2
046700         GO TO IDX-WRITE-9.                                       NC1334.2
046800     MOVE 2 TO CORRECT-A.                                         NC1334.2
046900     SET      INDEX-VALUE TO IDX-2.                               NC1334.2
047000     MOVE     INDEX-VALUE TO COMPUTED-18V0.                       NC1334.2
047100     PERFORM FAIL.                                                NC1334.2
047200     GO TO IDX-WRITE-9.                                           NC1334.2
047300 IDX-DELETE-9.                                                    NC1334.2
047400     PERFORM DE-LETE.                                             NC1334.2
047500 IDX-WRITE-9.                                                     NC1334.2
047600     MOVE "IDX-TEST-9" TO PAR-NAME.                               NC1334.2
047700     PERFORM PRINT-DETAIL.                                        NC1334.2
047800 IDX-TEST-10.                                                     NC1334.2
047900     SET IDX-2 TO 4.                                              NC1334.2
048000     SET USE-IDX TO IDX-2.                                        NC1334.2
048100     SET IDX-X2 TO USE-IDX.                                       NC1334.2
048200     IF ENTRY-A-8 (IDX-X2) EQUAL TO ENTRY-A-4 (IDX-2)             NC1334.2
048300         PERFORM PASS                                             NC1334.2
048400         GO TO IDX-WRITE-10.                                      NC1334.2
048500     MOVE ENTRY-A-4 (IDX-2) TO COMPUTED-A.                        NC1334.2
048600     MOVE ENTRY-A-8 (IDX-X2) TO CORRECT-A.                        NC1334.2
048700     MOVE "TABLE ENTRIES SHOULD BE EQUAL" TO RE-MARK.             NC1334.2
048800     PERFORM FAIL.                                                NC1334.2
048900     GO TO IDX-WRITE-10.                                          NC1334.2
049000 IDX-DELETE-10.                                                   NC1334.2
049100     PERFORM DE-LETE.                                             NC1334.2
049200 IDX-WRITE-10.                                                    NC1334.2
049300     MOVE "IDX-TEST-10" TO PAR-NAME.                              NC1334.2
049400     PERFORM PRINT-DETAIL.                                        NC1334.2
049500 IDX-TEST-11.                                                     NC1334.2
049600     SET IDX-3 TO 0004.                                           NC1334.2
049700     IF ENTRY-B-2 (IDX-3) EQUAL TO 4                              NC1334.2
049800         PERFORM PASS                                             NC1334.2
049900         GO TO IDX-WRITE-11.                                      NC1334.2
050000     MOVE ENTRY-B-2 (IDX-3) TO COMPUTED-A.                        NC1334.2
050100     MOVE 4 TO CORRECT-A.                                         NC1334.2
050200     PERFORM FAIL.                                                NC1334.2
050300     GO TO IDX-WRITE-11.                                          NC1334.2
050400 IDX-DELETE-11.                                                   NC1334.2
050500     PERFORM DE-LETE.                                             NC1334.2
050600 IDX-WRITE-11.                                                    NC1334.2
050700     MOVE "IDX-TEST-11" TO PAR-NAME.                              NC1334.2
050800     PERFORM PRINT-DETAIL.                                        NC1334.2
050900 IDX-TEST-12.                                                     NC1334.2
051000     SET IDX-3 TO 0000002.                                        NC1334.2
051100     IF ENTRY-B-2 (IDX-3) EQUAL TO 2                              NC1334.2
051200         PERFORM PASS                                             NC1334.2
051300         GO TO IDX-WRITE-12.                                      NC1334.2
051400     MOVE ENTRY-B-2 (IDX-3) TO COMPUTED-A.                        NC1334.2
051500     MOVE 2 TO CORRECT-A.                                         NC1334.2
051600     PERFORM FAIL.                                                NC1334.2
051700     GO TO IDX-WRITE-12.                                          NC1334.2
051800 IDX-DELETE-12.                                                   NC1334.2
051900     PERFORM DE-LETE.                                             NC1334.2
052000 IDX-WRITE-12.                                                    NC1334.2
052100     MOVE "IDX-TEST-12" TO PAR-NAME.                              NC1334.2
052200     PERFORM PRINT-DETAIL.                                        NC1334.2
052300 IDX-TEST-13.                                                     NC1334.2
052400     SET IDX-3 TO 000000000000000003.                             NC1334.2
052500     IF ENTRY-B-2 (IDX-3) EQUAL TO 3                              NC1334.2
052600         PERFORM PASS                                             NC1334.2
052700         GO TO IDX-WRITE-13.                                      NC1334.2
052800     MOVE ENTRY-B-2 (IDX-3) TO COMPUTED-A.                        NC1334.2
052900     MOVE 3 TO CORRECT-A.                                         NC1334.2
053000     PERFORM FAIL.                                                NC1334.2
053100     GO TO IDX-WRITE-13.                                          NC1334.2
053200 IDX-DELETE-13.                                                   NC1334.2
053300     PERFORM DE-LETE.                                             NC1334.2
053400 IDX-WRITE-13.                                                    NC1334.2
053500     MOVE "IDX-TEST-13" TO PAR-NAME.                              NC1334.2
053600     PERFORM PRINT-DETAIL.                                        NC1334.2
053700 IDX-TEST-14.                                                     NC1334.2
053800     SET IDX-3 TO 000000000000000004.                             NC1334.2
053900     IF ENTRY-B-2 (IDX-3) EQUAL TO 4                              NC1334.2
054000         PERFORM PASS                                             NC1334.2
054100         GO TO IDX-WRITE-14.                                      NC1334.2
054200     MOVE ENTRY-B-2 (IDX-3) TO COMPUTED-A.                        NC1334.2
054300     MOVE 4 TO CORRECT-A.                                         NC1334.2
054400     PERFORM FAIL.                                                NC1334.2
054500     GO TO IDX-WRITE-14.                                          NC1334.2
054600 IDX-DELETE-14.                                                   NC1334.2
054700     PERFORM DE-LETE.                                             NC1334.2
054800 IDX-WRITE-14.                                                    NC1334.2
054900     MOVE "IDX-TEST-14" TO PAR-NAME.                              NC1334.2
055000     PERFORM PRINT-DETAIL.                                        NC1334.2
055100 IDX-TEST-15.                                                     NC1334.2
055200     SET IDX-3 TO 000000000000000002.                             NC1334.2
055300     IF ENTRY-B-2 (IDX-3) EQUAL TO 2                              NC1334.2
055400         PERFORM PASS                                             NC1334.2
055500         GO TO IDX-WRITE-15.                                      NC1334.2
055600     MOVE ENTRY-B-2 (IDX-3) TO COMPUTED-A.                        NC1334.2
055700     MOVE 2 TO CORRECT-A.                                         NC1334.2
055800     PERFORM FAIL.                                                NC1334.2
055900     GO TO IDX-WRITE-15.                                          NC1334.2
056000 IDX-DELETE-15.                                                   NC1334.2
056100     PERFORM DE-LETE.                                             NC1334.2
056200 IDX-WRITE-15.                                                    NC1334.2
056300     MOVE "IDX-TEST-15" TO PAR-NAME.                              NC1334.2
056400     PERFORM PRINT-DETAIL.                                        NC1334.2
056500 IDX-TEST-16.                                                     NC1334.2
056600     SET IDX-3 TO 03.                                             NC1334.2
056700     SET IDX-2 TO 03.                                             NC1334.2
056800     IF ENTRY-A-3 (IDX-2) EQUAL TO ENTRY-B-2 (IDX-3)              NC1334.2
056900         PERFORM PASS                                             NC1334.2
057000         GO TO IDX-WRITE-16.                                      NC1334.2
057100     MOVE ENTRY-A-3 (IDX-2) TO COMPUTED-A.                        NC1334.2
057200     MOVE ENTRY-B-2 (IDX-3) TO CORRECT-A.                         NC1334.2
057300     MOVE "TABLE ENTRIES SHOULD BE EQUAL" TO RE-MARK.             NC1334.2
057400     PERFORM FAIL.                                                NC1334.2
057500     GO TO IDX-WRITE-16.                                          NC1334.2
057600 IDX-DELETE-16.                                                   NC1334.2
057700     PERFORM DE-LETE.                                             NC1334.2
057800 IDX-WRITE-16.                                                    NC1334.2
057900     MOVE "IDX-TEST-16" TO PAR-NAME.                              NC1334.2
058000     PERFORM PRINT-DETAIL.                                        NC1334.2
058100 IDX-TEST-17.                                                     NC1334.2
058200     MOVE 3 TO COMP-U-IDX18.                                      NC1334.2
058300     SET IDX-1 TO COMP-U-IDX18.                                   NC1334.2
058400     IF ENTRY-A-1 (IDX-1) EQUAL TO "333"                          NC1334.2
058500         PERFORM PASS                                             NC1334.2
058600         GO TO IDX-WRITE-17.                                      NC1334.2
058700     MOVE "333" TO CORRECT-A.                                     NC1334.2
058800     MOVE ENTRY-A-1 (IDX-1) TO COMPUTED-A.                        NC1334.2
058900     PERFORM FAIL.                                                NC1334.2
059000     GO TO IDX-WRITE-17.                                          NC1334.2
059100 IDX-DELETE-17.                                                   NC1334.2
059200     PERFORM DE-LETE.                                             NC1334.2
059300 IDX-WRITE-17.                                                    NC1334.2
059400     MOVE "IDX-TEST-17" TO PAR-NAME.                              NC1334.2
059500     PERFORM PRINT-DETAIL.                                        NC1334.2
059600 IDX-TEST-18.                                                     NC1334.2
059700     MOVE 4 TO COMP-S-IDX18.                                      NC1334.2
059800     SET IDX-2 TO COMP-S-IDX18.                                   NC1334.2
059900     IF ENTRY-A-2 (IDX-2) EQUAL TO "48"                           NC1334.2
060000         PERFORM PASS                                             NC1334.2
060100         GO TO IDX-WRITE-18.                                      NC1334.2
060200     MOVE "48" TO CORRECT-A.                                      NC1334.2
060300     MOVE ENTRY-A-2 (IDX-2) TO COMPUTED-A.                        NC1334.2
060400     PERFORM FAIL.                                                NC1334.2
060500     GO TO IDX-WRITE-18.                                          NC1334.2
060600 IDX-DELETE-18.                                                   NC1334.2
060700     PERFORM DE-LETE.                                             NC1334.2
060800 IDX-WRITE-18.                                                    NC1334.2
060900     MOVE "IDX-TEST-18" TO PAR-NAME.                              NC1334.2
061000     PERFORM PRINT-DETAIL.                                        NC1334.2
061100 IDX-TEST-19.                                                     NC1334.2
061200     MOVE 1 TO COMP-U-IDX1.                                       NC1334.2
061300     SET IDX-3 TO COMP-U-IDX1.                                    NC1334.2
061400     IF ENTRY-B-2 (IDX-3) EQUAL TO 1                              NC1334.2
061500         PERFORM PASS                                             NC1334.2
061600         GO TO IDX-WRITE-19.                                      NC1334.2
061700     MOVE "1" TO CORRECT-A.                                       NC1334.2
061800     MOVE ENTRY-B-2 (IDX-3) TO COMPUTED-A.                        NC1334.2
061900     PERFORM FAIL.                                                NC1334.2
062000     GO TO IDX-WRITE-19.                                          NC1334.2
062100 IDX-DELETE-19.                                                   NC1334.2
062200     PERFORM DE-LETE.                                             NC1334.2
062300 IDX-WRITE-19.                                                    NC1334.2
062400     MOVE "IDX-TEST-19" TO PAR-NAME.                              NC1334.2
062500     PERFORM PRINT-DETAIL.                                        NC1334.2
062600 IDX-TEST-20.                                                     NC1334.2
062700     MOVE 2 TO COMP-S-IDX1.                                       NC1334.2
062800     SET IDX-2 TO COMP-S-IDX1.                                    NC1334.2
062900     IF ENTRY-A-3 (IDX-2) EQUAL TO "2"                            NC1334.2
063000         PERFORM PASS                                             NC1334.2
063100         GO TO IDX-WRITE-20.                                      NC1334.2
063200     MOVE "2" TO CORRECT-A.                                       NC1334.2
063300     MOVE ENTRY-A-3 (IDX-2) TO COMPUTED-A.                        NC1334.2
063400     PERFORM FAIL.                                                NC1334.2
063500     GO TO IDX-WRITE-20.                                          NC1334.2
063600 IDX-DELETE-20.                                                   NC1334.2
063700     PERFORM DE-LETE.                                             NC1334.2
063800 IDX-WRITE-20.                                                    NC1334.2
063900     MOVE "IDX-TEST-20" TO PAR-NAME.                              NC1334.2
064000     PERFORM PRINT-DETAIL.                                        NC1334.2
064100 IDX-TEST-21.                                                     NC1334.2
064200     SET IDX-1 TO 3.                                              NC1334.2
064300     SET COMP-S-IDX18 TO IDX-1.                                   NC1334.2
064400     IF COMP-S-IDX18 EQUAL TO +3                                  NC1334.2
064500         PERFORM PASS                                             NC1334.2
064600         GO TO IDX-WRITE-21.                                      NC1334.2
064700     MOVE +3 TO CORRECT-N.                                        NC1334.2
064800     MOVE COMP-S-IDX18 TO COMPUTED-N.                             NC1334.2
064900     PERFORM FAIL.                                                NC1334.2
065000     GO TO IDX-WRITE-21.                                          NC1334.2
065100 IDX-DELETE-21.                                                   NC1334.2
065200     PERFORM DE-LETE.                                             NC1334.2
065300 IDX-WRITE-21.                                                    NC1334.2
065400     MOVE "IDX-TEST-21" TO PAR-NAME.                              NC1334.2
065500     PERFORM PRINT-DETAIL.                                        NC1334.2
065600 IDX-TEST-22.                                                     NC1334.2
065700     SET IDX-2 TO 2.                                              NC1334.2
065800     SET COMP-U-IDX1 TO IDX-2.                                    NC1334.2
065900     IF COMP-U-IDX1 EQUAL TO 2                                    NC1334.2
066000         PERFORM PASS                                             NC1334.2
066100         GO TO IDX-WRITE-22.                                      NC1334.2
066200     MOVE COMP-U-IDX1 TO COMPUTED-N.                              NC1334.2
066300     MOVE 2 TO CORRECT-N.                                         NC1334.2
066400     PERFORM FAIL.                                                NC1334.2
066500     GO TO IDX-WRITE-22.                                          NC1334.2
066600 IDX-DELETE-22.                                                   NC1334.2
066700     PERFORM DE-LETE.                                             NC1334.2
066800 IDX-WRITE-22.                                                    NC1334.2
066900     MOVE "IDX-TEST-22" TO PAR-NAME.                              NC1334.2
067000     PERFORM PRINT-DETAIL.                                        NC1334.2
067100 IDX-TEST-23.                                                     NC1334.2
067200     MOVE 4 TO COMP-S-IDX18.                                      NC1334.2
067300     SET IDX-2 TO COMP-S-IDX18.                                   NC1334.2
067400     IF COMP-S-IDX18 EQUAL TO IDX-2                               NC1334.2
067500         PERFORM PASS                                             NC1334.2
067600         GO TO IDX-WRITE-23.                                      NC1334.2
067700     SET      INDEX-VALUE TO IDX-2.                               NC1334.2
067800     MOVE     INDEX-VALUE TO COMPUTED-18V0.                       NC1334.2
067900     MOVE    COMP-S-IDX18 TO CORRECT-18V0.                        NC1334.2
068000     MOVE "INDEX VALUES SHOULD BE EQUAL" TO RE-MARK.              NC1334.2
068100     PERFORM FAIL.                                                NC1334.2
068200     GO TO IDX-WRITE-23.                                          NC1334.2
068300 IDX-DELETE-23.                                                   NC1334.2
068400     PERFORM DE-LETE.                                             NC1334.2
068500 IDX-WRITE-23.                                                    NC1334.2
068600     MOVE "IDX-TEST-23" TO PAR-NAME.                              NC1334.2
068700     PERFORM PRINT-DETAIL.                                        NC1334.2
068800 IDX-TEST-24.                                                     NC1334.2
068900     MOVE 2 TO COMP-U-IDX1.                                       NC1334.2
069000     SET IDX-3 TO COMP-U-IDX1.                                    NC1334.2
069100     IF IDX-3 EQUAL TO COMP-U-IDX1                                NC1334.2
069200         PERFORM PASS                                             NC1334.2
069300         GO TO IDX-WRITE-24.                                      NC1334.2
069400     MOVE     COMP-U-IDX1 TO CORRECT-18V0.                        NC1334.2
069500     SET      INDEX-VALUE TO IDX-3.                               NC1334.2
069600     MOVE     INDEX-VALUE TO COMPUTED-18V0.                       NC1334.2
069700     MOVE "INDEX VALUES SHOULD BE EQUAL" TO RE-MARK.              NC1334.2
069800     PERFORM FAIL.                                                NC1334.2
069900     GO TO IDX-WRITE-24.                                          NC1334.2
070000 IDX-DELETE-24.                                                   NC1334.2
070100     PERFORM DE-LETE.                                             NC1334.2
070200 IDX-WRITE-24.                                                    NC1334.2
070300     MOVE "IDX-TEST-24" TO PAR-NAME.                              NC1334.2
070400     PERFORM PRINT-DETAIL.                                        NC1334.2
070500*IDX-TEST-25.                                                     NC1334.2
070600*    THIS TEST WAS DELETED BY THE FCCTS SINCE IT DID NOT          NC1334.2
070700*    APPLY TO THE REVISED FEDERAL STANDARD - FIPS PUB 21-1.       NC1334.2
070800*IDX-TEST-26.                                                     NC1334.2
070900*    THIS TEST WAS DELETED BY THE FCCTS SINCE IT DID NOT          NC1334.2
071000*    APPLY TO THE REVISED FEDERAL STANDARD - FIPS PUB 21-1.       NC1334.2
071100 CCVS-EXIT SECTION.                                               NC1334.2
071200 CCVS-999999.                                                     NC1334.2
071300     GO TO CLOSE-FILES.                                           NC1334.2
      *END-OF,NC133A                                                                  
