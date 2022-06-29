      *HEADER,COBOL,IC101A                                                            
000100 IDENTIFICATION DIVISION.                                         IC1014.2
000200 PROGRAM-ID.                                                      IC1014.2
000300     IC101A.                                                      IC1014.2
000400****************************************************************  IC1014.2
000500*                                                              *  IC1014.2
000600*    VALIDATION FOR:-                                          *  IC1014.2
000700*                                                              *  IC1014.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IC1014.2
000900*                                                              *  IC1014.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IC1014.2
001100*                                                              *  IC1014.2
001200****************************************************************  IC1014.2
001300*                                                              *  IC1014.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  IC1014.2
001500*                                                              *  IC1014.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  IC1014.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  IC1014.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  IC1014.2
001900*                                                              *  IC1014.2
002000****************************************************************  IC1014.2
002100*         THIS ROUTINE CHECKS THE USE OF THE CALL STATEMENT       IC1014.2
002200*    WITH ONE PARAMETER IN THE USING PHRASE.  SUBSEQUENT CALLS    IC1014.2
002300*    CHECK THAT THE CALLED ROUTINE REMAINS IN THE LAST USED STATE.IC1014.2
002400*                                                                 IC1014.2
002500*        THERE ARE NO DELETE PARAGRAPHS IN THIS ROUTINE           IC1014.2
002600*    SINCE THESE ARE THE BASIC CALL TESTS AND IF A CALL           IC1014.2
002700*    STATEMENT IS REJECTED THERE IS NO REASON TO RUN THE ROUTINE. IC1014.2
002800*                                                                 IC1014.2
002900*    THE FIRST THREE CALLS USE A DATA-NAME THE SAME AS            IC1014.2
003000*    THE NAME IN THE SUBPROGRAM.  THE LAST TWO CALLS USE          IC1014.2
003100*    A DIFFERENT DATA-NAME FROM THE NAME IN THE SUBPROGRAM.       IC1014.2
003200*    THE PICTURE CLAUSES FOR DATA-NAMES IN THE USING PHRASES      IC1014.2
003300*    OF THE CALLED AND CALLING PROGRAMS ARE IDENTICAL.            IC1014.2
003400 ENVIRONMENT DIVISION.                                            IC1014.2
003500 CONFIGURATION SECTION.                                           IC1014.2
003600 SOURCE-COMPUTER.                                                 IC1014.2
003700     XXXXX082.                                                    IC1014.2
003800 OBJECT-COMPUTER.                                                 IC1014.2
003900     XXXXX083.                                                    IC1014.2
004000 INPUT-OUTPUT SECTION.                                            IC1014.2
004100 FILE-CONTROL.                                                    IC1014.2
004200     SELECT PRINT-FILE ASSIGN TO                                  IC1014.2
004300     XXXXX055.                                                    IC1014.2
004400 DATA DIVISION.                                                   IC1014.2
004500 FILE SECTION.                                                    IC1014.2
004600 FD  PRINT-FILE.                                                  IC1014.2
004700 01  PRINT-REC PICTURE X(120).                                    IC1014.2
004800 01  DUMMY-RECORD PICTURE X(120).                                 IC1014.2
004900 WORKING-STORAGE SECTION.                                         IC1014.2
005000 77  DN1 PICTURE S9  VALUE ZERO.                                  IC1014.2
005100 77  DN2 PICTURE S9  VALUE ZERO.                                  IC1014.2
005200 01  TEST-RESULTS.                                                IC1014.2
005300     02 FILLER                   PIC X      VALUE SPACE.          IC1014.2
005400     02 FEATURE                  PIC X(20)  VALUE SPACE.          IC1014.2
005500     02 FILLER                   PIC X      VALUE SPACE.          IC1014.2
005600     02 P-OR-F                   PIC X(5)   VALUE SPACE.          IC1014.2
005700     02 FILLER                   PIC X      VALUE SPACE.          IC1014.2
005800     02  PAR-NAME.                                                IC1014.2
005900       03 FILLER                 PIC X(19)  VALUE SPACE.          IC1014.2
006000       03  PARDOT-X              PIC X      VALUE SPACE.          IC1014.2
006100       03 DOTVALUE               PIC 99     VALUE ZERO.           IC1014.2
006200     02 FILLER                   PIC X(8)   VALUE SPACE.          IC1014.2
006300     02 RE-MARK                  PIC X(61).                       IC1014.2
006400 01  TEST-COMPUTED.                                               IC1014.2
006500     02 FILLER                   PIC X(30)  VALUE SPACE.          IC1014.2
006600     02 FILLER                   PIC X(17)  VALUE                 IC1014.2
006700            "       COMPUTED=".                                   IC1014.2
006800     02 COMPUTED-X.                                               IC1014.2
006900     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          IC1014.2
007000     03 COMPUTED-N               REDEFINES COMPUTED-A             IC1014.2
007100                                 PIC -9(9).9(9).                  IC1014.2
007200     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         IC1014.2
007300     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     IC1014.2
007400     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     IC1014.2
007500     03       CM-18V0 REDEFINES COMPUTED-A.                       IC1014.2
007600         04 COMPUTED-18V0                    PIC -9(18).          IC1014.2
007700         04 FILLER                           PIC X.               IC1014.2
007800     03 FILLER PIC X(50) VALUE SPACE.                             IC1014.2
007900 01  TEST-CORRECT.                                                IC1014.2
008000     02 FILLER PIC X(30) VALUE SPACE.                             IC1014.2
008100     02 FILLER PIC X(17) VALUE "       CORRECT =".                IC1014.2
008200     02 CORRECT-X.                                                IC1014.2
008300     03 CORRECT-A                  PIC X(20) VALUE SPACE.         IC1014.2
008400     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      IC1014.2
008500     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         IC1014.2
008600     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     IC1014.2
008700     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     IC1014.2
008800     03      CR-18V0 REDEFINES CORRECT-A.                         IC1014.2
008900         04 CORRECT-18V0                     PIC -9(18).          IC1014.2
009000         04 FILLER                           PIC X.               IC1014.2
009100     03 FILLER PIC X(2) VALUE SPACE.                              IC1014.2
009200     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     IC1014.2
009300 01  CCVS-C-1.                                                    IC1014.2
009400     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAIC1014.2
009500-    "SS  PARAGRAPH-NAME                                          IC1014.2
009600-    "       REMARKS".                                            IC1014.2
009700     02 FILLER                     PIC X(20)    VALUE SPACE.      IC1014.2
009800 01  CCVS-C-2.                                                    IC1014.2
009900     02 FILLER                     PIC X        VALUE SPACE.      IC1014.2
010000     02 FILLER                     PIC X(6)     VALUE "TESTED".   IC1014.2
010100     02 FILLER                     PIC X(15)    VALUE SPACE.      IC1014.2
010200     02 FILLER                     PIC X(4)     VALUE "FAIL".     IC1014.2
010300     02 FILLER                     PIC X(94)    VALUE SPACE.      IC1014.2
010400 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       IC1014.2
010500 01  REC-CT                        PIC 99       VALUE ZERO.       IC1014.2
010600 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       IC1014.2
010700 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       IC1014.2
010800 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       IC1014.2
010900 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       IC1014.2
011000 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       IC1014.2
011100 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       IC1014.2
011200 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      IC1014.2
011300 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       IC1014.2
011400 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     IC1014.2
011500 01  CCVS-H-1.                                                    IC1014.2
011600     02  FILLER                    PIC X(39)    VALUE SPACES.     IC1014.2
011700     02  FILLER                    PIC X(42)    VALUE             IC1014.2
011800     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 IC1014.2
011900     02  FILLER                    PIC X(39)    VALUE SPACES.     IC1014.2
012000 01  CCVS-H-2A.                                                   IC1014.2
012100   02  FILLER                        PIC X(40)  VALUE SPACE.      IC1014.2
012200   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  IC1014.2
012300   02  FILLER                        PIC XXXX   VALUE             IC1014.2
012400     "4.2 ".                                                      IC1014.2
012500   02  FILLER                        PIC X(28)  VALUE             IC1014.2
012600            " COPY - NOT FOR DISTRIBUTION".                       IC1014.2
012700   02  FILLER                        PIC X(41)  VALUE SPACE.      IC1014.2
012800                                                                  IC1014.2
012900 01  CCVS-H-2B.                                                   IC1014.2
013000   02  FILLER                        PIC X(15)  VALUE             IC1014.2
013100            "TEST RESULT OF ".                                    IC1014.2
013200   02  TEST-ID                       PIC X(9).                    IC1014.2
013300   02  FILLER                        PIC X(4)   VALUE             IC1014.2
013400            " IN ".                                               IC1014.2
013500   02  FILLER                        PIC X(12)  VALUE             IC1014.2
013600     " HIGH       ".                                              IC1014.2
013700   02  FILLER                        PIC X(22)  VALUE             IC1014.2
013800            " LEVEL VALIDATION FOR ".                             IC1014.2
013900   02  FILLER                        PIC X(58)  VALUE             IC1014.2
014000     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IC1014.2
014100 01  CCVS-H-3.                                                    IC1014.2
014200     02  FILLER                      PIC X(34)  VALUE             IC1014.2
014300            " FOR OFFICIAL USE ONLY    ".                         IC1014.2
014400     02  FILLER                      PIC X(58)  VALUE             IC1014.2
014500     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IC1014.2
014600     02  FILLER                      PIC X(28)  VALUE             IC1014.2
014700            "  COPYRIGHT   1985 ".                                IC1014.2
014800 01  CCVS-E-1.                                                    IC1014.2
014900     02 FILLER                       PIC X(52)  VALUE SPACE.      IC1014.2
015000     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              IC1014.2
015100     02 ID-AGAIN                     PIC X(9).                    IC1014.2
015200     02 FILLER                       PIC X(45)  VALUE SPACES.     IC1014.2
015300 01  CCVS-E-2.                                                    IC1014.2
015400     02  FILLER                      PIC X(31)  VALUE SPACE.      IC1014.2
015500     02  FILLER                      PIC X(21)  VALUE SPACE.      IC1014.2
015600     02 CCVS-E-2-2.                                               IC1014.2
015700         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      IC1014.2
015800         03 FILLER                   PIC X      VALUE SPACE.      IC1014.2
015900         03 ENDER-DESC               PIC X(44)  VALUE             IC1014.2
016000            "ERRORS ENCOUNTERED".                                 IC1014.2
016100 01  CCVS-E-3.                                                    IC1014.2
016200     02  FILLER                      PIC X(22)  VALUE             IC1014.2
016300            " FOR OFFICIAL USE ONLY".                             IC1014.2
016400     02  FILLER                      PIC X(12)  VALUE SPACE.      IC1014.2
016500     02  FILLER                      PIC X(58)  VALUE             IC1014.2
016600     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IC1014.2
016700     02  FILLER                      PIC X(13)  VALUE SPACE.      IC1014.2
016800     02 FILLER                       PIC X(15)  VALUE             IC1014.2
016900             " COPYRIGHT 1985".                                   IC1014.2
017000 01  CCVS-E-4.                                                    IC1014.2
017100     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      IC1014.2
017200     02 FILLER                       PIC X(4)   VALUE " OF ".     IC1014.2
017300     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      IC1014.2
017400     02 FILLER                       PIC X(40)  VALUE             IC1014.2
017500      "  TESTS WERE EXECUTED SUCCESSFULLY".                       IC1014.2
017600 01  XXINFO.                                                      IC1014.2
017700     02 FILLER                       PIC X(19)  VALUE             IC1014.2
017800            "*** INFORMATION ***".                                IC1014.2
017900     02 INFO-TEXT.                                                IC1014.2
018000       04 FILLER                     PIC X(8)   VALUE SPACE.      IC1014.2
018100       04 XXCOMPUTED                 PIC X(20).                   IC1014.2
018200       04 FILLER                     PIC X(5)   VALUE SPACE.      IC1014.2
018300       04 XXCORRECT                  PIC X(20).                   IC1014.2
018400     02 INF-ANSI-REFERENCE           PIC X(48).                   IC1014.2
018500 01  HYPHEN-LINE.                                                 IC1014.2
018600     02 FILLER  PIC IS X VALUE IS SPACE.                          IC1014.2
018700     02 FILLER  PIC IS X(65)    VALUE IS "************************IC1014.2
018800-    "*****************************************".                 IC1014.2
018900     02 FILLER  PIC IS X(54)    VALUE IS "************************IC1014.2
019000-    "******************************".                            IC1014.2
019100 01  CCVS-PGM-ID                     PIC X(9)   VALUE             IC1014.2
019200     "IC101A".                                                    IC1014.2
019300 PROCEDURE DIVISION.                                              IC1014.2
019400 CCVS1 SECTION.                                                   IC1014.2
019500 OPEN-FILES.                                                      IC1014.2
019600     OPEN     OUTPUT PRINT-FILE.                                  IC1014.2
019700     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   IC1014.2
019800     MOVE    SPACE TO TEST-RESULTS.                               IC1014.2
019900     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             IC1014.2
020000     GO TO CCVS1-EXIT.                                            IC1014.2
020100 CLOSE-FILES.                                                     IC1014.2
020200     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   IC1014.2
020300 TERMINATE-CCVS.                                                  IC1014.2
020400*    EXIT PROGRAM.                                                IC1014.2
020500*TERMINATE-CALL.                                                  IC1014.2
020600     STOP     RUN.                                                IC1014.2
020700 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         IC1014.2
020800 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           IC1014.2
020900 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          IC1014.2
021000 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      IC1014.2
021100     MOVE "****TEST DELETED****" TO RE-MARK.                      IC1014.2
021200 PRINT-DETAIL.                                                    IC1014.2
021300     IF REC-CT NOT EQUAL TO ZERO                                  IC1014.2
021400             MOVE "." TO PARDOT-X                                 IC1014.2
021500             MOVE REC-CT TO DOTVALUE.                             IC1014.2
021600     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      IC1014.2
021700     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               IC1014.2
021800        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 IC1014.2
021900          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 IC1014.2
022000     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              IC1014.2
022100     MOVE SPACE TO CORRECT-X.                                     IC1014.2
022200     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         IC1014.2
022300     MOVE     SPACE TO RE-MARK.                                   IC1014.2
022400 HEAD-ROUTINE.                                                    IC1014.2
022500     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IC1014.2
022600     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IC1014.2
022700     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IC1014.2
022800     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IC1014.2
022900 COLUMN-NAMES-ROUTINE.                                            IC1014.2
023000     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IC1014.2
023100     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IC1014.2
023200     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        IC1014.2
023300 END-ROUTINE.                                                     IC1014.2
023400     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.IC1014.2
023500 END-RTN-EXIT.                                                    IC1014.2
023600     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IC1014.2
023700 END-ROUTINE-1.                                                   IC1014.2
023800      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      IC1014.2
023900      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               IC1014.2
024000      ADD PASS-COUNTER TO ERROR-HOLD.                             IC1014.2
024100*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   IC1014.2
024200      MOVE PASS-COUNTER TO CCVS-E-4-1.                            IC1014.2
024300      MOVE ERROR-HOLD TO CCVS-E-4-2.                              IC1014.2
024400      MOVE CCVS-E-4 TO CCVS-E-2-2.                                IC1014.2
024500      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           IC1014.2
024600  END-ROUTINE-12.                                                 IC1014.2
024700      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        IC1014.2
024800     IF       ERROR-COUNTER IS EQUAL TO ZERO                      IC1014.2
024900         MOVE "NO " TO ERROR-TOTAL                                IC1014.2
025000         ELSE                                                     IC1014.2
025100         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       IC1014.2
025200     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           IC1014.2
025300     PERFORM WRITE-LINE.                                          IC1014.2
025400 END-ROUTINE-13.                                                  IC1014.2
025500     IF DELETE-COUNTER IS EQUAL TO ZERO                           IC1014.2
025600         MOVE "NO " TO ERROR-TOTAL  ELSE                          IC1014.2
025700         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      IC1014.2
025800     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   IC1014.2
025900     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IC1014.2
026000      IF   INSPECT-COUNTER EQUAL TO ZERO                          IC1014.2
026100          MOVE "NO " TO ERROR-TOTAL                               IC1014.2
026200      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   IC1014.2
026300      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            IC1014.2
026400      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          IC1014.2
026500     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IC1014.2
026600 WRITE-LINE.                                                      IC1014.2
026700     ADD 1 TO RECORD-COUNT.                                       IC1014.2
026800*    IF RECORD-COUNT GREATER 50                                   IC1014.2
026900*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          IC1014.2
027000*        MOVE SPACE TO DUMMY-RECORD                               IC1014.2
027100*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  IC1014.2
027200*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             IC1014.2
027300*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     IC1014.2
027400*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          IC1014.2
027500*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          IC1014.2
027600*        MOVE ZERO TO RECORD-COUNT.                               IC1014.2
027700     PERFORM WRT-LN.                                              IC1014.2
027800 WRT-LN.                                                          IC1014.2
027900     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               IC1014.2
028000     MOVE SPACE TO DUMMY-RECORD.                                  IC1014.2
028100 BLANK-LINE-PRINT.                                                IC1014.2
028200     PERFORM WRT-LN.                                              IC1014.2
028300 FAIL-ROUTINE.                                                    IC1014.2
028400     IF   COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE. IC1014.2
028500     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.IC1014.2
028600     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IC1014.2
028700     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   IC1014.2
028800     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IC1014.2
028900     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IC1014.2
029000     GO TO  FAIL-ROUTINE-EX.                                      IC1014.2
029100 FAIL-ROUTINE-WRITE.                                              IC1014.2
029200     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         IC1014.2
029300     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 IC1014.2
029400     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. IC1014.2
029500     MOVE   SPACES TO COR-ANSI-REFERENCE.                         IC1014.2
029600 FAIL-ROUTINE-EX. EXIT.                                           IC1014.2
029700 BAIL-OUT.                                                        IC1014.2
029800     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   IC1014.2
029900     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           IC1014.2
030000 BAIL-OUT-WRITE.                                                  IC1014.2
030100     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  IC1014.2
030200     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IC1014.2
030300     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IC1014.2
030400     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IC1014.2
030500 BAIL-OUT-EX. EXIT.                                               IC1014.2
030600 CCVS1-EXIT.                                                      IC1014.2
030700     EXIT.                                                        IC1014.2
030800 SECT-IC101-0001 SECTION.                                         IC1014.2
030900 CALL-INIT-1.                                                     IC1014.2
031000     MOVE "CALL...USING DATA-NM" TO FEATURE.                      IC1014.2
031100     MOVE "CALL-TEST-01" TO PAR-NAME.                             IC1014.2
031200 CALL-TEST-1.                                                     IC1014.2
031300     CALL "IC102A" USING DN1.                                     IC1014.2
031400     IF DN1 IS EQUAL TO 1                                         IC1014.2
031500         PERFORM PASS                                             IC1014.2
031600             GO TO CALL-WRITE-1.                                  IC1014.2
031700 CALL-FAIL-1.                                                     IC1014.2
031800     MOVE 1 TO CORRECT-18V0.                                      IC1014.2
031900     MOVE DN1 TO COMPUTED-18V0.                                   IC1014.2
032000     PERFORM FAIL.                                                IC1014.2
032100 CALL-WRITE-1.                                                    IC1014.2
032200     PERFORM PRINT-DETAIL.                                        IC1014.2
032300 CALL-INIT-2.                                                     IC1014.2
032400     MOVE 0 TO DN1.                                               IC1014.2
032500 CALL-TEST-2.                                                     IC1014.2
032600     CALL "IC102A" USING DN1.                                     IC1014.2
032700     IF DN1 IS EQUAL TO 2                                         IC1014.2
032800         PERFORM PASS                                             IC1014.2
032900             GO TO CALL-WRITE-2.                                  IC1014.2
033000 CALL-FAIL-2.                                                     IC1014.2
033100     MOVE 2 TO CORRECT-18V0.                                      IC1014.2
033200     MOVE DN1 TO COMPUTED-18V0.                                   IC1014.2
033300     PERFORM FAIL.                                                IC1014.2
033400 CALL-WRITE-2.                                                    IC1014.2
033500     MOVE "CALL-TEST-02" TO PAR-NAME.                             IC1014.2
033600     PERFORM PRINT-DETAIL.                                        IC1014.2
033700 CALL-INIT-3.                                                     IC1014.2
033800     ADD 4 TO DN1.                                                IC1014.2
033900 CALL-TEST-3.                                                     IC1014.2
034000     CALL "IC102A" USING DN1.                                     IC1014.2
034100     IF DN1 IS EQUAL TO 3                                         IC1014.2
034200         PERFORM PASS                                             IC1014.2
034300             GO TO CALL-WRITE-3.                                  IC1014.2
034400 CALL-FAIL-3.                                                     IC1014.2
034500     MOVE 3 TO CORRECT-18V0.                                      IC1014.2
034600     MOVE DN1 TO COMPUTED-18V0.                                   IC1014.2
034700     PERFORM FAIL.                                                IC1014.2
034800 CALL-WRITE-3.                                                    IC1014.2
034900     MOVE "CALL-TEST-03" TO PAR-NAME.                             IC1014.2
035000     PERFORM PRINT-DETAIL.                                        IC1014.2
035100 CALL-TEST-4.                                                     IC1014.2
035200     CALL "IC102A" USING DN2.                                     IC1014.2
035300     IF DN2 IS NOT EQUAL TO 4                                     IC1014.2
035400             GO TO CALL-FAIL-4.                                   IC1014.2
035500     PERFORM PASS.                                                IC1014.2
035600     GO TO CALL-WRITE-4.                                          IC1014.2
035700 CALL-FAIL-4.                                                     IC1014.2
035800     MOVE 4 TO CORRECT-18V0.                                      IC1014.2
035900     MOVE DN2 TO COMPUTED-18V0.                                   IC1014.2
036000     PERFORM FAIL.                                                IC1014.2
036100 CALL-WRITE-4.                                                    IC1014.2
036200     MOVE "CALL-TEST-04" TO PAR-NAME.                             IC1014.2
036300     PERFORM PRINT-DETAIL.                                        IC1014.2
036400 CALL-INIT-5.                                                     IC1014.2
036500     MOVE 0 TO DN2.                                               IC1014.2
036600 CALL-TEST-5.                                                     IC1014.2
036700     CALL "IC102A" USING DN2.                                     IC1014.2
036800     IF DN2 IS EQUAL TO 5                                         IC1014.2
036900         PERFORM PASS                                             IC1014.2
037000             GO TO CALL-WRITE-5.                                  IC1014.2
037100 CALL-FAIL-5.                                                     IC1014.2
037200     MOVE 5 TO CORRECT-18V0.                                      IC1014.2
037300     MOVE DN2 TO COMPUTED-18V0.                                   IC1014.2
037400     PERFORM FAIL.                                                IC1014.2
037500 CALL-WRITE-5.                                                    IC1014.2
037600     MOVE "CALL-TEST-05" TO PAR-NAME.                             IC1014.2
037700     PERFORM PRINT-DETAIL.                                        IC1014.2
037800 CALL-END-ROUTINE.                                                IC1014.2
037900     GO TO CCVS-EXIT.                                             IC1014.2
038000 CCVS-EXIT SECTION.                                               IC1014.2
038100 CCVS-999999.                                                     IC1014.2
038200     GO TO CLOSE-FILES.                                           IC1014.2
      *END-OF,IC101A                                                                  
