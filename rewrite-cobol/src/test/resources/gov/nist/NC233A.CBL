      *HEADER,COBOL,NC233A                                                            
000100 IDENTIFICATION DIVISION.                                         NC2334.2
000200 PROGRAM-ID.                                                      NC2334.2
000300     NC233A.                                                      NC2334.2
000400****************************************************************  NC2334.2
000500*                                                              *  NC2334.2
000600*    VALIDATION FOR:-                                          *  NC2334.2
000700*                                                              *  NC2334.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2334.2
000900*                                                              *  NC2334.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC2334.2
001100*                                                              *  NC2334.2
001200****************************************************************  NC2334.2
001300*                                                              *  NC2334.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  NC2334.2
001500*                                                              *  NC2334.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  NC2334.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  NC2334.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  NC2334.2
001900*                                                              *  NC2334.2
002000****************************************************************  NC2334.2
002100                                                                  NC2334.2
002200*                                                              *  NC2334.2
002300*    PROGRAM NC233A USES FORMAT 2 OF THE "SEARCH" STATEMENT    *  NC2334.2
002400*    TO ACCESS THRE AND SEVEN-DIMENSIONAL TABLES.              *  NC2334.2
002500*    THE SCOPE TERMINATOR "END-SEARCH" IS ALSO TESTED.         *  NC2334.2
002600*                                                              *  NC2334.2
002700****************************************************************  NC2334.2
002800 ENVIRONMENT DIVISION.                                            NC2334.2
002900 CONFIGURATION SECTION.                                           NC2334.2
003000 SOURCE-COMPUTER.                                                 NC2334.2
003100     XXXXX082.                                                    NC2334.2
003200 OBJECT-COMPUTER.                                                 NC2334.2
003300     XXXXX083.                                                    NC2334.2
003400 INPUT-OUTPUT SECTION.                                            NC2334.2
003500 FILE-CONTROL.                                                    NC2334.2
003600     SELECT PRINT-FILE ASSIGN TO                                  NC2334.2
003700     XXXXX055.                                                    NC2334.2
003800 DATA DIVISION.                                                   NC2334.2
003900 FILE SECTION.                                                    NC2334.2
004000 FD  PRINT-FILE.                                                  NC2334.2
004100 01  PRINT-REC PICTURE X(120).                                    NC2334.2
004200 01  DUMMY-RECORD PICTURE X(120).                                 NC2334.2
004300 WORKING-STORAGE SECTION.                                         NC2334.2
004400 77  SUB-1              PICTURE S99  VALUE ZERO.                  NC2334.2
004500 77  SUB-2              PICTURE 99   VALUE ZERO.                  NC2334.2
004600 77  SUB-3              PICTURE 99   VALUE ZERO.                  NC2334.2
004700 77  CON-7              PICTURE 99  VALUE 07.                     NC2334.2
004800 77  CON-10             PICTURE 99  VALUE 10.                     NC2334.2
004900 77  GRP-HOLD-AREA                PICTURE X(5)  VALUE SPACES.     NC2334.2
005000 77  CON-5              PICTURE 99  VALUE 05.                     NC2334.2
005100 77  SEC-HOLD-AREA                PICTURE X(11)  VALUE SPACES.    NC2334.2
005200 77  CON-6              PICTURE 99  VALUE 06.                     NC2334.2
005300 77  ELEM-HOLD-AREA               PICTURE X(15)  VALUE SPACES.    NC2334.2
005400 77  L1-HOLD                     PIC XX.                          NC2334.2
005500 77  L2-HOLD                     PIC XX.                          NC2334.2
005600 77  L3-HOLD                     PIC XX.                          NC2334.2
005700 77  L4-HOLD                     PIC XX.                          NC2334.2
005800 77  L5-HOLD                     PIC XX.                          NC2334.2
005900 77  L6-HOLD                     PIC XX.                          NC2334.2
006000 77  L7-HOLD                     PIC XX.                          NC2334.2
006100 77  N1                         PIC 99.                           NC2334.2
006200 77  N2                         PIC 99.                           NC2334.2
006300 77  N3                         PIC 99.                           NC2334.2
006400 77  N4                         PIC 99.                           NC2334.2
006500 77  N5                         PIC 99.                           NC2334.2
006600 77  N6                         PIC 99.                           NC2334.2
006700 77  N7                         PIC 99.                           NC2334.2
006800 01  GRP-NAME.                                                    NC2334.2
006900     02  FILLER              PICTURE XXX    VALUE "GRP".          NC2334.2
007000     02  ADD-GRP             PICTURE 99     VALUE 01.             NC2334.2
007100                                                                  NC2334.2
007200 01  SEC-NAME.                                                    NC2334.2
007300     02  FILLER              PICTURE X(5)   VALUE "SEC (".        NC2334.2
007400     02  SEC-GRP             PICTURE 99     VALUE 00.             NC2334.2
007500     02  FILLER              PICTURE X      VALUE ",".            NC2334.2
007600     02  ADD-SEC             PICTURE 99     VALUE 01.             NC2334.2
007700     02  FILLER              PICTURE X      VALUE ")".            NC2334.2
007800                                                                  NC2334.2
007900 01  ELEM-NAME.                                                   NC2334.2
008000     02  FILLER              PICTURE X(6)   VALUE "ELEM (".       NC2334.2
008100     02  ELEM-GRP            PICTURE 99     VALUE 00.             NC2334.2
008200     02  FILLER              PICTURE X      VALUE ",".            NC2334.2
008300     02  ELEM-SEC            PICTURE 99     VALUE 00.             NC2334.2
008400     02  FILLER              PICTURE X      VALUE ",".            NC2334.2
008500     02  ADD-ELEM            PICTURE 99     VALUE 01.             NC2334.2
008600     02  FILLER              PICTURE X      VALUE ")".            NC2334.2
008700                                                                  NC2334.2
008800 01  3-DIMENSION-TBL.                                             NC2334.2
008900     02  GRP-ENTRY OCCURS 10 TIMES ASCENDING KEY IS GRP           NC2334.2
009000         INDEXED BY IDX-1.                                        NC2334.2
009100         03  ENTRY-1.                                             NC2334.2
009200             05  GRP         PICTURE X(5).                        NC2334.2
009300         03  GRP2-ENTRY OCCURS 10 TIMES ASCENDING KEY IS SEC      NC2334.2
009400             INDEXED BY IDX-2.                                    NC2334.2
009500             04  ENTRY-2.                                         NC2334.2
009600                 05  FILLER  PICTURE X(4).                        NC2334.2
009700                 05  SEC     PICTURE X(7).                        NC2334.2
009800             04  GRP3-ENTRY OCCURS 10 TIMES ASCENDING KEY IS ELEM NC2334.2
009900                 INDEXED BY IDX-3.                                NC2334.2
010000                 05  ENTRY-3.                                     NC2334.2
010100                 07  FILLER  PICTURE X(8).                        NC2334.2
010200                 07  ELEM    PICTURE X(7).                        NC2334.2
010300                                                                  NC2334.2
010400                                                                  NC2334.2
010500 01  7-DIMENSION-TBL.                                             NC2334.2
010600   02  GRP-7-1-ENTRY             OCCURS 2                         NC2334.2
010700                                 ASCENDING KEY IS ENTRY-7-1G      NC2334.2
010800                                 INDEXED BY X1.                   NC2334.2
010900     03  ENTRY-7-1G.                                              NC2334.2
011000       04  CHARS-7-1               PIC X.                         NC2334.2
011100       04  ENTRY-7-1               PIC 9.                         NC2334.2
011200     03  GRP-7-2-ENTRY           OCCURS 2                         NC2334.2
011300                                 ASCENDING KEY IS ENTRY-7-2G      NC2334.2
011400                                 INDEXED BY X2.                   NC2334.2
011500       04  ENTRY-7-2G.                                            NC2334.2
011600         05  CHARS-7-2             PIC X.                         NC2334.2
011700         05  ENTRY-7-2             PIC 9.                         NC2334.2
011800       04  GRP-7-3-ENTRY         OCCURS 2                         NC2334.2
011900                                 ASCENDING KEY IS ENTRY-7-3G      NC2334.2
012000                                 INDEXED BY X3.                   NC2334.2
012100         05  ENTRY-7-3G.                                          NC2334.2
012200           06  CHARS-7-3           PIC X.                         NC2334.2
012300           06  ENTRY-7-3           PIC 9.                         NC2334.2
012400         05  GRP-7-4-ENTRY       OCCURS 2                         NC2334.2
012500                                 ASCENDING KEY IS ENTRY-7-4G      NC2334.2
012600                                 INDEXED BY X4.                   NC2334.2
012700           06  ENTRY-7-4G.                                        NC2334.2
012800             07  CHARS-7-4       PIC X.                           NC2334.2
012900             07  ENTRY-7-4       PIC 9.                           NC2334.2
013000           06  GRP-7-5-ENTRY     OCCURS 2                         NC2334.2
013100                                 ASCENDING KEY IS ENTRY-7-5G      NC2334.2
013200                                 INDEXED BY X5.                   NC2334.2
013300             07  ENTRY-7-5G.                                      NC2334.2
013400               08  CHARS-7-5     PIC X.                           NC2334.2
013500               08  ENTRY-7-5     PIC 9.                           NC2334.2
013600             07  GRP-7-6-ENTRY   OCCURS 2                         NC2334.2
013700                                 ASCENDING KEY IS ENTRY-7-6G      NC2334.2
013800                                 INDEXED BY X6.                   NC2334.2
013900               08  ENTRY-7-6G.                                    NC2334.2
014000                 09  CHARS-7-6   PIC X.                           NC2334.2
014100                 09  ENTRY-7-6   PIC 9.                           NC2334.2
014200               08  GRP-7-7-ENTRY OCCURS 2                         NC2334.2
014300                                 ASCENDING KEY IS ENTRY-7-7G      NC2334.2
014400                                 INDEXED BY X7.                   NC2334.2
014500                 09  ENTRY-7-7G.                                  NC2334.2
014600                   10  CHARS-7-7 PIC X.                           NC2334.2
014700                   10  ENTRY-7-7 PIC 9.                           NC2334.2
014800                                                                  NC2334.2
014900 01  NOTE-1.                                                      NC2334.2
015000     02  FILLER                   PICTURE X(74)  VALUE            NC2334.2
015100     "NOTE 1 - CORRECT AND COMPUTED DATA ARE EQUAL BUT THE AT END NC2334.2
015200-    "PATH WAS TAKEN".                                            NC2334.2
015300     02  FILLER                   PICTURE X(46)  VALUE SPACES.    NC2334.2
015400 01  NOTE-2.                                                      NC2334.2
015500     02  FILLER                   PICTURE X(112)  VALUE           NC2334.2
015600     "NOTE 2 - CORRECT AND COMPUTED DATA ARE NOT EQUAL. THE COMPUTNC2334.2
015700-    "ED ENTRY WAS EXTRACTED FROM THE TABLE BY SUBSCRIPTS.".      NC2334.2
015800     02  FILLER                   PICTURE X(8)  VALUE SPACES.     NC2334.2
015900                                                                  NC2334.2
016000 01  END-STMT.                                                    NC2334.2
016100     02  FILLER         PICTURE X(7)  VALUE "AT END ".            NC2334.2
016200     02  END-IDX                  PICTURE X(5)  VALUE SPACES.     NC2334.2
016300     02  FILLER                   PICTURE XXX  VALUE " = ".       NC2334.2
016400     02  IDX-VALU                 PICTURE 99  VALUE 00.           NC2334.2
016500     02  FILLER                   PICTURE XXX  VALUE SPACES.      NC2334.2
016600 01  TEST-RESULTS.                                                NC2334.2
016700     02 FILLER                   PIC X      VALUE SPACE.          NC2334.2
016800     02 FEATURE                  PIC X(20)  VALUE SPACE.          NC2334.2
016900     02 FILLER                   PIC X      VALUE SPACE.          NC2334.2
017000     02 P-OR-F                   PIC X(5)   VALUE SPACE.          NC2334.2
017100     02 FILLER                   PIC X      VALUE SPACE.          NC2334.2
017200     02  PAR-NAME.                                                NC2334.2
017300       03 FILLER                 PIC X(19)  VALUE SPACE.          NC2334.2
017400       03  PARDOT-X              PIC X      VALUE SPACE.          NC2334.2
017500       03 DOTVALUE               PIC 99     VALUE ZERO.           NC2334.2
017600     02 FILLER                   PIC X(8)   VALUE SPACE.          NC2334.2
017700     02 RE-MARK                  PIC X(61).                       NC2334.2
017800 01  TEST-COMPUTED.                                               NC2334.2
017900     02 FILLER                   PIC X(30)  VALUE SPACE.          NC2334.2
018000     02 FILLER                   PIC X(17)  VALUE                 NC2334.2
018100            "       COMPUTED=".                                   NC2334.2
018200     02 COMPUTED-X.                                               NC2334.2
018300     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          NC2334.2
018400     03 COMPUTED-N               REDEFINES COMPUTED-A             NC2334.2
018500                                 PIC -9(9).9(9).                  NC2334.2
018600     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         NC2334.2
018700     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     NC2334.2
018800     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     NC2334.2
018900     03       CM-18V0 REDEFINES COMPUTED-A.                       NC2334.2
019000         04 COMPUTED-18V0                    PIC -9(18).          NC2334.2
019100         04 FILLER                           PIC X.               NC2334.2
019200     03 FILLER PIC X(50) VALUE SPACE.                             NC2334.2
019300 01  TEST-CORRECT.                                                NC2334.2
019400     02 FILLER PIC X(30) VALUE SPACE.                             NC2334.2
019500     02 FILLER PIC X(17) VALUE "       CORRECT =".                NC2334.2
019600     02 CORRECT-X.                                                NC2334.2
019700     03 CORRECT-A                  PIC X(20) VALUE SPACE.         NC2334.2
019800     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      NC2334.2
019900     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         NC2334.2
020000     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     NC2334.2
020100     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     NC2334.2
020200     03      CR-18V0 REDEFINES CORRECT-A.                         NC2334.2
020300         04 CORRECT-18V0                     PIC -9(18).          NC2334.2
020400         04 FILLER                           PIC X.               NC2334.2
020500     03 FILLER PIC X(2) VALUE SPACE.                              NC2334.2
020600     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     NC2334.2
020700 01  CCVS-C-1.                                                    NC2334.2
020800     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PANC2334.2
020900-    "SS  PARAGRAPH-NAME                                          NC2334.2
021000-    "       REMARKS".                                            NC2334.2
021100     02 FILLER                     PIC X(20)    VALUE SPACE.      NC2334.2
021200 01  CCVS-C-2.                                                    NC2334.2
021300     02 FILLER                     PIC X        VALUE SPACE.      NC2334.2
021400     02 FILLER                     PIC X(6)     VALUE "TESTED".   NC2334.2
021500     02 FILLER                     PIC X(15)    VALUE SPACE.      NC2334.2
021600     02 FILLER                     PIC X(4)     VALUE "FAIL".     NC2334.2
021700     02 FILLER                     PIC X(94)    VALUE SPACE.      NC2334.2
021800 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       NC2334.2
021900 01  REC-CT                        PIC 99       VALUE ZERO.       NC2334.2
022000 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       NC2334.2
022100 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       NC2334.2
022200 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       NC2334.2
022300 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       NC2334.2
022400 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       NC2334.2
022500 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       NC2334.2
022600 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      NC2334.2
022700 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       NC2334.2
022800 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     NC2334.2
022900 01  CCVS-H-1.                                                    NC2334.2
023000     02  FILLER                    PIC X(39)    VALUE SPACES.     NC2334.2
023100     02  FILLER                    PIC X(42)    VALUE             NC2334.2
023200     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 NC2334.2
023300     02  FILLER                    PIC X(39)    VALUE SPACES.     NC2334.2
023400 01  CCVS-H-2A.                                                   NC2334.2
023500   02  FILLER                        PIC X(40)  VALUE SPACE.      NC2334.2
023600   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  NC2334.2
023700   02  FILLER                        PIC XXXX   VALUE             NC2334.2
023800     "4.2 ".                                                      NC2334.2
023900   02  FILLER                        PIC X(28)  VALUE             NC2334.2
024000            " COPY - NOT FOR DISTRIBUTION".                       NC2334.2
024100   02  FILLER                        PIC X(41)  VALUE SPACE.      NC2334.2
024200                                                                  NC2334.2
024300 01  CCVS-H-2B.                                                   NC2334.2
024400   02  FILLER                        PIC X(15)  VALUE             NC2334.2
024500            "TEST RESULT OF ".                                    NC2334.2
024600   02  TEST-ID                       PIC X(9).                    NC2334.2
024700   02  FILLER                        PIC X(4)   VALUE             NC2334.2
024800            " IN ".                                               NC2334.2
024900   02  FILLER                        PIC X(12)  VALUE             NC2334.2
025000     " HIGH       ".                                              NC2334.2
025100   02  FILLER                        PIC X(22)  VALUE             NC2334.2
025200            " LEVEL VALIDATION FOR ".                             NC2334.2
025300   02  FILLER                        PIC X(58)  VALUE             NC2334.2
025400     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2334.2
025500 01  CCVS-H-3.                                                    NC2334.2
025600     02  FILLER                      PIC X(34)  VALUE             NC2334.2
025700            " FOR OFFICIAL USE ONLY    ".                         NC2334.2
025800     02  FILLER                      PIC X(58)  VALUE             NC2334.2
025900     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC2334.2
026000     02  FILLER                      PIC X(28)  VALUE             NC2334.2
026100            "  COPYRIGHT   1985 ".                                NC2334.2
026200 01  CCVS-E-1.                                                    NC2334.2
026300     02 FILLER                       PIC X(52)  VALUE SPACE.      NC2334.2
026400     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              NC2334.2
026500     02 ID-AGAIN                     PIC X(9).                    NC2334.2
026600     02 FILLER                       PIC X(45)  VALUE SPACES.     NC2334.2
026700 01  CCVS-E-2.                                                    NC2334.2
026800     02  FILLER                      PIC X(31)  VALUE SPACE.      NC2334.2
026900     02  FILLER                      PIC X(21)  VALUE SPACE.      NC2334.2
027000     02 CCVS-E-2-2.                                               NC2334.2
027100         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      NC2334.2
027200         03 FILLER                   PIC X      VALUE SPACE.      NC2334.2
027300         03 ENDER-DESC               PIC X(44)  VALUE             NC2334.2
027400            "ERRORS ENCOUNTERED".                                 NC2334.2
027500 01  CCVS-E-3.                                                    NC2334.2
027600     02  FILLER                      PIC X(22)  VALUE             NC2334.2
027700            " FOR OFFICIAL USE ONLY".                             NC2334.2
027800     02  FILLER                      PIC X(12)  VALUE SPACE.      NC2334.2
027900     02  FILLER                      PIC X(58)  VALUE             NC2334.2
028000     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2334.2
028100     02  FILLER                      PIC X(13)  VALUE SPACE.      NC2334.2
028200     02 FILLER                       PIC X(15)  VALUE             NC2334.2
028300             " COPYRIGHT 1985".                                   NC2334.2
028400 01  CCVS-E-4.                                                    NC2334.2
028500     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      NC2334.2
028600     02 FILLER                       PIC X(4)   VALUE " OF ".     NC2334.2
028700     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      NC2334.2
028800     02 FILLER                       PIC X(40)  VALUE             NC2334.2
028900      "  TESTS WERE EXECUTED SUCCESSFULLY".                       NC2334.2
029000 01  XXINFO.                                                      NC2334.2
029100     02 FILLER                       PIC X(19)  VALUE             NC2334.2
029200            "*** INFORMATION ***".                                NC2334.2
029300     02 INFO-TEXT.                                                NC2334.2
029400       04 FILLER                     PIC X(8)   VALUE SPACE.      NC2334.2
029500       04 XXCOMPUTED                 PIC X(20).                   NC2334.2
029600       04 FILLER                     PIC X(5)   VALUE SPACE.      NC2334.2
029700       04 XXCORRECT                  PIC X(20).                   NC2334.2
029800     02 INF-ANSI-REFERENCE           PIC X(48).                   NC2334.2
029900 01  HYPHEN-LINE.                                                 NC2334.2
030000     02 FILLER  PIC IS X VALUE IS SPACE.                          NC2334.2
030100     02 FILLER  PIC IS X(65)    VALUE IS "************************NC2334.2
030200-    "*****************************************".                 NC2334.2
030300     02 FILLER  PIC IS X(54)    VALUE IS "************************NC2334.2
030400-    "******************************".                            NC2334.2
030500 01  CCVS-PGM-ID                     PIC X(9)   VALUE             NC2334.2
030600     "NC233A".                                                    NC2334.2
030700 PROCEDURE DIVISION.                                              NC2334.2
030800 CCVS1 SECTION.                                                   NC2334.2
030900 OPEN-FILES.                                                      NC2334.2
031000     OPEN     OUTPUT PRINT-FILE.                                  NC2334.2
031100     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   NC2334.2
031200     MOVE    SPACE TO TEST-RESULTS.                               NC2334.2
031300     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             NC2334.2
031400     GO TO CCVS1-EXIT.                                            NC2334.2
031500 CLOSE-FILES.                                                     NC2334.2
031600     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   NC2334.2
031700 TERMINATE-CCVS.                                                  NC2334.2
031800*    EXIT PROGRAM.                                                NC2334.2
031900*TERMINATE-CALL.                                                  NC2334.2
032000     STOP     RUN.                                                NC2334.2
032100 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         NC2334.2
032200 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           NC2334.2
032300 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          NC2334.2
032400 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      NC2334.2
032500     MOVE "****TEST DELETED****" TO RE-MARK.                      NC2334.2
032600 PRINT-DETAIL.                                                    NC2334.2
032700     IF REC-CT NOT EQUAL TO ZERO                                  NC2334.2
032800             MOVE "." TO PARDOT-X                                 NC2334.2
032900             MOVE REC-CT TO DOTVALUE.                             NC2334.2
033000     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      NC2334.2
033100     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               NC2334.2
033200        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 NC2334.2
033300          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 NC2334.2
033400     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              NC2334.2
033500     MOVE SPACE TO CORRECT-X.                                     NC2334.2
033600     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         NC2334.2
033700     MOVE     SPACE TO RE-MARK.                                   NC2334.2
033800 HEAD-ROUTINE.                                                    NC2334.2
033900     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC2334.2
034000     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC2334.2
034100     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC2334.2
034200     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC2334.2
034300 COLUMN-NAMES-ROUTINE.                                            NC2334.2
034400     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2334.2
034500     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2334.2
034600     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        NC2334.2
034700 END-ROUTINE.                                                     NC2334.2
034800     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.NC2334.2
034900 END-RTN-EXIT.                                                    NC2334.2
035000     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2334.2
035100 END-ROUTINE-1.                                                   NC2334.2
035200      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      NC2334.2
035300      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               NC2334.2
035400      ADD PASS-COUNTER TO ERROR-HOLD.                             NC2334.2
035500*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   NC2334.2
035600      MOVE PASS-COUNTER TO CCVS-E-4-1.                            NC2334.2
035700      MOVE ERROR-HOLD TO CCVS-E-4-2.                              NC2334.2
035800      MOVE CCVS-E-4 TO CCVS-E-2-2.                                NC2334.2
035900      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           NC2334.2
036000  END-ROUTINE-12.                                                 NC2334.2
036100      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        NC2334.2
036200     IF       ERROR-COUNTER IS EQUAL TO ZERO                      NC2334.2
036300         MOVE "NO " TO ERROR-TOTAL                                NC2334.2
036400         ELSE                                                     NC2334.2
036500         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       NC2334.2
036600     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           NC2334.2
036700     PERFORM WRITE-LINE.                                          NC2334.2
036800 END-ROUTINE-13.                                                  NC2334.2
036900     IF DELETE-COUNTER IS EQUAL TO ZERO                           NC2334.2
037000         MOVE "NO " TO ERROR-TOTAL  ELSE                          NC2334.2
037100         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      NC2334.2
037200     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   NC2334.2
037300     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2334.2
037400      IF   INSPECT-COUNTER EQUAL TO ZERO                          NC2334.2
037500          MOVE "NO " TO ERROR-TOTAL                               NC2334.2
037600      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   NC2334.2
037700      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            NC2334.2
037800      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          NC2334.2
037900     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2334.2
038000 WRITE-LINE.                                                      NC2334.2
038100     ADD 1 TO RECORD-COUNT.                                       NC2334.2
038200*    IF RECORD-COUNT GREATER 50                                   NC2334.2
038300*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          NC2334.2
038400*        MOVE SPACE TO DUMMY-RECORD                               NC2334.2
038500*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  NC2334.2
038600*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             NC2334.2
038700*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     NC2334.2
038800*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          NC2334.2
038900*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          NC2334.2
039000*        MOVE ZERO TO RECORD-COUNT.                               NC2334.2
039100     PERFORM WRT-LN.                                              NC2334.2
039200 WRT-LN.                                                          NC2334.2
039300     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               NC2334.2
039400     MOVE SPACE TO DUMMY-RECORD.                                  NC2334.2
039500 BLANK-LINE-PRINT.                                                NC2334.2
039600     PERFORM WRT-LN.                                              NC2334.2
039700 FAIL-ROUTINE.                                                    NC2334.2
039800     IF   COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE. NC2334.2
039900     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.NC2334.2
040000     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC2334.2
040100     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   NC2334.2
040200     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2334.2
040300     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC2334.2
040400     GO TO  FAIL-ROUTINE-EX.                                      NC2334.2
040500 FAIL-ROUTINE-WRITE.                                              NC2334.2
040600     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         NC2334.2
040700     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 NC2334.2
040800     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. NC2334.2
040900     MOVE   SPACES TO COR-ANSI-REFERENCE.                         NC2334.2
041000 FAIL-ROUTINE-EX. EXIT.                                           NC2334.2
041100 BAIL-OUT.                                                        NC2334.2
041200     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   NC2334.2
041300     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           NC2334.2
041400 BAIL-OUT-WRITE.                                                  NC2334.2
041500     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  NC2334.2
041600     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC2334.2
041700     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2334.2
041800     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC2334.2
041900 BAIL-OUT-EX. EXIT.                                               NC2334.2
042000 CCVS1-EXIT.                                                      NC2334.2
042100     EXIT.                                                        NC2334.2
042200 SECT-NC233A-001 SECTION.                                         NC2334.2
042300 TH-05-001.                                                       NC2334.2
042400 BUILD-LEVEL-1.                                                   NC2334.2
042500     ADD 1 TO SUB-1.                                              NC2334.2
042600     IF SUB-1 = 11 GO TO CHECK-ENTRIES.                           NC2334.2
042700     MOVE GRP-NAME TO ENTRY-1 (SUB-1).                            NC2334.2
042800     ADD 1 TO ADD-GRP.                                            NC2334.2
042900 BUILD-LEVEL-2.                                                   NC2334.2
043000     ADD 1 TO SUB-2.                                              NC2334.2
043100     IF SUB-2 = 11                                                NC2334.2
043200         MOVE ZERO TO SUB-2                                       NC2334.2
043300         MOVE 01 TO ADD-SEC                                       NC2334.2
043400         GO TO BUILD-LEVEL-1.                                     NC2334.2
043500     MOVE SUB-1 TO SEC-GRP.                                       NC2334.2
043600     MOVE SEC-NAME TO ENTRY-2 (SUB-1, SUB-2).                     NC2334.2
043700     ADD 1 TO ADD-SEC.                                            NC2334.2
043800 BUILD-LEVEL-3.                                                   NC2334.2
043900     ADD 1 TO SUB-3.                                              NC2334.2
044000     IF SUB-3 = 11                                                NC2334.2
044100         MOVE ZERO TO SUB-3                                       NC2334.2
044200              MOVE 01 TO ADD-ELEM                                 NC2334.2
044300              GO TO BUILD-LEVEL-2.                                NC2334.2
044400     MOVE SUB-1 TO ELEM-GRP.                                      NC2334.2
044500     MOVE SUB-2 TO ELEM-SEC.                                      NC2334.2
044600     MOVE ELEM-NAME TO ENTRY-3 (SUB-1, SUB-2, SUB-3).             NC2334.2
044700     ADD 1 TO ADD-ELEM.                                           NC2334.2
044800     GO TO BUILD-LEVEL-3.                                         NC2334.2
044900                                                                  NC2334.2
045000 CHECK-ENTRIES.                                                   NC2334.2
045100     MOVE "SEARCH ALL-FIRST LEV" TO FEATURE.                      NC2334.2
045200     MOVE "CHECK-ENTRIES       " TO PAR-NAME.                     NC2334.2
045300     MOVE "GRP02" TO GRP-HOLD-AREA.                               NC2334.2
045400     MOVE 02 TO SUB-2.                                            NC2334.2
045500     SET IDX-1 TO 1.                                              NC2334.2
045600     SEARCH ALL GRP-ENTRY AT END                                  NC2334.2
045700         PERFORM GRP-FAIL-PARGRAPH                                NC2334.2
045800         GO TO LEVEL-1-TEST-2                                     NC2334.2
045900         WHEN GRP (IDX-1) = GRP-HOLD-AREA NEXT SENTENCE.          NC2334.2
046000                                                                  NC2334.2
046100     PERFORM PASS-TH.                                             NC2334.2
046200     GO TO LEVEL-1-TEST-2.                                        NC2334.2
046300                                                                  NC2334.2
046400 GRP-FAIL-PARGRAPH.                                               NC2334.2
046500     MOVE GRP-HOLD-AREA TO CORRECT-A.                             NC2334.2
046600     IF ENTRY-1 (SUB-2) EQUAL TO GRP-HOLD-AREA                    NC2334.2
046700         MOVE "IDX-1" TO END-IDX                                  NC2334.2
046800         SET IDX-VALU TO IDX-1                                    NC2334.2
046900         MOVE "SEE NOTE 1 FOR DIAGNOSTIC  " TO RE-MARK            NC2334.2
047000         MOVE END-STMT TO COMPUTED-A  ELSE                        NC2334.2
047100     MOVE ENTRY-1 (SUB-2) TO COMPUTED-A                           NC2334.2
047200     MOVE "SEE NOTE 2 FOR DIAGNOSTIC  " TO RE-MARK.               NC2334.2
047300                                                                  NC2334.2
047400     PERFORM FAIL-TH.                                             NC2334.2
047500 LEVEL-1-TEST-2.                                                  NC2334.2
047600     MOVE "LEVEL-1-TEST-2      " TO PAR-NAME.                     NC2334.2
047700     MOVE "GRP01" TO GRP-HOLD-AREA.                               NC2334.2
047800     MOVE 01 TO SUB-2.                                            NC2334.2
047900     SET IDX-1 TO 1.                                              NC2334.2
048000     SEARCH ALL GRP-ENTRY AT END                                  NC2334.2
048100         PERFORM GRP-FAIL-PARGRAPH                                NC2334.2
048200         GO TO LEVEL-1-TEST-3                                     NC2334.2
048300         WHEN GRP (IDX-1) = GRP-HOLD-AREA NEXT SENTENCE.          NC2334.2
048400                                                                  NC2334.2
048500     PERFORM PASS-TH.                                             NC2334.2
048600 LEVEL-1-TEST-3.                                                  NC2334.2
048700     MOVE "LEVEL-1-TEST-3      " TO PAR-NAME.                     NC2334.2
048800     MOVE "GRP10" TO GRP-HOLD-AREA.                               NC2334.2
048900     MOVE 10 TO SUB-2.                                            NC2334.2
049000     SET IDX-1 TO 1.                                              NC2334.2
049100     SEARCH ALL GRP-ENTRY AT END                                  NC2334.2
049200         PERFORM GRP-FAIL-PARGRAPH                                NC2334.2
049300         GO TO LEVEL-1-TEST-4                                     NC2334.2
049400         WHEN GRP (IDX-1) = GRP-HOLD-AREA NEXT SENTENCE.          NC2334.2
049500                                                                  NC2334.2
049600     PERFORM PASS-TH.                                             NC2334.2
049700 LEVEL-1-TEST-4.                                                  NC2334.2
049800     MOVE "LEVEL-1-TEST-4      " TO PAR-NAME.                     NC2334.2
049900     MOVE "GRP05" TO GRP-HOLD-AREA.                               NC2334.2
050000     MOVE 05 TO SUB-2.                                            NC2334.2
050100     SET IDX-1 TO 05.                                             NC2334.2
050200     SEARCH ALL GRP-ENTRY                                         NC2334.2
050300         WHEN GRP (IDX-1) = GRP-HOLD-AREA GO TO PASS-TH-TEST-4.   NC2334.2
050400     PERFORM GRP-FAIL-PARGRAPH.                                   NC2334.2
050500     GO TO LEVEL-2-TEST-1.                                        NC2334.2
050600 PASS-TH-TEST-4.                                                  NC2334.2
050700                                                                  NC2334.2
050800     PERFORM PASS-TH.                                             NC2334.2
050900                                                                  NC2334.2
051000 LEVEL-2-TEST-1.                                                  NC2334.2
051100     MOVE "SEARCH ALL-SEC LEVEL" TO FEATURE.                      NC2334.2
051200     MOVE "LEVEL-2-TEST-1      " TO PAR-NAME.                     NC2334.2
051300     MOVE "SEC (01,01)" TO SEC-HOLD-AREA.                         NC2334.2
051400     MOVE 1 TO SUB-1   SUB-2.                                     NC2334.2
051500     SET IDX-1 IDX-2 TO 1.                                        NC2334.2
051600     SEARCH ALL GRP2-ENTRY AT END                                 NC2334.2
051700         PERFORM SEC-FAIL-PARGRAF                                 NC2334.2
051800         GO TO LEVEL-2-TEST-2                                     NC2334.2
051900         WHEN SEC (IDX-1, IDX-2) = "(01,01)" NEXT SENTENCE.       NC2334.2
052000                                                                  NC2334.2
052100     PERFORM PASS-TH.                                             NC2334.2
052200                                                                  NC2334.2
052300 LEVEL-2-TEST-2.                                                  NC2334.2
052400     MOVE "LEVEL-2-TEST-2      " TO PAR-NAME.                     NC2334.2
052500     MOVE "SEC (05,10)" TO SEC-HOLD-AREA.                         NC2334.2
052600     MOVE 05 TO SUB-1.                                            NC2334.2
052700     MOVE 10 TO SUB-2.                                            NC2334.2
052800     SET IDX-1 TO 5.                                              NC2334.2
052900     SET IDX-2 TO 1.                                              NC2334.2
053000     SEARCH ALL GRP2-ENTRY AT END                                 NC2334.2
053100         PERFORM SEC-FAIL-PARGRAF                                 NC2334.2
053200         GO TO LEVEL-2-TEST-3                                     NC2334.2
053300         WHEN SEC (IDX-1, IDX-2) = "(05,10)" NEXT SENTENCE.       NC2334.2
053400                                                                  NC2334.2
053500     PERFORM PASS-TH.                                             NC2334.2
053600                                                                  NC2334.2
053700 LEVEL-2-TEST-3.                                                  NC2334.2
053800     MOVE "LEVEL-2-TEST-3      " TO PAR-NAME.                     NC2334.2
053900     MOVE "SEC (10,10)" TO SEC-HOLD-AREA.                         NC2334.2
054000     SET IDX-1 TO 10.                                             NC2334.2
054100     SET IDX-2 TO 1.                                              NC2334.2
054200     MOVE 10 TO SUB-1  SUB-2.                                     NC2334.2
054300     SEARCH ALL GRP2-ENTRY AT END                                 NC2334.2
054400         PERFORM SEC-FAIL-PARGRAF                                 NC2334.2
054500         GO TO LEVEL-2-TEST-4                                     NC2334.2
054600         WHEN SEC (IDX-1, IDX-2) = "(10,10)" NEXT SENTENCE.       NC2334.2
054700                                                                  NC2334.2
054800     PERFORM PASS-TH.                                             NC2334.2
054900 LEVEL-2-TEST-4.                                                  NC2334.2
055000     MOVE "LEVEL-2-TEST-4      " TO PAR-NAME.                     NC2334.2
055100     MOVE "SEC (08,02)" TO SEC-HOLD-AREA.                         NC2334.2
055200     MOVE 08 TO SUB-1.                                            NC2334.2
055300     MOVE 02 TO SUB-2.                                            NC2334.2
055400     SET IDX-1 TO 08.                                             NC2334.2
055500     SET IDX-2 TO 01.                                             NC2334.2
055600     SEARCH ALL GRP2-ENTRY                                        NC2334.2
055700         WHEN SEC (IDX-1, IDX-2) = "(08,02)" GO TO PASS-TH-2-4.   NC2334.2
055800     PERFORM SEC-FAIL-PARGRAF.                                    NC2334.2
055900     GO TO LEVEL-3-TEST-1.                                        NC2334.2
056000 PASS-TH-2-4.                                                     NC2334.2
056100                                                                  NC2334.2
056200     PERFORM PASS-TH.                                             NC2334.2
056300     GO TO LEVEL-3-TEST-1.                                        NC2334.2
056400                                                                  NC2334.2
056500 SEC-FAIL-PARGRAF.                                                NC2334.2
056600     MOVE SEC-HOLD-AREA TO CORRECT-A.                             NC2334.2
056700     IF ENTRY-2 (SUB-1, SUB-2) EQUAL TO SEC-HOLD-AREA             NC2334.2
056800         MOVE "IDX-2" TO END-IDX                                  NC2334.2
056900         SET IDX-VALU TO IDX-2                                    NC2334.2
057000         MOVE "SEE NOTE 1 FOR DIAGNOSTIC  " TO RE-MARK            NC2334.2
057100         MOVE END-STMT TO COMPUTED-A  ELSE                        NC2334.2
057200     MOVE ENTRY-2 (SUB-1, SUB-2) TO COMPUTED-A                    NC2334.2
057300     MOVE "SEE NOTE 2 FOR DIAGNOSTIC  " TO RE-MARK.               NC2334.2
057400                                                                  NC2334.2
057500     PERFORM FAIL-TH.                                             NC2334.2
057600                                                                  NC2334.2
057700 LEVEL-3-TEST-1.                                                  NC2334.2
057800     MOVE "LEVEL-3-TEST-1      " TO PAR-NAME.                     NC2334.2
057900     MOVE "SEARCH ALL THIRD LEV" TO FEATURE.                      NC2334.2
058000     MOVE 1 TO SUB-1  SUB-2  SUB-3.                               NC2334.2
058100     MOVE "ELEM (01,01,01)" TO ELEM-HOLD-AREA.                    NC2334.2
058200     SET IDX-1 IDX-2 IDX-3 TO 1.                                  NC2334.2
058300     SEARCH ALL GRP3-ENTRY                                        NC2334.2
058400         WHEN ELEM (IDX-1, IDX-2, IDX-3) = ",01,01)"              NC2334.2
058500             GO TO PASS-TH-3-1.                                   NC2334.2
058600     PERFORM ELEM-FAIL-PARA.                                      NC2334.2
058700     GO TO LEVEL-3-TEST-2.                                        NC2334.2
058800 PASS-TH-3-1.                                                     NC2334.2
058900                                                                  NC2334.2
059000     PERFORM PASS-TH.                                             NC2334.2
059100                                                                  NC2334.2
059200 LEVEL-3-TEST-2.                                                  NC2334.2
059300     MOVE "LEVEL-3-TEST-2      " TO PAR-NAME.                     NC2334.2
059400     MOVE 05 TO SUB-1.                                            NC2334.2
059500     MOVE 06 TO SUB-2.                                            NC2334.2
059600     MOVE 07 TO SUB-3.                                            NC2334.2
059700     SET IDX-1 TO 05.                                             NC2334.2
059800     SET IDX-2 TO 06.                                             NC2334.2
059900     SET IDX-3 TO 1.                                              NC2334.2
060000     MOVE "ELEM (05,06,07)" TO ELEM-HOLD-AREA.                    NC2334.2
060100     SEARCH ALL GRP3-ENTRY AT END                                 NC2334.2
060200         PERFORM ELEM-FAIL-PARA                                   NC2334.2
060300         GO TO LEVEL-3-TEST-3                                     NC2334.2
060400         WHEN ELEM (IDX-1, IDX-2, IDX-3) = ",06,07)"              NC2334.2
060500             NEXT SENTENCE.                                       NC2334.2
060600                                                                  NC2334.2
060700     PERFORM PASS-TH.                                             NC2334.2
060800                                                                  NC2334.2
060900 LEVEL-3-TEST-3.                                                  NC2334.2
061000     MOVE "LEVEL-3-TEST-3      " TO PAR-NAME.                     NC2334.2
061100     MOVE 10 TO SUB-1 SUB-2 SUB-3.                                NC2334.2
061200     SET IDX-1 IDX-2 TO 10.                                       NC2334.2
061300     SET IDX-3 TO 1.                                              NC2334.2
061400     MOVE "ELEM (10,10,10)" TO ELEM-HOLD-AREA.                    NC2334.2
061500     SEARCH ALL GRP3-ENTRY AT END                                 NC2334.2
061600         PERFORM ELEM-FAIL-PARA                                   NC2334.2
061700         GO TO LEVEL-3-TEST-4                                     NC2334.2
061800         WHEN ELEM (IDX-1, IDX-2, IDX-3) = ",10,10)"              NC2334.2
061900             NEXT SENTENCE.                                       NC2334.2
062000                                                                  NC2334.2
062100     PERFORM PASS-TH.                                             NC2334.2
062200 LEVEL-3-TEST-4.                                                  NC2334.2
062300     MOVE "LEVEL-3-TEST-4      " TO PAR-NAME.                     NC2334.2
062400     MOVE "ELEM (07,06,05)" TO ELEM-HOLD-AREA.                    NC2334.2
062500     MOVE 07 TO SUB-1.                                            NC2334.2
062600     MOVE 06 TO SUB-2.                                            NC2334.2
062700     MOVE 05 TO SUB-3.                                            NC2334.2
062800     SET IDX-1 TO 07.                                             NC2334.2
062900     SET IDX-2 TO 06.                                             NC2334.2
063000     SET IDX-3 TO 03.                                             NC2334.2
063100     SEARCH ALL GRP3-ENTRY AT END                                 NC2334.2
063200         PERFORM ELEM-FAIL-PARA                                   NC2334.2
063300         GO TO MULT-SEARCH-TEST-1                                 NC2334.2
063400         WHEN ELEM (IDX-1, IDX-2, IDX-3) = ",06,05)"              NC2334.2
063500             NEXT SENTENCE.                                       NC2334.2
063600                                                                  NC2334.2
063700     PERFORM PASS-TH.                                             NC2334.2
063800     GO TO MULT-SEARCH-TEST-1.                                    NC2334.2
063900 ELEM-FAIL-PARA.                                                  NC2334.2
064000     MOVE ELEM-HOLD-AREA TO CORRECT-A.                            NC2334.2
064100     IF ENTRY-3 (SUB-1, SUB-2, SUB-3) EQUAL TO ELEM-HOLD-AREA     NC2334.2
064200         MOVE "IDX-3" TO END-IDX                                  NC2334.2
064300         SET IDX-VALU TO IDX-3                                    NC2334.2
064400         MOVE "SEE NOTE 1 FOR DIAGNOSTIC  " TO RE-MARK            NC2334.2
064500         MOVE END-STMT TO COMPUTED-A  ELSE                        NC2334.2
064600     MOVE ENTRY-3 (SUB-1, SUB-2, SUB-3) TO COMPUTED-A             NC2334.2
064700     MOVE "SEE NOTE 2 FOR DIAGNOSTIC  " TO RE-MARK.               NC2334.2
064800                                                                  NC2334.2
064900     PERFORM FAIL-TH.                                             NC2334.2
065000                                                                  NC2334.2
065100 MULT-SEARCH-TEST-1.                                              NC2334.2
065200     MOVE "MULT-SEARCH-TEST-1  " TO PAR-NAME.                     NC2334.2
065300     MOVE "MULTIPLE SEARCH STMT" TO FEATURE.                      NC2334.2
065400     MOVE "GRP08" TO GRP-HOLD-AREA.                               NC2334.2
065500     MOVE "SEC (08,07)" TO SEC-HOLD-AREA.                         NC2334.2
065600     SET IDX-1 IDX-2 TO 1.                                        NC2334.2
065700     SEARCH ALL GRP-ENTRY AT END GO TO MULT-SEARCH-FAIL1          NC2334.2
065800         WHEN GRP (IDX-1) = "GRP08" NEXT SENTENCE.                NC2334.2
065900     SEARCH ALL GRP2-ENTRY AT END GO TO MULT-SEARCH-FAIL          NC2334.2
066000         WHEN SEC (IDX-1, IDX-2) = "(08,07)" NEXT SENTENCE.       NC2334.2
066100                                                                  NC2334.2
066200     PERFORM PASS-TH.                                             NC2334.2
066300     GO TO MULT-SEARCH-7-INIT-3.                                  NC2334.2
066400 MULT-SEARCH-FAIL1.                                               NC2334.2
066500     MOVE GRP-HOLD-AREA TO CORRECT-A.                             NC2334.2
066600     IF ENTRY-1 (08) EQUAL TO GRP-HOLD-AREA                       NC2334.2
066700         MOVE "IDX-1" TO END-IDX                                  NC2334.2
066800         MOVE "SEE NOTE 1 FOR DIAGNOSTIC  " TO RE-MARK            NC2334.2
066900         SET IDX-VALU TO IDX-1                                    NC2334.2
067000         MOVE END-STMT TO COMPUTED-A  ELSE                        NC2334.2
067100     MOVE ENTRY-1 (08) TO COMPUTED-A                              NC2334.2
067200     MOVE "SEE NOTE 2 FOR DIAGNOSTIC  " TO RE-MARK.               NC2334.2
067300                                                                  NC2334.2
067400     PERFORM FAIL-TH.                                             NC2334.2
067500     GO TO MULT-SEARCH-7-INIT-3.                                  NC2334.2
067600 MULT-SEARCH-FAIL.                                                NC2334.2
067700     MOVE SEC-HOLD-AREA TO CORRECT-A.                             NC2334.2
067800     IF ENTRY-2 (08, 07) EQUAL TO SEC-HOLD-AREA                   NC2334.2
067900         MOVE "IDX-2" TO END-IDX                                  NC2334.2
068000         SET IDX-VALU TO IDX-2                                    NC2334.2
068100         MOVE "SEE NOTE 1 FOR DIAGNOSTIC  " TO RE-MARK            NC2334.2
068200         MOVE END-STMT TO COMPUTED-A  ELSE                        NC2334.2
068300     MOVE ENTRY-2 (08, 07) TO COMPUTED-A                          NC2334.2
068400     MOVE "SEE NOTE 2 FOR DIAGNOSTIC  " TO RE-MARK.               NC2334.2
068500                                                                  NC2334.2
068600     PERFORM FAIL-TH.                                             NC2334.2
068700                                                                  NC2334.2
068800 MULT-SEARCH-7-INIT-3.                                            NC2334.2
068900     MOVE   "MULT-SEARCH-7-TEST-3" TO PAR-NAME.                   NC2334.2
069000     MOVE   "VI-122 6.21"          TO ANSI-REFERENCE.             NC2334.2
069100     MOVE   "A2" TO L1-HOLD.                                      NC2334.2
069200     MOVE   "B1" TO L2-HOLD.                                      NC2334.2
069300     MOVE   "C2" TO L3-HOLD.                                      NC2334.2
069400     MOVE   "D1" TO L4-HOLD.                                      NC2334.2
069500     MOVE   "E2" TO L5-HOLD.                                      NC2334.2
069600     MOVE   "F1" TO L6-HOLD.                                      NC2334.2
069700     MOVE   "G2" TO L7-HOLD.                                      NC2334.2
069800     SET     X1 X2 X3 X4 X5 X6 X7 TO 1.                           NC2334.2
069900     PERFORM MULT-SEARCH-7-INIT-3-A                               NC2334.2
070000             VARYING N1 FROM 1 BY 1 UNTIL N1 > 2                  NC2334.2
070100               AFTER N2 FROM 1 BY 1 UNTIL N2 > 2                  NC2334.2
070200               AFTER N3 FROM 1 BY 1 UNTIL N3 > 2                  NC2334.2
070300               AFTER N4 FROM 1 BY 1 UNTIL N4 > 2                  NC2334.2
070400               AFTER N5 FROM 1 BY 1 UNTIL N5 > 2                  NC2334.2
070500               AFTER N6 FROM 1 BY 1 UNTIL N6 > 2                  NC2334.2
070600               AFTER N7 FROM 1 BY 1 UNTIL N7 > 2.                 NC2334.2
070700     GO TO   MULT-SEARCH-7-TEST-3.                                NC2334.2
070800 MULT-SEARCH-7-INIT-3-A.                                          NC2334.2
070900                                                                  NC2334.2
071000     MOVE    N1 TO ENTRY-7-1 (N1).                                NC2334.2
071100     MOVE   "A" TO CHARS-7-1 (N1).                                NC2334.2
071200     MOVE    N2 TO ENTRY-7-2 (N1 N2).                             NC2334.2
071300     MOVE   "B" TO CHARS-7-2 (N1 N2).                             NC2334.2
071400     MOVE    N3 TO ENTRY-7-3 (N1 N2 N3).                          NC2334.2
071500     MOVE   "C" TO CHARS-7-3 (N1 N2 N3).                          NC2334.2
071600     MOVE    N4 TO ENTRY-7-4 (N1 N2 N3 N4).                       NC2334.2
071700     MOVE   "D" TO CHARS-7-4 (N1 N2 N3 N4).                       NC2334.2
071800     MOVE    N5 TO ENTRY-7-5 (N1 N2 N3 N4 N5).                    NC2334.2
071900     MOVE   "E" TO CHARS-7-5 (N1 N2 N3 N4 N5).                    NC2334.2
072000     MOVE    N6 TO ENTRY-7-6 (N1 N2 N3 N4 N5 N6).                 NC2334.2
072100     MOVE   "F" TO CHARS-7-6 (N1 N2 N3 N4 N5 N6).                 NC2334.2
072200     MOVE    N7 TO ENTRY-7-7 (N1 N2 N3 N4 N5 N6 N7).              NC2334.2
072300     MOVE   "G" TO CHARS-7-7 (N1 N2 N3 N4 N5 N6 N7).              NC2334.2
072400 MULT-SEARCH-7-DELETE-3.                                          NC2334.2
072500     PERFORM DE-LETE.                                             NC2334.2
072600     PERFORM PRINT-DETAIL.                                        NC2334.2
072700     GO TO   END-SEARCH-TEST.                                     NC2334.2
072800 MULT-SEARCH-7-TEST-3.                                            NC2334.2
072900     SEARCH  ALL GRP-7-1-ENTRY                                    NC2334.2
073000             AT END  GO TO MULT-SEARCH-7-FAIL-1                   NC2334.2
073100             WHEN    ENTRY-7-1G (X1) =  L1-HOLD                   NC2334.2
073200                     NEXT SENTENCE.                               NC2334.2
073300     SET     X1 TO 1.                                             NC2334.2
073400     SEARCH  ALL GRP-7-2-ENTRY                                    NC2334.2
073500             AT END  GO TO MULT-SEARCH-7-FAIL-2                   NC2334.2
073600             WHEN    ENTRY-7-2G (X1 X2) = L2-HOLD                 NC2334.2
073700                     NEXT SENTENCE.                               NC2334.2
073800     SET     X1 TO 2.                                             NC2334.2
073900     SET     X2 TO 1.                                             NC2334.2
074000     SEARCH  ALL GRP-7-3-ENTRY                                    NC2334.2
074100             AT END  GO TO MULT-SEARCH-7-FAIL-3                   NC2334.2
074200             WHEN    ENTRY-7-3G (X1 X2 X3) = L3-HOLD              NC2334.2
074300                     NEXT SENTENCE.                               NC2334.2
074400     SET     X1 TO 1.                                             NC2334.2
074500     SET     X2, X3 TO 1.                                         NC2334.2
074600     SEARCH  ALL GRP-7-4-ENTRY                                    NC2334.2
074700             AT END  GO TO MULT-SEARCH-7-FAIL-4                   NC2334.2
074800             WHEN    ENTRY-7-4G (X1 X2 X3 X4) =  L4-HOLD          NC2334.2
074900                     NEXT SENTENCE.                               NC2334.2
075000     SET     X1 TO 2.                                             NC2334.2
075100     SET     X2, X3, X4 TO 1.                                     NC2334.2
075200     SEARCH  ALL GRP-7-5-ENTRY                                    NC2334.2
075300             AT END  GO TO MULT-SEARCH-7-FAIL-5                   NC2334.2
075400             WHEN    ENTRY-7-5G (X1 X2 X3 X4 X5) = L5-HOLD        NC2334.2
075500                     NEXT SENTENCE.                               NC2334.2
075600     SET     X1 TO 1.                                             NC2334.2
075700     SET     X2, X3, X4, X5 TO 1.                                 NC2334.2
075800     SEARCH  ALL GRP-7-6-ENTRY                                    NC2334.2
075900             AT END  GO TO MULT-SEARCH-7-FAIL-6                   NC2334.2
076000             WHEN ENTRY-7-6G (X1 X2 X3 X4 X5 X6) = L6-HOLD        NC2334.2
076100                     NEXT SENTENCE.                               NC2334.2
076200     SET     X1 TO 2.                                             NC2334.2
076300     SET     X2, X3, X4, X6 TO 1.                                 NC2334.2
076400     SEARCH  ALL GRP-7-7-ENTRY                                    NC2334.2
076500             AT END  GO TO MULT-SEARCH-7-FAIL-7                   NC2334.2
076600             WHEN ENTRY-7-7G (X1 X2 X3 X4 X5 X6 X7) = L7-HOLD     NC2334.2
076700                     NEXT SENTENCE.                               NC2334.2
076800                                                                  NC2334.2
076900     PERFORM PASS-TH.                                             NC2334.2
077000     GO TO   END-SEARCH-TEST.                                     NC2334.2
077100                                                                  NC2334.2
077200 MULT-SEARCH-7-FAIL-1.                                            NC2334.2
077300     MOVE    L1-HOLD TO CORRECT-A.                                NC2334.2
077400     IF      ENTRY-7-1 (2) = L1-HOLD                              NC2334.2
077500             MOVE   "IX-1" TO END-IDX                             NC2334.2
077600             SET     IDX-VALU TO X1                               NC2334.2
077700             MOVE   "SEE NOTE 1 FOR DIAGNOSTIC  " TO RE-MARK      NC2334.2
077800             MOVE    END-STMT TO COMPUTED-A                       NC2334.2
077900     ELSE                                                         NC2334.2
078000             MOVE    ENTRY-7-1 (2) TO COMPUTED-A                  NC2334.2
078100             MOVE   "SEE NOTE 2 FOR DIAGNOSTIC  " TO RE-MARK.     NC2334.2
078200                                                                  NC2334.2
078300     PERFORM FAIL-TH.                                             NC2334.2
078400     GO TO   END-SEARCH-TEST.                                     NC2334.2
078500                                                                  NC2334.2
078600 MULT-SEARCH-7-FAIL-2.                                            NC2334.2
078700     MOVE    L2-HOLD TO CORRECT-A.                                NC2334.2
078800     IF      ENTRY-7-2 (2 1) = L1-HOLD                            NC2334.2
078900             MOVE   "X2" TO END-IDX                               NC2334.2
079000             SET     IDX-VALU TO X2                               NC2334.2
079100             MOVE   "SEE NOTE 1 FOR DIAGNOSTIC  " TO RE-MARK      NC2334.2
079200             MOVE    END-STMT TO COMPUTED-A                       NC2334.2
079300     ELSE                                                         NC2334.2
079400             MOVE    ENTRY-7-2 (2 1) TO COMPUTED-A                NC2334.2
079500             MOVE   "SEE NOTE 2 FOR DIAGNOSTIC  " TO RE-MARK.     NC2334.2
079600                                                                  NC2334.2
079700     PERFORM FAIL-TH.                                             NC2334.2
079800     GO TO   END-SEARCH-TEST.                                     NC2334.2
079900                                                                  NC2334.2
080000 MULT-SEARCH-7-FAIL-3.                                            NC2334.2
080100     MOVE    L3-HOLD TO CORRECT-A.                                NC2334.2
080200     IF      ENTRY-7-3 (2 1 2) = L3-HOLD                          NC2334.2
080300             MOVE   "X3" TO END-IDX                               NC2334.2
080400             SET     IDX-VALU TO X3                               NC2334.2
080500             MOVE   "SEE NOTE 1 FOR DIAGNOSTIC  " TO RE-MARK      NC2334.2
080600             MOVE    END-STMT TO COMPUTED-A                       NC2334.2
080700     ELSE                                                         NC2334.2
080800             MOVE    ENTRY-7-3 (2 1 2) TO COMPUTED-A              NC2334.2
080900             MOVE   "SEE NOTE 2 FOR DIAGNOSTIC  " TO RE-MARK.     NC2334.2
081000                                                                  NC2334.2
081100     PERFORM FAIL-TH.                                             NC2334.2
081200     GO TO   END-SEARCH-TEST.                                     NC2334.2
081300                                                                  NC2334.2
081400 MULT-SEARCH-7-FAIL-4.                                            NC2334.2
081500     MOVE    L4-HOLD TO CORRECT-A.                                NC2334.2
081600     IF      ENTRY-7-4 (2 1 2 1) = L4-HOLD                        NC2334.2
081700             MOVE   "X4" TO END-IDX                               NC2334.2
081800             SET     IDX-VALU TO X4                               NC2334.2
081900             MOVE   "SEE NOTE 1 FOR DIAGNOSTIC  " TO RE-MARK      NC2334.2
082000             MOVE    END-STMT TO COMPUTED-A                       NC2334.2
082100     ELSE                                                         NC2334.2
082200             MOVE    ENTRY-7-4 (2 1 2 1) TO COMPUTED-A            NC2334.2
082300             MOVE   "SEE NOTE 2 FOR DIAGNOSTIC  " TO RE-MARK.     NC2334.2
082400                                                                  NC2334.2
082500     PERFORM FAIL-TH.                                             NC2334.2
082600     GO TO   END-SEARCH-TEST.                                     NC2334.2
082700                                                                  NC2334.2
082800 MULT-SEARCH-7-FAIL-5.                                            NC2334.2
082900     MOVE    L5-HOLD TO CORRECT-A.                                NC2334.2
083000     IF      ENTRY-7-5 (2 1 2 1 2) = L5-HOLD                      NC2334.2
083100             MOVE   "X5" TO END-IDX                               NC2334.2
083200             SET     IDX-VALU TO X5                               NC2334.2
083300             MOVE   "SEE NOTE 1 FOR DIAGNOSTIC  " TO RE-MARK      NC2334.2
083400             MOVE    END-STMT TO COMPUTED-A                       NC2334.2
083500     ELSE                                                         NC2334.2
083600             MOVE    ENTRY-7-5 (2 1 2 1 2) TO COMPUTED-A          NC2334.2
083700             MOVE   "SEE NOTE 2 FOR DIAGNOSTIC  " TO RE-MARK.     NC2334.2
083800                                                                  NC2334.2
083900     PERFORM FAIL-TH.                                             NC2334.2
084000     GO TO   END-SEARCH-TEST.                                     NC2334.2
084100                                                                  NC2334.2
084200 MULT-SEARCH-7-FAIL-6.                                            NC2334.2
084300     MOVE    L6-HOLD TO CORRECT-A.                                NC2334.2
084400     IF      ENTRY-7-6 (2 1 2 1 2 1) = L6-HOLD                    NC2334.2
084500             MOVE   "X6" TO END-IDX                               NC2334.2
084600             SET     IDX-VALU TO X6                               NC2334.2
084700             MOVE   "SEE NOTE 1 FOR DIAGNOSTIC  " TO RE-MARK      NC2334.2
084800             MOVE    END-STMT TO COMPUTED-A                       NC2334.2
084900     ELSE                                                         NC2334.2
085000             MOVE    ENTRY-7-6 (2 1 2 1 2 1) TO COMPUTED-A        NC2334.2
085100             MOVE   "SEE NOTE 2 FOR DIAGNOSTIC  " TO RE-MARK.     NC2334.2
085200                                                                  NC2334.2
085300     PERFORM FAIL-TH.                                             NC2334.2
085400     GO TO   END-SEARCH-TEST.                                     NC2334.2
085500                                                                  NC2334.2
085600 MULT-SEARCH-7-FAIL-7.                                            NC2334.2
085700     MOVE    L7-HOLD TO CORRECT-A.                                NC2334.2
085800     IF      ENTRY-7-7 (2 1 2 1 2 1 2) = L6-HOLD                  NC2334.2
085900             MOVE   "X7" TO END-IDX                               NC2334.2
086000             SET     IDX-VALU TO X7                               NC2334.2
086100             MOVE   "SEE NOTE 1 FOR DIAGNOSTIC  " TO RE-MARK      NC2334.2
086200             MOVE    END-STMT TO COMPUTED-A                       NC2334.2
086300     ELSE                                                         NC2334.2
086400             MOVE    ENTRY-7-7 (2 1 2 1 2 1 2) TO COMPUTED-A      NC2334.2
086500             MOVE   "SEE NOTE 2 FOR DIAGNOSTIC  " TO RE-MARK.     NC2334.2
086600                                                                  NC2334.2
086700     PERFORM FAIL-TH.                                             NC2334.2
086800                                                                  NC2334.2
086900     GO TO END-SEARCH-TEST.                                       NC2334.2
087000                                                                  NC2334.2
087100 PASS-TH.                                                         NC2334.2
087200     PERFORM PASS.                                                NC2334.2
087300     PERFORM PRINT-DETAIL.                                        NC2334.2
087400 FAIL-TH.                                                         NC2334.2
087500     PERFORM FAIL.                                                NC2334.2
087600     PERFORM  PRINT-DETAIL.                                       NC2334.2
087700 END-SEARCH-TEST.                                                 NC2334.2
087800     EXIT.                                                        NC2334.2
087900 CCVS-EXIT SECTION.                                               NC2334.2
088000 CCVS-999999.                                                     NC2334.2
088100     GO TO CLOSE-FILES.                                           NC2334.2
      *END-OF,NC233A                                                                  
