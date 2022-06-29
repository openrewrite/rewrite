      *HEADER,COBOL,ST106A,SUBPRG,ST107A                                              
000100 IDENTIFICATION DIVISION.                                         ST1074.2
000200 PROGRAM-ID.                                                      ST1074.2
000300     ST107A.                                                      ST1074.2
000400****************************************************************  ST1074.2
000500*                                                              *  ST1074.2
000600*    VALIDATION FOR:-                                          *  ST1074.2
000700*                                                              *  ST1074.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1074.2
000900*                                                              *  ST1074.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".ST1074.2
001100*                                                              *  ST1074.2
001200****************************************************************  ST1074.2
001300*                                                              *  ST1074.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  ST1074.2
001500*                                                              *  ST1074.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  ST1074.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  ST1074.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  ST1074.2
001900*                                                              *  ST1074.2
002000****************************************************************  ST1074.2
002100 ENVIRONMENT DIVISION.                                            ST1074.2
002200 CONFIGURATION SECTION.                                           ST1074.2
002300 SOURCE-COMPUTER.                                                 ST1074.2
002400     XXXXX082.                                                    ST1074.2
002500 OBJECT-COMPUTER.                                                 ST1074.2
002600     XXXXX083.                                                    ST1074.2
002700 INPUT-OUTPUT SECTION.                                            ST1074.2
002800 FILE-CONTROL.                                                    ST1074.2
002900     SELECT PRINT-FILE ASSIGN TO                                  ST1074.2
003000     XXXXX055.                                                    ST1074.2
003100     SELECT   SORTIN-1G ASSIGN TO                                 ST1074.2
003200     XXXXD001.                                                    ST1074.2
003300 DATA DIVISION.                                                   ST1074.2
003400 FILE SECTION.                                                    ST1074.2
003500 FD  PRINT-FILE.                                                  ST1074.2
003600 01  PRINT-REC PICTURE X(120).                                    ST1074.2
003700 01  DUMMY-RECORD PICTURE X(120).                                 ST1074.2
003800 FD  SORTIN-1G                                                    ST1074.2
003900     LABEL RECORDS STANDARD                                       ST1074.2
004000C    VALUE OF                                                     ST1074.2
004100C    XXXXX074                                                     ST1074.2
004200C    IS                                                           ST1074.2
004300C    XXXXX075                                                     ST1074.2
004400*    XXXXX069                                                     ST1074.2
004500     RECORD   CONTAINS 27 CHARACTERS.                             ST1074.2
004600 01  SORTIN-REC.                                                  ST1074.2
004700     02 SORTKEY-3       PICTURE X.                                ST1074.2
004800     02 SORTKEY-1       PICTURE S9V9(7).                          ST1074.2
004900     02 SORTKEY-2       PICTURE 9(18).                            ST1074.2
005000 WORKING-STORAGE SECTION.                                         ST1074.2
005100 77  UTIL-CTR           PICTURE S99999 VALUE ZERO.                ST1074.2
005200 77  ITEM-3   PICTURE X(27) VALUE "FIRST OF 3 ITEMS IN RECORD ".  ST1074.2
005300 77  ITEM-1   PICTURE X(27) VALUE "  SECOND OF 3 ITEMS        ".  ST1074.2
005400 77  ITEM-2   PICTURE X(27) VALUE "    THIRD OF 3 ITEMS       ".  ST1074.2
005500 77  DUM-MY   PICTURE X(27) VALUE "TEST UNNECESSARY - BYPASSED".  ST1074.2
005600 77  ZER-O              PICTURE X VALUE "0".                      ST1074.2
005700 77  SPAC-E             PICTURE X VALUE " ".                      ST1074.2
005800 01  UTILITY-KEYS.                                                ST1074.2
005900     02 UTILITY-3       PICTURE X.                                ST1074.2
006000     02 UTILITY-1       PICTURE S9V9(7).                          ST1074.2
006100     02 UTILITY-2       PICTURE 9(018).                           ST1074.2
006200 01  TEST-RESULTS.                                                ST1074.2
006300     02 FILLER                   PIC X      VALUE SPACE.          ST1074.2
006400     02 FEATURE                  PIC X(20)  VALUE SPACE.          ST1074.2
006500     02 FILLER                   PIC X      VALUE SPACE.          ST1074.2
006600     02 P-OR-F                   PIC X(5)   VALUE SPACE.          ST1074.2
006700     02 FILLER                   PIC X      VALUE SPACE.          ST1074.2
006800     02  PAR-NAME.                                                ST1074.2
006900       03 FILLER                 PIC X(19)  VALUE SPACE.          ST1074.2
007000       03  PARDOT-X              PIC X      VALUE SPACE.          ST1074.2
007100       03 DOTVALUE               PIC 99     VALUE ZERO.           ST1074.2
007200     02 FILLER                   PIC X(8)   VALUE SPACE.          ST1074.2
007300     02 RE-MARK                  PIC X(61).                       ST1074.2
007400 01  TEST-COMPUTED.                                               ST1074.2
007500     02 FILLER                   PIC X(30)  VALUE SPACE.          ST1074.2
007600     02 FILLER                   PIC X(17)  VALUE                 ST1074.2
007700            "       COMPUTED=".                                   ST1074.2
007800     02 COMPUTED-X.                                               ST1074.2
007900     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          ST1074.2
008000     03 COMPUTED-N               REDEFINES COMPUTED-A             ST1074.2
008100                                 PIC -9(9).9(9).                  ST1074.2
008200     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         ST1074.2
008300     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     ST1074.2
008400     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     ST1074.2
008500     03       CM-18V0 REDEFINES COMPUTED-A.                       ST1074.2
008600         04 COMPUTED-18V0                    PIC -9(18).          ST1074.2
008700         04 FILLER                           PIC X.               ST1074.2
008800     03 FILLER PIC X(50) VALUE SPACE.                             ST1074.2
008900 01  TEST-CORRECT.                                                ST1074.2
009000     02 FILLER PIC X(30) VALUE SPACE.                             ST1074.2
009100     02 FILLER PIC X(17) VALUE "       CORRECT =".                ST1074.2
009200     02 CORRECT-X.                                                ST1074.2
009300     03 CORRECT-A                  PIC X(20) VALUE SPACE.         ST1074.2
009400     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      ST1074.2
009500     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         ST1074.2
009600     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     ST1074.2
009700     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     ST1074.2
009800     03      CR-18V0 REDEFINES CORRECT-A.                         ST1074.2
009900         04 CORRECT-18V0                     PIC -9(18).          ST1074.2
010000         04 FILLER                           PIC X.               ST1074.2
010100     03 FILLER PIC X(2) VALUE SPACE.                              ST1074.2
010200     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     ST1074.2
010300 01  CCVS-C-1.                                                    ST1074.2
010400     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAST1074.2
010500-    "SS  PARAGRAPH-NAME                                          ST1074.2
010600-    "       REMARKS".                                            ST1074.2
010700     02 FILLER                     PIC X(20)    VALUE SPACE.      ST1074.2
010800 01  CCVS-C-2.                                                    ST1074.2
010900     02 FILLER                     PIC X        VALUE SPACE.      ST1074.2
011000     02 FILLER                     PIC X(6)     VALUE "TESTED".   ST1074.2
011100     02 FILLER                     PIC X(15)    VALUE SPACE.      ST1074.2
011200     02 FILLER                     PIC X(4)     VALUE "FAIL".     ST1074.2
011300     02 FILLER                     PIC X(94)    VALUE SPACE.      ST1074.2
011400 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       ST1074.2
011500 01  REC-CT                        PIC 99       VALUE ZERO.       ST1074.2
011600 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       ST1074.2
011700 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       ST1074.2
011800 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       ST1074.2
011900 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       ST1074.2
012000 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       ST1074.2
012100 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       ST1074.2
012200 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      ST1074.2
012300 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       ST1074.2
012400 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     ST1074.2
012500 01  CCVS-H-1.                                                    ST1074.2
012600     02  FILLER                    PIC X(39)    VALUE SPACES.     ST1074.2
012700     02  FILLER                    PIC X(42)    VALUE             ST1074.2
012800     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 ST1074.2
012900     02  FILLER                    PIC X(39)    VALUE SPACES.     ST1074.2
013000 01  CCVS-H-2A.                                                   ST1074.2
013100   02  FILLER                        PIC X(40)  VALUE SPACE.      ST1074.2
013200   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  ST1074.2
013300   02  FILLER                        PIC XXXX   VALUE             ST1074.2
013400     "4.2 ".                                                      ST1074.2
013500   02  FILLER                        PIC X(28)  VALUE             ST1074.2
013600            " COPY - NOT FOR DISTRIBUTION".                       ST1074.2
013700   02  FILLER                        PIC X(41)  VALUE SPACE.      ST1074.2
013800                                                                  ST1074.2
013900 01  CCVS-H-2B.                                                   ST1074.2
014000   02  FILLER                        PIC X(15)  VALUE             ST1074.2
014100            "TEST RESULT OF ".                                    ST1074.2
014200   02  TEST-ID                       PIC X(9).                    ST1074.2
014300   02  FILLER                        PIC X(4)   VALUE             ST1074.2
014400            " IN ".                                               ST1074.2
014500   02  FILLER                        PIC X(12)  VALUE             ST1074.2
014600     " HIGH       ".                                              ST1074.2
014700   02  FILLER                        PIC X(22)  VALUE             ST1074.2
014800            " LEVEL VALIDATION FOR ".                             ST1074.2
014900   02  FILLER                        PIC X(58)  VALUE             ST1074.2
015000     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1074.2
015100 01  CCVS-H-3.                                                    ST1074.2
015200     02  FILLER                      PIC X(34)  VALUE             ST1074.2
015300            " FOR OFFICIAL USE ONLY    ".                         ST1074.2
015400     02  FILLER                      PIC X(58)  VALUE             ST1074.2
015500     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".ST1074.2
015600     02  FILLER                      PIC X(28)  VALUE             ST1074.2
015700            "  COPYRIGHT   1985 ".                                ST1074.2
015800 01  CCVS-E-1.                                                    ST1074.2
015900     02 FILLER                       PIC X(52)  VALUE SPACE.      ST1074.2
016000     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              ST1074.2
016100     02 ID-AGAIN                     PIC X(9).                    ST1074.2
016200     02 FILLER                       PIC X(45)  VALUE SPACES.     ST1074.2
016300 01  CCVS-E-2.                                                    ST1074.2
016400     02  FILLER                      PIC X(31)  VALUE SPACE.      ST1074.2
016500     02  FILLER                      PIC X(21)  VALUE SPACE.      ST1074.2
016600     02 CCVS-E-2-2.                                               ST1074.2
016700         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      ST1074.2
016800         03 FILLER                   PIC X      VALUE SPACE.      ST1074.2
016900         03 ENDER-DESC               PIC X(44)  VALUE             ST1074.2
017000            "ERRORS ENCOUNTERED".                                 ST1074.2
017100 01  CCVS-E-3.                                                    ST1074.2
017200     02  FILLER                      PIC X(22)  VALUE             ST1074.2
017300            " FOR OFFICIAL USE ONLY".                             ST1074.2
017400     02  FILLER                      PIC X(12)  VALUE SPACE.      ST1074.2
017500     02  FILLER                      PIC X(58)  VALUE             ST1074.2
017600     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1074.2
017700     02  FILLER                      PIC X(13)  VALUE SPACE.      ST1074.2
017800     02 FILLER                       PIC X(15)  VALUE             ST1074.2
017900             " COPYRIGHT 1985".                                   ST1074.2
018000 01  CCVS-E-4.                                                    ST1074.2
018100     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      ST1074.2
018200     02 FILLER                       PIC X(4)   VALUE " OF ".     ST1074.2
018300     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      ST1074.2
018400     02 FILLER                       PIC X(40)  VALUE             ST1074.2
018500      "  TESTS WERE EXECUTED SUCCESSFULLY".                       ST1074.2
018600 01  XXINFO.                                                      ST1074.2
018700     02 FILLER                       PIC X(19)  VALUE             ST1074.2
018800            "*** INFORMATION ***".                                ST1074.2
018900     02 INFO-TEXT.                                                ST1074.2
019000       04 FILLER                     PIC X(8)   VALUE SPACE.      ST1074.2
019100       04 XXCOMPUTED                 PIC X(20).                   ST1074.2
019200       04 FILLER                     PIC X(5)   VALUE SPACE.      ST1074.2
019300       04 XXCORRECT                  PIC X(20).                   ST1074.2
019400     02 INF-ANSI-REFERENCE           PIC X(48).                   ST1074.2
019500 01  HYPHEN-LINE.                                                 ST1074.2
019600     02 FILLER  PIC IS X VALUE IS SPACE.                          ST1074.2
019700     02 FILLER  PIC IS X(65)    VALUE IS "************************ST1074.2
019800-    "*****************************************".                 ST1074.2
019900     02 FILLER  PIC IS X(54)    VALUE IS "************************ST1074.2
020000-    "******************************".                            ST1074.2
020100 01  CCVS-PGM-ID                     PIC X(9)   VALUE             ST1074.2
020200     "ST107A".                                                    ST1074.2
020300 PROCEDURE DIVISION.                                              ST1074.2
020400 CCVS1 SECTION.                                                   ST1074.2
020500 OPEN-FILES.                                                      ST1074.2
020600     OPEN     OUTPUT PRINT-FILE.                                  ST1074.2
020700     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   ST1074.2
020800     MOVE    SPACE TO TEST-RESULTS.                               ST1074.2
020900     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             ST1074.2
021000     GO TO CCVS1-EXIT.                                            ST1074.2
021100 CLOSE-FILES.                                                     ST1074.2
021200     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   ST1074.2
021300 TERMINATE-CCVS.                                                  ST1074.2
021400*    EXIT PROGRAM.                                                ST1074.2
021500*TERMINATE-CALL.                                                  ST1074.2
021600     STOP     RUN.                                                ST1074.2
021700 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         ST1074.2
021800 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           ST1074.2
021900 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          ST1074.2
022000 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      ST1074.2
022100     MOVE "****TEST DELETED****" TO RE-MARK.                      ST1074.2
022200 PRINT-DETAIL.                                                    ST1074.2
022300     IF REC-CT NOT EQUAL TO ZERO                                  ST1074.2
022400             MOVE "." TO PARDOT-X                                 ST1074.2
022500             MOVE REC-CT TO DOTVALUE.                             ST1074.2
022600     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      ST1074.2
022700     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               ST1074.2
022800        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 ST1074.2
022900          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 ST1074.2
023000     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              ST1074.2
023100     MOVE SPACE TO CORRECT-X.                                     ST1074.2
023200     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         ST1074.2
023300     MOVE     SPACE TO RE-MARK.                                   ST1074.2
023400 HEAD-ROUTINE.                                                    ST1074.2
023500     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  ST1074.2
023600     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  ST1074.2
023700     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  ST1074.2
023800     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  ST1074.2
023900 COLUMN-NAMES-ROUTINE.                                            ST1074.2
024000     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           ST1074.2
024100     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1074.2
024200     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        ST1074.2
024300 END-ROUTINE.                                                     ST1074.2
024400     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.ST1074.2
024500 END-RTN-EXIT.                                                    ST1074.2
024600     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1074.2
024700 END-ROUTINE-1.                                                   ST1074.2
024800      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      ST1074.2
024900      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               ST1074.2
025000      ADD PASS-COUNTER TO ERROR-HOLD.                             ST1074.2
025100*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   ST1074.2
025200      MOVE PASS-COUNTER TO CCVS-E-4-1.                            ST1074.2
025300      MOVE ERROR-HOLD TO CCVS-E-4-2.                              ST1074.2
025400      MOVE CCVS-E-4 TO CCVS-E-2-2.                                ST1074.2
025500      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           ST1074.2
025600  END-ROUTINE-12.                                                 ST1074.2
025700      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        ST1074.2
025800     IF       ERROR-COUNTER IS EQUAL TO ZERO                      ST1074.2
025900         MOVE "NO " TO ERROR-TOTAL                                ST1074.2
026000         ELSE                                                     ST1074.2
026100         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       ST1074.2
026200     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           ST1074.2
026300     PERFORM WRITE-LINE.                                          ST1074.2
026400 END-ROUTINE-13.                                                  ST1074.2
026500     IF DELETE-COUNTER IS EQUAL TO ZERO                           ST1074.2
026600         MOVE "NO " TO ERROR-TOTAL  ELSE                          ST1074.2
026700         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      ST1074.2
026800     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   ST1074.2
026900     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           ST1074.2
027000      IF   INSPECT-COUNTER EQUAL TO ZERO                          ST1074.2
027100          MOVE "NO " TO ERROR-TOTAL                               ST1074.2
027200      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   ST1074.2
027300      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            ST1074.2
027400      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          ST1074.2
027500     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           ST1074.2
027600 WRITE-LINE.                                                      ST1074.2
027700     ADD 1 TO RECORD-COUNT.                                       ST1074.2
027800*    IF RECORD-COUNT GREATER 42                                   ST1074.2
027900*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          ST1074.2
028000*        MOVE SPACE TO DUMMY-RECORD                               ST1074.2
028100*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  ST1074.2
028200*        MOVE CCVS-H-1  TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    ST1074.2
028300*        MOVE CCVS-H-2A TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    ST1074.2
028400*        MOVE CCVS-H-2B TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    ST1074.2
028500*        MOVE CCVS-H-3  TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    ST1074.2
028600*        MOVE CCVS-C-1  TO DUMMY-RECORD PERFORM WRT-LN            ST1074.2
028700*        MOVE CCVS-C-2  TO DUMMY-RECORD PERFORM WRT-LN            ST1074.2
028800*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          ST1074.2
028900*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          ST1074.2
029000*        MOVE ZERO TO RECORD-COUNT.                               ST1074.2
029100     PERFORM WRT-LN.                                              ST1074.2
029200 WRT-LN.                                                          ST1074.2
029300     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               ST1074.2
029400     MOVE SPACE TO DUMMY-RECORD.                                  ST1074.2
029500 BLANK-LINE-PRINT.                                                ST1074.2
029600     PERFORM WRT-LN.                                              ST1074.2
029700 FAIL-ROUTINE.                                                    ST1074.2
029800     IF     COMPUTED-X NOT EQUAL TO SPACE                         ST1074.2
029900            GO TO   FAIL-ROUTINE-WRITE.                           ST1074.2
030000     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.ST1074.2
030100     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 ST1074.2
030200     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   ST1074.2
030300     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1074.2
030400     MOVE   SPACES TO INF-ANSI-REFERENCE.                         ST1074.2
030500     GO TO  FAIL-ROUTINE-EX.                                      ST1074.2
030600 FAIL-ROUTINE-WRITE.                                              ST1074.2
030700     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         ST1074.2
030800     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 ST1074.2
030900     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. ST1074.2
031000     MOVE   SPACES TO COR-ANSI-REFERENCE.                         ST1074.2
031100 FAIL-ROUTINE-EX. EXIT.                                           ST1074.2
031200 BAIL-OUT.                                                        ST1074.2
031300     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   ST1074.2
031400     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           ST1074.2
031500 BAIL-OUT-WRITE.                                                  ST1074.2
031600     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  ST1074.2
031700     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 ST1074.2
031800     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1074.2
031900     MOVE   SPACES TO INF-ANSI-REFERENCE.                         ST1074.2
032000 BAIL-OUT-EX. EXIT.                                               ST1074.2
032100 CCVS1-EXIT.                                                      ST1074.2
032200     EXIT.                                                        ST1074.2
032300 ST107-0001-01.                                                   ST1074.2
032400     OPEN    INPUT SORTIN-1G.                                     ST1074.2
032500     MOVE     "SORT, MIXED CLASSES" TO FEATURE.                   ST1074.2
032600     IF       ZER-O IS LESS THAN SPAC-E                           ST1074.2
032700              GO TO ZERO-IS-LESS-THAN-SPACE.                      ST1074.2
032800 SPACE-IS-LESS-THAN-ZERO SECTION.                                 ST1074.2
032900 SORT-INIT-A.                                                     ST1074.2
033000     MOVE     +0.0000001 TO UTILITY-1.                            ST1074.2
033100     MOVE     999999999999999999 TO UTILITY-2.                    ST1074.2
033200     MOVE     SPACE TO UTILITY-3.                                 ST1074.2
033300 SORT-TEST-1.                                                     ST1074.2
033400     PERFORM  READ-SORTIN.                                        ST1074.2
033500     MOVE     "SORT-TEST-1" TO PAR-NAME.                          ST1074.2
033600     IF       SORTIN-REC EQUAL TO UTILITY-KEYS                    ST1074.2
033700              PERFORM PASS GO TO SORT-WRITE-1.                    ST1074.2
033800 SORT-FAIL-1.                                                     ST1074.2
033900     MOVE     SORTKEY-3 TO COMPUTED-A.                            ST1074.2
034000     MOVE     UTILITY-3 TO CORRECT-A.                             ST1074.2
034100     MOVE     ITEM-3    TO RE-MARK.                               ST1074.2
034200     PERFORM  PRINT-DETAIL.                                       ST1074.2
034300     MOVE     SORTKEY-1 TO COMPUTED-4V14.                         ST1074.2
034400     MOVE     UTILITY-1 TO CORRECT-4V14.                          ST1074.2
034500     MOVE     ITEM-1    TO RE-MARK.                               ST1074.2
034600     PERFORM  PRINT-DETAIL.                                       ST1074.2
034700     MOVE     SORTKEY-2 TO COMPUTED-18V0.                         ST1074.2
034800     MOVE     UTILITY-2 TO CORRECT-18V0.                          ST1074.2
034900     MOVE     ITEM-2    TO RE-MARK.                               ST1074.2
035000     PERFORM  FAIL.                                               ST1074.2
035100 SORT-WRITE-1.                                                    ST1074.2
035200     PERFORM  PRINT-DETAIL.                                       ST1074.2
035300 SORT-INIT-B.                                                     ST1074.2
035400     MOVE     ZERO TO UTILITY-3.                                  ST1074.2
035500     PERFORM  READ-SORTIN 4 TIMES.                                ST1074.2
035600*        NOTE SORT-TEST-2 CHECKS THE SIXTH RECORD IN THE FILE.    ST1074.2
035700 SORT-TEST-2.                                                     ST1074.2
035800     PERFORM  READ-SORTIN.                                        ST1074.2
035900     MOVE     "SORT-TEST-2" TO PAR-NAME.                          ST1074.2
036000     IF       SORTIN-REC EQUAL TO UTILITY-KEYS                    ST1074.2
036100              PERFORM PASS GO TO SORT-WRITE-2.                    ST1074.2
036200 SORT-FAIL-2.                                                     ST1074.2
036300     MOVE     SORTKEY-3 TO COMPUTED-A.                            ST1074.2
036400     MOVE     UTILITY-3 TO CORRECT-A.                             ST1074.2
036500     MOVE     ITEM-3    TO RE-MARK.                               ST1074.2
036600     PERFORM  PRINT-DETAIL.                                       ST1074.2
036700     MOVE     SORTKEY-1 TO COMPUTED-4V14.                         ST1074.2
036800     MOVE     UTILITY-1 TO CORRECT-4V14.                          ST1074.2
036900     MOVE     ITEM-1    TO RE-MARK.                               ST1074.2
037000     PERFORM  PRINT-DETAIL.                                       ST1074.2
037100     MOVE     SORTKEY-2 TO COMPUTED-18V0.                         ST1074.2
037200     MOVE     UTILITY-2 TO CORRECT-18V0.                          ST1074.2
037300     MOVE     ITEM-2    TO RE-MARK.                               ST1074.2
037400     PERFORM  FAIL.                                               ST1074.2
037500 SORT-WRITE-2.                                                    ST1074.2
037600     PERFORM  PRINT-DETAIL.                                       ST1074.2
037700 DUMMY-3-AND-4.                                                   ST1074.2
037800     MOVE     "SORT-TEST-3" TO PAR-NAME.                          ST1074.2
037900     MOVE     DUM-MY TO RE-MARK.                                  ST1074.2
038000     PERFORM  PRINT-DETAIL.                                       ST1074.2
038100     MOVE     "SORT-TEST-4" TO PAR-NAME.                          ST1074.2
038200     MOVE     DUM-MY TO RE-MARK.                                  ST1074.2
038300     PERFORM  PRINT-DETAIL.                                       ST1074.2
038400     GO       TO CONTINUE-TESTING.                                ST1074.2
038500 ZERO-IS-LESS-THAN-SPACE SECTION.                                 ST1074.2
038600 SORT-INIT-C.                                                     ST1074.2
038700     MOVE     +0.0000001 TO UTILITY-1.                            ST1074.2
038800     MOVE     999999999999999999 TO UTILITY-2.                    ST1074.2
038900     MOVE     ZERO TO UTILITY-3.                                  ST1074.2
039000 DUMMY-1-AND-2.                                                   ST1074.2
039100     MOVE     "SORT-TEST-1" TO PAR-NAME.                          ST1074.2
039200     MOVE     DUM-MY TO RE-MARK.                                  ST1074.2
039300     PERFORM  PRINT-DETAIL.                                       ST1074.2
039400     MOVE     "SORT-TEST-2" TO PAR-NAME.                          ST1074.2
039500     MOVE     DUM-MY TO RE-MARK.                                  ST1074.2
039600     PERFORM  PRINT-DETAIL.                                       ST1074.2
039700 SORT-TEST-3.                                                     ST1074.2
039800     PERFORM  READ-SORTIN.                                        ST1074.2
039900     MOVE     "SORT-TEST-3" TO PAR-NAME.                          ST1074.2
040000     IF       SORTIN-REC EQUAL TO UTILITY-KEYS                    ST1074.2
040100              PERFORM PASS GO TO SORT-WRITE-3.                    ST1074.2
040200 SORT-FAIL-3.                                                     ST1074.2
040300     MOVE     SORTKEY-3 TO COMPUTED-A.                            ST1074.2
040400     MOVE     UTILITY-3 TO CORRECT-A.                             ST1074.2
040500     MOVE     ITEM-3    TO RE-MARK.                               ST1074.2
040600     PERFORM  PRINT-DETAIL.                                       ST1074.2
040700     MOVE     SORTKEY-1 TO COMPUTED-4V14.                         ST1074.2
040800     MOVE     UTILITY-1 TO CORRECT-4V14.                          ST1074.2
040900     MOVE     ITEM-1    TO RE-MARK.                               ST1074.2
041000     PERFORM  PRINT-DETAIL.                                       ST1074.2
041100     MOVE     SORTKEY-2 TO COMPUTED-18V0.                         ST1074.2
041200     MOVE     UTILITY-2 TO CORRECT-18V0.                          ST1074.2
041300     MOVE     ITEM-2    TO RE-MARK.                               ST1074.2
041400     PERFORM  FAIL.                                               ST1074.2
041500 SORT-WRITE-3.                                                    ST1074.2
041600     PERFORM  PRINT-DETAIL.                                       ST1074.2
041700 SORT-INIT-D.                                                     ST1074.2
041800     PERFORM  READ-SORTIN 4 TIMES.                                ST1074.2
041900     MOVE     SPACE TO UTILITY-3.                                 ST1074.2
042000*        NOTE SORT-TEST-4 CHECKS THE SIXTH RECORD IN THE FILE.    ST1074.2
042100 SORT-TEST-4.                                                     ST1074.2
042200     PERFORM  READ-SORTIN.                                        ST1074.2
042300     MOVE     "SORT-TEST-4" TO PAR-NAME.                          ST1074.2
042400     IF       SORTIN-REC EQUAL TO UTILITY-KEYS                    ST1074.2
042500              PERFORM PASS GO TO SORT-WRITE-4.                    ST1074.2
042600 SORT-FAIL-4.                                                     ST1074.2
042700     MOVE     SORTKEY-3 TO COMPUTED-A.                            ST1074.2
042800     MOVE     UTILITY-3 TO CORRECT-A.                             ST1074.2
042900     MOVE     ITEM-3    TO RE-MARK.                               ST1074.2
043000     PERFORM  PRINT-DETAIL.                                       ST1074.2
043100     MOVE     SORTKEY-1 TO COMPUTED-4V14.                         ST1074.2
043200     MOVE     UTILITY-1 TO CORRECT-4V14.                          ST1074.2
043300     MOVE     ITEM-1    TO RE-MARK.                               ST1074.2
043400     PERFORM  PRINT-DETAIL.                                       ST1074.2
043500     MOVE     SORTKEY-2 TO COMPUTED-18V0.                         ST1074.2
043600     MOVE     UTILITY-2 TO CORRECT-18V0.                          ST1074.2
043700     MOVE     ITEM-2    TO RE-MARK.                               ST1074.2
043800     PERFORM  FAIL.                                               ST1074.2
043900 SORT-WRITE-4.                                                    ST1074.2
044000     PERFORM  PRINT-DETAIL.                                       ST1074.2
044100 CONTINUE-TESTING SECTION.                                        ST1074.2
044200 SORT-INIT-E.                                                     ST1074.2
044300     MOVE     +1.1111112 TO UTILITY-1.                            ST1074.2
044400     MOVE     SPACE TO UTILITY-3.                                 ST1074.2
044500*        NOTE SORT-TEST-5 CHECKS THE SEVENTH RECORD IN THE FILE.  ST1074.2
044600 SORT-TEST-5.                                                     ST1074.2
044700     PERFORM  READ-SORTIN.                                        ST1074.2
044800     MOVE     "SORT-TEST-5" TO PAR-NAME.                          ST1074.2
044900     IF       SORTIN-REC EQUAL TO UTILITY-KEYS                    ST1074.2
045000              PERFORM PASS GO TO SORT-WRITE-5.                    ST1074.2
045100 SORT-FAIL-5.                                                     ST1074.2
045200     MOVE     SORTKEY-3 TO COMPUTED-A.                            ST1074.2
045300     MOVE     UTILITY-3 TO CORRECT-A.                             ST1074.2
045400     MOVE     ITEM-3    TO RE-MARK.                               ST1074.2
045500     PERFORM  PRINT-DETAIL.                                       ST1074.2
045600     MOVE     SORTKEY-1 TO COMPUTED-4V14.                         ST1074.2
045700     MOVE     UTILITY-1 TO CORRECT-4V14.                          ST1074.2
045800     MOVE     ITEM-1    TO RE-MARK.                               ST1074.2
045900     PERFORM  PRINT-DETAIL.                                       ST1074.2
046000     MOVE     SORTKEY-2 TO COMPUTED-18V0.                         ST1074.2
046100     MOVE     UTILITY-2 TO CORRECT-18V0.                          ST1074.2
046200     MOVE     ITEM-2    TO RE-MARK.                               ST1074.2
046300     PERFORM  FAIL.                                               ST1074.2
046400 SORT-WRITE-5.                                                    ST1074.2
046500     PERFORM  PRINT-DETAIL.                                       ST1074.2
046600 SORT-INIT-F.                                                     ST1074.2
046700     PERFORM  READ-SORTIN.                                        ST1074.2
046800     MOVE     888888888888888888 TO UTILITY-2.                    ST1074.2
046900*        NOTE SORT-TEST-6 CHECKS THE NINTH RECORD IN THE FILE.    ST1074.2
047000 SORT-TEST-6.                                                     ST1074.2
047100     PERFORM  READ-SORTIN.                                        ST1074.2
047200     MOVE     "SORT-TEST-6" TO PAR-NAME.                          ST1074.2
047300     IF       SORTIN-REC EQUAL TO UTILITY-KEYS                    ST1074.2
047400              PERFORM PASS GO TO SORT-WRITE-6.                    ST1074.2
047500 SORT-FAIL-6.                                                     ST1074.2
047600     MOVE     SORTKEY-3 TO COMPUTED-A.                            ST1074.2
047700     MOVE     UTILITY-3 TO CORRECT-A.                             ST1074.2
047800     MOVE     ITEM-3    TO RE-MARK.                               ST1074.2
047900     PERFORM  PRINT-DETAIL.                                       ST1074.2
048000     MOVE     SORTKEY-1 TO COMPUTED-4V14.                         ST1074.2
048100     MOVE     UTILITY-1 TO CORRECT-4V14.                          ST1074.2
048200     MOVE     ITEM-1    TO RE-MARK.                               ST1074.2
048300     PERFORM  PRINT-DETAIL.                                       ST1074.2
048400     MOVE     SORTKEY-2 TO COMPUTED-18V0.                         ST1074.2
048500     MOVE     UTILITY-2 TO CORRECT-18V0.                          ST1074.2
048600     MOVE     ITEM-2    TO RE-MARK.                               ST1074.2
048700     PERFORM  FAIL.                                               ST1074.2
048800 SORT-WRITE-6.                                                    ST1074.2
048900     PERFORM  PRINT-DETAIL.                                       ST1074.2
049000 SORT-TEST-7.                                                     ST1074.2
049100     READ     SORTIN-1G AT END                                    ST1074.2
049200              PERFORM PASS GO TO SORT-WRITE-7.                    ST1074.2
049300 SORT-FAIL-7.                                                     ST1074.2
049400     MOVE     SORTKEY-3 TO COMPUTED-A.                            ST1074.2
049500     MOVE     "END OF FILE NOT FOUND" TO RE-MARK.                 ST1074.2
049600     PERFORM  FAIL.                                               ST1074.2
049700 SORT-WRITE-7.                                                    ST1074.2
049800     MOVE     "SORT-TEST-7" TO PAR-NAME.                          ST1074.2
049900     PERFORM  PRINT-DETAIL.                                       ST1074.2
050000 SORT-TEST-8.                                                     ST1074.2
050100     IF       UTIL-CTR EQUAL TO 9                                 ST1074.2
050200              PERFORM PASS GO TO SORT-WRITE-8.                    ST1074.2
050300 SORT-FAIL-8.                                                     ST1074.2
050400     MOVE     UTIL-CTR TO COMPUTED-4V14.                          ST1074.2
050500     MOVE     9 TO CORRECT-4V14.                                  ST1074.2
050600     PERFORM  FAIL.                                               ST1074.2
050700 SORT-WRITE-8.                                                    ST1074.2
050800     MOVE     "SORT-TEST-8" TO PAR-NAME.                          ST1074.2
050900     PERFORM  PRINT-DETAIL.                                       ST1074.2
051000     CLOSE    SORTIN-1G.                                          ST1074.2
051100     GO TO    CCVS-EXIT.                                          ST1074.2
051200 READ-SORTIN.                                                     ST1074.2
051300     READ     SORTIN-1G AT END GO TO READ-ERROR.                  ST1074.2
051400     ADD      1 TO UTIL-CTR.                                      ST1074.2
051500 READ-ERROR.                                                      ST1074.2
051600     MOVE     "READ-ERROR" TO PAR-NAME.                           ST1074.2
051700     MOVE     "EOF PREMATURELY FOUND" TO RE-MARK.                 ST1074.2
051800     PERFORM  FAIL.                                               ST1074.2
051900     PERFORM  PRINT-DETAIL.                                       ST1074.2
052000 CCVS-EXIT SECTION.                                               ST1074.2
052100 CCVS-999999.                                                     ST1074.2
052200     GO TO CLOSE-FILES.                                           ST1074.2
      *END-OF,ST107A                                                                  
