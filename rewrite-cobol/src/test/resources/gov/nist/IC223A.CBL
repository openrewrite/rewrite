      *HEADER,COBOL,IC223A                                                            
000100 IDENTIFICATION DIVISION.                                         IC2234.2
000200 PROGRAM-ID.                                                      IC2234.2
000300     IC223A.                                                      IC2234.2
000400****************************************************************  IC2234.2
000500*                                                              *  IC2234.2
000600*    VALIDATION FOR:-                                          *  IC2234.2
000700*                                                              *  IC2234.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IC2234.2
000900*                                                              *  IC2234.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IC2234.2
001100*                                                              *  IC2234.2
001200****************************************************************  IC2234.2
001300*                                                              *  IC2234.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  IC2234.2
001500*                                                              *  IC2234.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  IC2234.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  IC2234.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  IC2234.2
001900*                                                              *  IC2234.2
002000****************************************************************  IC2234.2
002100*                                                              *  IC2234.2
002200*    PROGRAM IC223A AND IC223A-1 WILL TEST THE NEW LANGUAGE    *  IC2234.2
002300*    ELEMENTS FOR THE LEVEL 2 INTER-PROGRAM COMMUNICATION      *  IC2234.2
002400*    MODULE.                                                   *  IC2234.2
002500*    THE NEW LANGUAGE ELEMENTS TO BE TESTED WILL BE:           *  IC2234.2
002600*          "BY REFERENCE"     PHRASE                           *  IC2234.2
002700*    THE TWO PROGRAMS WILL BE COMPILED IN THE SAME FLOW        *  IC2234.2
002800*    (TO TEST THE "END PROGRAM" STATEMENT) AS SHOWN BELOW:     *  IC2234.2
002900*    IDENTIFICATION DIVISION.                                  *  IC2234.2
003000*    PROGRAM-ID. IC223A.                                       *  IC2234.2
003100*              .                                               *  IC2234.2
003200*              .                                               *  IC2234.2
003300*              .                                               *  IC2234.2
003400*    END PROGRAM IC223A.                                       *  IC2234.2
003500*    PROGRAM-ID. IC223A-1.                                     *  IC2234.2
003600*              .                                               *  IC2234.2
003700*              .                                               *  IC2234.2
003800*              .                                               *  IC2234.2
003900****************************************************************  IC2234.2
004000 ENVIRONMENT DIVISION.                                            IC2234.2
004100 CONFIGURATION SECTION.                                           IC2234.2
004200 SOURCE-COMPUTER.                                                 IC2234.2
004300     XXXXX082.                                                    IC2234.2
004400 OBJECT-COMPUTER.                                                 IC2234.2
004500     XXXXX083.                                                    IC2234.2
004600 INPUT-OUTPUT SECTION.                                            IC2234.2
004700 FILE-CONTROL.                                                    IC2234.2
004800     SELECT PRINT-FILE ASSIGN TO                                  IC2234.2
004900     XXXXX055.                                                    IC2234.2
005000 DATA DIVISION.                                                   IC2234.2
005100 FILE SECTION.                                                    IC2234.2
005200 FD  PRINT-FILE.                                                  IC2234.2
005300 01  PRINT-REC PICTURE X(120).                                    IC2234.2
005400 01  DUMMY-RECORD PICTURE X(120).                                 IC2234.2
005500 WORKING-STORAGE SECTION.                                         IC2234.2
005600 77  DN1 PICTURE S99  VALUE ZERO.                                 IC2234.2
005700 77  DN3 PICTURE S99.                                             IC2234.2
005800 77  ID1 PICTURE X(8) VALUE "IC223A-1".                           IC2234.2
005900 77  ID2 PICTURE X(8).                                            IC2234.2
006000 77  DN2 PICTURE S99                                              IC2234.2
006100         USAGE COMPUTATIONAL, VALUE ZERO.                         IC2234.2
006200 77  DN4 PICTURE S99                                              IC2234.2
006300         USAGE IS COMPUTATIONAL.                                  IC2234.2
006400 77  CALL-COUNT PIC S99.                                          IC2234.2
006500 77  FAIL-FLAG PIC 9.                                             IC2234.2
006600 01  TEST-RESULTS.                                                IC2234.2
006700     02 FILLER                   PIC X      VALUE SPACE.          IC2234.2
006800     02 FEATURE                  PIC X(20)  VALUE SPACE.          IC2234.2
006900     02 FILLER                   PIC X      VALUE SPACE.          IC2234.2
007000     02 P-OR-F                   PIC X(5)   VALUE SPACE.          IC2234.2
007100     02 FILLER                   PIC X      VALUE SPACE.          IC2234.2
007200     02  PAR-NAME.                                                IC2234.2
007300       03 FILLER                 PIC X(19)  VALUE SPACE.          IC2234.2
007400       03  PARDOT-X              PIC X      VALUE SPACE.          IC2234.2
007500       03 DOTVALUE               PIC 99     VALUE ZERO.           IC2234.2
007600     02 FILLER                   PIC X(8)   VALUE SPACE.          IC2234.2
007700     02 RE-MARK                  PIC X(61).                       IC2234.2
007800 01  TEST-COMPUTED.                                               IC2234.2
007900     02 FILLER                   PIC X(30)  VALUE SPACE.          IC2234.2
008000     02 FILLER                   PIC X(17)  VALUE                 IC2234.2
008100            "       COMPUTED=".                                   IC2234.2
008200     02 COMPUTED-X.                                               IC2234.2
008300     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          IC2234.2
008400     03 COMPUTED-N               REDEFINES COMPUTED-A             IC2234.2
008500                                 PIC -9(9).9(9).                  IC2234.2
008600     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         IC2234.2
008700     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     IC2234.2
008800     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     IC2234.2
008900     03       CM-18V0 REDEFINES COMPUTED-A.                       IC2234.2
009000         04 COMPUTED-18V0                    PIC -9(18).          IC2234.2
009100         04 FILLER                           PIC X.               IC2234.2
009200     03 FILLER PIC X(50) VALUE SPACE.                             IC2234.2
009300 01  TEST-CORRECT.                                                IC2234.2
009400     02 FILLER PIC X(30) VALUE SPACE.                             IC2234.2
009500     02 FILLER PIC X(17) VALUE "       CORRECT =".                IC2234.2
009600     02 CORRECT-X.                                                IC2234.2
009700     03 CORRECT-A                  PIC X(20) VALUE SPACE.         IC2234.2
009800     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      IC2234.2
009900     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         IC2234.2
010000     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     IC2234.2
010100     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     IC2234.2
010200     03      CR-18V0 REDEFINES CORRECT-A.                         IC2234.2
010300         04 CORRECT-18V0                     PIC -9(18).          IC2234.2
010400         04 FILLER                           PIC X.               IC2234.2
010500     03 FILLER PIC X(2) VALUE SPACE.                              IC2234.2
010600     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     IC2234.2
010700 01  CCVS-C-1.                                                    IC2234.2
010800     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAIC2234.2
010900-    "SS  PARAGRAPH-NAME                                          IC2234.2
011000-    "       REMARKS".                                            IC2234.2
011100     02 FILLER                     PIC X(20)    VALUE SPACE.      IC2234.2
011200 01  CCVS-C-2.                                                    IC2234.2
011300     02 FILLER                     PIC X        VALUE SPACE.      IC2234.2
011400     02 FILLER                     PIC X(6)     VALUE "TESTED".   IC2234.2
011500     02 FILLER                     PIC X(15)    VALUE SPACE.      IC2234.2
011600     02 FILLER                     PIC X(4)     VALUE "FAIL".     IC2234.2
011700     02 FILLER                     PIC X(94)    VALUE SPACE.      IC2234.2
011800 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       IC2234.2
011900 01  REC-CT                        PIC 99       VALUE ZERO.       IC2234.2
012000 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       IC2234.2
012100 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       IC2234.2
012200 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       IC2234.2
012300 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       IC2234.2
012400 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       IC2234.2
012500 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       IC2234.2
012600 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      IC2234.2
012700 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       IC2234.2
012800 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     IC2234.2
012900 01  CCVS-H-1.                                                    IC2234.2
013000     02  FILLER                    PIC X(39)    VALUE SPACES.     IC2234.2
013100     02  FILLER                    PIC X(42)    VALUE             IC2234.2
013200     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 IC2234.2
013300     02  FILLER                    PIC X(39)    VALUE SPACES.     IC2234.2
013400 01  CCVS-H-2A.                                                   IC2234.2
013500   02  FILLER                        PIC X(40)  VALUE SPACE.      IC2234.2
013600   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  IC2234.2
013700   02  FILLER                        PIC XXXX   VALUE             IC2234.2
013800     "4.2 ".                                                      IC2234.2
013900   02  FILLER                        PIC X(28)  VALUE             IC2234.2
014000            " COPY - NOT FOR DISTRIBUTION".                       IC2234.2
014100   02  FILLER                        PIC X(41)  VALUE SPACE.      IC2234.2
014200                                                                  IC2234.2
014300 01  CCVS-H-2B.                                                   IC2234.2
014400   02  FILLER                        PIC X(15)  VALUE             IC2234.2
014500            "TEST RESULT OF ".                                    IC2234.2
014600   02  TEST-ID                       PIC X(9).                    IC2234.2
014700   02  FILLER                        PIC X(4)   VALUE             IC2234.2
014800            " IN ".                                               IC2234.2
014900   02  FILLER                        PIC X(12)  VALUE             IC2234.2
015000     " HIGH       ".                                              IC2234.2
015100   02  FILLER                        PIC X(22)  VALUE             IC2234.2
015200            " LEVEL VALIDATION FOR ".                             IC2234.2
015300   02  FILLER                        PIC X(58)  VALUE             IC2234.2
015400     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IC2234.2
015500 01  CCVS-H-3.                                                    IC2234.2
015600     02  FILLER                      PIC X(34)  VALUE             IC2234.2
015700            " FOR OFFICIAL USE ONLY    ".                         IC2234.2
015800     02  FILLER                      PIC X(58)  VALUE             IC2234.2
015900     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IC2234.2
016000     02  FILLER                      PIC X(28)  VALUE             IC2234.2
016100            "  COPYRIGHT   1985 ".                                IC2234.2
016200 01  CCVS-E-1.                                                    IC2234.2
016300     02 FILLER                       PIC X(52)  VALUE SPACE.      IC2234.2
016400     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              IC2234.2
016500     02 ID-AGAIN                     PIC X(9).                    IC2234.2
016600     02 FILLER                       PIC X(45)  VALUE SPACES.     IC2234.2
016700 01  CCVS-E-2.                                                    IC2234.2
016800     02  FILLER                      PIC X(31)  VALUE SPACE.      IC2234.2
016900     02  FILLER                      PIC X(21)  VALUE SPACE.      IC2234.2
017000     02 CCVS-E-2-2.                                               IC2234.2
017100         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      IC2234.2
017200         03 FILLER                   PIC X      VALUE SPACE.      IC2234.2
017300         03 ENDER-DESC               PIC X(44)  VALUE             IC2234.2
017400            "ERRORS ENCOUNTERED".                                 IC2234.2
017500 01  CCVS-E-3.                                                    IC2234.2
017600     02  FILLER                      PIC X(22)  VALUE             IC2234.2
017700            " FOR OFFICIAL USE ONLY".                             IC2234.2
017800     02  FILLER                      PIC X(12)  VALUE SPACE.      IC2234.2
017900     02  FILLER                      PIC X(58)  VALUE             IC2234.2
018000     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IC2234.2
018100     02  FILLER                      PIC X(13)  VALUE SPACE.      IC2234.2
018200     02 FILLER                       PIC X(15)  VALUE             IC2234.2
018300             " COPYRIGHT 1985".                                   IC2234.2
018400 01  CCVS-E-4.                                                    IC2234.2
018500     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      IC2234.2
018600     02 FILLER                       PIC X(4)   VALUE " OF ".     IC2234.2
018700     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      IC2234.2
018800     02 FILLER                       PIC X(40)  VALUE             IC2234.2
018900      "  TESTS WERE EXECUTED SUCCESSFULLY".                       IC2234.2
019000 01  XXINFO.                                                      IC2234.2
019100     02 FILLER                       PIC X(19)  VALUE             IC2234.2
019200            "*** INFORMATION ***".                                IC2234.2
019300     02 INFO-TEXT.                                                IC2234.2
019400       04 FILLER                     PIC X(8)   VALUE SPACE.      IC2234.2
019500       04 XXCOMPUTED                 PIC X(20).                   IC2234.2
019600       04 FILLER                     PIC X(5)   VALUE SPACE.      IC2234.2
019700       04 XXCORRECT                  PIC X(20).                   IC2234.2
019800     02 INF-ANSI-REFERENCE           PIC X(48).                   IC2234.2
019900 01  HYPHEN-LINE.                                                 IC2234.2
020000     02 FILLER  PIC IS X VALUE IS SPACE.                          IC2234.2
020100     02 FILLER  PIC IS X(65)    VALUE IS "************************IC2234.2
020200-    "*****************************************".                 IC2234.2
020300     02 FILLER  PIC IS X(54)    VALUE IS "************************IC2234.2
020400-    "******************************".                            IC2234.2
020500 01  CCVS-PGM-ID                     PIC X(9)   VALUE             IC2234.2
020600     "IC223A".                                                    IC2234.2
020700 PROCEDURE DIVISION.                                              IC2234.2
020800 CCVS1 SECTION.                                                   IC2234.2
020900 OPEN-FILES.                                                      IC2234.2
021000     OPEN     OUTPUT PRINT-FILE.                                  IC2234.2
021100     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   IC2234.2
021200     MOVE    SPACE TO TEST-RESULTS.                               IC2234.2
021300     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             IC2234.2
021400     GO TO CCVS1-EXIT.                                            IC2234.2
021500 CLOSE-FILES.                                                     IC2234.2
021600     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   IC2234.2
021700 TERMINATE-CCVS.                                                  IC2234.2
021800*    EXIT PROGRAM.                                                IC2234.2
021900*TERMINATE-CALL.                                                  IC2234.2
022000     STOP     RUN.                                                IC2234.2
022100 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         IC2234.2
022200 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           IC2234.2
022300 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          IC2234.2
022400 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      IC2234.2
022500     MOVE "****TEST DELETED****" TO RE-MARK.                      IC2234.2
022600 PRINT-DETAIL.                                                    IC2234.2
022700     IF REC-CT NOT EQUAL TO ZERO                                  IC2234.2
022800             MOVE "." TO PARDOT-X                                 IC2234.2
022900             MOVE REC-CT TO DOTVALUE.                             IC2234.2
023000     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      IC2234.2
023100     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               IC2234.2
023200        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 IC2234.2
023300          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 IC2234.2
023400     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              IC2234.2
023500     MOVE SPACE TO CORRECT-X.                                     IC2234.2
023600     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         IC2234.2
023700     MOVE     SPACE TO RE-MARK.                                   IC2234.2
023800 HEAD-ROUTINE.                                                    IC2234.2
023900     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IC2234.2
024000     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IC2234.2
024100     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IC2234.2
024200     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IC2234.2
024300 COLUMN-NAMES-ROUTINE.                                            IC2234.2
024400     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IC2234.2
024500     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IC2234.2
024600     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        IC2234.2
024700 END-ROUTINE.                                                     IC2234.2
024800     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.IC2234.2
024900 END-RTN-EXIT.                                                    IC2234.2
025000     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IC2234.2
025100 END-ROUTINE-1.                                                   IC2234.2
025200      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      IC2234.2
025300      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               IC2234.2
025400      ADD PASS-COUNTER TO ERROR-HOLD.                             IC2234.2
025500*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   IC2234.2
025600      MOVE PASS-COUNTER TO CCVS-E-4-1.                            IC2234.2
025700      MOVE ERROR-HOLD TO CCVS-E-4-2.                              IC2234.2
025800      MOVE CCVS-E-4 TO CCVS-E-2-2.                                IC2234.2
025900      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           IC2234.2
026000  END-ROUTINE-12.                                                 IC2234.2
026100      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        IC2234.2
026200     IF       ERROR-COUNTER IS EQUAL TO ZERO                      IC2234.2
026300         MOVE "NO " TO ERROR-TOTAL                                IC2234.2
026400         ELSE                                                     IC2234.2
026500         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       IC2234.2
026600     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           IC2234.2
026700     PERFORM WRITE-LINE.                                          IC2234.2
026800 END-ROUTINE-13.                                                  IC2234.2
026900     IF DELETE-COUNTER IS EQUAL TO ZERO                           IC2234.2
027000         MOVE "NO " TO ERROR-TOTAL  ELSE                          IC2234.2
027100         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      IC2234.2
027200     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   IC2234.2
027300     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IC2234.2
027400      IF   INSPECT-COUNTER EQUAL TO ZERO                          IC2234.2
027500          MOVE "NO " TO ERROR-TOTAL                               IC2234.2
027600      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   IC2234.2
027700      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            IC2234.2
027800      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          IC2234.2
027900     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IC2234.2
028000 WRITE-LINE.                                                      IC2234.2
028100     ADD 1 TO RECORD-COUNT.                                       IC2234.2
028200*    IF RECORD-COUNT GREATER 50                                   IC2234.2
028300*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          IC2234.2
028400*        MOVE SPACE TO DUMMY-RECORD                               IC2234.2
028500*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  IC2234.2
028600*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             IC2234.2
028700*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     IC2234.2
028800*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          IC2234.2
028900*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          IC2234.2
029000*        MOVE ZERO TO RECORD-COUNT.                               IC2234.2
029100     PERFORM WRT-LN.                                              IC2234.2
029200 WRT-LN.                                                          IC2234.2
029300     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               IC2234.2
029400     MOVE SPACE TO DUMMY-RECORD.                                  IC2234.2
029500 BLANK-LINE-PRINT.                                                IC2234.2
029600     PERFORM WRT-LN.                                              IC2234.2
029700 FAIL-ROUTINE.                                                    IC2234.2
029800     IF   COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE. IC2234.2
029900     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.IC2234.2
030000     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IC2234.2
030100     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   IC2234.2
030200     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IC2234.2
030300     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IC2234.2
030400     GO TO  FAIL-ROUTINE-EX.                                      IC2234.2
030500 FAIL-ROUTINE-WRITE.                                              IC2234.2
030600     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         IC2234.2
030700     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 IC2234.2
030800     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. IC2234.2
030900     MOVE   SPACES TO COR-ANSI-REFERENCE.                         IC2234.2
031000 FAIL-ROUTINE-EX. EXIT.                                           IC2234.2
031100 BAIL-OUT.                                                        IC2234.2
031200     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   IC2234.2
031300     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           IC2234.2
031400 BAIL-OUT-WRITE.                                                  IC2234.2
031500     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  IC2234.2
031600     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IC2234.2
031700     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IC2234.2
031800     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IC2234.2
031900 BAIL-OUT-EX. EXIT.                                               IC2234.2
032000 CCVS1-EXIT.                                                      IC2234.2
032100     EXIT.                                                        IC2234.2
032200 SECT-IC223A-001 SECTION.                                         IC2234.2
032300 CALL-TEST-01.                                                    IC2234.2
032400     MOVE "CALL-TEST-01" TO PAR-NAME.                             IC2234.2
032500     MOVE     "LEV 2 CALL STATEMENT" TO FEATURE.                  IC2234.2
032600     MOVE 0 TO CALL-COUNT.                                        IC2234.2
032700*        THIS TEST HAS CALL STATEMENTS WITH AN IDENTIFIER         IC2234.2
032800*    CONTAINING THE NAME OF THE SUBPROGRAM TO BE CALLED.          IC2234.2
032900*        CALL-TEST-01 CONTAINS THE BASIC LEVEL 2 CALL STATEMENT.  IC2234.2
033000*    IF IT CANNOT BE COMPILED AND EXECUTED CORRECTLY, THERE IS    IC2234.2
033100*    NO USE IN RUNNING THE LEVEL 2 IPC ROUTINES.                  IC2234.2
033200 CALL-TEST-01-01.                                                 IC2234.2
033300     MOVE 1 TO REC-CT.                                            IC2234.2
033400     MOVE ZERO TO DN3, DN4.                                       IC2234.2
033500     CALL "IC223A-1" USING BY REFERENCE DN1, DN2, DN3, DN4        IC2234.2
033600     END-CALL                                                     IC2234.2
033700     PERFORM CHECK-TEST-01.                                       IC2234.2
033800     IF FAIL-FLAG EQUAL TO ZERO                                   IC2234.2
033900         PERFORM PASS                                             IC2234.2
034000         GO TO CALL-WRITE-01-01.                                  IC2234.2
034100 CALL-FAIL-01-01.                                                 IC2234.2
034200     PERFORM FAIL.                                                IC2234.2
034300     MOVE FAIL-FLAG TO COMPUTED-18V0.                             IC2234.2
034400     MOVE "NO. OF WRONG VALUES RETURNED" TO RE-MARK.              IC2234.2
034500 CALL-WRITE-01-01.                                                IC2234.2
034600     PERFORM PRINT-DETAIL.                                        IC2234.2
034700 CALL-TEST-01-02.                                                 IC2234.2
034800     ADD 1 TO REC-CT.                                             IC2234.2
034900     MOVE ZERO TO DN3, DN4.                                       IC2234.2
035000     CALL ID1 USING BY REFERENCE DN1, DN2, DN3, DN4               IC2234.2
035100     END-CALL                                                     IC2234.2
035200     PERFORM CHECK-TEST-01.                                       IC2234.2
035300     IF FAIL-FLAG EQUAL TO ZERO                                   IC2234.2
035400         PERFORM PASS                                             IC2234.2
035500         GO TO CALL-WRITE-01-02.                                  IC2234.2
035600 CALL-FAIL-01-02.                                                 IC2234.2
035700     PERFORM FAIL.                                                IC2234.2
035800     MOVE FAIL-FLAG TO COMPUTED-18V0.                             IC2234.2
035900     MOVE "NO. OF WRONG VALUES RETURNED" TO RE-MARK.              IC2234.2
036000 CALL-WRITE-01-02.                                                IC2234.2
036100     PERFORM PRINT-DETAIL.                                        IC2234.2
036200 CALL-TEST-01-03.                                                 IC2234.2
036300     ADD 1 TO REC-CT.                                             IC2234.2
036400     MOVE ID1 TO ID2.                                             IC2234.2
036500     MOVE ZERO TO DN3, DN4.                                       IC2234.2
036600     CALL ID2 USING REFERENCE DN1 DN2 DN3 DN4                     IC2234.2
036700     END-CALL.                                                    IC2234.2
036800     PERFORM CHECK-TEST-01.                                       IC2234.2
036900     IF FAIL-FLAG EQUAL TO ZERO                                   IC2234.2
037000         PERFORM PASS                                             IC2234.2
037100         GO TO CALL-WRITE-01-03.                                  IC2234.2
037200 CALL-FAIL-01-03.                                                 IC2234.2
037300     PERFORM FAIL.                                                IC2234.2
037400     MOVE FAIL-FLAG TO COMPUTED-18V0.                             IC2234.2
037500     MOVE "NO. OF WRONG VALUES RETURNED" TO RE-MARK.              IC2234.2
037600 CALL-WRITE-01-03.                                                IC2234.2
037700     PERFORM PRINT-DETAIL.                                        IC2234.2
037800 CALL-TEST-01-04.                                                 IC2234.2
037900     ADD 1 TO REC-CT.                                             IC2234.2
038000     MOVE "IC223A-1" TO ID2.                                      IC2234.2
038100     MOVE ZERO TO DN3, DN4.                                       IC2234.2
038200     CALL ID2 USING REFERENCE DN1, DN2, DN3, DN4                  IC2234.2
038300     END-CALL.                                                    IC2234.2
038400     PERFORM CHECK-TEST-01.                                       IC2234.2
038500     IF FAIL-FLAG EQUAL TO ZERO                                   IC2234.2
038600         PERFORM PASS                                             IC2234.2
038700         GO TO CALL-WRITE-01-04.                                  IC2234.2
038800 CALL-FAIL-01-04.                                                 IC2234.2
038900     PERFORM FAIL.                                                IC2234.2
039000     MOVE FAIL-FLAG TO COMPUTED-18V0.                             IC2234.2
039100     MOVE "NO. OF WRONG VALUES RETURNED" TO RE-MARK.              IC2234.2
039200 CALL-WRITE-01-04.                                                IC2234.2
039300     PERFORM PRINT-DETAIL.                                        IC2234.2
039400 CALL-TEST-02.                                                    IC2234.2
039500     MOVE "CALL-TEST-02" TO PAR-NAME.                             IC2234.2
039600     MOVE "DATA-NAME USED TWICE" TO FEATURE.                      IC2234.2
039700*        THIS TEST USES A DATA-NAME MORE THAN ONCE IN             IC2234.2
039800*    A USING PHRASE OF A CALL STATEMENT.                          IC2234.2
039900 CALL-TEST-02-01.                                                 IC2234.2
040000     MOVE 1 TO REC-CT.                                            IC2234.2
040100     MOVE 1 TO DN1.                                               IC2234.2
040200     MOVE 0 TO DN2, DN3, DN4.                                     IC2234.2
040300     CALL "IC223A-1" USING REFERENCE DN1, DN2, DN1, DN4           IC2234.2
040400     END-CALL.                                                    IC2234.2
040500     IF DN1 NOT EQUAL TO 2                                        IC2234.2
040600         GO TO CALL-FAIL-02-01-1.                                 IC2234.2
040700     IF DN2 NOT EQUAL TO 0                                        IC2234.2
040800         GO TO CALL-FAIL-02-01-2.                                 IC2234.2
040900     IF DN3 NOT EQUAL TO 0                                        IC2234.2
041000         GO TO CALL-FAIL-02-01-3.                                 IC2234.2
041100     IF DN4 NOT EQUAL TO 5                                        IC2234.2
041200         GO TO CALL-FAIL-02-01-4.                                 IC2234.2
041300     GO TO CALL-PASS-02-01.                                       IC2234.2
041400 CALL-DELETE-02-01.                                               IC2234.2
041500     PERFORM DE-LETE.                                             IC2234.2
041600     GO TO CALL-WRITE-02-01.                                      IC2234.2
041700 CALL-PASS-02-01.                                                 IC2234.2
041800     PERFORM PASS.                                                IC2234.2
041900     GO TO CALL-WRITE-02-01.                                      IC2234.2
042000 CALL-FAIL-02-01-1.                                               IC2234.2
042100     MOVE DN1 TO COMPUTED-18V0.                                   IC2234.2
042200     MOVE 2 TO CORRECT-18V0.                                      IC2234.2
042300     MOVE "ERROR IN DN1 VALUE RETURNED" TO RE-MARK.               IC2234.2
042400     GO TO CALL-FAIL-02-01.                                       IC2234.2
042500 CALL-FAIL-02-01-2.                                               IC2234.2
042600     MOVE DN2 TO COMPUTED-18V0.                                   IC2234.2
042700     MOVE 0 TO CORRECT-18V0.                                      IC2234.2
042800     MOVE "ERROR IN DN2 VALUE RETURNED" TO RE-MARK.               IC2234.2
042900     GO TO CALL-FAIL-02-01.                                       IC2234.2
043000 CALL-FAIL-02-01-3.                                               IC2234.2
043100     MOVE DN3 TO COMPUTED-18V0.                                   IC2234.2
043200     MOVE ZERO TO CORRECT-18V0.                                   IC2234.2
043300     MOVE "DN3 VALUE CHANGED BY CALL" TO RE-MARK.                 IC2234.2
043400     GO TO CALL-FAIL-02-01.                                       IC2234.2
043500 CALL-FAIL-02-01-4.                                               IC2234.2
043600     MOVE DN4 TO COMPUTED-18V0.                                   IC2234.2
043700     MOVE 5 TO CORRECT-18V0.                                      IC2234.2
043800     MOVE "ERROR IN DN4 VALUE RETURNED" TO RE-MARK.               IC2234.2
043900 CALL-FAIL-02-01.                                                 IC2234.2
044000     PERFORM FAIL.                                                IC2234.2
044100 CALL-WRITE-02-01.                                                IC2234.2
044200     PERFORM PRINT-DETAIL.                                        IC2234.2
044300 CALL-TEST-02-02.                                                 IC2234.2
044400     ADD 1 TO REC-CT.                                             IC2234.2
044500     MOVE 0 TO DN4, DN3, DN2, DN1.                                IC2234.2
044600     CALL ID1 USING REFERENCE DN1 DN2 DN3 DN2                     IC2234.2
044700     END-CALL.                                                    IC2234.2
044800     IF DN1 NOT EQUAL TO 0                                        IC2234.2
044900         GO TO CALL-FAIL-02-02-1.                                 IC2234.2
045000     IF DN2 NOT EQUAL TO 6                                        IC2234.2
045100         GO TO CALL-FAIL-02-02-2.                                 IC2234.2
045200     IF DN3 NOT EQUAL TO 1                                        IC2234.2
045300         GO TO CALL-FAIL-02-02-3.                                 IC2234.2
045400     IF DN4 NOT EQUAL TO 0                                        IC2234.2
045500         GO TO CALL-FAIL-02-02-4.                                 IC2234.2
045600     GO TO CALL-PASS-02-02.                                       IC2234.2
045700 CALL-DELETE-02-02.                                               IC2234.2
045800     PERFORM DE-LETE.                                             IC2234.2
045900     GO TO CALL-WRITE-02-02.                                      IC2234.2
046000 CALL-PASS-02-02.                                                 IC2234.2
046100     PERFORM PASS.                                                IC2234.2
046200     GO TO CALL-WRITE-02-02.                                      IC2234.2
046300 CALL-FAIL-02-02-1.                                               IC2234.2
046400     MOVE DN1 TO COMPUTED-18V0.                                   IC2234.2
046500     MOVE ZERO TO CORRECT-18V0.                                   IC2234.2
046600     MOVE "ERROR IN DN1 VALUE RETURNED" TO RE-MARK.               IC2234.2
046700     GO TO CALL-FAIL-02-02.                                       IC2234.2
046800 CALL-FAIL-02-02-2.                                               IC2234.2
046900     MOVE DN2 TO COMPUTED-18V0.                                   IC2234.2
047000     MOVE 6 TO CORRECT-18V0.                                      IC2234.2
047100     MOVE "ERROR IN DN2 VALUE RETURNED" TO RE-MARK.               IC2234.2
047200     GO TO CALL-FAIL-02-02.                                       IC2234.2
047300 CALL-FAIL-02-02-3.                                               IC2234.2
047400     MOVE DN3 TO COMPUTED-18V0.                                   IC2234.2
047500     MOVE 1 TO CORRECT-18V0.                                      IC2234.2
047600     MOVE "ERROR IN DN3 VALUE RETURNED" TO RE-MARK.               IC2234.2
047700     GO TO CALL-FAIL-02-02.                                       IC2234.2
047800 CALL-FAIL-02-02-4.                                               IC2234.2
047900     MOVE DN4 TO COMPUTED-18V0.                                   IC2234.2
048000     MOVE 0 TO CORRECT-18V0.                                      IC2234.2
048100     MOVE "DN4 VALUE CHANGED BY CALL" TO RE-MARK.                 IC2234.2
048200 CALL-FAIL-02-02.                                                 IC2234.2
048300     PERFORM FAIL.                                                IC2234.2
048400 CALL-WRITE-02-02.                                                IC2234.2
048500     PERFORM PRINT-DETAIL.                                        IC2234.2
048600 CALL-TEST-02-03.                                                 IC2234.2
048700     ADD 1 TO REC-CT.                                             IC2234.2
048800     MOVE 0 TO DN4, DN3.                                          IC2234.2
048900     MOVE 10 TO DN2.                                              IC2234.2
049000     MOVE 25 TO DN1.                                              IC2234.2
049100     CALL ID1 USING REFERENCE DN1 DN2 DN1 DN2                     IC2234.2
049200     END-CALL.                                                    IC2234.2
049300     IF DN1 EQUAL TO 26                                           IC2234.2
049400         GO TO CHECK-02-03-2.                                     IC2234.2
049500     GO TO CALL-FAIL-02-03-1.                                     IC2234.2
049600 CALL-DELETE-02-03.                                               IC2234.2
049700     PERFORM DE-LETE.                                             IC2234.2
049800     GO TO CALL-WRITE-02-03.                                      IC2234.2
049900 CALL-FAIL-02-03-1.                                               IC2234.2
050000     MOVE DN1 TO COMPUTED-18V0.                                   IC2234.2
050100     MOVE 26 TO CORRECT-18V0.                                     IC2234.2
050200     MOVE "ERROR IN DN1 VALUE RETURNED" TO RE-MARK.               IC2234.2
050300     GO TO CALL-FAIL-02-03.                                       IC2234.2
050400 CHECK-02-03-2.                                                   IC2234.2
050500     IF DN2 EQUAL TO 7                                            IC2234.2
050600         GO TO CHECK-02-03-3.                                     IC2234.2
050700 CALL-FAIL-02-03-2.                                               IC2234.2
050800     MOVE DN2 TO COMPUTED-18V0.                                   IC2234.2
050900     MOVE 7 TO CORRECT-18V0.                                      IC2234.2
051000     MOVE "ERROR IN DN2 VALUE RETURNED" TO RE-MARK.               IC2234.2
051100     GO TO CALL-FAIL-02-03.                                       IC2234.2
051200 CHECK-02-03-3.                                                   IC2234.2
051300     IF DN3 EQUAL TO 0                                            IC2234.2
051400         GO TO CHECK-02-03-4.                                     IC2234.2
051500 CALL-FAIL-02-03-3.                                               IC2234.2
051600     MOVE DN3 TO COMPUTED-18V0.                                   IC2234.2
051700     MOVE 0 TO CORRECT-18V0.                                      IC2234.2
051800     MOVE "DN3 VALUE CHANGED BY CALL" TO RE-MARK.                 IC2234.2
051900     GO TO CALL-FAIL-02-03.                                       IC2234.2
052000 CHECK-02-03-4.                                                   IC2234.2
052100     IF DN4 EQUAL TO 0                                            IC2234.2
052200         GO TO CALL-PASS-02-03.                                   IC2234.2
052300 CALL-FAIL-02-03-4.                                               IC2234.2
052400     MOVE DN4 TO COMPUTED-18V0.                                   IC2234.2
052500     MOVE 0 TO CORRECT-18V0.                                      IC2234.2
052600     MOVE "DN4 VALUE CHANGED BY CALL" TO RE-MARK.                 IC2234.2
052700 CALL-FAIL-02-03.                                                 IC2234.2
052800     PERFORM FAIL.                                                IC2234.2
052900     GO TO CALL-WRITE-02-03.                                      IC2234.2
053000 CALL-PASS-02-03.                                                 IC2234.2
053100     PERFORM PASS.                                                IC2234.2
053200 CALL-WRITE-02-03.                                                IC2234.2
053300     PERFORM PRINT-DETAIL.                                        IC2234.2
053400 CALL-TEST-03.                                                    IC2234.2
053500*        THIS TEST USES THE ON OVERFLOW PHRASE IN THE CALL        IC2234.2
053600*    STATEMENT.  THIS IS A SYNTACTICAL CHECK ONLY, THE ON         IC2234.2
053700*    OVERFLOW CONDITION SHOULD NEVER OCCUR.                       IC2234.2
053800     MOVE "CALL-TEST-03" TO PAR-NAME.                             IC2234.2
053900     MOVE "ON OVERFLOW PHRASE" TO FEATURE.                        IC2234.2
054000 CALL-TEST-03-01.                                                 IC2234.2
054100     MOVE 7 TO CALL-COUNT.                                        IC2234.2
054200     MOVE 20 TO DN1.                                              IC2234.2
054300     MOVE 30 TO DN2.                                              IC2234.2
054400     MOVE ZERO TO DN3, DN4.                                       IC2234.2
054500     CALL "IC223A-1" USING REFERENCE DN1, DN2, DN3, DN4;          IC2234.2
054600         ON OVERFLOW MOVE "OVERFLOW SHOULD NOT OCCUR" TO RE-MARK  IC2234.2
054700                     GO TO CALL-FAIL-03-01                        IC2234.2
054800     END-CALL.                                                    IC2234.2
054900     PERFORM CHECK-TEST-03.                                       IC2234.2
055000     IF FAIL-FLAG EQUAL TO ZERO                                   IC2234.2
055100         PERFORM PASS                                             IC2234.2
055200         GO TO CALL-WRITE-03-01.                                  IC2234.2
055300     MOVE FAIL-FLAG TO COMPUTED-18V0.                             IC2234.2
055400     MOVE "NO. OF WRONG VALUES RETURNED" TO RE-MARK.              IC2234.2
055500 CALL-FAIL-03-01.                                                 IC2234.2
055600     PERFORM FAIL.                                                IC2234.2
055700 CALL-WRITE-03-01.                                                IC2234.2
055800     PERFORM PRINT-DETAIL.                                        IC2234.2
055900 CALL-TEST-03-02.                                                 IC2234.2
056000     MOVE ZERO TO DN3, DN4.                                       IC2234.2
056100     CALL "IC223A-1" USING REFERENCE DN1, DN2, DN3, DN4;          IC2234.2
056200         OVERFLOW MOVE "OVERFLOW SHOULD NOT OCCUR" TO RE-MARK     IC2234.2
056300                  GO TO CALL-FAIL-03-02                           IC2234.2
056400     END-CALL.                                                    IC2234.2
056500     PERFORM CHECK-TEST-03.                                       IC2234.2
056600     IF FAIL-FLAG EQUAL TO ZERO                                   IC2234.2
056700         PERFORM PASS                                             IC2234.2
056800         GO TO CALL-WRITE-03-02.                                  IC2234.2
056900     MOVE FAIL-FLAG TO COMPUTED-18V0.                             IC2234.2
057000     MOVE "NO. OF WRONG VALUES RETURNED" TO RE-MARK.              IC2234.2
057100 CALL-FAIL-03-02.                                                 IC2234.2
057200     PERFORM FAIL.                                                IC2234.2
057300 CALL-WRITE-03-02.                                                IC2234.2
057400     PERFORM PRINT-DETAIL.                                        IC2234.2
057500 CALL-TEST-03-03.                                                 IC2234.2
057600     MOVE ZERO TO DN3, DN4.                                       IC2234.2
057700     CALL ID1 USING REFERENCE DN1 DN2 DN3 DN4                     IC2234.2
057800         ON OVERFLOW MOVE "OVERFLOW SHOULD NOT OCCUR" TO RE-MARK  IC2234.2
057900                     GO TO CALL-FAIL-03-03                        IC2234.2
058000     END-CALL.                                                    IC2234.2
058100     PERFORM   CHECK-TEST-03.                                     IC2234.2
058200     IF FAIL-FLAG EQUAL TO ZERO                                   IC2234.2
058300         PERFORM PASS                                             IC2234.2
058400         GO TO CALL-WRITE-03-03.                                  IC2234.2
058500     MOVE FAIL-FLAG TO COMPUTED-18V0.                             IC2234.2
058600     MOVE "NO. OF WRONG VALUES RETURNED" TO RE-MARK.              IC2234.2
058700 CALL-FAIL-03-03.                                                 IC2234.2
058800     PERFORM FAIL.                                                IC2234.2
058900 CALL-WRITE-03-03.                                                IC2234.2
059000     PERFORM PRINT-DETAIL.                                        IC2234.2
059100 CALL-TEST-03-04.                                                 IC2234.2
059200     MOVE ZERO TO DN3, DN4.                                       IC2234.2
059300     CALL ID1 USING REFERENCE DN1 DN2 DN3 DN4;                    IC2234.2
059400         OVERFLOW MOVE "OVERFLOW SHOULD NOT OCCUR" TO RE-MARK,    IC2234.2
059500                  GO TO CALL-FAIL-03-04                           IC2234.2
059600     END-CALL.                                                    IC2234.2
059700     PERFORM CHECK-TEST-03.                                       IC2234.2
059800     IF FAIL-FLAG EQUAL TO ZERO                                   IC2234.2
059900         PERFORM PASS                                             IC2234.2
060000         GO TO CALL-WRITE-03-04.                                  IC2234.2
060100     MOVE FAIL-FLAG TO COMPUTED-18V0.                             IC2234.2
060200     MOVE "NO. OF WRONG VALUES RETURNED" TO RE-MARK.              IC2234.2
060300 CALL-FAIL-03-04.                                                 IC2234.2
060400     PERFORM FAIL.                                                IC2234.2
060500 CALL-WRITE-03-04.                                                IC2234.2
060600     PERFORM PRINT-DETAIL.                                        IC2234.2
060700     GO TO EXIT-IC223A.                                           IC2234.2
060800 CALL-DELETE-03.                                                  IC2234.2
060900*        IF THE ON OVERFLOW PHRASE IS NOT RECOGNIZED, DELETE ALL  IC2234.2
061000*    OF THE ABOVE CALL-TEST-03 PARAGRAPHS, STARTING WITH          IC2234.2
061100*    CALL-TEST-03-01.                                             IC2234.2
061200     PERFORM DE-LETE.                                             IC2234.2
061300     PERFORM PRINT-DETAIL.                                        IC2234.2
061400 EXIT-IC223A.                                                     IC2234.2
061500     GO TO CCVS-EXIT.                                             IC2234.2
061600 SECT-IC223A-002 SECTION.                                         IC2234.2
061700 CHECK-TEST-01.                                                   IC2234.2
061800     MOVE ZERO TO FAIL-FLAG.                                      IC2234.2
061900     ADD 1 TO CALL-COUNT.                                         IC2234.2
062000     IF DN1 EQUAL TO ZERO                                         IC2234.2
062100             NEXT SENTENCE                                        IC2234.2
062200         ELSE ADD 1 TO FAIL-FLAG.                                 IC2234.2
062300     IF DN2 NOT EQUAL TO ZERO                                     IC2234.2
062400         ADD 1 TO FAIL-FLAG.                                      IC2234.2
062500     IF DN3 NOT EQUAL TO 1                                        IC2234.2
062600         ADD 1 TO FAIL-FLAG.                                      IC2234.2
062700     IF DN4 NOT EQUAL TO CALL-COUNT                               IC2234.2
062800         ADD 1 TO FAIL-FLAG.                                      IC2234.2
062900 CHECK-TEST-03.                                                   IC2234.2
063000     MOVE ZERO TO FAIL-FLAG.                                      IC2234.2
063100     ADD 1 TO CALL-COUNT.                                         IC2234.2
063200     IF DN4 NOT EQUAL TO CALL-COUNT                               IC2234.2
063300         ADD 1 TO FAIL-FLAG.                                      IC2234.2
063400     IF DN3 NOT EQUAL TO 21                                       IC2234.2
063500         ADD 1 TO FAIL-FLAG.                                      IC2234.2
063600     IF DN2 NOT EQUAL TO 30                                       IC2234.2
063700         ADD 1 TO FAIL-FLAG.                                      IC2234.2
063800     IF DN1 NOT EQUAL TO 20                                       IC2234.2
063900         ADD 1 TO FAIL-FLAG.                                      IC2234.2
064000 CCVS-EXIT SECTION.                                               IC2234.2
064100 CCVS-999999.                                                     IC2234.2
064200     GO TO CLOSE-FILES.                                           IC2234.2
064300 END PROGRAM IC223A.                                              IC2234.2
064400 IDENTIFICATION DIVISION.                                         IC2234.2
064500 PROGRAM-ID.                                                      IC2234.2
064600     IC223A-1.                                                    IC2234.2
064700****************************************************************  IC2234.2
064800*                                                              *  IC2234.2
064900*    THIS PROGRAM FORMS PART OF THE COBOL COMPILER VALIDATION  *  IC2234.2
065000*    SYSTEM (CCVS) USED TO TEST COBOL COMPILERS FOR            *  IC2234.2
065100*    CONFORMANCE WITH THE AMERICAN NATIONAL STANDARD           *  IC2234.2
065200*    (ANSI DOCUMENT REFERENCE: X3.23-1985) AND THE STANDARD OF *  IC2234.2
065300*    THE INTERNATIONAL ORGANIZATION FOR STANDARDISATION        *  IC2234.2
065400*    (ISO DOCUMENT REFERENCE: ISO-1989-1985).                  *  IC2234.2
065500*                                                              *  IC2234.2
065600*    THIS CCVS INCORPORATES ENHANCEMENTS TO THE CCVS FOR THE   *  IC2234.2
065700*    1974 STANDARD (ANSI DOCUMENT REFERENCE: X3.23-1974; ISO   *  IC2234.2
065800*    DOCUMENT REFERENCE: ISO-1989-1978).                       *  IC2234.2
065900*                                                              *  IC2234.2
066000*    THESE ENHANCEMENTS WERE SPECIFIED BY A PROJECT TEAM WHICH *  IC2234.2
066100*    WAS FUNDED BY THE COMMISSION FOR EUROPEAN COMMUNITIES AND *  IC2234.2
066200*    WHICH WAS RESPONSIBLE FOR TECHNICAL ISSUES TO:            *  IC2234.2
066300*                                                              *  IC2234.2
066400*          THE FEDERAL SOFTWARE TESTING CENTER                 *  IC2234.2
066500*          OFFICE OF SOFTWARE DEVELOPMENT                      *  IC2234.2
066600*                & INFORMATION TECHNOLOGY                      *  IC2234.2
066700*          TWO SKYLINE PLACE                                   *  IC2234.2
066800*          SUITE 1100                                          *  IC2234.2
066900*          5203 LEESBURG PIKE                                  *  IC2234.2
067000*          FALLS CHURCH                                        *  IC2234.2
067100*          VA 22041                                            *  IC2234.2
067200*          U.S.A.                                              *  IC2234.2
067300*                                                              *  IC2234.2
067400*    THE PROJECT TEAM MEMBERS WERE:                            *  IC2234.2
067500*                                                              *  IC2234.2
067600*          BIADI (BUREAU INTER ADMINISTRATION                  *  IC2234.2
067700*                 DE DOCUMENTATION INFORMATIQUE)               *  IC2234.2
067800*          21 RUE BARA                                         *  IC2234.2
067900*          F-92132 ISSY                                        *  IC2234.2
068000*          FRANCE                                              *  IC2234.2
068100*                                                              *  IC2234.2
068200*                                                              *  IC2234.2
068300*          GMD (GESELLSCHAFT FUR MATHEMATIK                    *  IC2234.2
068400*               UND DATENVERARBEITUNG MBH)                     *  IC2234.2
068500*          SCHLOSS BIRLINGHOVEN                                *  IC2234.2
068600*          POSTFACH 12 40                                      *  IC2234.2
068700*          D-5205 ST. AUGUSTIN 1                               *  IC2234.2
068800*          GERMANY FR                                          *  IC2234.2
068900*                                                              *  IC2234.2
069000*                                                              *  IC2234.2
069100*          NCC (THE NATIONAL COMPUTING CENTRE LTD)             *  IC2234.2
069200*          OXFORD ROAD                                         *  IC2234.2
069300*          MANCHESTER                                          *  IC2234.2
069400*          M1 7ED                                              *  IC2234.2
069500*          UNITED KINGDOM                                      *  IC2234.2
069600*                                                              *  IC2234.2
069700*                                                              *  IC2234.2
069800*    THIS TEST SUITE WAS PRODUCED BY THE NATIONAL COMPUTING    *  IC2234.2
069900*    CENTRE IN ENGLAND AND IS THE OFFICIAL CCVS TEST SUITE     *  IC2234.2
070000*    USED THROUGHOUT EUROPE AND THE UNITED STATES OF AMERICA.  *  IC2234.2
070100*                                                              *  IC2234.2
070200****************************************************************  IC2234.2
070300*                                                              *  IC2234.2
070400*    VALIDATION FOR:-                                          *  IC2234.2
070500*                                                              *  IC2234.2
070600*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IC2234.2
070700*                                                              *  IC2234.2
070800*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IC2234.2
070900*                                                              *  IC2234.2
071000****************************************************************  IC2234.2
071100*                                                              *  IC2234.2
071200*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  IC2234.2
071300*                                                              *  IC2234.2
071400*        X-55  - SYSTEM PRINTER NAME.                          *  IC2234.2
071500*        X-82  - SOURCE COMPUTER NAME.                         *  IC2234.2
071600*        X-83  - OBJECT COMPUTER NAME.                         *  IC2234.2
071700*                                                              *  IC2234.2
071800****************************************************************  IC2234.2
071900*                                                              *  IC2234.2
072000*    PROGRAM IC223A AND IC223A-1 WILL TEST THE NEW LANGUAGE    *  IC2234.2
072100*    ELEMENTS FOR THE LEVEL 2 INTER-PROGRAM COMMUNICATION      *  IC2234.2
072200*    MODULE.                                                   *  IC2234.2
072300*    THE NEW LANGUAGE ELEMENTS TO BE TESTED WILL BE:           *  IC2234.2
072400*          "BY REFERENCE"     PHRASE                           *  IC2234.2
072500*    THE TWO PROGRAMS WILL BE COMPILED IN THE SAME FLOW        *  IC2234.2
072600*    (TO TEST THE "END PROGRAM" STATEMENT) AS SHOWN BELOW:     *  IC2234.2
072700*    IDENTIFICATION DIVISION.                                  *  IC2234.2
072800*    PROGRAM-ID. IC223A.                                       *  IC2234.2
072900*              .                                               *  IC2234.2
073000*              .                                               *  IC2234.2
073100*              .                                               *  IC2234.2
073200*    END PROGRAM IC223A.                                       *  IC2234.2
073300*    PROGRAM-ID. IC223A-1.                                     *  IC2234.2
073400*              .                                               *  IC2234.2
073500*              .                                               *  IC2234.2
073600*              .                                               *  IC2234.2
073700****************************************************************  IC2234.2
073800 ENVIRONMENT DIVISION.                                            IC2234.2
073900 CONFIGURATION SECTION.                                           IC2234.2
074000 SOURCE-COMPUTER.                                                 IC2234.2
074100     XXXXX082.                                                    IC2234.2
074200 OBJECT-COMPUTER.                                                 IC2234.2
074300     XXXXX083.                                                    IC2234.2
074400 INPUT-OUTPUT SECTION.                                            IC2234.2
074500 FILE-CONTROL.                                                    IC2234.2
074600     SELECT PRINT-FILE ASSIGN TO                                  IC2234.2
074700     XXXXX055.                                                    IC2234.2
074800 DATA DIVISION.                                                   IC2234.2
074900 FILE SECTION.                                                    IC2234.2
075000 FD  PRINT-FILE.                                                  IC2234.2
075100 01  PRINT-REC PICTURE X(120).                                    IC2234.2
075200 01  DUMMY-RECORD PICTURE X(120).                                 IC2234.2
075300 WORKING-STORAGE SECTION.                                         IC2234.2
075400 77  WS1 PICTURE S999.                                            IC2234.2
075500 77  WS2 PICTURE S999                                             IC2234.2
075600         USAGE COMPUTATIONAL, VALUE ZERO.                         IC2234.2
075700 LINKAGE SECTION.                                                 IC2234.2
075800 77  DN1 PICTURE S99.                                             IC2234.2
075900 77  DN2 PICTURE S99 USAGE COMPUTATIONAL.                         IC2234.2
076000 77  DN3 PICTURE S99.                                             IC2234.2
076100 77  DN4 PICTURE S99 USAGE COMPUTATIONAL.                         IC2234.2
076200 PROCEDURE DIVISION USING DN1, DN2, DN3, DN4.                     IC2234.2
076300 SECT-IC223A-1-001 SECTION.                                       IC2234.2
076400 CALL-TEST-001.                                                   IC2234.2
076500     MOVE DN1 TO WS1.                                             IC2234.2
076600     ADD 1 TO WS1.                                                IC2234.2
076700     ADD 1 TO WS2.                                                IC2234.2
076800     MOVE WS1 TO DN3.                                             IC2234.2
076900     MOVE WS2 TO DN4.                                             IC2234.2
077000 CALL-EXIT-001.                                                   IC2234.2
077100     EXIT PROGRAM.                                                IC2234.2
      *END-OF,IC223A                                                                  
