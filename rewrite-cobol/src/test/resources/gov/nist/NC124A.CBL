      *HEADER,COBOL,NC124A                                                            
000100 IDENTIFICATION DIVISION.                                         NC1244.2
000200 PROGRAM-ID.                                                      NC1244.2
000300                                                                  NC1244.2
000400     NC124A.                                                      NC1244.2
000500****************************************************************  NC1244.2
000600*                                                              *  NC1244.2
000700*    VALIDATION FOR:-                                          *  NC1244.2
000800*                                                              *  NC1244.2
000900*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1244.2
001000*                                                              *  NC1244.2
001100*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC1244.2
001200*                                                              *  NC1244.2
001300****************************************************************  NC1244.2
001400*                                                              *  NC1244.2
001500*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  NC1244.2
001600*                                                              *  NC1244.2
001700*        X-55  - SYSTEM PRINTER NAME.                          *  NC1244.2
001800*        X-82  - SOURCE COMPUTER NAME.                         *  NC1244.2
001900*        X-83  - OBJECT COMPUTER NAME.                         *  NC1244.2
002000*                                                              *  NC1244.2
002100****************************************************************  NC1244.2
002200*                                                                 NC1244.2
002300*               PROGRAM NC124A TESTS THE USE OF                   NC1244.2
002400*            PICTURE CHARACTERS P, S, +, -, Z AND *.              NC1244.2
002500*                                                                 NC1244.2
002600 ENVIRONMENT DIVISION.                                            NC1244.2
002700 CONFIGURATION SECTION.                                           NC1244.2
002800 SOURCE-COMPUTER.                                                 NC1244.2
002900     XXXXX082.                                                    NC1244.2
003000 OBJECT-COMPUTER.                                                 NC1244.2
003100     XXXXX083.                                                    NC1244.2
003200 INPUT-OUTPUT SECTION.                                            NC1244.2
003300 FILE-CONTROL.                                                    NC1244.2
003400     SELECT PRINT-FILE ASSIGN TO                                  NC1244.2
003500     XXXXX055.                                                    NC1244.2
003600 DATA DIVISION.                                                   NC1244.2
003700 FILE SECTION.                                                    NC1244.2
003800 FD  PRINT-FILE.                                                  NC1244.2
003900 01  PRINT-REC PICTURE X(120).                                    NC1244.2
004000 01  DUMMY-RECORD PICTURE X(120).                                 NC1244.2
004100 WORKING-STORAGE SECTION.                                         NC1244.2
004200 01  WORK-AREA-1                 PICTURE 9       VALUE 0.         NC1244.2
004300 01  WORK-AREA-2                 PICTURE 99      VALUE 12.        NC1244.2
004400 01  WORK-AREA-3                 PICTURE S99     VALUE -12.       NC1244.2
004500 01  WORK-AREA-4                 PICTURE S999    VALUE 123.       NC1244.2
004600 01  WORK-AREA-5                 PICTURE S999    VALUE -123.      NC1244.2
004700 01  WORK-AREA-6                 PICTURE 9999    VALUE 1234.      NC1244.2
004800 01  WORK-AREA-7                 PICTURE S9999   VALUE -1234.     NC1244.2
004900 01  WORK-AREA-8                 PICTURE S99V99  VALUE 12.34.     NC1244.2
005000 01  WORK-AREA-9                 PICTURE S99V99  VALUE -12.34.    NC1244.2
005100 01  WORK-AREA-10                PICTURE 9       VALUE 0.         NC1244.2
005200 01  WORK-AREA-11                PICTURE V99     VALUE .02.       NC1244.2
005300 01  WORK-AREA-12                PICTURE 99      VALUE 12.        NC1244.2
005400 01  WORK-AREA-13                PICTURE 99V99   VALUE 12.34.     NC1244.2
005500 01  WORK-AREA-14                PICTURE 9999    VALUE 1234.      NC1244.2
005600 01  WORK-AREA-15                PICTURE 9999V99 VALUE 1234.56.   NC1244.2
005700 01  WORK-AREA-16                PICTURE 9       VALUE 0.         NC1244.2
005800 01  WORK-AREA-17                PICTURE 99      VALUE 13.        NC1244.2
005900 01  WORK-AREA-18                PICTURE 999     VALUE 123.       NC1244.2
006000 01  WORK-AREA-19                PICTURE 9999    VALUE 2010.      NC1244.2
006100 01  WORK-AREA-20                PICTURE 9999V9  VALUE 1010.2.    NC1244.2
006200 01  WORK-AREA-21                PICTURE V99     VALUE .01.       NC1244.2
006300 01  WORK-AREA-22                PICTURE 9       VALUE 0.         NC1244.2
006400 01  WORK-AREA-23                PICTURE 9V99    VALUE 1.01.      NC1244.2
006500 01  WORK-AREA-24                PICTURE 999V    VALUE 217.       NC1244.2
006600 01  WORK-AREA-25                PICTURE 9999V99 VALUE 1010.20.   NC1244.2
006700 01  WORK-AREA-26                PICTURE V99     VALUE .01.       NC1244.2
006800 01  WORK-AREA-27                PICTURE S9PP    VALUE 200.       NC1244.2
006900 01  WORK-AREA-27A               PICTURE X(3)    VALUE SPACE.     NC1244.2
007000 01  WORK-AREA-28                PICTURE 999     VALUE 567.       NC1244.2
007100 01  WORK-AREA-28A               PICTURE S9PP    VALUE ZERO.      NC1244.2
007200 01  WORK-AREA-29                PICTURE 999     VALUE 123.       NC1244.2
007300 01  WORK-AREA-29A               PICTURE 9PP     VALUE ZERO.      NC1244.2
007400 01  WORK-AREA-29B               PICTURE X(3)    VALUE SPACE.     NC1244.2
007500 01  WORK-AREA-30                PICTURE 999PP   VALUE 00900.     NC1244.2
007600 01  WORK-AREA-30A               PICTURE ZZZPP   VALUE ZERO.      NC1244.2
007700 01  WORK-AREA-31                PICTURE 999PP   VALUE 01200.     NC1244.2
007800 01  WORK-AREA-31A               PICTURE ZZZPP   VALUE ZERO.      NC1244.2
007900 01  WORK-AREA-31B               PICTURE X(5)    VALUE SPACE.     NC1244.2
008000 01  WORK-AREA-32                PICTURE PP9     VALUE .001.      NC1244.2
008100 01  WORK-AREA-32A               PICTURE V999    VALUE ZERO.      NC1244.2
008200 01  WORK-AREA-33                PICTURE V999    VALUE .567.      NC1244.2
008300 01  WORK-AREA-33A               PICTURE PP9     VALUE ZERO.      NC1244.2
008400 01  WORK-AREA-34                PICTURE V999    VALUE .123.      NC1244.2
008500 01  WORK-AREA-34A               PICTURE PP9     VALUE ZERO.      NC1244.2
008600 01  WORK-AREA-34B               PICTURE V999    VALUE ZERO.      NC1244.2
008700 01  EDIT-AREA-1                 PICTURE +9999.                   NC1244.2
008800 01  EDIT-AREA-2                 PICTURE -9999.                   NC1244.2
008900 01  EDIT-AREA-3                 PICTURE ++++9.                   NC1244.2
009000 01  EDIT-AREA-4                 PICTURE ----9.                   NC1244.2
009100 01  EDIT-AREA-5                 PICTURE +++++.                   NC1244.2
009200 01  EDIT-AREA-6                 PICTURE -----.                   NC1244.2
009300 01  EDIT-AREA-7                 PICTURE +++++.++.                NC1244.2
009400 01  EDIT-AREA-8                 PICTURE --,---.--.               NC1244.2
009500 01  EDIT-AREA-9                 PICTURE $$99.                    NC1244.2
009600 01  EDIT-AREA-10                PICTURE $$$$9.                   NC1244.2
009700 01  EDIT-AREA-11                PICTURE $$$$$.99.                NC1244.2
009800 01  EDIT-AREA-12                PICTURE $$,$$$.$$.               NC1244.2
009900 01  EDIT-AREA-13                PICTURE *999.                    NC1244.2
010000 01  EDIT-AREA-14                PICTURE **99.                    NC1244.2
010100 01  EDIT-AREA-15                PICTURE ***9.                    NC1244.2
010200 01  EDIT-AREA-16                PICTURE **.**.                   NC1244.2
010300 01  EDIT-AREA-17                PICTURE *,***.**.                NC1244.2
010400 01  EDIT-AREA-18                PICTURE 9999.                    NC1244.2
010500 01  EDIT-AREA-19                PICTURE Z999.                    NC1244.2
010600 01  EDIT-AREA-20                PICTURE ZZ99.                    NC1244.2
010700 01  EDIT-AREA-21                PICTURE ZZZ9.                    NC1244.2
010800 01  EDIT-AREA-22                PICTURE ZZZZ.                    NC1244.2
010900 01  EDIT-AREA-23                PICTURE ZZ.ZZ.                   NC1244.2
011000 01  EDIT-AREA-24                PICTURE Z,ZZZ.                   NC1244.2
011100 01  TEST-RESULTS.                                                NC1244.2
011200     02 FILLER                   PIC X      VALUE SPACE.          NC1244.2
011300     02 FEATURE                  PIC X(20)  VALUE SPACE.          NC1244.2
011400     02 FILLER                   PIC X      VALUE SPACE.          NC1244.2
011500     02 P-OR-F                   PIC X(5)   VALUE SPACE.          NC1244.2
011600     02 FILLER                   PIC X      VALUE SPACE.          NC1244.2
011700     02  PAR-NAME.                                                NC1244.2
011800       03 FILLER                 PIC X(19)  VALUE SPACE.          NC1244.2
011900       03  PARDOT-X              PIC X      VALUE SPACE.          NC1244.2
012000       03 DOTVALUE               PIC 99     VALUE ZERO.           NC1244.2
012100     02 FILLER                   PIC X(8)   VALUE SPACE.          NC1244.2
012200     02 RE-MARK                  PIC X(61).                       NC1244.2
012300 01  TEST-COMPUTED.                                               NC1244.2
012400     02 FILLER                   PIC X(30)  VALUE SPACE.          NC1244.2
012500     02 FILLER                   PIC X(17)  VALUE                 NC1244.2
012600            "       COMPUTED=".                                   NC1244.2
012700     02 COMPUTED-X.                                               NC1244.2
012800     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          NC1244.2
012900     03 COMPUTED-N               REDEFINES COMPUTED-A             NC1244.2
013000                                 PIC -9(9).9(9).                  NC1244.2
013100     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         NC1244.2
013200     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     NC1244.2
013300     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     NC1244.2
013400     03       CM-18V0 REDEFINES COMPUTED-A.                       NC1244.2
013500         04 COMPUTED-18V0                    PIC -9(18).          NC1244.2
013600         04 FILLER                           PIC X.               NC1244.2
013700     03 FILLER PIC X(50) VALUE SPACE.                             NC1244.2
013800 01  TEST-CORRECT.                                                NC1244.2
013900     02 FILLER PIC X(30) VALUE SPACE.                             NC1244.2
014000     02 FILLER PIC X(17) VALUE "       CORRECT =".                NC1244.2
014100     02 CORRECT-X.                                                NC1244.2
014200     03 CORRECT-A                  PIC X(20) VALUE SPACE.         NC1244.2
014300     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      NC1244.2
014400     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         NC1244.2
014500     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     NC1244.2
014600     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     NC1244.2
014700     03      CR-18V0 REDEFINES CORRECT-A.                         NC1244.2
014800         04 CORRECT-18V0                     PIC -9(18).          NC1244.2
014900         04 FILLER                           PIC X.               NC1244.2
015000     03 FILLER PIC X(2) VALUE SPACE.                              NC1244.2
015100     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     NC1244.2
015200 01  CCVS-C-1.                                                    NC1244.2
015300     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PANC1244.2
015400-    "SS  PARAGRAPH-NAME                                          NC1244.2
015500-    "       REMARKS".                                            NC1244.2
015600     02 FILLER                     PIC X(20)    VALUE SPACE.      NC1244.2
015700 01  CCVS-C-2.                                                    NC1244.2
015800     02 FILLER                     PIC X        VALUE SPACE.      NC1244.2
015900     02 FILLER                     PIC X(6)     VALUE "TESTED".   NC1244.2
016000     02 FILLER                     PIC X(15)    VALUE SPACE.      NC1244.2
016100     02 FILLER                     PIC X(4)     VALUE "FAIL".     NC1244.2
016200     02 FILLER                     PIC X(94)    VALUE SPACE.      NC1244.2
016300 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       NC1244.2
016400 01  REC-CT                        PIC 99       VALUE ZERO.       NC1244.2
016500 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       NC1244.2
016600 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       NC1244.2
016700 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       NC1244.2
016800 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       NC1244.2
016900 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       NC1244.2
017000 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       NC1244.2
017100 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      NC1244.2
017200 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       NC1244.2
017300 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     NC1244.2
017400 01  CCVS-H-1.                                                    NC1244.2
017500     02  FILLER                    PIC X(39)    VALUE SPACES.     NC1244.2
017600     02  FILLER                    PIC X(42)    VALUE             NC1244.2
017700     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 NC1244.2
017800     02  FILLER                    PIC X(39)    VALUE SPACES.     NC1244.2
017900 01  CCVS-H-2A.                                                   NC1244.2
018000   02  FILLER                        PIC X(40)  VALUE SPACE.      NC1244.2
018100   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  NC1244.2
018200   02  FILLER                        PIC XXXX   VALUE             NC1244.2
018300     "4.2 ".                                                      NC1244.2
018400   02  FILLER                        PIC X(28)  VALUE             NC1244.2
018500            " COPY - NOT FOR DISTRIBUTION".                       NC1244.2
018600   02  FILLER                        PIC X(41)  VALUE SPACE.      NC1244.2
018700                                                                  NC1244.2
018800 01  CCVS-H-2B.                                                   NC1244.2
018900   02  FILLER                        PIC X(15)  VALUE             NC1244.2
019000            "TEST RESULT OF ".                                    NC1244.2
019100   02  TEST-ID                       PIC X(9).                    NC1244.2
019200   02  FILLER                        PIC X(4)   VALUE             NC1244.2
019300            " IN ".                                               NC1244.2
019400   02  FILLER                        PIC X(12)  VALUE             NC1244.2
019500     " HIGH       ".                                              NC1244.2
019600   02  FILLER                        PIC X(22)  VALUE             NC1244.2
019700            " LEVEL VALIDATION FOR ".                             NC1244.2
019800   02  FILLER                        PIC X(58)  VALUE             NC1244.2
019900     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1244.2
020000 01  CCVS-H-3.                                                    NC1244.2
020100     02  FILLER                      PIC X(34)  VALUE             NC1244.2
020200            " FOR OFFICIAL USE ONLY    ".                         NC1244.2
020300     02  FILLER                      PIC X(58)  VALUE             NC1244.2
020400     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC1244.2
020500     02  FILLER                      PIC X(28)  VALUE             NC1244.2
020600            "  COPYRIGHT   1985 ".                                NC1244.2
020700 01  CCVS-E-1.                                                    NC1244.2
020800     02 FILLER                       PIC X(52)  VALUE SPACE.      NC1244.2
020900     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              NC1244.2
021000     02 ID-AGAIN                     PIC X(9).                    NC1244.2
021100     02 FILLER                       PIC X(45)  VALUE SPACES.     NC1244.2
021200 01  CCVS-E-2.                                                    NC1244.2
021300     02  FILLER                      PIC X(31)  VALUE SPACE.      NC1244.2
021400     02  FILLER                      PIC X(21)  VALUE SPACE.      NC1244.2
021500     02 CCVS-E-2-2.                                               NC1244.2
021600         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      NC1244.2
021700         03 FILLER                   PIC X      VALUE SPACE.      NC1244.2
021800         03 ENDER-DESC               PIC X(44)  VALUE             NC1244.2
021900            "ERRORS ENCOUNTERED".                                 NC1244.2
022000 01  CCVS-E-3.                                                    NC1244.2
022100     02  FILLER                      PIC X(22)  VALUE             NC1244.2
022200            " FOR OFFICIAL USE ONLY".                             NC1244.2
022300     02  FILLER                      PIC X(12)  VALUE SPACE.      NC1244.2
022400     02  FILLER                      PIC X(58)  VALUE             NC1244.2
022500     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1244.2
022600     02  FILLER                      PIC X(13)  VALUE SPACE.      NC1244.2
022700     02 FILLER                       PIC X(15)  VALUE             NC1244.2
022800             " COPYRIGHT 1985".                                   NC1244.2
022900 01  CCVS-E-4.                                                    NC1244.2
023000     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      NC1244.2
023100     02 FILLER                       PIC X(4)   VALUE " OF ".     NC1244.2
023200     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      NC1244.2
023300     02 FILLER                       PIC X(40)  VALUE             NC1244.2
023400      "  TESTS WERE EXECUTED SUCCESSFULLY".                       NC1244.2
023500 01  XXINFO.                                                      NC1244.2
023600     02 FILLER                       PIC X(19)  VALUE             NC1244.2
023700            "*** INFORMATION ***".                                NC1244.2
023800     02 INFO-TEXT.                                                NC1244.2
023900       04 FILLER                     PIC X(8)   VALUE SPACE.      NC1244.2
024000       04 XXCOMPUTED                 PIC X(20).                   NC1244.2
024100       04 FILLER                     PIC X(5)   VALUE SPACE.      NC1244.2
024200       04 XXCORRECT                  PIC X(20).                   NC1244.2
024300     02 INF-ANSI-REFERENCE           PIC X(48).                   NC1244.2
024400 01  HYPHEN-LINE.                                                 NC1244.2
024500     02 FILLER  PIC IS X VALUE IS SPACE.                          NC1244.2
024600     02 FILLER  PIC IS X(65)    VALUE IS "************************NC1244.2
024700-    "*****************************************".                 NC1244.2
024800     02 FILLER  PIC IS X(54)    VALUE IS "************************NC1244.2
024900-    "******************************".                            NC1244.2
025000 01  CCVS-PGM-ID                     PIC X(9)   VALUE             NC1244.2
025100     "NC124A".                                                    NC1244.2
025200 PROCEDURE DIVISION.                                              NC1244.2
025300 CCVS1 SECTION.                                                   NC1244.2
025400 OPEN-FILES.                                                      NC1244.2
025500     OPEN     OUTPUT PRINT-FILE.                                  NC1244.2
025600     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   NC1244.2
025700     MOVE    SPACE TO TEST-RESULTS.                               NC1244.2
025800     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             NC1244.2
025900     GO TO CCVS1-EXIT.                                            NC1244.2
026000 CLOSE-FILES.                                                     NC1244.2
026100     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   NC1244.2
026200 TERMINATE-CCVS.                                                  NC1244.2
026300*    EXIT PROGRAM.                                                NC1244.2
026400*TERMINATE-CALL.                                                  NC1244.2
026500     STOP     RUN.                                                NC1244.2
026600 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         NC1244.2
026700 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           NC1244.2
026800 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          NC1244.2
026900 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      NC1244.2
027000     MOVE "****TEST DELETED****" TO RE-MARK.                      NC1244.2
027100 PRINT-DETAIL.                                                    NC1244.2
027200     IF REC-CT NOT EQUAL TO ZERO                                  NC1244.2
027300             MOVE "." TO PARDOT-X                                 NC1244.2
027400             MOVE REC-CT TO DOTVALUE.                             NC1244.2
027500     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      NC1244.2
027600     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               NC1244.2
027700        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 NC1244.2
027800          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 NC1244.2
027900     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              NC1244.2
028000     MOVE SPACE TO CORRECT-X.                                     NC1244.2
028100     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         NC1244.2
028200     MOVE     SPACE TO RE-MARK.                                   NC1244.2
028300 HEAD-ROUTINE.                                                    NC1244.2
028400     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC1244.2
028500     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC1244.2
028600     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC1244.2
028700     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC1244.2
028800 COLUMN-NAMES-ROUTINE.                                            NC1244.2
028900     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1244.2
029000     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1244.2
029100     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        NC1244.2
029200 END-ROUTINE.                                                     NC1244.2
029300     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.NC1244.2
029400 END-RTN-EXIT.                                                    NC1244.2
029500     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1244.2
029600 END-ROUTINE-1.                                                   NC1244.2
029700      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      NC1244.2
029800      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               NC1244.2
029900      ADD PASS-COUNTER TO ERROR-HOLD.                             NC1244.2
030000*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   NC1244.2
030100      MOVE PASS-COUNTER TO CCVS-E-4-1.                            NC1244.2
030200      MOVE ERROR-HOLD TO CCVS-E-4-2.                              NC1244.2
030300      MOVE CCVS-E-4 TO CCVS-E-2-2.                                NC1244.2
030400      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           NC1244.2
030500  END-ROUTINE-12.                                                 NC1244.2
030600      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        NC1244.2
030700     IF       ERROR-COUNTER IS EQUAL TO ZERO                      NC1244.2
030800         MOVE "NO " TO ERROR-TOTAL                                NC1244.2
030900         ELSE                                                     NC1244.2
031000         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       NC1244.2
031100     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           NC1244.2
031200     PERFORM WRITE-LINE.                                          NC1244.2
031300 END-ROUTINE-13.                                                  NC1244.2
031400     IF DELETE-COUNTER IS EQUAL TO ZERO                           NC1244.2
031500         MOVE "NO " TO ERROR-TOTAL  ELSE                          NC1244.2
031600         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      NC1244.2
031700     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   NC1244.2
031800     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1244.2
031900      IF   INSPECT-COUNTER EQUAL TO ZERO                          NC1244.2
032000          MOVE "NO " TO ERROR-TOTAL                               NC1244.2
032100      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   NC1244.2
032200      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            NC1244.2
032300      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          NC1244.2
032400     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1244.2
032500 WRITE-LINE.                                                      NC1244.2
032600     ADD 1 TO RECORD-COUNT.                                       NC1244.2
032700*    IF RECORD-COUNT GREATER 42                                   NC1244.2
032800*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          NC1244.2
032900*        MOVE SPACE TO DUMMY-RECORD                               NC1244.2
033000*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  NC1244.2
033100*        MOVE CCVS-H-1  TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   NC1244.2
033200*        MOVE CCVS-H-2A TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   NC1244.2
033300*        MOVE CCVS-H-2B TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   NC1244.2
033400*        MOVE CCVS-H-3  TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   NC1244.2
033500*        MOVE CCVS-C-1  TO DUMMY-RECORD  PERFORM WRT-LN           NC1244.2
033600*        MOVE CCVS-C-2  TO DUMMY-RECORD  PERFORM WRT-LN           NC1244.2
033700*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          NC1244.2
033800*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          NC1244.2
033900*        MOVE ZERO TO RECORD-COUNT.                               NC1244.2
034000     PERFORM WRT-LN.                                              NC1244.2
034100 WRT-LN.                                                          NC1244.2
034200     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               NC1244.2
034300     MOVE SPACE TO DUMMY-RECORD.                                  NC1244.2
034400 BLANK-LINE-PRINT.                                                NC1244.2
034500     PERFORM WRT-LN.                                              NC1244.2
034600 FAIL-ROUTINE.                                                    NC1244.2
034700     IF     COMPUTED-X NOT EQUAL TO SPACE                         NC1244.2
034800            GO TO FAIL-ROUTINE-WRITE.                             NC1244.2
034900     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.NC1244.2
035000     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC1244.2
035100     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   NC1244.2
035200     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1244.2
035300     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC1244.2
035400     GO TO  FAIL-ROUTINE-EX.                                      NC1244.2
035500 FAIL-ROUTINE-WRITE.                                              NC1244.2
035600     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         NC1244.2
035700     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 NC1244.2
035800     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. NC1244.2
035900     MOVE   SPACES TO COR-ANSI-REFERENCE.                         NC1244.2
036000 FAIL-ROUTINE-EX. EXIT.                                           NC1244.2
036100 BAIL-OUT.                                                        NC1244.2
036200     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   NC1244.2
036300     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           NC1244.2
036400 BAIL-OUT-WRITE.                                                  NC1244.2
036500     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  NC1244.2
036600     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC1244.2
036700     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1244.2
036800     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC1244.2
036900 BAIL-OUT-EX. EXIT.                                               NC1244.2
037000 CCVS1-EXIT.                                                      NC1244.2
037100     EXIT.                                                        NC1244.2
037200 PICTURE-INIT-1.                                                  NC1244.2
037300     MOVE "VI-31"           TO ANSI-REFERENCE.                    NC1244.2
037400     MOVE "PICTRE-TST-1"    TO PAR-NAME.                          NC1244.2
037500     MOVE "PICTURE + AND -" TO FEATURE.                           NC1244.2
037600     MOVE  0                TO WORK-AREA-1.                       NC1244.2
037700     MOVE 1 TO REC-CT.                                            NC1244.2
037800 PICTURE-TEST-1.                                                  NC1244.2
037900     MOVE WORK-AREA-1 TO EDIT-AREA-1  EDIT-AREA-2  EDIT-AREA-3    NC1244.2
038000                         EDIT-AREA-4  EDIT-AREA-5  EDIT-AREA-6    NC1244.2
038100                         EDIT-AREA-7  EDIT-AREA-8.                NC1244.2
038200     GO TO PICTURE-TEST-1-A.                                      NC1244.2
038300 PICTURE-DELETE-1.                                                NC1244.2
038400     PERFORM DE-LETE.                                             NC1244.2
038500     PERFORM PRINT-DETAIL.                                        NC1244.2
038600     GO TO PICTURE-INIT-2.                                        NC1244.2
038700 PICTURE-TEST-1-A.                                                NC1244.2
038800     IF EDIT-AREA-1 EQUAL TO "+0000"                              NC1244.2
038900        PERFORM PASS                                              NC1244.2
039000        PERFORM PRINT-DETAIL                                      NC1244.2
039100        ELSE PERFORM FAIL                                         NC1244.2
039200        MOVE EDIT-AREA-1 TO COMPUTED-A                            NC1244.2
039300        MOVE "+0000" TO CORRECT-A                                 NC1244.2
039400        PERFORM PRINT-DETAIL.                                     NC1244.2
039500     ADD 1 TO REC-CT.                                             NC1244.2
039600 PICTURE-TEST-1-B.                                                NC1244.2
039700     IF EDIT-AREA-2 EQUAL TO " 0000"                              NC1244.2
039800        PERFORM PASS                                              NC1244.2
039900        PERFORM PRINT-DETAIL                                      NC1244.2
040000        ELSE PERFORM FAIL                                         NC1244.2
040100        MOVE EDIT-AREA-2 TO COMPUTED-A                            NC1244.2
040200        MOVE " 0000" TO CORRECT-A                                 NC1244.2
040300        PERFORM PRINT-DETAIL.                                     NC1244.2
040400     ADD 1 TO REC-CT.                                             NC1244.2
040500 PICTURE-TEST-1-C.                                                NC1244.2
040600     IF EDIT-AREA-3 EQUAL TO "   +0"                              NC1244.2
040700        PERFORM PASS                                              NC1244.2
040800        PERFORM PRINT-DETAIL                                      NC1244.2
040900        ELSE PERFORM FAIL                                         NC1244.2
041000        MOVE EDIT-AREA-3 TO COMPUTED-A                            NC1244.2
041100        MOVE "   +0" TO CORRECT-A                                 NC1244.2
041200        PERFORM PRINT-DETAIL.                                     NC1244.2
041300     ADD 1 TO REC-CT.                                             NC1244.2
041400 PICTURE-TEST-1-D.                                                NC1244.2
041500     IF EDIT-AREA-4 EQUAL TO "    0"                              NC1244.2
041600        PERFORM PASS                                              NC1244.2
041700        PERFORM PRINT-DETAIL                                      NC1244.2
041800        ELSE PERFORM FAIL                                         NC1244.2
041900        MOVE EDIT-AREA-4 TO COMPUTED-A                            NC1244.2
042000        MOVE "    0" TO CORRECT-A                                 NC1244.2
042100        PERFORM PRINT-DETAIL.                                     NC1244.2
042200     ADD 1 TO REC-CT.                                             NC1244.2
042300 PICTURE-TEST-1-E.                                                NC1244.2
042400     IF EDIT-AREA-5 EQUAL TO "     "                              NC1244.2
042500        PERFORM PASS                                              NC1244.2
042600        PERFORM PRINT-DETAIL                                      NC1244.2
042700        ELSE PERFORM FAIL                                         NC1244.2
042800        MOVE EDIT-AREA-5 TO COMPUTED-A                            NC1244.2
042900        MOVE "SPACES" TO CORRECT-A                                NC1244.2
043000        PERFORM PRINT-DETAIL.                                     NC1244.2
043100     ADD 1 TO REC-CT.                                             NC1244.2
043200 PICTURE-TEST-1-F.                                                NC1244.2
043300     IF EDIT-AREA-6 EQUAL TO "      "                             NC1244.2
043400        PERFORM PASS                                              NC1244.2
043500        PERFORM PRINT-DETAIL                                      NC1244.2
043600        ELSE PERFORM FAIL                                         NC1244.2
043700        MOVE EDIT-AREA-6 TO COMPUTED-A                            NC1244.2
043800        MOVE "SPACES" TO CORRECT-A                                NC1244.2
043900        PERFORM PRINT-DETAIL.                                     NC1244.2
044000     ADD 1 TO REC-CT.                                             NC1244.2
044100 PICTURE-TEST-1-G.                                                NC1244.2
044200     IF EDIT-AREA-7 EQUAL TO "        "                           NC1244.2
044300        PERFORM PASS                                              NC1244.2
044400        PERFORM PRINT-DETAIL                                      NC1244.2
044500        ELSE PERFORM FAIL                                         NC1244.2
044600        MOVE EDIT-AREA-7 TO COMPUTED-A                            NC1244.2
044700        MOVE "SPACES" TO CORRECT-A                                NC1244.2
044800        PERFORM PRINT-DETAIL.                                     NC1244.2
044900     ADD 1 TO REC-CT.                                             NC1244.2
045000 PICTURE-TEST-1-H.                                                NC1244.2
045100     IF EDIT-AREA-8 EQUAL TO "         "                          NC1244.2
045200        PERFORM PASS                                              NC1244.2
045300        PERFORM PRINT-DETAIL                                      NC1244.2
045400        ELSE PERFORM FAIL                                         NC1244.2
045500        MOVE EDIT-AREA-8 TO COMPUTED-A                            NC1244.2
045600        MOVE "SPACES" TO CORRECT-A                                NC1244.2
045700        PERFORM PRINT-DETAIL.                                     NC1244.2
045800 PICTURE-INIT-2.                                                  NC1244.2
045900     MOVE "VI-31"           TO ANSI-REFERENCE.                    NC1244.2
046000     MOVE "PICTRE-TST-2" TO PAR-NAME.                             NC1244.2
046100     MOVE "PICTURE + AND -" TO FEATURE.                           NC1244.2
046200     MOVE  12               TO WORK-AREA-2.                       NC1244.2
046300     MOVE 1 TO REC-CT.                                            NC1244.2
046400 PICTURE-TEST-2.                                                  NC1244.2
046500     MOVE WORK-AREA-2 TO EDIT-AREA-1  EDIT-AREA-2  EDIT-AREA-3    NC1244.2
046600                         EDIT-AREA-4  EDIT-AREA-5  EDIT-AREA-6    NC1244.2
046700                         EDIT-AREA-7  EDIT-AREA-8.                NC1244.2
046800     GO TO PICTURE-TEST-2-A.                                      NC1244.2
046900 PICTURE-DELETE-2.                                                NC1244.2
047000     PERFORM DE-LETE.                                             NC1244.2
047100     PERFORM PRINT-DETAIL.                                        NC1244.2
047200     GO TO PICTURE-INIT-3.                                        NC1244.2
047300 PICTURE-TEST-2-A.                                                NC1244.2
047400     IF EDIT-AREA-1 EQUAL TO "+0012"                              NC1244.2
047500        PERFORM PASS                                              NC1244.2
047600        PERFORM PRINT-DETAIL                                      NC1244.2
047700        ELSE PERFORM FAIL                                         NC1244.2
047800        MOVE EDIT-AREA-1 TO COMPUTED-A                            NC1244.2
047900        MOVE "+0012" TO CORRECT-A                                 NC1244.2
048000        PERFORM PRINT-DETAIL.                                     NC1244.2
048100     ADD 1 TO REC-CT.                                             NC1244.2
048200 PICTURE-TEST-2-B.                                                NC1244.2
048300     IF EDIT-AREA-2 EQUAL TO " 0012"                              NC1244.2
048400        PERFORM PASS                                              NC1244.2
048500        PERFORM PRINT-DETAIL                                      NC1244.2
048600        ELSE PERFORM FAIL                                         NC1244.2
048700        MOVE EDIT-AREA-2 TO COMPUTED-A                            NC1244.2
048800        MOVE " 0012" TO CORRECT-A                                 NC1244.2
048900        PERFORM PRINT-DETAIL.                                     NC1244.2
049000     ADD 1 TO REC-CT.                                             NC1244.2
049100 PICTURE-TEST-2-C.                                                NC1244.2
049200     IF EDIT-AREA-3 EQUAL TO "  +12"                              NC1244.2
049300        PERFORM PASS                                              NC1244.2
049400        PERFORM PRINT-DETAIL                                      NC1244.2
049500        ELSE PERFORM FAIL                                         NC1244.2
049600        MOVE EDIT-AREA-3 TO COMPUTED-A                            NC1244.2
049700        MOVE "  +12" TO CORRECT-A                                 NC1244.2
049800        PERFORM PRINT-DETAIL.                                     NC1244.2
049900     ADD 1 TO REC-CT.                                             NC1244.2
050000 PICTURE-TEST-2-D.                                                NC1244.2
050100     IF EDIT-AREA-4 EQUAL TO "   12"                              NC1244.2
050200        PERFORM PASS                                              NC1244.2
050300        PERFORM PRINT-DETAIL                                      NC1244.2
050400        ELSE PERFORM FAIL                                         NC1244.2
050500        MOVE EDIT-AREA-4 TO COMPUTED-A                            NC1244.2
050600        MOVE "   12" TO CORRECT-A                                 NC1244.2
050700        PERFORM PRINT-DETAIL.                                     NC1244.2
050800     ADD 1 TO REC-CT.                                             NC1244.2
050900 PICTURE-TEST-2-E.                                                NC1244.2
051000     IF EDIT-AREA-5 EQUAL TO "  +12"                              NC1244.2
051100        PERFORM PASS                                              NC1244.2
051200        PERFORM PRINT-DETAIL                                      NC1244.2
051300        ELSE PERFORM FAIL                                         NC1244.2
051400        MOVE EDIT-AREA-5 TO COMPUTED-A                            NC1244.2
051500        MOVE "  +12" TO CORRECT-A                                 NC1244.2
051600        PERFORM PRINT-DETAIL.                                     NC1244.2
051700     ADD 1 TO REC-CT.                                             NC1244.2
051800 PICTURE-TEST-2-F.                                                NC1244.2
051900     IF EDIT-AREA-6 EQUAL TO "   12"                              NC1244.2
052000        PERFORM PASS                                              NC1244.2
052100        PERFORM PRINT-DETAIL                                      NC1244.2
052200        ELSE PERFORM FAIL                                         NC1244.2
052300        MOVE EDIT-AREA-6 TO COMPUTED-A                            NC1244.2
052400        MOVE "   12" TO CORRECT-A                                 NC1244.2
052500        PERFORM PRINT-DETAIL.                                     NC1244.2
052600     ADD 1 TO REC-CT.                                             NC1244.2
052700 PICTURE-TEST-2-G.                                                NC1244.2
052800     IF EDIT-AREA-7 EQUAL TO "  +12.00"                           NC1244.2
052900        PERFORM PASS                                              NC1244.2
053000        PERFORM PRINT-DETAIL                                      NC1244.2
053100        ELSE PERFORM FAIL                                         NC1244.2
053200        MOVE EDIT-AREA-7 TO COMPUTED-A                            NC1244.2
053300        MOVE "  +12.00" TO CORRECT-A                              NC1244.2
053400        PERFORM PRINT-DETAIL.                                     NC1244.2
053500     ADD 1 TO REC-CT.                                             NC1244.2
053600 PICTURE-TEST-2-H.                                                NC1244.2
053700     IF EDIT-AREA-8 EQUAL TO "    12.00"                          NC1244.2
053800        PERFORM PASS                                              NC1244.2
053900        PERFORM PRINT-DETAIL                                      NC1244.2
054000        ELSE PERFORM FAIL                                         NC1244.2
054100        MOVE EDIT-AREA-8 TO COMPUTED-A                            NC1244.2
054200        MOVE "    12.00" TO CORRECT-A                             NC1244.2
054300        PERFORM PRINT-DETAIL.                                     NC1244.2
054400 PICTURE-INIT-3.                                                  NC1244.2
054500     MOVE "VI-31"           TO ANSI-REFERENCE.                    NC1244.2
054600     MOVE "PICTRE-TST-3" TO PAR-NAME.                             NC1244.2
054700     MOVE "PICTURE + AND -" TO FEATURE.                           NC1244.2
054800     MOVE  -12              TO WORK-AREA-3.                       NC1244.2
054900     MOVE 1 TO REC-CT.                                            NC1244.2
055000 PICTURE-TEST-3.                                                  NC1244.2
055100     MOVE WORK-AREA-3 TO EDIT-AREA-1  EDIT-AREA-2  EDIT-AREA-3    NC1244.2
055200                         EDIT-AREA-4  EDIT-AREA-5  EDIT-AREA-6    NC1244.2
055300                         EDIT-AREA-7  EDIT-AREA-8.                NC1244.2
055400     GO TO PICTURE-TEST-3-A.                                      NC1244.2
055500 PICTURE-DELETE-3.                                                NC1244.2
055600     PERFORM DE-LETE.                                             NC1244.2
055700     PERFORM PRINT-DETAIL.                                        NC1244.2
055800     GO TO PICTURE-INIT-4.                                        NC1244.2
055900 PICTURE-TEST-3-A.                                                NC1244.2
056000     IF EDIT-AREA-1 EQUAL TO "-0012"                              NC1244.2
056100        PERFORM PASS                                              NC1244.2
056200        PERFORM PRINT-DETAIL                                      NC1244.2
056300        ELSE PERFORM FAIL                                         NC1244.2
056400        MOVE EDIT-AREA-1 TO COMPUTED-A                            NC1244.2
056500        MOVE "-0012" TO CORRECT-A                                 NC1244.2
056600        PERFORM PRINT-DETAIL.                                     NC1244.2
056700     ADD 1 TO REC-CT.                                             NC1244.2
056800 PICTURE-TEST-3-B.                                                NC1244.2
056900     IF EDIT-AREA-2 EQUAL TO "-0012"                              NC1244.2
057000        PERFORM PASS                                              NC1244.2
057100        PERFORM PRINT-DETAIL                                      NC1244.2
057200        ELSE PERFORM FAIL                                         NC1244.2
057300        MOVE EDIT-AREA-2 TO COMPUTED-A                            NC1244.2
057400        MOVE "-0012" TO CORRECT-A                                 NC1244.2
057500        PERFORM PRINT-DETAIL.                                     NC1244.2
057600     ADD 1 TO REC-CT.                                             NC1244.2
057700 PICTURE-TEST-3-C.                                                NC1244.2
057800     IF EDIT-AREA-3 EQUAL TO "  -12"                              NC1244.2
057900        PERFORM PASS                                              NC1244.2
058000        PERFORM PRINT-DETAIL                                      NC1244.2
058100        ELSE PERFORM FAIL                                         NC1244.2
058200        MOVE EDIT-AREA-3 TO COMPUTED-A                            NC1244.2
058300        MOVE "  -12" TO CORRECT-A                                 NC1244.2
058400        PERFORM PRINT-DETAIL.                                     NC1244.2
058500     ADD 1 TO REC-CT.                                             NC1244.2
058600 PICTURE-TEST-3-D.                                                NC1244.2
058700     IF EDIT-AREA-4 EQUAL TO "  -12"                              NC1244.2
058800        PERFORM PASS                                              NC1244.2
058900        PERFORM PRINT-DETAIL                                      NC1244.2
059000        ELSE PERFORM FAIL                                         NC1244.2
059100        MOVE EDIT-AREA-4 TO COMPUTED-A                            NC1244.2
059200        MOVE "  -12" TO CORRECT-A                                 NC1244.2
059300        PERFORM PRINT-DETAIL.                                     NC1244.2
059400     ADD 1 TO REC-CT.                                             NC1244.2
059500 PICTURE-TEST-3-E.                                                NC1244.2
059600     IF EDIT-AREA-5 EQUAL TO "  -12"                              NC1244.2
059700        PERFORM PASS                                              NC1244.2
059800        PERFORM PRINT-DETAIL                                      NC1244.2
059900        ELSE PERFORM FAIL                                         NC1244.2
060000        MOVE EDIT-AREA-5 TO COMPUTED-A                            NC1244.2
060100        MOVE "  -12" TO CORRECT-A                                 NC1244.2
060200        PERFORM PRINT-DETAIL.                                     NC1244.2
060300     ADD 1 TO REC-CT.                                             NC1244.2
060400 PICTURE-TEST-3-F.                                                NC1244.2
060500     IF EDIT-AREA-6 EQUAL TO "  -12"                              NC1244.2
060600        PERFORM PASS                                              NC1244.2
060700        PERFORM PRINT-DETAIL                                      NC1244.2
060800        ELSE PERFORM FAIL                                         NC1244.2
060900        MOVE EDIT-AREA-6 TO COMPUTED-A                            NC1244.2
061000        MOVE "  -12" TO CORRECT-A                                 NC1244.2
061100        PERFORM PRINT-DETAIL.                                     NC1244.2
061200     ADD 1 TO REC-CT.                                             NC1244.2
061300 PICTURE-TEST-3-G.                                                NC1244.2
061400     IF EDIT-AREA-7 EQUAL TO "  -12.00"                           NC1244.2
061500        PERFORM PASS                                              NC1244.2
061600        PERFORM PRINT-DETAIL                                      NC1244.2
061700        ELSE PERFORM FAIL                                         NC1244.2
061800        MOVE EDIT-AREA-7 TO COMPUTED-A                            NC1244.2
061900        MOVE "  -12.00" TO CORRECT-A                              NC1244.2
062000        PERFORM PRINT-DETAIL.                                     NC1244.2
062100     ADD 1 TO REC-CT.                                             NC1244.2
062200 PICTURE-TEST-3-H.                                                NC1244.2
062300     IF EDIT-AREA-8 EQUAL TO "   -12.00"                          NC1244.2
062400        PERFORM PASS                                              NC1244.2
062500        PERFORM PRINT-DETAIL                                      NC1244.2
062600        ELSE PERFORM FAIL                                         NC1244.2
062700        MOVE EDIT-AREA-8 TO COMPUTED-A                            NC1244.2
062800        MOVE "   -12.00" TO CORRECT-A                             NC1244.2
062900        PERFORM PRINT-DETAIL.                                     NC1244.2
063000 PICTURE-INIT-4.                                                  NC1244.2
063100     MOVE "VI-31"           TO ANSI-REFERENCE.                    NC1244.2
063200     MOVE "PICTRE-TST-4" TO PAR-NAME.                             NC1244.2
063300     MOVE "PICTURE + AND -" TO FEATURE.                           NC1244.2
063400     MOVE  123              TO WORK-AREA-4.                       NC1244.2
063500     MOVE 1 TO REC-CT.                                            NC1244.2
063600 PICTURE-TEST-4.                                                  NC1244.2
063700     MOVE WORK-AREA-4 TO EDIT-AREA-1  EDIT-AREA-2  EDIT-AREA-3    NC1244.2
063800                         EDIT-AREA-4  EDIT-AREA-5  EDIT-AREA-6    NC1244.2
063900                         EDIT-AREA-7  EDIT-AREA-8.                NC1244.2
064000     GO TO PICTURE-TEST-4-A.                                      NC1244.2
064100 PICTURE-DELETE-4.                                                NC1244.2
064200     PERFORM DE-LETE.                                             NC1244.2
064300     PERFORM PRINT-DETAIL.                                        NC1244.2
064400     GO TO PICTURE-INIT-5.                                        NC1244.2
064500 PICTURE-TEST-4-A.                                                NC1244.2
064600     IF EDIT-AREA-1 EQUAL TO "+0123"                              NC1244.2
064700        PERFORM PASS                                              NC1244.2
064800        PERFORM PRINT-DETAIL                                      NC1244.2
064900        ELSE PERFORM FAIL                                         NC1244.2
065000        MOVE EDIT-AREA-1 TO COMPUTED-A                            NC1244.2
065100        MOVE "+0123" TO CORRECT-A                                 NC1244.2
065200        PERFORM PRINT-DETAIL.                                     NC1244.2
065300     ADD 1 TO REC-CT.                                             NC1244.2
065400 PICTURE-TEST-4-B.                                                NC1244.2
065500     IF EDIT-AREA-2 EQUAL TO " 0123"                              NC1244.2
065600        PERFORM PASS                                              NC1244.2
065700        PERFORM PRINT-DETAIL                                      NC1244.2
065800        ELSE PERFORM FAIL                                         NC1244.2
065900        MOVE EDIT-AREA-2 TO COMPUTED-A                            NC1244.2
066000        MOVE " 0123" TO CORRECT-A                                 NC1244.2
066100        PERFORM PRINT-DETAIL.                                     NC1244.2
066200     ADD 1 TO REC-CT.                                             NC1244.2
066300 PICTURE-TEST-4-C.                                                NC1244.2
066400     IF EDIT-AREA-3 EQUAL TO " +123"                              NC1244.2
066500        PERFORM PASS                                              NC1244.2
066600        PERFORM PRINT-DETAIL                                      NC1244.2
066700        ELSE PERFORM FAIL                                         NC1244.2
066800        MOVE EDIT-AREA-3 TO COMPUTED-A                            NC1244.2
066900        MOVE " +123" TO CORRECT-A                                 NC1244.2
067000        PERFORM PRINT-DETAIL.                                     NC1244.2
067100     ADD 1 TO REC-CT.                                             NC1244.2
067200 PICTURE-TEST-4-D.                                                NC1244.2
067300     IF EDIT-AREA-4 EQUAL TO "  123"                              NC1244.2
067400        PERFORM PASS                                              NC1244.2
067500        PERFORM PRINT-DETAIL                                      NC1244.2
067600        ELSE PERFORM FAIL                                         NC1244.2
067700        MOVE EDIT-AREA-4 TO COMPUTED-A                            NC1244.2
067800        MOVE "  123" TO CORRECT-A                                 NC1244.2
067900        PERFORM PRINT-DETAIL.                                     NC1244.2
068000     ADD 1 TO REC-CT.                                             NC1244.2
068100 PICTURE-TEST-4-E.                                                NC1244.2
068200     IF EDIT-AREA-5 EQUAL TO " +123"                              NC1244.2
068300        PERFORM PASS                                              NC1244.2
068400        PERFORM PRINT-DETAIL                                      NC1244.2
068500        ELSE PERFORM FAIL                                         NC1244.2
068600        MOVE EDIT-AREA-5 TO COMPUTED-A                            NC1244.2
068700        MOVE " +123" TO CORRECT-A                                 NC1244.2
068800        PERFORM PRINT-DETAIL.                                     NC1244.2
068900     ADD 1 TO REC-CT.                                             NC1244.2
069000 PICTURE-TEST-4-F.                                                NC1244.2
069100     IF EDIT-AREA-6 EQUAL TO "  123"                              NC1244.2
069200        PERFORM PASS                                              NC1244.2
069300        PERFORM PRINT-DETAIL                                      NC1244.2
069400        ELSE PERFORM FAIL                                         NC1244.2
069500        MOVE EDIT-AREA-6 TO COMPUTED-A                            NC1244.2
069600        MOVE "  123" TO CORRECT-A                                 NC1244.2
069700        PERFORM PRINT-DETAIL.                                     NC1244.2
069800     ADD 1 TO REC-CT.                                             NC1244.2
069900 PICTURE-TEST-4-G.                                                NC1244.2
070000     IF EDIT-AREA-7 EQUAL TO " +123.00"                           NC1244.2
070100        PERFORM PASS                                              NC1244.2
070200        PERFORM PRINT-DETAIL                                      NC1244.2
070300        ELSE PERFORM FAIL                                         NC1244.2
070400        MOVE EDIT-AREA-7 TO COMPUTED-A                            NC1244.2
070500        MOVE " +123.00" TO CORRECT-A                              NC1244.2
070600        PERFORM PRINT-DETAIL.                                     NC1244.2
070700     ADD 1 TO REC-CT.                                             NC1244.2
070800 PICTURE-TEST-4-H.                                                NC1244.2
070900     IF EDIT-AREA-8 EQUAL TO "   123.00"                          NC1244.2
071000        PERFORM PASS                                              NC1244.2
071100        PERFORM PRINT-DETAIL                                      NC1244.2
071200        ELSE PERFORM FAIL                                         NC1244.2
071300        MOVE EDIT-AREA-8 TO COMPUTED-A                            NC1244.2
071400        MOVE "   123.00" TO CORRECT-A                             NC1244.2
071500        PERFORM PRINT-DETAIL.                                     NC1244.2
071600 PICTURE-INIT-5.                                                  NC1244.2
071700     MOVE "VI-31"           TO ANSI-REFERENCE.                    NC1244.2
071800     MOVE "PICTRE-TST-5" TO PAR-NAME.                             NC1244.2
071900     MOVE "PICTURE + AND -" TO FEATURE.                           NC1244.2
072000     MOVE  -123             TO WORK-AREA-5.                       NC1244.2
072100     MOVE 1 TO REC-CT.                                            NC1244.2
072200 PICTURE-TEST-5.                                                  NC1244.2
072300     MOVE WORK-AREA-5 TO EDIT-AREA-1  EDIT-AREA-2  EDIT-AREA-3    NC1244.2
072400                         EDIT-AREA-4  EDIT-AREA-5  EDIT-AREA-6    NC1244.2
072500                         EDIT-AREA-7  EDIT-AREA-8.                NC1244.2
072600     GO TO PICTURE-TEST-5-A.                                      NC1244.2
072700 PICTURE-DELETE-5.                                                NC1244.2
072800     PERFORM DE-LETE.                                             NC1244.2
072900     PERFORM PRINT-DETAIL.                                        NC1244.2
073000     GO TO PICTURE-INIT-6.                                        NC1244.2
073100 PICTURE-TEST-5-A.                                                NC1244.2
073200     IF EDIT-AREA-1 EQUAL TO "-0123"                              NC1244.2
073300        PERFORM PASS                                              NC1244.2
073400        PERFORM PRINT-DETAIL                                      NC1244.2
073500        ELSE PERFORM FAIL                                         NC1244.2
073600        MOVE EDIT-AREA-1 TO COMPUTED-A                            NC1244.2
073700        MOVE "-0123" TO CORRECT-A                                 NC1244.2
073800        PERFORM PRINT-DETAIL.                                     NC1244.2
073900     ADD 1 TO REC-CT.                                             NC1244.2
074000 PICTURE-TEST-5-B.                                                NC1244.2
074100     IF EDIT-AREA-2 EQUAL TO "-0123"                              NC1244.2
074200        PERFORM PASS                                              NC1244.2
074300        PERFORM PRINT-DETAIL                                      NC1244.2
074400        ELSE PERFORM FAIL                                         NC1244.2
074500        MOVE EDIT-AREA-2 TO COMPUTED-A                            NC1244.2
074600        MOVE "-0123" TO CORRECT-A                                 NC1244.2
074700        PERFORM PRINT-DETAIL.                                     NC1244.2
074800     ADD 1 TO REC-CT.                                             NC1244.2
074900 PICTURE-TEST-5-C.                                                NC1244.2
075000     IF EDIT-AREA-3 EQUAL TO " -123"                              NC1244.2
075100        PERFORM PASS                                              NC1244.2
075200        PERFORM PRINT-DETAIL                                      NC1244.2
075300        ELSE PERFORM FAIL                                         NC1244.2
075400        MOVE EDIT-AREA-3 TO COMPUTED-A                            NC1244.2
075500        MOVE " -123" TO CORRECT-A                                 NC1244.2
075600        PERFORM PRINT-DETAIL.                                     NC1244.2
075700     ADD 1 TO REC-CT.                                             NC1244.2
075800 PICTURE-TEST-5-D.                                                NC1244.2
075900     IF EDIT-AREA-4 EQUAL TO " -123"                              NC1244.2
076000        PERFORM PASS                                              NC1244.2
076100        PERFORM PRINT-DETAIL                                      NC1244.2
076200        ELSE PERFORM FAIL                                         NC1244.2
076300        MOVE EDIT-AREA-4 TO COMPUTED-A                            NC1244.2
076400        MOVE " -123" TO CORRECT-A                                 NC1244.2
076500        PERFORM PRINT-DETAIL.                                     NC1244.2
076600     ADD 1 TO REC-CT.                                             NC1244.2
076700 PICTURE-TEST-5-E.                                                NC1244.2
076800     IF EDIT-AREA-5 EQUAL TO " -123"                              NC1244.2
076900        PERFORM PASS                                              NC1244.2
077000        PERFORM PRINT-DETAIL                                      NC1244.2
077100        ELSE PERFORM FAIL                                         NC1244.2
077200        MOVE EDIT-AREA-5 TO COMPUTED-A                            NC1244.2
077300        MOVE " -123" TO CORRECT-A                                 NC1244.2
077400        PERFORM PRINT-DETAIL.                                     NC1244.2
077500     ADD 1 TO REC-CT.                                             NC1244.2
077600 PICTURE-TEST-5-F.                                                NC1244.2
077700     IF EDIT-AREA-6 EQUAL TO " -123"                              NC1244.2
077800        PERFORM PASS                                              NC1244.2
077900        PERFORM PRINT-DETAIL                                      NC1244.2
078000        ELSE PERFORM FAIL                                         NC1244.2
078100        MOVE EDIT-AREA-6 TO COMPUTED-A                            NC1244.2
078200        MOVE " -123" TO CORRECT-A                                 NC1244.2
078300        PERFORM PRINT-DETAIL.                                     NC1244.2
078400     ADD 1 TO REC-CT.                                             NC1244.2
078500 PICTURE-TEST-5-G.                                                NC1244.2
078600     IF EDIT-AREA-7 EQUAL TO " -123.00"                           NC1244.2
078700        PERFORM PASS                                              NC1244.2
078800        PERFORM PRINT-DETAIL                                      NC1244.2
078900        ELSE PERFORM FAIL                                         NC1244.2
079000        MOVE EDIT-AREA-7 TO COMPUTED-A                            NC1244.2
079100        MOVE " -123.00" TO CORRECT-A                              NC1244.2
079200        PERFORM PRINT-DETAIL.                                     NC1244.2
079300     ADD 1 TO REC-CT.                                             NC1244.2
079400 PICTURE-TEST-5-H.                                                NC1244.2
079500     IF EDIT-AREA-8 EQUAL TO "  -123.00"                          NC1244.2
079600        PERFORM PASS                                              NC1244.2
079700        PERFORM PRINT-DETAIL                                      NC1244.2
079800        ELSE PERFORM FAIL                                         NC1244.2
079900        MOVE EDIT-AREA-8 TO COMPUTED-A                            NC1244.2
080000        MOVE "  -123.00" TO CORRECT-A                             NC1244.2
080100        PERFORM PRINT-DETAIL.                                     NC1244.2
080200 PICTURE-INIT-6.                                                  NC1244.2
080300     MOVE "VI-31"           TO ANSI-REFERENCE.                    NC1244.2
080400     MOVE "PICTRE-TST-6" TO PAR-NAME.                             NC1244.2
080500     MOVE "PICTURE + AND -" TO FEATURE.                           NC1244.2
080600     MOVE  1234             TO WORK-AREA-6.                       NC1244.2
080700     MOVE 1 TO REC-CT.                                            NC1244.2
080800 PICTURE-TEST-6.                                                  NC1244.2
080900     MOVE WORK-AREA-6 TO EDIT-AREA-1  EDIT-AREA-2  EDIT-AREA-3    NC1244.2
081000                         EDIT-AREA-4  EDIT-AREA-5  EDIT-AREA-6    NC1244.2
081100                         EDIT-AREA-7  EDIT-AREA-8.                NC1244.2
081200     GO TO PICTURE-TEST-6-A.                                      NC1244.2
081300 PICTURE-DELETE-6.                                                NC1244.2
081400     PERFORM DE-LETE.                                             NC1244.2
081500     PERFORM PRINT-DETAIL.                                        NC1244.2
081600     GO TO PICTURE-INIT-7.                                        NC1244.2
081700 PICTURE-TEST-6-A.                                                NC1244.2
081800     IF EDIT-AREA-1 EQUAL TO "+1234"                              NC1244.2
081900        PERFORM PASS                                              NC1244.2
082000        PERFORM PRINT-DETAIL                                      NC1244.2
082100        ELSE PERFORM FAIL                                         NC1244.2
082200        MOVE EDIT-AREA-1 TO COMPUTED-A                            NC1244.2
082300        MOVE "+1234" TO CORRECT-A                                 NC1244.2
082400        PERFORM PRINT-DETAIL.                                     NC1244.2
082500     ADD 1 TO REC-CT.                                             NC1244.2
082600 PICTURE-TEST-6-B.                                                NC1244.2
082700     IF EDIT-AREA-2 EQUAL TO " 1234"                              NC1244.2
082800        PERFORM PASS                                              NC1244.2
082900        PERFORM PRINT-DETAIL                                      NC1244.2
083000        ELSE PERFORM FAIL                                         NC1244.2
083100        MOVE EDIT-AREA-2 TO COMPUTED-A                            NC1244.2
083200        MOVE " 1234" TO CORRECT-A                                 NC1244.2
083300        PERFORM PRINT-DETAIL.                                     NC1244.2
083400     ADD 1 TO REC-CT.                                             NC1244.2
083500 PICTURE-TEST-6-C.                                                NC1244.2
083600     IF EDIT-AREA-3 EQUAL TO "+1234"                              NC1244.2
083700        PERFORM PASS                                              NC1244.2
083800        PERFORM PRINT-DETAIL                                      NC1244.2
083900        ELSE PERFORM FAIL                                         NC1244.2
084000        MOVE EDIT-AREA-3 TO COMPUTED-A                            NC1244.2
084100        MOVE "+1234" TO CORRECT-A                                 NC1244.2
084200        PERFORM PRINT-DETAIL.                                     NC1244.2
084300     ADD 1 TO REC-CT.                                             NC1244.2
084400 PICTURE-TEST-6-D.                                                NC1244.2
084500     IF EDIT-AREA-4 EQUAL TO " 1234"                              NC1244.2
084600        PERFORM PASS                                              NC1244.2
084700        PERFORM PRINT-DETAIL                                      NC1244.2
084800        ELSE PERFORM FAIL                                         NC1244.2
084900        MOVE EDIT-AREA-4 TO COMPUTED-A                            NC1244.2
085000        MOVE " 1234" TO CORRECT-A                                 NC1244.2
085100        PERFORM PRINT-DETAIL.                                     NC1244.2
085200     ADD 1 TO REC-CT.                                             NC1244.2
085300 PICTURE-TEST-6-E.                                                NC1244.2
085400     IF EDIT-AREA-5 EQUAL TO "+1234"                              NC1244.2
085500        PERFORM PASS                                              NC1244.2
085600        PERFORM PRINT-DETAIL                                      NC1244.2
085700        ELSE PERFORM FAIL                                         NC1244.2
085800        MOVE EDIT-AREA-5 TO COMPUTED-A                            NC1244.2
085900        MOVE "+1234" TO CORRECT-A                                 NC1244.2
086000        PERFORM PRINT-DETAIL.                                     NC1244.2
086100     ADD 1 TO REC-CT.                                             NC1244.2
086200 PICTURE-TEST-6-F.                                                NC1244.2
086300     IF EDIT-AREA-6 EQUAL TO " 1234"                              NC1244.2
086400        PERFORM PASS                                              NC1244.2
086500        PERFORM PRINT-DETAIL                                      NC1244.2
086600        ELSE PERFORM FAIL                                         NC1244.2
086700        MOVE EDIT-AREA-6 TO COMPUTED-A                            NC1244.2
086800        MOVE " 1234" TO CORRECT-A                                 NC1244.2
086900        PERFORM PRINT-DETAIL.                                     NC1244.2
087000     ADD 1 TO REC-CT.                                             NC1244.2
087100 PICTURE-TEST-6-G.                                                NC1244.2
087200     IF EDIT-AREA-7 EQUAL TO "+1234.00"                           NC1244.2
087300        PERFORM PASS                                              NC1244.2
087400        PERFORM PRINT-DETAIL                                      NC1244.2
087500        ELSE PERFORM FAIL                                         NC1244.2
087600        MOVE EDIT-AREA-7 TO COMPUTED-A                            NC1244.2
087700        MOVE "+1234.00" TO CORRECT-A                              NC1244.2
087800        PERFORM PRINT-DETAIL.                                     NC1244.2
087900     ADD 1 TO REC-CT.                                             NC1244.2
088000 PICTURE-TEST-6-H.                                                NC1244.2
088100     IF EDIT-AREA-8 EQUAL TO " 1,234.00"                          NC1244.2
088200        PERFORM PASS                                              NC1244.2
088300        PERFORM PRINT-DETAIL                                      NC1244.2
088400        ELSE PERFORM FAIL                                         NC1244.2
088500        MOVE EDIT-AREA-8 TO COMPUTED-A                            NC1244.2
088600        MOVE " 1,234.00" TO CORRECT-A                             NC1244.2
088700        PERFORM PRINT-DETAIL.                                     NC1244.2
088800 PICTURE-INIT-7.                                                  NC1244.2
088900     MOVE "VI-31"           TO ANSI-REFERENCE.                    NC1244.2
089000     MOVE "PICTRE-TST-7" TO PAR-NAME.                             NC1244.2
089100     MOVE "PICTURE + AND -" TO FEATURE.                           NC1244.2
089200     MOVE  -1234           TO WORK-AREA-7.                        NC1244.2
089300     MOVE 1 TO REC-CT.                                            NC1244.2
089400 PICTURE-TEST-7.                                                  NC1244.2
089500     MOVE WORK-AREA-7 TO EDIT-AREA-1  EDIT-AREA-2  EDIT-AREA-3    NC1244.2
089600                         EDIT-AREA-4  EDIT-AREA-5  EDIT-AREA-6    NC1244.2
089700                         EDIT-AREA-7  EDIT-AREA-8.                NC1244.2
089800     GO TO PICTURE-TEST-7-A.                                      NC1244.2
089900 PICTURE-DELETE-7.                                                NC1244.2
090000     PERFORM DE-LETE.                                             NC1244.2
090100     PERFORM PRINT-DETAIL.                                        NC1244.2
090200     GO TO PICTURE-INIT-8.                                        NC1244.2
090300 PICTURE-TEST-7-A.                                                NC1244.2
090400     IF EDIT-AREA-1 EQUAL TO "-1234"                              NC1244.2
090500        PERFORM PASS                                              NC1244.2
090600        PERFORM PRINT-DETAIL                                      NC1244.2
090700        ELSE PERFORM FAIL                                         NC1244.2
090800        MOVE EDIT-AREA-1 TO COMPUTED-A                            NC1244.2
090900        MOVE "-1234" TO CORRECT-A                                 NC1244.2
091000        PERFORM PRINT-DETAIL.                                     NC1244.2
091100     ADD 1 TO REC-CT.                                             NC1244.2
091200 PICTURE-TEST-7-B.                                                NC1244.2
091300     IF EDIT-AREA-2 EQUAL TO "-1234"                              NC1244.2
091400        PERFORM PASS                                              NC1244.2
091500        PERFORM PRINT-DETAIL                                      NC1244.2
091600        ELSE PERFORM FAIL                                         NC1244.2
091700        MOVE EDIT-AREA-2 TO COMPUTED-A                            NC1244.2
091800        MOVE "-1234" TO CORRECT-A                                 NC1244.2
091900        PERFORM PRINT-DETAIL.                                     NC1244.2
092000     ADD 1 TO REC-CT.                                             NC1244.2
092100 PICTURE-TEST-7-C.                                                NC1244.2
092200     IF EDIT-AREA-3 EQUAL TO "-1234"                              NC1244.2
092300        PERFORM PASS                                              NC1244.2
092400        PERFORM PRINT-DETAIL                                      NC1244.2
092500        ELSE PERFORM FAIL                                         NC1244.2
092600        MOVE EDIT-AREA-3 TO COMPUTED-A                            NC1244.2
092700        MOVE "-1234" TO CORRECT-A                                 NC1244.2
092800        PERFORM PRINT-DETAIL.                                     NC1244.2
092900     ADD 1 TO REC-CT.                                             NC1244.2
093000 PICTURE-TEST-7-D.                                                NC1244.2
093100     IF EDIT-AREA-4 EQUAL TO "-1234"                              NC1244.2
093200        PERFORM PASS                                              NC1244.2
093300        PERFORM PRINT-DETAIL                                      NC1244.2
093400        ELSE PERFORM FAIL                                         NC1244.2
093500        MOVE EDIT-AREA-4 TO COMPUTED-A                            NC1244.2
093600        MOVE "-1234" TO CORRECT-A                                 NC1244.2
093700        PERFORM PRINT-DETAIL.                                     NC1244.2
093800     ADD 1 TO REC-CT.                                             NC1244.2
093900 PICTURE-TEST-7-E.                                                NC1244.2
094000     IF EDIT-AREA-5 EQUAL TO "-1234"                              NC1244.2
094100        PERFORM PASS                                              NC1244.2
094200        PERFORM PRINT-DETAIL                                      NC1244.2
094300        ELSE PERFORM FAIL                                         NC1244.2
094400        MOVE EDIT-AREA-5 TO COMPUTED-A                            NC1244.2
094500        MOVE "-1234" TO CORRECT-A                                 NC1244.2
094600        PERFORM PRINT-DETAIL.                                     NC1244.2
094700     ADD 1 TO REC-CT.                                             NC1244.2
094800 PICTURE-TEST-7-F.                                                NC1244.2
094900     IF EDIT-AREA-6 EQUAL TO "-1234"                              NC1244.2
095000        PERFORM PASS                                              NC1244.2
095100        PERFORM PRINT-DETAIL                                      NC1244.2
095200        ELSE PERFORM FAIL                                         NC1244.2
095300        MOVE EDIT-AREA-6 TO COMPUTED-A                            NC1244.2
095400        MOVE "-1234" TO CORRECT-A                                 NC1244.2
095500        PERFORM PRINT-DETAIL.                                     NC1244.2
095600     ADD 1 TO REC-CT.                                             NC1244.2
095700 PICTURE-TEST-7-G.                                                NC1244.2
095800     IF EDIT-AREA-7 EQUAL TO "-1234.00"                           NC1244.2
095900        PERFORM PASS                                              NC1244.2
096000        PERFORM PRINT-DETAIL                                      NC1244.2
096100        ELSE PERFORM FAIL                                         NC1244.2
096200        MOVE EDIT-AREA-7 TO COMPUTED-A                            NC1244.2
096300        MOVE "-1234.00" TO CORRECT-A                              NC1244.2
096400        PERFORM PRINT-DETAIL.                                     NC1244.2
096500     ADD 1 TO REC-CT.                                             NC1244.2
096600 PICTURE-TEST-7-H.                                                NC1244.2
096700     IF EDIT-AREA-8 EQUAL TO "-1,234.00"                          NC1244.2
096800        PERFORM PASS                                              NC1244.2
096900        PERFORM PRINT-DETAIL                                      NC1244.2
097000        ELSE PERFORM FAIL                                         NC1244.2
097100        MOVE EDIT-AREA-8 TO COMPUTED-A                            NC1244.2
097200        MOVE "-1,234.00" TO CORRECT-A                             NC1244.2
097300        PERFORM PRINT-DETAIL.                                     NC1244.2
097400 PICTURE-INIT-8.                                                  NC1244.2
097500     MOVE "VI-31"           TO ANSI-REFERENCE.                    NC1244.2
097600     MOVE "PICTRE-TST-8" TO PAR-NAME.                             NC1244.2
097700     MOVE "PICTURE + AND -" TO FEATURE.                           NC1244.2
097800     MOVE  12.34           TO WORK-AREA-8.                        NC1244.2
097900     MOVE 1 TO REC-CT.                                            NC1244.2
098000 PICTURE-TEST-8.                                                  NC1244.2
098100     MOVE WORK-AREA-8 TO EDIT-AREA-1  EDIT-AREA-2  EDIT-AREA-3    NC1244.2
098200                         EDIT-AREA-4  EDIT-AREA-5  EDIT-AREA-6    NC1244.2
098300                         EDIT-AREA-7  EDIT-AREA-8.                NC1244.2
098400     GO TO PICTURE-TEST-8-A.                                      NC1244.2
098500 PICTURE-DELETE-8.                                                NC1244.2
098600     PERFORM DE-LETE.                                             NC1244.2
098700     PERFORM PRINT-DETAIL.                                        NC1244.2
098800     GO TO PICTURE-INIT-9.                                        NC1244.2
098900 PICTURE-TEST-8-A.                                                NC1244.2
099000     IF EDIT-AREA-1 EQUAL TO "+0012"                              NC1244.2
099100        PERFORM PASS                                              NC1244.2
099200        PERFORM PRINT-DETAIL                                      NC1244.2
099300        ELSE PERFORM FAIL                                         NC1244.2
099400        MOVE EDIT-AREA-1 TO COMPUTED-A                            NC1244.2
099500        MOVE "+0012" TO CORRECT-A                                 NC1244.2
099600        PERFORM PRINT-DETAIL.                                     NC1244.2
099700     ADD 1 TO REC-CT.                                             NC1244.2
099800 PICTURE-TEST-8-B.                                                NC1244.2
099900     IF EDIT-AREA-2 EQUAL TO " 0012"                              NC1244.2
100000        PERFORM PASS                                              NC1244.2
100100        PERFORM PRINT-DETAIL                                      NC1244.2
100200        ELSE PERFORM FAIL                                         NC1244.2
100300        MOVE EDIT-AREA-2 TO COMPUTED-A                            NC1244.2
100400        MOVE " 0012" TO CORRECT-A                                 NC1244.2
100500        PERFORM PRINT-DETAIL.                                     NC1244.2
100600     ADD 1 TO REC-CT.                                             NC1244.2
100700 PICTURE-TEST-8-C.                                                NC1244.2
100800     IF EDIT-AREA-3 EQUAL TO "  +12"                              NC1244.2
100900        PERFORM PASS                                              NC1244.2
101000        PERFORM PRINT-DETAIL                                      NC1244.2
101100        ELSE PERFORM FAIL                                         NC1244.2
101200        MOVE EDIT-AREA-3 TO COMPUTED-A                            NC1244.2
101300        MOVE "  +12" TO CORRECT-A                                 NC1244.2
101400        PERFORM PRINT-DETAIL.                                     NC1244.2
101500     ADD 1 TO REC-CT.                                             NC1244.2
101600 PICTURE-TEST-8-D.                                                NC1244.2
101700     IF EDIT-AREA-4 EQUAL TO "   12"                              NC1244.2
101800        PERFORM PASS                                              NC1244.2
101900        PERFORM PRINT-DETAIL                                      NC1244.2
102000        ELSE PERFORM FAIL                                         NC1244.2
102100        MOVE EDIT-AREA-4 TO COMPUTED-A                            NC1244.2
102200        MOVE "   12" TO CORRECT-A                                 NC1244.2
102300        PERFORM PRINT-DETAIL.                                     NC1244.2
102400     ADD 1 TO REC-CT.                                             NC1244.2
102500 PICTURE-TEST-8-E.                                                NC1244.2
102600     IF EDIT-AREA-5 EQUAL TO "  +12"                              NC1244.2
102700        PERFORM PASS                                              NC1244.2
102800        PERFORM PRINT-DETAIL                                      NC1244.2
102900        ELSE PERFORM FAIL                                         NC1244.2
103000        MOVE EDIT-AREA-5 TO COMPUTED-A                            NC1244.2
103100        MOVE "  +12" TO CORRECT-A                                 NC1244.2
103200        PERFORM PRINT-DETAIL.                                     NC1244.2
103300     ADD 1 TO REC-CT.                                             NC1244.2
103400 PICTURE-TEST-8-F.                                                NC1244.2
103500     IF EDIT-AREA-6 EQUAL TO "   12"                              NC1244.2
103600        PERFORM PASS                                              NC1244.2
103700        PERFORM PRINT-DETAIL                                      NC1244.2
103800        ELSE PERFORM FAIL                                         NC1244.2
103900        MOVE EDIT-AREA-6 TO COMPUTED-A                            NC1244.2
104000        MOVE "   12" TO CORRECT-A                                 NC1244.2
104100        PERFORM PRINT-DETAIL.                                     NC1244.2
104200     ADD 1 TO REC-CT.                                             NC1244.2
104300 PICTURE-TEST-8-G.                                                NC1244.2
104400     IF EDIT-AREA-7 EQUAL TO "  +12.34"                           NC1244.2
104500        PERFORM PASS                                              NC1244.2
104600        PERFORM PRINT-DETAIL                                      NC1244.2
104700        ELSE PERFORM FAIL                                         NC1244.2
104800        MOVE EDIT-AREA-7 TO COMPUTED-A                            NC1244.2
104900        MOVE "  +12.34" TO CORRECT-A                              NC1244.2
105000        PERFORM PRINT-DETAIL.                                     NC1244.2
105100     ADD 1 TO REC-CT.                                             NC1244.2
105200 PICTURE-TEST-8-H.                                                NC1244.2
105300     IF EDIT-AREA-8 EQUAL TO "    12.34"                          NC1244.2
105400        PERFORM PASS                                              NC1244.2
105500        PERFORM PRINT-DETAIL                                      NC1244.2
105600        ELSE PERFORM FAIL                                         NC1244.2
105700        MOVE EDIT-AREA-8 TO COMPUTED-A                            NC1244.2
105800        MOVE "    12.34" TO CORRECT-A                             NC1244.2
105900        PERFORM PRINT-DETAIL.                                     NC1244.2
106000 PICTURE-INIT-9.                                                  NC1244.2
106100     MOVE "VI-31"           TO ANSI-REFERENCE.                    NC1244.2
106200     MOVE "PICTRE-TST-9" TO PAR-NAME.                             NC1244.2
106300     MOVE "PICTURE + AND -" TO FEATURE.                           NC1244.2
106400     MOVE -12.34           TO WORK-AREA-9.                        NC1244.2
106500     MOVE 1 TO REC-CT.                                            NC1244.2
106600 PICTURE-TEST-9.                                                  NC1244.2
106700     MOVE WORK-AREA-9 TO EDIT-AREA-1  EDIT-AREA-2  EDIT-AREA-3    NC1244.2
106800                         EDIT-AREA-4  EDIT-AREA-5  EDIT-AREA-6    NC1244.2
106900                         EDIT-AREA-7  EDIT-AREA-8.                NC1244.2
107000     GO TO PICTURE-TEST-9-A.                                      NC1244.2
107100 PICTURE-DELETE-9.                                                NC1244.2
107200     PERFORM DE-LETE.                                             NC1244.2
107300     PERFORM PRINT-DETAIL.                                        NC1244.2
107400     GO TO PICTURE-INIT-10.                                       NC1244.2
107500 PICTURE-TEST-9-A.                                                NC1244.2
107600     IF EDIT-AREA-1 EQUAL TO "-0012"                              NC1244.2
107700        PERFORM PASS                                              NC1244.2
107800        PERFORM PRINT-DETAIL                                      NC1244.2
107900        ELSE PERFORM FAIL                                         NC1244.2
108000        MOVE EDIT-AREA-1 TO COMPUTED-A                            NC1244.2
108100        MOVE "-0012" TO CORRECT-A                                 NC1244.2
108200        PERFORM PRINT-DETAIL.                                     NC1244.2
108300     ADD 1 TO REC-CT.                                             NC1244.2
108400 PICTURE-TEST-9-B.                                                NC1244.2
108500     IF EDIT-AREA-2 EQUAL TO "-0012"                              NC1244.2
108600        PERFORM PASS                                              NC1244.2
108700        PERFORM PRINT-DETAIL                                      NC1244.2
108800        ELSE PERFORM FAIL                                         NC1244.2
108900        MOVE EDIT-AREA-2 TO COMPUTED-A                            NC1244.2
109000        MOVE "-0012" TO CORRECT-A                                 NC1244.2
109100        PERFORM PRINT-DETAIL.                                     NC1244.2
109200     ADD 1 TO REC-CT.                                             NC1244.2
109300 PICTURE-TEST-9-C.                                                NC1244.2
109400     IF EDIT-AREA-3 EQUAL TO "  -12"                              NC1244.2
109500        PERFORM PASS                                              NC1244.2
109600        PERFORM PRINT-DETAIL                                      NC1244.2
109700        ELSE PERFORM FAIL                                         NC1244.2
109800        MOVE EDIT-AREA-3 TO COMPUTED-A                            NC1244.2
109900        MOVE "  -12" TO CORRECT-A                                 NC1244.2
110000        PERFORM PRINT-DETAIL.                                     NC1244.2
110100     ADD 1 TO REC-CT.                                             NC1244.2
110200 PICTURE-TEST-9-D.                                                NC1244.2
110300     IF EDIT-AREA-4 EQUAL TO "  -12"                              NC1244.2
110400        PERFORM PASS                                              NC1244.2
110500        PERFORM PRINT-DETAIL                                      NC1244.2
110600        ELSE PERFORM FAIL                                         NC1244.2
110700        MOVE EDIT-AREA-4 TO COMPUTED-A                            NC1244.2
110800        MOVE "  -12" TO CORRECT-A                                 NC1244.2
110900        PERFORM PRINT-DETAIL.                                     NC1244.2
111000     ADD 1 TO REC-CT.                                             NC1244.2
111100 PICTURE-TEST-9-E.                                                NC1244.2
111200     IF EDIT-AREA-5 EQUAL TO "  -12"                              NC1244.2
111300        PERFORM PASS                                              NC1244.2
111400        PERFORM PRINT-DETAIL                                      NC1244.2
111500        ELSE PERFORM FAIL                                         NC1244.2
111600        MOVE EDIT-AREA-5 TO COMPUTED-A                            NC1244.2
111700        MOVE "  -12" TO CORRECT-A                                 NC1244.2
111800        PERFORM PRINT-DETAIL.                                     NC1244.2
111900     ADD 1 TO REC-CT.                                             NC1244.2
112000 PICTURE-TEST-9-F.                                                NC1244.2
112100     IF EDIT-AREA-6 EQUAL TO "  -12"                              NC1244.2
112200        PERFORM PASS                                              NC1244.2
112300        PERFORM PRINT-DETAIL                                      NC1244.2
112400        ELSE PERFORM FAIL                                         NC1244.2
112500        MOVE EDIT-AREA-6 TO COMPUTED-A                            NC1244.2
112600        MOVE "  -12" TO CORRECT-A                                 NC1244.2
112700        PERFORM PRINT-DETAIL.                                     NC1244.2
112800     ADD 1 TO REC-CT.                                             NC1244.2
112900 PICTURE-TEST-9-G.                                                NC1244.2
113000     IF EDIT-AREA-7 EQUAL TO "  -12.34"                           NC1244.2
113100        PERFORM PASS                                              NC1244.2
113200        PERFORM PRINT-DETAIL                                      NC1244.2
113300        ELSE PERFORM FAIL                                         NC1244.2
113400        MOVE EDIT-AREA-7 TO COMPUTED-A                            NC1244.2
113500        MOVE "  -12.34" TO CORRECT-A                              NC1244.2
113600        PERFORM PRINT-DETAIL.                                     NC1244.2
113700     ADD 1 TO REC-CT.                                             NC1244.2
113800 PICTURE-TEST-9-H.                                                NC1244.2
113900     IF EDIT-AREA-8 EQUAL TO "   -12.34"                          NC1244.2
114000        PERFORM PASS                                              NC1244.2
114100        PERFORM PRINT-DETAIL                                      NC1244.2
114200        ELSE PERFORM FAIL                                         NC1244.2
114300        MOVE EDIT-AREA-8 TO COMPUTED-A                            NC1244.2
114400        MOVE "   -12.34" TO CORRECT-A                             NC1244.2
114500        PERFORM PRINT-DETAIL.                                     NC1244.2
114600 PICTURE-INIT-10.                                                 NC1244.2
114700     MOVE "VI-31"           TO ANSI-REFERENCE.                    NC1244.2
114800     MOVE "PCTRE-TST-10" TO PAR-NAME.                             NC1244.2
114900     MOVE "PICTURE CHARACTER l" TO FEATURE.                       NC1244.2
115000     MOVE  0               TO WORK-AREA-10.                       NC1244.2
115100     MOVE 1 TO REC-CT.                                            NC1244.2
115200 PICTURE-TEST-10.                                                 NC1244.2
115300     MOVE WORK-AREA-10 TO EDIT-AREA-9  EDIT-AREA-10               NC1244.2
115400                          EDIT-AREA-11  EDIT-AREA-12.             NC1244.2
115500     GO TO PICTURE-TEST-10-A.                                     NC1244.2
115600 PICTURE-DELETE-10.                                               NC1244.2
115700     PERFORM DE-LETE.                                             NC1244.2
115800     PERFORM PRINT-DETAIL.                                        NC1244.2
115900     GO TO PICTURE-INIT-11.                                       NC1244.2
116000 PICTURE-TEST-10-A.                                               NC1244.2
116100     IF EDIT-AREA-9 EQUAL TO " $00"                               NC1244.2
116200        PERFORM PASS                                              NC1244.2
116300        PERFORM PRINT-DETAIL                                      NC1244.2
116400        ELSE PERFORM FAIL                                         NC1244.2
116500        MOVE EDIT-AREA-9 TO COMPUTED-A                            NC1244.2
116600        MOVE " $00" TO CORRECT-A                                  NC1244.2
116700        PERFORM PRINT-DETAIL.                                     NC1244.2
116800     ADD 1 TO REC-CT.                                             NC1244.2
116900 PICTURE-TEST-10-B.                                               NC1244.2
117000     IF EDIT-AREA-10 EQUAL TO "   $0"                             NC1244.2
117100        PERFORM PASS                                              NC1244.2
117200        PERFORM PRINT-DETAIL                                      NC1244.2
117300        ELSE PERFORM FAIL                                         NC1244.2
117400        MOVE EDIT-AREA-10 TO COMPUTED-A                           NC1244.2
117500        MOVE "   $0" TO CORRECT-A                                 NC1244.2
117600        PERFORM PRINT-DETAIL.                                     NC1244.2
117700     ADD 1 TO REC-CT.                                             NC1244.2
117800 PICTURE-TEST-10-C.                                               NC1244.2
117900     IF EDIT-AREA-11 EQUAL TO "    $.00"                          NC1244.2
118000        PERFORM PASS                                              NC1244.2
118100        PERFORM PRINT-DETAIL                                      NC1244.2
118200        ELSE PERFORM FAIL                                         NC1244.2
118300        MOVE EDIT-AREA-11 TO COMPUTED-A                           NC1244.2
118400        MOVE "    $.00" TO CORRECT-A                              NC1244.2
118500        PERFORM PRINT-DETAIL.                                     NC1244.2
118600     ADD 1 TO REC-CT.                                             NC1244.2
118700 PICTURE-TEST-10-D.                                               NC1244.2
118800     IF EDIT-AREA-12 EQUAL TO "         "                         NC1244.2
118900        PERFORM PASS                                              NC1244.2
119000        PERFORM PRINT-DETAIL                                      NC1244.2
119100        ELSE PERFORM FAIL                                         NC1244.2
119200        MOVE EDIT-AREA-12 TO COMPUTED-A                           NC1244.2
119300        MOVE "SPACES" TO CORRECT-A                                NC1244.2
119400        PERFORM PRINT-DETAIL.                                     NC1244.2
119500 PICTURE-INIT-11.                                                 NC1244.2
119600     MOVE "VI-31"           TO ANSI-REFERENCE.                    NC1244.2
119700     MOVE "PCTRE-TST-11" TO PAR-NAME.                             NC1244.2
119800     MOVE "PICTURE CHARACTER l" TO FEATURE.                       NC1244.2
119900     MOVE  .02                  TO WORK-AREA-11.                  NC1244.2
120000     MOVE 1 TO REC-CT.                                            NC1244.2
120100 PICTURE-TEST-11.                                                 NC1244.2
120200     MOVE WORK-AREA-11 TO EDIT-AREA-9  EDIT-AREA-10               NC1244.2
120300                          EDIT-AREA-11  EDIT-AREA-12.             NC1244.2
120400     GO TO PICTURE-TEST-11-A.                                     NC1244.2
120500 PICTURE-DELETE-11.                                               NC1244.2
120600     PERFORM DE-LETE.                                             NC1244.2
120700     PERFORM PRINT-DETAIL.                                        NC1244.2
120800     GO TO PICTURE-INIT-12.                                       NC1244.2
120900 PICTURE-TEST-11-A.                                               NC1244.2
121000     IF EDIT-AREA-9 EQUAL TO " $00"                               NC1244.2
121100        PERFORM PASS                                              NC1244.2
121200        PERFORM PRINT-DETAIL                                      NC1244.2
121300        ELSE PERFORM FAIL                                         NC1244.2
121400        MOVE EDIT-AREA-9 TO COMPUTED-A                            NC1244.2
121500        MOVE " $00" TO CORRECT-A                                  NC1244.2
121600        PERFORM PRINT-DETAIL.                                     NC1244.2
121700     ADD 1 TO REC-CT.                                             NC1244.2
121800 PICTURE-TEST-11-B.                                               NC1244.2
121900     IF EDIT-AREA-10 EQUAL TO "   $0"                             NC1244.2
122000        PERFORM PASS                                              NC1244.2
122100        PERFORM PRINT-DETAIL                                      NC1244.2
122200        ELSE PERFORM FAIL                                         NC1244.2
122300        MOVE EDIT-AREA-10 TO COMPUTED-A                           NC1244.2
122400        MOVE "   $0" TO CORRECT-A                                 NC1244.2
122500        PERFORM PRINT-DETAIL.                                     NC1244.2
122600     ADD 1 TO REC-CT.                                             NC1244.2
122700 PICTURE-TEST-11-C.                                               NC1244.2
122800     IF EDIT-AREA-11 EQUAL TO "    $.02"                          NC1244.2
122900        PERFORM PASS                                              NC1244.2
123000        PERFORM PRINT-DETAIL                                      NC1244.2
123100        ELSE PERFORM FAIL                                         NC1244.2
123200        MOVE EDIT-AREA-11 TO COMPUTED-A                           NC1244.2
123300        MOVE "    $.02" TO CORRECT-A                              NC1244.2
123400        PERFORM PRINT-DETAIL.                                     NC1244.2
123500     ADD 1 TO REC-CT.                                             NC1244.2
123600 PICTURE-TEST-11-D.                                               NC1244.2
123700     IF EDIT-AREA-12 EQUAL TO "     $.02"                         NC1244.2
123800        PERFORM PASS                                              NC1244.2
123900        PERFORM PRINT-DETAIL                                      NC1244.2
124000        ELSE PERFORM FAIL                                         NC1244.2
124100        MOVE EDIT-AREA-12 TO COMPUTED-A                           NC1244.2
124200        MOVE "     $.02" TO CORRECT-A                             NC1244.2
124300        PERFORM PRINT-DETAIL.                                     NC1244.2
124400 PICTURE-INIT-12.                                                 NC1244.2
124500     MOVE "VI-31"           TO ANSI-REFERENCE.                    NC1244.2
124600     MOVE "PCTRE-TST-12" TO PAR-NAME.                             NC1244.2
124700     MOVE "PICTURE CHARACTER l" TO FEATURE.                       NC1244.2
124800     MOVE  12                   TO WORK-AREA-12.                  NC1244.2
124900     MOVE 1 TO REC-CT.                                            NC1244.2
125000 PICTURE-TEST-12.                                                 NC1244.2
125100     MOVE WORK-AREA-12 TO EDIT-AREA-9  EDIT-AREA-10               NC1244.2
125200                          EDIT-AREA-11  EDIT-AREA-12.             NC1244.2
125300     GO TO PICTURE-TEST-12-A.                                     NC1244.2
125400 PICTURE-DELETE-12.                                               NC1244.2
125500     PERFORM DE-LETE.                                             NC1244.2
125600     PERFORM PRINT-DETAIL.                                        NC1244.2
125700     GO TO PICTURE-INIT-13.                                       NC1244.2
125800 PICTURE-TEST-12-A.                                               NC1244.2
125900     IF EDIT-AREA-9 EQUAL TO " $12"                               NC1244.2
126000        PERFORM PASS                                              NC1244.2
126100        PERFORM PRINT-DETAIL                                      NC1244.2
126200        ELSE PERFORM FAIL                                         NC1244.2
126300        MOVE EDIT-AREA-9 TO COMPUTED-A                            NC1244.2
126400        MOVE " $12" TO CORRECT-A                                  NC1244.2
126500        PERFORM PRINT-DETAIL.                                     NC1244.2
126600     ADD 1 TO REC-CT.                                             NC1244.2
126700 PICTURE-TEST-12-B.                                               NC1244.2
126800     IF EDIT-AREA-10 EQUAL TO "  $12"                             NC1244.2
126900        PERFORM PASS                                              NC1244.2
127000        PERFORM PRINT-DETAIL                                      NC1244.2
127100        ELSE PERFORM FAIL                                         NC1244.2
127200        MOVE EDIT-AREA-10 TO COMPUTED-A                           NC1244.2
127300        MOVE "  $12" TO CORRECT-A                                 NC1244.2
127400        PERFORM PRINT-DETAIL.                                     NC1244.2
127500     ADD 1 TO REC-CT.                                             NC1244.2
127600 PICTURE-TEST-12-C.                                               NC1244.2
127700     IF EDIT-AREA-11 EQUAL TO "  $12.00"                          NC1244.2
127800        PERFORM PASS                                              NC1244.2
127900        PERFORM PRINT-DETAIL                                      NC1244.2
128000        ELSE PERFORM FAIL                                         NC1244.2
128100        MOVE EDIT-AREA-11 TO COMPUTED-A                           NC1244.2
128200        MOVE "  $12.00" TO CORRECT-A                              NC1244.2
128300        PERFORM PRINT-DETAIL.                                     NC1244.2
128400     ADD 1 TO REC-CT.                                             NC1244.2
128500 PICTURE-TEST-12-D.                                               NC1244.2
128600     IF EDIT-AREA-12 EQUAL TO "   $12.00"                         NC1244.2
128700        PERFORM PASS                                              NC1244.2
128800        PERFORM PRINT-DETAIL                                      NC1244.2
128900        ELSE PERFORM FAIL                                         NC1244.2
129000        MOVE EDIT-AREA-12 TO COMPUTED-A                           NC1244.2
129100        MOVE "   $12.00" TO CORRECT-A                             NC1244.2
129200        PERFORM PRINT-DETAIL.                                     NC1244.2
129300 PICTURE-INIT-13.                                                 NC1244.2
129400     MOVE "VI-31"           TO ANSI-REFERENCE.                    NC1244.2
129500     MOVE "PCTRE-TST-13" TO PAR-NAME.                             NC1244.2
129600     MOVE "PICTURE CHARACTER l" TO FEATURE.                       NC1244.2
129700     MOVE  12.34                TO WORK-AREA-13.                  NC1244.2
129800     MOVE 1 TO REC-CT.                                            NC1244.2
129900 PICTURE-TEST-13.                                                 NC1244.2
130000     MOVE WORK-AREA-13 TO EDIT-AREA-9  EDIT-AREA-10               NC1244.2
130100                          EDIT-AREA-11  EDIT-AREA-12.             NC1244.2
130200     GO TO PICTURE-TEST-13-A.                                     NC1244.2
130300 PICTURE-DELETE-13.                                               NC1244.2
130400     PERFORM DE-LETE.                                             NC1244.2
130500     PERFORM PRINT-DETAIL.                                        NC1244.2
130600     GO TO PICTURE-INIT-14.                                       NC1244.2
130700 PICTURE-TEST-13-A.                                               NC1244.2
130800     IF EDIT-AREA-9 EQUAL TO " $12"                               NC1244.2
130900        PERFORM PASS                                              NC1244.2
131000        PERFORM PRINT-DETAIL                                      NC1244.2
131100        ELSE PERFORM FAIL                                         NC1244.2
131200        MOVE EDIT-AREA-9 TO COMPUTED-A                            NC1244.2
131300        MOVE " $12" TO CORRECT-A                                  NC1244.2
131400        PERFORM PRINT-DETAIL.                                     NC1244.2
131500     ADD 1 TO REC-CT.                                             NC1244.2
131600 PICTURE-TEST-13-B.                                               NC1244.2
131700     IF EDIT-AREA-10 EQUAL TO "  $12"                             NC1244.2
131800        PERFORM PASS                                              NC1244.2
131900        PERFORM PRINT-DETAIL                                      NC1244.2
132000        ELSE PERFORM FAIL                                         NC1244.2
132100        MOVE EDIT-AREA-10 TO COMPUTED-A                           NC1244.2
132200        MOVE "  $12" TO CORRECT-A                                 NC1244.2
132300        PERFORM PRINT-DETAIL.                                     NC1244.2
132400     ADD 1 TO REC-CT.                                             NC1244.2
132500 PICTURE-TEST-13-C.                                               NC1244.2
132600     IF EDIT-AREA-11 EQUAL TO "  $12.34"                          NC1244.2
132700        PERFORM PASS                                              NC1244.2
132800        PERFORM PRINT-DETAIL                                      NC1244.2
132900        ELSE PERFORM FAIL                                         NC1244.2
133000        MOVE EDIT-AREA-11 TO COMPUTED-A                           NC1244.2
133100        MOVE "  $12.34" TO CORRECT-A                              NC1244.2
133200        PERFORM PRINT-DETAIL.                                     NC1244.2
133300     ADD 1 TO REC-CT.                                             NC1244.2
133400 PICTURE-TEST-13-D.                                               NC1244.2
133500     IF EDIT-AREA-12 EQUAL TO "   $12.34"                         NC1244.2
133600        PERFORM PASS                                              NC1244.2
133700        PERFORM PRINT-DETAIL                                      NC1244.2
133800        ELSE PERFORM FAIL                                         NC1244.2
133900        MOVE EDIT-AREA-12 TO COMPUTED-A                           NC1244.2
134000        MOVE "   $12.34" TO CORRECT-A                             NC1244.2
134100        PERFORM PRINT-DETAIL.                                     NC1244.2
134200 PICTURE-INIT-14.                                                 NC1244.2
134300     MOVE "VI-31"           TO ANSI-REFERENCE.                    NC1244.2
134400     MOVE "PCTRE-TST-14" TO PAR-NAME.                             NC1244.2
134500     MOVE "PICTURE CHARACTER l" TO FEATURE.                       NC1244.2
134600     MOVE  1234                 TO WORK-AREA-14.                  NC1244.2
134700     MOVE 1 TO REC-CT.                                            NC1244.2
134800 PICTURE-TEST-14.                                                 NC1244.2
134900     MOVE WORK-AREA-14 TO EDIT-AREA-9  EDIT-AREA-10               NC1244.2
135000                          EDIT-AREA-11  EDIT-AREA-12.             NC1244.2
135100     GO TO PICTURE-TEST-14-A.                                     NC1244.2
135200 PICTURE-DELETE-14.                                               NC1244.2
135300     PERFORM DE-LETE.                                             NC1244.2
135400     PERFORM PRINT-DETAIL.                                        NC1244.2
135500     GO TO PICTURE-INIT-15.                                       NC1244.2
135600 PICTURE-TEST-14-A.                                               NC1244.2
135700     IF EDIT-AREA-9 EQUAL TO "$234"                               NC1244.2
135800        PERFORM PASS                                              NC1244.2
135900        PERFORM PRINT-DETAIL                                      NC1244.2
136000        ELSE PERFORM FAIL                                         NC1244.2
136100        MOVE EDIT-AREA-9 TO COMPUTED-A                            NC1244.2
136200        MOVE "$234" TO CORRECT-A                                  NC1244.2
136300        PERFORM PRINT-DETAIL.                                     NC1244.2
136400     ADD 1 TO REC-CT.                                             NC1244.2
136500 PICTURE-TEST-14-B.                                               NC1244.2
136600     IF EDIT-AREA-10 EQUAL TO "$1234"                             NC1244.2
136700        PERFORM PASS                                              NC1244.2
136800        PERFORM PRINT-DETAIL                                      NC1244.2
136900        ELSE PERFORM FAIL                                         NC1244.2
137000        MOVE EDIT-AREA-10 TO COMPUTED-A                           NC1244.2
137100        MOVE "$1234" TO CORRECT-A                                 NC1244.2
137200        PERFORM PRINT-DETAIL.                                     NC1244.2
137300     ADD 1 TO REC-CT.                                             NC1244.2
137400 PICTURE-TEST-14-C.                                               NC1244.2
137500     IF EDIT-AREA-11 EQUAL TO "$1234.00"                          NC1244.2
137600        PERFORM PASS                                              NC1244.2
137700        PERFORM PRINT-DETAIL                                      NC1244.2
137800        ELSE PERFORM FAIL                                         NC1244.2
137900        MOVE EDIT-AREA-11 TO COMPUTED-A                           NC1244.2
138000        MOVE "$1234.00" TO CORRECT-A                              NC1244.2
138100        PERFORM PRINT-DETAIL.                                     NC1244.2
138200     ADD 1 TO REC-CT.                                             NC1244.2
138300 PICTURE-TEST-14-D.                                               NC1244.2
138400     IF EDIT-AREA-12 EQUAL TO "$1,234.00"                         NC1244.2
138500        PERFORM PASS                                              NC1244.2
138600        PERFORM PRINT-DETAIL                                      NC1244.2
138700        ELSE PERFORM FAIL                                         NC1244.2
138800        MOVE EDIT-AREA-12 TO COMPUTED-A                           NC1244.2
138900        MOVE "$1,234.00" TO CORRECT-A                             NC1244.2
139000        PERFORM PRINT-DETAIL.                                     NC1244.2
139100 PICTURE-INIT-15.                                                 NC1244.2
139200     MOVE "VI-31"           TO ANSI-REFERENCE.                    NC1244.2
139300     MOVE "PCTRE-TST-15" TO PAR-NAME.                             NC1244.2
139400     MOVE "PICTURE CHARACTER l" TO FEATURE.                       NC1244.2
139500     MOVE  1234.56              TO WORK-AREA-15.                  NC1244.2
139600     MOVE 1 TO REC-CT.                                            NC1244.2
139700 PICTURE-TEST-15.                                                 NC1244.2
139800     MOVE WORK-AREA-15 TO EDIT-AREA-9  EDIT-AREA-10               NC1244.2
139900                          EDIT-AREA-11  EDIT-AREA-12.             NC1244.2
140000     GO TO PICTURE-TEST-15-A.                                     NC1244.2
140100 PICTURE-DELETE-15.                                               NC1244.2
140200     PERFORM DE-LETE.                                             NC1244.2
140300     PERFORM PRINT-DETAIL.                                        NC1244.2
140400     GO TO PICTURE-INIT-16.                                       NC1244.2
140500 PICTURE-TEST-15-A.                                               NC1244.2
140600     IF EDIT-AREA-9 EQUAL TO "$234"                               NC1244.2
140700        PERFORM PASS                                              NC1244.2
140800        PERFORM PRINT-DETAIL                                      NC1244.2
140900        ELSE PERFORM FAIL                                         NC1244.2
141000        MOVE EDIT-AREA-9 TO COMPUTED-A                            NC1244.2
141100        MOVE "$234" TO CORRECT-A                                  NC1244.2
141200        PERFORM PRINT-DETAIL.                                     NC1244.2
141300     ADD 1 TO REC-CT.                                             NC1244.2
141400 PICTURE-TEST-15-B.                                               NC1244.2
141500     IF EDIT-AREA-10 EQUAL TO "$1234"                             NC1244.2
141600        PERFORM PASS                                              NC1244.2
141700        PERFORM PRINT-DETAIL                                      NC1244.2
141800        ELSE PERFORM FAIL                                         NC1244.2
141900        MOVE EDIT-AREA-10 TO COMPUTED-A                           NC1244.2
142000        MOVE "$1234" TO CORRECT-A                                 NC1244.2
142100        PERFORM PRINT-DETAIL.                                     NC1244.2
142200     ADD 1 TO REC-CT.                                             NC1244.2
142300 PICTURE-TEST-15-C.                                               NC1244.2
142400     IF EDIT-AREA-11 EQUAL TO "$1234.56"                          NC1244.2
142500        PERFORM PASS                                              NC1244.2
142600        PERFORM PRINT-DETAIL                                      NC1244.2
142700        ELSE PERFORM FAIL                                         NC1244.2
142800        MOVE EDIT-AREA-11 TO COMPUTED-A                           NC1244.2
142900        MOVE "$1234.56" TO CORRECT-A                              NC1244.2
143000        PERFORM PRINT-DETAIL.                                     NC1244.2
143100     ADD 1 TO REC-CT.                                             NC1244.2
143200 PICTURE-TEST-15-D.                                               NC1244.2
143300     IF EDIT-AREA-12 EQUAL TO "$1,234.56"                         NC1244.2
143400        PERFORM PASS                                              NC1244.2
143500        PERFORM PRINT-DETAIL                                      NC1244.2
143600        ELSE PERFORM FAIL                                         NC1244.2
143700        MOVE EDIT-AREA-12 TO COMPUTED-A                           NC1244.2
143800        MOVE "$1,234.56" TO CORRECT-A                             NC1244.2
143900        PERFORM PRINT-DETAIL.                                     NC1244.2
144000 PICTURE-INIT-16.                                                 NC1244.2
144100     MOVE "VI-31"           TO ANSI-REFERENCE.                    NC1244.2
144200     MOVE "PCTRE-TST-16" TO PAR-NAME.                             NC1244.2
144300     MOVE "PICTURE CHARACTER *" TO FEATURE.                       NC1244.2
144400     MOVE  0                    TO WORK-AREA-16.                  NC1244.2
144500     MOVE 1 TO REC-CT.                                            NC1244.2
144600 PICTURE-TEST-16.                                                 NC1244.2
144700     MOVE WORK-AREA-16 TO EDIT-AREA-13  EDIT-AREA-14  EDIT-AREA-15NC1244.2
144800                          EDIT-AREA-16  EDIT-AREA-17.             NC1244.2
144900     GO TO PICTURE-TEST-16-A.                                     NC1244.2
145000 PICTURE-DELETE-16.                                               NC1244.2
145100     PERFORM DE-LETE.                                             NC1244.2
145200     PERFORM PRINT-DETAIL.                                        NC1244.2
145300     GO TO PICTURE-INIT-17.                                       NC1244.2
145400 PICTURE-TEST-16-A.                                               NC1244.2
145500     IF EDIT-AREA-13 EQUAL TO "*000"                              NC1244.2
145600        PERFORM PASS                                              NC1244.2
145700        PERFORM PRINT-DETAIL                                      NC1244.2
145800        ELSE PERFORM FAIL                                         NC1244.2
145900        MOVE EDIT-AREA-13 TO COMPUTED-A                           NC1244.2
146000        MOVE "*000" TO CORRECT-A                                  NC1244.2
146100        PERFORM PRINT-DETAIL.                                     NC1244.2
146200     ADD 1 TO REC-CT.                                             NC1244.2
146300 PICTURE-TEST-16-B.                                               NC1244.2
146400     IF EDIT-AREA-14 EQUAL TO "**00"                              NC1244.2
146500        PERFORM PASS                                              NC1244.2
146600        PERFORM PRINT-DETAIL                                      NC1244.2
146700        ELSE PERFORM FAIL                                         NC1244.2
146800        MOVE EDIT-AREA-14 TO COMPUTED-A                           NC1244.2
146900        MOVE "**00" TO CORRECT-A                                  NC1244.2
147000        PERFORM PRINT-DETAIL.                                     NC1244.2
147100     ADD 1 TO REC-CT.                                             NC1244.2
147200 PICTURE-TEST-16-C.                                               NC1244.2
147300     IF EDIT-AREA-15 EQUAL TO "***0"                              NC1244.2
147400        PERFORM PASS                                              NC1244.2
147500        PERFORM PRINT-DETAIL                                      NC1244.2
147600        ELSE PERFORM FAIL                                         NC1244.2
147700        MOVE EDIT-AREA-15 TO COMPUTED-A                           NC1244.2
147800        MOVE "***0" TO CORRECT-A                                  NC1244.2
147900        PERFORM PRINT-DETAIL.                                     NC1244.2
148000     ADD 1 TO REC-CT.                                             NC1244.2
148100 PICTURE-TEST-16-D.                                               NC1244.2
148200     IF EDIT-AREA-16 EQUAL TO "**.**"                             NC1244.2
148300        PERFORM PASS                                              NC1244.2
148400        PERFORM PRINT-DETAIL                                      NC1244.2
148500        ELSE PERFORM FAIL                                         NC1244.2
148600        MOVE EDIT-AREA-16 TO COMPUTED-A                           NC1244.2
148700        MOVE "**.**" TO CORRECT-A                                 NC1244.2
148800        PERFORM PRINT-DETAIL.                                     NC1244.2
148900     ADD 1 TO REC-CT.                                             NC1244.2
149000 PICTURE-TEST-16-E.                                               NC1244.2
149100     IF EDIT-AREA-17 EQUAL TO "*****.**"                          NC1244.2
149200        PERFORM PASS                                              NC1244.2
149300        PERFORM PRINT-DETAIL                                      NC1244.2
149400        ELSE PERFORM FAIL                                         NC1244.2
149500        MOVE EDIT-AREA-17 TO COMPUTED-A                           NC1244.2
149600        MOVE "*****.**" TO CORRECT-A                              NC1244.2
149700        PERFORM PRINT-DETAIL.                                     NC1244.2
149800 PICTURE-INIT-17.                                                 NC1244.2
149900     MOVE "VI-31"           TO ANSI-REFERENCE.                    NC1244.2
150000     MOVE "PCTRE-TST-17" TO PAR-NAME.                             NC1244.2
150100     MOVE "PICTURE CHARACTER *" TO FEATURE.                       NC1244.2
150200     MOVE  13                   TO WORK-AREA-17.                  NC1244.2
150300     MOVE 1 TO REC-CT.                                            NC1244.2
150400 PICTURE-TEST-17.                                                 NC1244.2
150500     MOVE WORK-AREA-17 TO EDIT-AREA-13  EDIT-AREA-14  EDIT-AREA-15NC1244.2
150600                          EDIT-AREA-16  EDIT-AREA-17.             NC1244.2
150700     GO TO PICTURE-TEST-17-A.                                     NC1244.2
150800 PICTURE-DELETE-17.                                               NC1244.2
150900     PERFORM DE-LETE.                                             NC1244.2
151000     PERFORM PRINT-DETAIL.                                        NC1244.2
151100     GO TO PICTURE-INIT-18.                                       NC1244.2
151200 PICTURE-TEST-17-A.                                               NC1244.2
151300     IF EDIT-AREA-13 EQUAL TO "*013"                              NC1244.2
151400        PERFORM PASS                                              NC1244.2
151500        PERFORM PRINT-DETAIL                                      NC1244.2
151600        ELSE PERFORM FAIL                                         NC1244.2
151700        MOVE EDIT-AREA-13 TO COMPUTED-A                           NC1244.2
151800        MOVE "*013" TO CORRECT-A                                  NC1244.2
151900        PERFORM PRINT-DETAIL.                                     NC1244.2
152000     ADD 1 TO REC-CT.                                             NC1244.2
152100 PICTURE-TEST-17-B.                                               NC1244.2
152200     IF EDIT-AREA-14 EQUAL TO "**13"                              NC1244.2
152300        PERFORM PASS                                              NC1244.2
152400        PERFORM PRINT-DETAIL                                      NC1244.2
152500        ELSE PERFORM FAIL                                         NC1244.2
152600        MOVE EDIT-AREA-14 TO COMPUTED-A                           NC1244.2
152700        MOVE "**13" TO CORRECT-A                                  NC1244.2
152800        PERFORM PRINT-DETAIL.                                     NC1244.2
152900     ADD 1 TO REC-CT.                                             NC1244.2
153000 PICTURE-TEST-17-C.                                               NC1244.2
153100     IF EDIT-AREA-15 EQUAL TO "**13"                              NC1244.2
153200        PERFORM PASS                                              NC1244.2
153300        PERFORM PRINT-DETAIL                                      NC1244.2
153400        ELSE PERFORM FAIL                                         NC1244.2
153500        MOVE EDIT-AREA-15 TO COMPUTED-A                           NC1244.2
153600        MOVE "**13" TO CORRECT-A                                  NC1244.2
153700        PERFORM PRINT-DETAIL.                                     NC1244.2
153800     ADD 1 TO REC-CT.                                             NC1244.2
153900 PICTURE-TEST-17-D.                                               NC1244.2
154000     IF EDIT-AREA-16 EQUAL TO "13.00"                             NC1244.2
154100        PERFORM PASS                                              NC1244.2
154200        PERFORM PRINT-DETAIL                                      NC1244.2
154300        ELSE PERFORM FAIL                                         NC1244.2
154400        MOVE EDIT-AREA-16 TO COMPUTED-A                           NC1244.2
154500        MOVE "13.00" TO CORRECT-A                                 NC1244.2
154600        PERFORM PRINT-DETAIL.                                     NC1244.2
154700     ADD 1 TO REC-CT.                                             NC1244.2
154800 PICTURE-TEST-17-E.                                               NC1244.2
154900     IF EDIT-AREA-17 EQUAL TO "***13.00"                          NC1244.2
155000        PERFORM PASS                                              NC1244.2
155100        PERFORM PRINT-DETAIL                                      NC1244.2
155200        ELSE PERFORM FAIL                                         NC1244.2
155300        MOVE EDIT-AREA-17 TO COMPUTED-A                           NC1244.2
155400        MOVE "***13.00" TO CORRECT-A                              NC1244.2
155500        PERFORM PRINT-DETAIL.                                     NC1244.2
155600 PICTURE-INIT-18.                                                 NC1244.2
155700     MOVE "VI-31"           TO ANSI-REFERENCE.                    NC1244.2
155800     MOVE "PCTRE-TST-18" TO PAR-NAME.                             NC1244.2
155900     MOVE "PICTURE CHARACTER *" TO FEATURE.                       NC1244.2
156000     MOVE  123   TO WORK-AREA-18.                                 NC1244.2
156100     MOVE 1 TO REC-CT.                                            NC1244.2
156200 PICTURE-TEST-18.                                                 NC1244.2
156300     MOVE WORK-AREA-18 TO EDIT-AREA-13  EDIT-AREA-14  EDIT-AREA-15NC1244.2
156400                          EDIT-AREA-16  EDIT-AREA-17.             NC1244.2
156500     GO TO PICTURE-TEST-18-A.                                     NC1244.2
156600 PICTURE-DELETE-18.                                               NC1244.2
156700     PERFORM DE-LETE.                                             NC1244.2
156800     PERFORM PRINT-DETAIL.                                        NC1244.2
156900     GO TO PICTURE-INIT-19.                                       NC1244.2
157000 PICTURE-TEST-18-A.                                               NC1244.2
157100     IF EDIT-AREA-13 EQUAL TO "*123"                              NC1244.2
157200        PERFORM PASS                                              NC1244.2
157300        PERFORM PRINT-DETAIL                                      NC1244.2
157400        ELSE PERFORM FAIL                                         NC1244.2
157500        MOVE EDIT-AREA-13 TO COMPUTED-A                           NC1244.2
157600        MOVE "*123" TO CORRECT-A                                  NC1244.2
157700        PERFORM PRINT-DETAIL.                                     NC1244.2
157800     ADD 1 TO REC-CT.                                             NC1244.2
157900 PICTURE-TEST-18-B.                                               NC1244.2
158000     IF EDIT-AREA-14 EQUAL TO "*123"                              NC1244.2
158100        PERFORM PASS                                              NC1244.2
158200        PERFORM PRINT-DETAIL                                      NC1244.2
158300        ELSE PERFORM FAIL                                         NC1244.2
158400        MOVE EDIT-AREA-14 TO COMPUTED-A                           NC1244.2
158500        MOVE "*123" TO CORRECT-A                                  NC1244.2
158600        PERFORM PRINT-DETAIL.                                     NC1244.2
158700     ADD 1 TO REC-CT.                                             NC1244.2
158800 PICTURE-TEST-18-C.                                               NC1244.2
158900     IF EDIT-AREA-15 EQUAL TO "*123"                              NC1244.2
159000        PERFORM PASS                                              NC1244.2
159100        PERFORM PRINT-DETAIL                                      NC1244.2
159200        ELSE PERFORM FAIL                                         NC1244.2
159300        MOVE EDIT-AREA-15 TO COMPUTED-A                           NC1244.2
159400        MOVE "*123" TO CORRECT-A                                  NC1244.2
159500        PERFORM PRINT-DETAIL.                                     NC1244.2
159600     ADD 1 TO REC-CT.                                             NC1244.2
159700 PICTURE-TEST-18-D.                                               NC1244.2
159800     IF EDIT-AREA-16 EQUAL TO "23.00"                             NC1244.2
159900        PERFORM PASS                                              NC1244.2
160000        PERFORM PRINT-DETAIL                                      NC1244.2
160100        ELSE PERFORM FAIL                                         NC1244.2
160200        MOVE EDIT-AREA-16 TO COMPUTED-A                           NC1244.2
160300        MOVE "23.00" TO CORRECT-A                                 NC1244.2
160400        PERFORM PRINT-DETAIL.                                     NC1244.2
160500     ADD 1 TO REC-CT.                                             NC1244.2
160600 PICTURE-TEST-18-E.                                               NC1244.2
160700     IF EDIT-AREA-17 EQUAL TO "**123.00"                          NC1244.2
160800        PERFORM PASS                                              NC1244.2
160900        PERFORM PRINT-DETAIL                                      NC1244.2
161000        ELSE PERFORM FAIL                                         NC1244.2
161100        MOVE EDIT-AREA-17 TO COMPUTED-A                           NC1244.2
161200        MOVE "**123.00" TO CORRECT-A                              NC1244.2
161300        PERFORM PRINT-DETAIL.                                     NC1244.2
161400 PICTURE-INIT-19.                                                 NC1244.2
161500     MOVE "VI-31"           TO ANSI-REFERENCE.                    NC1244.2
161600     MOVE "PCTRE-TST-19" TO PAR-NAME.                             NC1244.2
161700     MOVE "PICTURE CHARACTER *" TO FEATURE.                       NC1244.2
161800     MOVE  2010  TO WORK-AREA-19.                                 NC1244.2
161900     MOVE 1 TO REC-CT.                                            NC1244.2
162000 PICTURE-TEST-19.                                                 NC1244.2
162100     MOVE WORK-AREA-19 TO EDIT-AREA-13  EDIT-AREA-14  EDIT-AREA-15NC1244.2
162200                          EDIT-AREA-16  EDIT-AREA-17.             NC1244.2
162300     GO TO PICTURE-TEST-19-A.                                     NC1244.2
162400 PICTURE-DELETE-19.                                               NC1244.2
162500     PERFORM DE-LETE.                                             NC1244.2
162600     PERFORM PRINT-DETAIL.                                        NC1244.2
162700     GO TO PICTURE-INIT-20.                                       NC1244.2
162800 PICTURE-TEST-19-A.                                               NC1244.2
162900     IF EDIT-AREA-13 EQUAL TO "2010"                              NC1244.2
163000        PERFORM PASS                                              NC1244.2
163100        PERFORM PRINT-DETAIL                                      NC1244.2
163200        ELSE PERFORM FAIL                                         NC1244.2
163300        MOVE EDIT-AREA-13 TO COMPUTED-A                           NC1244.2
163400        MOVE "2010" TO CORRECT-A                                  NC1244.2
163500        PERFORM PRINT-DETAIL.                                     NC1244.2
163600     ADD 1 TO REC-CT.                                             NC1244.2
163700 PICTURE-TEST-19-B.                                               NC1244.2
163800     IF EDIT-AREA-14 EQUAL TO "2010"                              NC1244.2
163900        PERFORM PASS                                              NC1244.2
164000        PERFORM PRINT-DETAIL                                      NC1244.2
164100        ELSE PERFORM FAIL                                         NC1244.2
164200        MOVE EDIT-AREA-14 TO COMPUTED-A                           NC1244.2
164300        MOVE "2010" TO CORRECT-A                                  NC1244.2
164400        PERFORM PRINT-DETAIL.                                     NC1244.2
164500     ADD 1 TO REC-CT.                                             NC1244.2
164600 PICTURE-TEST-19-C.                                               NC1244.2
164700     IF EDIT-AREA-15 EQUAL TO "2010"                              NC1244.2
164800        PERFORM PASS                                              NC1244.2
164900        PERFORM PRINT-DETAIL                                      NC1244.2
165000        ELSE PERFORM FAIL                                         NC1244.2
165100        MOVE EDIT-AREA-15 TO COMPUTED-A                           NC1244.2
165200        MOVE "2010" TO CORRECT-A                                  NC1244.2
165300        PERFORM PRINT-DETAIL.                                     NC1244.2
165400     ADD 1 TO REC-CT.                                             NC1244.2
165500 PICTURE-TEST-19-D.                                               NC1244.2
165600     IF EDIT-AREA-16 EQUAL TO "10.00"                             NC1244.2
165700        PERFORM PASS                                              NC1244.2
165800        PERFORM PRINT-DETAIL                                      NC1244.2
165900        ELSE PERFORM FAIL                                         NC1244.2
166000        MOVE EDIT-AREA-16 TO COMPUTED-A                           NC1244.2
166100        MOVE "10.00" TO CORRECT-A                                 NC1244.2
166200        PERFORM PRINT-DETAIL.                                     NC1244.2
166300     ADD 1 TO REC-CT.                                             NC1244.2
166400 PICTURE-TEST-19-E.                                               NC1244.2
166500     IF EDIT-AREA-17 EQUAL TO "2,010.00"                          NC1244.2
166600        PERFORM PASS                                              NC1244.2
166700        PERFORM PRINT-DETAIL                                      NC1244.2
166800        ELSE PERFORM FAIL                                         NC1244.2
166900        MOVE EDIT-AREA-17 TO COMPUTED-A                           NC1244.2
167000        MOVE "2,010.00" TO CORRECT-A                              NC1244.2
167100        PERFORM PRINT-DETAIL.                                     NC1244.2
167200 PICTURE-INIT-20.                                                 NC1244.2
167300     MOVE "VI-31"           TO ANSI-REFERENCE.                    NC1244.2
167400     MOVE "PCTRE-TST-20" TO PAR-NAME.                             NC1244.2
167500     MOVE "PICTURE CHARACTER *" TO FEATURE.                       NC1244.2
167600     MOVE 1 TO REC-CT.                                            NC1244.2
167700     MOVE  1010.2 TO WORK-AREA-20.                                NC1244.2
167800 PICTURE-TEST-20.                                                 NC1244.2
167900     MOVE WORK-AREA-20 TO EDIT-AREA-13  EDIT-AREA-14  EDIT-AREA-15NC1244.2
168000                          EDIT-AREA-16  EDIT-AREA-17.             NC1244.2
168100     GO TO PICTURE-TEST-20-A.                                     NC1244.2
168200 PICTURE-DELETE-20.                                               NC1244.2
168300     PERFORM DE-LETE.                                             NC1244.2
168400     PERFORM PRINT-DETAIL.                                        NC1244.2
168500     GO TO PICTURE-INIT-21.                                       NC1244.2
168600 PICTURE-TEST-20-A.                                               NC1244.2
168700     IF EDIT-AREA-13 EQUAL TO "1010"                              NC1244.2
168800        PERFORM PASS                                              NC1244.2
168900        PERFORM PRINT-DETAIL                                      NC1244.2
169000        ELSE PERFORM FAIL                                         NC1244.2
169100        MOVE EDIT-AREA-13 TO COMPUTED-A                           NC1244.2
169200        MOVE "1010" TO CORRECT-A                                  NC1244.2
169300        PERFORM PRINT-DETAIL.                                     NC1244.2
169400     ADD 1 TO REC-CT.                                             NC1244.2
169500 PICTURE-TEST-20-B.                                               NC1244.2
169600     IF EDIT-AREA-14 EQUAL TO "1010"                              NC1244.2
169700        PERFORM PASS                                              NC1244.2
169800        PERFORM PRINT-DETAIL                                      NC1244.2
169900        ELSE PERFORM FAIL                                         NC1244.2
170000        MOVE EDIT-AREA-14 TO COMPUTED-A                           NC1244.2
170100        MOVE "1010" TO CORRECT-A                                  NC1244.2
170200        PERFORM PRINT-DETAIL.                                     NC1244.2
170300     ADD 1 TO REC-CT.                                             NC1244.2
170400 PICTURE-TEST-20-C.                                               NC1244.2
170500     IF EDIT-AREA-15 EQUAL TO "1010"                              NC1244.2
170600        PERFORM PASS                                              NC1244.2
170700        PERFORM PRINT-DETAIL                                      NC1244.2
170800        ELSE PERFORM FAIL                                         NC1244.2
170900        MOVE EDIT-AREA-15 TO COMPUTED-A                           NC1244.2
171000        MOVE "1010" TO CORRECT-A                                  NC1244.2
171100        PERFORM PRINT-DETAIL.                                     NC1244.2
171200     ADD 1 TO REC-CT.                                             NC1244.2
171300 PICTURE-TEST-20-D.                                               NC1244.2
171400     IF EDIT-AREA-16 EQUAL TO "10.20"                             NC1244.2
171500        PERFORM PASS                                              NC1244.2
171600        PERFORM PRINT-DETAIL                                      NC1244.2
171700        ELSE PERFORM FAIL                                         NC1244.2
171800        MOVE EDIT-AREA-16 TO COMPUTED-A                           NC1244.2
171900        MOVE "10.20" TO CORRECT-A                                 NC1244.2
172000        PERFORM PRINT-DETAIL.                                     NC1244.2
172100     ADD 1 TO REC-CT.                                             NC1244.2
172200 PICTURE-TEST-20-E.                                               NC1244.2
172300     IF EDIT-AREA-17 EQUAL TO "1,010.20"                          NC1244.2
172400        PERFORM PASS                                              NC1244.2
172500        PERFORM PRINT-DETAIL                                      NC1244.2
172600        ELSE PERFORM FAIL                                         NC1244.2
172700        MOVE EDIT-AREA-17 TO COMPUTED-A                           NC1244.2
172800        MOVE "1,010.20" TO CORRECT-A                              NC1244.2
172900        PERFORM PRINT-DETAIL.                                     NC1244.2
173000 PICTURE-INIT-21.                                                 NC1244.2
173100     MOVE "VI-31"           TO ANSI-REFERENCE.                    NC1244.2
173200     MOVE "PCTRE-TST-21" TO PAR-NAME.                             NC1244.2
173300     MOVE "PICTURE CHARACTER *" TO FEATURE.                       NC1244.2
173400     MOVE  .01    TO WORK-AREA-21.                                NC1244.2
173500     MOVE 1 TO REC-CT.                                            NC1244.2
173600 PICTURE-TEST-21.                                                 NC1244.2
173700     MOVE WORK-AREA-21 TO EDIT-AREA-13  EDIT-AREA-14  EDIT-AREA-15NC1244.2
173800                          EDIT-AREA-16  EDIT-AREA-17.             NC1244.2
173900     GO TO PICTURE-TEST-21-A.                                     NC1244.2
174000 PICTURE-DELETE-21.                                               NC1244.2
174100     PERFORM DE-LETE.                                             NC1244.2
174200     PERFORM PRINT-DETAIL.                                        NC1244.2
174300     GO TO PICTURE-INIT-22.                                       NC1244.2
174400 PICTURE-TEST-21-A.                                               NC1244.2
174500     IF EDIT-AREA-13 EQUAL TO "*000"                              NC1244.2
174600        PERFORM PASS                                              NC1244.2
174700        PERFORM PRINT-DETAIL                                      NC1244.2
174800        ELSE PERFORM FAIL                                         NC1244.2
174900        MOVE EDIT-AREA-13 TO COMPUTED-A                           NC1244.2
175000        MOVE "*000" TO CORRECT-A                                  NC1244.2
175100        PERFORM PRINT-DETAIL.                                     NC1244.2
175200     ADD 1 TO REC-CT.                                             NC1244.2
175300 PICTURE-TEST-21-B.                                               NC1244.2
175400     IF EDIT-AREA-14 EQUAL TO "**00"                              NC1244.2
175500        PERFORM PASS                                              NC1244.2
175600        PERFORM PRINT-DETAIL                                      NC1244.2
175700        ELSE PERFORM FAIL                                         NC1244.2
175800        MOVE EDIT-AREA-14 TO COMPUTED-A                           NC1244.2
175900        MOVE "**00" TO CORRECT-A                                  NC1244.2
176000        PERFORM PRINT-DETAIL.                                     NC1244.2
176100     ADD 1 TO REC-CT.                                             NC1244.2
176200 PICTURE-TEST-21-C.                                               NC1244.2
176300     IF EDIT-AREA-15 EQUAL TO "***0"                              NC1244.2
176400        PERFORM PASS                                              NC1244.2
176500        PERFORM PRINT-DETAIL                                      NC1244.2
176600        ELSE PERFORM FAIL                                         NC1244.2
176700        MOVE EDIT-AREA-15 TO COMPUTED-A                           NC1244.2
176800        MOVE "***0" TO CORRECT-A                                  NC1244.2
176900        PERFORM PRINT-DETAIL.                                     NC1244.2
177000     ADD 1 TO REC-CT.                                             NC1244.2
177100 PICTURE-TEST-21-D.                                               NC1244.2
177200     IF EDIT-AREA-16 EQUAL TO "**.01"                             NC1244.2
177300        PERFORM PASS                                              NC1244.2
177400        PERFORM PRINT-DETAIL                                      NC1244.2
177500        ELSE PERFORM FAIL                                         NC1244.2
177600        MOVE EDIT-AREA-16 TO COMPUTED-A                           NC1244.2
177700        MOVE "**.01" TO CORRECT-A                                 NC1244.2
177800        PERFORM PRINT-DETAIL.                                     NC1244.2
177900     ADD 1 TO REC-CT.                                             NC1244.2
178000 PICTURE-TEST-21-E.                                               NC1244.2
178100     IF EDIT-AREA-17 EQUAL TO "*****.01"                          NC1244.2
178200        PERFORM PASS                                              NC1244.2
178300        PERFORM PRINT-DETAIL                                      NC1244.2
178400        ELSE PERFORM FAIL                                         NC1244.2
178500        MOVE EDIT-AREA-17 TO COMPUTED-A                           NC1244.2
178600        MOVE "*****.01" TO CORRECT-A                              NC1244.2
178700        PERFORM PRINT-DETAIL.                                     NC1244.2
178800 PICTURE-INIT-22.                                                 NC1244.2
178900     MOVE "VI-31"           TO ANSI-REFERENCE.                    NC1244.2
179000     MOVE "PCTRE-TST-22" TO PAR-NAME.                             NC1244.2
179100     MOVE "PICTURE CHARACTER Z" TO FEATURE.                       NC1244.2
179200     MOVE  0      TO WORK-AREA-22.                                NC1244.2
179300     MOVE 1 TO REC-CT.                                            NC1244.2
179400 PICTURE-TEST-22.                                                 NC1244.2
179500     MOVE WORK-AREA-22 TO EDIT-AREA-18  EDIT-AREA-19  EDIT-AREA-20NC1244.2
179600                          EDIT-AREA-21  EDIT-AREA-22  EDIT-AREA-23NC1244.2
179700                          EDIT-AREA-24.                           NC1244.2
179800     GO TO PICTURE-TEST-22-A.                                     NC1244.2
179900 PICTURE-DELETE-22.                                               NC1244.2
180000     PERFORM DE-LETE.                                             NC1244.2
180100     PERFORM PRINT-DETAIL.                                        NC1244.2
180200     GO TO PICTURE-INIT-23.                                       NC1244.2
180300 PICTURE-TEST-22-A.                                               NC1244.2
180400     IF EDIT-AREA-18 EQUAL TO "0000"                              NC1244.2
180500        PERFORM PASS                                              NC1244.2
180600        PERFORM PRINT-DETAIL                                      NC1244.2
180700        ELSE PERFORM FAIL                                         NC1244.2
180800        MOVE EDIT-AREA-18 TO COMPUTED-A                           NC1244.2
180900        MOVE "0000" TO CORRECT-A                                  NC1244.2
181000        PERFORM PRINT-DETAIL.                                     NC1244.2
181100     ADD 1 TO REC-CT.                                             NC1244.2
181200 PICTURE-TEST-22-B.                                               NC1244.2
181300     IF EDIT-AREA-19 EQUAL TO " 000"                              NC1244.2
181400        PERFORM PASS                                              NC1244.2
181500        PERFORM PRINT-DETAIL                                      NC1244.2
181600        ELSE PERFORM FAIL                                         NC1244.2
181700        MOVE EDIT-AREA-19 TO COMPUTED-A                           NC1244.2
181800        MOVE " 000" TO CORRECT-A                                  NC1244.2
181900        PERFORM PRINT-DETAIL.                                     NC1244.2
182000     ADD 1 TO REC-CT.                                             NC1244.2
182100 PICTURE-TEST-22-C.                                               NC1244.2
182200     IF EDIT-AREA-20 EQUAL TO "  00"                              NC1244.2
182300        PERFORM PASS                                              NC1244.2
182400        PERFORM PRINT-DETAIL                                      NC1244.2
182500        ELSE PERFORM FAIL                                         NC1244.2
182600        MOVE EDIT-AREA-20 TO COMPUTED-A                           NC1244.2
182700        MOVE "  00" TO CORRECT-A                                  NC1244.2
182800        PERFORM PRINT-DETAIL.                                     NC1244.2
182900     ADD 1 TO REC-CT.                                             NC1244.2
183000 PICTURE-TEST-22-D.                                               NC1244.2
183100     IF EDIT-AREA-21 EQUAL TO "   0"                              NC1244.2
183200        PERFORM PASS                                              NC1244.2
183300        PERFORM PRINT-DETAIL                                      NC1244.2
183400        ELSE PERFORM FAIL                                         NC1244.2
183500        MOVE EDIT-AREA-21 TO COMPUTED-A                           NC1244.2
183600        MOVE "   0" TO CORRECT-A                                  NC1244.2
183700        PERFORM PRINT-DETAIL.                                     NC1244.2
183800     ADD 1 TO REC-CT.                                             NC1244.2
183900 PICTURE-TEST-22-E.                                               NC1244.2
184000     IF EDIT-AREA-22 EQUAL TO "    "                              NC1244.2
184100        PERFORM PASS                                              NC1244.2
184200        PERFORM PRINT-DETAIL                                      NC1244.2
184300        ELSE PERFORM FAIL                                         NC1244.2
184400        MOVE EDIT-AREA-22 TO COMPUTED-A                           NC1244.2
184500        MOVE "SPACES" TO CORRECT-A                                NC1244.2
184600        PERFORM PRINT-DETAIL.                                     NC1244.2
184700     ADD 1 TO REC-CT.                                             NC1244.2
184800 PICTURE-TEST-22-F.                                               NC1244.2
184900     IF EDIT-AREA-23 EQUAL TO "     "                             NC1244.2
185000        PERFORM PASS                                              NC1244.2
185100        PERFORM PRINT-DETAIL                                      NC1244.2
185200        ELSE PERFORM FAIL                                         NC1244.2
185300        MOVE EDIT-AREA-23 TO COMPUTED-A                           NC1244.2
185400        MOVE "SPACES" TO CORRECT-A                                NC1244.2
185500        PERFORM PRINT-DETAIL.                                     NC1244.2
185600     ADD 1 TO REC-CT.                                             NC1244.2
185700 PICTURE-TEST-22-G.                                               NC1244.2
185800     IF EDIT-AREA-24 EQUAL TO "     "                             NC1244.2
185900        PERFORM PASS                                              NC1244.2
186000        PERFORM PRINT-DETAIL                                      NC1244.2
186100        ELSE PERFORM FAIL                                         NC1244.2
186200        MOVE EDIT-AREA-24 TO COMPUTED-A                           NC1244.2
186300        MOVE "SPACES" TO CORRECT-A                                NC1244.2
186400        PERFORM PRINT-DETAIL.                                     NC1244.2
186500 PICTURE-INIT-23.                                                 NC1244.2
186600     MOVE "VI-31"           TO ANSI-REFERENCE.                    NC1244.2
186700     MOVE "PCTRE-TST-23" TO PAR-NAME.                             NC1244.2
186800     MOVE "PICTURE CHARACTER Z" TO FEATURE.                       NC1244.2
186900     MOVE  1.01   TO WORK-AREA-23.                                NC1244.2
187000     MOVE 1 TO REC-CT.                                            NC1244.2
187100 PICTURE-TEST-23.                                                 NC1244.2
187200     MOVE WORK-AREA-23 TO EDIT-AREA-18  EDIT-AREA-19  EDIT-AREA-20NC1244.2
187300                          EDIT-AREA-21  EDIT-AREA-22  EDIT-AREA-23NC1244.2
187400                          EDIT-AREA-24.                           NC1244.2
187500     GO TO PICTURE-TEST-23-A.                                     NC1244.2
187600 PICTURE-DELETE-23.                                               NC1244.2
187700     PERFORM DE-LETE.                                             NC1244.2
187800     PERFORM PRINT-DETAIL.                                        NC1244.2
187900     GO TO PICTURE-INIT-24.                                       NC1244.2
188000 PICTURE-TEST-23-A.                                               NC1244.2
188100     IF EDIT-AREA-18 EQUAL TO "0001"                              NC1244.2
188200        PERFORM PASS                                              NC1244.2
188300        PERFORM PRINT-DETAIL                                      NC1244.2
188400        ELSE PERFORM FAIL                                         NC1244.2
188500        MOVE EDIT-AREA-18 TO COMPUTED-A                           NC1244.2
188600        MOVE "0001" TO CORRECT-A                                  NC1244.2
188700        PERFORM PRINT-DETAIL.                                     NC1244.2
188800     ADD 1 TO REC-CT.                                             NC1244.2
188900 PICTURE-TEST-23-B.                                               NC1244.2
189000     IF EDIT-AREA-19 EQUAL TO " 001"                              NC1244.2
189100        PERFORM PASS                                              NC1244.2
189200        PERFORM PRINT-DETAIL                                      NC1244.2
189300        ELSE PERFORM FAIL                                         NC1244.2
189400        MOVE EDIT-AREA-19 TO COMPUTED-A                           NC1244.2
189500        MOVE " 001" TO CORRECT-A                                  NC1244.2
189600        PERFORM PRINT-DETAIL.                                     NC1244.2
189700     ADD 1 TO REC-CT.                                             NC1244.2
189800 PICTURE-TEST-23-C.                                               NC1244.2
189900     IF EDIT-AREA-20 EQUAL TO "  01"                              NC1244.2
190000        PERFORM PASS                                              NC1244.2
190100        PERFORM PRINT-DETAIL                                      NC1244.2
190200        ELSE PERFORM FAIL                                         NC1244.2
190300        MOVE EDIT-AREA-20 TO COMPUTED-A                           NC1244.2
190400        MOVE "  01" TO CORRECT-A                                  NC1244.2
190500        PERFORM PRINT-DETAIL.                                     NC1244.2
190600     ADD 1 TO REC-CT.                                             NC1244.2
190700 PICTURE-TEST-23-D.                                               NC1244.2
190800     IF EDIT-AREA-21 EQUAL TO "   1"                              NC1244.2
190900        PERFORM PASS                                              NC1244.2
191000        PERFORM PRINT-DETAIL                                      NC1244.2
191100        ELSE PERFORM FAIL                                         NC1244.2
191200        MOVE EDIT-AREA-21 TO COMPUTED-A                           NC1244.2
191300        MOVE "   1" TO CORRECT-A                                  NC1244.2
191400        PERFORM PRINT-DETAIL.                                     NC1244.2
191500     ADD 1 TO REC-CT.                                             NC1244.2
191600 PICTURE-TEST-23-E.                                               NC1244.2
191700     IF EDIT-AREA-22 EQUAL TO "   1"                              NC1244.2
191800        PERFORM PASS                                              NC1244.2
191900        PERFORM PRINT-DETAIL                                      NC1244.2
192000        ELSE PERFORM FAIL                                         NC1244.2
192100        MOVE EDIT-AREA-22 TO COMPUTED-A                           NC1244.2
192200        MOVE "   1" TO CORRECT-A                                  NC1244.2
192300        PERFORM PRINT-DETAIL.                                     NC1244.2
192400     ADD 1 TO REC-CT.                                             NC1244.2
192500 PICTURE-TEST-23-F.                                               NC1244.2
192600     IF EDIT-AREA-23 EQUAL TO " 1.01"                             NC1244.2
192700        PERFORM PASS                                              NC1244.2
192800        PERFORM PRINT-DETAIL                                      NC1244.2
192900        ELSE PERFORM FAIL                                         NC1244.2
193000        MOVE EDIT-AREA-23 TO COMPUTED-A                           NC1244.2
193100        MOVE " 1.01" TO CORRECT-A                                 NC1244.2
193200        PERFORM PRINT-DETAIL.                                     NC1244.2
193300     ADD 1 TO REC-CT.                                             NC1244.2
193400 PICTURE-TEST-23-G.                                               NC1244.2
193500     IF EDIT-AREA-24 EQUAL TO "    1"                             NC1244.2
193600        PERFORM PASS                                              NC1244.2
193700        PERFORM PRINT-DETAIL                                      NC1244.2
193800        ELSE PERFORM FAIL                                         NC1244.2
193900        MOVE EDIT-AREA-24 TO COMPUTED-A                           NC1244.2
194000        MOVE "    1" TO CORRECT-A                                 NC1244.2
194100        PERFORM PRINT-DETAIL.                                     NC1244.2
194200 PICTURE-INIT-24.                                                 NC1244.2
194300     MOVE "VI-31"           TO ANSI-REFERENCE.                    NC1244.2
194400     MOVE "PCTRE-TST-24" TO PAR-NAME.                             NC1244.2
194500     MOVE "PICTURE CHARACTER Z" TO FEATURE.                       NC1244.2
194600     MOVE  217    TO WORK-AREA-24.                                NC1244.2
194700     MOVE 1 TO REC-CT.                                            NC1244.2
194800 PICTURE-TEST-24.                                                 NC1244.2
194900     MOVE WORK-AREA-24 TO EDIT-AREA-18  EDIT-AREA-19  EDIT-AREA-20NC1244.2
195000                          EDIT-AREA-21  EDIT-AREA-22  EDIT-AREA-23NC1244.2
195100                          EDIT-AREA-24.                           NC1244.2
195200     GO TO PICTURE-TEST-24-A.                                     NC1244.2
195300 PICTURE-DELETE-24.                                               NC1244.2
195400     PERFORM DE-LETE.                                             NC1244.2
195500     PERFORM PRINT-DETAIL.                                        NC1244.2
195600     GO TO PICTURE-INIT-25.                                       NC1244.2
195700 PICTURE-TEST-24-A.                                               NC1244.2
195800     IF EDIT-AREA-18 EQUAL TO "0217"                              NC1244.2
195900        PERFORM PASS                                              NC1244.2
196000        PERFORM PRINT-DETAIL                                      NC1244.2
196100        ELSE PERFORM FAIL                                         NC1244.2
196200        MOVE EDIT-AREA-18 TO COMPUTED-A                           NC1244.2
196300        MOVE "0217" TO CORRECT-A                                  NC1244.2
196400        PERFORM PRINT-DETAIL.                                     NC1244.2
196500     ADD 1 TO REC-CT.                                             NC1244.2
196600 PICTURE-TEST-24-B.                                               NC1244.2
196700     IF EDIT-AREA-19 EQUAL TO " 217"                              NC1244.2
196800        PERFORM PASS                                              NC1244.2
196900        PERFORM PRINT-DETAIL                                      NC1244.2
197000        ELSE PERFORM FAIL                                         NC1244.2
197100        MOVE EDIT-AREA-19 TO COMPUTED-A                           NC1244.2
197200        MOVE " 217" TO CORRECT-A                                  NC1244.2
197300        PERFORM PRINT-DETAIL.                                     NC1244.2
197400     ADD 1 TO REC-CT.                                             NC1244.2
197500 PICTURE-TEST-24-C.                                               NC1244.2
197600     IF EDIT-AREA-20 EQUAL TO " 217"                              NC1244.2
197700        PERFORM PASS                                              NC1244.2
197800        PERFORM PRINT-DETAIL                                      NC1244.2
197900        ELSE PERFORM FAIL                                         NC1244.2
198000        MOVE EDIT-AREA-20 TO COMPUTED-A                           NC1244.2
198100        MOVE " 217" TO CORRECT-A                                  NC1244.2
198200        PERFORM PRINT-DETAIL.                                     NC1244.2
198300     ADD 1 TO REC-CT.                                             NC1244.2
198400 PICTURE-TEST-24-D.                                               NC1244.2
198500     IF EDIT-AREA-21 EQUAL TO " 217"                              NC1244.2
198600        PERFORM PASS                                              NC1244.2
198700        PERFORM PRINT-DETAIL                                      NC1244.2
198800        ELSE PERFORM FAIL                                         NC1244.2
198900        MOVE EDIT-AREA-21 TO COMPUTED-A                           NC1244.2
199000        MOVE " 217" TO CORRECT-A                                  NC1244.2
199100        PERFORM PRINT-DETAIL.                                     NC1244.2
199200     ADD 1 TO REC-CT.                                             NC1244.2
199300 PICTURE-TEST-24-E.                                               NC1244.2
199400     IF EDIT-AREA-22 EQUAL TO " 217"                              NC1244.2
199500        PERFORM PASS                                              NC1244.2
199600        PERFORM PRINT-DETAIL                                      NC1244.2
199700        ELSE PERFORM FAIL                                         NC1244.2
199800        MOVE EDIT-AREA-22 TO COMPUTED-A                           NC1244.2
199900        MOVE " 217" TO CORRECT-A                                  NC1244.2
200000        PERFORM PRINT-DETAIL.                                     NC1244.2
200100     ADD 1 TO REC-CT.                                             NC1244.2
200200 PICTURE-TEST-24-F.                                               NC1244.2
200300     IF EDIT-AREA-23 EQUAL TO "17.00"                             NC1244.2
200400        PERFORM PASS                                              NC1244.2
200500        PERFORM PRINT-DETAIL                                      NC1244.2
200600        ELSE PERFORM FAIL                                         NC1244.2
200700        MOVE EDIT-AREA-23 TO COMPUTED-A                           NC1244.2
200800        MOVE "17.00" TO CORRECT-A                                 NC1244.2
200900        PERFORM PRINT-DETAIL.                                     NC1244.2
201000     ADD 1 TO REC-CT.                                             NC1244.2
201100 PICTURE-TEST-24-G.                                               NC1244.2
201200     IF EDIT-AREA-24 EQUAL TO "  217"                             NC1244.2
201300        PERFORM PASS                                              NC1244.2
201400        PERFORM PRINT-DETAIL                                      NC1244.2
201500        ELSE PERFORM FAIL                                         NC1244.2
201600        MOVE EDIT-AREA-24 TO COMPUTED-A                           NC1244.2
201700        MOVE "  217" TO CORRECT-A                                 NC1244.2
201800        PERFORM PRINT-DETAIL.                                     NC1244.2
201900 PICTURE-INIT-25.                                                 NC1244.2
202000     MOVE "VI-31"           TO ANSI-REFERENCE.                    NC1244.2
202100     MOVE "PCTRE-TST-25" TO PAR-NAME.                             NC1244.2
202200     MOVE "PICTURE CHARACTER Z" TO FEATURE.                       NC1244.2
202300     MOVE  1010.20 TO WORK-AREA-25.                               NC1244.2
202400     MOVE 1 TO REC-CT.                                            NC1244.2
202500 PICTURE-TEST-25.                                                 NC1244.2
202600     MOVE WORK-AREA-25 TO EDIT-AREA-18  EDIT-AREA-19  EDIT-AREA-20NC1244.2
202700                          EDIT-AREA-21  EDIT-AREA-22  EDIT-AREA-23NC1244.2
202800                          EDIT-AREA-24.                           NC1244.2
202900     GO TO PICTURE-TEST-25-A.                                     NC1244.2
203000 PICTURE-DELETE-25.                                               NC1244.2
203100     PERFORM DE-LETE.                                             NC1244.2
203200     PERFORM PRINT-DETAIL.                                        NC1244.2
203300     GO TO PICTURE-INIT-26.                                       NC1244.2
203400 PICTURE-TEST-25-A.                                               NC1244.2
203500     IF EDIT-AREA-18 EQUAL TO "1010"                              NC1244.2
203600        PERFORM PASS                                              NC1244.2
203700        PERFORM PRINT-DETAIL                                      NC1244.2
203800        ELSE PERFORM FAIL                                         NC1244.2
203900        MOVE EDIT-AREA-18 TO COMPUTED-A                           NC1244.2
204000        MOVE "1010" TO CORRECT-A                                  NC1244.2
204100        PERFORM PRINT-DETAIL.                                     NC1244.2
204200     ADD 1 TO REC-CT.                                             NC1244.2
204300 PICTURE-TEST-25-B.                                               NC1244.2
204400     IF EDIT-AREA-19 EQUAL TO "1010"                              NC1244.2
204500        PERFORM PASS                                              NC1244.2
204600        PERFORM PRINT-DETAIL                                      NC1244.2
204700        ELSE PERFORM FAIL                                         NC1244.2
204800        MOVE EDIT-AREA-19 TO COMPUTED-A                           NC1244.2
204900        MOVE "1010" TO CORRECT-A                                  NC1244.2
205000        PERFORM PRINT-DETAIL.                                     NC1244.2
205100     ADD 1 TO REC-CT.                                             NC1244.2
205200 PICTURE-TEST-25-C.                                               NC1244.2
205300     IF EDIT-AREA-20 EQUAL TO "1010"                              NC1244.2
205400        PERFORM PASS                                              NC1244.2
205500        PERFORM PRINT-DETAIL                                      NC1244.2
205600        ELSE PERFORM FAIL                                         NC1244.2
205700        MOVE EDIT-AREA-20 TO COMPUTED-A                           NC1244.2
205800        MOVE "1010" TO CORRECT-A                                  NC1244.2
205900        PERFORM PRINT-DETAIL.                                     NC1244.2
206000     ADD 1 TO REC-CT.                                             NC1244.2
206100 PICTURE-TEST-25-D.                                               NC1244.2
206200     IF EDIT-AREA-21 EQUAL TO "1010"                              NC1244.2
206300        PERFORM PASS                                              NC1244.2
206400        PERFORM PRINT-DETAIL                                      NC1244.2
206500        ELSE PERFORM FAIL                                         NC1244.2
206600        MOVE EDIT-AREA-21 TO COMPUTED-A                           NC1244.2
206700        MOVE "1010" TO CORRECT-A                                  NC1244.2
206800        PERFORM PRINT-DETAIL.                                     NC1244.2
206900     ADD 1 TO REC-CT.                                             NC1244.2
207000 PICTURE-TEST-25-E.                                               NC1244.2
207100     IF EDIT-AREA-22 EQUAL TO "1010"                              NC1244.2
207200        PERFORM PASS                                              NC1244.2
207300        PERFORM PRINT-DETAIL                                      NC1244.2
207400        ELSE PERFORM FAIL                                         NC1244.2
207500        MOVE EDIT-AREA-22 TO COMPUTED-A                           NC1244.2
207600        MOVE "1010" TO CORRECT-A                                  NC1244.2
207700        PERFORM PRINT-DETAIL.                                     NC1244.2
207800     ADD 1 TO REC-CT.                                             NC1244.2
207900 PICTURE-TEST-25-F.                                               NC1244.2
208000     IF EDIT-AREA-23 EQUAL TO "10.20"                             NC1244.2
208100        PERFORM PASS                                              NC1244.2
208200        PERFORM PRINT-DETAIL                                      NC1244.2
208300        ELSE PERFORM FAIL                                         NC1244.2
208400        MOVE EDIT-AREA-23 TO COMPUTED-A                           NC1244.2
208500        MOVE "10.20" TO CORRECT-A                                 NC1244.2
208600        PERFORM PRINT-DETAIL.                                     NC1244.2
208700     ADD 1 TO REC-CT.                                             NC1244.2
208800 PICTURE-TEST-25-G.                                               NC1244.2
208900     IF EDIT-AREA-24 EQUAL TO "1,010"                             NC1244.2
209000        PERFORM PASS                                              NC1244.2
209100        PERFORM PRINT-DETAIL                                      NC1244.2
209200        ELSE PERFORM FAIL                                         NC1244.2
209300        MOVE EDIT-AREA-24 TO COMPUTED-A                           NC1244.2
209400        MOVE "1,010" TO CORRECT-A                                 NC1244.2
209500        PERFORM PRINT-DETAIL.                                     NC1244.2
209600 PICTURE-INIT-26.                                                 NC1244.2
209700     MOVE "VI-31"           TO ANSI-REFERENCE.                    NC1244.2
209800     MOVE "PCTRE-TST-26" TO PAR-NAME.                             NC1244.2
209900     MOVE "PICTURE CHARACTER Z" TO FEATURE.                       NC1244.2
210000     MOVE  .01     TO WORK-AREA-26.                               NC1244.2
210100     MOVE 1 TO REC-CT.                                            NC1244.2
210200 PICTURE-TEST-26.                                                 NC1244.2
210300     MOVE WORK-AREA-26 TO EDIT-AREA-18  EDIT-AREA-19  EDIT-AREA-20NC1244.2
210400                          EDIT-AREA-21  EDIT-AREA-22  EDIT-AREA-23NC1244.2
210500                          EDIT-AREA-24.                           NC1244.2
210600     GO TO PICTURE-TEST-26-A.                                     NC1244.2
210700 PICTURE-DELETE-26.                                               NC1244.2
210800     PERFORM DE-LETE.                                             NC1244.2
210900     PERFORM PRINT-DETAIL.                                        NC1244.2
211000     GO TO PICTURE-INIT-27.                                       NC1244.2
211100 PICTURE-TEST-26-A.                                               NC1244.2
211200     IF EDIT-AREA-18 EQUAL TO "0000"                              NC1244.2
211300        PERFORM PASS                                              NC1244.2
211400        PERFORM PRINT-DETAIL                                      NC1244.2
211500        ELSE PERFORM FAIL                                         NC1244.2
211600        MOVE EDIT-AREA-18 TO COMPUTED-A                           NC1244.2
211700        MOVE "0000" TO CORRECT-A                                  NC1244.2
211800        PERFORM PRINT-DETAIL.                                     NC1244.2
211900     ADD 1 TO REC-CT.                                             NC1244.2
212000 PICTURE-TEST-26-B.                                               NC1244.2
212100     IF EDIT-AREA-19 EQUAL TO " 000"                              NC1244.2
212200        PERFORM PASS                                              NC1244.2
212300        PERFORM PRINT-DETAIL                                      NC1244.2
212400        ELSE PERFORM FAIL                                         NC1244.2
212500        MOVE EDIT-AREA-19 TO COMPUTED-A                           NC1244.2
212600        MOVE " 000" TO CORRECT-A                                  NC1244.2
212700        PERFORM PRINT-DETAIL.                                     NC1244.2
212800     ADD 1 TO REC-CT.                                             NC1244.2
212900 PICTURE-TEST-26-C.                                               NC1244.2
213000     IF EDIT-AREA-20 EQUAL TO "  00"                              NC1244.2
213100        PERFORM PASS                                              NC1244.2
213200        PERFORM PRINT-DETAIL                                      NC1244.2
213300        ELSE PERFORM FAIL                                         NC1244.2
213400        MOVE EDIT-AREA-20 TO COMPUTED-A                           NC1244.2
213500        MOVE "  00" TO CORRECT-A                                  NC1244.2
213600        PERFORM PRINT-DETAIL.                                     NC1244.2
213700     ADD 1 TO REC-CT.                                             NC1244.2
213800     IF EDIT-AREA-21 EQUAL TO "   0"                              NC1244.2
213900        PERFORM PASS                                              NC1244.2
214000        PERFORM PRINT-DETAIL                                      NC1244.2
214100        ELSE PERFORM FAIL                                         NC1244.2
214200        MOVE EDIT-AREA-21 TO COMPUTED-A                           NC1244.2
214300        MOVE "   0" TO CORRECT-A                                  NC1244.2
214400        PERFORM PRINT-DETAIL.                                     NC1244.2
214500     ADD 1 TO REC-CT.                                             NC1244.2
214600 PICTURE-TEST-26-E.                                               NC1244.2
214700     IF EDIT-AREA-22 EQUAL TO "    "                              NC1244.2
214800        PERFORM PASS                                              NC1244.2
214900        PERFORM PRINT-DETAIL                                      NC1244.2
215000        ELSE PERFORM FAIL                                         NC1244.2
215100        MOVE EDIT-AREA-22 TO COMPUTED-A                           NC1244.2
215200        MOVE "SPACES" TO CORRECT-A                                NC1244.2
215300        PERFORM PRINT-DETAIL.                                     NC1244.2
215400     ADD 1 TO REC-CT.                                             NC1244.2
215500 PICTURE-TEST-26-F.                                               NC1244.2
215600     IF EDIT-AREA-23 EQUAL TO "  .01"                             NC1244.2
215700        PERFORM PASS                                              NC1244.2
215800        PERFORM PRINT-DETAIL                                      NC1244.2
215900        ELSE PERFORM FAIL                                         NC1244.2
216000        MOVE EDIT-AREA-23 TO COMPUTED-A                           NC1244.2
216100        MOVE "  .01" TO CORRECT-A                                 NC1244.2
216200        PERFORM PRINT-DETAIL.                                     NC1244.2
216300     ADD 1 TO REC-CT.                                             NC1244.2
216400 PICTURE-TEST-26-G.                                               NC1244.2
216500     IF EDIT-AREA-24 EQUAL TO "     "                             NC1244.2
216600        PERFORM PASS                                              NC1244.2
216700        PERFORM PRINT-DETAIL                                      NC1244.2
216800        ELSE PERFORM FAIL                                         NC1244.2
216900        MOVE EDIT-AREA-24 TO COMPUTED-A                           NC1244.2
217000        MOVE "SPACES" TO CORRECT-A                                NC1244.2
217100        PERFORM PRINT-DETAIL.                                     NC1244.2
217200 PICTURE-INIT-27.                                                 NC1244.2
217300     MOVE "VI-31"           TO ANSI-REFERENCE.                    NC1244.2
217400     MOVE ZERO TO REC-CT.                                         NC1244.2
217500     MOVE  200     TO WORK-AREA-27.                               NC1244.2
217600     MOVE  SPACE   TO WORK-AREA-27A.                              NC1244.2
217700     MOVE "PICTURE CHARACTER P" TO FEATURE.                       NC1244.2
217800 PICTURE-TEST-27-0.                                               NC1244.2
217900     MOVE WORK-AREA-27 TO WORK-AREA-27A.                          NC1244.2
218000 PICTURE-TEST-27-1.                                               NC1244.2
218100     IF WORK-AREA-27A EQUAL TO "200"                              NC1244.2
218200        PERFORM PASS                                              NC1244.2
218300        ELSE GO TO PICTURE-FAIL-27.                               NC1244.2
218400     GO TO PICTURE-WRITE-27.                                      NC1244.2
218500 PICTURE-DELETE-27.                                               NC1244.2
218600     PERFORM DE-LETE.                                             NC1244.2
218700     GO TO PICTURE-WRITE-27.                                      NC1244.2
218800 PICTURE-FAIL-27.                                                 NC1244.2
218900     PERFORM FAIL.                                                NC1244.2
219000     MOVE WORK-AREA-27A TO COMPUTED-A.                            NC1244.2
219100     MOVE "200" TO CORRECT-A.                                     NC1244.2
219200 PICTURE-WRITE-27.                                                NC1244.2
219300     MOVE "PICTURE-TEST-27" TO PAR-NAME.                          NC1244.2
219400     PERFORM PRINT-DETAIL.                                        NC1244.2
219500 PICTURE-INIT-28.                                                 NC1244.2
219600     MOVE "VI-31"           TO ANSI-REFERENCE.                    NC1244.2
219700     MOVE "PICTURE CHARACTER P" TO FEATURE.                       NC1244.2
219800     MOVE  567     TO WORK-AREA-28.                               NC1244.2
219900     MOVE  ZERO    TO WORK-AREA-28A.                              NC1244.2
220000 PICTURE-TEST-28-0.                                               NC1244.2
220100     MOVE WORK-AREA-28 TO WORK-AREA-28A.                          NC1244.2
220200 PICTURE-TEST-28-1.                                               NC1244.2
220300     IF WORK-AREA-28A EQUAL TO 500                                NC1244.2
220400        PERFORM PASS                                              NC1244.2
220500        ELSE GO TO PICTURE-FAIL-28.                               NC1244.2
220600     GO TO PICTURE-WRITE-28.                                      NC1244.2
220700 PICTURE-DELETE-28.                                               NC1244.2
220800     PERFORM DE-LETE.                                             NC1244.2
220900     GO TO PICTURE-WRITE-28.                                      NC1244.2
221000 PICTURE-FAIL-28.                                                 NC1244.2
221100     PERFORM FAIL.                                                NC1244.2
221200     MOVE WORK-AREA-28A TO COMPUTED-A.                            NC1244.2
221300     MOVE 500 TO CORRECT-18V0.                                    NC1244.2
221400 PICTURE-WRITE-28.                                                NC1244.2
221500     MOVE "PICTURE-TEST-28" TO PAR-NAME.                          NC1244.2
221600     PERFORM PRINT-DETAIL.                                        NC1244.2
221700 PICTURE-INIT-29.                                                 NC1244.2
221800     MOVE "VI-31"           TO ANSI-REFERENCE.                    NC1244.2
221900     MOVE "PICTURE CHARACTER P" TO FEATURE.                       NC1244.2
222000     MOVE  123     TO WORK-AREA-29.                               NC1244.2
222100     MOVE  ZERO    TO WORK-AREA-29A.                              NC1244.2
222200     MOVE  SPACE   TO WORK-AREA-29B.                              NC1244.2
222300 PICTURE-TEST-29.                                                 NC1244.2
222400     MOVE WORK-AREA-29 TO WORK-AREA-29A.                          NC1244.2
222500     MOVE WORK-AREA-29A TO WORK-AREA-29B.                         NC1244.2
222600     IF WORK-AREA-29B EQUAL TO "100"                              NC1244.2
222700        PERFORM PASS                                              NC1244.2
222800        ELSE GO TO PICTURE-FAIL-29.                               NC1244.2
222900     GO TO PICTURE-WRITE-29.                                      NC1244.2
223000 PICTURE-DELETE-29.                                               NC1244.2
223100     PERFORM DE-LETE.                                             NC1244.2
223200     GO TO PICTURE-WRITE-29.                                      NC1244.2
223300 PICTURE-FAIL-29.                                                 NC1244.2
223400     PERFORM FAIL.                                                NC1244.2
223500     MOVE WORK-AREA-29B TO COMPUTED-A.                            NC1244.2
223600     MOVE "100" TO CORRECT-A.                                     NC1244.2
223700 PICTURE-WRITE-29.                                                NC1244.2
223800     MOVE "PICTURE-TEST-29" TO PAR-NAME.                          NC1244.2
223900     PERFORM PRINT-DETAIL.                                        NC1244.2
224000 PICTURE-INIT-30.                                                 NC1244.2
224100     MOVE "VI-31"           TO ANSI-REFERENCE.                    NC1244.2
224200     MOVE "PICTURE CHARACTER P" TO FEATURE.                       NC1244.2
224300     MOVE  00900   TO WORK-AREA-30.                               NC1244.2
224400     MOVE  ZERO    TO WORK-AREA-30A.                              NC1244.2
224500 PICTURE-TEST-30-0.                                               NC1244.2
224600     MOVE WORK-AREA-30 TO WORK-AREA-30A.                          NC1244.2
224700 PICTURE-TEST-30-1.                                               NC1244.2
224800     IF WORK-AREA-30A EQUAL TO "  9"                              NC1244.2
224900        PERFORM PASS                                              NC1244.2
225000        ELSE GO TO PICTURE-FAIL-30.                               NC1244.2
225100     GO TO PICTURE-WRITE-30.                                      NC1244.2
225200 PICTURE-DELETE-30.                                               NC1244.2
225300     PERFORM DE-LETE.                                             NC1244.2
225400     GO TO PICTURE-WRITE-30.                                      NC1244.2
225500 PICTURE-FAIL-30.                                                 NC1244.2
225600     PERFORM FAIL.                                                NC1244.2
225700     MOVE WORK-AREA-30A TO COMPUTED-A.                            NC1244.2
225800     MOVE "  9" TO CORRECT-A.                                     NC1244.2
225900 PICTURE-WRITE-30.                                                NC1244.2
226000     MOVE "PICTURE-TEST-30" TO PAR-NAME.                          NC1244.2
226100     PERFORM PRINT-DETAIL.                                        NC1244.2
226200 PICTURE-INIT-31.                                                 NC1244.2
226300     MOVE "VI-31"           TO ANSI-REFERENCE.                    NC1244.2
226400     MOVE "PICTURE CHARACTER P" TO FEATURE.                       NC1244.2
226500     MOVE  01200   TO WORK-AREA-31.                               NC1244.2
226600     MOVE  ZERO    TO WORK-AREA-31A.                              NC1244.2
226700     MOVE  SPACE   TO WORK-AREA-31B.                              NC1244.2
226800 PICTURE-TEST-31.                                                 NC1244.2
226900     MOVE WORK-AREA-31 TO WORK-AREA-31A.                          NC1244.2
227000     MOVE WORK-AREA-31A TO WORK-AREA-31B.                         NC1244.2
227100     IF WORK-AREA-31B EQUAL TO " 12  "                            NC1244.2
227200        PERFORM PASS                                              NC1244.2
227300        ELSE GO TO PICTURE-FAIL-31.                               NC1244.2
227400     GO TO PICTURE-WRITE-31.                                      NC1244.2
227500 PICTURE-DELETE-31.                                               NC1244.2
227600     PERFORM DE-LETE.                                             NC1244.2
227700     GO TO PICTURE-WRITE-31.                                      NC1244.2
227800 PICTURE-FAIL-31.                                                 NC1244.2
227900     PERFORM FAIL.                                                NC1244.2
228000     MOVE WORK-AREA-31B TO COMPUTED-A.                            NC1244.2
228100     MOVE " 12  " TO CORRECT-A.                                   NC1244.2
228200 PICTURE-WRITE-31.                                                NC1244.2
228300     MOVE "PICTURE-TEST-31" TO PAR-NAME.                          NC1244.2
228400     PERFORM PRINT-DETAIL.                                        NC1244.2
228500 PICTURE-INIT-32.                                                 NC1244.2
228600     MOVE "VI-31"           TO ANSI-REFERENCE.                    NC1244.2
228700     MOVE "PICTURE CHARACTER P" TO FEATURE.                       NC1244.2
228800     MOVE  .001    TO WORK-AREA-32.                               NC1244.2
228900     MOVE  ZERO    TO WORK-AREA-32A.                              NC1244.2
229000 PICTURE-TEST-32-0.                                               NC1244.2
229100     MOVE WORK-AREA-32 TO WORK-AREA-32A.                          NC1244.2
229200 PICTURE-TEST-32-1.                                               NC1244.2
229300     IF WORK-AREA-32A EQUAL TO .001                               NC1244.2
229400        PERFORM PASS                                              NC1244.2
229500        ELSE GO TO PICTURE-FAIL-32.                               NC1244.2
229600     GO TO PICTURE-WRITE-32.                                      NC1244.2
229700 PICTURE-DELETE-32.                                               NC1244.2
229800     PERFORM DE-LETE.                                             NC1244.2
229900     GO TO PICTURE-WRITE-32.                                      NC1244.2
230000 PICTURE-FAIL-32.                                                 NC1244.2
230100     PERFORM FAIL.                                                NC1244.2
230200     MOVE WORK-AREA-32A TO COMPUTED-0V18.                         NC1244.2
230300     MOVE .001 TO CORRECT-0V18.                                   NC1244.2
230400 PICTURE-WRITE-32.                                                NC1244.2
230500     MOVE "PICTURE-TEST-32" TO PAR-NAME.                          NC1244.2
230600     PERFORM PRINT-DETAIL.                                        NC1244.2
230700 PICTURE-INIT-33.                                                 NC1244.2
230800     MOVE "VI-31"           TO ANSI-REFERENCE.                    NC1244.2
230900     MOVE "PICTURE CHARACTER P" TO FEATURE.                       NC1244.2
231000     MOVE  .567    TO WORK-AREA-33.                               NC1244.2
231100     MOVE  ZERO    TO WORK-AREA-33A.                              NC1244.2
231200 PICTURE-TEST-33-0.                                               NC1244.2
231300     MOVE WORK-AREA-33 TO WORK-AREA-33A.                          NC1244.2
231400 PICTURE-TEST-33-1.                                               NC1244.2
231500     IF WORK-AREA-33A EQUAL TO .007                               NC1244.2
231600        PERFORM PASS                                              NC1244.2
231700        ELSE GO TO PICTURE-FAIL-33.                               NC1244.2
231800     GO TO PICTURE-WRITE-33.                                      NC1244.2
231900 PICTURE-DELETE-33.                                               NC1244.2
232000     PERFORM DE-LETE.                                             NC1244.2
232100     GO TO PICTURE-WRITE-33.                                      NC1244.2
232200 PICTURE-FAIL-33.                                                 NC1244.2
232300     PERFORM FAIL.                                                NC1244.2
232400     MOVE WORK-AREA-33A TO COMPUTED-0V18.                         NC1244.2
232500     MOVE .007 TO CORRECT-0V18.                                   NC1244.2
232600 PICTURE-WRITE-33.                                                NC1244.2
232700     MOVE "PICTURE-TEST-33" TO PAR-NAME.                          NC1244.2
232800     PERFORM PRINT-DETAIL.                                        NC1244.2
232900 PICTURE-INIT-34.                                                 NC1244.2
233000     MOVE "VI-31"           TO ANSI-REFERENCE.                    NC1244.2
233100     MOVE "PICTURE CHARACTER P" TO FEATURE.                       NC1244.2
233200     MOVE  .123    TO WORK-AREA-34.                               NC1244.2
233300     MOVE  ZERO    TO WORK-AREA-34A.                              NC1244.2
233400     MOVE  ZERO    TO WORK-AREA-34B.                              NC1244.2
233500 PICTURE-TEST-34.                                                 NC1244.2
233600     MOVE WORK-AREA-34 TO WORK-AREA-34A.                          NC1244.2
233700     MOVE WORK-AREA-34A TO WORK-AREA-34B.                         NC1244.2
233800     IF WORK-AREA-34B EQUAL TO .003                               NC1244.2
233900        PERFORM PASS                                              NC1244.2
234000        ELSE GO TO PICTURE-FAIL-34.                               NC1244.2
234100     GO TO PICTURE-WRITE-34.                                      NC1244.2
234200 PICTURE-DELETE-34.                                               NC1244.2
234300     PERFORM DE-LETE.                                             NC1244.2
234400     GO TO PICTURE-WRITE-34.                                      NC1244.2
234500 PICTURE-FAIL-34.                                                 NC1244.2
234600     PERFORM FAIL.                                                NC1244.2
234700     MOVE WORK-AREA-34B TO COMPUTED-0V18.                         NC1244.2
234800     MOVE .003 TO CORRECT-0V18.                                   NC1244.2
234900 PICTURE-WRITE-34.                                                NC1244.2
235000     MOVE "PICTURE-TEST-34" TO PAR-NAME.                          NC1244.2
235100     PERFORM PRINT-DETAIL.                                        NC1244.2
235200 CCVS-EXIT SECTION.                                               NC1244.2
235300 CCVS-999999.                                                     NC1244.2
235400     GO TO CLOSE-FILES.                                           NC1244.2
      *END-OF,NC124A                                                                  
