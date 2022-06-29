      *HEADER,COBOL,NC239A                                                            
000100 IDENTIFICATION DIVISION.                                         NC2394.2
000200 PROGRAM-ID.                                                      NC2394.2
000300     NC239A.                                                      NC2394.2
000400****************************************************************  NC2394.2
000500*                                                              *  NC2394.2
000600*    VALIDATION FOR:-                                          *  NC2394.2
000700*                                                              *  NC2394.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2394.2
000900*                                                              *  NC2394.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC2394.2
001100*                                                              *  NC2394.2
001200****************************************************************  NC2394.2
001300*                                                              *  NC2394.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  NC2394.2
001500*                                                              *  NC2394.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  NC2394.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  NC2394.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  NC2394.2
001900*                                                              *  NC2394.2
002000****************************************************************  NC2394.2
002100*                                                              *  NC2394.2
002200*    PROGRAM NC239A TESTS THE CONSTRUCTION AND ACCESS OF A     *  NC2394.2
002300*    THREE-DIMENSIONAL TABLE USING INDICES.                    *  NC2394.2
002400*    VALUES ARE VERIFIED USING THE "IF" STATEMENT.             *  NC2394.2
002500*                                                ~             *  NC2394.2
002600****************************************************************  NC2394.2
002700 ENVIRONMENT DIVISION.                                            NC2394.2
002800 CONFIGURATION SECTION.                                           NC2394.2
002900 SOURCE-COMPUTER.                                                 NC2394.2
003000     XXXXX082.                                                    NC2394.2
003100 OBJECT-COMPUTER.                                                 NC2394.2
003200     XXXXX083.                                                    NC2394.2
003300 INPUT-OUTPUT SECTION.                                            NC2394.2
003400 FILE-CONTROL.                                                    NC2394.2
003500     SELECT PRINT-FILE ASSIGN TO                                  NC2394.2
003600     XXXXX055.                                                    NC2394.2
003700 DATA DIVISION.                                                   NC2394.2
003800 FILE SECTION.                                                    NC2394.2
003900 FD  PRINT-FILE.                                                  NC2394.2
004000 01  PRINT-REC PICTURE X(120).                                    NC2394.2
004100 01  DUMMY-RECORD PICTURE X(120).                                 NC2394.2
004200 WORKING-STORAGE SECTION.                                         NC2394.2
004300 77  SUB-1              PICTURE S99  VALUE ZERO.                  NC2394.2
004400 77  SUB-2              PICTURE 99   VALUE ZERO.                  NC2394.2
004500 77  SUB-3              PICTURE 99   VALUE ZERO.                  NC2394.2
004600 77  CON-7              PICTURE 99   VALUE 07.                    NC2394.2
004700 77  CON-10             PICTURE 99   VALUE 10.                    NC2394.2
004800 77  CON-5              PICTURE 99   VALUE 05.                    NC2394.2
004900 77  CON-6              PICTURE 99   VALUE 06.                    NC2394.2
005000 01  GRP-NAME.                                                    NC2394.2
005100     02  FILLER              PICTURE XXX    VALUE "GRP".          NC2394.2
005200     02  ADD-GRP             PICTURE 99     VALUE 01.             NC2394.2
005300                                                                  NC2394.2
005400 01  SEC-NAME.                                                    NC2394.2
005500     02  FILLER              PICTURE X(5)   VALUE "SEC (".        NC2394.2
005600     02  SEC-GRP             PICTURE 99     VALUE 00.             NC2394.2
005700     02  FILLER              PICTURE X      VALUE ",".            NC2394.2
005800     02  ADD-SEC             PICTURE 99     VALUE 01.             NC2394.2
005900     02  FILLER              PICTURE X      VALUE ")".            NC2394.2
006000                                                                  NC2394.2
006100 01  ELEM-NAME.                                                   NC2394.2
006200     02  FILLER              PICTURE X(6)   VALUE "ELEM (".       NC2394.2
006300     02  ELEM-GRP            PICTURE 99     VALUE 00.             NC2394.2
006400     02  FILLER              PICTURE X      VALUE ",".            NC2394.2
006500     02  ELEM-SEC            PICTURE 99     VALUE 00.             NC2394.2
006600     02  FILLER              PICTURE X      VALUE ",".            NC2394.2
006700     02  ADD-ELEM            PICTURE 99     VALUE 01.             NC2394.2
006800     02  FILLER              PICTURE X      VALUE ")".            NC2394.2
006900                                                                  NC2394.2
007000 01  3-DIMENSION-TBL.                                             NC2394.2
007100     02  GRP-ENTRY OCCURS 10 TIMES INDEXED BY IDX-1.              NC2394.2
007200         03  ENTRY-1         PICTURE X(5).                        NC2394.2
007300         03  GRP2-ENTRY OCCURS 10 TIMES INDEXED BY IDX-2.         NC2394.2
007400             04  ENTRY-2     PICTURE X(11).                       NC2394.2
007500             04  GRP3-ENTRY OCCURS 10 TIMES INDEXED BY IDX-3.     NC2394.2
007600                 05  ENTRY-3 PICTURE X(15).                       NC2394.2
007700                                                                  NC2394.2
007800 01  TEST-RESULTS.                                                NC2394.2
007900     02 FILLER                   PIC X      VALUE SPACE.          NC2394.2
008000     02 FEATURE                  PIC X(20)  VALUE SPACE.          NC2394.2
008100     02 FILLER                   PIC X      VALUE SPACE.          NC2394.2
008200     02 P-OR-F                   PIC X(5)   VALUE SPACE.          NC2394.2
008300     02 FILLER                   PIC X      VALUE SPACE.          NC2394.2
008400     02  PAR-NAME.                                                NC2394.2
008500       03 FILLER                 PIC X(19)  VALUE SPACE.          NC2394.2
008600       03  PARDOT-X              PIC X      VALUE SPACE.          NC2394.2
008700       03 DOTVALUE               PIC 99     VALUE ZERO.           NC2394.2
008800     02 FILLER                   PIC X(8)   VALUE SPACE.          NC2394.2
008900     02 RE-MARK                  PIC X(61).                       NC2394.2
009000 01  TEST-COMPUTED.                                               NC2394.2
009100     02 FILLER                   PIC X(30)  VALUE SPACE.          NC2394.2
009200     02 FILLER                   PIC X(17)  VALUE                 NC2394.2
009300            "       COMPUTED=".                                   NC2394.2
009400     02 COMPUTED-X.                                               NC2394.2
009500     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          NC2394.2
009600     03 COMPUTED-N               REDEFINES COMPUTED-A             NC2394.2
009700                                 PIC -9(9).9(9).                  NC2394.2
009800     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         NC2394.2
009900     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     NC2394.2
010000     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     NC2394.2
010100     03       CM-18V0 REDEFINES COMPUTED-A.                       NC2394.2
010200         04 COMPUTED-18V0                    PIC -9(18).          NC2394.2
010300         04 FILLER                           PIC X.               NC2394.2
010400     03 FILLER PIC X(50) VALUE SPACE.                             NC2394.2
010500 01  TEST-CORRECT.                                                NC2394.2
010600     02 FILLER PIC X(30) VALUE SPACE.                             NC2394.2
010700     02 FILLER PIC X(17) VALUE "       CORRECT =".                NC2394.2
010800     02 CORRECT-X.                                                NC2394.2
010900     03 CORRECT-A                  PIC X(20) VALUE SPACE.         NC2394.2
011000     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      NC2394.2
011100     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         NC2394.2
011200     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     NC2394.2
011300     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     NC2394.2
011400     03      CR-18V0 REDEFINES CORRECT-A.                         NC2394.2
011500         04 CORRECT-18V0                     PIC -9(18).          NC2394.2
011600         04 FILLER                           PIC X.               NC2394.2
011700     03 FILLER PIC X(2) VALUE SPACE.                              NC2394.2
011800     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     NC2394.2
011900 01  CCVS-C-1.                                                    NC2394.2
012000     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PANC2394.2
012100-    "SS  PARAGRAPH-NAME                                          NC2394.2
012200-    "       REMARKS".                                            NC2394.2
012300     02 FILLER                     PIC X(20)    VALUE SPACE.      NC2394.2
012400 01  CCVS-C-2.                                                    NC2394.2
012500     02 FILLER                     PIC X        VALUE SPACE.      NC2394.2
012600     02 FILLER                     PIC X(6)     VALUE "TESTED".   NC2394.2
012700     02 FILLER                     PIC X(15)    VALUE SPACE.      NC2394.2
012800     02 FILLER                     PIC X(4)     VALUE "FAIL".     NC2394.2
012900     02 FILLER                     PIC X(94)    VALUE SPACE.      NC2394.2
013000 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       NC2394.2
013100 01  REC-CT                        PIC 99       VALUE ZERO.       NC2394.2
013200 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       NC2394.2
013300 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       NC2394.2
013400 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       NC2394.2
013500 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       NC2394.2
013600 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       NC2394.2
013700 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       NC2394.2
013800 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      NC2394.2
013900 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       NC2394.2
014000 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     NC2394.2
014100 01  CCVS-H-1.                                                    NC2394.2
014200     02  FILLER                    PIC X(39)    VALUE SPACES.     NC2394.2
014300     02  FILLER                    PIC X(42)    VALUE             NC2394.2
014400     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 NC2394.2
014500     02  FILLER                    PIC X(39)    VALUE SPACES.     NC2394.2
014600 01  CCVS-H-2A.                                                   NC2394.2
014700   02  FILLER                        PIC X(40)  VALUE SPACE.      NC2394.2
014800   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  NC2394.2
014900   02  FILLER                        PIC XXXX   VALUE             NC2394.2
015000     "4.2 ".                                                      NC2394.2
015100   02  FILLER                        PIC X(28)  VALUE             NC2394.2
015200            " COPY - NOT FOR DISTRIBUTION".                       NC2394.2
015300   02  FILLER                        PIC X(41)  VALUE SPACE.      NC2394.2
015400                                                                  NC2394.2
015500 01  CCVS-H-2B.                                                   NC2394.2
015600   02  FILLER                        PIC X(15)  VALUE             NC2394.2
015700            "TEST RESULT OF ".                                    NC2394.2
015800   02  TEST-ID                       PIC X(9).                    NC2394.2
015900   02  FILLER                        PIC X(4)   VALUE             NC2394.2
016000            " IN ".                                               NC2394.2
016100   02  FILLER                        PIC X(12)  VALUE             NC2394.2
016200     " HIGH       ".                                              NC2394.2
016300   02  FILLER                        PIC X(22)  VALUE             NC2394.2
016400            " LEVEL VALIDATION FOR ".                             NC2394.2
016500   02  FILLER                        PIC X(58)  VALUE             NC2394.2
016600     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2394.2
016700 01  CCVS-H-3.                                                    NC2394.2
016800     02  FILLER                      PIC X(34)  VALUE             NC2394.2
016900            " FOR OFFICIAL USE ONLY    ".                         NC2394.2
017000     02  FILLER                      PIC X(58)  VALUE             NC2394.2
017100     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC2394.2
017200     02  FILLER                      PIC X(28)  VALUE             NC2394.2
017300            "  COPYRIGHT   1985 ".                                NC2394.2
017400 01  CCVS-E-1.                                                    NC2394.2
017500     02 FILLER                       PIC X(52)  VALUE SPACE.      NC2394.2
017600     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              NC2394.2
017700     02 ID-AGAIN                     PIC X(9).                    NC2394.2
017800     02 FILLER                       PIC X(45)  VALUE SPACES.     NC2394.2
017900 01  CCVS-E-2.                                                    NC2394.2
018000     02  FILLER                      PIC X(31)  VALUE SPACE.      NC2394.2
018100     02  FILLER                      PIC X(21)  VALUE SPACE.      NC2394.2
018200     02 CCVS-E-2-2.                                               NC2394.2
018300         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      NC2394.2
018400         03 FILLER                   PIC X      VALUE SPACE.      NC2394.2
018500         03 ENDER-DESC               PIC X(44)  VALUE             NC2394.2
018600            "ERRORS ENCOUNTERED".                                 NC2394.2
018700 01  CCVS-E-3.                                                    NC2394.2
018800     02  FILLER                      PIC X(22)  VALUE             NC2394.2
018900            " FOR OFFICIAL USE ONLY".                             NC2394.2
019000     02  FILLER                      PIC X(12)  VALUE SPACE.      NC2394.2
019100     02  FILLER                      PIC X(58)  VALUE             NC2394.2
019200     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2394.2
019300     02  FILLER                      PIC X(13)  VALUE SPACE.      NC2394.2
019400     02 FILLER                       PIC X(15)  VALUE             NC2394.2
019500             " COPYRIGHT 1985".                                   NC2394.2
019600 01  CCVS-E-4.                                                    NC2394.2
019700     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      NC2394.2
019800     02 FILLER                       PIC X(4)   VALUE " OF ".     NC2394.2
019900     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      NC2394.2
020000     02 FILLER                       PIC X(40)  VALUE             NC2394.2
020100      "  TESTS WERE EXECUTED SUCCESSFULLY".                       NC2394.2
020200 01  XXINFO.                                                      NC2394.2
020300     02 FILLER                       PIC X(19)  VALUE             NC2394.2
020400            "*** INFORMATION ***".                                NC2394.2
020500     02 INFO-TEXT.                                                NC2394.2
020600       04 FILLER                     PIC X(8)   VALUE SPACE.      NC2394.2
020700       04 XXCOMPUTED                 PIC X(20).                   NC2394.2
020800       04 FILLER                     PIC X(5)   VALUE SPACE.      NC2394.2
020900       04 XXCORRECT                  PIC X(20).                   NC2394.2
021000     02 INF-ANSI-REFERENCE           PIC X(48).                   NC2394.2
021100 01  HYPHEN-LINE.                                                 NC2394.2
021200     02 FILLER  PIC IS X VALUE IS SPACE.                          NC2394.2
021300     02 FILLER  PIC IS X(65)    VALUE IS "************************NC2394.2
021400-    "*****************************************".                 NC2394.2
021500     02 FILLER  PIC IS X(54)    VALUE IS "************************NC2394.2
021600-    "******************************".                            NC2394.2
021700 01  CCVS-PGM-ID                     PIC X(9)   VALUE             NC2394.2
021800     "NC239A".                                                    NC2394.2
021900 PROCEDURE DIVISION.                                              NC2394.2
022000 CCVS1 SECTION.                                                   NC2394.2
022100 OPEN-FILES.                                                      NC2394.2
022200     OPEN     OUTPUT PRINT-FILE.                                  NC2394.2
022300     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   NC2394.2
022400     MOVE    SPACE TO TEST-RESULTS.                               NC2394.2
022500     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             NC2394.2
022600     GO TO CCVS1-EXIT.                                            NC2394.2
022700 CLOSE-FILES.                                                     NC2394.2
022800     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   NC2394.2
022900 TERMINATE-CCVS.                                                  NC2394.2
023000*    EXIT PROGRAM.                                                NC2394.2
023100*TERMINATE-CALL.                                                  NC2394.2
023200     STOP     RUN.                                                NC2394.2
023300 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         NC2394.2
023400 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           NC2394.2
023500 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          NC2394.2
023600 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      NC2394.2
023700     MOVE "****TEST DELETED****" TO RE-MARK.                      NC2394.2
023800 PRINT-DETAIL.                                                    NC2394.2
023900     IF REC-CT NOT EQUAL TO ZERO                                  NC2394.2
024000             MOVE "." TO PARDOT-X                                 NC2394.2
024100             MOVE REC-CT TO DOTVALUE.                             NC2394.2
024200     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      NC2394.2
024300     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               NC2394.2
024400        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 NC2394.2
024500          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 NC2394.2
024600     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              NC2394.2
024700     MOVE SPACE TO CORRECT-X.                                     NC2394.2
024800     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         NC2394.2
024900     MOVE     SPACE TO RE-MARK.                                   NC2394.2
025000 HEAD-ROUTINE.                                                    NC2394.2
025100     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC2394.2
025200     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC2394.2
025300     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC2394.2
025400     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC2394.2
025500 COLUMN-NAMES-ROUTINE.                                            NC2394.2
025600     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2394.2
025700     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2394.2
025800     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        NC2394.2
025900 END-ROUTINE.                                                     NC2394.2
026000     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.NC2394.2
026100 END-RTN-EXIT.                                                    NC2394.2
026200     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2394.2
026300 END-ROUTINE-1.                                                   NC2394.2
026400      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      NC2394.2
026500      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               NC2394.2
026600      ADD PASS-COUNTER TO ERROR-HOLD.                             NC2394.2
026700*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   NC2394.2
026800      MOVE PASS-COUNTER TO CCVS-E-4-1.                            NC2394.2
026900      MOVE ERROR-HOLD TO CCVS-E-4-2.                              NC2394.2
027000      MOVE CCVS-E-4 TO CCVS-E-2-2.                                NC2394.2
027100      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           NC2394.2
027200  END-ROUTINE-12.                                                 NC2394.2
027300      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        NC2394.2
027400     IF       ERROR-COUNTER IS EQUAL TO ZERO                      NC2394.2
027500         MOVE "NO " TO ERROR-TOTAL                                NC2394.2
027600         ELSE                                                     NC2394.2
027700         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       NC2394.2
027800     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           NC2394.2
027900     PERFORM WRITE-LINE.                                          NC2394.2
028000 END-ROUTINE-13.                                                  NC2394.2
028100     IF DELETE-COUNTER IS EQUAL TO ZERO                           NC2394.2
028200         MOVE "NO " TO ERROR-TOTAL  ELSE                          NC2394.2
028300         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      NC2394.2
028400     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   NC2394.2
028500     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2394.2
028600      IF   INSPECT-COUNTER EQUAL TO ZERO                          NC2394.2
028700          MOVE "NO " TO ERROR-TOTAL                               NC2394.2
028800      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   NC2394.2
028900      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            NC2394.2
029000      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          NC2394.2
029100     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2394.2
029200 WRITE-LINE.                                                      NC2394.2
029300     ADD 1 TO RECORD-COUNT.                                       NC2394.2
029400*    IF RECORD-COUNT GREATER 50                                   NC2394.2
029500*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          NC2394.2
029600*        MOVE SPACE TO DUMMY-RECORD                               NC2394.2
029700*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  NC2394.2
029800*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             NC2394.2
029900*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     NC2394.2
030000*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          NC2394.2
030100*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          NC2394.2
030200*        MOVE ZERO TO RECORD-COUNT.                               NC2394.2
030300     PERFORM WRT-LN.                                              NC2394.2
030400 WRT-LN.                                                          NC2394.2
030500     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               NC2394.2
030600     MOVE SPACE TO DUMMY-RECORD.                                  NC2394.2
030700 BLANK-LINE-PRINT.                                                NC2394.2
030800     PERFORM WRT-LN.                                              NC2394.2
030900 FAIL-ROUTINE.                                                    NC2394.2
031000     IF   COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE. NC2394.2
031100     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.NC2394.2
031200     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC2394.2
031300     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   NC2394.2
031400     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2394.2
031500     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC2394.2
031600     GO TO  FAIL-ROUTINE-EX.                                      NC2394.2
031700 FAIL-ROUTINE-WRITE.                                              NC2394.2
031800     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         NC2394.2
031900     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 NC2394.2
032000     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. NC2394.2
032100     MOVE   SPACES TO COR-ANSI-REFERENCE.                         NC2394.2
032200 FAIL-ROUTINE-EX. EXIT.                                           NC2394.2
032300 BAIL-OUT.                                                        NC2394.2
032400     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   NC2394.2
032500     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           NC2394.2
032600 BAIL-OUT-WRITE.                                                  NC2394.2
032700     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  NC2394.2
032800     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC2394.2
032900     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2394.2
033000     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC2394.2
033100 BAIL-OUT-EX. EXIT.                                               NC2394.2
033200 CCVS1-EXIT.                                                      NC2394.2
033300     EXIT.                                                        NC2394.2
033400 SECT-NC239A-001 SECTION.                                         NC2394.2
033500 TH-12-001.                                                       NC2394.2
033600 TABLE-INIT.                                                      NC2394.2
033700     PERFORM TABLE-INIT-SUBROUTINE VARYING SUB-1 FROM 1 BY 1      NC2394.2
033800         UNTIL SUB-1 EQUAL TO 11                                  NC2394.2
033900         AFTER SUB-2 FROM 1 BY 1 UNTIL SUB-2 EQUAL TO 11          NC2394.2
034000         AFTER SUB-3 FROM 1 BY 1 UNTIL SUB-3 EQUAL TO 11.         NC2394.2
034100     GO TO TABLE-TEST.                                            NC2394.2
034200                                                                  NC2394.2
034300 TABLE-INIT-SUBROUTINE.                                           NC2394.2
034400     SET IDX-1 TO SUB-1.                                          NC2394.2
034500     SET IDX-2 TO SUB-2.                                          NC2394.2
034600     SET IDX-3 TO SUB-3.                                          NC2394.2
034700     SET ADD-GRP, SEC-GRP, ELEM-GRP TO IDX-1.                     NC2394.2
034800     MOVE GRP-NAME TO ENTRY-1 (IDX-1).                            NC2394.2
034900     SET ADD-SEC, ELEM-SEC TO IDX-2.                              NC2394.2
035000     MOVE SEC-NAME TO ENTRY-2 (IDX-1, IDX-2).                     NC2394.2
035100     SET ADD-ELEM TO IDX-3.                                       NC2394.2
035200     MOVE ELEM-NAME TO ENTRY-3 (IDX-1, IDX-2, IDX-3).             NC2394.2
035300                                                                  NC2394.2
035400 TABLE-TEST.                                                      NC2394.2
035500     MOVE "LEVEL 1 INT INDEXING" TO FEATURE.                      NC2394.2
035600     MOVE "TABLE-TEST" TO PAR-NAME.                               NC2394.2
035700     MOVE "IV-21 & II-15 4.4.2" TO ANSI-REFERENCE.                NC2394.2
035800     SET IDX-1 TO 5.                                              NC2394.2
035900     IF ENTRY-1 (IDX-1) IS NOT EQUAL TO "GRP05"                   NC2394.2
036000         GO TO TABLE-FAIL.                                        NC2394.2
036100     PERFORM PASS.                                                NC2394.2
036200     GO TO TABLE-WRITE.                                           NC2394.2
036300 TABLE-FAIL.                                                      NC2394.2
036400     MOVE "GRP05" TO CORRECT-A                                    NC2394.2
036500     MOVE ENTRY-1 (IDX-1) TO COMPUTED-A                           NC2394.2
036600     MOVE "INTERNAL INDEXING LEVEL 1  " TO RE-MARK                NC2394.2
036700     PERFORM FAIL.                                                NC2394.2
036800 TABLE-WRITE.                                                     NC2394.2
036900     PERFORM PRINT-DETAIL.                                        NC2394.2
037000                                                                  NC2394.2
037100 TH1-INIT-GF-1.                                                   NC2394.2
037200     MOVE "TH1-TEST-GF-1" TO PAR-NAME.                            NC2394.2
037300     MOVE "IV-21 & II-15 4.4.2" TO ANSI-REFERENCE.                NC2394.2
037400     MOVE "LEVEL 1 INT INDEXING" TO FEATURE.                      NC2394.2
037500     SET IDX-1 TO 8.                                              NC2394.2
037600 TH1-TEST-GF-1.                                                   NC2394.2
037700     IF ENTRY-1 (IDX-1) IS NOT EQUAL TO "GRP08"                   NC2394.2
037800          GO TO TH1-FAIL-GF-1.                                    NC2394.2
037900     PERFORM PASS.                                                NC2394.2
038000     GO TO TH1-WRITE-GF-1.                                        NC2394.2
038100 TH1-DELETE-GF-1.                                                 NC2394.2
038200     PERFORM DE-LETE.                                             NC2394.2
038300     GO TO TH1-WRITE-GF-1.                                        NC2394.2
038400 TH1-FAIL-GF-1.                                                   NC2394.2
038500     MOVE "GRP08" TO CORRECT-A                                    NC2394.2
038600     MOVE ENTRY-1 (IDX-1) TO COMPUTED-A                           NC2394.2
038700     MOVE "INTERNAL INDEXING LEVEL 1  " TO RE-MARK                NC2394.2
038800     PERFORM FAIL.                                                NC2394.2
038900 TH1-WRITE-GF-1.                                                  NC2394.2
039000     PERFORM PRINT-DETAIL.                                        NC2394.2
039100                                                                  NC2394.2
039200 TH2-INIT-GF-1.                                                   NC2394.2
039300     MOVE "LEVEL 2 INT INDEXING" TO FEATURE.                      NC2394.2
039400     MOVE "TH2-TEST-GF-1" TO PAR-NAME.                            NC2394.2
039500     MOVE "IV-21 & II-15 4.4.2" TO ANSI-REFERENCE.                NC2394.2
039600     SET IDX-1 TO 5.                                              NC2394.2
039700     SET IDX-2 TO 6.                                              NC2394.2
039800 TH2-TEST-GF-1.                                                   NC2394.2
039900     IF ENTRY-2 (IDX-1, IDX-2) IS NOT EQUAL TO "SEC (05,06)"      NC2394.2
040000         GO TO TH2-FAIL-GF-1.                                     NC2394.2
040100     PERFORM PASS.                                                NC2394.2
040200     GO TO TH2-WRITE-GF-1.                                        NC2394.2
040300 TH2-DELETE-GF-1.                                                 NC2394.2
040400     PERFORM DE-LETE.                                             NC2394.2
040500     GO TO TH2-WRITE-GF-1.                                        NC2394.2
040600 TH2-FAIL-GF-1.                                                   NC2394.2
040700     MOVE "SEC (05,06)" TO CORRECT-A                              NC2394.2
040800     MOVE ENTRY-2 (IDX-1, IDX-2) TO COMPUTED-A                    NC2394.2
040900     MOVE "INTERNAL INDEXING LEVEL 2  " TO RE-MARK                NC2394.2
041000     PERFORM FAIL.                                                NC2394.2
041100 TH2-WRITE-GF-1.                                                  NC2394.2
041200     PERFORM PRINT-DETAIL.                                        NC2394.2
041300                                                                  NC2394.2
041400 TH2-INIT-GF-2.                                                   NC2394.2
041500     MOVE "TH2-TEST-GF-2" TO PAR-NAME.                            NC2394.2
041600     MOVE "IV-21 & II-15 4.4.2" TO ANSI-REFERENCE.                NC2394.2
041700     MOVE "LEVEL 2 INT INDEXING" TO FEATURE.                      NC2394.2
041800     SET IDX-1, IDX-2 TO 8.                                       NC2394.2
041900 TH2-TEST-GF-2.                                                   NC2394.2
042000     IF ENTRY-2 (IDX-1, IDX-2) IS NOT EQUAL TO "SEC (08,08)"      NC2394.2
042100         GO TO TH2-FAIL-GF-2.                                     NC2394.2
042200     PERFORM PASS.                                                NC2394.2
042300     GO TO TH2-WRITE-GF-2.                                        NC2394.2
042400 TH2-DELETE-GF-2.                                                 NC2394.2
042500     PERFORM DE-LETE.                                             NC2394.2
042600     GO TO TH2-WRITE-GF-2.                                        NC2394.2
042700 TH2-FAIL-GF-2.                                                   NC2394.2
042800     MOVE "SEC (08,08)" TO CORRECT-A                              NC2394.2
042900     MOVE ENTRY-2 (IDX-1, IDX-2) TO COMPUTED-A                    NC2394.2
043000     MOVE "INTERNAL INDEXING LEVEL 2  " TO RE-MARK                NC2394.2
043100     PERFORM FAIL.                                                NC2394.2
043200 TH2-WRITE-GF-2.                                                  NC2394.2
043300     PERFORM PRINT-DETAIL.                                        NC2394.2
043400                                                                  NC2394.2
043500 TH2-INIT-GF-3.                                                   NC2394.2
043600     MOVE "TH2-TEST-GF-3" TO PAR-NAME.                            NC2394.2
043700     MOVE "IV-21 & II-15 4.4.2" TO ANSI-REFERENCE.                NC2394.2
043800     MOVE "LEVEL 2 INT INDEXING" TO FEATURE.                      NC2394.2
043900     SET IDX-1 TO 3.                                              NC2394.2
044000     SET IDX-2 TO 7.                                              NC2394.2
044100 TH2-TEST-GF-3.                                                   NC2394.2
044200     IF ENTRY-2 (IDX-1, IDX-2) IS NOT EQUAL TO "SEC (03,07)"      NC2394.2
044300         GO TO TH2-FAIL-GF-3.                                     NC2394.2
044400     PERFORM PASS.                                                NC2394.2
044500     GO TO TH2-WRITE-GF-3.                                        NC2394.2
044600 TH2-DELETE-GF-3.                                                 NC2394.2
044700     PERFORM DE-LETE.                                             NC2394.2
044800     GO TO TH2-WRITE-GF-3.                                        NC2394.2
044900 TH2-FAIL-GF-3.                                                   NC2394.2
045000     MOVE "SEC (03,07)" TO CORRECT-A                              NC2394.2
045100     MOVE ENTRY-2 (IDX-1, IDX-2) TO COMPUTED-A                    NC2394.2
045200     MOVE "INTERNAL INDEXING LEVEL 2  " TO RE-MARK                NC2394.2
045300     PERFORM FAIL.                                                NC2394.2
045400 TH2-WRITE-GF-3.                                                  NC2394.2
045500     PERFORM PRINT-DETAIL.                                        NC2394.2
045600                                                                  NC2394.2
045700 TH3-INIT-GF-1.                                                   NC2394.2
045800     MOVE "TH3-TEST-GF-1" TO PAR-NAME.                            NC2394.2
045900     MOVE "IV-21 & II-15 4.4.2" TO ANSI-REFERENCE.                NC2394.2
046000     MOVE "LEVEL 3 INT INDEXING" TO FEATURE.                      NC2394.2
046100     SET IDX-1 TO 2.                                              NC2394.2
046200     SET IDX-2 TO 6.                                              NC2394.2
046300     SET IDX-3 TO 10.                                             NC2394.2
046400 TH3-TEST-GF-1.                                                   NC2394.2
046500     IF ENTRY-3 (IDX-1, IDX-2, IDX-3) IS NOT EQUAL TO             NC2394.2
046600              "ELEM (02,06,10)"                                   NC2394.2
046700         GO TO TH3-FAIL-GF-1.                                     NC2394.2
046800     PERFORM PASS.                                                NC2394.2
046900     GO TO TH3-WRITE-GF-1.                                        NC2394.2
047000 TH3-DELETE-GF-1.                                                 NC2394.2
047100     PERFORM DE-LETE.                                             NC2394.2
047200     GO TO TH3-WRITE-GF-1.                                        NC2394.2
047300 TH3-FAIL-GF-1.                                                   NC2394.2
047400     MOVE "ELEM (02,06,10)" TO CORRECT-A                          NC2394.2
047500     MOVE ENTRY-3 (IDX-1, IDX-2, IDX-3) TO COMPUTED-A             NC2394.2
047600     MOVE "INTERNAL INDEXING LEVEL 3  " TO RE-MARK                NC2394.2
047700     PERFORM FAIL.                                                NC2394.2
047800 TH3-WRITE-GF-1.                                                  NC2394.2
047900     PERFORM PRINT-DETAIL.                                        NC2394.2
048000                                                                  NC2394.2
048100 TH3-INIT-GF-2.                                                   NC2394.2
048200     MOVE "TH3-TEST-GF-2" TO PAR-NAME.                            NC2394.2
048300     MOVE "IV-21 & II-15 4.4.2" TO ANSI-REFERENCE.                NC2394.2
048400     MOVE "LEVEL 3 INT INDEXING" TO FEATURE.                      NC2394.2
048500     SET IDX-1, IDX-2, IDX-3 TO 6.                                NC2394.2
048600 TH3-TEST-GF-2.                                                   NC2394.2
048700     IF ENTRY-3 (IDX-1, IDX-2, IDX-3) IS NOT EQUAL TO             NC2394.2
048800              "ELEM (06,06,06)"                                   NC2394.2
048900         GO TO TH3-FAIL-GF-2.                                     NC2394.2
049000     PERFORM PASS.                                                NC2394.2
049100     GO TO TH3-WRITE-GF-2.                                        NC2394.2
049200 TH3-DELETE-GF-2.                                                 NC2394.2
049300     PERFORM DE-LETE.                                             NC2394.2
049400     GO TO TH3-WRITE-GF-2.                                        NC2394.2
049500 TH3-FAIL-GF-2.                                                   NC2394.2
049600     MOVE "ELEM (06,06,06)" TO CORRECT-A                          NC2394.2
049700     MOVE ENTRY-3 (IDX-1, IDX-2, IDX-3) TO COMPUTED-A             NC2394.2
049800     MOVE "INTERNAL INDEXING LEVEL 3  " TO RE-MARK                NC2394.2
049900     PERFORM FAIL.                                                NC2394.2
050000 TH3-WRITE-GF-2.                                                  NC2394.2
050100     PERFORM PRINT-DETAIL.                                        NC2394.2
050200                                                                  NC2394.2
050300 TH3-INIT-GF-3.                                                   NC2394.2
050400     MOVE "TH3-TEST-GF-3" TO PAR-NAME.                            NC2394.2
050500     MOVE "IV-21 & II-15 4.4.2" TO ANSI-REFERENCE.                NC2394.2
050600     MOVE "LEVEL 3 INT INDEXING" TO FEATURE.                      NC2394.2
050700     SET IDX-1 TO 9.                                              NC2394.2
050800     SET IDX-2 TO 8.                                              NC2394.2
050900     SET IDX-3 TO 7.                                              NC2394.2
051000 TH3-TEST-GF-3.                                                   NC2394.2
051100     IF ENTRY-3 (IDX-1, IDX-2, IDX-3) IS NOT EQUAL TO             NC2394.2
051200         "ELEM (09,08,07)"                                        NC2394.2
051300         GO TO TH3-FAIL-GF-3.                                     NC2394.2
051400     PERFORM PASS.                                                NC2394.2
051500     GO TO TH3-WRITE-GF-3.                                        NC2394.2
051600 TH3-DELETE-GF-3.                                                 NC2394.2
051700     PERFORM DE-LETE.                                             NC2394.2
051800     GO TO TH3-WRITE-GF-3.                                        NC2394.2
051900 TH3-FAIL-GF-3.                                                   NC2394.2
052000     MOVE "ELEM (09,08,07)" TO CORRECT-A                          NC2394.2
052100     MOVE ENTRY-3 (IDX-1, IDX-2, IDX-3) TO COMPUTED-A             NC2394.2
052200     MOVE "INTERNAL INDEXING LEVEL 3  " TO RE-MARK                NC2394.2
052300     PERFORM FAIL.                                                NC2394.2
052400 TH3-WRITE-GF-3.                                                  NC2394.2
052500     PERFORM PRINT-DETAIL.                                        NC2394.2
052600 CCVS-EXIT SECTION.                                               NC2394.2
052700 CCVS-999999.                                                     NC2394.2
052800     GO TO CLOSE-FILES.                                           NC2394.2
      *END-OF,NC239A                                                                  
