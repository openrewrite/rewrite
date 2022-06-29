      *HEADER,COBOL,NC243A                                                            
000100 IDENTIFICATION DIVISION.                                         NC2434.2
000200 PROGRAM-ID.                                                      NC2434.2
000300     NC243A.                                                      NC2434.2
000400                                                                  NC2434.2
000500****************************************************************  NC2434.2
000600*                                                              *  NC2434.2
000700*    VALIDATION FOR:-                                          *  NC2434.2
000800*                                                              *  NC2434.2
000900*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2434.2
001000*                                                              *  NC2434.2
001100*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC2434.2
001200*                                                              *  NC2434.2
001300****************************************************************  NC2434.2
001400*                                                              *  NC2434.2
001500*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  NC2434.2
001600*                                                              *  NC2434.2
001700*        X-55  - SYSTEM PRINTER NAME.                          *  NC2434.2
001800*        X-82  - SOURCE COMPUTER NAME.                         *  NC2434.2
001900*        X-83  - OBJECT COMPUTER NAME.                         *  NC2434.2
002000*                                                              *  NC2434.2
002100****************************************************************  NC2434.2
002200*                                                              *  NC2434.2
002300*    PROGRAM NC243A TESTS THE CONSTRUCTION AND ACCES OF A      *  NC2434.2
002400*    SEVEN-DIMENSIONAL TABLE.   THE CONSTRUCTION IS VIA        *  NC2434.2
002500*    SUBSCRIPTED LOOPS AND ACCESS IS BY FORMAT 4 "PERFORM"     *  NC2434.2
002600*    STATEMENTS USING INDICES.                                 *  NC2434.2
002700*                                                              *  NC2434.2
002800****************************************************************  NC2434.2
002900 ENVIRONMENT DIVISION.                                            NC2434.2
003000 CONFIGURATION SECTION.                                           NC2434.2
003100 SOURCE-COMPUTER.                                                 NC2434.2
003200     XXXXX082.                                                    NC2434.2
003300 OBJECT-COMPUTER.                                                 NC2434.2
003400     XXXXX083.                                                    NC2434.2
003500 INPUT-OUTPUT SECTION.                                            NC2434.2
003600 FILE-CONTROL.                                                    NC2434.2
003700     SELECT PRINT-FILE ASSIGN TO                                  NC2434.2
003800     XXXXX055.                                                    NC2434.2
003900 DATA DIVISION.                                                   NC2434.2
004000 FILE SECTION.                                                    NC2434.2
004100 FD  PRINT-FILE.                                                  NC2434.2
004200 01  PRINT-REC PICTURE X(120).                                    NC2434.2
004300 01  DUMMY-RECORD PICTURE X(120).                                 NC2434.2
004400 WORKING-STORAGE SECTION.                                         NC2434.2
004500 77  SUB-1              PICTURE S99  VALUE ZERO.                  NC2434.2
004600 77  SUB-2              PICTURE 99   VALUE ZERO.                  NC2434.2
004700 77  SUB-3              PICTURE 99   VALUE ZERO.                  NC2434.2
004800 77   TEST-CHECK PIC X(4) VALUE SPACE.                            NC2434.2
004900 77  CON-7              PICTURE 99  VALUE 07.                     NC2434.2
005000 77  CON-10             PICTURE 99  VALUE 10.                     NC2434.2
005100 77  ELEM-HOLD-AREA               PICTURE X(15)  VALUE SPACES.    NC2434.2
005200 77  CON-5              PICTURE 99  VALUE 05.                     NC2434.2
005300 77  SEC-HOLD-AREA                PICTURE X(11)  VALUE SPACES.    NC2434.2
005400 77  CON-6              PICTURE 99  VALUE 06.                     NC2434.2
005500 77  GRP-HOLD-AREA                PICTURE X(5)  VALUE SPACES.     NC2434.2
005600 77  N1                 PIC 9.                                    NC2434.2
005700 77  N2                 PIC 9.                                    NC2434.2
005800 77  N3                 PIC 9.                                    NC2434.2
005900 77  N4                 PIC 9.                                    NC2434.2
006000 77  N5                 PIC 9.                                    NC2434.2
006100 77  N6                 PIC 9.                                    NC2434.2
006200 77  N7                 PIC 9.                                    NC2434.2
006300 01  GRP-NAME.                                                    NC2434.2
006400     02  FILLER              PICTURE XXX    VALUE "GRP".          NC2434.2
006500     02  ADD-GRP             PICTURE 99     VALUE 01.             NC2434.2
006600                                                                  NC2434.2
006700 01  SEC-NAME.                                                    NC2434.2
006800     02  FILLER              PICTURE X(5)   VALUE "SEC (".        NC2434.2
006900     02  SEC-GRP             PICTURE 99     VALUE 00.             NC2434.2
007000     02  FILLER              PICTURE X      VALUE ",".            NC2434.2
007100     02  ADD-SEC             PICTURE 99     VALUE 01.             NC2434.2
007200     02  FILLER              PICTURE X      VALUE ")".            NC2434.2
007300                                                                  NC2434.2
007400 01  ELEM-NAME.                                                   NC2434.2
007500     02  FILLER              PICTURE X(6)   VALUE "ELEM (".       NC2434.2
007600     02  ELEM-GRP            PICTURE 99     VALUE 00.             NC2434.2
007700     02  FILLER              PICTURE X      VALUE ",".            NC2434.2
007800     02  ELEM-SEC            PICTURE 99     VALUE 00.             NC2434.2
007900     02  FILLER              PICTURE X      VALUE ",".            NC2434.2
008000     02  ADD-ELEM            PICTURE 99     VALUE 01.             NC2434.2
008100     02  FILLER              PICTURE X      VALUE ")".            NC2434.2
008200                                                                  NC2434.2
008300 01  3-DIMENSION-TBL.                                             NC2434.2
008400     02  GRP-ENTRY OCCURS 10 TIMES INDEXED BY IDX-1.              NC2434.2
008500         03  ENTRY-1         PICTURE X(5).                        NC2434.2
008600         03  GRP2-ENTRY OCCURS 10 TIMES INDEXED BY IDX-2.         NC2434.2
008700             04  ENTRY-2     PICTURE X(11).                       NC2434.2
008800             04  GRP3-ENTRY OCCURS 10 TIMES INDEXED BY IDX-3.     NC2434.2
008900                 05  ENTRY-3 PICTURE X(15).                       NC2434.2
009000                                                                  NC2434.2
009100 01  7-DIMENSION-TBL.                                             NC2434.2
009200   02  GRP-7-1-ENTRY             OCCURS 2 INDEXED BY X1.          NC2434.2
009300     03  ENTRY-7-1               PIC XX.                          NC2434.2
009400     03  GRP-7-2-ENTRY           OCCURS 2 INDEXED BY X2.          NC2434.2
009500       04  ENTRY-7-2             PIC XX.                          NC2434.2
009600       04  GRP-7-3-ENTRY         OCCURS 2 INDEXED BY X3.          NC2434.2
009700         05  ENTRY-7-3           PIC XX.                          NC2434.2
009800         05  GRP-7-4-ENTRY       OCCURS 2 INDEXED BY X4.          NC2434.2
009900           06  ENTRY-7-4         PIC XX.                          NC2434.2
010000           06  GRP-7-5-ENTRY     OCCURS 2 INDEXED BY X5.          NC2434.2
010100             07  ENTRY-7-5       PIC XX.                          NC2434.2
010200             07  GRP-7-6-ENTRY   OCCURS 2 INDEXED BY X6.          NC2434.2
010300               08  ENTRY-7-6     PIC XX.                          NC2434.2
010400               08  GRP-7-7-ENTRY OCCURS 2 INDEXED BY X7.          NC2434.2
010500                 09  ENTRY-7-7   PIC XX.                          NC2434.2
010600                                                                  NC2434.2
010700 01  WS-FLAG                     PIC X(5).                        NC2434.2
010800 01  TEST-RESULTS.                                                NC2434.2
010900     02 FILLER                   PIC X      VALUE SPACE.          NC2434.2
011000     02 FEATURE                  PIC X(20)  VALUE SPACE.          NC2434.2
011100     02 FILLER                   PIC X      VALUE SPACE.          NC2434.2
011200     02 P-OR-F                   PIC X(5)   VALUE SPACE.          NC2434.2
011300     02 FILLER                   PIC X      VALUE SPACE.          NC2434.2
011400     02  PAR-NAME.                                                NC2434.2
011500       03 FILLER                 PIC X(19)  VALUE SPACE.          NC2434.2
011600       03  PARDOT-X              PIC X      VALUE SPACE.          NC2434.2
011700       03 DOTVALUE               PIC 99     VALUE ZERO.           NC2434.2
011800     02 FILLER                   PIC X(8)   VALUE SPACE.          NC2434.2
011900     02 RE-MARK                  PIC X(61).                       NC2434.2
012000 01  TEST-COMPUTED.                                               NC2434.2
012100     02 FILLER                   PIC X(30)  VALUE SPACE.          NC2434.2
012200     02 FILLER                   PIC X(17)  VALUE                 NC2434.2
012300            "       COMPUTED=".                                   NC2434.2
012400     02 COMPUTED-X.                                               NC2434.2
012500     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          NC2434.2
012600     03 COMPUTED-N               REDEFINES COMPUTED-A             NC2434.2
012700                                 PIC -9(9).9(9).                  NC2434.2
012800     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         NC2434.2
012900     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     NC2434.2
013000     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     NC2434.2
013100     03       CM-18V0 REDEFINES COMPUTED-A.                       NC2434.2
013200         04 COMPUTED-18V0                    PIC -9(18).          NC2434.2
013300         04 FILLER                           PIC X.               NC2434.2
013400     03 FILLER PIC X(50) VALUE SPACE.                             NC2434.2
013500 01  TEST-CORRECT.                                                NC2434.2
013600     02 FILLER PIC X(30) VALUE SPACE.                             NC2434.2
013700     02 FILLER PIC X(17) VALUE "       CORRECT =".                NC2434.2
013800     02 CORRECT-X.                                                NC2434.2
013900     03 CORRECT-A                  PIC X(20) VALUE SPACE.         NC2434.2
014000     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      NC2434.2
014100     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         NC2434.2
014200     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     NC2434.2
014300     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     NC2434.2
014400     03      CR-18V0 REDEFINES CORRECT-A.                         NC2434.2
014500         04 CORRECT-18V0                     PIC -9(18).          NC2434.2
014600         04 FILLER                           PIC X.               NC2434.2
014700     03 FILLER PIC X(2) VALUE SPACE.                              NC2434.2
014800     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     NC2434.2
014900 01  CCVS-C-1.                                                    NC2434.2
015000     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PANC2434.2
015100-    "SS  PARAGRAPH-NAME                                          NC2434.2
015200-    "       REMARKS".                                            NC2434.2
015300     02 FILLER                     PIC X(20)    VALUE SPACE.      NC2434.2
015400 01  CCVS-C-2.                                                    NC2434.2
015500     02 FILLER                     PIC X        VALUE SPACE.      NC2434.2
015600     02 FILLER                     PIC X(6)     VALUE "TESTED".   NC2434.2
015700     02 FILLER                     PIC X(15)    VALUE SPACE.      NC2434.2
015800     02 FILLER                     PIC X(4)     VALUE "FAIL".     NC2434.2
015900     02 FILLER                     PIC X(94)    VALUE SPACE.      NC2434.2
016000 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       NC2434.2
016100 01  REC-CT                        PIC 99       VALUE ZERO.       NC2434.2
016200 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       NC2434.2
016300 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       NC2434.2
016400 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       NC2434.2
016500 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       NC2434.2
016600 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       NC2434.2
016700 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       NC2434.2
016800 01  L4-HOLD                       PIC XX       VALUE SPACE.      NC2434.2
016900 01  L5-HOLD                       PIC XX       VALUE SPACE.      NC2434.2
017000 01  L6-HOLD                       PIC XX       VALUE SPACE.      NC2434.2
017100 01  L7-HOLD                       PIC XX       VALUE SPACE.      NC2434.2
017200 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      NC2434.2
017300 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       NC2434.2
017400 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     NC2434.2
017500 01  CCVS-H-1.                                                    NC2434.2
017600     02  FILLER                    PIC X(39)    VALUE SPACES.     NC2434.2
017700     02  FILLER                    PIC X(42)    VALUE             NC2434.2
017800     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 NC2434.2
017900     02  FILLER                    PIC X(39)    VALUE SPACES.     NC2434.2
018000 01  CCVS-H-2A.                                                   NC2434.2
018100   02  FILLER                        PIC X(40)  VALUE SPACE.      NC2434.2
018200   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  NC2434.2
018300   02  FILLER                        PIC XXXX   VALUE             NC2434.2
018400     "4.2 ".                                                      NC2434.2
018500   02  FILLER                        PIC X(28)  VALUE             NC2434.2
018600            " COPY - NOT FOR DISTRIBUTION".                       NC2434.2
018700   02  FILLER                        PIC X(41)  VALUE SPACE.      NC2434.2
018800                                                                  NC2434.2
018900 01  CCVS-H-2B.                                                   NC2434.2
019000   02  FILLER                        PIC X(15)  VALUE             NC2434.2
019100            "TEST RESULT OF ".                                    NC2434.2
019200   02  TEST-ID                       PIC X(9).                    NC2434.2
019300   02  FILLER                        PIC X(4)   VALUE             NC2434.2
019400            " IN ".                                               NC2434.2
019500   02  FILLER                        PIC X(12)  VALUE             NC2434.2
019600     " HIGH       ".                                              NC2434.2
019700   02  FILLER                        PIC X(22)  VALUE             NC2434.2
019800            " LEVEL VALIDATION FOR ".                             NC2434.2
019900   02  FILLER                        PIC X(58)  VALUE             NC2434.2
020000     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2434.2
020100 01  CCVS-H-3.                                                    NC2434.2
020200     02  FILLER                      PIC X(34)  VALUE             NC2434.2
020300            " FOR OFFICIAL USE ONLY    ".                         NC2434.2
020400     02  FILLER                      PIC X(58)  VALUE             NC2434.2
020500     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC2434.2
020600     02  FILLER                      PIC X(28)  VALUE             NC2434.2
020700            "  COPYRIGHT   1985 ".                                NC2434.2
020800 01  CCVS-E-1.                                                    NC2434.2
020900     02 FILLER                       PIC X(52)  VALUE SPACE.      NC2434.2
021000     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              NC2434.2
021100     02 ID-AGAIN                     PIC X(9).                    NC2434.2
021200     02 FILLER                       PIC X(45)  VALUE SPACES.     NC2434.2
021300 01  CCVS-E-2.                                                    NC2434.2
021400     02  FILLER                      PIC X(31)  VALUE SPACE.      NC2434.2
021500     02  FILLER                      PIC X(21)  VALUE SPACE.      NC2434.2
021600     02 CCVS-E-2-2.                                               NC2434.2
021700         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      NC2434.2
021800         03 FILLER                   PIC X      VALUE SPACE.      NC2434.2
021900         03 ENDER-DESC               PIC X(44)  VALUE             NC2434.2
022000            "ERRORS ENCOUNTERED".                                 NC2434.2
022100 01  CCVS-E-3.                                                    NC2434.2
022200     02  FILLER                      PIC X(22)  VALUE             NC2434.2
022300            " FOR OFFICIAL USE ONLY".                             NC2434.2
022400     02  FILLER                      PIC X(12)  VALUE SPACE.      NC2434.2
022500     02  FILLER                      PIC X(58)  VALUE             NC2434.2
022600     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2434.2
022700     02  FILLER                      PIC X(13)  VALUE SPACE.      NC2434.2
022800     02 FILLER                       PIC X(15)  VALUE             NC2434.2
022900             " COPYRIGHT 1985".                                   NC2434.2
023000 01  CCVS-E-4.                                                    NC2434.2
023100     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      NC2434.2
023200     02 FILLER                       PIC X(4)   VALUE " OF ".     NC2434.2
023300     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      NC2434.2
023400     02 FILLER                       PIC X(40)  VALUE             NC2434.2
023500      "  TESTS WERE EXECUTED SUCCESSFULLY".                       NC2434.2
023600 01  XXINFO.                                                      NC2434.2
023700     02 FILLER                       PIC X(19)  VALUE             NC2434.2
023800            "*** INFORMATION ***".                                NC2434.2
023900     02 INFO-TEXT.                                                NC2434.2
024000       04 FILLER                     PIC X(8)   VALUE SPACE.      NC2434.2
024100       04 XXCOMPUTED                 PIC X(20).                   NC2434.2
024200       04 FILLER                     PIC X(5)   VALUE SPACE.      NC2434.2
024300       04 XXCORRECT                  PIC X(20).                   NC2434.2
024400     02 INF-ANSI-REFERENCE           PIC X(48).                   NC2434.2
024500 01  HYPHEN-LINE.                                                 NC2434.2
024600     02 FILLER  PIC IS X VALUE IS SPACE.                          NC2434.2
024700     02 FILLER  PIC IS X(65)    VALUE IS "************************NC2434.2
024800-    "*****************************************".                 NC2434.2
024900     02 FILLER  PIC IS X(54)    VALUE IS "************************NC2434.2
025000-    "******************************".                            NC2434.2
025100 01  CCVS-PGM-ID                     PIC X(9)   VALUE             NC2434.2
025200     "NC243A".                                                    NC2434.2
025300 PROCEDURE DIVISION.                                              NC2434.2
025400 CCVS1 SECTION.                                                   NC2434.2
025500 OPEN-FILES.                                                      NC2434.2
025600     OPEN     OUTPUT PRINT-FILE.                                  NC2434.2
025700     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   NC2434.2
025800     MOVE    SPACE TO TEST-RESULTS.                               NC2434.2
025900     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             NC2434.2
026000     GO TO CCVS1-EXIT.                                            NC2434.2
026100 CLOSE-FILES.                                                     NC2434.2
026200     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   NC2434.2
026300 TERMINATE-CCVS.                                                  NC2434.2
026400*    EXIT PROGRAM.                                                NC2434.2
026500*TERMINATE-CALL.                                                  NC2434.2
026600     STOP     RUN.                                                NC2434.2
026700 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         NC2434.2
026800 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           NC2434.2
026900 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          NC2434.2
027000 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      NC2434.2
027100     MOVE "****TEST DELETED****" TO RE-MARK.                      NC2434.2
027200 PRINT-DETAIL.                                                    NC2434.2
027300     IF REC-CT NOT EQUAL TO ZERO                                  NC2434.2
027400             MOVE "." TO PARDOT-X                                 NC2434.2
027500             MOVE REC-CT TO DOTVALUE.                             NC2434.2
027600     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      NC2434.2
027700     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               NC2434.2
027800        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 NC2434.2
027900          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 NC2434.2
028000     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              NC2434.2
028100     MOVE SPACE TO CORRECT-X.                                     NC2434.2
028200     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         NC2434.2
028300     MOVE     SPACE TO RE-MARK.                                   NC2434.2
028400 HEAD-ROUTINE.                                                    NC2434.2
028500     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC2434.2
028600     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC2434.2
028700     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC2434.2
028800     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC2434.2
028900 COLUMN-NAMES-ROUTINE.                                            NC2434.2
029000     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2434.2
029100     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2434.2
029200     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        NC2434.2
029300 END-ROUTINE.                                                     NC2434.2
029400     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.NC2434.2
029500 END-RTN-EXIT.                                                    NC2434.2
029600     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2434.2
029700 END-ROUTINE-1.                                                   NC2434.2
029800      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      NC2434.2
029900      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               NC2434.2
030000      ADD PASS-COUNTER TO ERROR-HOLD.                             NC2434.2
030100*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   NC2434.2
030200      MOVE PASS-COUNTER TO CCVS-E-4-1.                            NC2434.2
030300      MOVE ERROR-HOLD TO CCVS-E-4-2.                              NC2434.2
030400      MOVE CCVS-E-4 TO CCVS-E-2-2.                                NC2434.2
030500      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           NC2434.2
030600  END-ROUTINE-12.                                                 NC2434.2
030700      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        NC2434.2
030800     IF       ERROR-COUNTER IS EQUAL TO ZERO                      NC2434.2
030900         MOVE "NO " TO ERROR-TOTAL                                NC2434.2
031000         ELSE                                                     NC2434.2
031100         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       NC2434.2
031200     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           NC2434.2
031300     PERFORM WRITE-LINE.                                          NC2434.2
031400 END-ROUTINE-13.                                                  NC2434.2
031500     IF DELETE-COUNTER IS EQUAL TO ZERO                           NC2434.2
031600         MOVE "NO " TO ERROR-TOTAL  ELSE                          NC2434.2
031700         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      NC2434.2
031800     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   NC2434.2
031900     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2434.2
032000      IF   INSPECT-COUNTER EQUAL TO ZERO                          NC2434.2
032100          MOVE "NO " TO ERROR-TOTAL                               NC2434.2
032200      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   NC2434.2
032300      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            NC2434.2
032400      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          NC2434.2
032500     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2434.2
032600 WRITE-LINE.                                                      NC2434.2
032700     ADD 1 TO RECORD-COUNT.                                       NC2434.2
032800*    IF RECORD-COUNT GREATER 50                                   NC2434.2
032900*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          NC2434.2
033000*        MOVE SPACE TO DUMMY-RECORD                               NC2434.2
033100*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  NC2434.2
033200*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             NC2434.2
033300*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     NC2434.2
033400*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          NC2434.2
033500*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          NC2434.2
033600*        MOVE ZERO TO RECORD-COUNT.                               NC2434.2
033700     PERFORM WRT-LN.                                              NC2434.2
033800 WRT-LN.                                                          NC2434.2
033900     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               NC2434.2
034000     MOVE SPACE TO DUMMY-RECORD.                                  NC2434.2
034100 BLANK-LINE-PRINT.                                                NC2434.2
034200     PERFORM WRT-LN.                                              NC2434.2
034300 FAIL-ROUTINE.                                                    NC2434.2
034400     IF   COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE. NC2434.2
034500     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.NC2434.2
034600     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC2434.2
034700     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   NC2434.2
034800     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2434.2
034900     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC2434.2
035000     GO TO  FAIL-ROUTINE-EX.                                      NC2434.2
035100 FAIL-ROUTINE-WRITE.                                              NC2434.2
035200     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         NC2434.2
035300     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 NC2434.2
035400     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. NC2434.2
035500     MOVE   SPACES TO COR-ANSI-REFERENCE.                         NC2434.2
035600 FAIL-ROUTINE-EX. EXIT.                                           NC2434.2
035700 BAIL-OUT.                                                        NC2434.2
035800     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   NC2434.2
035900     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           NC2434.2
036000 BAIL-OUT-WRITE.                                                  NC2434.2
036100     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  NC2434.2
036200     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC2434.2
036300     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2434.2
036400     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC2434.2
036500 BAIL-OUT-EX. EXIT.                                               NC2434.2
036600 CCVS1-EXIT.                                                      NC2434.2
036700     EXIT.                                                        NC2434.2
036800 SECT-NC243A-001 SECTION.                                         NC2434.2
036900 TH-17-001.                                                       NC2434.2
037000                                                                  NC2434.2
037100 BUILD-LEVEL-1.                                                   NC2434.2
037200     ADD 1 TO SUB-1.                                              NC2434.2
037300     IF SUB-1 = 11 GO TO CHECK-ENTRIES.                           NC2434.2
037400     MOVE GRP-NAME TO ENTRY-1 (SUB-1).                            NC2434.2
037500     ADD 1 TO ADD-GRP.                                            NC2434.2
037600                                                                  NC2434.2
037700 BUILD-LEVEL-2.                                                   NC2434.2
037800     ADD 1 TO SUB-2.                                              NC2434.2
037900     IF SUB-2 = 11                                                NC2434.2
038000         MOVE ZERO TO SUB-2                                       NC2434.2
038100         MOVE 01 TO ADD-SEC                                       NC2434.2
038200         GO TO BUILD-LEVEL-1.                                     NC2434.2
038300     MOVE SUB-1 TO SEC-GRP.                                       NC2434.2
038400     MOVE SEC-NAME TO ENTRY-2 (SUB-1, SUB-2).                     NC2434.2
038500     ADD 1 TO ADD-SEC.                                            NC2434.2
038600                                                                  NC2434.2
038700 BUILD-LEVEL-3.                                                   NC2434.2
038800     ADD 1 TO SUB-3.                                              NC2434.2
038900     IF SUB-3 = 11                                                NC2434.2
039000         MOVE ZERO TO SUB-3                                       NC2434.2
039100              MOVE 01 TO ADD-ELEM                                 NC2434.2
039200              GO TO BUILD-LEVEL-2.                                NC2434.2
039300     MOVE SUB-1 TO ELEM-GRP.                                      NC2434.2
039400     MOVE SUB-2 TO ELEM-SEC.                                      NC2434.2
039500     MOVE ELEM-NAME TO ENTRY-3 (SUB-1, SUB-2, SUB-3).             NC2434.2
039600     ADD 1 TO ADD-ELEM.                                           NC2434.2
039700     GO TO BUILD-LEVEL-3.                                         NC2434.2
039800                                                                  NC2434.2
039900 CHECK-ENTRIES.                                                   NC2434.2
040000     MOVE "PERFORM VARYING LEV1" TO FEATURE.                      NC2434.2
040100     MOVE "CHECK-ENTRIES       " TO PAR-NAME.                     NC2434.2
040200     MOVE SPACES TO TEST-CHECK.                                   NC2434.2
040300     MOVE "GRP05" TO GRP-HOLD-AREA.                               NC2434.2
040400     PERFORM FIND-LEVEL-1-ENTRY VARYING IDX-1 FROM 1 BY 1         NC2434.2
040500         UNTIL IDX-1 GREATER 10.                                  NC2434.2
040600     IF TEST-CHECK = "PASS" GO TO LEVEL-1-TEST-2.                 NC2434.2
040700     MOVE GRP-HOLD-AREA TO CORRECT-A.                             NC2434.2
040800     MOVE ENTRY-1 (05) TO COMPUTED-A.                             NC2434.2
040900                                                                  NC2434.2
041000     MOVE "PERFORM VARYING USING INDEX" TO RE-MARK.               NC2434.2
041100     PERFORM FAIL-TH.                                             NC2434.2
041200                                                                  NC2434.2
041300 LEVEL-1-TEST-2.                                                  NC2434.2
041400     MOVE "GRP10" TO GRP-HOLD-AREA.                               NC2434.2
041500     MOVE "LEVEL-1-TEST-2      " TO PAR-NAME.                     NC2434.2
041600     MOVE SPACES TO TEST-CHECK.                                   NC2434.2
041700     PERFORM FIND-LEVEL-1-ENTRY VARYING IDX-1 FROM 1 BY 1         NC2434.2
041800         UNTIL IDX-1 GREATER 10.                                  NC2434.2
041900     IF TEST-CHECK = "PASS" GO TO LEVEL-1-TEST-3.                 NC2434.2
042000     MOVE GRP-HOLD-AREA TO CORRECT-A.                             NC2434.2
042100     MOVE ENTRY-1 (10) TO COMPUTED-A.                             NC2434.2
042200                                                                  NC2434.2
042300     MOVE "PERFORM VARYING USING INDEX" TO RE-MARK.               NC2434.2
042400     PERFORM FAIL-TH.                                             NC2434.2
042500                                                                  NC2434.2
042600 LEVEL-1-TEST-3.                                                  NC2434.2
042700     MOVE "GRP07" TO GRP-HOLD-AREA.                               NC2434.2
042800     MOVE "LEVEL-1-TEST-3      " TO PAR-NAME.                     NC2434.2
042900     MOVE SPACES TO TEST-CHECK.                                   NC2434.2
043000     PERFORM FIND-LEVEL-1-ENTRY VARYING IDX-1 FROM 1 BY 1         NC2434.2
043100         UNTIL IDX-1 GREATER 10.                                  NC2434.2
043200     IF TEST-CHECK = "PASS" GO TO LEVEL-1-TEST-4.                 NC2434.2
043300     MOVE GRP-HOLD-AREA TO CORRECT-A.                             NC2434.2
043400     MOVE ENTRY-1 (07) TO COMPUTED-A.                             NC2434.2
043500                                                                  NC2434.2
043600     MOVE "PERFORM VARYING USING INDEX" TO RE-MARK.               NC2434.2
043700     PERFORM FAIL-TH.                                             NC2434.2
043800 LEVEL-1-TEST-4.                                                  NC2434.2
043900     MOVE "LEVEL-1-TEST-4      " TO PAR-NAME.                     NC2434.2
044000     MOVE "GRP01" TO GRP-HOLD-AREA.                               NC2434.2
044100     PERFORM FIND-LEVEL-1-ENTRY VARYING IDX-1 FROM 1 BY 1         NC2434.2
044200         UNTIL IDX-1 GREATER 10.                                  NC2434.2
044300     IF TEST-CHECK = "PASS" GO TO LEVEL-2-TEST-1.                 NC2434.2
044400     MOVE GRP-HOLD-AREA TO CORRECT-A.                             NC2434.2
044500     MOVE ENTRY-1 (01) TO COMPUTED-A.                             NC2434.2
044600                                                                  NC2434.2
044700     MOVE "PERFORM VARYING USING INDEX" TO RE-MARK.               NC2434.2
044800     PERFORM FAIL-TH.                                             NC2434.2
044900     GO TO LEVEL-2-TEST-1.                                        NC2434.2
045000                                                                  NC2434.2
045100 FIND-LEVEL-1-ENTRY.                                              NC2434.2
045200     IF ENTRY-1 (IDX-1) = GRP-HOLD-AREA                           NC2434.2
045300         MOVE "PASS" TO TEST-CHECK                                NC2434.2
045400         PERFORM PASS-TH.                                         NC2434.2
045500                                                                  NC2434.2
045600 LEVEL-2-TEST-1.                                                  NC2434.2
045700     MOVE "LEVEL-2-TEST-1      " TO PAR-NAME.                     NC2434.2
045800     MOVE "PERFORM VARYING LEV2" TO FEATURE.                      NC2434.2
045900     MOVE "SEC (03,05)" TO SEC-HOLD-AREA.                         NC2434.2
046000     MOVE SPACES TO TEST-CHECK.                                   NC2434.2
046100     PERFORM FIND-LEVEL-2-ENTRY VARYING IDX-1 FROM 1 BY 1         NC2434.2
046200         UNTIL IDX-1 GREATER 10 AFTER IDX-2 FROM 1 BY 1 UNTIL     NC2434.2
046300         IDX-2 = 10.                                              NC2434.2
046400     IF TEST-CHECK = "PASS" GO TO LEVEL-2-TEST-2.                 NC2434.2
046500     MOVE SEC-HOLD-AREA TO CORRECT-A.                             NC2434.2
046600     MOVE ENTRY-2 (03, 05) TO COMPUTED-A.                         NC2434.2
046700                                                                  NC2434.2
046800     MOVE "PERFORM VARYING USING INDEX" TO RE-MARK.               NC2434.2
046900     PERFORM FAIL-TH.                                             NC2434.2
047000                                                                  NC2434.2
047100 LEVEL-2-TEST-2.                                                  NC2434.2
047200     MOVE "LEVEL-2-TEST-2      " TO PAR-NAME.                     NC2434.2
047300     MOVE SPACES TO TEST-CHECK.                                   NC2434.2
047400     MOVE "SEC (01,01)" TO SEC-HOLD-AREA.                         NC2434.2
047500     PERFORM FIND-LEVEL-2-ENTRY VARYING IDX-1 FROM 1 BY 1         NC2434.2
047600         UNTIL IDX-1 GREATER 10 AFTER IDX-2 FROM 1 BY 1           NC2434.2
047700             UNTIL IDX-2 = 10.                                    NC2434.2
047800     IF TEST-CHECK = "PASS" GO TO LEVEL-2-TEST-3.                 NC2434.2
047900     MOVE SEC-HOLD-AREA TO CORRECT-A.                             NC2434.2
048000     MOVE ENTRY-2 (01, 01) TO COMPUTED-A.                         NC2434.2
048100                                                                  NC2434.2
048200     MOVE "PERFORM VARYING USING INDEX" TO RE-MARK.               NC2434.2
048300     PERFORM FAIL-TH.                                             NC2434.2
048400                                                                  NC2434.2
048500 LEVEL-2-TEST-3.                                                  NC2434.2
048600     MOVE "LEVEL-2-TEST-3      " TO PAR-NAME.                     NC2434.2
048700     MOVE SPACES TO TEST-CHECK.                                   NC2434.2
048800     MOVE "SEC (10,01)" TO SEC-HOLD-AREA.                         NC2434.2
048900     PERFORM FIND-LEVEL-2-ENTRY VARYING IDX-1 FROM 1 BY 1         NC2434.2
049000         UNTIL IDX-1 GREATER 10 AFTER IDX-2 FROM 1 BY 1           NC2434.2
049100             UNTIL IDX-2 = 10.                                    NC2434.2
049200     IF TEST-CHECK = "PASS" GO TO LEVEL-2-TEST-4.                 NC2434.2
049300     MOVE SEC-HOLD-AREA TO CORRECT-A.                             NC2434.2
049400     MOVE ENTRY-2 (10, 01) TO COMPUTED-A.                         NC2434.2
049500                                                                  NC2434.2
049600     MOVE "PERFORM VARYING USING INDEX" TO RE-MARK.               NC2434.2
049700     PERFORM FAIL-TH.                                             NC2434.2
049800 LEVEL-2-TEST-4.                                                  NC2434.2
049900     MOVE "LEVEL-2-TEST-4      " TO PAR-NAME.                     NC2434.2
050000     MOVE SPACES TO TEST-CHECK.                                   NC2434.2
050100     MOVE SPACES TO TEST-CHECK.                                   NC2434.2
050200     MOVE "SEC (10,10)" TO SEC-HOLD-AREA.                         NC2434.2
050300     PERFORM FIND-LEVEL-2-ENTRY VARYING IDX-1 FROM 2 BY 2         NC2434.2
050400         UNTIL IDX-1 GREATER 10 AFTER IDX-2 FROM 2 BY 2           NC2434.2
050500             UNTIL IDX-2 GREATER 10.                              NC2434.2
050600     IF TEST-CHECK = "PASS" GO TO LEVEL-3-TEST-1.                 NC2434.2
050700     MOVE SEC-HOLD-AREA TO CORRECT-A.                             NC2434.2
050800     MOVE ENTRY-2 (10, 10) TO COMPUTED-A.                         NC2434.2
050900                                                                  NC2434.2
051000     MOVE "PERFORM VARYING USING INDEX" TO RE-MARK.               NC2434.2
051100     PERFORM FAIL-TH.                                             NC2434.2
051200     GO TO LEVEL-3-TEST-1.                                        NC2434.2
051300 FIND-LEVEL-2-ENTRY.                                              NC2434.2
051400     IF ENTRY-2 (IDX-1, IDX-2) = SEC-HOLD-AREA                    NC2434.2
051500         MOVE "PASS" TO TEST-CHECK                                NC2434.2
051600         PERFORM PASS-TH.                                         NC2434.2
051700 LEVEL-3-TEST-1.                                                  NC2434.2
051800     MOVE "PERFORM VARYING LEV3" TO FEATURE.                      NC2434.2
051900     MOVE SPACES TO TEST-CHECK.                                   NC2434.2
052000     MOVE "LEVEL-3-TEST-1      " TO PAR-NAME.                     NC2434.2
052100     MOVE "ELEM (01,02,03)" TO ELEM-HOLD-AREA.                    NC2434.2
052200     PERFORM FIND-LEVEL-3-ENTRY VARYING IDX-1 FROM 1 BY 1         NC2434.2
052300         UNTIL IDX-1 GREATER 10 AFTER IDX-2 FROM 1 BY 1 UNTIL     NC2434.2
052400             IDX-2 = 10 AFTER IDX-3 FROM 1 BY 1 UNTIL             NC2434.2
052500             IDX-3 = 10.                                          NC2434.2
052600     IF TEST-CHECK = "PASS" GO TO LEVEL-3-TEST-2.                 NC2434.2
052700     MOVE ELEM-HOLD-AREA TO CORRECT-A.                            NC2434.2
052800     MOVE ENTRY-3 (01, 02, 03) TO COMPUTED-A.                     NC2434.2
052900                                                                  NC2434.2
053000     MOVE "PERFORM VARYING USING INDEX" TO RE-MARK.               NC2434.2
053100     PERFORM FAIL-TH.                                             NC2434.2
053200                                                                  NC2434.2
053300 LEVEL-3-TEST-2.                                                  NC2434.2
053400     MOVE "LEVEL-3-TEST-2      " TO PAR-NAME.                     NC2434.2
053500     MOVE "ELEM (10,10,10)" TO ELEM-HOLD-AREA.                    NC2434.2
053600     MOVE SPACES TO TEST-CHECK.                                   NC2434.2
053700     PERFORM FIND-LEVEL-3-ENTRY VARYING IDX-1 FROM 1 BY 1         NC2434.2
053800         UNTIL IDX-1 GREATER 10 AFTER IDX-2 FROM 1 BY 1 UNTIL     NC2434.2
053900         IDX-2 GREATER 10 AFTER IDX-3 FROM 1 BY 1 UNTIL           NC2434.2
054000             IDX-3 GREATER 10.                                    NC2434.2
054100     IF TEST-CHECK = "PASS" GO TO LEVEL-3-TEST-3.                 NC2434.2
054200     MOVE ELEM-HOLD-AREA TO CORRECT-A.                            NC2434.2
054300     MOVE ENTRY-3 (10, 10, 10) TO COMPUTED-A.                     NC2434.2
054400                                                                  NC2434.2
054500     MOVE "PERFORM VARYING USING INDEX" TO RE-MARK.               NC2434.2
054600     PERFORM FAIL-TH.                                             NC2434.2
054700                                                                  NC2434.2
054800 LEVEL-3-TEST-3.                                                  NC2434.2
054900     MOVE "LEVEL-3-TEST-3      " TO PAR-NAME.                     NC2434.2
055000     MOVE "ELEM (08,07,06)" TO ELEM-HOLD-AREA.                    NC2434.2
055100     MOVE SPACES TO TEST-CHECK.                                   NC2434.2
055200     PERFORM FIND-LEVEL-3-ENTRY VARYING IDX-1 FROM 1 BY 1         NC2434.2
055300         UNTIL IDX-1 GREATER 10 AFTER IDX-2 FROM 1 BY 1 UNTIL     NC2434.2
055400             IDX-2 = 10 AFTER IDX-3 FROM 1 BY 1 UNTIL             NC2434.2
055500             IDX-3 = 10.                                          NC2434.2
055600     IF TEST-CHECK = "PASS" GO TO LEVEL-3-TEST-4.                 NC2434.2
055700     MOVE ELEM-HOLD-AREA TO CORRECT-A.                            NC2434.2
055800     MOVE ENTRY-3 (08, 07, 06) TO COMPUTED-A.                     NC2434.2
055900                                                                  NC2434.2
056000     MOVE "PERFORM VARYING USING INDEX" TO RE-MARK.               NC2434.2
056100     PERFORM FAIL-TH.                                             NC2434.2
056200 LEVEL-3-TEST-4.                                                  NC2434.2
056300     MOVE "LEVEL-3-TEST-4      " TO PAR-NAME.                     NC2434.2
056400     MOVE SPACES TO TEST-CHECK.                                   NC2434.2
056500     MOVE "ELEM (06,04,08)" TO ELEM-HOLD-AREA.                    NC2434.2
056600     PERFORM FIND-LEVEL-3-ENTRY VARYING IDX-1 FROM 3 BY 3         NC2434.2
056700         UNTIL IDX-1 GREATER 10 AFTER IDX-2 FROM 2 BY 2 UNTIL     NC2434.2
056800             IDX-2 GREATER 10 AFTER IDX-3 FROM 8 BY 8 UNTIL       NC2434.2
056900             IDX-3 GREATER 10.                                    NC2434.2
057000     IF TEST-CHECK = "PASS" GO TO END-3LEVEL-TEST.                NC2434.2
057100     MOVE ELEM-HOLD-AREA TO CORRECT-A.                            NC2434.2
057200     MOVE ENTRY-3 (06, 04, 08) TO COMPUTED-A.                     NC2434.2
057300                                                                  NC2434.2
057400     MOVE "PERFORM VARYING USING INDEX" TO RE-MARK.               NC2434.2
057500     PERFORM FAIL-TH.                                             NC2434.2
057600     GO TO END-3LEVEL-TEST.                                       NC2434.2
057700                                                                  NC2434.2
057800 FIND-LEVEL-3-ENTRY.                                              NC2434.2
057900     IF ENTRY-3 (IDX-1, IDX-2, IDX-3) = ELEM-HOLD-AREA            NC2434.2
058000         MOVE "PASS" TO TEST-CHECK                                NC2434.2
058100         PERFORM PASS-TH.                                         NC2434.2
058200                                                                  NC2434.2
058300 PASS-TH.                                                         NC2434.2
058400     PERFORM PASS.                                                NC2434.2
058500     PERFORM PRINT-DETAIL.                                        NC2434.2
058600 FAIL-TH.                                                         NC2434.2
058700     PERFORM FAIL.                                                NC2434.2
058800     PERFORM  PRINT-DETAIL.                                       NC2434.2
058900 END-3LEVEL-TEST.                                                 NC2434.2
059000     EXIT.                                                        NC2434.2
059100*                                                                 NC2434.2
059200 TH7-INIT-1.                                                      NC2434.2
059300     MOVE   "TH7-TEST"   TO PAR-NAME.                             NC2434.2
059400     MOVE   "VI-2 1.3.4" TO ANSI-REFERENCE.                       NC2434.2
059500     MOVE    ALL "ABCDEFGHIJKLMNOPQRSTUVWXYZ" TO 7-DIMENSION-TBL. NC2434.2
059600     MOVE   "KL" TO L4-HOLD.                                      NC2434.2
059700     MOVE   "AB" TO L5-HOLD.                                      NC2434.2
059800     MOVE   "CD" TO L6-HOLD.                                      NC2434.2
059900     MOVE   "GH" TO L7-HOLD.                                      NC2434.2
060000     MOVE    SPACES TO WS-FLAG.                                   NC2434.2
060100     MOVE    1 TO REC-CT.                                         NC2434.2
060200     GO TO   TH7-TEST-1-0.                                        NC2434.2
060300 TH7-DELETE-1.                                                    NC2434.2
060400     PERFORM DE-LETE.                                             NC2434.2
060500     PERFORM PRINT-DETAIL.                                        NC2434.2
060600     GO TO   CCVS-EXIT.                                           NC2434.2
060700 TH7-TEST-1-0.                                                    NC2434.2
060800     PERFORM TH7-FIND-LEVEL-4-ENTRY                               NC2434.2
060900             VARYING X1 FROM 1 BY 1 UNTIL X1 > 2                  NC2434.2
061000               AFTER X2 FROM 1 BY 1 UNTIL X2 > 2                  NC2434.2
061100               AFTER X3 FROM 1 BY 1 UNTIL X3 > 2                  NC2434.2
061200               AFTER X4 FROM 1 BY 1 UNTIL X4 > 2.                 NC2434.2
061300     GO TO   TH7-TEST-1-1.                                        NC2434.2
061400 TH7-FIND-LEVEL-4-ENTRY.                                          NC2434.2
061500     IF      ENTRY-7-4 (X1 X2 X3 X4) = L4-HOLD                    NC2434.2
061600             MOVE   "FOUND" TO WS-FLAG.                           NC2434.2
061700 TH7-TEST-1-1.                                                    NC2434.2
061800     IF      WS-FLAG = "FOUND"                                    NC2434.2
061900             PERFORM PASS                                         NC2434.2
062000             PERFORM PRINT-DETAIL                                 NC2434.2
062100     ELSE                                                         NC2434.2
062200             MOVE   "TABLE NOT CORRECT AT 4TH LEVEL" TO RE-MARK   NC2434.2
062300             MOVE    ENTRY-7-4 (X1 X2 X3 X4) TO COMPUTED-X        NC2434.2
062400             MOVE    L4-HOLD TO CORRECT-X                         NC2434.2
062500             PERFORM FAIL                                         NC2434.2
062600             PERFORM PRINT-DETAIL.                                NC2434.2
062700     MOVE    SPACES TO WS-FLAG.                                   NC2434.2
062800     ADD     1 TO REC-CT.                                         NC2434.2
062900 TH7-TEST-2-0.                                                    NC2434.2
063000     PERFORM TH7-FIND-LEVEL-5-ENTRY                               NC2434.2
063100             VARYING X1 FROM 1 BY 1 UNTIL X1 > 2                  NC2434.2
063200               AFTER X2 FROM 1 BY 1 UNTIL X2 > 2                  NC2434.2
063300               AFTER X3 FROM 1 BY 1 UNTIL X3 > 2                  NC2434.2
063400               AFTER X4 FROM 1 BY 1 UNTIL X4 > 2                  NC2434.2
063500               AFTER X5 FROM 1 BY 1 UNTIL X5 > 2.                 NC2434.2
063600     GO TO   TH7-TEST-2-1.                                        NC2434.2
063700 TH7-FIND-LEVEL-5-ENTRY.                                          NC2434.2
063800     IF      ENTRY-7-5 (X1 X2 X3 X4 X5) = L5-HOLD                 NC2434.2
063900             MOVE   "FOUND" TO WS-FLAG.                           NC2434.2
064000 TH7-TEST-2-1.                                                    NC2434.2
064100     IF      WS-FLAG = "FOUND"                                    NC2434.2
064200             PERFORM PASS                                         NC2434.2
064300             PERFORM PRINT-DETAIL                                 NC2434.2
064400     ELSE                                                         NC2434.2
064500             MOVE   "TABLE NOT CORRECT AT 5TH LEVEL" TO RE-MARK   NC2434.2
064600             MOVE    ENTRY-7-5 (X1 X2 X3 X4 X5) TO COMPUTED-X     NC2434.2
064700             MOVE    L5-HOLD TO CORRECT-X                         NC2434.2
064800             PERFORM FAIL                                         NC2434.2
064900             PERFORM PRINT-DETAIL.                                NC2434.2
065000     MOVE    SPACES TO WS-FLAG.                                   NC2434.2
065100     ADD     1 TO REC-CT.                                         NC2434.2
065200 TH7-TEST-3-0.                                                    NC2434.2
065300     PERFORM TH7-FIND-LEVEL-6-ENTRY                               NC2434.2
065400             VARYING X1 FROM 1 BY 1 UNTIL X1 > 2                  NC2434.2
065500               AFTER X2 FROM 1 BY 1 UNTIL X2 > 2                  NC2434.2
065600               AFTER X3 FROM 1 BY 1 UNTIL X3 > 2                  NC2434.2
065700               AFTER X4 FROM 1 BY 1 UNTIL X4 > 2                  NC2434.2
065800               AFTER X5 FROM 1 BY 1 UNTIL X5 > 2                  NC2434.2
065900               AFTER X6 FROM 1 BY 1 UNTIL X6 > 2.                 NC2434.2
066000     GO TO   TH7-TEST-3-1.                                        NC2434.2
066100 TH7-FIND-LEVEL-6-ENTRY.                                          NC2434.2
066200     IF      ENTRY-7-6 (X1 X2 X3 X4 X5 X6) = L6-HOLD              NC2434.2
066300             MOVE   "FOUND" TO WS-FLAG.                           NC2434.2
066400 TH7-TEST-3-1.                                                    NC2434.2
066500     IF      WS-FLAG = "FOUND"                                    NC2434.2
066600             PERFORM PASS                                         NC2434.2
066700             PERFORM PRINT-DETAIL                                 NC2434.2
066800     ELSE                                                         NC2434.2
066900             MOVE   "TABLE NOT CORRECT AT 6TH LEVEL" TO RE-MARK   NC2434.2
067000             MOVE    ENTRY-7-6 (X1 X2 X3 X4 X5 X6) TO COMPUTED-X  NC2434.2
067100             MOVE    L6-HOLD TO CORRECT-X                         NC2434.2
067200             PERFORM FAIL                                         NC2434.2
067300             PERFORM PRINT-DETAIL.                                NC2434.2
067400     MOVE    SPACES TO WS-FLAG.                                   NC2434.2
067500     ADD     1 TO REC-CT.                                         NC2434.2
067600 TH7-TEST-4-0.                                                    NC2434.2
067700     PERFORM TH7-FIND-LEVEL-7-ENTRY                               NC2434.2
067800             VARYING X1 FROM 1 BY 1 UNTIL X1 > 2                  NC2434.2
067900               AFTER X2 FROM 1 BY 1 UNTIL X2 > 2                  NC2434.2
068000               AFTER X3 FROM 1 BY 1 UNTIL X3 > 2                  NC2434.2
068100               AFTER X4 FROM 1 BY 1 UNTIL X4 > 2                  NC2434.2
068200               AFTER X5 FROM 1 BY 1 UNTIL X5 > 2                  NC2434.2
068300               AFTER X6 FROM 1 BY 1 UNTIL X6 > 2                  NC2434.2
068400               AFTER X7 FROM 1 BY 1 UNTIL X7 > 2.                 NC2434.2
068500     GO TO   TH7-TEST-4-1.                                        NC2434.2
068600 TH7-FIND-LEVEL-7-ENTRY.                                          NC2434.2
068700     IF      ENTRY-7-7 (X1 X2 X3 X4 X5 X6 X7) = L7-HOLD           NC2434.2
068800             MOVE   "FOUND" TO WS-FLAG.                           NC2434.2
068900 TH7-TEST-4-1.                                                    NC2434.2
069000     IF      WS-FLAG = "FOUND"                                    NC2434.2
069100             PERFORM PASS                                         NC2434.2
069200             PERFORM PRINT-DETAIL                                 NC2434.2
069300     ELSE                                                         NC2434.2
069400             MOVE   "TABLE NOT CORRECT AT 6TH LEVEL" TO RE-MARK   NC2434.2
069500             MOVE  ENTRY-7-7 (X1 X2 X3 X4 X5 X6 X7) TO COMPUTED-X NC2434.2
069600             MOVE    L7-HOLD TO CORRECT-X                         NC2434.2
069700             PERFORM FAIL                                         NC2434.2
069800             PERFORM PRINT-DETAIL.                                NC2434.2
069900*                                                                 NC2434.2
070000 CCVS-EXIT SECTION.                                               NC2434.2
070100 CCVS-999999.                                                     NC2434.2
070200     GO TO CLOSE-FILES.                                           NC2434.2
      *END-OF,NC243A                                                                  
