      *HEADER,COBOL,NC102A                                                            
000100 IDENTIFICATION DIVISION.                                         NC1024.2
000200 PROGRAM-ID.                                                      NC1024.2
000300     NC102A.                                                      NC1024.2
000400                                                                  NC1024.2
000500****************************************************************  NC1024.2
000600*                                                              *  NC1024.2
000700*    VALIDATION FOR:-                                          *  NC1024.2
000800*                                                              *  NC1024.2
000900*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1024.2
001000*                                                              *  NC1024.2
001100*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC1024.2
001200*                                                              *  NC1024.2
001300****************************************************************  NC1024.2
001400*                                                              *  NC1024.2
001500*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  NC1024.2
001600*                                                              *  NC1024.2
001700*        X-55  - SYSTEM PRINTER NAME.                          *  NC1024.2
001800*        X-82  - SOURCE COMPUTER NAME.                         *  NC1024.2
001900*        X-83  - OBJECT COMPUTER NAME.                         *  NC1024.2
002000*                                                              *  NC1024.2
002100****************************************************************  NC1024.2
002200*                                                                 NC1024.2
002300*    THIS PROGRAM TESTS FORMATS 1, 2 AND 3 OF THE "PERFORM"       NC1024.2
002400*    STATEMENT, FORMATS 1 AND 2 OF THE "GO" STATEMENT AND         NC1024.2
002500*    THE "EXIT" STATEMENT.                                        NC1024.2
002600*                                                                 NC1024.2
002700 ENVIRONMENT DIVISION.                                            NC1024.2
002800 CONFIGURATION SECTION.                                           NC1024.2
002900 SOURCE-COMPUTER.                                                 NC1024.2
003000     XXXXX082.                                                    NC1024.2
003100 OBJECT-COMPUTER.                                                 NC1024.2
003200     XXXXX083.                                                    NC1024.2
003300 INPUT-OUTPUT SECTION.                                            NC1024.2
003400 FILE-CONTROL.                                                    NC1024.2
003500     SELECT PRINT-FILE ASSIGN TO                                  NC1024.2
003600     XXXXX055.                                                    NC1024.2
003700 DATA DIVISION.                                                   NC1024.2
003800 FILE SECTION.                                                    NC1024.2
003900 FD  PRINT-FILE.                                                  NC1024.2
004000 01  PRINT-REC PICTURE X(120).                                    NC1024.2
004100 01  DUMMY-RECORD PICTURE X(120).                                 NC1024.2
004200 WORKING-STORAGE SECTION.                                         NC1024.2
004300 01  PERFORM3    PIC 9    VALUE 5.                                NC1024.2
004400 01  WRK-XN-18-1 PIC X(18).                                       NC1024.2
004500 01  WRK-AN-X-18-1, REDEFINES WRK-XN-18-1 PIC A(18).              NC1024.2
004600 01  WRK-DU-X-18V0-1  REDEFINES WRK-XN-18-1 PIC 9(18).            NC1024.2
004700 01  WRK-DU-0V1-1 PIC V9 VALUE .3.                                NC1024.2
004800 01  WRK-DU-0V2-1 PIC V99 VALUE .25.                              NC1024.2
004900 01  WRK-DU-0V12-1 PIC V9(12) VALUE .00001.                       NC1024.2
005000 01  WRK-DU-1V0-1 PIC 9 VALUE ZERO.                               NC1024.2
005100 01  WRK-DU-1V1-1 PIC 9V9 VALUE 1.1.                              NC1024.2
005200 01  WRK-DU-1V1-2 PIC 9V9 VALUE 2.4.                              NC1024.2
005300 01  WRK-DU-1V3-1 PIC 9V999 VALUE 1.001.                          NC1024.2
005400 01  WRK-DU-1V3-2 PIC 9V999 VALUE 1.001.                          NC1024.2
005500 01  WRK-DU-1V5-1 PIC 9V9(5).                                     NC1024.2
005600 01  WRK-DU-1V17-1 PIC 9V9(17) VALUE 3.14159265358979323.         NC1024.2
005700 01  WRK-DU-2P6-1 PIC 99P(6) VALUE 99000000.                      NC1024.2
005800 01  WRK-DU-2V0-1 PIC 99.                                         NC1024.2
005900 01  WRK-DU-2V0-2 PIC 99.                                         NC1024.2
006000 01  WRK-DU-2V0-3 PIC 99.                                         NC1024.2
006100 01  WRK-DU-2V1-1 PIC 99V9.                                       NC1024.2
006200 01  WRK-DU-2V1-2 PIC 99V9.                                       NC1024.2
006300 01  WRK-DU-2V1-3 PIC 99V9.                                       NC1024.2
006400 01  WRK-DU-2V2-1 PIC 99V99 VALUE 15.44.                          NC1024.2
006500 01  WRK-DU-2V2-2 PIC 99V99 VALUE 60.89.                          NC1024.2
006600 01  WRK-DU-2V2-3 PIC 99V99 VALUE 60.99.                          NC1024.2
006700 01  WRK-DU-2V2-4 PIC 99V99 VALUE 60.99.                          NC1024.2
006800 01  WRK-DU-2V2-5 PIC 99V99 VALUE 10.00.                          NC1024.2
006900 01  WRK-DU-2V5-1 PIC 99V9(5).                                    NC1024.2
007000 01  WRK-DU-4P1-1 PIC P(4)9 VALUE .00001.                         NC1024.2
007100 01  WRK-DU-5V1-1 PIC 9(5)V9 VALUE 12345.6.                       NC1024.2
007200 01  WRK-DU-6V0-1 PIC 9(6) VALUE 99999.                           NC1024.2
007300 01  WRK-DU-6V0-2 PIC 9(6) VALUE 99999.                           NC1024.2
007400 01  WRK-DU-16V2-1 PIC 9(16)V99 VALUE 9999999999999999.99.        NC1024.2
007500 01  WRK-NE-X-1 PIC 9(16).99.                                     NC1024.2
007600 01  WRK-DS-1V0-1 PIC S9 VALUE -3.                                NC1024.2
007700 01  WRK-DS-1V0-2 PIC S9 VALUE 2.                                 NC1024.2
007800 01  WRK-DS-1V2-1 PIC S9V99 VALUE ZERO.                           NC1024.2
007900 01  WRK-DS-2V0-1 PIC S99 VALUE ZERO.                             NC1024.2
008000 01  WRK-DS-2V1-1 PIC S99V9 VALUE ZERO.                           NC1024.2
008100 01  WRK-DS-2V2-1 PIC S99V99 VALUE ZERO.                          NC1024.2
008200 01  WRK-DS-2V2-2 PIC S99V99 VALUE -12.34.                        NC1024.2
008300 01  WRK-DS-16V2-1 PIC S9(16)V99 VALUE -9999999999999999.99.      NC1024.2
008400 01  WRK-NE-X-2 PIC -9(16).99.                                    NC1024.2
008500 01  WRK-NE-1 PIC .9999/99999,99999,99.                           NC1024.2
008600 01  WRK-NE-2 PIC $**.99.                                         NC1024.2
008700 01  WRK-NE-3 PIC $99.99CR.                                       NC1024.2
008800 01  WRK-NE-4 PIC $*9.99 VALUE ZERO.                              NC1024.2
008900 77  WRK-DS-02V00                 PICTURE S99.                    NC1024.2
009000 77  ATWO-DS-01V00                PICTURE S9                      NC1024.2
009100                                  VALUE 2.                        NC1024.2
009200 77  P-COUNT                      PICTURE 9(6).                   NC1024.2
009300 77  THREE                              PICTURE IS 9 VALUE IS 3.  NC1024.2
009400 77  WS-FOUR                            PICTURE IS 9 VALUE IS 4.  NC1024.2
009500 77  XRAY                               PICTURE IS X.             NC1024.2
009600 77  ALTERLOOP                          PICTURE IS 9 VALUE IS     NC1024.2
009700     ZERO.                                                        NC1024.2
009800 01  NOTE-RECORD.                                                 NC1024.2
009900     02 A     PICTURE X VALUE SPACE.                              NC1024.2
010000     02 B     PICTURE X VALUE SPACE.                              NC1024.2
010100     02 C     PICTURE X VALUE SPACE.                              NC1024.2
010200     02 D     PICTURE X VALUE SPACE.                              NC1024.2
010300     02 E     PICTURE X VALUE SPACE.                              NC1024.2
010400     02 F     PICTURE X VALUE SPACE.                              NC1024.2
010500     02 G     PICTURE X VALUE SPACE.                              NC1024.2
010600     02 H     PICTURE X VALUE SPACE.                              NC1024.2
010700     02 I     PICTURE X VALUE SPACE.                              NC1024.2
010800     02 J     PICTURE X VALUE SPACE.                              NC1024.2
010900     02 K     PICTURE X VALUE SPACE.                              NC1024.2
011000     02 L     PICTURE X VALUE SPACE.                              NC1024.2
011100     02 M     PICTURE X VALUE SPACE.                              NC1024.2
011200     02 N     PICTURE X VALUE SPACE.                              NC1024.2
011300     02 O     PICTURE X VALUE SPACE.                              NC1024.2
011400     02 P     PICTURE X VALUE SPACE.                              NC1024.2
011500 01  GO-TABLE.                                                    NC1024.2
011600     02 GO-SCRIPT OCCURS 8 TIMES PICTURE 9.                       NC1024.2
011700 01  GO-TO-DEPEND                       PICTURE IS 9 VALUE IS 0.  NC1024.2
011800 01  GO-TO-DEEP                         PICTURE IS 9 VALUE IS 1.  NC1024.2
011900 01  PERFORM1                           PICTURE IS XXX            NC1024.2
012000     VALUE IS SPACE.                                              NC1024.2
012100 01  PERFORM2                           PICTURE IS S999           NC1024.2
012200     VALUE IS 20.                                                 NC1024.2
012300 01  PERFORM4                           PICTURE IS S99V9.         NC1024.2
012400 01  PERFORM5                           PICTURE IS 999            NC1024.2
012500     VALUE IS ZERO.                                               NC1024.2
012600 01  PERFORM-KEY                        PICTURE IS 9.             NC1024.2
012700 01  PERFORM-HOLD.                                                NC1024.2
012800     02  TEST-LETTER OCCURS 20 TIMES  PICTURE X.                  NC1024.2
012900 01  TEST-RESULTS.                                                NC1024.2
013000     02 FILLER                   PIC X      VALUE SPACE.          NC1024.2
013100     02 FEATURE                  PIC X(20)  VALUE SPACE.          NC1024.2
013200     02 FILLER                   PIC X      VALUE SPACE.          NC1024.2
013300     02 P-OR-F                   PIC X(5)   VALUE SPACE.          NC1024.2
013400     02 FILLER                   PIC X      VALUE SPACE.          NC1024.2
013500     02  PAR-NAME.                                                NC1024.2
013600       03 FILLER                 PIC X(19)  VALUE SPACE.          NC1024.2
013700       03  PARDOT-X              PIC X      VALUE SPACE.          NC1024.2
013800       03 DOTVALUE               PIC 99     VALUE ZERO.           NC1024.2
013900     02 FILLER                   PIC X(8)   VALUE SPACE.          NC1024.2
014000     02 RE-MARK                  PIC X(61).                       NC1024.2
014100 01  TEST-COMPUTED.                                               NC1024.2
014200     02 FILLER                   PIC X(30)  VALUE SPACE.          NC1024.2
014300     02 FILLER                   PIC X(17)  VALUE                 NC1024.2
014400            "       COMPUTED=".                                   NC1024.2
014500     02 COMPUTED-X.                                               NC1024.2
014600     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          NC1024.2
014700     03 COMPUTED-N               REDEFINES COMPUTED-A             NC1024.2
014800                                 PIC -9(9).9(9).                  NC1024.2
014900     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         NC1024.2
015000     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     NC1024.2
015100     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     NC1024.2
015200     03       CM-18V0 REDEFINES COMPUTED-A.                       NC1024.2
015300         04 COMPUTED-18V0                    PIC -9(18).          NC1024.2
015400         04 FILLER                           PIC X.               NC1024.2
015500     03 FILLER PIC X(50) VALUE SPACE.                             NC1024.2
015600 01  TEST-CORRECT.                                                NC1024.2
015700     02 FILLER PIC X(30) VALUE SPACE.                             NC1024.2
015800     02 FILLER PIC X(17) VALUE "       CORRECT =".                NC1024.2
015900     02 CORRECT-X.                                                NC1024.2
016000     03 CORRECT-A                  PIC X(20) VALUE SPACE.         NC1024.2
016100     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      NC1024.2
016200     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         NC1024.2
016300     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     NC1024.2
016400     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     NC1024.2
016500     03      CR-18V0 REDEFINES CORRECT-A.                         NC1024.2
016600         04 CORRECT-18V0                     PIC -9(18).          NC1024.2
016700         04 FILLER                           PIC X.               NC1024.2
016800     03 FILLER PIC X(2) VALUE SPACE.                              NC1024.2
016900     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     NC1024.2
017000 01  CCVS-C-1.                                                    NC1024.2
017100     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PANC1024.2
017200-    "SS  PARAGRAPH-NAME                                          NC1024.2
017300-    "       REMARKS".                                            NC1024.2
017400     02 FILLER                     PIC X(20)    VALUE SPACE.      NC1024.2
017500 01  CCVS-C-2.                                                    NC1024.2
017600     02 FILLER                     PIC X        VALUE SPACE.      NC1024.2
017700     02 FILLER                     PIC X(6)     VALUE "TESTED".   NC1024.2
017800     02 FILLER                     PIC X(15)    VALUE SPACE.      NC1024.2
017900     02 FILLER                     PIC X(4)     VALUE "FAIL".     NC1024.2
018000     02 FILLER                     PIC X(94)    VALUE SPACE.      NC1024.2
018100 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       NC1024.2
018200 01  REC-CT                        PIC 99       VALUE ZERO.       NC1024.2
018300 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       NC1024.2
018400 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       NC1024.2
018500 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       NC1024.2
018600 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       NC1024.2
018700 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       NC1024.2
018800 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       NC1024.2
018900 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      NC1024.2
019000 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       NC1024.2
019100 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     NC1024.2
019200 01  CCVS-H-1.                                                    NC1024.2
019300     02  FILLER                    PIC X(39)    VALUE SPACES.     NC1024.2
019400     02  FILLER                    PIC X(42)    VALUE             NC1024.2
019500     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 NC1024.2
019600     02  FILLER                    PIC X(39)    VALUE SPACES.     NC1024.2
019700 01  CCVS-H-2A.                                                   NC1024.2
019800   02  FILLER                        PIC X(40)  VALUE SPACE.      NC1024.2
019900   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  NC1024.2
020000   02  FILLER                        PIC XXXX   VALUE             NC1024.2
020100     "4.2 ".                                                      NC1024.2
020200   02  FILLER                        PIC X(28)  VALUE             NC1024.2
020300            " COPY - NOT FOR DISTRIBUTION".                       NC1024.2
020400   02  FILLER                        PIC X(41)  VALUE SPACE.      NC1024.2
020500                                                                  NC1024.2
020600 01  CCVS-H-2B.                                                   NC1024.2
020700   02  FILLER                        PIC X(15)  VALUE             NC1024.2
020800            "TEST RESULT OF ".                                    NC1024.2
020900   02  TEST-ID                       PIC X(9).                    NC1024.2
021000   02  FILLER                        PIC X(4)   VALUE             NC1024.2
021100            " IN ".                                               NC1024.2
021200   02  FILLER                        PIC X(12)  VALUE             NC1024.2
021300     " HIGH       ".                                              NC1024.2
021400   02  FILLER                        PIC X(22)  VALUE             NC1024.2
021500            " LEVEL VALIDATION FOR ".                             NC1024.2
021600   02  FILLER                        PIC X(58)  VALUE             NC1024.2
021700     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1024.2
021800 01  CCVS-H-3.                                                    NC1024.2
021900     02  FILLER                      PIC X(34)  VALUE             NC1024.2
022000            " FOR OFFICIAL USE ONLY    ".                         NC1024.2
022100     02  FILLER                      PIC X(58)  VALUE             NC1024.2
022200     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC1024.2
022300     02  FILLER                      PIC X(28)  VALUE             NC1024.2
022400            "  COPYRIGHT   1985 ".                                NC1024.2
022500 01  CCVS-E-1.                                                    NC1024.2
022600     02 FILLER                       PIC X(52)  VALUE SPACE.      NC1024.2
022700     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              NC1024.2
022800     02 ID-AGAIN                     PIC X(9).                    NC1024.2
022900     02 FILLER                       PIC X(45)  VALUE SPACES.     NC1024.2
023000 01  CCVS-E-2.                                                    NC1024.2
023100     02  FILLER                      PIC X(31)  VALUE SPACE.      NC1024.2
023200     02  FILLER                      PIC X(21)  VALUE SPACE.      NC1024.2
023300     02 CCVS-E-2-2.                                               NC1024.2
023400         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      NC1024.2
023500         03 FILLER                   PIC X      VALUE SPACE.      NC1024.2
023600         03 ENDER-DESC               PIC X(44)  VALUE             NC1024.2
023700            "ERRORS ENCOUNTERED".                                 NC1024.2
023800 01  CCVS-E-3.                                                    NC1024.2
023900     02  FILLER                      PIC X(22)  VALUE             NC1024.2
024000            " FOR OFFICIAL USE ONLY".                             NC1024.2
024100     02  FILLER                      PIC X(12)  VALUE SPACE.      NC1024.2
024200     02  FILLER                      PIC X(58)  VALUE             NC1024.2
024300     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1024.2
024400     02  FILLER                      PIC X(13)  VALUE SPACE.      NC1024.2
024500     02 FILLER                       PIC X(15)  VALUE             NC1024.2
024600             " COPYRIGHT 1985".                                   NC1024.2
024700 01  CCVS-E-4.                                                    NC1024.2
024800     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      NC1024.2
024900     02 FILLER                       PIC X(4)   VALUE " OF ".     NC1024.2
025000     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      NC1024.2
025100     02 FILLER                       PIC X(40)  VALUE             NC1024.2
025200      "  TESTS WERE EXECUTED SUCCESSFULLY".                       NC1024.2
025300 01  XXINFO.                                                      NC1024.2
025400     02 FILLER                       PIC X(19)  VALUE             NC1024.2
025500            "*** INFORMATION ***".                                NC1024.2
025600     02 INFO-TEXT.                                                NC1024.2
025700       04 FILLER                     PIC X(8)   VALUE SPACE.      NC1024.2
025800       04 XXCOMPUTED                 PIC X(20).                   NC1024.2
025900       04 FILLER                     PIC X(5)   VALUE SPACE.      NC1024.2
026000       04 XXCORRECT                  PIC X(20).                   NC1024.2
026100     02 INF-ANSI-REFERENCE           PIC X(48).                   NC1024.2
026200 01  HYPHEN-LINE.                                                 NC1024.2
026300     02 FILLER  PIC IS X VALUE IS SPACE.                          NC1024.2
026400     02 FILLER  PIC IS X(65)    VALUE IS "************************NC1024.2
026500-    "*****************************************".                 NC1024.2
026600     02 FILLER  PIC IS X(54)    VALUE IS "************************NC1024.2
026700-    "******************************".                            NC1024.2
026800 01  CCVS-PGM-ID                     PIC X(9)   VALUE             NC1024.2
026900     "NC102A".                                                    NC1024.2
027000 PROCEDURE DIVISION.                                              NC1024.2
027100 CCVS1 SECTION.                                                   NC1024.2
027200 OPEN-FILES.                                                      NC1024.2
027300     OPEN     OUTPUT PRINT-FILE.                                  NC1024.2
027400     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   NC1024.2
027500     MOVE    SPACE TO TEST-RESULTS.                               NC1024.2
027600     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             NC1024.2
027700     GO TO CCVS1-EXIT.                                            NC1024.2
027800 CLOSE-FILES.                                                     NC1024.2
027900     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   NC1024.2
028000 TERMINATE-CCVS.                                                  NC1024.2
028100*    EXIT PROGRAM.                                                NC1024.2
028200*TERMINATE-CALL.                                                  NC1024.2
028300     STOP     RUN.                                                NC1024.2
028400 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         NC1024.2
028500 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           NC1024.2
028600 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          NC1024.2
028700 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      NC1024.2
028800     MOVE "****TEST DELETED****" TO RE-MARK.                      NC1024.2
028900 PRINT-DETAIL.                                                    NC1024.2
029000     IF REC-CT NOT EQUAL TO ZERO                                  NC1024.2
029100             MOVE "." TO PARDOT-X                                 NC1024.2
029200             MOVE REC-CT TO DOTVALUE.                             NC1024.2
029300     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      NC1024.2
029400     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               NC1024.2
029500        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 NC1024.2
029600          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 NC1024.2
029700     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              NC1024.2
029800     MOVE SPACE TO CORRECT-X.                                     NC1024.2
029900     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         NC1024.2
030000     MOVE     SPACE TO RE-MARK.                                   NC1024.2
030100 HEAD-ROUTINE.                                                    NC1024.2
030200     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC1024.2
030300     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC1024.2
030400     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC1024.2
030500     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC1024.2
030600 COLUMN-NAMES-ROUTINE.                                            NC1024.2
030700     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1024.2
030800     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1024.2
030900     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        NC1024.2
031000 END-ROUTINE.                                                     NC1024.2
031100     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.NC1024.2
031200 END-RTN-EXIT.                                                    NC1024.2
031300     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1024.2
031400 END-ROUTINE-1.                                                   NC1024.2
031500      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      NC1024.2
031600      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               NC1024.2
031700      ADD PASS-COUNTER TO ERROR-HOLD.                             NC1024.2
031800*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   NC1024.2
031900      MOVE PASS-COUNTER TO CCVS-E-4-1.                            NC1024.2
032000      MOVE ERROR-HOLD TO CCVS-E-4-2.                              NC1024.2
032100      MOVE CCVS-E-4 TO CCVS-E-2-2.                                NC1024.2
032200      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           NC1024.2
032300  END-ROUTINE-12.                                                 NC1024.2
032400      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        NC1024.2
032500     IF       ERROR-COUNTER IS EQUAL TO ZERO                      NC1024.2
032600         MOVE "NO " TO ERROR-TOTAL                                NC1024.2
032700         ELSE                                                     NC1024.2
032800         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       NC1024.2
032900     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           NC1024.2
033000     PERFORM WRITE-LINE.                                          NC1024.2
033100 END-ROUTINE-13.                                                  NC1024.2
033200     IF DELETE-COUNTER IS EQUAL TO ZERO                           NC1024.2
033300         MOVE "NO " TO ERROR-TOTAL  ELSE                          NC1024.2
033400         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      NC1024.2
033500     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   NC1024.2
033600     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1024.2
033700      IF   INSPECT-COUNTER EQUAL TO ZERO                          NC1024.2
033800          MOVE "NO " TO ERROR-TOTAL                               NC1024.2
033900      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   NC1024.2
034000      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            NC1024.2
034100      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          NC1024.2
034200     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1024.2
034300 WRITE-LINE.                                                      NC1024.2
034400     ADD 1 TO RECORD-COUNT.                                       NC1024.2
034500*    IF RECORD-COUNT GREATER 42                                   NC1024.2
034600*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          NC1024.2
034700*        MOVE SPACE TO DUMMY-RECORD                               NC1024.2
034800*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  NC1024.2
034900*        MOVE CCVS-H-1  TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   NC1024.2
035000*        MOVE CCVS-H-2A TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   NC1024.2
035100*        MOVE CCVS-H-2B TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   NC1024.2
035200*        MOVE CCVS-H-3  TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   NC1024.2
035300*        MOVE CCVS-C-1  TO DUMMY-RECORD  PERFORM WRT-LN           NC1024.2
035400*        MOVE CCVS-C-2  TO DUMMY-RECORD  PERFORM WRT-LN           NC1024.2
035500*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          NC1024.2
035600*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          NC1024.2
035700*        MOVE ZERO TO RECORD-COUNT.                               NC1024.2
035800     PERFORM WRT-LN.                                              NC1024.2
035900 WRT-LN.                                                          NC1024.2
036000     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               NC1024.2
036100     MOVE SPACE TO DUMMY-RECORD.                                  NC1024.2
036200 BLANK-LINE-PRINT.                                                NC1024.2
036300     PERFORM WRT-LN.                                              NC1024.2
036400 FAIL-ROUTINE.                                                    NC1024.2
036500     IF     COMPUTED-X NOT EQUAL TO SPACE                         NC1024.2
036600            GO TO FAIL-ROUTINE-WRITE.                             NC1024.2
036700     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.NC1024.2
036800     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC1024.2
036900     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   NC1024.2
037000     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1024.2
037100     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC1024.2
037200     GO TO  FAIL-ROUTINE-EX.                                      NC1024.2
037300 FAIL-ROUTINE-WRITE.                                              NC1024.2
037400     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         NC1024.2
037500     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 NC1024.2
037600     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. NC1024.2
037700     MOVE   SPACES TO COR-ANSI-REFERENCE.                         NC1024.2
037800 FAIL-ROUTINE-EX. EXIT.                                           NC1024.2
037900 BAIL-OUT.                                                        NC1024.2
038000     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   NC1024.2
038100     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           NC1024.2
038200 BAIL-OUT-WRITE.                                                  NC1024.2
038300     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  NC1024.2
038400     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC1024.2
038500     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1024.2
038600     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC1024.2
038700 BAIL-OUT-EX. EXIT.                                               NC1024.2
038800 CCVS1-EXIT.                                                      NC1024.2
038900     EXIT.                                                        NC1024.2
039000 SECT-NC102A-001 SECTION.                                         NC1024.2
039100 GO--INIT-F1-1.                                                   NC1024.2
039200     MOVE    "V1-88 6.14.4 GR1"  TO ANSI-REFERENCE.               NC1024.2
039300 GO--TEST-F1-1.                                                   NC1024.2
039400     GO TO    GO--PASS-F1-1.                                      NC1024.2
039500     PERFORM  FAIL.                                               NC1024.2
039600     GO TO    GO--WRITE-F1-1.                                     NC1024.2
039700 GO--DELETE-F1-1.                                                 NC1024.2
039800     PERFORM  DE-LETE.                                            NC1024.2
039900     GO TO    GO--WRITE-F1-1.                                     NC1024.2
040000 GO--PASS-F1-1.                                                   NC1024.2
040100     PERFORM  PASS.                                               NC1024.2
040200 GO--WRITE-F1-1.                                                  NC1024.2
040300     MOVE "GO TO " TO FEATURE.                                    NC1024.2
040400     MOVE "GO--TEST-F1-1" TO PAR-NAME.                            NC1024.2
040500     PERFORM  PRINT-DETAIL.                                       NC1024.2
040600 GO--INIT-F2-1.                                                   NC1024.2
040700     MOVE    "V1-88 6.14.4 GR3"  TO ANSI-REFERENCE.               NC1024.2
040800     MOVE    "GO--TEST-F2-1" TO PAR-NAME.                         NC1024.2
040900     MOVE     SPACE TO P-OR-F.                                    NC1024.2
041000     MOVE "GO TO DEPENDING" TO FEATURE.                           NC1024.2
041100     MOVE  0 TO GO-TO-DEPEND.                                     NC1024.2
041200 GO--TEST-F2-1.                                                   NC1024.2
041300     PERFORM  PRINT-DETAIL.                                       NC1024.2
041400     MOVE     SPACE TO FEATURE.                                   NC1024.2
041500     GO TO    GO--B                                               NC1024.2
041600              GO--D                                               NC1024.2
041700              GO--C DEPENDING ON GO-TO-DEPEND.                    NC1024.2
041800*    NOTE GO--TEST-F2-1 THRU GO--E TEST THE GO TO                 NC1024.2
041900*    DEPENDING OPTION FOR GO-TO-DEPEND EQUAL TO 0,1,3,2,4.        NC1024.2
042000*    NOTE INITIAL VALUE OF GO-TO-DEPEND IS ZERO.                  NC1024.2
042100     GO TO    GO--A.                                              NC1024.2
042200 GO--DELETE-F2-1.                                                 NC1024.2
042300     MOVE "GO--TEST-F2-1" TO PAR-NAME.                            NC1024.2
042400     PERFORM  DE-LETE.                                            NC1024.2
042500     GO TO    GO--WRITE-F2-1.                                     NC1024.2
042600 GO--A.                                                           NC1024.2
042700     MOVE "GO--A" TO PAR-NAME.                                    NC1024.2
042800     IF       GO-TO-DEPEND EQUAL TO 0                             NC1024.2
042900              PERFORM PASS                                        NC1024.2
043000              ADD 1 TO GO-TO-DEPEND                               NC1024.2
043100              GO TO GO--TEST-F2-1.                                NC1024.2
043200     IF       GO-TO-DEPEND GREATER THAN 3                         NC1024.2
043300              GO TO GO--E.                                        NC1024.2
043400     PERFORM  FAIL                                                NC1024.2
043500              MOVE 1 TO GO-TO-DEPEND                              NC1024.2
043600              GO TO GO--TEST-F2-1.                                NC1024.2
043700*    NOTE CONTROL SHOULD FALL THRU TO GO--A FOR GO-TO-DEPEND      NC1024.2
043800*    EQUAL TO 0, 4.                                               NC1024.2
043900 GO--B.                                                           NC1024.2
044000     MOVE "GO--B" TO PAR-NAME.                                    NC1024.2
044100     IF       GO-TO-DEPEND NOT EQUAL TO 1                         NC1024.2
044200              PERFORM FAIL                                        NC1024.2
044300              MOVE 3 TO GO-TO-DEPEND                              NC1024.2
044400              GO TO GO--TEST-F2-1.                                NC1024.2
044500     PERFORM  PASS.                                               NC1024.2
044600     ADD      2 TO GO-TO-DEPEND.                                  NC1024.2
044700     GO TO    GO--TEST-F2-1.                                      NC1024.2
044800 GO--C.                                                           NC1024.2
044900     MOVE "GO--C" TO PAR-NAME.                                    NC1024.2
045000     IF       GO-TO-DEPEND NOT EQUAL TO 3                         NC1024.2
045100              PERFORM FAIL                                        NC1024.2
045200              MOVE 2 TO GO-TO-DEPEND                              NC1024.2
045300              GO TO GO--TEST-F2-1.                                NC1024.2
045400     PERFORM  PASS.                                               NC1024.2
045500     SUBTRACT 1 FROM GO-TO-DEPEND.                                NC1024.2
045600     GO TO    GO--TEST-F2-1.                                      NC1024.2
045700 GO--D.                                                           NC1024.2
045800     MOVE "GO--D" TO PAR-NAME.                                    NC1024.2
045900     IF       GO-TO-DEPEND NOT EQUAL TO 2                         NC1024.2
046000              PERFORM FAIL                                        NC1024.2
046100              MOVE 4 TO GO-TO-DEPEND                              NC1024.2
046200              GO TO GO--TEST-F2-1.                                NC1024.2
046300     PERFORM  PASS.                                               NC1024.2
046400     ADD      2 TO GO-TO-DEPEND.                                  NC1024.2
046500     GO TO    GO--TEST-F2-1.                                      NC1024.2
046600 GO--E.                                                           NC1024.2
046700     MOVE "GO--E" TO PAR-NAME.                                    NC1024.2
046800     IF       GO-TO-DEPEND EQUAL TO 4                             NC1024.2
046900              PERFORM PASS                                        NC1024.2
047000              GO TO GO--WRITE-F2-1.                               NC1024.2
047100     PERFORM  FAIL.                                               NC1024.2
047200 GO--WRITE-F2-1.                                                  NC1024.2
047300     PERFORM  PRINT-DETAIL.                                       NC1024.2
047400 GO--INIT-F1-2.                                                   NC1024.2
047500     MOVE    "V1-88 6.14.4 GR1"  TO ANSI-REFERENCE.               NC1024.2
047600 GO--TEST-F1-2.                                                   NC1024.2
047700     GO TO    GO--PASS-F1-2.                                      NC1024.2
047800*    NOTE THAT GO--PASS-F1-2 IS A SECTION-NAME.                   NC1024.2
047900     PERFORM  FAIL.                                               NC1024.2
048000     GO TO    GO--WRITE-F1-2.                                     NC1024.2
048100 GO--DELETE-F1-2.                                                 NC1024.2
048200     PERFORM DE-LETE.                                             NC1024.2
048300     GO TO   GO--WRITE-F1-2.                                      NC1024.2
048400 GO--PASS-F1-2   SECTION.                                         NC1024.2
048500 GO--PAS-F1-2.                                                    NC1024.2
048600     PERFORM PASS.                                                NC1024.2
048700 GO--WRITE-F1-2.                                                  NC1024.2
048800     MOVE "GO TO" TO FEATURE.                                     NC1024.2
048900     MOVE "GO--TEST-F1-2" TO PAR-NAME.                            NC1024.2
049000     PERFORM PRINT-DETAIL.                                        NC1024.2
049100*                                                                 NC1024.2
049200*                                                                 NC1024.2
049300 GO--INIT-F2-2.                                                   NC1024.2
049400     MOVE    "V1-88 6.14.4 GR3"  TO ANSI-REFERENCE.               NC1024.2
049500     MOVE    1 TO GO-TO-DEEP.                                     NC1024.2
049600 GO--TEST-F2-2.                                                   NC1024.2
049700     GO TO   GO--PASS-F2-2                                        NC1024.2
049800             GO--FAIL-F2-2 DEPENDING ON GO-TO-DEEP.               NC1024.2
049900*    NOTE THAT GO--PASS-F2-2 IS A SECTION-NAME.                   NC1024.2
050000     GO TO   GO--FAIL-F2-2.                                       NC1024.2
050100 GO--DELETE-F2-2.                                                 NC1024.2
050200     PERFORM DE-LETE.                                             NC1024.2
050300     GO TO   GO--WRITE-F2-2.                                      NC1024.2
050400 GO--PASS-F2-2   SECTION.                                         NC1024.2
050500 GO--PAS-F2-2.                                                    NC1024.2
050600     IF      GO-TO-DEEP EQUAL TO 1                                NC1024.2
050700             PERFORM PASS                                         NC1024.2
050800             GO TO GO--WRITE-F2-2.                                NC1024.2
050900 GO--FAIL-F2-2.                                                   NC1024.2
051000     MOVE    GO-TO-DEEP TO COMPUTED-N.                            NC1024.2
051100     MOVE    1 TO CORRECT-N.                                      NC1024.2
051200     PERFORM FAIL.                                                NC1024.2
051300 GO--WRITE-F2-2.                                                  NC1024.2
051400     MOVE "GO TO DEPENDING" TO FEATURE.                           NC1024.2
051500     MOVE "GO--TEST-F2-2" TO PAR-NAME.                            NC1024.2
051600     PERFORM PRINT-DETAIL.                                        NC1024.2
051700 GO--INIT-F2-3.                                                   NC1024.2
051800     MOVE    "V1-88 6.14.4 GR3"  TO ANSI-REFERENCE.               NC1024.2
051900     MOVE    0 TO GO-TO-DEEP.                                     NC1024.2
052000     MOVE    2 TO GO-TO-DEPEND.                                   NC1024.2
052100 GO--TEST-F2-3.                                                   NC1024.2
052200     IF      GO-TO-DEPEND EQUAL TO 2 GO TO GO--A-F2-3 GO--B-F2-3  NC1024.2
052300             DEPENDING ON GO-TO-DEPEND ELSE GO TO GO--C-F2-3      NC1024.2
052400             GO--D-F2-3 GO--E-F2-3 DEPENDING GO-TO-DEPEND.        NC1024.2
052500 GO--DELETE-F2-3.                                                 NC1024.2
052600     PERFORM  DE-LETE.                                            NC1024.2
052700     GO       TO GO--WRITE-F2-3.                                  NC1024.2
052800 GO--A-F2-3.                                                      NC1024.2
052900     MOVE     1 TO GO-TO-DEEP.                                    NC1024.2
053000     GO       TO GO--F-F2-3.                                      NC1024.2
053100 GO--B-F2-3.                                                      NC1024.2
053200     MOVE     2 TO GO-TO-DEEP.                                    NC1024.2
053300     GO       TO GO--F-F2-3.                                      NC1024.2
053400 GO--C-F2-3.                                                      NC1024.2
053500     MOVE     3 TO GO-TO-DEEP.                                    NC1024.2
053600     GO       TO GO--F-F2-3.                                      NC1024.2
053700 GO--D-F2-3.                                                      NC1024.2
053800     MOVE     4 TO GO-TO-DEEP.                                    NC1024.2
053900     GO       TO GO--F-F2-3.                                      NC1024.2
054000 GO--E-F2-3.                                                      NC1024.2
054100     MOVE     5 TO GO-TO-DEEP.                                    NC1024.2
054200     GO       TO GO--F-F2-3.                                      NC1024.2
054300 GO--F-F2-3.                                                      NC1024.2
054400     IF       GO-TO-DEEP EQUAL TO 2                               NC1024.2
054500              PERFORM PASS GO TO GO--WRITE-F2-3.                  NC1024.2
054600 GO--FAIL-F2-3.                                                   NC1024.2
054700     PERFORM  FAIL.                                               NC1024.2
054800     MOVE     GO-TO-DEEP TO COMPUTED-N.                           NC1024.2
054900     MOVE     2 TO CORRECT-N.                                     NC1024.2
055000 GO--WRITE-F2-3.                                                  NC1024.2
055100     MOVE     "GO--TEST-F2-3 " TO PAR-NAME.                       NC1024.2
055200     PERFORM  PRINT-DETAIL.                                       NC1024.2
055300 GO--INIT-F2-4.                                                   NC1024.2
055400     MOVE    "V1-88 6.14.4 GR3"  TO ANSI-REFERENCE.               NC1024.2
055500     MOVE     0 TO GO-TO-DEEP.                                    NC1024.2
055600     MOVE     3 TO GO-TO-DEPEND.                                  NC1024.2
055700 GO--TEST-F2-4.                                                   NC1024.2
055800     IF       GO-TO-DEPEND EQUAL TO 2 GO TO GO--A-F2-4 GO--B-F2-4 NC1024.2
055900              DEPENDING ON GO-TO-DEPEND ELSE GO TO GO--C-F2-4     NC1024.2
056000              GO--D-F2-4 GO--E-F2-4 DEPENDING GO-TO-DEPEND.       NC1024.2
056100 GO--DELETE-F2-4.                                                 NC1024.2
056200     PERFORM  DE-LETE.                                            NC1024.2
056300     GO       TO GO--WRITE-F2-4.                                  NC1024.2
056400 GO--A-F2-4.                                                      NC1024.2
056500     MOVE     1 TO GO-TO-DEEP.                                    NC1024.2
056600     GO       TO GO--F-F2-4.                                      NC1024.2
056700 GO--B-F2-4.                                                      NC1024.2
056800     MOVE     2 TO GO-TO-DEEP.                                    NC1024.2
056900     GO       TO GO--F-F2-4.                                      NC1024.2
057000 GO--C-F2-4.                                                      NC1024.2
057100     MOVE     3 TO GO-TO-DEEP.                                    NC1024.2
057200     GO       TO GO--F-F2-4.                                      NC1024.2
057300 GO--D-F2-4.                                                      NC1024.2
057400     MOVE     4 TO GO-TO-DEEP.                                    NC1024.2
057500     GO       TO GO--F-F2-4.                                      NC1024.2
057600 GO--E-F2-4.                                                      NC1024.2
057700     MOVE     5 TO GO-TO-DEEP.                                    NC1024.2
057800     GO       TO GO--F-F2-4.                                      NC1024.2
057900 GO--F-F2-4.                                                      NC1024.2
058000     IF       GO-TO-DEEP EQUAL TO 5                               NC1024.2
058100              PERFORM PASS GO TO GO--WRITE-F2-4.                  NC1024.2
058200 GO--FAIL-F2-4.                                                   NC1024.2
058300     PERFORM  FAIL.                                               NC1024.2
058400     MOVE     GO-TO-DEEP TO COMPUTED-N.                           NC1024.2
058500     MOVE     5 TO CORRECT-N.                                     NC1024.2
058600 GO--WRITE-F2-4.                                                  NC1024.2
058700     MOVE     "GO--TEST-F2-4 " TO PAR-NAME.                       NC1024.2
058800     PERFORM  PRINT-DETAIL.                                       NC1024.2
058900*                                                                 NC1024.2
059000*                                                                 NC1024.2
059100 GO--INIT-F2-5.                                                   NC1024.2
059200     MOVE    "V1-88 6.14.4 GR3"  TO ANSI-REFERENCE.               NC1024.2
059300     MOVE     "87654321" TO GO-TABLE.                             NC1024.2
059400     MOVE     0 TO GO-TO-DEEP.                                    NC1024.2
059500 GO--TEST-F2-5.                                                   NC1024.2
059600     GO       TO GO--A-F2-5 GO--B-F2-5 GO--C-F2-5 DEPENDING ON    NC1024.2
059700              GO-SCRIPT (7).                                      NC1024.2
059800 GO--DELETE-F2-5.                                                 NC1024.2
059900     PERFORM  DE-LETE.                                            NC1024.2
060000     GO       TO GO--WRITE-F2-5.                                  NC1024.2
060100 GO--A-F2-5.                                                      NC1024.2
060200     MOVE     1 TO GO-TO-DEEP.                                    NC1024.2
060300     GO       TO GO--D-F2-5.                                      NC1024.2
060400 GO--B-F2-5.                                                      NC1024.2
060500     MOVE     2 TO GO-TO-DEEP.                                    NC1024.2
060600     GO       TO GO--D-F2-5.                                      NC1024.2
060700 GO--C-F2-5.                                                      NC1024.2
060800     MOVE     3 TO GO-TO-DEEP.                                    NC1024.2
060900     GO       TO GO--D-F2-5.                                      NC1024.2
061000 GO--D-F2-5.                                                      NC1024.2
061100     IF       GO-TO-DEEP EQUAL TO 2                               NC1024.2
061200              PERFORM PASS GO TO GO--WRITE-F2-5.                  NC1024.2
061300 GO--FAIL-F2-5.                                                   NC1024.2
061400     PERFORM  FAIL.                                               NC1024.2
061500     MOVE     GO-TO-DEEP TO COMPUTED-N.                           NC1024.2
061600     MOVE     2 TO CORRECT-N.                                     NC1024.2
061700 GO--WRITE-F2-5.                                                  NC1024.2
061800     MOVE     "GO--TEST-F2-5 " TO PAR-NAME.                       NC1024.2
061900     PERFORM  PRINT-DETAIL.                                       NC1024.2
062000*                                                                 NC1024.2
062100*                                                                 NC1024.2
062200 GO--INIT-F2-6.                                                   NC1024.2
062300*==--> SINGLE PROCEDURE GO DEPENDING <--==                        NC1024.2
062400     MOVE    "V1-88 6.14.4 GR3"  TO ANSI-REFERENCE.               NC1024.2
062500     MOVE    1 TO GO-TO-DEEP.                                     NC1024.2
062600 GO--TEST-F2-6.                                                   NC1024.2
062700     GO TO   GO--PASS-F2-6 DEPENDING ON GO-TO-DEEP.               NC1024.2
062800*    NOTE THAT GO--PASS-F2-6 IS A SECTION-NAME.                   NC1024.2
062900     GO TO   GO--FAIL-F2-6.                                       NC1024.2
063000 GO--DELETE-F2-6.                                                 NC1024.2
063100     PERFORM DE-LETE.                                             NC1024.2
063200     GO TO   GO--WRITE-F2-6.                                      NC1024.2
063300 GO--PASS-F2-6   SECTION.                                         NC1024.2
063400 GO--PAS-F2-6.                                                    NC1024.2
063500     IF      GO-TO-DEEP EQUAL TO 1                                NC1024.2
063600             PERFORM PASS                                         NC1024.2
063700             GO TO GO--WRITE-F2-6.                                NC1024.2
063800 GO--FAIL-F2-6.                                                   NC1024.2
063900     MOVE    GO-TO-DEEP TO COMPUTED-N.                            NC1024.2
064000     MOVE    1 TO CORRECT-N.                                      NC1024.2
064100     PERFORM FAIL.                                                NC1024.2
064200 GO--WRITE-F2-6.                                                  NC1024.2
064300     MOVE "GO TO DEPENDING" TO FEATURE.                           NC1024.2
064400     MOVE "GO--TEST-F2-6" TO PAR-NAME.                            NC1024.2
064500     PERFORM PRINT-DETAIL.                                        NC1024.2
064600*                                                                 NC1024.2
064700*                                                                 NC1024.2
064800 GO--INIT-F2-7.                                                   NC1024.2
064900     MOVE    "V1-88 6.14.4 GR3"  TO ANSI-REFERENCE.               NC1024.2
065000*    ==--> OPTIONAL "TO"  <--==                                   NC1024.2
065100     MOVE   "87654321" TO GO-TABLE.                               NC1024.2
065200     MOVE    0 TO GO-TO-DEEP.                                     NC1024.2
065300 GO--TEST-F2-7-0.                                                 NC1024.2
065400     GO      GO--A-F2-7 GO--B-F2-7 GO--C-F2-7 DEPENDING ON        NC1024.2
065500             GO-SCRIPT (7).                                       NC1024.2
065600 GO--DELETE-F2-7.                                                 NC1024.2
065700     PERFORM  DE-LETE.                                            NC1024.2
065800     GO       TO GO--WRITE-F2-7.                                  NC1024.2
065900 GO--A-F2-7.                                                      NC1024.2
066000     MOVE     1 TO GO-TO-DEEP.                                    NC1024.2
066100     GO       TO GO--D-F2-7.                                      NC1024.2
066200 GO--B-F2-7.                                                      NC1024.2
066300     MOVE     2 TO GO-TO-DEEP.                                    NC1024.2
066400     GO       TO GO--D-F2-7.                                      NC1024.2
066500 GO--C-F2-7.                                                      NC1024.2
066600     MOVE     3 TO GO-TO-DEEP.                                    NC1024.2
066700     GO       TO GO--D-F2-7.                                      NC1024.2
066800 GO--D-F2-7.                                                      NC1024.2
066900     IF       GO-TO-DEEP EQUAL TO 2                               NC1024.2
067000              PERFORM PASS GO TO GO--WRITE-F2-7.                  NC1024.2
067100 GO--FAIL-F2-7.                                                   NC1024.2
067200     PERFORM  FAIL.                                               NC1024.2
067300     MOVE     GO-TO-DEEP TO COMPUTED-N.                           NC1024.2
067400     MOVE     2 TO CORRECT-N.                                     NC1024.2
067500 GO--WRITE-F2-7.                                                  NC1024.2
067600     MOVE     "GO--TEST-F2-7 " TO PAR-NAME.                       NC1024.2
067700     PERFORM  PRINT-DETAIL.                                       NC1024.2
067800*                                                                 NC1024.2
067900*                                                                 NC1024.2
068000 GO--INIT-F1-3.                                                   NC1024.2
068100     MOVE    "V1-88 6.14.4 GR1"  TO ANSI-REFERENCE.               NC1024.2
068200 GOTO-TEST-F1-3.                                                  NC1024.2
068300     GO P2.                                                       NC1024.2
068400 GOTO-FAIL-F1-3.                                                  NC1024.2
068500     PERFORM FAIL.                                                NC1024.2
068600     GO TO GOTO-WRITE-F1-3.                                       NC1024.2
068700 GOTO-DELETE-F1-3.                                                NC1024.2
068800     PERFORM DE-LETE.                                             NC1024.2
068900     GO TO GOTO-WRITE-F1-3.                                       NC1024.2
069000 P2.                                                              NC1024.2
069100     PERFORM PASS.                                                NC1024.2
069200 GOTO-WRITE-F1-3.                                                 NC1024.2
069300     MOVE "GOTO-TEST-F1-3" TO PAR-NAME.                           NC1024.2
069400     MOVE "GO - NO OPTIONAL TO" TO FEATURE.                       NC1024.2
069500     PERFORM PRINT-DETAIL.                                        NC1024.2
069600 EXIT--INIT-GF-1.                                                 NC1024.2
069700     MOVE    "V1-87 6.13.2 "  TO ANSI-REFERENCE.                  NC1024.2
069800 EXIT-TEST-GF-1.                                                  NC1024.2
069900     GO TO    EXIT-CHECK-GF-1.                                    NC1024.2
070000 EXIT-DELETE-GF-1.                                                NC1024.2
070100     PERFORM  DE-LETE.                                            NC1024.2
070200     GO TO    EXIT-WRITE-GF-1.                                    NC1024.2
070300 EXIT-CHECK-GF-1.                                                 NC1024.2
070400     EXIT.                                                        NC1024.2
070500 EXIT-PASS-GF-1.                                                  NC1024.2
070600     PERFORM  PASS.                                               NC1024.2
070700 EXIT-WRITE-GF-1.                                                 NC1024.2
070800     MOVE "EXIT" TO FEATURE.                                      NC1024.2
070900     MOVE "EXIT-TEST-GF-1" TO PAR-NAME.                           NC1024.2
071000     PERFORM PRINT-DETAIL.                                        NC1024.2
071100 PFM-INIT-F1-1.                                                   NC1024.2
071200     MOVE    "V1-111 6.20.4 GR10"  TO ANSI-REFERENCE.             NC1024.2
071300     MOVE    1 TO PERFORM-KEY.                                    NC1024.2
071400 PFM-TEST-F1-1.                                                   NC1024.2
071500*    NOTE THIS TEST IS FOR OPTION 1 AND TESTS SIMPLE OUT OF       NC1024.2
071600*    LINE PERFORM.                                                NC1024.2
071700     PERFORM PFM-A.                                               NC1024.2
071800     IF      PERFORM1 EQUAL TO "ABC"                              NC1024.2
071900             PERFORM PASS                                         NC1024.2
072000     ELSE                                                         NC1024.2
072100             PERFORM FAIL.                                        NC1024.2
072200     GO TO   PFM-WRITE-F1-1.                                      NC1024.2
072300 PFM-DELETE-F1-1.                                                 NC1024.2
072400     PERFORM DE-LETE.                                             NC1024.2
072500 PFM-WRITE-F1-1.                                                  NC1024.2
072600     MOVE  "PERFORM" TO FEATURE.                                  NC1024.2
072700     MOVE  "PFM-TEST-F1-1" TO PAR-NAME.                           NC1024.2
072800     PERFORM PRINT-DETAIL.                                        NC1024.2
072900 PFM-INIT-F1-2.                                                   NC1024.2
073000     MOVE    "V1-111 6.20.4 GR10"  TO ANSI-REFERENCE.             NC1024.2
073100     MOVE    2 TO PERFORM-KEY.                                    NC1024.2
073200 PFM-TEST-F1-2.                                                   NC1024.2
073300*    NOTE THIS TEST IS DESIGNED TO TEST ENTERING A PROCEDURE      NC1024.2
073400*    IN LINE WHICH IS ALSO REFERENCED BY AN OUT OF LINE PERFORM.  NC1024.2
073500     GO TO    PFM-A.                                              NC1024.2
073600 PFM-DELETE-F1-2.                                                 NC1024.2
073700     PERFORM  DE-LETE.                                            NC1024.2
073800     GO TO    PFM-WRITE-F1-2.                                     NC1024.2
073900 PFM-A.                                                           NC1024.2
074000     IF       PERFORM-KEY EQUAL TO 1                              NC1024.2
074100              MOVE "ABC" TO PERFORM1                              NC1024.2
074200     ELSE                                                         NC1024.2
074300              MOVE "XYZ" TO PERFORM1.                             NC1024.2
074400 PFM-B.                                                           NC1024.2
074500     IF       PERFORM-KEY EQUAL TO 1                              NC1024.2
074600              PERFORM FAIL                                        NC1024.2
074700              PERFORM PRINT-DETAIL                                NC1024.2
074800              GO TO PFM-TEST-F1-2.                                NC1024.2
074900*    NOTE FOR PFM-TEST-F1-1 CONTROL SHOULD NOT BE TRANSFERRED     NC1024.2
075000*    TO THIS PARAGRAPH BUT FOR PFM-TEST-F1-2 IT SHOULD BE.        NC1024.2
075100     IF       PERFORM1 EQUAL TO "XYZ"                             NC1024.2
075200              PERFORM PASS                                        NC1024.2
075300              ELSE                                                NC1024.2
075400              PERFORM FAIL.                                       NC1024.2
075500 PFM-WRITE-F1-2.                                                  NC1024.2
075600     MOVE "PERFORM" TO FEATURE.                                   NC1024.2
075700     MOVE "PFM-TEST-F1-2" TO PAR-NAME.                            NC1024.2
075800     PERFORM PRINT-DETAIL.                                        NC1024.2
075900 PFM-INIT-F2-1.                                                   NC1024.2
076000     MOVE    "V1-108 6.20.2"  TO ANSI-REFERENCE.                  NC1024.2
076100     MOVE  3 TO THREE.                                            NC1024.2
076200 PFM-TEST-F2-1.                                                   NC1024.2
076300     PERFORM PFM-C 3 TIMES.                                       NC1024.2
076400     PERFORM PFM-C THREE TIMES.                                   NC1024.2
076500*    NOTE THIS TEST IS FOR OPTION 2.                              NC1024.2
076600     IF      PERFORM2 EQUAL TO 56                                 NC1024.2
076700             PERFORM PASS                                         NC1024.2
076800     ELSE                                                         NC1024.2
076900             PERFORM FAIL.                                        NC1024.2
077000     GO TO   PFM-WRITE-F2-1.                                      NC1024.2
077100 PFM-DELETE-F2-1.                                                 NC1024.2
077200     PERFORM DE-LETE.                                             NC1024.2
077300 PFM-WRITE-F2-1.                                                  NC1024.2
077400     MOVE   "PERFORM TIMES" TO FEATURE.                           NC1024.2
077500     MOVE   "PFM-TEST-F2-1" TO PAR-NAME.                          NC1024.2
077600     PERFORM PRINT-DETAIL.                                        NC1024.2
077700 PFM-INIT-F1-3.                                                   NC1024.2
077800     MOVE    "V1-111 6.20.4 GR10"  TO ANSI-REFERENCE.             NC1024.2
077900 PFM-TEST-F1-3.                                                   NC1024.2
078000     PERFORM PFM-E THRU PFM-H.                                    NC1024.2
078100*    NOTE THIS TEST IS FOR A NESTED PERFORM WITH THE INCLUDED     NC1024.2
078200*    PERFORM TOTALLY INCLUDED IN THE SEQUENCE REFERENCED BY THE   NC1024.2
078300*    FIRST PERFORM - IT ALSO TESTS THE EXIT VERB AND PERFORM      NC1024.2
078400*    THRU.                                                        NC1024.2
078500     IF       PERFORM1 NOT EQUAL TO "CSW"                         NC1024.2
078600              MOVE "CSW" TO CORRECT-A                             NC1024.2
078700              MOVE PERFORM1 TO COMPUTED-A                         NC1024.2
078800              PERFORM FAIL                                        NC1024.2
078900              GO TO PFM-WRITE-F1-3.                               NC1024.2
079000     IF       PERFORM4 EQUAL TO 70.0                              NC1024.2
079100              PERFORM PASS                                        NC1024.2
079200              ELSE                                                NC1024.2
079300              MOVE 70.0 TO CORRECT-N                              NC1024.2
079400              MOVE     PERFORM4 TO COMPUTED-N                     NC1024.2
079500              PERFORM FAIL.                                       NC1024.2
079600     GO TO    PFM-WRITE-F1-3.                                     NC1024.2
079700 PFM-DELETE-F1-3.                                                 NC1024.2
079800     PERFORM  DE-LETE.                                            NC1024.2
079900 PFM-WRITE-F1-3.                                                  NC1024.2
080000     MOVE "NESTED PERFORM THRU" TO FEATURE.                       NC1024.2
080100     MOVE "PFM-TEST-F1-3" TO PAR-NAME.                            NC1024.2
080200     PERFORM  PRINT-DETAIL.                                       NC1024.2
080300 PFM-INIT-F1-4.                                                   NC1024.2
080400     MOVE    "V1-111 6.20.4 GR10"  TO ANSI-REFERENCE.             NC1024.2
080500 PFM-TEST-F1-4.                                                   NC1024.2
080600     PERFORM  PFM-J.                                              NC1024.2
080700*    NOTE THIS TEST IS FOR A NESTED PERFORM WITH THE INCLUDED     NC1024.2
080800*    PERFORM TOTALLY EXCLUDED FROM THE SEQUENCE REFERENCED BY     NC1024.2
080900*    THE FIRST PERFORM.                                           NC1024.2
081000     IF       PERFORM1 EQUAL TO "YES"                             NC1024.2
081100              PERFORM PASS                                        NC1024.2
081200     ELSE                                                         NC1024.2
081300              MOVE "YES" TO CORRECT-A                             NC1024.2
081400              MOVE PERFORM1 TO COMPUTED-A                         NC1024.2
081500              PERFORM FAIL                                        NC1024.2
081600              GO TO PFM-WRITE-F1-4.                               NC1024.2
081700     IF       PERFORM2 EQUAL TO 312                               NC1024.2
081800              PERFORM PASS                                        NC1024.2
081900     ELSE                                                         NC1024.2
082000              MOVE 312 TO CORRECT-N                               NC1024.2
082100               MOVE PERFORM2 TO COMPUTED-N                        NC1024.2
082200              PERFORM FAIL.                                       NC1024.2
082300     GO TO    PFM-WRITE-F1-4.                                     NC1024.2
082400 PFM-DELETE-F1-4.                                                 NC1024.2
082500     PERFORM  DE-LETE.                                            NC1024.2
082600 PFM-WRITE-F1-4.                                                  NC1024.2
082700     MOVE "NESTED PERFORM" TO FEATURE.                            NC1024.2
082800     MOVE "PFM-TEST-F1-4" TO PAR-NAME.                            NC1024.2
082900     PERFORM  PRINT-DETAIL.                                       NC1024.2
083000 PFM-INIT-F1-5.                                                   NC1024.2
083100     MOVE    "V1-111 6.20.4 GR10"  TO ANSI-REFERENCE.             NC1024.2
083200 PFM-TEST-F1-5.                                                   NC1024.2
083300     PERFORM  PFM-N.                                              NC1024.2
083400*    NOTE PFM-N IS A SECTION-NAME.                                NC1024.2
083500     GO TO    PFM-WRITE-F1-5.                                     NC1024.2
083600 PFM-DELETE-F1-5.                                                 NC1024.2
083700     PERFORM  DE-LETE.                                            NC1024.2
083800 PFM-WRITE-F1-5.                                                  NC1024.2
083900     MOVE "PERFORM SECTION-NAME" TO FEATURE.                      NC1024.2
084000     MOVE "PFM-TEST-F1-5" TO PAR-NAME.                            NC1024.2
084100     PERFORM  PRINT-DETAIL.                                       NC1024.2
084200 PFM-INIT-F2-2.                                                   NC1024.2
084300     MOVE    "V1-108 6.20.2"  TO ANSI-REFERENCE.                  NC1024.2
084400 PFM-TEST-F2-2.                                                   NC1024.2
084500     PERFORM  PFM-V THRU PFM-Z 5 TIMES.                           NC1024.2
084600*        NOTE THESE ARE ALL EXIT PARAGRAPHS.                      NC1024.2
084700     PERFORM  PASS.                                               NC1024.2
084800     GO       TO PFM-WRITE-F2-2.                                  NC1024.2
084900 PFM-DELETE-F2-2.                                                 NC1024.2
085000     PERFORM  DE-LETE.                                            NC1024.2
085100 PFM-WRITE-F2-2.                                                  NC1024.2
085200     MOVE     "PERFORM EXIT PARAS" TO FEATURE.                    NC1024.2
085300     MOVE "PFM-TEST-F2-2" TO PAR-NAME.                            NC1024.2
085400     PERFORM  PRINT-DETAIL.                                       NC1024.2
085500 PFM-INIT-F1-6.                                                   NC1024.2
085600     MOVE    "V1-111 6.20.4 GR10"  TO ANSI-REFERENCE.             NC1024.2
085700     MOVE     ZERO TO P-COUNT.                                    NC1024.2
085800 PFM-TEST-F1-6.                                                   NC1024.2
085900     PERFORM  PFM-B-F1-6.                                         NC1024.2
086000     ADD      1 TO P-COUNT.                                       NC1024.2
086100     PERFORM  PFM-A-F1-6.                                         NC1024.2
086200     ADD      1 TO P-COUNT.                                       NC1024.2
086300 PFM-A-F1-6   SECTION.                                            NC1024.2
086400 PFM-B-F1-6.                                                      NC1024.2
086500     ADD      100 TO P-COUNT.                                     NC1024.2
086600 PFM-TESTT-F1-6 SECTION.                                          NC1024.2
086700 PFM-TESTTT-F1-6.                                                 NC1024.2
086800     IF       P-COUNT EQUAL TO 000302                             NC1024.2
086900              PERFORM PASS GO TO PFM-WRITE-F1-6.                  NC1024.2
087000     GO       TO PFM-FAIL-F1-6.                                   NC1024.2
087100 PFM-DELETE-F1-6.                                                 NC1024.2
087200     PERFORM  DE-LETE.                                            NC1024.2
087300     GO       TO PFM-WRITE-F1-6.                                  NC1024.2
087400 PFM-FAIL-F1-6.                                                   NC1024.2
087500     MOVE     P-COUNT TO COMPUTED-N.                              NC1024.2
087600     MOVE     000302 TO CORRECT-N.                                NC1024.2
087700     PERFORM  FAIL.                                               NC1024.2
087800 PFM-WRITE-F1-6.                                                  NC1024.2
087900     MOVE "PERFORM             " TO FEATURE.                      NC1024.2
088000     MOVE     "PFM-TEST-F1-6" TO PAR-NAME.                        NC1024.2
088100     PERFORM  PRINT-DETAIL.                                       NC1024.2
088200 PFM-INIT-F2-3.                                                   NC1024.2
088300     MOVE    "V1-108 6.20.2"  TO ANSI-REFERENCE.                  NC1024.2
088400     MOVE     ZERO TO P-COUNT                                     NC1024.2
088500     MOVE     2 TO ATWO-DS-01V00.                                 NC1024.2
088600 PFM-TEST-F2-3.                                                   NC1024.2
088700     PERFORM  PFM-B-F2-3    2 TIMES.                              NC1024.2
088800     ADD      1 TO P-COUNT.                                       NC1024.2
088900     PERFORM  PFM-A-F2-3   ATWO-DS-01V00 TIMES.                   NC1024.2
089000     ADD      1 TO P-COUNT.                                       NC1024.2
089100 PFM-A-F2-3   SECTION.                                            NC1024.2
089200 PFM-B-F2-3.                                                      NC1024.2
089300     ADD      100 TO P-COUNT.                                     NC1024.2
089400 PFM-TESTT-F2-3 SECTION.                                          NC1024.2
089500 PFM-TESTTT-F2-3.                                                 NC1024.2
089600     IF       P-COUNT EQUAL TO 000502                             NC1024.2
089700              PERFORM PASS GO TO PFM-WRITE-F2-3.                  NC1024.2
089800     GO       TO PFM-FAIL-F2-3.                                   NC1024.2
089900 PFM-DELETE-F2-3.                                                 NC1024.2
090000     PERFORM  DE-LETE.                                            NC1024.2
090100     GO       TO PFM-WRITE-F2-3.                                  NC1024.2
090200 PFM-FAIL-F2-3.                                                   NC1024.2
090300     MOVE     P-COUNT TO COMPUTED-N.                              NC1024.2
090400     MOVE     000502 TO CORRECT-N.                                NC1024.2
090500     PERFORM  FAIL.                                               NC1024.2
090600 PFM-WRITE-F2-3.                                                  NC1024.2
090700     MOVE "PERFORM TIMES       " TO FEATURE.                      NC1024.2
090800     MOVE     "PFM-TEST-F2-3" TO PAR-NAME.                        NC1024.2
090900     PERFORM  PRINT-DETAIL.                                       NC1024.2
091000 PFM-INIT-F1-7.                                                   NC1024.2
091100     MOVE    "V1-111 6.20.4 GR10"  TO ANSI-REFERENCE.             NC1024.2
091200     MOVE     ZERO TO P-COUNT.                                    NC1024.2
091300 PFM-TEST-F1-7.                                                   NC1024.2
091400     PERFORM  PFM-B-F1-7 THROUGH PFM-D-F1-7.                      NC1024.2
091500     ADD      1 TO P-COUNT                                        NC1024.2
091600     PERFORM  PFM-A-F1-7 THRU PFM-C-F1-7.                         NC1024.2
091700     ADD      1 TO P-COUNT.                                       NC1024.2
091800     PERFORM  PFM-A-F1-7 THRU PFM-D-F1-7.                         NC1024.2
091900     ADD      1 TO P-COUNT.                                       NC1024.2
092000     PERFORM  PFM-B-F1-7 THRU PFM-C-F1-7.                         NC1024.2
092100     ADD     1 TO P-COUNT.                                        NC1024.2
092200 PFM-A-F1-7 SECTION.                                              NC1024.2
092300 PFM-B-F1-7.                                                      NC1024.2
092400     ADD      100 TO P-COUNT.                                     NC1024.2
092500 PFM-C-F1-7 SECTION.                                              NC1024.2
092600 PFM-D-F1-7.                                                      NC1024.2
092700     ADD      10000 TO P-COUNT.                                   NC1024.2
092800 PFM-TESTT-F1-7 SECTION.                                          NC1024.2
092900 PFM-TESTTT-F1-7.                                                 NC1024.2
093000     IF       P-COUNT EQUAL TO 050504                             NC1024.2
093100              PERFORM PASS                                        NC1024.2
093200              GO TO PFM-WRITE-F1-7.                               NC1024.2
093300     GO       TO PFM-FAIL-F1-7.                                   NC1024.2
093400 PFM-DELETE-F1-7.                                                 NC1024.2
093500     PERFORM  DE-LETE.                                            NC1024.2
093600     GO       TO PFM-WRITE-F1-7.                                  NC1024.2
093700 PFM-FAIL-F1-7.                                                   NC1024.2
093800     MOVE     P-COUNT TO COMPUTED-N.                              NC1024.2
093900     MOVE     050504 TO CORRECT-N.                                NC1024.2
094000     PERFORM  FAIL.                                               NC1024.2
094100 PFM-WRITE-F1-7.                                                  NC1024.2
094200     MOVE "PERFORM THRU        " TO FEATURE.                      NC1024.2
094300     MOVE     "PFM-TEST-F1-7" TO PAR-NAME.                        NC1024.2
094400     PERFORM  PRINT-DETAIL.                                       NC1024.2
094500 PFM-INIT-F2-4.                                                   NC1024.2
094600     MOVE    "V1-108 6.20.2"  TO ANSI-REFERENCE.                  NC1024.2
094700     MOVE     ZERO TO P-COUNT.                                    NC1024.2
094800 PFM-TEST-F2-4.                                                   NC1024.2
094900     PERFORM  PFM-B-F2-4 THROUGH PFM-D-F2-4 2 TIMES.              NC1024.2
095000     ADD      1 TO P-COUNT.                                       NC1024.2
095100     PERFORM  PFM-A-F2-4 THRU PFM-C-F2-4 2 TIMES.                 NC1024.2
095200     ADD      1 TO P-COUNT.                                       NC1024.2
095300     PERFORM  PFM-A-F2-4 THRU PFM-D-F2-4 2 TIMES.                 NC1024.2
095400     ADD      1 TO P-COUNT.                                       NC1024.2
095500     PERFORM  PFM-B-F2-4 THRU PFM-D-F2-4 2 TIMES.                 NC1024.2
095600     ADD      1 TO P-COUNT.                                       NC1024.2
095700 PFM-A-F2-4 SECTION.                                              NC1024.2
095800 PFM-B-F2-4.                                                      NC1024.2
095900     ADD      100 TO P-COUNT.                                     NC1024.2
096000 PFM-C-F2-4 SECTION.                                              NC1024.2
096100 PFM-D-F2-4.                                                      NC1024.2
096200     ADD     10000 TO P-COUNT.                                    NC1024.2
096300 PFM-TESTT-F2-4 SECTION.                                          NC1024.2
096400 PFM-TESTTT-F2-4.                                                 NC1024.2
096500     IF       P-COUNT EQUAL TO 090904                             NC1024.2
096600              PERFORM PASS GO TO PFM-WRITE-F2-4.                  NC1024.2
096700     GO       TO PFM-FAIL-F2-4.                                   NC1024.2
096800 PFM-DELETE-F2-4.                                                 NC1024.2
096900     PERFORM  DE-LETE.                                            NC1024.2
097000     GO       TO PFM-WRITE-F2-4.                                  NC1024.2
097100 PFM-FAIL-F2-4.                                                   NC1024.2
097200     MOVE     P-COUNT TO COMPUTED-N.                              NC1024.2
097300     MOVE     090904 TO CORRECT-N.                                NC1024.2
097400     PERFORM  FAIL.                                               NC1024.2
097500 PFM-WRITE-F2-4.                                                  NC1024.2
097600     MOVE "PERFORM THRU, TIMES " TO FEATURE.                      NC1024.2
097700     MOVE     "PFM-TEST-F2-4" TO PAR-NAME.                        NC1024.2
097800     PERFORM  PRINT-DETAIL.                                       NC1024.2
097900 PFM-INIT-F1-8.                                                   NC1024.2
098000     MOVE    "V1-111 6.20.4 GR10"  TO ANSI-REFERENCE.             NC1024.2
098100     MOVE     ZERO TO P-COUNT.                                    NC1024.2
098200 PFM-TEST-F1-8.                                                   NC1024.2
098300     ADD      1 TO P-COUNT.                                       NC1024.2
098400     PERFORM  PFM-A-F1-8.                                         NC1024.2
098500     ADD      2 TO P-COUNT.                                       NC1024.2
098600     GO       TO PFM-TESTT-F1-8.                                  NC1024.2
098700 PFM-A-F1-8.                                                      NC1024.2
098800     ADD      10 TO P-COUNT.                                      NC1024.2
098900     PERFORM  PFM-B-F1-8.                                         NC1024.2
099000     ADD      20 TO P-COUNT.                                      NC1024.2
099100 PFM-B-F1-8.                                                      NC1024.2
099200     ADD      100 TO P-COUNT.                                     NC1024.2
099300     PERFORM  PFM-C-F1-8.                                         NC1024.2
099400     ADD      200 TO P-COUNT.                                     NC1024.2
099500 PFM-C-F1-8.                                                      NC1024.2
099600     ADD      1000 TO P-COUNT.                                    NC1024.2
099700     PERFORM  PFM-D-F1-8.                                         NC1024.2
099800     ADD      2000 TO P-COUNT.                                    NC1024.2
099900 PFM-D-F1-8.                                                      NC1024.2
100000     ADD      10000 TO P-COUNT.                                   NC1024.2
100100     PERFORM  PFM-E-F1-8.                                         NC1024.2
100200     ADD      20000 TO P-COUNT.                                   NC1024.2
100300 PFM-E-F1-8.                                                      NC1024.2
100400     ADD      100000 TO P-COUNT.                                  NC1024.2
100500 PFM-TESTT-F1-8.                                                  NC1024.2
100600     IF       P-COUNT EQUAL TO 133333                             NC1024.2
100700              PERFORM PASS GO TO PFM-WRITE-F1-8.                  NC1024.2
100800     GO       TO PFM-FAIL-F1-8.                                   NC1024.2
100900 PFM-DELETE-F1-8.                                                 NC1024.2
101000     PERFORM  DE-LETE.                                            NC1024.2
101100     GO       TO PFM-WRITE-F1-8.                                  NC1024.2
101200 PFM-FAIL-F1-8.                                                   NC1024.2
101300     MOVE     P-COUNT TO COMPUTED-N.                              NC1024.2
101400     MOVE     133333 TO CORRECT-N.                                NC1024.2
101500     PERFORM  FAIL.                                               NC1024.2
101600 PFM-WRITE-F1-8.                                                  NC1024.2
101700     MOVE "NESTED PERFORM      " TO FEATURE.                      NC1024.2
101800     MOVE     "PFM-TEST-F1-8" TO PAR-NAME.                        NC1024.2
101900     PERFORM  PRINT-DETAIL.                                       NC1024.2
102000 PFM-INIT-F2-5.                                                   NC1024.2
102100     MOVE    "V1-108 6.20.2"  TO ANSI-REFERENCE.                  NC1024.2
102200     MOVE     ZERO TO P-COUNT.                                    NC1024.2
102300 PFM-TEST-F2-5.                                                   NC1024.2
102400     PERFORM  PFM-A-F2-5 THRU PFM-B-F2-5.                         NC1024.2
102500     ADD      1 TO P-COUNT.                                       NC1024.2
102600     PERFORM  PFM-A-F2-5 THRU PFM-B-F2-5 2 TIMES.                 NC1024.2
102700     ADD      2 TO P-COUNT.                                       NC1024.2
102800 PFM-A-F2-5.                                                      NC1024.2
102900     ADD      100 TO P-COUNT.                                     NC1024.2
103000 PFM-B-F2-5.                                                      NC1024.2
103100     EXIT.                                                        NC1024.2
103200 PFM-TESTT-F2-5.                                                  NC1024.2
103300     IF       P-COUNT EQUAL TO 000403                             NC1024.2
103400              PERFORM PASS GO TO PFM-WRITE-F2-5.                  NC1024.2
103500     GO       TO PFM-FAIL-F2-5.                                   NC1024.2
103600 PFM-DELETE-F2-5.                                                 NC1024.2
103700     PERFORM  DE-LETE.                                            NC1024.2
103800     GO       TO PFM-WRITE-F2-5.                                  NC1024.2
103900 PFM-FAIL-F2-5.                                                   NC1024.2
104000     MOVE     P-COUNT TO COMPUTED-N.                              NC1024.2
104100     MOVE     000403 TO CORRECT-N.                                NC1024.2
104200     MOVE     "PERFORM WITH EXIT" TO FEATURE.                     NC1024.2
104300     PERFORM  FAIL.                                               NC1024.2
104400 PFM-WRITE-F2-5.                                                  NC1024.2
104500     MOVE     "PFM-TEST-F2-5" TO PAR-NAME.                        NC1024.2
104600     PERFORM  PRINT-DETAIL.                                       NC1024.2
104700 PFM-INIT-F1-9.                                                   NC1024.2
104800     MOVE    "V1-111 6.20.4 GR10"  TO ANSI-REFERENCE.             NC1024.2
104900     MOVE     SPACES TO PERFORM-HOLD.                             NC1024.2
105000 PFM-TEST-F1-9.                                                   NC1024.2
105100     PERFORM A101.                                                NC1024.2
105200     IF PERFORM-HOLD EQUAL TO "ABCDEFGHIJKLMNOPQRST"              NC1024.2
105300         PERFORM PASS                                             NC1024.2
105400         GO TO PFM-WRITE-F1-9.                                    NC1024.2
105500     MOVE "ABCDEFGHIJKLMNOPQRST" TO CORRECT-A.                    NC1024.2
105600     MOVE PERFORM-HOLD TO COMPUTED-A.                             NC1024.2
105700     PERFORM FAIL.                                                NC1024.2
105800     GO TO PFM-WRITE-F1-9.                                        NC1024.2
105900 PFM-DELETE-F1-9.                                                 NC1024.2
106000     PERFORM  DE-LETE.                                            NC1024.2
106100 PFM-WRITE-F1-9.                                                  NC1024.2
106200     MOVE     "PFM-TEST-F1-9" TO PAR-NAME.                        NC1024.2
106300     PERFORM  PRINT-DETAIL.                                       NC1024.2
106400 PFM-A-F1-10 SECTION.                                             NC1024.2
106500 PFM-INIT-F1-10.                                                  NC1024.2
106600     MOVE    "V1-111 6.20.4 GR10"  TO ANSI-REFERENCE.             NC1024.2
106700 PFM-TEST-F1-10.                                                  NC1024.2
106800     PERFORM  PFM-G-F1-10 THRU PFM-B-F1-10.                       NC1024.2
106900*        NOTE PERFORM SECTION-NAME THRU PARAGRAPH-NAME -- SECOND  NC1024.2
107000*             PROCEDURE-NAME PHYSICALLY PRECEEDS THE FIRST BUT    NC1024.2
107100*             LOGICALLY FOLLOWS IT.                               NC1024.2
107200     GO       TO PFM-WRITE-F1-10.                                 NC1024.2
107300 PFM-DELETE-F1-10.                                                NC1024.2
107400     PERFORM  DE-LETE.                                            NC1024.2
107500     GO       TO PFM-WRITE-F1-10.                                 NC1024.2
107600 PFM-B-F1-10.                                                     NC1024.2
107700     PERFORM  PASS.                                               NC1024.2
107800 PFM-C-F1-10.                                                     NC1024.2
107900     PERFORM  FAIL.                                               NC1024.2
108000     MOVE     "RETURN MECHANISM LOST" TO RE-MARK.                 NC1024.2
108100     GO       TO PFM-WRITE-F1-10.                                 NC1024.2
108200 PFM-D-F1-10.                                                     NC1024.2
108300     PERFORM  FAIL.                                               NC1024.2
108400     MOVE     "PERFORM GOT LOST IN GO TOS" TO RE-MARK.            NC1024.2
108500     GO       TO PFM-WRITE-F1-10.                                 NC1024.2
108600 PFM-E-F1-10.                                                     NC1024.2
108700     GO       TO PFM-L-F1-10.                                     NC1024.2
108800 PFM-F-F1-10.                                                     NC1024.2
108900     GO       TO PFM-D-F1-10.                                     NC1024.2
109000 PFM-G-F1-10 SECTION.                                             NC1024.2
109100 PFM-H-F1-10.                                                     NC1024.2
109200     GO       TO PFM-E-F1-10.                                     NC1024.2
109300 PFM-I-F1-10.                                                     NC1024.2
109400     GO       TO PFM-D-F1-10.                                     NC1024.2
109500*        NOTE SINCE THIS PARAGRAPH SHOULD NEVER BE ENTERED, IT IS NC1024.2
109600*             NOT POSSIBLE TO EXECUTE THE LAST SENTENCE IN PFM-   NC1024.2
109700*             G-F1-10 EVEN THOUGH PFM-G-F1-10 IS A SECTION WHICH  NC1024.2
109800*             IS THE OBJECT OF A PERFORM -- ALL THIS IS LEGAL.    NC1024.2
109900 PFM-J-F1-10 SECTION.                                             NC1024.2
110000                                                                  NC1024.2
110100 PFM-K-F1-10.                                                     NC1024.2
110200     PERFORM  FAIL.                                               NC1024.2
110300     MOVE     "PFM-K-F1-10 ENTERED" TO RE-MARK.                   NC1024.2
110400     GO       TO PFM-WRITE-F1-10.                                 NC1024.2
110500 PFM-L-F1-10.                                                     NC1024.2
110600     GO       TO PFM-B-F1-10.                                     NC1024.2
110700 PFM-WRITE-F1-10.                                                 NC1024.2
110800     MOVE     "PERFORM GO TO PARAS" TO FEATURE.                   NC1024.2
110900     MOVE     "PFM-TEST-F1-10" TO PAR-NAME.                       NC1024.2
111000     PERFORM  PRINT-DETAIL.                                       NC1024.2
111100 PFM-INIT-F2-5.                                                   NC1024.2
111200     MOVE    "V1-108 6.20.2"  TO ANSI-REFERENCE.                  NC1024.2
111300     MOVE     ZERO TO PERFORM2.                                   NC1024.2
111400 PFM-TEST-F2-6.                                                   NC1024.2
111500     PERFORM  PFM-S PERFORM2 TIMES.                               NC1024.2
111600     MOVE     -3 TO PERFORM2.                                     NC1024.2
111700     PERFORM  PFM-T PERFORM2 TIMES.                               NC1024.2
111800     MOVE     7 TO PERFORM5.                                      NC1024.2
111900     PERFORM  PFM-U PERFORM5 TIMES.                               NC1024.2
112000*        NOTE THE STANDARD SPECIFIES THAT THE COMPILER MUST       NC1024.2
112100*             SIMPLY IGNORE THE FIRST TWO PERFORM5, AND MUST      NC1024.2
112200*             PERFORM PFM-U SEVEN TIMES --- NOTE THAT PERFORM5    NC1024.2
112300*             IS INCREMENTED IN PFM-U, BUT THIS SHOULD HAVE NO    NC1024.2
112400*             EFFECT ON THE NUMBER OF TIMES PFM-U IS PERFORMED.   NC1024.2
112500     IF       PERFORM5 EQUAL TO 707                               NC1024.2
112600             PERFORM PASS GO TO PFM-WRITE-F2-6.                   NC1024.2
112700     GO      TO PFM-FAIL-F2-6.                                    NC1024.2
112800 PFM-DELETE-F2-6.                                                 NC1024.2
112900     PERFORM DE-LETE.                                             NC1024.2
113000     GO      TO PFM-WRITE-F2-6.                                   NC1024.2
113100 PFM-FAIL-F2-6.                                                   NC1024.2
113200     MOVE    PERFORM5 TO COMPUTED-N.                              NC1024.2
113300     MOVE    707 TO CORRECT-N.                                    NC1024.2
113400     PERFORM FAIL.                                                NC1024.2
113500 PFM-WRITE-F2-6.                                                  NC1024.2
113600     MOVE   "PERFORM ... TIMES" TO FEATURE.                       NC1024.2
113700     MOVE   "PFM-TEST-F2-6" TO PAR-NAME.                          NC1024.2
113800     PERFORM PRINT-DETAIL.                                        NC1024.2
113900*                                                                 NC1024.2
114000*                                                                 NC1024.2
114100 PFM-INIT-F1-11.                                                  NC1024.2
114200     MOVE    "V1-110 6.20.4 GR6"  TO ANSI-REFERENCE.              NC1024.2
114300*    ==--> IN LINE PERFORM <--==                                  NC1024.2
114400     MOVE    0   TO WRK-DU-2V0-1.                                 NC1024.2
114500     MOVE    0   TO WRK-DU-2V0-2.                                 NC1024.2
114600 PFM-TEST-F1-11-0.                                                NC1024.2
114700     PERFORM MOVE   88   TO WRK-DU-2V0-1                          NC1024.2
114800             MOVE   99   TO WRK-DU-2V0-2                          NC1024.2
114900     END-PERFORM.                                                 NC1024.2
115000 PFM-TEST-F1-11-1.                                                NC1024.2
115100     IF      WRK-DU-2V0-1 = 88                                    NC1024.2
115200             PERFORM PASS                                         NC1024.2
115300     ELSE                                                         NC1024.2
115400             MOVE    88           TO CORRECT-N                    NC1024.2
115500             MOVE    WRK-DU-2V0-1 TO COMPUTED-N                   NC1024.2
115600             PERFORM FAIL.                                        NC1024.2
115700     GO TO   PFM-WRITE-F1-11-1.                                   NC1024.2
115800 PFM-DELETE-F1-11-1.                                              NC1024.2
115900     PERFORM DE-LETE.                                             NC1024.2
116000 PFM-WRITE-F1-11-1.                                               NC1024.2
116100     MOVE   "PFM-TEST-F1-11-1" TO PAR-NAME.                       NC1024.2
116200     PERFORM PRINT-DETAIL.                                        NC1024.2
116300 PFM-TEST-F1-11-2.                                                NC1024.2
116400     IF      WRK-DU-2V0-2 = 99                                    NC1024.2
116500             PERFORM PASS                                         NC1024.2
116600     ELSE                                                         NC1024.2
116700             MOVE    99           TO CORRECT-N                    NC1024.2
116800             MOVE    WRK-DU-2V0-2 TO COMPUTED-N                   NC1024.2
116900             PERFORM FAIL.                                        NC1024.2
117000     GO TO   PFM-WRITE-F1-11-2.                                   NC1024.2
117100 PFM-DELETE-F1-11-2.                                              NC1024.2
117200     PERFORM DE-LETE.                                             NC1024.2
117300 PFM-WRITE-F1-11-2.                                               NC1024.2
117400     MOVE   "PFM-TEST-F1-11-2" TO PAR-NAME.                       NC1024.2
117500     PERFORM PRINT-DETAIL.                                        NC1024.2
117600*                                                                 NC1024.2
117700*                                                                 NC1024.2
117800 PFM-INIT-F2-7.                                                   NC1024.2
117900*    ==--> IN LINE PERFORM <--==                                  NC1024.2
118000     MOVE   "V1-110 6.20.4 GR6"  TO ANSI-REFERENCE.               NC1024.2
118100     MOVE   "PERFORM .... TIMES" TO FEATURE.                      NC1024.2
118200     MOVE    0   TO P-COUNT.                                      NC1024.2
118300     MOVE    0   TO WRK-DU-2V0-1.                                 NC1024.2
118400 PFM-TEST-F2-7-0.                                                 NC1024.2
118500     PERFORM 4 TIMES                                              NC1024.2
118600             ADD     3   TO P-COUNT                               NC1024.2
118700             ADD     4   TO P-COUNT                               NC1024.2
118800     END-PERFORM                                                  NC1024.2
118900     MOVE    77 TO WRK-DU-2V0-1.                                  NC1024.2
119000 PFM-TEST-F2-7-1.                                                 NC1024.2
119100     IF      P-COUNT = 28                                         NC1024.2
119200             PERFORM PASS                                         NC1024.2
119300     ELSE                                                         NC1024.2
119400             MOVE    28      TO CORRECT-N                         NC1024.2
119500             MOVE    P-COUNT TO COMPUTED-N                        NC1024.2
119600             PERFORM FAIL.                                        NC1024.2
119700     GO TO   PFM-WRITE-F2-7-1.                                    NC1024.2
119800 PFM-DELETE-F2-7-1.                                               NC1024.2
119900     PERFORM DE-LETE.                                             NC1024.2
120000 PFM-WRITE-F2-7-1.                                                NC1024.2
120100     MOVE   "PFM-TEST-F2-7-1" TO PAR-NAME.                        NC1024.2
120200     PERFORM PRINT-DETAIL.                                        NC1024.2
120300 PFM-TEST-F2-7-2.                                                 NC1024.2
120400     IF      WRK-DU-2V0-1 = 77                                    NC1024.2
120500             PERFORM PASS                                         NC1024.2
120600     ELSE                                                         NC1024.2
120700             MOVE    77           TO CORRECT-N                    NC1024.2
120800             MOVE    WRK-DU-2V0-1 TO COMPUTED-N                   NC1024.2
120900             PERFORM FAIL.                                        NC1024.2
121000     GO TO   PFM-WRITE-F2-7-2.                                    NC1024.2
121100 PFM-DELETE-F2-7-2.                                               NC1024.2
121200     PERFORM DE-LETE.                                             NC1024.2
121300 PFM-WRITE-F2-7-2.                                                NC1024.2
121400     MOVE   "PFM-TEST-F2-7-2" TO PAR-NAME.                        NC1024.2
121500     PERFORM PRINT-DETAIL.                                        NC1024.2
121600*                                                                 NC1024.2
121700*                                                                 NC1024.2
121800 PFM-INIT-F2-8.                                                   NC1024.2
121900*    ==--> IN LINE PERFORM <--==                                  NC1024.2
122000     MOVE   "V1-110 6.20.4 GR6"  TO ANSI-REFERENCE.               NC1024.2
122100     MOVE   "PERFORM .... TIMES" TO FEATURE.                      NC1024.2
122200     MOVE    0   TO P-COUNT.                                      NC1024.2
122300     MOVE    0   TO WRK-DU-2V0-1.                                 NC1024.2
122400     MOVE    4   TO WRK-DU-2V0-2.                                 NC1024.2
122500 PFM-TEST-F2-8-0.                                                 NC1024.2
122600     PERFORM WRK-DU-2V0-2 TIMES                                   NC1024.2
122700             ADD     3   TO P-COUNT                               NC1024.2
122800             ADD     4   TO P-COUNT                               NC1024.2
122900     END-PERFORM                                                  NC1024.2
123000     MOVE    77 TO WRK-DU-2V0-1.                                  NC1024.2
123100 PFM-TEST-F2-8-1.                                                 NC1024.2
123200     IF      P-COUNT = 28                                         NC1024.2
123300             PERFORM PASS                                         NC1024.2
123400     ELSE                                                         NC1024.2
123500             MOVE    28      TO CORRECT-N                         NC1024.2
123600             MOVE    P-COUNT TO COMPUTED-N                        NC1024.2
123700             PERFORM FAIL.                                        NC1024.2
123800     GO TO   PFM-WRITE-F2-8-1.                                    NC1024.2
123900 PFM-DELETE-F2-8-1.                                               NC1024.2
124000     PERFORM DE-LETE.                                             NC1024.2
124100 PFM-WRITE-F2-8-1.                                                NC1024.2
124200     MOVE   "PFM-TEST-F2-8-1" TO PAR-NAME.                        NC1024.2
124300     PERFORM PRINT-DETAIL.                                        NC1024.2
124400 PFM-TEST-F2-8-2.                                                 NC1024.2
124500     IF      WRK-DU-2V0-1 = 77                                    NC1024.2
124600             PERFORM PASS                                         NC1024.2
124700     ELSE                                                         NC1024.2
124800             MOVE    77           TO CORRECT-N                    NC1024.2
124900             MOVE    WRK-DU-2V0-1 TO COMPUTED-N                   NC1024.2
125000             PERFORM FAIL.                                        NC1024.2
125100     GO TO   PFM-WRITE-F2-8-2.                                    NC1024.2
125200 PFM-DELETE-F2-8-2.                                               NC1024.2
125300     PERFORM DE-LETE.                                             NC1024.2
125400 PFM-WRITE-F2-8-2.                                                NC1024.2
125500     MOVE   "PFM-TEST-F2-8-2" TO PAR-NAME.                        NC1024.2
125600     PERFORM PRINT-DETAIL.                                        NC1024.2
125700*                                                                 NC1024.2
125800*                                                                 NC1024.2
125900 PFM-INIT-F3-1.                                                   NC1024.2
126000     MOVE   "PERFORM UNTIL"  TO FEATURE.                          NC1024.2
126100     MOVE   "V1-108 6.20.2"  TO ANSI-REFERENCE.                   NC1024.2
126200     MOVE    1 TO PERFORM2.                                       NC1024.2
126300     MOVE    5 TO PERFORM3.                                       NC1024.2
126400 PFM-TEST-F3-1.                                                   NC1024.2
126500     PERFORM PFM-F3-1-A THRU PFM-F3-1-AA                          NC1024.2
126600       UNTIL PERFORM2 EQUAL TO 48.                                NC1024.2
126700*    NOTE IN THIS TEST THE CONDITION IS NOT SATISFIED             NC1024.2
126800*    ORIGINALLY WHEN THE PERFORM IS ENTERED.                      NC1024.2
126900     IF      PERFORM2 = 48                                        NC1024.2
127000             PERFORM PASS                                         NC1024.2
127100             GO TO   PFM-WRITE-F3-1.                              NC1024.2
127200     GO TO   PFM-FAIL-F3-1.                                       NC1024.2
127300 PFM-DELETE-F3-1.                                                 NC1024.2
127400     PERFORM DE-LETE.                                             NC1024.2
127500     GO TO   PFM-WRITE-F3-1.                                      NC1024.2
127600 PFM-F3-1-A.                                                      NC1024.2
127700     MULTIPLY PERFORM3 BY 6 GIVING PERFORM2.                      NC1024.2
127800 PFM-F3-1-AA.                                                     NC1024.2
127900     ADD     1 TO PERFORM3.                                       NC1024.2
128000 PFM-FAIL-F3-1.                                                   NC1024.2
128100     MOVE    PERFORM2 TO COMPUTED-N.                              NC1024.2
128200     MOVE    48 TO CORRECT-N.                                     NC1024.2
128300     PERFORM FAIL.                                                NC1024.2
128400 PFM-WRITE-F3-1.                                                  NC1024.2
128500     MOVE   "PFM-TEST-F3-1" TO PAR-NAME.                          NC1024.2
128600     PERFORM PRINT-DETAIL.                                        NC1024.2
128700*                                                                 NC1024.2
128800*                                                                 NC1024.2
128900 PFM-INIT-F3-2.                                                   NC1024.2
129000     MOVE   "V1-108 6.20.2"  TO ANSI-REFERENCE.                   NC1024.2
129100     MOVE    50 TO PERFORM2.                                      NC1024.2
129200*    NOTE: IN THIS TEST CONDITION IS SATISFIED WHEN PERFORM IS    NC1024.2
129300*    ENTERED AND CONTROL SHOULD NOT BE PASSED TO PFM-F3-2-C.      NC1024.2
129400 PFM-TEST-F3-2-0.                                                 NC1024.2
129500     PERFORM PFM-F3-2-C UNTIL PERFORM2 GREATER THAN 25.           NC1024.2
129600     IF      PERFORM2 EQUAL TO 50                                 NC1024.2
129700             PERFORM PASS                                         NC1024.2
129800             GO TO   PFM-WRITE-F3-2.                              NC1024.2
129900     GO TO   PFM-FAIL-F3-2.                                       NC1024.2
130000 PFM-DELETE-F3-2.                                                 NC1024.2
130100     PERFORM DE-LETE.                                             NC1024.2
130200     GO TO   PFM-WRITE-F3-2.                                      NC1024.2
130300 PFM-F3-2-C.                                                      NC1024.2
130400     ADD     1 TO PERFORM2.                                       NC1024.2
130500 PFM-FAIL-F3-2.                                                   NC1024.2
130600     MOVE    PERFORM2 TO COMPUTED-N.                              NC1024.2
130700     MOVE    50 TO CORRECT-N.                                     NC1024.2
130800     PERFORM FAIL.                                                NC1024.2
130900 PFM-WRITE-F3-2.                                                  NC1024.2
131000     MOVE   "PFM-TEST-F3-2" TO PAR-NAME.                          NC1024.2
131100     PERFORM PRINT-DETAIL.                                        NC1024.2
131200*                                                                 NC1024.2
131300*                                                                 NC1024.2
131400 PFM-INIT-F3-3.                                                   NC1024.2
131500     MOVE   "V1-108 6.20.2"  TO ANSI-REFERENCE.                   NC1024.2
131600     MOVE    ZERO TO WRK-DS-02V00.                                NC1024.2
131700 PFM-TEST-F3-3.                                                   NC1024.2
131800     PERFORM PFM-A-F3-3 THROUGH PFM-B-F3-3                        NC1024.2
131900       UNTIL WRK-DS-02V00 IS EQUAL TO 99.                         NC1024.2
132000     GO TO   PFM-TESTT-F3-3.                                      NC1024.2
132100 PFM-A-F3-3.                                                      NC1024.2
132200     EXIT.                                                        NC1024.2
132300 PFM-B-F3-3.                                                      NC1024.2
132400     ADD      1 TO WRK-DS-02V00.                                  NC1024.2
132500 PFM-TESTT-F3-3.                                                  NC1024.2
132600     IF      WRK-DS-02V00 EQUAL TO 99                             NC1024.2
132700             PERFORM PASS GO TO PFM-WRITE-F3-3.                   NC1024.2
132800     GO TO   PFM-FAIL-F3-3.                                       NC1024.2
132900 PFM-DELETE-F3-3.                                                 NC1024.2
133000     PERFORM DE-LETE.                                             NC1024.2
133100     GO TO   PFM-WRITE-F3-3.                                      NC1024.2
133200 PFM-FAIL-F3-3.                                                   NC1024.2
133300     MOVE    WRK-DS-02V00 TO COMPUTED-N.                          NC1024.2
133400     MOVE    99 TO CORRECT-N.                                     NC1024.2
133500     PERFORM FAIL.                                                NC1024.2
133600 PFM-WRITE-F3-3.                                                  NC1024.2
133700     MOVE   "PFM-TEST-F3-3  " TO PAR-NAME.                        NC1024.2
133800     PERFORM PRINT-DETAIL.                                        NC1024.2
133900*                                                                 NC1024.2
134000*                                                                 NC1024.2
134100 PFM-INIT-F3-4.                                                   NC1024.2
134200*    ==--> IN-LINE PERFORM <--==                                  NC1024.2
134300     MOVE   "V1-110 6.20.4 GR6"  TO ANSI-REFERENCE.               NC1024.2
134400     MOVE    ZERO TO WRK-DU-6V0-1.                                NC1024.2
134500 PFM-TEST-F3-4-0.                                                 NC1024.2
134600     PERFORM UNTIL WRK-DU-6V0-1 = 99                              NC1024.2
134700             ADD      6 TO   WRK-DU-6V0-1                         NC1024.2
134800             SUBTRACT 3 FROM WRK-DU-6V0-1                         NC1024.2
134900     END-PERFORM                                                  NC1024.2
135000     SUBTRACT 1 FROM WRK-DU-6V0-1.                                NC1024.2
135100 PFM-TESTT-F3-4-1.                                                NC1024.2
135200     IF      WRK-DU-6V0-1 EQUAL TO 98                             NC1024.2
135300             PERFORM PASS GO TO PFM-WRITE-F3-4.                   NC1024.2
135400     GO TO   PFM-FAIL-F3-4.                                       NC1024.2
135500 PFM-DELETE-F3-4.                                                 NC1024.2
135600     PERFORM DE-LETE.                                             NC1024.2
135700     GO TO   PFM-WRITE-F3-4.                                      NC1024.2
135800 PFM-FAIL-F3-4.                                                   NC1024.2
135900     MOVE    WRK-DU-6V0-1 TO COMPUTED-N.                          NC1024.2
136000     MOVE    98           TO CORRECT-N.                           NC1024.2
136100     PERFORM FAIL.                                                NC1024.2
136200 PFM-WRITE-F3-4.                                                  NC1024.2
136300     MOVE   "PFM-TEST-F3-4  " TO PAR-NAME.                        NC1024.2
136400     PERFORM PRINT-DETAIL.                                        NC1024.2
136500     GO TO   CCVS-EXIT.                                           NC1024.2
136600 A121.                                                            NC1024.2
136700     EXIT.                                                        NC1024.2
136800 A120.                                                            NC1024.2
136900     MOVE "T" TO TEST-LETTER (20).                                NC1024.2
137000     PERFORM A121.                                                NC1024.2
137100 A119.                                                            NC1024.2
137200     MOVE "S" TO TEST-LETTER (19).                                NC1024.2
137300     PERFORM A120.                                                NC1024.2
137400 A118.                                                            NC1024.2
137500     MOVE "R" TO TEST-LETTER (18).                                NC1024.2
137600     PERFORM A119.                                                NC1024.2
137700 A117.                                                            NC1024.2
137800     MOVE "Q" TO TEST-LETTER (17).                                NC1024.2
137900     PERFORM A118.                                                NC1024.2
138000 A116.                                                            NC1024.2
138100     MOVE "P" TO TEST-LETTER (16).                                NC1024.2
138200     PERFORM A117.                                                NC1024.2
138300 A115.                                                            NC1024.2
138400     MOVE "O" TO TEST-LETTER (15).                                NC1024.2
138500     PERFORM A116.                                                NC1024.2
138600 A114.                                                            NC1024.2
138700     MOVE "N" TO TEST-LETTER (14).                                NC1024.2
138800     PERFORM A115.                                                NC1024.2
138900 A113.                                                            NC1024.2
139000     MOVE "M" TO TEST-LETTER (13).                                NC1024.2
139100     PERFORM A114.                                                NC1024.2
139200 A112.                                                            NC1024.2
139300     MOVE "L" TO TEST-LETTER (12).                                NC1024.2
139400     PERFORM A113.                                                NC1024.2
139500 A111.                                                            NC1024.2
139600     MOVE "K" TO TEST-LETTER (11).                                NC1024.2
139700     PERFORM A112.                                                NC1024.2
139800 A110.                                                            NC1024.2
139900     MOVE "J" TO TEST-LETTER (10).                                NC1024.2
140000     PERFORM A111.                                                NC1024.2
140100 A109.                                                            NC1024.2
140200     MOVE "I" TO TEST-LETTER (9).                                 NC1024.2
140300     PERFORM A110.                                                NC1024.2
140400 A108.                                                            NC1024.2
140500     MOVE "H" TO TEST-LETTER (8).                                 NC1024.2
140600     PERFORM A109.                                                NC1024.2
140700 A107.                                                            NC1024.2
140800     MOVE "G" TO TEST-LETTER (7).                                 NC1024.2
140900     PERFORM A108.                                                NC1024.2
141000 A106.                                                            NC1024.2
141100     MOVE "F" TO TEST-LETTER (6).                                 NC1024.2
141200     PERFORM A107.                                                NC1024.2
141300 A105.                                                            NC1024.2
141400     MOVE "E" TO TEST-LETTER (5).                                 NC1024.2
141500     PERFORM A106.                                                NC1024.2
141600 A104.                                                            NC1024.2
141700     MOVE "D" TO TEST-LETTER (4).                                 NC1024.2
141800     PERFORM A105.                                                NC1024.2
141900 A103.                                                            NC1024.2
142000     MOVE "C" TO TEST-LETTER (3).                                 NC1024.2
142100     PERFORM A104.                                                NC1024.2
142200 A102.                                                            NC1024.2
142300     MOVE "B" TO TEST-LETTER (2).                                 NC1024.2
142400     PERFORM A103.                                                NC1024.2
142500 A101.                                                            NC1024.2
142600     MOVE "A" TO TEST-LETTER (1).                                 NC1024.2
142700     PERFORM A102.                                                NC1024.2
142800 PFM-C.                                                           NC1024.2
142900     ADD      6 TO PERFORM2.                                      NC1024.2
143000 PFM-D.                                                           NC1024.2
143100     PERFORM  FAIL.                                               NC1024.2
143200     GO TO    PFM-TEST-F1-3.                                      NC1024.2
143300*    NOTE CONTROL SHOULD NOT PASS TO THIS PARAGRAPH               NC1024.2
143400*    FROM THE PREVIOUS ONE.                                       NC1024.2
143500 PFM-E.                                                           NC1024.2
143600     MOVE "CSW" TO PERFORM1.                                      NC1024.2
143700     PERFORM  PFM-F THRU PFM-G.                                   NC1024.2
143800     SUBTRACT .8 FROM PERFORM4.                                   NC1024.2
143900     GO TO    PFM-H.                                              NC1024.2
144000 PFM-F.                                                           NC1024.2
144100     MOVE     60.5 TO PERFORM4.                                   NC1024.2
144200 PFM-G.                                                           NC1024.2
144300     ADD      10.3 TO PERFORM4.                                   NC1024.2
144400 PFM-H.                                                           NC1024.2
144500     EXIT.                                                        NC1024.2
144600 PFM-I.                                                           NC1024.2
144700     PERFORM  FAIL.                                               NC1024.2
144800     GO TO    PFM-WRITE-F3-4.                                     NC1024.2
144900*    NOTE CONTROL SHOULD NOT PASS TO THIS PARAGRAPH               NC1024.2
145000*    FROM THE PREVIOUS ONE.                                       NC1024.2
145100 PFM-J.                                                           NC1024.2
145200     MOVE "YES" TO PERFORM1.                                      NC1024.2
145300     PERFORM  PFM-L.                                              NC1024.2
145400     MULTIPLY 3 BY PERFORM2.                                      NC1024.2
145500 PFM-K.                                                           NC1024.2
145600     PERFORM  FAIL.                                               NC1024.2
145700     GO TO    PFM-WRITE-F1-4.                                     NC1024.2
145800*    NOTE CONTROL SHOULD NOT PASS TO THIS PARAGRAPH               NC1024.2
145900*    FROM THE PREVIOUS ONE.                                       NC1024.2
146000 PFM-L.                                                           NC1024.2
146100     MOVE     4 TO PERFORM2.                                      NC1024.2
146200     ADD      100 TO PERFORM2.                                    NC1024.2
146300 PFM-M.                                                           NC1024.2
146400     PERFORM  FAIL.                                               NC1024.2
146500     GO TO    PFM-WRITE-F1-4.                                     NC1024.2
146600*    NOTE CONTROL SHOULD NOT PASS TO THIS PARAGRAPH               NC1024.2
146700*    FROM THE PREVIOUS ONE.                                       NC1024.2
146800 PFM-N        SECTION.                                            NC1024.2
146900 PFM-O.                                                           NC1024.2
147000     PERFORM  FAIL.                                               NC1024.2
147100 PFM-P.                                                           NC1024.2
147200     SUBTRACT 1 FROM ERROR-COUNTER.                               NC1024.2
147300     PERFORM  PASS.                                               NC1024.2
147400 PFM-Q        SECTION.                                            NC1024.2
147500 PFM-R.                                                           NC1024.2
147600     PERFORM  FAIL.                                               NC1024.2
147700     GO TO    PFM-WRITE-F1-5.                                     NC1024.2
147800*    NOTE CONTROL SHOULD NOT PASS TO THIS PARAGRAPH FROM THE      NC1024.2
147900*        PREVIOUS ONE.                                            NC1024.2
148000 PFM-S.                                                           NC1024.2
148100     ADD      1 TO PERFORM5.                                      NC1024.2
148200 PFM-T.                                                           NC1024.2
148300     ADD      10 TO PERFORM5.                                     NC1024.2
148400 PFM-U.                                                           NC1024.2
148500     ADD      100 TO PERFORM5.                                    NC1024.2
148600     IF       PERFORM5 GREATER THAN 899                           NC1024.2
148700              MOVE PERFORM5 TO COMPUTED-N                         NC1024.2
148800              MOVE 707 TO CORRECT-N                               NC1024.2
148900              PERFORM FAIL                                        NC1024.2
149000              MOVE "PFM-TEST-F2-6" TO PAR-NAME                    NC1024.2
149100              MOVE "*** ABORTED ***   SEE PFM-U" TO RE-MARK       NC1024.2
149200              PERFORM PRINT-DETAIL                                NC1024.2
149300              PERFORM END-ROUTINE THRU END-ROUTINE-13             NC1024.2
149400              CLOSE PRINT-FILE                                    NC1024.2
149500              STOP RUN.                                           NC1024.2
149600 PFM-V.       EXIT.                                               NC1024.2
149700 PFM-W.       EXIT.                                               NC1024.2
149800 PFM-X.       EXIT.                                               NC1024.2
149900 PFM-Y.       EXIT.                                               NC1024.2
150000 PFM-Z.       EXIT.                                               NC1024.2
150100 CCVS-EXIT SECTION.                                               NC1024.2
150200 CCVS-999999.                                                     NC1024.2
150300     GO TO CLOSE-FILES.                                           NC1024.2
      *END-OF,NC102A                                                                  
