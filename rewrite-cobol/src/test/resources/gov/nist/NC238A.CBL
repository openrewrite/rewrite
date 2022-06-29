      *HEADER,COBOL,NC238A                                                            
000100 IDENTIFICATION DIVISION.                                         NC2384.2
000200 PROGRAM-ID.                                                      NC2384.2
000300     NC238A.                                                      NC2384.2
000400****************************************************************  NC2384.2
000500*                                                              *  NC2384.2
000600*    VALIDATION FOR:-                                          *  NC2384.2
000700*                                                              *  NC2384.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2384.2
000900*                                                              *  NC2384.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC2384.2
001100*                                                              *  NC2384.2
001200****************************************************************  NC2384.2
001300*                                                              *  NC2384.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  NC2384.2
001500*                                                              *  NC2384.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  NC2384.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  NC2384.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  NC2384.2
001900*                                                              *  NC2384.2
002000****************************************************************  NC2384.2
002100*                                                                 NC2384.2
002200*    PROGRAM NC238A TESTS FORMATS 1 AND 2 OF THE "SEARCH"      *  NC2384.2
002300*    STATEMENT USING A TWO-DIMENDIONAL TABLE WITH ASCENDING    *  NC2384.2
002400*    AND DESCENDING KEYS AND MULTIPLE INDICES.  SINGLE AND     *  NC2384.2
002500*    MULTIPLE CONDITIONS ARE USED IN THE "WHEN" PHRASE.        *  NC2384.2
002600*                                                              *  NC2384.2
002700****************************************************************  NC2384.2
002800 ENVIRONMENT DIVISION.                                            NC2384.2
002900 CONFIGURATION SECTION.                                           NC2384.2
003000 SOURCE-COMPUTER.                                                 NC2384.2
003100     XXXXX082.                                                    NC2384.2
003200 OBJECT-COMPUTER.                                                 NC2384.2
003300     XXXXX083.                                                    NC2384.2
003400 INPUT-OUTPUT SECTION.                                            NC2384.2
003500 FILE-CONTROL.                                                    NC2384.2
003600     SELECT PRINT-FILE ASSIGN TO                                  NC2384.2
003700     XXXXX055.                                                    NC2384.2
003800 DATA DIVISION.                                                   NC2384.2
003900 FILE SECTION.                                                    NC2384.2
004000 FD  PRINT-FILE.                                                  NC2384.2
004100 01  PRINT-REC PICTURE X(120).                                    NC2384.2
004200 01  DUMMY-RECORD PICTURE X(120).                                 NC2384.2
004300 WORKING-STORAGE SECTION.                                         NC2384.2
004400 77  SUB-1              PICTURE S99  VALUE ZERO.                  NC2384.2
004500 77  SUB-2              PICTURE 99   VALUE ZERO.                  NC2384.2
004600 77  SUB-3              PICTURE 99   VALUE ZERO.                  NC2384.2
004700 77  CON-7              PICTURE 99  VALUE 07.                     NC2384.2
004800 77  CON-10             PICTURE 99  VALUE 10.                     NC2384.2
004900 77  GRP-HOLD-AREA                PICTURE X(5)  VALUE SPACES.     NC2384.2
005000 77  CON-5              PICTURE 99  VALUE 05.                     NC2384.2
005100 77  SEC-HOLD-AREA                PICTURE X(11)  VALUE SPACES.    NC2384.2
005200 77  CON-6              PICTURE 99  VALUE 06.                     NC2384.2
005300 77  ELEM-HOLD-AREA               PICTURE X(15)  VALUE SPACES.    NC2384.2
005400 77  SUB-4  PICTURE 9 VALUE 9.                                    NC2384.2
005500 77  SUB-5  PICTURE 9 VALUE 1.                                    NC2384.2
005600 77  SUB-6  PICTURE 99 VALUE 01.                                  NC2384.2
005700 77  LEVEL-HOLD   PICTURE X(4) VALUE SPACES.                      NC2384.2
005800 01  ALPHA-TABLE.                                                 NC2384.2
005900     02  FILLER   PICTURE X(4)  VALUE "PPPP".                     NC2384.2
006000     02  FILLER   PICTURE X(4)  VALUE "OOOO".                     NC2384.2
006100     02  FILLER   PICTURE X(4)  VALUE "NNNN".                     NC2384.2
006200     02  FILLER   PICTURE X(4)  VALUE "MMMM".                     NC2384.2
006300     02  FILLER   PICTURE X(4)  VALUE "LLLL".                     NC2384.2
006400     02  FILLER   PICTURE X(4)  VALUE "KKKK".                     NC2384.2
006500     02  FILLER   PICTURE X(4)  VALUE "JJJJ".                     NC2384.2
006600     02  FILLER   PICTURE X(4)  VALUE "IIII".                     NC2384.2
006700     02  FILLER   PICTURE X(4)  VALUE "HHHH".                     NC2384.2
006800     02  FILLER   PICTURE X(4)  VALUE "GGGG".                     NC2384.2
006900     02  FILLER   PICTURE X(4)  VALUE "FFFF".                     NC2384.2
007000     02  FILLER   PICTURE X(4)  VALUE "EEEE".                     NC2384.2
007100     02  FILLER   PICTURE X(4)  VALUE "DDDD".                     NC2384.2
007200     02  FILLER   PICTURE X(4)  VALUE "CCCC".                     NC2384.2
007300     02  FILLER   PICTURE X(4)  VALUE "BBBB".                     NC2384.2
007400     02  FILLER   PICTURE X(4)  VALUE "AAAA".                     NC2384.2
007500 01  ALPHA-BET-TABLE REDEFINES ALPHA-TABLE.                       NC2384.2
007600     02  ALPHA-BET OCCURS 16 TIMES PICTURE X(4).                  NC2384.2
007700                                                                  NC2384.2
007800 01  SERIES-TABLE-2.                                              NC2384.2
007900     02  1ST-ENTRY OCCURS 4 TIMES ASCENDING KEY IS FIELD-1 FIELD-2NC2384.2
008000         DESCENDING KEY IS FIELD-3  FIELD-4 INDEXED BY IDX-4      NC2384.2
008100         IDX-5  IDX-6.                                            NC2384.2
008200         03  FIELD-1  PICTURE 9.                                  NC2384.2
008300         03  FIELD-2  PICTURE 9.                                  NC2384.2
008400         03  FIELD-3  PICTURE 9.                                  NC2384.2
008500         03  FIELD-4  PICTURE 9.                                  NC2384.2
008600         03  2ND-ENTRY OCCURS 4 TIMES DESCENDING IS FIELD-5       NC2384.2
008700             FIELD-6  FIELD-7  FIELD-8 INDEXED IDX-7 IDX-8 IDX-9. NC2384.2
008800             04  FIELD-5  PICTURE X.                              NC2384.2
008900             04  FIELD-6  PICTURE X.                              NC2384.2
009000             04  FIELD-7  PICTURE X.                              NC2384.2
009100             04  FIELD-8  PICTURE X.                              NC2384.2
009200 01  NOTE-1.                                                      NC2384.2
009300     02  FILLER                   PICTURE X(74)  VALUE            NC2384.2
009400     "NOTE 1 - CORRECT AND COMPUTED DATA ARE EQUAL BUT THE AT END NC2384.2
009500-    "PATH WAS TAKEN".                                            NC2384.2
009600     02  FILLER                   PICTURE X(46)  VALUE SPACES.    NC2384.2
009700 01  NOTE-2.                                                      NC2384.2
009800     02  FILLER                   PICTURE X(112)  VALUE           NC2384.2
009900     "NOTE 2 - CORRECT AND COMPUTED DATA ARE NOT EQUAL. THE COMPUTNC2384.2
010000-    "ED ENTRY WAS EXTRACTED FROM THE TABLE BY SUBSCRIPTS.".      NC2384.2
010100     02  FILLER                   PICTURE X(8)  VALUE SPACES.     NC2384.2
010200                                                                  NC2384.2
010300 01  END-STMT.                                                    NC2384.2
010400     02  FILLER         PICTURE X(7)  VALUE "AT END ".            NC2384.2
010500     02  END-IDX                  PICTURE X(5)  VALUE SPACES.     NC2384.2
010600     02  FILLER                   PICTURE XXX  VALUE " = ".       NC2384.2
010700     02  IDX-VALU                 PICTURE 99  VALUE 00.           NC2384.2
010800     02  FILLER                   PICTURE XXX  VALUE SPACES.      NC2384.2
010900 01  TEST-RESULTS.                                                NC2384.2
011000     02 FILLER                   PIC X      VALUE SPACE.          NC2384.2
011100     02 FEATURE                  PIC X(20)  VALUE SPACE.          NC2384.2
011200     02 FILLER                   PIC X      VALUE SPACE.          NC2384.2
011300     02 P-OR-F                   PIC X(5)   VALUE SPACE.          NC2384.2
011400     02 FILLER                   PIC X      VALUE SPACE.          NC2384.2
011500     02  PAR-NAME.                                                NC2384.2
011600       03 FILLER                 PIC X(19)  VALUE SPACE.          NC2384.2
011700       03  PARDOT-X              PIC X      VALUE SPACE.          NC2384.2
011800       03 DOTVALUE               PIC 99     VALUE ZERO.           NC2384.2
011900     02 FILLER                   PIC X(8)   VALUE SPACE.          NC2384.2
012000     02 RE-MARK                  PIC X(61).                       NC2384.2
012100 01  TEST-COMPUTED.                                               NC2384.2
012200     02 FILLER                   PIC X(30)  VALUE SPACE.          NC2384.2
012300     02 FILLER                   PIC X(17)  VALUE                 NC2384.2
012400            "       COMPUTED=".                                   NC2384.2
012500     02 COMPUTED-X.                                               NC2384.2
012600     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          NC2384.2
012700     03 COMPUTED-N               REDEFINES COMPUTED-A             NC2384.2
012800                                 PIC -9(9).9(9).                  NC2384.2
012900     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         NC2384.2
013000     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     NC2384.2
013100     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     NC2384.2
013200     03       CM-18V0 REDEFINES COMPUTED-A.                       NC2384.2
013300         04 COMPUTED-18V0                    PIC -9(18).          NC2384.2
013400         04 FILLER                           PIC X.               NC2384.2
013500     03 FILLER PIC X(50) VALUE SPACE.                             NC2384.2
013600 01  TEST-CORRECT.                                                NC2384.2
013700     02 FILLER PIC X(30) VALUE SPACE.                             NC2384.2
013800     02 FILLER PIC X(17) VALUE "       CORRECT =".                NC2384.2
013900     02 CORRECT-X.                                                NC2384.2
014000     03 CORRECT-A                  PIC X(20) VALUE SPACE.         NC2384.2
014100     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      NC2384.2
014200     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         NC2384.2
014300     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     NC2384.2
014400     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     NC2384.2
014500     03      CR-18V0 REDEFINES CORRECT-A.                         NC2384.2
014600         04 CORRECT-18V0                     PIC -9(18).          NC2384.2
014700         04 FILLER                           PIC X.               NC2384.2
014800     03 FILLER PIC X(2) VALUE SPACE.                              NC2384.2
014900     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     NC2384.2
015000 01  CCVS-C-1.                                                    NC2384.2
015100     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PANC2384.2
015200-    "SS  PARAGRAPH-NAME                                          NC2384.2
015300-    "       REMARKS".                                            NC2384.2
015400     02 FILLER                     PIC X(20)    VALUE SPACE.      NC2384.2
015500 01  CCVS-C-2.                                                    NC2384.2
015600     02 FILLER                     PIC X        VALUE SPACE.      NC2384.2
015700     02 FILLER                     PIC X(6)     VALUE "TESTED".   NC2384.2
015800     02 FILLER                     PIC X(15)    VALUE SPACE.      NC2384.2
015900     02 FILLER                     PIC X(4)     VALUE "FAIL".     NC2384.2
016000     02 FILLER                     PIC X(94)    VALUE SPACE.      NC2384.2
016100 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       NC2384.2
016200 01  REC-CT                        PIC 99       VALUE ZERO.       NC2384.2
016300 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       NC2384.2
016400 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       NC2384.2
016500 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       NC2384.2
016600 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       NC2384.2
016700 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       NC2384.2
016800 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       NC2384.2
016900 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      NC2384.2
017000 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       NC2384.2
017100 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     NC2384.2
017200 01  CCVS-H-1.                                                    NC2384.2
017300     02  FILLER                    PIC X(39)    VALUE SPACES.     NC2384.2
017400     02  FILLER                    PIC X(42)    VALUE             NC2384.2
017500     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 NC2384.2
017600     02  FILLER                    PIC X(39)    VALUE SPACES.     NC2384.2
017700 01  CCVS-H-2A.                                                   NC2384.2
017800   02  FILLER                        PIC X(40)  VALUE SPACE.      NC2384.2
017900   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  NC2384.2
018000   02  FILLER                        PIC XXXX   VALUE             NC2384.2
018100     "4.2 ".                                                      NC2384.2
018200   02  FILLER                        PIC X(28)  VALUE             NC2384.2
018300            " COPY - NOT FOR DISTRIBUTION".                       NC2384.2
018400   02  FILLER                        PIC X(41)  VALUE SPACE.      NC2384.2
018500                                                                  NC2384.2
018600 01  CCVS-H-2B.                                                   NC2384.2
018700   02  FILLER                        PIC X(15)  VALUE             NC2384.2
018800            "TEST RESULT OF ".                                    NC2384.2
018900   02  TEST-ID                       PIC X(9).                    NC2384.2
019000   02  FILLER                        PIC X(4)   VALUE             NC2384.2
019100            " IN ".                                               NC2384.2
019200   02  FILLER                        PIC X(12)  VALUE             NC2384.2
019300     " HIGH       ".                                              NC2384.2
019400   02  FILLER                        PIC X(22)  VALUE             NC2384.2
019500            " LEVEL VALIDATION FOR ".                             NC2384.2
019600   02  FILLER                        PIC X(58)  VALUE             NC2384.2
019700     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2384.2
019800 01  CCVS-H-3.                                                    NC2384.2
019900     02  FILLER                      PIC X(34)  VALUE             NC2384.2
020000            " FOR OFFICIAL USE ONLY    ".                         NC2384.2
020100     02  FILLER                      PIC X(58)  VALUE             NC2384.2
020200     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC2384.2
020300     02  FILLER                      PIC X(28)  VALUE             NC2384.2
020400            "  COPYRIGHT   1985 ".                                NC2384.2
020500 01  CCVS-E-1.                                                    NC2384.2
020600     02 FILLER                       PIC X(52)  VALUE SPACE.      NC2384.2
020700     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              NC2384.2
020800     02 ID-AGAIN                     PIC X(9).                    NC2384.2
020900     02 FILLER                       PIC X(45)  VALUE SPACES.     NC2384.2
021000 01  CCVS-E-2.                                                    NC2384.2
021100     02  FILLER                      PIC X(31)  VALUE SPACE.      NC2384.2
021200     02  FILLER                      PIC X(21)  VALUE SPACE.      NC2384.2
021300     02 CCVS-E-2-2.                                               NC2384.2
021400         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      NC2384.2
021500         03 FILLER                   PIC X      VALUE SPACE.      NC2384.2
021600         03 ENDER-DESC               PIC X(44)  VALUE             NC2384.2
021700            "ERRORS ENCOUNTERED".                                 NC2384.2
021800 01  CCVS-E-3.                                                    NC2384.2
021900     02  FILLER                      PIC X(22)  VALUE             NC2384.2
022000            " FOR OFFICIAL USE ONLY".                             NC2384.2
022100     02  FILLER                      PIC X(12)  VALUE SPACE.      NC2384.2
022200     02  FILLER                      PIC X(58)  VALUE             NC2384.2
022300     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2384.2
022400     02  FILLER                      PIC X(13)  VALUE SPACE.      NC2384.2
022500     02 FILLER                       PIC X(15)  VALUE             NC2384.2
022600             " COPYRIGHT 1985".                                   NC2384.2
022700 01  CCVS-E-4.                                                    NC2384.2
022800     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      NC2384.2
022900     02 FILLER                       PIC X(4)   VALUE " OF ".     NC2384.2
023000     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      NC2384.2
023100     02 FILLER                       PIC X(40)  VALUE             NC2384.2
023200      "  TESTS WERE EXECUTED SUCCESSFULLY".                       NC2384.2
023300 01  XXINFO.                                                      NC2384.2
023400     02 FILLER                       PIC X(19)  VALUE             NC2384.2
023500            "*** INFORMATION ***".                                NC2384.2
023600     02 INFO-TEXT.                                                NC2384.2
023700       04 FILLER                     PIC X(8)   VALUE SPACE.      NC2384.2
023800       04 XXCOMPUTED                 PIC X(20).                   NC2384.2
023900       04 FILLER                     PIC X(5)   VALUE SPACE.      NC2384.2
024000       04 XXCORRECT                  PIC X(20).                   NC2384.2
024100     02 INF-ANSI-REFERENCE           PIC X(48).                   NC2384.2
024200 01  HYPHEN-LINE.                                                 NC2384.2
024300     02 FILLER  PIC IS X VALUE IS SPACE.                          NC2384.2
024400     02 FILLER  PIC IS X(65)    VALUE IS "************************NC2384.2
024500-    "*****************************************".                 NC2384.2
024600     02 FILLER  PIC IS X(54)    VALUE IS "************************NC2384.2
024700-    "******************************".                            NC2384.2
024800 01  CCVS-PGM-ID                     PIC X(9)   VALUE             NC2384.2
024900     "NC238A".                                                    NC2384.2
025000 PROCEDURE DIVISION.                                              NC2384.2
025100 CCVS1 SECTION.                                                   NC2384.2
025200 OPEN-FILES.                                                      NC2384.2
025300     OPEN     OUTPUT PRINT-FILE.                                  NC2384.2
025400     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   NC2384.2
025500     MOVE    SPACE TO TEST-RESULTS.                               NC2384.2
025600     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             NC2384.2
025700     GO TO CCVS1-EXIT.                                            NC2384.2
025800 CLOSE-FILES.                                                     NC2384.2
025900     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   NC2384.2
026000 TERMINATE-CCVS.                                                  NC2384.2
026100*    EXIT PROGRAM.                                                NC2384.2
026200*TERMINATE-CALL.                                                  NC2384.2
026300     STOP     RUN.                                                NC2384.2
026400 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         NC2384.2
026500 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           NC2384.2
026600 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          NC2384.2
026700 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      NC2384.2
026800     MOVE "****TEST DELETED****" TO RE-MARK.                      NC2384.2
026900 PRINT-DETAIL.                                                    NC2384.2
027000     IF REC-CT NOT EQUAL TO ZERO                                  NC2384.2
027100             MOVE "." TO PARDOT-X                                 NC2384.2
027200             MOVE REC-CT TO DOTVALUE.                             NC2384.2
027300     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      NC2384.2
027400     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               NC2384.2
027500        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 NC2384.2
027600          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 NC2384.2
027700     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              NC2384.2
027800     MOVE SPACE TO CORRECT-X.                                     NC2384.2
027900     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         NC2384.2
028000     MOVE     SPACE TO RE-MARK.                                   NC2384.2
028100 HEAD-ROUTINE.                                                    NC2384.2
028200     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC2384.2
028300     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC2384.2
028400     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC2384.2
028500     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC2384.2
028600 COLUMN-NAMES-ROUTINE.                                            NC2384.2
028700     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2384.2
028800     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2384.2
028900     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        NC2384.2
029000 END-ROUTINE.                                                     NC2384.2
029100     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.NC2384.2
029200 END-RTN-EXIT.                                                    NC2384.2
029300     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2384.2
029400 END-ROUTINE-1.                                                   NC2384.2
029500      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      NC2384.2
029600      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               NC2384.2
029700      ADD PASS-COUNTER TO ERROR-HOLD.                             NC2384.2
029800*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   NC2384.2
029900      MOVE PASS-COUNTER TO CCVS-E-4-1.                            NC2384.2
030000      MOVE ERROR-HOLD TO CCVS-E-4-2.                              NC2384.2
030100      MOVE CCVS-E-4 TO CCVS-E-2-2.                                NC2384.2
030200      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           NC2384.2
030300  END-ROUTINE-12.                                                 NC2384.2
030400      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        NC2384.2
030500     IF       ERROR-COUNTER IS EQUAL TO ZERO                      NC2384.2
030600         MOVE "NO " TO ERROR-TOTAL                                NC2384.2
030700         ELSE                                                     NC2384.2
030800         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       NC2384.2
030900     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           NC2384.2
031000     PERFORM WRITE-LINE.                                          NC2384.2
031100 END-ROUTINE-13.                                                  NC2384.2
031200     IF DELETE-COUNTER IS EQUAL TO ZERO                           NC2384.2
031300         MOVE "NO " TO ERROR-TOTAL  ELSE                          NC2384.2
031400         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      NC2384.2
031500     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   NC2384.2
031600     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2384.2
031700      IF   INSPECT-COUNTER EQUAL TO ZERO                          NC2384.2
031800          MOVE "NO " TO ERROR-TOTAL                               NC2384.2
031900      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   NC2384.2
032000      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            NC2384.2
032100      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          NC2384.2
032200     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2384.2
032300 WRITE-LINE.                                                      NC2384.2
032400     ADD 1 TO RECORD-COUNT.                                       NC2384.2
032500*    IF RECORD-COUNT GREATER 50                                   NC2384.2
032600*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          NC2384.2
032700*        MOVE SPACE TO DUMMY-RECORD                               NC2384.2
032800*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  NC2384.2
032900*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             NC2384.2
033000*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     NC2384.2
033100*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          NC2384.2
033200*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          NC2384.2
033300*        MOVE ZERO TO RECORD-COUNT.                               NC2384.2
033400     PERFORM WRT-LN.                                              NC2384.2
033500 WRT-LN.                                                          NC2384.2
033600     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               NC2384.2
033700     MOVE SPACE TO DUMMY-RECORD.                                  NC2384.2
033800 BLANK-LINE-PRINT.                                                NC2384.2
033900     PERFORM WRT-LN.                                              NC2384.2
034000 FAIL-ROUTINE.                                                    NC2384.2
034100     IF   COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE. NC2384.2
034200     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.NC2384.2
034300     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC2384.2
034400     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   NC2384.2
034500     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2384.2
034600     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC2384.2
034700     GO TO  FAIL-ROUTINE-EX.                                      NC2384.2
034800 FAIL-ROUTINE-WRITE.                                              NC2384.2
034900     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         NC2384.2
035000     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 NC2384.2
035100     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. NC2384.2
035200     MOVE   SPACES TO COR-ANSI-REFERENCE.                         NC2384.2
035300 FAIL-ROUTINE-EX. EXIT.                                           NC2384.2
035400 BAIL-OUT.                                                        NC2384.2
035500     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   NC2384.2
035600     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           NC2384.2
035700 BAIL-OUT-WRITE.                                                  NC2384.2
035800     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  NC2384.2
035900     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC2384.2
036000     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2384.2
036100     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC2384.2
036200 BAIL-OUT-EX. EXIT.                                               NC2384.2
036300 CCVS1-EXIT.                                                      NC2384.2
036400     EXIT.                                                        NC2384.2
036500 SECT-NC238A-001 SECTION.                                         NC2384.2
036600 TH-11-001.                                                       NC2384.2
036700     SET IDX-4  IDX-7 TO 01.                                      NC2384.2
036800 BUILD-LOOP-1.                                                    NC2384.2
036900     MOVE SUB-5 TO FIELD-1 (IDX-4) FIELD-2 (IDX-4).               NC2384.2
037000     MOVE SUB-4 TO FIELD-3 (IDX-4) FIELD-4 (IDX-4).               NC2384.2
037100     PERFORM BUILD-ENTRY-2 THRU ENTRY-2-EXIT.                     NC2384.2
037200     IF 2ND-ENTRY (4, 4) EQUAL TO "AAAA" GO TO BUILD-EXIT.        NC2384.2
037300     ADD 1 TO SUB-5.                                              NC2384.2
037400     SUBTRACT 1 FROM SUB-4.                                       NC2384.2
037500     SET IDX-4 UP BY 1.                                           NC2384.2
037600     GO TO BUILD-LOOP-1.                                          NC2384.2
037700 BUILD-ENTRY-2.                                                   NC2384.2
037800     MOVE ALPHA-BET (SUB-6) TO 2ND-ENTRY (IDX-4, IDX-7).          NC2384.2
037900     IF IDX-7 EQUAL TO 4                                          NC2384.2
038000         SET IDX-7 TO 1                                           NC2384.2
038100         ADD 1 TO SUB-6                                           NC2384.2
038200         GO TO ENTRY-2-EXIT.                                      NC2384.2
038300     SET IDX-7 UP BY 1.                                           NC2384.2
038400     ADD 1 TO SUB-6.                                              NC2384.2
038500     GO TO BUILD-ENTRY-2.                                         NC2384.2
038600 ENTRY-2-EXIT.                                                    NC2384.2
038700     EXIT.                                                        NC2384.2
038800 BUILD-EXIT.                                                      NC2384.2
038900     EXIT.                                                        NC2384.2
039000*                                                                 NC2384.2
039100 SCH-INIT-F1-1.                                                   NC2384.2
039200     MOVE "SCH-TEST-F1-1" TO PAR-NAME.                            NC2384.2
039300     MOVE "VI-121 6.21.2" TO ANSI-REFERENCE.                      NC2384.2
039400     MOVE "SEARCH" TO FEATURE.                                    NC2384.2
039500     MOVE "IMPLIED VARYING OPTION" TO RE-MARK.                    NC2384.2
039600     SET IDX-4 TO 01.                                             NC2384.2
039700 SCH-TEST-F1-1.                                                   NC2384.2
039800     SEARCH 1ST-ENTRY                                             NC2384.2
039900         WHEN 1ST-ENTRY (IDX-4) EQUAL TO "2288LLLLKKKKJJJJIIII"   NC2384.2
040000             MOVE 1ST-ENTRY (IDX-4) TO LEVEL-HOLD.                NC2384.2
040100     IF LEVEL-HOLD EQUAL TO "2288"                                NC2384.2
040200         PERFORM PASS                                             NC2384.2
040300         GO TO SCH-WRITE-F1-1.                                    NC2384.2
040400     GO TO SCH-FAIL-F1-1.                                         NC2384.2
040500 SCH-DELETE-F1-1.                                                 NC2384.2
040600     PERFORM DE-LETE.                                             NC2384.2
040700     GO TO SCH-WRITE-F1-1.                                        NC2384.2
040800 SCH-FAIL-F1-1.                                                   NC2384.2
040900     MOVE "2288" TO CORRECT-A.                                    NC2384.2
041000     MOVE "ENTRY NOT FOUND" TO COMPUTED-A.                        NC2384.2
041100     PERFORM FAIL.                                                NC2384.2
041200 SCH-WRITE-F1-1.                                                  NC2384.2
041300     PERFORM PRINT-DETAIL.                                        NC2384.2
041400*                                                                 NC2384.2
041500 SCH-INIT-F1-2.                                                   NC2384.2
041600     MOVE "SCH-TEST-F1-2" TO PAR-NAME.                            NC2384.2
041700     MOVE "VI-121 6.21.2" TO ANSI-REFERENCE.                      NC2384.2
041800     MOVE "FORMAT 1 VARYING OPTION" TO RE-MARK.                   NC2384.2
041900     MOVE "SEARCH WHEN SERIES" TO FEATURE.                        NC2384.2
042000     SET IDX-5 TO 04.                                             NC2384.2
042100 SCH-TEST-F1-2.                                                   NC2384.2
042200     SEARCH 1ST-ENTRY VARYING IDX-5                               NC2384.2
042300                      AT END MOVE SPACES TO LEVEL-HOLD            NC2384.2
042400         WHEN FIELD-1 (IDX-5) EQUAL TO 3                          NC2384.2
042500             MOVE FIELD-1 (IDX-5) TO LEVEL-HOLD                   NC2384.2
042600         WHEN FIELD-4 (IDX-5) EQUAL TO 6                          NC2384.2
042700             MOVE FIELD-4 (IDX-5) TO LEVEL-HOLD.                  NC2384.2
042800     MOVE "FORMAT 1 W/0 VARYING" TO RE-MARK.                      NC2384.2
042900     IF LEVEL-HOLD EQUAL TO "6   "                                NC2384.2
043000         PERFORM PASS                                             NC2384.2
043100         GO TO SCH-WRITE-F1-2.                                    NC2384.2
043200     GO TO SCH-FAIL-F1-2.                                         NC2384.2
043300 SCH-DELETE-F1-2.                                                 NC2384.2
043400     PERFORM DE-LETE.                                             NC2384.2
043500     GO TO SCH-WRITE-F1-2.                                        NC2384.2
043600 SCH-FAIL-F1-2.                                                   NC2384.2
043700     MOVE "6" TO CORRECT-A.                                       NC2384.2
043800     MOVE LEVEL-HOLD TO COMPUTED-A.                               NC2384.2
043900     PERFORM FAIL.                                                NC2384.2
044000 SCH-WRITE-F1-2.                                                  NC2384.2
044100     PERFORM PRINT-DETAIL.                                        NC2384.2
044200*                                                                 NC2384.2
044300 SCH-INIT-F1-3.                                                   NC2384.2
044400     MOVE "SCH-TEST-F1-3" TO PAR-NAME.                            NC2384.2
044500     MOVE "VI-121 6.21.2" TO ANSI-REFERENCE.                      NC2384.2
044600     MOVE "SEARCH WHEN SERIES" TO FEATURE.                        NC2384.2
044700     SET IDX-5 TO 03.                                             NC2384.2
044800     SET IDX-8 TO 01.                                             NC2384.2
044900 SCH-TEST-F1-3.                                                   NC2384.2
045000     SEARCH 2ND-ENTRY VARYING IDX-8                               NC2384.2
045100                      AT END MOVE SPACES TO LEVEL-HOLD            NC2384.2
045200         WHEN FIELD-7 (IDX-5, IDX-8) EQUAL TO "E"                 NC2384.2
045300             MOVE FIELD-7 (IDX-5, IDX-8) TO LEVEL-HOLD            NC2384.2
045400         WHEN FIELD-5 (IDX-5, IDX-8) EQUAL TO "E"                 NC2384.2
045500             MOVE FIELD-5 (IDX-5, IDX-8) TO LEVEL-HOLD            NC2384.2
045600         WHEN FIELD-8 (IDX-5, IDX-8) EQUAL TO "E"                 NC2384.2
045700             MOVE FIELD-8 (IDX-5, IDX-8) TO LEVEL-HOLD.           NC2384.2
045800     IF LEVEL-HOLD EQUAL TO "E   "                                NC2384.2
045900         PERFORM PASS                                             NC2384.2
046000         GO TO SCH-WRITE-F1-3.                                    NC2384.2
046100     GO TO SCH-FAIL-F1-3.                                         NC2384.2
046200 SCH-DELETE-F1-3.                                                 NC2384.2
046300     PERFORM DE-LETE.                                             NC2384.2
046400     GO TO SCH-WRITE-F1-3.                                        NC2384.2
046500 SCH-FAIL-F1-3.                                                   NC2384.2
046600     MOVE "E" TO CORRECT-A.                                       NC2384.2
046700     MOVE LEVEL-HOLD TO COMPUTED-A.                               NC2384.2
046800     PERFORM FAIL.                                                NC2384.2
046900 SCH-WRITE-F1-3.                                                  NC2384.2
047000     PERFORM PRINT-DETAIL.                                        NC2384.2
047100*                                                                 NC2384.2
047200 SCH-INIT-F1-4.                                                   NC2384.2
047300     MOVE "SCH-TEST-F1-4" TO PAR-NAME.                            NC2384.2
047400     MOVE "VI-121 6.21.2" TO ANSI-REFERENCE.                      NC2384.2
047500     MOVE "SEARCH VARYING" TO FEATURE.                            NC2384.2
047600     MOVE "WHEN-COMPOUND CONDITION" TO RE-MARK.                   NC2384.2
047700     SET IDX-4 IDX-9 TO 04.                                       NC2384.2
047800 SCH-TEST-F1-4.                                                   NC2384.2
047900     SEARCH 2ND-ENTRY VARYING IDX-9                               NC2384.2
048000                      AT END MOVE SPACES TO LEVEL-HOLD            NC2384.2
048100         WHEN FIELD-6 (IDX-4, IDX-9) NOT EQUAL TO "A"             NC2384.2
048200          AND FIELD-7 (IDX-4, IDX-9) NOT EQUAL TO "A"             NC2384.2
048300              MOVE "A" TO LEVEL-HOLD.                             NC2384.2
048400     IF LEVEL-HOLD EQUAL TO SPACES                                NC2384.2
048500         PERFORM PASS                                             NC2384.2
048600         GO TO SCH-WRITE-F1-4.                                    NC2384.2
048700     GO TO SCH-FAIL-F1-4.                                         NC2384.2
048800 SCH-DELETE-F1-4.                                                 NC2384.2
048900     PERFORM DE-LETE.                                             NC2384.2
049000     GO TO SCH-WRITE-F1-4.                                        NC2384.2
049100 SCH-FAIL-F1-4.                                                   NC2384.2
049200     MOVE LEVEL-HOLD TO COMPUTED-A.                               NC2384.2
049300     MOVE "NO SUCH ENTRY" TO CORRECT-A.                           NC2384.2
049400     PERFORM FAIL.                                                NC2384.2
049500 SCH-WRITE-F1-4.                                                  NC2384.2
049600     PERFORM PRINT-DETAIL.                                        NC2384.2
049700*                                                                 NC2384.2
049800 SCH-INIT-F1-5.                                                   NC2384.2
049900     MOVE "SCH-TEST-F1-5" TO PAR-NAME.                            NC2384.2
050000     MOVE "VI-121 6.21.2" TO ANSI-REFERENCE.                      NC2384.2
050100     MOVE "SEARCH WHEN SERIES" TO FEATURE.                        NC2384.2
050200     SET IDX-6 TO 02.                                             NC2384.2
050300     SET IDX-7 TO 02.                                             NC2384.2
050400 SCH-TEST-F1-5.                                                   NC2384.2
050500     SEARCH 2ND-ENTRY VARYING IDX-7                               NC2384.2
050600                      AT END MOVE SPACES TO LEVEL-HOLD            NC2384.2
050700         WHEN FIELD-5 (IDX-6, IDX-7) EQUAL TO "M"                 NC2384.2
050800             MOVE FIELD-5 (IDX-6, IDX-7) TO LEVEL-HOLD            NC2384.2
050900         WHEN FIELD-6 (IDX-6, IDX-7) EQUAL TO "N"                 NC2384.2
051000             MOVE FIELD-6 (IDX-6, IDX-7) TO LEVEL-HOLD            NC2384.2
051100         WHEN FIELD-7 (IDX-6, IDX-7) EQUAL TO "O"                 NC2384.2
051200             MOVE FIELD-7 (IDX-6, IDX-7) TO LEVEL-HOLD            NC2384.2
051300         WHEN FIELD-8 (IDX-6, IDX-7) EQUAL TO "I"                 NC2384.2
051400             MOVE FIELD-8 (IDX-6, IDX-7) TO LEVEL-HOLD.           NC2384.2
051500     IF LEVEL-HOLD EQUAL TO "I   "                                NC2384.2
051600         PERFORM PASS                                             NC2384.2
051700         GO TO SCH-WRITE-F1-5.                                    NC2384.2
051800     GO TO SCH-FAIL-F1-5.                                         NC2384.2
051900 SCH-DELETE-F1-5.                                                 NC2384.2
052000     PERFORM DE-LETE.                                             NC2384.2
052100     GO TO SCH-WRITE-F1-5.                                        NC2384.2
052200 SCH-FAIL-F1-5.                                                   NC2384.2
052300     MOVE "I" TO CORRECT-A.                                       NC2384.2
052400     MOVE LEVEL-HOLD TO COMPUTED-A.                               NC2384.2
052500     PERFORM FAIL.                                                NC2384.2
052600 SCH-WRITE-F1-5.                                                  NC2384.2
052700     PERFORM PRINT-DETAIL.                                        NC2384.2
052800*                                                                 NC2384.2
052900 SCH-INIT-F2-6.                                                   NC2384.2
053000     MOVE "SCH-TEST-F2-6" TO PAR-NAME.                            NC2384.2
053100     MOVE "VI-121 6.21.2" TO ANSI-REFERENCE.                      NC2384.2
053200     MOVE "SEARCH ALL " TO FEATURE.                               NC2384.2
053300     MOVE "WHEN-COMPOUND CONDITION" TO RE-MARK.                   NC2384.2
053400 SCH-TEST-F2-6.                                                   NC2384.2
053500     SEARCH ALL 1ST-ENTRY                                         NC2384.2
053600                      AT END MOVE SPACES TO LEVEL-HOLD            NC2384.2
053700         WHEN FIELD-1 (IDX-4) EQUAL TO 2 AND                      NC2384.2
053800              FIELD-2 (IDX-4) EQUAL TO 2 MOVE 2 TO LEVEL-HOLD.    NC2384.2
053900     IF LEVEL-HOLD EQUAL TO "2   "                                NC2384.2
054000         PERFORM PASS                                             NC2384.2
054100         GO TO SCH-WRITE-F2-6.                                    NC2384.2
054200     GO TO SCH-FAIL-F2-6.                                         NC2384.2
054300 SCH-DELETE-F2-6.                                                 NC2384.2
054400     PERFORM DE-LETE.                                             NC2384.2
054500     GO TO SCH-WRITE-F2-6.                                        NC2384.2
054600 SCH-FAIL-F2-6.                                                   NC2384.2
054700     MOVE LEVEL-HOLD TO COMPUTED-A.                               NC2384.2
054800     MOVE "2" TO CORRECT-A.                                       NC2384.2
054900     PERFORM FAIL.                                                NC2384.2
055000 SCH-WRITE-F2-6.                                                  NC2384.2
055100     PERFORM PRINT-DETAIL.                                        NC2384.2
055200*                                                                 NC2384.2
055300 SCH-INIT-F2-7.                                                   NC2384.2
055400     MOVE "SCH-TEST-F2-7" TO PAR-NAME.                            NC2384.2
055500     MOVE "VI-121 6.21.2" TO ANSI-REFERENCE.                      NC2384.2
055600     MOVE "SEARCH ALL " TO FEATURE.                               NC2384.2
055700 SCH-TEST-F2-7.                                                   NC2384.2
055800     SEARCH ALL 1ST-ENTRY                                         NC2384.2
055900                      AT END MOVE SPACES TO LEVEL-HOLD            NC2384.2
056000         WHEN FIELD-1 (IDX-4) EQUAL TO 4 AND                      NC2384.2
056100              FIELD-2 (IDX-4) EQUAL TO 4 AND                      NC2384.2
056200              FIELD-3 (IDX-4) EQUAL TO 6 AND                      NC2384.2
056300              FIELD-4 (IDX-4) EQUAL TO 6                          NC2384.2
056400              MOVE 6 TO LEVEL-HOLD.                               NC2384.2
056500     IF LEVEL-HOLD EQUAL TO "6   "                                NC2384.2
056600         PERFORM PASS                                             NC2384.2
056700         GO TO SCH-WRITE-F2-7.                                    NC2384.2
056800     GO TO SCH-FAIL-F2-7.                                         NC2384.2
056900 SCH-DELETE-F2-7.                                                 NC2384.2
057000     PERFORM DE-LETE.                                             NC2384.2
057100     GO TO SCH-WRITE-F2-7.                                        NC2384.2
057200 SCH-FAIL-F2-7.                                                   NC2384.2
057300     MOVE LEVEL-HOLD TO COMPUTED-A.                               NC2384.2
057400     MOVE "6" TO CORRECT-A.                                       NC2384.2
057500     PERFORM FAIL.                                                NC2384.2
057600 SCH-WRITE-F2-7.                                                  NC2384.2
057700     PERFORM PRINT-DETAIL.                                        NC2384.2
057800*                                                                 NC2384.2
057900 SCH-INIT-F2-8.                                                   NC2384.2
058000     MOVE "SCH-TEST-F2-8" TO PAR-NAME.                            NC2384.2
058100     MOVE "VI-121 6.21.2" TO ANSI-REFERENCE.                      NC2384.2
058200     MOVE "SEARCH ALL " TO FEATURE.                               NC2384.2
058300     MOVE SPACES TO LEVEL-HOLD.                                   NC2384.2
058400     SET IDX-4 TO 01.                                             NC2384.2
058500 SCH-TEST-F2-8.                                                   NC2384.2
058600     SEARCH ALL 2ND-ENTRY                                         NC2384.2
058700         WHEN FIELD-5  (IDX-4, IDX-7) EQUAL TO "O" AND            NC2384.2
058800              FIELD-6  (IDX-4, IDX-7) EQUAL TO "O" AND            NC2384.2
058900              FIELD-7  (IDX-4, IDX-7) EQUAL TO "O" AND            NC2384.2
059000              FIELD-8  (IDX-4, IDX-7) EQUAL TO "P"                NC2384.2
059100             MOVE "OOOP" TO LEVEL-HOLD.                           NC2384.2
059200     IF LEVEL-HOLD EQUAL TO "OOOP"                                NC2384.2
059300         GO TO SCH-FAIL-F2-8.                                     NC2384.2
059400     PERFORM PASS.                                                NC2384.2
059500     GO TO SCH-WRITE-F2-8.                                        NC2384.2
059600 SCH-DELETE-F2-8.                                                 NC2384.2
059700     PERFORM DE-LETE.                                             NC2384.2
059800     GO TO SCH-WRITE-F2-8.                                        NC2384.2
059900 SCH-FAIL-F2-8.                                                   NC2384.2
060000     MOVE "NO SUCH ENTRY" TO CORRECT-A                            NC2384.2
060100     MOVE LEVEL-HOLD TO COMPUTED-A                                NC2384.2
060200     PERFORM FAIL.                                                NC2384.2
060300 SCH-WRITE-F2-8.                                                  NC2384.2
060400     PERFORM PRINT-DETAIL.                                        NC2384.2
060500*                                                                 NC2384.2
060600 SCH-INIT-F2-9.                                                   NC2384.2
060700     MOVE "SCH-TEST-F2-9" TO PAR-NAME.                            NC2384.2
060800     MOVE "VI-121 6.21.2" TO ANSI-REFERENCE.                      NC2384.2
060900     MOVE "SEARCH ALL " TO FEATURE.                               NC2384.2
061000     MOVE SPACES TO LEVEL-HOLD.                                   NC2384.2
061100     SET IDX-4 TO 04.                                             NC2384.2
061200 SCH-TEST-F2-9.                                                   NC2384.2
061300     SEARCH ALL 2ND-ENTRY                                         NC2384.2
061400         WHEN FIELD-5 (IDX-4, IDX-7) EQUAL TO "B" AND             NC2384.2
061500              FIELD-6 (IDX-4, IDX-7) EQUAL TO "B"                 NC2384.2
061600             MOVE "BB" TO LEVEL-HOLD.                             NC2384.2
061700     IF LEVEL-HOLD EQUAL TO "BB "                                 NC2384.2
061800         PERFORM PASS                                             NC2384.2
061900         GO TO SCH-WRITE-F2-9.                                    NC2384.2
062000     GO TO SCH-FAIL-F2-9.                                         NC2384.2
062100 SCH-DELETE-F2-9.                                                 NC2384.2
062200     PERFORM DE-LETE.                                             NC2384.2
062300     GO TO SCH-WRITE-F2-9.                                        NC2384.2
062400 SCH-FAIL-F2-9.                                                   NC2384.2
062500     MOVE "BB  " TO CORRECT-A.                                    NC2384.2
062600     MOVE LEVEL-HOLD TO COMPUTED-A.                               NC2384.2
062700     PERFORM FAIL.                                                NC2384.2
062800 SCH-WRITE-F2-9.                                                  NC2384.2
062900     PERFORM PRINT-DETAIL.                                        NC2384.2
063000*                                                                 NC2384.2
063100 SCH-INIT-F2-10.                                                  NC2384.2
063200*    ===-->  ARITHMETIC EXPRESSION OF ZERO  <--===                NC2384.2
063300     MOVE "SCH-TEST-F2-10" TO PAR-NAME.                           NC2384.2
063400     MOVE   "VI-51 6.2" TO ANSI-REFERENCE.                        NC2384.2
063500     MOVE "SEARCH ALL " TO FEATURE.                               NC2384.2
063600     MOVE "ARITHMETIC EXPRESSION OF ZERO" TO RE-MARK.             NC2384.2
063700 SCH-TEST-F2-10.                                                  NC2384.2
063800     SEARCH  ALL 1ST-ENTRY                                        NC2384.2
063900             AT END  MOVE ZERO TO LEVEL-HOLD                      NC2384.2
064000        WHEN FIELD-1 (IDX-4) EQUAL TO ZERO                        NC2384.2
064100             MOVE    2 TO LEVEL-HOLD.                             NC2384.2
064200     IF      LEVEL-HOLD EQUAL TO ZERO                             NC2384.2
064300             PERFORM PASS                                         NC2384.2
064400             GO TO   SCH-WRITE-F2-10.                             NC2384.2
064500     GO TO SCH-FAIL-F2-10.                                        NC2384.2
064600 SCH-DELETE-F2-10.                                                NC2384.2
064700     PERFORM DE-LETE.                                             NC2384.2
064800     GO TO SCH-WRITE-F2-10.                                       NC2384.2
064900 SCH-FAIL-F2-10.                                                  NC2384.2
065000     MOVE    LEVEL-HOLD TO COMPUTED-A.                            NC2384.2
065100     MOVE    ZERO TO CORRECT-A.                                   NC2384.2
065200     PERFORM FAIL.                                                NC2384.2
065300 SCH-WRITE-F2-10.                                                 NC2384.2
065400     PERFORM PRINT-DETAIL.                                        NC2384.2
065500*                                                                 NC2384.2
065600 CCVS-EXIT SECTION.                                               NC2384.2
065700 CCVS-999999.                                                     NC2384.2
065800     GO TO CLOSE-FILES.                                           NC2384.2
      *END-OF,NC238A                                                                  
