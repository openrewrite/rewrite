      *HEADER,COBOL,ST104A,SUBPRG,ST105A                                              
000100 IDENTIFICATION DIVISION.                                         ST1054.2
000200 PROGRAM-ID.                                                      ST1054.2
000300     ST105A.                                                      ST1054.2
000400****************************************************************  ST1054.2
000500*                                                              *  ST1054.2
000600*    VALIDATION FOR:-                                          *  ST1054.2
000700*                                                              *  ST1054.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1054.2
000900*                                                              *  ST1054.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".ST1054.2
001100*                                                              *  ST1054.2
001200****************************************************************  ST1054.2
001300*                                                              *  ST1054.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  ST1054.2
001500*                                                              *  ST1054.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  ST1054.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  ST1054.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  ST1054.2
001900*                                                              *  ST1054.2
002000****************************************************************  ST1054.2
002100*    THIS PROGRAM TESTS THE SORT WITH USING AND OUTPUT PROCEDURE. ST1054.2
002200*    SORTIN-1E, THE INPUT FILE, WAS CREATED IN ST104 EXPRESSLY FORST1054.2
002300*    USE IN THIS PROGRAM. EACH RECORD PASSED TO SORTOUT-1E, THE   ST1054.2
002400*    OUTPUT FILE, IS CHECKED BY THIS PROGRAM. SORTOUT-1E WILL NOT ST1054.2
002500*    BE USED BY ANY FURTHER PROGRAM.                              ST1054.2
002600*    RECORDS ARE RETURNED USING THE "RETURN INTO" PHRASE.         ST1054.2
002700*    SORTIN-1E CONTAINS 203 RECORDS, ARRANGED SO THAT THE KEYS    ST1054.2
002800*    START AT 000.000 AND RISE IN INCREMENTS OF 2.142 UNTIL THEY  ST1054.2
002900*    REACH 216.342, AND THEN DESCEND TO 000.000 IN THE SAME       ST1054.2
003000*    INCREMENTS. ALL RECORDS CONTAIN THE NUMBER +987654321078     ST1054.2
003100*    IN A NON-KEY AREA.                                           ST1054.2
003200*    SORTOUT-1E WILL BE SORTED IN DESCENDING ORDER. ALL RECORDS   ST1054.2
003300*    OCCUR IN IDENTICAL PAIRS EXCEPT THE FIRST ONE.               ST1054.2
003400*                                                                 ST1054.2
003500*    *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  * *.ST1054.2
003600                                                                  ST1054.2
003700 ENVIRONMENT DIVISION.                                            ST1054.2
003800 CONFIGURATION SECTION.                                           ST1054.2
003900 SOURCE-COMPUTER.                                                 ST1054.2
004000     XXXXX082.                                                    ST1054.2
004100 OBJECT-COMPUTER.                                                 ST1054.2
004200     XXXXX083.                                                    ST1054.2
004300 INPUT-OUTPUT SECTION.                                            ST1054.2
004400 FILE-CONTROL.                                                    ST1054.2
004500     SELECT PRINT-FILE ASSIGN TO                                  ST1054.2
004600     XXXXX055.                                                    ST1054.2
004700     SELECT   SORTIN-1E ASSIGN TO                                 ST1054.2
004800     XXXXD001.                                                    ST1054.2
004900     SELECT   SORTOUT-1E ASSIGN TO                                ST1054.2
005000     XXXXX002.                                                    ST1054.2
005100     SELECT   SORTFILE-1E ASSIGN TO                               ST1054.2
005200     XXXXX027.                                                    ST1054.2
005300 DATA DIVISION.                                                   ST1054.2
005400 FILE SECTION.                                                    ST1054.2
005500 FD  PRINT-FILE.                                                  ST1054.2
005600 01  PRINT-REC PICTURE X(120).                                    ST1054.2
005700 01  DUMMY-RECORD PICTURE X(120).                                 ST1054.2
005800 FD  SORTIN-1E                                                    ST1054.2
005900     LABEL RECORDS STANDARD                                       ST1054.2
006000C    VALUE OF                                                     ST1054.2
006100C    XXXXX074                                                     ST1054.2
006200C    IS                                                           ST1054.2
006300C    XXXXX075                                                     ST1054.2
006400*    XXXXX069                                                     ST1054.2
006500     DATA RECORD IS SORTIN-REC.                                   ST1054.2
006600 01  SORTIN-REC.                                                  ST1054.2
006700     02 FILLER          PICTURE X(18).                            ST1054.2
006800 FD  SORTOUT-1E                                                   ST1054.2
006900     LABEL RECORDS STANDARD                                       ST1054.2
007000C    VALUE OF                                                     ST1054.2
007100C    XXXXX074                                                     ST1054.2
007200C    IS                                                           ST1054.2
007300C    XXXXX076                                                     ST1054.2
007400*    XXXXX069                                                     ST1054.2
007500     DATA RECORD IS SORTOUT-REC.                                  ST1054.2
007600 01  SORTOUT-REC.                                                 ST1054.2
007700     02 FILLER          PICTURE X(18).                            ST1054.2
007800 SD  SORTFILE-1E                                                  ST1054.2
007900     DATA RECORD IS GRP-RECORD.                                   ST1054.2
008000 01  GRP-RECORD.                                                  ST1054.2
008100     02       KEY-ITEM            PICTURE S999V999.               ST1054.2
008200     02       NON-KEY-ITEM        PICTURE S9(12).                 ST1054.2
008300 WORKING-STORAGE SECTION.                                         ST1054.2
008400 77  U-TILITY          PICTURE S999V999 VALUE 216.342.            ST1054.2
008500 77  UTIL-SW            PICTURE 9 VALUE ZERO.                     ST1054.2
008600 77  RECORD-NUMBER      PICTURE 999 VALUE 203.                    ST1054.2
008700 77  WRITE-COUNTER      PICTURE 999         VALUE ZERO.           ST1054.2
008800 01  FEATURE-BUILDER.                                             ST1054.2
008900     02 NON             PICTURE X(4).                             ST1054.2
009000     02 FILLER          PICTURE X(13) VALUE "KEY-ITEM NO. ".      ST1054.2
009100     02 EDITED-NUMBER   PICTURE ZZ9.                              ST1054.2
009200 01  TEST-RESULTS.                                                ST1054.2
009300     02 FILLER                   PIC X      VALUE SPACE.          ST1054.2
009400     02 FEATURE                  PIC X(20)  VALUE SPACE.          ST1054.2
009500     02 FILLER                   PIC X      VALUE SPACE.          ST1054.2
009600     02 P-OR-F                   PIC X(5)   VALUE SPACE.          ST1054.2
009700     02 FILLER                   PIC X      VALUE SPACE.          ST1054.2
009800     02  PAR-NAME.                                                ST1054.2
009900       03 FILLER                 PIC X(19)  VALUE SPACE.          ST1054.2
010000       03  PARDOT-X              PIC X      VALUE SPACE.          ST1054.2
010100       03 DOTVALUE               PIC 99     VALUE ZERO.           ST1054.2
010200     02 FILLER                   PIC X(8)   VALUE SPACE.          ST1054.2
010300     02 RE-MARK                  PIC X(61).                       ST1054.2
010400 01  TEST-COMPUTED.                                               ST1054.2
010500     02 FILLER                   PIC X(30)  VALUE SPACE.          ST1054.2
010600     02 FILLER                   PIC X(17)  VALUE                 ST1054.2
010700            "       COMPUTED=".                                   ST1054.2
010800     02 COMPUTED-X.                                               ST1054.2
010900     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          ST1054.2
011000     03 COMPUTED-N               REDEFINES COMPUTED-A             ST1054.2
011100                                 PIC -9(9).9(9).                  ST1054.2
011200     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         ST1054.2
011300     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     ST1054.2
011400     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     ST1054.2
011500     03       CM-18V0 REDEFINES COMPUTED-A.                       ST1054.2
011600         04 COMPUTED-18V0                    PIC -9(18).          ST1054.2
011700         04 FILLER                           PIC X.               ST1054.2
011800     03 FILLER PIC X(50) VALUE SPACE.                             ST1054.2
011900 01  TEST-CORRECT.                                                ST1054.2
012000     02 FILLER PIC X(30) VALUE SPACE.                             ST1054.2
012100     02 FILLER PIC X(17) VALUE "       CORRECT =".                ST1054.2
012200     02 CORRECT-X.                                                ST1054.2
012300     03 CORRECT-A                  PIC X(20) VALUE SPACE.         ST1054.2
012400     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      ST1054.2
012500     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         ST1054.2
012600     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     ST1054.2
012700     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     ST1054.2
012800     03      CR-18V0 REDEFINES CORRECT-A.                         ST1054.2
012900         04 CORRECT-18V0                     PIC -9(18).          ST1054.2
013000         04 FILLER                           PIC X.               ST1054.2
013100     03 FILLER PIC X(2) VALUE SPACE.                              ST1054.2
013200     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     ST1054.2
013300 01  CCVS-C-1.                                                    ST1054.2
013400     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAST1054.2
013500-    "SS  PARAGRAPH-NAME                                          ST1054.2
013600-    "       REMARKS".                                            ST1054.2
013700     02 FILLER                     PIC X(20)    VALUE SPACE.      ST1054.2
013800 01  CCVS-C-2.                                                    ST1054.2
013900     02 FILLER                     PIC X        VALUE SPACE.      ST1054.2
014000     02 FILLER                     PIC X(6)     VALUE "TESTED".   ST1054.2
014100     02 FILLER                     PIC X(15)    VALUE SPACE.      ST1054.2
014200     02 FILLER                     PIC X(4)     VALUE "FAIL".     ST1054.2
014300     02 FILLER                     PIC X(94)    VALUE SPACE.      ST1054.2
014400 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       ST1054.2
014500 01  REC-CT                        PIC 99       VALUE ZERO.       ST1054.2
014600 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       ST1054.2
014700 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       ST1054.2
014800 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       ST1054.2
014900 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       ST1054.2
015000 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       ST1054.2
015100 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       ST1054.2
015200 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      ST1054.2
015300 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       ST1054.2
015400 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     ST1054.2
015500 01  CCVS-H-1.                                                    ST1054.2
015600     02  FILLER                    PIC X(39)    VALUE SPACES.     ST1054.2
015700     02  FILLER                    PIC X(42)    VALUE             ST1054.2
015800     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 ST1054.2
015900     02  FILLER                    PIC X(39)    VALUE SPACES.     ST1054.2
016000 01  CCVS-H-2A.                                                   ST1054.2
016100   02  FILLER                        PIC X(40)  VALUE SPACE.      ST1054.2
016200   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  ST1054.2
016300   02  FILLER                        PIC XXXX   VALUE             ST1054.2
016400     "4.2 ".                                                      ST1054.2
016500   02  FILLER                        PIC X(28)  VALUE             ST1054.2
016600            " COPY - NOT FOR DISTRIBUTION".                       ST1054.2
016700   02  FILLER                        PIC X(41)  VALUE SPACE.      ST1054.2
016800                                                                  ST1054.2
016900 01  CCVS-H-2B.                                                   ST1054.2
017000   02  FILLER                        PIC X(15)  VALUE             ST1054.2
017100            "TEST RESULT OF ".                                    ST1054.2
017200   02  TEST-ID                       PIC X(9).                    ST1054.2
017300   02  FILLER                        PIC X(4)   VALUE             ST1054.2
017400            " IN ".                                               ST1054.2
017500   02  FILLER                        PIC X(12)  VALUE             ST1054.2
017600     " HIGH       ".                                              ST1054.2
017700   02  FILLER                        PIC X(22)  VALUE             ST1054.2
017800            " LEVEL VALIDATION FOR ".                             ST1054.2
017900   02  FILLER                        PIC X(58)  VALUE             ST1054.2
018000     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1054.2
018100 01  CCVS-H-3.                                                    ST1054.2
018200     02  FILLER                      PIC X(34)  VALUE             ST1054.2
018300            " FOR OFFICIAL USE ONLY    ".                         ST1054.2
018400     02  FILLER                      PIC X(58)  VALUE             ST1054.2
018500     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".ST1054.2
018600     02  FILLER                      PIC X(28)  VALUE             ST1054.2
018700            "  COPYRIGHT   1985 ".                                ST1054.2
018800 01  CCVS-E-1.                                                    ST1054.2
018900     02 FILLER                       PIC X(52)  VALUE SPACE.      ST1054.2
019000     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              ST1054.2
019100     02 ID-AGAIN                     PIC X(9).                    ST1054.2
019200     02 FILLER                       PIC X(45)  VALUE SPACES.     ST1054.2
019300 01  CCVS-E-2.                                                    ST1054.2
019400     02  FILLER                      PIC X(31)  VALUE SPACE.      ST1054.2
019500     02  FILLER                      PIC X(21)  VALUE SPACE.      ST1054.2
019600     02 CCVS-E-2-2.                                               ST1054.2
019700         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      ST1054.2
019800         03 FILLER                   PIC X      VALUE SPACE.      ST1054.2
019900         03 ENDER-DESC               PIC X(44)  VALUE             ST1054.2
020000            "ERRORS ENCOUNTERED".                                 ST1054.2
020100 01  CCVS-E-3.                                                    ST1054.2
020200     02  FILLER                      PIC X(22)  VALUE             ST1054.2
020300            " FOR OFFICIAL USE ONLY".                             ST1054.2
020400     02  FILLER                      PIC X(12)  VALUE SPACE.      ST1054.2
020500     02  FILLER                      PIC X(58)  VALUE             ST1054.2
020600     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1054.2
020700     02  FILLER                      PIC X(13)  VALUE SPACE.      ST1054.2
020800     02 FILLER                       PIC X(15)  VALUE             ST1054.2
020900             " COPYRIGHT 1985".                                   ST1054.2
021000 01  CCVS-E-4.                                                    ST1054.2
021100     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      ST1054.2
021200     02 FILLER                       PIC X(4)   VALUE " OF ".     ST1054.2
021300     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      ST1054.2
021400     02 FILLER                       PIC X(40)  VALUE             ST1054.2
021500      "  TESTS WERE EXECUTED SUCCESSFULLY".                       ST1054.2
021600 01  XXINFO.                                                      ST1054.2
021700     02 FILLER                       PIC X(19)  VALUE             ST1054.2
021800            "*** INFORMATION ***".                                ST1054.2
021900     02 INFO-TEXT.                                                ST1054.2
022000       04 FILLER                     PIC X(8)   VALUE SPACE.      ST1054.2
022100       04 XXCOMPUTED                 PIC X(20).                   ST1054.2
022200       04 FILLER                     PIC X(5)   VALUE SPACE.      ST1054.2
022300       04 XXCORRECT                  PIC X(20).                   ST1054.2
022400     02 INF-ANSI-REFERENCE           PIC X(48).                   ST1054.2
022500 01  HYPHEN-LINE.                                                 ST1054.2
022600     02 FILLER  PIC IS X VALUE IS SPACE.                          ST1054.2
022700     02 FILLER  PIC IS X(65)    VALUE IS "************************ST1054.2
022800-    "*****************************************".                 ST1054.2
022900     02 FILLER  PIC IS X(54)    VALUE IS "************************ST1054.2
023000-    "******************************".                            ST1054.2
023100 01  CCVS-PGM-ID                     PIC X(9)   VALUE             ST1054.2
023200     "ST105A".                                                    ST1054.2
023300 PROCEDURE DIVISION.                                              ST1054.2
023400 SORTPARA SECTION.                                                ST1054.2
023500 SORT-PARAGRAPH.                                                  ST1054.2
023600     SORT     SORTFILE-1E ON                                      ST1054.2
023700              DESCENDING                                          ST1054.2
023800              KEY-ITEM                                            ST1054.2
023900              USING SORTIN-1E                                     ST1054.2
024000              OUTPUT PROCEDURE OUTPROC THRU OUTPROC-EXIT.         ST1054.2
024100     STOP     RUN.                                                ST1054.2
024200 OUTPROC SECTION.                                                 ST1054.2
024300 OPEN-FILES.                                                      ST1054.2
024400     OPEN     OUTPUT PRINT-FILE.                                  ST1054.2
024500     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   ST1054.2
024600     MOVE    SPACE TO TEST-RESULTS.                               ST1054.2
024700     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             ST1054.2
024800     GO TO CCVS1-EXIT.                                            ST1054.2
024900 CLOSE-FILES.                                                     ST1054.2
025000     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   ST1054.2
025100 TERMINATE-CCVS.                                                  ST1054.2
025200*    EXIT PROGRAM.                                                ST1054.2
025300*TERMINATE-CALL.                                                  ST1054.2
025400     STOP     RUN.                                                ST1054.2
025500 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         ST1054.2
025600 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           ST1054.2
025700 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          ST1054.2
025800 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      ST1054.2
025900     MOVE "****TEST DELETED****" TO RE-MARK.                      ST1054.2
026000 PRINT-DETAIL.                                                    ST1054.2
026100     IF REC-CT NOT EQUAL TO ZERO                                  ST1054.2
026200             MOVE "." TO PARDOT-X                                 ST1054.2
026300             MOVE REC-CT TO DOTVALUE.                             ST1054.2
026400     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      ST1054.2
026500     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               ST1054.2
026600        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 ST1054.2
026700          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 ST1054.2
026800     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              ST1054.2
026900     MOVE SPACE TO CORRECT-X.                                     ST1054.2
027000     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         ST1054.2
027100     MOVE     SPACE TO RE-MARK.                                   ST1054.2
027200 HEAD-ROUTINE.                                                    ST1054.2
027300     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  ST1054.2
027400     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  ST1054.2
027500     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  ST1054.2
027600     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  ST1054.2
027700 COLUMN-NAMES-ROUTINE.                                            ST1054.2
027800     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           ST1054.2
027900     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1054.2
028000     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        ST1054.2
028100 END-ROUTINE.                                                     ST1054.2
028200     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.ST1054.2
028300 END-RTN-EXIT.                                                    ST1054.2
028400     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1054.2
028500 END-ROUTINE-1.                                                   ST1054.2
028600      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      ST1054.2
028700      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               ST1054.2
028800      ADD PASS-COUNTER TO ERROR-HOLD.                             ST1054.2
028900*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   ST1054.2
029000      MOVE PASS-COUNTER TO CCVS-E-4-1.                            ST1054.2
029100      MOVE ERROR-HOLD TO CCVS-E-4-2.                              ST1054.2
029200      MOVE CCVS-E-4 TO CCVS-E-2-2.                                ST1054.2
029300      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           ST1054.2
029400  END-ROUTINE-12.                                                 ST1054.2
029500      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        ST1054.2
029600     IF       ERROR-COUNTER IS EQUAL TO ZERO                      ST1054.2
029700         MOVE "NO " TO ERROR-TOTAL                                ST1054.2
029800         ELSE                                                     ST1054.2
029900         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       ST1054.2
030000     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           ST1054.2
030100     PERFORM WRITE-LINE.                                          ST1054.2
030200 END-ROUTINE-13.                                                  ST1054.2
030300     IF DELETE-COUNTER IS EQUAL TO ZERO                           ST1054.2
030400         MOVE "NO " TO ERROR-TOTAL  ELSE                          ST1054.2
030500         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      ST1054.2
030600     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   ST1054.2
030700     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           ST1054.2
030800      IF   INSPECT-COUNTER EQUAL TO ZERO                          ST1054.2
030900          MOVE "NO " TO ERROR-TOTAL                               ST1054.2
031000      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   ST1054.2
031100      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            ST1054.2
031200      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          ST1054.2
031300     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           ST1054.2
031400 WRITE-LINE.                                                      ST1054.2
031500     ADD 1 TO RECORD-COUNT.                                       ST1054.2
031600*    IF RECORD-COUNT GREATER 42                                   ST1054.2
031700*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          ST1054.2
031800*        MOVE SPACE TO DUMMY-RECORD                               ST1054.2
031900*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  ST1054.2
032000*        MOVE CCVS-H-1  TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    ST1054.2
032100*        MOVE CCVS-H-2A TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    ST1054.2
032200*        MOVE CCVS-H-2B TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    ST1054.2
032300*        MOVE CCVS-H-3  TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    ST1054.2
032400*        MOVE CCVS-C-1  TO DUMMY-RECORD PERFORM WRT-LN            ST1054.2
032500*        MOVE CCVS-C-2  TO DUMMY-RECORD PERFORM WRT-LN            ST1054.2
032600*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          ST1054.2
032700*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          ST1054.2
032800*        MOVE ZERO TO RECORD-COUNT.                               ST1054.2
032900     PERFORM WRT-LN.                                              ST1054.2
033000 WRT-LN.                                                          ST1054.2
033100     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               ST1054.2
033200     MOVE SPACE TO DUMMY-RECORD.                                  ST1054.2
033300 BLANK-LINE-PRINT.                                                ST1054.2
033400     PERFORM WRT-LN.                                              ST1054.2
033500 FAIL-ROUTINE.                                                    ST1054.2
033600     IF     COMPUTED-X NOT EQUAL TO SPACE                         ST1054.2
033700            GO TO   FAIL-ROUTINE-WRITE.                           ST1054.2
033800     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.ST1054.2
033900     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 ST1054.2
034000     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   ST1054.2
034100     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1054.2
034200     MOVE   SPACES TO INF-ANSI-REFERENCE.                         ST1054.2
034300     GO TO  FAIL-ROUTINE-EX.                                      ST1054.2
034400 FAIL-ROUTINE-WRITE.                                              ST1054.2
034500     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         ST1054.2
034600     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 ST1054.2
034700     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. ST1054.2
034800     MOVE   SPACES TO COR-ANSI-REFERENCE.                         ST1054.2
034900 FAIL-ROUTINE-EX. EXIT.                                           ST1054.2
035000 BAIL-OUT.                                                        ST1054.2
035100     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   ST1054.2
035200     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           ST1054.2
035300 BAIL-OUT-WRITE.                                                  ST1054.2
035400     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  ST1054.2
035500     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 ST1054.2
035600     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1054.2
035700     MOVE   SPACES TO INF-ANSI-REFERENCE.                         ST1054.2
035800 BAIL-OUT-EX. EXIT.                                               ST1054.2
035900 CCVS1-EXIT.                                                      ST1054.2
036000     EXIT.                                                        ST1054.2
036100 ST105-0001-01.                                                   ST1054.2
036200     OPEN     OUTPUT      SORTOUT-1E.                             ST1054.2
036300     MOVE     "THIS PROGRAM CHECKS ALL" TO RE-MARK.               ST1054.2
036400     PERFORM  PRINT-DETAIL.                                       ST1054.2
036500     MOVE     "203 RECORDS, TWO ITEMS" TO RE-MARK.                ST1054.2
036600     PERFORM  PRINT-DETAIL.                                       ST1054.2
036700     MOVE     "PER RECORD." TO RE-MARK.                           ST1054.2
036800     PERFORM  PRINT-DETAIL.                                       ST1054.2
036900     MOVE     "SORT, USING-OUTPROC" TO FEATURE.                   ST1054.2
037000     PERFORM  PRINT-DETAIL.                                       ST1054.2
037100     MOVE     "SORT-TEST-1" TO PAR-NAME.                          ST1054.2
037200 SORT-TEST-1.                                                     ST1054.2
037300     RETURN   SORTFILE-1E INTO SORTOUT-REC                        ST1054.2
037400              AT END GO TO OUTPROC-EXIT.                          ST1054.2
037500     WRITE    SORTOUT-REC.                                        ST1054.2
037600     IF       NON-KEY-ITEM EQUAL TO +987654321078                 ST1054.2
037700              PERFORM PASS-1 ELSE PERFORM FAIL-1.                 ST1054.2
037800     IF       KEY-ITEM EQUAL TO U-TILITY                          ST1054.2
037900              PERFORM PASS-2 ELSE PERFORM FAIL-2.                 ST1054.2
038000     SUBTRACT 1 FROM RECORD-NUMBER.                               ST1054.2
038100     IF       U-TILITY GREATER THAN 214.200 GO TO NEW-PAIR.       ST1054.2
038200     IF       UTIL-SW EQUAL TO 1 GO TO NEW-PAIR.                  ST1054.2
038300 CONTINUE-PAIR.                                                   ST1054.2
038400     MOVE     1 TO UTIL-SW.                                       ST1054.2
038500     GO       TO SORT-TEST-1.                                     ST1054.2
038600 NEW-PAIR.                                                        ST1054.2
038700     MOVE     0 TO UTIL-SW.                                       ST1054.2
038800     SUBTRACT +002.142 FROM U-TILITY.                             ST1054.2
038900     GO       TO SORT-TEST-1.                                     ST1054.2
039000 SUP-PORT SECTION.                                                ST1054.2
039100 PASS-1.                                                          ST1054.2
039200     MOVE     "PASS" TO P-OR-F.                                   ST1054.2
039300     MOVE     "NON-" TO NON.                                      ST1054.2
039400     PERFORM  PRINT-FEATURE.                                      ST1054.2
039500     PERFORM   PRINT-DETAIL.                                      ST1054.2
039600 FAIL-1.                                                          ST1054.2
039700     MOVE     "FAIL" TO P-OR-F.                                   ST1054.2
039800     ADD      1 TO ERROR-COUNTER.                                 ST1054.2
039900     MOVE     "NON-" TO NON.                                      ST1054.2
040000     MOVE     NON-KEY-ITEM TO COMPUTED-18V0.                      ST1054.2
040100     MOVE     +987654321078 TO CORRECT-18V0.                      ST1054.2
040200     PERFORM  PRINT-FEATURE.                                      ST1054.2
040300     PERFORM   PRINT-DETAIL.                                      ST1054.2
040400 PASS-2.                                                          ST1054.2
040500     MOVE     SPACE TO NON.                                       ST1054.2
040600     MOVE     "PASS" TO P-OR-F.                                   ST1054.2
040700     PERFORM  PRINT-FEATURE.                                      ST1054.2
040800     PERFORM   PRINT-DETAIL.                                      ST1054.2
040900 FAIL-2.                                                          ST1054.2
041000     MOVE     SPACE TO NON.                                       ST1054.2
041100     MOVE     "FAIL" TO P-OR-F.                                   ST1054.2
041200     ADD      1 TO ERROR-COUNTER.                                 ST1054.2
041300     MOVE    KEY-ITEM TO COMPUTED-N.                              ST1054.2
041400     MOVE    U-TILITY TO CORRECT-N.                               ST1054.2
041500     PERFORM  PRINT-FEATURE.                                      ST1054.2
041600     PERFORM   PRINT-DETAIL.                                      ST1054.2
041700 PRINT-FEATURE.                                                   ST1054.2
041800     MOVE     RECORD-NUMBER TO EDITED-NUMBER.                     ST1054.2
041900     MOVE     FEATURE-BUILDER TO FEATURE.                         ST1054.2
042000 OUTPROC-EXIT SECTION.                                            ST1054.2
042100 SORT-INIT-A.                                                     ST1054.2
042200     MOVE     "LAST SORTED RECORD" TO FEATURE.                    ST1054.2
042300 SORT-TEST-2.                                                     ST1054.2
042400     IF       U-TILITY  EQUAL TO -002.142                         ST1054.2
042500              PERFORM PASS GO TO SORT-WRITE-2.                    ST1054.2
042600 SORT-FAIL-2.                                                     ST1054.2
042700     MOVE     U-TILITY TO COMPUTED-N.                             ST1054.2
042800     MOVE     -002.142 TO CORRECT-N.                              ST1054.2
042900     PERFORM  FAIL.                                               ST1054.2
043000 SORT-WRITE-2.                                                    ST1054.2
043100     MOVE     "SORT-TEST-2" TO PAR-NAME.                          ST1054.2
043200     PERFORM  PRINT-DETAIL.                                       ST1054.2
043300 SORT-TEST-3.                                                     ST1054.2
043400     IF       UTIL-SW EQUAL TO ZERO                               ST1054.2
043500              PERFORM PASS GO TO SORT-WRITE-3.                    ST1054.2
043600 SORT-FAIL-3.                                                     ST1054.2
043700     MOVE     UTIL-SW TO COMPUTED-N.                              ST1054.2
043800     MOVE     ZERO TO CORRECT-N.                                  ST1054.2
043900     MOVE     "LAST RECORDS NOT IN PAIRS" TO RE-MARK.             ST1054.2
044000     PERFORM  FAIL.                                               ST1054.2
044100 SORT-WRITE-3.                                                    ST1054.2
044200     MOVE     "SORT-TEST-3" TO PAR-NAME.                          ST1054.2
044300     PERFORM  PRINT-DETAIL.                                       ST1054.2
044400     CLOSE    SORTOUT-1E.                                         ST1054.2
044500     PERFORM  CLOSE-FILES.                                        ST1054.2
      *END-OF,ST105A                                                                  
