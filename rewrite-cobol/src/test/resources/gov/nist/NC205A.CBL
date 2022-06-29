      *HEADER,COBOL,NC205A                                                            
000100 IDENTIFICATION DIVISION.                                         NC2054.2
000200 PROGRAM-ID.                                                      NC2054.2
000300     NC205A.                                                      NC2054.2
000400*                                                              *  NC2054.2
000500****************************************************************  NC2054.2
000600*                                                              *  NC2054.2
000700*    VALIDATION FOR:-                                          *  NC2054.2
000800*                                                              *  NC2054.2
000900*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2054.2
001000*                                                              *  NC2054.2
001100*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC2054.2
001200*                                                              *  NC2054.2
001300****************************************************************  NC2054.2
001400*                                                              *  NC2054.2
001500*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  NC2054.2
001600*                                                              *  NC2054.2
001700*        X-55  - SYSTEM PRINTER NAME.                          *  NC2054.2
001800*        X-82  - SOURCE COMPUTER NAME.                         *  NC2054.2
001900*        X-83  - OBJECT COMPUTER NAME.                         *  NC2054.2
002000*                                                              *  NC2054.2
002100****************************************************************  NC2054.2
002200*                                                              *  NC2054.2
002300*    PROGRAM NC205A TESTS THE CONTINUATION OF COBOL WORDS,     *  NC2054.2
002400*    NUMERIC AND NON-NUMERIC LITERALS AND PICTURE STRINGS USING*  NC2054.2
002500*    A HYPHEN IN THE INDICATOR AREA OF CONTINUATION LINES.     *  NC2054.2
002600*                                                              *  NC2054.2
002700****************************************************************  NC2054.2
002800 ENVIRONMENT DIVISION.                                            NC2054.2
002900 CONFIGURATION SECTION.                                           NC2054.2
003000 SOURCE-COMPUTER.                                                 NC2054.2
003100     XXXXX082.                                                    NC2054.2
003200 OBJECT-COMPUTER.                                                 NC2054.2
003300     XXXXX083.                                                    NC2054.2
003400 INPUT-OUTPUT SECTION.                                            NC2054.2
003500 FILE-CONTROL.                                                    NC2054.2
003600     SELECT PRINT-FILE ASSIGN TO                                  NC2054.2
003700     XXXXX055.                                                    NC2054.2
003800 DATA DIVISION.                                                   NC2054.2
003900 FILE SECTION.                                                    NC2054.2
004000 FD  PRINT-FILE.                                                  NC2054.2
004100 01  PRINT-REC PICTURE X(120).                                    NC2054.2
004200 01  DUMMY-RECORD PICTURE X(120).                                 NC2054.2
004300 WORKING-STORAGE SECTION.                                         NC2054.2
004400 77  WS-TEST-12-DATA                                              NC2054.2
004500                        PIC S9(                                   NC2054.2
004600-                              6)V9(6).                           NC2054.2
004700 77  PROCEDURE                                                    NC2054.2
004800-    DIVISION PICTURE X.                                          NC2054.2
004900 77  CONT-                                                        NC2054.2
005000-         A             PIC                                       NC2054.2
005100-                          TURE X(10) VAL                         NC2054.2
005200-                                        UE               "GOVERNMNC2054.2
005300-    "ENT".                                                       NC2054.2
005400 77  CONT-B                       PICTURE S9(5)V9(5) VALUE ZERO.  NC2054.2
005500 77  CONT-C                       PICTURE 9(8).                   NC2054.2
005600 77  CONT-D                       PICTURE 9(5).                   NC2054.2
005700 77  CONT-E                       PICTURE 9999.                   NC2054.2
005800 77  CONT-F                       PICTURE 9(5).                   NC2054.2
005900 77  CONT-88              PICTURE S99.                            NC2054.2
006000     88 GREATERZERO          VALUE -10.                           NC2054.2
006100     88 NEGATIVEZERO       VALUE +10.                             NC2054.2
006200 77                                                               NC2054.2
006300                                                                  NC2054.2
006400     SPACING-77                                                   NC2054.2
006500      PICTURE                                                     NC2054.2
006600                                                                  NC2054.2
006700     X(10)                                                   VALUENC2054.2
006800                                                                  NC2054.2
006900     "ABCDE12345".                                                NC2054.2
007000 77  SPACING-SEND       PICTURE 9(10) VALUE 1234567890.           NC2054.2
007100 77  SPACING-RECEIVE    PICTURE                                   NC2054.2
007200                                                                  NC2054.2
007300                                                                  NC2054.2
007400                                                                  NC2054.2
007500                                                                  NC2054.2
007600                                                                  NC2054.2
007700                                                                  NC2054.2
007800                                                                  NC2054.2
007900                                                                  NC2054.2
008000                                                                  NC2054.2
008100                                                                  NC2054.2
008200                                                                  NC2054.2
008300                                                                  NC2054.2
008400                                                                  NC2054.2
008500                                                                  NC2054.2
008600                                                                  NC2054.2
008700                                                                  NC2054.2
008800                                                                  NC2054.2
008900                                                                  NC2054.2
009000                                                                  NC2054.2
009100                                                                  NC2054.2
009200                                                                  NC2054.2
009300                                                                  NC2054.2
009400                                                                  NC2054.2
009500                                                                  NC2054.2
009600                                                                  NC2054.2
009700                                                                  NC2054.2
009800                                                                  NC2054.2
009900                                                                  NC2054.2
010000                                                                  NC2054.2
010100                                                                  NC2054.2
010200                                                                  NC2054.2
010300                                                                  NC2054.2
010400                                                                  NC2054.2
010500                                                                  NC2054.2
010600                                                                  NC2054.2
010700                                                                  NC2054.2
010800                                                                  NC2054.2
010900                                                                  NC2054.2
011000                                                                  NC2054.2
011100                                                                  NC2054.2
011200                                                                  NC2054.2
011300                                                                  NC2054.2
011400                                                                  NC2054.2
011500                                                                  NC2054.2
011600                                                                  NC2054.2
011700                                                                  NC2054.2
011800                                                                  NC2054.2
011900                                                                  NC2054.2
012000                                                                  NC2054.2
012100                                                                  NC2054.2
012200                                                                  NC2054.2
012300                                                                  NC2054.2
012400                                                                  NC2054.2
012500                                                                  NC2054.2
012600                                                                  NC2054.2
012700                                                                  NC2054.2
012800                                                                  NC2054.2
012900                                                                  NC2054.2
013000                                                                  NC2054.2
013100                                                                  NC2054.2
013200     9999999999.                                                  NC2054.2
013300 01  SPACING-01. 02 SPACING-02. 03 SPACING-03 PICTURE XX. 02      NC2054.2
013400     SPACING-2. 03 SPACING-3. 04 SPACING-4 PICTURE X(8).          NC2054.2
013500 01  CONT-G                                                       NC2054.2
013600-           RP.                                                   NC2054.2
013700                                  02 LEVEL-02.                    NC2054.2
013800                                                      03 LEVEL-03.NC2054.2
013900     04                                                           NC2054.2
014000       LEVEL-                                                     NC2054.2
014100-            04         PICTURE XXXXXXXXXX.                       NC2054.2
014200 01  TEST-RESULTS.                                                NC2054.2
014300     02 FILLER                   PIC X      VALUE SPACE.          NC2054.2
014400     02 FEATURE                  PIC X(20)  VALUE SPACE.          NC2054.2
014500     02 FILLER                   PIC X      VALUE SPACE.          NC2054.2
014600     02 P-OR-F                   PIC X(5)   VALUE SPACE.          NC2054.2
014700     02 FILLER                   PIC X      VALUE SPACE.          NC2054.2
014800     02  PAR-NAME.                                                NC2054.2
014900       03 FILLER                 PIC X(19)  VALUE SPACE.          NC2054.2
015000       03  PARDOT-X              PIC X      VALUE SPACE.          NC2054.2
015100       03 DOTVALUE               PIC 99     VALUE ZERO.           NC2054.2
015200     02 FILLER                   PIC X(8)   VALUE SPACE.          NC2054.2
015300     02 RE-MARK                  PIC X(61).                       NC2054.2
015400 01  TEST-COMPUTED.                                               NC2054.2
015500     02 FILLER                   PIC X(30)  VALUE SPACE.          NC2054.2
015600     02 FILLER                   PIC X(17)  VALUE                 NC2054.2
015700            "       COMPUTED=".                                   NC2054.2
015800     02 COMPUTED-X.                                               NC2054.2
015900     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          NC2054.2
016000     03 COMPUTED-N               REDEFINES COMPUTED-A             NC2054.2
016100                                 PIC -9(9).9(9).                  NC2054.2
016200     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         NC2054.2
016300     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     NC2054.2
016400     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     NC2054.2
016500     03       CM-18V0 REDEFINES COMPUTED-A.                       NC2054.2
016600         04 COMPUTED-18V0                    PIC -9(18).          NC2054.2
016700         04 FILLER                           PIC X.               NC2054.2
016800     03 FILLER PIC X(50) VALUE SPACE.                             NC2054.2
016900 01  TEST-CORRECT.                                                NC2054.2
017000     02 FILLER PIC X(30) VALUE SPACE.                             NC2054.2
017100     02 FILLER PIC X(17) VALUE "       CORRECT =".                NC2054.2
017200     02 CORRECT-X.                                                NC2054.2
017300     03 CORRECT-A                  PIC X(20) VALUE SPACE.         NC2054.2
017400     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      NC2054.2
017500     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         NC2054.2
017600     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     NC2054.2
017700     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     NC2054.2
017800     03      CR-18V0 REDEFINES CORRECT-A.                         NC2054.2
017900         04 CORRECT-18V0                     PIC -9(18).          NC2054.2
018000         04 FILLER                           PIC X.               NC2054.2
018100     03 FILLER PIC X(2) VALUE SPACE.                              NC2054.2
018200     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     NC2054.2
018300 01  CCVS-C-1.                                                    NC2054.2
018400     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PANC2054.2
018500-    "SS  PARAGRAPH-NAME                                          NC2054.2
018600-    "       REMARKS".                                            NC2054.2
018700     02 FILLER                     PIC X(20)    VALUE SPACE.      NC2054.2
018800 01  CCVS-C-2.                                                    NC2054.2
018900     02 FILLER                     PIC X        VALUE SPACE.      NC2054.2
019000     02 FILLER                     PIC X(6)     VALUE "TESTED".   NC2054.2
019100     02 FILLER                     PIC X(15)    VALUE SPACE.      NC2054.2
019200     02 FILLER                     PIC X(4)     VALUE "FAIL".     NC2054.2
019300     02 FILLER                     PIC X(94)    VALUE SPACE.      NC2054.2
019400 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       NC2054.2
019500 01  REC-CT                        PIC 99       VALUE ZERO.       NC2054.2
019600 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       NC2054.2
019700 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       NC2054.2
019800 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       NC2054.2
019900 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       NC2054.2
020000 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       NC2054.2
020100 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       NC2054.2
020200 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      NC2054.2
020300 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       NC2054.2
020400 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     NC2054.2
020500 01  CCVS-H-1.                                                    NC2054.2
020600     02  FILLER                    PIC X(39)    VALUE SPACES.     NC2054.2
020700     02  FILLER                    PIC X(42)    VALUE             NC2054.2
020800     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 NC2054.2
020900     02  FILLER                    PIC X(39)    VALUE SPACES.     NC2054.2
021000 01  CCVS-H-2A.                                                   NC2054.2
021100   02  FILLER                        PIC X(40)  VALUE SPACE.      NC2054.2
021200   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  NC2054.2
021300   02  FILLER                        PIC XXXX   VALUE             NC2054.2
021400     "4.2 ".                                                      NC2054.2
021500   02  FILLER                        PIC X(28)  VALUE             NC2054.2
021600            " COPY - NOT FOR DISTRIBUTION".                       NC2054.2
021700   02  FILLER                        PIC X(41)  VALUE SPACE.      NC2054.2
021800                                                                  NC2054.2
021900 01  CCVS-H-2B.                                                   NC2054.2
022000   02  FILLER                        PIC X(15)  VALUE             NC2054.2
022100            "TEST RESULT OF ".                                    NC2054.2
022200   02  TEST-ID                       PIC X(9).                    NC2054.2
022300   02  FILLER                        PIC X(4)   VALUE             NC2054.2
022400            " IN ".                                               NC2054.2
022500   02  FILLER                        PIC X(12)  VALUE             NC2054.2
022600     " HIGH       ".                                              NC2054.2
022700   02  FILLER                        PIC X(22)  VALUE             NC2054.2
022800            " LEVEL VALIDATION FOR ".                             NC2054.2
022900   02  FILLER                        PIC X(58)  VALUE             NC2054.2
023000     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2054.2
023100 01  CCVS-H-3.                                                    NC2054.2
023200     02  FILLER                      PIC X(34)  VALUE             NC2054.2
023300            " FOR OFFICIAL USE ONLY    ".                         NC2054.2
023400     02  FILLER                      PIC X(58)  VALUE             NC2054.2
023500     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC2054.2
023600     02  FILLER                      PIC X(28)  VALUE             NC2054.2
023700            "  COPYRIGHT   1985 ".                                NC2054.2
023800 01  CCVS-E-1.                                                    NC2054.2
023900     02 FILLER                       PIC X(52)  VALUE SPACE.      NC2054.2
024000     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              NC2054.2
024100     02 ID-AGAIN                     PIC X(9).                    NC2054.2
024200     02 FILLER                       PIC X(45)  VALUE SPACES.     NC2054.2
024300 01  CCVS-E-2.                                                    NC2054.2
024400     02  FILLER                      PIC X(31)  VALUE SPACE.      NC2054.2
024500     02  FILLER                      PIC X(21)  VALUE SPACE.      NC2054.2
024600     02 CCVS-E-2-2.                                               NC2054.2
024700         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      NC2054.2
024800         03 FILLER                   PIC X      VALUE SPACE.      NC2054.2
024900         03 ENDER-DESC               PIC X(44)  VALUE             NC2054.2
025000            "ERRORS ENCOUNTERED".                                 NC2054.2
025100 01  CCVS-E-3.                                                    NC2054.2
025200     02  FILLER                      PIC X(22)  VALUE             NC2054.2
025300            " FOR OFFICIAL USE ONLY".                             NC2054.2
025400     02  FILLER                      PIC X(12)  VALUE SPACE.      NC2054.2
025500     02  FILLER                      PIC X(58)  VALUE             NC2054.2
025600     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2054.2
025700     02  FILLER                      PIC X(13)  VALUE SPACE.      NC2054.2
025800     02 FILLER                       PIC X(15)  VALUE             NC2054.2
025900             " COPYRIGHT 1985".                                   NC2054.2
026000 01  CCVS-E-4.                                                    NC2054.2
026100     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      NC2054.2
026200     02 FILLER                       PIC X(4)   VALUE " OF ".     NC2054.2
026300     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      NC2054.2
026400     02 FILLER                       PIC X(40)  VALUE             NC2054.2
026500      "  TESTS WERE EXECUTED SUCCESSFULLY".                       NC2054.2
026600 01  XXINFO.                                                      NC2054.2
026700     02 FILLER                       PIC X(19)  VALUE             NC2054.2
026800            "*** INFORMATION ***".                                NC2054.2
026900     02 INFO-TEXT.                                                NC2054.2
027000       04 FILLER                     PIC X(8)   VALUE SPACE.      NC2054.2
027100       04 XXCOMPUTED                 PIC X(20).                   NC2054.2
027200       04 FILLER                     PIC X(5)   VALUE SPACE.      NC2054.2
027300       04 XXCORRECT                  PIC X(20).                   NC2054.2
027400     02 INF-ANSI-REFERENCE           PIC X(48).                   NC2054.2
027500 01  HYPHEN-LINE.                                                 NC2054.2
027600     02 FILLER  PIC IS X VALUE IS SPACE.                          NC2054.2
027700     02 FILLER  PIC IS X(65)    VALUE IS "************************NC2054.2
027800-    "*****************************************".                 NC2054.2
027900     02 FILLER  PIC IS X(54)    VALUE IS "************************NC2054.2
028000-    "******************************".                            NC2054.2
028100 01  CCVS-PGM-ID                     PIC X(9)   VALUE             NC2054.2
028200     "NC205A".                                                    NC2054.2
028300 PROCEDURE DIVISION.                                              NC2054.2
028400 CCVS1 SECTION.                                                   NC2054.2
028500 OPEN-FILES.                                                      NC2054.2
028600     OPEN     OUTPUT PRINT-FILE.                                  NC2054.2
028700     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   NC2054.2
028800     MOVE    SPACE TO TEST-RESULTS.                               NC2054.2
028900     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             NC2054.2
029000     GO TO CCVS1-EXIT.                                            NC2054.2
029100 CLOSE-FILES.                                                     NC2054.2
029200     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   NC2054.2
029300 TERMINATE-CCVS.                                                  NC2054.2
029400*    EXIT PROGRAM.                                                NC2054.2
029500*TERMINATE-CALL.                                                  NC2054.2
029600     STOP     RUN.                                                NC2054.2
029700 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         NC2054.2
029800 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           NC2054.2
029900 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          NC2054.2
030000 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      NC2054.2
030100     MOVE "****TEST DELETED****" TO RE-MARK.                      NC2054.2
030200 PRINT-DETAIL.                                                    NC2054.2
030300     IF REC-CT NOT EQUAL TO ZERO                                  NC2054.2
030400             MOVE "." TO PARDOT-X                                 NC2054.2
030500             MOVE REC-CT TO DOTVALUE.                             NC2054.2
030600     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      NC2054.2
030700     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               NC2054.2
030800        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 NC2054.2
030900          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 NC2054.2
031000     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              NC2054.2
031100     MOVE SPACE TO CORRECT-X.                                     NC2054.2
031200     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         NC2054.2
031300     MOVE     SPACE TO RE-MARK.                                   NC2054.2
031400 HEAD-ROUTINE.                                                    NC2054.2
031500     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC2054.2
031600     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC2054.2
031700     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC2054.2
031800     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC2054.2
031900 COLUMN-NAMES-ROUTINE.                                            NC2054.2
032000     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2054.2
032100     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2054.2
032200     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        NC2054.2
032300 END-ROUTINE.                                                     NC2054.2
032400     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.NC2054.2
032500 END-RTN-EXIT.                                                    NC2054.2
032600     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2054.2
032700 END-ROUTINE-1.                                                   NC2054.2
032800      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      NC2054.2
032900      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               NC2054.2
033000      ADD PASS-COUNTER TO ERROR-HOLD.                             NC2054.2
033100*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   NC2054.2
033200      MOVE PASS-COUNTER TO CCVS-E-4-1.                            NC2054.2
033300      MOVE ERROR-HOLD TO CCVS-E-4-2.                              NC2054.2
033400      MOVE CCVS-E-4 TO CCVS-E-2-2.                                NC2054.2
033500      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           NC2054.2
033600  END-ROUTINE-12.                                                 NC2054.2
033700      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        NC2054.2
033800     IF       ERROR-COUNTER IS EQUAL TO ZERO                      NC2054.2
033900         MOVE "NO " TO ERROR-TOTAL                                NC2054.2
034000         ELSE                                                     NC2054.2
034100         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       NC2054.2
034200     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           NC2054.2
034300     PERFORM WRITE-LINE.                                          NC2054.2
034400 END-ROUTINE-13.                                                  NC2054.2
034500     IF DELETE-COUNTER IS EQUAL TO ZERO                           NC2054.2
034600         MOVE "NO " TO ERROR-TOTAL  ELSE                          NC2054.2
034700         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      NC2054.2
034800     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   NC2054.2
034900     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2054.2
035000      IF   INSPECT-COUNTER EQUAL TO ZERO                          NC2054.2
035100          MOVE "NO " TO ERROR-TOTAL                               NC2054.2
035200      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   NC2054.2
035300      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            NC2054.2
035400      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          NC2054.2
035500     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2054.2
035600 WRITE-LINE.                                                      NC2054.2
035700     ADD 1 TO RECORD-COUNT.                                       NC2054.2
035800*    IF RECORD-COUNT GREATER 50                                   NC2054.2
035900*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          NC2054.2
036000*        MOVE SPACE TO DUMMY-RECORD                               NC2054.2
036100*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  NC2054.2
036200*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             NC2054.2
036300*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     NC2054.2
036400*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          NC2054.2
036500*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          NC2054.2
036600*        MOVE ZERO TO RECORD-COUNT.                               NC2054.2
036700     PERFORM WRT-LN.                                              NC2054.2
036800 WRT-LN.                                                          NC2054.2
036900     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               NC2054.2
037000     MOVE SPACE TO DUMMY-RECORD.                                  NC2054.2
037100 BLANK-LINE-PRINT.                                                NC2054.2
037200     PERFORM WRT-LN.                                              NC2054.2
037300 FAIL-ROUTINE.                                                    NC2054.2
037400     IF   COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE. NC2054.2
037500     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.NC2054.2
037600     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC2054.2
037700     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   NC2054.2
037800     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2054.2
037900     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC2054.2
038000     GO TO  FAIL-ROUTINE-EX.                                      NC2054.2
038100 FAIL-ROUTINE-WRITE.                                              NC2054.2
038200     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         NC2054.2
038300     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 NC2054.2
038400     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. NC2054.2
038500     MOVE   SPACES TO COR-ANSI-REFERENCE.                         NC2054.2
038600 FAIL-ROUTINE-EX. EXIT.                                           NC2054.2
038700 BAIL-OUT.                                                        NC2054.2
038800     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   NC2054.2
038900     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           NC2054.2
039000 BAIL-OUT-WRITE.                                                  NC2054.2
039100     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  NC2054.2
039200     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC2054.2
039300     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2054.2
039400     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC2054.2
039500 BAIL-OUT-EX. EXIT.                                               NC2054.2
039600 CCVS1-EXIT.                                                      NC2054.2
039700     EXIT.                                                        NC2054.2
039800 SECT-NC205A-001 SECTION.                                         NC2054.2
039900 CON-INIT-GF.                                                     NC2054.2
040000     MOVE   "CONTINUATION ---" TO FEATURE.                        NC2054.2
040100     MOVE   "IV-44 7.2.2" TO ANSI-REFERENCE.                      NC2054.2
040200     PERFORM  PRINT-DETAIL.                                       NC2054.2
040300 CON-INIT-GF-1.                                                   NC2054.2
040400     MOVE     "CON-TEST-GF-1" TO PAR-NAME                         NC2054.2
040500     MOVE     "  NUMERIC INTEGER" TO FEATURE.                     NC2054.2
040600 CON-TEST-GF-1.                                                   NC2054.2
040700     MOVE     4                                                   NC2054.2
040800-             5                                                   NC2054.2
040900-             6                                                   NC2054.2
041000-             7                                                   NC2054.2
041100-             8 TO CONT-B.                                        NC2054.2
041200     IF CONT-B EQUAL TO 45678                                     NC2054.2
041300              PERFORM PASS                                        NC2054.2
041400              GO TO CON-WRITE-GF-1.                               NC2054.2
041500     GO TO CON-FAIL-GF-1.                                         NC2054.2
041600 CON-DELETE-GF-1.                                                 NC2054.2
041700     PERFORM DE-LETE.                                             NC2054.2
041800     GO TO CON-WRITE-GF-1.                                        NC2054.2
041900 CON-FAIL-GF-1.                                                   NC2054.2
042000     PERFORM  FAIL.                                               NC2054.2
042100     MOVE     CONT-B TO COMPUTED-N.                               NC2054.2
042200     MOVE     45678 TO CORRECT-N.                                 NC2054.2
042300 CON-WRITE-GF-1.                                                  NC2054.2
042400     PERFORM  PRINT-DETAIL.                                       NC2054.2
042500*                                                                 NC2054.2
042600 CON-INIT-GF-2.                                                   NC2054.2
042700     MOVE     "CON-TEST-GF-2" TO PAR-NAME.                        NC2054.2
042800     MOVE     "  NUM NON-INTEGER" TO FEATURE.                     NC2054.2
042900 CON-TEST-GF-2.                                                   NC2054.2
043000     MOVE     -                                                   NC2054.2
043100-             9                                                   NC2054.2
043200-             9                                                   NC2054.2
043300-             9                                                   NC2054.2
043400-             .                                                   NC2054.2
043500-             7                                                   NC2054.2
043600-             7                                                   NC2054.2
043700-             7 TO CONT-B.                                        NC2054.2
043800     IF CONT-B EQUAL TO -999.777                                  NC2054.2
043900           PERFORM PASS                                           NC2054.2
044000           GO TO CON-WRITE-GF-2.                                  NC2054.2
044100     GO TO CON-FAIL-GF-2.                                         NC2054.2
044200 CON-DELETE-GF-2.                                                 NC2054.2
044300     PERFORM  DE-LETE.                                            NC2054.2
044400     GO TO CON-WRITE-GF-2.                                        NC2054.2
044500 CON-FAIL-GF-2.                                                   NC2054.2
044600     PERFORM  FAIL.                                               NC2054.2
044700     MOVE CONT-B   TO COMPUTED-N.                                 NC2054.2
044800     MOVE -999.777 TO CORRECT-N.                                  NC2054.2
044900 CON-WRITE-GF-2.                                                  NC2054.2
045000     PERFORM  PRINT-DETAIL.                                       NC2054.2
045100*                                                                 NC2054.2
045200*    N.B. CONTIN-TEST-3 HAS BEEN REMOVED, AND SUBSEQUENT          NC2054.2
045300*    TESTS HAVE BEEN RE-NUMBERED.                                 NC2054.2
045400 CON-INIT-GF-3.                                                   NC2054.2
045500     MOVE     "  COMP CONDITIONAL" TO FEATURE.                    NC2054.2
045600     MOVE     "CON-TEST-GF-3" TO PAR-NAME.                        NC2054.2
045700 CON-TEST-GF-3.                                                   NC2054.2
045800     MOVE     -10 TO CONT-B.                                      NC2054.2
045900     MOVE     10 TO CONT-C.                                       NC2054.2
046000     MOVE     1 TO CONT-D.                                        NC2054.2
046100     MOVE     0 TO CONT-E.                                        NC2054.2
046200     MOVE     10 TO CONT-F.                                       NC2054.2
046300     MOVE     -10 TO CONT-88.                                     NC2054.2
046400     IF       CONT-E EQUA                                         NC2054.2
046500-                        L TO ZERO                                NC2054.2
046600-                                 S AN                            NC2054.2
046700-                                     D GREATER                   NC2054.2
046800-                                              ZERO AND CONT-B    NC2054.2
046900              EQUAL TO CONT-C OR ((((((0                          NC2054.2
047000                                                                  NC2054.2
047100                                                                  NC2054.2
047200                                                                  NC2054.2
047300                                                                  NC2054.2
047400                                                                  NC2054.2
047500                                                                  NC2054.2
047600                                                                  NC2054.2
047700                                                                  NC2054.2
047800                                                                  NC2054.2
047900                                                                  NC2054.2
048000                                                                  NC2054.2
048100                                                                  NC2054.2
048200                                                                  NC2054.2
048300                                                                  NC2054.2
048400                                                                  NC2054.2
048500                                                                  NC2054.2
048600                                                                  NC2054.2
048700                                                                  NC2054.2
048800                                                                  NC2054.2
048900                                                                  NC2054.2
049000                                                                  NC2054.2
049100                                                                  NC2054.2
049200                                                                  NC2054.2
049300                                                                  NC2054.2
049400                                                                  NC2054.2
049500                                                                  NC2054.2
049600                                                                  NC2054.2
049700                                                                  NC2054.2
049800                                                                  NC2054.2
049900                                                                  NC2054.2
050000                                                                  NC2054.2
050100                                                                  NC2054.2
050200                                                                  NC2054.2
050300                                                                  NC2054.2
050400                                                                  NC2054.2
050500                                                                  NC2054.2
050600                                                                  NC2054.2
050700                                                                  NC2054.2
050800                                                                  NC2054.2
050900                                                                  NC2054.2
051000                                                                  NC2054.2
051100                                                                  NC2054.2
051200                                                                  NC2054.2
051300                                                                  NC2054.2
051400                                                                  NC2054.2
051500                                                                  NC2054.2
051600                                                                  NC2054.2
051700                                                                  NC2054.2
051800                                                                  NC2054.2
051900                                                                  NC2054.2
052000                                                                  NC2054.2
052100                                                                  NC2054.2
052200                                                                  NC2054.2
052300                                                                  NC2054.2
052400                                                                  NC2054.2
052500                                                                  NC2054.2
052600                                                                  NC2054.2
052700                                                                  NC2054.2
052800                                                                  NC2054.2
052900                                                                  NC2054.2
053000                                                                  NC2054.2
053100                                                                  NC2054.2
053200                                                                  NC2054.2
053300                                                                  NC2054.2
053400                                                                  NC2054.2
053500                                                                  NC2054.2
053600                                                                  NC2054.2
053700                                                                  NC2054.2
053800                                                                  NC2054.2
053900                                                                  NC2054.2
054000                                                                  NC2054.2
054100                                                                  NC2054.2
054200                                                                  NC2054.2
054300                                                                  NC2054.2
054400                                                                  NC2054.2
054500                                                                  NC2054.2
054600                                                                  NC2054.2
054700                                                                  NC2054.2
054800                                                                  NC2054.2
054900                                                                  NC2054.2
055000              - CONT-D EQUAL TO CONT-D O                          NC2054.2
055100-                                             R -11 + CONT-F))))))NC2054.2
055200              AND N                                               NC2054.2
055300-                            OT NEGATIVE                          NC2054.2
055400-                                       ZERO                      NC2054.2
055500              PERFORM PASS                                        NC2054.2
055600         EL                                                       NC2054.2
055700-          SE                                                     NC2054.2
055800              GO TO CON-FAIL-GF-3.                                NC2054.2
055900     GO       TO CON-WRITE-GF-3.                                  NC2054.2
056000 CON-DELETE-GF-3.                                                 NC2054.2
056100     PERFORM  DE-LETE.                                            NC2054.2
056200     GO TO CON-WRITE-GF-3.                                        NC2054.2
056300                                                                  NC2054.2
056400                                                                  NC2054.2
056500                                                                  NC2054.2
056600                                                                  NC2054.2
056700 CON-FAIL-GF-3.                                                   NC2054.2
056800     PERFORM FAIL.                                                NC2054.2
056900 CON-WRITE-GF-3.                                                  NC2054.2
057000     PERFORM  PRINT-DETAIL.                                       NC2054.2
057100*                                                                 NC2054.2
057200 CON-INIT-GF-4.                                                   NC2054.2
057300     MOVE     "  RESERVED WORDS" TO FEATURE                       NC2054.2
057400     MOVE     "CON-TEST-GF-4" TO PAR-NAME.                        NC2054.2
057500     MOVE     54321 TO CONT-D.                                    NC2054.2
057600     MOVE     12 TO CONT-E.                                       NC2054.2
057700     MOVE     1199997 TO CONT-C.                                  NC2054.2
057800 CON-TEST-GF-4.                                                   NC2054.2
057900     DIV                                                          NC2054.2
058000-       ID                                                        NC2054.2
058100-         E   CONT-E IN                                           NC2054.2
058200-                      TO CONT-C GIV                              NC2054.2
058300-                                   IN                            NC2054.2
058400-                                     G CONT-D ROUN               NC2054.2
058500-                                                  DE             NC2054.2
058600-                                                    D O          NC2054.2
058700-                                                       N SIZE ERRNC2054.2
058800-    OR       PERFOR                                              NC2054.2
058900-                   M PASS G                                      NC2054.2
059000-                           O T                                   NC2054.2
059100-                              O CON-WRITE-GF-4.                  NC2054.2
059200     GO       TO CON-FAIL-GF-4.                                   NC2054.2
059300 CON-DELETE-GF-4.                                                 NC2054.2
059400     PERFORM  DE-LETE.                                            NC2054.2
059500     GO       TO CON-WRITE-GF-4.                                  NC2054.2
059600 CON-FAIL-GF-4.                                                   NC2054.2
059700     PERFORM  FAIL.                                               NC2054.2
059800     MOVE     CONT-D TO COMPUTED-N.                               NC2054.2
059900     MOVE     54321 TO CORRECT-N.                                 NC2054.2
060000     MOVE     "SIZE ERROR EXPECTED" TO RE-MARK.                   NC2054.2
060100 CON-WRITE-GF-4.                                                  NC2054.2
060200     PERFORM  PRINT-DETAIL.                                       NC2054.2
060300*                                                                 NC2054.2
060400 CON-INIT-GF-5.                                                   NC2054.2
060500     MOVE     "  DATA-NAMES" TO FEATURE.                          NC2054.2
060600     MOVE     "CON-TEST-GF-5" TO PAR-NAME.                        NC2054.2
060700     MOVE     10000 TO CONT-D.                                    NC2054.2
060800     MOVE     1000 TO CONT-F.                                     NC2054.2
060900     MOVE     ZERO TO CONT-C.                                     NC2054.2
061000 CON-TEST-GF-5.                                                   NC2054.2
061100     IF       CONT                                                NC2054.2
061200-                 -D EQUAL TO 10000 ADD CONT                      NC2054.2
061300-                                           -D CONT               NC2054.2
061400-                                                  -F GIVING CONT-NC2054.2
061500-    C.                                                           NC2054.2
061600     IF       CONT-C EQUAL TO 11000                               NC2054.2
061700              PERFORM PASS GO TO CON-WRITE-GF-5.                  NC2054.2
061800     GO       TO CON-FAIL-GF-5.                                   NC2054.2
061900 CON-DELETE-GF-5.                                                 NC2054.2
062000     PERFORM  DE-LETE.                                            NC2054.2
062100     GO       TO CON-WRITE-GF-5.                                  NC2054.2
062200 CON-FAIL-GF-5.                                                   NC2054.2
062300     PERFORM  FAIL.                                               NC2054.2
062400     MOVE     CONT-C TO COMPUTED-A.                               NC2054.2
062500     MOVE     11000 TO CORRECT-A.                                 NC2054.2
062600 CON-WRITE-GF-5.                                                  NC2054.2
062700     PERFORM  PRINT-DETAIL.                                       NC2054.2
062800*                                                                 NC2054.2
062900 CON-TEST-GF-6.                                                   NC2054.2
063000     MOVE     "CON-TEST-GF-6" TO PAR-NAME.                        NC2054.2
063100     MOVE     "  PARAGRAPH-NAMES" TO FEATURE.                     NC2054.2
063200     PERFORM  PA                                                  NC2054.2
063300-               SS.                                               NC2054.2
063400     IF       P-OR-F NOT EQUAL TO "PASS" GO TO CON-FAIL-GF-6.     NC2054.2
063500     GO       TO CON                                              NC2054.2
063600-                   -WRITE-GF-6.                                  NC2054.2
063700 CON-TEST-GF-6-1.                                                 NC2054.2
063800     GO       TO CON-FAIL-GF-6.                                   NC2054.2
063900 CON-DELETE-GF-6.                                                 NC2054.2
064000     PERFORM  DE-LETE.                                            NC2054.2
064100     GO       TO CON-WRITE-GF-6.                                  NC2054.2
064200 CON-FAIL-GF-6.                                                   NC2054.2
064300     PERFORM  FAIL.                                               NC2054.2
064400     MOVE     "CNTD PARA-NAME NOT FOUND" TO RE-MARK.              NC2054.2
064500 CON-WRITE-GF-6.                                                  NC2054.2
064600     PERFORM  PRINT-DETAIL.                                       NC2054.2
064700*                                                                 NC2054.2
064800*    N.B. THE REFERENCE TO THE OLD TEST CALLED                    NC2054.2
064900*        CONTIN-TEST-8 HAS BEEN REMOVED.                          NC2054.2
065000*        NOTE TEST MOVED TO SQ215.                                NC2054.2
065100*                                                                 NC2054.2
065200 CON-INIT-GF-7.                                                   NC2054.2
065300     MOVE     "  RECORD, ITEM DESCR" TO FEATURE.                  NC2054.2
065400     MOVE     "CON-TEST-GF-7" TO PAR-NAME.                        NC2054.2
065500*     N.B.  CONT-A IS NOT EXPLICITLY INITIALISED HERE             NC2054.2
065600*           BECAUSE THE -VALUE IS- CLAUSE OF THE                  NC2054.2
065700*           DEFINITION IS UNDER TEST IN THE NEXT PARAGRAPH.       NC2054.2
065800 CON-TEST-GF-7.                                                   NC2054.2
065900     MOVE     CONT-A TO CONT-GRP.                                 NC2054.2
066000     IF LEVEL-04 EQUAL TO "GOVERNMENT"                            NC2054.2
066100              PERFORM PASS                                        NC2054.2
066200              GO TO CON-WRITE-GF-7.                               NC2054.2
066300     GO TO CON-FAIL-GF-7.                                         NC2054.2
066400 CON-DELETE-GF-7.                                                 NC2054.2
066500     PERFORM  DE-LETE.                                            NC2054.2
066600     GO       TO CON-WRITE-GF-7.                                  NC2054.2
066700 CON-FAIL-GF-7.                                                   NC2054.2
066800     PERFORM  FAIL.                                               NC2054.2
066900     MOVE     LEVEL-04 TO COMPUTED-A.                             NC2054.2
067000     MOVE     "GOVERNMENT"  TO CORRECT-A.                         NC2054.2
067100 CON-WRITE-GF-7.                                                  NC2054.2
067200     PERFORM  PRINT-DETAIL.                                       NC2054.2
067300 CON-INIT-GF-8.                                                   NC2054.2
067400     MOVE     "SPACES BETWEEN WORDS" TO FEATURE.                  NC2054.2
067500     MOVE     "CON-TEST-GF-10" TO PAR-NAME.                       NC2054.2
067600     MOVE     "ABCDE12345"     TO SPACING-77.                     NC2054.2
067700 CON-TEST-GF-8.                                                   NC2054.2
067800     MOVE     SPACING-77 TO SPACING-01.                           NC2054.2
067900     IF       SPACING-4 EQUAL TO "CDE12345"                       NC2054.2
068000              PERFORM PASS GO TO CON-WRITE-GF-8.                  NC2054.2
068100     GO       TO CON-FAIL-GF-8.                                   NC2054.2
068200 CON-DELETE-GF-8.                                                 NC2054.2
068300     PERFORM  DE-LETE.                                            NC2054.2
068400     GO       TO CON-WRITE-GF-8.                                  NC2054.2
068500 CON-FAIL-GF-8.                                                   NC2054.2
068600     PERFORM  FAIL.                                               NC2054.2
068700     MOVE     SPACING-4 TO COMPUTED-A.                            NC2054.2
068800     MOVE     "CDE12345" TO CORRECT-A.                            NC2054.2
068900 CON-WRITE-GF-8.                                                  NC2054.2
069000     PERFORM  PRINT-DETAIL.                                       NC2054.2
069100*                                                                 NC2054.2
069200 CON-INIT-GF-9.                                                   NC2054.2
069300     MOVE   "CON-WRITE-GF-9" TO PAR-NAME.                         NC2054.2
069400     MOVE     1234567890   TO SPACING-SEND.                       NC2054.2
069500     MOVE     SPACING-SEND TO SPACING-RECEIVE.                    NC2054.2
069600 CON-TEST-GF-9.                                                   NC2054.2
069700     IF       SPACING-RECEIVE EQUAL TO 1234567890                 NC2054.2
069800              PERFORM                                             NC2054.2
069900                                                                  NC2054.2
070000                                                                  NC2054.2
070100                                                                  NC2054.2
070200                                                                  NC2054.2
070300                                                                  NC2054.2
070400                                                                  NC2054.2
070500                                                                  NC2054.2
070600                                                                  NC2054.2
070700                                                                  NC2054.2
070800                                                                  NC2054.2
070900                                                                  NC2054.2
071000                                                                  NC2054.2
071100                                                                  NC2054.2
071200                                                                  NC2054.2
071300                                                                  NC2054.2
071400                                                                  NC2054.2
071500                                                                  NC2054.2
071600                                                                  NC2054.2
071700                                                                  NC2054.2
071800                                                                  NC2054.2
071900                                                                  NC2054.2
072000                                                                  NC2054.2
072100                                                                  NC2054.2
072200                                                                  NC2054.2
072300                                                                  NC2054.2
072400                                                                  NC2054.2
072500                                                                  NC2054.2
072600                                                                  NC2054.2
072700                                                                  NC2054.2
072800                                                                  NC2054.2
072900                                                                  NC2054.2
073000                                                                  NC2054.2
073100                                                                  NC2054.2
073200                                                                  NC2054.2
073300                                                                  NC2054.2
073400                                                                  NC2054.2
073500                                                                  NC2054.2
073600                                                                  NC2054.2
073700                                                                  NC2054.2
073800                                                                  NC2054.2
073900                                                                  NC2054.2
074000                                                                  NC2054.2
074100                                                                  NC2054.2
074200                                                                  NC2054.2
074300                                                                  NC2054.2
074400                                                                  NC2054.2
074500                                                                  NC2054.2
074600                                                                  NC2054.2
074700                                                                  NC2054.2
074800                                                                  NC2054.2
074900                                                                  NC2054.2
075000                                                                  NC2054.2
075100                                                                  NC2054.2
075200                                                                  NC2054.2
075300                                                                  NC2054.2
075400                                                                  NC2054.2
075500                                                                  NC2054.2
075600                                                                  NC2054.2
075700                                                                  NC2054.2
075800                                                                  NC2054.2
075900                                                                  NC2054.2
076000                                                                  NC2054.2
076100                                                                  NC2054.2
076200                                                                  NC2054.2
076300                                                                  NC2054.2
076400                                                                  NC2054.2
076500                                                                  NC2054.2
076600                                                                  NC2054.2
076700                                                                  NC2054.2
076800                                                                  NC2054.2
076900                      PA                                          NC2054.2
077000-                       SS GO TO CON-WRITE-GF-9.                  NC2054.2
077100     GO                 TO CON-FAIL-GF-9.                         NC2054.2
077200 CON-DELETE-GF-9.                                                 NC2054.2
077300     PERFORM  DE-LETE.                                            NC2054.2
077400     GO       TO CON-WRITE-GF-9.                                  NC2054.2
077500 CON-FAIL-GF-9.                                                   NC2054.2
077600     PERFORM  FAIL.                                               NC2054.2
077700     MOVE     SPACING-RECEIVE TO COMPUTED-18V0.                   NC2054.2
077800     MOVE     1234567890 TO CORRECT-18V0.                         NC2054.2
077900 CON-WRITE-GF-9.                                                  NC2054.2
078000     PERFORM PRINT-DETAIL.                                        NC2054.2
078100*                                                                 NC2054.2
078200 CON-INIT-GF-10.                                                  NC2054.2
078300*    ===-->  PICTURE CHARACTER STRING CONTINUED  <--===           NC2054.2
078400     MOVE   "IV-44 7.2.2"              TO ANSI-REFERENCE.         NC2054.2
078500     MOVE   "PICTURE STRING CONTINUED" TO FEATURE                 NC2054.2
078600     MOVE   "CON-TEST-GF-10"           TO PAR-NAME.               NC2054.2
078700 CON-TEST-GF-10-1.                                                NC2054.2
078800     MOVE    654321.987654 TO WS-TEST-12-DATA.                    NC2054.2
078900     IF WS-TEST-12-DATA = 654321.987654                           NC2054.2
079000             PERFORM PASS                                         NC2054.2
079100             GO TO CON-WRITE-GF-10.                               NC2054.2
079200     GO TO CON-FAIL-GF-10.                                        NC2054.2
079300 CON-DELETE-GF-10.                                                NC2054.2
079400     PERFORM DE-LETE.                                             NC2054.2
079500     GO TO   CON-WRITE-GF-10.                                     NC2054.2
079600 CON-FAIL-GF-10.                                                  NC2054.2
079700     PERFORM  FAIL.                                               NC2054.2
079800     MOVE     WS-TEST-12-DATA TO COMPUTED-N.                      NC2054.2
079900     MOVE     654321.987654   TO CORRECT-N.                       NC2054.2
080000 CON-WRITE-GF-10.                                                 NC2054.2
080100     PERFORM  PRINT-DETAIL.                                       NC2054.2
080200 CCVS-EXIT SECTION.                                               NC2054.2
080300 CCVS-999999.                                                     NC2054.2
080400     GO TO CLOSE-FILES.                                           NC2054.2
      *END-OF,NC205A                                                                  
