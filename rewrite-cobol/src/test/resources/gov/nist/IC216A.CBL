      *HEADER,COBOL,IC216A                                                            
000100 IDENTIFICATION DIVISION.                                         IC2164.2
000200 PROGRAM-ID.                                                      IC2164.2
000300     IC216A.                                                      IC2164.2
000400****************************************************************  IC2164.2
000500*                                                              *  IC2164.2
000600*    VALIDATION FOR:-                                          *  IC2164.2
000700*                                                              *  IC2164.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IC2164.2
000900*                                                              *  IC2164.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IC2164.2
001100*                                                              *  IC2164.2
001200****************************************************************  IC2164.2
001300*                                                              *  IC2164.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  IC2164.2
001500*                                                              *  IC2164.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  IC2164.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  IC2164.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  IC2164.2
001900*                                                              *  IC2164.2
002000****************************************************************  IC2164.2
002100*    THIS IS MAIN PROGRAM IC216.                                  IC2164.2
002200****************************************************************  IC2164.2
002300 ENVIRONMENT DIVISION.                                            IC2164.2
002400 CONFIGURATION SECTION.                                           IC2164.2
002500 SOURCE-COMPUTER.                                                 IC2164.2
002600     XXXXX082.                                                    IC2164.2
002700 OBJECT-COMPUTER.                                                 IC2164.2
002800     XXXXX083.                                                    IC2164.2
002900 INPUT-OUTPUT SECTION.                                            IC2164.2
003000 FILE-CONTROL.                                                    IC2164.2
003100     SELECT PRINT-FILE ASSIGN TO                                  IC2164.2
003200     XXXXX055.                                                    IC2164.2
003300 DATA DIVISION.                                                   IC2164.2
003400 FILE SECTION.                                                    IC2164.2
003500 FD  PRINT-FILE.                                                  IC2164.2
003600 01  PRINT-REC PICTURE X(120).                                    IC2164.2
003700 01  DUMMY-RECORD PICTURE X(120).                                 IC2164.2
003800 WORKING-STORAGE SECTION.                                         IC2164.2
003900 01  DN1.                                                         IC2164.2
004000     02  DN2                     PICTURE X(5).                    IC2164.2
004100     02  DN3                     REDEFINES DN2   PICTURE 9(5).    IC2164.2
004200 01  DN4.                                                         IC2164.2
004300     02  DN5.                                                     IC2164.2
004400          03  DN6                 PICTURE X(3).                   IC2164.2
004500         03  DN7                 PICTURE X(3).                    IC2164.2
004600         03  DN8                 REDEFINES DN7   PICTURE 9(3).    IC2164.2
004700     02  DN9                     PICTURE XX.                      IC2164.2
004800 01  TEST-RESULTS.                                                IC2164.2
004900     02 FILLER                   PIC X      VALUE SPACE.          IC2164.2
005000     02 FEATURE                  PIC X(20)  VALUE SPACE.          IC2164.2
005100     02 FILLER                   PIC X      VALUE SPACE.          IC2164.2
005200     02 P-OR-F                   PIC X(5)   VALUE SPACE.          IC2164.2
005300     02 FILLER                   PIC X      VALUE SPACE.          IC2164.2
005400     02  PAR-NAME.                                                IC2164.2
005500       03 FILLER                 PIC X(19)  VALUE SPACE.          IC2164.2
005600       03  PARDOT-X              PIC X      VALUE SPACE.          IC2164.2
005700       03 DOTVALUE               PIC 99     VALUE ZERO.           IC2164.2
005800     02 FILLER                   PIC X(8)   VALUE SPACE.          IC2164.2
005900     02 RE-MARK                  PIC X(61).                       IC2164.2
006000 01  TEST-COMPUTED.                                               IC2164.2
006100     02 FILLER                   PIC X(30)  VALUE SPACE.          IC2164.2
006200     02 FILLER                   PIC X(17)  VALUE                 IC2164.2
006300            "       COMPUTED=".                                   IC2164.2
006400     02 COMPUTED-X.                                               IC2164.2
006500     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          IC2164.2
006600     03 COMPUTED-N               REDEFINES COMPUTED-A             IC2164.2
006700                                 PIC -9(9).9(9).                  IC2164.2
006800     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         IC2164.2
006900     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     IC2164.2
007000     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     IC2164.2
007100     03       CM-18V0 REDEFINES COMPUTED-A.                       IC2164.2
007200         04 COMPUTED-18V0                    PIC -9(18).          IC2164.2
007300         04 FILLER                           PIC X.               IC2164.2
007400     03 FILLER PIC X(50) VALUE SPACE.                             IC2164.2
007500 01  TEST-CORRECT.                                                IC2164.2
007600     02 FILLER PIC X(30) VALUE SPACE.                             IC2164.2
007700     02 FILLER PIC X(17) VALUE "       CORRECT =".                IC2164.2
007800     02 CORRECT-X.                                                IC2164.2
007900     03 CORRECT-A                  PIC X(20) VALUE SPACE.         IC2164.2
008000     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      IC2164.2
008100     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         IC2164.2
008200     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     IC2164.2
008300     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     IC2164.2
008400     03      CR-18V0 REDEFINES CORRECT-A.                         IC2164.2
008500         04 CORRECT-18V0                     PIC -9(18).          IC2164.2
008600         04 FILLER                           PIC X.               IC2164.2
008700     03 FILLER PIC X(2) VALUE SPACE.                              IC2164.2
008800     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     IC2164.2
008900 01  CCVS-C-1.                                                    IC2164.2
009000     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAIC2164.2
009100-    "SS  PARAGRAPH-NAME                                          IC2164.2
009200-    "       REMARKS".                                            IC2164.2
009300     02 FILLER                     PIC X(20)    VALUE SPACE.      IC2164.2
009400 01  CCVS-C-2.                                                    IC2164.2
009500     02 FILLER                     PIC X        VALUE SPACE.      IC2164.2
009600     02 FILLER                     PIC X(6)     VALUE "TESTED".   IC2164.2
009700     02 FILLER                     PIC X(15)    VALUE SPACE.      IC2164.2
009800     02 FILLER                     PIC X(4)     VALUE "FAIL".     IC2164.2
009900     02 FILLER                     PIC X(94)    VALUE SPACE.      IC2164.2
010000 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       IC2164.2
010100 01  REC-CT                        PIC 99       VALUE ZERO.       IC2164.2
010200 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       IC2164.2
010300 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       IC2164.2
010400 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       IC2164.2
010500 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       IC2164.2
010600 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       IC2164.2
010700 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       IC2164.2
010800 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      IC2164.2
010900 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       IC2164.2
011000 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     IC2164.2
011100 01  CCVS-H-1.                                                    IC2164.2
011200     02  FILLER                    PIC X(39)    VALUE SPACES.     IC2164.2
011300     02  FILLER                    PIC X(42)    VALUE             IC2164.2
011400     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 IC2164.2
011500     02  FILLER                    PIC X(39)    VALUE SPACES.     IC2164.2
011600 01  CCVS-H-2A.                                                   IC2164.2
011700   02  FILLER                        PIC X(40)  VALUE SPACE.      IC2164.2
011800   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  IC2164.2
011900   02  FILLER                        PIC XXXX   VALUE             IC2164.2
012000     "4.2 ".                                                      IC2164.2
012100   02  FILLER                        PIC X(28)  VALUE             IC2164.2
012200            " COPY - NOT FOR DISTRIBUTION".                       IC2164.2
012300   02  FILLER                        PIC X(41)  VALUE SPACE.      IC2164.2
012400                                                                  IC2164.2
012500 01  CCVS-H-2B.                                                   IC2164.2
012600   02  FILLER                        PIC X(15)  VALUE             IC2164.2
012700            "TEST RESULT OF ".                                    IC2164.2
012800   02  TEST-ID                       PIC X(9).                    IC2164.2
012900   02  FILLER                        PIC X(4)   VALUE             IC2164.2
013000            " IN ".                                               IC2164.2
013100   02  FILLER                        PIC X(12)  VALUE             IC2164.2
013200     " HIGH       ".                                              IC2164.2
013300   02  FILLER                        PIC X(22)  VALUE             IC2164.2
013400            " LEVEL VALIDATION FOR ".                             IC2164.2
013500   02  FILLER                        PIC X(58)  VALUE             IC2164.2
013600     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IC2164.2
013700 01  CCVS-H-3.                                                    IC2164.2
013800     02  FILLER                      PIC X(34)  VALUE             IC2164.2
013900            " FOR OFFICIAL USE ONLY    ".                         IC2164.2
014000     02  FILLER                      PIC X(58)  VALUE             IC2164.2
014100     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IC2164.2
014200     02  FILLER                      PIC X(28)  VALUE             IC2164.2
014300            "  COPYRIGHT   1985 ".                                IC2164.2
014400 01  CCVS-E-1.                                                    IC2164.2
014500     02 FILLER                       PIC X(52)  VALUE SPACE.      IC2164.2
014600     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              IC2164.2
014700     02 ID-AGAIN                     PIC X(9).                    IC2164.2
014800     02 FILLER                       PIC X(45)  VALUE SPACES.     IC2164.2
014900 01  CCVS-E-2.                                                    IC2164.2
015000     02  FILLER                      PIC X(31)  VALUE SPACE.      IC2164.2
015100     02  FILLER                      PIC X(21)  VALUE SPACE.      IC2164.2
015200     02 CCVS-E-2-2.                                               IC2164.2
015300         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      IC2164.2
015400         03 FILLER                   PIC X      VALUE SPACE.      IC2164.2
015500         03 ENDER-DESC               PIC X(44)  VALUE             IC2164.2
015600            "ERRORS ENCOUNTERED".                                 IC2164.2
015700 01  CCVS-E-3.                                                    IC2164.2
015800     02  FILLER                      PIC X(22)  VALUE             IC2164.2
015900            " FOR OFFICIAL USE ONLY".                             IC2164.2
016000     02  FILLER                      PIC X(12)  VALUE SPACE.      IC2164.2
016100     02  FILLER                      PIC X(58)  VALUE             IC2164.2
016200     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IC2164.2
016300     02  FILLER                      PIC X(13)  VALUE SPACE.      IC2164.2
016400     02 FILLER                       PIC X(15)  VALUE             IC2164.2
016500             " COPYRIGHT 1985".                                   IC2164.2
016600 01  CCVS-E-4.                                                    IC2164.2
016700     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      IC2164.2
016800     02 FILLER                       PIC X(4)   VALUE " OF ".     IC2164.2
016900     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      IC2164.2
017000     02 FILLER                       PIC X(40)  VALUE             IC2164.2
017100      "  TESTS WERE EXECUTED SUCCESSFULLY".                       IC2164.2
017200 01  XXINFO.                                                      IC2164.2
017300     02 FILLER                       PIC X(19)  VALUE             IC2164.2
017400            "*** INFORMATION ***".                                IC2164.2
017500     02 INFO-TEXT.                                                IC2164.2
017600       04 FILLER                     PIC X(8)   VALUE SPACE.      IC2164.2
017700       04 XXCOMPUTED                 PIC X(20).                   IC2164.2
017800       04 FILLER                     PIC X(5)   VALUE SPACE.      IC2164.2
017900       04 XXCORRECT                  PIC X(20).                   IC2164.2
018000     02 INF-ANSI-REFERENCE           PIC X(48).                   IC2164.2
018100 01  HYPHEN-LINE.                                                 IC2164.2
018200     02 FILLER  PIC IS X VALUE IS SPACE.                          IC2164.2
018300     02 FILLER  PIC IS X(65)    VALUE IS "************************IC2164.2
018400-    "*****************************************".                 IC2164.2
018500     02 FILLER  PIC IS X(54)    VALUE IS "************************IC2164.2
018600-    "******************************".                            IC2164.2
018700 01  CCVS-PGM-ID                     PIC X(9)   VALUE             IC2164.2
018800     "IC216A".                                                    IC2164.2
018900 PROCEDURE DIVISION.                                              IC2164.2
019000 CCVS1 SECTION.                                                   IC2164.2
019100 OPEN-FILES.                                                      IC2164.2
019200     OPEN     OUTPUT PRINT-FILE.                                  IC2164.2
019300     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   IC2164.2
019400     MOVE    SPACE TO TEST-RESULTS.                               IC2164.2
019500     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             IC2164.2
019600     GO TO CCVS1-EXIT.                                            IC2164.2
019700 CLOSE-FILES.                                                     IC2164.2
019800     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   IC2164.2
019900 TERMINATE-CCVS.                                                  IC2164.2
020000*    EXIT PROGRAM.                                                IC2164.2
020100*TERMINATE-CALL.                                                  IC2164.2
020200     STOP     RUN.                                                IC2164.2
020300 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         IC2164.2
020400 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           IC2164.2
020500 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          IC2164.2
020600 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      IC2164.2
020700     MOVE "****TEST DELETED****" TO RE-MARK.                      IC2164.2
020800 PRINT-DETAIL.                                                    IC2164.2
020900     IF REC-CT NOT EQUAL TO ZERO                                  IC2164.2
021000             MOVE "." TO PARDOT-X                                 IC2164.2
021100             MOVE REC-CT TO DOTVALUE.                             IC2164.2
021200     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      IC2164.2
021300     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               IC2164.2
021400        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 IC2164.2
021500          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 IC2164.2
021600     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              IC2164.2
021700     MOVE SPACE TO CORRECT-X.                                     IC2164.2
021800     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         IC2164.2
021900     MOVE     SPACE TO RE-MARK.                                   IC2164.2
022000 HEAD-ROUTINE.                                                    IC2164.2
022100     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IC2164.2
022200     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IC2164.2
022300     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IC2164.2
022400     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IC2164.2
022500 COLUMN-NAMES-ROUTINE.                                            IC2164.2
022600     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IC2164.2
022700     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IC2164.2
022800     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        IC2164.2
022900 END-ROUTINE.                                                     IC2164.2
023000     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.IC2164.2
023100 END-RTN-EXIT.                                                    IC2164.2
023200     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IC2164.2
023300 END-ROUTINE-1.                                                   IC2164.2
023400      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      IC2164.2
023500      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               IC2164.2
023600      ADD PASS-COUNTER TO ERROR-HOLD.                             IC2164.2
023700*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   IC2164.2
023800      MOVE PASS-COUNTER TO CCVS-E-4-1.                            IC2164.2
023900      MOVE ERROR-HOLD TO CCVS-E-4-2.                              IC2164.2
024000      MOVE CCVS-E-4 TO CCVS-E-2-2.                                IC2164.2
024100      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           IC2164.2
024200  END-ROUTINE-12.                                                 IC2164.2
024300      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        IC2164.2
024400     IF       ERROR-COUNTER IS EQUAL TO ZERO                      IC2164.2
024500         MOVE "NO " TO ERROR-TOTAL                                IC2164.2
024600         ELSE                                                     IC2164.2
024700         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       IC2164.2
024800     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           IC2164.2
024900     PERFORM WRITE-LINE.                                          IC2164.2
025000 END-ROUTINE-13.                                                  IC2164.2
025100     IF DELETE-COUNTER IS EQUAL TO ZERO                           IC2164.2
025200         MOVE "NO " TO ERROR-TOTAL  ELSE                          IC2164.2
025300         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      IC2164.2
025400     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   IC2164.2
025500     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IC2164.2
025600      IF   INSPECT-COUNTER EQUAL TO ZERO                          IC2164.2
025700          MOVE "NO " TO ERROR-TOTAL                               IC2164.2
025800      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   IC2164.2
025900      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            IC2164.2
026000      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          IC2164.2
026100     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IC2164.2
026200 WRITE-LINE.                                                      IC2164.2
026300     ADD 1 TO RECORD-COUNT.                                       IC2164.2
026400*    IF RECORD-COUNT GREATER 50                                   IC2164.2
026500*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          IC2164.2
026600*        MOVE SPACE TO DUMMY-RECORD                               IC2164.2
026700*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  IC2164.2
026800*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             IC2164.2
026900*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     IC2164.2
027000*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          IC2164.2
027100*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          IC2164.2
027200*        MOVE ZERO TO RECORD-COUNT.                               IC2164.2
027300     PERFORM WRT-LN.                                              IC2164.2
027400 WRT-LN.                                                          IC2164.2
027500     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               IC2164.2
027600     MOVE SPACE TO DUMMY-RECORD.                                  IC2164.2
027700 BLANK-LINE-PRINT.                                                IC2164.2
027800     PERFORM WRT-LN.                                              IC2164.2
027900 FAIL-ROUTINE.                                                    IC2164.2
028000     IF   COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE. IC2164.2
028100     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.IC2164.2
028200     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IC2164.2
028300     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   IC2164.2
028400     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IC2164.2
028500     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IC2164.2
028600     GO TO  FAIL-ROUTINE-EX.                                      IC2164.2
028700 FAIL-ROUTINE-WRITE.                                              IC2164.2
028800     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         IC2164.2
028900     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 IC2164.2
029000     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. IC2164.2
029100     MOVE   SPACES TO COR-ANSI-REFERENCE.                         IC2164.2
029200 FAIL-ROUTINE-EX. EXIT.                                           IC2164.2
029300 BAIL-OUT.                                                        IC2164.2
029400     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   IC2164.2
029500     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           IC2164.2
029600 BAIL-OUT-WRITE.                                                  IC2164.2
029700     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  IC2164.2
029800     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IC2164.2
029900     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IC2164.2
030000     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IC2164.2
030100 BAIL-OUT-EX. EXIT.                                               IC2164.2
030200 CCVS1-EXIT.                                                      IC2164.2
030300     EXIT.                                                        IC2164.2
030400 CALL-TEST-1.                                                     IC2164.2
030500     MOVE "CALL...USING DATA-NM" TO FEATURE.                      IC2164.2
030600     MOVE "CALL-TEST-1" TO PAR-NAME.                              IC2164.2
030700     MOVE "REFERENCING REDEFINED DATA-NAMES" TO RE-MARK.          IC2164.2
030800     CALL "IC217A" USING DN1, DN4.                                IC2164.2
030900     IF DN1 = 12345                                               IC2164.2
031000        PERFORM PASS                                              IC2164.2
031100        GO TO CALL-WRITE-1.                                       IC2164.2
031200 CALL-FAIL-1.                                                     IC2164.2
031300     MOVE DN1 TO COMPUTED-A.                                      IC2164.2
031400     MOVE 12345 TO CORRECT-A.                                     IC2164.2
031500     PERFORM FAIL.                                                IC2164.2
031600 CALL-WRITE-1.                                                    IC2164.2
031700     PERFORM PRINT-DETAIL.                                        IC2164.2
031800 CALL-TEST-2.                                                     IC2164.2
031900     MOVE "CALL...USING DATA-NM" TO FEATURE.                      IC2164.2
032000     MOVE "CALL-TEST-2" TO PAR-NAME.                              IC2164.2
032100     MOVE "REFERENCING REDEFINED DATA-NAMES" TO RE-MARK.          IC2164.2
032200     IF DN4 = "YES987NO"                                          IC2164.2
032300        PERFORM PASS                                              IC2164.2
032400        GO TO CALL-WRITE-2.                                       IC2164.2
032500 CALL-FAIL-2.                                                     IC2164.2
032600     MOVE DN4 TO COMPUTED-A.                                      IC2164.2
032700     MOVE "YES987NO" TO CORRECT-A.                                IC2164.2
032800     PERFORM FAIL.                                                IC2164.2
032900 CALL-WRITE-2.                                                    IC2164.2
033000     PERFORM PRINT-DETAIL.                                        IC2164.2
033100 CCVS-EXIT SECTION.                                               IC2164.2
033200 CCVS-999999.                                                     IC2164.2
033300     GO TO CLOSE-FILES.                                           IC2164.2
      *END-OF,IC216A                                                                  
