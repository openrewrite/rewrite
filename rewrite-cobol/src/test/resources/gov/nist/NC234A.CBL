      *HEADER,COBOL,NC234A                                                            
000100 IDENTIFICATION DIVISION.                                         NC2344.2
000200 PROGRAM-ID.                                                      NC2344.2
000300     NC234A.                                                      NC2344.2
000400                                                                  NC2344.2
000500****************************************************************  NC2344.2
000600*                                                              *  NC2344.2
000700*    VALIDATION FOR:-                                          *  NC2344.2
000800*                                                              *  NC2344.2
000900*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2344.2
001000*                                                              *  NC2344.2
001100*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC2344.2
001200*                                                              *  NC2344.2
001300****************************************************************  NC2344.2
001400*                                                              *  NC2344.2
001500*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  NC2344.2
001600*                                                              *  NC2344.2
001700*        X-55  - SYSTEM PRINTER NAME.                          *  NC2344.2
001800*        X-82  - SOURCE COMPUTER NAME.                         *  NC2344.2
001900*        X-83  - OBJECT COMPUTER NAME.                         *  NC2344.2
002000*                                                              *  NC2344.2
002100****************************************************************  NC2344.2
002200*                                                                 NC2344.2
002300*    PROGRAM NC234A TESTS THE ACCESSING OF A "REDEFINED" THREE *  NC2344.2
002400*    -DIMENSIONAL TABLE USING FORMAT 1 OF THE "SEARCH"         *  NC2344.2
002500*    STATEMENT.  THE "VARYING" AND "AT END" PHRASES ARE USED.  *  NC2344.2
002600*                                                              *  NC2344.2
002700****************************************************************  NC2344.2
002800 ENVIRONMENT DIVISION.                                            NC2344.2
002900 CONFIGURATION SECTION.                                           NC2344.2
003000 SOURCE-COMPUTER.                                                 NC2344.2
003100     XXXXX082.                                                    NC2344.2
003200 OBJECT-COMPUTER.                                                 NC2344.2
003300     XXXXX083.                                                    NC2344.2
003400 INPUT-OUTPUT SECTION.                                            NC2344.2
003500 FILE-CONTROL.                                                    NC2344.2
003600     SELECT PRINT-FILE ASSIGN TO                                  NC2344.2
003700     XXXXX055.                                                    NC2344.2
003800 DATA DIVISION.                                                   NC2344.2
003900 FILE SECTION.                                                    NC2344.2
004000 FD  PRINT-FILE.                                                  NC2344.2
004100 01  PRINT-REC PICTURE X(120).                                    NC2344.2
004200 01  DUMMY-RECORD PICTURE X(120).                                 NC2344.2
004300 WORKING-STORAGE SECTION.                                         NC2344.2
004400 77  SUB-1              PICTURE S99  VALUE ZERO.                  NC2344.2
004500 77  SUB-2              PICTURE 99  VALUE ZERO.                   NC2344.2
004600 77  SUB-3              PICTURE 99  VALUE ZERO.                   NC2344.2
004700 77  CON-5              PICTURE 99  VALUE 05.                     NC2344.2
004800 77  CON-6              PICTURE 99  VALUE 06.                     NC2344.2
004900 77  CON-7              PICTURE 99  VALUE 07.                     NC2344.2
005000 77  CON-10             PICTURE 99  VALUE 10.                     NC2344.2
005100 77  GRP-HOLD-AREA      PICTURE X(5) VALUE SPACES.                NC2344.2
005200 77  SEC-HOLD-AREA      PICTURE X(11) VALUE SPACES.               NC2344.2
005300 77  ELEM-HOLD-AREA     PICTURE X(15) VALUE SPACES.               NC2344.2
005400 01  GRP-NAME.                                                    NC2344.2
005500     02  FILLER              PICTURE XXX    VALUE "GRP".          NC2344.2
005600     02  ADD-GRP             PICTURE 99     VALUE 01.             NC2344.2
005700                                                                  NC2344.2
005800 01  SEC-NAME.                                                    NC2344.2
005900     02  FILLER              PICTURE X(5)   VALUE "SEC (".        NC2344.2
006000     02  SEC-GRP             PICTURE 99     VALUE 00.             NC2344.2
006100     02  FILLER              PICTURE X      VALUE ",".            NC2344.2
006200     02  ADD-SEC             PICTURE 99     VALUE 01.             NC2344.2
006300     02  FILLER              PICTURE X      VALUE ")".            NC2344.2
006400                                                                  NC2344.2
006500 01  ELEM-NAME.                                                   NC2344.2
006600     02  FILLER              PICTURE X(6)   VALUE "ELEM (".       NC2344.2
006700     02  ELEM-GRP            PICTURE 99     VALUE 00.             NC2344.2
006800     02  FILLER              PICTURE X      VALUE ",".            NC2344.2
006900     02  ELEM-SEC            PICTURE 99     VALUE 00.             NC2344.2
007000     02  FILLER              PICTURE X      VALUE ",".            NC2344.2
007100     02  ADD-ELEM            PICTURE 99     VALUE 01.             NC2344.2
007200     02  FILLER              PICTURE X      VALUE ")".            NC2344.2
007300                                                                  NC2344.2
007400 01  3-DIMENSION-TBL.                                             NC2344.2
007500     02  GRP-ENTRY OCCURS 10 TIMES INDEXED BY IDX-1.              NC2344.2
007600         03  ENTRY-1         PICTURE X(5).                        NC2344.2
007700         03  GRP2-ENTRY OCCURS 10 TIMES INDEXED BY IDX-2.         NC2344.2
007800             04  ENTRY-2     PICTURE X(11).                       NC2344.2
007900             04  3-ENTRY OCCURS 10 TIMES INDEXED BY IDX-3.        NC2344.2
008000                 05  ENTRY-3 PICTURE X(15).                       NC2344.2
008100 01  3-DEM-TBL REDEFINES 3-DIMENSION-TBL.                         NC2344.2
008200     02  GRP-ENTRY-1 OCCURS 10 TIMES INDEXED BY IDX-1-1.          NC2344.2
008300         03  ENTRY-1-1           PIC X(5).                        NC2344.2
008400         03  GRP2-ENTRY-1 OCCURS 10 TIMES INDEXED BY IDX-2-1.     NC2344.2
008500             04  ENTRY-2-1       PIC X(11).                       NC2344.2
008600             04  GRP3-ENTRY-1 OCCURS 10 TIMES INDEXED BY IDX-3-1. NC2344.2
008700                 05  ENTRY-3-1   PIC X(15).                       NC2344.2
008800                                                                  NC2344.2
008900 01  END-STMT.                                                    NC2344.2
009000     02  FILLER              PICTURE X(7)  VALUE "AT END ".       NC2344.2
009100     02  END-IDX             PICTURE X(7)  VALUE SPACES.          NC2344.2
009200     02  FILLER              PICTURE XXX   VALUE " = ".           NC2344.2
009300     02  IDX-VALU            PICTURE 99    VALUE 00.              NC2344.2
009400     02  FILLER              PICTURE XXX   VALUE SPACES.          NC2344.2
009500 01  NOTE-1.                                                      NC2344.2
009600     02  FILLER                   PICTURE X(74)  VALUE            NC2344.2
009700     "NOTE 1 - CORRECT AND COMPUTED DATA ARE EQUAL BUT THE AT END NC2344.2
009800-    "PATH WAS TAKEN".                                            NC2344.2
009900     02  FILLER                   PICTURE X(46)  VALUE SPACES.    NC2344.2
010000 01  NOTE-2.                                                      NC2344.2
010100     02  FILLER                   PICTURE X(112)  VALUE           NC2344.2
010200     "NOTE 2 - CORRECT AND COMPUTED DATA ARE NOT EQUAL. THE COMPUTNC2344.2
010300-    "ED ENTRY WAS EXTRACTED FROM THE TABLE BY SUBSCRIPTS.".      NC2344.2
010400     02  FILLER                   PICTURE X(8)   VALUE SPACES.    NC2344.2
010500 01  TEST-RESULTS.                                                NC2344.2
010600     02 FILLER                   PIC X      VALUE SPACE.          NC2344.2
010700     02 FEATURE                  PIC X(20)  VALUE SPACE.          NC2344.2
010800     02 FILLER                   PIC X      VALUE SPACE.          NC2344.2
010900     02 P-OR-F                   PIC X(5)   VALUE SPACE.          NC2344.2
011000     02 FILLER                   PIC X      VALUE SPACE.          NC2344.2
011100     02  PAR-NAME.                                                NC2344.2
011200       03 FILLER                 PIC X(19)  VALUE SPACE.          NC2344.2
011300       03  PARDOT-X              PIC X      VALUE SPACE.          NC2344.2
011400       03 DOTVALUE               PIC 99     VALUE ZERO.           NC2344.2
011500     02 FILLER                   PIC X(8)   VALUE SPACE.          NC2344.2
011600     02 RE-MARK                  PIC X(61).                       NC2344.2
011700 01  TEST-COMPUTED.                                               NC2344.2
011800     02 FILLER                   PIC X(30)  VALUE SPACE.          NC2344.2
011900     02 FILLER                   PIC X(17)  VALUE                 NC2344.2
012000            "       COMPUTED=".                                   NC2344.2
012100     02 COMPUTED-X.                                               NC2344.2
012200     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          NC2344.2
012300     03 COMPUTED-N               REDEFINES COMPUTED-A             NC2344.2
012400                                 PIC -9(9).9(9).                  NC2344.2
012500     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         NC2344.2
012600     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     NC2344.2
012700     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     NC2344.2
012800     03       CM-18V0 REDEFINES COMPUTED-A.                       NC2344.2
012900         04 COMPUTED-18V0                    PIC -9(18).          NC2344.2
013000         04 FILLER                           PIC X.               NC2344.2
013100     03 FILLER PIC X(50) VALUE SPACE.                             NC2344.2
013200 01  TEST-CORRECT.                                                NC2344.2
013300     02 FILLER PIC X(30) VALUE SPACE.                             NC2344.2
013400     02 FILLER PIC X(17) VALUE "       CORRECT =".                NC2344.2
013500     02 CORRECT-X.                                                NC2344.2
013600     03 CORRECT-A                  PIC X(20) VALUE SPACE.         NC2344.2
013700     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      NC2344.2
013800     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         NC2344.2
013900     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     NC2344.2
014000     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     NC2344.2
014100     03      CR-18V0 REDEFINES CORRECT-A.                         NC2344.2
014200         04 CORRECT-18V0                     PIC -9(18).          NC2344.2
014300         04 FILLER                           PIC X.               NC2344.2
014400     03 FILLER PIC X(2) VALUE SPACE.                              NC2344.2
014500     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     NC2344.2
014600 01  CCVS-C-1.                                                    NC2344.2
014700     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PANC2344.2
014800-    "SS  PARAGRAPH-NAME                                          NC2344.2
014900-    "       REMARKS".                                            NC2344.2
015000     02 FILLER                     PIC X(20)    VALUE SPACE.      NC2344.2
015100 01  CCVS-C-2.                                                    NC2344.2
015200     02 FILLER                     PIC X        VALUE SPACE.      NC2344.2
015300     02 FILLER                     PIC X(6)     VALUE "TESTED".   NC2344.2
015400     02 FILLER                     PIC X(15)    VALUE SPACE.      NC2344.2
015500     02 FILLER                     PIC X(4)     VALUE "FAIL".     NC2344.2
015600     02 FILLER                     PIC X(94)    VALUE SPACE.      NC2344.2
015700 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       NC2344.2
015800 01  REC-CT                        PIC 99       VALUE ZERO.       NC2344.2
015900 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       NC2344.2
016000 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       NC2344.2
016100 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       NC2344.2
016200 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       NC2344.2
016300 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       NC2344.2
016400 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       NC2344.2
016500 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      NC2344.2
016600 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       NC2344.2
016700 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     NC2344.2
016800 01  CCVS-H-1.                                                    NC2344.2
016900     02  FILLER                    PIC X(39)    VALUE SPACES.     NC2344.2
017000     02  FILLER                    PIC X(42)    VALUE             NC2344.2
017100     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 NC2344.2
017200     02  FILLER                    PIC X(39)    VALUE SPACES.     NC2344.2
017300 01  CCVS-H-2A.                                                   NC2344.2
017400   02  FILLER                        PIC X(40)  VALUE SPACE.      NC2344.2
017500   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  NC2344.2
017600   02  FILLER                        PIC XXXX   VALUE             NC2344.2
017700     "4.2 ".                                                      NC2344.2
017800   02  FILLER                        PIC X(28)  VALUE             NC2344.2
017900            " COPY - NOT FOR DISTRIBUTION".                       NC2344.2
018000   02  FILLER                        PIC X(41)  VALUE SPACE.      NC2344.2
018100                                                                  NC2344.2
018200 01  CCVS-H-2B.                                                   NC2344.2
018300   02  FILLER                        PIC X(15)  VALUE             NC2344.2
018400            "TEST RESULT OF ".                                    NC2344.2
018500   02  TEST-ID                       PIC X(9).                    NC2344.2
018600   02  FILLER                        PIC X(4)   VALUE             NC2344.2
018700            " IN ".                                               NC2344.2
018800   02  FILLER                        PIC X(12)  VALUE             NC2344.2
018900     " HIGH       ".                                              NC2344.2
019000   02  FILLER                        PIC X(22)  VALUE             NC2344.2
019100            " LEVEL VALIDATION FOR ".                             NC2344.2
019200   02  FILLER                        PIC X(58)  VALUE             NC2344.2
019300     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2344.2
019400 01  CCVS-H-3.                                                    NC2344.2
019500     02  FILLER                      PIC X(34)  VALUE             NC2344.2
019600            " FOR OFFICIAL USE ONLY    ".                         NC2344.2
019700     02  FILLER                      PIC X(58)  VALUE             NC2344.2
019800     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC2344.2
019900     02  FILLER                      PIC X(28)  VALUE             NC2344.2
020000            "  COPYRIGHT   1985 ".                                NC2344.2
020100 01  CCVS-E-1.                                                    NC2344.2
020200     02 FILLER                       PIC X(52)  VALUE SPACE.      NC2344.2
020300     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              NC2344.2
020400     02 ID-AGAIN                     PIC X(9).                    NC2344.2
020500     02 FILLER                       PIC X(45)  VALUE SPACES.     NC2344.2
020600 01  CCVS-E-2.                                                    NC2344.2
020700     02  FILLER                      PIC X(31)  VALUE SPACE.      NC2344.2
020800     02  FILLER                      PIC X(21)  VALUE SPACE.      NC2344.2
020900     02 CCVS-E-2-2.                                               NC2344.2
021000         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      NC2344.2
021100         03 FILLER                   PIC X      VALUE SPACE.      NC2344.2
021200         03 ENDER-DESC               PIC X(44)  VALUE             NC2344.2
021300            "ERRORS ENCOUNTERED".                                 NC2344.2
021400 01  CCVS-E-3.                                                    NC2344.2
021500     02  FILLER                      PIC X(22)  VALUE             NC2344.2
021600            " FOR OFFICIAL USE ONLY".                             NC2344.2
021700     02  FILLER                      PIC X(12)  VALUE SPACE.      NC2344.2
021800     02  FILLER                      PIC X(58)  VALUE             NC2344.2
021900     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2344.2
022000     02  FILLER                      PIC X(13)  VALUE SPACE.      NC2344.2
022100     02 FILLER                       PIC X(15)  VALUE             NC2344.2
022200             " COPYRIGHT 1985".                                   NC2344.2
022300 01  CCVS-E-4.                                                    NC2344.2
022400     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      NC2344.2
022500     02 FILLER                       PIC X(4)   VALUE " OF ".     NC2344.2
022600     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      NC2344.2
022700     02 FILLER                       PIC X(40)  VALUE             NC2344.2
022800      "  TESTS WERE EXECUTED SUCCESSFULLY".                       NC2344.2
022900 01  XXINFO.                                                      NC2344.2
023000     02 FILLER                       PIC X(19)  VALUE             NC2344.2
023100            "*** INFORMATION ***".                                NC2344.2
023200     02 INFO-TEXT.                                                NC2344.2
023300       04 FILLER                     PIC X(8)   VALUE SPACE.      NC2344.2
023400       04 XXCOMPUTED                 PIC X(20).                   NC2344.2
023500       04 FILLER                     PIC X(5)   VALUE SPACE.      NC2344.2
023600       04 XXCORRECT                  PIC X(20).                   NC2344.2
023700     02 INF-ANSI-REFERENCE           PIC X(48).                   NC2344.2
023800 01  HYPHEN-LINE.                                                 NC2344.2
023900     02 FILLER  PIC IS X VALUE IS SPACE.                          NC2344.2
024000     02 FILLER  PIC IS X(65)    VALUE IS "************************NC2344.2
024100-    "*****************************************".                 NC2344.2
024200     02 FILLER  PIC IS X(54)    VALUE IS "************************NC2344.2
024300-    "******************************".                            NC2344.2
024400 01  CCVS-PGM-ID                     PIC X(9)   VALUE             NC2344.2
024500     "NC234A".                                                    NC2344.2
024600 PROCEDURE DIVISION.                                              NC2344.2
024700 CCVS1 SECTION.                                                   NC2344.2
024800 OPEN-FILES.                                                      NC2344.2
024900     OPEN     OUTPUT PRINT-FILE.                                  NC2344.2
025000     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   NC2344.2
025100     MOVE    SPACE TO TEST-RESULTS.                               NC2344.2
025200     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             NC2344.2
025300     GO TO CCVS1-EXIT.                                            NC2344.2
025400 CLOSE-FILES.                                                     NC2344.2
025500     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   NC2344.2
025600 TERMINATE-CCVS.                                                  NC2344.2
025700*    EXIT PROGRAM.                                                NC2344.2
025800*TERMINATE-CALL.                                                  NC2344.2
025900     STOP     RUN.                                                NC2344.2
026000 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         NC2344.2
026100 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           NC2344.2
026200 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          NC2344.2
026300 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      NC2344.2
026400     MOVE "****TEST DELETED****" TO RE-MARK.                      NC2344.2
026500 PRINT-DETAIL.                                                    NC2344.2
026600     IF REC-CT NOT EQUAL TO ZERO                                  NC2344.2
026700             MOVE "." TO PARDOT-X                                 NC2344.2
026800             MOVE REC-CT TO DOTVALUE.                             NC2344.2
026900     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      NC2344.2
027000     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               NC2344.2
027100        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 NC2344.2
027200          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 NC2344.2
027300     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              NC2344.2
027400     MOVE SPACE TO CORRECT-X.                                     NC2344.2
027500     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         NC2344.2
027600     MOVE     SPACE TO RE-MARK.                                   NC2344.2
027700 HEAD-ROUTINE.                                                    NC2344.2
027800     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC2344.2
027900     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC2344.2
028000     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC2344.2
028100     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC2344.2
028200 COLUMN-NAMES-ROUTINE.                                            NC2344.2
028300     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2344.2
028400     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2344.2
028500     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        NC2344.2
028600 END-ROUTINE.                                                     NC2344.2
028700     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.NC2344.2
028800 END-RTN-EXIT.                                                    NC2344.2
028900     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2344.2
029000 END-ROUTINE-1.                                                   NC2344.2
029100      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      NC2344.2
029200      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               NC2344.2
029300      ADD PASS-COUNTER TO ERROR-HOLD.                             NC2344.2
029400*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   NC2344.2
029500      MOVE PASS-COUNTER TO CCVS-E-4-1.                            NC2344.2
029600      MOVE ERROR-HOLD TO CCVS-E-4-2.                              NC2344.2
029700      MOVE CCVS-E-4 TO CCVS-E-2-2.                                NC2344.2
029800      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           NC2344.2
029900  END-ROUTINE-12.                                                 NC2344.2
030000      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        NC2344.2
030100     IF       ERROR-COUNTER IS EQUAL TO ZERO                      NC2344.2
030200         MOVE "NO " TO ERROR-TOTAL                                NC2344.2
030300         ELSE                                                     NC2344.2
030400         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       NC2344.2
030500     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           NC2344.2
030600     PERFORM WRITE-LINE.                                          NC2344.2
030700 END-ROUTINE-13.                                                  NC2344.2
030800     IF DELETE-COUNTER IS EQUAL TO ZERO                           NC2344.2
030900         MOVE "NO " TO ERROR-TOTAL  ELSE                          NC2344.2
031000         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      NC2344.2
031100     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   NC2344.2
031200     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2344.2
031300      IF   INSPECT-COUNTER EQUAL TO ZERO                          NC2344.2
031400          MOVE "NO " TO ERROR-TOTAL                               NC2344.2
031500      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   NC2344.2
031600      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            NC2344.2
031700      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          NC2344.2
031800     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2344.2
031900 WRITE-LINE.                                                      NC2344.2
032000     ADD 1 TO RECORD-COUNT.                                       NC2344.2
032100*    IF RECORD-COUNT GREATER 50                                   NC2344.2
032200*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          NC2344.2
032300*        MOVE SPACE TO DUMMY-RECORD                               NC2344.2
032400*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  NC2344.2
032500*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             NC2344.2
032600*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     NC2344.2
032700*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          NC2344.2
032800*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          NC2344.2
032900*        MOVE ZERO TO RECORD-COUNT.                               NC2344.2
033000     PERFORM WRT-LN.                                              NC2344.2
033100 WRT-LN.                                                          NC2344.2
033200     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               NC2344.2
033300     MOVE SPACE TO DUMMY-RECORD.                                  NC2344.2
033400 BLANK-LINE-PRINT.                                                NC2344.2
033500     PERFORM WRT-LN.                                              NC2344.2
033600 FAIL-ROUTINE.                                                    NC2344.2
033700     IF     COMPUTED-X NOT EQUAL TO SPACE                         NC2344.2
033800            GO TO    FAIL-ROUTINE-WRITE.                          NC2344.2
033900     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.NC2344.2
034000     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC2344.2
034100     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   NC2344.2
034200     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2344.2
034300     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC2344.2
034400     GO TO  FAIL-ROUTINE-EX.                                      NC2344.2
034500 FAIL-ROUTINE-WRITE.                                              NC2344.2
034600     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         NC2344.2
034700     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 NC2344.2
034800     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. NC2344.2
034900     MOVE   SPACES TO COR-ANSI-REFERENCE.                         NC2344.2
035000 FAIL-ROUTINE-EX. EXIT.                                           NC2344.2
035100 BAIL-OUT.                                                        NC2344.2
035200     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   NC2344.2
035300     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           NC2344.2
035400 BAIL-OUT-WRITE.                                                  NC2344.2
035500     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  NC2344.2
035600     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC2344.2
035700     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2344.2
035800     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC2344.2
035900 BAIL-OUT-EX. EXIT.                                               NC2344.2
036000 CCVS1-EXIT.                                                      NC2344.2
036100     EXIT.                                                        NC2344.2
036200 SECT-NC234A-001 SECTION.                                         NC2344.2
036300 TH-07-001.                                                       NC2344.2
036400 INITIALISE-TABLE.                                                NC2344.2
036500     PERFORM BUILD-TABLE VARYING SUB-1 FROM 1 BY 1                NC2344.2
036600         UNTIL SUB-1 EQUAL TO 11                                  NC2344.2
036700         AFTER SUB-2 FROM 1 BY 1 UNTIL SUB-2 EQUAL TO 11          NC2344.2
036800         AFTER SUB-3 FROM 1 BY 1 UNTIL SUB-3 EQUAL TO 11          NC2344.2
036900     GO TO CHECK-ENTRIES.                                         NC2344.2
037000                                                                  NC2344.2
037100 BUILD-TABLE.                                                     NC2344.2
037200     SET IDX-1 TO SUB-1.                                          NC2344.2
037300     SET IDX-2 TO SUB-2.                                          NC2344.2
037400     SET IDX-3 TO SUB-3.                                          NC2344.2
037500     SET ADD-GRP, SEC-GRP, ELEM-GRP TO IDX-1.                     NC2344.2
037600     MOVE GRP-NAME TO ENTRY-1 (IDX-1).                            NC2344.2
037700     SET ADD-SEC, ELEM-SEC TO IDX-2.                              NC2344.2
037800     MOVE SEC-NAME TO ENTRY-2 (IDX-1, IDX-2).                     NC2344.2
037900     SET ADD-ELEM TO IDX-3.                                       NC2344.2
038000     MOVE ELEM-NAME TO ENTRY-3 (IDX-1, IDX-2, IDX-3).             NC2344.2
038100*                                                                 NC2344.2
038200 CHECK-ENTRIES.                                                   NC2344.2
038300     MOVE "SEARCH VARYING LEV 1" TO FEATURE.                      NC2344.2
038400     MOVE "CHECK-ENTRIES" TO PAR-NAME.                            NC2344.2
038500     MOVE "GRP02" TO GRP-HOLD-AREA.                               NC2344.2
038600     MOVE 02 TO SUB-2.                                            NC2344.2
038700     SET IDX-1 TO 1.                                              NC2344.2
038800     SEARCH GRP-ENTRY VARYING IDX-1                               NC2344.2
038900         AT END                                                   NC2344.2
039000                GO TO CHECK-FAIL                                  NC2344.2
039100         WHEN ENTRY-1 (IDX-1) = GRP-HOLD-AREA                     NC2344.2
039200                PERFORM PASS                                      NC2344.2
039300                GO TO CHECK-WRITE.                                NC2344.2
039400 CHECK-DELETE.                                                    NC2344.2
039500     PERFORM DE-LETE.                                             NC2344.2
039600     GO TO CHECK-WRITE.                                           NC2344.2
039700 CHECK-FAIL.                                                      NC2344.2
039800     MOVE GRP-HOLD-AREA TO CORRECT-A.                             NC2344.2
039900     IF ENTRY-1 (SUB-2) EQUAL TO GRP-HOLD-AREA                    NC2344.2
040000         MOVE "IDX-1" TO END-IDX                                  NC2344.2
040100         SET IDX-VALU TO IDX-1                                    NC2344.2
040200         MOVE "SEE NOTE 1 FOR DIAGNOSTIC " TO RE-MARK             NC2344.2
040300         MOVE END-STMT TO COMPUTED-A ELSE                         NC2344.2
040400     MOVE ENTRY-1 (SUB-2) TO COMPUTED-A                           NC2344.2
040500     MOVE "SEE NOTE 2 FOR DIAGNOSTIC " TO RE-MARK.                NC2344.2
040600     PERFORM FAIL.                                                NC2344.2
040700 CHECK-WRITE.                                                     NC2344.2
040800     PERFORM PRINT-DETAIL.                                        NC2344.2
040900*                                                                 NC2344.2
041000 TH1-INIT-F1-2.                                                   NC2344.2
041100     MOVE "TH1-TEST-F1-2" TO PAR-NAME.                            NC2344.2
041200     MOVE "VI-122 6.21" TO ANSI-REFERENCE.                        NC2344.2
041300     MOVE "SEARCH VARYING LEV 1" TO FEATURE.                      NC2344.2
041400     MOVE "GRP01" TO GRP-HOLD-AREA.                               NC2344.2
041500     MOVE 01 TO SUB-2.                                            NC2344.2
041600     SET IDX-1-1 TO 1.                                            NC2344.2
041700 TH1-TEST-F1-2.                                                   NC2344.2
041800     SEARCH GRP-ENTRY-1 VARYING IDX-1                             NC2344.2
041900         AT END GO TO TH1-FAIL-F1-2                               NC2344.2
042000         WHEN ENTRY-1-1 (IDX-1-1) = GRP-HOLD-AREA NEXT SENTENCE.  NC2344.2
042100     PERFORM PASS                                                 NC2344.2
042200     GO TO TH1-WRITE-F1-2.                                        NC2344.2
042300 TH1-DELETE-F1-2.                                                 NC2344.2
042400     PERFORM DE-LETE.                                             NC2344.2
042500     GO TO TH1-WRITE-F1-2.                                        NC2344.2
042600 TH1-FAIL-F1-2.                                                   NC2344.2
042700     PERFORM CHECK-FAIL.                                          NC2344.2
042800 TH1-WRITE-F1-2.                                                  NC2344.2
042900     PERFORM PRINT-DETAIL.                                        NC2344.2
043000*                                                                 NC2344.2
043100 TH1-INIT-F1-3.                                                   NC2344.2
043200     MOVE "TH1-TEST-F1-3" TO PAR-NAME.                            NC2344.2
043300     MOVE "VI-122 6.21" TO ANSI-REFERENCE.                        NC2344.2
043400     MOVE "SEARCH VARYING LEV 1" TO FEATURE.                      NC2344.2
043500     MOVE "GRP10" TO GRP-HOLD-AREA.                               NC2344.2
043600     MOVE 10 TO SUB-2.                                            NC2344.2
043700     SET IDX-1-1 TO 1.                                            NC2344.2
043800 TH1-TEST-F1-3.                                                   NC2344.2
043900     SEARCH GRP-ENTRY-1 VARYING IDX-1-1                           NC2344.2
044000         AT END GO TO TH1-FAIL-F1-3                               NC2344.2
044100         WHEN ENTRY-1-1 (IDX-1-1) = GRP-HOLD-AREA NEXT SENTENCE.  NC2344.2
044200     MOVE "SEARCH VARYING LEV 2" TO FEATURE.                      NC2344.2
044300     PERFORM PASS                                                 NC2344.2
044400     GO TO TH1-WRITE-F1-3.                                        NC2344.2
044500 TH1-DELETE-F1-3.                                                 NC2344.2
044600     PERFORM DE-LETE.                                             NC2344.2
044700     GO TO TH1-WRITE-F1-3.                                        NC2344.2
044800 TH1-FAIL-F1-3.                                                   NC2344.2
044900     PERFORM CHECK-FAIL.                                          NC2344.2
045000 TH1-WRITE-F1-3.                                                  NC2344.2
045100     PERFORM PRINT-DETAIL.                                        NC2344.2
045200*                                                                 NC2344.2
045300 TH1-INIT-F1-4.                                                   NC2344.2
045400     MOVE "TH1-TEST-F1-4" TO PAR-NAME.                            NC2344.2
045500     MOVE "VI-122 6.21" TO ANSI-REFERENCE.                        NC2344.2
045600     MOVE "SEARCH VARYING LEV 1" TO FEATURE.                      NC2344.2
045700     MOVE "GRP05" TO GRP-HOLD-AREA.                               NC2344.2
045800     MOVE 05 TO SUB-2.                                            NC2344.2
045900     SET IDX-1-1 TO 05.                                           NC2344.2
046000 TH1-TEST-F1-4.                                                   NC2344.2
046100     SEARCH GRP-ENTRY-1 VARYING IDX-1-1                           NC2344.2
046200         WHEN ENTRY-1-1 (IDX-1-1) = GRP-HOLD-AREA                 NC2344.2
046300             PERFORM PASS                                         NC2344.2
046400             GO TO TH1-WRITE-F1-4.                                NC2344.2
046500     GO TO TH1-FAIL-F1-4.                                         NC2344.2
046600 TH1-DELETE-F1-4.                                                 NC2344.2
046700     PERFORM DE-LETE.                                             NC2344.2
046800     GO TO TH1-WRITE-F1-4.                                        NC2344.2
046900 TH1-FAIL-F1-4.                                                   NC2344.2
047000     PERFORM CHECK-FAIL.                                          NC2344.2
047100 TH1-WRITE-F1-4.                                                  NC2344.2
047200     PERFORM PRINT-DETAIL.                                        NC2344.2
047300*                                                                 NC2344.2
047400 TH2-INIT-F1-1.                                                   NC2344.2
047500     MOVE "SEARCH VARYING LEV 2" TO FEATURE.                      NC2344.2
047600     MOVE "VI-122 6.21" TO ANSI-REFERENCE.                        NC2344.2
047700     MOVE "TH2-TEST-F1-1" TO PAR-NAME.                            NC2344.2
047800     MOVE "SEC (01,01)" TO SEC-HOLD-AREA.                         NC2344.2
047900     MOVE  1 TO SUB-1  SUB-2.                                     NC2344.2
048000     SET IDX-1-1 IDX-2-1 TO 1.                                    NC2344.2
048100 TH2-TEST-F1-1.                                                   NC2344.2
048200     SEARCH GRP2-ENTRY-1 VARYING IDX-2-1 AT END                   NC2344.2
048300         GO TO TH2-FAIL-F1-1                                      NC2344.2
048400         WHEN ENTRY-2-1 (IDX-1-1, IDX-2-1) = SEC-HOLD-AREA        NC2344.2
048500             NEXT SENTENCE.                                       NC2344.2
048600     PERFORM PASS                                                 NC2344.2
048700     GO TO TH2-WRITE-F1-1.                                        NC2344.2
048800 TH2-DELETE-F1-1.                                                 NC2344.2
048900     PERFORM DE-LETE.                                             NC2344.2
049000     GO TO TH2-WRITE-F1-1.                                        NC2344.2
049100 TH2-FAIL-F1-1.                                                   NC2344.2
049200     PERFORM CHECK-FAIL2.                                         NC2344.2
049300 TH2-WRITE-F1-1.                                                  NC2344.2
049400     PERFORM PRINT-DETAIL.                                        NC2344.2
049500                                                                  NC2344.2
049600*                                                                 NC2344.2
049700 TH2-INIT-F1-2.                                                   NC2344.2
049800     MOVE "TH2-TEST-F1-2" TO PAR-NAME.                            NC2344.2
049900     MOVE "VI-122 6.21" TO ANSI-REFERENCE.                        NC2344.2
050000     MOVE "SEARCH VARYING LEV 2" TO FEATURE.                      NC2344.2
050100     MOVE "SEC (05,10)" TO SEC-HOLD-AREA.                         NC2344.2
050200     MOVE 05 TO SUB-1.                                            NC2344.2
050300     MOVE 10 TO SUB-2.                                            NC2344.2
050400     SET IDX-1-1 TO 5.                                            NC2344.2
050500     SET IDX-2-1 TO 1.                                            NC2344.2
050600 TH2-TEST-F1-2.                                                   NC2344.2
050700     SEARCH GRP2-ENTRY-1 VARYING IDX-2-1 AT END                   NC2344.2
050800         GO TO TH2-FAIL-F1-2                                      NC2344.2
050900         WHEN ENTRY-2-1 (IDX-1-1, IDX-2-1) = SEC-HOLD-AREA        NC2344.2
051000             NEXT SENTENCE.                                       NC2344.2
051100     PERFORM PASS                                                 NC2344.2
051200     GO TO TH2-WRITE-F1-2.                                        NC2344.2
051300 TH2-DELETE-F1-2.                                                 NC2344.2
051400     PERFORM DE-LETE.                                             NC2344.2
051500     GO TO TH2-WRITE-F1-2.                                        NC2344.2
051600 TH2-FAIL-F1-2.                                                   NC2344.2
051700     PERFORM CHECK-FAIL2.                                         NC2344.2
051800 TH2-WRITE-F1-2.                                                  NC2344.2
051900     PERFORM PRINT-DETAIL.                                        NC2344.2
052000*                                                                 NC2344.2
052100 TH2-INIT-F1-3.                                                   NC2344.2
052200     MOVE "TH2-TEST-F1-3" TO PAR-NAME.                            NC2344.2
052300     MOVE "VI-122 6.21" TO ANSI-REFERENCE.                        NC2344.2
052400     MOVE "SEARCH VARYING LEV 2" TO FEATURE.                      NC2344.2
052500     MOVE "SEC (10,10)" TO SEC-HOLD-AREA.                         NC2344.2
052600     SET IDX-1-1 TO 10.                                           NC2344.2
052700     SET IDX-2-1 TO 1.                                            NC2344.2
052800     MOVE 10 TO SUB-1  SUB-2.                                     NC2344.2
052900 TH2-TEST-F1-3.                                                   NC2344.2
053000     SEARCH GRP2-ENTRY-1 VARYING IDX-2-1 AT END                   NC2344.2
053100         GO TO TH2-FAIL-F1-3                                      NC2344.2
053200         WHEN ENTRY-2-1 (IDX-1-1, IDX-2-1) = SEC-HOLD-AREA        NC2344.2
053300             NEXT SENTENCE.                                       NC2344.2
053400     PERFORM PASS                                                 NC2344.2
053500     GO TO TH2-WRITE-F1-3.                                        NC2344.2
053600 TH2-DELETE-F1-3.                                                 NC2344.2
053700     PERFORM DE-LETE.                                             NC2344.2
053800     GO TO TH2-WRITE-F1-3.                                        NC2344.2
053900 TH2-FAIL-F1-3.                                                   NC2344.2
054000     PERFORM CHECK-FAIL2.                                         NC2344.2
054100 TH2-WRITE-F1-3.                                                  NC2344.2
054200     PERFORM PRINT-DETAIL.                                        NC2344.2
054300*                                                                 NC2344.2
054400 TH2-INIT-F1-4.                                                   NC2344.2
054500     MOVE "TH2-TEST-F1-4" TO PAR-NAME.                            NC2344.2
054600     MOVE "VI-122 6.21" TO ANSI-REFERENCE.                        NC2344.2
054700     MOVE "SEARCH VARYING LEV 2" TO FEATURE.                      NC2344.2
054800     MOVE "SEC (08,02)" TO SEC-HOLD-AREA.                         NC2344.2
054900     MOVE 08 TO SUB-1.                                            NC2344.2
055000     MOVE 02 TO SUB-2.                                            NC2344.2
055100     SET IDX-1-1 TO 08.                                           NC2344.2
055200     SET IDX-2-1 TO 01.                                           NC2344.2
055300 TH2-TEST-F1-4.                                                   NC2344.2
055400     SEARCH GRP2-ENTRY-1 VARYING IDX-2-1                          NC2344.2
055500         WHEN ENTRY-2-1 (IDX-1-1, IDX-2-1) = SEC-HOLD-AREA        NC2344.2
055600              PERFORM PASS                                        NC2344.2
055700              GO TO TH2-WRITE-F1-4.                               NC2344.2
055800     GO TO TH2-FAIL-F1-4.                                         NC2344.2
055900 TH2-DELETE-F1-4.                                                 NC2344.2
056000     PERFORM DE-LETE.                                             NC2344.2
056100     GO TO TH2-WRITE-F1-4.                                        NC2344.2
056200 TH2-FAIL-F1-4.                                                   NC2344.2
056300     PERFORM CHECK-FAIL2.                                         NC2344.2
056400 TH2-WRITE-F1-4.                                                  NC2344.2
056500     PERFORM PRINT-DETAIL.                                        NC2344.2
056600     GO TO TH3-INIT-F1-1.                                         NC2344.2
056700                                                                  NC2344.2
056800 CHECK-FAIL2.                                                     NC2344.2
056900     MOVE SEC-HOLD-AREA TO CORRECT-A.                             NC2344.2
057000     IF ENTRY-2-1 (SUB-1, SUB-2) EQUAL TO SEC-HOLD-AREA           NC2344.2
057100         MOVE "IDX-2" TO END-IDX                                  NC2344.2
057200         SET IDX-VALU TO IDX-2                                    NC2344.2
057300         MOVE "SEE NOTE 1 FOR DIAGNOSTIC " TO RE-MARK             NC2344.2
057400         MOVE END-STMT TO COMPUTED-A  ELSE                        NC2344.2
057500     MOVE ENTRY-2-1 (SUB-1, SUB-2) TO COMPUTED-A                  NC2344.2
057600     MOVE "SEE NOTE 2 FOR DIAGNOSTIC " TO RE-MARK.                NC2344.2
057700     PERFORM FAIL.                                                NC2344.2
057800*                                                                 NC2344.2
057900 TH3-INIT-F1-1.                                                   NC2344.2
058000     MOVE "TH3-TEST-F1-1" TO PAR-NAME.                            NC2344.2
058100     MOVE "VI-122 6.21" TO ANSI-REFERENCE.                        NC2344.2
058200     MOVE "SEARCH VARYING LEV 3" TO FEATURE.                      NC2344.2
058300     MOVE 1 TO SUB-1  SUB-2  SUB-3.                               NC2344.2
058400     MOVE "ELEM (01,01,01)" TO ELEM-HOLD-AREA.                    NC2344.2
058500     SET IDX-1-1 IDX-2-1 IDX-3-1 TO 1.                            NC2344.2
058600 TH3-TEST-F1-1.                                                   NC2344.2
058700     SEARCH GRP3-ENTRY-1 VARYING IDX-3                            NC2344.2
058800         WHEN ENTRY-3-1 (IDX-1-1, IDX-2-1, IDX-3-1)               NC2344.2
058900              = ELEM-HOLD-AREA                                    NC2344.2
059000              PERFORM PASS                                        NC2344.2
059100              GO TO TH3-WRITE-F1-1.                               NC2344.2
059200     GO TO TH3-FAIL-F1-1.                                         NC2344.2
059300 TH3-DELETE-F1-1.                                                 NC2344.2
059400     PERFORM DE-LETE.                                             NC2344.2
059500     GO TO TH3-WRITE-F1-1.                                        NC2344.2
059600 TH3-FAIL-F1-1.                                                   NC2344.2
059700     PERFORM CHECK-FAIL3.                                         NC2344.2
059800 TH3-WRITE-F1-1.                                                  NC2344.2
059900     PERFORM PRINT-DETAIL.                                        NC2344.2
060000*                                                                 NC2344.2
060100 TH3-INIT-F1-2.                                                   NC2344.2
060200     MOVE "TH3-TEST-F1-2" TO PAR-NAME.                            NC2344.2
060300     MOVE "VI-122 6.21" TO ANSI-REFERENCE.                        NC2344.2
060400     MOVE "SEARCH VARYING LEV 3" TO FEATURE.                      NC2344.2
060500     MOVE 05 TO SUB-1.                                            NC2344.2
060600     MOVE 06 TO SUB-2.                                            NC2344.2
060700     MOVE 07 TO SUB-3.                                            NC2344.2
060800     SET IDX-1-1 TO 05.                                           NC2344.2
060900     SET IDX-2-1 TO 06.                                           NC2344.2
061000     SET IDX-3-1 TO 1.                                            NC2344.2
061100     MOVE "ELEM (05,06,07)" TO ELEM-HOLD-AREA.                    NC2344.2
061200 TH3-TEST-F1-2.                                                   NC2344.2
061300     SEARCH GRP3-ENTRY-1 VARYING IDX-3-1 AT END                   NC2344.2
061400         GO TO TH3-FAIL-F1-2                                      NC2344.2
061500         WHEN ENTRY-3-1 (IDX-1-1, IDX-2-1, IDX-3-1)               NC2344.2
061600             = ELEM-HOLD-AREA                                     NC2344.2
061700             NEXT SENTENCE.                                       NC2344.2
061800     PERFORM PASS                                                 NC2344.2
061900     GO TO TH3-WRITE-F1-2.                                        NC2344.2
062000 TH3-DELETE-F1-2.                                                 NC2344.2
062100     PERFORM DE-LETE.                                             NC2344.2
062200     GO TO TH3-WRITE-F1-2.                                        NC2344.2
062300 TH3-FAIL-F1-2.                                                   NC2344.2
062400     PERFORM CHECK-FAIL3.                                         NC2344.2
062500 TH3-WRITE-F1-2.                                                  NC2344.2
062600     PERFORM PRINT-DETAIL.                                        NC2344.2
062700*                                                                 NC2344.2
062800 TH3-INIT-F1-3.                                                   NC2344.2
062900     MOVE "TH3-TEST-F1-3" TO PAR-NAME.                            NC2344.2
063000     MOVE "VI-122 6.21" TO ANSI-REFERENCE.                        NC2344.2
063100     MOVE "SEARCH VARYING LEV 3" TO FEATURE.                      NC2344.2
063200     MOVE 10 TO SUB-1 SUB-2 SUB-3.                                NC2344.2
063300     SET IDX-1-1 IDX-2-1 TO 10.                                   NC2344.2
063400     SET IDX-3-1 TO 1.                                            NC2344.2
063500     MOVE "ELEM (10,10,10)" TO ELEM-HOLD-AREA.                    NC2344.2
063600 TH3-TEST-F1-3.                                                   NC2344.2
063700     SEARCH GRP3-ENTRY-1 VARYING IDX-3-1 AT END                   NC2344.2
063800         GO TO TH3-FAIL-F1-3                                      NC2344.2
063900         WHEN ENTRY-3-1 (IDX-1-1, IDX-2-1, IDX-3-1)               NC2344.2
064000             = ELEM-HOLD-AREA                                     NC2344.2
064100             NEXT SENTENCE.                                       NC2344.2
064200     PERFORM PASS                                                 NC2344.2
064300     GO TO TH3-WRITE-F1-3.                                        NC2344.2
064400 TH3-DELETE-F1-3.                                                 NC2344.2
064500     PERFORM DE-LETE.                                             NC2344.2
064600     GO TO TH3-WRITE-F1-3.                                        NC2344.2
064700 TH3-FAIL-F1-3.                                                   NC2344.2
064800     PERFORM CHECK-FAIL3.                                         NC2344.2
064900 TH3-WRITE-F1-3.                                                  NC2344.2
065000     PERFORM PRINT-DETAIL.                                        NC2344.2
065100*                                                                 NC2344.2
065200 TH3-INIT-F1-4.                                                   NC2344.2
065300     MOVE "TH3-TEST-F1-4" TO PAR-NAME.                            NC2344.2
065400     MOVE "VI-122 6.21" TO ANSI-REFERENCE.                        NC2344.2
065500     MOVE "SEARCH VARYING LEV 3" TO FEATURE.                      NC2344.2
065600     MOVE "ELEM (07,06,05)" TO ELEM-HOLD-AREA.                    NC2344.2
065700     MOVE 07 TO SUB-1.                                            NC2344.2
065800     MOVE 06 TO SUB-2.                                            NC2344.2
065900     MOVE 05 TO SUB-3.                                            NC2344.2
066000     SET IDX-1-1 TO 07.                                           NC2344.2
066100     SET IDX-2-1 TO 06.                                           NC2344.2
066200     SET IDX-3-1 TO 03.                                           NC2344.2
066300 TH3-TEST-F1-4.                                                   NC2344.2
066400     SEARCH GRP3-ENTRY-1 VARYING IDX-3-1 AT END                   NC2344.2
066500         GO TO TH3-FAIL-F1-4                                      NC2344.2
066600         WHEN ENTRY-3-1 (IDX-1-1, IDX-2-1, IDX-3-1)               NC2344.2
066700         = ELEM-HOLD-AREA                                         NC2344.2
066800         NEXT SENTENCE.                                           NC2344.2
066900     PERFORM PASS                                                 NC2344.2
067000     GO TO TH3-WRITE-F1-4.                                        NC2344.2
067100 TH3-DELETE-F1-4.                                                 NC2344.2
067200     PERFORM DE-LETE.                                             NC2344.2
067300     GO TO TH3-WRITE-F1-4.                                        NC2344.2
067400 TH3-FAIL-F1-4.                                                   NC2344.2
067500     PERFORM CHECK-FAIL3.                                         NC2344.2
067600 TH3-WRITE-F1-4.                                                  NC2344.2
067700     PERFORM PRINT-DETAIL.                                        NC2344.2
067800     GO TO MLT-INIT-F1-1.                                         NC2344.2
067900*                                                                 NC2344.2
068000 CHECK-FAIL3.                                                     NC2344.2
068100     MOVE ELEM-HOLD-AREA TO CORRECT-A.                            NC2344.2
068200     IF ENTRY-3-1 (SUB-1, SUB-2, SUB-3) EQUAL TO ELEM-HOLD-AREA   NC2344.2
068300         MOVE "IDX-3-1" TO END-IDX                                NC2344.2
068400         SET IDX-VALU TO IDX-3-1                                  NC2344.2
068500         MOVE "SEE NOTE 1 FOR DIAGNOSTIC " TO RE-MARK             NC2344.2
068600         MOVE END-STMT TO COMPUTED-A ELSE                         NC2344.2
068700     MOVE ENTRY-3-1 (SUB-1, SUB-2, SUB-3) TO COMPUTED-A           NC2344.2
068800     MOVE "SEE NOTE 2 FOR DIAGNOSTIC " TO RE-MARK.                NC2344.2
068900     PERFORM FAIL.                                                NC2344.2
069000*                                                                 NC2344.2
069100 MLT-INIT-F1-1.                                                   NC2344.2
069200     MOVE "MLT-TEST-F1-1  " TO PAR-NAME.                          NC2344.2
069300     MOVE "VI-122 6.21" TO ANSI-REFERENCE.                        NC2344.2
069400     MOVE "MULTIPLE SEARCH STMT" TO FEATURE.                      NC2344.2
069500     MOVE "GRP08" TO GRP-HOLD-AREA.                               NC2344.2
069600     MOVE "SEC (08,07)" TO SEC-HOLD-AREA.                         NC2344.2
069700     SET IDX-1-1 IDX-2-1 TO 1.                                    NC2344.2
069800 MLT-TEST-F1-1.                                                   NC2344.2
069900     SEARCH GRP-ENTRY-1 VARYING IDX-1-1                           NC2344.2
070000            AT END GO TO MLT-FAIL-F1-1-A                          NC2344.2
070100            WHEN ENTRY-1-1 (IDX-1-1) = "GRP08" NEXT SENTENCE.     NC2344.2
070200     SEARCH GRP2-ENTRY-1 VARYING IDX-2-1                          NC2344.2
070300            AT END GO TO MLT-FAIL-F1-1-B                          NC2344.2
070400            WHEN ENTRY-2-1 (IDX-1-1, IDX-2-1) = SEC-HOLD-AREA     NC2344.2
070500                 NEXT SENTENCE.                                   NC2344.2
070600     PERFORM PASS                                                 NC2344.2
070700     GO TO MLT-WRITE-F1-1.                                        NC2344.2
070800 MLT-DELETE-F1-1.                                                 NC2344.2
070900     PERFORM DE-LETE.                                             NC2344.2
071000     GO TO MLT-WRITE-F1-1.                                        NC2344.2
071100*                                                                 NC2344.2
071200 MLT-FAIL-F1-1-A.                                                 NC2344.2
071300     MOVE GRP-HOLD-AREA TO CORRECT-A.                             NC2344.2
071400     IF ENTRY-1-1 (08) EQUAL TO GRP-HOLD-AREA                     NC2344.2
071500         MOVE "IDX-1-1" TO END-IDX                                NC2344.2
071600         MOVE "SEE NOTE 1 FOR DIAGNOSTIC " TO RE-MARK             NC2344.2
071700         SET IDX-VALU TO IDX-1-1                                  NC2344.2
071800         MOVE END-STMT TO COMPUTED-A ELSE                         NC2344.2
071900     MOVE ENTRY-1-1 (08) TO COMPUTED-A                            NC2344.2
072000     MOVE "SEE NOTE 2 FOR DIAGNOSTIC " TO RE-MARK.                NC2344.2
072100     PERFORM FAIL                                                 NC2344.2
072200     GO TO MLT-WRITE-F1-1.                                        NC2344.2
072300*                                                                 NC2344.2
072400 MLT-FAIL-F1-1-B.                                                 NC2344.2
072500     MOVE SEC-HOLD-AREA TO CORRECT-A.                             NC2344.2
072600     IF ENTRY-2-1 (08, 07) EQUAL TO SEC-HOLD-AREA                 NC2344.2
072700         MOVE "IDX-2-1" TO END-IDX                                NC2344.2
072800         SET IDX-VALU TO IDX-2-1                                  NC2344.2
072900         MOVE "SEE NOTE 1 FOR DIAGNOSTIC " TO RE-MARK             NC2344.2
073000         MOVE END-STMT TO COMPUTED-A ELSE                         NC2344.2
073100     MOVE ENTRY-2-1 (08, 07) TO COMPUTED-A                        NC2344.2
073200     MOVE "SEE NOTE 2 FOR DIAGNOSTIC " TO RE-MARK.                NC2344.2
073300     PERFORM FAIL.                                                NC2344.2
073400 MLT-WRITE-F1-1.                                                  NC2344.2
073500     PERFORM PRINT-DETAIL.                                        NC2344.2
073600*                                                                 NC2344.2
073700 MLT-INIT-F1-2.                                                   NC2344.2
073800     MOVE "MLT-TEST-F1-2  " TO PAR-NAME.                          NC2344.2
073900     MOVE "VI-122 6.21" TO ANSI-REFERENCE.                        NC2344.2
074000     MOVE "MULTIPLE SEARCH STMT" TO FEATURE.                      NC2344.2
074100     MOVE "GRP04" TO GRP-HOLD-AREA.                               NC2344.2
074200     MOVE "SEC (04,04)" TO SEC-HOLD-AREA.                         NC2344.2
074300     MOVE "ELEM (04,04,04)" TO ELEM-HOLD-AREA.                    NC2344.2
074400     SET IDX-1-1 IDX-2-1 IDX-3-1 TO 1.                            NC2344.2
074500 MLT-TEST-F1-2.                                                   NC2344.2
074600     SEARCH GRP-ENTRY-1  VARYING IDX-1-1 AT END                   NC2344.2
074700         GO TO MLT-FAIL-F1-2-A WHEN ENTRY-1-1 (IDX-1-1) =         NC2344.2
074800         GRP-HOLD-AREA  NEXT SENTENCE.                            NC2344.2
074900     SEARCH GRP2-ENTRY-1 VARYING IDX-2-1 AT END                   NC2344.2
075000         GO TO MLT-FAIL-F1-2-B WHEN ENTRY-2-1 (IDX-1-1, IDX-2-1) =NC2344.2
075100         SEC-HOLD-AREA  NEXT SENTENCE.                            NC2344.2
075200     SEARCH GRP3-ENTRY-1 VARYING IDX-3-1 AT END                   NC2344.2
075300         GO TO MLT-FAIL-F1-2-C WHEN ENTRY-3-1                     NC2344.2
075400             (IDX-1-1, IDX-2-1, IDX-3-1)                          NC2344.2
075500             = ELEM-HOLD-AREA NEXT SENTENCE.                      NC2344.2
075600     PERFORM PASS                                                 NC2344.2
075700     GO TO MLT-WRITE-F1-2.                                        NC2344.2
075800 MLT-DELETE-F1-2.                                                 NC2344.2
075900     PERFORM DE-LETE                                              NC2344.2
076000     GO TO MLT-WRITE-F1-2.                                        NC2344.2
076100 MLT-FAIL-F1-2-A.                                                 NC2344.2
076200     MOVE GRP-HOLD-AREA TO CORRECT-A.                             NC2344.2
076300     IF ENTRY-1-1 (04) EQUAL TO GRP-HOLD-AREA                     NC2344.2
076400         MOVE "IDX-1-1" TO END-IDX                                NC2344.2
076500         MOVE "SEE NOTE 1 FOR DIAGNOSTIC " TO RE-MARK             NC2344.2
076600         SET IDX-VALU TO IDX-1-1                                  NC2344.2
076700         MOVE END-STMT TO COMPUTED-A ELSE                         NC2344.2
076800     MOVE ENTRY-1-1 (04) TO COMPUTED-A                            NC2344.2
076900     MOVE "SEE NOTE 2 FOR DIAGNOSTIC " TO RE-MARK.                NC2344.2
077000     PERFORM FAIL.                                                NC2344.2
077100     GO TO MLT-WRITE-F1-2.                                        NC2344.2
077200                                                                  NC2344.2
077300 MLT-FAIL-F1-2-B.                                                 NC2344.2
077400     MOVE  SEC-HOLD-AREA TO CORRECT-A.                            NC2344.2
077500     IF ENTRY-2-1 (04, 04) EQUAL TO SEC-HOLD-AREA                 NC2344.2
077600         MOVE "IDX-2-1" TO END-IDX                                NC2344.2
077700         MOVE "SEE NOTE 1 FOR DIAGNOSTIC " TO RE-MARK             NC2344.2
077800         SET IDX-VALU TO IDX-2-1                                  NC2344.2
077900         MOVE END-STMT TO COMPUTED-A ELSE                         NC2344.2
078000     MOVE ENTRY-2-1 (04, 04) TO COMPUTED-A                        NC2344.2
078100     MOVE "SEE NOTE 2 FOR DIAGNOSTIC " TO RE-MARK.                NC2344.2
078200     PERFORM FAIL                                                 NC2344.2
078300     GO TO MLT-WRITE-F1-2.                                        NC2344.2
078400                                                                  NC2344.2
078500 MLT-FAIL-F1-2-C.                                                 NC2344.2
078600     MOVE ELEM-HOLD-AREA TO CORRECT-A.                            NC2344.2
078700     IF ENTRY-3-1 (04, 04, 04) EQUAL TO ELEM-HOLD-AREA            NC2344.2
078800         MOVE "IDX-3-1" TO END-IDX                                NC2344.2
078900         MOVE "SEE NOTE 1 FOR DIAGNOSTIC " TO RE-MARK             NC2344.2
079000         SET IDX-VALU TO IDX-3-1                                  NC2344.2
079100         MOVE END-STMT TO COMPUTED-A ELSE                         NC2344.2
079200     MOVE ENTRY-3-1 (04, 04, 04) TO COMPUTED-A                    NC2344.2
079300     MOVE "SEE NOTE 2 FOR DIAGNOSTIC " TO RE-MARK.                NC2344.2
079400     PERFORM FAIL.                                                NC2344.2
079500 MLT-WRITE-F1-2.                                                  NC2344.2
079600     PERFORM PRINT-DETAIL.                                        NC2344.2
079700*                                                                 NC2344.2
079800 SPC-INIT-F1-1.                                                   NC2344.2
079900     MOVE "SPC-TEST-F1-1" TO PAR-NAME.                            NC2344.2
080000     MOVE "VI-122 6.21" TO ANSI-REFERENCE.                        NC2344.2
080100     MOVE "SEARCH WITH HI INDEX" TO FEATURE.                      NC2344.2
080200     SET IDX-1-1 TO 4.                                            NC2344.2
080300 SPC-TEST-F1-1.                                                   NC2344.2
080400     SEARCH GRP-ENTRY-1 VARYING IDX-1-1                           NC2344.2
080500          AT END PERFORM PASS                                     NC2344.2
080600                 GO TO SPC-WRITE-F1-1                             NC2344.2
080700          WHEN ENTRY-1-1 (IDX-1-1) = "GRP03"                      NC2344.2
080800                 GO TO SPC-FAIL-F1-1.                             NC2344.2
080900 SPC-DELETE-F1-1.                                                 NC2344.2
081000     PERFORM DE-LETE.                                             NC2344.2
081100     GO TO SPC-WRITE-F1-1.                                        NC2344.2
081200 SPC-FAIL-F1-1.                                                   NC2344.2
081300     MOVE SPACES TO CORRECT-A.                                    NC2344.2
081400     MOVE ENTRY-1-1 (03) TO COMPUTED-A.                           NC2344.2
081500     MOVE SPACES TO RE-MARK.                                      NC2344.2
081600     PERFORM FAIL.                                                NC2344.2
081700 SPC-WRITE-F1-1.                                                  NC2344.2
081800     PERFORM PRINT-DETAIL.                                        NC2344.2
081900*                                                                 NC2344.2
082000 SP2-INIT-F1-1.                                                   NC2344.2
082100     MOVE "SP2-TEST-F1-1" TO PAR-NAME.                            NC2344.2
082200     MOVE "VI-122 6.21" TO ANSI-REFERENCE.                        NC2344.2
082300     MOVE "SEARCH WITH HI INDEX" TO FEATURE.                      NC2344.2
082400     SET IDX-1-1 TO 4.                                            NC2344.2
082500     SET IDX-2-1 TO 5.                                            NC2344.2
082600 SP2-TEST-F1-1.                                                   NC2344.2
082700     SEARCH GRP-ENTRY-1 VARYING IDX-1-1 AT END                    NC2344.2
082800         GO TO SP2-FAIL-F1-1-A                                    NC2344.2
082900     WHEN ENTRY-1-1 (IDX-1-1) = "GRP04" NEXT SENTENCE.            NC2344.2
083000     SET IDX-1-1 TO 4.                                            NC2344.2
083100     SEARCH GRP2-ENTRY-1 VARYING IDX-2-1                          NC2344.2
083200         AT END PERFORM PASS                                      NC2344.2
083300                GO TO SP2-WRITE-F1-1                              NC2344.2
083400     WHEN ENTRY-2-1 (IDX-1-1, IDX-2-1) = "SEC (04,04)"            NC2344.2
083500         GO TO SP2-FAIL-F1-1-B.                                   NC2344.2
083600 SP2-DELETE-F1-1.                                                 NC2344.2
083700     PERFORM DE-LETE.                                             NC2344.2
083800     GO TO SP2-WRITE-F1-1.                                        NC2344.2
083900 SP2-FAIL-F1-1-A.                                                 NC2344.2
084000     MOVE "GRP04" TO CORRECT-A.                                   NC2344.2
084100     IF ENTRY-1-1 (04) EQUAL TO "GRP04"                           NC2344.2
084200         MOVE "IDX-2-1" TO END-IDX                                NC2344.2
084300         MOVE "SEE NOTE 1 FOR DIAGNOSTIC " TO RE-MARK             NC2344.2
084400         SET IDX-VALU TO IDX-2-1                                  NC2344.2
084500         MOVE END-STMT TO COMPUTED-A ELSE                         NC2344.2
084600     MOVE ENTRY-1-1 (04) TO COMPUTED-A                            NC2344.2
084700     MOVE "SEE NOTE 2 FOR DIAGNOSTIC " TO RE-MARK.                NC2344.2
084800     PERFORM FAIL.                                                NC2344.2
084900     GO TO SP2-WRITE-F1-1.                                        NC2344.2
085000*                                                                 NC2344.2
085100 SP2-FAIL-F1-1-B.                                                 NC2344.2
085200     MOVE ENTRY-2-1 (04, 04) TO COMPUTED-A.                       NC2344.2
085300     MOVE SPACES TO CORRECT-A.                                    NC2344.2
085400     PERFORM FAIL.                                                NC2344.2
085500 SP2-WRITE-F1-1.                                                  NC2344.2
085600     PERFORM PRINT-DETAIL.                                        NC2344.2
085700*                                                                 NC2344.2
085800 SP3-INIT-F1-1.                                                   NC2344.2
085900     MOVE "SP3-TEST-F1-1" TO PAR-NAME.                            NC2344.2
086000     MOVE "VI-122 6.21" TO ANSI-REFERENCE.                        NC2344.2
086100     MOVE "SEARCH WITH HI INDEX" TO FEATURE.                      NC2344.2
086200     SET IDX-1-1 TO 02.                                           NC2344.2
086300 SP3-TEST-F1-1.                                                   NC2344.2
086400     SEARCH GRP-ENTRY-1 VARYING IDX-1-1                           NC2344.2
086500         AT END                                                   NC2344.2
086600                GO TO SP3-FAIL-F1-1-A                             NC2344.2
086700         WHEN ENTRY-1-1 (IDX-1-1) EQUAL TO "GRP02"                NC2344.2
086800                NEXT SENTENCE.                                    NC2344.2
086900     SET IDX-1-1 TO 02.                                           NC2344.2
087000     SET IDX-2-1 TO 01.                                           NC2344.2
087100     SEARCH GRP2-ENTRY-1 VARYING IDX-2-1                          NC2344.2
087200         AT END                                                   NC2344.2
087300                GO TO SP3-FAIL-F1-1-B                             NC2344.2
087400         WHEN ENTRY-2-1 (IDX-1-1, IDX-2-1) = "SEC (02,03)"        NC2344.2
087500                NEXT SENTENCE.                                    NC2344.2
087600     SET IDX-1-1 TO 02.                                           NC2344.2
087700     SET IDX-2-1 TO 03.                                           NC2344.2
087800     SET IDX-3-1 TO 05.                                           NC2344.2
087900     SEARCH GRP3-ENTRY-1 VARYING IDX-3-1                          NC2344.2
088000         AT END PERFORM PASS                                      NC2344.2
088100                GO TO SP3-WRITE-F1-1                              NC2344.2
088200         WHEN ENTRY-3-1 (IDX-1-1, IDX-2-1, IDX-3-1)               NC2344.2
088300              = "ELEM (02,03,04)"                                 NC2344.2
088400                GO TO SP3-FAIL-F1-1-C.                            NC2344.2
088500 SP3-DELETE-F1-1.                                                 NC2344.2
088600     PERFORM DE-LETE.                                             NC2344.2
088700     GO TO SP3-WRITE-F1-1.                                        NC2344.2
088800 SP3-FAIL-F1-1-A.                                                 NC2344.2
088900     MOVE "GRP02" TO CORRECT-A.                                   NC2344.2
089000     IF ENTRY-1-1 (02) EQUAL TO "GRP02"                           NC2344.2
089100         MOVE "IDX-1-1" TO END-IDX                                NC2344.2
089200         MOVE "SEE NOTE 1 FOR DIAGNOSTIC " TO RE-MARK             NC2344.2
089300         SET IDX-VALU TO IDX-1-1                                  NC2344.2
089400         MOVE END-STMT TO COMPUTED-A ELSE                         NC2344.2
089500     MOVE ENTRY-1-1 (02) TO COMPUTED-A                            NC2344.2
089600     MOVE "SEE NOTE 2 FOR DIAGNOSTIC " TO RE-MARK.                NC2344.2
089700     PERFORM FAIL.                                                NC2344.2
089800     GO TO SP3-WRITE-F1-1.                                        NC2344.2
089900*                                                                 NC2344.2
090000 SP3-FAIL-F1-1-B.                                                 NC2344.2
090100     MOVE "SEC (02,03)" TO CORRECT-A.                             NC2344.2
090200     IF ENTRY-2-1 (02, 03) EQUAL TO "SEC (02,03)"                 NC2344.2
090300         MOVE "IDX-2-1"  TO END-IDX                               NC2344.2
090400         MOVE "SEE NOTE 1 FOR DIAGNOSTIC " TO RE-MARK             NC2344.2
090500         SET IDX-VALU TO IDX-2-1                                  NC2344.2
090600         MOVE END-STMT TO COMPUTED-A ELSE                         NC2344.2
090700     MOVE ENTRY-2-1 (02, 03) TO COMPUTED-A                        NC2344.2
090800     MOVE "SEE NOTE 2 FOR DIAGNOSTIC " TO RE-MARK.                NC2344.2
090900     PERFORM FAIL.                                                NC2344.2
091000     GO TO SP3-WRITE-F1-1.                                        NC2344.2
091100*                                                                 NC2344.2
091200 SP3-FAIL-F1-1-C.                                                 NC2344.2
091300     MOVE "INDEX SET HIGHER THAN ENTRY" TO RE-MARK                NC2344.2
091400     MOVE SPACES TO CORRECT-A                                     NC2344.2
091500     MOVE "ELEM (02,03,04)" TO COMPUTED-A                         NC2344.2
091600     PERFORM FAIL.                                                NC2344.2
091700 SP3-WRITE-F1-1.                                                  NC2344.2
091800     PERFORM PRINT-DETAIL.                                        NC2344.2
091900*                                                                 NC2344.2
092000 CCVS-EXIT SECTION.                                               NC2344.2
092100 CCVS-999999.                                                     NC2344.2
092200     GO TO CLOSE-FILES.                                           NC2344.2
      *END-OF,NC234A                                                                  
