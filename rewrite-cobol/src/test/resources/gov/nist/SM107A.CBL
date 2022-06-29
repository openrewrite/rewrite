      *HEADER,COBOL,SM107A                                                            
000100 IDENTIFICATION DIVISION.                                         SM1074.2
000200 PROGRAM-ID.                                                      SM1074.2
000300     SM107A.                                                      SM1074.2
000400****************************************************************  SM1074.2
000500*                                                              *  SM1074.2
000600*    VALIDATION FOR:-                                          *  SM1074.2
000700*                                                              *  SM1074.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SM1074.2
000900*                                                              *  SM1074.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SM1074.2
001100*                                                              *  SM1074.2
001200****************************************************************  SM1074.2
001300*                                                              *  SM1074.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  SM1074.2
001500*                                                              *  SM1074.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  SM1074.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  SM1074.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  SM1074.2
001900*                                                              *  SM1074.2
002000****************************************************************  SM1074.2
002100*                                                              *  SM1074.2
002200*    PROGRAM SM107A TESTS THE CAPABILITY TO COPY 1599 RECORDS  *  SM1074.2
002300*    BY A SINGLE "COPY" STATEMENT IN THE PROCEDURE DIVISION.   *  SM1074.2
002400*                                                              *  SM1074.2
002500****************************************************************  SM1074.2
002600 ENVIRONMENT DIVISION.                                            SM1074.2
002700 CONFIGURATION SECTION.                                           SM1074.2
002800 SOURCE-COMPUTER.                                                 SM1074.2
002900     XXXXX082.                                                    SM1074.2
003000 OBJECT-COMPUTER.                                                 SM1074.2
003100     XXXXX083.                                                    SM1074.2
003200 INPUT-OUTPUT SECTION.                                            SM1074.2
003300 FILE-CONTROL.                                                    SM1074.2
003400     SELECT PRINT-FILE ASSIGN TO                                  SM1074.2
003500     XXXXX055.                                                    SM1074.2
003600 DATA DIVISION.                                                   SM1074.2
003700 FILE SECTION.                                                    SM1074.2
003800 FD  PRINT-FILE.                                                  SM1074.2
003900 01  PRINT-REC PICTURE X(120).                                    SM1074.2
004000 01  DUMMY-RECORD PICTURE X(120).                                 SM1074.2
004100 WORKING-STORAGE SECTION.                                         SM1074.2
004200 01  TEST-RESULTS.                                                SM1074.2
004300     02 FILLER                   PIC X      VALUE SPACE.          SM1074.2
004400     02 FEATURE                  PIC X(20)  VALUE SPACE.          SM1074.2
004500     02 FILLER                   PIC X      VALUE SPACE.          SM1074.2
004600     02 P-OR-F                   PIC X(5)   VALUE SPACE.          SM1074.2
004700     02 FILLER                   PIC X      VALUE SPACE.          SM1074.2
004800     02  PAR-NAME.                                                SM1074.2
004900       03 FILLER                 PIC X(19)  VALUE SPACE.          SM1074.2
005000       03  PARDOT-X              PIC X      VALUE SPACE.          SM1074.2
005100       03 DOTVALUE               PIC 99     VALUE ZERO.           SM1074.2
005200     02 FILLER                   PIC X(8)   VALUE SPACE.          SM1074.2
005300     02 RE-MARK                  PIC X(61).                       SM1074.2
005400 01  TEST-COMPUTED.                                               SM1074.2
005500     02 FILLER                   PIC X(30)  VALUE SPACE.          SM1074.2
005600     02 FILLER                   PIC X(17)  VALUE                 SM1074.2
005700            "       COMPUTED=".                                   SM1074.2
005800     02 COMPUTED-X.                                               SM1074.2
005900     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          SM1074.2
006000     03 COMPUTED-N               REDEFINES COMPUTED-A             SM1074.2
006100                                 PIC -9(9).9(9).                  SM1074.2
006200     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         SM1074.2
006300     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     SM1074.2
006400     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     SM1074.2
006500     03       CM-18V0 REDEFINES COMPUTED-A.                       SM1074.2
006600         04 COMPUTED-18V0                    PIC -9(18).          SM1074.2
006700         04 FILLER                           PIC X.               SM1074.2
006800     03 FILLER PIC X(50) VALUE SPACE.                             SM1074.2
006900 01  TEST-CORRECT.                                                SM1074.2
007000     02 FILLER PIC X(30) VALUE SPACE.                             SM1074.2
007100     02 FILLER PIC X(17) VALUE "       CORRECT =".                SM1074.2
007200     02 CORRECT-X.                                                SM1074.2
007300     03 CORRECT-A                  PIC X(20) VALUE SPACE.         SM1074.2
007400     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      SM1074.2
007500     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         SM1074.2
007600     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     SM1074.2
007700     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     SM1074.2
007800     03      CR-18V0 REDEFINES CORRECT-A.                         SM1074.2
007900         04 CORRECT-18V0                     PIC -9(18).          SM1074.2
008000         04 FILLER                           PIC X.               SM1074.2
008100     03 FILLER PIC X(2) VALUE SPACE.                              SM1074.2
008200     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     SM1074.2
008300 01  CCVS-C-1.                                                    SM1074.2
008400     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PASM1074.2
008500-    "SS  PARAGRAPH-NAME                                          SM1074.2
008600-    "       REMARKS".                                            SM1074.2
008700     02 FILLER                     PIC X(20)    VALUE SPACE.      SM1074.2
008800 01  CCVS-C-2.                                                    SM1074.2
008900     02 FILLER                     PIC X        VALUE SPACE.      SM1074.2
009000     02 FILLER                     PIC X(6)     VALUE "TESTED".   SM1074.2
009100     02 FILLER                     PIC X(15)    VALUE SPACE.      SM1074.2
009200     02 FILLER                     PIC X(4)     VALUE "FAIL".     SM1074.2
009300     02 FILLER                     PIC X(94)    VALUE SPACE.      SM1074.2
009400 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       SM1074.2
009500 01  REC-CT                        PIC 99       VALUE ZERO.       SM1074.2
009600 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       SM1074.2
009700 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       SM1074.2
009800 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       SM1074.2
009900 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       SM1074.2
010000 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       SM1074.2
010100 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       SM1074.2
010200 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      SM1074.2
010300 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       SM1074.2
010400 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     SM1074.2
010500 01  CCVS-H-1.                                                    SM1074.2
010600     02  FILLER                    PIC X(39)    VALUE SPACES.     SM1074.2
010700     02  FILLER                    PIC X(42)    VALUE             SM1074.2
010800     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 SM1074.2
010900     02  FILLER                    PIC X(39)    VALUE SPACES.     SM1074.2
011000 01  CCVS-H-2A.                                                   SM1074.2
011100   02  FILLER                        PIC X(40)  VALUE SPACE.      SM1074.2
011200   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  SM1074.2
011300   02  FILLER                        PIC XXXX   VALUE             SM1074.2
011400     "4.2 ".                                                      SM1074.2
011500   02  FILLER                        PIC X(28)  VALUE             SM1074.2
011600            " COPY - NOT FOR DISTRIBUTION".                       SM1074.2
011700   02  FILLER                        PIC X(41)  VALUE SPACE.      SM1074.2
011800                                                                  SM1074.2
011900 01  CCVS-H-2B.                                                   SM1074.2
012000   02  FILLER                        PIC X(15)  VALUE             SM1074.2
012100            "TEST RESULT OF ".                                    SM1074.2
012200   02  TEST-ID                       PIC X(9).                    SM1074.2
012300   02  FILLER                        PIC X(4)   VALUE             SM1074.2
012400            " IN ".                                               SM1074.2
012500   02  FILLER                        PIC X(12)  VALUE             SM1074.2
012600     " HIGH       ".                                              SM1074.2
012700   02  FILLER                        PIC X(22)  VALUE             SM1074.2
012800            " LEVEL VALIDATION FOR ".                             SM1074.2
012900   02  FILLER                        PIC X(58)  VALUE             SM1074.2
013000     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SM1074.2
013100 01  CCVS-H-3.                                                    SM1074.2
013200     02  FILLER                      PIC X(34)  VALUE             SM1074.2
013300            " FOR OFFICIAL USE ONLY    ".                         SM1074.2
013400     02  FILLER                      PIC X(58)  VALUE             SM1074.2
013500     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SM1074.2
013600     02  FILLER                      PIC X(28)  VALUE             SM1074.2
013700            "  COPYRIGHT   1985 ".                                SM1074.2
013800 01  CCVS-E-1.                                                    SM1074.2
013900     02 FILLER                       PIC X(52)  VALUE SPACE.      SM1074.2
014000     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              SM1074.2
014100     02 ID-AGAIN                     PIC X(9).                    SM1074.2
014200     02 FILLER                       PIC X(45)  VALUE SPACES.     SM1074.2
014300 01  CCVS-E-2.                                                    SM1074.2
014400     02  FILLER                      PIC X(31)  VALUE SPACE.      SM1074.2
014500     02  FILLER                      PIC X(21)  VALUE SPACE.      SM1074.2
014600     02 CCVS-E-2-2.                                               SM1074.2
014700         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      SM1074.2
014800         03 FILLER                   PIC X      VALUE SPACE.      SM1074.2
014900         03 ENDER-DESC               PIC X(44)  VALUE             SM1074.2
015000            "ERRORS ENCOUNTERED".                                 SM1074.2
015100 01  CCVS-E-3.                                                    SM1074.2
015200     02  FILLER                      PIC X(22)  VALUE             SM1074.2
015300            " FOR OFFICIAL USE ONLY".                             SM1074.2
015400     02  FILLER                      PIC X(12)  VALUE SPACE.      SM1074.2
015500     02  FILLER                      PIC X(58)  VALUE             SM1074.2
015600     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SM1074.2
015700     02  FILLER                      PIC X(13)  VALUE SPACE.      SM1074.2
015800     02 FILLER                       PIC X(15)  VALUE             SM1074.2
015900             " COPYRIGHT 1985".                                   SM1074.2
016000 01  CCVS-E-4.                                                    SM1074.2
016100     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      SM1074.2
016200     02 FILLER                       PIC X(4)   VALUE " OF ".     SM1074.2
016300     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      SM1074.2
016400     02 FILLER                       PIC X(40)  VALUE             SM1074.2
016500      "  TESTS WERE EXECUTED SUCCESSFULLY".                       SM1074.2
016600 01  XXINFO.                                                      SM1074.2
016700     02 FILLER                       PIC X(19)  VALUE             SM1074.2
016800            "*** INFORMATION ***".                                SM1074.2
016900     02 INFO-TEXT.                                                SM1074.2
017000       04 FILLER                     PIC X(8)   VALUE SPACE.      SM1074.2
017100       04 XXCOMPUTED                 PIC X(20).                   SM1074.2
017200       04 FILLER                     PIC X(5)   VALUE SPACE.      SM1074.2
017300       04 XXCORRECT                  PIC X(20).                   SM1074.2
017400     02 INF-ANSI-REFERENCE           PIC X(48).                   SM1074.2
017500 01  HYPHEN-LINE.                                                 SM1074.2
017600     02 FILLER  PIC IS X VALUE IS SPACE.                          SM1074.2
017700     02 FILLER  PIC IS X(65)    VALUE IS "************************SM1074.2
017800-    "*****************************************".                 SM1074.2
017900     02 FILLER  PIC IS X(54)    VALUE IS "************************SM1074.2
018000-    "******************************".                            SM1074.2
018100 01  CCVS-PGM-ID                     PIC X(9)   VALUE             SM1074.2
018200     "SM107A".                                                    SM1074.2
018300 PROCEDURE DIVISION.                                              SM1074.2
018400 CCVS1 SECTION.                                                   SM1074.2
018500 OPEN-FILES.                                                      SM1074.2
018600     OPEN     OUTPUT PRINT-FILE.                                  SM1074.2
018700     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   SM1074.2
018800     MOVE    SPACE TO TEST-RESULTS.                               SM1074.2
018900     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             SM1074.2
019000     GO TO CCVS1-EXIT.                                            SM1074.2
019100 CLOSE-FILES.                                                     SM1074.2
019200     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   SM1074.2
019300 TERMINATE-CCVS.                                                  SM1074.2
019400*    EXIT PROGRAM.                                                SM1074.2
019500*TERMINATE-CALL.                                                  SM1074.2
019600     STOP     RUN.                                                SM1074.2
019700 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         SM1074.2
019800 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           SM1074.2
019900 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          SM1074.2
020000 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      SM1074.2
020100     MOVE "****TEST DELETED****" TO RE-MARK.                      SM1074.2
020200 PRINT-DETAIL.                                                    SM1074.2
020300     IF REC-CT NOT EQUAL TO ZERO                                  SM1074.2
020400             MOVE "." TO PARDOT-X                                 SM1074.2
020500             MOVE REC-CT TO DOTVALUE.                             SM1074.2
020600     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      SM1074.2
020700     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               SM1074.2
020800        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 SM1074.2
020900          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 SM1074.2
021000     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              SM1074.2
021100     MOVE SPACE TO CORRECT-X.                                     SM1074.2
021200     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         SM1074.2
021300     MOVE     SPACE TO RE-MARK.                                   SM1074.2
021400 HEAD-ROUTINE.                                                    SM1074.2
021500     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  SM1074.2
021600     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  SM1074.2
021700     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  SM1074.2
021800     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  SM1074.2
021900 COLUMN-NAMES-ROUTINE.                                            SM1074.2
022000     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SM1074.2
022100     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SM1074.2
022200     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        SM1074.2
022300 END-ROUTINE.                                                     SM1074.2
022400     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.SM1074.2
022500 END-RTN-EXIT.                                                    SM1074.2
022600     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SM1074.2
022700 END-ROUTINE-1.                                                   SM1074.2
022800      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      SM1074.2
022900      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               SM1074.2
023000      ADD PASS-COUNTER TO ERROR-HOLD.                             SM1074.2
023100*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   SM1074.2
023200      MOVE PASS-COUNTER TO CCVS-E-4-1.                            SM1074.2
023300      MOVE ERROR-HOLD TO CCVS-E-4-2.                              SM1074.2
023400      MOVE CCVS-E-4 TO CCVS-E-2-2.                                SM1074.2
023500      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           SM1074.2
023600  END-ROUTINE-12.                                                 SM1074.2
023700      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        SM1074.2
023800     IF       ERROR-COUNTER IS EQUAL TO ZERO                      SM1074.2
023900         MOVE "NO " TO ERROR-TOTAL                                SM1074.2
024000         ELSE                                                     SM1074.2
024100         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       SM1074.2
024200     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           SM1074.2
024300     PERFORM WRITE-LINE.                                          SM1074.2
024400 END-ROUTINE-13.                                                  SM1074.2
024500     IF DELETE-COUNTER IS EQUAL TO ZERO                           SM1074.2
024600         MOVE "NO " TO ERROR-TOTAL  ELSE                          SM1074.2
024700         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      SM1074.2
024800     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   SM1074.2
024900     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SM1074.2
025000      IF   INSPECT-COUNTER EQUAL TO ZERO                          SM1074.2
025100          MOVE "NO " TO ERROR-TOTAL                               SM1074.2
025200      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   SM1074.2
025300      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            SM1074.2
025400      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          SM1074.2
025500     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SM1074.2
025600 WRITE-LINE.                                                      SM1074.2
025700     ADD 1 TO RECORD-COUNT.                                       SM1074.2
025800*    IF RECORD-COUNT GREATER 50                                   SM1074.2
025900*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          SM1074.2
026000*        MOVE SPACE TO DUMMY-RECORD                               SM1074.2
026100*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  SM1074.2
026200*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             SM1074.2
026300*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     SM1074.2
026400*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          SM1074.2
026500*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          SM1074.2
026600*        MOVE ZERO TO RECORD-COUNT.                               SM1074.2
026700     PERFORM WRT-LN.                                              SM1074.2
026800 WRT-LN.                                                          SM1074.2
026900     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               SM1074.2
027000     MOVE SPACE TO DUMMY-RECORD.                                  SM1074.2
027100 BLANK-LINE-PRINT.                                                SM1074.2
027200     PERFORM WRT-LN.                                              SM1074.2
027300 FAIL-ROUTINE.                                                    SM1074.2
027400     IF   COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE. SM1074.2
027500     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.SM1074.2
027600     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 SM1074.2
027700     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   SM1074.2
027800     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SM1074.2
027900     MOVE   SPACES TO INF-ANSI-REFERENCE.                         SM1074.2
028000     GO TO  FAIL-ROUTINE-EX.                                      SM1074.2
028100 FAIL-ROUTINE-WRITE.                                              SM1074.2
028200     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         SM1074.2
028300     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 SM1074.2
028400     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. SM1074.2
028500     MOVE   SPACES TO COR-ANSI-REFERENCE.                         SM1074.2
028600 FAIL-ROUTINE-EX. EXIT.                                           SM1074.2
028700 BAIL-OUT.                                                        SM1074.2
028800     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   SM1074.2
028900     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           SM1074.2
029000 BAIL-OUT-WRITE.                                                  SM1074.2
029100     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  SM1074.2
029200     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 SM1074.2
029300     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SM1074.2
029400     MOVE   SPACES TO INF-ANSI-REFERENCE.                         SM1074.2
029500 BAIL-OUT-EX. EXIT.                                               SM1074.2
029600 CCVS1-EXIT.                                                      SM1074.2
029700     EXIT.                                                        SM1074.2
029800 WARNING-MSG.                                                     SM1074.2
029900     MOVE " IF NO OTHER REPORT LINES APPEAR BELOW, ""COPY K7SEA"" SM1074.2
030000-         "FAILED." TO PRINT-REC.                                 SM1074.2
030100     PERFORM WRITE-LINE.                                          SM1074.2
030200*                                                                 SM1074.2
030300*********************** COPY STATEMENT USED **********************SM1074.2
030400*                                                                 SM1074.2
030500*COPY  K7SEA                                                      SM1074.2
030600*                                                                 SM1074.2
030700******************** COPIED TEXT BEGINS BELOW ********************SM1074.2
030800  COPY  K7SEA.                                                    SM1074.2
030900*********************** END OF COPIED TEXT ***********************SM1074.2
031000 CCVS-EXIT SECTION.                                               SM1074.2
031100 CCVS-999999.                                                     SM1074.2
031200     GO TO CLOSE-FILES.                                           SM1074.2
      *END-OF,SM107A                                                                  
