      *HEADER,COBOL,NC136A                                                            
000100 IDENTIFICATION DIVISION.                                         NC1364.2
000200 PROGRAM-ID.                                                      NC1364.2
000300     NC136A.                                                      NC1364.2
000400****************************************************************  NC1364.2
000500*                                                              *  NC1364.2
000600*    VALIDATION FOR:-                                          *  NC1364.2
000700*                                                              *  NC1364.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1364.2
000900*                                                              *  NC1364.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC1364.2
001100*                                                              *  NC1364.2
001200****************************************************************  NC1364.2
001300*                                                              *  NC1364.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  NC1364.2
001500*                                                              *  NC1364.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  NC1364.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  NC1364.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  NC1364.2
001900*                                                              *  NC1364.2
002000****************************************************************  NC1364.2
002100*                                                                 NC1364.2
002200*    PROGRAM NC136A VERIFIES THE ACCURACY OF BUILDING AND         NC1364.2
002300*    ACCESSING A 3 DIMENSIONAL TABLE USING VARIOUS COMBINATIONS   NC1364.2
002400*    OF NUMERIC LITERAL AND DATA-NAME SUBSCRIPTS.                 NC1364.2
002500*                                                                 NC1364.2
002600 ENVIRONMENT DIVISION.                                            NC1364.2
002700 CONFIGURATION SECTION.                                           NC1364.2
002800 SOURCE-COMPUTER.                                                 NC1364.2
002900     XXXXX082.                                                    NC1364.2
003000 OBJECT-COMPUTER.                                                 NC1364.2
003100     XXXXX083.                                                    NC1364.2
003200 INPUT-OUTPUT SECTION.                                            NC1364.2
003300 FILE-CONTROL.                                                    NC1364.2
003400     SELECT PRINT-FILE ASSIGN TO                                  NC1364.2
003500     XXXXX055.                                                    NC1364.2
003600 DATA DIVISION.                                                   NC1364.2
003700 FILE SECTION.                                                    NC1364.2
003800 FD  PRINT-FILE.                                                  NC1364.2
003900 01  PRINT-REC PICTURE X(120).                                    NC1364.2
004000 01  DUMMY-RECORD PICTURE X(120).                                 NC1364.2
004100 WORKING-STORAGE SECTION.                                         NC1364.2
004200 77  SUB-1              PICTURE S99  VALUE ZERO.                  NC1364.2
004300 77  SUB-2              PICTURE 99   VALUE ZERO.                  NC1364.2
004400 77  SUB-3              PICTURE 99   VALUE ZERO.                  NC1364.2
004500 77  CON-7              PICTURE 99   VALUE 07.                    NC1364.2
004600 77  CON-10             PICTURE 99   VALUE 10.                    NC1364.2
004700 77  CON-5              PICTURE 99   VALUE 05.                    NC1364.2
004800 77  CON-6              PICTURE 99   VALUE 06.                    NC1364.2
004900 01  GRP-NAME.                                                    NC1364.2
005000     02  FILLER              PICTURE XXX    VALUE "GRP".          NC1364.2
005100     02  ADD-GRP             PICTURE 99     VALUE 01.             NC1364.2
005200                                                                  NC1364.2
005300 01  SEC-NAME.                                                    NC1364.2
005400     02  FILLER              PICTURE X(5)   VALUE "SEC (".        NC1364.2
005500     02  SEC-GRP             PICTURE 99     VALUE 00.             NC1364.2
005600     02  FILLER              PICTURE X      VALUE " ".            NC1364.2
005700     02  ADD-SEC             PICTURE 99     VALUE 01.             NC1364.2
005800     02  FILLER              PICTURE X      VALUE ")".            NC1364.2
005900                                                                  NC1364.2
006000 01  ELEM-NAME.                                                   NC1364.2
006100     02  FILLER              PICTURE X(6)   VALUE "ELEM (".       NC1364.2
006200     02  ELEM-GRP            PICTURE 99     VALUE 00.             NC1364.2
006300     02  FILLER              PICTURE X      VALUE " ".            NC1364.2
006400     02  ELEM-SEC            PICTURE 99     VALUE 00.             NC1364.2
006500     02  FILLER              PICTURE X      VALUE " ".            NC1364.2
006600     02  ADD-ELEM            PICTURE 99     VALUE 01.             NC1364.2
006700     02  FILLER              PICTURE X      VALUE ")".            NC1364.2
006800                                                                  NC1364.2
006900 01  THREE-DIMENSION-TBL.                                         NC1364.2
007000     02  GRP-ENTRY OCCURS 10 TIMES INDEXED BY IDX-1.              NC1364.2
007100         03  ENTRY-1         PICTURE X(5).                        NC1364.2
007200         03  GRP2-ENTRY OCCURS 10 TIMES INDEXED BY IDX-2.         NC1364.2
007300             04  ENTRY-2     PICTURE X(11).                       NC1364.2
007400             04  GRP3-ENTRY OCCURS 10 TIMES INDEXED BY IDX-3.     NC1364.2
007500                 05  ENTRY-3 PICTURE X(15).                       NC1364.2
007600                                                                  NC1364.2
007700 01  TEST-RESULTS.                                                NC1364.2
007800     02 FILLER                   PIC X      VALUE SPACE.          NC1364.2
007900     02 FEATURE                  PIC X(20)  VALUE SPACE.          NC1364.2
008000     02 FILLER                   PIC X      VALUE SPACE.          NC1364.2
008100     02 P-OR-F                   PIC X(5)   VALUE SPACE.          NC1364.2
008200     02 FILLER                   PIC X      VALUE SPACE.          NC1364.2
008300     02  PAR-NAME.                                                NC1364.2
008400       03 FILLER                 PIC X(19)  VALUE SPACE.          NC1364.2
008500       03  PARDOT-X              PIC X      VALUE SPACE.          NC1364.2
008600       03 DOTVALUE               PIC 99     VALUE ZERO.           NC1364.2
008700     02 FILLER                   PIC X(8)   VALUE SPACE.          NC1364.2
008800     02 RE-MARK                  PIC X(61).                       NC1364.2
008900 01  TEST-COMPUTED.                                               NC1364.2
009000     02 FILLER                   PIC X(30)  VALUE SPACE.          NC1364.2
009100     02 FILLER                   PIC X(17)  VALUE                 NC1364.2
009200            "       COMPUTED=".                                   NC1364.2
009300     02 COMPUTED-X.                                               NC1364.2
009400     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          NC1364.2
009500     03 COMPUTED-N               REDEFINES COMPUTED-A             NC1364.2
009600                                 PIC -9(9).9(9).                  NC1364.2
009700     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         NC1364.2
009800     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     NC1364.2
009900     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     NC1364.2
010000     03       CM-18V0 REDEFINES COMPUTED-A.                       NC1364.2
010100         04 COMPUTED-18V0                    PIC -9(18).          NC1364.2
010200         04 FILLER                           PIC X.               NC1364.2
010300     03 FILLER PIC X(50) VALUE SPACE.                             NC1364.2
010400 01  TEST-CORRECT.                                                NC1364.2
010500     02 FILLER PIC X(30) VALUE SPACE.                             NC1364.2
010600     02 FILLER PIC X(17) VALUE "       CORRECT =".                NC1364.2
010700     02 CORRECT-X.                                                NC1364.2
010800     03 CORRECT-A                  PIC X(20) VALUE SPACE.         NC1364.2
010900     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      NC1364.2
011000     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         NC1364.2
011100     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     NC1364.2
011200     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     NC1364.2
011300     03      CR-18V0 REDEFINES CORRECT-A.                         NC1364.2
011400         04 CORRECT-18V0                     PIC -9(18).          NC1364.2
011500         04 FILLER                           PIC X.               NC1364.2
011600     03 FILLER PIC X(2) VALUE SPACE.                              NC1364.2
011700     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     NC1364.2
011800 01  CCVS-C-1.                                                    NC1364.2
011900     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PANC1364.2
012000-    "SS  PARAGRAPH-NAME                                          NC1364.2
012100-    "       REMARKS".                                            NC1364.2
012200     02 FILLER                     PIC X(20)    VALUE SPACE.      NC1364.2
012300 01  CCVS-C-2.                                                    NC1364.2
012400     02 FILLER                     PIC X        VALUE SPACE.      NC1364.2
012500     02 FILLER                     PIC X(6)     VALUE "TESTED".   NC1364.2
012600     02 FILLER                     PIC X(15)    VALUE SPACE.      NC1364.2
012700     02 FILLER                     PIC X(4)     VALUE "FAIL".     NC1364.2
012800     02 FILLER                     PIC X(94)    VALUE SPACE.      NC1364.2
012900 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       NC1364.2
013000 01  REC-CT                        PIC 99       VALUE ZERO.       NC1364.2
013100 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       NC1364.2
013200 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       NC1364.2
013300 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       NC1364.2
013400 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       NC1364.2
013500 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       NC1364.2
013600 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       NC1364.2
013700 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      NC1364.2
013800 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       NC1364.2
013900 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     NC1364.2
014000 01  CCVS-H-1.                                                    NC1364.2
014100     02  FILLER                    PIC X(39)    VALUE SPACES.     NC1364.2
014200     02  FILLER                    PIC X(42)    VALUE             NC1364.2
014300     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 NC1364.2
014400     02  FILLER                    PIC X(39)    VALUE SPACES.     NC1364.2
014500 01  CCVS-H-2A.                                                   NC1364.2
014600   02  FILLER                        PIC X(40)  VALUE SPACE.      NC1364.2
014700   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  NC1364.2
014800   02  FILLER                        PIC XXXX   VALUE             NC1364.2
014900     "4.2 ".                                                      NC1364.2
015000   02  FILLER                        PIC X(28)  VALUE             NC1364.2
015100            " COPY - NOT FOR DISTRIBUTION".                       NC1364.2
015200   02  FILLER                        PIC X(41)  VALUE SPACE.      NC1364.2
015300                                                                  NC1364.2
015400 01  CCVS-H-2B.                                                   NC1364.2
015500   02  FILLER                        PIC X(15)  VALUE             NC1364.2
015600            "TEST RESULT OF ".                                    NC1364.2
015700   02  TEST-ID                       PIC X(9).                    NC1364.2
015800   02  FILLER                        PIC X(4)   VALUE             NC1364.2
015900            " IN ".                                               NC1364.2
016000   02  FILLER                        PIC X(12)  VALUE             NC1364.2
016100     " HIGH       ".                                              NC1364.2
016200   02  FILLER                        PIC X(22)  VALUE             NC1364.2
016300            " LEVEL VALIDATION FOR ".                             NC1364.2
016400   02  FILLER                        PIC X(58)  VALUE             NC1364.2
016500     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1364.2
016600 01  CCVS-H-3.                                                    NC1364.2
016700     02  FILLER                      PIC X(34)  VALUE             NC1364.2
016800            " FOR OFFICIAL USE ONLY    ".                         NC1364.2
016900     02  FILLER                      PIC X(58)  VALUE             NC1364.2
017000     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC1364.2
017100     02  FILLER                      PIC X(28)  VALUE             NC1364.2
017200            "  COPYRIGHT   1985 ".                                NC1364.2
017300 01  CCVS-E-1.                                                    NC1364.2
017400     02 FILLER                       PIC X(52)  VALUE SPACE.      NC1364.2
017500     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              NC1364.2
017600     02 ID-AGAIN                     PIC X(9).                    NC1364.2
017700     02 FILLER                       PIC X(45)  VALUE SPACES.     NC1364.2
017800 01  CCVS-E-2.                                                    NC1364.2
017900     02  FILLER                      PIC X(31)  VALUE SPACE.      NC1364.2
018000     02  FILLER                      PIC X(21)  VALUE SPACE.      NC1364.2
018100     02 CCVS-E-2-2.                                               NC1364.2
018200         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      NC1364.2
018300         03 FILLER                   PIC X      VALUE SPACE.      NC1364.2
018400         03 ENDER-DESC               PIC X(44)  VALUE             NC1364.2
018500            "ERRORS ENCOUNTERED".                                 NC1364.2
018600 01  CCVS-E-3.                                                    NC1364.2
018700     02  FILLER                      PIC X(22)  VALUE             NC1364.2
018800            " FOR OFFICIAL USE ONLY".                             NC1364.2
018900     02  FILLER                      PIC X(12)  VALUE SPACE.      NC1364.2
019000     02  FILLER                      PIC X(58)  VALUE             NC1364.2
019100     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1364.2
019200     02  FILLER                      PIC X(13)  VALUE SPACE.      NC1364.2
019300     02 FILLER                       PIC X(15)  VALUE             NC1364.2
019400             " COPYRIGHT 1985".                                   NC1364.2
019500 01  CCVS-E-4.                                                    NC1364.2
019600     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      NC1364.2
019700     02 FILLER                       PIC X(4)   VALUE " OF ".     NC1364.2
019800     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      NC1364.2
019900     02 FILLER                       PIC X(40)  VALUE             NC1364.2
020000      "  TESTS WERE EXECUTED SUCCESSFULLY".                       NC1364.2
020100 01  XXINFO.                                                      NC1364.2
020200     02 FILLER                       PIC X(19)  VALUE             NC1364.2
020300            "*** INFORMATION ***".                                NC1364.2
020400     02 INFO-TEXT.                                                NC1364.2
020500       04 FILLER                     PIC X(8)   VALUE SPACE.      NC1364.2
020600       04 XXCOMPUTED                 PIC X(20).                   NC1364.2
020700       04 FILLER                     PIC X(5)   VALUE SPACE.      NC1364.2
020800       04 XXCORRECT                  PIC X(20).                   NC1364.2
020900     02 INF-ANSI-REFERENCE           PIC X(48).                   NC1364.2
021000 01  HYPHEN-LINE.                                                 NC1364.2
021100     02 FILLER  PIC IS X VALUE IS SPACE.                          NC1364.2
021200     02 FILLER  PIC IS X(65)    VALUE IS "************************NC1364.2
021300-    "*****************************************".                 NC1364.2
021400     02 FILLER  PIC IS X(54)    VALUE IS "************************NC1364.2
021500-    "******************************".                            NC1364.2
021600 01  CCVS-PGM-ID                     PIC X(9)   VALUE             NC1364.2
021700     "NC136A".                                                    NC1364.2
021800 PROCEDURE DIVISION.                                              NC1364.2
021900 CCVS1 SECTION.                                                   NC1364.2
022000 OPEN-FILES.                                                      NC1364.2
022100     OPEN     OUTPUT PRINT-FILE.                                  NC1364.2
022200     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   NC1364.2
022300     MOVE    SPACE TO TEST-RESULTS.                               NC1364.2
022400     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             NC1364.2
022500     GO TO CCVS1-EXIT.                                            NC1364.2
022600 CLOSE-FILES.                                                     NC1364.2
022700     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   NC1364.2
022800 TERMINATE-CCVS.                                                  NC1364.2
022900*    EXIT PROGRAM.                                                NC1364.2
023000*TERMINATE-CALL.                                                  NC1364.2
023100     STOP     RUN.                                                NC1364.2
023200 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         NC1364.2
023300 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           NC1364.2
023400 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          NC1364.2
023500 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      NC1364.2
023600     MOVE "****TEST DELETED****" TO RE-MARK.                      NC1364.2
023700 PRINT-DETAIL.                                                    NC1364.2
023800     IF REC-CT NOT EQUAL TO ZERO                                  NC1364.2
023900             MOVE "." TO PARDOT-X                                 NC1364.2
024000             MOVE REC-CT TO DOTVALUE.                             NC1364.2
024100     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      NC1364.2
024200     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               NC1364.2
024300        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 NC1364.2
024400          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 NC1364.2
024500     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              NC1364.2
024600     MOVE SPACE TO CORRECT-X.                                     NC1364.2
024700     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         NC1364.2
024800     MOVE     SPACE TO RE-MARK.                                   NC1364.2
024900 HEAD-ROUTINE.                                                    NC1364.2
025000     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC1364.2
025100     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC1364.2
025200     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC1364.2
025300     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC1364.2
025400 COLUMN-NAMES-ROUTINE.                                            NC1364.2
025500     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1364.2
025600     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1364.2
025700     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        NC1364.2
025800 END-ROUTINE.                                                     NC1364.2
025900     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.NC1364.2
026000 END-RTN-EXIT.                                                    NC1364.2
026100     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1364.2
026200 END-ROUTINE-1.                                                   NC1364.2
026300      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      NC1364.2
026400      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               NC1364.2
026500      ADD PASS-COUNTER TO ERROR-HOLD.                             NC1364.2
026600*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   NC1364.2
026700      MOVE PASS-COUNTER TO CCVS-E-4-1.                            NC1364.2
026800      MOVE ERROR-HOLD TO CCVS-E-4-2.                              NC1364.2
026900      MOVE CCVS-E-4 TO CCVS-E-2-2.                                NC1364.2
027000      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           NC1364.2
027100  END-ROUTINE-12.                                                 NC1364.2
027200      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        NC1364.2
027300     IF       ERROR-COUNTER IS EQUAL TO ZERO                      NC1364.2
027400         MOVE "NO " TO ERROR-TOTAL                                NC1364.2
027500         ELSE                                                     NC1364.2
027600         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       NC1364.2
027700     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           NC1364.2
027800     PERFORM WRITE-LINE.                                          NC1364.2
027900 END-ROUTINE-13.                                                  NC1364.2
028000     IF DELETE-COUNTER IS EQUAL TO ZERO                           NC1364.2
028100         MOVE "NO " TO ERROR-TOTAL  ELSE                          NC1364.2
028200         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      NC1364.2
028300     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   NC1364.2
028400     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1364.2
028500      IF   INSPECT-COUNTER EQUAL TO ZERO                          NC1364.2
028600          MOVE "NO " TO ERROR-TOTAL                               NC1364.2
028700      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   NC1364.2
028800      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            NC1364.2
028900      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          NC1364.2
029000     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1364.2
029100 WRITE-LINE.                                                      NC1364.2
029200     ADD 1 TO RECORD-COUNT.                                       NC1364.2
029300*    IF RECORD-COUNT GREATER 42                                   NC1364.2
029400*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          NC1364.2
029500*        MOVE SPACE TO DUMMY-RECORD                               NC1364.2
029600*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  NC1364.2
029700*        MOVE CCVS-H-1  TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   NC1364.2
029800*        MOVE CCVS-H-2A TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   NC1364.2
029900*        MOVE CCVS-H-2B TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   NC1364.2
030000*        MOVE CCVS-H-3  TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   NC1364.2
030100*        MOVE CCVS-C-1  TO DUMMY-RECORD  PERFORM WRT-LN           NC1364.2
030200*        MOVE CCVS-C-2  TO DUMMY-RECORD  PERFORM WRT-LN           NC1364.2
030300*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          NC1364.2
030400*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          NC1364.2
030500*        MOVE ZERO TO RECORD-COUNT.                               NC1364.2
030600     PERFORM WRT-LN.                                              NC1364.2
030700 WRT-LN.                                                          NC1364.2
030800     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               NC1364.2
030900     MOVE SPACE TO DUMMY-RECORD.                                  NC1364.2
031000 BLANK-LINE-PRINT.                                                NC1364.2
031100     PERFORM WRT-LN.                                              NC1364.2
031200 FAIL-ROUTINE.                                                    NC1364.2
031300     IF     COMPUTED-X NOT EQUAL TO SPACE                         NC1364.2
031400            GO TO FAIL-ROUTINE-WRITE.                             NC1364.2
031500     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.NC1364.2
031600     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC1364.2
031700     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   NC1364.2
031800     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1364.2
031900     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC1364.2
032000     GO TO  FAIL-ROUTINE-EX.                                      NC1364.2
032100 FAIL-ROUTINE-WRITE.                                              NC1364.2
032200     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         NC1364.2
032300     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 NC1364.2
032400     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. NC1364.2
032500     MOVE   SPACES TO COR-ANSI-REFERENCE.                         NC1364.2
032600 FAIL-ROUTINE-EX. EXIT.                                           NC1364.2
032700 BAIL-OUT.                                                        NC1364.2
032800     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   NC1364.2
032900     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           NC1364.2
033000 BAIL-OUT-WRITE.                                                  NC1364.2
033100     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  NC1364.2
033200     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC1364.2
033300     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1364.2
033400     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC1364.2
033500 BAIL-OUT-EX. EXIT.                                               NC1364.2
033600 CCVS1-EXIT.                                                      NC1364.2
033700     EXIT.                                                        NC1364.2
033800 SECT-NC136A-001 SECTION.                                         NC1364.2
033900 NC136-001.                                                       NC1364.2
034000                                                                  NC1364.2
034100 BUILD-LEVEL-1.                                                   NC1364.2
034200     MOVE "IV-21 4.3.8.2" TO ANSI-REFERENCE.                      NC1364.2
034300     ADD 1 TO SUB-1.                                              NC1364.2
034400     IF SUB-1 EQUAL TO 11 GO TO CHECK-ENTRIES.                    NC1364.2
034500     MOVE GRP-NAME TO ENTRY-1 (SUB-1).                            NC1364.2
034600     ADD 1 TO ADD-GRP.                                            NC1364.2
034700                                                                  NC1364.2
034800 BUILD-LEVEL-2.                                                   NC1364.2
034900     ADD 1 TO SUB-2.                                              NC1364.2
035000     IF SUB-2 EQUAL TO 11                                         NC1364.2
035100         MOVE ZERO TO SUB-2                                       NC1364.2
035200         MOVE 01 TO ADD-SEC                                       NC1364.2
035300         GO TO BUILD-LEVEL-1.                                     NC1364.2
035400     MOVE SUB-1 TO SEC-GRP.                                       NC1364.2
035500     MOVE SEC-NAME TO ENTRY-2 (SUB-1  SUB-2).                     NC1364.2
035600     ADD 1 TO ADD-SEC.                                            NC1364.2
035700                                                                  NC1364.2
035800 BUILD-LEVEL-3.                                                   NC1364.2
035900     ADD 1 TO SUB-3.                                              NC1364.2
036000     IF SUB-3 EQUAL TO 11                                         NC1364.2
036100         MOVE ZERO TO SUB-3                                       NC1364.2
036200              MOVE 01 TO ADD-ELEM                                 NC1364.2
036300              GO TO BUILD-LEVEL-2.                                NC1364.2
036400     MOVE SUB-1 TO ELEM-GRP.                                      NC1364.2
036500     MOVE SUB-2 TO ELEM-SEC.                                      NC1364.2
036600     MOVE ELEM-NAME TO ENTRY-3 (SUB-1  SUB-2  SUB-3).             NC1364.2
036700     ADD 1 TO ADD-ELEM.                                           NC1364.2
036800     GO TO BUILD-LEVEL-3.                                         NC1364.2
036900                                                                  NC1364.2
037000 CHECK-ENTRIES.                                                   NC1364.2
037100     MOVE "LEVEL 1 TBL SUBSCRPT" TO FEATURE.                      NC1364.2
037200     MOVE "CHECK-ENTRIES       " TO PAR-NAME.                     NC1364.2
037300     IF ENTRY-1 (5) IS NOT EQUAL TO "GRP05"                       NC1364.2
037400         MOVE "GRP05" TO CORRECT-A                                NC1364.2
037500         MOVE ENTRY-1 (5) TO COMPUTED-A                           NC1364.2
037600                                                                  NC1364.2
037700         MOVE "NUMERIC LITERAL SUBSCRIPT  " TO RE-MARK            NC1364.2
037800         PERFORM FAIL                                             NC1364.2
037900         GO TO TEST-1-WRITE.                                      NC1364.2
038000                                                                  NC1364.2
038100     PERFORM PASS.                                                NC1364.2
038200 TEST-1-WRITE.                                                    NC1364.2
038300     PERFORM PRINT-DETAIL.                                        NC1364.2
038400                                                                  NC1364.2
038500 TEST-1-2.                                                        NC1364.2
038600     MOVE "TEST-1-2            " TO PAR-NAME.                     NC1364.2
038700     IF ENTRY-1 (CON-5) IS NOT EQUAL TO "GRP05"                   NC1364.2
038800         MOVE "GRP05" TO CORRECT-A                                NC1364.2
038900         MOVE ENTRY-1 (CON-5) TO COMPUTED-A                       NC1364.2
039000                                                                  NC1364.2
039100         MOVE "NUMERIC CONSTANT SUBSCRIPT " TO RE-MARK            NC1364.2
039200         PERFORM FAIL                                             NC1364.2
039300          GO TO TEST-1-2-WRITE.                                   NC1364.2
039400                                                                  NC1364.2
039500     PERFORM PASS.                                                NC1364.2
039600 TEST-1-2-WRITE.                                                  NC1364.2
039700     PERFORM PRINT-DETAIL.                                        NC1364.2
039800                                                                  NC1364.2
039900 TEST-2.                                                          NC1364.2
040000     MOVE "LEVEL 2 TBL SUBSCRPT" TO FEATURE.                      NC1364.2
040100     MOVE "TEST-2              " TO PAR-NAME.                     NC1364.2
040200     IF ENTRY-2 (5  6) IS NOT EQUAL TO "SEC (05 06)"              NC1364.2
040300         MOVE "SEC (05 06)" TO CORRECT-A                          NC1364.2
040400         MOVE ENTRY-2 (5  6) TO COMPUTED-A                        NC1364.2
040500                                                                  NC1364.2
040600         MOVE "NUMERIC LITERAL SUBSCRIPT  " TO RE-MARK            NC1364.2
040700         PERFORM FAIL                                             NC1364.2
040800         GO TO TEST-2-WRITE.                                      NC1364.2
040900                                                                  NC1364.2
041000     PERFORM PASS.                                                NC1364.2
041100 TEST-2-WRITE.                                                    NC1364.2
041200     PERFORM PRINT-DETAIL.                                        NC1364.2
041300                                                                  NC1364.2
041400 TEST-2-2.                                                        NC1364.2
041500     MOVE "TEST-2-2            " TO PAR-NAME.                     NC1364.2
041600     IF ENTRY-2 (05  CON-6) IS NOT EQUAL TO "SEC (05 06)"         NC1364.2
041700         MOVE "SEC (05 06)" TO CORRECT-A                          NC1364.2
041800         MOVE ENTRY-2 (05  CON-6) TO COMPUTED-A                   NC1364.2
041900                                                                  NC1364.2
042000         MOVE "NUM LITRL/CONSTANT SUBSCRPT" TO RE-MARK            NC1364.2
042100         PERFORM FAIL                                             NC1364.2
042200         GO TO TEST-2-2-WRITE.                                    NC1364.2
042300                                                                  NC1364.2
042400     PERFORM PASS.                                                NC1364.2
042500 TEST-2-2-WRITE.                                                  NC1364.2
042600     PERFORM PRINT-DETAIL.                                        NC1364.2
042700                                                                  NC1364.2
042800 TEST-2-3.                                                        NC1364.2
042900     MOVE "TEST-2-3            " TO PAR-NAME.                     NC1364.2
043000     IF ENTRY-2 (CON-5  CON-6) IS NOT EQUAL TO "SEC (05 06)"      NC1364.2
043100         MOVE "SEC (05 06)" TO CORRECT-A                          NC1364.2
043200         MOVE ENTRY-2 (CON-5  CON-6) TO COMPUTED-A                NC1364.2
043300                                                                  NC1364.2
043400         MOVE "2 NUMERIC CONSTANT SUBSCRPT" TO RE-MARK            NC1364.2
043500         PERFORM FAIL                                             NC1364.2
043600         GO TO TEST-2-3-WRITE.                                    NC1364.2
043700                                                                  NC1364.2
043800     PERFORM PASS.                                                NC1364.2
043900 TEST-2-3-WRITE.                                                  NC1364.2
044000     PERFORM PRINT-DETAIL.                                        NC1364.2
044100                                                                  NC1364.2
044200 TEST-3.                                                          NC1364.2
044300     MOVE "LEVEL 3 TBL SUBSCRPT" TO FEATURE.                      NC1364.2
044400     MOVE "TEST-3              " TO PAR-NAME.                     NC1364.2
044500     IF ENTRY-3 (10  05  06) IS NOT EQUAL TO "ELEM (10 05 06)"    NC1364.2
044600         MOVE "ELEM (10 05 06)" TO CORRECT-A                      NC1364.2
044700         MOVE ENTRY-3 (10  05  06) TO COMPUTED-A                  NC1364.2
044800                                                                  NC1364.2
044900         MOVE "3 NUMERIC LITERAL SUBSCRPTS" TO RE-MARK            NC1364.2
045000         PERFORM FAIL                                             NC1364.2
045100         GO TO TEST-3-WRITE.                                      NC1364.2
045200                                                                  NC1364.2
045300     PERFORM PASS.                                                NC1364.2
045400 TEST-3-WRITE.                                                    NC1364.2
045500     PERFORM PRINT-DETAIL.                                        NC1364.2
045600                                                                  NC1364.2
045700 TEST-3-2.                                                        NC1364.2
045800     MOVE "TEST-3-2            " TO PAR-NAME.                     NC1364.2
045900     IF ENTRY-3 (10  CON-5  CON-6) IS NOT EQUAL TO                NC1364.2
046000         "ELEM (10 05 06)"                                        NC1364.2
046100         MOVE "ELEM (10 05 06)" TO CORRECT-A                      NC1364.2
046200         MOVE ENTRY-3 (10  CON-5  CON-6) TO COMPUTED-A            NC1364.2
046300                                                                  NC1364.2
046400         MOVE "1 NUM LTRL/2 CONSTANT SUBS " TO RE-MARK            NC1364.2
046500         PERFORM FAIL                                             NC1364.2
046600         GO TO TEST-3-2-WRITE.                                    NC1364.2
046700                                                                  NC1364.2
046800     PERFORM PASS.                                                NC1364.2
046900 TEST-3-2-WRITE.                                                  NC1364.2
047000     PERFORM PRINT-DETAIL.                                        NC1364.2
047100                                                                  NC1364.2
047200 TEST-3-3.                                                        NC1364.2
047300     MOVE "TEST-3-3            " TO PAR-NAME.                     NC1364.2
047400     IF ENTRY-3 (CON-10  CON-5  CON-6) IS NOT EQUAL TO            NC1364.2
047500         "ELEM (10 05 06)" MOVE "ELEM (10 05 06)" TO CORRECT-A    NC1364.2
047600         MOVE ENTRY-3 (CON-10  CON-5  CON-6) TO COMPUTED-A        NC1364.2
047700                                                                  NC1364.2
047800         MOVE "3 NUMERIC CONSTANT SUBSCRPT" TO RE-MARK            NC1364.2
047900         PERFORM FAIL                                             NC1364.2
048000         GO TO END-3LEVEL-SUBSCRPT-TEST.                          NC1364.2
048100                                                                  NC1364.2
048200     PERFORM PASS.                                                NC1364.2
048300     GO TO END-3LEVEL-SUBSCRPT-TEST.                              NC1364.2
048400                                                                  NC1364.2
048500 END-3LEVEL-SUBSCRPT-TEST.                                        NC1364.2
048600     PERFORM PRINT-DETAIL.                                        NC1364.2
048700 CCVS-EXIT SECTION.                                               NC1364.2
048800 CCVS-999999.                                                     NC1364.2
048900     GO TO CLOSE-FILES.                                           NC1364.2
      *END-OF,NC136A                                                                  
