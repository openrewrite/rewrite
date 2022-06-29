      *HEADER,COBOL,SM203A,SUBPRG,SM204A                                              
000100 IDENTIFICATION DIVISION.                                         SM2044.2
000200 PROGRAM-ID.                                                      SM2044.2
000300     SM204A.                                                      SM2044.2
000400****************************************************************  SM2044.2
000500*                                                              *  SM2044.2
000600*    VALIDATION FOR:-                                          *  SM2044.2
000700*                                                              *  SM2044.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SM2044.2
000900*                                                              *  SM2044.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SM2044.2
001100*                                                              *  SM2044.2
001200****************************************************************  SM2044.2
001300*                                                              *  SM2044.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  SM2044.2
001500*                                                              *  SM2044.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  SM2044.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  SM2044.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  SM2044.2
001900*                                                              *  SM2044.2
002000****************************************************************  SM2044.2
002100*                                                              *  SM2044.2
002200*    SM204A CHECKS THE FILE PRODUCED BY PROGRAM SM203A TO      *  SM2044.2
002300*    VERIFY THE PROPER EXECUTION OF THE "COPY"ED STATEMENTS    *  SM2044.2
002400*    IN THAT PROGRAM'S ENVIRONMENT DIVISION.                   *  SM2044.2
002500*                                                              *  SM2044.2
002600****************************************************************  SM2044.2
002700 ENVIRONMENT DIVISION.                                            SM2044.2
002800 CONFIGURATION SECTION.                                           SM2044.2
002900 SOURCE-COMPUTER.                                                 SM2044.2
003000     XXXXX082.                                                    SM2044.2
003100 OBJECT-COMPUTER.                                                 SM2044.2
003200     XXXXX083.                                                    SM2044.2
003300 INPUT-OUTPUT SECTION.                                            SM2044.2
003400 FILE-CONTROL.                                                    SM2044.2
003500     SELECT PRINT-FILE ASSIGN TO                                  SM2044.2
003600     XXXXX055.                                                    SM2044.2
003700     SELECT TEST-FILE ASSIGN TO                                   SM2044.2
003800* THE FOLLOWING LINE WILL BE CHANGED BY TPF ONLY WHEN THE         SM2044.2
003900* PROGRAM-ID IS PART OF THE REPLACEMENT BY THE X-CARD             SM2044.2
004000* DURING EXTRACTION.                                              SM2044.2
004100     XXXXD002.                                                    SM2044.2
004200 DATA DIVISION.                                                   SM2044.2
004300 FILE SECTION.                                                    SM2044.2
004400 FD  PRINT-FILE.                                                  SM2044.2
004500 01  PRINT-REC PICTURE X(120).                                    SM2044.2
004600 01  DUMMY-RECORD PICTURE X(120).                                 SM2044.2
004700 FD  TEST-FILE                                                    SM2044.2
004800     LABEL RECORD STANDARD                                        SM2044.2
004900C    VALUE OF                                                     SM2044.2
005000C    XXXXX074                                                     SM2044.2
005100C    IS                                                           SM2044.2
005200C    XXXXX077                                                     SM2044.2
005300*    XXXXX069                                                     SM2044.2
005400     DATA RECORD PROOF-REC.                                       SM2044.2
005500 01  PROOF-REC.                                                   SM2044.2
005600     02  TF-1 PICTURE 9(5).                                       SM2044.2
005700     02  FILLER PICTURE X(115).                                   SM2044.2
005800 WORKING-STORAGE SECTION.                                         SM2044.2
005900 01  TEST-RESULTS.                                                SM2044.2
006000     02 FILLER                   PIC X      VALUE SPACE.          SM2044.2
006100     02 FEATURE                  PIC X(20)  VALUE SPACE.          SM2044.2
006200     02 FILLER                   PIC X      VALUE SPACE.          SM2044.2
006300     02 P-OR-F                   PIC X(5)   VALUE SPACE.          SM2044.2
006400     02 FILLER                   PIC X      VALUE SPACE.          SM2044.2
006500     02  PAR-NAME.                                                SM2044.2
006600       03 FILLER                 PIC X(19)  VALUE SPACE.          SM2044.2
006700       03  PARDOT-X              PIC X      VALUE SPACE.          SM2044.2
006800       03 DOTVALUE               PIC 99     VALUE ZERO.           SM2044.2
006900     02 FILLER                   PIC X(8)   VALUE SPACE.          SM2044.2
007000     02 RE-MARK                  PIC X(61).                       SM2044.2
007100 01  TEST-COMPUTED.                                               SM2044.2
007200     02 FILLER                   PIC X(30)  VALUE SPACE.          SM2044.2
007300     02 FILLER                   PIC X(17)  VALUE                 SM2044.2
007400            "       COMPUTED=".                                   SM2044.2
007500     02 COMPUTED-X.                                               SM2044.2
007600     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          SM2044.2
007700     03 COMPUTED-N               REDEFINES COMPUTED-A             SM2044.2
007800                                 PIC -9(9).9(9).                  SM2044.2
007900     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         SM2044.2
008000     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     SM2044.2
008100     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     SM2044.2
008200     03       CM-18V0 REDEFINES COMPUTED-A.                       SM2044.2
008300         04 COMPUTED-18V0                    PIC -9(18).          SM2044.2
008400         04 FILLER                           PIC X.               SM2044.2
008500     03 FILLER PIC X(50) VALUE SPACE.                             SM2044.2
008600 01  TEST-CORRECT.                                                SM2044.2
008700     02 FILLER PIC X(30) VALUE SPACE.                             SM2044.2
008800     02 FILLER PIC X(17) VALUE "       CORRECT =".                SM2044.2
008900     02 CORRECT-X.                                                SM2044.2
009000     03 CORRECT-A                  PIC X(20) VALUE SPACE.         SM2044.2
009100     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      SM2044.2
009200     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         SM2044.2
009300     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     SM2044.2
009400     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     SM2044.2
009500     03      CR-18V0 REDEFINES CORRECT-A.                         SM2044.2
009600         04 CORRECT-18V0                     PIC -9(18).          SM2044.2
009700         04 FILLER                           PIC X.               SM2044.2
009800     03 FILLER PIC X(2) VALUE SPACE.                              SM2044.2
009900     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     SM2044.2
010000 01  CCVS-C-1.                                                    SM2044.2
010100     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PASM2044.2
010200-    "SS  PARAGRAPH-NAME                                          SM2044.2
010300-    "       REMARKS".                                            SM2044.2
010400     02 FILLER                     PIC X(20)    VALUE SPACE.      SM2044.2
010500 01  CCVS-C-2.                                                    SM2044.2
010600     02 FILLER                     PIC X        VALUE SPACE.      SM2044.2
010700     02 FILLER                     PIC X(6)     VALUE "TESTED".   SM2044.2
010800     02 FILLER                     PIC X(15)    VALUE SPACE.      SM2044.2
010900     02 FILLER                     PIC X(4)     VALUE "FAIL".     SM2044.2
011000     02 FILLER                     PIC X(94)    VALUE SPACE.      SM2044.2
011100 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       SM2044.2
011200 01  REC-CT                        PIC 99       VALUE ZERO.       SM2044.2
011300 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       SM2044.2
011400 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       SM2044.2
011500 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       SM2044.2
011600 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       SM2044.2
011700 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       SM2044.2
011800 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       SM2044.2
011900 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      SM2044.2
012000 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       SM2044.2
012100 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     SM2044.2
012200 01  CCVS-H-1.                                                    SM2044.2
012300     02  FILLER                    PIC X(39)    VALUE SPACES.     SM2044.2
012400     02  FILLER                    PIC X(42)    VALUE             SM2044.2
012500     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 SM2044.2
012600     02  FILLER                    PIC X(39)    VALUE SPACES.     SM2044.2
012700 01  CCVS-H-2A.                                                   SM2044.2
012800   02  FILLER                        PIC X(40)  VALUE SPACE.      SM2044.2
012900   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  SM2044.2
013000   02  FILLER                        PIC XXXX   VALUE             SM2044.2
013100     "4.2 ".                                                      SM2044.2
013200   02  FILLER                        PIC X(28)  VALUE             SM2044.2
013300            " COPY - NOT FOR DISTRIBUTION".                       SM2044.2
013400   02  FILLER                        PIC X(41)  VALUE SPACE.      SM2044.2
013500                                                                  SM2044.2
013600 01  CCVS-H-2B.                                                   SM2044.2
013700   02  FILLER                        PIC X(15)  VALUE             SM2044.2
013800            "TEST RESULT OF ".                                    SM2044.2
013900   02  TEST-ID                       PIC X(9).                    SM2044.2
014000   02  FILLER                        PIC X(4)   VALUE             SM2044.2
014100            " IN ".                                               SM2044.2
014200   02  FILLER                        PIC X(12)  VALUE             SM2044.2
014300     " HIGH       ".                                              SM2044.2
014400   02  FILLER                        PIC X(22)  VALUE             SM2044.2
014500            " LEVEL VALIDATION FOR ".                             SM2044.2
014600   02  FILLER                        PIC X(58)  VALUE             SM2044.2
014700     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SM2044.2
014800 01  CCVS-H-3.                                                    SM2044.2
014900     02  FILLER                      PIC X(34)  VALUE             SM2044.2
015000            " FOR OFFICIAL USE ONLY    ".                         SM2044.2
015100     02  FILLER                      PIC X(58)  VALUE             SM2044.2
015200     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SM2044.2
015300     02  FILLER                      PIC X(28)  VALUE             SM2044.2
015400            "  COPYRIGHT   1985 ".                                SM2044.2
015500 01  CCVS-E-1.                                                    SM2044.2
015600     02 FILLER                       PIC X(52)  VALUE SPACE.      SM2044.2
015700     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              SM2044.2
015800     02 ID-AGAIN                     PIC X(9).                    SM2044.2
015900     02 FILLER                       PIC X(45)  VALUE SPACES.     SM2044.2
016000 01  CCVS-E-2.                                                    SM2044.2
016100     02  FILLER                      PIC X(31)  VALUE SPACE.      SM2044.2
016200     02  FILLER                      PIC X(21)  VALUE SPACE.      SM2044.2
016300     02 CCVS-E-2-2.                                               SM2044.2
016400         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      SM2044.2
016500         03 FILLER                   PIC X      VALUE SPACE.      SM2044.2
016600         03 ENDER-DESC               PIC X(44)  VALUE             SM2044.2
016700            "ERRORS ENCOUNTERED".                                 SM2044.2
016800 01  CCVS-E-3.                                                    SM2044.2
016900     02  FILLER                      PIC X(22)  VALUE             SM2044.2
017000            " FOR OFFICIAL USE ONLY".                             SM2044.2
017100     02  FILLER                      PIC X(12)  VALUE SPACE.      SM2044.2
017200     02  FILLER                      PIC X(58)  VALUE             SM2044.2
017300     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SM2044.2
017400     02  FILLER                      PIC X(13)  VALUE SPACE.      SM2044.2
017500     02 FILLER                       PIC X(15)  VALUE             SM2044.2
017600             " COPYRIGHT 1985".                                   SM2044.2
017700 01  CCVS-E-4.                                                    SM2044.2
017800     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      SM2044.2
017900     02 FILLER                       PIC X(4)   VALUE " OF ".     SM2044.2
018000     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      SM2044.2
018100     02 FILLER                       PIC X(40)  VALUE             SM2044.2
018200      "  TESTS WERE EXECUTED SUCCESSFULLY".                       SM2044.2
018300 01  XXINFO.                                                      SM2044.2
018400     02 FILLER                       PIC X(19)  VALUE             SM2044.2
018500            "*** INFORMATION ***".                                SM2044.2
018600     02 INFO-TEXT.                                                SM2044.2
018700       04 FILLER                     PIC X(8)   VALUE SPACE.      SM2044.2
018800       04 XXCOMPUTED                 PIC X(20).                   SM2044.2
018900       04 FILLER                     PIC X(5)   VALUE SPACE.      SM2044.2
019000       04 XXCORRECT                  PIC X(20).                   SM2044.2
019100     02 INF-ANSI-REFERENCE           PIC X(48).                   SM2044.2
019200 01  HYPHEN-LINE.                                                 SM2044.2
019300     02 FILLER  PIC IS X VALUE IS SPACE.                          SM2044.2
019400     02 FILLER  PIC IS X(65)    VALUE IS "************************SM2044.2
019500-    "*****************************************".                 SM2044.2
019600     02 FILLER  PIC IS X(54)    VALUE IS "************************SM2044.2
019700-    "******************************".                            SM2044.2
019800 01  CCVS-PGM-ID                     PIC X(9)   VALUE             SM2044.2
019900     "SM204A".                                                    SM2044.2
020000 PROCEDURE DIVISION.                                              SM2044.2
020100 CCVS1 SECTION.                                                   SM2044.2
020200 OPEN-FILES.                                                      SM2044.2
020300     OPEN     OUTPUT PRINT-FILE.                                  SM2044.2
020400     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   SM2044.2
020500     MOVE    SPACE TO TEST-RESULTS.                               SM2044.2
020600     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             SM2044.2
020700     GO TO CCVS1-EXIT.                                            SM2044.2
020800 CLOSE-FILES.                                                     SM2044.2
020900     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   SM2044.2
021000 TERMINATE-CCVS.                                                  SM2044.2
021100*    EXIT PROGRAM.                                                SM2044.2
021200*TERMINATE-CALL.                                                  SM2044.2
021300     STOP     RUN.                                                SM2044.2
021400 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         SM2044.2
021500 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           SM2044.2
021600 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          SM2044.2
021700 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      SM2044.2
021800     MOVE "****TEST DELETED****" TO RE-MARK.                      SM2044.2
021900 PRINT-DETAIL.                                                    SM2044.2
022000     IF REC-CT NOT EQUAL TO ZERO                                  SM2044.2
022100             MOVE "." TO PARDOT-X                                 SM2044.2
022200             MOVE REC-CT TO DOTVALUE.                             SM2044.2
022300     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      SM2044.2
022400     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               SM2044.2
022500        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 SM2044.2
022600          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 SM2044.2
022700     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              SM2044.2
022800     MOVE SPACE TO CORRECT-X.                                     SM2044.2
022900     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         SM2044.2
023000     MOVE     SPACE TO RE-MARK.                                   SM2044.2
023100 HEAD-ROUTINE.                                                    SM2044.2
023200     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  SM2044.2
023300     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  SM2044.2
023400     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  SM2044.2
023500     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  SM2044.2
023600 COLUMN-NAMES-ROUTINE.                                            SM2044.2
023700     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SM2044.2
023800     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SM2044.2
023900     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        SM2044.2
024000 END-ROUTINE.                                                     SM2044.2
024100     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.SM2044.2
024200 END-RTN-EXIT.                                                    SM2044.2
024300     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SM2044.2
024400 END-ROUTINE-1.                                                   SM2044.2
024500      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      SM2044.2
024600      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               SM2044.2
024700      ADD PASS-COUNTER TO ERROR-HOLD.                             SM2044.2
024800*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   SM2044.2
024900      MOVE PASS-COUNTER TO CCVS-E-4-1.                            SM2044.2
025000      MOVE ERROR-HOLD TO CCVS-E-4-2.                              SM2044.2
025100      MOVE CCVS-E-4 TO CCVS-E-2-2.                                SM2044.2
025200      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           SM2044.2
025300  END-ROUTINE-12.                                                 SM2044.2
025400      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        SM2044.2
025500     IF       ERROR-COUNTER IS EQUAL TO ZERO                      SM2044.2
025600         MOVE "NO " TO ERROR-TOTAL                                SM2044.2
025700         ELSE                                                     SM2044.2
025800         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       SM2044.2
025900     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           SM2044.2
026000     PERFORM WRITE-LINE.                                          SM2044.2
026100 END-ROUTINE-13.                                                  SM2044.2
026200     IF DELETE-COUNTER IS EQUAL TO ZERO                           SM2044.2
026300         MOVE "NO " TO ERROR-TOTAL  ELSE                          SM2044.2
026400         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      SM2044.2
026500     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   SM2044.2
026600     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SM2044.2
026700      IF   INSPECT-COUNTER EQUAL TO ZERO                          SM2044.2
026800          MOVE "NO " TO ERROR-TOTAL                               SM2044.2
026900      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   SM2044.2
027000      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            SM2044.2
027100      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          SM2044.2
027200     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SM2044.2
027300 WRITE-LINE.                                                      SM2044.2
027400     ADD 1 TO RECORD-COUNT.                                       SM2044.2
027500*    IF RECORD-COUNT GREATER 50                                   SM2044.2
027600*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          SM2044.2
027700*        MOVE SPACE TO DUMMY-RECORD                               SM2044.2
027800*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  SM2044.2
027900*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             SM2044.2
028000*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     SM2044.2
028100*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          SM2044.2
028200*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          SM2044.2
028300*        MOVE ZERO TO RECORD-COUNT.                               SM2044.2
028400     PERFORM WRT-LN.                                              SM2044.2
028500 WRT-LN.                                                          SM2044.2
028600     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               SM2044.2
028700     MOVE SPACE TO DUMMY-RECORD.                                  SM2044.2
028800 BLANK-LINE-PRINT.                                                SM2044.2
028900     PERFORM WRT-LN.                                              SM2044.2
029000 FAIL-ROUTINE.                                                    SM2044.2
029100     IF   COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE. SM2044.2
029200     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.SM2044.2
029300     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 SM2044.2
029400     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   SM2044.2
029500     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SM2044.2
029600     MOVE   SPACES TO INF-ANSI-REFERENCE.                         SM2044.2
029700     GO TO  FAIL-ROUTINE-EX.                                      SM2044.2
029800 FAIL-ROUTINE-WRITE.                                              SM2044.2
029900     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         SM2044.2
030000     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 SM2044.2
030100     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. SM2044.2
030200     MOVE   SPACES TO COR-ANSI-REFERENCE.                         SM2044.2
030300 FAIL-ROUTINE-EX. EXIT.                                           SM2044.2
030400 BAIL-OUT.                                                        SM2044.2
030500     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   SM2044.2
030600     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           SM2044.2
030700 BAIL-OUT-WRITE.                                                  SM2044.2
030800     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  SM2044.2
030900     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 SM2044.2
031000     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SM2044.2
031100     MOVE   SPACES TO INF-ANSI-REFERENCE.                         SM2044.2
031200 BAIL-OUT-EX. EXIT.                                               SM2044.2
031300 CCVS1-EXIT.                                                      SM2044.2
031400     EXIT.                                                        SM2044.2
031500 INITIALIZATION SECTION.                                          SM2044.2
031600 SM204-INIT.                                                      SM2044.2
031700     OPEN     INPUT TEST-FILE.                                    SM2044.2
031800     MOVE     "ALL TESTS IN SM204A CHECK" TO RE-MARK.             SM2044.2
031900     PERFORM  PRINT-DETAIL.                                       SM2044.2
032000     MOVE     "OUTPUT OF SM203A."         TO RE-MARK.             SM2044.2
032100     PERFORM  PRINT-DETAIL.                                       SM2044.2
032200     MOVE     "COPY ENV DIV REPLAC" TO FEATURE.                   SM2044.2
032300 COPY-TEST-2.                                                     SM2044.2
032400     PERFORM  READ-TEST-FILE.                                     SM2044.2
032500     IF       TF-1 EQUAL TO 97532                                 SM2044.2
032600              PERFORM PASS GO TO COPY-WRITE-2.                    SM2044.2
032700     GO       TO COPY-FAIL-2.                                     SM2044.2
032800 COPY-DELETE-2.                                                   SM2044.2
032900     PERFORM  DE-LETE.                                            SM2044.2
033000     GO       TO COPY-WRITE-2.                                    SM2044.2
033100 COPY-FAIL-2.                                                     SM2044.2
033200     MOVE     TF-1 TO COMPUTED-N.                                 SM2044.2
033300     MOVE     97532 TO CORRECT-N.                                 SM2044.2
033400     PERFORM  FAIL.                                               SM2044.2
033500 COPY-WRITE-2.                                                    SM2044.2
033600     MOVE     "COPY-TEST-2 " TO PAR-NAME.                         SM2044.2
033700     PERFORM  PRINT-DETAIL.                                       SM2044.2
033800 COPY-TEST-3.                                                     SM2044.2
033900     PERFORM  READ-TEST-FILE.                                     SM2044.2
034000     IF       TF-1 EQUAL TO 23479                                 SM2044.2
034100              PERFORM PASS GO TO COPY-WRITE-3.                    SM2044.2
034200     GO       TO COPY-FAIL-3.                                     SM2044.2
034300 COPY-DELETE-3.                                                   SM2044.2
034400     PERFORM  DE-LETE.                                            SM2044.2
034500     GO       TO COPY-WRITE-3.                                    SM2044.2
034600 COPY-FAIL-3.                                                     SM2044.2
034700     MOVE     TF-1 TO COMPUTED-N.                                 SM2044.2
034800     MOVE     23479 TO CORRECT-N.                                 SM2044.2
034900     PERFORM  FAIL.                                               SM2044.2
035000 COPY-WRITE-3.                                                    SM2044.2
035100     MOVE     "COPY-TEST-3 " TO PAR-NAME.                         SM2044.2
035200     PERFORM  PRINT-DETAIL.                                       SM2044.2
035300 COPY-TEST-4.                                                     SM2044.2
035400     PERFORM  READ-TEST-FILE 3 TIMES.                             SM2044.2
035500     IF       TF-1 EQUAL TO 14003                                 SM2044.2
035600              PERFORM PASS GO TO COPY-WRITE-4.                    SM2044.2
035700     GO       TO COPY-FAIL-4.                                     SM2044.2
035800 COPY-DELETE-4.                                                   SM2044.2
035900     PERFORM  DE-LETE.                                            SM2044.2
036000     GO       TO COPY-WRITE-4.                                    SM2044.2
036100 COPY-FAIL-4.                                                     SM2044.2
036200     MOVE     TF-1 TO COMPUTED-N.                                 SM2044.2
036300     MOVE     14003 TO CORRECT-N.                                 SM2044.2
036400     PERFORM  FAIL.                                               SM2044.2
036500 COPY-WRITE-4.                                                    SM2044.2
036600     MOVE     "COPY-TEST-4 " TO PAR-NAME.                         SM2044.2
036700     PERFORM  PRINT-DETAIL.                                       SM2044.2
036800 COPY-TEST-5.                                                     SM2044.2
036900     PERFORM  READ-TEST-FILE 2 TIMES.                             SM2044.2
037000     IF       TF-1 EQUAL TO 03543                                 SM2044.2
037100              PERFORM PASS GO TO COPY-WRITE-5.                    SM2044.2
037200     GO       TO COPY-FAIL-5.                                     SM2044.2
037300 COPY-DELETE-5.                                                   SM2044.2
037400     PERFORM  DE-LETE.                                            SM2044.2
037500     GO       TO COPY-WRITE-5.                                    SM2044.2
037600 COPY-FAIL-5.                                                     SM2044.2
037700     MOVE     TF-1 TO COMPUTED-N.                                 SM2044.2
037800     MOVE     03543 TO CORRECT-N.                                 SM2044.2
037900     PERFORM  FAIL.                                               SM2044.2
038000 COPY-WRITE-5.                                                    SM2044.2
038100     MOVE     "COPY SPECIAL-NAMES" TO FEATURE.                    SM2044.2
038200     MOVE     "COPY-TEST-5 " TO PAR-NAME.                         SM2044.2
038300     PERFORM  PRINT-DETAIL.                                       SM2044.2
038400     CLOSE    TEST-FILE.                                          SM2044.2
038500     GO TO CCVS-EXIT.                                             SM2044.2
038600 READ-TEST-FILE.                                                  SM2044.2
038700     READ     TEST-FILE          AT END GO TO BAD-FILE.           SM2044.2
038800 BAD-FILE.                                                        SM2044.2
038900     PERFORM  FAIL.                                               SM2044.2
039000     MOVE     "BAD-FILE" TO PAR-NAME.                             SM2044.2
039100     MOVE     "EOF FOUND PREMATURELY" TO RE-MARK.                 SM2044.2
039200     PERFORM  PRINT-DETAIL.                                       SM2044.2
039300     CLOSE    TEST-FILE.                                          SM2044.2
039400     GO TO CCVS-EXIT.                                             SM2044.2
039500 CCVS-EXIT SECTION.                                               SM2044.2
039600 CCVS-999999.                                                     SM2044.2
039700     GO TO CLOSE-FILES.                                           SM2044.2
      *END-OF,SM204A                                                                  
