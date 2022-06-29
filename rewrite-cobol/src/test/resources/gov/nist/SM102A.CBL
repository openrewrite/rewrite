      *HEADER,COBOL,SM101A,SUBPRG,SM102A                                              
000100 IDENTIFICATION DIVISION.                                         SM1024.2
000200 PROGRAM-ID.                                                      SM1024.2
000300     SM102A.                                                      SM1024.2
000400****************************************************************  SM1024.2
000500*                                                              *  SM1024.2
000600*    VALIDATION FOR:-                                          *  SM1024.2
000700*                                                              *  SM1024.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SM1024.2
000900*                                                              *  SM1024.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SM1024.2
001100*                                                              *  SM1024.2
001200****************************************************************  SM1024.2
001300*                                                              *  SM1024.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  SM1024.2
001500*                                                              *  SM1024.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  SM1024.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  SM1024.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  SM1024.2
001900*                                                              *  SM1024.2
002000****************************************************************  SM1024.2
002100*                                                              *  SM1024.2
002200*    PROGRAM SM102A TESTS THE OUTPUT FILE PRODUCED BY SM101A   *  SM1024.2
002300*    TO ENSURE THE PROPER EXECUTION OF THE "COPY" STATEMENT    *  SM1024.2
002400*    IN THAT PROGRAM.                                          *  SM1024.2
002500*                                                              *  SM1024.2
002600****************************************************************  SM1024.2
002700 ENVIRONMENT DIVISION.                                            SM1024.2
002800 CONFIGURATION SECTION.                                           SM1024.2
002900 SOURCE-COMPUTER.                                                 SM1024.2
003000     XXXXX082.                                                    SM1024.2
003100 OBJECT-COMPUTER.                                                 SM1024.2
003200     XXXXX083.                                                    SM1024.2
003300 INPUT-OUTPUT SECTION.                                            SM1024.2
003400 FILE-CONTROL.                                                    SM1024.2
003500     SELECT PRINT-FILE ASSIGN TO                                  SM1024.2
003600     XXXXX055.                                                    SM1024.2
003700     SELECT TEST-FILE ASSIGN TO                                   SM1024.2
003800     XXXXD001.                                                    SM1024.2
003900 DATA DIVISION.                                                   SM1024.2
004000 FILE SECTION.                                                    SM1024.2
004100 FD  PRINT-FILE.                                                  SM1024.2
004200 01  PRINT-REC PICTURE X(120).                                    SM1024.2
004300 01  DUMMY-RECORD PICTURE X(120).                                 SM1024.2
004400 FD  TEST-FILE                                                    SM1024.2
004500     LABEL RECORD STANDARD                                        SM1024.2
004600C    VALUE OF                                                     SM1024.2
004700C    XXXXX074                                                     SM1024.2
004800C    IS                                                           SM1024.2
004900C    XXXXX075                                                     SM1024.2
005000*    XXXXX069                                                     SM1024.2
005100     DATA RECORD IS TST-TEST.                                     SM1024.2
005200 01  TST-TEST.                                                    SM1024.2
005300     02  TST-FLD-1 PICTURE 9(5).                                  SM1024.2
005400     02  FILLER PICTURE X(115).                                   SM1024.2
005500 WORKING-STORAGE SECTION.                                         SM1024.2
005600 01  TEST-RESULTS.                                                SM1024.2
005700     02 FILLER                   PIC X      VALUE SPACE.          SM1024.2
005800     02 FEATURE                  PIC X(20)  VALUE SPACE.          SM1024.2
005900     02 FILLER                   PIC X      VALUE SPACE.          SM1024.2
006000     02 P-OR-F                   PIC X(5)   VALUE SPACE.          SM1024.2
006100     02 FILLER                   PIC X      VALUE SPACE.          SM1024.2
006200     02  PAR-NAME.                                                SM1024.2
006300       03 FILLER                 PIC X(19)  VALUE SPACE.          SM1024.2
006400       03  PARDOT-X              PIC X      VALUE SPACE.          SM1024.2
006500       03 DOTVALUE               PIC 99     VALUE ZERO.           SM1024.2
006600     02 FILLER                   PIC X(8)   VALUE SPACE.          SM1024.2
006700     02 RE-MARK                  PIC X(61).                       SM1024.2
006800 01  TEST-COMPUTED.                                               SM1024.2
006900     02 FILLER                   PIC X(30)  VALUE SPACE.          SM1024.2
007000     02 FILLER                   PIC X(17)  VALUE                 SM1024.2
007100            "       COMPUTED=".                                   SM1024.2
007200     02 COMPUTED-X.                                               SM1024.2
007300     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          SM1024.2
007400     03 COMPUTED-N               REDEFINES COMPUTED-A             SM1024.2
007500                                 PIC -9(9).9(9).                  SM1024.2
007600     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         SM1024.2
007700     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     SM1024.2
007800     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     SM1024.2
007900     03       CM-18V0 REDEFINES COMPUTED-A.                       SM1024.2
008000         04 COMPUTED-18V0                    PIC -9(18).          SM1024.2
008100         04 FILLER                           PIC X.               SM1024.2
008200     03 FILLER PIC X(50) VALUE SPACE.                             SM1024.2
008300 01  TEST-CORRECT.                                                SM1024.2
008400     02 FILLER PIC X(30) VALUE SPACE.                             SM1024.2
008500     02 FILLER PIC X(17) VALUE "       CORRECT =".                SM1024.2
008600     02 CORRECT-X.                                                SM1024.2
008700     03 CORRECT-A                  PIC X(20) VALUE SPACE.         SM1024.2
008800     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      SM1024.2
008900     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         SM1024.2
009000     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     SM1024.2
009100     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     SM1024.2
009200     03      CR-18V0 REDEFINES CORRECT-A.                         SM1024.2
009300         04 CORRECT-18V0                     PIC -9(18).          SM1024.2
009400         04 FILLER                           PIC X.               SM1024.2
009500     03 FILLER PIC X(2) VALUE SPACE.                              SM1024.2
009600     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     SM1024.2
009700 01  CCVS-C-1.                                                    SM1024.2
009800     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PASM1024.2
009900-    "SS  PARAGRAPH-NAME                                          SM1024.2
010000-    "       REMARKS".                                            SM1024.2
010100     02 FILLER                     PIC X(20)    VALUE SPACE.      SM1024.2
010200 01  CCVS-C-2.                                                    SM1024.2
010300     02 FILLER                     PIC X        VALUE SPACE.      SM1024.2
010400     02 FILLER                     PIC X(6)     VALUE "TESTED".   SM1024.2
010500     02 FILLER                     PIC X(15)    VALUE SPACE.      SM1024.2
010600     02 FILLER                     PIC X(4)     VALUE "FAIL".     SM1024.2
010700     02 FILLER                     PIC X(94)    VALUE SPACE.      SM1024.2
010800 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       SM1024.2
010900 01  REC-CT                        PIC 99       VALUE ZERO.       SM1024.2
011000 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       SM1024.2
011100 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       SM1024.2
011200 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       SM1024.2
011300 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       SM1024.2
011400 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       SM1024.2
011500 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       SM1024.2
011600 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      SM1024.2
011700 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       SM1024.2
011800 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     SM1024.2
011900 01  CCVS-H-1.                                                    SM1024.2
012000     02  FILLER                    PIC X(39)    VALUE SPACES.     SM1024.2
012100     02  FILLER                    PIC X(42)    VALUE             SM1024.2
012200     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 SM1024.2
012300     02  FILLER                    PIC X(39)    VALUE SPACES.     SM1024.2
012400 01  CCVS-H-2A.                                                   SM1024.2
012500   02  FILLER                        PIC X(40)  VALUE SPACE.      SM1024.2
012600   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  SM1024.2
012700   02  FILLER                        PIC XXXX   VALUE             SM1024.2
012800     "4.2 ".                                                      SM1024.2
012900   02  FILLER                        PIC X(28)  VALUE             SM1024.2
013000            " COPY - NOT FOR DISTRIBUTION".                       SM1024.2
013100   02  FILLER                        PIC X(41)  VALUE SPACE.      SM1024.2
013200                                                                  SM1024.2
013300 01  CCVS-H-2B.                                                   SM1024.2
013400   02  FILLER                        PIC X(15)  VALUE             SM1024.2
013500            "TEST RESULT OF ".                                    SM1024.2
013600   02  TEST-ID                       PIC X(9).                    SM1024.2
013700   02  FILLER                        PIC X(4)   VALUE             SM1024.2
013800            " IN ".                                               SM1024.2
013900   02  FILLER                        PIC X(12)  VALUE             SM1024.2
014000     " HIGH       ".                                              SM1024.2
014100   02  FILLER                        PIC X(22)  VALUE             SM1024.2
014200            " LEVEL VALIDATION FOR ".                             SM1024.2
014300   02  FILLER                        PIC X(58)  VALUE             SM1024.2
014400     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SM1024.2
014500 01  CCVS-H-3.                                                    SM1024.2
014600     02  FILLER                      PIC X(34)  VALUE             SM1024.2
014700            " FOR OFFICIAL USE ONLY    ".                         SM1024.2
014800     02  FILLER                      PIC X(58)  VALUE             SM1024.2
014900     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SM1024.2
015000     02  FILLER                      PIC X(28)  VALUE             SM1024.2
015100            "  COPYRIGHT   1985 ".                                SM1024.2
015200 01  CCVS-E-1.                                                    SM1024.2
015300     02 FILLER                       PIC X(52)  VALUE SPACE.      SM1024.2
015400     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              SM1024.2
015500     02 ID-AGAIN                     PIC X(9).                    SM1024.2
015600     02 FILLER                       PIC X(45)  VALUE SPACES.     SM1024.2
015700 01  CCVS-E-2.                                                    SM1024.2
015800     02  FILLER                      PIC X(31)  VALUE SPACE.      SM1024.2
015900     02  FILLER                      PIC X(21)  VALUE SPACE.      SM1024.2
016000     02 CCVS-E-2-2.                                               SM1024.2
016100         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      SM1024.2
016200         03 FILLER                   PIC X      VALUE SPACE.      SM1024.2
016300         03 ENDER-DESC               PIC X(44)  VALUE             SM1024.2
016400            "ERRORS ENCOUNTERED".                                 SM1024.2
016500 01  CCVS-E-3.                                                    SM1024.2
016600     02  FILLER                      PIC X(22)  VALUE             SM1024.2
016700            " FOR OFFICIAL USE ONLY".                             SM1024.2
016800     02  FILLER                      PIC X(12)  VALUE SPACE.      SM1024.2
016900     02  FILLER                      PIC X(58)  VALUE             SM1024.2
017000     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SM1024.2
017100     02  FILLER                      PIC X(13)  VALUE SPACE.      SM1024.2
017200     02 FILLER                       PIC X(15)  VALUE             SM1024.2
017300             " COPYRIGHT 1985".                                   SM1024.2
017400 01  CCVS-E-4.                                                    SM1024.2
017500     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      SM1024.2
017600     02 FILLER                       PIC X(4)   VALUE " OF ".     SM1024.2
017700     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      SM1024.2
017800     02 FILLER                       PIC X(40)  VALUE             SM1024.2
017900      "  TESTS WERE EXECUTED SUCCESSFULLY".                       SM1024.2
018000 01  XXINFO.                                                      SM1024.2
018100     02 FILLER                       PIC X(19)  VALUE             SM1024.2
018200            "*** INFORMATION ***".                                SM1024.2
018300     02 INFO-TEXT.                                                SM1024.2
018400       04 FILLER                     PIC X(8)   VALUE SPACE.      SM1024.2
018500       04 XXCOMPUTED                 PIC X(20).                   SM1024.2
018600       04 FILLER                     PIC X(5)   VALUE SPACE.      SM1024.2
018700       04 XXCORRECT                  PIC X(20).                   SM1024.2
018800     02 INF-ANSI-REFERENCE           PIC X(48).                   SM1024.2
018900 01  HYPHEN-LINE.                                                 SM1024.2
019000     02 FILLER  PIC IS X VALUE IS SPACE.                          SM1024.2
019100     02 FILLER  PIC IS X(65)    VALUE IS "************************SM1024.2
019200-    "*****************************************".                 SM1024.2
019300     02 FILLER  PIC IS X(54)    VALUE IS "************************SM1024.2
019400-    "******************************".                            SM1024.2
019500 01  CCVS-PGM-ID                     PIC X(9)   VALUE             SM1024.2
019600     "SM102A".                                                    SM1024.2
019700 PROCEDURE DIVISION.                                              SM1024.2
019800 CCVS1 SECTION.                                                   SM1024.2
019900 OPEN-FILES.                                                      SM1024.2
020000     OPEN     OUTPUT PRINT-FILE.                                  SM1024.2
020100     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   SM1024.2
020200     MOVE    SPACE TO TEST-RESULTS.                               SM1024.2
020300     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             SM1024.2
020400     GO TO CCVS1-EXIT.                                            SM1024.2
020500 CLOSE-FILES.                                                     SM1024.2
020600     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   SM1024.2
020700 TERMINATE-CCVS.                                                  SM1024.2
020800*    EXIT PROGRAM.                                                SM1024.2
020900*TERMINATE-CALL.                                                  SM1024.2
021000     STOP     RUN.                                                SM1024.2
021100 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         SM1024.2
021200 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           SM1024.2
021300 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          SM1024.2
021400 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      SM1024.2
021500     MOVE "****TEST DELETED****" TO RE-MARK.                      SM1024.2
021600 PRINT-DETAIL.                                                    SM1024.2
021700     IF REC-CT NOT EQUAL TO ZERO                                  SM1024.2
021800             MOVE "." TO PARDOT-X                                 SM1024.2
021900             MOVE REC-CT TO DOTVALUE.                             SM1024.2
022000     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      SM1024.2
022100     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               SM1024.2
022200        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 SM1024.2
022300          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 SM1024.2
022400     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              SM1024.2
022500     MOVE SPACE TO CORRECT-X.                                     SM1024.2
022600     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         SM1024.2
022700     MOVE     SPACE TO RE-MARK.                                   SM1024.2
022800 HEAD-ROUTINE.                                                    SM1024.2
022900     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  SM1024.2
023000     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  SM1024.2
023100     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  SM1024.2
023200     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  SM1024.2
023300 COLUMN-NAMES-ROUTINE.                                            SM1024.2
023400     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SM1024.2
023500     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SM1024.2
023600     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        SM1024.2
023700 END-ROUTINE.                                                     SM1024.2
023800     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.SM1024.2
023900 END-RTN-EXIT.                                                    SM1024.2
024000     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SM1024.2
024100 END-ROUTINE-1.                                                   SM1024.2
024200      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      SM1024.2
024300      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               SM1024.2
024400      ADD PASS-COUNTER TO ERROR-HOLD.                             SM1024.2
024500*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   SM1024.2
024600      MOVE PASS-COUNTER TO CCVS-E-4-1.                            SM1024.2
024700      MOVE ERROR-HOLD TO CCVS-E-4-2.                              SM1024.2
024800      MOVE CCVS-E-4 TO CCVS-E-2-2.                                SM1024.2
024900      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           SM1024.2
025000  END-ROUTINE-12.                                                 SM1024.2
025100      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        SM1024.2
025200     IF       ERROR-COUNTER IS EQUAL TO ZERO                      SM1024.2
025300         MOVE "NO " TO ERROR-TOTAL                                SM1024.2
025400         ELSE                                                     SM1024.2
025500         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       SM1024.2
025600     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           SM1024.2
025700     PERFORM WRITE-LINE.                                          SM1024.2
025800 END-ROUTINE-13.                                                  SM1024.2
025900     IF DELETE-COUNTER IS EQUAL TO ZERO                           SM1024.2
026000         MOVE "NO " TO ERROR-TOTAL  ELSE                          SM1024.2
026100         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      SM1024.2
026200     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   SM1024.2
026300     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SM1024.2
026400      IF   INSPECT-COUNTER EQUAL TO ZERO                          SM1024.2
026500          MOVE "NO " TO ERROR-TOTAL                               SM1024.2
026600      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   SM1024.2
026700      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            SM1024.2
026800      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          SM1024.2
026900     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SM1024.2
027000 WRITE-LINE.                                                      SM1024.2
027100     ADD 1 TO RECORD-COUNT.                                       SM1024.2
027200*    IF RECORD-COUNT GREATER 50                                   SM1024.2
027300*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          SM1024.2
027400*        MOVE SPACE TO DUMMY-RECORD                               SM1024.2
027500*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  SM1024.2
027600*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             SM1024.2
027700*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     SM1024.2
027800*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          SM1024.2
027900*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          SM1024.2
028000*        MOVE ZERO TO RECORD-COUNT.                               SM1024.2
028100     PERFORM WRT-LN.                                              SM1024.2
028200 WRT-LN.                                                          SM1024.2
028300     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               SM1024.2
028400     MOVE SPACE TO DUMMY-RECORD.                                  SM1024.2
028500 BLANK-LINE-PRINT.                                                SM1024.2
028600     PERFORM WRT-LN.                                              SM1024.2
028700 FAIL-ROUTINE.                                                    SM1024.2
028800     IF   COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE. SM1024.2
028900     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.SM1024.2
029000     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 SM1024.2
029100     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   SM1024.2
029200     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SM1024.2
029300     MOVE   SPACES TO INF-ANSI-REFERENCE.                         SM1024.2
029400     GO TO  FAIL-ROUTINE-EX.                                      SM1024.2
029500 FAIL-ROUTINE-WRITE.                                              SM1024.2
029600     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         SM1024.2
029700     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 SM1024.2
029800     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. SM1024.2
029900     MOVE   SPACES TO COR-ANSI-REFERENCE.                         SM1024.2
030000 FAIL-ROUTINE-EX. EXIT.                                           SM1024.2
030100 BAIL-OUT.                                                        SM1024.2
030200     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   SM1024.2
030300     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           SM1024.2
030400 BAIL-OUT-WRITE.                                                  SM1024.2
030500     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  SM1024.2
030600     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 SM1024.2
030700     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SM1024.2
030800     MOVE   SPACES TO INF-ANSI-REFERENCE.                         SM1024.2
030900 BAIL-OUT-EX. EXIT.                                               SM1024.2
031000 CCVS1-EXIT.                                                      SM1024.2
031100     EXIT.                                                        SM1024.2
031200 INITIALIZATION SECTION.                                          SM1024.2
031300 SM102-INIT.                                                      SM1024.2
031400     OPEN     INPUT  TEST-FILE.                                   SM1024.2
031500     MOVE     "SM102A CHECKS A FILE WHICH" TO RE-MARK.            SM1024.2
031600     PERFORM  PRINT-DETAIL.                                       SM1024.2
031700     MOVE     "WAS GENERATED IN SM101A."  TO RE-MARK.             SM1024.2
031800     PERFORM  PRINT-DETAIL.                                       SM1024.2
031900     MOVE     "COPY FILE DESCR" TO FEATURE.                       SM1024.2
032000 FD-TEST SECTION.                                                 SM1024.2
032100 COPY-TEST-8.                                                     SM1024.2
032200     PERFORM READ-TSTFILE.                                        SM1024.2
032300     IF      TST-FLD-1 EQUAL TO 97523                             SM1024.2
032400             PERFORM PASS GO TO COPY-WRITE-8.                     SM1024.2
032500     GO      TO COPY-FAIL-8.                                      SM1024.2
032600 COPY-DELETE-8.                                                   SM1024.2
032700     PERFORM DE-LETE.                                             SM1024.2
032800     GO      TO COPY-WRITE-8.                                     SM1024.2
032900 COPY-FAIL-8.                                                     SM1024.2
033000     MOVE    TST-FLD-1 TO COMPUTED-N.                             SM1024.2
033100     MOVE    97523   TO CORRECT-N.                                SM1024.2
033200     PERFORM FAIL.                                                SM1024.2
033300 COPY-WRITE-8.                                                    SM1024.2
033400     MOVE   "COPY-TEST-8 " TO PAR-NAME.                           SM1024.2
033500     PERFORM PRINT-DETAIL.                                        SM1024.2
033600 COPY-TEST-9.                                                     SM1024.2
033700     PERFORM READ-TSTFILE.                                        SM1024.2
033800     IF      TST-FLD-1 EQUAL TO 23497                             SM1024.2
033900              PERFORM PASS GO TO COPY-WRITE-9.                    SM1024.2
034000     GO       TO COPY-FAIL-9.                                     SM1024.2
034100 COPY-DELETE-9.                                                   SM1024.2
034200     PERFORM  DE-LETE.                                            SM1024.2
034300     GO       TO COPY-WRITE-9.                                    SM1024.2
034400 COPY-FAIL-9.                                                     SM1024.2
034500     MOVE     TST-FLD-1 TO COMPUTED-N.                            SM1024.2
034600     MOVE     23497 TO CORRECT-N.                                 SM1024.2
034700     PERFORM  FAIL.                                               SM1024.2
034800 COPY-WRITE-9.                                                    SM1024.2
034900     MOVE     "COPY-TEST-9 " TO PAR-NAME.                         SM1024.2
035000     PERFORM  PRINT-DETAIL.                                       SM1024.2
035100 COPY-TEST-10.                                                    SM1024.2
035200     PERFORM  READ-TSTFILE 3 TIMES.                               SM1024.2
035300     IF       TST-FLD-1 EQUAL TO 14003                            SM1024.2
035400              PERFORM PASS GO TO COPY-WRITE-10.                   SM1024.2
035500     GO       TO COPY-FAIL-10.                                    SM1024.2
035600 COPY-DELETE-10.                                                  SM1024.2
035700     PERFORM  DE-LETE.                                            SM1024.2
035800     GO       TO COPY-WRITE-10.                                   SM1024.2
035900 COPY-FAIL-10.                                                    SM1024.2
036000     MOVE     TST-FLD-1 TO COMPUTED-N.                            SM1024.2
036100     MOVE     14003 TO CORRECT-N.                                 SM1024.2
036200     PERFORM  FAIL.                                               SM1024.2
036300 COPY-WRITE-10.                                                   SM1024.2
036400     MOVE     "COPY-TEST-10 " TO PAR-NAME.                        SM1024.2
036500     PERFORM  PRINT-DETAIL.                                       SM1024.2
036600 COPY-TEST-11.                                                    SM1024.2
036700     PERFORM  READ-TSTFILE 2 TIMES.                               SM1024.2
036800     IF       TST-FLD-1 EQUAL TO 03543                            SM1024.2
036900              PERFORM PASS GO TO COPY-WRITE-11.                   SM1024.2
037000     GO       TO COPY-FAIL-11.                                    SM1024.2
037100 COPY-DELETE-11.                                                  SM1024.2
037200     PERFORM  DE-LETE.                                            SM1024.2
037300     GO       TO COPY-WRITE-11.                                   SM1024.2
037400 COPY-FAIL-11.                                                    SM1024.2
037500     MOVE     TST-FLD-1 TO COMPUTED-N.                            SM1024.2
037600     MOVE     03543 TO CORRECT-N.                                 SM1024.2
037700     PERFORM  FAIL.                                               SM1024.2
037800 COPY-WRITE-11.                                                   SM1024.2
037900     MOVE     "COPY-TEST-11 " TO PAR-NAME.                        SM1024.2
038000     PERFORM  PRINT-DETAIL.                                       SM1024.2
038100     CLOSE    TEST-FILE.                                          SM1024.2
038200     GO TO CCVS-EXIT.                                             SM1024.2
038300 READ-TSTFILE.                                                    SM1024.2
038400     READ     TEST-FILE          AT END GO TO BAD-FILE.           SM1024.2
038500 BAD-FILE.                                                        SM1024.2
038600     PERFORM  FAIL.                                               SM1024.2
038700     MOVE     "BAD-FILE" TO PAR-NAME.                             SM1024.2
038800     MOVE     "EOF PREMATURELY FOUND" TO RE-MARK.                 SM1024.2
038900     PERFORM  PRINT-DETAIL.                                       SM1024.2
039000     CLOSE    TEST-FILE.                                          SM1024.2
039100 CCVS-EXIT SECTION.                                               SM1024.2
039200 CCVS-999999.                                                     SM1024.2
039300     GO TO CLOSE-FILES.                                           SM1024.2
      *END-OF,SM102A                                                                  
