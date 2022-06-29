      *HEADER,COBOL,NC237A                                                            
000100 IDENTIFICATION DIVISION.                                         NC2374.2
000200 PROGRAM-ID.                                                      NC2374.2
000300     NC237A.                                                      NC2374.2
000400****************************************************************  NC2374.2
000500*                                                              *  NC2374.2
000600*    VALIDATION FOR:-                                          *  NC2374.2
000700*                                                              *  NC2374.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2374.2
000900*                                                              *  NC2374.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC2374.2
001100*                                                              *  NC2374.2
001200****************************************************************  NC2374.2
001300*                                                              *  NC2374.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  NC2374.2
001500*                                                              *  NC2374.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  NC2374.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  NC2374.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  NC2374.2
001900*                                                              *  NC2374.2
002000****************************************************************  NC2374.2
002100                                                                  NC2374.2
002200*                                                                 NC2374.2
002300*    PROGRAM NC237A TESTS FORMAT 2 OF THE "SEARCH" STATEMENT   *  NC2374.2
002400*    WITH A THREE-DIMENSIONAL TABLE CONTAINING ASCENDING AND   *  NC2374.2
002500*    DESCENDING KEYS.                                          *  NC2374.2
002600*                                                              *  NC2374.2
002700****************************************************************  NC2374.2
002800 ENVIRONMENT DIVISION.                                            NC2374.2
002900 CONFIGURATION SECTION.                                           NC2374.2
003000 SOURCE-COMPUTER.                                                 NC2374.2
003100     XXXXX082.                                                    NC2374.2
003200 OBJECT-COMPUTER.                                                 NC2374.2
003300     XXXXX083.                                                    NC2374.2
003400 INPUT-OUTPUT SECTION.                                            NC2374.2
003500 FILE-CONTROL.                                                    NC2374.2
003600     SELECT PRINT-FILE ASSIGN TO                                  NC2374.2
003700     XXXXX055.                                                    NC2374.2
003800 DATA DIVISION.                                                   NC2374.2
003900 FILE SECTION.                                                    NC2374.2
004000 FD  PRINT-FILE.                                                  NC2374.2
004100 01  PRINT-REC PICTURE X(120).                                    NC2374.2
004200 01  DUMMY-RECORD PICTURE X(120).                                 NC2374.2
004300 WORKING-STORAGE SECTION.                                         NC2374.2
004400 77  IDENT-1  PICTURE S9(18) VALUE ZERO  COMPUTATIONAL.           NC2374.2
004500 77  IDENT-2  PICTURE S9(18) VALUE ZERO  COMPUTATIONAL.           NC2374.2
004600 77  IDENT-3  PICTURE S9(18) VALUE ZERO  COMPUTATIONAL.           NC2374.2
004700 77  IDENT-4  PICTURE 9  VALUE ZERO COMPUTATIONAL.                NC2374.2
004800 77  IDENT-5  PICTURE 9  VALUE ZERO COMPUTATIONAL.                NC2374.2
004900 77  IDENT-6  PICTURE 9(18) VALUE 3.                              NC2374.2
005000 77  IDENT-7  PICTURE 9(18) VALUE 1.                              NC2374.2
005100 77  IDENT-8  PICTURE 9   VALUE 6.                                NC2374.2
005200 77  IDENT-9  PICTURE 9   VALUE 5.                                NC2374.2
005300 01  TABLE-TH310.                                                 NC2374.2
005400     02  ENTRY-310 OCCURS 9 TIMES ASCENDING GRP                   NC2374.2
005500         INDEXED BY IDX-1.                                        NC2374.2
005600         03  ENTRY-1.                                             NC2374.2
005700             04  GRP    PIC 99.                                   NC2374.2
005800         03  ENTRY-310-2 OCCURS 9 ASCENDING KEY GRP-1             NC2374.2
005900             DESCENDING KEY IS SEC INDEXED BY IDX-2.              NC2374.2
006000             04  ENTRY-2.                                         NC2374.2
006100                 05  GRP-1  PIC 99.                               NC2374.2
006200                 05  SEC    PIC 99.                               NC2374.2
006300             04  ENTRY-310-3 OCCURS 9 TIMES ASCENDING IS GRP-2    NC2374.2
006400             DESCENDING KEY SEC-1 ASCENDING ELEM INDEXED IDX-3.   NC2374.2
006500                 05  ENTRY-3.                                     NC2374.2
006600                     06  GRP-2   PICTURE 99.                      NC2374.2
006700                     06  SEC-1   PICTURE 99.                      NC2374.2
006800                     06  ELEM    PICTURE 99.                      NC2374.2
006900 01  ENTRIES-X.                                                   NC2374.2
007000     02  ONE-99  PICTURE 99 VALUE 01.                             NC2374.2
007100     02  TWO-99  PICTURE 99  VALUE 09.                            NC2374.2
007200     02  THREE-99  PICTURE 99  VALUE 01.                          NC2374.2
007300 01  CT  PICTURE 999  VALUE 111.                                  NC2374.2
007400 01  SU REDEFINES CT.                                             NC2374.2
007500     02  S1  PICTURE 9.                                           NC2374.2
007600     02  S2  PICTURE 9.                                           NC2374.2
007700     02  S3  PICTURE 9.                                           NC2374.2
007800 01  TEST-RESULTS.                                                NC2374.2
007900     02 FILLER                   PIC X      VALUE SPACE.          NC2374.2
008000     02 FEATURE                  PIC X(20)  VALUE SPACE.          NC2374.2
008100     02 FILLER                   PIC X      VALUE SPACE.          NC2374.2
008200     02 P-OR-F                   PIC X(5)   VALUE SPACE.          NC2374.2
008300     02 FILLER                   PIC X      VALUE SPACE.          NC2374.2
008400     02  PAR-NAME.                                                NC2374.2
008500       03 FILLER                 PIC X(19)  VALUE SPACE.          NC2374.2
008600       03  PARDOT-X              PIC X      VALUE SPACE.          NC2374.2
008700       03 DOTVALUE               PIC 99     VALUE ZERO.           NC2374.2
008800     02 FILLER                   PIC X(8)   VALUE SPACE.          NC2374.2
008900     02 RE-MARK                  PIC X(61).                       NC2374.2
009000 01  TEST-COMPUTED.                                               NC2374.2
009100     02 FILLER                   PIC X(30)  VALUE SPACE.          NC2374.2
009200     02 FILLER                   PIC X(17)  VALUE                 NC2374.2
009300            "       COMPUTED=".                                   NC2374.2
009400     02 COMPUTED-X.                                               NC2374.2
009500     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          NC2374.2
009600     03 COMPUTED-N               REDEFINES COMPUTED-A             NC2374.2
009700                                 PIC -9(9).9(9).                  NC2374.2
009800     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         NC2374.2
009900     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     NC2374.2
010000     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     NC2374.2
010100     03       CM-18V0 REDEFINES COMPUTED-A.                       NC2374.2
010200         04 COMPUTED-18V0                    PIC -9(18).          NC2374.2
010300         04 FILLER                           PIC X.               NC2374.2
010400     03 FILLER PIC X(50) VALUE SPACE.                             NC2374.2
010500 01  TEST-CORRECT.                                                NC2374.2
010600     02 FILLER PIC X(30) VALUE SPACE.                             NC2374.2
010700     02 FILLER PIC X(17) VALUE "       CORRECT =".                NC2374.2
010800     02 CORRECT-X.                                                NC2374.2
010900     03 CORRECT-A                  PIC X(20) VALUE SPACE.         NC2374.2
011000     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      NC2374.2
011100     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         NC2374.2
011200     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     NC2374.2
011300     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     NC2374.2
011400     03      CR-18V0 REDEFINES CORRECT-A.                         NC2374.2
011500         04 CORRECT-18V0                     PIC -9(18).          NC2374.2
011600         04 FILLER                           PIC X.               NC2374.2
011700     03 FILLER PIC X(2) VALUE SPACE.                              NC2374.2
011800     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     NC2374.2
011900 01  CCVS-C-1.                                                    NC2374.2
012000     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PANC2374.2
012100-    "SS  PARAGRAPH-NAME                                          NC2374.2
012200-    "       REMARKS".                                            NC2374.2
012300     02 FILLER                     PIC X(20)    VALUE SPACE.      NC2374.2
012400 01  CCVS-C-2.                                                    NC2374.2
012500     02 FILLER                     PIC X        VALUE SPACE.      NC2374.2
012600     02 FILLER                     PIC X(6)     VALUE "TESTED".   NC2374.2
012700     02 FILLER                     PIC X(15)    VALUE SPACE.      NC2374.2
012800     02 FILLER                     PIC X(4)     VALUE "FAIL".     NC2374.2
012900     02 FILLER                     PIC X(94)    VALUE SPACE.      NC2374.2
013000 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       NC2374.2
013100 01  REC-CT                        PIC 99       VALUE ZERO.       NC2374.2
013200 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       NC2374.2
013300 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       NC2374.2
013400 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       NC2374.2
013500 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       NC2374.2
013600 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       NC2374.2
013700 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       NC2374.2
013800 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      NC2374.2
013900 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       NC2374.2
014000 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     NC2374.2
014100 01  CCVS-H-1.                                                    NC2374.2
014200     02  FILLER                    PIC X(39)    VALUE SPACES.     NC2374.2
014300     02  FILLER                    PIC X(42)    VALUE             NC2374.2
014400     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 NC2374.2
014500     02  FILLER                    PIC X(39)    VALUE SPACES.     NC2374.2
014600 01  CCVS-H-2A.                                                   NC2374.2
014700   02  FILLER                        PIC X(40)  VALUE SPACE.      NC2374.2
014800   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  NC2374.2
014900   02  FILLER                        PIC XXXX   VALUE             NC2374.2
015000     "4.2 ".                                                      NC2374.2
015100   02  FILLER                        PIC X(28)  VALUE             NC2374.2
015200            " COPY - NOT FOR DISTRIBUTION".                       NC2374.2
015300   02  FILLER                        PIC X(41)  VALUE SPACE.      NC2374.2
015400                                                                  NC2374.2
015500 01  CCVS-H-2B.                                                   NC2374.2
015600   02  FILLER                        PIC X(15)  VALUE             NC2374.2
015700            "TEST RESULT OF ".                                    NC2374.2
015800   02  TEST-ID                       PIC X(9).                    NC2374.2
015900   02  FILLER                        PIC X(4)   VALUE             NC2374.2
016000            " IN ".                                               NC2374.2
016100   02  FILLER                        PIC X(12)  VALUE             NC2374.2
016200     " HIGH       ".                                              NC2374.2
016300   02  FILLER                        PIC X(22)  VALUE             NC2374.2
016400            " LEVEL VALIDATION FOR ".                             NC2374.2
016500   02  FILLER                        PIC X(58)  VALUE             NC2374.2
016600     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2374.2
016700 01  CCVS-H-3.                                                    NC2374.2
016800     02  FILLER                      PIC X(34)  VALUE             NC2374.2
016900            " FOR OFFICIAL USE ONLY    ".                         NC2374.2
017000     02  FILLER                      PIC X(58)  VALUE             NC2374.2
017100     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC2374.2
017200     02  FILLER                      PIC X(28)  VALUE             NC2374.2
017300            "  COPYRIGHT   1985 ".                                NC2374.2
017400 01  CCVS-E-1.                                                    NC2374.2
017500     02 FILLER                       PIC X(52)  VALUE SPACE.      NC2374.2
017600     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              NC2374.2
017700     02 ID-AGAIN                     PIC X(9).                    NC2374.2
017800     02 FILLER                       PIC X(45)  VALUE SPACES.     NC2374.2
017900 01  CCVS-E-2.                                                    NC2374.2
018000     02  FILLER                      PIC X(31)  VALUE SPACE.      NC2374.2
018100     02  FILLER                      PIC X(21)  VALUE SPACE.      NC2374.2
018200     02 CCVS-E-2-2.                                               NC2374.2
018300         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      NC2374.2
018400         03 FILLER                   PIC X      VALUE SPACE.      NC2374.2
018500         03 ENDER-DESC               PIC X(44)  VALUE             NC2374.2
018600            "ERRORS ENCOUNTERED".                                 NC2374.2
018700 01  CCVS-E-3.                                                    NC2374.2
018800     02  FILLER                      PIC X(22)  VALUE             NC2374.2
018900            " FOR OFFICIAL USE ONLY".                             NC2374.2
019000     02  FILLER                      PIC X(12)  VALUE SPACE.      NC2374.2
019100     02  FILLER                      PIC X(58)  VALUE             NC2374.2
019200     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2374.2
019300     02  FILLER                      PIC X(13)  VALUE SPACE.      NC2374.2
019400     02 FILLER                       PIC X(15)  VALUE             NC2374.2
019500             " COPYRIGHT 1985".                                   NC2374.2
019600 01  CCVS-E-4.                                                    NC2374.2
019700     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      NC2374.2
019800     02 FILLER                       PIC X(4)   VALUE " OF ".     NC2374.2
019900     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      NC2374.2
020000     02 FILLER                       PIC X(40)  VALUE             NC2374.2
020100      "  TESTS WERE EXECUTED SUCCESSFULLY".                       NC2374.2
020200 01  XXINFO.                                                      NC2374.2
020300     02 FILLER                       PIC X(19)  VALUE             NC2374.2
020400            "*** INFORMATION ***".                                NC2374.2
020500     02 INFO-TEXT.                                                NC2374.2
020600       04 FILLER                     PIC X(8)   VALUE SPACE.      NC2374.2
020700       04 XXCOMPUTED                 PIC X(20).                   NC2374.2
020800       04 FILLER                     PIC X(5)   VALUE SPACE.      NC2374.2
020900       04 XXCORRECT                  PIC X(20).                   NC2374.2
021000     02 INF-ANSI-REFERENCE           PIC X(48).                   NC2374.2
021100 01  HYPHEN-LINE.                                                 NC2374.2
021200     02 FILLER  PIC IS X VALUE IS SPACE.                          NC2374.2
021300     02 FILLER  PIC IS X(65)    VALUE IS "************************NC2374.2
021400-    "*****************************************".                 NC2374.2
021500     02 FILLER  PIC IS X(54)    VALUE IS "************************NC2374.2
021600-    "******************************".                            NC2374.2
021700 01  CCVS-PGM-ID                     PIC X(9)   VALUE             NC2374.2
021800     "NC237A".                                                    NC2374.2
021900 PROCEDURE DIVISION.                                              NC2374.2
022000 CCVS1 SECTION.                                                   NC2374.2
022100 OPEN-FILES.                                                      NC2374.2
022200     OPEN     OUTPUT PRINT-FILE.                                  NC2374.2
022300     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   NC2374.2
022400     MOVE    SPACE TO TEST-RESULTS.                               NC2374.2
022500     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             NC2374.2
022600     GO TO CCVS1-EXIT.                                            NC2374.2
022700 CLOSE-FILES.                                                     NC2374.2
022800     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   NC2374.2
022900 TERMINATE-CCVS.                                                  NC2374.2
023000*    EXIT PROGRAM.                                                NC2374.2
023100*TERMINATE-CALL.                                                  NC2374.2
023200     STOP     RUN.                                                NC2374.2
023300 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         NC2374.2
023400 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           NC2374.2
023500 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          NC2374.2
023600 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      NC2374.2
023700     MOVE "****TEST DELETED****" TO RE-MARK.                      NC2374.2
023800 PRINT-DETAIL.                                                    NC2374.2
023900     IF REC-CT NOT EQUAL TO ZERO                                  NC2374.2
024000             MOVE "." TO PARDOT-X                                 NC2374.2
024100             MOVE REC-CT TO DOTVALUE.                             NC2374.2
024200     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      NC2374.2
024300     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               NC2374.2
024400        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 NC2374.2
024500          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 NC2374.2
024600     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              NC2374.2
024700     MOVE SPACE TO CORRECT-X.                                     NC2374.2
024800     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         NC2374.2
024900     MOVE     SPACE TO RE-MARK.                                   NC2374.2
025000 HEAD-ROUTINE.                                                    NC2374.2
025100     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC2374.2
025200     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC2374.2
025300     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC2374.2
025400     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC2374.2
025500 COLUMN-NAMES-ROUTINE.                                            NC2374.2
025600     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2374.2
025700     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2374.2
025800     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        NC2374.2
025900 END-ROUTINE.                                                     NC2374.2
026000     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.NC2374.2
026100 END-RTN-EXIT.                                                    NC2374.2
026200     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2374.2
026300 END-ROUTINE-1.                                                   NC2374.2
026400      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      NC2374.2
026500      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               NC2374.2
026600      ADD PASS-COUNTER TO ERROR-HOLD.                             NC2374.2
026700*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   NC2374.2
026800      MOVE PASS-COUNTER TO CCVS-E-4-1.                            NC2374.2
026900      MOVE ERROR-HOLD TO CCVS-E-4-2.                              NC2374.2
027000      MOVE CCVS-E-4 TO CCVS-E-2-2.                                NC2374.2
027100      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           NC2374.2
027200  END-ROUTINE-12.                                                 NC2374.2
027300      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        NC2374.2
027400     IF       ERROR-COUNTER IS EQUAL TO ZERO                      NC2374.2
027500         MOVE "NO " TO ERROR-TOTAL                                NC2374.2
027600         ELSE                                                     NC2374.2
027700         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       NC2374.2
027800     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           NC2374.2
027900     PERFORM WRITE-LINE.                                          NC2374.2
028000 END-ROUTINE-13.                                                  NC2374.2
028100     IF DELETE-COUNTER IS EQUAL TO ZERO                           NC2374.2
028200         MOVE "NO " TO ERROR-TOTAL  ELSE                          NC2374.2
028300         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      NC2374.2
028400     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   NC2374.2
028500     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2374.2
028600      IF   INSPECT-COUNTER EQUAL TO ZERO                          NC2374.2
028700          MOVE "NO " TO ERROR-TOTAL                               NC2374.2
028800      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   NC2374.2
028900      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            NC2374.2
029000      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          NC2374.2
029100     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2374.2
029200 WRITE-LINE.                                                      NC2374.2
029300     ADD 1 TO RECORD-COUNT.                                       NC2374.2
029400*    IF RECORD-COUNT GREATER 50                                   NC2374.2
029500*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          NC2374.2
029600*        MOVE SPACE TO DUMMY-RECORD                               NC2374.2
029700*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  NC2374.2
029800*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             NC2374.2
029900*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     NC2374.2
030000*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          NC2374.2
030100*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          NC2374.2
030200*        MOVE ZERO TO RECORD-COUNT.                               NC2374.2
030300     PERFORM WRT-LN.                                              NC2374.2
030400 WRT-LN.                                                          NC2374.2
030500     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               NC2374.2
030600     MOVE SPACE TO DUMMY-RECORD.                                  NC2374.2
030700 BLANK-LINE-PRINT.                                                NC2374.2
030800     PERFORM WRT-LN.                                              NC2374.2
030900 FAIL-ROUTINE.                                                    NC2374.2
031000     IF   COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE. NC2374.2
031100     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.NC2374.2
031200     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC2374.2
031300     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   NC2374.2
031400     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2374.2
031500     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC2374.2
031600     GO TO  FAIL-ROUTINE-EX.                                      NC2374.2
031700 FAIL-ROUTINE-WRITE.                                              NC2374.2
031800     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         NC2374.2
031900     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 NC2374.2
032000     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. NC2374.2
032100     MOVE   SPACES TO COR-ANSI-REFERENCE.                         NC2374.2
032200 FAIL-ROUTINE-EX. EXIT.                                           NC2374.2
032300 BAIL-OUT.                                                        NC2374.2
032400     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   NC2374.2
032500     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           NC2374.2
032600 BAIL-OUT-WRITE.                                                  NC2374.2
032700     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  NC2374.2
032800     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC2374.2
032900     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2374.2
033000     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC2374.2
033100 BAIL-OUT-EX. EXIT.                                               NC2374.2
033200 CCVS1-EXIT.                                                      NC2374.2
033300     EXIT.                                                        NC2374.2
033400 SECT-NC237A-001 SECTION.                                         NC2374.2
033500 TH-10-001.                                                       NC2374.2
033600*                                                                 NC2374.2
033700 BUILD-3DEM-TABLE.                                                NC2374.2
033800     MOVE "PERFORM VARYING" TO FEATURE.                           NC2374.2
033900     SET IDX-1, IDX-2, IDX-3 TO 1.                                NC2374.2
034000     PERFORM BUILD-TABLE THRU BUILD-EXIT VARYING ONE-99 FROM 1    NC2374.2
034100         BY 1 UNTIL ONE-99 EQUAL TO 10 AFTER TWO-99 FROM 9 BY -1  NC2374.2
034200         UNTIL TWO-99 EQUAL TO 0 AFTER THREE-99 FROM 1 BY 1       NC2374.2
034300         UNTIL THREE-99 EQUAL TO 10.                              NC2374.2
034400     GO TO IDX-INIT-GF-1.                                         NC2374.2
034500*                                                                 NC2374.2
034600 BUILD-TABLE.                                                     NC2374.2
034700     MOVE ONE-99 TO GRP (IDX-1), GRP-1 (IDX-1, IDX-2),            NC2374.2
034800         GRP-2 (IDX-1, IDX-2, IDX-3).                             NC2374.2
034900     MOVE TWO-99 TO SEC (IDX-1, IDX-2) SEC-1 (IDX-1, IDX-2, IDX-3)NC2374.2
035000     MOVE THREE-99 TO ELEM (IDX-1, IDX-2, IDX-3).                 NC2374.2
035100     IF CT = 999                                                  NC2374.2
035200          MOVE 0 TO CT                                            NC2374.2
035300     ELSE                                                         NC2374.2
035400          ADD 1 TO CT.                                            NC2374.2
035500     IF S3 EQUAL TO 0 ADD 1 TO S3.                                NC2374.2
035600     IF S2 EQUAL TO 0 ADD 1 TO S2.                                NC2374.2
035700     IF S1 EQUAL TO 0 ADD 1 TO S1.                                NC2374.2
035800     SET IDX-1 TO S1.                                             NC2374.2
035900     SET IDX-2 TO S2.                                             NC2374.2
036000     SET IDX-3 TO S3.                                             NC2374.2
036100 BUILD-EXIT.                                                      NC2374.2
036200     EXIT.                                                        NC2374.2
036300*                                                                 NC2374.2
036400 IDX-INIT-GF-1.                                                   NC2374.2
036500     MOVE "IDX-TEST-GF-1" TO PAR-NAME.                            NC2374.2
036600     MOVE "VI-121 6.21.2" TO ANSI-REFERENCE.                      NC2374.2
036700     MOVE "SET AND SEARCH-ALL" TO FEATURE.                        NC2374.2
036800 IDX-TEST-GF-1.                                                   NC2374.2
036900     IF ENTRY-3 (9, 9, 9) EQUAL TO 090109                         NC2374.2
037000         PERFORM PASS                                             NC2374.2
037100         MOVE "TABLE BUILT CORRECTLY" TO RE-MARK                  NC2374.2
037200         GO TO IDX-WRITE-GF-1.                                    NC2374.2
037300     GO TO IDX-FAIL-GF-1.                                         NC2374.2
037400 IDX-DELETE-GF-1.                                                 NC2374.2
037500     PERFORM DE-LETE.                                             NC2374.2
037600     GO TO IDX-WRITE-GF-1.                                        NC2374.2
037700 IDX-FAIL-GF-1.                                                   NC2374.2
037800     MOVE "TABLE CREATED INCORRECTLY" TO RE-MARK.                 NC2374.2
037900     PERFORM FAIL.                                                NC2374.2
038000 IDX-WRITE-GF-1.                                                  NC2374.2
038100     PERFORM PRINT-DETAIL.                                        NC2374.2
038200*                                                                 NC2374.2
038300 IDX-INIT-GF-2.                                                   NC2374.2
038400     MOVE "IDX-TEST-GF-2 " TO PAR-NAME.                           NC2374.2
038500     MOVE "VI-121 6.21.2" TO ANSI-REFERENCE.                      NC2374.2
038600     MOVE "SET AND SEARCH-ALL" TO FEATURE.                        NC2374.2
038700     MOVE 7 TO IDENT-1.                                           NC2374.2
038800     SET IDX-1  IDX-2  IDX-3 TO IDENT-1.                          NC2374.2
038900     SET IDX-1  IDX-3 DOWN BY IDENT-6.                            NC2374.2
039000 IDX-TEST-GF-2.                                                   NC2374.2
039100     IF ENTRY-3 (IDX-1, IDX-2, IDX-3) EQUAL TO 040304             NC2374.2
039200         PERFORM PASS                                             NC2374.2
039300         GO TO IDX-WRITE-GF-2.                                    NC2374.2
039400     GO TO IDX-FAIL-GF-2.                                         NC2374.2
039500 IDX-DELETE-GF-2.                                                 NC2374.2
039600     PERFORM DE-LETE.                                             NC2374.2
039700     GO TO IDX-WRITE-GF-2.                                        NC2374.2
039800 IDX-FAIL-GF-2.                                                   NC2374.2
039900     MOVE "040304" TO CORRECT-A.                                  NC2374.2
040000     MOVE ENTRY-3 (IDX-1, IDX-2, IDX-3) TO COMPUTED-A.            NC2374.2
040100     PERFORM FAIL.                                                NC2374.2
040200 IDX-WRITE-GF-2.                                                  NC2374.2
040300     PERFORM PRINT-DETAIL.                                        NC2374.2
040400*                                                                 NC2374.2
040500 IDX-INIT-GF-3.                                                   NC2374.2
040600     MOVE "IDX-TEST-GF-3" TO PAR-NAME.                            NC2374.2
040700     MOVE "VI-121 6.21.2" TO ANSI-REFERENCE.                      NC2374.2
040800     MOVE "SET AND SEARCH-ALL" TO FEATURE.                        NC2374.2
040900     MOVE 4 TO IDENT-1.                                           NC2374.2
041000     SET IDX-1 TO IDENT-7.                                        NC2374.2
041100     SET IDX-1 UP BY IDENT-1.                                     NC2374.2
041200 IDX-TEST-GF-3.                                                   NC2374.2
041300     IF ENTRY-1 (IDX-1) EQUAL TO "05"                             NC2374.2
041400         PERFORM PASS                                             NC2374.2
041500         GO TO IDX-WRITE-GF-3.                                    NC2374.2
041600     GO TO IDX-FAIL-GF-3.                                         NC2374.2
041700 IDX-DELETE-GF-3.                                                 NC2374.2
041800     PERFORM DE-LETE.                                             NC2374.2
041900     GO TO IDX-WRITE-GF-3.                                        NC2374.2
042000 IDX-FAIL-GF-3.                                                   NC2374.2
042100     MOVE ENTRY-1 (IDX-1) TO COMPUTED-A.                          NC2374.2
042200     MOVE "05" TO CORRECT-A.                                      NC2374.2
042300     PERFORM FAIL.                                                NC2374.2
042400 IDX-WRITE-GF-3.                                                  NC2374.2
042500     PERFORM PRINT-DETAIL.                                        NC2374.2
042600*                                                                 NC2374.2
042700 IDX-INIT-GF-4.                                                   NC2374.2
042800     MOVE "IDX-TEST-GF-4" TO PAR-NAME.                            NC2374.2
042900     MOVE "VI-121 6.21.2" TO ANSI-REFERENCE.                      NC2374.2
043000     MOVE "SET AND SEARCH-ALL" TO FEATURE.                        NC2374.2
043100     MOVE 1 TO IDENT-1.                                           NC2374.2
043200     SET IDX-1  IDX-2 TO IDENT-1.                                 NC2374.2
043300     SET IDX-2 UP BY IDENT-9.                                     NC2374.2
043400     SET IDX-2 UP BY IDENT-6.                                     NC2374.2
043500 IDX-TEST-GF-4.                                                   NC2374.2
043600     IF ENTRY-2 (IDX-1, IDX-2) EQUAL TO "0101"                    NC2374.2
043700         PERFORM PASS                                             NC2374.2
043800         GO TO IDX-WRITE-GF-4.                                    NC2374.2
043900     GO TO IDX-FAIL-GF-4.                                         NC2374.2
044000 IDX-DELETE-GF-4.                                                 NC2374.2
044100     PERFORM DE-LETE.                                             NC2374.2
044200     GO TO IDX-WRITE-GF-4.                                        NC2374.2
044300 IDX-FAIL-GF-4.                                                   NC2374.2
044400     MOVE "0101" TO CORRECT-A.                                    NC2374.2
044500     MOVE ENTRY-2 (IDX-1, IDX-2) TO COMPUTED-A.                   NC2374.2
044600     PERFORM FAIL.                                                NC2374.2
044700 IDX-WRITE-GF-4.                                                  NC2374.2
044800     PERFORM PRINT-DETAIL.                                        NC2374.2
044900*                                                                 NC2374.2
045000 IDX-INIT-GF-5.                                                   NC2374.2
045100     MOVE "VI-121 6.21.2" TO ANSI-REFERENCE.                      NC2374.2
045200     MOVE "SET AND SEARCH-ALL" TO FEATURE.                        NC2374.2
045300     MOVE "IDX-TEST-GF-5" TO PAR-NAME.                            NC2374.2
045400     MOVE 1 TO IDENT-1.                                           NC2374.2
045500     SET IDX-1 TO IDENT-6.                                        NC2374.2
045600     SET IDX-1 UP BY IDENT-1.                                     NC2374.2
045700 IDX-TEST-GF-5.                                                   NC2374.2
045800     IF ENTRY-1 (IDX-1) EQUAL TO 04                               NC2374.2
045900         PERFORM PASS                                             NC2374.2
046000         GO TO IDX-WRITE-GF-5.                                    NC2374.2
046100     GO TO IDX-FAIL-GF-5.                                         NC2374.2
046200 IDX-DELETE-GF-5.                                                 NC2374.2
046300     PERFORM DE-LETE.                                             NC2374.2
046400     GO TO IDX-WRITE-GF-5.                                        NC2374.2
046500 IDX-FAIL-GF-5.                                                   NC2374.2
046600     MOVE "04" TO CORRECT-A.                                      NC2374.2
046700     MOVE ENTRY-1 (IDX-1) TO COMPUTED-A.                          NC2374.2
046800     PERFORM FAIL.                                                NC2374.2
046900 IDX-WRITE-GF-5.                                                  NC2374.2
047000     PERFORM PRINT-DETAIL.                                        NC2374.2
047100*                                                                 NC2374.2
047200 IDX-INIT-GF-6.                                                   NC2374.2
047300     MOVE "IDX-TEST-GF-6" TO PAR-NAME.                            NC2374.2
047400     MOVE "VI-121 6.21.2" TO ANSI-REFERENCE.                      NC2374.2
047500     MOVE "SET AND SEARCH-ALL" TO FEATURE.                        NC2374.2
047600     SET IDX-3 TO 4.                                              NC2374.2
047700     SET IDX-1  IDX-2 TO IDX-3.                                   NC2374.2
047800     SET IDX-1  IDX-2  IDX-3 DOWN BY IDENT-7.                     NC2374.2
047900     MOVE 4 TO IDENT-1.                                           NC2374.2
048000     SET IDX-1  IDX-2  IDX-3 UP BY IDENT-1.                       NC2374.2
048100 IDX-TEST-GF-6.                                                   NC2374.2
048200     IF ENTRY-3 (IDX-1, IDX-2, IDX-3) EQUAL TO "070307"           NC2374.2
048300         PERFORM PASS                                             NC2374.2
048400         GO TO IDX-WRITE-GF-6.                                    NC2374.2
048500     GO TO IDX-FAIL-GF-6.                                         NC2374.2
048600 IDX-DELETE-GF-6.                                                 NC2374.2
048700     PERFORM DE-LETE.                                             NC2374.2
048800     GO TO IDX-WRITE-GF-6.                                        NC2374.2
048900 IDX-FAIL-GF-6.                                                   NC2374.2
049000     MOVE ENTRY-3 (IDX-1, IDX-2, IDX-3) TO COMPUTED-A.            NC2374.2
049100     MOVE "070307" TO CORRECT-A.                                  NC2374.2
049200     PERFORM FAIL.                                                NC2374.2
049300 IDX-WRITE-GF-6.                                                  NC2374.2
049400     PERFORM PRINT-DETAIL.                                        NC2374.2
049500*                                                                 NC2374.2
049600 IDX-INIT-GF-7.                                                   NC2374.2
049700     MOVE "IDX-TEST-GF-7" TO PAR-NAME.                            NC2374.2
049800     MOVE "VI-121 6.21.2" TO ANSI-REFERENCE.                      NC2374.2
049900     MOVE "SET AND SEARCH-ALL" TO FEATURE.                        NC2374.2
050000     MOVE 3 TO IDENT-1  IDENT-2  IDENT-4.                         NC2374.2
050100     SET IDX-1 TO IDENT-1.                                        NC2374.2
050200     SET IDX-2 TO IDENT-2.                                        NC2374.2
050300     SET IDX-3 TO IDENT-4.                                        NC2374.2
050400     SET IDX-1  IDX-2  IDX-3 UP BY IDENT-7.                       NC2374.2
050500 IDX-TEST-GF-7.                                                   NC2374.2
050600     IF ENTRY-3 (IDX-1, IDX-2, IDX-3) EQUAL TO "040604"           NC2374.2
050700         PERFORM PASS                                             NC2374.2
050800         GO TO IDX-WRITE-GF-7.                                    NC2374.2
050900     GO TO IDX-FAIL-GF-7.                                         NC2374.2
051000 IDX-DELETE-GF-7.                                                 NC2374.2
051100     PERFORM DE-LETE.                                             NC2374.2
051200     GO TO IDX-WRITE-GF-7.                                        NC2374.2
051300 IDX-FAIL-GF-7.                                                   NC2374.2
051400     MOVE "040604" TO CORRECT-A.                                  NC2374.2
051500     MOVE ENTRY-3 (IDX-1, IDX-2, IDX-3) TO COMPUTED-A.            NC2374.2
051600     PERFORM FAIL.                                                NC2374.2
051700 IDX-WRITE-GF-7.                                                  NC2374.2
051800     PERFORM PRINT-DETAIL.                                        NC2374.2
051900*                                                                 NC2374.2
052000 IDX-INIT-GF-8.                                                   NC2374.2
052100     MOVE "IDX-TEST-GF-8" TO PAR-NAME.                            NC2374.2
052200     MOVE "VI-121 6.21.2" TO ANSI-REFERENCE.                      NC2374.2
052300     MOVE "SET AND SEARCH-ALL" TO FEATURE.                        NC2374.2
052400     MOVE 9 TO IDENT-1.                                           NC2374.2
052500     SET IDX-1, IDX-2, IDX-3 TO IDENT-1.                          NC2374.2
052600     SET IDX-1, IDX-2, IDX-3 DOWN BY 5.                           NC2374.2
052700 IDX-TEST-GF-8.                                                   NC2374.2
052800     IF ENTRY-3 (IDX-1, IDX-2, IDX-3) EQUAL TO "040604"           NC2374.2
052900         PERFORM PASS                                             NC2374.2
053000         GO TO IDX-WRITE-GF-8.                                    NC2374.2
053100     GO TO IDX-FAIL-GF-8.                                         NC2374.2
053200 IDX-DELETE-GF-8.                                                 NC2374.2
053300     PERFORM DE-LETE.                                             NC2374.2
053400     GO TO IDX-WRITE-GF-8.                                        NC2374.2
053500 IDX-FAIL-GF-8.                                                   NC2374.2
053600     MOVE "040604" TO CORRECT-A.                                  NC2374.2
053700     MOVE ENTRY-3 (IDX-1, IDX-2, IDX-3) TO COMPUTED-A.            NC2374.2
053800     PERFORM FAIL.                                                NC2374.2
053900 IDX-WRITE-GF-8.                                                  NC2374.2
054000     PERFORM PRINT-DETAIL.                                        NC2374.2
054100*                                                                 NC2374.2
054200 IDX-INIT-F2-9.                                                   NC2374.2
054300     MOVE "IDX-TEST-F2-9" TO PAR-NAME.                            NC2374.2
054400     MOVE "VI-121 6.21.2" TO ANSI-REFERENCE.                      NC2374.2
054500     MOVE "SET AND SEARCH-ALL" TO FEATURE.                        NC2374.2
054600     SET IDX-1 TO 05.                                             NC2374.2
054700 IDX-TEST-F2-9.                                                   NC2374.2
054800     SEARCH ALL ENTRY-310-2 END                                   NC2374.2
054900         GO TO IDX-FAIL-F2-9                                      NC2374.2
055000     WHEN GRP-1 (IDX-1, IDX-2) EQUAL TO "05" AND                  NC2374.2
055100         SEC (IDX-1, IDX-2) EQUAL TO "07"                         NC2374.2
055200         PERFORM PASS                                             NC2374.2
055300         GO TO IDX-WRITE-F2-9.                                    NC2374.2
055400 IDX-DELETE-F2-9.                                                 NC2374.2
055500     PERFORM DE-LETE.                                             NC2374.2
055600     GO TO IDX-WRITE-F2-9.                                        NC2374.2
055700 IDX-FAIL-F2-9.                                                   NC2374.2
055800     MOVE ENTRY-2 (05, 03) TO COMPUTED-A                          NC2374.2
055900     MOVE "0507" TO CORRECT-A                                     NC2374.2
056000     MOVE "SUBSCRIPT USED FOR COMPUTED" TO RE-MARK                NC2374.2
056100     PERFORM FAIL.                                                NC2374.2
056200 IDX-WRITE-F2-9.                                                  NC2374.2
056300     PERFORM PRINT-DETAIL.                                        NC2374.2
056400*                                                                 NC2374.2
056500 IDX-INIT-F2-10.                                                  NC2374.2
056600     MOVE "IDX-TEST-F2-10" TO PAR-NAME.                           NC2374.2
056700     MOVE "VI-121 6.21.2" TO ANSI-REFERENCE.                      NC2374.2
056800     MOVE "SET AND SEARCH-ALL" TO FEATURE.                        NC2374.2
056900     SET     IDX-1  IDX-2 TO 9.                                   NC2374.2
057000 IDX-TEST-F2-10.                                                  NC2374.2
057100     SEARCH  ALL ENTRY-310-3 END                                  NC2374.2
057200             PERFORM PASS                                         NC2374.2
057300             GO TO   IDX-WRITE-F2-10                              NC2374.2
057400             WHEN    GRP-2 (IDX-1, IDX-2, IDX-3) EQUAL "09"       NC2374.2
057500                 AND SEC-1 (IDX-1, IDX-2, IDX-3) EQUAL "01"       NC2374.2
057600                 AND ELEM  (IDX-1, IDX-2, IDX-3) EQUAL "10"       NC2374.2
057700                     GO TO   IDX-FAIL-F2-10.                      NC2374.2
057800 IDX-DELETE-F2-10.                                                NC2374.2
057900     PERFORM DE-LETE.                                             NC2374.2
058000     GO TO IDX-WRITE-F2-10.                                       NC2374.2
058100 IDX-FAIL-F2-10.                                                  NC2374.2
058200     MOVE  "090110" TO COMPUTED-A                                 NC2374.2
058300     MOVE  "ENTRY SHOULD NOT BE FOUND" TO RE-MARK                 NC2374.2
058400     PERFORM FAIL.                                                NC2374.2
058500 IDX-WRITE-F2-10.                                                 NC2374.2
058600     PERFORM PRINT-DETAIL.                                        NC2374.2
058700*                                                                 NC2374.2
058800 IDX-INIT-F2-11.                                                  NC2374.2
058900     MOVE "IDX-TEST-F2-11" TO PAR-NAME.                           NC2374.2
059000     MOVE "VI-121 6.21.2" TO ANSI-REFERENCE.                      NC2374.2
059100     MOVE "SET AND SEARCH-ALL" TO FEATURE.                        NC2374.2
059200     SET IDX-1 TO 09.                                             NC2374.2
059300 IDX-TEST-F2-11.                                                  NC2374.2
059400     SEARCH ALL ENTRY-310                                      ENDNC2374.2
059500         GO TO IDX-FAIL-F2-11                                     NC2374.2
059600     WHEN GRP (IDX-1) EQUAL TO "07"                               NC2374.2
059700         PERFORM PASS                                             NC2374.2
059800         GO TO IDX-WRITE-F2-11.                                   NC2374.2
059900 IDX-DELETE-F2-11.                                                NC2374.2
060000     PERFORM DE-LETE.                                             NC2374.2
060100     GO TO IDX-WRITE-F2-11.                                       NC2374.2
060200 IDX-FAIL-F2-11.                                                  NC2374.2
060300     MOVE ENTRY-1 (07) TO COMPUTED-A                              NC2374.2
060400     MOVE "07" TO CORRECT-A                                       NC2374.2
060500     PERFORM FAIL                                                 NC2374.2
060600     MOVE "SUBSCRIPT USED FOR COMPUTED" TO RE-MARK.               NC2374.2
060700 IDX-WRITE-F2-11.                                                 NC2374.2
060800     PERFORM PRINT-DETAIL.                                        NC2374.2
060900*                                                                 NC2374.2
061000 IDX-INIT-F2-12.                                                  NC2374.2
061100     MOVE "IDX-TEST-F2-12" TO PAR-NAME.                           NC2374.2
061200     MOVE "VI-121 6.21.2" TO ANSI-REFERENCE.                      NC2374.2
061300     MOVE "SET AND SEARCH-ALL" TO FEATURE.                        NC2374.2
061400     MOVE 04 TO IDENT-1.                                          NC2374.2
061500     SET IDX-1, IDX-2, IDX-3 TO IDENT-1.                          NC2374.2
061600     SET IDX-1 UP BY 1.                                           NC2374.2
061700 IDX-TEST-F2-12.                                                  NC2374.2
061800     SEARCH ALL ENTRY-310-3 AT END                                NC2374.2
061900         GO TO IDX-FAIL-F2-12                                     NC2374.2
062000     WHEN GRP-2 (IDX-1, IDX-2, IDX-3) EQUAL TO "05" AND           NC2374.2
062100         SEC-1 (IDX-1, IDX-2, IDX-3) EQUAL TO "06" AND            NC2374.2
062200             ELEM (IDX-1, IDX-2, IDX-3) EQUAL TO "03"             NC2374.2
062300         PERFORM PASS                                             NC2374.2
062400         GO TO IDX-WRITE-F2-12.                                   NC2374.2
062500 IDX-DELETE-F2-12.                                                NC2374.2
062600     PERFORM DE-LETE.                                             NC2374.2
062700     GO TO IDX-WRITE-F2-12.                                       NC2374.2
062800 IDX-FAIL-F2-12.                                                  NC2374.2
062900     MOVE ENTRY-3 (05, 04, 03) TO COMPUTED-A                      NC2374.2
063000     MOVE "SUBSCRIPT USED FOR COMPUTED" TO RE-MARK                NC2374.2
063100     MOVE "050603" TO CORRECT-A                                   NC2374.2
063200     PERFORM FAIL.                                                NC2374.2
063300 IDX-WRITE-F2-12.                                                 NC2374.2
063400     PERFORM PRINT-DETAIL.                                        NC2374.2
063500*                                                                 NC2374.2
063600 IDX-INIT-F2-13.                                                  NC2374.2
063700     MOVE "IDX-TEST-F2-13" TO PAR-NAME.                           NC2374.2
063800     MOVE "VI-121 6.21.2" TO ANSI-REFERENCE.                      NC2374.2
063900     MOVE "SET AND SEARCH-ALL" TO FEATURE.                        NC2374.2
064000     SET IDX-1 TO 9.                                              NC2374.2
064100 IDX-TEST-F2-13.                                                  NC2374.2
064200     SEARCH ALL ENTRY-310-2 AT END                                NC2374.2
064300         GO TO IDX-FAIL-F2-13                                     NC2374.2
064400     WHEN GRP-1 (IDX-1, IDX-2) EQUAL TO "09" AND SEC              NC2374.2
064500         (IDX-1, IDX-2) EQUAL TO "01"                             NC2374.2
064600         PERFORM PASS                                             NC2374.2
064700         GO TO IDX-WRITE-F2-13.                                   NC2374.2
064800 IDX-DELETE-F2-13.                                                NC2374.2
064900     PERFORM DE-LETE.                                             NC2374.2
065000     GO TO IDX-WRITE-F2-13.                                       NC2374.2
065100 IDX-FAIL-F2-13.                                                  NC2374.2
065200     MOVE ENTRY-2 (09, 09) TO COMPUTED-A                          NC2374.2
065300     MOVE "SUBSCRIPT USED FOR COMPUTED" TO RE-MARK                NC2374.2
065400     MOVE "0901" TO CORRECT-A                                     NC2374.2
065500     PERFORM FAIL.                                                NC2374.2
065600 IDX-WRITE-F2-13.                                                 NC2374.2
065700     PERFORM PRINT-DETAIL.                                        NC2374.2
065800*                                                                 NC2374.2
065900 CCVS-EXIT SECTION.                                               NC2374.2
066000 CCVS-999999.                                                     NC2374.2
066100     GO TO CLOSE-FILES.                                           NC2374.2
      *END-OF,NC237A                                                                  
