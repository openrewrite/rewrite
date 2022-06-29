      *HEADER,COBOL,NC231A                                                            
000100 IDENTIFICATION DIVISION.                                         NC2314.2
000200 PROGRAM-ID.                                                      NC2314.2
000300     NC231A.                                                      NC2314.2
000400****************************************************************  NC2314.2
000500*                                                              *  NC2314.2
000600*    VALIDATION FOR:-                                          *  NC2314.2
000700*                                                              *  NC2314.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2314.2
000900*                                                              *  NC2314.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC2314.2
001100*                                                              *  NC2314.2
001200****************************************************************  NC2314.2
001300*                                                              *  NC2314.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  NC2314.2
001500*                                                              *  NC2314.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  NC2314.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  NC2314.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  NC2314.2
001900*                                                              *  NC2314.2
002000****************************************************************  NC2314.2
002100*                                                              *  NC2314.2
002200*    PROGRAM NC231A USES FORMAT 1 OF THE "SEARCH" STATEMENT TO *  NC2314.2
002300*    ACCESS THREE AND SEVEN DIMENSIONAL TABLES.                *  NC2314.2
002400*    THE OPTIONAL "VARYING" PHRASE IS USED WITH AN IDENTIFIER. *  NC2314.2
002500*    THE SCOPE TERMINATOR "END-SEARCH" IS ALSO TESTED.         *  NC2314.2
002600*                                                              *  NC2314.2
002700****************************************************************  NC2314.2
002800 ENVIRONMENT DIVISION.                                            NC2314.2
002900 CONFIGURATION SECTION.                                           NC2314.2
003000 SOURCE-COMPUTER.                                                 NC2314.2
003100     XXXXX082.                                                    NC2314.2
003200 OBJECT-COMPUTER.                                                 NC2314.2
003300     XXXXX083.                                                    NC2314.2
003400 INPUT-OUTPUT SECTION.                                            NC2314.2
003500 FILE-CONTROL.                                                    NC2314.2
003600     SELECT PRINT-FILE ASSIGN TO                                  NC2314.2
003700     XXXXX055.                                                    NC2314.2
003800 DATA DIVISION.                                                   NC2314.2
003900 FILE SECTION.                                                    NC2314.2
004000 FD  PRINT-FILE.                                                  NC2314.2
004100 01  PRINT-REC PICTURE X(120).                                    NC2314.2
004200 01  DUMMY-RECORD PICTURE X(120).                                 NC2314.2
004300 WORKING-STORAGE SECTION.                                         NC2314.2
004400 77  SUB-1              PICTURE S99  VALUE ZERO.                  NC2314.2
004500 77  SUB-2              PICTURE 99   VALUE ZERO.                  NC2314.2
004600 77  SUB-3              PICTURE 99   VALUE ZERO.                  NC2314.2
004700 77  CON-7              PICTURE 99   VALUE 07.                    NC2314.2
004800 77  CON-10             PICTURE 99   VALUE 10.                    NC2314.2
004900 77  GRP-HOLD-AREA                PICTURE X(5) VALUE SPACES.      NC2314.2
005000 77  CON-5              PICTURE 99  VALUE 05.                     NC2314.2
005100 77  SEC-HOLD-AREA                PICTURE X(11) VALUE SPACES.     NC2314.2
005200 77  CON-6              PICTURE 99  VALUE 06.                     NC2314.2
005300 77  ELEM-HOLD-AREA               PICTURE X(15)  VALUE SPACES.    NC2314.2
005400 77  L1-HOLD                     PIC XX.                          NC2314.2
005500 77  L2-HOLD                     PIC XX.                          NC2314.2
005600 77  L3-HOLD                     PIC XX.                          NC2314.2
005700 77  L4-HOLD                     PIC XX.                          NC2314.2
005800 77  L5-HOLD                     PIC XX.                          NC2314.2
005900 77  L6-HOLD                     PIC XX.                          NC2314.2
006000 77  L7-HOLD                     PIC XX.                          NC2314.2
006100 77  N1                         PIC 99.                           NC2314.2
006200 77  N2                         PIC 99.                           NC2314.2
006300 77  N3                         PIC 99.                           NC2314.2
006400 77  N4                         PIC 99.                           NC2314.2
006500 77  N5                         PIC 99.                           NC2314.2
006600 77  N6                         PIC 99.                           NC2314.2
006700 77  N7                         PIC 99.                           NC2314.2
006800 01  GRP-NAME.                                                    NC2314.2
006900     02  FILLER              PICTURE XXX    VALUE "GRP".          NC2314.2
007000     02  ADD-GRP             PICTURE 99     VALUE 01.             NC2314.2
007100                                                                  NC2314.2
007200 01  SEC-NAME.                                                    NC2314.2
007300     02  FILLER              PICTURE X(5)   VALUE "SEC (".        NC2314.2
007400     02  SEC-GRP             PICTURE 99     VALUE 00.             NC2314.2
007500     02  FILLER              PICTURE X      VALUE ",".            NC2314.2
007600     02  ADD-SEC             PICTURE 99     VALUE 01.             NC2314.2
007700     02  FILLER              PICTURE X      VALUE ")".            NC2314.2
007800                                                                  NC2314.2
007900 01  ELEM-NAME.                                                   NC2314.2
008000     02  FILLER              PICTURE X(6)   VALUE "ELEM (".       NC2314.2
008100     02  ELEM-GRP            PICTURE 99     VALUE 00.             NC2314.2
008200     02  FILLER              PICTURE X      VALUE ",".            NC2314.2
008300     02  ELEM-SEC            PICTURE 99     VALUE 00.             NC2314.2
008400     02  FILLER              PICTURE X      VALUE ",".            NC2314.2
008500     02  ADD-ELEM            PICTURE 99     VALUE 01.             NC2314.2
008600     02  FILLER              PICTURE X      VALUE ")".            NC2314.2
008700                                                                  NC2314.2
008800                                                                  NC2314.2
008900 01  3-DIMENSION-TBL.                                             NC2314.2
009000     02  GRP-ENTRY OCCURS 10 TIMES INDEXED BY IDX-1.              NC2314.2
009100         03  ENTRY-1         PICTURE X(5).                        NC2314.2
009200         03  GRP2-ENTRY OCCURS 10 TIMES INDEXED BY IDX-2.         NC2314.2
009300             04  ENTRY-2     PICTURE X(11).                       NC2314.2
009400             04  GRP3-ENTRY OCCURS 10 TIMES INDEXED BY IDX-3.     NC2314.2
009500                 05  ENTRY-3 PICTURE X(15).                       NC2314.2
009600                                                                  NC2314.2
009700 01  7-DIMENSION-TBL.                                             NC2314.2
009800   02  GRP-7-1-ENTRY             OCCURS 2 INDEXED BY IX-1.        NC2314.2
009900     03  ENTRY-7-1               PIC XX.                          NC2314.2
010000     03  GRP-7-2-ENTRY           OCCURS 2 INDEXED BY IX-2.        NC2314.2
010100       04  ENTRY-7-2             PIC XX.                          NC2314.2
010200       04  GRP-7-3-ENTRY         OCCURS 2 INDEXED BY IX-3.        NC2314.2
010300         05  ENTRY-7-3           PIC XX.                          NC2314.2
010400         05  GRP-7-4-ENTRY       OCCURS 2 INDEXED BY IX-4.        NC2314.2
010500           06  ENTRY-7-4         PIC XX.                          NC2314.2
010600           06  GRP-7-5-ENTRY     OCCURS 2 INDEXED BY IX-5.        NC2314.2
010700             07  ENTRY-7-5       PIC XX.                          NC2314.2
010800             07  GRP-7-6-ENTRY   OCCURS 2 INDEXED BY IX-6.        NC2314.2
010900               08  ENTRY-7-6     PIC XX.                          NC2314.2
011000               08  GRP-7-7-ENTRY OCCURS 2 INDEXED BY IX-7.        NC2314.2
011100                 09  ENTRY-7-7   PIC XX.                          NC2314.2
011200                                                                  NC2314.2
011300 01  END-STMT.                                                    NC2314.2
011400     02  FILLER                   PICTURE X(7) VALUE "AT END ".   NC2314.2
011500     02  END-IDX                  PICTURE X(5) VALUE SPACES.      NC2314.2
011600     02  FILLER                   PICTURE XXX  VALUE " = ".       NC2314.2
011700     02  IDX-VALU                 PICTURE 99  VALUE 00.           NC2314.2
011800     02  FILLER                   PICTURE XXX  VALUE SPACES.      NC2314.2
011900                                                                  NC2314.2
012000 01  NOTE-1.                                                      NC2314.2
012100     02  FILLER                   PICTURE X(74)  VALUE            NC2314.2
012200     "NOTE 1 - CORRECT AND COMPUTED DATA ARE EQUAL BUT THE AT END NC2314.2
012300-     "PATH WAS TAKEN".                                           NC2314.2
012400     02  FILLER                   PICTURE X(46)  VALUE SPACE.     NC2314.2
012500                                                                  NC2314.2
012600 01  NOTE-2.                                                      NC2314.2
012700     02  FILLER                   PICTURE X(112)  VALUE           NC2314.2
012800     "NOTE 2 - CORRECT AND COMPUTED DATA ARE NOT EQUAL. THE COMPUTNC2314.2
012900-    "ED ENTRY WAS EXTRACTED FROM THE TABLE BY SUBSCRIPTS.".      NC2314.2
013000     02  FILLER                   PICTURE X(8)  VALUE SPACE.      NC2314.2
013100                                                                  NC2314.2
013200 01  TEST-RESULTS.                                                NC2314.2
013300     02 FILLER                   PIC X      VALUE SPACE.          NC2314.2
013400     02 FEATURE                  PIC X(20)  VALUE SPACE.          NC2314.2
013500     02 FILLER                   PIC X      VALUE SPACE.          NC2314.2
013600     02 P-OR-F                   PIC X(5)   VALUE SPACE.          NC2314.2
013700     02 FILLER                   PIC X      VALUE SPACE.          NC2314.2
013800     02  PAR-NAME.                                                NC2314.2
013900       03 FILLER                 PIC X(19)  VALUE SPACE.          NC2314.2
014000       03  PARDOT-X              PIC X      VALUE SPACE.          NC2314.2
014100       03 DOTVALUE               PIC 99     VALUE ZERO.           NC2314.2
014200     02 FILLER                   PIC X(8)   VALUE SPACE.          NC2314.2
014300     02 RE-MARK                  PIC X(61).                       NC2314.2
014400 01  TEST-COMPUTED.                                               NC2314.2
014500     02 FILLER                   PIC X(30)  VALUE SPACE.          NC2314.2
014600     02 FILLER                   PIC X(17)  VALUE                 NC2314.2
014700            "       COMPUTED=".                                   NC2314.2
014800     02 COMPUTED-X.                                               NC2314.2
014900     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          NC2314.2
015000     03 COMPUTED-N               REDEFINES COMPUTED-A             NC2314.2
015100                                 PIC -9(9).9(9).                  NC2314.2
015200     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         NC2314.2
015300     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     NC2314.2
015400     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     NC2314.2
015500     03       CM-18V0 REDEFINES COMPUTED-A.                       NC2314.2
015600         04 COMPUTED-18V0                    PIC -9(18).          NC2314.2
015700         04 FILLER                           PIC X.               NC2314.2
015800     03 FILLER PIC X(50) VALUE SPACE.                             NC2314.2
015900 01  TEST-CORRECT.                                                NC2314.2
016000     02 FILLER PIC X(30) VALUE SPACE.                             NC2314.2
016100     02 FILLER PIC X(17) VALUE "       CORRECT =".                NC2314.2
016200     02 CORRECT-X.                                                NC2314.2
016300     03 CORRECT-A                  PIC X(20) VALUE SPACE.         NC2314.2
016400     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      NC2314.2
016500     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         NC2314.2
016600     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     NC2314.2
016700     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     NC2314.2
016800     03      CR-18V0 REDEFINES CORRECT-A.                         NC2314.2
016900         04 CORRECT-18V0                     PIC -9(18).          NC2314.2
017000         04 FILLER                           PIC X.               NC2314.2
017100     03 FILLER PIC X(2) VALUE SPACE.                              NC2314.2
017200     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     NC2314.2
017300 01  CCVS-C-1.                                                    NC2314.2
017400     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PANC2314.2
017500-    "SS  PARAGRAPH-NAME                                          NC2314.2
017600-    "       REMARKS".                                            NC2314.2
017700     02 FILLER                     PIC X(20)    VALUE SPACE.      NC2314.2
017800 01  CCVS-C-2.                                                    NC2314.2
017900     02 FILLER                     PIC X        VALUE SPACE.      NC2314.2
018000     02 FILLER                     PIC X(6)     VALUE "TESTED".   NC2314.2
018100     02 FILLER                     PIC X(15)    VALUE SPACE.      NC2314.2
018200     02 FILLER                     PIC X(4)     VALUE "FAIL".     NC2314.2
018300     02 FILLER                     PIC X(94)    VALUE SPACE.      NC2314.2
018400 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       NC2314.2
018500 01  REC-CT                        PIC 99       VALUE ZERO.       NC2314.2
018600 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       NC2314.2
018700 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       NC2314.2
018800 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       NC2314.2
018900 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       NC2314.2
019000 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       NC2314.2
019100 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       NC2314.2
019200 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      NC2314.2
019300 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       NC2314.2
019400 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     NC2314.2
019500 01  CCVS-H-1.                                                    NC2314.2
019600     02  FILLER                    PIC X(39)    VALUE SPACES.     NC2314.2
019700     02  FILLER                    PIC X(42)    VALUE             NC2314.2
019800     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 NC2314.2
019900     02  FILLER                    PIC X(39)    VALUE SPACES.     NC2314.2
020000 01  CCVS-H-2A.                                                   NC2314.2
020100   02  FILLER                        PIC X(40)  VALUE SPACE.      NC2314.2
020200   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  NC2314.2
020300   02  FILLER                        PIC XXXX   VALUE             NC2314.2
020400     "4.2 ".                                                      NC2314.2
020500   02  FILLER                        PIC X(28)  VALUE             NC2314.2
020600            " COPY - NOT FOR DISTRIBUTION".                       NC2314.2
020700   02  FILLER                        PIC X(41)  VALUE SPACE.      NC2314.2
020800                                                                  NC2314.2
020900 01  CCVS-H-2B.                                                   NC2314.2
021000   02  FILLER                        PIC X(15)  VALUE             NC2314.2
021100            "TEST RESULT OF ".                                    NC2314.2
021200   02  TEST-ID                       PIC X(9).                    NC2314.2
021300   02  FILLER                        PIC X(4)   VALUE             NC2314.2
021400            " IN ".                                               NC2314.2
021500   02  FILLER                        PIC X(12)  VALUE             NC2314.2
021600     " HIGH       ".                                              NC2314.2
021700   02  FILLER                        PIC X(22)  VALUE             NC2314.2
021800            " LEVEL VALIDATION FOR ".                             NC2314.2
021900   02  FILLER                        PIC X(58)  VALUE             NC2314.2
022000     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2314.2
022100 01  CCVS-H-3.                                                    NC2314.2
022200     02  FILLER                      PIC X(34)  VALUE             NC2314.2
022300            " FOR OFFICIAL USE ONLY    ".                         NC2314.2
022400     02  FILLER                      PIC X(58)  VALUE             NC2314.2
022500     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC2314.2
022600     02  FILLER                      PIC X(28)  VALUE             NC2314.2
022700            "  COPYRIGHT   1985 ".                                NC2314.2
022800 01  CCVS-E-1.                                                    NC2314.2
022900     02 FILLER                       PIC X(52)  VALUE SPACE.      NC2314.2
023000     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              NC2314.2
023100     02 ID-AGAIN                     PIC X(9).                    NC2314.2
023200     02 FILLER                       PIC X(45)  VALUE SPACES.     NC2314.2
023300 01  CCVS-E-2.                                                    NC2314.2
023400     02  FILLER                      PIC X(31)  VALUE SPACE.      NC2314.2
023500     02  FILLER                      PIC X(21)  VALUE SPACE.      NC2314.2
023600     02 CCVS-E-2-2.                                               NC2314.2
023700         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      NC2314.2
023800         03 FILLER                   PIC X      VALUE SPACE.      NC2314.2
023900         03 ENDER-DESC               PIC X(44)  VALUE             NC2314.2
024000            "ERRORS ENCOUNTERED".                                 NC2314.2
024100 01  CCVS-E-3.                                                    NC2314.2
024200     02  FILLER                      PIC X(22)  VALUE             NC2314.2
024300            " FOR OFFICIAL USE ONLY".                             NC2314.2
024400     02  FILLER                      PIC X(12)  VALUE SPACE.      NC2314.2
024500     02  FILLER                      PIC X(58)  VALUE             NC2314.2
024600     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2314.2
024700     02  FILLER                      PIC X(13)  VALUE SPACE.      NC2314.2
024800     02 FILLER                       PIC X(15)  VALUE             NC2314.2
024900             " COPYRIGHT 1985".                                   NC2314.2
025000 01  CCVS-E-4.                                                    NC2314.2
025100     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      NC2314.2
025200     02 FILLER                       PIC X(4)   VALUE " OF ".     NC2314.2
025300     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      NC2314.2
025400     02 FILLER                       PIC X(40)  VALUE             NC2314.2
025500      "  TESTS WERE EXECUTED SUCCESSFULLY".                       NC2314.2
025600 01  XXINFO.                                                      NC2314.2
025700     02 FILLER                       PIC X(19)  VALUE             NC2314.2
025800            "*** INFORMATION ***".                                NC2314.2
025900     02 INFO-TEXT.                                                NC2314.2
026000       04 FILLER                     PIC X(8)   VALUE SPACE.      NC2314.2
026100       04 XXCOMPUTED                 PIC X(20).                   NC2314.2
026200       04 FILLER                     PIC X(5)   VALUE SPACE.      NC2314.2
026300       04 XXCORRECT                  PIC X(20).                   NC2314.2
026400     02 INF-ANSI-REFERENCE           PIC X(48).                   NC2314.2
026500 01  HYPHEN-LINE.                                                 NC2314.2
026600     02 FILLER  PIC IS X VALUE IS SPACE.                          NC2314.2
026700     02 FILLER  PIC IS X(65)    VALUE IS "************************NC2314.2
026800-    "*****************************************".                 NC2314.2
026900     02 FILLER  PIC IS X(54)    VALUE IS "************************NC2314.2
027000-    "******************************".                            NC2314.2
027100 01  CCVS-PGM-ID                     PIC X(9)   VALUE             NC2314.2
027200     "NC231A".                                                    NC2314.2
027300 PROCEDURE DIVISION.                                              NC2314.2
027400 CCVS1 SECTION.                                                   NC2314.2
027500 OPEN-FILES.                                                      NC2314.2
027600     OPEN     OUTPUT PRINT-FILE.                                  NC2314.2
027700     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   NC2314.2
027800     MOVE    SPACE TO TEST-RESULTS.                               NC2314.2
027900     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             NC2314.2
028000     GO TO CCVS1-EXIT.                                            NC2314.2
028100 CLOSE-FILES.                                                     NC2314.2
028200     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   NC2314.2
028300 TERMINATE-CCVS.                                                  NC2314.2
028400*    EXIT PROGRAM.                                                NC2314.2
028500*TERMINATE-CALL.                                                  NC2314.2
028600     STOP     RUN.                                                NC2314.2
028700 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         NC2314.2
028800 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           NC2314.2
028900 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          NC2314.2
029000 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      NC2314.2
029100     MOVE "****TEST DELETED****" TO RE-MARK.                      NC2314.2
029200 PRINT-DETAIL.                                                    NC2314.2
029300     IF REC-CT NOT EQUAL TO ZERO                                  NC2314.2
029400             MOVE "." TO PARDOT-X                                 NC2314.2
029500             MOVE REC-CT TO DOTVALUE.                             NC2314.2
029600     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      NC2314.2
029700     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               NC2314.2
029800        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 NC2314.2
029900          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 NC2314.2
030000     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              NC2314.2
030100     MOVE SPACE TO CORRECT-X.                                     NC2314.2
030200     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         NC2314.2
030300     MOVE     SPACE TO RE-MARK.                                   NC2314.2
030400 HEAD-ROUTINE.                                                    NC2314.2
030500     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC2314.2
030600     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC2314.2
030700     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC2314.2
030800     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC2314.2
030900 COLUMN-NAMES-ROUTINE.                                            NC2314.2
031000     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2314.2
031100     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2314.2
031200     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        NC2314.2
031300 END-ROUTINE.                                                     NC2314.2
031400     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.NC2314.2
031500 END-RTN-EXIT.                                                    NC2314.2
031600     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2314.2
031700 END-ROUTINE-1.                                                   NC2314.2
031800      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      NC2314.2
031900      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               NC2314.2
032000      ADD PASS-COUNTER TO ERROR-HOLD.                             NC2314.2
032100*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   NC2314.2
032200      MOVE PASS-COUNTER TO CCVS-E-4-1.                            NC2314.2
032300      MOVE ERROR-HOLD TO CCVS-E-4-2.                              NC2314.2
032400      MOVE CCVS-E-4 TO CCVS-E-2-2.                                NC2314.2
032500      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           NC2314.2
032600  END-ROUTINE-12.                                                 NC2314.2
032700      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        NC2314.2
032800     IF       ERROR-COUNTER IS EQUAL TO ZERO                      NC2314.2
032900         MOVE "NO " TO ERROR-TOTAL                                NC2314.2
033000         ELSE                                                     NC2314.2
033100         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       NC2314.2
033200     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           NC2314.2
033300     PERFORM WRITE-LINE.                                          NC2314.2
033400 END-ROUTINE-13.                                                  NC2314.2
033500     IF DELETE-COUNTER IS EQUAL TO ZERO                           NC2314.2
033600         MOVE "NO " TO ERROR-TOTAL  ELSE                          NC2314.2
033700         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      NC2314.2
033800     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   NC2314.2
033900     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2314.2
034000      IF   INSPECT-COUNTER EQUAL TO ZERO                          NC2314.2
034100          MOVE "NO " TO ERROR-TOTAL                               NC2314.2
034200      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   NC2314.2
034300      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            NC2314.2
034400      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          NC2314.2
034500     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2314.2
034600 WRITE-LINE.                                                      NC2314.2
034700     ADD 1 TO RECORD-COUNT.                                       NC2314.2
034800*    IF RECORD-COUNT GREATER 50                                   NC2314.2
034900*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          NC2314.2
035000*        MOVE SPACE TO DUMMY-RECORD                               NC2314.2
035100*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  NC2314.2
035200*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             NC2314.2
035300*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     NC2314.2
035400*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          NC2314.2
035500*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          NC2314.2
035600*        MOVE ZERO TO RECORD-COUNT.                               NC2314.2
035700     PERFORM WRT-LN.                                              NC2314.2
035800 WRT-LN.                                                          NC2314.2
035900     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               NC2314.2
036000     MOVE SPACE TO DUMMY-RECORD.                                  NC2314.2
036100 BLANK-LINE-PRINT.                                                NC2314.2
036200     PERFORM WRT-LN.                                              NC2314.2
036300 FAIL-ROUTINE.                                                    NC2314.2
036400     IF   COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE. NC2314.2
036500     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.NC2314.2
036600     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC2314.2
036700     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   NC2314.2
036800     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2314.2
036900     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC2314.2
037000     GO TO  FAIL-ROUTINE-EX.                                      NC2314.2
037100 FAIL-ROUTINE-WRITE.                                              NC2314.2
037200     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         NC2314.2
037300     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 NC2314.2
037400     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. NC2314.2
037500     MOVE   SPACES TO COR-ANSI-REFERENCE.                         NC2314.2
037600 FAIL-ROUTINE-EX. EXIT.                                           NC2314.2
037700 BAIL-OUT.                                                        NC2314.2
037800     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   NC2314.2
037900     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           NC2314.2
038000 BAIL-OUT-WRITE.                                                  NC2314.2
038100     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  NC2314.2
038200     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC2314.2
038300     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2314.2
038400     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC2314.2
038500 BAIL-OUT-EX. EXIT.                                               NC2314.2
038600 CCVS1-EXIT.                                                      NC2314.2
038700     EXIT.                                                        NC2314.2
038800 SECT-NC231A-001 SECTION.                                         NC2314.2
038900 TH-01-001.                                                       NC2314.2
039000     MOVE   "VI-2 1.3.4" TO ANSI-REFERENCE.                       NC2314.2
039100     PERFORM PARA-1 VARYING SUB-1 FROM 1 BY 1                     NC2314.2
039200         UNTIL SUB-1 EQUAL TO 11                                  NC2314.2
039300         AFTER SUB-2 FROM 1 BY 1 UNTIL SUB-2 EQUAL TO 11          NC2314.2
039400         AFTER SUB-3 FROM 1 BY 1 UNTIL SUB-3 EQUAL TO 11          NC2314.2
039500     GO TO CHECK-ENTRIES.                                         NC2314.2
039600                                                                  NC2314.2
039700 PARA-1.                                                          NC2314.2
039800     SET IDX-1 TO SUB-1.                                          NC2314.2
039900     SET IDX-2 TO SUB-2.                                          NC2314.2
040000     SET IDX-3 TO SUB-3.                                          NC2314.2
040100     SET ADD-GRP, SEC-GRP, ELEM-GRP TO IDX-1.                     NC2314.2
040200     MOVE GRP-NAME TO ENTRY-1 (IDX-1).                            NC2314.2
040300     SET ADD-SEC, ELEM-SEC TO IDX-2.                              NC2314.2
040400     MOVE SEC-NAME TO ENTRY-2 (IDX-1, IDX-2).                     NC2314.2
040500     SET ADD-ELEM TO IDX-3.                                       NC2314.2
040600     MOVE ELEM-NAME TO ENTRY-3 (IDX-1, IDX-2, IDX-3).             NC2314.2
040700                                                                  NC2314.2
040800 CHECK-ENTRIES.                                                   NC2314.2
040900     MOVE "SEARCH VARYING LEV 1" TO FEATURE.                      NC2314.2
041000     MOVE "CHECK-ENTRIES       " TO PAR-NAME.                     NC2314.2
041100     MOVE "GRP02" TO GRP-HOLD-AREA.                               NC2314.2
041200     MOVE 02 TO SUB-2.                                            NC2314.2
041300     MOVE 01 TO CON-5.                                            NC2314.2
041400     SET IDX-1 TO 01.                                             NC2314.2
041500     SEARCH GRP-ENTRY VARYING CON-5 AT END                        NC2314.2
041600         PERFORM GRP-FAIL-PARGRAPH                                NC2314.2
041700         GO TO LEVEL-1-TEST-2                                     NC2314.2
041800         WHEN ENTRY-1 (IDX-1) = GRP-HOLD-AREA NEXT SENTENCE.      NC2314.2
041900                                                                  NC2314.2
042000     PERFORM PASS-TH.                                             NC2314.2
042100     GO TO LEVEL-1-TEST-2.                                        NC2314.2
042200                                                                  NC2314.2
042300 GRP-FAIL-PARGRAPH.                                               NC2314.2
042400     MOVE GRP-HOLD-AREA TO CORRECT-A.                             NC2314.2
042500     IF ENTRY-1 (SUB-2) NOT EQUAL TO GRP-HOLD-AREA                NC2314.2
042600         MOVE ENTRY-1 (SUB-2) TO COMPUTED-A                       NC2314.2
042700         MOVE "SEE NOTE 2 FOR DIAGNOSTIC  " TO RE-MARK  ELSE      NC2314.2
042800     MOVE "IDX-1" TO END-IDX                                      NC2314.2
042900     SET IDX-VALU TO IDX-1                                        NC2314.2
043000     MOVE "SEE NOTE 1 FOR DIAGNOSTIC  " TO RE-MARK                NC2314.2
043100     MOVE END-STMT TO COMPUTED-A.                                 NC2314.2
043200                                                                  NC2314.2
043300     PERFORM FAIL-TH.                                             NC2314.2
043400 LEVEL-1-TEST-2.                                                  NC2314.2
043500     MOVE "LEVEL-1-TEST-2      " TO PAR-NAME.                     NC2314.2
043600     MOVE "GRP01" TO GRP-HOLD-AREA.                               NC2314.2
043700     MOVE 01 TO SUB-2.                                            NC2314.2
043800     MOVE 01 TO CON-5.                                            NC2314.2
043900     SET IDX-1 TO 01.                                             NC2314.2
044000     SEARCH GRP-ENTRY VARYING CON-5 AT END                        NC2314.2
044100         PERFORM GRP-FAIL-PARGRAPH                                NC2314.2
044200         GO TO LEVEL-1-TEST-3                                     NC2314.2
044300         WHEN ENTRY-1 (IDX-1) = GRP-HOLD-AREA NEXT SENTENCE.      NC2314.2
044400                                                                  NC2314.2
044500     PERFORM PASS-TH.                                             NC2314.2
044600 LEVEL-1-TEST-3.                                                  NC2314.2
044700     MOVE "LEVEL-1-TEST-3      " TO PAR-NAME.                     NC2314.2
044800     MOVE "GRP10" TO GRP-HOLD-AREA.                               NC2314.2
044900     MOVE 10 TO SUB-2.                                            NC2314.2
045000     MOVE 01 TO CON-5.                                            NC2314.2
045100     SET IDX-1 TO 01.                                             NC2314.2
045200     SEARCH GRP-ENTRY VARYING CON-5 AT END                        NC2314.2
045300         PERFORM GRP-FAIL-PARGRAPH                                NC2314.2
045400         GO TO LEVEL-1-TEST-4                                     NC2314.2
045500         WHEN ENTRY-1 (IDX-1) = GRP-HOLD-AREA NEXT SENTENCE.      NC2314.2
045600                                                                  NC2314.2
045700     PERFORM PASS-TH.                                             NC2314.2
045800 LEVEL-1-TEST-4.                                                  NC2314.2
045900     MOVE "LEVEL-1-TEST-4      " TO PAR-NAME.                     NC2314.2
046000     MOVE "GRP05" TO GRP-HOLD-AREA.                               NC2314.2
046100     MOVE 05 TO SUB-2.                                            NC2314.2
046200     MOVE 05 TO CON-5.                                            NC2314.2
046300     SET IDX-1 TO 05.                                             NC2314.2
046400     SEARCH GRP-ENTRY VARYING CON-5 WHEN ENTRY-1 (CON-5)          NC2314.2
046500         EQUAL TO GRP-HOLD-AREA GO TO PASS-TH-TEST-4.             NC2314.2
046600     PERFORM GRP-FAIL-PARGRAPH.                                   NC2314.2
046700     GO TO LEVEL-2-TEST-1.                                        NC2314.2
046800 PASS-TH-TEST-4.                                                  NC2314.2
046900                                                                  NC2314.2
047000     PERFORM PASS-TH.                                             NC2314.2
047100                                                                  NC2314.2
047200 LEVEL-2-TEST-1.                                                  NC2314.2
047300     MOVE "SEARCH VARYING LEV 2" TO FEATURE.                      NC2314.2
047400     MOVE "LEVEL-2-TEST-1      " TO PAR-NAME.                     NC2314.2
047500     MOVE "SEC (01,01)" TO SEC-HOLD-AREA.                         NC2314.2
047600     MOVE  1 TO SUB-1  SUB-2.                                     NC2314.2
047700     SET IDX-1 IDX-2 TO 01.                                       NC2314.2
047800     MOVE 01 TO CON-6.                                            NC2314.2
047900     SEARCH GRP2-ENTRY VARYING CON-6 AT END                       NC2314.2
048000         PERFORM SEC-FAIL-PARGRAF                                 NC2314.2
048100         GO TO LEVEL-2-TEST-2                                     NC2314.2
048200         WHEN ENTRY-2 (IDX-1, IDX-2) = SEC-HOLD-AREA              NC2314.2
048300             NEXT SENTENCE.                                       NC2314.2
048400                                                                  NC2314.2
048500     PERFORM PASS-TH.                                             NC2314.2
048600                                                                  NC2314.2
048700 LEVEL-2-TEST-2.                                                  NC2314.2
048800     MOVE "LEVEL-2-TEST-2      " TO PAR-NAME.                     NC2314.2
048900     MOVE "SEC (05,10)" TO SEC-HOLD-AREA.                         NC2314.2
049000     MOVE 05 TO SUB-1.                                            NC2314.2
049100     MOVE 10 TO SUB-2.                                            NC2314.2
049200     SET IDX-1 TO 5.                                              NC2314.2
049300     MOVE 01 TO CON-6.                                            NC2314.2
049400     SET IDX-2 TO 01.                                             NC2314.2
049500     SEARCH GRP2-ENTRY VARYING CON-6 AT END                       NC2314.2
049600         PERFORM SEC-FAIL-PARGRAF                                 NC2314.2
049700         GO TO LEVEL-2-TEST-3                                     NC2314.2
049800         WHEN ENTRY-2 (IDX-1, IDX-2) = SEC-HOLD-AREA              NC2314.2
049900             NEXT SENTENCE.                                       NC2314.2
050000                                                                  NC2314.2
050100     PERFORM PASS-TH.                                             NC2314.2
050200                                                                  NC2314.2
050300 LEVEL-2-TEST-3.                                                  NC2314.2
050400     MOVE "LEVEL-2-TEST-3      " TO PAR-NAME.                     NC2314.2
050500     MOVE "SEC (10,10)" TO SEC-HOLD-AREA.                         NC2314.2
050600     SET IDX-1 TO 10.                                             NC2314.2
050700     MOVE 01 TO CON-6.                                            NC2314.2
050800     SET IDX-2 TO 01.                                             NC2314.2
050900     MOVE 10 TO SUB-1  SUB-2.                                     NC2314.2
051000     SEARCH GRP2-ENTRY VARYING CON-6 AT END                       NC2314.2
051100         PERFORM SEC-FAIL-PARGRAF                                 NC2314.2
051200         GO TO LEVEL-2-TEST-4                                     NC2314.2
051300         WHEN ENTRY-2 (IDX-1, IDX-2) = SEC-HOLD-AREA              NC2314.2
051400             NEXT SENTENCE.                                       NC2314.2
051500                                                                  NC2314.2
051600     PERFORM PASS-TH.                                             NC2314.2
051700 LEVEL-2-TEST-4.                                                  NC2314.2
051800     MOVE "LEVEL-2-TEST-4      " TO PAR-NAME.                     NC2314.2
051900     MOVE "SEC (08,02)" TO SEC-HOLD-AREA.                         NC2314.2
052000     MOVE 08 TO SUB-1.                                            NC2314.2
052100     MOVE 02 TO SUB-2.                                            NC2314.2
052200     SET IDX-1 TO 08.                                             NC2314.2
052300     MOVE 01 TO CON-6.                                            NC2314.2
052400     SET IDX-2 TO 01.                                             NC2314.2
052500     SEARCH GRP2-ENTRY VARYING CON-6                              NC2314.2
052600         WHEN ENTRY-2 (IDX-1, IDX-2) = SEC-HOLD-AREA              NC2314.2
052700             GO TO PASS-TH-2-4.                                   NC2314.2
052800     PERFORM SEC-FAIL-PARGRAF.                                    NC2314.2
052900     GO TO LEVEL-3-TEST-1.                                        NC2314.2
053000 PASS-TH-2-4.                                                     NC2314.2
053100                                                                  NC2314.2
053200     PERFORM PASS-TH.                                             NC2314.2
053300     GO TO LEVEL-3-TEST-1.                                        NC2314.2
053400                                                                  NC2314.2
053500 SEC-FAIL-PARGRAF.                                                NC2314.2
053600     MOVE SEC-HOLD-AREA TO CORRECT-A.                             NC2314.2
053700     IF ENTRY-2 (SUB-1, SUB-2) = SEC-HOLD-AREA                    NC2314.2
053800         MOVE "IDX-2" TO END-IDX                                  NC2314.2
053900         SET IDX-VALU TO IDX-2                                    NC2314.2
054000         MOVE "SEE NOTE 1 FOR DIAGNOSTIC  " TO RE-MARK            NC2314.2
054100         MOVE END-STMT TO COMPUTED-A  ELSE                        NC2314.2
054200     MOVE ENTRY-2 (SUB-1, SUB-2) TO COMPUTED-A                    NC2314.2
054300     MOVE "SEE NOTE 2 FOR DIAGNOSTIC  " TO RE-MARK.               NC2314.2
054400                                                                  NC2314.2
054500     PERFORM FAIL-TH.                                             NC2314.2
054600                                                                  NC2314.2
054700 LEVEL-3-TEST-1.                                                  NC2314.2
054800     MOVE "LEVEL-3-TEST-1      " TO PAR-NAME.                     NC2314.2
054900     MOVE "SEARCH VARYING LEV 3" TO FEATURE.                      NC2314.2
055000     MOVE 1 TO SUB-1  SUB-2  SUB-3.                               NC2314.2
055100     MOVE "ELEM (01,01,01)" TO ELEM-HOLD-AREA.                    NC2314.2
055200     SET IDX-1 IDX-2 IDX-3 TO 01.                                 NC2314.2
055300     MOVE 01 TO CON-7.                                            NC2314.2
055400     SEARCH GRP3-ENTRY VARYING CON-7                              NC2314.2
055500         WHEN ENTRY-3 (IDX-1, IDX-2, IDX-3) = ELEM-HOLD-AREA      NC2314.2
055600             GO TO PASS-TH-3-1.                                   NC2314.2
055700     PERFORM ELEM-FAIL-PARA.                                      NC2314.2
055800     GO TO LEVEL-3-TEST-2.                                        NC2314.2
055900 PASS-TH-3-1.                                                     NC2314.2
056000                                                                  NC2314.2
056100     PERFORM PASS-TH.                                             NC2314.2
056200                                                                  NC2314.2
056300 LEVEL-3-TEST-2.                                                  NC2314.2
056400     MOVE "LEVEL-3-TEST-2      " TO PAR-NAME.                     NC2314.2
056500     MOVE 05 TO SUB-1.                                            NC2314.2
056600     MOVE 06 TO SUB-2.                                            NC2314.2
056700     MOVE 07 TO SUB-3.                                            NC2314.2
056800     SET IDX-1 TO 05.                                             NC2314.2
056900     SET IDX-2 TO 06.                                             NC2314.2
057000     MOVE 01 TO CON-7.                                            NC2314.2
057100     SET IDX-3 TO 01.                                             NC2314.2
057200     MOVE "ELEM (05,06,07)" TO ELEM-HOLD-AREA.                    NC2314.2
057300     SEARCH GRP3-ENTRY VARYING CON-7 AT END                       NC2314.2
057400         PERFORM ELEM-FAIL-PARA                                   NC2314.2
057500         GO TO LEVEL-3-TEST-3                                     NC2314.2
057600         WHEN ENTRY-3 (IDX-1, IDX-2, IDX-3) = ELEM-HOLD-AREA      NC2314.2
057700             NEXT SENTENCE.                                       NC2314.2
057800                                                                  NC2314.2
057900     PERFORM PASS-TH.                                             NC2314.2
058000                                                                  NC2314.2
058100 LEVEL-3-TEST-3.                                                  NC2314.2
058200     MOVE "LEVEL-3-TEST-3      " TO PAR-NAME.                     NC2314.2
058300     MOVE 10 TO SUB-1 SUB-2 SUB-3.                                NC2314.2
058400     SET IDX-1  IDX-2 TO 10.                                      NC2314.2
058500     SET IDX-3 TO 01.                                             NC2314.2
058600     MOVE 01 TO CON-7.                                            NC2314.2
058700     MOVE "ELEM (10,10,10)" TO ELEM-HOLD-AREA.                    NC2314.2
058800     SEARCH GRP3-ENTRY VARYING CON-7 AT END                       NC2314.2
058900         PERFORM ELEM-FAIL-PARA                                   NC2314.2
059000         GO TO LEVEL-3-TEST-4                                     NC2314.2
059100         WHEN ENTRY-3 (IDX-1, IDX-2, IDX-3) = ELEM-HOLD-AREA      NC2314.2
059200             NEXT SENTENCE.                                       NC2314.2
059300                                                                  NC2314.2
059400     PERFORM PASS-TH.                                             NC2314.2
059500 LEVEL-3-TEST-4.                                                  NC2314.2
059600     MOVE "LEVEL-3-TEST-4      " TO PAR-NAME.                     NC2314.2
059700     MOVE "ELEM (07,06,05)" TO ELEM-HOLD-AREA.                    NC2314.2
059800     MOVE 07 TO SUB-1.                                            NC2314.2
059900     MOVE 06 TO SUB-2.                                            NC2314.2
060000     MOVE 05 TO SUB-3.                                            NC2314.2
060100     SET IDX-1 TO 07.                                             NC2314.2
060200     SET IDX-2 TO 06.                                             NC2314.2
060300     SET IDX-3 TO 03.                                             NC2314.2
060400     MOVE 03 TO CON-7.                                            NC2314.2
060500     SEARCH GRP3-ENTRY VARYING CON-7 AT END                       NC2314.2
060600         PERFORM ELEM-FAIL-PARA                                   NC2314.2
060700         GO TO MULT-SEARCH-TEST-1                                 NC2314.2
060800         WHEN ENTRY-3 (IDX-1, IDX-2, IDX-3) = ELEM-HOLD-AREA      NC2314.2
060900         NEXT SENTENCE.                                           NC2314.2
061000                                                                  NC2314.2
061100     PERFORM PASS-TH.                                             NC2314.2
061200     GO TO MULT-SEARCH-TEST-1.                                    NC2314.2
061300 ELEM-FAIL-PARA.                                                  NC2314.2
061400     MOVE ELEM-HOLD-AREA TO CORRECT-A.                            NC2314.2
061500     IF ENTRY-3 (SUB-1, SUB-2, SUB-3) = ELEM-HOLD-AREA            NC2314.2
061600         MOVE "IDX-3" TO END-IDX                                  NC2314.2
061700         SET IDX-VALU TO IDX-3                                    NC2314.2
061800         MOVE "SEE NOTE 1 FOR DIAGNOSTIC  " TO RE-MARK            NC2314.2
061900         MOVE END-STMT TO COMPUTED-A  ELSE                        NC2314.2
062000     MOVE ENTRY-3 (SUB-1, SUB-2, SUB-3) TO COMPUTED-A             NC2314.2
062100     MOVE "SEE NOTE 2 FOR DIAGNOSTIC  " TO RE-MARK.               NC2314.2
062200                                                                  NC2314.2
062300     PERFORM FAIL-TH.                                             NC2314.2
062400                                                                  NC2314.2
062500 MULT-SEARCH-TEST-1.                                              NC2314.2
062600     MOVE "MULT-SEARCH-TEST-1  " TO PAR-NAME.                     NC2314.2
062700     MOVE "MULTIPLE SEARCH STMT" TO FEATURE.                      NC2314.2
062800     MOVE "GRP08" TO GRP-HOLD-AREA.                               NC2314.2
062900     MOVE "SEC (08,07)" TO SEC-HOLD-AREA.                         NC2314.2
063000     MOVE 01 TO CON-5 CON-6.                                      NC2314.2
063100     SET IDX-1 IDX-2 TO 01.                                       NC2314.2
063200     SEARCH GRP-ENTRY VARYING CON-5 AT END GO TO MULT-SEARCH-FAIL1NC2314.2
063300         WHEN ENTRY-1 (CON-5) = "GRP08" NEXT SENTENCE.            NC2314.2
063400     SEARCH GRP2-ENTRY VARYING CON-6 AT END GO TO MULT-SEARCH-FAILNC2314.2
063500         WHEN ENTRY-2 (CON-5, CON-6) = SEC-HOLD-AREA              NC2314.2
063600             NEXT SENTENCE.                                       NC2314.2
063700                                                                  NC2314.2
063800     PERFORM PASS-TH.                                             NC2314.2
063900     GO TO MULT-SEARCH-TEST-2.                                    NC2314.2
064000 MULT-SEARCH-FAIL1.                                               NC2314.2
064100     MOVE GRP-HOLD-AREA TO CORRECT-A.                             NC2314.2
064200     IF ENTRY-1 (08) = GRP-HOLD-AREA                              NC2314.2
064300         MOVE "IDX-1" TO END-IDX                                  NC2314.2
064400         SET IDX-VALU TO IDX-1                                    NC2314.2
064500         MOVE "SEE NOTE 1 FOR DIAGNOSTIC  " TO RE-MARK            NC2314.2
064600         MOVE END-STMT TO COMPUTED-A  ELSE                        NC2314.2
064700     MOVE ENTRY-1 (08) TO COMPUTED-A                              NC2314.2
064800     MOVE "SEE NOTE 2 FOR DIAGNOSTIC  " TO RE-MARK.               NC2314.2
064900                                                                  NC2314.2
065000     PERFORM FAIL-TH.                                             NC2314.2
065100     GO TO MULT-SEARCH-TEST-2.                                    NC2314.2
065200 MULT-SEARCH-FAIL.                                                NC2314.2
065300     MOVE SEC-HOLD-AREA TO CORRECT-A.                             NC2314.2
065400     IF ENTRY-2 (08, 07) = SEC-HOLD-AREA                          NC2314.2
065500         MOVE "IDX-2" TO END-IDX                                  NC2314.2
065600         SET IDX-VALU TO IDX-2                                    NC2314.2
065700         MOVE "SEE NOTE 1 FOR DIAGNOSTIC  " TO RE-MARK            NC2314.2
065800         MOVE END-STMT TO COMPUTED-A  ELSE                        NC2314.2
065900     MOVE ENTRY-2 (08, 07) TO COMPUTED-A                          NC2314.2
066000     MOVE "SEE NOTE 2 FOR DIAGNOSTIC  " TO RE-MARK.               NC2314.2
066100                                                                  NC2314.2
066200     PERFORM FAIL-TH.                                             NC2314.2
066300                                                                  NC2314.2
066400 MULT-SEARCH-TEST-2.                                              NC2314.2
066500     MOVE "MULT-SEARCH-TEST-2  " TO PAR-NAME.                     NC2314.2
066600     MOVE "GRP04" TO GRP-HOLD-AREA.                               NC2314.2
066700     MOVE "SEC (04,04)" TO SEC-HOLD-AREA.                         NC2314.2
066800     MOVE "ELEM (04,04,04)" TO ELEM-HOLD-AREA.                    NC2314.2
066900     MOVE 01 TO CON-5 CON-6 CON-7.                                NC2314.2
067000     SET IDX-1  IDX-2  IDX-3 TO 01.                               NC2314.2
067100     SEARCH GRP-ENTRY VARYING CON-5 AT END                        NC2314.2
067200         GO TO MULT-SEARCH-2-FAIL WHEN ENTRY-1 (CON-5) =          NC2314.2
067300         GRP-HOLD-AREA  NEXT SENTENCE.                            NC2314.2
067400     SEARCH GRP2-ENTRY VARYING CON-6 AT END                       NC2314.2
067500         GO TO MULT-SEARCH-3-FAIL WHEN ENTRY-2 (CON-5, CON-6) =   NC2314.2
067600         SEC-HOLD-AREA  NEXT SENTENCE.                            NC2314.2
067700     SEARCH GRP3-ENTRY VARYING CON-7 AT END                       NC2314.2
067800         GO TO MULT-SEARCH-4-FAIL WHEN ENTRY-3                    NC2314.2
067900             (CON-5, CON-6, CON-7) = ELEM-HOLD-AREA NEXT SENTENCE.NC2314.2
068000                                                                  NC2314.2
068100     PERFORM PASS-TH.                                             NC2314.2
068200     GO TO   MULT-SEARCH-7-INIT-3.                                NC2314.2
068300                                                                  NC2314.2
068400 MULT-SEARCH-2-FAIL.                                              NC2314.2
068500     MOVE GRP-HOLD-AREA TO CORRECT-A.                             NC2314.2
068600     IF ENTRY-1 (04) = GRP-HOLD-AREA                              NC2314.2
068700         MOVE "IDX-1" TO END-IDX                                  NC2314.2
068800         SET IDX-VALU TO IDX-1                                    NC2314.2
068900         MOVE "SEE NOTE 1 FOR DIAGNOSTIC  " TO RE-MARK            NC2314.2
069000         MOVE END-STMT TO COMPUTED-A  ELSE                        NC2314.2
069100     MOVE ENTRY-1 (04) TO COMPUTED-A                              NC2314.2
069200     MOVE "SEE NOTE 2 FOR DIAGNOSTIC  " TO RE-MARK.               NC2314.2
069300                                                                  NC2314.2
069400     PERFORM FAIL-TH.                                             NC2314.2
069500     GO TO   MULT-SEARCH-7-INIT-3.                                NC2314.2
069600                                                                  NC2314.2
069700 MULT-SEARCH-3-FAIL.                                              NC2314.2
069800     MOVE  SEC-HOLD-AREA TO CORRECT-A.                            NC2314.2
069900     IF ENTRY-2 (04, 04) = SEC-HOLD-AREA                          NC2314.2
070000         MOVE "IDX-2" TO END-IDX                                  NC2314.2
070100         MOVE "SEE NOTE 1 FOR DIAGNOSTIC  " TO RE-MARK            NC2314.2
070200         SET IDX-VALU TO IDX-2                                    NC2314.2
070300         MOVE END-STMT TO COMPUTED-A  ELSE                        NC2314.2
070400     MOVE ENTRY-2 (04, 04) TO COMPUTED-A                          NC2314.2
070500     MOVE "SEE NOTE 2 FOR DIAGNOSTIC  " TO RE-MARK.               NC2314.2
070600                                                                  NC2314.2
070700     PERFORM FAIL-TH.                                             NC2314.2
070800     GO TO   MULT-SEARCH-7-INIT-3.                                NC2314.2
070900                                                                  NC2314.2
071000 MULT-SEARCH-4-FAIL.                                              NC2314.2
071100     MOVE ELEM-HOLD-AREA TO CORRECT-A.                            NC2314.2
071200     IF ENTRY-3 (04, 04, 04) = ELEM-HOLD-AREA                     NC2314.2
071300         MOVE "IDX-3" TO END-IDX                                  NC2314.2
071400         MOVE "SEE NOTE 1 FOR DIAGNOSTIC  " TO RE-MARK            NC2314.2
071500         SET IDX-VALU TO IDX-3                                    NC2314.2
071600         MOVE END-STMT TO COMPUTED-A  ELSE                        NC2314.2
071700     MOVE ENTRY-3 (04, 04, 04) TO COMPUTED-A                      NC2314.2
071800     MOVE "SEE NOTE 2 FOR DIAGNOSTIC  " TO RE-MARK.               NC2314.2
071900                                                                  NC2314.2
072000     PERFORM FAIL-TH.                                             NC2314.2
072100                                                                  NC2314.2
072200 MULT-SEARCH-7-INIT-3.                                            NC2314.2
072300     MOVE   "MULT-SEARCH-7-TEST-3" TO PAR-NAME.                   NC2314.2
072400     MOVE   "VI-122 6.21"          TO ANSI-REFERENCE.             NC2314.2
072500     MOVE    ALL "ABCDEFGHIJKLMNOPQRSTUVWXYZ" TO 7-DIMENSION-TBL. NC2314.2
072600     MOVE   "UV" TO L1-HOLD.                                      NC2314.2
072700     MOVE   "WX" TO L2-HOLD.                                      NC2314.2
072800     MOVE   "IJ" TO L3-HOLD.                                      NC2314.2
072900     MOVE   "KL" TO L4-HOLD.                                      NC2314.2
073000     MOVE   "AB" TO L5-HOLD.                                      NC2314.2
073100     MOVE   "CD" TO L6-HOLD.                                      NC2314.2
073200     MOVE   "GH" TO L7-HOLD.                                      NC2314.2
073300     SET     IX-1 IX-2 IX-3 IX-4 IX-5 IX-6 IX-7 TO 1.             NC2314.2
073400     MOVE    1 TO N1 N2 N3 N4 N5 N6 N7.                           NC2314.2
073500     GO TO   MULT-SEARCH-7-TEST-3.                                NC2314.2
073600 MULT-SEARCH-7-DELETE-3.                                          NC2314.2
073700     PERFORM DE-LETE.                                             NC2314.2
073800     PERFORM PRINT-DETAIL.                                        NC2314.2
073900     GO TO   SPECIAL-TEST-1.                                      NC2314.2
074000 MULT-SEARCH-7-TEST-3.                                            NC2314.2
074100     SEARCH  GRP-7-1-ENTRY VARYING N1                             NC2314.2
074200             AT END  GO TO MULT-SEARCH-7-FAIL-1                   NC2314.2
074300             WHEN    ENTRY-7-1 (N1) =  L1-HOLD                    NC2314.2
074400                     NEXT SENTENCE.                               NC2314.2
074500     SEARCH  GRP-7-2-ENTRY VARYING N2                             NC2314.2
074600             AT END  GO TO MULT-SEARCH-7-FAIL-2                   NC2314.2
074700             WHEN    ENTRY-7-2 (N1 N2) = L2-HOLD                  NC2314.2
074800                     NEXT SENTENCE.                               NC2314.2
074900     SEARCH  GRP-7-3-ENTRY VARYING N3                             NC2314.2
075000             AT END  GO TO MULT-SEARCH-7-FAIL-3                   NC2314.2
075100             WHEN    ENTRY-7-3 (N1 N2 N3) = L3-HOLD               NC2314.2
075200                     NEXT SENTENCE.                               NC2314.2
075300     SEARCH  GRP-7-4-ENTRY VARYING N4                             NC2314.2
075400             AT END  GO TO MULT-SEARCH-7-FAIL-4                   NC2314.2
075500             WHEN    ENTRY-7-4 (N1 N2 N3 N4) =  L4-HOLD           NC2314.2
075600                     NEXT SENTENCE.                               NC2314.2
075700     SEARCH  GRP-7-5-ENTRY VARYING N5                             NC2314.2
075800             AT END  GO TO MULT-SEARCH-7-FAIL-5                   NC2314.2
075900             WHEN    ENTRY-7-5 (N1 N2 N3 N4 N5) = L5-HOLD         NC2314.2
076000                     NEXT SENTENCE.                               NC2314.2
076100     SEARCH  GRP-7-6-ENTRY VARYING N6                             NC2314.2
076200             AT END  GO TO MULT-SEARCH-7-FAIL-6                   NC2314.2
076300             WHEN    ENTRY-7-6 (N1 N2 N3 N4 N5 N6) = L6-HOLD      NC2314.2
076400                     NEXT SENTENCE.                               NC2314.2
076500     SEARCH  GRP-7-7-ENTRY VARYING N7                             NC2314.2
076600             AT END  GO TO MULT-SEARCH-7-FAIL-7                   NC2314.2
076700             WHEN    ENTRY-7-7 (N1 N2 N3 N4 N5 N6 N7) = L7-HOLD   NC2314.2
076800                     NEXT SENTENCE.                               NC2314.2
076900                                                                  NC2314.2
077000     PERFORM PASS-TH.                                             NC2314.2
077100     GO TO   SPECIAL-TEST-1.                                      NC2314.2
077200                                                                  NC2314.2
077300 MULT-SEARCH-7-FAIL-1.                                            NC2314.2
077400     MOVE    L1-HOLD TO CORRECT-A.                                NC2314.2
077500     IF      ENTRY-7-1 (2) = L1-HOLD                              NC2314.2
077600             MOVE   "IX-1" TO END-IDX                             NC2314.2
077700             SET     IDX-VALU TO IX-1                             NC2314.2
077800             MOVE   "SEE NOTE 1 FOR DIAGNOSTIC  " TO RE-MARK      NC2314.2
077900             MOVE    END-STMT TO COMPUTED-A                       NC2314.2
078000     ELSE                                                         NC2314.2
078100             MOVE    ENTRY-7-1 (2) TO COMPUTED-A                  NC2314.2
078200             MOVE   "SEE NOTE 2 FOR DIAGNOSTIC  " TO RE-MARK.     NC2314.2
078300                                                                  NC2314.2
078400     PERFORM FAIL-TH.                                             NC2314.2
078500     GO TO   SPECIAL-TEST-1.                                      NC2314.2
078600                                                                  NC2314.2
078700 MULT-SEARCH-7-FAIL-2.                                            NC2314.2
078800     MOVE    L2-HOLD TO CORRECT-A.                                NC2314.2
078900     IF      ENTRY-7-2 (2 1) = L1-HOLD                            NC2314.2
079000             MOVE   "IX-2" TO END-IDX                             NC2314.2
079100             SET     IDX-VALU TO IX-2                             NC2314.2
079200             MOVE   "SEE NOTE 1 FOR DIAGNOSTIC  " TO RE-MARK      NC2314.2
079300             MOVE    END-STMT TO COMPUTED-A                       NC2314.2
079400     ELSE                                                         NC2314.2
079500             MOVE    ENTRY-7-2 (2 1) TO COMPUTED-A                NC2314.2
079600             MOVE   "SEE NOTE 2 FOR DIAGNOSTIC  " TO RE-MARK.     NC2314.2
079700                                                                  NC2314.2
079800     PERFORM FAIL-TH.                                             NC2314.2
079900     GO TO   SPECIAL-TEST-1.                                      NC2314.2
080000                                                                  NC2314.2
080100 MULT-SEARCH-7-FAIL-3.                                            NC2314.2
080200     MOVE    L3-HOLD TO CORRECT-A.                                NC2314.2
080300     IF      ENTRY-7-3 (2 1 2) = L3-HOLD                          NC2314.2
080400             MOVE   "IX-3" TO END-IDX                             NC2314.2
080500             SET     IDX-VALU TO IX-3                             NC2314.2
080600             MOVE   "SEE NOTE 1 FOR DIAGNOSTIC  " TO RE-MARK      NC2314.2
080700             MOVE    END-STMT TO COMPUTED-A                       NC2314.2
080800     ELSE                                                         NC2314.2
080900             MOVE    ENTRY-7-3 (2 1 2) TO COMPUTED-A              NC2314.2
081000             MOVE   "SEE NOTE 2 FOR DIAGNOSTIC  " TO RE-MARK.     NC2314.2
081100                                                                  NC2314.2
081200     PERFORM FAIL-TH.                                             NC2314.2
081300     GO TO   SPECIAL-TEST-1.                                      NC2314.2
081400                                                                  NC2314.2
081500 MULT-SEARCH-7-FAIL-4.                                            NC2314.2
081600     MOVE    L4-HOLD TO CORRECT-A.                                NC2314.2
081700     IF      ENTRY-7-4 (2 1 2 1) = L4-HOLD                        NC2314.2
081800             MOVE   "IX-4" TO END-IDX                             NC2314.2
081900             SET     IDX-VALU TO IX-4                             NC2314.2
082000             MOVE   "SEE NOTE 1 FOR DIAGNOSTIC  " TO RE-MARK      NC2314.2
082100             MOVE    END-STMT TO COMPUTED-A                       NC2314.2
082200     ELSE                                                         NC2314.2
082300             MOVE    ENTRY-7-4 (2 1 2 1) TO COMPUTED-A            NC2314.2
082400             MOVE   "SEE NOTE 2 FOR DIAGNOSTIC  " TO RE-MARK.     NC2314.2
082500                                                                  NC2314.2
082600     PERFORM FAIL-TH.                                             NC2314.2
082700     GO TO   SPECIAL-TEST-1.                                      NC2314.2
082800                                                                  NC2314.2
082900 MULT-SEARCH-7-FAIL-5.                                            NC2314.2
083000     MOVE    L5-HOLD TO CORRECT-A.                                NC2314.2
083100     IF      ENTRY-7-5 (2 1 2 1 2) = L5-HOLD                      NC2314.2
083200             MOVE   "IX-5" TO END-IDX                             NC2314.2
083300             SET     IDX-VALU TO IX-5                             NC2314.2
083400             MOVE   "SEE NOTE 1 FOR DIAGNOSTIC  " TO RE-MARK      NC2314.2
083500             MOVE    END-STMT TO COMPUTED-A                       NC2314.2
083600     ELSE                                                         NC2314.2
083700             MOVE    ENTRY-7-5 (2 1 2 1 2) TO COMPUTED-A          NC2314.2
083800             MOVE   "SEE NOTE 2 FOR DIAGNOSTIC  " TO RE-MARK.     NC2314.2
083900                                                                  NC2314.2
084000     PERFORM FAIL-TH.                                             NC2314.2
084100     GO TO   SPECIAL-TEST-1.                                      NC2314.2
084200                                                                  NC2314.2
084300 MULT-SEARCH-7-FAIL-6.                                            NC2314.2
084400     MOVE    L6-HOLD TO CORRECT-A.                                NC2314.2
084500     IF      ENTRY-7-6 (2 1 2 1 2 1) = L6-HOLD                    NC2314.2
084600             MOVE   "IX-6" TO END-IDX                             NC2314.2
084700             SET     IDX-VALU TO IX-6                             NC2314.2
084800             MOVE   "SEE NOTE 1 FOR DIAGNOSTIC  " TO RE-MARK      NC2314.2
084900             MOVE    END-STMT TO COMPUTED-A                       NC2314.2
085000     ELSE                                                         NC2314.2
085100             MOVE    ENTRY-7-6 (2 1 2 1 2 1) TO COMPUTED-A        NC2314.2
085200             MOVE   "SEE NOTE 2 FOR DIAGNOSTIC  " TO RE-MARK.     NC2314.2
085300                                                                  NC2314.2
085400     PERFORM FAIL-TH.                                             NC2314.2
085500     GO TO   SPECIAL-TEST-1.                                      NC2314.2
085600                                                                  NC2314.2
085700 MULT-SEARCH-7-FAIL-7.                                            NC2314.2
085800     MOVE    L7-HOLD TO CORRECT-A.                                NC2314.2
085900     IF      ENTRY-7-7 (2 1 2 1 2 1 2) = L7-HOLD                  NC2314.2
086000             MOVE   "IX-7" TO END-IDX                             NC2314.2
086100             SET     IDX-VALU TO IX-7                             NC2314.2
086200             MOVE   "SEE NOTE 1 FOR DIAGNOSTIC  " TO RE-MARK      NC2314.2
086300             MOVE    END-STMT TO COMPUTED-A                       NC2314.2
086400     ELSE                                                         NC2314.2
086500             MOVE    ENTRY-7-7 (2 1 2 1 2 1 2) TO COMPUTED-A      NC2314.2
086600             MOVE   "SEE NOTE 2 FOR DIAGNOSTIC  " TO RE-MARK.     NC2314.2
086700                                                                  NC2314.2
086800     PERFORM FAIL-TH.                                             NC2314.2
086900                                                                  NC2314.2
087000 SPECIAL-TEST-1.                                                  NC2314.2
087100     MOVE "SPECIAL-TEST-1      " TO PAR-NAME.                     NC2314.2
087200     MOVE "IDX SET HI TO ENTRY " TO FEATURE.                      NC2314.2
087300     MOVE 04 TO CON-5.                                            NC2314.2
087400     SET IDX-1 TO 04.                                             NC2314.2
087500     SEARCH GRP-ENTRY VARYING CON-5 AT END                        NC2314.2
087600     GO TO SPEC-PASS-PARAGRAPH-1 WHEN ENTRY-1 (CON-5) = "GRP03"   NC2314.2
087700         GO TO SPEC-FAIL-PARAGRAPH-1.                             NC2314.2
087800 SPECIAL-2-LEVEL-SEARCH.                                          NC2314.2
087900     MOVE "SPECIAL-2-LEVEL-SEAR" TO PAR-NAME.                     NC2314.2
088000     MOVE 04 TO CON-5.                                            NC2314.2
088100     MOVE 05 TO CON-6.                                            NC2314.2
088200     SET IDX-1 TO 04.                                             NC2314.2
088300     SET IDX-2 TO 05.                                             NC2314.2
088400     SEARCH GRP-ENTRY VARYING IDX-1 AT END                        NC2314.2
088500         GO TO SPEC-FAIL-PARAGRAPH-2                              NC2314.2
088600         WHEN ENTRY-1 (CON-5) = "GRP04" NEXT SENTENCE.            NC2314.2
088700     SEARCH GRP2-ENTRY VARYING CON-6 AT END                       NC2314.2
088800         GO TO SPEC-PASS-PARAGRAPH-2                              NC2314.2
088900         WHEN ENTRY-2 (CON-5, CON-6) = "SEC (04,04)"              NC2314.2
089000         GO TO SPEC-FAIL-PARAGRAPH-3.                             NC2314.2
089100 SPEC-PASS-PARAGRAPH-1.                                           NC2314.2
089200                                                                  NC2314.2
089300     PERFORM PASS-TH.                                             NC2314.2
089400     GO TO SPECIAL-2-LEVEL-SEARCH.                                NC2314.2
089500                                                                  NC2314.2
089600 SPEC-FAIL-PARAGRAPH-1.                                           NC2314.2
089700     MOVE "ENTRY SHOULD NOT BE FOUND  " TO RE-MARK.               NC2314.2
089800     MOVE "GRP03" TO COMPUTED-A.                                  NC2314.2
089900                                                                  NC2314.2
090000     MOVE SPACES TO CORRECT-A.                                    NC2314.2
090100     PERFORM FAIL-TH.                                             NC2314.2
090200     GO TO SPECIAL-2-LEVEL-SEARCH.                                NC2314.2
090300                                                                  NC2314.2
090400 SPEC-FAIL-PARAGRAPH-2.                                           NC2314.2
090500     MOVE "GRP04" TO CORRECT-A.                                   NC2314.2
090600     MOVE ENTRY-1 (04) TO COMPUTED-A.                             NC2314.2
090700                                                                  NC2314.2
090800     MOVE "SEE NOTE 1 FOR DIAGNOSTIC  " TO RE-MARK.               NC2314.2
090900     PERFORM FAIL-TH.                                             NC2314.2
091000     GO TO SPECIAL-3-LEVEL-SEARCH.                                NC2314.2
091100                                                                  NC2314.2
091200 SPEC-FAIL-PARAGRAPH-3.                                           NC2314.2
091300     MOVE ENTRY-2 (04, 04) TO COMPUTED-A.                         NC2314.2
091400     MOVE SPACE TO CORRECT-A.                                     NC2314.2
091500     MOVE "ENTRY SHOULD NOT BE FOUND  " TO RE-MARK.               NC2314.2
091600                                                                  NC2314.2
091700     PERFORM FAIL-TH.                                             NC2314.2
091800     GO TO SPECIAL-3-LEVEL-SEARCH.                                NC2314.2
091900                                                                  NC2314.2
092000 SPEC-PASS-PARAGRAPH-2.                                           NC2314.2
092100                                                                  NC2314.2
092200     PERFORM PASS-TH.                                             NC2314.2
092300     GO TO SPECIAL-3-LEVEL-SEARCH.                                NC2314.2
092400                                                                  NC2314.2
092500 SPECIAL-3-LEVEL-SEARCH.                                          NC2314.2
092600     MOVE "SPECIAL-3-LEVEL-SEAR" TO PAR-NAME.                     NC2314.2
092700     SET IDX-1 TO 02.                                             NC2314.2
092800     MOVE 02 TO CON-5.                                            NC2314.2
092900     SEARCH GRP-ENTRY VARYING CON-5 AT END                        NC2314.2
093000         GO TO SPEC-FAIL-PARAGRAPH-4 WHEN ENTRY-1 (CON-5)         NC2314.2
093100         EQUAL TO "GRP02" NEXT SENTENCE.                          NC2314.2
093200     MOVE 01 TO CON-6.                                            NC2314.2
093300     SET IDX-2 TO 01.                                             NC2314.2
093400     SEARCH GRP2-ENTRY VARYING CON-6 AT END                       NC2314.2
093500         GO TO SPEC-FAIL-PARAGRAPH-5                              NC2314.2
093600     WHEN ENTRY-2 (CON-5, CON-6) = "SEC (02,03)" NEXT SENTENCE.   NC2314.2
093700     MOVE 05 TO CON-7.                                            NC2314.2
093800     SET IDX-3 TO 05.                                             NC2314.2
093900     SEARCH GRP3-ENTRY VARYING CON-7 AT END                       NC2314.2
094000         GO TO SPEC-PASS-PARAGRAPH-3                              NC2314.2
094100     WHEN ENTRY-3 (CON-5, CON-6, CON-7) = "ELEM (02,03,04)"       NC2314.2
094200                                                                  NC2314.2
094300     MOVE "INDEX SET HIGHER THAN ENTRY" TO RE-MARK                NC2314.2
094400         MOVE SPACES TO CORRECT-A                                 NC2314.2
094500         MOVE "ELEM (02,03,04)" TO COMPUTED-A                     NC2314.2
094600         PERFORM FAIL-TH                                          NC2314.2
094700         GO TO SEARCH-INIT-1.                                     NC2314.2
094800 SPEC-PASS-PARAGRAPH-3.                                           NC2314.2
094900                                                                  NC2314.2
095000     PERFORM PASS-TH.                                             NC2314.2
095100     GO TO SEARCH-INIT-1.                                         NC2314.2
095200                                                                  NC2314.2
095300 SPEC-FAIL-PARAGRAPH-4.                                           NC2314.2
095400     IF ENTRY-1 (02) = "GRP02"                                    NC2314.2
095500         MOVE "IDX-1" TO END-IDX                                  NC2314.2
095600         MOVE "SEE NOTE 1 FOR DIAGNOSTIC  " TO RE-MARK            NC2314.2
095700         SET IDX-VALU TO IDX-1                                    NC2314.2
095800         MOVE END-STMT TO COMPUTED-A  ELSE                        NC2314.2
095900     MOVE ENTRY-1 (02) TO COMPUTED-A                              NC2314.2
096000     MOVE "SEE NOTE 2 FOR DIAGNOSTIC  " TO RE-MARK.               NC2314.2
096100                                                                  NC2314.2
096200     MOVE "GRP02" TO CORRECT-A.                                   NC2314.2
096300     PERFORM FAIL-TH.                                             NC2314.2
096400     GO TO SEARCH-INIT-1.                                         NC2314.2
096500 SPEC-FAIL-PARAGRAPH-5.                                           NC2314.2
096600     IF ENTRY-2 (02, 03) = "SEC (02,03)"                          NC2314.2
096700         MOVE "IDX-2" TO END-IDX                                  NC2314.2
096800         SET IDX-VALU TO IDX-2                                    NC2314.2
096900         MOVE "SEE NOTE 1 FOR DIAGNOSTIC  " TO RE-MARK            NC2314.2
097000         MOVE END-STMT TO COMPUTED-A  ELSE                        NC2314.2
097100     MOVE ENTRY-2 (02, 03) TO COMPUTED-A                          NC2314.2
097200     MOVE "SEE NOTE 2 FOR DIAGNOSTIC  " TO RE-MARK.               NC2314.2
097300                                                                  NC2314.2
097400     MOVE "SEC (02, 03)" TO CORRECT-A.                            NC2314.2
097500     PERFORM FAIL-TH.                                             NC2314.2
097600                                                                  NC2314.2
097700                                                                  NC2314.2
097800 SEARCH-INIT-1.                                                   NC2314.2
097900     MOVE   "SEARCH-TEST-1" TO PAR-NAME.                          NC2314.2
098000     MOVE   "IV-41 6.4.3"   TO ANSI-REFERENCE.                    NC2314.2
098100     MOVE   "EXP.SCOPE TERMINATOR" TO FEATURE.                    NC2314.2
098200     MOVE   "CD"   TO L1-HOLD.                                    NC2314.2
098300     MOVE   "CD"   TO ENTRY-7-1 (2).                              NC2314.2
098400     MOVE    SPACE TO L2-HOLD.                                    NC2314.2
098500     MOVE    SPACE TO L3-HOLD.                                    NC2314.2
098600     MOVE    SPACE TO L4-HOLD.                                    NC2314.2
098700     MOVE    1 TO REC-CT.                                         NC2314.2
098800     MOVE    1 TO N1.                                             NC2314.2
098900     SET     IX-1 IX-2 IX-3 IX-4 IX-5 IX-6 IX-7 TO 1.             NC2314.2
099000     GO TO   SEARCH-TEST-1-0.                                     NC2314.2
099100 SEARCH-DELETE-1.                                                 NC2314.2
099200     PERFORM DE-LETE.                                             NC2314.2
099300     PERFORM PRINT-DETAIL.                                        NC2314.2
099400     GO TO   SEARCH-INIT-2.                                       NC2314.2
099500 SEARCH-TEST-1-0.                                                 NC2314.2
099600     SEARCH  GRP-7-1-ENTRY VARYING N1                             NC2314.2
099700             WHEN    ENTRY-7-1 (N1) =  L1-HOLD                    NC2314.2
099800                     MOVE   "AA" TO L2-HOLD                       NC2314.2
099900                     MOVE   "BB" TO L3-HOLD                       NC2314.2
100000     END-SEARCH                                                   NC2314.2
100100     MOVE   "CC" TO L4-HOLD.                                      NC2314.2
100200 SEARCH-TEST-1-1.                                                 NC2314.2
100300     MOVE   "SEARCH-TEST-1-1" TO PAR-NAME.                        NC2314.2
100400     IF      L2-HOLD = "AA"                                       NC2314.2
100500             PERFORM PASS                                         NC2314.2
100600             PERFORM PRINT-DETAIL                                 NC2314.2
100700     ELSE                                                         NC2314.2
100800             MOVE   "'WHEN' PHRASE SHOULD BE TRUE" TO RE-MARK     NC2314.2
100900             MOVE   "AA"  TO CORRECT-X                            NC2314.2
101000             MOVE    L2-HOLD TO COMPUTED-X                        NC2314.2
101100             PERFORM FAIL                                         NC2314.2
101200             PERFORM PRINT-DETAIL.                                NC2314.2
101300     ADD     1 TO REC-CT.                                         NC2314.2
101400 SEARCH-TEST-1-2.                                                 NC2314.2
101500     MOVE   "SEARCH-TEST-1-2" TO PAR-NAME.                        NC2314.2
101600     IF      L3-HOLD = "BB"                                       NC2314.2
101700             PERFORM PASS                                         NC2314.2
101800             PERFORM PRINT-DETAIL                                 NC2314.2
101900     ELSE                                                         NC2314.2
102000             MOVE   "'WHEN' PHRASE SHOULD BE TRUE" TO RE-MARK     NC2314.2
102100             MOVE   "BB"  TO CORRECT-X                            NC2314.2
102200             MOVE    L3-HOLD TO COMPUTED-X                        NC2314.2
102300             PERFORM FAIL                                         NC2314.2
102400             PERFORM PRINT-DETAIL.                                NC2314.2
102500     ADD     1 TO REC-CT.                                         NC2314.2
102600 SEARCH-TEST-1-3.                                                 NC2314.2
102700     MOVE   "SEARCH-TEST-1-3" TO PAR-NAME.                        NC2314.2
102800     IF      L4-HOLD = "CC"                                       NC2314.2
102900             PERFORM PASS                                         NC2314.2
103000             PERFORM PRINT-DETAIL                                 NC2314.2
103100     ELSE                                                         NC2314.2
103200             MOVE   "SCOPE TERMINATOR IGNORED" TO RE-MARK         NC2314.2
103300             MOVE   "CC"  TO CORRECT-X                            NC2314.2
103400             MOVE    L4-HOLD TO COMPUTED-X                        NC2314.2
103500             PERFORM FAIL                                         NC2314.2
103600             PERFORM PRINT-DETAIL.                                NC2314.2
103700                                                                  NC2314.2
103800                                                                  NC2314.2
103900 SEARCH-INIT-2.                                                   NC2314.2
104000     MOVE   "SEARCH-TEST-2" TO PAR-NAME.                          NC2314.2
104100     MOVE   "IV-41 6.4.3"   TO ANSI-REFERENCE.                    NC2314.2
104200     MOVE   "CD"   TO L1-HOLD.                                    NC2314.2
104300     MOVE   "ZZ"   TO ENTRY-7-1 (2).                              NC2314.2
104400     MOVE    SPACE TO L2-HOLD.                                    NC2314.2
104500     MOVE    SPACE TO L3-HOLD.                                    NC2314.2
104600     MOVE    SPACE TO L4-HOLD.                                    NC2314.2
104700     MOVE    1 TO REC-CT.                                         NC2314.2
104800     MOVE    1 TO N1.                                             NC2314.2
104900     SET     IX-1 IX-2 IX-3 IX-4 IX-5 IX-6 IX-7 TO 1.             NC2314.2
105000     GO TO   SEARCH-TEST-2-0.                                     NC2314.2
105100 SEARCH-DELETE-2.                                                 NC2314.2
105200     PERFORM DE-LETE.                                             NC2314.2
105300     PERFORM PRINT-DETAIL.                                        NC2314.2
105400     GO TO   END-SEARCH-TEST.                                     NC2314.2
105500 SEARCH-TEST-2-0.                                                 NC2314.2
105600     SEARCH  GRP-7-1-ENTRY VARYING N1                             NC2314.2
105700             WHEN    ENTRY-7-1 (N1) =  L1-HOLD                    NC2314.2
105800                     MOVE   "AA" TO L2-HOLD                       NC2314.2
105900                     MOVE   "BB" TO L3-HOLD                       NC2314.2
106000     END-SEARCH                                                   NC2314.2
106100     MOVE   "CC" TO L4-HOLD.                                      NC2314.2
106200 SEARCH-TEST-2-1.                                                 NC2314.2
106300     MOVE   "SEARCH-TEST-2-1" TO PAR-NAME.                        NC2314.2
106400     IF      L2-HOLD = SPACE                                      NC2314.2
106500             PERFORM PASS                                         NC2314.2
106600             PERFORM PRINT-DETAIL                                 NC2314.2
106700     ELSE                                                         NC2314.2
106800             MOVE   "'WHEN' PHRASE SHOULD BE FALSE" TO RE-MARK    NC2314.2
106900             MOVE    SPACE TO CORRECT-X                           NC2314.2
107000             MOVE    L2-HOLD TO COMPUTED-X                        NC2314.2
107100             PERFORM FAIL                                         NC2314.2
107200             PERFORM PRINT-DETAIL.                                NC2314.2
107300     ADD     1 TO REC-CT.                                         NC2314.2
107400 SEARCH-TEST-2-2.                                                 NC2314.2
107500     MOVE   "SEARCH-TEST-2-2" TO PAR-NAME.                        NC2314.2
107600     IF      L3-HOLD = SPACE                                      NC2314.2
107700             PERFORM PASS                                         NC2314.2
107800             PERFORM PRINT-DETAIL                                 NC2314.2
107900     ELSE                                                         NC2314.2
108000             MOVE   "'WHEN' PHRASE SHOULD BE FALSE" TO RE-MARK    NC2314.2
108100             MOVE    SPACE TO CORRECT-X                           NC2314.2
108200             MOVE    L3-HOLD TO COMPUTED-X                        NC2314.2
108300             PERFORM FAIL                                         NC2314.2
108400             PERFORM PRINT-DETAIL.                                NC2314.2
108500     ADD     1 TO REC-CT.                                         NC2314.2
108600 SEARCH-TEST-2-3.                                                 NC2314.2
108700     MOVE   "SEARCH-TEST-2-3" TO PAR-NAME.                        NC2314.2
108800     IF      L4-HOLD = "CC"                                       NC2314.2
108900             PERFORM PASS                                         NC2314.2
109000             PERFORM PRINT-DETAIL                                 NC2314.2
109100     ELSE                                                         NC2314.2
109200             MOVE   "SCOPE TERMINATOR IGNORED" TO RE-MARK         NC2314.2
109300             MOVE   "CC"  TO CORRECT-X                            NC2314.2
109400             MOVE    L4-HOLD TO COMPUTED-X                        NC2314.2
109500             PERFORM FAIL                                         NC2314.2
109600             PERFORM PRINT-DETAIL.                                NC2314.2
109700                                                                  NC2314.2
109800     GO TO END-SEARCH-TEST.                                       NC2314.2
109900                                                                  NC2314.2
110000 PASS-TH.                                                         NC2314.2
110100     PERFORM PASS.                                                NC2314.2
110200     PERFORM PRINT-DETAIL.                                        NC2314.2
110300 FAIL-TH.                                                         NC2314.2
110400     PERFORM FAIL.                                                NC2314.2
110500     PERFORM  PRINT-DETAIL.                                       NC2314.2
110600 END-SEARCH-TEST.                                                 NC2314.2
110700     EXIT.                                                        NC2314.2
110800 CCVS-EXIT SECTION.                                               NC2314.2
110900 CCVS-999999.                                                     NC2314.2
111000     GO TO CLOSE-FILES.                                           NC2314.2
      *END-OF,NC231A                                                                  
