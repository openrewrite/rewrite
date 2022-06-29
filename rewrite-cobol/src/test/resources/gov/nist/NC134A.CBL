      *HEADER,COBOL,NC134A                                                            
000100 IDENTIFICATION DIVISION.                                         NC1344.2
000200 PROGRAM-ID.                                                      NC1344.2
000300     NC134A.                                                      NC1344.2
000400****************************************************************  NC1344.2
000500*                                                              *  NC1344.2
000600*    VALIDATION FOR:-                                          *  NC1344.2
000700*                                                              *  NC1344.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1344.2
000900*                                                              *  NC1344.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC1344.2
001100*                                                              *  NC1344.2
001200****************************************************************  NC1344.2
001300*                                                              *  NC1344.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  NC1344.2
001500*                                                              *  NC1344.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  NC1344.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  NC1344.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  NC1344.2
001900*                                                              *  NC1344.2
002000****************************************************************  NC1344.2
002100*                                                                 NC1344.2
002200*   PROGRAM NC134A TESTS THE ACCESSING OF A THREE-DIMENSIONAL     NC1344.2
002300*   TABLE USING NUMERIC LITERALS AND DATA-NAMES AS SUBSCRIPTS.    NC1344.2
002400*   RELATIVE SUBSCRIPTING IS ALSO USED.                           NC1344.2
002500*                                                                 NC1344.2
002600 ENVIRONMENT DIVISION.                                            NC1344.2
002700 CONFIGURATION SECTION.                                           NC1344.2
002800 SOURCE-COMPUTER.                                                 NC1344.2
002900     XXXXX082.                                                    NC1344.2
003000 OBJECT-COMPUTER.                                                 NC1344.2
003100     XXXXX083.                                                    NC1344.2
003200 INPUT-OUTPUT SECTION.                                            NC1344.2
003300 FILE-CONTROL.                                                    NC1344.2
003400     SELECT PRINT-FILE ASSIGN TO                                  NC1344.2
003500     XXXXX055.                                                    NC1344.2
003600 DATA DIVISION.                                                   NC1344.2
003700 FILE SECTION.                                                    NC1344.2
003800 FD  PRINT-FILE.                                                  NC1344.2
003900 01  PRINT-REC PICTURE X(120).                                    NC1344.2
004000 01  DUMMY-RECORD PICTURE X(120).                                 NC1344.2
004100 WORKING-STORAGE SECTION.                                         NC1344.2
004200 77  A-NAME-30-CHARACTERS-IN-LENGTH PICTURE IS XXX VALUE IS "END".NC1344.2
004300 77  LONG-PICTURE PICTURE IS XXXXXXXXXXXXXXXXXXXXXXXXXXXXX.       NC1344.2
004400 77  ONE      PICTURE IS 9 VALUE IS 1 USAGE IS COMPUTATIONAL.     NC1344.2
004500 77  TWO      PICTURE IS 9 VALUE IS 2 USAGE IS COMPUTATIONAL.     NC1344.2
004600 77  THREE    PICTURE IS 9 VALUE IS 3 COMPUTATIONAL.              NC1344.2
004700 77  FOUR     PICTURE IS 9 VALUE IS 4 COMPUTATIONAL.              NC1344.2
004800 77  FIVE     PICTURE IS 9 VALUE IS 5 COMPUTATIONAL.              NC1344.2
004900 77  SIX      PICTURE IS 9 VALUE IS 6 COMPUTATIONAL.              NC1344.2
005000 77  SEVEN    PICTURE IS 9 VALUE IS 7 COMPUTATIONAL.              NC1344.2
005100 77  EIGHT    PICTURE IS 9 VALUE    8 COMPUTATIONAL.              NC1344.2
005200 77  NINE     PICTURE IS 9 VALUE    9 USAGE IS COMPUTATIONAL.     NC1344.2
005300 77  TEN      PICTURE   99 VALUE   10 USAGE COMPUTATIONAL.        NC1344.2
005400 77  FIFTEEN  PICTURE   99 VALUE   15 USAGE COMPUTATIONAL.        NC1344.2
005500 77  TWENTY   PICTURE   99 VALUE   20 USAGE IS COMPUTATIONAL.     NC1344.2
005600 77  TWENTY-5 PICTURE   99 VALUE   25.                            NC1344.2
005700 77  D-1 PICTURE IS S9V99 VALUE IS 1.06.                          NC1344.2
005800 77  D-2 PICTURE IS S9V99 VALUE IS -1.06.                         NC1344.2
005900 77  D-3 PICTURE IS 9(18) VALUE IS 979797979797979798.            NC1344.2
006000 77  D-4 PICTURE IS S99V99 VALUE IS +10.1.                        NC1344.2
006100 77  D-5 PICTURE IS S999 VALUE IS -1.                             NC1344.2
006200 77  D-6 PICTURE IS S999P VALUE IS 10.                            NC1344.2
006300 77  D-7 PICTURE IS S99V99 VALUE IS 1.09.                         NC1344.2
006400 77  D-8 PICTURE IS S999V9 VALUE 175.                             NC1344.2
006500 77  D-9 PICTURE IS 9(4)V9(4) VALUE IS 111.1189.                  NC1344.2
006600 77  D-10 PICTURE 999 VALUE 100.                                  NC1344.2
006700 77  D-11 PICTURE 999 VALUE 300.                                  NC1344.2
006800 77  D-12 PICTURE 999 VALUE 900.                                  NC1344.2
006900 77  W-1 PICTURE IS 9.                                            NC1344.2
007000 77  W-2 PICTURE IS 99.                                           NC1344.2
007100 77  W-3 PICTURE IS 999.                                          NC1344.2
007200 77  W-4 PICTURE IS 9 VALUE IS ZERO.                              NC1344.2
007300 77  W-5 PICTURE IS 99 VALUE IS ZERO.                             NC1344.2
007400 77  W-6 PICTURE IS 999 VALUE IS ZERO.                            NC1344.2
007500 77  W-7 PICTURE IS 9.                                            NC1344.2
007600 77  W-8 PICTURE    99 BLANK ZERO.                                NC1344.2
007700 77  W-9 PICTURE    999.                                          NC1344.2
007800 77  W-10 PICTURE 99V9.                                           NC1344.2
007900 77  W-11 PICTURE S99V9.                                          NC1344.2
008000 77  W-12 PICTURE S9V99.                                          NC1344.2
008100 77  W-13 PICTURE S9(2)V9(2).                                     NC1344.2
008200 77  W-14 PICTURE IS S99V99.                                      NC1344.2
008300 77  XRAY PICTURE IS 9.                                           NC1344.2
008400 77  CTR-1 PICTURE IS 999.                                        NC1344.2
008500 77  SUBSCRIPT-1 PICTURE IS 999.                                  NC1344.2
008600 77  SUBSCRIPT-2 PICTURE IS 999.                                  NC1344.2
008700 77  SUBSCRIPT-3  PICTURE IS 999.                                 NC1344.2
008800 01  TABLE-10.                                                    NC1344.2
008900     02 STATE-1 OCCURS 10 TIMES.                                  NC1344.2
009000       03  YEAR-1 OCCURS 10 TIMES.                                NC1344.2
009100         04 ANIMAL PICTURE IS 999 OCCURS 03 TIMES.                NC1344.2
009200 01  NUMBER-LIST.                                                 NC1344.2
009300     02 FILLER PICTURE IS X VALUE IS SPACE.                       NC1344.2
009400     02 LINE-1 OCCURS 20 TIMES.                                   NC1344.2
009500       03 BLANKSPACE PICTURE IS XX.                               NC1344.2
009600       03 PRINT-ELE PICTURE IS 999.                               NC1344.2
009700 01  TABLE-1.                                                     NC1344.2
009800     02  TAB-ELE PICTURE IS 999 OCCURS 100 TIMES.                 NC1344.2
009900 01  TEST-RESULTS.                                                NC1344.2
010000     02 FILLER                   PIC X      VALUE SPACE.          NC1344.2
010100     02 FEATURE                  PIC X(20)  VALUE SPACE.          NC1344.2
010200     02 FILLER                   PIC X      VALUE SPACE.          NC1344.2
010300     02 P-OR-F                   PIC X(5)   VALUE SPACE.          NC1344.2
010400     02 FILLER                   PIC X      VALUE SPACE.          NC1344.2
010500     02  PAR-NAME.                                                NC1344.2
010600       03 FILLER                 PIC X(19)  VALUE SPACE.          NC1344.2
010700       03  PARDOT-X              PIC X      VALUE SPACE.          NC1344.2
010800       03 DOTVALUE               PIC 99     VALUE ZERO.           NC1344.2
010900     02 FILLER                   PIC X(8)   VALUE SPACE.          NC1344.2
011000     02 RE-MARK                  PIC X(61).                       NC1344.2
011100 01  TEST-COMPUTED.                                               NC1344.2
011200     02 FILLER                   PIC X(30)  VALUE SPACE.          NC1344.2
011300     02 FILLER                   PIC X(17)  VALUE                 NC1344.2
011400            "       COMPUTED=".                                   NC1344.2
011500     02 COMPUTED-X.                                               NC1344.2
011600     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          NC1344.2
011700     03 COMPUTED-N               REDEFINES COMPUTED-A             NC1344.2
011800                                 PIC -9(9).9(9).                  NC1344.2
011900     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         NC1344.2
012000     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     NC1344.2
012100     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     NC1344.2
012200     03       CM-18V0 REDEFINES COMPUTED-A.                       NC1344.2
012300         04 COMPUTED-18V0                    PIC -9(18).          NC1344.2
012400         04 FILLER                           PIC X.               NC1344.2
012500     03 FILLER PIC X(50) VALUE SPACE.                             NC1344.2
012600 01  TEST-CORRECT.                                                NC1344.2
012700     02 FILLER PIC X(30) VALUE SPACE.                             NC1344.2
012800     02 FILLER PIC X(17) VALUE "       CORRECT =".                NC1344.2
012900     02 CORRECT-X.                                                NC1344.2
013000     03 CORRECT-A                  PIC X(20) VALUE SPACE.         NC1344.2
013100     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      NC1344.2
013200     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         NC1344.2
013300     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     NC1344.2
013400     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     NC1344.2
013500     03      CR-18V0 REDEFINES CORRECT-A.                         NC1344.2
013600         04 CORRECT-18V0                     PIC -9(18).          NC1344.2
013700         04 FILLER                           PIC X.               NC1344.2
013800     03 FILLER PIC X(2) VALUE SPACE.                              NC1344.2
013900     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     NC1344.2
014000 01  CCVS-C-1.                                                    NC1344.2
014100     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PANC1344.2
014200-    "SS  PARAGRAPH-NAME                                          NC1344.2
014300-    "       REMARKS".                                            NC1344.2
014400     02 FILLER                     PIC X(20)    VALUE SPACE.      NC1344.2
014500 01  CCVS-C-2.                                                    NC1344.2
014600     02 FILLER                     PIC X        VALUE SPACE.      NC1344.2
014700     02 FILLER                     PIC X(6)     VALUE "TESTED".   NC1344.2
014800     02 FILLER                     PIC X(15)    VALUE SPACE.      NC1344.2
014900     02 FILLER                     PIC X(4)     VALUE "FAIL".     NC1344.2
015000     02 FILLER                     PIC X(94)    VALUE SPACE.      NC1344.2
015100 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       NC1344.2
015200 01  REC-CT                        PIC 99       VALUE ZERO.       NC1344.2
015300 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       NC1344.2
015400 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       NC1344.2
015500 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       NC1344.2
015600 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       NC1344.2
015700 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       NC1344.2
015800 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       NC1344.2
015900 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      NC1344.2
016000 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       NC1344.2
016100 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     NC1344.2
016200 01  CCVS-H-1.                                                    NC1344.2
016300     02  FILLER                    PIC X(39)    VALUE SPACES.     NC1344.2
016400     02  FILLER                    PIC X(42)    VALUE             NC1344.2
016500     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 NC1344.2
016600     02  FILLER                    PIC X(39)    VALUE SPACES.     NC1344.2
016700 01  CCVS-H-2A.                                                   NC1344.2
016800   02  FILLER                        PIC X(40)  VALUE SPACE.      NC1344.2
016900   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  NC1344.2
017000   02  FILLER                        PIC XXXX   VALUE             NC1344.2
017100     "4.2 ".                                                      NC1344.2
017200   02  FILLER                        PIC X(28)  VALUE             NC1344.2
017300            " COPY - NOT FOR DISTRIBUTION".                       NC1344.2
017400   02  FILLER                        PIC X(41)  VALUE SPACE.      NC1344.2
017500                                                                  NC1344.2
017600 01  CCVS-H-2B.                                                   NC1344.2
017700   02  FILLER                        PIC X(15)  VALUE             NC1344.2
017800            "TEST RESULT OF ".                                    NC1344.2
017900   02  TEST-ID                       PIC X(9).                    NC1344.2
018000   02  FILLER                        PIC X(4)   VALUE             NC1344.2
018100            " IN ".                                               NC1344.2
018200   02  FILLER                        PIC X(12)  VALUE             NC1344.2
018300     " HIGH       ".                                              NC1344.2
018400   02  FILLER                        PIC X(22)  VALUE             NC1344.2
018500            " LEVEL VALIDATION FOR ".                             NC1344.2
018600   02  FILLER                        PIC X(58)  VALUE             NC1344.2
018700     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1344.2
018800 01  CCVS-H-3.                                                    NC1344.2
018900     02  FILLER                      PIC X(34)  VALUE             NC1344.2
019000            " FOR OFFICIAL USE ONLY    ".                         NC1344.2
019100     02  FILLER                      PIC X(58)  VALUE             NC1344.2
019200     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC1344.2
019300     02  FILLER                      PIC X(28)  VALUE             NC1344.2
019400            "  COPYRIGHT   1985 ".                                NC1344.2
019500 01  CCVS-E-1.                                                    NC1344.2
019600     02 FILLER                       PIC X(52)  VALUE SPACE.      NC1344.2
019700     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              NC1344.2
019800     02 ID-AGAIN                     PIC X(9).                    NC1344.2
019900     02 FILLER                       PIC X(45)  VALUE SPACES.     NC1344.2
020000 01  CCVS-E-2.                                                    NC1344.2
020100     02  FILLER                      PIC X(31)  VALUE SPACE.      NC1344.2
020200     02  FILLER                      PIC X(21)  VALUE SPACE.      NC1344.2
020300     02 CCVS-E-2-2.                                               NC1344.2
020400         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      NC1344.2
020500         03 FILLER                   PIC X      VALUE SPACE.      NC1344.2
020600         03 ENDER-DESC               PIC X(44)  VALUE             NC1344.2
020700            "ERRORS ENCOUNTERED".                                 NC1344.2
020800 01  CCVS-E-3.                                                    NC1344.2
020900     02  FILLER                      PIC X(22)  VALUE             NC1344.2
021000            " FOR OFFICIAL USE ONLY".                             NC1344.2
021100     02  FILLER                      PIC X(12)  VALUE SPACE.      NC1344.2
021200     02  FILLER                      PIC X(58)  VALUE             NC1344.2
021300     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1344.2
021400     02  FILLER                      PIC X(13)  VALUE SPACE.      NC1344.2
021500     02 FILLER                       PIC X(15)  VALUE             NC1344.2
021600             " COPYRIGHT 1985".                                   NC1344.2
021700 01  CCVS-E-4.                                                    NC1344.2
021800     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      NC1344.2
021900     02 FILLER                       PIC X(4)   VALUE " OF ".     NC1344.2
022000     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      NC1344.2
022100     02 FILLER                       PIC X(40)  VALUE             NC1344.2
022200      "  TESTS WERE EXECUTED SUCCESSFULLY".                       NC1344.2
022300 01  XXINFO.                                                      NC1344.2
022400     02 FILLER                       PIC X(19)  VALUE             NC1344.2
022500            "*** INFORMATION ***".                                NC1344.2
022600     02 INFO-TEXT.                                                NC1344.2
022700       04 FILLER                     PIC X(8)   VALUE SPACE.      NC1344.2
022800       04 XXCOMPUTED                 PIC X(20).                   NC1344.2
022900       04 FILLER                     PIC X(5)   VALUE SPACE.      NC1344.2
023000       04 XXCORRECT                  PIC X(20).                   NC1344.2
023100     02 INF-ANSI-REFERENCE           PIC X(48).                   NC1344.2
023200 01  HYPHEN-LINE.                                                 NC1344.2
023300     02 FILLER  PIC IS X VALUE IS SPACE.                          NC1344.2
023400     02 FILLER  PIC IS X(65)    VALUE IS "************************NC1344.2
023500-    "*****************************************".                 NC1344.2
023600     02 FILLER  PIC IS X(54)    VALUE IS "************************NC1344.2
023700-    "******************************".                            NC1344.2
023800 01  CCVS-PGM-ID                     PIC X(9)   VALUE             NC1344.2
023900     "NC134A".                                                    NC1344.2
024000 PROCEDURE DIVISION.                                              NC1344.2
024100 CCVS1 SECTION.                                                   NC1344.2
024200 OPEN-FILES.                                                      NC1344.2
024300     OPEN     OUTPUT PRINT-FILE.                                  NC1344.2
024400     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   NC1344.2
024500     MOVE    SPACE TO TEST-RESULTS.                               NC1344.2
024600     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             NC1344.2
024700     GO TO CCVS1-EXIT.                                            NC1344.2
024800 CLOSE-FILES.                                                     NC1344.2
024900     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   NC1344.2
025000 TERMINATE-CCVS.                                                  NC1344.2
025100*    EXIT PROGRAM.                                                NC1344.2
025200*TERMINATE-CALL.                                                  NC1344.2
025300     STOP     RUN.                                                NC1344.2
025400 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         NC1344.2
025500 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           NC1344.2
025600 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          NC1344.2
025700 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      NC1344.2
025800     MOVE "****TEST DELETED****" TO RE-MARK.                      NC1344.2
025900 PRINT-DETAIL.                                                    NC1344.2
026000     IF REC-CT NOT EQUAL TO ZERO                                  NC1344.2
026100             MOVE "." TO PARDOT-X                                 NC1344.2
026200             MOVE REC-CT TO DOTVALUE.                             NC1344.2
026300     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      NC1344.2
026400     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               NC1344.2
026500        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 NC1344.2
026600          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 NC1344.2
026700     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              NC1344.2
026800     MOVE SPACE TO CORRECT-X.                                     NC1344.2
026900     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         NC1344.2
027000     MOVE     SPACE TO RE-MARK.                                   NC1344.2
027100 HEAD-ROUTINE.                                                    NC1344.2
027200     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC1344.2
027300     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC1344.2
027400     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC1344.2
027500     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC1344.2
027600 COLUMN-NAMES-ROUTINE.                                            NC1344.2
027700     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1344.2
027800     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1344.2
027900     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        NC1344.2
028000 END-ROUTINE.                                                     NC1344.2
028100     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.NC1344.2
028200 END-RTN-EXIT.                                                    NC1344.2
028300     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1344.2
028400 END-ROUTINE-1.                                                   NC1344.2
028500      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      NC1344.2
028600      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               NC1344.2
028700      ADD PASS-COUNTER TO ERROR-HOLD.                             NC1344.2
028800*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   NC1344.2
028900      MOVE PASS-COUNTER TO CCVS-E-4-1.                            NC1344.2
029000      MOVE ERROR-HOLD TO CCVS-E-4-2.                              NC1344.2
029100      MOVE CCVS-E-4 TO CCVS-E-2-2.                                NC1344.2
029200      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           NC1344.2
029300  END-ROUTINE-12.                                                 NC1344.2
029400      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        NC1344.2
029500     IF       ERROR-COUNTER IS EQUAL TO ZERO                      NC1344.2
029600         MOVE "NO " TO ERROR-TOTAL                                NC1344.2
029700         ELSE                                                     NC1344.2
029800         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       NC1344.2
029900     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           NC1344.2
030000     PERFORM WRITE-LINE.                                          NC1344.2
030100 END-ROUTINE-13.                                                  NC1344.2
030200     IF DELETE-COUNTER IS EQUAL TO ZERO                           NC1344.2
030300         MOVE "NO " TO ERROR-TOTAL  ELSE                          NC1344.2
030400         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      NC1344.2
030500     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   NC1344.2
030600     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1344.2
030700      IF   INSPECT-COUNTER EQUAL TO ZERO                          NC1344.2
030800          MOVE "NO " TO ERROR-TOTAL                               NC1344.2
030900      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   NC1344.2
031000      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            NC1344.2
031100      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          NC1344.2
031200     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1344.2
031300 WRITE-LINE.                                                      NC1344.2
031400     ADD 1 TO RECORD-COUNT.                                       NC1344.2
031500*    IF RECORD-COUNT GREATER 42                                   NC1344.2
031600*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          NC1344.2
031700*        MOVE SPACE TO DUMMY-RECORD                               NC1344.2
031800*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  NC1344.2
031900*        MOVE CCVS-H-1  TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   NC1344.2
032000*        MOVE CCVS-H-2A TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   NC1344.2
032100*        MOVE CCVS-H-2B TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   NC1344.2
032200*        MOVE CCVS-H-3  TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   NC1344.2
032300*        MOVE CCVS-C-1  TO DUMMY-RECORD  PERFORM WRT-LN           NC1344.2
032400*        MOVE CCVS-C-2  TO DUMMY-RECORD  PERFORM WRT-LN           NC1344.2
032500*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          NC1344.2
032600*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          NC1344.2
032700*        MOVE ZERO TO RECORD-COUNT.                               NC1344.2
032800     PERFORM WRT-LN.                                              NC1344.2
032900 WRT-LN.                                                          NC1344.2
033000     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               NC1344.2
033100     MOVE SPACE TO DUMMY-RECORD.                                  NC1344.2
033200 BLANK-LINE-PRINT.                                                NC1344.2
033300     PERFORM WRT-LN.                                              NC1344.2
033400 FAIL-ROUTINE.                                                    NC1344.2
033500     IF     COMPUTED-X NOT EQUAL TO SPACE                         NC1344.2
033600            GO TO FAIL-ROUTINE-WRITE.                             NC1344.2
033700     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.NC1344.2
033800     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC1344.2
033900     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   NC1344.2
034000     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1344.2
034100     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC1344.2
034200     GO TO  FAIL-ROUTINE-EX.                                      NC1344.2
034300 FAIL-ROUTINE-WRITE.                                              NC1344.2
034400     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         NC1344.2
034500     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 NC1344.2
034600     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. NC1344.2
034700     MOVE   SPACES TO COR-ANSI-REFERENCE.                         NC1344.2
034800 FAIL-ROUTINE-EX. EXIT.                                           NC1344.2
034900 BAIL-OUT.                                                        NC1344.2
035000     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   NC1344.2
035100     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           NC1344.2
035200 BAIL-OUT-WRITE.                                                  NC1344.2
035300     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  NC1344.2
035400     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC1344.2
035500     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1344.2
035600     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC1344.2
035700 BAIL-OUT-EX. EXIT.                                               NC1344.2
035800 CCVS1-EXIT.                                                      NC1344.2
035900     EXIT.                                                        NC1344.2
036000 SECT-NC134A-001 SECTION.                                         NC1344.2
036100 NC-05-001.                                                       NC1344.2
036200     MOVE "IV-21 4.3.8.2" TO ANSI-REFERENCE.                      NC1344.2
036300     MOVE 1 TO SUBSCRIPT-1.                                       NC1344.2
036400     MOVE 1 TO W-3.                                               NC1344.2
036500     PERFORM BUILD-TABLE 100 TIMES.                               NC1344.2
036600*    NOTE TABLE IS CONSTRUCTED WITH VALUES FROM 1 TO 100.         NC1344.2
036700     MOVE "SUBSCRIPTING" TO FEATURE.                              NC1344.2
036800 TEST-1.                                                          NC1344.2
036900     IF TAB-ELE (50) EQUAL TO 50 PERFORM PASS ELSE GO TO TST-11.  NC1344.2
037000     GO TO TST-12.                                                NC1344.2
037100 TST-11.                                                          NC1344.2
037200     PERFORM FAIL.                                                NC1344.2
037300     MOVE TAB-ELE (50) TO COMPUTED-A.                             NC1344.2
037400     MOVE "50" TO CORRECT-A.                                      NC1344.2
037500 TST-12.                                                          NC1344.2
037600     MOVE "TEST-1" TO PAR-NAME.                                   NC1344.2
037700     PERFORM PRINT-DETAIL.                                        NC1344.2
037800 TEST-2.                                                          NC1344.2
037900     IF TAB-ELE (TWENTY-5) EQUAL TO 25 PERFORM PASS ELSE GO TO    NC1344.2
038000     TST-21.                                                      NC1344.2
038100     GO TO TST-22.                                                NC1344.2
038200 TST-21.                                                          NC1344.2
038300     PERFORM FAIL.                                                NC1344.2
038400     MOVE TAB-ELE (TWENTY-5) TO COMPUTED-A.                       NC1344.2
038500     MOVE "25" TO CORRECT-A.                                      NC1344.2
038600 TST-22.                                                          NC1344.2
038700     MOVE "TEST-2" TO PAR-NAME.                                   NC1344.2
038800     PERFORM PRINT-DETAIL.                                        NC1344.2
038900 TEST-3.                                                          NC1344.2
039000     IF TAB-ELE (99) EQUAL TO 99 PERFORM PASS ELSE GO TO TST-31.  NC1344.2
039100     GO TO TST-32.                                                NC1344.2
039200 TST-31.                                                          NC1344.2
039300     PERFORM FAIL.                                                NC1344.2
039400     MOVE TAB-ELE (99) TO COMPUTED-A.                             NC1344.2
039500     MOVE "99" TO CORRECT-A.                                      NC1344.2
039600 TST-32.                                                          NC1344.2
039700     MOVE "TEST-3" TO PAR-NAME.                                   NC1344.2
039800     PERFORM PRINT-DETAIL.                                        NC1344.2
039900 WRITE-TABLE-OUT.                                                 NC1344.2
040000     MOVE 1 TO SUBSCRIPT-2.                                       NC1344.2
040100     MOVE 1 TO CTR-1.                                             NC1344.2
040200     PERFORM TABLE-WRITE THRU END-OF 100 TIMES.                   NC1344.2
040300     GO TO CONSTRUCTION.                                          NC1344.2
040400 BUILD-TABLE.                                                     NC1344.2
040500     MOVE W-3 TO TAB-ELE (SUBSCRIPT-1).                           NC1344.2
040600     ADD 1 TO SUBSCRIPT-1.                                        NC1344.2
040700     ADD 1 TO W-3.                                                NC1344.2
040800 TABLE-WRITE.                                                     NC1344.2
040900     MOVE TAB-ELE (SUBSCRIPT-2) TO PRINT-ELE (CTR-1)              NC1344.2
041000     MOVE SPACE TO BLANKSPACE (CTR-1).                            NC1344.2
041100     ADD 1 TO SUBSCRIPT-2.                                        NC1344.2
041200     ADD 1 TO CTR-1.                                              NC1344.2
041300     IF CTR-1 IS EQUAL TO 21 PERFORM TABLE-DUMP.                  NC1344.2
041400 END-OF.                                                          NC1344.2
041500     EXIT.                                                        NC1344.2
041600 TABLE-DUMP.                                                      NC1344.2
041700     MOVE SPACE TO PRINT-REC.                                     NC1344.2
041800     MOVE NUMBER-LIST TO PRINT-REC.                               NC1344.2
041900     PERFORM WRITE-LINE.                                          NC1344.2
042000     MOVE 01 TO CTR-1.                                            NC1344.2
042100 CONSTRUCTION.                                                    NC1344.2
042200     MOVE 1    TO SUBSCRIPT-1.                                    NC1344.2
042300     MOVE 1    TO SUBSCRIPT-2.                                    NC1344.2
042400     MOVE 1 TO SUBSCRIPT-3.                                       NC1344.2
042500     MOVE 1 TO W-3.                                               NC1344.2
042600     PERFORM TABLE-BUILD-2 THROUGH FINE 300 TIMES.                NC1344.2
042700 TABTEST-1.                                                       NC1344.2
042800     IF ANIMAL (1  1  1) EQUAL TO 1 PERFORM PASS ELSE GO TO       NC1344.2
042900     TTST-11.                                                     NC1344.2
043000     GO TO TTST-12.                                               NC1344.2
043100 TTST-11.                                                         NC1344.2
043200     PERFORM FAIL.                                                NC1344.2
043300     MOVE ANIMAL (1  1  1) TO COMPUTED-A.                         NC1344.2
043400     MOVE "001" TO CORRECT-A.                                     NC1344.2
043500 TTST-12.                                                         NC1344.2
043600     MOVE "TABTEST-1" TO PAR-NAME.                                NC1344.2
043700     PERFORM PRINT-DETAIL.                                        NC1344.2
043800 TABTEST-2.                                                       NC1344.2
043900     MOVE 1 TO W-1.                                               NC1344.2
044000     MOVE 1 TO W-2.                                               NC1344.2
044100     MOVE 1 TO W-3.                                               NC1344.2
044200     IF ANIMAL (W-1  W-2  W-3)  EQUAL TO 1   PERFORM PASS ELSE GO NC1344.2
044300     TO TTST-21.                                                  NC1344.2
044400     GO TO TTST-22.                                               NC1344.2
044500 TTST-21.                                                         NC1344.2
044600     PERFORM FAIL.                                                NC1344.2
044700     MOVE ANIMAL (W-1  W-2  W-3) TO COMPUTED-A.                   NC1344.2
044800     MOVE "001" TO CORRECT-A.                                     NC1344.2
044900 TTST-22.                                                         NC1344.2
045000     MOVE "TABTEST-2" TO PAR-NAME.                                NC1344.2
045100     PERFORM PRINT-DETAIL.                                        NC1344.2
045200 TABTEST-3.                                                       NC1344.2
045300     MOVE 3 TO W-3.                                               NC1344.2
045400     MOVE 3 TO W-2.                                               NC1344.2
045500     MOVE 1 TO W-1.                                               NC1344.2
045600     IF ANIMAL (W-1  W-2  W-3) EQUAL TO 9 PERFORM PASS ELSE GO    NC1344.2
045700     TO TTST-31.                                                  NC1344.2
045800     GO TO TTST-32.                                               NC1344.2
045900 TTST-31.                                                         NC1344.2
046000     PERFORM FAIL.                                                NC1344.2
046100     MOVE ANIMAL (W-1  W-2  W-3) TO COMPUTED-A.                   NC1344.2
046200     MOVE "009" TO CORRECT-A.                                     NC1344.2
046300 TTST-32.                                                         NC1344.2
046400     MOVE "TABTEST-3" TO PAR-NAME.                                NC1344.2
046500     PERFORM PRINT-DETAIL.                                        NC1344.2
046600 TABTEST-4.                                                       NC1344.2
046700     IF ANIMAL (10  10  1)    EQUAL TO 298 PERFORM PASS ELSE GO TONC1344.2
046800     TTST-41.                                                     NC1344.2
046900     GO TO TTST-42.                                               NC1344.2
047000 TTST-41.                                                         NC1344.2
047100     PERFORM FAIL.                                                NC1344.2
047200     MOVE ANIMAL (10  10  1) TO COMPUTED-A.                       NC1344.2
047300     MOVE "298" TO CORRECT-A.                                     NC1344.2
047400 TTST-42.                                                         NC1344.2
047500     MOVE "TABTEST-4" TO PAR-NAME.                                NC1344.2
047600     PERFORM PRINT-DETAIL.                                        NC1344.2
047700 TABTEST-5.                                                       NC1344.2
047800     MOVE 3 TO W-1.                                               NC1344.2
047900     MOVE 10 TO W-2.                                              NC1344.2
048000     MOVE 10 TO W-3.                                              NC1344.2
048100     IF ANIMAL (W-3  W-2  W-1) EQUAL TO 300 PERFORM PASS ELSE GO  NC1344.2
048200     TO TTST-51.                                                  NC1344.2
048300     GO TO TTST-52.                                               NC1344.2
048400 TTST-51.                                                         NC1344.2
048500     PERFORM FAIL.                                                NC1344.2
048600     MOVE ANIMAL (W-3  W-2  W-1) TO COMPUTED-A.                   NC1344.2
048700     MOVE "300" TO CORRECT-A.                                     NC1344.2
048800 TTST-52.                                                         NC1344.2
048900     MOVE "TABTEST-5" TO PAR-NAME.                                NC1344.2
049000     PERFORM PRINT-DETAIL.                                        NC1344.2
049100 TABTEST-6.                                                       NC1344.2
049200      IF YEAR-1 (1  1) EQUAL TO "001002003" PERFORM PASS ELSE GO  NC1344.2
049300     TO TTST-61.                                                  NC1344.2
049400     GO TO TTST-62.                                               NC1344.2
049500 TTST-61.                                                         NC1344.2
049600     PERFORM FAIL.                                                NC1344.2
049700     MOVE YEAR-1 (1  1) TO COMPUTED-A.                            NC1344.2
049800     MOVE "001002003" TO CORRECT-A.                               NC1344.2
049900 TTST-62.                                                         NC1344.2
050000     MOVE "TABTEST-6" TO PAR-NAME.                                NC1344.2
050100     PERFORM PRINT-DETAIL.                                        NC1344.2
050200 TABTEST-7.                                                       NC1344.2
050300      IF YEAR-1 (10  10) EQUAL TO "298299300" PERFORM PASS ELSE GONC1344.2
050400     TO TTST-71.                                                  NC1344.2
050500     GO TO TTST-72.                                               NC1344.2
050600 TTST-71.                                                         NC1344.2
050700     MOVE YEAR-1 (10  10) TO COMPUTED-A.                          NC1344.2
050800     MOVE "298299300" TO CORRECT-A.                               NC1344.2
050900     PERFORM FAIL.                                                NC1344.2
051000 TTST-72.                                                         NC1344.2
051100     MOVE "TABTEST-7" TO PAR-NAME.                                NC1344.2
051200     PERFORM PRINT-DETAIL.                                        NC1344.2
051300 TABTEST-8.                                                       NC1344.2
051400     MOVE 02 TO W-1.                                              NC1344.2
051500     MOVE 07  TO W-2.                                             NC1344.2
051600     IF ANIMAL (W-1  W-2  1) EQUAL TO 49 PERFORM PASS ELSE GO TO  NC1344.2
051700     TTST-81.                                                     NC1344.2
051800     GO TO TTST-82.                                               NC1344.2
051900 TTST-81.                                                         NC1344.2
052000     PERFORM FAIL.                                                NC1344.2
052100     MOVE ANIMAL (W-1  W-2  1) TO COMPUTED-A.                     NC1344.2
052200     MOVE "049" TO CORRECT-A.                                     NC1344.2
052300 TTST-82.                                                         NC1344.2
052400     MOVE "TABTEST-8" TO PAR-NAME.                                NC1344.2
052500     PERFORM PRINT-DETAIL.                                        NC1344.2
052600 TABTEST-9.                                                       NC1344.2
052700     MOVE 08 TO W-1.                                              NC1344.2
052800     MOVE 03 TO W-3.                                              NC1344.2
052900     IF ANIMAL (W-1  1  W-3) EQUAL TO 213 PERFORM PASS ELSE GO TO NC1344.2
053000     TTST-91.                                                     NC1344.2
053100     GO TO TTST-92.                                               NC1344.2
053200 TTST-91.                                                         NC1344.2
053300     PERFORM FAIL.                                                NC1344.2
053400     MOVE ANIMAL (W-1  1  W-3) TO COMPUTED-A.                     NC1344.2
053500     MOVE "213" TO CORRECT-A.                                     NC1344.2
053600 TTST-92.                                                         NC1344.2
053700     MOVE "TABTEST-9" TO PAR-NAME.                                NC1344.2
053800     PERFORM PRINT-DETAIL.                                        NC1344.2
053900 TABTEST-10.                                                      NC1344.2
054000     MOVE 5 TO W-1.                                               NC1344.2
054100     IF YEAR-1 (W-1  10) EQUAL TO "148149150" PERFORM PASS ELSE   NC1344.2
054200     GO TO TTST-101.                                              NC1344.2
054300     GO TO TTST-102.                                              NC1344.2
054400 TTST-101.                                                        NC1344.2
054500     PERFORM FAIL.                                                NC1344.2
054600     MOVE     YEAR-1 (W-1  10) TO COMPUTED-A.                     NC1344.2
054700     MOVE "148149150" TO CORRECT-A.                               NC1344.2
054800 TTST-102.                                                        NC1344.2
054900     MOVE "TABTEST-10" TO PAR-NAME.                               NC1344.2
055000     PERFORM PRINT-DETAIL.                                        NC1344.2
055100 TABTEST-11.                                                      NC1344.2
055200     IF YEAR-1 (+10  +10) EQUAL TO "298299300"                    NC1344.2
055300             PERFORM PASS                                         NC1344.2
055400             GO TO TABTEST-11B.                                   NC1344.2
055500     MOVE YEAR-1 (+10  +10) TO COMPUTED-A.                        NC1344.2
055600     MOVE "298299300" TO CORRECT-A.                               NC1344.2
055700     PERFORM FAIL.                                                NC1344.2
055800     GO TO TABTEST-11B.                                           NC1344.2
055900 TABTEST-11A.                                                     NC1344.2
056000     PERFORM DE-LETE.                                             NC1344.2
056100 TABTEST-11B.                                                     NC1344.2
056200     MOVE "TABTEST-11" TO PAR-NAME.                               NC1344.2
056300*    NOTE SIGNED NUMERIC LITERALS AS SUBSCRIPTS.                  NC1344.2
056400     PERFORM PRINT-DETAIL.                                        NC1344.2
056500 TABTEST-12.                                                      NC1344.2
056600     MOVE 1 TO W-1.                                               NC1344.2
056700     IF YEAR-1 (W-1  +1) EQUAL TO "001002003"                     NC1344.2
056800             PERFORM PASS                                         NC1344.2
056900             GO TO TABTEST-12B.                                   NC1344.2
057000     MOVE YEAR-1 (W-1  +1) TO COMPUTED-A.                         NC1344.2
057100     MOVE "001002003" TO CORRECT-A.                               NC1344.2
057200     PERFORM FAIL.                                                NC1344.2
057300     GO TO TABTEST-12B.                                           NC1344.2
057400 TABTEST-12A.                                                     NC1344.2
057500     PERFORM DE-LETE.                                             NC1344.2
057600 TABTEST-12B.                                                     NC1344.2
057700     MOVE "TABTEST-12" TO PAR-NAME.                               NC1344.2
057800*    NOTE SIGNED NUMERIC LITERAL AND                              NC1344.2
057900*          UNSIGNED NUMERIC ITEM AS SUBSCRIPTS.                   NC1344.2
058000     PERFORM PRINT-DETAIL.                                        NC1344.2
058100 TABTEST-13.                                                      NC1344.2
058200     IF ANIMAL (+8  +1  +3) EQUAL TO 213                          NC1344.2
058300             PERFORM PASS                                         NC1344.2
058400             GO TO TABTEST-13B.                                   NC1344.2
058500     MOVE ANIMAL (+8  +1   +3) TO COMPUTED-A.                     NC1344.2
058600     MOVE "213" TO CORRECT-A.                                     NC1344.2
058700     PERFORM FAIL.                                                NC1344.2
058800     GO TO TABTEST-13B.                                           NC1344.2
058900 TABTEST-13A.                                                     NC1344.2
059000     PERFORM DE-LETE.                                             NC1344.2
059100 TABTEST-13B.                                                     NC1344.2
059200     MOVE "TABTEST-13" TO PAR-NAME.                               NC1344.2
059300*    NOTE SIGNED NUMERIC LITERALS AS SUBSCRIPTS.                  NC1344.2
059400     PERFORM PRINT-DETAIL.                                        NC1344.2
059500 TABTEST-14.                                                      NC1344.2
059600     MOVE 1 TO W-2.                                               NC1344.2
059700     IF ANIMAL (+8  W-2  +3) EQUAL TO 213                         NC1344.2
059800             PERFORM PASS                                         NC1344.2
059900             GO TO TABTEST-14B.                                   NC1344.2
060000     MOVE ANIMAL (+8  W-2  +3) TO COMPUTED-A.                     NC1344.2
060100     MOVE "213" TO CORRECT-A.                                     NC1344.2
060200     PERFORM FAIL.                                                NC1344.2
060300     GO TO TABTEST-14B.                                           NC1344.2
060400 TABTEST-14A.                                                     NC1344.2
060500     PERFORM DE-LETE.                                             NC1344.2
060600 TABTEST-14B.                                                     NC1344.2
060700     MOVE "TABTEST-14" TO PAR-NAME.                               NC1344.2
060800*    NOTE SIGNED NUMERIC LITERALS AND                             NC1344.2
060900*         UNSIGNED NUMERIC ITEM AS SUBSCRIPTS.                    NC1344.2
061000     PERFORM PRINT-DETAIL.                                        NC1344.2
061100 TABTEST-15.                                                      NC1344.2
061200     MOVE 8 TO W-1.                                               NC1344.2
061300     MOVE 3 TO W-3.                                               NC1344.2
061400     IF ANIMAL (W-1  +1  W-3) EQUAL TO 213                        NC1344.2
061500             PERFORM PASS                                         NC1344.2
061600             GO TO TABTEST-15B.                                   NC1344.2
061700     MOVE ANIMAL (W-1  +1  W-3) TO COMPUTED-A.                    NC1344.2
061800     MOVE "213" TO CORRECT-A.                                     NC1344.2
061900     PERFORM FAIL.                                                NC1344.2
062000     GO TO TABTEST-15B.                                           NC1344.2
062100 TABTEST-15A.                                                     NC1344.2
062200     PERFORM DE-LETE.                                             NC1344.2
062300 TABTEST-15B.                                                     NC1344.2
062400     MOVE "TABTEST-15" TO PAR-NAME.                               NC1344.2
062500*    NOTE UNSIGNED NUMERIC ITEMS AND SIGNED NUMERIC LITERAL       NC1344.2
062600     PERFORM PRINT-DETAIL.                                        NC1344.2
062700 TABINIT-16.                                                      NC1344.2
062800*    ==-->  RELATIVE SUBSCRIPTING  <--==                          NC1344.2
062900     MOVE   "IV-22 4.3.8.4 GR4" TO ANSI-REFERENCE.                NC1344.2
063000     MOVE 1  TO W-1.                                              NC1344.2
063100     MOVE 20 TO W-2.                                              NC1344.2
063200     MOVE 5  TO W-3.                                              NC1344.2
063300 TABTEST-16.                                                      NC1344.2
063400     IF      ANIMAL (W-3 + 5  W-2 - 10  W-1 + 2) = 300            NC1344.2
063500             PERFORM PASS                                         NC1344.2
063600     ELSE                                                         NC1344.2
063700             GO TO TTST-161.                                      NC1344.2
063800     GO TO TTST-162.                                              NC1344.2
063900 TTST-161.                                                        NC1344.2
064000     PERFORM FAIL.                                                NC1344.2
064100     MOVE    ANIMAL (W-3 + 5  W-2 - 10  W-1 + 2) TO COMPUTED-A.   NC1344.2
064200     MOVE "300" TO CORRECT-A.                                     NC1344.2
064300 TTST-162.                                                        NC1344.2
064400     MOVE "TABTEST-16" TO PAR-NAME.                               NC1344.2
064500     PERFORM PRINT-DETAIL.                                        NC1344.2
064600 TABINIT-17.                                                      NC1344.2
064700*    ==-->  RELATIVE SUBSCRIPTING  <--==                          NC1344.2
064800     MOVE   "IV-22 4.3.8.4 GR4" TO ANSI-REFERENCE.                NC1344.2
064900     MOVE   9 TO W-1.                                             NC1344.2
065000     MOVE   6 TO W-2.                                             NC1344.2
065100     MOVE 999 TO W-3.                                             NC1344.2
065200 TABTEST-17.                                                      NC1344.2
065300     IF      ANIMAL (W-1 - 7  W-2 + 1 W-3 - 998) EQUAL TO 49      NC1344.2
065400             PERFORM PASS                                         NC1344.2
065500     ELSE                                                         NC1344.2
065600             GO TO   TTST-171.                                    NC1344.2
065700     GO TO   TTST-172.                                            NC1344.2
065800 TTST-171.                                                        NC1344.2
065900     PERFORM FAIL.                                                NC1344.2
066000     MOVE    ANIMAL (W-1 - 7  W-2 + 1 W-3 - 998) TO COMPUTED-A.   NC1344.2
066100     MOVE "049" TO CORRECT-A.                                     NC1344.2
066200 TTST-172.                                                        NC1344.2
066300     MOVE "TABTEST-17" TO PAR-NAME.                               NC1344.2
066400     PERFORM PRINT-DETAIL.                                        NC1344.2
066500*    USED AS SUBSCRIPT.                                           NC1344.2
066600 WRITE-TABLE.                                                     NC1344.2
066700     PERFORM BLANK-LINE-PRINT 2 TIMES.                            NC1344.2
066800     MOVE 1 TO SUBSCRIPT-3                                        NC1344.2
066900     MOVE  1  TO SUBSCRIPT-2.                                     NC1344.2
067000     MOVE  1  TO  SUBSCRIPT-1.                                    NC1344.2
067100     MOVE 1 TO CTR-1                                              NC1344.2
067200     PERFORM PRINT-TABLE THROUGH END-TAB 300 TIMES.               NC1344.2
067300     GO TO EXIT-NOTE.                                             NC1344.2
067400 EXIT-NOTE.                                                       NC1344.2
067500     GO TO FIN-WRAPUP.                                            NC1344.2
067600 PRINT-TABLE.                                                     NC1344.2
067700     MOVE ANIMAL (SUBSCRIPT-1  SUBSCRIPT-2  SUBSCRIPT-3) TO       NC1344.2
067800     PRINT-ELE (CTR-1).                                           NC1344.2
067900     ADD 1 TO CTR-1                                               NC1344.2
068000     IF CTR-1 EQUAL TO 21 PERFORM TABLE-DUMP.                     NC1344.2
068100     ADD 1 TO SUBSCRIPT-3.                                        NC1344.2
068200     IF SUBSCRIPT-3 GREATER THAN 3 GO TO CCCC ELSE GO TO END-TAB. NC1344.2
068300 CCCC.                                                            NC1344.2
068400     ADD 1 TO  SUBSCRIPT-2                                        NC1344.2
068500     MOVE 1 TO SUBSCRIPT-3.                                       NC1344.2
068600     IF SUBSCRIPT-2 GREATER THAN 10 GO TO DDD ELSE GO TO END-TAB. NC1344.2
068700 DDD.                                                             NC1344.2
068800     ADD 1 TO SUBSCRIPT-1.                                        NC1344.2
068900     MOVE 1 TO SUBSCRIPT-2.                                       NC1344.2
069000 END-TAB.                                                         NC1344.2
069100     EXIT.                                                        NC1344.2
069200                                                                  NC1344.2
069300 TABLE-BUILD-2.                                                   NC1344.2
069400     MOVE W-3 TO ANIMAL (SUBSCRIPT-1  SUBSCRIPT-2  SUBSCRIPT-3).  NC1344.2
069500     ADD 01 TO W-3.                                               NC1344.2
069600     ADD 01 TO SUBSCRIPT-3.                                       NC1344.2
069700     IF SUBSCRIPT-3 IS GREATER THAN 3 GO TO   AAAA ELSE GO TO     NC1344.2
069800     FINE.                                                        NC1344.2
069900 AAAA.                                                            NC1344.2
070000     ADD 1 TO SUBSCRIPT-2.                                        NC1344.2
070100     MOVE  1   TO SUBSCRIPT-3.                                    NC1344.2
070200     IF SUBSCRIPT-2 IS GREATER THAN 10 GO TO BBB ELSE GO TO FINE. NC1344.2
070300 BBB.                                                             NC1344.2
070400     ADD 1 TO SUBSCRIPT-1.                                        NC1344.2
070500     MOVE   1   TO  SUBSCRIPT-2.                                  NC1344.2
070600 FINE.                                                            NC1344.2
070700     EXIT.                                                        NC1344.2
070800 FIN-WRAPUP.                                                      NC1344.2
070900     EXIT.                                                        NC1344.2
071000 END-JOB.                                                         NC1344.2
071100 CCVS-EXIT SECTION.                                               NC1344.2
071200 CCVS-999999.                                                     NC1344.2
071300     GO TO CLOSE-FILES.                                           NC1344.2
      *END-OF,NC134A                                                                  
