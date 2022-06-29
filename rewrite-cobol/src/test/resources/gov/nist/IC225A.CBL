      *HEADER,COBOL,IC225A                                                            
000100 IDENTIFICATION DIVISION.                                         IC2254.2
000200 PROGRAM-ID.                                                      IC2254.2
000300     IC225A.                                                      IC2254.2
000400****************************************************************  IC2254.2
000500*                                                              *  IC2254.2
000600*    VALIDATION FOR:-                                          *  IC2254.2
000700*                                                              *  IC2254.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IC2254.2
000900*                                                              *  IC2254.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IC2254.2
001100*                                                              *  IC2254.2
001200****************************************************************  IC2254.2
001300*                                                              *  IC2254.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  IC2254.2
001500*                                                              *  IC2254.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  IC2254.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  IC2254.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  IC2254.2
001900*                                                              *  IC2254.2
002000****************************************************************  IC2254.2
002100*                                                              *  IC2254.2
002200*    PROGRAM IC225A AND IC225A-1 WILL TEST THE NEW LANGUAGE    *  IC2254.2
002300*    ELEMENTS FOR THE LEVEL 2 INTER-PROGRAM COMMUNICATION      *  IC2254.2
002400*    MODULE.                                                   *  IC2254.2
002500*    THE NEW LANGUAGE ELEMENTS TO BE TESTED WILL BE:           *  IC2254.2
002600*          "BY REFERENCE"     PHRASE                           *  IC2254.2
002700*    THE TWO PROGRAMS WILL BE COMPILED IN THE SAME FLOW        *  IC2254.2
002800*    (TO TEST THE "END PROGRAM" STATEMENT) AS SHOWN BELOW:     *  IC2254.2
002900*    IDENTIFICATION DIVISION.                                  *  IC2254.2
003000*    PROGRAM-ID. IC225A.                                       *  IC2254.2
003100*              .                                               *  IC2254.2
003200*              .                                               *  IC2254.2
003300*              .                                               *  IC2254.2
003400*    END PROGRAM IC225A.                                       *  IC2254.2
003500*    PROGRAM-ID. IC225A-1.                                     *  IC2254.2
003600*              .                                               *  IC2254.2
003700*              .                                               *  IC2254.2
003800*              .                                               *  IC2254.2
003900****************************************************************  IC2254.2
004000 ENVIRONMENT DIVISION.                                            IC2254.2
004100 CONFIGURATION SECTION.                                           IC2254.2
004200 SOURCE-COMPUTER.                                                 IC2254.2
004300     XXXXX082.                                                    IC2254.2
004400 OBJECT-COMPUTER.                                                 IC2254.2
004500     XXXXX083.                                                    IC2254.2
004600 INPUT-OUTPUT SECTION.                                            IC2254.2
004700 FILE-CONTROL.                                                    IC2254.2
004800     SELECT PRINT-FILE ASSIGN TO                                  IC2254.2
004900     XXXXX055.                                                    IC2254.2
005000 DATA DIVISION.                                                   IC2254.2
005100 FILE SECTION.                                                    IC2254.2
005200 FD  PRINT-FILE.                                                  IC2254.2
005300 01  PRINT-REC PICTURE X(120).                                    IC2254.2
005400 01  DUMMY-RECORD PICTURE X(120).                                 IC2254.2
005500 WORKING-STORAGE SECTION.                                         IC2254.2
005600 77  DN1 PICTURE S99  VALUE ZERO.                                 IC2254.2
005700 77  DN3 PICTURE S99.                                             IC2254.2
005800 77  ID1 PICTURE X(8) VALUE "IC225A-1".                           IC2254.2
005900 77  ID2 PICTURE X(8).                                            IC2254.2
006000 77  DN2 PICTURE S99                                              IC2254.2
006100         USAGE COMPUTATIONAL, VALUE ZERO.                         IC2254.2
006200 77  DN4 PICTURE S99                                              IC2254.2
006300         USAGE IS COMPUTATIONAL.                                  IC2254.2
006400 77  CALL-COUNT PIC S99.                                          IC2254.2
006500 77  FAIL-FLAG PIC 9.                                             IC2254.2
006600 01  TEST-RESULTS.                                                IC2254.2
006700     02 FILLER                   PIC X      VALUE SPACE.          IC2254.2
006800     02 FEATURE                  PIC X(20)  VALUE SPACE.          IC2254.2
006900     02 FILLER                   PIC X      VALUE SPACE.          IC2254.2
007000     02 P-OR-F                   PIC X(5)   VALUE SPACE.          IC2254.2
007100     02 FILLER                   PIC X      VALUE SPACE.          IC2254.2
007200     02  PAR-NAME.                                                IC2254.2
007300       03 FILLER                 PIC X(19)  VALUE SPACE.          IC2254.2
007400       03  PARDOT-X              PIC X      VALUE SPACE.          IC2254.2
007500       03 DOTVALUE               PIC 99     VALUE ZERO.           IC2254.2
007600     02 FILLER                   PIC X(8)   VALUE SPACE.          IC2254.2
007700     02 RE-MARK                  PIC X(61).                       IC2254.2
007800 01  TEST-COMPUTED.                                               IC2254.2
007900     02 FILLER                   PIC X(30)  VALUE SPACE.          IC2254.2
008000     02 FILLER                   PIC X(17)  VALUE                 IC2254.2
008100            "       COMPUTED=".                                   IC2254.2
008200     02 COMPUTED-X.                                               IC2254.2
008300     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          IC2254.2
008400     03 COMPUTED-N               REDEFINES COMPUTED-A             IC2254.2
008500                                 PIC -9(9).9(9).                  IC2254.2
008600     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         IC2254.2
008700     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     IC2254.2
008800     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     IC2254.2
008900     03       CM-18V0 REDEFINES COMPUTED-A.                       IC2254.2
009000         04 COMPUTED-18V0                    PIC -9(18).          IC2254.2
009100         04 FILLER                           PIC X.               IC2254.2
009200     03 FILLER PIC X(50) VALUE SPACE.                             IC2254.2
009300 01  TEST-CORRECT.                                                IC2254.2
009400     02 FILLER PIC X(30) VALUE SPACE.                             IC2254.2
009500     02 FILLER PIC X(17) VALUE "       CORRECT =".                IC2254.2
009600     02 CORRECT-X.                                                IC2254.2
009700     03 CORRECT-A                  PIC X(20) VALUE SPACE.         IC2254.2
009800     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      IC2254.2
009900     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         IC2254.2
010000     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     IC2254.2
010100     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     IC2254.2
010200     03      CR-18V0 REDEFINES CORRECT-A.                         IC2254.2
010300         04 CORRECT-18V0                     PIC -9(18).          IC2254.2
010400         04 FILLER                           PIC X.               IC2254.2
010500     03 FILLER PIC X(2) VALUE SPACE.                              IC2254.2
010600     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     IC2254.2
010700 01  CCVS-C-1.                                                    IC2254.2
010800     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAIC2254.2
010900-    "SS  PARAGRAPH-NAME                                          IC2254.2
011000-    "       REMARKS".                                            IC2254.2
011100     02 FILLER                     PIC X(20)    VALUE SPACE.      IC2254.2
011200 01  CCVS-C-2.                                                    IC2254.2
011300     02 FILLER                     PIC X        VALUE SPACE.      IC2254.2
011400     02 FILLER                     PIC X(6)     VALUE "TESTED".   IC2254.2
011500     02 FILLER                     PIC X(15)    VALUE SPACE.      IC2254.2
011600     02 FILLER                     PIC X(4)     VALUE "FAIL".     IC2254.2
011700     02 FILLER                     PIC X(94)    VALUE SPACE.      IC2254.2
011800 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       IC2254.2
011900 01  REC-CT                        PIC 99       VALUE ZERO.       IC2254.2
012000 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       IC2254.2
012100 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       IC2254.2
012200 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       IC2254.2
012300 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       IC2254.2
012400 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       IC2254.2
012500 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       IC2254.2
012600 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      IC2254.2
012700 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       IC2254.2
012800 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     IC2254.2
012900 01  CCVS-H-1.                                                    IC2254.2
013000     02  FILLER                    PIC X(39)    VALUE SPACES.     IC2254.2
013100     02  FILLER                    PIC X(42)    VALUE             IC2254.2
013200     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 IC2254.2
013300     02  FILLER                    PIC X(39)    VALUE SPACES.     IC2254.2
013400 01  CCVS-H-2A.                                                   IC2254.2
013500   02  FILLER                        PIC X(40)  VALUE SPACE.      IC2254.2
013600   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  IC2254.2
013700   02  FILLER                        PIC XXXX   VALUE             IC2254.2
013800     "4.2 ".                                                      IC2254.2
013900   02  FILLER                        PIC X(28)  VALUE             IC2254.2
014000            " COPY - NOT FOR DISTRIBUTION".                       IC2254.2
014100   02  FILLER                        PIC X(41)  VALUE SPACE.      IC2254.2
014200                                                                  IC2254.2
014300 01  CCVS-H-2B.                                                   IC2254.2
014400   02  FILLER                        PIC X(15)  VALUE             IC2254.2
014500            "TEST RESULT OF ".                                    IC2254.2
014600   02  TEST-ID                       PIC X(9).                    IC2254.2
014700   02  FILLER                        PIC X(4)   VALUE             IC2254.2
014800            " IN ".                                               IC2254.2
014900   02  FILLER                        PIC X(12)  VALUE             IC2254.2
015000     " HIGH       ".                                              IC2254.2
015100   02  FILLER                        PIC X(22)  VALUE             IC2254.2
015200            " LEVEL VALIDATION FOR ".                             IC2254.2
015300   02  FILLER                        PIC X(58)  VALUE             IC2254.2
015400     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IC2254.2
015500 01  CCVS-H-3.                                                    IC2254.2
015600     02  FILLER                      PIC X(34)  VALUE             IC2254.2
015700            " FOR OFFICIAL USE ONLY    ".                         IC2254.2
015800     02  FILLER                      PIC X(58)  VALUE             IC2254.2
015900     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IC2254.2
016000     02  FILLER                      PIC X(28)  VALUE             IC2254.2
016100            "  COPYRIGHT   1985 ".                                IC2254.2
016200 01  CCVS-E-1.                                                    IC2254.2
016300     02 FILLER                       PIC X(52)  VALUE SPACE.      IC2254.2
016400     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              IC2254.2
016500     02 ID-AGAIN                     PIC X(9).                    IC2254.2
016600     02 FILLER                       PIC X(45)  VALUE SPACES.     IC2254.2
016700 01  CCVS-E-2.                                                    IC2254.2
016800     02  FILLER                      PIC X(31)  VALUE SPACE.      IC2254.2
016900     02  FILLER                      PIC X(21)  VALUE SPACE.      IC2254.2
017000     02 CCVS-E-2-2.                                               IC2254.2
017100         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      IC2254.2
017200         03 FILLER                   PIC X      VALUE SPACE.      IC2254.2
017300         03 ENDER-DESC               PIC X(44)  VALUE             IC2254.2
017400            "ERRORS ENCOUNTERED".                                 IC2254.2
017500 01  CCVS-E-3.                                                    IC2254.2
017600     02  FILLER                      PIC X(22)  VALUE             IC2254.2
017700            " FOR OFFICIAL USE ONLY".                             IC2254.2
017800     02  FILLER                      PIC X(12)  VALUE SPACE.      IC2254.2
017900     02  FILLER                      PIC X(58)  VALUE             IC2254.2
018000     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IC2254.2
018100     02  FILLER                      PIC X(13)  VALUE SPACE.      IC2254.2
018200     02 FILLER                       PIC X(15)  VALUE             IC2254.2
018300             " COPYRIGHT 1985".                                   IC2254.2
018400 01  CCVS-E-4.                                                    IC2254.2
018500     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      IC2254.2
018600     02 FILLER                       PIC X(4)   VALUE " OF ".     IC2254.2
018700     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      IC2254.2
018800     02 FILLER                       PIC X(40)  VALUE             IC2254.2
018900      "  TESTS WERE EXECUTED SUCCESSFULLY".                       IC2254.2
019000 01  XXINFO.                                                      IC2254.2
019100     02 FILLER                       PIC X(19)  VALUE             IC2254.2
019200            "*** INFORMATION ***".                                IC2254.2
019300     02 INFO-TEXT.                                                IC2254.2
019400       04 FILLER                     PIC X(8)   VALUE SPACE.      IC2254.2
019500       04 XXCOMPUTED                 PIC X(20).                   IC2254.2
019600       04 FILLER                     PIC X(5)   VALUE SPACE.      IC2254.2
019700       04 XXCORRECT                  PIC X(20).                   IC2254.2
019800     02 INF-ANSI-REFERENCE           PIC X(48).                   IC2254.2
019900 01  HYPHEN-LINE.                                                 IC2254.2
020000     02 FILLER  PIC IS X VALUE IS SPACE.                          IC2254.2
020100     02 FILLER  PIC IS X(65)    VALUE IS "************************IC2254.2
020200-    "*****************************************".                 IC2254.2
020300     02 FILLER  PIC IS X(54)    VALUE IS "************************IC2254.2
020400-    "******************************".                            IC2254.2
020500 01  CCVS-PGM-ID                     PIC X(9)   VALUE             IC2254.2
020600     "IC225A".                                                    IC2254.2
020700 PROCEDURE DIVISION.                                              IC2254.2
020800 CCVS1 SECTION.                                                   IC2254.2
020900 OPEN-FILES.                                                      IC2254.2
021000     OPEN     OUTPUT PRINT-FILE.                                  IC2254.2
021100     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   IC2254.2
021200     MOVE    SPACE TO TEST-RESULTS.                               IC2254.2
021300     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             IC2254.2
021400     GO TO CCVS1-EXIT.                                            IC2254.2
021500 CLOSE-FILES.                                                     IC2254.2
021600     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   IC2254.2
021700 TERMINATE-CCVS.                                                  IC2254.2
021800*    EXIT PROGRAM.                                                IC2254.2
021900*TERMINATE-CALL.                                                  IC2254.2
022000     STOP     RUN.                                                IC2254.2
022100 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         IC2254.2
022200 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           IC2254.2
022300 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          IC2254.2
022400 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      IC2254.2
022500     MOVE "****TEST DELETED****" TO RE-MARK.                      IC2254.2
022600 PRINT-DETAIL.                                                    IC2254.2
022700     IF REC-CT NOT EQUAL TO ZERO                                  IC2254.2
022800             MOVE "." TO PARDOT-X                                 IC2254.2
022900             MOVE REC-CT TO DOTVALUE.                             IC2254.2
023000     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      IC2254.2
023100     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               IC2254.2
023200        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 IC2254.2
023300          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 IC2254.2
023400     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              IC2254.2
023500     MOVE SPACE TO CORRECT-X.                                     IC2254.2
023600     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         IC2254.2
023700     MOVE     SPACE TO RE-MARK.                                   IC2254.2
023800 HEAD-ROUTINE.                                                    IC2254.2
023900     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IC2254.2
024000     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IC2254.2
024100     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IC2254.2
024200     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IC2254.2
024300 COLUMN-NAMES-ROUTINE.                                            IC2254.2
024400     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IC2254.2
024500     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IC2254.2
024600     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        IC2254.2
024700 END-ROUTINE.                                                     IC2254.2
024800     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.IC2254.2
024900 END-RTN-EXIT.                                                    IC2254.2
025000     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IC2254.2
025100 END-ROUTINE-1.                                                   IC2254.2
025200      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      IC2254.2
025300      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               IC2254.2
025400      ADD PASS-COUNTER TO ERROR-HOLD.                             IC2254.2
025500*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   IC2254.2
025600      MOVE PASS-COUNTER TO CCVS-E-4-1.                            IC2254.2
025700      MOVE ERROR-HOLD TO CCVS-E-4-2.                              IC2254.2
025800      MOVE CCVS-E-4 TO CCVS-E-2-2.                                IC2254.2
025900      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           IC2254.2
026000  END-ROUTINE-12.                                                 IC2254.2
026100      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        IC2254.2
026200     IF       ERROR-COUNTER IS EQUAL TO ZERO                      IC2254.2
026300         MOVE "NO " TO ERROR-TOTAL                                IC2254.2
026400         ELSE                                                     IC2254.2
026500         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       IC2254.2
026600     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           IC2254.2
026700     PERFORM WRITE-LINE.                                          IC2254.2
026800 END-ROUTINE-13.                                                  IC2254.2
026900     IF DELETE-COUNTER IS EQUAL TO ZERO                           IC2254.2
027000         MOVE "NO " TO ERROR-TOTAL  ELSE                          IC2254.2
027100         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      IC2254.2
027200     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   IC2254.2
027300     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IC2254.2
027400      IF   INSPECT-COUNTER EQUAL TO ZERO                          IC2254.2
027500          MOVE "NO " TO ERROR-TOTAL                               IC2254.2
027600      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   IC2254.2
027700      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            IC2254.2
027800      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          IC2254.2
027900     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IC2254.2
028000 WRITE-LINE.                                                      IC2254.2
028100     ADD 1 TO RECORD-COUNT.                                       IC2254.2
028200*    IF RECORD-COUNT GREATER 50                                   IC2254.2
028300*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          IC2254.2
028400*        MOVE SPACE TO DUMMY-RECORD                               IC2254.2
028500*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  IC2254.2
028600*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             IC2254.2
028700*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     IC2254.2
028800*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          IC2254.2
028900*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          IC2254.2
029000*        MOVE ZERO TO RECORD-COUNT.                               IC2254.2
029100     PERFORM WRT-LN.                                              IC2254.2
029200 WRT-LN.                                                          IC2254.2
029300     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               IC2254.2
029400     MOVE SPACE TO DUMMY-RECORD.                                  IC2254.2
029500 BLANK-LINE-PRINT.                                                IC2254.2
029600     PERFORM WRT-LN.                                              IC2254.2
029700 FAIL-ROUTINE.                                                    IC2254.2
029800     IF   COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE. IC2254.2
029900     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.IC2254.2
030000     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IC2254.2
030100     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   IC2254.2
030200     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IC2254.2
030300     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IC2254.2
030400     GO TO  FAIL-ROUTINE-EX.                                      IC2254.2
030500 FAIL-ROUTINE-WRITE.                                              IC2254.2
030600     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         IC2254.2
030700     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 IC2254.2
030800     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. IC2254.2
030900     MOVE   SPACES TO COR-ANSI-REFERENCE.                         IC2254.2
031000 FAIL-ROUTINE-EX. EXIT.                                           IC2254.2
031100 BAIL-OUT.                                                        IC2254.2
031200     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   IC2254.2
031300     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           IC2254.2
031400 BAIL-OUT-WRITE.                                                  IC2254.2
031500     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  IC2254.2
031600     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IC2254.2
031700     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IC2254.2
031800     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IC2254.2
031900 BAIL-OUT-EX. EXIT.                                               IC2254.2
032000 CCVS1-EXIT.                                                      IC2254.2
032100     EXIT.                                                        IC2254.2
032200 SECT-IC225A-001 SECTION.                                         IC2254.2
032300 CALL-TEST-01.                                                    IC2254.2
032400     MOVE   "X-27 5.2.2" TO ANSI-REFERENCE.                       IC2254.2
032500     MOVE   "CALL-TEST-01" TO PAR-NAME.                           IC2254.2
032600     MOVE     "LEV 2 CALL STATEMENT" TO FEATURE.                  IC2254.2
032700     MOVE 0 TO CALL-COUNT.                                        IC2254.2
032800*        THIS TEST HAS CALL STATEMENTS WITH AN IDENTIFIER         IC2254.2
032900*    CONTAINING THE NAME OF THE SUBPROGRAM TO BE CALLED.          IC2254.2
033000*        CALL-TEST-01 CONTAINS THE BASIC LEVEL 2 CALL STATEMENT.  IC2254.2
033100*    IF IT CANNOT BE COMPILED AND EXECUTED CORRECTLY, THERE IS    IC2254.2
033200*    NO USE IN RUNNING THE LEVEL 2 IPC ROUTINES.                  IC2254.2
033300 CALL-INIT-01-01.                                                 IC2254.2
033400     MOVE    1    TO REC-CT.                                      IC2254.2
033500     MOVE    ZERO TO DN3, DN4.                                    IC2254.2
033600 CALL-TEST-01-01-0.                                               IC2254.2
033700     CALL   "IC225A-1" USING BY REFERENCE DN1, DN2,               IC2254.2
033800                                CONTENT   DN3, DN4                IC2254.2
033900     END-CALL.                                                    IC2254.2
034000     GO TO   CALL-TEST-01-01-1.                                   IC2254.2
034100 CALL-DELETE-01-01.                                               IC2254.2
034200     PERFORM DE-LETE.                                             IC2254.2
034300     PERFORM PRINT-DETAIL.                                        IC2254.2
034400     GO TO   CALL-INIT-01-02.                                     IC2254.2
034500 CALL-TEST-01-01-1.                                               IC2254.2
034600     MOVE   "CALL-TEST-01-01-1" TO PAR-NAME.                      IC2254.2
034700     IF      DN1 = ZERO                                           IC2254.2
034800             PERFORM PASS                                         IC2254.2
034900             PERFORM PRINT-DETAIL                                 IC2254.2
035000     ELSE                                                         IC2254.2
035100             MOVE    DN1  TO COMPUTED-N                           IC2254.2
035200             MOVE    ZERO TO CORRECT-N                            IC2254.2
035300             MOVE   "INCORRECT DN1 VALUE RETURNED" TO RE-MARK     IC2254.2
035400             PERFORM FAIL                                         IC2254.2
035500             PERFORM PRINT-DETAIL.                                IC2254.2
035600     ADD     1 TO REC-CT.                                         IC2254.2
035700 CALL-TEST-01-01-2.                                               IC2254.2
035800     MOVE   "CALL-TEST-01-01-2" TO PAR-NAME.                      IC2254.2
035900     IF      DN2 = ZERO                                           IC2254.2
036000             PERFORM PASS                                         IC2254.2
036100             PERFORM PRINT-DETAIL                                 IC2254.2
036200     ELSE                                                         IC2254.2
036300             MOVE    DN2  TO COMPUTED-N                           IC2254.2
036400             MOVE    ZERO TO CORRECT-N                            IC2254.2
036500             MOVE   "VALUE OF DN2 HAS BEEN CHANGED" TO RE-MARK    IC2254.2
036600             PERFORM FAIL                                         IC2254.2
036700             PERFORM PRINT-DETAIL.                                IC2254.2
036800     ADD     1 TO REC-CT.                                         IC2254.2
036900 CALL-TEST-01-01-3.                                               IC2254.2
037000     MOVE   "CALL-TEST-01-01-3" TO PAR-NAME.                      IC2254.2
037100     IF      DN3 = ZERO                                           IC2254.2
037200             PERFORM PASS                                         IC2254.2
037300             PERFORM PRINT-DETAIL                                 IC2254.2
037400     ELSE                                                         IC2254.2
037500             MOVE    DN3  TO COMPUTED-N                           IC2254.2
037600             MOVE    ZERO TO CORRECT-N                            IC2254.2
037700             MOVE   "INCORRECT DN3 VALUE RETURNED" TO RE-MARK     IC2254.2
037800             PERFORM FAIL                                         IC2254.2
037900             PERFORM PRINT-DETAIL.                                IC2254.2
038000     ADD     1 TO REC-CT.                                         IC2254.2
038100 CALL-TEST-01-01-4.                                               IC2254.2
038200     MOVE   "CALL-TEST-01-01-4" TO PAR-NAME.                      IC2254.2
038300     IF      DN4 = ZERO                                           IC2254.2
038400             PERFORM PASS                                         IC2254.2
038500             PERFORM PRINT-DETAIL                                 IC2254.2
038600     ELSE                                                         IC2254.2
038700             MOVE    DN4  TO COMPUTED-N                           IC2254.2
038800             MOVE    ZERO TO CORRECT-N                            IC2254.2
038900             MOVE   "VALUE OF DN4 HAS BEEN CHANGED" TO RE-MARK    IC2254.2
039000             PERFORM FAIL                                         IC2254.2
039100             PERFORM PRINT-DETAIL.                                IC2254.2
039200*                                                                 IC2254.2
039300 CALL-INIT-01-02.                                                 IC2254.2
039400     MOVE    1    TO REC-CT.                                      IC2254.2
039500     MOVE    2    TO DN1, DN2, DN3                                IC2254.2
039600     MOVE   42    TO DN4.                                         IC2254.2
039700 CALL-TEST-01-02-0.                                               IC2254.2
039800     CALL   "IC225A-1" USING BY CONTENT   DN1 DN2                 IC2254.2
039900                                REFERENCE DN3                     IC2254.2
040000                                CONTENT   DN4                     IC2254.2
040100     END-CALL.                                                    IC2254.2
040200     GO TO   CALL-TEST-01-02-1.                                   IC2254.2
040300 CALL-DELETE-01-02.                                               IC2254.2
040400     PERFORM DE-LETE.                                             IC2254.2
040500     PERFORM PRINT-DETAIL.                                        IC2254.2
040600     GO TO   CALL-INIT-01-03.                                     IC2254.2
040700 CALL-TEST-01-02-1.                                               IC2254.2
040800     MOVE   "CALL-TEST-01-02-1" TO PAR-NAME.                      IC2254.2
040900     IF      DN1 = 2                                              IC2254.2
041000             PERFORM PASS                                         IC2254.2
041100             PERFORM PRINT-DETAIL                                 IC2254.2
041200     ELSE                                                         IC2254.2
041300             MOVE    DN1  TO COMPUTED-N                           IC2254.2
041400             MOVE    2    TO CORRECT-N                            IC2254.2
041500             MOVE   "INCORRECT DN1 VALUE RETURNED" TO RE-MARK     IC2254.2
041600             PERFORM FAIL                                         IC2254.2
041700             PERFORM PRINT-DETAIL.                                IC2254.2
041800     ADD     1 TO REC-CT.                                         IC2254.2
041900 CALL-TEST-01-02-2.                                               IC2254.2
042000     MOVE   "CALL-TEST-01-02-2" TO PAR-NAME.                      IC2254.2
042100     IF      DN2 = 2                                              IC2254.2
042200             PERFORM PASS                                         IC2254.2
042300             PERFORM PRINT-DETAIL                                 IC2254.2
042400     ELSE                                                         IC2254.2
042500             MOVE    DN2  TO COMPUTED-N                           IC2254.2
042600             MOVE    2    TO CORRECT-N                            IC2254.2
042700             MOVE   "VALUE OF DN2 HAS BEEN CHANGED" TO RE-MARK    IC2254.2
042800             PERFORM FAIL                                         IC2254.2
042900             PERFORM PRINT-DETAIL.                                IC2254.2
043000     ADD     1 TO REC-CT.                                         IC2254.2
043100 CALL-TEST-01-02-3.                                               IC2254.2
043200     MOVE   "CALL-TEST-01-02-3" TO PAR-NAME.                      IC2254.2
043300     IF      DN3 = 3                                              IC2254.2
043400             PERFORM PASS                                         IC2254.2
043500             PERFORM PRINT-DETAIL                                 IC2254.2
043600     ELSE                                                         IC2254.2
043700             MOVE    DN3  TO COMPUTED-N                           IC2254.2
043800             MOVE    3    TO CORRECT-N                            IC2254.2
043900             MOVE   "INCORRECT DN3 VALUE RETURNED" TO RE-MARK     IC2254.2
044000             PERFORM FAIL                                         IC2254.2
044100             PERFORM PRINT-DETAIL.                                IC2254.2
044200     ADD     1 TO REC-CT.                                         IC2254.2
044300 CALL-TEST-01-02-4.                                               IC2254.2
044400     MOVE   "CALL-TEST-01-02-4" TO PAR-NAME.                      IC2254.2
044500     IF      DN4 = 42                                             IC2254.2
044600             PERFORM PASS                                         IC2254.2
044700             PERFORM PRINT-DETAIL                                 IC2254.2
044800     ELSE                                                         IC2254.2
044900             MOVE    DN4  TO COMPUTED-N                           IC2254.2
045000             MOVE    42   TO CORRECT-N                            IC2254.2
045100             MOVE   "VALUE OF DN4 HAS BEEN CHANGED" TO RE-MARK    IC2254.2
045200             PERFORM FAIL                                         IC2254.2
045300             PERFORM PRINT-DETAIL.                                IC2254.2
045400*                                                                 IC2254.2
045500 CALL-INIT-01-03.                                                 IC2254.2
045600     MOVE    1    TO REC-CT.                                      IC2254.2
045700     MOVE    3    TO DN1, DN2, DN3                                IC2254.2
045800     MOVE   71    TO DN4.                                         IC2254.2
045900 CALL-TEST-01-03-0.                                               IC2254.2
046000     CALL   "IC225A-1" USING BY CONTENT   DN1                     IC2254.2
046100                                REFERENCE DN2                     IC2254.2
046200                                CONTENT   DN3                     IC2254.2
046300                                REFERENCE DN4                     IC2254.2
046400     END-CALL.                                                    IC2254.2
046500     GO TO   CALL-TEST-01-03-1.                                   IC2254.2
046600 CALL-DELETE-01-03.                                               IC2254.2
046700     PERFORM DE-LETE.                                             IC2254.2
046800     PERFORM PRINT-DETAIL.                                        IC2254.2
046900     GO TO   CALL-TEST-02.                                        IC2254.2
047000 CALL-TEST-01-03-1.                                               IC2254.2
047100     MOVE   "CALL-TEST-01-03-1" TO PAR-NAME.                      IC2254.2
047200     IF      DN1 = 3                                              IC2254.2
047300             PERFORM PASS                                         IC2254.2
047400             PERFORM PRINT-DETAIL                                 IC2254.2
047500     ELSE                                                         IC2254.2
047600             MOVE    DN1  TO COMPUTED-N                           IC2254.2
047700             MOVE    3    TO CORRECT-N                            IC2254.2
047800             MOVE   "VALUE OF DN1 HAS BEEN CHANGED" TO RE-MARK    IC2254.2
047900             PERFORM FAIL                                         IC2254.2
048000             PERFORM PRINT-DETAIL.                                IC2254.2
048100     ADD     1 TO REC-CT.                                         IC2254.2
048200 CALL-TEST-01-03-2.                                               IC2254.2
048300     MOVE   "CALL-TEST-01-03-2" TO PAR-NAME.                      IC2254.2
048400     IF      DN2 = 3                                              IC2254.2
048500             PERFORM PASS                                         IC2254.2
048600             PERFORM PRINT-DETAIL                                 IC2254.2
048700     ELSE                                                         IC2254.2
048800             MOVE    DN2  TO COMPUTED-N                           IC2254.2
048900             MOVE    3    TO CORRECT-N                            IC2254.2
049000             MOVE   "INCORRECT DN2 VALUE RETURNED" TO RE-MARK     IC2254.2
049100             PERFORM FAIL                                         IC2254.2
049200             PERFORM PRINT-DETAIL.                                IC2254.2
049300     ADD     1 TO REC-CT.                                         IC2254.2
049400 CALL-TEST-01-03-3.                                               IC2254.2
049500     MOVE   "CALL-TEST-01-03-3" TO PAR-NAME.                      IC2254.2
049600     IF      DN3 = 3                                              IC2254.2
049700             PERFORM PASS                                         IC2254.2
049800             PERFORM PRINT-DETAIL                                 IC2254.2
049900     ELSE                                                         IC2254.2
050000             MOVE    DN3  TO COMPUTED-N                           IC2254.2
050100             MOVE    3    TO CORRECT-N                            IC2254.2
050200             MOVE   "VALUE OF DN3 HAS BEEN CHANGED" TO RE-MARK    IC2254.2
050300             PERFORM FAIL                                         IC2254.2
050400             PERFORM PRINT-DETAIL.                                IC2254.2
050500     ADD     1 TO REC-CT.                                         IC2254.2
050600 CALL-TEST-01-03-4.                                               IC2254.2
050700     MOVE   "CALL-TEST-01-03-4" TO PAR-NAME.                      IC2254.2
050800     IF      DN4 = 3                                              IC2254.2
050900             PERFORM PASS                                         IC2254.2
051000             PERFORM PRINT-DETAIL                                 IC2254.2
051100     ELSE                                                         IC2254.2
051200             MOVE    DN4  TO COMPUTED-N                           IC2254.2
051300             MOVE    3    TO CORRECT-N                            IC2254.2
051400             MOVE   "INCORRECT DN4 VALUE RETURNED" TO RE-MARK     IC2254.2
051500             PERFORM FAIL                                         IC2254.2
051600             PERFORM PRINT-DETAIL.                                IC2254.2
051700*                                                                 IC2254.2
051800 CALL-TEST-02.                                                    IC2254.2
051900     MOVE   "DATA-NAME USED TWICE" TO FEATURE.                    IC2254.2
052000*            THIS TEST USES A DATA-NAME MORE THAN ONCE IN         IC2254.2
052100*            A USING PHRASE OF A CALL STATEMENT.                  IC2254.2
052200 CALL-INIT-02-01.                                                 IC2254.2
052300     MOVE    1 TO REC-CT.                                         IC2254.2
052400     MOVE    1 TO DN1.                                            IC2254.2
052500     MOVE    0 TO DN2, DN3, DN4.                                  IC2254.2
052600 CALL-TEST-02-01-0.                                               IC2254.2
052700     CALL   "IC225A-1" USING REFERENCE DN1,                       IC2254.2
052800                             CONTENT   DN2,                       IC2254.2
052900                             REFERENCE DN1, DN4,                  IC2254.2
053000     END-CALL.                                                    IC2254.2
053100     GO TO   CALL-TEST-02-01-1.                                   IC2254.2
053200 CALL-DELETE-02-01.                                               IC2254.2
053300     PERFORM DE-LETE.                                             IC2254.2
053400     PERFORM PRINT-DETAIL.                                        IC2254.2
053500     GO TO   CALL-INIT-02-02.                                     IC2254.2
053600 CALL-TEST-02-01-1.                                               IC2254.2
053700     MOVE   "CALL-TEST-02-01-1" TO PAR-NAME.                      IC2254.2
053800     IF      DN1 = 2                                              IC2254.2
053900             PERFORM PASS                                         IC2254.2
054000             PERFORM PRINT-DETAIL                                 IC2254.2
054100     ELSE                                                         IC2254.2
054200             MOVE    DN1  TO COMPUTED-N                           IC2254.2
054300             MOVE    2    TO CORRECT-N                            IC2254.2
054400             MOVE   "INCORRECT DN1 VALUE RETURNED" TO RE-MARK     IC2254.2
054500             PERFORM FAIL                                         IC2254.2
054600             PERFORM PRINT-DETAIL.                                IC2254.2
054700     ADD     1 TO REC-CT.                                         IC2254.2
054800 CALL-TEST-02-01-2.                                               IC2254.2
054900     MOVE   "CALL-TEST-02-01-2" TO PAR-NAME.                      IC2254.2
055000     IF      DN2 = 0                                              IC2254.2
055100             PERFORM PASS                                         IC2254.2
055200             PERFORM PRINT-DETAIL                                 IC2254.2
055300     ELSE                                                         IC2254.2
055400             MOVE    DN2  TO COMPUTED-N                           IC2254.2
055500             MOVE    ZERO TO CORRECT-N                            IC2254.2
055600             MOVE   "VALUE OF DN2 HAS BEEN CHANGED" TO RE-MARK    IC2254.2
055700             PERFORM FAIL                                         IC2254.2
055800             PERFORM PRINT-DETAIL.                                IC2254.2
055900     ADD     1 TO REC-CT.                                         IC2254.2
056000 CALL-TEST-02-01-3.                                               IC2254.2
056100     MOVE   "CALL-TEST-02-01-3" TO PAR-NAME.                      IC2254.2
056200     IF      DN3 = 0                                              IC2254.2
056300             PERFORM PASS                                         IC2254.2
056400             PERFORM PRINT-DETAIL                                 IC2254.2
056500     ELSE                                                         IC2254.2
056600             MOVE    DN3  TO COMPUTED-N                           IC2254.2
056700             MOVE    ZERO TO CORRECT-N                            IC2254.2
056800             MOVE   "VALUE OF DN3 HAS BEEN CHANGED" TO RE-MARK    IC2254.2
056900             PERFORM FAIL                                         IC2254.2
057000             PERFORM PRINT-DETAIL.                                IC2254.2
057100     ADD     1 TO REC-CT.                                         IC2254.2
057200 CALL-TEST-02-01-4.                                               IC2254.2
057300     MOVE   "CALL-TEST-02-01-4" TO PAR-NAME.                      IC2254.2
057400     IF      DN4 = 4                                              IC2254.2
057500             PERFORM PASS                                         IC2254.2
057600             PERFORM PRINT-DETAIL                                 IC2254.2
057700     ELSE                                                         IC2254.2
057800             MOVE    DN4  TO COMPUTED-N                           IC2254.2
057900             MOVE    4    TO CORRECT-N                            IC2254.2
058000             MOVE   "INCORRECT DN4 VALUE RETURNED" TO RE-MARK     IC2254.2
058100             PERFORM FAIL                                         IC2254.2
058200             PERFORM PRINT-DETAIL.                                IC2254.2
058300*                                                                 IC2254.2
058400 CALL-INIT-02-02.                                                 IC2254.2
058500     MOVE 1 TO REC-CT.                                            IC2254.2
058600     MOVE 0 TO DN4, DN3, DN2, DN1.                                IC2254.2
058700 CALL-TEST-02-02-0.                                               IC2254.2
058800     CALL ID1 USING BY REFERENCE DN1                              IC2254.2
058900                       CONTENT   DN2 DN3 DN2                      IC2254.2
059000     END-CALL.                                                    IC2254.2
059100     GO TO   CALL-TEST-02-02-1.                                   IC2254.2
059200 CALL-DELETE-02-02.                                               IC2254.2
059300     PERFORM DE-LETE.                                             IC2254.2
059400     PERFORM PRINT-DETAIL.                                        IC2254.2
059500     GO TO   CALL-INIT-02-03.                                     IC2254.2
059600 CALL-TEST-02-02-1.                                               IC2254.2
059700     MOVE   "CALL-TEST-02-02-1" TO PAR-NAME.                      IC2254.2
059800     IF      DN1 = 0                                              IC2254.2
059900             PERFORM PASS                                         IC2254.2
060000             PERFORM PRINT-DETAIL                                 IC2254.2
060100     ELSE                                                         IC2254.2
060200             MOVE    DN1  TO COMPUTED-N                           IC2254.2
060300             MOVE    ZERO TO CORRECT-N                            IC2254.2
060400             MOVE   "INCORRECT DN1 VALUE RETURNED" TO RE-MARK     IC2254.2
060500             PERFORM FAIL                                         IC2254.2
060600             PERFORM PRINT-DETAIL.                                IC2254.2
060700     ADD     1 TO REC-CT.                                         IC2254.2
060800 CALL-TEST-02-02-2.                                               IC2254.2
060900     MOVE   "CALL-TEST-02-02-2" TO PAR-NAME.                      IC2254.2
061000     IF      DN2 = 0                                              IC2254.2
061100             PERFORM PASS                                         IC2254.2
061200             PERFORM PRINT-DETAIL                                 IC2254.2
061300     ELSE                                                         IC2254.2
061400             MOVE    DN2  TO COMPUTED-N                           IC2254.2
061500             MOVE    ZERO TO CORRECT-N                            IC2254.2
061600             MOVE   "VALUE OF DN2 HAS BEEN CHANGED" TO RE-MARK    IC2254.2
061700             PERFORM FAIL                                         IC2254.2
061800             PERFORM PRINT-DETAIL.                                IC2254.2
061900     ADD     1 TO REC-CT.                                         IC2254.2
062000 CALL-TEST-02-02-3.                                               IC2254.2
062100     MOVE   "CALL-TEST-02-02-3" TO PAR-NAME.                      IC2254.2
062200     IF      DN3 = 0                                              IC2254.2
062300             PERFORM PASS                                         IC2254.2
062400             PERFORM PRINT-DETAIL                                 IC2254.2
062500     ELSE                                                         IC2254.2
062600             MOVE    DN3  TO COMPUTED-N                           IC2254.2
062700             MOVE    ZERO TO CORRECT-N                            IC2254.2
062800             MOVE   "VALUE OF DN3 HAS BEEN CHANGED" TO RE-MARK    IC2254.2
062900             PERFORM FAIL                                         IC2254.2
063000             PERFORM PRINT-DETAIL.                                IC2254.2
063100     ADD     1 TO REC-CT.                                         IC2254.2
063200 CALL-TEST-02-02-4.                                               IC2254.2
063300     MOVE   "CALL-TEST-02-02-4" TO PAR-NAME.                      IC2254.2
063400     IF      DN4 = ZERO                                           IC2254.2
063500             PERFORM PASS                                         IC2254.2
063600             PERFORM PRINT-DETAIL                                 IC2254.2
063700     ELSE                                                         IC2254.2
063800             MOVE    DN4  TO COMPUTED-N                           IC2254.2
063900             MOVE    ZERO TO CORRECT-N                            IC2254.2
064000             MOVE   "VALUE OF DN4 HAS BEEN CHANGED" TO RE-MARK    IC2254.2
064100             PERFORM FAIL                                         IC2254.2
064200             PERFORM PRINT-DETAIL.                                IC2254.2
064300*                                                                 IC2254.2
064400 CALL-INIT-02-03.                                                 IC2254.2
064500     MOVE 1 TO REC-CT.                                            IC2254.2
064600     MOVE 0 TO DN4, DN3.                                          IC2254.2
064700     MOVE 10 TO DN2.                                              IC2254.2
064800     MOVE 25 TO DN1.                                              IC2254.2
064900 CALL-TEST-02-03-0.                                               IC2254.2
065000     CALL ID1 USING CONTENT   DN1                                 IC2254.2
065100                    REFERENCE DN2 DN1                             IC2254.2
065200                    REFERENCE DN2                                 IC2254.2
065300     END-CALL.                                                    IC2254.2
065400     GO TO   CALL-TEST-02-03-1.                                   IC2254.2
065500 CALL-DELETE-02-03.                                               IC2254.2
065600     PERFORM DE-LETE.                                             IC2254.2
065700     PERFORM PRINT-DETAIL.                                        IC2254.2
065800     GO TO   CALL-INIT-03-01.                                     IC2254.2
065900 CALL-TEST-02-03-1.                                               IC2254.2
066000     MOVE   "CALL-TEST-02-03-1" TO PAR-NAME.                      IC2254.2
066100     IF      DN1 = 26                                             IC2254.2
066200             PERFORM PASS                                         IC2254.2
066300             PERFORM PRINT-DETAIL                                 IC2254.2
066400     ELSE                                                         IC2254.2
066500             MOVE    DN1  TO COMPUTED-N                           IC2254.2
066600             MOVE    26   TO CORRECT-N                            IC2254.2
066700             MOVE   "INCORRECT VALUE RETURNED     " TO RE-MARK    IC2254.2
066800             PERFORM FAIL                                         IC2254.2
066900             PERFORM PRINT-DETAIL.                                IC2254.2
067000     ADD     1 TO REC-CT.                                         IC2254.2
067100 CALL-TEST-02-03-2.                                               IC2254.2
067200     MOVE   "CALL-TEST-02-03-2" TO PAR-NAME.                      IC2254.2
067300     IF      DN2 = 6                                              IC2254.2
067400             PERFORM PASS                                         IC2254.2
067500             PERFORM PRINT-DETAIL                                 IC2254.2
067600     ELSE                                                         IC2254.2
067700             MOVE    DN2  TO COMPUTED-N                           IC2254.2
067800             MOVE    6    TO CORRECT-N                            IC2254.2
067900             MOVE   "INCORRECT DN2 VALUE RETURNED" TO RE-MARK     IC2254.2
068000             PERFORM FAIL                                         IC2254.2
068100             PERFORM PRINT-DETAIL.                                IC2254.2
068200     ADD     1 TO REC-CT.                                         IC2254.2
068300 CALL-TEST-02-03-3.                                               IC2254.2
068400     MOVE   "CALL-TEST-02-03-3" TO PAR-NAME.                      IC2254.2
068500     IF      DN3 = 0                                              IC2254.2
068600             PERFORM PASS                                         IC2254.2
068700             PERFORM PRINT-DETAIL                                 IC2254.2
068800     ELSE                                                         IC2254.2
068900             MOVE    DN3  TO COMPUTED-N                           IC2254.2
069000             MOVE    ZERO TO CORRECT-N                            IC2254.2
069100             MOVE   "VALUE OF DN3 HAS BEEN CHANGED" TO RE-MARK    IC2254.2
069200             PERFORM FAIL                                         IC2254.2
069300             PERFORM PRINT-DETAIL.                                IC2254.2
069400     ADD     1 TO REC-CT.                                         IC2254.2
069500 CALL-TEST-02-03-4.                                               IC2254.2
069600     MOVE   "CALL-TEST-02-03-4" TO PAR-NAME.                      IC2254.2
069700     IF      DN4 = ZERO                                           IC2254.2
069800             PERFORM PASS                                         IC2254.2
069900             PERFORM PRINT-DETAIL                                 IC2254.2
070000     ELSE                                                         IC2254.2
070100             MOVE    DN4  TO COMPUTED-N                           IC2254.2
070200             MOVE    ZERO TO CORRECT-N                            IC2254.2
070300             MOVE   "VALUE OF DN4 HAS BEEN CHANGED" TO RE-MARK    IC2254.2
070400             PERFORM FAIL                                         IC2254.2
070500             PERFORM PRINT-DETAIL.                                IC2254.2
070600*                                                                 IC2254.2
070700 CALL-TEST-03.                                                    IC2254.2
070800*    THIS TEST USES THE ON OVERFLOW PHRASE IN THE CALL            IC2254.2
070900*    STATEMENT.  THIS IS A SYNTACTICAL CHECK ONLY, THE            IC2254.2
071000*    ON OVERFLOW CONDITION SHOULD NEVER OCCUR.                    IC2254.2
071100     MOVE "CALL-TEST-03" TO PAR-NAME.                             IC2254.2
071200     MOVE "ON OVERFLOW PHRASE" TO FEATURE.                        IC2254.2
071300 CALL-INIT-03-01.                                                 IC2254.2
071400     MOVE    1  TO REC-CT.                                        IC2254.2
071500     MOVE    6  TO CALL-COUNT.                                    IC2254.2
071600     MOVE    20 TO DN1.                                           IC2254.2
071700     MOVE 30 TO DN2.                                              IC2254.2
071800     MOVE ZERO TO DN3, DN4.                                       IC2254.2
071900 CALL-TEST-03-01-0.                                               IC2254.2
072000     MOVE   "CALL-TEST-03-01-0" TO PAR-NAME.                      IC2254.2
072100     CALL "IC225A-1" USING BY CONTENT   DN1, DN2,                 IC2254.2
072200                              REFERENCE DN3, DN4;                 IC2254.2
072300         ON OVERFLOW MOVE "OVERFLOW SHOULD NOT OCCUR" TO RE-MARK  IC2254.2
072400                     PERFORM FAIL                                 IC2254.2
072500                     PERFORM PRINT-DETAIL.                        IC2254.2
072600     GO TO   CALL-TEST-03-01-1.                                   IC2254.2
072700 CALL-DELETE-03-01.                                               IC2254.2
072800     PERFORM DE-LETE.                                             IC2254.2
072900     PERFORM PRINT-DETAIL.                                        IC2254.2
073000     GO TO   CALL-INIT-03-02.                                     IC2254.2
073100 CALL-TEST-03-01-1.                                               IC2254.2
073200     MOVE   "CALL-TEST-03-01-1" TO PAR-NAME.                      IC2254.2
073300     IF      DN1 = 20                                             IC2254.2
073400             PERFORM PASS                                         IC2254.2
073500             PERFORM PRINT-DETAIL                                 IC2254.2
073600     ELSE                                                         IC2254.2
073700             MOVE    DN1  TO COMPUTED-N                           IC2254.2
073800             MOVE    20   TO CORRECT-N                            IC2254.2
073900             MOVE   "VALUE OF DN1 HAS BEEN CHANGED" TO RE-MARK    IC2254.2
074000             PERFORM FAIL                                         IC2254.2
074100             PERFORM PRINT-DETAIL.                                IC2254.2
074200     ADD     1 TO REC-CT.                                         IC2254.2
074300 CALL-TEST-03-01-2.                                               IC2254.2
074400     MOVE   "CALL-TEST-03-01-2" TO PAR-NAME.                      IC2254.2
074500     IF      DN2 = 30                                             IC2254.2
074600             PERFORM PASS                                         IC2254.2
074700             PERFORM PRINT-DETAIL                                 IC2254.2
074800     ELSE                                                         IC2254.2
074900             MOVE    DN2  TO COMPUTED-N                           IC2254.2
075000             MOVE    30   TO CORRECT-N                            IC2254.2
075100             MOVE   "VALUE OF DN2 HAS BEEN CHANGED" TO RE-MARK    IC2254.2
075200             PERFORM FAIL                                         IC2254.2
075300             PERFORM PRINT-DETAIL.                                IC2254.2
075400     ADD     1 TO REC-CT.                                         IC2254.2
075500 CALL-TEST-03-01-3.                                               IC2254.2
075600     MOVE   "CALL-TEST-03-01-3" TO PAR-NAME.                      IC2254.2
075700     IF      DN3 = 21                                             IC2254.2
075800             PERFORM PASS                                         IC2254.2
075900             PERFORM PRINT-DETAIL                                 IC2254.2
076000     ELSE                                                         IC2254.2
076100             MOVE    DN3  TO COMPUTED-N                           IC2254.2
076200             MOVE    21   TO CORRECT-N                            IC2254.2
076300             MOVE   "INCORRECT DN3 VALUE RETURNED" TO RE-MARK     IC2254.2
076400             PERFORM FAIL                                         IC2254.2
076500             PERFORM PRINT-DETAIL.                                IC2254.2
076600     ADD     1 TO REC-CT.                                         IC2254.2
076700 CALL-TEST-03-01-4.                                               IC2254.2
076800     MOVE   "CALL-TEST-03-01-4" TO PAR-NAME.                      IC2254.2
076900     IF      DN4 = 7                                              IC2254.2
077000             PERFORM PASS                                         IC2254.2
077100             PERFORM PRINT-DETAIL                                 IC2254.2
077200     ELSE                                                         IC2254.2
077300             MOVE    DN4  TO COMPUTED-N                           IC2254.2
077400             MOVE    7    TO CORRECT-N                            IC2254.2
077500             MOVE   "INCORRECT DN4 VALUE RETURNED" TO RE-MARK     IC2254.2
077600             PERFORM FAIL                                         IC2254.2
077700             PERFORM PRINT-DETAIL.                                IC2254.2
077800*                                                                 IC2254.2
077900 CALL-INIT-03-02.                                                 IC2254.2
078000     MOVE   "CALL-TEST-03-02-0" TO PAR-NAME.                      IC2254.2
078100     MOVE    0 TO DN3, DN4.                                       IC2254.2
078200     MOVE    1 TO REC-CT.                                         IC2254.2
078300 CALL-TEST-03-02-0.                                               IC2254.2
078400     CALL "IC225A-1" USING REFERENCE DN1,                         IC2254.2
078500                           CONTENT   DN2,                         IC2254.2
078600                           REFERENCE DN3,                         IC2254.2
078700                           CONTENT   DN4,                         IC2254.2
078800         OVERFLOW MOVE "OVERFLOW SHOULD NOT OCCUR" TO RE-MARK     IC2254.2
078900                     PERFORM FAIL                                 IC2254.2
079000                     PERFORM PRINT-DETAIL.                        IC2254.2
079100     GO TO   CALL-TEST-03-02-1.                                   IC2254.2
079200 CALL-DELETE-03-02.                                               IC2254.2
079300     PERFORM DE-LETE.                                             IC2254.2
079400     PERFORM PRINT-DETAIL.                                        IC2254.2
079500     GO TO   CALL-INIT-03-03.                                     IC2254.2
079600 CALL-TEST-03-02-1.                                               IC2254.2
079700     MOVE   "CALL-TEST-03-02-1" TO PAR-NAME.                      IC2254.2
079800     IF      DN1 = 20                                             IC2254.2
079900             PERFORM PASS                                         IC2254.2
080000             PERFORM PRINT-DETAIL                                 IC2254.2
080100     ELSE                                                         IC2254.2
080200             MOVE    DN1  TO COMPUTED-N                           IC2254.2
080300             MOVE    20   TO CORRECT-N                            IC2254.2
080400             MOVE   "VALUE OF DN1 HAS BEEN CHANGED" TO RE-MARK    IC2254.2
080500             PERFORM FAIL                                         IC2254.2
080600             PERFORM PRINT-DETAIL.                                IC2254.2
080700     ADD     1 TO REC-CT.                                         IC2254.2
080800 CALL-TEST-03-02-2.                                               IC2254.2
080900     MOVE   "CALL-TEST-03-02-2" TO PAR-NAME.                      IC2254.2
081000     IF      DN2 = 30                                             IC2254.2
081100             PERFORM PASS                                         IC2254.2
081200             PERFORM PRINT-DETAIL                                 IC2254.2
081300     ELSE                                                         IC2254.2
081400             MOVE    DN2  TO COMPUTED-N                           IC2254.2
081500             MOVE    30   TO CORRECT-N                            IC2254.2
081600             MOVE   "VALUE OF DN2 HAS BEEN CHANGED" TO RE-MARK    IC2254.2
081700             PERFORM FAIL                                         IC2254.2
081800             PERFORM PRINT-DETAIL.                                IC2254.2
081900     ADD     1 TO REC-CT.                                         IC2254.2
082000 CALL-TEST-03-02-3.                                               IC2254.2
082100     MOVE   "CALL-TEST-03-02-3" TO PAR-NAME.                      IC2254.2
082200     IF      DN3 = 21                                             IC2254.2
082300             PERFORM PASS                                         IC2254.2
082400             PERFORM PRINT-DETAIL                                 IC2254.2
082500     ELSE                                                         IC2254.2
082600             MOVE    DN3  TO COMPUTED-N                           IC2254.2
082700             MOVE    21   TO CORRECT-N                            IC2254.2
082800             MOVE   "INCORRECT DN3 VALUE RETURNED" TO RE-MARK     IC2254.2
082900             PERFORM FAIL                                         IC2254.2
083000             PERFORM PRINT-DETAIL.                                IC2254.2
083100     ADD     1 TO REC-CT.                                         IC2254.2
083200 CALL-TEST-03-02-4.                                               IC2254.2
083300     MOVE   "CALL-TEST-03-02-4" TO PAR-NAME.                      IC2254.2
083400     IF      DN4 = 0                                              IC2254.2
083500             PERFORM PASS                                         IC2254.2
083600             PERFORM PRINT-DETAIL                                 IC2254.2
083700     ELSE                                                         IC2254.2
083800             MOVE    DN4  TO COMPUTED-N                           IC2254.2
083900             MOVE    ZERO TO CORRECT-N                            IC2254.2
084000             MOVE   "VALUE OF DN4 HAS BEEN CHANGED" TO RE-MARK    IC2254.2
084100             PERFORM FAIL                                         IC2254.2
084200             PERFORM PRINT-DETAIL.                                IC2254.2
084300*                                                                 IC2254.2
084400 CALL-INIT-03-03.                                                 IC2254.2
084500     MOVE   "CALL-TEST-03-03-0" TO PAR-NAME.                      IC2254.2
084600     MOVE    0 TO DN3, DN4.                                       IC2254.2
084700     MOVE    1 TO REC-CT.                                         IC2254.2
084800 CALL-TEST-03-03-0.                                               IC2254.2
084900     CALL ID1 USING BY CONTENT DN1                                IC2254.2
085000                       REFERENCE DN2 DN3 DN4                      IC2254.2
085100         ON OVERFLOW MOVE "OVERFLOW SHOULD NOT OCCUR" TO RE-MARK  IC2254.2
085200             PERFORM FAIL                                         IC2254.2
085300             PERFORM PRINT-DETAIL.                                IC2254.2
085400     GO TO   CALL-TEST-03-03-1.                                   IC2254.2
085500 CALL-DELETE-03-03.                                               IC2254.2
085600     PERFORM DE-LETE.                                             IC2254.2
085700     PERFORM PRINT-DETAIL.                                        IC2254.2
085800     GO TO   CALL-INIT-03-03.                                     IC2254.2
085900 CALL-TEST-03-03-1.                                               IC2254.2
086000     MOVE   "CALL-TEST-03-03-1" TO PAR-NAME.                      IC2254.2
086100     IF      DN1 = 20                                             IC2254.2
086200             PERFORM PASS                                         IC2254.2
086300             PERFORM PRINT-DETAIL                                 IC2254.2
086400     ELSE                                                         IC2254.2
086500             MOVE    DN1  TO COMPUTED-N                           IC2254.2
086600             MOVE    20   TO CORRECT-N                            IC2254.2
086700             MOVE   "VALUE OF DN1 HAS BEEN CHANGED" TO RE-MARK    IC2254.2
086800             PERFORM FAIL                                         IC2254.2
086900             PERFORM PRINT-DETAIL.                                IC2254.2
087000     ADD     1 TO REC-CT.                                         IC2254.2
087100 CALL-TEST-03-03-2.                                               IC2254.2
087200     MOVE   "CALL-TEST-03-03-2" TO PAR-NAME.                      IC2254.2
087300     IF      DN2 = 30                                             IC2254.2
087400             PERFORM PASS                                         IC2254.2
087500             PERFORM PRINT-DETAIL                                 IC2254.2
087600     ELSE                                                         IC2254.2
087700             MOVE    DN2  TO COMPUTED-N                           IC2254.2
087800             MOVE    30   TO CORRECT-N                            IC2254.2
087900             MOVE   "VALUE OF DN2 HAS BEEN CHANGED" TO RE-MARK    IC2254.2
088000             PERFORM FAIL                                         IC2254.2
088100             PERFORM PRINT-DETAIL.                                IC2254.2
088200     ADD     1 TO REC-CT.                                         IC2254.2
088300 CALL-TEST-03-03-3.                                               IC2254.2
088400     MOVE   "CALL-TEST-03-03-3" TO PAR-NAME.                      IC2254.2
088500     IF      DN3 = 21                                             IC2254.2
088600             PERFORM PASS                                         IC2254.2
088700             PERFORM PRINT-DETAIL                                 IC2254.2
088800     ELSE                                                         IC2254.2
088900             MOVE    DN3  TO COMPUTED-N                           IC2254.2
089000             MOVE    21   TO CORRECT-N                            IC2254.2
089100             MOVE   "INCORRECT DN3 VALUE RETURNED" TO RE-MARK     IC2254.2
089200             PERFORM FAIL                                         IC2254.2
089300             PERFORM PRINT-DETAIL.                                IC2254.2
089400     ADD     1 TO REC-CT.                                         IC2254.2
089500 CALL-TEST-03-03-4.                                               IC2254.2
089600     MOVE   "CALL-TEST-03-03-4" TO PAR-NAME.                      IC2254.2
089700     IF      DN4 = 9                                              IC2254.2
089800             PERFORM PASS                                         IC2254.2
089900             PERFORM PRINT-DETAIL                                 IC2254.2
090000     ELSE                                                         IC2254.2
090100             MOVE    DN4  TO COMPUTED-N                           IC2254.2
090200             MOVE    9    TO CORRECT-N                            IC2254.2
090300             MOVE   "VALUE OF DN4 HAS BEEN CHANGED" TO RE-MARK    IC2254.2
090400             PERFORM FAIL                                         IC2254.2
090500             PERFORM PRINT-DETAIL.                                IC2254.2
090600*                                                                 IC2254.2
090700     GO TO EXIT-IC225A.                                           IC2254.2
090800*                                                                 IC2254.2
090900 CALL-DELETE-03.                                                  IC2254.2
091000*        IF THE ON OVERFLOW PHRASE IS NOT RECOGNIZED, DELETE ALL  IC2254.2
091100*    OF THE ABOVE CALL-TEST-03 PARAGRAPHS, STARTING WITH          IC2254.2
091200*    CALL-TEST-03-01.                                             IC2254.2
091300     PERFORM DE-LETE.                                             IC2254.2
091400     PERFORM PRINT-DETAIL.                                        IC2254.2
091500 EXIT-IC225A.                                                     IC2254.2
091600     GO TO CCVS-EXIT.                                             IC2254.2
091700 SECT-IC225A-002 SECTION.                                         IC2254.2
091800 CHECK-TEST-03.                                                   IC2254.2
091900     MOVE ZERO TO FAIL-FLAG.                                      IC2254.2
092000     ADD 1 TO CALL-COUNT.                                         IC2254.2
092100     IF DN4 NOT EQUAL TO CALL-COUNT                               IC2254.2
092200         ADD 1 TO FAIL-FLAG.                                      IC2254.2
092300     IF DN3 NOT EQUAL TO 21                                       IC2254.2
092400         ADD 1 TO FAIL-FLAG.                                      IC2254.2
092500     IF DN2 NOT EQUAL TO 30                                       IC2254.2
092600         ADD 1 TO FAIL-FLAG.                                      IC2254.2
092700     IF DN1 NOT EQUAL TO 20                                       IC2254.2
092800         ADD 1 TO FAIL-FLAG.                                      IC2254.2
092900 CCVS-EXIT SECTION.                                               IC2254.2
093000 CCVS-999999.                                                     IC2254.2
093100     GO TO CLOSE-FILES.                                           IC2254.2
093200 END PROGRAM IC225A.                                              IC2254.2
093300 IDENTIFICATION DIVISION.                                         IC2254.2
093400 PROGRAM-ID.                                                      IC2254.2
093500     IC225A-1.                                                    IC2254.2
093600****************************************************************  IC2254.2
093700*                                                              *  IC2254.2
093800*    THIS PROGRAM FORMS PART OF THE COBOL COMPILER VALIDATION  *  IC2254.2
093900*    SYSTEM (CCVS) USED TO TEST COBOL COMPILERS FOR            *  IC2254.2
094000*    CONFORMANCE WITH THE AMERICAN NATIONAL STANDARD           *  IC2254.2
094100*    (ANSI DOCUMENT REFERENCE: X3.23-1985) AND THE STANDARD OF *  IC2254.2
094200*    THE INTERNATIONAL ORGANIZATION FOR STANDARDISATION        *  IC2254.2
094300*    (ISO DOCUMENT REFERENCE: ISO-1989-1985).                  *  IC2254.2
094400*                                                              *  IC2254.2
094500*    THIS CCVS INCORPORATES ENHANCEMENTS TO THE CCVS FOR THE   *  IC2254.2
094600*    1974 STANDARD (ANSI DOCUMENT REFERENCE: X3.23-1974; ISO   *  IC2254.2
094700*    DOCUMENT REFERENCE: ISO-1989-1978).                       *  IC2254.2
094800*                                                              *  IC2254.2
094900*    THESE ENHANCEMENTS WERE SPECIFIED BY A PROJECT TEAM WHICH *  IC2254.2
095000*    WAS FUNDED BY THE COMMISSION FOR EUROPEAN COMMUNITIES AND *  IC2254.2
095100*    WHICH WAS RESPONSIBLE FOR TECHNICAL ISSUES TO:            *  IC2254.2
095200*                                                              *  IC2254.2
095300*          THE FEDERAL SOFTWARE TESTING CENTER                 *  IC2254.2
095400*          OFFICE OF SOFTWARE DEVELOPMENT                      *  IC2254.2
095500*                & INFORMATION TECHNOLOGY                      *  IC2254.2
095600*          TWO SKYLINE PLACE                                   *  IC2254.2
095700*          SUITE 1100                                          *  IC2254.2
095800*          5203 LEESBURG PIKE                                  *  IC2254.2
095900*          FALLS CHURCH                                        *  IC2254.2
096000*          VA 22041                                            *  IC2254.2
096100*          U.S.A.                                              *  IC2254.2
096200*                                                              *  IC2254.2
096300*    THE PROJECT TEAM MEMBERS WERE:                            *  IC2254.2
096400*                                                              *  IC2254.2
096500*          BIADI (BUREAU INTER ADMINISTRATION                  *  IC2254.2
096600*                 DE DOCUMENTATION INFORMATIQUE)               *  IC2254.2
096700*          21 RUE BARA                                         *  IC2254.2
096800*          F-92132 ISSY                                        *  IC2254.2
096900*          FRANCE                                              *  IC2254.2
097000*                                                              *  IC2254.2
097100*                                                              *  IC2254.2
097200*          GMD (GESELLSCHAFT FUR MATHEMATIK                    *  IC2254.2
097300*               UND DATENVERARBEITUNG MBH)                     *  IC2254.2
097400*          SCHLOSS BIRLINGHOVEN                                *  IC2254.2
097500*          POSTFACH 12 40                                      *  IC2254.2
097600*          D-5205 ST. AUGUSTIN 1                               *  IC2254.2
097700*          GERMANY FR                                          *  IC2254.2
097800*                                                              *  IC2254.2
097900*                                                              *  IC2254.2
098000*          NCC (THE NATIONAL COMPUTING CENTRE LTD)             *  IC2254.2
098100*          OXFORD ROAD                                         *  IC2254.2
098200*          MANCHESTER                                          *  IC2254.2
098300*          M1 7ED                                              *  IC2254.2
098400*          UNITED KINGDOM                                      *  IC2254.2
098500*                                                              *  IC2254.2
098600*                                                              *  IC2254.2
098700*    THIS TEST SUITE WAS PRODUCED BY THE NATIONAL COMPUTING    *  IC2254.2
098800*    CENTRE IN ENGLAND AND IS THE OFFICIAL CCVS TEST SUITE     *  IC2254.2
098900*    USED THROUGHOUT EUROPE AND THE UNITED STATES OF AMERICA.  *  IC2254.2
099000*                                                              *  IC2254.2
099100****************************************************************  IC2254.2
099200*                                                              *  IC2254.2
099300*    VALIDATION FOR:-                                          *  IC2254.2
099400*                                                              *  IC2254.2
099500*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IC2254.2
099600*                                                              *  IC2254.2
099700*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IC2254.2
099800*                                                              *  IC2254.2
099900****************************************************************  IC2254.2
100000*                                                              *  IC2254.2
100100*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  IC2254.2
100200*                                                              *  IC2254.2
100300*        X-55  - SYSTEM PRINTER NAME.                          *  IC2254.2
100400*        X-82  - SOURCE COMPUTER NAME.                         *  IC2254.2
100500*        X-83  - OBJECT COMPUTER NAME.                         *  IC2254.2
100600*                                                              *  IC2254.2
100700****************************************************************  IC2254.2
100800*                                                              *  IC2254.2
100900*    PROGRAM IC225A AND IC225A-1 WILL TEST THE NEW LANGUAGE    *  IC2254.2
101000*    ELEMENTS FOR THE LEVEL 2 INTER-PROGRAM COMMUNICATION      *  IC2254.2
101100*    MODULE.                                                   *  IC2254.2
101200*    THE NEW LANGUAGE ELEMENTS TO BE TESTED WILL BE:           *  IC2254.2
101300*          "BY REFERENCE"     PHRASE                           *  IC2254.2
101400*    THE TWO PROGRAMS WILL BE COMPILED IN THE SAME FLOW        *  IC2254.2
101500*    (TO TEST THE "END PROGRAM" STATEMENT) AS SHOWN BELOW:     *  IC2254.2
101600*    IDENTIFICATION DIVISION.                                  *  IC2254.2
101700*    PROGRAM-ID. IC225A.                                       *  IC2254.2
101800*              .                                               *  IC2254.2
101900*              .                                               *  IC2254.2
102000*              .                                               *  IC2254.2
102100*    END PROGRAM IC225A.                                       *  IC2254.2
102200*    PROGRAM-ID. IC225A-1.                                     *  IC2254.2
102300*              .                                               *  IC2254.2
102400*              .                                               *  IC2254.2
102500*              .                                               *  IC2254.2
102600****************************************************************  IC2254.2
102700 ENVIRONMENT DIVISION.                                            IC2254.2
102800 CONFIGURATION SECTION.                                           IC2254.2
102900 SOURCE-COMPUTER.                                                 IC2254.2
103000     XXXXX082.                                                    IC2254.2
103100 OBJECT-COMPUTER.                                                 IC2254.2
103200     XXXXX083.                                                    IC2254.2
103300*INPUT-OUTPUT SECTION.                                            IC2254.2
103400 DATA DIVISION.                                                   IC2254.2
103500 FILE SECTION.                                                    IC2254.2
103600 WORKING-STORAGE SECTION.                                         IC2254.2
103700 77  WS1 PICTURE S999.                                            IC2254.2
103800 77  WS2 PICTURE S999                                             IC2254.2
103900         USAGE COMPUTATIONAL, VALUE ZERO.                         IC2254.2
104000 LINKAGE SECTION.                                                 IC2254.2
104100 77  DN1 PICTURE S99.                                             IC2254.2
104200 77  DN2 PICTURE S99 USAGE COMPUTATIONAL.                         IC2254.2
104300 77  DN3 PICTURE S99.                                             IC2254.2
104400 77  DN4 PICTURE S99 USAGE COMPUTATIONAL.                         IC2254.2
104500 PROCEDURE DIVISION USING DN1, DN2, DN3, DN4.                     IC2254.2
104600 SECT-IC225A-1-001 SECTION.                                       IC2254.2
104700 CALL-TEST-001.                                                   IC2254.2
104800     MOVE DN1 TO WS1.                                             IC2254.2
104900     ADD 1 TO WS1.                                                IC2254.2
105000     ADD 1 TO WS2.                                                IC2254.2
105100     MOVE WS1 TO DN3.                                             IC2254.2
105200     MOVE WS2 TO DN4.                                             IC2254.2
105300 CALL-EXIT-001.                                                   IC2254.2
105400     EXIT PROGRAM.                                                IC2254.2
      *END-OF,IC225A                                                                  
