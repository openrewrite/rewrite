      *HEADER,COBOL,IC103A                                                            
000100 IDENTIFICATION DIVISION.                                         IC1034.2
000200 PROGRAM-ID.                                                      IC1034.2
000300     IC103A.                                                      IC1034.2
000400****************************************************************  IC1034.2
000500*                                                              *  IC1034.2
000600*    VALIDATION FOR:-                                          *  IC1034.2
000700*                                                              *  IC1034.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IC1034.2
000900*                                                              *  IC1034.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IC1034.2
001100*                                                              *  IC1034.2
001200****************************************************************  IC1034.2
001300*                                                              *  IC1034.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  IC1034.2
001500*                                                              *  IC1034.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  IC1034.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  IC1034.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  IC1034.2
001900*                                                              *  IC1034.2
002000****************************************************************  IC1034.2
002100*        THIS PROGRAM TESTS THE USE OF MULTIPLE DATA-NAMES        IC1034.2
002200*    IN THE USING PHRASE OF THE CALL STATEMENT.  TWO 01 GROUP     IC1034.2
002300*    ITEMS AND AN ELEMENTARY 77 ITEM ARE THE PARAMETERS.  THE     IC1034.2
002400*    DATA DEFINITIONS FOR THE GROUP ITEM PARAMETERS ARE NOT       IC1034.2
002500*    THE SAME AS IN THE SUBPROGRAM BUT THE NUMBER OF CHARACTERS   IC1034.2
002600*    ARE IDENTICAL.                                               IC1034.2
002700*        THIS PROGRAM ALSO CALLS A SUBPROGRAM WITH MORE           IC1034.2
002800*    THAN ONE EXIT PROGRAM STATEMENT.                             IC1034.2
002900 ENVIRONMENT DIVISION.                                            IC1034.2
003000 CONFIGURATION SECTION.                                           IC1034.2
003100 SOURCE-COMPUTER.                                                 IC1034.2
003200     XXXXX082.                                                    IC1034.2
003300 OBJECT-COMPUTER.                                                 IC1034.2
003400     XXXXX083.                                                    IC1034.2
003500 INPUT-OUTPUT SECTION.                                            IC1034.2
003600 FILE-CONTROL.                                                    IC1034.2
003700     SELECT PRINT-FILE ASSIGN TO                                  IC1034.2
003800     XXXXX055.                                                    IC1034.2
003900 DATA DIVISION.                                                   IC1034.2
004000 FILE SECTION.                                                    IC1034.2
004100 FD  PRINT-FILE.                                                  IC1034.2
004200 01  PRINT-REC PICTURE X(120).                                    IC1034.2
004300 01  DUMMY-RECORD PICTURE X(120).                                 IC1034.2
004400 WORKING-STORAGE SECTION.                                         IC1034.2
004500 77  MAIN-DN1 PICTURE 999.                                        IC1034.2
004600 77  MAIN-DN2 PICTURE S99 COMPUTATIONAL.                          IC1034.2
004700 77  ELEM-77   PICTURE V9(4) COMPUTATIONAL.                       IC1034.2
004800 01  GROUP-01.                                                    IC1034.2
004900     02 ALPHA-NUM-FIELD  PIC X(5).                                IC1034.2
005000     02 GROUP-LEV2.                                               IC1034.2
005100        03 NUMER-FIELD PIC 99.                                    IC1034.2
005200        03 ALPHA-FIELD PIC A(3).                                  IC1034.2
005300 01  GROUP-02.                                                    IC1034.2
005400     02  NUM-ITEM PIC S99.                                        IC1034.2
005500     02  ALPHA-EDITED PICTURE X(6).                               IC1034.2
005600 01  TEST-RESULTS.                                                IC1034.2
005700     02 FILLER                   PIC X      VALUE SPACE.          IC1034.2
005800     02 FEATURE                  PIC X(20)  VALUE SPACE.          IC1034.2
005900     02 FILLER                   PIC X      VALUE SPACE.          IC1034.2
006000     02 P-OR-F                   PIC X(5)   VALUE SPACE.          IC1034.2
006100     02 FILLER                   PIC X      VALUE SPACE.          IC1034.2
006200     02  PAR-NAME.                                                IC1034.2
006300       03 FILLER                 PIC X(19)  VALUE SPACE.          IC1034.2
006400       03  PARDOT-X              PIC X      VALUE SPACE.          IC1034.2
006500       03 DOTVALUE               PIC 99     VALUE ZERO.           IC1034.2
006600     02 FILLER                   PIC X(8)   VALUE SPACE.          IC1034.2
006700     02 RE-MARK                  PIC X(61).                       IC1034.2
006800 01  TEST-COMPUTED.                                               IC1034.2
006900     02 FILLER                   PIC X(30)  VALUE SPACE.          IC1034.2
007000     02 FILLER                   PIC X(17)  VALUE                 IC1034.2
007100            "       COMPUTED=".                                   IC1034.2
007200     02 COMPUTED-X.                                               IC1034.2
007300     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          IC1034.2
007400     03 COMPUTED-N               REDEFINES COMPUTED-A             IC1034.2
007500                                 PIC -9(9).9(9).                  IC1034.2
007600     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         IC1034.2
007700     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     IC1034.2
007800     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     IC1034.2
007900     03       CM-18V0 REDEFINES COMPUTED-A.                       IC1034.2
008000         04 COMPUTED-18V0                    PIC -9(18).          IC1034.2
008100         04 FILLER                           PIC X.               IC1034.2
008200     03 FILLER PIC X(50) VALUE SPACE.                             IC1034.2
008300 01  TEST-CORRECT.                                                IC1034.2
008400     02 FILLER PIC X(30) VALUE SPACE.                             IC1034.2
008500     02 FILLER PIC X(17) VALUE "       CORRECT =".                IC1034.2
008600     02 CORRECT-X.                                                IC1034.2
008700     03 CORRECT-A                  PIC X(20) VALUE SPACE.         IC1034.2
008800     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      IC1034.2
008900     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         IC1034.2
009000     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     IC1034.2
009100     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     IC1034.2
009200     03      CR-18V0 REDEFINES CORRECT-A.                         IC1034.2
009300         04 CORRECT-18V0                     PIC -9(18).          IC1034.2
009400         04 FILLER                           PIC X.               IC1034.2
009500     03 FILLER PIC X(2) VALUE SPACE.                              IC1034.2
009600     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     IC1034.2
009700 01  CCVS-C-1.                                                    IC1034.2
009800     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAIC1034.2
009900-    "SS  PARAGRAPH-NAME                                          IC1034.2
010000-    "       REMARKS".                                            IC1034.2
010100     02 FILLER                     PIC X(20)    VALUE SPACE.      IC1034.2
010200 01  CCVS-C-2.                                                    IC1034.2
010300     02 FILLER                     PIC X        VALUE SPACE.      IC1034.2
010400     02 FILLER                     PIC X(6)     VALUE "TESTED".   IC1034.2
010500     02 FILLER                     PIC X(15)    VALUE SPACE.      IC1034.2
010600     02 FILLER                     PIC X(4)     VALUE "FAIL".     IC1034.2
010700     02 FILLER                     PIC X(94)    VALUE SPACE.      IC1034.2
010800 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       IC1034.2
010900 01  REC-CT                        PIC 99       VALUE ZERO.       IC1034.2
011000 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       IC1034.2
011100 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       IC1034.2
011200 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       IC1034.2
011300 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       IC1034.2
011400 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       IC1034.2
011500 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       IC1034.2
011600 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      IC1034.2
011700 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       IC1034.2
011800 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     IC1034.2
011900 01  CCVS-H-1.                                                    IC1034.2
012000     02  FILLER                    PIC X(39)    VALUE SPACES.     IC1034.2
012100     02  FILLER                    PIC X(42)    VALUE             IC1034.2
012200     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 IC1034.2
012300     02  FILLER                    PIC X(39)    VALUE SPACES.     IC1034.2
012400 01  CCVS-H-2A.                                                   IC1034.2
012500   02  FILLER                        PIC X(40)  VALUE SPACE.      IC1034.2
012600   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  IC1034.2
012700   02  FILLER                        PIC XXXX   VALUE             IC1034.2
012800     "4.2 ".                                                      IC1034.2
012900   02  FILLER                        PIC X(28)  VALUE             IC1034.2
013000            " COPY - NOT FOR DISTRIBUTION".                       IC1034.2
013100   02  FILLER                        PIC X(41)  VALUE SPACE.      IC1034.2
013200                                                                  IC1034.2
013300 01  CCVS-H-2B.                                                   IC1034.2
013400   02  FILLER                        PIC X(15)  VALUE             IC1034.2
013500            "TEST RESULT OF ".                                    IC1034.2
013600   02  TEST-ID                       PIC X(9).                    IC1034.2
013700   02  FILLER                        PIC X(4)   VALUE             IC1034.2
013800            " IN ".                                               IC1034.2
013900   02  FILLER                        PIC X(12)  VALUE             IC1034.2
014000     " HIGH       ".                                              IC1034.2
014100   02  FILLER                        PIC X(22)  VALUE             IC1034.2
014200            " LEVEL VALIDATION FOR ".                             IC1034.2
014300   02  FILLER                        PIC X(58)  VALUE             IC1034.2
014400     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IC1034.2
014500 01  CCVS-H-3.                                                    IC1034.2
014600     02  FILLER                      PIC X(34)  VALUE             IC1034.2
014700            " FOR OFFICIAL USE ONLY    ".                         IC1034.2
014800     02  FILLER                      PIC X(58)  VALUE             IC1034.2
014900     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IC1034.2
015000     02  FILLER                      PIC X(28)  VALUE             IC1034.2
015100            "  COPYRIGHT   1985 ".                                IC1034.2
015200 01  CCVS-E-1.                                                    IC1034.2
015300     02 FILLER                       PIC X(52)  VALUE SPACE.      IC1034.2
015400     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              IC1034.2
015500     02 ID-AGAIN                     PIC X(9).                    IC1034.2
015600     02 FILLER                       PIC X(45)  VALUE SPACES.     IC1034.2
015700 01  CCVS-E-2.                                                    IC1034.2
015800     02  FILLER                      PIC X(31)  VALUE SPACE.      IC1034.2
015900     02  FILLER                      PIC X(21)  VALUE SPACE.      IC1034.2
016000     02 CCVS-E-2-2.                                               IC1034.2
016100         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      IC1034.2
016200         03 FILLER                   PIC X      VALUE SPACE.      IC1034.2
016300         03 ENDER-DESC               PIC X(44)  VALUE             IC1034.2
016400            "ERRORS ENCOUNTERED".                                 IC1034.2
016500 01  CCVS-E-3.                                                    IC1034.2
016600     02  FILLER                      PIC X(22)  VALUE             IC1034.2
016700            " FOR OFFICIAL USE ONLY".                             IC1034.2
016800     02  FILLER                      PIC X(12)  VALUE SPACE.      IC1034.2
016900     02  FILLER                      PIC X(58)  VALUE             IC1034.2
017000     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IC1034.2
017100     02  FILLER                      PIC X(13)  VALUE SPACE.      IC1034.2
017200     02 FILLER                       PIC X(15)  VALUE             IC1034.2
017300             " COPYRIGHT 1985".                                   IC1034.2
017400 01  CCVS-E-4.                                                    IC1034.2
017500     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      IC1034.2
017600     02 FILLER                       PIC X(4)   VALUE " OF ".     IC1034.2
017700     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      IC1034.2
017800     02 FILLER                       PIC X(40)  VALUE             IC1034.2
017900      "  TESTS WERE EXECUTED SUCCESSFULLY".                       IC1034.2
018000 01  XXINFO.                                                      IC1034.2
018100     02 FILLER                       PIC X(19)  VALUE             IC1034.2
018200            "*** INFORMATION ***".                                IC1034.2
018300     02 INFO-TEXT.                                                IC1034.2
018400       04 FILLER                     PIC X(8)   VALUE SPACE.      IC1034.2
018500       04 XXCOMPUTED                 PIC X(20).                   IC1034.2
018600       04 FILLER                     PIC X(5)   VALUE SPACE.      IC1034.2
018700       04 XXCORRECT                  PIC X(20).                   IC1034.2
018800     02 INF-ANSI-REFERENCE           PIC X(48).                   IC1034.2
018900 01  HYPHEN-LINE.                                                 IC1034.2
019000     02 FILLER  PIC IS X VALUE IS SPACE.                          IC1034.2
019100     02 FILLER  PIC IS X(65)    VALUE IS "************************IC1034.2
019200-    "*****************************************".                 IC1034.2
019300     02 FILLER  PIC IS X(54)    VALUE IS "************************IC1034.2
019400-    "******************************".                            IC1034.2
019500 01  CCVS-PGM-ID                     PIC X(9)   VALUE             IC1034.2
019600     "IC103A".                                                    IC1034.2
019700 PROCEDURE DIVISION.                                              IC1034.2
019800 CCVS1 SECTION.                                                   IC1034.2
019900 OPEN-FILES.                                                      IC1034.2
020000     OPEN     OUTPUT PRINT-FILE.                                  IC1034.2
020100     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   IC1034.2
020200     MOVE    SPACE TO TEST-RESULTS.                               IC1034.2
020300     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             IC1034.2
020400     GO TO CCVS1-EXIT.                                            IC1034.2
020500 CLOSE-FILES.                                                     IC1034.2
020600     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   IC1034.2
020700 TERMINATE-CCVS.                                                  IC1034.2
020800*    EXIT PROGRAM.                                                IC1034.2
020900*TERMINATE-CALL.                                                  IC1034.2
021000     STOP     RUN.                                                IC1034.2
021100 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         IC1034.2
021200 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           IC1034.2
021300 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          IC1034.2
021400 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      IC1034.2
021500     MOVE "****TEST DELETED****" TO RE-MARK.                      IC1034.2
021600 PRINT-DETAIL.                                                    IC1034.2
021700     IF REC-CT NOT EQUAL TO ZERO                                  IC1034.2
021800             MOVE "." TO PARDOT-X                                 IC1034.2
021900             MOVE REC-CT TO DOTVALUE.                             IC1034.2
022000     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      IC1034.2
022100     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               IC1034.2
022200        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 IC1034.2
022300          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 IC1034.2
022400     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              IC1034.2
022500     MOVE SPACE TO CORRECT-X.                                     IC1034.2
022600     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         IC1034.2
022700     MOVE     SPACE TO RE-MARK.                                   IC1034.2
022800 HEAD-ROUTINE.                                                    IC1034.2
022900     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IC1034.2
023000     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IC1034.2
023100     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IC1034.2
023200     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IC1034.2
023300 COLUMN-NAMES-ROUTINE.                                            IC1034.2
023400     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IC1034.2
023500     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IC1034.2
023600     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        IC1034.2
023700 END-ROUTINE.                                                     IC1034.2
023800     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.IC1034.2
023900 END-RTN-EXIT.                                                    IC1034.2
024000     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IC1034.2
024100 END-ROUTINE-1.                                                   IC1034.2
024200      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      IC1034.2
024300      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               IC1034.2
024400      ADD PASS-COUNTER TO ERROR-HOLD.                             IC1034.2
024500*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   IC1034.2
024600      MOVE PASS-COUNTER TO CCVS-E-4-1.                            IC1034.2
024700      MOVE ERROR-HOLD TO CCVS-E-4-2.                              IC1034.2
024800      MOVE CCVS-E-4 TO CCVS-E-2-2.                                IC1034.2
024900      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           IC1034.2
025000  END-ROUTINE-12.                                                 IC1034.2
025100      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        IC1034.2
025200     IF       ERROR-COUNTER IS EQUAL TO ZERO                      IC1034.2
025300         MOVE "NO " TO ERROR-TOTAL                                IC1034.2
025400         ELSE                                                     IC1034.2
025500         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       IC1034.2
025600     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           IC1034.2
025700     PERFORM WRITE-LINE.                                          IC1034.2
025800 END-ROUTINE-13.                                                  IC1034.2
025900     IF DELETE-COUNTER IS EQUAL TO ZERO                           IC1034.2
026000         MOVE "NO " TO ERROR-TOTAL  ELSE                          IC1034.2
026100         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      IC1034.2
026200     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   IC1034.2
026300     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IC1034.2
026400      IF   INSPECT-COUNTER EQUAL TO ZERO                          IC1034.2
026500          MOVE "NO " TO ERROR-TOTAL                               IC1034.2
026600      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   IC1034.2
026700      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            IC1034.2
026800      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          IC1034.2
026900     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IC1034.2
027000 WRITE-LINE.                                                      IC1034.2
027100     ADD 1 TO RECORD-COUNT.                                       IC1034.2
027200*    IF RECORD-COUNT GREATER 50                                   IC1034.2
027300*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          IC1034.2
027400*        MOVE SPACE TO DUMMY-RECORD                               IC1034.2
027500*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  IC1034.2
027600*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             IC1034.2
027700*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     IC1034.2
027800*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          IC1034.2
027900*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          IC1034.2
028000*        MOVE ZERO TO RECORD-COUNT.                               IC1034.2
028100     PERFORM WRT-LN.                                              IC1034.2
028200 WRT-LN.                                                          IC1034.2
028300     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               IC1034.2
028400     MOVE SPACE TO DUMMY-RECORD.                                  IC1034.2
028500 BLANK-LINE-PRINT.                                                IC1034.2
028600     PERFORM WRT-LN.                                              IC1034.2
028700 FAIL-ROUTINE.                                                    IC1034.2
028800     IF   COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE. IC1034.2
028900     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.IC1034.2
029000     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IC1034.2
029100     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   IC1034.2
029200     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IC1034.2
029300     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IC1034.2
029400     GO TO  FAIL-ROUTINE-EX.                                      IC1034.2
029500 FAIL-ROUTINE-WRITE.                                              IC1034.2
029600     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         IC1034.2
029700     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 IC1034.2
029800     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. IC1034.2
029900     MOVE   SPACES TO COR-ANSI-REFERENCE.                         IC1034.2
030000 FAIL-ROUTINE-EX. EXIT.                                           IC1034.2
030100 BAIL-OUT.                                                        IC1034.2
030200     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   IC1034.2
030300     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           IC1034.2
030400 BAIL-OUT-WRITE.                                                  IC1034.2
030500     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  IC1034.2
030600     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IC1034.2
030700     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IC1034.2
030800     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IC1034.2
030900 BAIL-OUT-EX. EXIT.                                               IC1034.2
031000 CCVS1-EXIT.                                                      IC1034.2
031100     EXIT.                                                        IC1034.2
031200 SECT-IC103-0001 SECTION.                                         IC1034.2
031300*        THE TESTS IN THIS SECTION CALL A SUBPROGRAM WHICH        IC1034.2
031400*    HAS FOUR EXIT PROGRAM STATEMENTS.  A DIFFERENT EXIT IS       IC1034.2
031500*    TAKEN FOR EACH CALL TO THE SUBPROGRAM.                       IC1034.2
031600 EXIT-INIT.                                                       IC1034.2
031700     MOVE "MULTIPLE EXIT PROGRM" TO FEATURE.                      IC1034.2
031800 EXIT-INIT-001.                                                   IC1034.2
031900     MOVE 0 TO MAIN-DN2.                                          IC1034.2
032000     MOVE 1 TO MAIN-DN1.                                          IC1034.2
032100 EXIT-TEST-001.                                                   IC1034.2
032200     CALL "IC105A" USING MAIN-DN1 MAIN-DN2.                       IC1034.2
032300     IF MAIN-DN2 EQUAL TO 1                                       IC1034.2
032400         PERFORM PASS                                             IC1034.2
032500         GO TO EXIT-WRITE-001.                                    IC1034.2
032600 EXIT-FAIL-001.                                                   IC1034.2
032700     MOVE MAIN-DN1 TO CORRECT-18V0.                               IC1034.2
032800     MOVE MAIN-DN2 TO COMPUTED-18V0.                              IC1034.2
032900     MOVE "FIRST EXIT FROM SUBPROGRAM" TO RE-MARK.                IC1034.2
033000     PERFORM FAIL.                                                IC1034.2
033100 EXIT-WRITE-001.                                                  IC1034.2
033200     MOVE "EXIT-TEST-01" TO PAR-NAME.                             IC1034.2
033300     PERFORM PRINT-DETAIL.                                        IC1034.2
033400 EXIT-INIT-002.                                                   IC1034.2
033500     MOVE 0 TO MAIN-DN2.                                          IC1034.2
033600     MOVE 2 TO MAIN-DN1.                                          IC1034.2
033700 EXIT-TEST-002.                                                   IC1034.2
033800     CALL "IC105A" USING MAIN-DN1 MAIN-DN2.                       IC1034.2
033900     IF MAIN-DN2 EQUAL TO 2                                       IC1034.2
034000          PERFORM PASS                                            IC1034.2
034100          GO TO EXIT-WRITE-002.                                   IC1034.2
034200 EXIT-FAIL-002.                                                   IC1034.2
034300     MOVE MAIN-DN1 TO CORRECT-18V0.                               IC1034.2
034400     MOVE MAIN-DN2 TO COMPUTED-18V0.                              IC1034.2
034500     MOVE "SECOND EXIT FROM SUBPROGRAM" TO RE-MARK.               IC1034.2
034600     PERFORM FAIL.                                                IC1034.2
034700 EXIT-WRITE-002.                                                  IC1034.2
034800     MOVE "EXIT-TEST-02" TO PAR-NAME.                             IC1034.2
034900     PERFORM PRINT-DETAIL.                                        IC1034.2
035000 EXIT-INIT-003.                                                   IC1034.2
035100     MOVE 0 TO MAIN-DN2.                                          IC1034.2
035200     MOVE 3 TO MAIN-DN1.                                          IC1034.2
035300 EXIT-TEST-003.                                                   IC1034.2
035400     CALL "IC105A" USING MAIN-DN1 MAIN-DN2.                       IC1034.2
035500     IF MAIN-DN2 NOT EQUAL TO 3                                   IC1034.2
035600         GO TO EXIT-FAIL-003.                                     IC1034.2
035700     PERFORM PASS.                                                IC1034.2
035800     GO TO EXIT-WRITE-003.                                        IC1034.2
035900 EXIT-FAIL-003.                                                   IC1034.2
036000     MOVE MAIN-DN1 TO CORRECT-18V0.                               IC1034.2
036100     MOVE MAIN-DN2 TO COMPUTED-18V0.                              IC1034.2
036200     MOVE "THIRD EXIT FROM SUBPROGRAM" TO RE-MARK.                IC1034.2
036300     PERFORM FAIL.                                                IC1034.2
036400 EXIT-WRITE-003.                                                  IC1034.2
036500     MOVE "EXIT-TEST-03" TO PAR-NAME.                             IC1034.2
036600     PERFORM PRINT-DETAIL.                                        IC1034.2
036700 EXIT-INIT-004.                                                   IC1034.2
036800     MOVE 0 TO MAIN-DN2.                                          IC1034.2
036900     MOVE 4 TO MAIN-DN1.                                          IC1034.2
037000 EXIT-TEST-004.                                                   IC1034.2
037100     CALL "IC105A" USING MAIN-DN1 MAIN-DN2.                       IC1034.2
037200     IF MAIN-DN2 NOT EQUAL TO 4                                   IC1034.2
037300         GO TO EXIT-FAIL-004.                                     IC1034.2
037400     PERFORM PASS.                                                IC1034.2
037500     GO TO EXIT-WRITE-004.                                        IC1034.2
037600 EXIT-FAIL-004.                                                   IC1034.2
037700     MOVE MAIN-DN1 TO CORRECT-18V0.                               IC1034.2
037800     MOVE MAIN-DN2 TO COMPUTED-18V0.                              IC1034.2
037900     MOVE "FOURTH EXIT FROM SUBPROGRAM" TO RE-MARK.               IC1034.2
038000     PERFORM FAIL.                                                IC1034.2
038100 EXIT-WRITE-004.                                                  IC1034.2
038200     MOVE "EXIT-TEST-04" TO PAR-NAME.                             IC1034.2
038300     PERFORM PRINT-DETAIL.                                        IC1034.2
038400     GO TO SECT-IC103-0002.                                       IC1034.2
038500 EXIT-DELETES.                                                    IC1034.2
038600*        IF THE SUBPROGRAM WITH MULTIPLE EXIT PROGRAM             IC1034.2
038700*    STATEMENTS CANNOT BE INCLUDED IN THE RUN UNIT                IC1034.2
038800*    DELETE PARAGRAPH EXIT-INIT-001 THRU EXIT-WRITE-004.          IC1034.2
038900     PERFORM DE-LETE.                                             IC1034.2
039000     MOVE "EXIT-TEST-01" TO PAR-NAME.                             IC1034.2
039100     PERFORM PRINT-DETAIL.                                        IC1034.2
039200     PERFORM DE-LETE.                                             IC1034.2
039300     MOVE "EXIT-TEST-02" TO PAR-NAME.                             IC1034.2
039400     PERFORM PRINT-DETAIL.                                        IC1034.2
039500     PERFORM DE-LETE.                                             IC1034.2
039600     MOVE "EXIT-TEST-03" TO PAR-NAME.                             IC1034.2
039700     PERFORM PRINT-DETAIL.                                        IC1034.2
039800     PERFORM DE-LETE.                                             IC1034.2
039900     MOVE "EXIT-TEST-04" TO PAR-NAME.                             IC1034.2
040000     PERFORM PRINT-DETAIL.                                        IC1034.2
040100 SECT-IC103-0002 SECTION.                                         IC1034.2
040200*        THIS SECTION CALLS A SUBPROGRAM WITH TWO GROUP ITEMS     IC1034.2
040300*    AND ONE ELEMENTARY ITEM IN THE USING PHRASE. THE ITEM        IC1034.2
040400*    DESCRIPTIONS ARE DIFFERENT IN THE SUBPROGRAM FROM THE MAIN   IC1034.2
040500*    PROGRAM, BUT THE NUMBER OF CHARACTERS IS IDENTICAL.          IC1034.2
040600*    REFERENCE  X3.23-1974,  SECTION XII, 3.1 AND 3.2.            IC1034.2
040700 CALL-INIT-06.                                                    IC1034.2
040800     MOVE "CALL-TEST-06" TO PAR-NAME.                             IC1034.2
040900     MOVE 0 TO NUMER-FIELD  ELEM-77 NUM-ITEM.                     IC1034.2
041000     MOVE SPACE TO ALPHA-NUM-FIELD ALPHA-FIELD ALPHA-EDITED.      IC1034.2
041100     MOVE "CALL USING DN SERIES" TO FEATURE.                      IC1034.2
041200 CALL-TEST-06.                                                    IC1034.2
041300     CALL "IC104A" USING GROUP-01 ELEM-77 GROUP-02.               IC1034.2
041400     GO TO CALL-TEST-06-01.                                       IC1034.2
041500 CALL-DELETE-06.                                                  IC1034.2
041600     PERFORM DE-LETE.                                             IC1034.2
041700     PERFORM PRINT-DETAIL.                                        IC1034.2
041800     GO TO CCVS-EXIT.                                             IC1034.2
041900*       IF IC104 CANNOT BE INCLUDED IN THE RUN UNIT               IC1034.2
042000*    DELETE THE PARAGRAPH CALL-TEST-06.                           IC1034.2
042100 CALL-TEST-06-01.                                                 IC1034.2
042200     IF ALPHA-NUM-FIELD NOT EQUAL TO "IC104"                      IC1034.2
042300         GO TO CALL-FAIL-06-01.                                   IC1034.2
042400     PERFORM PASS.                                                IC1034.2
042500     GO TO CALL-WRITE-06-01.                                      IC1034.2
042600 CALL-FAIL-06-01.                                                 IC1034.2
042700     MOVE ALPHA-NUM-FIELD TO COMPUTED-A.                          IC1034.2
042800     MOVE "IC104" TO CORRECT-A.                                   IC1034.2
042900     PERFORM FAIL.                                                IC1034.2
043000     MOVE "ALPHANUMERIC PARAMETER" TO RE-MARK.                    IC1034.2
043100 CALL-WRITE-06-01.                                                IC1034.2
043200     ADD 1 TO REC-CT.                                             IC1034.2
043300     PERFORM PRINT-DETAIL.                                        IC1034.2
043400 CALL-TEST-06-02.                                                 IC1034.2
043500     IF NUMER-FIELD EQUAL TO 25                                   IC1034.2
043600         PERFORM PASS                                             IC1034.2
043700         GO TO CALL-WRITE-06-02.                                  IC1034.2
043800 CALL-FAIL-06-02.                                                 IC1034.2
043900     PERFORM FAIL.                                                IC1034.2
044000     MOVE NUMER-FIELD TO COMPUTED-18V0.                           IC1034.2
044100     MOVE 25 TO CORRECT-18V0.                                     IC1034.2
044200     MOVE "NUMERIC DISPLAY PARAMETER" TO RE-MARK.                 IC1034.2
044300 CALL-WRITE-06-02.                                                IC1034.2
044400     ADD 1 TO REC-CT.                                             IC1034.2
044500     PERFORM PRINT-DETAIL.                                        IC1034.2
044600 CALL-TEST-06-03.                                                 IC1034.2
044700     IF ALPHA-FIELD EQUAL TO "YES"                                IC1034.2
044800         PERFORM PASS                                             IC1034.2
044900         GO TO CALL-WRITE-06-03.                                  IC1034.2
045000 CALL-FAIL-06-03.                                                 IC1034.2
045100     PERFORM FAIL.                                                IC1034.2
045200     MOVE ALPHA-FIELD TO COMPUTED-A.                              IC1034.2
045300     MOVE "YES" TO CORRECT-A.                                     IC1034.2
045400     MOVE "ALPHABETIC PARAMETER" TO RE-MARK.                      IC1034.2
045500 CALL-WRITE-06-03.                                                IC1034.2
045600     ADD 1 TO REC-CT.                                             IC1034.2
045700     PERFORM PRINT-DETAIL.                                        IC1034.2
045800 CALL-TEST-06-04.                                                 IC1034.2
045900     IF ELEM-77 EQUAL TO 0.7654                                   IC1034.2
046000         PERFORM PASS                                             IC1034.2
046100         GO TO CALL-WRITE-06-04.                                  IC1034.2
046200 CALL-FAIL-06-04.                                                 IC1034.2
046300     PERFORM FAIL.                                                IC1034.2
046400     MOVE ELEM-77 TO COMPUTED-4V14.                               IC1034.2
046500     MOVE 0.7654 TO CORRECT-4V14.                                 IC1034.2
046600     MOVE "COMPUTATIONAL PARAMETER" TO RE-MARK.                   IC1034.2
046700 CALL-WRITE-06-04.                                                IC1034.2
046800     ADD 1 TO REC-CT.                                             IC1034.2
046900     PERFORM PRINT-DETAIL.                                        IC1034.2
047000 CALL-TEST-06-05.                                                 IC1034.2
047100     IF NUM-ITEM EQUAL TO 25                                      IC1034.2
047200         PERFORM PASS                                             IC1034.2
047300         GO TO CALL-WRITE-06-05.                                  IC1034.2
047400 CALL-FAIL-06-05.                                                 IC1034.2
047500     PERFORM FAIL.                                                IC1034.2
047600     MOVE NUM-ITEM TO COMPUTED-18V0.                              IC1034.2
047700     MOVE 25 TO CORRECT-18V0.                                     IC1034.2
047800     MOVE "SIGNED NUMERIC PARAMETER" TO RE-MARK.                  IC1034.2
047900 CALL-WRITE-06-05.                                                IC1034.2
048000     ADD 1 TO REC-CT.                                             IC1034.2
048100     PERFORM PRINT-DETAIL.                                        IC1034.2
048200 CALL-TEST-06-06.                                                 IC1034.2
048300     IF ALPHA-EDITED EQUAL TO "AB C0D"                            IC1034.2
048400         PERFORM PASS                                             IC1034.2
048500         GO TO CALL-WRITE-06-06.                                  IC1034.2
048600 CALL-FAIL-06-06.                                                 IC1034.2
048700     PERFORM FAIL.                                                IC1034.2
048800     MOVE ALPHA-EDITED TO COMPUTED-A.                             IC1034.2
048900     MOVE "AB C0D" TO CORRECT-A.                                  IC1034.2
049000     MOVE "ALPHANUMERIC EDITED" TO RE-MARK.                       IC1034.2
049100 CALL-WRITE-06-06.                                                IC1034.2
049200     ADD 1 TO REC-CT.                                             IC1034.2
049300     PERFORM PRINT-DETAIL.                                        IC1034.2
049400     GO TO CCVS-EXIT.                                             IC1034.2
049500 CCVS-EXIT SECTION.                                               IC1034.2
049600 CCVS-999999.                                                     IC1034.2
049700     GO TO CLOSE-FILES.                                           IC1034.2
      *END-OF,IC103A                                                                  
