      *HEADER,COBOL,IC224A                                                            
000100 IDENTIFICATION DIVISION.                                         IC2244.2
000200 PROGRAM-ID.                                                      IC2244.2
000300     IC224A.                                                      IC2244.2
000400****************************************************************  IC2244.2
000500*                                                              *  IC2244.2
000600*    VALIDATION FOR:-                                          *  IC2244.2
000700*                                                              *  IC2244.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IC2244.2
000900*                                                              *  IC2244.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IC2244.2
001100*                                                              *  IC2244.2
001200****************************************************************  IC2244.2
001300*                                                              *  IC2244.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  IC2244.2
001500*                                                              *  IC2244.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  IC2244.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  IC2244.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  IC2244.2
001900*                                                              *  IC2244.2
002000****************************************************************  IC2244.2
002100*                                                              *  IC2244.2
002200*    PROGRAM IC224A AND IC224A-1 WILL TEST THE NEW LANGUAGE    *  IC2244.2
002300*    ELEMENTS FOR THE LEVEL 2 INTER-PROGRAM COMMUNICATION      *  IC2244.2
002400*    MODULE.                                                   *  IC2244.2
002500*    THE NEW LANGUAGE ELEMENTS TO BE TESTED WILL BE:           *  IC2244.2
002600*          "BY CONTENT"       PHRASE                           *  IC2244.2
002700*    THE TWO PROGRAMS WILL BE COMPILED IN THE SAME FLOW        *  IC2244.2
002800*    (TO TEST THE "END PROGRAM" STATEMENT) AS SHOWN BELOW:     *  IC2244.2
002900*    IDENTIFICATION DIVISION.                                  *  IC2244.2
003000*    PROGRAM-ID. IC224A.                                       *  IC2244.2
003100*              .                                               *  IC2244.2
003200*              .                                               *  IC2244.2
003300*              .                                               *  IC2244.2
003400*    END PROGRAM IC224A.                                       *  IC2244.2
003500*    PROGRAM-ID. IC224A-1.                                     *  IC2244.2
003600*              .                                               *  IC2244.2
003700*              .                                               *  IC2244.2
003800*              .                                               *  IC2244.2
003900****************************************************************  IC2244.2
004000 ENVIRONMENT DIVISION.                                            IC2244.2
004100 CONFIGURATION SECTION.                                           IC2244.2
004200 SOURCE-COMPUTER.                                                 IC2244.2
004300     XXXXX082.                                                    IC2244.2
004400 OBJECT-COMPUTER.                                                 IC2244.2
004500     XXXXX083.                                                    IC2244.2
004600 INPUT-OUTPUT SECTION.                                            IC2244.2
004700 FILE-CONTROL.                                                    IC2244.2
004800     SELECT PRINT-FILE ASSIGN TO                                  IC2244.2
004900     XXXXX055.                                                    IC2244.2
005000 DATA DIVISION.                                                   IC2244.2
005100 FILE SECTION.                                                    IC2244.2
005200 FD  PRINT-FILE.                                                  IC2244.2
005300 01  PRINT-REC PICTURE X(120).                                    IC2244.2
005400 01  DUMMY-RECORD PICTURE X(120).                                 IC2244.2
005500 WORKING-STORAGE SECTION.                                         IC2244.2
005600 77  DN1 PICTURE S99  VALUE ZERO.                                 IC2244.2
005700 77  DN3 PICTURE S99.                                             IC2244.2
005800 77  ID1 PICTURE X(8) VALUE "IC224A-1".                           IC2244.2
005900 77  ID2 PICTURE X(8).                                            IC2244.2
006000 77  DN2 PICTURE S99                                              IC2244.2
006100         USAGE COMPUTATIONAL, VALUE ZERO.                         IC2244.2
006200 77  DN4 PICTURE S99                                              IC2244.2
006300         USAGE IS COMPUTATIONAL.                                  IC2244.2
006400 77  SAVE-DN1 PICTURE S99.                                        IC2244.2
006500 77  SAVE-DN3 PICTURE S99.                                        IC2244.2
006600 77  SAVE-DN2 PICTURE S99                                         IC2244.2
006700         USAGE COMPUTATIONAL.                                     IC2244.2
006800 77  SAVE-DN4 PICTURE S99                                         IC2244.2
006900         USAGE IS COMPUTATIONAL.                                  IC2244.2
007000 77  CALL-COUNT PIC S99.                                          IC2244.2
007100 77  FAIL-FLAG PIC 9.                                             IC2244.2
007200 01  TEST-RESULTS.                                                IC2244.2
007300     02 FILLER                   PIC X      VALUE SPACE.          IC2244.2
007400     02 FEATURE                  PIC X(20)  VALUE SPACE.          IC2244.2
007500     02 FILLER                   PIC X      VALUE SPACE.          IC2244.2
007600     02 P-OR-F                   PIC X(5)   VALUE SPACE.          IC2244.2
007700     02 FILLER                   PIC X      VALUE SPACE.          IC2244.2
007800     02  PAR-NAME.                                                IC2244.2
007900       03 FILLER                 PIC X(19)  VALUE SPACE.          IC2244.2
008000       03  PARDOT-X              PIC X      VALUE SPACE.          IC2244.2
008100       03 DOTVALUE               PIC 99     VALUE ZERO.           IC2244.2
008200     02 FILLER                   PIC X(8)   VALUE SPACE.          IC2244.2
008300     02 RE-MARK                  PIC X(61).                       IC2244.2
008400 01  TEST-COMPUTED.                                               IC2244.2
008500     02 FILLER                   PIC X(30)  VALUE SPACE.          IC2244.2
008600     02 FILLER                   PIC X(17)  VALUE                 IC2244.2
008700            "       COMPUTED=".                                   IC2244.2
008800     02 COMPUTED-X.                                               IC2244.2
008900     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          IC2244.2
009000     03 COMPUTED-N               REDEFINES COMPUTED-A             IC2244.2
009100                                 PIC -9(9).9(9).                  IC2244.2
009200     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         IC2244.2
009300     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     IC2244.2
009400     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     IC2244.2
009500     03       CM-18V0 REDEFINES COMPUTED-A.                       IC2244.2
009600         04 COMPUTED-18V0                    PIC -9(18).          IC2244.2
009700         04 FILLER                           PIC X.               IC2244.2
009800     03 FILLER PIC X(50) VALUE SPACE.                             IC2244.2
009900 01  TEST-CORRECT.                                                IC2244.2
010000     02 FILLER PIC X(30) VALUE SPACE.                             IC2244.2
010100     02 FILLER PIC X(17) VALUE "       CORRECT =".                IC2244.2
010200     02 CORRECT-X.                                                IC2244.2
010300     03 CORRECT-A                  PIC X(20) VALUE SPACE.         IC2244.2
010400     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      IC2244.2
010500     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         IC2244.2
010600     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     IC2244.2
010700     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     IC2244.2
010800     03      CR-18V0 REDEFINES CORRECT-A.                         IC2244.2
010900         04 CORRECT-18V0                     PIC -9(18).          IC2244.2
011000         04 FILLER                           PIC X.               IC2244.2
011100     03 FILLER PIC X(2) VALUE SPACE.                              IC2244.2
011200     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     IC2244.2
011300 01  CCVS-C-1.                                                    IC2244.2
011400     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAIC2244.2
011500-    "SS  PARAGRAPH-NAME                                          IC2244.2
011600-    "       REMARKS".                                            IC2244.2
011700     02 FILLER                     PIC X(20)    VALUE SPACE.      IC2244.2
011800 01  CCVS-C-2.                                                    IC2244.2
011900     02 FILLER                     PIC X        VALUE SPACE.      IC2244.2
012000     02 FILLER                     PIC X(6)     VALUE "TESTED".   IC2244.2
012100     02 FILLER                     PIC X(15)    VALUE SPACE.      IC2244.2
012200     02 FILLER                     PIC X(4)     VALUE "FAIL".     IC2244.2
012300     02 FILLER                     PIC X(94)    VALUE SPACE.      IC2244.2
012400 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       IC2244.2
012500 01  REC-CT                        PIC 99       VALUE ZERO.       IC2244.2
012600 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       IC2244.2
012700 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       IC2244.2
012800 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       IC2244.2
012900 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       IC2244.2
013000 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       IC2244.2
013100 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       IC2244.2
013200 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      IC2244.2
013300 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       IC2244.2
013400 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     IC2244.2
013500 01  CCVS-H-1.                                                    IC2244.2
013600     02  FILLER                    PIC X(39)    VALUE SPACES.     IC2244.2
013700     02  FILLER                    PIC X(42)    VALUE             IC2244.2
013800     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 IC2244.2
013900     02  FILLER                    PIC X(39)    VALUE SPACES.     IC2244.2
014000 01  CCVS-H-2A.                                                   IC2244.2
014100   02  FILLER                        PIC X(40)  VALUE SPACE.      IC2244.2
014200   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  IC2244.2
014300   02  FILLER                        PIC XXXX   VALUE             IC2244.2
014400     "4.2 ".                                                      IC2244.2
014500   02  FILLER                        PIC X(28)  VALUE             IC2244.2
014600            " COPY - NOT FOR DISTRIBUTION".                       IC2244.2
014700   02  FILLER                        PIC X(41)  VALUE SPACE.      IC2244.2
014800                                                                  IC2244.2
014900 01  CCVS-H-2B.                                                   IC2244.2
015000   02  FILLER                        PIC X(15)  VALUE             IC2244.2
015100            "TEST RESULT OF ".                                    IC2244.2
015200   02  TEST-ID                       PIC X(9).                    IC2244.2
015300   02  FILLER                        PIC X(4)   VALUE             IC2244.2
015400            " IN ".                                               IC2244.2
015500   02  FILLER                        PIC X(12)  VALUE             IC2244.2
015600     " HIGH       ".                                              IC2244.2
015700   02  FILLER                        PIC X(22)  VALUE             IC2244.2
015800            " LEVEL VALIDATION FOR ".                             IC2244.2
015900   02  FILLER                        PIC X(58)  VALUE             IC2244.2
016000     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IC2244.2
016100 01  CCVS-H-3.                                                    IC2244.2
016200     02  FILLER                      PIC X(34)  VALUE             IC2244.2
016300            " FOR OFFICIAL USE ONLY    ".                         IC2244.2
016400     02  FILLER                      PIC X(58)  VALUE             IC2244.2
016500     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IC2244.2
016600     02  FILLER                      PIC X(28)  VALUE             IC2244.2
016700            "  COPYRIGHT   1985 ".                                IC2244.2
016800 01  CCVS-E-1.                                                    IC2244.2
016900     02 FILLER                       PIC X(52)  VALUE SPACE.      IC2244.2
017000     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              IC2244.2
017100     02 ID-AGAIN                     PIC X(9).                    IC2244.2
017200     02 FILLER                       PIC X(45)  VALUE SPACES.     IC2244.2
017300 01  CCVS-E-2.                                                    IC2244.2
017400     02  FILLER                      PIC X(31)  VALUE SPACE.      IC2244.2
017500     02  FILLER                      PIC X(21)  VALUE SPACE.      IC2244.2
017600     02 CCVS-E-2-2.                                               IC2244.2
017700         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      IC2244.2
017800         03 FILLER                   PIC X      VALUE SPACE.      IC2244.2
017900         03 ENDER-DESC               PIC X(44)  VALUE             IC2244.2
018000            "ERRORS ENCOUNTERED".                                 IC2244.2
018100 01  CCVS-E-3.                                                    IC2244.2
018200     02  FILLER                      PIC X(22)  VALUE             IC2244.2
018300            " FOR OFFICIAL USE ONLY".                             IC2244.2
018400     02  FILLER                      PIC X(12)  VALUE SPACE.      IC2244.2
018500     02  FILLER                      PIC X(58)  VALUE             IC2244.2
018600     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IC2244.2
018700     02  FILLER                      PIC X(13)  VALUE SPACE.      IC2244.2
018800     02 FILLER                       PIC X(15)  VALUE             IC2244.2
018900             " COPYRIGHT 1985".                                   IC2244.2
019000 01  CCVS-E-4.                                                    IC2244.2
019100     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      IC2244.2
019200     02 FILLER                       PIC X(4)   VALUE " OF ".     IC2244.2
019300     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      IC2244.2
019400     02 FILLER                       PIC X(40)  VALUE             IC2244.2
019500      "  TESTS WERE EXECUTED SUCCESSFULLY".                       IC2244.2
019600 01  XXINFO.                                                      IC2244.2
019700     02 FILLER                       PIC X(19)  VALUE             IC2244.2
019800            "*** INFORMATION ***".                                IC2244.2
019900     02 INFO-TEXT.                                                IC2244.2
020000       04 FILLER                     PIC X(8)   VALUE SPACE.      IC2244.2
020100       04 XXCOMPUTED                 PIC X(20).                   IC2244.2
020200       04 FILLER                     PIC X(5)   VALUE SPACE.      IC2244.2
020300       04 XXCORRECT                  PIC X(20).                   IC2244.2
020400     02 INF-ANSI-REFERENCE           PIC X(48).                   IC2244.2
020500 01  HYPHEN-LINE.                                                 IC2244.2
020600     02 FILLER  PIC IS X VALUE IS SPACE.                          IC2244.2
020700     02 FILLER  PIC IS X(65)    VALUE IS "************************IC2244.2
020800-    "*****************************************".                 IC2244.2
020900     02 FILLER  PIC IS X(54)    VALUE IS "************************IC2244.2
021000-    "******************************".                            IC2244.2
021100 01  CCVS-PGM-ID                     PIC X(9)   VALUE             IC2244.2
021200     "IC224A".                                                    IC2244.2
021300 PROCEDURE DIVISION.                                              IC2244.2
021400 CCVS1 SECTION.                                                   IC2244.2
021500 OPEN-FILES.                                                      IC2244.2
021600     OPEN     OUTPUT PRINT-FILE.                                  IC2244.2
021700     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   IC2244.2
021800     MOVE    SPACE TO TEST-RESULTS.                               IC2244.2
021900     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             IC2244.2
022000     GO TO CCVS1-EXIT.                                            IC2244.2
022100 CLOSE-FILES.                                                     IC2244.2
022200     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   IC2244.2
022300 TERMINATE-CCVS.                                                  IC2244.2
022400*    EXIT PROGRAM.                                                IC2244.2
022500*TERMINATE-CALL.                                                  IC2244.2
022600     STOP     RUN.                                                IC2244.2
022700 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         IC2244.2
022800 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           IC2244.2
022900 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          IC2244.2
023000 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      IC2244.2
023100     MOVE "****TEST DELETED****" TO RE-MARK.                      IC2244.2
023200 PRINT-DETAIL.                                                    IC2244.2
023300     IF REC-CT NOT EQUAL TO ZERO                                  IC2244.2
023400             MOVE "." TO PARDOT-X                                 IC2244.2
023500             MOVE REC-CT TO DOTVALUE.                             IC2244.2
023600     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      IC2244.2
023700     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               IC2244.2
023800        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 IC2244.2
023900          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 IC2244.2
024000     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              IC2244.2
024100     MOVE SPACE TO CORRECT-X.                                     IC2244.2
024200     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         IC2244.2
024300     MOVE     SPACE TO RE-MARK.                                   IC2244.2
024400 HEAD-ROUTINE.                                                    IC2244.2
024500     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IC2244.2
024600     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IC2244.2
024700     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IC2244.2
024800     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IC2244.2
024900 COLUMN-NAMES-ROUTINE.                                            IC2244.2
025000     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IC2244.2
025100     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IC2244.2
025200     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        IC2244.2
025300 END-ROUTINE.                                                     IC2244.2
025400     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.IC2244.2
025500 END-RTN-EXIT.                                                    IC2244.2
025600     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IC2244.2
025700 END-ROUTINE-1.                                                   IC2244.2
025800      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      IC2244.2
025900      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               IC2244.2
026000      ADD PASS-COUNTER TO ERROR-HOLD.                             IC2244.2
026100*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   IC2244.2
026200      MOVE PASS-COUNTER TO CCVS-E-4-1.                            IC2244.2
026300      MOVE ERROR-HOLD TO CCVS-E-4-2.                              IC2244.2
026400      MOVE CCVS-E-4 TO CCVS-E-2-2.                                IC2244.2
026500      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           IC2244.2
026600  END-ROUTINE-12.                                                 IC2244.2
026700      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        IC2244.2
026800     IF       ERROR-COUNTER IS EQUAL TO ZERO                      IC2244.2
026900         MOVE "NO " TO ERROR-TOTAL                                IC2244.2
027000         ELSE                                                     IC2244.2
027100         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       IC2244.2
027200     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           IC2244.2
027300     PERFORM WRITE-LINE.                                          IC2244.2
027400 END-ROUTINE-13.                                                  IC2244.2
027500     IF DELETE-COUNTER IS EQUAL TO ZERO                           IC2244.2
027600         MOVE "NO " TO ERROR-TOTAL  ELSE                          IC2244.2
027700         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      IC2244.2
027800     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   IC2244.2
027900     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IC2244.2
028000      IF   INSPECT-COUNTER EQUAL TO ZERO                          IC2244.2
028100          MOVE "NO " TO ERROR-TOTAL                               IC2244.2
028200      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   IC2244.2
028300      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            IC2244.2
028400      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          IC2244.2
028500     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IC2244.2
028600 WRITE-LINE.                                                      IC2244.2
028700     ADD 1 TO RECORD-COUNT.                                       IC2244.2
028800*    IF RECORD-COUNT GREATER 50                                   IC2244.2
028900*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          IC2244.2
029000*        MOVE SPACE TO DUMMY-RECORD                               IC2244.2
029100*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  IC2244.2
029200*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             IC2244.2
029300*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     IC2244.2
029400*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          IC2244.2
029500*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          IC2244.2
029600*        MOVE ZERO TO RECORD-COUNT.                               IC2244.2
029700     PERFORM WRT-LN.                                              IC2244.2
029800 WRT-LN.                                                          IC2244.2
029900     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               IC2244.2
030000     MOVE SPACE TO DUMMY-RECORD.                                  IC2244.2
030100 BLANK-LINE-PRINT.                                                IC2244.2
030200     PERFORM WRT-LN.                                              IC2244.2
030300 FAIL-ROUTINE.                                                    IC2244.2
030400     IF   COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE. IC2244.2
030500     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.IC2244.2
030600     MOVE ANSI-REFERENCE TO INF-ANSI-REFERENCE.                   IC2244.2
030700     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   IC2244.2
030800     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IC2244.2
030900     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IC2244.2
031000     GO TO  FAIL-ROUTINE-EX.                                      IC2244.2
031100 FAIL-ROUTINE-WRITE.                                              IC2244.2
031200     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         IC2244.2
031300     MOVE ANSI-REFERENCE TO COR-ANSI-REFERENCE.                   IC2244.2
031400     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. IC2244.2
031500     MOVE   SPACES TO COR-ANSI-REFERENCE.                         IC2244.2
031600 FAIL-ROUTINE-EX. EXIT.                                           IC2244.2
031700 BAIL-OUT.                                                        IC2244.2
031800     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   IC2244.2
031900     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           IC2244.2
032000 BAIL-OUT-WRITE.                                                  IC2244.2
032100     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  IC2244.2
032200     MOVE ANSI-REFERENCE TO INF-ANSI-REFERENCE.                   IC2244.2
032300     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IC2244.2
032400     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IC2244.2
032500 BAIL-OUT-EX. EXIT.                                               IC2244.2
032600 CCVS1-EXIT.                                                      IC2244.2
032700     EXIT.                                                        IC2244.2
032800 SECT-IC224A-001 SECTION.                                         IC2244.2
032900 CALL-TEST-01.                                                    IC2244.2
033000     MOVE "CALL-TEST-01" TO PAR-NAME.                             IC2244.2
033100     MOVE     "LEV 2 CALL STATEMENT" TO FEATURE.                  IC2244.2
033200     MOVE 0 TO CALL-COUNT.                                        IC2244.2
033300*        THIS TEST HAS CALL STATEMENTS WITH AN IDENTIFIER         IC2244.2
033400*    CONTAINING THE NAME OF THE SUBPROGRAM TO BE CALLED.          IC2244.2
033500*        CALL-TEST-01 CONTAINS THE BASIC LEVEL 2 CALL STATEMENT.  IC2244.2
033600*    IF IT CANNOT BE COMPILED AND EXECUTED CORRECTLY, THERE IS    IC2244.2
033700*    NO USE IN RUNNING THE LEVEL 2 IPC ROUTINES.                  IC2244.2
033800 CALL-TEST-01-01.                                                 IC2244.2
033900     MOVE ZERO TO DN3, DN4.                                       IC2244.2
034000     MOVE  "CALL-TEST-01-01" TO PAR-NAME.                         IC2244.2
034100     MOVE  DN1 TO SAVE-DN1.                                       IC2244.2
034200     MOVE  DN2 TO SAVE-DN2.                                       IC2244.2
034300     MOVE  DN3 TO SAVE-DN3.                                       IC2244.2
034400     MOVE  DN4 TO SAVE-DN4.                                       IC2244.2
034500     CALL "IC224A-1" USING BY CONTENT DN1, DN2, DN3, DN4          IC2244.2
034600     END-CALL.                                                    IC2244.2
034700     PERFORM CHECK-TEST-01.                                       IC2244.2
034800 CALL-TEST-01-02.                                                 IC2244.2
034900     ADD 1 TO REC-CT.                                             IC2244.2
035000     MOVE ZERO TO DN3, DN4.                                       IC2244.2
035100     MOVE  "CALL-TEST-01-02" TO PAR-NAME.                         IC2244.2
035200     MOVE  DN1 TO SAVE-DN1.                                       IC2244.2
035300     MOVE  DN2 TO SAVE-DN2.                                       IC2244.2
035400     MOVE  DN3 TO SAVE-DN3.                                       IC2244.2
035500     MOVE  DN4 TO SAVE-DN4.                                       IC2244.2
035600     CALL ID1 USING CONTENT DN1, DN2, DN3, DN4                    IC2244.2
035700     END-CALL.                                                    IC2244.2
035800     PERFORM CHECK-TEST-01.                                       IC2244.2
035900 CALL-TEST-01-03.                                                 IC2244.2
036000     MOVE ID1 TO ID2.                                             IC2244.2
036100     MOVE ZERO TO DN3, DN4.                                       IC2244.2
036200     MOVE  "CALL-TEST-01-03" TO PAR-NAME.                         IC2244.2
036300     MOVE  DN1 TO SAVE-DN1.                                       IC2244.2
036400     MOVE  DN2 TO SAVE-DN2.                                       IC2244.2
036500     MOVE  DN3 TO SAVE-DN3.                                       IC2244.2
036600     MOVE  DN4 TO SAVE-DN4.                                       IC2244.2
036700     CALL ID2 USING CONTENT DN1 DN2 DN3 DN4                       IC2244.2
036800     END-CALL.                                                    IC2244.2
036900     PERFORM CHECK-TEST-01.                                       IC2244.2
037000 CALL-TEST-01-04.                                                 IC2244.2
037100     MOVE "IC224A-1" TO ID2.                                      IC2244.2
037200     MOVE ZERO TO DN3, DN4.                                       IC2244.2
037300     MOVE  "CALL-TEST-01-03" TO PAR-NAME.                         IC2244.2
037400     MOVE  DN1 TO SAVE-DN1.                                       IC2244.2
037500     MOVE  DN2 TO SAVE-DN2.                                       IC2244.2
037600     MOVE  DN3 TO SAVE-DN3.                                       IC2244.2
037700     MOVE  DN4 TO SAVE-DN4.                                       IC2244.2
037800     CALL ID2 USING CONTENT DN1, DN2, DN3, DN4                    IC2244.2
037900     END-CALL.                                                    IC2244.2
038000     PERFORM CHECK-TEST-01.                                       IC2244.2
038100 CALL-TEST-02.                                                    IC2244.2
038200     MOVE "CALL-TEST-02" TO PAR-NAME.                             IC2244.2
038300     MOVE "DATA-NAME USED TWICE" TO FEATURE.                      IC2244.2
038400*        THIS TEST USES A DATA-NAME MORE THAN ONCE IN             IC2244.2
038500*    A USING PHRASE OF A CALL STATEMENT.                          IC2244.2
038600 CALL-INIT-02-01.                                                 IC2244.2
038700     MOVE    1 TO DN1.                                            IC2244.2
038800     MOVE    0 TO DN2, DN3, DN4.                                  IC2244.2
038900     MOVE   "CALL-TEST-02-01" TO PAR-NAME.                        IC2244.2
039000     MOVE    DN1 TO SAVE-DN1.                                     IC2244.2
039100     MOVE    DN2 TO SAVE-DN2.                                     IC2244.2
039200     MOVE    DN3 TO SAVE-DN3.                                     IC2244.2
039300     MOVE    DN4 TO SAVE-DN4.                                     IC2244.2
039400     GO TO   CALL-TEST-02-01.                                     IC2244.2
039500 CALL-DELETE-02-01.                                               IC2244.2
039600     PERFORM DE-LETE.                                             IC2244.2
039700     PERFORM PRINT-DETAIL.                                        IC2244.2
039800     GO TO   CALL-INIT-02-02.                                     IC2244.2
039900 CALL-TEST-02-01.                                                 IC2244.2
040000     CALL   "IC224A-1" USING CONTENT DN1, DN2, DN1, DN4           IC2244.2
040100     END-CALL.                                                    IC2244.2
040200     PERFORM CHECK-TEST-01.                                       IC2244.2
040300 CALL-INIT-02-02.                                                 IC2244.2
040400     MOVE    0 TO DN1, DN2, DN3, DN4.                             IC2244.2
040500     MOVE   "CALL-TEST-02-02" TO PAR-NAME.                        IC2244.2
040600     MOVE    DN1 TO SAVE-DN1.                                     IC2244.2
040700     MOVE    DN2 TO SAVE-DN2.                                     IC2244.2
040800     MOVE    DN3 TO SAVE-DN3.                                     IC2244.2
040900     MOVE    DN4 TO SAVE-DN4.                                     IC2244.2
041000     GO TO   CALL-TEST-02-02.                                     IC2244.2
041100 CALL-DELETE-02-02.                                               IC2244.2
041200     PERFORM DE-LETE.                                             IC2244.2
041300     PERFORM PRINT-DETAIL.                                        IC2244.2
041400     GO TO   CALL-INIT-02-03.                                     IC2244.2
041500 CALL-TEST-02-02.                                                 IC2244.2
041600     CALL   "IC224A-1" USING CONTENT DN1, DN2, DN3, DN2           IC2244.2
041700     END-CALL.                                                    IC2244.2
041800     PERFORM CHECK-TEST-01.                                       IC2244.2
041900 CALL-INIT-02-03.                                                 IC2244.2
042000     MOVE    0 TO DN4, DN3.                                       IC2244.2
042100     MOVE    10 TO DN2.                                           IC2244.2
042200     MOVE    25 TO DN1.                                           IC2244.2
042300     MOVE   "CALL-TEST-02-03" TO PAR-NAME.                        IC2244.2
042400     MOVE    DN1 TO SAVE-DN1.                                     IC2244.2
042500     MOVE    DN2 TO SAVE-DN2.                                     IC2244.2
042600     MOVE    DN3 TO SAVE-DN3.                                     IC2244.2
042700     MOVE    DN4 TO SAVE-DN4.                                     IC2244.2
042800     GO TO   CALL-TEST-02-03.                                     IC2244.2
042900 CALL-DELETE-02-03.                                               IC2244.2
043000     PERFORM DE-LETE.                                             IC2244.2
043100     PERFORM PRINT-DETAIL.                                        IC2244.2
043200     GO TO   CALL-TEST-03.                                        IC2244.2
043300 CALL-TEST-02-03.                                                 IC2244.2
043400     CALL ID1 USING CONTENT DN1 DN2 DN1 DN2                       IC2244.2
043500     END-CALL.                                                    IC2244.2
043600     PERFORM CHECK-TEST-01.                                       IC2244.2
043700 CALL-TEST-03.                                                    IC2244.2
043800*        THIS TEST USES THE ON OVERFLOW PHRASE IN THE CALL        IC2244.2
043900*    STATEMENT.  THIS IS A SYNTACTICAL CHECK ONLY, THE ON         IC2244.2
044000*    OVERFLOW CONDITION SHOULD NEVER OCCUR.                       IC2244.2
044100     MOVE "ON OVERFLOW PHRASE" TO FEATURE.                        IC2244.2
044200 CALL-INIT-03-01.                                                 IC2244.2
044300     MOVE    20 TO DN1.                                           IC2244.2
044400     MOVE    30 TO DN2.                                           IC2244.2
044500     MOVE    ZERO TO DN3, DN4.                                    IC2244.2
044600     MOVE   "CALL-TEST-03-01" TO PAR-NAME.                        IC2244.2
044700     MOVE    DN1 TO SAVE-DN1.                                     IC2244.2
044800     MOVE    DN2 TO SAVE-DN2.                                     IC2244.2
044900     MOVE    DN3 TO SAVE-DN3.                                     IC2244.2
045000     MOVE    DN4 TO SAVE-DN4.                                     IC2244.2
045100     GO TO   CALL-TEST-03-01.                                     IC2244.2
045200 CALL-DELETE-03-01.                                               IC2244.2
045300     PERFORM DE-LETE.                                             IC2244.2
045400     PERFORM PRINT-DETAIL.                                        IC2244.2
045500     GO TO   CALL-INIT-03-02.                                     IC2244.2
045600 CALL-TEST-03-01.                                                 IC2244.2
045700     CALL "IC224A-1" USING CONTENT DN1, DN2, DN3, DN4;            IC2244.2
045800         ON OVERFLOW MOVE "OVERFLOW SHOULD NOT OCCUR" TO RE-MARK  IC2244.2
045900                     PERFORM FAIL PERFORM PRINT-DETAIL            IC2244.2
046000     END-CALL.                                                    IC2244.2
046100     MOVE   "CALL-TEST-03-01" TO PAR-NAME.                        IC2244.2
046200     PERFORM CHECK-TEST-01.                                       IC2244.2
046300 CALL-INIT-03-02.                                                 IC2244.2
046400     MOVE    ZERO TO DN3, DN4.                                    IC2244.2
046500     MOVE   "CALL-TEST-03-02" TO PAR-NAME.                        IC2244.2
046600     MOVE    DN1 TO SAVE-DN1.                                     IC2244.2
046700     MOVE    DN2 TO SAVE-DN2.                                     IC2244.2
046800     MOVE    DN3 TO SAVE-DN3.                                     IC2244.2
046900     MOVE    DN4 TO SAVE-DN4.                                     IC2244.2
047000     GO TO   CALL-TEST-03-02.                                     IC2244.2
047100 CALL-DELETE-03-02.                                               IC2244.2
047200     PERFORM DE-LETE.                                             IC2244.2
047300     PERFORM PRINT-DETAIL.                                        IC2244.2
047400     GO TO   CALL-INIT-03-03.                                     IC2244.2
047500 CALL-TEST-03-02.                                                 IC2244.2
047600     CALL "IC224A-1" USING CONTENT DN1, DN2, DN3, DN4;            IC2244.2
047700         OVERFLOW MOVE "OVERFLOW SHOULD NOT OCCUR" TO RE-MARK     IC2244.2
047800                   PERFORM FAIL PERFORM PRINT-DETAIL              IC2244.2
047900     END-CALL.                                                    IC2244.2
048000     PERFORM CHECK-TEST-01.                                       IC2244.2
048100 CALL-INIT-03-03.                                                 IC2244.2
048200     MOVE    ZERO TO DN3, DN4.                                    IC2244.2
048300     MOVE   "CALL-TEST-03-03" TO PAR-NAME.                        IC2244.2
048400     MOVE    DN1 TO SAVE-DN1.                                     IC2244.2
048500     MOVE    DN2 TO SAVE-DN2.                                     IC2244.2
048600     MOVE    DN3 TO SAVE-DN3.                                     IC2244.2
048700     MOVE    DN4 TO SAVE-DN4.                                     IC2244.2
048800     GO TO   CALL-TEST-03-03.                                     IC2244.2
048900 CALL-DELETE-03-03.                                               IC2244.2
049000     PERFORM DE-LETE.                                             IC2244.2
049100     PERFORM PRINT-DETAIL.                                        IC2244.2
049200     GO TO   CALL-INIT-03-04.                                     IC2244.2
049300 CALL-TEST-03-03.                                                 IC2244.2
049400     CALL ID1 USING CONTENT DN1 DN2 DN3 DN4                       IC2244.2
049500         ON OVERFLOW MOVE "OVERFLOW SHOULD NOT OCCUR" TO RE-MARK  IC2244.2
049600                     PERFORM FAIL  PERFORM PRINT-DETAIL           IC2244.2
049700     END-CALL.                                                    IC2244.2
049800     PERFORM CHECK-TEST-01.                                       IC2244.2
049900 CALL-INIT-03-04.                                                 IC2244.2
050000     MOVE ZERO TO DN3, DN4.                                       IC2244.2
050100     MOVE   "CALL-TEST-03-04" TO PAR-NAME.                        IC2244.2
050200     MOVE    DN1 TO SAVE-DN1.                                     IC2244.2
050300     MOVE    DN2 TO SAVE-DN2.                                     IC2244.2
050400     MOVE    DN3 TO SAVE-DN3.                                     IC2244.2
050500     MOVE    DN4 TO SAVE-DN4.                                     IC2244.2
050600     GO TO   CALL-TEST-03-04.                                     IC2244.2
050700 CALL-DELETE-03-04.                                               IC2244.2
050800     PERFORM DE-LETE.                                             IC2244.2
050900     PERFORM PRINT-DETAIL.                                        IC2244.2
051000     GO TO   EXIT-IC224A.                                         IC2244.2
051100 CALL-TEST-03-04.                                                 IC2244.2
051200     CALL ID1 USING CONTENT DN1 DN2 DN3 DN4;                      IC2244.2
051300         OVERFLOW MOVE "OVERFLOW SHOULD NOT OCCUR" TO RE-MARK,    IC2244.2
051400                  PERFORM FAIL  PERFORM PRINT-DETAIL              IC2244.2
051500     END-CALL.                                                    IC2244.2
051600     PERFORM CHECK-TEST-01.                                       IC2244.2
051700     GO TO   EXIT-IC224A.                                         IC2244.2
051800 CALL-DELETE-03.                                                  IC2244.2
051900*        IF THE ON OVERFLOW PHRASE IS NOT RECOGNIZED, DELETE ALL  IC2244.2
052000*    OF THE ABOVE CALL-TEST-03 PARAGRAPHS, STARTING WITH          IC2244.2
052100*    CALL-TEST-03-01.                                             IC2244.2
052200     PERFORM DE-LETE.                                             IC2244.2
052300     PERFORM PRINT-DETAIL.                                        IC2244.2
052400 EXIT-IC224A.                                                     IC2244.2
052500     GO TO   CCVS-EXIT.                                           IC2244.2
052600*                                                                 IC2244.2
052700 SECT-IC224A-CHECK-01.                                            IC2244.2
052800*=====================                                            IC2244.2
052900 CHECK-TEST-01.                                                   IC2244.2
053000     MOVE    1 TO REC-CT.                                         IC2244.2
053100     IF      DN1 = SAVE-DN1                                       IC2244.2
053200             PERFORM PASS                                         IC2244.2
053300             PERFORM PRINT-DETAIL                                 IC2244.2
053400     ELSE                                                         IC2244.2
053500             MOVE    SAVE-DN1 TO CORRECT-N                        IC2244.2
053600             MOVE    DN1      TO COMPUTED-N                       IC2244.2
053700             MOVE   "VALUE OF DN1 HAS CHANGED" TO RE-MARK         IC2244.2
053800             PERFORM FAIL                                         IC2244.2
053900             PERFORM PRINT-DETAIL.                                IC2244.2
054000     ADD     1 TO REC-CT.                                         IC2244.2
054100     IF      DN2 = SAVE-DN2                                       IC2244.2
054200             PERFORM PASS                                         IC2244.2
054300             PERFORM PRINT-DETAIL                                 IC2244.2
054400     ELSE                                                         IC2244.2
054500             MOVE    SAVE-DN2 TO CORRECT-N                        IC2244.2
054600             MOVE    DN2      TO COMPUTED-N                       IC2244.2
054700             MOVE   "VALUE OF DN2 HAS CHANGED" TO RE-MARK         IC2244.2
054800             PERFORM FAIL                                         IC2244.2
054900             PERFORM PRINT-DETAIL.                                IC2244.2
055000     ADD     1 TO REC-CT.                                         IC2244.2
055100     IF      DN3 = SAVE-DN3                                       IC2244.2
055200             PERFORM PASS                                         IC2244.2
055300             PERFORM PRINT-DETAIL                                 IC2244.2
055400     ELSE                                                         IC2244.2
055500             MOVE    SAVE-DN3 TO CORRECT-N                        IC2244.2
055600             MOVE    DN3      TO COMPUTED-N                       IC2244.2
055700             MOVE   "VALUE OF DN3 HAS CHANGED" TO RE-MARK         IC2244.2
055800             PERFORM FAIL                                         IC2244.2
055900             PERFORM PRINT-DETAIL.                                IC2244.2
056000     ADD     1 TO REC-CT.                                         IC2244.2
056100     IF      DN4 = SAVE-DN4                                       IC2244.2
056200             PERFORM PASS                                         IC2244.2
056300             PERFORM PRINT-DETAIL                                 IC2244.2
056400     ELSE                                                         IC2244.2
056500             MOVE    SAVE-DN4 TO CORRECT-N                        IC2244.2
056600             MOVE    DN4      TO COMPUTED-N                       IC2244.2
056700             MOVE   "VALUE OF DN4 HAS CHANGED" TO RE-MARK         IC2244.2
056800             PERFORM FAIL                                         IC2244.2
056900             PERFORM PRINT-DETAIL.                                IC2244.2
057000*                                                                 IC2244.2
057100*                                                                 IC2244.2
057200 CCVS-EXIT SECTION.                                               IC2244.2
057300 CCVS-999999.                                                     IC2244.2
057400     GO TO CLOSE-FILES.                                           IC2244.2
057500 END PROGRAM IC224A.                                              IC2244.2
057600 IDENTIFICATION DIVISION.                                         IC2244.2
057700 PROGRAM-ID.                                                      IC2244.2
057800     IC224A-1.                                                    IC2244.2
057900****************************************************************  IC2244.2
058000*                                                              *  IC2244.2
058100*    THIS PROGRAM FORMS PART OF THE COBOL COMPILER VALIDATION  *  IC2244.2
058200*    SYSTEM (CCVS) USED TO TEST COBOL COMPILERS FOR            *  IC2244.2
058300*    CONFORMANCE WITH THE AMERICAN NATIONAL STANDARD           *  IC2244.2
058400*    (ANSI DOCUMENT REFERENCE: X3.23-1985) AND THE STANDARD OF *  IC2244.2
058500*    THE INTERNATIONAL ORGANIZATION FOR STANDARDISATION        *  IC2244.2
058600*    (ISO DOCUMENT REFERENCE: ISO-1989-1985).                  *  IC2244.2
058700*                                                              *  IC2244.2
058800*    THIS CCVS INCORPORATES ENHANCEMENTS TO THE CCVS FOR THE   *  IC2244.2
058900*    1974 STANDARD (ANSI DOCUMENT REFERENCE: X3.23-1974; ISO   *  IC2244.2
059000*    DOCUMENT REFERENCE: ISO-1989-1978).                       *  IC2244.2
059100*                                                              *  IC2244.2
059200*    THESE ENHANCEMENTS WERE SPECIFIED BY A PROJECT TEAM WHICH *  IC2244.2
059300*    WAS FUNDED BY THE COMMISSION FOR EUROPEAN COMMUNITIES AND *  IC2244.2
059400*    WHICH WAS RESPONSIBLE FOR TECHNICAL ISSUES TO:            *  IC2244.2
059500*                                                              *  IC2244.2
059600*          THE FEDERAL SOFTWARE TESTING CENTER                 *  IC2244.2
059700*          OFFICE OF SOFTWARE DEVELOPMENT                      *  IC2244.2
059800*                & INFORMATION TECHNOLOGY                      *  IC2244.2
059900*          TWO SKYLINE PLACE                                   *  IC2244.2
060000*          SUITE 1100                                          *  IC2244.2
060100*          5203 LEESBURG PIKE                                  *  IC2244.2
060200*          FALLS CHURCH                                        *  IC2244.2
060300*          VA 22041                                            *  IC2244.2
060400*          U.S.A.                                              *  IC2244.2
060500*                                                              *  IC2244.2
060600*    THE PROJECT TEAM MEMBERS WERE:                            *  IC2244.2
060700*                                                              *  IC2244.2
060800*          BIADI (BUREAU INTER ADMINISTRATION                  *  IC2244.2
060900*                 DE DOCUMENTATION INFORMATIQUE)               *  IC2244.2
061000*          21 RUE BARA                                         *  IC2244.2
061100*          F-92132 ISSY                                        *  IC2244.2
061200*          FRANCE                                              *  IC2244.2
061300*                                                              *  IC2244.2
061400*                                                              *  IC2244.2
061500*          GMD (GESELLSCHAFT FUR MATHEMATIK                    *  IC2244.2
061600*               UND DATENVERARBEITUNG MBH)                     *  IC2244.2
061700*          SCHLOSS BIRLINGHOVEN                                *  IC2244.2
061800*          POSTFACH 12 40                                      *  IC2244.2
061900*          D-5205 ST. AUGUSTIN 1                               *  IC2244.2
062000*          GERMANY FR                                          *  IC2244.2
062100*                                                              *  IC2244.2
062200*                                                              *  IC2244.2
062300*          NCC (THE NATIONAL COMPUTING CENTRE LTD)             *  IC2244.2
062400*          OXFORD ROAD                                         *  IC2244.2
062500*          MANCHESTER                                          *  IC2244.2
062600*          M1 7ED                                              *  IC2244.2
062700*          UNITED KINGDOM                                      *  IC2244.2
062800*                                                              *  IC2244.2
062900*                                                              *  IC2244.2
063000*    THIS TEST SUITE WAS PRODUCED BY THE NATIONAL COMPUTING    *  IC2244.2
063100*    CENTRE IN ENGLAND AND IS THE OFFICIAL CCVS TEST SUITE     *  IC2244.2
063200*    USED THROUGHOUT EUROPE AND THE UNITED STATES OF AMERICA.  *  IC2244.2
063300*                                                              *  IC2244.2
063400****************************************************************  IC2244.2
063500*                                                              *  IC2244.2
063600*    VALIDATION FOR:-                                          *  IC2244.2
063700*                                                              *  IC2244.2
063800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IC2244.2
063900*                                                              *  IC2244.2
064000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IC2244.2
064100*                                                              *  IC2244.2
064200****************************************************************  IC2244.2
064300*                                                              *  IC2244.2
064400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  IC2244.2
064500*                                                              *  IC2244.2
064600*        X-55  - SYSTEM PRINTER NAME.                          *  IC2244.2
064700*        X-82  - SOURCE COMPUTER NAME.                         *  IC2244.2
064800*        X-83  - OBJECT COMPUTER NAME.                         *  IC2244.2
064900*                                                              *  IC2244.2
065000****************************************************************  IC2244.2
065100*                                                              *  IC2244.2
065200*    PROGRAM IC224A AND IC224A-1 WILL TEST THE NEW LANGUAGE    *  IC2244.2
065300*    ELEMENTS FOR THE LEVEL 2 INTER-PROGRAM COMMUNICATION      *  IC2244.2
065400*    MODULE.                                                   *  IC2244.2
065500*    THE NEW LANGUAGE ELEMENTS TO BE TESTED WILL BE:           *  IC2244.2
065600*          "BY CONTENT"       PHRASE                           *  IC2244.2
065700*    THE TWO PROGRAMS WILL BE COMPILED IN THE SAME FLOW        *  IC2244.2
065800*    (TO TEST THE "END PROGRAM" STATEMENT) AS SHOWN BELOW:     *  IC2244.2
065900*    IDENTIFICATION DIVISION.                                  *  IC2244.2
066000*    PROGRAM-ID. IC224A.                                       *  IC2244.2
066100*              .                                               *  IC2244.2
066200*              .                                               *  IC2244.2
066300*              .                                               *  IC2244.2
066400*    END PROGRAM IC224A.                                       *  IC2244.2
066500*    PROGRAM-ID. IC224A-1.                                     *  IC2244.2
066600*              .                                               *  IC2244.2
066700*              .                                               *  IC2244.2
066800*              .                                               *  IC2244.2
066900****************************************************************  IC2244.2
067000 ENVIRONMENT DIVISION.                                            IC2244.2
067100 CONFIGURATION SECTION.                                           IC2244.2
067200 SOURCE-COMPUTER.                                                 IC2244.2
067300     XXXXX082.                                                    IC2244.2
067400 OBJECT-COMPUTER.                                                 IC2244.2
067500     XXXXX083.                                                    IC2244.2
067600 INPUT-OUTPUT SECTION.                                            IC2244.2
067700 FILE-CONTROL.                                                    IC2244.2
067800     SELECT PRINT-FILE ASSIGN TO                                  IC2244.2
067900     XXXXX055.                                                    IC2244.2
068000 DATA DIVISION.                                                   IC2244.2
068100 FILE SECTION.                                                    IC2244.2
068200 FD  PRINT-FILE.                                                  IC2244.2
068300 01  PRINT-REC PICTURE X(120).                                    IC2244.2
068400 01  DUMMY-RECORD PICTURE X(120).                                 IC2244.2
068500 WORKING-STORAGE SECTION.                                         IC2244.2
068600 77  WS1 PICTURE S999.                                            IC2244.2
068700 77  WS2 PICTURE S999                                             IC2244.2
068800         USAGE COMPUTATIONAL, VALUE ZERO.                         IC2244.2
068900 LINKAGE SECTION.                                                 IC2244.2
069000 77  DN1 PICTURE S99.                                             IC2244.2
069100 77  DN2 PICTURE S99 USAGE COMPUTATIONAL.                         IC2244.2
069200 77  DN3 PICTURE S99.                                             IC2244.2
069300 77  DN4 PICTURE S99 USAGE COMPUTATIONAL.                         IC2244.2
069400 PROCEDURE DIVISION USING DN1, DN2, DN3, DN4.                     IC2244.2
069500 SECT-IC224A-1-001 SECTION.                                       IC2244.2
069600 CALL-TEST-001.                                                   IC2244.2
069700     MOVE DN1 TO WS1.                                             IC2244.2
069800     ADD 1 TO WS1.                                                IC2244.2
069900     ADD 1 TO WS2.                                                IC2244.2
070000     MOVE WS1 TO DN3.                                             IC2244.2
070100     MOVE WS2 TO DN4.                                             IC2244.2
070200 CALL-EXIT-001.                                                   IC2244.2
070300     EXIT PROGRAM.                                                IC2244.2
      *END-OF,IC224A                                                                  
