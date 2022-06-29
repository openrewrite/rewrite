      *HEADER,COBOL,SG102A                                                            
000100 IDENTIFICATION DIVISION.                                         SG1024.2
000200 PROGRAM-ID.                                                      SG1024.2
000300     SG102A.                                                      SG1024.2
000400 AUTHOR.                                                          SG1024.2
000500     FEDERAL COMPILER TESTING CENTER.                             SG1024.2
000600 INSTALLATION.                                                    SG1024.2
000700     GENERAL SERVICES ADMINISTRATION                              SG1024.2
000800*     AUTOMATED DATA AND TELECOMMUNICATION SERVICE.                SG1024.2
000900     SOFTWARE DEVELOPMENT OFFICE.                                 SG1024.2
001000     5203 LEESBURG PIKE  SUITE 1100                               SG1024.2
001100     FALLS CHURCH VIRGINIA 22041.                                 SG1024.2
001200                                                                  SG1024.2
001300     PHONE   (703) 756-6153                                       SG1024.2
001400                                                                  SG1024.2
001500     " HIGH       ".                                              SG1024.2
001600 DATE-WRITTEN.                                                    SG1024.2
001700     CCVS-74 VERSION 4.0 - 1980 JULY 1.                           SG1024.2
001800     CREATION DATE     /    VALIDATION DATE                       SG1024.2
001900     "4.2 ".                                                      SG1024.2
002000 SECURITY.                                                        SG1024.2
002100     NONE.                                                        SG1024.2
002200       THE FOLLOWING FEATURES ARE TESTED BY THIS PROGRAM ---      SG1024.2
002300         VARIOUS ALTER AND PERFORM STATEMENTS ARE EXERCISED       SG1024.2
002400         AND A DIRECTORY IS PREPARED IN EACH TEST TO TRACE        SG1024.2
002500         PROGRAM FLOW.                                            SG1024.2
002600                                                                  SG1024.2
002700 ENVIRONMENT DIVISION.                                            SG1024.2
002800 CONFIGURATION SECTION.                                           SG1024.2
002900 SOURCE-COMPUTER.                                                 SG1024.2
003000     XXXXX082.                                                    SG1024.2
003100 OBJECT-COMPUTER.                                                 SG1024.2
003200     XXXXX083.                                                    SG1024.2
003300 INPUT-OUTPUT SECTION.                                            SG1024.2
003400 FILE-CONTROL.                                                    SG1024.2
003500     SELECT PRINT-FILE ASSIGN TO                                  SG1024.2
003600     XXXXX055.                                                    SG1024.2
003700 DATA DIVISION.                                                   SG1024.2
003800 FILE SECTION.                                                    SG1024.2
003900 FD  PRINT-FILE                                                   SG1024.2
004000     LABEL RECORDS                                                SG1024.2
004100     XXXXX084                                                     SG1024.2
004200     DATA RECORD IS PRINT-REC DUMMY-RECORD.                       SG1024.2
004300 01  PRINT-REC PICTURE X(120).                                    SG1024.2
004400 01  DUMMY-RECORD PICTURE X(120).                                 SG1024.2
004500 WORKING-STORAGE SECTION.                                         SG1024.2
004600 77  SEG-CALC           PICTURE 9  VALUE 0.                       SG1024.2
004700 77  RANGE-SUB                    PICTURE 9  VALUE 0.             SG1024.2
004800 01  COMPUTED-RANGE.                                              SG1024.2
004900     02  RANGE-X OCCURS 7 TIMES  PICTURE X.                       SG1024.2
005000 01  TEST-RESULTS.                                                SG1024.2
005100     02 FILLER                    PICTURE X VALUE SPACE.          SG1024.2
005200     02 FEATURE                   PICTURE X(20) VALUE SPACE.      SG1024.2
005300     02 FILLER                    PICTURE X VALUE SPACE.          SG1024.2
005400     02 P-OR-F                    PICTURE X(5) VALUE SPACE.       SG1024.2
005500     02 FILLER                    PICTURE X  VALUE SPACE.         SG1024.2
005600     02  PAR-NAME.                                                SG1024.2
005700       03 FILLER PICTURE X(12) VALUE SPACE.                       SG1024.2
005800       03  PARDOT-X PICTURE X  VALUE SPACE.                       SG1024.2
005900       03 DOTVALUE PICTURE 99  VALUE ZERO.                        SG1024.2
006000       03 FILLER PIC X(5) VALUE SPACE.                            SG1024.2
006100     02 FILLER PIC X(10) VALUE SPACE.                             SG1024.2
006200     02 RE-MARK PIC X(61).                                        SG1024.2
006300 01  TEST-COMPUTED.                                               SG1024.2
006400     02 FILLER PIC X(30) VALUE SPACE.                             SG1024.2
006500     02 FILLER PIC X(17) VALUE "       COMPUTED=".                SG1024.2
006600     02 COMPUTED-X.                                               SG1024.2
006700     03 COMPUTED-A                PICTURE X(20) VALUE SPACE.      SG1024.2
006800     03 COMPUTED-N REDEFINES COMPUTED-A PICTURE -9(9).9(9).       SG1024.2
006900     03 COMPUTED-0V18 REDEFINES COMPUTED-A  PICTURE -.9(18).      SG1024.2
007000     03 COMPUTED-4V14 REDEFINES COMPUTED-A  PICTURE -9(4).9(14).  SG1024.2
007100     03 COMPUTED-14V4 REDEFINES COMPUTED-A  PICTURE -9(14).9(4).  SG1024.2
007200     03       CM-18V0 REDEFINES COMPUTED-A.                       SG1024.2
007300         04 COMPUTED-18V0                   PICTURE -9(18).       SG1024.2
007400         04 FILLER                          PICTURE X.            SG1024.2
007500     03 FILLER PIC X(50) VALUE SPACE.                             SG1024.2
007600 01  TEST-CORRECT.                                                SG1024.2
007700     02 FILLER PIC X(30) VALUE SPACE.                             SG1024.2
007800     02 FILLER PIC X(17) VALUE "       CORRECT =".                SG1024.2
007900     02 CORRECT-X.                                                SG1024.2
008000     03 CORRECT-A                 PICTURE X(20) VALUE SPACE.      SG1024.2
008100     03 CORRECT-N REDEFINES CORRECT-A PICTURE -9(9).9(9).         SG1024.2
008200     03 CORRECT-0V18 REDEFINES CORRECT-A    PICTURE -.9(18).      SG1024.2
008300     03 CORRECT-4V14 REDEFINES CORRECT-A    PICTURE -9(4).9(14).  SG1024.2
008400     03 CORRECT-14V4 REDEFINES CORRECT-A    PICTURE -9(14).9(4).  SG1024.2
008500     03      CR-18V0 REDEFINES CORRECT-A.                         SG1024.2
008600         04 CORRECT-18V0                    PICTURE -9(18).       SG1024.2
008700         04 FILLER                          PICTURE X.            SG1024.2
008800     03 FILLER PIC X(50) VALUE SPACE.                             SG1024.2
008900 01  CCVS-C-1.                                                    SG1024.2
009000     02 FILLER PICTURE IS X(99) VALUE IS " FEATURE              PASG1024.2
009100-    "SS  PARAGRAPH-NAME                                          SG1024.2
009200-    "        REMARKS".                                           SG1024.2
009300     02 FILLER PICTURE IS X(20) VALUE IS SPACE.                   SG1024.2
009400 01  CCVS-C-2.                                                    SG1024.2
009500     02 FILLER PICTURE IS X VALUE IS SPACE.                       SG1024.2
009600     02 FILLER PICTURE IS X(6) VALUE IS "TESTED".                 SG1024.2
009700     02 FILLER PICTURE IS X(15) VALUE IS SPACE.                   SG1024.2
009800     02 FILLER PICTURE IS X(4) VALUE IS "FAIL".                   SG1024.2
009900     02 FILLER PICTURE IS X(94) VALUE IS SPACE.                   SG1024.2
010000 01  REC-SKL-SUB PICTURE 9(2) VALUE ZERO.                         SG1024.2
010100 01  REC-CT PICTURE 99 VALUE ZERO.                                SG1024.2
010200 01  DELETE-CNT                   PICTURE 999  VALUE ZERO.        SG1024.2
010300 01  ERROR-COUNTER PICTURE IS 999 VALUE IS ZERO.                  SG1024.2
010400 01  INSPECT-COUNTER PIC 999 VALUE ZERO.                          SG1024.2
010500 01  PASS-COUNTER PIC 999 VALUE ZERO.                             SG1024.2
010600 01  TOTAL-ERROR PIC 999 VALUE ZERO.                              SG1024.2
010700 01  ERROR-HOLD PIC 999 VALUE ZERO.                               SG1024.2
010800 01  DUMMY-HOLD PIC X(120) VALUE SPACE.                           SG1024.2
010900 01  RECORD-COUNT PIC 9(5) VALUE ZERO.                            SG1024.2
011000 01  CCVS-H-1.                                                    SG1024.2
011100     02  FILLER   PICTURE X(27)  VALUE SPACE.                     SG1024.2
011200     02 FILLER PICTURE X(67) VALUE                                SG1024.2
011300     " FEDERAL COMPILER TESTING CENTER COBOL COMPILER VALIDATION  SG1024.2
011400-    " SYSTEM".                                                   SG1024.2
011500     02  FILLER     PICTURE X(26)  VALUE SPACE.                   SG1024.2
011600 01  CCVS-H-2.                                                    SG1024.2
011700     02 FILLER PICTURE X(52) VALUE IS                             SG1024.2
011800     "CCVS74 NCC  COPY, NOT FOR DISTRIBUTION.".                   SG1024.2
011900     02 FILLER PICTURE IS X(19) VALUE IS "TEST RESULTS SET-  ".   SG1024.2
012000     02 TEST-ID PICTURE IS X(9).                                  SG1024.2
012100     02 FILLER PICTURE IS X(40) VALUE IS SPACE.                   SG1024.2
012200 01  CCVS-H-3.                                                    SG1024.2
012300     02  FILLER PICTURE X(34) VALUE                               SG1024.2
012400     " FOR OFFICIAL USE ONLY    ".                                SG1024.2
012500     02  FILLER PICTURE X(58) VALUE                               SG1024.2
012600     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SG1024.2
012700     02  FILLER PICTURE X(28) VALUE                               SG1024.2
012800     "  COPYRIGHT   1974 ".                                       SG1024.2
012900 01  CCVS-E-1.                                                    SG1024.2
013000     02 FILLER PICTURE IS X(52) VALUE IS SPACE.                   SG1024.2
013100     02 FILLER PICTURE IS X(14) VALUE IS "END OF TEST-  ".        SG1024.2
013200     02 ID-AGAIN PICTURE IS X(9).                                 SG1024.2
013300     02 FILLER PICTURE X(45) VALUE IS                             SG1024.2
013400     " NTIS DISTRIBUTION COBOL 74".                               SG1024.2
013500 01  CCVS-E-2.                                                    SG1024.2
013600     02  FILLER                   PICTURE X(31)  VALUE            SG1024.2
013700     SPACE.                                                       SG1024.2
013800     02  FILLER                   PICTURE X(21)  VALUE SPACE.     SG1024.2
013900     02 CCVS-E-2-2.                                               SG1024.2
014000         03 ERROR-TOTAL PICTURE IS XXX VALUE IS SPACE.            SG1024.2
014100         03 FILLER PICTURE IS X VALUE IS SPACE.                   SG1024.2
014200         03 ENDER-DESC PIC X(44) VALUE "ERRORS ENCOUNTERED".      SG1024.2
014300 01  CCVS-E-3.                                                    SG1024.2
014400     02  FILLER PICTURE X(22) VALUE                               SG1024.2
014500     " FOR OFFICIAL USE ONLY".                                    SG1024.2
014600     02  FILLER PICTURE X(12) VALUE SPACE.                        SG1024.2
014700     02  FILLER PICTURE X(58) VALUE                               SG1024.2
014800     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SG1024.2
014900     02  FILLER PICTURE X(13) VALUE SPACE.                        SG1024.2
015000     02 FILLER PIC X(15) VALUE " COPYRIGHT 1974".                 SG1024.2
015100 01  CCVS-E-4.                                                    SG1024.2
015200     02 CCVS-E-4-1 PIC XXX VALUE SPACE.                           SG1024.2
015300     02 FILLER PIC XXXX VALUE " OF ".                             SG1024.2
015400     02 CCVS-E-4-2 PIC XXX VALUE SPACE.                           SG1024.2
015500     02 FILLER PIC X(40) VALUE                                    SG1024.2
015600      "  TESTS WERE EXECUTED SUCCESSFULLY".                       SG1024.2
015700 01  XXINFO.                                                      SG1024.2
015800     02 FILLER PIC X(30) VALUE "        *** INFORMATION  ***".    SG1024.2
015900     02 INFO-TEXT.                                                SG1024.2
016000     04 FILLER PIC X(20) VALUE SPACE.                             SG1024.2
016100     04 XXCOMPUTED PIC X(20).                                     SG1024.2
016200     04 FILLER PIC X(5) VALUE SPACE.                              SG1024.2
016300     04 XXCORRECT PIC X(20).                                      SG1024.2
016400 01  HYPHEN-LINE.                                                 SG1024.2
016500     02 FILLER PICTURE IS X VALUE IS SPACE.                       SG1024.2
016600     02 FILLER PICTURE IS X(65) VALUE IS "************************SG1024.2
016700-    "*****************************************".                 SG1024.2
016800     02 FILLER PICTURE IS X(54) VALUE IS "************************SG1024.2
016900-    "******************************".                            SG1024.2
017000 01  CCVS-PGM-ID PIC X(6) VALUE                                   SG1024.2
017100     "SG102A".                                                    SG1024.2
017200 PROCEDURE DIVISION.                                              SG1024.2
017300 SECT-SG-02-001 SECTION 50.                                       SG1024.2
017400 SG-02-001.                                                       SG1024.2
017500     PERFORM  CCVS1.                                              SG1024.2
017600     GO TO    SEG-TEST-1.                                         SG1024.2
017700 CCVS1 SECTION.                                                   SG1024.2
017800 OPEN-FILES.                                                      SG1024.2
017900     OPEN     OUTPUT PRINT-FILE.                                  SG1024.2
018000     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   SG1024.2
018100     MOVE    SPACE TO TEST-RESULTS.                               SG1024.2
018200     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             SG1024.2
018300     GO TO CCVS1-EXIT.                                            SG1024.2
018400 CLOSE-FILES.                                                     SG1024.2
018500     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   SG1024.2
018600 TERMINATE-CCVS.                                                  SG1024.2
018700*    EXIT PROGRAM.                                                SG1024.2
018800*TERMINATE-CALL.                                                  SG1024.2
018900     STOP     RUN.                                                SG1024.2
019000 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         SG1024.2
019100 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           SG1024.2
019200 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          SG1024.2
019300 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-CNT.          SG1024.2
019400     MOVE "****TEST DELETED****" TO RE-MARK.                      SG1024.2
019500 PRINT-DETAIL.                                                    SG1024.2
019600     IF REC-CT NOT EQUAL TO ZERO                                  SG1024.2
019700             MOVE "." TO PARDOT-X                                 SG1024.2
019800             MOVE REC-CT TO DOTVALUE.                             SG1024.2
019900     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      SG1024.2
020000     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               SG1024.2
020100        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 SG1024.2
020200          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 SG1024.2
020300     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              SG1024.2
020400     MOVE SPACE TO CORRECT-X.                                     SG1024.2
020500     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         SG1024.2
020600     MOVE     SPACE TO RE-MARK.                                   SG1024.2
020700 HEAD-ROUTINE.                                                    SG1024.2
020800     MOVE CCVS-H-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SG1024.2
020900     MOVE CCVS-H-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.   SG1024.2
021000     MOVE CCVS-H-3 TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.   SG1024.2
021100 COLUMN-NAMES-ROUTINE.                                            SG1024.2
021200     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SG1024.2
021300     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SG1024.2
021400     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        SG1024.2
021500 END-ROUTINE.                                                     SG1024.2
021600     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.SG1024.2
021700 END-RTN-EXIT.                                                    SG1024.2
021800     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SG1024.2
021900 END-ROUTINE-1.                                                   SG1024.2
022000      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      SG1024.2
022100      ERROR-HOLD. ADD DELETE-CNT TO ERROR-HOLD.                   SG1024.2
022200      ADD PASS-COUNTER TO ERROR-HOLD.                             SG1024.2
022300*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   SG1024.2
022400      MOVE PASS-COUNTER TO CCVS-E-4-1.                            SG1024.2
022500      MOVE ERROR-HOLD TO CCVS-E-4-2.                              SG1024.2
022600      MOVE CCVS-E-4 TO CCVS-E-2-2.                                SG1024.2
022700      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           SG1024.2
022800  END-ROUTINE-12.                                                 SG1024.2
022900      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        SG1024.2
023000     IF       ERROR-COUNTER IS EQUAL TO ZERO                      SG1024.2
023100         MOVE "NO " TO ERROR-TOTAL                                SG1024.2
023200         ELSE                                                     SG1024.2
023300         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       SG1024.2
023400     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           SG1024.2
023500     PERFORM WRITE-LINE.                                          SG1024.2
023600 END-ROUTINE-13.                                                  SG1024.2
023700     IF DELETE-CNT IS EQUAL TO ZERO                               SG1024.2
023800         MOVE "NO " TO ERROR-TOTAL  ELSE                          SG1024.2
023900         MOVE DELETE-CNT TO ERROR-TOTAL.                          SG1024.2
024000     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   SG1024.2
024100     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SG1024.2
024200      IF   INSPECT-COUNTER EQUAL TO ZERO                          SG1024.2
024300          MOVE "NO " TO ERROR-TOTAL                               SG1024.2
024400      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   SG1024.2
024500      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            SG1024.2
024600      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          SG1024.2
024700     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SG1024.2
024800 WRITE-LINE.                                                      SG1024.2
024900     ADD 1 TO RECORD-COUNT.                                       SG1024.2
025000*    IF RECORD-COUNT GREATER 50                                   SG1024.2
025100*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          SG1024.2
025200*        MOVE SPACE TO DUMMY-RECORD                               SG1024.2
025300*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  SG1024.2
025400*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             SG1024.2
025500*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     SG1024.2
025600*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          SG1024.2
025700*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          SG1024.2
025800*        MOVE ZERO TO RECORD-COUNT.                               SG1024.2
025900     PERFORM WRT-LN.                                              SG1024.2
026000 WRT-LN.                                                          SG1024.2
026100     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               SG1024.2
026200     MOVE SPACE TO DUMMY-RECORD.                                  SG1024.2
026300 BLANK-LINE-PRINT.                                                SG1024.2
026400     PERFORM WRT-LN.                                              SG1024.2
026500 FAIL-ROUTINE.                                                    SG1024.2
026600     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.   SG1024.2
026700     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.    SG1024.2
026800     MOVE "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.    SG1024.2
026900     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     SG1024.2
027000     GO TO FAIL-ROUTINE-EX.                                       SG1024.2
027100 FAIL-ROUTINE-WRITE.                                              SG1024.2
027200     MOVE TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE           SG1024.2
027300     MOVE TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES.   SG1024.2
027400 FAIL-ROUTINE-EX. EXIT.                                           SG1024.2
027500 BAIL-OUT.                                                        SG1024.2
027600     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.       SG1024.2
027700     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.               SG1024.2
027800 BAIL-OUT-WRITE.                                                  SG1024.2
027900     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  SG1024.2
028000     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     SG1024.2
028100 BAIL-OUT-EX. EXIT.                                               SG1024.2
028200 CCVS1-EXIT.                                                      SG1024.2
028300     EXIT.                                                        SG1024.2
028400 TEST-1 SECTION 00.                                               SG1024.2
028500 TEST-1A.                                                         SG1024.2
028600     GO TO TEST-1D.                                               SG1024.2
028700 TEST-1B.                                                         SG1024.2
028800     ADD 2 TO SEG-CALC.                                           SG1024.2
028900     MOVE SEG-CALC TO RANGE-X (RANGE-SUB).                        SG1024.2
029000     ADD 2 TO RANGE-SUB.                                          SG1024.2
029100     GO TO TEST-1D.                                               SG1024.2
029200 TEST-1C.                                                         SG1024.2
029300     ALTER PARA-1-22 TO PROCEED TO PARA-2-22.                     SG1024.2
029400*    NOTE ALTERED PARAGRAPH IN SECTION 22.                        SG1024.2
029500     PERFORM TEST22.                                              SG1024.2
029600 TEST-1D.                                                         SG1024.2
029700     EXIT.                                                        SG1024.2
029800 SEG-TEST2 SECTION 00.                                            SG1024.2
029900 SEG-TEST-2.                                                      SG1024.2
030000     MOVE 0 TO SEG-CALC.                                          SG1024.2
030100     MOVE SPACE TO COMPUTED-RANGE.                                SG1024.2
030200     ALTER TEST-1A TO PROCEED TO TEST-1B.                         SG1024.2
030300     MOVE "-" TO RANGE-X (2) RANGE-X (4) RANGE-X (6).             SG1024.2
030400     MOVE 0 TO RANGE-X (1).                                       SG1024.2
030500     MOVE 3 TO RANGE-SUB.                                         SG1024.2
030600     PERFORM TEST-1.                                              SG1024.2
030700     ALTER TEST-1A TO PROCEED TO TEST-1C.                         SG1024.2
030800     PERFORM TEST-1.                                              SG1024.2
030900     PERFORM TEST-1.                                              SG1024.2
031000     IF SEG-CALC EQUAL TO 2                                       SG1024.2
031100         PERFORM PASS                                             SG1024.2
031200         GO TO TEST-2-WRITE.                                      SG1024.2
031300     MOVE COMPUTED-RANGE TO COMPUTED-A.                           SG1024.2
031400     MOVE "0-2-0-2" TO CORRECT-A.                                 SG1024.2
031500     PERFORM FAIL.                                                SG1024.2
031600     GO TO TEST-2-WRITE.                                          SG1024.2
031700 TEST-2-DELETE.                                                   SG1024.2
031800     PERFORM DE-LETE.                                             SG1024.2
031900 TEST-2-WRITE.                                                    SG1024.2
032000     MOVE "SEG-TEST-2" TO PAR-NAME.                               SG1024.2
032100     PERFORM PRINT-DETAIL.                                        SG1024.2
032200     GO TO SEG-TEST-3.                                            SG1024.2
032300 TEST-6-1 SECTION 07.                                             SG1024.2
032400 TEST-6A.                                                         SG1024.2
032500     ALTER TEST-6B TO PROCEED TO TEST-6D.                         SG1024.2
032600 TEST-6-2 SECTION 08.                                             SG1024.2
032700 TEST-6B.                                                         SG1024.2
032800     GO TO TEST-6E.                                               SG1024.2
032900 TEST-6-3 SECTION 09.                                             SG1024.2
033000 TEST-6C.                                                         SG1024.2
033100     SUBTRACT 9 FROM SEG-CALC.                                    SG1024.2
033200     MOVE SEG-CALC TO RANGE-X (RANGE-SUB).                        SG1024.2
033300     ADD 2 TO RANGE-SUB.                                          SG1024.2
033400 TEST-6-4 SECTION 10.                                             SG1024.2
033500 TEST-6D.                                                         SG1024.2
033600     ALTER TEST-6B TO PROCEED TO TEST-6F.                         SG1024.2
033700 TEST-6-5 SECTION 11.                                             SG1024.2
033800 TEST-6E.                                                         SG1024.2
033900     SUBTRACT SEG-CALC FROM SEG-CALC.                             SG1024.2
034000     MOVE 0 TO RANGE-X (RANGE-SUB).                               SG1024.2
034100     ADD 2 TO RANGE-SUB.                                          SG1024.2
034200     GO TO TEST-6-2.                                              SG1024.2
034300 START-TESTING SECTION 11.                                        SG1024.2
034400 SEG-TEST-1.                                                      SG1024.2
034500     MOVE SPACE TO COMPUTED-RANGE.                                SG1024.2
034600     MOVE 4 TO SEG-CALC.                                          SG1024.2
034700     MOVE "-" TO RANGE-X (2) RANGE-X (4).                         SG1024.2
034800     MOVE 4 TO RANGE-X (1).                                       SG1024.2
034900     MOVE 3 TO RANGE-SUB.                                         SG1024.2
035000     ALTER TEST-1A TO PROCEED TO TEST-1C.                         SG1024.2
035100*    NOTE ALTERED PARAGRAPH IN SECTION 00.                        SG1024.2
035200     PERFORM TEST-1.                                              SG1024.2
035300     PERFORM TEST-1.                                              SG1024.2
035400*    NOTE 2ND PERFORM VERIFIES THAT TEST-1A IS STILL ALTERED      SG1024.2
035500*         TO TEST-1B.                                             SG1024.2
035600     IF SEG-CALC EQUAL TO 2                                       SG1024.2
035700         PERFORM PASS                                             SG1024.2
035800         GO TO TEST-1-WRITE.                                      SG1024.2
035900     MOVE COMPUTED-RANGE TO COMPUTED-A.                           SG1024.2
036000     MOVE "4-0-2" TO CORRECT-A.                                   SG1024.2
036100     PERFORM FAIL.                                                SG1024.2
036200     GO TO TEST-1-WRITE.                                          SG1024.2
036300 TEST-1-DELETE.                                                   SG1024.2
036400     PERFORM DE-LETE.                                             SG1024.2
036500 TEST-1-WRITE.                                                    SG1024.2
036600     MOVE "SEG-TEST-1" TO PAR-NAME.                               SG1024.2
036700     MOVE "SEGMENTATION" TO FEATURE.                              SG1024.2
036800     PERFORM PRINT-DETAIL.                                        SG1024.2
036900     GO TO SEG-TEST-2.                                            SG1024.2
037000 TEST-8-BRANCH SECTION 12.                                        SG1024.2
037100 PARA-8.                                                          SG1024.2
037200     GO TO SEG-TEST8.                                             SG1024.2
037300 TEST-6-6 SECTION 15.                                             SG1024.2
037400 TEST-6F.                                                         SG1024.2
037500     ADD 9 TO SEG-CALC.                                           SG1024.2
037600     MOVE SEG-CALC TO RANGE-X (RANGE-SUB).                        SG1024.2
037700     ADD 2 TO RANGE-SUB.                                          SG1024.2
037800 TEST-6-7 SECTION 18.                                             SG1024.2
037900 TEST-6G.                                                         SG1024.2
038000     ALTER TEST-6B TO PROCEED TO TEST-6-8.                        SG1024.2
038100     GO TO TEST-6-2.                                              SG1024.2
038200 TEST-6-8 SECTION 20.                                             SG1024.2
038300 TEST-6H.                                                         SG1024.2
038400     SUBTRACT 1 FROM SEG-CALC.                                    SG1024.2
038500     MOVE SEG-CALC TO RANGE-X (RANGE-SUB).                        SG1024.2
038600     ADD 2 TO RANGE-SUB.                                          SG1024.2
038700 TEST-6-9 SECTION 22.                                             SG1024.2
038800 TEST-6I.                                                         SG1024.2
038900     EXIT.                                                        SG1024.2
039000 TEST22 SECTION 22.                                               SG1024.2
039100 PARA-1-22.                                                       SG1024.2
039200     GO TO PARA-3-22.                                             SG1024.2
039300 PARA-2-22.                                                       SG1024.2
039400     MOVE 0 TO SEG-CALC.                                          SG1024.2
039500     MOVE SEG-CALC TO RANGE-X (RANGE-SUB).                        SG1024.2
039600     ADD 2 TO RANGE-SUB.                                          SG1024.2
039700     ALTER TEST-1A TO PROCEED TO TEST-1B.                         SG1024.2
039800 PARA-3-22.                                                       SG1024.2
039900     EXIT.                                                        SG1024.2
040000 TEST-4 SECTION 43.                                               SG1024.2
040100 TEST-4A.                                                         SG1024.2
040200     GO TO TEST-4C.                                               SG1024.2
040300 TEST-4B.                                                         SG1024.2
040400     SUBTRACT 1 FROM SEG-CALC.                                    SG1024.2
040500     MOVE SEG-CALC TO RANGE-X (RANGE-SUB).                        SG1024.2
040600     ADD 2 TO RANGE-SUB.                                          SG1024.2
040700     IF SEG-CALC IS GREATER THAN 0                                SG1024.2
040800         GO TO TEST-4A.                                           SG1024.2
040900     GO TO TEST-4D.                                               SG1024.2
041000 TEST-4C.                                                         SG1024.2
041100     ALTER TEST-4A TO PROCEED TO TEST-4B.                         SG1024.2
041200     GO TO TEST-4B.                                               SG1024.2
041300 TEST-4D.                                                         SG1024.2
041400     EXIT.                                                        SG1024.2
041500 SEG-TEST5 SECTION 43.                                            SG1024.2
041600 SEG-TEST-5.                                                      SG1024.2
041700     MOVE SPACE TO COMPUTED-RANGE.                                SG1024.2
041800     MOVE 5 TO SEG-CALC.                                          SG1024.2
041900     MOVE SEG-CALC TO RANGE-X (1).                                SG1024.2
042000     MOVE "-" TO RANGE-X (2) RANGE-X (4).                         SG1024.2
042100     MOVE 3 TO RANGE-SUB.                                         SG1024.2
042200     PERFORM TEST-5.                                              SG1024.2
042300 SEG-5A.                                                          SG1024.2
042400     GO TO SEG-5C.                                                SG1024.2
042500 SEG-5B.                                                          SG1024.2
042600     PERFORM TEST-5B THRU TEST-5C.                                SG1024.2
042700     IF SEG-CALC EQUAL TO 7                                       SG1024.2
042800         PERFORM PASS                                             SG1024.2
042900         GO TO TEST-5-WRITE.                                      SG1024.2
043000 SEG-5C.                                                          SG1024.2
043100     MOVE COMPUTED-RANGE TO COMPUTED-A.                           SG1024.2
043200     MOVE "5-6-7" TO CORRECT-A.                                   SG1024.2
043300     PERFORM FAIL.                                                SG1024.2
043400     GO TO TEST-5-WRITE.                                          SG1024.2
043500 TEST-5-DELETE.                                                   SG1024.2
043600     PERFORM DE-LETE.                                             SG1024.2
043700 TEST-5-WRITE.                                                    SG1024.2
043800     MOVE "SEG-TEST-5" TO PAR-NAME.                               SG1024.2
043900     PERFORM PRINT-DETAIL.                                        SG1024.2
044000     GO TO SEG-TEST-6.                                            SG1024.2
044100 SEG-TEST7 SECTION 74.                                            SG1024.2
044200 SEG-TEST-7.                                                      SG1024.2
044300     MOVE SPACE TO COMPUTED-RANGE.                                SG1024.2
044400     MOVE 3 TO SEG-CALC.                                          SG1024.2
044500     MOVE 3 TO RANGE-SUB.                                         SG1024.2
044600     MOVE SEG-CALC TO RANGE-X (1).                                SG1024.2
044700     MOVE "-" TO RANGE-X (2) RANGE-X (4) RANGE-X (6).             SG1024.2
044800     ALTER TEST-7A TO PROCEED TO TEST-7D.                         SG1024.2
044900     PERFORM TEST-7-1 THRU TEST-7-4.                              SG1024.2
045000     PERFORM TEST-7-1 THRU TEST-7-4.                              SG1024.2
045100     IF SEG-CALC EQUAL TO 4                                       SG1024.2
045200         PERFORM PASS                                             SG1024.2
045300         GO TO TEST-7-WRITE.                                      SG1024.2
045400     MOVE COMPUTED-RANGE TO COMPUTED-A.                           SG1024.2
045500     MOVE "3-2-5-4" TO CORRECT-A.                                 SG1024.2
045600     PERFORM FAIL.                                                SG1024.2
045700     GO TO TEST-7-WRITE.                                          SG1024.2
045800 TEST-7-DELETE.                                                   SG1024.2
045900     PERFORM DE-LETE.                                             SG1024.2
046000 TEST-7-WRITE.                                                    SG1024.2
046100     MOVE "SEG-TEST-7" TO PAR-NAME.                               SG1024.2
046200     PERFORM PRINT-DETAIL.                                        SG1024.2
046300     MOVE 0 TO SEG-CALC.                                          SG1024.2
046400     GO TO TEST-8-BRANCH.                                         SG1024.2
046500 TEST-7-1 SECTION 74.                                             SG1024.2
046600 TEST-7A.                                                         SG1024.2
046700     GO TO TEST-7B.                                               SG1024.2
046800 TEST-7-2 SECTION 74.                                             SG1024.2
046900 TEST-7B.                                                         SG1024.2
047000     ALTER TEST-7A TO PROCEED TO TEST-7C.                         SG1024.2
047100 TEST-7-3 SECTION 74.                                             SG1024.2
047200 TEST-7C.                                                         SG1024.2
047300     ADD 3 TO SEG-CALC.                                           SG1024.2
047400     MOVE SEG-CALC TO RANGE-X (RANGE-SUB).                        SG1024.2
047500     ADD 2 TO RANGE-SUB.                                          SG1024.2
047600 TEST-7D.                                                         SG1024.2
047700     SUBTRACT 1 FROM SEG-CALC.                                    SG1024.2
047800     MOVE SEG-CALC TO RANGE-X (RANGE-SUB).                        SG1024.2
047900     ADD 2 TO RANGE-SUB.                                          SG1024.2
048000     PERFORM TEST-7B.                                             SG1024.2
048100 TEST-7-4 SECTION 74.                                             SG1024.2
048200 TEST-7E.                                                         SG1024.2
048300     GO TO TEST-7F.                                               SG1024.2
048400 TEST-7F.                                                         SG1024.2
048500     ALTER TEST-7E TO PROCEED TO TEST-7G.                         SG1024.2
048600 TEST-7G.                                                         SG1024.2
048700     EXIT.                                                        SG1024.2
048800 SEG-TEST3  SECTION 66.                                           SG1024.2
048900 SEG-TEST-3.                                                      SG1024.2
049000     MOVE 2 TO SEG-CALC.                                          SG1024.2
049100     MOVE SPACE TO COMPUTED-RANGE.                                SG1024.2
049200     MOVE 2 TO RANGE-X (1).                                       SG1024.2
049300     MOVE "-" TO RANGE-X (2) RANGE-X (4).                         SG1024.2
049400     MOVE 3 TO RANGE-SUB.                                         SG1024.2
049500     PERFORM  TEST-3.                                             SG1024.2
049600     ALTER TEST-3X TO PROCEED TO TEST-3B.                         SG1024.2
049700     ALTER TEST-3A TO PROCEED TO TEST-3C.                         SG1024.2
049800     PERFORM TEST-3A THRU TEST-3EXIT.                             SG1024.2
049900     PERFORM TEST-3C.                                             SG1024.2
050000     GO TO TEST-3X.                                               SG1024.2
050100*    NOTE PERFORMING AND GO TO SECTION 66 PARAGRAPHS.             SG1024.2
050200 TEST-3-DELETE.                                                   SG1024.2
050300     PERFORM DE-LETE.                                             SG1024.2
050400 TEST-3-WRITE.                                                    SG1024.2
050500     MOVE "SEG-TEST-3" TO PAR-NAME.                               SG1024.2
050600     PERFORM PRINT-DETAIL.                                        SG1024.2
050700     GO TO SEG-TEST-4.                                            SG1024.2
050800 TEST-3 SECTION 66.                                               SG1024.2
050900 TEST-3X.                                                         SG1024.2
051000     GO TO TEST-3D.                                               SG1024.2
051100 TEST-3A.                                                         SG1024.2
051200     GO TO TEST-3B.                                               SG1024.2
051300 TEST-3B.                                                         SG1024.2
051400     IF SEG-CALC EQUAL TO 6                                       SG1024.2
051500         PERFORM PASS                                             SG1024.2
051600         GO TO TEST-3-WRITE.                                      SG1024.2
051700     MOVE COMPUTED-RANGE TO COMPUTED-A.                           SG1024.2
051800     MOVE "2-4-6" TO CORRECT-A.                                   SG1024.2
051900     PERFORM FAIL.                                                SG1024.2
052000     GO TO TEST-3-WRITE.                                          SG1024.2
052100 TEST-3C.                                                         SG1024.2
052200     ADD 2 TO SEG-CALC.                                           SG1024.2
052300     MOVE SEG-CALC TO RANGE-X (RANGE-SUB).                        SG1024.2
052400     ADD 2 TO RANGE-SUB.                                          SG1024.2
052500 TEST-3D.                                                         SG1024.2
052600     IF SEG-CALC EQUAL TO 2 GO TO TEST-3EXIT.                     SG1024.2
052700     PERFORM TEST-3C.                                             SG1024.2
052800     GO TO TEST-3B.                                               SG1024.2
052900 TEST-3EXIT.                                                      SG1024.2
053000     EXIT.                                                        SG1024.2
053100 SEG-TEST4  SECTION 66.                                           SG1024.2
053200 SEG-TEST-4.                                                      SG1024.2
053300     MOVE SPACE TO COMPUTED-RANGE.                                SG1024.2
053400     MOVE 3 TO RANGE-SUB.                                         SG1024.2
053500     MOVE 3 TO SEG-CALC.                                          SG1024.2
053600     MOVE 3 TO RANGE-X (1).                                       SG1024.2
053700     MOVE "-" TO RANGE-X (2) RANGE-X (4) RANGE-X (6).             SG1024.2
053800     PERFORM TEST-4.                                              SG1024.2
053900     IF SEG-CALC EQUAL TO 0                                       SG1024.2
054000         PERFORM PASS                                             SG1024.2
054100         GO TO TEST-4-WRITE.                                      SG1024.2
054200     MOVE COMPUTED-RANGE TO COMPUTED-A.                           SG1024.2
054300     MOVE "3-2-1-0" TO CORRECT-A.                                 SG1024.2
054400     PERFORM FAIL.                                                SG1024.2
054500     GO TO TEST-4-WRITE.                                          SG1024.2
054600 TEST-4-DELETE.                                                   SG1024.2
054700     PERFORM DE-LETE.                                             SG1024.2
054800 TEST-4-WRITE.                                                    SG1024.2
054900     MOVE "SEG-TEST-4" TO PAR-NAME.                               SG1024.2
055000     PERFORM PRINT-DETAIL.                                        SG1024.2
055100     GO TO SEG-TEST-5.                                            SG1024.2
055200 SEG-TEST6 SECTION 83.                                            SG1024.2
055300 SEG-TEST-6.                                                      SG1024.2
055400     MOVE 9 TO SEG-CALC.                                          SG1024.2
055500     MOVE SPACE TO COMPUTED-RANGE.                                SG1024.2
055600     MOVE SEG-CALC TO RANGE-X (1).                                SG1024.2
055700     MOVE "-" TO RANGE-X (2) RANGE-X (4) RANGE-X (6).             SG1024.2
055800     MOVE 3 TO RANGE-SUB.                                         SG1024.2
055900     PERFORM TEST-6A THRU TEST-6I.                                SG1024.2
056000     IF SEG-CALC EQUAL TO 8                                       SG1024.2
056100         PERFORM PASS                                             SG1024.2
056200         GO TO TEST-6-WRITE.                                      SG1024.2
056300     MOVE COMPUTED-RANGE TO COMPUTED-A.                           SG1024.2
056400     MOVE "9-0-9-8" TO CORRECT-A.                                 SG1024.2
056500     PERFORM FAIL.                                                SG1024.2
056600     GO TO TEST-6-WRITE.                                          SG1024.2
056700 TEST-6-DELETE.                                                   SG1024.2
056800     PERFORM DE-LETE.                                             SG1024.2
056900 TEST-6-WRITE.                                                    SG1024.2
057000     MOVE "SEG-TEST-6" TO PAR-NAME.                               SG1024.2
057100     PERFORM PRINT-DETAIL.                                        SG1024.2
057200     GO TO SEG-TEST-7.                                            SG1024.2
057300*    NOTE PERFORM RESIDENT SECTIONS 7 THRU 22.                    SG1024.2
057400 SEG-TEST8 SECTION 84.                                            SG1024.2
057500 SEG-TEST-8.                                                      SG1024.2
057600     ALTER PARA-8 TO PROCEED TO SEG-TEST-8A.                      SG1024.2
057700     ADD 1 TO SEG-CALC.                                           SG1024.2
057800     IF SEG-CALC EQUAL TO 2                                       SG1024.2
057900         PERFORM FAIL                                             SG1024.2
058000         GO TO TEST-8-WRITE.                                      SG1024.2
058100     GO TO TEST-8-BRANCH.                                         SG1024.2
058200 SEG-TEST-8A SECTION 85.                                          SG1024.2
058300 PARA-85.                                                         SG1024.2
058400     PERFORM PASS.                                                SG1024.2
058500 TEST-8-WRITE.                                                    SG1024.2
058600     MOVE "ALTER RES TO NON-RES" TO FEATURE.                      SG1024.2
058700     MOVE "SEG-TEST-8" TO PAR-NAME.                               SG1024.2
058800     PERFORM PRINT-DETAIL.                                        SG1024.2
058900     GO TO    CLOSE-FILES.                                        SG1024.2
059000 TEST-5 SECTION 99.                                               SG1024.2
059100 TEST-5A.                                                         SG1024.2
059200     GO TO TEST-5B.                                               SG1024.2
059300 TEST-5B.                                                         SG1024.2
059400     ALTER SEG-5A TO PROCEED TO SEG-5B.                           SG1024.2
059500     ALTER TEST-5A TO PROCEED TO TEST-5C.                         SG1024.2
059600     PERFORM SEG-99A THROUGH SEG-99C.                             SG1024.2
059700     GO TO TEST-5A.                                               SG1024.2
059800 TEST-5C.                                                         SG1024.2
059900     EXIT.                                                        SG1024.2
060000 SEG SECTION 99.                                                  SG1024.2
060100 SEG-99A.                                                         SG1024.2
060200     GO TO SEG-99B.                                               SG1024.2
060300 SEG-99B.                                                         SG1024.2
060400     ALTER SEG-99A TO PROCEED TO SEG-99C.                         SG1024.2
060500     ALTER TEST-5A TO PROCEED TO TEST-5B.                         SG1024.2
060600     ADD 1 TO SEG-CALC.                                           SG1024.2
060700     MOVE SEG-CALC TO RANGE-X (RANGE-SUB).                        SG1024.2
060800     ADD 2 TO RANGE-SUB.                                          SG1024.2
060900 SEG-99C.                                                         SG1024.2
061000     EXIT.                                                        SG1024.2
      *END-OF,SG102A                                                                  
