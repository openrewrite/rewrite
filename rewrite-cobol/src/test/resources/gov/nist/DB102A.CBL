      *HEADER,COBOL,DB102A                                                            
000100 IDENTIFICATION DIVISION.                                         DB1024.2
000200 PROGRAM-ID.                                                      DB1024.2
000300     DB102A.                                                      DB1024.2
000400 AUTHOR.                                                          DB1024.2
000500     FEDERAL COMPILER TESTING CENTER.                             DB1024.2
000600 INSTALLATION.                                                    DB1024.2
000700     GENERAL SERVICES ADMINISTRATION                              DB1024.2
000800*     AUTOMATED DATA AND TELECOMMUNICATION SERVICE.                DB1024.2
000900     SOFTWARE DEVELOPMENT OFFICE.                                 DB1024.2
001000     5203 LEESBURG PIKE  SUITE 1100                               DB1024.2
001100     FALLS CHURCH VIRGINIA 22041.                                 DB1024.2
001200                                                                  DB1024.2
001300     PHONE   (703) 756-6153                                       DB1024.2
001400                                                                  DB1024.2
001500     " HIGH       ".                                              DB1024.2
001600 DATE-WRITTEN.                                                    DB1024.2
001700     CCVS-74 VERSION 4.0 - 1980 JULY 1.                           DB1024.2
001800     CREATION DATE     /    VALIDATION DATE                       DB1024.2
001900     "4.2 ".                                                      DB1024.2
002000 SECURITY.                                                        DB1024.2
002100     NONE.                                                        DB1024.2
002200*                                                                 DB1024.2
002300*    *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *DB1024.2
002400*                                                                 DB1024.2
002500*                       PROGRAM ABSTRACT                          DB1024.2
002600*                                                                 DB1024.2
002700*    DB102A TESTS THE BASIC OPERATION OF THE DEBUG MODULE         DB1024.2
002800*    FACILITIES WHEN THE COMPILE TIME DEBUGGING SWITCH IS ON      DB1024.2
002900*    AND THE OBJECT TIME SWITCH IS OFF.  ALL DEBUG LINES AND      DB1024.2
003000*    DEBUGGING PROCEDURES SHOULD BE INCLUDED IN COMPILATION AND   DB1024.2
003100*    GENERATE CODE.                                               DB1024.2
003200*                                                                 DB1024.2
003300*    BEFORE BEGINNING EXECUTION OF THE OBJECT PROGRAM, THE JOB    DB1024.2
003400*    CONTROL LANGUAGE NECESSARY TO DEACTIVATE (TURN OFF) THE      DB1024.2
003500*    OBJECT TIME DEBUGGING SWITCH MUST BE SUBMITTED.              DB1024.2
003600*                                                                 DB1024.2
003700*    AT EXECUTION TIME, CODE GENERATED FROM DEBUG LINES SHOULD    DB1024.2
003800*    BE EXECUTED, BUT DEBUGGING PROCEDURES SHOULD BE DEACTIVATED  DB1024.2
003900*    BY THE OBJECT TIME SWITCH.                                   DB1024.2
004000*                                                                 DB1024.2
004100*                                                                 DB1024.2
004200*                                                                 DB1024.2
004300 ENVIRONMENT DIVISION.                                            DB1024.2
004400 CONFIGURATION SECTION.                                           DB1024.2
004500 SOURCE-COMPUTER.                                                 DB1024.2
004600     XXXXX082                                                     DB1024.2
004700         WITH DEBUGGING MODE.                                     DB1024.2
004800 OBJECT-COMPUTER.                                                 DB1024.2
004900     XXXXX083.                                                    DB1024.2
005000 INPUT-OUTPUT SECTION.                                            DB1024.2
005100 FILE-CONTROL.                                                    DB1024.2
005200     SELECT PRINT-FILE ASSIGN TO                                  DB1024.2
005300     XXXXX055.                                                    DB1024.2
005400 DATA DIVISION.                                                   DB1024.2
005500 FILE SECTION.                                                    DB1024.2
005600 FD  PRINT-FILE                                                   DB1024.2
005700     LABEL RECORDS                                                DB1024.2
005800     XXXXX084                                                     DB1024.2
005900     DATA RECORD IS PRINT-REC DUMMY-RECORD.                       DB1024.2
006000 01  PRINT-REC PICTURE X(120).                                    DB1024.2
006100 01  DUMMY-RECORD PICTURE X(120).                                 DB1024.2
006200 WORKING-STORAGE SECTION.                                         DB1024.2
006300 77  A PIC 9 COMP VALUE 1.                                        DB1024.2
006400 77  B PIC 9 COMP VALUE 5.                                        DB1024.2
006500 77  C PIC 9 COMP VALUE 9.                                        DB1024.2
006600 77  D PIC 99 COMP.                                               DB1024.2
006700 77  RESULT-FLAG PIC 99 COMP VALUE 0.                             DB1024.2
006800 77  DBLINE-HOLD PIC X(6).                                        DB1024.2
006900 77  DBNAME-HOLD PIC X(30).                                       DB1024.2
007000 77  DBCONT-HOLD PIC X(30).                                       DB1024.2
007100 77  FIVE PIC 9 COMP VALUE 5.                                     DB1024.2
007200 01  SIZE-19.                                                     DB1024.2
007300     02  FILLER PIC X.                                            DB1024.2
007400     02  SIZE-18.                                                 DB1024.2
007500         03  FILLER PIC X.                                        DB1024.2
007600         03  SIZE-17.                                             DB1024.2
007700             04  FILLER PIC X.                                    DB1024.2
007800             04  SIZE-16.                                         DB1024.2
007900                 05  FILLER PIC X.                                DB1024.2
008000                 05  SIZE-15.                                     DB1024.2
008100                     06  FILLER PIC X.                            DB1024.2
008200                     06  SIZE-14.                                 DB1024.2
008300                         07  FILLER PIC X.                        DB1024.2
008400                         07  SIZE-13.                             DB1024.2
008500                             08  FILLER PIC X.                    DB1024.2
008600                             08  SIZE-12.                         DB1024.2
008700                                 09  FILLER PIC X.                DB1024.2
008800                                 09  SIZE-11.                     DB1024.2
008900                                     10  FILLER PIC X.            DB1024.2
009000                                     10  SIZE-10 PIC X(10).       DB1024.2
009100 01  TEST-RESULTS.                                                DB1024.2
009200     02 FILLER                    PICTURE X VALUE SPACE.          DB1024.2
009300     02 FEATURE                   PICTURE X(20) VALUE SPACE.      DB1024.2
009400     02 FILLER                    PICTURE X VALUE SPACE.          DB1024.2
009500     02 P-OR-F                    PICTURE X(5) VALUE SPACE.       DB1024.2
009600     02 FILLER                    PICTURE X  VALUE SPACE.         DB1024.2
009700     02  PAR-NAME.                                                DB1024.2
009800       03 FILLER PICTURE X(12) VALUE SPACE.                       DB1024.2
009900       03  PARDOT-X PICTURE X  VALUE SPACE.                       DB1024.2
010000       03 DOTVALUE PICTURE 99  VALUE ZERO.                        DB1024.2
010100       03 FILLER PIC X(5) VALUE SPACE.                            DB1024.2
010200     02 FILLER PIC X(10) VALUE SPACE.                             DB1024.2
010300     02 RE-MARK PIC X(61).                                        DB1024.2
010400 01  TEST-COMPUTED.                                               DB1024.2
010500     02 FILLER PIC X(30) VALUE SPACE.                             DB1024.2
010600     02 FILLER PIC X(17) VALUE "       COMPUTED=".                DB1024.2
010700     02 COMPUTED-X.                                               DB1024.2
010800     03 COMPUTED-A                PICTURE X(20) VALUE SPACE.      DB1024.2
010900     03 COMPUTED-N REDEFINES COMPUTED-A PICTURE -9(9).9(9).       DB1024.2
011000     03 COMPUTED-0V18 REDEFINES COMPUTED-A  PICTURE -.9(18).      DB1024.2
011100     03 COMPUTED-4V14 REDEFINES COMPUTED-A  PICTURE -9(4).9(14).  DB1024.2
011200     03 COMPUTED-14V4 REDEFINES COMPUTED-A  PICTURE -9(14).9(4).  DB1024.2
011300     03       CM-18V0 REDEFINES COMPUTED-A.                       DB1024.2
011400         04 COMPUTED-18V0                   PICTURE -9(18).       DB1024.2
011500         04 FILLER                          PICTURE X.            DB1024.2
011600     03 FILLER PIC X(50) VALUE SPACE.                             DB1024.2
011700 01  TEST-CORRECT.                                                DB1024.2
011800     02 FILLER PIC X(30) VALUE SPACE.                             DB1024.2
011900     02 FILLER PIC X(17) VALUE "       CORRECT =".                DB1024.2
012000     02 CORRECT-X.                                                DB1024.2
012100     03 CORRECT-A                 PICTURE X(20) VALUE SPACE.      DB1024.2
012200     03 CORRECT-N REDEFINES CORRECT-A PICTURE -9(9).9(9).         DB1024.2
012300     03 CORRECT-0V18 REDEFINES CORRECT-A    PICTURE -.9(18).      DB1024.2
012400     03 CORRECT-4V14 REDEFINES CORRECT-A    PICTURE -9(4).9(14).  DB1024.2
012500     03 CORRECT-14V4 REDEFINES CORRECT-A    PICTURE -9(14).9(4).  DB1024.2
012600     03      CR-18V0 REDEFINES CORRECT-A.                         DB1024.2
012700         04 CORRECT-18V0                    PICTURE -9(18).       DB1024.2
012800         04 FILLER                          PICTURE X.            DB1024.2
012900     03 FILLER PIC X(50) VALUE SPACE.                             DB1024.2
013000 01  CCVS-C-1.                                                    DB1024.2
013100     02 FILLER PICTURE IS X(99) VALUE IS " FEATURE              PADB1024.2
013200-    "SS  PARAGRAPH-NAME                                          DB1024.2
013300-    "        REMARKS".                                           DB1024.2
013400     02 FILLER PICTURE IS X(20) VALUE IS SPACE.                   DB1024.2
013500 01  CCVS-C-2.                                                    DB1024.2
013600     02 FILLER PICTURE IS X VALUE IS SPACE.                       DB1024.2
013700     02 FILLER PICTURE IS X(6) VALUE IS "TESTED".                 DB1024.2
013800     02 FILLER PICTURE IS X(15) VALUE IS SPACE.                   DB1024.2
013900     02 FILLER PICTURE IS X(4) VALUE IS "FAIL".                   DB1024.2
014000     02 FILLER PICTURE IS X(94) VALUE IS SPACE.                   DB1024.2
014100 01  REC-SKL-SUB PICTURE 9(2) VALUE ZERO.                         DB1024.2
014200 01  REC-CT PICTURE 99 VALUE ZERO.                                DB1024.2
014300 01  DELETE-CNT                   PICTURE 999  VALUE ZERO.        DB1024.2
014400 01  ERROR-COUNTER PICTURE IS 999 VALUE IS ZERO.                  DB1024.2
014500 01  INSPECT-COUNTER PIC 999 VALUE ZERO.                          DB1024.2
014600 01  PASS-COUNTER PIC 999 VALUE ZERO.                             DB1024.2
014700 01  TOTAL-ERROR PIC 999 VALUE ZERO.                              DB1024.2
014800 01  ERROR-HOLD PIC 999 VALUE ZERO.                               DB1024.2
014900 01  DUMMY-HOLD PIC X(120) VALUE SPACE.                           DB1024.2
015000 01  RECORD-COUNT PIC 9(5) VALUE ZERO.                            DB1024.2
015100 01  CCVS-H-1.                                                    DB1024.2
015200     02  FILLER   PICTURE X(27)  VALUE SPACE.                     DB1024.2
015300     02 FILLER PICTURE X(67) VALUE                                DB1024.2
015400     " FEDERAL COMPILER TESTING CENTER COBOL COMPILER VALIDATION  DB1024.2
015500-    " SYSTEM".                                                   DB1024.2
015600     02  FILLER     PICTURE X(26)  VALUE SPACE.                   DB1024.2
015700 01  CCVS-H-2.                                                    DB1024.2
015800     02 FILLER PICTURE X(52) VALUE IS                             DB1024.2
015900     "CCVS74 NCC  COPY, NOT FOR DISTRIBUTION.".                   DB1024.2
016000     02 FILLER PICTURE IS X(19) VALUE IS "TEST RESULTS SET-  ".   DB1024.2
016100     02 TEST-ID PICTURE IS X(9).                                  DB1024.2
016200     02 FILLER PICTURE IS X(40) VALUE IS SPACE.                   DB1024.2
016300 01  CCVS-H-3.                                                    DB1024.2
016400     02  FILLER PICTURE X(34) VALUE                               DB1024.2
016500     " FOR OFFICIAL USE ONLY    ".                                DB1024.2
016600     02  FILLER PICTURE X(58) VALUE                               DB1024.2
016700     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".DB1024.2
016800     02  FILLER PICTURE X(28) VALUE                               DB1024.2
016900     "  COPYRIGHT   1974 ".                                       DB1024.2
017000 01  CCVS-E-1.                                                    DB1024.2
017100     02 FILLER PICTURE IS X(52) VALUE IS SPACE.                   DB1024.2
017200     02 FILLER PICTURE IS X(14) VALUE IS "END OF TEST-  ".        DB1024.2
017300     02 ID-AGAIN PICTURE IS X(9).                                 DB1024.2
017400     02 FILLER PICTURE X(45) VALUE IS                             DB1024.2
017500     " NTIS DISTRIBUTION COBOL 74".                               DB1024.2
017600 01  CCVS-E-2.                                                    DB1024.2
017700     02  FILLER                   PICTURE X(31)  VALUE            DB1024.2
017800     SPACE.                                                       DB1024.2
017900     02  FILLER                   PICTURE X(21)  VALUE SPACE.     DB1024.2
018000     02 CCVS-E-2-2.                                               DB1024.2
018100         03 ERROR-TOTAL PICTURE IS XXX VALUE IS SPACE.            DB1024.2
018200         03 FILLER PICTURE IS X VALUE IS SPACE.                   DB1024.2
018300         03 ENDER-DESC PIC X(44) VALUE "ERRORS ENCOUNTERED".      DB1024.2
018400 01  CCVS-E-3.                                                    DB1024.2
018500     02  FILLER PICTURE X(22) VALUE                               DB1024.2
018600     " FOR OFFICIAL USE ONLY".                                    DB1024.2
018700     02  FILLER PICTURE X(12) VALUE SPACE.                        DB1024.2
018800     02  FILLER PICTURE X(58) VALUE                               DB1024.2
018900     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".DB1024.2
019000     02  FILLER PICTURE X(13) VALUE SPACE.                        DB1024.2
019100     02 FILLER PIC X(15) VALUE " COPYRIGHT 1974".                 DB1024.2
019200 01  CCVS-E-4.                                                    DB1024.2
019300     02 CCVS-E-4-1 PIC XXX VALUE SPACE.                           DB1024.2
019400     02 FILLER PIC XXXX VALUE " OF ".                             DB1024.2
019500     02 CCVS-E-4-2 PIC XXX VALUE SPACE.                           DB1024.2
019600     02 FILLER PIC X(40) VALUE                                    DB1024.2
019700      "  TESTS WERE EXECUTED SUCCESSFULLY".                       DB1024.2
019800 01  XXINFO.                                                      DB1024.2
019900     02 FILLER PIC X(30) VALUE "        *** INFORMATION  ***".    DB1024.2
020000     02 INFO-TEXT.                                                DB1024.2
020100     04 FILLER PIC X(20) VALUE SPACE.                             DB1024.2
020200     04 XXCOMPUTED PIC X(20).                                     DB1024.2
020300     04 FILLER PIC X(5) VALUE SPACE.                              DB1024.2
020400     04 XXCORRECT PIC X(20).                                      DB1024.2
020500 01  HYPHEN-LINE.                                                 DB1024.2
020600     02 FILLER PICTURE IS X VALUE IS SPACE.                       DB1024.2
020700     02 FILLER PICTURE IS X(65) VALUE IS "************************DB1024.2
020800-    "*****************************************".                 DB1024.2
020900     02 FILLER PICTURE IS X(54) VALUE IS "************************DB1024.2
021000-    "******************************".                            DB1024.2
021100 01  CCVS-PGM-ID PIC X(6) VALUE                                   DB1024.2
021200     "DB102A".                                                    DB1024.2
021300 PROCEDURE DIVISION.                                              DB1024.2
021400 DECLARATIVES.                                                    DB1024.2
021500 START-UP SECTION.                                                DB1024.2
021600     USE FOR DEBUGGING ON OPEN-FILES.                             DB1024.2
021700 BEGIN-START-UP.                                                  DB1024.2
021800     MOVE 1 TO RESULT-FLAG.                                       DB1024.2
021900 DB-COMMON.                                                       DB1024.2
022000     MOVE DEBUG-LINE TO DBLINE-HOLD.                              DB1024.2
022100     MOVE DEBUG-NAME TO DBNAME-HOLD.                              DB1024.2
022200     MOVE DEBUG-CONTENTS TO DBCONT-HOLD.                          DB1024.2
022300 FALL-THROUGH-AND-SERIES SECTION.                                 DB1024.2
022400     USE FOR DEBUGGING ON FALL-THROUGH-TEST                       DB1024.2
022500              PROC-SERIES-TEST.                                   DB1024.2
022600 BEGIN-FALL-THROUGH-AND-SERIES.                                   DB1024.2
022700     PERFORM DB-COMMON.                                           DB1024.2
022800     MOVE 2 TO RESULT-FLAG.                                       DB1024.2
022900 GO-TO SECTION.                                                   DB1024.2
023000     USE FOR DEBUGGING ON GO-TO-TEST.                             DB1024.2
023100 BEGIN-GO-TO.                                                     DB1024.2
023200     PERFORM DB-COMMON.                                           DB1024.2
023300     MOVE 3 TO RESULT-FLAG.                                       DB1024.2
023400 ALTER-PARAGRAPH SECTION.                                         DB1024.2
023500     USE FOR DEBUGGING ON ALTERABLE-PARAGRAPH.                    DB1024.2
023600 BEGIN-ALTER-PARAGRAPH.                                           DB1024.2
023700     PERFORM DB-COMMON.                                           DB1024.2
023800     MOVE 4 TO RESULT-FLAG.                                       DB1024.2
023900 LOOP-ITERATION SECTION.                                          DB1024.2
024000     USE FOR DEBUGGING ON LOOP-ROUTINE.                           DB1024.2
024100 BEGIN-LOOP-ITERATION.                                            DB1024.2
024200     PERFORM DB-COMMON.                                           DB1024.2
024300     ADD 1 TO RESULT-FLAG.                                        DB1024.2
024400 PERFORM-THRU SECTION.                                            DB1024.2
024500     USE FOR DEBUGGING ON DO-NOTHING-1.                           DB1024.2
024600 BEGIN-PERFORM-THRU.                                              DB1024.2
024700     PERFORM DB-COMMON.                                           DB1024.2
024800     ADD 1 TO RESULT-FLAG.                                        DB1024.2
024900 END DECLARATIVES.                                                DB1024.2
025000 CCVS1 SECTION.                                                   DB1024.2
025100 OPEN-FILES.                                                      DB1024.2
025200     OPEN     OUTPUT PRINT-FILE.                                  DB1024.2
025300     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   DB1024.2
025400     MOVE    SPACE TO TEST-RESULTS.                               DB1024.2
025500     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             DB1024.2
025600     GO TO CCVS1-EXIT.                                            DB1024.2
025700 CLOSE-FILES.                                                     DB1024.2
025800     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   DB1024.2
025900 TERMINATE-CCVS.                                                  DB1024.2
026000*    EXIT PROGRAM.                                                DB1024.2
026100*TERMINATE-CALL.                                                  DB1024.2
026200     STOP     RUN.                                                DB1024.2
026300 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         DB1024.2
026400 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           DB1024.2
026500 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          DB1024.2
026600 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-CNT.          DB1024.2
026700     MOVE "****TEST DELETED****" TO RE-MARK.                      DB1024.2
026800 PRINT-DETAIL.                                                    DB1024.2
026900     IF REC-CT NOT EQUAL TO ZERO                                  DB1024.2
027000             MOVE "." TO PARDOT-X                                 DB1024.2
027100             MOVE REC-CT TO DOTVALUE.                             DB1024.2
027200     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      DB1024.2
027300     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               DB1024.2
027400        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 DB1024.2
027500          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 DB1024.2
027600     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              DB1024.2
027700     MOVE SPACE TO CORRECT-X.                                     DB1024.2
027800     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         DB1024.2
027900     MOVE     SPACE TO RE-MARK.                                   DB1024.2
028000 HEAD-ROUTINE.                                                    DB1024.2
028100     MOVE CCVS-H-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   DB1024.2
028200     MOVE CCVS-H-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.   DB1024.2
028300     MOVE CCVS-H-3 TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.   DB1024.2
028400 COLUMN-NAMES-ROUTINE.                                            DB1024.2
028500     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           DB1024.2
028600     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   DB1024.2
028700     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        DB1024.2
028800 END-ROUTINE.                                                     DB1024.2
028900     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.DB1024.2
029000 END-RTN-EXIT.                                                    DB1024.2
029100     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   DB1024.2
029200 END-ROUTINE-1.                                                   DB1024.2
029300      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      DB1024.2
029400      ERROR-HOLD. ADD DELETE-CNT TO ERROR-HOLD.                   DB1024.2
029500      ADD PASS-COUNTER TO ERROR-HOLD.                             DB1024.2
029600*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   DB1024.2
029700      MOVE PASS-COUNTER TO CCVS-E-4-1.                            DB1024.2
029800      MOVE ERROR-HOLD TO CCVS-E-4-2.                              DB1024.2
029900      MOVE CCVS-E-4 TO CCVS-E-2-2.                                DB1024.2
030000      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           DB1024.2
030100  END-ROUTINE-12.                                                 DB1024.2
030200      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        DB1024.2
030300     IF       ERROR-COUNTER IS EQUAL TO ZERO                      DB1024.2
030400         MOVE "NO " TO ERROR-TOTAL                                DB1024.2
030500         ELSE                                                     DB1024.2
030600         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       DB1024.2
030700     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           DB1024.2
030800     PERFORM WRITE-LINE.                                          DB1024.2
030900 END-ROUTINE-13.                                                  DB1024.2
031000     IF DELETE-CNT IS EQUAL TO ZERO                               DB1024.2
031100         MOVE "NO " TO ERROR-TOTAL  ELSE                          DB1024.2
031200         MOVE DELETE-CNT TO ERROR-TOTAL.                          DB1024.2
031300     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   DB1024.2
031400     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           DB1024.2
031500      IF   INSPECT-COUNTER EQUAL TO ZERO                          DB1024.2
031600          MOVE "NO " TO ERROR-TOTAL                               DB1024.2
031700      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   DB1024.2
031800      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            DB1024.2
031900      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          DB1024.2
032000     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           DB1024.2
032100 WRITE-LINE.                                                      DB1024.2
032200     ADD 1 TO RECORD-COUNT.                                       DB1024.2
032300*    IF RECORD-COUNT GREATER 50                                   DB1024.2
032400*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          DB1024.2
032500*        MOVE SPACE TO DUMMY-RECORD                               DB1024.2
032600*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  DB1024.2
032700*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             DB1024.2
032800*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     DB1024.2
032900*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          DB1024.2
033000*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          DB1024.2
033100*        MOVE ZERO TO RECORD-COUNT.                               DB1024.2
033200     PERFORM WRT-LN.                                              DB1024.2
033300 WRT-LN.                                                          DB1024.2
033400     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               DB1024.2
033500     MOVE SPACE TO DUMMY-RECORD.                                  DB1024.2
033600 BLANK-LINE-PRINT.                                                DB1024.2
033700     PERFORM WRT-LN.                                              DB1024.2
033800 FAIL-ROUTINE.                                                    DB1024.2
033900     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.   DB1024.2
034000     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.    DB1024.2
034100     MOVE "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.    DB1024.2
034200     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     DB1024.2
034300     GO TO FAIL-ROUTINE-EX.                                       DB1024.2
034400 FAIL-ROUTINE-WRITE.                                              DB1024.2
034500     MOVE TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE           DB1024.2
034600     MOVE TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES.   DB1024.2
034700 FAIL-ROUTINE-EX. EXIT.                                           DB1024.2
034800 BAIL-OUT.                                                        DB1024.2
034900     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.       DB1024.2
035000     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.               DB1024.2
035100 BAIL-OUT-WRITE.                                                  DB1024.2
035200     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  DB1024.2
035300     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     DB1024.2
035400 BAIL-OUT-EX. EXIT.                                               DB1024.2
035500 CCVS1-EXIT.                                                      DB1024.2
035600     EXIT.                                                        DB1024.2
035700 START-PROGRAM-TEST.                                              DB1024.2
035800     IF RESULT-FLAG IS NOT EQUAL TO 0                             DB1024.2
035900         MOVE "USE PROCEDURE EXECUTED" TO RE-MARK                 DB1024.2
036000         PERFORM FAIL                                             DB1024.2
036100         PERFORM START-PROGRAM-WRITE                              DB1024.2
036200         ELSE PERFORM PASS                                        DB1024.2
036300         GO TO START-PROGRAM-WRITE.                               DB1024.2
036400     MOVE "DEBUG-LINE; SEE NEXT LINE" TO RE-MARK.                 DB1024.2
036500     MOVE "<===  DEBUG-LINE" TO CORRECT-A.                        DB1024.2
036600     MOVE DBLINE-HOLD TO COMPUTED-A.                              DB1024.2
036700     PERFORM START-PROGRAM-WRITE.                                 DB1024.2
036800     MOVE "DEBUG-NAME; SEE NEXT LINE" TO RE-MARK.                 DB1024.2
036900     MOVE "<===  DEBUG-NAME" TO CORRECT-A.                        DB1024.2
037000     MOVE DBNAME-HOLD TO COMPUTED-A.                              DB1024.2
037100     PERFORM START-PROGRAM-WRITE.                                 DB1024.2
037200     MOVE "DEBUG-CONTENTS; SEE NEXT LINE" TO RE-MARK.             DB1024.2
037300     MOVE "<===  DEBUG-CONTENTS" TO CORRECT-A.                    DB1024.2
037400     MOVE DBCONT-HOLD TO COMPUTED-A.                              DB1024.2
037500     GO TO   START-PROGRAM-WRITE.                                 DB1024.2
037600 START-PROGRAM-DELETE.                                            DB1024.2
037700     PERFORM DE-LETE.                                             DB1024.2
037800 START-PROGRAM-WRITE.                                             DB1024.2
037900     MOVE "DEBUG ON PROC-NAME" TO FEATURE.                        DB1024.2
038000     MOVE "START-PROGRAM-TEST" TO PAR-NAME.                       DB1024.2
038100     PERFORM PRINT-DETAIL.                                        DB1024.2
038200     MOVE 0 TO RESULT-FLAG.                                       DB1024.2
038300 FALL-THROUGH-TEST.                                               DB1024.2
038400     IF RESULT-FLAG IS NOT EQUAL TO 0                             DB1024.2
038500         MOVE "USE PROCEDURE EXECUTED" TO RE-MARK                 DB1024.2
038600         PERFORM FAIL                                             DB1024.2
038700         PERFORM FALL-THROUGH-WRITE                               DB1024.2
038800         ELSE PERFORM PASS                                        DB1024.2
038900         GO TO FALL-THROUGH-WRITE.                                DB1024.2
039000     MOVE "DEBUG-LINE; SEE NEXT LINE" TO RE-MARK.                 DB1024.2
039100     MOVE "<===  DEBUG-LINE" TO CORRECT-A.                        DB1024.2
039200     MOVE DBLINE-HOLD TO COMPUTED-A.                              DB1024.2
039300     PERFORM FALL-THROUGH-WRITE.                                  DB1024.2
039400     MOVE "DEBUG-NAME; SEE NEXT LINE" TO RE-MARK.                 DB1024.2
039500     MOVE "<===  DEBUG-NAME" TO CORRECT-A.                        DB1024.2
039600     MOVE DBNAME-HOLD TO COMPUTED-A.                              DB1024.2
039700     PERFORM FALL-THROUGH-WRITE.                                  DB1024.2
039800     MOVE "DEBUG-CONTENTS; SEE NEXT LINE" TO RE-MARK.             DB1024.2
039900     MOVE "<===  DEBUG-CONTENTS" TO CORRECT-A.                    DB1024.2
040000     MOVE DBCONT-HOLD TO COMPUTED-A.                              DB1024.2
040100     GO TO   FALL-THROUGH-WRITE.                                  DB1024.2
040200 FALL-THROUGH-DELETE.                                             DB1024.2
040300     PERFORM DE-LETE.                                             DB1024.2
040400 FALL-THROUGH-WRITE.                                              DB1024.2
040500     MOVE "DEBUG ON PROC-NAME" TO FEATURE.                        DB1024.2
040600     MOVE "FALL-THROUGH-TEST" TO PAR-NAME.                        DB1024.2
040700     PERFORM PRINT-DETAIL.                                        DB1024.2
040800     MOVE 0 TO RESULT-FLAG.                                       DB1024.2
040900 PROC-SERIES-TEST.                                                DB1024.2
041000     IF RESULT-FLAG IS NOT EQUAL TO 0                             DB1024.2
041100         MOVE "USE PROCEDURE EXECUTED" TO RE-MARK                 DB1024.2
041200         PERFORM FAIL                                             DB1024.2
041300         PERFORM PROC-SERIES-WRITE                                DB1024.2
041400         ELSE PERFORM PASS                                        DB1024.2
041500         GO TO PROC-SERIES-WRITE.                                 DB1024.2
041600     MOVE "DEBUG-LINE; SEE NEXT LINE" TO RE-MARK.                 DB1024.2
041700     MOVE "<===  DEBUG-LINE" TO CORRECT-A.                        DB1024.2
041800     MOVE DBLINE-HOLD TO COMPUTED-A.                              DB1024.2
041900     PERFORM PROC-SERIES-WRITE.                                   DB1024.2
042000     MOVE "DEBUG-NAME; SEE NEXT LINE" TO RE-MARK.                 DB1024.2
042100     MOVE "<===  DEBUG-NAME" TO CORRECT-A.                        DB1024.2
042200     MOVE DBNAME-HOLD TO COMPUTED-A.                              DB1024.2
042300     PERFORM PROC-SERIES-WRITE.                                   DB1024.2
042400     MOVE "DEBUG-CONTENTS; SEE NEXT LINE" TO RE-MARK.             DB1024.2
042500     MOVE "<===  DEBUG-CONTENTS" TO CORRECT-A.                    DB1024.2
042600     MOVE DBCONT-HOLD TO COMPUTED-A.                              DB1024.2
042700     GO TO   PROC-SERIES-WRITE.                                   DB1024.2
042800 PROC-SERIES-DELETE.                                              DB1024.2
042900     PERFORM DE-LETE.                                             DB1024.2
043000 PROC-SERIES-WRITE.                                               DB1024.2
043100     MOVE "DEBUG ON PROC-NAME" TO FEATURE.                        DB1024.2
043200     MOVE "PROC-SERIES-TEST" TO PAR-NAME.                         DB1024.2
043300     PERFORM PRINT-DETAIL.                                        DB1024.2
043400     MOVE 0 TO RESULT-FLAG.                                       DB1024.2
043500 ALTERABLE-PARAGRAPH.                                             DB1024.2
043600     GO TO GO-TO-TEST.                                            DB1024.2
043700 FILLER-PARAGRAPH.                                                DB1024.2
043800     DISPLAY "ALTER FAILED AT ALTER-TEST-INIT".                   DB1024.2
043900     PERFORM FAIL.                                                DB1024.2
044000     GO TO ALTERED-GO-TO-TEST.                                    DB1024.2
044100 GO-TO-TEST.                                                      DB1024.2
044200     IF RESULT-FLAG IS NOT EQUAL TO 0                             DB1024.2
044300         MOVE "USE PROCEDURE EXECUTED" TO RE-MARK                 DB1024.2
044400         PERFORM FAIL                                             DB1024.2
044500         PERFORM GO-TO-WRITE                                      DB1024.2
044600         ELSE PERFORM PASS                                        DB1024.2
044700         GO TO GO-TO-WRITE.                                       DB1024.2
044800     MOVE "DEBUG-LINE; SEE NEXT LINE" TO RE-MARK.                 DB1024.2
044900     MOVE "<===  DEBUG-LINE" TO CORRECT-A.                        DB1024.2
045000     MOVE DBLINE-HOLD TO COMPUTED-A.                              DB1024.2
045100     PERFORM GO-TO-WRITE.                                         DB1024.2
045200     MOVE "DEBUG-NAME; SEE NEXT LINE" TO RE-MARK.                 DB1024.2
045300     MOVE "<===  DEBUG-NAME" TO CORRECT-A.                        DB1024.2
045400     MOVE DBNAME-HOLD TO COMPUTED-A.                              DB1024.2
045500     PERFORM GO-TO-WRITE.                                         DB1024.2
045600     MOVE "DEBUG-CONTENTS; SEE NEXT LINE" TO RE-MARK.             DB1024.2
045700     MOVE "<===  DEBUG-CONTENTS" TO CORRECT-A.                    DB1024.2
045800     MOVE DBCONT-HOLD TO COMPUTED-A.                              DB1024.2
045900     GO TO   GO-TO-WRITE.                                         DB1024.2
046000 GO-TO-DELETE.                                                    DB1024.2
046100     PERFORM DE-LETE.                                             DB1024.2
046200 GO-TO-WRITE.                                                     DB1024.2
046300     MOVE "DEBUG ON PROC-NAME" TO FEATURE.                        DB1024.2
046400     MOVE "GO-TO-TEST" TO PAR-NAME.                               DB1024.2
046500     PERFORM PRINT-DETAIL.                                        DB1024.2
046600     MOVE 0 TO RESULT-FLAG.                                       DB1024.2
046700 ALTER-TEST-INIT.                                                 DB1024.2
046800     ALTER ALTERABLE-PARAGRAPH TO PROCEED TO ALTERED-GO-TO-TEST.  DB1024.2
046900 ALTER-TEST.                                                      DB1024.2
047000     IF RESULT-FLAG IS NOT EQUAL TO 0                             DB1024.2
047100         MOVE "USE PROCEDURE EXECUTED" TO RE-MARK                 DB1024.2
047200         PERFORM FAIL                                             DB1024.2
047300         PERFORM ALTER-WRITE                                      DB1024.2
047400         ELSE PERFORM PASS                                        DB1024.2
047500         GO TO ALTER-WRITE.                                       DB1024.2
047600     MOVE "DEBUG-LINE; SEE NEXT LINE" TO RE-MARK.                 DB1024.2
047700     MOVE "<===  DEBUG-LINE" TO CORRECT-A.                        DB1024.2
047800     MOVE DBLINE-HOLD TO COMPUTED-A.                              DB1024.2
047900     PERFORM ALTER-WRITE.                                         DB1024.2
048000     MOVE "DEBUG-NAME; SEE NEXT LINE" TO RE-MARK.                 DB1024.2
048100     MOVE "<===  DEBUG-NAME" TO CORRECT-A.                        DB1024.2
048200     MOVE DBNAME-HOLD TO COMPUTED-A.                              DB1024.2
048300     PERFORM ALTER-WRITE.                                         DB1024.2
048400     MOVE "DEBUG-CONTENTS; SEE NEXT LINE" TO RE-MARK.             DB1024.2
048500     MOVE "<===  DEBUG-CONTENTS" TO CORRECT-A.                    DB1024.2
048600     MOVE DBCONT-HOLD TO COMPUTED-A.                              DB1024.2
048700     GO TO   ALTER-WRITE.                                         DB1024.2
048800 ALTER-DELETE.                                                    DB1024.2
048900     PERFORM DE-LETE.                                             DB1024.2
049000 ALTER-WRITE.                                                     DB1024.2
049100     MOVE "DEBUG ON PROC-NAME" TO FEATURE.                        DB1024.2
049200     MOVE "ALTER-TEST" TO PAR-NAME.                               DB1024.2
049300     PERFORM PRINT-DETAIL.                                        DB1024.2
049400     MOVE 0 TO RESULT-FLAG.                                       DB1024.2
049500 ALTER-WRITE-END.                                                 DB1024.2
049600     GO TO ALTERABLE-PARAGRAPH.                                   DB1024.2
049700 ALTERED-GO-TO-TEST.                                              DB1024.2
049800     IF RESULT-FLAG IS NOT EQUAL TO 0                             DB1024.2
049900         MOVE "USE PROCEDURE EXECUTED" TO RE-MARK                 DB1024.2
050000         PERFORM FAIL                                             DB1024.2
050100         PERFORM ALTERED-GO-TO-WRITE                              DB1024.2
050200         ELSE PERFORM PASS                                        DB1024.2
050300         GO TO ALTERED-GO-TO-WRITE.                               DB1024.2
050400     MOVE "DEBUG-LINE; SEE NEXT LINE" TO RE-MARK.                 DB1024.2
050500     MOVE "<===  DEBUG-LINE" TO CORRECT-A.                        DB1024.2
050600     MOVE DBLINE-HOLD TO COMPUTED-A.                              DB1024.2
050700     PERFORM ALTERED-GO-TO-WRITE.                                 DB1024.2
050800     MOVE "DEBUG-NAME; SEE NEXT LINE" TO RE-MARK.                 DB1024.2
050900     MOVE "<===  DEBUG-NAME" TO CORRECT-A.                        DB1024.2
051000     MOVE DBNAME-HOLD TO COMPUTED-A.                              DB1024.2
051100     PERFORM ALTERED-GO-TO-WRITE.                                 DB1024.2
051200     MOVE "DEBUG-CONTENTS; SEE NEXT LINE" TO RE-MARK.             DB1024.2
051300     MOVE "<===  DEBUG-CONTENTS" TO CORRECT-A.                    DB1024.2
051400     MOVE DBCONT-HOLD TO COMPUTED-A.                              DB1024.2
051500     GO TO   ALTERED-GO-TO-WRITE.                                 DB1024.2
051600 ALTERED-GO-TO-DELETE.                                            DB1024.2
051700     PERFORM DE-LETE.                                             DB1024.2
051800 ALTERED-GO-TO-WRITE.                                             DB1024.2
051900     MOVE "DEBUG ON PROC-NAME" TO FEATURE.                        DB1024.2
052000     MOVE "ALTERED-GO-TO-TEST" TO PAR-NAME.                       DB1024.2
052100     PERFORM PRINT-DETAIL.                                        DB1024.2
052200     MOVE 0 TO RESULT-FLAG.                                       DB1024.2
052300 PERF-ITERATION-TEST.                                             DB1024.2
052400     PERFORM LOOP-ROUTINE FIVE TIMES.                             DB1024.2
052500     IF RESULT-FLAG IS NOT EQUAL TO 0                             DB1024.2
052600         MOVE "USE PROCEDURE EXECUTED" TO RE-MARK                 DB1024.2
052700         PERFORM FAIL                                             DB1024.2
052800         PERFORM PERF-ITERATION-WRITE                             DB1024.2
052900         ELSE  PERFORM PASS                                       DB1024.2
053000         GO TO PERF-ITERATION-WRITE.                              DB1024.2
053100     MOVE "DEBUG-LINE; SEE NEXT LINE" TO RE-MARK.                 DB1024.2
053200     MOVE "<===  DEBUG-LINE" TO CORRECT-A.                        DB1024.2
053300     MOVE DBLINE-HOLD TO COMPUTED-A.                              DB1024.2
053400     PERFORM PERF-ITERATION-WRITE.                                DB1024.2
053500     MOVE "DEBUG-NAME; SEE NEXT LINE" TO RE-MARK.                 DB1024.2
053600     MOVE "<===  DEBUG-NAME" TO CORRECT-A.                        DB1024.2
053700     MOVE DBNAME-HOLD TO COMPUTED-A.                              DB1024.2
053800     PERFORM PERF-ITERATION-WRITE.                                DB1024.2
053900     MOVE "DEBUG-CONTENTS; SEE NEXT LINE" TO RE-MARK.             DB1024.2
054000     MOVE "<===  DEBUG-CONTENTS" TO CORRECT-A.                    DB1024.2
054100     MOVE DBCONT-HOLD TO COMPUTED-A.                              DB1024.2
054200     GO TO   PERF-ITERATION-WRITE.                                DB1024.2
054300 PERF-ITERATION-DELETE.                                           DB1024.2
054400     PERFORM DE-LETE.                                             DB1024.2
054500 PERF-ITERATION-WRITE.                                            DB1024.2
054600     MOVE "PERF-ITERATION-TEST" TO PAR-NAME.                      DB1024.2
054700     MOVE "DEBUG ON PROC-NAME" TO FEATURE.                        DB1024.2
054800     PERFORM PRINT-DETAIL.                                        DB1024.2
054900     MOVE 0 TO RESULT-FLAG.                                       DB1024.2
055000 PERF-ITERATION-END.                                              DB1024.2
055100     GO TO PERFORM-THRU-TEST.                                     DB1024.2
055200 LOOP-ROUTINE.                                                    DB1024.2
055300**NESTED PERFORMS ARE USED HERE TO ATTEMPT TO PREVENT OPTIMIZER   DB1024.2
055400* ACTION RESULTING IN LOOP UNFOLDING AND REDUCTION.               DB1024.2
055500     PERFORM DO-NOTHING.                                          DB1024.2
055600 DO-NOTHING.                                                      DB1024.2
055700     ADD A B C GIVING D.                                          DB1024.2
055800 DO-NOTHING-1.                                                    DB1024.2
055900     SUBTRACT A FROM B.                                           DB1024.2
056000 PERFORM-THRU-TEST.                                               DB1024.2
056100     PERFORM DO-NOTHING THRU DO-NOTHING-1 FIVE TIMES.             DB1024.2
056200     IF RESULT-FLAG IS NOT EQUAL TO 0                             DB1024.2
056300         MOVE "USE PROCEDURE EXECUTED" TO RE-MARK                 DB1024.2
056400         PERFORM FAIL                                             DB1024.2
056500         PERFORM PERFORM-THRU-WRITE                               DB1024.2
056600         ELSE PERFORM PASS                                        DB1024.2
056700         GO TO PERFORM-THRU-WRITE.                                DB1024.2
056800     MOVE "DEBUG-LINE; SEE NEXT LINE" TO RE-MARK.                 DB1024.2
056900     MOVE "<===  DEBUG-LINE" TO CORRECT-A.                        DB1024.2
057000     MOVE DBLINE-HOLD TO COMPUTED-A.                              DB1024.2
057100     PERFORM PERFORM-THRU-WRITE.                                  DB1024.2
057200     MOVE "DEBUG-NAME; SEE NEXT LINE" TO RE-MARK.                 DB1024.2
057300     MOVE "<===  DEBUG-NAME" TO CORRECT-A.                        DB1024.2
057400     MOVE DBNAME-HOLD TO COMPUTED-A.                              DB1024.2
057500     PERFORM PERFORM-THRU-WRITE.                                  DB1024.2
057600     MOVE "DEBUG-CONTENTS; SEE NEXT LINE" TO RE-MARK.             DB1024.2
057700     MOVE "<===  DEBUG-CONTENTS" TO CORRECT-A.                    DB1024.2
057800     MOVE DBCONT-HOLD TO COMPUTED-A.                              DB1024.2
057900     GO TO   PERFORM-THRU-WRITE.                                  DB1024.2
058000 PERFORM-THRU-DELETE.                                             DB1024.2
058100     PERFORM DE-LETE.                                             DB1024.2
058200 PERFORM-THRU-WRITE.                                              DB1024.2
058300     MOVE "PERFORM-THRU-TEST" TO PAR-NAME.                        DB1024.2
058400     MOVE "DEBUG ON PROC-NAME" TO FEATURE.                        DB1024.2
058500     PERFORM PRINT-DETAIL.                                        DB1024.2
058600     MOVE 0 TO RESULT-FLAG.                                       DB1024.2
058700 SIMPLE-PERFORM-TEST.                                             DB1024.2
058800     PERFORM LOOP-ROUTINE.                                        DB1024.2
058900     IF RESULT-FLAG IS NOT EQUAL TO 0                             DB1024.2
059000         MOVE "USE PROCEDURE EXECUTED" TO RE-MARK                 DB1024.2
059100         PERFORM FAIL                                             DB1024.2
059200         PERFORM SIMPLE-PERFORM-WRITE                             DB1024.2
059300         ELSE PERFORM PASS                                        DB1024.2
059400         GO TO SIMPLE-PERFORM-WRITE.                              DB1024.2
059500     MOVE "DEBUG-LINE; SEE NEXT LINE" TO RE-MARK.                 DB1024.2
059600     MOVE "<===  DEBUG-LINE" TO CORRECT-A.                        DB1024.2
059700     MOVE DBLINE-HOLD TO COMPUTED-A.                              DB1024.2
059800     PERFORM SIMPLE-PERFORM-WRITE.                                DB1024.2
059900     MOVE "DEBUG-NAME; SEE NEXT LINE" TO RE-MARK.                 DB1024.2
060000     MOVE "<===  DEBUG-NAME" TO CORRECT-A.                        DB1024.2
060100     MOVE DBNAME-HOLD TO COMPUTED-A.                              DB1024.2
060200     PERFORM SIMPLE-PERFORM-WRITE.                                DB1024.2
060300     MOVE "DEBUG-CONTENTS; SEE NEXT LINE" TO RE-MARK.             DB1024.2
060400     MOVE "<===  DEBUG-CONTENTS" TO CORRECT-A.                    DB1024.2
060500     MOVE DBCONT-HOLD TO COMPUTED-A.                              DB1024.2
060600     GO TO   SIMPLE-PERFORM-WRITE.                                DB1024.2
060700 SIMPLE-PERFORM-DELETE.                                           DB1024.2
060800     PERFORM DE-LETE.                                             DB1024.2
060900 SIMPLE-PERFORM-WRITE.                                            DB1024.2
061000     MOVE "SIMPLE-PERFORM-TEST" TO PAR-NAME.                      DB1024.2
061100     MOVE "DEBUG ON PROC-NAME" TO FEATURE.                        DB1024.2
061200     PERFORM PRINT-DETAIL.                                        DB1024.2
061300     MOVE 0 TO RESULT-FLAG.                                       DB1024.2
061400 DEBUG-LINE-TESTS-INIT.                                           DB1024.2
061500     MOVE "DEBUG LINE TESTS" TO FEATURE.                          DB1024.2
061600 DEBUG-LINE-TEST-01.                                              DB1024.2
061700     MOVE "COMPLETE ENTITY" TO RE-MARK.                           DB1024.2
061800     PERFORM FAIL.                                                DB1024.2
061900D    PERFORM PASS  SUBTRACT 1 FROM ERROR-COUNTER.                 DB1024.2
062000     GO TO DEBUG-LINE-WRITE-01.                                   DB1024.2
062100 DEBUG-LINE-DELETE-01.                                            DB1024.2
062200     PERFORM DE-LETE.                                             DB1024.2
062300 DEBUG-LINE-WRITE-01.                                             DB1024.2
062400     MOVE "DEBUG-LINE-TEST-01" TO PAR-NAME.                       DB1024.2
062500     PERFORM PRINT-DETAIL.                                        DB1024.2
062600 DEBUG-LINE-TEST-02.                                              DB1024.2
062700     MOVE "CONSECUTIVE DEBUG LINES" TO RE-MARK.                   DB1024.2
062800     PERFORM FAIL.                                                DB1024.2
062900D    PERFORM PASS.                                                DB1024.2
063000D    SUBTRACT 1 FROM ERROR-COUNTER.                               DB1024.2
063100     GO TO DEBUG-LINE-WRITE-02.                                   DB1024.2
063200 DEBUG-LINE-DELETE-02.                                            DB1024.2
063300     PERFORM DE-LETE.                                             DB1024.2
063400 DEBUG-LINE-WRITE-02.                                             DB1024.2
063500     MOVE "DEBUG-LINE-TEST-02" TO PAR-NAME.                       DB1024.2
063600     PERFORM PRINT-DETAIL.                                        DB1024.2
063700 DEBUG-LINE-TEST-03.                                              DB1024.2
063800     MOVE "BROKEN STATEMENTS" TO RE-MARK.                         DB1024.2
063900     PERFORM                                                      DB1024.2
064000D        PASS.  GO TO DEBUG-LINE-WRITE-03.                        DB1024.2
064100DDEBUG-LINE-TEST-03-A.    PERFORM                                 DB1024.2
064200                             FAIL.                                DB1024.2
064300                             GO TO DEBUG-LINE-WRITE-03.           DB1024.2
064400 DEBUG-LINE-DELETE-03.                                            DB1024.2
064500     PERFORM DE-LETE.                                             DB1024.2
064600 DEBUG-LINE-WRITE-03.                                             DB1024.2
064700     MOVE "DEBUG-LINE-TEST-03" TO PAR-NAME.                       DB1024.2
064800     PERFORM PRINT-DETAIL.                                        DB1024.2
064900 DEBUG-LINE-TEST-04.                                              DB1024.2
065000     MOVE "NESTED COMMENTS" TO RE-MARK.                           DB1024.2
065100D    PERFORM                                                      DB1024.2
065200*        FAIL.  GO TO DEBUG-LINE-WRITE-04.                        DB1024.2
065300*DEBUG-LINE-TEST-04-A.    PERFORM                                 DB1024.2
065400D                         PASS.  GO TO DEBUG-LINE-WRITE-04.       DB1024.2
065500 DEBUG-LINE-TEST-04-B.                                            DB1024.2
065600     MOVE "    FAILURE 04B" TO COMPUTED-A.                        DB1024.2
065700     PERFORM FAIL.                                                DB1024.2
065800     GO TO DEBUG-LINE-WRITE-04.                                   DB1024.2
065900 DEBUG-LINE-DELETE-04.                                            DB1024.2
066000     PERFORM DE-LETE.                                             DB1024.2
066100 DEBUG-LINE-WRITE-04.                                             DB1024.2
066200     MOVE "DEBUG-LINE-TEST-04" TO PAR-NAME.                       DB1024.2
066300     PERFORM PRINT-DETAIL.                                        DB1024.2
066400 DEBUG-LINE-TEST-05.                                              DB1024.2
066500     MOVE "NESTED INSIDE COMMENTS" TO RE-MARK.                    DB1024.2
066600*    PERFORM FAIL.                                                DB1024.2
066700*    GO TO DEBUG-LINE-WRITE-05.                                   DB1024.2
066800*DEBUG-LINE-TEST-05-A.                                            DB1024.2
066900D    PERFORM PASS.                                                DB1024.2
067000D    GO TO DEBUG-LINE-WRITE-05.                                   DB1024.2
067100*DEBUG-LINE-TEST-05-B.                                            DB1024.2
067200*    MOVE "    FAILURE 05B" TO COMPUTED-A.                        DB1024.2
067300*    PERFORM FAIL.  GO TO DEBUG-LINE-WRITE-05.                    DB1024.2
067400 DEBUG-LINE-TEST-05-C.                                            DB1024.2
067500     MOVE "    FAILURE 05C" TO COMPUTED-A.                        DB1024.2
067600     PERFORM FAIL.   GO TO DEBUG-LINE-WRITE-05.                   DB1024.2
067700 DEBUG-LINE-DELETE-05.                                            DB1024.2
067800     PERFORM DE-LETE.                                             DB1024.2
067900 DEBUG-LINE-WRITE-05.                                             DB1024.2
068000     MOVE "DEBUG-LINE-TEST-05" TO PAR-NAME.                       DB1024.2
068100     PERFORM PRINT-DETAIL.                                        DB1024.2
068200 CCVS-EXIT SECTION.                                               DB1024.2
068300 CCVS-999999.                                                     DB1024.2
068400     GO TO CLOSE-FILES.                                           DB1024.2
      *END-OF,DB102A                                                                  
