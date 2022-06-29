      *HEADER,COBOL,ST106A                                                            
000100 IDENTIFICATION DIVISION.                                         ST1064.2
000200 PROGRAM-ID.                                                      ST1064.2
000300     ST106A.                                                      ST1064.2
000400****************************************************************  ST1064.2
000500*                                                              *  ST1064.2
000600*    VALIDATION FOR:-                                          *  ST1064.2
000700*                                                              *  ST1064.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1064.2
000900*                                                              *  ST1064.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".ST1064.2
001100*                                                              *  ST1064.2
001200****************************************************************  ST1064.2
001300*                                                              *  ST1064.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  ST1064.2
001500*                                                              *  ST1064.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  ST1064.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  ST1064.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  ST1064.2
001900*                                                              *  ST1064.2
002000****************************************************************  ST1064.2
002100*        ) IS RIGHT PARENTHESIS                                   ST1064.2
002200*        ( IS LEFT PARENTHESIS                                    ST1064.2
002300*        " IS QUOTE                                               ST1064.2
002400*        + IS PLUS                                                ST1064.2
002500*                                                                 ST1064.2
002600*    THIS PROGRAM BUILDS A FILE OF NINE RECORDS. EACH RECORD HAS  ST1064.2
002700*    THREE KEYS, AND THE VALUES OF THE RECORDS ARE SHOWN BELOW-   ST1064.2
002800*             S                                                   ST1064.2
002900*             O                                                   ST1064.2
003000*             R               SORT                                ST1064.2
003100*             T  SORT         KEY                                 ST1064.2
003200*             K  KEY           -2                                 ST1064.2
003300*             E   -1           ..                                 ST1064.2
003400*             Y   ..         .    .                               ST1064.2
003500*             -  .  .      .        .                             ST1064.2
003600*             3 .    .   .            .                           ST1064.2
003700*             ..      ..                .                         ST1064.2
003800*              11111112888888888888888888                         ST1064.2
003900*              11111112999999999999999999                         ST1064.2
004000*              11111112999999999999999999                         ST1064.2
004100*              00000001999999999999999999                         ST1064.2
004200*             000000001999999999999999999                         ST1064.2
004300*             000000001999999999999999999                         ST1064.2
004400*             000000001999999999999999999                         ST1064.2
004500*             000000001999999999999999999                         ST1064.2
004600*             000000001999999999999999999                         ST1064.2
004700*    THERE IS AN ASSUMED DECIMAL POINT BETWEEN THE FIRST AND      ST1064.2
004800*    SECOND COLUMNS OF SORTKEY-1.                                 ST1064.2
004900*    THIS FILE IS BUILT AND SORTED BY THIS PROGRAM AND THE OUTPUT ST1064.2
005000*    IS PASSED ON TO ST107 FOR CHECKING.                          ST1064.2
005100                                                                  ST1064.2
005200 ENVIRONMENT DIVISION.                                            ST1064.2
005300 CONFIGURATION SECTION.                                           ST1064.2
005400 SOURCE-COMPUTER.                                                 ST1064.2
005500     XXXXX082.                                                    ST1064.2
005600 OBJECT-COMPUTER.                                                 ST1064.2
005700     XXXXX083.                                                    ST1064.2
005800 INPUT-OUTPUT SECTION.                                            ST1064.2
005900 FILE-CONTROL.                                                    ST1064.2
006000     SELECT PRINT-FILE ASSIGN TO                                  ST1064.2
006100     XXXXX055.                                                    ST1064.2
006200     SELECT SORTFILE-1F ASSIGN TO                                 ST1064.2
006300     XXXXX027.                                                    ST1064.2
006400     SELECT SORTOUT-1F ASSIGN TO                                  ST1064.2
006500     XXXXP001.                                                    ST1064.2
006600 DATA DIVISION.                                                   ST1064.2
006700 FILE SECTION.                                                    ST1064.2
006800 FD  PRINT-FILE.                                                  ST1064.2
006900 01  PRINT-REC PICTURE X(120).                                    ST1064.2
007000 01  DUMMY-RECORD PICTURE X(120).                                 ST1064.2
007100 FD  SORTOUT-1F                                                   ST1064.2
007200     LABEL RECORDS STANDARD                                       ST1064.2
007300C    VALUE OF                                                     ST1064.2
007400C    XXXXX074                                                     ST1064.2
007500C    IS                                                           ST1064.2
007600C    XXXXX075                                                     ST1064.2
007700*    XXXXX069                                                     ST1064.2
007800     RECORD CONTAINS 27 CHARACTERS                                ST1064.2
007900     DATA RECORD IS SORTOUT-REC.                                  ST1064.2
008000 01  SORTOUT-REC.                                                 ST1064.2
008100     02 FILLER          PICTURE X(27).                            ST1064.2
008200 SD  SORTFILE-1F                                                  ST1064.2
008300     RECORD  CONTAINS 27 CHARACTERS                               ST1064.2
008400     DATA RECORD IS SORT-GROUP.                                   ST1064.2
008500 01  SORT-GROUP.                                                  ST1064.2
008600     02 SORTKEY-3       PICTURE X.                                ST1064.2
008700     02 SORTKEY-1       PICTURE S9V9(7).                          ST1064.2
008800     02 SORTKEY-2       PICTURE 9(18).                            ST1064.2
008900 WORKING-STORAGE SECTION.                                         ST1064.2
009000 77  UTIL-CTR           PICTURE S99999 VALUE ZERO.                ST1064.2
009100 77  UTILITY-1          PICTURE S9V9(7) VALUE +1.1111112.         ST1064.2
009200 77  UTILITY-2          PICTURE 9(018) VALUE 888888888888888888.  ST1064.2
009300 77  UTILITY-3          PICTURE X VALUE SPACE.                    ST1064.2
009400 01  TEST-RESULTS.                                                ST1064.2
009500     02 FILLER                   PIC X      VALUE SPACE.          ST1064.2
009600     02 FEATURE                  PIC X(20)  VALUE SPACE.          ST1064.2
009700     02 FILLER                   PIC X      VALUE SPACE.          ST1064.2
009800     02 P-OR-F                   PIC X(5)   VALUE SPACE.          ST1064.2
009900     02 FILLER                   PIC X      VALUE SPACE.          ST1064.2
010000     02  PAR-NAME.                                                ST1064.2
010100       03 FILLER                 PIC X(19)  VALUE SPACE.          ST1064.2
010200       03  PARDOT-X              PIC X      VALUE SPACE.          ST1064.2
010300       03 DOTVALUE               PIC 99     VALUE ZERO.           ST1064.2
010400     02 FILLER                   PIC X(8)   VALUE SPACE.          ST1064.2
010500     02 RE-MARK                  PIC X(61).                       ST1064.2
010600 01  TEST-COMPUTED.                                               ST1064.2
010700     02 FILLER                   PIC X(30)  VALUE SPACE.          ST1064.2
010800     02 FILLER                   PIC X(17)  VALUE                 ST1064.2
010900            "       COMPUTED=".                                   ST1064.2
011000     02 COMPUTED-X.                                               ST1064.2
011100     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          ST1064.2
011200     03 COMPUTED-N               REDEFINES COMPUTED-A             ST1064.2
011300                                 PIC -9(9).9(9).                  ST1064.2
011400     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         ST1064.2
011500     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     ST1064.2
011600     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     ST1064.2
011700     03       CM-18V0 REDEFINES COMPUTED-A.                       ST1064.2
011800         04 COMPUTED-18V0                    PIC -9(18).          ST1064.2
011900         04 FILLER                           PIC X.               ST1064.2
012000     03 FILLER PIC X(50) VALUE SPACE.                             ST1064.2
012100 01  TEST-CORRECT.                                                ST1064.2
012200     02 FILLER PIC X(30) VALUE SPACE.                             ST1064.2
012300     02 FILLER PIC X(17) VALUE "       CORRECT =".                ST1064.2
012400     02 CORRECT-X.                                                ST1064.2
012500     03 CORRECT-A                  PIC X(20) VALUE SPACE.         ST1064.2
012600     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      ST1064.2
012700     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         ST1064.2
012800     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     ST1064.2
012900     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     ST1064.2
013000     03      CR-18V0 REDEFINES CORRECT-A.                         ST1064.2
013100         04 CORRECT-18V0                     PIC -9(18).          ST1064.2
013200         04 FILLER                           PIC X.               ST1064.2
013300     03 FILLER PIC X(2) VALUE SPACE.                              ST1064.2
013400     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     ST1064.2
013500 01  CCVS-C-1.                                                    ST1064.2
013600     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAST1064.2
013700-    "SS  PARAGRAPH-NAME                                          ST1064.2
013800-    "       REMARKS".                                            ST1064.2
013900     02 FILLER                     PIC X(20)    VALUE SPACE.      ST1064.2
014000 01  CCVS-C-2.                                                    ST1064.2
014100     02 FILLER                     PIC X        VALUE SPACE.      ST1064.2
014200     02 FILLER                     PIC X(6)     VALUE "TESTED".   ST1064.2
014300     02 FILLER                     PIC X(15)    VALUE SPACE.      ST1064.2
014400     02 FILLER                     PIC X(4)     VALUE "FAIL".     ST1064.2
014500     02 FILLER                     PIC X(94)    VALUE SPACE.      ST1064.2
014600 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       ST1064.2
014700 01  REC-CT                        PIC 99       VALUE ZERO.       ST1064.2
014800 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       ST1064.2
014900 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       ST1064.2
015000 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       ST1064.2
015100 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       ST1064.2
015200 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       ST1064.2
015300 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       ST1064.2
015400 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      ST1064.2
015500 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       ST1064.2
015600 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     ST1064.2
015700 01  CCVS-H-1.                                                    ST1064.2
015800     02  FILLER                    PIC X(39)    VALUE SPACES.     ST1064.2
015900     02  FILLER                    PIC X(42)    VALUE             ST1064.2
016000     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 ST1064.2
016100     02  FILLER                    PIC X(39)    VALUE SPACES.     ST1064.2
016200 01  CCVS-H-2A.                                                   ST1064.2
016300   02  FILLER                        PIC X(40)  VALUE SPACE.      ST1064.2
016400   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  ST1064.2
016500   02  FILLER                        PIC XXXX   VALUE             ST1064.2
016600     "4.2 ".                                                      ST1064.2
016700   02  FILLER                        PIC X(28)  VALUE             ST1064.2
016800            " COPY - NOT FOR DISTRIBUTION".                       ST1064.2
016900   02  FILLER                        PIC X(41)  VALUE SPACE.      ST1064.2
017000                                                                  ST1064.2
017100 01  CCVS-H-2B.                                                   ST1064.2
017200   02  FILLER                        PIC X(15)  VALUE             ST1064.2
017300            "TEST RESULT OF ".                                    ST1064.2
017400   02  TEST-ID                       PIC X(9).                    ST1064.2
017500   02  FILLER                        PIC X(4)   VALUE             ST1064.2
017600            " IN ".                                               ST1064.2
017700   02  FILLER                        PIC X(12)  VALUE             ST1064.2
017800     " HIGH       ".                                              ST1064.2
017900   02  FILLER                        PIC X(22)  VALUE             ST1064.2
018000            " LEVEL VALIDATION FOR ".                             ST1064.2
018100   02  FILLER                        PIC X(58)  VALUE             ST1064.2
018200     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1064.2
018300 01  CCVS-H-3.                                                    ST1064.2
018400     02  FILLER                      PIC X(34)  VALUE             ST1064.2
018500            " FOR OFFICIAL USE ONLY    ".                         ST1064.2
018600     02  FILLER                      PIC X(58)  VALUE             ST1064.2
018700     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".ST1064.2
018800     02  FILLER                      PIC X(28)  VALUE             ST1064.2
018900            "  COPYRIGHT   1985 ".                                ST1064.2
019000 01  CCVS-E-1.                                                    ST1064.2
019100     02 FILLER                       PIC X(52)  VALUE SPACE.      ST1064.2
019200     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              ST1064.2
019300     02 ID-AGAIN                     PIC X(9).                    ST1064.2
019400     02 FILLER                       PIC X(45)  VALUE SPACES.     ST1064.2
019500 01  CCVS-E-2.                                                    ST1064.2
019600     02  FILLER                      PIC X(31)  VALUE SPACE.      ST1064.2
019700     02  FILLER                      PIC X(21)  VALUE SPACE.      ST1064.2
019800     02 CCVS-E-2-2.                                               ST1064.2
019900         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      ST1064.2
020000         03 FILLER                   PIC X      VALUE SPACE.      ST1064.2
020100         03 ENDER-DESC               PIC X(44)  VALUE             ST1064.2
020200            "ERRORS ENCOUNTERED".                                 ST1064.2
020300 01  CCVS-E-3.                                                    ST1064.2
020400     02  FILLER                      PIC X(22)  VALUE             ST1064.2
020500            " FOR OFFICIAL USE ONLY".                             ST1064.2
020600     02  FILLER                      PIC X(12)  VALUE SPACE.      ST1064.2
020700     02  FILLER                      PIC X(58)  VALUE             ST1064.2
020800     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1064.2
020900     02  FILLER                      PIC X(13)  VALUE SPACE.      ST1064.2
021000     02 FILLER                       PIC X(15)  VALUE             ST1064.2
021100             " COPYRIGHT 1985".                                   ST1064.2
021200 01  CCVS-E-4.                                                    ST1064.2
021300     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      ST1064.2
021400     02 FILLER                       PIC X(4)   VALUE " OF ".     ST1064.2
021500     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      ST1064.2
021600     02 FILLER                       PIC X(40)  VALUE             ST1064.2
021700      "  TESTS WERE EXECUTED SUCCESSFULLY".                       ST1064.2
021800 01  XXINFO.                                                      ST1064.2
021900     02 FILLER                       PIC X(19)  VALUE             ST1064.2
022000            "*** INFORMATION ***".                                ST1064.2
022100     02 INFO-TEXT.                                                ST1064.2
022200       04 FILLER                     PIC X(8)   VALUE SPACE.      ST1064.2
022300       04 XXCOMPUTED                 PIC X(20).                   ST1064.2
022400       04 FILLER                     PIC X(5)   VALUE SPACE.      ST1064.2
022500       04 XXCORRECT                  PIC X(20).                   ST1064.2
022600     02 INF-ANSI-REFERENCE           PIC X(48).                   ST1064.2
022700 01  HYPHEN-LINE.                                                 ST1064.2
022800     02 FILLER  PIC IS X VALUE IS SPACE.                          ST1064.2
022900     02 FILLER  PIC IS X(65)    VALUE IS "************************ST1064.2
023000-    "*****************************************".                 ST1064.2
023100     02 FILLER  PIC IS X(54)    VALUE IS "************************ST1064.2
023200-    "******************************".                            ST1064.2
023300 01  CCVS-PGM-ID                     PIC X(9)   VALUE             ST1064.2
023400     "ST106A".                                                    ST1064.2
023500 PROCEDURE DIVISION.                                              ST1064.2
023600 SORTPARA SECTION.                                                ST1064.2
023700 SORT-PARAGRAPH.                                                  ST1064.2
023800     SORT     SORTFILE-1F ON                                      ST1064.2
023900              ASCENDING SORTKEY-1                                 ST1064.2
024000              DESCENDING SORTKEY-2                                ST1064.2
024100              ASCENDING SORTKEY-3                                 ST1064.2
024200              INPUT PROCEDURE INPROC THRU INPROC-EXIT             ST1064.2
024300              GIVING SORTOUT-1F.                                  ST1064.2
024400     STOP     RUN.                                                ST1064.2
024500 INPROC SECTION.                                                  ST1064.2
024600 OPEN-FILES.                                                      ST1064.2
024700     OPEN     OUTPUT PRINT-FILE.                                  ST1064.2
024800     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   ST1064.2
024900     MOVE    SPACE TO TEST-RESULTS.                               ST1064.2
025000     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             ST1064.2
025100     GO TO CCVS1-EXIT.                                            ST1064.2
025200 CLOSE-FILES.                                                     ST1064.2
025300     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   ST1064.2
025400 TERMINATE-CCVS.                                                  ST1064.2
025500*    EXIT PROGRAM.                                                ST1064.2
025600*TERMINATE-CALL.                                                  ST1064.2
025700     STOP     RUN.                                                ST1064.2
025800 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         ST1064.2
025900 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           ST1064.2
026000 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          ST1064.2
026100 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      ST1064.2
026200     MOVE "****TEST DELETED****" TO RE-MARK.                      ST1064.2
026300 PRINT-DETAIL.                                                    ST1064.2
026400     IF REC-CT NOT EQUAL TO ZERO                                  ST1064.2
026500             MOVE "." TO PARDOT-X                                 ST1064.2
026600             MOVE REC-CT TO DOTVALUE.                             ST1064.2
026700     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      ST1064.2
026800     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               ST1064.2
026900        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 ST1064.2
027000          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 ST1064.2
027100     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              ST1064.2
027200     MOVE SPACE TO CORRECT-X.                                     ST1064.2
027300     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         ST1064.2
027400     MOVE     SPACE TO RE-MARK.                                   ST1064.2
027500 HEAD-ROUTINE.                                                    ST1064.2
027600     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  ST1064.2
027700     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  ST1064.2
027800     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  ST1064.2
027900     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  ST1064.2
028000 COLUMN-NAMES-ROUTINE.                                            ST1064.2
028100     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           ST1064.2
028200     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1064.2
028300     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        ST1064.2
028400 END-ROUTINE.                                                     ST1064.2
028500     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.ST1064.2
028600 END-RTN-EXIT.                                                    ST1064.2
028700     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1064.2
028800 END-ROUTINE-1.                                                   ST1064.2
028900      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      ST1064.2
029000      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               ST1064.2
029100      ADD PASS-COUNTER TO ERROR-HOLD.                             ST1064.2
029200*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   ST1064.2
029300      MOVE PASS-COUNTER TO CCVS-E-4-1.                            ST1064.2
029400      MOVE ERROR-HOLD TO CCVS-E-4-2.                              ST1064.2
029500      MOVE CCVS-E-4 TO CCVS-E-2-2.                                ST1064.2
029600      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           ST1064.2
029700  END-ROUTINE-12.                                                 ST1064.2
029800      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        ST1064.2
029900     IF       ERROR-COUNTER IS EQUAL TO ZERO                      ST1064.2
030000         MOVE "NO " TO ERROR-TOTAL                                ST1064.2
030100         ELSE                                                     ST1064.2
030200         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       ST1064.2
030300     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           ST1064.2
030400     PERFORM WRITE-LINE.                                          ST1064.2
030500 END-ROUTINE-13.                                                  ST1064.2
030600     IF DELETE-COUNTER IS EQUAL TO ZERO                           ST1064.2
030700         MOVE "NO " TO ERROR-TOTAL  ELSE                          ST1064.2
030800         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      ST1064.2
030900     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   ST1064.2
031000     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           ST1064.2
031100      IF   INSPECT-COUNTER EQUAL TO ZERO                          ST1064.2
031200          MOVE "NO " TO ERROR-TOTAL                               ST1064.2
031300      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   ST1064.2
031400      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            ST1064.2
031500      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          ST1064.2
031600     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           ST1064.2
031700 WRITE-LINE.                                                      ST1064.2
031800     ADD 1 TO RECORD-COUNT.                                       ST1064.2
031900*    IF RECORD-COUNT GREATER 42                                   ST1064.2
032000*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          ST1064.2
032100*        MOVE SPACE TO DUMMY-RECORD                               ST1064.2
032200*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  ST1064.2
032300*        MOVE CCVS-H-1  TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    ST1064.2
032400*        MOVE CCVS-H-2A TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    ST1064.2
032500*        MOVE CCVS-H-2B TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    ST1064.2
032600*        MOVE CCVS-H-3  TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    ST1064.2
032700*        MOVE CCVS-C-1  TO DUMMY-RECORD PERFORM WRT-LN            ST1064.2
032800*        MOVE CCVS-C-2  TO DUMMY-RECORD PERFORM WRT-LN            ST1064.2
032900*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          ST1064.2
033000*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          ST1064.2
033100*        MOVE ZERO TO RECORD-COUNT.                               ST1064.2
033200     PERFORM WRT-LN.                                              ST1064.2
033300 WRT-LN.                                                          ST1064.2
033400     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               ST1064.2
033500     MOVE SPACE TO DUMMY-RECORD.                                  ST1064.2
033600 BLANK-LINE-PRINT.                                                ST1064.2
033700     PERFORM WRT-LN.                                              ST1064.2
033800 FAIL-ROUTINE.                                                    ST1064.2
033900     IF     COMPUTED-X NOT EQUAL TO SPACE                         ST1064.2
034000            GO TO   FAIL-ROUTINE-WRITE.                           ST1064.2
034100     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.ST1064.2
034200     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 ST1064.2
034300     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   ST1064.2
034400     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1064.2
034500     MOVE   SPACES TO INF-ANSI-REFERENCE.                         ST1064.2
034600     GO TO  FAIL-ROUTINE-EX.                                      ST1064.2
034700 FAIL-ROUTINE-WRITE.                                              ST1064.2
034800     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         ST1064.2
034900     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 ST1064.2
035000     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. ST1064.2
035100     MOVE   SPACES TO COR-ANSI-REFERENCE.                         ST1064.2
035200 FAIL-ROUTINE-EX. EXIT.                                           ST1064.2
035300 BAIL-OUT.                                                        ST1064.2
035400     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   ST1064.2
035500     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           ST1064.2
035600 BAIL-OUT-WRITE.                                                  ST1064.2
035700     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  ST1064.2
035800     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 ST1064.2
035900     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1064.2
036000     MOVE   SPACES TO INF-ANSI-REFERENCE.                         ST1064.2
036100 BAIL-OUT-EX. EXIT.                                               ST1064.2
036200 CCVS1-EXIT.                                                      ST1064.2
036300     EXIT.                                                        ST1064.2
036400 ST106-0001-01.                                                   ST1064.2
036500     OPEN     OUTPUT   SORTOUT-1F.                                ST1064.2
036600     MOVE     "THIS PROGRAM BUILDS AND" TO RE-MARK.               ST1064.2
036700     PERFORM  PRINT-DETAIL.                                       ST1064.2
036800     MOVE     "SORTS A FILE AND PASSES" TO RE-MARK.               ST1064.2
036900     PERFORM  PRINT-DETAIL.                                       ST1064.2
037000     MOVE      "THE OUTPUT TO ST107." TO RE-MARK.                 ST1064.2
037100     PERFORM  PRINT-DETAIL.                                       ST1064.2
037200 BUILD-FILE.                                                      ST1064.2
037300     ADD      1 TO UTIL-CTR                                       ST1064.2
037400     IF       UTIL-CTR EQUAL TO 2                                 ST1064.2
037500              MOVE 999999999999999999 TO UTILITY-2.               ST1064.2
037600     IF       UTIL-CTR EQUAL TO 4                                 ST1064.2
037700              ADD -1.1111111 TO UTILITY-1.                        ST1064.2
037800     IF       UTIL-CTR EQUAL TO 5                                 ST1064.2
037900              MOVE ZERO TO UTILITY-3.                             ST1064.2
038000     MOVE     UTILITY-1 TO SORTKEY-1.                             ST1064.2
038100     MOVE     UTILITY-3 TO SORTKEY-3.                             ST1064.2
038200     MOVE     UTILITY-2 TO SORTKEY-2.                             ST1064.2
038300     RELEASE  SORT-GROUP.                                         ST1064.2
038400     IF       UTIL-CTR LESS THAN 9 GO TO BUILD-FILE.              ST1064.2
038500 BUILD-FILE-TEST.                                                 ST1064.2
038600     IF       UTIL-CTR EQUAL TO 9                                 ST1064.2
038700              PERFORM PASS GO TO BUILD-FILE-WRITE.                ST1064.2
038800 BUILD-FILE-FAIL.                                                 ST1064.2
038900     MOVE     UTIL-CTR TO COMPUTED-N.                             ST1064.2
039000     MOVE     9 TO CORRECT-N.                                     ST1064.2
039100     PERFORM  FAIL.                                               ST1064.2
039200 BUILD-FILE-WRITE.                                                ST1064.2
039300     MOVE     "CREATE A FILE" TO FEATURE.                         ST1064.2
039400     MOVE     "BUILD-FILE-TEST" TO PAR-NAME.                      ST1064.2
039500     PERFORM  PRINT-DETAIL.                                       ST1064.2
039600     CLOSE    SORTOUT-1F.                                         ST1064.2
039700     GO       TO INPROC-EXIT.                                     ST1064.2
039800 INPROC-EXIT SECTION.                                             ST1064.2
039900 EXITPARA.                                                        ST1064.2
040000     PERFORM CLOSE-FILES.                                         ST1064.2
      *END-OF,ST106A                                                                  
