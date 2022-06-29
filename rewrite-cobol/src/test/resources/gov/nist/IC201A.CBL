      *HEADER,COBOL,IC201A                                                            
000100 IDENTIFICATION DIVISION.                                         IC2014.2
000200 PROGRAM-ID.                                                      IC2014.2
000300     IC201A.                                                      IC2014.2
000400****************************************************************  IC2014.2
000500*                                                              *  IC2014.2
000600*    VALIDATION FOR:-                                          *  IC2014.2
000700*                                                              *  IC2014.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IC2014.2
000900*                                                              *  IC2014.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IC2014.2
001100*                                                              *  IC2014.2
001200****************************************************************  IC2014.2
001300*                                                              *  IC2014.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  IC2014.2
001500*                                                              *  IC2014.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  IC2014.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  IC2014.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  IC2014.2
001900*                                                              *  IC2014.2
002000****************************************************************  IC2014.2
002100*        THE PROGRAM IC201 TESTS THE CALL STATEMENT WITH AN       IC2014.2
002200*    IDENTIFIER AS AN OPERAND, AND FOUR OPERANDS IN THE           IC2014.2
002300*    USING PHRASE.  THE REPETITION OF A DATA-NAME IN THE          IC2014.2
002400*    USING PHRASE IS TESTED, AND THE USE OF THE ON OVERFLOW       IC2014.2
002500*    PHRASE IN A CALL STATEMENT IS SYNTACTICALLY CHECKED          IC2014.2
002600*    IN THE PROGRAM.                                              IC2014.2
002700****************************************************************  IC2014.2
002800 ENVIRONMENT DIVISION.                                            IC2014.2
002900 CONFIGURATION SECTION.                                           IC2014.2
003000 SOURCE-COMPUTER.                                                 IC2014.2
003100     XXXXX082.                                                    IC2014.2
003200 OBJECT-COMPUTER.                                                 IC2014.2
003300     XXXXX083.                                                    IC2014.2
003400 INPUT-OUTPUT SECTION.                                            IC2014.2
003500 FILE-CONTROL.                                                    IC2014.2
003600     SELECT PRINT-FILE ASSIGN TO                                  IC2014.2
003700     XXXXX055.                                                    IC2014.2
003800 DATA DIVISION.                                                   IC2014.2
003900 FILE SECTION.                                                    IC2014.2
004000 FD  PRINT-FILE.                                                  IC2014.2
004100 01  PRINT-REC PICTURE X(120).                                    IC2014.2
004200 01  DUMMY-RECORD PICTURE X(120).                                 IC2014.2
004300 WORKING-STORAGE SECTION.                                         IC2014.2
004400 77  DN1 PICTURE S99  VALUE ZERO.                                 IC2014.2
004500 77  DN3 PICTURE S99.                                             IC2014.2
004600 77  ID1 PICTURE X(6) VALUE "IC202A".                             IC2014.2
004700 77  ID2 PICTURE X(6).                                            IC2014.2
004800 77  DN2 PICTURE S99                                              IC2014.2
004900         USAGE COMPUTATIONAL, VALUE ZERO.                         IC2014.2
005000 77  DN4 PICTURE S99                                              IC2014.2
005100         USAGE IS COMPUTATIONAL.                                  IC2014.2
005200 77  CALL-COUNT PIC S99.                                          IC2014.2
005300 77  FAIL-FLAG PIC 9.                                             IC2014.2
005400 01  TEST-RESULTS.                                                IC2014.2
005500     02 FILLER                   PIC X      VALUE SPACE.          IC2014.2
005600     02 FEATURE                  PIC X(20)  VALUE SPACE.          IC2014.2
005700     02 FILLER                   PIC X      VALUE SPACE.          IC2014.2
005800     02 P-OR-F                   PIC X(5)   VALUE SPACE.          IC2014.2
005900     02 FILLER                   PIC X      VALUE SPACE.          IC2014.2
006000     02  PAR-NAME.                                                IC2014.2
006100       03 FILLER                 PIC X(19)  VALUE SPACE.          IC2014.2
006200       03  PARDOT-X              PIC X      VALUE SPACE.          IC2014.2
006300       03 DOTVALUE               PIC 99     VALUE ZERO.           IC2014.2
006400     02 FILLER                   PIC X(8)   VALUE SPACE.          IC2014.2
006500     02 RE-MARK                  PIC X(61).                       IC2014.2
006600 01  TEST-COMPUTED.                                               IC2014.2
006700     02 FILLER                   PIC X(30)  VALUE SPACE.          IC2014.2
006800     02 FILLER                   PIC X(17)  VALUE                 IC2014.2
006900            "       COMPUTED=".                                   IC2014.2
007000     02 COMPUTED-X.                                               IC2014.2
007100     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          IC2014.2
007200     03 COMPUTED-N               REDEFINES COMPUTED-A             IC2014.2
007300                                 PIC -9(9).9(9).                  IC2014.2
007400     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         IC2014.2
007500     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     IC2014.2
007600     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     IC2014.2
007700     03       CM-18V0 REDEFINES COMPUTED-A.                       IC2014.2
007800         04 COMPUTED-18V0                    PIC -9(18).          IC2014.2
007900         04 FILLER                           PIC X.               IC2014.2
008000     03 FILLER PIC X(50) VALUE SPACE.                             IC2014.2
008100 01  TEST-CORRECT.                                                IC2014.2
008200     02 FILLER PIC X(30) VALUE SPACE.                             IC2014.2
008300     02 FILLER PIC X(17) VALUE "       CORRECT =".                IC2014.2
008400     02 CORRECT-X.                                                IC2014.2
008500     03 CORRECT-A                  PIC X(20) VALUE SPACE.         IC2014.2
008600     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      IC2014.2
008700     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         IC2014.2
008800     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     IC2014.2
008900     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     IC2014.2
009000     03      CR-18V0 REDEFINES CORRECT-A.                         IC2014.2
009100         04 CORRECT-18V0                     PIC -9(18).          IC2014.2
009200         04 FILLER                           PIC X.               IC2014.2
009300     03 FILLER PIC X(2) VALUE SPACE.                              IC2014.2
009400     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     IC2014.2
009500 01  CCVS-C-1.                                                    IC2014.2
009600     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAIC2014.2
009700-    "SS  PARAGRAPH-NAME                                          IC2014.2
009800-    "       REMARKS".                                            IC2014.2
009900     02 FILLER                     PIC X(20)    VALUE SPACE.      IC2014.2
010000 01  CCVS-C-2.                                                    IC2014.2
010100     02 FILLER                     PIC X        VALUE SPACE.      IC2014.2
010200     02 FILLER                     PIC X(6)     VALUE "TESTED".   IC2014.2
010300     02 FILLER                     PIC X(15)    VALUE SPACE.      IC2014.2
010400     02 FILLER                     PIC X(4)     VALUE "FAIL".     IC2014.2
010500     02 FILLER                     PIC X(94)    VALUE SPACE.      IC2014.2
010600 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       IC2014.2
010700 01  REC-CT                        PIC 99       VALUE ZERO.       IC2014.2
010800 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       IC2014.2
010900 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       IC2014.2
011000 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       IC2014.2
011100 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       IC2014.2
011200 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       IC2014.2
011300 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       IC2014.2
011400 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      IC2014.2
011500 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       IC2014.2
011600 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     IC2014.2
011700 01  CCVS-H-1.                                                    IC2014.2
011800     02  FILLER                    PIC X(39)    VALUE SPACES.     IC2014.2
011900     02  FILLER                    PIC X(42)    VALUE             IC2014.2
012000     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 IC2014.2
012100     02  FILLER                    PIC X(39)    VALUE SPACES.     IC2014.2
012200 01  CCVS-H-2A.                                                   IC2014.2
012300   02  FILLER                        PIC X(40)  VALUE SPACE.      IC2014.2
012400   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  IC2014.2
012500   02  FILLER                        PIC XXXX   VALUE             IC2014.2
012600     "4.2 ".                                                      IC2014.2
012700   02  FILLER                        PIC X(28)  VALUE             IC2014.2
012800            " COPY - NOT FOR DISTRIBUTION".                       IC2014.2
012900   02  FILLER                        PIC X(41)  VALUE SPACE.      IC2014.2
013000                                                                  IC2014.2
013100 01  CCVS-H-2B.                                                   IC2014.2
013200   02  FILLER                        PIC X(15)  VALUE             IC2014.2
013300            "TEST RESULT OF ".                                    IC2014.2
013400   02  TEST-ID                       PIC X(9).                    IC2014.2
013500   02  FILLER                        PIC X(4)   VALUE             IC2014.2
013600            " IN ".                                               IC2014.2
013700   02  FILLER                        PIC X(12)  VALUE             IC2014.2
013800     " HIGH       ".                                              IC2014.2
013900   02  FILLER                        PIC X(22)  VALUE             IC2014.2
014000            " LEVEL VALIDATION FOR ".                             IC2014.2
014100   02  FILLER                        PIC X(58)  VALUE             IC2014.2
014200     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IC2014.2
014300 01  CCVS-H-3.                                                    IC2014.2
014400     02  FILLER                      PIC X(34)  VALUE             IC2014.2
014500            " FOR OFFICIAL USE ONLY    ".                         IC2014.2
014600     02  FILLER                      PIC X(58)  VALUE             IC2014.2
014700     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IC2014.2
014800     02  FILLER                      PIC X(28)  VALUE             IC2014.2
014900            "  COPYRIGHT   1985 ".                                IC2014.2
015000 01  CCVS-E-1.                                                    IC2014.2
015100     02 FILLER                       PIC X(52)  VALUE SPACE.      IC2014.2
015200     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              IC2014.2
015300     02 ID-AGAIN                     PIC X(9).                    IC2014.2
015400     02 FILLER                       PIC X(45)  VALUE SPACES.     IC2014.2
015500 01  CCVS-E-2.                                                    IC2014.2
015600     02  FILLER                      PIC X(31)  VALUE SPACE.      IC2014.2
015700     02  FILLER                      PIC X(21)  VALUE SPACE.      IC2014.2
015800     02 CCVS-E-2-2.                                               IC2014.2
015900         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      IC2014.2
016000         03 FILLER                   PIC X      VALUE SPACE.      IC2014.2
016100         03 ENDER-DESC               PIC X(44)  VALUE             IC2014.2
016200            "ERRORS ENCOUNTERED".                                 IC2014.2
016300 01  CCVS-E-3.                                                    IC2014.2
016400     02  FILLER                      PIC X(22)  VALUE             IC2014.2
016500            " FOR OFFICIAL USE ONLY".                             IC2014.2
016600     02  FILLER                      PIC X(12)  VALUE SPACE.      IC2014.2
016700     02  FILLER                      PIC X(58)  VALUE             IC2014.2
016800     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IC2014.2
016900     02  FILLER                      PIC X(13)  VALUE SPACE.      IC2014.2
017000     02 FILLER                       PIC X(15)  VALUE             IC2014.2
017100             " COPYRIGHT 1985".                                   IC2014.2
017200 01  CCVS-E-4.                                                    IC2014.2
017300     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      IC2014.2
017400     02 FILLER                       PIC X(4)   VALUE " OF ".     IC2014.2
017500     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      IC2014.2
017600     02 FILLER                       PIC X(40)  VALUE             IC2014.2
017700      "  TESTS WERE EXECUTED SUCCESSFULLY".                       IC2014.2
017800 01  XXINFO.                                                      IC2014.2
017900     02 FILLER                       PIC X(19)  VALUE             IC2014.2
018000            "*** INFORMATION ***".                                IC2014.2
018100     02 INFO-TEXT.                                                IC2014.2
018200       04 FILLER                     PIC X(8)   VALUE SPACE.      IC2014.2
018300       04 XXCOMPUTED                 PIC X(20).                   IC2014.2
018400       04 FILLER                     PIC X(5)   VALUE SPACE.      IC2014.2
018500       04 XXCORRECT                  PIC X(20).                   IC2014.2
018600     02 INF-ANSI-REFERENCE           PIC X(48).                   IC2014.2
018700 01  HYPHEN-LINE.                                                 IC2014.2
018800     02 FILLER  PIC IS X VALUE IS SPACE.                          IC2014.2
018900     02 FILLER  PIC IS X(65)    VALUE IS "************************IC2014.2
019000-    "*****************************************".                 IC2014.2
019100     02 FILLER  PIC IS X(54)    VALUE IS "************************IC2014.2
019200-    "******************************".                            IC2014.2
019300 01  CCVS-PGM-ID                     PIC X(9)   VALUE             IC2014.2
019400     "IC201A".                                                    IC2014.2
019500 PROCEDURE DIVISION.                                              IC2014.2
019600 CCVS1 SECTION.                                                   IC2014.2
019700 OPEN-FILES.                                                      IC2014.2
019800     OPEN     OUTPUT PRINT-FILE.                                  IC2014.2
019900     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   IC2014.2
020000     MOVE    SPACE TO TEST-RESULTS.                               IC2014.2
020100     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             IC2014.2
020200     GO TO CCVS1-EXIT.                                            IC2014.2
020300 CLOSE-FILES.                                                     IC2014.2
020400     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   IC2014.2
020500 TERMINATE-CCVS.                                                  IC2014.2
020600*    EXIT PROGRAM.                                                IC2014.2
020700*TERMINATE-CALL.                                                  IC2014.2
020800     STOP     RUN.                                                IC2014.2
020900 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         IC2014.2
021000 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           IC2014.2
021100 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          IC2014.2
021200 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      IC2014.2
021300     MOVE "****TEST DELETED****" TO RE-MARK.                      IC2014.2
021400 PRINT-DETAIL.                                                    IC2014.2
021500     IF REC-CT NOT EQUAL TO ZERO                                  IC2014.2
021600             MOVE "." TO PARDOT-X                                 IC2014.2
021700             MOVE REC-CT TO DOTVALUE.                             IC2014.2
021800     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      IC2014.2
021900     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               IC2014.2
022000        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 IC2014.2
022100          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 IC2014.2
022200     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              IC2014.2
022300     MOVE SPACE TO CORRECT-X.                                     IC2014.2
022400     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         IC2014.2
022500     MOVE     SPACE TO RE-MARK.                                   IC2014.2
022600 HEAD-ROUTINE.                                                    IC2014.2
022700     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IC2014.2
022800     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IC2014.2
022900     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IC2014.2
023000     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IC2014.2
023100 COLUMN-NAMES-ROUTINE.                                            IC2014.2
023200     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IC2014.2
023300     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IC2014.2
023400     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        IC2014.2
023500 END-ROUTINE.                                                     IC2014.2
023600     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.IC2014.2
023700 END-RTN-EXIT.                                                    IC2014.2
023800     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IC2014.2
023900 END-ROUTINE-1.                                                   IC2014.2
024000      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      IC2014.2
024100      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               IC2014.2
024200      ADD PASS-COUNTER TO ERROR-HOLD.                             IC2014.2
024300*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   IC2014.2
024400      MOVE PASS-COUNTER TO CCVS-E-4-1.                            IC2014.2
024500      MOVE ERROR-HOLD TO CCVS-E-4-2.                              IC2014.2
024600      MOVE CCVS-E-4 TO CCVS-E-2-2.                                IC2014.2
024700      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           IC2014.2
024800  END-ROUTINE-12.                                                 IC2014.2
024900      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        IC2014.2
025000     IF       ERROR-COUNTER IS EQUAL TO ZERO                      IC2014.2
025100         MOVE "NO " TO ERROR-TOTAL                                IC2014.2
025200         ELSE                                                     IC2014.2
025300         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       IC2014.2
025400     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           IC2014.2
025500     PERFORM WRITE-LINE.                                          IC2014.2
025600 END-ROUTINE-13.                                                  IC2014.2
025700     IF DELETE-COUNTER IS EQUAL TO ZERO                           IC2014.2
025800         MOVE "NO " TO ERROR-TOTAL  ELSE                          IC2014.2
025900         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      IC2014.2
026000     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   IC2014.2
026100     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IC2014.2
026200      IF   INSPECT-COUNTER EQUAL TO ZERO                          IC2014.2
026300          MOVE "NO " TO ERROR-TOTAL                               IC2014.2
026400      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   IC2014.2
026500      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            IC2014.2
026600      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          IC2014.2
026700     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IC2014.2
026800 WRITE-LINE.                                                      IC2014.2
026900     ADD 1 TO RECORD-COUNT.                                       IC2014.2
027000*    IF RECORD-COUNT GREATER 50                                   IC2014.2
027100*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          IC2014.2
027200*        MOVE SPACE TO DUMMY-RECORD                               IC2014.2
027300*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  IC2014.2
027400*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             IC2014.2
027500*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     IC2014.2
027600*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          IC2014.2
027700*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          IC2014.2
027800*        MOVE ZERO TO RECORD-COUNT.                               IC2014.2
027900     PERFORM WRT-LN.                                              IC2014.2
028000 WRT-LN.                                                          IC2014.2
028100     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               IC2014.2
028200     MOVE SPACE TO DUMMY-RECORD.                                  IC2014.2
028300 BLANK-LINE-PRINT.                                                IC2014.2
028400     PERFORM WRT-LN.                                              IC2014.2
028500 FAIL-ROUTINE.                                                    IC2014.2
028600     IF   COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE. IC2014.2
028700     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.IC2014.2
028800     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IC2014.2
028900     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   IC2014.2
029000     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IC2014.2
029100     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IC2014.2
029200     GO TO  FAIL-ROUTINE-EX.                                      IC2014.2
029300 FAIL-ROUTINE-WRITE.                                              IC2014.2
029400     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         IC2014.2
029500     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 IC2014.2
029600     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. IC2014.2
029700     MOVE   SPACES TO COR-ANSI-REFERENCE.                         IC2014.2
029800 FAIL-ROUTINE-EX. EXIT.                                           IC2014.2
029900 BAIL-OUT.                                                        IC2014.2
030000     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   IC2014.2
030100     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           IC2014.2
030200 BAIL-OUT-WRITE.                                                  IC2014.2
030300     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  IC2014.2
030400     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IC2014.2
030500     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IC2014.2
030600     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IC2014.2
030700 BAIL-OUT-EX. EXIT.                                               IC2014.2
030800 CCVS1-EXIT.                                                      IC2014.2
030900     EXIT.                                                        IC2014.2
031000 SECT-IC201-0001 SECTION.                                         IC2014.2
031100 CALL-TEST-01.                                                    IC2014.2
031200     MOVE "CALL-TEST-01" TO PAR-NAME.                             IC2014.2
031300     MOVE     "LEV 2 CALL STATEMENT" TO FEATURE.                  IC2014.2
031400     MOVE 0 TO CALL-COUNT.                                        IC2014.2
031500*        THIS TEST HAS CALL STATEMENTS WITH AN IDENTIFIER         IC2014.2
031600*    CONTAINING THE NAME OF THE SUBPROGRAM TO BE CALLED.          IC2014.2
031700*        CALL-TEST-01 CONTAINS THE BASIC LEVEL 2 CALL STATEMENT.  IC2014.2
031800*    IF IT CANNOT BE COMPILED AND EXECUTED CORRECTLY, THERE IS    IC2014.2
031900*    NO USE IN RUNNING THE LEVEL 2 IPC ROUTINES.                  IC2014.2
032000 CALL-TEST-01-01.                                                 IC2014.2
032100     MOVE 1 TO REC-CT.                                            IC2014.2
032200     MOVE ZERO TO DN3, DN4.                                       IC2014.2
032300     CALL "IC202A" USING DN1, DN2, DN3, DN4.                      IC2014.2
032400     PERFORM CHECK-TEST-01.                                       IC2014.2
032500     IF FAIL-FLAG EQUAL TO ZERO                                   IC2014.2
032600         PERFORM PASS                                             IC2014.2
032700         GO TO CALL-WRITE-01-01.                                  IC2014.2
032800 CALL-FAIL-01-01.                                                 IC2014.2
032900     PERFORM FAIL.                                                IC2014.2
033000     MOVE FAIL-FLAG TO COMPUTED-18V0.                             IC2014.2
033100     MOVE "NO. OF WRONG VALUES RETURNED" TO RE-MARK.              IC2014.2
033200 CALL-WRITE-01-01.                                                IC2014.2
033300     PERFORM PRINT-DETAIL.                                        IC2014.2
033400 CALL-TEST-01-02.                                                 IC2014.2
033500     ADD 1 TO REC-CT.                                             IC2014.2
033600     MOVE ZERO TO DN3, DN4.                                       IC2014.2
033700     CALL ID1 USING DN1, DN2, DN3, DN4.                           IC2014.2
033800     PERFORM CHECK-TEST-01.                                       IC2014.2
033900     IF FAIL-FLAG EQUAL TO ZERO                                   IC2014.2
034000         PERFORM PASS                                             IC2014.2
034100         GO TO CALL-WRITE-01-02.                                  IC2014.2
034200 CALL-FAIL-01-02.                                                 IC2014.2
034300     PERFORM FAIL.                                                IC2014.2
034400     MOVE FAIL-FLAG TO COMPUTED-18V0.                             IC2014.2
034500     MOVE "NO. OF WRONG VALUES RETURNED" TO RE-MARK.              IC2014.2
034600 CALL-WRITE-01-02.                                                IC2014.2
034700     PERFORM PRINT-DETAIL.                                        IC2014.2
034800 CALL-TEST-01-03.                                                 IC2014.2
034900     ADD 1 TO REC-CT.                                             IC2014.2
035000     MOVE ID1 TO ID2.                                             IC2014.2
035100     MOVE ZERO TO DN3, DN4.                                       IC2014.2
035200     CALL ID2 USING DN1 DN2 DN3 DN4.                              IC2014.2
035300     PERFORM CHECK-TEST-01.                                       IC2014.2
035400     IF FAIL-FLAG EQUAL TO ZERO                                   IC2014.2
035500         PERFORM PASS                                             IC2014.2
035600         GO TO CALL-WRITE-01-03.                                  IC2014.2
035700 CALL-FAIL-01-03.                                                 IC2014.2
035800     PERFORM FAIL.                                                IC2014.2
035900     MOVE FAIL-FLAG TO COMPUTED-18V0.                             IC2014.2
036000     MOVE "NO. OF WRONG VALUES RETURNED" TO RE-MARK.              IC2014.2
036100 CALL-WRITE-01-03.                                                IC2014.2
036200     PERFORM PRINT-DETAIL.                                        IC2014.2
036300 CALL-TEST-01-04.                                                 IC2014.2
036400     ADD 1 TO REC-CT.                                             IC2014.2
036500     MOVE "IC202A" TO ID2.                                        IC2014.2
036600     MOVE ZERO TO DN3, DN4.                                       IC2014.2
036700     CALL ID2 USING DN1, DN2, DN3, DN4.                           IC2014.2
036800     PERFORM CHECK-TEST-01.                                       IC2014.2
036900     IF FAIL-FLAG EQUAL TO ZERO                                   IC2014.2
037000         PERFORM PASS                                             IC2014.2
037100         GO TO CALL-WRITE-01-04.                                  IC2014.2
037200 CALL-FAIL-01-04.                                                 IC2014.2
037300     PERFORM FAIL.                                                IC2014.2
037400     MOVE FAIL-FLAG TO COMPUTED-18V0.                             IC2014.2
037500     MOVE "NO. OF WRONG VALUES RETURNED" TO RE-MARK.              IC2014.2
037600 CALL-WRITE-01-04.                                                IC2014.2
037700     PERFORM PRINT-DETAIL.                                        IC2014.2
037800 CALL-TEST-02.                                                    IC2014.2
037900     MOVE "CALL-TEST-02" TO PAR-NAME.                             IC2014.2
038000     MOVE "DATA-NAME USED TWICE" TO FEATURE.                      IC2014.2
038100*        THIS TEST USES A DATA-NAME MORE THAN ONCE IN             IC2014.2
038200*    A USING PHRASE OF A CALL STATEMENT.                          IC2014.2
038300 CALL-TEST-02-01.                                                 IC2014.2
038400     MOVE 1 TO REC-CT.                                            IC2014.2
038500     MOVE 1 TO DN1.                                               IC2014.2
038600     MOVE 0 TO DN2, DN3, DN4.                                     IC2014.2
038700     CALL "IC202A" USING DN1, DN2, DN1, DN4.                      IC2014.2
038800     IF DN1 NOT EQUAL TO 2                                        IC2014.2
038900         GO TO CALL-FAIL-02-01-1.                                 IC2014.2
039000     IF DN2 NOT EQUAL TO 0                                        IC2014.2
039100         GO TO CALL-FAIL-02-01-2.                                 IC2014.2
039200     IF DN3 NOT EQUAL TO 0                                        IC2014.2
039300         GO TO CALL-FAIL-02-01-3.                                 IC2014.2
039400     IF DN4 NOT EQUAL TO 5                                        IC2014.2
039500         GO TO CALL-FAIL-02-01-4.                                 IC2014.2
039600     GO TO CALL-PASS-02-01.                                       IC2014.2
039700 CALL-DELETE-02-01.                                               IC2014.2
039800     PERFORM DE-LETE.                                             IC2014.2
039900     GO TO CALL-WRITE-02-01.                                      IC2014.2
040000 CALL-PASS-02-01.                                                 IC2014.2
040100     PERFORM PASS.                                                IC2014.2
040200     GO TO CALL-WRITE-02-01.                                      IC2014.2
040300 CALL-FAIL-02-01-1.                                               IC2014.2
040400     MOVE DN1 TO COMPUTED-18V0.                                   IC2014.2
040500     MOVE 2 TO CORRECT-18V0.                                      IC2014.2
040600     MOVE "ERROR IN DN1 VALUE RETURNED" TO RE-MARK.               IC2014.2
040700     GO TO CALL-FAIL-02-01.                                       IC2014.2
040800 CALL-FAIL-02-01-2.                                               IC2014.2
040900     MOVE DN2 TO COMPUTED-18V0.                                   IC2014.2
041000     MOVE 0 TO CORRECT-18V0.                                      IC2014.2
041100     MOVE "ERROR IN DN2 VALUE RETURNED" TO RE-MARK.               IC2014.2
041200     GO TO CALL-FAIL-02-01.                                       IC2014.2
041300 CALL-FAIL-02-01-3.                                               IC2014.2
041400     MOVE DN3 TO COMPUTED-18V0.                                   IC2014.2
041500     MOVE ZERO TO CORRECT-18V0.                                   IC2014.2
041600     MOVE "DN3 VALUE CHANGED BY CALL" TO RE-MARK.                 IC2014.2
041700     GO TO CALL-FAIL-02-01.                                       IC2014.2
041800 CALL-FAIL-02-01-4.                                               IC2014.2
041900     MOVE DN4 TO COMPUTED-18V0.                                   IC2014.2
042000     MOVE 5 TO CORRECT-18V0.                                      IC2014.2
042100     MOVE "ERROR IN DN4 VALUE RETURNED" TO RE-MARK.               IC2014.2
042200 CALL-FAIL-02-01.                                                 IC2014.2
042300     PERFORM FAIL.                                                IC2014.2
042400 CALL-WRITE-02-01.                                                IC2014.2
042500     PERFORM PRINT-DETAIL.                                        IC2014.2
042600 CALL-TEST-02-02.                                                 IC2014.2
042700     ADD 1 TO REC-CT.                                             IC2014.2
042800     MOVE 0 TO DN4, DN3, DN2, DN1.                                IC2014.2
042900     CALL ID1 USING DN1 DN2 DN3 DN2.                              IC2014.2
043000     IF DN1 NOT EQUAL TO 0                                        IC2014.2
043100         GO TO CALL-FAIL-02-02-1.                                 IC2014.2
043200     IF DN2 NOT EQUAL TO 6                                        IC2014.2
043300         GO TO CALL-FAIL-02-02-2.                                 IC2014.2
043400     IF DN3 NOT EQUAL TO 1                                        IC2014.2
043500         GO TO CALL-FAIL-02-02-3.                                 IC2014.2
043600     IF DN4 NOT EQUAL TO 0                                        IC2014.2
043700         GO TO CALL-FAIL-02-02-4.                                 IC2014.2
043800     GO TO CALL-PASS-02-02.                                       IC2014.2
043900 CALL-DELETE-02-02.                                               IC2014.2
044000     PERFORM DE-LETE.                                             IC2014.2
044100     GO TO CALL-WRITE-02-02.                                      IC2014.2
044200 CALL-PASS-02-02.                                                 IC2014.2
044300     PERFORM PASS.                                                IC2014.2
044400     GO TO CALL-WRITE-02-02.                                      IC2014.2
044500 CALL-FAIL-02-02-1.                                               IC2014.2
044600     MOVE DN1 TO COMPUTED-18V0.                                   IC2014.2
044700     MOVE ZERO TO CORRECT-18V0.                                   IC2014.2
044800     MOVE "ERROR IN DN1 VALUE RETURNED" TO RE-MARK.               IC2014.2
044900     GO TO CALL-FAIL-02-02.                                       IC2014.2
045000 CALL-FAIL-02-02-2.                                               IC2014.2
045100     MOVE DN2 TO COMPUTED-18V0.                                   IC2014.2
045200     MOVE 6 TO CORRECT-18V0.                                      IC2014.2
045300     MOVE "ERROR IN DN2 VALUE RETURNED" TO RE-MARK.               IC2014.2
045400     GO TO CALL-FAIL-02-02.                                       IC2014.2
045500 CALL-FAIL-02-02-3.                                               IC2014.2
045600     MOVE DN3 TO COMPUTED-18V0.                                   IC2014.2
045700     MOVE 1 TO CORRECT-18V0.                                      IC2014.2
045800     MOVE "ERROR IN DN3 VALUE RETURNED" TO RE-MARK.               IC2014.2
045900     GO TO CALL-FAIL-02-02.                                       IC2014.2
046000 CALL-FAIL-02-02-4.                                               IC2014.2
046100     MOVE DN4 TO COMPUTED-18V0.                                   IC2014.2
046200     MOVE 0 TO CORRECT-18V0.                                      IC2014.2
046300     MOVE "DN4 VALUE CHANGED BY CALL" TO RE-MARK.                 IC2014.2
046400 CALL-FAIL-02-02.                                                 IC2014.2
046500     PERFORM FAIL.                                                IC2014.2
046600 CALL-WRITE-02-02.                                                IC2014.2
046700     PERFORM PRINT-DETAIL.                                        IC2014.2
046800 CALL-TEST-02-03.                                                 IC2014.2
046900     ADD 1 TO REC-CT.                                             IC2014.2
047000     MOVE 0 TO DN4, DN3.                                          IC2014.2
047100     MOVE 10 TO DN2.                                              IC2014.2
047200     MOVE 25 TO DN1.                                              IC2014.2
047300     CALL ID1 USING DN1 DN2 DN1 DN2.                              IC2014.2
047400     IF DN1 EQUAL TO 26                                           IC2014.2
047500         GO TO CHECK-02-03-2.                                     IC2014.2
047600     GO TO CALL-FAIL-02-03-1.                                     IC2014.2
047700 CALL-DELETE-02-03.                                               IC2014.2
047800     PERFORM DE-LETE.                                             IC2014.2
047900     GO TO CALL-WRITE-02-03.                                      IC2014.2
048000 CALL-FAIL-02-03-1.                                               IC2014.2
048100     MOVE DN1 TO COMPUTED-18V0.                                   IC2014.2
048200     MOVE 26 TO CORRECT-18V0.                                     IC2014.2
048300     MOVE "ERROR IN DN1 VALUE RETURNED" TO RE-MARK.               IC2014.2
048400     GO TO CALL-FAIL-02-03.                                       IC2014.2
048500 CHECK-02-03-2.                                                   IC2014.2
048600     IF DN2 EQUAL TO 7                                            IC2014.2
048700         GO TO CHECK-02-03-3.                                     IC2014.2
048800 CALL-FAIL-02-03-2.                                               IC2014.2
048900     MOVE DN2 TO COMPUTED-18V0.                                   IC2014.2
049000     MOVE 7 TO CORRECT-18V0.                                      IC2014.2
049100     MOVE "ERROR IN DN2 VALUE RETURNED" TO RE-MARK.               IC2014.2
049200     GO TO CALL-FAIL-02-03.                                       IC2014.2
049300 CHECK-02-03-3.                                                   IC2014.2
049400     IF DN3 EQUAL TO 0                                            IC2014.2
049500         GO TO CHECK-02-03-4.                                     IC2014.2
049600 CALL-FAIL-02-03-3.                                               IC2014.2
049700     MOVE DN3 TO COMPUTED-18V0.                                   IC2014.2
049800     MOVE 0 TO CORRECT-18V0.                                      IC2014.2
049900     MOVE "DN3 VALUE CHANGED BY CALL" TO RE-MARK.                 IC2014.2
050000     GO TO CALL-FAIL-02-03.                                       IC2014.2
050100 CHECK-02-03-4.                                                   IC2014.2
050200     IF DN4 EQUAL TO 0                                            IC2014.2
050300         GO TO CALL-PASS-02-03.                                   IC2014.2
050400 CALL-FAIL-02-03-4.                                               IC2014.2
050500     MOVE DN4 TO COMPUTED-18V0.                                   IC2014.2
050600     MOVE 0 TO CORRECT-18V0.                                      IC2014.2
050700     MOVE "DN4 VALUE CHANGED BY CALL" TO RE-MARK.                 IC2014.2
050800 CALL-FAIL-02-03.                                                 IC2014.2
050900     PERFORM FAIL.                                                IC2014.2
051000     GO TO CALL-WRITE-02-03.                                      IC2014.2
051100 CALL-PASS-02-03.                                                 IC2014.2
051200     PERFORM PASS.                                                IC2014.2
051300 CALL-WRITE-02-03.                                                IC2014.2
051400     PERFORM PRINT-DETAIL.                                        IC2014.2
051500 CALL-TEST-03.                                                    IC2014.2
051600*        THIS TEST USES THE ON OVERFLOW PHRASE IN THE CALL        IC2014.2
051700*    STATEMENT.  THIS IS A SYNTACTICAL CHECK ONLY, THE ON         IC2014.2
051800*    OVERFLOW CONDITION SHOULD NEVER OCCUR.                       IC2014.2
051900     MOVE "CALL-TEST-03" TO PAR-NAME.                             IC2014.2
052000     MOVE "ON OVERFLOW PHRASE" TO FEATURE.                        IC2014.2
052100 CALL-TEST-03-01.                                                 IC2014.2
052200     MOVE 7 TO CALL-COUNT.                                        IC2014.2
052300     MOVE 20 TO DN1.                                              IC2014.2
052400     MOVE 30 TO DN2.                                              IC2014.2
052500     MOVE ZERO TO DN3, DN4.                                       IC2014.2
052600     CALL "IC202A" USING DN1, DN2, DN3, DN4;                      IC2014.2
052700         ON OVERFLOW MOVE "OVERFLOW SHOULD NOT OCCUR" TO RE-MARK  IC2014.2
052800                     GO TO CALL-FAIL-03-01.                       IC2014.2
052900     PERFORM CHECK-TEST-03.                                       IC2014.2
053000     IF FAIL-FLAG EQUAL TO ZERO                                   IC2014.2
053100         PERFORM PASS                                             IC2014.2
053200         GO TO CALL-WRITE-03-01.                                  IC2014.2
053300     MOVE FAIL-FLAG TO COMPUTED-18V0.                             IC2014.2
053400     MOVE "NO. OF WRONG VALUES RETURNED" TO RE-MARK.              IC2014.2
053500 CALL-FAIL-03-01.                                                 IC2014.2
053600     PERFORM FAIL.                                                IC2014.2
053700 CALL-WRITE-03-01.                                                IC2014.2
053800     PERFORM PRINT-DETAIL.                                        IC2014.2
053900 CALL-TEST-03-02.                                                 IC2014.2
054000     MOVE ZERO TO DN3, DN4.                                       IC2014.2
054100     CALL "IC202A" USING DN1, DN2, DN3, DN4;                      IC2014.2
054200         OVERFLOW MOVE "OVERFLOW SHOULD NOT OCCUR" TO RE-MARK     IC2014.2
054300                  GO TO CALL-FAIL-03-02.                          IC2014.2
054400     PERFORM CHECK-TEST-03.                                       IC2014.2
054500     IF FAIL-FLAG EQUAL TO ZERO                                   IC2014.2
054600         PERFORM PASS                                             IC2014.2
054700         GO TO CALL-WRITE-03-02.                                  IC2014.2
054800     MOVE FAIL-FLAG TO COMPUTED-18V0.                             IC2014.2
054900     MOVE "NO. OF WRONG VALUES RETURNED" TO RE-MARK.              IC2014.2
055000 CALL-FAIL-03-02.                                                 IC2014.2
055100     PERFORM FAIL.                                                IC2014.2
055200 CALL-WRITE-03-02.                                                IC2014.2
055300     PERFORM PRINT-DETAIL.                                        IC2014.2
055400 CALL-TEST-03-03.                                                 IC2014.2
055500     MOVE ZERO TO DN3, DN4.                                       IC2014.2
055600     CALL ID1 USING DN1 DN2 DN3 DN4                               IC2014.2
055700         ON OVERFLOW MOVE "OVERFLOW SHOULD NOT OCCUR" TO RE-MARK  IC2014.2
055800                     GO TO CALL-FAIL-03-03.                       IC2014.2
055900     PERFORM   CHECK-TEST-03.                                     IC2014.2
056000     IF FAIL-FLAG EQUAL TO ZERO                                   IC2014.2
056100         PERFORM PASS                                             IC2014.2
056200         GO TO CALL-WRITE-03-03.                                  IC2014.2
056300     MOVE FAIL-FLAG TO COMPUTED-18V0.                             IC2014.2
056400     MOVE "NO. OF WRONG VALUES RETURNED" TO RE-MARK.              IC2014.2
056500 CALL-FAIL-03-03.                                                 IC2014.2
056600     PERFORM FAIL.                                                IC2014.2
056700 CALL-WRITE-03-03.                                                IC2014.2
056800     PERFORM PRINT-DETAIL.                                        IC2014.2
056900 CALL-TEST-03-04.                                                 IC2014.2
057000     MOVE ZERO TO DN3, DN4.                                       IC2014.2
057100     CALL ID1 USING DN1 DN2 DN3 DN4;                              IC2014.2
057200         OVERFLOW MOVE "OVERFLOW SHOULD NOT OCCUR" TO RE-MARK,    IC2014.2
057300                  GO TO CALL-FAIL-03-04.                          IC2014.2
057400     PERFORM CHECK-TEST-03.                                       IC2014.2
057500     IF FAIL-FLAG EQUAL TO ZERO                                   IC2014.2
057600         PERFORM PASS                                             IC2014.2
057700         GO TO CALL-WRITE-03-04.                                  IC2014.2
057800     MOVE FAIL-FLAG TO COMPUTED-18V0.                             IC2014.2
057900     MOVE "NO. OF WRONG VALUES RETURNED" TO RE-MARK.              IC2014.2
058000 CALL-FAIL-03-04.                                                 IC2014.2
058100     PERFORM FAIL.                                                IC2014.2
058200 CALL-WRITE-03-04.                                                IC2014.2
058300     PERFORM PRINT-DETAIL.                                        IC2014.2
058400     GO TO EXIT-IC201.                                            IC2014.2
058500 CALL-DELETE-03.                                                  IC2014.2
058600*        IF THE ON OVERFLOW PHRASE IS NOT RECOGNIZED, DELETE ALL  IC2014.2
058700*    OF THE ABOVE CALL-TEST-03 PARAGRAPHS, STARTING WITH          IC2014.2
058800*    CALL-TEST-03-01.                                             IC2014.2
058900     PERFORM DE-LETE.                                             IC2014.2
059000     PERFORM PRINT-DETAIL.                                        IC2014.2
059100 EXIT-IC201.                                                      IC2014.2
059200     GO TO CCVS-EXIT.                                             IC2014.2
059300 SECT-IC201-0002 SECTION.                                         IC2014.2
059400 CHECK-TEST-01.                                                   IC2014.2
059500     MOVE ZERO TO FAIL-FLAG.                                      IC2014.2
059600     ADD 1 TO CALL-COUNT.                                         IC2014.2
059700     IF DN1 EQUAL TO ZERO                                         IC2014.2
059800             NEXT SENTENCE                                        IC2014.2
059900         ELSE ADD 1 TO FAIL-FLAG.                                 IC2014.2
060000     IF DN2 NOT EQUAL TO ZERO                                     IC2014.2
060100         ADD 1 TO FAIL-FLAG.                                      IC2014.2
060200     IF DN3 NOT EQUAL TO 1                                        IC2014.2
060300         ADD 1 TO FAIL-FLAG.                                      IC2014.2
060400     IF DN4 NOT EQUAL TO CALL-COUNT                               IC2014.2
060500         ADD 1 TO FAIL-FLAG.                                      IC2014.2
060600 CHECK-TEST-03.                                                   IC2014.2
060700     MOVE ZERO TO FAIL-FLAG.                                      IC2014.2
060800     ADD 1 TO CALL-COUNT.                                         IC2014.2
060900     IF DN4 NOT EQUAL TO CALL-COUNT                               IC2014.2
061000         ADD 1 TO FAIL-FLAG.                                      IC2014.2
061100     IF DN3 NOT EQUAL TO 21                                       IC2014.2
061200         ADD 1 TO FAIL-FLAG.                                      IC2014.2
061300     IF DN2 NOT EQUAL TO 30                                       IC2014.2
061400         ADD 1 TO FAIL-FLAG.                                      IC2014.2
061500     IF DN1 NOT EQUAL TO 20                                       IC2014.2
061600         ADD 1 TO FAIL-FLAG.                                      IC2014.2
061700 CCVS-EXIT SECTION.                                               IC2014.2
061800 CCVS-999999.                                                     IC2014.2
061900     GO TO CLOSE-FILES.                                           IC2014.2
      *END-OF,IC201A                                                                  
