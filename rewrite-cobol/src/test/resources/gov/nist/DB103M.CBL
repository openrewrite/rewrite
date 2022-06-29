      *HEADER,COBOL,DB103M                                                            
000100 IDENTIFICATION DIVISION.                                         DB1034.2
000200 PROGRAM-ID.                                                      DB1034.2
000300     DB103M.                                                      DB1034.2
000400 AUTHOR.                                                          DB1034.2
000500     FEDERAL COMPILER TESTING CENTER.                             DB1034.2
000600 INSTALLATION.                                                    DB1034.2
000700     GENERAL SERVICES ADMINISTRATION                              DB1034.2
000800*     AUTOMATED DATA AND TELECOMMUNICATION SERVICE.                DB1034.2
000900     SOFTWARE DEVELOPMENT OFFICE.                                 DB1034.2
001000     5203 LEESBURG PIKE  SUITE 1100                               DB1034.2
001100     FALLS CHURCH VIRGINIA 22041.                                 DB1034.2
001200                                                                  DB1034.2
001300     PHONE   (703) 756-6153                                       DB1034.2
001400                                                                  DB1034.2
001500     " HIGH       ".                                              DB1034.2
001600 DATE-WRITTEN.                                                    DB1034.2
001700     CCVS-74 VERSION 4.0 - 1980 JULY 1.                           DB1034.2
001800     CREATION DATE     /    VALIDATION DATE                       DB1034.2
001900     "4.2 ".                                                      DB1034.2
002000 SECURITY.                                                        DB1034.2
002100     NONE.                                                        DB1034.2
002200*                                                                 DB1034.2
002300*    *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *DB1034.2
002400*                                                                 DB1034.2
002500*                       PROGRAM ABSTRACT                          DB1034.2
002600*                                                                 DB1034.2
002700*    DB103M TESTS THE BASIC OPERATION OF DEBUG MODULE FACILITIES  DB1034.2
002800*    WHEN THE COMPILE TIME DEBUGGING SWITCH IS OFF.  ALL DEBUG    DB1034.2
002900*    LINES  SHOULD BE TREATED AS COMMENTS AND NO CODE SHOULD      DB1034.2
003000*    BE GENERATED FOR EITHER DEBUG LINES OR DEBUGGING PROCED-     DB1034.2
003100*    URES.                                                        DB1034.2
003200*                                                                 DB1034.2
003300*    THE OBJECT PROGRAM FOR DB103M SHOULD BE EXECUTED TWICE;ONCE  DB1034.2
003400*    WITH THE OBJECT TIME DEBUGGING SWITCH ENABLED (ON), AND ONCE DB1034.2
003500*    WITH THE OBJECT TIME DEBUGGING SWITCH DISABLED (OFF).  BOTH  DB1034.2
003600*    EXECUTION RUNS SHOULD YIELD THE SAME RESULTS AS THE SETTING  DB1034.2
003700*    OF THE OBJECT TIME DEBUGGING SWITCH SHOULD MAKE NO DIFFER-   DB1034.2
003800*    ENCE SINCE THE COMPILE TIME DEBUGGING SWITCH WAS INITIALLY   DB1034.2
003900*    DISABLED.                                                    DB1034.2
004000*                                                                 DB1034.2
004100*                                                                 DB1034.2
004200 ENVIRONMENT DIVISION.                                            DB1034.2
004300 CONFIGURATION SECTION.                                           DB1034.2
004400 SOURCE-COMPUTER.                                                 DB1034.2
004500     XXXXX082.                                                    DB1034.2
004600 OBJECT-COMPUTER.                                                 DB1034.2
004700     XXXXX083.                                                    DB1034.2
004800 INPUT-OUTPUT SECTION.                                            DB1034.2
004900 FILE-CONTROL.                                                    DB1034.2
005000     SELECT PRINT-FILE ASSIGN TO                                  DB1034.2
005100     XXXXX055.                                                    DB1034.2
005200 DATA DIVISION.                                                   DB1034.2
005300 FILE SECTION.                                                    DB1034.2
005400 FD  PRINT-FILE                                                   DB1034.2
005500     LABEL RECORDS                                                DB1034.2
005600     XXXXX084                                                     DB1034.2
005700     DATA RECORD IS PRINT-REC DUMMY-RECORD.                       DB1034.2
005800 01  PRINT-REC PICTURE X(120).                                    DB1034.2
005900 01  DUMMY-RECORD PICTURE X(120).                                 DB1034.2
006000 WORKING-STORAGE SECTION.                                         DB1034.2
006100 77  A PIC 9 COMP VALUE 1.                                        DB1034.2
006200 77  B PIC 9 COMP VALUE 5.                                        DB1034.2
006300 77  C PIC 9 COMP VALUE 9.                                        DB1034.2
006400 77  D PIC 99 COMP.                                               DB1034.2
006500 77  RESULT-FLAG PIC 99 COMP VALUE 0.                             DB1034.2
006600 77  DBLINE-HOLD PIC X(6).                                        DB1034.2
006700 77  DBNAME-HOLD PIC X(30).                                       DB1034.2
006800 77  DBCONT-HOLD PIC X(30).                                       DB1034.2
006900 77  FIVE PIC 9 COMP VALUE 5.                                     DB1034.2
007000 01  SIZE-19.                                                     DB1034.2
007100     02  FILLER PIC X.                                            DB1034.2
007200     02  SIZE-18.                                                 DB1034.2
007300         03  FILLER PIC X.                                        DB1034.2
007400         03  SIZE-17.                                             DB1034.2
007500             04  FILLER PIC X.                                    DB1034.2
007600             04  SIZE-16.                                         DB1034.2
007700                 05  FILLER PIC X.                                DB1034.2
007800                 05  SIZE-15.                                     DB1034.2
007900                     06  FILLER PIC X.                            DB1034.2
008000                     06  SIZE-14.                                 DB1034.2
008100                         07  FILLER PIC X.                        DB1034.2
008200                         07  SIZE-13.                             DB1034.2
008300                             08  FILLER PIC X.                    DB1034.2
008400                             08  SIZE-12.                         DB1034.2
008500                                 09  FILLER PIC X.                DB1034.2
008600                                 09  SIZE-11.                     DB1034.2
008700                                     10  FILLER PIC X.            DB1034.2
008800                                     10  SIZE-10 PIC X(10).       DB1034.2
008900 01  TEST-RESULTS.                                                DB1034.2
009000     02 FILLER                    PICTURE X VALUE SPACE.          DB1034.2
009100     02 FEATURE                   PICTURE X(20) VALUE SPACE.      DB1034.2
009200     02 FILLER                    PICTURE X VALUE SPACE.          DB1034.2
009300     02 P-OR-F                    PICTURE X(5) VALUE SPACE.       DB1034.2
009400     02 FILLER                    PICTURE X  VALUE SPACE.         DB1034.2
009500     02  PAR-NAME.                                                DB1034.2
009600       03 FILLER PICTURE X(12) VALUE SPACE.                       DB1034.2
009700       03  PARDOT-X PICTURE X  VALUE SPACE.                       DB1034.2
009800       03 DOTVALUE PICTURE 99  VALUE ZERO.                        DB1034.2
009900       03 FILLER PIC X(5) VALUE SPACE.                            DB1034.2
010000     02 FILLER PIC X(10) VALUE SPACE.                             DB1034.2
010100     02 RE-MARK PIC X(61).                                        DB1034.2
010200 01  TEST-COMPUTED.                                               DB1034.2
010300     02 FILLER PIC X(30) VALUE SPACE.                             DB1034.2
010400     02 FILLER PIC X(17) VALUE "       COMPUTED=".                DB1034.2
010500     02 COMPUTED-X.                                               DB1034.2
010600     03 COMPUTED-A                PICTURE X(20) VALUE SPACE.      DB1034.2
010700     03 COMPUTED-N REDEFINES COMPUTED-A PICTURE -9(9).9(9).       DB1034.2
010800     03 COMPUTED-0V18 REDEFINES COMPUTED-A  PICTURE -.9(18).      DB1034.2
010900     03 COMPUTED-4V14 REDEFINES COMPUTED-A  PICTURE -9(4).9(14).  DB1034.2
011000     03 COMPUTED-14V4 REDEFINES COMPUTED-A  PICTURE -9(14).9(4).  DB1034.2
011100     03       CM-18V0 REDEFINES COMPUTED-A.                       DB1034.2
011200         04 COMPUTED-18V0                   PICTURE -9(18).       DB1034.2
011300         04 FILLER                          PICTURE X.            DB1034.2
011400     03 FILLER PIC X(50) VALUE SPACE.                             DB1034.2
011500 01  TEST-CORRECT.                                                DB1034.2
011600     02 FILLER PIC X(30) VALUE SPACE.                             DB1034.2
011700     02 FILLER PIC X(17) VALUE "       CORRECT =".                DB1034.2
011800     02 CORRECT-X.                                                DB1034.2
011900     03 CORRECT-A                 PICTURE X(20) VALUE SPACE.      DB1034.2
012000     03 CORRECT-N REDEFINES CORRECT-A PICTURE -9(9).9(9).         DB1034.2
012100     03 CORRECT-0V18 REDEFINES CORRECT-A    PICTURE -.9(18).      DB1034.2
012200     03 CORRECT-4V14 REDEFINES CORRECT-A    PICTURE -9(4).9(14).  DB1034.2
012300     03 CORRECT-14V4 REDEFINES CORRECT-A    PICTURE -9(14).9(4).  DB1034.2
012400     03      CR-18V0 REDEFINES CORRECT-A.                         DB1034.2
012500         04 CORRECT-18V0                    PICTURE -9(18).       DB1034.2
012600         04 FILLER                          PICTURE X.            DB1034.2
012700     03 FILLER PIC X(50) VALUE SPACE.                             DB1034.2
012800 01  CCVS-C-1.                                                    DB1034.2
012900     02 FILLER PICTURE IS X(99) VALUE IS " FEATURE              PADB1034.2
013000-    "SS  PARAGRAPH-NAME                                          DB1034.2
013100-    "        REMARKS".                                           DB1034.2
013200     02 FILLER PICTURE IS X(20) VALUE IS SPACE.                   DB1034.2
013300 01  CCVS-C-2.                                                    DB1034.2
013400     02 FILLER PICTURE IS X VALUE IS SPACE.                       DB1034.2
013500     02 FILLER PICTURE IS X(6) VALUE IS "TESTED".                 DB1034.2
013600     02 FILLER PICTURE IS X(15) VALUE IS SPACE.                   DB1034.2
013700     02 FILLER PICTURE IS X(4) VALUE IS "FAIL".                   DB1034.2
013800     02 FILLER PICTURE IS X(94) VALUE IS SPACE.                   DB1034.2
013900 01  REC-SKL-SUB PICTURE 9(2) VALUE ZERO.                         DB1034.2
014000 01  REC-CT PICTURE 99 VALUE ZERO.                                DB1034.2
014100 01  DELETE-CNT                   PICTURE 999  VALUE ZERO.        DB1034.2
014200 01  ERROR-COUNTER PICTURE IS 999 VALUE IS ZERO.                  DB1034.2
014300 01  INSPECT-COUNTER PIC 999 VALUE ZERO.                          DB1034.2
014400 01  PASS-COUNTER PIC 999 VALUE ZERO.                             DB1034.2
014500 01  TOTAL-ERROR PIC 999 VALUE ZERO.                              DB1034.2
014600 01  ERROR-HOLD PIC 999 VALUE ZERO.                               DB1034.2
014700 01  DUMMY-HOLD PIC X(120) VALUE SPACE.                           DB1034.2
014800 01  RECORD-COUNT PIC 9(5) VALUE ZERO.                            DB1034.2
014900 01  CCVS-H-1.                                                    DB1034.2
015000     02  FILLER   PICTURE X(27)  VALUE SPACE.                     DB1034.2
015100     02 FILLER PICTURE X(67) VALUE                                DB1034.2
015200     " FEDERAL COMPILER TESTING CENTER COBOL COMPILER VALIDATION  DB1034.2
015300-    " SYSTEM".                                                   DB1034.2
015400     02  FILLER     PICTURE X(26)  VALUE SPACE.                   DB1034.2
015500 01  CCVS-H-2.                                                    DB1034.2
015600     02 FILLER PICTURE X(52) VALUE IS                             DB1034.2
015700     "CCVS74 NCC  COPY, NOT FOR DISTRIBUTION.".                   DB1034.2
015800     02 FILLER PICTURE IS X(19) VALUE IS "TEST RESULTS SET-  ".   DB1034.2
015900     02 TEST-ID PICTURE IS X(9).                                  DB1034.2
016000     02 FILLER PICTURE IS X(40) VALUE IS SPACE.                   DB1034.2
016100 01  CCVS-H-3.                                                    DB1034.2
016200     02  FILLER PICTURE X(34) VALUE                               DB1034.2
016300     " FOR OFFICIAL USE ONLY    ".                                DB1034.2
016400     02  FILLER PICTURE X(58) VALUE                               DB1034.2
016500     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".DB1034.2
016600     02  FILLER PICTURE X(28) VALUE                               DB1034.2
016700     "  COPYRIGHT   1974 ".                                       DB1034.2
016800 01  CCVS-E-1.                                                    DB1034.2
016900     02 FILLER PICTURE IS X(52) VALUE IS SPACE.                   DB1034.2
017000     02 FILLER PICTURE IS X(14) VALUE IS "END OF TEST-  ".        DB1034.2
017100     02 ID-AGAIN PICTURE IS X(9).                                 DB1034.2
017200     02 FILLER PICTURE X(45) VALUE IS                             DB1034.2
017300     " NTIS DISTRIBUTION COBOL 74".                               DB1034.2
017400 01  CCVS-E-2.                                                    DB1034.2
017500     02  FILLER                   PICTURE X(31)  VALUE            DB1034.2
017600     SPACE.                                                       DB1034.2
017700     02  FILLER                   PICTURE X(21)  VALUE SPACE.     DB1034.2
017800     02 CCVS-E-2-2.                                               DB1034.2
017900         03 ERROR-TOTAL PICTURE IS XXX VALUE IS SPACE.            DB1034.2
018000         03 FILLER PICTURE IS X VALUE IS SPACE.                   DB1034.2
018100         03 ENDER-DESC PIC X(44) VALUE "ERRORS ENCOUNTERED".      DB1034.2
018200 01  CCVS-E-3.                                                    DB1034.2
018300     02  FILLER PICTURE X(22) VALUE                               DB1034.2
018400     " FOR OFFICIAL USE ONLY".                                    DB1034.2
018500     02  FILLER PICTURE X(12) VALUE SPACE.                        DB1034.2
018600     02  FILLER PICTURE X(58) VALUE                               DB1034.2
018700     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".DB1034.2
018800     02  FILLER PICTURE X(13) VALUE SPACE.                        DB1034.2
018900     02 FILLER PIC X(15) VALUE " COPYRIGHT 1974".                 DB1034.2
019000 01  CCVS-E-4.                                                    DB1034.2
019100     02 CCVS-E-4-1 PIC XXX VALUE SPACE.                           DB1034.2
019200     02 FILLER PIC XXXX VALUE " OF ".                             DB1034.2
019300     02 CCVS-E-4-2 PIC XXX VALUE SPACE.                           DB1034.2
019400     02 FILLER PIC X(40) VALUE                                    DB1034.2
019500      "  TESTS WERE EXECUTED SUCCESSFULLY".                       DB1034.2
019600 01  XXINFO.                                                      DB1034.2
019700     02 FILLER PIC X(30) VALUE "        *** INFORMATION  ***".    DB1034.2
019800     02 INFO-TEXT.                                                DB1034.2
019900     04 FILLER PIC X(20) VALUE SPACE.                             DB1034.2
020000     04 XXCOMPUTED PIC X(20).                                     DB1034.2
020100     04 FILLER PIC X(5) VALUE SPACE.                              DB1034.2
020200     04 XXCORRECT PIC X(20).                                      DB1034.2
020300 01  HYPHEN-LINE.                                                 DB1034.2
020400     02 FILLER PICTURE IS X VALUE IS SPACE.                       DB1034.2
020500     02 FILLER PICTURE IS X(65) VALUE IS "************************DB1034.2
020600-    "*****************************************".                 DB1034.2
020700     02 FILLER PICTURE IS X(54) VALUE IS "************************DB1034.2
020800-    "******************************".                            DB1034.2
020900 01  CCVS-PGM-ID PIC X(6) VALUE                                   DB1034.2
021000     "DB103M".                                                    DB1034.2
021100 PROCEDURE DIVISION.                                              DB1034.2
021200 DECLARATIVES.                                                    DB1034.2
021300 START-UP SECTION.                                                DB1034.2
021400     USE FOR DEBUGGING ON OPEN-FILES.                             DB1034.2
021500 BEGIN-START-UP.                                                  DB1034.2
021600     MOVE 1 TO RESULT-FLAG.                                       DB1034.2
021700 DB-COMMON.                                                       DB1034.2
021800     MOVE DEBUG-LINE TO DBLINE-HOLD.                              DB1034.2
021900     MOVE DEBUG-NAME TO DBNAME-HOLD.                              DB1034.2
022000     MOVE DEBUG-CONTENTS TO DBCONT-HOLD.                          DB1034.2
022100 FALL-THROUGH-AND-SERIES SECTION.                                 DB1034.2
022200     USE FOR DEBUGGING ON FALL-THROUGH-TEST                       DB1034.2
022300              PROC-SERIES-TEST.                                   DB1034.2
022400 BEGIN-FALL-THROUGH-AND-SERIES.                                   DB1034.2
022500     PERFORM DB-COMMON.                                           DB1034.2
022600     MOVE 2 TO RESULT-FLAG.                                       DB1034.2
022700 GO-TO SECTION.                                                   DB1034.2
022800     USE FOR DEBUGGING ON GO-TO-TEST.                             DB1034.2
022900 BEGIN-GO-TO.                                                     DB1034.2
023000     PERFORM DB-COMMON.                                           DB1034.2
023100     MOVE 3 TO RESULT-FLAG.                                       DB1034.2
023200 ALTER-PARAGRAPH SECTION.                                         DB1034.2
023300     USE FOR DEBUGGING ON ALTERABLE-PARAGRAPH.                    DB1034.2
023400 BEGIN-ALTER-PARAGRAPH.                                           DB1034.2
023500     PERFORM DB-COMMON.                                           DB1034.2
023600     MOVE 4 TO RESULT-FLAG.                                       DB1034.2
023700 LOOP-ITERATION SECTION.                                          DB1034.2
023800     USE FOR DEBUGGING ON LOOP-ROUTINE.                           DB1034.2
023900 BEGIN-LOOP-ITERATION.                                            DB1034.2
024000     PERFORM DB-COMMON.                                           DB1034.2
024100     ADD 1 TO RESULT-FLAG.                                        DB1034.2
024200 PERFORM-THRU SECTION.                                            DB1034.2
024300     USE FOR DEBUGGING ON DO-NOTHING-1.                           DB1034.2
024400 BEGIN-PERFORM-THRU.                                              DB1034.2
024500     PERFORM DB-COMMON.                                           DB1034.2
024600     ADD 1 TO RESULT-FLAG.                                        DB1034.2
024700 END DECLARATIVES.                                                DB1034.2
024800 CCVS1 SECTION.                                                   DB1034.2
024900 OPEN-FILES.                                                      DB1034.2
025000     OPEN     OUTPUT PRINT-FILE.                                  DB1034.2
025100     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   DB1034.2
025200     MOVE    SPACE TO TEST-RESULTS.                               DB1034.2
025300     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             DB1034.2
025400     GO TO CCVS1-EXIT.                                            DB1034.2
025500 CLOSE-FILES.                                                     DB1034.2
025600     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   DB1034.2
025700 TERMINATE-CCVS.                                                  DB1034.2
025800*    EXIT PROGRAM.                                                DB1034.2
025900*TERMINATE-CALL.                                                  DB1034.2
026000     STOP     RUN.                                                DB1034.2
026100 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         DB1034.2
026200 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           DB1034.2
026300 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          DB1034.2
026400 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-CNT.          DB1034.2
026500     MOVE "****TEST DELETED****" TO RE-MARK.                      DB1034.2
026600 PRINT-DETAIL.                                                    DB1034.2
026700     IF REC-CT NOT EQUAL TO ZERO                                  DB1034.2
026800             MOVE "." TO PARDOT-X                                 DB1034.2
026900             MOVE REC-CT TO DOTVALUE.                             DB1034.2
027000     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      DB1034.2
027100     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               DB1034.2
027200        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 DB1034.2
027300          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 DB1034.2
027400     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              DB1034.2
027500     MOVE SPACE TO CORRECT-X.                                     DB1034.2
027600     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         DB1034.2
027700     MOVE     SPACE TO RE-MARK.                                   DB1034.2
027800 HEAD-ROUTINE.                                                    DB1034.2
027900     MOVE CCVS-H-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   DB1034.2
028000     MOVE CCVS-H-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.   DB1034.2
028100     MOVE CCVS-H-3 TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.   DB1034.2
028200 COLUMN-NAMES-ROUTINE.                                            DB1034.2
028300     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           DB1034.2
028400     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   DB1034.2
028500     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        DB1034.2
028600 END-ROUTINE.                                                     DB1034.2
028700     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.DB1034.2
028800 END-RTN-EXIT.                                                    DB1034.2
028900     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   DB1034.2
029000 END-ROUTINE-1.                                                   DB1034.2
029100      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      DB1034.2
029200      ERROR-HOLD. ADD DELETE-CNT TO ERROR-HOLD.                   DB1034.2
029300      ADD PASS-COUNTER TO ERROR-HOLD.                             DB1034.2
029400*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   DB1034.2
029500      MOVE PASS-COUNTER TO CCVS-E-4-1.                            DB1034.2
029600      MOVE ERROR-HOLD TO CCVS-E-4-2.                              DB1034.2
029700      MOVE CCVS-E-4 TO CCVS-E-2-2.                                DB1034.2
029800      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           DB1034.2
029900  END-ROUTINE-12.                                                 DB1034.2
030000      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        DB1034.2
030100     IF       ERROR-COUNTER IS EQUAL TO ZERO                      DB1034.2
030200         MOVE "NO " TO ERROR-TOTAL                                DB1034.2
030300         ELSE                                                     DB1034.2
030400         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       DB1034.2
030500     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           DB1034.2
030600     PERFORM WRITE-LINE.                                          DB1034.2
030700 END-ROUTINE-13.                                                  DB1034.2
030800     IF DELETE-CNT IS EQUAL TO ZERO                               DB1034.2
030900         MOVE "NO " TO ERROR-TOTAL  ELSE                          DB1034.2
031000         MOVE DELETE-CNT TO ERROR-TOTAL.                          DB1034.2
031100     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   DB1034.2
031200     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           DB1034.2
031300      IF   INSPECT-COUNTER EQUAL TO ZERO                          DB1034.2
031400          MOVE "NO " TO ERROR-TOTAL                               DB1034.2
031500      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   DB1034.2
031600      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            DB1034.2
031700      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          DB1034.2
031800     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           DB1034.2
031900 WRITE-LINE.                                                      DB1034.2
032000     ADD 1 TO RECORD-COUNT.                                       DB1034.2
032100*    IF RECORD-COUNT GREATER 50                                   DB1034.2
032200*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          DB1034.2
032300*        MOVE SPACE TO DUMMY-RECORD                               DB1034.2
032400*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  DB1034.2
032500*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             DB1034.2
032600*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     DB1034.2
032700*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          DB1034.2
032800*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          DB1034.2
032900*        MOVE ZERO TO RECORD-COUNT.                               DB1034.2
033000     PERFORM WRT-LN.                                              DB1034.2
033100 WRT-LN.                                                          DB1034.2
033200     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               DB1034.2
033300     MOVE SPACE TO DUMMY-RECORD.                                  DB1034.2
033400 BLANK-LINE-PRINT.                                                DB1034.2
033500     PERFORM WRT-LN.                                              DB1034.2
033600 FAIL-ROUTINE.                                                    DB1034.2
033700     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.   DB1034.2
033800     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.    DB1034.2
033900     MOVE "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.    DB1034.2
034000     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     DB1034.2
034100     GO TO FAIL-ROUTINE-EX.                                       DB1034.2
034200 FAIL-ROUTINE-WRITE.                                              DB1034.2
034300     MOVE TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE           DB1034.2
034400     MOVE TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES.   DB1034.2
034500 FAIL-ROUTINE-EX. EXIT.                                           DB1034.2
034600 BAIL-OUT.                                                        DB1034.2
034700     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.       DB1034.2
034800     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.               DB1034.2
034900 BAIL-OUT-WRITE.                                                  DB1034.2
035000     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  DB1034.2
035100     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     DB1034.2
035200 BAIL-OUT-EX. EXIT.                                               DB1034.2
035300 CCVS1-EXIT.                                                      DB1034.2
035400     EXIT.                                                        DB1034.2
035500 START-PROGRAM-TEST.                                              DB1034.2
035600     IF RESULT-FLAG IS NOT EQUAL TO 0                             DB1034.2
035700         MOVE "USE PROCEDURE EXECUTED" TO RE-MARK                 DB1034.2
035800         PERFORM FAIL                                             DB1034.2
035900         PERFORM START-PROGRAM-WRITE                              DB1034.2
036000         ELSE PERFORM PASS                                        DB1034.2
036100         GO TO START-PROGRAM-WRITE.                               DB1034.2
036200     MOVE "DEBUG-LINE; SEE NEXT LINE" TO RE-MARK.                 DB1034.2
036300     MOVE "<===  DEBUG-LINE" TO CORRECT-A.                        DB1034.2
036400     MOVE DBLINE-HOLD TO COMPUTED-A.                              DB1034.2
036500     PERFORM START-PROGRAM-WRITE.                                 DB1034.2
036600     MOVE "DEBUG-NAME; SEE NEXT LINE" TO RE-MARK.                 DB1034.2
036700     MOVE "<===  DEBUG-NAME" TO CORRECT-A.                        DB1034.2
036800     MOVE DBNAME-HOLD TO COMPUTED-A.                              DB1034.2
036900     PERFORM START-PROGRAM-WRITE.                                 DB1034.2
037000     MOVE "DEBUG-CONTENTS; SEE NEXT LINE" TO RE-MARK.             DB1034.2
037100     MOVE "<===  DEBUG-CONTENTS" TO CORRECT-A.                    DB1034.2
037200     MOVE DBCONT-HOLD TO COMPUTED-A.                              DB1034.2
037300     GO TO   START-PROGRAM-WRITE.                                 DB1034.2
037400 START-PROGRAM-DELETE.                                            DB1034.2
037500     PERFORM DE-LETE.                                             DB1034.2
037600 START-PROGRAM-WRITE.                                             DB1034.2
037700     MOVE "DEBUG ON PROC-NAME" TO FEATURE.                        DB1034.2
037800     MOVE "START-PROGRAM-TEST" TO PAR-NAME.                       DB1034.2
037900     PERFORM PRINT-DETAIL.                                        DB1034.2
038000     MOVE 0 TO RESULT-FLAG.                                       DB1034.2
038100 FALL-THROUGH-TEST.                                               DB1034.2
038200     IF RESULT-FLAG IS NOT EQUAL TO 0                             DB1034.2
038300         MOVE "USE PROCEDURE EXECUTED" TO RE-MARK                 DB1034.2
038400         PERFORM FAIL                                             DB1034.2
038500         PERFORM FALL-THROUGH-WRITE                               DB1034.2
038600         ELSE PERFORM PASS                                        DB1034.2
038700         GO TO FALL-THROUGH-WRITE.                                DB1034.2
038800     MOVE "DEBUG-LINE; SEE NEXT LINE" TO RE-MARK.                 DB1034.2
038900     MOVE "<===  DEBUG-LINE" TO CORRECT-A.                        DB1034.2
039000     MOVE DBLINE-HOLD TO COMPUTED-A.                              DB1034.2
039100     PERFORM FALL-THROUGH-WRITE.                                  DB1034.2
039200     MOVE "DEBUG-NAME; SEE NEXT LINE" TO RE-MARK.                 DB1034.2
039300     MOVE "<===  DEBUG-NAME" TO CORRECT-A.                        DB1034.2
039400     MOVE DBNAME-HOLD TO COMPUTED-A.                              DB1034.2
039500     PERFORM FALL-THROUGH-WRITE.                                  DB1034.2
039600     MOVE "DEBUG-CONTENTS; SEE NEXT LINE" TO RE-MARK.             DB1034.2
039700     MOVE "<===  DEBUG-CONTENTS" TO CORRECT-A.                    DB1034.2
039800     MOVE DBCONT-HOLD TO COMPUTED-A.                              DB1034.2
039900     GO TO   FALL-THROUGH-WRITE.                                  DB1034.2
040000 FALL-THROUGH-DELETE.                                             DB1034.2
040100     PERFORM DE-LETE.                                             DB1034.2
040200 FALL-THROUGH-WRITE.                                              DB1034.2
040300     MOVE "DEBUG ON PROC-NAME" TO FEATURE.                        DB1034.2
040400     MOVE "FALL-THROUGH-TEST" TO PAR-NAME.                        DB1034.2
040500     PERFORM PRINT-DETAIL.                                        DB1034.2
040600     MOVE 0 TO RESULT-FLAG.                                       DB1034.2
040700 PROC-SERIES-TEST.                                                DB1034.2
040800     IF RESULT-FLAG IS NOT EQUAL TO 0                             DB1034.2
040900         MOVE "USE PROCEDURE EXECUTED" TO RE-MARK                 DB1034.2
041000         PERFORM FAIL                                             DB1034.2
041100         PERFORM PROC-SERIES-WRITE                                DB1034.2
041200         ELSE PERFORM PASS                                        DB1034.2
041300         GO TO PROC-SERIES-WRITE.                                 DB1034.2
041400     MOVE "DEBUG-LINE; SEE NEXT LINE" TO RE-MARK.                 DB1034.2
041500     MOVE "<===  DEBUG-LINE" TO CORRECT-A.                        DB1034.2
041600     MOVE DBLINE-HOLD TO COMPUTED-A.                              DB1034.2
041700     PERFORM PROC-SERIES-WRITE.                                   DB1034.2
041800     MOVE "DEBUG-NAME; SEE NEXT LINE" TO RE-MARK.                 DB1034.2
041900     MOVE "<===  DEBUG-NAME" TO CORRECT-A.                        DB1034.2
042000     MOVE DBNAME-HOLD TO COMPUTED-A.                              DB1034.2
042100     PERFORM PROC-SERIES-WRITE.                                   DB1034.2
042200     MOVE "DEBUG-CONTENTS; SEE NEXT LINE" TO RE-MARK.             DB1034.2
042300     MOVE "<===  DEBUG-CONTENTS" TO CORRECT-A.                    DB1034.2
042400     MOVE DBCONT-HOLD TO COMPUTED-A.                              DB1034.2
042500     GO TO   PROC-SERIES-WRITE.                                   DB1034.2
042600 PROC-SERIES-DELETE.                                              DB1034.2
042700     PERFORM DE-LETE.                                             DB1034.2
042800 PROC-SERIES-WRITE.                                               DB1034.2
042900     MOVE "DEBUG ON PROC-NAME" TO FEATURE.                        DB1034.2
043000     MOVE "PROC-SERIES-TEST" TO PAR-NAME.                         DB1034.2
043100     PERFORM PRINT-DETAIL.                                        DB1034.2
043200     MOVE 0 TO RESULT-FLAG.                                       DB1034.2
043300 ALTERABLE-PARAGRAPH.                                             DB1034.2
043400     GO TO GO-TO-TEST.                                            DB1034.2
043500 FILLER-PARAGRAPH.                                                DB1034.2
043600     DISPLAY "ALTER FAILED AT ALTER-TEST-INIT".                   DB1034.2
043700     PERFORM FAIL.                                                DB1034.2
043800     GO TO ALTERED-GO-TO-TEST.                                    DB1034.2
043900 GO-TO-TEST.                                                      DB1034.2
044000     IF RESULT-FLAG IS NOT EQUAL TO 0                             DB1034.2
044100         MOVE "USE PROCEDURE EXECUTED" TO RE-MARK                 DB1034.2
044200         PERFORM FAIL                                             DB1034.2
044300         PERFORM GO-TO-WRITE                                      DB1034.2
044400         ELSE PERFORM PASS                                        DB1034.2
044500         GO TO GO-TO-WRITE.                                       DB1034.2
044600     MOVE "DEBUG-LINE; SEE NEXT LINE" TO RE-MARK.                 DB1034.2
044700     MOVE "<===  DEBUG-LINE" TO CORRECT-A.                        DB1034.2
044800     MOVE DBLINE-HOLD TO COMPUTED-A.                              DB1034.2
044900     PERFORM GO-TO-WRITE.                                         DB1034.2
045000     MOVE "DEBUG-NAME; SEE NEXT LINE" TO RE-MARK.                 DB1034.2
045100     MOVE "<===  DEBUG-NAME" TO CORRECT-A.                        DB1034.2
045200     MOVE DBNAME-HOLD TO COMPUTED-A.                              DB1034.2
045300     PERFORM GO-TO-WRITE.                                         DB1034.2
045400     MOVE "DEBUG-CONTENTS; SEE NEXT LINE" TO RE-MARK.             DB1034.2
045500     MOVE "<===  DEBUG-CONTENTS" TO CORRECT-A.                    DB1034.2
045600     MOVE DBCONT-HOLD TO COMPUTED-A.                              DB1034.2
045700     GO TO   GO-TO-WRITE.                                         DB1034.2
045800 GO-TO-DELETE.                                                    DB1034.2
045900     PERFORM DE-LETE.                                             DB1034.2
046000 GO-TO-WRITE.                                                     DB1034.2
046100     MOVE "DEBUG ON PROC-NAME" TO FEATURE.                        DB1034.2
046200     MOVE "GO-TO-TEST" TO PAR-NAME.                               DB1034.2
046300     PERFORM PRINT-DETAIL.                                        DB1034.2
046400     MOVE 0 TO RESULT-FLAG.                                       DB1034.2
046500 ALTER-TEST-INIT.                                                 DB1034.2
046600     ALTER ALTERABLE-PARAGRAPH TO PROCEED TO ALTERED-GO-TO-TEST.  DB1034.2
046700 ALTER-TEST.                                                      DB1034.2
046800     IF RESULT-FLAG IS NOT EQUAL TO 0                             DB1034.2
046900         MOVE "USE PROCEDURE EXECUTED" TO RE-MARK                 DB1034.2
047000         PERFORM FAIL                                             DB1034.2
047100         PERFORM ALTER-WRITE                                      DB1034.2
047200         ELSE PERFORM PASS                                        DB1034.2
047300         GO TO ALTER-WRITE.                                       DB1034.2
047400     MOVE "DEBUG-LINE; SEE NEXT LINE" TO RE-MARK.                 DB1034.2
047500     MOVE "<===  DEBUG-LINE" TO CORRECT-A.                        DB1034.2
047600     MOVE DBLINE-HOLD TO COMPUTED-A.                              DB1034.2
047700     PERFORM ALTER-WRITE.                                         DB1034.2
047800     MOVE "DEBUG-NAME; SEE NEXT LINE" TO RE-MARK.                 DB1034.2
047900     MOVE "<===  DEBUG-NAME" TO CORRECT-A.                        DB1034.2
048000     MOVE DBNAME-HOLD TO COMPUTED-A.                              DB1034.2
048100     PERFORM ALTER-WRITE.                                         DB1034.2
048200     MOVE "DEBUG-CONTENTS; SEE NEXT LINE" TO RE-MARK.             DB1034.2
048300     MOVE "<===  DEBUG-CONTENTS" TO CORRECT-A.                    DB1034.2
048400     MOVE DBCONT-HOLD TO COMPUTED-A.                              DB1034.2
048500     GO TO   ALTER-WRITE.                                         DB1034.2
048600 ALTER-DELETE.                                                    DB1034.2
048700     PERFORM DE-LETE.                                             DB1034.2
048800 ALTER-WRITE.                                                     DB1034.2
048900     MOVE "DEBUG ON PROC-NAME" TO FEATURE.                        DB1034.2
049000     MOVE "ALTER-TEST" TO PAR-NAME.                               DB1034.2
049100     PERFORM PRINT-DETAIL.                                        DB1034.2
049200     MOVE 0 TO RESULT-FLAG.                                       DB1034.2
049300 ALTER-WRITE-END.                                                 DB1034.2
049400     GO TO ALTERABLE-PARAGRAPH.                                   DB1034.2
049500 ALTERED-GO-TO-TEST.                                              DB1034.2
049600     IF RESULT-FLAG IS NOT EQUAL TO 0                             DB1034.2
049700         MOVE "USE PROCEDURE EXECUTED" TO RE-MARK                 DB1034.2
049800         PERFORM FAIL                                             DB1034.2
049900         PERFORM ALTERED-GO-TO-WRITE                              DB1034.2
050000         ELSE PERFORM PASS                                        DB1034.2
050100         GO TO ALTERED-GO-TO-WRITE.                               DB1034.2
050200     MOVE "DEBUG-LINE; SEE NEXT LINE" TO RE-MARK.                 DB1034.2
050300     MOVE "<===  DEBUG-LINE" TO CORRECT-A.                        DB1034.2
050400     MOVE DBLINE-HOLD TO COMPUTED-A.                              DB1034.2
050500     PERFORM ALTERED-GO-TO-WRITE.                                 DB1034.2
050600     MOVE "DEBUG-NAME; SEE NEXT LINE" TO RE-MARK.                 DB1034.2
050700     MOVE "<===  DEBUG-NAME" TO CORRECT-A.                        DB1034.2
050800     MOVE DBNAME-HOLD TO COMPUTED-A.                              DB1034.2
050900     PERFORM ALTERED-GO-TO-WRITE.                                 DB1034.2
051000     MOVE "DEBUG-CONTENTS; SEE NEXT LINE" TO RE-MARK.             DB1034.2
051100     MOVE "<===  DEBUG-CONTENTS" TO CORRECT-A.                    DB1034.2
051200     MOVE DBCONT-HOLD TO COMPUTED-A.                              DB1034.2
051300     GO TO   ALTERED-GO-TO-WRITE.                                 DB1034.2
051400 ALTERED-GO-TO-DELETE.                                            DB1034.2
051500     PERFORM DE-LETE.                                             DB1034.2
051600 ALTERED-GO-TO-WRITE.                                             DB1034.2
051700     MOVE "DEBUG ON PROC-NAME" TO FEATURE.                        DB1034.2
051800     MOVE "ALTERED-GO-TO-TEST" TO PAR-NAME.                       DB1034.2
051900     PERFORM PRINT-DETAIL.                                        DB1034.2
052000     MOVE 0 TO RESULT-FLAG.                                       DB1034.2
052100 PERF-ITERATION-TEST.                                             DB1034.2
052200     PERFORM LOOP-ROUTINE FIVE TIMES.                             DB1034.2
052300     IF RESULT-FLAG IS NOT EQUAL TO 0                             DB1034.2
052400         MOVE "USE PROCEDURE EXECUTED" TO RE-MARK                 DB1034.2
052500         PERFORM FAIL                                             DB1034.2
052600         PERFORM PERF-ITERATION-WRITE                             DB1034.2
052700         ELSE  PERFORM PASS                                       DB1034.2
052800         GO TO PERF-ITERATION-WRITE.                              DB1034.2
052900     MOVE "DEBUG-LINE; SEE NEXT LINE" TO RE-MARK.                 DB1034.2
053000     MOVE "<===  DEBUG-LINE" TO CORRECT-A.                        DB1034.2
053100     MOVE DBLINE-HOLD TO COMPUTED-A.                              DB1034.2
053200     PERFORM PERF-ITERATION-WRITE.                                DB1034.2
053300     MOVE "DEBUG-NAME; SEE NEXT LINE" TO RE-MARK.                 DB1034.2
053400     MOVE "<===  DEBUG-NAME" TO CORRECT-A.                        DB1034.2
053500     MOVE DBNAME-HOLD TO COMPUTED-A.                              DB1034.2
053600     PERFORM PERF-ITERATION-WRITE.                                DB1034.2
053700     MOVE "DEBUG-CONTENTS; SEE NEXT LINE" TO RE-MARK.             DB1034.2
053800     MOVE "<===  DEBUG-CONTENTS" TO CORRECT-A.                    DB1034.2
053900     MOVE DBCONT-HOLD TO COMPUTED-A.                              DB1034.2
054000     GO TO   PERF-ITERATION-WRITE.                                DB1034.2
054100 PERF-ITERATION-DELETE.                                           DB1034.2
054200     PERFORM DE-LETE.                                             DB1034.2
054300 PERF-ITERATION-WRITE.                                            DB1034.2
054400     MOVE "PERF-ITERATION-TEST" TO PAR-NAME.                      DB1034.2
054500     MOVE "DEBUG ON PROC-NAME" TO FEATURE.                        DB1034.2
054600     PERFORM PRINT-DETAIL.                                        DB1034.2
054700     MOVE 0 TO RESULT-FLAG.                                       DB1034.2
054800 PERF-ITERATION-END.                                              DB1034.2
054900     GO TO PERFORM-THRU-TEST.                                     DB1034.2
055000 LOOP-ROUTINE.                                                    DB1034.2
055100**NESTED PERFORMS ARE USED HERE TO ATTEMPT TO PREVENT OPTIMIZER   DB1034.2
055200* ACTION RESULTING IN LOOP UNFOLDING AND REDUCTION.               DB1034.2
055300     PERFORM DO-NOTHING.                                          DB1034.2
055400 DO-NOTHING.                                                      DB1034.2
055500     ADD A B C GIVING D.                                          DB1034.2
055600 DO-NOTHING-1.                                                    DB1034.2
055700     SUBTRACT A FROM B.                                           DB1034.2
055800 PERFORM-THRU-TEST.                                               DB1034.2
055900     PERFORM DO-NOTHING THRU DO-NOTHING-1 FIVE TIMES.             DB1034.2
056000     IF RESULT-FLAG IS NOT EQUAL TO 0                             DB1034.2
056100         MOVE "USE PROCEDURE EXECUTED" TO RE-MARK                 DB1034.2
056200         PERFORM FAIL                                             DB1034.2
056300         PERFORM PERFORM-THRU-WRITE                               DB1034.2
056400         ELSE PERFORM PASS                                        DB1034.2
056500         GO TO PERFORM-THRU-WRITE.                                DB1034.2
056600     MOVE "DEBUG-LINE; SEE NEXT LINE" TO RE-MARK.                 DB1034.2
056700     MOVE "<===  DEBUG-LINE" TO CORRECT-A.                        DB1034.2
056800     MOVE DBLINE-HOLD TO COMPUTED-A.                              DB1034.2
056900     PERFORM PERFORM-THRU-WRITE.                                  DB1034.2
057000     MOVE "DEBUG-NAME; SEE NEXT LINE" TO RE-MARK.                 DB1034.2
057100     MOVE "<===  DEBUG-NAME" TO CORRECT-A.                        DB1034.2
057200     MOVE DBNAME-HOLD TO COMPUTED-A.                              DB1034.2
057300     PERFORM PERFORM-THRU-WRITE.                                  DB1034.2
057400     MOVE "DEBUG-CONTENTS; SEE NEXT LINE" TO RE-MARK.             DB1034.2
057500     MOVE "<===  DEBUG-CONTENTS" TO CORRECT-A.                    DB1034.2
057600     MOVE DBCONT-HOLD TO COMPUTED-A.                              DB1034.2
057700     GO TO   PERFORM-THRU-WRITE.                                  DB1034.2
057800 PERFORM-THRU-DELETE.                                             DB1034.2
057900     PERFORM DE-LETE.                                             DB1034.2
058000 PERFORM-THRU-WRITE.                                              DB1034.2
058100     MOVE "PERFORM-THRU-TEST" TO PAR-NAME.                        DB1034.2
058200     MOVE "DEBUG ON PROC-NAME" TO FEATURE.                        DB1034.2
058300     PERFORM PRINT-DETAIL.                                        DB1034.2
058400     MOVE 0 TO RESULT-FLAG.                                       DB1034.2
058500 SIMPLE-PERFORM-TEST.                                             DB1034.2
058600     PERFORM LOOP-ROUTINE.                                        DB1034.2
058700     IF RESULT-FLAG IS NOT EQUAL TO 0                             DB1034.2
058800         MOVE "USE PROCEDURE EXECUTED" TO RE-MARK                 DB1034.2
058900         PERFORM FAIL                                             DB1034.2
059000         PERFORM SIMPLE-PERFORM-WRITE                             DB1034.2
059100         ELSE PERFORM PASS                                        DB1034.2
059200         GO TO SIMPLE-PERFORM-WRITE.                              DB1034.2
059300     MOVE "DEBUG-LINE; SEE NEXT LINE" TO RE-MARK.                 DB1034.2
059400     MOVE "<===  DEBUG-LINE" TO CORRECT-A.                        DB1034.2
059500     MOVE DBLINE-HOLD TO COMPUTED-A.                              DB1034.2
059600     PERFORM SIMPLE-PERFORM-WRITE.                                DB1034.2
059700     MOVE "DEBUG-NAME; SEE NEXT LINE" TO RE-MARK.                 DB1034.2
059800     MOVE "<===  DEBUG-NAME" TO CORRECT-A.                        DB1034.2
059900     MOVE DBNAME-HOLD TO COMPUTED-A.                              DB1034.2
060000     PERFORM SIMPLE-PERFORM-WRITE.                                DB1034.2
060100     MOVE "DEBUG-CONTENTS; SEE NEXT LINE" TO RE-MARK.             DB1034.2
060200     MOVE "<===  DEBUG-CONTENTS" TO CORRECT-A.                    DB1034.2
060300     MOVE DBCONT-HOLD TO COMPUTED-A.                              DB1034.2
060400     GO TO   SIMPLE-PERFORM-WRITE.                                DB1034.2
060500 SIMPLE-PERFORM-DELETE.                                           DB1034.2
060600     PERFORM DE-LETE.                                             DB1034.2
060700 SIMPLE-PERFORM-WRITE.                                            DB1034.2
060800     MOVE "SIMPLE-PERFORM-TEST" TO PAR-NAME.                      DB1034.2
060900     MOVE "DEBUG ON PROC-NAME" TO FEATURE.                        DB1034.2
061000     PERFORM PRINT-DETAIL.                                        DB1034.2
061100     MOVE 0 TO RESULT-FLAG.                                       DB1034.2
061200 DEBUG-LINE-TESTS-INIT.                                           DB1034.2
061300     MOVE "DEBUG LINE TESTS" TO FEATURE.                          DB1034.2
061400 DEBUG-LINE-TEST-01.                                              DB1034.2
061500     MOVE "COMPLETE ENTITY" TO RE-MARK.                           DB1034.2
061600     PERFORM PASS.                                                DB1034.2
061700D    PERFORM FAIL.                                                DB1034.2
061800     GO TO DEBUG-LINE-WRITE-01.                                   DB1034.2
061900 DEBUG-LINE-DELETE-01.                                            DB1034.2
062000     PERFORM DE-LETE.                                             DB1034.2
062100 DEBUG-LINE-WRITE-01.                                             DB1034.2
062200     MOVE "DEBUG-LINE-TEST-01" TO PAR-NAME.                       DB1034.2
062300     PERFORM PRINT-DETAIL.                                        DB1034.2
062400 DEBUG-LINE-TEST-02.                                              DB1034.2
062500     MOVE "CONSECUTIVE DEBUG LINES" TO RE-MARK.                   DB1034.2
062600     PERFORM PASS.                                                DB1034.2
062700D    PERFORM FAIL.                                                DB1034.2
062800D    SUBTRACT 1 FROM D.                                           DB1034.2
062900     GO TO DEBUG-LINE-WRITE-02.                                   DB1034.2
063000 DEBUG-LINE-DELETE-02.                                            DB1034.2
063100     PERFORM DE-LETE.                                             DB1034.2
063200 DEBUG-LINE-WRITE-02.                                             DB1034.2
063300     MOVE "DEBUG-LINE-TEST-02" TO PAR-NAME.                       DB1034.2
063400     PERFORM PRINT-DETAIL.                                        DB1034.2
063500 DEBUG-LINE-TEST-03.                                              DB1034.2
063600     MOVE "BROKEN STATEMENTS" TO RE-MARK.                         DB1034.2
063700     PERFORM                                                      DB1034.2
063800D        FAIL.   GO TO DEBUG-LINE-WRITE-03.                       DB1034.2
063900DDEBUG-LINE-TEST-03A.     PERFORM                                 DB1034.2
064000                             PASS.                                DB1034.2
064100                             GO TO DEBUG-LINE-WRITE-03.           DB1034.2
064200 DEBUG-LINE-DELETE-03.                                            DB1034.2
064300     PERFORM DE-LETE.                                             DB1034.2
064400 DEBUG-LINE-WRITE-03.                                             DB1034.2
064500     MOVE "DEBUG-LINE-TEST-03" TO PAR-NAME.                       DB1034.2
064600     PERFORM PRINT-DETAIL.                                        DB1034.2
064700 DEBUG-LINE-TEST-04.                                              DB1034.2
064800     MOVE "NESTED COMMENTS" TO RE-MARK.                           DB1034.2
064900D    PERFORM FAIL.                                                DB1034.2
065000*    PERFORM FAIL.  MOVE "COMMENTS EXECUTED" TO COMPUTED-A.       DB1034.2
065100*    GO TO DEBUG-LINE-WRITE-04.                                   DB1034.2
065200*DEBUG-LINE-TEST-04-A.                                            DB1034.2
065300D    GO TO DEBUG-LINE-WRITE-04.                                   DB1034.2
065400 DEBUG-LINE-TEST-04-B.                                            DB1034.2
065500     PERFORM PASS.                                                DB1034.2
065600     GO TO DEBUG-LINE-WRITE-04.                                   DB1034.2
065700 DEBUG-LINE-DELETE-04.                                            DB1034.2
065800     PERFORM DE-LETE.                                             DB1034.2
065900 DEBUG-LINE-WRITE-04.                                             DB1034.2
066000     MOVE "DEBUG-LINE-TEST-04" TO PAR-NAME.                       DB1034.2
066100     PERFORM PRINT-DETAIL.                                        DB1034.2
066200 DEBUG-LINE-TEST-05.                                              DB1034.2
066300     MOVE "NESTED INSIDE COMMENTS" TO RE-MARK.                    DB1034.2
066400*    PERFORM FAIL.  MOVE "COMMENTS EXECUTED" TO COMPUTED-A.       DB1034.2
066500*    GO TO DEBUG-LINE-WRITE-05.                                   DB1034.2
066600DDEBUG-LINE-TEST-05-A.                                            DB1034.2
066700D    PERFORM FAIL.                                                DB1034.2
066800D    GO TO DEBUG-LINE-WRITE-05.                                   DB1034.2
066900*DEBUG-LINE-TEST-05-B.                                            DB1034.2
067000*    MOVE "    FAILURE 05B" TO COMPUTED-A.                        DB1034.2
067100*    PERFORM FAIL.  GO TO DEBUG-LINE-WRITE-05.                    DB1034.2
067200 DEBUG-LINE-TEST-05-C.                                            DB1034.2
067300     PERFORM PASS.                                                DB1034.2
067400     GO TO DEBUG-LINE-WRITE-05.                                   DB1034.2
067500 DEBUG-LINE-DELETE-05.                                            DB1034.2
067600     PERFORM DE-LETE.                                             DB1034.2
067700 DEBUG-LINE-WRITE-05.                                             DB1034.2
067800     MOVE "DEBUG-LINE-TEST-05" TO PAR-NAME.                       DB1034.2
067900     PERFORM PRINT-DETAIL.                                        DB1034.2
068000 CCVS-EXIT SECTION.                                               DB1034.2
068100 CCVS-999999.                                                     DB1034.2
068200     GO TO CLOSE-FILES.                                           DB1034.2
      *END-OF,DB103M                                                                  
