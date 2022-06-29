      *HEADER,COBOL,NC214M                                                            
000100 IDENTIFICATION DIVISION.                                         NC2144.2
000200 PROGRAM-ID.                                                      NC2144.2
000300     NC214M.                                                      NC2144.2
000400****************************************************************  NC2144.2
000500*                                                              *  NC2144.2
000600*    VALIDATION FOR:-                                          *  NC2144.2
000700*                                                              *  NC2144.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2144.2
000900*                                                              *  NC2144.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC2144.2
001100*                                                              *  NC2144.2
001200****************************************************************  NC2144.2
001300*                                                              *  NC2144.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  NC2144.2
001500*                                                              *  NC2144.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  NC2144.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  NC2144.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  NC2144.2
001900*                                                              *  NC2144.2
002000****************************************************************  NC2144.2
002100*                                                              *  NC2144.2
002200*    PROGRAM NC214M TESTS FORMAT 2 OF THE "ACCEPT" STATEMENT.  *  NC2144.2
002300*                                                              *  NC2144.2
002400****************************************************************  NC2144.2
002500 ENVIRONMENT DIVISION.                                            NC2144.2
002600 CONFIGURATION SECTION.                                           NC2144.2
002700 SOURCE-COMPUTER.                                                 NC2144.2
002800     XXXXX082.                                                    NC2144.2
002900 OBJECT-COMPUTER.                                                 NC2144.2
003000     XXXXX083                                                     NC2144.2
003100     PROGRAM COLLATING SEQUENCE IS N-A-T-I-V-E.                   NC2144.2
003200 SPECIAL-NAMES.                                                   NC2144.2
003300*                                                                 NC2144.2
003400*                                                                 NC2144.2
003500*    THE FOLLOWING IS THE ALPHABET FOR THE PROGRAM COLLATING      NC2144.2
003600*    SEQUENCE CLAUSE.                                             NC2144.2
003700*                                                                 NC2144.2
003800     ALPHABET                                                     NC2144.2
003900     N-A-T-I-V-E IS NATIVE                                        NC2144.2
004000*                                                                 NC2144.2
004100*                                                                 NC2144.2
004200*                                                                 NC2144.2
004300*                                                                 NC2144.2
004400     ALPHABET                                                     NC2144.2
004500     THE-ONE-CHARACTER-ALPHABET IS "Q" ALSO LOW-VALUE             NC2144.2
004600                                       ALSO HIGH-VALUE            NC2144.2
004700                                       ALSO QUOTE                 NC2144.2
004800                                       ALSO SPACES.               NC2144.2
004900*                                                                 NC2144.2
005000*                                                                 NC2144.2
005100*    COLLATING-AND-ALPHABET-TEST-9     *****     TEST OF SYNTAX   NC2144.2
005200*    ON THE PROGRAM COLLATING SEQUENCE CLAUSE AND ALPHABET-NAME   NC2144.2
005300*    CLAUSES.                                                     NC2144.2
005400*                                                                 NC2144.2
005500*                                                                 NC2144.2
005600 INPUT-OUTPUT SECTION.                                            NC2144.2
005700 FILE-CONTROL.                                                    NC2144.2
005800     SELECT PRINT-FILE ASSIGN TO                                  NC2144.2
005900     XXXXX055.                                                    NC2144.2
006000 DATA DIVISION.                                                   NC2144.2
006100 FILE SECTION.                                                    NC2144.2
006200 FD  PRINT-FILE.                                                  NC2144.2
006300 01  PRINT-REC PICTURE X(120).                                    NC2144.2
006400 01  DUMMY-RECORD PICTURE X(120).                                 NC2144.2
006500 WORKING-STORAGE SECTION.                                         NC2144.2
006600 01  WRK-DU-6V0-1 PIC 9(6) VALUE ZERO.                            NC2144.2
006700 01  WRK-DU-5V0-1 PIC 9(5) VALUE ZERO.                            NC2144.2
006800 01  WRK-DU-8V0-1 PIC 9(8) VALUE ZERO.                            NC2144.2
006900 01  WRK-XN-120-1 PIC X(120) VALUE                                NC2144.2
007000     """""""""""""""""""""""""""""""""""""""""""""""""""""""""""""NC2144.2
007100-    """""""""""""""""""""""""""""""""""""""""""""""""""""""""""""NC2144.2
007200-    """""""""""""""""""""""""""""""""""""""""""""""""""""""""""""NC2144.2
007300-    """""""""""""""""""""""""""""""""""""""""""""""""""""""""""""NC2144.2
007400-    "".                                                          NC2144.2
007500 01  WRK-DU-2V1-1 PIC 99V9 VALUE ZERO.                            NC2144.2
007600 01  WRK-DU-0V1-1 PIC V9 VALUE .1.                                NC2144.2
007700 01  WRK-DU-2V1-2 PIC 99V9 VALUE 0.1.                             NC2144.2
007800 01  WRK-DU-2V1-3  PIC 99V9  VALUE  11.1.                         NC2144.2
007900 01  WRK-DU-1V0-1 PIC 9 VALUE 9.                                  NC2144.2
008000 01  WRK-DU-1V0-2 PIC 9 VALUE 2.                                  NC2144.2
008100 01  WRK-DU-1V0-3 PIC 9 VALUE 3.                                  NC2144.2
008200 01  WRK-DU-1V0-4 PIC 9 VALUE ZERO.                               NC2144.2
008300 01  WRK-DU-2V0-1 PIC 99 VALUE 10.                                NC2144.2
008400 01  WRK-DU-2V0-2 PIC 99 VALUE 11.                                NC2144.2
008500 01  WRK-DU-2V0-3 PIC 99 VALUE 12.                                NC2144.2
008600 01  COUNT-DU-6V0  PIC 9(6).                                      NC2144.2
008700 01  TEST-RESULTS.                                                NC2144.2
008800     02 FILLER                   PIC X      VALUE SPACE.          NC2144.2
008900     02 FEATURE                  PIC X(20)  VALUE SPACE.          NC2144.2
009000     02 FILLER                   PIC X      VALUE SPACE.          NC2144.2
009100     02 P-OR-F                   PIC X(5)   VALUE SPACE.          NC2144.2
009200     02 FILLER                   PIC X      VALUE SPACE.          NC2144.2
009300     02  PAR-NAME.                                                NC2144.2
009400       03 FILLER                 PIC X(19)  VALUE SPACE.          NC2144.2
009500       03  PARDOT-X              PIC X      VALUE SPACE.          NC2144.2
009600       03 DOTVALUE               PIC 99     VALUE ZERO.           NC2144.2
009700     02 FILLER                   PIC X(8)   VALUE SPACE.          NC2144.2
009800     02 RE-MARK                  PIC X(61).                       NC2144.2
009900 01  TEST-COMPUTED.                                               NC2144.2
010000     02 FILLER                   PIC X(30)  VALUE SPACE.          NC2144.2
010100     02 FILLER                   PIC X(17)  VALUE                 NC2144.2
010200            "       COMPUTED=".                                   NC2144.2
010300     02 COMPUTED-X.                                               NC2144.2
010400     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          NC2144.2
010500     03 COMPUTED-N               REDEFINES COMPUTED-A             NC2144.2
010600                                 PIC -9(9).9(9).                  NC2144.2
010700     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         NC2144.2
010800     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     NC2144.2
010900     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     NC2144.2
011000     03       CM-18V0 REDEFINES COMPUTED-A.                       NC2144.2
011100         04 COMPUTED-18V0                    PIC -9(18).          NC2144.2
011200         04 FILLER                           PIC X.               NC2144.2
011300     03 FILLER PIC X(50) VALUE SPACE.                             NC2144.2
011400 01  TEST-CORRECT.                                                NC2144.2
011500     02 FILLER PIC X(30) VALUE SPACE.                             NC2144.2
011600     02 FILLER PIC X(17) VALUE "       CORRECT =".                NC2144.2
011700     02 CORRECT-X.                                                NC2144.2
011800     03 CORRECT-A                  PIC X(20) VALUE SPACE.         NC2144.2
011900     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      NC2144.2
012000     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         NC2144.2
012100     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     NC2144.2
012200     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     NC2144.2
012300     03      CR-18V0 REDEFINES CORRECT-A.                         NC2144.2
012400         04 CORRECT-18V0                     PIC -9(18).          NC2144.2
012500         04 FILLER                           PIC X.               NC2144.2
012600     03 FILLER PIC X(2) VALUE SPACE.                              NC2144.2
012700     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     NC2144.2
012800 01  CCVS-C-1.                                                    NC2144.2
012900     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PANC2144.2
013000-    "SS  PARAGRAPH-NAME                                          NC2144.2
013100-    "       REMARKS".                                            NC2144.2
013200     02 FILLER                     PIC X(20)    VALUE SPACE.      NC2144.2
013300 01  CCVS-C-2.                                                    NC2144.2
013400     02 FILLER                     PIC X        VALUE SPACE.      NC2144.2
013500     02 FILLER                     PIC X(6)     VALUE "TESTED".   NC2144.2
013600     02 FILLER                     PIC X(15)    VALUE SPACE.      NC2144.2
013700     02 FILLER                     PIC X(4)     VALUE "FAIL".     NC2144.2
013800     02 FILLER                     PIC X(94)    VALUE SPACE.      NC2144.2
013900 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       NC2144.2
014000 01  REC-CT                        PIC 99       VALUE ZERO.       NC2144.2
014100 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       NC2144.2
014200 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       NC2144.2
014300 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       NC2144.2
014400 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       NC2144.2
014500 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       NC2144.2
014600 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       NC2144.2
014700 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      NC2144.2
014800 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       NC2144.2
014900 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     NC2144.2
015000 01  CCVS-H-1.                                                    NC2144.2
015100     02  FILLER                    PIC X(39)    VALUE SPACES.     NC2144.2
015200     02  FILLER                    PIC X(42)    VALUE             NC2144.2
015300     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 NC2144.2
015400     02  FILLER                    PIC X(39)    VALUE SPACES.     NC2144.2
015500 01  CCVS-H-2A.                                                   NC2144.2
015600   02  FILLER                        PIC X(40)  VALUE SPACE.      NC2144.2
015700   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  NC2144.2
015800   02  FILLER                        PIC XXXX   VALUE             NC2144.2
015900     "4.2 ".                                                      NC2144.2
016000   02  FILLER                        PIC X(28)  VALUE             NC2144.2
016100            " COPY - NOT FOR DISTRIBUTION".                       NC2144.2
016200   02  FILLER                        PIC X(41)  VALUE SPACE.      NC2144.2
016300                                                                  NC2144.2
016400 01  CCVS-H-2B.                                                   NC2144.2
016500   02  FILLER                        PIC X(15)  VALUE             NC2144.2
016600            "TEST RESULT OF ".                                    NC2144.2
016700   02  TEST-ID                       PIC X(9).                    NC2144.2
016800   02  FILLER                        PIC X(4)   VALUE             NC2144.2
016900            " IN ".                                               NC2144.2
017000   02  FILLER                        PIC X(12)  VALUE             NC2144.2
017100     " HIGH       ".                                              NC2144.2
017200   02  FILLER                        PIC X(22)  VALUE             NC2144.2
017300            " LEVEL VALIDATION FOR ".                             NC2144.2
017400   02  FILLER                        PIC X(58)  VALUE             NC2144.2
017500     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2144.2
017600 01  CCVS-H-3.                                                    NC2144.2
017700     02  FILLER                      PIC X(34)  VALUE             NC2144.2
017800            " FOR OFFICIAL USE ONLY    ".                         NC2144.2
017900     02  FILLER                      PIC X(58)  VALUE             NC2144.2
018000     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC2144.2
018100     02  FILLER                      PIC X(28)  VALUE             NC2144.2
018200            "  COPYRIGHT   1985 ".                                NC2144.2
018300 01  CCVS-E-1.                                                    NC2144.2
018400     02 FILLER                       PIC X(52)  VALUE SPACE.      NC2144.2
018500     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              NC2144.2
018600     02 ID-AGAIN                     PIC X(9).                    NC2144.2
018700     02 FILLER                       PIC X(45)  VALUE SPACES.     NC2144.2
018800 01  CCVS-E-2.                                                    NC2144.2
018900     02  FILLER                      PIC X(31)  VALUE SPACE.      NC2144.2
019000     02  FILLER                      PIC X(21)  VALUE SPACE.      NC2144.2
019100     02 CCVS-E-2-2.                                               NC2144.2
019200         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      NC2144.2
019300         03 FILLER                   PIC X      VALUE SPACE.      NC2144.2
019400         03 ENDER-DESC               PIC X(44)  VALUE             NC2144.2
019500            "ERRORS ENCOUNTERED".                                 NC2144.2
019600 01  CCVS-E-3.                                                    NC2144.2
019700     02  FILLER                      PIC X(22)  VALUE             NC2144.2
019800            " FOR OFFICIAL USE ONLY".                             NC2144.2
019900     02  FILLER                      PIC X(12)  VALUE SPACE.      NC2144.2
020000     02  FILLER                      PIC X(58)  VALUE             NC2144.2
020100     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2144.2
020200     02  FILLER                      PIC X(13)  VALUE SPACE.      NC2144.2
020300     02 FILLER                       PIC X(15)  VALUE             NC2144.2
020400             " COPYRIGHT 1985".                                   NC2144.2
020500 01  CCVS-E-4.                                                    NC2144.2
020600     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      NC2144.2
020700     02 FILLER                       PIC X(4)   VALUE " OF ".     NC2144.2
020800     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      NC2144.2
020900     02 FILLER                       PIC X(40)  VALUE             NC2144.2
021000      "  TESTS WERE EXECUTED SUCCESSFULLY".                       NC2144.2
021100 01  XXINFO.                                                      NC2144.2
021200     02 FILLER                       PIC X(19)  VALUE             NC2144.2
021300            "*** INFORMATION ***".                                NC2144.2
021400     02 INFO-TEXT.                                                NC2144.2
021500       04 FILLER                     PIC X(8)   VALUE SPACE.      NC2144.2
021600       04 XXCOMPUTED                 PIC X(20).                   NC2144.2
021700       04 FILLER                     PIC X(5)   VALUE SPACE.      NC2144.2
021800       04 XXCORRECT                  PIC X(20).                   NC2144.2
021900     02 INF-ANSI-REFERENCE           PIC X(48).                   NC2144.2
022000 01  HYPHEN-LINE.                                                 NC2144.2
022100     02 FILLER  PIC IS X VALUE IS SPACE.                          NC2144.2
022200     02 FILLER  PIC IS X(65)    VALUE IS "************************NC2144.2
022300-    "*****************************************".                 NC2144.2
022400     02 FILLER  PIC IS X(54)    VALUE IS "************************NC2144.2
022500-    "******************************".                            NC2144.2
022600 01  CCVS-PGM-ID                     PIC X(9)   VALUE             NC2144.2
022700     "NC214M".                                                    NC2144.2
022800 PROCEDURE DIVISION.                                              NC2144.2
022900 CCVS1 SECTION.                                                   NC2144.2
023000 OPEN-FILES.                                                      NC2144.2
023100     OPEN     OUTPUT PRINT-FILE.                                  NC2144.2
023200     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   NC2144.2
023300     MOVE    SPACE TO TEST-RESULTS.                               NC2144.2
023400     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             NC2144.2
023500     GO TO CCVS1-EXIT.                                            NC2144.2
023600 CLOSE-FILES.                                                     NC2144.2
023700     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   NC2144.2
023800 TERMINATE-CCVS.                                                  NC2144.2
023900*    EXIT PROGRAM.                                                NC2144.2
024000*TERMINATE-CALL.                                                  NC2144.2
024100     STOP     RUN.                                                NC2144.2
024200 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         NC2144.2
024300 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           NC2144.2
024400 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          NC2144.2
024500 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      NC2144.2
024600     MOVE "****TEST DELETED****" TO RE-MARK.                      NC2144.2
024700 PRINT-DETAIL.                                                    NC2144.2
024800     IF REC-CT NOT EQUAL TO ZERO                                  NC2144.2
024900             MOVE "." TO PARDOT-X                                 NC2144.2
025000             MOVE REC-CT TO DOTVALUE.                             NC2144.2
025100     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      NC2144.2
025200     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               NC2144.2
025300        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 NC2144.2
025400          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 NC2144.2
025500     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              NC2144.2
025600     MOVE SPACE TO CORRECT-X.                                     NC2144.2
025700     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         NC2144.2
025800     MOVE     SPACE TO RE-MARK.                                   NC2144.2
025900 HEAD-ROUTINE.                                                    NC2144.2
026000     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC2144.2
026100     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC2144.2
026200     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC2144.2
026300     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC2144.2
026400 COLUMN-NAMES-ROUTINE.                                            NC2144.2
026500     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2144.2
026600     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2144.2
026700     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        NC2144.2
026800 END-ROUTINE.                                                     NC2144.2
026900     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.NC2144.2
027000 END-RTN-EXIT.                                                    NC2144.2
027100     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2144.2
027200 END-ROUTINE-1.                                                   NC2144.2
027300      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      NC2144.2
027400      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               NC2144.2
027500      ADD PASS-COUNTER TO ERROR-HOLD.                             NC2144.2
027600*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   NC2144.2
027700      MOVE PASS-COUNTER TO CCVS-E-4-1.                            NC2144.2
027800      MOVE ERROR-HOLD TO CCVS-E-4-2.                              NC2144.2
027900      MOVE CCVS-E-4 TO CCVS-E-2-2.                                NC2144.2
028000      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           NC2144.2
028100  END-ROUTINE-12.                                                 NC2144.2
028200      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        NC2144.2
028300     IF       ERROR-COUNTER IS EQUAL TO ZERO                      NC2144.2
028400         MOVE "NO " TO ERROR-TOTAL                                NC2144.2
028500         ELSE                                                     NC2144.2
028600         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       NC2144.2
028700     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           NC2144.2
028800     PERFORM WRITE-LINE.                                          NC2144.2
028900 END-ROUTINE-13.                                                  NC2144.2
029000     IF DELETE-COUNTER IS EQUAL TO ZERO                           NC2144.2
029100         MOVE "NO " TO ERROR-TOTAL  ELSE                          NC2144.2
029200         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      NC2144.2
029300     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   NC2144.2
029400     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2144.2
029500      IF   INSPECT-COUNTER EQUAL TO ZERO                          NC2144.2
029600          MOVE "NO " TO ERROR-TOTAL                               NC2144.2
029700      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   NC2144.2
029800      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            NC2144.2
029900      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          NC2144.2
030000     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2144.2
030100 WRITE-LINE.                                                      NC2144.2
030200     ADD 1 TO RECORD-COUNT.                                       NC2144.2
030300*    IF RECORD-COUNT GREATER 50                                   NC2144.2
030400*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          NC2144.2
030500*        MOVE SPACE TO DUMMY-RECORD                               NC2144.2
030600*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  NC2144.2
030700*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             NC2144.2
030800*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     NC2144.2
030900*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          NC2144.2
031000*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          NC2144.2
031100*        MOVE ZERO TO RECORD-COUNT.                               NC2144.2
031200     PERFORM WRT-LN.                                              NC2144.2
031300 WRT-LN.                                                          NC2144.2
031400     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               NC2144.2
031500     MOVE SPACE TO DUMMY-RECORD.                                  NC2144.2
031600 BLANK-LINE-PRINT.                                                NC2144.2
031700     PERFORM WRT-LN.                                              NC2144.2
031800 FAIL-ROUTINE.                                                    NC2144.2
031900     IF   COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE. NC2144.2
032000     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.NC2144.2
032100     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC2144.2
032200     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   NC2144.2
032300     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2144.2
032400     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC2144.2
032500     GO TO  FAIL-ROUTINE-EX.                                      NC2144.2
032600 FAIL-ROUTINE-WRITE.                                              NC2144.2
032700     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         NC2144.2
032800     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 NC2144.2
032900     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. NC2144.2
033000     MOVE   SPACES TO COR-ANSI-REFERENCE.                         NC2144.2
033100 FAIL-ROUTINE-EX. EXIT.                                           NC2144.2
033200 BAIL-OUT.                                                        NC2144.2
033300     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   NC2144.2
033400     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           NC2144.2
033500 BAIL-OUT-WRITE.                                                  NC2144.2
033600     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  NC2144.2
033700     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC2144.2
033800     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2144.2
033900     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC2144.2
034000 BAIL-OUT-EX. EXIT.                                               NC2144.2
034100 CCVS1-EXIT.                                                      NC2144.2
034200     EXIT.                                                        NC2144.2
034300 SECT-NC214M-001 SECTION.                                         NC2144.2
034400 ACC-INIT-F2-1.                                                   NC2144.2
034500*    ===---> TEST THE ACCEPT FROM DATE STATEMENT <---===          NC2144.2
034600     MOVE "ACC-TEST-F2-1" TO PAR-NAME.                            NC2144.2
034700     MOVE "VI-72 6.5.4 GR7" TO ANSI-REFERENCE.                    NC2144.2
034800     MOVE "ACCEPT DATE" TO FEATURE.                               NC2144.2
034900 ACC-TEST-F2-1.                                                   NC2144.2
035000     ACCEPT WRK-DU-6V0-1 FROM DATE.                               NC2144.2
035100     MOVE WRK-DU-6V0-1 TO COMPUTED-N.                             NC2144.2
035200     MOVE "DATE YYMMDD FORMAT" TO CORRECT-A.                      NC2144.2
035300     MOVE "CHECK VISUALLY" TO RE-MARK.                            NC2144.2
035400     GO TO ACC-WRITE-F2-1.                                        NC2144.2
035500 ACC-DELETE-F2-1.                                                 NC2144.2
035600     PERFORM DE-LETE.                                             NC2144.2
035700 ACC-WRITE-F2-1.                                                  NC2144.2
035800     PERFORM PRINT-DETAIL.                                        NC2144.2
035900*                                                                 NC2144.2
036000 ACC-INIT-F2-2.                                                   NC2144.2
036100*    ===---> TEST THE ACCEPT FROM DAY STATEMENT <---===           NC2144.2
036200     MOVE "ACC-TEST-F2-2" TO PAR-NAME.                            NC2144.2
036300     MOVE "VI-72 6.5.4 GR8" TO ANSI-REFERENCE.                    NC2144.2
036400     MOVE "ACCEPT DAY" TO FEATURE.                                NC2144.2
036500 ACC-TEST-F2-2.                                                   NC2144.2
036600     ACCEPT WRK-DU-5V0-1 FROM DAY.                                NC2144.2
036700     MOVE WRK-DU-5V0-1 TO COMPUTED-N.                             NC2144.2
036800     MOVE "DAY YYDDD FORMAT" TO CORRECT-A.                        NC2144.2
036900     MOVE "CHECK VISUALLY" TO RE-MARK.                            NC2144.2
037000     GO TO ACC-WRITE-F2-2.                                        NC2144.2
037100 ACC-DELETE-F2-2.                                                 NC2144.2
037200     PERFORM DE-LETE.                                             NC2144.2
037300 ACC-WRITE-F2-2.                                                  NC2144.2
037400     PERFORM PRINT-DETAIL.                                        NC2144.2
037500*                                                                 NC2144.2
037600 ACC-INIT-F2-3.                                                   NC2144.2
037700*    ===---> TEST THE ACCEPT FROM TIME STATEMENT <---===          NC2144.2
037800     MOVE "ACC-TEST-F2-3" TO PAR-NAME.                            NC2144.2
037900     MOVE "VI-72 6.5.4 GR9" TO ANSI-REFERENCE.                    NC2144.2
038000     MOVE "ACCEPT TIME" TO FEATURE.                               NC2144.2
038100 ACC-TEST-F2-3.                                                   NC2144.2
038200     ACCEPT WRK-DU-8V0-1 FROM TIME.                               NC2144.2
038300     MOVE WRK-DU-8V0-1 TO COMPUTED-N.                             NC2144.2
038400     MOVE "HHMMSSFF FORMAT" TO CORRECT-A.                         NC2144.2
038500     MOVE "CHECK VISUALLY" TO RE-MARK.                            NC2144.2
038600     GO TO ACC-WRITE-F2-3.                                        NC2144.2
038700 ACC-DELETE-F2-3.                                                 NC2144.2
038800     PERFORM DE-LETE.                                             NC2144.2
038900 ACC-WRITE-F2-3.                                                  NC2144.2
039000     PERFORM PRINT-DETAIL.                                        NC2144.2
039100*                                                                 NC2144.2
039200 ACC-INIT-F2-4.                                                   NC2144.2
039300*    ===---> TEST THE ACCEPT FROM DAY-OF-WEEK STATEMENT <---===   NC2144.2
039400     MOVE "ACC-TEST-F2-4" TO PAR-NAME.                            NC2144.2
039500     MOVE "VI-72 6.5.4 GR10" TO ANSI-REFERENCE.                   NC2144.2
039600     MOVE "ACCEPT DAY-OF-WEEK" TO FEATURE.                        NC2144.2
039700 ACC-TEST-F2-4.                                                   NC2144.2
039800     ACCEPT WRK-DU-1V0-1 FROM DAY-OF-WEEK.                        NC2144.2
039900     MOVE WRK-DU-1V0-1 TO COMPUTED-N.                             NC2144.2
040000     MOVE "SINGLE DIGIT INTEGER REPRESENTING DAY" TO CORRECT-A.   NC2144.2
040100     MOVE "CHECK VISUALLY" TO RE-MARK.                            NC2144.2
040200     GO TO ACC-WRITE-F2-4.                                        NC2144.2
040300 ACC-DELETE-F2-4.                                                 NC2144.2
040400     PERFORM DE-LETE.                                             NC2144.2
040500 ACC-WRITE-F2-4.                                                  NC2144.2
040600     PERFORM PRINT-DETAIL.                                        NC2144.2
040700 CCVS-EXIT SECTION.                                               NC2144.2
040800 CCVS-999999.                                                     NC2144.2
040900     GO TO CLOSE-FILES.                                           NC2144.2
      *END-OF,NC214M                                                                  
