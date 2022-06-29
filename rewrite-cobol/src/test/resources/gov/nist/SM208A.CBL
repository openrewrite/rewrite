      *HEADER,COBOL,SM208A                                                            
000100 IDENTIFICATION DIVISION.                                         SM2084.2
000200 PROGRAM-ID. SM208A.                                              SM2084.2
000300 REPLACE OFF.                                                     SM2084.2
000400****************************************************************  SM2084.2
000500*                                                              *  SM2084.2
000600*    VALIDATION FOR:-                                          *  SM2084.2
000700*                                                              *  SM2084.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SM2084.2
000900*                                                              *  SM2084.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SM2084.2
001100*                                                              *  SM2084.2
001200****************************************************************  SM2084.2
001300*                                                              *  SM2084.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  SM2084.2
001500*                                                              *  SM2084.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  SM2084.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  SM2084.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  SM2084.2
001900*                                                              *  SM2084.2
002000****************************************************************  SM2084.2
002100*                                                              *  SM2084.2
002200*    PROGRAM SM208A TESTS FORMATS 1 AND 2 OF THE "REPLACE"     *  SM2084.2
002300*    STATEMENT WITH VARIOUS COMBINATIONS OF PSEUDO-TEXT IN     *  SM2084.2
002400*    EACH OF THE FOUR DIVISIONS.                               *  SM2084.2
002500*                                                              *  SM2084.2
002600****************************************************************  SM2084.2
002700                                                                  SM2084.2
002800                                                                  SM2084.2
002900 ENVIRONMENT DIVISION.                                            SM2084.2
003000 CONFIGURATION SECTION.                                           SM2084.2
003100 SOURCE-COMPUTER.                                                 SM2084.2
003200     XXXXX082.                                                    SM2084.2
003300 OBJECT-COMPUTER.                                                 SM2084.2
003400     XXXXX083.                                                    SM2084.2
003500 INPUT-OUTPUT SECTION.                                            SM2084.2
003600 FILE-CONTROL.                                                    SM2084.2
003700     SELECT PRINT-FILE ASSIGN TO                                  SM2084.2
003800     XXXXX055.                                                    SM2084.2
003900 DATA DIVISION.                                                   SM2084.2
004000 FILE SECTION.                                                    SM2084.2
004100 FD  PRINT-FILE.                                                  SM2084.2
004200 01  PRINT-REC PICTURE X(120).                                    SM2084.2
004300 01  DUMMY-RECORD PICTURE X(120).                                 SM2084.2
004400 WORKING-STORAGE SECTION.                                         SM2084.2
004500*    THE ANSI-REFERENCE FOR THE TEST OF THE FIRST FOUR "01"       SM2084.2
004600*    LEVEL DATA-ITEMS IS "XII-7 3.4 GR3 AND XII-6 3.4 GR2".       SM2084.2
004700 REPLACE ==PICTURE== BY ==PIC==.                                  SM2084.2
004800 01  A     PICTURE X.                                             SM2084.2
004900 01  B     PICTURE S9(7) COMP.                                    SM2084.2
005000 01  C     PICTURE XXBXX/XX.                                      SM2084.2
005100 REPLACE OFF.                                                     SM2084.2
005200 01  D     PICTURE X(7) VALUE "PICTURE".                          SM2084.2
005300 01  WRK-XN-00001  PIC X.                                         SM2084.2
005400 01  WRK-XN-00020  PIC X(20).                                     SM2084.2
005500 01  WRK-XN-00322  PIC X(322).                                    SM2084.2
005600 01  FILLER REDEFINES WRK-XN-00322.                               SM2084.2
005700   03  WRK-XN-00322-1         PIC X.                              SM2084.2
005800   03  WRK-XN-00322-2-322.                                        SM2084.2
005900     05  WRK-XN-00322-2       PIC X.                              SM2084.2
006000     05  WRK-XN-00322-20      PIC X(20)                           SM2084.2
006100                              OCCURS 16                           SM2084.2
006200                              INDEXED BY X1.                      SM2084.2
006300 01  WS-A          PIC X.                                         SM2084.2
006400 01  WS-B          PIC X.                                         SM2084.2
006500 01  WS-C          PIC X.                                         SM2084.2
006600 01  WS-D          PIC X.                                         SM2084.2
006700 01  WS-E          PIC X.                                         SM2084.2
006800 01  WS-F          PIC X.                                         SM2084.2
006900 01  TEST-RESULTS.                                                SM2084.2
007000     02 FILLER                   PIC X      VALUE SPACE.          SM2084.2
007100     02 FEATURE                  PIC X(20)  VALUE SPACE.          SM2084.2
007200     02 FILLER                   PIC X      VALUE SPACE.          SM2084.2
007300     02 P-OR-F                   PIC X(5)   VALUE SPACE.          SM2084.2
007400     02 FILLER                   PIC X      VALUE SPACE.          SM2084.2
007500     02  PAR-NAME.                                                SM2084.2
007600       03 FILLER                 PIC X(19)  VALUE SPACE.          SM2084.2
007700       03  PARDOT-X              PIC X      VALUE SPACE.          SM2084.2
007800       03 DOTVALUE               PIC 99     VALUE ZERO.           SM2084.2
007900     02 FILLER                   PIC X(8)   VALUE SPACE.          SM2084.2
008000     02 RE-MARK                  PIC X(61).                       SM2084.2
008100 01  TEST-COMPUTED.                                               SM2084.2
008200     02 FILLER                   PIC X(30)  VALUE SPACE.          SM2084.2
008300     02 FILLER                   PIC X(17)  VALUE                 SM2084.2
008400            "       COMPUTED=".                                   SM2084.2
008500     02 COMPUTED-X.                                               SM2084.2
008600     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          SM2084.2
008700     03 COMPUTED-N               REDEFINES COMPUTED-A             SM2084.2
008800                                 PIC -9(9).9(9).                  SM2084.2
008900     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         SM2084.2
009000     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     SM2084.2
009100     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     SM2084.2
009200     03       CM-18V0 REDEFINES COMPUTED-A.                       SM2084.2
009300         04 COMPUTED-18V0                    PIC -9(18).          SM2084.2
009400         04 FILLER                           PIC X.               SM2084.2
009500     03 FILLER PIC X(50) VALUE SPACE.                             SM2084.2
009600 01  TEST-CORRECT.                                                SM2084.2
009700     02 FILLER PIC X(30) VALUE SPACE.                             SM2084.2
009800     02 FILLER PIC X(17) VALUE "       CORRECT =".                SM2084.2
009900     02 CORRECT-X.                                                SM2084.2
010000     03 CORRECT-A                  PIC X(20) VALUE SPACE.         SM2084.2
010100     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      SM2084.2
010200     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         SM2084.2
010300     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     SM2084.2
010400     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     SM2084.2
010500     03      CR-18V0 REDEFINES CORRECT-A.                         SM2084.2
010600         04 CORRECT-18V0                     PIC -9(18).          SM2084.2
010700         04 FILLER                           PIC X.               SM2084.2
010800     03 FILLER PIC X(2) VALUE SPACE.                              SM2084.2
010900     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     SM2084.2
011000 01  CCVS-C-1.                                                    SM2084.2
011100     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PASM2084.2
011200-    "SS  PARAGRAPH-NAME                                          SM2084.2
011300-    "       REMARKS".                                            SM2084.2
011400     02 FILLER                     PIC X(20)    VALUE SPACE.      SM2084.2
011500 01  CCVS-C-2.                                                    SM2084.2
011600     02 FILLER                     PIC X        VALUE SPACE.      SM2084.2
011700     02 FILLER                     PIC X(6)     VALUE "TESTED".   SM2084.2
011800     02 FILLER                     PIC X(15)    VALUE SPACE.      SM2084.2
011900     02 FILLER                     PIC X(4)     VALUE "FAIL".     SM2084.2
012000     02 FILLER                     PIC X(94)    VALUE SPACE.      SM2084.2
012100 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       SM2084.2
012200 01  REC-CT                        PIC 99       VALUE ZERO.       SM2084.2
012300 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       SM2084.2
012400 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       SM2084.2
012500 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       SM2084.2
012600 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       SM2084.2
012700 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       SM2084.2
012800 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       SM2084.2
012900 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      SM2084.2
013000 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       SM2084.2
013100 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     SM2084.2
013200 01  CCVS-H-1.                                                    SM2084.2
013300     02  FILLER                    PIC X(39)    VALUE SPACES.     SM2084.2
013400     02  FILLER                    PIC X(42)    VALUE             SM2084.2
013500     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 SM2084.2
013600     02  FILLER                    PIC X(39)    VALUE SPACES.     SM2084.2
013700 01  CCVS-H-2A.                                                   SM2084.2
013800   02  FILLER                        PIC X(40)  VALUE SPACE.      SM2084.2
013900   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  SM2084.2
014000   02  FILLER                        PIC XXXX   VALUE             SM2084.2
014100     "4.2 ".                                                      SM2084.2
014200   02  FILLER                        PIC X(28)  VALUE             SM2084.2
014300            " COPY - NOT FOR DISTRIBUTION".                       SM2084.2
014400   02  FILLER                        PIC X(41)  VALUE SPACE.      SM2084.2
014500                                                                  SM2084.2
014600 01  CCVS-H-2B.                                                   SM2084.2
014700   02  FILLER                        PIC X(15)  VALUE             SM2084.2
014800            "TEST RESULT OF ".                                    SM2084.2
014900   02  TEST-ID                       PIC X(9).                    SM2084.2
015000   02  FILLER                        PIC X(4)   VALUE             SM2084.2
015100            " IN ".                                               SM2084.2
015200   02  FILLER                        PIC X(12)  VALUE             SM2084.2
015300     " HIGH       ".                                              SM2084.2
015400   02  FILLER                        PIC X(22)  VALUE             SM2084.2
015500            " LEVEL VALIDATION FOR ".                             SM2084.2
015600   02  FILLER                        PIC X(58)  VALUE             SM2084.2
015700     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SM2084.2
015800 01  CCVS-H-3.                                                    SM2084.2
015900     02  FILLER                      PIC X(34)  VALUE             SM2084.2
016000            " FOR OFFICIAL USE ONLY    ".                         SM2084.2
016100     02  FILLER                      PIC X(58)  VALUE             SM2084.2
016200     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SM2084.2
016300     02  FILLER                      PIC X(28)  VALUE             SM2084.2
016400            "  COPYRIGHT   1985 ".                                SM2084.2
016500 01  CCVS-E-1.                                                    SM2084.2
016600     02 FILLER                       PIC X(52)  VALUE SPACE.      SM2084.2
016700     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              SM2084.2
016800     02 ID-AGAIN                     PIC X(9).                    SM2084.2
016900     02 FILLER                       PIC X(45)  VALUE SPACES.     SM2084.2
017000 01  CCVS-E-2.                                                    SM2084.2
017100     02  FILLER                      PIC X(31)  VALUE SPACE.      SM2084.2
017200     02  FILLER                      PIC X(21)  VALUE SPACE.      SM2084.2
017300     02 CCVS-E-2-2.                                               SM2084.2
017400         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      SM2084.2
017500         03 FILLER                   PIC X      VALUE SPACE.      SM2084.2
017600         03 ENDER-DESC               PIC X(44)  VALUE             SM2084.2
017700            "ERRORS ENCOUNTERED".                                 SM2084.2
017800 01  CCVS-E-3.                                                    SM2084.2
017900     02  FILLER                      PIC X(22)  VALUE             SM2084.2
018000            " FOR OFFICIAL USE ONLY".                             SM2084.2
018100     02  FILLER                      PIC X(12)  VALUE SPACE.      SM2084.2
018200     02  FILLER                      PIC X(58)  VALUE             SM2084.2
018300     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SM2084.2
018400     02  FILLER                      PIC X(13)  VALUE SPACE.      SM2084.2
018500     02 FILLER                       PIC X(15)  VALUE             SM2084.2
018600             " COPYRIGHT 1985".                                   SM2084.2
018700 01  CCVS-E-4.                                                    SM2084.2
018800     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      SM2084.2
018900     02 FILLER                       PIC X(4)   VALUE " OF ".     SM2084.2
019000     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      SM2084.2
019100     02 FILLER                       PIC X(40)  VALUE             SM2084.2
019200      "  TESTS WERE EXECUTED SUCCESSFULLY".                       SM2084.2
019300 01  XXINFO.                                                      SM2084.2
019400     02 FILLER                       PIC X(19)  VALUE             SM2084.2
019500            "*** INFORMATION ***".                                SM2084.2
019600     02 INFO-TEXT.                                                SM2084.2
019700       04 FILLER                     PIC X(8)   VALUE SPACE.      SM2084.2
019800       04 XXCOMPUTED                 PIC X(20).                   SM2084.2
019900       04 FILLER                     PIC X(5)   VALUE SPACE.      SM2084.2
020000       04 XXCORRECT                  PIC X(20).                   SM2084.2
020100     02 INF-ANSI-REFERENCE           PIC X(48).                   SM2084.2
020200 01  HYPHEN-LINE.                                                 SM2084.2
020300     02 FILLER  PIC IS X VALUE IS SPACE.                          SM2084.2
020400     02 FILLER  PIC IS X(65)    VALUE IS "************************SM2084.2
020500-    "*****************************************".                 SM2084.2
020600     02 FILLER  PIC IS X(54)    VALUE IS "************************SM2084.2
020700-    "******************************".                            SM2084.2
020800 01  CCVS-PGM-ID                     PIC X(9)   VALUE             SM2084.2
020900     "SM208A".                                                    SM2084.2
021000 PROCEDURE DIVISION.                                              SM2084.2
021100 CCVS1 SECTION.                                                   SM2084.2
021200 OPEN-FILES.                                                      SM2084.2
021300     OPEN     OUTPUT PRINT-FILE.                                  SM2084.2
021400     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   SM2084.2
021500     MOVE    SPACE TO TEST-RESULTS.                               SM2084.2
021600     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             SM2084.2
021700     GO TO CCVS1-EXIT.                                            SM2084.2
021800 CLOSE-FILES.                                                     SM2084.2
021900     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   SM2084.2
022000 TERMINATE-CCVS.                                                  SM2084.2
022100*    EXIT PROGRAM.                                                SM2084.2
022200*TERMINATE-CALL.                                                  SM2084.2
022300     STOP     RUN.                                                SM2084.2
022400 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         SM2084.2
022500 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           SM2084.2
022600 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          SM2084.2
022700 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      SM2084.2
022800     MOVE "****TEST DELETED****" TO RE-MARK.                      SM2084.2
022900 PRINT-DETAIL.                                                    SM2084.2
023000     IF REC-CT NOT EQUAL TO ZERO                                  SM2084.2
023100             MOVE "." TO PARDOT-X                                 SM2084.2
023200             MOVE REC-CT TO DOTVALUE.                             SM2084.2
023300     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      SM2084.2
023400     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               SM2084.2
023500        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 SM2084.2
023600          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 SM2084.2
023700     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              SM2084.2
023800     MOVE SPACE TO CORRECT-X.                                     SM2084.2
023900     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         SM2084.2
024000     MOVE     SPACE TO RE-MARK.                                   SM2084.2
024100 HEAD-ROUTINE.                                                    SM2084.2
024200     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  SM2084.2
024300     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  SM2084.2
024400     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  SM2084.2
024500     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  SM2084.2
024600 COLUMN-NAMES-ROUTINE.                                            SM2084.2
024700     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SM2084.2
024800     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SM2084.2
024900     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        SM2084.2
025000 END-ROUTINE.                                                     SM2084.2
025100     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.SM2084.2
025200 END-RTN-EXIT.                                                    SM2084.2
025300     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SM2084.2
025400 END-ROUTINE-1.                                                   SM2084.2
025500      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      SM2084.2
025600      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               SM2084.2
025700      ADD PASS-COUNTER TO ERROR-HOLD.                             SM2084.2
025800*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   SM2084.2
025900      MOVE PASS-COUNTER TO CCVS-E-4-1.                            SM2084.2
026000      MOVE ERROR-HOLD TO CCVS-E-4-2.                              SM2084.2
026100      MOVE CCVS-E-4 TO CCVS-E-2-2.                                SM2084.2
026200      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           SM2084.2
026300  END-ROUTINE-12.                                                 SM2084.2
026400      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        SM2084.2
026500     IF       ERROR-COUNTER IS EQUAL TO ZERO                      SM2084.2
026600         MOVE "NO " TO ERROR-TOTAL                                SM2084.2
026700         ELSE                                                     SM2084.2
026800         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       SM2084.2
026900     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           SM2084.2
027000     PERFORM WRITE-LINE.                                          SM2084.2
027100 END-ROUTINE-13.                                                  SM2084.2
027200     IF DELETE-COUNTER IS EQUAL TO ZERO                           SM2084.2
027300         MOVE "NO " TO ERROR-TOTAL  ELSE                          SM2084.2
027400         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      SM2084.2
027500     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   SM2084.2
027600     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SM2084.2
027700      IF   INSPECT-COUNTER EQUAL TO ZERO                          SM2084.2
027800          MOVE "NO " TO ERROR-TOTAL                               SM2084.2
027900      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   SM2084.2
028000      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            SM2084.2
028100      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          SM2084.2
028200     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SM2084.2
028300 WRITE-LINE.                                                      SM2084.2
028400     ADD 1 TO RECORD-COUNT.                                       SM2084.2
028500*    IF RECORD-COUNT GREATER 50                                   SM2084.2
028600*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          SM2084.2
028700*        MOVE SPACE TO DUMMY-RECORD                               SM2084.2
028800*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  SM2084.2
028900*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             SM2084.2
029000*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     SM2084.2
029100*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          SM2084.2
029200*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          SM2084.2
029300*        MOVE ZERO TO RECORD-COUNT.                               SM2084.2
029400     PERFORM WRT-LN.                                              SM2084.2
029500 WRT-LN.                                                          SM2084.2
029600     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               SM2084.2
029700     MOVE SPACE TO DUMMY-RECORD.                                  SM2084.2
029800 BLANK-LINE-PRINT.                                                SM2084.2
029900     PERFORM WRT-LN.                                              SM2084.2
030000 FAIL-ROUTINE.                                                    SM2084.2
030100     IF   COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE. SM2084.2
030200     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.SM2084.2
030300     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 SM2084.2
030400     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   SM2084.2
030500     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SM2084.2
030600     MOVE   SPACES TO INF-ANSI-REFERENCE.                         SM2084.2
030700     GO TO  FAIL-ROUTINE-EX.                                      SM2084.2
030800 FAIL-ROUTINE-WRITE.                                              SM2084.2
030900     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         SM2084.2
031000     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 SM2084.2
031100     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. SM2084.2
031200     MOVE   SPACES TO COR-ANSI-REFERENCE.                         SM2084.2
031300 FAIL-ROUTINE-EX. EXIT.                                           SM2084.2
031400 BAIL-OUT.                                                        SM2084.2
031500     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   SM2084.2
031600     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           SM2084.2
031700 BAIL-OUT-WRITE.                                                  SM2084.2
031800     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  SM2084.2
031900     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 SM2084.2
032000     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SM2084.2
032100     MOVE   SPACES TO INF-ANSI-REFERENCE.                         SM2084.2
032200 BAIL-OUT-EX. EXIT.                                               SM2084.2
032300 CCVS1-EXIT.                                                      SM2084.2
032400     EXIT.                                                        SM2084.2
032500 SECT-SM208A-001 SECTION.                                         SM2084.2
032600 REP-INIT-1.                                                      SM2084.2
032700*    ===-->  MULTIPLE OPERANDS  <--===                            SM2084.2
032800     MOVE   "XII-6 3.2"  TO ANSI-REFERENCE.                       SM2084.2
032900     MOVE   "REP-TEST-1" TO PAR-NAME.                             SM2084.2
033000     MOVE    SPACE       TO WRK-XN-00001.                         SM2084.2
033100 REP-TEST-1-0.                                                    SM2084.2
033200 REPLACE ==AO==  BY ==TO==                                        SM2084.2
033300         ==IE==  BY ==IF==                                        SM2084.2
033400         == = == BY ==EQUAL==.                                    SM2084.2
033500     GO TO   REP-TEST-1-1.                                        SM2084.2
033600 REP-DELETE-1.                                                    SM2084.2
033700     PERFORM DE-LETE.                                             SM2084.2
033800     PERFORM PRINT-DETAIL.                                        SM2084.2
033900     GO TO   REP-INIT-2.                                          SM2084.2
034000 REP-TEST-1-1.                                                    SM2084.2
034100     MOVE   "*" AO WRK-XN-00001.                                  SM2084.2
034200     IE      WRK-XN-00001 = "*"                                   SM2084.2
034300             PERFORM PASS                                         SM2084.2
034400             PERFORM PRINT-DETAIL                                 SM2084.2
034500     ELSE                                                         SM2084.2
034600             MOVE   "REPLACE FAILED" TO RE-MARK                   SM2084.2
034700             MOVE   "*"  TO CORRECT-X                             SM2084.2
034800             MOVE    WRK-XN-00001 TO COMPUTED-X                   SM2084.2
034900             PERFORM FAIL                                         SM2084.2
035000             PERFORM PRINT-DETAIL.                                SM2084.2
035100 REPLACE OFF.                                                     SM2084.2
035200*                                                                 SM2084.2
035300 REP-INIT-2.                                                      SM2084.2
035400*    ===-->  MINIMUM AND MAXIMUM LENGTHS  <--===                  SM2084.2
035500     MOVE   "XII-6 3.3 (SR5&6) AND XII-8 3.4(GR11)"               SM2084.2
035600          TO ANSI-REFERENCE.                                      SM2084.2
035700     MOVE   "REP-TEST-2" TO PAR-NAME.                             SM2084.2
035800     MOVE    SPACES      TO WRK-XN-00322.                         SM2084.2
035900     MOVE    1 TO REC-CT.                                         SM2084.2
036000 REP-TEST-2-0.                                                    SM2084.2
036100 REPLACE   =="Z"== BY                          =="""""""""""""""""SM2084.2
036200-    """""""""""""""""""""""""""""""""""""""""""""""""""""""""""""SM2084.2
036300-    """""""""""""""""""""""""""""""""""""""""""""""""""""""""""""SM2084.2
036400-    """""""""""""""""""""""""""""""""""""""""""""""""""""""""""""SM2084.2
036500-    """""""""""""""""""""""""""""""""""""""""""""""""""""""""""""SM2084.2
036600-    """""""""""""""""""""""""""""""""""""""""""""""""""""""""""""SM2084.2
036700-    """"""==.                                                    SM2084.2
036800     MOVE "Z" TO WRK-XN-00322.                                    SM2084.2
036900 REPLACE OFF.                                                     SM2084.2
037000     GO TO   REP-TEST-2-1.                                        SM2084.2
037100 REP-DELETE-2.                                                    SM2084.2
037200     PERFORM DE-LETE.                                             SM2084.2
037300     PERFORM PRINT-DETAIL.                                        SM2084.2
037400     GO TO   REP-INIT-3.                                          SM2084.2
037500 REP-TEST-2-1.                                                    SM2084.2
037600     IF      WRK-XN-00322 =                      """""""""""""""""SM2084.2
037700-    """""""""""""""""""""""""""""""""""""""""""""""""""""""""""""SM2084.2
037800-    """""""""""""""""""""""""""""""""""""""""""""""""""""""""""""SM2084.2
037900-    """""""""""""""""""""""""""""""""""""""""""""""""""""""""""""SM2084.2
038000-    """""""""""""""""""""""""""""""""""""""""""""""""""""""""""""SM2084.2
038100-    """""""""""""""""""""""""""""""""""""""""""""""""""""""""""""SM2084.2
038200-    """"""                                                       SM2084.2
038300             PERFORM PASS                                         SM2084.2
038400             PERFORM PRINT-DETAIL                                 SM2084.2
038500     ELSE                                                         SM2084.2
038600             MOVE   "REPLACING SINGLE CHARACTER BY 160 QUOTES"    SM2084.2
038700                  TO RE-MARK                                      SM2084.2
038800             MOVE   """" TO CORRECT-X                             SM2084.2
038900             MOVE    WRK-XN-00322-1 TO COMPUTED-X                 SM2084.2
039000             PERFORM FAIL                                         SM2084.2
039100             PERFORM PRINT-DETAIL                                 SM2084.2
039200             ADD     1 TO REC-CT                                  SM2084.2
039300             MOVE   """""""""""""""" TO CORRECT-X                 SM2084.2
039400             MOVE    WRK-XN-00322-2 TO COMPUTED-X                 SM2084.2
039500*            PERFORM FAIL                                         SM2084.2
039600             PERFORM PRINT-DETAIL                                 SM2084.2
039700             PERFORM WITH TEST AFTER                              SM2084.2
039800                VARYING X1 FROM 1 BY 1                            SM2084.2
039900                   UNTIL X1 > 7                                   SM2084.2
040000                ADD     1 TO REC-CT                               SM2084.2
040100                MOVE """"""""""""""""""""""""""""""""""""""""""   SM2084.2
040200                       TO CORRECT-X                               SM2084.2
040300                MOVE  WRK-XN-00322-20 (X1) TO COMPUTED-X          SM2084.2
040400                PERFORM PRINT-DETAIL                              SM2084.2
040500             END-PERFORM.                                         SM2084.2
040600*                                                                 SM2084.2
040700 REP-INIT-3.                                                      SM2084.2
040800*    ===-->  MINIMUM AND MAXIMUM LENGTHS  <--===                  SM2084.2
040900     MOVE   "XII-6 3.3 (SR5&6) AND XII-8 3.4(GR11)"               SM2084.2
041000          TO ANSI-REFERENCE.                                      SM2084.2
041100     MOVE   "REP-TEST-3" TO PAR-NAME.                             SM2084.2
041200     MOVE    SPACES      TO WRK-XN-00322.                         SM2084.2
041300     MOVE    1 TO REC-CT.                                         SM2084.2
041400 REP-TEST-3-0.                                                    SM2084.2
041500 REPLACE                                       =="""""""""""""""""SM2084.2
041600-    """""""""""""""""""""""""""""""""""""""""""""""""""""""""""""SM2084.2
041700-    """""""""""""""""""""""""""""""""""""""""""""""""""""""""""""SM2084.2
041800-    """""""""""""""""""""""""""""""""""""""""""""""""""""""""""""SM2084.2
041900-    """""""""""""""""""""""""""""""""""""""""""""""""""""""""""""SM2084.2
042000-    """""""""""""""""""""""""""""""""""""""""""""""""""""""""""""SM2084.2
042100-    """"""== BY =="Y"==.                                         SM2084.2
042200     MOVE                                        """""""""""""""""SM2084.2
042300-    """""""""""""""""""""""""""""""""""""""""""""""""""""""""""""SM2084.2
042400-    """""""""""""""""""""""""""""""""""""""""""""""""""""""""""""SM2084.2
042500-    """""""""""""""""""""""""""""""""""""""""""""""""""""""""""""SM2084.2
042600-    """""""""""""""""""""""""""""""""""""""""""""""""""""""""""""SM2084.2
042700-    """""""""""""""""""""""""""""""""""""""""""""""""""""""""""""SM2084.2
042800-    """""" TO WRK-XN-00322.                                      SM2084.2
042900 REPLACE OFF.                                                     SM2084.2
043000     GO TO   REP-TEST-3-1.                                        SM2084.2
043100 REP-DELETE-3.                                                    SM2084.2
043200     PERFORM DE-LETE.                                             SM2084.2
043300     PERFORM PRINT-DETAIL.                                        SM2084.2
043400     GO TO   REP-INIT-4.                                          SM2084.2
043500 REP-TEST-3-1.                                                    SM2084.2
043600     IF      WRK-XN-00322-1 = "Y"                                 SM2084.2
043700         AND WRK-XN-00322-2-322 = SPACES                          SM2084.2
043800             PERFORM PASS                                         SM2084.2
043900             PERFORM PRINT-DETAIL                                 SM2084.2
044000     ELSE                                                         SM2084.2
044100             MOVE   "REPLACING 160 QUOTES BY A SINGLE CHARACTER"  SM2084.2
044200                  TO RE-MARK                                      SM2084.2
044300             MOVE   "Y"  TO CORRECT-X                             SM2084.2
044400             MOVE    WRK-XN-00322-1 TO COMPUTED-X                 SM2084.2
044500             PERFORM FAIL                                         SM2084.2
044600             PERFORM PRINT-DETAIL                                 SM2084.2
044700             ADD     1 TO REC-CT                                  SM2084.2
044800             MOVE    SPACE TO CORRECT-X                           SM2084.2
044900             MOVE    WRK-XN-00322-2 TO COMPUTED-X                 SM2084.2
045000*            PERFORM FAIL                                         SM2084.2
045100             PERFORM PRINT-DETAIL                                 SM2084.2
045200             PERFORM WITH TEST AFTER                              SM2084.2
045300                     VARYING X1 FROM 1 BY 1                       SM2084.2
045400                       UNTIL X1 > 7                               SM2084.2
045500                     ADD     1 TO REC-CT                          SM2084.2
045600                     MOVE    SPACES TO CORRECT-X                  SM2084.2
045700                     MOVE    WRK-XN-00322-20 (X1) TO COMPUTED-X   SM2084.2
045800                     PERFORM PRINT-DETAIL                         SM2084.2
045900             END-PERFORM.                                         SM2084.2
046000*                                                                 SM2084.2
046100 REP-INIT-4.                                                      SM2084.2
046200*    ===-->  INSERTING SPACES  <--===                             SM2084.2
046300     MOVE   "XII-8 3.4 (GR10)"  TO ANSI-REFERENCE.                SM2084.2
046400     MOVE   "REP-TEST-4" TO PAR-NAME.                             SM2084.2
046500     MOVE    SPACE       TO WRK-XN-00001.                         SM2084.2
046600 REP-TEST-4-0.                                                    SM2084.2
046700 REPLACE ==MOVE   "*" AO WRK-XN-00001.                            SM2084.2
046800                  IE      WRK-XN-00001 = "*"==                    SM2084.2
046900     BY                                                           SM2084.2
047000         ==MOVE   "*" TO WRK-XN-00001.                            SM2084.2
047100                                                                  SM2084.2
047200                  IF      WRK-XN-00001 = "*"==.                   SM2084.2
047300     GO TO   REP-TEST-4-1.                                        SM2084.2
047400 REP-DELETE-4.                                                    SM2084.2
047500     PERFORM DE-LETE.                                             SM2084.2
047600     PERFORM PRINT-DETAIL.                                        SM2084.2
047700     GO TO   REP-INIT-5.                                          SM2084.2
047800 REP-TEST-4-1.                                                    SM2084.2
047900     MOVE   "*" AO WRK-XN-00001.                                  SM2084.2
048000     IE      WRK-XN-00001 = "*"                                   SM2084.2
048100             PERFORM PASS                                         SM2084.2
048200             PERFORM PRINT-DETAIL                                 SM2084.2
048300     ELSE                                                         SM2084.2
048400             MOVE   "REPLACE FAILED" TO RE-MARK                   SM2084.2
048500             MOVE   "*"  TO CORRECT-X                             SM2084.2
048600             MOVE    WRK-XN-00001 TO COMPUTED-X                   SM2084.2
048700             PERFORM FAIL                                         SM2084.2
048800             PERFORM PRINT-DETAIL.                                SM2084.2
048900 REPLACE OFF.                                                     SM2084.2
049000*                                                                 SM2084.2
049100 REP-INIT-5.                                                      SM2084.2
049200*    ===-->  DELETING SOURCE  <--===                              SM2084.2
049300     MOVE   "XII-6 3.3 (SR4)" TO ANSI-REFERENCE.                  SM2084.2
049400     MOVE   "REP-TEST-5" TO PAR-NAME.                             SM2084.2
049500     MOVE    SPACES      TO WRK-XN-00020 WRK-XN-00001.            SM2084.2
049600 REP-TEST-5-0.                                                    SM2084.2
049700 REPLACE ==NOT== BY ====.                                         SM2084.2
049800     MOVE   "AA BB CC DD EE FF GG" TO WRK-XN-00020.               SM2084.2
049900     IF WRK-XN-00020 NOT EQUAL SPACES                             SM2084.2
050000         MOVE "*" TO WRK-XN-00001.                                SM2084.2
050100 REPLACE OFF.                                                     SM2084.2
050200     GO TO   REP-TEST-5-1.                                        SM2084.2
050300 REP-DELETE-5.                                                    SM2084.2
050400     PERFORM DE-LETE.                                             SM2084.2
050500     PERFORM PRINT-DETAIL.                                        SM2084.2
050600     GO TO   REP-INIT-6.                                          SM2084.2
050700 REP-TEST-5-1.                                                    SM2084.2
050800     IF      WRK-XN-00001 EQUAL SPACES                            SM2084.2
050900             PERFORM PASS                                         SM2084.2
051000             PERFORM PRINT-DETAIL                                 SM2084.2
051100     ELSE                                                         SM2084.2
051200             MOVE   "REPLACE FAILED" TO RE-MARK                   SM2084.2
051300             PERFORM FAIL                                         SM2084.2
051400             PERFORM PRINT-DETAIL.                                SM2084.2
051500*                                                                 SM2084.2
051600 REP-INIT-6.                                                      SM2084.2
051700*    ===-->  EMBEDDED COMMENT AND BLANK LINES <--===              SM2084.2
051800     MOVE   "XII-7/8 3.4 (GR7)" TO ANSI-REFERENCE.                SM2084.2
051900     MOVE   "REP-TEST-6" TO PAR-NAME.                             SM2084.2
052000 REP-TEST-6-0.                                                    SM2084.2
052100 REPLACE ==MOVE "FAIL" TO==                                       SM2084.2
052200      BY ==MOVE "PASS" TO==.                                      SM2084.2
052300      MOVE                                                        SM2084.2
052400*                                                                 SM2084.2
052500*                                                                 SM2084.2
052600*                                                                 SM2084.2
052700     "FAIL"                                                       SM2084.2
052800                                                                  SM2084.2
052900     TO P-OR-F.                                                   SM2084.2
053000                                                                  SM2084.2
053100*                                                                 SM2084.2
053200 REPLACE OFF.                                                     SM2084.2
053300     GO TO   REP-TEST-6-1.                                        SM2084.2
053400 REP-DELETE-6.                                                    SM2084.2
053500     PERFORM DE-LETE.                                             SM2084.2
053600     PERFORM PRINT-DETAIL.                                        SM2084.2
053700     GO TO   REP-INIT-7.                                          SM2084.2
053800 REP-TEST-6-1.                                                    SM2084.2
053900     IF      P-OR-F = "PASS"                                      SM2084.2
054000             PERFORM PASS                                         SM2084.2
054100             PERFORM PRINT-DETAIL                                 SM2084.2
054200     ELSE                                                         SM2084.2
054300             MOVE   "REPLACE FAILED" TO RE-MARK                   SM2084.2
054400             MOVE   "PASS"  TO CORRECT-X                          SM2084.2
054500             MOVE    P-OR-F TO COMPUTED-X                         SM2084.2
054600             PERFORM FAIL                                         SM2084.2
054700             PERFORM PRINT-DETAIL.                                SM2084.2
054800*                                                                 SM2084.2
054900 REP-INIT-7.                                                      SM2084.2
055000*    ===-->  EMBEDDED DEBUG LINES <--===                          SM2084.2
055100     MOVE   "XII-8 3.4 (GR8)" TO ANSI-REFERENCE.                  SM2084.2
055200     MOVE   "REP-TEST-7" TO PAR-NAME.                             SM2084.2
055300     MOVE   "A" TO WS-A.                                          SM2084.2
055400     MOVE   "B" TO WS-B.                                          SM2084.2
055500     MOVE   "C" TO WS-C.                                          SM2084.2
055600     MOVE   "D" TO WS-D.                                          SM2084.2
055700     MOVE   "E" TO WS-E.                                          SM2084.2
055800     MOVE   "F" TO WS-F.                                          SM2084.2
055900 REP-TEST-7-0.                                                    SM2084.2
056000 REPLACE ==MOVE WS-A TO WS-B==                                    SM2084.2
056100      BY ==MOVE WS-C TO WS-B==                                    SM2084.2
056200         ==MOVE WS-D TO WS-F==                                    SM2084.2
056300      BY ==MOVE WS-E TO WS-F==.                                   SM2084.2
056400                                                                  SM2084.2
056500     MOVE WS-A TO WS-B.                                           SM2084.2
056600                                                                  SM2084.2
056700*D    MOVE                                                        SM2084.2
056800*D    WS-D                                                        SM2084.2
056900*D    TO WS-F.                                                    SM2084.2
057000                                                                  SM2084.2
057100*                                                                 SM2084.2
057200 REPLACE OFF.                                                     SM2084.2
057300*    GO TO   REP-TEST-7-1.                                        SM2084.2
057400 REP-DELETE-7.                                                    SM2084.2
057500     PERFORM DE-LETE.                                             SM2084.2
057600     PERFORM PRINT-DETAIL.                                        SM2084.2
057700     GO TO   REP-INIT-8.                                          SM2084.2
057800 REP-TEST-7-1.                                                    SM2084.2
057900     IF      WS-B = "C"                                           SM2084.2
058000             PERFORM PASS                                         SM2084.2
058100             PERFORM PRINT-DETAIL                                 SM2084.2
058200     ELSE                                                         SM2084.2
058300             MOVE   "REPLACE FAILED" TO RE-MARK                   SM2084.2
058400             MOVE   "C"   TO CORRECT-X                            SM2084.2
058500             MOVE    WS-B TO COMPUTED-X                           SM2084.2
058600             PERFORM FAIL                                         SM2084.2
058700             PERFORM PRINT-DETAIL.                                SM2084.2
058800*                                                                 SM2084.2
058900 REP-INIT-8.                                                      SM2084.2
059000*    ===-->  SEPARATORS  <--===                                   SM2084.2
059100     MOVE   "XII-7 3.4 GR6(b)" TO ANSI-REFERENCE.                 SM2084.2
059200     MOVE   "REP-TEST-8" TO PAR-NAME.                             SM2084.2
059300     MOVE    SPACES      TO P-OR-F.                               SM2084.2
059400 REP-TEST-8-0.                                                    SM2084.2
059500 REPLACE ==MOVE;  "FAIL"  , TO==                                  SM2084.2
059600      BY ==MOVE "PASS" TO==.                                      SM2084.2
059700      MOVE  , "FAIL";      TO  P-OR-F.                            SM2084.2
059800 REPLACE OFF.                                                     SM2084.2
059900     GO TO   REP-TEST-8-1.                                        SM2084.2
060000 REP-DELETE-8.                                                    SM2084.2
060100     PERFORM DE-LETE.                                             SM2084.2
060200     PERFORM PRINT-DETAIL.                                        SM2084.2
060300     GO TO   REP-INIT-9.                                          SM2084.2
060400 REP-TEST-8-1.                                                    SM2084.2
060500     IF      P-OR-F = "PASS"                                      SM2084.2
060600             PERFORM PASS                                         SM2084.2
060700             PERFORM PRINT-DETAIL                                 SM2084.2
060800     ELSE                                                         SM2084.2
060900             MOVE   "REPLACE FAILED" TO RE-MARK                   SM2084.2
061000             MOVE   "PASS"  TO CORRECT-X                          SM2084.2
061100             MOVE    P-OR-F TO COMPUTED-X                         SM2084.2
061200             PERFORM FAIL                                         SM2084.2
061300             PERFORM PRINT-DETAIL.                                SM2084.2
061400*                                                                 SM2084.2
061500 REP-INIT-9.                                                      SM2084.2
061600*    ===-->  SEQUENCE OF COPY AND REPLACE STATEMENTS  <--===      SM2084.2
061700     MOVE   "XII-7 3.4 GR4" TO ANSI-REFERENCE.                    SM2084.2
061800     MOVE   "REP-TEST-9" TO PAR-NAME.                             SM2084.2
061900     MOVE   "FAIL"       TO P-OR-F.                               SM2084.2
062000 REP-TEST-9-0.                                                    SM2084.2
062100 REPLACE =="FAIL"== BY =="PASS"==.                                SM2084.2
062200     COPY    KK208A.                                              SM2084.2
062300 REPLACE OFF.                                                     SM2084.2
062400     GO TO   REP-TEST-9-1.                                        SM2084.2
062500 REP-DELETE-9.                                                    SM2084.2
062600     PERFORM DE-LETE.                                             SM2084.2
062700     PERFORM PRINT-DETAIL.                                        SM2084.2
062800     GO TO   CCVS-EXIT.                                           SM2084.2
062900 REP-TEST-9-1.                                                    SM2084.2
063000     IF      P-OR-F = "PASS"                                      SM2084.2
063100             PERFORM PASS                                         SM2084.2
063200             PERFORM PRINT-DETAIL                                 SM2084.2
063300     ELSE                                                         SM2084.2
063400             MOVE   "REPLACE FAILED" TO RE-MARK                   SM2084.2
063500             MOVE   "PASS"  TO CORRECT-X                          SM2084.2
063600             MOVE    P-OR-F TO COMPUTED-X                         SM2084.2
063700             PERFORM FAIL                                         SM2084.2
063800             PERFORM PRINT-DETAIL.                                SM2084.2
063900*                                                                 SM2084.2
064000 CCVS-EXIT SECTION.                                               SM2084.2
064100 CCVS-999999.                                                     SM2084.2
064200     GO TO CLOSE-FILES.                                           SM2084.2
      *END-OF,SM208A                                                                  
