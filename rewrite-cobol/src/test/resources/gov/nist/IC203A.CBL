      *HEADER,COBOL,IC203A                                                            
000100 IDENTIFICATION DIVISION.                                         IC2034.2
000200 PROGRAM-ID.                                                      IC2034.2
000300     IC203A.                                                      IC2034.2
000400****************************************************************  IC2034.2
000500*                                                              *  IC2034.2
000600*    VALIDATION FOR:-                                          *  IC2034.2
000700*                                                              *  IC2034.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IC2034.2
000900*                                                              *  IC2034.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IC2034.2
001100*                                                              *  IC2034.2
001200****************************************************************  IC2034.2
001300*                                                              *  IC2034.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  IC2034.2
001500*                                                              *  IC2034.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  IC2034.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  IC2034.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  IC2034.2
001900*                                                              *  IC2034.2
002000****************************************************************  IC2034.2
002100*        THE PROGRAM IC203 TESTS THE USE OF THE CANCEL         *  IC2034.2
002200*    STATEMENT.  THIS PROGRAM VERIFIES THAT THE INITIAL        *  IC2034.2
002300*    CALL TO A SUBPROGRAM AND THE FIRST CALL AFTER A CANCEL    *  IC2034.2
002400*    RESULTS IN A SUBPROGRAM BEING INITIATED IN ITS INITIAL    *  IC2034.2
002500*    STATE.  THE PROGRAM ALSO CANCELS A PROGRAM WHICH HAS      *  IC2034.2
002600*    NOT BEEN CALLED, IN WHICH CASE CONTROL SHOULD PASS        *  IC2034.2
002700*    TO THE NEXT SENTENCE.                                     *  IC2034.2
002800****************************************************************  IC2034.2
002900 ENVIRONMENT DIVISION.                                            IC2034.2
003000 CONFIGURATION SECTION.                                           IC2034.2
003100 SOURCE-COMPUTER.                                                 IC2034.2
003200     XXXXX082.                                                    IC2034.2
003300 OBJECT-COMPUTER.                                                 IC2034.2
003400     XXXXX083.                                                    IC2034.2
003500 INPUT-OUTPUT SECTION.                                            IC2034.2
003600 FILE-CONTROL.                                                    IC2034.2
003700     SELECT PRINT-FILE ASSIGN TO                                  IC2034.2
003800     XXXXX055.                                                    IC2034.2
003900 DATA DIVISION.                                                   IC2034.2
004000 FILE SECTION.                                                    IC2034.2
004100 FD  PRINT-FILE.                                                  IC2034.2
004200 01  PRINT-REC PICTURE X(120).                                    IC2034.2
004300 01  DUMMY-RECORD PICTURE X(120).                                 IC2034.2
004400 WORKING-STORAGE SECTION.                                         IC2034.2
004500 77  ID1 PICTURE X(6)     VALUE "IC204A".                         IC2034.2
004600 77  ID2 PICTURE X(6)     VALUE "IC206A".                         IC2034.2
004700 77  DN1 PICTURE S999.                                            IC2034.2
004800 77  DN5 PICTURE S999.                                            IC2034.2
004900 01  TABLE-1.                                                     IC2034.2
005000     02  DN2 PICTURE XXX.                                         IC2034.2
005100     02  DN3 PICTURE 99.                                          IC2034.2
005200     02  DN4 PICTURE X(5).                                        IC2034.2
005300 01  TABLE-2.                                                     IC2034.2
005400     02  DN6 PICTURE X                                            IC2034.2
005500             OCCURS 2 TIMES.                                      IC2034.2
005600 01  TEST-RESULTS.                                                IC2034.2
005700     02 FILLER                   PIC X      VALUE SPACE.          IC2034.2
005800     02 FEATURE                  PIC X(20)  VALUE SPACE.          IC2034.2
005900     02 FILLER                   PIC X      VALUE SPACE.          IC2034.2
006000     02 P-OR-F                   PIC X(5)   VALUE SPACE.          IC2034.2
006100     02 FILLER                   PIC X      VALUE SPACE.          IC2034.2
006200     02  PAR-NAME.                                                IC2034.2
006300       03 FILLER                 PIC X(19)  VALUE SPACE.          IC2034.2
006400       03  PARDOT-X              PIC X      VALUE SPACE.          IC2034.2
006500       03 DOTVALUE               PIC 99     VALUE ZERO.           IC2034.2
006600     02 FILLER                   PIC X(8)   VALUE SPACE.          IC2034.2
006700     02 RE-MARK                  PIC X(61).                       IC2034.2
006800 01  TEST-COMPUTED.                                               IC2034.2
006900     02 FILLER                   PIC X(30)  VALUE SPACE.          IC2034.2
007000     02 FILLER                   PIC X(17)  VALUE                 IC2034.2
007100            "       COMPUTED=".                                   IC2034.2
007200     02 COMPUTED-X.                                               IC2034.2
007300     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          IC2034.2
007400     03 COMPUTED-N               REDEFINES COMPUTED-A             IC2034.2
007500                                 PIC -9(9).9(9).                  IC2034.2
007600     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         IC2034.2
007700     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     IC2034.2
007800     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     IC2034.2
007900     03       CM-18V0 REDEFINES COMPUTED-A.                       IC2034.2
008000         04 COMPUTED-18V0                    PIC -9(18).          IC2034.2
008100         04 FILLER                           PIC X.               IC2034.2
008200     03 FILLER PIC X(50) VALUE SPACE.                             IC2034.2
008300 01  TEST-CORRECT.                                                IC2034.2
008400     02 FILLER PIC X(30) VALUE SPACE.                             IC2034.2
008500     02 FILLER PIC X(17) VALUE "       CORRECT =".                IC2034.2
008600     02 CORRECT-X.                                                IC2034.2
008700     03 CORRECT-A                  PIC X(20) VALUE SPACE.         IC2034.2
008800     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      IC2034.2
008900     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         IC2034.2
009000     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     IC2034.2
009100     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     IC2034.2
009200     03      CR-18V0 REDEFINES CORRECT-A.                         IC2034.2
009300         04 CORRECT-18V0                     PIC -9(18).          IC2034.2
009400         04 FILLER                           PIC X.               IC2034.2
009500     03 FILLER PIC X(2) VALUE SPACE.                              IC2034.2
009600     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     IC2034.2
009700 01  CCVS-C-1.                                                    IC2034.2
009800     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAIC2034.2
009900-    "SS  PARAGRAPH-NAME                                          IC2034.2
010000-    "       REMARKS".                                            IC2034.2
010100     02 FILLER                     PIC X(20)    VALUE SPACE.      IC2034.2
010200 01  CCVS-C-2.                                                    IC2034.2
010300     02 FILLER                     PIC X        VALUE SPACE.      IC2034.2
010400     02 FILLER                     PIC X(6)     VALUE "TESTED".   IC2034.2
010500     02 FILLER                     PIC X(15)    VALUE SPACE.      IC2034.2
010600     02 FILLER                     PIC X(4)     VALUE "FAIL".     IC2034.2
010700     02 FILLER                     PIC X(94)    VALUE SPACE.      IC2034.2
010800 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       IC2034.2
010900 01  REC-CT                        PIC 99       VALUE ZERO.       IC2034.2
011000 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       IC2034.2
011100 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       IC2034.2
011200 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       IC2034.2
011300 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       IC2034.2
011400 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       IC2034.2
011500 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       IC2034.2
011600 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      IC2034.2
011700 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       IC2034.2
011800 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     IC2034.2
011900 01  CCVS-H-1.                                                    IC2034.2
012000     02  FILLER                    PIC X(39)    VALUE SPACES.     IC2034.2
012100     02  FILLER                    PIC X(42)    VALUE             IC2034.2
012200     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 IC2034.2
012300     02  FILLER                    PIC X(39)    VALUE SPACES.     IC2034.2
012400 01  CCVS-H-2A.                                                   IC2034.2
012500   02  FILLER                        PIC X(40)  VALUE SPACE.      IC2034.2
012600   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  IC2034.2
012700   02  FILLER                        PIC XXXX   VALUE             IC2034.2
012800     "4.2 ".                                                      IC2034.2
012900   02  FILLER                        PIC X(28)  VALUE             IC2034.2
013000            " COPY - NOT FOR DISTRIBUTION".                       IC2034.2
013100   02  FILLER                        PIC X(41)  VALUE SPACE.      IC2034.2
013200                                                                  IC2034.2
013300 01  CCVS-H-2B.                                                   IC2034.2
013400   02  FILLER                        PIC X(15)  VALUE             IC2034.2
013500            "TEST RESULT OF ".                                    IC2034.2
013600   02  TEST-ID                       PIC X(9).                    IC2034.2
013700   02  FILLER                        PIC X(4)   VALUE             IC2034.2
013800            " IN ".                                               IC2034.2
013900   02  FILLER                        PIC X(12)  VALUE             IC2034.2
014000     " HIGH       ".                                              IC2034.2
014100   02  FILLER                        PIC X(22)  VALUE             IC2034.2
014200            " LEVEL VALIDATION FOR ".                             IC2034.2
014300   02  FILLER                        PIC X(58)  VALUE             IC2034.2
014400     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IC2034.2
014500 01  CCVS-H-3.                                                    IC2034.2
014600     02  FILLER                      PIC X(34)  VALUE             IC2034.2
014700            " FOR OFFICIAL USE ONLY    ".                         IC2034.2
014800     02  FILLER                      PIC X(58)  VALUE             IC2034.2
014900     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IC2034.2
015000     02  FILLER                      PIC X(28)  VALUE             IC2034.2
015100            "  COPYRIGHT   1985 ".                                IC2034.2
015200 01  CCVS-E-1.                                                    IC2034.2
015300     02 FILLER                       PIC X(52)  VALUE SPACE.      IC2034.2
015400     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              IC2034.2
015500     02 ID-AGAIN                     PIC X(9).                    IC2034.2
015600     02 FILLER                       PIC X(45)  VALUE SPACES.     IC2034.2
015700 01  CCVS-E-2.                                                    IC2034.2
015800     02  FILLER                      PIC X(31)  VALUE SPACE.      IC2034.2
015900     02  FILLER                      PIC X(21)  VALUE SPACE.      IC2034.2
016000     02 CCVS-E-2-2.                                               IC2034.2
016100         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      IC2034.2
016200         03 FILLER                   PIC X      VALUE SPACE.      IC2034.2
016300         03 ENDER-DESC               PIC X(44)  VALUE             IC2034.2
016400            "ERRORS ENCOUNTERED".                                 IC2034.2
016500 01  CCVS-E-3.                                                    IC2034.2
016600     02  FILLER                      PIC X(22)  VALUE             IC2034.2
016700            " FOR OFFICIAL USE ONLY".                             IC2034.2
016800     02  FILLER                      PIC X(12)  VALUE SPACE.      IC2034.2
016900     02  FILLER                      PIC X(58)  VALUE             IC2034.2
017000     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IC2034.2
017100     02  FILLER                      PIC X(13)  VALUE SPACE.      IC2034.2
017200     02 FILLER                       PIC X(15)  VALUE             IC2034.2
017300             " COPYRIGHT 1985".                                   IC2034.2
017400 01  CCVS-E-4.                                                    IC2034.2
017500     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      IC2034.2
017600     02 FILLER                       PIC X(4)   VALUE " OF ".     IC2034.2
017700     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      IC2034.2
017800     02 FILLER                       PIC X(40)  VALUE             IC2034.2
017900      "  TESTS WERE EXECUTED SUCCESSFULLY".                       IC2034.2
018000 01  XXINFO.                                                      IC2034.2
018100     02 FILLER                       PIC X(19)  VALUE             IC2034.2
018200            "*** INFORMATION ***".                                IC2034.2
018300     02 INFO-TEXT.                                                IC2034.2
018400       04 FILLER                     PIC X(8)   VALUE SPACE.      IC2034.2
018500       04 XXCOMPUTED                 PIC X(20).                   IC2034.2
018600       04 FILLER                     PIC X(5)   VALUE SPACE.      IC2034.2
018700       04 XXCORRECT                  PIC X(20).                   IC2034.2
018800     02 INF-ANSI-REFERENCE           PIC X(48).                   IC2034.2
018900 01  HYPHEN-LINE.                                                 IC2034.2
019000     02 FILLER  PIC IS X VALUE IS SPACE.                          IC2034.2
019100     02 FILLER  PIC IS X(65)    VALUE IS "************************IC2034.2
019200-    "*****************************************".                 IC2034.2
019300     02 FILLER  PIC IS X(54)    VALUE IS "************************IC2034.2
019400-    "******************************".                            IC2034.2
019500 01  CCVS-PGM-ID                     PIC X(9)   VALUE             IC2034.2
019600     "IC203A".                                                    IC2034.2
019700 PROCEDURE DIVISION.                                              IC2034.2
019800 CCVS1 SECTION.                                                   IC2034.2
019900 OPEN-FILES.                                                      IC2034.2
020000     OPEN     OUTPUT PRINT-FILE.                                  IC2034.2
020100     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   IC2034.2
020200     MOVE    SPACE TO TEST-RESULTS.                               IC2034.2
020300     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             IC2034.2
020400     GO TO CCVS1-EXIT.                                            IC2034.2
020500 CLOSE-FILES.                                                     IC2034.2
020600     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   IC2034.2
020700 TERMINATE-CCVS.                                                  IC2034.2
020800*    EXIT PROGRAM.                                                IC2034.2
020900*TERMINATE-CALL.                                                  IC2034.2
021000     STOP     RUN.                                                IC2034.2
021100 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         IC2034.2
021200 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           IC2034.2
021300 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          IC2034.2
021400 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      IC2034.2
021500     MOVE "****TEST DELETED****" TO RE-MARK.                      IC2034.2
021600 PRINT-DETAIL.                                                    IC2034.2
021700     IF REC-CT NOT EQUAL TO ZERO                                  IC2034.2
021800             MOVE "." TO PARDOT-X                                 IC2034.2
021900             MOVE REC-CT TO DOTVALUE.                             IC2034.2
022000     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      IC2034.2
022100     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               IC2034.2
022200        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 IC2034.2
022300          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 IC2034.2
022400     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              IC2034.2
022500     MOVE SPACE TO CORRECT-X.                                     IC2034.2
022600     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         IC2034.2
022700     MOVE     SPACE TO RE-MARK.                                   IC2034.2
022800 HEAD-ROUTINE.                                                    IC2034.2
022900     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IC2034.2
023000     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IC2034.2
023100     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IC2034.2
023200     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IC2034.2
023300 COLUMN-NAMES-ROUTINE.                                            IC2034.2
023400     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IC2034.2
023500     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IC2034.2
023600     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        IC2034.2
023700 END-ROUTINE.                                                     IC2034.2
023800     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.IC2034.2
023900 END-RTN-EXIT.                                                    IC2034.2
024000     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IC2034.2
024100 END-ROUTINE-1.                                                   IC2034.2
024200      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      IC2034.2
024300      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               IC2034.2
024400      ADD PASS-COUNTER TO ERROR-HOLD.                             IC2034.2
024500*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   IC2034.2
024600      MOVE PASS-COUNTER TO CCVS-E-4-1.                            IC2034.2
024700      MOVE ERROR-HOLD TO CCVS-E-4-2.                              IC2034.2
024800      MOVE CCVS-E-4 TO CCVS-E-2-2.                                IC2034.2
024900      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           IC2034.2
025000  END-ROUTINE-12.                                                 IC2034.2
025100      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        IC2034.2
025200     IF       ERROR-COUNTER IS EQUAL TO ZERO                      IC2034.2
025300         MOVE "NO " TO ERROR-TOTAL                                IC2034.2
025400         ELSE                                                     IC2034.2
025500         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       IC2034.2
025600     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           IC2034.2
025700     PERFORM WRITE-LINE.                                          IC2034.2
025800 END-ROUTINE-13.                                                  IC2034.2
025900     IF DELETE-COUNTER IS EQUAL TO ZERO                           IC2034.2
026000         MOVE "NO " TO ERROR-TOTAL  ELSE                          IC2034.2
026100         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      IC2034.2
026200     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   IC2034.2
026300     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IC2034.2
026400      IF   INSPECT-COUNTER EQUAL TO ZERO                          IC2034.2
026500          MOVE "NO " TO ERROR-TOTAL                               IC2034.2
026600      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   IC2034.2
026700      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            IC2034.2
026800      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          IC2034.2
026900     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IC2034.2
027000 WRITE-LINE.                                                      IC2034.2
027100     ADD 1 TO RECORD-COUNT.                                       IC2034.2
027200*    IF RECORD-COUNT GREATER 50                                   IC2034.2
027300*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          IC2034.2
027400*        MOVE SPACE TO DUMMY-RECORD                               IC2034.2
027500*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  IC2034.2
027600*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             IC2034.2
027700*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     IC2034.2
027800*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          IC2034.2
027900*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          IC2034.2
028000*        MOVE ZERO TO RECORD-COUNT.                               IC2034.2
028100     PERFORM WRT-LN.                                              IC2034.2
028200 WRT-LN.                                                          IC2034.2
028300     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               IC2034.2
028400     MOVE SPACE TO DUMMY-RECORD.                                  IC2034.2
028500 BLANK-LINE-PRINT.                                                IC2034.2
028600     PERFORM WRT-LN.                                              IC2034.2
028700 FAIL-ROUTINE.                                                    IC2034.2
028800     IF   COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE. IC2034.2
028900     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.IC2034.2
029000     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IC2034.2
029100     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   IC2034.2
029200     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IC2034.2
029300     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IC2034.2
029400     GO TO  FAIL-ROUTINE-EX.                                      IC2034.2
029500 FAIL-ROUTINE-WRITE.                                              IC2034.2
029600     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         IC2034.2
029700     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 IC2034.2
029800     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. IC2034.2
029900     MOVE   SPACES TO COR-ANSI-REFERENCE.                         IC2034.2
030000 FAIL-ROUTINE-EX. EXIT.                                           IC2034.2
030100 BAIL-OUT.                                                        IC2034.2
030200     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   IC2034.2
030300     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           IC2034.2
030400 BAIL-OUT-WRITE.                                                  IC2034.2
030500     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  IC2034.2
030600     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IC2034.2
030700     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IC2034.2
030800     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IC2034.2
030900 BAIL-OUT-EX. EXIT.                                               IC2034.2
031000 CCVS1-EXIT.                                                      IC2034.2
031100     EXIT.                                                        IC2034.2
031200 SECT-IC203-0001 SECTION.                                         IC2034.2
031300 CALL-TEST-04.                                                    IC2034.2
031400*        CALL-TEST-04 VERIFIES THAT A PROGRAM IS IN ITS           IC2034.2
031500*    INITIAL STATE THE FIRST TIME IT IS CALLED.                   IC2034.2
031600     MOVE "CALL-TEST-04" TO PAR-NAME.                             IC2034.2
031700     MOVE "INITIAL STATE" TO FEATURE.                             IC2034.2
031800     MOVE 1 TO DN3.                                               IC2034.2
031900     MOVE SPACE TO DN2, DN4.                                      IC2034.2
032000     MOVE ZERO TO DN1.                                            IC2034.2
032100     CALL ID1 USING TABLE-1, DN1.                                 IC2034.2
032200     GO TO CALL-TEST-04-01.                                       IC2034.2
032300 CALL-DELETE-04.                                                  IC2034.2
032400     PERFORM DE-LETE.                                             IC2034.2
032500     PERFORM PRINT-DETAIL.                                        IC2034.2
032600     GO TO CALL-TEST-05.                                          IC2034.2
032700 CALL-TEST-04-01.                                                 IC2034.2
032800     MOVE 1 TO REC-CT.                                            IC2034.2
032900     IF DN1 IS EQUAL TO 1                                         IC2034.2
033000         PERFORM PASS                                             IC2034.2
033100         GO TO CALL-WRITE-04-01.                                  IC2034.2
033200 CALL-FAIL-04-01.                                                 IC2034.2
033300     PERFORM FAIL.                                                IC2034.2
033400     MOVE DN1 TO COMPUTED-18V0.                                   IC2034.2
033500     MOVE 1 TO CORRECT-18V0.                                      IC2034.2
033600     MOVE "DN1 INCORRECT" TO RE-MARK.                             IC2034.2
033700 CALL-WRITE-04-01.                                                IC2034.2
033800     PERFORM PRINT-DETAIL.                                        IC2034.2
033900 CALL-TEST-04-02.                                                 IC2034.2
034000     ADD 1 TO REC-CT.                                             IC2034.2
034100     IF DN2 IS EQUAL TO "YES"                                     IC2034.2
034200         PERFORM PASS                                             IC2034.2
034300         GO TO CALL-WRITE-04-02.                                  IC2034.2
034400 CALL-FAIL-04-02.                                                 IC2034.2
034500     PERFORM FAIL.                                                IC2034.2
034600     MOVE DN2 TO COMPUTED-A.                                      IC2034.2
034700     MOVE "YES" TO CORRECT-A.                                     IC2034.2
034800     MOVE "DN2 INCORRECT" TO RE-MARK.                             IC2034.2
034900 CALL-WRITE-04-02.                                                IC2034.2
035000     PERFORM PRINT-DETAIL.                                        IC2034.2
035100 CALL-TEST-04-03.                                                 IC2034.2
035200     ADD 1 TO REC-CT.                                             IC2034.2
035300     IF DN4 EQUAL TO "EQUAL"                                      IC2034.2
035400         PERFORM PASS                                             IC2034.2
035500         GO TO CALL-WRITE-04-03.                                  IC2034.2
035600 CALL-FAIL-04-03.                                                 IC2034.2
035700     PERFORM FAIL.                                                IC2034.2
035800     MOVE DN4 TO COMPUTED-A.                                      IC2034.2
035900     MOVE "EQUAL" TO CORRECT-A.                                   IC2034.2
036000     MOVE "DN4 INCORRECT" TO RE-MARK.                             IC2034.2
036100 CALL-WRITE-04-03.                                                IC2034.2
036200     PERFORM PRINT-DETAIL.                                        IC2034.2
036300 CALL-TEST-05.                                                    IC2034.2
036400     MOVE "CALL-TEST-05" TO PAR-NAME.                             IC2034.2
036500     MOVE "STATE UNCHANGED" TO FEATURE.                           IC2034.2
036600*        CALL-TEST-05 TESTS THAT THE STATE OF THE SUBPROGRAM      IC2034.2
036700*    IS UNCHANGED FROM ITS STATE WHEN LAST EXITED.                IC2034.2
036800     MOVE 2 TO DN3.                                               IC2034.2
036900     MOVE SPACE TO DN2, DN4.                                      IC2034.2
037000     MOVE ZERO TO DN5.                                            IC2034.2
037100     CALL ID1 USING TABLE-1, DN5.                                 IC2034.2
037200     GO TO CALL-TEST-05-01.                                       IC2034.2
037300 CALL-DELETE-05.                                                  IC2034.2
037400     PERFORM DE-LETE.                                             IC2034.2
037500     PERFORM PRINT-DETAIL.                                        IC2034.2
037600     GO TO CNCL-TEST-01.                                          IC2034.2
037700 CALL-TEST-05-01.                                                 IC2034.2
037800     MOVE 1 TO REC-CT.                                            IC2034.2
037900     IF DN5 EQUAL TO 2                                            IC2034.2
038000         PERFORM PASS                                             IC2034.2
038100         GO TO CALL-WRITE-05-01.                                  IC2034.2
038200 CALL-FAIL-05-01.                                                 IC2034.2
038300     PERFORM FAIL.                                                IC2034.2
038400     MOVE DN5 TO COMPUTED-18V0.                                   IC2034.2
038500     MOVE 2 TO CORRECT-18V0.                                      IC2034.2
038600     MOVE "DN5 INCORRECT" TO RE-MARK.                             IC2034.2
038700 CALL-WRITE-05-01.                                                IC2034.2
038800     PERFORM PRINT-DETAIL.                                        IC2034.2
038900 CALL-TEST-05-02.                                                 IC2034.2
039000     ADD 1 TO REC-CT.                                             IC2034.2
039100     IF DN2 EQUAL TO "NO"                                         IC2034.2
039200         PERFORM PASS                                             IC2034.2
039300         GO TO CALL-WRITE-05-02.                                  IC2034.2
039400 CALL-FAIL-05-02.                                                 IC2034.2
039500     PERFORM FAIL.                                                IC2034.2
039600     MOVE DN2 TO COMPUTED-A.                                      IC2034.2
039700     MOVE "NO" TO CORRECT-A.                                      IC2034.2
039800     MOVE "DN2 INCORRECT" TO RE-MARK.                             IC2034.2
039900 CALL-WRITE-05-02.                                                IC2034.2
040000     PERFORM PRINT-DETAIL.                                        IC2034.2
040100 CALL-TEST-05-03.                                                 IC2034.2
040200     ADD 1 TO REC-CT.                                             IC2034.2
040300     IF DN4 EQUAL TO "EQUAL"                                      IC2034.2
040400         PERFORM PASS                                             IC2034.2
040500         GO TO CALL-WRITE-05-03.                                  IC2034.2
040600 CALL-FAIL-05-03.                                                 IC2034.2
040700     PERFORM FAIL.                                                IC2034.2
040800     MOVE DN4 TO COMPUTED-A.                                      IC2034.2
040900     MOVE "EQUAL" TO CORRECT-A.                                   IC2034.2
041000     MOVE "DN4 INCORRECT" TO RE-MARK.                             IC2034.2
041100 CALL-WRITE-05-03.                                                IC2034.2
041200     PERFORM PRINT-DETAIL.                                        IC2034.2
041300 CNCL-TEST-01.                                                    IC2034.2
041400*        THIS TEST VERIFIES THAT A SUBPROGRAM IS IN ITS           IC2034.2
041500*    INITIAL STATE THE FIRST TIME IT IS CALLED FOLLOWING          IC2034.2
041600*    A CANCEL STATEMENT.                                          IC2034.2
041700     MOVE "CNCL-TEST-01" TO PAR-NAME.                             IC2034.2
041800     MOVE "SET TO INITIAL STATE" TO FEATURE.                      IC2034.2
041900     CALL "IC204A" USING TABLE-1, DN1.                            IC2034.2
042000     CANCEL "IC204A".                                             IC2034.2
042100     MOVE 1 TO DN3.                                               IC2034.2
042200     MOVE SPACE TO DN2, DN4.                                      IC2034.2
042300     MOVE ZERO TO DN1.                                            IC2034.2
042400     CALL "IC204A" USING TABLE-1, DN1.                            IC2034.2
042500     GO TO CNCL-TEST-01-01.                                       IC2034.2
042600 CNCL-DELETE-01.                                                  IC2034.2
042700     PERFORM DE-LETE.                                             IC2034.2
042800     PERFORM PRINT-DETAIL.                                        IC2034.2
042900     GO TO CNCL-TEST-02.                                          IC2034.2
043000 CNCL-TEST-01-01.                                                 IC2034.2
043100     MOVE 1 TO REC-CT.                                            IC2034.2
043200     IF DN1 IS EQUAL TO 1                                         IC2034.2
043300         PERFORM PASS                                             IC2034.2
043400         GO TO CNCL-WRITE-01-01.                                  IC2034.2
043500 CNCL-FAIL-01-01.                                                 IC2034.2
043600     PERFORM FAIL.                                                IC2034.2
043700     MOVE DN1 TO COMPUTED-18V0.                                   IC2034.2
043800     MOVE 1 TO CORRECT-18V0.                                      IC2034.2
043900     MOVE "DN1 INCORRECT" TO RE-MARK.                             IC2034.2
044000 CNCL-WRITE-01-01.                                                IC2034.2
044100     PERFORM PRINT-DETAIL.                                        IC2034.2
044200 CNCL-TEST-01-02.                                                 IC2034.2
044300     ADD 1 TO REC-CT.                                             IC2034.2
044400     IF DN2 IS EQUAL TO "YES"                                     IC2034.2
044500         PERFORM PASS                                             IC2034.2
044600         GO TO CNCL-WRITE-01-02.                                  IC2034.2
044700 CNCL-FAIL-01-02.                                                 IC2034.2
044800     PERFORM FAIL.                                                IC2034.2
044900     MOVE DN2 TO COMPUTED-A.                                      IC2034.2
045000     MOVE "YES" TO CORRECT-A.                                     IC2034.2
045100     MOVE "DN2 INCORRECT" TO RE-MARK.                             IC2034.2
045200 CNCL-WRITE-01-02.                                                IC2034.2
045300     PERFORM PRINT-DETAIL.                                        IC2034.2
045400 CNCL-TEST-01-03.                                                 IC2034.2
045500     ADD 1 TO REC-CT.                                             IC2034.2
045600     IF DN4 EQUAL TO "EQUAL"                                      IC2034.2
045700         PERFORM PASS                                             IC2034.2
045800         GO TO CNCL-WRITE-01-03.                                  IC2034.2
045900 CNCL-FAIL-01-03.                                                 IC2034.2
046000     PERFORM FAIL.                                                IC2034.2
046100     MOVE DN4 TO COMPUTED-A.                                      IC2034.2
046200     MOVE "EQUAL" TO CORRECT-A.                                   IC2034.2
046300     MOVE "DN4 INCORRECT" TO RE-MARK.                             IC2034.2
046400 CNCL-WRITE-01-03.                                                IC2034.2
046500     PERFORM PRINT-DETAIL.                                        IC2034.2
046600 CNCL-TEST-02.                                                    IC2034.2
046700*        THIS TEST USES AN IDENTIFIER IN THE CANCEL STATEMENT.    IC2034.2
046800*    THE SUBPROGRAM SHOULD BE IN ITS INITIAL STATE ON THE FIRST   IC2034.2
046900*    CALL FOLLOWING A CANCEL OF THE SUBPROGRAM.                   IC2034.2
047000     MOVE "CNCL-TEST-02" TO PAR-NAME.                             IC2034.2
047100     MOVE "SET TO INITIAL STATE" TO FEATURE.                      IC2034.2
047200     CALL "IC204A" USING TABLE-1, DN1.                            IC2034.2
047300     CANCEL ID1.                                                  IC2034.2
047400     MOVE 1 TO DN3.                                               IC2034.2
047500     MOVE SPACE TO DN2, DN4.                                      IC2034.2
047600     MOVE ZERO TO DN1.                                            IC2034.2
047700     CALL ID1 USING TABLE-1, DN1.                                 IC2034.2
047800     GO TO CNCL-TEST-02-01.                                       IC2034.2
047900 CNCL-DELETE-02.                                                  IC2034.2
048000     PERFORM DE-LETE.                                             IC2034.2
048100     PERFORM PRINT-DETAIL.                                        IC2034.2
048200     GO TO CNCL-TEST-03.                                          IC2034.2
048300 CNCL-TEST-02-01.                                                 IC2034.2
048400     MOVE 1 TO REC-CT.                                            IC2034.2
048500     IF DN1 EQUAL TO 1                                            IC2034.2
048600         PERFORM PASS                                             IC2034.2
048700         GO TO CNCL-WRITE-02-01.                                  IC2034.2
048800 CNCL-FAIL-02-01.                                                 IC2034.2
048900     PERFORM FAIL.                                                IC2034.2
049000     MOVE 1 TO CORRECT-18V0.                                      IC2034.2
049100     MOVE DN1 TO COMPUTED-18V0.                                   IC2034.2
049200     MOVE "DN1 INCORRECT" TO RE-MARK.                             IC2034.2
049300 CNCL-WRITE-02-01.                                                IC2034.2
049400     PERFORM PRINT-DETAIL.                                        IC2034.2
049500 CNCL-TEST-02-02.                                                 IC2034.2
049600     ADD 1 TO REC-CT.                                             IC2034.2
049700     IF DN2 EQUAL TO "YES"                                        IC2034.2
049800         PERFORM PASS                                             IC2034.2
049900         GO TO CNCL-WRITE-02-02.                                  IC2034.2
050000 CNCL-FAIL-02-02.                                                 IC2034.2
050100     PERFORM FAIL.                                                IC2034.2
050200     MOVE DN2 TO COMPUTED-A.                                      IC2034.2
050300     MOVE "YES" TO CORRECT-A.                                     IC2034.2
050400     MOVE "DN2 INCORRECT" TO RE-MARK.                             IC2034.2
050500 CNCL-WRITE-02-02.                                                IC2034.2
050600     PERFORM PRINT-DETAIL.                                        IC2034.2
050700 CNCL-TEST-02-03.                                                 IC2034.2
050800     ADD 1 TO REC-CT.                                             IC2034.2
050900     IF DN4 EQUAL TO "EQUAL"                                      IC2034.2
051000         PERFORM PASS                                             IC2034.2
051100         GO TO CNCL-WRITE-02-03.                                  IC2034.2
051200 CNCL-FAIL-02-03.                                                 IC2034.2
051300     PERFORM FAIL.                                                IC2034.2
051400     MOVE DN4 TO COMPUTED-A.                                      IC2034.2
051500     MOVE "EQUAL" TO CORRECT-A.                                   IC2034.2
051600     MOVE "DN4 INCORRECT" TO RE-MARK.                             IC2034.2
051700 CNCL-WRITE-02-03.                                                IC2034.2
051800     PERFORM PRINT-DETAIL.                                        IC2034.2
051900 CNCL-TEST-03.                                                    IC2034.2
052000*        THIS TEST CANCELS A SUBPROGRAM WHICH HAS ALREADY         IC2034.2
052100*    BEEN CANCELED.  THE SUBPROGRAM IS THEN CALLED AND A CHECK    IC2034.2
052200*    IS MADE TO ENSURE THAT THE SUBPROGRAM WAS IN ITS INITIAL     IC2034.2
052300*    STATE.                                                       IC2034.2
052400     MOVE "CNCL-TEST-03" TO PAR-NAME.                             IC2034.2
052500     MOVE "PREVIOUSLY CANCELED" TO FEATURE.                       IC2034.2
052600 CNCL-INIT-03.                                                    IC2034.2
052700     CALL "IC204A" USING TABLE-1, DN1.                            IC2034.2
052800     CANCEL ID1.                                                  IC2034.2
052900     CANCEL ID1.                                                  IC2034.2
053000     MOVE 1 TO DN3.                                               IC2034.2
053100     MOVE SPACE TO DN2, DN4.                                      IC2034.2
053200     MOVE ZERO TO DN1.                                            IC2034.2
053300     CALL ID1 USING TABLE-1, DN1.                                 IC2034.2
053400     GO TO CNCL-TEST-03-01.                                       IC2034.2
053500 CNCL-DELETE-03.                                                  IC2034.2
053600     PERFORM DE-LETE.                                             IC2034.2
053700     PERFORM PRINT-DETAIL.                                        IC2034.2
053800     GO TO CNCL-TEST-04.                                          IC2034.2
053900 CNCL-TEST-03-01.                                                 IC2034.2
054000     MOVE 1 TO REC-CT.                                            IC2034.2
054100     IF DN1 EQUAL TO 1                                            IC2034.2
054200         PERFORM PASS                                             IC2034.2
054300         GO TO CNCL-WRITE-03-01.                                  IC2034.2
054400 CNCL-FAIL-03-01.                                                 IC2034.2
054500     PERFORM FAIL.                                                IC2034.2
054600     MOVE 1 TO CORRECT-18V0.                                      IC2034.2
054700     MOVE DN1 TO COMPUTED-18V0.                                   IC2034.2
054800     MOVE "DN1 INCORRECT" TO RE-MARK.                             IC2034.2
054900 CNCL-WRITE-03-01.                                                IC2034.2
055000     PERFORM PRINT-DETAIL.                                        IC2034.2
055100 CNCL-TEST-03-02.                                                 IC2034.2
055200     ADD 1 TO REC-CT.                                             IC2034.2
055300     IF DN2 IS EQUAL TO "YES"                                     IC2034.2
055400         PERFORM PASS                                             IC2034.2
055500         GO TO CNCL-WRITE-03-02.                                  IC2034.2
055600 CNCL-FAIL-03-02.                                                 IC2034.2
055700     PERFORM FAIL.                                                IC2034.2
055800     MOVE DN2 TO COMPUTED-A.                                      IC2034.2
055900     MOVE "YES" TO CORRECT-A.                                     IC2034.2
056000     MOVE "DN2 INCORRECT" TO RE-MARK.                             IC2034.2
056100 CNCL-WRITE-03-02.                                                IC2034.2
056200     PERFORM PRINT-DETAIL.                                        IC2034.2
056300 CNCL-TEST-03-03.                                                 IC2034.2
056400     ADD 1 TO REC-CT.                                             IC2034.2
056500     IF DN4 EQUAL TO "EQUAL"                                      IC2034.2
056600         PERFORM PASS                                             IC2034.2
056700         GO TO CNCL-WRITE-03-03.                                  IC2034.2
056800 CNCL-FAIL-03-03.                                                 IC2034.2
056900     PERFORM FAIL.                                                IC2034.2
057000     MOVE DN4 TO COMPUTED-A.                                      IC2034.2
057100     MOVE "EQUAL" TO CORRECT-A.                                   IC2034.2
057200     MOVE "DN4 INCORRECT" TO RE-MARK.                             IC2034.2
057300 CNCL-WRITE-03-03.                                                IC2034.2
057400     PERFORM PRINT-DETAIL.                                        IC2034.2
057500 CNCL-INIT-04.                                                    IC2034.2
057600     MOVE "CNCL-TEST-04" TO PAR-NAME.                             IC2034.2
057700     MOVE "CANCEL UNCALLED PROG" TO FEATURE.                      IC2034.2
057800     MOVE ZERO TO REC-CT.                                         IC2034.2
057900 CNCL-TEST-04.                                                    IC2034.2
058000*        THIS TEST CANCELS A SUBPROGRAM WHICH HAS NEVER           IC2034.2
058100*    BEEN CALLED.  THE NEXT SENTENCE SHOULD BE EXECUTED           IC2034.2
058200*    IN THIS CASE.                                                IC2034.2
058300     CANCEL "IC205A".                                             IC2034.2
058400     GO TO CNCL-PASS-04.                                          IC2034.2
058500 CNCL-DELETE-04.                                                  IC2034.2
058600     PERFORM DE-LETE.                                             IC2034.2
058700     GO TO CNCL-WRITE-04.                                         IC2034.2
058800 CNCL-PASS-04.                                                    IC2034.2
058900     PERFORM PASS.                                                IC2034.2
059000 CNCL-WRITE-04.                                                   IC2034.2
059100     PERFORM PRINT-DETAIL.                                        IC2034.2
059200 CNCL-INIT-05.                                                    IC2034.2
059300     MOVE "CNCL-TEST-05" TO PAR-NAME.                             IC2034.2
059400     MOVE "CANCEL IN SUBPROGRAM" TO FEATURE.                      IC2034.2
059500*        THIS TEST CALLS SUBPROGRAM IC205 WHICH CALLS AND         IC2034.2
059600*    CANCELS A THIRD SUBPROGRAM IC206.                            IC2034.2
059700 CNCL-TEST-05.                                                    IC2034.2
059800     MOVE SPACE TO DN2, DN4, TABLE-2.                             IC2034.2
059900     MOVE ZERO TO DN1.                                            IC2034.2
060000     CALL "IC205A" USING TABLE-1, TABLE-2, DN1.                   IC2034.2
060100     IF TABLE-2 EQUAL TO "AB"                                     IC2034.2
060200         PERFORM PASS                                             IC2034.2
060300         GO TO CNCL-WRITE-05.                                     IC2034.2
060400     GO TO CNCL-FAIL-05.                                          IC2034.2
060500 CNCL-DELETE-05.                                                  IC2034.2
060600     PERFORM DE-LETE.                                             IC2034.2
060700     GO TO CNCL-WRITE-05.                                         IC2034.2
060800 CNCL-FAIL-05.                                                    IC2034.2
060900     PERFORM FAIL.                                                IC2034.2
061000     MOVE "AB" TO CORRECT-A.                                      IC2034.2
061100     MOVE TABLE-2 TO COMPUTED-A.                                  IC2034.2
061200     MOVE "TABLE-2 INCORRECT" TO RE-MARK.                         IC2034.2
061300 CNCL-WRITE-05.                                                   IC2034.2
061400     PERFORM PRINT-DETAIL.                                        IC2034.2
061500 CNCL-INIT-06.                                                    IC2034.2
061600     MOVE "CNCL-TEST-06" TO PAR-NAME.                             IC2034.2
061700     MOVE "CALL CANCELED PROG" TO FEATURE.                        IC2034.2
061800*        THIS TEST CHECKS THAT THE CANCEL OF IC204 WHICH          IC2034.2
061900*    WAS MADE IN THE SUBPROGRAM IC205 WAS EXECUTED PROPERLY.      IC2034.2
062000*    THE SUBPROGRAM IC204 IS CALLED AND THE DATA VALUES           IC2034.2
062100*    ARE CHECKED TO SEE IF IC204 WAS IN ITS INITIAL STATE.        IC2034.2
062200 CNCL-TEST-06.                                                    IC2034.2
062300     MOVE 1 TO DN3.                                               IC2034.2
062400     MOVE SPACE TO DN2, DN4.                                      IC2034.2
062500     MOVE ZERO TO DN1.                                            IC2034.2
062600     CALL ID1 USING TABLE-1, DN1.                                 IC2034.2
062700     GO TO CNCL-TEST-06-01.                                       IC2034.2
062800 CNCL-DELETE-06.                                                  IC2034.2
062900     PERFORM DE-LETE.                                             IC2034.2
063000     GO TO CNCL-WRITE-06-03.                                      IC2034.2
063100 CNCL-TEST-06-01.                                                 IC2034.2
063200     MOVE 1 TO REC-CT.                                            IC2034.2
063300     IF DN1 IS EQUAL TO 1                                         IC2034.2
063400         PERFORM PASS                                             IC2034.2
063500         GO TO CNCL-WRITE-06-01.                                  IC2034.2
063600 CNCL-FAIL-06-01.                                                 IC2034.2
063700     PERFORM FAIL.                                                IC2034.2
063800     MOVE DN1 TO COMPUTED-18V0.                                   IC2034.2
063900     MOVE 1 TO CORRECT-18V0.                                      IC2034.2
064000     MOVE "DN1 INCORRECT" TO RE-MARK.                             IC2034.2
064100 CNCL-WRITE-06-01.                                                IC2034.2
064200     PERFORM PRINT-DETAIL.                                        IC2034.2
064300 CNCL-TEST-06-02.                                                 IC2034.2
064400     ADD 1 TO REC-CT.                                             IC2034.2
064500     IF DN2 IS EQUAL TO "YES"                                     IC2034.2
064600         PERFORM PASS                                             IC2034.2
064700         GO TO CNCL-WRITE-06-02.                                  IC2034.2
064800 CNCL-FAIL-06-02.                                                 IC2034.2
064900     PERFORM FAIL.                                                IC2034.2
065000     MOVE DN2 TO COMPUTED-A.                                      IC2034.2
065100     MOVE "YES" TO CORRECT-A.                                     IC2034.2
065200     MOVE "DN2 INCORRECT" TO RE-MARK.                             IC2034.2
065300 CNCL-WRITE-06-02.                                                IC2034.2
065400     PERFORM PRINT-DETAIL.                                        IC2034.2
065500 CNCL-TEST-06-03.                                                 IC2034.2
065600     ADD 1 TO REC-CT.                                             IC2034.2
065700     IF DN4 EQUAL TO "EQUAL"                                      IC2034.2
065800         PERFORM PASS                                             IC2034.2
065900         GO TO CNCL-WRITE-06-03.                                  IC2034.2
066000 CNCL-FAIL-06-03.                                                 IC2034.2
066100     PERFORM FAIL.                                                IC2034.2
066200     MOVE DN4 TO COMPUTED-A.                                      IC2034.2
066300     MOVE "EQUAL" TO CORRECT-A.                                   IC2034.2
066400     MOVE "DN4 INCORRECT" TO RE-MARK.                             IC2034.2
066500 CNCL-WRITE-06-03.                                                IC2034.2
066600     PERFORM PRINT-DETAIL.                                        IC2034.2
066700 CNCL-INIT-07.                                                    IC2034.2
066800*        THIS TEST CANCELS THE THREE SUBPROGRAMS                  IC2034.2
066900*    CALLED BY THIS ROUTINE.                                      IC2034.2
067000     MOVE "CNCL-TEST-07" TO PAR-NAME.                             IC2034.2
067100     MOVE "CANCEL 3 PROGS" TO FEATURE.                            IC2034.2
067200     MOVE ZERO TO REC-CT.                                         IC2034.2
067300 CNCL-TEST-07.                                                    IC2034.2
067400     CANCEL ID1, "IC205A", ID2.                                   IC2034.2
067500     PERFORM PASS.                                                IC2034.2
067600     GO TO CNCL-WRITE-07.                                         IC2034.2
067700 CNCL-DELETE-07.                                                  IC2034.2
067800     PERFORM DE-LETE.                                             IC2034.2
067900 CNCL-WRITE-07.                                                   IC2034.2
068000     PERFORM PRINT-DETAIL.                                        IC2034.2
068100 CCVS-EXIT SECTION.                                               IC2034.2
068200 CCVS-999999.                                                     IC2034.2
068300     GO TO CLOSE-FILES.                                           IC2034.2
      *END-OF,IC203A                                                                  
