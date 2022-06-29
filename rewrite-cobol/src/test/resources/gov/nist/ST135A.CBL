      *HEADER,COBOL,ST135A                                                            
000100 IDENTIFICATION DIVISION.                                         ST1354.2
000200 PROGRAM-ID.                                                      ST1354.2
000300     ST135A.                                                      ST1354.2
000400****************************************************************  ST1354.2
000500*                                                              *  ST1354.2
000600*    VALIDATION FOR:-                                          *  ST1354.2
000700*                                                              *  ST1354.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1354.2
000900*                                                              *  ST1354.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".ST1354.2
001100*                                                              *  ST1354.2
001200****************************************************************  ST1354.2
001300*                                                              *  ST1354.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  ST1354.2
001500*                                                              *  ST1354.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  ST1354.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  ST1354.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  ST1354.2
001900*                                                              *  ST1354.2
002000****************************************************************  ST1354.2
002100*                                                                 ST1354.2
002200*    ST205 BUILDS A FILE, SORTS IT, AND CHECKS IT IN AN OUTPUT    ST1354.2
002300*    PROCEDURE. THE UNUSUAL FEATURE OF THIS PROGRAM IS THAT THE   ST1354.2
002400*    FILES SHARE A NETWORK OF SAME AREA, SAME RECORD AREA, AND    ST1354.2
002500*    SAME SORT AREA. IN ORDER TO THOROUGHLY EXERCISE THE "SAME"   ST1354.2
002600*    OPTIONS, TWO DUMMY FILES, CALLED USELESS-FILE AND DEADWEIGHT-ST1354.2
002700*    FILE, ARE OPENED, CLOSED, READ, AND WRITTEN UPON. HOWEVER,   ST1354.2
002800*    THE CONTENTS OF THESE TWO FILES ARE NEVER CHECKED.           ST1354.2
002900*    SEE THE I-O-CONTROL PARAGRAPH FOR THE ACTUAL ORGANIZATION.   ST1354.2
003000                                                                  ST1354.2
003100 ENVIRONMENT DIVISION.                                            ST1354.2
003200 CONFIGURATION SECTION.                                           ST1354.2
003300 SOURCE-COMPUTER.                                                 ST1354.2
003400     XXXXX082.                                                    ST1354.2
003500 OBJECT-COMPUTER.                                                 ST1354.2
003600     XXXXX083.                                                    ST1354.2
003700 INPUT-OUTPUT SECTION.                                            ST1354.2
003800 FILE-CONTROL.                                                    ST1354.2
003900     SELECT PRINT-FILE ASSIGN TO                                  ST1354.2
004000     XXXXX055.                                                    ST1354.2
004100     SELECT SORTIN-2E ASSIGN TO                                   ST1354.2
004200     XXXXX001.                                                    ST1354.2
004300     SELECT SORTOUT-2E ASSIGN TO                                  ST1354.2
004400     XXXXX002.                                                    ST1354.2
004500     SELECT USELESS-FILE ASSIGN TO                                ST1354.2
004600     XXXXX003.                                                    ST1354.2
004700     SELECT DEADWEIGHT-FILE ASSIGN TO                             ST1354.2
004800     XXXXX004.                                                    ST1354.2
004900     SELECT SORTFILE-2E ASSIGN TO                                 ST1354.2
005000     XXXXX027.                                                    ST1354.2
005100 I-O-CONTROL.                                                     ST1354.2
005200     SAME RECORD AREA FOR                                         ST1354.2
005300              USELESS-FILE                                        ST1354.2
005400              DEADWEIGHT-FILE                                     ST1354.2
005500     SAME SORT                                                    ST1354.2
005600              SORTFILE-2E                                         ST1354.2
005700              USELESS-FILE                                        ST1354.2
005800     SAME                                                         ST1354.2
005900              SORTIN-2E                                           ST1354.2
006000              SORTOUT-2E.                                         ST1354.2
006100 DATA DIVISION.                                                   ST1354.2
006200 FILE SECTION.                                                    ST1354.2
006300 FD  PRINT-FILE.                                                  ST1354.2
006400 01  PRINT-REC PICTURE X(120).                                    ST1354.2
006500 01  DUMMY-RECORD PICTURE X(120).                                 ST1354.2
006600 FD  SORTIN-2E                                                    ST1354.2
006700     LABEL RECORDS STANDARD                                       ST1354.2
006800C    VALUE OF                                                     ST1354.2
006900C    XXXXX074                                                     ST1354.2
007000C    IS                                                           ST1354.2
007100C    XXXXX075                                                     ST1354.2
007200*    XXXXX069                                                     ST1354.2
007300     DATA RECORD IS SORTIN-REC.                                   ST1354.2
007400 01  SORTIN-REC.                                                  ST1354.2
007500     02 SORTIN-NON-KEY-1          PICTURE A(36) JUSTIFIED RIGHT.  ST1354.2
007600     02 SORTIN-KEY                PICTURE 9(8) USAGE DISPLAY.     ST1354.2
007700     02 SORTIN-NON-KEY-2          PICTURE A(36) JUSTIFIED RIGHT.  ST1354.2
007800 FD  SORTOUT-2E                                                   ST1354.2
007900     LABEL RECORDS STANDARD                                       ST1354.2
008000C    VALUE OF                                                     ST1354.2
008100C    XXXXX074                                                     ST1354.2
008200C    IS                                                           ST1354.2
008300C    XXXXX076                                                     ST1354.2
008400*    XXXXX069                                                     ST1354.2
008500     DATA RECORD IS SORTOUT-REC.                                  ST1354.2
008600 01  SORTOUT-REC.                                                 ST1354.2
008700     02 SORTOUT-NON-KEY-1         PICTURE A(36) JUSTIFIED RIGHT.  ST1354.2
008800     02 SORTOUT-KEY               PICTURE 9(8).                   ST1354.2
008900     02 SORTOUT-NON-KEY-2         PICTURE A(36) JUSTIFIED RIGHT.  ST1354.2
009000 FD  USELESS-FILE                                                 ST1354.2
009100     LABEL RECORDS STANDARD                                       ST1354.2
009200C    VALUE OF                                                     ST1354.2
009300C    XXXXX074                                                     ST1354.2
009400C    IS                                                           ST1354.2
009500C    XXXXX077                                                     ST1354.2
009600*    XXXXX069                                                     ST1354.2
009700     DATA RECORD IS USELESS-REC.                                  ST1354.2
009800 01  USELESS-REC.                                                 ST1354.2
009900     02 FILLER                    PICTURE X(80).                  ST1354.2
010000 FD  DEADWEIGHT-FILE                                              ST1354.2
010100     LABEL RECORDS STANDARD                                       ST1354.2
010200C    VALUE OF                                                     ST1354.2
010300C    XXXXX074                                                     ST1354.2
010400C    IS                                                           ST1354.2
010500C    XXXXX078                                                     ST1354.2
010600*    XXXXX069                                                     ST1354.2
010700     DATA RECORD IS DEADWEIGHT-REC.                               ST1354.2
010800 01  DEADWEIGHT-REC               PICTURE X(80).                  ST1354.2
010900 SD  SORTFILE-2E                                                  ST1354.2
011000     DATA RECORD IS SORTFILE-REC.                                 ST1354.2
011100 01  SORTFILE-REC.                                                ST1354.2
011200     02 SORTFILE-NON-KEY-1        PICTURE A(36) JUSTIFIED RIGHT.  ST1354.2
011300     02 SORTFILE-KEY              PICTURE 9(8).                   ST1354.2
011400     02 SORTFILE-NON-KEY-2        PICTURE A(36) JUSTIFIED RIGHT.  ST1354.2
011500 WORKING-STORAGE SECTION.                                         ST1354.2
011600 77  COMMENT-1 PICTURE X(27) VALUE "FIRST 20 OF 36-CHAR FIELD  ". ST1354.2
011700 77  COMMENT-2 PICTURE X(27) VALUE "LAST 16 OF 36-CHAR FIELD   ". ST1354.2
011800 77  UTIL-CTR           PICTURE S99999.                           ST1354.2
011900 01  LITERALS.                                                    ST1354.2
012000     02 SP-ACE          PICTURE X(14) VALUE "      (SPACES)".     ST1354.2
012100     02 LITERAL-A       PICTURE X(16) VALUE "               A".   ST1354.2
012200     02 LITERAL-B       PICTURE X(16) VALUE "               B".   ST1354.2
012300 01  COMPUTED-BREAKDOWN.                                          ST1354.2
012400     02 FIRST-20        PICTURE X(20).                            ST1354.2
012500     02 LAST-20         PICTURE X(20).                            ST1354.2
012600 01  TEST-RESULTS.                                                ST1354.2
012700     02 FILLER                   PIC X      VALUE SPACE.          ST1354.2
012800     02 FEATURE                  PIC X(20)  VALUE SPACE.          ST1354.2
012900     02 FILLER                   PIC X      VALUE SPACE.          ST1354.2
013000     02 P-OR-F                   PIC X(5)   VALUE SPACE.          ST1354.2
013100     02 FILLER                   PIC X      VALUE SPACE.          ST1354.2
013200     02  PAR-NAME.                                                ST1354.2
013300       03 FILLER                 PIC X(19)  VALUE SPACE.          ST1354.2
013400       03  PARDOT-X              PIC X      VALUE SPACE.          ST1354.2
013500       03 DOTVALUE               PIC 99     VALUE ZERO.           ST1354.2
013600     02 FILLER                   PIC X(8)   VALUE SPACE.          ST1354.2
013700     02 RE-MARK                  PIC X(61).                       ST1354.2
013800 01  TEST-COMPUTED.                                               ST1354.2
013900     02 FILLER                   PIC X(30)  VALUE SPACE.          ST1354.2
014000     02 FILLER                   PIC X(17)  VALUE                 ST1354.2
014100            "       COMPUTED=".                                   ST1354.2
014200     02 COMPUTED-X.                                               ST1354.2
014300     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          ST1354.2
014400     03 COMPUTED-N               REDEFINES COMPUTED-A             ST1354.2
014500                                 PIC -9(9).9(9).                  ST1354.2
014600     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         ST1354.2
014700     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     ST1354.2
014800     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     ST1354.2
014900     03       CM-18V0 REDEFINES COMPUTED-A.                       ST1354.2
015000         04 COMPUTED-18V0                    PIC -9(18).          ST1354.2
015100         04 FILLER                           PIC X.               ST1354.2
015200     03 FILLER PIC X(50) VALUE SPACE.                             ST1354.2
015300 01  TEST-CORRECT.                                                ST1354.2
015400     02 FILLER PIC X(30) VALUE SPACE.                             ST1354.2
015500     02 FILLER PIC X(17) VALUE "       CORRECT =".                ST1354.2
015600     02 CORRECT-X.                                                ST1354.2
015700     03 CORRECT-A                  PIC X(20) VALUE SPACE.         ST1354.2
015800     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      ST1354.2
015900     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         ST1354.2
016000     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     ST1354.2
016100     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     ST1354.2
016200     03      CR-18V0 REDEFINES CORRECT-A.                         ST1354.2
016300         04 CORRECT-18V0                     PIC -9(18).          ST1354.2
016400         04 FILLER                           PIC X.               ST1354.2
016500     03 FILLER PIC X(2) VALUE SPACE.                              ST1354.2
016600     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     ST1354.2
016700 01  CCVS-C-1.                                                    ST1354.2
016800     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAST1354.2
016900-    "SS  PARAGRAPH-NAME                                          ST1354.2
017000-    "       REMARKS".                                            ST1354.2
017100     02 FILLER                     PIC X(20)    VALUE SPACE.      ST1354.2
017200 01  CCVS-C-2.                                                    ST1354.2
017300     02 FILLER                     PIC X        VALUE SPACE.      ST1354.2
017400     02 FILLER                     PIC X(6)     VALUE "TESTED".   ST1354.2
017500     02 FILLER                     PIC X(15)    VALUE SPACE.      ST1354.2
017600     02 FILLER                     PIC X(4)     VALUE "FAIL".     ST1354.2
017700     02 FILLER                     PIC X(94)    VALUE SPACE.      ST1354.2
017800 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       ST1354.2
017900 01  REC-CT                        PIC 99       VALUE ZERO.       ST1354.2
018000 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       ST1354.2
018100 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       ST1354.2
018200 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       ST1354.2
018300 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       ST1354.2
018400 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       ST1354.2
018500 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       ST1354.2
018600 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      ST1354.2
018700 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       ST1354.2
018800 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     ST1354.2
018900 01  CCVS-H-1.                                                    ST1354.2
019000     02  FILLER                    PIC X(39)    VALUE SPACES.     ST1354.2
019100     02  FILLER                    PIC X(42)    VALUE             ST1354.2
019200     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 ST1354.2
019300     02  FILLER                    PIC X(39)    VALUE SPACES.     ST1354.2
019400 01  CCVS-H-2A.                                                   ST1354.2
019500   02  FILLER                        PIC X(40)  VALUE SPACE.      ST1354.2
019600   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  ST1354.2
019700   02  FILLER                        PIC XXXX   VALUE             ST1354.2
019800     "4.2 ".                                                      ST1354.2
019900   02  FILLER                        PIC X(28)  VALUE             ST1354.2
020000            " COPY - NOT FOR DISTRIBUTION".                       ST1354.2
020100   02  FILLER                        PIC X(41)  VALUE SPACE.      ST1354.2
020200                                                                  ST1354.2
020300 01  CCVS-H-2B.                                                   ST1354.2
020400   02  FILLER                        PIC X(15)  VALUE             ST1354.2
020500            "TEST RESULT OF ".                                    ST1354.2
020600   02  TEST-ID                       PIC X(9).                    ST1354.2
020700   02  FILLER                        PIC X(4)   VALUE             ST1354.2
020800            " IN ".                                               ST1354.2
020900   02  FILLER                        PIC X(12)  VALUE             ST1354.2
021000     " HIGH       ".                                              ST1354.2
021100   02  FILLER                        PIC X(22)  VALUE             ST1354.2
021200            " LEVEL VALIDATION FOR ".                             ST1354.2
021300   02  FILLER                        PIC X(58)  VALUE             ST1354.2
021400     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1354.2
021500 01  CCVS-H-3.                                                    ST1354.2
021600     02  FILLER                      PIC X(34)  VALUE             ST1354.2
021700            " FOR OFFICIAL USE ONLY    ".                         ST1354.2
021800     02  FILLER                      PIC X(58)  VALUE             ST1354.2
021900     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".ST1354.2
022000     02  FILLER                      PIC X(28)  VALUE             ST1354.2
022100            "  COPYRIGHT   1985 ".                                ST1354.2
022200 01  CCVS-E-1.                                                    ST1354.2
022300     02 FILLER                       PIC X(52)  VALUE SPACE.      ST1354.2
022400     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              ST1354.2
022500     02 ID-AGAIN                     PIC X(9).                    ST1354.2
022600     02 FILLER                       PIC X(45)  VALUE SPACES.     ST1354.2
022700 01  CCVS-E-2.                                                    ST1354.2
022800     02  FILLER                      PIC X(31)  VALUE SPACE.      ST1354.2
022900     02  FILLER                      PIC X(21)  VALUE SPACE.      ST1354.2
023000     02 CCVS-E-2-2.                                               ST1354.2
023100         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      ST1354.2
023200         03 FILLER                   PIC X      VALUE SPACE.      ST1354.2
023300         03 ENDER-DESC               PIC X(44)  VALUE             ST1354.2
023400            "ERRORS ENCOUNTERED".                                 ST1354.2
023500 01  CCVS-E-3.                                                    ST1354.2
023600     02  FILLER                      PIC X(22)  VALUE             ST1354.2
023700            " FOR OFFICIAL USE ONLY".                             ST1354.2
023800     02  FILLER                      PIC X(12)  VALUE SPACE.      ST1354.2
023900     02  FILLER                      PIC X(58)  VALUE             ST1354.2
024000     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1354.2
024100     02  FILLER                      PIC X(13)  VALUE SPACE.      ST1354.2
024200     02 FILLER                       PIC X(15)  VALUE             ST1354.2
024300             " COPYRIGHT 1985".                                   ST1354.2
024400 01  CCVS-E-4.                                                    ST1354.2
024500     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      ST1354.2
024600     02 FILLER                       PIC X(4)   VALUE " OF ".     ST1354.2
024700     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      ST1354.2
024800     02 FILLER                       PIC X(40)  VALUE             ST1354.2
024900      "  TESTS WERE EXECUTED SUCCESSFULLY".                       ST1354.2
025000 01  XXINFO.                                                      ST1354.2
025100     02 FILLER                       PIC X(19)  VALUE             ST1354.2
025200            "*** INFORMATION ***".                                ST1354.2
025300     02 INFO-TEXT.                                                ST1354.2
025400       04 FILLER                     PIC X(8)   VALUE SPACE.      ST1354.2
025500       04 XXCOMPUTED                 PIC X(20).                   ST1354.2
025600       04 FILLER                     PIC X(5)   VALUE SPACE.      ST1354.2
025700       04 XXCORRECT                  PIC X(20).                   ST1354.2
025800     02 INF-ANSI-REFERENCE           PIC X(48).                   ST1354.2
025900 01  HYPHEN-LINE.                                                 ST1354.2
026000     02 FILLER  PIC IS X VALUE IS SPACE.                          ST1354.2
026100     02 FILLER  PIC IS X(65)    VALUE IS "************************ST1354.2
026200-    "*****************************************".                 ST1354.2
026300     02 FILLER  PIC IS X(54)    VALUE IS "************************ST1354.2
026400-    "******************************".                            ST1354.2
026500 01  CCVS-PGM-ID                     PIC X(9)   VALUE             ST1354.2
026600     "ST135A".                                                    ST1354.2
026700 PROCEDURE DIVISION.                                              ST1354.2
026800 CCVS1 SECTION.                                                   ST1354.2
026900 OPEN-FILES.                                                      ST1354.2
027000     OPEN     OUTPUT PRINT-FILE.                                  ST1354.2
027100     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   ST1354.2
027200     MOVE    SPACE TO TEST-RESULTS.                               ST1354.2
027300     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             ST1354.2
027400     GO TO CCVS1-EXIT.                                            ST1354.2
027500 CLOSE-FILES.                                                     ST1354.2
027600     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   ST1354.2
027700 TERMINATE-CCVS.                                                  ST1354.2
027800*    EXIT PROGRAM.                                                ST1354.2
027900*TERMINATE-CALL.                                                  ST1354.2
028000     STOP     RUN.                                                ST1354.2
028100 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         ST1354.2
028200 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           ST1354.2
028300 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          ST1354.2
028400 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      ST1354.2
028500     MOVE "****TEST DELETED****" TO RE-MARK.                      ST1354.2
028600 PRINT-DETAIL.                                                    ST1354.2
028700     IF REC-CT NOT EQUAL TO ZERO                                  ST1354.2
028800             MOVE "." TO PARDOT-X                                 ST1354.2
028900             MOVE REC-CT TO DOTVALUE.                             ST1354.2
029000     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      ST1354.2
029100     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               ST1354.2
029200        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 ST1354.2
029300          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 ST1354.2
029400     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              ST1354.2
029500     MOVE SPACE TO CORRECT-X.                                     ST1354.2
029600     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         ST1354.2
029700     MOVE     SPACE TO RE-MARK.                                   ST1354.2
029800 HEAD-ROUTINE.                                                    ST1354.2
029900     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  ST1354.2
030000     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  ST1354.2
030100     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  ST1354.2
030200     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  ST1354.2
030300 COLUMN-NAMES-ROUTINE.                                            ST1354.2
030400     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           ST1354.2
030500     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1354.2
030600     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        ST1354.2
030700 END-ROUTINE.                                                     ST1354.2
030800     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.ST1354.2
030900 END-RTN-EXIT.                                                    ST1354.2
031000     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1354.2
031100 END-ROUTINE-1.                                                   ST1354.2
031200      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      ST1354.2
031300      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               ST1354.2
031400      ADD PASS-COUNTER TO ERROR-HOLD.                             ST1354.2
031500*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   ST1354.2
031600      MOVE PASS-COUNTER TO CCVS-E-4-1.                            ST1354.2
031700      MOVE ERROR-HOLD TO CCVS-E-4-2.                              ST1354.2
031800      MOVE CCVS-E-4 TO CCVS-E-2-2.                                ST1354.2
031900      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           ST1354.2
032000  END-ROUTINE-12.                                                 ST1354.2
032100      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        ST1354.2
032200     IF       ERROR-COUNTER IS EQUAL TO ZERO                      ST1354.2
032300         MOVE "NO " TO ERROR-TOTAL                                ST1354.2
032400         ELSE                                                     ST1354.2
032500         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       ST1354.2
032600     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           ST1354.2
032700     PERFORM WRITE-LINE.                                          ST1354.2
032800 END-ROUTINE-13.                                                  ST1354.2
032900     IF DELETE-COUNTER IS EQUAL TO ZERO                           ST1354.2
033000         MOVE "NO " TO ERROR-TOTAL  ELSE                          ST1354.2
033100         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      ST1354.2
033200     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   ST1354.2
033300     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           ST1354.2
033400      IF   INSPECT-COUNTER EQUAL TO ZERO                          ST1354.2
033500          MOVE "NO " TO ERROR-TOTAL                               ST1354.2
033600      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   ST1354.2
033700      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            ST1354.2
033800      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          ST1354.2
033900     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           ST1354.2
034000 WRITE-LINE.                                                      ST1354.2
034100     ADD 1 TO RECORD-COUNT.                                       ST1354.2
034200*    IF RECORD-COUNT GREATER 42                                   ST1354.2
034300*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          ST1354.2
034400*        MOVE SPACE TO DUMMY-RECORD                               ST1354.2
034500*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  ST1354.2
034600*        MOVE CCVS-H-1  TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    ST1354.2
034700*        MOVE CCVS-H-2A TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    ST1354.2
034800*        MOVE CCVS-H-2B TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    ST1354.2
034900*        MOVE CCVS-H-3  TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    ST1354.2
035000*        MOVE CCVS-C-1  TO DUMMY-RECORD PERFORM WRT-LN            ST1354.2
035100*        MOVE CCVS-C-2  TO DUMMY-RECORD PERFORM WRT-LN            ST1354.2
035200*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          ST1354.2
035300*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          ST1354.2
035400*        MOVE ZERO TO RECORD-COUNT.                               ST1354.2
035500     PERFORM WRT-LN.                                              ST1354.2
035600 WRT-LN.                                                          ST1354.2
035700     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               ST1354.2
035800     MOVE SPACE TO DUMMY-RECORD.                                  ST1354.2
035900 BLANK-LINE-PRINT.                                                ST1354.2
036000     PERFORM WRT-LN.                                              ST1354.2
036100 FAIL-ROUTINE.                                                    ST1354.2
036200     IF     COMPUTED-X NOT EQUAL TO SPACE                         ST1354.2
036300            GO TO   FAIL-ROUTINE-WRITE.                           ST1354.2
036400     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.ST1354.2
036500     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 ST1354.2
036600     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   ST1354.2
036700     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1354.2
036800     MOVE   SPACES TO INF-ANSI-REFERENCE.                         ST1354.2
036900     GO TO  FAIL-ROUTINE-EX.                                      ST1354.2
037000 FAIL-ROUTINE-WRITE.                                              ST1354.2
037100     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         ST1354.2
037200     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 ST1354.2
037300     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. ST1354.2
037400     MOVE   SPACES TO COR-ANSI-REFERENCE.                         ST1354.2
037500 FAIL-ROUTINE-EX. EXIT.                                           ST1354.2
037600 BAIL-OUT.                                                        ST1354.2
037700     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   ST1354.2
037800     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           ST1354.2
037900 BAIL-OUT-WRITE.                                                  ST1354.2
038000     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  ST1354.2
038100     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 ST1354.2
038200     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1354.2
038300     MOVE   SPACES TO INF-ANSI-REFERENCE.                         ST1354.2
038400 BAIL-OUT-EX. EXIT.                                               ST1354.2
038500 CCVS1-EXIT.                                                      ST1354.2
038600     EXIT.                                                        ST1354.2
038700 BUILD-AND-SORT-AND-CHECK SECTION.                                ST1354.2
038800 BASAC.                                                           ST1354.2
038900     OPEN     OUTPUT SORTIN-2E.                                   ST1354.2
039000     OPEN     OUTPUT USELESS-FILE.                                ST1354.2
039100     OPEN     OUTPUT DEADWEIGHT-FILE.                             ST1354.2
039200     MOVE     +10 TO UTIL-CTR.                                    ST1354.2
039300 BUILD-LOOP.                                                      ST1354.2
039400     MOVE     UTIL-CTR TO SORTIN-KEY.                             ST1354.2
039500*        NOTE UTIL-CTR IS INTENTIONALLY MOVED TO AN UNSIGNED ITEM.ST1354.2
039600     MOVE     "A" TO SORTIN-NON-KEY-1.                            ST1354.2
039700     MOVE     "B" TO SORTIN-NON-KEY-2.                            ST1354.2
039800     WRITE    SORTIN-REC.                                         ST1354.2
039900     MOVE     SPACE TO USELESS-REC.                               ST1354.2
040000     MOVE     SPACE TO DEADWEIGHT-REC.                            ST1354.2
040100     WRITE    USELESS-REC.                                        ST1354.2
040200     WRITE    DEADWEIGHT-REC.                                     ST1354.2
040300     SUBTRACT +1 FROM UTIL-CTR.                                   ST1354.2
040400     IF       UTIL-CTR GREATER THAN -11 GO TO BUILD-LOOP.         ST1354.2
040500     CLOSE    SORTIN-2E.                                          ST1354.2
040600     CLOSE    USELESS-FILE.                                       ST1354.2
040700     CLOSE    DEADWEIGHT-FILE.                                    ST1354.2
040800 BUILD-TEST.                                                      ST1354.2
040900     IF       UTIL-CTR EQUAL TO -11                               ST1354.2
041000              PERFORM PASS GO TO BUILD-WRITE.                     ST1354.2
041100 BUILD-FAIL.                                                      ST1354.2
041200     PERFORM  FAIL.                                               ST1354.2
041300     MOVE     UTIL-CTR TO COMPUTED-N.                             ST1354.2
041400     MOVE     -11 TO CORRECT-N.                                   ST1354.2
041500 BUILD-WRITE.                                                     ST1354.2
041600     MOVE     "TAPE BEING BUILT" TO FEATURE.                      ST1354.2
041700     MOVE     "BUILD-TEST" TO PAR-NAME.                           ST1354.2
041800     PERFORM  PRINT-DETAIL.                                       ST1354.2
041900 SORT-PARAGRAPH.                                                  ST1354.2
042000     SORT     SORTFILE-2E ON ASCENDING                            ST1354.2
042100              SORTFILE-KEY                                        ST1354.2
042200              USING SORTIN-2E                                     ST1354.2
042300              OUTPUT PROCEDURE OUTPROC.                           ST1354.2
042400 SORT-INIT.                                                       ST1354.2
042500     OPEN     INPUT SORTOUT-2E.                                   ST1354.2
042600     OPEN     INPUT USELESS-FILE.                                 ST1354.2
042700     OPEN     INPUT DEADWEIGHT-FILE.                              ST1354.2
042800     MOVE     +0 TO UTIL-CTR.                                     ST1354.2
042900     MOVE     "SORT ---" TO FEATURE.                              ST1354.2
043000     PERFORM  PRINT-DETAIL.                                       ST1354.2
043100     MOVE     "  SAME AREA" TO FEATURE.                           ST1354.2
043200 SORT-TEST-1.                                                     ST1354.2
043300     MOVE     "SORT-TEST-1" TO PAR-NAME.                          ST1354.2
043400     PERFORM  READ-SORTOUT.                                       ST1354.2
043500     IF      SORTOUT-NON-KEY-1 NOT EQUAL TO "                     ST1354.2
043600-    "              A" GO TO SORT-FAIL-1.                         ST1354.2
043700     IF      SORTOUT-NON-KEY-2 NOT EQUAL TO "                     ST1354.2
043800-    "              B" GO TO SORT-FAIL-1.                         ST1354.2
043900     IF      SORTOUT-KEY EQUAL TO ZERO                            ST1354.2
044000              PERFORM PASS GO TO SORT-WRITE-1.                    ST1354.2
044100 SORT-FAIL-1.                                                     ST1354.2
044200     MOVE     ZERO TO CORRECT-N.                                  ST1354.2
044300     PERFORM  BREAKDOWN-PARA.                                     ST1354.2
044400 SORT-WRITE-1.                                                    ST1354.2
044500     PERFORM  PRINT-DETAIL.                                       ST1354.2
044600 SORT-TEST-2.                                                     ST1354.2
044700     MOVE     "SORT-TEST-2" TO PAR-NAME.                          ST1354.2
044800     PERFORM  READ-SORTOUT.                                       ST1354.2
044900     IF      SORTOUT-NON-KEY-1 NOT EQUAL TO "                     ST1354.2
045000-    "              A" GO TO SORT-FAIL-2.                         ST1354.2
045100     IF      SORTOUT-NON-KEY-2 NOT EQUAL TO "                     ST1354.2
045200-    "              B" GO TO SORT-FAIL-2.                         ST1354.2
045300     IF      SORTOUT-KEY EQUAL TO 1                               ST1354.2
045400              PERFORM PASS GO TO SORT-WRITE-2.                    ST1354.2
045500 SORT-FAIL-2.                                                     ST1354.2
045600     MOVE     1    TO CORRECT-N.                                  ST1354.2
045700     PERFORM  BREAKDOWN-PARA.                                     ST1354.2
045800 SORT-WRITE-2.                                                    ST1354.2
045900     PERFORM  PRINT-DETAIL.                                       ST1354.2
046000 SORT-TEST-3.                                                     ST1354.2
046100     MOVE     "SORT-TEST-3" TO PAR-NAME.                          ST1354.2
046200     PERFORM  READ-SORTOUT.                                       ST1354.2
046300     IF      SORTOUT-NON-KEY-1 NOT EQUAL TO "                     ST1354.2
046400-    "              A" GO TO SORT-FAIL-3.                         ST1354.2
046500     IF      SORTOUT-NON-KEY-2 NOT EQUAL TO "                     ST1354.2
046600-    "              B" GO TO SORT-FAIL-3.                         ST1354.2
046700     IF      SORTOUT-KEY EQUAL TO 1                               ST1354.2
046800              PERFORM PASS GO TO SORT-WRITE-3.                    ST1354.2
046900 SORT-FAIL-3.                                                     ST1354.2
047000     MOVE     1    TO CORRECT-N.                                  ST1354.2
047100     PERFORM  BREAKDOWN-PARA.                                     ST1354.2
047200 SORT-WRITE-3.                                                    ST1354.2
047300     PERFORM  PRINT-DETAIL.                                       ST1354.2
047400 SORT-TEST-4.                                                     ST1354.2
047500     MOVE     "SORT-TEST-4" TO PAR-NAME.                          ST1354.2
047600     PERFORM  READ-SORTOUT 10 TIMES.                              ST1354.2
047700     IF      SORTOUT-NON-KEY-1 NOT EQUAL TO "                     ST1354.2
047800-    "              A" GO TO SORT-FAIL-4.                         ST1354.2
047900     IF      SORTOUT-NON-KEY-2 NOT EQUAL TO "                     ST1354.2
048000-    "              B" GO TO SORT-FAIL-4.                         ST1354.2
048100     IF      SORTOUT-KEY EQUAL TO 6                               ST1354.2
048200              PERFORM PASS GO TO SORT-WRITE-4.                    ST1354.2
048300 SORT-FAIL-4.                                                     ST1354.2
048400     MOVE     6    TO CORRECT-N.                                  ST1354.2
048500     PERFORM  BREAKDOWN-PARA.                                     ST1354.2
048600 SORT-WRITE-4.                                                    ST1354.2
048700     PERFORM  PRINT-DETAIL.                                       ST1354.2
048800 SORT-TEST-5.                                                     ST1354.2
048900     MOVE     "SORT-TEST-5" TO PAR-NAME.                          ST1354.2
049000     PERFORM  READ-SORTOUT.                                       ST1354.2
049100     IF      SORTOUT-NON-KEY-1 NOT EQUAL TO "                     ST1354.2
049200-    "              A" GO TO SORT-FAIL-5.                         ST1354.2
049300     IF      SORTOUT-NON-KEY-2 NOT EQUAL TO "                     ST1354.2
049400-    "              B" GO TO SORT-FAIL-5.                         ST1354.2
049500     IF      SORTOUT-KEY EQUAL TO 7                               ST1354.2
049600              PERFORM PASS GO TO SORT-WRITE-5.                    ST1354.2
049700 SORT-FAIL-5.                                                     ST1354.2
049800     MOVE     7    TO CORRECT-N.                                  ST1354.2
049900     PERFORM  BREAKDOWN-PARA.                                     ST1354.2
050000 SORT-WRITE-5.                                                    ST1354.2
050100     PERFORM  PRINT-DETAIL.                                       ST1354.2
050200 SORT-TEST-6.                                                     ST1354.2
050300     MOVE     "SORT-TEST-6" TO PAR-NAME.                          ST1354.2
050400     PERFORM  READ-SORTOUT 6 TIMES                                ST1354.2
050500     IF      SORTOUT-NON-KEY-1 NOT EQUAL TO "                     ST1354.2
050600-    "              A" GO TO SORT-FAIL-6.                         ST1354.2
050700     IF      SORTOUT-NON-KEY-2 NOT EQUAL TO "                     ST1354.2
050800-    "              B" GO TO SORT-FAIL-6.                         ST1354.2
050900     IF      SORTOUT-KEY EQUAL TO 10                              ST1354.2
051000              PERFORM PASS GO TO SORT-WRITE-6.                    ST1354.2
051100 SORT-FAIL-6.                                                     ST1354.2
051200     MOVE     10   TO CORRECT-N.                                  ST1354.2
051300     PERFORM  BREAKDOWN-PARA.                                     ST1354.2
051400 SORT-WRITE-6.                                                    ST1354.2
051500     PERFORM  PRINT-DETAIL.                                       ST1354.2
051600 SORT-TEST-7.                                                     ST1354.2
051700     MOVE     "SORT-TEST-7" TO PAR-NAME.                          ST1354.2
051800     PERFORM  READ-SORTOUT.                                       ST1354.2
051900     IF      SORTOUT-NON-KEY-1 NOT EQUAL TO "                     ST1354.2
052000-    "              A" GO TO SORT-FAIL-7.                         ST1354.2
052100     IF      SORTOUT-NON-KEY-2 NOT EQUAL TO "                     ST1354.2
052200-    "              B" GO TO SORT-FAIL-7.                         ST1354.2
052300     IF      SORTOUT-KEY EQUAL TO 10                              ST1354.2
052400              PERFORM PASS GO TO SORT-WRITE-7.                    ST1354.2
052500 SORT-FAIL-7.                                                     ST1354.2
052600     MOVE     10   TO CORRECT-N.                                  ST1354.2
052700     PERFORM  BREAKDOWN-PARA.                                     ST1354.2
052800 SORT-WRITE-7.                                                    ST1354.2
052900     PERFORM  PRINT-DETAIL.                                       ST1354.2
053000 SORT-TEST-8.                                                     ST1354.2
053100     MOVE     "SORT-TEST-8" TO PAR-NAME.                          ST1354.2
053200     READ     SORTOUT-2E AT END                                   ST1354.2
053300              PERFORM PASS GO TO SORT-WRITE-8.                    ST1354.2
053400 SORT-FAIL-8.                                                     ST1354.2
053500     MOVE     SPACE TO LITERALS.                                  ST1354.2
053600     MOVE     UTIL-CTR TO COMPUTED-N.                             ST1354.2
053700     MOVE     21 TO CORRECT-N.                                    ST1354.2
053800     MOVE     "END OF FILE NOT FOUND" TO RE-MARK.                 ST1354.2
053900     PERFORM  PRINT-DETAIL.                                       ST1354.2
054000     PERFORM  BREAKDOWN-PARA.                                     ST1354.2
054100 SORT-WRITE-8.                                                    ST1354.2
054200     PERFORM  PRINT-DETAIL.                                       ST1354.2
054300 CLOSE-SORT-FILES.                                                ST1354.2
054400     CLOSE    USELESS-FILE.                                       ST1354.2
054500     CLOSE    DEADWEIGHT-FILE.                                    ST1354.2
054600     CLOSE    SORTOUT-2E.                                         ST1354.2
054700     GO TO CCVS-EXIT.                                             ST1354.2
054800 BREAKDOWN-PARA.                                                  ST1354.2
054900     MOVE     SORTOUT-KEY TO COMPUTED-N.                          ST1354.2
055000     PERFORM  FAIL.                                               ST1354.2
055100     MOVE     "KEY AREA" TO RE-MARK.                              ST1354.2
055200     PERFORM  PRINT-DETAIL.                                       ST1354.2
055300     MOVE     SORTOUT-NON-KEY-1 TO COMPUTED-BREAKDOWN.            ST1354.2
055400     MOVE     FIRST-20 TO COMPUTED-A.                             ST1354.2
055500     MOVE     SP-ACE TO CORRECT-A.                                ST1354.2
055600     MOVE     COMMENT-1 TO RE-MARK.                               ST1354.2
055700     PERFORM  PRINT-DETAIL.                                       ST1354.2
055800     MOVE     LAST-20 TO COMPUTED-A.                              ST1354.2
055900     MOVE     LITERAL-A TO CORRECT-A.                             ST1354.2
056000     MOVE     COMMENT-2 TO RE-MARK.                               ST1354.2
056100     PERFORM  PRINT-DETAIL.                                       ST1354.2
056200     MOVE     SORTOUT-NON-KEY-2 TO COMPUTED-BREAKDOWN.            ST1354.2
056300     MOVE     FIRST-20 TO COMPUTED-A.                             ST1354.2
056400     MOVE     SP-ACE TO CORRECT-A.                                ST1354.2
056500     MOVE     COMMENT-1 TO RE-MARK.                               ST1354.2
056600     PERFORM  PRINT-DETAIL.                                       ST1354.2
056700     MOVE     LAST-20 TO COMPUTED-A.                              ST1354.2
056800     MOVE     SP-ACE TO CORRECT-A.                                ST1354.2
056900     MOVE     COMMENT-2 TO RE-MARK.                               ST1354.2
057000 READ-SORTOUT.                                                    ST1354.2
057100     READ     SORTOUT-2E AT END GO TO READ-ERROR.                 ST1354.2
057200     ADD      1 TO UTIL-CTR.                                      ST1354.2
057300 READ-ERROR.                                                      ST1354.2
057400     PERFORM  FAIL.                                               ST1354.2
057500     MOVE     "READ-ERROR" TO PAR-NAME.                           ST1354.2
057600     MOVE     UTIL-CTR TO COMPUTED-N.                             ST1354.2
057700     MOVE     21 TO CORRECT-N.                                    ST1354.2
057800     MOVE     "EOF PREMATURELY FOUND" TO RE-MARK.                 ST1354.2
057900     PERFORM  PRINT-DETAIL.                                       ST1354.2
058000     GO TO    CLOSE-SORT-FILES.                                   ST1354.2
058100 OUTPROC SECTION.                                                 ST1354.2
058200 OUTPROC-OPEN.                                                    ST1354.2
058300     OPEN     OUTPUT SORTOUT-2E.                                  ST1354.2
058400 OUTPROC-RETURN.                                                  ST1354.2
058500     RETURN   SORTFILE-2E AT END GO TO OUTPROC-CLOSE.             ST1354.2
058600     MOVE     SORTFILE-REC TO SORTOUT-REC.                        ST1354.2
058700     WRITE    SORTOUT-REC.                                        ST1354.2
058800     GO       TO OUTPROC-RETURN.                                  ST1354.2
058900 OUTPROC-CLOSE.                                                   ST1354.2
059000     CLOSE    SORTOUT-2E.                                         ST1354.2
059100 CCVS-EXIT SECTION.                                               ST1354.2
059200 CCVS-999999.                                                     ST1354.2
059300     GO TO CLOSE-FILES.                                           ST1354.2
      *END-OF,ST135A                                                          TES06520
