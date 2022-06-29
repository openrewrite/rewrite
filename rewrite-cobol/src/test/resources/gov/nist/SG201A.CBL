      *HEADER,COBOL,SG201A                                                            
000100 IDENTIFICATION DIVISION.                                         SG2014.2
000200 PROGRAM-ID.                                                      SG2014.2
000300     SG201A.                                                      SG2014.2
000400 AUTHOR.                                                          SG2014.2
000500     FEDERAL COMPILER TESTING CENTER.                             SG2014.2
000600 INSTALLATION.                                                    SG2014.2
000700     GENERAL SERVICES ADMINISTRATION                              SG2014.2
000800*     AUTOMATED DATA AND TELECOMMUNICATION SERVICE.                SG2014.2
000900     SOFTWARE DEVELOPMENT OFFICE.                                 SG2014.2
001000     5203 LEESBURG PIKE  SUITE 1100                               SG2014.2
001100     FALLS CHURCH VIRGINIA 22041.                                 SG2014.2
001200                                                                  SG2014.2
001300     PHONE   (703) 756-6153                                       SG2014.2
001400                                                                  SG2014.2
001500     " HIGH       ".                                              SG2014.2
001600 DATE-WRITTEN.                                                    SG2014.2
001700     CCVS-74 VERSION 4.0 - 1980 JULY 1.                           SG2014.2
001800     CREATION DATE     /    VALIDATION DATE                       SG2014.2
001900     "4.2 ".                                                      SG2014.2
002000 SECURITY.                                                        SG2014.2
002100     NONE.                                                        SG2014.2
002200       THE FOLLOWING FEATURES ARE TESTED BY THIS PROGRAM ---      SG2014.2
002300         SEGMENT-LIMIT FEATURE IS TESTED BY USE OF ALTER          SG2014.2
002400         PERFORM AND GO TO STATEMENTS VERIFYING LAST-USED         SG2014.2
002500         STATE ON SEGMENTS GREATER THAN AND EQUAL TO THE          SG2014.2
002600         SEGMENT-LIMIT INCLUDING SEGMENTS PERMANENTLY RESIDENT    SG2014.2
002700         (LESS THAN SEGMENT-LIMIT) WITH THE INITIAL STATE         SG2014.2
002800         ON SEGMENTS GREATER THAN 49.                             SG2014.2
002900                                                                  SG2014.2
003000*                                                                 SG2014.2
003100 ENVIRONMENT DIVISION.                                            SG2014.2
003200 CONFIGURATION SECTION.                                           SG2014.2
003300 SOURCE-COMPUTER.                                                 SG2014.2
003400     XXXXX082.                                                    SG2014.2
003500 OBJECT-COMPUTER.                                                 SG2014.2
003600     XXXXX083                                                     SG2014.2
003700     SEGMENT-LIMIT IS 30.                                         SG2014.2
003800 INPUT-OUTPUT SECTION.                                            SG2014.2
003900 FILE-CONTROL.                                                    SG2014.2
004000     SELECT PRINT-FILE ASSIGN TO                                  SG2014.2
004100     XXXXX055.                                                    SG2014.2
004200 DATA DIVISION.                                                   SG2014.2
004300 FILE SECTION.                                                    SG2014.2
004400 FD  PRINT-FILE                                                   SG2014.2
004500     LABEL RECORDS                                                SG2014.2
004600     XXXXX084                                                     SG2014.2
004700     DATA RECORD IS PRINT-REC DUMMY-RECORD.                       SG2014.2
004800 01  PRINT-REC PICTURE X(120).                                    SG2014.2
004900 01  DUMMY-RECORD PICTURE X(120).                                 SG2014.2
005000 WORKING-STORAGE SECTION.                                         SG2014.2
005100 77  TEST-CHECK                    PICTURE XXXX VALUE SPACE.      SG2014.2
005200 01  TEST-RESULTS.                                                SG2014.2
005300     02 FILLER                    PICTURE X VALUE SPACE.          SG2014.2
005400     02 FEATURE                   PICTURE X(20) VALUE SPACE.      SG2014.2
005500     02 FILLER                    PICTURE X VALUE SPACE.          SG2014.2
005600     02 P-OR-F                    PICTURE X(5) VALUE SPACE.       SG2014.2
005700     02 FILLER                    PICTURE X  VALUE SPACE.         SG2014.2
005800     02  PAR-NAME.                                                SG2014.2
005900       03 FILLER PICTURE X(12) VALUE SPACE.                       SG2014.2
006000       03  PARDOT-X PICTURE X  VALUE SPACE.                       SG2014.2
006100       03 DOTVALUE PICTURE 99  VALUE ZERO.                        SG2014.2
006200       03 FILLER PIC X(5) VALUE SPACE.                            SG2014.2
006300     02 FILLER PIC X(10) VALUE SPACE.                             SG2014.2
006400     02 RE-MARK PIC X(61).                                        SG2014.2
006500 01  TEST-COMPUTED.                                               SG2014.2
006600     02 FILLER PIC X(30) VALUE SPACE.                             SG2014.2
006700     02 FILLER PIC X(17) VALUE "       COMPUTED=".                SG2014.2
006800     02 COMPUTED-X.                                               SG2014.2
006900     03 COMPUTED-A                PICTURE X(20) VALUE SPACE.      SG2014.2
007000     03 COMPUTED-N REDEFINES COMPUTED-A PICTURE -9(9).9(9).       SG2014.2
007100     03 COMPUTED-0V18 REDEFINES COMPUTED-A  PICTURE -.9(18).      SG2014.2
007200     03 COMPUTED-4V14 REDEFINES COMPUTED-A  PICTURE -9(4).9(14).  SG2014.2
007300     03 COMPUTED-14V4 REDEFINES COMPUTED-A  PICTURE -9(14).9(4).  SG2014.2
007400     03       CM-18V0 REDEFINES COMPUTED-A.                       SG2014.2
007500         04 COMPUTED-18V0                   PICTURE -9(18).       SG2014.2
007600         04 FILLER                          PICTURE X.            SG2014.2
007700     03 FILLER PIC X(50) VALUE SPACE.                             SG2014.2
007800 01  TEST-CORRECT.                                                SG2014.2
007900     02 FILLER PIC X(30) VALUE SPACE.                             SG2014.2
008000     02 FILLER PIC X(17) VALUE "       CORRECT =".                SG2014.2
008100     02 CORRECT-X.                                                SG2014.2
008200     03 CORRECT-A                 PICTURE X(20) VALUE SPACE.      SG2014.2
008300     03 CORRECT-N REDEFINES CORRECT-A PICTURE -9(9).9(9).         SG2014.2
008400     03 CORRECT-0V18 REDEFINES CORRECT-A    PICTURE -.9(18).      SG2014.2
008500     03 CORRECT-4V14 REDEFINES CORRECT-A    PICTURE -9(4).9(14).  SG2014.2
008600     03 CORRECT-14V4 REDEFINES CORRECT-A    PICTURE -9(14).9(4).  SG2014.2
008700     03      CR-18V0 REDEFINES CORRECT-A.                         SG2014.2
008800         04 CORRECT-18V0                    PICTURE -9(18).       SG2014.2
008900         04 FILLER                          PICTURE X.            SG2014.2
009000     03 FILLER PIC X(50) VALUE SPACE.                             SG2014.2
009100 01  CCVS-C-1.                                                    SG2014.2
009200     02 FILLER PICTURE IS X(99) VALUE IS " FEATURE              PASG2014.2
009300-    "SS  PARAGRAPH-NAME                                          SG2014.2
009400-    "        REMARKS".                                           SG2014.2
009500     02 FILLER PICTURE IS X(20) VALUE IS SPACE.                   SG2014.2
009600 01  CCVS-C-2.                                                    SG2014.2
009700     02 FILLER PICTURE IS X VALUE IS SPACE.                       SG2014.2
009800     02 FILLER PICTURE IS X(6) VALUE IS "TESTED".                 SG2014.2
009900     02 FILLER PICTURE IS X(15) VALUE IS SPACE.                   SG2014.2
010000     02 FILLER PICTURE IS X(4) VALUE IS "FAIL".                   SG2014.2
010100     02 FILLER PICTURE IS X(94) VALUE IS SPACE.                   SG2014.2
010200 01  REC-SKL-SUB PICTURE 9(2) VALUE ZERO.                         SG2014.2
010300 01  REC-CT PICTURE 99 VALUE ZERO.                                SG2014.2
010400 01  DELETE-CNT                   PICTURE 999  VALUE ZERO.        SG2014.2
010500 01  ERROR-COUNTER PICTURE IS 999 VALUE IS ZERO.                  SG2014.2
010600 01  INSPECT-COUNTER PIC 999 VALUE ZERO.                          SG2014.2
010700 01  PASS-COUNTER PIC 999 VALUE ZERO.                             SG2014.2
010800 01  TOTAL-ERROR PIC 999 VALUE ZERO.                              SG2014.2
010900 01  ERROR-HOLD PIC 999 VALUE ZERO.                               SG2014.2
011000 01  DUMMY-HOLD PIC X(120) VALUE SPACE.                           SG2014.2
011100 01  RECORD-COUNT PIC 9(5) VALUE ZERO.                            SG2014.2
011200 01  CCVS-H-1.                                                    SG2014.2
011300     02  FILLER   PICTURE X(27)  VALUE SPACE.                     SG2014.2
011400     02 FILLER PICTURE X(67) VALUE                                SG2014.2
011500     " FEDERAL COMPILER TESTING CENTER COBOL COMPILER VALIDATION  SG2014.2
011600-    " SYSTEM".                                                   SG2014.2
011700     02  FILLER     PICTURE X(26)  VALUE SPACE.                   SG2014.2
011800 01  CCVS-H-2.                                                    SG2014.2
011900     02 FILLER PICTURE X(52) VALUE IS                             SG2014.2
012000     "CCVS74 NCC  COPY, NOT FOR DISTRIBUTION.".                   SG2014.2
012100     02 FILLER PICTURE IS X(19) VALUE IS "TEST RESULTS SET-  ".   SG2014.2
012200     02 TEST-ID PICTURE IS X(9).                                  SG2014.2
012300     02 FILLER PICTURE IS X(40) VALUE IS SPACE.                   SG2014.2
012400 01  CCVS-H-3.                                                    SG2014.2
012500     02  FILLER PICTURE X(34) VALUE                               SG2014.2
012600     " FOR OFFICIAL USE ONLY    ".                                SG2014.2
012700     02  FILLER PICTURE X(58) VALUE                               SG2014.2
012800     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SG2014.2
012900     02  FILLER PICTURE X(28) VALUE                               SG2014.2
013000     "  COPYRIGHT   1974 ".                                       SG2014.2
013100 01  CCVS-E-1.                                                    SG2014.2
013200     02 FILLER PICTURE IS X(52) VALUE IS SPACE.                   SG2014.2
013300     02 FILLER PICTURE IS X(14) VALUE IS "END OF TEST-  ".        SG2014.2
013400     02 ID-AGAIN PICTURE IS X(9).                                 SG2014.2
013500     02 FILLER PICTURE X(45) VALUE IS                             SG2014.2
013600     " NTIS DISTRIBUTION COBOL 74".                               SG2014.2
013700 01  CCVS-E-2.                                                    SG2014.2
013800     02  FILLER                   PICTURE X(31)  VALUE            SG2014.2
013900     SPACE.                                                       SG2014.2
014000     02  FILLER                   PICTURE X(21)  VALUE SPACE.     SG2014.2
014100     02 CCVS-E-2-2.                                               SG2014.2
014200         03 ERROR-TOTAL PICTURE IS XXX VALUE IS SPACE.            SG2014.2
014300         03 FILLER PICTURE IS X VALUE IS SPACE.                   SG2014.2
014400         03 ENDER-DESC PIC X(44) VALUE "ERRORS ENCOUNTERED".      SG2014.2
014500 01  CCVS-E-3.                                                    SG2014.2
014600     02  FILLER PICTURE X(22) VALUE                               SG2014.2
014700     " FOR OFFICIAL USE ONLY".                                    SG2014.2
014800     02  FILLER PICTURE X(12) VALUE SPACE.                        SG2014.2
014900     02  FILLER PICTURE X(58) VALUE                               SG2014.2
015000     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SG2014.2
015100     02  FILLER PICTURE X(13) VALUE SPACE.                        SG2014.2
015200     02 FILLER PIC X(15) VALUE " COPYRIGHT 1974".                 SG2014.2
015300 01  CCVS-E-4.                                                    SG2014.2
015400     02 CCVS-E-4-1 PIC XXX VALUE SPACE.                           SG2014.2
015500     02 FILLER PIC XXXX VALUE " OF ".                             SG2014.2
015600     02 CCVS-E-4-2 PIC XXX VALUE SPACE.                           SG2014.2
015700     02 FILLER PIC X(40) VALUE                                    SG2014.2
015800      "  TESTS WERE EXECUTED SUCCESSFULLY".                       SG2014.2
015900 01  XXINFO.                                                      SG2014.2
016000     02 FILLER PIC X(30) VALUE "        *** INFORMATION  ***".    SG2014.2
016100     02 INFO-TEXT.                                                SG2014.2
016200     04 FILLER PIC X(20) VALUE SPACE.                             SG2014.2
016300     04 XXCOMPUTED PIC X(20).                                     SG2014.2
016400     04 FILLER PIC X(5) VALUE SPACE.                              SG2014.2
016500     04 XXCORRECT PIC X(20).                                      SG2014.2
016600 01  HYPHEN-LINE.                                                 SG2014.2
016700     02 FILLER PICTURE IS X VALUE IS SPACE.                       SG2014.2
016800     02 FILLER PICTURE IS X(65) VALUE IS "************************SG2014.2
016900-    "*****************************************".                 SG2014.2
017000     02 FILLER PICTURE IS X(54) VALUE IS "************************SG2014.2
017100-    "******************************".                            SG2014.2
017200 01  CCVS-PGM-ID PIC X(6) VALUE                                   SG2014.2
017300     "SG201A".                                                    SG2014.2
017400 PROCEDURE DIVISION.                                              SG2014.2
017500 HOUSEKEEPING SECTION 50.                                         SG2014.2
017600 PARAGRAPH-1.                                                     SG2014.2
017700     PERFORM   CCVS1.                                             SG2014.2
017800     GO TO SEG-TEST-1.                                            SG2014.2
017900 CCVS1 SECTION.                                                   SG2014.2
018000 OPEN-FILES.                                                      SG2014.2
018100     OPEN     OUTPUT PRINT-FILE.                                  SG2014.2
018200     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   SG2014.2
018300     MOVE    SPACE TO TEST-RESULTS.                               SG2014.2
018400     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             SG2014.2
018500     GO TO CCVS1-EXIT.                                            SG2014.2
018600 CLOSE-FILES.                                                     SG2014.2
018700     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   SG2014.2
018800 TERMINATE-CCVS.                                                  SG2014.2
018900*    EXIT PROGRAM.                                                SG2014.2
019000*TERMINATE-CALL.                                                  SG2014.2
019100     STOP     RUN.                                                SG2014.2
019200 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         SG2014.2
019300 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           SG2014.2
019400 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          SG2014.2
019500 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-CNT.          SG2014.2
019600     MOVE "****TEST DELETED****" TO RE-MARK.                      SG2014.2
019700 PRINT-DETAIL.                                                    SG2014.2
019800     IF REC-CT NOT EQUAL TO ZERO                                  SG2014.2
019900             MOVE "." TO PARDOT-X                                 SG2014.2
020000             MOVE REC-CT TO DOTVALUE.                             SG2014.2
020100     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      SG2014.2
020200     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               SG2014.2
020300        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 SG2014.2
020400          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 SG2014.2
020500     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              SG2014.2
020600     MOVE SPACE TO CORRECT-X.                                     SG2014.2
020700     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         SG2014.2
020800     MOVE     SPACE TO RE-MARK.                                   SG2014.2
020900 HEAD-ROUTINE.                                                    SG2014.2
021000     MOVE CCVS-H-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SG2014.2
021100     MOVE CCVS-H-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.   SG2014.2
021200     MOVE CCVS-H-3 TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.   SG2014.2
021300 COLUMN-NAMES-ROUTINE.                                            SG2014.2
021400     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SG2014.2
021500     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SG2014.2
021600     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        SG2014.2
021700 END-ROUTINE.                                                     SG2014.2
021800     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.SG2014.2
021900 END-RTN-EXIT.                                                    SG2014.2
022000     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SG2014.2
022100 END-ROUTINE-1.                                                   SG2014.2
022200      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      SG2014.2
022300      ERROR-HOLD. ADD DELETE-CNT TO ERROR-HOLD.                   SG2014.2
022400      ADD PASS-COUNTER TO ERROR-HOLD.                             SG2014.2
022500*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   SG2014.2
022600      MOVE PASS-COUNTER TO CCVS-E-4-1.                            SG2014.2
022700      MOVE ERROR-HOLD TO CCVS-E-4-2.                              SG2014.2
022800      MOVE CCVS-E-4 TO CCVS-E-2-2.                                SG2014.2
022900      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           SG2014.2
023000  END-ROUTINE-12.                                                 SG2014.2
023100      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        SG2014.2
023200     IF       ERROR-COUNTER IS EQUAL TO ZERO                      SG2014.2
023300         MOVE "NO " TO ERROR-TOTAL                                SG2014.2
023400         ELSE                                                     SG2014.2
023500         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       SG2014.2
023600     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           SG2014.2
023700     PERFORM WRITE-LINE.                                          SG2014.2
023800 END-ROUTINE-13.                                                  SG2014.2
023900     IF DELETE-CNT IS EQUAL TO ZERO                               SG2014.2
024000         MOVE "NO " TO ERROR-TOTAL  ELSE                          SG2014.2
024100         MOVE DELETE-CNT TO ERROR-TOTAL.                          SG2014.2
024200     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   SG2014.2
024300     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SG2014.2
024400      IF   INSPECT-COUNTER EQUAL TO ZERO                          SG2014.2
024500          MOVE "NO " TO ERROR-TOTAL                               SG2014.2
024600      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   SG2014.2
024700      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            SG2014.2
024800      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          SG2014.2
024900     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SG2014.2
025000 WRITE-LINE.                                                      SG2014.2
025100     ADD 1 TO RECORD-COUNT.                                       SG2014.2
025200*    IF RECORD-COUNT GREATER 50                                   SG2014.2
025300*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          SG2014.2
025400*        MOVE SPACE TO DUMMY-RECORD                               SG2014.2
025500*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  SG2014.2
025600*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             SG2014.2
025700*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     SG2014.2
025800*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          SG2014.2
025900*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          SG2014.2
026000*        MOVE ZERO TO RECORD-COUNT.                               SG2014.2
026100     PERFORM WRT-LN.                                              SG2014.2
026200 WRT-LN.                                                          SG2014.2
026300     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               SG2014.2
026400     MOVE SPACE TO DUMMY-RECORD.                                  SG2014.2
026500 BLANK-LINE-PRINT.                                                SG2014.2
026600     PERFORM WRT-LN.                                              SG2014.2
026700 FAIL-ROUTINE.                                                    SG2014.2
026800     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.   SG2014.2
026900     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.    SG2014.2
027000     MOVE "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.    SG2014.2
027100     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     SG2014.2
027200     GO TO FAIL-ROUTINE-EX.                                       SG2014.2
027300 FAIL-ROUTINE-WRITE.                                              SG2014.2
027400     MOVE TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE           SG2014.2
027500     MOVE TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES.   SG2014.2
027600 FAIL-ROUTINE-EX. EXIT.                                           SG2014.2
027700 BAIL-OUT.                                                        SG2014.2
027800     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.       SG2014.2
027900     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.               SG2014.2
028000 BAIL-OUT-WRITE.                                                  SG2014.2
028100     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  SG2014.2
028200     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     SG2014.2
028300 BAIL-OUT-EX. EXIT.                                               SG2014.2
028400 CCVS1-EXIT.                                                      SG2014.2
028500     EXIT.                                                        SG2014.2
028600 RUN-THE-TESTS SECTION.                                           SG2014.2
028700 SEG-TEST-1.                                                      SG2014.2
028800     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
028900     PERFORM 00.                                                  SG2014.2
029000     IF TEST-CHECK EQUAL TO "GOOD"                                SG2014.2
029100         PERFORM PASS                                             SG2014.2
029200         GO TO SEG-WRITE-1.                                       SG2014.2
029300     MOVE SPACE TO COMPUTED-A.                                    SG2014.2
029400     MOVE "GOOD" TO CORRECT-A.                                    SG2014.2
029500     PERFORM FAIL.                                                SG2014.2
029600     GO TO SEG-WRITE-1.                                           SG2014.2
029700 SEG-DELETE-1.                                                    SG2014.2
029800     PERFORM DE-LETE.                                             SG2014.2
029900 SEG-WRITE-1.                                                     SG2014.2
030000     MOVE "SEGMENT-LIMIT" TO FEATURE.                             SG2014.2
030100     MOVE "SEG-TEST-1  " TO PAR-NAME.                             SG2014.2
030200     PERFORM PRINT-DETAIL.                                        SG2014.2
030300 SEG-TEST-2.                                                      SG2014.2
030400     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
030500     PERFORM 30.                                                  SG2014.2
030600     PERFORM 30.                                                  SG2014.2
030700     IF TEST-CHECK EQUAL TO "GOOD"                                SG2014.2
030800         PERFORM PASS                                             SG2014.2
030900         GO TO SEG-WRITE-2.                                       SG2014.2
031000     MOVE TEST-CHECK TO COMPUTED-A.                               SG2014.2
031100     MOVE "GOOD" TO CORRECT-A.                                    SG2014.2
031200     PERFORM FAIL.                                                SG2014.2
031300     GO TO SEG-WRITE-2.                                           SG2014.2
031400 SEG-DELETE-2.                                                    SG2014.2
031500     PERFORM DE-LETE.                                             SG2014.2
031600 SEG-WRITE-2.                                                     SG2014.2
031700     MOVE "SEG-TEST-2  " TO PAR-NAME.                             SG2014.2
031800     PERFORM PRINT-DETAIL.                                        SG2014.2
031900 SEG-TEST-3.                                                      SG2014.2
032000     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
032100     PERFORM 31.                                                  SG2014.2
032200     PERFORM 31.                                                  SG2014.2
032300     IF TEST-CHECK EQUAL TO "GOOD"                                SG2014.2
032400         PERFORM PASS                                             SG2014.2
032500         GO TO SEG-WRITE-3.                                       SG2014.2
032600     MOVE TEST-CHECK TO COMPUTED-A.                               SG2014.2
032700     MOVE "GOOD" TO CORRECT-A.                                    SG2014.2
032800     PERFORM FAIL.                                                SG2014.2
032900     GO TO SEG-WRITE-3.                                           SG2014.2
033000 SEG-DELETE-3.                                                    SG2014.2
033100     PERFORM DE-LETE.                                             SG2014.2
033200 SEG-WRITE-3.                                                     SG2014.2
033300     MOVE "SEG-TEST-3  " TO PAR-NAME.                             SG2014.2
033400     PERFORM PRINT-DETAIL.                                        SG2014.2
033500 SEG-TEST-4.                                                      SG2014.2
033600     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
033700     PERFORM 32.                                                  SG2014.2
033800     PERFORM 32.                                                  SG2014.2
033900     IF TEST-CHECK EQUAL TO "GOOD"                                SG2014.2
034000         PERFORM PASS                                             SG2014.2
034100         GO TO SEG-WRITE-4.                                       SG2014.2
034200     MOVE TEST-CHECK TO COMPUTED-A.                               SG2014.2
034300     MOVE "GOOD" TO CORRECT-A.                                    SG2014.2
034400     PERFORM FAIL.                                                SG2014.2
034500     GO TO SEG-WRITE-4.                                           SG2014.2
034600 SEG-DELETE-4.                                                    SG2014.2
034700     PERFORM DE-LETE.                                             SG2014.2
034800 SEG-WRITE-4.                                                     SG2014.2
034900     MOVE "SEG-TEST-4  " TO PAR-NAME.                             SG2014.2
035000     PERFORM PRINT-DETAIL.                                        SG2014.2
035100 SEG-TEST-5.                                                      SG2014.2
035200     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
035300     PERFORM 33.                                                  SG2014.2
035400     PERFORM 33.                                                  SG2014.2
035500     IF TEST-CHECK EQUAL TO "GOOD"                                SG2014.2
035600         PERFORM PASS                                             SG2014.2
035700         GO TO SEG-WRITE-5.                                       SG2014.2
035800     MOVE TEST-CHECK TO COMPUTED-A.                               SG2014.2
035900     MOVE "GOOD" TO CORRECT-A.                                    SG2014.2
036000     PERFORM FAIL.                                                SG2014.2
036100     GO TO SEG-WRITE-5.                                           SG2014.2
036200 SEG-DELETE-5.                                                    SG2014.2
036300     PERFORM DE-LETE.                                             SG2014.2
036400 SEG-WRITE-5.                                                     SG2014.2
036500     MOVE "SEG-TEST-5  " TO PAR-NAME.                             SG2014.2
036600     PERFORM PRINT-DETAIL.                                        SG2014.2
036700 SEG-TEST-6.                                                      SG2014.2
036800     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
036900     PERFORM 34.                                                  SG2014.2
037000     PERFORM 34.                                                  SG2014.2
037100     IF TEST-CHECK EQUAL TO "GOOD"                                SG2014.2
037200         PERFORM PASS                                             SG2014.2
037300         GO TO SEG-WRITE-6.                                       SG2014.2
037400     MOVE TEST-CHECK TO COMPUTED-A.                               SG2014.2
037500     MOVE "GOOD" TO CORRECT-A.                                    SG2014.2
037600     PERFORM FAIL.                                                SG2014.2
037700     GO TO SEG-WRITE-6.                                           SG2014.2
037800 SEG-DELETE-6.                                                    SG2014.2
037900     PERFORM DE-LETE.                                             SG2014.2
038000 SEG-WRITE-6.                                                     SG2014.2
038100     MOVE "SEG-TEST-6  " TO PAR-NAME.                             SG2014.2
038200     PERFORM PRINT-DETAIL.                                        SG2014.2
038300 SEG-TEST-7.                                                      SG2014.2
038400     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
038500     PERFORM 35.                                                  SG2014.2
038600     PERFORM 35.                                                  SG2014.2
038700     IF TEST-CHECK EQUAL TO "GOOD"                                SG2014.2
038800         PERFORM PASS                                             SG2014.2
038900         GO TO SEG-WRITE-7.                                       SG2014.2
039000     MOVE TEST-CHECK TO COMPUTED-A.                               SG2014.2
039100     MOVE "GOOD" TO CORRECT-A.                                    SG2014.2
039200     PERFORM FAIL.                                                SG2014.2
039300     GO TO SEG-WRITE-7.                                           SG2014.2
039400 SEG-DELETE-7.                                                    SG2014.2
039500     PERFORM DE-LETE.                                             SG2014.2
039600 SEG-WRITE-7.                                                     SG2014.2
039700     MOVE "SEG-TEST-7  " TO PAR-NAME.                             SG2014.2
039800     PERFORM PRINT-DETAIL.                                        SG2014.2
039900 SEG-TEST-8.                                                      SG2014.2
040000     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
040100     PERFORM 36.                                                  SG2014.2
040200     PERFORM 36.                                                  SG2014.2
040300     IF TEST-CHECK EQUAL TO "GOOD"                                SG2014.2
040400         PERFORM PASS                                             SG2014.2
040500         GO TO SEG-WRITE-8.                                       SG2014.2
040600     MOVE TEST-CHECK TO COMPUTED-A.                               SG2014.2
040700     MOVE "GOOD" TO CORRECT-A.                                    SG2014.2
040800     PERFORM FAIL.                                                SG2014.2
040900     GO TO SEG-WRITE-8.                                           SG2014.2
041000 SEG-DELETE-8.                                                    SG2014.2
041100     PERFORM DE-LETE.                                             SG2014.2
041200 SEG-WRITE-8.                                                     SG2014.2
041300     MOVE "SEG-TEST-8  " TO PAR-NAME.                             SG2014.2
041400     PERFORM PRINT-DETAIL.                                        SG2014.2
041500 SEG-TEST-9.                                                      SG2014.2
041600     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
041700     PERFORM 37.                                                  SG2014.2
041800     PERFORM 37.                                                  SG2014.2
041900     IF TEST-CHECK EQUAL TO "GOOD"                                SG2014.2
042000         PERFORM PASS                                             SG2014.2
042100         GO TO SEG-WRITE-9.                                       SG2014.2
042200     MOVE TEST-CHECK TO COMPUTED-A.                               SG2014.2
042300     MOVE "GOOD" TO CORRECT-A.                                    SG2014.2
042400     PERFORM FAIL.                                                SG2014.2
042500     GO TO SEG-WRITE-9.                                           SG2014.2
042600 SEG-DELETE-9.                                                    SG2014.2
042700     PERFORM DE-LETE.                                             SG2014.2
042800 SEG-WRITE-9.                                                     SG2014.2
042900     MOVE "SEG-TEST-9  " TO PAR-NAME.                             SG2014.2
043000     PERFORM PRINT-DETAIL.                                        SG2014.2
043100 SEG-TEST-10.                                                     SG2014.2
043200     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
043300     PERFORM 38.                                                  SG2014.2
043400     PERFORM 38.                                                  SG2014.2
043500     IF TEST-CHECK EQUAL TO "GOOD"                                SG2014.2
043600         PERFORM PASS                                             SG2014.2
043700         GO TO SEG-WRITE-10.                                      SG2014.2
043800     MOVE TEST-CHECK TO COMPUTED-A.                               SG2014.2
043900     MOVE "GOOD" TO CORRECT-A.                                    SG2014.2
044000     PERFORM FAIL.                                                SG2014.2
044100     GO TO SEG-WRITE-10.                                          SG2014.2
044200 SEG-DELETE-10.                                                   SG2014.2
044300     PERFORM DE-LETE.                                             SG2014.2
044400 SEG-WRITE-10.                                                    SG2014.2
044500     MOVE "SEG-TEST-10 " TO PAR-NAME.                             SG2014.2
044600     PERFORM PRINT-DETAIL.                                        SG2014.2
044700 SEG-TEST-11.                                                     SG2014.2
044800     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
044900     PERFORM 39.                                                  SG2014.2
045000     PERFORM 39.                                                  SG2014.2
045100     IF TEST-CHECK EQUAL TO "GOOD"                                SG2014.2
045200         PERFORM PASS                                             SG2014.2
045300         GO TO SEG-WRITE-11.                                      SG2014.2
045400     MOVE TEST-CHECK TO COMPUTED-A.                               SG2014.2
045500     MOVE "GOOD" TO CORRECT-A.                                    SG2014.2
045600     PERFORM FAIL.                                                SG2014.2
045700     GO TO SEG-WRITE-11.                                          SG2014.2
045800 SEG-DELETE-11.                                                   SG2014.2
045900     PERFORM DE-LETE.                                             SG2014.2
046000 SEG-WRITE-11.                                                    SG2014.2
046100     MOVE "SEG-TEST-11 " TO PAR-NAME.                             SG2014.2
046200     PERFORM PRINT-DETAIL.                                        SG2014.2
046300 SEG-TEST-12.                                                     SG2014.2
046400     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
046500     PERFORM 40.                                                  SG2014.2
046600     PERFORM 40.                                                  SG2014.2
046700     IF TEST-CHECK EQUAL TO "GOOD"                                SG2014.2
046800         PERFORM PASS                                             SG2014.2
046900         GO TO SEG-WRITE-12.                                      SG2014.2
047000     MOVE TEST-CHECK TO COMPUTED-A.                               SG2014.2
047100     MOVE "GOOD" TO CORRECT-A.                                    SG2014.2
047200     PERFORM FAIL.                                                SG2014.2
047300     GO TO SEG-WRITE-12.                                          SG2014.2
047400 SEG-DELETE-12.                                                   SG2014.2
047500     PERFORM DE-LETE.                                             SG2014.2
047600 SEG-WRITE-12.                                                    SG2014.2
047700     MOVE "SEG-TEST-12 " TO PAR-NAME.                             SG2014.2
047800     PERFORM PRINT-DETAIL.                                        SG2014.2
047900 SEG-TEST-13.                                                     SG2014.2
048000     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
048100     PERFORM 41.                                                  SG2014.2
048200     PERFORM 41.                                                  SG2014.2
048300     IF TEST-CHECK EQUAL TO "GOOD"                                SG2014.2
048400         PERFORM PASS                                             SG2014.2
048500         GO TO SEG-WRITE-13.                                      SG2014.2
048600     MOVE TEST-CHECK TO COMPUTED-A.                               SG2014.2
048700     MOVE "GOOD" TO CORRECT-A.                                    SG2014.2
048800     PERFORM FAIL.                                                SG2014.2
048900     GO TO SEG-WRITE-13.                                          SG2014.2
049000 SEG-DELETE-13.                                                   SG2014.2
049100     PERFORM DE-LETE.                                             SG2014.2
049200 SEG-WRITE-13.                                                    SG2014.2
049300     MOVE "SEG-TEST-13 " TO PAR-NAME.                             SG2014.2
049400     PERFORM PRINT-DETAIL.                                        SG2014.2
049500 SEG-TEST-14.                                                     SG2014.2
049600     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
049700     PERFORM 42.                                                  SG2014.2
049800     PERFORM 42.                                                  SG2014.2
049900     IF TEST-CHECK EQUAL TO "GOOD"                                SG2014.2
050000         PERFORM PASS                                             SG2014.2
050100         GO TO SEG-WRITE-14.                                      SG2014.2
050200     MOVE TEST-CHECK TO COMPUTED-A.                               SG2014.2
050300     MOVE "GOOD" TO CORRECT-A.                                    SG2014.2
050400     PERFORM FAIL.                                                SG2014.2
050500     GO TO SEG-WRITE-14.                                          SG2014.2
050600 SEG-DELETE-14.                                                   SG2014.2
050700     PERFORM DE-LETE.                                             SG2014.2
050800 SEG-WRITE-14.                                                    SG2014.2
050900     MOVE "SEG-TEST-14 " TO PAR-NAME.                             SG2014.2
051000     PERFORM PRINT-DETAIL.                                        SG2014.2
051100 SEG-TEST-15.                                                     SG2014.2
051200     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
051300     PERFORM 43.                                                  SG2014.2
051400     PERFORM 43.                                                  SG2014.2
051500     IF TEST-CHECK EQUAL TO "GOOD"                                SG2014.2
051600         PERFORM PASS                                             SG2014.2
051700         GO TO SEG-WRITE-15.                                      SG2014.2
051800     MOVE TEST-CHECK TO COMPUTED-A.                               SG2014.2
051900     MOVE "GOOD" TO CORRECT-A.                                    SG2014.2
052000     PERFORM FAIL.                                                SG2014.2
052100     GO TO SEG-WRITE-15.                                          SG2014.2
052200 SEG-DELETE-15.                                                   SG2014.2
052300     PERFORM DE-LETE.                                             SG2014.2
052400 SEG-WRITE-15.                                                    SG2014.2
052500     MOVE "SEG-TEST-15 " TO PAR-NAME.                             SG2014.2
052600     PERFORM PRINT-DETAIL.                                        SG2014.2
052700 SEG-TEST-16.                                                     SG2014.2
052800     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
052900     PERFORM 44.                                                  SG2014.2
053000     PERFORM 44.                                                  SG2014.2
053100     IF TEST-CHECK EQUAL TO "GOOD"                                SG2014.2
053200         PERFORM PASS                                             SG2014.2
053300         GO TO SEG-WRITE-16.                                      SG2014.2
053400     MOVE TEST-CHECK TO COMPUTED-A.                               SG2014.2
053500     MOVE "GOOD" TO CORRECT-A.                                    SG2014.2
053600     PERFORM FAIL.                                                SG2014.2
053700     GO TO SEG-WRITE-16.                                          SG2014.2
053800 SEG-DELETE-16.                                                   SG2014.2
053900     PERFORM DE-LETE.                                             SG2014.2
054000 SEG-WRITE-16.                                                    SG2014.2
054100     MOVE "SEG-TEST-16 " TO PAR-NAME.                             SG2014.2
054200     PERFORM PRINT-DETAIL.                                        SG2014.2
054300 SEG-TEST-17.                                                     SG2014.2
054400     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
054500     PERFORM 45.                                                  SG2014.2
054600     PERFORM 45.                                                  SG2014.2
054700     IF TEST-CHECK EQUAL TO "GOOD"                                SG2014.2
054800         PERFORM PASS                                             SG2014.2
054900         GO TO SEG-WRITE-17.                                      SG2014.2
055000     MOVE TEST-CHECK TO COMPUTED-A.                               SG2014.2
055100     MOVE "GOOD" TO CORRECT-A.                                    SG2014.2
055200     PERFORM FAIL.                                                SG2014.2
055300     GO TO SEG-WRITE-17.                                          SG2014.2
055400 SEG-DELETE-17.                                                   SG2014.2
055500     PERFORM DE-LETE.                                             SG2014.2
055600 SEG-WRITE-17.                                                    SG2014.2
055700     MOVE "SEG-TEST-17 " TO PAR-NAME.                             SG2014.2
055800     PERFORM PRINT-DETAIL.                                        SG2014.2
055900 SEG-TEST-18.                                                     SG2014.2
056000     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
056100     PERFORM 46.                                                  SG2014.2
056200     PERFORM 46.                                                  SG2014.2
056300     IF TEST-CHECK EQUAL TO "GOOD"                                SG2014.2
056400         PERFORM PASS                                             SG2014.2
056500         GO TO SEG-WRITE-18.                                      SG2014.2
056600     MOVE TEST-CHECK TO COMPUTED-A.                               SG2014.2
056700     MOVE "GOOD" TO CORRECT-A.                                    SG2014.2
056800     PERFORM FAIL.                                                SG2014.2
056900     GO TO SEG-WRITE-18.                                          SG2014.2
057000 SEG-DELETE-18.                                                   SG2014.2
057100     PERFORM DE-LETE.                                             SG2014.2
057200 SEG-WRITE-18.                                                    SG2014.2
057300     MOVE "SEG-TEST-18 " TO PAR-NAME.                             SG2014.2
057400     PERFORM PRINT-DETAIL.                                        SG2014.2
057500 SEG-TEST-19.                                                     SG2014.2
057600     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
057700     PERFORM 47.                                                  SG2014.2
057800     PERFORM 47.                                                  SG2014.2
057900     IF TEST-CHECK EQUAL TO "GOOD"                                SG2014.2
058000         PERFORM PASS                                             SG2014.2
058100         GO TO SEG-WRITE-19.                                      SG2014.2
058200     MOVE TEST-CHECK TO COMPUTED-A.                               SG2014.2
058300     MOVE "GOOD" TO CORRECT-A.                                    SG2014.2
058400     PERFORM FAIL.                                                SG2014.2
058500     GO TO SEG-WRITE-19.                                          SG2014.2
058600 SEG-DELETE-19.                                                   SG2014.2
058700     PERFORM DE-LETE.                                             SG2014.2
058800 SEG-WRITE-19.                                                    SG2014.2
058900     MOVE "SEG-TEST-19 " TO PAR-NAME.                             SG2014.2
059000     PERFORM PRINT-DETAIL.                                        SG2014.2
059100 SEG-TEST-20.                                                     SG2014.2
059200     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
059300     PERFORM 48.                                                  SG2014.2
059400     PERFORM 48.                                                  SG2014.2
059500     IF TEST-CHECK EQUAL TO "GOOD"                                SG2014.2
059600         PERFORM PASS                                             SG2014.2
059700         GO TO SEG-WRITE-20.                                      SG2014.2
059800     MOVE TEST-CHECK TO COMPUTED-A.                               SG2014.2
059900     MOVE "GOOD" TO CORRECT-A.                                    SG2014.2
060000     PERFORM FAIL.                                                SG2014.2
060100     GO TO SEG-WRITE-20.                                          SG2014.2
060200 SEG-DELETE-20.                                                   SG2014.2
060300     PERFORM DE-LETE.                                             SG2014.2
060400 SEG-WRITE-20.                                                    SG2014.2
060500     MOVE "SEG-TEST-20 " TO PAR-NAME.                             SG2014.2
060600     PERFORM PRINT-DETAIL.                                        SG2014.2
060700 SEG-TEST-21.                                                     SG2014.2
060800     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
060900     PERFORM 49.                                                  SG2014.2
061000     PERFORM 49.                                                  SG2014.2
061100     IF TEST-CHECK EQUAL TO "GOOD"                                SG2014.2
061200         PERFORM PASS                                             SG2014.2
061300         GO TO SEG-WRITE-21.                                      SG2014.2
061400     MOVE TEST-CHECK TO COMPUTED-A.                               SG2014.2
061500     MOVE "GOOD" TO CORRECT-A.                                    SG2014.2
061600     PERFORM FAIL.                                                SG2014.2
061700     GO TO SEG-WRITE-21.                                          SG2014.2
061800 SEG-DELETE-21.                                                   SG2014.2
061900     PERFORM DE-LETE.                                             SG2014.2
062000 SEG-WRITE-21.                                                    SG2014.2
062100     MOVE "SEG-TEST-21 " TO PAR-NAME.                             SG2014.2
062200     PERFORM PRINT-DETAIL.                                        SG2014.2
062300 SEG-TEST-22.                                                     SG2014.2
062400     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
062500     PERFORM 50.                                                  SG2014.2
062600     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
062700     PERFORM 50.                                                  SG2014.2
062800     IF TEST-CHECK EQUAL TO "GOOD"                                SG2014.2
062900         PERFORM PASS                                             SG2014.2
063000         GO TO SEG-WRITE-22.                                      SG2014.2
063100     MOVE SPACE TO COMPUTED-A.                                    SG2014.2
063200     MOVE "GOOD" TO CORRECT-A.                                    SG2014.2
063300     PERFORM FAIL.                                                SG2014.2
063400     GO TO SEG-WRITE-22.                                          SG2014.2
063500 SEG-DELETE-22.                                                   SG2014.2
063600     PERFORM DE-LETE.                                             SG2014.2
063700 SEG-WRITE-22.                                                    SG2014.2
063800     MOVE "SEG-TEST-22 " TO PAR-NAME.                             SG2014.2
063900     PERFORM PRINT-DETAIL.                                        SG2014.2
064000 SEG-TEST-23.                                                     SG2014.2
064100     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
064200     PERFORM 51.                                                  SG2014.2
064300     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
064400     PERFORM 51.                                                  SG2014.2
064500     IF TEST-CHECK EQUAL TO "GOOD"                                SG2014.2
064600         PERFORM PASS                                             SG2014.2
064700         GO TO SEG-WRITE-23.                                      SG2014.2
064800     MOVE SPACE TO COMPUTED-A.                                    SG2014.2
064900     MOVE "GOOD" TO CORRECT-A.                                    SG2014.2
065000     PERFORM FAIL.                                                SG2014.2
065100     GO TO SEG-WRITE-23.                                          SG2014.2
065200 SEG-DELETE-23.                                                   SG2014.2
065300     PERFORM DE-LETE.                                             SG2014.2
065400 SEG-WRITE-23.                                                    SG2014.2
065500     MOVE "SEG-TEST-23 " TO PAR-NAME.                             SG2014.2
065600     PERFORM PRINT-DETAIL.                                        SG2014.2
065700 SEG-TEST-24.                                                     SG2014.2
065800     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
065900     PERFORM 52.                                                  SG2014.2
066000     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
066100     PERFORM 52.                                                  SG2014.2
066200     IF TEST-CHECK EQUAL TO "GOOD"                                SG2014.2
066300         PERFORM PASS                                             SG2014.2
066400         GO TO SEG-WRITE-24.                                      SG2014.2
066500     MOVE SPACE TO COMPUTED-A.                                    SG2014.2
066600     MOVE "GOOD" TO CORRECT-A.                                    SG2014.2
066700     PERFORM FAIL.                                                SG2014.2
066800     GO TO SEG-WRITE-24.                                          SG2014.2
066900 SEG-DELETE-24.                                                   SG2014.2
067000     PERFORM DE-LETE.                                             SG2014.2
067100 SEG-WRITE-24.                                                    SG2014.2
067200     MOVE "SEG-TEST-24 " TO PAR-NAME.                             SG2014.2
067300     PERFORM PRINT-DETAIL.                                        SG2014.2
067400 SEG-TEST-25.                                                     SG2014.2
067500     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
067600     PERFORM 53.                                                  SG2014.2
067700     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
067800     PERFORM 53.                                                  SG2014.2
067900     IF TEST-CHECK EQUAL TO "GOOD"                                SG2014.2
068000         PERFORM PASS                                             SG2014.2
068100         GO TO SEG-WRITE-25.                                      SG2014.2
068200     MOVE SPACE TO COMPUTED-A.                                    SG2014.2
068300     MOVE "GOOD" TO CORRECT-A.                                    SG2014.2
068400     PERFORM FAIL.                                                SG2014.2
068500     GO TO SEG-WRITE-25.                                          SG2014.2
068600 SEG-DELETE-25.                                                   SG2014.2
068700     PERFORM DE-LETE.                                             SG2014.2
068800 SEG-WRITE-25.                                                    SG2014.2
068900     MOVE "SEG-TEST-25 " TO PAR-NAME.                             SG2014.2
069000     PERFORM PRINT-DETAIL.                                        SG2014.2
069100 SEG-TEST-26.                                                     SG2014.2
069200     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
069300     PERFORM 54.                                                  SG2014.2
069400     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
069500     PERFORM 54.                                                  SG2014.2
069600     IF TEST-CHECK EQUAL TO "GOOD"                                SG2014.2
069700         PERFORM PASS                                             SG2014.2
069800         GO TO SEG-WRITE-26.                                      SG2014.2
069900     MOVE SPACE TO COMPUTED-A.                                    SG2014.2
070000     MOVE "GOOD" TO CORRECT-A.                                    SG2014.2
070100     PERFORM FAIL.                                                SG2014.2
070200     GO TO SEG-WRITE-26.                                          SG2014.2
070300 SEG-DELETE-26.                                                   SG2014.2
070400     PERFORM DE-LETE.                                             SG2014.2
070500 SEG-WRITE-26.                                                    SG2014.2
070600     MOVE "SEG-TEST-26 " TO PAR-NAME.                             SG2014.2
070700     PERFORM PRINT-DETAIL.                                        SG2014.2
070800 SEG-TEST-27.                                                     SG2014.2
070900     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
071000     PERFORM 55.                                                  SG2014.2
071100     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
071200     PERFORM 55.                                                  SG2014.2
071300     IF TEST-CHECK EQUAL TO "GOOD"                                SG2014.2
071400         PERFORM PASS                                             SG2014.2
071500         GO TO SEG-WRITE-27.                                      SG2014.2
071600     MOVE SPACE TO COMPUTED-A.                                    SG2014.2
071700     MOVE "GOOD" TO CORRECT-A.                                    SG2014.2
071800     PERFORM FAIL.                                                SG2014.2
071900     GO TO SEG-WRITE-27.                                          SG2014.2
072000 SEG-DELETE-27.                                                   SG2014.2
072100     PERFORM DE-LETE.                                             SG2014.2
072200 SEG-WRITE-27.                                                    SG2014.2
072300     MOVE "SEG-TEST-27 " TO PAR-NAME.                             SG2014.2
072400     PERFORM PRINT-DETAIL.                                        SG2014.2
072500 SEG-TEST-28.                                                     SG2014.2
072600     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
072700     PERFORM 56.                                                  SG2014.2
072800     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
072900     PERFORM 56.                                                  SG2014.2
073000     IF TEST-CHECK EQUAL TO "GOOD"                                SG2014.2
073100         PERFORM PASS                                             SG2014.2
073200         GO TO SEG-WRITE-28.                                      SG2014.2
073300     MOVE SPACE TO COMPUTED-A.                                    SG2014.2
073400     MOVE "GOOD" TO CORRECT-A.                                    SG2014.2
073500     PERFORM FAIL.                                                SG2014.2
073600     GO TO SEG-WRITE-28.                                          SG2014.2
073700 SEG-DELETE-28.                                                   SG2014.2
073800     PERFORM DE-LETE.                                             SG2014.2
073900 SEG-WRITE-28.                                                    SG2014.2
074000     MOVE "SEG-TEST-28 " TO PAR-NAME.                             SG2014.2
074100     PERFORM PRINT-DETAIL.                                        SG2014.2
074200 SEG-TEST-29.                                                     SG2014.2
074300     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
074400     PERFORM 57.                                                  SG2014.2
074500     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
074600     PERFORM 57.                                                  SG2014.2
074700     IF TEST-CHECK EQUAL TO "GOOD"                                SG2014.2
074800         PERFORM PASS                                             SG2014.2
074900         GO TO SEG-WRITE-29.                                      SG2014.2
075000     MOVE SPACE TO COMPUTED-A.                                    SG2014.2
075100     MOVE "GOOD" TO CORRECT-A.                                    SG2014.2
075200     PERFORM FAIL.                                                SG2014.2
075300     GO TO SEG-WRITE-29.                                          SG2014.2
075400 SEG-DELETE-29.                                                   SG2014.2
075500     PERFORM DE-LETE.                                             SG2014.2
075600 SEG-WRITE-29.                                                    SG2014.2
075700     MOVE "SEG-TEST-29 " TO PAR-NAME.                             SG2014.2
075800     PERFORM PRINT-DETAIL.                                        SG2014.2
075900 SEG-TEST-30.                                                     SG2014.2
076000     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
076100     PERFORM 58.                                                  SG2014.2
076200     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
076300     PERFORM 58.                                                  SG2014.2
076400     IF TEST-CHECK EQUAL TO "GOOD"                                SG2014.2
076500         PERFORM PASS                                             SG2014.2
076600         GO TO SEG-WRITE-30.                                      SG2014.2
076700     MOVE SPACE TO COMPUTED-A.                                    SG2014.2
076800     MOVE "GOOD" TO CORRECT-A.                                    SG2014.2
076900     PERFORM FAIL.                                                SG2014.2
077000     GO TO SEG-WRITE-30.                                          SG2014.2
077100 SEG-DELETE-30.                                                   SG2014.2
077200     PERFORM DE-LETE.                                             SG2014.2
077300 SEG-WRITE-30.                                                    SG2014.2
077400     MOVE "SEG-TEST-30 " TO PAR-NAME.                             SG2014.2
077500     PERFORM PRINT-DETAIL.                                        SG2014.2
077600 SEG-TEST-31.                                                     SG2014.2
077700     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
077800     PERFORM 59.                                                  SG2014.2
077900     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
078000     PERFORM 59.                                                  SG2014.2
078100     IF TEST-CHECK EQUAL TO "GOOD"                                SG2014.2
078200         PERFORM PASS                                             SG2014.2
078300         GO TO SEG-WRITE-31.                                      SG2014.2
078400     MOVE SPACE TO COMPUTED-A.                                    SG2014.2
078500     MOVE "GOOD" TO CORRECT-A.                                    SG2014.2
078600     PERFORM FAIL.                                                SG2014.2
078700     GO TO SEG-WRITE-31.                                          SG2014.2
078800 SEG-DELETE-31.                                                   SG2014.2
078900     PERFORM DE-LETE.                                             SG2014.2
079000 SEG-WRITE-31.                                                    SG2014.2
079100     MOVE "SEG-TEST-31 " TO PAR-NAME.                             SG2014.2
079200     PERFORM PRINT-DETAIL.                                        SG2014.2
079300 SEG-TEST-32.                                                     SG2014.2
079400     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
079500     PERFORM 60.                                                  SG2014.2
079600     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
079700     PERFORM 60.                                                  SG2014.2
079800     IF TEST-CHECK EQUAL TO "GOOD"                                SG2014.2
079900         PERFORM PASS                                             SG2014.2
080000         GO TO SEG-WRITE-32.                                      SG2014.2
080100     MOVE SPACE TO COMPUTED-A.                                    SG2014.2
080200     MOVE "GOOD" TO CORRECT-A.                                    SG2014.2
080300     PERFORM FAIL.                                                SG2014.2
080400     GO TO SEG-WRITE-32.                                          SG2014.2
080500 SEG-DELETE-32.                                                   SG2014.2
080600     PERFORM DE-LETE.                                             SG2014.2
080700 SEG-WRITE-32.                                                    SG2014.2
080800     MOVE "SEG-TEST-32 " TO PAR-NAME.                             SG2014.2
080900     PERFORM PRINT-DETAIL.                                        SG2014.2
081000 SEG-TEST-33.                                                     SG2014.2
081100     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
081200     PERFORM 60.                                                  SG2014.2
081300     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
081400     PERFORM 60.                                                  SG2014.2
081500     IF TEST-CHECK EQUAL TO "GOOD"                                SG2014.2
081600         PERFORM PASS                                             SG2014.2
081700         GO TO SEG-WRITE-33.                                      SG2014.2
081800     MOVE SPACE TO COMPUTED-A.                                    SG2014.2
081900     MOVE "GOOD" TO CORRECT-A.                                    SG2014.2
082000     PERFORM FAIL.                                                SG2014.2
082100     GO TO SEG-WRITE-33.                                          SG2014.2
082200 SEG-DELETE-33.                                                   SG2014.2
082300     PERFORM DE-LETE.                                             SG2014.2
082400 SEG-WRITE-33.                                                    SG2014.2
082500     MOVE "SEG-TEST-33 " TO PAR-NAME.                             SG2014.2
082600     PERFORM PRINT-DETAIL.                                        SG2014.2
082700 SEG-TEST-34.                                                     SG2014.2
082800     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
082900     PERFORM 59.                                                  SG2014.2
083000     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
083100     PERFORM 59.                                                  SG2014.2
083200     IF TEST-CHECK EQUAL TO "GOOD"                                SG2014.2
083300         PERFORM PASS                                             SG2014.2
083400         GO TO SEG-WRITE-34.                                      SG2014.2
083500     MOVE SPACE TO COMPUTED-A.                                    SG2014.2
083600     MOVE "GOOD" TO CORRECT-A.                                    SG2014.2
083700     PERFORM FAIL.                                                SG2014.2
083800     GO TO SEG-WRITE-34.                                          SG2014.2
083900 SEG-DELETE-34.                                                   SG2014.2
084000     PERFORM DE-LETE.                                             SG2014.2
084100 SEG-WRITE-34.                                                    SG2014.2
084200     MOVE "SEG-TEST-34 " TO PAR-NAME.                             SG2014.2
084300     PERFORM PRINT-DETAIL.                                        SG2014.2
084400 SEG-TEST-35.                                                     SG2014.2
084500     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
084600     PERFORM 58.                                                  SG2014.2
084700     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
084800     PERFORM 58.                                                  SG2014.2
084900     IF TEST-CHECK EQUAL TO "GOOD"                                SG2014.2
085000         PERFORM PASS                                             SG2014.2
085100         GO TO SEG-WRITE-35.                                      SG2014.2
085200     MOVE SPACE TO COMPUTED-A.                                    SG2014.2
085300     MOVE "GOOD" TO CORRECT-A.                                    SG2014.2
085400     PERFORM FAIL.                                                SG2014.2
085500     GO TO SEG-WRITE-35.                                          SG2014.2
085600 SEG-DELETE-35.                                                   SG2014.2
085700     PERFORM DE-LETE.                                             SG2014.2
085800 SEG-WRITE-35.                                                    SG2014.2
085900     MOVE "SEG-TEST-35 " TO PAR-NAME.                             SG2014.2
086000     PERFORM PRINT-DETAIL.                                        SG2014.2
086100 SEG-TEST-36.                                                     SG2014.2
086200     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
086300     PERFORM 57.                                                  SG2014.2
086400     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
086500     PERFORM 57.                                                  SG2014.2
086600     IF TEST-CHECK EQUAL TO "GOOD"                                SG2014.2
086700         PERFORM PASS                                             SG2014.2
086800         GO TO SEG-WRITE-36.                                      SG2014.2
086900     MOVE SPACE TO COMPUTED-A.                                    SG2014.2
087000     MOVE "GOOD" TO CORRECT-A.                                    SG2014.2
087100     PERFORM FAIL.                                                SG2014.2
087200     GO TO SEG-WRITE-36.                                          SG2014.2
087300 SEG-DELETE-36.                                                   SG2014.2
087400     PERFORM DE-LETE.                                             SG2014.2
087500 SEG-WRITE-36.                                                    SG2014.2
087600     MOVE "SEG-TEST-36 " TO PAR-NAME.                             SG2014.2
087700     PERFORM PRINT-DETAIL.                                        SG2014.2
087800 SEG-TEST-37.                                                     SG2014.2
087900     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
088000     PERFORM 56.                                                  SG2014.2
088100     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
088200     PERFORM 56.                                                  SG2014.2
088300     IF TEST-CHECK EQUAL TO "GOOD"                                SG2014.2
088400         PERFORM PASS                                             SG2014.2
088500         GO TO SEG-WRITE-37.                                      SG2014.2
088600     MOVE SPACE TO COMPUTED-A.                                    SG2014.2
088700     MOVE "GOOD" TO CORRECT-A.                                    SG2014.2
088800     PERFORM FAIL.                                                SG2014.2
088900     GO TO SEG-WRITE-37.                                          SG2014.2
089000 SEG-DELETE-37.                                                   SG2014.2
089100     PERFORM DE-LETE.                                             SG2014.2
089200 SEG-WRITE-37.                                                    SG2014.2
089300     MOVE "SEG-TEST-37 " TO PAR-NAME.                             SG2014.2
089400     PERFORM PRINT-DETAIL.                                        SG2014.2
089500 SEG-TEST-38.                                                     SG2014.2
089600     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
089700     PERFORM 55.                                                  SG2014.2
089800     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
089900     PERFORM 55.                                                  SG2014.2
090000     IF TEST-CHECK EQUAL TO "GOOD"                                SG2014.2
090100         PERFORM PASS                                             SG2014.2
090200         GO TO SEG-WRITE-38.                                      SG2014.2
090300     MOVE SPACE TO COMPUTED-A.                                    SG2014.2
090400     MOVE "GOOD" TO CORRECT-A.                                    SG2014.2
090500     GO TO SEG-WRITE-38.                                          SG2014.2
090600 SEG-DELETE-38.                                                   SG2014.2
090700     PERFORM DE-LETE.                                             SG2014.2
090800 SEG-WRITE-38.                                                    SG2014.2
090900     MOVE "SEG-TEST-38 " TO PAR-NAME.                             SG2014.2
091000     PERFORM PRINT-DETAIL.                                        SG2014.2
091100 SEG-TEST-39.                                                     SG2014.2
091200     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
091300     PERFORM 54.                                                  SG2014.2
091400     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
091500     PERFORM 54.                                                  SG2014.2
091600     IF TEST-CHECK EQUAL TO "GOOD"                                SG2014.2
091700         PERFORM PASS                                             SG2014.2
091800         GO TO SEG-WRITE-39.                                      SG2014.2
091900     MOVE SPACE TO COMPUTED-A.                                    SG2014.2
092000     MOVE "GOOD" TO CORRECT-A.                                    SG2014.2
092100     PERFORM FAIL.                                                SG2014.2
092200     GO TO SEG-WRITE-39.                                          SG2014.2
092300 SEG-DELETE-39.                                                   SG2014.2
092400     PERFORM DE-LETE.                                             SG2014.2
092500 SEG-WRITE-39.                                                    SG2014.2
092600     MOVE "SEG-TEST-39 " TO PAR-NAME.                             SG2014.2
092700     PERFORM PRINT-DETAIL.                                        SG2014.2
092800 SEG-TEST-40.                                                     SG2014.2
092900     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
093000     PERFORM 53.                                                  SG2014.2
093100     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
093200     PERFORM 53.                                                  SG2014.2
093300     IF TEST-CHECK EQUAL TO "GOOD"                                SG2014.2
093400         PERFORM PASS                                             SG2014.2
093500         GO TO SEG-WRITE-40.                                      SG2014.2
093600     MOVE SPACE TO COMPUTED-A.                                    SG2014.2
093700     MOVE "GOOD" TO CORRECT-A.                                    SG2014.2
093800     PERFORM FAIL.                                                SG2014.2
093900     GO TO SEG-WRITE-40.                                          SG2014.2
094000 SEG-DELETE-40.                                                   SG2014.2
094100     PERFORM DE-LETE.                                             SG2014.2
094200 SEG-WRITE-40.                                                    SG2014.2
094300     MOVE "SEG-TEST-40 " TO PAR-NAME.                             SG2014.2
094400     PERFORM PRINT-DETAIL.                                        SG2014.2
094500 SEG-TEST-41.                                                     SG2014.2
094600     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
094700     PERFORM 52.                                                  SG2014.2
094800     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
094900     PERFORM 52.                                                  SG2014.2
095000     IF TEST-CHECK EQUAL TO "GOOD"                                SG2014.2
095100         PERFORM PASS                                             SG2014.2
095200         GO TO SEG-WRITE-41.                                      SG2014.2
095300     MOVE SPACE TO COMPUTED-A.                                    SG2014.2
095400     MOVE "GOOD" TO CORRECT-A.                                    SG2014.2
095500     PERFORM FAIL.                                                SG2014.2
095600     GO TO SEG-WRITE-41.                                          SG2014.2
095700 SEG-DELETE-41.                                                   SG2014.2
095800     PERFORM DE-LETE.                                             SG2014.2
095900 SEG-WRITE-41.                                                    SG2014.2
096000     MOVE "SEG-TEST-41 " TO PAR-NAME.                             SG2014.2
096100     PERFORM PRINT-DETAIL.                                        SG2014.2
096200 SEG-TEST-42.                                                     SG2014.2
096300     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
096400     PERFORM 51.                                                  SG2014.2
096500     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
096600     PERFORM 51.                                                  SG2014.2
096700     IF TEST-CHECK EQUAL TO "GOOD"                                SG2014.2
096800         PERFORM PASS                                             SG2014.2
096900         GO TO SEG-WRITE-42.                                      SG2014.2
097000     MOVE SPACE TO COMPUTED-A.                                    SG2014.2
097100     MOVE "GOOD" TO CORRECT-A.                                    SG2014.2
097200     PERFORM FAIL.                                                SG2014.2
097300     GO TO SEG-WRITE-42.                                          SG2014.2
097400 SEG-DELETE-42.                                                   SG2014.2
097500     PERFORM DE-LETE.                                             SG2014.2
097600 SEG-WRITE-42.                                                    SG2014.2
097700     MOVE "SEG-TEST-42 " TO PAR-NAME.                             SG2014.2
097800     PERFORM PRINT-DETAIL.                                        SG2014.2
097900 SEG-TEST-43.                                                     SG2014.2
098000     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
098100     PERFORM 50.                                                  SG2014.2
098200     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
098300     PERFORM 50.                                                  SG2014.2
098400     IF TEST-CHECK EQUAL TO "GOOD"                                SG2014.2
098500         PERFORM PASS                                             SG2014.2
098600         GO TO SEG-WRITE-43.                                      SG2014.2
098700     MOVE SPACE TO COMPUTED-A.                                    SG2014.2
098800     MOVE "GOOD" TO CORRECT-A.                                    SG2014.2
098900     PERFORM FAIL.                                                SG2014.2
099000     GO TO SEG-WRITE-43.                                          SG2014.2
099100 SEG-DELETE-43.                                                   SG2014.2
099200     PERFORM DE-LETE.                                             SG2014.2
099300 SEG-WRITE-43.                                                    SG2014.2
099400     MOVE "SEG-TEST-43 " TO PAR-NAME.                             SG2014.2
099500     PERFORM PRINT-DETAIL.                                        SG2014.2
099600 SEG-TEST-44.                                                     SG2014.2
099700     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
099800     PERFORM 49.                                                  SG2014.2
099900     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
100000     PERFORM 49.                                                  SG2014.2
100100     IF TEST-CHECK EQUAL TO "GOOD"                                SG2014.2
100200         PERFORM PASS                                             SG2014.2
100300         GO TO SEG-WRITE-44.                                      SG2014.2
100400     MOVE SPACE TO COMPUTED-A.                                    SG2014.2
100500     MOVE "GOOD" TO CORRECT-A.                                    SG2014.2
100600     PERFORM FAIL.                                                SG2014.2
100700     GO TO SEG-WRITE-44.                                          SG2014.2
100800 SEG-DELETE-44.                                                   SG2014.2
100900     PERFORM DE-LETE.                                             SG2014.2
101000 SEG-WRITE-44.                                                    SG2014.2
101100     MOVE "SEG-TEST-44 " TO PAR-NAME.                             SG2014.2
101200     PERFORM PRINT-DETAIL.                                        SG2014.2
101300 SEG-TEST-45.                                                     SG2014.2
101400     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
101500     PERFORM 48.                                                  SG2014.2
101600     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
101700     PERFORM 48.                                                  SG2014.2
101800     IF TEST-CHECK EQUAL TO "GOOD"                                SG2014.2
101900         PERFORM PASS                                             SG2014.2
102000         GO TO SEG-WRITE-45.                                      SG2014.2
102100     MOVE SPACE TO COMPUTED-A.                                    SG2014.2
102200     MOVE "GOOD" TO CORRECT-A.                                    SG2014.2
102300     PERFORM FAIL.                                                SG2014.2
102400     GO TO SEG-WRITE-45.                                          SG2014.2
102500 SEG-DELETE-45.                                                   SG2014.2
102600     PERFORM DE-LETE.                                             SG2014.2
102700 SEG-WRITE-45.                                                    SG2014.2
102800     MOVE "SEG-TEST-45 " TO PAR-NAME.                             SG2014.2
102900     PERFORM PRINT-DETAIL.                                        SG2014.2
103000 SEG-TEST-46.                                                     SG2014.2
103100     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
103200     PERFORM 47.                                                  SG2014.2
103300     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
103400     PERFORM 47.                                                  SG2014.2
103500     IF TEST-CHECK EQUAL TO "GOOD"                                SG2014.2
103600         PERFORM PASS                                             SG2014.2
103700         GO TO SEG-WRITE-46.                                      SG2014.2
103800     MOVE SPACE TO COMPUTED-A.                                    SG2014.2
103900     MOVE "GOOD" TO CORRECT-A.                                    SG2014.2
104000     PERFORM FAIL.                                                SG2014.2
104100     GO TO SEG-WRITE-46.                                          SG2014.2
104200 SEG-DELETE-46.                                                   SG2014.2
104300     PERFORM DE-LETE.                                             SG2014.2
104400 SEG-WRITE-46.                                                    SG2014.2
104500     MOVE "SEG-TEST-46 " TO PAR-NAME.                             SG2014.2
104600     PERFORM PRINT-DETAIL.                                        SG2014.2
104700 SEG-TEST-47.                                                     SG2014.2
104800     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
104900     PERFORM 46.                                                  SG2014.2
105000     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
105100     PERFORM 46.                                                  SG2014.2
105200     IF TEST-CHECK EQUAL TO "GOOD"                                SG2014.2
105300         PERFORM PASS                                             SG2014.2
105400         GO TO SEG-WRITE-47.                                      SG2014.2
105500     MOVE SPACE TO COMPUTED-A.                                    SG2014.2
105600     MOVE "GOOD" TO CORRECT-A.                                    SG2014.2
105700     PERFORM FAIL.                                                SG2014.2
105800     GO TO SEG-WRITE-47.                                          SG2014.2
105900 SEG-DELETE-47.                                                   SG2014.2
106000     PERFORM DE-LETE.                                             SG2014.2
106100 SEG-WRITE-47.                                                    SG2014.2
106200     MOVE "SEG-TEST-47 " TO PAR-NAME.                             SG2014.2
106300     PERFORM PRINT-DETAIL.                                        SG2014.2
106400 SEG-TEST-48.                                                     SG2014.2
106500     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
106600     PERFORM 45.                                                  SG2014.2
106700     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
106800     PERFORM 45.                                                  SG2014.2
106900     IF TEST-CHECK EQUAL TO "GOOD"                                SG2014.2
107000         PERFORM PASS                                             SG2014.2
107100         GO TO SEG-WRITE-48.                                      SG2014.2
107200     MOVE SPACE TO COMPUTED-A.                                    SG2014.2
107300     MOVE "GOOD" TO CORRECT-A.                                    SG2014.2
107400     PERFORM FAIL.                                                SG2014.2
107500     GO TO SEG-WRITE-48.                                          SG2014.2
107600 SEG-DELETE-48.                                                   SG2014.2
107700     PERFORM DE-LETE.                                             SG2014.2
107800 SEG-WRITE-48.                                                    SG2014.2
107900     MOVE "SEG-TEST-48 " TO PAR-NAME.                             SG2014.2
108000     PERFORM PRINT-DETAIL.                                        SG2014.2
108100 SEG-TEST-49.                                                     SG2014.2
108200     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
108300     PERFORM 44.                                                  SG2014.2
108400     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
108500     PERFORM 44.                                                  SG2014.2
108600     IF TEST-CHECK EQUAL TO "GOOD"                                SG2014.2
108700         PERFORM PASS                                             SG2014.2
108800         GO TO SEG-WRITE-49.                                      SG2014.2
108900     MOVE SPACE TO COMPUTED-A.                                    SG2014.2
109000     MOVE "GOOD" TO CORRECT-A.                                    SG2014.2
109100     PERFORM FAIL.                                                SG2014.2
109200     GO TO SEG-WRITE-49.                                          SG2014.2
109300 SEG-DELETE-49.                                                   SG2014.2
109400     PERFORM DE-LETE.                                             SG2014.2
109500 SEG-WRITE-49.                                                    SG2014.2
109600     MOVE "SEG-TEST-49 " TO PAR-NAME.                             SG2014.2
109700     PERFORM PRINT-DETAIL.                                        SG2014.2
109800 SEG-TEST-50.                                                     SG2014.2
109900     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
110000     PERFORM 43.                                                  SG2014.2
110100     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
110200     PERFORM 43.                                                  SG2014.2
110300     IF TEST-CHECK EQUAL TO "GOOD"                                SG2014.2
110400         PERFORM PASS                                             SG2014.2
110500         GO TO SEG-WRITE-50.                                      SG2014.2
110600     MOVE SPACE TO COMPUTED-A.                                    SG2014.2
110700     MOVE "GOOD" TO CORRECT-A.                                    SG2014.2
110800     PERFORM FAIL.                                                SG2014.2
110900     GO TO SEG-WRITE-50.                                          SG2014.2
111000 SEG-DELETE-50.                                                   SG2014.2
111100     PERFORM DE-LETE.                                             SG2014.2
111200 SEG-WRITE-50.                                                    SG2014.2
111300     MOVE "SEG-TEST-50 " TO PAR-NAME.                             SG2014.2
111400     PERFORM PRINT-DETAIL.                                        SG2014.2
111500 SEG-TEST-51.                                                     SG2014.2
111600     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
111700     PERFORM 42.                                                  SG2014.2
111800     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
111900     PERFORM 42.                                                  SG2014.2
112000     IF TEST-CHECK EQUAL TO "GOOD"                                SG2014.2
112100         PERFORM PASS                                             SG2014.2
112200         GO TO SEG-WRITE-51.                                      SG2014.2
112300     MOVE SPACE TO COMPUTED-A.                                    SG2014.2
112400     MOVE "GOOD" TO CORRECT-A.                                    SG2014.2
112500     PERFORM FAIL.                                                SG2014.2
112600     GO TO SEG-WRITE-51.                                          SG2014.2
112700 SEG-DELETE-51.                                                   SG2014.2
112800     PERFORM DE-LETE.                                             SG2014.2
112900 SEG-WRITE-51.                                                    SG2014.2
113000     MOVE "SEG-TEST-51 " TO PAR-NAME.                             SG2014.2
113100     PERFORM PRINT-DETAIL.                                        SG2014.2
113200 SEG-TEST-52.                                                     SG2014.2
113300     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
113400     PERFORM 41.                                                  SG2014.2
113500     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
113600     PERFORM 41.                                                  SG2014.2
113700     IF TEST-CHECK EQUAL TO "GOOD"                                SG2014.2
113800         PERFORM PASS                                             SG2014.2
113900         GO TO SEG-WRITE-52.                                      SG2014.2
114000     MOVE SPACE TO COMPUTED-A.                                    SG2014.2
114100     MOVE "GOOD" TO CORRECT-A.                                    SG2014.2
114200     PERFORM FAIL.                                                SG2014.2
114300     GO TO SEG-WRITE-52.                                          SG2014.2
114400 SEG-DELETE-52.                                                   SG2014.2
114500     PERFORM DE-LETE.                                             SG2014.2
114600 SEG-WRITE-52.                                                    SG2014.2
114700     MOVE "SEG-TEST-52 " TO PAR-NAME.                             SG2014.2
114800     PERFORM PRINT-DETAIL.                                        SG2014.2
114900 SEG-TEST-53.                                                     SG2014.2
115000     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
115100     PERFORM 40.                                                  SG2014.2
115200     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
115300     PERFORM 40.                                                  SG2014.2
115400     IF TEST-CHECK EQUAL TO "GOOD"                                SG2014.2
115500         PERFORM PASS                                             SG2014.2
115600         GO TO SEG-WRITE-53.                                      SG2014.2
115700     MOVE SPACE TO COMPUTED-A.                                    SG2014.2
115800     MOVE "GOOD" TO CORRECT-A.                                    SG2014.2
115900     PERFORM FAIL.                                                SG2014.2
116000     GO TO SEG-WRITE-53.                                          SG2014.2
116100 SEG-DELETE-53.                                                   SG2014.2
116200     PERFORM DE-LETE.                                             SG2014.2
116300 SEG-WRITE-53.                                                    SG2014.2
116400     MOVE "SEG-TEST-53 " TO PAR-NAME.                             SG2014.2
116500     PERFORM PRINT-DETAIL.                                        SG2014.2
116600 SEG-TEST-54.                                                     SG2014.2
116700     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
116800     PERFORM 39.                                                  SG2014.2
116900     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
117000     PERFORM 39.                                                  SG2014.2
117100     IF TEST-CHECK EQUAL TO "GOOD"                                SG2014.2
117200         PERFORM PASS                                             SG2014.2
117300         GO TO SEG-WRITE-54.                                      SG2014.2
117400     MOVE SPACE TO COMPUTED-A.                                    SG2014.2
117500     MOVE "GOOD" TO CORRECT-A.                                    SG2014.2
117600     PERFORM FAIL.                                                SG2014.2
117700     GO TO SEG-WRITE-54.                                          SG2014.2
117800 SEG-DELETE-54.                                                   SG2014.2
117900     PERFORM DE-LETE.                                             SG2014.2
118000 SEG-WRITE-54.                                                    SG2014.2
118100     MOVE "SEG-TEST-54 " TO PAR-NAME.                             SG2014.2
118200     PERFORM PRINT-DETAIL.                                        SG2014.2
118300 SEG-TEST-55.                                                     SG2014.2
118400     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
118500     PERFORM 38.                                                  SG2014.2
118600     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
118700     PERFORM 38.                                                  SG2014.2
118800     IF TEST-CHECK EQUAL TO "GOOD"                                SG2014.2
118900         PERFORM PASS                                             SG2014.2
119000         GO TO SEG-WRITE-55.                                      SG2014.2
119100     MOVE SPACE TO COMPUTED-A.                                    SG2014.2
119200     MOVE "GOOD" TO CORRECT-A.                                    SG2014.2
119300     PERFORM FAIL.                                                SG2014.2
119400     GO TO SEG-WRITE-55.                                          SG2014.2
119500 SEG-DELETE-55.                                                   SG2014.2
119600     PERFORM DE-LETE.                                             SG2014.2
119700 SEG-WRITE-55.                                                    SG2014.2
119800     MOVE "SEG-TEST-55 " TO PAR-NAME.                             SG2014.2
119900     PERFORM PRINT-DETAIL.                                        SG2014.2
120000 SEG-TEST-56.                                                     SG2014.2
120100     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
120200     PERFORM 37.                                                  SG2014.2
120300     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
120400     PERFORM 37.                                                  SG2014.2
120500     IF TEST-CHECK EQUAL TO "GOOD"                                SG2014.2
120600         PERFORM PASS                                             SG2014.2
120700         GO TO SEG-WRITE-56.                                      SG2014.2
120800     MOVE SPACE TO COMPUTED-A.                                    SG2014.2
120900     MOVE "GOOD" TO CORRECT-A.                                    SG2014.2
121000     PERFORM FAIL.                                                SG2014.2
121100     GO TO SEG-WRITE-56.                                          SG2014.2
121200 SEG-DELETE-56.                                                   SG2014.2
121300     PERFORM DE-LETE.                                             SG2014.2
121400 SEG-WRITE-56.                                                    SG2014.2
121500     MOVE "SEG-TEST-56 " TO PAR-NAME.                             SG2014.2
121600     PERFORM PRINT-DETAIL.                                        SG2014.2
121700 SEG-TEST-57.                                                     SG2014.2
121800     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
121900     PERFORM 36.                                                  SG2014.2
122000     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
122100     PERFORM 36.                                                  SG2014.2
122200     IF TEST-CHECK EQUAL TO "GOOD"                                SG2014.2
122300         PERFORM PASS                                             SG2014.2
122400         GO TO SEG-WRITE-57.                                      SG2014.2
122500     MOVE SPACE TO COMPUTED-A.                                    SG2014.2
122600     MOVE "GOOD" TO CORRECT-A.                                    SG2014.2
122700     PERFORM FAIL.                                                SG2014.2
122800     GO TO SEG-WRITE-57.                                          SG2014.2
122900 SEG-DELETE-57.                                                   SG2014.2
123000     PERFORM DE-LETE.                                             SG2014.2
123100 SEG-WRITE-57.                                                    SG2014.2
123200     MOVE "SEG-TEST-57 " TO PAR-NAME.                             SG2014.2
123300     PERFORM PRINT-DETAIL.                                        SG2014.2
123400 SEG-TEST-58.                                                     SG2014.2
123500     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
123600     PERFORM 35.                                                  SG2014.2
123700     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
123800     PERFORM 35.                                                  SG2014.2
123900     IF TEST-CHECK EQUAL TO "GOOD"                                SG2014.2
124000         PERFORM PASS                                             SG2014.2
124100         GO TO SEG-WRITE-58.                                      SG2014.2
124200     MOVE SPACE TO COMPUTED-A.                                    SG2014.2
124300     MOVE "GOOD" TO CORRECT-A.                                    SG2014.2
124400     PERFORM FAIL.                                                SG2014.2
124500     GO TO SEG-WRITE-58.                                          SG2014.2
124600 SEG-DELETE-58.                                                   SG2014.2
124700     PERFORM DE-LETE.                                             SG2014.2
124800 SEG-WRITE-58.                                                    SG2014.2
124900     MOVE "SEG-TEST-58 " TO PAR-NAME.                             SG2014.2
125000     PERFORM PRINT-DETAIL.                                        SG2014.2
125100 SEG-TEST-59.                                                     SG2014.2
125200     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
125300     PERFORM 34.                                                  SG2014.2
125400     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
125500     PERFORM 34.                                                  SG2014.2
125600     IF TEST-CHECK EQUAL TO "GOOD"                                SG2014.2
125700         PERFORM PASS                                             SG2014.2
125800         GO TO SEG-WRITE-59.                                      SG2014.2
125900     MOVE SPACE TO COMPUTED-A.                                    SG2014.2
126000     MOVE "GOOD" TO CORRECT-A.                                    SG2014.2
126100     PERFORM FAIL.                                                SG2014.2
126200     GO TO SEG-WRITE-59.                                          SG2014.2
126300 SEG-DELETE-59.                                                   SG2014.2
126400     PERFORM DE-LETE.                                             SG2014.2
126500 SEG-WRITE-59.                                                    SG2014.2
126600     MOVE "SEG-TEST-59 " TO PAR-NAME.                             SG2014.2
126700     PERFORM PRINT-DETAIL.                                        SG2014.2
126800 SEG-TEST-60.                                                     SG2014.2
126900     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
127000     PERFORM 33.                                                  SG2014.2
127100     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
127200     PERFORM 33.                                                  SG2014.2
127300     IF TEST-CHECK EQUAL TO "GOOD"                                SG2014.2
127400         PERFORM PASS                                             SG2014.2
127500         GO TO SEG-WRITE-60.                                      SG2014.2
127600     MOVE SPACE TO COMPUTED-A.                                    SG2014.2
127700     MOVE "GOOD" TO CORRECT-A.                                    SG2014.2
127800     PERFORM FAIL.                                                SG2014.2
127900     GO TO SEG-WRITE-60.                                          SG2014.2
128000 SEG-DELETE-60.                                                   SG2014.2
128100     PERFORM DE-LETE.                                             SG2014.2
128200 SEG-WRITE-60.                                                    SG2014.2
128300     MOVE "SEG-TEST-60 " TO PAR-NAME.                             SG2014.2
128400     PERFORM PRINT-DETAIL.                                        SG2014.2
128500 SEG-TEST-61.                                                     SG2014.2
128600     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
128700     PERFORM 32.                                                  SG2014.2
128800     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
128900     PERFORM 32.                                                  SG2014.2
129000     IF TEST-CHECK EQUAL TO "GOOD"                                SG2014.2
129100         PERFORM PASS                                             SG2014.2
129200         GO TO SEG-WRITE-61.                                      SG2014.2
129300     MOVE SPACE TO COMPUTED-A.                                    SG2014.2
129400     MOVE "GOOD" TO CORRECT-A.                                    SG2014.2
129500     PERFORM FAIL.                                                SG2014.2
129600     GO TO SEG-WRITE-61.                                          SG2014.2
129700 SEG-DELETE-61.                                                   SG2014.2
129800     PERFORM DE-LETE.                                             SG2014.2
129900 SEG-WRITE-61.                                                    SG2014.2
130000     MOVE "SEG-TEST-61 " TO PAR-NAME.                             SG2014.2
130100     PERFORM PRINT-DETAIL.                                        SG2014.2
130200 SEG-TEST-62.                                                     SG2014.2
130300     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
130400     PERFORM 31.                                                  SG2014.2
130500     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
130600     PERFORM 31.                                                  SG2014.2
130700     IF TEST-CHECK EQUAL TO "GOOD"                                SG2014.2
130800         PERFORM PASS                                             SG2014.2
130900         GO TO SEG-WRITE-62.                                      SG2014.2
131000     MOVE SPACE TO COMPUTED-A.                                    SG2014.2
131100     MOVE "GOOD" TO CORRECT-A.                                    SG2014.2
131200     PERFORM FAIL.                                                SG2014.2
131300     GO TO SEG-WRITE-62.                                          SG2014.2
131400 SEG-DELETE-62.                                                   SG2014.2
131500     PERFORM DE-LETE.                                             SG2014.2
131600 SEG-WRITE-62.                                                    SG2014.2
131700     MOVE "SEG-TEST-62 " TO PAR-NAME.                             SG2014.2
131800     PERFORM PRINT-DETAIL.                                        SG2014.2
131900 SEG-TEST-63.                                                     SG2014.2
132000     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
132100     PERFORM 30.                                                  SG2014.2
132200     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
132300     PERFORM 30.                                                  SG2014.2
132400     IF TEST-CHECK EQUAL TO "GOOD"                                SG2014.2
132500         PERFORM PASS                                             SG2014.2
132600         GO TO SEG-WRITE-63.                                      SG2014.2
132700     MOVE SPACE TO COMPUTED-A.                                    SG2014.2
132800     MOVE "GOOD" TO CORRECT-A.                                    SG2014.2
132900     PERFORM FAIL.                                                SG2014.2
133000     GO TO SEG-WRITE-63.                                          SG2014.2
133100 SEG-DELETE-63.                                                   SG2014.2
133200     PERFORM DE-LETE.                                             SG2014.2
133300 SEG-WRITE-63.                                                    SG2014.2
133400     MOVE "SEG-TEST-63 " TO PAR-NAME.                             SG2014.2
133500     PERFORM PRINT-DETAIL.                                        SG2014.2
133600 SEG-TEST-64.                                                     SG2014.2
133700     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
133800     PERFORM 99.                                                  SG2014.2
133900     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
134000     PERFORM 99.                                                  SG2014.2
134100     IF TEST-CHECK EQUAL TO "GOOD"                                SG2014.2
134200         PERFORM PASS                                             SG2014.2
134300         GO TO SEG-WRITE-64.                                      SG2014.2
134400     MOVE SPACE TO COMPUTED-A.                                    SG2014.2
134500     MOVE "GOOD" TO CORRECT-A.                                    SG2014.2
134600     PERFORM FAIL.                                                SG2014.2
134700     GO TO SEG-WRITE-64.                                          SG2014.2
134800 SEG-DELETE-64.                                                   SG2014.2
134900     PERFORM DE-LETE.                                             SG2014.2
135000 SEG-WRITE-64.                                                    SG2014.2
135100     MOVE "SEG-TEST-64 " TO PAR-NAME.                             SG2014.2
135200     PERFORM PRINT-DETAIL.                                        SG2014.2
135300 SEG-TEST-65.                                                     SG2014.2
135400     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
135500     PERFORM 99.                                                  SG2014.2
135600     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
135700     PERFORM 99.                                                  SG2014.2
135800     IF TEST-CHECK EQUAL TO "GOOD"                                SG2014.2
135900         PERFORM PASS                                             SG2014.2
136000         GO TO SEG-WRITE-65.                                      SG2014.2
136100     MOVE SPACE TO COMPUTED-A.                                    SG2014.2
136200     MOVE "GOOD" TO CORRECT-A.                                    SG2014.2
136300     PERFORM FAIL.                                                SG2014.2
136400     GO TO SEG-WRITE-65.                                          SG2014.2
136500 SEG-DELETE-65.                                                   SG2014.2
136600     PERFORM DE-LETE.                                             SG2014.2
136700 SEG-WRITE-65.                                                    SG2014.2
136800     MOVE "SEG-TEST-65 " TO PAR-NAME.                             SG2014.2
136900     PERFORM PRINT-DETAIL.                                        SG2014.2
137000 SEG-TEST-66.                                                     SG2014.2
137100     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
137200     PERFORM 37.                                                  SG2014.2
137300     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
137400     PERFORM 37.                                                  SG2014.2
137500     IF TEST-CHECK EQUAL TO "GOOD"                                SG2014.2
137600         PERFORM PASS                                             SG2014.2
137700         GO TO SEG-WRITE-66.                                      SG2014.2
137800     MOVE SPACE TO COMPUTED-A.                                    SG2014.2
137900     MOVE "GOOD" TO CORRECT-A.                                    SG2014.2
138000     PERFORM FAIL.                                                SG2014.2
138100     GO TO SEG-WRITE-66.                                          SG2014.2
138200 SEG-DELETE-66.                                                   SG2014.2
138300     PERFORM DE-LETE.                                             SG2014.2
138400 SEG-WRITE-66.                                                    SG2014.2
138500     MOVE "SEG-TEST-66 " TO PAR-NAME.                             SG2014.2
138600     PERFORM PRINT-DETAIL.                                        SG2014.2
138700 SEG-TEST-67.                                                     SG2014.2
138800     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
138900     PERFORM 38.                                                  SG2014.2
139000     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
139100     PERFORM 38.                                                  SG2014.2
139200     IF TEST-CHECK EQUAL TO "GOOD"                                SG2014.2
139300         PERFORM PASS                                             SG2014.2
139400         GO TO SEG-WRITE-67.                                      SG2014.2
139500     MOVE SPACE TO COMPUTED-A.                                    SG2014.2
139600     MOVE "GOOD" TO CORRECT-A.                                    SG2014.2
139700     PERFORM FAIL.                                                SG2014.2
139800     GO TO SEG-WRITE-67.                                          SG2014.2
139900 SEG-DELETE-67.                                                   SG2014.2
140000     PERFORM DE-LETE.                                             SG2014.2
140100 SEG-WRITE-67.                                                    SG2014.2
140200     MOVE "SEG-TEST-67 " TO PAR-NAME.                             SG2014.2
140300     PERFORM PRINT-DETAIL.                                        SG2014.2
140400 SEG-TEST-68.                                                     SG2014.2
140500     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
140600     PERFORM 39.                                                  SG2014.2
140700     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
140800     PERFORM 39.                                                  SG2014.2
140900     IF TEST-CHECK EQUAL TO "GOOD"                                SG2014.2
141000         PERFORM PASS                                             SG2014.2
141100         GO TO SEG-WRITE-68.                                      SG2014.2
141200     MOVE SPACE TO COMPUTED-A.                                    SG2014.2
141300     MOVE "GOOD" TO CORRECT-A.                                    SG2014.2
141400     PERFORM FAIL.                                                SG2014.2
141500     GO TO SEG-WRITE-68.                                          SG2014.2
141600 SEG-DELETE-68.                                                   SG2014.2
141700     PERFORM DE-LETE.                                             SG2014.2
141800 SEG-WRITE-68.                                                    SG2014.2
141900     MOVE "SEG-TEST-68 " TO PAR-NAME.                             SG2014.2
142000     PERFORM PRINT-DETAIL.                                        SG2014.2
142100 SEG-TEST-69.                                                     SG2014.2
142200     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
142300     PERFORM 40.                                                  SG2014.2
142400     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
142500     PERFORM 40.                                                  SG2014.2
142600     IF TEST-CHECK EQUAL TO "GOOD"                                SG2014.2
142700         PERFORM PASS                                             SG2014.2
142800         GO TO SEG-WRITE-69.                                      SG2014.2
142900     MOVE SPACE TO COMPUTED-A.                                    SG2014.2
143000     MOVE "GOOD" TO CORRECT-A.                                    SG2014.2
143100     PERFORM FAIL.                                                SG2014.2
143200     GO TO SEG-WRITE-69.                                          SG2014.2
143300 SEG-DELETE-69.                                                   SG2014.2
143400     PERFORM DE-LETE.                                             SG2014.2
143500 SEG-WRITE-69.                                                    SG2014.2
143600     MOVE "SEG-TEST-69 " TO PAR-NAME.                             SG2014.2
143700     PERFORM PRINT-DETAIL.                                        SG2014.2
143800 SEG-TEST-70.                                                     SG2014.2
143900     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
144000     PERFORM 41.                                                  SG2014.2
144100     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
144200     PERFORM 41.                                                  SG2014.2
144300     IF TEST-CHECK EQUAL TO "GOOD"                                SG2014.2
144400         PERFORM PASS                                             SG2014.2
144500         GO TO SEG-WRITE-70.                                      SG2014.2
144600     MOVE SPACE TO COMPUTED-A.                                    SG2014.2
144700     MOVE "GOOD" TO CORRECT-A.                                    SG2014.2
144800     PERFORM FAIL.                                                SG2014.2
144900     GO TO SEG-WRITE-70.                                          SG2014.2
145000 SEG-DELETE-70.                                                   SG2014.2
145100     PERFORM DE-LETE.                                             SG2014.2
145200 SEG-WRITE-70.                                                    SG2014.2
145300     MOVE "SEG-TEST-70 " TO PAR-NAME.                             SG2014.2
145400     PERFORM PRINT-DETAIL.                                        SG2014.2
145500 SEG-TEST-71.                                                     SG2014.2
145600     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
145700     PERFORM 42.                                                  SG2014.2
145800     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
145900     PERFORM 42.                                                  SG2014.2
146000     IF TEST-CHECK EQUAL TO "GOOD"                                SG2014.2
146100         PERFORM PASS                                             SG2014.2
146200         GO TO SEG-WRITE-71.                                      SG2014.2
146300     MOVE SPACE TO COMPUTED-A.                                    SG2014.2
146400     MOVE "GOOD" TO CORRECT-A.                                    SG2014.2
146500     PERFORM FAIL.                                                SG2014.2
146600     GO TO SEG-WRITE-71.                                          SG2014.2
146700 SEG-DELETE-71.                                                   SG2014.2
146800     PERFORM DE-LETE.                                             SG2014.2
146900 SEG-WRITE-71.                                                    SG2014.2
147000     MOVE "SEG-TEST-71 " TO PAR-NAME.                             SG2014.2
147100     PERFORM PRINT-DETAIL.                                        SG2014.2
147200 SEG-TEST-72.                                                     SG2014.2
147300     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
147400     PERFORM 43.                                                  SG2014.2
147500     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
147600     PERFORM 43.                                                  SG2014.2
147700     IF TEST-CHECK EQUAL TO "GOOD"                                SG2014.2
147800         PERFORM PASS                                             SG2014.2
147900         GO TO SEG-WRITE-72.                                      SG2014.2
148000     MOVE SPACE TO COMPUTED-A.                                    SG2014.2
148100     MOVE "GOOD" TO CORRECT-A.                                    SG2014.2
148200     PERFORM FAIL.                                                SG2014.2
148300     GO TO SEG-WRITE-72.                                          SG2014.2
148400 SEG-DELETE-72.                                                   SG2014.2
148500     PERFORM DE-LETE.                                             SG2014.2
148600 SEG-WRITE-72.                                                    SG2014.2
148700     MOVE "SEG-TEST-72 " TO PAR-NAME.                             SG2014.2
148800     PERFORM PRINT-DETAIL.                                        SG2014.2
148900 SEG-TEST-73.                                                     SG2014.2
149000     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
149100     PERFORM 44.                                                  SG2014.2
149200     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
149300     PERFORM 44.                                                  SG2014.2
149400     IF TEST-CHECK EQUAL TO "GOOD"                                SG2014.2
149500         PERFORM PASS                                             SG2014.2
149600         GO TO SEG-WRITE-73.                                      SG2014.2
149700     MOVE SPACE TO COMPUTED-A.                                    SG2014.2
149800     MOVE "GOOD" TO CORRECT-A.                                    SG2014.2
149900     PERFORM FAIL.                                                SG2014.2
150000     GO TO SEG-WRITE-73.                                          SG2014.2
150100 SEG-DELETE-73.                                                   SG2014.2
150200     PERFORM DE-LETE.                                             SG2014.2
150300 SEG-WRITE-73.                                                    SG2014.2
150400     MOVE "SEG-TEST-73 " TO PAR-NAME.                             SG2014.2
150500     PERFORM PRINT-DETAIL.                                        SG2014.2
150600 SECOND-HALF SECTION 50.                                          SG2014.2
150700 SEG-TEST-74.                                                     SG2014.2
150800     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
150900     PERFORM 01.                                                  SG2014.2
151000     IF TEST-CHECK EQUAL TO "GOOD"                                SG2014.2
151100         PERFORM PASS                                             SG2014.2
151200         GO TO SEG-WRITE-74.                                      SG2014.2
151300     MOVE SPACE TO COMPUTED-A.                                    SG2014.2
151400     MOVE "GOOD" TO CORRECT-A.                                    SG2014.2
151500     PERFORM FAIL.                                                SG2014.2
151600     GO TO SEG-WRITE-74.                                          SG2014.2
151700 SEG-DELETE-74.                                                   SG2014.2
151800     PERFORM DE-LETE.                                             SG2014.2
151900 SEG-WRITE-74.                                                    SG2014.2
152000     MOVE "SEG-TEST-74 " TO PAR-NAME.                             SG2014.2
152100     PERFORM PRINT-DETAIL.                                        SG2014.2
152200 SEG-TEST-75.                                                     SG2014.2
152300     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
152400     PERFORM 02.                                                  SG2014.2
152500     IF TEST-CHECK EQUAL TO "GOOD"                                SG2014.2
152600         PERFORM PASS                                             SG2014.2
152700         GO TO SEG-WRITE-75.                                      SG2014.2
152800     MOVE SPACE TO COMPUTED-A.                                    SG2014.2
152900     MOVE "GOOD" TO CORRECT-A.                                    SG2014.2
153000     PERFORM FAIL.                                                SG2014.2
153100     GO TO SEG-WRITE-75.                                          SG2014.2
153200 SEG-DELETE-75.                                                   SG2014.2
153300     PERFORM DE-LETE.                                             SG2014.2
153400 SEG-WRITE-75.                                                    SG2014.2
153500     MOVE "SEG-TEST-75 " TO PAR-NAME.                             SG2014.2
153600     PERFORM PRINT-DETAIL.                                        SG2014.2
153700 SEG-TEST-76.                                                     SG2014.2
153800     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
153900     PERFORM 03.                                                  SG2014.2
154000     IF TEST-CHECK EQUAL TO "GOOD"                                SG2014.2
154100         PERFORM PASS                                             SG2014.2
154200         GO TO SEG-WRITE-76.                                      SG2014.2
154300     MOVE SPACE TO COMPUTED-A.                                    SG2014.2
154400     MOVE "GOOD" TO CORRECT-A.                                    SG2014.2
154500     PERFORM FAIL.                                                SG2014.2
154600     GO TO SEG-WRITE-76.                                          SG2014.2
154700 SEG-DELETE-76.                                                   SG2014.2
154800     PERFORM DE-LETE.                                             SG2014.2
154900 SEG-WRITE-76.                                                    SG2014.2
155000     MOVE "SEG-TEST-76 " TO PAR-NAME.                             SG2014.2
155100     PERFORM PRINT-DETAIL.                                        SG2014.2
155200 SEG-TEST-77.                                                     SG2014.2
155300     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
155400     PERFORM 04.                                                  SG2014.2
155500     IF TEST-CHECK EQUAL TO "GOOD"                                SG2014.2
155600         PERFORM PASS                                             SG2014.2
155700         GO TO SEG-WRITE-77.                                      SG2014.2
155800     MOVE SPACE TO COMPUTED-A.                                    SG2014.2
155900     MOVE "GOOD" TO CORRECT-A.                                    SG2014.2
156000     PERFORM FAIL.                                                SG2014.2
156100     GO TO SEG-WRITE-77.                                          SG2014.2
156200 SEG-DELETE-77.                                                   SG2014.2
156300     PERFORM DE-LETE.                                             SG2014.2
156400 SEG-WRITE-77.                                                    SG2014.2
156500     MOVE "SEG-TEST-77 " TO PAR-NAME.                             SG2014.2
156600     PERFORM PRINT-DETAIL.                                        SG2014.2
156700 SEG-TEST-78.                                                     SG2014.2
156800     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
156900     PERFORM 05.                                                  SG2014.2
157000     IF TEST-CHECK EQUAL TO "GOOD"                                SG2014.2
157100         PERFORM PASS                                             SG2014.2
157200         GO TO SEG-WRITE-78.                                      SG2014.2
157300     MOVE SPACE TO COMPUTED-A.                                    SG2014.2
157400     MOVE "GOOD" TO CORRECT-A.                                    SG2014.2
157500     PERFORM FAIL.                                                SG2014.2
157600     GO TO SEG-WRITE-78.                                          SG2014.2
157700 SEG-DELETE-78.                                                   SG2014.2
157800     PERFORM DE-LETE.                                             SG2014.2
157900 SEG-WRITE-78.                                                    SG2014.2
158000     MOVE "SEG-TEST-78 " TO PAR-NAME.                             SG2014.2
158100     PERFORM PRINT-DETAIL.                                        SG2014.2
158200 SEG-TEST-79.                                                     SG2014.2
158300     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
158400     PERFORM 06.                                                  SG2014.2
158500     IF TEST-CHECK EQUAL TO "GOOD"                                SG2014.2
158600         PERFORM PASS                                             SG2014.2
158700         GO TO SEG-WRITE-79.                                      SG2014.2
158800     MOVE SPACE TO COMPUTED-A.                                    SG2014.2
158900     MOVE "GOOD" TO CORRECT-A.                                    SG2014.2
159000     PERFORM FAIL.                                                SG2014.2
159100     GO TO SEG-WRITE-79.                                          SG2014.2
159200 SEG-DELETE-79.                                                   SG2014.2
159300     PERFORM DE-LETE.                                             SG2014.2
159400 SEG-WRITE-79.                                                    SG2014.2
159500     MOVE "SEG-TEST-79 " TO PAR-NAME.                             SG2014.2
159600     PERFORM PRINT-DETAIL.                                        SG2014.2
159700     GO TO    CLOSE-FILES.                                        SG2014.2
159800 00  SECTION 00.                                                  SG2014.2
159900 PARA-00.                                                         SG2014.2
160000     MOVE "GOOD" TO TEST-CHECK.                                   SG2014.2
160100 01 SECTION 01.                                                   SG2014.2
160200 PARA-01.                                                         SG2014.2
160300     MOVE "GOOD" TO TEST-CHECK.                                   SG2014.2
160400 02 SECTION 02.                                                   SG2014.2
160500 PARA-02.                                                         SG2014.2
160600     MOVE "GOOD" TO TEST-CHECK.                                   SG2014.2
160700 03 SECTION 03.                                                   SG2014.2
160800 PARA-03.                                                         SG2014.2
160900     MOVE "GOOD" TO TEST-CHECK.                                   SG2014.2
161000 04 SECTION 04.                                                   SG2014.2
161100 PARA-04.                                                         SG2014.2
161200     MOVE "GOOD" TO TEST-CHECK.                                   SG2014.2
161300 05 SECTION 05.                                                   SG2014.2
161400 PARA-05.                                                         SG2014.2
161500     MOVE "GOOD" TO TEST-CHECK.                                   SG2014.2
161600 06 SECTION 06.                                                   SG2014.2
161700 PARA-06.                                                         SG2014.2
161800     MOVE "GOOD" TO TEST-CHECK.                                   SG2014.2
161900 30 SECTION 30.                                                   SG2014.2
162000 PARA-30.                                                         SG2014.2
162100     GO TO PARA-30C.                                              SG2014.2
162200 PARA-30A.                                                        SG2014.2
162300     MOVE "GOOD" TO TEST-CHECK.                                   SG2014.2
162400 PARA-30B.                                                        SG2014.2
162500     GO TO PARA-30D.                                              SG2014.2
162600 PARA-30C.                                                        SG2014.2
162700     ALTER PARA-30 TO PROCEED TO PARA-30A.                        SG2014.2
162800 PARA-30D.                                                        SG2014.2
162900     EXIT.                                                        SG2014.2
163000 31 SECTION 31.                                                   SG2014.2
163100 PARA-31.                                                         SG2014.2
163200     GO TO PARA-31C.                                              SG2014.2
163300 PARA-31A.                                                        SG2014.2
163400     MOVE "GOOD" TO TEST-CHECK.                                   SG2014.2
163500 PARA-31B.                                                        SG2014.2
163600     GO TO PARA-31D.                                              SG2014.2
163700 PARA-31C.                                                        SG2014.2
163800     ALTER PARA-31 TO PROCEED TO PARA-31A.                        SG2014.2
163900 PARA-31D.                                                        SG2014.2
164000     EXIT.                                                        SG2014.2
164100 32 SECTION 32.                                                   SG2014.2
164200 PARA-32.                                                         SG2014.2
164300     GO TO PARA-32C.                                              SG2014.2
164400 PARA-32A.                                                        SG2014.2
164500     MOVE "GOOD" TO TEST-CHECK.                                   SG2014.2
164600 PARA-32B.                                                        SG2014.2
164700     GO TO PARA-32D.                                              SG2014.2
164800 PARA-32C.                                                        SG2014.2
164900     ALTER PARA-32 TO PROCEED TO PARA-32A.                        SG2014.2
165000 PARA-32D.                                                        SG2014.2
165100     EXIT.                                                        SG2014.2
165200 33 SECTION 33.                                                   SG2014.2
165300 PARA-33.                                                         SG2014.2
165400     GO TO PARA-33C.                                              SG2014.2
165500 PARA-33A.                                                        SG2014.2
165600     MOVE "GOOD" TO TEST-CHECK.                                   SG2014.2
165700 PARA-33B.                                                        SG2014.2
165800     GO TO PARA-33D.                                              SG2014.2
165900 PARA-33C.                                                        SG2014.2
166000     ALTER PARA-33 TO PROCEED TO PARA-33A.                        SG2014.2
166100 PARA-33D.                                                        SG2014.2
166200     EXIT.                                                        SG2014.2
166300 34 SECTION 34.                                                   SG2014.2
166400 PARA-34.                                                         SG2014.2
166500     GO TO PARA-34C.                                              SG2014.2
166600 PARA-34A.                                                        SG2014.2
166700     MOVE "GOOD" TO TEST-CHECK.                                   SG2014.2
166800 PARA-34B.                                                        SG2014.2
166900     GO TO PARA-34D.                                              SG2014.2
167000 PARA-34C.                                                        SG2014.2
167100     ALTER PARA-34 TO PROCEED TO PARA-34A.                        SG2014.2
167200 PARA-34D.                                                        SG2014.2
167300     EXIT.                                                        SG2014.2
167400 35 SECTION 35.                                                   SG2014.2
167500 PARA-35.                                                         SG2014.2
167600     GO TO PARA-35C.                                              SG2014.2
167700 PARA-35A.                                                        SG2014.2
167800     MOVE "GOOD" TO TEST-CHECK.                                   SG2014.2
167900 PARA-35B.                                                        SG2014.2
168000     GO TO PARA-35D.                                              SG2014.2
168100 PARA-35C.                                                        SG2014.2
168200     ALTER PARA-35 TO PROCEED TO PARA-35A.                        SG2014.2
168300 PARA-35D.                                                        SG2014.2
168400     EXIT.                                                        SG2014.2
168500 36 SECTION 36.                                                   SG2014.2
168600 PARA-36.                                                         SG2014.2
168700     GO TO PARA-36C.                                              SG2014.2
168800 PARA-36A.                                                        SG2014.2
168900     MOVE "GOOD" TO TEST-CHECK.                                   SG2014.2
169000 PARA-36B.                                                        SG2014.2
169100     GO TO PARA-36D.                                              SG2014.2
169200 PARA-36C.                                                        SG2014.2
169300     ALTER PARA-36 TO PROCEED TO PARA-36A.                        SG2014.2
169400 PARA-36D.                                                        SG2014.2
169500     EXIT.                                                        SG2014.2
169600 37 SECTION 37.                                                   SG2014.2
169700 PARA-37.                                                         SG2014.2
169800     GO TO PARA-37C.                                              SG2014.2
169900 PARA-37A.                                                        SG2014.2
170000     MOVE "GOOD" TO TEST-CHECK.                                   SG2014.2
170100 PARA-37B.                                                        SG2014.2
170200     GO TO PARA-37D.                                              SG2014.2
170300 PARA-37C.                                                        SG2014.2
170400     ALTER PARA-37 TO PROCEED TO PARA-37A.                        SG2014.2
170500 PARA-37D.                                                        SG2014.2
170600     EXIT.                                                        SG2014.2
170700 38 SECTION 38.                                                   SG2014.2
170800 PARA-38.                                                         SG2014.2
170900     GO TO PARA-38C.                                              SG2014.2
171000 PARA-38A.                                                        SG2014.2
171100     MOVE "GOOD" TO TEST-CHECK.                                   SG2014.2
171200 PARA-38B.                                                        SG2014.2
171300     GO TO PARA-38D.                                              SG2014.2
171400 PARA-38C.                                                        SG2014.2
171500     ALTER PARA-38 TO PROCEED TO PARA-38A.                        SG2014.2
171600 PARA-38D.                                                        SG2014.2
171700     EXIT.                                                        SG2014.2
171800 39 SECTION 39.                                                   SG2014.2
171900 PARA-39.                                                         SG2014.2
172000     GO TO PARA-39C.                                              SG2014.2
172100 PARA-39A.                                                        SG2014.2
172200     MOVE "GOOD" TO TEST-CHECK.                                   SG2014.2
172300 PARA-39B.                                                        SG2014.2
172400     GO TO PARA-39D.                                              SG2014.2
172500 PARA-39C.                                                        SG2014.2
172600     ALTER PARA-39 TO PROCEED TO PARA-39A.                        SG2014.2
172700 PARA-39D.                                                        SG2014.2
172800     EXIT.                                                        SG2014.2
172900 40 SECTION 40.                                                   SG2014.2
173000 PARA-40.                                                         SG2014.2
173100     GO TO PARA-40C.                                              SG2014.2
173200 PARA-40A.                                                        SG2014.2
173300     MOVE "GOOD" TO TEST-CHECK.                                   SG2014.2
173400 PARA-40B.                                                        SG2014.2
173500     GO TO PARA-40D.                                              SG2014.2
173600 PARA-40C.                                                        SG2014.2
173700     ALTER PARA-40 TO PROCEED TO PARA-40A.                        SG2014.2
173800 PARA-40D.                                                        SG2014.2
173900     EXIT.                                                        SG2014.2
174000 41 SECTION 41.                                                   SG2014.2
174100 PARA-41.                                                         SG2014.2
174200     GO TO PARA-41C.                                              SG2014.2
174300 PARA-41A.                                                        SG2014.2
174400     MOVE "GOOD" TO TEST-CHECK.                                   SG2014.2
174500 PARA-41B.                                                        SG2014.2
174600     GO TO PARA-41D.                                              SG2014.2
174700 PARA-41C.                                                        SG2014.2
174800     ALTER PARA-41 TO PROCEED TO PARA-41A.                        SG2014.2
174900 PARA-41D.                                                        SG2014.2
175000     EXIT.                                                        SG2014.2
175100 42 SECTION 42.                                                   SG2014.2
175200 PARA-42.                                                         SG2014.2
175300     GO TO PARA-42C.                                              SG2014.2
175400 PARA-42A.                                                        SG2014.2
175500     MOVE "GOOD" TO TEST-CHECK.                                   SG2014.2
175600 PARA-42B.                                                        SG2014.2
175700     GO TO PARA-42D.                                              SG2014.2
175800 PARA-42C.                                                        SG2014.2
175900     ALTER PARA-42 TO PROCEED TO PARA-42A.                        SG2014.2
176000 PARA-42D.                                                        SG2014.2
176100     EXIT.                                                        SG2014.2
176200 43 SECTION 43.                                                   SG2014.2
176300 PARA-43.                                                         SG2014.2
176400     GO TO PARA-43C.                                              SG2014.2
176500 PARA-43A.                                                        SG2014.2
176600     MOVE "GOOD" TO TEST-CHECK.                                   SG2014.2
176700 PARA-43B.                                                        SG2014.2
176800     GO TO PARA-43D.                                              SG2014.2
176900 PARA-43C.                                                        SG2014.2
177000     ALTER PARA-43 TO PROCEED TO PARA-43A.                        SG2014.2
177100 PARA-43D.                                                        SG2014.2
177200     EXIT.                                                        SG2014.2
177300 44 SECTION 44.                                                   SG2014.2
177400 PARA-44.                                                         SG2014.2
177500     GO TO PARA-44C.                                              SG2014.2
177600 PARA-44A.                                                        SG2014.2
177700     MOVE "GOOD" TO TEST-CHECK.                                   SG2014.2
177800 PARA-44B.                                                        SG2014.2
177900     GO TO PARA-44D.                                              SG2014.2
178000 PARA-44C.                                                        SG2014.2
178100     ALTER PARA-44 TO PROCEED TO PARA-44A.                        SG2014.2
178200 PARA-44D.                                                        SG2014.2
178300     EXIT.                                                        SG2014.2
178400 45 SECTION 45.                                                   SG2014.2
178500 PARA-45.                                                         SG2014.2
178600     GO TO PARA-45C.                                              SG2014.2
178700 PARA-45A.                                                        SG2014.2
178800     MOVE "GOOD" TO TEST-CHECK.                                   SG2014.2
178900 PARA-45B.                                                        SG2014.2
179000     GO TO PARA-45D.                                              SG2014.2
179100 PARA-45C.                                                        SG2014.2
179200     ALTER PARA-45 TO PROCEED TO PARA-45A.                        SG2014.2
179300 PARA-45D.                                                        SG2014.2
179400     EXIT.                                                        SG2014.2
179500 46 SECTION 46.                                                   SG2014.2
179600 PARA-46.                                                         SG2014.2
179700     GO TO PARA-46C.                                              SG2014.2
179800 PARA-46A.                                                        SG2014.2
179900     MOVE "GOOD" TO TEST-CHECK.                                   SG2014.2
180000 PARA-46B.                                                        SG2014.2
180100     GO TO PARA-46D.                                              SG2014.2
180200 PARA-46C.                                                        SG2014.2
180300     ALTER PARA-46 TO PROCEED TO PARA-46A.                        SG2014.2
180400 PARA-46D.                                                        SG2014.2
180500     EXIT.                                                        SG2014.2
180600 47 SECTION 47.                                                   SG2014.2
180700 PARA-47.                                                         SG2014.2
180800     GO TO PARA-47C.                                              SG2014.2
180900 PARA-47A.                                                        SG2014.2
181000     MOVE "GOOD" TO TEST-CHECK.                                   SG2014.2
181100 PARA-47B.                                                        SG2014.2
181200     GO TO PARA-47D.                                              SG2014.2
181300 PARA-47C.                                                        SG2014.2
181400     ALTER PARA-47 TO PROCEED TO PARA-47A.                        SG2014.2
181500 PARA-47D.                                                        SG2014.2
181600     EXIT.                                                        SG2014.2
181700 48 SECTION 48.                                                   SG2014.2
181800 PARA-48.                                                         SG2014.2
181900     GO TO PARA-48C.                                              SG2014.2
182000 PARA-48A.                                                        SG2014.2
182100     MOVE "GOOD" TO TEST-CHECK.                                   SG2014.2
182200 PARA-48B.                                                        SG2014.2
182300     GO TO PARA-48D.                                              SG2014.2
182400 PARA-48C.                                                        SG2014.2
182500     ALTER PARA-48 TO PROCEED TO PARA-48A.                        SG2014.2
182600 PARA-48D.                                                        SG2014.2
182700     EXIT.                                                        SG2014.2
182800 49 SECTION 49.                                                   SG2014.2
182900 PARA-49.                                                         SG2014.2
183000     GO TO PARA-49C.                                              SG2014.2
183100 PARA-49A.                                                        SG2014.2
183200     MOVE "GOOD" TO TEST-CHECK.                                   SG2014.2
183300 PARA-49B.                                                        SG2014.2
183400     GO TO PARA-49D.                                              SG2014.2
183500 PARA-49C.                                                        SG2014.2
183600     ALTER PARA-49 TO PROCEED TO PARA-49A.                        SG2014.2
183700 PARA-49D.                                                        SG2014.2
183800     EXIT.                                                        SG2014.2
183900 50  SECTION 50.                                                  SG2014.2
184000 PARA-50.                                                         SG2014.2
184100     GO TO PARA-50A.                                              SG2014.2
184200 PARA-50A.                                                        SG2014.2
184300     MOVE "GOOD" TO TEST-CHECK.                                   SG2014.2
184400 PARA-50B.                                                        SG2014.2
184500     ALTER PARA-50 TO PROCEED TO PARA-50C.                        SG2014.2
184600 PARA-50C.                                                        SG2014.2
184700     EXIT.                                                        SG2014.2
184800 51  SECTION 51.                                                  SG2014.2
184900 PARA-51.                                                         SG2014.2
185000     GO TO PARA-51A.                                              SG2014.2
185100 PARA-51A.                                                        SG2014.2
185200     MOVE "GOOD" TO TEST-CHECK.                                   SG2014.2
185300 PARA-51B.                                                        SG2014.2
185400     ALTER PARA-51 TO PROCEED TO PARA-51C.                        SG2014.2
185500 PARA-51C.                                                        SG2014.2
185600     EXIT.                                                        SG2014.2
185700 52  SECTION 52.                                                  SG2014.2
185800 PARA-52.                                                         SG2014.2
185900     GO TO PARA-52A.                                              SG2014.2
186000 PARA-52A.                                                        SG2014.2
186100     MOVE "GOOD" TO TEST-CHECK.                                   SG2014.2
186200 PARA-52B.                                                        SG2014.2
186300     ALTER PARA-52 TO PROCEED TO PARA-52C.                        SG2014.2
186400 PARA-52C.                                                        SG2014.2
186500     EXIT.                                                        SG2014.2
186600 53  SECTION 53.                                                  SG2014.2
186700 PARA-53.                                                         SG2014.2
186800     GO TO PARA-53A.                                              SG2014.2
186900 PARA-53A.                                                        SG2014.2
187000     MOVE "GOOD" TO TEST-CHECK.                                   SG2014.2
187100 PARA-53B.                                                        SG2014.2
187200     ALTER PARA-53 TO PROCEED TO PARA-53C.                        SG2014.2
187300 PARA-53C.                                                        SG2014.2
187400     EXIT.                                                        SG2014.2
187500 54  SECTION 54.                                                  SG2014.2
187600 PARA-54.                                                         SG2014.2
187700     GO TO PARA-54A.                                              SG2014.2
187800 PARA-54A.                                                        SG2014.2
187900     MOVE "GOOD" TO TEST-CHECK.                                   SG2014.2
188000 PARA-54B.                                                        SG2014.2
188100     ALTER PARA-54 TO PROCEED TO PARA-54C.                        SG2014.2
188200 PARA-54C.                                                        SG2014.2
188300     EXIT.                                                        SG2014.2
188400 55  SECTION 55.                                                  SG2014.2
188500 PARA-55.                                                         SG2014.2
188600     GO TO PARA-55A.                                              SG2014.2
188700 PARA-55A.                                                        SG2014.2
188800     MOVE "GOOD" TO TEST-CHECK.                                   SG2014.2
188900 PARA-55B.                                                        SG2014.2
189000     ALTER PARA-55 TO PROCEED TO PARA-55C.                        SG2014.2
189100 PARA-55C.                                                        SG2014.2
189200     EXIT.                                                        SG2014.2
189300 56  SECTION 56.                                                  SG2014.2
189400 PARA-56.                                                         SG2014.2
189500     GO TO PARA-56A.                                              SG2014.2
189600 PARA-56A.                                                        SG2014.2
189700     MOVE "GOOD" TO TEST-CHECK.                                   SG2014.2
189800 PARA-56B.                                                        SG2014.2
189900     ALTER PARA-56 TO PROCEED TO PARA-56C.                        SG2014.2
190000 PARA-56C.                                                        SG2014.2
190100     EXIT.                                                        SG2014.2
190200 57  SECTION 57.                                                  SG2014.2
190300 PARA-57.                                                         SG2014.2
190400     GO TO PARA-57A.                                              SG2014.2
190500 PARA-57A.                                                        SG2014.2
190600     MOVE "GOOD" TO TEST-CHECK.                                   SG2014.2
190700 PARA-57B.                                                        SG2014.2
190800     ALTER PARA-57 TO PROCEED TO PARA-57C.                        SG2014.2
190900 PARA-57C.                                                        SG2014.2
191000     EXIT.                                                        SG2014.2
191100 58  SECTION 58.                                                  SG2014.2
191200 PARA-58.                                                         SG2014.2
191300     GO TO PARA-58A.                                              SG2014.2
191400 PARA-58A.                                                        SG2014.2
191500     MOVE "GOOD" TO TEST-CHECK.                                   SG2014.2
191600 PARA-58B.                                                        SG2014.2
191700     ALTER PARA-58 TO PROCEED TO PARA-58C.                        SG2014.2
191800 PARA-58C.                                                        SG2014.2
191900     EXIT.                                                        SG2014.2
192000 59  SECTION 59.                                                  SG2014.2
192100 PARA-59.                                                         SG2014.2
192200     GO TO PARA-59A.                                              SG2014.2
192300 PARA-59A.                                                        SG2014.2
192400     MOVE "GOOD" TO TEST-CHECK.                                   SG2014.2
192500 PARA-59B.                                                        SG2014.2
192600     ALTER PARA-59 TO PROCEED TO PARA-59C.                        SG2014.2
192700 PARA-59C.                                                        SG2014.2
192800     EXIT.                                                        SG2014.2
192900 60  SECTION 60.                                                  SG2014.2
193000 PARA-60.                                                         SG2014.2
193100     GO TO PARA-60A.                                              SG2014.2
193200 PARA-60A.                                                        SG2014.2
193300     MOVE "GOOD" TO TEST-CHECK.                                   SG2014.2
193400 PARA-60B.                                                        SG2014.2
193500     ALTER PARA-60 TO PROCEED TO PARA-60C.                        SG2014.2
193600 PARA-60C.                                                        SG2014.2
193700     EXIT.                                                        SG2014.2
193800 99  SECTION 99.                                                  SG2014.2
193900 PARA-99.                                                         SG2014.2
194000     GO TO PARA-99A.                                              SG2014.2
194100 PARA-99A.                                                        SG2014.2
194200     ALTER PARA-99 TO PARA-99B.                                   SG2014.2
194300     GO TO PARA-99C.                                              SG2014.2
194400 PARA-99B.                                                        SG2014.2
194500     MOVE SPACE TO TEST-CHECK.                                    SG2014.2
194600     GO TO PARA-99D.                                              SG2014.2
194700 PARA-99C.                                                        SG2014.2
194800     MOVE "GOOD" TO TEST-CHECK.                                   SG2014.2
194900 PARA-99D.                                                        SG2014.2
195000     EXIT.                                                        SG2014.2
      *END-OF,SG201A                                                                  
