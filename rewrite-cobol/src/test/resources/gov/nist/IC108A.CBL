      *HEADER,COBOL,IC108A                                                            
000100 IDENTIFICATION DIVISION.                                         IC1084.2
000200 PROGRAM-ID.                                                      IC1084.2
000300     IC108A.                                                      IC1084.2
000400****************************************************************  IC1084.2
000500*                                                              *  IC1084.2
000600*    VALIDATION FOR:-                                          *  IC1084.2
000700*                                                              *  IC1084.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IC1084.2
000900*                                                              *  IC1084.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IC1084.2
001100*                                                              *  IC1084.2
001200****************************************************************  IC1084.2
001300*                                                              *  IC1084.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  IC1084.2
001500*                                                              *  IC1084.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  IC1084.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  IC1084.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  IC1084.2
001900*                                                              *  IC1084.2
002000****************************************************************  IC1084.2
002100*        THE PROGRAM IC108 IS THE MAIN PROGRAM WHICH STARTS       IC1084.2
002200*    A SEQUENCE OF CALLS TO THE SUBPROGRAMS IC109A,IC110A AND     IC1084.2
002300*    IC111A.  PARAMETERS ARE SET IN EACH OF THESE SUBPROGRAMS     IC1084.2
002400*    AND CHECKED WHEN CONTROL IS RETURNED TO THE MAIN PROGRAM.    IC1084.2
002500 ENVIRONMENT DIVISION.                                            IC1084.2
002600 CONFIGURATION SECTION.                                           IC1084.2
002700 SOURCE-COMPUTER.                                                 IC1084.2
002800     XXXXX082.                                                    IC1084.2
002900 OBJECT-COMPUTER.                                                 IC1084.2
003000     XXXXX083.                                                    IC1084.2
003100 INPUT-OUTPUT SECTION.                                            IC1084.2
003200 FILE-CONTROL.                                                    IC1084.2
003300     SELECT PRINT-FILE ASSIGN TO                                  IC1084.2
003400     XXXXX055.                                                    IC1084.2
003500 DATA DIVISION.                                                   IC1084.2
003600 FILE SECTION.                                                    IC1084.2
003700 FD  PRINT-FILE.                                                  IC1084.2
003800 01  PRINT-REC PICTURE X(120).                                    IC1084.2
003900 01  DUMMY-RECORD PICTURE X(120).                                 IC1084.2
004000 WORKING-STORAGE SECTION.                                         IC1084.2
004100 01  GRP-01.                                                      IC1084.2
004200     02  SUB-CALLED.                                              IC1084.2
004300         03  DN1 PICTURE X(6).                                    IC1084.2
004400         03  DN2 PICTURE X(6).                                    IC1084.2
004500         03  DN3 PICTURE X(6).                                    IC1084.2
004600     02  TIMES-CALLED.                                            IC1084.2
004700         03  DN4 PICTURE S999    VALUE ZERO.                      IC1084.2
004800         03  DN5 PICTURE S999    VALUE ZERO.                      IC1084.2
004900         03  DN6 PICTURE S999    VALUE ZERO.                      IC1084.2
005000     02  SPECIAL-FLAGS.                                           IC1084.2
005100         03  DN7 PICTURE X.                                       IC1084.2
005200         03  DN8 PICTURE X.                                       IC1084.2
005300         03  DN9 PICTURE X.                                       IC1084.2
005400 01  TEST-RESULTS.                                                IC1084.2
005500     02 FILLER                   PIC X      VALUE SPACE.          IC1084.2
005600     02 FEATURE                  PIC X(20)  VALUE SPACE.          IC1084.2
005700     02 FILLER                   PIC X      VALUE SPACE.          IC1084.2
005800     02 P-OR-F                   PIC X(5)   VALUE SPACE.          IC1084.2
005900     02 FILLER                   PIC X      VALUE SPACE.          IC1084.2
006000     02  PAR-NAME.                                                IC1084.2
006100       03 FILLER                 PIC X(19)  VALUE SPACE.          IC1084.2
006200       03  PARDOT-X              PIC X      VALUE SPACE.          IC1084.2
006300       03 DOTVALUE               PIC 99     VALUE ZERO.           IC1084.2
006400     02 FILLER                   PIC X(8)   VALUE SPACE.          IC1084.2
006500     02 RE-MARK                  PIC X(61).                       IC1084.2
006600 01  TEST-COMPUTED.                                               IC1084.2
006700     02 FILLER                   PIC X(30)  VALUE SPACE.          IC1084.2
006800     02 FILLER                   PIC X(17)  VALUE                 IC1084.2
006900            "       COMPUTED=".                                   IC1084.2
007000     02 COMPUTED-X.                                               IC1084.2
007100     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          IC1084.2
007200     03 COMPUTED-N               REDEFINES COMPUTED-A             IC1084.2
007300                                 PIC -9(9).9(9).                  IC1084.2
007400     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         IC1084.2
007500     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     IC1084.2
007600     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     IC1084.2
007700     03       CM-18V0 REDEFINES COMPUTED-A.                       IC1084.2
007800         04 COMPUTED-18V0                    PIC -9(18).          IC1084.2
007900         04 FILLER                           PIC X.               IC1084.2
008000     03 FILLER PIC X(50) VALUE SPACE.                             IC1084.2
008100 01  TEST-CORRECT.                                                IC1084.2
008200     02 FILLER PIC X(30) VALUE SPACE.                             IC1084.2
008300     02 FILLER PIC X(17) VALUE "       CORRECT =".                IC1084.2
008400     02 CORRECT-X.                                                IC1084.2
008500     03 CORRECT-A                  PIC X(20) VALUE SPACE.         IC1084.2
008600     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      IC1084.2
008700     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         IC1084.2
008800     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     IC1084.2
008900     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     IC1084.2
009000     03      CR-18V0 REDEFINES CORRECT-A.                         IC1084.2
009100         04 CORRECT-18V0                     PIC -9(18).          IC1084.2
009200         04 FILLER                           PIC X.               IC1084.2
009300     03 FILLER PIC X(2) VALUE SPACE.                              IC1084.2
009400     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     IC1084.2
009500 01  CCVS-C-1.                                                    IC1084.2
009600     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAIC1084.2
009700-    "SS  PARAGRAPH-NAME                                          IC1084.2
009800-    "       REMARKS".                                            IC1084.2
009900     02 FILLER                     PIC X(20)    VALUE SPACE.      IC1084.2
010000 01  CCVS-C-2.                                                    IC1084.2
010100     02 FILLER                     PIC X        VALUE SPACE.      IC1084.2
010200     02 FILLER                     PIC X(6)     VALUE "TESTED".   IC1084.2
010300     02 FILLER                     PIC X(15)    VALUE SPACE.      IC1084.2
010400     02 FILLER                     PIC X(4)     VALUE "FAIL".     IC1084.2
010500     02 FILLER                     PIC X(94)    VALUE SPACE.      IC1084.2
010600 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       IC1084.2
010700 01  REC-CT                        PIC 99       VALUE ZERO.       IC1084.2
010800 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       IC1084.2
010900 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       IC1084.2
011000 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       IC1084.2
011100 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       IC1084.2
011200 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       IC1084.2
011300 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       IC1084.2
011400 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      IC1084.2
011500 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       IC1084.2
011600 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     IC1084.2
011700 01  CCVS-H-1.                                                    IC1084.2
011800     02  FILLER                    PIC X(39)    VALUE SPACES.     IC1084.2
011900     02  FILLER                    PIC X(42)    VALUE             IC1084.2
012000     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 IC1084.2
012100     02  FILLER                    PIC X(39)    VALUE SPACES.     IC1084.2
012200 01  CCVS-H-2A.                                                   IC1084.2
012300   02  FILLER                        PIC X(40)  VALUE SPACE.      IC1084.2
012400   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  IC1084.2
012500   02  FILLER                        PIC XXXX   VALUE             IC1084.2
012600     "4.2 ".                                                      IC1084.2
012700   02  FILLER                        PIC X(28)  VALUE             IC1084.2
012800            " COPY - NOT FOR DISTRIBUTION".                       IC1084.2
012900   02  FILLER                        PIC X(41)  VALUE SPACE.      IC1084.2
013000                                                                  IC1084.2
013100 01  CCVS-H-2B.                                                   IC1084.2
013200   02  FILLER                        PIC X(15)  VALUE             IC1084.2
013300            "TEST RESULT OF ".                                    IC1084.2
013400   02  TEST-ID                       PIC X(9).                    IC1084.2
013500   02  FILLER                        PIC X(4)   VALUE             IC1084.2
013600            " IN ".                                               IC1084.2
013700   02  FILLER                        PIC X(12)  VALUE             IC1084.2
013800     " HIGH       ".                                              IC1084.2
013900   02  FILLER                        PIC X(22)  VALUE             IC1084.2
014000            " LEVEL VALIDATION FOR ".                             IC1084.2
014100   02  FILLER                        PIC X(58)  VALUE             IC1084.2
014200     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IC1084.2
014300 01  CCVS-H-3.                                                    IC1084.2
014400     02  FILLER                      PIC X(34)  VALUE             IC1084.2
014500            " FOR OFFICIAL USE ONLY    ".                         IC1084.2
014600     02  FILLER                      PIC X(58)  VALUE             IC1084.2
014700     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IC1084.2
014800     02  FILLER                      PIC X(28)  VALUE             IC1084.2
014900            "  COPYRIGHT   1985 ".                                IC1084.2
015000 01  CCVS-E-1.                                                    IC1084.2
015100     02 FILLER                       PIC X(52)  VALUE SPACE.      IC1084.2
015200     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              IC1084.2
015300     02 ID-AGAIN                     PIC X(9).                    IC1084.2
015400     02 FILLER                       PIC X(45)  VALUE SPACES.     IC1084.2
015500 01  CCVS-E-2.                                                    IC1084.2
015600     02  FILLER                      PIC X(31)  VALUE SPACE.      IC1084.2
015700     02  FILLER                      PIC X(21)  VALUE SPACE.      IC1084.2
015800     02 CCVS-E-2-2.                                               IC1084.2
015900         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      IC1084.2
016000         03 FILLER                   PIC X      VALUE SPACE.      IC1084.2
016100         03 ENDER-DESC               PIC X(44)  VALUE             IC1084.2
016200            "ERRORS ENCOUNTERED".                                 IC1084.2
016300 01  CCVS-E-3.                                                    IC1084.2
016400     02  FILLER                      PIC X(22)  VALUE             IC1084.2
016500            " FOR OFFICIAL USE ONLY".                             IC1084.2
016600     02  FILLER                      PIC X(12)  VALUE SPACE.      IC1084.2
016700     02  FILLER                      PIC X(58)  VALUE             IC1084.2
016800     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IC1084.2
016900     02  FILLER                      PIC X(13)  VALUE SPACE.      IC1084.2
017000     02 FILLER                       PIC X(15)  VALUE             IC1084.2
017100             " COPYRIGHT 1985".                                   IC1084.2
017200 01  CCVS-E-4.                                                    IC1084.2
017300     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      IC1084.2
017400     02 FILLER                       PIC X(4)   VALUE " OF ".     IC1084.2
017500     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      IC1084.2
017600     02 FILLER                       PIC X(40)  VALUE             IC1084.2
017700      "  TESTS WERE EXECUTED SUCCESSFULLY".                       IC1084.2
017800 01  XXINFO.                                                      IC1084.2
017900     02 FILLER                       PIC X(19)  VALUE             IC1084.2
018000            "*** INFORMATION ***".                                IC1084.2
018100     02 INFO-TEXT.                                                IC1084.2
018200       04 FILLER                     PIC X(8)   VALUE SPACE.      IC1084.2
018300       04 XXCOMPUTED                 PIC X(20).                   IC1084.2
018400       04 FILLER                     PIC X(5)   VALUE SPACE.      IC1084.2
018500       04 XXCORRECT                  PIC X(20).                   IC1084.2
018600     02 INF-ANSI-REFERENCE           PIC X(48).                   IC1084.2
018700 01  HYPHEN-LINE.                                                 IC1084.2
018800     02 FILLER  PIC IS X VALUE IS SPACE.                          IC1084.2
018900     02 FILLER  PIC IS X(65)    VALUE IS "************************IC1084.2
019000-    "*****************************************".                 IC1084.2
019100     02 FILLER  PIC IS X(54)    VALUE IS "************************IC1084.2
019200-    "******************************".                            IC1084.2
019300 01  CCVS-PGM-ID                     PIC X(9)   VALUE             IC1084.2
019400     "IC108A".                                                    IC1084.2
019500 PROCEDURE DIVISION.                                              IC1084.2
019600 CCVS1 SECTION.                                                   IC1084.2
019700 OPEN-FILES.                                                      IC1084.2
019800     OPEN     OUTPUT PRINT-FILE.                                  IC1084.2
019900     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   IC1084.2
020000     MOVE    SPACE TO TEST-RESULTS.                               IC1084.2
020100     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             IC1084.2
020200     GO TO CCVS1-EXIT.                                            IC1084.2
020300 CLOSE-FILES.                                                     IC1084.2
020400     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   IC1084.2
020500 TERMINATE-CCVS.                                                  IC1084.2
020600*    EXIT PROGRAM.                                                IC1084.2
020700*TERMINATE-CALL.                                                  IC1084.2
020800     STOP     RUN.                                                IC1084.2
020900 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         IC1084.2
021000 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           IC1084.2
021100 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          IC1084.2
021200 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      IC1084.2
021300     MOVE "****TEST DELETED****" TO RE-MARK.                      IC1084.2
021400 PRINT-DETAIL.                                                    IC1084.2
021500     IF REC-CT NOT EQUAL TO ZERO                                  IC1084.2
021600             MOVE "." TO PARDOT-X                                 IC1084.2
021700             MOVE REC-CT TO DOTVALUE.                             IC1084.2
021800     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      IC1084.2
021900     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               IC1084.2
022000        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 IC1084.2
022100          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 IC1084.2
022200     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              IC1084.2
022300     MOVE SPACE TO CORRECT-X.                                     IC1084.2
022400     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         IC1084.2
022500     MOVE     SPACE TO RE-MARK.                                   IC1084.2
022600 HEAD-ROUTINE.                                                    IC1084.2
022700     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IC1084.2
022800     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IC1084.2
022900     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IC1084.2
023000     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IC1084.2
023100 COLUMN-NAMES-ROUTINE.                                            IC1084.2
023200     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IC1084.2
023300     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IC1084.2
023400     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        IC1084.2
023500 END-ROUTINE.                                                     IC1084.2
023600     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.IC1084.2
023700 END-RTN-EXIT.                                                    IC1084.2
023800     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IC1084.2
023900 END-ROUTINE-1.                                                   IC1084.2
024000      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      IC1084.2
024100      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               IC1084.2
024200      ADD PASS-COUNTER TO ERROR-HOLD.                             IC1084.2
024300*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   IC1084.2
024400      MOVE PASS-COUNTER TO CCVS-E-4-1.                            IC1084.2
024500      MOVE ERROR-HOLD TO CCVS-E-4-2.                              IC1084.2
024600      MOVE CCVS-E-4 TO CCVS-E-2-2.                                IC1084.2
024700      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           IC1084.2
024800  END-ROUTINE-12.                                                 IC1084.2
024900      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        IC1084.2
025000     IF       ERROR-COUNTER IS EQUAL TO ZERO                      IC1084.2
025100         MOVE "NO " TO ERROR-TOTAL                                IC1084.2
025200         ELSE                                                     IC1084.2
025300         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       IC1084.2
025400     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           IC1084.2
025500     PERFORM WRITE-LINE.                                          IC1084.2
025600 END-ROUTINE-13.                                                  IC1084.2
025700     IF DELETE-COUNTER IS EQUAL TO ZERO                           IC1084.2
025800         MOVE "NO " TO ERROR-TOTAL  ELSE                          IC1084.2
025900         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      IC1084.2
026000     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   IC1084.2
026100     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IC1084.2
026200      IF   INSPECT-COUNTER EQUAL TO ZERO                          IC1084.2
026300          MOVE "NO " TO ERROR-TOTAL                               IC1084.2
026400      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   IC1084.2
026500      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            IC1084.2
026600      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          IC1084.2
026700     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IC1084.2
026800 WRITE-LINE.                                                      IC1084.2
026900     ADD 1 TO RECORD-COUNT.                                       IC1084.2
027000*    IF RECORD-COUNT GREATER 50                                   IC1084.2
027100*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          IC1084.2
027200*        MOVE SPACE TO DUMMY-RECORD                               IC1084.2
027300*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  IC1084.2
027400*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             IC1084.2
027500*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     IC1084.2
027600*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          IC1084.2
027700*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          IC1084.2
027800*        MOVE ZERO TO RECORD-COUNT.                               IC1084.2
027900     PERFORM WRT-LN.                                              IC1084.2
028000 WRT-LN.                                                          IC1084.2
028100     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               IC1084.2
028200     MOVE SPACE TO DUMMY-RECORD.                                  IC1084.2
028300 BLANK-LINE-PRINT.                                                IC1084.2
028400     PERFORM WRT-LN.                                              IC1084.2
028500 FAIL-ROUTINE.                                                    IC1084.2
028600     IF   COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE. IC1084.2
028700     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.IC1084.2
028800     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IC1084.2
028900     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   IC1084.2
029000     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IC1084.2
029100     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IC1084.2
029200     GO TO  FAIL-ROUTINE-EX.                                      IC1084.2
029300 FAIL-ROUTINE-WRITE.                                              IC1084.2
029400     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         IC1084.2
029500     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 IC1084.2
029600     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. IC1084.2
029700     MOVE   SPACES TO COR-ANSI-REFERENCE.                         IC1084.2
029800 FAIL-ROUTINE-EX. EXIT.                                           IC1084.2
029900 BAIL-OUT.                                                        IC1084.2
030000     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   IC1084.2
030100     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           IC1084.2
030200 BAIL-OUT-WRITE.                                                  IC1084.2
030300     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  IC1084.2
030400     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IC1084.2
030500     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IC1084.2
030600     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IC1084.2
030700 BAIL-OUT-EX. EXIT.                                               IC1084.2
030800 CCVS1-EXIT.                                                      IC1084.2
030900     EXIT.                                                        IC1084.2
031000 SECTION-IC108-0001 SECTION.                                      IC1084.2
031100 CALL-PARAGRAPH.                                                  IC1084.2
031200*        THE CALL IN THIS PARAGRAPH STARTS THE SEQUENCE           IC1084.2
031300*    OF CALLS TO THE SUBPROGRAMS.                                 IC1084.2
031400     MOVE SPACE TO SUB-CALLED.                                    IC1084.2
031500     MOVE SPACE TO SPECIAL-FLAGS.                                 IC1084.2
031600     CALL "IC109A" USING GRP-01.                                  IC1084.2
031700 CALL-TEST-07.                                                    IC1084.2
031800*        THIS TEST VERIFIES THAT EACH SUBPROGRAM WAS CALLED       IC1084.2
031900*    BY CHECKING THE PARAMETER FIELDS SET IN EACH SUBPROGRAM.     IC1084.2
032000     MOVE "SUBPROGRAM CALLS" TO FEATURE.                          IC1084.2
032100     MOVE "CALL-TEST-07" TO PAR-NAME.                             IC1084.2
032200 CALL-TEST-07-01.                                                 IC1084.2
032300     MOVE 1 TO REC-CT.                                            IC1084.2
032400     IF DN1 IS EQUAL TO "IC109A"                                  IC1084.2
032500         PERFORM PASS                                             IC1084.2
032600         GO TO CALL-WRITE-07-01.                                  IC1084.2
032700 CALL-FAIL-07-01.                                                 IC1084.2
032800     PERFORM FAIL.                                                IC1084.2
032900     MOVE DN1 TO COMPUTED-A.                                      IC1084.2
033000     MOVE "IC109A" TO CORRECT-A.                                  IC1084.2
033100     MOVE "SUBPROGRAM IC109A ERROR" TO RE-MARK.                   IC1084.2
033200 CALL-WRITE-07-01.                                                IC1084.2
033300     PERFORM PRINT-DETAIL.                                        IC1084.2
033400 CALL-TEST-07-02.                                                 IC1084.2
033500     ADD 1 TO REC-CT.                                             IC1084.2
033600     IF DN2 IS EQUAL TO "IC110A"                                  IC1084.2
033700         PERFORM PASS                                             IC1084.2
033800         GO TO CALL-WRITE-07-02.                                  IC1084.2
033900 CALL-FAIL-07-02.                                                 IC1084.2
034000     PERFORM FAIL.                                                IC1084.2
034100     MOVE DN2 TO COMPUTED-A.                                      IC1084.2
034200     MOVE "IC110A" TO CORRECT-A.                                  IC1084.2
034300     MOVE "SUBPROGRAM IC110A ERROR" TO RE-MARK.                   IC1084.2
034400 CALL-WRITE-07-02.                                                IC1084.2
034500     PERFORM PRINT-DETAIL.                                        IC1084.2
034600 CALL-TEST-07-03.                                                 IC1084.2
034700     ADD 1 TO REC-CT.                                             IC1084.2
034800     IF DN3 EQUAL TO "IC111A"                                     IC1084.2
034900         PERFORM PASS                                             IC1084.2
035000         GO TO CALL-WRITE-07-03.                                  IC1084.2
035100 CALL-FAIL-07-03.                                                 IC1084.2
035200     PERFORM FAIL.                                                IC1084.2
035300     MOVE DN3 TO COMPUTED-A.                                      IC1084.2
035400     MOVE "IC111A" TO CORRECT-A.                                  IC1084.2
035500     MOVE "SUBPROGRAM IC111A ERROR" TO RE-MARK.                   IC1084.2
035600 CALL-WRITE-07-03.                                                IC1084.2
035700     PERFORM PRINT-DETAIL.                                        IC1084.2
035800 CALL-TEST-08.                                                    IC1084.2
035900*        THIS TEST VERIFIES THAT EACH OF THE SUBPROGRAMS          IC1084.2
036000*    WERE CALLED ONLY ONCE.                                       IC1084.2
036100     MOVE "CALL-TEST-08" TO PAR-NAME.                             IC1084.2
036200     MOVE "SUBPRGMS CALLED ONCE" TO FEATURE.                      IC1084.2
036300 CALL-TEST-08-01.                                                 IC1084.2
036400     MOVE 1 TO REC-CT.                                            IC1084.2
036500     IF DN4 EQUAL TO 1                                            IC1084.2
036600         PERFORM PASS                                             IC1084.2
036700         GO TO CALL-WRITE-08-01.                                  IC1084.2
036800 CALL-FAIL-08-01.                                                 IC1084.2
036900     PERFORM FAIL.                                                IC1084.2
037000     MOVE DN4 TO COMPUTED-18V0.                                   IC1084.2
037100     MOVE 1 TO CORRECT-18V0.                                      IC1084.2
037200     MOVE "IC109A CALLED N TIMES" TO RE-MARK.                     IC1084.2
037300 CALL-WRITE-08-01.                                                IC1084.2
037400     PERFORM PRINT-DETAIL.                                        IC1084.2
037500 CALL-TEST-08-02.                                                 IC1084.2
037600     ADD 1 TO REC-CT.                                             IC1084.2
037700     IF DN5 EQUAL TO 1                                            IC1084.2
037800         PERFORM PASS                                             IC1084.2
037900         GO TO CALL-WRITE-08-02.                                  IC1084.2
038000 CALL-FAIL-08-02.                                                 IC1084.2
038100     PERFORM FAIL.                                                IC1084.2
038200     MOVE DN5 TO COMPUTED-18V0.                                   IC1084.2
038300     MOVE 1 TO CORRECT-18V0.                                      IC1084.2
038400     MOVE "IC110A CALLED N TIMES" TO RE-MARK.                     IC1084.2
038500 CALL-WRITE-08-02.                                                IC1084.2
038600     PERFORM PRINT-DETAIL.                                        IC1084.2
038700 CALL-TEST-08-03.                                                 IC1084.2
038800     ADD 1 TO REC-CT.                                             IC1084.2
038900     IF DN6 EQUAL TO 1                                            IC1084.2
039000         PERFORM PASS                                             IC1084.2
039100         GO TO CALL-WRITE-08-03.                                  IC1084.2
039200 CALL-FAIL-08-03.                                                 IC1084.2
039300     PERFORM FAIL.                                                IC1084.2
039400     MOVE DN6 TO COMPUTED-18V0.                                   IC1084.2
039500     MOVE 1 TO CORRECT-18V0.                                      IC1084.2
039600     MOVE "IC111A CALLED N TIMES" TO RE-MARK.                     IC1084.2
039700 CALL-WRITE-08-03.                                                IC1084.2
039800     PERFORM PRINT-DETAIL.                                        IC1084.2
039900 LINK-TEST-07.                                                    IC1084.2
040000*        THIS TEST VERIFIES THAT USING PHRASE OPERANDS            IC1084.2
040100*    WHICH WERE DEFINED IN SUBPROGRAM WORKING-STORAGE             IC1084.2
040200*    SECTIONS WERE PROCESSED CORRECTLY.                           IC1084.2
040300     MOVE "LINK-TEST-07" TO PAR-NAME.                             IC1084.2
040400     MOVE "USING OPERANDS" TO FEATURE.                            IC1084.2
040500 LINK-TEST-07-01.                                                 IC1084.2
040600     MOVE 1 TO REC-CT.                                            IC1084.2
040700     IF DN7 EQUAL TO "A"                                          IC1084.2
040800         PERFORM PASS                                             IC1084.2
040900         GO TO LINK-WRITE-07-01.                                  IC1084.2
041000 LINK-FAIL-07-01.                                                 IC1084.2
041100     PERFORM FAIL.                                                IC1084.2
041200     MOVE DN7 TO COMPUTED-A.                                      IC1084.2
041300     MOVE "A" TO CORRECT-A.                                       IC1084.2
041400     MOVE "IC109A WK-STORAGE OPERAND" TO RE-MARK.                 IC1084.2
041500 LINK-WRITE-07-01.                                                IC1084.2
041600     PERFORM PRINT-DETAIL.                                        IC1084.2
041700 LINK-TEST-07-02.                                                 IC1084.2
041800     ADD 1 TO REC-CT.                                             IC1084.2
041900     IF DN8 EQUAL TO "A"                                          IC1084.2
042000         PERFORM PASS                                             IC1084.2
042100         GO TO LINK-WRITE-07-02.                                  IC1084.2
042200 LINK-FAIL-07-02.                                                 IC1084.2
042300     PERFORM FAIL.                                                IC1084.2
042400     MOVE DN8 TO COMPUTED-A.                                      IC1084.2
042500     MOVE "A" TO CORRECT-A.                                       IC1084.2
042600     MOVE "IC110A WK-STORAGE OPERAND" TO RE-MARK.                 IC1084.2
042700 LINK-WRITE-07-02.                                                IC1084.2
042800     PERFORM PRINT-DETAIL.                                        IC1084.2
042900 LINK-TEST-07-03.                                                 IC1084.2
043000     ADD 1 TO REC-CT.                                             IC1084.2
043100     IF DN9 EQUAL TO "B"                                          IC1084.2
043200         PERFORM PASS                                             IC1084.2
043300         GO TO LINK-WRITE-07-03.                                  IC1084.2
043400 LINK-FAIL-07-03.                                                 IC1084.2
043500     PERFORM FAIL.                                                IC1084.2
043600     MOVE DN9 TO COMPUTED-A.                                      IC1084.2
043700     MOVE "B" TO CORRECT-A.                                       IC1084.2
043800     MOVE "IC111A WK-STORAGE OPERAND" TO RE-MARK.                 IC1084.2
043900 LINK-WRITE-07-03.                                                IC1084.2
044000     PERFORM PRINT-DETAIL.                                        IC1084.2
044100     GO TO CCVS-EXIT.                                             IC1084.2
044200 CCVS-EXIT SECTION.                                               IC1084.2
044300 CCVS-999999.                                                     IC1084.2
044400     GO TO CLOSE-FILES.                                           IC1084.2
      *END-OF,IC108A                                                                  
