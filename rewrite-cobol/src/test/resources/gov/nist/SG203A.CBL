      *HEADER,COBOL,SG203A                                                            
000100 IDENTIFICATION DIVISION.                                         SG2034.2
000200 PROGRAM-ID.                                                      SG2034.2
000300     SG203A.                                                      SG2034.2
000400 AUTHOR.                                                          SG2034.2
000500     FEDERAL COMPILER TESTING CENTER.                             SG2034.2
000600 INSTALLATION.                                                    SG2034.2
000700     GENERAL SERVICES ADMINISTRATION                              SG2034.2
000800*     AUTOMATED DATA AND TELECOMMUNICATION SERVICE.                SG2034.2
000900     SOFTWARE DEVELOPMENT OFFICE.                                 SG2034.2
001000     5203 LEESBURG PIKE  SUITE 1100                               SG2034.2
001100     FALLS CHURCH VIRGINIA 22041.                                 SG2034.2
001200                                                                  SG2034.2
001300     PHONE   (703) 756-6153                                       SG2034.2
001400                                                                  SG2034.2
001500     " HIGH       ".                                              SG2034.2
001600 DATE-WRITTEN.                                                    SG2034.2
001700     CCVS-74 VERSION 4.0 - 1980 JULY 1.                           SG2034.2
001800     CREATION DATE     /    VALIDATION DATE                       SG2034.2
001900     "4.2 ".                                                      SG2034.2
002000 SECURITY.                                                        SG2034.2
002100     NONE.                                                        SG2034.2
002200       THE FOLLOWING FEATURES ARE TESTED BY THIS PROGRAM ---      SG2034.2
002300         VARIOUS OPTIONS OF THE PERFORM AND ALTER STATEMENTS      SG2034.2
002400         ARE USED IN CONJUNCTION WITH THE SEGMENT-LIMIT CLAUSE    SG2034.2
002500         CHECKING INITIAL AND LAST-USED STATES.                   SG2034.2
002600*                                                                 SG2034.2
002700 ENVIRONMENT DIVISION.                                            SG2034.2
002800 CONFIGURATION SECTION.                                           SG2034.2
002900 SOURCE-COMPUTER.                                                 SG2034.2
003000     XXXXX082.                                                    SG2034.2
003100 OBJECT-COMPUTER.                                                 SG2034.2
003200     XXXXX083                                                     SG2034.2
003300     SEGMENT-LIMIT IS 30.                                         SG2034.2
003400 INPUT-OUTPUT SECTION.                                            SG2034.2
003500 FILE-CONTROL.                                                    SG2034.2
003600     SELECT PRINT-FILE ASSIGN TO                                  SG2034.2
003700     XXXXX055.                                                    SG2034.2
003800 DATA DIVISION.                                                   SG2034.2
003900 FILE SECTION.                                                    SG2034.2
004000 FD  PRINT-FILE                                                   SG2034.2
004100     LABEL RECORDS                                                SG2034.2
004200     XXXXX084                                                     SG2034.2
004300     DATA RECORD IS PRINT-REC DUMMY-RECORD.                       SG2034.2
004400 01  PRINT-REC PICTURE X(120).                                    SG2034.2
004500 01  DUMMY-RECORD PICTURE X(120).                                 SG2034.2
004600 WORKING-STORAGE SECTION.                                         SG2034.2
004700 77  TEST-COUNTER       PICTURE 99  VALUE ZERO.                   SG2034.2
004800 77  TEST-CHECK                    PICTURE XXXX VALUE SPACE.      SG2034.2
004900 01  TEST-RESULTS.                                                SG2034.2
005000     02 FILLER                    PICTURE X VALUE SPACE.          SG2034.2
005100     02 FEATURE                   PICTURE X(20) VALUE SPACE.      SG2034.2
005200     02 FILLER                    PICTURE X VALUE SPACE.          SG2034.2
005300     02 P-OR-F                    PICTURE X(5) VALUE SPACE.       SG2034.2
005400     02 FILLER                    PICTURE X  VALUE SPACE.         SG2034.2
005500     02  PAR-NAME.                                                SG2034.2
005600       03 FILLER PICTURE X(12) VALUE SPACE.                       SG2034.2
005700       03  PARDOT-X PICTURE X  VALUE SPACE.                       SG2034.2
005800       03 DOTVALUE PICTURE 99  VALUE ZERO.                        SG2034.2
005900       03 FILLER PIC X(5) VALUE SPACE.                            SG2034.2
006000     02 FILLER PIC X(10) VALUE SPACE.                             SG2034.2
006100     02 RE-MARK PIC X(61).                                        SG2034.2
006200 01  TEST-COMPUTED.                                               SG2034.2
006300     02 FILLER PIC X(30) VALUE SPACE.                             SG2034.2
006400     02 FILLER PIC X(17) VALUE "       COMPUTED=".                SG2034.2
006500     02 COMPUTED-X.                                               SG2034.2
006600     03 COMPUTED-A                PICTURE X(20) VALUE SPACE.      SG2034.2
006700     03 COMPUTED-N REDEFINES COMPUTED-A PICTURE -9(9).9(9).       SG2034.2
006800     03 COMPUTED-0V18 REDEFINES COMPUTED-A  PICTURE -.9(18).      SG2034.2
006900     03 COMPUTED-4V14 REDEFINES COMPUTED-A  PICTURE -9(4).9(14).  SG2034.2
007000     03 COMPUTED-14V4 REDEFINES COMPUTED-A  PICTURE -9(14).9(4).  SG2034.2
007100     03       CM-18V0 REDEFINES COMPUTED-A.                       SG2034.2
007200         04 COMPUTED-18V0                   PICTURE -9(18).       SG2034.2
007300         04 FILLER                          PICTURE X.            SG2034.2
007400     03 FILLER PIC X(50) VALUE SPACE.                             SG2034.2
007500 01  TEST-CORRECT.                                                SG2034.2
007600     02 FILLER PIC X(30) VALUE SPACE.                             SG2034.2
007700     02 FILLER PIC X(17) VALUE "       CORRECT =".                SG2034.2
007800     02 CORRECT-X.                                                SG2034.2
007900     03 CORRECT-A                 PICTURE X(20) VALUE SPACE.      SG2034.2
008000     03 CORRECT-N REDEFINES CORRECT-A PICTURE -9(9).9(9).         SG2034.2
008100     03 CORRECT-0V18 REDEFINES CORRECT-A    PICTURE -.9(18).      SG2034.2
008200     03 CORRECT-4V14 REDEFINES CORRECT-A    PICTURE -9(4).9(14).  SG2034.2
008300     03 CORRECT-14V4 REDEFINES CORRECT-A    PICTURE -9(14).9(4).  SG2034.2
008400     03      CR-18V0 REDEFINES CORRECT-A.                         SG2034.2
008500         04 CORRECT-18V0                    PICTURE -9(18).       SG2034.2
008600         04 FILLER                          PICTURE X.            SG2034.2
008700     03 FILLER PIC X(50) VALUE SPACE.                             SG2034.2
008800 01  CCVS-C-1.                                                    SG2034.2
008900     02 FILLER PICTURE IS X(99) VALUE IS " FEATURE              PASG2034.2
009000-    "SS  PARAGRAPH-NAME                                          SG2034.2
009100-    "        REMARKS".                                           SG2034.2
009200     02 FILLER PICTURE IS X(20) VALUE IS SPACE.                   SG2034.2
009300 01  CCVS-C-2.                                                    SG2034.2
009400     02 FILLER PICTURE IS X VALUE IS SPACE.                       SG2034.2
009500     02 FILLER PICTURE IS X(6) VALUE IS "TESTED".                 SG2034.2
009600     02 FILLER PICTURE IS X(15) VALUE IS SPACE.                   SG2034.2
009700     02 FILLER PICTURE IS X(4) VALUE IS "FAIL".                   SG2034.2
009800     02 FILLER PICTURE IS X(94) VALUE IS SPACE.                   SG2034.2
009900 01  REC-SKL-SUB PICTURE 9(2) VALUE ZERO.                         SG2034.2
010000 01  REC-CT PICTURE 99 VALUE ZERO.                                SG2034.2
010100 01  DELETE-CNT                   PICTURE 999  VALUE ZERO.        SG2034.2
010200 01  ERROR-COUNTER PICTURE IS 999 VALUE IS ZERO.                  SG2034.2
010300 01  INSPECT-COUNTER PIC 999 VALUE ZERO.                          SG2034.2
010400 01  PASS-COUNTER PIC 999 VALUE ZERO.                             SG2034.2
010500 01  TOTAL-ERROR PIC 999 VALUE ZERO.                              SG2034.2
010600 01  ERROR-HOLD PIC 999 VALUE ZERO.                               SG2034.2
010700 01  DUMMY-HOLD PIC X(120) VALUE SPACE.                           SG2034.2
010800 01  RECORD-COUNT PIC 9(5) VALUE ZERO.                            SG2034.2
010900 01  CCVS-H-1.                                                    SG2034.2
011000     02  FILLER   PICTURE X(27)  VALUE SPACE.                     SG2034.2
011100     02 FILLER PICTURE X(67) VALUE                                SG2034.2
011200     " FEDERAL COMPILER TESTING CENTER COBOL COMPILER VALIDATION  SG2034.2
011300-    " SYSTEM".                                                   SG2034.2
011400     02  FILLER     PICTURE X(26)  VALUE SPACE.                   SG2034.2
011500 01  CCVS-H-2.                                                    SG2034.2
011600     02 FILLER PICTURE X(52) VALUE IS                             SG2034.2
011700     "CCVS74 NCC  COPY, NOT FOR DISTRIBUTION.".                   SG2034.2
011800     02 FILLER PICTURE IS X(19) VALUE IS "TEST RESULTS SET-  ".   SG2034.2
011900     02 TEST-ID PICTURE IS X(9).                                  SG2034.2
012000     02 FILLER PICTURE IS X(40) VALUE IS SPACE.                   SG2034.2
012100 01  CCVS-H-3.                                                    SG2034.2
012200     02  FILLER PICTURE X(34) VALUE                               SG2034.2
012300     " FOR OFFICIAL USE ONLY    ".                                SG2034.2
012400     02  FILLER PICTURE X(58) VALUE                               SG2034.2
012500     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SG2034.2
012600     02  FILLER PICTURE X(28) VALUE                               SG2034.2
012700     "  COPYRIGHT   1974 ".                                       SG2034.2
012800 01  CCVS-E-1.                                                    SG2034.2
012900     02 FILLER PICTURE IS X(52) VALUE IS SPACE.                   SG2034.2
013000     02 FILLER PICTURE IS X(14) VALUE IS "END OF TEST-  ".        SG2034.2
013100     02 ID-AGAIN PICTURE IS X(9).                                 SG2034.2
013200     02 FILLER PICTURE X(45) VALUE IS                             SG2034.2
013300     " NTIS DISTRIBUTION COBOL 74".                               SG2034.2
013400 01  CCVS-E-2.                                                    SG2034.2
013500     02  FILLER                   PICTURE X(31)  VALUE            SG2034.2
013600     SPACE.                                                       SG2034.2
013700     02  FILLER                   PICTURE X(21)  VALUE SPACE.     SG2034.2
013800     02 CCVS-E-2-2.                                               SG2034.2
013900         03 ERROR-TOTAL PICTURE IS XXX VALUE IS SPACE.            SG2034.2
014000         03 FILLER PICTURE IS X VALUE IS SPACE.                   SG2034.2
014100         03 ENDER-DESC PIC X(44) VALUE "ERRORS ENCOUNTERED".      SG2034.2
014200 01  CCVS-E-3.                                                    SG2034.2
014300     02  FILLER PICTURE X(22) VALUE                               SG2034.2
014400     " FOR OFFICIAL USE ONLY".                                    SG2034.2
014500     02  FILLER PICTURE X(12) VALUE SPACE.                        SG2034.2
014600     02  FILLER PICTURE X(58) VALUE                               SG2034.2
014700     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SG2034.2
014800     02  FILLER PICTURE X(13) VALUE SPACE.                        SG2034.2
014900     02 FILLER PIC X(15) VALUE " COPYRIGHT 1974".                 SG2034.2
015000 01  CCVS-E-4.                                                    SG2034.2
015100     02 CCVS-E-4-1 PIC XXX VALUE SPACE.                           SG2034.2
015200     02 FILLER PIC XXXX VALUE " OF ".                             SG2034.2
015300     02 CCVS-E-4-2 PIC XXX VALUE SPACE.                           SG2034.2
015400     02 FILLER PIC X(40) VALUE                                    SG2034.2
015500      "  TESTS WERE EXECUTED SUCCESSFULLY".                       SG2034.2
015600 01  XXINFO.                                                      SG2034.2
015700     02 FILLER PIC X(30) VALUE "        *** INFORMATION  ***".    SG2034.2
015800     02 INFO-TEXT.                                                SG2034.2
015900     04 FILLER PIC X(20) VALUE SPACE.                             SG2034.2
016000     04 XXCOMPUTED PIC X(20).                                     SG2034.2
016100     04 FILLER PIC X(5) VALUE SPACE.                              SG2034.2
016200     04 XXCORRECT PIC X(20).                                      SG2034.2
016300 01  HYPHEN-LINE.                                                 SG2034.2
016400     02 FILLER PICTURE IS X VALUE IS SPACE.                       SG2034.2
016500     02 FILLER PICTURE IS X(65) VALUE IS "************************SG2034.2
016600-    "*****************************************".                 SG2034.2
016700     02 FILLER PICTURE IS X(54) VALUE IS "************************SG2034.2
016800-    "******************************".                            SG2034.2
016900 01  CCVS-PGM-ID PIC X(6) VALUE                                   SG2034.2
017000     "SG203A".                                                    SG2034.2
017100 PROCEDURE DIVISION.                                              SG2034.2
017200 SECT-SG-03-001 SECTION 49.                                       SG2034.2
017300 INIT-SG203.                                                      SG2034.2
017400     PERFORM  CCVS1.                                              SG2034.2
017500     GO TO 50.                                                    SG2034.2
017600 CCVS1 SECTION.                                                   SG2034.2
017700 OPEN-FILES.                                                      SG2034.2
017800     OPEN     OUTPUT PRINT-FILE.                                  SG2034.2
017900     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   SG2034.2
018000     MOVE    SPACE TO TEST-RESULTS.                               SG2034.2
018100     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             SG2034.2
018200     GO TO CCVS1-EXIT.                                            SG2034.2
018300 CLOSE-FILES.                                                     SG2034.2
018400     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   SG2034.2
018500 TERMINATE-CCVS.                                                  SG2034.2
018600*    EXIT PROGRAM.                                                SG2034.2
018700*TERMINATE-CALL.                                                  SG2034.2
018800     STOP     RUN.                                                SG2034.2
018900 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         SG2034.2
019000 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           SG2034.2
019100 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          SG2034.2
019200 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-CNT.          SG2034.2
019300     MOVE "****TEST DELETED****" TO RE-MARK.                      SG2034.2
019400 PRINT-DETAIL.                                                    SG2034.2
019500     IF REC-CT NOT EQUAL TO ZERO                                  SG2034.2
019600             MOVE "." TO PARDOT-X                                 SG2034.2
019700             MOVE REC-CT TO DOTVALUE.                             SG2034.2
019800     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      SG2034.2
019900     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               SG2034.2
020000        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 SG2034.2
020100          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 SG2034.2
020200     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              SG2034.2
020300     MOVE SPACE TO CORRECT-X.                                     SG2034.2
020400     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         SG2034.2
020500     MOVE     SPACE TO RE-MARK.                                   SG2034.2
020600 HEAD-ROUTINE.                                                    SG2034.2
020700     MOVE CCVS-H-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SG2034.2
020800     MOVE CCVS-H-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.   SG2034.2
020900     MOVE CCVS-H-3 TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.   SG2034.2
021000 COLUMN-NAMES-ROUTINE.                                            SG2034.2
021100     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SG2034.2
021200     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SG2034.2
021300     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        SG2034.2
021400 END-ROUTINE.                                                     SG2034.2
021500     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.SG2034.2
021600 END-RTN-EXIT.                                                    SG2034.2
021700     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SG2034.2
021800 END-ROUTINE-1.                                                   SG2034.2
021900      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      SG2034.2
022000      ERROR-HOLD. ADD DELETE-CNT TO ERROR-HOLD.                   SG2034.2
022100      ADD PASS-COUNTER TO ERROR-HOLD.                             SG2034.2
022200*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   SG2034.2
022300      MOVE PASS-COUNTER TO CCVS-E-4-1.                            SG2034.2
022400      MOVE ERROR-HOLD TO CCVS-E-4-2.                              SG2034.2
022500      MOVE CCVS-E-4 TO CCVS-E-2-2.                                SG2034.2
022600      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           SG2034.2
022700  END-ROUTINE-12.                                                 SG2034.2
022800      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        SG2034.2
022900     IF       ERROR-COUNTER IS EQUAL TO ZERO                      SG2034.2
023000         MOVE "NO " TO ERROR-TOTAL                                SG2034.2
023100         ELSE                                                     SG2034.2
023200         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       SG2034.2
023300     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           SG2034.2
023400     PERFORM WRITE-LINE.                                          SG2034.2
023500 END-ROUTINE-13.                                                  SG2034.2
023600     IF DELETE-CNT IS EQUAL TO ZERO                               SG2034.2
023700         MOVE "NO " TO ERROR-TOTAL  ELSE                          SG2034.2
023800         MOVE DELETE-CNT TO ERROR-TOTAL.                          SG2034.2
023900     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   SG2034.2
024000     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SG2034.2
024100      IF   INSPECT-COUNTER EQUAL TO ZERO                          SG2034.2
024200          MOVE "NO " TO ERROR-TOTAL                               SG2034.2
024300      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   SG2034.2
024400      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            SG2034.2
024500      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          SG2034.2
024600     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SG2034.2
024700 WRITE-LINE.                                                      SG2034.2
024800     ADD 1 TO RECORD-COUNT.                                       SG2034.2
024900*    IF RECORD-COUNT GREATER 50                                   SG2034.2
025000*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          SG2034.2
025100*        MOVE SPACE TO DUMMY-RECORD                               SG2034.2
025200*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  SG2034.2
025300*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             SG2034.2
025400*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     SG2034.2
025500*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          SG2034.2
025600*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          SG2034.2
025700*        MOVE ZERO TO RECORD-COUNT.                               SG2034.2
025800     PERFORM WRT-LN.                                              SG2034.2
025900 WRT-LN.                                                          SG2034.2
026000     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               SG2034.2
026100     MOVE SPACE TO DUMMY-RECORD.                                  SG2034.2
026200 BLANK-LINE-PRINT.                                                SG2034.2
026300     PERFORM WRT-LN.                                              SG2034.2
026400 FAIL-ROUTINE.                                                    SG2034.2
026500     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.   SG2034.2
026600     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.    SG2034.2
026700     MOVE "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.    SG2034.2
026800     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     SG2034.2
026900     GO TO FAIL-ROUTINE-EX.                                       SG2034.2
027000 FAIL-ROUTINE-WRITE.                                              SG2034.2
027100     MOVE TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE           SG2034.2
027200     MOVE TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES.   SG2034.2
027300 FAIL-ROUTINE-EX. EXIT.                                           SG2034.2
027400 BAIL-OUT.                                                        SG2034.2
027500     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.       SG2034.2
027600     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.               SG2034.2
027700 BAIL-OUT-WRITE.                                                  SG2034.2
027800     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  SG2034.2
027900     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     SG2034.2
028000 BAIL-OUT-EX. EXIT.                                               SG2034.2
028100 CCVS1-EXIT.                                                      SG2034.2
028200     EXIT.                                                        SG2034.2
028300 50  SECTION 50.                                                  SG2034.2
028400 PARA-50.                                                         SG2034.2
028500     MOVE SPACE TO TEST-CHECK.                                    SG2034.2
028600 29 SECTION 29.                                                   SG2034.2
028700 PARA-29.                                                         SG2034.2
028800     MOVE "GOOD" TO TEST-CHECK.                                   SG2034.2
028900*    NOTE       *******TEST 1 BEGINS HERE**********.              SG2034.2
029000 67  SECTION 67.                                                  SG2034.2
029100 PARA-67.                                                         SG2034.2
029200     IF TEST-CHECK EQUAL TO "GOOD"                                SG2034.2
029300         PERFORM PASS                                             SG2034.2
029400         GO TO WRITE-67.                                          SG2034.2
029500     MOVE "GOOD" TO CORRECT-A.                                    SG2034.2
029600     MOVE TEST-CHECK TO COMPUTED-A.                               SG2034.2
029700     PERFORM FAIL.                                                SG2034.2
029800     GO TO WRITE-67.                                              SG2034.2
029900 DELETE-67.                                                       SG2034.2
030000     PERFORM DE-LETE.                                             SG2034.2
030100 WRITE-67.                                                        SG2034.2
030200     MOVE "SEGM-TEST-01" TO PAR-NAME.                             SG2034.2
030300     MOVE "TEST BEGINS IN PARA-67" TO RE-MARK.                    SG2034.2
030400     MOVE "FALL THRU IND SEG" TO FEATURE.                         SG2034.2
030500     PERFORM PRINT-DETAIL.                                        SG2034.2
030600*    NOTE       *******TEST 2 BEGINS HERE**********.              SG2034.2
030700 30 SECTION 30.                                                   SG2034.2
030800 PARA-30.                                                         SG2034.2
030900     MOVE SPACE TO TEST-CHECK.                                    SG2034.2
031000 51  SECTION 51.                                                  SG2034.2
031100 PARA-51.                                                         SG2034.2
031200     MOVE "BAD " TO TEST-CHECK.                                   SG2034.2
031300 52  SECTION 52.                                                  SG2034.2
031400 PARA-52.                                                         SG2034.2
031500     MOVE "GOOD" TO TEST-CHECK.                                   SG2034.2
031600 31 SECTION 31.                                                   SG2034.2
031700 PARA-31.                                                         SG2034.2
031800     IF TEST-CHECK EQUAL TO "GOOD"                                SG2034.2
031900         PERFORM PASS                                             SG2034.2
032000         GO TO WRITE-31.                                          SG2034.2
032100     MOVE TEST-CHECK TO COMPUTED-A.                               SG2034.2
032200     MOVE "GOOD" TO CORRECT-A.                                    SG2034.2
032300     PERFORM FAIL.                                                SG2034.2
032400     GO TO WRITE-31.                                              SG2034.2
032500 DELETE-31.                                                       SG2034.2
032600     PERFORM DE-LETE.                                             SG2034.2
032700 WRITE-31.                                                        SG2034.2
032800     MOVE "SEGM-TEST-02" TO PAR-NAME.                             SG2034.2
032900     MOVE "TEST BEGINS IN PARA-31" TO RE-MARK.                    SG2034.2
033000     PERFORM PRINT-DETAIL.                                        SG2034.2
033100*    NOTE       *******TEST 3 BEGINS HERE**********.              SG2034.2
033200 53  SECTION 53.                                                  SG2034.2
033300 PARA-53.                                                         SG2034.2
033400     GO TO PARA-54.                                               SG2034.2
033500 99  SECTION 99.                                                  SG2034.2
033600 PARA-99.                                                         SG2034.2
033700     PERFORM PARA-32 THROUGH PARA-33 8 TIMES.                     SG2034.2
033800     IF TEST-COUNTER EQUAL TO 8                                   SG2034.2
033900         PERFORM PASS                                             SG2034.2
034000         GO TO WRITE-99.                                          SG2034.2
034100     MOVE TEST-COUNTER TO COMPUTED-N.                             SG2034.2
034200     MOVE 8 TO CORRECT-N.                                         SG2034.2
034300     PERFORM FAIL.                                                SG2034.2
034400     GO TO WRITE-99.                                              SG2034.2
034500 DELETE-99.                                                       SG2034.2
034600     PERFORM DE-LETE.                                             SG2034.2
034700 WRITE-99.                                                        SG2034.2
034800     MOVE "SEGM-TEST-03" TO PAR-NAME.                             SG2034.2
034900     MOVE "TEST BEGINS IN PARA-99" TO RE-MARK.                    SG2034.2
035000     MOVE "PERFORM IND SEG " TO FEATURE.                          SG2034.2
035100     PERFORM PRINT-DETAIL.                                        SG2034.2
035200*    NOTE       *******TEST 4 BEGINS HERE**********.              SG2034.2
035300     ALTER PARA-32 TO PARA-32A.                                   SG2034.2
035400     GO TO PARA-34.                                               SG2034.2
035500 54  SECTION 54.                                                  SG2034.2
035600 PARA-54.                                                         SG2034.2
035700     GO TO PARA-54A.                                              SG2034.2
035800 PARA-54A.                                                        SG2034.2
035900     ALTER PARA-54 TO PROCEED TO PARA-54B.                        SG2034.2
036000     GO TO PARA-54.                                               SG2034.2
036100 PARA-54B.                                                        SG2034.2
036200     ALTER PARA-54 TO PROCEED TO PARA-54A.                        SG2034.2
036300     GO TO PARA-99.                                               SG2034.2
036400 32 SECTION 32.                                                   SG2034.2
036500 PARA-32.                                                         SG2034.2
036600     GO TO PARA-32A.                                              SG2034.2
036700 PARA-32A.                                                        SG2034.2
036800     ALTER PARA-32 TO PROCEED TO PARA-32C.                        SG2034.2
036900 PARA-32B.                                                        SG2034.2
037000     MOVE 16 TO TEST-COUNTER.                                     SG2034.2
037100     GO TO PARA-32.                                               SG2034.2
037200 PARA-32C.                                                        SG2034.2
037300     SUBTRACT 1 FROM TEST-COUNTER.                                SG2034.2
037400 33 SECTION 33.                                                   SG2034.2
037500 PARA-33.                                                         SG2034.2
037600     EXIT.                                                        SG2034.2
037700 34 SECTION 34.                                                   SG2034.2
037800 PARA-34.                                                         SG2034.2
037900     GO TO PARA-34A.                                              SG2034.2
038000 PARA-34A.                                                        SG2034.2
038100     ALTER PARA-34 TO PROCEED TO PARA-55.                         SG2034.2
038200     GO TO PARA-32.                                               SG2034.2
038300 55  SECTION 55.                                                  SG2034.2
038400 PARA-55.                                                         SG2034.2
038500     IF TEST-COUNTER EQUAL TO 15                                  SG2034.2
038600         PERFORM PASS                                             SG2034.2
038700         GO TO WRITE-55.                                          SG2034.2
038800     MOVE TEST-COUNTER TO COMPUTED-N.                             SG2034.2
038900     MOVE 15 TO CORRECT-N.                                        SG2034.2
039000     PERFORM FAIL.                                                SG2034.2
039100     GO TO WRITE-55.                                              SG2034.2
039200 DELETE-55.                                                       SG2034.2
039300     PERFORM DE-LETE.                                             SG2034.2
039400 WRITE-55.                                                        SG2034.2
039500     MOVE "SEGM-TEST-04" TO PAR-NAME.                             SG2034.2
039600     MOVE "TEST BEGINS IN PARA-55" TO RE-MARK.                    SG2034.2
039700     MOVE "ALTER OVLY FIXED SEG" TO FEATURE.                      SG2034.2
039800     PERFORM PRINT-DETAIL.                                        SG2034.2
039900*    NOTE       *******TEST 5 BEGINS HERE**********.              SG2034.2
040000 56  SECTION 56.                                                  SG2034.2
040100 PARA-56.                                                         SG2034.2
040200     ALTER PARA-34 TO PROCEED TO PARA-56A.                        SG2034.2
040300     MOVE 5 TO TEST-COUNTER.                                      SG2034.2
040400     GO TO PARA-32C.                                              SG2034.2
040500 PARA-56A.                                                        SG2034.2
040600     IF TEST-COUNTER EQUAL TO 4                                   SG2034.2
040700         PERFORM PASS                                             SG2034.2
040800         GO TO WRITE-56.                                          SG2034.2
040900     MOVE TEST-COUNTER TO COMPUTED-N.                             SG2034.2
041000     MOVE 4 TO CORRECT-N.                                         SG2034.2
041100     PERFORM FAIL.                                                SG2034.2
041200     GO TO WRITE-56.                                              SG2034.2
041300 DELETE-56.                                                       SG2034.2
041400     PERFORM DE-LETE.                                             SG2034.2
041500 WRITE-56.                                                        SG2034.2
041600     MOVE "SEGM-TEST-05" TO PAR-NAME.                             SG2034.2
041700     MOVE "TEST BEGINS IN PARA-56" TO RE-MARK.                    SG2034.2
041800     PERFORM PRINT-DETAIL.                                        SG2034.2
041900*    NOTE       *******TEST 6 BEGINS HERE**********.              SG2034.2
042000 57  SECTION 57.                                                  SG2034.2
042100 PARA-57.                                                         SG2034.2
042200     MOVE "GOOD" TO TEST-CHECK.                                   SG2034.2
042300     ALTER PARA-00 TO PROCEED TO PARA-00B.                        SG2034.2
042400     GO TO PARA-00.                                               SG2034.2
042500 PARA-00.                                                         SG2034.2
042600     GO TO PARA-00A.                                              SG2034.2
042700 PARA-00A.                                                        SG2034.2
042800     MOVE "BAD " TO TEST-CHECK.                                   SG2034.2
042900 PARA-00B.                                                        SG2034.2
043000*    NOTE  THIS PARAGRAPH SERVES NO PURPOSE OTHER THAN TO         SG2034.2
043100*        VERIFY THAT FALLING THRU WILL NOT DISTURB PROGRAM FLOW.  SG2034.2
043200 59  SECTION 59.                                                  SG2034.2
043300 PARA-59.                                                         SG2034.2
043400*    THIS SECTION SERVES NO PURPOSE EXCEPT TO VERIFY THAT         SG2034.2
043500*        PROGRAM FLOW WILL NOT BE AFFECTED AND THE FOLLOWING      SG2034.2
043600*            STATEMENT WILL BE IGNORED.                           SG2034.2
043700     IF       TEST-CHECK EQUAL TO "BAD "                          SG2034.2
043800               MOVE "BAD " TO TEST-CHECK.                         SG2034.2
043900 01 SECTION 01.                                                   SG2034.2
044000 PARA-01.                                                         SG2034.2
044100     IF TEST-CHECK EQUAL TO "GOOD"                                SG2034.2
044200         PERFORM PASS                                             SG2034.2
044300         GO TO WRITE-01.                                          SG2034.2
044400     MOVE "GOOD" TO CORRECT-A.                                    SG2034.2
044500     MOVE TEST-CHECK TO COMPUTED-A.                               SG2034.2
044600     PERFORM FAIL.                                                SG2034.2
044700     GO TO WRITE-01.                                              SG2034.2
044800 DELETE-01.                                                       SG2034.2
044900     PERFORM DE-LETE.                                             SG2034.2
045000 WRITE-01.                                                        SG2034.2
045100     MOVE "SEGM-TEST-06" TO PAR-NAME.                             SG2034.2
045200     MOVE "TEST BEGINS IN PARA-01" TO RE-MARK.                    SG2034.2
045300     MOVE "FALL THRU IND SEG" TO FEATURE.                         SG2034.2
045400     PERFORM PRINT-DETAIL.                                        SG2034.2
045500*    NOTE       *******TEST 7 BEGINS HERE**********.              SG2034.2
045600 02 SECTION 02.                                                   SG2034.2
045700 PARA-02.                                                         SG2034.2
045800     MOVE SPACE TO TEST-CHECK.                                    SG2034.2
045900     PERFORM 59.                                                  SG2034.2
046000     IF TEST-CHECK EQUAL TO SPACE                                 SG2034.2
046100         PERFORM PASS                                             SG2034.2
046200         GO TO WRITE-02.                                          SG2034.2
046300     MOVE TEST-CHECK TO COMPUTED-A.                               SG2034.2
046400     MOVE SPACE TO CORRECT-A.                                     SG2034.2
046500     PERFORM FAIL.                                                SG2034.2
046600     GO TO WRITE-02.                                              SG2034.2
046700 DELETE-02.                                                       SG2034.2
046800     PERFORM DE-LETE.                                             SG2034.2
046900 WRITE-02.                                                        SG2034.2
047000     MOVE "SEGM-TEST-07" TO PAR-NAME.                             SG2034.2
047100     MOVE "TEST BEGINS IN PARA-02" TO RE-MARK.                    SG2034.2
047200     MOVE "PERFORM  IND SEG" TO FEATURE.                          SG2034.2
047300     PERFORM PRINT-DETAIL.                                        SG2034.2
047400*    NOTE       *******TEST 8 BEGINS HERE**********.              SG2034.2
047500 35 SECTION 35.                                                   SG2034.2
047600 PARA-35.                                                         SG2034.2
047700     ALTER PARA-34 TO PROCEED TO PARA-35A.                        SG2034.2
047800     MOVE 1 TO TEST-COUNTER.                                      SG2034.2
047900     GO TO PARA-32.                                               SG2034.2
048000 PARA-35A.                                                        SG2034.2
048100     IF TEST-COUNTER EQUAL TO ZERO                                SG2034.2
048200         PERFORM PASS                                             SG2034.2
048300         GO TO WRITE-35.                                          SG2034.2
048400     MOVE TEST-COUNTER TO COMPUTED-N.                             SG2034.2
048500     MOVE 0 TO CORRECT-N.                                         SG2034.2
048600     PERFORM FAIL.                                                SG2034.2
048700     GO TO WRITE-35.                                              SG2034.2
048800 DELETE-35.                                                       SG2034.2
048900     PERFORM DE-LETE.                                             SG2034.2
049000 WRITE-35.                                                        SG2034.2
049100     MOVE "SEGM-TEST-08" TO PAR-NAME.                             SG2034.2
049200     MOVE "TEST BEGINS IN PARA-35" TO RE-MARK.                    SG2034.2
049300     MOVE "ALTER OVLY FIXED SEG" TO FEATURE.                      SG2034.2
049400     PERFORM PRINT-DETAIL.                                        SG2034.2
049500*    NOTE       *******TEST 9 BEGINS HERE**********.              SG2034.2
049600 36 SECTION 36.                                                   SG2034.2
049700 PARA-36.                                                         SG2034.2
049800     GO TO PARA-36A.                                              SG2034.2
049900 PARA-36A.                                                        SG2034.2
050000     ALTER PARA-36 TO PROCEED TO PARA-36B.                        SG2034.2
050100     MOVE SPACE TO TEST-CHECK.                                    SG2034.2
050200     GO TO 85.                                                    SG2034.2
050300 PARA-36B.                                                        SG2034.2
050400     IF TEST-CHECK EQUAL TO "GOOD"                                SG2034.2
050500         PERFORM PASS                                             SG2034.2
050600         GO TO WRITE-36.                                          SG2034.2
050700     MOVE TEST-CHECK TO COMPUTED-A.                               SG2034.2
050800     MOVE "GOOD" TO CORRECT-A.                                    SG2034.2
050900     PERFORM FAIL.                                                SG2034.2
051000     GO TO WRITE-36.                                              SG2034.2
051100 DELETE-36.                                                       SG2034.2
051200     PERFORM DE-LETE.                                             SG2034.2
051300 WRITE-36.                                                        SG2034.2
051400     MOVE "SEGM-TEST-09" TO PAR-NAME.                             SG2034.2
051500     MOVE "TEST BEGINS IN PARA-36" TO RE-MARK.                    SG2034.2
051600     MOVE "GO TO NON-RES SEG" TO FEATURE.                         SG2034.2
051700     PERFORM PRINT-DETAIL.                                        SG2034.2
051800*    NOTE       *******TEST 10 BEGINS HERE*********.              SG2034.2
051900     GO TO PARA-58.                                               SG2034.2
052000 85  SECTION 85.                                                  SG2034.2
052100 PARA-85.                                                         SG2034.2
052200     MOVE "GOOD" TO TEST-CHECK.                                   SG2034.2
052300     GO TO 36.                                                    SG2034.2
052400 98  SECTION 98.                                                  SG2034.2
052500 PARA-98.                                                         SG2034.2
052600     MOVE "GOOD" TO TEST-CHECK                                    SG2034.2
052700     GO TO PARA-37.                                               SG2034.2
052800 58  SECTION 58.                                                  SG2034.2
052900 PARA-58.                                                         SG2034.2
053000     MOVE SPACE TO TEST-CHECK.                                    SG2034.2
053100 PARA-58A.                                                        SG2034.2
053200     GO TO PARA-58B.                                              SG2034.2
053300 PARA-58B.                                                        SG2034.2
053400     MOVE "BAD " TO TEST-CHECK.                                   SG2034.2
053500     GO TO PARA-58D.                                              SG2034.2
053600 PARA-58C.                                                        SG2034.2
053700     MOVE "GOOD" TO TEST-CHECK.                                   SG2034.2
053800     GO TO PARA-58E.                                              SG2034.2
053900 PARA-58D.                                                        SG2034.2
054000     ALTER PARA-58A TO PARA-58C.                                  SG2034.2
054100 PARA-58E.                                                        SG2034.2
054200     EXIT.                                                        SG2034.2
054300 37 SECTION 37.                                                   SG2034.2
054400 PARA-37.                                                         SG2034.2
054500     PERFORM PARA-58A THRU PARA-58E.                              SG2034.2
054600     IF TEST-CHECK EQUAL TO "BAD "                                SG2034.2
054700         PERFORM PASS                                             SG2034.2
054800         GO TO WRITE-37.                                          SG2034.2
054900     MOVE "BAD " TO CORRECT-A.                                    SG2034.2
055000     MOVE TEST-CHECK TO COMPUTED-A.                               SG2034.2
055100     PERFORM FAIL.                                                SG2034.2
055200     GO TO WRITE-37.                                              SG2034.2
055300 DELETE-37.                                                       SG2034.2
055400     PERFORM DE-LETE.                                             SG2034.2
055500 WRITE-37.                                                        SG2034.2
055600     MOVE "SEGM-TEST-10" TO PAR-NAME.                             SG2034.2
055700     MOVE "TEST BEGINS IN PARA-37" TO RE-MARK.                    SG2034.2
055800     MOVE "INITIAL STATE" TO FEATURE.                             SG2034.2
055900     PERFORM PRINT-DETAIL.                                        SG2034.2
056000*    NOTE       *******TEST 11 BEGINS HERE*********.              SG2034.2
056100 38 SECTION 38.                                                   SG2034.2
056200 PARA-38.                                                         SG2034.2
056300     PERFORM PARA-58D.                                            SG2034.2
056400     PERFORM PARA-58A THRU PARA-58E.                              SG2034.2
056500     IF TEST-CHECK EQUAL TO "BAD "                                SG2034.2
056600         PERFORM PASS                                             SG2034.2
056700         GO TO WRITE-38.                                          SG2034.2
056800     MOVE TEST-CHECK TO COMPUTED-A.                               SG2034.2
056900     MOVE "BAD " TO CORRECT-A.                                    SG2034.2
057000     PERFORM FAIL.                                                SG2034.2
057100     GO TO WRITE-38.                                              SG2034.2
057200 DELETE-38.                                                       SG2034.2
057300     PERFORM DE-LETE.                                             SG2034.2
057400 WRITE-38.                                                        SG2034.2
057500     MOVE "SEGM-TEST-11" TO PAR-NAME.                             SG2034.2
057600     MOVE "TEST BEGINS IN PARA-38" TO RE-MARK.                    SG2034.2
057700     PERFORM PRINT-DETAIL.                                        SG2034.2
057800*    NOTE       *******TEST 12 BEGINS HERE*********.              SG2034.2
057900 03 SECTION 03.                                                   SG2034.2
058000 PARA-03.                                                         SG2034.2
058100     MOVE SPACE TO TEST-CHECK.                                    SG2034.2
058200     PERFORM PARA-39D.                                            SG2034.2
058300     PERFORM PARA-39A THROUGH PARA-39E.                           SG2034.2
058400     IF TEST-CHECK EQUAL TO "GOOD"                                SG2034.2
058500         PERFORM PASS                                             SG2034.2
058600         GO TO WRITE-03.                                          SG2034.2
058700     MOVE TEST-CHECK TO COMPUTED-A.                               SG2034.2
058800     MOVE "GOOD" TO CORRECT-A.                                    SG2034.2
058900     PERFORM FAIL.                                                SG2034.2
059000     GO TO WRITE-03.                                              SG2034.2
059100 DELETE-03.                                                       SG2034.2
059200     PERFORM DE-LETE.                                             SG2034.2
059300 WRITE-03.                                                        SG2034.2
059400     MOVE "SEGM-TEST-12" TO PAR-NAME.                             SG2034.2
059500     MOVE "TEST BEGINS IN PARA-03" TO RE-MARK.                    SG2034.2
059600     MOVE "LAST USED STATE" TO FEATURE.                           SG2034.2
059700     PERFORM PRINT-DETAIL.                                        SG2034.2
059800*    NOTE       *******TEST 13 BEGINS HERE*********.              SG2034.2
059900 39 SECTION 39.                                                   SG2034.2
060000 PARA-39A.                                                        SG2034.2
060100     GO TO PARA-39B.                                              SG2034.2
060200 PARA-39B.                                                        SG2034.2
060300     MOVE "BAD " TO TEST-CHECK.                                   SG2034.2
060400     GO TO PARA-39D.                                              SG2034.2
060500 PARA-39C.                                                        SG2034.2
060600     MOVE "GOOD" TO TEST-CHECK.                                   SG2034.2
060700     GO TO PARA-39E.                                              SG2034.2
060800 PARA-39D.                                                        SG2034.2
060900     ALTER PARA-39A TO PARA-39C.                                  SG2034.2
061000 PARA-39E.                                                        SG2034.2
061100     EXIT.                                                        SG2034.2
061200 04 SECTION 04.                                                   SG2034.2
061300 PARA-04.                                                         SG2034.2
061400     IF TEST-CHECK EQUAL TO "GOOD"                                SG2034.2
061500         PERFORM PASS                                             SG2034.2
061600         GO TO WRITE-04.                                          SG2034.2
061700     MOVE "GOOD" TO CORRECT-A.                                    SG2034.2
061800     MOVE TEST-CHECK TO COMPUTED-A.                               SG2034.2
061900     PERFORM FAIL.                                                SG2034.2
062000     GO TO WRITE-04.                                              SG2034.2
062100 DELETE-04.                                                       SG2034.2
062200     PERFORM DE-LETE.                                             SG2034.2
062300 WRITE-04.                                                        SG2034.2
062400     MOVE "SEGM-TEST-13" TO PAR-NAME.                             SG2034.2
062500     MOVE "TEST BEGINS IN PARA-04" TO RE-MARK.                    SG2034.2
062600     PERFORM PRINT-DETAIL.                                        SG2034.2
062700*    NOTE       *******TEST 14 BEGINS HERE*********.              SG2034.2
062800     MOVE SPACE TO TEST-CHECK.                                    SG2034.2
062900     MOVE 0 TO TEST-COUNTER.                                      SG2034.2
063000 40 SECTION 40.                                                   SG2034.2
063100 PARA-40.                                                         SG2034.2
063200     GO TO PARA-68.                                               SG2034.2
063300 PARA-40A.                                                        SG2034.2
063400     GO TO PARA-68.                                               SG2034.2
063500 PARA-40B.                                                        SG2034.2
063600     IF TEST-CHECK EQUAL TO "GOOD"                                SG2034.2
063700         PERFORM PASS                                             SG2034.2
063800         GO TO WRITE-40.                                          SG2034.2
063900     MOVE "GOOD" TO CORRECT-A.                                    SG2034.2
064000     MOVE TEST-CHECK TO COMPUTED-A.                               SG2034.2
064100     PERFORM FAIL.                                                SG2034.2
064200     GO TO WRITE-40.                                              SG2034.2
064300 DELETE-40.                                                       SG2034.2
064400     PERFORM DE-LETE.                                             SG2034.2
064500 WRITE-40.                                                        SG2034.2
064600     MOVE "SEGM-TEST-14" TO PAR-NAME.                             SG2034.2
064700     MOVE "TEST BEGINS IN PARA-40B" TO RE-MARK.                   SG2034.2
064800     MOVE "INITIAL STATE" TO FEATURE.                             SG2034.2
064900     PERFORM PRINT-DETAIL.                                        SG2034.2
065000*    NOTE       *******TEST 15 BEGINS HERE*********.              SG2034.2
065100     MOVE SPACE TO TEST-CHECK.                                    SG2034.2
065200     IF TEST-COUNTER EQUAL TO 2 GO TO PARA-68C.                   SG2034.2
065300*    NOTE ***** THE PREVIOUS IF STMT WAS INSERTED TO KEEP TEST-14 SG2034.2
065400*        FROM LOOPING IF SEGMENT 68 WAS NOT IN THE INITIAL STATE  SG2034.2
065500*        EACH TIME IT WAS ENTERED -- TEST-15 WILL ALSO FAIL       SG2034.2
065600*        IN THIS CASE *****.                                      SG2034.2
065700     MOVE 2 TO TEST-COUNTER.                                      SG2034.2
065800*    NOTE FALL THRU.                                              SG2034.2
065900 68  SECTION 68.                                                  SG2034.2
066000 PARA-68.                                                         SG2034.2
066100     GO TO PARA-68A.                                              SG2034.2
066200 PARA-68A.                                                        SG2034.2
066300     ALTER PARA-68 TO PROCEED TO PARA-68B.                        SG2034.2
066400     MOVE "GOOD" TO TEST-CHECK.                                   SG2034.2
066500     IF TEST-COUNTER EQUAL TO 0                                   SG2034.2
066600         ADD 1 TO TEST-COUNTER                                    SG2034.2
066700         GO TO PARA-40A.                                          SG2034.2
066800     IF TEST-COUNTER EQUAL TO 1  GO TO PARA-40B.                  SG2034.2
066900     GO TO PARA-68C.                                              SG2034.2
067000 PARA-68B.                                                        SG2034.2
067100     MOVE "BAD " TO TEST-CHECK.                                   SG2034.2
067200     ADD 1 TO TEST-COUNTER.                                       SG2034.2
067300     GO TO PARA-40B.                                              SG2034.2
067400 PARA-68C.                                                        SG2034.2
067500     IF TEST-CHECK EQUAL TO "GOOD"                                SG2034.2
067600         PERFORM PASS                                             SG2034.2
067700         GO TO WRITE-68.                                          SG2034.2
067800     MOVE "GOOD" TO CORRECT-A.                                    SG2034.2
067900     MOVE TEST-CHECK TO COMPUTED-A.                               SG2034.2
068000     PERFORM FAIL.                                                SG2034.2
068100     GO TO WRITE-68.                                              SG2034.2
068200 DELETE-68.                                                       SG2034.2
068300     PERFORM DE-LETE.                                             SG2034.2
068400 WRITE-68.                                                        SG2034.2
068500     MOVE "SEGM-TEST-15" TO PAR-NAME.                             SG2034.2
068600     MOVE "TEST BEGINS IN PARA-68C" TO RE-MARK.                   SG2034.2
068700     PERFORM PRINT-DETAIL.                                        SG2034.2
068800 41  SECTION 41.                                                  SG2034.2
068900 PARA-41A.                                                        SG2034.2
069000     MOVE SPACE TO TEST-CHECK.                                    SG2034.2
069100*    NOTE     *****    TEST 16 BEGINS HERE   *****.               SG2034.2
069200     PERFORM TEST-16.                                             SG2034.2
069300     ALTER PARA-41B TO PARA-41D.                                  SG2034.2
069400     PERFORM PARA-41B THRU PARA-41E.                              SG2034.2
069500     PERFORM TEST-16.                                             SG2034.2
069600     IF TEST-CHECK EQUAL TO "END"                                 SG2034.2
069700         PERFORM PASS                                             SG2034.2
069800         GO TO WRITE-41.                                          SG2034.2
069900     MOVE "END " TO CORRECT-A.                                    SG2034.2
070000     MOVE TEST-CHECK TO COMPUTED-A.                               SG2034.2
070100     PERFORM FAIL.                                                SG2034.2
070200     GO TO WRITE-41.                                              SG2034.2
070300 DELETE-41.                                                       SG2034.2
070400     PERFORM DE-LETE.                                             SG2034.2
070500 WRITE-41.                                                        SG2034.2
070600     MOVE "SEGM-TEST-16" TO PAR-NAME.                             SG2034.2
070700     MOVE "TEST BEGINS IN PARA-41A" TO RE-MARK.                   SG2034.2
070800     PERFORM PRINT-DETAIL.                                        SG2034.2
070900     GO TO 45.                                                    SG2034.2
071000 TEST-16 SECTION 41.                                              SG2034.2
071100 PARA-41B.                                                        SG2034.2
071200     GO TO PARA-41C.                                              SG2034.2
071300 PARA-41C.                                                        SG2034.2
071400     MOVE "PFM1" TO TEST-CHECK.                                   SG2034.2
071500     GO TO PARA-41E.                                              SG2034.2
071600 PARA-41D.                                                        SG2034.2
071700     MOVE "ALT1" TO TEST-CHECK.                                   SG2034.2
071800     ALTER PARA-41B TO PARA-41F.                                  SG2034.2
071900     GO TO TEST-16.                                               SG2034.2
072000 PARA-41F.                                                        SG2034.2
072100     MOVE "END " TO TEST-CHECK.                                   SG2034.2
072200     GO TO PARA-41E.                                              SG2034.2
072300 PARA-41E.                                                        SG2034.2
072400     EXIT.                                                        SG2034.2
072500 TEST-16A SECTION 41.                                             SG2034.2
072600 61DUMMY.                                                         SG2034.2
072700     EXIT.                                                        SG2034.2
072800 45  SECTION 45.                                                  SG2034.2
072900 PARA-45A.                                                        SG2034.2
073000     MOVE SPACE TO TEST-CHECK.                                    SG2034.2
073100*    NOTE     *****    TEST 17 BEGINS HERE   *****.               SG2034.2
073200     PERFORM PARA-45C.                                            SG2034.2
073300*    NOTE PERFORM PARAGRAPH THAT ALTERS A PARAGRAPH IN A UNIQUE   SG2034.2
073400*         SEGMENT CONTAINING THE SAME PRIORITY NUMBER.            SG2034.2
073500     PERFORM 45A THRU 45B.                                        SG2034.2
073600*    NOTE THE WRITE PARAGRAPH FOR THIS TEST IS IN SEGMENT 46.     SG2034.2
073700     GO TO 45A.                                                   SG2034.2
073800 DELETE-45.                                                       SG2034.2
073900     PERFORM DE-LETE.                                             SG2034.2
074000     GO TO WRITE-46.                                              SG2034.2
074100 45A SECTION 45.                                                  SG2034.2
074200 PARA-45B.                                                        SG2034.2
074300     GO TO PARA-45D.                                              SG2034.2
074400 45B SECTION 45.                                                  SG2034.2
074500 PARA-45C.                                                        SG2034.2
074600     ALTER PARA-45B TO PROCEED TO PARA-45E.                       SG2034.2
074700 PARA-45D.                                                        SG2034.2
074800     MOVE "BAD " TO TEST-CHECK.                                   SG2034.2
074900     GO TO PARA-45G.                                              SG2034.2
075000 PARA-45E.                                                        SG2034.2
075100     MOVE "GOOD" TO TEST-CHECK.                                   SG2034.2
075200     ALTER PARA-45B TO PROCEED TO PARA-45F.                       SG2034.2
075300     GO TO PARA-45G.                                              SG2034.2
075400 PARA-45F.                                                        SG2034.2
075500     MOVE "BETR" TO TEST-CHECK.                                   SG2034.2
075600*    NOTE THE GO TO STMT IN PARA-45A SHOULD SHIFT CONTROL THRU    SG2034.2
075700*    THIS PARAGRAPH AND FALL THRU TO THE EXIT FOLLOWED BY THE     SG2034.2
075800*         COMPARISON OF TEST-CHECK.                               SG2034.2
075900 PARA-45G.                                                        SG2034.2
076000     EXIT.                                                        SG2034.2
076100 46  SECTION 46.                                                  SG2034.2
076200 PARA-46.                                                         SG2034.2
076300     IF TEST-CHECK EQUAL TO "BETR"                                SG2034.2
076400         PERFORM PASS                                             SG2034.2
076500         GO TO WRITE-46.                                          SG2034.2
076600     MOVE "BETR" TO CORRECT-A.                                    SG2034.2
076700     MOVE TEST-CHECK TO COMPUTED-A.                               SG2034.2
076800     PERFORM FAIL.                                                SG2034.2
076900 WRITE-46.                                                        SG2034.2
077000     MOVE "SEGM-TEST-17" TO PAR-NAME.                             SG2034.2
077100     MOVE "TEST BEGINS IN PARA-46" TO RE-MARK.                    SG2034.2
077200     PERFORM PRINT-DETAIL.                                        SG2034.2
077300 TEST-18 SECTION 47.                                              SG2034.2
077400 PARA-47.                                                         SG2034.2
077500     MOVE SPACE TO TEST-CHECK.                                    SG2034.2
077600*    NOTE ************** TEST 18 BEGINS HERE ***************.     SG2034.2
077700     ALTER PARA-05 TO PROCEED TO 69.                              SG2034.2
077800     GO TO 05.                                                    SG2034.2
077900 05 SECTION 05.                                                   SG2034.2
078000 PARA-05.                                                         SG2034.2
078100     GO TO.                                                       SG2034.2
078200 PARA-05A.                                                        SG2034.2
078300     MOVE "BAD" TO COMPUTED-A.                                    SG2034.2
078400     PERFORM FAIL.                                                SG2034.2
078500     GO TO WRITE-69.                                              SG2034.2
078600 69 SECTION 69.                                                   SG2034.2
078700 PARA-69.                                                         SG2034.2
078800     PERFORM PASS.                                                SG2034.2
078900 WRITE-69.                                                        SG2034.2
079000     MOVE "ALTER RES TO NON-RES" TO FEATURE.                      SG2034.2
079100     MOVE "SEGM-TEST-18" TO PAR-NAME.                             SG2034.2
079200     MOVE "TEST BEGINS IN PARA-47" TO RE-MARK.                    SG2034.2
079300     PERFORM PRINT-DETAIL.                                        SG2034.2
079400     GO TO    CLOSE-FILES.                                        SG2034.2
      *END-OF,SG203A                                                                  
