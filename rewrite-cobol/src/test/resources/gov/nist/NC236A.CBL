      *HEADER,COBOL,NC236A                                                            
000100 IDENTIFICATION DIVISION.                                         NC2364.2
000200 PROGRAM-ID.                                                      NC2364.2
000300     NC236A.                                                      NC2364.2
000400****************************************************************  NC2364.2
000500*                                                              *  NC2364.2
000600*    VALIDATION FOR:-                                          *  NC2364.2
000700*                                                              *  NC2364.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2364.2
000900*                                                              *  NC2364.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC2364.2
001100*                                                              *  NC2364.2
001200****************************************************************  NC2364.2
001300*                                                              *  NC2364.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  NC2364.2
001500*                                                              *  NC2364.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  NC2364.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  NC2364.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  NC2364.2
001900*                                                              *  NC2364.2
002000****************************************************************  NC2364.2
002100                                                                  NC2364.2
002200*                                                              *  NC2364.2
002300*    PROGRAM NC236A TESTS FORMAT 1 OF THE "SEARCH" STATEMENT   *  NC2364.2
002400*    USING TWO-DIMAENSIONAL TABKES WHICH HAVE BEEN REDEFINED.  *  NC2364.2
002500*    THE OPTIONAL "VARYING" AND "AT END" PHRASES ARE USED.     *  NC2364.2
002600*                                                              *  NC2364.2
002700****************************************************************  NC2364.2
002800 ENVIRONMENT DIVISION.                                            NC2364.2
002900 CONFIGURATION SECTION.                                           NC2364.2
003000 SOURCE-COMPUTER.                                                 NC2364.2
003100     XXXXX082.                                                    NC2364.2
003200 OBJECT-COMPUTER.                                                 NC2364.2
003300     XXXXX083.                                                    NC2364.2
003400 INPUT-OUTPUT SECTION.                                            NC2364.2
003500 FILE-CONTROL.                                                    NC2364.2
003600     SELECT PRINT-FILE ASSIGN TO                                  NC2364.2
003700     XXXXX055.                                                    NC2364.2
003800 DATA DIVISION.                                                   NC2364.2
003900 FILE SECTION.                                                    NC2364.2
004000 FD  PRINT-FILE.                                                  NC2364.2
004100 01  PRINT-REC PICTURE X(120).                                    NC2364.2
004200 01  DUMMY-RECORD PICTURE X(120).                                 NC2364.2
004300 WORKING-STORAGE SECTION.                                         NC2364.2
004400 01  NOTE-1.                                                      NC2364.2
004500     02  FILLER        PIC X(74)  VALUE                           NC2364.2
004600     "NOTE 1 - CORRECT AND COMPUTED DATA ARE EQUAL BUT THE AT END NC2364.2
004700-    "PATH WAS TAKEN".                                            NC2364.2
004800     02  FILLER        PIC X(46)  VALUE SPACES.                   NC2364.2
004900 01  NOTE-2.                                                      NC2364.2
005000     02  FILLER        PIC X(112) VALUE                           NC2364.2
005100     "NOTE 2 - CORRECT AND COMPUTED DATA ARE NOT EQUAL. THE COMPUTNC2364.2
005200-    "ED ENTRY WAS EXTRACTED FROM THE TABLE BY SUBSCRIPTS.".      NC2364.2
005300     02  FILLER        PIC X(8)   VALUE SPACES.                   NC2364.2
005400 01  TABLE-A PIC X(20) VALUE "01020304050607080910".              NC2364.2
005500 01  TABLE-1 REDEFINES TABLE-A.                                   NC2364.2
005600     02  TBL-A OCCURS 10 TIMES INDEXED BY A.                      NC2364.2
005700         03  ELMT-A   PIC 99.                                     NC2364.2
005800 01  W  USAGE INDEX.                                              NC2364.2
005900 01   INDEX-VALUE  PIC 9999.                                      NC2364.2
006000 01  TABLE-B PIC X(20) VALUE "01020304050607080910".              NC2364.2
006100 01  TABLE-2 REDEFINES TABLE-B.                                   NC2364.2
006200     02  TBL-B OCCURS 10 TIMES INDEXED BY B.                      NC2364.2
006300         03  ELMT-B   PIC 99.                                     NC2364.2
006400 01  TEST-RESULTS.                                                NC2364.2
006500     02 FILLER                   PIC X      VALUE SPACE.          NC2364.2
006600     02 FEATURE                  PIC X(20)  VALUE SPACE.          NC2364.2
006700     02 FILLER                   PIC X      VALUE SPACE.          NC2364.2
006800     02 P-OR-F                   PIC X(5)   VALUE SPACE.          NC2364.2
006900     02 FILLER                   PIC X      VALUE SPACE.          NC2364.2
007000     02  PAR-NAME.                                                NC2364.2
007100       03 FILLER                 PIC X(19)  VALUE SPACE.          NC2364.2
007200       03  PARDOT-X              PIC X      VALUE SPACE.          NC2364.2
007300       03 DOTVALUE               PIC 99     VALUE ZERO.           NC2364.2
007400     02 FILLER                   PIC X(8)   VALUE SPACE.          NC2364.2
007500     02 RE-MARK                  PIC X(61).                       NC2364.2
007600 01  TEST-COMPUTED.                                               NC2364.2
007700     02 FILLER                   PIC X(30)  VALUE SPACE.          NC2364.2
007800     02 FILLER                   PIC X(17)  VALUE                 NC2364.2
007900            "       COMPUTED=".                                   NC2364.2
008000     02 COMPUTED-X.                                               NC2364.2
008100     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          NC2364.2
008200     03 COMPUTED-N               REDEFINES COMPUTED-A             NC2364.2
008300                                 PIC -9(9).9(9).                  NC2364.2
008400     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         NC2364.2
008500     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     NC2364.2
008600     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     NC2364.2
008700     03       CM-18V0 REDEFINES COMPUTED-A.                       NC2364.2
008800         04 COMPUTED-18V0                    PIC -9(18).          NC2364.2
008900         04 FILLER                           PIC X.               NC2364.2
009000     03 FILLER PIC X(50) VALUE SPACE.                             NC2364.2
009100 01  TEST-CORRECT.                                                NC2364.2
009200     02 FILLER PIC X(30) VALUE SPACE.                             NC2364.2
009300     02 FILLER PIC X(17) VALUE "       CORRECT =".                NC2364.2
009400     02 CORRECT-X.                                                NC2364.2
009500     03 CORRECT-A                  PIC X(20) VALUE SPACE.         NC2364.2
009600     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      NC2364.2
009700     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         NC2364.2
009800     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     NC2364.2
009900     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     NC2364.2
010000     03      CR-18V0 REDEFINES CORRECT-A.                         NC2364.2
010100         04 CORRECT-18V0                     PIC -9(18).          NC2364.2
010200         04 FILLER                           PIC X.               NC2364.2
010300     03 FILLER PIC X(2) VALUE SPACE.                              NC2364.2
010400     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     NC2364.2
010500 01  CCVS-C-1.                                                    NC2364.2
010600     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PANC2364.2
010700-    "SS  PARAGRAPH-NAME                                          NC2364.2
010800-    "       REMARKS".                                            NC2364.2
010900     02 FILLER                     PIC X(20)    VALUE SPACE.      NC2364.2
011000 01  CCVS-C-2.                                                    NC2364.2
011100     02 FILLER                     PIC X        VALUE SPACE.      NC2364.2
011200     02 FILLER                     PIC X(6)     VALUE "TESTED".   NC2364.2
011300     02 FILLER                     PIC X(15)    VALUE SPACE.      NC2364.2
011400     02 FILLER                     PIC X(4)     VALUE "FAIL".     NC2364.2
011500     02 FILLER                     PIC X(94)    VALUE SPACE.      NC2364.2
011600 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       NC2364.2
011700 01  REC-CT                        PIC 99       VALUE ZERO.       NC2364.2
011800 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       NC2364.2
011900 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       NC2364.2
012000 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       NC2364.2
012100 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       NC2364.2
012200 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       NC2364.2
012300 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       NC2364.2
012400 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      NC2364.2
012500 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       NC2364.2
012600 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     NC2364.2
012700 01  CCVS-H-1.                                                    NC2364.2
012800     02  FILLER                    PIC X(39)    VALUE SPACES.     NC2364.2
012900     02  FILLER                    PIC X(42)    VALUE             NC2364.2
013000     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 NC2364.2
013100     02  FILLER                    PIC X(39)    VALUE SPACES.     NC2364.2
013200 01  CCVS-H-2A.                                                   NC2364.2
013300   02  FILLER                        PIC X(40)  VALUE SPACE.      NC2364.2
013400   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  NC2364.2
013500   02  FILLER                        PIC XXXX   VALUE             NC2364.2
013600     "4.2 ".                                                      NC2364.2
013700   02  FILLER                        PIC X(28)  VALUE             NC2364.2
013800            " COPY - NOT FOR DISTRIBUTION".                       NC2364.2
013900   02  FILLER                        PIC X(41)  VALUE SPACE.      NC2364.2
014000                                                                  NC2364.2
014100 01  CCVS-H-2B.                                                   NC2364.2
014200   02  FILLER                        PIC X(15)  VALUE             NC2364.2
014300            "TEST RESULT OF ".                                    NC2364.2
014400   02  TEST-ID                       PIC X(9).                    NC2364.2
014500   02  FILLER                        PIC X(4)   VALUE             NC2364.2
014600            " IN ".                                               NC2364.2
014700   02  FILLER                        PIC X(12)  VALUE             NC2364.2
014800     " HIGH       ".                                              NC2364.2
014900   02  FILLER                        PIC X(22)  VALUE             NC2364.2
015000            " LEVEL VALIDATION FOR ".                             NC2364.2
015100   02  FILLER                        PIC X(58)  VALUE             NC2364.2
015200     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2364.2
015300 01  CCVS-H-3.                                                    NC2364.2
015400     02  FILLER                      PIC X(34)  VALUE             NC2364.2
015500            " FOR OFFICIAL USE ONLY    ".                         NC2364.2
015600     02  FILLER                      PIC X(58)  VALUE             NC2364.2
015700     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC2364.2
015800     02  FILLER                      PIC X(28)  VALUE             NC2364.2
015900            "  COPYRIGHT   1985 ".                                NC2364.2
016000 01  CCVS-E-1.                                                    NC2364.2
016100     02 FILLER                       PIC X(52)  VALUE SPACE.      NC2364.2
016200     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              NC2364.2
016300     02 ID-AGAIN                     PIC X(9).                    NC2364.2
016400     02 FILLER                       PIC X(45)  VALUE SPACES.     NC2364.2
016500 01  CCVS-E-2.                                                    NC2364.2
016600     02  FILLER                      PIC X(31)  VALUE SPACE.      NC2364.2
016700     02  FILLER                      PIC X(21)  VALUE SPACE.      NC2364.2
016800     02 CCVS-E-2-2.                                               NC2364.2
016900         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      NC2364.2
017000         03 FILLER                   PIC X      VALUE SPACE.      NC2364.2
017100         03 ENDER-DESC               PIC X(44)  VALUE             NC2364.2
017200            "ERRORS ENCOUNTERED".                                 NC2364.2
017300 01  CCVS-E-3.                                                    NC2364.2
017400     02  FILLER                      PIC X(22)  VALUE             NC2364.2
017500            " FOR OFFICIAL USE ONLY".                             NC2364.2
017600     02  FILLER                      PIC X(12)  VALUE SPACE.      NC2364.2
017700     02  FILLER                      PIC X(58)  VALUE             NC2364.2
017800     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2364.2
017900     02  FILLER                      PIC X(13)  VALUE SPACE.      NC2364.2
018000     02 FILLER                       PIC X(15)  VALUE             NC2364.2
018100             " COPYRIGHT 1985".                                   NC2364.2
018200 01  CCVS-E-4.                                                    NC2364.2
018300     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      NC2364.2
018400     02 FILLER                       PIC X(4)   VALUE " OF ".     NC2364.2
018500     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      NC2364.2
018600     02 FILLER                       PIC X(40)  VALUE             NC2364.2
018700      "  TESTS WERE EXECUTED SUCCESSFULLY".                       NC2364.2
018800 01  XXINFO.                                                      NC2364.2
018900     02 FILLER                       PIC X(19)  VALUE             NC2364.2
019000            "*** INFORMATION ***".                                NC2364.2
019100     02 INFO-TEXT.                                                NC2364.2
019200       04 FILLER                     PIC X(8)   VALUE SPACE.      NC2364.2
019300       04 XXCOMPUTED                 PIC X(20).                   NC2364.2
019400       04 FILLER                     PIC X(5)   VALUE SPACE.      NC2364.2
019500       04 XXCORRECT                  PIC X(20).                   NC2364.2
019600     02 INF-ANSI-REFERENCE           PIC X(48).                   NC2364.2
019700 01  HYPHEN-LINE.                                                 NC2364.2
019800     02 FILLER  PIC IS X VALUE IS SPACE.                          NC2364.2
019900     02 FILLER  PIC IS X(65)    VALUE IS "************************NC2364.2
020000-    "*****************************************".                 NC2364.2
020100     02 FILLER  PIC IS X(54)    VALUE IS "************************NC2364.2
020200-    "******************************".                            NC2364.2
020300 01  CCVS-PGM-ID                     PIC X(9)   VALUE             NC2364.2
020400     "NC236A".                                                    NC2364.2
020500 PROCEDURE DIVISION.                                              NC2364.2
020600 CCVS1 SECTION.                                                   NC2364.2
020700 OPEN-FILES.                                                      NC2364.2
020800     OPEN     OUTPUT PRINT-FILE.                                  NC2364.2
020900     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   NC2364.2
021000     MOVE    SPACE TO TEST-RESULTS.                               NC2364.2
021100     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             NC2364.2
021200     GO TO CCVS1-EXIT.                                            NC2364.2
021300 CLOSE-FILES.                                                     NC2364.2
021400     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   NC2364.2
021500 TERMINATE-CCVS.                                                  NC2364.2
021600*    EXIT PROGRAM.                                                NC2364.2
021700*TERMINATE-CALL.                                                  NC2364.2
021800     STOP     RUN.                                                NC2364.2
021900 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         NC2364.2
022000 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           NC2364.2
022100 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          NC2364.2
022200 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      NC2364.2
022300     MOVE "****TEST DELETED****" TO RE-MARK.                      NC2364.2
022400 PRINT-DETAIL.                                                    NC2364.2
022500     IF REC-CT NOT EQUAL TO ZERO                                  NC2364.2
022600             MOVE "." TO PARDOT-X                                 NC2364.2
022700             MOVE REC-CT TO DOTVALUE.                             NC2364.2
022800     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      NC2364.2
022900     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               NC2364.2
023000        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 NC2364.2
023100          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 NC2364.2
023200     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              NC2364.2
023300     MOVE SPACE TO CORRECT-X.                                     NC2364.2
023400     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         NC2364.2
023500     MOVE     SPACE TO RE-MARK.                                   NC2364.2
023600 HEAD-ROUTINE.                                                    NC2364.2
023700     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC2364.2
023800     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC2364.2
023900     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC2364.2
024000     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC2364.2
024100 COLUMN-NAMES-ROUTINE.                                            NC2364.2
024200     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2364.2
024300     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2364.2
024400     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        NC2364.2
024500 END-ROUTINE.                                                     NC2364.2
024600     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.NC2364.2
024700 END-RTN-EXIT.                                                    NC2364.2
024800     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2364.2
024900 END-ROUTINE-1.                                                   NC2364.2
025000      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      NC2364.2
025100      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               NC2364.2
025200      ADD PASS-COUNTER TO ERROR-HOLD.                             NC2364.2
025300*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   NC2364.2
025400      MOVE PASS-COUNTER TO CCVS-E-4-1.                            NC2364.2
025500      MOVE ERROR-HOLD TO CCVS-E-4-2.                              NC2364.2
025600      MOVE CCVS-E-4 TO CCVS-E-2-2.                                NC2364.2
025700      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           NC2364.2
025800  END-ROUTINE-12.                                                 NC2364.2
025900      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        NC2364.2
026000     IF       ERROR-COUNTER IS EQUAL TO ZERO                      NC2364.2
026100         MOVE "NO " TO ERROR-TOTAL                                NC2364.2
026200         ELSE                                                     NC2364.2
026300         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       NC2364.2
026400     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           NC2364.2
026500     PERFORM WRITE-LINE.                                          NC2364.2
026600 END-ROUTINE-13.                                                  NC2364.2
026700     IF DELETE-COUNTER IS EQUAL TO ZERO                           NC2364.2
026800         MOVE "NO " TO ERROR-TOTAL  ELSE                          NC2364.2
026900         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      NC2364.2
027000     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   NC2364.2
027100     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2364.2
027200      IF   INSPECT-COUNTER EQUAL TO ZERO                          NC2364.2
027300          MOVE "NO " TO ERROR-TOTAL                               NC2364.2
027400      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   NC2364.2
027500      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            NC2364.2
027600      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          NC2364.2
027700     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2364.2
027800 WRITE-LINE.                                                      NC2364.2
027900     ADD 1 TO RECORD-COUNT.                                       NC2364.2
028000*    IF RECORD-COUNT GREATER 50                                   NC2364.2
028100*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          NC2364.2
028200*        MOVE SPACE TO DUMMY-RECORD                               NC2364.2
028300*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  NC2364.2
028400*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             NC2364.2
028500*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     NC2364.2
028600*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          NC2364.2
028700*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          NC2364.2
028800*        MOVE ZERO TO RECORD-COUNT.                               NC2364.2
028900     PERFORM WRT-LN.                                              NC2364.2
029000 WRT-LN.                                                          NC2364.2
029100     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               NC2364.2
029200     MOVE SPACE TO DUMMY-RECORD.                                  NC2364.2
029300 BLANK-LINE-PRINT.                                                NC2364.2
029400     PERFORM WRT-LN.                                              NC2364.2
029500 FAIL-ROUTINE.                                                    NC2364.2
029600     IF   COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE. NC2364.2
029700     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.NC2364.2
029800     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC2364.2
029900     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   NC2364.2
030000     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2364.2
030100     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC2364.2
030200     GO TO  FAIL-ROUTINE-EX.                                      NC2364.2
030300 FAIL-ROUTINE-WRITE.                                              NC2364.2
030400     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         NC2364.2
030500     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 NC2364.2
030600     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. NC2364.2
030700     MOVE   SPACES TO COR-ANSI-REFERENCE.                         NC2364.2
030800 FAIL-ROUTINE-EX. EXIT.                                           NC2364.2
030900 BAIL-OUT.                                                        NC2364.2
031000     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   NC2364.2
031100     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           NC2364.2
031200 BAIL-OUT-WRITE.                                                  NC2364.2
031300     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  NC2364.2
031400     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC2364.2
031500     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2364.2
031600     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC2364.2
031700 BAIL-OUT-EX. EXIT.                                               NC2364.2
031800 CCVS1-EXIT.                                                      NC2364.2
031900     EXIT.                                                        NC2364.2
032000 SECT-NC236A-001 SECTION.                                         NC2364.2
032100 TH-09-001.                                                       NC2364.2
032200 SCH-INIT-F1-1.                                                   NC2364.2
032300     MOVE "SCH-TEST-F1-1" TO PAR-NAME.                            NC2364.2
032400     MOVE "VI-121 6.21.6" TO ANSI-REFERENCE.                      NC2364.2
032500     MOVE "SEARCH " TO FEATURE.                                   NC2364.2
032600     SET A TO 01.                                                 NC2364.2
032700     SET W TO A.                                                  NC2364.2
032800 SCH-TEST-F1-1.                                                   NC2364.2
032900     SEARCH TBL-A VARYING W AT END GO TO SCH-FAIL-F1-1            NC2364.2
033000         WHEN ELMT-A (A) EQUAL TO  05                             NC2364.2
033100         SET A TO W.                                              NC2364.2
033200     IF ELMT-A (A) EQUAL TO  05                                   NC2364.2
033300         PERFORM PASS                                             NC2364.2
033400         GO TO SCH-WRITE-F1-1.                                    NC2364.2
033500 SCH-DELETE-F1-1.                                                 NC2364.2
033600     PERFORM DE-LETE.                                             NC2364.2
033700     GO TO SCH-WRITE-F1-1.                                        NC2364.2
033800 SCH-FAIL-F1-1.                                                   NC2364.2
033900     IF ELMT-A (05) EQUAL TO  05                                  NC2364.2
034000         MOVE 05 TO CORRECT-18V0                                  NC2364.2
034100         MOVE ELMT-A (05) TO COMPUTED-18V0                        NC2364.2
034200         MOVE "SEE NOTE 1 FOR DIAGNOSTIC" TO RE-MARK ELSE         NC2364.2
034300     MOVE "SEE NOTE 2 FOR DIAGNOSTIC" TO RE-MARK                  NC2364.2
034400     MOVE ELMT-A (05) TO COMPUTED-18V0                            NC2364.2
034500     MOVE 05 TO CORRECT-18V0.                                     NC2364.2
034600     PERFORM FAIL.                                                NC2364.2
034700 SCH-WRITE-F1-1.                                                  NC2364.2
034800     PERFORM PRINT-DETAIL.                                        NC2364.2
034900*                                                                 NC2364.2
035000 SCH-INIT-F1-2.                                                   NC2364.2
035100     MOVE "SCH-TEST-F1-2" TO PAR-NAME.                            NC2364.2
035200     MOVE "VI-121 6.21.6" TO ANSI-REFERENCE.                      NC2364.2
035300     MOVE "SEARCH " TO FEATURE.                                   NC2364.2
035400     SET A TO 09.                                                 NC2364.2
035500     SET W TO A.                                                  NC2364.2
035600 SCH-TEST-F1-2.                                                   NC2364.2
035700     SEARCH TBL-A VARYING W AT END GO TO SCH-FAIL-F1-2            NC2364.2
035800         WHEN ELMT-A (A) EQUAL TO  10                             NC2364.2
035900         SET A TO W.                                              NC2364.2
036000     IF ELMT-A (A) EQUAL TO  10                                   NC2364.2
036100         PERFORM PASS                                             NC2364.2
036200         GO TO SCH-WRITE-F1-2.                                    NC2364.2
036300 SCH-DELETE-F1-2.                                                 NC2364.2
036400     PERFORM DE-LETE.                                             NC2364.2
036500     GO TO SCH-WRITE-F1-2.                                        NC2364.2
036600 SCH-FAIL-F1-2.                                                   NC2364.2
036700     IF ELMT-A (10) EQUAL TO  10                                  NC2364.2
036800         MOVE 10 TO CORRECT-18V0                                  NC2364.2
036900         MOVE ELMT-A (10) TO COMPUTED-18V0                        NC2364.2
037000         MOVE "SEE NOTE 1 FOR DIAGNOSTIC" TO RE-MARK ELSE         NC2364.2
037100     MOVE "SEE NOTE 2 FOR DIAGNOSTIC" TO RE-MARK                  NC2364.2
037200     MOVE ELMT-A (10) TO COMPUTED-18V0                            NC2364.2
037300     MOVE 10 TO CORRECT-18V0.                                     NC2364.2
037400     PERFORM FAIL.                                                NC2364.2
037500 SCH-WRITE-F1-2.                                                  NC2364.2
037600     PERFORM PRINT-DETAIL.                                        NC2364.2
037700*                                                                 NC2364.2
037800 SCH-INIT-F1-3.                                                   NC2364.2
037900     MOVE "SCH-TEST-F1-3" TO PAR-NAME.                            NC2364.2
038000     MOVE "VI-121 6.21.6" TO ANSI-REFERENCE.                      NC2364.2
038100     MOVE "SEARCH " TO FEATURE.                                   NC2364.2
038200     SET A TO 02.                                                 NC2364.2
038300     SET W TO A.                                                  NC2364.2
038400 SCH-TEST-F1-3.                                                   NC2364.2
038500     SEARCH TBL-A VARYING W AT END GO TO SCH-FAIL-F1-3            NC2364.2
038600         WHEN ELMT-A (A) EQUAL TO  02                             NC2364.2
038700         SET A TO W.                                              NC2364.2
038800     IF ELMT-A (A) EQUAL TO  02                                   NC2364.2
038900         PERFORM PASS                                             NC2364.2
039000         GO TO SCH-WRITE-F1-3.                                    NC2364.2
039100 SCH-DELETE-F1-3.                                                 NC2364.2
039200     PERFORM DE-LETE.                                             NC2364.2
039300     GO TO SCH-WRITE-F1-3.                                        NC2364.2
039400 SCH-FAIL-F1-3.                                                   NC2364.2
039500     IF ELMT-A (02) EQUAL TO  02                                  NC2364.2
039600         MOVE 02 TO CORRECT-18V0                                  NC2364.2
039700         MOVE ELMT-A (02) TO COMPUTED-18V0                        NC2364.2
039800         MOVE "SEE NOTE 1 FOR DIAGNOSTIC" TO RE-MARK ELSE         NC2364.2
039900     MOVE "SEE NOTE 2 FOR DIAGNOSTIC" TO RE-MARK                  NC2364.2
040000     MOVE ELMT-A (02) TO COMPUTED-18V0                            NC2364.2
040100     MOVE 02 TO CORRECT-18V0.                                     NC2364.2
040200     PERFORM FAIL.                                                NC2364.2
040300 SCH-WRITE-F1-3.                                                  NC2364.2
040400     PERFORM PRINT-DETAIL.                                        NC2364.2
040500*                                                                 NC2364.2
040600 SCH-INIT-F1-4.                                                   NC2364.2
040700     MOVE "SCH-TEST-F1-4" TO PAR-NAME.                            NC2364.2
040800     MOVE "VI-121 6.21.6" TO ANSI-REFERENCE.                      NC2364.2
040900     MOVE "SEARCH " TO FEATURE.                                   NC2364.2
041000     SET A TO 07.                                                 NC2364.2
041100     SET W TO A.                                                  NC2364.2
041200 SCH-TEST-F1-4.                                                   NC2364.2
041300     SEARCH TBL-A VARYING W AT END GO TO SCH-FAIL-F1-4            NC2364.2
041400         WHEN ELMT-A (A) EQUAL TO  07                             NC2364.2
041500         SET A TO W.                                              NC2364.2
041600     IF ELMT-A (A) EQUAL TO  07                                   NC2364.2
041700         PERFORM PASS                                             NC2364.2
041800         GO TO SCH-WRITE-F1-4.                                    NC2364.2
041900 SCH-DELETE-F1-4.                                                 NC2364.2
042000     PERFORM DE-LETE.                                             NC2364.2
042100     GO TO SCH-WRITE-F1-4.                                        NC2364.2
042200 SCH-FAIL-F1-4.                                                   NC2364.2
042300     IF ELMT-A (07) EQUAL TO  07                                  NC2364.2
042400         MOVE 07 TO CORRECT-18V0                                  NC2364.2
042500         MOVE ELMT-A (07) TO COMPUTED-18V0                        NC2364.2
042600         MOVE "SEE NOTE 1 FOR DIAGNOSTIC" TO RE-MARK ELSE         NC2364.2
042700     MOVE "SEE NOTE 2 FOR DIAGNOSTIC" TO RE-MARK                  NC2364.2
042800     MOVE ELMT-A (07) TO COMPUTED-18V0                            NC2364.2
042900     MOVE 07 TO CORRECT-18V0.                                     NC2364.2
043000     PERFORM FAIL.                                                NC2364.2
043100 SCH-WRITE-F1-4.                                                  NC2364.2
043200     PERFORM PRINT-DETAIL.                                        NC2364.2
043300*                                                                 NC2364.2
043400 SCH-INIT-F1-5.                                                   NC2364.2
043500     MOVE "SCH-TEST-F1-5" TO PAR-NAME.                            NC2364.2
043600     MOVE "VI-121 6.21.6" TO ANSI-REFERENCE.                      NC2364.2
043700     MOVE "SEARCH " TO FEATURE.                                   NC2364.2
043800     SET A TO 03.                                                 NC2364.2
043900     SET W TO A.                                                  NC2364.2
044000 SCH-TEST-F1-5.                                                   NC2364.2
044100     SEARCH TBL-A VARYING W AT END GO TO SCH-FAIL-F1-5            NC2364.2
044200         WHEN ELMT-A (A) EQUAL TO 08                              NC2364.2
044300         SET A TO W.                                              NC2364.2
044400     IF ELMT-A (A) EQUAL TO 08                                    NC2364.2
044500         PERFORM PASS                                             NC2364.2
044600         GO TO SCH-WRITE-F1-5.                                    NC2364.2
044700 SCH-DELETE-F1-5.                                                 NC2364.2
044800     PERFORM DE-LETE.                                             NC2364.2
044900     GO TO SCH-WRITE-F1-5.                                        NC2364.2
045000 SCH-FAIL-F1-5.                                                   NC2364.2
045100     IF ELMT-A (08) EQUAL TO 08                                   NC2364.2
045200         MOVE 08 TO CORRECT-18V0                                  NC2364.2
045300         MOVE ELMT-A (08) TO COMPUTED-18V0                        NC2364.2
045400         MOVE "SEE NOTE 1 FOR DIAGNOSTIC" TO RE-MARK ELSE         NC2364.2
045500     MOVE "SEE NOTE 2 FOR DIAGNOSTIC" TO RE-MARK                  NC2364.2
045600     MOVE ELMT-A (08) TO COMPUTED-18V0                            NC2364.2
045700     MOVE 08 TO CORRECT-18V0.                                     NC2364.2
045800     PERFORM FAIL.                                                NC2364.2
045900 SCH-WRITE-F1-5.                                                  NC2364.2
046000     PERFORM PRINT-DETAIL.                                        NC2364.2
046100*                                                                 NC2364.2
046200 SCH-INIT-F1-6.                                                   NC2364.2
046300     MOVE "SCH-TEST-F1-6" TO PAR-NAME.                            NC2364.2
046400     MOVE "VI-121 6.21.6" TO ANSI-REFERENCE.                      NC2364.2
046500     MOVE "SEARCH " TO FEATURE.                                   NC2364.2
046600     SET A B TO 01.                                               NC2364.2
046700 SCH-TEST-F1-6.                                                   NC2364.2
046800     SEARCH TBL-B VARYING A AT END GO TO SCH-FAIL-F1-6            NC2364.2
046900         WHEN ELMT-B (B) EQUAL TO ELMT-A (8)                      NC2364.2
047000             PERFORM PASS                                         NC2364.2
047100             GO TO SCH-WRITE-F1-6.                                NC2364.2
047200 SCH-DELETE-F1-6.                                                 NC2364.2
047300     PERFORM DE-LETE.                                             NC2364.2
047400     GO TO SCH-WRITE-F1-6.                                        NC2364.2
047500 SCH-FAIL-F1-6.                                                   NC2364.2
047600     IF ELMT-B (8) EQUAL TO ELMT-A (8)                            NC2364.2
047700         MOVE 08 TO CORRECT-18V0 COMPUTED-18V0                    NC2364.2
047800         MOVE "SEE NOTE 1 FOR DIAGNOSTIC" TO RE-MARK ELSE         NC2364.2
047900     MOVE "SEE NOTE 2 FOR DIAGNOSTIC" TO RE-MARK                  NC2364.2
048000     MOVE ELMT-B (8) TO COMPUTED-18V0                             NC2364.2
048100     MOVE ELMT-A (8) TO CORRECT-18V0.                             NC2364.2
048200     PERFORM FAIL.                                                NC2364.2
048300 SCH-WRITE-F1-6.                                                  NC2364.2
048400     PERFORM PRINT-DETAIL.                                        NC2364.2
048500*                                                                 NC2364.2
048600 SCH-INIT-F1-7.                                                   NC2364.2
048700     MOVE "SCH-TEST-F1-7" TO PAR-NAME.                            NC2364.2
048800     MOVE "VI-121 6.21.6" TO ANSI-REFERENCE.                      NC2364.2
048900     MOVE "SEARCH " TO FEATURE.                                   NC2364.2
049000     SET A B TO 05.                                               NC2364.2
049100 SCH-TEST-F1-7.                                                   NC2364.2
049200     SEARCH TBL-B VARYING A AT END GO TO SCH-FAIL-F1-7            NC2364.2
049300         WHEN ELMT-B (B) EQUAL TO ELMT-A (10)                     NC2364.2
049400             PERFORM PASS                                         NC2364.2
049500             GO TO SCH-WRITE-F1-7.                                NC2364.2
049600 SCH-DELETE-F1-7.                                                 NC2364.2
049700     PERFORM DE-LETE.                                             NC2364.2
049800     GO TO SCH-WRITE-F1-6.                                        NC2364.2
049900 SCH-FAIL-F1-7.                                                   NC2364.2
050000     IF ELMT-B (10) EQUAL TO ELMT-A (10)                          NC2364.2
050100         MOVE 10 TO CORRECT-18V0 COMPUTED-18V0                    NC2364.2
050200         MOVE "SEE NOTE 1 FOR DIAGNOSTIC" TO RE-MARK ELSE         NC2364.2
050300     MOVE "SEE NOTE 2 FOR DIAGNOSTIC" TO RE-MARK                  NC2364.2
050400     MOVE ELMT-B (10) TO COMPUTED-18V0                            NC2364.2
050500     MOVE ELMT-A (10) TO CORRECT-18V0.                            NC2364.2
050600     PERFORM FAIL.                                                NC2364.2
050700 SCH-WRITE-F1-7.                                                  NC2364.2
050800     PERFORM PRINT-DETAIL.                                        NC2364.2
050900*                                                                 NC2364.2
051000 SCH-INIT-F1-8.                                                   NC2364.2
051100     MOVE "SCH-TEST-F1-8" TO PAR-NAME.                            NC2364.2
051200     MOVE "VI-121 6.21.6" TO ANSI-REFERENCE.                      NC2364.2
051300     MOVE "SEARCH " TO FEATURE.                                   NC2364.2
051400     SET A B TO 09.                                               NC2364.2
051500 SCH-TEST-F1-8.                                                   NC2364.2
051600     SEARCH TBL-B VARYING A AT END GO TO SCH-FAIL-F1-8            NC2364.2
051700         WHEN ELMT-B (09) EQUAL TO ELMT-A (A)                     NC2364.2
051800             PERFORM PASS                                         NC2364.2
051900             GO TO SCH-WRITE-F1-8.                                NC2364.2
052000 SCH-DELETE-F1-8.                                                 NC2364.2
052100     PERFORM DE-LETE.                                             NC2364.2
052200     GO TO SCH-WRITE-F1-8.                                        NC2364.2
052300 SCH-FAIL-F1-8.                                                   NC2364.2
052400     IF ELMT-B (09) EQUAL TO ELMT-A (09)                          NC2364.2
052500         MOVE 09 TO CORRECT-18V0 COMPUTED-18V0                    NC2364.2
052600         MOVE "SEE NOTE 1 FOR DIAGNOSTIC" TO RE-MARK ELSE         NC2364.2
052700     MOVE "SEE NOTE 2 FOR DIAGNOSTIC" TO RE-MARK                  NC2364.2
052800     MOVE ELMT-B (09) TO COMPUTED-18V0                            NC2364.2
052900     MOVE ELMT-A (09) TO CORRECT-18V0.                            NC2364.2
053000     PERFORM FAIL.                                                NC2364.2
053100 SCH-WRITE-F1-8.                                                  NC2364.2
053200     PERFORM PRINT-DETAIL.                                        NC2364.2
053300*                                                                 NC2364.2
053400 SCH-INIT-F1-9.                                                   NC2364.2
053500     MOVE "SCH-TEST-F1-9" TO PAR-NAME.                            NC2364.2
053600     MOVE "VI-121 6.21.6" TO ANSI-REFERENCE.                      NC2364.2
053700     MOVE "SEARCH " TO FEATURE.                                   NC2364.2
053800     SET A B TO 3.                                                NC2364.2
053900 SCH-TEST-F1-9.                                                   NC2364.2
054000     SEARCH TBL-B VARYING A AT END GO TO SCH-FAIL-F1-9            NC2364.2
054100         WHEN ELMT-B (B) EQUAL TO ELMT-A (A)                      NC2364.2
054200             PERFORM PASS                                         NC2364.2
054300             GO TO SCH-WRITE-F1-9.                                NC2364.2
054400 SCH-DELETE-F1-9.                                                 NC2364.2
054500     PERFORM DE-LETE.                                             NC2364.2
054600     GO TO SCH-WRITE-F1-9.                                        NC2364.2
054700 SCH-FAIL-F1-9.                                                   NC2364.2
054800     IF ELMT-B (3) EQUAL TO ELMT-A (3)                            NC2364.2
054900         MOVE 03 TO CORRECT-18V0 COMPUTED-18V0                    NC2364.2
055000         MOVE "SEE NOTE 1 FOR DIAGNOSTIC" TO RE-MARK ELSE         NC2364.2
055100     MOVE "SEE NOTE 2 FOR DIAGNOSTIC" TO RE-MARK                  NC2364.2
055200     MOVE ELMT-B (3) TO COMPUTED-18V0                             NC2364.2
055300     MOVE ELMT-A (3) TO CORRECT-18V0.                             NC2364.2
055400     PERFORM FAIL.                                                NC2364.2
055500 SCH-WRITE-F1-9.                                                  NC2364.2
055600     PERFORM PRINT-DETAIL.                                        NC2364.2
055700*                                                                 NC2364.2
055800 SCH-INIT-F1-10.                                                  NC2364.2
055900     MOVE "SCH-TEST-F1-10" TO PAR-NAME.                           NC2364.2
056000     MOVE "VI-121 6.21.6" TO ANSI-REFERENCE.                      NC2364.2
056100     MOVE "SEARCH " TO FEATURE.                                   NC2364.2
056200     SET A B TO 06.                                               NC2364.2
056300 SCH-TEST-F1-10.                                                  NC2364.2
056400     SEARCH TBL-B VARYING A AT END GO TO SCH-FAIL-F1-10           NC2364.2
056500         WHEN ELMT-B (9) EQUAL TO ELMT-A (9)                      NC2364.2
056600             PERFORM PASS                                         NC2364.2
056700             GO TO SCH-WRITE-F1-10.                               NC2364.2
056800 SCH-DELETE-F1-10.                                                NC2364.2
056900     PERFORM DE-LETE.                                             NC2364.2
057000     GO TO SCH-WRITE-F1-10.                                       NC2364.2
057100 SCH-FAIL-F1-10.                                                  NC2364.2
057200     IF ELMT-B (9) EQUAL TO ELMT-A (9)                            NC2364.2
057300         MOVE 09 TO CORRECT-18V0 COMPUTED-18V0                    NC2364.2
057400         MOVE "SEE NOTE 1 FOR DIAGNOSTIC" TO RE-MARK ELSE         NC2364.2
057500     MOVE "SEE NOTE 2 FOR DIAGNOSTIC" TO RE-MARK                  NC2364.2
057600     MOVE ELMT-B (9) TO COMPUTED-18V0                             NC2364.2
057700     MOVE ELMT-A (9) TO CORRECT-18V0.                             NC2364.2
057800     PERFORM FAIL.                                                NC2364.2
057900 SCH-WRITE-F1-10.                                                 NC2364.2
058000     PERFORM PRINT-DETAIL.                                        NC2364.2
058100*                                                                 NC2364.2
058200 CCVS-EXIT SECTION.                                               NC2364.2
058300 CCVS-999999.                                                     NC2364.2
058400     GO TO CLOSE-FILES.                                           NC2364.2
      *END-OF,NC236A                                                                  
