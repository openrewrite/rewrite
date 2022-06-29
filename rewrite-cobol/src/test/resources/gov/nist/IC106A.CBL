      *HEADER,COBOL,IC106A                                                            
000100 IDENTIFICATION DIVISION.                                         IC1064.2
000200 PROGRAM-ID.                                                      IC1064.2
000300     IC106A.                                                      IC1064.2
000400****************************************************************  IC1064.2
000500*                                                              *  IC1064.2
000600*    VALIDATION FOR:-                                          *  IC1064.2
000700*                                                              *  IC1064.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IC1064.2
000900*                                                              *  IC1064.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IC1064.2
001100*                                                              *  IC1064.2
001200****************************************************************  IC1064.2
001300*                                                              *  IC1064.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  IC1064.2
001500*                                                              *  IC1064.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  IC1064.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  IC1064.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  IC1064.2
001900*                                                              *  IC1064.2
002000****************************************************************  IC1064.2
002100*        THIS PROGRAM CALLS A SUBPROGRAM WITH TWO TABLES          IC1064.2
002200*    AND AN INDEX DATA ITEM REFERENCED IN THE USING PHRASE        IC1064.2
002300*    OF THE CALL STATEMENT.  BOTH OF THE TABLES CONTAIN AN        IC1064.2
002400*    INDEXED BY CLAUSE.                                           IC1064.2
002500*        THE TESTS IN THIS PROGRAM VERIFY THAT                    IC1064.2
002600*           (1)  THE INDICES IN THE MAIN PROGRAM AND THE          IC1064.2
002700*                SUBPROGRAM ARE SEPARATE,                         IC1064.2
002800*           (2)  AN INDEX DATA ITEM SET IN A MAIN PROGRAM         IC1064.2
002900*                CAN BE USED TO SET AN INDEX IN A SUBPROGRAM,     IC1064.2
003000*           (3)  TABLES CAN BE SHARED BETWEEN A MAIN PROGRAM      IC1064.2
003100*                AND A SUBPROGRAM.                                IC1064.2
003200*        THE SUBPROGRAM IC107 IS CALLED BY THIS PROGRAM.          IC1064.2
003300 ENVIRONMENT DIVISION.                                            IC1064.2
003400 CONFIGURATION SECTION.                                           IC1064.2
003500 SOURCE-COMPUTER.                                                 IC1064.2
003600     XXXXX082.                                                    IC1064.2
003700 OBJECT-COMPUTER.                                                 IC1064.2
003800     XXXXX083.                                                    IC1064.2
003900 INPUT-OUTPUT SECTION.                                            IC1064.2
004000 FILE-CONTROL.                                                    IC1064.2
004100     SELECT PRINT-FILE ASSIGN TO                                  IC1064.2
004200     XXXXX055.                                                    IC1064.2
004300 DATA DIVISION.                                                   IC1064.2
004400 FILE SECTION.                                                    IC1064.2
004500 FD  PRINT-FILE.                                                  IC1064.2
004600 01  PRINT-REC PICTURE X(120).                                    IC1064.2
004700 01  DUMMY-RECORD PICTURE X(120).                                 IC1064.2
004800 WORKING-STORAGE SECTION.                                         IC1064.2
004900 77  IDN1  USAGE IS INDEX.                                        IC1064.2
005000 77  INDEX-VALUE PIC 999.                                         IC1064.2
005100 01  TABLE-1.                                                     IC1064.2
005200     02  DN1 PICTURE X                                            IC1064.2
005300             OCCURS 10 TIMES                                      IC1064.2
005400             INDEXED BY IN1.                                      IC1064.2
005500 01  TABLE-2.                                                     IC1064.2
005600     02  DN2 PICTURE X                                            IC1064.2
005700             OCCURS 10 TIMES                                      IC1064.2
005800             INDEXED BY IN2.                                      IC1064.2
005900 01  TEST-RESULTS.                                                IC1064.2
006000     02 FILLER                   PIC X      VALUE SPACE.          IC1064.2
006100     02 FEATURE                  PIC X(20)  VALUE SPACE.          IC1064.2
006200     02 FILLER                   PIC X      VALUE SPACE.          IC1064.2
006300     02 P-OR-F                   PIC X(5)   VALUE SPACE.          IC1064.2
006400     02 FILLER                   PIC X      VALUE SPACE.          IC1064.2
006500     02  PAR-NAME.                                                IC1064.2
006600       03 FILLER                 PIC X(19)  VALUE SPACE.          IC1064.2
006700       03  PARDOT-X              PIC X      VALUE SPACE.          IC1064.2
006800       03 DOTVALUE               PIC 99     VALUE ZERO.           IC1064.2
006900     02 FILLER                   PIC X(8)   VALUE SPACE.          IC1064.2
007000     02 RE-MARK                  PIC X(61).                       IC1064.2
007100 01  TEST-COMPUTED.                                               IC1064.2
007200     02 FILLER                   PIC X(30)  VALUE SPACE.          IC1064.2
007300     02 FILLER                   PIC X(17)  VALUE                 IC1064.2
007400            "       COMPUTED=".                                   IC1064.2
007500     02 COMPUTED-X.                                               IC1064.2
007600     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          IC1064.2
007700     03 COMPUTED-N               REDEFINES COMPUTED-A             IC1064.2
007800                                 PIC -9(9).9(9).                  IC1064.2
007900     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         IC1064.2
008000     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     IC1064.2
008100     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     IC1064.2
008200     03       CM-18V0 REDEFINES COMPUTED-A.                       IC1064.2
008300         04 COMPUTED-18V0                    PIC -9(18).          IC1064.2
008400         04 FILLER                           PIC X.               IC1064.2
008500     03 FILLER PIC X(50) VALUE SPACE.                             IC1064.2
008600 01  TEST-CORRECT.                                                IC1064.2
008700     02 FILLER PIC X(30) VALUE SPACE.                             IC1064.2
008800     02 FILLER PIC X(17) VALUE "       CORRECT =".                IC1064.2
008900     02 CORRECT-X.                                                IC1064.2
009000     03 CORRECT-A                  PIC X(20) VALUE SPACE.         IC1064.2
009100     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      IC1064.2
009200     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         IC1064.2
009300     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     IC1064.2
009400     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     IC1064.2
009500     03      CR-18V0 REDEFINES CORRECT-A.                         IC1064.2
009600         04 CORRECT-18V0                     PIC -9(18).          IC1064.2
009700         04 FILLER                           PIC X.               IC1064.2
009800     03 FILLER PIC X(2) VALUE SPACE.                              IC1064.2
009900     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     IC1064.2
010000 01  CCVS-C-1.                                                    IC1064.2
010100     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAIC1064.2
010200-    "SS  PARAGRAPH-NAME                                          IC1064.2
010300-    "       REMARKS".                                            IC1064.2
010400     02 FILLER                     PIC X(20)    VALUE SPACE.      IC1064.2
010500 01  CCVS-C-2.                                                    IC1064.2
010600     02 FILLER                     PIC X        VALUE SPACE.      IC1064.2
010700     02 FILLER                     PIC X(6)     VALUE "TESTED".   IC1064.2
010800     02 FILLER                     PIC X(15)    VALUE SPACE.      IC1064.2
010900     02 FILLER                     PIC X(4)     VALUE "FAIL".     IC1064.2
011000     02 FILLER                     PIC X(94)    VALUE SPACE.      IC1064.2
011100 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       IC1064.2
011200 01  REC-CT                        PIC 99       VALUE ZERO.       IC1064.2
011300 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       IC1064.2
011400 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       IC1064.2
011500 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       IC1064.2
011600 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       IC1064.2
011700 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       IC1064.2
011800 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       IC1064.2
011900 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      IC1064.2
012000 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       IC1064.2
012100 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     IC1064.2
012200 01  CCVS-H-1.                                                    IC1064.2
012300     02  FILLER                    PIC X(39)    VALUE SPACES.     IC1064.2
012400     02  FILLER                    PIC X(42)    VALUE             IC1064.2
012500     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 IC1064.2
012600     02  FILLER                    PIC X(39)    VALUE SPACES.     IC1064.2
012700 01  CCVS-H-2A.                                                   IC1064.2
012800   02  FILLER                        PIC X(40)  VALUE SPACE.      IC1064.2
012900   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  IC1064.2
013000   02  FILLER                        PIC XXXX   VALUE             IC1064.2
013100     "4.2 ".                                                      IC1064.2
013200   02  FILLER                        PIC X(28)  VALUE             IC1064.2
013300            " COPY - NOT FOR DISTRIBUTION".                       IC1064.2
013400   02  FILLER                        PIC X(41)  VALUE SPACE.      IC1064.2
013500                                                                  IC1064.2
013600 01  CCVS-H-2B.                                                   IC1064.2
013700   02  FILLER                        PIC X(15)  VALUE             IC1064.2
013800            "TEST RESULT OF ".                                    IC1064.2
013900   02  TEST-ID                       PIC X(9).                    IC1064.2
014000   02  FILLER                        PIC X(4)   VALUE             IC1064.2
014100            " IN ".                                               IC1064.2
014200   02  FILLER                        PIC X(12)  VALUE             IC1064.2
014300     " HIGH       ".                                              IC1064.2
014400   02  FILLER                        PIC X(22)  VALUE             IC1064.2
014500            " LEVEL VALIDATION FOR ".                             IC1064.2
014600   02  FILLER                        PIC X(58)  VALUE             IC1064.2
014700     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IC1064.2
014800 01  CCVS-H-3.                                                    IC1064.2
014900     02  FILLER                      PIC X(34)  VALUE             IC1064.2
015000            " FOR OFFICIAL USE ONLY    ".                         IC1064.2
015100     02  FILLER                      PIC X(58)  VALUE             IC1064.2
015200     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IC1064.2
015300     02  FILLER                      PIC X(28)  VALUE             IC1064.2
015400            "  COPYRIGHT   1985 ".                                IC1064.2
015500 01  CCVS-E-1.                                                    IC1064.2
015600     02 FILLER                       PIC X(52)  VALUE SPACE.      IC1064.2
015700     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              IC1064.2
015800     02 ID-AGAIN                     PIC X(9).                    IC1064.2
015900     02 FILLER                       PIC X(45)  VALUE SPACES.     IC1064.2
016000 01  CCVS-E-2.                                                    IC1064.2
016100     02  FILLER                      PIC X(31)  VALUE SPACE.      IC1064.2
016200     02  FILLER                      PIC X(21)  VALUE SPACE.      IC1064.2
016300     02 CCVS-E-2-2.                                               IC1064.2
016400         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      IC1064.2
016500         03 FILLER                   PIC X      VALUE SPACE.      IC1064.2
016600         03 ENDER-DESC               PIC X(44)  VALUE             IC1064.2
016700            "ERRORS ENCOUNTERED".                                 IC1064.2
016800 01  CCVS-E-3.                                                    IC1064.2
016900     02  FILLER                      PIC X(22)  VALUE             IC1064.2
017000            " FOR OFFICIAL USE ONLY".                             IC1064.2
017100     02  FILLER                      PIC X(12)  VALUE SPACE.      IC1064.2
017200     02  FILLER                      PIC X(58)  VALUE             IC1064.2
017300     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IC1064.2
017400     02  FILLER                      PIC X(13)  VALUE SPACE.      IC1064.2
017500     02 FILLER                       PIC X(15)  VALUE             IC1064.2
017600             " COPYRIGHT 1985".                                   IC1064.2
017700 01  CCVS-E-4.                                                    IC1064.2
017800     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      IC1064.2
017900     02 FILLER                       PIC X(4)   VALUE " OF ".     IC1064.2
018000     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      IC1064.2
018100     02 FILLER                       PIC X(40)  VALUE             IC1064.2
018200      "  TESTS WERE EXECUTED SUCCESSFULLY".                       IC1064.2
018300 01  XXINFO.                                                      IC1064.2
018400     02 FILLER                       PIC X(19)  VALUE             IC1064.2
018500            "*** INFORMATION ***".                                IC1064.2
018600     02 INFO-TEXT.                                                IC1064.2
018700       04 FILLER                     PIC X(8)   VALUE SPACE.      IC1064.2
018800       04 XXCOMPUTED                 PIC X(20).                   IC1064.2
018900       04 FILLER                     PIC X(5)   VALUE SPACE.      IC1064.2
019000       04 XXCORRECT                  PIC X(20).                   IC1064.2
019100     02 INF-ANSI-REFERENCE           PIC X(48).                   IC1064.2
019200 01  HYPHEN-LINE.                                                 IC1064.2
019300     02 FILLER  PIC IS X VALUE IS SPACE.                          IC1064.2
019400     02 FILLER  PIC IS X(65)    VALUE IS "************************IC1064.2
019500-    "*****************************************".                 IC1064.2
019600     02 FILLER  PIC IS X(54)    VALUE IS "************************IC1064.2
019700-    "******************************".                            IC1064.2
019800 01  CCVS-PGM-ID                     PIC X(9)   VALUE             IC1064.2
019900     "IC106A".                                                    IC1064.2
020000 PROCEDURE DIVISION.                                              IC1064.2
020100 CCVS1 SECTION.                                                   IC1064.2
020200 OPEN-FILES.                                                      IC1064.2
020300     OPEN     OUTPUT PRINT-FILE.                                  IC1064.2
020400     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   IC1064.2
020500     MOVE    SPACE TO TEST-RESULTS.                               IC1064.2
020600     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             IC1064.2
020700     GO TO CCVS1-EXIT.                                            IC1064.2
020800 CLOSE-FILES.                                                     IC1064.2
020900     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   IC1064.2
021000 TERMINATE-CCVS.                                                  IC1064.2
021100*    EXIT PROGRAM.                                                IC1064.2
021200*TERMINATE-CALL.                                                  IC1064.2
021300     STOP     RUN.                                                IC1064.2
021400 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         IC1064.2
021500 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           IC1064.2
021600 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          IC1064.2
021700 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      IC1064.2
021800     MOVE "****TEST DELETED****" TO RE-MARK.                      IC1064.2
021900 PRINT-DETAIL.                                                    IC1064.2
022000     IF REC-CT NOT EQUAL TO ZERO                                  IC1064.2
022100             MOVE "." TO PARDOT-X                                 IC1064.2
022200             MOVE REC-CT TO DOTVALUE.                             IC1064.2
022300     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      IC1064.2
022400     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               IC1064.2
022500        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 IC1064.2
022600          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 IC1064.2
022700     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              IC1064.2
022800     MOVE SPACE TO CORRECT-X.                                     IC1064.2
022900     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         IC1064.2
023000     MOVE     SPACE TO RE-MARK.                                   IC1064.2
023100 HEAD-ROUTINE.                                                    IC1064.2
023200     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IC1064.2
023300     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IC1064.2
023400     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IC1064.2
023500     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IC1064.2
023600 COLUMN-NAMES-ROUTINE.                                            IC1064.2
023700     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IC1064.2
023800     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IC1064.2
023900     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        IC1064.2
024000 END-ROUTINE.                                                     IC1064.2
024100     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.IC1064.2
024200 END-RTN-EXIT.                                                    IC1064.2
024300     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IC1064.2
024400 END-ROUTINE-1.                                                   IC1064.2
024500      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      IC1064.2
024600      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               IC1064.2
024700      ADD PASS-COUNTER TO ERROR-HOLD.                             IC1064.2
024800*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   IC1064.2
024900      MOVE PASS-COUNTER TO CCVS-E-4-1.                            IC1064.2
025000      MOVE ERROR-HOLD TO CCVS-E-4-2.                              IC1064.2
025100      MOVE CCVS-E-4 TO CCVS-E-2-2.                                IC1064.2
025200      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           IC1064.2
025300  END-ROUTINE-12.                                                 IC1064.2
025400      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        IC1064.2
025500     IF       ERROR-COUNTER IS EQUAL TO ZERO                      IC1064.2
025600         MOVE "NO " TO ERROR-TOTAL                                IC1064.2
025700         ELSE                                                     IC1064.2
025800         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       IC1064.2
025900     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           IC1064.2
026000     PERFORM WRITE-LINE.                                          IC1064.2
026100 END-ROUTINE-13.                                                  IC1064.2
026200     IF DELETE-COUNTER IS EQUAL TO ZERO                           IC1064.2
026300         MOVE "NO " TO ERROR-TOTAL  ELSE                          IC1064.2
026400         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      IC1064.2
026500     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   IC1064.2
026600     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IC1064.2
026700      IF   INSPECT-COUNTER EQUAL TO ZERO                          IC1064.2
026800          MOVE "NO " TO ERROR-TOTAL                               IC1064.2
026900      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   IC1064.2
027000      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            IC1064.2
027100      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          IC1064.2
027200     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IC1064.2
027300 WRITE-LINE.                                                      IC1064.2
027400     ADD 1 TO RECORD-COUNT.                                       IC1064.2
027500*    IF RECORD-COUNT GREATER 50                                   IC1064.2
027600*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          IC1064.2
027700*        MOVE SPACE TO DUMMY-RECORD                               IC1064.2
027800*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  IC1064.2
027900*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             IC1064.2
028000*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     IC1064.2
028100*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          IC1064.2
028200*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          IC1064.2
028300*        MOVE ZERO TO RECORD-COUNT.                               IC1064.2
028400     PERFORM WRT-LN.                                              IC1064.2
028500 WRT-LN.                                                          IC1064.2
028600     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               IC1064.2
028700     MOVE SPACE TO DUMMY-RECORD.                                  IC1064.2
028800 BLANK-LINE-PRINT.                                                IC1064.2
028900     PERFORM WRT-LN.                                              IC1064.2
029000 FAIL-ROUTINE.                                                    IC1064.2
029100     IF   COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE. IC1064.2
029200     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.IC1064.2
029300     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IC1064.2
029400     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   IC1064.2
029500     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IC1064.2
029600     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IC1064.2
029700     GO TO  FAIL-ROUTINE-EX.                                      IC1064.2
029800 FAIL-ROUTINE-WRITE.                                              IC1064.2
029900     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         IC1064.2
030000     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 IC1064.2
030100     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. IC1064.2
030200     MOVE   SPACES TO COR-ANSI-REFERENCE.                         IC1064.2
030300 FAIL-ROUTINE-EX. EXIT.                                           IC1064.2
030400 BAIL-OUT.                                                        IC1064.2
030500     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   IC1064.2
030600     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           IC1064.2
030700 BAIL-OUT-WRITE.                                                  IC1064.2
030800     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  IC1064.2
030900     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IC1064.2
031000     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IC1064.2
031100     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IC1064.2
031200 BAIL-OUT-EX. EXIT.                                               IC1064.2
031300 CCVS1-EXIT.                                                      IC1064.2
031400     EXIT.                                                        IC1064.2
031500 SEC-IC106-0001 SECTION.                                          IC1064.2
031600 LINK-TEST-INITIALIZE.                                            IC1064.2
031700     MOVE "ABCDEFGHIJ" TO TABLE-1.                                IC1064.2
031800     MOVE SPACE TO TABLE-2.                                       IC1064.2
031900     SET IN1 TO 6.                                                IC1064.2
032000     SET IDN1 TO IN1.                                             IC1064.2
032100     CALL "IC107A" USING IDN1 TABLE-1 TABLE-2.                    IC1064.2
032200 LINK-TEST-01.                                                    IC1064.2
032300     MOVE "SEPARATE INDEXES" TO FEATURE.                          IC1064.2
032400     MOVE "LINK-TEST-01" TO PAR-NAME.                             IC1064.2
032500*        THIS TEST VERIFIES THAT IN1 HAS NOT BEEN AFFECTED        IC1064.2
032600*    BY THE USE OF AN INDEX FOR TABLE-1 IN THE SUBPROGRAM.        IC1064.2
032700 LINK-TEST-01-01.                                                 IC1064.2
032800     MOVE 1 TO REC-CT.                                            IC1064.2
032900     IF DN1 (IN1) EQUAL TO "F"                                    IC1064.2
033000         PERFORM PASS                                             IC1064.2
033100         GO TO LINK-WRITE-01-01.                                  IC1064.2
033200 LINK-FAIL-01-01.                                                 IC1064.2
033300     PERFORM FAIL.                                                IC1064.2
033400     MOVE DN1 (IN1) TO COMPUTED-A.                                IC1064.2
033500     MOVE "F" TO CORRECT-A.                                       IC1064.2
033600     MOVE "TABLE INDEX DESTROYED" TO RE-MARK.                     IC1064.2
033700 LINK-WRITE-01-01.                                                IC1064.2
033800     PERFORM PRINT-DETAIL.                                        IC1064.2
033900 LINK-TEST-01-02.                                                 IC1064.2
034000     ADD 1 TO REC-CT.                                             IC1064.2
034100     IF IN1 EQUAL TO 6                                            IC1064.2
034200         PERFORM PASS                                             IC1064.2
034300         GO TO LINK-WRITE-01-02.                                  IC1064.2
034400 LINK-FAIL-01-02.                                                 IC1064.2
034500     PERFORM FAIL.                                                IC1064.2
034600     MOVE 6 TO CORRECT-18V0.                                      IC1064.2
034700     SET      INDEX-VALUE TO IN1.                                 IC1064.2
034800     MOVE     INDEX-VALUE TO COMPUTED-18V0.                       IC1064.2
034900     MOVE "TABLE INDEX DESTROYED" TO RE-MARK.                     IC1064.2
035000 LINK-WRITE-01-02.                                                IC1064.2
035100     PERFORM PRINT-DETAIL.                                        IC1064.2
035200 LINK-TEST-02.                                                    IC1064.2
035300     MOVE "INDEX DATA ITEM" TO FEATURE.                           IC1064.2
035400     MOVE "LINK-TEST-02" TO PAR-NAME.                             IC1064.2
035500*        THIS TEST VERIFIES THAT THE INDEX DATA ITEM WAS          IC1064.2
035600*    USED IN THE SUBPROGRAM TO SET AN INDEX AND AN INDEX          IC1064.2
035700*    DATA ITEM.                                                   IC1064.2
035800 LINK-TEST-02-01.                                                 IC1064.2
035900     MOVE 1 TO REC-CT.                                            IC1064.2
036000     IF DN2 (7) IS EQUAL TO "G"                                   IC1064.2
036100         PERFORM PASS                                             IC1064.2
036200         GO TO LINK-WRITE-02-01.                                  IC1064.2
036300 LINK-FAIL-02-01.                                                 IC1064.2
036400     PERFORM FAIL.                                                IC1064.2
036500     MOVE DN2 (7) TO COMPUTED-A.                                  IC1064.2
036600     MOVE "G" TO CORRECT-A.                                       IC1064.2
036700     MOVE "INDEX DATA ITEM IN LINKAGE SEC" TO RE-MARK.            IC1064.2
036800 LINK-WRITE-02-01.                                                IC1064.2
036900     PERFORM PRINT-DETAIL.                                        IC1064.2
037000 LINK-TEST-02-02.                                                 IC1064.2
037100     ADD 1 TO REC-CT.                                             IC1064.2
037200     IF DN2 (6) EQUAL TO "F"                                      IC1064.2
037300         PERFORM PASS                                             IC1064.2
037400         GO TO LINK-WRITE-02-02.                                  IC1064.2
037500 LINK-FAIL-02-02.                                                 IC1064.2
037600     PERFORM FAIL.                                                IC1064.2
037700     MOVE DN2 (6) TO COMPUTED-A.                                  IC1064.2
037800     MOVE "F" TO CORRECT-A.                                       IC1064.2
037900     MOVE "INDEX DATA ITEM IN LINKAGE SEC" TO RE-MARK.            IC1064.2
038000 LINK-WRITE-02-02.                                                IC1064.2
038100     PERFORM PRINT-DETAIL.                                        IC1064.2
038200 LINK-TEST-03.                                                    IC1064.2
038300     MOVE "SUBPROGRAM INDEX" TO FEATURE.                          IC1064.2
038400     MOVE "LINK-TEST-03" TO PAR-NAME.                             IC1064.2
038500*        THIS TEST  VERIFIES THAT A SUBPROGRAM INDEX FOR          IC1064.2
038600*    A TABLE DEFINED IN THE LINKAGE SECTION OF IC107 CAN BE       IC1064.2
038700*    USED TO REFERENCE THE TABLE.                                 IC1064.2
038800 LINK-TEST-03-01.                                                 IC1064.2
038900     MOVE 1 TO REC-CT.                                            IC1064.2
039000     IF DN2 (1) EQUAL TO "A"                                      IC1064.2
039100         PERFORM PASS                                             IC1064.2
039200         GO TO LINK-WRITE-03-01.                                  IC1064.2
039300 LINK-FAIL-03-01.                                                 IC1064.2
039400     PERFORM FAIL.                                                IC1064.2
039500     MOVE DN2 (1) TO COMPUTED-A.                                  IC1064.2
039600     MOVE "A" TO CORRECT-A.                                       IC1064.2
039700     MOVE "INDEX IN LINKAGE SECTION" TO RE-MARK.                  IC1064.2
039800 LINK-WRITE-03-01.                                                IC1064.2
039900     PERFORM PRINT-DETAIL.                                        IC1064.2
040000 LINK-TEST-03-02.                                                 IC1064.2
040100     ADD 1 TO REC-CT.                                             IC1064.2
040200     IF DN2 (2) EQUAL TO "B"                                      IC1064.2
040300         PERFORM PASS                                             IC1064.2
040400         GO TO LINK-WRITE-03-02.                                  IC1064.2
040500 LINK-FAIL-03-02.                                                 IC1064.2
040600     PERFORM FAIL.                                                IC1064.2
040700     MOVE DN2 (2) TO COMPUTED-A.                                  IC1064.2
040800     MOVE "B" TO CORRECT-A.                                       IC1064.2
040900     MOVE "INDEX IN LINKAGE SECTION" TO RE-MARK.                  IC1064.2
041000 LINK-WRITE-03-02.                                                IC1064.2
041100     PERFORM PRINT-DETAIL.                                        IC1064.2
041200 LINK-TEST-04.                                                    IC1064.2
041300     MOVE "INDEX DATA ITEM" TO FEATURE.                           IC1064.2
041400     MOVE "LINK-TEST-04" TO PAR-NAME.                             IC1064.2
041500*        THIS TEST VERIFIES THAT AN INDEX DATA ITEM               IC1064.2
041600*    SET IN THE SUBPROGRAM CAN BE USED IN THE MAIN PROGRAM.       IC1064.2
041700 LINK-TEST-04-01.                                                 IC1064.2
041800     MOVE 1 TO REC-CT.                                            IC1064.2
041900     SET IN1 TO IDN1.                                             IC1064.2
042000     IF IN1 EQUAL TO 3                                            IC1064.2
042100         PERFORM PASS                                             IC1064.2
042200         GO TO LINK-WRITE-04-01.                                  IC1064.2
042300 LINK-FAIL-04-01.                                                 IC1064.2
042400     MOVE  3  TO CORRECT-18V0.                                    IC1064.2
042500     SET      INDEX-VALUE TO IN1.                                 IC1064.2
042600     MOVE     INDEX-VALUE TO COMPUTED-18V0.                       IC1064.2
042700     PERFORM FAIL.                                                IC1064.2
042800     MOVE "INDEX DATA ITEM SET IN SUBPROG" TO RE-MARK.            IC1064.2
042900 LINK-WRITE-04-01.                                                IC1064.2
043000     PERFORM PRINT-DETAIL.                                        IC1064.2
043100 LINK-TEST-04-02.                                                 IC1064.2
043200     ADD 1 TO REC-CT.                                             IC1064.2
043300     IF DN1 (IN1) EQUAL TO "C"                                    IC1064.2
043400         PERFORM PASS                                             IC1064.2
043500         GO TO LINK-WRITE-04-02.                                  IC1064.2
043600 LINK-FAIL-04-02.                                                 IC1064.2
043700     MOVE "C" TO CORRECT-A.                                       IC1064.2
043800     MOVE DN1 (IN1) TO COMPUTED-A.                                IC1064.2
043900     MOVE "INDEX DATA ITEM SET IN SUBPROG" TO RE-MARK.            IC1064.2
044000     PERFORM FAIL.                                                IC1064.2
044100 LINK-WRITE-04-02.                                                IC1064.2
044200     PERFORM PRINT-DETAIL.                                        IC1064.2
044300 LINK-TEST-04-03.                                                 IC1064.2
044400     ADD 1 TO REC-CT.                                             IC1064.2
044500     IF DN2 (3) EQUAL TO "C"                                      IC1064.2
044600         PERFORM PASS                                             IC1064.2
044700         GO TO LINK-WRITE-04-03.                                  IC1064.2
044800 LINK-FAIL-04-03.                                                 IC1064.2
044900     PERFORM FAIL.                                                IC1064.2
045000     MOVE "C" TO CORRECT-A.                                       IC1064.2
045100     MOVE DN2 (3) TO COMPUTED-A.                                  IC1064.2
045200     MOVE "INDEX DATA ITEM SET IN SUBPROG" TO RE-MARK.            IC1064.2
045300 LINK-WRITE-04-03.                                                IC1064.2
045400     PERFORM PRINT-DETAIL.                                        IC1064.2
045500 LINK-TEST-05.                                                    IC1064.2
045600     MOVE "TABLE REFERENCES" TO FEATURE.                          IC1064.2
045700     MOVE "LINK-TEST-05" TO PAR-NAME.                             IC1064.2
045800*        THIS TEST VERIFIES THAT DATA WAS MOVED FROM THE          IC1064.2
045900*    FIRST TABLE IN USING PHRASE TO SECOND TABLE IN USING PHRASE. IC1064.2
046000*    DATA WAS MOVED IN SUBPROGRAM IC107.                          IC1064.2
046100 LINK-TEST-05-01.                                                 IC1064.2
046200     MOVE 1 TO REC-CT.                                            IC1064.2
046300     IF DN2 (4) EQUAL TO "D"                                      IC1064.2
046400         PERFORM PASS                                             IC1064.2
046500         GO TO LINK-WRITE-05-01.                                  IC1064.2
046600 LINK-FAIL-05-01.                                                 IC1064.2
046700     PERFORM FAIL.                                                IC1064.2
046800     MOVE DN2 (4) TO COMPUTED-A.                                  IC1064.2
046900     MOVE "D" TO CORRECT-A.                                       IC1064.2
047000     MOVE "TABLES DEFINED IN LINKAGE SEC" TO RE-MARK.             IC1064.2
047100 LINK-WRITE-05-01.                                                IC1064.2
047200     PERFORM PRINT-DETAIL.                                        IC1064.2
047300 LINK-TEST-05-02.                                                 IC1064.2
047400     ADD 1 TO REC-CT.                                             IC1064.2
047500     IF DN2 (5) EQUAL TO "E"                                      IC1064.2
047600         PERFORM PASS                                             IC1064.2
047700         GO TO LINK-WRITE-05-02.                                  IC1064.2
047800 LINK-FAIL-05-02.                                                 IC1064.2
047900     PERFORM FAIL.                                                IC1064.2
048000     MOVE DN2 (5) TO COMPUTED-A.                                  IC1064.2
048100     MOVE "E" TO CORRECT-A.                                       IC1064.2
048200     MOVE "TABLES DEFINED IN LINKAGE SEC" TO RE-MARK.             IC1064.2
048300 LINK-WRITE-05-02.                                                IC1064.2
048400     PERFORM PRINT-DETAIL.                                        IC1064.2
048500 LINK-TEST-06.                                                    IC1064.2
048600     MOVE "REDEFINED ITEM" TO FEATURE.                            IC1064.2
048700     MOVE "LINK-TEST-06" TO PAR-NAME.                             IC1064.2
048800*        THIS TEST VERIFIES THAT DATA WAS MOVED TO                IC1064.2
048900*    A REDEFINED ITEM IN THE LINKAGE SECTION OF IC107.            IC1064.2
049000 LINK-TEST-06-01.                                                 IC1064.2
049100     MOVE 1 TO REC-CT.                                            IC1064.2
049200     IF DN2 (8) EQUAL TO "X"                                      IC1064.2
049300         PERFORM PASS                                             IC1064.2
049400         GO TO LINK-WRITE-06-01.                                  IC1064.2
049500 LINK-FAIL-06-01.                                                 IC1064.2
049600     PERFORM FAIL.                                                IC1064.2
049700     MOVE DN2 (8) TO COMPUTED-A.                                  IC1064.2
049800     MOVE "X" TO CORRECT-A.                                       IC1064.2
049900     MOVE "REDEFINED ITEM IN LINKAGE SEC" TO RE-MARK.             IC1064.2
050000 LINK-WRITE-06-01.                                                IC1064.2
050100     PERFORM PRINT-DETAIL.                                        IC1064.2
050200 LINK-TEST-06-02.                                                 IC1064.2
050300     ADD 1 TO REC-CT.                                             IC1064.2
050400     IF DN2 (9) EQUAL TO "Y"                                      IC1064.2
050500         PERFORM PASS                                             IC1064.2
050600         GO TO LINK-WRITE-06-02.                                  IC1064.2
050700 LINK-FAIL-06-02.                                                 IC1064.2
050800     PERFORM FAIL.                                                IC1064.2
050900     MOVE DN2 (9) TO COMPUTED-A.                                  IC1064.2
051000     MOVE "Y" TO CORRECT-A.                                       IC1064.2
051100     MOVE "REDEFINED ITEM IN LINKAGE SEC" TO RE-MARK.             IC1064.2
051200 LINK-WRITE-06-02.                                                IC1064.2
051300     PERFORM PRINT-DETAIL.                                        IC1064.2
051400 LINK-TEST-06-03.                                                 IC1064.2
051500     ADD 1 TO REC-CT.                                             IC1064.2
051600     IF DN2 (10) EQUAL TO "Z"                                     IC1064.2
051700         PERFORM PASS                                             IC1064.2
051800         GO TO LINK-WRITE-06-03.                                  IC1064.2
051900 LINK-FAIL-06-03.                                                 IC1064.2
052000     PERFORM FAIL.                                                IC1064.2
052100     MOVE DN2 (10) TO COMPUTED-A.                                 IC1064.2
052200     MOVE "Z" TO CORRECT-A.                                       IC1064.2
052300     MOVE "REDEFINED ITEM IN LINKAGE SEC" TO RE-MARK.             IC1064.2
052400 LINK-WRITE-06-03.                                                IC1064.2
052500     PERFORM PRINT-DETAIL.                                        IC1064.2
052600 LINK-END-ROUTINE.                                                IC1064.2
052700     GO TO CCVS-EXIT.                                             IC1064.2
052800 CCVS-EXIT SECTION.                                               IC1064.2
052900 CCVS-999999.                                                     IC1064.2
053000     GO TO CLOSE-FILES.                                           IC1064.2
      *END-OF,IC106A                                                                  
