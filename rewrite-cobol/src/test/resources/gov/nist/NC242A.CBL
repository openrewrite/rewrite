      *HEADER,COBOL,NC242A                                                            
000100 IDENTIFICATION DIVISION.                                         NC2424.2
000200 PROGRAM-ID.                                                      NC2424.2
000300     NC242A.                                                      NC2424.2
000400                                                                  NC2424.2
000500****************************************************************  NC2424.2
000600*                                                              *  NC2424.2
000700*    VALIDATION FOR:-                                          *  NC2424.2
000800*                                                              *  NC2424.2
000900*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2424.2
001000*                                                              *  NC2424.2
001100*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC2424.2
001200*                                                              *  NC2424.2
001300****************************************************************  NC2424.2
001400*                                                              *  NC2424.2
001500*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  NC2424.2
001600*                                                              *  NC2424.2
001700*        X-55  - SYSTEM PRINTER NAME.                          *  NC2424.2
001800*        X-82  - SOURCE COMPUTER NAME.                         *  NC2424.2
001900*        X-83  - OBJECT COMPUTER NAME.                         *  NC2424.2
002000*                                                              *  NC2424.2
002100****************************************************************  NC2424.2
002200*                                                              *  NC2424.2
002300*    PROGRAM NC242A TESTS THE CONSTRUCTION AND ACCESS OF       *  NC2424.2
002400*    THREE AND SEVEN-DIMENSIONAL TABLES.                       *  NC2424.2
002500*    THE CONSTRUCTION IS BY MEANS OF A FORMAT 4 "PERFORM"      *  NC2424.2
002600*    STATEMENT UTILIZING SUBSCRIPTS WHICH ARE CONVERTED TO     *  NC2424.2
002700*    INDICES BY "SET".   ACCESS IS VIA "IF" STATEMENTS USING   *  NC2424.2
002800*    SUBSCRIPTS OF NUMERIC LITERALS AND CONSTANTS.             *  NC2424.2
002900*                                                              *  NC2424.2
003000****************************************************************  NC2424.2
003100 ENVIRONMENT DIVISION.                                            NC2424.2
003200 CONFIGURATION SECTION.                                           NC2424.2
003300 SOURCE-COMPUTER.                                                 NC2424.2
003400     XXXXX082.                                                    NC2424.2
003500 OBJECT-COMPUTER.                                                 NC2424.2
003600     XXXXX083.                                                    NC2424.2
003700 INPUT-OUTPUT SECTION.                                            NC2424.2
003800 FILE-CONTROL.                                                    NC2424.2
003900     SELECT PRINT-FILE ASSIGN TO                                  NC2424.2
004000     XXXXX055.                                                    NC2424.2
004100 DATA DIVISION.                                                   NC2424.2
004200 FILE SECTION.                                                    NC2424.2
004300 FD  PRINT-FILE.                                                  NC2424.2
004400 01  PRINT-REC PICTURE X(120).                                    NC2424.2
004500 01  DUMMY-RECORD PICTURE X(120).                                 NC2424.2
004600 WORKING-STORAGE SECTION.                                         NC2424.2
004700 77  SUB-1              PICTURE S99  VALUE ZERO.                  NC2424.2
004800 77  SUB-2              PICTURE 99   VALUE ZERO.                  NC2424.2
004900 77  SUB-3              PICTURE 99   VALUE ZERO.                  NC2424.2
005000 77  CON-7              PICTURE 99   VALUE 07.                    NC2424.2
005100 77  CON-10             PICTURE 99  VALUE 10.                     NC2424.2
005200 77  CON-5              PICTURE 99  VALUE 05.                     NC2424.2
005300 77  CON-6              PICTURE 99  VALUE 06.                     NC2424.2
005400 77  N1                 PICTURE 99  VALUE ZERO.                   NC2424.2
005500 77  N2                 PICTURE 99  VALUE ZERO.                   NC2424.2
005600 77  N3                 PICTURE 99  VALUE ZERO.                   NC2424.2
005700 77  N4                 PICTURE 99  VALUE ZERO.                   NC2424.2
005800 77  N5                 PICTURE 99  VALUE ZERO.                   NC2424.2
005900 77  N6                 PICTURE 99  VALUE ZERO.                   NC2424.2
006000 77  N7                 PICTURE 99  VALUE ZERO.                   NC2424.2
006100                                                                  NC2424.2
006200                                                                  NC2424.2
006300                                                                  NC2424.2
006400 01  GRP-NAME.                                                    NC2424.2
006500     02  FILLER              PICTURE XXX    VALUE "GRP".          NC2424.2
006600     02  ADD-GRP             PICTURE 99     VALUE 01.             NC2424.2
006700                                                                  NC2424.2
006800 01  SEC-NAME.                                                    NC2424.2
006900     02  FILLER              PICTURE X(5)   VALUE "SEC (".        NC2424.2
007000     02  SEC-GRP             PICTURE 99     VALUE 00.             NC2424.2
007100     02  FILLER              PICTURE X      VALUE ",".            NC2424.2
007200     02  ADD-SEC             PICTURE 99     VALUE 01.             NC2424.2
007300     02  FILLER              PICTURE X      VALUE ")".            NC2424.2
007400                                                                  NC2424.2
007500 01  ELEM-NAME.                                                   NC2424.2
007600     02  FILLER              PICTURE X(6)   VALUE "ELEM (".       NC2424.2
007700     02  ELEM-GRP            PICTURE 99     VALUE 00.             NC2424.2
007800     02  FILLER              PICTURE X      VALUE ",".            NC2424.2
007900     02  ELEM-SEC            PICTURE 99     VALUE 00.             NC2424.2
008000     02  FILLER              PICTURE X      VALUE ",".            NC2424.2
008100     02  ADD-ELEM            PICTURE 99     VALUE 01.             NC2424.2
008200     02  FILLER              PICTURE X      VALUE ")".            NC2424.2
008300                                                                  NC2424.2
008400 01  3-DIMENSION-TBL.                                             NC2424.2
008500     02  GRP-ENTRY OCCURS 10 TIMES INDEXED BY IDX-1.              NC2424.2
008600         03  ENTRY-1         PICTURE X(5).                        NC2424.2
008700         03  GRP2-ENTRY OCCURS 10 TIMES INDEXED BY IDX-2.         NC2424.2
008800             04  ENTRY-2     PICTURE X(11).                       NC2424.2
008900             04  GRP3-ENTRY OCCURS 10 TIMES INDEXED BY IDX-3.     NC2424.2
009000                 05  ENTRY-3 PICTURE X(15).                       NC2424.2
009100                                                                  NC2424.2
009200 01  7-DIMENSION-TBL.                                             NC2424.2
009300   02  GRP-7-1-ENTRY             OCCURS 2 INDEXED BY IX-1.        NC2424.2
009400     03  ENTRY-7-1               PIC XX.                          NC2424.2
009500     03  GRP-7-2-ENTRY           OCCURS 2 INDEXED BY IX-2.        NC2424.2
009600       04  ENTRY-7-2             PIC XX.                          NC2424.2
009700       04  GRP-7-3-ENTRY         OCCURS 2 INDEXED BY IX-3.        NC2424.2
009800         05  ENTRY-7-3           PIC XX.                          NC2424.2
009900         05  GRP-7-4-ENTRY       OCCURS 2 INDEXED BY IX-4.        NC2424.2
010000           06  ENTRY-7-4         PIC XX.                          NC2424.2
010100           06  GRP-7-5-ENTRY     OCCURS 2 INDEXED BY IX-5.        NC2424.2
010200             07  ENTRY-7-5       PIC XX.                          NC2424.2
010300             07  GRP-7-6-ENTRY   OCCURS 2 INDEXED BY IX-6.        NC2424.2
010400               08  ENTRY-7-6     PIC XX.                          NC2424.2
010500               08  GRP-7-7-ENTRY OCCURS 2 INDEXED BY IX-7.        NC2424.2
010600                 09  ENTRY-7-7   PIC XX.                          NC2424.2
010700                                                                  NC2424.2
010800 77  L1-HOLD                     PIC XX.                          NC2424.2
010900 77  L2-HOLD                     PIC XX.                          NC2424.2
011000 77  L3-HOLD                     PIC XX.                          NC2424.2
011100 77  L4-HOLD                     PIC XX.                          NC2424.2
011200 77  L5-HOLD                     PIC XX.                          NC2424.2
011300 77  L6-HOLD                     PIC XX.                          NC2424.2
011400 77  L7-HOLD                     PIC XX.                          NC2424.2
011500 01  TEST-RESULTS.                                                NC2424.2
011600     02 FILLER                   PIC X      VALUE SPACE.          NC2424.2
011700     02 FEATURE                  PIC X(20)  VALUE SPACE.          NC2424.2
011800     02 FILLER                   PIC X      VALUE SPACE.          NC2424.2
011900     02 P-OR-F                   PIC X(5)   VALUE SPACE.          NC2424.2
012000     02 FILLER                   PIC X      VALUE SPACE.          NC2424.2
012100     02  PAR-NAME.                                                NC2424.2
012200       03 FILLER                 PIC X(19)  VALUE SPACE.          NC2424.2
012300       03  PARDOT-X              PIC X      VALUE SPACE.          NC2424.2
012400       03 DOTVALUE               PIC 99     VALUE ZERO.           NC2424.2
012500     02 FILLER                   PIC X(8)   VALUE SPACE.          NC2424.2
012600     02 RE-MARK                  PIC X(61).                       NC2424.2
012700 01  TEST-COMPUTED.                                               NC2424.2
012800     02 FILLER                   PIC X(30)  VALUE SPACE.          NC2424.2
012900     02 FILLER                   PIC X(17)  VALUE                 NC2424.2
013000            "       COMPUTED=".                                   NC2424.2
013100     02 COMPUTED-X.                                               NC2424.2
013200     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          NC2424.2
013300     03 COMPUTED-N               REDEFINES COMPUTED-A             NC2424.2
013400                                 PIC -9(9).9(9).                  NC2424.2
013500     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         NC2424.2
013600     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     NC2424.2
013700     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     NC2424.2
013800     03       CM-18V0 REDEFINES COMPUTED-A.                       NC2424.2
013900         04 COMPUTED-18V0                    PIC -9(18).          NC2424.2
014000         04 FILLER                           PIC X.               NC2424.2
014100     03 FILLER PIC X(50) VALUE SPACE.                             NC2424.2
014200 01  TEST-CORRECT.                                                NC2424.2
014300     02 FILLER PIC X(30) VALUE SPACE.                             NC2424.2
014400     02 FILLER PIC X(17) VALUE "       CORRECT =".                NC2424.2
014500     02 CORRECT-X.                                                NC2424.2
014600     03 CORRECT-A                  PIC X(20) VALUE SPACE.         NC2424.2
014700     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      NC2424.2
014800     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         NC2424.2
014900     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     NC2424.2
015000     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     NC2424.2
015100     03      CR-18V0 REDEFINES CORRECT-A.                         NC2424.2
015200         04 CORRECT-18V0                     PIC -9(18).          NC2424.2
015300         04 FILLER                           PIC X.               NC2424.2
015400     03 FILLER PIC X(2) VALUE SPACE.                              NC2424.2
015500     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     NC2424.2
015600 01  CCVS-C-1.                                                    NC2424.2
015700     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PANC2424.2
015800-    "SS  PARAGRAPH-NAME                                          NC2424.2
015900-    "       REMARKS".                                            NC2424.2
016000     02 FILLER                     PIC X(20)    VALUE SPACE.      NC2424.2
016100 01  CCVS-C-2.                                                    NC2424.2
016200     02 FILLER                     PIC X        VALUE SPACE.      NC2424.2
016300     02 FILLER                     PIC X(6)     VALUE "TESTED".   NC2424.2
016400     02 FILLER                     PIC X(15)    VALUE SPACE.      NC2424.2
016500     02 FILLER                     PIC X(4)     VALUE "FAIL".     NC2424.2
016600     02 FILLER                     PIC X(94)    VALUE SPACE.      NC2424.2
016700 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       NC2424.2
016800 01  REC-CT                        PIC 99       VALUE ZERO.       NC2424.2
016900 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       NC2424.2
017000 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       NC2424.2
017100 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       NC2424.2
017200 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       NC2424.2
017300 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       NC2424.2
017400 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       NC2424.2
017500 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      NC2424.2
017600 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       NC2424.2
017700 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     NC2424.2
017800 01  CCVS-H-1.                                                    NC2424.2
017900     02  FILLER                    PIC X(39)    VALUE SPACES.     NC2424.2
018000     02  FILLER                    PIC X(42)    VALUE             NC2424.2
018100     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 NC2424.2
018200     02  FILLER                    PIC X(39)    VALUE SPACES.     NC2424.2
018300 01  CCVS-H-2A.                                                   NC2424.2
018400   02  FILLER                        PIC X(40)  VALUE SPACE.      NC2424.2
018500   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  NC2424.2
018600   02  FILLER                        PIC XXXX   VALUE             NC2424.2
018700     "4.2 ".                                                      NC2424.2
018800   02  FILLER                        PIC X(28)  VALUE             NC2424.2
018900            " COPY - NOT FOR DISTRIBUTION".                       NC2424.2
019000   02  FILLER                        PIC X(41)  VALUE SPACE.      NC2424.2
019100                                                                  NC2424.2
019200 01  CCVS-H-2B.                                                   NC2424.2
019300   02  FILLER                        PIC X(15)  VALUE             NC2424.2
019400            "TEST RESULT OF ".                                    NC2424.2
019500   02  TEST-ID                       PIC X(9).                    NC2424.2
019600   02  FILLER                        PIC X(4)   VALUE             NC2424.2
019700            " IN ".                                               NC2424.2
019800   02  FILLER                        PIC X(12)  VALUE             NC2424.2
019900     " HIGH       ".                                              NC2424.2
020000   02  FILLER                        PIC X(22)  VALUE             NC2424.2
020100            " LEVEL VALIDATION FOR ".                             NC2424.2
020200   02  FILLER                        PIC X(58)  VALUE             NC2424.2
020300     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2424.2
020400 01  CCVS-H-3.                                                    NC2424.2
020500     02  FILLER                      PIC X(34)  VALUE             NC2424.2
020600            " FOR OFFICIAL USE ONLY    ".                         NC2424.2
020700     02  FILLER                      PIC X(58)  VALUE             NC2424.2
020800     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC2424.2
020900     02  FILLER                      PIC X(28)  VALUE             NC2424.2
021000            "  COPYRIGHT   1985 ".                                NC2424.2
021100 01  CCVS-E-1.                                                    NC2424.2
021200     02 FILLER                       PIC X(52)  VALUE SPACE.      NC2424.2
021300     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              NC2424.2
021400     02 ID-AGAIN                     PIC X(9).                    NC2424.2
021500     02 FILLER                       PIC X(45)  VALUE SPACES.     NC2424.2
021600 01  CCVS-E-2.                                                    NC2424.2
021700     02  FILLER                      PIC X(31)  VALUE SPACE.      NC2424.2
021800     02  FILLER                      PIC X(21)  VALUE SPACE.      NC2424.2
021900     02 CCVS-E-2-2.                                               NC2424.2
022000         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      NC2424.2
022100         03 FILLER                   PIC X      VALUE SPACE.      NC2424.2
022200         03 ENDER-DESC               PIC X(44)  VALUE             NC2424.2
022300            "ERRORS ENCOUNTERED".                                 NC2424.2
022400 01  CCVS-E-3.                                                    NC2424.2
022500     02  FILLER                      PIC X(22)  VALUE             NC2424.2
022600            " FOR OFFICIAL USE ONLY".                             NC2424.2
022700     02  FILLER                      PIC X(12)  VALUE SPACE.      NC2424.2
022800     02  FILLER                      PIC X(58)  VALUE             NC2424.2
022900     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2424.2
023000     02  FILLER                      PIC X(13)  VALUE SPACE.      NC2424.2
023100     02 FILLER                       PIC X(15)  VALUE             NC2424.2
023200             " COPYRIGHT 1985".                                   NC2424.2
023300 01  CCVS-E-4.                                                    NC2424.2
023400     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      NC2424.2
023500     02 FILLER                       PIC X(4)   VALUE " OF ".     NC2424.2
023600     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      NC2424.2
023700     02 FILLER                       PIC X(40)  VALUE             NC2424.2
023800      "  TESTS WERE EXECUTED SUCCESSFULLY".                       NC2424.2
023900 01  XXINFO.                                                      NC2424.2
024000     02 FILLER                       PIC X(19)  VALUE             NC2424.2
024100            "*** INFORMATION ***".                                NC2424.2
024200     02 INFO-TEXT.                                                NC2424.2
024300       04 FILLER                     PIC X(8)   VALUE SPACE.      NC2424.2
024400       04 XXCOMPUTED                 PIC X(20).                   NC2424.2
024500       04 FILLER                     PIC X(5)   VALUE SPACE.      NC2424.2
024600       04 XXCORRECT                  PIC X(20).                   NC2424.2
024700     02 INF-ANSI-REFERENCE           PIC X(48).                   NC2424.2
024800 01  HYPHEN-LINE.                                                 NC2424.2
024900     02 FILLER  PIC IS X VALUE IS SPACE.                          NC2424.2
025000     02 FILLER  PIC IS X(65)    VALUE IS "************************NC2424.2
025100-    "*****************************************".                 NC2424.2
025200     02 FILLER  PIC IS X(54)    VALUE IS "************************NC2424.2
025300-    "******************************".                            NC2424.2
025400 01  CCVS-PGM-ID                     PIC X(9)   VALUE             NC2424.2
025500     "NC242A".                                                    NC2424.2
025600 PROCEDURE DIVISION.                                              NC2424.2
025700 CCVS1 SECTION.                                                   NC2424.2
025800 OPEN-FILES.                                                      NC2424.2
025900     OPEN     OUTPUT PRINT-FILE.                                  NC2424.2
026000     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   NC2424.2
026100     MOVE    SPACE TO TEST-RESULTS.                               NC2424.2
026200     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             NC2424.2
026300     GO TO CCVS1-EXIT.                                            NC2424.2
026400 CLOSE-FILES.                                                     NC2424.2
026500     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   NC2424.2
026600 TERMINATE-CCVS.                                                  NC2424.2
026700*    EXIT PROGRAM.                                                NC2424.2
026800*TERMINATE-CALL.                                                  NC2424.2
026900     STOP     RUN.                                                NC2424.2
027000 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         NC2424.2
027100 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           NC2424.2
027200 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          NC2424.2
027300 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      NC2424.2
027400     MOVE "****TEST DELETED****" TO RE-MARK.                      NC2424.2
027500 PRINT-DETAIL.                                                    NC2424.2
027600     IF REC-CT NOT EQUAL TO ZERO                                  NC2424.2
027700             MOVE "." TO PARDOT-X                                 NC2424.2
027800             MOVE REC-CT TO DOTVALUE.                             NC2424.2
027900     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      NC2424.2
028000     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               NC2424.2
028100        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 NC2424.2
028200          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 NC2424.2
028300     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              NC2424.2
028400     MOVE SPACE TO CORRECT-X.                                     NC2424.2
028500     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         NC2424.2
028600     MOVE     SPACE TO RE-MARK.                                   NC2424.2
028700 HEAD-ROUTINE.                                                    NC2424.2
028800     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC2424.2
028900     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC2424.2
029000     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC2424.2
029100     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC2424.2
029200 COLUMN-NAMES-ROUTINE.                                            NC2424.2
029300     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2424.2
029400     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2424.2
029500     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        NC2424.2
029600 END-ROUTINE.                                                     NC2424.2
029700     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.NC2424.2
029800 END-RTN-EXIT.                                                    NC2424.2
029900     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2424.2
030000 END-ROUTINE-1.                                                   NC2424.2
030100      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      NC2424.2
030200      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               NC2424.2
030300      ADD PASS-COUNTER TO ERROR-HOLD.                             NC2424.2
030400*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   NC2424.2
030500      MOVE PASS-COUNTER TO CCVS-E-4-1.                            NC2424.2
030600      MOVE ERROR-HOLD TO CCVS-E-4-2.                              NC2424.2
030700      MOVE CCVS-E-4 TO CCVS-E-2-2.                                NC2424.2
030800      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           NC2424.2
030900  END-ROUTINE-12.                                                 NC2424.2
031000      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        NC2424.2
031100     IF       ERROR-COUNTER IS EQUAL TO ZERO                      NC2424.2
031200         MOVE "NO " TO ERROR-TOTAL                                NC2424.2
031300         ELSE                                                     NC2424.2
031400         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       NC2424.2
031500     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           NC2424.2
031600     PERFORM WRITE-LINE.                                          NC2424.2
031700 END-ROUTINE-13.                                                  NC2424.2
031800     IF DELETE-COUNTER IS EQUAL TO ZERO                           NC2424.2
031900         MOVE "NO " TO ERROR-TOTAL  ELSE                          NC2424.2
032000         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      NC2424.2
032100     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   NC2424.2
032200     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2424.2
032300      IF   INSPECT-COUNTER EQUAL TO ZERO                          NC2424.2
032400          MOVE "NO " TO ERROR-TOTAL                               NC2424.2
032500      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   NC2424.2
032600      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            NC2424.2
032700      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          NC2424.2
032800     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2424.2
032900 WRITE-LINE.                                                      NC2424.2
033000     ADD 1 TO RECORD-COUNT.                                       NC2424.2
033100*    IF RECORD-COUNT GREATER 50                                   NC2424.2
033200*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          NC2424.2
033300*        MOVE SPACE TO DUMMY-RECORD                               NC2424.2
033400*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  NC2424.2
033500*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             NC2424.2
033600*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     NC2424.2
033700*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          NC2424.2
033800*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          NC2424.2
033900*        MOVE ZERO TO RECORD-COUNT.                               NC2424.2
034000     PERFORM WRT-LN.                                              NC2424.2
034100 WRT-LN.                                                          NC2424.2
034200     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               NC2424.2
034300     MOVE SPACE TO DUMMY-RECORD.                                  NC2424.2
034400 BLANK-LINE-PRINT.                                                NC2424.2
034500     PERFORM WRT-LN.                                              NC2424.2
034600 FAIL-ROUTINE.                                                    NC2424.2
034700     IF   COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE. NC2424.2
034800     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.NC2424.2
034900     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC2424.2
035000     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   NC2424.2
035100     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2424.2
035200     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC2424.2
035300     GO TO  FAIL-ROUTINE-EX.                                      NC2424.2
035400 FAIL-ROUTINE-WRITE.                                              NC2424.2
035500     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         NC2424.2
035600     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 NC2424.2
035700     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. NC2424.2
035800     MOVE   SPACES TO COR-ANSI-REFERENCE.                         NC2424.2
035900 FAIL-ROUTINE-EX. EXIT.                                           NC2424.2
036000 BAIL-OUT.                                                        NC2424.2
036100     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   NC2424.2
036200     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           NC2424.2
036300 BAIL-OUT-WRITE.                                                  NC2424.2
036400     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  NC2424.2
036500     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC2424.2
036600     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2424.2
036700     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC2424.2
036800 BAIL-OUT-EX. EXIT.                                               NC2424.2
036900 CCVS1-EXIT.                                                      NC2424.2
037000     EXIT.                                                        NC2424.2
037100 SECT-NC242A-001 SECTION.                                         NC2424.2
037200 TH-16-001.                                                       NC2424.2
037300     PERFORM PARA-1 VARYING SUB-1 FROM 1 BY 1                     NC2424.2
037400         UNTIL SUB-1 EQUAL TO 11                                  NC2424.2
037500         AFTER SUB-2 FROM 1 BY 1 UNTIL SUB-2 EQUAL TO 11          NC2424.2
037600         AFTER SUB-3 FROM 1 BY 1 UNTIL SUB-3 EQUAL TO 11          NC2424.2
037700     GO TO CHECK-ENTRIES.                                         NC2424.2
037800                                                                  NC2424.2
037900 PARA-1.                                                          NC2424.2
038000     SET IDX-1 TO SUB-1.                                          NC2424.2
038100     SET IDX-2 TO SUB-2.                                          NC2424.2
038200     SET IDX-3 TO SUB-3.                                          NC2424.2
038300     SET ADD-GRP, SEC-GRP, ELEM-GRP TO IDX-1.                     NC2424.2
038400     MOVE GRP-NAME TO ENTRY-1 (IDX-1).                            NC2424.2
038500     SET ADD-SEC, ELEM-SEC TO IDX-2.                              NC2424.2
038600     MOVE SEC-NAME TO ENTRY-2 (IDX-1, IDX-2).                     NC2424.2
038700     SET ADD-ELEM TO IDX-3.                                       NC2424.2
038800     MOVE ELEM-NAME TO ENTRY-3 (IDX-1, IDX-2, IDX-3).             NC2424.2
038900                                                                  NC2424.2
039000 CHECK-ENTRIES.                                                   NC2424.2
039100     MOVE "LEVEL 1 TBL SUBSCRPT" TO FEATURE.                      NC2424.2
039200     MOVE "CHECK-ENTRIES       " TO PAR-NAME.                     NC2424.2
039300     IF ENTRY-1 (5) IS NOT EQUAL TO "GRP05"                       NC2424.2
039400         MOVE "GRP05" TO CORRECT-A                                NC2424.2
039500         MOVE ENTRY-1 (5) TO COMPUTED-A                           NC2424.2
039600                                                                  NC2424.2
039700         MOVE "NUMERIC LITERAL SUBSCRIPT  " TO RE-MARK            NC2424.2
039800         PERFORM FAIL                                             NC2424.2
039900         GO TO TEST-1-WRITE.                                      NC2424.2
040000                                                                  NC2424.2
040100     PERFORM PASS.                                                NC2424.2
040200 TEST-1-WRITE.                                                    NC2424.2
040300     PERFORM PRINT-DETAIL.                                        NC2424.2
040400                                                                  NC2424.2
040500 TEST-1-2.                                                        NC2424.2
040600     MOVE "TEST-1-2" TO PAR-NAME.                                 NC2424.2
040700     IF ENTRY-1 (CON-5) IS NOT EQUAL TO "GRP05"                   NC2424.2
040800         MOVE "GRP05" TO CORRECT-A                                NC2424.2
040900         MOVE ENTRY-1 (CON-5) TO COMPUTED-A                       NC2424.2
041000                                                                  NC2424.2
041100         MOVE "NUMERIC CONSTANT SUBSCRIPT " TO RE-MARK            NC2424.2
041200         PERFORM FAIL                                             NC2424.2
041300          GO TO TEST-1-2-WRITE.                                   NC2424.2
041400                                                                  NC2424.2
041500     PERFORM PASS.                                                NC2424.2
041600 TEST-1-2-WRITE.                                                  NC2424.2
041700     PERFORM PRINT-DETAIL.                                        NC2424.2
041800                                                                  NC2424.2
041900 TEST-2.                                                          NC2424.2
042000     MOVE "LEVEL 2 TBL SUBSCRPT" TO FEATURE.                      NC2424.2
042100     MOVE "TEST-2              " TO PAR-NAME.                     NC2424.2
042200     IF ENTRY-2 (5, 6) IS NOT EQUAL TO "SEC (05,06)"              NC2424.2
042300         MOVE "SEC (05,06)" TO CORRECT-A                          NC2424.2
042400         MOVE ENTRY-2 (5, 6) TO COMPUTED-A                        NC2424.2
042500                                                                  NC2424.2
042600         MOVE "NUMERIC LITERAL SUBSCRIPT  " TO RE-MARK            NC2424.2
042700         PERFORM FAIL                                             NC2424.2
042800         GO TO TEST-2-WRITE.                                      NC2424.2
042900                                                                  NC2424.2
043000     PERFORM PASS.                                                NC2424.2
043100 TEST-2-WRITE.                                                    NC2424.2
043200     PERFORM PRINT-DETAIL.                                        NC2424.2
043300                                                                  NC2424.2
043400 TEST-2-2.                                                        NC2424.2
043500     MOVE "TEST-2-2            " TO PAR-NAME.                     NC2424.2
043600     IF ENTRY-2 (05, CON-6) IS NOT EQUAL TO "SEC (05,06)"         NC2424.2
043700         MOVE "SEC (05,06)" TO CORRECT-A                          NC2424.2
043800         MOVE ENTRY-2 (05, CON-6) TO COMPUTED-A                   NC2424.2
043900                                                                  NC2424.2
044000         MOVE "NUM LITRL/CONSTANT SUBSCRPT" TO RE-MARK            NC2424.2
044100         PERFORM FAIL                                             NC2424.2
044200         GO TO TEST-2-2-WRITE.                                    NC2424.2
044300                                                                  NC2424.2
044400     PERFORM PASS.                                                NC2424.2
044500 TEST-2-2-WRITE.                                                  NC2424.2
044600     PERFORM PRINT-DETAIL.                                        NC2424.2
044700                                                                  NC2424.2
044800 TEST-2-3.                                                        NC2424.2
044900     MOVE "TEST-2-3            " TO PAR-NAME.                     NC2424.2
045000     IF ENTRY-2 (CON-5, CON-6) IS NOT EQUAL TO "SEC (05,06)"      NC2424.2
045100         MOVE "SEC (05,06)" TO CORRECT-A                          NC2424.2
045200         MOVE ENTRY-2 (CON-5, CON-6) TO COMPUTED-A                NC2424.2
045300                                                                  NC2424.2
045400         MOVE "2 NUMERIC CONSTANT SUBSCRPT" TO RE-MARK            NC2424.2
045500         PERFORM FAIL                                             NC2424.2
045600         GO TO TEST-2-3-WRITE.                                    NC2424.2
045700                                                                  NC2424.2
045800     PERFORM PASS.                                                NC2424.2
045900 TEST-2-3-WRITE.                                                  NC2424.2
046000     PERFORM PRINT-DETAIL.                                        NC2424.2
046100                                                                  NC2424.2
046200 TEST-3.                                                          NC2424.2
046300     MOVE "LEVEL 3 TBL SUBSCRPT" TO FEATURE.                      NC2424.2
046400     MOVE "TEST-3              " TO PAR-NAME.                     NC2424.2
046500     IF ENTRY-3 (10, 05, 06) IS NOT EQUAL TO "ELEM (10,05,06)"    NC2424.2
046600         MOVE "ELEM (10,05,06)" TO CORRECT-A                      NC2424.2
046700         MOVE ENTRY-3 (10, 05, 06) TO COMPUTED-A                  NC2424.2
046800                                                                  NC2424.2
046900         MOVE "3 NUMERIC LITERAL SUBSCRPTS" TO RE-MARK            NC2424.2
047000         PERFORM FAIL                                             NC2424.2
047100         GO TO TEST-3-WRITE.                                      NC2424.2
047200                                                                  NC2424.2
047300     PERFORM PASS.                                                NC2424.2
047400 TEST-3-WRITE.                                                    NC2424.2
047500     PERFORM PRINT-DETAIL.                                        NC2424.2
047600                                                                  NC2424.2
047700 TEST-3-2.                                                        NC2424.2
047800     MOVE "TEST-3-2            " TO PAR-NAME.                     NC2424.2
047900     IF ENTRY-3 (10, CON-5, CON-6) IS NOT EQUAL TO                NC2424.2
048000         "ELEM (10,05,06)"                                        NC2424.2
048100         MOVE "ELEM (10,05,06)" TO CORRECT-A                      NC2424.2
048200         MOVE ENTRY-3 (10, CON-5, CON-6) TO COMPUTED-A            NC2424.2
048300                                                                  NC2424.2
048400         MOVE "1 NUM LTRL/2 CONSTANT SUBS " TO RE-MARK            NC2424.2
048500         PERFORM FAIL                                             NC2424.2
048600         GO TO TEST-3-2-WRITE.                                    NC2424.2
048700                                                                  NC2424.2
048800     PERFORM PASS.                                                NC2424.2
048900 TEST-3-2-WRITE.                                                  NC2424.2
049000     PERFORM PRINT-DETAIL.                                        NC2424.2
049100                                                                  NC2424.2
049200 TEST-3-3.                                                        NC2424.2
049300     MOVE "TEST-3-3            " TO PAR-NAME.                     NC2424.2
049400     IF ENTRY-3 (CON-10, CON-5, CON-6) IS NOT EQUAL TO            NC2424.2
049500         "ELEM (10,05,06)" MOVE "ELEM (10,05,06)" TO CORRECT-A    NC2424.2
049600         MOVE ENTRY-3 (CON-10, CON-5, CON-6) TO COMPUTED-A        NC2424.2
049700                                                                  NC2424.2
049800         MOVE "3 NUMERIC CONSTANT SUBSCRPT" TO RE-MARK            NC2424.2
049900         PERFORM FAIL                                             NC2424.2
050000         GO TO END-3LEVEL-SUBSCRPT-TEST.                          NC2424.2
050100                                                                  NC2424.2
050200     PERFORM PASS.                                                NC2424.2
050300     GO TO END-3LEVEL-SUBSCRPT-TEST.                              NC2424.2
050400                                                                  NC2424.2
050500 END-3LEVEL-SUBSCRPT-TEST.                                        NC2424.2
050600     PERFORM PRINT-DETAIL.                                        NC2424.2
050700*                                                                 NC2424.2
050800 TH7-INIT.                                                        NC2424.2
050900     MOVE   "TH7-TEST"   TO PAR-NAME.                             NC2424.2
051000     MOVE   "VI-2 1.3.4" TO ANSI-REFERENCE.                       NC2424.2
051100     MOVE    ALL "ABCDEFGHIJKLMNOPQRSTUVWXYZ" TO 7-DIMENSION-TBL. NC2424.2
051200     MOVE   "KL" TO L4-HOLD.                                      NC2424.2
051300     MOVE   "AB" TO L5-HOLD.                                      NC2424.2
051400     MOVE   "CD" TO L6-HOLD.                                      NC2424.2
051500     MOVE   "GH" TO L7-HOLD.                                      NC2424.2
051600     MOVE    1 TO REC-CT.                                         NC2424.2
051700     SET     IX-1 IX-2 IX-3 IX-4 IX-5 IX-6 IX-7 TO 1.             NC2424.2
051800     MOVE    2 TO N1 N3 N5 N7.                                    NC2424.2
051900     GO TO   TH7-TEST-1.                                          NC2424.2
052000 TH7-DELETE-1.                                                    NC2424.2
052100     PERFORM DE-LETE.                                             NC2424.2
052200     PERFORM PRINT-DETAIL.                                        NC2424.2
052300     GO TO   CCVS-999999.                                         NC2424.2
052400 TH7-TEST-1.                                                      NC2424.2
052500     MOVE   "TH7-TEST-1" TO PAR-NAME.                             NC2424.2
052600     IF      ENTRY-7-4 (N1 1 N3 1) = L4-HOLD                      NC2424.2
052700             PERFORM PASS                                         NC2424.2
052800             PERFORM PRINT-DETAIL                                 NC2424.2
052900     ELSE                                                         NC2424.2
053000             MOVE    ENTRY-7-4 (N1 1 N3 1) TO COMPUTED-A          NC2424.2
053100             MOVE    L4-HOLD TO CORRECT-A                         NC2424.2
053200             MOVE   "TABLE INCORRECT" TO RE-MARK                  NC2424.2
053300             PERFORM FAIL                                         NC2424.2
053400             PERFORM PRINT-DETAIL.                                NC2424.2
053500     ADD     1 TO REC-CT.                                         NC2424.2
053600 TH7-TEST-2.                                                      NC2424.2
053700     MOVE   "TH7-TEST-2" TO PAR-NAME.                             NC2424.2
053800     IF      ENTRY-7-5 (N1 1 N3 1 N5) = L5-HOLD                   NC2424.2
053900             PERFORM PASS                                         NC2424.2
054000             PERFORM PRINT-DETAIL                                 NC2424.2
054100     ELSE                                                         NC2424.2
054200             MOVE    ENTRY-7-5 (N1 1 N3 1 N5) TO COMPUTED-A       NC2424.2
054300             MOVE    L5-HOLD TO CORRECT-A                         NC2424.2
054400             MOVE   "TABLE INCORRECT" TO RE-MARK                  NC2424.2
054500             PERFORM FAIL                                         NC2424.2
054600             PERFORM PRINT-DETAIL.                                NC2424.2
054700     ADD     1 TO REC-CT.                                         NC2424.2
054800 TH7-TEST-3.                                                      NC2424.2
054900     MOVE   "TH7-TEST-3" TO PAR-NAME.                             NC2424.2
055000     IF      ENTRY-7-6 (N1 1 N3 1 N5 1) = L6-HOLD                 NC2424.2
055100             PERFORM PASS                                         NC2424.2
055200             PERFORM PRINT-DETAIL                                 NC2424.2
055300     ELSE                                                         NC2424.2
055400             MOVE    ENTRY-7-6 (N1 1 N3 1 N5 1) TO COMPUTED-A     NC2424.2
055500             MOVE    L6-HOLD TO CORRECT-A                         NC2424.2
055600             MOVE   "TABLE INCORRECT" TO RE-MARK                  NC2424.2
055700             PERFORM FAIL                                         NC2424.2
055800             PERFORM PRINT-DETAIL.                                NC2424.2
055900     ADD     1 TO REC-CT.                                         NC2424.2
056000 TH7-TEST-4.                                                      NC2424.2
056100     MOVE   "TH7-TEST-4" TO PAR-NAME.                             NC2424.2
056200     IF      ENTRY-7-7 (N1 1 N3 1 N5 1 N7) = L7-HOLD              NC2424.2
056300             PERFORM PASS                                         NC2424.2
056400             PERFORM PRINT-DETAIL                                 NC2424.2
056500     ELSE                                                         NC2424.2
056600             MOVE    ENTRY-7-7 (N1 1 N3 1 N5 1 N7) TO COMPUTED-A  NC2424.2
056700             MOVE    L7-HOLD TO CORRECT-A                         NC2424.2
056800             MOVE   "TABLE INCORRECT" TO RE-MARK                  NC2424.2
056900             PERFORM FAIL                                         NC2424.2
057000             PERFORM PRINT-DETAIL.                                NC2424.2
057100                                                                  NC2424.2
057200*                                                                 NC2424.2
057300 CCVS-EXIT SECTION.                                               NC2424.2
057400 CCVS-999999.                                                     NC2424.2
057500     GO TO CLOSE-FILES.                                           NC2424.2
      *END-OF,NC242A                                                                  
