      *HEADER,COBOL,IC209A                                                            
000100 IDENTIFICATION DIVISION.                                         IC2094.2
000200 PROGRAM-ID.                                                      IC2094.2
000300     IC209A.                                                      IC2094.2
000400****************************************************************  IC2094.2
000500*                                                              *  IC2094.2
000600*    VALIDATION FOR:-                                          *  IC2094.2
000700*                                                              *  IC2094.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IC2094.2
000900*                                                              *  IC2094.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IC2094.2
001100*                                                              *  IC2094.2
001200****************************************************************  IC2094.2
001300*                                                              *  IC2094.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  IC2094.2
001500*                                                              *  IC2094.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  IC2094.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  IC2094.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  IC2094.2
001900*                                                              *  IC2094.2
002000****************************************************************  IC2094.2
002100*    THIS IS MAIN PROGRAM IC209.                                  IC2094.2
002200****************************************************************  IC2094.2
002300 ENVIRONMENT DIVISION.                                            IC2094.2
002400 CONFIGURATION SECTION.                                           IC2094.2
002500 SOURCE-COMPUTER.                                                 IC2094.2
002600     XXXXX082.                                                    IC2094.2
002700 OBJECT-COMPUTER.                                                 IC2094.2
002800     XXXXX083.                                                    IC2094.2
002900 INPUT-OUTPUT SECTION.                                            IC2094.2
003000 FILE-CONTROL.                                                    IC2094.2
003100     SELECT PRINT-FILE ASSIGN TO                                  IC2094.2
003200     XXXXX055.                                                    IC2094.2
003300 DATA DIVISION.                                                   IC2094.2
003400 FILE SECTION.                                                    IC2094.2
003500 FD  PRINT-FILE.                                                  IC2094.2
003600 01  PRINT-REC PICTURE X(120).                                    IC2094.2
003700 01  DUMMY-RECORD PICTURE X(120).                                 IC2094.2
003800 WORKING-STORAGE SECTION.                                         IC2094.2
003900 01  TEST-AREA.                                                   IC2094.2
004000     02  TEST1                   PICTURE X.                       IC2094.2
004100     02  TEST2                   PICTURE X.                       IC2094.2
004200     02  TEST3                   PICTURE X.                       IC2094.2
004300     02  TEST4                   PICTURE X.                       IC2094.2
004400 01  TEST-RESULTS.                                                IC2094.2
004500     02 FILLER                   PIC X      VALUE SPACE.          IC2094.2
004600     02 FEATURE                  PIC X(20)  VALUE SPACE.          IC2094.2
004700     02 FILLER                   PIC X      VALUE SPACE.          IC2094.2
004800     02 P-OR-F                   PIC X(5)   VALUE SPACE.          IC2094.2
004900     02 FILLER                   PIC X      VALUE SPACE.          IC2094.2
005000     02  PAR-NAME.                                                IC2094.2
005100       03 FILLER                 PIC X(19)  VALUE SPACE.          IC2094.2
005200       03  PARDOT-X              PIC X      VALUE SPACE.          IC2094.2
005300       03 DOTVALUE               PIC 99     VALUE ZERO.           IC2094.2
005400     02 FILLER                   PIC X(8)   VALUE SPACE.          IC2094.2
005500     02 RE-MARK                  PIC X(61).                       IC2094.2
005600 01  TEST-COMPUTED.                                               IC2094.2
005700     02 FILLER                   PIC X(30)  VALUE SPACE.          IC2094.2
005800     02 FILLER                   PIC X(17)  VALUE                 IC2094.2
005900            "       COMPUTED=".                                   IC2094.2
006000     02 COMPUTED-X.                                               IC2094.2
006100     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          IC2094.2
006200     03 COMPUTED-N               REDEFINES COMPUTED-A             IC2094.2
006300                                 PIC -9(9).9(9).                  IC2094.2
006400     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         IC2094.2
006500     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     IC2094.2
006600     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     IC2094.2
006700     03       CM-18V0 REDEFINES COMPUTED-A.                       IC2094.2
006800         04 COMPUTED-18V0                    PIC -9(18).          IC2094.2
006900         04 FILLER                           PIC X.               IC2094.2
007000     03 FILLER PIC X(50) VALUE SPACE.                             IC2094.2
007100 01  TEST-CORRECT.                                                IC2094.2
007200     02 FILLER PIC X(30) VALUE SPACE.                             IC2094.2
007300     02 FILLER PIC X(17) VALUE "       CORRECT =".                IC2094.2
007400     02 CORRECT-X.                                                IC2094.2
007500     03 CORRECT-A                  PIC X(20) VALUE SPACE.         IC2094.2
007600     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      IC2094.2
007700     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         IC2094.2
007800     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     IC2094.2
007900     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     IC2094.2
008000     03      CR-18V0 REDEFINES CORRECT-A.                         IC2094.2
008100         04 CORRECT-18V0                     PIC -9(18).          IC2094.2
008200         04 FILLER                           PIC X.               IC2094.2
008300     03 FILLER PIC X(2) VALUE SPACE.                              IC2094.2
008400     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     IC2094.2
008500 01  CCVS-C-1.                                                    IC2094.2
008600     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAIC2094.2
008700-    "SS  PARAGRAPH-NAME                                          IC2094.2
008800-    "       REMARKS".                                            IC2094.2
008900     02 FILLER                     PIC X(20)    VALUE SPACE.      IC2094.2
009000 01  CCVS-C-2.                                                    IC2094.2
009100     02 FILLER                     PIC X        VALUE SPACE.      IC2094.2
009200     02 FILLER                     PIC X(6)     VALUE "TESTED".   IC2094.2
009300     02 FILLER                     PIC X(15)    VALUE SPACE.      IC2094.2
009400     02 FILLER                     PIC X(4)     VALUE "FAIL".     IC2094.2
009500     02 FILLER                     PIC X(94)    VALUE SPACE.      IC2094.2
009600 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       IC2094.2
009700 01  REC-CT                        PIC 99       VALUE ZERO.       IC2094.2
009800 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       IC2094.2
009900 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       IC2094.2
010000 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       IC2094.2
010100 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       IC2094.2
010200 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       IC2094.2
010300 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       IC2094.2
010400 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      IC2094.2
010500 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       IC2094.2
010600 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     IC2094.2
010700 01  CCVS-H-1.                                                    IC2094.2
010800     02  FILLER                    PIC X(39)    VALUE SPACES.     IC2094.2
010900     02  FILLER                    PIC X(42)    VALUE             IC2094.2
011000     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 IC2094.2
011100     02  FILLER                    PIC X(39)    VALUE SPACES.     IC2094.2
011200 01  CCVS-H-2A.                                                   IC2094.2
011300   02  FILLER                        PIC X(40)  VALUE SPACE.      IC2094.2
011400   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  IC2094.2
011500   02  FILLER                        PIC XXXX   VALUE             IC2094.2
011600     "4.2 ".                                                      IC2094.2
011700   02  FILLER                        PIC X(28)  VALUE             IC2094.2
011800            " COPY - NOT FOR DISTRIBUTION".                       IC2094.2
011900   02  FILLER                        PIC X(41)  VALUE SPACE.      IC2094.2
012000                                                                  IC2094.2
012100 01  CCVS-H-2B.                                                   IC2094.2
012200   02  FILLER                        PIC X(15)  VALUE             IC2094.2
012300            "TEST RESULT OF ".                                    IC2094.2
012400   02  TEST-ID                       PIC X(9).                    IC2094.2
012500   02  FILLER                        PIC X(4)   VALUE             IC2094.2
012600            " IN ".                                               IC2094.2
012700   02  FILLER                        PIC X(12)  VALUE             IC2094.2
012800     " HIGH       ".                                              IC2094.2
012900   02  FILLER                        PIC X(22)  VALUE             IC2094.2
013000            " LEVEL VALIDATION FOR ".                             IC2094.2
013100   02  FILLER                        PIC X(58)  VALUE             IC2094.2
013200     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IC2094.2
013300 01  CCVS-H-3.                                                    IC2094.2
013400     02  FILLER                      PIC X(34)  VALUE             IC2094.2
013500            " FOR OFFICIAL USE ONLY    ".                         IC2094.2
013600     02  FILLER                      PIC X(58)  VALUE             IC2094.2
013700     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IC2094.2
013800     02  FILLER                      PIC X(28)  VALUE             IC2094.2
013900            "  COPYRIGHT   1985 ".                                IC2094.2
014000 01  CCVS-E-1.                                                    IC2094.2
014100     02 FILLER                       PIC X(52)  VALUE SPACE.      IC2094.2
014200     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              IC2094.2
014300     02 ID-AGAIN                     PIC X(9).                    IC2094.2
014400     02 FILLER                       PIC X(45)  VALUE SPACES.     IC2094.2
014500 01  CCVS-E-2.                                                    IC2094.2
014600     02  FILLER                      PIC X(31)  VALUE SPACE.      IC2094.2
014700     02  FILLER                      PIC X(21)  VALUE SPACE.      IC2094.2
014800     02 CCVS-E-2-2.                                               IC2094.2
014900         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      IC2094.2
015000         03 FILLER                   PIC X      VALUE SPACE.      IC2094.2
015100         03 ENDER-DESC               PIC X(44)  VALUE             IC2094.2
015200            "ERRORS ENCOUNTERED".                                 IC2094.2
015300 01  CCVS-E-3.                                                    IC2094.2
015400     02  FILLER                      PIC X(22)  VALUE             IC2094.2
015500            " FOR OFFICIAL USE ONLY".                             IC2094.2
015600     02  FILLER                      PIC X(12)  VALUE SPACE.      IC2094.2
015700     02  FILLER                      PIC X(58)  VALUE             IC2094.2
015800     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IC2094.2
015900     02  FILLER                      PIC X(13)  VALUE SPACE.      IC2094.2
016000     02 FILLER                       PIC X(15)  VALUE             IC2094.2
016100             " COPYRIGHT 1985".                                   IC2094.2
016200 01  CCVS-E-4.                                                    IC2094.2
016300     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      IC2094.2
016400     02 FILLER                       PIC X(4)   VALUE " OF ".     IC2094.2
016500     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      IC2094.2
016600     02 FILLER                       PIC X(40)  VALUE             IC2094.2
016700      "  TESTS WERE EXECUTED SUCCESSFULLY".                       IC2094.2
016800 01  XXINFO.                                                      IC2094.2
016900     02 FILLER                       PIC X(19)  VALUE             IC2094.2
017000            "*** INFORMATION ***".                                IC2094.2
017100     02 INFO-TEXT.                                                IC2094.2
017200       04 FILLER                     PIC X(8)   VALUE SPACE.      IC2094.2
017300       04 XXCOMPUTED                 PIC X(20).                   IC2094.2
017400       04 FILLER                     PIC X(5)   VALUE SPACE.      IC2094.2
017500       04 XXCORRECT                  PIC X(20).                   IC2094.2
017600     02 INF-ANSI-REFERENCE           PIC X(48).                   IC2094.2
017700 01  HYPHEN-LINE.                                                 IC2094.2
017800     02 FILLER  PIC IS X VALUE IS SPACE.                          IC2094.2
017900     02 FILLER  PIC IS X(65)    VALUE IS "************************IC2094.2
018000-    "*****************************************".                 IC2094.2
018100     02 FILLER  PIC IS X(54)    VALUE IS "************************IC2094.2
018200-    "******************************".                            IC2094.2
018300 01  CCVS-PGM-ID                     PIC X(9)   VALUE             IC2094.2
018400     "IC209A".                                                    IC2094.2
018500 PROCEDURE DIVISION.                                              IC2094.2
018600 CCVS1 SECTION.                                                   IC2094.2
018700 OPEN-FILES.                                                      IC2094.2
018800     OPEN     OUTPUT PRINT-FILE.                                  IC2094.2
018900     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   IC2094.2
019000     MOVE    SPACE TO TEST-RESULTS.                               IC2094.2
019100     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             IC2094.2
019200     GO TO CCVS1-EXIT.                                            IC2094.2
019300 CLOSE-FILES.                                                     IC2094.2
019400     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   IC2094.2
019500 TERMINATE-CCVS.                                                  IC2094.2
019600*    EXIT PROGRAM.                                                IC2094.2
019700*TERMINATE-CALL.                                                  IC2094.2
019800     STOP     RUN.                                                IC2094.2
019900 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         IC2094.2
020000 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           IC2094.2
020100 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          IC2094.2
020200 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      IC2094.2
020300     MOVE "****TEST DELETED****" TO RE-MARK.                      IC2094.2
020400 PRINT-DETAIL.                                                    IC2094.2
020500     IF REC-CT NOT EQUAL TO ZERO                                  IC2094.2
020600             MOVE "." TO PARDOT-X                                 IC2094.2
020700             MOVE REC-CT TO DOTVALUE.                             IC2094.2
020800     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      IC2094.2
020900     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               IC2094.2
021000        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 IC2094.2
021100          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 IC2094.2
021200     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              IC2094.2
021300     MOVE SPACE TO CORRECT-X.                                     IC2094.2
021400     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         IC2094.2
021500     MOVE     SPACE TO RE-MARK.                                   IC2094.2
021600 HEAD-ROUTINE.                                                    IC2094.2
021700     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IC2094.2
021800     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IC2094.2
021900     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IC2094.2
022000     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IC2094.2
022100 COLUMN-NAMES-ROUTINE.                                            IC2094.2
022200     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IC2094.2
022300     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IC2094.2
022400     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        IC2094.2
022500 END-ROUTINE.                                                     IC2094.2
022600     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.IC2094.2
022700 END-RTN-EXIT.                                                    IC2094.2
022800     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IC2094.2
022900 END-ROUTINE-1.                                                   IC2094.2
023000      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      IC2094.2
023100      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               IC2094.2
023200      ADD PASS-COUNTER TO ERROR-HOLD.                             IC2094.2
023300*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   IC2094.2
023400      MOVE PASS-COUNTER TO CCVS-E-4-1.                            IC2094.2
023500      MOVE ERROR-HOLD TO CCVS-E-4-2.                              IC2094.2
023600      MOVE CCVS-E-4 TO CCVS-E-2-2.                                IC2094.2
023700      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           IC2094.2
023800  END-ROUTINE-12.                                                 IC2094.2
023900      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        IC2094.2
024000     IF       ERROR-COUNTER IS EQUAL TO ZERO                      IC2094.2
024100         MOVE "NO " TO ERROR-TOTAL                                IC2094.2
024200         ELSE                                                     IC2094.2
024300         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       IC2094.2
024400     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           IC2094.2
024500     PERFORM WRITE-LINE.                                          IC2094.2
024600 END-ROUTINE-13.                                                  IC2094.2
024700     IF DELETE-COUNTER IS EQUAL TO ZERO                           IC2094.2
024800         MOVE "NO " TO ERROR-TOTAL  ELSE                          IC2094.2
024900         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      IC2094.2
025000     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   IC2094.2
025100     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IC2094.2
025200      IF   INSPECT-COUNTER EQUAL TO ZERO                          IC2094.2
025300          MOVE "NO " TO ERROR-TOTAL                               IC2094.2
025400      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   IC2094.2
025500      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            IC2094.2
025600      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          IC2094.2
025700     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IC2094.2
025800 WRITE-LINE.                                                      IC2094.2
025900     ADD 1 TO RECORD-COUNT.                                       IC2094.2
026000*    IF RECORD-COUNT GREATER 50                                   IC2094.2
026100*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          IC2094.2
026200*        MOVE SPACE TO DUMMY-RECORD                               IC2094.2
026300*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  IC2094.2
026400*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             IC2094.2
026500*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     IC2094.2
026600*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          IC2094.2
026700*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          IC2094.2
026800*        MOVE ZERO TO RECORD-COUNT.                               IC2094.2
026900     PERFORM WRT-LN.                                              IC2094.2
027000 WRT-LN.                                                          IC2094.2
027100     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               IC2094.2
027200     MOVE SPACE TO DUMMY-RECORD.                                  IC2094.2
027300 BLANK-LINE-PRINT.                                                IC2094.2
027400     PERFORM WRT-LN.                                              IC2094.2
027500 FAIL-ROUTINE.                                                    IC2094.2
027600     IF   COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE. IC2094.2
027700     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.IC2094.2
027800     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IC2094.2
027900     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   IC2094.2
028000     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IC2094.2
028100     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IC2094.2
028200     GO TO  FAIL-ROUTINE-EX.                                      IC2094.2
028300 FAIL-ROUTINE-WRITE.                                              IC2094.2
028400     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         IC2094.2
028500     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 IC2094.2
028600     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. IC2094.2
028700     MOVE   SPACES TO COR-ANSI-REFERENCE.                         IC2094.2
028800 FAIL-ROUTINE-EX. EXIT.                                           IC2094.2
028900 BAIL-OUT.                                                        IC2094.2
029000     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   IC2094.2
029100     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           IC2094.2
029200 BAIL-OUT-WRITE.                                                  IC2094.2
029300     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  IC2094.2
029400     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IC2094.2
029500     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IC2094.2
029600     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IC2094.2
029700 BAIL-OUT-EX. EXIT.                                               IC2094.2
029800 CCVS1-EXIT.                                                      IC2094.2
029900     EXIT.                                                        IC2094.2
030000 CALL-TEST-1.                                                     IC2094.2
030100     MOVE SPACES TO TEST-AREA.                                    IC2094.2
030200     CALL "IC210A" USING TEST-AREA.                               IC2094.2
030300     MOVE "CALL...USING DATA-NM" TO FEATURE.                      IC2094.2
030400     MOVE "CALL-TEST-1" TO PAR-NAME.                              IC2094.2
030500     MOVE "MAIN PROGRAM CALLS SUBPROGRAM1" TO RE-MARK.            IC2094.2
030600     IF TEST1 = "Y" PERFORM PASS                                  IC2094.2
030700        GO TO CALL-WRITE-1.                                       IC2094.2
030800 CALL-FAIL-1.                                                     IC2094.2
030900     MOVE TEST1 TO COMPUTED-A.                                    IC2094.2
031000     MOVE "Y" TO CORRECT-A.                                       IC2094.2
031100     PERFORM FAIL.                                                IC2094.2
031200 CALL-WRITE-1.                                                    IC2094.2
031300     PERFORM PRINT-DETAIL.                                        IC2094.2
031400 CALL-TEST-2.                                                     IC2094.2
031500     MOVE "CALL...USING DATA-NM" TO FEATURE.                      IC2094.2
031600     MOVE "CALL-TEST-2" TO PAR-NAME.                              IC2094.2
031700     MOVE "SUBPROGRAM1 CALLS SUBPROGRAM2" TO RE-MARK.             IC2094.2
031800     IF TEST2 = "Y" PERFORM PASS                                  IC2094.2
031900        GO TO CALL-WRITE-2.                                       IC2094.2
032000 CALL-FAIL-2.                                                     IC2094.2
032100     MOVE TEST2 TO COMPUTED-A.                                    IC2094.2
032200     MOVE "Y" TO CORRECT-A.                                       IC2094.2
032300     PERFORM FAIL.                                                IC2094.2
032400 CALL-WRITE-2.                                                    IC2094.2
032500     PERFORM PRINT-DETAIL.                                        IC2094.2
032600 CALL-TEST-3.                                                     IC2094.2
032700     MOVE "CALL...USING DATA-NM" TO FEATURE.                      IC2094.2
032800     MOVE "CALL-TEST-3" TO PAR-NAME.                              IC2094.2
032900     MOVE "SUBPROGRAM1 CALLS SUBPROGRAM3" TO RE-MARK.             IC2094.2
033000     IF TEST3 = "Y" PERFORM PASS                                  IC2094.2
033100        GO TO CALL-WRITE-3.                                       IC2094.2
033200 CALL-FAIL-3.                                                     IC2094.2
033300     MOVE TEST3 TO COMPUTED-A.                                    IC2094.2
033400     MOVE "Y" TO CORRECT-A.                                       IC2094.2
033500     PERFORM FAIL.                                                IC2094.2
033600 CALL-WRITE-3.                                                    IC2094.2
033700     PERFORM PRINT-DETAIL.                                        IC2094.2
033800 CANCEL-TEST-1.                                                   IC2094.2
033900     MOVE "CANCEL" TO FEATURE.                                    IC2094.2
034000     MOVE "CANCEL-TEST-1" TO PAR-NAME.                            IC2094.2
034100     MOVE "SUBPROGRAM1 CANCELS SUBPROGRAM2" TO RE-MARK.           IC2094.2
034200     IF TEST4 = "Y" PERFORM PASS                                  IC2094.2
034300        GO TO CANCEL-WRITE-1.                                     IC2094.2
034400 CANCEL-FAIL-1.                                                   IC2094.2
034500     MOVE TEST4 TO COMPUTED-A.                                    IC2094.2
034600     MOVE "Y" TO CORRECT-A.                                       IC2094.2
034700     PERFORM FAIL.                                                IC2094.2
034800 CANCEL-WRITE-1.                                                  IC2094.2
034900     PERFORM PRINT-DETAIL.                                        IC2094.2
035000 CCVS-EXIT SECTION.                                               IC2094.2
035100 CCVS-999999.                                                     IC2094.2
035200     GO TO CLOSE-FILES.                                           IC2094.2
      *END-OF,IC209A                                                                  
