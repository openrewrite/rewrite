      *HEADER,CLBRY,K6SCA                                                             
000100 CONFIGURATION SECTION.                                           K6SCA4.2
000200 SOURCE-COMPUTER.                                                 K6SCA4.2
000300     XXXXX082.                                                    K6SCA4.2
000400 OBJECT-COMPUTER.                                                 K6SCA4.2
000500     XXXXX083.                                                    K6SCA4.2
000600 INPUT-OUTPUT SECTION.                                            K6SCA4.2
000700 FILE-CONTROL.                                                    K6SCA4.2
000800     SELECT PRINT-FILE ASSIGN TO                                  K6SCA4.2
000900     XXXXX055.                                                    K6SCA4.2
001000 DATA DIVISION.                                                   K6SCA4.2
001100 FILE SECTION.                                                    K6SCA4.2
001200 FD  PRINT-FILE.                                                  K6SCA4.2
001300 01  PRINT-REC PICTURE X(120).                                    K6SCA4.2
001400 01  DUMMY-RECORD PICTURE X(120).                                 K6SCA4.2
001500 WORKING-STORAGE SECTION.                                         K6SCA4.2
001600 01  TEST-RESULTS.                                                K6SCA4.2
001700     02 FILLER                   PIC X      VALUE SPACE.          K6SCA4.2
001800     02 FEATURE                  PIC X(20)  VALUE SPACE.          K6SCA4.2
001900     02 FILLER                   PIC X      VALUE SPACE.          K6SCA4.2
002000     02 P-OR-F                   PIC X(5)   VALUE SPACE.          K6SCA4.2
002100     02 FILLER                   PIC X      VALUE SPACE.          K6SCA4.2
002200     02  PAR-NAME.                                                K6SCA4.2
002300       03 FILLER                 PIC X(19)  VALUE SPACE.          K6SCA4.2
002400       03  PARDOT-X              PIC X      VALUE SPACE.          K6SCA4.2
002500       03 DOTVALUE               PIC 99     VALUE ZERO.           K6SCA4.2
002600     02 FILLER                   PIC X(8)   VALUE SPACE.          K6SCA4.2
002700     02 RE-MARK                  PIC X(61).                       K6SCA4.2
002800 01  TEST-COMPUTED.                                               K6SCA4.2
002900     02 FILLER                   PIC X(30)  VALUE SPACE.          K6SCA4.2
003000     02 FILLER                   PIC X(17)  VALUE                 K6SCA4.2
003100            "       COMPUTED=".                                   K6SCA4.2
003200     02 COMPUTED-X.                                               K6SCA4.2
003300     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          K6SCA4.2
003400     03 COMPUTED-N               REDEFINES COMPUTED-A             K6SCA4.2
003500                                 PIC -9(9).9(9).                  K6SCA4.2
003600     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         K6SCA4.2
003700     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     K6SCA4.2
003800     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     K6SCA4.2
003900     03       CM-18V0 REDEFINES COMPUTED-A.                       K6SCA4.2
004000         04 COMPUTED-18V0                    PIC -9(18).          K6SCA4.2
004100         04 FILLER                           PIC X.               K6SCA4.2
004200     03 FILLER PIC X(50) VALUE SPACE.                             K6SCA4.2
004300 01  TEST-CORRECT.                                                K6SCA4.2
004400     02 FILLER PIC X(30) VALUE SPACE.                             K6SCA4.2
004500     02 FILLER PIC X(17) VALUE "       CORRECT =".                K6SCA4.2
004600     02 CORRECT-X.                                                K6SCA4.2
004700     03 CORRECT-A                  PIC X(20) VALUE SPACE.         K6SCA4.2
004800     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      K6SCA4.2
004900     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         K6SCA4.2
005000     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     K6SCA4.2
005100     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     K6SCA4.2
005200     03      CR-18V0 REDEFINES CORRECT-A.                         K6SCA4.2
005300         04 CORRECT-18V0                     PIC -9(18).          K6SCA4.2
005400         04 FILLER                           PIC X.               K6SCA4.2
005500     03 FILLER PIC X(2) VALUE SPACE.                              K6SCA4.2
005600     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     K6SCA4.2
005700 01  CCVS-C-1.                                                    K6SCA4.2
005800     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAK6SCA4.2
005900-    "SS  PARAGRAPH-NAME                                          K6SCA4.2
006000-    "       REMARKS".                                            K6SCA4.2
006100     02 FILLER                     PIC X(20)    VALUE SPACE.      K6SCA4.2
006200 01  CCVS-C-2.                                                    K6SCA4.2
006300     02 FILLER                     PIC X        VALUE SPACE.      K6SCA4.2
006400     02 FILLER                     PIC X(6)     VALUE "TESTED".   K6SCA4.2
006500     02 FILLER                     PIC X(15)    VALUE SPACE.      K6SCA4.2
006600     02 FILLER                     PIC X(4)     VALUE "FAIL".     K6SCA4.2
006700     02 FILLER                     PIC X(94)    VALUE SPACE.      K6SCA4.2
006800 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       K6SCA4.2
006900 01  REC-CT                        PIC 99       VALUE ZERO.       K6SCA4.2
007000 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       K6SCA4.2
007100 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       K6SCA4.2
007200 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       K6SCA4.2
007300 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       K6SCA4.2
007400 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       K6SCA4.2
007500 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       K6SCA4.2
007600 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      K6SCA4.2
007700 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       K6SCA4.2
007800 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     K6SCA4.2
007900 01  CCVS-H-1.                                                    K6SCA4.2
008000     02  FILLER                    PIC X(39)    VALUE SPACES.     K6SCA4.2
008100     02  FILLER                    PIC X(42)    VALUE             K6SCA4.2
008200     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 K6SCA4.2
008300     02  FILLER                    PIC X(39)    VALUE SPACES.     K6SCA4.2
008400 01  CCVS-H-2A.                                                   K6SCA4.2
008500   02  FILLER                        PIC X(40)  VALUE SPACE.      K6SCA4.2
008600   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  K6SCA4.2
008700   02  FILLER                        PIC XXXX   VALUE             K6SCA4.2
008800     "4.2 ".                                                      K6SCA4.2
008900   02  FILLER                        PIC X(28)  VALUE             K6SCA4.2
009000            " COPY - NOT FOR DISTRIBUTION".                       K6SCA4.2
009100   02  FILLER                        PIC X(41)  VALUE SPACE.      K6SCA4.2
009200                                                                  K6SCA4.2
009300 01  CCVS-H-2B.                                                   K6SCA4.2
009400   02  FILLER                        PIC X(15)  VALUE             K6SCA4.2
009500            "TEST RESULT OF ".                                    K6SCA4.2
009600   02  TEST-ID                       PIC X(9).                    K6SCA4.2
009700   02  FILLER                        PIC X(4)   VALUE             K6SCA4.2
009800            " IN ".                                               K6SCA4.2
009900   02  FILLER                        PIC X(12)  VALUE             K6SCA4.2
010000     " HIGH       ".                                              K6SCA4.2
010100   02  FILLER                        PIC X(22)  VALUE             K6SCA4.2
010200            " LEVEL VALIDATION FOR ".                             K6SCA4.2
010300   02  FILLER                        PIC X(58)  VALUE             K6SCA4.2
010400     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".K6SCA4.2
010500 01  CCVS-H-3.                                                    K6SCA4.2
010600     02  FILLER                      PIC X(34)  VALUE             K6SCA4.2
010700            " FOR OFFICIAL USE ONLY    ".                         K6SCA4.2
010800     02  FILLER                      PIC X(58)  VALUE             K6SCA4.2
010900     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".K6SCA4.2
011000     02  FILLER                      PIC X(28)  VALUE             K6SCA4.2
011100            "  COPYRIGHT   1985 ".                                K6SCA4.2
011200 01  CCVS-E-1.                                                    K6SCA4.2
011300     02 FILLER                       PIC X(52)  VALUE SPACE.      K6SCA4.2
011400     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              K6SCA4.2
011500     02 ID-AGAIN                     PIC X(9).                    K6SCA4.2
011600     02 FILLER                       PIC X(45)  VALUE SPACES.     K6SCA4.2
011700 01  CCVS-E-2.                                                    K6SCA4.2
011800     02  FILLER                      PIC X(31)  VALUE SPACE.      K6SCA4.2
011900     02  FILLER                      PIC X(21)  VALUE SPACE.      K6SCA4.2
012000     02 CCVS-E-2-2.                                               K6SCA4.2
012100         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      K6SCA4.2
012200         03 FILLER                   PIC X      VALUE SPACE.      K6SCA4.2
012300         03 ENDER-DESC               PIC X(44)  VALUE             K6SCA4.2
012400            "ERRORS ENCOUNTERED".                                 K6SCA4.2
012500 01  CCVS-E-3.                                                    K6SCA4.2
012600     02  FILLER                      PIC X(22)  VALUE             K6SCA4.2
012700            " FOR OFFICIAL USE ONLY".                             K6SCA4.2
012800     02  FILLER                      PIC X(12)  VALUE SPACE.      K6SCA4.2
012900     02  FILLER                      PIC X(58)  VALUE             K6SCA4.2
013000     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".K6SCA4.2
013100     02  FILLER                      PIC X(13)  VALUE SPACE.      K6SCA4.2
013200     02 FILLER                       PIC X(15)  VALUE             K6SCA4.2
013300             " COPYRIGHT 1985".                                   K6SCA4.2
013400 01  CCVS-E-4.                                                    K6SCA4.2
013500     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      K6SCA4.2
013600     02 FILLER                       PIC X(4)   VALUE " OF ".     K6SCA4.2
013700     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      K6SCA4.2
013800     02 FILLER                       PIC X(40)  VALUE             K6SCA4.2
013900      "  TESTS WERE EXECUTED SUCCESSFULLY".                       K6SCA4.2
014000 01  XXINFO.                                                      K6SCA4.2
014100     02 FILLER                       PIC X(19)  VALUE             K6SCA4.2
014200            "*** INFORMATION ***".                                K6SCA4.2
014300     02 INFO-TEXT.                                                K6SCA4.2
014400       04 FILLER                     PIC X(8)   VALUE SPACE.      K6SCA4.2
014500       04 XXCOMPUTED                 PIC X(20).                   K6SCA4.2
014600       04 FILLER                     PIC X(5)   VALUE SPACE.      K6SCA4.2
014700       04 XXCORRECT                  PIC X(20).                   K6SCA4.2
014800     02 INF-ANSI-REFERENCE           PIC X(48).                   K6SCA4.2
014900 01  HYPHEN-LINE.                                                 K6SCA4.2
015000     02 FILLER  PIC IS X VALUE IS SPACE.                          K6SCA4.2
015100     02 FILLER  PIC IS X(65)    VALUE IS "************************K6SCA4.2
015200-    "*****************************************".                 K6SCA4.2
015300     02 FILLER  PIC IS X(54)    VALUE IS "************************K6SCA4.2
015400-    "******************************".                            K6SCA4.2
015500 01  CCVS-PGM-ID                     PIC X(9)   VALUE             K6SCA4.2
015600     "K6SCA".                                                     K6SCA4.2
015700 PROCEDURE DIVISION.                                              K6SCA4.2
015800 CCVS1 SECTION.                                                   K6SCA4.2
015900 OPEN-FILES.                                                      K6SCA4.2
016000     OPEN     OUTPUT PRINT-FILE.                                  K6SCA4.2
016100     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   K6SCA4.2
016200     MOVE    SPACE TO TEST-RESULTS.                               K6SCA4.2
016300     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             K6SCA4.2
016400     GO TO CCVS1-EXIT.                                            K6SCA4.2
016500 CLOSE-FILES.                                                     K6SCA4.2
016600     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   K6SCA4.2
016700 TERMINATE-CCVS.                                                  K6SCA4.2
016800*    EXIT PROGRAM.                                                K6SCA4.2
016900*TERMINATE-CALL.                                                  K6SCA4.2
017000     STOP     RUN.                                                K6SCA4.2
017100 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         K6SCA4.2
017200 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           K6SCA4.2
017300 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          K6SCA4.2
017400 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      K6SCA4.2
017500     MOVE "****TEST DELETED****" TO RE-MARK.                      K6SCA4.2
017600 PRINT-DETAIL.                                                    K6SCA4.2
017700     IF REC-CT NOT EQUAL TO ZERO                                  K6SCA4.2
017800             MOVE "." TO PARDOT-X                                 K6SCA4.2
017900             MOVE REC-CT TO DOTVALUE.                             K6SCA4.2
018000     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      K6SCA4.2
018100     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               K6SCA4.2
018200        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 K6SCA4.2
018300          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 K6SCA4.2
018400     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              K6SCA4.2
018500     MOVE SPACE TO CORRECT-X.                                     K6SCA4.2
018600     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         K6SCA4.2
018700     MOVE     SPACE TO RE-MARK.                                   K6SCA4.2
018800 HEAD-ROUTINE.                                                    K6SCA4.2
018900     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  K6SCA4.2
019000     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  K6SCA4.2
019100     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  K6SCA4.2
019200     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  K6SCA4.2
019300 COLUMN-NAMES-ROUTINE.                                            K6SCA4.2
019400     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           K6SCA4.2
019500     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   K6SCA4.2
019600     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        K6SCA4.2
019700 END-ROUTINE.                                                     K6SCA4.2
019800     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.K6SCA4.2
019900 END-RTN-EXIT.                                                    K6SCA4.2
020000     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   K6SCA4.2
020100 END-ROUTINE-1.                                                   K6SCA4.2
020200      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      K6SCA4.2
020300      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               K6SCA4.2
020400      ADD PASS-COUNTER TO ERROR-HOLD.                             K6SCA4.2
020500*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   K6SCA4.2
020600      MOVE PASS-COUNTER TO CCVS-E-4-1.                            K6SCA4.2
020700      MOVE ERROR-HOLD TO CCVS-E-4-2.                              K6SCA4.2
020800      MOVE CCVS-E-4 TO CCVS-E-2-2.                                K6SCA4.2
020900      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           K6SCA4.2
021000  END-ROUTINE-12.                                                 K6SCA4.2
021100      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        K6SCA4.2
021200     IF       ERROR-COUNTER IS EQUAL TO ZERO                      K6SCA4.2
021300         MOVE "NO " TO ERROR-TOTAL                                K6SCA4.2
021400         ELSE                                                     K6SCA4.2
021500         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       K6SCA4.2
021600     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           K6SCA4.2
021700     PERFORM WRITE-LINE.                                          K6SCA4.2
021800 END-ROUTINE-13.                                                  K6SCA4.2
021900     IF DELETE-COUNTER IS EQUAL TO ZERO                           K6SCA4.2
022000         MOVE "NO " TO ERROR-TOTAL  ELSE                          K6SCA4.2
022100         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      K6SCA4.2
022200     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   K6SCA4.2
022300     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           K6SCA4.2
022400      IF   INSPECT-COUNTER EQUAL TO ZERO                          K6SCA4.2
022500          MOVE "NO " TO ERROR-TOTAL                               K6SCA4.2
022600      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   K6SCA4.2
022700      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            K6SCA4.2
022800      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          K6SCA4.2
022900     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           K6SCA4.2
023000 WRITE-LINE.                                                      K6SCA4.2
023100     ADD 1 TO RECORD-COUNT.                                       K6SCA4.2
023200*    IF RECORD-COUNT GREATER 42                                   K6SCA4.2
023300*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          K6SCA4.2
023400*        MOVE SPACE TO DUMMY-RECORD                               K6SCA4.2
023500*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  K6SCA4.2
023600*        MOVE CCVS-H-1  TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    K6SCA4.2
023700*        MOVE CCVS-H-2A TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    K6SCA4.2
023800*        MOVE CCVS-H-2B TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    K6SCA4.2
023900*        MOVE CCVS-H-3  TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    K6SCA4.2
024000*        MOVE CCVS-C-1  TO DUMMY-RECORD PERFORM WRT-LN            K6SCA4.2
024100*        MOVE CCVS-C-2  TO DUMMY-RECORD PERFORM WRT-LN            K6SCA4.2
024200*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          K6SCA4.2
024300*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          K6SCA4.2
024400*        MOVE ZERO TO RECORD-COUNT.                               K6SCA4.2
024500     PERFORM WRT-LN.                                              K6SCA4.2
024600 WRT-LN.                                                          K6SCA4.2
024700     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               K6SCA4.2
024800     MOVE SPACE TO DUMMY-RECORD.                                  K6SCA4.2
024900 BLANK-LINE-PRINT.                                                K6SCA4.2
025000     PERFORM WRT-LN.                                              K6SCA4.2
025100 FAIL-ROUTINE.                                                    K6SCA4.2
025200     IF     COMPUTED-X NOT EQUAL TO SPACE                         K6SCA4.2
025300            GO TO   FAIL-ROUTINE-WRITE.                           K6SCA4.2
025400     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.K6SCA4.2
025500     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 K6SCA4.2
025600     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   K6SCA4.2
025700     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   K6SCA4.2
025800     MOVE   SPACES TO INF-ANSI-REFERENCE.                         K6SCA4.2
025900     GO TO  FAIL-ROUTINE-EX.                                      K6SCA4.2
026000 FAIL-ROUTINE-WRITE.                                              K6SCA4.2
026100     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         K6SCA4.2
026200     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 K6SCA4.2
026300     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. K6SCA4.2
026400     MOVE   SPACES TO COR-ANSI-REFERENCE.                         K6SCA4.2
026500 FAIL-ROUTINE-EX. EXIT.                                           K6SCA4.2
026600 BAIL-OUT.                                                        K6SCA4.2
026700     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   K6SCA4.2
026800     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           K6SCA4.2
026900 BAIL-OUT-WRITE.                                                  K6SCA4.2
027000     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  K6SCA4.2
027100     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 K6SCA4.2
027200     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   K6SCA4.2
027300     MOVE   SPACES TO INF-ANSI-REFERENCE.                         K6SCA4.2
027400 BAIL-OUT-EX. EXIT.                                               K6SCA4.2
027500 CCVS1-EXIT.                                                      K6SCA4.2
027600     EXIT.                                                        K6SCA4.2
027700 LB106A-INIT SECTION.                                             K6SCA4.2
027800 LB106A-001.                                                      K6SCA4.2
027900     MOVE  " REGARDLESS OF WHAT APPEARS ABOVE OR BELOW, THIS IS THK6SCA4.2
028000-          "E REPORT FOR SM106A" TO PRINT-REC.                    K6SCA4.2
028100     PERFORM WRITE-LINE.                                          K6SCA4.2
028200     PERFORM BLANK-LINE-PRINT.                                    K6SCA4.2
028300     MOVE     " THE PRESENCE OF THIS MESSAGE INDICATES THAT TEXT FK6SCA4.2
028400-    "OR ALL 3 DIVISIONS CAN BE GENERATED BY ONE COPY STATEMENT." K6SCA4.2
028500              TO PRINT-REC.                                       K6SCA4.2
028600     PERFORM       WRITE-LINE.                                    K6SCA4.2
028700     PERFORM       INSPT.                                         K6SCA4.2
028800 CCVS-EXIT SECTION.                                               K6SCA4.2
028900 CCVS-999999.                                                     K6SCA4.2
029000     GO TO CLOSE-FILES.                                           K6SCA4.2
      *END-OF,K6SCA                                                                   
