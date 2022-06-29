      *HEADER,COBOL,OBIC1A,SUBRTN,OBIC3A                                              
000100 IDENTIFICATION DIVISION.                                         OBIC34.2
000200 PROGRAM-ID.                                                      OBIC34.2
000300     OBIC3A.                                                      OBIC34.2
000400****************************************************************  OBIC34.2
000500*                                                              *  OBIC34.2
000600*    VALIDATION FOR:-                                          *  OBIC34.2
000700*                                                              *  OBIC34.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".OBIC34.2
000900*                                                              *  OBIC34.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".OBIC34.2
001100*                                                              *  OBIC34.2
001200****************************************************************  OBIC34.2
001300*                                                              *  OBIC34.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  OBIC34.2
001500*                                                              *  OBIC34.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  OBIC34.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  OBIC34.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  OBIC34.2
001900*                                                              *  OBIC34.2
002000****************************************************************  OBIC34.2
002100*                                                                 OBIC34.2
002200*        THE SUBPROGRAM IC220 PRINTS THE RESULTS FOR THE TESTING  OBIC34.2
002300*    OF A SEGMENTED LEVEL 1 SORT PROGRAM AS A SUBPROGRAM.  IT IS  OBIC34.2
002400*    CALLED BY THE MAIN PROGRAM IC218 AND THE SUBPROGRAM IC219.   OBIC34.2
002500*    THE LINKAGE VARIABLE PRINT-FLAG INDICATES WHETHER THE        OBIC34.2
002600*    HEADING (FLAG=1), FOOTING (FLAG=3), OR A REPORT LINE (FLAG=2)OBIC34.2
002700*    SHOULD BE PRINTED.                                           OBIC34.2
002800*                                                                 OBIC34.2
002900******************************************************************OBIC34.2
003000 ENVIRONMENT DIVISION.                                            OBIC34.2
003100 CONFIGURATION SECTION.                                           OBIC34.2
003200 SOURCE-COMPUTER.                                                 OBIC34.2
003300     XXXXX082.                                                    OBIC34.2
003400 OBJECT-COMPUTER.                                                 OBIC34.2
003500     XXXXX083.                                                    OBIC34.2
003600 INPUT-OUTPUT SECTION.                                            OBIC34.2
003700 FILE-CONTROL.                                                    OBIC34.2
003800     SELECT PRINT-FILE ASSIGN TO                                  OBIC34.2
003900     XXXXX055.                                                    OBIC34.2
004000 DATA DIVISION.                                                   OBIC34.2
004100 FILE SECTION.                                                    OBIC34.2
004200 FD  PRINT-FILE.                                                  OBIC34.2
004300 01  PRINT-REC PICTURE X(120).                                    OBIC34.2
004400 01  DUMMY-RECORD PICTURE X(120).                                 OBIC34.2
004500 WORKING-STORAGE SECTION.                                         OBIC34.2
004600 01  TEST-RESULTS.                                                OBIC34.2
004700     02 FILLER                   PIC X      VALUE SPACE.          OBIC34.2
004800     02 FEATURE                  PIC X(20)  VALUE SPACE.          OBIC34.2
004900     02 FILLER                   PIC X      VALUE SPACE.          OBIC34.2
005000     02 P-OR-F                   PIC X(5)   VALUE SPACE.          OBIC34.2
005100     02 FILLER                   PIC X      VALUE SPACE.          OBIC34.2
005200     02  PAR-NAME.                                                OBIC34.2
005300       03 FILLER                 PIC X(19)  VALUE SPACE.          OBIC34.2
005400       03  PARDOT-X              PIC X      VALUE SPACE.          OBIC34.2
005500       03 DOTVALUE               PIC 99     VALUE ZERO.           OBIC34.2
005600     02 FILLER                   PIC X(8)   VALUE SPACE.          OBIC34.2
005700     02 RE-MARK                  PIC X(61).                       OBIC34.2
005800 01  TEST-COMPUTED.                                               OBIC34.2
005900     02 FILLER                   PIC X(30)  VALUE SPACE.          OBIC34.2
006000     02 FILLER                   PIC X(17)  VALUE                 OBIC34.2
006100            "       COMPUTED=".                                   OBIC34.2
006200     02 COMPUTED-X.                                               OBIC34.2
006300     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          OBIC34.2
006400     03 COMPUTED-N               REDEFINES COMPUTED-A             OBIC34.2
006500                                 PIC -9(9).9(9).                  OBIC34.2
006600     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         OBIC34.2
006700     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     OBIC34.2
006800     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     OBIC34.2
006900     03       CM-18V0 REDEFINES COMPUTED-A.                       OBIC34.2
007000         04 COMPUTED-18V0                    PIC -9(18).          OBIC34.2
007100         04 FILLER                           PIC X.               OBIC34.2
007200     03 FILLER PIC X(50) VALUE SPACE.                             OBIC34.2
007300 01  TEST-CORRECT.                                                OBIC34.2
007400     02 FILLER PIC X(30) VALUE SPACE.                             OBIC34.2
007500     02 FILLER PIC X(17) VALUE "       CORRECT =".                OBIC34.2
007600     02 CORRECT-X.                                                OBIC34.2
007700     03 CORRECT-A                  PIC X(20) VALUE SPACE.         OBIC34.2
007800     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      OBIC34.2
007900     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         OBIC34.2
008000     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     OBIC34.2
008100     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     OBIC34.2
008200     03      CR-18V0 REDEFINES CORRECT-A.                         OBIC34.2
008300         04 CORRECT-18V0                     PIC -9(18).          OBIC34.2
008400         04 FILLER                           PIC X.               OBIC34.2
008500     03 FILLER PIC X(2) VALUE SPACE.                              OBIC34.2
008600     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     OBIC34.2
008700 01  CCVS-C-1.                                                    OBIC34.2
008800     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAOBIC34.2
008900-    "SS  PARAGRAPH-NAME                                          OBIC34.2
009000-    "       REMARKS".                                            OBIC34.2
009100     02 FILLER                     PIC X(20)    VALUE SPACE.      OBIC34.2
009200 01  CCVS-C-2.                                                    OBIC34.2
009300     02 FILLER                     PIC X        VALUE SPACE.      OBIC34.2
009400     02 FILLER                     PIC X(6)     VALUE "TESTED".   OBIC34.2
009500     02 FILLER                     PIC X(15)    VALUE SPACE.      OBIC34.2
009600     02 FILLER                     PIC X(4)     VALUE "FAIL".     OBIC34.2
009700     02 FILLER                     PIC X(94)    VALUE SPACE.      OBIC34.2
009800 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       OBIC34.2
009900 01  REC-CT                        PIC 99       VALUE ZERO.       OBIC34.2
010000 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       OBIC34.2
010100 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       OBIC34.2
010200 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       OBIC34.2
010300 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       OBIC34.2
010400 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       OBIC34.2
010500 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       OBIC34.2
010600 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      OBIC34.2
010700 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       OBIC34.2
010800 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     OBIC34.2
010900 01  CCVS-H-1.                                                    OBIC34.2
011000     02  FILLER                    PIC X(39)    VALUE SPACES.     OBIC34.2
011100     02  FILLER                    PIC X(42)    VALUE             OBIC34.2
011200     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 OBIC34.2
011300     02  FILLER                    PIC X(39)    VALUE SPACES.     OBIC34.2
011400 01  CCVS-H-2A.                                                   OBIC34.2
011500   02  FILLER                        PIC X(40)  VALUE SPACE.      OBIC34.2
011600   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  OBIC34.2
011700   02  FILLER                        PIC XXXX   VALUE             OBIC34.2
011800     "4.2 ".                                                      OBIC34.2
011900   02  FILLER                        PIC X(28)  VALUE             OBIC34.2
012000            " COPY - NOT FOR DISTRIBUTION".                       OBIC34.2
012100   02  FILLER                        PIC X(41)  VALUE SPACE.      OBIC34.2
012200                                                                  OBIC34.2
012300 01  CCVS-H-2B.                                                   OBIC34.2
012400   02  FILLER                        PIC X(15)  VALUE             OBIC34.2
012500            "TEST RESULT OF ".                                    OBIC34.2
012600   02  TEST-ID                       PIC X(9).                    OBIC34.2
012700   02  FILLER                        PIC X(4)   VALUE             OBIC34.2
012800            " IN ".                                               OBIC34.2
012900   02  FILLER                        PIC X(12)  VALUE             OBIC34.2
013000     " HIGH       ".                                              OBIC34.2
013100   02  FILLER                        PIC X(22)  VALUE             OBIC34.2
013200            " LEVEL VALIDATION FOR ".                             OBIC34.2
013300   02  FILLER                        PIC X(58)  VALUE             OBIC34.2
013400     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".OBIC34.2
013500 01  CCVS-H-3.                                                    OBIC34.2
013600     02  FILLER                      PIC X(34)  VALUE             OBIC34.2
013700            " FOR OFFICIAL USE ONLY    ".                         OBIC34.2
013800     02  FILLER                      PIC X(58)  VALUE             OBIC34.2
013900     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".OBIC34.2
014000     02  FILLER                      PIC X(28)  VALUE             OBIC34.2
014100            "  COPYRIGHT   1985 ".                                OBIC34.2
014200 01  CCVS-E-1.                                                    OBIC34.2
014300     02 FILLER                       PIC X(52)  VALUE SPACE.      OBIC34.2
014400     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              OBIC34.2
014500     02 ID-AGAIN                     PIC X(9).                    OBIC34.2
014600     02 FILLER                       PIC X(45)  VALUE SPACES.     OBIC34.2
014700 01  CCVS-E-2.                                                    OBIC34.2
014800     02  FILLER                      PIC X(31)  VALUE SPACE.      OBIC34.2
014900     02  FILLER                      PIC X(21)  VALUE SPACE.      OBIC34.2
015000     02 CCVS-E-2-2.                                               OBIC34.2
015100         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      OBIC34.2
015200         03 FILLER                   PIC X      VALUE SPACE.      OBIC34.2
015300         03 ENDER-DESC               PIC X(44)  VALUE             OBIC34.2
015400            "ERRORS ENCOUNTERED".                                 OBIC34.2
015500 01  CCVS-E-3.                                                    OBIC34.2
015600     02  FILLER                      PIC X(22)  VALUE             OBIC34.2
015700            " FOR OFFICIAL USE ONLY".                             OBIC34.2
015800     02  FILLER                      PIC X(12)  VALUE SPACE.      OBIC34.2
015900     02  FILLER                      PIC X(58)  VALUE             OBIC34.2
016000     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".OBIC34.2
016100     02  FILLER                      PIC X(13)  VALUE SPACE.      OBIC34.2
016200     02 FILLER                       PIC X(15)  VALUE             OBIC34.2
016300             " COPYRIGHT 1985".                                   OBIC34.2
016400 01  CCVS-E-4.                                                    OBIC34.2
016500     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      OBIC34.2
016600     02 FILLER                       PIC X(4)   VALUE " OF ".     OBIC34.2
016700     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      OBIC34.2
016800     02 FILLER                       PIC X(40)  VALUE             OBIC34.2
016900      "  TESTS WERE EXECUTED SUCCESSFULLY".                       OBIC34.2
017000 01  XXINFO.                                                      OBIC34.2
017100     02 FILLER                       PIC X(19)  VALUE             OBIC34.2
017200            "*** INFORMATION ***".                                OBIC34.2
017300     02 INFO-TEXT.                                                OBIC34.2
017400       04 FILLER                     PIC X(8)   VALUE SPACE.      OBIC34.2
017500       04 XXCOMPUTED                 PIC X(20).                   OBIC34.2
017600       04 FILLER                     PIC X(5)   VALUE SPACE.      OBIC34.2
017700       04 XXCORRECT                  PIC X(20).                   OBIC34.2
017800     02 INF-ANSI-REFERENCE           PIC X(48).                   OBIC34.2
017900 01  HYPHEN-LINE.                                                 OBIC34.2
018000     02 FILLER  PIC IS X VALUE IS SPACE.                          OBIC34.2
018100     02 FILLER  PIC IS X(65)    VALUE IS "************************OBIC34.2
018200-    "*****************************************".                 OBIC34.2
018300     02 FILLER  PIC IS X(54)    VALUE IS "************************OBIC34.2
018400-    "******************************".                            OBIC34.2
018500 01  CCVS-PGM-ID                     PIC X(9)   VALUE             OBIC34.2
018600     "OBIC3A".                                                    OBIC34.2
018700 LINKAGE SECTION.                                                 OBIC34.2
018800 01  PRINT-LINE-VALUES.                                           OBIC34.2
018900     02  PASS-OR-FAIL        PICTURE X(5).                        OBIC34.2
019000     02  R-COUNT             PICTURE 99.                          OBIC34.2
019100     02  FEATURE-TESTED      PICTURE X(20).                       OBIC34.2
019200     02  COMPUTED-SORT-KEY   PICTURE X(20).                       OBIC34.2
019300     02  CORRECT-SORT-KEY    PICTURE X(20).                       OBIC34.2
019400     02  PARAGRAPH-NAME      PICTURE X(12).                       OBIC34.2
019500 01  PRINT-FLAG              PICTURE 9.                           OBIC34.2
019600 PROCEDURE DIVISION USING PRINT-LINE-VALUES  PRINT-FLAG.          OBIC34.2
019700 SECT-IC220-0001 SECTION.                                         OBIC34.2
019800 BOILER-PLATE.                                                    OBIC34.2
019900     GO TO CCVS1-EXIT.                                            OBIC34.2
020000 CLOSE-FILES.                                                     OBIC34.2
020100     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   OBIC34.2
020200 TERMINATE-CCVS.                                                  OBIC34.2
020300*    EXIT PROGRAM.                                                OBIC34.2
020400*TERMINATE-CALL.                                                  OBIC34.2
020500     STOP     RUN.                                                OBIC34.2
020600 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         OBIC34.2
020700 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           OBIC34.2
020800 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          OBIC34.2
020900 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      OBIC34.2
021000     MOVE "****TEST DELETED****" TO RE-MARK.                      OBIC34.2
021100 PRINT-DETAIL.                                                    OBIC34.2
021200     IF REC-CT NOT EQUAL TO ZERO                                  OBIC34.2
021300             MOVE "." TO PARDOT-X                                 OBIC34.2
021400             MOVE REC-CT TO DOTVALUE.                             OBIC34.2
021500     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      OBIC34.2
021600     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               OBIC34.2
021700        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 OBIC34.2
021800          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 OBIC34.2
021900     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              OBIC34.2
022000     MOVE SPACE TO CORRECT-X.                                     OBIC34.2
022100     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         OBIC34.2
022200     MOVE     SPACE TO RE-MARK.                                   OBIC34.2
022300 HEAD-ROUTINE.                                                    OBIC34.2
022400     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  OBIC34.2
022500     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  OBIC34.2
022600     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  OBIC34.2
022700     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  OBIC34.2
022800 COLUMN-NAMES-ROUTINE.                                            OBIC34.2
022900     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           OBIC34.2
023000     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   OBIC34.2
023100     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        OBIC34.2
023200 END-ROUTINE.                                                     OBIC34.2
023300     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.OBIC34.2
023400 END-RTN-EXIT.                                                    OBIC34.2
023500     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   OBIC34.2
023600 END-ROUTINE-1.                                                   OBIC34.2
023700      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      OBIC34.2
023800      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               OBIC34.2
023900      ADD PASS-COUNTER TO ERROR-HOLD.                             OBIC34.2
024000*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   OBIC34.2
024100      MOVE PASS-COUNTER TO CCVS-E-4-1.                            OBIC34.2
024200      MOVE ERROR-HOLD TO CCVS-E-4-2.                              OBIC34.2
024300      MOVE CCVS-E-4 TO CCVS-E-2-2.                                OBIC34.2
024400      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           OBIC34.2
024500  END-ROUTINE-12.                                                 OBIC34.2
024600      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        OBIC34.2
024700     IF       ERROR-COUNTER IS EQUAL TO ZERO                      OBIC34.2
024800         MOVE "NO " TO ERROR-TOTAL                                OBIC34.2
024900         ELSE                                                     OBIC34.2
025000         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       OBIC34.2
025100     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           OBIC34.2
025200     PERFORM WRITE-LINE.                                          OBIC34.2
025300 END-ROUTINE-13.                                                  OBIC34.2
025400     IF DELETE-COUNTER IS EQUAL TO ZERO                           OBIC34.2
025500         MOVE "NO " TO ERROR-TOTAL  ELSE                          OBIC34.2
025600         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      OBIC34.2
025700     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   OBIC34.2
025800     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           OBIC34.2
025900      IF   INSPECT-COUNTER EQUAL TO ZERO                          OBIC34.2
026000          MOVE "NO " TO ERROR-TOTAL                               OBIC34.2
026100      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   OBIC34.2
026200      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            OBIC34.2
026300      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          OBIC34.2
026400     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           OBIC34.2
026500 WRITE-LINE.                                                      OBIC34.2
026600     ADD 1 TO RECORD-COUNT.                                       OBIC34.2
026700*    IF RECORD-COUNT GREATER 50                                   OBIC34.2
026800*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          OBIC34.2
026900*        MOVE SPACE TO DUMMY-RECORD                               OBIC34.2
027000*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  OBIC34.2
027100*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             OBIC34.2
027200*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     OBIC34.2
027300*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          OBIC34.2
027400*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          OBIC34.2
027500*        MOVE ZERO TO RECORD-COUNT.                               OBIC34.2
027600     PERFORM WRT-LN.                                              OBIC34.2
027700 WRT-LN.                                                          OBIC34.2
027800     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               OBIC34.2
027900     MOVE SPACE TO DUMMY-RECORD.                                  OBIC34.2
028000 BLANK-LINE-PRINT.                                                OBIC34.2
028100     PERFORM WRT-LN.                                              OBIC34.2
028200 FAIL-ROUTINE.                                                    OBIC34.2
028300     IF   COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE. OBIC34.2
028400     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.OBIC34.2
028500     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 OBIC34.2
028600     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   OBIC34.2
028700     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   OBIC34.2
028800     MOVE   SPACES TO INF-ANSI-REFERENCE.                         OBIC34.2
028900     GO TO  FAIL-ROUTINE-EX.                                      OBIC34.2
029000 FAIL-ROUTINE-WRITE.                                              OBIC34.2
029100     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         OBIC34.2
029200     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 OBIC34.2
029300     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. OBIC34.2
029400     MOVE   SPACES TO COR-ANSI-REFERENCE.                         OBIC34.2
029500 FAIL-ROUTINE-EX. EXIT.                                           OBIC34.2
029600 BAIL-OUT.                                                        OBIC34.2
029700     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   OBIC34.2
029800     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           OBIC34.2
029900 BAIL-OUT-WRITE.                                                  OBIC34.2
030000     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  OBIC34.2
030100     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 OBIC34.2
030200     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   OBIC34.2
030300     MOVE   SPACES TO INF-ANSI-REFERENCE.                         OBIC34.2
030400 BAIL-OUT-EX. EXIT.                                               OBIC34.2
030500 CCVS1-EXIT.                                                      OBIC34.2
030600     EXIT.                                                        OBIC34.2
030700 SECT-IC220-0002 SECTION.                                         OBIC34.2
030800 BRANCH-STATEMENT.                                                OBIC34.2
030900     GO TO PRINT-HEADING  PROCESS-LINE  PRINT-FOOTING             OBIC34.2
031000         DEPENDING ON PRINT-FLAG.                                 OBIC34.2
031100     MOVE "ERROR IN PRINT-FLAG" TO DUMMY-RECORD.                  OBIC34.2
031200     PERFORM WRITE-LINE.                                          OBIC34.2
031300     GO TO IC220-EXIT.                                            OBIC34.2
031400 PRINT-HEADING.                                                   OBIC34.2
031500     MOVE 0 TO R-COUNT.                                           OBIC34.2
031600 OPEN-FILES.                                                      OBIC34.2
031700     OPEN     OUTPUT PRINT-FILE.                                  OBIC34.2
031800     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   OBIC34.2
031900     MOVE    SPACE TO TEST-RESULTS.                               OBIC34.2
032000     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             OBIC34.2
032100     GO TO IC220-EXIT.                                            OBIC34.2
032200 PRINT-FOOTING.                                                   OBIC34.2
032300     PERFORM CLOSE-FILES.                                         OBIC34.2
032400     GO TO IC220-EXIT.                                            OBIC34.2
032500 PROCESS-LINE.                                                    OBIC34.2
032600     IF PASS-OR-FAIL EQUAL TO "PASS "                             OBIC34.2
032700         PERFORM PASS                                             OBIC34.2
032800             ELSE PERFORM FAIL                                    OBIC34.2
032900                  MOVE COMPUTED-SORT-KEY TO COMPUTED-A            OBIC34.2
033000                  MOVE CORRECT-SORT-KEY TO CORRECT-A.             OBIC34.2
033100     MOVE R-COUNT TO REC-CT.                                      OBIC34.2
033200     MOVE FEATURE-TESTED TO FEATURE.                              OBIC34.2
033300     MOVE PARAGRAPH-NAME TO PAR-NAME.                             OBIC34.2
033400     PERFORM PRINT-DETAIL.                                        OBIC34.2
033500 IC220-EXIT.                                                      OBIC34.2
033600     EXIT PROGRAM.                                                OBIC34.2
      *END-OF,OBIC3A                                                                  
