      *HEADER,COBOL,DB205A                                                            
000100 IDENTIFICATION DIVISION.                                         DB2054.2
000200 PROGRAM-ID.                                                      DB2054.2
000300     DB205A.                                                      DB2054.2
000400 AUTHOR.                                                          DB2054.2
000500     FEDERAL COMPILER TESTING CENTER.                             DB2054.2
000600 INSTALLATION.                                                    DB2054.2
000700     GENERAL SERVICES ADMINISTRATION                              DB2054.2
000800*     AUTOMATED DATA AND TELECOMMUNICATION SERVICE.                DB2054.2
000900     SOFTWARE DEVELOPMENT OFFICE.                                 DB2054.2
001000     5203 LEESBURG PIKE  SUITE 1100                               DB2054.2
001100     FALLS CHURCH VIRGINIA 22041.                                 DB2054.2
001200                                                                  DB2054.2
001300     PHONE   (703) 756-6153                                       DB2054.2
001400                                                                  DB2054.2
001500     " HIGH       ".                                              DB2054.2
001600 DATE-WRITTEN.                                                    DB2054.2
001700     CCVS-74 VERSION 4.0 - 1980 JULY 1.                           DB2054.2
001800     CREATION DATE     /    VALIDATION DATE                       DB2054.2
001900     "4.2 ".                                                      DB2054.2
002000 SECURITY.                                                        DB2054.2
002100     NONE.                                                        DB2054.2
002200*                                                                 DB2054.2
002300 ENVIRONMENT DIVISION.                                            DB2054.2
002400 CONFIGURATION SECTION.                                           DB2054.2
002500 SOURCE-COMPUTER.                                                 DB2054.2
002600     XXXXX082                                                     DB2054.2
002700         WITH DEBUGGING MODE.                                     DB2054.2
002800 OBJECT-COMPUTER.                                                 DB2054.2
002900     XXXXX083.                                                    DB2054.2
003000 INPUT-OUTPUT SECTION.                                            DB2054.2
003100 FILE-CONTROL.                                                    DB2054.2
003200     SELECT PRINT-FILE ASSIGN TO                                  DB2054.2
003300     XXXXX055.                                                    DB2054.2
003400 DATA DIVISION.                                                   DB2054.2
003500 FILE SECTION.                                                    DB2054.2
003600 FD  PRINT-FILE                                                   DB2054.2
003700     LABEL RECORDS                                                DB2054.2
003800     XXXXX084                                                     DB2054.2
003900     DATA RECORD IS PRINT-REC DUMMY-RECORD.                       DB2054.2
004000 01  PRINT-REC PICTURE X(120).                                    DB2054.2
004100 01  DUMMY-RECORD PICTURE X(120).                                 DB2054.2
004200 WORKING-STORAGE SECTION.                                         DB2054.2
004300 77  WORK-AREA PIC X(72).                                         DB2054.2
004400 01  ITEM-1.                                                      DB2054.2
004500     02  KEY-1 PIC 99.                                            DB2054.2
004600     02  LINE-1 PIC X(6).                                         DB2054.2
004700     02  NAME-1 PIC X(30).                                        DB2054.2
004800     02  UNQUAL-NAME-1 PIC X(30).                                 DB2054.2
004900     02  CONTENTS-1 PIC X(87).                                    DB2054.2
005000 01  TEST-RESULTS.                                                DB2054.2
005100     02 FILLER                    PICTURE X VALUE SPACE.          DB2054.2
005200     02 FEATURE                   PICTURE X(20) VALUE SPACE.      DB2054.2
005300     02 FILLER                    PICTURE X VALUE SPACE.          DB2054.2
005400     02 P-OR-F                    PICTURE X(5) VALUE SPACE.       DB2054.2
005500     02 FILLER                    PICTURE X  VALUE SPACE.         DB2054.2
005600     02  PAR-NAME.                                                DB2054.2
005700       03 FILLER PICTURE X(12) VALUE SPACE.                       DB2054.2
005800       03  PARDOT-X PICTURE X  VALUE SPACE.                       DB2054.2
005900       03 DOTVALUE PICTURE 99  VALUE ZERO.                        DB2054.2
006000       03 FILLER PIC X(5) VALUE SPACE.                            DB2054.2
006100     02 FILLER PIC X(10) VALUE SPACE.                             DB2054.2
006200     02 RE-MARK PIC X(61).                                        DB2054.2
006300 01  TEST-COMPUTED.                                               DB2054.2
006400     02 FILLER PIC X(30) VALUE SPACE.                             DB2054.2
006500     02 FILLER PIC X(17) VALUE "       COMPUTED=".                DB2054.2
006600     02 COMPUTED-X.                                               DB2054.2
006700     03 COMPUTED-A                PICTURE X(20) VALUE SPACE.      DB2054.2
006800     03 COMPUTED-N REDEFINES COMPUTED-A PICTURE -9(9).9(9).       DB2054.2
006900     03 COMPUTED-0V18 REDEFINES COMPUTED-A  PICTURE -.9(18).      DB2054.2
007000     03 COMPUTED-4V14 REDEFINES COMPUTED-A  PICTURE -9(4).9(14).  DB2054.2
007100     03 COMPUTED-14V4 REDEFINES COMPUTED-A  PICTURE -9(14).9(4).  DB2054.2
007200     03       CM-18V0 REDEFINES COMPUTED-A.                       DB2054.2
007300         04 COMPUTED-18V0                   PICTURE -9(18).       DB2054.2
007400         04 FILLER                          PICTURE X.            DB2054.2
007500     03 FILLER PIC X(50) VALUE SPACE.                             DB2054.2
007600 01  TEST-CORRECT.                                                DB2054.2
007700     02 FILLER PIC X(30) VALUE SPACE.                             DB2054.2
007800     02 FILLER PIC X(17) VALUE "       CORRECT =".                DB2054.2
007900     02 CORRECT-X.                                                DB2054.2
008000     03 CORRECT-A                 PICTURE X(20) VALUE SPACE.      DB2054.2
008100     03 CORRECT-N REDEFINES CORRECT-A PICTURE -9(9).9(9).         DB2054.2
008200     03 CORRECT-0V18 REDEFINES CORRECT-A    PICTURE -.9(18).      DB2054.2
008300     03 CORRECT-4V14 REDEFINES CORRECT-A    PICTURE -9(4).9(14).  DB2054.2
008400     03 CORRECT-14V4 REDEFINES CORRECT-A    PICTURE -9(14).9(4).  DB2054.2
008500     03      CR-18V0 REDEFINES CORRECT-A.                         DB2054.2
008600         04 CORRECT-18V0                    PICTURE -9(18).       DB2054.2
008700         04 FILLER                          PICTURE X.            DB2054.2
008800     03 FILLER PIC X(50) VALUE SPACE.                             DB2054.2
008900 01  CCVS-C-1.                                                    DB2054.2
009000     02 FILLER PICTURE IS X(99) VALUE IS " FEATURE              PADB2054.2
009100-    "SS  PARAGRAPH-NAME                                          DB2054.2
009200-    "        REMARKS".                                           DB2054.2
009300     02 FILLER PICTURE IS X(20) VALUE IS SPACE.                   DB2054.2
009400 01  CCVS-C-2.                                                    DB2054.2
009500     02 FILLER PICTURE IS X VALUE IS SPACE.                       DB2054.2
009600     02 FILLER PICTURE IS X(6) VALUE IS "TESTED".                 DB2054.2
009700     02 FILLER PICTURE IS X(15) VALUE IS SPACE.                   DB2054.2
009800     02 FILLER PICTURE IS X(4) VALUE IS "FAIL".                   DB2054.2
009900     02 FILLER PICTURE IS X(94) VALUE IS SPACE.                   DB2054.2
010000 01  REC-SKL-SUB PICTURE 9(2) VALUE ZERO.                         DB2054.2
010100 01  REC-CT PICTURE 99 VALUE ZERO.                                DB2054.2
010200 01  DELETE-CNT                   PICTURE 999  VALUE ZERO.        DB2054.2
010300 01  ERROR-COUNTER PICTURE IS 999 VALUE IS ZERO.                  DB2054.2
010400 01  INSPECT-COUNTER PIC 999 VALUE ZERO.                          DB2054.2
010500 01  PASS-COUNTER PIC 999 VALUE ZERO.                             DB2054.2
010600 01  TOTAL-ERROR PIC 999 VALUE ZERO.                              DB2054.2
010700 01  ERROR-HOLD PIC 999 VALUE ZERO.                               DB2054.2
010800 01  DUMMY-HOLD PIC X(120) VALUE SPACE.                           DB2054.2
010900 01  RECORD-COUNT PIC 9(5) VALUE ZERO.                            DB2054.2
011000 01  CCVS-H-1.                                                    DB2054.2
011100     02  FILLER   PICTURE X(27)  VALUE SPACE.                     DB2054.2
011200     02 FILLER PICTURE X(67) VALUE                                DB2054.2
011300     " FEDERAL COMPILER TESTING CENTER COBOL COMPILER VALIDATION  DB2054.2
011400-    " SYSTEM".                                                   DB2054.2
011500     02  FILLER     PICTURE X(26)  VALUE SPACE.                   DB2054.2
011600 01  CCVS-H-2.                                                    DB2054.2
011700     02 FILLER PICTURE X(52) VALUE IS                             DB2054.2
011800     "CCVS74 NCC  COPY, NOT FOR DISTRIBUTION.".                   DB2054.2
011900     02 FILLER PICTURE IS X(19) VALUE IS "TEST RESULTS SET-  ".   DB2054.2
012000     02 TEST-ID PICTURE IS X(9).                                  DB2054.2
012100     02 FILLER PICTURE IS X(40) VALUE IS SPACE.                   DB2054.2
012200 01  CCVS-H-3.                                                    DB2054.2
012300     02  FILLER PICTURE X(34) VALUE                               DB2054.2
012400     " FOR OFFICIAL USE ONLY    ".                                DB2054.2
012500     02  FILLER PICTURE X(58) VALUE                               DB2054.2
012600     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".DB2054.2
012700     02  FILLER PICTURE X(28) VALUE                               DB2054.2
012800     "  COPYRIGHT   1974 ".                                       DB2054.2
012900 01  CCVS-E-1.                                                    DB2054.2
013000     02 FILLER PICTURE IS X(52) VALUE IS SPACE.                   DB2054.2
013100     02 FILLER PICTURE IS X(14) VALUE IS "END OF TEST-  ".        DB2054.2
013200     02 ID-AGAIN PICTURE IS X(9).                                 DB2054.2
013300     02 FILLER PICTURE X(45) VALUE IS                             DB2054.2
013400     " NTIS DISTRIBUTION COBOL 74".                               DB2054.2
013500 01  CCVS-E-2.                                                    DB2054.2
013600     02  FILLER                   PICTURE X(31)  VALUE            DB2054.2
013700     SPACE.                                                       DB2054.2
013800     02  FILLER                   PICTURE X(21)  VALUE SPACE.     DB2054.2
013900     02 CCVS-E-2-2.                                               DB2054.2
014000         03 ERROR-TOTAL PICTURE IS XXX VALUE IS SPACE.            DB2054.2
014100         03 FILLER PICTURE IS X VALUE IS SPACE.                   DB2054.2
014200         03 ENDER-DESC PIC X(44) VALUE "ERRORS ENCOUNTERED".      DB2054.2
014300 01  CCVS-E-3.                                                    DB2054.2
014400     02  FILLER PICTURE X(22) VALUE                               DB2054.2
014500     " FOR OFFICIAL USE ONLY".                                    DB2054.2
014600     02  FILLER PICTURE X(12) VALUE SPACE.                        DB2054.2
014700     02  FILLER PICTURE X(58) VALUE                               DB2054.2
014800     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".DB2054.2
014900     02  FILLER PICTURE X(13) VALUE SPACE.                        DB2054.2
015000     02 FILLER PIC X(15) VALUE " COPYRIGHT 1974".                 DB2054.2
015100 01  CCVS-E-4.                                                    DB2054.2
015200     02 CCVS-E-4-1 PIC XXX VALUE SPACE.                           DB2054.2
015300     02 FILLER PIC XXXX VALUE " OF ".                             DB2054.2
015400     02 CCVS-E-4-2 PIC XXX VALUE SPACE.                           DB2054.2
015500     02 FILLER PIC X(40) VALUE                                    DB2054.2
015600      "  TESTS WERE EXECUTED SUCCESSFULLY".                       DB2054.2
015700 01  XXINFO.                                                      DB2054.2
015800     02 FILLER PIC X(30) VALUE "        *** INFORMATION  ***".    DB2054.2
015900     02 INFO-TEXT.                                                DB2054.2
016000     04 FILLER PIC X(20) VALUE SPACE.                             DB2054.2
016100     04 XXCOMPUTED PIC X(20).                                     DB2054.2
016200     04 FILLER PIC X(5) VALUE SPACE.                              DB2054.2
016300     04 XXCORRECT PIC X(20).                                      DB2054.2
016400 01  HYPHEN-LINE.                                                 DB2054.2
016500     02 FILLER PICTURE IS X VALUE IS SPACE.                       DB2054.2
016600     02 FILLER PICTURE IS X(65) VALUE IS "************************DB2054.2
016700-    "*****************************************".                 DB2054.2
016800     02 FILLER PICTURE IS X(54) VALUE IS "************************DB2054.2
016900-    "******************************".                            DB2054.2
017000 01  CCVS-PGM-ID PIC X(6) VALUE                                   DB2054.2
017100     "DB205A".                                                    DB2054.2
017200 COMMUNICATION SECTION.                                           DB2054.2
017300 CD  CM-INQUE FOR INPUT.                                          DB2054.2
017400 01  INQUE-SPECS.                                                 DB2054.2
017500     02  IN-QUEUE PIC X(12) VALUE                                 DB2054.2
017600     XXXXX030.                                                    DB2054.2
017700     02  FILLER PIC X(75) VALUE SPACES.                           DB2054.2
017800 CD  CM-OUTQUE FOR OUTPUT.                                        DB2054.2
017900 01  OUTQUE-SPECS.                                                DB2054.2
018000     02  DEST-COUNT PIC 9(4) VALUE IS 1.                          DB2054.2
018100     02  OUT-LENGTH PIC 9(4) VALUE IS 72.                         DB2054.2
018200     02  OUTT-STATUS PIC X(3).                                    DB2054.2
018300     02  SYM-DEST PIC X(12) VALUE IS                              DB2054.2
018400     XXXXX032.                                                    DB2054.2
018500 PROCEDURE DIVISION.                                              DB2054.2
018600 DECLARATIVES.                                                    DB2054.2
018700 DEBUG-PROCEDURE SECTION.                                         DB2054.2
018800     USE FOR DEBUGGING ON CM-INQUE  CM-OUTQUE.                    DB2054.2
018900 COMMUNICATION-PROC.                                              DB2054.2
019000     MOVE 1 TO KEY-1.                                             DB2054.2
019100     MOVE DEBUG-LINE TO LINE-1.                                   DB2054.2
019200     MOVE DEBUG-NAME TO NAME-1  UNQUAL-NAME-1.                    DB2054.2
019300     MOVE DEBUG-CONTENTS TO CONTENTS-1.                           DB2054.2
019400     INSPECT UNQUAL-NAME-1 REPLACING CHARACTERS BY SPACES         DB2054.2
019500         AFTER INITIAL SPACE.                                     DB2054.2
019600 END DECLARATIVES.                                                DB2054.2
019700 CCVS1 SECTION.                                                   DB2054.2
019800 OPEN-FILES.                                                      DB2054.2
019900     OPEN     OUTPUT PRINT-FILE.                                  DB2054.2
020000     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   DB2054.2
020100     MOVE    SPACE TO TEST-RESULTS.                               DB2054.2
020200     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             DB2054.2
020300     GO TO CCVS1-EXIT.                                            DB2054.2
020400 CLOSE-FILES.                                                     DB2054.2
020500     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   DB2054.2
020600 TERMINATE-CCVS.                                                  DB2054.2
020700*    EXIT PROGRAM.                                                DB2054.2
020800*TERMINATE-CALL.                                                  DB2054.2
020900     STOP     RUN.                                                DB2054.2
021000 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         DB2054.2
021100 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           DB2054.2
021200 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          DB2054.2
021300 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-CNT.          DB2054.2
021400     MOVE "****TEST DELETED****" TO RE-MARK.                      DB2054.2
021500 PRINT-DETAIL.                                                    DB2054.2
021600     IF REC-CT NOT EQUAL TO ZERO                                  DB2054.2
021700             MOVE "." TO PARDOT-X                                 DB2054.2
021800             MOVE REC-CT TO DOTVALUE.                             DB2054.2
021900     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      DB2054.2
022000     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               DB2054.2
022100        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 DB2054.2
022200          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 DB2054.2
022300     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              DB2054.2
022400     MOVE SPACE TO CORRECT-X.                                     DB2054.2
022500     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         DB2054.2
022600     MOVE     SPACE TO RE-MARK.                                   DB2054.2
022700 HEAD-ROUTINE.                                                    DB2054.2
022800     MOVE CCVS-H-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   DB2054.2
022900     MOVE CCVS-H-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.   DB2054.2
023000     MOVE CCVS-H-3 TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.   DB2054.2
023100 COLUMN-NAMES-ROUTINE.                                            DB2054.2
023200     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           DB2054.2
023300     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   DB2054.2
023400     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        DB2054.2
023500 END-ROUTINE.                                                     DB2054.2
023600     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.DB2054.2
023700 END-RTN-EXIT.                                                    DB2054.2
023800     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   DB2054.2
023900 END-ROUTINE-1.                                                   DB2054.2
024000      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      DB2054.2
024100      ERROR-HOLD. ADD DELETE-CNT TO ERROR-HOLD.                   DB2054.2
024200      ADD PASS-COUNTER TO ERROR-HOLD.                             DB2054.2
024300*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   DB2054.2
024400      MOVE PASS-COUNTER TO CCVS-E-4-1.                            DB2054.2
024500      MOVE ERROR-HOLD TO CCVS-E-4-2.                              DB2054.2
024600      MOVE CCVS-E-4 TO CCVS-E-2-2.                                DB2054.2
024700      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           DB2054.2
024800  END-ROUTINE-12.                                                 DB2054.2
024900      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        DB2054.2
025000     IF       ERROR-COUNTER IS EQUAL TO ZERO                      DB2054.2
025100         MOVE "NO " TO ERROR-TOTAL                                DB2054.2
025200         ELSE                                                     DB2054.2
025300         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       DB2054.2
025400     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           DB2054.2
025500     PERFORM WRITE-LINE.                                          DB2054.2
025600 END-ROUTINE-13.                                                  DB2054.2
025700     IF DELETE-CNT IS EQUAL TO ZERO                               DB2054.2
025800         MOVE "NO " TO ERROR-TOTAL  ELSE                          DB2054.2
025900         MOVE DELETE-CNT TO ERROR-TOTAL.                          DB2054.2
026000     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   DB2054.2
026100     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           DB2054.2
026200      IF   INSPECT-COUNTER EQUAL TO ZERO                          DB2054.2
026300          MOVE "NO " TO ERROR-TOTAL                               DB2054.2
026400      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   DB2054.2
026500      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            DB2054.2
026600      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          DB2054.2
026700     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           DB2054.2
026800 WRITE-LINE.                                                      DB2054.2
026900     ADD 1 TO RECORD-COUNT.                                       DB2054.2
027000*    IF RECORD-COUNT GREATER 50                                   DB2054.2
027100*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          DB2054.2
027200*        MOVE SPACE TO DUMMY-RECORD                               DB2054.2
027300*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  DB2054.2
027400*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             DB2054.2
027500*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     DB2054.2
027600*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          DB2054.2
027700*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          DB2054.2
027800*        MOVE ZERO TO RECORD-COUNT.                               DB2054.2
027900     PERFORM WRT-LN.                                              DB2054.2
028000 WRT-LN.                                                          DB2054.2
028100     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               DB2054.2
028200     MOVE SPACE TO DUMMY-RECORD.                                  DB2054.2
028300 BLANK-LINE-PRINT.                                                DB2054.2
028400     PERFORM WRT-LN.                                              DB2054.2
028500 FAIL-ROUTINE.                                                    DB2054.2
028600     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.   DB2054.2
028700     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.    DB2054.2
028800     MOVE "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.    DB2054.2
028900     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     DB2054.2
029000     GO TO FAIL-ROUTINE-EX.                                       DB2054.2
029100 FAIL-ROUTINE-WRITE.                                              DB2054.2
029200     MOVE TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE           DB2054.2
029300     MOVE TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES.   DB2054.2
029400 FAIL-ROUTINE-EX. EXIT.                                           DB2054.2
029500 BAIL-OUT.                                                        DB2054.2
029600     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.       DB2054.2
029700     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.               DB2054.2
029800 BAIL-OUT-WRITE.                                                  DB2054.2
029900     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  DB2054.2
030000     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     DB2054.2
030100 BAIL-OUT-EX. EXIT.                                               DB2054.2
030200 CCVS1-EXIT.                                                      DB2054.2
030300     EXIT.                                                        DB2054.2
030400 BEGIN-DB205A-TESTS SECTION.                                      DB2054.2
030500 DISABLE-TEST-1-INIT.                                             DB2054.2
030600     MOVE SPACES TO ITEM-1.                                       DB2054.2
030700     MOVE 0 TO KEY-1.                                             DB2054.2
030800     MOVE "DISABLE-TEST-1" TO PAR-NAME.                           DB2054.2
030900     MOVE "DISABLE CD-NAME" TO FEATURE.                           DB2054.2
031000 DISABLE-TEST-1.                                                  DB2054.2
031100     DISABLE INPUT CM-INQUE WITH KEY                              DB2054.2
031200     XXXXX031.                                                    DB2054.2
031300     IF KEY-1 IS EQUAL TO 1                                       DB2054.2
031400         PERFORM PASS                                             DB2054.2
031500         MOVE "DEBUG PROC WAS EXECUTED" TO RE-MARK                DB2054.2
031600         PERFORM PRINT-DETAIL                                     DB2054.2
031700         GO TO DISABLE-TEST-1A                                    DB2054.2
031800     ELSE  PERFORM FAIL                                           DB2054.2
031900         MOVE "DEBUG PROC NOT EXECUTED" TO RE-MARK                DB2054.2
032000         PERFORM PRINT-DETAIL                                     DB2054.2
032100         PERFORM DELETE-DISABLE-TEST-1-SUBTEST                    DB2054.2
032200         GO TO ENABLE-TEST-1-INIT.                                DB2054.2
032300 DISABLE-TEST-1-DELETE.                                           DB2054.2
032400     PERFORM DE-LETE.                                             DB2054.2
032500     PERFORM PRINT-DETAIL.                                        DB2054.2
032600     PERFORM DELETE-DISABLE-TEST-1-SUBTEST.                       DB2054.2
032700     GO TO ENABLE-TEST-1-INIT.                                    DB2054.2
032800 DELETE-DISABLE-TEST-1-SUBTEST.                                   DB2054.2
032900     MOVE "DISABLE-TEST-1A" TO PAR-NAME.                          DB2054.2
033000     PERFORM DE-LETE.                                             DB2054.2
033100     PERFORM PRINT-DETAIL.                                        DB2054.2
033200 DISABLE-TEST-1A.                                                 DB2054.2
033300     MOVE "DISABLE-TEST-1A" TO PAR-NAME.                          DB2054.2
033400     MOVE "DEBUG-CONTENTS" TO RE-MARK.                            DB2054.2
033500     MOVE "DISABLE-TEST-1A" TO PAR-NAME.                          DB2054.2
033600     IF CONTENTS-1 IS EQUAL TO INQUE-SPECS                        DB2054.2
033700         PERFORM PASS                                             DB2054.2
033800         PERFORM PRINT-DETAIL                                     DB2054.2
033900         GO TO ENABLE-TEST-1-INIT                                 DB2054.2
034000     ELSE  PERFORM FAIL                                           DB2054.2
034100         MOVE "1ST LINE FOLLOWING" TO CORRECT-A                   DB2054.2
034200         MOVE "2ND LINE FOLLOWING" TO COMPUTED-A                  DB2054.2
034300         PERFORM PRINT-DETAIL.                                    DB2054.2
034400     MOVE INQUE-SPECS TO PRINT-REC.                               DB2054.2
034500     PERFORM WRITE-LINE.                                          DB2054.2
034600     MOVE CONTENTS-1 TO PRINT-REC.                                DB2054.2
034700     PERFORM WRITE-LINE.                                          DB2054.2
034800 ENABLE-TEST-1-INIT.                                              DB2054.2
034900     MOVE SPACES TO ITEM-1.                                       DB2054.2
035000     MOVE 0 TO KEY-1.                                             DB2054.2
035100     MOVE "ENABLE-TEST-1" TO PAR-NAME.                            DB2054.2
035200     MOVE "ENABLE CD-NAME" TO FEATURE.                            DB2054.2
035300 ENABLE-TEST-1.                                                   DB2054.2
035400     ENABLE OUTPUT CM-OUTQUE WITH KEY                             DB2054.2
035500     XXXXX033.                                                    DB2054.2
035600     IF KEY-1 IS EQUAL TO 1                                       DB2054.2
035700         MOVE "DEBUG PROC WAS EXECUTED" TO RE-MARK                DB2054.2
035800         PERFORM PASS                                             DB2054.2
035900         PERFORM PRINT-DETAIL                                     DB2054.2
036000         GO TO ENABLE-TEST-1A                                     DB2054.2
036100     ELSE  PERFORM FAIL                                           DB2054.2
036200         MOVE "DEBUG PROC NOT EXECUTED" TO RE-MARK                DB2054.2
036300         PERFORM PRINT-DETAIL                                     DB2054.2
036400         PERFORM DELETE-ENABLE-TEST-1-SUBTEST                     DB2054.2
036500         GO TO ACCEPT-TEST-1-INIT.                                DB2054.2
036600 ENABLE-TEST-1-DELETE.                                            DB2054.2
036700     PERFORM DE-LETE.                                             DB2054.2
036800     PERFORM PRINT-DETAIL.                                        DB2054.2
036900     PERFORM DELETE-ENABLE-TEST-1-SUBTEST.                        DB2054.2
037000     GO TO ACCEPT-TEST-1-INIT.                                    DB2054.2
037100 DELETE-ENABLE-TEST-1-SUBTEST.                                    DB2054.2
037200     MOVE "ENABLE-TEST-1A" TO PAR-NAME.                           DB2054.2
037300     PERFORM DE-LETE.                                             DB2054.2
037400     PERFORM PRINT-DETAIL.                                        DB2054.2
037500 ENABLE-TEST-1A.                                                  DB2054.2
037600     MOVE "ENABLE-TEST-1A" TO PAR-NAME.                           DB2054.2
037700     MOVE "DEBUG-CONTENTS" TO RE-MARK.                            DB2054.2
037800     IF CONTENTS-1 IS EQUAL TO OUTQUE-SPECS                       DB2054.2
037900         PERFORM PASS                                             DB2054.2
038000         PERFORM PRINT-DETAIL                                     DB2054.2
038100         GO TO ACCEPT-TEST-1-INIT                                 DB2054.2
038200     ELSE  PERFORM FAIL                                           DB2054.2
038300         MOVE "1ST LINE FOLLOWING" TO CORRECT-A                   DB2054.2
038400         MOVE "2ND LINE FOLLOWING" TO COMPUTED-A                  DB2054.2
038500         PERFORM PRINT-DETAIL.                                    DB2054.2
038600     MOVE OUTQUE-SPECS TO PRINT-REC.                              DB2054.2
038700     PERFORM WRITE-LINE.                                          DB2054.2
038800     MOVE CONTENTS-1 TO PRINT-REC.                                DB2054.2
038900     PERFORM WRITE-LINE.                                          DB2054.2
039000 ACCEPT-TEST-1-INIT.                                              DB2054.2
039100     MOVE SPACES TO ITEM-1.                                       DB2054.2
039200     MOVE 0 TO KEY-1.                                             DB2054.2
039300     MOVE "ACCEPT-TEST-1" TO PAR-NAME.                            DB2054.2
039400     MOVE "ACCEPT CD-NAME" TO FEATURE.                            DB2054.2
039500 ACCEPT-TEST-1.                                                   DB2054.2
039600     ACCEPT CM-INQUE MESSAGE COUNT.                               DB2054.2
039700     IF KEY-1 IS EQUAL TO 1                                       DB2054.2
039800         MOVE "DEBUG PROC WAS EXECUTED" TO RE-MARK                DB2054.2
039900         PERFORM PASS                                             DB2054.2
040000         PERFORM PRINT-DETAIL                                     DB2054.2
040100         GO TO ACCEPT-TEST-1A                                     DB2054.2
040200     ELSE  PERFORM FAIL                                           DB2054.2
040300         MOVE "DEBUG PROC NOT EXECUTED" TO RE-MARK                DB2054.2
040400         PERFORM PRINT-DETAIL                                     DB2054.2
040500         PERFORM DELETE-ACCEPT-TEST-1-SUBTEST                     DB2054.2
040600         GO TO RECEIVE-TEST-1-INIT.                               DB2054.2
040700 ACCEPT-TEST-1-DELETE.                                            DB2054.2
040800     PERFORM DE-LETE.                                             DB2054.2
040900     PERFORM PRINT-DETAIL.                                        DB2054.2
041000     PERFORM DELETE-ACCEPT-TEST-1-SUBTEST.                        DB2054.2
041100     GO TO RECEIVE-TEST-1-INIT.                                   DB2054.2
041200 DELETE-ACCEPT-TEST-1-SUBTEST.                                    DB2054.2
041300     MOVE "ACCEPT-TEST-1-1A" TO PAR-NAME                          DB2054.2
041400     PERFORM DE-LETE.                                             DB2054.2
041500     PERFORM PRINT-DETAIL.                                        DB2054.2
041600 ACCEPT-TEST-1A.                                                  DB2054.2
041700     MOVE "ACCEPT-TEST-1A" TO PAR-NAME.                           DB2054.2
041800     MOVE "DEBUG-CONTENTS" TO RE-MARK.                            DB2054.2
041900     IF CONTENTS-1 IS EQUAL TO INQUE-SPECS                        DB2054.2
042000         PERFORM PASS                                             DB2054.2
042100         PERFORM PRINT-DETAIL                                     DB2054.2
042200         GO TO RECEIVE-TEST-1-INIT                                DB2054.2
042300     ELSE    PERFORM FAIL                                         DB2054.2
042400         MOVE "1ST LINE FOLLOWING" TO CORRECT-A                   DB2054.2
042500         MOVE "2ND LINE FOLLOWING" TO COMPUTED-A                  DB2054.2
042600         PERFORM PRINT-DETAIL.                                    DB2054.2
042700     MOVE INQUE-SPECS TO PRINT-REC.                               DB2054.2
042800     PERFORM WRITE-LINE.                                          DB2054.2
042900     MOVE CONTENTS-1 TO PRINT-REC.                                DB2054.2
043000     PERFORM WRITE-LINE.                                          DB2054.2
043100 RECEIVE-TEST-1-INIT.                                             DB2054.2
043200     MOVE SPACES TO ITEM-1.                                       DB2054.2
043300     MOVE 0 TO KEY-1.                                             DB2054.2
043400     MOVE "RECEIVE-TEST-1" TO PAR-NAME.                           DB2054.2
043500     MOVE "RECEIVE W/ NO DATA" TO FEATURE.                        DB2054.2
043600 RECEIVE-TEST-1.                                                  DB2054.2
043700     RECEIVE CM-INQUE MESSAGE INTO WORK-AREA                      DB2054.2
043800         NO DATA GO TO RECEIVE-TEST-1-CONT.                       DB2054.2
043900     GO TO RECEIVE-TEST-1-INIT.                                   DB2054.2
044000 RECEIVE-TEST-1-DELETE.                                           DB2054.2
044100     PERFORM DE-LETE.                                             DB2054.2
044200     PERFORM PRINT-DETAIL.                                        DB2054.2
044300     GO TO SEND-TEST-1-INIT.                                      DB2054.2
044400 RECEIVE-TEST-1-CONT.                                             DB2054.2
044500     IF KEY-1 IS EQUAL TO 0                                       DB2054.2
044600         PERFORM PASS                                             DB2054.2
044700         MOVE "DEBUG PROC NOT EXECUTED" TO RE-MARK                DB2054.2
044800     ELSE  PERFORM FAIL                                           DB2054.2
044900         MOVE "DEBUG PROC WAS EXECUTED" TO RE-MARK.               DB2054.2
045000     PERFORM PRINT-DETAIL.                                        DB2054.2
045100 SEND-TEST-1-INIT.                                                DB2054.2
045200     ENABLE INPUT CM-INQUE WITH KEY                               DB2054.2
045300     XXXXX031.                                                    DB2054.2
045400     MOVE SPACES TO ITEM-1.                                       DB2054.2
045500     MOVE 0 TO KEY-1.                                             DB2054.2
045600     MOVE "SEND-TEST-1" TO PAR-NAME.                              DB2054.2
045700     MOVE "SEND CD-NAME" TO FEATURE.                              DB2054.2
045800 SEND-TEST-1.                                                     DB2054.2
045900     MOVE "ENTER ONE MESSAGE NOW." TO WORK-AREA.                  DB2054.2
046000******************************************************************DB2054.2
046100*    THE DEBUG-LINE (INSPT) SUBTEST FOR THE TEST NAMED IN THE    *DB2054.2
046200*    OUTPUT REPORT AS "SEND-TEST-1A" SHOULD POINT TO THE         *DB2054.2
046300*    EXECUTABLE STATEMENT WHICH FOLLOWS THIS COMMENT SET AND     *DB2054.2
046400*    WHICH READS, "SEND CM-OUTQUE FROM WORK-AREA WITH EGI.".     *DB2054.2
046500******************************************************************DB2054.2
046600     SEND CM-OUTQUE FROM WORK-AREA WITH EGI.                      DB2054.2
046700     IF KEY-1 IS EQUAL TO 1                                       DB2054.2
046800         PERFORM PASS                                             DB2054.2
046900         MOVE "DEBUG PROC WAS EXECUTED" TO RE-MARK                DB2054.2
047000         PERFORM PRINT-DETAIL                                     DB2054.2
047100         GO TO SEND-TEST-1A                                       DB2054.2
047200     ELSE  PERFORM FAIL                                           DB2054.2
047300         MOVE "DEBUG PROC NOT EXECUTED" TO RE-MARK                DB2054.2
047400         PERFORM PRINT-DETAIL                                     DB2054.2
047500         PERFORM DELETE-SEND-TEST-1-SUBTESTS                      DB2054.2
047600         GO TO RECEIVE-TEST-2-INIT.                               DB2054.2
047700 SEND-TEST-1-DELETE.                                              DB2054.2
047800     PERFORM DE-LETE.                                             DB2054.2
047900     PERFORM PRINT-DETAIL.                                        DB2054.2
048000     PERFORM DELETE-SEND-TEST-1-SUBTESTS.                         DB2054.2
048100     GO TO RECEIVE-TEST-2-INIT.                                   DB2054.2
048200 DELETE-SEND-TEST-1-SUBTESTS.                                     DB2054.2
048300     MOVE "SEND-TEST-1A" TO PAR-NAME.                             DB2054.2
048400     PERFORM DE-LETE.                                             DB2054.2
048500     PERFORM PRINT-DETAIL.                                        DB2054.2
048600     MOVE "SEND-TEST-1B" TO PAR-NAME.                             DB2054.2
048700     PERFORM DE-LETE.                                             DB2054.2
048800     PERFORM PRINT-DETAIL.                                        DB2054.2
048900     MOVE "SEND-TEST-1C" TO PAR-NAME.                             DB2054.2
049000     PERFORM DE-LETE.                                             DB2054.2
049100     PERFORM PRINT-DETAIL.                                        DB2054.2
049200 SEND-TEST-1A.                                                    DB2054.2
049300     MOVE "SEND-TEST-1A" TO PAR-NAME.                             DB2054.2
049400     MOVE "DEBUG-LINE; SEE NEXT LINE" TO RE-MARK.                 DB2054.2
049500     MOVE "<===  DEBUG-LINE" TO CORRECT-A.                        DB2054.2
049600     MOVE LINE-1 TO COMPUTED-A.                                   DB2054.2
049700     PERFORM INSPT.                                               DB2054.2
049800     PERFORM PRINT-DETAIL.                                        DB2054.2
049900 SEND-TEST-1B.                                                    DB2054.2
050000     MOVE "SEND-TEST-1B" TO PAR-NAME.                             DB2054.2
050100     IF UNQUAL-NAME-1 IS EQUAL TO "CM-OUTQUE"                     DB2054.2
050200         PERFORM PASS                                             DB2054.2
050300     ELSE PERFORM FAIL                                            DB2054.2
050400         MOVE "CM-OUTQUE" TO CORRECT-A                            DB2054.2
050500         MOVE NAME-1 TO COMPUTED-A.                               DB2054.2
050600     MOVE "DEBUG-NAME" TO RE-MARK.                                DB2054.2
050700     PERFORM PRINT-DETAIL.                                        DB2054.2
050800 SEND-TEST-1C.                                                    DB2054.2
050900     MOVE "SEND-TEST-1C" TO PAR-NAME.                             DB2054.2
051000     MOVE "DEBUG-CONTENTS" TO RE-MARK.                            DB2054.2
051100     IF CONTENTS-1 IS EQUAL TO OUTQUE-SPECS                       DB2054.2
051200         PERFORM PASS                                             DB2054.2
051300         PERFORM PRINT-DETAIL                                     DB2054.2
051400         GO TO RECEIVE-TEST-2-INIT                                DB2054.2
051500     ELSE  PERFORM FAIL                                           DB2054.2
051600         MOVE "1ST LINE FOLLOWING" TO CORRECT-A                   DB2054.2
051700         MOVE "2ND LINE FOLLOWING" TO COMPUTED-A                  DB2054.2
051800         PERFORM PRINT-DETAIL.                                    DB2054.2
051900     MOVE OUTQUE-SPECS TO PRINT-REC.                              DB2054.2
052000     PERFORM WRITE-LINE.                                          DB2054.2
052100     MOVE CONTENTS-1 TO PRINT-REC.                                DB2054.2
052200     PERFORM WRITE-LINE.                                          DB2054.2
052300 RECEIVE-TEST-2-INIT.                                             DB2054.2
052400     MOVE SPACES TO ITEM-1.                                       DB2054.2
052500     MOVE 0 TO KEY-1.                                             DB2054.2
052600     MOVE "RECEIVE-TEST-2" TO PAR-NAME.                           DB2054.2
052700     MOVE "RECEIVE W/O NO DATA" TO FEATURE.                       DB2054.2
052800******************************************************************DB2054.2
052900*    THE DEBUG-LINE (INSPT) SUBTEST FOR THE TEST NAMED IN THE    *DB2054.2
053000*    OUTPUT REPORT AS "RECEIVE-TEST-2A" SHOULD POINT TO THE      *DB2054.2
053100*    EXECUTABLE STATEMENT WHICH FOLLOWS THIS COMMENT SET AND     *DB2054.2
053200*    WHICH READS, "RECEIVE CM-INQUE MESSAGE INTO WORK-AREA.".    *DB2054.2
053300******************************************************************DB2054.2
053400 RECEIVE-TEST-2.                                                  DB2054.2
053500     RECEIVE CM-INQUE MESSAGE INTO WORK-AREA.                     DB2054.2
053600     IF KEY-1 IS EQUAL TO 1                                       DB2054.2
053700         PERFORM PASS                                             DB2054.2
053800         MOVE "DEBUG PROC WAS EXECUTED" TO RE-MARK                DB2054.2
053900         PERFORM PRINT-DETAIL                                     DB2054.2
054000         GO TO RECEIVE-TEST-2A                                    DB2054.2
054100     ELSE  PERFORM FAIL                                           DB2054.2
054200         MOVE "DEBUG PROC NOT EXECUTED" TO RE-MARK                DB2054.2
054300         PERFORM PRINT-DETAIL                                     DB2054.2
054400         PERFORM DELETE-RECEIVE-TEST-2-SUBTESTS                   DB2054.2
054500         GO TO END-OF-DB205A.                                     DB2054.2
054600 DELETE-RECEIVE-TEST-2.                                           DB2054.2
054700     PERFORM DE-LETE.                                             DB2054.2
054800     PERFORM PRINT-DETAIL.                                        DB2054.2
054900     PERFORM DELETE-RECEIVE-TEST-2-SUBTESTS.                      DB2054.2
055000     GO TO END-OF-DB205A.                                         DB2054.2
055100 DELETE-RECEIVE-TEST-2-SUBTESTS.                                  DB2054.2
055200     MOVE "RECEIVE-TEST-2A" TO PAR-NAME.                          DB2054.2
055300     PERFORM DE-LETE.                                             DB2054.2
055400     PERFORM PRINT-DETAIL.                                        DB2054.2
055500     MOVE "RECEIVE-TEST-2B" TO PAR-NAME.                          DB2054.2
055600     PERFORM DE-LETE.                                             DB2054.2
055700     PERFORM PRINT-DETAIL.                                        DB2054.2
055800     MOVE "RECEIVE-TEST-2C" TO PAR-NAME.                          DB2054.2
055900     PERFORM DE-LETE.                                             DB2054.2
056000     PERFORM PRINT-DETAIL.                                        DB2054.2
056100 RECEIVE-TEST-2A.                                                 DB2054.2
056200     MOVE "RECEIVE-TEST-2A" TO PAR-NAME.                          DB2054.2
056300     MOVE "DEBUG-LINE; SEE NEXT LINE" TO RE-MARK.                 DB2054.2
056400     MOVE "<===  DEBUG-LINE" TO CORRECT-A.                        DB2054.2
056500     MOVE LINE-1 TO COMPUTED-A.                                   DB2054.2
056600     PERFORM INSPT.                                               DB2054.2
056700     PERFORM PRINT-DETAIL.                                        DB2054.2
056800 RECEIVE-TEST-2B.                                                 DB2054.2
056900     MOVE "RECEIVE-TEST-2B" TO PAR-NAME.                          DB2054.2
057000     IF UNQUAL-NAME-1 IS EQUAL TO "CM-INQUE"                      DB2054.2
057100         PERFORM PASS                                             DB2054.2
057200     ELSE  PERFORM FAIL                                           DB2054.2
057300         MOVE "CM-INQUE" TO CORRECT-A                             DB2054.2
057400         MOVE NAME-1 TO COMPUTED-A.                               DB2054.2
057500     MOVE "DEBUG-NAME" TO RE-MARK.                                DB2054.2
057600     PERFORM PRINT-DETAIL.                                        DB2054.2
057700 RECEIVE-TEST-2C.                                                 DB2054.2
057800     MOVE "RECEIVE-TEST-2C" TO PAR-NAME.                          DB2054.2
057900     MOVE "DEBUG-CONTENTS" TO RE-MARK.                            DB2054.2
058000     IF CONTENTS-1 IS EQUAL TO INQUE-SPECS                        DB2054.2
058100         PERFORM PASS                                             DB2054.2
058200         PERFORM PRINT-DETAIL                                     DB2054.2
058300         GO TO END-OF-DB205A                                      DB2054.2
058400     ELSE  PERFORM FAIL                                           DB2054.2
058500         MOVE "1ST LINE FOLLOWING" TO CORRECT-A                   DB2054.2
058600         MOVE "2ND LINE FOLLOWING" TO COMPUTED-A.                 DB2054.2
058700         PERFORM PRINT-DETAIL.                                    DB2054.2
058800     MOVE INQUE-SPECS TO PRINT-REC.                               DB2054.2
058900     PERFORM WRITE-LINE.                                          DB2054.2
059000     MOVE CONTENTS-1 TO PRINT-REC.                                DB2054.2
059100     PERFORM WRITE-LINE.                                          DB2054.2
059200 END-OF-DB205A.                                                   DB2054.2
059300     EXIT.                                                        DB2054.2
059400 CCVS-EXIT SECTION.                                               DB2054.2
059500 CCVS-999999.                                                     DB2054.2
059600     GO TO CLOSE-FILES.                                           DB2054.2
      *END-OF,DB205A                                                                  
