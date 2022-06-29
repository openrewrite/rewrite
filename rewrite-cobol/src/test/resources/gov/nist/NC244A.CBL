      *HEADER,COBOL,NC244A                                                            
000100 IDENTIFICATION DIVISION.                                         NC2444.2
000200 PROGRAM-ID.                                                      NC2444.2
000300     NC244A.                                                      NC2444.2
000400****************************************************************  NC2444.2
000500*                                                              *  NC2444.2
000600*    VALIDATION FOR:-                                          *  NC2444.2
000700*                                                              *  NC2444.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2444.2
000900*                                                              *  NC2444.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC2444.2
001100*                                                              *  NC2444.2
001200****************************************************************  NC2444.2
001300*                                                              *  NC2444.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  NC2444.2
001500*                                                              *  NC2444.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  NC2444.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  NC2444.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  NC2444.2
001900*                                                              *  NC2444.2
002000****************************************************************  NC2444.2
002100*                                                              *  NC2444.2
002200*    PROGRAM NCC244A TESTS THE CONSTRUCTION  AND ACCESS OF A   *  NC2444.2
002300*    TWO-DIMENSIONAL TABLE WHICH HAS MULTIPLE INDICES.         *  NC2444.2
002400*    RELATIVE INDEXING AND FORMATS 1 AND 2 OF THE "SET"        *  NC2444.2
002500*    STATEMENT ARE USED.                                       *  NC2444.2
002600*                                                              *  NC2444.2
002700****************************************************************  NC2444.2
002800 ENVIRONMENT DIVISION.                                            NC2444.2
002900 CONFIGURATION SECTION.                                           NC2444.2
003000 SOURCE-COMPUTER.                                                 NC2444.2
003100     XXXXX082.                                                    NC2444.2
003200 OBJECT-COMPUTER.                                                 NC2444.2
003300     XXXXX083.                                                    NC2444.2
003400 INPUT-OUTPUT SECTION.                                            NC2444.2
003500 FILE-CONTROL.                                                    NC2444.2
003600     SELECT PRINT-FILE ASSIGN TO                                  NC2444.2
003700     XXXXX055.                                                    NC2444.2
003800 DATA DIVISION.                                                   NC2444.2
003900 FILE SECTION.                                                    NC2444.2
004000 FD  PRINT-FILE.                                                  NC2444.2
004100 01  PRINT-REC PICTURE X(120).                                    NC2444.2
004200 01  DUMMY-RECORD PICTURE X(120).                                 NC2444.2
004300 WORKING-STORAGE SECTION.                                         NC2444.2
004400 77  SUB-COMP1    PICTURE S9     VALUE 3    COMPUTATIONAL.        NC2444.2
004500 77  SUB-COMP2    PICTURE S9(10) VALUE 1    COMPUTATIONAL.        NC2444.2
004600 77  SUB-COMP3    PICTURE S9(18) VALUE 49   COMPUTATIONAL.        NC2444.2
004700 77  SUB-COMP4    PICTURE 9      VALUE 3    COMPUTATIONAL.        NC2444.2
004800 77  SUB-COMP5    PICTURE 9(10)  VALUE 1    COMPUTATIONAL.        NC2444.2
004900 77  SUB-COMP6    PICTURE 9(18)  VALUE 9    COMPUTATIONAL.        NC2444.2
005000 77  SUB-7        PICTURE 99  VALUE 20.                           NC2444.2
005100 77  SUB-8        PICTURE 99 VALUE 01.                            NC2444.2
005200 77  SUB-9        PICTURE 99  VALUE 10.                           NC2444.2
005300 77  IN-SERT     PICTURE AA  VALUE "AA".                          NC2444.2
005400 77  ENTRY-HOLD  PICTURE XX  VALUE SPACES.                        NC2444.2
005500 01  IDX-HOLD.                                                    NC2444.2
005600     02  IDX-3HOLD PICTURE 9(6)  VALUE 0.                         NC2444.2
005700     02  FILLER    PICTURE X(8)  VALUE SPACES.                    NC2444.2
005800     02  IDX-14HOLD PICTURE 9(6) VALUE 0.                         NC2444.2
005900 01  TWO-DIMENSION-TABLE.                                         NC2444.2
006000     02  GRP-ENTRY OCCURS 50 INDEXED IDX-1 IDX-2 IDX-3 IDX-4      NC2444.2
006100         IDX-5.                                                   NC2444.2
006200         03  ENTRY-1  PICTURE 99.                                 NC2444.2
006300         03  ELEM-ENTRY OCCURS 10 TIMES INDEXED BY IDX-6 IDX-7    NC2444.2
006400             IDX-8 IDX-9 IDX-10 IDX-11 IDX-12 IDX-13 IDX-14       NC2444.2
006500             IDX-15.                                              NC2444.2
006600             04  ENTRY-2 PICTURE XX.                              NC2444.2
006700 01  TEST-RESULTS.                                                NC2444.2
006800     02 FILLER                   PIC X      VALUE SPACE.          NC2444.2
006900     02 FEATURE                  PIC X(20)  VALUE SPACE.          NC2444.2
007000     02 FILLER                   PIC X      VALUE SPACE.          NC2444.2
007100     02 P-OR-F                   PIC X(5)   VALUE SPACE.          NC2444.2
007200     02 FILLER                   PIC X      VALUE SPACE.          NC2444.2
007300     02  PAR-NAME.                                                NC2444.2
007400       03 FILLER                 PIC X(19)  VALUE SPACE.          NC2444.2
007500       03  PARDOT-X              PIC X      VALUE SPACE.          NC2444.2
007600       03 DOTVALUE               PIC 99     VALUE ZERO.           NC2444.2
007700     02 FILLER                   PIC X(8)   VALUE SPACE.          NC2444.2
007800     02 RE-MARK                  PIC X(61).                       NC2444.2
007900 01  TEST-COMPUTED.                                               NC2444.2
008000     02 FILLER                   PIC X(30)  VALUE SPACE.          NC2444.2
008100     02 FILLER                   PIC X(17)  VALUE                 NC2444.2
008200            "       COMPUTED=".                                   NC2444.2
008300     02 COMPUTED-X.                                               NC2444.2
008400     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          NC2444.2
008500     03 COMPUTED-N               REDEFINES COMPUTED-A             NC2444.2
008600                                 PIC -9(9).9(9).                  NC2444.2
008700     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         NC2444.2
008800     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     NC2444.2
008900     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     NC2444.2
009000     03       CM-18V0 REDEFINES COMPUTED-A.                       NC2444.2
009100         04 COMPUTED-18V0                    PIC -9(18).          NC2444.2
009200         04 FILLER                           PIC X.               NC2444.2
009300     03 FILLER PIC X(50) VALUE SPACE.                             NC2444.2
009400 01  TEST-CORRECT.                                                NC2444.2
009500     02 FILLER PIC X(30) VALUE SPACE.                             NC2444.2
009600     02 FILLER PIC X(17) VALUE "       CORRECT =".                NC2444.2
009700     02 CORRECT-X.                                                NC2444.2
009800     03 CORRECT-A                  PIC X(20) VALUE SPACE.         NC2444.2
009900     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      NC2444.2
010000     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         NC2444.2
010100     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     NC2444.2
010200     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     NC2444.2
010300     03      CR-18V0 REDEFINES CORRECT-A.                         NC2444.2
010400         04 CORRECT-18V0                     PIC -9(18).          NC2444.2
010500         04 FILLER                           PIC X.               NC2444.2
010600     03 FILLER PIC X(2) VALUE SPACE.                              NC2444.2
010700     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     NC2444.2
010800 01  CCVS-C-1.                                                    NC2444.2
010900     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PANC2444.2
011000-    "SS  PARAGRAPH-NAME                                          NC2444.2
011100-    "       REMARKS".                                            NC2444.2
011200     02 FILLER                     PIC X(20)    VALUE SPACE.      NC2444.2
011300 01  CCVS-C-2.                                                    NC2444.2
011400     02 FILLER                     PIC X        VALUE SPACE.      NC2444.2
011500     02 FILLER                     PIC X(6)     VALUE "TESTED".   NC2444.2
011600     02 FILLER                     PIC X(15)    VALUE SPACE.      NC2444.2
011700     02 FILLER                     PIC X(4)     VALUE "FAIL".     NC2444.2
011800     02 FILLER                     PIC X(94)    VALUE SPACE.      NC2444.2
011900 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       NC2444.2
012000 01  REC-CT                        PIC 99       VALUE ZERO.       NC2444.2
012100 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       NC2444.2
012200 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       NC2444.2
012300 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       NC2444.2
012400 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       NC2444.2
012500 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       NC2444.2
012600 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       NC2444.2
012700 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      NC2444.2
012800 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       NC2444.2
012900 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     NC2444.2
013000 01  CCVS-H-1.                                                    NC2444.2
013100     02  FILLER                    PIC X(39)    VALUE SPACES.     NC2444.2
013200     02  FILLER                    PIC X(42)    VALUE             NC2444.2
013300     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 NC2444.2
013400     02  FILLER                    PIC X(39)    VALUE SPACES.     NC2444.2
013500 01  CCVS-H-2A.                                                   NC2444.2
013600   02  FILLER                        PIC X(40)  VALUE SPACE.      NC2444.2
013700   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  NC2444.2
013800   02  FILLER                        PIC XXXX   VALUE             NC2444.2
013900     "4.2 ".                                                      NC2444.2
014000   02  FILLER                        PIC X(28)  VALUE             NC2444.2
014100            " COPY - NOT FOR DISTRIBUTION".                       NC2444.2
014200   02  FILLER                        PIC X(41)  VALUE SPACE.      NC2444.2
014300                                                                  NC2444.2
014400 01  CCVS-H-2B.                                                   NC2444.2
014500   02  FILLER                        PIC X(15)  VALUE             NC2444.2
014600            "TEST RESULT OF ".                                    NC2444.2
014700   02  TEST-ID                       PIC X(9).                    NC2444.2
014800   02  FILLER                        PIC X(4)   VALUE             NC2444.2
014900            " IN ".                                               NC2444.2
015000   02  FILLER                        PIC X(12)  VALUE             NC2444.2
015100     " HIGH       ".                                              NC2444.2
015200   02  FILLER                        PIC X(22)  VALUE             NC2444.2
015300            " LEVEL VALIDATION FOR ".                             NC2444.2
015400   02  FILLER                        PIC X(58)  VALUE             NC2444.2
015500     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2444.2
015600 01  CCVS-H-3.                                                    NC2444.2
015700     02  FILLER                      PIC X(34)  VALUE             NC2444.2
015800            " FOR OFFICIAL USE ONLY    ".                         NC2444.2
015900     02  FILLER                      PIC X(58)  VALUE             NC2444.2
016000     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC2444.2
016100     02  FILLER                      PIC X(28)  VALUE             NC2444.2
016200            "  COPYRIGHT   1985 ".                                NC2444.2
016300 01  CCVS-E-1.                                                    NC2444.2
016400     02 FILLER                       PIC X(52)  VALUE SPACE.      NC2444.2
016500     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              NC2444.2
016600     02 ID-AGAIN                     PIC X(9).                    NC2444.2
016700     02 FILLER                       PIC X(45)  VALUE SPACES.     NC2444.2
016800 01  CCVS-E-2.                                                    NC2444.2
016900     02  FILLER                      PIC X(31)  VALUE SPACE.      NC2444.2
017000     02  FILLER                      PIC X(21)  VALUE SPACE.      NC2444.2
017100     02 CCVS-E-2-2.                                               NC2444.2
017200         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      NC2444.2
017300         03 FILLER                   PIC X      VALUE SPACE.      NC2444.2
017400         03 ENDER-DESC               PIC X(44)  VALUE             NC2444.2
017500            "ERRORS ENCOUNTERED".                                 NC2444.2
017600 01  CCVS-E-3.                                                    NC2444.2
017700     02  FILLER                      PIC X(22)  VALUE             NC2444.2
017800            " FOR OFFICIAL USE ONLY".                             NC2444.2
017900     02  FILLER                      PIC X(12)  VALUE SPACE.      NC2444.2
018000     02  FILLER                      PIC X(58)  VALUE             NC2444.2
018100     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2444.2
018200     02  FILLER                      PIC X(13)  VALUE SPACE.      NC2444.2
018300     02 FILLER                       PIC X(15)  VALUE             NC2444.2
018400             " COPYRIGHT 1985".                                   NC2444.2
018500 01  CCVS-E-4.                                                    NC2444.2
018600     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      NC2444.2
018700     02 FILLER                       PIC X(4)   VALUE " OF ".     NC2444.2
018800     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      NC2444.2
018900     02 FILLER                       PIC X(40)  VALUE             NC2444.2
019000      "  TESTS WERE EXECUTED SUCCESSFULLY".                       NC2444.2
019100 01  XXINFO.                                                      NC2444.2
019200     02 FILLER                       PIC X(19)  VALUE             NC2444.2
019300            "*** INFORMATION ***".                                NC2444.2
019400     02 INFO-TEXT.                                                NC2444.2
019500       04 FILLER                     PIC X(8)   VALUE SPACE.      NC2444.2
019600       04 XXCOMPUTED                 PIC X(20).                   NC2444.2
019700       04 FILLER                     PIC X(5)   VALUE SPACE.      NC2444.2
019800       04 XXCORRECT                  PIC X(20).                   NC2444.2
019900     02 INF-ANSI-REFERENCE           PIC X(48).                   NC2444.2
020000 01  HYPHEN-LINE.                                                 NC2444.2
020100     02 FILLER  PIC IS X VALUE IS SPACE.                          NC2444.2
020200     02 FILLER  PIC IS X(65)    VALUE IS "************************NC2444.2
020300-    "*****************************************".                 NC2444.2
020400     02 FILLER  PIC IS X(54)    VALUE IS "************************NC2444.2
020500-    "******************************".                            NC2444.2
020600 01  CCVS-PGM-ID                     PIC X(9)   VALUE             NC2444.2
020700     "NC244A".                                                    NC2444.2
020800 PROCEDURE DIVISION.                                              NC2444.2
020900 CCVS1 SECTION.                                                   NC2444.2
021000 OPEN-FILES.                                                      NC2444.2
021100     OPEN     OUTPUT PRINT-FILE.                                  NC2444.2
021200     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   NC2444.2
021300     MOVE    SPACE TO TEST-RESULTS.                               NC2444.2
021400     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             NC2444.2
021500     GO TO CCVS1-EXIT.                                            NC2444.2
021600 CLOSE-FILES.                                                     NC2444.2
021700     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   NC2444.2
021800 TERMINATE-CCVS.                                                  NC2444.2
021900*    EXIT PROGRAM.                                                NC2444.2
022000*TERMINATE-CALL.                                                  NC2444.2
022100     STOP     RUN.                                                NC2444.2
022200 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         NC2444.2
022300 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           NC2444.2
022400 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          NC2444.2
022500 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      NC2444.2
022600     MOVE "****TEST DELETED****" TO RE-MARK.                      NC2444.2
022700 PRINT-DETAIL.                                                    NC2444.2
022800     IF REC-CT NOT EQUAL TO ZERO                                  NC2444.2
022900             MOVE "." TO PARDOT-X                                 NC2444.2
023000             MOVE REC-CT TO DOTVALUE.                             NC2444.2
023100     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      NC2444.2
023200     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               NC2444.2
023300        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 NC2444.2
023400          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 NC2444.2
023500     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              NC2444.2
023600     MOVE SPACE TO CORRECT-X.                                     NC2444.2
023700     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         NC2444.2
023800     MOVE     SPACE TO RE-MARK.                                   NC2444.2
023900 HEAD-ROUTINE.                                                    NC2444.2
024000     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC2444.2
024100     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC2444.2
024200     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC2444.2
024300     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC2444.2
024400 COLUMN-NAMES-ROUTINE.                                            NC2444.2
024500     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2444.2
024600     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2444.2
024700     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        NC2444.2
024800 END-ROUTINE.                                                     NC2444.2
024900     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.NC2444.2
025000 END-RTN-EXIT.                                                    NC2444.2
025100     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2444.2
025200 END-ROUTINE-1.                                                   NC2444.2
025300      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      NC2444.2
025400      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               NC2444.2
025500      ADD PASS-COUNTER TO ERROR-HOLD.                             NC2444.2
025600*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   NC2444.2
025700      MOVE PASS-COUNTER TO CCVS-E-4-1.                            NC2444.2
025800      MOVE ERROR-HOLD TO CCVS-E-4-2.                              NC2444.2
025900      MOVE CCVS-E-4 TO CCVS-E-2-2.                                NC2444.2
026000      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           NC2444.2
026100  END-ROUTINE-12.                                                 NC2444.2
026200      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        NC2444.2
026300     IF       ERROR-COUNTER IS EQUAL TO ZERO                      NC2444.2
026400         MOVE "NO " TO ERROR-TOTAL                                NC2444.2
026500         ELSE                                                     NC2444.2
026600         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       NC2444.2
026700     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           NC2444.2
026800     PERFORM WRITE-LINE.                                          NC2444.2
026900 END-ROUTINE-13.                                                  NC2444.2
027000     IF DELETE-COUNTER IS EQUAL TO ZERO                           NC2444.2
027100         MOVE "NO " TO ERROR-TOTAL  ELSE                          NC2444.2
027200         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      NC2444.2
027300     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   NC2444.2
027400     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2444.2
027500      IF   INSPECT-COUNTER EQUAL TO ZERO                          NC2444.2
027600          MOVE "NO " TO ERROR-TOTAL                               NC2444.2
027700      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   NC2444.2
027800      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            NC2444.2
027900      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          NC2444.2
028000     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2444.2
028100 WRITE-LINE.                                                      NC2444.2
028200     ADD 1 TO RECORD-COUNT.                                       NC2444.2
028300*    IF RECORD-COUNT GREATER 50                                   NC2444.2
028400*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          NC2444.2
028500*        MOVE SPACE TO DUMMY-RECORD                               NC2444.2
028600*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  NC2444.2
028700*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             NC2444.2
028800*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     NC2444.2
028900*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          NC2444.2
029000*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          NC2444.2
029100*        MOVE ZERO TO RECORD-COUNT.                               NC2444.2
029200     PERFORM WRT-LN.                                              NC2444.2
029300 WRT-LN.                                                          NC2444.2
029400     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               NC2444.2
029500     MOVE SPACE TO DUMMY-RECORD.                                  NC2444.2
029600 BLANK-LINE-PRINT.                                                NC2444.2
029700     PERFORM WRT-LN.                                              NC2444.2
029800 FAIL-ROUTINE.                                                    NC2444.2
029900     IF   COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE. NC2444.2
030000     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.NC2444.2
030100     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC2444.2
030200     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   NC2444.2
030300     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2444.2
030400     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC2444.2
030500     GO TO  FAIL-ROUTINE-EX.                                      NC2444.2
030600 FAIL-ROUTINE-WRITE.                                              NC2444.2
030700     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         NC2444.2
030800     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 NC2444.2
030900     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. NC2444.2
031000     MOVE   SPACES TO COR-ANSI-REFERENCE.                         NC2444.2
031100 FAIL-ROUTINE-EX. EXIT.                                           NC2444.2
031200 BAIL-OUT.                                                        NC2444.2
031300     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   NC2444.2
031400     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           NC2444.2
031500 BAIL-OUT-WRITE.                                                  NC2444.2
031600     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  NC2444.2
031700     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC2444.2
031800     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2444.2
031900     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC2444.2
032000 BAIL-OUT-EX. EXIT.                                               NC2444.2
032100 CCVS1-EXIT.                                                      NC2444.2
032200     EXIT.                                                        NC2444.2
032300 SECT-NC244A-001 SECTION.                                         NC2444.2
032400 TH-18-001.                                                       NC2444.2
032500 BUILD-2DEM-TABLE.                                                NC2444.2
032600     SET IDX-1  IDX-2  IDX-3  IDX-4  IDX-5  IDX-6  IDX-7  IDX-8   NC2444.2
032700         IDX-9  IDX-10 TO 01.                                     NC2444.2
032800     SET IDX-11  IDX-12  IDX-13  IDX-14  IDX-15 TO 01.            NC2444.2
032900 BUILD-LEVEL-1.                                                   NC2444.2
033000     SET ENTRY-1 (IDX-5) TO IDX-5.                                NC2444.2
033100     IF IDX-5 EQUAL TO 6 MOVE "BB" TO IN-SERT GO TO BUILD-ENTRY.  NC2444.2
033200     IF IDX-5 EQUAL TO 11 MOVE "CC" TO IN-SERT GO TO BUILD-ENTRY. NC2444.2
033300     IF IDX-5 EQUAL TO 16 MOVE "DD" TO IN-SERT GO TO BUILD-ENTRY. NC2444.2
033400     IF IDX-5 EQUAL TO 21 MOVE "EE" TO IN-SERT GO TO BUILD-ENTRY. NC2444.2
033500     IF IDX-5 EQUAL TO 26 MOVE "FF" TO IN-SERT GO TO BUILD-ENTRY. NC2444.2
033600     IF IDX-5 EQUAL TO 31 MOVE "GG" TO IN-SERT GO TO BUILD-ENTRY. NC2444.2
033700     IF IDX-5 EQUAL TO 36 MOVE "HH" TO IN-SERT GO TO BUILD-ENTRY. NC2444.2
033800     IF IDX-5 EQUAL TO 41 MOVE "II" TO IN-SERT GO TO BUILD-ENTRY. NC2444.2
033900     IF IDX-5 EQUAL TO 46 MOVE "JJ" TO IN-SERT.                   NC2444.2
034000 BUILD-ENTRY.                                                     NC2444.2
034100     MOVE IN-SERT TO ENTRY-2 (IDX-5, IDX-15).                     NC2444.2
034200     IF IDX-15 EQUAL TO 10 AND IDX-5 EQUAL TO 50                  NC2444.2
034300         GO TO BUILD-EXIT.                                        NC2444.2
034400     IF IDX-15 EQUAL TO 10                                        NC2444.2
034500         SET IDX-15 TO 01                                         NC2444.2
034600         SET IDX-5 UP BY 1                                        NC2444.2
034700         GO TO BUILD-LEVEL-1.                                     NC2444.2
034800     SET IDX-15 UP BY 01.                                         NC2444.2
034900     GO TO BUILD-ENTRY.                                           NC2444.2
035000 BUILD-EXIT.                                                      NC2444.2
035100     EXIT.                                                        NC2444.2
035200 TABLE-CHECKING SECTION.                                          NC2444.2
035300*                                                                 NC2444.2
035400 IDX-INIT-F1-1.                                                   NC2444.2
035500     MOVE "IDX-TEST-F1-1" TO PAR-NAME.                            NC2444.2
035600     MOVE "VI-126 6.22.2" TO ANSI-REFERENCE.                      NC2444.2
035700     MOVE "RELATIVE INDEXING   " TO FEATURE.                      NC2444.2
035800 IDX-TEST-F1-1.                                                   NC2444.2
035900     SET IDX-4  IDX-14 TO SUB-COMP2.                              NC2444.2
036000     IF ENTRY-2 (IDX-4 + 49, IDX-14 + 9) EQUAL TO "JJ"            NC2444.2
036100         PERFORM PASS                                             NC2444.2
036200         GO TO IDX-WRITE-F1-1                                     NC2444.2
036300     ELSE                                                         NC2444.2
036400         GO TO IDX-FAIL-F1-1.                                     NC2444.2
036500 IDX-DELETE-F1-1.                                                 NC2444.2
036600     PERFORM DE-LETE.                                             NC2444.2
036700     GO TO IDX-WRITE-F1-1.                                        NC2444.2
036800 IDX-FAIL-F1-1.                                                   NC2444.2
036900     MOVE ENTRY-2 (IDX-4 + 49, IDX-14 + 9) TO COMPUTED-A.         NC2444.2
037000     MOVE "JJ" TO CORRECT-A.                                      NC2444.2
037100     PERFORM FAIL.                                                NC2444.2
037200 IDX-WRITE-F1-1.                                                  NC2444.2
037300     PERFORM PRINT-DETAIL.                                        NC2444.2
037400*                                                                 NC2444.2
037500 IDX-INIT-F2-2.                                                   NC2444.2
037600     MOVE "IDX-TEST-F2-2" TO PAR-NAME.                            NC2444.2
037700     MOVE "VI-126 6.22.2" TO ANSI-REFERENCE.                      NC2444.2
037800     MOVE "SET DN BY COMP ITEM " TO FEATURE.                      NC2444.2
037900 IDX-TEST-F2-2.                                                   NC2444.2
038000     SET IDX-3 TO SUB-COMP3.                                      NC2444.2
038100     SET IDX-3 DOWN BY SUB-7.                                     NC2444.2
038200     IF ENTRY-1 (IDX-3) EQUAL TO 29                               NC2444.2
038300         PERFORM PASS                                             NC2444.2
038400         GO TO IDX-WRITE-F2-2                                     NC2444.2
038500     ELSE                                                         NC2444.2
038600         GO TO IDX-FAIL-F2-2.                                     NC2444.2
038700 IDX-DELETE-F2-2.                                                 NC2444.2
038800     PERFORM DE-LETE.                                             NC2444.2
038900     GO TO IDX-WRITE-F2-2.                                        NC2444.2
039000 IDX-FAIL-F2-2.                                                   NC2444.2
039100     MOVE ENTRY-1 (IDX-3) TO COMPUTED-N.                          NC2444.2
039200     MOVE 29 TO CORRECT-N.                                        NC2444.2
039300     PERFORM FAIL.                                                NC2444.2
039400 IDX-WRITE-F2-2.                                                  NC2444.2
039500     PERFORM PRINT-DETAIL.                                        NC2444.2
039600*                                                                 NC2444.2
039700 IDX-INIT-F2-3.                                                   NC2444.2
039800     MOVE "IDX-TEST-F2-3" TO PAR-NAME.                            NC2444.2
039900     MOVE "VI-126 6.22.2" TO ANSI-REFERENCE.                      NC2444.2
040000     MOVE "SET UP BY COMP ITEM " TO FEATURE.                      NC2444.2
040100 IDX-TEST-F2-3.                                                   NC2444.2
040200     SET IDX-2 TO SUB-COMP6.                                      NC2444.2
040300     SET IDX-2 UP BY SUB-COMP1.                                   NC2444.2
040400     IF ENTRY-1 (IDX-2) EQUAL TO 12                               NC2444.2
040500         PERFORM PASS                                             NC2444.2
040600         GO TO IDX-WRITE-F2-3                                     NC2444.2
040700     ELSE                                                         NC2444.2
040800         GO TO IDX-FAIL-F2-3.                                     NC2444.2
040900 IDX-DELETE-F2-3.                                                 NC2444.2
041000     PERFORM DE-LETE.                                             NC2444.2
041100     GO TO IDX-WRITE-F2-3.                                        NC2444.2
041200 IDX-FAIL-F2-3.                                                   NC2444.2
041300     MOVE ENTRY-1 (IDX-2) TO COMPUTED-N.                          NC2444.2
041400     MOVE 12 TO CORRECT-N.                                        NC2444.2
041500     PERFORM FAIL.                                                NC2444.2
041600 IDX-WRITE-F2-3.                                                  NC2444.2
041700     PERFORM PRINT-DETAIL.                                        NC2444.2
041800*                                                                 NC2444.2
041900 IDX-INIT-F2-4.                                                   NC2444.2
042000     MOVE "IDX-TEST-F2-4" TO PAR-NAME.                            NC2444.2
042100     MOVE "VI-126 6.22.2" TO ANSI-REFERENCE.                      NC2444.2
042200     MOVE "MULT OPERND SET STMT" TO FEATURE.                      NC2444.2
042300     GO TO IDX-TEST-F2-4.                                         NC2444.2
042400 TEST-4.                                                          NC2444.2
042500     SET IDX-1  IDX-6 DOWN BY SUB-COMP5.                          NC2444.2
042600     MOVE ENTRY-2 (IDX-1, IDX-6) TO ENTRY-HOLD.                   NC2444.2
042700 TEST-4EXIT.                                                      NC2444.2
042800     EXIT.                                                        NC2444.2
042900 IDX-TEST-F2-4.                                                   NC2444.2
043000     SET IDX-1 TO SUB-COMP3.                                      NC2444.2
043100     SET IDX-6 TO SUB-9.                                          NC2444.2
043200     PERFORM TEST-4 THRU TEST-4EXIT UNTIL                         NC2444.2
043300                ENTRY-2 (IDX-1, IDX-6) EQUAL TO "II".             NC2444.2
043400     IF ENTRY-HOLD EQUAL TO "II"                                  NC2444.2
043500         PERFORM PASS                                             NC2444.2
043600         GO TO IDX-WRITE-F2-4                                     NC2444.2
043700     ELSE                                                         NC2444.2
043800         GO TO IDX-FAIL-F2-4.                                     NC2444.2
043900 IDX-DELETE-F2-4.                                                 NC2444.2
044000     PERFORM DE-LETE.                                             NC2444.2
044100     GO TO IDX-WRITE-F2-4.                                        NC2444.2
044200 IDX-FAIL-F2-4.                                                   NC2444.2
044300     MOVE ENTRY-HOLD TO COMPUTED-A.                               NC2444.2
044400     MOVE "II" TO CORRECT-A.                                      NC2444.2
044500 IDX-WRITE-F2-4.                                                  NC2444.2
044600     PERFORM PRINT-DETAIL.                                        NC2444.2
044700*                                                                 NC2444.2
044800 IDX-INIT-F2-5.                                                   NC2444.2
044900     MOVE "IDX-TEST-F2-5" TO PAR-NAME.                            NC2444.2
045000     MOVE "VI-126 6.22.2" TO ANSI-REFERENCE.                      NC2444.2
045100     MOVE "PFM VARYNG COMP ITEM" TO FEATURE.                      NC2444.2
045200     MOVE SPACES TO ENTRY-HOLD.                                   NC2444.2
045300     GO TO IDX-TEST-F2-5.                                         NC2444.2
045400 TEST-5.                                                          NC2444.2
045500     SET IDX-3 TO SUB-COMP2.                                      NC2444.2
045600     SET IDX-14 TO SUB-COMP5.                                     NC2444.2
045700     MOVE ENTRY-2 (IDX-3, IDX-14) TO ENTRY-HOLD.                  NC2444.2
045800 IDX-TEST-F2-5.                                                   NC2444.2
045900     SET IDX-3, IDX-14 TO 01.                                     NC2444.2
046000     PERFORM TEST-5                                               NC2444.2
046100             VARYING SUB-COMP5 FROM 1 BY SUB-8                    NC2444.2
046200             UNTIL ENTRY-2 (IDX-3, IDX-14) EQUAL TO "JJ"          NC2444.2
046300                OR IDX-14 EQUAL TO 10                             NC2444.2
046400             AFTER SUB-COMP2 FROM 1 BY 1                          NC2444.2
046500             UNTIL ENTRY-1 (IDX-3) EQUAL TO 46.                   NC2444.2
046600     IF ENTRY-HOLD EQUAL TO "JJ"                                  NC2444.2
046700         PERFORM PASS                                             NC2444.2
046800         GO TO IDX-WRITE-F2-5                                     NC2444.2
046900     ELSE                                                         NC2444.2
047000         GO TO IDX-FAIL-F2-5.                                     NC2444.2
047100 IDX-DELETE-F2-5.                                                 NC2444.2
047200     PERFORM DE-LETE.                                             NC2444.2
047300     MOVE "IDX-TEST-F2-5" TO PAR-NAME.                            NC2444.2
047400     MOVE  "PFM VARYING COMP ITEM" TO FEATURE.                    NC2444.2
047500     PERFORM PRINT-DETAIL.                                        NC2444.2
047600*    NOTE IF THIS TEST IS DELETED TEST-6 WILL ALSO BE DELETED.    NC2444.2
047700     PERFORM DE-LETE.                                             NC2444.2
047800     MOVE "IDX-TEST-F2-6" TO PAR-NAME.                            NC2444.2
047900     PERFORM PRINT-DETAIL.                                        NC2444.2
048000     GO TO CLOSE-FILES.                                           NC2444.2
048100 IDX-FAIL-F2-5.                                                   NC2444.2
048200     MOVE "JJ" TO CORRECT-A.                                      NC2444.2
048300     MOVE ENTRY-HOLD TO COMPUTED-A.                               NC2444.2
048400     PERFORM FAIL.                                                NC2444.2
048500 IDX-WRITE-F2-5.                                                  NC2444.2
048600     PERFORM PRINT-DETAIL.                                        NC2444.2
048700*                                                                 NC2444.2
048800 IDX-INIT-F2-6.                                                   NC2444.2
048900     MOVE "IDX-TEST-F2-6" TO PAR-NAME.                            NC2444.2
049000     MOVE "VI-126 6.22.2" TO ANSI-REFERENCE.                      NC2444.2
049100 IDX-TEST-F2-6.                                                   NC2444.2
049200     IF IDX-3 EQUAL TO 46 AND IDX-14 EQUAL TO 01                  NC2444.2
049300         PERFORM PASS                                             NC2444.2
049400         GO TO IDX-WRITE-F2-6                                     NC2444.2
049500     ELSE                                                         NC2444.2
049600         GO TO IDX-FAIL-F2-6.                                     NC2444.2
049700 IDX-DELETE-F2-6.                                                 NC2444.2
049800     PERFORM DE-LETE.                                             NC2444.2
049900     GO TO IDX-WRITE-F2-6.                                        NC2444.2
050000 IDX-FAIL-F2-6.                                                   NC2444.2
050100     SET IDX-3HOLD TO IDX-3.                                      NC2444.2
050200     SET IDX-14HOLD TO IDX-14.                                    NC2444.2
050300     MOVE IDX-HOLD TO COMPUTED-A.                                 NC2444.2
050400     PERFORM FAIL.                                                NC2444.2
050500     MOVE "000046        000001" TO CORRECT-A.                    NC2444.2
050600     MOVE "COMPARE INDEXES OF TEST-5" TO RE-MARK.                 NC2444.2
050700 IDX-WRITE-F2-6.                                                  NC2444.2
050800     PERFORM PRINT-DETAIL.                                        NC2444.2
050900 CCVS-EXIT SECTION.                                               NC2444.2
051000 CCVS-999999.                                                     NC2444.2
051100     GO TO CLOSE-FILES.                                           NC2444.2
      *END-OF,NC244A                                                                  
