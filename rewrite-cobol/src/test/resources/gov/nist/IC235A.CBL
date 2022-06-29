      *HEADER,COBOL,IC235A                                                            
000100 IDENTIFICATION DIVISION.                                         IC2354.2
000200 PROGRAM-ID.                                                      IC2354.2
000300     IC235A.                                                      IC2354.2
000400****************************************************************  IC2354.2
000500*                                                              *  IC2354.2
000600*    VALIDATION FOR:-                                          *  IC2354.2
000700*                                                              *  IC2354.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IC2354.2
000900*                                                              *  IC2354.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IC2354.2
001100*                                                              *  IC2354.2
001200****************************************************************  IC2354.2
001300*                                                              *  IC2354.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  IC2354.2
001500*                                                              *  IC2354.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  IC2354.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  IC2354.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  IC2354.2
001900*                                                              *  IC2354.2
002000****************************************************************  IC2354.2
002100*        THIS PROGRAM TESTS THE USE OF MULTIPLE DATA-NAMES        IC2354.2
002200*    IN THE USING PHRASE OF THE CALL STATEMENT.  TWO 01 GROUP     IC2354.2
002300*    ITEMS AND AN ELEMENTARY 77 ITEM ARE THE PARAMETERS.  THE     IC2354.2
002400*    DATA DEFINITIONS FOR THE GROUP ITEM PARAMETERS ARE NOT       IC2354.2
002500*    THE SAME AS IN THE SUBPROGRAM BUT THE NUMBER OF CHARACTERS   IC2354.2
002600*    ARE IDENTICAL.                                               IC2354.2
002700*        THIS PROGRAM ALSO CALLS A SUBPROGRAM WITH MORE           IC2354.2
002800*    THAN ONE EXIT PROGRAM STATEMENT.                             IC2354.2
002900*         REFERENCE:  AMERICAN NATIONAL STANDARD                  IC2354.2
003000*                     PROGRAMMING LANGUAGE COBOL, X3.23-1985.     IC2354.2
003100 ENVIRONMENT DIVISION.                                            IC2354.2
003200 CONFIGURATION SECTION.                                           IC2354.2
003300 SOURCE-COMPUTER.                                                 IC2354.2
003400     XXXXX082.                                                    IC2354.2
003500 OBJECT-COMPUTER.                                                 IC2354.2
003600     XXXXX083.                                                    IC2354.2
003700 INPUT-OUTPUT SECTION.                                            IC2354.2
003800 FILE-CONTROL.                                                    IC2354.2
003900     SELECT PRINT-FILE ASSIGN TO                                  IC2354.2
004000     XXXXX055.                                                    IC2354.2
004100 DATA DIVISION.                                                   IC2354.2
004200 FILE SECTION.                                                    IC2354.2
004300 FD  PRINT-FILE.                                                  IC2354.2
004400 01  PRINT-REC PICTURE X(120).                                    IC2354.2
004500 01  DUMMY-RECORD PICTURE X(120).                                 IC2354.2
004600 WORKING-STORAGE SECTION.                                         IC2354.2
004700 77  MAIN-DN1 PICTURE 999.                                        IC2354.2
004800 77  MAIN-DN2 PICTURE S99 COMPUTATIONAL.                          IC2354.2
004900 77  ELEM-77   PICTURE V9(4) COMPUTATIONAL.                       IC2354.2
005000 01  GROUP-01.                                                    IC2354.2
005100     02 ALPHA-NUM-FIELD  PIC X(8).                                IC2354.2
005200     02 GROUP-LEV2.                                               IC2354.2
005300        03 NUMER-FIELD PIC 99.                                    IC2354.2
005400        03 ALPHA-FIELD PIC A(3).                                  IC2354.2
005500 01  GROUP-02.                                                    IC2354.2
005600     02  NUM-ITEM PIC S99.                                        IC2354.2
005700     02  ALPHA-EDITED PICTURE X(6).                               IC2354.2
005800 01  GROUP-03.                                                    IC2354.2
005900     02  ALPHA-NUM-FIELD-3 PIC X(5).                              IC2354.2
006000     02  GROUP-LEV2-3.                                            IC2354.2
006100       03  NUMER-FIELD-3   PIC 99.                                IC2354.2
006200       03  ALPHA-FIELD-3   PIC A(3).                              IC2354.2
006300 01  GROUP-04.                                                    IC2354.2
006400   03  FILLER              PIC XX.                                IC2354.2
006500   03  ELEM-NON-01         PIC XX.                                IC2354.2
006600 01  FILLER.                                                      IC2354.2
006700   03  SUBSCRIPTED-DATA    OCCURS 10                              IC2354.2
006800                           PIC XX.                                IC2354.2
006900 01  TEST-RESULTS.                                                IC2354.2
007000     02 FILLER                   PIC X      VALUE SPACE.          IC2354.2
007100     02 FEATURE                  PIC X(20)  VALUE SPACE.          IC2354.2
007200     02 FILLER                   PIC X      VALUE SPACE.          IC2354.2
007300     02 P-OR-F                   PIC X(5)   VALUE SPACE.          IC2354.2
007400     02 FILLER                   PIC X      VALUE SPACE.          IC2354.2
007500     02  PAR-NAME.                                                IC2354.2
007600       03 FILLER                 PIC X(19)  VALUE SPACE.          IC2354.2
007700       03  PARDOT-X              PIC X      VALUE SPACE.          IC2354.2
007800       03 DOTVALUE               PIC 99     VALUE ZERO.           IC2354.2
007900     02 FILLER                   PIC X(8)   VALUE SPACE.          IC2354.2
008000     02 RE-MARK                  PIC X(61).                       IC2354.2
008100 01  TEST-COMPUTED.                                               IC2354.2
008200     02 FILLER                   PIC X(30)  VALUE SPACE.          IC2354.2
008300     02 FILLER                   PIC X(17)  VALUE                 IC2354.2
008400            "       COMPUTED=".                                   IC2354.2
008500     02 COMPUTED-X.                                               IC2354.2
008600     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          IC2354.2
008700     03 COMPUTED-N               REDEFINES COMPUTED-A             IC2354.2
008800                                 PIC -9(9).9(9).                  IC2354.2
008900     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         IC2354.2
009000     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     IC2354.2
009100     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     IC2354.2
009200     03       CM-18V0 REDEFINES COMPUTED-A.                       IC2354.2
009300         04 COMPUTED-18V0                    PIC -9(18).          IC2354.2
009400         04 FILLER                           PIC X.               IC2354.2
009500     03 FILLER PIC X(50) VALUE SPACE.                             IC2354.2
009600 01  TEST-CORRECT.                                                IC2354.2
009700     02 FILLER PIC X(30) VALUE SPACE.                             IC2354.2
009800     02 FILLER PIC X(17) VALUE "       CORRECT =".                IC2354.2
009900     02 CORRECT-X.                                                IC2354.2
010000     03 CORRECT-A                  PIC X(20) VALUE SPACE.         IC2354.2
010100     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      IC2354.2
010200     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         IC2354.2
010300     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     IC2354.2
010400     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     IC2354.2
010500     03      CR-18V0 REDEFINES CORRECT-A.                         IC2354.2
010600         04 CORRECT-18V0                     PIC -9(18).          IC2354.2
010700         04 FILLER                           PIC X.               IC2354.2
010800     03 FILLER PIC X(2) VALUE SPACE.                              IC2354.2
010900     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     IC2354.2
011000 01  CCVS-C-1.                                                    IC2354.2
011100     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAIC2354.2
011200-    "SS  PARAGRAPH-NAME                                          IC2354.2
011300-    "       REMARKS".                                            IC2354.2
011400     02 FILLER                     PIC X(20)    VALUE SPACE.      IC2354.2
011500 01  CCVS-C-2.                                                    IC2354.2
011600     02 FILLER                     PIC X        VALUE SPACE.      IC2354.2
011700     02 FILLER                     PIC X(6)     VALUE "TESTED".   IC2354.2
011800     02 FILLER                     PIC X(15)    VALUE SPACE.      IC2354.2
011900     02 FILLER                     PIC X(4)     VALUE "FAIL".     IC2354.2
012000     02 FILLER                     PIC X(94)    VALUE SPACE.      IC2354.2
012100 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       IC2354.2
012200 01  REC-CT                        PIC 99       VALUE ZERO.       IC2354.2
012300 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       IC2354.2
012400 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       IC2354.2
012500 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       IC2354.2
012600 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       IC2354.2
012700 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       IC2354.2
012800 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       IC2354.2
012900 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      IC2354.2
013000 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       IC2354.2
013100 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     IC2354.2
013200 01  CCVS-H-1.                                                    IC2354.2
013300     02  FILLER                    PIC X(39)    VALUE SPACES.     IC2354.2
013400     02  FILLER                    PIC X(42)    VALUE             IC2354.2
013500     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 IC2354.2
013600     02  FILLER                    PIC X(39)    VALUE SPACES.     IC2354.2
013700 01  CCVS-H-2A.                                                   IC2354.2
013800   02  FILLER                        PIC X(40)  VALUE SPACE.      IC2354.2
013900   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  IC2354.2
014000   02  FILLER                        PIC XXXX   VALUE             IC2354.2
014100     "4.2 ".                                                      IC2354.2
014200   02  FILLER                        PIC X(28)  VALUE             IC2354.2
014300            " COPY - NOT FOR DISTRIBUTION".                       IC2354.2
014400   02  FILLER                        PIC X(41)  VALUE SPACE.      IC2354.2
014500                                                                  IC2354.2
014600 01  CCVS-H-2B.                                                   IC2354.2
014700   02  FILLER                        PIC X(15)  VALUE             IC2354.2
014800            "TEST RESULT OF ".                                    IC2354.2
014900   02  TEST-ID                       PIC X(9).                    IC2354.2
015000   02  FILLER                        PIC X(4)   VALUE             IC2354.2
015100            " IN ".                                               IC2354.2
015200   02  FILLER                        PIC X(12)  VALUE             IC2354.2
015300     " HIGH       ".                                              IC2354.2
015400   02  FILLER                        PIC X(22)  VALUE             IC2354.2
015500            " LEVEL VALIDATION FOR ".                             IC2354.2
015600   02  FILLER                        PIC X(58)  VALUE             IC2354.2
015700     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IC2354.2
015800 01  CCVS-H-3.                                                    IC2354.2
015900     02  FILLER                      PIC X(34)  VALUE             IC2354.2
016000            " FOR OFFICIAL USE ONLY    ".                         IC2354.2
016100     02  FILLER                      PIC X(58)  VALUE             IC2354.2
016200     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IC2354.2
016300     02  FILLER                      PIC X(28)  VALUE             IC2354.2
016400            "  COPYRIGHT   1985 ".                                IC2354.2
016500 01  CCVS-E-1.                                                    IC2354.2
016600     02 FILLER                       PIC X(52)  VALUE SPACE.      IC2354.2
016700     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              IC2354.2
016800     02 ID-AGAIN                     PIC X(9).                    IC2354.2
016900     02 FILLER                       PIC X(45)  VALUE SPACES.     IC2354.2
017000 01  CCVS-E-2.                                                    IC2354.2
017100     02  FILLER                      PIC X(31)  VALUE SPACE.      IC2354.2
017200     02  FILLER                      PIC X(21)  VALUE SPACE.      IC2354.2
017300     02 CCVS-E-2-2.                                               IC2354.2
017400         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      IC2354.2
017500         03 FILLER                   PIC X      VALUE SPACE.      IC2354.2
017600         03 ENDER-DESC               PIC X(44)  VALUE             IC2354.2
017700            "ERRORS ENCOUNTERED".                                 IC2354.2
017800 01  CCVS-E-3.                                                    IC2354.2
017900     02  FILLER                      PIC X(22)  VALUE             IC2354.2
018000            " FOR OFFICIAL USE ONLY".                             IC2354.2
018100     02  FILLER                      PIC X(12)  VALUE SPACE.      IC2354.2
018200     02  FILLER                      PIC X(58)  VALUE             IC2354.2
018300     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IC2354.2
018400     02  FILLER                      PIC X(13)  VALUE SPACE.      IC2354.2
018500     02 FILLER                       PIC X(15)  VALUE             IC2354.2
018600             " COPYRIGHT 1985".                                   IC2354.2
018700 01  CCVS-E-4.                                                    IC2354.2
018800     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      IC2354.2
018900     02 FILLER                       PIC X(4)   VALUE " OF ".     IC2354.2
019000     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      IC2354.2
019100     02 FILLER                       PIC X(40)  VALUE             IC2354.2
019200      "  TESTS WERE EXECUTED SUCCESSFULLY".                       IC2354.2
019300 01  XXINFO.                                                      IC2354.2
019400     02 FILLER                       PIC X(19)  VALUE             IC2354.2
019500            "*** INFORMATION ***".                                IC2354.2
019600     02 INFO-TEXT.                                                IC2354.2
019700       04 FILLER                     PIC X(8)   VALUE SPACE.      IC2354.2
019800       04 XXCOMPUTED                 PIC X(20).                   IC2354.2
019900       04 FILLER                     PIC X(5)   VALUE SPACE.      IC2354.2
020000       04 XXCORRECT                  PIC X(20).                   IC2354.2
020100     02 INF-ANSI-REFERENCE           PIC X(48).                   IC2354.2
020200 01  HYPHEN-LINE.                                                 IC2354.2
020300     02 FILLER  PIC IS X VALUE IS SPACE.                          IC2354.2
020400     02 FILLER  PIC IS X(65)    VALUE IS "************************IC2354.2
020500-    "*****************************************".                 IC2354.2
020600     02 FILLER  PIC IS X(54)    VALUE IS "************************IC2354.2
020700-    "******************************".                            IC2354.2
020800 01  CCVS-PGM-ID                     PIC X(9)   VALUE             IC2354.2
020900     "IC235A".                                                    IC2354.2
021000 PROCEDURE DIVISION.                                              IC2354.2
021100 CCVS1 SECTION.                                                   IC2354.2
021200 OPEN-FILES.                                                      IC2354.2
021300     OPEN     OUTPUT PRINT-FILE.                                  IC2354.2
021400     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   IC2354.2
021500     MOVE    SPACE TO TEST-RESULTS.                               IC2354.2
021600     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             IC2354.2
021700     GO TO CCVS1-EXIT.                                            IC2354.2
021800 CLOSE-FILES.                                                     IC2354.2
021900     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   IC2354.2
022000 TERMINATE-CCVS.                                                  IC2354.2
022100*    EXIT PROGRAM.                                                IC2354.2
022200*TERMINATE-CALL.                                                  IC2354.2
022300     STOP     RUN.                                                IC2354.2
022400 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         IC2354.2
022500 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           IC2354.2
022600 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          IC2354.2
022700 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      IC2354.2
022800     MOVE "****TEST DELETED****" TO RE-MARK.                      IC2354.2
022900 PRINT-DETAIL.                                                    IC2354.2
023000     IF REC-CT NOT EQUAL TO ZERO                                  IC2354.2
023100             MOVE "." TO PARDOT-X                                 IC2354.2
023200             MOVE REC-CT TO DOTVALUE.                             IC2354.2
023300     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      IC2354.2
023400     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               IC2354.2
023500        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 IC2354.2
023600          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 IC2354.2
023700     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              IC2354.2
023800     MOVE SPACE TO CORRECT-X.                                     IC2354.2
023900     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         IC2354.2
024000     MOVE     SPACE TO RE-MARK.                                   IC2354.2
024100 HEAD-ROUTINE.                                                    IC2354.2
024200     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IC2354.2
024300     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IC2354.2
024400     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IC2354.2
024500     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IC2354.2
024600 COLUMN-NAMES-ROUTINE.                                            IC2354.2
024700     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IC2354.2
024800     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IC2354.2
024900     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        IC2354.2
025000 END-ROUTINE.                                                     IC2354.2
025100     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.IC2354.2
025200 END-RTN-EXIT.                                                    IC2354.2
025300     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IC2354.2
025400 END-ROUTINE-1.                                                   IC2354.2
025500      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      IC2354.2
025600      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               IC2354.2
025700      ADD PASS-COUNTER TO ERROR-HOLD.                             IC2354.2
025800*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   IC2354.2
025900      MOVE PASS-COUNTER TO CCVS-E-4-1.                            IC2354.2
026000      MOVE ERROR-HOLD TO CCVS-E-4-2.                              IC2354.2
026100      MOVE CCVS-E-4 TO CCVS-E-2-2.                                IC2354.2
026200      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           IC2354.2
026300  END-ROUTINE-12.                                                 IC2354.2
026400      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        IC2354.2
026500     IF       ERROR-COUNTER IS EQUAL TO ZERO                      IC2354.2
026600         MOVE "NO " TO ERROR-TOTAL                                IC2354.2
026700         ELSE                                                     IC2354.2
026800         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       IC2354.2
026900     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           IC2354.2
027000     PERFORM WRITE-LINE.                                          IC2354.2
027100 END-ROUTINE-13.                                                  IC2354.2
027200     IF DELETE-COUNTER IS EQUAL TO ZERO                           IC2354.2
027300         MOVE "NO " TO ERROR-TOTAL  ELSE                          IC2354.2
027400         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      IC2354.2
027500     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   IC2354.2
027600     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IC2354.2
027700      IF   INSPECT-COUNTER EQUAL TO ZERO                          IC2354.2
027800          MOVE "NO " TO ERROR-TOTAL                               IC2354.2
027900      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   IC2354.2
028000      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            IC2354.2
028100      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          IC2354.2
028200     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IC2354.2
028300 WRITE-LINE.                                                      IC2354.2
028400     ADD 1 TO RECORD-COUNT.                                       IC2354.2
028500*    IF RECORD-COUNT GREATER 50                                   IC2354.2
028600*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          IC2354.2
028700*        MOVE SPACE TO DUMMY-RECORD                               IC2354.2
028800*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  IC2354.2
028900*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             IC2354.2
029000*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     IC2354.2
029100*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          IC2354.2
029200*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          IC2354.2
029300*        MOVE ZERO TO RECORD-COUNT.                               IC2354.2
029400     PERFORM WRT-LN.                                              IC2354.2
029500 WRT-LN.                                                          IC2354.2
029600     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               IC2354.2
029700     MOVE SPACE TO DUMMY-RECORD.                                  IC2354.2
029800 BLANK-LINE-PRINT.                                                IC2354.2
029900     PERFORM WRT-LN.                                              IC2354.2
030000 FAIL-ROUTINE.                                                    IC2354.2
030100     IF   COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE. IC2354.2
030200     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.IC2354.2
030300     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IC2354.2
030400     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   IC2354.2
030500     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IC2354.2
030600     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IC2354.2
030700     GO TO  FAIL-ROUTINE-EX.                                      IC2354.2
030800 FAIL-ROUTINE-WRITE.                                              IC2354.2
030900     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         IC2354.2
031000     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 IC2354.2
031100     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. IC2354.2
031200     MOVE   SPACES TO COR-ANSI-REFERENCE.                         IC2354.2
031300 FAIL-ROUTINE-EX. EXIT.                                           IC2354.2
031400 BAIL-OUT.                                                        IC2354.2
031500     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   IC2354.2
031600     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           IC2354.2
031700 BAIL-OUT-WRITE.                                                  IC2354.2
031800     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  IC2354.2
031900     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IC2354.2
032000     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IC2354.2
032100     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IC2354.2
032200 BAIL-OUT-EX. EXIT.                                               IC2354.2
032300 CCVS1-EXIT.                                                      IC2354.2
032400     EXIT.                                                        IC2354.2
032500 SECT-IC235-0001 SECTION.                                         IC2354.2
032600*        THE TESTS IN THIS SECTION CALL A SUBPROGRAM WHICH        IC2354.2
032700*    HAS FOUR EXIT PROGRAM STATEMENTS.  A DIFFERENT EXIT IS       IC2354.2
032800*    TAKEN FOR EACH CALL TO THE SUBPROGRAM.                       IC2354.2
032900 EXIT-INIT.                                                       IC2354.2
033000     MOVE "MULTIPLE EXIT PROGRM" TO FEATURE.                      IC2354.2
033100 EXIT-INIT-001.                                                   IC2354.2
033200     MOVE 0 TO MAIN-DN2.                                          IC2354.2
033300     MOVE 1 TO MAIN-DN1.                                          IC2354.2
033400 EXIT-TEST-001.                                                   IC2354.2
033500     CALL "IC235A-2" USING MAIN-DN1 MAIN-DN2.                     IC2354.2
033600     IF MAIN-DN2 EQUAL TO 1                                       IC2354.2
033700         PERFORM PASS                                             IC2354.2
033800         GO TO EXIT-WRITE-001.                                    IC2354.2
033900 EXIT-FAIL-001.                                                   IC2354.2
034000     MOVE MAIN-DN1 TO CORRECT-18V0.                               IC2354.2
034100     MOVE MAIN-DN2 TO COMPUTED-18V0.                              IC2354.2
034200     MOVE "FIRST EXIT FROM SUBPROGRAM" TO RE-MARK.                IC2354.2
034300     PERFORM FAIL.                                                IC2354.2
034400 EXIT-WRITE-001.                                                  IC2354.2
034500     MOVE "EXIT-TEST-01" TO PAR-NAME.                             IC2354.2
034600     PERFORM PRINT-DETAIL.                                        IC2354.2
034700 EXIT-INIT-002.                                                   IC2354.2
034800     MOVE 0 TO MAIN-DN2.                                          IC2354.2
034900     MOVE 2 TO MAIN-DN1.                                          IC2354.2
035000 EXIT-TEST-002.                                                   IC2354.2
035100     CALL "IC235A-2" USING MAIN-DN1 MAIN-DN2.                     IC2354.2
035200     IF MAIN-DN2 EQUAL TO 2                                       IC2354.2
035300          PERFORM PASS                                            IC2354.2
035400          GO TO EXIT-WRITE-002.                                   IC2354.2
035500 EXIT-FAIL-002.                                                   IC2354.2
035600     MOVE MAIN-DN1 TO CORRECT-18V0.                               IC2354.2
035700     MOVE MAIN-DN2 TO COMPUTED-18V0.                              IC2354.2
035800     MOVE "SECOND EXIT FROM SUBPROGRAM" TO RE-MARK.               IC2354.2
035900     PERFORM FAIL.                                                IC2354.2
036000 EXIT-WRITE-002.                                                  IC2354.2
036100     MOVE "EXIT-TEST-02" TO PAR-NAME.                             IC2354.2
036200     PERFORM PRINT-DETAIL.                                        IC2354.2
036300 EXIT-INIT-003.                                                   IC2354.2
036400     MOVE 0 TO MAIN-DN2.                                          IC2354.2
036500     MOVE 3 TO MAIN-DN1.                                          IC2354.2
036600 EXIT-TEST-003.                                                   IC2354.2
036700     CALL "IC235A-2" USING MAIN-DN1 MAIN-DN2.                     IC2354.2
036800     IF MAIN-DN2 NOT EQUAL TO 3                                   IC2354.2
036900         GO TO EXIT-FAIL-003.                                     IC2354.2
037000     PERFORM PASS.                                                IC2354.2
037100     GO TO EXIT-WRITE-003.                                        IC2354.2
037200 EXIT-FAIL-003.                                                   IC2354.2
037300     MOVE MAIN-DN1 TO CORRECT-18V0.                               IC2354.2
037400     MOVE MAIN-DN2 TO COMPUTED-18V0.                              IC2354.2
037500     MOVE "THIRD EXIT FROM SUBPROGRAM" TO RE-MARK.                IC2354.2
037600     PERFORM FAIL.                                                IC2354.2
037700 EXIT-WRITE-003.                                                  IC2354.2
037800     MOVE "EXIT-TEST-03" TO PAR-NAME.                             IC2354.2
037900     PERFORM PRINT-DETAIL.                                        IC2354.2
038000 EXIT-INIT-004.                                                   IC2354.2
038100     MOVE 0 TO MAIN-DN2.                                          IC2354.2
038200     MOVE 4 TO MAIN-DN1.                                          IC2354.2
038300 EXIT-TEST-004.                                                   IC2354.2
038400     CALL "IC235A-2" USING MAIN-DN1 MAIN-DN2.                     IC2354.2
038500     IF MAIN-DN2 NOT EQUAL TO 4                                   IC2354.2
038600         GO TO EXIT-FAIL-004.                                     IC2354.2
038700     PERFORM PASS.                                                IC2354.2
038800     GO TO EXIT-WRITE-004.                                        IC2354.2
038900 EXIT-FAIL-004.                                                   IC2354.2
039000     MOVE MAIN-DN1 TO CORRECT-18V0.                               IC2354.2
039100     MOVE MAIN-DN2 TO COMPUTED-18V0.                              IC2354.2
039200     MOVE "FOURTH EXIT FROM SUBPROGRAM" TO RE-MARK.               IC2354.2
039300     PERFORM FAIL.                                                IC2354.2
039400 EXIT-WRITE-004.                                                  IC2354.2
039500     MOVE "EXIT-TEST-04" TO PAR-NAME.                             IC2354.2
039600     PERFORM PRINT-DETAIL.                                        IC2354.2
039700     GO TO SECT-IC235-0002.                                       IC2354.2
039800 EXIT-DELETES.                                                    IC2354.2
039900*        IF THE SUBPROGRAM WITH MULTIPLE EXIT PROGRAM             IC2354.2
040000*    STATEMENTS CANNOT BE INCLUDED IN THE RUN UNIT                IC2354.2
040100*    DELETE PARAGRAPH EXIT-INIT-001 THRU EXIT-WRITE-004.          IC2354.2
040200     PERFORM DE-LETE.                                             IC2354.2
040300     MOVE "EXIT-TEST-01" TO PAR-NAME.                             IC2354.2
040400     PERFORM PRINT-DETAIL.                                        IC2354.2
040500     PERFORM DE-LETE.                                             IC2354.2
040600     MOVE "EXIT-TEST-02" TO PAR-NAME.                             IC2354.2
040700     PERFORM PRINT-DETAIL.                                        IC2354.2
040800     PERFORM DE-LETE.                                             IC2354.2
040900     MOVE "EXIT-TEST-03" TO PAR-NAME.                             IC2354.2
041000     PERFORM PRINT-DETAIL.                                        IC2354.2
041100     PERFORM DE-LETE.                                             IC2354.2
041200     MOVE "EXIT-TEST-04" TO PAR-NAME.                             IC2354.2
041300     PERFORM PRINT-DETAIL.                                        IC2354.2
041400 SECT-IC235-0002 SECTION.                                         IC2354.2
041500*        THIS SECTION CALLS A SUBPROGRAM WITH TWO GROUP ITEMS     IC2354.2
041600*    AND ONE ELEMENTARY ITEM IN THE USING PHRASE. THE ITEM        IC2354.2
041700*    DESCRIPTIONS ARE DIFFERENT IN THE SUBPROGRAM FROM THE MAIN   IC2354.2
041800*    PROGRAM, BUT THE NUMBER OF CHARACTERS IS IDENTICAL.          IC2354.2
041900 CALL-INIT-06.                                                    IC2354.2
042000     MOVE "CALL-TEST-06" TO PAR-NAME.                             IC2354.2
042100     MOVE 0 TO NUMER-FIELD  ELEM-77 NUM-ITEM.                     IC2354.2
042200     MOVE SPACE TO ALPHA-NUM-FIELD ALPHA-FIELD ALPHA-EDITED.      IC2354.2
042300     MOVE  11    TO ELEM-NON-01.                                  IC2354.2
042400     MOVE  99    TO SUBSCRIPTED-DATA (4).                         IC2354.2
042500     MOVE "CALL USING DN SERIES" TO FEATURE.                      IC2354.2
042600 CALL-TEST-06.                                                    IC2354.2
042700     CALL "IC235A-1" USING GROUP-01 ELEM-77 GROUP-02              IC2354.2
042800                           ELEM-NON-01 SUBSCRIPTED-DATA (4).      IC2354.2
042900     GO TO CALL-TEST-06-01.                                       IC2354.2
043000 CALL-DELETE-06.                                                  IC2354.2
043100     PERFORM DE-LETE.                                             IC2354.2
043200     PERFORM PRINT-DETAIL.                                        IC2354.2
043300     GO TO CCVS-EXIT.                                             IC2354.2
043400*       IF IC235A-1 CANNOT BE INCLUDED IN THE RUN UNIT            IC2354.2
043500*    DELETE THE PARAGRAPH CALL-TEST-06.                           IC2354.2
043600 CALL-TEST-06-01.                                                 IC2354.2
043700     IF ALPHA-NUM-FIELD NOT EQUAL TO "IC235A-1"                   IC2354.2
043800         GO TO CALL-FAIL-06-01.                                   IC2354.2
043900     PERFORM PASS.                                                IC2354.2
044000     GO TO CALL-WRITE-06-01.                                      IC2354.2
044100 CALL-FAIL-06-01.                                                 IC2354.2
044200     MOVE ALPHA-NUM-FIELD TO COMPUTED-A.                          IC2354.2
044300     MOVE "IC235A-1" TO CORRECT-A.                                IC2354.2
044400     PERFORM FAIL.                                                IC2354.2
044500     MOVE "ALPHANUMERIC PARAMETER" TO RE-MARK.                    IC2354.2
044600 CALL-WRITE-06-01.                                                IC2354.2
044700     ADD 1 TO REC-CT.                                             IC2354.2
044800     PERFORM PRINT-DETAIL.                                        IC2354.2
044900 CALL-TEST-06-02.                                                 IC2354.2
045000     IF NUMER-FIELD EQUAL TO 25                                   IC2354.2
045100         PERFORM PASS                                             IC2354.2
045200         GO TO CALL-WRITE-06-02.                                  IC2354.2
045300 CALL-FAIL-06-02.                                                 IC2354.2
045400     PERFORM FAIL.                                                IC2354.2
045500     MOVE NUMER-FIELD TO COMPUTED-18V0.                           IC2354.2
045600     MOVE 25 TO CORRECT-18V0.                                     IC2354.2
045700     MOVE "NUMERIC DISPLAY PARAMETER" TO RE-MARK.                 IC2354.2
045800 CALL-WRITE-06-02.                                                IC2354.2
045900     ADD 1 TO REC-CT.                                             IC2354.2
046000     PERFORM PRINT-DETAIL.                                        IC2354.2
046100 CALL-TEST-06-03.                                                 IC2354.2
046200     IF ALPHA-FIELD EQUAL TO "YES"                                IC2354.2
046300         PERFORM PASS                                             IC2354.2
046400         GO TO CALL-WRITE-06-03.                                  IC2354.2
046500 CALL-FAIL-06-03.                                                 IC2354.2
046600     PERFORM FAIL.                                                IC2354.2
046700     MOVE ALPHA-FIELD TO COMPUTED-A.                              IC2354.2
046800     MOVE "YES" TO CORRECT-A.                                     IC2354.2
046900     MOVE "ALPHABETIC PARAMETER" TO RE-MARK.                      IC2354.2
047000 CALL-WRITE-06-03.                                                IC2354.2
047100     ADD 1 TO REC-CT.                                             IC2354.2
047200     PERFORM PRINT-DETAIL.                                        IC2354.2
047300 CALL-TEST-06-04.                                                 IC2354.2
047400     IF ELEM-77 EQUAL TO 0.7654                                   IC2354.2
047500         PERFORM PASS                                             IC2354.2
047600         GO TO CALL-WRITE-06-04.                                  IC2354.2
047700 CALL-FAIL-06-04.                                                 IC2354.2
047800     PERFORM FAIL.                                                IC2354.2
047900     MOVE ELEM-77 TO COMPUTED-4V14.                               IC2354.2
048000     MOVE 0.7654 TO CORRECT-4V14.                                 IC2354.2
048100     MOVE "COMPUTATIONAL PARAMETER" TO RE-MARK.                   IC2354.2
048200 CALL-WRITE-06-04.                                                IC2354.2
048300     ADD 1 TO REC-CT.                                             IC2354.2
048400     PERFORM PRINT-DETAIL.                                        IC2354.2
048500 CALL-TEST-06-05.                                                 IC2354.2
048600     IF NUM-ITEM EQUAL TO 25                                      IC2354.2
048700         PERFORM PASS                                             IC2354.2
048800         GO TO CALL-WRITE-06-05.                                  IC2354.2
048900 CALL-FAIL-06-05.                                                 IC2354.2
049000     PERFORM FAIL.                                                IC2354.2
049100     MOVE NUM-ITEM TO COMPUTED-18V0.                              IC2354.2
049200     MOVE 25 TO CORRECT-18V0.                                     IC2354.2
049300     MOVE "SIGNED NUMERIC PARAMETER" TO RE-MARK.                  IC2354.2
049400 CALL-WRITE-06-05.                                                IC2354.2
049500     ADD 1 TO REC-CT.                                             IC2354.2
049600     PERFORM PRINT-DETAIL.                                        IC2354.2
049700 CALL-TEST-06-06.                                                 IC2354.2
049800     IF ALPHA-EDITED EQUAL TO "AB C0D"                            IC2354.2
049900         PERFORM PASS                                             IC2354.2
050000         GO TO CALL-WRITE-06-06.                                  IC2354.2
050100 CALL-FAIL-06-06.                                                 IC2354.2
050200     PERFORM FAIL.                                                IC2354.2
050300     MOVE ALPHA-EDITED TO COMPUTED-A.                             IC2354.2
050400     MOVE "AB C0D" TO CORRECT-A.                                  IC2354.2
050500     MOVE "ALPHANUMERIC EDITED" TO RE-MARK.                       IC2354.2
050600 CALL-WRITE-06-06.                                                IC2354.2
050700     ADD 1 TO REC-CT.                                             IC2354.2
050800     PERFORM PRINT-DETAIL.                                        IC2354.2
050900 CALL-TEST-06-07.                                                 IC2354.2
051000     IF      ELEM-NON-01 = "ZZ"                                   IC2354.2
051100             PERFORM PASS                                         IC2354.2
051200             GO TO CALL-WRITE-06-07.                              IC2354.2
051300 CALL-FAIL-06-07.                                                 IC2354.2
051400     PERFORM FAIL.                                                IC2354.2
051500     MOVE    ELEM-NON-01 TO COMPUTED-A.                           IC2354.2
051600     MOVE   "ZZ"        TO CORRECT-A.                             IC2354.2
051700     MOVE   "ELEMENTARY NON LEVEL-01 DATA ITEM" TO RE-MARK.       IC2354.2
051800 CALL-WRITE-06-07.                                                IC2354.2
051900     MOVE   "X-27 5.2.3 SR3" TO ANSI-REFERENCE.                   IC2354.2
052000     ADD 1 TO REC-CT.                                             IC2354.2
052100     PERFORM PRINT-DETAIL.                                        IC2354.2
052200 CALL-TEST-06-08.                                                 IC2354.2
052300     IF      SUBSCRIPTED-DATA (4) = "1A"                          IC2354.2
052400             PERFORM PASS                                         IC2354.2
052500             GO TO CALL-WRITE-06-08.                              IC2354.2
052600 CALL-FAIL-06-08.                                                 IC2354.2
052700     PERFORM FAIL.                                                IC2354.2
052800     MOVE    SUBSCRIPTED-DATA (4) TO COMPUTED-A.                  IC2354.2
052900     MOVE   "1A"        TO CORRECT-A.                             IC2354.2
053000     MOVE   "SUBSCRIPTED LINKAGE DATA ITEM" TO RE-MARK.           IC2354.2
053100 CALL-WRITE-06-08.                                                IC2354.2
053200     MOVE   "XVII-46 (59)" TO ANSI-REFERENCE.                     IC2354.2
053300     ADD 1 TO REC-CT.                                             IC2354.2
053400     PERFORM PRINT-DETAIL.                                        IC2354.2
053500*                                                                 IC2354.2
053600     GO TO CCVS-EXIT.                                             IC2354.2
053700 CCVS-EXIT SECTION.                                               IC2354.2
053800 CCVS-999999.                                                     IC2354.2
053900     GO TO CLOSE-FILES.                                           IC2354.2
054000 IDENTIFICATION DIVISION.                                         IC2354.2
054100 PROGRAM-ID.                                                      IC2354.2
054200     IC235A-1.                                                    IC2354.2
054300****************************************************************  IC2354.2
054400*                                                              *  IC2354.2
054500*    VALIDATION FOR:-                                          *  IC2354.2
054600*                                                              *  IC2354.2
054700*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IC2354.2
054800*                                                              *  IC2354.2
054900*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IC2354.2
055000*                                                              *  IC2354.2
055100****************************************************************  IC2354.2
055200*                                                              *  IC2354.2
055300*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  IC2354.2
055400*                                                              *  IC2354.2
055500*        X-55  - SYSTEM PRINTER NAME.                          *  IC2354.2
055600*        X-82  - SOURCE COMPUTER NAME.                         *  IC2354.2
055700*        X-83  - OBJECT COMPUTER NAME.                         *  IC2354.2
055800*                                                              *  IC2354.2
055900****************************************************************  IC2354.2
056000*        THE SUBPROGRAM IC235A-1 HAS THREE OPERANDS IN THE        IC2354.2
056100*    USING PHRASE OF THE PROCEDURE DIVISION HEADER.  TWO          IC2354.2
056200*    OPERANDS ARE 01 GROUP ITEMS AND THE THIRD OPERAND IS         IC2354.2
056300*    AN ELEMENTARY 77 ITEM.  THE DATA DESCRIPTIONS OF THESE       IC2354.2
056400*    OPERANDS IN THE LINKAGE SECTION ARE NOT THE SAME AS THE      IC2354.2
056500*    DATA DESCRIPTIONS IN THE WORKING-STORAGE SECTION OF THE      IC2354.2
056600*    CALLING PROGRAM, BUT AN EQUAL NUMBER OF CHARACTER            IC2354.2
056700*    POSITIONS ARE DEFINED.  THE CALLING PROGRAM IS IC235.        IC2354.2
056800 ENVIRONMENT DIVISION.                                            IC2354.2
056900 INPUT-OUTPUT SECTION.                                            IC2354.2
057000 FILE-CONTROL.                                                    IC2354.2
057100     SELECT PRINT-FILE ASSIGN TO                                  IC2354.2
057200     XXXXX055.                                                    IC2354.2
057300 DATA DIVISION.                                                   IC2354.2
057400 FILE SECTION.                                                    IC2354.2
057500 FD  PRINT-FILE.                                                  IC2354.2
057600 01  PRINT-REC PICTURE X(120).                                    IC2354.2
057700 01  DUMMY-RECORD PICTURE X(120).                                 IC2354.2
057800 WORKING-STORAGE SECTION.                                         IC2354.2
057900 01  CONSTANT-VALUES.                                             IC2354.2
058000     02  AN-CONSTANT PIC X(8) VALUE "IC235A-1".                   IC2354.2
058100     02  NUM-CONSTANT PIC 99V9999 VALUE 0.7654.                   IC2354.2
058200 LINKAGE SECTION.                                                 IC2354.2
058300 01  GRP-01.                                                      IC2354.2
058400     02  AN-FIELD PICTURE X(8).                                   IC2354.2
058500     02  NUM-DISPLAY PIC 99.                                      IC2354.2
058600     02  GRP-LEVEL.                                               IC2354.2
058700         03  A-FIELD PICTURE A(3).                                IC2354.2
058800 77  ELEM-01 PIC  V9(4) COMPUTATIONAL.                            IC2354.2
058900 01  GRP-02.                                                      IC2354.2
059000     02  GRP-03.                                                  IC2354.2
059100         03  NUM-ITEM PICTURE S99.                                IC2354.2
059200         03  EDITED-FIELD  PIC XXBX0X.                            IC2354.2
059300 01  ELEM-NON-01           PIC XX.                                IC2354.2
059400 01  SUBSCRIPTED-DATA PIC XX.                                     IC2354.2
059500 PROCEDURE DIVISION USING GRP-01 ELEM-01 GRP-02                   IC2354.2
059600                          ELEM-NON-01 SUBSCRIPTED-DATA.           IC2354.2
059700 SECT-IC235A-1-001 SECTION.                                       IC2354.2
059800*        THIS SECTION SETS THE PARAMETER FIELDS REFERRED TO       IC2354.2
059900*    IN THE USING PHRASE AND DEFINED IN THE LINKAGE SECTION.      IC2354.2
060000 CALL-TEST-06.                                                    IC2354.2
060100     MOVE AN-CONSTANT TO AN-FIELD.                                IC2354.2
060200     ADD 25 TO NUM-DISPLAY.                                       IC2354.2
060300     MOVE "YES" TO A-FIELD.                                       IC2354.2
060400     MOVE NUM-CONSTANT TO ELEM-01.                                IC2354.2
060500     MOVE NUM-DISPLAY TO NUM-ITEM.                                IC2354.2
060600     MOVE "ABCD" TO EDITED-FIELD.                                 IC2354.2
060700     MOVE "ZZ"   TO ELEM-NON-01.                                  IC2354.2
060800     MOVE "1A"   TO SUBSCRIPTED-DATA.                             IC2354.2
060900 CALL-EXIT-06.                                                    IC2354.2
061000     EXIT PROGRAM.                                                IC2354.2
061100 END PROGRAM IC235A-1.                                            IC2354.2
061200 IDENTIFICATION DIVISION.                                         IC2354.2
061300 PROGRAM-ID.                                                      IC2354.2
061400     IC235A-2.                                                    IC2354.2
061500****************************************************************  IC2354.2
061600*                                                              *  IC2354.2
061700*    VALIDATION FOR:-                                          *  IC2354.2
061800*                                                              *  IC2354.2
061900*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IC2354.2
062000*                                                              *  IC2354.2
062100*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IC2354.2
062200*                                                              *  IC2354.2
062300****************************************************************  IC2354.2
062400*                                                              *  IC2354.2
062500*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  IC2354.2
062600*                                                              *  IC2354.2
062700*        X-55  - SYSTEM PRINTER NAME.                          *  IC2354.2
062800*        X-82  - SOURCE COMPUTER NAME.                         *  IC2354.2
062900*        X-83  - OBJECT COMPUTER NAME.                         *  IC2354.2
063000*                                                              *  IC2354.2
063100****************************************************************  IC2354.2
063200*        THE SUBPROGRAM IC235A-2 HAS TWO OPERANDS IN THE          IC2354.2
063300*    PROCEDURE DIVISION HEADER AND THE ROUTINE CONTAINS           IC2354.2
063400*    FOUR EXIT PROGRAM STATEMENTS.  THE CALLING PROGRAM           IC2354.2
063500*    IS IC235.                                                    IC2354.2
063600 ENVIRONMENT DIVISION.                                            IC2354.2
063700 DATA DIVISION.                                                   IC2354.2
063800 LINKAGE SECTION.                                                 IC2354.2
063900 77  DN1 PICTURE 999.                                             IC2354.2
064000 77  DN2 PICTURE S99 COMPUTATIONAL.                               IC2354.2
064100 PROCEDURE DIVISION  USING DN1 DN2.                               IC2354.2
064200*        THIS SUBPROGRAM CONTANS FOUR EXIT PROGRAM STATEMENTS.    IC2354.2
064300 SECT-IC235A-2-0001 SECTION.                                      IC2354.2
064400 EXIT-TEST-001.                                                   IC2354.2
064500     IF DN1 IS NOT EQUAL TO 1                                     IC2354.2
064600          GO TO EXIT-TEST-002.                                    IC2354.2
064700     MOVE 1 TO DN2.                                               IC2354.2
064800     EXIT PROGRAM.                                                IC2354.2
064900 EXIT-TEST-002.                                                   IC2354.2
065000     IF DN1 IS NOT EQUAL TO 2                                     IC2354.2
065100          GO TO EXIT-TEST-003.                                    IC2354.2
065200     MOVE 2 TO DN2.                                               IC2354.2
065300     EXIT PROGRAM.                                                IC2354.2
065400 EXIT-TEST-003.                                                   IC2354.2
065500     IF DN1 NOT EQUAL TO 3                                        IC2354.2
065600          GO TO EXIT-TEST-004.                                    IC2354.2
065700     MOVE 3 TO DN2.                                               IC2354.2
065800     EXIT PROGRAM.                                                IC2354.2
065900 EXIT-TEST-004.                                                   IC2354.2
066000     MOVE 4 TO DN2.                                               IC2354.2
066100     GO TO EXIT-STATEMENT-004.                                    IC2354.2
066200 EXTRANEOUS-PARAGRAPH.                                            IC2354.2
066300*        THIS PARAGRAPH IS NEVER EXECUTED.                        IC2354.2
066400     MOVE 5 TO DN2.                                               IC2354.2
066500 EXIT-STATEMENT-004.                                              IC2354.2
066600     EXIT PROGRAM.                                                IC2354.2
066700 END PROGRAM IC235A-2.                                            IC2354.2
066800 END PROGRAM IC235A.                                              IC2354.2
      *END-OF,IC235A                                                                  
