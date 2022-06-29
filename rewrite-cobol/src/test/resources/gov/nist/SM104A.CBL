      *HEADER,COBOL,SM103A,SUBPRG,SM104A                                              
000100 IDENTIFICATION DIVISION.                                         SM1044.2
000200 PROGRAM-ID.                                                      SM1044.2
000300     SM104A.                                                      SM1044.2
000400****************************************************************  SM1044.2
000500*                                                              *  SM1044.2
000600*    VALIDATION FOR:-                                          *  SM1044.2
000700*                                                              *  SM1044.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SM1044.2
000900*                                                              *  SM1044.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SM1044.2
001100*                                                              *  SM1044.2
001200****************************************************************  SM1044.2
001300*                                                              *  SM1044.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  SM1044.2
001500*                                                              *  SM1044.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  SM1044.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  SM1044.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  SM1044.2
001900*                                                              *  SM1044.2
002000****************************************************************  SM1044.2
002100*                                                              *  SM1044.2
002200*    PROGRAM SM104A READS AND CHECKS THE FILE PRODUCED BY      *  SM1044.2
002300*    SM103A TO VERIFY THE PROPER EXECUTION OF THE "COPY"       *  SM1044.2
002400*    STATEMENTS IN THAT PROGRAM.                               *  SM1044.2
002500*                                                              *  SM1044.2
002600****************************************************************  SM1044.2
002700 ENVIRONMENT DIVISION.                                            SM1044.2
002800 CONFIGURATION SECTION.                                           SM1044.2
002900 SOURCE-COMPUTER.                                                 SM1044.2
003000     XXXXX082.                                                    SM1044.2
003100 OBJECT-COMPUTER.                                                 SM1044.2
003200     XXXXX083.                                                    SM1044.2
003300 INPUT-OUTPUT SECTION.                                            SM1044.2
003400 FILE-CONTROL.                                                    SM1044.2
003500     SELECT PRINT-FILE ASSIGN TO                                  SM1044.2
003600     XXXXX055.                                                    SM1044.2
003700     SELECT TEST-FILE ASSIGN TO                                   SM1044.2
003800* THE FOLLOWING LINE WILL BE CHANGED BY TPF ONLY WHEN THE         SM1044.2
003900* PROGRAM-ID IS PART OF THE REPLACEMENT BY THE X-CARD             SM1044.2
004000* DURING EXTRACTION.                                              SM1044.2
004100     XXXXD001.                                                    SM1044.2
004200 DATA DIVISION.                                                   SM1044.2
004300 FILE SECTION.                                                    SM1044.2
004400 FD  PRINT-FILE.                                                  SM1044.2
004500 01  PRINT-REC PICTURE X(120).                                    SM1044.2
004600 01  DUMMY-RECORD PICTURE X(120).                                 SM1044.2
004700 FD  TEST-FILE                                                    SM1044.2
004800     LABEL RECORD STANDARD                                        SM1044.2
004900C    VALUE OF                                                     SM1044.2
005000C    XXXXX074                                                     SM1044.2
005100C    IS                                                           SM1044.2
005200C    XXXXX075                                                     SM1044.2
005300*    XXXXX069                                                     SM1044.2
005400     DATA RECORD TEST-REC.                                        SM1044.2
005500 01  TEST-REC.                                                    SM1044.2
005600     02  TST-FLD-1 PICTURE 9(5).                                  SM1044.2
005700     02  TST-FLD-2 PICTURE X(13).                                 SM1044.2
005800     02  FILLER PICTURE X(102).                                   SM1044.2
005900 WORKING-STORAGE SECTION.                                         SM1044.2
006000 01  TEST-RESULTS.                                                SM1044.2
006100     02 FILLER                   PIC X      VALUE SPACE.          SM1044.2
006200     02 FEATURE                  PIC X(20)  VALUE SPACE.          SM1044.2
006300     02 FILLER                   PIC X      VALUE SPACE.          SM1044.2
006400     02 P-OR-F                   PIC X(5)   VALUE SPACE.          SM1044.2
006500     02 FILLER                   PIC X      VALUE SPACE.          SM1044.2
006600     02  PAR-NAME.                                                SM1044.2
006700       03 FILLER                 PIC X(19)  VALUE SPACE.          SM1044.2
006800       03  PARDOT-X              PIC X      VALUE SPACE.          SM1044.2
006900       03 DOTVALUE               PIC 99     VALUE ZERO.           SM1044.2
007000     02 FILLER                   PIC X(8)   VALUE SPACE.          SM1044.2
007100     02 RE-MARK                  PIC X(61).                       SM1044.2
007200 01  TEST-COMPUTED.                                               SM1044.2
007300     02 FILLER                   PIC X(30)  VALUE SPACE.          SM1044.2
007400     02 FILLER                   PIC X(17)  VALUE                 SM1044.2
007500            "       COMPUTED=".                                   SM1044.2
007600     02 COMPUTED-X.                                               SM1044.2
007700     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          SM1044.2
007800     03 COMPUTED-N               REDEFINES COMPUTED-A             SM1044.2
007900                                 PIC -9(9).9(9).                  SM1044.2
008000     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         SM1044.2
008100     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     SM1044.2
008200     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     SM1044.2
008300     03       CM-18V0 REDEFINES COMPUTED-A.                       SM1044.2
008400         04 COMPUTED-18V0                    PIC -9(18).          SM1044.2
008500         04 FILLER                           PIC X.               SM1044.2
008600     03 FILLER PIC X(50) VALUE SPACE.                             SM1044.2
008700 01  TEST-CORRECT.                                                SM1044.2
008800     02 FILLER PIC X(30) VALUE SPACE.                             SM1044.2
008900     02 FILLER PIC X(17) VALUE "       CORRECT =".                SM1044.2
009000     02 CORRECT-X.                                                SM1044.2
009100     03 CORRECT-A                  PIC X(20) VALUE SPACE.         SM1044.2
009200     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      SM1044.2
009300     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         SM1044.2
009400     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     SM1044.2
009500     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     SM1044.2
009600     03      CR-18V0 REDEFINES CORRECT-A.                         SM1044.2
009700         04 CORRECT-18V0                     PIC -9(18).          SM1044.2
009800         04 FILLER                           PIC X.               SM1044.2
009900     03 FILLER PIC X(2) VALUE SPACE.                              SM1044.2
010000     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     SM1044.2
010100 01  CCVS-C-1.                                                    SM1044.2
010200     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PASM1044.2
010300-    "SS  PARAGRAPH-NAME                                          SM1044.2
010400-    "       REMARKS".                                            SM1044.2
010500     02 FILLER                     PIC X(20)    VALUE SPACE.      SM1044.2
010600 01  CCVS-C-2.                                                    SM1044.2
010700     02 FILLER                     PIC X        VALUE SPACE.      SM1044.2
010800     02 FILLER                     PIC X(6)     VALUE "TESTED".   SM1044.2
010900     02 FILLER                     PIC X(15)    VALUE SPACE.      SM1044.2
011000     02 FILLER                     PIC X(4)     VALUE "FAIL".     SM1044.2
011100     02 FILLER                     PIC X(94)    VALUE SPACE.      SM1044.2
011200 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       SM1044.2
011300 01  REC-CT                        PIC 99       VALUE ZERO.       SM1044.2
011400 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       SM1044.2
011500 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       SM1044.2
011600 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       SM1044.2
011700 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       SM1044.2
011800 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       SM1044.2
011900 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       SM1044.2
012000 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      SM1044.2
012100 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       SM1044.2
012200 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     SM1044.2
012300 01  CCVS-H-1.                                                    SM1044.2
012400     02  FILLER                    PIC X(39)    VALUE SPACES.     SM1044.2
012500     02  FILLER                    PIC X(42)    VALUE             SM1044.2
012600     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 SM1044.2
012700     02  FILLER                    PIC X(39)    VALUE SPACES.     SM1044.2
012800 01  CCVS-H-2A.                                                   SM1044.2
012900   02  FILLER                        PIC X(40)  VALUE SPACE.      SM1044.2
013000   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  SM1044.2
013100   02  FILLER                        PIC XXXX   VALUE             SM1044.2
013200     "4.2 ".                                                      SM1044.2
013300   02  FILLER                        PIC X(28)  VALUE             SM1044.2
013400            " COPY - NOT FOR DISTRIBUTION".                       SM1044.2
013500   02  FILLER                        PIC X(41)  VALUE SPACE.      SM1044.2
013600                                                                  SM1044.2
013700 01  CCVS-H-2B.                                                   SM1044.2
013800   02  FILLER                        PIC X(15)  VALUE             SM1044.2
013900            "TEST RESULT OF ".                                    SM1044.2
014000   02  TEST-ID                       PIC X(9).                    SM1044.2
014100   02  FILLER                        PIC X(4)   VALUE             SM1044.2
014200            " IN ".                                               SM1044.2
014300   02  FILLER                        PIC X(12)  VALUE             SM1044.2
014400     " HIGH       ".                                              SM1044.2
014500   02  FILLER                        PIC X(22)  VALUE             SM1044.2
014600            " LEVEL VALIDATION FOR ".                             SM1044.2
014700   02  FILLER                        PIC X(58)  VALUE             SM1044.2
014800     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SM1044.2
014900 01  CCVS-H-3.                                                    SM1044.2
015000     02  FILLER                      PIC X(34)  VALUE             SM1044.2
015100            " FOR OFFICIAL USE ONLY    ".                         SM1044.2
015200     02  FILLER                      PIC X(58)  VALUE             SM1044.2
015300     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SM1044.2
015400     02  FILLER                      PIC X(28)  VALUE             SM1044.2
015500            "  COPYRIGHT   1985 ".                                SM1044.2
015600 01  CCVS-E-1.                                                    SM1044.2
015700     02 FILLER                       PIC X(52)  VALUE SPACE.      SM1044.2
015800     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              SM1044.2
015900     02 ID-AGAIN                     PIC X(9).                    SM1044.2
016000     02 FILLER                       PIC X(45)  VALUE SPACES.     SM1044.2
016100 01  CCVS-E-2.                                                    SM1044.2
016200     02  FILLER                      PIC X(31)  VALUE SPACE.      SM1044.2
016300     02  FILLER                      PIC X(21)  VALUE SPACE.      SM1044.2
016400     02 CCVS-E-2-2.                                               SM1044.2
016500         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      SM1044.2
016600         03 FILLER                   PIC X      VALUE SPACE.      SM1044.2
016700         03 ENDER-DESC               PIC X(44)  VALUE             SM1044.2
016800            "ERRORS ENCOUNTERED".                                 SM1044.2
016900 01  CCVS-E-3.                                                    SM1044.2
017000     02  FILLER                      PIC X(22)  VALUE             SM1044.2
017100            " FOR OFFICIAL USE ONLY".                             SM1044.2
017200     02  FILLER                      PIC X(12)  VALUE SPACE.      SM1044.2
017300     02  FILLER                      PIC X(58)  VALUE             SM1044.2
017400     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SM1044.2
017500     02  FILLER                      PIC X(13)  VALUE SPACE.      SM1044.2
017600     02 FILLER                       PIC X(15)  VALUE             SM1044.2
017700             " COPYRIGHT 1985".                                   SM1044.2
017800 01  CCVS-E-4.                                                    SM1044.2
017900     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      SM1044.2
018000     02 FILLER                       PIC X(4)   VALUE " OF ".     SM1044.2
018100     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      SM1044.2
018200     02 FILLER                       PIC X(40)  VALUE             SM1044.2
018300      "  TESTS WERE EXECUTED SUCCESSFULLY".                       SM1044.2
018400 01  XXINFO.                                                      SM1044.2
018500     02 FILLER                       PIC X(19)  VALUE             SM1044.2
018600            "*** INFORMATION ***".                                SM1044.2
018700     02 INFO-TEXT.                                                SM1044.2
018800       04 FILLER                     PIC X(8)   VALUE SPACE.      SM1044.2
018900       04 XXCOMPUTED                 PIC X(20).                   SM1044.2
019000       04 FILLER                     PIC X(5)   VALUE SPACE.      SM1044.2
019100       04 XXCORRECT                  PIC X(20).                   SM1044.2
019200     02 INF-ANSI-REFERENCE           PIC X(48).                   SM1044.2
019300 01  HYPHEN-LINE.                                                 SM1044.2
019400     02 FILLER  PIC IS X VALUE IS SPACE.                          SM1044.2
019500     02 FILLER  PIC IS X(65)    VALUE IS "************************SM1044.2
019600-    "*****************************************".                 SM1044.2
019700     02 FILLER  PIC IS X(54)    VALUE IS "************************SM1044.2
019800-    "******************************".                            SM1044.2
019900 01  CCVS-PGM-ID                     PIC X(9)   VALUE             SM1044.2
020000     "SM104A".                                                    SM1044.2
020100 PROCEDURE DIVISION.                                              SM1044.2
020200 CCVS1 SECTION.                                                   SM1044.2
020300 OPEN-FILES.                                                      SM1044.2
020400     OPEN     OUTPUT PRINT-FILE.                                  SM1044.2
020500     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   SM1044.2
020600     MOVE    SPACE TO TEST-RESULTS.                               SM1044.2
020700     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             SM1044.2
020800     GO TO CCVS1-EXIT.                                            SM1044.2
020900 CLOSE-FILES.                                                     SM1044.2
021000     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   SM1044.2
021100 TERMINATE-CCVS.                                                  SM1044.2
021200*    EXIT PROGRAM.                                                SM1044.2
021300*TERMINATE-CALL.                                                  SM1044.2
021400     STOP     RUN.                                                SM1044.2
021500 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         SM1044.2
021600 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           SM1044.2
021700 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          SM1044.2
021800 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      SM1044.2
021900     MOVE "****TEST DELETED****" TO RE-MARK.                      SM1044.2
022000 PRINT-DETAIL.                                                    SM1044.2
022100     IF REC-CT NOT EQUAL TO ZERO                                  SM1044.2
022200             MOVE "." TO PARDOT-X                                 SM1044.2
022300             MOVE REC-CT TO DOTVALUE.                             SM1044.2
022400     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      SM1044.2
022500     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               SM1044.2
022600        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 SM1044.2
022700          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 SM1044.2
022800     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              SM1044.2
022900     MOVE SPACE TO CORRECT-X.                                     SM1044.2
023000     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         SM1044.2
023100     MOVE     SPACE TO RE-MARK.                                   SM1044.2
023200 HEAD-ROUTINE.                                                    SM1044.2
023300     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  SM1044.2
023400     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  SM1044.2
023500     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  SM1044.2
023600     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  SM1044.2
023700 COLUMN-NAMES-ROUTINE.                                            SM1044.2
023800     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SM1044.2
023900     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SM1044.2
024000     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        SM1044.2
024100 END-ROUTINE.                                                     SM1044.2
024200     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.SM1044.2
024300 END-RTN-EXIT.                                                    SM1044.2
024400     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SM1044.2
024500 END-ROUTINE-1.                                                   SM1044.2
024600      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      SM1044.2
024700      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               SM1044.2
024800      ADD PASS-COUNTER TO ERROR-HOLD.                             SM1044.2
024900*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   SM1044.2
025000      MOVE PASS-COUNTER TO CCVS-E-4-1.                            SM1044.2
025100      MOVE ERROR-HOLD TO CCVS-E-4-2.                              SM1044.2
025200      MOVE CCVS-E-4 TO CCVS-E-2-2.                                SM1044.2
025300      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           SM1044.2
025400  END-ROUTINE-12.                                                 SM1044.2
025500      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        SM1044.2
025600     IF       ERROR-COUNTER IS EQUAL TO ZERO                      SM1044.2
025700         MOVE "NO " TO ERROR-TOTAL                                SM1044.2
025800         ELSE                                                     SM1044.2
025900         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       SM1044.2
026000     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           SM1044.2
026100     PERFORM WRITE-LINE.                                          SM1044.2
026200 END-ROUTINE-13.                                                  SM1044.2
026300     IF DELETE-COUNTER IS EQUAL TO ZERO                           SM1044.2
026400         MOVE "NO " TO ERROR-TOTAL  ELSE                          SM1044.2
026500         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      SM1044.2
026600     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   SM1044.2
026700     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SM1044.2
026800      IF   INSPECT-COUNTER EQUAL TO ZERO                          SM1044.2
026900          MOVE "NO " TO ERROR-TOTAL                               SM1044.2
027000      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   SM1044.2
027100      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            SM1044.2
027200      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          SM1044.2
027300     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SM1044.2
027400 WRITE-LINE.                                                      SM1044.2
027500     ADD 1 TO RECORD-COUNT.                                       SM1044.2
027600*    IF RECORD-COUNT GREATER 50                                   SM1044.2
027700*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          SM1044.2
027800*        MOVE SPACE TO DUMMY-RECORD                               SM1044.2
027900*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  SM1044.2
028000*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             SM1044.2
028100*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     SM1044.2
028200*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          SM1044.2
028300*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          SM1044.2
028400*        MOVE ZERO TO RECORD-COUNT.                               SM1044.2
028500     PERFORM WRT-LN.                                              SM1044.2
028600 WRT-LN.                                                          SM1044.2
028700     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               SM1044.2
028800     MOVE SPACE TO DUMMY-RECORD.                                  SM1044.2
028900 BLANK-LINE-PRINT.                                                SM1044.2
029000     PERFORM WRT-LN.                                              SM1044.2
029100 FAIL-ROUTINE.                                                    SM1044.2
029200     IF   COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE. SM1044.2
029300     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.SM1044.2
029400     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 SM1044.2
029500     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   SM1044.2
029600     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SM1044.2
029700     MOVE   SPACES TO INF-ANSI-REFERENCE.                         SM1044.2
029800     GO TO  FAIL-ROUTINE-EX.                                      SM1044.2
029900 FAIL-ROUTINE-WRITE.                                              SM1044.2
030000     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         SM1044.2
030100     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 SM1044.2
030200     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. SM1044.2
030300     MOVE   SPACES TO COR-ANSI-REFERENCE.                         SM1044.2
030400 FAIL-ROUTINE-EX. EXIT.                                           SM1044.2
030500 BAIL-OUT.                                                        SM1044.2
030600     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   SM1044.2
030700     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           SM1044.2
030800 BAIL-OUT-WRITE.                                                  SM1044.2
030900     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  SM1044.2
031000     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 SM1044.2
031100     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SM1044.2
031200     MOVE   SPACES TO INF-ANSI-REFERENCE.                         SM1044.2
031300 BAIL-OUT-EX. EXIT.                                               SM1044.2
031400 CCVS1-EXIT.                                                      SM1044.2
031500     EXIT.                                                        SM1044.2
031600 INITIALIZATION SECTION.                                          SM1044.2
031700 SM104A-INIT.                                                     SM1044.2
031800     OPEN     INPUT TEST-FILE.                                    SM1044.2
031900     MOVE     "ALL TESTS IN SM104A CHECK" TO RE-MARK.             SM1044.2
032000     PERFORM  PRINT-DETAIL.                                       SM1044.2
032100     MOVE     "OUTPUT OF SM103A."         TO RE-MARK.             SM1044.2
032200     PERFORM  PRINT-DETAIL.                                       SM1044.2
032300     MOVE     "COPY ---" TO FEATURE.                              SM1044.2
032400     PERFORM  PRINT-DETAIL.                                       SM1044.2
032500 COPY-INIT-A.                                                     SM1044.2
032600     MOVE     "COPY WITHIN NOTE" TO FEATURE.                      SM1044.2
032700 COPY-TEST-1.                                                     SM1044.2
032800                                                                  SM1044.2
032900                                                                  SM1044.2
033000                                                                  SM1044.2
033100                                                                  SM1044.2
033200                                                                  SM1044.2
033300*                                                                 SM1044.2
033400******************* COPY WITHIN NOTE USED ************************SM1044.2
033500*                                                                 SM1044.2
033600*        NOTE                                         COPY K4NTA. SM1044.2
033700*                                                                 SM1044.2
033800**************** NO TEXT EXPANSION SHOULD OCCUR ******************SM1044.2
033900*        NOTE                                          COPY K4NTA.SM1044.2
034000 COPY-PASS-1.                                                     SM1044.2
034100     PERFORM PASS.                                                SM1044.2
034200*        NOTE K4NTA IS IN THE LIBRARY BUT IT SHOULD               SM1044.2
034300*             NOT BE COPIED.                                      SM1044.2
034400 COPY-WRITE-1.                                                    SM1044.2
034500     MOVE     "COPY-TEST-1" TO PAR-NAME.                          SM1044.2
034600     PERFORM  PRINT-DETAIL.                                       SM1044.2
034700 COPY-TEST-2.                                                     SM1044.2
034800*        NOTE THE WORDS COPY WHICH ARE IN THIS NOTE SHOULD        SM1044.2
034900*             NOT BE TREATED AS COPY VERBS, AND THE FOLLOWING     SM1044.2
035000*             "STATEMENTS" SHOULD BE TREATED AS PART OF THIS NOTE.SM1044.2
035100*             PERFORM FAIL.                                       SM1044.2
035200*             MOVE "SEE COPY-TEST-2" TO RE-MARK.                  SM1044.2
035300*             GO TO COPY-WRITE-2.                                 SM1044.2
035400 COPY-PASS-2.                                                     SM1044.2
035500     PERFORM PASS.                                                SM1044.2
035600 COPY-WRITE-2.                                                    SM1044.2
035700     MOVE     "COPY-TEST-2" TO PAR-NAME.                          SM1044.2
035800     PERFORM PRINT-DETAIL.                                        SM1044.2
035900 ENVIRONMENT-TEST SECTION.                                        SM1044.2
036000 COPY-TEST-3.                                                     SM1044.2
036100     PERFORM  READ-TEST-FILE.                                     SM1044.2
036200     IF       TST-FLD-2 EQUAL TO "12.345.678,91"                  SM1044.2
036300              PERFORM PASS GO TO COPY-WRITE-3.                    SM1044.2
036400     GO       TO COPY-FAIL-3.                                     SM1044.2
036500 COPY-DELETE-3.                                                   SM1044.2
036600     PERFORM  DE-LETE.                                            SM1044.2
036700     GO       TO COPY-WRITE-3.                                    SM1044.2
036800 COPY-FAIL-3.                                                     SM1044.2
036900     MOVE     TST-FLD-2 TO COMPUTED-N.                            SM1044.2
037000     MOVE     "12.345.678,91" TO CORRECT-A.                       SM1044.2
037100     PERFORM  FAIL.                                               SM1044.2
037200 COPY-WRITE-3.                                                    SM1044.2
037300     MOVE     "  DEC POINT IS COMMA" TO FEATURE.                  SM1044.2
037400     MOVE     "COPY-TEST-3 " TO PAR-NAME.                         SM1044.2
037500     PERFORM  PRINT-DETAIL.                                       SM1044.2
037600 COPY-INIT-B.                                                     SM1044.2
037700     MOVE     "  ENVIR DIV ENTRIES" TO FEATURE.                   SM1044.2
037800 COPY-TEST-4.                                                     SM1044.2
037900     IF       TST-FLD-1 EQUAL TO 97532                            SM1044.2
038000              PERFORM PASS GO TO COPY-WRITE-4.                    SM1044.2
038100     GO       TO COPY-FAIL-4.                                     SM1044.2
038200 COPY-DELETE-4.                                                   SM1044.2
038300     PERFORM  DE-LETE.                                            SM1044.2
038400     GO       TO COPY-WRITE-4.                                    SM1044.2
038500 COPY-FAIL-4.                                                     SM1044.2
038600     MOVE     TST-FLD-1 TO COMPUTED-N.                            SM1044.2
038700     MOVE     97532 TO CORRECT-N.                                 SM1044.2
038800     PERFORM  FAIL.                                               SM1044.2
038900 COPY-WRITE-4.                                                    SM1044.2
039000     MOVE     "COPY-TEST-4 " TO PAR-NAME.                         SM1044.2
039100     PERFORM  PRINT-DETAIL.                                       SM1044.2
039200 COPY-TEST-5.                                                     SM1044.2
039300     PERFORM  READ-TEST-FILE.                                     SM1044.2
039400     IF       TST-FLD-1 EQUAL TO 23479                            SM1044.2
039500              PERFORM PASS GO TO COPY-WRITE-5.                    SM1044.2
039600     GO       TO COPY-FAIL-5.                                     SM1044.2
039700 COPY-DELETE-5.                                                   SM1044.2
039800     PERFORM  DE-LETE.                                            SM1044.2
039900     GO       TO COPY-WRITE-5.                                    SM1044.2
040000 COPY-FAIL-5.                                                     SM1044.2
040100     MOVE     TST-FLD-1 TO COMPUTED-N.                            SM1044.2
040200     MOVE     23479 TO CORRECT-N.                                 SM1044.2
040300     PERFORM  FAIL.                                               SM1044.2
040400 COPY-WRITE-5.                                                    SM1044.2
040500     MOVE     "COPY-TEST-5 " TO PAR-NAME.                         SM1044.2
040600     PERFORM  PRINT-DETAIL.                                       SM1044.2
040700 COPY-TEST-6.                                                     SM1044.2
040800     PERFORM  READ-TEST-FILE 3 TIMES.                             SM1044.2
040900     IF       TST-FLD-1 EQUAL TO 14003                            SM1044.2
041000              PERFORM PASS GO TO COPY-WRITE-6.                    SM1044.2
041100     GO       TO COPY-FAIL-6.                                     SM1044.2
041200 COPY-DELETE-6.                                                   SM1044.2
041300     PERFORM  DE-LETE.                                            SM1044.2
041400     GO       TO COPY-WRITE-6.                                    SM1044.2
041500 COPY-FAIL-6.                                                     SM1044.2
041600     MOVE     TST-FLD-1 TO COMPUTED-N.                            SM1044.2
041700     MOVE     14003 TO CORRECT-N.                                 SM1044.2
041800     PERFORM  FAIL.                                               SM1044.2
041900 COPY-WRITE-6.                                                    SM1044.2
042000     MOVE     "COPY-TEST-6 " TO PAR-NAME.                         SM1044.2
042100     PERFORM  PRINT-DETAIL.                                       SM1044.2
042200 COPY-TEST-7.                                                     SM1044.2
042300     PERFORM  READ-TEST-FILE 2 TIMES.                             SM1044.2
042400     IF       TST-FLD-1 EQUAL TO 03543                            SM1044.2
042500              PERFORM PASS GO TO COPY-WRITE-7.                    SM1044.2
042600     GO       TO COPY-FAIL-7.                                     SM1044.2
042700 COPY-DELETE-7.                                                   SM1044.2
042800     PERFORM  DE-LETE.                                            SM1044.2
042900     GO       TO COPY-WRITE-7.                                    SM1044.2
043000 COPY-FAIL-7.                                                     SM1044.2
043100     MOVE     TST-FLD-1 TO COMPUTED-N.                            SM1044.2
043200     MOVE     03543 TO CORRECT-N.                                 SM1044.2
043300     PERFORM  FAIL.                                               SM1044.2
043400 COPY-WRITE-7.                                                    SM1044.2
043500     MOVE     "COPY-TEST-7 " TO PAR-NAME.                         SM1044.2
043600     PERFORM  PRINT-DETAIL.                                       SM1044.2
043700     CLOSE    TEST-FILE.                                          SM1044.2
043800     GO TO CCVS-EXIT.                                             SM1044.2
043900 READ-TEST-FILE.                                                  SM1044.2
044000     READ     TEST-FILE          AT END GO TO BAD-FILE.           SM1044.2
044100 BAD-FILE.                                                        SM1044.2
044200     MOVE     "BAD-FILE" TO PAR-NAME.                             SM1044.2
044300     PERFORM  FAIL.                                               SM1044.2
044400     MOVE     "EOF PREMATURELY FOUND" TO RE-MARK.                 SM1044.2
044500     PERFORM  PRINT-DETAIL.                                       SM1044.2
044600     CLOSE    TEST-FILE.                                          SM1044.2
044700 CCVS-EXIT SECTION.                                               SM1044.2
044800 CCVS-999999.                                                     SM1044.2
044900     GO TO CLOSE-FILES.                                           SM1044.2
      *END-OF,SM104A                                                                  
