      *HEADER,COBOL,NC215A                                                            
000100 IDENTIFICATION DIVISION.                                         NC2154.2
000200 PROGRAM-ID.                                                      NC2154.2
000300     NC215A.                                                      NC2154.2
000400****************************************************************  NC2154.2
000500*                                                              *  NC2154.2
000600*    VALIDATION FOR:-                                          *  NC2154.2
000700*                                                              *  NC2154.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2154.2
000900*                                                              *  NC2154.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC2154.2
001100*                                                              *  NC2154.2
001200****************************************************************  NC2154.2
001300*                                                              *  NC2154.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  NC2154.2
001500*                                                              *  NC2154.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  NC2154.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  NC2154.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  NC2154.2
001900*                                                              *  NC2154.2
002000****************************************************************  NC2154.2
002100*                                                                 NC2154.2
002200*                                                              *  NC2154.2
002300*    PROGRAM NC215A TESTS THE LITERAL PHRASE OF THE "ALPHABET" *  NC2154.2
002400*    CLAUSE OF THE "SPECIAL-NAMES" PARAGRAPH AND THE           *  NC2154.2
002500*    "PROGRAM COLLATING SEQUENCE" OF THE "OBJECT COMPUTER      *  NC2154.2
002600*    PARAGRAPH.                                                *  NC2154.2
002700*                                                              *  NC2154.2
002800****************************************************************  NC2154.2
002900 ENVIRONMENT DIVISION.                                            NC2154.2
003000 CONFIGURATION SECTION.                                           NC2154.2
003100 SOURCE-COMPUTER.                                                 NC2154.2
003200     XXXXX082.                                                    NC2154.2
003300 OBJECT-COMPUTER.                                                 NC2154.2
003400     XXXXX083                                                     NC2154.2
003500     PROGRAM COLLATING SEQUENCE IS THE-WILD-ONE.                  NC2154.2
003600 SPECIAL-NAMES.                                                   NC2154.2
003700     ALPHABET                                                     NC2154.2
003800     THE-WILD-ONE IS "A" THRU "H" "I" ALSO "J", ALSO "K", ALSO    NC2154.2
003900     "L" ALSO "M" ALSO "N" "O" THROUGH "Z" "0" THRU "9",          NC2154.2
004000*                                                                 NC2154.2
004100*                                                                 NC2154.2
004200*ALPHABET-TEST-10     *****     THE WHOLE ALPHABET IS ONE LITERAL NC2154.2
004300*    WITH ALL 51 CHARACTERS IN THE COBOL CHARACTER SET.  TEST-10  NC2154.2
004400*    IS ONLY A SYNTAX CHECK ON                                    NC2154.2
004500*            ALPHABET-NAME IS LITERAL.                            NC2154.2
004600*                                                                 NC2154.2
004700*                                                                 NC2154.2
004800     ALPHABET                                                     NC2154.2
004900     THE-BIG-OL-LITERAL-ALPHABET IS "A+0B-1C*2D/3E=4Fl5G,6H;7I.8J"NC2154.2
005000-    ""9K(L)M>N<O PQRSTUVWXYZ".                                   NC2154.2
005100*                                                                 NC2154.2
005200*                                                                 NC2154.2
005300 INPUT-OUTPUT SECTION.                                            NC2154.2
005400 FILE-CONTROL.                                                    NC2154.2
005500     SELECT PRINT-FILE ASSIGN TO                                  NC2154.2
005600     XXXXX055.                                                    NC2154.2
005700 DATA DIVISION.                                                   NC2154.2
005800 FILE SECTION.                                                    NC2154.2
005900 FD  PRINT-FILE.                                                  NC2154.2
006000 01  PRINT-REC PICTURE X(120).                                    NC2154.2
006100 01  DUMMY-RECORD PICTURE X(120).                                 NC2154.2
006200 WORKING-STORAGE SECTION.                                         NC2154.2
006300 01  A-AN-1 PIC A VALUE "A".                                      NC2154.2
006400 01  B-AN-1 PIC A VALUE "B".                                      NC2154.2
006500 01  H-AN-1 PIC A VALUE "H".                                      NC2154.2
006600 01  I-AN-1 PIC A VALUE "I".                                      NC2154.2
006700 01  J-AN-1 PIC A VALUE "J".                                      NC2154.2
006800 01  K-AN-1 PIC A VALUE "K".                                      NC2154.2
006900 01  L-AN-1 PIC A VALUE "L".                                      NC2154.2
007000 01  M-AN-1 PIC A VALUE "M".                                      NC2154.2
007100 01  N-AN-1 PIC A VALUE "N".                                      NC2154.2
007200 01  O-AN-1 PIC A VALUE "O".                                      NC2154.2
007300 01  ZERO-DU-9V0-1 PIC 9 VALUE ZERO.                              NC2154.2
007400 01  NINE-DU-9V0-1 PIC 9 VALUE 9.                                 NC2154.2
007500 01  TEST-RESULTS.                                                NC2154.2
007600     02 FILLER                   PIC X      VALUE SPACE.          NC2154.2
007700     02 FEATURE                  PIC X(20)  VALUE SPACE.          NC2154.2
007800     02 FILLER                   PIC X      VALUE SPACE.          NC2154.2
007900     02 P-OR-F                   PIC X(5)   VALUE SPACE.          NC2154.2
008000     02 FILLER                   PIC X      VALUE SPACE.          NC2154.2
008100     02  PAR-NAME.                                                NC2154.2
008200       03 FILLER                 PIC X(19)  VALUE SPACE.          NC2154.2
008300       03  PARDOT-X              PIC X      VALUE SPACE.          NC2154.2
008400       03 DOTVALUE               PIC 99     VALUE ZERO.           NC2154.2
008500     02 FILLER                   PIC X(8)   VALUE SPACE.          NC2154.2
008600     02 RE-MARK                  PIC X(61).                       NC2154.2
008700 01  TEST-COMPUTED.                                               NC2154.2
008800     02 FILLER                   PIC X(30)  VALUE SPACE.          NC2154.2
008900     02 FILLER                   PIC X(17)  VALUE                 NC2154.2
009000            "       COMPUTED=".                                   NC2154.2
009100     02 COMPUTED-X.                                               NC2154.2
009200     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          NC2154.2
009300     03 COMPUTED-N               REDEFINES COMPUTED-A             NC2154.2
009400                                 PIC -9(9).9(9).                  NC2154.2
009500     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         NC2154.2
009600     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     NC2154.2
009700     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     NC2154.2
009800     03       CM-18V0 REDEFINES COMPUTED-A.                       NC2154.2
009900         04 COMPUTED-18V0                    PIC -9(18).          NC2154.2
010000         04 FILLER                           PIC X.               NC2154.2
010100     03 FILLER PIC X(50) VALUE SPACE.                             NC2154.2
010200 01  TEST-CORRECT.                                                NC2154.2
010300     02 FILLER PIC X(30) VALUE SPACE.                             NC2154.2
010400     02 FILLER PIC X(17) VALUE "       CORRECT =".                NC2154.2
010500     02 CORRECT-X.                                                NC2154.2
010600     03 CORRECT-A                  PIC X(20) VALUE SPACE.         NC2154.2
010700     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      NC2154.2
010800     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         NC2154.2
010900     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     NC2154.2
011000     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     NC2154.2
011100     03      CR-18V0 REDEFINES CORRECT-A.                         NC2154.2
011200         04 CORRECT-18V0                     PIC -9(18).          NC2154.2
011300         04 FILLER                           PIC X.               NC2154.2
011400     03 FILLER PIC X(2) VALUE SPACE.                              NC2154.2
011500     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     NC2154.2
011600 01  CCVS-C-1.                                                    NC2154.2
011700     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PANC2154.2
011800-    "SS  PARAGRAPH-NAME                                          NC2154.2
011900-    "       REMARKS".                                            NC2154.2
012000     02 FILLER                     PIC X(20)    VALUE SPACE.      NC2154.2
012100 01  CCVS-C-2.                                                    NC2154.2
012200     02 FILLER                     PIC X        VALUE SPACE.      NC2154.2
012300     02 FILLER                     PIC X(6)     VALUE "TESTED".   NC2154.2
012400     02 FILLER                     PIC X(15)    VALUE SPACE.      NC2154.2
012500     02 FILLER                     PIC X(4)     VALUE "FAIL".     NC2154.2
012600     02 FILLER                     PIC X(94)    VALUE SPACE.      NC2154.2
012700 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       NC2154.2
012800 01  REC-CT                        PIC 99       VALUE ZERO.       NC2154.2
012900 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       NC2154.2
013000 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       NC2154.2
013100 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       NC2154.2
013200 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       NC2154.2
013300 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       NC2154.2
013400 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       NC2154.2
013500 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      NC2154.2
013600 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       NC2154.2
013700 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     NC2154.2
013800 01  CCVS-H-1.                                                    NC2154.2
013900     02  FILLER                    PIC X(39)    VALUE SPACES.     NC2154.2
014000     02  FILLER                    PIC X(42)    VALUE             NC2154.2
014100     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 NC2154.2
014200     02  FILLER                    PIC X(39)    VALUE SPACES.     NC2154.2
014300 01  CCVS-H-2A.                                                   NC2154.2
014400   02  FILLER                        PIC X(40)  VALUE SPACE.      NC2154.2
014500   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  NC2154.2
014600   02  FILLER                        PIC XXXX   VALUE             NC2154.2
014700     "4.2 ".                                                      NC2154.2
014800   02  FILLER                        PIC X(28)  VALUE             NC2154.2
014900            " COPY - NOT FOR DISTRIBUTION".                       NC2154.2
015000   02  FILLER                        PIC X(41)  VALUE SPACE.      NC2154.2
015100                                                                  NC2154.2
015200 01  CCVS-H-2B.                                                   NC2154.2
015300   02  FILLER                        PIC X(15)  VALUE             NC2154.2
015400            "TEST RESULT OF ".                                    NC2154.2
015500   02  TEST-ID                       PIC X(9).                    NC2154.2
015600   02  FILLER                        PIC X(4)   VALUE             NC2154.2
015700            " IN ".                                               NC2154.2
015800   02  FILLER                        PIC X(12)  VALUE             NC2154.2
015900     " HIGH       ".                                              NC2154.2
016000   02  FILLER                        PIC X(22)  VALUE             NC2154.2
016100            " LEVEL VALIDATION FOR ".                             NC2154.2
016200   02  FILLER                        PIC X(58)  VALUE             NC2154.2
016300     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2154.2
016400 01  CCVS-H-3.                                                    NC2154.2
016500     02  FILLER                      PIC X(34)  VALUE             NC2154.2
016600            " FOR OFFICIAL USE ONLY    ".                         NC2154.2
016700     02  FILLER                      PIC X(58)  VALUE             NC2154.2
016800     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC2154.2
016900     02  FILLER                      PIC X(28)  VALUE             NC2154.2
017000            "  COPYRIGHT   1985 ".                                NC2154.2
017100 01  CCVS-E-1.                                                    NC2154.2
017200     02 FILLER                       PIC X(52)  VALUE SPACE.      NC2154.2
017300     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              NC2154.2
017400     02 ID-AGAIN                     PIC X(9).                    NC2154.2
017500     02 FILLER                       PIC X(45)  VALUE SPACES.     NC2154.2
017600 01  CCVS-E-2.                                                    NC2154.2
017700     02  FILLER                      PIC X(31)  VALUE SPACE.      NC2154.2
017800     02  FILLER                      PIC X(21)  VALUE SPACE.      NC2154.2
017900     02 CCVS-E-2-2.                                               NC2154.2
018000         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      NC2154.2
018100         03 FILLER                   PIC X      VALUE SPACE.      NC2154.2
018200         03 ENDER-DESC               PIC X(44)  VALUE             NC2154.2
018300            "ERRORS ENCOUNTERED".                                 NC2154.2
018400 01  CCVS-E-3.                                                    NC2154.2
018500     02  FILLER                      PIC X(22)  VALUE             NC2154.2
018600            " FOR OFFICIAL USE ONLY".                             NC2154.2
018700     02  FILLER                      PIC X(12)  VALUE SPACE.      NC2154.2
018800     02  FILLER                      PIC X(58)  VALUE             NC2154.2
018900     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2154.2
019000     02  FILLER                      PIC X(13)  VALUE SPACE.      NC2154.2
019100     02 FILLER                       PIC X(15)  VALUE             NC2154.2
019200             " COPYRIGHT 1985".                                   NC2154.2
019300 01  CCVS-E-4.                                                    NC2154.2
019400     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      NC2154.2
019500     02 FILLER                       PIC X(4)   VALUE " OF ".     NC2154.2
019600     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      NC2154.2
019700     02 FILLER                       PIC X(40)  VALUE             NC2154.2
019800      "  TESTS WERE EXECUTED SUCCESSFULLY".                       NC2154.2
019900 01  XXINFO.                                                      NC2154.2
020000     02 FILLER                       PIC X(19)  VALUE             NC2154.2
020100            "*** INFORMATION ***".                                NC2154.2
020200     02 INFO-TEXT.                                                NC2154.2
020300       04 FILLER                     PIC X(8)   VALUE SPACE.      NC2154.2
020400       04 XXCOMPUTED                 PIC X(20).                   NC2154.2
020500       04 FILLER                     PIC X(5)   VALUE SPACE.      NC2154.2
020600       04 XXCORRECT                  PIC X(20).                   NC2154.2
020700     02 INF-ANSI-REFERENCE           PIC X(48).                   NC2154.2
020800 01  HYPHEN-LINE.                                                 NC2154.2
020900     02 FILLER  PIC IS X VALUE IS SPACE.                          NC2154.2
021000     02 FILLER  PIC IS X(65)    VALUE IS "************************NC2154.2
021100-    "*****************************************".                 NC2154.2
021200     02 FILLER  PIC IS X(54)    VALUE IS "************************NC2154.2
021300-    "******************************".                            NC2154.2
021400 01  CCVS-PGM-ID                     PIC X(9)   VALUE             NC2154.2
021500     "NC215A".                                                    NC2154.2
021600 PROCEDURE DIVISION.                                              NC2154.2
021700 CCVS1 SECTION.                                                   NC2154.2
021800 OPEN-FILES.                                                      NC2154.2
021900     OPEN     OUTPUT PRINT-FILE.                                  NC2154.2
022000     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   NC2154.2
022100     MOVE    SPACE TO TEST-RESULTS.                               NC2154.2
022200     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             NC2154.2
022300     GO TO CCVS1-EXIT.                                            NC2154.2
022400 CLOSE-FILES.                                                     NC2154.2
022500     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   NC2154.2
022600 TERMINATE-CCVS.                                                  NC2154.2
022700*    EXIT PROGRAM.                                                NC2154.2
022800*TERMINATE-CALL.                                                  NC2154.2
022900     STOP     RUN.                                                NC2154.2
023000 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         NC2154.2
023100 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           NC2154.2
023200 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          NC2154.2
023300 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      NC2154.2
023400     MOVE "****TEST DELETED****" TO RE-MARK.                      NC2154.2
023500 PRINT-DETAIL.                                                    NC2154.2
023600     IF REC-CT NOT EQUAL TO ZERO                                  NC2154.2
023700             MOVE "." TO PARDOT-X                                 NC2154.2
023800             MOVE REC-CT TO DOTVALUE.                             NC2154.2
023900     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      NC2154.2
024000     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               NC2154.2
024100        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 NC2154.2
024200          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 NC2154.2
024300     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              NC2154.2
024400     MOVE SPACE TO CORRECT-X.                                     NC2154.2
024500     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         NC2154.2
024600     MOVE     SPACE TO RE-MARK.                                   NC2154.2
024700 HEAD-ROUTINE.                                                    NC2154.2
024800     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC2154.2
024900     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC2154.2
025000     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC2154.2
025100     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC2154.2
025200 COLUMN-NAMES-ROUTINE.                                            NC2154.2
025300     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2154.2
025400     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2154.2
025500     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        NC2154.2
025600 END-ROUTINE.                                                     NC2154.2
025700     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.NC2154.2
025800 END-RTN-EXIT.                                                    NC2154.2
025900     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2154.2
026000 END-ROUTINE-1.                                                   NC2154.2
026100      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      NC2154.2
026200      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               NC2154.2
026300      ADD PASS-COUNTER TO ERROR-HOLD.                             NC2154.2
026400*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   NC2154.2
026500      MOVE PASS-COUNTER TO CCVS-E-4-1.                            NC2154.2
026600      MOVE ERROR-HOLD TO CCVS-E-4-2.                              NC2154.2
026700      MOVE CCVS-E-4 TO CCVS-E-2-2.                                NC2154.2
026800      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           NC2154.2
026900  END-ROUTINE-12.                                                 NC2154.2
027000      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        NC2154.2
027100     IF       ERROR-COUNTER IS EQUAL TO ZERO                      NC2154.2
027200         MOVE "NO " TO ERROR-TOTAL                                NC2154.2
027300         ELSE                                                     NC2154.2
027400         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       NC2154.2
027500     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           NC2154.2
027600     PERFORM WRITE-LINE.                                          NC2154.2
027700 END-ROUTINE-13.                                                  NC2154.2
027800     IF DELETE-COUNTER IS EQUAL TO ZERO                           NC2154.2
027900         MOVE "NO " TO ERROR-TOTAL  ELSE                          NC2154.2
028000         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      NC2154.2
028100     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   NC2154.2
028200     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2154.2
028300      IF   INSPECT-COUNTER EQUAL TO ZERO                          NC2154.2
028400          MOVE "NO " TO ERROR-TOTAL                               NC2154.2
028500      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   NC2154.2
028600      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            NC2154.2
028700      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          NC2154.2
028800     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2154.2
028900 WRITE-LINE.                                                      NC2154.2
029000     ADD 1 TO RECORD-COUNT.                                       NC2154.2
029100*    IF RECORD-COUNT GREATER 50                                   NC2154.2
029200*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          NC2154.2
029300*        MOVE SPACE TO DUMMY-RECORD                               NC2154.2
029400*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  NC2154.2
029500*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             NC2154.2
029600*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     NC2154.2
029700*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          NC2154.2
029800*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          NC2154.2
029900*        MOVE ZERO TO RECORD-COUNT.                               NC2154.2
030000     PERFORM WRT-LN.                                              NC2154.2
030100 WRT-LN.                                                          NC2154.2
030200     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               NC2154.2
030300     MOVE SPACE TO DUMMY-RECORD.                                  NC2154.2
030400 BLANK-LINE-PRINT.                                                NC2154.2
030500     PERFORM WRT-LN.                                              NC2154.2
030600 FAIL-ROUTINE.                                                    NC2154.2
030700     IF   COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE. NC2154.2
030800     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.NC2154.2
030900     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC2154.2
031000     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   NC2154.2
031100     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2154.2
031200     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC2154.2
031300     GO TO  FAIL-ROUTINE-EX.                                      NC2154.2
031400 FAIL-ROUTINE-WRITE.                                              NC2154.2
031500     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         NC2154.2
031600     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 NC2154.2
031700     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. NC2154.2
031800     MOVE   SPACES TO COR-ANSI-REFERENCE.                         NC2154.2
031900 FAIL-ROUTINE-EX. EXIT.                                           NC2154.2
032000 BAIL-OUT.                                                        NC2154.2
032100     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   NC2154.2
032200     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           NC2154.2
032300 BAIL-OUT-WRITE.                                                  NC2154.2
032400     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  NC2154.2
032500     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC2154.2
032600     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2154.2
032700     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC2154.2
032800 BAIL-OUT-EX. EXIT.                                               NC2154.2
032900 CCVS1-EXIT.                                                      NC2154.2
033000     EXIT.                                                        NC2154.2
033100 SECT-NC215A-001 SECTION.                                         NC2154.2
033200 SEQ-INIT-GF-1.                                                   NC2154.2
033300     MOVE      "SEQ-TEST-GF-1" TO PAR-NAME.                       NC2154.2
033400     MOVE "VI-15 4.5.4 GR4(D)" TO ANSI-REFERENCE.                 NC2154.2
033500     MOVE      "A = LOW-VALUE" TO FEATURE.                        NC2154.2
033600     MOVE                  "A" TO A-AN-1.                         NC2154.2
033700 SEQ-TEST-GF-1.                                                   NC2154.2
033800     IF      A-AN-1 EQUAL TO LOW-VALUE PERFORM PASS               NC2154.2
033900         ELSE                                                     NC2154.2
034000         GO TO SEQ-FAIL-GF-1.                                     NC2154.2
034100     GO TO SEQ-WRITE-GF-1.                                        NC2154.2
034200 SEQ-DELETE-GF-1.                                                 NC2154.2
034300     PERFORM DE-LETE.                                             NC2154.2
034400     GO TO SEQ-WRITE-GF-1.                                        NC2154.2
034500 SEQ-FAIL-GF-1.                                                   NC2154.2
034600     MOVE "A NOT = LOW-VALUE" TO COMPUTED-A.                      NC2154.2
034700     PERFORM FAIL.                                                NC2154.2
034800 SEQ-WRITE-GF-1.                                                  NC2154.2
034900     PERFORM PRINT-DETAIL.                                        NC2154.2
035000*                                                                 NC2154.2
035100 SEQ-INIT-GF-2.                                                   NC2154.2
035200     MOVE      "SEQ-TEST-GF-2" TO PAR-NAME.                       NC2154.2
035300     MOVE "VI-15 4.5.4 GR4(D)" TO ANSI-REFERENCE.                 NC2154.2
035400     MOVE   "H I J B SEQUENCE" TO FEATURE.                        NC2154.2
035500     MOVE                  "H" TO H-AN-1.                         NC2154.2
035600     MOVE                  "I" TO I-AN-1.                         NC2154.2
035700     MOVE                  "J" TO J-AN-1.                         NC2154.2
035800     MOVE                  "B" TO B-AN-1.                         NC2154.2
035900 SEQ-TEST-GF-2.                                                   NC2154.2
036000     IF H-AN-1 < I-AN-1 AND J-AN-1 > B-AN-1 PERFORM PASS          NC2154.2
036100         ELSE                                                     NC2154.2
036200         GO TO SEQ-FAIL-GF-2.                                     NC2154.2
036300     GO TO SEQ-WRITE-GF-2.                                        NC2154.2
036400 SEQ-DELETE-GF-2.                                                 NC2154.2
036500     PERFORM DE-LETE.                                             NC2154.2
036600     GO TO SEQ-WRITE-GF-2.                                        NC2154.2
036700 SEQ-FAIL-GF-2.                                                   NC2154.2
036800     MOVE "H I J B NOT SEQUENCED" TO COMPUTED-A.                  NC2154.2
036900     PERFORM FAIL.                                                NC2154.2
037000 SEQ-WRITE-GF-2.                                                  NC2154.2
037100     PERFORM PRINT-DETAIL.                                        NC2154.2
037200*                                                                 NC2154.2
037300 SEQ-INIT-GF-3.                                                   NC2154.2
037400     MOVE      "SEQ-TEST-GF-3" TO PAR-NAME.                       NC2154.2
037500     MOVE "VI-15 4.5.4 GR4(D)" TO ANSI-REFERENCE.                 NC2154.2
037600     MOVE  "I J K L M N EQUAL" TO FEATURE.                        NC2154.2
037700     MOVE                  "I" TO I-AN-1.                         NC2154.2
037800     MOVE                  "J" TO J-AN-1.                         NC2154.2
037900     MOVE                  "K" TO K-AN-1.                         NC2154.2
038000     MOVE                  "L" TO L-AN-1.                         NC2154.2
038100     MOVE                  "M" TO M-AN-1.                         NC2154.2
038200     MOVE                  "N" TO N-AN-1.                         NC2154.2
038300 SEQ-TEST-GF-3.                                                   NC2154.2
038400     IF I-AN-1 = J-AN-1 AND K-AN-1 AND L-AN-1 AND M-AN-1          NC2154.2
038500         AND N-AN-1 PERFORM PASS                                  NC2154.2
038600     ELSE                                                         NC2154.2
038700         GO TO SEQ-FAIL-GF-3.                                     NC2154.2
038800     GO TO SEQ-WRITE-GF-3.                                        NC2154.2
038900 SEQ-DELETE-GF-3.                                                 NC2154.2
039000     PERFORM DE-LETE.                                             NC2154.2
039100     GO TO SEQ-WRITE-GF-3.                                        NC2154.2
039200 SEQ-FAIL-GF-3.                                                   NC2154.2
039300     MOVE    "I J K L M N NOT =" TO COMPUTED-A.                   NC2154.2
039400     PERFORM FAIL.                                                NC2154.2
039500 SEQ-WRITE-GF-3.                                                  NC2154.2
039600     PERFORM PRINT-DETAIL.                                        NC2154.2
039700*                                                                 NC2154.2
039800 SEQ-INIT-GF-4.                                                   NC2154.2
039900     MOVE      "SEQ-TEST-GF-4" TO PAR-NAME.                       NC2154.2
040000     MOVE "VI-15 4.5.4 GR4(D)" TO ANSI-REFERENCE.                 NC2154.2
040100     MOVE         "O > THAN N" TO FEATURE.                        NC2154.2
040200     MOVE                  "O" TO O-AN-1.                         NC2154.2
040300     MOVE                  "N" TO N-AN-1.                         NC2154.2
040400 SEQ-TEST-GF-4.                                                   NC2154.2
040500     IF O-AN-1 > N-AN-1 PERFORM PASS                              NC2154.2
040600         ELSE                                                     NC2154.2
040700         GO TO SEQ-FAIL-GF-4.                                     NC2154.2
040800     GO TO SEQ-WRITE-GF-4.                                        NC2154.2
040900 SEQ-DELETE-GF-4.                                                 NC2154.2
041000     PERFORM DE-LETE.                                             NC2154.2
041100     GO TO SEQ-WRITE-GF-4.                                        NC2154.2
041200 SEQ-FAIL-GF-4.                                                   NC2154.2
041300     MOVE "O NOT > THAN N" TO COMPUTED-A.                         NC2154.2
041400     PERFORM FAIL.                                                NC2154.2
041500 SEQ-WRITE-GF-4.                                                  NC2154.2
041600     PERFORM PRINT-DETAIL.                                        NC2154.2
041700*                                                                 NC2154.2
041800 SEQ-INIT-GF-5.                                                   NC2154.2
041900     MOVE      "SEQ-TEST-GF-5" TO PAR-NAME.                       NC2154.2
042000     MOVE "VI-15 4.5.4 GR4(D)" TO ANSI-REFERENCE.                 NC2154.2
042100     MOVE      "A < THAN ZERO" TO FEATURE.                        NC2154.2
042200     MOVE                  "A" TO A-AN-1.                         NC2154.2
042300     MOVE                 ZERO TO ZERO-DU-9V0-1.                  NC2154.2
042400 SEQ-TEST-GF-5.                                                   NC2154.2
042500     IF A-AN-1 < ZERO-DU-9V0-1 PERFORM PASS                       NC2154.2
042600         ELSE                                                     NC2154.2
042700         GO TO SEQ-FAIL-GF-5.                                     NC2154.2
042800     GO      TO SEQ-WRITE-GF-5.                                   NC2154.2
042900 SEQ-DELETE-GF-5.                                                 NC2154.2
043000     PERFORM DE-LETE.                                             NC2154.2
043100     GO TO SEQ-WRITE-GF-5.                                        NC2154.2
043200 SEQ-FAIL-GF-5.                                                   NC2154.2
043300     MOVE "A FOUND > THAN ZERO" TO COMPUTED-A.                    NC2154.2
043400     PERFORM FAIL.                                                NC2154.2
043500 SEQ-WRITE-GF-5.                                                  NC2154.2
043600     PERFORM PRINT-DETAIL.                                        NC2154.2
043700*                                                                 NC2154.2
043800 SEQ-INIT-GF-6.                                                   NC2154.2
043900     MOVE      "SEQ-TEST-GF-6" TO PAR-NAME.                       NC2154.2
044000     MOVE "VI-15 4.5.4 GR4(D)" TO ANSI-REFERENCE.                 NC2154.2
044100     MOVE  "NINE < THAN SPACE" TO FEATURE.                        NC2154.2
044200     MOVE                    9 TO NINE-DU-9V0-1.                  NC2154.2
044300 SEQ-TEST-GF-6.                                                   NC2154.2
044400     IF NINE-DU-9V0-1 < SPACE PERFORM PASS                        NC2154.2
044500         ELSE                                                     NC2154.2
044600         GO TO SEQ-FAIL-GF-6.                                     NC2154.2
044700     GO TO SEQ-WRITE-GF-6.                                        NC2154.2
044800 SEQ-DELETE-GF-6.                                                 NC2154.2
044900     PERFORM DE-LETE.                                             NC2154.2
045000     GO TO SEQ-WRITE-GF-6.                                        NC2154.2
045100 SEQ-FAIL-GF-6.                                                   NC2154.2
045200     MOVE "9 FOUND > THAN SPACE" TO COMPUTED-A.                   NC2154.2
045300     PERFORM FAIL.                                                NC2154.2
045400 SEQ-WRITE-GF-6.                                                  NC2154.2
045500     PERFORM PRINT-DETAIL.                                        NC2154.2
045600*                                                                 NC2154.2
045700 SEQ-INIT-GF-7.                                                   NC2154.2
045800     MOVE      "SEQ-TEST-GF-7" TO PAR-NAME.                       NC2154.2
045900     MOVE "VI-15 4.5.4 GR4(D)" TO ANSI-REFERENCE.                 NC2154.2
046000     MOVE  "NINE < THAN QUOTE" TO FEATURE.                        NC2154.2
046100     MOVE                    9 TO NINE-DU-9V0-1.                  NC2154.2
046200 SEQ-TEST-GF-7.                                                   NC2154.2
046300     IF NINE-DU-9V0-1 < QUOTE PERFORM PASS                        NC2154.2
046400         ELSE                                                     NC2154.2
046500         GO TO SEQ-FAIL-GF-7.                                     NC2154.2
046600     GO TO SEQ-WRITE-GF-7.                                        NC2154.2
046700 SEQ-DELETE-GF-7.                                                 NC2154.2
046800     PERFORM DE-LETE.                                             NC2154.2
046900     GO TO SEQ-WRITE-GF-7.                                        NC2154.2
047000 SEQ-FAIL-GF-7.                                                   NC2154.2
047100     MOVE "NINE FOUND >  QUOTE" TO COMPUTED-A.                    NC2154.2
047200     PERFORM FAIL.                                                NC2154.2
047300 SEQ-WRITE-GF-7.                                                  NC2154.2
047400     PERFORM PRINT-DETAIL.                                        NC2154.2
047500*                                                                 NC2154.2
047600 ALPHABET-TEST-10.                                                NC2154.2
047700     PERFORM END-ROUTINE.                                         NC2154.2
047800     MOVE    " ALPHABET-NAME     *****     CHECK THE ALPHABET-NAMENC2154.2
047900-    " IN THE SPECIAL-NAMES PARAGRAPH" TO TEST-RESULTS.           NC2154.2
048000     PERFORM PRINT-DETAIL.                                        NC2154.2
048100 CCVS-EXIT SECTION.                                               NC2154.2
048200 CCVS-999999.                                                     NC2154.2
048300     GO TO CLOSE-FILES.                                           NC2154.2
      *END-OF,NC215A                                                                  
