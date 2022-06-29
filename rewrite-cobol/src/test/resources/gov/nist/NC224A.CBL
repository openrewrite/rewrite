      *HEADER,COBOL,NC224A                                                            
000100 IDENTIFICATION DIVISION.                                         NC2244.2
000200 PROGRAM-ID.                                                      NC2244.2
000300     NC224A.                                                      NC2244.2
000400****************************************************************  NC2244.2
000500*                                                              *  NC2244.2
000600*    VALIDATION FOR:-                                          *  NC2244.2
000700*                                                              *  NC2244.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2244.2
000900*                                                              *  NC2244.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC2244.2
001100*                                                              *  NC2244.2
001200****************************************************************  NC2244.2
001300*                                                              *  NC2244.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  NC2244.2
001500*                                                              *  NC2244.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  NC2244.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  NC2244.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  NC2244.2
001900*                                                              *  NC2244.2
002000****************************************************************  NC2244.2
002100*    PROGRAM NC224A TESTS THE USE OF REFERENCE MODIFICATION    *  NC2244.2
002200*    ON A VARIETY OF DATA ITEMS USING LITERALS, DATA NAMES     *  NC2244.2
002300*    AND ARITHMETIC EXPRESSIONS AS PARAMETERS.                 *  NC2244.2
002400*    SUBSCRIPTED AND QUALIFIED DATA ITEMS ARE ALSO USED.       *  NC2244.2
002500*                                                              *  NC2244.2
002600****************************************************************  NC2244.2
002700 ENVIRONMENT DIVISION.                                            NC2244.2
002800 CONFIGURATION SECTION.                                           NC2244.2
002900 SOURCE-COMPUTER.                                                 NC2244.2
003000     XXXXX082.                                                    NC2244.2
003100 OBJECT-COMPUTER.                                                 NC2244.2
003200     XXXXX083.                                                    NC2244.2
003300 INPUT-OUTPUT SECTION.                                            NC2244.2
003400 FILE-CONTROL.                                                    NC2244.2
003500     SELECT PRINT-FILE ASSIGN TO                                  NC2244.2
003600     XXXXX055.                                                    NC2244.2
003700 DATA DIVISION.                                                   NC2244.2
003800 FILE SECTION.                                                    NC2244.2
003900 FD  PRINT-FILE.                                                  NC2244.2
004000 01  PRINT-REC PICTURE X(120).                                    NC2244.2
004100 01  DUMMY-RECORD PICTURE X(120).                                 NC2244.2
004200 WORKING-STORAGE SECTION.                                         NC2244.2
004300 01  TEST-1-DATA                 PIC   9(6)     VALUE 123456.     NC2244.2
004400 01  TEST-2-DATA                 PIC   Z(5)9.                     NC2244.2
004500 01  TEST-3-DATA-GRP.                                             NC2244.2
004600   03  TEST-3-DATA               PIC   X(6)     VALUE "ABCDEF".   NC2244.2
004700 01  TEST-4-DATA                 PIC   XXBXXBXX VALUE "AB CD EF". NC2244.2
004800                                                                  NC2244.2
004900 01  WS-2                        PIC  S9        VALUE +2.         NC2244.2
005000 01  WS-3                        PIC  S9        VALUE +3.         NC2244.2
005100 01  WS-5                        PIC  S9        VALUE +5.         NC2244.2
005200 01  WS-6                        PIC  S9        VALUE +6.         NC2244.2
005300 01  WS-7                        PIC  S9        VALUE +7.         NC2244.2
005400 01  WS-10                       PIC  S99       VALUE +10.        NC2244.2
005500 01  TEST-5-TABLE.                                                NC2244.2
005600   03        TABLE-GROUP         OCCURS 4.                        NC2244.2
005700     05      TABLE-1             PIC 9(8)                         NC2244.2
005800                                 OCCURS 2.                        NC2244.2
005900   03         TEST-3-DATA        PIC X(6).                        NC2244.2
006000*                                                                 NC2244.2
006100 01  TEST-RESULTS.                                                NC2244.2
006200     02 FILLER                   PIC X      VALUE SPACE.          NC2244.2
006300     02 FEATURE                  PIC X(20)  VALUE SPACE.          NC2244.2
006400     02 FILLER                   PIC X      VALUE SPACE.          NC2244.2
006500     02 P-OR-F                   PIC X(5)   VALUE SPACE.          NC2244.2
006600     02 FILLER                   PIC X      VALUE SPACE.          NC2244.2
006700     02  PAR-NAME.                                                NC2244.2
006800       03 FILLER                 PIC X(19)  VALUE SPACE.          NC2244.2
006900       03  PARDOT-X              PIC X      VALUE SPACE.          NC2244.2
007000       03 DOTVALUE               PIC 99     VALUE ZERO.           NC2244.2
007100     02 FILLER                   PIC X(8)   VALUE SPACE.          NC2244.2
007200     02 RE-MARK                  PIC X(61).                       NC2244.2
007300 01  TEST-COMPUTED.                                               NC2244.2
007400     02 FILLER                   PIC X(30)  VALUE SPACE.          NC2244.2
007500     02 FILLER                   PIC X(17)  VALUE                 NC2244.2
007600            "       COMPUTED=".                                   NC2244.2
007700     02 COMPUTED-X.                                               NC2244.2
007800     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          NC2244.2
007900     03 COMPUTED-N               REDEFINES COMPUTED-A             NC2244.2
008000                                 PIC -9(9).9(9).                  NC2244.2
008100     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         NC2244.2
008200     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     NC2244.2
008300     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     NC2244.2
008400     03       CM-18V0 REDEFINES COMPUTED-A.                       NC2244.2
008500         04 COMPUTED-18V0                    PIC -9(18).          NC2244.2
008600         04 FILLER                           PIC X.               NC2244.2
008700     03 FILLER PIC X(50) VALUE SPACE.                             NC2244.2
008800 01  TEST-CORRECT.                                                NC2244.2
008900     02 FILLER PIC X(30) VALUE SPACE.                             NC2244.2
009000     02 FILLER PIC X(17) VALUE "       CORRECT =".                NC2244.2
009100     02 CORRECT-X.                                                NC2244.2
009200     03 CORRECT-A                  PIC X(20) VALUE SPACE.         NC2244.2
009300     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      NC2244.2
009400     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         NC2244.2
009500     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     NC2244.2
009600     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     NC2244.2
009700     03      CR-18V0 REDEFINES CORRECT-A.                         NC2244.2
009800         04 CORRECT-18V0                     PIC -9(18).          NC2244.2
009900         04 FILLER                           PIC X.               NC2244.2
010000     03 FILLER PIC X(2) VALUE SPACE.                              NC2244.2
010100     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     NC2244.2
010200 01  CCVS-C-1.                                                    NC2244.2
010300     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PANC2244.2
010400-    "SS  PARAGRAPH-NAME                                          NC2244.2
010500-    "       REMARKS".                                            NC2244.2
010600     02 FILLER                     PIC X(20)    VALUE SPACE.      NC2244.2
010700 01  CCVS-C-2.                                                    NC2244.2
010800     02 FILLER                     PIC X        VALUE SPACE.      NC2244.2
010900     02 FILLER                     PIC X(6)     VALUE "TESTED".   NC2244.2
011000     02 FILLER                     PIC X(15)    VALUE SPACE.      NC2244.2
011100     02 FILLER                     PIC X(4)     VALUE "FAIL".     NC2244.2
011200     02 FILLER                     PIC X(94)    VALUE SPACE.      NC2244.2
011300 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       NC2244.2
011400 01  REC-CT                        PIC 99       VALUE ZERO.       NC2244.2
011500 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       NC2244.2
011600 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       NC2244.2
011700 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       NC2244.2
011800 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       NC2244.2
011900 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       NC2244.2
012000 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       NC2244.2
012100 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      NC2244.2
012200 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       NC2244.2
012300 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     NC2244.2
012400 01  CCVS-H-1.                                                    NC2244.2
012500     02  FILLER                    PIC X(39)    VALUE SPACES.     NC2244.2
012600     02  FILLER                    PIC X(42)    VALUE             NC2244.2
012700     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 NC2244.2
012800     02  FILLER                    PIC X(39)    VALUE SPACES.     NC2244.2
012900 01  CCVS-H-2A.                                                   NC2244.2
013000   02  FILLER                        PIC X(40)  VALUE SPACE.      NC2244.2
013100   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  NC2244.2
013200   02  FILLER                        PIC XXXX   VALUE             NC2244.2
013300     "4.2 ".                                                      NC2244.2
013400   02  FILLER                        PIC X(28)  VALUE             NC2244.2
013500            " COPY - NOT FOR DISTRIBUTION".                       NC2244.2
013600   02  FILLER                        PIC X(41)  VALUE SPACE.      NC2244.2
013700                                                                  NC2244.2
013800 01  CCVS-H-2B.                                                   NC2244.2
013900   02  FILLER                        PIC X(15)  VALUE             NC2244.2
014000            "TEST RESULT OF ".                                    NC2244.2
014100   02  TEST-ID                       PIC X(9).                    NC2244.2
014200   02  FILLER                        PIC X(4)   VALUE             NC2244.2
014300            " IN ".                                               NC2244.2
014400   02  FILLER                        PIC X(12)  VALUE             NC2244.2
014500     " HIGH       ".                                              NC2244.2
014600   02  FILLER                        PIC X(22)  VALUE             NC2244.2
014700            " LEVEL VALIDATION FOR ".                             NC2244.2
014800   02  FILLER                        PIC X(58)  VALUE             NC2244.2
014900     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2244.2
015000 01  CCVS-H-3.                                                    NC2244.2
015100     02  FILLER                      PIC X(34)  VALUE             NC2244.2
015200            " FOR OFFICIAL USE ONLY    ".                         NC2244.2
015300     02  FILLER                      PIC X(58)  VALUE             NC2244.2
015400     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC2244.2
015500     02  FILLER                      PIC X(28)  VALUE             NC2244.2
015600            "  COPYRIGHT   1985 ".                                NC2244.2
015700 01  CCVS-E-1.                                                    NC2244.2
015800     02 FILLER                       PIC X(52)  VALUE SPACE.      NC2244.2
015900     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              NC2244.2
016000     02 ID-AGAIN                     PIC X(9).                    NC2244.2
016100     02 FILLER                       PIC X(45)  VALUE SPACES.     NC2244.2
016200 01  CCVS-E-2.                                                    NC2244.2
016300     02  FILLER                      PIC X(31)  VALUE SPACE.      NC2244.2
016400     02  FILLER                      PIC X(21)  VALUE SPACE.      NC2244.2
016500     02 CCVS-E-2-2.                                               NC2244.2
016600         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      NC2244.2
016700         03 FILLER                   PIC X      VALUE SPACE.      NC2244.2
016800         03 ENDER-DESC               PIC X(44)  VALUE             NC2244.2
016900            "ERRORS ENCOUNTERED".                                 NC2244.2
017000 01  CCVS-E-3.                                                    NC2244.2
017100     02  FILLER                      PIC X(22)  VALUE             NC2244.2
017200            " FOR OFFICIAL USE ONLY".                             NC2244.2
017300     02  FILLER                      PIC X(12)  VALUE SPACE.      NC2244.2
017400     02  FILLER                      PIC X(58)  VALUE             NC2244.2
017500     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2244.2
017600     02  FILLER                      PIC X(13)  VALUE SPACE.      NC2244.2
017700     02 FILLER                       PIC X(15)  VALUE             NC2244.2
017800             " COPYRIGHT 1985".                                   NC2244.2
017900 01  CCVS-E-4.                                                    NC2244.2
018000     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      NC2244.2
018100     02 FILLER                       PIC X(4)   VALUE " OF ".     NC2244.2
018200     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      NC2244.2
018300     02 FILLER                       PIC X(40)  VALUE             NC2244.2
018400      "  TESTS WERE EXECUTED SUCCESSFULLY".                       NC2244.2
018500 01  XXINFO.                                                      NC2244.2
018600     02 FILLER                       PIC X(19)  VALUE             NC2244.2
018700            "*** INFORMATION ***".                                NC2244.2
018800     02 INFO-TEXT.                                                NC2244.2
018900       04 FILLER                     PIC X(8)   VALUE SPACE.      NC2244.2
019000       04 XXCOMPUTED                 PIC X(20).                   NC2244.2
019100       04 FILLER                     PIC X(5)   VALUE SPACE.      NC2244.2
019200       04 XXCORRECT                  PIC X(20).                   NC2244.2
019300     02 INF-ANSI-REFERENCE           PIC X(48).                   NC2244.2
019400 01  HYPHEN-LINE.                                                 NC2244.2
019500     02 FILLER  PIC IS X VALUE IS SPACE.                          NC2244.2
019600     02 FILLER  PIC IS X(65)    VALUE IS "************************NC2244.2
019700-    "*****************************************".                 NC2244.2
019800     02 FILLER  PIC IS X(54)    VALUE IS "************************NC2244.2
019900-    "******************************".                            NC2244.2
020000 01  CCVS-PGM-ID                     PIC X(9)   VALUE             NC2244.2
020100     "NC224A".                                                    NC2244.2
020200 PROCEDURE DIVISION.                                              NC2244.2
020300 CCVS1 SECTION.                                                   NC2244.2
020400 OPEN-FILES.                                                      NC2244.2
020500     OPEN     OUTPUT PRINT-FILE.                                  NC2244.2
020600     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   NC2244.2
020700     MOVE    SPACE TO TEST-RESULTS.                               NC2244.2
020800     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             NC2244.2
020900     GO TO CCVS1-EXIT.                                            NC2244.2
021000 CLOSE-FILES.                                                     NC2244.2
021100     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   NC2244.2
021200 TERMINATE-CCVS.                                                  NC2244.2
021300*    EXIT PROGRAM.                                                NC2244.2
021400*TERMINATE-CALL.                                                  NC2244.2
021500     STOP     RUN.                                                NC2244.2
021600 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         NC2244.2
021700 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           NC2244.2
021800 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          NC2244.2
021900 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      NC2244.2
022000     MOVE "****TEST DELETED****" TO RE-MARK.                      NC2244.2
022100 PRINT-DETAIL.                                                    NC2244.2
022200     IF REC-CT NOT EQUAL TO ZERO                                  NC2244.2
022300             MOVE "." TO PARDOT-X                                 NC2244.2
022400             MOVE REC-CT TO DOTVALUE.                             NC2244.2
022500     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      NC2244.2
022600     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               NC2244.2
022700        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 NC2244.2
022800          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 NC2244.2
022900     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              NC2244.2
023000     MOVE SPACE TO CORRECT-X.                                     NC2244.2
023100     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         NC2244.2
023200     MOVE     SPACE TO RE-MARK.                                   NC2244.2
023300 HEAD-ROUTINE.                                                    NC2244.2
023400     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC2244.2
023500     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC2244.2
023600     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC2244.2
023700     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC2244.2
023800 COLUMN-NAMES-ROUTINE.                                            NC2244.2
023900     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2244.2
024000     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2244.2
024100     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        NC2244.2
024200 END-ROUTINE.                                                     NC2244.2
024300     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.NC2244.2
024400 END-RTN-EXIT.                                                    NC2244.2
024500     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2244.2
024600 END-ROUTINE-1.                                                   NC2244.2
024700      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      NC2244.2
024800      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               NC2244.2
024900      ADD PASS-COUNTER TO ERROR-HOLD.                             NC2244.2
025000*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   NC2244.2
025100                                                                  NC2244.2
025200      MOVE PASS-COUNTER TO CCVS-E-4-1.                            NC2244.2
025300      MOVE ERROR-HOLD TO CCVS-E-4-2.                              NC2244.2
025400      MOVE CCVS-E-4 TO CCVS-E-2-2.                                NC2244.2
025500      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           NC2244.2
025600  END-ROUTINE-12.                                                 NC2244.2
025700      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        NC2244.2
025800     IF       ERROR-COUNTER IS EQUAL TO ZERO                      NC2244.2
025900         MOVE "NO " TO ERROR-TOTAL                                NC2244.2
026000         ELSE                                                     NC2244.2
026100         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       NC2244.2
026200     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           NC2244.2
026300     PERFORM WRITE-LINE.                                          NC2244.2
026400 END-ROUTINE-13.                                                  NC2244.2
026500     IF DELETE-COUNTER IS EQUAL TO ZERO                           NC2244.2
026600         MOVE "NO " TO ERROR-TOTAL  ELSE                          NC2244.2
026700         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      NC2244.2
026800     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   NC2244.2
026900     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2244.2
027000      IF   INSPECT-COUNTER EQUAL TO ZERO                          NC2244.2
027100          MOVE "NO " TO ERROR-TOTAL                               NC2244.2
027200      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   NC2244.2
027300      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            NC2244.2
027400      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          NC2244.2
027500     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2244.2
027600 WRITE-LINE.                                                      NC2244.2
027700     ADD 1 TO RECORD-COUNT.                                       NC2244.2
027800*    IF RECORD-COUNT GREATER 50                                   NC2244.2
027900*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          NC2244.2
028000*        MOVE SPACE TO DUMMY-RECORD                               NC2244.2
028100*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  NC2244.2
028200*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             NC2244.2
028300*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     NC2244.2
028400*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          NC2244.2
028500*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          NC2244.2
028600*        MOVE ZERO TO RECORD-COUNT.                               NC2244.2
028700     PERFORM WRT-LN.                                              NC2244.2
028800 WRT-LN.                                                          NC2244.2
028900     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               NC2244.2
029000     MOVE SPACE TO DUMMY-RECORD.                                  NC2244.2
029100 BLANK-LINE-PRINT.                                                NC2244.2
029200     PERFORM WRT-LN.                                              NC2244.2
029300 FAIL-ROUTINE.                                                    NC2244.2
029400     IF     COMPUTED-X NOT EQUAL TO SPACE                         NC2244.2
029500            GO TO    FAIL-ROUTINE-WRITE.                          NC2244.2
029600     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.NC2244.2
029700     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC2244.2
029800     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   NC2244.2
029900     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2244.2
030000     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC2244.2
030100     GO TO  FAIL-ROUTINE-EX.                                      NC2244.2
030200 FAIL-ROUTINE-WRITE.                                              NC2244.2
030300     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         NC2244.2
030400     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 NC2244.2
030500     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. NC2244.2
030600     MOVE   SPACES TO COR-ANSI-REFERENCE.                         NC2244.2
030700 FAIL-ROUTINE-EX. EXIT.                                           NC2244.2
030800 BAIL-OUT.                                                        NC2244.2
030900     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   NC2244.2
031000     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           NC2244.2
031100 BAIL-OUT-WRITE.                                                  NC2244.2
031200     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  NC2244.2
031300     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC2244.2
031400     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2244.2
031500     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC2244.2
031600 BAIL-OUT-EX. EXIT.                                               NC2244.2
031700 CCVS1-EXIT.                                                      NC2244.2
031800     EXIT.                                                        NC2244.2
031900 SECT-NC224A-001 SECTION.                                         NC2244.2
032000*                                                                 NC2244.2
032100 REF-INIT-GF-1.                                                   NC2244.2
032200     MOVE   "REFERENCE MODIFICATION" TO FEATURE.                  NC2244.2
032300     MOVE   "IV-22 4.3.8.3" TO ANSI-REFERENCE.                    NC2244.2
032400     MOVE    123456  TO TEST-1-DATA.                              NC2244.2
032500     MOVE    1       TO REC-CT.                                   NC2244.2
032600     GO TO   REF-TEST-GF-1-1.                                     NC2244.2
032700 REF-DELETE-GF-1.                                                 NC2244.2
032800     PERFORM DE-LETE.                                             NC2244.2
032900     PERFORM PRINT-DETAIL.                                        NC2244.2
033000     GO TO   REF-INIT-GF-2.                                       NC2244.2
033100 REF-TEST-GF-1-1.                                                 NC2244.2
033200     MOVE   "REF-TEST-GF-1-1" TO PAR-NAME.                        NC2244.2
033300     IF      TEST-1-DATA (3:) = 3456                              NC2244.2
033400             PERFORM PASS                                         NC2244.2
033500             GO TO REF-WRITE-GF-1-1                               NC2244.2
033600     ELSE                                                         NC2244.2
033700             GO TO REF-FAIL-GF-1-1.                               NC2244.2
033800 REF-DELETE-GF-1-1.                                               NC2244.2
033900     PERFORM DE-LETE.                                             NC2244.2
034000     GO TO REF-WRITE-GF-1-1.                                      NC2244.2
034100 REF-FAIL-GF-1-1.                                                 NC2244.2
034200     MOVE    3456 TO CORRECT-N                                    NC2244.2
034300     MOVE    TEST-1-DATA (3:) TO COMPUTED-X                       NC2244.2
034400     MOVE   "INCORRECT REFERENCE MODIFICATION" TO RE-MARK         NC2244.2
034500     PERFORM FAIL.                                                NC2244.2
034600 REF-WRITE-GF-1-1.                                                NC2244.2
034700     PERFORM PRINT-DETAIL.                                        NC2244.2
034800*                                                                 NC2244.2
034900 REF-TEST-GF-1-2.                                                 NC2244.2
035000     ADD     1       TO REC-CT.                                   NC2244.2
035100     MOVE   "REF-TEST-GF-1-2" TO PAR-NAME.                        NC2244.2
035200     IF      TEST-1-DATA (2: WS-3) = 234                          NC2244.2
035300             PERFORM PASS                                         NC2244.2
035400             GO TO REF-WRITE-GF-1-2                               NC2244.2
035500     ELSE                                                         NC2244.2
035600             GO TO REF-FAIL-GF-1-2.                               NC2244.2
035700 REF-DELETE-GF-1-2.                                               NC2244.2
035800     PERFORM DE-LETE.                                             NC2244.2
035900     GO TO REF-WRITE-GF-1-2.                                      NC2244.2
036000 REF-FAIL-GF-1-2.                                                 NC2244.2
036100     MOVE    234 TO CORRECT-N                                     NC2244.2
036200     MOVE    TEST-1-DATA (2: WS-3) TO COMPUTED-X                  NC2244.2
036300     MOVE   "INCORRECT REFERENCE MODIFICATION" TO RE-MARK         NC2244.2
036400     PERFORM FAIL.                                                NC2244.2
036500 REF-WRITE-GF-1-2.                                                NC2244.2
036600     PERFORM PRINT-DETAIL.                                        NC2244.2
036700*                                                                 NC2244.2
036800 REF-TEST-GF-1-3.                                                 NC2244.2
036900     ADD     1       TO REC-CT.                                   NC2244.2
037000     MOVE   "REF-TEST-GF-1-3" TO PAR-NAME.                        NC2244.2
037100     IF      TEST-1-DATA (10 - 7: 6 + 2 - 5) = 345                NC2244.2
037200             PERFORM PASS                                         NC2244.2
037300             GO TO REF-WRITE-GF-1-3                               NC2244.2
037400     ELSE                                                         NC2244.2
037500             GO TO REF-FAIL-GF-1-3.                               NC2244.2
037600 REF-DELETE-GF-1-3.                                               NC2244.2
037700     PERFORM DE-LETE.                                             NC2244.2
037800     GO TO REF-WRITE-GF-1-3.                                      NC2244.2
037900 REF-FAIL-GF-1-3.                                                 NC2244.2
038000     MOVE    345 TO CORRECT-N                                     NC2244.2
038100     MOVE    TEST-1-DATA (10 - 7: 6 + 2 - 5)                      NC2244.2
038200         TO COMPUTED-X                                            NC2244.2
038300     MOVE   "INCORRECT REFERENCE MODIFICATION" TO RE-MARK         NC2244.2
038400     PERFORM FAIL.                                                NC2244.2
038500 REF-WRITE-GF-1-3.                                                NC2244.2
038600     PERFORM PRINT-DETAIL.                                        NC2244.2
038700*                                                                 NC2244.2
038800 REF-INIT-GF-2.                                                   NC2244.2
038900     MOVE   "IV-22 4.3.8.3" TO ANSI-REFERENCE.                    NC2244.2
039000     MOVE    1234    TO TEST-2-DATA.                              NC2244.2
039100     MOVE    1       TO REC-CT.                                   NC2244.2
039200     GO TO   REF-TEST-GF-2-1.                                     NC2244.2
039300 REF-DELETE-GF-2.                                                 NC2244.2
039400     PERFORM DE-LETE.                                             NC2244.2
039500     PERFORM PRINT-DETAIL.                                        NC2244.2
039600     GO TO   REF-INIT-GF-3.                                       NC2244.2
039700 REF-TEST-GF-2-1.                                                 NC2244.2
039800     MOVE   "REF-TEST-GF-2-1" TO PAR-NAME.                        NC2244.2
039900     IF      TEST-2-DATA (WS-3:) = "1234"                         NC2244.2
040000             PERFORM PASS                                         NC2244.2
040100             GO TO REF-WRITE-GF-2-1                               NC2244.2
040200     ELSE                                                         NC2244.2
040300             GO TO REF-FAIL-GF-2-1.                               NC2244.2
040400 REF-DELETE-GF-2-1.                                               NC2244.2
040500     PERFORM DE-LETE.                                             NC2244.2
040600     GO TO REF-WRITE-GF-2-1.                                      NC2244.2
040700 REF-FAIL-GF-2-1.                                                 NC2244.2
040800     MOVE   "1234" TO CORRECT-X                                   NC2244.2
040900     MOVE    TEST-2-DATA (WS-3:) TO COMPUTED-X                    NC2244.2
041000     MOVE   "INCORRECT REFERENCE MODIFICATION" TO RE-MARK         NC2244.2
041100     PERFORM FAIL.                                                NC2244.2
041200 REF-WRITE-GF-2-1.                                                NC2244.2
041300     PERFORM PRINT-DETAIL.                                        NC2244.2
041400*                                                                 NC2244.2
041500 REF-TEST-GF-2-2.                                                 NC2244.2
041600     ADD     1       TO REC-CT.                                   NC2244.2
041700     MOVE   "REF-TEST-GF-2-2" TO PAR-NAME.                        NC2244.2
041800     IF      TEST-2-DATA (WS-2: 3) = " 12"                        NC2244.2
041900             PERFORM PASS                                         NC2244.2
042000             GO TO REF-WRITE-GF-2-2                               NC2244.2
042100     ELSE                                                         NC2244.2
042200             GO TO REF-FAIL-GF-2-2.                               NC2244.2
042300 REF-DELETE-GF-2-2.                                               NC2244.2
042400     PERFORM DE-LETE.                                             NC2244.2
042500     GO TO REF-WRITE-GF-2-2.                                      NC2244.2
042600 REF-FAIL-GF-2-2.                                                 NC2244.2
042700     MOVE   " 12" TO CORRECT-X                                    NC2244.2
042800     MOVE    TEST-2-DATA (WS-2: 3) TO COMPUTED-X                  NC2244.2
042900     MOVE   "INCORRECT REFERENCE MODIFICATION" TO RE-MARK         NC2244.2
043000     PERFORM FAIL.                                                NC2244.2
043100 REF-WRITE-GF-2-2.                                                NC2244.2
043200     PERFORM PRINT-DETAIL.                                        NC2244.2
043300*                                                                 NC2244.2
043400 REF-TEST-GF-2-3.                                                 NC2244.2
043500     ADD     1       TO REC-CT.                                   NC2244.2
043600     MOVE   "REF-TEST-GF-2-3" TO PAR-NAME.                        NC2244.2
043700     IF      TEST-2-DATA (10 - 7: 6 + 2 - 5) = "123"              NC2244.2
043800             PERFORM PASS                                         NC2244.2
043900             GO TO REF-WRITE-GF-2-3                               NC2244.2
044000     ELSE                                                         NC2244.2
044100             GO TO REF-FAIL-GF-2-3.                               NC2244.2
044200 REF-DELETE-GF-2-3.                                               NC2244.2
044300     PERFORM DE-LETE.                                             NC2244.2
044400     GO TO REF-WRITE-GF-2-3.                                      NC2244.2
044500 REF-FAIL-GF-2-3.                                                 NC2244.2
044600     MOVE   "123" TO CORRECT-X                                    NC2244.2
044700     MOVE    TEST-2-DATA (10 - 7: 6 + 2 - 5)                      NC2244.2
044800          TO COMPUTED-X                                           NC2244.2
044900     MOVE   "INCORRECT REFERENCE MODIFICATION" TO RE-MARK         NC2244.2
045000     PERFORM FAIL.                                                NC2244.2
045100 REF-WRITE-GF-2-3.                                                NC2244.2
045200     PERFORM PRINT-DETAIL.                                        NC2244.2
045300*                                                                 NC2244.2
045400 REF-INIT-GF-3.                                                   NC2244.2
045500     MOVE   "IV-22 4.3.8.3" TO ANSI-REFERENCE.                    NC2244.2
045600     MOVE   "ABCDEF" TO TEST-3-DATA IN TEST-3-DATA-GRP.           NC2244.2
045700     MOVE    1       TO REC-CT.                                   NC2244.2
045800     GO TO   REF-TEST-GF-3-1.                                     NC2244.2
045900 REF-DELETE-GF-3.                                                 NC2244.2
046000     PERFORM DE-LETE.                                             NC2244.2
046100     PERFORM PRINT-DETAIL.                                        NC2244.2
046200     GO TO   REF-INIT-GF-3.                                       NC2244.2
046300 REF-TEST-GF-3-1.                                                 NC2244.2
046400     MOVE   "REF-TEST-GF-3-1" TO PAR-NAME.                        NC2244.2
046500     IF      TEST-3-DATA IN TEST-3-DATA-GRP (3:) = "CDEF"         NC2244.2
046600             PERFORM PASS                                         NC2244.2
046700             GO TO REF-WRITE-GF-3-1                               NC2244.2
046800     ELSE                                                         NC2244.2
046900             GO TO REF-FAIL-GF-3-1.                               NC2244.2
047000 REF-DELETE-GF-3-1.                                               NC2244.2
047100     PERFORM DE-LETE.                                             NC2244.2
047200     GO TO REF-WRITE-GF-3-1.                                      NC2244.2
047300 REF-FAIL-GF-3-1.                                                 NC2244.2
047400     MOVE   "CDEF" TO CORRECT-X.                                  NC2244.2
047500     MOVE    TEST-3-DATA IN TEST-3-DATA-GRP (3:) TO COMPUTED-X.   NC2244.2
047600     MOVE   "INCORRECT REFERENCE MODIFICATION" TO RE-MARK.        NC2244.2
047700     PERFORM FAIL.                                                NC2244.2
047800 REF-WRITE-GF-3-1.                                                NC2244.2
047900     PERFORM PRINT-DETAIL.                                        NC2244.2
048000*                                                                 NC2244.2
048100 REF-TEST-GF-3-2.                                                 NC2244.2
048200     ADD     1       TO REC-CT.                                   NC2244.2
048300     MOVE   "REF-TEST-GF-3-2" TO PAR-NAME.                        NC2244.2
048400     IF      TEST-3-DATA IN TEST-3-DATA-GRP (2: WS-3) = "BCD"     NC2244.2
048500             PERFORM PASS                                         NC2244.2
048600             GO TO REF-WRITE-GF-3-2                               NC2244.2
048700     ELSE                                                         NC2244.2
048800             GO TO REF-FAIL-GF-3-2.                               NC2244.2
048900 REF-DELETE-GF-3-2.                                               NC2244.2
049000     PERFORM DE-LETE.                                             NC2244.2
049100     GO TO REF-WRITE-GF-3-2.                                      NC2244.2
049200 REF-FAIL-GF-3-2.                                                 NC2244.2
049300     MOVE   "BCD" TO CORRECT-X.                                   NC2244.2
049400     MOVE    TEST-3-DATA IN TEST-3-DATA-GRP (2: WS-3)             NC2244.2
049500          TO COMPUTED-X.                                          NC2244.2
049600     MOVE   "INCORRECT REFERENCE MODIFICATION" TO RE-MARK.        NC2244.2
049700     PERFORM FAIL.                                                NC2244.2
049800 REF-WRITE-GF-3-2.                                                NC2244.2
049900     PERFORM PRINT-DETAIL.                                        NC2244.2
050000*                                                                 NC2244.2
050100 REF-TEST-GF-3-3.                                                 NC2244.2
050200     ADD     1       TO REC-CT.                                   NC2244.2
050300     MOVE   "REF-TEST-GF-3-3" TO PAR-NAME.                        NC2244.2
050400     IF      TEST-3-DATA IN TEST-3-DATA-GRP (10 - 7: 6 + 2 - 5)   NC2244.2
050500           = "CDE"                                                NC2244.2
050600             PERFORM PASS                                         NC2244.2
050700             GO TO REF-WRITE-GF-3-3                               NC2244.2
050800     ELSE                                                         NC2244.2
050900             GO TO REF-FAIL-GF-3-3.                               NC2244.2
051000 REF-DELETE-GF-3-3.                                               NC2244.2
051100     PERFORM DE-LETE.                                             NC2244.2
051200     GO TO REF-WRITE-GF-3-3.                                      NC2244.2
051300 REF-FAIL-GF-3-3.                                                 NC2244.2
051400     MOVE   "CDE" TO CORRECT-X.                                   NC2244.2
051500     MOVE    TEST-3-DATA IN TEST-3-DATA-GRP (10 - 7: 6 + 2 - 5)   NC2244.2
051600          TO COMPUTED-X                                           NC2244.2
051700     MOVE   "INCORRECT REFERENCE MODIFICATION" TO RE-MARK         NC2244.2
051800     PERFORM FAIL.                                                NC2244.2
051900 REF-WRITE-GF-3-3.                                                NC2244.2
052000     PERFORM PRINT-DETAIL.                                        NC2244.2
052100*                                                                 NC2244.2
052200 REF-INIT-GF-4.                                                   NC2244.2
052300     MOVE   "IV-22 4.3.8.3" TO ANSI-REFERENCE.                    NC2244.2
052400     MOVE   "ABCDEF" TO TEST-4-DATA.                              NC2244.2
052500     MOVE    1       TO REC-CT.                                   NC2244.2
052600     GO TO   REF-TEST-GF-4-1.                                     NC2244.2
052700 REF-DELETE-GF-4.                                                 NC2244.2
052800     PERFORM DE-LETE.                                             NC2244.2
052900     PERFORM PRINT-DETAIL.                                        NC2244.2
053000     GO TO   REF-INIT-GF-5.                                       NC2244.2
053100 REF-TEST-GF-4-1.                                                 NC2244.2
053200     MOVE   "REF-TEST-GF-4-1" TO PAR-NAME.                        NC2244.2
053300     IF      TEST-4-DATA (3:) = " CD EF"                          NC2244.2
053400             PERFORM PASS                                         NC2244.2
053500             GO TO REF-WRITE-GF-4-1                               NC2244.2
053600     ELSE                                                         NC2244.2
053700             GO TO REF-FAIL-GF-4-1.                               NC2244.2
053800 REF-DELETE-GF-4-1.                                               NC2244.2
053900     PERFORM DE-LETE.                                             NC2244.2
054000     GO TO REF-WRITE-GF-4-1.                                      NC2244.2
054100 REF-FAIL-GF-4-1.                                                 NC2244.2
054200     MOVE   " CD EF" TO CORRECT-X                                 NC2244.2
054300     MOVE    TEST-4-DATA (3:) TO COMPUTED-X                       NC2244.2
054400     MOVE   "INCORRECT REFERENCE MODIFICATION" TO RE-MARK         NC2244.2
054500     PERFORM FAIL.                                                NC2244.2
054600 REF-WRITE-GF-4-1.                                                NC2244.2
054700     PERFORM PRINT-DETAIL.                                        NC2244.2
054800*                                                                 NC2244.2
054900 REF-TEST-GF-4-2.                                                 NC2244.2
055000     ADD     1       TO REC-CT.                                   NC2244.2
055100     MOVE   "REF-TEST-GF-4-2" TO PAR-NAME.                        NC2244.2
055200     IF      TEST-4-DATA (WS-2: WS-3) = "B C"                     NC2244.2
055300             PERFORM PASS                                         NC2244.2
055400             GO TO REF-WRITE-GF-4-2                               NC2244.2
055500     ELSE                                                         NC2244.2
055600             GO TO REF-FAIL-GF-4-2.                               NC2244.2
055700 REF-DELETE-GF-4-2.                                               NC2244.2
055800     PERFORM DE-LETE.                                             NC2244.2
055900     GO TO REF-WRITE-GF-4-2.                                      NC2244.2
056000 REF-FAIL-GF-4-2.                                                 NC2244.2
056100     MOVE   "B C" TO CORRECT-X                                    NC2244.2
056200     MOVE    TEST-4-DATA (WS-2: WS-3) TO COMPUTED-X               NC2244.2
056300     MOVE   "INCORRECT REFERENCE MODIFICATION" TO RE-MARK         NC2244.2
056400     PERFORM FAIL.                                                NC2244.2
056500 REF-WRITE-GF-4-2.                                                NC2244.2
056600     PERFORM PRINT-DETAIL.                                        NC2244.2
056700*                                                                 NC2244.2
056800 REF-TEST-GF-4-3.                                                 NC2244.2
056900     ADD     1       TO REC-CT.                                   NC2244.2
057000     MOVE   "REF-TEST-GF-4-3" TO PAR-NAME.                        NC2244.2
057100     IF      TEST-4-DATA (10 - 7: 6 + 2 - 5) = " CD"              NC2244.2
057200             PERFORM PASS                                         NC2244.2
057300             GO TO REF-WRITE-GF-4-3                               NC2244.2
057400     ELSE                                                         NC2244.2
057500             GO TO REF-FAIL-GF-4-3.                               NC2244.2
057600 REF-DELETE-GF-4-3.                                               NC2244.2
057700     PERFORM DE-LETE.                                             NC2244.2
057800     GO TO REF-WRITE-GF-4-3.                                      NC2244.2
057900 REF-FAIL-GF-4-3.                                                 NC2244.2
058000     MOVE   " CD" TO CORRECT-X                                    NC2244.2
058100     MOVE    TEST-4-DATA (10 - 7: 6 + 2 - 5)                      NC2244.2
058200          TO COMPUTED-X                                           NC2244.2
058300     MOVE   "INCORRECT REFERENCE MODIFICATION" TO RE-MARK         NC2244.2
058400     PERFORM FAIL.                                                NC2244.2
058500 REF-WRITE-GF-4-3.                                                NC2244.2
058600     PERFORM PRINT-DETAIL.                                        NC2244.2
058700*                                                                 NC2244.2
058800 REF-INIT-GF-5.                                                   NC2244.2
058900*    ===-->  SUBSCRIPTED DATA-NAME  <--===                        NC2244.2
059000     MOVE   "IV-22 4.3.8.3.3 SR4" TO ANSI-REFERENCE.              NC2244.2
059100     MOVE    12345678 TO TABLE-1 (3 2).                           NC2244.2
059200     MOVE    1       TO REC-CT.                                   NC2244.2
059300     GO TO   REF-TEST-GF-5-1.                                     NC2244.2
059400 REF-DELETE-GF-5.                                                 NC2244.2
059500     PERFORM DE-LETE.                                             NC2244.2
059600     PERFORM PRINT-DETAIL.                                        NC2244.2
059700     GO TO   REF-INIT-GF-6.                                       NC2244.2
059800 REF-TEST-GF-5-1.                                                 NC2244.2
059900     MOVE   "REF-TEST-GF-5-1" TO PAR-NAME.                        NC2244.2
060000     IF      TABLE-1 (3 2) (2: 5) = 23456                         NC2244.2
060100             PERFORM PASS                                         NC2244.2
060200             GO TO REF-WRITE-GF-5-1                               NC2244.2
060300     ELSE                                                         NC2244.2
060400             GO TO REF-FAIL-GF-5-1.                               NC2244.2
060500 REF-DELETE-GF-5-1.                                               NC2244.2
060600     PERFORM DE-LETE.                                             NC2244.2
060700     GO TO REF-WRITE-GF-5-1.                                      NC2244.2
060800 REF-FAIL-GF-5-1.                                                 NC2244.2
060900     MOVE    23456   TO CORRECT-N                                 NC2244.2
061000     MOVE    TABLE-1 (3 2) (2: 5) TO COMPUTED-X                   NC2244.2
061100     MOVE   "INCORRECT REFERENCE MODIFICATION" TO RE-MARK         NC2244.2
061200     PERFORM FAIL.                                                NC2244.2
061300 REF-WRITE-GF-5-1.                                                NC2244.2
061400     PERFORM PRINT-DETAIL.                                        NC2244.2
061500*                                                                 NC2244.2
061600 REF-INIT-GF-6.                                                   NC2244.2
061700*    ===-->  QUALIFIED DATA-NAME  <--===                          NC2244.2
061800     MOVE   "IV-22 4.3.8.3.3 SR4" TO ANSI-REFERENCE.              NC2244.2
061900     MOVE   "OPQRST" TO TEST-3-DATA OF TEST-5-TABLE.              NC2244.2
062000     MOVE    1       TO REC-CT.                                   NC2244.2
062100     GO TO   REF-TEST-GF-6-1.                                     NC2244.2
062200 REF-DELETE-GF-6.                                                 NC2244.2
062300     PERFORM DE-LETE.                                             NC2244.2
062400     PERFORM PRINT-DETAIL.                                        NC2244.2
062500     GO TO   CCVS-EXIT.                                           NC2244.2
062600 REF-TEST-GF-6-1.                                                 NC2244.2
062700     MOVE   "REF-TEST-GF-6-1" TO PAR-NAME.                        NC2244.2
062800     IF      TEST-3-DATA OF TEST-5-TABLE (2: 4) = "PQRS"          NC2244.2
062900             PERFORM PASS                                         NC2244.2
063000             GO TO   REF-WRITE-GF-6-1                             NC2244.2
063100     ELSE                                                         NC2244.2
063200             GO TO   REF-FAIL-GF-6-1.                             NC2244.2
063300 REF-DELETE-GF-6-1.                                               NC2244.2
063400     PERFORM DE-LETE.                                             NC2244.2
063500     GO TO   REF-WRITE-GF-6-1.                                    NC2244.2
063600 REF-FAIL-GF-6-1.                                                 NC2244.2
063700     MOVE   "PQRS"   TO CORRECT-X.                                NC2244.2
063800     MOVE    TEST-3-DATA OF TEST-5-TABLE (2: 4)                   NC2244.2
063900          TO COMPUTED-X                                           NC2244.2
064000     MOVE   "INCORRECT REFERENCE MODIFICATION" TO RE-MARK         NC2244.2
064100     PERFORM FAIL.                                                NC2244.2
064200 REF-WRITE-GF-6-1.                                                NC2244.2
064300     PERFORM PRINT-DETAIL.                                        NC2244.2
064400*                                                                 NC2244.2
064500 CCVS-EXIT SECTION.                                               NC2244.2
064600 CCVS-999999.                                                     NC2244.2
064700     GO TO CLOSE-FILES.                                           NC2244.2
      *END-OF,NC224A                                                                  
