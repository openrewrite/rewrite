      *HEADER,COBOL,SM206A                                                            
000100 IDENTIFICATION DIVISION.                                         SM2064.2
000200 PROGRAM-ID.                                                      SM2064.2
000300     SM206A.                                                      SM2064.2
000400****************************************************************  SM2064.2
000500*                                                              *  SM2064.2
000600*    VALIDATION FOR:-                                          *  SM2064.2
000700*                                                              *  SM2064.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SM2064.2
000900*                                                              *  SM2064.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SM2064.2
001100*                                                              *  SM2064.2
001200****************************************************************  SM2064.2
001300*                                                              *  SM2064.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  SM2064.2
001500*                                                              *  SM2064.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  SM2064.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  SM2064.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  SM2064.2
001900*                                                              *  SM2064.2
002000****************************************************************  SM2064.2
002100*                                                              *  SM2064.2
002200*    PROGRAM NC206A TESTS THE "REPLACING" PHRASE OF THE "COPY" *  SM2064.2
002300*    STATEMENT USING A VARIETY OF PSEUDO-TEXT OPERANDS.        *  SM2064.2
002400*    MAXIMUM AND MINIMUM LENGTH TEXT WORDS ARE ALSO TESTED.    *  SM2064.2
002500*                                                              *  SM2064.2
002600****************************************************************  SM2064.2
002700 ENVIRONMENT DIVISION.                                            SM2064.2
002800 CONFIGURATION SECTION.                                           SM2064.2
002900 SOURCE-COMPUTER.                                                 SM2064.2
003000     XXXXX082.                                                    SM2064.2
003100 OBJECT-COMPUTER.                                                 SM2064.2
003200     XXXXX083.                                                    SM2064.2
003300 INPUT-OUTPUT SECTION.                                            SM2064.2
003400 FILE-CONTROL.                                                    SM2064.2
003500     SELECT PRINT-FILE ASSIGN TO                                  SM2064.2
003600     XXXXX055.                                                    SM2064.2
003700 DATA DIVISION.                                                   SM2064.2
003800 FILE SECTION.                                                    SM2064.2
003900 FD  PRINT-FILE.                                                  SM2064.2
004000 01  PRINT-REC PICTURE X(120).                                    SM2064.2
004100 01  DUMMY-RECORD PICTURE X(120).                                 SM2064.2
004200 WORKING-STORAGE SECTION.                                         SM2064.2
004300 01  GRP-001.                                                     SM2064.2
004400     02 GRP-002.                                                  SM2064.2
004500        04 GRP-004.                                               SM2064.2
004600           06 GRP-006.                                            SM2064.2
004700              08 WRK-XN-00005-001 PIC X(5) VALUE "FIRST".         SM2064.2
004800              08 WRK-XN-00050-O005F-001  OCCURS 5 TIMES.          SM2064.2
004900                 10 WRK-XN-00005-O005-001 PIC X(5).               SM2064.2
005000                 10 WRK-DS-05V00-O005-001 PIC S9(5).              SM2064.2
005100     02 GRP-003.                                                  SM2064.2
005200        04 GRP-004.                                               SM2064.2
005300           06 GRP-006.                                            SM2064.2
005400              08 WRK-XN-00005-001 PIC X(5) VALUE "SECON".         SM2064.2
005500              08 WRK-XN-00050-O005F-001 OCCURS 5 TIMES.           SM2064.2
005600                 10 WRK-XN-00005-O005-001 PIC X(5).               SM2064.2
005700                 10 WRK-DS-05V00-O005-001 PIC S9(5).              SM2064.2
005800 01  GRP-007.                                                     SM2064.2
005900              08 WRK-XN-00005-001 PIC X(5) VALUE "THIRD".         SM2064.2
006000 01  WRK-DS-09V00-901 PIC S9(9) VALUE ZERO.                       SM2064.2
006100 01  WRK-DS-09V00-902 PIC S9(9) VALUE ZERO.                       SM2064.2
006200 01  WRK-XN-00001     PIC  X.                                     SM2064.2
006300 01  WRK-XN-00322     PIC  X(322).                                SM2064.2
006400 01  FILLER REDEFINES WRK-XN-00322.                               SM2064.2
006500   03  WRK-XN-00322-1         PIC X.                              SM2064.2
006600   03  WRK-XN-00322-2-322.                                        SM2064.2
006700     05  WRK-XN-00322-2-3     PIC X.                              SM2064.2
006800     05  WRK-XN-00322-20      PIC X(20)                           SM2064.2
006900                              OCCURS 16                           SM2064.2
007000                              INDEXED BY X1.                      SM2064.2
007100 01  WRK-DU-9                    PIC 9          VALUE ZERO.       SM2064.2
007200 01  WRK-DU-99                   PIC 99         VALUE ZERO.       SM2064.2
007300 01  WRK-DU-99-LONGER            PIC 99         VALUE ZERO.       SM2064.2
007400 01  TEST-RESULTS.                                                SM2064.2
007500     02 FILLER                   PIC X      VALUE SPACE.          SM2064.2
007600     02 FEATURE                  PIC X(20)  VALUE SPACE.          SM2064.2
007700     02 FILLER                   PIC X      VALUE SPACE.          SM2064.2
007800     02 P-OR-F                   PIC X(5)   VALUE SPACE.          SM2064.2
007900     02 FILLER                   PIC X      VALUE SPACE.          SM2064.2
008000     02  PAR-NAME.                                                SM2064.2
008100       03 FILLER                 PIC X(19)  VALUE SPACE.          SM2064.2
008200       03  PARDOT-X              PIC X      VALUE SPACE.          SM2064.2
008300       03 DOTVALUE               PIC 99     VALUE ZERO.           SM2064.2
008400     02 FILLER                   PIC X(8)   VALUE SPACE.          SM2064.2
008500     02 RE-MARK                  PIC X(61).                       SM2064.2
008600 01  TEST-COMPUTED.                                               SM2064.2
008700     02 FILLER                   PIC X(30)  VALUE SPACE.          SM2064.2
008800     02 FILLER                   PIC X(17)  VALUE                 SM2064.2
008900            "       COMPUTED=".                                   SM2064.2
009000     02 COMPUTED-X.                                               SM2064.2
009100     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          SM2064.2
009200     03 COMPUTED-N               REDEFINES COMPUTED-A             SM2064.2
009300                                 PIC -9(9).9(9).                  SM2064.2
009400     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         SM2064.2
009500     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     SM2064.2
009600     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     SM2064.2
009700     03       CM-18V0 REDEFINES COMPUTED-A.                       SM2064.2
009800         04 COMPUTED-18V0                    PIC -9(18).          SM2064.2
009900         04 FILLER                           PIC X.               SM2064.2
010000     03 FILLER PIC X(50) VALUE SPACE.                             SM2064.2
010100 01  TEST-CORRECT.                                                SM2064.2
010200     02 FILLER PIC X(30) VALUE SPACE.                             SM2064.2
010300     02 FILLER PIC X(17) VALUE "       CORRECT =".                SM2064.2
010400     02 CORRECT-X.                                                SM2064.2
010500     03 CORRECT-A                  PIC X(20) VALUE SPACE.         SM2064.2
010600     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      SM2064.2
010700     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         SM2064.2
010800     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     SM2064.2
010900     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     SM2064.2
011000     03      CR-18V0 REDEFINES CORRECT-A.                         SM2064.2
011100         04 CORRECT-18V0                     PIC -9(18).          SM2064.2
011200         04 FILLER                           PIC X.               SM2064.2
011300     03 FILLER PIC X(2) VALUE SPACE.                              SM2064.2
011400     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     SM2064.2
011500 01  CCVS-C-1.                                                    SM2064.2
011600     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PASM2064.2
011700-    "SS  PARAGRAPH-NAME                                          SM2064.2
011800-    "       REMARKS".                                            SM2064.2
011900     02 FILLER                     PIC X(20)    VALUE SPACE.      SM2064.2
012000 01  CCVS-C-2.                                                    SM2064.2
012100     02 FILLER                     PIC X        VALUE SPACE.      SM2064.2
012200     02 FILLER                     PIC X(6)     VALUE "TESTED".   SM2064.2
012300     02 FILLER                     PIC X(15)    VALUE SPACE.      SM2064.2
012400     02 FILLER                     PIC X(4)     VALUE "FAIL".     SM2064.2
012500     02 FILLER                     PIC X(94)    VALUE SPACE.      SM2064.2
012600 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       SM2064.2
012700 01  REC-CT                        PIC 99       VALUE ZERO.       SM2064.2
012800 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       SM2064.2
012900 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       SM2064.2
013000 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       SM2064.2
013100 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       SM2064.2
013200 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       SM2064.2
013300 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       SM2064.2
013400 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      SM2064.2
013500 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       SM2064.2
013600 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     SM2064.2
013700 01  CCVS-H-1.                                                    SM2064.2
013800     02  FILLER                    PIC X(39)    VALUE SPACES.     SM2064.2
013900     02  FILLER                    PIC X(42)    VALUE             SM2064.2
014000     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 SM2064.2
014100     02  FILLER                    PIC X(39)    VALUE SPACES.     SM2064.2
014200 01  CCVS-H-2A.                                                   SM2064.2
014300   02  FILLER                        PIC X(40)  VALUE SPACE.      SM2064.2
014400   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  SM2064.2
014500   02  FILLER                        PIC XXXX   VALUE             SM2064.2
014600     "4.2 ".                                                      SM2064.2
014700   02  FILLER                        PIC X(28)  VALUE             SM2064.2
014800            " COPY - NOT FOR DISTRIBUTION".                       SM2064.2
014900   02  FILLER                        PIC X(41)  VALUE SPACE.      SM2064.2
015000                                                                  SM2064.2
015100 01  CCVS-H-2B.                                                   SM2064.2
015200   02  FILLER                        PIC X(15)  VALUE             SM2064.2
015300            "TEST RESULT OF ".                                    SM2064.2
015400   02  TEST-ID                       PIC X(9).                    SM2064.2
015500   02  FILLER                        PIC X(4)   VALUE             SM2064.2
015600            " IN ".                                               SM2064.2
015700   02  FILLER                        PIC X(12)  VALUE             SM2064.2
015800     " HIGH       ".                                              SM2064.2
015900   02  FILLER                        PIC X(22)  VALUE             SM2064.2
016000            " LEVEL VALIDATION FOR ".                             SM2064.2
016100   02  FILLER                        PIC X(58)  VALUE             SM2064.2
016200     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SM2064.2
016300 01  CCVS-H-3.                                                    SM2064.2
016400     02  FILLER                      PIC X(34)  VALUE             SM2064.2
016500            " FOR OFFICIAL USE ONLY    ".                         SM2064.2
016600     02  FILLER                      PIC X(58)  VALUE             SM2064.2
016700     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SM2064.2
016800     02  FILLER                      PIC X(28)  VALUE             SM2064.2
016900            "  COPYRIGHT   1985 ".                                SM2064.2
017000 01  CCVS-E-1.                                                    SM2064.2
017100     02 FILLER                       PIC X(52)  VALUE SPACE.      SM2064.2
017200     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              SM2064.2
017300     02 ID-AGAIN                     PIC X(9).                    SM2064.2
017400     02 FILLER                       PIC X(45)  VALUE SPACES.     SM2064.2
017500 01  CCVS-E-2.                                                    SM2064.2
017600     02  FILLER                      PIC X(31)  VALUE SPACE.      SM2064.2
017700     02  FILLER                      PIC X(21)  VALUE SPACE.      SM2064.2
017800     02 CCVS-E-2-2.                                               SM2064.2
017900         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      SM2064.2
018000         03 FILLER                   PIC X      VALUE SPACE.      SM2064.2
018100         03 ENDER-DESC               PIC X(44)  VALUE             SM2064.2
018200            "ERRORS ENCOUNTERED".                                 SM2064.2
018300 01  CCVS-E-3.                                                    SM2064.2
018400     02  FILLER                      PIC X(22)  VALUE             SM2064.2
018500            " FOR OFFICIAL USE ONLY".                             SM2064.2
018600     02  FILLER                      PIC X(12)  VALUE SPACE.      SM2064.2
018700     02  FILLER                      PIC X(58)  VALUE             SM2064.2
018800     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SM2064.2
018900     02  FILLER                      PIC X(13)  VALUE SPACE.      SM2064.2
019000     02 FILLER                       PIC X(15)  VALUE             SM2064.2
019100             " COPYRIGHT 1985".                                   SM2064.2
019200 01  CCVS-E-4.                                                    SM2064.2
019300     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      SM2064.2
019400     02 FILLER                       PIC X(4)   VALUE " OF ".     SM2064.2
019500     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      SM2064.2
019600     02 FILLER                       PIC X(40)  VALUE             SM2064.2
019700      "  TESTS WERE EXECUTED SUCCESSFULLY".                       SM2064.2
019800 01  XXINFO.                                                      SM2064.2
019900     02 FILLER                       PIC X(19)  VALUE             SM2064.2
020000            "*** INFORMATION ***".                                SM2064.2
020100     02 INFO-TEXT.                                                SM2064.2
020200       04 FILLER                     PIC X(8)   VALUE SPACE.      SM2064.2
020300       04 XXCOMPUTED                 PIC X(20).                   SM2064.2
020400       04 FILLER                     PIC X(5)   VALUE SPACE.      SM2064.2
020500       04 XXCORRECT                  PIC X(20).                   SM2064.2
020600     02 INF-ANSI-REFERENCE           PIC X(48).                   SM2064.2
020700 01  HYPHEN-LINE.                                                 SM2064.2
020800     02 FILLER  PIC IS X VALUE IS SPACE.                          SM2064.2
020900     02 FILLER  PIC IS X(65)    VALUE IS "************************SM2064.2
021000-    "*****************************************".                 SM2064.2
021100     02 FILLER  PIC IS X(54)    VALUE IS "************************SM2064.2
021200-    "******************************".                            SM2064.2
021300 01  CCVS-PGM-ID                     PIC X(9)   VALUE             SM2064.2
021400     "SM206A".                                                    SM2064.2
021500 PROCEDURE DIVISION.                                              SM2064.2
021600 CCVS1 SECTION.                                                   SM2064.2
021700 OPEN-FILES.                                                      SM2064.2
021800     OPEN     OUTPUT PRINT-FILE.                                  SM2064.2
021900     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   SM2064.2
022000     MOVE    SPACE TO TEST-RESULTS.                               SM2064.2
022100     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             SM2064.2
022200     GO TO CCVS1-EXIT.                                            SM2064.2
022300 CLOSE-FILES.                                                     SM2064.2
022400     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   SM2064.2
022500 TERMINATE-CCVS.                                                  SM2064.2
022600*    EXIT PROGRAM.                                                SM2064.2
022700*TERMINATE-CALL.                                                  SM2064.2
022800     STOP     RUN.                                                SM2064.2
022900 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         SM2064.2
023000 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           SM2064.2
023100 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          SM2064.2
023200 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      SM2064.2
023300     MOVE "****TEST DELETED****" TO RE-MARK.                      SM2064.2
023400 PRINT-DETAIL.                                                    SM2064.2
023500     IF REC-CT NOT EQUAL TO ZERO                                  SM2064.2
023600             MOVE "." TO PARDOT-X                                 SM2064.2
023700             MOVE REC-CT TO DOTVALUE.                             SM2064.2
023800     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      SM2064.2
023900     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               SM2064.2
024000        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 SM2064.2
024100          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 SM2064.2
024200     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              SM2064.2
024300     MOVE SPACE TO CORRECT-X.                                     SM2064.2
024400     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         SM2064.2
024500     MOVE     SPACE TO RE-MARK.                                   SM2064.2
024600 HEAD-ROUTINE.                                                    SM2064.2
024700     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  SM2064.2
024800     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  SM2064.2
024900     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  SM2064.2
025000     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  SM2064.2
025100 COLUMN-NAMES-ROUTINE.                                            SM2064.2
025200     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SM2064.2
025300     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SM2064.2
025400     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        SM2064.2
025500 END-ROUTINE.                                                     SM2064.2
025600     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.SM2064.2
025700 END-RTN-EXIT.                                                    SM2064.2
025800     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SM2064.2
025900 END-ROUTINE-1.                                                   SM2064.2
026000      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      SM2064.2
026100      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               SM2064.2
026200      ADD PASS-COUNTER TO ERROR-HOLD.                             SM2064.2
026300*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   SM2064.2
026400      MOVE PASS-COUNTER TO CCVS-E-4-1.                            SM2064.2
026500      MOVE ERROR-HOLD TO CCVS-E-4-2.                              SM2064.2
026600      MOVE CCVS-E-4 TO CCVS-E-2-2.                                SM2064.2
026700      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           SM2064.2
026800  END-ROUTINE-12.                                                 SM2064.2
026900      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        SM2064.2
027000     IF       ERROR-COUNTER IS EQUAL TO ZERO                      SM2064.2
027100         MOVE "NO " TO ERROR-TOTAL                                SM2064.2
027200         ELSE                                                     SM2064.2
027300         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       SM2064.2
027400     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           SM2064.2
027500     PERFORM WRITE-LINE.                                          SM2064.2
027600 END-ROUTINE-13.                                                  SM2064.2
027700     IF DELETE-COUNTER IS EQUAL TO ZERO                           SM2064.2
027800         MOVE "NO " TO ERROR-TOTAL  ELSE                          SM2064.2
027900         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      SM2064.2
028000     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   SM2064.2
028100     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SM2064.2
028200      IF   INSPECT-COUNTER EQUAL TO ZERO                          SM2064.2
028300          MOVE "NO " TO ERROR-TOTAL                               SM2064.2
028400      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   SM2064.2
028500      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            SM2064.2
028600      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          SM2064.2
028700     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SM2064.2
028800 WRITE-LINE.                                                      SM2064.2
028900     ADD 1 TO RECORD-COUNT.                                       SM2064.2
029000*    IF RECORD-COUNT GREATER 50                                   SM2064.2
029100*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          SM2064.2
029200*        MOVE SPACE TO DUMMY-RECORD                               SM2064.2
029300*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  SM2064.2
029400*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             SM2064.2
029500*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     SM2064.2
029600*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          SM2064.2
029700*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          SM2064.2
029800*        MOVE ZERO TO RECORD-COUNT.                               SM2064.2
029900     PERFORM WRT-LN.                                              SM2064.2
030000 WRT-LN.                                                          SM2064.2
030100     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               SM2064.2
030200     MOVE SPACE TO DUMMY-RECORD.                                  SM2064.2
030300 BLANK-LINE-PRINT.                                                SM2064.2
030400     PERFORM WRT-LN.                                              SM2064.2
030500 FAIL-ROUTINE.                                                    SM2064.2
030600     IF   COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE. SM2064.2
030700     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.SM2064.2
030800     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 SM2064.2
030900     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   SM2064.2
031000     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SM2064.2
031100     MOVE   SPACES TO INF-ANSI-REFERENCE.                         SM2064.2
031200     GO TO  FAIL-ROUTINE-EX.                                      SM2064.2
031300 FAIL-ROUTINE-WRITE.                                              SM2064.2
031400     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         SM2064.2
031500     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 SM2064.2
031600     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. SM2064.2
031700     MOVE   SPACES TO COR-ANSI-REFERENCE.                         SM2064.2
031800 FAIL-ROUTINE-EX. EXIT.                                           SM2064.2
031900 BAIL-OUT.                                                        SM2064.2
032000     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   SM2064.2
032100     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           SM2064.2
032200 BAIL-OUT-WRITE.                                                  SM2064.2
032300     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  SM2064.2
032400     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 SM2064.2
032500     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SM2064.2
032600     MOVE   SPACES TO INF-ANSI-REFERENCE.                         SM2064.2
032700 BAIL-OUT-EX. EXIT.                                               SM2064.2
032800 CCVS1-EXIT.                                                      SM2064.2
032900     EXIT.                                                        SM2064.2
033000 SECT-SM206-0001 SECTION.                                         SM2064.2
033100*                                                                 SM2064.2
033200*********************** COPY STATEMENT USED **********************SM2064.2
033300*                                                                 SM2064.2
033400*    COPY                                                    KP001SM2064.2
033500*            REPLACING ==PERFORM FAIL. == BY ====.                SM2064.2
033600*                                                                 SM2064.2
033700******************** COPIED TEXT BEGINS BELOW ********************SM2064.2
033800     COPY                                                    KP001SM2064.2
033900             REPLACING ==PERFORM FAIL. == BY ====.                SM2064.2
034000*********************** END OF COPIED TEXT ***********************SM2064.2
034100 SECT-SM206-0002 SECTION.                                         SM2064.2
034200 PST-INIT-002.                                                    SM2064.2
034300     MOVE   +00005 TO WRK-DS-05V00-O005-001 OF GRP-002 (1).       SM2064.2
034400     MOVE   +000000005 TO WRK-DS-09V00-901.                       SM2064.2
034500 PST-TEST-002.                                                    SM2064.2
034600*    THIS TEST EXERCISES THE REPLACING PHRASE BY REPLACING        SM2064.2
034700*    PSEUDO-TEXT BY AN IDENTIFIER.                                SM2064.2
034800     MOVE    "PSEUDO-TEXT/IDENTIFR" TO FEATURE.                   SM2064.2
034900*                                                                 SM2064.2
035000*********************** COPY STATEMENT USED **********************SM2064.2
035100*                                                                 SM2064.2
035200*    COPY                                                    KP002SM2064.2
035300*            REPLACING == WRK-DS-09V00-901                        SM2064.2
035400*                         SUBTRACT 1 FROM                         SM2064.2
035500*                         WRK-DS-05V00-O005-001 IN GRP-002 (1)==  SM2064.2
035600*            BY       WRK-DS-05V00-O005-001 IN WRK-XN-00050-0005  SM2064.2
035700*-(COL 7)          F-001 IN GRP-006 IN GRP-004 IN GRP-002 IN GRP-0SM2064.2
035800*-(COL 7)              01 (1).                                    SM2064.2
035900*                                                                 SM2064.2
036000******************** COPIED TEXT BEGINS BELOW ********************SM2064.2
036100     COPY                                                    KP002SM2064.2
036200             REPLACING == WRK-DS-09V00-901                        SM2064.2
036300                          SUBTRACT 1 FROM                         SM2064.2
036400                          WRK-DS-05V00-O005-001 IN GRP-002 (1)==  SM2064.2
036500             BY         WRK-DS-05V00-O005-001 IN WRK-XN-00050-O005SM2064.2
036600-                  F-001 IN GRP-006 IN GRP-004 IN GRP-002 IN GRP-0SM2064.2
036700-                      01 (1).                                    SM2064.2
036800*********************** END OF COPIED TEXT ***********************SM2064.2
036900     MOVE    "PST-TEST-002" TO PAR-NAME.                          SM2064.2
037000     MOVE    01 TO REC-CT.                                        SM2064.2
037100     IF      WRK-DS-05V00-O005-001 OF GRP-002 (1) EQUAL TO +6     SM2064.2
037200             PERFORM PASS                                         SM2064.2
037300             ELSE                                                 SM2064.2
037400             MOVE +6 TO CORRECT-18V0                              SM2064.2
037500             MOVE WRK-DS-05V00-O005-001 OF GRP-002 (1) TO         SM2064.2
037600                 COMPUTED-18V0                                    SM2064.2
037700             PERFORM FAIL.                                        SM2064.2
037800     PERFORM PRINT-DETAIL.                                        SM2064.2
037900*    THIS IDENTIFIER SHOULD HAVE BEEN INCREMENTED BY ONE AS A     SM2064.2
038000*    RESULT OF THE REPLACING PHRASE SPECIFIED IN THE COPY         SM2064.2
038100*    STATEMENT.                                                   SM2064.2
038200     ADD     +01 TO REC-CT.                                       SM2064.2
038300     IF      WRK-DS-09V00-901 NOT EQUAL TO +5                     SM2064.2
038400             MOVE +5 TO CORRECT-18V0                              SM2064.2
038500             MOVE WRK-DS-09V00-901 TO COMPUTED-18V0               SM2064.2
038600             PERFORM FAIL                                         SM2064.2
038700             ELSE                                                 SM2064.2
038800             PERFORM PASS.                                        SM2064.2
038900*    THIS IDENTIFIER SHOULD NOT HAVE BEEN ALTERED AS A RESULT OF  SM2064.2
039000*    THE REPLACING PHRASE OF THE COPY STATEMENT.                  SM2064.2
039100     PERFORM PRINT-DETAIL.                                        SM2064.2
039200     ADD     +01 TO REC-CT.                                       SM2064.2
039300     IF      WRK-DS-05V00-O005-001 IN WRK-XN-00050-O005F-001 IN   SM2064.2
039400             GRP-006 IN GRP-004 IN GRP-003 (2) EQUAL TO +9        SM2064.2
039500             PERFORM PASS                                         SM2064.2
039600             ELSE                                                 SM2064.2
039700             MOVE WRK-DS-05V00-O005-001   IN                      SM2064.2
039800                  WRK-XN-00050-O005F-001  IN                      SM2064.2
039900                  GRP-006                 IN                      SM2064.2
040000                  GRP-004                 IN                      SM2064.2
040100                  GRP-003 (2) TO COMPUTED-18V0                    SM2064.2
040200             MOVE +9 TO  CORRECT-18V0                             SM2064.2
040300             PERFORM FAIL.                                        SM2064.2
040400*    THE REPLACING PHRASE SHOULD NOT HAVE AFFECTED THE ACTION TO  SM2064.2
040500*    BE TAKEN ON THIS IDENTIFIER IN THE TEXT BEING COPIED.        SM2064.2
040600*                                                                 SM2064.2
040700*                                                                 SM2064.2
040800     PERFORM PRINT-DETAIL.                                        SM2064.2
040900*THIS IS THE BEGINNING OF PST-TEST-003.                           SM2064.2
041000*                                                                 SM2064.2
041100 PST-INIT-003.                                                    SM2064.2
041200     MOVE "PSEUDO-TEXT/LITERAL" TO FEATURE.                       SM2064.2
041300     MOVE "PST-TEST-003" TO PAR-NAME.                             SM2064.2
041400     MOVE    +00005 TO WRK-DS-05V00-O005-001 OF GRP-002 (3).      SM2064.2
041500     MOVE    +000000005 TO WRK-DS-09V00-901.                      SM2064.2
041600     MOVE    ZERO TO  WRK-DS-05V00-O005-001 IN GRP-003 (3).       SM2064.2
041700*                                                                 SM2064.2
041800*********************** COPY STATEMENT USED **********************SM2064.2
041900*                                                                 SM2064.2
042000*    COPY                                                    KP003SM2064.2
042100*             REPLACING ==+00001== BY  +2                         SM2064.2
042200*                       == 1 ==    BY  -3.                        SM2064.2
042300*                                                                 SM2064.2
042400******************** COPIED TEXT BEGINS BELOW ********************SM2064.2
042500     COPY                                                    KP003SM2064.2
042600             REPLACING ==+00001== BY  +2                          SM2064.2
042700                       == 1 ==    BY  -3 .                        SM2064.2
042800*********************** END OF COPIED TEXT ***********************SM2064.2
042900 PST-TEST-003-1.                                                  SM2064.2
043000     MOVE    01 TO REC-CT.                                        SM2064.2
043100     IF      WRK-DS-05V00-O005-001 IN GRP-003 (3) EQUAL TO +00009 SM2064.2
043200             PERFORM PASS                                         SM2064.2
043300             ELSE                                                 SM2064.2
043400             MOVE   +009 TO CORRECT-18V0                          SM2064.2
043500             MOVE   WRK-DS-05V00-O005-001  IN                     SM2064.2
043600                    GRP-003 (3) TO COMPUTED-18V0                  SM2064.2
043700             PERFORM FAIL.                                        SM2064.2
043800     PERFORM PRINT-DETAIL.                                        SM2064.2
043900     ADD     +01 TO REC-CT.                                       SM2064.2
044000     IF      WRK-DS-09V00-901 EQUAL TO +000000007                 SM2064.2
044100             PERFORM PASS                                         SM2064.2
044200             ELSE                                                 SM2064.2
044300             PERFORM FAIL                                         SM2064.2
044400             MOVE  +7 TO CORRECT-18V0                             SM2064.2
044500             MOVE  WRK-DS-09V00-901 TO COMPUTED-18V0.             SM2064.2
044600     PERFORM PRINT-DETAIL.                                        SM2064.2
044700     ADD     +01 TO REC-CT.                                       SM2064.2
044800     IF      WRK-DS-05V00-O005-001 OF GRP-002 (3) EQUAL TO +8     SM2064.2
044900             PERFORM PASS                                         SM2064.2
045000             ELSE                                                 SM2064.2
045100             MOVE +8 TO CORRECT-18V0                              SM2064.2
045200             PERFORM FAIL                                         SM2064.2
045300             MOVE WRK-DS-05V00-O005-001 IN GRP-002 (3) TO         SM2064.2
045400             COMPUTED-18V0.                                       SM2064.2
045500     PERFORM PRINT-DETAIL.                                        SM2064.2
045600     MOVE 0 TO WRK-DS-09V00-901.                                  SM2064.2
045700*THE NEXT BIT OF CODING REPRESENTS WHAT WE FEEL IS PST-TEST-004,  SM2064.2
045800*            WHAT YOU SEE IS WHAT THIS COMPILER FEELS IS          SM2064.2
045900*                PST-TEST-004.                                    SM2064.2
046000*                                                                 SM2064.2
046100*********************** COPY STATEMENT USED **********************SM2064.2
046200*                                                                 SM2064.2
046300*            COPY                                            KP004SM2064.2
046400*                REPLACING ==THIS IS NOT REAL COBOL-74 SYNTAX HOWESM2064.2
046500*-(COL 7)        VER SHOVE==                                      SM2064.2
046600*                BY MOVE                                          SM2064.2
046700*                   == DELETE==                                   SM2064.2
046800*                BY DE-LETE.                                      SM2064.2
046900*                                                                 SM2064.2
047000******************** COPIED TEXT BEGINS BELOW ********************SM2064.2
047100             COPY                                        KP004    SM2064.2
047200                 REPLACING ==THIS IS NOT REAL COBOL-74 SYNTAX HOWESM2064.2
047300-                VER SHOVE==                                      SM2064.2
047400                 BY MOVE                                          SM2064.2
047500                     == DELETE==                                  SM2064.2
047600                 BY  DE-LETE.                                     SM2064.2
047700*********************** END OF COPIED TEXT ***********************SM2064.2
047800 PST-WRITE-004.                                                   SM2064.2
047900     MOVE    "PST-TEST-004" TO PAR-NAME.                          SM2064.2
048000     MOVE    01 TO REC-CT.                                        SM2064.2
048100     IF      WRK-DS-09V00-901 EQUAL TO 5                          SM2064.2
048200             PERFORM PASS                                         SM2064.2
048300             ELSE                                                 SM2064.2
048400             PERFORM FAIL                                         SM2064.2
048500             MOVE 5 TO CORRECT-18V0                               SM2064.2
048600             MOVE WRK-DS-09V00-901 TO COMPUTED-18V0.              SM2064.2
048700     PERFORM PRINT-DETAIL.                                        SM2064.2
048800     ADD     1 TO REC-CT.                                         SM2064.2
048900     IF      WRK-DS-09V00-902 EQUAL TO 2                          SM2064.2
049000             PERFORM PASS                                         SM2064.2
049100             ELSE                                                 SM2064.2
049200             MOVE 2 TO CORRECT-18V0                               SM2064.2
049300             MOVE WRK-DS-09V00-902 TO COMPUTED-18V0               SM2064.2
049400             PERFORM FAIL.                                        SM2064.2
049500     PERFORM PRINT-DETAIL.                                        SM2064.2
049600 PST-TEST-005.                                                    SM2064.2
049700     MOVE 0 TO WRK-DS-09V00-901.                                  SM2064.2
049800*                                                                 SM2064.2
049900*********************** COPY STATEMENT USED **********************SM2064.2
050000*                                                                 SM2064.2
050100*    COPY                                                    KP005SM2064.2
050200*             REPLACING == 1 == BY  == 5 ==                       SM2064.2
050300*                       == 5 == BY  == 7 ==.                      SM2064.2
050400*                                                                 SM2064.2
050500******************** COPIED TEXT BEGINS BELOW ********************SM2064.2
050600     COPY                                                    KP005SM2064.2
050700              REPLACING == 1 == BY  == 5 ==                       SM2064.2
050800                        == 5 == BY  == 7 ==.                      SM2064.2
050900*********************** END OF COPIED TEXT ***********************SM2064.2
051000     IF WRK-DS-09V00-901 IS EQUAL TO 5                            SM2064.2
051100         PERFORM PASS   GO TO PST-WRITE-005.                      SM2064.2
051200     PERFORM FAIL.                                                SM2064.2
051300     MOVE WRK-DS-09V00-901 TO COMPUTED-18V0.                      SM2064.2
051400     MOVE 5 TO CORRECT-18V0.                                      SM2064.2
051500     IF WRK-DS-09V00-901 IS EQUAL TO 7                            SM2064.2
051600         MOVE "CASCADED REPLACEMENT PERFORMED" TO RE-MARK.        SM2064.2
051700     GO TO PST-WRITE-005.                                         SM2064.2
051800 PST-DELETE-005.                                                  SM2064.2
051900     PERFORM DE-LETE.                                             SM2064.2
052000 PST-WRITE-005.                                                   SM2064.2
052100     MOVE "CASCADED REPLACE PST" TO FEATURE.                      SM2064.2
052200     MOVE "PST-TEST-005" TO PAR-NAME.                             SM2064.2
052300     MOVE 01 TO REC-CT.                                           SM2064.2
052400     PERFORM PRINT-DETAIL.                                        SM2064.2
052500 PST-TEST-006.                                                    SM2064.2
052600     MOVE 0 TO WRK-DS-09V00-901.                                  SM2064.2
052700*                                                                 SM2064.2
052800*********************** COPY STATEMENT USED **********************SM2064.2
052900*                                                                 SM2064.2
053000*    COPY                                                    KP006SM2064.2
053100*            REPLACING ==001== BY == 3 ==                         SM2064.2
053200*                      ==005== BY == 7 ==.                        SM2064.2
053300*                                                                 SM2064.2
053400******************** COPIED TEXT BEGINS BELOW ********************SM2064.2
053500     COPY                                                    KP006SM2064.2
053600             REPLACING ==001== BY == 3 ==                         SM2064.2
053700                       ==005== BY == 7 ==.                        SM2064.2
053800*********************** END OF COPIED TEXT ***********************SM2064.2
053900     IF WRK-DS-09V00-901 IS EQUAL TO 1005                         SM2064.2
054000         PERFORM PASS   GO TO PST-WRITE-006.                      SM2064.2
054100     PERFORM FAIL.                                                SM2064.2
054200     MOVE WRK-DS-09V00-901 TO COMPUTED-18V0.                      SM2064.2
054300     MOVE 1005 TO CORRECT-18V0.                                   SM2064.2
054400     IF WRK-DS-09V00-901 IS EQUAL TO 10                           SM2064.2
054500         MOVE "PART REPLACING, CONT IGNORED" TO RE-MARK.          SM2064.2
054600     IF WRK-DS-09V00-901 IS EQUAL TO 37                           SM2064.2
054700         MOVE "PART REPLACING, CONT HONORED" TO RE-MARK.          SM2064.2
054800     GO TO PST-WRITE-006.                                         SM2064.2
054900 PST-DELETE-006.                                                  SM2064.2
055000     PERFORM DE-LETE.                                             SM2064.2
055100 PST-WRITE-006.                                                   SM2064.2
055200     MOVE "CONT LIT/PST PART RPL" TO FEATURE.                     SM2064.2
055300     MOVE "PST-TEST-006" TO PAR-NAME.                             SM2064.2
055400     PERFORM PRINT-DETAIL.                                        SM2064.2
055500 PST-TEST-007.                                                    SM2064.2
055600     PERFORM FAIL.                                                SM2064.2
055700     SUBTRACT 1 FROM ERROR-COUNTER.                               SM2064.2
055800*                                                                 SM2064.2
055900*********************** COPY STATEMENT USED **********************SM2064.2
056000*                                                                 SM2064.2
056100*    COPY                                                   KP007 SM2064.2
056200*        REPLACING ==FAIL. SUBTRACT 1 FROM ERROR-COUNTER. ==      SM2064.2
056300*        BY ==PASS. ==.                                           SM2064.2
056400*                                                                 SM2064.2
056500******************** COPIED TEXT BEGINS BELOW ********************SM2064.2
056600     COPY                                                   KP007 SM2064.2
056700         REPLACING ==FAIL. SUBTRACT 1 FROM ERROR-COUNTER. ==      SM2064.2
056800         BY ==PASS. ==.                                           SM2064.2
056900*********************** END OF COPIED TEXT ***********************SM2064.2
057000     IF P-OR-F IS EQUAL TO "FAIL*"  ADD 1 TO ERROR-COUNTER.       SM2064.2
057100     GO TO PST-WRITE-007.                                         SM2064.2
057200 PST-DELETE-007.                                                  SM2064.2
057300     PERFORM DE-LETE.                                             SM2064.2
057400 PST-WRITE-007.                                                   SM2064.2
057500     MOVE "PST/EMBEDDED COMMENT" TO FEATURE.                      SM2064.2
057600     MOVE "PST-TEST-007" TO PAR-NAME.                             SM2064.2
057700     MOVE 01 TO REC-CT.                                           SM2064.2
057800     PERFORM PRINT-DETAIL.                                        SM2064.2
057900 PST-TEST-008.                                                    SM2064.2
058000*    PERFORM PASS.                                                SM2064.2
058100*                                                                 SM2064.2
058200*********************** COPY STATEMENT USED **********************SM2064.2
058300*                                                                 SM2064.2
058400*D   COPY                                                  KP007. SM2064.2
058500*                                                                 SM2064.2
058600******************** COPIED TEXT BEGINS BELOW ********************SM2064.2
058700*D   COPY                                                  KP007. SM2064.2
058800*********************** END OF COPIED TEXT ***********************SM2064.2
058900*    IF P-OR-F IS EQUAL TO "FAIL*"  ADD 1 TO ERROR-COUNTER.       SM2064.2
059000*    GO TO PST-WRITE-008.                                         SM2064.2
059100 PST-DELETE-008.                                                  SM2064.2
059200     PERFORM DE-LETE.                                             SM2064.2
059300 PST-WRITE-008.                                                   SM2064.2
059400     MOVE "COPY IN DEBUG LINE" TO FEATURE.                        SM2064.2
059500     MOVE "PST-TEST-008" TO PAR-NAME.                             SM2064.2
059600     PERFORM PRINT-DETAIL.                                        SM2064.2
059700 PST-TEST-009.                                                    SM2064.2
059800     PERFORM FAIL.                                                SM2064.2
059900     SUBTRACT 1 FROM ERROR-COUNTER.                               SM2064.2
060000*                                                                 SM2064.2
060100*********************** COPY STATEMENT USED **********************SM2064.2
060200*                                                                 SM2064.2
060300*    COPY                                                   KP008 SM2064.2
060400*        REPLACING ==FAIL. THIS IS GARBAGE. SUBTRACT 1 FROM       SM2064.2
060500*                    ERROR-COUNTER. ==                            SM2064.2
060600*        BY  ==PASS. ==.                                          SM2064.2
060700*                                                                 SM2064.2
060800******************** COPIED TEXT BEGINS BELOW ********************SM2064.2
060900     COPY                                                   KP008 SM2064.2
061000         REPLACING ==FAIL. THIS IS GARBAGE. SUBTRACT 1 FROM       SM2064.2
061100                     ERROR-COUNTER. ==                            SM2064.2
061200         BY  ==PASS. ==.                                          SM2064.2
061300*********************** END OF COPIED TEXT ***********************SM2064.2
061400     IF P-OR-F IS EQUAL TO "FAIL*"  ADD 1 TO ERROR-COUNTER.       SM2064.2
061500     GO TO PST-WRITE-009.                                         SM2064.2
061600 PST-DELETE-009.                                                  SM2064.2
061700     PERFORM DE-LETE.                                             SM2064.2
061800 PST-WRITE-009.                                                   SM2064.2
061900     MOVE "DEBUG LINE IN TEXT" TO FEATURE.                        SM2064.2
062000     MOVE "PST-TEST-009" TO PAR-NAME.                             SM2064.2
062100     PERFORM PRINT-DETAIL.                                        SM2064.2
062200*                                                                 SM2064.2
062300 PST-TEST-10.                                                     SM2064.2
062400*    ===-->  MINIMUM LENGTH TEXT WORD  <--===                     SM2064.2
062500     MOVE   "XII-2 2.3 SR8" TO ANSI-REFERENCE.                    SM2064.2
062600     MOVE   "PST-TEST-10"   TO PAR-NAME.                          SM2064.2
062700     MOVE   "T" TO WRK-XN-00001.                                  SM2064.2
062800     GO TO   PST-TEST-10-0.                                       SM2064.2
062900 PST-DELETE-10.                                                   SM2064.2
063000     PERFORM DE-LETE.                                             SM2064.2
063100     PERFORM PRINT-DETAIL.                                        SM2064.2
063200     GO TO   PST-INIT-11.                                         SM2064.2
063300 PST-TEST-10-0.                                                   SM2064.2
063400********************* COPY TEXT USED ***************************  SM2064.2
063500*    IF      WRK-XN-00001 = "G"                                *  SM2064.2
063600*********************END OF COPY TEXT***************************  SM2064.2
063700     COPY    KP009                                                SM2064.2
063800             REPLACING =="G"== BY =="T"==.                        SM2064.2
063900                                                                  SM2064.2
064000             PERFORM PASS                                         SM2064.2
064100             PERFORM PRINT-DETAIL                                 SM2064.2
064200     ELSE                                                         SM2064.2
064300             MOVE   "REPLACING SINGLE CHARACTER FAILED"           SM2064.2
064400                  TO RE-MARK                                      SM2064.2
064500             MOVE   "T"  TO CORRECT-X                             SM2064.2
064600             MOVE    WRK-XN-00001 TO COMPUTED-X                   SM2064.2
064700             PERFORM FAIL                                         SM2064.2
064800             PERFORM PRINT-DETAIL.                                SM2064.2
064900*                                                                 SM2064.2
065000 PST-INIT-11.                                                     SM2064.2
065100*    ===-->  MAXIMUM LENGTH TEXT WORD  <--===                     SM2064.2
065200     MOVE   "XII-2 2.3 (SR8) AND XII-5 2.4(GR11)"                 SM2064.2
065300          TO ANSI-REFERENCE.                                      SM2064.2
065400     MOVE   "PST-TEST-11" TO PAR-NAME.                            SM2064.2
065500     MOVE    SPACES      TO WRK-XN-00322.                         SM2064.2
065600     MOVE    1 TO REC-CT.                                         SM2064.2
065700 REP-TEST-11-0.                                                   SM2064.2
065800********************* COPY TEXT USED ***************************  SM2064.2
065900*    YYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYSM2064.2
066000*    YYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYSM2064.2
066100*    YYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYSM2064.2
066200*    YYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYSM2064.2
066300*    YYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYSM2064.2
066400*    YYYYYYYYYYYYYYYYY                                            SM2064.2
066500*********************END OF COPY TEXT***************************  SM2064.2
066600 PST-DELETE-11.                                                   SM2064.2
066700     PERFORM DE-LETE.                                             SM2064.2
066800     PERFORM PRINT-DETAIL.                                        SM2064.2
066900     GO TO   CCVS-EXIT.                                           SM2064.2
067000 PST-TEST-11-1.                                                   SM2064.2
067100     MOVE   "PST-TEST-11-1" TO PAR-NAME.                          SM2064.2
067200     IF      WRK-DU-9 = 6                                         SM2064.2
067300             PERFORM PASS                                         SM2064.2
067400             PERFORM PRINT-DETAIL                                 SM2064.2
067500     ELSE                                                         SM2064.2
067600             MOVE   "COPYING ALL 322 CHARACTERS FAILED"           SM2064.2
067700                  TO RE-MARK                                      SM2064.2
067800             MOVE    6   TO CORRECT-N                             SM2064.2
067900             MOVE    WRK-DU-9 TO COMPUTED-N                       SM2064.2
068000             PERFORM FAIL                                         SM2064.2
068100             PERFORM PRINT-DETAIL.                                SM2064.2
068200     ADD     1 TO REC-CT.                                         SM2064.2
068300 PST-TEST-11-2.                                                   SM2064.2
068400     MOVE   "PST-TEST-11-2" TO PAR-NAME.                          SM2064.2
068500     IF      WRK-DU-99 = 9                                        SM2064.2
068600             PERFORM PASS                                         SM2064.2
068700             PERFORM PRINT-DETAIL                                 SM2064.2
068800     ELSE                                                         SM2064.2
068900             MOVE   "COPYING ALL 322 CHARACTERS FAILED"           SM2064.2
069000                  TO RE-MARK                                      SM2064.2
069100             MOVE    9   TO CORRECT-N                             SM2064.2
069200             MOVE    WRK-DU-99 TO COMPUTED-N                      SM2064.2
069300             PERFORM FAIL                                         SM2064.2
069400             PERFORM PRINT-DETAIL.                                SM2064.2
069500     ADD     1 TO REC-CT.                                         SM2064.2
069600 PST-TEST-11-3.                                                   SM2064.2
069700     MOVE   "PST-TEST-11-3" TO PAR-NAME.                          SM2064.2
069800     IF      WRK-DU-99-LONGER = 10                                SM2064.2
069900             PERFORM PASS                                         SM2064.2
070000             PERFORM PRINT-DETAIL                                 SM2064.2
070100     ELSE                                                         SM2064.2
070200             MOVE   "COPYING ALL 322 CHARACTERS FAILED"           SM2064.2
070300                  TO RE-MARK                                      SM2064.2
070400             MOVE    10  TO CORRECT-N                             SM2064.2
070500             MOVE    WRK-DU-99-LONGER TO COMPUTED-N               SM2064.2
070600             PERFORM FAIL                                         SM2064.2
070700             PERFORM PRINT-DETAIL.                                SM2064.2
070800*                                                                 SM2064.2
070900 CCVS-EXIT SECTION.                                               SM2064.2
071000 CCVS-999999.                                                     SM2064.2
071100     GO TO CLOSE-FILES.                                           SM2064.2
      *END-OF,SM206A                                                                  
