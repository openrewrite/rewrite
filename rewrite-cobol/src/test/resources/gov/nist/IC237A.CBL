      *HEADER,COBOL,IC237A                                                            
000100 IDENTIFICATION DIVISION.                                         IC2374.2
000200 PROGRAM-ID.                                                      IC2374.2
000300     IC237A.                                                      IC2374.2
000400****************************************************************  IC2374.2
000500*                                                              *  IC2374.2
000600*    VALIDATION FOR:-                                          *  IC2374.2
000700*                                                              *  IC2374.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IC2374.2
000900*                                                              *  IC2374.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IC2374.2
001100*                                                              *  IC2374.2
001200****************************************************************  IC2374.2
001300*                                                              *  IC2374.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  IC2374.2
001500*                                                              *  IC2374.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  IC2374.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  IC2374.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  IC2374.2
001900*                                                              *  IC2374.2
002000****************************************************************  IC2374.2
002100*                                                              *  IC2374.2
002200*    PROGRAMS IC237A AND IC237A-1 TEST THE ACCESSING OF A      *  IC2374.2
002300*    LINKAGE SECTION ITEM.                                     *  IC2374.2
002400*                                                              *  IC2374.2
002500****************************************************************  IC2374.2
002600 ENVIRONMENT DIVISION.                                            IC2374.2
002700 CONFIGURATION SECTION.                                           IC2374.2
002800 SOURCE-COMPUTER.                                                 IC2374.2
002900     XXXXX082.                                                    IC2374.2
003000 OBJECT-COMPUTER.                                                 IC2374.2
003100     XXXXX083.                                                    IC2374.2
003200 INPUT-OUTPUT SECTION.                                            IC2374.2
003300 FILE-CONTROL.                                                    IC2374.2
003400     SELECT PRINT-FILE ASSIGN TO                                  IC2374.2
003500     XXXXX055.                                                    IC2374.2
003600 DATA DIVISION.                                                   IC2374.2
003700 FILE SECTION.                                                    IC2374.2
003800 FD  PRINT-FILE.                                                  IC2374.2
003900 01  PRINT-REC PICTURE X(120).                                    IC2374.2
004000 01  DUMMY-RECORD PICTURE X(120).                                 IC2374.2
004100 WORKING-STORAGE SECTION.                                         IC2374.2
004200 01  WS-A                        PIC 9 VALUE ZERO.                IC2374.2
004300 01  WS-B                        PIC 9 VALUE ZERO.                IC2374.2
004400 01  WS-C                        PIC 9 VALUE ZERO.                IC2374.2
004500*                                                                 IC2374.2
004600 01  TEST-RESULTS.                                                IC2374.2
004700     02 FILLER                   PIC X      VALUE SPACE.          IC2374.2
004800     02 FEATURE                  PIC X(20)  VALUE SPACE.          IC2374.2
004900     02 FILLER                   PIC X      VALUE SPACE.          IC2374.2
005000     02 P-OR-F                   PIC X(5)   VALUE SPACE.          IC2374.2
005100     02 FILLER                   PIC X      VALUE SPACE.          IC2374.2
005200     02  PAR-NAME.                                                IC2374.2
005300       03 FILLER                 PIC X(19)  VALUE SPACE.          IC2374.2
005400       03  PARDOT-X              PIC X      VALUE SPACE.          IC2374.2
005500       03 DOTVALUE               PIC 99     VALUE ZERO.           IC2374.2
005600     02 FILLER                   PIC X(8)   VALUE SPACE.          IC2374.2
005700     02 RE-MARK                  PIC X(61).                       IC2374.2
005800 01  TEST-COMPUTED.                                               IC2374.2
005900     02 FILLER                   PIC X(30)  VALUE SPACE.          IC2374.2
006000     02 FILLER                   PIC X(17)  VALUE                 IC2374.2
006100            "       COMPUTED=".                                   IC2374.2
006200     02 COMPUTED-X.                                               IC2374.2
006300     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          IC2374.2
006400     03 COMPUTED-N               REDEFINES COMPUTED-A             IC2374.2
006500                                 PIC -9(9).9(9).                  IC2374.2
006600     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         IC2374.2
006700     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     IC2374.2
006800     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     IC2374.2
006900     03       CM-18V0 REDEFINES COMPUTED-A.                       IC2374.2
007000         04 COMPUTED-18V0                    PIC -9(18).          IC2374.2
007100         04 FILLER                           PIC X.               IC2374.2
007200     03 FILLER PIC X(50) VALUE SPACE.                             IC2374.2
007300 01  TEST-CORRECT.                                                IC2374.2
007400     02 FILLER PIC X(30) VALUE SPACE.                             IC2374.2
007500     02 FILLER PIC X(17) VALUE "       CORRECT =".                IC2374.2
007600     02 CORRECT-X.                                                IC2374.2
007700     03 CORRECT-A                  PIC X(20) VALUE SPACE.         IC2374.2
007800     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      IC2374.2
007900     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         IC2374.2
008000     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     IC2374.2
008100     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     IC2374.2
008200     03      CR-18V0 REDEFINES CORRECT-A.                         IC2374.2
008300         04 CORRECT-18V0                     PIC -9(18).          IC2374.2
008400         04 FILLER                           PIC X.               IC2374.2
008500     03 FILLER PIC X(2) VALUE SPACE.                              IC2374.2
008600     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     IC2374.2
008700 01  CCVS-C-1.                                                    IC2374.2
008800     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAIC2374.2
008900-    "SS  PARAGRAPH-NAME                                          IC2374.2
009000-    "       REMARKS".                                            IC2374.2
009100     02 FILLER                     PIC X(20)    VALUE SPACE.      IC2374.2
009200 01  CCVS-C-2.                                                    IC2374.2
009300     02 FILLER                     PIC X        VALUE SPACE.      IC2374.2
009400     02 FILLER                     PIC X(6)     VALUE "TESTED".   IC2374.2
009500     02 FILLER                     PIC X(15)    VALUE SPACE.      IC2374.2
009600     02 FILLER                     PIC X(4)     VALUE "FAIL".     IC2374.2
009700     02 FILLER                     PIC X(94)    VALUE SPACE.      IC2374.2
009800 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       IC2374.2
009900 01  REC-CT                        PIC 99       VALUE ZERO.       IC2374.2
010000 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       IC2374.2
010100 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       IC2374.2
010200 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       IC2374.2
010300 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       IC2374.2
010400 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       IC2374.2
010500 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       IC2374.2
010600 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      IC2374.2
010700 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       IC2374.2
010800 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     IC2374.2
010900 01  CCVS-H-1.                                                    IC2374.2
011000     02  FILLER                    PIC X(39)    VALUE SPACES.     IC2374.2
011100     02  FILLER                    PIC X(42)    VALUE             IC2374.2
011200     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 IC2374.2
011300     02  FILLER                    PIC X(39)    VALUE SPACES.     IC2374.2
011400 01  CCVS-H-2A.                                                   IC2374.2
011500   02  FILLER                        PIC X(40)  VALUE SPACE.      IC2374.2
011600   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  IC2374.2
011700   02  FILLER                        PIC XXXX   VALUE             IC2374.2
011800     "4.2 ".                                                      IC2374.2
011900   02  FILLER                        PIC X(28)  VALUE             IC2374.2
012000            " COPY - NOT FOR DISTRIBUTION".                       IC2374.2
012100   02  FILLER                        PIC X(41)  VALUE SPACE.      IC2374.2
012200                                                                  IC2374.2
012300 01  CCVS-H-2B.                                                   IC2374.2
012400   02  FILLER                        PIC X(15)  VALUE             IC2374.2
012500            "TEST RESULT OF ".                                    IC2374.2
012600   02  TEST-ID                       PIC X(9).                    IC2374.2
012700   02  FILLER                        PIC X(4)   VALUE             IC2374.2
012800            " IN ".                                               IC2374.2
012900   02  FILLER                        PIC X(12)  VALUE             IC2374.2
013000     " HIGH       ".                                              IC2374.2
013100   02  FILLER                        PIC X(22)  VALUE             IC2374.2
013200            " LEVEL VALIDATION FOR ".                             IC2374.2
013300   02  FILLER                        PIC X(58)  VALUE             IC2374.2
013400     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IC2374.2
013500 01  CCVS-H-3.                                                    IC2374.2
013600     02  FILLER                      PIC X(34)  VALUE             IC2374.2
013700            " FOR OFFICIAL USE ONLY    ".                         IC2374.2
013800     02  FILLER                      PIC X(58)  VALUE             IC2374.2
013900     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IC2374.2
014000     02  FILLER                      PIC X(28)  VALUE             IC2374.2
014100            "  COPYRIGHT   1985 ".                                IC2374.2
014200 01  CCVS-E-1.                                                    IC2374.2
014300     02 FILLER                       PIC X(52)  VALUE SPACE.      IC2374.2
014400     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              IC2374.2
014500     02 ID-AGAIN                     PIC X(9).                    IC2374.2
014600     02 FILLER                       PIC X(45)  VALUE SPACES.     IC2374.2
014700 01  CCVS-E-2.                                                    IC2374.2
014800     02  FILLER                      PIC X(31)  VALUE SPACE.      IC2374.2
014900     02  FILLER                      PIC X(21)  VALUE SPACE.      IC2374.2
015000     02 CCVS-E-2-2.                                               IC2374.2
015100         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      IC2374.2
015200         03 FILLER                   PIC X      VALUE SPACE.      IC2374.2
015300         03 ENDER-DESC               PIC X(44)  VALUE             IC2374.2
015400            "ERRORS ENCOUNTERED".                                 IC2374.2
015500 01  CCVS-E-3.                                                    IC2374.2
015600     02  FILLER                      PIC X(22)  VALUE             IC2374.2
015700            " FOR OFFICIAL USE ONLY".                             IC2374.2
015800     02  FILLER                      PIC X(12)  VALUE SPACE.      IC2374.2
015900     02  FILLER                      PIC X(58)  VALUE             IC2374.2
016000     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IC2374.2
016100     02  FILLER                      PIC X(13)  VALUE SPACE.      IC2374.2
016200     02 FILLER                       PIC X(15)  VALUE             IC2374.2
016300             " COPYRIGHT 1985".                                   IC2374.2
016400 01  CCVS-E-4.                                                    IC2374.2
016500     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      IC2374.2
016600     02 FILLER                       PIC X(4)   VALUE " OF ".     IC2374.2
016700     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      IC2374.2
016800     02 FILLER                       PIC X(40)  VALUE             IC2374.2
016900      "  TESTS WERE EXECUTED SUCCESSFULLY".                       IC2374.2
017000 01  XXINFO.                                                      IC2374.2
017100     02 FILLER                       PIC X(19)  VALUE             IC2374.2
017200            "*** INFORMATION ***".                                IC2374.2
017300     02 INFO-TEXT.                                                IC2374.2
017400       04 FILLER                     PIC X(8)   VALUE SPACE.      IC2374.2
017500       04 XXCOMPUTED                 PIC X(20).                   IC2374.2
017600       04 FILLER                     PIC X(5)   VALUE SPACE.      IC2374.2
017700       04 XXCORRECT                  PIC X(20).                   IC2374.2
017800     02 INF-ANSI-REFERENCE           PIC X(48).                   IC2374.2
017900 01  HYPHEN-LINE.                                                 IC2374.2
018000     02 FILLER  PIC IS X VALUE IS SPACE.                          IC2374.2
018100     02 FILLER  PIC IS X(65)    VALUE IS "************************IC2374.2
018200-    "*****************************************".                 IC2374.2
018300     02 FILLER  PIC IS X(54)    VALUE IS "************************IC2374.2
018400-    "******************************".                            IC2374.2
018500 01  CCVS-PGM-ID                     PIC X(9)   VALUE             IC2374.2
018600     "IC237A".                                                    IC2374.2
018700 PROCEDURE DIVISION.                                              IC2374.2
018800 CCVS1 SECTION.                                                   IC2374.2
018900 OPEN-FILES.                                                      IC2374.2
019000     OPEN     OUTPUT PRINT-FILE.                                  IC2374.2
019100     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   IC2374.2
019200     MOVE    SPACE TO TEST-RESULTS.                               IC2374.2
019300     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             IC2374.2
019400     GO TO CCVS1-EXIT.                                            IC2374.2
019500 CLOSE-FILES.                                                     IC2374.2
019600     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   IC2374.2
019700 TERMINATE-CCVS.                                                  IC2374.2
019800*    EXIT PROGRAM.                                                IC2374.2
019900*TERMINATE-CALL.                                                  IC2374.2
020000     STOP     RUN.                                                IC2374.2
020100 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         IC2374.2
020200 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           IC2374.2
020300 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          IC2374.2
020400 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      IC2374.2
020500     MOVE "****TEST DELETED****" TO RE-MARK.                      IC2374.2
020600 PRINT-DETAIL.                                                    IC2374.2
020700     IF REC-CT NOT EQUAL TO ZERO                                  IC2374.2
020800             MOVE "." TO PARDOT-X                                 IC2374.2
020900             MOVE REC-CT TO DOTVALUE.                             IC2374.2
021000     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      IC2374.2
021100     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               IC2374.2
021200        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 IC2374.2
021300          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 IC2374.2
021400     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              IC2374.2
021500     MOVE SPACE TO CORRECT-X.                                     IC2374.2
021600     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         IC2374.2
021700     MOVE     SPACE TO RE-MARK.                                   IC2374.2
021800 HEAD-ROUTINE.                                                    IC2374.2
021900     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IC2374.2
022000     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IC2374.2
022100     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IC2374.2
022200     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IC2374.2
022300 COLUMN-NAMES-ROUTINE.                                            IC2374.2
022400     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IC2374.2
022500     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IC2374.2
022600     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        IC2374.2
022700 END-ROUTINE.                                                     IC2374.2
022800     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.IC2374.2
022900 END-RTN-EXIT.                                                    IC2374.2
023000     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IC2374.2
023100 END-ROUTINE-1.                                                   IC2374.2
023200      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      IC2374.2
023300      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               IC2374.2
023400      ADD PASS-COUNTER TO ERROR-HOLD.                             IC2374.2
023500*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   IC2374.2
023600      MOVE PASS-COUNTER TO CCVS-E-4-1.                            IC2374.2
023700      MOVE ERROR-HOLD TO CCVS-E-4-2.                              IC2374.2
023800      MOVE CCVS-E-4 TO CCVS-E-2-2.                                IC2374.2
023900      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           IC2374.2
024000  END-ROUTINE-12.                                                 IC2374.2
024100      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        IC2374.2
024200     IF       ERROR-COUNTER IS EQUAL TO ZERO                      IC2374.2
024300         MOVE "NO " TO ERROR-TOTAL                                IC2374.2
024400         ELSE                                                     IC2374.2
024500         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       IC2374.2
024600     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           IC2374.2
024700     PERFORM WRITE-LINE.                                          IC2374.2
024800 END-ROUTINE-13.                                                  IC2374.2
024900     IF DELETE-COUNTER IS EQUAL TO ZERO                           IC2374.2
025000         MOVE "NO " TO ERROR-TOTAL  ELSE                          IC2374.2
025100         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      IC2374.2
025200     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   IC2374.2
025300     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IC2374.2
025400      IF   INSPECT-COUNTER EQUAL TO ZERO                          IC2374.2
025500          MOVE "NO " TO ERROR-TOTAL                               IC2374.2
025600      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   IC2374.2
025700      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            IC2374.2
025800      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          IC2374.2
025900     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IC2374.2
026000 WRITE-LINE.                                                      IC2374.2
026100     ADD 1 TO RECORD-COUNT.                                       IC2374.2
026200*    IF RECORD-COUNT GREATER 50                                   IC2374.2
026300*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          IC2374.2
026400*        MOVE SPACE TO DUMMY-RECORD                               IC2374.2
026500*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  IC2374.2
026600*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             IC2374.2
026700*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     IC2374.2
026800*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          IC2374.2
026900*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          IC2374.2
027000*        MOVE ZERO TO RECORD-COUNT.                               IC2374.2
027100     PERFORM WRT-LN.                                              IC2374.2
027200 WRT-LN.                                                          IC2374.2
027300     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               IC2374.2
027400     MOVE SPACE TO DUMMY-RECORD.                                  IC2374.2
027500 BLANK-LINE-PRINT.                                                IC2374.2
027600     PERFORM WRT-LN.                                              IC2374.2
027700 FAIL-ROUTINE.                                                    IC2374.2
027800     IF   COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE. IC2374.2
027900     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.IC2374.2
028000     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IC2374.2
028100     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   IC2374.2
028200     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IC2374.2
028300     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IC2374.2
028400     GO TO  FAIL-ROUTINE-EX.                                      IC2374.2
028500 FAIL-ROUTINE-WRITE.                                              IC2374.2
028600     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         IC2374.2
028700     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 IC2374.2
028800     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. IC2374.2
028900     MOVE   SPACES TO COR-ANSI-REFERENCE.                         IC2374.2
029000 FAIL-ROUTINE-EX. EXIT.                                           IC2374.2
029100 BAIL-OUT.                                                        IC2374.2
029200     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   IC2374.2
029300     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           IC2374.2
029400 BAIL-OUT-WRITE.                                                  IC2374.2
029500     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  IC2374.2
029600     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IC2374.2
029700     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IC2374.2
029800     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IC2374.2
029900 BAIL-OUT-EX. EXIT.                                               IC2374.2
030000 CCVS1-EXIT.                                                      IC2374.2
030100     EXIT.                                                        IC2374.2
030200 SECT-IC237A-001 SECTION.                                         IC2374.2
030300*                                                                 IC2374.2
030400 CALL-INIT-1.                                                     IC2374.2
030500     MOVE   "CALL-TEST-1" TO PAR-NAME.                            IC2374.2
030600     MOVE    1 TO WS-A.                                           IC2374.2
030700     MOVE    3 TO WS-B.                                           IC2374.2
030800     MOVE    5 TO WS-C.                                           IC2374.2
030900 CALL-TEST-0.                                                     IC2374.2
031000     CALL   "IC237A-1" USING WS-A WS-B WS-C.                      IC2374.2
031100 CALL-TEST-1.                                                     IC2374.2
031200     IF      WS-C = WS-A                                          IC2374.2
031300             PERFORM PASS                                         IC2374.2
031400             PERFORM PRINT-DETAIL                                 IC2374.2
031500     ELSE                                                         IC2374.2
031600             MOVE    1 TO CORRECT-N                               IC2374.2
031700             MOVE    WS-A TO COMPUTED-N                           IC2374.2
031800             MOVE   "WRONG VALUE RETURNED FROM CALL TO IC237A-1"  IC2374.2
031900                  TO RE-MARK                                      IC2374.2
032000             PERFORM FAIL                                         IC2374.2
032100             PERFORM PRINT-DETAIL.                                IC2374.2
032200*                                                                 IC2374.2
032300 CCVS-EXIT SECTION.                                               IC2374.2
032400 CCVS-999999.                                                     IC2374.2
032500     GO TO CLOSE-FILES.                                           IC2374.2
032600 END PROGRAM IC237A.                                              IC2374.2
032700 IDENTIFICATION DIVISION.                                         IC2374.2
032800 PROGRAM-ID.                                                      IC2374.2
032900     IC237A-1.                                                    IC2374.2
033000****************************************************************  IC2374.2
033100*                                                              *  IC2374.2
033200*    THIS PROGRAM FORMS PART OF THE COBOL COMPILER VALIDATION  *  IC2374.2
033300*    SYSTEM (CCVS) USED TO TEST COBOL COMPILERS FOR            *  IC2374.2
033400*    CONFORMANCE WITH THE AMERICAN NATIONAL STANDARD           *  IC2374.2
033500*    (ANSI DOCUMENT REFERENCE: X3.23-1985) AND THE STANDARD OF *  IC2374.2
033600*    THE INTERNATIONAL ORGANIZATION FOR STANDARDISATION        *  IC2374.2
033700*    (ISO DOCUMENT REFERENCE: ISO-1989-1985).                  *  IC2374.2
033800*                                                              *  IC2374.2
033900*    THIS CCVS INCORPORATES ENHANCEMENTS TO THE CCVS FOR THE   *  IC2374.2
034000*    1974 STANDARD (ANSI DOCUMENT REFERENCE: X3.23-1974; ISO   *  IC2374.2
034100*    DOCUMENT REFERENCE: ISO-1989-1978).                       *  IC2374.2
034200*                                                              *  IC2374.2
034300*    THESE ENHANCEMENTS WERE SPECIFIED BY A PROJECT TEAM WHICH *  IC2374.2
034400*    WAS FUNDED BY THE COMMISSION FOR EUROPEAN COMMUNITIES AND *  IC2374.2
034500*    WHICH WAS RESPONSIBLE FOR TECHNICAL ISSUES TO:            *  IC2374.2
034600*                                                              *  IC2374.2
034700*          THE FEDERAL SOFTWARE TESTING CENTER                 *  IC2374.2
034800*          OFFICE OF SOFTWARE DEVELOPMENT                      *  IC2374.2
034900*                & INFORMATION TECHNOLOGY                      *  IC2374.2
035000*          TWO SKYLINE PLACE                                   *  IC2374.2
035100*          SUITE 1100                                          *  IC2374.2
035200*          5203 LEESBURG PIKE                                  *  IC2374.2
035300*          FALLS CHURCH                                        *  IC2374.2
035400*          VA 22041                                            *  IC2374.2
035500*          U.S.A.                                              *  IC2374.2
035600*                                                              *  IC2374.2
035700*    THE PROJECT TEAM MEMBERS WERE:                            *  IC2374.2
035800*                                                              *  IC2374.2
035900*          BIADI (BUREAU INTER ADMINISTRATION                  *  IC2374.2
036000*                 DE DOCUMENTATION INFORMATIQUE)               *  IC2374.2
036100*          21 RUE BARA                                         *  IC2374.2
036200*          F-92132 ISSY                                        *  IC2374.2
036300*          FRANCE                                              *  IC2374.2
036400*                                                              *  IC2374.2
036500*                                                              *  IC2374.2
036600*          GMD (GESELLSCHAFT FUR MATHEMATIK                    *  IC2374.2
036700*               UND DATENVERARBEITUNG MBH)                     *  IC2374.2
036800*          SCHLOSS BIRLINGHOVEN                                *  IC2374.2
036900*          POSTFACH 12 40                                      *  IC2374.2
037000*          D-5205 ST. AUGUSTIN 1                               *  IC2374.2
037100*          GERMANY FR                                          *  IC2374.2
037200*                                                              *  IC2374.2
037300*                                                              *  IC2374.2
037400*          NCC (THE NATIONAL COMPUTING CENTRE LTD)             *  IC2374.2
037500*          OXFORD ROAD                                         *  IC2374.2
037600*          MANCHESTER                                          *  IC2374.2
037700*          M1 7ED                                              *  IC2374.2
037800*          UNITED KINGDOM                                      *  IC2374.2
037900*                                                              *  IC2374.2
038000*                                                              *  IC2374.2
038100*    THIS TEST SUITE WAS PRODUCED BY THE NATIONAL COMPUTING    *  IC2374.2
038200*    CENTRE IN ENGLAND AND IS THE OFFICIAL CCVS TEST SUITE     *  IC2374.2
038300*    USED THROUGHOUT EUROPE AND THE UNITED STATES OF AMERICA.  *  IC2374.2
038400*                                                              *  IC2374.2
038500****************************************************************  IC2374.2
038600*                                                              *  IC2374.2
038700*    VALIDATION FOR:-                                          *  IC2374.2
038800*                                                              *  IC2374.2
038900*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IC2374.2
039000*                                                              *  IC2374.2
039100*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IC2374.2
039200*                                                              *  IC2374.2
039300****************************************************************  IC2374.2
039400*                                                              *  IC2374.2
039500*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  IC2374.2
039600*                                                              *  IC2374.2
039700*        X-55  - SYSTEM PRINTER NAME.                          *  IC2374.2
039800*        X-82  - SOURCE COMPUTER NAME.                         *  IC2374.2
039900*        X-83  - OBJECT COMPUTER NAME.                         *  IC2374.2
040000*                                                              *  IC2374.2
040100****************************************************************  IC2374.2
040200 ENVIRONMENT DIVISION.                                            IC2374.2
040300 CONFIGURATION SECTION.                                           IC2374.2
040400 SOURCE-COMPUTER.                                                 IC2374.2
040500     XXXXX082.                                                    IC2374.2
040600 OBJECT-COMPUTER.                                                 IC2374.2
040700     XXXXX083.                                                    IC2374.2
040800 INPUT-OUTPUT SECTION.                                            IC2374.2
040900 FILE-CONTROL.                                                    IC2374.2
041000     SELECT PRINT-FILE ASSIGN TO                                  IC2374.2
041100     XXXXX055.                                                    IC2374.2
041200 DATA DIVISION.                                                   IC2374.2
041300 FILE SECTION.                                                    IC2374.2
041400 FD  PRINT-FILE.                                                  IC2374.2
041500 01  PRINT-REC PICTURE X(120).                                    IC2374.2
041600 01  DUMMY-RECORD PICTURE X(120).                                 IC2374.2
041700 WORKING-STORAGE SECTION.                                         IC2374.2
041800*                                                                 IC2374.2
041900 LINKAGE SECTION.                                                 IC2374.2
042000 01  L-A                   PIC 9.                                 IC2374.2
042100 01  L-A1 REDEFINES L-A    PIC 9.                                 IC2374.2
042200 01  L-B                   PIC 9.                                 IC2374.2
042300 01  L-C                   PIC 9.                                 IC2374.2
042400 PROCEDURE DIVISION USING L-A L-B L-C.                            IC2374.2
042500*                                                                 IC2374.2
042600 SECT-IC237A-1-001 SECTION.                                       IC2374.2
042700*                                                                 IC2374.2
042800 CALLED-FROM-NC121A-FUNCTION.                                     IC2374.2
042900     MOVE    L-A1 TO L-C.                                         IC2374.2
043000 IC237A-EXIT.                                                     IC2374.2
043100     EXIT PROGRAM.                                                IC2374.2
043200 END-OF-PROGRAM.                                                  IC2374.2
043300 END PROGRAM IC237A-1.                                            IC2374.2
      *END-OF,IC237A                                                                  
