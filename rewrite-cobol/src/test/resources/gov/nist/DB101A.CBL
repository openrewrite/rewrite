      *HEADER,COBOL,DB101A                                                            
000100 IDENTIFICATION DIVISION.                                         DB1014.2
000200 PROGRAM-ID.                                                      DB1014.2
000300     DB101A.                                                      DB1014.2
000400 AUTHOR.                                                          DB1014.2
000500     FEDERAL COMPILER TESTING CENTER.                             DB1014.2
000600 INSTALLATION.                                                    DB1014.2
000700     GENERAL SERVICES ADMINISTRATION                              DB1014.2
000800*     AUTOMATED DATA AND TELECOMMUNICATION SERVICE.                DB1014.2
000900     SOFTWARE DEVELOPMENT OFFICE.                                 DB1014.2
001000     5203 LEESBURG PIKE  SUITE 1100                               DB1014.2
001100     FALLS CHURCH VIRGINIA 22041.                                 DB1014.2
001200                                                                  DB1014.2
001300     PHONE   (703) 756-6153                                       DB1014.2
001400                                                                  DB1014.2
001500     " HIGH       ".                                              DB1014.2
001600 DATE-WRITTEN.                                                    DB1014.2
001700     CCVS-74 VERSION 4.0 - 1980 JULY 1.                           DB1014.2
001800     CREATION DATE     /    VALIDATION DATE                       DB1014.2
001900     "4.2 ".                                                      DB1014.2
002000 SECURITY.                                                        DB1014.2
002100     NONE.                                                        DB1014.2
002200*                                                                 DB1014.2
002300*    *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *DB1014.2
002400*                                                                 DB1014.2
002500*                       PROGRAM ABSTRACT                          DB1014.2
002600*                                                                 DB1014.2
002700*    DB101A TESTS THE BASIC OPERATION OF THE DEBUG MODULE WHEN    DB1014.2
002800*    BOTH THE COMPILE AND OBJECT TIME DEBUGGING SWITCHES ARE      DB1014.2
002900*    TURNED ON.  THE PROGRAM CONTAINS BOTH DEBUG LINES AND SIMPLE DB1014.2
003000*    DEBUGGING PROCEDURES.  THE DEBUGGING PROCEDURES ARE SPECI-   DB1014.2
003100*    FIED FOR PROCEDURE-NAMES AND PROCEDURE-NAME SERIES.  THE     DB1014.2
003200*    FOLLOWING CONDITIONS ARE EVALUATED FOR THE "DEBUG-ITEM"      DB1014.2
003300*    REGISTER                                                     DB1014.2
003400*                                                                 DB1014.2
003500*        (1)  START OF PROGRAM                                    DB1014.2
003600*        (2)  REFERENCE BY "ALTER"                                DB1014.2
003700*        (3)  REFERENCE BY "GO TO"                                DB1014.2
003800*        (4)  REFERENCE BY "PERFORM"                              DB1014.2
003900*        (5)  SEQUENTIAL PASSAGE OF CONTROL  (FALL THROUGH)       DB1014.2
004000*                                                                 DB1014.2
004100*    BEFORE BEGINNING EXECUTION OF THE OBJECT PROGRAM,            DB1014.2
004200*    WHATEVER JOB CONTROL LANGUAGE IS NECESSARY TO ACTIVATE       DB1014.2
004300*    (TURN ON) THE OBJECT TIME DEBUGGING SWITCH SHOULD BE         DB1014.2
004400*    SUBMITTED.                                                   DB1014.2
004500*                                                                 DB1014.2
004600*                                                                 DB1014.2
004700*                                                                 DB1014.2
004800*                                                                 DB1014.2
004900 ENVIRONMENT DIVISION.                                            DB1014.2
005000 CONFIGURATION SECTION.                                           DB1014.2
005100 SOURCE-COMPUTER.                                                 DB1014.2
005200     XXXXX082                                                     DB1014.2
005300         WITH DEBUGGING MODE.                                     DB1014.2
005400 OBJECT-COMPUTER.                                                 DB1014.2
005500     XXXXX083.                                                    DB1014.2
005600 INPUT-OUTPUT SECTION.                                            DB1014.2
005700 FILE-CONTROL.                                                    DB1014.2
005800     SELECT PRINT-FILE ASSIGN TO                                  DB1014.2
005900     XXXXX055.                                                    DB1014.2
006000 DATA DIVISION.                                                   DB1014.2
006100 FILE SECTION.                                                    DB1014.2
006200 FD  PRINT-FILE                                                   DB1014.2
006300     LABEL RECORDS                                                DB1014.2
006400     XXXXX084                                                     DB1014.2
006500     DATA RECORD IS PRINT-REC DUMMY-RECORD.                       DB1014.2
006600 01  PRINT-REC PICTURE X(120).                                    DB1014.2
006700 01  DUMMY-RECORD PICTURE X(120).                                 DB1014.2
006800 WORKING-STORAGE SECTION.                                         DB1014.2
006900 77  A PIC 9 COMP VALUE 1.                                        DB1014.2
007000 77  B PIC 9 COMP VALUE 5.                                        DB1014.2
007100 77  C PIC 9 COMP VALUE 9.                                        DB1014.2
007200 77  D PIC 99 COMP.                                               DB1014.2
007300 77  RESULT-FLAG PIC 99 VALUE 0.                                  DB1014.2
007400 77  DBLINE-HOLD PIC X(6).                                        DB1014.2
007500 77  DBNAME-HOLD PIC X(30).                                       DB1014.2
007600 77  DBCONT-HOLD PIC X(30).                                       DB1014.2
007700 77  FIVE PIC 9 COMP VALUE 5.                                     DB1014.2
007800 01  SIZE-19.                                                     DB1014.2
007900     02  FILLER PIC X.                                            DB1014.2
008000     02  SIZE-18.                                                 DB1014.2
008100         03  FILLER PIC X.                                        DB1014.2
008200         03  SIZE-17.                                             DB1014.2
008300             04  FILLER PIC X.                                    DB1014.2
008400             04  SIZE-16.                                         DB1014.2
008500                 05  FILLER PIC X.                                DB1014.2
008600                 05  SIZE-15.                                     DB1014.2
008700                     06  FILLER PIC X.                            DB1014.2
008800                     06  SIZE-14.                                 DB1014.2
008900                         07  FILLER PIC X.                        DB1014.2
009000                         07  SIZE-13.                             DB1014.2
009100                             08  FILLER PIC X.                    DB1014.2
009200                             08  SIZE-12.                         DB1014.2
009300                                 09  FILLER PIC XX.               DB1014.2
009400                                 09  SIZE-10.                     DB1014.2
009500                                     10  FILLER PICTURE X(5).     DB1014.2
009600                                     10  SIZE-5 PICTURE X(5).     DB1014.2
009700 01  TEST-RESULTS.                                                DB1014.2
009800     02 FILLER                    PICTURE X VALUE SPACE.          DB1014.2
009900     02 FEATURE                   PICTURE X(20) VALUE SPACE.      DB1014.2
010000     02 FILLER                    PICTURE X VALUE SPACE.          DB1014.2
010100     02 P-OR-F                    PICTURE X(5) VALUE SPACE.       DB1014.2
010200     02 FILLER                    PICTURE X  VALUE SPACE.         DB1014.2
010300     02  PAR-NAME.                                                DB1014.2
010400       03 FILLER PICTURE X(12) VALUE SPACE.                       DB1014.2
010500       03  PARDOT-X PICTURE X  VALUE SPACE.                       DB1014.2
010600       03 DOTVALUE PICTURE 99  VALUE ZERO.                        DB1014.2
010700       03 FILLER PIC X(5) VALUE SPACE.                            DB1014.2
010800     02 FILLER PIC X(10) VALUE SPACE.                             DB1014.2
010900     02 RE-MARK PIC X(61).                                        DB1014.2
011000 01  TEST-COMPUTED.                                               DB1014.2
011100     02 FILLER PIC X(30) VALUE SPACE.                             DB1014.2
011200     02 FILLER PIC X(17) VALUE "       COMPUTED=".                DB1014.2
011300     02 COMPUTED-X.                                               DB1014.2
011400     03 COMPUTED-A                PICTURE X(20) VALUE SPACE.      DB1014.2
011500     03 COMPUTED-N REDEFINES COMPUTED-A PICTURE -9(9).9(9).       DB1014.2
011600     03 COMPUTED-0V18 REDEFINES COMPUTED-A  PICTURE -.9(18).      DB1014.2
011700     03 COMPUTED-4V14 REDEFINES COMPUTED-A  PICTURE -9(4).9(14).  DB1014.2
011800     03 COMPUTED-14V4 REDEFINES COMPUTED-A  PICTURE -9(14).9(4).  DB1014.2
011900     03       CM-18V0 REDEFINES COMPUTED-A.                       DB1014.2
012000         04 COMPUTED-18V0                   PICTURE -9(18).       DB1014.2
012100         04 FILLER                          PICTURE X.            DB1014.2
012200     03 FILLER PIC X(50) VALUE SPACE.                             DB1014.2
012300 01  TEST-CORRECT.                                                DB1014.2
012400     02 FILLER PIC X(30) VALUE SPACE.                             DB1014.2
012500     02 FILLER PIC X(17) VALUE "       CORRECT =".                DB1014.2
012600     02 CORRECT-X.                                                DB1014.2
012700     03 CORRECT-A                 PICTURE X(20) VALUE SPACE.      DB1014.2
012800     03 CORRECT-N REDEFINES CORRECT-A PICTURE -9(9).9(9).         DB1014.2
012900     03 CORRECT-0V18 REDEFINES CORRECT-A    PICTURE -.9(18).      DB1014.2
013000     03 CORRECT-4V14 REDEFINES CORRECT-A    PICTURE -9(4).9(14).  DB1014.2
013100     03 CORRECT-14V4 REDEFINES CORRECT-A    PICTURE -9(14).9(4).  DB1014.2
013200     03      CR-18V0 REDEFINES CORRECT-A.                         DB1014.2
013300         04 CORRECT-18V0                    PICTURE -9(18).       DB1014.2
013400         04 FILLER                          PICTURE X.            DB1014.2
013500     03 FILLER PIC X(50) VALUE SPACE.                             DB1014.2
013600 01  CCVS-C-1.                                                    DB1014.2
013700     02 FILLER PICTURE IS X(99) VALUE IS " FEATURE              PADB1014.2
013800-    "SS  PARAGRAPH-NAME                                          DB1014.2
013900-    "        REMARKS".                                           DB1014.2
014000     02 FILLER PICTURE IS X(20) VALUE IS SPACE.                   DB1014.2
014100 01  CCVS-C-2.                                                    DB1014.2
014200     02 FILLER PICTURE IS X VALUE IS SPACE.                       DB1014.2
014300     02 FILLER PICTURE IS X(6) VALUE IS "TESTED".                 DB1014.2
014400     02 FILLER PICTURE IS X(15) VALUE IS SPACE.                   DB1014.2
014500     02 FILLER PICTURE IS X(4) VALUE IS "FAIL".                   DB1014.2
014600     02 FILLER PICTURE IS X(94) VALUE IS SPACE.                   DB1014.2
014700 01  REC-SKL-SUB PICTURE 9(2) VALUE ZERO.                         DB1014.2
014800 01  REC-CT PICTURE 99 VALUE ZERO.                                DB1014.2
014900 01  DELETE-CNT                   PICTURE 999  VALUE ZERO.        DB1014.2
015000 01  ERROR-COUNTER PICTURE IS 999 VALUE IS ZERO.                  DB1014.2
015100 01  INSPECT-COUNTER PIC 999 VALUE ZERO.                          DB1014.2
015200 01  PASS-COUNTER PIC 999 VALUE ZERO.                             DB1014.2
015300 01  TOTAL-ERROR PIC 999 VALUE ZERO.                              DB1014.2
015400 01  ERROR-HOLD PIC 999 VALUE ZERO.                               DB1014.2
015500 01  DUMMY-HOLD PIC X(120) VALUE SPACE.                           DB1014.2
015600 01  RECORD-COUNT PIC 9(5) VALUE ZERO.                            DB1014.2
015700 01  CCVS-H-1.                                                    DB1014.2
015800     02  FILLER   PICTURE X(27)  VALUE SPACE.                     DB1014.2
015900     02 FILLER PICTURE X(67) VALUE                                DB1014.2
016000     " FEDERAL COMPILER TESTING CENTER COBOL COMPILER VALIDATION  DB1014.2
016100-    " SYSTEM".                                                   DB1014.2
016200     02  FILLER     PICTURE X(26)  VALUE SPACE.                   DB1014.2
016300 01  CCVS-H-2.                                                    DB1014.2
016400     02 FILLER PICTURE X(52) VALUE IS                             DB1014.2
016500     "CCVS74 NCC  COPY, NOT FOR DISTRIBUTION.".                   DB1014.2
016600     02 FILLER PICTURE IS X(19) VALUE IS "TEST RESULTS SET-  ".   DB1014.2
016700     02 TEST-ID PICTURE IS X(9).                                  DB1014.2
016800     02 FILLER PICTURE IS X(40) VALUE IS SPACE.                   DB1014.2
016900 01  CCVS-H-3.                                                    DB1014.2
017000     02  FILLER PICTURE X(34) VALUE                               DB1014.2
017100     " FOR OFFICIAL USE ONLY    ".                                DB1014.2
017200     02  FILLER PICTURE X(58) VALUE                               DB1014.2
017300     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".DB1014.2
017400     02  FILLER PICTURE X(28) VALUE                               DB1014.2
017500     "  COPYRIGHT   1974 ".                                       DB1014.2
017600 01  CCVS-E-1.                                                    DB1014.2
017700     02 FILLER PICTURE IS X(52) VALUE IS SPACE.                   DB1014.2
017800     02 FILLER PICTURE IS X(14) VALUE IS "END OF TEST-  ".        DB1014.2
017900     02 ID-AGAIN PICTURE IS X(9).                                 DB1014.2
018000     02 FILLER PICTURE X(45) VALUE IS                             DB1014.2
018100     " NTIS DISTRIBUTION COBOL 74".                               DB1014.2
018200 01  CCVS-E-2.                                                    DB1014.2
018300     02  FILLER                   PICTURE X(31)  VALUE            DB1014.2
018400     SPACE.                                                       DB1014.2
018500     02  FILLER                   PICTURE X(21)  VALUE SPACE.     DB1014.2
018600     02 CCVS-E-2-2.                                               DB1014.2
018700         03 ERROR-TOTAL PICTURE IS XXX VALUE IS SPACE.            DB1014.2
018800         03 FILLER PICTURE IS X VALUE IS SPACE.                   DB1014.2
018900         03 ENDER-DESC PIC X(44) VALUE "ERRORS ENCOUNTERED".      DB1014.2
019000 01  CCVS-E-3.                                                    DB1014.2
019100     02  FILLER PICTURE X(22) VALUE                               DB1014.2
019200     " FOR OFFICIAL USE ONLY".                                    DB1014.2
019300     02  FILLER PICTURE X(12) VALUE SPACE.                        DB1014.2
019400     02  FILLER PICTURE X(58) VALUE                               DB1014.2
019500     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".DB1014.2
019600     02  FILLER PICTURE X(13) VALUE SPACE.                        DB1014.2
019700     02 FILLER PIC X(15) VALUE " COPYRIGHT 1974".                 DB1014.2
019800 01  CCVS-E-4.                                                    DB1014.2
019900     02 CCVS-E-4-1 PIC XXX VALUE SPACE.                           DB1014.2
020000     02 FILLER PIC XXXX VALUE " OF ".                             DB1014.2
020100     02 CCVS-E-4-2 PIC XXX VALUE SPACE.                           DB1014.2
020200     02 FILLER PIC X(40) VALUE                                    DB1014.2
020300      "  TESTS WERE EXECUTED SUCCESSFULLY".                       DB1014.2
020400 01  XXINFO.                                                      DB1014.2
020500     02 FILLER PIC X(30) VALUE "        *** INFORMATION  ***".    DB1014.2
020600     02 INFO-TEXT.                                                DB1014.2
020700     04 FILLER PIC X(20) VALUE SPACE.                             DB1014.2
020800     04 XXCOMPUTED PIC X(20).                                     DB1014.2
020900     04 FILLER PIC X(5) VALUE SPACE.                              DB1014.2
021000     04 XXCORRECT PIC X(20).                                      DB1014.2
021100 01  HYPHEN-LINE.                                                 DB1014.2
021200     02 FILLER PICTURE IS X VALUE IS SPACE.                       DB1014.2
021300     02 FILLER PICTURE IS X(65) VALUE IS "************************DB1014.2
021400-    "*****************************************".                 DB1014.2
021500     02 FILLER PICTURE IS X(54) VALUE IS "************************DB1014.2
021600-    "******************************".                            DB1014.2
021700 01  CCVS-PGM-ID PIC X(6) VALUE                                   DB1014.2
021800     "DB101A".                                                    DB1014.2
021900 PROCEDURE DIVISION.                                              DB1014.2
022000 DECLARATIVES.                                                    DB1014.2
022100 START-UP SECTION.                                                DB1014.2
022200     USE FOR DEBUGGING ON CCVS1.                                  DB1014.2
022300 BEGIN-START-UP.                                                  DB1014.2
022400     MOVE 1 TO RESULT-FLAG.                                       DB1014.2
022500 DB-COMMON.                                                       DB1014.2
022600     MOVE DEBUG-LINE TO DBLINE-HOLD.                              DB1014.2
022700     MOVE DEBUG-NAME TO DBNAME-HOLD.                              DB1014.2
022800     MOVE DEBUG-CONTENTS TO DBCONT-HOLD.                          DB1014.2
022900 FALL-THROUGH-AND-SERIES SECTION.                                 DB1014.2
023000     USE FOR DEBUGGING ON FALL-THROUGH-TEST                       DB1014.2
023100              PROC-SERIES-TEST.                                   DB1014.2
023200 BEGIN-FALL-THROUGH-AND-SERIES.                                   DB1014.2
023300     PERFORM DB-COMMON.                                           DB1014.2
023400     MOVE 2 TO RESULT-FLAG.                                       DB1014.2
023500 GO-TO SECTION.                                                   DB1014.2
023600     USE FOR DEBUGGING ON GO-TO-TEST.                             DB1014.2
023700 BEGIN-GO-TO.                                                     DB1014.2
023800     PERFORM DB-COMMON.                                           DB1014.2
023900     MOVE 3 TO RESULT-FLAG.                                       DB1014.2
024000 ALTER-PARAGRAPH SECTION.                                         DB1014.2
024100     USE FOR DEBUGGING ON ALTERABLE-PARAGRAPH.                    DB1014.2
024200 BEGIN-ALTER-PARAGRAPH.                                           DB1014.2
024300     PERFORM DB-COMMON.                                           DB1014.2
024400     MOVE 4 TO RESULT-FLAG.                                       DB1014.2
024500 LOOP-ITERATION SECTION.                                          DB1014.2
024600     USE FOR DEBUGGING ON LOOP-ROUTINE.                           DB1014.2
024700 BEGIN-LOOP-ITERATION.                                            DB1014.2
024800     PERFORM DB-COMMON.                                           DB1014.2
024900     ADD 1 TO RESULT-FLAG.                                        DB1014.2
025000 PERFORM-THRU SECTION.                                            DB1014.2
025100     USE FOR DEBUGGING ON DO-NOTHING-1.                           DB1014.2
025200 BEGIN-PERFORM-THRU.                                              DB1014.2
025300     PERFORM DB-COMMON.                                           DB1014.2
025400     ADD 1 TO RESULT-FLAG.                                        DB1014.2
025500 END DECLARATIVES.                                                DB1014.2
025600******************************************************************DB1014.2
025700*    THE DEBUG-LINE (INSPT) SUBTEST FOR THE TEST NAMED IN THE    *DB1014.2
025800*    OUTPUT REPORT AS "START-PROGRAM-TEST" SHOULD POINT TO THE   *DB1014.2
025900*    EXECUTABLE STATEMENT WHICH FOLLOWS THIS COMMENT SET AND     *DB1014.2
026000*    WHICH READS, "OPEN OUTPUT PRINT-FILE."                      *DB1014.2
026100******************************************************************DB1014.2
026200 CCVS1 SECTION.                                                   DB1014.2
026300 OPEN-FILES.                                                      DB1014.2
026400     OPEN     OUTPUT PRINT-FILE.                                  DB1014.2
026500     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   DB1014.2
026600     MOVE    SPACE TO TEST-RESULTS.                               DB1014.2
026700     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             DB1014.2
026800     GO TO CCVS1-EXIT.                                            DB1014.2
026900 CLOSE-FILES.                                                     DB1014.2
027000     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   DB1014.2
027100 TERMINATE-CCVS.                                                  DB1014.2
027200*    EXIT PROGRAM.                                                DB1014.2
027300*TERMINATE-CALL.                                                  DB1014.2
027400     STOP     RUN.                                                DB1014.2
027500 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         DB1014.2
027600 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           DB1014.2
027700 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          DB1014.2
027800 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-CNT.          DB1014.2
027900     MOVE "****TEST DELETED****" TO RE-MARK.                      DB1014.2
028000 PRINT-DETAIL.                                                    DB1014.2
028100     IF REC-CT NOT EQUAL TO ZERO                                  DB1014.2
028200             MOVE "." TO PARDOT-X                                 DB1014.2
028300             MOVE REC-CT TO DOTVALUE.                             DB1014.2
028400     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      DB1014.2
028500     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               DB1014.2
028600        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 DB1014.2
028700          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 DB1014.2
028800     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              DB1014.2
028900     MOVE SPACE TO CORRECT-X.                                     DB1014.2
029000     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         DB1014.2
029100     MOVE     SPACE TO RE-MARK.                                   DB1014.2
029200 HEAD-ROUTINE.                                                    DB1014.2
029300     MOVE CCVS-H-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   DB1014.2
029400     MOVE CCVS-H-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.   DB1014.2
029500     MOVE CCVS-H-3 TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.   DB1014.2
029600 COLUMN-NAMES-ROUTINE.                                            DB1014.2
029700     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           DB1014.2
029800     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   DB1014.2
029900     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        DB1014.2
030000 END-ROUTINE.                                                     DB1014.2
030100     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.DB1014.2
030200 END-RTN-EXIT.                                                    DB1014.2
030300     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   DB1014.2
030400 END-ROUTINE-1.                                                   DB1014.2
030500      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      DB1014.2
030600      ERROR-HOLD. ADD DELETE-CNT TO ERROR-HOLD.                   DB1014.2
030700      ADD PASS-COUNTER TO ERROR-HOLD.                             DB1014.2
030800*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   DB1014.2
030900      MOVE PASS-COUNTER TO CCVS-E-4-1.                            DB1014.2
031000      MOVE ERROR-HOLD TO CCVS-E-4-2.                              DB1014.2
031100      MOVE CCVS-E-4 TO CCVS-E-2-2.                                DB1014.2
031200      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           DB1014.2
031300  END-ROUTINE-12.                                                 DB1014.2
031400      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        DB1014.2
031500     IF       ERROR-COUNTER IS EQUAL TO ZERO                      DB1014.2
031600         MOVE "NO " TO ERROR-TOTAL                                DB1014.2
031700         ELSE                                                     DB1014.2
031800         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       DB1014.2
031900     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           DB1014.2
032000     PERFORM WRITE-LINE.                                          DB1014.2
032100 END-ROUTINE-13.                                                  DB1014.2
032200     IF DELETE-CNT IS EQUAL TO ZERO                               DB1014.2
032300         MOVE "NO " TO ERROR-TOTAL  ELSE                          DB1014.2
032400         MOVE DELETE-CNT TO ERROR-TOTAL.                          DB1014.2
032500     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   DB1014.2
032600     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           DB1014.2
032700      IF   INSPECT-COUNTER EQUAL TO ZERO                          DB1014.2
032800          MOVE "NO " TO ERROR-TOTAL                               DB1014.2
032900      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   DB1014.2
033000      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            DB1014.2
033100      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          DB1014.2
033200     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           DB1014.2
033300 WRITE-LINE.                                                      DB1014.2
033400     ADD 1 TO RECORD-COUNT.                                       DB1014.2
033500*    IF RECORD-COUNT GREATER 50                                   DB1014.2
033600*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          DB1014.2
033700*        MOVE SPACE TO DUMMY-RECORD                               DB1014.2
033800*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  DB1014.2
033900*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             DB1014.2
034000*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     DB1014.2
034100*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          DB1014.2
034200*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          DB1014.2
034300*        MOVE ZERO TO RECORD-COUNT.                               DB1014.2
034400     PERFORM WRT-LN.                                              DB1014.2
034500 WRT-LN.                                                          DB1014.2
034600     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               DB1014.2
034700     MOVE SPACE TO DUMMY-RECORD.                                  DB1014.2
034800 BLANK-LINE-PRINT.                                                DB1014.2
034900     PERFORM WRT-LN.                                              DB1014.2
035000 FAIL-ROUTINE.                                                    DB1014.2
035100     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.   DB1014.2
035200     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.    DB1014.2
035300     MOVE "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.    DB1014.2
035400     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     DB1014.2
035500     GO TO FAIL-ROUTINE-EX.                                       DB1014.2
035600 FAIL-ROUTINE-WRITE.                                              DB1014.2
035700     MOVE TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE           DB1014.2
035800     MOVE TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES.   DB1014.2
035900 FAIL-ROUTINE-EX. EXIT.                                           DB1014.2
036000 BAIL-OUT.                                                        DB1014.2
036100     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.       DB1014.2
036200     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.               DB1014.2
036300 BAIL-OUT-WRITE.                                                  DB1014.2
036400     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  DB1014.2
036500     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     DB1014.2
036600 BAIL-OUT-EX. EXIT.                                               DB1014.2
036700 CCVS1-EXIT.                                                      DB1014.2
036800     EXIT.                                                        DB1014.2
036900 START-PROGRAM-TEST.                                              DB1014.2
037000     IF RESULT-FLAG IS NOT EQUAL TO 1                             DB1014.2
037100         MOVE "USE PROCEDURE NOT EXECUTED" TO RE-MARK             DB1014.2
037200         PERFORM FAIL                                             DB1014.2
037300         GO TO START-PROGRAM-WRITE.                               DB1014.2
037400     MOVE "DEBUG-LINE; SEE NEXT LINE" TO RE-MARK.                 DB1014.2
037500     MOVE "<===  DEBUG-LINE" TO CORRECT-A.                        DB1014.2
037600     MOVE DBLINE-HOLD TO COMPUTED-A.                              DB1014.2
037700     PERFORM INSPT.                                               DB1014.2
037800     PERFORM START-PROGRAM-WRITE.                                 DB1014.2
037900     MOVE DBNAME-HOLD TO SIZE-5.                                  DB1014.2
038000     IF SIZE-5 IS EQUAL TO "CCVS1"                                DB1014.2
038100         PERFORM PASS  ELSE                                       DB1014.2
038200         MOVE "CCVS1" TO CORRECT-A                                DB1014.2
038300         MOVE DBNAME-HOLD TO COMPUTED-A                           DB1014.2
038400         PERFORM FAIL.                                            DB1014.2
038500 START-PROGRAM-TEST-1.                                            DB1014.2
038600     MOVE "DEBUG-NAME" TO RE-MARK.                                DB1014.2
038700     PERFORM START-PROGRAM-WRITE.                                 DB1014.2
038800     MOVE DBCONT-HOLD TO SIZE-13.                                 DB1014.2
038900     IF SIZE-13 IS EQUAL TO "START PROGRAM"                       DB1014.2
039000         PERFORM PASS ELSE                                        DB1014.2
039100         MOVE "START PROGRAM" TO CORRECT-A                        DB1014.2
039200         MOVE DBCONT-HOLD TO COMPUTED-A                           DB1014.2
039300         PERFORM FAIL.                                            DB1014.2
039400     MOVE "DEBUG-CONTENTS" TO RE-MARK.                            DB1014.2
039500     GO TO START-PROGRAM-WRITE.                                   DB1014.2
039600 START-PROGRAM-DELETE.                                            DB1014.2
039700     PERFORM DE-LETE.                                             DB1014.2
039800 START-PROGRAM-WRITE.                                             DB1014.2
039900     MOVE "DEBUG ON PROC-NAME" TO FEATURE.                        DB1014.2
040000     MOVE "START-PROGRAM-TEST" TO PAR-NAME.                       DB1014.2
040100     PERFORM PRINT-DETAIL.                                        DB1014.2
040200******************************************************************DB1014.2
040300*    THE DEBUG-LINE (INSPT) SUBTEST FOR THE TEST NAMED IN THE    *DB1014.2
040400*    OUTPUT REPORT AS "FALL-THROUGH-TEST" SHOULD POINT TO THE    *DB1014.2
040500*    EXECUTABLE STATEMENT WHICH FOLLOWS THIS COMMENT SET AND     *DB1014.2
040600*    WHICH READS, "MOVE 0 TO RESULT-FLAG."                       *DB1014.2
040700******************************************************************DB1014.2
040800     MOVE 0 TO RESULT-FLAG.                                       DB1014.2
040900 FALL-THROUGH-TEST.                                               DB1014.2
041000     IF RESULT-FLAG IS NOT EQUAL TO 2                             DB1014.2
041100         MOVE "USE PROCEDURE NOT EXECUTED" TO RE-MARK             DB1014.2
041200         PERFORM FAIL                                             DB1014.2
041300         GO TO FALL-THROUGH-WRITE.                                DB1014.2
041400     MOVE "DEBUG-LINE; SEE NEXT LINE" TO RE-MARK.                 DB1014.2
041500     MOVE "<===  DEBUG-LINE" TO CORRECT-A.                        DB1014.2
041600     MOVE DBLINE-HOLD TO COMPUTED-A.                              DB1014.2
041700     PERFORM INSPT.                                               DB1014.2
041800     PERFORM FALL-THROUGH-WRITE.                                  DB1014.2
041900     MOVE DBNAME-HOLD TO SIZE-17.                                 DB1014.2
042000     IF SIZE-17 IS EQUAL TO "FALL-THROUGH-TEST"                   DB1014.2
042100         PERFORM PASS ELSE                                        DB1014.2
042200         MOVE "FALL-THROUGH-TEST" TO CORRECT-A                    DB1014.2
042300         MOVE DBNAME-HOLD TO COMPUTED-A                           DB1014.2
042400         PERFORM FAIL.                                            DB1014.2
042500     MOVE "DEBUG-NAME" TO RE-MARK.                                DB1014.2
042600     PERFORM FALL-THROUGH-WRITE.                                  DB1014.2
042700     MOVE DBCONT-HOLD TO SIZE-12.                                 DB1014.2
042800     IF SIZE-12 IS EQUAL TO "FALL THROUGH"                        DB1014.2
042900         PERFORM PASS ELSE                                        DB1014.2
043000         MOVE "FALL THROUGH" TO CORRECT-A                         DB1014.2
043100         MOVE DBCONT-HOLD TO COMPUTED-A                           DB1014.2
043200         PERFORM FAIL.                                            DB1014.2
043300     MOVE "DEBUG-CONTENTS" TO RE-MARK.                            DB1014.2
043400     GO TO FALL-THROUGH-WRITE.                                    DB1014.2
043500 FALL-THROUGH-DELETE.                                             DB1014.2
043600     PERFORM DE-LETE.                                             DB1014.2
043700 FALL-THROUGH-WRITE.                                              DB1014.2
043800     MOVE "DEBUG ON PROC-NAME" TO FEATURE.                        DB1014.2
043900     MOVE "FALL-THROUGH-TEST" TO PAR-NAME.                        DB1014.2
044000     PERFORM PRINT-DETAIL.                                        DB1014.2
044100******************************************************************DB1014.2
044200*    THE DEBUG-LINE (INSPT) SUBTEST FOR THE TEST NAMED IN THE    *DB1014.2
044300*    OUTPUT REPORT AS "PROC-SERIES-TEST" SHOULD POINT TO THE     *DB1014.2
044400*    EXECUTABLE STATEMENT WHICH FOLLOWS THIS COMMENT SET AND     *DB1014.2
044500*    WHICH READS, "MOVE 0 TO RESULT-FLAG."                       *DB1014.2
044600******************************************************************DB1014.2
044700     MOVE 0 TO RESULT-FLAG.                                       DB1014.2
044800 PROC-SERIES-TEST.                                                DB1014.2
044900     IF RESULT-FLAG IS NOT EQUAL TO 2                             DB1014.2
045000         MOVE "USE PROCEDURE NOT EXECUTED" TO RE-MARK             DB1014.2
045100         PERFORM FAIL                                             DB1014.2
045200         GO TO PROC-SERIES-WRITE.                                 DB1014.2
045300     MOVE "DEBUG-LINE; SEE NEXT LINE" TO RE-MARK.                 DB1014.2
045400     MOVE "<===  DEBUG-LINE" TO CORRECT-A.                        DB1014.2
045500     MOVE DBLINE-HOLD TO COMPUTED-A.                              DB1014.2
045600     PERFORM INSPT.                                               DB1014.2
045700     PERFORM PROC-SERIES-WRITE.                                   DB1014.2
045800     MOVE DBNAME-HOLD TO SIZE-16.                                 DB1014.2
045900     IF SIZE-16 IS EQUAL TO "PROC-SERIES-TEST"                    DB1014.2
046000         PERFORM PASS ELSE                                        DB1014.2
046100         MOVE "PROC-SERIES-TEST" TO CORRECT-A                     DB1014.2
046200         MOVE DBNAME-HOLD TO COMPUTED-A                           DB1014.2
046300         PERFORM FAIL.                                            DB1014.2
046400     MOVE "DEBUG-NAME" TO RE-MARK.                                DB1014.2
046500     PERFORM PROC-SERIES-WRITE.                                   DB1014.2
046600     MOVE DBCONT-HOLD TO SIZE-12.                                 DB1014.2
046700     IF SIZE-12 IS EQUAL TO "FALL THROUGH"                        DB1014.2
046800         PERFORM PASS ELSE                                        DB1014.2
046900         MOVE "FALL THROUGH" TO CORRECT-A                         DB1014.2
047000         MOVE DBCONT-HOLD TO COMPUTED-A                           DB1014.2
047100         PERFORM FAIL.                                            DB1014.2
047200     MOVE "DEBUG-CONTENTS" TO RE-MARK.                            DB1014.2
047300     GO TO PROC-SERIES-WRITE.                                     DB1014.2
047400 PROC-SERIES-DELETE.                                              DB1014.2
047500     PERFORM DE-LETE.                                             DB1014.2
047600 PROC-SERIES-WRITE.                                               DB1014.2
047700     MOVE "DEBUG ON PROC-NAME" TO FEATURE.                        DB1014.2
047800     MOVE "PROC-SERIES-TEST" TO PAR-NAME.                         DB1014.2
047900     PERFORM PRINT-DETAIL.                                        DB1014.2
048000     MOVE 0 TO RESULT-FLAG.                                       DB1014.2
048100******************************************************************DB1014.2
048200*    THE DEBUG-LINE (INSPT) SUBTEST FOR THE TEST NAMED IN THE    *DB1014.2
048300*    OUTPUT REPORT AS "GO-TO-TEST" SHOULD POINT TO THE           *DB1014.2
048400*    EXECUTABLE STATEMENT WHICH FOLLOWS THIS COMMENT SET AND     *DB1014.2
048500*    WHICH READS, "GO TO GO-TO-TEST.".                           *DB1014.2
048600******************************************************************DB1014.2
048700 ALTERABLE-PARAGRAPH.                                             DB1014.2
048800     GO TO GO-TO-TEST.                                            DB1014.2
048900 FILLER-PARAGRAPH.                                                DB1014.2
049000     DISPLAY "ALTER FAILED AT ALTER-TEST-INIT".                   DB1014.2
049100     PERFORM FAIL.                                                DB1014.2
049200     GO TO ALTERED-GO-TO-TEST.                                    DB1014.2
049300 GO-TO-TEST.                                                      DB1014.2
049400     IF RESULT-FLAG IS NOT EQUAL TO 3                             DB1014.2
049500         MOVE "USE PROCEDURE NOT EXECUTED" TO RE-MARK             DB1014.2
049600         PERFORM FAIL                                             DB1014.2
049700         GO TO GO-TO-WRITE.                                       DB1014.2
049800     MOVE "DEBUG-LINE; SEE NEXT LINE" TO RE-MARK.                 DB1014.2
049900     MOVE "<===  DEBUG-LINE" TO CORRECT-A.                        DB1014.2
050000     MOVE DBLINE-HOLD TO COMPUTED-A.                              DB1014.2
050100     PERFORM INSPT.                                               DB1014.2
050200     PERFORM GO-TO-WRITE.                                         DB1014.2
050300     MOVE DBNAME-HOLD TO SIZE-10.                                 DB1014.2
050400     IF SIZE-10 IS EQUAL TO "GO-TO-TEST"                          DB1014.2
050500         PERFORM PASS ELSE                                        DB1014.2
050600         MOVE DBNAME-HOLD TO COMPUTED-A                           DB1014.2
050700         MOVE "GO-TO-TEST" TO CORRECT-A                           DB1014.2
050800         PERFORM FAIL.                                            DB1014.2
050900     MOVE "DEBUG-NAME" TO RE-MARK.                                DB1014.2
051000     PERFORM GO-TO-WRITE.                                         DB1014.2
051100     MOVE DBCONT-HOLD TO SIZE-12.                                 DB1014.2
051200     IF       SIZE-12 IS EQUAL TO SPACE                           DB1014.2
051300              PERFORM PASS                                        DB1014.2
051400              ELSE                                                DB1014.2
051500              PERFORM FAIL                                        DB1014.2
051600              MOVE DBCONT-HOLD TO COMPUTED-A                      DB1014.2
051700              MOVE "SPACES" TO CORRECT-A.                         DB1014.2
051800     MOVE     "DEBUG-CONTENTS" TO RE-MARK.                        DB1014.2
051900     GO TO     GO-TO-WRITE.                                       DB1014.2
052000 GO-TO-DELETE.                                                    DB1014.2
052100         PERFORM DE-LETE.                                         DB1014.2
052200 GO-TO-WRITE.                                                     DB1014.2
052300     MOVE "DEBUG ON PROC-NAME" TO FEATURE.                        DB1014.2
052400     MOVE "GO-TO-TEST" TO PAR-NAME.                               DB1014.2
052500     PERFORM PRINT-DETAIL.                                        DB1014.2
052600     MOVE 0 TO RESULT-FLAG.                                       DB1014.2
052700******************************************************************DB1014.2
052800*    THE DEBUG-LINE (INSPT) SUBTEST FOR THE TEST NAMED IN THE    *DB1014.2
052900*    OUTPUT REPORT AS "ALTER-TEST" SHOULD POINT TO THE           *DB1014.2
053000*    EXECUTABLE STATEMENT WHICH FOLLOWS THIS COMMENT SET AND     *DB1014.2
053100*    WHICH READS, "ALTER ALTERABLE-PARAGRAPH TO PROCEED TO       *DB1014.2
053200*    ALTERED-GO-TO-TEST.".                                       *DB1014.2
053300******************************************************************DB1014.2
053400 ALTER-TEST-INIT.                                                 DB1014.2
053500     ALTER ALTERABLE-PARAGRAPH TO PROCEED TO ALTERED-GO-TO-TEST.  DB1014.2
053600 ALTER-TEST.                                                      DB1014.2
053700     IF RESULT-FLAG IS NOT EQUAL TO 4                             DB1014.2
053800         MOVE "USE PROCEDURE NOT EXECUTED" TO RE-MARK             DB1014.2
053900         PERFORM FAIL                                             DB1014.2
054000         GO TO ALTER-WRITE.                                       DB1014.2
054100     MOVE "DEBUG-LINE; SEE NEXT LINE" TO RE-MARK.                 DB1014.2
054200     MOVE "<===  DEBUG-LINE" TO CORRECT-A.                        DB1014.2
054300     MOVE DBLINE-HOLD TO COMPUTED-A                               DB1014.2
054400     PERFORM INSPT.                                               DB1014.2
054500     PERFORM ALTER-WRITE.                                         DB1014.2
054600     MOVE DBNAME-HOLD TO SIZE-19.                                 DB1014.2
054700     IF SIZE-19 IS EQUAL TO "ALTERABLE-PARAGRAPH"                 DB1014.2
054800         PERFORM PASS ELSE                                        DB1014.2
054900         MOVE "ALTERABLE-PARAGRAPH" TO CORRECT-A                  DB1014.2
055000         MOVE DBNAME-HOLD TO COMPUTED-A                           DB1014.2
055100         PERFORM FAIL.                                            DB1014.2
055200     MOVE "DEBUG-NAME" TO RE-MARK.                                DB1014.2
055300     PERFORM ALTER-WRITE.                                         DB1014.2
055400     MOVE DBCONT-HOLD TO SIZE-18.                                 DB1014.2
055500     IF SIZE-18 IS EQUAL TO "ALTERED-GO-TO-TEST"                  DB1014.2
055600         PERFORM PASS ELSE                                        DB1014.2
055700         MOVE "ALTERED-GO-TO-TEST" TO CORRECT-A                   DB1014.2
055800         MOVE DBCONT-HOLD TO COMPUTED-A                           DB1014.2
055900         PERFORM FAIL.                                            DB1014.2
056000     MOVE "DEBUG-CONTENTS" TO RE-MARK.                            DB1014.2
056100     GO TO ALTER-WRITE.                                           DB1014.2
056200 ALTER-DELETE.                                                    DB1014.2
056300     PERFORM DE-LETE.                                             DB1014.2
056400 ALTER-WRITE.                                                     DB1014.2
056500     MOVE "DEBUG ON PROC-NAME" TO FEATURE.                        DB1014.2
056600     MOVE "ALTER-TEST" TO PAR-NAME.                               DB1014.2
056700     PERFORM PRINT-DETAIL.                                        DB1014.2
056800     MOVE 0 TO RESULT-FLAG.                                       DB1014.2
056900******************************************************************DB1014.2
057000*    THE DEBUG-LINE (INSPT) SUBTEST FOR THE TEST NAMED IN THE    *DB1014.2
057100*    OUTPUT REPORT AS "ALTERED-GO-TO-TEST" SHOULD POINT TO THE   *DB1014.2
057200*    EXECUTABLE STATEMENT WHICH FOLLOWS THIS COMMENT SET AND     *DB1014.2
057300*    WHICH READS, "GO TO ALTERABLE-PARAGRAPH.".                  *DB1014.2
057400******************************************************************DB1014.2
057500 ALTER-WRITE-END.                                                 DB1014.2
057600     GO TO ALTERABLE-PARAGRAPH.                                   DB1014.2
057700 ALTERED-GO-TO-TEST.                                              DB1014.2
057800     IF RESULT-FLAG IS NOT EQUAL TO 4                             DB1014.2
057900         MOVE "USE PROCEDURE NOT EXECUTED" TO RE-MARK             DB1014.2
058000         PERFORM FAIL                                             DB1014.2
058100         GO TO ALTERED-GO-TO-WRITE.                               DB1014.2
058200     MOVE "DEBUG-LINE; SEE NEXT LINE" TO RE-MARK.                 DB1014.2
058300     MOVE "<===  DEBUG-LINE" TO CORRECT-A.                        DB1014.2
058400     MOVE DBLINE-HOLD TO COMPUTED-A.                              DB1014.2
058500     PERFORM INSPT.                                               DB1014.2
058600     PERFORM ALTERED-GO-TO-WRITE.                                 DB1014.2
058700     MOVE DBNAME-HOLD TO SIZE-19.                                 DB1014.2
058800     IF SIZE-19 IS EQUAL TO "ALTERABLE-PARAGRAPH"                 DB1014.2
058900         PERFORM PASS ELSE                                        DB1014.2
059000         MOVE DBNAME-HOLD TO COMPUTED-A                           DB1014.2
059100         MOVE "ALTERABLE-PARAGRAPH" TO CORRECT-A                  DB1014.2
059200         PERFORM FAIL.                                            DB1014.2
059300     MOVE "DEBUG-NAME" TO RE-MARK.                                DB1014.2
059400     PERFORM ALTERED-GO-TO-WRITE.                                 DB1014.2
059500     IF       DBCONT-HOLD EQUAL TO SPACE                          DB1014.2
059600              PERFORM PASS                                        DB1014.2
059700              ELSE                                                DB1014.2
059800              PERFORM FAIL                                        DB1014.2
059900              MOVE DBCONT-HOLD TO COMPUTED-A                      DB1014.2
060000              MOVE "SPACES" TO CORRECT-A.                         DB1014.2
060100     MOVE     "DEBUG-CONTENTS" TO RE-MARK.                        DB1014.2
060200     GO TO ALTERED-GO-TO-WRITE.                                   DB1014.2
060300 ALTERED-GO-TO-DELETE.                                            DB1014.2
060400     PERFORM DE-LETE.                                             DB1014.2
060500 ALTERED-GO-TO-WRITE.                                             DB1014.2
060600     MOVE "DEBUG ON PROC-NAME" TO FEATURE.                        DB1014.2
060700     MOVE "ALTERED-GO-TO-TEST" TO PAR-NAME.                       DB1014.2
060800     PERFORM PRINT-DETAIL.                                        DB1014.2
060900     MOVE 0 TO RESULT-FLAG.                                       DB1014.2
061000******************************************************************DB1014.2
061100*    THE DEBUG-LINE (INSPT) SUBTEST FOR THE TEST NAMED IN THE    *DB1014.2
061200*    OUTPUT REPORT AS "PERF-ITERATION-TEST" SHOULD POINT TO THE  *DB1014.2
061300*    EXECUTABLE STATEMENT WHICH FOLLOWS THIS COMMENT SET AND     *DB1014.2
061400*    WHICH READS, "PERFORM LOOP-ROUTINE FIVE TIMES.".            *DB1014.2
061500******************************************************************DB1014.2
061600 PERF-ITERATION-TEST.                                             DB1014.2
061700     PERFORM LOOP-ROUTINE FIVE TIMES.                             DB1014.2
061800     IF RESULT-FLAG IS NOT EQUAL TO 5                             DB1014.2
061900         MOVE "05" TO CORRECT-A                                   DB1014.2
062000         MOVE RESULT-FLAG TO COMPUTED-A                           DB1014.2
062100         MOVE "NO. OF TIMES USE PROC EXECUTED" TO RE-MARK         DB1014.2
062200         PERFORM FAIL                                             DB1014.2
062300              ELSE                                                DB1014.2
062400              MOVE "PROC EXECUTED FIVE TIMES" TO RE-MARK          DB1014.2
062500              PERFORM PASS.                                       DB1014.2
062600     IF RESULT-FLAG IS EQUAL TO 0                                 DB1014.2
062700         GO TO PERF-ITERATION-WRITE                               DB1014.2
062800         ELSE PERFORM PERF-ITERATION-WRITE.                       DB1014.2
062900     MOVE DBLINE-HOLD TO COMPUTED-A.                              DB1014.2
063000     PERFORM INSPT.                                               DB1014.2
063100     MOVE "DEBUG-LINE; SEE NEXT LINE" TO RE-MARK.                 DB1014.2
063200     MOVE "<===  DEBUG-LINE" TO CORRECT-A.                        DB1014.2
063300     PERFORM PERF-ITERATION-WRITE.                                DB1014.2
063400     MOVE DBNAME-HOLD TO SIZE-12.                                 DB1014.2
063500     IF SIZE-12 IS EQUAL TO "LOOP-ROUTINE"                        DB1014.2
063600         PERFORM PASS ELSE                                        DB1014.2
063700         MOVE "LOOP-ROUTINE" TO CORRECT-A                         DB1014.2
063800         MOVE DBNAME-HOLD TO COMPUTED-A                           DB1014.2
063900         PERFORM FAIL.                                            DB1014.2
064000     MOVE "DEBUG-NAME" TO RE-MARK.                                DB1014.2
064100     PERFORM PERF-ITERATION-WRITE.                                DB1014.2
064200     MOVE DBCONT-HOLD TO SIZE-12.                                 DB1014.2
064300     IF SIZE-12 IS EQUAL TO "PERFORM LOOP"                        DB1014.2
064400         PERFORM PASS ELSE                                        DB1014.2
064500         MOVE "PERFORM LOOP" TO CORRECT-A                         DB1014.2
064600         MOVE DBCONT-HOLD TO COMPUTED-A                           DB1014.2
064700         PERFORM FAIL.                                            DB1014.2
064800     MOVE "DEBUG-CONTENTS" TO RE-MARK.                            DB1014.2
064900     GO TO PERF-ITERATION-WRITE.                                  DB1014.2
065000 PERF-ITERATION-DELETE.                                           DB1014.2
065100     PERFORM DE-LETE.                                             DB1014.2
065200 PERF-ITERATION-WRITE.                                            DB1014.2
065300     MOVE "PERF-ITERATION-TEST" TO PAR-NAME.                      DB1014.2
065400     MOVE "DEBUG ON PROC-NAME" TO FEATURE.                        DB1014.2
065500     PERFORM PRINT-DETAIL.                                        DB1014.2
065600     MOVE 0 TO RESULT-FLAG.                                       DB1014.2
065700 PERF-ITERATION-END.                                              DB1014.2
065800     GO TO PERFORM-THRU-TEST.                                     DB1014.2
065900 LOOP-ROUTINE.                                                    DB1014.2
066000**NESTED PERFORMS ARE USED HERE TO ATTEMPT TO PREVENT OPTIMIZER   DB1014.2
066100* ACTION RESULTING IN LOOP UNFOLDING AND REDUCTION.               DB1014.2
066200     PERFORM DO-NOTHING.                                          DB1014.2
066300******************************************************************DB1014.2
066400*    THE DEBUG-LINE (INSPT) SUBTEST FOR THE TEST NAMED IN THE    *DB1014.2
066500*    OUTPUT REPORT AS "PERFORM-THRU-TEST" SHOULD POINT TO THE    *DB1014.2
066600*    EXECUTABLE STATEMENT WHICH FOLLOWS THIS COMMENT SET AND     *DB1014.2
066700*    WHICH READS, "ADD A B C GIVING D.".                         *DB1014.2
066800******************************************************************DB1014.2
066900 DO-NOTHING.                                                      DB1014.2
067000     ADD A B C GIVING D.                                          DB1014.2
067100 DO-NOTHING-1.                                                    DB1014.2
067200     SUBTRACT A FROM B.                                           DB1014.2
067300 PERFORM-THRU-TEST.                                               DB1014.2
067400     PERFORM DO-NOTHING THRU DO-NOTHING-1 FIVE TIMES.             DB1014.2
067500     IF RESULT-FLAG IS NOT EQUAL TO 5                             DB1014.2
067600         MOVE "05" TO CORRECT-A                                   DB1014.2
067700         MOVE RESULT-FLAG TO COMPUTED-A                           DB1014.2
067800         MOVE "NO. OF TIMES USE PROC EXECUTED" TO RE-MARK         DB1014.2
067900         PERFORM FAIL                                             DB1014.2
068000              ELSE                                                DB1014.2
068100              MOVE "PROC EXECUTED FIVE TIMES" TO RE-MARK          DB1014.2
068200              PERFORM PASS.                                       DB1014.2
068300     IF RESULT-FLAG IS EQUAL TO 0                                 DB1014.2
068400         GO TO PERFORM-THRU-WRITE   ELSE                          DB1014.2
068500         PERFORM PERFORM-THRU-WRITE.                              DB1014.2
068600     MOVE "DEBUG-LINE; SEE NEXT LINE" TO RE-MARK.                 DB1014.2
068700     MOVE "<===  DEBUG-LINE" TO CORRECT-A.                        DB1014.2
068800     MOVE DBLINE-HOLD TO COMPUTED-A.                              DB1014.2
068900     PERFORM INSPT.                                               DB1014.2
069000     PERFORM PERFORM-THRU-WRITE.                                  DB1014.2
069100     MOVE DBNAME-HOLD TO SIZE-12.                                 DB1014.2
069200     IF SIZE-12 IS EQUAL TO "DO-NOTHING-1"                        DB1014.2
069300         PERFORM PASS   ELSE                                      DB1014.2
069400         MOVE "DO-NOTHING-1" TO CORRECT-A                         DB1014.2
069500         MOVE DBNAME-HOLD TO COMPUTED-A                           DB1014.2
069600         PERFORM FAIL.                                            DB1014.2
069700     MOVE "DEBUG-NAME" TO RE-MARK.                                DB1014.2
069800     PERFORM PERFORM-THRU-WRITE.                                  DB1014.2
069900     MOVE DBCONT-HOLD TO SIZE-12                                  DB1014.2
070000     IF SIZE-12 IS EQUAL TO "FALL THROUGH"                        DB1014.2
070100         PERFORM PASS   ELSE                                      DB1014.2
070200         MOVE "FALL THROUGH" TO CORRECT-A                         DB1014.2
070300         MOVE DBCONT-HOLD TO COMPUTED-A                           DB1014.2
070400         PERFORM FAIL.                                            DB1014.2
070500     MOVE "DEBUG-CONTENTS" TO RE-MARK.                            DB1014.2
070600     GO TO PERFORM-THRU-WRITE.                                    DB1014.2
070700 PERFORM-THRU-DELETE.                                             DB1014.2
070800     PERFORM DE-LETE.                                             DB1014.2
070900 PERFORM-THRU-WRITE.                                              DB1014.2
071000     MOVE "PERFORM-THRU-TEST" TO PAR-NAME.                        DB1014.2
071100     MOVE "DEBUG ON PROC-NAME" TO FEATURE.                        DB1014.2
071200     PERFORM PRINT-DETAIL.                                        DB1014.2
071300     MOVE 0 TO RESULT-FLAG.                                       DB1014.2
071400******************************************************************DB1014.2
071500*    THE DEBUG-LINE (INSPT) SUBTEST FOR THE TEST NAMED IN THE    *DB1014.2
071600*    OUTPUT REPORT AS "SIMPLE-PERFORM-TEST" SHOULD POINT TO THE  *DB1014.2
071700*    EXECUTABLE STATEMENT WHICH FOLLOWS THIS COMMENT SET AND     *DB1014.2
071800*    WHICH READS, "PERFORM LOOP-ROUTINE.".                       *DB1014.2
071900******************************************************************DB1014.2
072000 SIMPLE-PERFORM-TEST.                                             DB1014.2
072100     PERFORM LOOP-ROUTINE.                                        DB1014.2
072200     IF RESULT-FLAG IS NOT EQUAL TO 1                             DB1014.2
072300         MOVE "USE PROCEDURE NOT EXECUTED" TO RE-MARK             DB1014.2
072400         PERFORM FAIL                                             DB1014.2
072500         GO TO SIMPLE-PERFORM-WRITE.                              DB1014.2
072600     MOVE "DEBUG-LINE; SEE NEXT LINE" TO RE-MARK.                 DB1014.2
072700     MOVE "<===  DEBUG-LINE" TO CORRECT-A.                        DB1014.2
072800     MOVE DBLINE-HOLD TO COMPUTED-A.                              DB1014.2
072900     PERFORM INSPT.                                               DB1014.2
073000     PERFORM SIMPLE-PERFORM-WRITE.                                DB1014.2
073100     MOVE DBNAME-HOLD TO SIZE-12.                                 DB1014.2
073200     IF SIZE-12 IS EQUAL TO "LOOP-ROUTINE"                        DB1014.2
073300         PERFORM PASS   ELSE                                      DB1014.2
073400         MOVE "LOOP-ROUTINE" TO CORRECT-A                         DB1014.2
073500         MOVE DBNAME-HOLD TO COMPUTED-A                           DB1014.2
073600         PERFORM FAIL.                                            DB1014.2
073700     MOVE "DEBUG-NAME" TO RE-MARK.                                DB1014.2
073800     PERFORM SIMPLE-PERFORM-WRITE.                                DB1014.2
073900     MOVE DBCONT-HOLD TO SIZE-12.                                 DB1014.2
074000     IF SIZE-12 IS EQUAL TO "PERFORM LOOP"                        DB1014.2
074100         PERFORM PASS   ELSE                                      DB1014.2
074200         MOVE "PERFORM LOOP" TO CORRECT-A                         DB1014.2
074300         MOVE DBCONT-HOLD TO COMPUTED-A                           DB1014.2
074400         PERFORM FAIL.                                            DB1014.2
074500     MOVE "DEBUG-CONTENTS" TO RE-MARK.                            DB1014.2
074600     GO TO SIMPLE-PERFORM-WRITE.                                  DB1014.2
074700 SIMPLE-PERFORM-DELETE.                                           DB1014.2
074800     PERFORM DE-LETE.                                             DB1014.2
074900 SIMPLE-PERFORM-WRITE.                                            DB1014.2
075000     MOVE "SIMPLE-PERFORM-TEST" TO PAR-NAME.                      DB1014.2
075100     MOVE "DEBUG ON PROC-NAME" TO FEATURE.                        DB1014.2
075200     PERFORM PRINT-DETAIL.                                        DB1014.2
075300     MOVE 0 TO RESULT-FLAG.                                       DB1014.2
075400 DEBUG-LINE-TESTS-INIT.                                           DB1014.2
075500     MOVE "DEBUG LINE TESTS" TO FEATURE.                          DB1014.2
075600 DEBUG-LINE-TEST-01.                                              DB1014.2
075700     MOVE "COMPLETE ENTITY" TO RE-MARK.                           DB1014.2
075800     PERFORM FAIL.                                                DB1014.2
075900D    PERFORM PASS  SUBTRACT 1 FROM ERROR-COUNTER.                 DB1014.2
076000     GO TO DEBUG-LINE-WRITE-01.                                   DB1014.2
076100 DEBUG-LINE-DELETE-01.                                            DB1014.2
076200     PERFORM DE-LETE.                                             DB1014.2
076300 DEBUG-LINE-WRITE-01.                                             DB1014.2
076400     MOVE "DEBUG-LINE-TEST-01" TO PAR-NAME.                       DB1014.2
076500     PERFORM PRINT-DETAIL.                                        DB1014.2
076600 DEBUG-LINE-TEST-02.                                              DB1014.2
076700     MOVE "CONSECUTIVE DEBUG LINES" TO RE-MARK.                   DB1014.2
076800     PERFORM FAIL.                                                DB1014.2
076900D    PERFORM PASS.                                                DB1014.2
077000D    SUBTRACT 1 FROM ERROR-COUNTER.                               DB1014.2
077100     GO TO DEBUG-LINE-WRITE-02.                                   DB1014.2
077200 DEBUG-LINE-DELETE-02.                                            DB1014.2
077300     PERFORM DE-LETE.                                             DB1014.2
077400 DEBUG-LINE-WRITE-02.                                             DB1014.2
077500     MOVE "DEBUG-LINE-TEST-02" TO PAR-NAME.                       DB1014.2
077600     PERFORM PRINT-DETAIL.                                        DB1014.2
077700 DEBUG-LINE-TEST-03.                                              DB1014.2
077800     MOVE "BROKEN STATEMENTS" TO RE-MARK.                         DB1014.2
077900     PERFORM                                                      DB1014.2
078000D        PASS.  GO TO DEBUG-LINE-WRITE-03.                        DB1014.2
078100DDEBUG-LINE-TEST-03-A.    PERFORM                                 DB1014.2
078200                             FAIL.                                DB1014.2
078300                             GO TO DEBUG-LINE-WRITE-03.           DB1014.2
078400 DEBUG-LINE-DELETE-03.                                            DB1014.2
078500     PERFORM DE-LETE.                                             DB1014.2
078600 DEBUG-LINE-WRITE-03.                                             DB1014.2
078700     MOVE "DEBUG-LINE-TEST-03" TO PAR-NAME.                       DB1014.2
078800     PERFORM PRINT-DETAIL.                                        DB1014.2
078900 DEBUG-LINE-TEST-04.                                              DB1014.2
079000     MOVE "NESTED COMMENTS" TO RE-MARK.                           DB1014.2
079100D    PERFORM                                                      DB1014.2
079200*        FAIL.  GO TO DEBUG-LINE-WRITE-04.                        DB1014.2
079300*DEBUG-LINE-TEST-04-A.    PERFORM                                 DB1014.2
079400D                         PASS.  GO TO DEBUG-LINE-WRITE-04.       DB1014.2
079500 DEBUG-LINE-TEST-04-B.                                            DB1014.2
079600     MOVE "    FAILURE 04B" TO COMPUTED-A.                        DB1014.2
079700     PERFORM FAIL.                                                DB1014.2
079800     GO TO DEBUG-LINE-WRITE-04.                                   DB1014.2
079900 DEBUG-LINE-DELETE-04.                                            DB1014.2
080000     PERFORM DE-LETE.                                             DB1014.2
080100 DEBUG-LINE-WRITE-04.                                             DB1014.2
080200     MOVE "DEBUG-LINE-TEST-04" TO PAR-NAME.                       DB1014.2
080300     PERFORM PRINT-DETAIL.                                        DB1014.2
080400 DEBUG-LINE-TEST-05.                                              DB1014.2
080500     MOVE "NESTED INSIDE COMMENTS" TO RE-MARK.                    DB1014.2
080600*    PERFORM FAIL.                                                DB1014.2
080700*    GO TO DEBUG-LINE-WRITE-05.                                   DB1014.2
080800*DEBUG-LINE-TEST-05-A.                                            DB1014.2
080900D    PERFORM PASS.                                                DB1014.2
081000D    GO TO DEBUG-LINE-WRITE-05.                                   DB1014.2
081100*DEBUG-LINE-TEST-05-B.                                            DB1014.2
081200*    MOVE "    FAILURE 05B" TO COMPUTED-A.                        DB1014.2
081300*    PERFORM FAIL.  GO TO DEBUG-LINE-WRITE-05.                    DB1014.2
081400 DEBUG-LINE-TEST-05-C.                                            DB1014.2
081500     MOVE "    FAILURE 05C" TO COMPUTED-A.                        DB1014.2
081600     PERFORM FAIL.   GO TO DEBUG-LINE-WRITE-05.                   DB1014.2
081700 DEBUG-LINE-DELETE-05.                                            DB1014.2
081800     PERFORM DE-LETE.                                             DB1014.2
081900 DEBUG-LINE-WRITE-05.                                             DB1014.2
082000     MOVE "DEBUG-LINE-TEST-05" TO PAR-NAME.                       DB1014.2
082100     PERFORM PRINT-DETAIL.                                        DB1014.2
082200 CCVS-EXIT SECTION.                                               DB1014.2
082300 CCVS-999999.                                                     DB1014.2
082400     GO TO CLOSE-FILES.                                           DB1014.2
      *END-OF,DB101A                                                                  
