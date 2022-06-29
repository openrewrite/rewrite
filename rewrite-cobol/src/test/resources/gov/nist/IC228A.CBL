      *HEADER,COBOL,IC228A                                                            
000100 IDENTIFICATION DIVISION.                                         IC2284.2
000200 PROGRAM-ID.                                                      IC2284.2
000300     IC228A.                                                      IC2284.2
000400****************************************************************  IC2284.2
000500*                                                              *  IC2284.2
000600*    VALIDATION FOR:-                                          *  IC2284.2
000700*                                                              *  IC2284.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IC2284.2
000900*                                                              *  IC2284.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IC2284.2
001100*                                                              *  IC2284.2
001200****************************************************************  IC2284.2
001300*                                                              *  IC2284.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  IC2284.2
001500*                                                              *  IC2284.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  IC2284.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  IC2284.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  IC2284.2
001900*                                                              *  IC2284.2
002000****************************************************************  IC2284.2
002100*                                                              *  IC2284.2
002200*    PROGRAM IC228A AND IC228A-1 WILL TEST THE NEW LANGUAGE    *  IC2284.2
002300*    ELEMENTS FOR THE LEVEL 2 INTER-PROGRAM COMMUNICATION      *  IC2284.2
002400*    MODULE.                                                   *  IC2284.2
002500*    THE NEW LANGUAGE ELEMENT  TO BE TESTED WILL BE:           *  IC2284.2
002600*          THE "GLOBAL"       PHRASE IN WORKING-STORAGE.       *  IC2284.2
002700*    THE TWO PROGRAMS WILL BE COMPILED IN THE SAME FLOW        *  IC2284.2
002800*    (TO TEST THE "END PROGRAM" STATEMENT) AS SHOWN BELOW:     *  IC2284.2
002900*    IDENTIFICATION DIVISION.                                  *  IC2284.2
003000*    PROGRAM-ID. IC228A.                                       *  IC2284.2
003100*              .                                               *  IC2284.2
003200*              .                                               *  IC2284.2
003300*              .                                               *  IC2284.2
003400*    IDENTIFICATION DIVISION.                                  *  IC2284.2
003500*    PROGRAM-ID. IC228A-1.                                     *  IC2284.2
003600*              .                                               *  IC2284.2
003700*              .                                               *  IC2284.2
003800*              .                                               *  IC2284.2
003900*    END PROGRAM IC228A-1.                                     *  IC2284.2
004000*    END PROGRAM IC228A.                                       *  IC2284.2
004100****************************************************************  IC2284.2
004200 ENVIRONMENT DIVISION.                                            IC2284.2
004300 CONFIGURATION SECTION.                                           IC2284.2
004400 SOURCE-COMPUTER.                                                 IC2284.2
004500     XXXXX082.                                                    IC2284.2
004600 OBJECT-COMPUTER.                                                 IC2284.2
004700     XXXXX083.                                                    IC2284.2
004800 INPUT-OUTPUT SECTION.                                            IC2284.2
004900 FILE-CONTROL.                                                    IC2284.2
005000     SELECT PRINT-FILE ASSIGN TO                                  IC2284.2
005100     XXXXX055.                                                    IC2284.2
005200 DATA DIVISION.                                                   IC2284.2
005300 FILE SECTION.                                                    IC2284.2
005400 FD  PRINT-FILE.                                                  IC2284.2
005500 01  PRINT-REC PICTURE X(120).                                    IC2284.2
005600 01  DUMMY-RECORD PICTURE X(120).                                 IC2284.2
005700 WORKING-STORAGE SECTION.                                         IC2284.2
005800 01  GLOBAL-DATA IS GLOBAL.                                       IC2284.2
005900   03        GLO-DATA-1          PIC X(2).                        IC2284.2
006000   03        GLO-DATA-2          PIC X(6).                        IC2284.2
006100           88  CHANGE-MADE-OK  VALUE "CHANGE".                    IC2284.2
006200   03        GLO-DATA-3          PIC 9(8).                        IC2284.2
006300   03        GLO-DATA-4          PIC 9(4).                        IC2284.2
006400 01  SUB                         PIC 9(4)  VALUE ZERO.            IC2284.2
006500*                                                                 IC2284.2
006600 01  TEST-RESULTS.                                                IC2284.2
006700     02 FILLER                   PIC X      VALUE SPACE.          IC2284.2
006800     02 FEATURE                  PIC X(20)  VALUE SPACE.          IC2284.2
006900     02 FILLER                   PIC X      VALUE SPACE.          IC2284.2
007000     02 P-OR-F                   PIC X(5)   VALUE SPACE.          IC2284.2
007100     02 FILLER                   PIC X      VALUE SPACE.          IC2284.2
007200     02  PAR-NAME.                                                IC2284.2
007300       03 FILLER                 PIC X(19)  VALUE SPACE.          IC2284.2
007400       03  PARDOT-X              PIC X      VALUE SPACE.          IC2284.2
007500       03 DOTVALUE               PIC 99     VALUE ZERO.           IC2284.2
007600     02 FILLER                   PIC X(8)   VALUE SPACE.          IC2284.2
007700     02 RE-MARK                  PIC X(61).                       IC2284.2
007800 01  TEST-COMPUTED.                                               IC2284.2
007900     02 FILLER                   PIC X(30)  VALUE SPACE.          IC2284.2
008000     02 FILLER                   PIC X(17)  VALUE                 IC2284.2
008100            "       COMPUTED=".                                   IC2284.2
008200     02 COMPUTED-X.                                               IC2284.2
008300     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          IC2284.2
008400     03 COMPUTED-N               REDEFINES COMPUTED-A             IC2284.2
008500                                 PIC -9(9).9(9).                  IC2284.2
008600     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         IC2284.2
008700     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     IC2284.2
008800     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     IC2284.2
008900     03       CM-18V0 REDEFINES COMPUTED-A.                       IC2284.2
009000         04 COMPUTED-18V0                    PIC -9(18).          IC2284.2
009100         04 FILLER                           PIC X.               IC2284.2
009200     03 FILLER PIC X(50) VALUE SPACE.                             IC2284.2
009300 01  TEST-CORRECT.                                                IC2284.2
009400     02 FILLER PIC X(30) VALUE SPACE.                             IC2284.2
009500     02 FILLER PIC X(17) VALUE "       CORRECT =".                IC2284.2
009600     02 CORRECT-X.                                                IC2284.2
009700     03 CORRECT-A                  PIC X(20) VALUE SPACE.         IC2284.2
009800     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      IC2284.2
009900     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         IC2284.2
010000     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     IC2284.2
010100     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     IC2284.2
010200     03      CR-18V0 REDEFINES CORRECT-A.                         IC2284.2
010300         04 CORRECT-18V0                     PIC -9(18).          IC2284.2
010400         04 FILLER                           PIC X.               IC2284.2
010500     03 FILLER PIC X(2) VALUE SPACE.                              IC2284.2
010600     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     IC2284.2
010700 01  CCVS-C-1.                                                    IC2284.2
010800     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAIC2284.2
010900-    "SS  PARAGRAPH-NAME                                          IC2284.2
011000-    "       REMARKS".                                            IC2284.2
011100     02 FILLER                     PIC X(20)    VALUE SPACE.      IC2284.2
011200 01  CCVS-C-2.                                                    IC2284.2
011300     02 FILLER                     PIC X        VALUE SPACE.      IC2284.2
011400     02 FILLER                     PIC X(6)     VALUE "TESTED".   IC2284.2
011500     02 FILLER                     PIC X(15)    VALUE SPACE.      IC2284.2
011600     02 FILLER                     PIC X(4)     VALUE "FAIL".     IC2284.2
011700     02 FILLER                     PIC X(94)    VALUE SPACE.      IC2284.2
011800 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       IC2284.2
011900 01  REC-CT                        PIC 99       VALUE ZERO.       IC2284.2
012000 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       IC2284.2
012100 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       IC2284.2
012200 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       IC2284.2
012300 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       IC2284.2
012400 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       IC2284.2
012500 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       IC2284.2
012600 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      IC2284.2
012700 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       IC2284.2
012800 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     IC2284.2
012900 01  CCVS-H-1.                                                    IC2284.2
013000     02  FILLER                    PIC X(39)    VALUE SPACES.     IC2284.2
013100     02  FILLER                    PIC X(42)    VALUE             IC2284.2
013200     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 IC2284.2
013300     02  FILLER                    PIC X(39)    VALUE SPACES.     IC2284.2
013400 01  CCVS-H-2A.                                                   IC2284.2
013500   02  FILLER                        PIC X(40)  VALUE SPACE.      IC2284.2
013600   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  IC2284.2
013700   02  FILLER                        PIC XXXX   VALUE             IC2284.2
013800     "4.2 ".                                                      IC2284.2
013900   02  FILLER                        PIC X(28)  VALUE             IC2284.2
014000            " COPY - NOT FOR DISTRIBUTION".                       IC2284.2
014100   02  FILLER                        PIC X(41)  VALUE SPACE.      IC2284.2
014200                                                                  IC2284.2
014300 01  CCVS-H-2B.                                                   IC2284.2
014400   02  FILLER                        PIC X(15)  VALUE             IC2284.2
014500            "TEST RESULT OF ".                                    IC2284.2
014600   02  TEST-ID                       PIC X(9).                    IC2284.2
014700   02  FILLER                        PIC X(4)   VALUE             IC2284.2
014800            " IN ".                                               IC2284.2
014900   02  FILLER                        PIC X(12)  VALUE             IC2284.2
015000     " HIGH       ".                                              IC2284.2
015100   02  FILLER                        PIC X(22)  VALUE             IC2284.2
015200            " LEVEL VALIDATION FOR ".                             IC2284.2
015300   02  FILLER                        PIC X(58)  VALUE             IC2284.2
015400     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IC2284.2
015500 01  CCVS-H-3.                                                    IC2284.2
015600     02  FILLER                      PIC X(34)  VALUE             IC2284.2
015700            " FOR OFFICIAL USE ONLY    ".                         IC2284.2
015800     02  FILLER                      PIC X(58)  VALUE             IC2284.2
015900     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IC2284.2
016000     02  FILLER                      PIC X(28)  VALUE             IC2284.2
016100            "  COPYRIGHT   1985 ".                                IC2284.2
016200 01  CCVS-E-1.                                                    IC2284.2
016300     02 FILLER                       PIC X(52)  VALUE SPACE.      IC2284.2
016400     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              IC2284.2
016500     02 ID-AGAIN                     PIC X(9).                    IC2284.2
016600     02 FILLER                       PIC X(45)  VALUE SPACES.     IC2284.2
016700 01  CCVS-E-2.                                                    IC2284.2
016800     02  FILLER                      PIC X(31)  VALUE SPACE.      IC2284.2
016900     02  FILLER                      PIC X(21)  VALUE SPACE.      IC2284.2
017000     02 CCVS-E-2-2.                                               IC2284.2
017100         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      IC2284.2
017200         03 FILLER                   PIC X      VALUE SPACE.      IC2284.2
017300         03 ENDER-DESC               PIC X(44)  VALUE             IC2284.2
017400            "ERRORS ENCOUNTERED".                                 IC2284.2
017500 01  CCVS-E-3.                                                    IC2284.2
017600     02  FILLER                      PIC X(22)  VALUE             IC2284.2
017700            " FOR OFFICIAL USE ONLY".                             IC2284.2
017800     02  FILLER                      PIC X(12)  VALUE SPACE.      IC2284.2
017900     02  FILLER                      PIC X(58)  VALUE             IC2284.2
018000     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IC2284.2
018100     02  FILLER                      PIC X(13)  VALUE SPACE.      IC2284.2
018200     02 FILLER                       PIC X(15)  VALUE             IC2284.2
018300             " COPYRIGHT 1985".                                   IC2284.2
018400 01  CCVS-E-4.                                                    IC2284.2
018500     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      IC2284.2
018600     02 FILLER                       PIC X(4)   VALUE " OF ".     IC2284.2
018700     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      IC2284.2
018800     02 FILLER                       PIC X(40)  VALUE             IC2284.2
018900      "  TESTS WERE EXECUTED SUCCESSFULLY".                       IC2284.2
019000 01  XXINFO.                                                      IC2284.2
019100     02 FILLER                       PIC X(19)  VALUE             IC2284.2
019200            "*** INFORMATION ***".                                IC2284.2
019300     02 INFO-TEXT.                                                IC2284.2
019400       04 FILLER                     PIC X(8)   VALUE SPACE.      IC2284.2
019500       04 XXCOMPUTED                 PIC X(20).                   IC2284.2
019600       04 FILLER                     PIC X(5)   VALUE SPACE.      IC2284.2
019700       04 XXCORRECT                  PIC X(20).                   IC2284.2
019800     02 INF-ANSI-REFERENCE           PIC X(48).                   IC2284.2
019900 01  HYPHEN-LINE.                                                 IC2284.2
020000     02 FILLER  PIC IS X VALUE IS SPACE.                          IC2284.2
020100     02 FILLER  PIC IS X(65)    VALUE IS "************************IC2284.2
020200-    "*****************************************".                 IC2284.2
020300     02 FILLER  PIC IS X(54)    VALUE IS "************************IC2284.2
020400-    "******************************".                            IC2284.2
020500 01  CCVS-PGM-ID                     PIC X(9)   VALUE             IC2284.2
020600     "IC228A".                                                    IC2284.2
020700 PROCEDURE DIVISION.                                              IC2284.2
020800 CCVS1 SECTION.                                                   IC2284.2
020900 OPEN-FILES.                                                      IC2284.2
021000     OPEN     OUTPUT PRINT-FILE.                                  IC2284.2
021100     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   IC2284.2
021200     MOVE    SPACE TO TEST-RESULTS.                               IC2284.2
021300     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             IC2284.2
021400     GO TO CCVS1-EXIT.                                            IC2284.2
021500 CLOSE-FILES.                                                     IC2284.2
021600     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   IC2284.2
021700 TERMINATE-CCVS.                                                  IC2284.2
021800*    EXIT PROGRAM.                                                IC2284.2
021900*TERMINATE-CALL.                                                  IC2284.2
022000     STOP     RUN.                                                IC2284.2
022100 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         IC2284.2
022200 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           IC2284.2
022300 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          IC2284.2
022400 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      IC2284.2
022500     MOVE "****TEST DELETED****" TO RE-MARK.                      IC2284.2
022600 PRINT-DETAIL.                                                    IC2284.2
022700     IF REC-CT NOT EQUAL TO ZERO                                  IC2284.2
022800             MOVE "." TO PARDOT-X                                 IC2284.2
022900             MOVE REC-CT TO DOTVALUE.                             IC2284.2
023000     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      IC2284.2
023100     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               IC2284.2
023200        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 IC2284.2
023300          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 IC2284.2
023400     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              IC2284.2
023500     MOVE SPACE TO CORRECT-X.                                     IC2284.2
023600     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         IC2284.2
023700     MOVE     SPACE TO RE-MARK.                                   IC2284.2
023800 HEAD-ROUTINE.                                                    IC2284.2
023900     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IC2284.2
024000     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IC2284.2
024100     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IC2284.2
024200     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IC2284.2
024300 COLUMN-NAMES-ROUTINE.                                            IC2284.2
024400     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IC2284.2
024500     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IC2284.2
024600     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        IC2284.2
024700 END-ROUTINE.                                                     IC2284.2
024800     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.IC2284.2
024900 END-RTN-EXIT.                                                    IC2284.2
025000     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IC2284.2
025100 END-ROUTINE-1.                                                   IC2284.2
025200      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      IC2284.2
025300      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               IC2284.2
025400      ADD PASS-COUNTER TO ERROR-HOLD.                             IC2284.2
025500*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   IC2284.2
025600      MOVE PASS-COUNTER TO CCVS-E-4-1.                            IC2284.2
025700      MOVE ERROR-HOLD TO CCVS-E-4-2.                              IC2284.2
025800      MOVE CCVS-E-4 TO CCVS-E-2-2.                                IC2284.2
025900      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           IC2284.2
026000  END-ROUTINE-12.                                                 IC2284.2
026100      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        IC2284.2
026200     IF       ERROR-COUNTER IS EQUAL TO ZERO                      IC2284.2
026300         MOVE "NO " TO ERROR-TOTAL                                IC2284.2
026400         ELSE                                                     IC2284.2
026500         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       IC2284.2
026600     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           IC2284.2
026700     PERFORM WRITE-LINE.                                          IC2284.2
026800 END-ROUTINE-13.                                                  IC2284.2
026900     IF DELETE-COUNTER IS EQUAL TO ZERO                           IC2284.2
027000         MOVE "NO " TO ERROR-TOTAL  ELSE                          IC2284.2
027100         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      IC2284.2
027200     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   IC2284.2
027300     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IC2284.2
027400      IF   INSPECT-COUNTER EQUAL TO ZERO                          IC2284.2
027500          MOVE "NO " TO ERROR-TOTAL                               IC2284.2
027600      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   IC2284.2
027700      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            IC2284.2
027800      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          IC2284.2
027900     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IC2284.2
028000 WRITE-LINE.                                                      IC2284.2
028100     ADD 1 TO RECORD-COUNT.                                       IC2284.2
028200*    IF RECORD-COUNT GREATER 50                                   IC2284.2
028300*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          IC2284.2
028400*        MOVE SPACE TO DUMMY-RECORD                               IC2284.2
028500*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  IC2284.2
028600*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             IC2284.2
028700*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     IC2284.2
028800*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          IC2284.2
028900*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          IC2284.2
029000*        MOVE ZERO TO RECORD-COUNT.                               IC2284.2
029100     PERFORM WRT-LN.                                              IC2284.2
029200 WRT-LN.                                                          IC2284.2
029300     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               IC2284.2
029400     MOVE SPACE TO DUMMY-RECORD.                                  IC2284.2
029500 BLANK-LINE-PRINT.                                                IC2284.2
029600     PERFORM WRT-LN.                                              IC2284.2
029700 FAIL-ROUTINE.                                                    IC2284.2
029800     IF   COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE. IC2284.2
029900     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.IC2284.2
030000     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IC2284.2
030100     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   IC2284.2
030200     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IC2284.2
030300     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IC2284.2
030400     GO TO  FAIL-ROUTINE-EX.                                      IC2284.2
030500 FAIL-ROUTINE-WRITE.                                              IC2284.2
030600     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         IC2284.2
030700     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 IC2284.2
030800     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. IC2284.2
030900     MOVE   SPACES TO COR-ANSI-REFERENCE.                         IC2284.2
031000 FAIL-ROUTINE-EX. EXIT.                                           IC2284.2
031100 BAIL-OUT.                                                        IC2284.2
031200     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   IC2284.2
031300     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           IC2284.2
031400 BAIL-OUT-WRITE.                                                  IC2284.2
031500     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  IC2284.2
031600     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IC2284.2
031700     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IC2284.2
031800     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IC2284.2
031900 BAIL-OUT-EX. EXIT.                                               IC2284.2
032000 CCVS1-EXIT.                                                      IC2284.2
032100     EXIT.                                                        IC2284.2
032200 SECT-IC228A-001 SECTION.                                         IC2284.2
032300 GLO-INIT-01.                                                     IC2284.2
032400     MOVE   "X-20 4.3.2" TO ANSI-REFERENCE.                       IC2284.2
032500     MOVE   "GLOBAL CLAUSE" TO FEATURE.                           IC2284.2
032600     MOVE   "AA"      TO GLO-DATA-1.                              IC2284.2
032700     MOVE   "FIRST]"  TO GLO-DATA-2.                              IC2284.2
032800     MOVE    12345678 TO GLO-DATA-3.                              IC2284.2
032900     MOVE    1        TO GLO-DATA-4.                              IC2284.2
033000 GLO-TEST-01-01-0.                                                IC2284.2
033100     CALL   "IC228A-1"                                            IC2284.2
033200     END-CALL.                                                    IC2284.2
033300     GO TO   GLO-TEST-01-01-1.                                    IC2284.2
033400 GLO-DELETE-01-01.                                                IC2284.2
033500     PERFORM DE-LETE.                                             IC2284.2
033600     PERFORM PRINT-DETAIL.                                        IC2284.2
033700     GO TO   CCVS-EXIT.                                           IC2284.2
033800 GLO-TEST-01-01-1.                                                IC2284.2
033900     MOVE   "GLO-TEST-01-01-1" TO PAR-NAME.                       IC2284.2
034000     IF      GLO-DATA-1 = "ZZ"                                    IC2284.2
034100             PERFORM PASS                                         IC2284.2
034200             PERFORM PRINT-DETAIL                                 IC2284.2
034300     ELSE                                                         IC2284.2
034400             MOVE    GLO-DATA-1  TO COMPUTED-X                    IC2284.2
034500             MOVE   "ZZ" TO CORRECT-X                             IC2284.2
034600             MOVE   "INCORRECT VALUE RETURNED" TO RE-MARK         IC2284.2
034700             PERFORM FAIL                                         IC2284.2
034800             PERFORM PRINT-DETAIL.                                IC2284.2
034900     ADD     1 TO REC-CT.                                         IC2284.2
035000 CALL-TEST-01-01-2.                                               IC2284.2
035100     MOVE   "CALL-TEST-01-01-2" TO PAR-NAME.                      IC2284.2
035200     IF      CHANGE-MADE-OK                                       IC2284.2
035300             PERFORM PASS                                         IC2284.2
035400             PERFORM PRINT-DETAIL                                 IC2284.2
035500     ELSE                                                         IC2284.2
035600             MOVE    GLO-DATA-2  TO COMPUTED-X                    IC2284.2
035700             MOVE   "CHANGE" TO CORRECT-X                         IC2284.2
035800             MOVE   "INCORRECT VALUE RETURNED" TO RE-MARK         IC2284.2
035900             PERFORM FAIL                                         IC2284.2
036000             PERFORM PRINT-DETAIL.                                IC2284.2
036100     ADD     1 TO REC-CT.                                         IC2284.2
036200 CALL-TEST-01-01-3.                                               IC2284.2
036300     MOVE   "CALL-TEST-01-01-3" TO PAR-NAME.                      IC2284.2
036400     IF      GLO-DATA-3 = 87654321                                IC2284.2
036500             PERFORM PASS                                         IC2284.2
036600             PERFORM PRINT-DETAIL                                 IC2284.2
036700     ELSE                                                         IC2284.2
036800             MOVE    GLO-DATA-3  TO COMPUTED-N                    IC2284.2
036900             MOVE    87654321 TO CORRECT-N                        IC2284.2
037000             MOVE   "INCORRECT VALUE RETURNED" TO RE-MARK         IC2284.2
037100             PERFORM FAIL                                         IC2284.2
037200             PERFORM PRINT-DETAIL.                                IC2284.2
037300     ADD     1 TO REC-CT.                                         IC2284.2
037400 CALL-TEST-01-01-4.                                               IC2284.2
037500     MOVE   "CALL-TEST-01-01-4" TO PAR-NAME.                      IC2284.2
037600     IF      GLO-DATA-4 = 11                                      IC2284.2
037700             PERFORM PASS                                         IC2284.2
037800             PERFORM PRINT-DETAIL                                 IC2284.2
037900     ELSE                                                         IC2284.2
038000             MOVE    GLO-DATA-4  TO COMPUTED-N                    IC2284.2
038100             MOVE    11   TO CORRECT-N                            IC2284.2
038200             MOVE   "INCORRECT VALUE RETURNED" TO RE-MARK         IC2284.2
038300             PERFORM FAIL                                         IC2284.2
038400             PERFORM PRINT-DETAIL.                                IC2284.2
038500*                                                                 IC2284.2
038600 CCVS-EXIT SECTION.                                               IC2284.2
038700 CCVS-999999.                                                     IC2284.2
038800     GO TO CLOSE-FILES.                                           IC2284.2
038900 IDENTIFICATION DIVISION.                                         IC2284.2
039000 PROGRAM-ID.                                                      IC2284.2
039100     IC228A-1.                                                    IC2284.2
039200****************************************************************  IC2284.2
039300*                                                              *  IC2284.2
039400*    VALIDATION FOR:-                                          *  IC2284.2
039500*                                                              *  IC2284.2
039600*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IC2284.2
039700*                                                              *  IC2284.2
039800*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IC2284.2
039900*                                                              *  IC2284.2
040000****************************************************************  IC2284.2
040100*                                                              *  IC2284.2
040200*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  IC2284.2
040300*                                                              *  IC2284.2
040400*        X-55  - SYSTEM PRINTER NAME.                          *  IC2284.2
040500*        X-82  - SOURCE COMPUTER NAME.                         *  IC2284.2
040600*        X-83  - OBJECT COMPUTER NAME.                         *  IC2284.2
040700*                                                              *  IC2284.2
040800****************************************************************  IC2284.2
040900*                                                              *  IC2284.2
041000*    PROGRAM IC228A AND IC228A-1 WILL TEST THE NEW LANGUAGE    *  IC2284.2
041100*    ELEMENTS FOR THE LEVEL 2 INTER-PROGRAM COMMUNICATION      *  IC2284.2
041200*    MODULE.                                                   *  IC2284.2
041300*    THE NEW LANGUAGE ELEMENTS TO BE TESTED WILL BE:           *  IC2284.2
041400*           THE "GLOBAL" CLAUSE IN WORKING-STORAGE.          *    IC2284.2
041500*    THE TWO PROGRAMS WILL BE COMPILED IN THE SAME FLOW        *  IC2284.2
041600*    (TO TEST THE "END PROGRAM" STATEMENT) AS SHOWN BELOW:     *  IC2284.2
041700*    IDENTIFICATION DIVISION.                                  *  IC2284.2
041800*    PROGRAM-ID. IC228A.                                       *  IC2284.2
041900*              .                                               *  IC2284.2
042000*              .                                               *  IC2284.2
042100*              .                                               *  IC2284.2
042200*    IDENTIFICATION DIVISION.                                  *  IC2284.2
042300*    PROGRAM-ID. IC228A-1.                                     *  IC2284.2
042400*              .                                               *  IC2284.2
042500*              .                                               *  IC2284.2
042600*              .                                               *  IC2284.2
042700*    END PROGRAM IC228A-1.                                     *  IC2284.2
042800*    END PROGRAM IC228A.                                       *  IC2284.2
042900****************************************************************  IC2284.2
043000 ENVIRONMENT DIVISION.                                            IC2284.2
043100*INPUT-OUTPUT SECTION.                                            IC2284.2
043200 DATA DIVISION.                                                   IC2284.2
043300*FILE SECTION.                                                    IC2284.2
043400 WORKING-STORAGE SECTION.                                         IC2284.2
043500 PROCEDURE DIVISION.                                              IC2284.2
043600 SECT-IC228A-1-001 SECTION.                                       IC2284.2
043700 GLO-TEST-001.                                                    IC2284.2
043800     MOVE   "ZZ"      TO GLO-DATA-1.                              IC2284.2
043900     MOVE   "CHANGE"  TO GLO-DATA-2.                              IC2284.2
044000     MOVE    87654321 TO GLO-DATA-3.                              IC2284.2
044100     ADD     10       TO GLO-DATA-4.                              IC2284.2
044200 GLO-EXIT-001.                                                    IC2284.2
044300     EXIT PROGRAM.                                                IC2284.2
044400 END PROGRAM IC228A-1.                                            IC2284.2
044500 END PROGRAM IC228A.                                              IC2284.2
      *END-OF,IC228A                                                                  
