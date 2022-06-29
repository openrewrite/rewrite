      *HEADER,COBOL,IC114A                                                            
000100 IDENTIFICATION DIVISION.                                         IC1144.2
000200 PROGRAM-ID.                                                      IC1144.2
000300     IC114A.                                                      IC1144.2
000400****************************************************************  IC1144.2
000500*                                                              *  IC1144.2
000600*    VALIDATION FOR:-                                          *  IC1144.2
000700*                                                              *  IC1144.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IC1144.2
000900*                                                              *  IC1144.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IC1144.2
001100*                                                              *  IC1144.2
001200****************************************************************  IC1144.2
001300*                                                              *  IC1144.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  IC1144.2
001500*                                                              *  IC1144.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  IC1144.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  IC1144.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  IC1144.2
001900*                                                              *  IC1144.2
002000****************************************************************  IC1144.2
002100*                                                                 IC1144.2
002200*         THE ROUTINE IC114 IS A MAIN PROGRAM WHICH CALLS THE     IC1144.2
002300*    SUBPROGRAM IC115.  THE PURPOSE OF THESE PROGRAMS IS TO       IC1144.2
002400*    VERIFY THAT A FILE SECTION, A WORKING-STORAGE SECTION, AND   IC1144.2
002500*    A LINKAGE SECTION CAN APPEAR IN A SUBPROGRAM, AND A FILE     IC1144.2
002600*    CAN BE WRITTEN AND READ WITHIN A SUBPROGRAM.                 IC1144.2
002700*                                                                 IC1144.2
002800*         THE PROGRAM IC114 CALLS IC115 TO CREATE AND VERIFY THE  IC1144.2
002900*    FILE.  SUBSEQUENT CALLS TO THE SUBPROGRAM ARE MADE TO READ   IC1144.2
003000*    THE FILE AND RETURN A RECORD TO THE MAIN PROGRAM WHICH CHECKSIC1144.2
003100*    THE RECORD CONTENTS.                                         IC1144.2
003200*                                                                 IC1144.2
003300*         THE SUBPROGRAM IC115 IS ADAPTED FROM THE SEQUENTIAL I-O IC1144.2
003400*    ROUTINE SQ104.  IF SQ104 DOES NOT EXECUTE CORRECTLY THEN     IC1144.2
003500*    THE RESULTS OF THESE TESTS ARE INCONCLUSIVE.                 IC1144.2
003600*                                                                 IC1144.2
003700****************************************************************  IC1144.2
003800 ENVIRONMENT DIVISION.                                            IC1144.2
003900 CONFIGURATION SECTION.                                           IC1144.2
004000 SOURCE-COMPUTER.                                                 IC1144.2
004100     XXXXX082.                                                    IC1144.2
004200 OBJECT-COMPUTER.                                                 IC1144.2
004300     XXXXX083.                                                    IC1144.2
004400 INPUT-OUTPUT SECTION.                                            IC1144.2
004500 FILE-CONTROL.                                                    IC1144.2
004600     SELECT PRINT-FILE ASSIGN TO                                  IC1144.2
004700     XXXXX055.                                                    IC1144.2
004800 DATA DIVISION.                                                   IC1144.2
004900 FILE SECTION.                                                    IC1144.2
005000 FD  PRINT-FILE.                                                  IC1144.2
005100 01  PRINT-REC PICTURE X(120).                                    IC1144.2
005200 01  DUMMY-RECORD PICTURE X(120).                                 IC1144.2
005300 WORKING-STORAGE SECTION.                                         IC1144.2
005400 01  GROUP-LINKAGE-VARIABLES.                                     IC1144.2
005500         02  COUNT-OF-RECORDS  PICTURE 9(6).                      IC1144.2
005600     02  RECORDS-IN-ERROR  PICTURE 9(6).                          IC1144.2
005700     02  ERROR-FLAG  PICTURE 9.                                   IC1144.2
005800     02  EOF-FLAG  PICTURE 9.                                     IC1144.2
005900     02  CALL-FLAG  PICTURE 9.                                    IC1144.2
006000 01  FILE-REC-SQ-FS3.                                             IC1144.2
006100     02  XFILE-NAME-GROUP.                                        IC1144.2
006200         03  FILLER   PIC X(5).                                   IC1144.2
006300         03  XFILE-NAME   PIC X(6).                               IC1144.2
006400     02  XRECORD-NAME-GROUP.                                      IC1144.2
006500         03  FILLER   PIC X(8).                                   IC1144.2
006600         03  XRECORD-NAME   PIC X(6).                             IC1144.2
006700     02  REELUNIT-NUMBER-GROUP.                                   IC1144.2
006800         03  FILLER   PIC X(1).                                   IC1144.2
006900         03  REELUNIT-NUMBER   PIC 9(1).                          IC1144.2
007000     02  FILLER   PIC X(7).                                       IC1144.2
007100     02  XRECORD-NUMBER   PIC 9(6).                               IC1144.2
007200     02  FILLER   PIC X(79).                                      IC1144.2
007300     02  XLABEL-TYPE   PIC X(1).                                  IC1144.2
007400 01  TEST-RESULTS.                                                IC1144.2
007500     02 FILLER                   PIC X      VALUE SPACE.          IC1144.2
007600     02 FEATURE                  PIC X(20)  VALUE SPACE.          IC1144.2
007700     02 FILLER                   PIC X      VALUE SPACE.          IC1144.2
007800     02 P-OR-F                   PIC X(5)   VALUE SPACE.          IC1144.2
007900     02 FILLER                   PIC X      VALUE SPACE.          IC1144.2
008000     02  PAR-NAME.                                                IC1144.2
008100       03 FILLER                 PIC X(19)  VALUE SPACE.          IC1144.2
008200       03  PARDOT-X              PIC X      VALUE SPACE.          IC1144.2
008300       03 DOTVALUE               PIC 99     VALUE ZERO.           IC1144.2
008400     02 FILLER                   PIC X(8)   VALUE SPACE.          IC1144.2
008500     02 RE-MARK                  PIC X(61).                       IC1144.2
008600 01  TEST-COMPUTED.                                               IC1144.2
008700     02 FILLER                   PIC X(30)  VALUE SPACE.          IC1144.2
008800     02 FILLER                   PIC X(17)  VALUE                 IC1144.2
008900            "       COMPUTED=".                                   IC1144.2
009000     02 COMPUTED-X.                                               IC1144.2
009100     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          IC1144.2
009200     03 COMPUTED-N               REDEFINES COMPUTED-A             IC1144.2
009300                                 PIC -9(9).9(9).                  IC1144.2
009400     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         IC1144.2
009500     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     IC1144.2
009600     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     IC1144.2
009700     03       CM-18V0 REDEFINES COMPUTED-A.                       IC1144.2
009800         04 COMPUTED-18V0                    PIC -9(18).          IC1144.2
009900         04 FILLER                           PIC X.               IC1144.2
010000     03 FILLER PIC X(50) VALUE SPACE.                             IC1144.2
010100 01  TEST-CORRECT.                                                IC1144.2
010200     02 FILLER PIC X(30) VALUE SPACE.                             IC1144.2
010300     02 FILLER PIC X(17) VALUE "       CORRECT =".                IC1144.2
010400     02 CORRECT-X.                                                IC1144.2
010500     03 CORRECT-A                  PIC X(20) VALUE SPACE.         IC1144.2
010600     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      IC1144.2
010700     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         IC1144.2
010800     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     IC1144.2
010900     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     IC1144.2
011000     03      CR-18V0 REDEFINES CORRECT-A.                         IC1144.2
011100         04 CORRECT-18V0                     PIC -9(18).          IC1144.2
011200         04 FILLER                           PIC X.               IC1144.2
011300     03 FILLER PIC X(2) VALUE SPACE.                              IC1144.2
011400     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     IC1144.2
011500 01  CCVS-C-1.                                                    IC1144.2
011600     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAIC1144.2
011700-    "SS  PARAGRAPH-NAME                                          IC1144.2
011800-    "       REMARKS".                                            IC1144.2
011900     02 FILLER                     PIC X(20)    VALUE SPACE.      IC1144.2
012000 01  CCVS-C-2.                                                    IC1144.2
012100     02 FILLER                     PIC X        VALUE SPACE.      IC1144.2
012200     02 FILLER                     PIC X(6)     VALUE "TESTED".   IC1144.2
012300     02 FILLER                     PIC X(15)    VALUE SPACE.      IC1144.2
012400     02 FILLER                     PIC X(4)     VALUE "FAIL".     IC1144.2
012500     02 FILLER                     PIC X(94)    VALUE SPACE.      IC1144.2
012600 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       IC1144.2
012700 01  REC-CT                        PIC 99       VALUE ZERO.       IC1144.2
012800 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       IC1144.2
012900 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       IC1144.2
013000 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       IC1144.2
013100 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       IC1144.2
013200 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       IC1144.2
013300 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       IC1144.2
013400 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      IC1144.2
013500 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       IC1144.2
013600 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     IC1144.2
013700 01  CCVS-H-1.                                                    IC1144.2
013800     02  FILLER                    PIC X(39)    VALUE SPACES.     IC1144.2
013900     02  FILLER                    PIC X(42)    VALUE             IC1144.2
014000     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 IC1144.2
014100     02  FILLER                    PIC X(39)    VALUE SPACES.     IC1144.2
014200 01  CCVS-H-2A.                                                   IC1144.2
014300   02  FILLER                        PIC X(40)  VALUE SPACE.      IC1144.2
014400   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  IC1144.2
014500   02  FILLER                        PIC XXXX   VALUE             IC1144.2
014600     "4.2 ".                                                      IC1144.2
014700   02  FILLER                        PIC X(28)  VALUE             IC1144.2
014800            " COPY - NOT FOR DISTRIBUTION".                       IC1144.2
014900   02  FILLER                        PIC X(41)  VALUE SPACE.      IC1144.2
015000                                                                  IC1144.2
015100 01  CCVS-H-2B.                                                   IC1144.2
015200   02  FILLER                        PIC X(15)  VALUE             IC1144.2
015300            "TEST RESULT OF ".                                    IC1144.2
015400   02  TEST-ID                       PIC X(9).                    IC1144.2
015500   02  FILLER                        PIC X(4)   VALUE             IC1144.2
015600            " IN ".                                               IC1144.2
015700   02  FILLER                        PIC X(12)  VALUE             IC1144.2
015800     " HIGH       ".                                              IC1144.2
015900   02  FILLER                        PIC X(22)  VALUE             IC1144.2
016000            " LEVEL VALIDATION FOR ".                             IC1144.2
016100   02  FILLER                        PIC X(58)  VALUE             IC1144.2
016200     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IC1144.2
016300 01  CCVS-H-3.                                                    IC1144.2
016400     02  FILLER                      PIC X(34)  VALUE             IC1144.2
016500            " FOR OFFICIAL USE ONLY    ".                         IC1144.2
016600     02  FILLER                      PIC X(58)  VALUE             IC1144.2
016700     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IC1144.2
016800     02  FILLER                      PIC X(28)  VALUE             IC1144.2
016900            "  COPYRIGHT   1985 ".                                IC1144.2
017000 01  CCVS-E-1.                                                    IC1144.2
017100     02 FILLER                       PIC X(52)  VALUE SPACE.      IC1144.2
017200     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              IC1144.2
017300     02 ID-AGAIN                     PIC X(9).                    IC1144.2
017400     02 FILLER                       PIC X(45)  VALUE SPACES.     IC1144.2
017500 01  CCVS-E-2.                                                    IC1144.2
017600     02  FILLER                      PIC X(31)  VALUE SPACE.      IC1144.2
017700     02  FILLER                      PIC X(21)  VALUE SPACE.      IC1144.2
017800     02 CCVS-E-2-2.                                               IC1144.2
017900         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      IC1144.2
018000         03 FILLER                   PIC X      VALUE SPACE.      IC1144.2
018100         03 ENDER-DESC               PIC X(44)  VALUE             IC1144.2
018200            "ERRORS ENCOUNTERED".                                 IC1144.2
018300 01  CCVS-E-3.                                                    IC1144.2
018400     02  FILLER                      PIC X(22)  VALUE             IC1144.2
018500            " FOR OFFICIAL USE ONLY".                             IC1144.2
018600     02  FILLER                      PIC X(12)  VALUE SPACE.      IC1144.2
018700     02  FILLER                      PIC X(58)  VALUE             IC1144.2
018800     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IC1144.2
018900     02  FILLER                      PIC X(13)  VALUE SPACE.      IC1144.2
019000     02 FILLER                       PIC X(15)  VALUE             IC1144.2
019100             " COPYRIGHT 1985".                                   IC1144.2
019200 01  CCVS-E-4.                                                    IC1144.2
019300     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      IC1144.2
019400     02 FILLER                       PIC X(4)   VALUE " OF ".     IC1144.2
019500     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      IC1144.2
019600     02 FILLER                       PIC X(40)  VALUE             IC1144.2
019700      "  TESTS WERE EXECUTED SUCCESSFULLY".                       IC1144.2
019800 01  XXINFO.                                                      IC1144.2
019900     02 FILLER                       PIC X(19)  VALUE             IC1144.2
020000            "*** INFORMATION ***".                                IC1144.2
020100     02 INFO-TEXT.                                                IC1144.2
020200       04 FILLER                     PIC X(8)   VALUE SPACE.      IC1144.2
020300       04 XXCOMPUTED                 PIC X(20).                   IC1144.2
020400       04 FILLER                     PIC X(5)   VALUE SPACE.      IC1144.2
020500       04 XXCORRECT                  PIC X(20).                   IC1144.2
020600     02 INF-ANSI-REFERENCE           PIC X(48).                   IC1144.2
020700 01  HYPHEN-LINE.                                                 IC1144.2
020800     02 FILLER  PIC IS X VALUE IS SPACE.                          IC1144.2
020900     02 FILLER  PIC IS X(65)    VALUE IS "************************IC1144.2
021000-    "*****************************************".                 IC1144.2
021100     02 FILLER  PIC IS X(54)    VALUE IS "************************IC1144.2
021200-    "******************************".                            IC1144.2
021300 01  CCVS-PGM-ID                     PIC X(9)   VALUE             IC1144.2
021400     "IC114A".                                                    IC1144.2
021500 PROCEDURE DIVISION.                                              IC1144.2
021600 CCVS1 SECTION.                                                   IC1144.2
021700 OPEN-FILES.                                                      IC1144.2
021800     OPEN     OUTPUT PRINT-FILE.                                  IC1144.2
021900     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   IC1144.2
022000     MOVE    SPACE TO TEST-RESULTS.                               IC1144.2
022100     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             IC1144.2
022200     GO TO CCVS1-EXIT.                                            IC1144.2
022300 CLOSE-FILES.                                                     IC1144.2
022400     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   IC1144.2
022500 TERMINATE-CCVS.                                                  IC1144.2
022600*    EXIT PROGRAM.                                                IC1144.2
022700*TERMINATE-CALL.                                                  IC1144.2
022800     STOP     RUN.                                                IC1144.2
022900 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         IC1144.2
023000 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           IC1144.2
023100 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          IC1144.2
023200 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      IC1144.2
023300     MOVE "****TEST DELETED****" TO RE-MARK.                      IC1144.2
023400 PRINT-DETAIL.                                                    IC1144.2
023500     IF REC-CT NOT EQUAL TO ZERO                                  IC1144.2
023600             MOVE "." TO PARDOT-X                                 IC1144.2
023700             MOVE REC-CT TO DOTVALUE.                             IC1144.2
023800     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      IC1144.2
023900     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               IC1144.2
024000        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 IC1144.2
024100          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 IC1144.2
024200     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              IC1144.2
024300     MOVE SPACE TO CORRECT-X.                                     IC1144.2
024400     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         IC1144.2
024500     MOVE     SPACE TO RE-MARK.                                   IC1144.2
024600 HEAD-ROUTINE.                                                    IC1144.2
024700     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IC1144.2
024800     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IC1144.2
024900     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IC1144.2
025000     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IC1144.2
025100 COLUMN-NAMES-ROUTINE.                                            IC1144.2
025200     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IC1144.2
025300     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IC1144.2
025400     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        IC1144.2
025500 END-ROUTINE.                                                     IC1144.2
025600     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.IC1144.2
025700 END-RTN-EXIT.                                                    IC1144.2
025800     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IC1144.2
025900 END-ROUTINE-1.                                                   IC1144.2
026000      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      IC1144.2
026100      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               IC1144.2
026200      ADD PASS-COUNTER TO ERROR-HOLD.                             IC1144.2
026300*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   IC1144.2
026400      MOVE PASS-COUNTER TO CCVS-E-4-1.                            IC1144.2
026500      MOVE ERROR-HOLD TO CCVS-E-4-2.                              IC1144.2
026600      MOVE CCVS-E-4 TO CCVS-E-2-2.                                IC1144.2
026700      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           IC1144.2
026800  END-ROUTINE-12.                                                 IC1144.2
026900      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        IC1144.2
027000     IF       ERROR-COUNTER IS EQUAL TO ZERO                      IC1144.2
027100         MOVE "NO " TO ERROR-TOTAL                                IC1144.2
027200         ELSE                                                     IC1144.2
027300         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       IC1144.2
027400     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           IC1144.2
027500     PERFORM WRITE-LINE.                                          IC1144.2
027600 END-ROUTINE-13.                                                  IC1144.2
027700     IF DELETE-COUNTER IS EQUAL TO ZERO                           IC1144.2
027800         MOVE "NO " TO ERROR-TOTAL  ELSE                          IC1144.2
027900         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      IC1144.2
028000     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   IC1144.2
028100     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IC1144.2
028200      IF   INSPECT-COUNTER EQUAL TO ZERO                          IC1144.2
028300          MOVE "NO " TO ERROR-TOTAL                               IC1144.2
028400      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   IC1144.2
028500      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            IC1144.2
028600      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          IC1144.2
028700     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IC1144.2
028800 WRITE-LINE.                                                      IC1144.2
028900     ADD 1 TO RECORD-COUNT.                                       IC1144.2
029000*    IF RECORD-COUNT GREATER 50                                   IC1144.2
029100*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          IC1144.2
029200*        MOVE SPACE TO DUMMY-RECORD                               IC1144.2
029300*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  IC1144.2
029400*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             IC1144.2
029500*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     IC1144.2
029600*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          IC1144.2
029700*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          IC1144.2
029800*        MOVE ZERO TO RECORD-COUNT.                               IC1144.2
029900     PERFORM WRT-LN.                                              IC1144.2
030000 WRT-LN.                                                          IC1144.2
030100     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               IC1144.2
030200     MOVE SPACE TO DUMMY-RECORD.                                  IC1144.2
030300 BLANK-LINE-PRINT.                                                IC1144.2
030400     PERFORM WRT-LN.                                              IC1144.2
030500 FAIL-ROUTINE.                                                    IC1144.2
030600     IF   COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE. IC1144.2
030700     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.IC1144.2
030800     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IC1144.2
030900     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   IC1144.2
031000     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IC1144.2
031100     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IC1144.2
031200     GO TO  FAIL-ROUTINE-EX.                                      IC1144.2
031300 FAIL-ROUTINE-WRITE.                                              IC1144.2
031400     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         IC1144.2
031500     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 IC1144.2
031600     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. IC1144.2
031700     MOVE   SPACES TO COR-ANSI-REFERENCE.                         IC1144.2
031800 FAIL-ROUTINE-EX. EXIT.                                           IC1144.2
031900 BAIL-OUT.                                                        IC1144.2
032000     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   IC1144.2
032100     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           IC1144.2
032200 BAIL-OUT-WRITE.                                                  IC1144.2
032300     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  IC1144.2
032400     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IC1144.2
032500     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IC1144.2
032600     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IC1144.2
032700 BAIL-OUT-EX. EXIT.                                               IC1144.2
032800 CCVS1-EXIT.                                                      IC1144.2
032900     EXIT.                                                        IC1144.2
033000 LINK-TEST-10.                                                    IC1144.2
033100     MOVE 1 TO CALL-FLAG.                                         IC1144.2
033200*                                                                 IC1144.2
033300*         THIS TEST CALLS IC115 WHICH CREATES THE FILE SQ-FS3.    IC1144.2
033400*    THIS FILE IS A MASS STORAGE SEQUENTIAL FILE WITH 120         IC1144.2
033500*    CHARACTER RECORDS.  THERE ARE 649 RECORDS IN THE FILE.       IC1144.2
033600*                                                                 IC1144.2
033700     CALL "IC115A" USING GROUP-LINKAGE-VARIABLES                  IC1144.2
033800                        FILE-REC-SQ-FS3.                          IC1144.2
033900     IF COUNT-OF-RECORDS EQUAL TO 649                             IC1144.2
034000              GO TO LINK-PASS-10.                                 IC1144.2
034100 LINK-FAIL-10.                                                    IC1144.2
034200     PERFORM FAIL.                                                IC1144.2
034300     MOVE "FILE NOT CREATED IN IC115" TO RE-MARK.                 IC1144.2
034400     MOVE "RECS WRITTEN =" TO COMPUTED-A.                         IC1144.2
034500     GO TO LINK-WRITE-10.                                         IC1144.2
034600 LINK-PASS-10.                                                    IC1144.2
034700     PERFORM PASS.                                                IC1144.2
034800     MOVE "FILE CREATED, RECS =" TO COMPUTED-A.                   IC1144.2
034900 LINK-WRITE-10.                                                   IC1144.2
035000     MOVE "LINK-TEST-10" TO PAR-NAME.                             IC1144.2
035100     MOVE "CREATE FILE SQ-FS3" TO FEATURE.                        IC1144.2
035200     MOVE COUNT-OF-RECORDS TO CORRECT-18V0.                       IC1144.2
035300     PERFORM PRINT-DETAIL.                                        IC1144.2
035400 LINK-TEST-11.                                                    IC1144.2
035500     MOVE 2 TO CALL-FLAG.                                         IC1144.2
035600     MOVE ZERO TO COUNT-OF-RECORDS RECORDS-IN-ERROR               IC1144.2
035700                  ERROR-FLAG EOF-FLAG.                            IC1144.2
035800     CALL "IC115A" USING GROUP-LINKAGE-VARIABLES                  IC1144.2
035900                        FILE-REC-SQ-FS3.                          IC1144.2
036000     IF ERROR-FLAG EQUAL TO ZERO                                  IC1144.2
036100              GO TO LINK-PASS-11.                                 IC1144.2
036200     IF COUNT-OF-RECORDS GREATER THAN 649                         IC1144.2
036300              MOVE "MORE THAN 649 RECORDS" TO RE-MARK             IC1144.2
036400              GO TO LINK-FAIL-11.                                 IC1144.2
036500     MOVE "ERRORS IN READING SQ-FS3" TO RE-MARK.                  IC1144.2
036600 LINK-FAIL-11.                                                    IC1144.2
036700     MOVE "RECORDS-IN-ERROR =" TO COMPUTED-A.                     IC1144.2
036800     MOVE RECORDS-IN-ERROR TO CORRECT-18V0.                       IC1144.2
036900     GO TO LINK-WRITE-11.                                         IC1144.2
037000 LINK-PASS-11.                                                    IC1144.2
037100     MOVE "FILE VERIFIED RECS =" TO COMPUTED-A.                   IC1144.2
037200     MOVE COUNT-OF-RECORDS TO CORRECT-18V0.                       IC1144.2
037300     PERFORM PASS.                                                IC1144.2
037400 LINK-WRITE-11.                                                   IC1144.2
037500     MOVE "LINK-TEST-11" TO PAR-NAME.                             IC1144.2
037600     MOVE "VERIFY FILE SQ-FS3" TO FEATURE.                        IC1144.2
037700     PERFORM PRINT-DETAIL.                                        IC1144.2
037800 LINK-INIT-12.                                                    IC1144.2
037900     MOVE 3 TO CALL-FLAG.                                         IC1144.2
038000     MOVE ZERO TO COUNT-OF-RECORDS RECORDS-IN-ERROR               IC1144.2
038100                  ERROR-FLAG EOF-FLAG.                            IC1144.2
038200     CALL "IC115A" USING GROUP-LINKAGE-VARIABLES                  IC1144.2
038300                        FILE-REC-SQ-FS3.                          IC1144.2
038400*         CALL IC115 TO OPEN FILE SQ-FS3.                         IC1144.2
038500     MOVE 4 TO CALL-FLAG.                                         IC1144.2
038600 LINK-TEST-12.                                                    IC1144.2
038700     CALL "IC115A" USING GROUP-LINKAGE-VARIABLES                  IC1144.2
038800                        FILE-REC-SQ-FS3.                          IC1144.2
038900*                                                                 IC1144.2
039000*        THIS TEST REPEATEDLY CALLS IC115 TO READ THE FILE SQ-FS3.IC1144.2
039100*    THE CONTENTS OF EACH DATA RECORD IS CHECKED FOR VALID DATA.  IC1144.2
039200*                                                                 IC1144.2
039300     IF EOF-FLAG EQUAL TO 1                                       IC1144.2
039400              GO TO LINK-TEST-12-01.                              IC1144.2
039500     ADD 1 TO COUNT-OF-RECORDS.                                   IC1144.2
039600     IF COUNT-OF-RECORDS GREATER THAN 649                         IC1144.2
039700              GO TO LINK-FAIL-12-02.                              IC1144.2
039800     IF COUNT-OF-RECORDS NOT EQUAL TO XRECORD-NUMBER              IC1144.2
039900              GO TO LINK-FAIL-12-01.                              IC1144.2
040000     IF REELUNIT-NUMBER-GROUP NOT EQUAL TO "/0"                   IC1144.2
040100              GO TO LINK-FAIL-12-01.                              IC1144.2
040200     IF XFILE-NAME NOT EQUAL TO "SQ-FS3"                          IC1144.2
040300              GO TO LINK-FAIL-12-01.                              IC1144.2
040400     IF XRECORD-NAME NOT EQUAL TO "R1-F-G"                        IC1144.2
040500              GO TO LINK-FAIL-12-01.                              IC1144.2
040600     IF XLABEL-TYPE NOT EQUAL TO "S"                              IC1144.2
040700              GO TO LINK-FAIL-12-01.                              IC1144.2
040800     GO TO LINK-TEST-12.                                          IC1144.2
040900 LINK-FAIL-12-01.                                                 IC1144.2
041000     ADD 1 TO RECORDS-IN-ERROR.                                   IC1144.2
041100     MOVE 1 TO ERROR-FLAG.                                        IC1144.2
041200     GO TO LINK-TEST-12.                                          IC1144.2
041300 LINK-FAIL-12-02.                                                 IC1144.2
041400     MOVE "MORE THAN 649 RECORDS" TO RE-MARK.                     IC1144.2
041500     GO TO LINK-FAIL-12.                                          IC1144.2
041600 LINK-TEST-12-01.                                                 IC1144.2
041700     IF COUNT-OF-RECORDS LESS THAN 649                            IC1144.2
041800              GO TO LINK-FAIL-12-04.                              IC1144.2
041900     IF ERROR-FLAG EQUAL TO ZERO                                  IC1144.2
042000              GO TO LINK-PASS-12.                                 IC1144.2
042100 LINK-FAIL-12-03.                                                 IC1144.2
042200     MOVE "RECORDS-IN-ERROR =" TO COMPUTED-A.                     IC1144.2
042300     MOVE RECORDS-IN-ERROR TO CORRECT-18V0.                       IC1144.2
042400 LINK-FAIL-12.                                                    IC1144.2
042500     PERFORM FAIL.                                                IC1144.2
042600     GO TO LINK-WRITE-12.                                         IC1144.2
042700 LINK-FAIL-12-04.                                                 IC1144.2
042800     MOVE "UNEXPECTED EOF" TO RE-MARK.                            IC1144.2
042900     MOVE "RECORDS READ =" TO COMPUTED-A.                         IC1144.2
043000     MOVE COUNT-OF-RECORDS TO CORRECT-18V0.                       IC1144.2
043100     GO TO LINK-FAIL-12.                                          IC1144.2
043200 LINK-PASS-12.                                                    IC1144.2
043300     PERFORM PASS.                                                IC1144.2
043400 LINK-WRITE-12.                                                   IC1144.2
043500     MOVE "LINK-TEST-12" TO PAR-NAME.                             IC1144.2
043600     MOVE "READ IN SUBPRGM" TO FEATURE.                           IC1144.2
043700     PERFORM PRINT-DETAIL.                                        IC1144.2
043800 LINK-CLOSE-12.                                                   IC1144.2
043900     MOVE 5 TO CALL-FLAG.                                         IC1144.2
044000     CALL "IC115A" USING GROUP-LINKAGE-VARIABLES                  IC1144.2
044100                        FILE-REC-SQ-FS3.                          IC1144.2
044200 TERMINATE-ROUTINE.                                               IC1144.2
044300     EXIT.                                                        IC1144.2
044400*FILE-DUMP SECTION.                                               IC1144.2
044500*FILE-DUMP-INIT.                                                  IC1144.2
044600*    MOVE 3 TO CALL-FLAG.                                         IC1144.2
044700*    MOVE ZERO TO EOF-FLAG COUNT-OF-RECORDS.                      IC1144.2
044800*    CALL "IC115A" USING GROUP-LINKAGE-VARIABLES                  IC1144.2
044900*                       FILE-REC-SQ-FS3.                          IC1144.2
045000*    MOVE 4 TO CALL-FLAG.                                         IC1144.2
045100*FILE-3-DUMP.                                                     IC1144.2
045200*    CALL "IC115A" USING GROUP-LINKAGE-VARIABLES                  IC1144.2
045300*                       FILE-REC-SQ-FS3.                          IC1144.2
045400*    IF EOF-FLAG EQUAL TO 1                                       IC1144.2
045500*             GO TO FILE-3-DUMP-END.                              IC1144.2
045600*    ADD 1 TO COUNT-OF-RECORDS.                                   IC1144.2
045700*    IF COUNT-OF-RECORDS EQUAL TO 650                             IC1144.2
045800*             PERFORM BLANK-LINE-PRINT 5 TIMES.                   IC1144.2
045900*    MOVE FILE-REC-SQ-FS3 TO DUMMY-RECORD.                        IC1144.2
046000*    PERFORM WRITE-LINE.                                          IC1144.2
046100*    IF COUNT-OF-RECORDS LESS THAN 669                            IC1144.2
046200*             GO TO FILE-3-DUMP.                                  IC1144.2
046300*FILE-3-DUMP-END.                                                 IC1144.2
046400*    MOVE 5 TO CALL-FLAG.                                         IC1144.2
046500*    CALL "IC115A" USING GROUP-LINKAGE-VARIABLES                  IC1144.2
046600*                       FILE-REC-SQ-FS3.                          IC1144.2
046700*FILE-3-DUMP-EXIT.                                                IC1144.2
046800*    EXIT.                                                        IC1144.2
046900 CCVS-EXIT SECTION.                                               IC1144.2
047000 CCVS-999999.                                                     IC1144.2
047100     GO TO CLOSE-FILES.                                           IC1144.2
      *END-OF,IC114A                                                                  
