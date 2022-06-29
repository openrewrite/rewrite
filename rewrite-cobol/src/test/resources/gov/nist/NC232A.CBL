      *HEADER,COBOL,NC232A                                                            
000100 IDENTIFICATION DIVISION.                                         NC2324.2
000200 PROGRAM-ID.                                                      NC2324.2
000300     NC232A.                                                      NC2324.2
000400****************************************************************  NC2324.2
000500*                                                              *  NC2324.2
000600*    VALIDATION FOR:-                                          *  NC2324.2
000700*                                                              *  NC2324.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2324.2
000900*                                                              *  NC2324.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC2324.2
001100*                                                              *  NC2324.2
001200****************************************************************  NC2324.2
001300*                                                              *  NC2324.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  NC2324.2
001500*                                                              *  NC2324.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  NC2324.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  NC2324.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  NC2324.2
001900*                                                              *  NC2324.2
002000****************************************************************  NC2324.2
002100                                                                  NC2324.2
002200*                                                                 NC2324.2
002300*    PROGRAM NC232A USES FORMAT 1 OF THE "SEARCH" STATEMENT TO *  NC2324.2
002400*    ACCESS A THREE DIMENSIONAL TABLE.  THE OPTIONAL "VARYING" *  NC2324.2
002500*    PHRASE IS USED WITH AN INDEX-NAME.                        *  NC2324.2
002600*                                                              *  NC2324.2
002700****************************************************************  NC2324.2
002800 ENVIRONMENT DIVISION.                                            NC2324.2
002900 CONFIGURATION SECTION.                                           NC2324.2
003000 SOURCE-COMPUTER.                                                 NC2324.2
003100     XXXXX082.                                                    NC2324.2
003200 OBJECT-COMPUTER.                                                 NC2324.2
003300     XXXXX083.                                                    NC2324.2
003400 INPUT-OUTPUT SECTION.                                            NC2324.2
003500 FILE-CONTROL.                                                    NC2324.2
003600     SELECT PRINT-FILE ASSIGN TO                                  NC2324.2
003700     XXXXX055.                                                    NC2324.2
003800 DATA DIVISION.                                                   NC2324.2
003900 FILE SECTION.                                                    NC2324.2
004000 FD  PRINT-FILE.                                                  NC2324.2
004100 01  PRINT-REC PICTURE X(120).                                    NC2324.2
004200 01  DUMMY-RECORD PICTURE X(120).                                 NC2324.2
004300 WORKING-STORAGE SECTION.                                         NC2324.2
004400 77  SUB-1              PICTURE S99  VALUE ZERO.                  NC2324.2
004500 77  SUB-2              PICTURE 99   VALUE ZERO.                  NC2324.2
004600 77  SUB-3              PICTURE 99   VALUE ZERO.                  NC2324.2
004700 77  CON-7              PICTURE 99  VALUE 07.                     NC2324.2
004800 77  CON-10             PICTURE 99  VALUE 10.                     NC2324.2
004900 77  ELEM-HOLD-AREA               PICTURE X(15)  VALUE SPACES.    NC2324.2
005000 77  CON-5              PICTURE 99  VALUE 05.                     NC2324.2
005100 77  SEC-HOLD-AREA                PICTURE X(11)  VALUE SPACES.    NC2324.2
005200 77  CON-6              PICTURE 99  VALUE 06.                     NC2324.2
005300 77  GRP-HOLD-AREA                PICTURE X(5)  VALUE SPACES.     NC2324.2
005400 01  GRP-NAME.                                                    NC2324.2
005500     02  FILLER              PICTURE XXX    VALUE "GRP".          NC2324.2
005600     02  ADD-GRP             PICTURE 99     VALUE 01.             NC2324.2
005700                                                                  NC2324.2
005800 01  SEC-NAME.                                                    NC2324.2
005900     02  FILLER              PICTURE X(5)   VALUE "SEC (".        NC2324.2
006000     02  SEC-GRP             PICTURE 99     VALUE 00.             NC2324.2
006100     02  FILLER              PICTURE X      VALUE ",".            NC2324.2
006200     02  ADD-SEC             PICTURE 99     VALUE 01.             NC2324.2
006300     02  FILLER              PICTURE X      VALUE ")".            NC2324.2
006400                                                                  NC2324.2
006500 01  ELEM-NAME.                                                   NC2324.2
006600     02  FILLER              PICTURE X(6)   VALUE "ELEM (".       NC2324.2
006700     02  ELEM-GRP            PICTURE 99     VALUE 00.             NC2324.2
006800     02  FILLER              PICTURE X      VALUE ",".            NC2324.2
006900     02  ELEM-SEC            PICTURE 99     VALUE 00.             NC2324.2
007000     02  FILLER              PICTURE X      VALUE ",".            NC2324.2
007100     02  ADD-ELEM            PICTURE 99     VALUE 01.             NC2324.2
007200     02  FILLER              PICTURE X      VALUE ")".            NC2324.2
007300                                                                  NC2324.2
007400 01  3-DIMENSION-TBL.                                             NC2324.2
007500     02  GRP-ENTRY OCCURS 10 TIMES INDEXED BY IDX-1.              NC2324.2
007600         03  ENTRY-1         PICTURE X(5).                        NC2324.2
007700         03  GRP2-ENTRY OCCURS 10 TIMES INDEXED BY IDX-2.         NC2324.2
007800             04  ENTRY-2     PICTURE X(11).                       NC2324.2
007900             04  GRP3-ENTRY OCCURS 10 TIMES INDEXED BY IDX-3.     NC2324.2
008000                 05  ENTRY-3 PICTURE X(15).                       NC2324.2
008100                                                                  NC2324.2
008200 01  END-STMT.                                                    NC2324.2
008300     02  FILLER              PICTURE X(7)  VALUE "AT END ".       NC2324.2
008400     02  END-IDX             PICTURE X(5)  VALUE SPACES.          NC2324.2
008500     02  FILLER              PICTURE XXX   VALUE " = ".           NC2324.2
008600     02  IDX-VALU            PICTURE 99    VALUE 00.              NC2324.2
008700     02  FILLER              PICTURE XXX   VALUE SPACES.          NC2324.2
008800 01  NOTE-1.                                                      NC2324.2
008900     02  FILLER                   PICTURE X(74)  VALUE            NC2324.2
009000     "NOTE 1 - CORRECT AND COMPUTED DATA ARE EQUAL BUT THE AT END NC2324.2
009100-    "PATH WAS TAKEN".                                            NC2324.2
009200     02  FILLER                   PICTURE X(46)  VALUE SPACES.    NC2324.2
009300 01  NOTE-2.                                                      NC2324.2
009400     02  FILLER                   PICTURE X(112)  VALUE           NC2324.2
009500     "NOTE 2 - CORRECT AND COMPUTED DATA ARE NOT EQUAL. THE COMPUTNC2324.2
009600-    "ED ENTRY WAS EXTRACTED FROM THE TABLE BY SUBSCRIPTS.".      NC2324.2
009700     02  FILLER                   PICTURE X(8)  VALUE SPACES.     NC2324.2
009800 01  TEST-RESULTS.                                                NC2324.2
009900     02 FILLER                   PIC X      VALUE SPACE.          NC2324.2
010000     02 FEATURE                  PIC X(20)  VALUE SPACE.          NC2324.2
010100     02 FILLER                   PIC X      VALUE SPACE.          NC2324.2
010200     02 P-OR-F                   PIC X(5)   VALUE SPACE.          NC2324.2
010300     02 FILLER                   PIC X      VALUE SPACE.          NC2324.2
010400     02  PAR-NAME.                                                NC2324.2
010500       03 FILLER                 PIC X(19)  VALUE SPACE.          NC2324.2
010600       03  PARDOT-X              PIC X      VALUE SPACE.          NC2324.2
010700       03 DOTVALUE               PIC 99     VALUE ZERO.           NC2324.2
010800     02 FILLER                   PIC X(8)   VALUE SPACE.          NC2324.2
010900     02 RE-MARK                  PIC X(61).                       NC2324.2
011000 01  TEST-COMPUTED.                                               NC2324.2
011100     02 FILLER                   PIC X(30)  VALUE SPACE.          NC2324.2
011200     02 FILLER                   PIC X(17)  VALUE                 NC2324.2
011300            "       COMPUTED=".                                   NC2324.2
011400     02 COMPUTED-X.                                               NC2324.2
011500     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          NC2324.2
011600     03 COMPUTED-N               REDEFINES COMPUTED-A             NC2324.2
011700                                 PIC -9(9).9(9).                  NC2324.2
011800     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         NC2324.2
011900     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     NC2324.2
012000     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     NC2324.2
012100     03       CM-18V0 REDEFINES COMPUTED-A.                       NC2324.2
012200         04 COMPUTED-18V0                    PIC -9(18).          NC2324.2
012300         04 FILLER                           PIC X.               NC2324.2
012400     03 FILLER PIC X(50) VALUE SPACE.                             NC2324.2
012500 01  TEST-CORRECT.                                                NC2324.2
012600     02 FILLER PIC X(30) VALUE SPACE.                             NC2324.2
012700     02 FILLER PIC X(17) VALUE "       CORRECT =".                NC2324.2
012800     02 CORRECT-X.                                                NC2324.2
012900     03 CORRECT-A                  PIC X(20) VALUE SPACE.         NC2324.2
013000     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      NC2324.2
013100     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         NC2324.2
013200     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     NC2324.2
013300     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     NC2324.2
013400     03      CR-18V0 REDEFINES CORRECT-A.                         NC2324.2
013500         04 CORRECT-18V0                     PIC -9(18).          NC2324.2
013600         04 FILLER                           PIC X.               NC2324.2
013700     03 FILLER PIC X(2) VALUE SPACE.                              NC2324.2
013800     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     NC2324.2
013900 01  CCVS-C-1.                                                    NC2324.2
014000     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PANC2324.2
014100-    "SS  PARAGRAPH-NAME                                          NC2324.2
014200-    "       REMARKS".                                            NC2324.2
014300     02 FILLER                     PIC X(20)    VALUE SPACE.      NC2324.2
014400 01  CCVS-C-2.                                                    NC2324.2
014500     02 FILLER                     PIC X        VALUE SPACE.      NC2324.2
014600     02 FILLER                     PIC X(6)     VALUE "TESTED".   NC2324.2
014700     02 FILLER                     PIC X(15)    VALUE SPACE.      NC2324.2
014800     02 FILLER                     PIC X(4)     VALUE "FAIL".     NC2324.2
014900     02 FILLER                     PIC X(94)    VALUE SPACE.      NC2324.2
015000 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       NC2324.2
015100 01  REC-CT                        PIC 99       VALUE ZERO.       NC2324.2
015200 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       NC2324.2
015300 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       NC2324.2
015400 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       NC2324.2
015500 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       NC2324.2
015600 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       NC2324.2
015700 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       NC2324.2
015800 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      NC2324.2
015900 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       NC2324.2
016000 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     NC2324.2
016100 01  CCVS-H-1.                                                    NC2324.2
016200     02  FILLER                    PIC X(39)    VALUE SPACES.     NC2324.2
016300     02  FILLER                    PIC X(42)    VALUE             NC2324.2
016400     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 NC2324.2
016500     02  FILLER                    PIC X(39)    VALUE SPACES.     NC2324.2
016600 01  CCVS-H-2A.                                                   NC2324.2
016700   02  FILLER                        PIC X(40)  VALUE SPACE.      NC2324.2
016800   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  NC2324.2
016900   02  FILLER                        PIC XXXX   VALUE             NC2324.2
017000     "4.2 ".                                                      NC2324.2
017100   02  FILLER                        PIC X(28)  VALUE             NC2324.2
017200            " COPY - NOT FOR DISTRIBUTION".                       NC2324.2
017300   02  FILLER                        PIC X(41)  VALUE SPACE.      NC2324.2
017400                                                                  NC2324.2
017500 01  CCVS-H-2B.                                                   NC2324.2
017600   02  FILLER                        PIC X(15)  VALUE             NC2324.2
017700            "TEST RESULT OF ".                                    NC2324.2
017800   02  TEST-ID                       PIC X(9).                    NC2324.2
017900   02  FILLER                        PIC X(4)   VALUE             NC2324.2
018000            " IN ".                                               NC2324.2
018100   02  FILLER                        PIC X(12)  VALUE             NC2324.2
018200     " HIGH       ".                                              NC2324.2
018300   02  FILLER                        PIC X(22)  VALUE             NC2324.2
018400            " LEVEL VALIDATION FOR ".                             NC2324.2
018500   02  FILLER                        PIC X(58)  VALUE             NC2324.2
018600     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2324.2
018700 01  CCVS-H-3.                                                    NC2324.2
018800     02  FILLER                      PIC X(34)  VALUE             NC2324.2
018900            " FOR OFFICIAL USE ONLY    ".                         NC2324.2
019000     02  FILLER                      PIC X(58)  VALUE             NC2324.2
019100     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC2324.2
019200     02  FILLER                      PIC X(28)  VALUE             NC2324.2
019300            "  COPYRIGHT   1985 ".                                NC2324.2
019400 01  CCVS-E-1.                                                    NC2324.2
019500     02 FILLER                       PIC X(52)  VALUE SPACE.      NC2324.2
019600     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              NC2324.2
019700     02 ID-AGAIN                     PIC X(9).                    NC2324.2
019800     02 FILLER                       PIC X(45)  VALUE SPACES.     NC2324.2
019900 01  CCVS-E-2.                                                    NC2324.2
020000     02  FILLER                      PIC X(31)  VALUE SPACE.      NC2324.2
020100     02  FILLER                      PIC X(21)  VALUE SPACE.      NC2324.2
020200     02 CCVS-E-2-2.                                               NC2324.2
020300         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      NC2324.2
020400         03 FILLER                   PIC X      VALUE SPACE.      NC2324.2
020500         03 ENDER-DESC               PIC X(44)  VALUE             NC2324.2
020600            "ERRORS ENCOUNTERED".                                 NC2324.2
020700 01  CCVS-E-3.                                                    NC2324.2
020800     02  FILLER                      PIC X(22)  VALUE             NC2324.2
020900            " FOR OFFICIAL USE ONLY".                             NC2324.2
021000     02  FILLER                      PIC X(12)  VALUE SPACE.      NC2324.2
021100     02  FILLER                      PIC X(58)  VALUE             NC2324.2
021200     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2324.2
021300     02  FILLER                      PIC X(13)  VALUE SPACE.      NC2324.2
021400     02 FILLER                       PIC X(15)  VALUE             NC2324.2
021500             " COPYRIGHT 1985".                                   NC2324.2
021600 01  CCVS-E-4.                                                    NC2324.2
021700     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      NC2324.2
021800     02 FILLER                       PIC X(4)   VALUE " OF ".     NC2324.2
021900     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      NC2324.2
022000     02 FILLER                       PIC X(40)  VALUE             NC2324.2
022100      "  TESTS WERE EXECUTED SUCCESSFULLY".                       NC2324.2
022200 01  XXINFO.                                                      NC2324.2
022300     02 FILLER                       PIC X(19)  VALUE             NC2324.2
022400            "*** INFORMATION ***".                                NC2324.2
022500     02 INFO-TEXT.                                                NC2324.2
022600       04 FILLER                     PIC X(8)   VALUE SPACE.      NC2324.2
022700       04 XXCOMPUTED                 PIC X(20).                   NC2324.2
022800       04 FILLER                     PIC X(5)   VALUE SPACE.      NC2324.2
022900       04 XXCORRECT                  PIC X(20).                   NC2324.2
023000     02 INF-ANSI-REFERENCE           PIC X(48).                   NC2324.2
023100 01  HYPHEN-LINE.                                                 NC2324.2
023200     02 FILLER  PIC IS X VALUE IS SPACE.                          NC2324.2
023300     02 FILLER  PIC IS X(65)    VALUE IS "************************NC2324.2
023400-    "*****************************************".                 NC2324.2
023500     02 FILLER  PIC IS X(54)    VALUE IS "************************NC2324.2
023600-    "******************************".                            NC2324.2
023700 01  CCVS-PGM-ID                     PIC X(9)   VALUE             NC2324.2
023800     "NC232A".                                                    NC2324.2
023900 PROCEDURE DIVISION.                                              NC2324.2
024000 CCVS1 SECTION.                                                   NC2324.2
024100 OPEN-FILES.                                                      NC2324.2
024200     OPEN     OUTPUT PRINT-FILE.                                  NC2324.2
024300     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   NC2324.2
024400     MOVE    SPACE TO TEST-RESULTS.                               NC2324.2
024500     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             NC2324.2
024600     GO TO CCVS1-EXIT.                                            NC2324.2
024700 CLOSE-FILES.                                                     NC2324.2
024800     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   NC2324.2
024900 TERMINATE-CCVS.                                                  NC2324.2
025000*    EXIT PROGRAM.                                                NC2324.2
025100*TERMINATE-CALL.                                                  NC2324.2
025200     STOP     RUN.                                                NC2324.2
025300 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         NC2324.2
025400 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           NC2324.2
025500 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          NC2324.2
025600 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      NC2324.2
025700     MOVE "****TEST DELETED****" TO RE-MARK.                      NC2324.2
025800 PRINT-DETAIL.                                                    NC2324.2
025900     IF REC-CT NOT EQUAL TO ZERO                                  NC2324.2
026000             MOVE "." TO PARDOT-X                                 NC2324.2
026100             MOVE REC-CT TO DOTVALUE.                             NC2324.2
026200     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      NC2324.2
026300     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               NC2324.2
026400        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 NC2324.2
026500          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 NC2324.2
026600     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              NC2324.2
026700     MOVE SPACE TO CORRECT-X.                                     NC2324.2
026800     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         NC2324.2
026900     MOVE     SPACE TO RE-MARK.                                   NC2324.2
027000 HEAD-ROUTINE.                                                    NC2324.2
027100     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC2324.2
027200     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC2324.2
027300     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC2324.2
027400     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC2324.2
027500 COLUMN-NAMES-ROUTINE.                                            NC2324.2
027600     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2324.2
027700     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2324.2
027800     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        NC2324.2
027900 END-ROUTINE.                                                     NC2324.2
028000     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.NC2324.2
028100 END-RTN-EXIT.                                                    NC2324.2
028200     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2324.2
028300 END-ROUTINE-1.                                                   NC2324.2
028400      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      NC2324.2
028500      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               NC2324.2
028600      ADD PASS-COUNTER TO ERROR-HOLD.                             NC2324.2
028700*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   NC2324.2
028800      MOVE PASS-COUNTER TO CCVS-E-4-1.                            NC2324.2
028900      MOVE ERROR-HOLD TO CCVS-E-4-2.                              NC2324.2
029000      MOVE CCVS-E-4 TO CCVS-E-2-2.                                NC2324.2
029100      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           NC2324.2
029200  END-ROUTINE-12.                                                 NC2324.2
029300      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        NC2324.2
029400     IF       ERROR-COUNTER IS EQUAL TO ZERO                      NC2324.2
029500         MOVE "NO " TO ERROR-TOTAL                                NC2324.2
029600         ELSE                                                     NC2324.2
029700         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       NC2324.2
029800     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           NC2324.2
029900     PERFORM WRITE-LINE.                                          NC2324.2
030000 END-ROUTINE-13.                                                  NC2324.2
030100     IF DELETE-COUNTER IS EQUAL TO ZERO                           NC2324.2
030200         MOVE "NO " TO ERROR-TOTAL  ELSE                          NC2324.2
030300         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      NC2324.2
030400     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   NC2324.2
030500     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2324.2
030600      IF   INSPECT-COUNTER EQUAL TO ZERO                          NC2324.2
030700          MOVE "NO " TO ERROR-TOTAL                               NC2324.2
030800      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   NC2324.2
030900      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            NC2324.2
031000      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          NC2324.2
031100     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2324.2
031200 WRITE-LINE.                                                      NC2324.2
031300     ADD 1 TO RECORD-COUNT.                                       NC2324.2
031400*    IF RECORD-COUNT GREATER 50                                   NC2324.2
031500*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          NC2324.2
031600*        MOVE SPACE TO DUMMY-RECORD                               NC2324.2
031700*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  NC2324.2
031800*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             NC2324.2
031900*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     NC2324.2
032000*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          NC2324.2
032100*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          NC2324.2
032200*        MOVE ZERO TO RECORD-COUNT.                               NC2324.2
032300     PERFORM WRT-LN.                                              NC2324.2
032400 WRT-LN.                                                          NC2324.2
032500     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               NC2324.2
032600     MOVE SPACE TO DUMMY-RECORD.                                  NC2324.2
032700 BLANK-LINE-PRINT.                                                NC2324.2
032800     PERFORM WRT-LN.                                              NC2324.2
032900 FAIL-ROUTINE.                                                    NC2324.2
033000     IF   COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE. NC2324.2
033100     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.NC2324.2
033200     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC2324.2
033300     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   NC2324.2
033400     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2324.2
033500     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC2324.2
033600     GO TO  FAIL-ROUTINE-EX.                                      NC2324.2
033700 FAIL-ROUTINE-WRITE.                                              NC2324.2
033800     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         NC2324.2
033900     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 NC2324.2
034000     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. NC2324.2
034100     MOVE   SPACES TO COR-ANSI-REFERENCE.                         NC2324.2
034200 FAIL-ROUTINE-EX. EXIT.                                           NC2324.2
034300 BAIL-OUT.                                                        NC2324.2
034400     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   NC2324.2
034500     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           NC2324.2
034600 BAIL-OUT-WRITE.                                                  NC2324.2
034700     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  NC2324.2
034800     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC2324.2
034900     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2324.2
035000     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC2324.2
035100 BAIL-OUT-EX. EXIT.                                               NC2324.2
035200 CCVS1-EXIT.                                                      NC2324.2
035300     EXIT.                                                        NC2324.2
035400 SECT-NC232A-001 SECTION.                                         NC2324.2
035500 TH-03-001.                                                       NC2324.2
035600*                                                                 NC2324.2
035700 BUILD-LEVEL-1.                                                   NC2324.2
035800     ADD 1 TO SUB-1.                                              NC2324.2
035900     IF SUB-1 = 11 GO TO CHECK-ENTRIES.                           NC2324.2
036000     MOVE GRP-NAME TO ENTRY-1 (SUB-1).                            NC2324.2
036100     ADD 1 TO ADD-GRP.                                            NC2324.2
036200                                                                  NC2324.2
036300 BUILD-LEVEL-2.                                                   NC2324.2
036400     ADD 1 TO SUB-2.                                              NC2324.2
036500     IF SUB-2 = 11                                                NC2324.2
036600         MOVE ZERO TO SUB-2                                       NC2324.2
036700         MOVE 01 TO ADD-SEC                                       NC2324.2
036800         GO TO BUILD-LEVEL-1.                                     NC2324.2
036900     MOVE SUB-1 TO SEC-GRP.                                       NC2324.2
037000     MOVE SEC-NAME TO ENTRY-2 (SUB-1, SUB-2).                     NC2324.2
037100     ADD 1 TO ADD-SEC.                                            NC2324.2
037200                                                                  NC2324.2
037300 BUILD-LEVEL-3.                                                   NC2324.2
037400     ADD 1 TO SUB-3.                                              NC2324.2
037500     IF SUB-3 = 11                                                NC2324.2
037600         MOVE ZERO TO SUB-3                                       NC2324.2
037700              MOVE 01 TO ADD-ELEM                                 NC2324.2
037800              GO TO BUILD-LEVEL-2.                                NC2324.2
037900     MOVE SUB-1 TO ELEM-GRP.                                      NC2324.2
038000     MOVE SUB-2 TO ELEM-SEC.                                      NC2324.2
038100     MOVE ELEM-NAME TO ENTRY-3 (SUB-1, SUB-2, SUB-3).             NC2324.2
038200     ADD 1 TO ADD-ELEM.                                           NC2324.2
038300     GO TO BUILD-LEVEL-3.                                         NC2324.2
038400                                                                  NC2324.2
038500 CHECK-ENTRIES.                                                   NC2324.2
038600     MOVE "SEARCH VARYING LEV 1" TO FEATURE.                      NC2324.2
038700     MOVE "CHECK-ENTRIES       " TO PAR-NAME.                     NC2324.2
038800     MOVE "GRP02" TO GRP-HOLD-AREA.                               NC2324.2
038900     MOVE 02 TO SUB-2.                                            NC2324.2
039000     SET IDX-1 TO 1.                                              NC2324.2
039100     SEARCH GRP-ENTRY VARYING IDX-1 AT END                        NC2324.2
039200         PERFORM GRP-FAIL-PARGRAPH                                NC2324.2
039300         GO TO TH1-TEST-F1-2                                      NC2324.2
039400         WHEN ENTRY-1 (IDX-1) = GRP-HOLD-AREA NEXT SENTENCE.      NC2324.2
039500     PERFORM PASS                                                 NC2324.2
039600     PERFORM PRINT-DETAIL.                                        NC2324.2
039700     GO TO TH1-TEST-F1-2.                                         NC2324.2
039800                                                                  NC2324.2
039900 GRP-FAIL-PARGRAPH.                                               NC2324.2
040000     MOVE GRP-HOLD-AREA TO CORRECT-A.                             NC2324.2
040100     IF ENTRY-1 (SUB-2) EQUAL TO GRP-HOLD-AREA                    NC2324.2
040200         MOVE "IDX-1" TO END-IDX                                  NC2324.2
040300         SET IDX-VALU TO IDX-1                                    NC2324.2
040400         MOVE "SEE NOTE 1 FOR DIAGNOSTIC  " TO RE-MARK            NC2324.2
040500         MOVE END-STMT TO COMPUTED-A  ELSE                        NC2324.2
040600     MOVE ENTRY-1 (SUB-2) TO COMPUTED-A                           NC2324.2
040700     MOVE "SEE NOTE 2 FOR DIAGNOSTIC  " TO RE-MARK.               NC2324.2
040800                                                                  NC2324.2
040900     PERFORM FAIL                                                 NC2324.2
041000     PERFORM PRINT-DETAIL.                                        NC2324.2
041100*                                                                 NC2324.2
041200 TH1-INIT-F1-2.                                                   NC2324.2
041300     MOVE "TH1-TEST-F1-2      " TO PAR-NAME.                      NC2324.2
041400     MOVE "VI-122 6.21"   TO ANSI-REFERENCE.                      NC2324.2
041500     MOVE "GRP01" TO GRP-HOLD-AREA.                               NC2324.2
041600     MOVE 01 TO SUB-2.                                            NC2324.2
041700     SET IDX-1 TO 1.                                              NC2324.2
041800 TH1-TEST-F1-2.                                                   NC2324.2
041900     SEARCH GRP-ENTRY VARYING IDX-1 AT END                        NC2324.2
042000         GO TO TH1-FAIL-F1-2                                      NC2324.2
042100         WHEN ENTRY-1 (IDX-1) = GRP-HOLD-AREA NEXT SENTENCE.      NC2324.2
042200                                                                  NC2324.2
042300     PERFORM PASS                                                 NC2324.2
042400     GO TO TH1-WRITE-F1-2.                                        NC2324.2
042500 TH1-DELETE-F1-2.                                                 NC2324.2
042600     PERFORM DE-LETE.                                             NC2324.2
042700     GO TO TH1-WRITE-F1-2.                                        NC2324.2
042800 TH1-FAIL-F1-2.                                                   NC2324.2
042900     PERFORM FAIL.                                                NC2324.2
043000 TH1-WRITE-F1-2.                                                  NC2324.2
043100     PERFORM PRINT-DETAIL.                                        NC2324.2
043200*                                                                 NC2324.2
043300 TH1-INIT-F1-3.                                                   NC2324.2
043400     MOVE "TH1-TEST-F1-3      " TO PAR-NAME.                      NC2324.2
043500     MOVE "VI-122 6.21"   TO ANSI-REFERENCE.                      NC2324.2
043600     MOVE "GRP10" TO GRP-HOLD-AREA.                               NC2324.2
043700     MOVE 10 TO SUB-2.                                            NC2324.2
043800     SET IDX-1 TO 1.                                              NC2324.2
043900 TH1-TEST-F1-3.                                                   NC2324.2
044000     SEARCH GRP-ENTRY VARYING IDX-1 AT END                        NC2324.2
044100         GO TO TH1-FAIL-F1-3                                      NC2324.2
044200         WHEN ENTRY-1 (IDX-1) = GRP-HOLD-AREA NEXT SENTENCE.      NC2324.2
044300                                                                  NC2324.2
044400     PERFORM PASS                                                 NC2324.2
044500     GO TO TH1-WRITE-F1-3.                                        NC2324.2
044600 TH1-DELETE-F1-3.                                                 NC2324.2
044700     PERFORM DE-LETE.                                             NC2324.2
044800     GO TO TH1-WRITE-F1-3.                                        NC2324.2
044900 TH1-FAIL-F1-3.                                                   NC2324.2
045000     PERFORM FAIL.                                                NC2324.2
045100 TH1-WRITE-F1-3.                                                  NC2324.2
045200     PERFORM PRINT-DETAIL.                                        NC2324.2
045300*                                                                 NC2324.2
045400 TH1-INIT-F1-4.                                                   NC2324.2
045500     MOVE "TH1-TEST-F1-4      " TO PAR-NAME.                      NC2324.2
045600     MOVE "VI-122 6.21"   TO ANSI-REFERENCE.                      NC2324.2
045700     MOVE "GRP05" TO GRP-HOLD-AREA.                               NC2324.2
045800     MOVE 05 TO SUB-2.                                            NC2324.2
045900     SET IDX-1 TO 05.                                             NC2324.2
046000 TH1-TEST-F1-4.                                                   NC2324.2
046100     SEARCH GRP-ENTRY VARYING IDX-1                               NC2324.2
046200         WHEN ENTRY-1 (IDX-1) = GRP-HOLD-AREA                     NC2324.2
046300             GO TO PASS-TH1-F1-4.                                 NC2324.2
046400     GO TO TH1-FAIL-F1-4.                                         NC2324.2
046500 PASS-TH1-F1-4.                                                   NC2324.2
046600                                                                  NC2324.2
046700     PERFORM PASS                                                 NC2324.2
046800     GO TO TH1-WRITE-F1-4.                                        NC2324.2
046900 TH1-DELETE-F1-4.                                                 NC2324.2
047000     PERFORM DE-LETE.                                             NC2324.2
047100     GO TO TH1-WRITE-F1-4.                                        NC2324.2
047200 TH1-FAIL-F1-4.                                                   NC2324.2
047300     PERFORM GRP-FAIL-PARGRAPH.                                   NC2324.2
047400 TH1-WRITE-F1-4.                                                  NC2324.2
047500     PERFORM PRINT-DETAIL.                                        NC2324.2
047600*                                                                 NC2324.2
047700 TH2-INIT-F1-1.                                                   NC2324.2
047800     MOVE "TH2-TEST-F1-1      " TO PAR-NAME.                      NC2324.2
047900     MOVE "VI-122 6.21"   TO ANSI-REFERENCE.                      NC2324.2
048000     MOVE "SEARCH VARYING LEV 2" TO FEATURE.                      NC2324.2
048100     MOVE "SEC (01,01)" TO SEC-HOLD-AREA.                         NC2324.2
048200     MOVE 1 TO SUB-1 SUB-2.                                       NC2324.2
048300     SET IDX-1 IDX-2 TO 1.                                        NC2324.2
048400 TH2-TEST-F1-1.                                                   NC2324.2
048500     SEARCH GRP2-ENTRY VARYING IDX-2 AT END                       NC2324.2
048600         GO TO TH2-FAIL-F1-1                                      NC2324.2
048700         WHEN ENTRY-2 (IDX-1, IDX-2) = SEC-HOLD-AREA              NC2324.2
048800             NEXT SENTENCE.                                       NC2324.2
048900                                                                  NC2324.2
049000     PERFORM PASS.                                                NC2324.2
049100     GO TO TH2-WRITE-F1-1.                                        NC2324.2
049200 TH2-DELETE-F1-1.                                                 NC2324.2
049300     PERFORM DE-LETE.                                             NC2324.2
049400     GO TO TH2-WRITE-F1-1.                                        NC2324.2
049500 TH2-FAIL-F1-1.                                                   NC2324.2
049600     PERFORM SEC-FAIL-PARGRAF.                                    NC2324.2
049700 TH2-WRITE-F1-1.                                                  NC2324.2
049800     PERFORM PRINT-DETAIL.                                        NC2324.2
049900                                                                  NC2324.2
050000 TH2-INIT-F1-2.                                                   NC2324.2
050100     MOVE "TH2-TEST-F1-2      " TO PAR-NAME.                      NC2324.2
050200     MOVE "VI-122 6.21"   TO ANSI-REFERENCE.                      NC2324.2
050300     MOVE "SEARCH VARYING LEV 2" TO FEATURE.                      NC2324.2
050400     MOVE "SEC (05,10)" TO SEC-HOLD-AREA.                         NC2324.2
050500     MOVE 05 TO SUB-1.                                            NC2324.2
050600     MOVE 10 TO SUB-2.                                            NC2324.2
050700     SET IDX-1 TO 5.                                              NC2324.2
050800     SET IDX-2 TO 1.                                              NC2324.2
050900 TH2-TEST-F1-2.                                                   NC2324.2
051000     SEARCH GRP2-ENTRY VARYING IDX-2 AT END                       NC2324.2
051100         GO TO TH2-FAIL-F1-2                                      NC2324.2
051200         WHEN ENTRY-2 (IDX-1, IDX-2) = SEC-HOLD-AREA              NC2324.2
051300             NEXT SENTENCE.                                       NC2324.2
051400                                                                  NC2324.2
051500     PERFORM PASS                                                 NC2324.2
051600     GO TO TH2-WRITE-F1-2.                                        NC2324.2
051700 TH2-DELETE-F1-2.                                                 NC2324.2
051800     PERFORM DE-LETE.                                             NC2324.2
051900     GO TO TH2-WRITE-F1-2.                                        NC2324.2
052000 TH2-FAIL-F1-2.                                                   NC2324.2
052100     PERFORM SEC-FAIL-PARGRAF.                                    NC2324.2
052200 TH2-WRITE-F1-2.                                                  NC2324.2
052300     PERFORM PRINT-DETAIL.                                        NC2324.2
052400*                                                                 NC2324.2
052500 TH2-INIT-F1-3.                                                   NC2324.2
052600     MOVE "TH2-TEST-F1-3      " TO PAR-NAME.                      NC2324.2
052700     MOVE "VI-122 6.21"   TO ANSI-REFERENCE.                      NC2324.2
052800     MOVE "SEARCH VARYING LEV 2" TO FEATURE.                      NC2324.2
052900     MOVE "SEC (10,10)" TO SEC-HOLD-AREA.                         NC2324.2
053000     SET IDX-1 TO 10.                                             NC2324.2
053100     SET IDX-2 TO 1.                                              NC2324.2
053200     MOVE 10 TO SUB-1  SUB-2.                                     NC2324.2
053300 TH2-TEST-F1-3.                                                   NC2324.2
053400     SEARCH GRP2-ENTRY VARYING IDX-2 AT END                       NC2324.2
053500         GO TO TH2-FAIL-F1-3                                      NC2324.2
053600         WHEN ENTRY-2 (IDX-1, IDX-2) = SEC-HOLD-AREA              NC2324.2
053700             NEXT SENTENCE.                                       NC2324.2
053800                                                                  NC2324.2
053900     PERFORM PASS                                                 NC2324.2
054000     GO TO TH2-WRITE-F1-3.                                        NC2324.2
054100 TH2-DELETE-F1-3.                                                 NC2324.2
054200     PERFORM DE-LETE.                                             NC2324.2
054300     GO TO TH2-WRITE-F1-3.                                        NC2324.2
054400 TH2-FAIL-F1-3.                                                   NC2324.2
054500     PERFORM SEC-FAIL-PARGRAF.                                    NC2324.2
054600 TH2-WRITE-F1-3.                                                  NC2324.2
054700     PERFORM PRINT-DETAIL.                                        NC2324.2
054800*                                                                 NC2324.2
054900 TH2-INIT-F1-4.                                                   NC2324.2
055000     MOVE "TH2-TEST-F1-4      " TO PAR-NAME.                      NC2324.2
055100     MOVE "VI-122 6.21"   TO ANSI-REFERENCE.                      NC2324.2
055200     MOVE "SEARCH VARYING LEV 2" TO FEATURE.                      NC2324.2
055300     MOVE "SEC (08,02)" TO SEC-HOLD-AREA.                         NC2324.2
055400     MOVE 08 TO SUB-1.                                            NC2324.2
055500     MOVE 02 TO SUB-2.                                            NC2324.2
055600     SET IDX-1 TO 08.                                             NC2324.2
055700     SET IDX-2 TO 01.                                             NC2324.2
055800 TH2-TEST-F1-4.                                                   NC2324.2
055900     SEARCH GRP2-ENTRY VARYING IDX-2                              NC2324.2
056000         WHEN ENTRY-2 (IDX-1, IDX-2) = SEC-HOLD-AREA              NC2324.2
056100             PERFORM PASS                                         NC2324.2
056200             GO TO TH2-WRITE-F1-4.                                NC2324.2
056300     GO TO TH2-FAIL-F1-4.                                         NC2324.2
056400 TH2-DELETE-F1-4.                                                 NC2324.2
056500     PERFORM DE-LETE.                                             NC2324.2
056600     GO TO TH2-WRITE-F1-4.                                        NC2324.2
056700 TH2-FAIL-F1-4.                                                   NC2324.2
056800     PERFORM SEC-FAIL-PARGRAF.                                    NC2324.2
056900 TH2-WRITE-F1-4.                                                  NC2324.2
057000     PERFORM PRINT-DETAIL.                                        NC2324.2
057100     GO TO TH3-INIT-F1-1.                                         NC2324.2
057200*                                                                 NC2324.2
057300 SEC-FAIL-PARGRAF.                                                NC2324.2
057400     MOVE SEC-HOLD-AREA TO CORRECT-A.                             NC2324.2
057500     IF ENTRY-2 (SUB-1, SUB-2) EQUAL TO SEC-HOLD-AREA             NC2324.2
057600         MOVE "IDX-2" TO END-IDX                                  NC2324.2
057700         SET IDX-VALU TO IDX-2                                    NC2324.2
057800         MOVE "SEE NOTE 1 FOR DIAGNOSTIC  " TO RE-MARK            NC2324.2
057900         MOVE END-STMT TO COMPUTED-A ELSE                         NC2324.2
058000     MOVE ENTRY-2 (SUB-1, SUB-2) TO COMPUTED-A                    NC2324.2
058100     MOVE "SEE NOTE 2 FOR DIAGNOSTIC  " TO RE-MARK.               NC2324.2
058200                                                                  NC2324.2
058300     PERFORM FAIL.                                                NC2324.2
058400                                                                  NC2324.2
058500 TH3-INIT-F1-1.                                                   NC2324.2
058600     MOVE "TH3-TEST-F1-1      " TO PAR-NAME.                      NC2324.2
058700     MOVE "VI-122 6.21"   TO ANSI-REFERENCE.                      NC2324.2
058800     MOVE "SEARCH VARYING LEV 3" TO FEATURE.                      NC2324.2
058900     MOVE 1 TO SUB-1  SUB-2  SUB-3.                               NC2324.2
059000     MOVE "ELEM (01,01,01)" TO ELEM-HOLD-AREA.                    NC2324.2
059100     SET IDX-1 IDX-2 IDX-3 TO 1.                                  NC2324.2
059200 TH3-TEST-F1-1.                                                   NC2324.2
059300     SEARCH GRP3-ENTRY VARYING IDX-3                              NC2324.2
059400         WHEN ENTRY-3 (IDX-1, IDX-2, IDX-3) = ELEM-HOLD-AREA      NC2324.2
059500             GO TO PASS-TH3-1.                                    NC2324.2
059600     GO TO TH3-FAIL-F1-1.                                         NC2324.2
059700 PASS-TH3-1.                                                      NC2324.2
059800                                                                  NC2324.2
059900     PERFORM PASS.                                                NC2324.2
060000     GO TO TH3-WRITE-F1-1.                                        NC2324.2
060100 TH3-DELETE-F1-1.                                                 NC2324.2
060200     PERFORM DE-LETE.                                             NC2324.2
060300     GO TO TH3-WRITE-F1-1.                                        NC2324.2
060400 TH3-FAIL-F1-1.                                                   NC2324.2
060500     PERFORM ELEM-FAIL-PARA.                                      NC2324.2
060600 TH3-WRITE-F1-1.                                                  NC2324.2
060700     PERFORM PRINT-DETAIL.                                        NC2324.2
060800*                                                                 NC2324.2
060900 TH3-INIT-F1-2.                                                   NC2324.2
061000     MOVE "TH3-TEST-F1-2      " TO PAR-NAME.                      NC2324.2
061100     MOVE "VI-122 6.21"   TO ANSI-REFERENCE.                      NC2324.2
061200     MOVE "SEARCH VARYING LEV 3" TO FEATURE.                      NC2324.2
061300     MOVE 05 TO SUB-1.                                            NC2324.2
061400     MOVE 06 TO SUB-2.                                            NC2324.2
061500     MOVE 07 TO SUB-3.                                            NC2324.2
061600     SET IDX-1 TO 05.                                             NC2324.2
061700     SET IDX-2 TO 06.                                             NC2324.2
061800     SET IDX-3 TO 1.                                              NC2324.2
061900     MOVE "ELEM (05,06,07)" TO ELEM-HOLD-AREA.                    NC2324.2
062000 TH3-TEST-F1-2.                                                   NC2324.2
062100     SEARCH GRP3-ENTRY VARYING IDX-3 AT END                       NC2324.2
062200         GO TO TH3-FAIL-F1-2                                      NC2324.2
062300         WHEN ENTRY-3 (IDX-1, IDX-2, IDX-3) = ELEM-HOLD-AREA      NC2324.2
062400             NEXT SENTENCE.                                       NC2324.2
062500                                                                  NC2324.2
062600     PERFORM PASS                                                 NC2324.2
062700     GO TO TH3-WRITE-F1-2.                                        NC2324.2
062800 TH3-DELETE-F1-2.                                                 NC2324.2
062900     PERFORM DE-LETE.                                             NC2324.2
063000     GO TO TH3-WRITE-F1-2.                                        NC2324.2
063100 TH3-FAIL-F1-2.                                                   NC2324.2
063200     PERFORM ELEM-FAIL-PARA.                                      NC2324.2
063300 TH3-WRITE-F1-2.                                                  NC2324.2
063400     PERFORM PRINT-DETAIL.                                        NC2324.2
063500*                                                                 NC2324.2
063600 TH3-INIT-F1-3.                                                   NC2324.2
063700     MOVE "TH3-TEST-F1-3      " TO PAR-NAME.                      NC2324.2
063800     MOVE "VI-122 6.21"   TO ANSI-REFERENCE.                      NC2324.2
063900     MOVE "SEARCH VARYING LEV 3" TO FEATURE.                      NC2324.2
064000     MOVE 10 TO SUB-1 SUB-2 SUB-3.                                NC2324.2
064100     SET IDX-1 IDX-2 TO 10.                                       NC2324.2
064200     SET IDX-3 TO 1.                                              NC2324.2
064300 TH3-TEST-F1-3.                                                   NC2324.2
064400     MOVE "ELEM (10,10,10)" TO ELEM-HOLD-AREA.                    NC2324.2
064500     SEARCH GRP3-ENTRY VARYING IDX-3 AT END                       NC2324.2
064600         GO TO TH3-FAIL-F1-3                                      NC2324.2
064700         WHEN ENTRY-3 (IDX-1, IDX-2, IDX-3) = ELEM-HOLD-AREA      NC2324.2
064800             NEXT SENTENCE.                                       NC2324.2
064900                                                                  NC2324.2
065000     PERFORM PASS                                                 NC2324.2
065100     GO TO TH3-WRITE-F1-3.                                        NC2324.2
065200 TH3-DELETE-F1-3.                                                 NC2324.2
065300     PERFORM DE-LETE.                                             NC2324.2
065400     GO TO TH3-WRITE-F1-3.                                        NC2324.2
065500 TH3-FAIL-F1-3.                                                   NC2324.2
065600     PERFORM ELEM-FAIL-PARA.                                      NC2324.2
065700 TH3-WRITE-F1-3.                                                  NC2324.2
065800     PERFORM PRINT-DETAIL.                                        NC2324.2
065900*                                                                 NC2324.2
066000 TH3-INIT-F1-4.                                                   NC2324.2
066100     MOVE "TH3-TEST-F1-4      " TO PAR-NAME.                      NC2324.2
066200     MOVE "VI-122 6.21"   TO ANSI-REFERENCE.                      NC2324.2
066300     MOVE "SEARCH VARYING LEV 3" TO FEATURE.                      NC2324.2
066400     MOVE "ELEM (07,06,05)" TO ELEM-HOLD-AREA.                    NC2324.2
066500     MOVE 07 TO SUB-1.                                            NC2324.2
066600     MOVE 06 TO SUB-2.                                            NC2324.2
066700     MOVE 05 TO SUB-3.                                            NC2324.2
066800     SET IDX-1 TO 07.                                             NC2324.2
066900     SET IDX-2 TO 06.                                             NC2324.2
067000     SET IDX-3 TO 03.                                             NC2324.2
067100 TH3-TEST-F1-4.                                                   NC2324.2
067200     SEARCH GRP3-ENTRY VARYING IDX-3 AT END                       NC2324.2
067300         GO TO TH3-TEST-F1-4                                      NC2324.2
067400         WHEN ENTRY-3 (IDX-1, IDX-2, IDX-3) = ELEM-HOLD-AREA      NC2324.2
067500         NEXT SENTENCE.                                           NC2324.2
067600                                                                  NC2324.2
067700     PERFORM PASS                                                 NC2324.2
067800     GO TO TH3-WRITE-F1-4.                                        NC2324.2
067900 TH3-DELETE-F1-4.                                                 NC2324.2
068000     PERFORM DE-LETE.                                             NC2324.2
068100     GO TO TH3-WRITE-F1-4.                                        NC2324.2
068200 TH3-FAIL-F1-4.                                                   NC2324.2
068300     PERFORM ELEM-FAIL-PARA.                                      NC2324.2
068400 TH3-WRITE-F1-4.                                                  NC2324.2
068500     PERFORM PRINT-DETAIL.                                        NC2324.2
068600     GO TO SCH-INIT-F1-1.                                         NC2324.2
068700*                                                                 NC2324.2
068800 ELEM-FAIL-PARA.                                                  NC2324.2
068900     MOVE ELEM-HOLD-AREA TO CORRECT-A.                            NC2324.2
069000     IF ENTRY-3 (SUB-1, SUB-2, SUB-3) EQUAL TO ELEM-HOLD-AREA     NC2324.2
069100         MOVE "IDX-3" TO END-IDX                                  NC2324.2
069200         SET IDX-VALU TO IDX-3                                    NC2324.2
069300         MOVE "SEE NOTE 1 FOR DIAGNOSTIC  " TO RE-MARK            NC2324.2
069400         MOVE END-STMT TO COMPUTED-A  ELSE                        NC2324.2
069500     MOVE ENTRY-3 (SUB-1, SUB-2, SUB-3) TO COMPUTED-A             NC2324.2
069600     MOVE "SEE NOTE 2 FOR DIAGNOSTIC  " TO RE-MARK.               NC2324.2
069700     PERFORM FAIL.                                                NC2324.2
069800*                                                                 NC2324.2
069900 SCH-INIT-F1-1.                                                   NC2324.2
070000     MOVE "SCH-TEST-F1-1  " TO PAR-NAME.                          NC2324.2
070100     MOVE "VI-122 6.21"   TO ANSI-REFERENCE.                      NC2324.2
070200     MOVE "MULTIPLE SEARCH STMT" TO FEATURE.                      NC2324.2
070300     MOVE "GRP08" TO GRP-HOLD-AREA.                               NC2324.2
070400     MOVE "SEC (08,07)" TO SEC-HOLD-AREA.                         NC2324.2
070500     SET IDX-1 IDX-2 TO 1.                                        NC2324.2
070600 SCH-TEST-F1-1.                                                   NC2324.2
070700     SEARCH GRP-ENTRY VARYING IDX-1 AT END GO TO SCH-FAIL-F1-1-A  NC2324.2
070800         WHEN ENTRY-1 (IDX-1) = "GRP08" NEXT SENTENCE.            NC2324.2
070900     SEARCH GRP2-ENTRY VARYING IDX-2 AT END GO TO SCH-FAIL-F1-1-B NC2324.2
071000         WHEN ENTRY-2 (IDX-1, IDX-2) = SEC-HOLD-AREA              NC2324.2
071100             NEXT SENTENCE.                                       NC2324.2
071200     PERFORM PASS                                                 NC2324.2
071300     GO TO SCH-WRITE-F1-1.                                        NC2324.2
071400 SCH-DELETE-F1-1.                                                 NC2324.2
071500     PERFORM DE-LETE.                                             NC2324.2
071600     GO TO SCH-WRITE-F1-1.                                        NC2324.2
071700 SCH-FAIL-F1-1-A.                                                 NC2324.2
071800     MOVE GRP-HOLD-AREA TO CORRECT-A.                             NC2324.2
071900     IF ENTRY-1 (08) EQUAL TO GRP-HOLD-AREA                       NC2324.2
072000         MOVE "IDX-1" TO END-IDX                                  NC2324.2
072100         MOVE "SEE NOTE 1 FOR DIAGNOSTIC  " TO RE-MARK            NC2324.2
072200         SET IDX-VALU TO IDX-1                                    NC2324.2
072300         MOVE END-STMT TO COMPUTED-A  ELSE                        NC2324.2
072400     MOVE ENTRY-1 (08) TO COMPUTED-A                              NC2324.2
072500     MOVE "SEE NOTE 2 FOR DIAGNOSTIC  " TO RE-MARK.               NC2324.2
072600                                                                  NC2324.2
072700     PERFORM FAIL                                                 NC2324.2
072800     GO TO SCH-WRITE-F1-1.                                        NC2324.2
072900 SCH-FAIL-F1-1-B.                                                 NC2324.2
073000     MOVE SEC-HOLD-AREA TO CORRECT-A.                             NC2324.2
073100     IF ENTRY-2 (08, 07) EQUAL TO SEC-HOLD-AREA                   NC2324.2
073200         MOVE "IDX-2" TO END-IDX                                  NC2324.2
073300         SET IDX-VALU TO IDX-2                                    NC2324.2
073400         MOVE "SEE NOTE 1 FOR DIAGNOSTIC  " TO RE-MARK            NC2324.2
073500         MOVE END-STMT TO COMPUTED-A  ELSE                        NC2324.2
073600     MOVE ENTRY-2 (08, 07) TO COMPUTED-A                          NC2324.2
073700     MOVE "SEE NOTE 2 FOR DIAGNOSTIC  " TO RE-MARK.               NC2324.2
073800                                                                  NC2324.2
073900     PERFORM FAIL.                                                NC2324.2
074000 SCH-WRITE-F1-1.                                                  NC2324.2
074100     PERFORM PRINT-DETAIL.                                        NC2324.2
074200*                                                                 NC2324.2
074300 SCH-INIT-F1-2.                                                   NC2324.2
074400     MOVE "SCH-TEST-F1-2  " TO PAR-NAME.                          NC2324.2
074500     MOVE "VI-122 6.21"   TO ANSI-REFERENCE.                      NC2324.2
074600     MOVE "MULTIPLE SEARCH STMT" TO FEATURE.                      NC2324.2
074700     MOVE "GRP04" TO GRP-HOLD-AREA.                               NC2324.2
074800     MOVE "SEC (04,04)" TO SEC-HOLD-AREA.                         NC2324.2
074900     MOVE "ELEM (04,04,04)" TO ELEM-HOLD-AREA.                    NC2324.2
075000     SET IDX-1 IDX-2 IDX-3 TO 1.                                  NC2324.2
075100 SCH-TEST-F1-2.                                                   NC2324.2
075200     SEARCH GRP-ENTRY VARYING IDX-1                               NC2324.2
075300         AT END                                                   NC2324.2
075400              GO TO SCH-FAIL-F1-2-A                               NC2324.2
075500         WHEN ENTRY-1 (IDX-1) = GRP-HOLD-AREA                     NC2324.2
075600              NEXT SENTENCE.                                      NC2324.2
075700*                                                                 NC2324.2
075800     SEARCH GRP2-ENTRY VARYING IDX-2                              NC2324.2
075900         AT END                                                   NC2324.2
076000              GO TO SCH-FAIL-F1-2-B                               NC2324.2
076100         WHEN ENTRY-2 (IDX-1, IDX-2) = SEC-HOLD-AREA              NC2324.2
076200              NEXT SENTENCE.                                      NC2324.2
076300*                                                                 NC2324.2
076400     SEARCH GRP3-ENTRY VARYING IDX-3                              NC2324.2
076500         AT END                                                   NC2324.2
076600              GO TO SCH-FAIL-F1-2-C                               NC2324.2
076700         WHEN ENTRY-3 (IDX-1, IDX-2, IDX-3) = ELEM-HOLD-AREA      NC2324.2
076800              NEXT SENTENCE.                                      NC2324.2
076900                                                                  NC2324.2
077000     PERFORM PASS                                                 NC2324.2
077100     GO TO SCH-WRITE-F1-2.                                        NC2324.2
077200 SCH-FAIL-F1-2-A.                                                 NC2324.2
077300     MOVE GRP-HOLD-AREA TO CORRECT-A.                             NC2324.2
077400     IF ENTRY-1 (04) EQUAL TO GRP-HOLD-AREA                       NC2324.2
077500         MOVE "IDX-1" TO END-IDX                                  NC2324.2
077600         MOVE "SEE NOTE 1 FOR DIAGNOSTIC  " TO RE-MARK            NC2324.2
077700         SET IDX-VALU TO IDX-1                                    NC2324.2
077800         MOVE END-STMT TO COMPUTED-A                              NC2324.2
077900     ELSE                                                         NC2324.2
078000         MOVE ENTRY-1 (04) TO COMPUTED-A                          NC2324.2
078100         MOVE "SEE NOTE 2 FOR DIAGNOSTIC  " TO RE-MARK.           NC2324.2
078200     PERFORM FAIL.                                                NC2324.2
078300     GO TO SCH-WRITE-F1-2.                                        NC2324.2
078400 SCH-FAIL-F1-2-B.                                                 NC2324.2
078500     MOVE  SEC-HOLD-AREA TO CORRECT-A.                            NC2324.2
078600     IF ENTRY-2 (04, 04) EQUAL TO SEC-HOLD-AREA                   NC2324.2
078700         MOVE "IDX-2" TO END-IDX                                  NC2324.2
078800         MOVE "SEE NOTE 1 FOR DIAGNOSTIC  " TO RE-MARK            NC2324.2
078900         SET IDX-VALU TO IDX-2                                    NC2324.2
079000         MOVE END-STMT TO COMPUTED-A                              NC2324.2
079100     ELSE                                                         NC2324.2
079200         MOVE ENTRY-2 (04, 04) TO COMPUTED-A                      NC2324.2
079300         MOVE "SEE NOTE 2 FOR DIAGNOSTIC  " TO RE-MARK.           NC2324.2
079400     PERFORM FAIL                                                 NC2324.2
079500     GO TO SCH-WRITE-F1-2.                                        NC2324.2
079600 SCH-FAIL-F1-2-C.                                                 NC2324.2
079700     MOVE ELEM-HOLD-AREA TO CORRECT-A.                            NC2324.2
079800     IF ENTRY-3 (04, 04, 04) EQUAL TO ELEM-HOLD-AREA              NC2324.2
079900         MOVE "IDX-3" TO END-IDX                                  NC2324.2
080000         MOVE "SEE NOTE 1 FOR DIAGNOSTIC  " TO RE-MARK            NC2324.2
080100         SET IDX-VALU TO IDX-3                                    NC2324.2
080200         MOVE END-STMT TO COMPUTED-A                              NC2324.2
080300     ELSE                                                         NC2324.2
080400         MOVE ENTRY-3 (04, 04, 04) TO COMPUTED-A                  NC2324.2
080500         MOVE "SEE NOTE 2 FOR DIAGNOSTIC  " TO RE-MARK.           NC2324.2
080600     PERFORM FAIL.                                                NC2324.2
080700 SCH-WRITE-F1-2.                                                  NC2324.2
080800     PERFORM PRINT-DETAIL.                                        NC2324.2
080900*                                                                 NC2324.2
081000 SPC-INIT-F1-1.                                                   NC2324.2
081100     MOVE "SPC-TEST-F1-1      " TO PAR-NAME.                      NC2324.2
081200     MOVE "VI-122 6.21"   TO ANSI-REFERENCE.                      NC2324.2
081300     MOVE "SEARCH WITH HI INDEX" TO FEATURE.                      NC2324.2
081400     SET IDX-1 TO 4.                                              NC2324.2
081500 SPC-TEST-F1-1.                                                   NC2324.2
081600     SEARCH GRP-ENTRY VARYING IDX-1                               NC2324.2
081700         AT END                                                   NC2324.2
081800              PERFORM PASS                                        NC2324.2
081900              GO TO SPC-WRITE-F1-1                                NC2324.2
082000         WHEN ENTRY-1 (IDX-1) = "GRP03"                           NC2324.2
082100              GO TO SPC-FAIL-F1-1.                                NC2324.2
082200 SPC-DELETE-F1-1.                                                 NC2324.2
082300     PERFORM DE-LETE.                                             NC2324.2
082400     GO TO SPC-WRITE-F1-1.                                        NC2324.2
082500 SPC-FAIL-F1-1.                                                   NC2324.2
082600     MOVE SPACES TO CORRECT-A.                                    NC2324.2
082700     MOVE ENTRY-1 (03) TO COMPUTED-A.                             NC2324.2
082800     MOVE SPACES TO RE-MARK.                                      NC2324.2
082900     PERFORM FAIL.                                                NC2324.2
083000 SPC-WRITE-F1-1.                                                  NC2324.2
083100     PERFORM PRINT-DETAIL.                                        NC2324.2
083200*                                                                 NC2324.2
083300 SPC-INIT-F1-2.                                                   NC2324.2
083400     MOVE "SPC-TEST-F1-2" TO PAR-NAME.                            NC2324.2
083500     MOVE "VI-122 6.21"   TO ANSI-REFERENCE.                      NC2324.2
083600     MOVE "SEARCH WITH HI INDEX" TO FEATURE.                      NC2324.2
083700     SET IDX-1 TO 4.                                              NC2324.2
083800     SET IDX-2 TO 5.                                              NC2324.2
083900 SPC-TEST-F1-2.                                                   NC2324.2
084000     SEARCH GRP-ENTRY VARYING IDX-1                               NC2324.2
084100         AT END GO TO SPC-FAIL-F1-2-A                             NC2324.2
084200         WHEN ENTRY-1 (IDX-1) = "GRP04" NEXT SENTENCE.            NC2324.2
084300     SEARCH GRP2-ENTRY VARYING IDX-2                              NC2324.2
084400         AT END PERFORM PASS                                      NC2324.2
084500                GO TO SPC-WRITE-F1-2                              NC2324.2
084600         WHEN ENTRY-2 (IDX-1, IDX-2) = "SEC (04,04)"              NC2324.2
084700                GO TO SPC-FAIL-F1-2-B.                            NC2324.2
084800 SPC-DELETE-F1-2.                                                 NC2324.2
084900     PERFORM DE-LETE.                                             NC2324.2
085000     GO TO SPC-WRITE-F1-2.                                        NC2324.2
085100 SPC-FAIL-F1-2-A.                                                 NC2324.2
085200     MOVE "GRP04" TO CORRECT-A.                                   NC2324.2
085300     IF ENTRY-1 (04) EQUAL TO "GRP04"                             NC2324.2
085400         MOVE "IDX-2" TO END-IDX                                  NC2324.2
085500         MOVE "SEE NOTE 1 FOR DIAGNOSTIC  " TO RE-MARK            NC2324.2
085600         SET IDX-VALU TO IDX-2                                    NC2324.2
085700         MOVE END-STMT TO COMPUTED-A  ELSE                        NC2324.2
085800     MOVE ENTRY-1 (04) TO COMPUTED-A                              NC2324.2
085900     MOVE "SEE NOTE 2 FOR DIAGNOSTIC  " TO RE-MARK.               NC2324.2
086000     PERFORM FAIL                                                 NC2324.2
086100     GO TO SPC-WRITE-F1-2.                                        NC2324.2
086200 SPC-FAIL-F1-2-B.                                                 NC2324.2
086300     MOVE ENTRY-2 (04, 04) TO COMPUTED-A                          NC2324.2
086400     MOVE SPACES TO CORRECT-A.                                    NC2324.2
086500     PERFORM FAIL.                                                NC2324.2
086600 SPC-WRITE-F1-2.                                                  NC2324.2
086700     PERFORM PRINT-DETAIL.                                        NC2324.2
086800*                                                                 NC2324.2
086900 SPC-INIT-F1-3.                                                   NC2324.2
087000     MOVE "SPC-TEST-F1-3" TO PAR-NAME.                            NC2324.2
087100     MOVE "VI-122 6.21"   TO ANSI-REFERENCE.                      NC2324.2
087200     MOVE "SEARCH WITH HI INDEX" TO FEATURE.                      NC2324.2
087300     SET IDX-1 TO 02.                                             NC2324.2
087400 SPC-TEST-F1-3.                                                   NC2324.2
087500     SEARCH GRP-ENTRY VARYING IDX-1                               NC2324.2
087600         AT END                                                   NC2324.2
087700                GO TO SPC-FAIL-F1-3-A                             NC2324.2
087800         WHEN ENTRY-1 (IDX-1) EQUAL TO "GRP02"                    NC2324.2
087900                NEXT SENTENCE.                                    NC2324.2
088000     SET IDX-2 TO 01.                                             NC2324.2
088100     SEARCH GRP2-ENTRY VARYING IDX-2                              NC2324.2
088200         AT END                                                   NC2324.2
088300                GO TO SPC-FAIL-F1-3-B                             NC2324.2
088400         WHEN ENTRY-2 (IDX-1, IDX-2) = "SEC (02,03)"              NC2324.2
088500                NEXT SENTENCE.                                    NC2324.2
088600     SET IDX-3 TO 05.                                             NC2324.2
088700     SEARCH GRP3-ENTRY VARYING IDX-3                              NC2324.2
088800         AT END PERFORM PASS                                      NC2324.2
088900                GO TO SPC-WRITE-F1-3                              NC2324.2
089000         WHEN ENTRY-3 (IDX-1, IDX-2, IDX-3) = "ELEM (02,03,04)"   NC2324.2
089100                GO TO SPC-FAIL-F1-3-C.                            NC2324.2
089200 SPC-FAIL-F1-3-A.                                                 NC2324.2
089300     MOVE "GRP02" TO CORRECT-A.                                   NC2324.2
089400     IF ENTRY-1 (02) EQUAL TO "GRP02"                             NC2324.2
089500         MOVE "IDX-1" TO END-IDX                                  NC2324.2
089600         MOVE "SEE NOTE 1 FOR DIAGNOSTIC  " TO RE-MARK            NC2324.2
089700         SET IDX-VALU TO IDX-1                                    NC2324.2
089800         MOVE END-STMT TO COMPUTED-A  ELSE                        NC2324.2
089900     MOVE ENTRY-1 (02) TO COMPUTED-A                              NC2324.2
090000     MOVE "SEE NOTE 2 FOR DIAGNOSTIC  " TO RE-MARK.               NC2324.2
090100     PERFORM FAIL                                                 NC2324.2
090200     GO TO SPC-WRITE-F1-3.                                        NC2324.2
090300 SPC-FAIL-F1-3-B.                                                 NC2324.2
090400     MOVE "SEC (02,03)" TO CORRECT-A.                             NC2324.2
090500     IF ENTRY-2 (02, 03) EQUAL TO "SEC (02,03)"                   NC2324.2
090600         MOVE "IDX-2" TO END-IDX                                  NC2324.2
090700         MOVE "SEE NOTE 1 FOR DIAGNOSTIC  " TO RE-MARK            NC2324.2
090800         SET IDX-VALU TO IDX-2                                    NC2324.2
090900         MOVE END-STMT TO COMPUTED-A  ELSE                        NC2324.2
091000     MOVE ENTRY-2 (02, 03) TO COMPUTED-A                          NC2324.2
091100     MOVE "SEE NOTE 2 FOR DIAGNOSTIC  " TO RE-MARK.               NC2324.2
091200     PERFORM FAIL                                                 NC2324.2
091300     GO TO SPC-WRITE-F1-3.                                        NC2324.2
091400 SPC-FAIL-F1-3-C.                                                 NC2324.2
091500     MOVE "INDEX SET HIGHER THAN ENTRY" TO RE-MARK                NC2324.2
091600     MOVE SPACES TO CORRECT-A                                     NC2324.2
091700     MOVE "ELEM (02,03,04)" TO COMPUTED-A                         NC2324.2
091800     PERFORM FAIL.                                                NC2324.2
091900 SPC-WRITE-F1-3.                                                  NC2324.2
092000     PERFORM PRINT-DETAIL.                                        NC2324.2
092100 CCVS-EXIT SECTION.                                               NC2324.2
092200 CCVS-999999.                                                     NC2324.2
092300     GO TO CLOSE-FILES.                                           NC2324.2
      *END-OF,NC232A                                                                  
