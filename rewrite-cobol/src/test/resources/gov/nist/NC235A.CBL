      *HEADER,COBOL,NC235A                                                            
000100 IDENTIFICATION DIVISION.                                         NC2354.2
000200 PROGRAM-ID.                                                      NC2354.2
000300     NC235A.                                                      NC2354.2
000400*                                                                 NC2354.2
000500****************************************************************  NC2354.2
000600*                                                              *  NC2354.2
000700*    VALIDATION FOR:-                                          *  NC2354.2
000800*                                                              *  NC2354.2
000900*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2354.2
001000*                                                              *  NC2354.2
001100*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC2354.2
001200*                                                              *  NC2354.2
001300****************************************************************  NC2354.2
001400*                                                              *  NC2354.2
001500*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  NC2354.2
001600*                                                              *  NC2354.2
001700*        X-55  - SYSTEM PRINTER NAME.                          *  NC2354.2
001800*        X-82  - SOURCE COMPUTER NAME.                         *  NC2354.2
001900*        X-83  - OBJECT COMPUTER NAME.                         *  NC2354.2
002000*                                                              *  NC2354.2
002100****************************************************************  NC2354.2
002200*    PROGRAM NC235A TESTS THE USE OF FORMATS 1 AND 2 OF THE    *  NC2354.2
002300*    "SEARCH" STATEMENT ON A ONE DIMENSIONAL TABLE WITH A      *  NC2354.2
002400*    VARIABLE NUMBER OF OCCURRENCES.   THE TABLE IS DEFINED    *  NC2354.2
002500*    USING FORMAT 2 OF THE "OCCURS" CLAUSE.                    *  NC2354.2
002600*                                                              *  NC2354.2
002700****************************************************************  NC2354.2
002800 ENVIRONMENT DIVISION.                                            NC2354.2
002900 CONFIGURATION SECTION.                                           NC2354.2
003000 SOURCE-COMPUTER.                                                 NC2354.2
003100     XXXXX082.                                                    NC2354.2
003200 OBJECT-COMPUTER.                                                 NC2354.2
003300     XXXXX083.                                                    NC2354.2
003400 INPUT-OUTPUT SECTION.                                            NC2354.2
003500 FILE-CONTROL.                                                    NC2354.2
003600     SELECT PRINT-FILE ASSIGN TO                                  NC2354.2
003700     XXXXX055.                                                    NC2354.2
003800 DATA DIVISION.                                                   NC2354.2
003900 FILE SECTION.                                                    NC2354.2
004000 FD  PRINT-FILE.                                                  NC2354.2
004100 01  PRINT-REC PICTURE X(120).                                    NC2354.2
004200 01  DUMMY-RECORD PICTURE X(120).                                 NC2354.2
004300 WORKING-STORAGE SECTION.                                         NC2354.2
004400 77  TBL-LENGTH                  PIC 99  VALUE 26.                NC2354.2
004500 77  SUB-1                       PIC 99  VALUE ZERO.              NC2354.2
004600 01  TBL-TH309.                                                   NC2354.2
004700     02  TH309-ENTRY OCCURS 1 TO 26 DEPENDING TBL-LENGTH          NC2354.2
004800         DESCENDING KEY IS DEC-KEY INDEXED BY IDX-1, IDX-2, IDX-3.NC2354.2
004900         03  DEC-KEY             PIC XX.                          NC2354.2
005000             88  FIRSTZ VALUE "ZZ".                               NC2354.2
005100             88  LASTA  VALUE "AA".                               NC2354.2
005200             88  MIDDLE-PP VALUE "PP".                            NC2354.2
005300 01  NOTE-1.                                                      NC2354.2
005400     02  FILLER                  PIC X(74) VALUE                  NC2354.2
005500     "NOTE 1 - CORRECT AND COMPUTED DATA ARE EQUAL BUT THE AT END NC2354.2
005600-    "PATH WAS TAKEN".                                            NC2354.2
005700     02  FILLER                  PIC X(46) VALUE SPACE.           NC2354.2
005800 01  NOTE-2.                                                      NC2354.2
005900     02  FILLER                  PIC X(112) VALUE                 NC2354.2
006000     "NOTE 2 - CORRECT AND COMPUTED DATA ARE NOT EQUAL. THE COMPUTNC2354.2
006100-    "ED ENTRY WAS EXTRACTED FROM THE TABLE BY SUBSCRIPTS.".      NC2354.2
006200     02  FILLER                  PIC X(8)  VALUE SPACE.           NC2354.2
006300 01  TEST-RESULTS.                                                NC2354.2
006400     02 FILLER                   PIC X      VALUE SPACE.          NC2354.2
006500     02 FEATURE                  PIC X(20)  VALUE SPACE.          NC2354.2
006600     02 FILLER                   PIC X      VALUE SPACE.          NC2354.2
006700     02 P-OR-F                   PIC X(5)   VALUE SPACE.          NC2354.2
006800     02 FILLER                   PIC X      VALUE SPACE.          NC2354.2
006900     02  PAR-NAME.                                                NC2354.2
007000       03 FILLER                 PIC X(19)  VALUE SPACE.          NC2354.2
007100       03  PARDOT-X              PIC X      VALUE SPACE.          NC2354.2
007200       03 DOTVALUE               PIC 99     VALUE ZERO.           NC2354.2
007300     02 FILLER                   PIC X(8)   VALUE SPACE.          NC2354.2
007400     02 RE-MARK                  PIC X(61).                       NC2354.2
007500 01  TEST-COMPUTED.                                               NC2354.2
007600     02 FILLER                   PIC X(30)  VALUE SPACE.          NC2354.2
007700     02 FILLER                   PIC X(17)  VALUE                 NC2354.2
007800            "       COMPUTED=".                                   NC2354.2
007900     02 COMPUTED-X.                                               NC2354.2
008000     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          NC2354.2
008100     03 COMPUTED-N               REDEFINES COMPUTED-A             NC2354.2
008200                                 PIC -9(9).9(9).                  NC2354.2
008300     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         NC2354.2
008400     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     NC2354.2
008500     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     NC2354.2
008600     03       CM-18V0 REDEFINES COMPUTED-A.                       NC2354.2
008700         04 COMPUTED-18V0                    PIC -9(18).          NC2354.2
008800         04 FILLER                           PIC X.               NC2354.2
008900     03 FILLER PIC X(50) VALUE SPACE.                             NC2354.2
009000 01  TEST-CORRECT.                                                NC2354.2
009100     02 FILLER PIC X(30) VALUE SPACE.                             NC2354.2
009200     02 FILLER PIC X(17) VALUE "       CORRECT =".                NC2354.2
009300     02 CORRECT-X.                                                NC2354.2
009400     03 CORRECT-A                  PIC X(20) VALUE SPACE.         NC2354.2
009500     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      NC2354.2
009600     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         NC2354.2
009700     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     NC2354.2
009800     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     NC2354.2
009900     03      CR-18V0 REDEFINES CORRECT-A.                         NC2354.2
010000         04 CORRECT-18V0                     PIC -9(18).          NC2354.2
010100         04 FILLER                           PIC X.               NC2354.2
010200     03 FILLER PIC X(2) VALUE SPACE.                              NC2354.2
010300     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     NC2354.2
010400 01  CCVS-C-1.                                                    NC2354.2
010500     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PANC2354.2
010600-    "SS  PARAGRAPH-NAME                                          NC2354.2
010700-    "       REMARKS".                                            NC2354.2
010800     02 FILLER                     PIC X(20)    VALUE SPACE.      NC2354.2
010900 01  CCVS-C-2.                                                    NC2354.2
011000     02 FILLER                     PIC X        VALUE SPACE.      NC2354.2
011100     02 FILLER                     PIC X(6)     VALUE "TESTED".   NC2354.2
011200     02 FILLER                     PIC X(15)    VALUE SPACE.      NC2354.2
011300     02 FILLER                     PIC X(4)     VALUE "FAIL".     NC2354.2
011400     02 FILLER                     PIC X(94)    VALUE SPACE.      NC2354.2
011500 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       NC2354.2
011600 01  REC-CT                        PIC 99       VALUE ZERO.       NC2354.2
011700 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       NC2354.2
011800 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       NC2354.2
011900 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       NC2354.2
012000 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       NC2354.2
012100 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       NC2354.2
012200 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       NC2354.2
012300 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      NC2354.2
012400 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       NC2354.2
012500 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     NC2354.2
012600 01  CCVS-H-1.                                                    NC2354.2
012700     02  FILLER                    PIC X(39)    VALUE SPACES.     NC2354.2
012800     02  FILLER                    PIC X(42)    VALUE             NC2354.2
012900     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 NC2354.2
013000     02  FILLER                    PIC X(39)    VALUE SPACES.     NC2354.2
013100 01  CCVS-H-2A.                                                   NC2354.2
013200   02  FILLER                        PIC X(40)  VALUE SPACE.      NC2354.2
013300   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  NC2354.2
013400   02  FILLER                        PIC XXXX   VALUE             NC2354.2
013500     "4.2 ".                                                      NC2354.2
013600   02  FILLER                        PIC X(28)  VALUE             NC2354.2
013700            " COPY - NOT FOR DISTRIBUTION".                       NC2354.2
013800   02  FILLER                        PIC X(41)  VALUE SPACE.      NC2354.2
013900                                                                  NC2354.2
014000 01  CCVS-H-2B.                                                   NC2354.2
014100   02  FILLER                        PIC X(15)  VALUE             NC2354.2
014200            "TEST RESULT OF ".                                    NC2354.2
014300   02  TEST-ID                       PIC X(9).                    NC2354.2
014400   02  FILLER                        PIC X(4)   VALUE             NC2354.2
014500            " IN ".                                               NC2354.2
014600   02  FILLER                        PIC X(12)  VALUE             NC2354.2
014700     " HIGH       ".                                              NC2354.2
014800   02  FILLER                        PIC X(22)  VALUE             NC2354.2
014900            " LEVEL VALIDATION FOR ".                             NC2354.2
015000   02  FILLER                        PIC X(58)  VALUE             NC2354.2
015100     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2354.2
015200 01  CCVS-H-3.                                                    NC2354.2
015300     02  FILLER                      PIC X(34)  VALUE             NC2354.2
015400            " FOR OFFICIAL USE ONLY    ".                         NC2354.2
015500     02  FILLER                      PIC X(58)  VALUE             NC2354.2
015600     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC2354.2
015700     02  FILLER                      PIC X(28)  VALUE             NC2354.2
015800            "  COPYRIGHT   1985 ".                                NC2354.2
015900 01  CCVS-E-1.                                                    NC2354.2
016000     02 FILLER                       PIC X(52)  VALUE SPACE.      NC2354.2
016100     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              NC2354.2
016200     02 ID-AGAIN                     PIC X(9).                    NC2354.2
016300     02 FILLER                       PIC X(45)  VALUE SPACES.     NC2354.2
016400 01  CCVS-E-2.                                                    NC2354.2
016500     02  FILLER                      PIC X(31)  VALUE SPACE.      NC2354.2
016600     02  FILLER                      PIC X(21)  VALUE SPACE.      NC2354.2
016700     02 CCVS-E-2-2.                                               NC2354.2
016800         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      NC2354.2
016900         03 FILLER                   PIC X      VALUE SPACE.      NC2354.2
017000         03 ENDER-DESC               PIC X(44)  VALUE             NC2354.2
017100            "ERRORS ENCOUNTERED".                                 NC2354.2
017200 01  CCVS-E-3.                                                    NC2354.2
017300     02  FILLER                      PIC X(22)  VALUE             NC2354.2
017400            " FOR OFFICIAL USE ONLY".                             NC2354.2
017500     02  FILLER                      PIC X(12)  VALUE SPACE.      NC2354.2
017600     02  FILLER                      PIC X(58)  VALUE             NC2354.2
017700     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2354.2
017800     02  FILLER                      PIC X(13)  VALUE SPACE.      NC2354.2
017900     02 FILLER                       PIC X(15)  VALUE             NC2354.2
018000             " COPYRIGHT 1985".                                   NC2354.2
018100 01  CCVS-E-4.                                                    NC2354.2
018200     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      NC2354.2
018300     02 FILLER                       PIC X(4)   VALUE " OF ".     NC2354.2
018400     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      NC2354.2
018500     02 FILLER                       PIC X(40)  VALUE             NC2354.2
018600      "  TESTS WERE EXECUTED SUCCESSFULLY".                       NC2354.2
018700 01  XXINFO.                                                      NC2354.2
018800     02 FILLER                       PIC X(19)  VALUE             NC2354.2
018900            "*** INFORMATION ***".                                NC2354.2
019000     02 INFO-TEXT.                                                NC2354.2
019100       04 FILLER                     PIC X(8)   VALUE SPACE.      NC2354.2
019200       04 XXCOMPUTED                 PIC X(20).                   NC2354.2
019300       04 FILLER                     PIC X(5)   VALUE SPACE.      NC2354.2
019400       04 XXCORRECT                  PIC X(20).                   NC2354.2
019500     02 INF-ANSI-REFERENCE           PIC X(48).                   NC2354.2
019600 01  HYPHEN-LINE.                                                 NC2354.2
019700     02 FILLER  PIC IS X VALUE IS SPACE.                          NC2354.2
019800     02 FILLER  PIC IS X(65)    VALUE IS "************************NC2354.2
019900-    "*****************************************".                 NC2354.2
020000     02 FILLER  PIC IS X(54)    VALUE IS "************************NC2354.2
020100-    "******************************".                            NC2354.2
020200 01  CCVS-PGM-ID                     PIC X(9)   VALUE             NC2354.2
020300     "NC235A".                                                    NC2354.2
020400 PROCEDURE DIVISION.                                              NC2354.2
020500 CCVS1 SECTION.                                                   NC2354.2
020600 OPEN-FILES.                                                      NC2354.2
020700     OPEN     OUTPUT PRINT-FILE.                                  NC2354.2
020800     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   NC2354.2
020900     MOVE    SPACE TO TEST-RESULTS.                               NC2354.2
021000     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             NC2354.2
021100     GO TO CCVS1-EXIT.                                            NC2354.2
021200 CLOSE-FILES.                                                     NC2354.2
021300     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   NC2354.2
021400 TERMINATE-CCVS.                                                  NC2354.2
021500*    EXIT PROGRAM.                                                NC2354.2
021600*TERMINATE-CALL.                                                  NC2354.2
021700     STOP     RUN.                                                NC2354.2
021800 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         NC2354.2
021900 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           NC2354.2
022000 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          NC2354.2
022100 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      NC2354.2
022200     MOVE "****TEST DELETED****" TO RE-MARK.                      NC2354.2
022300 PRINT-DETAIL.                                                    NC2354.2
022400     IF REC-CT NOT EQUAL TO ZERO                                  NC2354.2
022500             MOVE "." TO PARDOT-X                                 NC2354.2
022600             MOVE REC-CT TO DOTVALUE.                             NC2354.2
022700     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      NC2354.2
022800     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               NC2354.2
022900        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 NC2354.2
023000          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 NC2354.2
023100     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              NC2354.2
023200     MOVE SPACE TO CORRECT-X.                                     NC2354.2
023300     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         NC2354.2
023400     MOVE     SPACE TO RE-MARK.                                   NC2354.2
023500 HEAD-ROUTINE.                                                    NC2354.2
023600     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC2354.2
023700     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC2354.2
023800     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC2354.2
023900     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC2354.2
024000 COLUMN-NAMES-ROUTINE.                                            NC2354.2
024100     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2354.2
024200     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2354.2
024300     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        NC2354.2
024400 END-ROUTINE.                                                     NC2354.2
024500     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.NC2354.2
024600 END-RTN-EXIT.                                                    NC2354.2
024700     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2354.2
024800 END-ROUTINE-1.                                                   NC2354.2
024900      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      NC2354.2
025000      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               NC2354.2
025100      ADD PASS-COUNTER TO ERROR-HOLD.                             NC2354.2
025200*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   NC2354.2
025300      MOVE PASS-COUNTER TO CCVS-E-4-1.                            NC2354.2
025400      MOVE ERROR-HOLD TO CCVS-E-4-2.                              NC2354.2
025500      MOVE CCVS-E-4 TO CCVS-E-2-2.                                NC2354.2
025600      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           NC2354.2
025700  END-ROUTINE-12.                                                 NC2354.2
025800      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        NC2354.2
025900     IF       ERROR-COUNTER IS EQUAL TO ZERO                      NC2354.2
026000         MOVE "NO " TO ERROR-TOTAL                                NC2354.2
026100         ELSE                                                     NC2354.2
026200         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       NC2354.2
026300     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           NC2354.2
026400     PERFORM WRITE-LINE.                                          NC2354.2
026500 END-ROUTINE-13.                                                  NC2354.2
026600     IF DELETE-COUNTER IS EQUAL TO ZERO                           NC2354.2
026700         MOVE "NO " TO ERROR-TOTAL  ELSE                          NC2354.2
026800         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      NC2354.2
026900     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   NC2354.2
027000     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2354.2
027100      IF   INSPECT-COUNTER EQUAL TO ZERO                          NC2354.2
027200          MOVE "NO " TO ERROR-TOTAL                               NC2354.2
027300      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   NC2354.2
027400      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            NC2354.2
027500      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          NC2354.2
027600     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2354.2
027700 WRITE-LINE.                                                      NC2354.2
027800     ADD 1 TO RECORD-COUNT.                                       NC2354.2
027900*    IF RECORD-COUNT GREATER 50                                   NC2354.2
028000*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          NC2354.2
028100*        MOVE SPACE TO DUMMY-RECORD                               NC2354.2
028200*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  NC2354.2
028300*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             NC2354.2
028400*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     NC2354.2
028500*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          NC2354.2
028600*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          NC2354.2
028700*        MOVE ZERO TO RECORD-COUNT.                               NC2354.2
028800     PERFORM WRT-LN.                                              NC2354.2
028900 WRT-LN.                                                          NC2354.2
029000     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               NC2354.2
029100     MOVE SPACE TO DUMMY-RECORD.                                  NC2354.2
029200 BLANK-LINE-PRINT.                                                NC2354.2
029300     PERFORM WRT-LN.                                              NC2354.2
029400 FAIL-ROUTINE.                                                    NC2354.2
029500     IF   COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE. NC2354.2
029600     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.NC2354.2
029700     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC2354.2
029800     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   NC2354.2
029900     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2354.2
030000     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC2354.2
030100     GO TO  FAIL-ROUTINE-EX.                                      NC2354.2
030200 FAIL-ROUTINE-WRITE.                                              NC2354.2
030300     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         NC2354.2
030400     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 NC2354.2
030500     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. NC2354.2
030600     MOVE   SPACES TO COR-ANSI-REFERENCE.                         NC2354.2
030700 FAIL-ROUTINE-EX. EXIT.                                           NC2354.2
030800 BAIL-OUT.                                                        NC2354.2
030900     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   NC2354.2
031000     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           NC2354.2
031100 BAIL-OUT-WRITE.                                                  NC2354.2
031200     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  NC2354.2
031300     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC2354.2
031400     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2354.2
031500     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC2354.2
031600 BAIL-OUT-EX. EXIT.                                               NC2354.2
031700 CCVS1-EXIT.                                                      NC2354.2
031800     EXIT.                                                        NC2354.2
031900 SECT-NC235A-001 SECTION.                                         NC2354.2
032000 TH-08-001.                                                       NC2354.2
032100 INIT-TBL-TH309.                                                  NC2354.2
032200     MOVE "ZZYYXXWWVVUUTTSSRRQQPPOONNMMLLKKJJIIHHGGFFEEDDCCBBAA"  NC2354.2
032300         TO TBL-TH309.                                            NC2354.2
032400     IF FIRSTZ (1)               AND LASTA (26)                   NC2354.2
032500         MOVE "26 ENTRY TABLE CONSTRUCTED " TO RE-MARK            NC2354.2
032600         GO TO INIT-WRITE.                                        NC2354.2
032700     MOVE "TBL ENTRIES BUILT INCORRECT" TO RE-MARK.               NC2354.2
032800     MOVE "*****" TO CORRECT-A  COMPUTED-A.                       NC2354.2
032900 INIT-WRITE.                                                      NC2354.2
033000     MOVE "INIT-TBL-TH309" TO PAR-NAME.                           NC2354.2
033100     MOVE "LEVEL 3 TBL HANDLING" TO FEATURE.                      NC2354.2
033200     PERFORM PRINT-DETAIL.                                        NC2354.2
033300*                                                                 NC2354.2
033400 IDX-INIT-F2-1.                                                   NC2354.2
033500     MOVE "IDX-TEST-F2-1 " TO PAR-NAME.                           NC2354.2
033600     MOVE "VI-121 6.21.2" TO ANSI-REFERENCE.                      NC2354.2
033700     MOVE "LEVEL 3 TBL HANDLING" TO FEATURE.                      NC2354.2
033800     SET IDX-2 TO 26.                                             NC2354.2
033900 IDX-TEST-F2-1.                                                   NC2354.2
034000     SEARCH ALL TH309-ENTRY AT END                                NC2354.2
034100          GO TO IDX-FAIL-F2-1                                     NC2354.2
034200      WHEN DEC-KEY (IDX-1) EQUAL TO "BB" NEXT SENTENCE.           NC2354.2
034300     PERFORM PASS.                                                NC2354.2
034400     GO TO IDX-WRITE-F2-1.                                        NC2354.2
034500 IDX-DELETE-F2-1.                                                 NC2354.2
034600     PERFORM DE-LETE                                              NC2354.2
034700     GO TO IDX-WRITE-F2-1.                                        NC2354.2
034800 IDX-FAIL-F2-1.                                                   NC2354.2
034900     MOVE 25 TO SUB-1                                             NC2354.2
035000     MOVE "BB" TO CORRECT-A                                       NC2354.2
035100     PERFORM PUTOUT-COMPUTED-A.                                   NC2354.2
035200     PERFORM FAIL.                                                NC2354.2
035300 IDX-WRITE-F2-1.                                                  NC2354.2
035400     PERFORM PRINT-DETAIL.                                        NC2354.2
035500*                                                                 NC2354.2
035600 IDX-INIT-F2-2.                                                   NC2354.2
035700     MOVE "IDX-TEST-F2-2 " TO PAR-NAME.                           NC2354.2
035800     MOVE "VI-121 6.21.2" TO ANSI-REFERENCE.                      NC2354.2
035900     MOVE "LEVEL 3 TBL HANDLING" TO FEATURE.                      NC2354.2
036000     SET IDX-3 TO 01.                                             NC2354.2
036100 IDX-TEST-F2-2.                                                   NC2354.2
036200     SEARCH ALL TH309-ENTRY AT END                                NC2354.2
036300         GO TO IDX-FAIL-F2-2                                      NC2354.2
036400      WHEN DEC-KEY (IDX-1) EQUAL TO "XX"                          NC2354.2
036500         PERFORM PASS                                             NC2354.2
036600         GO TO IDX-WRITE-F2-2.                                    NC2354.2
036700 IDX-DELETE-F2-2.                                                 NC2354.2
036800     PERFORM DE-LETE.                                             NC2354.2
036900     GO TO IDX-WRITE-F2-2.                                        NC2354.2
037000 IDX-FAIL-F2-2.                                                   NC2354.2
037100     MOVE 03 TO SUB-1                                             NC2354.2
037200     MOVE "XX" TO CORRECT-A                                       NC2354.2
037300     PERFORM PUTOUT-COMPUTED-A.                                   NC2354.2
037400     PERFORM FAIL.                                                NC2354.2
037500 IDX-WRITE-F2-2.                                                  NC2354.2
037600     PERFORM PRINT-DETAIL.                                        NC2354.2
037700*                                                                 NC2354.2
037800 IDX-INIT-F2-3.                                                   NC2354.2
037900     MOVE "IDX-TEST-F2-3 " TO PAR-NAME.                           NC2354.2
038000     MOVE "VI-121 6.21.2" TO ANSI-REFERENCE.                      NC2354.2
038100     MOVE "LEVEL 3 TBL HANDLING" TO FEATURE.                      NC2354.2
038200     MOVE 25 TO TBL-LENGTH.                                       NC2354.2
038300 IDX-TEST-F2-3.                                                   NC2354.2
038400     SEARCH ALL TH309-ENTRY AT END                                NC2354.2
038500         PERFORM PASS                                             NC2354.2
038600         GO TO IDX-WRITE-F2-3                                     NC2354.2
038700      WHEN DEC-KEY (IDX-1) EQUAL TO "AA"                          NC2354.2
038800         GO TO IDX-FAIL-F2-3.                                     NC2354.2
038900 IDX-DELETE-F2-3.                                                 NC2354.2
039000     PERFORM DE-LETE.                                             NC2354.2
039100     GO TO IDX-WRITE-F2-3.                                        NC2354.2
039200 IDX-FAIL-F2-3.                                                   NC2354.2
039300     MOVE "ENTRY SHOULD NOT BE FOUND  " TO RE-MARK                NC2354.2
039400     MOVE "AA" TO COMPUTED-A                                      NC2354.2
039500     PERFORM FAIL.                                                NC2354.2
039600 IDX-WRITE-F2-3.                                                  NC2354.2
039700     PERFORM PRINT-DETAIL.                                        NC2354.2
039800*                                                                 NC2354.2
039900 IDX-INIT-F1-4.                                                   NC2354.2
040000     MOVE "IDX-TEST-F1-4" TO PAR-NAME.                            NC2354.2
040100     MOVE "VI-121 6.21.2" TO ANSI-REFERENCE.                      NC2354.2
040200     MOVE "LEVEL 3 TBL HANDLING" TO FEATURE.                      NC2354.2
040300     MOVE 24 TO TBL-LENGTH.                                       NC2354.2
040400     SET IDX-3 TO 01.                                             NC2354.2
040500 IDX-TEST-F1-4.                                                   NC2354.2
040600     SEARCH TH309-ENTRY VARYING IDX-3 AT END                      NC2354.2
040700         PERFORM PASS                                             NC2354.2
040800         GO TO IDX-WRITE-F1-4                                     NC2354.2
040900      WHEN DEC-KEY (IDX-3) EQUAL TO "BB"                          NC2354.2
041000         GO TO IDX-FAIL-F1-4.                                     NC2354.2
041100 IDX-DELETE-F1-4.                                                 NC2354.2
041200     PERFORM DE-LETE.                                             NC2354.2
041300     GO TO IDX-WRITE-F1-4.                                        NC2354.2
041400 IDX-FAIL-F1-4.                                                   NC2354.2
041500     MOVE "ENTRY SHOULD NOT BE FOUND  " TO RE-MARK                NC2354.2
041600     MOVE "BB" TO COMPUTED-A                                      NC2354.2
041700     PERFORM FAIL.                                                NC2354.2
041800 IDX-WRITE-F1-4.                                                  NC2354.2
041900     PERFORM PRINT-DETAIL.                                        NC2354.2
042000*                                                                 NC2354.2
042100 IDX-INIT-F1-5.                                                   NC2354.2
042200     MOVE "IDX-TEST-F1-5 " TO PAR-NAME.                           NC2354.2
042300     MOVE "VI-121 6.21.2" TO ANSI-REFERENCE.                      NC2354.2
042400     MOVE "LEVEL 3 TBL HANDLING" TO FEATURE.                      NC2354.2
042500     SET IDX-2 TO 01.                                             NC2354.2
042600 IDX-TEST-F1-5.                                                   NC2354.2
042700     SEARCH TH309-ENTRY VARYING IDX-2 AT END                      NC2354.2
042800         GO TO IDX-FAIL-F1-5                                      NC2354.2
042900     WHEN DEC-KEY (IDX-2) EQUAL TO "KK"                           NC2354.2
043000         PERFORM PASS                                             NC2354.2
043100         GO TO IDX-WRITE-F1-5.                                    NC2354.2
043200 IDX-DELETE-F1-5.                                                 NC2354.2
043300     PERFORM DE-LETE.                                             NC2354.2
043400     GO TO IDX-WRITE-F1-5.                                        NC2354.2
043500 IDX-FAIL-F1-5.                                                   NC2354.2
043600     MOVE 16 TO SUB-1                                             NC2354.2
043700     MOVE "KK" TO CORRECT-A                                       NC2354.2
043800     PERFORM PUTOUT-COMPUTED-A.                                   NC2354.2
043900     PERFORM FAIL.                                                NC2354.2
044000 IDX-WRITE-F1-5.                                                  NC2354.2
044100     PERFORM PRINT-DETAIL.                                        NC2354.2
044200*                                                                 NC2354.2
044300 IDX-INIT-F1-6.                                                   NC2354.2
044400     MOVE "IDX-TEST-F1-6 " TO PAR-NAME.                           NC2354.2
044500     MOVE "VI-121 6.21.2" TO ANSI-REFERENCE.                      NC2354.2
044600     MOVE "LEVEL 3 TBL HANDLING" TO FEATURE.                      NC2354.2
044700     MOVE 22 TO TBL-LENGTH.                                       NC2354.2
044800     SET IDX-1 TO 09.                                             NC2354.2
044900 IDX-TEST-F1-6.                                                   NC2354.2
045000     SEARCH TH309-ENTRY VARYING IDX-1 AT END                      NC2354.2
045100         PERFORM PASS                                             NC2354.2
045200         GO TO IDX-WRITE-F1-6                                     NC2354.2
045300     WHEN TH309-ENTRY (IDX-1) EQUAL TO "DD"                       NC2354.2
045400         GO TO IDX-FAIL-F1-6.                                     NC2354.2
045500 IDX-DELETE-F1-6.                                                 NC2354.2
045600     PERFORM DE-LETE.                                             NC2354.2
045700     GO TO IDX-WRITE-F1-6.                                        NC2354.2
045800 IDX-FAIL-F1-6.                                                   NC2354.2
045900     MOVE "ENTRY SHOULD NOT BE FOUND  " TO RE-MARK                NC2354.2
046000     MOVE "DD" TO COMPUTED-A                                      NC2354.2
046100     PERFORM FAIL.                                                NC2354.2
046200 IDX-WRITE-F1-6.                                                  NC2354.2
046300     PERFORM PRINT-DETAIL.                                        NC2354.2
046400*                                                                 NC2354.2
046500 IDX-INIT-F1-7.                                                   NC2354.2
046600     MOVE "IDX-TEST-F1-7 " TO PAR-NAME.                           NC2354.2
046700     MOVE "VI-121 6.21.2" TO ANSI-REFERENCE.                      NC2354.2
046800     MOVE "LEVEL 3 TBL HANDLING" TO FEATURE.                      NC2354.2
046900     MOVE 22 TO TBL-LENGTH.                                       NC2354.2
047000     SET IDX-3 TO 23.                                             NC2354.2
047100 IDX-TEST-F1-7.                                                   NC2354.2
047200     SEARCH TH309-ENTRY VARYING IDX-3 AT END                      NC2354.2
047300         PERFORM PASS                                             NC2354.2
047400         GO TO IDX-WRITE-F1-7                                     NC2354.2
047500     WHEN TH309-ENTRY (IDX-3) EQUAL TO "DD"                       NC2354.2
047600         GO TO IDX-FAIL-F1-7.                                     NC2354.2
047700 IDX-DELETE-F1-7.                                                 NC2354.2
047800     PERFORM DE-LETE.                                             NC2354.2
047900     GO TO IDX-WRITE-F1-7.                                        NC2354.2
048000 IDX-FAIL-F1-7.                                                   NC2354.2
048100     MOVE "ENTRY SHOULD NOT BE FOUND  " TO RE-MARK                NC2354.2
048200     MOVE "DD" TO COMPUTED-A                                      NC2354.2
048300     PERFORM FAIL.                                                NC2354.2
048400 IDX-WRITE-F1-7.                                                  NC2354.2
048500     PERFORM PRINT-DETAIL.                                        NC2354.2
048600*                                                                 NC2354.2
048700 IDX-INIT-F2-8.                                                   NC2354.2
048800     MOVE "IDX-TEST-F2-8 " TO PAR-NAME.                           NC2354.2
048900     MOVE "VI-121 6.21.2" TO ANSI-REFERENCE.                      NC2354.2
049000     MOVE "LEVEL 3 TBL HANDLING" TO FEATURE.                      NC2354.2
049100     MOVE 20 TO TBL-LENGTH.                                       NC2354.2
049200     SET IDX-2 TO 21.                                             NC2354.2
049300 IDX-TEST-F2-8.                                                   NC2354.2
049400     SEARCH ALL TH309-ENTRY AT END                                NC2354.2
049500         GO TO IDX-FAIL-F2-8                                      NC2354.2
049600     WHEN DEC-KEY (IDX-1) EQUAL TO "GG"                           NC2354.2
049700         PERFORM PASS                                             NC2354.2
049800         GO TO IDX-WRITE-F2-8.                                    NC2354.2
049900 IDX-DELETE-F2-8.                                                 NC2354.2
050000     PERFORM DE-LETE.                                             NC2354.2
050100     GO TO IDX-WRITE-F2-8.                                        NC2354.2
050200 IDX-FAIL-F2-8.                                                   NC2354.2
050300     MOVE 20 TO SUB-1                                             NC2354.2
050400     MOVE "GG" TO CORRECT-A                                       NC2354.2
050500     PERFORM PUTOUT-COMPUTED-A.                                   NC2354.2
050600     PERFORM FAIL.                                                NC2354.2
050700 IDX-WRITE-F2-8.                                                  NC2354.2
050800     PERFORM PRINT-DETAIL.                                        NC2354.2
050900*                                                                 NC2354.2
051000 IDX-INIT-F2-9.                                                   NC2354.2
051100     MOVE "IDX-TEST-F2-9 " TO PAR-NAME.                           NC2354.2
051200     MOVE "VI-121 6.21.2" TO ANSI-REFERENCE.                      NC2354.2
051300     MOVE "LEVEL 3 TBL HANDLING" TO FEATURE.                      NC2354.2
051400     MOVE 20 TO TBL-LENGTH.                                       NC2354.2
051500 IDX-TEST-F2-9.                                                   NC2354.2
051600     SEARCH ALL TH309-ENTRY AT END                                NC2354.2
051700         PERFORM PASS                                             NC2354.2
051800         GO TO IDX-WRITE-F2-9                                     NC2354.2
051900     WHEN   LASTA (IDX-1)                                         NC2354.2
052000         GO TO IDX-FAIL-F2-9.                                     NC2354.2
052100 IDX-DELETE-F2-9.                                                 NC2354.2
052200     PERFORM DE-LETE.                                             NC2354.2
052300     GO TO IDX-WRITE-F2-9.                                        NC2354.2
052400 IDX-FAIL-F2-9.                                                   NC2354.2
052500     MOVE "CONDITION-NAME TEST" TO RE-MARK                        NC2354.2
052600     PERFORM FAIL                                                 NC2354.2
052700     MOVE "AA" TO COMPUTED-A.                                     NC2354.2
052800 IDX-WRITE-F2-9.                                                  NC2354.2
052900     PERFORM PRINT-DETAIL.                                        NC2354.2
053000*                                                                 NC2354.2
053100 IDX-INIT-F2-10.                                                  NC2354.2
053200     MOVE "IDX-TEST-F2-10 " TO PAR-NAME.                          NC2354.2
053300     MOVE "VI-121 6.21.2" TO ANSI-REFERENCE.                      NC2354.2
053400     MOVE "LEVEL 3 TBL HANDLING" TO FEATURE.                      NC2354.2
053500     SET IDX-1, IDX-2, IDX-3 TO 10.                               NC2354.2
053600 IDX-TEST-F2-10.                                                  NC2354.2
053700     SEARCH ALL TH309-ENTRY AT END                                NC2354.2
053800         GO TO IDX-FAIL-F2-10                                     NC2354.2
053900     WHEN DEC-KEY (IDX-1) EQUAL TO "RR"                           NC2354.2
054000         PERFORM PASS                                             NC2354.2
054100         GO TO IDX-WRITE-F2-10.                                   NC2354.2
054200 IDX-DELETE-F2-10.                                                NC2354.2
054300     PERFORM DE-LETE.                                             NC2354.2
054400     GO TO IDX-WRITE-F2-10.                                       NC2354.2
054500 IDX-FAIL-F2-10.                                                  NC2354.2
054600     MOVE 9 TO SUB-1                                              NC2354.2
054700     MOVE "RR" TO CORRECT-A                                       NC2354.2
054800     PERFORM PUTOUT-COMPUTED-A.                                   NC2354.2
054900     PERFORM FAIL.                                                NC2354.2
055000 IDX-WRITE-F2-10.                                                 NC2354.2
055100     PERFORM PRINT-DETAIL.                                        NC2354.2
055200*                                                                 NC2354.2
055300 IDX-INIT-F2-11.                                                  NC2354.2
055400     MOVE "IDX-TEST-F2-11 " TO PAR-NAME.                          NC2354.2
055500     MOVE "VI-121 6.21.2" TO ANSI-REFERENCE.                      NC2354.2
055600     MOVE "LEVEL 3 TBL HANDLING" TO FEATURE.                      NC2354.2
055700     MOVE 1 TO TBL-LENGTH.                                        NC2354.2
055800 IDX-TEST-F2-11.                                                  NC2354.2
055900     SEARCH ALL TH309-ENTRY AT END                                NC2354.2
056000         PERFORM PASS                                             NC2354.2
056100         GO TO IDX-WRITE-F2-11                                    NC2354.2
056200     WHEN DEC-KEY (IDX-1) EQUAL TO "YY"                           NC2354.2
056300         GO TO IDX-FAIL-F2-11.                                    NC2354.2
056400 IDX-DELETE-F2-11.                                                NC2354.2
056500     PERFORM DE-LETE.                                             NC2354.2
056600     GO TO IDX-WRITE-F2-11.                                       NC2354.2
056700 IDX-FAIL-F2-11.                                                  NC2354.2
056800     MOVE 2 TO SUB-1                                              NC2354.2
056900     MOVE "YY" TO COMPUTED-A                                      NC2354.2
057000     MOVE "ENTRY SHOULD NOT BE FOUND" TO RE-MARK                  NC2354.2
057100     PERFORM FAIL.                                                NC2354.2
057200 IDX-WRITE-F2-11.                                                 NC2354.2
057300     PERFORM PRINT-DETAIL.                                        NC2354.2
057400*                                                                 NC2354.2
057500 IDX-INIT-F2-12.                                                  NC2354.2
057600     MOVE "IDX-TEST-F2-12 " TO PAR-NAME.                          NC2354.2
057700     MOVE "VI-121 6.21.2" TO ANSI-REFERENCE.                      NC2354.2
057800     MOVE "LEVEL 3 TBL HANDLING" TO FEATURE.                      NC2354.2
057900     MOVE 10 TO TBL-LENGTH.                                       NC2354.2
058000 IDX-TEST-F2-12.                                                  NC2354.2
058100     SEARCH ALL TH309-ENTRY AT END                                NC2354.2
058200         PERFORM PASS                                             NC2354.2
058300         GO TO IDX-WRITE-F2-12                                    NC2354.2
058400     WHEN MIDDLE-PP (IDX-1)                                       NC2354.2
058500         GO TO IDX-FAIL-F2-12.                                    NC2354.2
058600 IDX-DELETE-F2-12.                                                NC2354.2
058700     PERFORM DE-LETE.                                             NC2354.2
058800     GO TO IDX-WRITE-F2-12.                                       NC2354.2
058900 IDX-FAIL-F2-12.                                                  NC2354.2
059000     MOVE 10 TO SUB-1                                             NC2354.2
059100     MOVE "ENTRY SHOULD NOT BE FOUND " TO RE-MARK                 NC2354.2
059200     MOVE "PP" TO COMPUTED-A                                      NC2354.2
059300     PERFORM FAIL.                                                NC2354.2
059400 IDX-WRITE-F2-12.                                                 NC2354.2
059500     PERFORM PRINT-DETAIL.                                        NC2354.2
059600*                                                                 NC2354.2
059700 IDX-INIT-F2-13.                                                  NC2354.2
059800     MOVE "IDX-TEST-F2-13 " TO PAR-NAME.                          NC2354.2
059900     MOVE "VI-121 6.21.2" TO ANSI-REFERENCE.                      NC2354.2
060000     MOVE "LEVEL 3 TBL HANDLING" TO FEATURE.                      NC2354.2
060100     MOVE 2 TO TBL-LENGTH.                                        NC2354.2
060200 IDX-TEST-F2-13.                                                  NC2354.2
060300     SEARCH ALL TH309-ENTRY AT END                                NC2354.2
060400         PERFORM PASS                                             NC2354.2
060500         GO TO IDX-WRITE-F2-13                                    NC2354.2
060600     WHEN DEC-KEY (IDX-1) EQUAL TO "XX"                           NC2354.2
060700         GO TO IDX-FAIL-F2-13.                                    NC2354.2
060800 IDX-DELETE-F2-13.                                                NC2354.2
060900     PERFORM DE-LETE.                                             NC2354.2
061000     GO TO IDX-WRITE-F2-13.                                       NC2354.2
061100 IDX-FAIL-F2-13.                                                  NC2354.2
061200     MOVE "XX" TO COMPUTED-A                                      NC2354.2
061300     MOVE "ENTRY SHOULD NOT BE FOUND " TO RE-MARK                 NC2354.2
061400     PERFORM FAIL.                                                NC2354.2
061500 IDX-WRITE-F2-13.                                                 NC2354.2
061600     PERFORM PRINT-DETAIL.                                        NC2354.2
061700     GO TO CCVS-EXIT.                                             NC2354.2
061800*                                                                 NC2354.2
061900 PUTOUT-COMPUTED-A.                                               NC2354.2
062000     IF TH309-ENTRY (SUB-1) EQUAL TO CORRECT-A                    NC2354.2
062100         MOVE "SEE NOTE 1 FOR DIAGNOSTIC" TO RE-MARK ELSE         NC2354.2
062200     MOVE "SEE NOTE 2 FOR DIAGNOSTIC" TO RE-MARK.                 NC2354.2
062300     MOVE TH309-ENTRY (SUB-1) TO COMPUTED-A.                      NC2354.2
062400 CCVS-EXIT SECTION.                                               NC2354.2
062500 CCVS-999999.                                                     NC2354.2
062600     GO TO CLOSE-FILES.                                           NC2354.2
      *END-OF,NC235A                                                                  
