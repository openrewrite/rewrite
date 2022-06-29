      *HEADER,COBOL,IC116M                                                            
000100 IDENTIFICATION DIVISION.                                         IC1164.2
000200 PROGRAM-ID.                                                      IC1164.2
000300     IC116M.                                                      IC1164.2
000400****************************************************************  IC1164.2
000500*                                                              *  IC1164.2
000600*    VALIDATION FOR:-                                          *  IC1164.2
000700*                                                              *  IC1164.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IC1164.2
000900*                                                              *  IC1164.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IC1164.2
001100*                                                              *  IC1164.2
001200****************************************************************  IC1164.2
001300*                                                              *  IC1164.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  IC1164.2
001500*                                                              *  IC1164.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  IC1164.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  IC1164.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  IC1164.2
001900*                                                              *  IC1164.2
002000****************************************************************  IC1164.2
002100*                                                                 IC1164.2
002200*        THE PROGRAM IC116 AND THE SUBPROGRAMS IC117 AND IC118    IC1164.2
002300*    TEST THE CALL STATEMENT WITHOUT THE OPTIONAL USING PHRASE    IC1164.2
002400*    AND THE PROCEDURE DIVISION HEADER WITHOUT THE OPTIONAL       IC1164.2
002500*    USING PHRASE IN THE SUBPROGRAMS.  THE MAIN PROGRAM IC116     IC1164.2
002600*    CALLS THE SUBPROGRAM IC117 WHICH IN TURN CALLS THE SUBPRO-   IC1164.2
002700*    GRAM IC118.  THE SUBPROGRAMS CONTAIN DISPLAY STATEMENTS WHICHIC1164.2
002800*    SHOW THE EXECUTION SEQUENCE FOR THE PROGRAMS.                IC1164.2
002900*                                                                 IC1164.2
003000*    REFERENCE - AMERICAN NATIONAL STANDARD PROGRAMMING LANGUAGE  IC1164.2
003100*                    COBOL, X3.23-1974                            IC1164.2
003200*                SECTION XII, INTER-PROGRAM COMMUNICATION MODULE. IC1164.2
003300*                                                                 IC1164.2
003400******************************************************************IC1164.2
003500 ENVIRONMENT DIVISION.                                            IC1164.2
003600 CONFIGURATION SECTION.                                           IC1164.2
003700 SOURCE-COMPUTER.                                                 IC1164.2
003800     XXXXX082.                                                    IC1164.2
003900 OBJECT-COMPUTER.                                                 IC1164.2
004000     XXXXX083.                                                    IC1164.2
004100 INPUT-OUTPUT SECTION.                                            IC1164.2
004200 FILE-CONTROL.                                                    IC1164.2
004300     SELECT PRINT-FILE ASSIGN TO                                  IC1164.2
004400     XXXXX055.                                                    IC1164.2
004500 DATA DIVISION.                                                   IC1164.2
004600 FILE SECTION.                                                    IC1164.2
004700 FD  PRINT-FILE.                                                  IC1164.2
004800 01  PRINT-REC PICTURE X(120).                                    IC1164.2
004900 01  DUMMY-RECORD PICTURE X(120).                                 IC1164.2
005000 WORKING-STORAGE SECTION.                                         IC1164.2
005100 01  SUMMARY-MESSAGE-1.                                           IC1164.2
005200     02  FILLER          PICTURE X(10)  VALUE SPACE.              IC1164.2
005300     02  FILLER          PICTURE X(46)                            IC1164.2
005400         VALUE "THERE SHOULD BE THREE DISPLAY MESSAGES ON THE ".  IC1164.2
005500     02  FILLER          PICTURE X(23)                            IC1164.2
005600         VALUE "DEFAULT DISPLAY DEVICE.".                         IC1164.2
005700 01  SUMMARY-MESSAGE-2.                                           IC1164.2
005800     02  FILLER          PICTURE X(10)  VALUE SPACE.              IC1164.2
005900     02  FILLER          PICTURE X(44)                            IC1164.2
006000         VALUE "IF THERE ARE NOT THREE DISPLAY MESSAGES THE ".    IC1164.2
006100     02  FILLER          PICTURE X(33)                            IC1164.2
006200         VALUE "OPTIONAL USING PHRASE TESTS FAIL.".               IC1164.2
006300 01  TEST-RESULTS.                                                IC1164.2
006400     02 FILLER                   PIC X      VALUE SPACE.          IC1164.2
006500     02 FEATURE                  PIC X(20)  VALUE SPACE.          IC1164.2
006600     02 FILLER                   PIC X      VALUE SPACE.          IC1164.2
006700     02 P-OR-F                   PIC X(5)   VALUE SPACE.          IC1164.2
006800     02 FILLER                   PIC X      VALUE SPACE.          IC1164.2
006900     02  PAR-NAME.                                                IC1164.2
007000       03 FILLER                 PIC X(19)  VALUE SPACE.          IC1164.2
007100       03  PARDOT-X              PIC X      VALUE SPACE.          IC1164.2
007200       03 DOTVALUE               PIC 99     VALUE ZERO.           IC1164.2
007300     02 FILLER                   PIC X(8)   VALUE SPACE.          IC1164.2
007400     02 RE-MARK                  PIC X(61).                       IC1164.2
007500 01  TEST-COMPUTED.                                               IC1164.2
007600     02 FILLER                   PIC X(30)  VALUE SPACE.          IC1164.2
007700     02 FILLER                   PIC X(17)  VALUE                 IC1164.2
007800            "       COMPUTED=".                                   IC1164.2
007900     02 COMPUTED-X.                                               IC1164.2
008000     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          IC1164.2
008100     03 COMPUTED-N               REDEFINES COMPUTED-A             IC1164.2
008200                                 PIC -9(9).9(9).                  IC1164.2
008300     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         IC1164.2
008400     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     IC1164.2
008500     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     IC1164.2
008600     03       CM-18V0 REDEFINES COMPUTED-A.                       IC1164.2
008700         04 COMPUTED-18V0                    PIC -9(18).          IC1164.2
008800         04 FILLER                           PIC X.               IC1164.2
008900     03 FILLER PIC X(50) VALUE SPACE.                             IC1164.2
009000 01  TEST-CORRECT.                                                IC1164.2
009100     02 FILLER PIC X(30) VALUE SPACE.                             IC1164.2
009200     02 FILLER PIC X(17) VALUE "       CORRECT =".                IC1164.2
009300     02 CORRECT-X.                                                IC1164.2
009400     03 CORRECT-A                  PIC X(20) VALUE SPACE.         IC1164.2
009500     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      IC1164.2
009600     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         IC1164.2
009700     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     IC1164.2
009800     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     IC1164.2
009900     03      CR-18V0 REDEFINES CORRECT-A.                         IC1164.2
010000         04 CORRECT-18V0                     PIC -9(18).          IC1164.2
010100         04 FILLER                           PIC X.               IC1164.2
010200     03 FILLER PIC X(2) VALUE SPACE.                              IC1164.2
010300     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     IC1164.2
010400 01  CCVS-C-1.                                                    IC1164.2
010500     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAIC1164.2
010600-    "SS  PARAGRAPH-NAME                                          IC1164.2
010700-    "       REMARKS".                                            IC1164.2
010800     02 FILLER                     PIC X(20)    VALUE SPACE.      IC1164.2
010900 01  CCVS-C-2.                                                    IC1164.2
011000     02 FILLER                     PIC X        VALUE SPACE.      IC1164.2
011100     02 FILLER                     PIC X(6)     VALUE "TESTED".   IC1164.2
011200     02 FILLER                     PIC X(15)    VALUE SPACE.      IC1164.2
011300     02 FILLER                     PIC X(4)     VALUE "FAIL".     IC1164.2
011400     02 FILLER                     PIC X(94)    VALUE SPACE.      IC1164.2
011500 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       IC1164.2
011600 01  REC-CT                        PIC 99       VALUE ZERO.       IC1164.2
011700 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       IC1164.2
011800 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       IC1164.2
011900 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       IC1164.2
012000 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       IC1164.2
012100 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       IC1164.2
012200 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       IC1164.2
012300 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      IC1164.2
012400 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       IC1164.2
012500 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     IC1164.2
012600 01  CCVS-H-1.                                                    IC1164.2
012700     02  FILLER                    PIC X(39)    VALUE SPACES.     IC1164.2
012800     02  FILLER                    PIC X(42)    VALUE             IC1164.2
012900     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 IC1164.2
013000     02  FILLER                    PIC X(39)    VALUE SPACES.     IC1164.2
013100 01  CCVS-H-2A.                                                   IC1164.2
013200   02  FILLER                        PIC X(40)  VALUE SPACE.      IC1164.2
013300   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  IC1164.2
013400   02  FILLER                        PIC XXXX   VALUE             IC1164.2
013500     "4.2 ".                                                      IC1164.2
013600   02  FILLER                        PIC X(28)  VALUE             IC1164.2
013700            " COPY - NOT FOR DISTRIBUTION".                       IC1164.2
013800   02  FILLER                        PIC X(41)  VALUE SPACE.      IC1164.2
013900                                                                  IC1164.2
014000 01  CCVS-H-2B.                                                   IC1164.2
014100   02  FILLER                        PIC X(15)  VALUE             IC1164.2
014200            "TEST RESULT OF ".                                    IC1164.2
014300   02  TEST-ID                       PIC X(9).                    IC1164.2
014400   02  FILLER                        PIC X(4)   VALUE             IC1164.2
014500            " IN ".                                               IC1164.2
014600   02  FILLER                        PIC X(12)  VALUE             IC1164.2
014700     " HIGH       ".                                              IC1164.2
014800   02  FILLER                        PIC X(22)  VALUE             IC1164.2
014900            " LEVEL VALIDATION FOR ".                             IC1164.2
015000   02  FILLER                        PIC X(58)  VALUE             IC1164.2
015100     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IC1164.2
015200 01  CCVS-H-3.                                                    IC1164.2
015300     02  FILLER                      PIC X(34)  VALUE             IC1164.2
015400            " FOR OFFICIAL USE ONLY    ".                         IC1164.2
015500     02  FILLER                      PIC X(58)  VALUE             IC1164.2
015600     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IC1164.2
015700     02  FILLER                      PIC X(28)  VALUE             IC1164.2
015800            "  COPYRIGHT   1985 ".                                IC1164.2
015900 01  CCVS-E-1.                                                    IC1164.2
016000     02 FILLER                       PIC X(52)  VALUE SPACE.      IC1164.2
016100     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              IC1164.2
016200     02 ID-AGAIN                     PIC X(9).                    IC1164.2
016300     02 FILLER                       PIC X(45)  VALUE SPACES.     IC1164.2
016400 01  CCVS-E-2.                                                    IC1164.2
016500     02  FILLER                      PIC X(31)  VALUE SPACE.      IC1164.2
016600     02  FILLER                      PIC X(21)  VALUE SPACE.      IC1164.2
016700     02 CCVS-E-2-2.                                               IC1164.2
016800         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      IC1164.2
016900         03 FILLER                   PIC X      VALUE SPACE.      IC1164.2
017000         03 ENDER-DESC               PIC X(44)  VALUE             IC1164.2
017100            "ERRORS ENCOUNTERED".                                 IC1164.2
017200 01  CCVS-E-3.                                                    IC1164.2
017300     02  FILLER                      PIC X(22)  VALUE             IC1164.2
017400            " FOR OFFICIAL USE ONLY".                             IC1164.2
017500     02  FILLER                      PIC X(12)  VALUE SPACE.      IC1164.2
017600     02  FILLER                      PIC X(58)  VALUE             IC1164.2
017700     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IC1164.2
017800     02  FILLER                      PIC X(13)  VALUE SPACE.      IC1164.2
017900     02 FILLER                       PIC X(15)  VALUE             IC1164.2
018000             " COPYRIGHT 1985".                                   IC1164.2
018100 01  CCVS-E-4.                                                    IC1164.2
018200     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      IC1164.2
018300     02 FILLER                       PIC X(4)   VALUE " OF ".     IC1164.2
018400     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      IC1164.2
018500     02 FILLER                       PIC X(40)  VALUE             IC1164.2
018600      "  TESTS WERE EXECUTED SUCCESSFULLY".                       IC1164.2
018700 01  XXINFO.                                                      IC1164.2
018800     02 FILLER                       PIC X(19)  VALUE             IC1164.2
018900            "*** INFORMATION ***".                                IC1164.2
019000     02 INFO-TEXT.                                                IC1164.2
019100       04 FILLER                     PIC X(8)   VALUE SPACE.      IC1164.2
019200       04 XXCOMPUTED                 PIC X(20).                   IC1164.2
019300       04 FILLER                     PIC X(5)   VALUE SPACE.      IC1164.2
019400       04 XXCORRECT                  PIC X(20).                   IC1164.2
019500     02 INF-ANSI-REFERENCE           PIC X(48).                   IC1164.2
019600 01  HYPHEN-LINE.                                                 IC1164.2
019700     02 FILLER  PIC IS X VALUE IS SPACE.                          IC1164.2
019800     02 FILLER  PIC IS X(65)    VALUE IS "************************IC1164.2
019900-    "*****************************************".                 IC1164.2
020000     02 FILLER  PIC IS X(54)    VALUE IS "************************IC1164.2
020100-    "******************************".                            IC1164.2
020200 01  CCVS-PGM-ID                     PIC X(9)   VALUE             IC1164.2
020300     "IC116M".                                                    IC1164.2
020400 PROCEDURE DIVISION.                                              IC1164.2
020500 CCVS1 SECTION.                                                   IC1164.2
020600 OPEN-FILES.                                                      IC1164.2
020700     OPEN     OUTPUT PRINT-FILE.                                  IC1164.2
020800     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   IC1164.2
020900     MOVE    SPACE TO TEST-RESULTS.                               IC1164.2
021000     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             IC1164.2
021100     GO TO CCVS1-EXIT.                                            IC1164.2
021200 CLOSE-FILES.                                                     IC1164.2
021300     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   IC1164.2
021400 TERMINATE-CCVS.                                                  IC1164.2
021500*    EXIT PROGRAM.                                                IC1164.2
021600*TERMINATE-CALL.                                                  IC1164.2
021700     STOP     RUN.                                                IC1164.2
021800 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         IC1164.2
021900 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           IC1164.2
022000 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          IC1164.2
022100 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      IC1164.2
022200     MOVE "****TEST DELETED****" TO RE-MARK.                      IC1164.2
022300 PRINT-DETAIL.                                                    IC1164.2
022400     IF REC-CT NOT EQUAL TO ZERO                                  IC1164.2
022500             MOVE "." TO PARDOT-X                                 IC1164.2
022600             MOVE REC-CT TO DOTVALUE.                             IC1164.2
022700     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      IC1164.2
022800     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               IC1164.2
022900        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 IC1164.2
023000          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 IC1164.2
023100     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              IC1164.2
023200     MOVE SPACE TO CORRECT-X.                                     IC1164.2
023300     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         IC1164.2
023400     MOVE     SPACE TO RE-MARK.                                   IC1164.2
023500 HEAD-ROUTINE.                                                    IC1164.2
023600     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IC1164.2
023700     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IC1164.2
023800     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IC1164.2
023900     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IC1164.2
024000 COLUMN-NAMES-ROUTINE.                                            IC1164.2
024100     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IC1164.2
024200     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IC1164.2
024300     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        IC1164.2
024400 END-ROUTINE.                                                     IC1164.2
024500     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.IC1164.2
024600 END-RTN-EXIT.                                                    IC1164.2
024700     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IC1164.2
024800 END-ROUTINE-1.                                                   IC1164.2
024900      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      IC1164.2
025000      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               IC1164.2
025100      ADD PASS-COUNTER TO ERROR-HOLD.                             IC1164.2
025200*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   IC1164.2
025300      MOVE PASS-COUNTER TO CCVS-E-4-1.                            IC1164.2
025400      MOVE ERROR-HOLD TO CCVS-E-4-2.                              IC1164.2
025500      MOVE CCVS-E-4 TO CCVS-E-2-2.                                IC1164.2
025600      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           IC1164.2
025700  END-ROUTINE-12.                                                 IC1164.2
025800      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        IC1164.2
025900     IF       ERROR-COUNTER IS EQUAL TO ZERO                      IC1164.2
026000         MOVE "NO " TO ERROR-TOTAL                                IC1164.2
026100         ELSE                                                     IC1164.2
026200         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       IC1164.2
026300     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           IC1164.2
026400     PERFORM WRITE-LINE.                                          IC1164.2
026500 END-ROUTINE-13.                                                  IC1164.2
026600     IF DELETE-COUNTER IS EQUAL TO ZERO                           IC1164.2
026700         MOVE "NO " TO ERROR-TOTAL  ELSE                          IC1164.2
026800         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      IC1164.2
026900     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   IC1164.2
027000     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IC1164.2
027100      IF   INSPECT-COUNTER EQUAL TO ZERO                          IC1164.2
027200          MOVE "NO " TO ERROR-TOTAL                               IC1164.2
027300      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   IC1164.2
027400      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            IC1164.2
027500      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          IC1164.2
027600     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IC1164.2
027700 WRITE-LINE.                                                      IC1164.2
027800     ADD 1 TO RECORD-COUNT.                                       IC1164.2
027900*    IF RECORD-COUNT GREATER 50                                   IC1164.2
028000*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          IC1164.2
028100*        MOVE SPACE TO DUMMY-RECORD                               IC1164.2
028200*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  IC1164.2
028300*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             IC1164.2
028400*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     IC1164.2
028500*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          IC1164.2
028600*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          IC1164.2
028700*        MOVE ZERO TO RECORD-COUNT.                               IC1164.2
028800     PERFORM WRT-LN.                                              IC1164.2
028900 WRT-LN.                                                          IC1164.2
029000     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               IC1164.2
029100     MOVE SPACE TO DUMMY-RECORD.                                  IC1164.2
029200 BLANK-LINE-PRINT.                                                IC1164.2
029300     PERFORM WRT-LN.                                              IC1164.2
029400 FAIL-ROUTINE.                                                    IC1164.2
029500     IF   COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE. IC1164.2
029600     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.IC1164.2
029700     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IC1164.2
029800     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   IC1164.2
029900     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IC1164.2
030000     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IC1164.2
030100     GO TO  FAIL-ROUTINE-EX.                                      IC1164.2
030200 FAIL-ROUTINE-WRITE.                                              IC1164.2
030300     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         IC1164.2
030400     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 IC1164.2
030500     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. IC1164.2
030600     MOVE   SPACES TO COR-ANSI-REFERENCE.                         IC1164.2
030700 FAIL-ROUTINE-EX. EXIT.                                           IC1164.2
030800 BAIL-OUT.                                                        IC1164.2
030900     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   IC1164.2
031000     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           IC1164.2
031100 BAIL-OUT-WRITE.                                                  IC1164.2
031200     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  IC1164.2
031300     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IC1164.2
031400     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IC1164.2
031500     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IC1164.2
031600 BAIL-OUT-EX. EXIT.                                               IC1164.2
031700 CCVS1-EXIT.                                                      IC1164.2
031800     EXIT.                                                        IC1164.2
031900 SECT-IC116-0001 SECTION.                                         IC1164.2
032000 USNG-TEST-01.                                                    IC1164.2
032100     CALL "IC117M".                                               IC1164.2
032200*                                                                 IC1164.2
032300*    THIS TEST CONTAINS A CALL STATEMENT WITHOUT THE OPTIONAL     IC1164.2
032400*    USING PHRASE.                                                IC1164.2
032500*                                                                 IC1164.2
032600 USNG-WRITE-01.                                                   IC1164.2
032700     PERFORM BLANK-LINE-PRINT.                                    IC1164.2
032800     MOVE "CALL WITHOUT USING" TO FEATURE.                        IC1164.2
032900     MOVE "USNG-TEST-01" TO PAR-NAME.                             IC1164.2
033000     PERFORM PASS.                                                IC1164.2
033100     PERFORM PRINT-DETAIL.                                        IC1164.2
033200 SUMMARY-REMARKS.                                                 IC1164.2
033300     PERFORM BLANK-LINE-PRINT.                                    IC1164.2
033400     MOVE SUMMARY-MESSAGE-1 TO DUMMY-RECORD.                      IC1164.2
033500     PERFORM WRITE-LINE.                                          IC1164.2
033600     MOVE SUMMARY-MESSAGE-2 TO DUMMY-RECORD.                      IC1164.2
033700     PERFORM WRITE-LINE.                                          IC1164.2
033800     PERFORM BLANK-LINE-PRINT.                                    IC1164.2
033900 IC116-EXIT.                                                      IC1164.2
034000     EXIT.                                                        IC1164.2
034100 CCVS-EXIT SECTION.                                               IC1164.2
034200 CCVS-999999.                                                     IC1164.2
034300     GO TO CLOSE-FILES.                                           IC1164.2
      *END-OF,IC116M                                                                  
