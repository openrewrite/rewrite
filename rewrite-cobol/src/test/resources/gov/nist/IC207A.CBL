      *HEADER,COBOL,IC207A                                                            
000100 IDENTIFICATION DIVISION.                                         IC2074.2
000200 PROGRAM-ID.                                                      IC2074.2
000300     IC207A.                                                      IC2074.2
000400****************************************************************  IC2074.2
000500*                                                              *  IC2074.2
000600*    VALIDATION FOR:-                                          *  IC2074.2
000700*                                                              *  IC2074.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IC2074.2
000900*                                                              *  IC2074.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IC2074.2
001100*                                                              *  IC2074.2
001200****************************************************************  IC2074.2
001300*                                                              *  IC2074.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  IC2074.2
001500*                                                              *  IC2074.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  IC2074.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  IC2074.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  IC2074.2
001900*                                                              *  IC2074.2
002000****************************************************************  IC2074.2
002100*        THE PROGRAM IC207 DEFINES A VARIABLE LENGTH TABLE.       IC2074.2
002200*    THE TABLE AND THE VARIABLE CONTAINING THE TABLE LENGTH       IC2074.2
002300*    ARE OPERANDS IN A CALL STATEMENT USING PHRASE.  ALSO AN      IC2074.2
002400*    INDEX IS DEFINED FOR THE TABLE AND AN INDEX DATA ITEM        IC2074.2
002500*    IS USED TO PASS AN INDEX VALUE FOR A TABLE REFERENCE         IC2074.2
002600*    TO AND FROM THE SUBPROGRAM IC208.                            IC2074.2
002700****************************************************************  IC2074.2
002800 ENVIRONMENT DIVISION.                                            IC2074.2
002900 CONFIGURATION SECTION.                                           IC2074.2
003000 SOURCE-COMPUTER.                                                 IC2074.2
003100     XXXXX082.                                                    IC2074.2
003200 OBJECT-COMPUTER.                                                 IC2074.2
003300     XXXXX083.                                                    IC2074.2
003400 INPUT-OUTPUT SECTION.                                            IC2074.2
003500 FILE-CONTROL.                                                    IC2074.2
003600     SELECT PRINT-FILE ASSIGN TO                                  IC2074.2
003700     XXXXX055.                                                    IC2074.2
003800 DATA DIVISION.                                                   IC2074.2
003900 FILE SECTION.                                                    IC2074.2
004000 FD  PRINT-FILE.                                                  IC2074.2
004100 01  PRINT-REC PICTURE X(120).                                    IC2074.2
004200 01  DUMMY-RECORD PICTURE X(120).                                 IC2074.2
004300 WORKING-STORAGE SECTION.                                         IC2074.2
004400 77  INDEX-1 USAGE IS INDEX.                                      IC2074.2
004500 77  DN3 PICTURE 99  VALUE 15.                                    IC2074.2
004600 77  ID1 PICTURE X(6) VALUE "IC208A".                             IC2074.2
004700 77  DN4 PICTURE X.                                               IC2074.2
004800 77  DN5 PICTURE X(15).                                           IC2074.2
004900 01  TABLE-01.                                                    IC2074.2
005000     02  DN1 PICTURE X                                            IC2074.2
005100             OCCURS 1 TO 15 TIMES                                 IC2074.2
005200             DEPENDING ON DN3                                     IC2074.2
005300             INDEXED BY IN1.                                      IC2074.2
005400 01  TABLE-02.                                                    IC2074.2
005500     02  DN2   PICTURE X     OCCURS 8 TIMES.                      IC2074.2
005600 01  TEST-RESULTS.                                                IC2074.2
005700     02 FILLER                   PIC X      VALUE SPACE.          IC2074.2
005800     02 FEATURE                  PIC X(20)  VALUE SPACE.          IC2074.2
005900     02 FILLER                   PIC X      VALUE SPACE.          IC2074.2
006000     02 P-OR-F                   PIC X(5)   VALUE SPACE.          IC2074.2
006100     02 FILLER                   PIC X      VALUE SPACE.          IC2074.2
006200     02  PAR-NAME.                                                IC2074.2
006300       03 FILLER                 PIC X(19)  VALUE SPACE.          IC2074.2
006400       03  PARDOT-X              PIC X      VALUE SPACE.          IC2074.2
006500       03 DOTVALUE               PIC 99     VALUE ZERO.           IC2074.2
006600     02 FILLER                   PIC X(8)   VALUE SPACE.          IC2074.2
006700     02 RE-MARK                  PIC X(61).                       IC2074.2
006800 01  TEST-COMPUTED.                                               IC2074.2
006900     02 FILLER                   PIC X(30)  VALUE SPACE.          IC2074.2
007000     02 FILLER                   PIC X(17)  VALUE                 IC2074.2
007100            "       COMPUTED=".                                   IC2074.2
007200     02 COMPUTED-X.                                               IC2074.2
007300     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          IC2074.2
007400     03 COMPUTED-N               REDEFINES COMPUTED-A             IC2074.2
007500                                 PIC -9(9).9(9).                  IC2074.2
007600     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         IC2074.2
007700     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     IC2074.2
007800     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     IC2074.2
007900     03       CM-18V0 REDEFINES COMPUTED-A.                       IC2074.2
008000         04 COMPUTED-18V0                    PIC -9(18).          IC2074.2
008100         04 FILLER                           PIC X.               IC2074.2
008200     03 FILLER PIC X(50) VALUE SPACE.                             IC2074.2
008300 01  TEST-CORRECT.                                                IC2074.2
008400     02 FILLER PIC X(30) VALUE SPACE.                             IC2074.2
008500     02 FILLER PIC X(17) VALUE "       CORRECT =".                IC2074.2
008600     02 CORRECT-X.                                                IC2074.2
008700     03 CORRECT-A                  PIC X(20) VALUE SPACE.         IC2074.2
008800     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      IC2074.2
008900     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         IC2074.2
009000     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     IC2074.2
009100     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     IC2074.2
009200     03      CR-18V0 REDEFINES CORRECT-A.                         IC2074.2
009300         04 CORRECT-18V0                     PIC -9(18).          IC2074.2
009400         04 FILLER                           PIC X.               IC2074.2
009500     03 FILLER PIC X(2) VALUE SPACE.                              IC2074.2
009600     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     IC2074.2
009700 01  CCVS-C-1.                                                    IC2074.2
009800     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAIC2074.2
009900-    "SS  PARAGRAPH-NAME                                          IC2074.2
010000-    "       REMARKS".                                            IC2074.2
010100     02 FILLER                     PIC X(20)    VALUE SPACE.      IC2074.2
010200 01  CCVS-C-2.                                                    IC2074.2
010300     02 FILLER                     PIC X        VALUE SPACE.      IC2074.2
010400     02 FILLER                     PIC X(6)     VALUE "TESTED".   IC2074.2
010500     02 FILLER                     PIC X(15)    VALUE SPACE.      IC2074.2
010600     02 FILLER                     PIC X(4)     VALUE "FAIL".     IC2074.2
010700     02 FILLER                     PIC X(94)    VALUE SPACE.      IC2074.2
010800 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       IC2074.2
010900 01  REC-CT                        PIC 99       VALUE ZERO.       IC2074.2
011000 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       IC2074.2
011100 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       IC2074.2
011200 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       IC2074.2
011300 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       IC2074.2
011400 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       IC2074.2
011500 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       IC2074.2
011600 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      IC2074.2
011700 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       IC2074.2
011800 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     IC2074.2
011900 01  CCVS-H-1.                                                    IC2074.2
012000     02  FILLER                    PIC X(39)    VALUE SPACES.     IC2074.2
012100     02  FILLER                    PIC X(42)    VALUE             IC2074.2
012200     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 IC2074.2
012300     02  FILLER                    PIC X(39)    VALUE SPACES.     IC2074.2
012400 01  CCVS-H-2A.                                                   IC2074.2
012500   02  FILLER                        PIC X(40)  VALUE SPACE.      IC2074.2
012600   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  IC2074.2
012700   02  FILLER                        PIC XXXX   VALUE             IC2074.2
012800     "4.2 ".                                                      IC2074.2
012900   02  FILLER                        PIC X(28)  VALUE             IC2074.2
013000            " COPY - NOT FOR DISTRIBUTION".                       IC2074.2
013100   02  FILLER                        PIC X(41)  VALUE SPACE.      IC2074.2
013200                                                                  IC2074.2
013300 01  CCVS-H-2B.                                                   IC2074.2
013400   02  FILLER                        PIC X(15)  VALUE             IC2074.2
013500            "TEST RESULT OF ".                                    IC2074.2
013600   02  TEST-ID                       PIC X(9).                    IC2074.2
013700   02  FILLER                        PIC X(4)   VALUE             IC2074.2
013800            " IN ".                                               IC2074.2
013900   02  FILLER                        PIC X(12)  VALUE             IC2074.2
014000     " HIGH       ".                                              IC2074.2
014100   02  FILLER                        PIC X(22)  VALUE             IC2074.2
014200            " LEVEL VALIDATION FOR ".                             IC2074.2
014300   02  FILLER                        PIC X(58)  VALUE             IC2074.2
014400     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IC2074.2
014500 01  CCVS-H-3.                                                    IC2074.2
014600     02  FILLER                      PIC X(34)  VALUE             IC2074.2
014700            " FOR OFFICIAL USE ONLY    ".                         IC2074.2
014800     02  FILLER                      PIC X(58)  VALUE             IC2074.2
014900     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IC2074.2
015000     02  FILLER                      PIC X(28)  VALUE             IC2074.2
015100            "  COPYRIGHT   1985 ".                                IC2074.2
015200 01  CCVS-E-1.                                                    IC2074.2
015300     02 FILLER                       PIC X(52)  VALUE SPACE.      IC2074.2
015400     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              IC2074.2
015500     02 ID-AGAIN                     PIC X(9).                    IC2074.2
015600     02 FILLER                       PIC X(45)  VALUE SPACES.     IC2074.2
015700 01  CCVS-E-2.                                                    IC2074.2
015800     02  FILLER                      PIC X(31)  VALUE SPACE.      IC2074.2
015900     02  FILLER                      PIC X(21)  VALUE SPACE.      IC2074.2
016000     02 CCVS-E-2-2.                                               IC2074.2
016100         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      IC2074.2
016200         03 FILLER                   PIC X      VALUE SPACE.      IC2074.2
016300         03 ENDER-DESC               PIC X(44)  VALUE             IC2074.2
016400            "ERRORS ENCOUNTERED".                                 IC2074.2
016500 01  CCVS-E-3.                                                    IC2074.2
016600     02  FILLER                      PIC X(22)  VALUE             IC2074.2
016700            " FOR OFFICIAL USE ONLY".                             IC2074.2
016800     02  FILLER                      PIC X(12)  VALUE SPACE.      IC2074.2
016900     02  FILLER                      PIC X(58)  VALUE             IC2074.2
017000     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IC2074.2
017100     02  FILLER                      PIC X(13)  VALUE SPACE.      IC2074.2
017200     02 FILLER                       PIC X(15)  VALUE             IC2074.2
017300             " COPYRIGHT 1985".                                   IC2074.2
017400 01  CCVS-E-4.                                                    IC2074.2
017500     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      IC2074.2
017600     02 FILLER                       PIC X(4)   VALUE " OF ".     IC2074.2
017700     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      IC2074.2
017800     02 FILLER                       PIC X(40)  VALUE             IC2074.2
017900      "  TESTS WERE EXECUTED SUCCESSFULLY".                       IC2074.2
018000 01  XXINFO.                                                      IC2074.2
018100     02 FILLER                       PIC X(19)  VALUE             IC2074.2
018200            "*** INFORMATION ***".                                IC2074.2
018300     02 INFO-TEXT.                                                IC2074.2
018400       04 FILLER                     PIC X(8)   VALUE SPACE.      IC2074.2
018500       04 XXCOMPUTED                 PIC X(20).                   IC2074.2
018600       04 FILLER                     PIC X(5)   VALUE SPACE.      IC2074.2
018700       04 XXCORRECT                  PIC X(20).                   IC2074.2
018800     02 INF-ANSI-REFERENCE           PIC X(48).                   IC2074.2
018900 01  HYPHEN-LINE.                                                 IC2074.2
019000     02 FILLER  PIC IS X VALUE IS SPACE.                          IC2074.2
019100     02 FILLER  PIC IS X(65)    VALUE IS "************************IC2074.2
019200-    "*****************************************".                 IC2074.2
019300     02 FILLER  PIC IS X(54)    VALUE IS "************************IC2074.2
019400-    "******************************".                            IC2074.2
019500 01  CCVS-PGM-ID                     PIC X(9)   VALUE             IC2074.2
019600     "IC207A".                                                    IC2074.2
019700 PROCEDURE DIVISION.                                              IC2074.2
019800 CCVS1 SECTION.                                                   IC2074.2
019900 OPEN-FILES.                                                      IC2074.2
020000     OPEN     OUTPUT PRINT-FILE.                                  IC2074.2
020100     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   IC2074.2
020200     MOVE    SPACE TO TEST-RESULTS.                               IC2074.2
020300     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             IC2074.2
020400     GO TO CCVS1-EXIT.                                            IC2074.2
020500 CLOSE-FILES.                                                     IC2074.2
020600     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   IC2074.2
020700 TERMINATE-CCVS.                                                  IC2074.2
020800*    EXIT PROGRAM.                                                IC2074.2
020900*TERMINATE-CALL.                                                  IC2074.2
021000     STOP     RUN.                                                IC2074.2
021100 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         IC2074.2
021200 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           IC2074.2
021300 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          IC2074.2
021400 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      IC2074.2
021500     MOVE "****TEST DELETED****" TO RE-MARK.                      IC2074.2
021600 PRINT-DETAIL.                                                    IC2074.2
021700     IF REC-CT NOT EQUAL TO ZERO                                  IC2074.2
021800             MOVE "." TO PARDOT-X                                 IC2074.2
021900             MOVE REC-CT TO DOTVALUE.                             IC2074.2
022000     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      IC2074.2
022100     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               IC2074.2
022200        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 IC2074.2
022300          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 IC2074.2
022400     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              IC2074.2
022500     MOVE SPACE TO CORRECT-X.                                     IC2074.2
022600     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         IC2074.2
022700     MOVE     SPACE TO RE-MARK.                                   IC2074.2
022800 HEAD-ROUTINE.                                                    IC2074.2
022900     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IC2074.2
023000     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IC2074.2
023100     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IC2074.2
023200     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IC2074.2
023300 COLUMN-NAMES-ROUTINE.                                            IC2074.2
023400     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IC2074.2
023500     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IC2074.2
023600     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        IC2074.2
023700 END-ROUTINE.                                                     IC2074.2
023800     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.IC2074.2
023900 END-RTN-EXIT.                                                    IC2074.2
024000     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IC2074.2
024100 END-ROUTINE-1.                                                   IC2074.2
024200      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      IC2074.2
024300      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               IC2074.2
024400      ADD PASS-COUNTER TO ERROR-HOLD.                             IC2074.2
024500*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   IC2074.2
024600      MOVE PASS-COUNTER TO CCVS-E-4-1.                            IC2074.2
024700      MOVE ERROR-HOLD TO CCVS-E-4-2.                              IC2074.2
024800      MOVE CCVS-E-4 TO CCVS-E-2-2.                                IC2074.2
024900      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           IC2074.2
025000  END-ROUTINE-12.                                                 IC2074.2
025100      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        IC2074.2
025200     IF       ERROR-COUNTER IS EQUAL TO ZERO                      IC2074.2
025300         MOVE "NO " TO ERROR-TOTAL                                IC2074.2
025400         ELSE                                                     IC2074.2
025500         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       IC2074.2
025600     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           IC2074.2
025700     PERFORM WRITE-LINE.                                          IC2074.2
025800 END-ROUTINE-13.                                                  IC2074.2
025900     IF DELETE-COUNTER IS EQUAL TO ZERO                           IC2074.2
026000         MOVE "NO " TO ERROR-TOTAL  ELSE                          IC2074.2
026100         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      IC2074.2
026200     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   IC2074.2
026300     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IC2074.2
026400      IF   INSPECT-COUNTER EQUAL TO ZERO                          IC2074.2
026500          MOVE "NO " TO ERROR-TOTAL                               IC2074.2
026600      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   IC2074.2
026700      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            IC2074.2
026800      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          IC2074.2
026900     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IC2074.2
027000 WRITE-LINE.                                                      IC2074.2
027100     ADD 1 TO RECORD-COUNT.                                       IC2074.2
027200*    IF RECORD-COUNT GREATER 50                                   IC2074.2
027300*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          IC2074.2
027400*        MOVE SPACE TO DUMMY-RECORD                               IC2074.2
027500*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  IC2074.2
027600*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             IC2074.2
027700*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     IC2074.2
027800*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          IC2074.2
027900*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          IC2074.2
028000*        MOVE ZERO TO RECORD-COUNT.                               IC2074.2
028100     PERFORM WRT-LN.                                              IC2074.2
028200 WRT-LN.                                                          IC2074.2
028300     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               IC2074.2
028400     MOVE SPACE TO DUMMY-RECORD.                                  IC2074.2
028500 BLANK-LINE-PRINT.                                                IC2074.2
028600     PERFORM WRT-LN.                                              IC2074.2
028700 FAIL-ROUTINE.                                                    IC2074.2
028800     IF   COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE. IC2074.2
028900     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.IC2074.2
029000     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IC2074.2
029100     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   IC2074.2
029200     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IC2074.2
029300     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IC2074.2
029400     GO TO  FAIL-ROUTINE-EX.                                      IC2074.2
029500 FAIL-ROUTINE-WRITE.                                              IC2074.2
029600     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         IC2074.2
029700     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 IC2074.2
029800     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. IC2074.2
029900     MOVE   SPACES TO COR-ANSI-REFERENCE.                         IC2074.2
030000 FAIL-ROUTINE-EX. EXIT.                                           IC2074.2
030100 BAIL-OUT.                                                        IC2074.2
030200     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   IC2074.2
030300     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           IC2074.2
030400 BAIL-OUT-WRITE.                                                  IC2074.2
030500     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  IC2074.2
030600     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IC2074.2
030700     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IC2074.2
030800     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IC2074.2
030900 BAIL-OUT-EX. EXIT.                                               IC2074.2
031000 CCVS1-EXIT.                                                      IC2074.2
031100     EXIT.                                                        IC2074.2
031200 SECT-IC207-0001 SECTION.                                         IC2074.2
031300 INIT-PARAGRAPH.                                                  IC2074.2
031400     MOVE "ABCDEFGHIJKLMNO" TO TABLE-01.                          IC2074.2
031500     SET IN1 TO 3.                                                IC2074.2
031600     SET INDEX-1 TO IN1.                                          IC2074.2
031700     MOVE 3 TO DN3.                                               IC2074.2
031800     MOVE SPACE TO TABLE-02.                                      IC2074.2
031900     CALL ID1 USING TABLE-01, TABLE-02, INDEX-1, DN3.             IC2074.2
032000 LINK-TEST-01.                                                    IC2074.2
032100*            THIS TEST CHECKS THAT AN INDEX DATA ITEM WAS         IC2074.2
032200*        CORRECTLY PASSED TO A SUBPROGRAM.                        IC2074.2
032300     MOVE "LINK-TEST-01" TO PAR-NAME.                             IC2074.2
032400     MOVE "INDEX DATA ITEM" TO FEATURE.                           IC2074.2
032500     IF DN2 (1) IS EQUAL TO "C"                                   IC2074.2
032600         PERFORM PASS                                             IC2074.2
032700         GO TO LINK-WRITE-01.                                     IC2074.2
032800 LINK-FAIL-01.                                                    IC2074.2
032900     PERFORM FAIL.                                                IC2074.2
033000     MOVE DN2 (1) TO COMPUTED-A.                                  IC2074.2
033100     MOVE "C" TO CORRECT-A.                                       IC2074.2
033200     MOVE "VALUE OF DN2(1)" TO RE-MARK.                           IC2074.2
033300 LINK-WRITE-01.                                                   IC2074.2
033400     PERFORM PRINT-DETAIL.                                        IC2074.2
033500 LINK-TEST-02.                                                    IC2074.2
033600*        THIS TEST VERIFIES THAT THE VARIABLE LENGTH TABLE        IC2074.2
033700*    AND ITS LENGTH WERE PROCESSED CORRECTLY IN THE SUBPROGRAM.   IC2074.2
033800     MOVE 1 TO REC-CT.                                            IC2074.2
033900     MOVE "LINK-TEST-02" TO PAR-NAME.                             IC2074.2
034000     MOVE "VAR. LENGTH TABLE" TO FEATURE.                         IC2074.2
034100 LINK-TEST-02-01.                                                 IC2074.2
034200     IF DN2 (2) EQUAL TO "Z"                                      IC2074.2
034300         PERFORM PASS                                             IC2074.2
034400         GO TO LINK-WRITE-02-01.                                  IC2074.2
034500 LINK-FAIL-02-01.                                                 IC2074.2
034600     PERFORM FAIL.                                                IC2074.2
034700     MOVE DN2 (2) TO COMPUTED-A.                                  IC2074.2
034800     MOVE "Z" TO CORRECT-A.                                       IC2074.2
034900     MOVE "VALUE OF DN2(2)" TO RE-MARK.                           IC2074.2
035000 LINK-WRITE-02-01.                                                IC2074.2
035100     PERFORM PRINT-DETAIL.                                        IC2074.2
035200 LINK-TEST-02-02.                                                 IC2074.2
035300     ADD 1 TO REC-CT.                                             IC2074.2
035400     IF DN2 (3) EQUAL TO "B"                                      IC2074.2
035500         PERFORM PASS                                             IC2074.2
035600         GO TO LINK-WRITE-02-02.                                  IC2074.2
035700 LINK-FAIL-02-02.                                                 IC2074.2
035800     PERFORM FAIL.                                                IC2074.2
035900     MOVE DN2 (3) TO COMPUTED-A.                                  IC2074.2
036000     MOVE "B" TO CORRECT-A.                                       IC2074.2
036100     MOVE "VALUE OF DN2(3)" TO RE-MARK.                           IC2074.2
036200 LINK-WRITE-02-02.                                                IC2074.2
036300     PERFORM PRINT-DETAIL.                                        IC2074.2
036400 LINK-TEST-02-03.                                                 IC2074.2
036500     ADD 1 TO REC-CT.                                             IC2074.2
036600     IF DN2 (4) EQUAL TO "X"                                      IC2074.2
036700         PERFORM PASS                                             IC2074.2
036800         GO TO LINK-WRITE-02-03.                                  IC2074.2
036900 LINK-FAIL-02-03.                                                 IC2074.2
037000     PERFORM FAIL.                                                IC2074.2
037100     MOVE DN2 (4) TO COMPUTED-A.                                  IC2074.2
037200     MOVE "X" TO CORRECT-A.                                       IC2074.2
037300     MOVE "VALUE OF DN2(4)" TO RE-MARK.                           IC2074.2
037400 LINK-WRITE-02-03.                                                IC2074.2
037500     PERFORM PRINT-DETAIL.                                        IC2074.2
037600 LINK-TEST-02-04.                                                 IC2074.2
037700     ADD 1 TO REC-CT.                                             IC2074.2
037800     IF DN2 (5) EQUAL TO "G"                                      IC2074.2
037900         PERFORM PASS                                             IC2074.2
038000         GO TO LINK-WRITE-02-04.                                  IC2074.2
038100 LINK-FAIL-02-04.                                                 IC2074.2
038200     PERFORM FAIL.                                                IC2074.2
038300     MOVE DN2 (5) TO COMPUTED-A.                                  IC2074.2
038400     MOVE "G" TO CORRECT-A.                                       IC2074.2
038500     MOVE "VALUE OF DN2(5)" TO RE-MARK.                           IC2074.2
038600 LINK-WRITE-02-04.                                                IC2074.2
038700     PERFORM PRINT-DETAIL.                                        IC2074.2
038800 LINK-TEST-03.                                                    IC2074.2
038900*        THIS TEST VERIFIES THAT THE CONDITION NAMES DEFINED      IC2074.2
039000*    IN THE LINKAGE SECTION OF THE SUBPROGRAM WERE PROCESSED      IC2074.2
039100*    CORRECTLY.                                                   IC2074.2
039200     MOVE "LINK-TEST-03" TO PAR-NAME.                             IC2074.2
039300     MOVE 1 TO REC-CT.                                            IC2074.2
039400     MOVE "CONDITION NAME" TO FEATURE.                            IC2074.2
039500 LINK-TEST-03-01.                                                 IC2074.2
039600     IF DN2 (6) EQUAL TO "A"                                      IC2074.2
039700         PERFORM PASS                                             IC2074.2
039800         GO TO LINK-WRITE-03-01.                                  IC2074.2
039900 LINK-FAIL-03-01.                                                 IC2074.2
040000     PERFORM FAIL.                                                IC2074.2
040100     MOVE DN2 (6) TO COMPUTED-A.                                  IC2074.2
040200     MOVE "A" TO CORRECT-A.                                       IC2074.2
040300     MOVE "VALUE OF DN2(6)" TO RE-MARK.                           IC2074.2
040400 LINK-WRITE-03-01.                                                IC2074.2
040500     PERFORM PRINT-DETAIL.                                        IC2074.2
040600 LINK-TEST-03-02.                                                 IC2074.2
040700     ADD 1 TO REC-CT.                                             IC2074.2
040800     IF DN2 (7) EQUAL TO "V"                                      IC2074.2
040900         PERFORM PASS                                             IC2074.2
041000         GO TO LINK-WRITE-03-02.                                  IC2074.2
041100 LINK-FAIL-03-02.                                                 IC2074.2
041200     PERFORM FAIL.                                                IC2074.2
041300     MOVE DN2 (7) TO COMPUTED-A.                                  IC2074.2
041400     MOVE "V" TO CORRECT-A.                                       IC2074.2
041500     MOVE "VALUE OF DN2(7)" TO RE-MARK.                           IC2074.2
041600 LINK-WRITE-03-02.                                                IC2074.2
041700     PERFORM PRINT-DETAIL.                                        IC2074.2
041800 LINK-TEST-03-03.                                                 IC2074.2
041900     ADD 1 TO REC-CT.                                             IC2074.2
042000     IF DN2 (8) EQUAL TO "H"                                      IC2074.2
042100         PERFORM PASS                                             IC2074.2
042200         GO TO LINK-WRITE-03-03.                                  IC2074.2
042300 LINK-FAIL-03-03.                                                 IC2074.2
042400     PERFORM FAIL.                                                IC2074.2
042500     MOVE DN2 (8) TO COMPUTED-A.                                  IC2074.2
042600     MOVE "H" TO CORRECT-A.                                       IC2074.2
042700     MOVE "VALUE OF DN2(8)" TO RE-MARK.                           IC2074.2
042800 LINK-WRITE-03-03.                                                IC2074.2
042900     PERFORM PRINT-DETAIL.                                        IC2074.2
043000 LINK-TEST-04.                                                    IC2074.2
043100     MOVE "LINK-TEST-04" TO PAR-NAME.                             IC2074.2
043200     MOVE "CALL PARAMETERS" TO FEATURE.                           IC2074.2
043300     MOVE 1 TO REC-CT.                                            IC2074.2
043400*        CHECK THE INDEX DATA ITEM AND TABLE LENGTH WHICH         IC2074.2
043500*    WERE SET IN THE SUBPROGRAM AND RETURNED CORRECTLY            IC2074.2
043600*    TO THE CALLING PROGRAM.                                      IC2074.2
043700 LINK-TEST-04-01.                                                 IC2074.2
043800     SET IN1 TO INDEX-1.                                          IC2074.2
043900     MOVE DN1 (IN1) TO DN4.                                       IC2074.2
044000     IF DN4 EQUAL TO "B"                                          IC2074.2
044100         PERFORM PASS                                             IC2074.2
044200         GO TO LINK-WRITE-04-01.                                  IC2074.2
044300 LINK-FAIL-04-01.                                                 IC2074.2
044400     PERFORM FAIL.                                                IC2074.2
044500     MOVE DN4 TO COMPUTED-A.                                      IC2074.2
044600     MOVE "B" TO CORRECT-A.                                       IC2074.2
044700     MOVE "VALUE OF DN1(IN1)" TO RE-MARK.                         IC2074.2
044800 LINK-WRITE-04-01.                                                IC2074.2
044900     PERFORM PRINT-DETAIL.                                        IC2074.2
045000 LINK-TEST-04-02.                                                 IC2074.2
045100     SET IN1 TO 1.                                                IC2074.2
045200     SEARCH DN1 VARYING IN1                                       IC2074.2
045300         AT END PERFORM PASS                                      IC2074.2
045400                GO TO LINK-WRITE-04-02,                           IC2074.2
045500         WHEN DN1 (IN1) EQUAL TO "J"                              IC2074.2
045600             PERFORM FAIL                                         IC2074.2
045700             MOVE DN1 (IN1) TO COMPUTED-A                         IC2074.2
045800             MOVE "MATCH SHOULD NOT BE FOUND" TO RE-MARK.         IC2074.2
045900 LINK-WRITE-04-02.                                                IC2074.2
046000     ADD 1 TO REC-CT.                                             IC2074.2
046100     PERFORM PRINT-DETAIL.                                        IC2074.2
046200 LINK-TEST-04-03.                                                 IC2074.2
046300     ADD 1 TO REC-CT.                                             IC2074.2
046400     MOVE TABLE-01 TO DN5.                                        IC2074.2
046500     IF DN5 EQUAL TO "ABCDEFGHI      "                            IC2074.2
046600         PERFORM PASS                                             IC2074.2
046700         GO TO LINK-WRITE-04-03.                                  IC2074.2
046800 LINK-FAIL-04-03.                                                 IC2074.2
046900     PERFORM FAIL.                                                IC2074.2
047000     MOVE DN5 TO COMPUTED-A.                                      IC2074.2
047100     MOVE "ABCDEFGHI      " TO CORRECT-A.                         IC2074.2
047200     MOVE "CONTENTS OF TABLE-01" TO RE-MARK.                      IC2074.2
047300 LINK-WRITE-04-03.                                                IC2074.2
047400     PERFORM PRINT-DETAIL.                                        IC2074.2
047500 EXIT-IC207.                                                      IC2074.2
047600     GO TO CCVS-EXIT.                                             IC2074.2
047700 CCVS-EXIT SECTION.                                               IC2074.2
047800 CCVS-999999.                                                     IC2074.2
047900     GO TO CLOSE-FILES.                                           IC2074.2
      *END-OF,IC207A                                                                  
