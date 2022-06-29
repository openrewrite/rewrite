      *HEADER,COBOL,SM207A                                                            
000100 IDENTIFICATION DIVISION.                                         SM2074.2
000200 PROGRAM-ID.                                                      SM2074.2
000300     SM207A.                                                      SM2074.2
000400****************************************************************  SM2074.2
000500*                                                              *  SM2074.2
000600*    VALIDATION FOR:-                                          *  SM2074.2
000700*                                                              *  SM2074.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SM2074.2
000900*                                                              *  SM2074.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SM2074.2
001100*                                                              *  SM2074.2
001200****************************************************************  SM2074.2
001300*                                                              *  SM2074.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  SM2074.2
001500*                                                              *  SM2074.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  SM2074.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  SM2074.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  SM2074.2
001900*                                                              *  SM2074.2
002000****************************************************************  SM2074.2
002100*                                                              *  SM2074.2
002200*    PROGRAM SM207A TESTS THE "COPY" STATEMENT USING TWO       *  SM2074.2
002300*    DIFFERENT LIBRARY NAMES TO QUALIFY THE SAME TEXT NAME.    *  SM2074.2
002400*                                                              *  SM2074.2
002500****************************************************************  SM2074.2
002600*            VP-ROUTINE CONTROL OF LIBRARY CREATION               SM2074.2
002700*            --------------------------------------               SM2074.2
002800*    WHEN THE LIBRARIES ARE PREPARED (CREATED) IN PREPARATION FOR SM2074.2
002900*    RUNNING OF SM207, THE TEXT WHICH WILL BE PLACED IN THE       SM2074.2
003000*    LIBRARY EQUATED TO X-47 SHOULD BE SELECTED FROM THE          SM2074.2
003100*    POPULATION FILE USING THE PLUS-CARD "+ALTLB".  THE TEXT      SM2074.2
003200*    WHICH WILL BE PLACED IN THE LIBRARY EQUATED TO X-48 SHOULD   SM2074.2
003300*    BE SELECTED FROM THE POPULATION FILE USING THE PLUS-CARD     SM2074.2
003400*    "+ALTL1,,,ALTLB".                                            SM2074.2
003500                                                                  SM2074.2
003600                                                                  SM2074.2
003700 ENVIRONMENT DIVISION.                                            SM2074.2
003800 CONFIGURATION SECTION.                                           SM2074.2
003900 SOURCE-COMPUTER.                                                 SM2074.2
004000     XXXXX082.                                                    SM2074.2
004100 OBJECT-COMPUTER.                                                 SM2074.2
004200     XXXXX083.                                                    SM2074.2
004300 INPUT-OUTPUT SECTION.                                            SM2074.2
004400 FILE-CONTROL.                                                    SM2074.2
004500     SELECT PRINT-FILE ASSIGN TO                                  SM2074.2
004600     XXXXX055.                                                    SM2074.2
004700 DATA DIVISION.                                                   SM2074.2
004800 FILE SECTION.                                                    SM2074.2
004900 FD  PRINT-FILE.                                                  SM2074.2
005000 01  PRINT-REC PICTURE X(120).                                    SM2074.2
005100 01  DUMMY-RECORD PICTURE X(120).                                 SM2074.2
005200 WORKING-STORAGE SECTION.                                         SM2074.2
005300 01  TEST-RESULTS.                                                SM2074.2
005400     02 FILLER                   PIC X      VALUE SPACE.          SM2074.2
005500     02 FEATURE                  PIC X(20)  VALUE SPACE.          SM2074.2
005600     02 FILLER                   PIC X      VALUE SPACE.          SM2074.2
005700     02 P-OR-F                   PIC X(5)   VALUE SPACE.          SM2074.2
005800     02 FILLER                   PIC X      VALUE SPACE.          SM2074.2
005900     02  PAR-NAME.                                                SM2074.2
006000       03 FILLER                 PIC X(19)  VALUE SPACE.          SM2074.2
006100       03  PARDOT-X              PIC X      VALUE SPACE.          SM2074.2
006200       03 DOTVALUE               PIC 99     VALUE ZERO.           SM2074.2
006300     02 FILLER                   PIC X(8)   VALUE SPACE.          SM2074.2
006400     02 RE-MARK                  PIC X(61).                       SM2074.2
006500 01  TEST-COMPUTED.                                               SM2074.2
006600     02 FILLER                   PIC X(30)  VALUE SPACE.          SM2074.2
006700     02 FILLER                   PIC X(17)  VALUE                 SM2074.2
006800            "       COMPUTED=".                                   SM2074.2
006900     02 COMPUTED-X.                                               SM2074.2
007000     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          SM2074.2
007100     03 COMPUTED-N               REDEFINES COMPUTED-A             SM2074.2
007200                                 PIC -9(9).9(9).                  SM2074.2
007300     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         SM2074.2
007400     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     SM2074.2
007500     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     SM2074.2
007600     03       CM-18V0 REDEFINES COMPUTED-A.                       SM2074.2
007700         04 COMPUTED-18V0                    PIC -9(18).          SM2074.2
007800         04 FILLER                           PIC X.               SM2074.2
007900     03 FILLER PIC X(50) VALUE SPACE.                             SM2074.2
008000 01  TEST-CORRECT.                                                SM2074.2
008100     02 FILLER PIC X(30) VALUE SPACE.                             SM2074.2
008200     02 FILLER PIC X(17) VALUE "       CORRECT =".                SM2074.2
008300     02 CORRECT-X.                                                SM2074.2
008400     03 CORRECT-A                  PIC X(20) VALUE SPACE.         SM2074.2
008500     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      SM2074.2
008600     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         SM2074.2
008700     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     SM2074.2
008800     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     SM2074.2
008900     03      CR-18V0 REDEFINES CORRECT-A.                         SM2074.2
009000         04 CORRECT-18V0                     PIC -9(18).          SM2074.2
009100         04 FILLER                           PIC X.               SM2074.2
009200     03 FILLER PIC X(2) VALUE SPACE.                              SM2074.2
009300     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     SM2074.2
009400 01  CCVS-C-1.                                                    SM2074.2
009500     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PASM2074.2
009600-    "SS  PARAGRAPH-NAME                                          SM2074.2
009700-    "       REMARKS".                                            SM2074.2
009800     02 FILLER                     PIC X(20)    VALUE SPACE.      SM2074.2
009900 01  CCVS-C-2.                                                    SM2074.2
010000     02 FILLER                     PIC X        VALUE SPACE.      SM2074.2
010100     02 FILLER                     PIC X(6)     VALUE "TESTED".   SM2074.2
010200     02 FILLER                     PIC X(15)    VALUE SPACE.      SM2074.2
010300     02 FILLER                     PIC X(4)     VALUE "FAIL".     SM2074.2
010400     02 FILLER                     PIC X(94)    VALUE SPACE.      SM2074.2
010500 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       SM2074.2
010600 01  REC-CT                        PIC 99       VALUE ZERO.       SM2074.2
010700 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       SM2074.2
010800 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       SM2074.2
010900 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       SM2074.2
011000 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       SM2074.2
011100 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       SM2074.2
011200 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       SM2074.2
011300 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      SM2074.2
011400 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       SM2074.2
011500 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     SM2074.2
011600 01  CCVS-H-1.                                                    SM2074.2
011700     02  FILLER                    PIC X(39)    VALUE SPACES.     SM2074.2
011800     02  FILLER                    PIC X(42)    VALUE             SM2074.2
011900     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 SM2074.2
012000     02  FILLER                    PIC X(39)    VALUE SPACES.     SM2074.2
012100 01  CCVS-H-2A.                                                   SM2074.2
012200   02  FILLER                        PIC X(40)  VALUE SPACE.      SM2074.2
012300   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  SM2074.2
012400   02  FILLER                        PIC XXXX   VALUE             SM2074.2
012500     "4.2 ".                                                      SM2074.2
012600   02  FILLER                        PIC X(28)  VALUE             SM2074.2
012700            " COPY - NOT FOR DISTRIBUTION".                       SM2074.2
012800   02  FILLER                        PIC X(41)  VALUE SPACE.      SM2074.2
012900                                                                  SM2074.2
013000 01  CCVS-H-2B.                                                   SM2074.2
013100   02  FILLER                        PIC X(15)  VALUE             SM2074.2
013200            "TEST RESULT OF ".                                    SM2074.2
013300   02  TEST-ID                       PIC X(9).                    SM2074.2
013400   02  FILLER                        PIC X(4)   VALUE             SM2074.2
013500            " IN ".                                               SM2074.2
013600   02  FILLER                        PIC X(12)  VALUE             SM2074.2
013700     " HIGH       ".                                              SM2074.2
013800   02  FILLER                        PIC X(22)  VALUE             SM2074.2
013900            " LEVEL VALIDATION FOR ".                             SM2074.2
014000   02  FILLER                        PIC X(58)  VALUE             SM2074.2
014100     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SM2074.2
014200 01  CCVS-H-3.                                                    SM2074.2
014300     02  FILLER                      PIC X(34)  VALUE             SM2074.2
014400            " FOR OFFICIAL USE ONLY    ".                         SM2074.2
014500     02  FILLER                      PIC X(58)  VALUE             SM2074.2
014600     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SM2074.2
014700     02  FILLER                      PIC X(28)  VALUE             SM2074.2
014800            "  COPYRIGHT   1985 ".                                SM2074.2
014900 01  CCVS-E-1.                                                    SM2074.2
015000     02 FILLER                       PIC X(52)  VALUE SPACE.      SM2074.2
015100     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              SM2074.2
015200     02 ID-AGAIN                     PIC X(9).                    SM2074.2
015300     02 FILLER                       PIC X(45)  VALUE SPACES.     SM2074.2
015400 01  CCVS-E-2.                                                    SM2074.2
015500     02  FILLER                      PIC X(31)  VALUE SPACE.      SM2074.2
015600     02  FILLER                      PIC X(21)  VALUE SPACE.      SM2074.2
015700     02 CCVS-E-2-2.                                               SM2074.2
015800         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      SM2074.2
015900         03 FILLER                   PIC X      VALUE SPACE.      SM2074.2
016000         03 ENDER-DESC               PIC X(44)  VALUE             SM2074.2
016100            "ERRORS ENCOUNTERED".                                 SM2074.2
016200 01  CCVS-E-3.                                                    SM2074.2
016300     02  FILLER                      PIC X(22)  VALUE             SM2074.2
016400            " FOR OFFICIAL USE ONLY".                             SM2074.2
016500     02  FILLER                      PIC X(12)  VALUE SPACE.      SM2074.2
016600     02  FILLER                      PIC X(58)  VALUE             SM2074.2
016700     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SM2074.2
016800     02  FILLER                      PIC X(13)  VALUE SPACE.      SM2074.2
016900     02 FILLER                       PIC X(15)  VALUE             SM2074.2
017000             " COPYRIGHT 1985".                                   SM2074.2
017100 01  CCVS-E-4.                                                    SM2074.2
017200     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      SM2074.2
017300     02 FILLER                       PIC X(4)   VALUE " OF ".     SM2074.2
017400     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      SM2074.2
017500     02 FILLER                       PIC X(40)  VALUE             SM2074.2
017600      "  TESTS WERE EXECUTED SUCCESSFULLY".                       SM2074.2
017700 01  XXINFO.                                                      SM2074.2
017800     02 FILLER                       PIC X(19)  VALUE             SM2074.2
017900            "*** INFORMATION ***".                                SM2074.2
018000     02 INFO-TEXT.                                                SM2074.2
018100       04 FILLER                     PIC X(8)   VALUE SPACE.      SM2074.2
018200       04 XXCOMPUTED                 PIC X(20).                   SM2074.2
018300       04 FILLER                     PIC X(5)   VALUE SPACE.      SM2074.2
018400       04 XXCORRECT                  PIC X(20).                   SM2074.2
018500     02 INF-ANSI-REFERENCE           PIC X(48).                   SM2074.2
018600 01  HYPHEN-LINE.                                                 SM2074.2
018700     02 FILLER  PIC IS X VALUE IS SPACE.                          SM2074.2
018800     02 FILLER  PIC IS X(65)    VALUE IS "************************SM2074.2
018900-    "*****************************************".                 SM2074.2
019000     02 FILLER  PIC IS X(54)    VALUE IS "************************SM2074.2
019100-    "******************************".                            SM2074.2
019200 01  CCVS-PGM-ID                     PIC X(9)   VALUE             SM2074.2
019300     "SM207A".                                                    SM2074.2
019400 PROCEDURE DIVISION.                                              SM2074.2
019500 CCVS1 SECTION.                                                   SM2074.2
019600 OPEN-FILES.                                                      SM2074.2
019700     OPEN     OUTPUT PRINT-FILE.                                  SM2074.2
019800     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   SM2074.2
019900     MOVE    SPACE TO TEST-RESULTS.                               SM2074.2
020000     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             SM2074.2
020100     GO TO CCVS1-EXIT.                                            SM2074.2
020200 CLOSE-FILES.                                                     SM2074.2
020300     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   SM2074.2
020400 TERMINATE-CCVS.                                                  SM2074.2
020500*    EXIT PROGRAM.                                                SM2074.2
020600*TERMINATE-CALL.                                                  SM2074.2
020700     STOP     RUN.                                                SM2074.2
020800 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         SM2074.2
020900 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           SM2074.2
021000 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          SM2074.2
021100 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      SM2074.2
021200     MOVE "****TEST DELETED****" TO RE-MARK.                      SM2074.2
021300 PRINT-DETAIL.                                                    SM2074.2
021400     IF REC-CT NOT EQUAL TO ZERO                                  SM2074.2
021500             MOVE "." TO PARDOT-X                                 SM2074.2
021600             MOVE REC-CT TO DOTVALUE.                             SM2074.2
021700     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      SM2074.2
021800     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               SM2074.2
021900        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 SM2074.2
022000          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 SM2074.2
022100     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              SM2074.2
022200     MOVE SPACE TO CORRECT-X.                                     SM2074.2
022300     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         SM2074.2
022400     MOVE     SPACE TO RE-MARK.                                   SM2074.2
022500 HEAD-ROUTINE.                                                    SM2074.2
022600     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  SM2074.2
022700     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  SM2074.2
022800     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  SM2074.2
022900     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  SM2074.2
023000 COLUMN-NAMES-ROUTINE.                                            SM2074.2
023100     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SM2074.2
023200     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SM2074.2
023300     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        SM2074.2
023400 END-ROUTINE.                                                     SM2074.2
023500     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.SM2074.2
023600 END-RTN-EXIT.                                                    SM2074.2
023700     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SM2074.2
023800 END-ROUTINE-1.                                                   SM2074.2
023900      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      SM2074.2
024000      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               SM2074.2
024100      ADD PASS-COUNTER TO ERROR-HOLD.                             SM2074.2
024200*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   SM2074.2
024300      MOVE PASS-COUNTER TO CCVS-E-4-1.                            SM2074.2
024400      MOVE ERROR-HOLD TO CCVS-E-4-2.                              SM2074.2
024500      MOVE CCVS-E-4 TO CCVS-E-2-2.                                SM2074.2
024600      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           SM2074.2
024700  END-ROUTINE-12.                                                 SM2074.2
024800      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        SM2074.2
024900     IF       ERROR-COUNTER IS EQUAL TO ZERO                      SM2074.2
025000         MOVE "NO " TO ERROR-TOTAL                                SM2074.2
025100         ELSE                                                     SM2074.2
025200         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       SM2074.2
025300     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           SM2074.2
025400     PERFORM WRITE-LINE.                                          SM2074.2
025500 END-ROUTINE-13.                                                  SM2074.2
025600     IF DELETE-COUNTER IS EQUAL TO ZERO                           SM2074.2
025700         MOVE "NO " TO ERROR-TOTAL  ELSE                          SM2074.2
025800         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      SM2074.2
025900     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   SM2074.2
026000     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SM2074.2
026100      IF   INSPECT-COUNTER EQUAL TO ZERO                          SM2074.2
026200          MOVE "NO " TO ERROR-TOTAL                               SM2074.2
026300      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   SM2074.2
026400      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            SM2074.2
026500      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          SM2074.2
026600     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SM2074.2
026700 WRITE-LINE.                                                      SM2074.2
026800     ADD 1 TO RECORD-COUNT.                                       SM2074.2
026900*    IF RECORD-COUNT GREATER 50                                   SM2074.2
027000*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          SM2074.2
027100*        MOVE SPACE TO DUMMY-RECORD                               SM2074.2
027200*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  SM2074.2
027300*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             SM2074.2
027400*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     SM2074.2
027500*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          SM2074.2
027600*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          SM2074.2
027700*        MOVE ZERO TO RECORD-COUNT.                               SM2074.2
027800     PERFORM WRT-LN.                                              SM2074.2
027900 WRT-LN.                                                          SM2074.2
028000     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               SM2074.2
028100     MOVE SPACE TO DUMMY-RECORD.                                  SM2074.2
028200 BLANK-LINE-PRINT.                                                SM2074.2
028300     PERFORM WRT-LN.                                              SM2074.2
028400 FAIL-ROUTINE.                                                    SM2074.2
028500     IF   COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE. SM2074.2
028600     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.SM2074.2
028700     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 SM2074.2
028800     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   SM2074.2
028900     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SM2074.2
029000     MOVE   SPACES TO INF-ANSI-REFERENCE.                         SM2074.2
029100     GO TO  FAIL-ROUTINE-EX.                                      SM2074.2
029200 FAIL-ROUTINE-WRITE.                                              SM2074.2
029300     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         SM2074.2
029400     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 SM2074.2
029500     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. SM2074.2
029600     MOVE   SPACES TO COR-ANSI-REFERENCE.                         SM2074.2
029700 FAIL-ROUTINE-EX. EXIT.                                           SM2074.2
029800 BAIL-OUT.                                                        SM2074.2
029900     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   SM2074.2
030000     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           SM2074.2
030100 BAIL-OUT-WRITE.                                                  SM2074.2
030200     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  SM2074.2
030300     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 SM2074.2
030400     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SM2074.2
030500     MOVE   SPACES TO INF-ANSI-REFERENCE.                         SM2074.2
030600 BAIL-OUT-EX. EXIT.                                               SM2074.2
030700 CCVS1-EXIT.                                                      SM2074.2
030800     EXIT.                                                        SM2074.2
030900 SECT-SM207A-001 SECTION.                                         SM2074.2
031000 QUAL-TEST-01.                                                    SM2074.2
031100     MOVE "NOTHING COPIED" TO RE-MARK.                            SM2074.2
031200     PERFORM FAIL.                                                SM2074.2
031300*                                                                 SM2074.2
031400*********************** COPY STATEMENT USED **********************SM2074.2
031500*                                                                 SM2074.2
031600*    COPY ALTLB OF                                                SM2074.2
031700*    XXXXX047.                                                    SM2074.2
031800*                                                                 SM2074.2
031900******************** COPIED TEXT BEGINS BELOW ********************SM2074.2
032000     COPY ALTLB OF                                                SM2074.2
032100     XXXXX047.                                                    SM2074.2
032200*********************** END OF COPIED TEXT ***********************SM2074.2
032300     GO TO QUAL-WRITE-01.                                         SM2074.2
032400 QUAL-DELETE-01.                                                  SM2074.2
032500     PERFORM DE-LETE.                                             SM2074.2
032600 QUAL-WRITE-01.                                                   SM2074.2
032700     MOVE "QUAL-TEST-01" TO PAR-NAME.                             SM2074.2
032800     MOVE "QUALIFIED LIBRY NAME" TO FEATURE.                      SM2074.2
032900     PERFORM PRINT-DETAIL.                                        SM2074.2
033000 QUAL-TEST-02.                                                    SM2074.2
033100     ADD 1 TO ERROR-COUNTER.                                      SM2074.2
033200*                                                                 SM2074.2
033300*********************** COPY STATEMENT USED **********************SM2074.2
033400*                                                                 SM2074.2
033500*    COPY ALTLB IN                                                SM2074.2
033600*    XXXXX048.                                                    SM2074.2
033700*                                                                 SM2074.2
033800******************** COPIED TEXT BEGINS BELOW ********************SM2074.2
033900     COPY ALTLB IN                                                SM2074.2
034000     XXXXX048.                                                    SM2074.2
034100*********************** END OF COPIED TEXT ***********************SM2074.2
034200     IF P-OR-F IS EQUAL TO "PASS "                                SM2074.2
034300         PERFORM FAIL                                             SM2074.2
034400         MOVE "TEXT COPIED FROM WRONG LIBRARY" TO RE-MARK         SM2074.2
034500         GO TO QUAL-WRITE-02.                                     SM2074.2
034600     IF P-OR-F IS EQUAL TO "FAIL*"                                SM2074.2
034700         PERFORM PASS                                             SM2074.2
034800         SUBTRACT 1 FROM ERROR-COUNTER                            SM2074.2
034900         MOVE SPACES TO RE-MARK                                   SM2074.2
035000         GO TO QUAL-WRITE-02.                                     SM2074.2
035100     PERFORM FAIL.                                                SM2074.2
035200     SUBTRACT 1 FROM ERROR-COUNTER.                               SM2074.2
035300     MOVE "NOTHING COPIED" TO RE-MARK.                            SM2074.2
035400     GO TO QUAL-WRITE-02.                                         SM2074.2
035500 QUAL-DELETE-02.                                                  SM2074.2
035600     PERFORM DE-LETE.                                             SM2074.2
035700 QUAL-WRITE-02.                                                   SM2074.2
035800     MOVE "QUAL-TEST-02" TO PAR-NAME.                             SM2074.2
035900     PERFORM PRINT-DETAIL.                                        SM2074.2
036000 CCVS-EXIT SECTION.                                               SM2074.2
036100 CCVS-999999.                                                     SM2074.2
036200     GO TO CLOSE-FILES.                                           SM2074.2
      *END-OF,SM207A                                                                  
