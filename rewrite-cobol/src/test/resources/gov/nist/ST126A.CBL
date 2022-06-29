      *HEADER,COBOL,ST125A,SUBPRG,ST126A                                              
000100 IDENTIFICATION DIVISION.                                         ST1264.2
000200 PROGRAM-ID.                                                      ST1264.2
000300     ST126A.                                                      ST1264.2
000400****************************************************************  ST1264.2
000500*                                                              *  ST1264.2
000600*    VALIDATION FOR:-                                          *  ST1264.2
000700*                                                              *  ST1264.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1264.2
000900*                                                              *  ST1264.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".ST1264.2
001100*                                                              *  ST1264.2
001200****************************************************************  ST1264.2
001300*                                                              *  ST1264.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  ST1264.2
001500*                                                              *  ST1264.2
001600*        X-01                                                  *  ST1264.2
001700*        X-55  - SYSTEM PRINTER NAME.                          *  ST1264.2
001800*        X-69                                                  *  ST1264.2
001900*        X-74                                                  *  ST1264.2
002000*        X-75                                                  *  ST1264.2
002100*        X-82  - SOURCE COMPUTER NAME.                         *  ST1264.2
002200*        X-83  - OBJECT COMPUTER NAME.                         *  ST1264.2
002300*                                                              *  ST1264.2
002400****************************************************************  ST1264.2
002500*                                                              *  ST1264.2
002600*    PROGRAM ST126A VERIFIES THE CONTENT OF THE THREE FILES    *  ST1264.2
002700*    PRODUCED BY ST125A.                                       *  ST1264.2
002800*                                                              *  ST1264.2
002900****************************************************************  ST1264.2
003000 ENVIRONMENT DIVISION.                                            ST1264.2
003100 CONFIGURATION SECTION.                                           ST1264.2
003200 SOURCE-COMPUTER.                                                 ST1264.2
003300     XXXXX082.                                                    ST1264.2
003400 OBJECT-COMPUTER.                                                 ST1264.2
003500     XXXXX083.                                                    ST1264.2
003600 INPUT-OUTPUT SECTION.                                            ST1264.2
003700 FILE-CONTROL.                                                    ST1264.2
003800     SELECT PRINT-FILE ASSIGN TO                                  ST1264.2
003900     XXXXX055.                                                    ST1264.2
004000     SELECT   SORTIN-1G ASSIGN TO                                 ST1264.2
004100     XXXXD001.                                                    ST1264.2
004200     SELECT   SORTIN-2G ASSIGN TO                                 ST1264.2
004300     XXXXD002.                                                    ST1264.2
004400     SELECT   SORTIN-3G ASSIGN TO                                 ST1264.2
004500     XXXXD003.                                                    ST1264.2
004600 DATA DIVISION.                                                   ST1264.2
004700 FILE SECTION.                                                    ST1264.2
004800 FD  PRINT-FILE.                                                  ST1264.2
004900 01  PRINT-REC PICTURE X(120).                                    ST1264.2
005000 01  DUMMY-RECORD PICTURE X(120).                                 ST1264.2
005100 FD  SORTIN-1G                                                    ST1264.2
005200     LABEL RECORDS STANDARD                                       ST1264.2
005300C    VALUE OF                                                     ST1264.2
005400C    XXXXX074                                                     ST1264.2
005500C    IS                                                           ST1264.2
005600C    XXXXX075                                                     ST1264.2
005700*    XXXXX069                                                     ST1264.2
005800     RECORD   CONTAINS 27 CHARACTERS.                             ST1264.2
005900 01  SORTIN-REC-1.                                                ST1264.2
006000     02 SORTKEY-3-1     PICTURE X.                                ST1264.2
006100     02 SORTKEY-1-1     PICTURE S9V9(7).                          ST1264.2
006200     02 SORTKEY-2-1     PICTURE 9(18).                            ST1264.2
006300 FD  SORTIN-2G                                                    ST1264.2
006400     LABEL RECORDS STANDARD                                       ST1264.2
006500C    VALUE OF                                                     ST1264.2
006600C    XXXXX074                                                     ST1264.2
006700C    IS                                                           ST1264.2
006800C    XXXXX075                                                     ST1264.2
006900*    XXXXX069                                                     ST1264.2
007000     RECORD   CONTAINS 27 CHARACTERS.                             ST1264.2
007100 01  SORTIN-REC-2.                                                ST1264.2
007200     02 SORTKEY-3-2     PICTURE X.                                ST1264.2
007300     02 SORTKEY-1-2     PICTURE S9V9(7).                          ST1264.2
007400     02 SORTKEY-2-2     PICTURE 9(18).                            ST1264.2
007500 FD  SORTIN-3G                                                    ST1264.2
007600     LABEL RECORDS STANDARD                                       ST1264.2
007700C    VALUE OF                                                     ST1264.2
007800C    XXXXX074                                                     ST1264.2
007900C    IS                                                           ST1264.2
008000C    XXXXX075                                                     ST1264.2
008100*    XXXXX069                                                     ST1264.2
008200     RECORD   CONTAINS 27 CHARACTERS.                             ST1264.2
008300 01  SORTIN-REC-3.                                                ST1264.2
008400     02 SORTKEY-3-3     PICTURE X.                                ST1264.2
008500     02 SORTKEY-1-3     PICTURE S9V9(7).                          ST1264.2
008600     02 SORTKEY-2-3     PICTURE 9(18).                            ST1264.2
008700 WORKING-STORAGE SECTION.                                         ST1264.2
008800 77  UTIL-CTR           PICTURE S99999 VALUE ZERO.                ST1264.2
008900 77  ITEM-3   PICTURE X(27) VALUE "FIRST OF 3 ITEMS IN RECORD ".  ST1264.2
009000 77  ITEM-1   PICTURE X(27) VALUE "  SECOND OF 3 ITEMS        ".  ST1264.2
009100 77  ITEM-2   PICTURE X(27) VALUE "    THIRD OF 3 ITEMS       ".  ST1264.2
009200 77  DUM-MY   PICTURE X(27) VALUE "TEST UNNECESSARY - BYPASSED".  ST1264.2
009300 77  ZER-O              PICTURE X VALUE "0".                      ST1264.2
009400 77  SPAC-E             PICTURE X VALUE " ".                      ST1264.2
009500 01  UTILITY-KEYS.                                                ST1264.2
009600     02 UTILITY-3       PICTURE X.                                ST1264.2
009700     02 UTILITY-1       PICTURE S9V9(7).                          ST1264.2
009800     02 UTILITY-2       PICTURE 9(018).                           ST1264.2
009900 01  TEST-RESULTS.                                                ST1264.2
010000     02 FILLER                   PIC X      VALUE SPACE.          ST1264.2
010100     02 FEATURE                  PIC X(20)  VALUE SPACE.          ST1264.2
010200     02 FILLER                   PIC X      VALUE SPACE.          ST1264.2
010300     02 P-OR-F                   PIC X(5)   VALUE SPACE.          ST1264.2
010400     02 FILLER                   PIC X      VALUE SPACE.          ST1264.2
010500     02  PAR-NAME.                                                ST1264.2
010600       03 FILLER                 PIC X(19)  VALUE SPACE.          ST1264.2
010700       03  PARDOT-X              PIC X      VALUE SPACE.          ST1264.2
010800       03 DOTVALUE               PIC 99     VALUE ZERO.           ST1264.2
010900     02 FILLER                   PIC X(8)   VALUE SPACE.          ST1264.2
011000     02 RE-MARK                  PIC X(61).                       ST1264.2
011100 01  TEST-COMPUTED.                                               ST1264.2
011200     02 FILLER                   PIC X(30)  VALUE SPACE.          ST1264.2
011300     02 FILLER                   PIC X(17)  VALUE                 ST1264.2
011400            "       COMPUTED=".                                   ST1264.2
011500     02 COMPUTED-X.                                               ST1264.2
011600     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          ST1264.2
011700     03 COMPUTED-N               REDEFINES COMPUTED-A             ST1264.2
011800                                 PIC -9(9).9(9).                  ST1264.2
011900     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         ST1264.2
012000     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     ST1264.2
012100     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     ST1264.2
012200     03       CM-18V0 REDEFINES COMPUTED-A.                       ST1264.2
012300         04 COMPUTED-18V0                    PIC -9(18).          ST1264.2
012400         04 FILLER                           PIC X.               ST1264.2
012500     03 FILLER PIC X(50) VALUE SPACE.                             ST1264.2
012600 01  TEST-CORRECT.                                                ST1264.2
012700     02 FILLER PIC X(30) VALUE SPACE.                             ST1264.2
012800     02 FILLER PIC X(17) VALUE "       CORRECT =".                ST1264.2
012900     02 CORRECT-X.                                                ST1264.2
013000     03 CORRECT-A                  PIC X(20) VALUE SPACE.         ST1264.2
013100     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      ST1264.2
013200     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         ST1264.2
013300     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     ST1264.2
013400     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     ST1264.2
013500     03      CR-18V0 REDEFINES CORRECT-A.                         ST1264.2
013600         04 CORRECT-18V0                     PIC -9(18).          ST1264.2
013700         04 FILLER                           PIC X.               ST1264.2
013800     03 FILLER PIC X(2) VALUE SPACE.                              ST1264.2
013900     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     ST1264.2
014000 01  CCVS-C-1.                                                    ST1264.2
014100     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAST1264.2
014200-    "SS  PARAGRAPH-NAME                                          ST1264.2
014300-    "       REMARKS".                                            ST1264.2
014400     02 FILLER                     PIC X(20)    VALUE SPACE.      ST1264.2
014500 01  CCVS-C-2.                                                    ST1264.2
014600     02 FILLER                     PIC X        VALUE SPACE.      ST1264.2
014700     02 FILLER                     PIC X(6)     VALUE "TESTED".   ST1264.2
014800     02 FILLER                     PIC X(15)    VALUE SPACE.      ST1264.2
014900     02 FILLER                     PIC X(4)     VALUE "FAIL".     ST1264.2
015000     02 FILLER                     PIC X(94)    VALUE SPACE.      ST1264.2
015100 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       ST1264.2
015200 01  REC-CT                        PIC 99       VALUE ZERO.       ST1264.2
015300 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       ST1264.2
015400 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       ST1264.2
015500 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       ST1264.2
015600 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       ST1264.2
015700 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       ST1264.2
015800 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       ST1264.2
015900 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      ST1264.2
016000 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       ST1264.2
016100 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     ST1264.2
016200 01  CCVS-H-1.                                                    ST1264.2
016300     02  FILLER                    PIC X(39)    VALUE SPACES.     ST1264.2
016400     02  FILLER                    PIC X(42)    VALUE             ST1264.2
016500     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 ST1264.2
016600     02  FILLER                    PIC X(39)    VALUE SPACES.     ST1264.2
016700 01  CCVS-H-2A.                                                   ST1264.2
016800   02  FILLER                        PIC X(40)  VALUE SPACE.      ST1264.2
016900   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  ST1264.2
017000   02  FILLER                        PIC XXXX   VALUE             ST1264.2
017100     "4.2 ".                                                      ST1264.2
017200   02  FILLER                        PIC X(28)  VALUE             ST1264.2
017300            " COPY - NOT FOR DISTRIBUTION".                       ST1264.2
017400   02  FILLER                        PIC X(41)  VALUE SPACE.      ST1264.2
017500                                                                  ST1264.2
017600 01  CCVS-H-2B.                                                   ST1264.2
017700   02  FILLER                        PIC X(15)  VALUE             ST1264.2
017800            "TEST RESULT OF ".                                    ST1264.2
017900   02  TEST-ID                       PIC X(9).                    ST1264.2
018000   02  FILLER                        PIC X(4)   VALUE             ST1264.2
018100            " IN ".                                               ST1264.2
018200   02  FILLER                        PIC X(12)  VALUE             ST1264.2
018300     " HIGH       ".                                              ST1264.2
018400   02  FILLER                        PIC X(22)  VALUE             ST1264.2
018500            " LEVEL VALIDATION FOR ".                             ST1264.2
018600   02  FILLER                        PIC X(58)  VALUE             ST1264.2
018700     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1264.2
018800 01  CCVS-H-3.                                                    ST1264.2
018900     02  FILLER                      PIC X(34)  VALUE             ST1264.2
019000            " FOR OFFICIAL USE ONLY    ".                         ST1264.2
019100     02  FILLER                      PIC X(58)  VALUE             ST1264.2
019200     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".ST1264.2
019300     02  FILLER                      PIC X(28)  VALUE             ST1264.2
019400            "  COPYRIGHT   1985 ".                                ST1264.2
019500 01  CCVS-E-1.                                                    ST1264.2
019600     02 FILLER                       PIC X(52)  VALUE SPACE.      ST1264.2
019700     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              ST1264.2
019800     02 ID-AGAIN                     PIC X(9).                    ST1264.2
019900     02 FILLER                       PIC X(45)  VALUE SPACES.     ST1264.2
020000 01  CCVS-E-2.                                                    ST1264.2
020100     02  FILLER                      PIC X(31)  VALUE SPACE.      ST1264.2
020200     02  FILLER                      PIC X(21)  VALUE SPACE.      ST1264.2
020300     02 CCVS-E-2-2.                                               ST1264.2
020400         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      ST1264.2
020500         03 FILLER                   PIC X      VALUE SPACE.      ST1264.2
020600         03 ENDER-DESC               PIC X(44)  VALUE             ST1264.2
020700            "ERRORS ENCOUNTERED".                                 ST1264.2
020800 01  CCVS-E-3.                                                    ST1264.2
020900     02  FILLER                      PIC X(22)  VALUE             ST1264.2
021000            " FOR OFFICIAL USE ONLY".                             ST1264.2
021100     02  FILLER                      PIC X(12)  VALUE SPACE.      ST1264.2
021200     02  FILLER                      PIC X(58)  VALUE             ST1264.2
021300     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1264.2
021400     02  FILLER                      PIC X(13)  VALUE SPACE.      ST1264.2
021500     02 FILLER                       PIC X(15)  VALUE             ST1264.2
021600             " COPYRIGHT 1985".                                   ST1264.2
021700 01  CCVS-E-4.                                                    ST1264.2
021800     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      ST1264.2
021900     02 FILLER                       PIC X(4)   VALUE " OF ".     ST1264.2
022000     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      ST1264.2
022100     02 FILLER                       PIC X(40)  VALUE             ST1264.2
022200      "  TESTS WERE EXECUTED SUCCESSFULLY".                       ST1264.2
022300 01  XXINFO.                                                      ST1264.2
022400     02 FILLER                       PIC X(19)  VALUE             ST1264.2
022500            "*** INFORMATION ***".                                ST1264.2
022600     02 INFO-TEXT.                                                ST1264.2
022700       04 FILLER                     PIC X(8)   VALUE SPACE.      ST1264.2
022800       04 XXCOMPUTED                 PIC X(20).                   ST1264.2
022900       04 FILLER                     PIC X(5)   VALUE SPACE.      ST1264.2
023000       04 XXCORRECT                  PIC X(20).                   ST1264.2
023100     02 INF-ANSI-REFERENCE           PIC X(48).                   ST1264.2
023200 01  HYPHEN-LINE.                                                 ST1264.2
023300     02 FILLER  PIC IS X VALUE IS SPACE.                          ST1264.2
023400     02 FILLER  PIC IS X(65)    VALUE IS "************************ST1264.2
023500-    "*****************************************".                 ST1264.2
023600     02 FILLER  PIC IS X(54)    VALUE IS "************************ST1264.2
023700-    "******************************".                            ST1264.2
023800 01  CCVS-PGM-ID                     PIC X(9)   VALUE             ST1264.2
023900     "ST126A".                                                    ST1264.2
024000 PROCEDURE DIVISION.                                              ST1264.2
024100 CCVS1 SECTION.                                                   ST1264.2
024200 OPEN-FILES.                                                      ST1264.2
024300     OPEN     OUTPUT PRINT-FILE.                                  ST1264.2
024400     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   ST1264.2
024500     MOVE    SPACE TO TEST-RESULTS.                               ST1264.2
024600     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             ST1264.2
024700     GO TO CCVS1-EXIT.                                            ST1264.2
024800 CLOSE-FILES.                                                     ST1264.2
024900     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   ST1264.2
025000 TERMINATE-CCVS.                                                  ST1264.2
025100*    EXIT PROGRAM.                                                ST1264.2
025200*TERMINATE-CALL.                                                  ST1264.2
025300     STOP     RUN.                                                ST1264.2
025400 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         ST1264.2
025500 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           ST1264.2
025600 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          ST1264.2
025700 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      ST1264.2
025800     MOVE "****TEST DELETED****" TO RE-MARK.                      ST1264.2
025900 PRINT-DETAIL.                                                    ST1264.2
026000     IF REC-CT NOT EQUAL TO ZERO                                  ST1264.2
026100             MOVE "." TO PARDOT-X                                 ST1264.2
026200             MOVE REC-CT TO DOTVALUE.                             ST1264.2
026300     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      ST1264.2
026400     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               ST1264.2
026500        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 ST1264.2
026600          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 ST1264.2
026700     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              ST1264.2
026800     MOVE SPACE TO CORRECT-X.                                     ST1264.2
026900     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         ST1264.2
027000     MOVE     SPACE TO RE-MARK.                                   ST1264.2
027100 HEAD-ROUTINE.                                                    ST1264.2
027200     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  ST1264.2
027300     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  ST1264.2
027400     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  ST1264.2
027500     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  ST1264.2
027600 COLUMN-NAMES-ROUTINE.                                            ST1264.2
027700     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           ST1264.2
027800     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1264.2
027900     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        ST1264.2
028000 END-ROUTINE.                                                     ST1264.2
028100     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.ST1264.2
028200 END-RTN-EXIT.                                                    ST1264.2
028300     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1264.2
028400 END-ROUTINE-1.                                                   ST1264.2
028500      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      ST1264.2
028600      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               ST1264.2
028700      ADD PASS-COUNTER TO ERROR-HOLD.                             ST1264.2
028800*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   ST1264.2
028900      MOVE PASS-COUNTER TO CCVS-E-4-1.                            ST1264.2
029000      MOVE ERROR-HOLD TO CCVS-E-4-2.                              ST1264.2
029100      MOVE CCVS-E-4 TO CCVS-E-2-2.                                ST1264.2
029200      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           ST1264.2
029300  END-ROUTINE-12.                                                 ST1264.2
029400      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        ST1264.2
029500     IF       ERROR-COUNTER IS EQUAL TO ZERO                      ST1264.2
029600         MOVE "NO " TO ERROR-TOTAL                                ST1264.2
029700         ELSE                                                     ST1264.2
029800         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       ST1264.2
029900     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           ST1264.2
030000     PERFORM WRITE-LINE.                                          ST1264.2
030100 END-ROUTINE-13.                                                  ST1264.2
030200     IF DELETE-COUNTER IS EQUAL TO ZERO                           ST1264.2
030300         MOVE "NO " TO ERROR-TOTAL  ELSE                          ST1264.2
030400         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      ST1264.2
030500     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   ST1264.2
030600     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           ST1264.2
030700      IF   INSPECT-COUNTER EQUAL TO ZERO                          ST1264.2
030800          MOVE "NO " TO ERROR-TOTAL                               ST1264.2
030900      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   ST1264.2
031000      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            ST1264.2
031100      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          ST1264.2
031200     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           ST1264.2
031300 WRITE-LINE.                                                      ST1264.2
031400     ADD 1 TO RECORD-COUNT.                                       ST1264.2
031500*    IF RECORD-COUNT GREATER 42                                   ST1264.2
031600*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          ST1264.2
031700*        MOVE SPACE TO DUMMY-RECORD                               ST1264.2
031800*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  ST1264.2
031900*        MOVE CCVS-H-1  TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    ST1264.2
032000*        MOVE CCVS-H-2A TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    ST1264.2
032100*        MOVE CCVS-H-2B TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    ST1264.2
032200*        MOVE CCVS-H-3  TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    ST1264.2
032300*        MOVE CCVS-C-1  TO DUMMY-RECORD PERFORM WRT-LN            ST1264.2
032400*        MOVE CCVS-C-2  TO DUMMY-RECORD PERFORM WRT-LN            ST1264.2
032500*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          ST1264.2
032600*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          ST1264.2
032700*        MOVE ZERO TO RECORD-COUNT.                               ST1264.2
032800     PERFORM WRT-LN.                                              ST1264.2
032900 WRT-LN.                                                          ST1264.2
033000     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               ST1264.2
033100     MOVE SPACE TO DUMMY-RECORD.                                  ST1264.2
033200 BLANK-LINE-PRINT.                                                ST1264.2
033300     PERFORM WRT-LN.                                              ST1264.2
033400 FAIL-ROUTINE.                                                    ST1264.2
033500     IF     COMPUTED-X NOT EQUAL TO SPACE                         ST1264.2
033600            GO TO   FAIL-ROUTINE-WRITE.                           ST1264.2
033700     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.ST1264.2
033800     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 ST1264.2
033900     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   ST1264.2
034000     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1264.2
034100     MOVE   SPACES TO INF-ANSI-REFERENCE.                         ST1264.2
034200     GO TO  FAIL-ROUTINE-EX.                                      ST1264.2
034300 FAIL-ROUTINE-WRITE.                                              ST1264.2
034400     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         ST1264.2
034500     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 ST1264.2
034600     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. ST1264.2
034700     MOVE   SPACES TO COR-ANSI-REFERENCE.                         ST1264.2
034800 FAIL-ROUTINE-EX. EXIT.                                           ST1264.2
034900 BAIL-OUT.                                                        ST1264.2
035000     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   ST1264.2
035100     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           ST1264.2
035200 BAIL-OUT-WRITE.                                                  ST1264.2
035300     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  ST1264.2
035400     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 ST1264.2
035500     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1264.2
035600     MOVE   SPACES TO INF-ANSI-REFERENCE.                         ST1264.2
035700 BAIL-OUT-EX. EXIT.                                               ST1264.2
035800 CCVS1-EXIT.                                                      ST1264.2
035900     EXIT.                                                        ST1264.2
036000 ST126A-001-01.                                                   ST1264.2
036100     MOVE   "XI-20 4.4.4 GR(12)" TO ANSI-REFERENCE.               ST1264.2
036200     OPEN    INPUT SORTIN-1G.                                     ST1264.2
036300     OPEN    INPUT SORTIN-2G.                                     ST1264.2
036400     OPEN    INPUT SORTIN-3G.                                     ST1264.2
036500     MOVE     "SORT, MIXED CLASSES" TO FEATURE.                   ST1264.2
036600     IF       ZER-O IS LESS THAN SPAC-E                           ST1264.2
036700              GO TO ZERO-IS-LESS-THAN-SPACE.                      ST1264.2
036800 SPACE-IS-LESS-THAN-ZERO SECTION.                                 ST1264.2
036900 SORT-INIT-A.                                                     ST1264.2
037000     MOVE     +0.0000001 TO UTILITY-1.                            ST1264.2
037100     MOVE     999999999999999999 TO UTILITY-2.                    ST1264.2
037200     MOVE     SPACE TO UTILITY-3.                                 ST1264.2
037300 SORT-TEST-1.                                                     ST1264.2
037400     PERFORM  READ-SORTIN.                                        ST1264.2
037500     MOVE     "SORT-TEST-1" TO PAR-NAME.                          ST1264.2
037600     IF       SORTIN-REC-1 EQUAL TO UTILITY-KEYS                  ST1264.2
037700              PERFORM PASS GO TO SORT-WRITE-1.                    ST1264.2
037800 SORT-FAIL-1.                                                     ST1264.2
037900     MOVE     SORTKEY-3-1 TO COMPUTED-A.                          ST1264.2
038000     MOVE     UTILITY-3 TO CORRECT-A.                             ST1264.2
038100     MOVE     ITEM-3    TO RE-MARK.                               ST1264.2
038200     PERFORM  PRINT-DETAIL.                                       ST1264.2
038300     MOVE     SORTKEY-1-1 TO COMPUTED-4V14.                       ST1264.2
038400     MOVE     UTILITY-1 TO CORRECT-4V14.                          ST1264.2
038500     MOVE     ITEM-1    TO RE-MARK.                               ST1264.2
038600     PERFORM  PRINT-DETAIL.                                       ST1264.2
038700     MOVE     SORTKEY-2-1 TO COMPUTED-18V0.                       ST1264.2
038800     MOVE     UTILITY-2 TO CORRECT-18V0.                          ST1264.2
038900     MOVE     ITEM-2    TO RE-MARK.                               ST1264.2
039000     PERFORM  FAIL.                                               ST1264.2
039100 SORT-WRITE-1.                                                    ST1264.2
039200     PERFORM  PRINT-DETAIL.                                       ST1264.2
039300 SORT-INIT-B.                                                     ST1264.2
039400     MOVE     ZERO TO UTILITY-3.                                  ST1264.2
039500     PERFORM  READ-SORTIN 4 TIMES.                                ST1264.2
039600*        NOTE SORT-TEST-2 CHECKS THE SIXTH RECORD IN THE FILE.    ST1264.2
039700 SORT-TEST-2.                                                     ST1264.2
039800     PERFORM  READ-SORTIN.                                        ST1264.2
039900     MOVE     "SORT-TEST-2" TO PAR-NAME.                          ST1264.2
040000     IF       SORTIN-REC-1 EQUAL TO UTILITY-KEYS                  ST1264.2
040100              PERFORM PASS GO TO SORT-WRITE-2.                    ST1264.2
040200 SORT-FAIL-2.                                                     ST1264.2
040300     MOVE     SORTKEY-3-1 TO COMPUTED-A.                          ST1264.2
040400     MOVE     UTILITY-3 TO CORRECT-A.                             ST1264.2
040500     MOVE     ITEM-3    TO RE-MARK.                               ST1264.2
040600     PERFORM  PRINT-DETAIL.                                       ST1264.2
040700     MOVE     SORTKEY-1-1 TO COMPUTED-4V14.                       ST1264.2
040800     MOVE     UTILITY-1 TO CORRECT-4V14.                          ST1264.2
040900     MOVE     ITEM-1    TO RE-MARK.                               ST1264.2
041000     PERFORM  PRINT-DETAIL.                                       ST1264.2
041100     MOVE     SORTKEY-2-1 TO COMPUTED-18V0.                       ST1264.2
041200     MOVE     UTILITY-2 TO CORRECT-18V0.                          ST1264.2
041300     MOVE     ITEM-2    TO RE-MARK.                               ST1264.2
041400     PERFORM  FAIL.                                               ST1264.2
041500 SORT-WRITE-2.                                                    ST1264.2
041600     PERFORM  PRINT-DETAIL.                                       ST1264.2
041700 DUMMY-3-AND-4.                                                   ST1264.2
041800     MOVE     "SORT-TEST-3" TO PAR-NAME.                          ST1264.2
041900     MOVE     DUM-MY TO RE-MARK.                                  ST1264.2
042000     PERFORM  PRINT-DETAIL.                                       ST1264.2
042100     MOVE     "SORT-TEST-4" TO PAR-NAME.                          ST1264.2
042200     MOVE     DUM-MY TO RE-MARK.                                  ST1264.2
042300     PERFORM  PRINT-DETAIL.                                       ST1264.2
042400     GO       TO CONTINUE-TESTING.                                ST1264.2
042500 ZERO-IS-LESS-THAN-SPACE SECTION.                                 ST1264.2
042600 SORT-INIT-C.                                                     ST1264.2
042700     MOVE     +0.0000001 TO UTILITY-1.                            ST1264.2
042800     MOVE     999999999999999999 TO UTILITY-2.                    ST1264.2
042900     MOVE     ZERO TO UTILITY-3.                                  ST1264.2
043000 DUMMY-1-AND-2.                                                   ST1264.2
043100     MOVE     "SORT-TEST-1" TO PAR-NAME.                          ST1264.2
043200     MOVE     DUM-MY TO RE-MARK.                                  ST1264.2
043300     PERFORM  PRINT-DETAIL.                                       ST1264.2
043400     MOVE     "SORT-TEST-2" TO PAR-NAME.                          ST1264.2
043500     MOVE     DUM-MY TO RE-MARK.                                  ST1264.2
043600     PERFORM  PRINT-DETAIL.                                       ST1264.2
043700 SORT-TEST-3.                                                     ST1264.2
043800     PERFORM  READ-SORTIN.                                        ST1264.2
043900     MOVE     "SORT-TEST-3" TO PAR-NAME.                          ST1264.2
044000     IF       SORTIN-REC-1 EQUAL TO UTILITY-KEYS                  ST1264.2
044100              PERFORM PASS GO TO SORT-WRITE-3.                    ST1264.2
044200 SORT-FAIL-3.                                                     ST1264.2
044300     MOVE     SORTKEY-3-1 TO COMPUTED-A.                          ST1264.2
044400     MOVE     UTILITY-3 TO CORRECT-A.                             ST1264.2
044500     MOVE     ITEM-3    TO RE-MARK.                               ST1264.2
044600     PERFORM  PRINT-DETAIL.                                       ST1264.2
044700     MOVE     SORTKEY-1-1 TO COMPUTED-4V14.                       ST1264.2
044800     MOVE     UTILITY-1 TO CORRECT-4V14.                          ST1264.2
044900     MOVE     ITEM-1    TO RE-MARK.                               ST1264.2
045000     PERFORM  PRINT-DETAIL.                                       ST1264.2
045100     MOVE     SORTKEY-2-1 TO COMPUTED-18V0.                       ST1264.2
045200     MOVE     UTILITY-2 TO CORRECT-18V0.                          ST1264.2
045300     MOVE     ITEM-2    TO RE-MARK.                               ST1264.2
045400     PERFORM  FAIL.                                               ST1264.2
045500 SORT-WRITE-3.                                                    ST1264.2
045600     PERFORM  PRINT-DETAIL.                                       ST1264.2
045700 SORT-INIT-D.                                                     ST1264.2
045800     PERFORM  READ-SORTIN 4 TIMES.                                ST1264.2
045900     MOVE     SPACE TO UTILITY-3.                                 ST1264.2
046000*        NOTE SORT-TEST-4 CHECKS THE SIXTH RECORD IN THE FILE.    ST1264.2
046100 SORT-TEST-4.                                                     ST1264.2
046200     PERFORM  READ-SORTIN.                                        ST1264.2
046300     MOVE     "SORT-TEST-4" TO PAR-NAME.                          ST1264.2
046400     IF       SORTIN-REC-1 EQUAL TO UTILITY-KEYS                  ST1264.2
046500              PERFORM PASS GO TO SORT-WRITE-4.                    ST1264.2
046600 SORT-FAIL-4.                                                     ST1264.2
046700     MOVE     SORTKEY-3-1 TO COMPUTED-A.                          ST1264.2
046800     MOVE     UTILITY-3 TO CORRECT-A.                             ST1264.2
046900     MOVE     ITEM-3    TO RE-MARK.                               ST1264.2
047000     PERFORM  PRINT-DETAIL.                                       ST1264.2
047100     MOVE     SORTKEY-1-1 TO COMPUTED-4V14.                       ST1264.2
047200     MOVE     UTILITY-1 TO CORRECT-4V14.                          ST1264.2
047300     MOVE     ITEM-1    TO RE-MARK.                               ST1264.2
047400     PERFORM  PRINT-DETAIL.                                       ST1264.2
047500     MOVE     SORTKEY-2-1 TO COMPUTED-18V0.                       ST1264.2
047600     MOVE     UTILITY-2 TO CORRECT-18V0.                          ST1264.2
047700     MOVE     ITEM-2    TO RE-MARK.                               ST1264.2
047800     PERFORM  FAIL.                                               ST1264.2
047900 SORT-WRITE-4.                                                    ST1264.2
048000     PERFORM  PRINT-DETAIL.                                       ST1264.2
048100 CONTINUE-TESTING SECTION.                                        ST1264.2
048200 SORT-INIT-E.                                                     ST1264.2
048300     MOVE     +1.1111112 TO UTILITY-1.                            ST1264.2
048400     MOVE     SPACE TO UTILITY-3.                                 ST1264.2
048500*        NOTE SORT-TEST-5 CHECKS THE SEVENTH RECORD IN THE FILE.  ST1264.2
048600 SORT-TEST-5.                                                     ST1264.2
048700     PERFORM  READ-SORTIN.                                        ST1264.2
048800     MOVE     "SORT-TEST-5" TO PAR-NAME.                          ST1264.2
048900     IF       SORTIN-REC-1 EQUAL TO UTILITY-KEYS                  ST1264.2
049000              PERFORM PASS GO TO SORT-WRITE-5.                    ST1264.2
049100 SORT-FAIL-5.                                                     ST1264.2
049200     MOVE     SORTKEY-3-1 TO COMPUTED-A.                          ST1264.2
049300     MOVE     UTILITY-3 TO CORRECT-A.                             ST1264.2
049400     MOVE     ITEM-3    TO RE-MARK.                               ST1264.2
049500     PERFORM  PRINT-DETAIL.                                       ST1264.2
049600     MOVE     SORTKEY-1-1 TO COMPUTED-4V14.                       ST1264.2
049700     MOVE     UTILITY-1 TO CORRECT-4V14.                          ST1264.2
049800     MOVE     ITEM-1    TO RE-MARK.                               ST1264.2
049900     PERFORM  PRINT-DETAIL.                                       ST1264.2
050000     MOVE     SORTKEY-2-1 TO COMPUTED-18V0.                       ST1264.2
050100     MOVE     UTILITY-2 TO CORRECT-18V0.                          ST1264.2
050200     MOVE     ITEM-2    TO RE-MARK.                               ST1264.2
050300     PERFORM  FAIL.                                               ST1264.2
050400 SORT-WRITE-5.                                                    ST1264.2
050500     PERFORM  PRINT-DETAIL.                                       ST1264.2
050600 SORT-INIT-F.                                                     ST1264.2
050700     PERFORM  READ-SORTIN.                                        ST1264.2
050800     MOVE     888888888888888888 TO UTILITY-2.                    ST1264.2
050900*        NOTE SORT-TEST-6 CHECKS THE NINTH RECORD IN THE FILE.    ST1264.2
051000 SORT-TEST-6.                                                     ST1264.2
051100     PERFORM  READ-SORTIN.                                        ST1264.2
051200     MOVE     "SORT-TEST-6" TO PAR-NAME.                          ST1264.2
051300     IF       SORTIN-REC-1 EQUAL TO UTILITY-KEYS                  ST1264.2
051400              PERFORM PASS GO TO SORT-WRITE-6.                    ST1264.2
051500 SORT-FAIL-6.                                                     ST1264.2
051600     MOVE     SORTKEY-3-1 TO COMPUTED-A.                          ST1264.2
051700     MOVE     UTILITY-3 TO CORRECT-A.                             ST1264.2
051800     MOVE     ITEM-3    TO RE-MARK.                               ST1264.2
051900     PERFORM  PRINT-DETAIL.                                       ST1264.2
052000     MOVE     SORTKEY-1-1 TO COMPUTED-4V14.                       ST1264.2
052100     MOVE     UTILITY-1 TO CORRECT-4V14.                          ST1264.2
052200     MOVE     ITEM-1    TO RE-MARK.                               ST1264.2
052300     PERFORM  PRINT-DETAIL.                                       ST1264.2
052400     MOVE     SORTKEY-2-1 TO COMPUTED-18V0.                       ST1264.2
052500     MOVE     UTILITY-2 TO CORRECT-18V0.                          ST1264.2
052600     MOVE     ITEM-2    TO RE-MARK.                               ST1264.2
052700     PERFORM  FAIL.                                               ST1264.2
052800 SORT-WRITE-6.                                                    ST1264.2
052900     PERFORM  PRINT-DETAIL.                                       ST1264.2
053000 SORT-TEST-7.                                                     ST1264.2
053100     READ     SORTIN-1G AT END                                    ST1264.2
053200              PERFORM PASS GO TO SORT-WRITE-7.                    ST1264.2
053300 SORT-FAIL-7.                                                     ST1264.2
053400     MOVE     SORTKEY-3-1 TO COMPUTED-A.                          ST1264.2
053500     MOVE     "END OF FILE NOT FOUND" TO RE-MARK.                 ST1264.2
053600     PERFORM  FAIL.                                               ST1264.2
053700 SORT-WRITE-7.                                                    ST1264.2
053800     MOVE     "SORT-TEST-7" TO PAR-NAME.                          ST1264.2
053900     PERFORM  PRINT-DETAIL.                                       ST1264.2
054000 SORT-TEST-8.                                                     ST1264.2
054100     IF       UTIL-CTR EQUAL TO 9                                 ST1264.2
054200              PERFORM PASS GO TO SORT-WRITE-8.                    ST1264.2
054300 SORT-FAIL-8.                                                     ST1264.2
054400     MOVE     UTIL-CTR TO COMPUTED-4V14.                          ST1264.2
054500     MOVE     9 TO CORRECT-4V14.                                  ST1264.2
054600     PERFORM  FAIL.                                               ST1264.2
054700 SORT-WRITE-8.                                                    ST1264.2
054800     MOVE     "SORT-TEST-8" TO PAR-NAME.                          ST1264.2
054900     PERFORM  PRINT-DETAIL.                                       ST1264.2
055000     CLOSE    SORTIN-1G.                                          ST1264.2
055100     GO TO    ST126A-001-02.                                      ST1264.2
055200 READ-SORTIN.                                                     ST1264.2
055300     READ     SORTIN-1G AT END GO TO READ-ERROR.                  ST1264.2
055400     ADD      1 TO UTIL-CTR.                                      ST1264.2
055500 READ-ERROR.                                                      ST1264.2
055600     MOVE     "READ-ERROR" TO PAR-NAME.                           ST1264.2
055700     MOVE     "EOF PREMATURELY FOUND" TO RE-MARK.                 ST1264.2
055800     PERFORM  FAIL.                                               ST1264.2
055900     PERFORM  PRINT-DETAIL.                                       ST1264.2
056000*                                                                 ST1264.2
056100*                                                                 ST1264.2
056200 ST126A-001-02.                                                   ST1264.2
056300     MOVE     "SORT, MIXED CLASSES" TO FEATURE.                   ST1264.2
056400     MOVE     ZERO TO UTIL-CTR.                                   ST1264.2
056500     IF       ZER-O IS LESS THAN SPAC-E                           ST1264.2
056600              GO TO ZERO-IS-LESS-THAN-SPACE-2.                    ST1264.2
056700 SPACE-IS-LESS-THAN-ZERO-2 SECTION.                               ST1264.2
056800 SORT-INIT-A-2.                                                   ST1264.2
056900     MOVE     +0.0000001 TO UTILITY-1.                            ST1264.2
057000     MOVE     999999999999999999 TO UTILITY-2.                    ST1264.2
057100     MOVE     SPACE TO UTILITY-3.                                 ST1264.2
057200 SORT-TEST-1-2.                                                   ST1264.2
057300     PERFORM  READ-SORTIN-2.                                      ST1264.2
057400     MOVE     "SORT-TEST-1-2" TO PAR-NAME.                        ST1264.2
057500     IF       SORTIN-REC-2 EQUAL TO UTILITY-KEYS                  ST1264.2
057600              PERFORM PASS GO TO SORT-WRITE-1-2.                  ST1264.2
057700 SORT-FAIL-1-2.                                                   ST1264.2
057800     MOVE     SORTKEY-3-2 TO COMPUTED-A.                          ST1264.2
057900     MOVE     UTILITY-3 TO CORRECT-A.                             ST1264.2
058000     MOVE     ITEM-3    TO RE-MARK.                               ST1264.2
058100     PERFORM  PRINT-DETAIL.                                       ST1264.2
058200     MOVE     SORTKEY-1-2 TO COMPUTED-4V14.                       ST1264.2
058300     MOVE     UTILITY-1 TO CORRECT-4V14.                          ST1264.2
058400     MOVE     ITEM-1    TO RE-MARK.                               ST1264.2
058500     PERFORM  PRINT-DETAIL.                                       ST1264.2
058600     MOVE     SORTKEY-2-2 TO COMPUTED-18V0.                       ST1264.2
058700     MOVE     UTILITY-2 TO CORRECT-18V0.                          ST1264.2
058800     MOVE     ITEM-2    TO RE-MARK.                               ST1264.2
058900     PERFORM  FAIL.                                               ST1264.2
059000 SORT-WRITE-1-2.                                                  ST1264.2
059100     PERFORM  PRINT-DETAIL.                                       ST1264.2
059200 SORT-INIT-B-2.                                                   ST1264.2
059300     MOVE     ZERO TO UTILITY-3.                                  ST1264.2
059400     PERFORM  READ-SORTIN-2 4 TIMES.                              ST1264.2
059500*        NOTE SORT-TEST-2-2 CHECKS THE 6TH RECORD IN THE FILE.    ST1264.2
059600 SORT-TEST-2-2.                                                   ST1264.2
059700     PERFORM  READ-SORTIN-2.                                      ST1264.2
059800     MOVE     "SORT-TEST-2-2" TO PAR-NAME.                        ST1264.2
059900     IF       SORTIN-REC-2 EQUAL TO UTILITY-KEYS                  ST1264.2
060000              PERFORM PASS GO TO SORT-WRITE-2-2.                  ST1264.2
060100 SORT-FAIL-2-2.                                                   ST1264.2
060200     MOVE     SORTKEY-3-2 TO COMPUTED-A.                          ST1264.2
060300     MOVE     UTILITY-3 TO CORRECT-A.                             ST1264.2
060400     MOVE     ITEM-3    TO RE-MARK.                               ST1264.2
060500     PERFORM  PRINT-DETAIL.                                       ST1264.2
060600     MOVE     SORTKEY-1-2 TO COMPUTED-4V14.                       ST1264.2
060700     MOVE     UTILITY-1 TO CORRECT-4V14.                          ST1264.2
060800     MOVE     ITEM-1    TO RE-MARK.                               ST1264.2
060900     PERFORM  PRINT-DETAIL.                                       ST1264.2
061000     MOVE     SORTKEY-2-2 TO COMPUTED-18V0.                       ST1264.2
061100     MOVE     UTILITY-2 TO CORRECT-18V0.                          ST1264.2
061200     MOVE     ITEM-2    TO RE-MARK.                               ST1264.2
061300     PERFORM  FAIL.                                               ST1264.2
061400 SORT-WRITE-2-2.                                                  ST1264.2
061500     PERFORM  PRINT-DETAIL.                                       ST1264.2
061600 DUMMY-3-AND-4-2.                                                 ST1264.2
061700     MOVE     "SORT-TEST-3-2" TO PAR-NAME.                        ST1264.2
061800     MOVE     DUM-MY TO RE-MARK.                                  ST1264.2
061900     PERFORM  PRINT-DETAIL.                                       ST1264.2
062000     MOVE     "SORT-TEST-4-2" TO PAR-NAME.                        ST1264.2
062100     MOVE     DUM-MY TO RE-MARK.                                  ST1264.2
062200     PERFORM  PRINT-DETAIL.                                       ST1264.2
062300     GO       TO CONTINUE-TESTING-2.                              ST1264.2
062400 ZERO-IS-LESS-THAN-SPACE-2 SECTION.                               ST1264.2
062500 SORT-INIT-C-2.                                                   ST1264.2
062600     MOVE     +0.0000001 TO UTILITY-1.                            ST1264.2
062700     MOVE     999999999999999999 TO UTILITY-2.                    ST1264.2
062800     MOVE     ZERO TO UTILITY-3.                                  ST1264.2
062900 DUMMY-1-AND-2-2.                                                 ST1264.2
063000     MOVE     "SORT-TEST-1-2" TO PAR-NAME.                        ST1264.2
063100     MOVE     DUM-MY TO RE-MARK.                                  ST1264.2
063200     PERFORM  PRINT-DETAIL.                                       ST1264.2
063300     MOVE     "SORT-TEST-2-2" TO PAR-NAME.                        ST1264.2
063400     MOVE     DUM-MY TO RE-MARK.                                  ST1264.2
063500     PERFORM  PRINT-DETAIL.                                       ST1264.2
063600 SORT-TEST-3-2.                                                   ST1264.2
063700     PERFORM  READ-SORTIN-2.                                      ST1264.2
063800     MOVE     "SORT-TEST-3-2" TO PAR-NAME.                        ST1264.2
063900     IF       SORTIN-REC-2 EQUAL TO UTILITY-KEYS                  ST1264.2
064000              PERFORM PASS GO TO SORT-WRITE-3-2.                  ST1264.2
064100 SORT-FAIL-3-2.                                                   ST1264.2
064200     MOVE     SORTKEY-3-2 TO COMPUTED-A.                          ST1264.2
064300     MOVE     UTILITY-3 TO CORRECT-A.                             ST1264.2
064400     MOVE     ITEM-3    TO RE-MARK.                               ST1264.2
064500     PERFORM  PRINT-DETAIL.                                       ST1264.2
064600     MOVE     SORTKEY-1-2 TO COMPUTED-4V14.                       ST1264.2
064700     MOVE     UTILITY-1 TO CORRECT-4V14.                          ST1264.2
064800     MOVE     ITEM-1    TO RE-MARK.                               ST1264.2
064900     PERFORM  PRINT-DETAIL.                                       ST1264.2
065000     MOVE     SORTKEY-2-2 TO COMPUTED-18V0.                       ST1264.2
065100     MOVE     UTILITY-2 TO CORRECT-18V0.                          ST1264.2
065200     MOVE     ITEM-2    TO RE-MARK.                               ST1264.2
065300     PERFORM  FAIL.                                               ST1264.2
065400 SORT-WRITE-3-2.                                                  ST1264.2
065500     PERFORM  PRINT-DETAIL.                                       ST1264.2
065600 SORT-INIT-D-2.                                                   ST1264.2
065700     PERFORM  READ-SORTIN-2 4 TIMES.                              ST1264.2
065800     MOVE     SPACE TO UTILITY-3.                                 ST1264.2
065900*        NOTE SORT-TEST-4-2 CHECKS THE SIXTH RECORD IN THE FILE.  ST1264.2
066000 SORT-TEST-4-2.                                                   ST1264.2
066100     PERFORM  READ-SORTIN-2.                                      ST1264.2
066200     MOVE     "SORT-TEST-4-2" TO PAR-NAME.                        ST1264.2
066300     IF       SORTIN-REC-2 EQUAL TO UTILITY-KEYS                  ST1264.2
066400              PERFORM PASS GO TO SORT-WRITE-4-2.                  ST1264.2
066500 SORT-FAIL-4-2.                                                   ST1264.2
066600     MOVE     SORTKEY-3-2 TO COMPUTED-A.                          ST1264.2
066700     MOVE     UTILITY-3 TO CORRECT-A.                             ST1264.2
066800     MOVE     ITEM-3    TO RE-MARK.                               ST1264.2
066900     PERFORM  PRINT-DETAIL.                                       ST1264.2
067000     MOVE     SORTKEY-1-2 TO COMPUTED-4V14.                       ST1264.2
067100     MOVE     UTILITY-1 TO CORRECT-4V14.                          ST1264.2
067200     MOVE     ITEM-1    TO RE-MARK.                               ST1264.2
067300     PERFORM  PRINT-DETAIL.                                       ST1264.2
067400     MOVE     SORTKEY-2-2 TO COMPUTED-18V0.                       ST1264.2
067500     MOVE     UTILITY-2 TO CORRECT-18V0.                          ST1264.2
067600     MOVE     ITEM-2    TO RE-MARK.                               ST1264.2
067700     PERFORM  FAIL.                                               ST1264.2
067800 SORT-WRITE-4-2.                                                  ST1264.2
067900     PERFORM  PRINT-DETAIL.                                       ST1264.2
068000 CONTINUE-TESTING-2 SECTION.                                      ST1264.2
068100 SORT-INIT-E-2.                                                   ST1264.2
068200     MOVE     +1.1111112 TO UTILITY-1.                            ST1264.2
068300     MOVE     SPACE TO UTILITY-3.                                 ST1264.2
068400*        NOTE SORT-TEST-5-2 CHECKS THE 7TH RECORD IN THE FILE.    ST1264.2
068500 SORT-TEST-5-2.                                                   ST1264.2
068600     PERFORM  READ-SORTIN-2.                                      ST1264.2
068700     MOVE     "SORT-TEST-5-2" TO PAR-NAME.                        ST1264.2
068800     IF       SORTIN-REC-2 EQUAL TO UTILITY-KEYS                  ST1264.2
068900              PERFORM PASS GO TO SORT-WRITE-5-2.                  ST1264.2
069000 SORT-FAIL-5-2.                                                   ST1264.2
069100     MOVE     SORTKEY-3-2 TO COMPUTED-A.                          ST1264.2
069200     MOVE     UTILITY-3 TO CORRECT-A.                             ST1264.2
069300     MOVE     ITEM-3    TO RE-MARK.                               ST1264.2
069400     PERFORM  PRINT-DETAIL.                                       ST1264.2
069500     MOVE     SORTKEY-1-2 TO COMPUTED-4V14.                       ST1264.2
069600     MOVE     UTILITY-1 TO CORRECT-4V14.                          ST1264.2
069700     MOVE     ITEM-1    TO RE-MARK.                               ST1264.2
069800     PERFORM  PRINT-DETAIL.                                       ST1264.2
069900     MOVE     SORTKEY-2-2 TO COMPUTED-18V0.                       ST1264.2
070000     MOVE     UTILITY-2 TO CORRECT-18V0.                          ST1264.2
070100     MOVE     ITEM-2    TO RE-MARK.                               ST1264.2
070200     PERFORM  FAIL.                                               ST1264.2
070300 SORT-WRITE-5-2.                                                  ST1264.2
070400     PERFORM  PRINT-DETAIL.                                       ST1264.2
070500 SORT-INIT-F-2.                                                   ST1264.2
070600     PERFORM  READ-SORTIN-2.                                      ST1264.2
070700     MOVE     888888888888888888 TO UTILITY-2.                    ST1264.2
070800*        NOTE SORT-TEST-6-2 CHECKS THE 9TH RECORD IN THE FILE.    ST1264.2
070900 SORT-TEST-6-2.                                                   ST1264.2
071000     PERFORM  READ-SORTIN-2.                                      ST1264.2
071100     MOVE     "SORT-TEST-6-2" TO PAR-NAME.                        ST1264.2
071200     IF       SORTIN-REC-2 EQUAL TO UTILITY-KEYS                  ST1264.2
071300              PERFORM PASS GO TO SORT-WRITE-6-2.                  ST1264.2
071400 SORT-FAIL-6-2.                                                   ST1264.2
071500     MOVE     SORTKEY-3-2 TO COMPUTED-A.                          ST1264.2
071600     MOVE     UTILITY-3 TO CORRECT-A.                             ST1264.2
071700     MOVE     ITEM-3    TO RE-MARK.                               ST1264.2
071800     PERFORM  PRINT-DETAIL.                                       ST1264.2
071900     MOVE     SORTKEY-1-2 TO COMPUTED-4V14.                       ST1264.2
072000     MOVE     UTILITY-1 TO CORRECT-4V14.                          ST1264.2
072100     MOVE     ITEM-1    TO RE-MARK.                               ST1264.2
072200     PERFORM  PRINT-DETAIL.                                       ST1264.2
072300     MOVE     SORTKEY-2-2 TO COMPUTED-18V0.                       ST1264.2
072400     MOVE     UTILITY-2 TO CORRECT-18V0.                          ST1264.2
072500     MOVE     ITEM-2    TO RE-MARK.                               ST1264.2
072600     PERFORM  FAIL.                                               ST1264.2
072700 SORT-WRITE-6-2.                                                  ST1264.2
072800     PERFORM  PRINT-DETAIL.                                       ST1264.2
072900 SORT-TEST-7-2.                                                   ST1264.2
073000     READ     SORTIN-2G AT END                                    ST1264.2
073100              PERFORM PASS GO TO SORT-WRITE-7-2.                  ST1264.2
073200 SORT-FAIL-7-2.                                                   ST1264.2
073300     MOVE     SORTKEY-3-2 TO COMPUTED-A.                          ST1264.2
073400     MOVE     "END OF FILE NOT FOUND" TO RE-MARK.                 ST1264.2
073500     PERFORM  FAIL.                                               ST1264.2
073600 SORT-WRITE-7-2.                                                  ST1264.2
073700     MOVE     "SORT-TEST-7-2" TO PAR-NAME.                        ST1264.2
073800     PERFORM  PRINT-DETAIL.                                       ST1264.2
073900 SORT-TEST-8-2.                                                   ST1264.2
074000     IF       UTIL-CTR EQUAL TO 9                                 ST1264.2
074100              PERFORM PASS GO TO SORT-WRITE-8-2.                  ST1264.2
074200 SORT-FAIL-8-2.                                                   ST1264.2
074300     MOVE     UTIL-CTR TO COMPUTED-4V14.                          ST1264.2
074400     MOVE     9 TO CORRECT-4V14.                                  ST1264.2
074500     PERFORM  FAIL.                                               ST1264.2
074600 SORT-WRITE-8-2.                                                  ST1264.2
074700     MOVE     "SORT-TEST-8-2" TO PAR-NAME.                        ST1264.2
074800     PERFORM  PRINT-DETAIL.                                       ST1264.2
074900     CLOSE    SORTIN-2G.                                          ST1264.2
075000     GO TO    ST126A-001-03.                                      ST1264.2
075100 READ-SORTIN-2.                                                   ST1264.2
075200     READ     SORTIN-2G AT END GO TO READ-ERROR-2.                ST1264.2
075300     ADD      1 TO UTIL-CTR.                                      ST1264.2
075400 READ-ERROR-2.                                                    ST1264.2
075500     MOVE     "READ-ERROR-2" TO PAR-NAME.                         ST1264.2
075600     MOVE     "EOF PREMATURELY FOUND" TO RE-MARK.                 ST1264.2
075700     PERFORM  FAIL.                                               ST1264.2
075800     PERFORM  PRINT-DETAIL.                                       ST1264.2
075900*                                                                 ST1264.2
076000*                                                                 ST1264.2
076100 ST126A-001-03.                                                   ST1264.2
076200     MOVE    "SORT, MIXED CLASSES" TO FEATURE.                    ST1264.2
076300     MOVE     ZERO TO UTIL-CTR.                                   ST1264.2
076400     IF       ZER-O IS LESS THAN SPAC-E                           ST1264.2
076500              GO TO ZERO-IS-LESS-THAN-SPACE-3.                    ST1264.2
076600 SPACE-IS-LESS-THAN-ZERO-3 SECTION.                               ST1264.2
076700 SORT-INIT-A-3.                                                   ST1264.2
076800     MOVE     +0.0000001 TO UTILITY-1.                            ST1264.2
076900     MOVE     999999999999999999 TO UTILITY-2.                    ST1264.2
077000     MOVE     SPACE TO UTILITY-3.                                 ST1264.2
077100 SORT-TEST-1-3.                                                   ST1264.2
077200     PERFORM  READ-SORTIN-3.                                      ST1264.2
077300     MOVE     "SORT-TEST-1-3" TO PAR-NAME.                        ST1264.2
077400     IF       SORTIN-REC-3 EQUAL TO UTILITY-KEYS                  ST1264.2
077500              PERFORM PASS GO TO SORT-WRITE-1-3.                  ST1264.2
077600 SORT-FAIL-1-3.                                                   ST1264.2
077700     MOVE     SORTKEY-3-3 TO COMPUTED-A.                          ST1264.2
077800     MOVE     UTILITY-3 TO CORRECT-A.                             ST1264.2
077900     MOVE     ITEM-3    TO RE-MARK.                               ST1264.2
078000     PERFORM  PRINT-DETAIL.                                       ST1264.2
078100     MOVE     SORTKEY-1-3 TO COMPUTED-4V14.                       ST1264.2
078200     MOVE     UTILITY-1 TO CORRECT-4V14.                          ST1264.2
078300     MOVE     ITEM-1    TO RE-MARK.                               ST1264.2
078400     PERFORM  PRINT-DETAIL.                                       ST1264.2
078500     MOVE     SORTKEY-2-3 TO COMPUTED-18V0.                       ST1264.2
078600     MOVE     UTILITY-2 TO CORRECT-18V0.                          ST1264.2
078700     MOVE     ITEM-2    TO RE-MARK.                               ST1264.2
078800     PERFORM  FAIL.                                               ST1264.2
078900 SORT-WRITE-1-3.                                                  ST1264.2
079000     PERFORM  PRINT-DETAIL.                                       ST1264.2
079100 SORT-INIT-B-3.                                                   ST1264.2
079200     MOVE     ZERO TO UTILITY-3.                                  ST1264.2
079300     PERFORM  READ-SORTIN-3 4 TIMES.                              ST1264.2
079400*        NOTE SORT-TEST-2-3 CHECKS THE 6TH RECORD IN THE FILE.    ST1264.2
079500 SORT-TEST-2-3.                                                   ST1264.2
079600     PERFORM  READ-SORTIN-3.                                      ST1264.2
079700     MOVE     "SORT-TEST-2-3" TO PAR-NAME.                        ST1264.2
079800     IF       SORTIN-REC-3 EQUAL TO UTILITY-KEYS                  ST1264.2
079900              PERFORM PASS GO TO SORT-WRITE-2-3.                  ST1264.2
080000 SORT-FAIL-2-3.                                                   ST1264.2
080100     MOVE     SORTKEY-3-3 TO COMPUTED-A.                          ST1264.2
080200     MOVE     UTILITY-3 TO CORRECT-A.                             ST1264.2
080300     MOVE     ITEM-3    TO RE-MARK.                               ST1264.2
080400     PERFORM  PRINT-DETAIL.                                       ST1264.2
080500     MOVE     SORTKEY-1-3 TO COMPUTED-4V14.                       ST1264.2
080600     MOVE     UTILITY-1 TO CORRECT-4V14.                          ST1264.2
080700     MOVE     ITEM-1    TO RE-MARK.                               ST1264.2
080800     PERFORM  PRINT-DETAIL.                                       ST1264.2
080900     MOVE     SORTKEY-2-3 TO COMPUTED-18V0.                       ST1264.2
081000     MOVE     UTILITY-2 TO CORRECT-18V0.                          ST1264.2
081100     MOVE     ITEM-2    TO RE-MARK.                               ST1264.2
081200     PERFORM  FAIL.                                               ST1264.2
081300 SORT-WRITE-2-3.                                                  ST1264.2
081400     PERFORM  PRINT-DETAIL.                                       ST1264.2
081500 DUMMY-3-AND-4-3.                                                 ST1264.2
081600     MOVE     "SORT-TEST-3-3" TO PAR-NAME.                        ST1264.2
081700     MOVE     DUM-MY TO RE-MARK.                                  ST1264.2
081800     PERFORM  PRINT-DETAIL.                                       ST1264.2
081900     MOVE     "SORT-TEST-4-3" TO PAR-NAME.                        ST1264.2
082000     MOVE     DUM-MY TO RE-MARK.                                  ST1264.2
082100     PERFORM  PRINT-DETAIL.                                       ST1264.2
082200     GO       TO CONTINUE-TESTING-3.                              ST1264.2
082300 ZERO-IS-LESS-THAN-SPACE-3 SECTION.                               ST1264.2
082400 SORT-INIT-C-3.                                                   ST1264.2
082500     MOVE     +0.0000001 TO UTILITY-1.                            ST1264.2
082600     MOVE     999999999999999999 TO UTILITY-2.                    ST1264.2
082700     MOVE     ZERO TO UTILITY-3.                                  ST1264.2
082800 DUMMY-1-AND-2-3.                                                 ST1264.2
082900     MOVE     "SORT-TEST-1-3" TO PAR-NAME.                        ST1264.2
083000     MOVE     DUM-MY TO RE-MARK.                                  ST1264.2
083100     PERFORM  PRINT-DETAIL.                                       ST1264.2
083200     MOVE     "SORT-TEST-2-3" TO PAR-NAME.                        ST1264.2
083300     MOVE     DUM-MY TO RE-MARK.                                  ST1264.2
083400     PERFORM  PRINT-DETAIL.                                       ST1264.2
083500 SORT-TEST-3-3.                                                   ST1264.2
083600     PERFORM  READ-SORTIN-3.                                      ST1264.2
083700     MOVE     "SORT-TEST-3-3" TO PAR-NAME.                        ST1264.2
083800     IF       SORTIN-REC-3 EQUAL TO UTILITY-KEYS                  ST1264.2
083900              PERFORM PASS GO TO SORT-WRITE-3-3.                  ST1264.2
084000 SORT-FAIL-3-3.                                                   ST1264.2
084100     MOVE     SORTKEY-3-3 TO COMPUTED-A.                          ST1264.2
084200     MOVE     UTILITY-3 TO CORRECT-A.                             ST1264.2
084300     MOVE     ITEM-3    TO RE-MARK.                               ST1264.2
084400     PERFORM  PRINT-DETAIL.                                       ST1264.2
084500     MOVE     SORTKEY-1-3 TO COMPUTED-4V14.                       ST1264.2
084600     MOVE     UTILITY-1 TO CORRECT-4V14.                          ST1264.2
084700     MOVE     ITEM-1    TO RE-MARK.                               ST1264.2
084800     PERFORM  PRINT-DETAIL.                                       ST1264.2
084900     MOVE     SORTKEY-2-3 TO COMPUTED-18V0.                       ST1264.2
085000     MOVE     UTILITY-2 TO CORRECT-18V0.                          ST1264.2
085100     MOVE     ITEM-2    TO RE-MARK.                               ST1264.2
085200     PERFORM  FAIL.                                               ST1264.2
085300 SORT-WRITE-3-3.                                                  ST1264.2
085400     PERFORM  PRINT-DETAIL.                                       ST1264.2
085500 SORT-INIT-D-3.                                                   ST1264.2
085600     PERFORM  READ-SORTIN-3 4 TIMES.                              ST1264.2
085700     MOVE     SPACE TO UTILITY-3.                                 ST1264.2
085800*        NOTE SORT-TEST-4-3 CHECKS THE SIXTH RECORD IN THE FILE.  ST1264.2
085900 SORT-TEST-4-3.                                                   ST1264.2
086000     PERFORM  READ-SORTIN-3.                                      ST1264.2
086100     MOVE     "SORT-TEST-4-3" TO PAR-NAME.                        ST1264.2
086200     IF       SORTIN-REC-3 EQUAL TO UTILITY-KEYS                  ST1264.2
086300              PERFORM PASS GO TO SORT-WRITE-4-3.                  ST1264.2
086400 SORT-FAIL-4-3.                                                   ST1264.2
086500     MOVE     SORTKEY-3-3 TO COMPUTED-A.                          ST1264.2
086600     MOVE     UTILITY-3 TO CORRECT-A.                             ST1264.2
086700     MOVE     ITEM-3    TO RE-MARK.                               ST1264.2
086800     PERFORM  PRINT-DETAIL.                                       ST1264.2
086900     MOVE     SORTKEY-1-3 TO COMPUTED-4V14.                       ST1264.2
087000     MOVE     UTILITY-1 TO CORRECT-4V14.                          ST1264.2
087100     MOVE     ITEM-1    TO RE-MARK.                               ST1264.2
087200     PERFORM  PRINT-DETAIL.                                       ST1264.2
087300     MOVE     SORTKEY-2-3 TO COMPUTED-18V0.                       ST1264.2
087400     MOVE     UTILITY-2 TO CORRECT-18V0.                          ST1264.2
087500     MOVE     ITEM-2    TO RE-MARK.                               ST1264.2
087600     PERFORM  FAIL.                                               ST1264.2
087700 SORT-WRITE-4-3.                                                  ST1264.2
087800     PERFORM  PRINT-DETAIL.                                       ST1264.2
087900 CONTINUE-TESTING-3 SECTION.                                      ST1264.2
088000 SORT-INIT-E-3.                                                   ST1264.2
088100     MOVE     +1.1111112 TO UTILITY-1.                            ST1264.2
088200     MOVE     SPACE TO UTILITY-3.                                 ST1264.2
088300*        NOTE SORT-TEST-5-3 CHECKS THE 7TH RECORD IN THE FILE.    ST1264.2
088400 SORT-TEST-5-3.                                                   ST1264.2
088500     PERFORM  READ-SORTIN-3.                                      ST1264.2
088600     MOVE     "SORT-TEST-5-3" TO PAR-NAME.                        ST1264.2
088700     IF       SORTIN-REC-3 EQUAL TO UTILITY-KEYS                  ST1264.2
088800              PERFORM PASS GO TO SORT-WRITE-5-3.                  ST1264.2
088900 SORT-FAIL-5-3.                                                   ST1264.2
089000     MOVE     SORTKEY-3-3 TO COMPUTED-A.                          ST1264.2
089100     MOVE     UTILITY-3 TO CORRECT-A.                             ST1264.2
089200     MOVE     ITEM-3    TO RE-MARK.                               ST1264.2
089300     PERFORM  PRINT-DETAIL.                                       ST1264.2
089400     MOVE     SORTKEY-1-3 TO COMPUTED-4V14.                       ST1264.2
089500     MOVE     UTILITY-1 TO CORRECT-4V14.                          ST1264.2
089600     MOVE     ITEM-1    TO RE-MARK.                               ST1264.2
089700     PERFORM  PRINT-DETAIL.                                       ST1264.2
089800     MOVE     SORTKEY-2-3 TO COMPUTED-18V0.                       ST1264.2
089900     MOVE     UTILITY-2 TO CORRECT-18V0.                          ST1264.2
090000     MOVE     ITEM-2    TO RE-MARK.                               ST1264.2
090100     PERFORM  FAIL.                                               ST1264.2
090200 SORT-WRITE-5-3.                                                  ST1264.2
090300     PERFORM  PRINT-DETAIL.                                       ST1264.2
090400 SORT-INIT-F-3.                                                   ST1264.2
090500     PERFORM  READ-SORTIN-3.                                      ST1264.2
090600     MOVE     888888888888888888 TO UTILITY-2.                    ST1264.2
090700*        NOTE SORT-TEST-6-3 CHECKS THE 9TH RECORD IN THE FILE.    ST1264.2
090800 SORT-TEST-6-3.                                                   ST1264.2
090900     PERFORM  READ-SORTIN-3.                                      ST1264.2
091000     MOVE     "SORT-TEST-6-3" TO PAR-NAME.                        ST1264.2
091100     IF       SORTIN-REC-3 EQUAL TO UTILITY-KEYS                  ST1264.2
091200              PERFORM PASS GO TO SORT-WRITE-6-3.                  ST1264.2
091300 SORT-FAIL-6-3.                                                   ST1264.2
091400     MOVE     SORTKEY-3-3 TO COMPUTED-A.                          ST1264.2
091500     MOVE     UTILITY-3 TO CORRECT-A.                             ST1264.2
091600     MOVE     ITEM-3    TO RE-MARK.                               ST1264.2
091700     PERFORM  PRINT-DETAIL.                                       ST1264.2
091800     MOVE     SORTKEY-1-3 TO COMPUTED-4V14.                       ST1264.2
091900     MOVE     UTILITY-1 TO CORRECT-4V14.                          ST1264.2
092000     MOVE     ITEM-1    TO RE-MARK.                               ST1264.2
092100     PERFORM  PRINT-DETAIL.                                       ST1264.2
092200     MOVE     SORTKEY-2-3 TO COMPUTED-18V0.                       ST1264.2
092300     MOVE     UTILITY-3 TO CORRECT-18V0.                          ST1264.2
092400     MOVE     ITEM-2    TO RE-MARK.                               ST1264.2
092500     PERFORM  FAIL.                                               ST1264.2
092600 SORT-WRITE-6-3.                                                  ST1264.2
092700     PERFORM  PRINT-DETAIL.                                       ST1264.2
092800 SORT-TEST-7-3.                                                   ST1264.2
092900     READ     SORTIN-3G AT END                                    ST1264.2
093000              PERFORM PASS GO TO SORT-WRITE-7-3.                  ST1264.2
093100 SORT-FAIL-7-3.                                                   ST1264.2
093200     MOVE     SORTKEY-3-3 TO COMPUTED-A.                          ST1264.2
093300     MOVE     "END OF FILE NOT FOUND" TO RE-MARK.                 ST1264.2
093400     PERFORM  FAIL.                                               ST1264.2
093500 SORT-WRITE-7-3.                                                  ST1264.2
093600     MOVE     "SORT-TEST-7-3" TO PAR-NAME.                        ST1264.2
093700     PERFORM  PRINT-DETAIL.                                       ST1264.2
093800 SORT-TEST-8-3.                                                   ST1264.2
093900     IF       UTIL-CTR EQUAL TO 9                                 ST1264.2
094000              PERFORM PASS GO TO SORT-WRITE-8-3.                  ST1264.2
094100 SORT-FAIL-8-3.                                                   ST1264.2
094200     MOVE     UTIL-CTR TO COMPUTED-4V14.                          ST1264.2
094300     MOVE     9 TO CORRECT-4V14.                                  ST1264.2
094400     PERFORM  FAIL.                                               ST1264.2
094500 SORT-WRITE-8-3.                                                  ST1264.2
094600     MOVE     "SORT-TEST-8-3" TO PAR-NAME.                        ST1264.2
094700     PERFORM  PRINT-DETAIL.                                       ST1264.2
094800     CLOSE    SORTIN-3G.                                          ST1264.2
094900     GO TO    CCVS-EXIT.                                          ST1264.2
095000 READ-SORTIN-3.                                                   ST1264.2
095100     READ     SORTIN-3G AT END GO TO READ-ERROR-3.                ST1264.2
095200     ADD      1 TO UTIL-CTR.                                      ST1264.2
095300 READ-ERROR-3.                                                    ST1264.2
095400     MOVE     "READ-ERROR-3" TO PAR-NAME.                         ST1264.2
095500     MOVE     "EOF PREMATURELY FOUND" TO RE-MARK.                 ST1264.2
095600     PERFORM  FAIL.                                               ST1264.2
095700     PERFORM  PRINT-DETAIL.                                       ST1264.2
095800 CCVS-EXIT SECTION.                                               ST1264.2
095900 CCVS-999999.                                                     ST1264.2
096000     GO TO CLOSE-FILES.                                           ST1264.2
      *END-OF,ST126A                                                                  
