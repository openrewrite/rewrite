      *HEADER,COBOL,ST101A                                                            
000100 IDENTIFICATION DIVISION.                                         ST1014.2
000200 PROGRAM-ID.                                                      ST1014.2
000300     ST101A.                                                      ST1014.2
000400****************************************************************  ST1014.2
000500*                                                              *  ST1014.2
000600*    VALIDATION FOR:-                                          *  ST1014.2
000700*                                                              *  ST1014.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1014.2
000900*                                                              *  ST1014.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".ST1014.2
001100*                                                              *  ST1014.2
001200****************************************************************  ST1014.2
001300*                                                              *  ST1014.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  ST1014.2
001500*                                                              *  ST1014.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  ST1014.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  ST1014.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  ST1014.2
001900*                                                              *  ST1014.2
002000****************************************************************  ST1014.2
002100 ENVIRONMENT DIVISION.                                            ST1014.2
002200 CONFIGURATION SECTION.                                           ST1014.2
002300 SOURCE-COMPUTER.                                                 ST1014.2
002400     XXXXX082.                                                    ST1014.2
002500 OBJECT-COMPUTER.                                                 ST1014.2
002600     XXXXX083.                                                    ST1014.2
002700 INPUT-OUTPUT SECTION.                                            ST1014.2
002800 FILE-CONTROL.                                                    ST1014.2
002900     SELECT PRINT-FILE ASSIGN TO                                  ST1014.2
003000     XXXXX055.                                                    ST1014.2
003100     SELECT SORTFILE-1A ASSIGN TO                                 ST1014.2
003200     XXXXX027.                                                    ST1014.2
003300     SELECT SORTOUT-1A ASSIGN TO                                  ST1014.2
003400     XXXXP001.                                                    ST1014.2
003500 DATA DIVISION.                                                   ST1014.2
003600 FILE SECTION.                                                    ST1014.2
003700 FD  PRINT-FILE.                                                  ST1014.2
003800 01  PRINT-REC PICTURE X(120).                                    ST1014.2
003900 01  DUMMY-RECORD PICTURE X(120).                                 ST1014.2
004000 SD  SORTFILE-1A                                                  ST1014.2
004100     DATA RECORD IS S-RECORD.                                     ST1014.2
004200 01  S-RECORD.                                                    ST1014.2
004300     02  KEYS-GROUP.                                              ST1014.2
004400         03  KEY-1 PICTURE 9.                                     ST1014.2
004500         03  KEY-2 PICTURE 99.                                    ST1014.2
004600         03  KEY-3 PICTURE 999.                                   ST1014.2
004700         03  KEY-4 PICTURE 9999.                                  ST1014.2
004800         03  KEY-5 PICTURE 9(5).                                  ST1014.2
004900     02  RDF-KEYS REDEFINES KEYS-GROUP PICTURE 9(15).             ST1014.2
005000     02  FILLER PICTURE X(105).                                   ST1014.2
005100 FD  SORTOUT-1A                                                   ST1014.2
005200     BLOCK CONTAINS 10 RECORDS                                    ST1014.2
005300     LABEL RECORDS ARE STANDARD                                   ST1014.2
005400C    VALUE OF                                                     ST1014.2
005500C    XXXXX074                                                     ST1014.2
005600C    IS                                                           ST1014.2
005700C    XXXXX075                                                     ST1014.2
005800*    XXXXX069                                                     ST1014.2
005900     DATA RECORD IS SORTED.                                       ST1014.2
006000 01  SORTED PICTURE X(120).                                       ST1014.2
006100 WORKING-STORAGE SECTION.                                         ST1014.2
006200 77  C0 PICTURE 9 VALUE 0.                                        ST1014.2
006300 77  C1 PICTURE 9 VALUE 1.                                        ST1014.2
006400 77  C2 PICTURE 9 VALUE 2.                                        ST1014.2
006500 77  C6 PICTURE 9 VALUE 6.                                        ST1014.2
006600 77  C3 PICTURE 9 VALUE 3.                                        ST1014.2
006700 77  COMMENT-SENTENCE  PIC X(116) VALUE " THE FILE BUILT IN ST101AST1014.2
006800-    " IS USED BY ST102A. ST102A DOES NOT PRODUCE A REPORT- THE R ST1014.2
006900-    "ESULTS ARE CHECKED IN ST103A.".                             ST1014.2
007000 01  WKEYS-GROUP.                                                 ST1014.2
007100     02  WKEY-1 PICTURE 9.                                        ST1014.2
007200     02  WKEY-2 PICTURE 99.                                       ST1014.2
007300     02  WKEY-3 PICTURE 999.                                      ST1014.2
007400     02  WKEY-4 PICTURE 9999.                                     ST1014.2
007500     02  WKEY-5 PICTURE 9(5).                                     ST1014.2
007600 01  WKEYS-RDF REDEFINES WKEYS-GROUP PICTURE 9(15).               ST1014.2
007700 01  TEST-RESULTS.                                                ST1014.2
007800     02 FILLER                   PIC X      VALUE SPACE.          ST1014.2
007900     02 FEATURE                  PIC X(20)  VALUE SPACE.          ST1014.2
008000     02 FILLER                   PIC X      VALUE SPACE.          ST1014.2
008100     02 P-OR-F                   PIC X(5)   VALUE SPACE.          ST1014.2
008200     02 FILLER                   PIC X      VALUE SPACE.          ST1014.2
008300     02  PAR-NAME.                                                ST1014.2
008400       03 FILLER                 PIC X(19)  VALUE SPACE.          ST1014.2
008500       03  PARDOT-X              PIC X      VALUE SPACE.          ST1014.2
008600       03 DOTVALUE               PIC 99     VALUE ZERO.           ST1014.2
008700     02 FILLER                   PIC X(8)   VALUE SPACE.          ST1014.2
008800     02 RE-MARK                  PIC X(61).                       ST1014.2
008900 01  TEST-COMPUTED.                                               ST1014.2
009000     02 FILLER                   PIC X(30)  VALUE SPACE.          ST1014.2
009100     02 FILLER                   PIC X(17)  VALUE                 ST1014.2
009200            "       COMPUTED=".                                   ST1014.2
009300     02 COMPUTED-X.                                               ST1014.2
009400     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          ST1014.2
009500     03 COMPUTED-N               REDEFINES COMPUTED-A             ST1014.2
009600                                 PIC -9(9).9(9).                  ST1014.2
009700     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         ST1014.2
009800     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     ST1014.2
009900     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     ST1014.2
010000     03       CM-18V0 REDEFINES COMPUTED-A.                       ST1014.2
010100         04 COMPUTED-18V0                    PIC -9(18).          ST1014.2
010200         04 FILLER                           PIC X.               ST1014.2
010300     03 FILLER PIC X(50) VALUE SPACE.                             ST1014.2
010400 01  TEST-CORRECT.                                                ST1014.2
010500     02 FILLER PIC X(30) VALUE SPACE.                             ST1014.2
010600     02 FILLER PIC X(17) VALUE "       CORRECT =".                ST1014.2
010700     02 CORRECT-X.                                                ST1014.2
010800     03 CORRECT-A                  PIC X(20) VALUE SPACE.         ST1014.2
010900     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      ST1014.2
011000     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         ST1014.2
011100     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     ST1014.2
011200     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     ST1014.2
011300     03      CR-18V0 REDEFINES CORRECT-A.                         ST1014.2
011400         04 CORRECT-18V0                     PIC -9(18).          ST1014.2
011500         04 FILLER                           PIC X.               ST1014.2
011600     03 FILLER PIC X(2) VALUE SPACE.                              ST1014.2
011700     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     ST1014.2
011800 01  CCVS-C-1.                                                    ST1014.2
011900     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAST1014.2
012000-    "SS  PARAGRAPH-NAME                                          ST1014.2
012100-    "       REMARKS".                                            ST1014.2
012200     02 FILLER                     PIC X(20)    VALUE SPACE.      ST1014.2
012300 01  CCVS-C-2.                                                    ST1014.2
012400     02 FILLER                     PIC X        VALUE SPACE.      ST1014.2
012500     02 FILLER                     PIC X(6)     VALUE "TESTED".   ST1014.2
012600     02 FILLER                     PIC X(15)    VALUE SPACE.      ST1014.2
012700     02 FILLER                     PIC X(4)     VALUE "FAIL".     ST1014.2
012800     02 FILLER                     PIC X(94)    VALUE SPACE.      ST1014.2
012900 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       ST1014.2
013000 01  REC-CT                        PIC 99       VALUE ZERO.       ST1014.2
013100 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       ST1014.2
013200 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       ST1014.2
013300 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       ST1014.2
013400 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       ST1014.2
013500 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       ST1014.2
013600 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       ST1014.2
013700 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      ST1014.2
013800 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       ST1014.2
013900 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     ST1014.2
014000 01  CCVS-H-1.                                                    ST1014.2
014100     02  FILLER                    PIC X(39)    VALUE SPACES.     ST1014.2
014200     02  FILLER                    PIC X(42)    VALUE             ST1014.2
014300     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 ST1014.2
014400     02  FILLER                    PIC X(39)    VALUE SPACES.     ST1014.2
014500 01  CCVS-H-2A.                                                   ST1014.2
014600   02  FILLER                        PIC X(40)  VALUE SPACE.      ST1014.2
014700   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  ST1014.2
014800   02  FILLER                        PIC XXXX   VALUE             ST1014.2
014900     "4.2 ".                                                      ST1014.2
015000   02  FILLER                        PIC X(28)  VALUE             ST1014.2
015100            " COPY - NOT FOR DISTRIBUTION".                       ST1014.2
015200   02  FILLER                        PIC X(41)  VALUE SPACE.      ST1014.2
015300                                                                  ST1014.2
015400 01  CCVS-H-2B.                                                   ST1014.2
015500   02  FILLER                        PIC X(15)  VALUE             ST1014.2
015600            "TEST RESULT OF ".                                    ST1014.2
015700   02  TEST-ID                       PIC X(9).                    ST1014.2
015800   02  FILLER                        PIC X(4)   VALUE             ST1014.2
015900            " IN ".                                               ST1014.2
016000   02  FILLER                        PIC X(12)  VALUE             ST1014.2
016100     " HIGH       ".                                              ST1014.2
016200   02  FILLER                        PIC X(22)  VALUE             ST1014.2
016300            " LEVEL VALIDATION FOR ".                             ST1014.2
016400   02  FILLER                        PIC X(58)  VALUE             ST1014.2
016500     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1014.2
016600 01  CCVS-H-3.                                                    ST1014.2
016700     02  FILLER                      PIC X(34)  VALUE             ST1014.2
016800            " FOR OFFICIAL USE ONLY    ".                         ST1014.2
016900     02  FILLER                      PIC X(58)  VALUE             ST1014.2
017000     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".ST1014.2
017100     02  FILLER                      PIC X(28)  VALUE             ST1014.2
017200            "  COPYRIGHT   1985 ".                                ST1014.2
017300 01  CCVS-E-1.                                                    ST1014.2
017400     02 FILLER                       PIC X(52)  VALUE SPACE.      ST1014.2
017500     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              ST1014.2
017600     02 ID-AGAIN                     PIC X(9).                    ST1014.2
017700     02 FILLER                       PIC X(45)  VALUE SPACES.     ST1014.2
017800 01  CCVS-E-2.                                                    ST1014.2
017900     02  FILLER                      PIC X(31)  VALUE SPACE.      ST1014.2
018000     02  FILLER                      PIC X(21)  VALUE SPACE.      ST1014.2
018100     02 CCVS-E-2-2.                                               ST1014.2
018200         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      ST1014.2
018300         03 FILLER                   PIC X      VALUE SPACE.      ST1014.2
018400         03 ENDER-DESC               PIC X(44)  VALUE             ST1014.2
018500            "ERRORS ENCOUNTERED".                                 ST1014.2
018600 01  CCVS-E-3.                                                    ST1014.2
018700     02  FILLER                      PIC X(22)  VALUE             ST1014.2
018800            " FOR OFFICIAL USE ONLY".                             ST1014.2
018900     02  FILLER                      PIC X(12)  VALUE SPACE.      ST1014.2
019000     02  FILLER                      PIC X(58)  VALUE             ST1014.2
019100     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1014.2
019200     02  FILLER                      PIC X(13)  VALUE SPACE.      ST1014.2
019300     02 FILLER                       PIC X(15)  VALUE             ST1014.2
019400             " COPYRIGHT 1985".                                   ST1014.2
019500 01  CCVS-E-4.                                                    ST1014.2
019600     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      ST1014.2
019700     02 FILLER                       PIC X(4)   VALUE " OF ".     ST1014.2
019800     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      ST1014.2
019900     02 FILLER                       PIC X(40)  VALUE             ST1014.2
020000      "  TESTS WERE EXECUTED SUCCESSFULLY".                       ST1014.2
020100 01  XXINFO.                                                      ST1014.2
020200     02 FILLER                       PIC X(19)  VALUE             ST1014.2
020300            "*** INFORMATION ***".                                ST1014.2
020400     02 INFO-TEXT.                                                ST1014.2
020500       04 FILLER                     PIC X(8)   VALUE SPACE.      ST1014.2
020600       04 XXCOMPUTED                 PIC X(20).                   ST1014.2
020700       04 FILLER                     PIC X(5)   VALUE SPACE.      ST1014.2
020800       04 XXCORRECT                  PIC X(20).                   ST1014.2
020900     02 INF-ANSI-REFERENCE           PIC X(48).                   ST1014.2
021000 01  HYPHEN-LINE.                                                 ST1014.2
021100     02 FILLER  PIC IS X VALUE IS SPACE.                          ST1014.2
021200     02 FILLER  PIC IS X(65)    VALUE IS "************************ST1014.2
021300-    "*****************************************".                 ST1014.2
021400     02 FILLER  PIC IS X(54)    VALUE IS "************************ST1014.2
021500-    "******************************".                            ST1014.2
021600 01  CCVS-PGM-ID                     PIC X(9)   VALUE             ST1014.2
021700     "ST101A".                                                    ST1014.2
021800 PROCEDURE DIVISION.                                              ST1014.2
021900 SORT-INIT SECTION.                                               ST1014.2
022000 I-1.                                                             ST1014.2
022100     SORT SORTFILE-1A                                             ST1014.2
022200     ON ASCENDING KEY KEY-1                                       ST1014.2
022300     ON DESCENDING KEY KEY-2                                      ST1014.2
022400     ON ASCENDING KEY KEY-3                                       ST1014.2
022500     DESCENDING KEY-4 KEY-5                                       ST1014.2
022600     INPUT PROCEDURE IS INSORT                                    ST1014.2
022700     OUTPUT PROCEDURE IS OUTP1 THRU OUTP3.                        ST1014.2
022800 I-2.                                                             ST1014.2
022900     STOP RUN.                                                    ST1014.2
023000 INSORT SECTION.                                                  ST1014.2
023100 IN-1.                                                            ST1014.2
023200*    NOTE.                                                        ST1014.2
023300*       KEYS 1 AND 3 THRU 5 WILL VARY IN VALUE BETWEEN 1 AND 2.   ST1014.2
023400*       KEY 2 VARIES FROM 1 THRU 6. THUS 96 RECORDS ARE CREATED   ST1014.2
023500*       IN REVERSE SEQUENCE OF SORTING ORDER. TWO RECORDS ARE     ST1014.2
023600*       ADDED TO EACH END OF THE SORTED STRING FOR HI-LOW CONTROL.ST1014.2
023700*       THE SORT STATEMENT TESTS THE SERIES AND THRU OPTIONS WITH ST1014.2
023800*       INCLUSION AND OMISSION OF OPTIONAL WORDS. THE SORT        ST1014.2
023900*       STATEMENT REPRESENTS BASIC SORTING PERMITTED BY LEVEL 1 OFST1014.2
024000*       THE SORT MODULE.                                          ST1014.2
024100 IN-2.                                                            ST1014.2
024200     MOVE 900009000000000 TO RDF-KEYS.                            ST1014.2
024300     RELEASE S-RECORD.                                            ST1014.2
024400     MOVE 009000000900009 TO RDF-KEYS.                            ST1014.2
024500     RELEASE S-RECORD.                                            ST1014.2
024600     MOVE 900008000000000 TO RDF-KEYS.                            ST1014.2
024700     RELEASE S-RECORD.                                            ST1014.2
024800     MOVE 009000000900008 TO RDF-KEYS.                            ST1014.2
024900     RELEASE S-RECORD.                                            ST1014.2
025000*    NOTE HI-LOW CONTROL RECORDS DONE.                            ST1014.2
025100     MOVE 300003000000000 TO WKEYS-RDF.                           ST1014.2
025200 IN-3.                                                            ST1014.2
025300     PERFORM IN-4 2 TIMES.                                        ST1014.2
025400     GO TO IN-EXIT.                                               ST1014.2
025500 IN-4.                                                            ST1014.2
025600     SUBTRACT C1 FROM WKEY-1.                                     ST1014.2
025700     PERFORM IN-5 6 TIMES.                                        ST1014.2
025800 IN-5.                                                            ST1014.2
025900     IF WKEY-2 IS EQUAL TO C6                                     ST1014.2
026000         MOVE C0 TO WKEY-2.                                       ST1014.2
026100     ADD C1 TO WKEY-2.                                            ST1014.2
026200     PERFORM IN-6 2 TIMES.                                        ST1014.2
026300 IN-6.                                                            ST1014.2
026400     IF WKEY-3 IS EQUAL TO C1                                     ST1014.2
026500         MOVE C3 TO WKEY-3.                                       ST1014.2
026600     SUBTRACT C1 FROM WKEY-3.                                     ST1014.2
026700     PERFORM IN-7 2 TIMES.                                        ST1014.2
026800 IN-7.                                                            ST1014.2
026900     IF WKEY-4 IS EQUAL TO C2                                     ST1014.2
027000         MOVE C0 TO WKEY-4.                                       ST1014.2
027100     ADD C1 TO WKEY-4.                                            ST1014.2
027200     PERFORM IN-8 2 TIMES.                                        ST1014.2
027300 IN-8.                                                            ST1014.2
027400     IF WKEY-5 IS EQUAL TO C2                                     ST1014.2
027500         MOVE C0 TO WKEY-5.                                       ST1014.2
027600     ADD C1 TO WKEY-5.                                            ST1014.2
027700     MOVE WKEYS-RDF TO RDF-KEYS.                                  ST1014.2
027800     RELEASE S-RECORD.                                            ST1014.2
027900 IN-EXIT.                                                         ST1014.2
028000     EXIT.                                                        ST1014.2
028100 OUTP1 SECTION.                                                   ST1014.2
028200 SORTING-TEST.                                                    ST1014.2
028300     OPEN     OUTPUT SORTOUT-1A.                                  ST1014.2
028400 OPEN-FILES.                                                      ST1014.2
028500     OPEN     OUTPUT PRINT-FILE.                                  ST1014.2
028600     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   ST1014.2
028700     MOVE    SPACE TO TEST-RESULTS.                               ST1014.2
028800     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             ST1014.2
028900     GO TO CCVS1-EXIT.                                            ST1014.2
029000 CLOSE-FILES.                                                     ST1014.2
029100     MOVE SPACES TO TEST-RESULTS.                                 ST1014.2
029200     MOVE COMMENT-SENTENCE TO TEST-RESULTS.                       ST1014.2
029300     PERFORM PRINT-DETAIL.                                        ST1014.2
029400     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   ST1014.2
029500     MOVE SPACES TO TEST-RESULTS.                                 ST1014.2
029600 TERMINATE-CCVS.                                                  ST1014.2
029700*    EXIT PROGRAM.                                                ST1014.2
029800*TERMINATE-CALL.                                                  ST1014.2
029900     STOP     RUN.                                                ST1014.2
030000 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         ST1014.2
030100 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           ST1014.2
030200 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          ST1014.2
030300 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      ST1014.2
030400     MOVE "****TEST DELETED****" TO RE-MARK.                      ST1014.2
030500 PRINT-DETAIL.                                                    ST1014.2
030600     IF REC-CT NOT EQUAL TO ZERO                                  ST1014.2
030700             MOVE "." TO PARDOT-X                                 ST1014.2
030800             MOVE REC-CT TO DOTVALUE.                             ST1014.2
030900     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      ST1014.2
031000     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               ST1014.2
031100        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 ST1014.2
031200          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 ST1014.2
031300     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              ST1014.2
031400     MOVE SPACE TO CORRECT-X.                                     ST1014.2
031500     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         ST1014.2
031600     MOVE     SPACE TO RE-MARK.                                   ST1014.2
031700 HEAD-ROUTINE.                                                    ST1014.2
031800     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  ST1014.2
031900     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  ST1014.2
032000     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  ST1014.2
032100     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  ST1014.2
032200 COLUMN-NAMES-ROUTINE.                                            ST1014.2
032300     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           ST1014.2
032400     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1014.2
032500     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        ST1014.2
032600 END-ROUTINE.                                                     ST1014.2
032700     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.ST1014.2
032800 END-RTN-EXIT.                                                    ST1014.2
032900     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1014.2
033000 END-ROUTINE-1.                                                   ST1014.2
033100      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      ST1014.2
033200      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               ST1014.2
033300      ADD PASS-COUNTER TO ERROR-HOLD.                             ST1014.2
033400*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   ST1014.2
033500      MOVE PASS-COUNTER TO CCVS-E-4-1.                            ST1014.2
033600      MOVE ERROR-HOLD TO CCVS-E-4-2.                              ST1014.2
033700      MOVE CCVS-E-4 TO CCVS-E-2-2.                                ST1014.2
033800      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           ST1014.2
033900  END-ROUTINE-12.                                                 ST1014.2
034000      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        ST1014.2
034100     IF       ERROR-COUNTER IS EQUAL TO ZERO                      ST1014.2
034200         MOVE "NO " TO ERROR-TOTAL                                ST1014.2
034300         ELSE                                                     ST1014.2
034400         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       ST1014.2
034500     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           ST1014.2
034600     PERFORM WRITE-LINE.                                          ST1014.2
034700 END-ROUTINE-13.                                                  ST1014.2
034800     IF DELETE-COUNTER IS EQUAL TO ZERO                           ST1014.2
034900         MOVE "NO " TO ERROR-TOTAL  ELSE                          ST1014.2
035000         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      ST1014.2
035100     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   ST1014.2
035200     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           ST1014.2
035300      IF   INSPECT-COUNTER EQUAL TO ZERO                          ST1014.2
035400          MOVE "NO " TO ERROR-TOTAL                               ST1014.2
035500      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   ST1014.2
035600      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            ST1014.2
035700      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          ST1014.2
035800     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           ST1014.2
035900 WRITE-LINE.                                                      ST1014.2
036000     ADD 1 TO RECORD-COUNT.                                       ST1014.2
036100*    IF RECORD-COUNT GREATER 42                                   ST1014.2
036200*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          ST1014.2
036300*        MOVE SPACE TO DUMMY-RECORD                               ST1014.2
036400*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  ST1014.2
036500*        MOVE CCVS-H-1  TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    ST1014.2
036600*        MOVE CCVS-H-2A TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    ST1014.2
036700*        MOVE CCVS-H-2B TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    ST1014.2
036800*        MOVE CCVS-H-3  TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    ST1014.2
036900*        MOVE CCVS-C-1  TO DUMMY-RECORD PERFORM WRT-LN            ST1014.2
037000*        MOVE CCVS-C-2  TO DUMMY-RECORD PERFORM WRT-LN            ST1014.2
037100*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          ST1014.2
037200*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          ST1014.2
037300*        MOVE ZERO TO RECORD-COUNT.                               ST1014.2
037400     PERFORM WRT-LN.                                              ST1014.2
037500 WRT-LN.                                                          ST1014.2
037600     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               ST1014.2
037700     MOVE SPACE TO DUMMY-RECORD.                                  ST1014.2
037800 BLANK-LINE-PRINT.                                                ST1014.2
037900     PERFORM WRT-LN.                                              ST1014.2
038000 FAIL-ROUTINE.                                                    ST1014.2
038100     IF     COMPUTED-X NOT EQUAL TO SPACE                         ST1014.2
038200            GO TO   FAIL-ROUTINE-WRITE.                           ST1014.2
038300     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.ST1014.2
038400     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 ST1014.2
038500     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   ST1014.2
038600     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1014.2
038700     MOVE   SPACES TO INF-ANSI-REFERENCE.                         ST1014.2
038800     GO TO  FAIL-ROUTINE-EX.                                      ST1014.2
038900 FAIL-ROUTINE-WRITE.                                              ST1014.2
039000     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         ST1014.2
039100     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 ST1014.2
039200     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. ST1014.2
039300     MOVE   SPACES TO COR-ANSI-REFERENCE.                         ST1014.2
039400 FAIL-ROUTINE-EX. EXIT.                                           ST1014.2
039500 BAIL-OUT.                                                        ST1014.2
039600     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   ST1014.2
039700     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           ST1014.2
039800 BAIL-OUT-WRITE.                                                  ST1014.2
039900     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  ST1014.2
040000     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 ST1014.2
040100     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1014.2
040200     MOVE   SPACES TO INF-ANSI-REFERENCE.                         ST1014.2
040300 BAIL-OUT-EX. EXIT.                                               ST1014.2
040400 CCVS1-EXIT.                                                      ST1014.2
040500     EXIT.                                                        ST1014.2
040600 ST101-0001-01.                                                   ST1014.2
040700     MOVE     "ST101 GENERATES OUTPUT" TO RE-MARK.                ST1014.2
040800     PERFORM  PRINT-DETAIL.                                       ST1014.2
040900     MOVE     "WHICH AFFECTS PROGRAMS" TO RE-MARK.                ST1014.2
041000     PERFORM  PRINT-DETAIL.                                       ST1014.2
041100     MOVE     "ST102 AND ST103."       TO RE-MARK.                ST1014.2
041200     PERFORM  PRINT-DETAIL.                                       ST1014.2
041300     MOVE     "SORT --- FIVE KEYS" TO FEATURE.                    ST1014.2
041400 SORT-TEST-1.                                                     ST1014.2
041500     PERFORM  RET-1.                                              ST1014.2
041600     IF       RDF-KEYS EQUAL TO 009000000900009                   ST1014.2
041700              PERFORM PASS GO TO SORT-WRITE-1.                    ST1014.2
041800     GO       TO SORT-FAIL-1.                                     ST1014.2
041900 SORT-DELETE-1.                                                   ST1014.2
042000     PERFORM  DE-LETE.                                            ST1014.2
042100     GO       TO SORT-WRITE-1.                                    ST1014.2
042200 SORT-FAIL-1.                                                     ST1014.2
042300     MOVE     RDF-KEYS TO COMPUTED-18V0.                          ST1014.2
042400     MOVE     009000000900009 TO CORRECT-18V0.                    ST1014.2
042500     PERFORM  FAIL.                                               ST1014.2
042600 SORT-WRITE-1.                                                    ST1014.2
042700     MOVE     "SORT-TEST-1 " TO PAR-NAME.                         ST1014.2
042800     PERFORM  PRINT-DETAIL.                                       ST1014.2
042900 SORT-TEST-2.                                                     ST1014.2
043000     PERFORM  RET-1.                                              ST1014.2
043100     IF       RDF-KEYS EQUAL TO 009000000900008                   ST1014.2
043200              PERFORM PASS GO TO SORT-WRITE-2.                    ST1014.2
043300     GO       TO SORT-FAIL-2.                                     ST1014.2
043400 SORT-DELETE-2.                                                   ST1014.2
043500     PERFORM  DE-LETE.                                            ST1014.2
043600     GO       TO SORT-WRITE-2.                                    ST1014.2
043700 SORT-FAIL-2.                                                     ST1014.2
043800     MOVE     RDF-KEYS TO COMPUTED-18V0.                          ST1014.2
043900     MOVE     009000000900009 TO CORRECT-18V0.                    ST1014.2
044000     PERFORM  FAIL.                                               ST1014.2
044100 SORT-WRITE-2.                                                    ST1014.2
044200     MOVE     "SORT-TEST-2 " TO PAR-NAME.                         ST1014.2
044300     PERFORM  PRINT-DETAIL.                                       ST1014.2
044400 SORT-TEST-3.                                                     ST1014.2
044500     PERFORM  RET-1.                                              ST1014.2
044600     IF       RDF-KEYS EQUAL TO 106001000200002                   ST1014.2
044700              PERFORM PASS GO TO SORT-WRITE-3.                    ST1014.2
044800     GO       TO SORT-FAIL-3.                                     ST1014.2
044900 SORT-DELETE-3.                                                   ST1014.2
045000     PERFORM  DE-LETE.                                            ST1014.2
045100     GO       TO SORT-WRITE-3.                                    ST1014.2
045200 SORT-FAIL-3.                                                     ST1014.2
045300     MOVE     RDF-KEYS TO COMPUTED-18V0.                          ST1014.2
045400     MOVE     106001000200002  TO CORRECT-18V0.                   ST1014.2
045500     PERFORM  FAIL.                                               ST1014.2
045600 SORT-WRITE-3.                                                    ST1014.2
045700     MOVE     "SORT-TEST-3 " TO PAR-NAME.                         ST1014.2
045800     PERFORM  PRINT-DETAIL.                                       ST1014.2
045900 OUTP2 SECTION.                                                   ST1014.2
046000 SORT-TEST-4.                                                     ST1014.2
046100     PERFORM  RET-2 48 TIMES.                                     ST1014.2
046200     IF       RDF-KEYS EQUAL TO 206001000200002                   ST1014.2
046300              PERFORM PASS GO TO SORT-WRITE-4.                    ST1014.2
046400     GO       TO SORT-FAIL-4.                                     ST1014.2
046500 SORT-DELETE-4.                                                   ST1014.2
046600     PERFORM  DE-LETE.                                            ST1014.2
046700     GO       TO SORT-WRITE-4.                                    ST1014.2
046800 SORT-FAIL-4.                                                     ST1014.2
046900     MOVE     RDF-KEYS TO COMPUTED-18V0.                          ST1014.2
047000     MOVE    206001000200002 TO CORRECT-18V0.                     ST1014.2
047100     PERFORM  FAIL.                                               ST1014.2
047200 SORT-WRITE-4.                                                    ST1014.2
047300     MOVE     "SORT-TEST-4 " TO PAR-NAME.                         ST1014.2
047400     PERFORM  PRINT-DETAIL.                                       ST1014.2
047500 SORT-TEST-5.                                                     ST1014.2
047600     PERFORM  RET-2 40 TIMES.                                     ST1014.2
047700     IF       RDF-KEYS EQUAL TO 201001000200002                   ST1014.2
047800              PERFORM PASS GO TO SORT-WRITE-5.                    ST1014.2
047900     GO       TO SORT-FAIL-5.                                     ST1014.2
048000 SORT-DELETE-5.                                                   ST1014.2
048100     PERFORM  DE-LETE.                                            ST1014.2
048200     GO       TO SORT-WRITE-5.                                    ST1014.2
048300 SORT-FAIL-5.                                                     ST1014.2
048400     MOVE     RDF-KEYS TO COMPUTED-18V0.                          ST1014.2
048500     MOVE     201001000200002 TO CORRECT-18V0.                    ST1014.2
048600     PERFORM  FAIL.                                               ST1014.2
048700 SORT-WRITE-5.                                                    ST1014.2
048800     MOVE     "SORT-TEST-5 " TO PAR-NAME.                         ST1014.2
048900     PERFORM  PRINT-DETAIL.                                       ST1014.2
049000 SORT-TEST-6.                                                     ST1014.2
049100     PERFORM  RET-2 7 TIMES.                                      ST1014.2
049200     IF       RDF-KEYS EQUAL TO 201002000100001                   ST1014.2
049300              PERFORM PASS GO TO SORT-WRITE-6.                    ST1014.2
049400     GO       TO SORT-FAIL-6.                                     ST1014.2
049500 SORT-DELETE-6.                                                   ST1014.2
049600     PERFORM  DE-LETE.                                            ST1014.2
049700     GO       TO SORT-WRITE-6.                                    ST1014.2
049800 SORT-FAIL-6.                                                     ST1014.2
049900     MOVE     RDF-KEYS TO COMPUTED-18V0.                          ST1014.2
050000     MOVE     201002000100001 TO CORRECT-18V0.                    ST1014.2
050100     PERFORM  FAIL.                                               ST1014.2
050200 SORT-WRITE-6.                                                    ST1014.2
050300     MOVE     "SORT-TEST-6 " TO PAR-NAME.                         ST1014.2
050400     PERFORM  PRINT-DETAIL.                                       ST1014.2
050500 SORT-TEST-7.                                                     ST1014.2
050600     PERFORM  RET-2.                                              ST1014.2
050700     IF       RDF-KEYS EQUAL TO 900008000000000                   ST1014.2
050800              PERFORM PASS GO TO SORT-WRITE-7.                    ST1014.2
050900     GO       TO SORT-FAIL-7.                                     ST1014.2
051000 SORT-DELETE-7.                                                   ST1014.2
051100     PERFORM  DE-LETE.                                            ST1014.2
051200     GO       TO SORT-WRITE-7.                                    ST1014.2
051300 SORT-FAIL-7.                                                     ST1014.2
051400     MOVE     RDF-KEYS TO COMPUTED-18V0.                          ST1014.2
051500     MOVE     900008000000000 TO CORRECT-18V0.                    ST1014.2
051600     PERFORM  FAIL.                                               ST1014.2
051700 SORT-WRITE-7.                                                    ST1014.2
051800     MOVE     "SORT-TEST-7 " TO PAR-NAME.                         ST1014.2
051900     PERFORM  PRINT-DETAIL.                                       ST1014.2
052000 SORT-TEST-8.                                                     ST1014.2
052100     PERFORM  RET-2.                                              ST1014.2
052200     IF       RDF-KEYS EQUAL TO 900009000000000                   ST1014.2
052300              PERFORM PASS GO TO SORT-WRITE-8.                    ST1014.2
052400     GO       TO SORT-FAIL-8.                                     ST1014.2
052500 SORT-DELETE-8.                                                   ST1014.2
052600     PERFORM  DE-LETE.                                            ST1014.2
052700     GO       TO SORT-WRITE-8.                                    ST1014.2
052800 SORT-FAIL-8.                                                     ST1014.2
052900     MOVE     RDF-KEYS TO COMPUTED-18V0.                          ST1014.2
053000     MOVE     900009000000000 TO CORRECT-18V0.                    ST1014.2
053100     PERFORM  FAIL.                                               ST1014.2
053200 SORT-WRITE-8.                                                    ST1014.2
053300     MOVE     "SORT-TEST-8 " TO PAR-NAME.                         ST1014.2
053400     PERFORM  PRINT-DETAIL.                                       ST1014.2
053500 SORT-TEST-9.                                                     ST1014.2
053600     RETURN   SORTFILE-1A AT END                                  ST1014.2
053700              PERFORM PASS GO TO SORT-WRITE-9.                    ST1014.2
053800     GO       TO SORT-FAIL-9.                                     ST1014.2
053900 SORT-DELETE-9.                                                   ST1014.2
054000     PERFORM  DE-LETE.                                            ST1014.2
054100     GO       TO SORT-WRITE-9.                                    ST1014.2
054200 SORT-FAIL-9.                                                     ST1014.2
054300     MOVE     RDF-KEYS TO COMPUTED-18V0.                          ST1014.2
054400     MOVE     "END OF FILE NOT FOUND" TO RE-MARK.                 ST1014.2
054500     PERFORM  FAIL.                                               ST1014.2
054600 SORT-WRITE-9.                                                    ST1014.2
054700     MOVE     "SORT-TEST-9 " TO PAR-NAME.                         ST1014.2
054800     PERFORM  PRINT-DETAIL.                                       ST1014.2
054900 OUTP3 SECTION.                                                   ST1014.2
055000 ST101-0002-01.                                                   ST1014.2
055100     CLOSE    SORTOUT-1A.                                         ST1014.2
055200     GO      TO OUTP3-EXIT.                                       ST1014.2
055300 BAD-FILE.                                                        ST1014.2
055400     MOVE     "BAD-FILE" TO PAR-NAME.                             ST1014.2
055500     PERFORM  FAIL.                                               ST1014.2
055600     MOVE     "END OF FILE PREMATURELY" TO RE-MARK.               ST1014.2
055700     PERFORM  PRINT-DETAIL.                                       ST1014.2
055800     MOVE     "REACHED, PREVIOUS TEST WAS" TO RE-MARK.            ST1014.2
055900     PERFORM  PRINT-DETAIL.                                       ST1014.2
056000     MOVE     "THE LAST SUCCESSFUL TEST." TO RE-MARK.             ST1014.2
056100     PERFORM  PRINT-DETAIL.                                       ST1014.2
056200     MOVE     SPACE TO FEATURE.                                   ST1014.2
056300     GO TO    OUTP3-EXIT.                                         ST1014.2
056400 RET-1.                                                           ST1014.2
056500     RETURN   SORTFILE-1A RECORD AT END GO TO BAD-FILE.           ST1014.2
056600     MOVE     S-RECORD TO SORTED.                                 ST1014.2
056700     WRITE    SORTED.                                             ST1014.2
056800*    NOTE     THE RETURN VERB WITH ALL OPTIONAL WORDS.            ST1014.2
056900 RET-2.                                                           ST1014.2
057000     RETURN   SORTFILE-1A           END GO TO BAD-FILE.           ST1014.2
057100     MOVE     S-RECORD TO SORTED.                                 ST1014.2
057200     WRITE    SORTED.                                             ST1014.2
057300*    NOTE     THE RETURN VERB WITHOUT OPTIONAL WORDS.             ST1014.2
057400 OUTP3-EXIT.                                                      ST1014.2
057500     PERFORM  CLOSE-FILES.                                        ST1014.2
      *END-OF,ST101A                                                                  
