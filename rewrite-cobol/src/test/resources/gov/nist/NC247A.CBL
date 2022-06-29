      *HEADER,COBOL,NC247A                                                            
000100 IDENTIFICATION DIVISION.                                         NC2474.2
000200 PROGRAM-ID.                                                      NC2474.2
000300     NC247A.                                                      NC2474.2
000400****************************************************************  NC2474.2
000500*                                                              *  NC2474.2
000600*    VALIDATION FOR:-                                          *  NC2474.2
000700*                                                              *  NC2474.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2474.2
000900*                                                              *  NC2474.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC2474.2
001100*                                                              *  NC2474.2
001200****************************************************************  NC2474.2
001300*                                                              *  NC2474.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  NC2474.2
001500*                                                              *  NC2474.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  NC2474.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  NC2474.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  NC2474.2
001900*                                                              *  NC2474.2
002000****************************************************************  NC2474.2
002100*                                                                 NC2474.2
002200*                                                                 NC2474.2
002300*    PROGRAM NC247A TESTS FORMAT2 OF THE "OCCURS" CLAUSE TO    *  NC2474.2
002400*    VERIFY THAT THE NUMBER OF TABLE OCCURRENCES CARIES        *  NC2474.2
002500*    ACCORDING TO THE CURRENT VALUE OF THE IDENTIDIER ON WHICH *  NC2474.2
002600*    IT DEPENDS.                                               *  NC2474.2
002700*                                                              *  NC2474.2
002800****************************************************************  NC2474.2
002900 ENVIRONMENT DIVISION.                                            NC2474.2
003000 CONFIGURATION SECTION.                                           NC2474.2
003100 SOURCE-COMPUTER.                                                 NC2474.2
003200     XXXXX082.                                                    NC2474.2
003300 OBJECT-COMPUTER.                                                 NC2474.2
003400     XXXXX083.                                                    NC2474.2
003500 INPUT-OUTPUT SECTION.                                            NC2474.2
003600 FILE-CONTROL.                                                    NC2474.2
003700     SELECT PRINT-FILE ASSIGN TO                                  NC2474.2
003800     XXXXX055.                                                    NC2474.2
003900 DATA DIVISION.                                                   NC2474.2
004000 FILE SECTION.                                                    NC2474.2
004100 FD  PRINT-FILE.                                                  NC2474.2
004200 01  PRINT-REC PICTURE X(120).                                    NC2474.2
004300 01  DUMMY-RECORD PICTURE X(120).                                 NC2474.2
004400 WORKING-STORAGE SECTION.                                         NC2474.2
004500 01  ODO-RECORD.                                                  NC2474.2
004600     02  FILLER PIC X(120).                                       NC2474.2
004700     02  GRP-ODO.                                                 NC2474.2
004800         03  DOI-DU-01V00 PIC 9.                                  NC2474.2
004900         03  ODO-XN-00009 PIC X(9).                               NC2474.2
005000         03  ODO-GRP-00009.                                       NC2474.2
005100         04  ODO-XN-00001-O009D OCCURS 0 TO 9 TIMES DEPENDING ON  NC2474.2
005200                 DOI-DU-01V00 ASCENDING KEY ODO-XN-00001-O009D    NC2474.2
005300                 INDEXED BY ODO-IX PIC X.                         NC2474.2
005400 01  NEW-RECORD.                                                  NC2474.2
005500     02  FILLER PIC X(120).                                       NC2474.2
005600     02  NEW-ODO.                                                 NC2474.2
005700         03  NEW-DU-01V00 PIC 9.                                  NC2474.2
005800         03  NEW-XN-00009 PIC X(9).                               NC2474.2
005900         03  NEW-GRP-00009.                                       NC2474.2
006000         04  NEW-XN-00001-O009D OCCURS 0 TO 9 TIMES DEPENDING ON  NC2474.2
006100                 NEW-DU-01V00 ASCENDING KEY NEW-XN-00001-O009D    NC2474.2
006200                 INDEXED BY NEW-IX PIC X.                         NC2474.2
006300 01  STATIC-VALUE.                                                NC2474.2
006400     02  FILLER PIC 9 VALUE 9.                                    NC2474.2
006500     02  FILLER PIC X(18) VALUE " ACTIVE: 123456789".             NC2474.2
006600 01  WRK-GRP-00019.                                               NC2474.2
006700     02  WRK-DU-01V00 PIC 9.                                      NC2474.2
006800     02  WRK-XN-00009-1 PIC X(9).                                 NC2474.2
006900     02  WRK-XN-00009-2 PIC X(9).                                 NC2474.2
007000 01  WRK-DU-05V00 PIC 9(5).                                       NC2474.2
007100 01  WRK-XN-00020 PIC X(20).                                      NC2474.2
007200 01  WRK-XN-00010 PIC X(10).                                      NC2474.2
007300 01  WRK-XN-00001 PIC X.                                          NC2474.2
007400 01  TEST-RESULTS.                                                NC2474.2
007500     02 FILLER                   PIC X      VALUE SPACE.          NC2474.2
007600     02 FEATURE                  PIC X(20)  VALUE SPACE.          NC2474.2
007700     02 FILLER                   PIC X      VALUE SPACE.          NC2474.2
007800     02 P-OR-F                   PIC X(5)   VALUE SPACE.          NC2474.2
007900     02 FILLER                   PIC X      VALUE SPACE.          NC2474.2
008000     02  PAR-NAME.                                                NC2474.2
008100       03 FILLER                 PIC X(19)  VALUE SPACE.          NC2474.2
008200       03  PARDOT-X              PIC X      VALUE SPACE.          NC2474.2
008300       03 DOTVALUE               PIC 99     VALUE ZERO.           NC2474.2
008400     02 FILLER                   PIC X(8)   VALUE SPACE.          NC2474.2
008500     02 RE-MARK                  PIC X(61).                       NC2474.2
008600 01  TEST-COMPUTED.                                               NC2474.2
008700     02 FILLER                   PIC X(30)  VALUE SPACE.          NC2474.2
008800     02 FILLER                   PIC X(17)  VALUE                 NC2474.2
008900            "       COMPUTED=".                                   NC2474.2
009000     02 COMPUTED-X.                                               NC2474.2
009100     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          NC2474.2
009200     03 COMPUTED-N               REDEFINES COMPUTED-A             NC2474.2
009300                                 PIC -9(9).9(9).                  NC2474.2
009400     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         NC2474.2
009500     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     NC2474.2
009600     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     NC2474.2
009700     03       CM-18V0 REDEFINES COMPUTED-A.                       NC2474.2
009800         04 COMPUTED-18V0                    PIC -9(18).          NC2474.2
009900         04 FILLER                           PIC X.               NC2474.2
010000     03 FILLER PIC X(50) VALUE SPACE.                             NC2474.2
010100 01  TEST-CORRECT.                                                NC2474.2
010200     02 FILLER PIC X(30) VALUE SPACE.                             NC2474.2
010300     02 FILLER PIC X(17) VALUE "       CORRECT =".                NC2474.2
010400     02 CORRECT-X.                                                NC2474.2
010500     03 CORRECT-A                  PIC X(20) VALUE SPACE.         NC2474.2
010600     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      NC2474.2
010700     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         NC2474.2
010800     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     NC2474.2
010900     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     NC2474.2
011000     03      CR-18V0 REDEFINES CORRECT-A.                         NC2474.2
011100         04 CORRECT-18V0                     PIC -9(18).          NC2474.2
011200         04 FILLER                           PIC X.               NC2474.2
011300     03 FILLER PIC X(2) VALUE SPACE.                              NC2474.2
011400     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     NC2474.2
011500 01  CCVS-C-1.                                                    NC2474.2
011600     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PANC2474.2
011700-    "SS  PARAGRAPH-NAME                                          NC2474.2
011800-    "       REMARKS".                                            NC2474.2
011900     02 FILLER                     PIC X(20)    VALUE SPACE.      NC2474.2
012000 01  CCVS-C-2.                                                    NC2474.2
012100     02 FILLER                     PIC X        VALUE SPACE.      NC2474.2
012200     02 FILLER                     PIC X(6)     VALUE "TESTED".   NC2474.2
012300     02 FILLER                     PIC X(15)    VALUE SPACE.      NC2474.2
012400     02 FILLER                     PIC X(4)     VALUE "FAIL".     NC2474.2
012500     02 FILLER                     PIC X(94)    VALUE SPACE.      NC2474.2
012600 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       NC2474.2
012700 01  REC-CT                        PIC 99       VALUE ZERO.       NC2474.2
012800 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       NC2474.2
012900 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       NC2474.2
013000 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       NC2474.2
013100 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       NC2474.2
013200 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       NC2474.2
013300 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       NC2474.2
013400 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      NC2474.2
013500 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       NC2474.2
013600 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     NC2474.2
013700 01  CCVS-H-1.                                                    NC2474.2
013800     02  FILLER                    PIC X(39)    VALUE SPACES.     NC2474.2
013900     02  FILLER                    PIC X(42)    VALUE             NC2474.2
014000     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 NC2474.2
014100     02  FILLER                    PIC X(39)    VALUE SPACES.     NC2474.2
014200 01  CCVS-H-2A.                                                   NC2474.2
014300   02  FILLER                        PIC X(40)  VALUE SPACE.      NC2474.2
014400   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  NC2474.2
014500   02  FILLER                        PIC XXXX   VALUE             NC2474.2
014600     "4.2 ".                                                      NC2474.2
014700   02  FILLER                        PIC X(28)  VALUE             NC2474.2
014800            " COPY - NOT FOR DISTRIBUTION".                       NC2474.2
014900   02  FILLER                        PIC X(41)  VALUE SPACE.      NC2474.2
015000                                                                  NC2474.2
015100 01  CCVS-H-2B.                                                   NC2474.2
015200   02  FILLER                        PIC X(15)  VALUE             NC2474.2
015300            "TEST RESULT OF ".                                    NC2474.2
015400   02  TEST-ID                       PIC X(9).                    NC2474.2
015500   02  FILLER                        PIC X(4)   VALUE             NC2474.2
015600            " IN ".                                               NC2474.2
015700   02  FILLER                        PIC X(12)  VALUE             NC2474.2
015800     " HIGH       ".                                              NC2474.2
015900   02  FILLER                        PIC X(22)  VALUE             NC2474.2
016000            " LEVEL VALIDATION FOR ".                             NC2474.2
016100   02  FILLER                        PIC X(58)  VALUE             NC2474.2
016200     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2474.2
016300 01  CCVS-H-3.                                                    NC2474.2
016400     02  FILLER                      PIC X(34)  VALUE             NC2474.2
016500            " FOR OFFICIAL USE ONLY    ".                         NC2474.2
016600     02  FILLER                      PIC X(58)  VALUE             NC2474.2
016700     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC2474.2
016800     02  FILLER                      PIC X(28)  VALUE             NC2474.2
016900            "  COPYRIGHT   1985 ".                                NC2474.2
017000 01  CCVS-E-1.                                                    NC2474.2
017100     02 FILLER                       PIC X(52)  VALUE SPACE.      NC2474.2
017200     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              NC2474.2
017300     02 ID-AGAIN                     PIC X(9).                    NC2474.2
017400     02 FILLER                       PIC X(45)  VALUE SPACES.     NC2474.2
017500 01  CCVS-E-2.                                                    NC2474.2
017600     02  FILLER                      PIC X(31)  VALUE SPACE.      NC2474.2
017700     02  FILLER                      PIC X(21)  VALUE SPACE.      NC2474.2
017800     02 CCVS-E-2-2.                                               NC2474.2
017900         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      NC2474.2
018000         03 FILLER                   PIC X      VALUE SPACE.      NC2474.2
018100         03 ENDER-DESC               PIC X(44)  VALUE             NC2474.2
018200            "ERRORS ENCOUNTERED".                                 NC2474.2
018300 01  CCVS-E-3.                                                    NC2474.2
018400     02  FILLER                      PIC X(22)  VALUE             NC2474.2
018500            " FOR OFFICIAL USE ONLY".                             NC2474.2
018600     02  FILLER                      PIC X(12)  VALUE SPACE.      NC2474.2
018700     02  FILLER                      PIC X(58)  VALUE             NC2474.2
018800     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2474.2
018900     02  FILLER                      PIC X(13)  VALUE SPACE.      NC2474.2
019000     02 FILLER                       PIC X(15)  VALUE             NC2474.2
019100             " COPYRIGHT 1985".                                   NC2474.2
019200 01  CCVS-E-4.                                                    NC2474.2
019300     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      NC2474.2
019400     02 FILLER                       PIC X(4)   VALUE " OF ".     NC2474.2
019500     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      NC2474.2
019600     02 FILLER                       PIC X(40)  VALUE             NC2474.2
019700      "  TESTS WERE EXECUTED SUCCESSFULLY".                       NC2474.2
019800 01  XXINFO.                                                      NC2474.2
019900     02 FILLER                       PIC X(19)  VALUE             NC2474.2
020000            "*** INFORMATION ***".                                NC2474.2
020100     02 INFO-TEXT.                                                NC2474.2
020200       04 FILLER                     PIC X(8)   VALUE SPACE.      NC2474.2
020300       04 XXCOMPUTED                 PIC X(20).                   NC2474.2
020400       04 FILLER                     PIC X(5)   VALUE SPACE.      NC2474.2
020500       04 XXCORRECT                  PIC X(20).                   NC2474.2
020600     02 INF-ANSI-REFERENCE           PIC X(48).                   NC2474.2
020700 01  HYPHEN-LINE.                                                 NC2474.2
020800     02 FILLER  PIC IS X VALUE IS SPACE.                          NC2474.2
020900     02 FILLER  PIC IS X(65)    VALUE IS "************************NC2474.2
021000-    "*****************************************".                 NC2474.2
021100     02 FILLER  PIC IS X(54)    VALUE IS "************************NC2474.2
021200-    "******************************".                            NC2474.2
021300 01  CCVS-PGM-ID                     PIC X(9)   VALUE             NC2474.2
021400     "NC247A".                                                    NC2474.2
021500 PROCEDURE DIVISION.                                              NC2474.2
021600 CCVS1 SECTION.                                                   NC2474.2
021700 OPEN-FILES.                                                      NC2474.2
021800     OPEN     OUTPUT PRINT-FILE.                                  NC2474.2
021900     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   NC2474.2
022000     MOVE    SPACE TO TEST-RESULTS.                               NC2474.2
022100     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             NC2474.2
022200     GO TO CCVS1-EXIT.                                            NC2474.2
022300 CLOSE-FILES.                                                     NC2474.2
022400     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   NC2474.2
022500 TERMINATE-CCVS.                                                  NC2474.2
022600*    EXIT PROGRAM.                                                NC2474.2
022700*TERMINATE-CALL.                                                  NC2474.2
022800     STOP     RUN.                                                NC2474.2
022900 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         NC2474.2
023000 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           NC2474.2
023100 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          NC2474.2
023200 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      NC2474.2
023300     MOVE "****TEST DELETED****" TO RE-MARK.                      NC2474.2
023400 PRINT-DETAIL.                                                    NC2474.2
023500     IF REC-CT NOT EQUAL TO ZERO                                  NC2474.2
023600             MOVE "." TO PARDOT-X                                 NC2474.2
023700             MOVE REC-CT TO DOTVALUE.                             NC2474.2
023800     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      NC2474.2
023900     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               NC2474.2
024000        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 NC2474.2
024100          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 NC2474.2
024200     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              NC2474.2
024300     MOVE SPACE TO CORRECT-X.                                     NC2474.2
024400     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         NC2474.2
024500     MOVE     SPACE TO RE-MARK.                                   NC2474.2
024600 HEAD-ROUTINE.                                                    NC2474.2
024700     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC2474.2
024800     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC2474.2
024900     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC2474.2
025000     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC2474.2
025100 COLUMN-NAMES-ROUTINE.                                            NC2474.2
025200     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2474.2
025300     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2474.2
025400     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        NC2474.2
025500 END-ROUTINE.                                                     NC2474.2
025600     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.NC2474.2
025700 END-RTN-EXIT.                                                    NC2474.2
025800     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2474.2
025900 END-ROUTINE-1.                                                   NC2474.2
026000      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      NC2474.2
026100      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               NC2474.2
026200      ADD PASS-COUNTER TO ERROR-HOLD.                             NC2474.2
026300*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   NC2474.2
026400      MOVE PASS-COUNTER TO CCVS-E-4-1.                            NC2474.2
026500      MOVE ERROR-HOLD TO CCVS-E-4-2.                              NC2474.2
026600      MOVE CCVS-E-4 TO CCVS-E-2-2.                                NC2474.2
026700      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           NC2474.2
026800  END-ROUTINE-12.                                                 NC2474.2
026900      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        NC2474.2
027000     IF       ERROR-COUNTER IS EQUAL TO ZERO                      NC2474.2
027100         MOVE "NO " TO ERROR-TOTAL                                NC2474.2
027200         ELSE                                                     NC2474.2
027300         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       NC2474.2
027400     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           NC2474.2
027500     PERFORM WRITE-LINE.                                          NC2474.2
027600 END-ROUTINE-13.                                                  NC2474.2
027700     IF DELETE-COUNTER IS EQUAL TO ZERO                           NC2474.2
027800         MOVE "NO " TO ERROR-TOTAL  ELSE                          NC2474.2
027900         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      NC2474.2
028000     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   NC2474.2
028100     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2474.2
028200      IF   INSPECT-COUNTER EQUAL TO ZERO                          NC2474.2
028300          MOVE "NO " TO ERROR-TOTAL                               NC2474.2
028400      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   NC2474.2
028500      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            NC2474.2
028600      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          NC2474.2
028700     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2474.2
028800 WRITE-LINE.                                                      NC2474.2
028900     ADD 1 TO RECORD-COUNT.                                       NC2474.2
029000*    IF RECORD-COUNT GREATER 50                                   NC2474.2
029100*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          NC2474.2
029200*        MOVE SPACE TO DUMMY-RECORD                               NC2474.2
029300*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  NC2474.2
029400*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             NC2474.2
029500*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     NC2474.2
029600*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          NC2474.2
029700*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          NC2474.2
029800*        MOVE ZERO TO RECORD-COUNT.                               NC2474.2
029900     PERFORM WRT-LN.                                              NC2474.2
030000 WRT-LN.                                                          NC2474.2
030100     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               NC2474.2
030200     MOVE SPACE TO DUMMY-RECORD.                                  NC2474.2
030300 BLANK-LINE-PRINT.                                                NC2474.2
030400     PERFORM WRT-LN.                                              NC2474.2
030500 FAIL-ROUTINE.                                                    NC2474.2
030600     IF   COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE. NC2474.2
030700     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.NC2474.2
030800     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC2474.2
030900     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   NC2474.2
031000     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2474.2
031100     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC2474.2
031200     GO TO  FAIL-ROUTINE-EX.                                      NC2474.2
031300 FAIL-ROUTINE-WRITE.                                              NC2474.2
031400     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         NC2474.2
031500     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 NC2474.2
031600     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. NC2474.2
031700     MOVE   SPACES TO COR-ANSI-REFERENCE.                         NC2474.2
031800 FAIL-ROUTINE-EX. EXIT.                                           NC2474.2
031900 BAIL-OUT.                                                        NC2474.2
032000     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   NC2474.2
032100     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           NC2474.2
032200 BAIL-OUT-WRITE.                                                  NC2474.2
032300     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  NC2474.2
032400     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC2474.2
032500     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2474.2
032600     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC2474.2
032700 BAIL-OUT-EX. EXIT.                                               NC2474.2
032800 CCVS1-EXIT.                                                      NC2474.2
032900     EXIT.                                                        NC2474.2
033000 SECT-NC247A-001 SECTION.                                         NC2474.2
033100 INIT-WRK-AREA.                                                   NC2474.2
033200     MOVE STATIC-VALUE TO WRK-GRP-00019.                          NC2474.2
033300     MOVE 9 TO DOI-DU-01V00.                                      NC2474.2
033400     MOVE " ACTIVE: " TO ODO-XN-00009.                            NC2474.2
033500     MOVE "1" TO ODO-XN-00001-O009D (1).                          NC2474.2
033600     MOVE "2" TO ODO-XN-00001-O009D (2).                          NC2474.2
033700     MOVE "3" TO ODO-XN-00001-O009D (3).                          NC2474.2
033800     MOVE "4" TO ODO-XN-00001-O009D (4).                          NC2474.2
033900     MOVE "5" TO ODO-XN-00001-O009D (5).                          NC2474.2
034000     MOVE "6" TO ODO-XN-00001-O009D (6).                          NC2474.2
034100     MOVE "7" TO ODO-XN-00001-O009D (7).                          NC2474.2
034200     MOVE "8" TO ODO-XN-00001-O009D (8).                          NC2474.2
034300     MOVE "9" TO ODO-XN-00001-O009D (9).                          NC2474.2
034400*                                                                 NC2474.2
034500 IF-INIT-GF-1.                                                    NC2474.2
034600     MOVE "IF-TEST-GF-1" TO PAR-NAME.                             NC2474.2
034700     MOVE "VI-26 5.8" TO ANSI-REFERENCE.                          NC2474.2
034800     MOVE "OCCURS DEPENDING ON" TO FEATURE.                       NC2474.2
034900     MOVE STATIC-VALUE TO RE-MARK.                                NC2474.2
035000 IF-TEST-GF-1.                                                    NC2474.2
035100     IF STATIC-VALUE IS EQUAL TO GRP-ODO                          NC2474.2
035200         PERFORM PASS                                             NC2474.2
035300         GO TO IF-WRITE-GF-1                                      NC2474.2
035400     ELSE                                                         NC2474.2
035500         GO TO IF-FAIL-GF-1.                                      NC2474.2
035600 IF-DELETE-GF-1.                                                  NC2474.2
035700     PERFORM DE-LETE.                                             NC2474.2
035800     GO TO IF-WRITE-GF-1.                                         NC2474.2
035900 IF-FAIL-GF-1.                                                    NC2474.2
036000     PERFORM FAIL                                                 NC2474.2
036100     MOVE "CONDITION WAS EQUAL" TO CORRECT-A                      NC2474.2
036200     MOVE "CONDITION NOT EQUAL" TO COMPUTED-A.                    NC2474.2
036300 IF-WRITE-GF-1.                                                   NC2474.2
036400     PERFORM PRINT-DETAIL.                                        NC2474.2
036500*                                                                 NC2474.2
036600 IF-INIT-GF-2.                                                    NC2474.2
036700     MOVE WRK-GRP-00019 TO RE-MARK.                               NC2474.2
036800     MOVE "IF-TEST-GF-2" TO PAR-NAME.                             NC2474.2
036900     MOVE "VI-26 5.8" TO ANSI-REFERENCE.                          NC2474.2
037000     PERFORM INIT-WRK-AREA.                                       NC2474.2
037100     MOVE 3 TO WRK-DU-01V00  DOI-DU-01V00                         NC2474.2
037200     MOVE "123      " TO WRK-XN-00009-2.                          NC2474.2
037300 IF-TEST-GF-2.                                                    NC2474.2
037400     IF GRP-ODO IS EQUAL TO WRK-GRP-00019                         NC2474.2
037500         PERFORM PASS                                             NC2474.2
037600         GO TO IF-WRITE-GF-2                                      NC2474.2
037700     ELSE                                                         NC2474.2
037800         GO TO IF-FAIL-GF-2.                                      NC2474.2
037900 IF-DELETE-GF-2.                                                  NC2474.2
038000     PERFORM DE-LETE.                                             NC2474.2
038100     GO TO IF-WRITE-GF-2.                                         NC2474.2
038200 IF-FAIL-GF-2.                                                    NC2474.2
038300     PERFORM FAIL                                                 NC2474.2
038400     MOVE "CONDITION WAS EQUAL" TO CORRECT-A                      NC2474.2
038500     MOVE "CONDITION NOT EQUAL" TO COMPUTED-A.                    NC2474.2
038600 IF-WRITE-GF-2.                                                   NC2474.2
038700     PERFORM PRINT-DETAIL.                                        NC2474.2
038800*                                                                 NC2474.2
038900 INS-INIT-F1-1.                                                   NC2474.2
039000     MOVE "INS-TEST-F1-1" TO PAR-NAME.                            NC2474.2
039100     MOVE "VI-26 5.8" TO ANSI-REFERENCE.                          NC2474.2
039200     MOVE STATIC-VALUE TO RE-MARK.                                NC2474.2
039300     PERFORM INIT-WRK-AREA.                                       NC2474.2
039400     MOVE 0 TO WRK-DU-05V00.                                      NC2474.2
039500 INS-TEST-F1-1.                                                   NC2474.2
039600     INSPECT ODO-GRP-00009 TALLYING WRK-DU-05V00 FOR ALL "7".     NC2474.2
039700     IF WRK-DU-05V00 IS EQUAL TO 1                                NC2474.2
039800         PERFORM PASS                                             NC2474.2
039900         GO TO INS-WRITE-F1-1                                     NC2474.2
040000     ELSE                                                         NC2474.2
040100         GO TO INS-FAIL-F1-1.                                     NC2474.2
040200 INS-DELETE-F1-1.                                                 NC2474.2
040300     PERFORM DE-LETE.                                             NC2474.2
040400     GO TO INS-WRITE-F1-1.                                        NC2474.2
040500 INS-FAIL-F1-1.                                                   NC2474.2
040600     PERFORM FAIL                                                 NC2474.2
040700     MOVE 1 TO  CORRECT-18V0                                      NC2474.2
040800     MOVE WRK-DU-05V00 TO COMPUTED-18V0.                          NC2474.2
040900 INS-WRITE-F1-1.                                                  NC2474.2
041000     PERFORM PRINT-DETAIL.                                        NC2474.2
041100*                                                                 NC2474.2
041200 INS-INIT-F1-2.                                                   NC2474.2
041300     MOVE "INS-TEST-F1-2" TO PAR-NAME.                            NC2474.2
041400     MOVE "VI-26 5.8" TO ANSI-REFERENCE.                          NC2474.2
041500     PERFORM INIT-WRK-AREA.                                       NC2474.2
041600     MOVE 3 TO DOI-DU-01V00  WRK-DU-01V00 WRK-DU-05V00.           NC2474.2
041700     MOVE "123      " TO WRK-XN-00009-2.                          NC2474.2
041800     MOVE 0 TO WRK-DU-05V00.                                      NC2474.2
041900     MOVE WRK-GRP-00019 TO RE-MARK.                               NC2474.2
042000 INS-TEST-F1-2.                                                   NC2474.2
042100     INSPECT ODO-GRP-00009 TALLYING WRK-DU-05V00 FOR ALL "7".     NC2474.2
042200     IF WRK-DU-05V00 IS EQUAL TO 0                                NC2474.2
042300         PERFORM PASS                                             NC2474.2
042400         GO TO INS-WRITE-F1-2                                     NC2474.2
042500     ELSE                                                         NC2474.2
042600         GO TO INS-FAIL-F1-2.                                     NC2474.2
042700 INS-DELETE-F1-2.                                                 NC2474.2
042800     PERFORM DE-LETE.                                             NC2474.2
042900     GO TO INS-WRITE-F1-2.                                        NC2474.2
043000 INS-FAIL-F1-2.                                                   NC2474.2
043100     PERFORM FAIL                                                 NC2474.2
043200     MOVE 0 TO CORRECT-18V0                                       NC2474.2
043300     MOVE WRK-DU-05V00 TO COMPUTED-18V0.                          NC2474.2
043400 INS-WRITE-F1-2.                                                  NC2474.2
043500     PERFORM PRINT-DETAIL.                                        NC2474.2
043600*                                                                 NC2474.2
043700 MOV-INIT-F1-1.                                                   NC2474.2
043800     MOVE "MOV-TEST-F1-1" TO PAR-NAME.                            NC2474.2
043900     MOVE "VI-26 5.8" TO ANSI-REFERENCE.                          NC2474.2
044000     MOVE "FULL ODO + BASE SEGMENT SOURCE" TO RE-MARK.            NC2474.2
044100     PERFORM INIT-WRK-AREA.                                       NC2474.2
044200     MOVE SPACES TO WRK-GRP-00019.                                NC2474.2
044300     MOVE GRP-ODO TO WRK-GRP-00019.                               NC2474.2
044400 MOV-TEST-F1-1.                                                   NC2474.2
044500     IF WRK-GRP-00019 IS EQUAL TO STATIC-VALUE                    NC2474.2
044600         PERFORM PASS                                             NC2474.2
044700         GO TO MOV-WRITE-F1-1                                     NC2474.2
044800     ELSE                                                         NC2474.2
044900         GO TO MOV-FAIL-F1-1.                                     NC2474.2
045000 MOV-DELETE-F1-1.                                                 NC2474.2
045100     PERFORM DE-LETE.                                             NC2474.2
045200     GO TO MOV-WRITE-F1-1.                                        NC2474.2
045300 MOV-FAIL-F1-1.                                                   NC2474.2
045400     PERFORM FAIL                                                 NC2474.2
045500     MOVE STATIC-VALUE TO CORRECT-A                               NC2474.2
045600     MOVE WRK-GRP-00019 TO COMPUTED-A.                            NC2474.2
045700 MOV-WRITE-F1-1.                                                  NC2474.2
045800     PERFORM PRINT-DETAIL.                                        NC2474.2
045900*                                                                 NC2474.2
046000 MOV-INIT-F1-2.                                                   NC2474.2
046100     MOVE "MOV-TEST-F1-2" TO PAR-NAME.                            NC2474.2
046200     MOVE "VI-26 5.8" TO ANSI-REFERENCE.                          NC2474.2
046300     MOVE "PART ODO + BASE SEGMENT SOURCE" TO RE-MARK.            NC2474.2
046400     PERFORM INIT-WRK-AREA.                                       NC2474.2
046500     MOVE SPACES TO WRK-GRP-00019.                                NC2474.2
046600     MOVE 3 TO DOI-DU-01V00.                                      NC2474.2
046700     MOVE GRP-ODO TO WRK-GRP-00019.                               NC2474.2
046800 MOV-TEST-F1-2.                                                   NC2474.2
046900     IF WRK-GRP-00019 IS EQUAL TO "3 ACTIVE: 123      "           NC2474.2
047000         PERFORM PASS                                             NC2474.2
047100         GO TO MOV-WRITE-F1-2                                     NC2474.2
047200     ELSE                                                         NC2474.2
047300         GO TO MOV-FAIL-F1-2.                                     NC2474.2
047400 MOV-DELETE-F1-2.                                                 NC2474.2
047500     PERFORM DE-LETE.                                             NC2474.2
047600     GO TO MOV-WRITE-F1-2.                                        NC2474.2
047700 MOV-FAIL-F1-2.                                                   NC2474.2
047800     PERFORM FAIL                                                 NC2474.2
047900     MOVE "3 ACTIVE: 123" TO CORRECT-A                            NC2474.2
048000     MOVE WRK-GRP-00019 TO COMPUTED-A.                            NC2474.2
048100 MOV-WRITE-F1-2.                                                  NC2474.2
048200     PERFORM PRINT-DETAIL.                                        NC2474.2
048300*                                                                 NC2474.2
048400 MOV-INIT-F1-3.                                                   NC2474.2
048500     MOVE "MOV-TEST-F1-3" TO PAR-NAME.                            NC2474.2
048600     MOVE "VI-26 5.8" TO ANSI-REFERENCE.                          NC2474.2
048700     MOVE "FULL ODO +BASE SEG RECEIVING" TO RE-MARK.              NC2474.2
048800     MOVE 9 TO DOI-DU-01V00.                                      NC2474.2
048900     MOVE "F" TO  ODO-XN-00001-O009D (6).                         NC2474.2
049000     MOVE "A" TO  ODO-XN-00001-O009D (7).                         NC2474.2
049100     MOVE "I" TO  ODO-XN-00001-O009D (8).                         NC2474.2
049200     MOVE "L" TO  ODO-XN-00001-O009D (9).                         NC2474.2
049300     MOVE "3 ACTIVE: TEST PASS" TO GRP-ODO.                       NC2474.2
049400     MOVE 9 TO DOI-DU-01V00.                                      NC2474.2
049500 MOV-TEST-F1-3.                                                   NC2474.2
049600     IF GRP-ODO IS EQUAL TO "9 ACTIVE: TEST PASS"                 NC2474.2
049700         PERFORM PASS                                             NC2474.2
049800         GO TO MOV-WRITE-F1-3                                     NC2474.2
049900     ELSE                                                         NC2474.2
050000         GO TO MOV-FAIL-F1-3.                                     NC2474.2
050100 MOV-DELETE-F1-3.                                                 NC2474.2
050200     PERFORM DE-LETE.                                             NC2474.2
050300     GO TO MOV-WRITE-F1-3.                                        NC2474.2
050400 MOV-FAIL-F1-3.                                                   NC2474.2
050500     PERFORM FAIL                                                 NC2474.2
050600     MOVE "9 ACTIVE: TEST PASS" TO CORRECT-A                      NC2474.2
050700     MOVE GRP-ODO TO COMPUTED-A.                                  NC2474.2
050800 MOV-WRITE-F1-3.                                                  NC2474.2
050900     PERFORM PRINT-DETAIL.                                        NC2474.2
051000*                                                                 NC2474.2
051100 MOV-INIT-F1-4.                                                   NC2474.2
051200     MOVE "MOV-TEST-F1-4" TO PAR-NAME.                            NC2474.2
051300     MOVE "VI-26 5.8" TO ANSI-REFERENCE.                          NC2474.2
051400     MOVE "PART ODO + BASE SEG RECEIVING" TO RE-MARK.             NC2474.2
051500     MOVE 9 TO DOI-DU-01V00.                                      NC2474.2
051600     MOVE "F" TO ODO-XN-00001-O009D (6).                          NC2474.2
051700     MOVE "A" TO ODO-XN-00001-O009D (7).                          NC2474.2
051800     MOVE "I" TO ODO-XN-00001-O009D (8).                          NC2474.2
051900     MOVE "L" TO ODO-XN-00001-O009D (9).                          NC2474.2
052000     MOVE "9 ACTIVE: TEST PASS" TO GRP-ODO.                       NC2474.2
052100     MOVE 9 TO DOI-DU-01V00.                                      NC2474.2
052200     MOVE GRP-ODO TO WRK-GRP-00019.                               NC2474.2
052300     MOVE 5 TO WRK-DU-01V00.                                      NC2474.2
052400 MOV-TEST-F1-4.                                                   NC2474.2
052500     IF GRP-ODO IS EQUAL TO "9 ACTIVE: TEST PASS"                 NC2474.2
052600         PERFORM PASS                                             NC2474.2
052700         GO TO MOV-WRITE-F1-4                                     NC2474.2
052800     ELSE                                                         NC2474.2
052900         GO TO MOV-FAIL-F1-4.                                     NC2474.2
053000 MOV-DELETE-F1-4.                                                 NC2474.2
053100     PERFORM DE-LETE.                                             NC2474.2
053200     GO TO MOV-WRITE-F1-4.                                        NC2474.2
053300 MOV-FAIL-F1-4.                                                   NC2474.2
053400     PERFORM FAIL                                                 NC2474.2
053500     MOVE WRK-GRP-00019 TO COMPUTED-A                             NC2474.2
053600     MOVE "9 ACTIVE: TEST PASS" TO CORRECT-A.                     NC2474.2
053700 MOV-WRITE-F1-4.                                                  NC2474.2
053800     PERFORM PRINT-DETAIL.                                        NC2474.2
053900*                                                                 NC2474.2
054000 MOV-INIT-F1-5.                                                   NC2474.2
054100     MOVE   "MOV-TEST-F1-5" TO PAR-NAME.                          NC2474.2
054200     MOVE   "VI-26 5.8.3 SR5" TO ANSI-REFERENCE.                  NC2474.2
054300*    MOVE    9  TO DOI-DU-01V00.                                  NC2474.2
054400*    MOVE   "Z" TO ODO-XN-00001-O009D (1).                        NC2474.2
054500*    MOVE   "E" TO ODO-XN-00001-O009D (2).                        NC2474.2
054600*    MOVE   "R" TO ODO-XN-00001-O009D (3).                        NC2474.2
054700*    MOVE   "O" TO ODO-XN-00001-O009D (4).                        NC2474.2
054800*    MOVE   "*" TO WRK-XN-00001.                                  NC2474.2
054900*    MOVE    ZERO TO DOI-DU-01V00.                                NC2474.2
055000*    MOVE    ODO-XN-00001-O009D (1) TO WRK-XN-00001.              NC2474.2
055100*MOV-TEST-F1-5.                                                   NC2474.2
055200*    IF      WRK-XN-00001 = "*"                                   NC2474.2
055300*            PERFORM PASS                                         NC2474.2
055400*            GO TO MOV-WRITE-F1-5                                 NC2474.2
055500*    ELSE                                                         NC2474.2
055600*            GO TO MOV-FAIL-F1-5.                                 NC2474.2
055700 MOV-DELETE-F1-5.                                                 NC2474.2
055800     PERFORM DE-LETE.                                             NC2474.2
055900     GO TO   MOV-WRITE-F1-5.                                      NC2474.2
056000 MOV-FAIL-F1-5.                                                   NC2474.2
056100     MOVE    WRK-XN-00001 TO COMPUTED-A                           NC2474.2
056200     MOVE   "*"           TO CORRECT-A                            NC2474.2
056300     MOVE   "OCCURS ZERO TIMES - MOVE SHOULD HAVE FAILED"         NC2474.2
056400            TO RE-MARK                                            NC2474.2
056500     PERFORM FAIL.                                                NC2474.2
056600 MOV-WRITE-F1-5.                                                  NC2474.2
056700     PERFORM PRINT-DETAIL.                                        NC2474.2
056800*                                                                 NC2474.2
056900 MOV-INIT-F1-6.                                                   NC2474.2
057000     MOVE   "MOV-TEST-F1-6" TO PAR-NAME.                          NC2474.2
057100     MOVE   "VI-26 5.8.3 SR5" TO ANSI-REFERENCE.                  NC2474.2
057200     MOVE    9  TO DOI-DU-01V00.                                  NC2474.2
057300     MOVE   "P" TO ODO-XN-00001-O009D (1).                        NC2474.2
057400     MOVE   "Q" TO ODO-XN-00001-O009D (2).                        NC2474.2
057500     MOVE   "R" TO ODO-XN-00001-O009D (3).                        NC2474.2
057600     MOVE   "S" TO ODO-XN-00001-O009D (4).                        NC2474.2
057700     MOVE   "T" TO ODO-XN-00001-O009D (5).                        NC2474.2
057800     MOVE   "U" TO ODO-XN-00001-O009D (6).                        NC2474.2
057900     MOVE   "V" TO ODO-XN-00001-O009D (7).                        NC2474.2
058000     MOVE   "W" TO ODO-XN-00001-O009D (8).                        NC2474.2
058100     MOVE   "X" TO ODO-XN-00001-O009D (9).                        NC2474.2
058200     MOVE    3  TO NEW-DU-01V00.                                  NC2474.2
058300     MOVE    ODO-RECORD TO NEW-RECORD.                            NC2474.2
058400 MOV-TEST-F1-6.                                                   NC2474.2
058500     IF      NEW-GRP-00009 = "PQRSTUVWX"                          NC2474.2
058600             PERFORM PASS                                         NC2474.2
058700             GO TO MOV-WRITE-F1-6                                 NC2474.2
058800     ELSE                                                         NC2474.2
058900             GO TO MOV-FAIL-F1-6.                                 NC2474.2
059000 MOVE-DELETE-F1-6.                                                NC2474.2
059100     PERFORM DE-LETE.                                             NC2474.2
059200     GO TO   MOV-WRITE-F1-6.                                      NC2474.2
059300 MOV-FAIL-F1-6.                                                   NC2474.2
059400     MOVE    NEW-GRP-00009 TO COMPUTED-A                          NC2474.2
059500     MOVE   "PQRSTUVWX"   TO CORRECT-A                            NC2474.2
059600     MOVE   "ALL 9 FIELDS SHOULD BE MOVED IN GROUP MOVE"          NC2474.2
059700            TO RE-MARK                                            NC2474.2
059800     PERFORM FAIL.                                                NC2474.2
059900 MOV-WRITE-F1-6.                                                  NC2474.2
060000     PERFORM PRINT-DETAIL.                                        NC2474.2
060100*                                                                 NC2474.2
060200 SCH-INIT-F1-1.                                                   NC2474.2
060300     MOVE "SCH-TEST-F1-1" TO PAR-NAME.                            NC2474.2
060400     MOVE "VI-26 5.8" TO ANSI-REFERENCE.                          NC2474.2
060500     MOVE "SEARCH FULL ODO TABLE" TO RE-MARK.                     NC2474.2
060600     PERFORM INIT-WRK-AREA.                                       NC2474.2
060700     SET ODO-IX TO 1.                                             NC2474.2
060800 SCH-TEST-F1-1.                                                   NC2474.2
060900     SEARCH ODO-XN-00001-O009D                                    NC2474.2
061000         WHEN ODO-XN-00001-O009D (ODO-IX) IS EQUAL TO "7"         NC2474.2
061100             PERFORM PASS                                         NC2474.2
061200             GO TO SCH-WRITE-F1-1.                                NC2474.2
061300     GO TO SCH-FAIL-F1-1.                                         NC2474.2
061400 SCH-DELETE-F1-1.                                                 NC2474.2
061500     PERFORM DE-LETE.                                             NC2474.2
061600     GO TO SCH-WRITE-F1-1.                                        NC2474.2
061700 SCH-FAIL-F1-1.                                                   NC2474.2
061800     PERFORM FAIL.                                                NC2474.2
061900     MOVE "7 SHOULD BE FOUND" TO CORRECT-A                        NC2474.2
062000     MOVE "7 WAS NOT FOUND" TO COMPUTED-A.                        NC2474.2
062100 SCH-WRITE-F1-1.                                                  NC2474.2
062200     PERFORM PRINT-DETAIL.                                        NC2474.2
062300*                                                                 NC2474.2
062400 SCH-INIT-F1-2.                                                   NC2474.2
062500     MOVE "SCH-TEST-F1-2" TO PAR-NAME.                            NC2474.2
062600     MOVE "VI-26 5.8" TO ANSI-REFERENCE.                          NC2474.2
062700     MOVE "SEARCH PARTIAL ODO TABLE" TO RE-MARK.                  NC2474.2
062800     PERFORM INIT-WRK-AREA.                                       NC2474.2
062900     MOVE 3 TO DOI-DU-01V00.                                      NC2474.2
063000     SET ODO-IX TO 1.                                             NC2474.2
063100 SCH-TEST-F1-2.                                                   NC2474.2
063200     SEARCH ODO-XN-00001-O009D                                    NC2474.2
063300         AT END                                                   NC2474.2
063400             PERFORM PASS                                         NC2474.2
063500             GO TO SCH-WRITE-F1-2                                 NC2474.2
063600         WHEN ODO-XN-00001-O009D (ODO-IX) IS EQUAL TO "7"         NC2474.2
063700             GO TO SCH-FAIL-F1-2.                                 NC2474.2
063800 SCH-DELETE-F1-2.                                                 NC2474.2
063900     PERFORM DE-LETE.                                             NC2474.2
064000     GO TO SCH-WRITE-F1-2.                                        NC2474.2
064100 SCH-FAIL-F1-2.                                                   NC2474.2
064200     PERFORM FAIL                                                 NC2474.2
064300     MOVE "7 SHOULDN""T BE FOUND" TO CORRECT-A                    NC2474.2
064400     MOVE "7 WAS FOUND" TO COMPUTED-A.                            NC2474.2
064500 SCH-WRITE-F1-2.                                                  NC2474.2
064600     PERFORM PRINT-DETAIL.                                        NC2474.2
064700*                                                                 NC2474.2
064800 SCH-INIT-F2-3.                                                   NC2474.2
064900     MOVE "SCH-TEST-F2-3" TO PAR-NAME.                            NC2474.2
065000     MOVE "VI-26 5.8" TO ANSI-REFERENCE.                          NC2474.2
065100     MOVE "SEARCH ALL FULL ODO TABLE" TO RE-MARK.                 NC2474.2
065200     PERFORM INIT-WRK-AREA.                                       NC2474.2
065300 SCH-TEST-F2-3.                                                   NC2474.2
065400     SEARCH ALL ODO-XN-00001-O009D                                NC2474.2
065500         WHEN ODO-XN-00001-O009D (ODO-IX) IS EQUAL TO "7"         NC2474.2
065600             PERFORM PASS                                         NC2474.2
065700             GO TO SCH-WRITE-F2-3.                                NC2474.2
065800     GO TO SCH-FAIL-F2-3.                                         NC2474.2
065900 SCH-DELETE-F2-3.                                                 NC2474.2
066000     PERFORM DE-LETE.                                             NC2474.2
066100     GO TO SCH-WRITE-F2-3.                                        NC2474.2
066200 SCH-FAIL-F2-3.                                                   NC2474.2
066300     PERFORM FAIL                                                 NC2474.2
066400     MOVE "7 SHOULD BE FOUND" TO CORRECT-A                        NC2474.2
066500     MOVE "7 WAS NOT FOUND" TO COMPUTED-A.                        NC2474.2
066600 SCH-WRITE-F2-3.                                                  NC2474.2
066700     PERFORM PRINT-DETAIL.                                        NC2474.2
066800*                                                                 NC2474.2
066900 SCH-INIT-4.                                                      NC2474.2
067000     MOVE "SCH-TEST-4" TO PAR-NAME.                               NC2474.2
067100     MOVE "VI-26 5.8" TO ANSI-REFERENCE.                          NC2474.2
067200     MOVE "SEARCH ALL PARTIAL ODO TABLE" TO RE-MARK.              NC2474.2
067300     PERFORM INIT-WRK-AREA.                                       NC2474.2
067400     MOVE 3 TO DOI-DU-01V00.                                      NC2474.2
067500 SCH-TEST-4.                                                      NC2474.2
067600     SEARCH ALL ODO-XN-00001-O009D                                NC2474.2
067700         AT END                                                   NC2474.2
067800             PERFORM PASS                                         NC2474.2
067900             GO TO SCH-WRITE-4                                    NC2474.2
068000         WHEN ODO-XN-00001-O009D (ODO-IX) IS EQUAL TO "7"         NC2474.2
068100             GO TO SCH-FAIL-4.                                    NC2474.2
068200 SCH-DELETE-4.                                                    NC2474.2
068300     PERFORM DE-LETE.                                             NC2474.2
068400     GO TO SCH-WRITE-4.                                           NC2474.2
068500 SCH-FAIL-4.                                                      NC2474.2
068600     PERFORM FAIL                                                 NC2474.2
068700     MOVE "7 SHOULDN""T BE FOUND" TO CORRECT-A                    NC2474.2
068800     MOVE "7 WAS FOUND" TO COMPUTED-A.                            NC2474.2
068900 SCH-WRITE-4.                                                     NC2474.2
069000     PERFORM PRINT-DETAIL.                                        NC2474.2
069100*                                                                 NC2474.2
069200 STR-INIT-GF-1.                                                   NC2474.2
069300     MOVE "STR-TEST-GF-1" TO PAR-NAME.                            NC2474.2
069400     MOVE "VI-26 5.8" TO ANSI-REFERENCE.                          NC2474.2
069500     MOVE "STRING FULL ODO AND LITERAL"  TO RE-MARK.              NC2474.2
069600     PERFORM INIT-WRK-AREA.                                       NC2474.2
069700     MOVE SPACES TO WRK-XN-00020.                                 NC2474.2
069800 STR-TEST-GF-1.                                                   NC2474.2
069900     STRING ODO-GRP-00009  "-TRAILER" DELIMITED BY SIZE           NC2474.2
070000         INTO WRK-XN-00020.                                       NC2474.2
070100     IF WRK-XN-00020 IS EQUAL TO "123456789-TRAILER   "           NC2474.2
070200         PERFORM PASS                                             NC2474.2
070300         GO TO STR-WRITE-GF-1                                     NC2474.2
070400     ELSE                                                         NC2474.2
070500         GO TO STR-FAIL-GF-1.                                     NC2474.2
070600 STR-DELETE-GF-1.                                                 NC2474.2
070700     PERFORM DE-LETE.                                             NC2474.2
070800     GO TO STR-WRITE-GF-1.                                        NC2474.2
070900 STR-FAIL-GF-1.                                                   NC2474.2
071000     PERFORM FAIL                                                 NC2474.2
071100     MOVE "123456789-TRAILER" TO CORRECT-A                        NC2474.2
071200     MOVE WRK-XN-00020 TO COMPUTED-A.                             NC2474.2
071300 STR-WRITE-GF-1.                                                  NC2474.2
071400     PERFORM PRINT-DETAIL.                                        NC2474.2
071500*                                                                 NC2474.2
071600 STR-INIT-GF-2.                                                   NC2474.2
071700     MOVE "STR-TEST-GF-2" TO PAR-NAME.                            NC2474.2
071800     MOVE "VI-26 5.8" TO ANSI-REFERENCE.                          NC2474.2
071900     MOVE "STRING PARTIAL ODO AND LITERAL" TO RE-MARK.            NC2474.2
072000     PERFORM INIT-WRK-AREA.                                       NC2474.2
072100     MOVE SPACES TO WRK-XN-00020.                                 NC2474.2
072200     MOVE 3 TO DOI-DU-01V00.                                      NC2474.2
072300 STR-TEST-GF-2.                                                   NC2474.2
072400     STRING ODO-GRP-00009 "-TRAILER" DELIMITED BY SIZE            NC2474.2
072500         INTO WRK-XN-00020.                                       NC2474.2
072600     IF WRK-XN-00020 IS EQUAL TO "123-TRAILER         "           NC2474.2
072700         PERFORM PASS                                             NC2474.2
072800         GO TO STR-WRITE-GF-2                                     NC2474.2
072900     ELSE                                                         NC2474.2
073000         GO TO STR-FAIL-GF-2.                                     NC2474.2
073100 STR-DELETE-GF-2.                                                 NC2474.2
073200     PERFORM DE-LETE.                                             NC2474.2
073300     GO TO STR-WRITE-GF-2.                                        NC2474.2
073400 STR-FAIL-GF-2.                                                   NC2474.2
073500     PERFORM FAIL                                                 NC2474.2
073600     MOVE "123-TRAILER" TO CORRECT-A                              NC2474.2
073700     MOVE WRK-XN-00020 TO COMPUTED-A.                             NC2474.2
073800 STR-WRITE-GF-2.                                                  NC2474.2
073900     PERFORM PRINT-DETAIL.                                        NC2474.2
074000*                                                                 NC2474.2
074100 STR-TEST-GF-3.                                                   NC2474.2
074200     PERFORM INIT-WRK-AREA.                                       NC2474.2
074300     MOVE SPACES TO WRK-XN-00020.                                 NC2474.2
074400     MOVE 3 TO DOI-DU-01V00.                                      NC2474.2
074500     STRING "LEADER-" ODO-GRP-00009 DELIMITED BY SIZE             NC2474.2
074600         INTO WRK-XN-00020.                                       NC2474.2
074700     IF WRK-XN-00020 IS EQUAL TO "LEADER-123      "               NC2474.2
074800         PERFORM PASS                                             NC2474.2
074900         GO TO STR-WRITE-GF-3                                     NC2474.2
075000         ELSE                                                     NC2474.2
075100         PERFORM FAIL                                             NC2474.2
075200         MOVE "LEADER-123" TO CORRECT-A                           NC2474.2
075300         MOVE WRK-XN-00020 TO COMPUTED-A                          NC2474.2
075400         PERFORM STR-WRITE-GF-3                                   NC2474.2
075500         GO TO STR-DELETE-GF-4.                                   NC2474.2
075600 STR-DELETE-GF-3.                                                 NC2474.2
075700     PERFORM DE-LETE.                                             NC2474.2
075800 STR-WRITE-GF-3.                                                  NC2474.2
075900     MOVE "STR-TEST-GF-3" TO PAR-NAME.                            NC2474.2
076000     MOVE "VI-26 5.8" TO ANSI-REFERENCE.                          NC2474.2
076100     MOVE "STRING LITERAL AND PARTIAL ODO" TO RE-MARK.            NC2474.2
076200     PERFORM PRINT-DETAIL.                                        NC2474.2
076300*                                                                 NC2474.2
076400 STR-INIT-GF-4.                                                   NC2474.2
076500     MOVE "STR-TEST-GF-4" TO PAR-NAME.                            NC2474.2
076600     MOVE "VI-26 5.8" TO ANSI-REFERENCE.                          NC2474.2
076700     MOVE "PARTIAL ODO/UNNEEDED OVERFLOW" TO RE-MARK.             NC2474.2
076800     PERFORM INIT-WRK-AREA                                        NC2474.2
076900     MOVE SPACES TO WRK-XN-00010.                                 NC2474.2
077000     MOVE 3 TO DOI-DU-01V00.                                      NC2474.2
077100 STR-TEST-GF-4.                                                   NC2474.2
077200     STRING "LEADER-"  ODO-GRP-00009 DELIMITED BY SIZE            NC2474.2
077300         INTO WRK-XN-00010                                        NC2474.2
077400         ON OVERFLOW                                              NC2474.2
077500             GO TO STR-FAIL-GF-4.                                 NC2474.2
077600     PERFORM PASS.                                                NC2474.2
077700     GO TO STR-WRITE-GF-4.                                        NC2474.2
077800 STR-DELETE-GF-4.                                                 NC2474.2
077900     PERFORM DE-LETE.                                             NC2474.2
078000     MOVE "STR-TEST-GF-4" TO PAR-NAME.                            NC2474.2
078100     MOVE "DELETE AUTOMATIC IF" TO COMPUTED-A.                    NC2474.2
078200     MOVE "STR-TEST-GF-3 FAILS" TO CORRECT-A.                     NC2474.2
078300     GO TO STR-WRITE-GF-4.                                        NC2474.2
078400 STR-FAIL-GF-4.                                                   NC2474.2
078500     PERFORM FAIL                                                 NC2474.2
078600     MOVE "OVERFLOW EXIT TAKEN" TO COMPUTED-A                     NC2474.2
078700     MOVE "NO EXIT NECESSARY" TO CORRECT-A.                       NC2474.2
078800 STR-WRITE-GF-4.                                                  NC2474.2
078900     PERFORM PRINT-DETAIL.                                        NC2474.2
079000*                                                                 NC2474.2
079100 UST-INIT-GF-1.                                                   NC2474.2
079200     MOVE "UST-TEST-GF-1" TO PAR-NAME.                            NC2474.2
079300     MOVE "VI-26 5.8" TO ANSI-REFERENCE.                          NC2474.2
079400     MOVE "UNSTRING FULL ODO" TO RE-MARK.                         NC2474.2
079500     PERFORM INIT-WRK-AREA.                                       NC2474.2
079600     MOVE SPACES TO WRK-XN-00010 WRK-XN-00020.                    NC2474.2
079700 UST-TEST-GF-1.                                                   NC2474.2
079800     UNSTRING GRP-ODO INTO WRK-XN-00010 WRK-XN-00020.             NC2474.2
079900     IF WRK-XN-00020 IS EQUAL TO "123456789           "           NC2474.2
080000         PERFORM PASS                                             NC2474.2
080100         GO TO UST-WRITE-GF-1                                     NC2474.2
080200     ELSE                                                         NC2474.2
080300         GO TO UST-FAIL-GF-1.                                     NC2474.2
080400 UST-DELETE-GF-1.                                                 NC2474.2
080500     PERFORM DE-LETE.                                             NC2474.2
080600     GO TO UST-WRITE-GF-1.                                        NC2474.2
080700 UST-FAIL-GF-1.                                                   NC2474.2
080800     PERFORM FAIL                                                 NC2474.2
080900     MOVE  WRK-XN-00020 TO COMPUTED-A                             NC2474.2
081000     MOVE "123456789" TO CORRECT-A.                               NC2474.2
081100 UST-WRITE-GF-1.                                                  NC2474.2
081200     PERFORM PRINT-DETAIL.                                        NC2474.2
081300*                                                                 NC2474.2
081400 UST-INIT-GF-2.                                                   NC2474.2
081500     MOVE "UST-TEST-GF-2" TO PAR-NAME.                            NC2474.2
081600     MOVE "VI-26 5.8" TO ANSI-REFERENCE.                          NC2474.2
081700     MOVE "UNSTRING PARTIAL ODO" TO RE-MARK.                      NC2474.2
081800     PERFORM INIT-WRK-AREA.                                       NC2474.2
081900     MOVE SPACES TO WRK-XN-00020  WRK-XN-00010.                   NC2474.2
082000     MOVE 3 TO DOI-DU-01V00.                                      NC2474.2
082100 UST-TEST-GF-2.                                                   NC2474.2
082200     UNSTRING GRP-ODO INTO WRK-XN-00010  WRK-XN-00020.            NC2474.2
082300     IF WRK-XN-00020 IS EQUAL TO "123                 "           NC2474.2
082400         PERFORM PASS                                             NC2474.2
082500         GO TO UST-WRITE-GF-2                                     NC2474.2
082600     ELSE                                                         NC2474.2
082700         GO TO UST-FAIL-GF-2.                                     NC2474.2
082800 UST-DELETE-GF-2.                                                 NC2474.2
082900     PERFORM DE-LETE.                                             NC2474.2
083000     GO TO UST-WRITE-GF-2.                                        NC2474.2
083100 UST-FAIL-GF-2.                                                   NC2474.2
083200     PERFORM FAIL                                                 NC2474.2
083300     MOVE WRK-XN-00020 TO COMPUTED-A                              NC2474.2
083400     MOVE "123" TO CORRECT-A.                                     NC2474.2
083500 UST-WRITE-GF-2.                                                  NC2474.2
083600     PERFORM PRINT-DETAIL.                                        NC2474.2
083700*                                                                 NC2474.2
083800 UST-INIT-GF-3.                                                   NC2474.2
083900     MOVE "UST-TEST-GF-3" TO PAR-NAME.                            NC2474.2
084000     MOVE "VI-26 5.8" TO ANSI-REFERENCE.                          NC2474.2
084100     MOVE "UNSTRING DELIMITED PARTIAL ODO" TO RE-MARK.            NC2474.2
084200     PERFORM INIT-WRK-AREA.                                       NC2474.2
084300     MOVE SPACES TO WRK-XN-00020.                                 NC2474.2
084400     MOVE 3 TO DOI-DU-01V00.                                      NC2474.2
084500 UST-TEST-GF-3.                                                   NC2474.2
084600     UNSTRING GRP-ODO DELIMITED BY "7" INTO WRK-XN-00020.         NC2474.2
084700     IF WRK-XN-00020 IS EQUAL TO "3 ACTIVE: 123       "           NC2474.2
084800         PERFORM PASS                                             NC2474.2
084900         GO TO UST-WRITE-GF-3                                     NC2474.2
085000     ELSE                                                         NC2474.2
085100         GO TO UST-FAIL-GF-3.                                     NC2474.2
085200 UST-DELETE-GF-3.                                                 NC2474.2
085300     PERFORM DE-LETE.                                             NC2474.2
085400     GO TO UST-WRITE-GF-3.                                        NC2474.2
085500 UST-FAIL-GF-3.                                                   NC2474.2
085600     PERFORM FAIL                                                 NC2474.2
085700     MOVE WRK-XN-00020 TO COMPUTED-A                              NC2474.2
085800     MOVE "3 ACTIVE: 123" TO CORRECT-A.                           NC2474.2
085900 UST-WRITE-GF-3.                                                  NC2474.2
086000     PERFORM PRINT-DETAIL.                                        NC2474.2
086100*                                                                 NC2474.2
086200 CCVS-EXIT SECTION.                                               NC2474.2
086300 CCVS-999999.                                                     NC2474.2
086400     GO TO CLOSE-FILES.                                           NC2474.2
      *END-OF,NC247A                                                                  
