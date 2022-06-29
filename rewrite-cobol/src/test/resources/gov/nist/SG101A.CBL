      *HEADER,COBOL,SG101A                                                            
000100 IDENTIFICATION DIVISION.                                         SG1014.2
000200 PROGRAM-ID.                                                      SG1014.2
000300     SG101A.                                                      SG1014.2
000400 AUTHOR.                                                          SG1014.2
000500     FEDERAL COMPILER TESTING CENTER.                             SG1014.2
000600 INSTALLATION.                                                    SG1014.2
000700     GENERAL SERVICES ADMINISTRATION                              SG1014.2
000800*     AUTOMATED DATA AND TELECOMMUNICATION SERVICE.                SG1014.2
000900     SOFTWARE DEVELOPMENT OFFICE.                                 SG1014.2
001000     5203 LEESBURG PIKE  SUITE 1100                               SG1014.2
001100     FALLS CHURCH VIRGINIA 22041.                                 SG1014.2
001200                                                                  SG1014.2
001300     PHONE   (703) 756-6153                                       SG1014.2
001400                                                                  SG1014.2
001500     " HIGH       ".                                              SG1014.2
001600 DATE-WRITTEN.                                                    SG1014.2
001700     CCVS-74 VERSION 4.0 - 1980 JULY 1.                           SG1014.2
001800     CREATION DATE     /    VALIDATION DATE                       SG1014.2
001900     "4.2 ".                                                      SG1014.2
002000 SECURITY.                                                        SG1014.2
002100     NONE.                                                        SG1014.2
002200       THE FOLLOWING FEATURES ARE TESTED BY THIS PROGRAM ---      SG1014.2
002300         THE ABILITY OF THE COMPILER TO ACCEPT 100 SEGMENTS       SG1014.2
002400         AND REFERENCE SEGMENTS GREATER THAN 49 IN THEIR INITIAL  SG1014.2
002500         STATE WITH SEGMENTS LESS THAN 50 IN THEIR LAST-USED      SG1014.2
002600         STATE                                                    SG1014.2
002700                                                                  SG1014.2
002800 ENVIRONMENT DIVISION.                                            SG1014.2
002900 CONFIGURATION SECTION.                                           SG1014.2
003000 SOURCE-COMPUTER.                                                 SG1014.2
003100     XXXXX082.                                                    SG1014.2
003200 OBJECT-COMPUTER.                                                 SG1014.2
003300     XXXXX083.                                                    SG1014.2
003400 INPUT-OUTPUT SECTION.                                            SG1014.2
003500 FILE-CONTROL.                                                    SG1014.2
003600     SELECT PRINT-FILE ASSIGN TO                                  SG1014.2
003700     XXXXX055.                                                    SG1014.2
003800 DATA DIVISION.                                                   SG1014.2
003900 FILE SECTION.                                                    SG1014.2
004000 FD  PRINT-FILE                                                   SG1014.2
004100     LABEL RECORDS                                                SG1014.2
004200     XXXXX084                                                     SG1014.2
004300     DATA RECORD IS PRINT-REC DUMMY-RECORD.                       SG1014.2
004400 01  PRINT-REC PICTURE X(120).                                    SG1014.2
004500 01  DUMMY-RECORD PICTURE X(120).                                 SG1014.2
004600 WORKING-STORAGE SECTION.                                         SG1014.2
004700 77  TEST-CHECK                    PICTURE XXXX VALUE SPACE.      SG1014.2
004800 01  TEST-RESULTS.                                                SG1014.2
004900     02 FILLER                    PICTURE X VALUE SPACE.          SG1014.2
005000     02 FEATURE                   PICTURE X(20) VALUE SPACE.      SG1014.2
005100     02 FILLER                    PICTURE X VALUE SPACE.          SG1014.2
005200     02 P-OR-F                    PICTURE X(5) VALUE SPACE.       SG1014.2
005300     02 FILLER                    PICTURE X  VALUE SPACE.         SG1014.2
005400     02  PAR-NAME.                                                SG1014.2
005500       03 FILLER PICTURE X(12) VALUE SPACE.                       SG1014.2
005600       03  PARDOT-X PICTURE X  VALUE SPACE.                       SG1014.2
005700       03 DOTVALUE PICTURE 99  VALUE ZERO.                        SG1014.2
005800       03 FILLER PIC X(5) VALUE SPACE.                            SG1014.2
005900     02 FILLER PIC X(10) VALUE SPACE.                             SG1014.2
006000     02 RE-MARK PIC X(61).                                        SG1014.2
006100 01  TEST-COMPUTED.                                               SG1014.2
006200     02 FILLER PIC X(30) VALUE SPACE.                             SG1014.2
006300     02 FILLER PIC X(17) VALUE "       COMPUTED=".                SG1014.2
006400     02 COMPUTED-X.                                               SG1014.2
006500     03 COMPUTED-A                PICTURE X(20) VALUE SPACE.      SG1014.2
006600     03 COMPUTED-N REDEFINES COMPUTED-A PICTURE -9(9).9(9).       SG1014.2
006700     03 COMPUTED-0V18 REDEFINES COMPUTED-A  PICTURE -.9(18).      SG1014.2
006800     03 COMPUTED-4V14 REDEFINES COMPUTED-A  PICTURE -9(4).9(14).  SG1014.2
006900     03 COMPUTED-14V4 REDEFINES COMPUTED-A  PICTURE -9(14).9(4).  SG1014.2
007000     03       CM-18V0 REDEFINES COMPUTED-A.                       SG1014.2
007100         04 COMPUTED-18V0                   PICTURE -9(18).       SG1014.2
007200         04 FILLER                          PICTURE X.            SG1014.2
007300     03 FILLER PIC X(50) VALUE SPACE.                             SG1014.2
007400 01  TEST-CORRECT.                                                SG1014.2
007500     02 FILLER PIC X(30) VALUE SPACE.                             SG1014.2
007600     02 FILLER PIC X(17) VALUE "       CORRECT =".                SG1014.2
007700     02 CORRECT-X.                                                SG1014.2
007800     03 CORRECT-A                 PICTURE X(20) VALUE SPACE.      SG1014.2
007900     03 CORRECT-N REDEFINES CORRECT-A PICTURE -9(9).9(9).         SG1014.2
008000     03 CORRECT-0V18 REDEFINES CORRECT-A    PICTURE -.9(18).      SG1014.2
008100     03 CORRECT-4V14 REDEFINES CORRECT-A    PICTURE -9(4).9(14).  SG1014.2
008200     03 CORRECT-14V4 REDEFINES CORRECT-A    PICTURE -9(14).9(4).  SG1014.2
008300     03      CR-18V0 REDEFINES CORRECT-A.                         SG1014.2
008400         04 CORRECT-18V0                    PICTURE -9(18).       SG1014.2
008500         04 FILLER                          PICTURE X.            SG1014.2
008600     03 FILLER PIC X(50) VALUE SPACE.                             SG1014.2
008700 01  CCVS-C-1.                                                    SG1014.2
008800     02 FILLER PICTURE IS X(99) VALUE IS " FEATURE              PASG1014.2
008900-    "SS  PARAGRAPH-NAME                                          SG1014.2
009000-    "        REMARKS".                                           SG1014.2
009100     02 FILLER PICTURE IS X(20) VALUE IS SPACE.                   SG1014.2
009200 01  CCVS-C-2.                                                    SG1014.2
009300     02 FILLER PICTURE IS X VALUE IS SPACE.                       SG1014.2
009400     02 FILLER PICTURE IS X(6) VALUE IS "TESTED".                 SG1014.2
009500     02 FILLER PICTURE IS X(15) VALUE IS SPACE.                   SG1014.2
009600     02 FILLER PICTURE IS X(4) VALUE IS "FAIL".                   SG1014.2
009700     02 FILLER PICTURE IS X(94) VALUE IS SPACE.                   SG1014.2
009800 01  REC-SKL-SUB PICTURE 9(2) VALUE ZERO.                         SG1014.2
009900 01  REC-CT PICTURE 99 VALUE ZERO.                                SG1014.2
010000 01  DELETE-CNT                   PICTURE 999  VALUE ZERO.        SG1014.2
010100 01  ERROR-COUNTER PICTURE IS 999 VALUE IS ZERO.                  SG1014.2
010200 01  INSPECT-COUNTER PIC 999 VALUE ZERO.                          SG1014.2
010300 01  PASS-COUNTER PIC 999 VALUE ZERO.                             SG1014.2
010400 01  TOTAL-ERROR PIC 999 VALUE ZERO.                              SG1014.2
010500 01  ERROR-HOLD PIC 999 VALUE ZERO.                               SG1014.2
010600 01  DUMMY-HOLD PIC X(120) VALUE SPACE.                           SG1014.2
010700 01  RECORD-COUNT PIC 9(5) VALUE ZERO.                            SG1014.2
010800 01  CCVS-H-1.                                                    SG1014.2
010900     02  FILLER   PICTURE X(27)  VALUE SPACE.                     SG1014.2
011000     02 FILLER PICTURE X(67) VALUE                                SG1014.2
011100     " FEDERAL COMPILER TESTING CENTER COBOL COMPILER VALIDATION  SG1014.2
011200-    " SYSTEM".                                                   SG1014.2
011300     02  FILLER     PICTURE X(26)  VALUE SPACE.                   SG1014.2
011400 01  CCVS-H-2.                                                    SG1014.2
011500     02 FILLER PICTURE X(52) VALUE IS                             SG1014.2
011600     "CCVS74 NCC  COPY, NOT FOR DISTRIBUTION.".                   SG1014.2
011700     02 FILLER PICTURE IS X(19) VALUE IS "TEST RESULTS SET-  ".   SG1014.2
011800     02 TEST-ID PICTURE IS X(9).                                  SG1014.2
011900     02 FILLER PICTURE IS X(40) VALUE IS SPACE.                   SG1014.2
012000 01  CCVS-H-3.                                                    SG1014.2
012100     02  FILLER PICTURE X(34) VALUE                               SG1014.2
012200     " FOR OFFICIAL USE ONLY    ".                                SG1014.2
012300     02  FILLER PICTURE X(58) VALUE                               SG1014.2
012400     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SG1014.2
012500     02  FILLER PICTURE X(28) VALUE                               SG1014.2
012600     "  COPYRIGHT   1974 ".                                       SG1014.2
012700 01  CCVS-E-1.                                                    SG1014.2
012800     02 FILLER PICTURE IS X(52) VALUE IS SPACE.                   SG1014.2
012900     02 FILLER PICTURE IS X(14) VALUE IS "END OF TEST-  ".        SG1014.2
013000     02 ID-AGAIN PICTURE IS X(9).                                 SG1014.2
013100     02 FILLER PICTURE X(45) VALUE IS                             SG1014.2
013200     " NTIS DISTRIBUTION COBOL 74".                               SG1014.2
013300 01  CCVS-E-2.                                                    SG1014.2
013400     02  FILLER                   PICTURE X(31)  VALUE            SG1014.2
013500     SPACE.                                                       SG1014.2
013600     02  FILLER                   PICTURE X(21)  VALUE SPACE.     SG1014.2
013700     02 CCVS-E-2-2.                                               SG1014.2
013800         03 ERROR-TOTAL PICTURE IS XXX VALUE IS SPACE.            SG1014.2
013900         03 FILLER PICTURE IS X VALUE IS SPACE.                   SG1014.2
014000         03 ENDER-DESC PIC X(44) VALUE "ERRORS ENCOUNTERED".      SG1014.2
014100 01  CCVS-E-3.                                                    SG1014.2
014200     02  FILLER PICTURE X(22) VALUE                               SG1014.2
014300     " FOR OFFICIAL USE ONLY".                                    SG1014.2
014400     02  FILLER PICTURE X(12) VALUE SPACE.                        SG1014.2
014500     02  FILLER PICTURE X(58) VALUE                               SG1014.2
014600     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SG1014.2
014700     02  FILLER PICTURE X(13) VALUE SPACE.                        SG1014.2
014800     02 FILLER PIC X(15) VALUE " COPYRIGHT 1974".                 SG1014.2
014900 01  CCVS-E-4.                                                    SG1014.2
015000     02 CCVS-E-4-1 PIC XXX VALUE SPACE.                           SG1014.2
015100     02 FILLER PIC XXXX VALUE " OF ".                             SG1014.2
015200     02 CCVS-E-4-2 PIC XXX VALUE SPACE.                           SG1014.2
015300     02 FILLER PIC X(40) VALUE                                    SG1014.2
015400      "  TESTS WERE EXECUTED SUCCESSFULLY".                       SG1014.2
015500 01  XXINFO.                                                      SG1014.2
015600     02 FILLER PIC X(30) VALUE "        *** INFORMATION  ***".    SG1014.2
015700     02 INFO-TEXT.                                                SG1014.2
015800     04 FILLER PIC X(20) VALUE SPACE.                             SG1014.2
015900     04 XXCOMPUTED PIC X(20).                                     SG1014.2
016000     04 FILLER PIC X(5) VALUE SPACE.                              SG1014.2
016100     04 XXCORRECT PIC X(20).                                      SG1014.2
016200 01  HYPHEN-LINE.                                                 SG1014.2
016300     02 FILLER PICTURE IS X VALUE IS SPACE.                       SG1014.2
016400     02 FILLER PICTURE IS X(65) VALUE IS "************************SG1014.2
016500-    "*****************************************".                 SG1014.2
016600     02 FILLER PICTURE IS X(54) VALUE IS "************************SG1014.2
016700-    "******************************".                            SG1014.2
016800 01  CCVS-PGM-ID PIC X(6) VALUE                                   SG1014.2
016900     "SG101A".                                                    SG1014.2
017000 PROCEDURE DIVISION.                                              SG1014.2
017100 CCVS1 SECTION.                                                   SG1014.2
017200 OPEN-FILES.                                                      SG1014.2
017300     OPEN     OUTPUT PRINT-FILE.                                  SG1014.2
017400     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   SG1014.2
017500     MOVE    SPACE TO TEST-RESULTS.                               SG1014.2
017600     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             SG1014.2
017700     GO TO CCVS1-EXIT.                                            SG1014.2
017800 CLOSE-FILES.                                                     SG1014.2
017900     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   SG1014.2
018000 TERMINATE-CCVS.                                                  SG1014.2
018100*    EXIT PROGRAM.                                                SG1014.2
018200*TERMINATE-CALL.                                                  SG1014.2
018300     STOP     RUN.                                                SG1014.2
018400 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         SG1014.2
018500 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           SG1014.2
018600 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          SG1014.2
018700 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-CNT.          SG1014.2
018800     MOVE "****TEST DELETED****" TO RE-MARK.                      SG1014.2
018900 PRINT-DETAIL.                                                    SG1014.2
019000     IF REC-CT NOT EQUAL TO ZERO                                  SG1014.2
019100             MOVE "." TO PARDOT-X                                 SG1014.2
019200             MOVE REC-CT TO DOTVALUE.                             SG1014.2
019300     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      SG1014.2
019400     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               SG1014.2
019500        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 SG1014.2
019600          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 SG1014.2
019700     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              SG1014.2
019800     MOVE SPACE TO CORRECT-X.                                     SG1014.2
019900     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         SG1014.2
020000     MOVE     SPACE TO RE-MARK.                                   SG1014.2
020100 HEAD-ROUTINE.                                                    SG1014.2
020200     MOVE CCVS-H-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SG1014.2
020300     MOVE CCVS-H-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.   SG1014.2
020400     MOVE CCVS-H-3 TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.   SG1014.2
020500 COLUMN-NAMES-ROUTINE.                                            SG1014.2
020600     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SG1014.2
020700     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SG1014.2
020800     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        SG1014.2
020900 END-ROUTINE.                                                     SG1014.2
021000     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.SG1014.2
021100 END-RTN-EXIT.                                                    SG1014.2
021200     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SG1014.2
021300 END-ROUTINE-1.                                                   SG1014.2
021400      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      SG1014.2
021500      ERROR-HOLD. ADD DELETE-CNT TO ERROR-HOLD.                   SG1014.2
021600      ADD PASS-COUNTER TO ERROR-HOLD.                             SG1014.2
021700*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   SG1014.2
021800      MOVE PASS-COUNTER TO CCVS-E-4-1.                            SG1014.2
021900      MOVE ERROR-HOLD TO CCVS-E-4-2.                              SG1014.2
022000      MOVE CCVS-E-4 TO CCVS-E-2-2.                                SG1014.2
022100      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           SG1014.2
022200  END-ROUTINE-12.                                                 SG1014.2
022300      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        SG1014.2
022400     IF       ERROR-COUNTER IS EQUAL TO ZERO                      SG1014.2
022500         MOVE "NO " TO ERROR-TOTAL                                SG1014.2
022600         ELSE                                                     SG1014.2
022700         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       SG1014.2
022800     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           SG1014.2
022900     PERFORM WRITE-LINE.                                          SG1014.2
023000 END-ROUTINE-13.                                                  SG1014.2
023100     IF DELETE-CNT IS EQUAL TO ZERO                               SG1014.2
023200         MOVE "NO " TO ERROR-TOTAL  ELSE                          SG1014.2
023300         MOVE DELETE-CNT TO ERROR-TOTAL.                          SG1014.2
023400     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   SG1014.2
023500     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SG1014.2
023600      IF   INSPECT-COUNTER EQUAL TO ZERO                          SG1014.2
023700          MOVE "NO " TO ERROR-TOTAL                               SG1014.2
023800      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   SG1014.2
023900      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            SG1014.2
024000      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          SG1014.2
024100     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SG1014.2
024200 WRITE-LINE.                                                      SG1014.2
024300     ADD 1 TO RECORD-COUNT.                                       SG1014.2
024400*    IF RECORD-COUNT GREATER 50                                   SG1014.2
024500*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          SG1014.2
024600*        MOVE SPACE TO DUMMY-RECORD                               SG1014.2
024700*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  SG1014.2
024800*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             SG1014.2
024900*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     SG1014.2
025000*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          SG1014.2
025100*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          SG1014.2
025200*        MOVE ZERO TO RECORD-COUNT.                               SG1014.2
025300     PERFORM WRT-LN.                                              SG1014.2
025400 WRT-LN.                                                          SG1014.2
025500     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               SG1014.2
025600     MOVE SPACE TO DUMMY-RECORD.                                  SG1014.2
025700 BLANK-LINE-PRINT.                                                SG1014.2
025800     PERFORM WRT-LN.                                              SG1014.2
025900 FAIL-ROUTINE.                                                    SG1014.2
026000     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.   SG1014.2
026100     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.    SG1014.2
026200     MOVE "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.    SG1014.2
026300     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     SG1014.2
026400     GO TO FAIL-ROUTINE-EX.                                       SG1014.2
026500 FAIL-ROUTINE-WRITE.                                              SG1014.2
026600     MOVE TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE           SG1014.2
026700     MOVE TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES.   SG1014.2
026800 FAIL-ROUTINE-EX. EXIT.                                           SG1014.2
026900 BAIL-OUT.                                                        SG1014.2
027000     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.       SG1014.2
027100     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.               SG1014.2
027200 BAIL-OUT-WRITE.                                                  SG1014.2
027300     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  SG1014.2
027400     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     SG1014.2
027500 BAIL-OUT-EX. EXIT.                                               SG1014.2
027600 CCVS1-EXIT.                                                      SG1014.2
027700     EXIT.                                                        SG1014.2
027800 SECT-SG-01-001 SECTION 00.                                       SG1014.2
027900 SG-01-001.                                                       SG1014.2
028000     MOVE "SEGMENTATION" TO FEATURE.                              SG1014.2
028100     GO TO SEG-TEST-1.                                            SG1014.2
028200 00  SECTION 00.                                                  SG1014.2
028300 PARA-00.                                                         SG1014.2
028400     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
028500 RUN-THE-TESTS SECTION.                                           SG1014.2
028600 SEG-TEST-1.                                                      SG1014.2
028700     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
028800     PERFORM 00.                                                  SG1014.2
028900     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
029000         PERFORM PASS                                             SG1014.2
029100         GO TO SEG-WRITE-1.                                       SG1014.2
029200     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
029300     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
029400     PERFORM FAIL.                                                SG1014.2
029500     GO TO SEG-WRITE-1.                                           SG1014.2
029600 SEG-DELETE-1.                                                    SG1014.2
029700     PERFORM DE-LETE.                                             SG1014.2
029800 SEG-WRITE-1.                                                     SG1014.2
029900     MOVE "SEG-TEST-1  " TO PAR-NAME.                             SG1014.2
030000     PERFORM PRINT-DETAIL.                                        SG1014.2
030100 SEG-TEST-2.                                                      SG1014.2
030200     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
030300     PERFORM 01.                                                  SG1014.2
030400     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
030500         PERFORM PASS                                             SG1014.2
030600         GO TO SEG-WRITE-2.                                       SG1014.2
030700     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
030800     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
030900     PERFORM FAIL.                                                SG1014.2
031000     GO TO SEG-WRITE-2.                                           SG1014.2
031100 SEG-DELETE-2.                                                    SG1014.2
031200     PERFORM DE-LETE.                                             SG1014.2
031300 SEG-WRITE-2.                                                     SG1014.2
031400     MOVE "SEG-TEST-2  " TO PAR-NAME.                             SG1014.2
031500     PERFORM PRINT-DETAIL.                                        SG1014.2
031600 SEG-TEST-3.                                                      SG1014.2
031700     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
031800     PERFORM 02.                                                  SG1014.2
031900     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
032000         PERFORM PASS                                             SG1014.2
032100         GO TO SEG-WRITE-3.                                       SG1014.2
032200     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
032300     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
032400     PERFORM FAIL.                                                SG1014.2
032500     GO TO SEG-WRITE-3.                                           SG1014.2
032600 SEG-DELETE-3.                                                    SG1014.2
032700     PERFORM DE-LETE.                                             SG1014.2
032800 SEG-WRITE-3.                                                     SG1014.2
032900     MOVE "SEG-TEST-3  " TO PAR-NAME.                             SG1014.2
033000     PERFORM PRINT-DETAIL.                                        SG1014.2
033100 SEG-TEST-4.                                                      SG1014.2
033200     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
033300     PERFORM 03.                                                  SG1014.2
033400     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
033500         PERFORM PASS                                             SG1014.2
033600         GO TO SEG-WRITE-4.                                       SG1014.2
033700     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
033800     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
033900     PERFORM FAIL.                                                SG1014.2
034000     GO TO SEG-WRITE-4.                                           SG1014.2
034100 SEG-DELETE-4.                                                    SG1014.2
034200     PERFORM DE-LETE.                                             SG1014.2
034300 SEG-WRITE-4.                                                     SG1014.2
034400     MOVE "SEG-TEST-4  " TO PAR-NAME.                             SG1014.2
034500     PERFORM PRINT-DETAIL.                                        SG1014.2
034600 SEG-TEST-5.                                                      SG1014.2
034700     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
034800     PERFORM 04.                                                  SG1014.2
034900     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
035000         PERFORM PASS                                             SG1014.2
035100         GO TO SEG-WRITE-5.                                       SG1014.2
035200     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
035300     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
035400     PERFORM FAIL.                                                SG1014.2
035500     GO TO SEG-WRITE-5.                                           SG1014.2
035600 SEG-DELETE-5.                                                    SG1014.2
035700     PERFORM DE-LETE.                                             SG1014.2
035800 SEG-WRITE-5.                                                     SG1014.2
035900     MOVE "SEG-TEST-5  " TO PAR-NAME.                             SG1014.2
036000     PERFORM PRINT-DETAIL.                                        SG1014.2
036100 SEG-TEST-6.                                                      SG1014.2
036200     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
036300     PERFORM 05.                                                  SG1014.2
036400     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
036500         PERFORM PASS                                             SG1014.2
036600         GO TO SEG-WRITE-6.                                       SG1014.2
036700     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
036800     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
036900     PERFORM FAIL.                                                SG1014.2
037000     GO TO SEG-WRITE-6.                                           SG1014.2
037100 SEG-DELETE-6.                                                    SG1014.2
037200     PERFORM DE-LETE.                                             SG1014.2
037300 SEG-WRITE-6.                                                     SG1014.2
037400     MOVE "SEG-TEST-6  " TO PAR-NAME.                             SG1014.2
037500     PERFORM PRINT-DETAIL.                                        SG1014.2
037600 SEG-TEST-7.                                                      SG1014.2
037700     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
037800     PERFORM 06.                                                  SG1014.2
037900     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
038000         PERFORM PASS                                             SG1014.2
038100         GO TO SEG-WRITE-7.                                       SG1014.2
038200     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
038300     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
038400     PERFORM FAIL.                                                SG1014.2
038500     GO TO SEG-WRITE-7.                                           SG1014.2
038600 SEG-DELETE-7.                                                    SG1014.2
038700     PERFORM DE-LETE.                                             SG1014.2
038800 SEG-WRITE-7.                                                     SG1014.2
038900     MOVE "SEG-TEST-7  " TO PAR-NAME.                             SG1014.2
039000     PERFORM PRINT-DETAIL.                                        SG1014.2
039100 SEG-TEST-8.                                                      SG1014.2
039200     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
039300     PERFORM 07.                                                  SG1014.2
039400     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
039500         PERFORM PASS                                             SG1014.2
039600         GO TO SEG-WRITE-8.                                       SG1014.2
039700     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
039800     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
039900     PERFORM FAIL.                                                SG1014.2
040000     GO TO SEG-WRITE-8.                                           SG1014.2
040100 SEG-DELETE-8.                                                    SG1014.2
040200     PERFORM DE-LETE.                                             SG1014.2
040300 SEG-WRITE-8.                                                     SG1014.2
040400     MOVE "SEG-TEST-8  " TO PAR-NAME.                             SG1014.2
040500     PERFORM PRINT-DETAIL.                                        SG1014.2
040600 SEG-TEST-9.                                                      SG1014.2
040700     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
040800     PERFORM 08.                                                  SG1014.2
040900     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
041000         PERFORM PASS                                             SG1014.2
041100         GO TO SEG-WRITE-9.                                       SG1014.2
041200     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
041300     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
041400     PERFORM FAIL.                                                SG1014.2
041500     GO TO SEG-WRITE-9.                                           SG1014.2
041600 SEG-DELETE-9.                                                    SG1014.2
041700     PERFORM DE-LETE.                                             SG1014.2
041800 SEG-WRITE-9.                                                     SG1014.2
041900     MOVE "SEG-TEST-9  " TO PAR-NAME.                             SG1014.2
042000     PERFORM PRINT-DETAIL.                                        SG1014.2
042100 SEG-TEST-10.                                                     SG1014.2
042200     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
042300     PERFORM 09.                                                  SG1014.2
042400     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
042500         PERFORM PASS                                             SG1014.2
042600         GO TO SEG-WRITE-10.                                      SG1014.2
042700     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
042800     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
042900     PERFORM FAIL.                                                SG1014.2
043000     GO TO SEG-WRITE-10.                                          SG1014.2
043100 SEG-DELETE-10.                                                   SG1014.2
043200     PERFORM DE-LETE.                                             SG1014.2
043300 SEG-WRITE-10.                                                    SG1014.2
043400     MOVE "SEG-TEST-10 " TO PAR-NAME.                             SG1014.2
043500     PERFORM PRINT-DETAIL.                                        SG1014.2
043600 SEG-TEST-11.                                                     SG1014.2
043700     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
043800     PERFORM 10.                                                  SG1014.2
043900     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
044000         PERFORM PASS                                             SG1014.2
044100         GO TO SEG-WRITE-11.                                      SG1014.2
044200     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
044300     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
044400     PERFORM FAIL.                                                SG1014.2
044500     GO TO SEG-WRITE-11.                                          SG1014.2
044600 SEG-DELETE-11.                                                   SG1014.2
044700     PERFORM DE-LETE.                                             SG1014.2
044800 SEG-WRITE-11.                                                    SG1014.2
044900     MOVE "SEG-TEST-11 " TO PAR-NAME.                             SG1014.2
045000     PERFORM PRINT-DETAIL.                                        SG1014.2
045100 SEG-TEST-12.                                                     SG1014.2
045200     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
045300     PERFORM 11.                                                  SG1014.2
045400     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
045500         PERFORM PASS                                             SG1014.2
045600         GO TO SEG-WRITE-12.                                      SG1014.2
045700     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
045800     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
045900     PERFORM FAIL.                                                SG1014.2
046000     GO TO SEG-WRITE-12.                                          SG1014.2
046100 SEG-DELETE-12.                                                   SG1014.2
046200     PERFORM DE-LETE.                                             SG1014.2
046300 SEG-WRITE-12.                                                    SG1014.2
046400     MOVE "SEG-TEST-12 " TO PAR-NAME.                             SG1014.2
046500     PERFORM PRINT-DETAIL.                                        SG1014.2
046600 SEG-TEST-13.                                                     SG1014.2
046700     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
046800     PERFORM 12.                                                  SG1014.2
046900     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
047000         PERFORM PASS                                             SG1014.2
047100         GO TO SEG-WRITE-13.                                      SG1014.2
047200     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
047300     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
047400     PERFORM FAIL.                                                SG1014.2
047500     GO TO SEG-WRITE-13.                                          SG1014.2
047600 SEG-DELETE-13.                                                   SG1014.2
047700     PERFORM DE-LETE.                                             SG1014.2
047800 SEG-WRITE-13.                                                    SG1014.2
047900     MOVE "SEG-TEST-13 " TO PAR-NAME.                             SG1014.2
048000     PERFORM PRINT-DETAIL.                                        SG1014.2
048100 SEG-TEST-14.                                                     SG1014.2
048200     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
048300     PERFORM 13.                                                  SG1014.2
048400     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
048500         PERFORM PASS                                             SG1014.2
048600         GO TO SEG-WRITE-14.                                      SG1014.2
048700     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
048800     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
048900     PERFORM FAIL.                                                SG1014.2
049000     GO TO SEG-WRITE-14.                                          SG1014.2
049100 SEG-DELETE-14.                                                   SG1014.2
049200     PERFORM DE-LETE.                                             SG1014.2
049300 SEG-WRITE-14.                                                    SG1014.2
049400     MOVE "SEG-TEST-14 " TO PAR-NAME.                             SG1014.2
049500     PERFORM PRINT-DETAIL.                                        SG1014.2
049600 SEG-TEST-15.                                                     SG1014.2
049700     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
049800     PERFORM 14.                                                  SG1014.2
049900     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
050000         PERFORM PASS                                             SG1014.2
050100         GO TO SEG-WRITE-15.                                      SG1014.2
050200     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
050300     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
050400     PERFORM FAIL.                                                SG1014.2
050500     GO TO SEG-WRITE-15.                                          SG1014.2
050600 SEG-DELETE-15.                                                   SG1014.2
050700     PERFORM DE-LETE.                                             SG1014.2
050800 SEG-WRITE-15.                                                    SG1014.2
050900     MOVE "SEG-TEST-15 " TO PAR-NAME.                             SG1014.2
051000     PERFORM PRINT-DETAIL.                                        SG1014.2
051100 SEG-TEST-16.                                                     SG1014.2
051200     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
051300     PERFORM 15.                                                  SG1014.2
051400     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
051500         PERFORM PASS                                             SG1014.2
051600         GO TO SEG-WRITE-16.                                      SG1014.2
051700     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
051800     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
051900     PERFORM FAIL.                                                SG1014.2
052000     GO TO SEG-WRITE-16.                                          SG1014.2
052100 SEG-DELETE-16.                                                   SG1014.2
052200     PERFORM DE-LETE.                                             SG1014.2
052300 SEG-WRITE-16.                                                    SG1014.2
052400     MOVE "SEG-TEST-16 " TO PAR-NAME.                             SG1014.2
052500     PERFORM PRINT-DETAIL.                                        SG1014.2
052600 SEG-TEST-17.                                                     SG1014.2
052700     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
052800     PERFORM 16.                                                  SG1014.2
052900     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
053000         PERFORM PASS                                             SG1014.2
053100         GO TO SEG-WRITE-17.                                      SG1014.2
053200     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
053300     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
053400     PERFORM FAIL.                                                SG1014.2
053500     GO TO SEG-WRITE-17.                                          SG1014.2
053600 SEG-DELETE-17.                                                   SG1014.2
053700     PERFORM DE-LETE.                                             SG1014.2
053800 SEG-WRITE-17.                                                    SG1014.2
053900     MOVE "SEG-TEST-17 " TO PAR-NAME.                             SG1014.2
054000     PERFORM PRINT-DETAIL.                                        SG1014.2
054100 SEG-TEST-18.                                                     SG1014.2
054200     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
054300     PERFORM 17.                                                  SG1014.2
054400     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
054500         PERFORM PASS                                             SG1014.2
054600         GO TO SEG-WRITE-18.                                      SG1014.2
054700     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
054800     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
054900     PERFORM FAIL.                                                SG1014.2
055000     GO TO SEG-WRITE-18.                                          SG1014.2
055100 SEG-DELETE-18.                                                   SG1014.2
055200     PERFORM DE-LETE.                                             SG1014.2
055300 SEG-WRITE-18.                                                    SG1014.2
055400     MOVE "SEG-TEST-18 " TO PAR-NAME.                             SG1014.2
055500     PERFORM PRINT-DETAIL.                                        SG1014.2
055600 SEG-TEST-19.                                                     SG1014.2
055700     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
055800     PERFORM 18.                                                  SG1014.2
055900     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
056000         PERFORM PASS                                             SG1014.2
056100         GO TO SEG-WRITE-19.                                      SG1014.2
056200     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
056300     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
056400     PERFORM FAIL.                                                SG1014.2
056500     GO TO SEG-WRITE-19.                                          SG1014.2
056600 SEG-DELETE-19.                                                   SG1014.2
056700     PERFORM DE-LETE.                                             SG1014.2
056800 SEG-WRITE-19.                                                    SG1014.2
056900     MOVE "SEG-TEST-19 " TO PAR-NAME.                             SG1014.2
057000     PERFORM PRINT-DETAIL.                                        SG1014.2
057100 SEG-TEST-20.                                                     SG1014.2
057200     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
057300     PERFORM 19.                                                  SG1014.2
057400     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
057500         PERFORM PASS                                             SG1014.2
057600         GO TO SEG-WRITE-20.                                      SG1014.2
057700     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
057800     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
057900     PERFORM FAIL.                                                SG1014.2
058000     GO TO SEG-WRITE-20.                                          SG1014.2
058100 SEG-DELETE-20.                                                   SG1014.2
058200     PERFORM DE-LETE.                                             SG1014.2
058300 SEG-WRITE-20.                                                    SG1014.2
058400     MOVE "SEG-TEST-20 " TO PAR-NAME.                             SG1014.2
058500     PERFORM PRINT-DETAIL.                                        SG1014.2
058600 SEG-TEST-21.                                                     SG1014.2
058700     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
058800     PERFORM 20.                                                  SG1014.2
058900     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
059000         PERFORM PASS                                             SG1014.2
059100         GO TO SEG-WRITE-21.                                      SG1014.2
059200     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
059300     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
059400     PERFORM FAIL.                                                SG1014.2
059500     GO TO SEG-WRITE-21.                                          SG1014.2
059600 SEG-DELETE-21.                                                   SG1014.2
059700     PERFORM DE-LETE.                                             SG1014.2
059800 SEG-WRITE-21.                                                    SG1014.2
059900     MOVE "SEG-TEST-21 " TO PAR-NAME.                             SG1014.2
060000     PERFORM PRINT-DETAIL.                                        SG1014.2
060100 SEG-TEST-22.                                                     SG1014.2
060200     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
060300     PERFORM 21.                                                  SG1014.2
060400     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
060500         PERFORM PASS                                             SG1014.2
060600         GO TO SEG-WRITE-22.                                      SG1014.2
060700     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
060800     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
060900     PERFORM FAIL.                                                SG1014.2
061000     GO TO SEG-WRITE-22.                                          SG1014.2
061100 SEG-DELETE-22.                                                   SG1014.2
061200     PERFORM DE-LETE.                                             SG1014.2
061300 SEG-WRITE-22.                                                    SG1014.2
061400     MOVE "SEG-TEST-22 " TO PAR-NAME.                             SG1014.2
061500     PERFORM PRINT-DETAIL.                                        SG1014.2
061600 SEG-TEST-23.                                                     SG1014.2
061700     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
061800     PERFORM 22.                                                  SG1014.2
061900     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
062000         PERFORM PASS                                             SG1014.2
062100         GO TO SEG-WRITE-23.                                      SG1014.2
062200     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
062300     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
062400     PERFORM FAIL.                                                SG1014.2
062500     GO TO SEG-WRITE-23.                                          SG1014.2
062600 SEG-DELETE-23.                                                   SG1014.2
062700     PERFORM DE-LETE.                                             SG1014.2
062800 SEG-WRITE-23.                                                    SG1014.2
062900     MOVE "SEG-TEST-23 " TO PAR-NAME.                             SG1014.2
063000     PERFORM PRINT-DETAIL.                                        SG1014.2
063100 SEG-TEST-24.                                                     SG1014.2
063200     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
063300     PERFORM 23.                                                  SG1014.2
063400     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
063500         PERFORM PASS                                             SG1014.2
063600         GO TO SEG-WRITE-24.                                      SG1014.2
063700     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
063800     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
063900     PERFORM FAIL.                                                SG1014.2
064000     GO TO SEG-WRITE-24.                                          SG1014.2
064100 SEG-DELETE-24.                                                   SG1014.2
064200     PERFORM DE-LETE.                                             SG1014.2
064300 SEG-WRITE-24.                                                    SG1014.2
064400     MOVE "SEG-TEST-24 " TO PAR-NAME.                             SG1014.2
064500     PERFORM PRINT-DETAIL.                                        SG1014.2
064600 SEG-TEST-25.                                                     SG1014.2
064700     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
064800     PERFORM 24.                                                  SG1014.2
064900     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
065000         PERFORM PASS                                             SG1014.2
065100         GO TO SEG-WRITE-25.                                      SG1014.2
065200     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
065300     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
065400     PERFORM FAIL.                                                SG1014.2
065500     GO TO SEG-WRITE-25.                                          SG1014.2
065600 SEG-DELETE-25.                                                   SG1014.2
065700     PERFORM DE-LETE.                                             SG1014.2
065800 SEG-WRITE-25.                                                    SG1014.2
065900     MOVE "SEG-TEST-25 " TO PAR-NAME.                             SG1014.2
066000     PERFORM PRINT-DETAIL.                                        SG1014.2
066100 SEG-TEST-26.                                                     SG1014.2
066200     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
066300     PERFORM 25.                                                  SG1014.2
066400     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
066500         PERFORM PASS                                             SG1014.2
066600         GO TO SEG-WRITE-26.                                      SG1014.2
066700     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
066800     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
066900     PERFORM FAIL.                                                SG1014.2
067000     GO TO SEG-WRITE-26.                                          SG1014.2
067100 SEG-DELETE-26.                                                   SG1014.2
067200     PERFORM DE-LETE.                                             SG1014.2
067300 SEG-WRITE-26.                                                    SG1014.2
067400     MOVE "SEG-TEST-26 " TO PAR-NAME.                             SG1014.2
067500     PERFORM PRINT-DETAIL.                                        SG1014.2
067600 SEG-TEST-27.                                                     SG1014.2
067700     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
067800     PERFORM 26.                                                  SG1014.2
067900     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
068000         PERFORM PASS                                             SG1014.2
068100         GO TO SEG-WRITE-27.                                      SG1014.2
068200     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
068300     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
068400     PERFORM FAIL.                                                SG1014.2
068500     GO TO SEG-WRITE-27.                                          SG1014.2
068600 SEG-DELETE-27.                                                   SG1014.2
068700     PERFORM DE-LETE.                                             SG1014.2
068800 SEG-WRITE-27.                                                    SG1014.2
068900     MOVE "SEG-TEST-27 " TO PAR-NAME.                             SG1014.2
069000     PERFORM PRINT-DETAIL.                                        SG1014.2
069100 SEG-TEST-28.                                                     SG1014.2
069200     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
069300     PERFORM 27.                                                  SG1014.2
069400     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
069500         PERFORM PASS                                             SG1014.2
069600         GO TO SEG-WRITE-28.                                      SG1014.2
069700     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
069800     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
069900     PERFORM FAIL.                                                SG1014.2
070000     GO TO SEG-WRITE-28.                                          SG1014.2
070100 SEG-DELETE-28.                                                   SG1014.2
070200     PERFORM DE-LETE.                                             SG1014.2
070300 SEG-WRITE-28.                                                    SG1014.2
070400     MOVE "SEG-TEST-28 " TO PAR-NAME.                             SG1014.2
070500     PERFORM PRINT-DETAIL.                                        SG1014.2
070600 SEG-TEST-29.                                                     SG1014.2
070700     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
070800     PERFORM 28.                                                  SG1014.2
070900     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
071000         PERFORM PASS                                             SG1014.2
071100         GO TO SEG-WRITE-29.                                      SG1014.2
071200     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
071300     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
071400     PERFORM FAIL.                                                SG1014.2
071500     GO TO SEG-WRITE-29.                                          SG1014.2
071600 SEG-DELETE-29.                                                   SG1014.2
071700     PERFORM DE-LETE.                                             SG1014.2
071800 SEG-WRITE-29.                                                    SG1014.2
071900     MOVE "SEG-TEST-29 " TO PAR-NAME.                             SG1014.2
072000     PERFORM PRINT-DETAIL.                                        SG1014.2
072100 SEG-TEST-30.                                                     SG1014.2
072200     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
072300     PERFORM 29.                                                  SG1014.2
072400     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
072500         PERFORM PASS                                             SG1014.2
072600         GO TO SEG-WRITE-30.                                      SG1014.2
072700     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
072800     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
072900     PERFORM FAIL.                                                SG1014.2
073000     GO TO SEG-WRITE-30.                                          SG1014.2
073100 SEG-DELETE-30.                                                   SG1014.2
073200     PERFORM DE-LETE.                                             SG1014.2
073300 SEG-WRITE-30.                                                    SG1014.2
073400     MOVE "SEG-TEST-30 " TO PAR-NAME.                             SG1014.2
073500     PERFORM PRINT-DETAIL.                                        SG1014.2
073600 SEG-TEST-31.                                                     SG1014.2
073700     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
073800     PERFORM 30.                                                  SG1014.2
073900     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
074000         PERFORM PASS                                             SG1014.2
074100         GO TO SEG-WRITE-31.                                      SG1014.2
074200     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
074300     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
074400     PERFORM FAIL.                                                SG1014.2
074500     GO TO SEG-WRITE-31.                                          SG1014.2
074600 SEG-DELETE-31.                                                   SG1014.2
074700     PERFORM DE-LETE.                                             SG1014.2
074800 SEG-WRITE-31.                                                    SG1014.2
074900     MOVE "SEG-TEST-31 " TO PAR-NAME.                             SG1014.2
075000     PERFORM PRINT-DETAIL.                                        SG1014.2
075100 SEG-TEST-32.                                                     SG1014.2
075200     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
075300     PERFORM 31.                                                  SG1014.2
075400     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
075500         PERFORM PASS                                             SG1014.2
075600         GO TO SEG-WRITE-32.                                      SG1014.2
075700     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
075800     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
075900     PERFORM FAIL.                                                SG1014.2
076000     GO TO SEG-WRITE-32.                                          SG1014.2
076100 SEG-DELETE-32.                                                   SG1014.2
076200     PERFORM DE-LETE.                                             SG1014.2
076300 SEG-WRITE-32.                                                    SG1014.2
076400     MOVE "SEG-TEST-32 " TO PAR-NAME.                             SG1014.2
076500     PERFORM PRINT-DETAIL.                                        SG1014.2
076600 SEG-TEST-33.                                                     SG1014.2
076700     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
076800     PERFORM 32.                                                  SG1014.2
076900     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
077000         PERFORM PASS                                             SG1014.2
077100         GO TO SEG-WRITE-33.                                      SG1014.2
077200     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
077300     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
077400     PERFORM FAIL.                                                SG1014.2
077500     GO TO SEG-WRITE-33.                                          SG1014.2
077600 SEG-DELETE-33.                                                   SG1014.2
077700     PERFORM DE-LETE.                                             SG1014.2
077800 SEG-WRITE-33.                                                    SG1014.2
077900     MOVE "SEG-TEST-33 " TO PAR-NAME.                             SG1014.2
078000     PERFORM PRINT-DETAIL.                                        SG1014.2
078100 SEG-TEST-34.                                                     SG1014.2
078200     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
078300     PERFORM 33.                                                  SG1014.2
078400     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
078500         PERFORM PASS                                             SG1014.2
078600         GO TO SEG-WRITE-34.                                      SG1014.2
078700     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
078800     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
078900     PERFORM FAIL.                                                SG1014.2
079000     GO TO SEG-WRITE-34.                                          SG1014.2
079100 SEG-DELETE-34.                                                   SG1014.2
079200     PERFORM DE-LETE.                                             SG1014.2
079300 SEG-WRITE-34.                                                    SG1014.2
079400     MOVE "SEG-TEST-34 " TO PAR-NAME.                             SG1014.2
079500     PERFORM PRINT-DETAIL.                                        SG1014.2
079600 SEG-TEST-35.                                                     SG1014.2
079700     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
079800     PERFORM 34.                                                  SG1014.2
079900     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
080000         PERFORM PASS                                             SG1014.2
080100         GO TO SEG-WRITE-35.                                      SG1014.2
080200     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
080300     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
080400     PERFORM FAIL.                                                SG1014.2
080500     GO TO SEG-WRITE-35.                                          SG1014.2
080600 SEG-DELETE-35.                                                   SG1014.2
080700     PERFORM DE-LETE.                                             SG1014.2
080800 SEG-WRITE-35.                                                    SG1014.2
080900     MOVE "SEG-TEST-35 " TO PAR-NAME.                             SG1014.2
081000     PERFORM PRINT-DETAIL.                                        SG1014.2
081100 SEG-TEST-36.                                                     SG1014.2
081200     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
081300     PERFORM 35.                                                  SG1014.2
081400     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
081500         PERFORM PASS                                             SG1014.2
081600         GO TO SEG-WRITE-36.                                      SG1014.2
081700     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
081800     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
081900     PERFORM FAIL.                                                SG1014.2
082000     GO TO SEG-WRITE-36.                                          SG1014.2
082100 SEG-DELETE-36.                                                   SG1014.2
082200     PERFORM DE-LETE.                                             SG1014.2
082300 SEG-WRITE-36.                                                    SG1014.2
082400     MOVE "SEG-TEST-36 " TO PAR-NAME.                             SG1014.2
082500     PERFORM PRINT-DETAIL.                                        SG1014.2
082600 SEG-TEST-37.                                                     SG1014.2
082700     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
082800     PERFORM 36.                                                  SG1014.2
082900     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
083000         PERFORM PASS                                             SG1014.2
083100         GO TO SEG-WRITE-37.                                      SG1014.2
083200     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
083300     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
083400     PERFORM FAIL.                                                SG1014.2
083500     GO TO SEG-WRITE-37.                                          SG1014.2
083600 SEG-DELETE-37.                                                   SG1014.2
083700     PERFORM DE-LETE.                                             SG1014.2
083800 SEG-WRITE-37.                                                    SG1014.2
083900     MOVE "SEG-TEST-37 " TO PAR-NAME.                             SG1014.2
084000     PERFORM PRINT-DETAIL.                                        SG1014.2
084100 SEG-TEST-38.                                                     SG1014.2
084200     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
084300     PERFORM 37.                                                  SG1014.2
084400     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
084500         PERFORM PASS                                             SG1014.2
084600         GO TO SEG-WRITE-38.                                      SG1014.2
084700     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
084800     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
084900     PERFORM FAIL.                                                SG1014.2
085000     GO TO SEG-WRITE-38.                                          SG1014.2
085100 SEG-DELETE-38.                                                   SG1014.2
085200     PERFORM DE-LETE.                                             SG1014.2
085300 SEG-WRITE-38.                                                    SG1014.2
085400     MOVE "SEG-TEST-38 " TO PAR-NAME.                             SG1014.2
085500     PERFORM PRINT-DETAIL.                                        SG1014.2
085600 SEG-TEST-39.                                                     SG1014.2
085700     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
085800     PERFORM 38.                                                  SG1014.2
085900     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
086000         PERFORM PASS                                             SG1014.2
086100         GO TO SEG-WRITE-39.                                      SG1014.2
086200     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
086300     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
086400     PERFORM FAIL.                                                SG1014.2
086500     GO TO SEG-WRITE-39.                                          SG1014.2
086600 SEG-DELETE-39.                                                   SG1014.2
086700     PERFORM DE-LETE.                                             SG1014.2
086800 SEG-WRITE-39.                                                    SG1014.2
086900     MOVE "SEG-TEST-39 " TO PAR-NAME.                             SG1014.2
087000     PERFORM PRINT-DETAIL.                                        SG1014.2
087100 SEG-TEST-40.                                                     SG1014.2
087200     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
087300     PERFORM 39.                                                  SG1014.2
087400     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
087500         PERFORM PASS                                             SG1014.2
087600         GO TO SEG-WRITE-40.                                      SG1014.2
087700     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
087800     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
087900     PERFORM FAIL.                                                SG1014.2
088000     GO TO SEG-WRITE-40.                                          SG1014.2
088100 SEG-DELETE-40.                                                   SG1014.2
088200     PERFORM DE-LETE.                                             SG1014.2
088300 SEG-WRITE-40.                                                    SG1014.2
088400     MOVE "SEG-TEST-40 " TO PAR-NAME.                             SG1014.2
088500     PERFORM PRINT-DETAIL.                                        SG1014.2
088600 SEG-TEST-41.                                                     SG1014.2
088700     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
088800     PERFORM 40.                                                  SG1014.2
088900     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
089000         PERFORM PASS                                             SG1014.2
089100         GO TO SEG-WRITE-41.                                      SG1014.2
089200     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
089300     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
089400     PERFORM FAIL.                                                SG1014.2
089500     GO TO SEG-WRITE-41.                                          SG1014.2
089600 SEG-DELETE-41.                                                   SG1014.2
089700     PERFORM DE-LETE.                                             SG1014.2
089800 SEG-WRITE-41.                                                    SG1014.2
089900     MOVE "SEG-TEST-41 " TO PAR-NAME.                             SG1014.2
090000     PERFORM PRINT-DETAIL.                                        SG1014.2
090100 SEG-TEST-42.                                                     SG1014.2
090200     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
090300     PERFORM 41.                                                  SG1014.2
090400     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
090500         PERFORM PASS                                             SG1014.2
090600         GO TO SEG-WRITE-42.                                      SG1014.2
090700     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
090800     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
090900     PERFORM FAIL.                                                SG1014.2
091000     GO TO SEG-WRITE-42.                                          SG1014.2
091100 SEG-DELETE-42.                                                   SG1014.2
091200     PERFORM DE-LETE.                                             SG1014.2
091300 SEG-WRITE-42.                                                    SG1014.2
091400     MOVE "SEG-TEST-42 " TO PAR-NAME.                             SG1014.2
091500     PERFORM PRINT-DETAIL.                                        SG1014.2
091600 SEG-TEST-43.                                                     SG1014.2
091700     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
091800     PERFORM 42.                                                  SG1014.2
091900     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
092000         PERFORM PASS                                             SG1014.2
092100         GO TO SEG-WRITE-43.                                      SG1014.2
092200     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
092300     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
092400     PERFORM FAIL.                                                SG1014.2
092500     GO TO SEG-WRITE-43.                                          SG1014.2
092600 SEG-DELETE-43.                                                   SG1014.2
092700     PERFORM DE-LETE.                                             SG1014.2
092800 SEG-WRITE-43.                                                    SG1014.2
092900     MOVE "SEG-TEST-43 " TO PAR-NAME.                             SG1014.2
093000     PERFORM PRINT-DETAIL.                                        SG1014.2
093100 SEG-TEST-44.                                                     SG1014.2
093200     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
093300     PERFORM 43.                                                  SG1014.2
093400     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
093500         PERFORM PASS                                             SG1014.2
093600         GO TO SEG-WRITE-44.                                      SG1014.2
093700     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
093800     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
093900     PERFORM FAIL.                                                SG1014.2
094000     GO TO SEG-WRITE-44.                                          SG1014.2
094100 SEG-DELETE-44.                                                   SG1014.2
094200     PERFORM DE-LETE.                                             SG1014.2
094300 SEG-WRITE-44.                                                    SG1014.2
094400     MOVE "SEG-TEST-44 " TO PAR-NAME.                             SG1014.2
094500     PERFORM PRINT-DETAIL.                                        SG1014.2
094600 SEG-TEST-45.                                                     SG1014.2
094700     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
094800     PERFORM 44.                                                  SG1014.2
094900     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
095000         PERFORM PASS                                             SG1014.2
095100         GO TO SEG-WRITE-45.                                      SG1014.2
095200     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
095300     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
095400     PERFORM FAIL.                                                SG1014.2
095500     GO TO SEG-WRITE-45.                                          SG1014.2
095600 SEG-DELETE-45.                                                   SG1014.2
095700     PERFORM DE-LETE.                                             SG1014.2
095800 SEG-WRITE-45.                                                    SG1014.2
095900     MOVE "SEG-TEST-45 " TO PAR-NAME.                             SG1014.2
096000     PERFORM PRINT-DETAIL.                                        SG1014.2
096100 SEG-TEST-46.                                                     SG1014.2
096200     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
096300     PERFORM 45.                                                  SG1014.2
096400     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
096500         PERFORM PASS                                             SG1014.2
096600         GO TO SEG-WRITE-46.                                      SG1014.2
096700     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
096800     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
096900     PERFORM FAIL.                                                SG1014.2
097000     GO TO SEG-WRITE-46.                                          SG1014.2
097100 SEG-DELETE-46.                                                   SG1014.2
097200     PERFORM DE-LETE.                                             SG1014.2
097300 SEG-WRITE-46.                                                    SG1014.2
097400     MOVE "SEG-TEST-46 " TO PAR-NAME.                             SG1014.2
097500     PERFORM PRINT-DETAIL.                                        SG1014.2
097600 SEG-TEST-47.                                                     SG1014.2
097700     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
097800     PERFORM 46.                                                  SG1014.2
097900     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
098000         PERFORM PASS                                             SG1014.2
098100         GO TO SEG-WRITE-47.                                      SG1014.2
098200     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
098300     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
098400     PERFORM FAIL.                                                SG1014.2
098500     GO TO SEG-WRITE-47.                                          SG1014.2
098600 SEG-DELETE-47.                                                   SG1014.2
098700     PERFORM DE-LETE.                                             SG1014.2
098800 SEG-WRITE-47.                                                    SG1014.2
098900     MOVE "SEG-TEST-47 " TO PAR-NAME.                             SG1014.2
099000     PERFORM PRINT-DETAIL.                                        SG1014.2
099100 SEG-TEST-48.                                                     SG1014.2
099200     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
099300     PERFORM 47.                                                  SG1014.2
099400     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
099500         PERFORM PASS                                             SG1014.2
099600         GO TO SEG-WRITE-48.                                      SG1014.2
099700     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
099800     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
099900     PERFORM FAIL.                                                SG1014.2
100000     GO TO SEG-WRITE-48.                                          SG1014.2
100100 SEG-DELETE-48.                                                   SG1014.2
100200     PERFORM DE-LETE.                                             SG1014.2
100300 SEG-WRITE-48.                                                    SG1014.2
100400     MOVE "SEG-TEST-48 " TO PAR-NAME.                             SG1014.2
100500     PERFORM PRINT-DETAIL.                                        SG1014.2
100600 SEG-TEST-49.                                                     SG1014.2
100700     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
100800     PERFORM 48.                                                  SG1014.2
100900     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
101000         PERFORM PASS                                             SG1014.2
101100         GO TO SEG-WRITE-49.                                      SG1014.2
101200     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
101300     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
101400     PERFORM FAIL.                                                SG1014.2
101500     GO TO SEG-WRITE-49.                                          SG1014.2
101600 SEG-DELETE-49.                                                   SG1014.2
101700     PERFORM DE-LETE.                                             SG1014.2
101800 SEG-WRITE-49.                                                    SG1014.2
101900     MOVE "SEG-TEST-49 " TO PAR-NAME.                             SG1014.2
102000     PERFORM PRINT-DETAIL.                                        SG1014.2
102100 SEG-TEST-50.                                                     SG1014.2
102200     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
102300     PERFORM 49.                                                  SG1014.2
102400     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
102500         PERFORM PASS                                             SG1014.2
102600         GO TO SEG-WRITE-50.                                      SG1014.2
102700     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
102800     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
102900     PERFORM FAIL.                                                SG1014.2
103000     GO TO SEG-WRITE-50.                                          SG1014.2
103100 SEG-DELETE-50.                                                   SG1014.2
103200     PERFORM DE-LETE.                                             SG1014.2
103300 SEG-WRITE-50.                                                    SG1014.2
103400     MOVE "SEG-TEST-50 " TO PAR-NAME.                             SG1014.2
103500     PERFORM PRINT-DETAIL.                                        SG1014.2
103600 SEG-TEST-51.                                                     SG1014.2
103700     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
103800     PERFORM 50.                                                  SG1014.2
103900     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
104000         PERFORM PASS                                             SG1014.2
104100         GO TO SEG-WRITE-51.                                      SG1014.2
104200     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
104300     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
104400     PERFORM FAIL.                                                SG1014.2
104500     GO TO SEG-WRITE-51.                                          SG1014.2
104600 SEG-DELETE-51.                                                   SG1014.2
104700     PERFORM DE-LETE.                                             SG1014.2
104800 SEG-WRITE-51.                                                    SG1014.2
104900     MOVE "SEG-TEST-51 " TO PAR-NAME.                             SG1014.2
105000     PERFORM PRINT-DETAIL.                                        SG1014.2
105100 SEG-TEST-52.                                                     SG1014.2
105200     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
105300     PERFORM 51.                                                  SG1014.2
105400     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
105500         PERFORM PASS                                             SG1014.2
105600         GO TO SEG-WRITE-52.                                      SG1014.2
105700     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
105800     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
105900     PERFORM FAIL.                                                SG1014.2
106000     GO TO SEG-WRITE-52.                                          SG1014.2
106100 SEG-DELETE-52.                                                   SG1014.2
106200     PERFORM DE-LETE.                                             SG1014.2
106300 SEG-WRITE-52.                                                    SG1014.2
106400     MOVE "SEG-TEST-52 " TO PAR-NAME.                             SG1014.2
106500     PERFORM PRINT-DETAIL.                                        SG1014.2
106600 SEG-TEST-53.                                                     SG1014.2
106700     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
106800     PERFORM 52.                                                  SG1014.2
106900     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
107000         PERFORM PASS                                             SG1014.2
107100         GO TO SEG-WRITE-53.                                      SG1014.2
107200     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
107300     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
107400     PERFORM FAIL.                                                SG1014.2
107500     GO TO SEG-WRITE-53.                                          SG1014.2
107600 SEG-DELETE-53.                                                   SG1014.2
107700     PERFORM DE-LETE.                                             SG1014.2
107800 SEG-WRITE-53.                                                    SG1014.2
107900     MOVE "SEG-TEST-53 " TO PAR-NAME.                             SG1014.2
108000     PERFORM PRINT-DETAIL.                                        SG1014.2
108100 SEG-TEST-54.                                                     SG1014.2
108200     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
108300     PERFORM 53.                                                  SG1014.2
108400     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
108500         PERFORM PASS                                             SG1014.2
108600         GO TO SEG-WRITE-54.                                      SG1014.2
108700     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
108800     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
108900     PERFORM FAIL.                                                SG1014.2
109000     GO TO SEG-WRITE-54.                                          SG1014.2
109100 SEG-DELETE-54.                                                   SG1014.2
109200     PERFORM DE-LETE.                                             SG1014.2
109300 SEG-WRITE-54.                                                    SG1014.2
109400     MOVE "SEG-TEST-54 " TO PAR-NAME.                             SG1014.2
109500     PERFORM PRINT-DETAIL.                                        SG1014.2
109600 SEG-TEST-55.                                                     SG1014.2
109700     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
109800     PERFORM 54.                                                  SG1014.2
109900     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
110000         PERFORM PASS                                             SG1014.2
110100         GO TO SEG-WRITE-55.                                      SG1014.2
110200     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
110300     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
110400     PERFORM FAIL.                                                SG1014.2
110500     GO TO SEG-WRITE-55.                                          SG1014.2
110600 SEG-DELETE-55.                                                   SG1014.2
110700     PERFORM DE-LETE.                                             SG1014.2
110800 SEG-WRITE-55.                                                    SG1014.2
110900     MOVE "SEG-TEST-55 " TO PAR-NAME.                             SG1014.2
111000     PERFORM PRINT-DETAIL.                                        SG1014.2
111100 SEG-TEST-56.                                                     SG1014.2
111200     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
111300     PERFORM 55.                                                  SG1014.2
111400     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
111500         PERFORM PASS                                             SG1014.2
111600         GO TO SEG-WRITE-56.                                      SG1014.2
111700     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
111800     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
111900     PERFORM FAIL.                                                SG1014.2
112000     GO TO SEG-WRITE-56.                                          SG1014.2
112100 SEG-DELETE-56.                                                   SG1014.2
112200     PERFORM DE-LETE.                                             SG1014.2
112300 SEG-WRITE-56.                                                    SG1014.2
112400     MOVE "SEG-TEST-56 " TO PAR-NAME.                             SG1014.2
112500     PERFORM PRINT-DETAIL.                                        SG1014.2
112600 SEG-TEST-57.                                                     SG1014.2
112700     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
112800     PERFORM 56.                                                  SG1014.2
112900     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
113000         PERFORM PASS                                             SG1014.2
113100         GO TO SEG-WRITE-57.                                      SG1014.2
113200     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
113300     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
113400     PERFORM FAIL.                                                SG1014.2
113500     GO TO SEG-WRITE-57.                                          SG1014.2
113600 SEG-DELETE-57.                                                   SG1014.2
113700     PERFORM DE-LETE.                                             SG1014.2
113800 SEG-WRITE-57.                                                    SG1014.2
113900     MOVE "SEG-TEST-57 " TO PAR-NAME.                             SG1014.2
114000     PERFORM PRINT-DETAIL.                                        SG1014.2
114100 SEG-TEST-58.                                                     SG1014.2
114200     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
114300     PERFORM 57.                                                  SG1014.2
114400     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
114500         PERFORM PASS                                             SG1014.2
114600         GO TO SEG-WRITE-58.                                      SG1014.2
114700     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
114800     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
114900     PERFORM FAIL.                                                SG1014.2
115000     GO TO SEG-WRITE-58.                                          SG1014.2
115100 SEG-DELETE-58.                                                   SG1014.2
115200     PERFORM DE-LETE.                                             SG1014.2
115300 SEG-WRITE-58.                                                    SG1014.2
115400     MOVE "SEG-TEST-58 " TO PAR-NAME.                             SG1014.2
115500     PERFORM PRINT-DETAIL.                                        SG1014.2
115600 SEG-TEST-59.                                                     SG1014.2
115700     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
115800     PERFORM 58.                                                  SG1014.2
115900     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
116000         PERFORM PASS                                             SG1014.2
116100         GO TO SEG-WRITE-59.                                      SG1014.2
116200     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
116300     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
116400     PERFORM FAIL.                                                SG1014.2
116500     GO TO SEG-WRITE-59.                                          SG1014.2
116600 SEG-DELETE-59.                                                   SG1014.2
116700     PERFORM DE-LETE.                                             SG1014.2
116800 SEG-WRITE-59.                                                    SG1014.2
116900     MOVE "SEG-TEST-59 " TO PAR-NAME.                             SG1014.2
117000     PERFORM PRINT-DETAIL.                                        SG1014.2
117100 SEG-TEST-60.                                                     SG1014.2
117200     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
117300     PERFORM 59.                                                  SG1014.2
117400     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
117500         PERFORM PASS                                             SG1014.2
117600         GO TO SEG-WRITE-60.                                      SG1014.2
117700     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
117800     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
117900     PERFORM FAIL.                                                SG1014.2
118000     GO TO SEG-WRITE-60.                                          SG1014.2
118100 SEG-DELETE-60.                                                   SG1014.2
118200     PERFORM DE-LETE.                                             SG1014.2
118300 SEG-WRITE-60.                                                    SG1014.2
118400     MOVE "SEG-TEST-60 " TO PAR-NAME.                             SG1014.2
118500     PERFORM PRINT-DETAIL.                                        SG1014.2
118600 SEG-TEST-61.                                                     SG1014.2
118700     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
118800     PERFORM 60.                                                  SG1014.2
118900     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
119000         PERFORM PASS                                             SG1014.2
119100         GO TO SEG-WRITE-61.                                      SG1014.2
119200     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
119300     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
119400     PERFORM FAIL.                                                SG1014.2
119500     GO TO SEG-WRITE-61.                                          SG1014.2
119600 SEG-DELETE-61.                                                   SG1014.2
119700     PERFORM DE-LETE.                                             SG1014.2
119800 SEG-WRITE-61.                                                    SG1014.2
119900     MOVE "SEG-TEST-61 " TO PAR-NAME.                             SG1014.2
120000     PERFORM PRINT-DETAIL.                                        SG1014.2
120100 SEG-TEST-62.                                                     SG1014.2
120200     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
120300     PERFORM 61.                                                  SG1014.2
120400     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
120500         PERFORM PASS                                             SG1014.2
120600         GO TO SEG-WRITE-62.                                      SG1014.2
120700     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
120800     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
120900     PERFORM FAIL.                                                SG1014.2
121000     GO TO SEG-WRITE-62.                                          SG1014.2
121100 SEG-DELETE-62.                                                   SG1014.2
121200     PERFORM DE-LETE.                                             SG1014.2
121300 SEG-WRITE-62.                                                    SG1014.2
121400     MOVE "SEG-TEST-62 " TO PAR-NAME.                             SG1014.2
121500     PERFORM PRINT-DETAIL.                                        SG1014.2
121600 SEG-TEST-63.                                                     SG1014.2
121700     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
121800     PERFORM 62.                                                  SG1014.2
121900     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
122000         PERFORM PASS                                             SG1014.2
122100         GO TO SEG-WRITE-63.                                      SG1014.2
122200     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
122300     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
122400     PERFORM FAIL.                                                SG1014.2
122500     GO TO SEG-WRITE-63.                                          SG1014.2
122600 SEG-DELETE-63.                                                   SG1014.2
122700     PERFORM DE-LETE.                                             SG1014.2
122800 SEG-WRITE-63.                                                    SG1014.2
122900     MOVE "SEG-TEST-63 " TO PAR-NAME.                             SG1014.2
123000     PERFORM PRINT-DETAIL.                                        SG1014.2
123100 SEG-TEST-64.                                                     SG1014.2
123200     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
123300     PERFORM 63.                                                  SG1014.2
123400     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
123500         PERFORM PASS                                             SG1014.2
123600         GO TO SEG-WRITE-64.                                      SG1014.2
123700     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
123800     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
123900     PERFORM FAIL.                                                SG1014.2
124000     GO TO SEG-WRITE-64.                                          SG1014.2
124100 SEG-DELETE-64.                                                   SG1014.2
124200     PERFORM DE-LETE.                                             SG1014.2
124300 SEG-WRITE-64.                                                    SG1014.2
124400     MOVE "SEG-TEST-64 " TO PAR-NAME.                             SG1014.2
124500     PERFORM PRINT-DETAIL.                                        SG1014.2
124600 SEG-TEST-65.                                                     SG1014.2
124700     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
124800     PERFORM 64.                                                  SG1014.2
124900     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
125000         PERFORM PASS                                             SG1014.2
125100         GO TO SEG-WRITE-65.                                      SG1014.2
125200     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
125300     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
125400     PERFORM FAIL.                                                SG1014.2
125500     GO TO SEG-WRITE-65.                                          SG1014.2
125600 SEG-DELETE-65.                                                   SG1014.2
125700     PERFORM DE-LETE.                                             SG1014.2
125800 SEG-WRITE-65.                                                    SG1014.2
125900     MOVE "SEG-TEST-65 " TO PAR-NAME.                             SG1014.2
126000     PERFORM PRINT-DETAIL.                                        SG1014.2
126100 SEG-TEST-66.                                                     SG1014.2
126200     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
126300     PERFORM 65.                                                  SG1014.2
126400     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
126500         PERFORM PASS                                             SG1014.2
126600         GO TO SEG-WRITE-66.                                      SG1014.2
126700     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
126800     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
126900     PERFORM FAIL.                                                SG1014.2
127000     GO TO SEG-WRITE-66.                                          SG1014.2
127100 SEG-DELETE-66.                                                   SG1014.2
127200     PERFORM DE-LETE.                                             SG1014.2
127300 SEG-WRITE-66.                                                    SG1014.2
127400     MOVE "SEG-TEST-66 " TO PAR-NAME.                             SG1014.2
127500     PERFORM PRINT-DETAIL.                                        SG1014.2
127600 SEG-TEST-67.                                                     SG1014.2
127700     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
127800     PERFORM 66.                                                  SG1014.2
127900     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
128000         PERFORM PASS                                             SG1014.2
128100         GO TO SEG-WRITE-67.                                      SG1014.2
128200     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
128300     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
128400     PERFORM FAIL.                                                SG1014.2
128500     GO TO SEG-WRITE-67.                                          SG1014.2
128600 SEG-DELETE-67.                                                   SG1014.2
128700     PERFORM DE-LETE.                                             SG1014.2
128800 SEG-WRITE-67.                                                    SG1014.2
128900     MOVE "SEG-TEST-67 " TO PAR-NAME.                             SG1014.2
129000     PERFORM PRINT-DETAIL.                                        SG1014.2
129100 SEG-TEST-68.                                                     SG1014.2
129200     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
129300     PERFORM 67.                                                  SG1014.2
129400     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
129500         PERFORM PASS                                             SG1014.2
129600         GO TO SEG-WRITE-68.                                      SG1014.2
129700     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
129800     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
129900     PERFORM FAIL.                                                SG1014.2
130000     GO TO SEG-WRITE-68.                                          SG1014.2
130100 SEG-DELETE-68.                                                   SG1014.2
130200     PERFORM DE-LETE.                                             SG1014.2
130300 SEG-WRITE-68.                                                    SG1014.2
130400     MOVE "SEG-TEST-68 " TO PAR-NAME.                             SG1014.2
130500     PERFORM PRINT-DETAIL.                                        SG1014.2
130600 SEG-TEST-69.                                                     SG1014.2
130700     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
130800     PERFORM 68.                                                  SG1014.2
130900     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
131000         PERFORM PASS                                             SG1014.2
131100         GO TO SEG-WRITE-69.                                      SG1014.2
131200     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
131300     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
131400     PERFORM FAIL.                                                SG1014.2
131500     GO TO SEG-WRITE-69.                                          SG1014.2
131600 SEG-DELETE-69.                                                   SG1014.2
131700     PERFORM DE-LETE.                                             SG1014.2
131800 SEG-WRITE-69.                                                    SG1014.2
131900     MOVE "SEG-TEST-69 " TO PAR-NAME.                             SG1014.2
132000     PERFORM PRINT-DETAIL.                                        SG1014.2
132100 SEG-TEST-70.                                                     SG1014.2
132200     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
132300     PERFORM 69.                                                  SG1014.2
132400     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
132500         PERFORM PASS                                             SG1014.2
132600         GO TO SEG-WRITE-70.                                      SG1014.2
132700     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
132800     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
132900     PERFORM FAIL.                                                SG1014.2
133000     GO TO SEG-WRITE-70.                                          SG1014.2
133100 SEG-DELETE-70.                                                   SG1014.2
133200     PERFORM DE-LETE.                                             SG1014.2
133300 SEG-WRITE-70.                                                    SG1014.2
133400     MOVE "SEG-TEST-70 " TO PAR-NAME.                             SG1014.2
133500     PERFORM PRINT-DETAIL.                                        SG1014.2
133600 SEG-TEST-71.                                                     SG1014.2
133700     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
133800     PERFORM 70.                                                  SG1014.2
133900     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
134000         PERFORM PASS                                             SG1014.2
134100         GO TO SEG-WRITE-71.                                      SG1014.2
134200     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
134300     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
134400     PERFORM FAIL.                                                SG1014.2
134500     GO TO SEG-WRITE-71.                                          SG1014.2
134600 SEG-DELETE-71.                                                   SG1014.2
134700     PERFORM DE-LETE.                                             SG1014.2
134800 SEG-WRITE-71.                                                    SG1014.2
134900     MOVE "SEG-TEST-71 " TO PAR-NAME.                             SG1014.2
135000     PERFORM PRINT-DETAIL.                                        SG1014.2
135100 SEG-TEST-72.                                                     SG1014.2
135200     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
135300     PERFORM 71.                                                  SG1014.2
135400     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
135500         PERFORM PASS                                             SG1014.2
135600         GO TO SEG-WRITE-72.                                      SG1014.2
135700     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
135800     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
135900     PERFORM FAIL.                                                SG1014.2
136000     GO TO SEG-WRITE-72.                                          SG1014.2
136100 SEG-DELETE-72.                                                   SG1014.2
136200     PERFORM DE-LETE.                                             SG1014.2
136300 SEG-WRITE-72.                                                    SG1014.2
136400     MOVE "SEG-TEST-72 " TO PAR-NAME.                             SG1014.2
136500     PERFORM PRINT-DETAIL.                                        SG1014.2
136600 SEG-TEST-73.                                                     SG1014.2
136700     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
136800     PERFORM 72.                                                  SG1014.2
136900     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
137000         PERFORM PASS                                             SG1014.2
137100         GO TO SEG-WRITE-73.                                      SG1014.2
137200     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
137300     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
137400     PERFORM FAIL.                                                SG1014.2
137500     GO TO SEG-WRITE-73.                                          SG1014.2
137600 SEG-DELETE-73.                                                   SG1014.2
137700     PERFORM DE-LETE.                                             SG1014.2
137800 SEG-WRITE-73.                                                    SG1014.2
137900     MOVE "SEG-TEST-73 " TO PAR-NAME.                             SG1014.2
138000     PERFORM PRINT-DETAIL.                                        SG1014.2
138100 SEG-TEST-74.                                                     SG1014.2
138200     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
138300     PERFORM 73.                                                  SG1014.2
138400     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
138500         PERFORM PASS                                             SG1014.2
138600         GO TO SEG-WRITE-74.                                      SG1014.2
138700     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
138800     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
138900     PERFORM FAIL.                                                SG1014.2
139000     GO TO SEG-WRITE-74.                                          SG1014.2
139100 SEG-DELETE-74.                                                   SG1014.2
139200     PERFORM DE-LETE.                                             SG1014.2
139300 SEG-WRITE-74.                                                    SG1014.2
139400     MOVE "SEG-TEST-74 " TO PAR-NAME.                             SG1014.2
139500     PERFORM PRINT-DETAIL.                                        SG1014.2
139600 SEG-TEST-75.                                                     SG1014.2
139700     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
139800     PERFORM 74.                                                  SG1014.2
139900     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
140000         PERFORM PASS                                             SG1014.2
140100         GO TO SEG-WRITE-75.                                      SG1014.2
140200     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
140300     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
140400     PERFORM FAIL.                                                SG1014.2
140500     GO TO SEG-WRITE-75.                                          SG1014.2
140600 SEG-DELETE-75.                                                   SG1014.2
140700     PERFORM DE-LETE.                                             SG1014.2
140800 SEG-WRITE-75.                                                    SG1014.2
140900     MOVE "SEG-TEST-75 " TO PAR-NAME.                             SG1014.2
141000     PERFORM PRINT-DETAIL.                                        SG1014.2
141100 SEG-TEST-76.                                                     SG1014.2
141200     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
141300     PERFORM 75.                                                  SG1014.2
141400     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
141500         PERFORM PASS                                             SG1014.2
141600         GO TO SEG-WRITE-76.                                      SG1014.2
141700     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
141800     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
141900     PERFORM FAIL.                                                SG1014.2
142000     GO TO SEG-WRITE-76.                                          SG1014.2
142100 SEG-DELETE-76.                                                   SG1014.2
142200     PERFORM DE-LETE.                                             SG1014.2
142300 SEG-WRITE-76.                                                    SG1014.2
142400     MOVE "SEG-TEST-76 " TO PAR-NAME.                             SG1014.2
142500     PERFORM PRINT-DETAIL.                                        SG1014.2
142600 SEG-TEST-77.                                                     SG1014.2
142700     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
142800     PERFORM 76.                                                  SG1014.2
142900     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
143000         PERFORM PASS                                             SG1014.2
143100         GO TO SEG-WRITE-77.                                      SG1014.2
143200     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
143300     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
143400     PERFORM FAIL.                                                SG1014.2
143500     GO TO SEG-WRITE-77.                                          SG1014.2
143600 SEG-DELETE-77.                                                   SG1014.2
143700     PERFORM DE-LETE.                                             SG1014.2
143800 SEG-WRITE-77.                                                    SG1014.2
143900     MOVE "SEG-TEST-77 " TO PAR-NAME.                             SG1014.2
144000     PERFORM PRINT-DETAIL.                                        SG1014.2
144100 SEG-TEST-78.                                                     SG1014.2
144200     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
144300     PERFORM 77.                                                  SG1014.2
144400     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
144500         PERFORM PASS                                             SG1014.2
144600         GO TO SEG-WRITE-78.                                      SG1014.2
144700     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
144800     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
144900     PERFORM FAIL.                                                SG1014.2
145000     GO TO SEG-WRITE-78.                                          SG1014.2
145100 SEG-DELETE-78.                                                   SG1014.2
145200     PERFORM DE-LETE.                                             SG1014.2
145300 SEG-WRITE-78.                                                    SG1014.2
145400     MOVE "SEG-TEST-78 " TO PAR-NAME.                             SG1014.2
145500     PERFORM PRINT-DETAIL.                                        SG1014.2
145600 SEG-TEST-79.                                                     SG1014.2
145700     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
145800     PERFORM 78.                                                  SG1014.2
145900     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
146000         PERFORM PASS                                             SG1014.2
146100         GO TO SEG-WRITE-79.                                      SG1014.2
146200     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
146300     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
146400     PERFORM FAIL.                                                SG1014.2
146500     GO TO SEG-WRITE-79.                                          SG1014.2
146600 SEG-DELETE-79.                                                   SG1014.2
146700     PERFORM DE-LETE.                                             SG1014.2
146800 SEG-WRITE-79.                                                    SG1014.2
146900     MOVE "SEG-TEST-79 " TO PAR-NAME.                             SG1014.2
147000     PERFORM PRINT-DETAIL.                                        SG1014.2
147100 SEG-TEST-80.                                                     SG1014.2
147200     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
147300     PERFORM 79.                                                  SG1014.2
147400     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
147500         PERFORM PASS                                             SG1014.2
147600         GO TO SEG-WRITE-80.                                      SG1014.2
147700     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
147800     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
147900     PERFORM FAIL.                                                SG1014.2
148000     GO TO SEG-WRITE-80.                                          SG1014.2
148100 SEG-DELETE-80.                                                   SG1014.2
148200     PERFORM DE-LETE.                                             SG1014.2
148300 SEG-WRITE-80.                                                    SG1014.2
148400     MOVE "SEG-TEST-80 " TO PAR-NAME.                             SG1014.2
148500     PERFORM PRINT-DETAIL.                                        SG1014.2
148600 SEG-TEST-81.                                                     SG1014.2
148700     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
148800     PERFORM 80.                                                  SG1014.2
148900     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
149000         PERFORM PASS                                             SG1014.2
149100         GO TO SEG-WRITE-81.                                      SG1014.2
149200     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
149300     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
149400     PERFORM FAIL.                                                SG1014.2
149500     GO TO SEG-WRITE-81.                                          SG1014.2
149600 SEG-DELETE-81.                                                   SG1014.2
149700     PERFORM DE-LETE.                                             SG1014.2
149800 SEG-WRITE-81.                                                    SG1014.2
149900     MOVE "SEG-TEST-81 " TO PAR-NAME.                             SG1014.2
150000     PERFORM PRINT-DETAIL.                                        SG1014.2
150100 SEG-TEST-82.                                                     SG1014.2
150200     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
150300     PERFORM 81.                                                  SG1014.2
150400     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
150500         PERFORM PASS                                             SG1014.2
150600         GO TO SEG-WRITE-82.                                      SG1014.2
150700     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
150800     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
150900     PERFORM FAIL.                                                SG1014.2
151000     GO TO SEG-WRITE-82.                                          SG1014.2
151100 SEG-DELETE-82.                                                   SG1014.2
151200     PERFORM DE-LETE.                                             SG1014.2
151300 SEG-WRITE-82.                                                    SG1014.2
151400     MOVE "SEG-TEST-82 " TO PAR-NAME.                             SG1014.2
151500     PERFORM PRINT-DETAIL.                                        SG1014.2
151600 SEG-TEST-83.                                                     SG1014.2
151700     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
151800     PERFORM 82.                                                  SG1014.2
151900     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
152000         PERFORM PASS                                             SG1014.2
152100         GO TO SEG-WRITE-83.                                      SG1014.2
152200     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
152300     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
152400     PERFORM FAIL.                                                SG1014.2
152500     GO TO SEG-WRITE-83.                                          SG1014.2
152600 SEG-DELETE-83.                                                   SG1014.2
152700     PERFORM DE-LETE.                                             SG1014.2
152800 SEG-WRITE-83.                                                    SG1014.2
152900     MOVE "SEG-TEST-83 " TO PAR-NAME.                             SG1014.2
153000     PERFORM PRINT-DETAIL.                                        SG1014.2
153100 SEG-TEST-84.                                                     SG1014.2
153200     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
153300     PERFORM 83.                                                  SG1014.2
153400     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
153500         PERFORM PASS                                             SG1014.2
153600         GO TO SEG-WRITE-84.                                      SG1014.2
153700     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
153800     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
153900     PERFORM FAIL.                                                SG1014.2
154000     GO TO SEG-WRITE-84.                                          SG1014.2
154100 SEG-DELETE-84.                                                   SG1014.2
154200     PERFORM DE-LETE.                                             SG1014.2
154300 SEG-WRITE-84.                                                    SG1014.2
154400     MOVE "SEG-TEST-84 " TO PAR-NAME.                             SG1014.2
154500     PERFORM PRINT-DETAIL.                                        SG1014.2
154600 SEG-TEST-85.                                                     SG1014.2
154700     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
154800     PERFORM 84.                                                  SG1014.2
154900     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
155000         PERFORM PASS                                             SG1014.2
155100         GO TO SEG-WRITE-85.                                      SG1014.2
155200     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
155300     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
155400     PERFORM FAIL.                                                SG1014.2
155500     GO TO SEG-WRITE-85.                                          SG1014.2
155600 SEG-DELETE-85.                                                   SG1014.2
155700     PERFORM DE-LETE.                                             SG1014.2
155800 SEG-WRITE-85.                                                    SG1014.2
155900     MOVE "SEG-TEST-85 " TO PAR-NAME.                             SG1014.2
156000     PERFORM PRINT-DETAIL.                                        SG1014.2
156100 SEG-TEST-86.                                                     SG1014.2
156200     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
156300     PERFORM 85.                                                  SG1014.2
156400     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
156500         PERFORM PASS                                             SG1014.2
156600         GO TO SEG-WRITE-86.                                      SG1014.2
156700     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
156800     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
156900     PERFORM FAIL.                                                SG1014.2
157000     GO TO SEG-WRITE-86.                                          SG1014.2
157100 SEG-DELETE-86.                                                   SG1014.2
157200     PERFORM DE-LETE.                                             SG1014.2
157300 SEG-WRITE-86.                                                    SG1014.2
157400     MOVE "SEG-TEST-86 " TO PAR-NAME.                             SG1014.2
157500     PERFORM PRINT-DETAIL.                                        SG1014.2
157600 SEG-TEST-87.                                                     SG1014.2
157700     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
157800     PERFORM 86.                                                  SG1014.2
157900     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
158000         PERFORM PASS                                             SG1014.2
158100         GO TO SEG-WRITE-87.                                      SG1014.2
158200     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
158300     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
158400     PERFORM FAIL.                                                SG1014.2
158500     GO TO SEG-WRITE-87.                                          SG1014.2
158600 SEG-DELETE-87.                                                   SG1014.2
158700     PERFORM DE-LETE.                                             SG1014.2
158800 SEG-WRITE-87.                                                    SG1014.2
158900     MOVE "SEG-TEST-87 " TO PAR-NAME.                             SG1014.2
159000     PERFORM PRINT-DETAIL.                                        SG1014.2
159100 SEG-TEST-88.                                                     SG1014.2
159200     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
159300     PERFORM 87.                                                  SG1014.2
159400     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
159500         PERFORM PASS                                             SG1014.2
159600         GO TO SEG-WRITE-88.                                      SG1014.2
159700     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
159800     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
159900     PERFORM FAIL.                                                SG1014.2
160000     GO TO SEG-WRITE-88.                                          SG1014.2
160100 SEG-DELETE-88.                                                   SG1014.2
160200     PERFORM DE-LETE.                                             SG1014.2
160300 SEG-WRITE-88.                                                    SG1014.2
160400     MOVE "SEG-TEST-88 " TO PAR-NAME.                             SG1014.2
160500     PERFORM PRINT-DETAIL.                                        SG1014.2
160600 SEG-TEST-89.                                                     SG1014.2
160700     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
160800     PERFORM 88.                                                  SG1014.2
160900     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
161000         PERFORM PASS                                             SG1014.2
161100         GO TO SEG-WRITE-89.                                      SG1014.2
161200     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
161300     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
161400     PERFORM FAIL.                                                SG1014.2
161500     GO TO SEG-WRITE-89.                                          SG1014.2
161600 SEG-DELETE-89.                                                   SG1014.2
161700     PERFORM DE-LETE.                                             SG1014.2
161800 SEG-WRITE-89.                                                    SG1014.2
161900     MOVE "SEG-TEST-89 " TO PAR-NAME.                             SG1014.2
162000     PERFORM PRINT-DETAIL.                                        SG1014.2
162100 SEG-TEST-90.                                                     SG1014.2
162200     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
162300     PERFORM 89.                                                  SG1014.2
162400     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
162500         PERFORM PASS                                             SG1014.2
162600         GO TO SEG-WRITE-90.                                      SG1014.2
162700     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
162800     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
162900     PERFORM FAIL.                                                SG1014.2
163000     GO TO SEG-WRITE-90.                                          SG1014.2
163100 SEG-DELETE-90.                                                   SG1014.2
163200     PERFORM DE-LETE.                                             SG1014.2
163300 SEG-WRITE-90.                                                    SG1014.2
163400     MOVE "SEG-TEST-90 " TO PAR-NAME.                             SG1014.2
163500     PERFORM PRINT-DETAIL.                                        SG1014.2
163600 SEG-TEST-91.                                                     SG1014.2
163700     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
163800     PERFORM 90.                                                  SG1014.2
163900     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
164000         PERFORM PASS                                             SG1014.2
164100         GO TO SEG-WRITE-91.                                      SG1014.2
164200     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
164300     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
164400     PERFORM FAIL.                                                SG1014.2
164500     GO TO SEG-WRITE-91.                                          SG1014.2
164600 SEG-DELETE-91.                                                   SG1014.2
164700     PERFORM DE-LETE.                                             SG1014.2
164800 SEG-WRITE-91.                                                    SG1014.2
164900     MOVE "SEG-TEST-91 " TO PAR-NAME.                             SG1014.2
165000     PERFORM PRINT-DETAIL.                                        SG1014.2
165100 SEG-TEST-92.                                                     SG1014.2
165200     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
165300     PERFORM 91.                                                  SG1014.2
165400     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
165500         PERFORM PASS                                             SG1014.2
165600         GO TO SEG-WRITE-92.                                      SG1014.2
165700     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
165800     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
165900     PERFORM FAIL.                                                SG1014.2
166000     GO TO SEG-WRITE-92.                                          SG1014.2
166100 SEG-DELETE-92.                                                   SG1014.2
166200     PERFORM DE-LETE.                                             SG1014.2
166300 SEG-WRITE-92.                                                    SG1014.2
166400     MOVE "SEG-TEST-92 " TO PAR-NAME.                             SG1014.2
166500     PERFORM PRINT-DETAIL.                                        SG1014.2
166600 SEG-TEST-93.                                                     SG1014.2
166700     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
166800     PERFORM 92.                                                  SG1014.2
166900     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
167000         PERFORM PASS                                             SG1014.2
167100         GO TO SEG-WRITE-93.                                      SG1014.2
167200     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
167300     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
167400     PERFORM FAIL.                                                SG1014.2
167500     GO TO SEG-WRITE-93.                                          SG1014.2
167600 SEG-DELETE-93.                                                   SG1014.2
167700     PERFORM DE-LETE.                                             SG1014.2
167800 SEG-WRITE-93.                                                    SG1014.2
167900     MOVE "SEG-TEST-93 " TO PAR-NAME.                             SG1014.2
168000     PERFORM PRINT-DETAIL.                                        SG1014.2
168100 SEG-TEST-94.                                                     SG1014.2
168200     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
168300     PERFORM 93.                                                  SG1014.2
168400     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
168500         PERFORM PASS                                             SG1014.2
168600         GO TO SEG-WRITE-94.                                      SG1014.2
168700     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
168800     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
168900     PERFORM FAIL.                                                SG1014.2
169000     GO TO SEG-WRITE-94.                                          SG1014.2
169100 SEG-DELETE-94.                                                   SG1014.2
169200     PERFORM DE-LETE.                                             SG1014.2
169300 SEG-WRITE-94.                                                    SG1014.2
169400     MOVE "SEG-TEST-94 " TO PAR-NAME.                             SG1014.2
169500     PERFORM PRINT-DETAIL.                                        SG1014.2
169600 SEG-TEST-95.                                                     SG1014.2
169700     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
169800     PERFORM 94.                                                  SG1014.2
169900     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
170000         PERFORM PASS                                             SG1014.2
170100         GO TO SEG-WRITE-95.                                      SG1014.2
170200     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
170300     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
170400     PERFORM FAIL.                                                SG1014.2
170500     GO TO SEG-WRITE-95.                                          SG1014.2
170600 SEG-DELETE-95.                                                   SG1014.2
170700     PERFORM DE-LETE.                                             SG1014.2
170800 SEG-WRITE-95.                                                    SG1014.2
170900     MOVE "SEG-TEST-95 " TO PAR-NAME.                             SG1014.2
171000     PERFORM PRINT-DETAIL.                                        SG1014.2
171100 SEG-TEST-96.                                                     SG1014.2
171200     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
171300     PERFORM 95.                                                  SG1014.2
171400     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
171500         PERFORM PASS                                             SG1014.2
171600         GO TO SEG-WRITE-96.                                      SG1014.2
171700     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
171800     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
171900     PERFORM FAIL.                                                SG1014.2
172000     GO TO SEG-WRITE-96.                                          SG1014.2
172100 SEG-DELETE-96.                                                   SG1014.2
172200     PERFORM DE-LETE.                                             SG1014.2
172300 SEG-WRITE-96.                                                    SG1014.2
172400     MOVE "SEG-TEST-96 " TO PAR-NAME.                             SG1014.2
172500     PERFORM PRINT-DETAIL.                                        SG1014.2
172600 SEG-TEST-97.                                                     SG1014.2
172700     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
172800     PERFORM 96.                                                  SG1014.2
172900     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
173000         PERFORM PASS                                             SG1014.2
173100         GO TO SEG-WRITE-97.                                      SG1014.2
173200     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
173300     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
173400     PERFORM FAIL.                                                SG1014.2
173500     GO TO SEG-WRITE-97.                                          SG1014.2
173600 SEG-DELETE-97.                                                   SG1014.2
173700     PERFORM DE-LETE.                                             SG1014.2
173800 SEG-WRITE-97.                                                    SG1014.2
173900     MOVE "SEG-TEST-97 " TO PAR-NAME.                             SG1014.2
174000     PERFORM PRINT-DETAIL.                                        SG1014.2
174100 SEG-TEST-98.                                                     SG1014.2
174200     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
174300     PERFORM 97.                                                  SG1014.2
174400     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
174500         PERFORM PASS                                             SG1014.2
174600         GO TO SEG-WRITE-98.                                      SG1014.2
174700     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
174800     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
174900     PERFORM FAIL.                                                SG1014.2
175000     GO TO SEG-WRITE-98.                                          SG1014.2
175100 SEG-DELETE-98.                                                   SG1014.2
175200     PERFORM DE-LETE.                                             SG1014.2
175300 SEG-WRITE-98.                                                    SG1014.2
175400     MOVE "SEG-TEST-98 " TO PAR-NAME.                             SG1014.2
175500     PERFORM PRINT-DETAIL.                                        SG1014.2
175600 SEG-TEST-99.                                                     SG1014.2
175700     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
175800     PERFORM 98.                                                  SG1014.2
175900     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
176000         PERFORM PASS                                             SG1014.2
176100         GO TO SEG-WRITE-99.                                      SG1014.2
176200     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
176300     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
176400     PERFORM FAIL.                                                SG1014.2
176500     GO TO SEG-WRITE-99.                                          SG1014.2
176600 SEG-DELETE-99.                                                   SG1014.2
176700     PERFORM DE-LETE.                                             SG1014.2
176800 SEG-WRITE-99.                                                    SG1014.2
176900     MOVE "SEG-TEST-99 " TO PAR-NAME.                             SG1014.2
177000     PERFORM PRINT-DETAIL.                                        SG1014.2
177100 SEG-TEST-100.                                                    SG1014.2
177200     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
177300     PERFORM 99.                                                  SG1014.2
177400     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
177500         PERFORM PASS                                             SG1014.2
177600         GO TO SEG-WRITE-100.                                     SG1014.2
177700     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
177800     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
177900     PERFORM FAIL.                                                SG1014.2
178000     GO TO SEG-WRITE-100.                                         SG1014.2
178100 SEG-DELETE-100.                                                  SG1014.2
178200     PERFORM DE-LETE.                                             SG1014.2
178300 SEG-WRITE-100.                                                   SG1014.2
178400     MOVE "SEG-TEST-100" TO PAR-NAME.                             SG1014.2
178500     PERFORM PRINT-DETAIL.                                        SG1014.2
178600     GO TO SECOND-HALF.                                           SG1014.2
178700 01  SECTION 01.                                                  SG1014.2
178800 PARA-01.                                                         SG1014.2
178900     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
179000 02  SECTION 02.                                                  SG1014.2
179100 PARA-02.                                                         SG1014.2
179200     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
179300 03  SECTION 03.                                                  SG1014.2
179400 PARA-03.                                                         SG1014.2
179500     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
179600 04  SECTION 04.                                                  SG1014.2
179700 PARA-04.                                                         SG1014.2
179800     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
179900 05  SECTION 05.                                                  SG1014.2
180000 PARA-05.                                                         SG1014.2
180100     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
180200 06  SECTION 06.                                                  SG1014.2
180300 PARA-06.                                                         SG1014.2
180400     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
180500 07  SECTION 07.                                                  SG1014.2
180600 PARA-07.                                                         SG1014.2
180700     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
180800 08  SECTION 08.                                                  SG1014.2
180900 PARA-08.                                                         SG1014.2
181000     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
181100 09  SECTION 09.                                                  SG1014.2
181200 PARA-09.                                                         SG1014.2
181300     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
181400 10  SECTION 10.                                                  SG1014.2
181500 PARA-10.                                                         SG1014.2
181600     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
181700 11  SECTION 11.                                                  SG1014.2
181800 PARA-11.                                                         SG1014.2
181900     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
182000 12  SECTION 12.                                                  SG1014.2
182100 PARA-12.                                                         SG1014.2
182200     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
182300 13  SECTION 13.                                                  SG1014.2
182400 PARA-13.                                                         SG1014.2
182500     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
182600 14  SECTION 14.                                                  SG1014.2
182700 PARA-14.                                                         SG1014.2
182800     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
182900 15  SECTION 15.                                                  SG1014.2
183000 PARA-15.                                                         SG1014.2
183100     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
183200 16  SECTION 16.                                                  SG1014.2
183300 PARA-16.                                                         SG1014.2
183400     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
183500 17  SECTION 17.                                                  SG1014.2
183600 PARA-17.                                                         SG1014.2
183700     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
183800 18  SECTION 18.                                                  SG1014.2
183900 PARA-18.                                                         SG1014.2
184000     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
184100 19  SECTION 19.                                                  SG1014.2
184200 PARA-19.                                                         SG1014.2
184300     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
184400 20  SECTION 20.                                                  SG1014.2
184500 PARA-20.                                                         SG1014.2
184600     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
184700 21  SECTION 21.                                                  SG1014.2
184800 PARA-21.                                                         SG1014.2
184900     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
185000 22  SECTION 22.                                                  SG1014.2
185100 PARA-22.                                                         SG1014.2
185200     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
185300 23  SECTION 23.                                                  SG1014.2
185400 PARA-23.                                                         SG1014.2
185500     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
185600 24  SECTION 24.                                                  SG1014.2
185700 PARA-24.                                                         SG1014.2
185800     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
185900 25  SECTION 25.                                                  SG1014.2
186000 PARA-25.                                                         SG1014.2
186100     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
186200 26  SECTION 26.                                                  SG1014.2
186300 PARA-26.                                                         SG1014.2
186400     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
186500 27  SECTION 27.                                                  SG1014.2
186600 PARA-27.                                                         SG1014.2
186700     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
186800 28  SECTION 28.                                                  SG1014.2
186900 PARA-28.                                                         SG1014.2
187000     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
187100 29  SECTION 29.                                                  SG1014.2
187200 PARA-29.                                                         SG1014.2
187300     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
187400 30  SECTION 30.                                                  SG1014.2
187500 PARA-30.                                                         SG1014.2
187600     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
187700 31  SECTION 31.                                                  SG1014.2
187800 PARA-31.                                                         SG1014.2
187900     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
188000 32  SECTION 32.                                                  SG1014.2
188100 PARA-32.                                                         SG1014.2
188200     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
188300 33  SECTION 33.                                                  SG1014.2
188400 PARA-33.                                                         SG1014.2
188500     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
188600 34  SECTION 34.                                                  SG1014.2
188700 PARA-34.                                                         SG1014.2
188800     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
188900 35  SECTION 35.                                                  SG1014.2
189000 PARA-35.                                                         SG1014.2
189100     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
189200 36  SECTION 36.                                                  SG1014.2
189300 PARA-36.                                                         SG1014.2
189400     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
189500 37  SECTION 37.                                                  SG1014.2
189600 PARA-37.                                                         SG1014.2
189700     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
189800 38  SECTION 38.                                                  SG1014.2
189900 PARA-38.                                                         SG1014.2
190000     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
190100 39  SECTION 39.                                                  SG1014.2
190200 PARA-39.                                                         SG1014.2
190300     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
190400 40  SECTION 40.                                                  SG1014.2
190500 PARA-40.                                                         SG1014.2
190600     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
190700 41  SECTION 41.                                                  SG1014.2
190800 PARA-41.                                                         SG1014.2
190900     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
191000 42  SECTION 42.                                                  SG1014.2
191100 PARA-42.                                                         SG1014.2
191200     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
191300 43  SECTION 43.                                                  SG1014.2
191400 PARA-43.                                                         SG1014.2
191500     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
191600 44  SECTION 44.                                                  SG1014.2
191700 PARA-44.                                                         SG1014.2
191800     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
191900 45  SECTION 45.                                                  SG1014.2
192000 PARA-45.                                                         SG1014.2
192100     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
192200 46  SECTION 46.                                                  SG1014.2
192300 PARA-46.                                                         SG1014.2
192400     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
192500 47  SECTION 47.                                                  SG1014.2
192600 PARA-47.                                                         SG1014.2
192700     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
192800 48  SECTION 48.                                                  SG1014.2
192900 PARA-48.                                                         SG1014.2
193000     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
193100 49  SECTION 49.                                                  SG1014.2
193200 PARA-49.                                                         SG1014.2
193300     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
193400 SECOND-HALF SECTION 50.                                          SG1014.2
193500 SEG-TEST-101.                                                    SG1014.2
193600     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
193700     PERFORM 50.                                                  SG1014.2
193800     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
193900         PERFORM PASS                                             SG1014.2
194000         GO TO SEG-WRITE-101.                                     SG1014.2
194100     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
194200     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
194300     PERFORM FAIL.                                                SG1014.2
194400     GO TO SEG-WRITE-101.                                         SG1014.2
194500 SEG-DELETE-101.                                                  SG1014.2
194600     PERFORM DE-LETE.                                             SG1014.2
194700 SEG-WRITE-101.                                                   SG1014.2
194800     MOVE "SEG-TEST-101" TO PAR-NAME.                             SG1014.2
194900     PERFORM PRINT-DETAIL.                                        SG1014.2
195000 SEG-TEST-102.                                                    SG1014.2
195100     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
195200     PERFORM 49.                                                  SG1014.2
195300     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
195400         PERFORM PASS                                             SG1014.2
195500         GO TO SEG-WRITE-102.                                     SG1014.2
195600     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
195700     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
195800     PERFORM FAIL.                                                SG1014.2
195900     GO TO SEG-WRITE-102.                                         SG1014.2
196000 SEG-DELETE-102.                                                  SG1014.2
196100     PERFORM DE-LETE.                                             SG1014.2
196200 SEG-WRITE-102.                                                   SG1014.2
196300     MOVE "SEG-TEST-102" TO PAR-NAME.                             SG1014.2
196400     PERFORM PRINT-DETAIL.                                        SG1014.2
196500 SEG-TEST-103.                                                    SG1014.2
196600     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
196700     PERFORM 48.                                                  SG1014.2
196800     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
196900         PERFORM PASS                                             SG1014.2
197000         GO TO SEG-WRITE-103.                                     SG1014.2
197100     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
197200     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
197300     PERFORM FAIL.                                                SG1014.2
197400     GO TO SEG-WRITE-103.                                         SG1014.2
197500 SEG-DELETE-103.                                                  SG1014.2
197600     PERFORM DE-LETE.                                             SG1014.2
197700 SEG-WRITE-103.                                                   SG1014.2
197800     MOVE "SEG-TEST-103" TO PAR-NAME.                             SG1014.2
197900     PERFORM PRINT-DETAIL.                                        SG1014.2
198000 SEG-TEST-104.                                                    SG1014.2
198100     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
198200     PERFORM 47.                                                  SG1014.2
198300     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
198400         PERFORM PASS                                             SG1014.2
198500         GO TO SEG-WRITE-104.                                     SG1014.2
198600     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
198700     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
198800     PERFORM FAIL.                                                SG1014.2
198900     GO TO SEG-WRITE-104.                                         SG1014.2
199000 SEG-DELETE-104.                                                  SG1014.2
199100     PERFORM DE-LETE.                                             SG1014.2
199200 SEG-WRITE-104.                                                   SG1014.2
199300     MOVE "SEG-TEST-104" TO PAR-NAME.                             SG1014.2
199400     PERFORM PRINT-DETAIL.                                        SG1014.2
199500 SEG-TEST-105.                                                    SG1014.2
199600     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
199700     PERFORM 46.                                                  SG1014.2
199800     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
199900         PERFORM PASS                                             SG1014.2
200000         GO TO SEG-WRITE-105.                                     SG1014.2
200100     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
200200     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
200300     PERFORM FAIL.                                                SG1014.2
200400     GO TO SEG-WRITE-105.                                         SG1014.2
200500 SEG-DELETE-105.                                                  SG1014.2
200600     PERFORM DE-LETE.                                             SG1014.2
200700 SEG-WRITE-105.                                                   SG1014.2
200800     MOVE "SEG-TEST-105" TO PAR-NAME.                             SG1014.2
200900     PERFORM PRINT-DETAIL.                                        SG1014.2
201000 SEG-TEST-106.                                                    SG1014.2
201100     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
201200     PERFORM 45.                                                  SG1014.2
201300     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
201400         PERFORM PASS                                             SG1014.2
201500         GO TO SEG-WRITE-106.                                     SG1014.2
201600     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
201700     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
201800     PERFORM FAIL.                                                SG1014.2
201900     GO TO SEG-WRITE-106.                                         SG1014.2
202000 SEG-DELETE-106.                                                  SG1014.2
202100     PERFORM DE-LETE.                                             SG1014.2
202200 SEG-WRITE-106.                                                   SG1014.2
202300     MOVE "SEG-TEST-106" TO PAR-NAME.                             SG1014.2
202400     PERFORM PRINT-DETAIL.                                        SG1014.2
202500 SEG-TEST-107.                                                    SG1014.2
202600     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
202700     PERFORM 44.                                                  SG1014.2
202800     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
202900         PERFORM PASS                                             SG1014.2
203000         GO TO SEG-WRITE-107.                                     SG1014.2
203100     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
203200     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
203300     PERFORM FAIL.                                                SG1014.2
203400     GO TO SEG-WRITE-107.                                         SG1014.2
203500 SEG-DELETE-107.                                                  SG1014.2
203600     PERFORM DE-LETE.                                             SG1014.2
203700 SEG-WRITE-107.                                                   SG1014.2
203800     MOVE "SEG-TEST-107" TO PAR-NAME.                             SG1014.2
203900     PERFORM PRINT-DETAIL.                                        SG1014.2
204000 SEG-TEST-108.                                                    SG1014.2
204100     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
204200     PERFORM 43.                                                  SG1014.2
204300     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
204400         PERFORM PASS                                             SG1014.2
204500         GO TO SEG-WRITE-108.                                     SG1014.2
204600     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
204700     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
204800     PERFORM FAIL.                                                SG1014.2
204900     GO TO SEG-WRITE-108.                                         SG1014.2
205000 SEG-DELETE-108.                                                  SG1014.2
205100     PERFORM DE-LETE.                                             SG1014.2
205200 SEG-WRITE-108.                                                   SG1014.2
205300     MOVE "SEG-TEST-108" TO PAR-NAME.                             SG1014.2
205400     PERFORM PRINT-DETAIL.                                        SG1014.2
205500 SEG-TEST-109.                                                    SG1014.2
205600     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
205700     PERFORM 42.                                                  SG1014.2
205800     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
205900         PERFORM PASS                                             SG1014.2
206000         GO TO SEG-WRITE-109.                                     SG1014.2
206100     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
206200     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
206300     PERFORM FAIL.                                                SG1014.2
206400     GO TO SEG-WRITE-109.                                         SG1014.2
206500 SEG-DELETE-109.                                                  SG1014.2
206600     PERFORM DE-LETE.                                             SG1014.2
206700 SEG-WRITE-109.                                                   SG1014.2
206800     MOVE "SEG-TEST-109" TO PAR-NAME.                             SG1014.2
206900     PERFORM PRINT-DETAIL.                                        SG1014.2
207000 SEG-TEST-110.                                                    SG1014.2
207100     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
207200     PERFORM 41.                                                  SG1014.2
207300     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
207400         PERFORM PASS                                             SG1014.2
207500         GO TO SEG-WRITE-110.                                     SG1014.2
207600     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
207700     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
207800     PERFORM FAIL.                                                SG1014.2
207900     GO TO SEG-WRITE-110.                                         SG1014.2
208000 SEG-DELETE-110.                                                  SG1014.2
208100     PERFORM DE-LETE.                                             SG1014.2
208200 SEG-WRITE-110.                                                   SG1014.2
208300     MOVE "SEG-TEST-110" TO PAR-NAME.                             SG1014.2
208400     PERFORM PRINT-DETAIL.                                        SG1014.2
208500 SEG-TEST-111.                                                    SG1014.2
208600     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
208700     PERFORM 40.                                                  SG1014.2
208800     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
208900         PERFORM PASS                                             SG1014.2
209000         GO TO SEG-WRITE-111.                                     SG1014.2
209100     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
209200     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
209300     PERFORM FAIL.                                                SG1014.2
209400     GO TO SEG-WRITE-111.                                         SG1014.2
209500 SEG-DELETE-111.                                                  SG1014.2
209600     PERFORM DE-LETE.                                             SG1014.2
209700 SEG-WRITE-111.                                                   SG1014.2
209800     MOVE "SEG-TEST-111" TO PAR-NAME.                             SG1014.2
209900     PERFORM PRINT-DETAIL.                                        SG1014.2
210000 SEG-TEST-112.                                                    SG1014.2
210100     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
210200     PERFORM 39.                                                  SG1014.2
210300     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
210400         PERFORM PASS                                             SG1014.2
210500         GO TO SEG-WRITE-112.                                     SG1014.2
210600     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
210700     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
210800     PERFORM FAIL.                                                SG1014.2
210900     GO TO SEG-WRITE-112.                                         SG1014.2
211000 SEG-DELETE-112.                                                  SG1014.2
211100     PERFORM DE-LETE.                                             SG1014.2
211200 SEG-WRITE-112.                                                   SG1014.2
211300     MOVE "SEG-TEST-112" TO PAR-NAME.                             SG1014.2
211400     PERFORM PRINT-DETAIL.                                        SG1014.2
211500 SEG-TEST-113.                                                    SG1014.2
211600     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
211700     PERFORM 38.                                                  SG1014.2
211800     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
211900         PERFORM PASS                                             SG1014.2
212000         GO TO SEG-WRITE-113.                                     SG1014.2
212100     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
212200     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
212300     PERFORM FAIL.                                                SG1014.2
212400     GO TO SEG-WRITE-113.                                         SG1014.2
212500 SEG-DELETE-113.                                                  SG1014.2
212600     PERFORM DE-LETE.                                             SG1014.2
212700 SEG-WRITE-113.                                                   SG1014.2
212800     MOVE "SEG-TEST-113" TO PAR-NAME.                             SG1014.2
212900     PERFORM PRINT-DETAIL.                                        SG1014.2
213000 SEG-TEST-114.                                                    SG1014.2
213100     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
213200     PERFORM 37.                                                  SG1014.2
213300     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
213400         PERFORM PASS                                             SG1014.2
213500         GO TO SEG-WRITE-114.                                     SG1014.2
213600     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
213700     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
213800     PERFORM FAIL.                                                SG1014.2
213900     GO TO SEG-WRITE-114.                                         SG1014.2
214000 SEG-DELETE-114.                                                  SG1014.2
214100     PERFORM DE-LETE.                                             SG1014.2
214200 SEG-WRITE-114.                                                   SG1014.2
214300     MOVE "SEG-TEST-114" TO PAR-NAME.                             SG1014.2
214400     PERFORM PRINT-DETAIL.                                        SG1014.2
214500 SEG-TEST-115.                                                    SG1014.2
214600     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
214700     PERFORM 36.                                                  SG1014.2
214800     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
214900         PERFORM PASS                                             SG1014.2
215000         GO TO SEG-WRITE-115.                                     SG1014.2
215100     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
215200     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
215300     PERFORM FAIL.                                                SG1014.2
215400     GO TO SEG-WRITE-115.                                         SG1014.2
215500 SEG-DELETE-115.                                                  SG1014.2
215600     PERFORM DE-LETE.                                             SG1014.2
215700 SEG-WRITE-115.                                                   SG1014.2
215800     MOVE "SEG-TEST-115" TO PAR-NAME.                             SG1014.2
215900     PERFORM PRINT-DETAIL.                                        SG1014.2
216000 SEG-TEST-116.                                                    SG1014.2
216100     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
216200     PERFORM 35.                                                  SG1014.2
216300     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
216400         PERFORM PASS                                             SG1014.2
216500         GO TO SEG-WRITE-116.                                     SG1014.2
216600     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
216700     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
216800     PERFORM FAIL.                                                SG1014.2
216900     GO TO SEG-WRITE-116.                                         SG1014.2
217000 SEG-DELETE-116.                                                  SG1014.2
217100     PERFORM DE-LETE.                                             SG1014.2
217200 SEG-WRITE-116.                                                   SG1014.2
217300     MOVE "SEG-TEST-116" TO PAR-NAME.                             SG1014.2
217400     PERFORM PRINT-DETAIL.                                        SG1014.2
217500 SEG-TEST-117.                                                    SG1014.2
217600     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
217700     PERFORM 34.                                                  SG1014.2
217800     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
217900         PERFORM PASS                                             SG1014.2
218000         GO TO SEG-WRITE-117.                                     SG1014.2
218100     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
218200     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
218300     PERFORM FAIL.                                                SG1014.2
218400     GO TO SEG-WRITE-117.                                         SG1014.2
218500 SEG-DELETE-117.                                                  SG1014.2
218600     PERFORM DE-LETE.                                             SG1014.2
218700 SEG-WRITE-117.                                                   SG1014.2
218800     MOVE "SEG-TEST-117" TO PAR-NAME.                             SG1014.2
218900     PERFORM PRINT-DETAIL.                                        SG1014.2
219000 SEG-TEST-118.                                                    SG1014.2
219100     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
219200     PERFORM 33.                                                  SG1014.2
219300     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
219400         PERFORM PASS                                             SG1014.2
219500         GO TO SEG-WRITE-118.                                     SG1014.2
219600     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
219700     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
219800     PERFORM FAIL.                                                SG1014.2
219900     GO TO SEG-WRITE-118.                                         SG1014.2
220000 SEG-DELETE-118.                                                  SG1014.2
220100     PERFORM DE-LETE.                                             SG1014.2
220200 SEG-WRITE-118.                                                   SG1014.2
220300     MOVE "SEG-TEST-118" TO PAR-NAME.                             SG1014.2
220400     PERFORM PRINT-DETAIL.                                        SG1014.2
220500 SEG-TEST-119.                                                    SG1014.2
220600     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
220700     PERFORM 32.                                                  SG1014.2
220800     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
220900         PERFORM PASS                                             SG1014.2
221000         GO TO SEG-WRITE-119.                                     SG1014.2
221100     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
221200     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
221300     PERFORM FAIL.                                                SG1014.2
221400     GO TO SEG-WRITE-119.                                         SG1014.2
221500 SEG-DELETE-119.                                                  SG1014.2
221600     PERFORM DE-LETE.                                             SG1014.2
221700 SEG-WRITE-119.                                                   SG1014.2
221800     MOVE "SEG-TEST-119" TO PAR-NAME.                             SG1014.2
221900     PERFORM PRINT-DETAIL.                                        SG1014.2
222000 SEG-TEST-120.                                                    SG1014.2
222100     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
222200     PERFORM 31.                                                  SG1014.2
222300     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
222400         PERFORM PASS                                             SG1014.2
222500         GO TO SEG-WRITE-120.                                     SG1014.2
222600     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
222700     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
222800     PERFORM FAIL.                                                SG1014.2
222900     GO TO SEG-WRITE-120.                                         SG1014.2
223000 SEG-DELETE-120.                                                  SG1014.2
223100     PERFORM DE-LETE.                                             SG1014.2
223200 SEG-WRITE-120.                                                   SG1014.2
223300     MOVE "SEG-TEST-120" TO PAR-NAME.                             SG1014.2
223400     PERFORM PRINT-DETAIL.                                        SG1014.2
223500 SEG-TEST-121.                                                    SG1014.2
223600     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
223700     PERFORM 30.                                                  SG1014.2
223800     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
223900         PERFORM PASS                                             SG1014.2
224000         GO TO SEG-WRITE-121.                                     SG1014.2
224100     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
224200     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
224300     PERFORM FAIL.                                                SG1014.2
224400     GO TO SEG-WRITE-121.                                         SG1014.2
224500 SEG-DELETE-121.                                                  SG1014.2
224600     PERFORM DE-LETE.                                             SG1014.2
224700 SEG-WRITE-121.                                                   SG1014.2
224800     MOVE "SEG-TEST-121" TO PAR-NAME.                             SG1014.2
224900     PERFORM PRINT-DETAIL.                                        SG1014.2
225000 SEG-TEST-122.                                                    SG1014.2
225100     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
225200     PERFORM 29.                                                  SG1014.2
225300     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
225400         PERFORM PASS                                             SG1014.2
225500         GO TO SEG-WRITE-122.                                     SG1014.2
225600     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
225700     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
225800     PERFORM FAIL.                                                SG1014.2
225900     GO TO SEG-WRITE-122.                                         SG1014.2
226000 SEG-DELETE-122.                                                  SG1014.2
226100     PERFORM DE-LETE.                                             SG1014.2
226200 SEG-WRITE-122.                                                   SG1014.2
226300     MOVE "SEG-TEST-122" TO PAR-NAME.                             SG1014.2
226400     PERFORM PRINT-DETAIL.                                        SG1014.2
226500 SEG-TEST-123.                                                    SG1014.2
226600     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
226700     PERFORM 28.                                                  SG1014.2
226800     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
226900         PERFORM PASS                                             SG1014.2
227000         GO TO SEG-WRITE-123.                                     SG1014.2
227100     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
227200     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
227300     PERFORM FAIL.                                                SG1014.2
227400     GO TO SEG-WRITE-123.                                         SG1014.2
227500 SEG-DELETE-123.                                                  SG1014.2
227600     PERFORM DE-LETE.                                             SG1014.2
227700 SEG-WRITE-123.                                                   SG1014.2
227800     MOVE "SEG-TEST-123" TO PAR-NAME.                             SG1014.2
227900     PERFORM PRINT-DETAIL.                                        SG1014.2
228000 SEG-TEST-124.                                                    SG1014.2
228100     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
228200     PERFORM 27.                                                  SG1014.2
228300     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
228400         PERFORM PASS                                             SG1014.2
228500         GO TO SEG-WRITE-124.                                     SG1014.2
228600     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
228700     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
228800     PERFORM FAIL.                                                SG1014.2
228900     GO TO SEG-WRITE-124.                                         SG1014.2
229000 SEG-DELETE-124.                                                  SG1014.2
229100     PERFORM DE-LETE.                                             SG1014.2
229200 SEG-WRITE-124.                                                   SG1014.2
229300     MOVE "SEG-TEST-124" TO PAR-NAME.                             SG1014.2
229400     PERFORM PRINT-DETAIL.                                        SG1014.2
229500 SEG-TEST-125.                                                    SG1014.2
229600     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
229700     PERFORM 26.                                                  SG1014.2
229800     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
229900         PERFORM PASS                                             SG1014.2
230000         GO TO SEG-WRITE-125.                                     SG1014.2
230100     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
230200     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
230300     PERFORM FAIL.                                                SG1014.2
230400     GO TO SEG-WRITE-125.                                         SG1014.2
230500 SEG-DELETE-125.                                                  SG1014.2
230600     PERFORM DE-LETE.                                             SG1014.2
230700 SEG-WRITE-125.                                                   SG1014.2
230800     MOVE "SEG-TEST-125" TO PAR-NAME.                             SG1014.2
230900     PERFORM PRINT-DETAIL.                                        SG1014.2
231000 SEG-TEST-126.                                                    SG1014.2
231100     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
231200     PERFORM 25.                                                  SG1014.2
231300     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
231400         PERFORM PASS                                             SG1014.2
231500         GO TO SEG-WRITE-126.                                     SG1014.2
231600     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
231700     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
231800     PERFORM FAIL.                                                SG1014.2
231900     GO TO SEG-WRITE-126.                                         SG1014.2
232000 SEG-DELETE-126.                                                  SG1014.2
232100     PERFORM DE-LETE.                                             SG1014.2
232200 SEG-WRITE-126.                                                   SG1014.2
232300     MOVE "SEG-TEST-126" TO PAR-NAME.                             SG1014.2
232400     PERFORM PRINT-DETAIL.                                        SG1014.2
232500 SEG-TEST-127.                                                    SG1014.2
232600     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
232700     PERFORM 24.                                                  SG1014.2
232800     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
232900         PERFORM PASS                                             SG1014.2
233000         GO TO SEG-WRITE-127.                                     SG1014.2
233100     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
233200     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
233300     PERFORM FAIL.                                                SG1014.2
233400     GO TO SEG-WRITE-127.                                         SG1014.2
233500 SEG-DELETE-127.                                                  SG1014.2
233600     PERFORM DE-LETE.                                             SG1014.2
233700 SEG-WRITE-127.                                                   SG1014.2
233800     MOVE "SEG-TEST-127" TO PAR-NAME.                             SG1014.2
233900     PERFORM PRINT-DETAIL.                                        SG1014.2
234000 SEG-TEST-128.                                                    SG1014.2
234100     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
234200     PERFORM 23.                                                  SG1014.2
234300     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
234400         PERFORM PASS                                             SG1014.2
234500         GO TO SEG-WRITE-128.                                     SG1014.2
234600     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
234700     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
234800     PERFORM FAIL.                                                SG1014.2
234900     GO TO SEG-WRITE-128.                                         SG1014.2
235000 SEG-DELETE-128.                                                  SG1014.2
235100     PERFORM DE-LETE.                                             SG1014.2
235200 SEG-WRITE-128.                                                   SG1014.2
235300     MOVE "SEG-TEST-128" TO PAR-NAME.                             SG1014.2
235400     PERFORM PRINT-DETAIL.                                        SG1014.2
235500 SEG-TEST-129.                                                    SG1014.2
235600     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
235700     PERFORM 22.                                                  SG1014.2
235800     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
235900         PERFORM PASS                                             SG1014.2
236000         GO TO SEG-WRITE-129.                                     SG1014.2
236100     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
236200     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
236300     PERFORM FAIL.                                                SG1014.2
236400     GO TO SEG-WRITE-129.                                         SG1014.2
236500 SEG-DELETE-129.                                                  SG1014.2
236600     PERFORM DE-LETE.                                             SG1014.2
236700 SEG-WRITE-129.                                                   SG1014.2
236800     MOVE "SEG-TEST-129" TO PAR-NAME.                             SG1014.2
236900     PERFORM PRINT-DETAIL.                                        SG1014.2
237000 SEG-TEST-130.                                                    SG1014.2
237100     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
237200     PERFORM 21.                                                  SG1014.2
237300     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
237400         PERFORM PASS                                             SG1014.2
237500         GO TO SEG-WRITE-130.                                     SG1014.2
237600     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
237700     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
237800     PERFORM FAIL.                                                SG1014.2
237900     GO TO SEG-WRITE-130.                                         SG1014.2
238000 SEG-DELETE-130.                                                  SG1014.2
238100     PERFORM DE-LETE.                                             SG1014.2
238200 SEG-WRITE-130.                                                   SG1014.2
238300     MOVE "SEG-TEST-130" TO PAR-NAME.                             SG1014.2
238400     PERFORM PRINT-DETAIL.                                        SG1014.2
238500 SEG-TEST-131.                                                    SG1014.2
238600     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
238700     PERFORM 20.                                                  SG1014.2
238800     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
238900         PERFORM PASS                                             SG1014.2
239000         GO TO SEG-WRITE-131.                                     SG1014.2
239100     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
239200     PERFORM FAIL.                                                SG1014.2
239300     GO TO SEG-WRITE-131.                                         SG1014.2
239400 SEG-DELETE-131.                                                  SG1014.2
239500     PERFORM DE-LETE.                                             SG1014.2
239600 SEG-WRITE-131.                                                   SG1014.2
239700     MOVE "SEG-TEST-131" TO PAR-NAME.                             SG1014.2
239800     PERFORM PRINT-DETAIL.                                        SG1014.2
239900 SEG-TEST-132.                                                    SG1014.2
240000     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
240100     PERFORM 19.                                                  SG1014.2
240200     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
240300         PERFORM PASS                                             SG1014.2
240400         GO TO SEG-WRITE-132.                                     SG1014.2
240500     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
240600     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
240700     PERFORM FAIL.                                                SG1014.2
240800     GO TO SEG-WRITE-132.                                         SG1014.2
240900 SEG-DELETE-132.                                                  SG1014.2
241000     PERFORM DE-LETE.                                             SG1014.2
241100 SEG-WRITE-132.                                                   SG1014.2
241200     MOVE "SEG-TEST-132" TO PAR-NAME.                             SG1014.2
241300     PERFORM PRINT-DETAIL.                                        SG1014.2
241400 SEG-TEST-133.                                                    SG1014.2
241500     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
241600     PERFORM 18.                                                  SG1014.2
241700     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
241800         PERFORM PASS                                             SG1014.2
241900         GO TO SEG-WRITE-133.                                     SG1014.2
242000     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
242100     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
242200     PERFORM FAIL.                                                SG1014.2
242300     GO TO SEG-WRITE-133.                                         SG1014.2
242400 SEG-DELETE-133.                                                  SG1014.2
242500     PERFORM DE-LETE.                                             SG1014.2
242600 SEG-WRITE-133.                                                   SG1014.2
242700     MOVE "SEG-TEST-133" TO PAR-NAME.                             SG1014.2
242800     PERFORM PRINT-DETAIL.                                        SG1014.2
242900 SEG-TEST-134.                                                    SG1014.2
243000     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
243100     PERFORM 17.                                                  SG1014.2
243200     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
243300         PERFORM PASS                                             SG1014.2
243400         GO TO SEG-WRITE-134.                                     SG1014.2
243500     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
243600     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
243700     PERFORM FAIL.                                                SG1014.2
243800     GO TO SEG-WRITE-134.                                         SG1014.2
243900 SEG-DELETE-134.                                                  SG1014.2
244000     PERFORM DE-LETE.                                             SG1014.2
244100 SEG-WRITE-134.                                                   SG1014.2
244200     MOVE "SEG-TEST-134" TO PAR-NAME.                             SG1014.2
244300     PERFORM PRINT-DETAIL.                                        SG1014.2
244400 SEG-TEST-135.                                                    SG1014.2
244500     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
244600     PERFORM 16.                                                  SG1014.2
244700     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
244800         PERFORM PASS                                             SG1014.2
244900         GO TO SEG-WRITE-135.                                     SG1014.2
245000     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
245100     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
245200     PERFORM FAIL.                                                SG1014.2
245300     GO TO SEG-WRITE-135.                                         SG1014.2
245400 SEG-DELETE-135.                                                  SG1014.2
245500     PERFORM DE-LETE.                                             SG1014.2
245600 SEG-WRITE-135.                                                   SG1014.2
245700     MOVE "SEG-TEST-135" TO PAR-NAME.                             SG1014.2
245800     PERFORM PRINT-DETAIL.                                        SG1014.2
245900 SEG-TEST-136.                                                    SG1014.2
246000     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
246100     PERFORM 15.                                                  SG1014.2
246200     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
246300         PERFORM PASS                                             SG1014.2
246400         GO TO SEG-WRITE-136.                                     SG1014.2
246500     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
246600     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
246700     PERFORM FAIL.                                                SG1014.2
246800     GO TO SEG-WRITE-136.                                         SG1014.2
246900 SEG-DELETE-136.                                                  SG1014.2
247000     PERFORM DE-LETE.                                             SG1014.2
247100 SEG-WRITE-136.                                                   SG1014.2
247200     MOVE "SEG-TEST-136" TO PAR-NAME.                             SG1014.2
247300     PERFORM PRINT-DETAIL.                                        SG1014.2
247400 SEG-TEST-137.                                                    SG1014.2
247500     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
247600     PERFORM 14.                                                  SG1014.2
247700     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
247800         PERFORM PASS                                             SG1014.2
247900         GO TO SEG-WRITE-137.                                     SG1014.2
248000     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
248100     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
248200     PERFORM FAIL.                                                SG1014.2
248300     GO TO SEG-WRITE-137.                                         SG1014.2
248400 SEG-DELETE-137.                                                  SG1014.2
248500     PERFORM DE-LETE.                                             SG1014.2
248600 SEG-WRITE-137.                                                   SG1014.2
248700     MOVE "SEG-TEST-137" TO PAR-NAME.                             SG1014.2
248800     PERFORM PRINT-DETAIL.                                        SG1014.2
248900 SEG-TEST-138.                                                    SG1014.2
249000     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
249100     PERFORM 13.                                                  SG1014.2
249200     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
249300         PERFORM PASS                                             SG1014.2
249400         GO TO SEG-WRITE-138.                                     SG1014.2
249500     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
249600     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
249700     PERFORM FAIL.                                                SG1014.2
249800     GO TO SEG-WRITE-138.                                         SG1014.2
249900 SEG-DELETE-138.                                                  SG1014.2
250000     PERFORM DE-LETE.                                             SG1014.2
250100 SEG-WRITE-138.                                                   SG1014.2
250200     MOVE "SEG-TEST-138" TO PAR-NAME.                             SG1014.2
250300     PERFORM PRINT-DETAIL.                                        SG1014.2
250400 SEG-TEST-139.                                                    SG1014.2
250500     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
250600     PERFORM 12.                                                  SG1014.2
250700     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
250800         PERFORM PASS                                             SG1014.2
250900         GO TO SEG-WRITE-139.                                     SG1014.2
251000     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
251100     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
251200     PERFORM FAIL.                                                SG1014.2
251300     GO TO SEG-WRITE-139.                                         SG1014.2
251400 SEG-DELETE-139.                                                  SG1014.2
251500     PERFORM DE-LETE.                                             SG1014.2
251600 SEG-WRITE-139.                                                   SG1014.2
251700     MOVE "SEG-TEST-139" TO PAR-NAME.                             SG1014.2
251800     PERFORM PRINT-DETAIL.                                        SG1014.2
251900 SEG-TEST-140.                                                    SG1014.2
252000     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
252100     PERFORM 11.                                                  SG1014.2
252200     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
252300         PERFORM PASS                                             SG1014.2
252400         GO TO SEG-WRITE-140.                                     SG1014.2
252500     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
252600     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
252700     PERFORM FAIL.                                                SG1014.2
252800     GO TO SEG-WRITE-140.                                         SG1014.2
252900 SEG-DELETE-140.                                                  SG1014.2
253000     PERFORM DE-LETE.                                             SG1014.2
253100 SEG-WRITE-140.                                                   SG1014.2
253200     MOVE "SEG-TEST-140" TO PAR-NAME.                             SG1014.2
253300     PERFORM PRINT-DETAIL.                                        SG1014.2
253400 SEG-TEST-141.                                                    SG1014.2
253500     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
253600     PERFORM 10.                                                  SG1014.2
253700     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
253800         PERFORM PASS                                             SG1014.2
253900         GO TO SEG-WRITE-141.                                     SG1014.2
254000     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
254100     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
254200     PERFORM FAIL.                                                SG1014.2
254300     GO TO SEG-WRITE-141.                                         SG1014.2
254400 SEG-DELETE-141.                                                  SG1014.2
254500     PERFORM DE-LETE.                                             SG1014.2
254600 SEG-WRITE-141.                                                   SG1014.2
254700     MOVE "SEG-TEST-141" TO PAR-NAME.                             SG1014.2
254800     PERFORM PRINT-DETAIL.                                        SG1014.2
254900 SEG-TEST-142.                                                    SG1014.2
255000     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
255100     PERFORM 09.                                                  SG1014.2
255200     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
255300         PERFORM PASS                                             SG1014.2
255400         GO TO SEG-WRITE-142.                                     SG1014.2
255500     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
255600     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
255700     PERFORM FAIL.                                                SG1014.2
255800     GO TO SEG-WRITE-142.                                         SG1014.2
255900 SEG-DELETE-142.                                                  SG1014.2
256000     PERFORM DE-LETE.                                             SG1014.2
256100 SEG-WRITE-142.                                                   SG1014.2
256200     MOVE "SEG-TEST-142" TO PAR-NAME.                             SG1014.2
256300     PERFORM PRINT-DETAIL.                                        SG1014.2
256400 SEG-TEST-143.                                                    SG1014.2
256500     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
256600     PERFORM 08.                                                  SG1014.2
256700     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
256800         PERFORM PASS                                             SG1014.2
256900         GO TO SEG-WRITE-143.                                     SG1014.2
257000     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
257100     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
257200     PERFORM FAIL.                                                SG1014.2
257300     GO TO SEG-WRITE-143.                                         SG1014.2
257400 SEG-DELETE-143.                                                  SG1014.2
257500     PERFORM DE-LETE.                                             SG1014.2
257600 SEG-WRITE-143.                                                   SG1014.2
257700     MOVE "SEG-TEST-143" TO PAR-NAME.                             SG1014.2
257800     PERFORM PRINT-DETAIL.                                        SG1014.2
257900 SEG-TEST-144.                                                    SG1014.2
258000     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
258100     PERFORM 07.                                                  SG1014.2
258200     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
258300         PERFORM PASS                                             SG1014.2
258400         GO TO SEG-WRITE-144.                                     SG1014.2
258500     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
258600     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
258700     PERFORM FAIL.                                                SG1014.2
258800     GO TO SEG-WRITE-144.                                         SG1014.2
258900 SEG-DELETE-144.                                                  SG1014.2
259000     PERFORM DE-LETE.                                             SG1014.2
259100 SEG-WRITE-144.                                                   SG1014.2
259200     MOVE "SEG-TEST-144" TO PAR-NAME.                             SG1014.2
259300     PERFORM PRINT-DETAIL.                                        SG1014.2
259400 SEG-TEST-145.                                                    SG1014.2
259500     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
259600     PERFORM 06.                                                  SG1014.2
259700     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
259800         PERFORM PASS                                             SG1014.2
259900         GO TO SEG-WRITE-145.                                     SG1014.2
260000     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
260100     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
260200     PERFORM FAIL.                                                SG1014.2
260300     GO TO SEG-WRITE-145.                                         SG1014.2
260400 SEG-DELETE-145.                                                  SG1014.2
260500     PERFORM DE-LETE.                                             SG1014.2
260600 SEG-WRITE-145.                                                   SG1014.2
260700     MOVE "SEG-TEST-145" TO PAR-NAME.                             SG1014.2
260800     PERFORM PRINT-DETAIL.                                        SG1014.2
260900 SEG-TEST-146.                                                    SG1014.2
261000     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
261100     PERFORM 05.                                                  SG1014.2
261200     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
261300         PERFORM PASS                                             SG1014.2
261400         GO TO SEG-WRITE-146.                                     SG1014.2
261500     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
261600     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
261700     PERFORM FAIL.                                                SG1014.2
261800     GO TO SEG-WRITE-146.                                         SG1014.2
261900 SEG-DELETE-146.                                                  SG1014.2
262000     PERFORM DE-LETE.                                             SG1014.2
262100 SEG-WRITE-146.                                                   SG1014.2
262200     MOVE "SEG-TEST-146" TO PAR-NAME.                             SG1014.2
262300     PERFORM PRINT-DETAIL.                                        SG1014.2
262400 SEG-TEST-147.                                                    SG1014.2
262500     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
262600     PERFORM 04.                                                  SG1014.2
262700     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
262800         PERFORM PASS                                             SG1014.2
262900         GO TO SEG-WRITE-147.                                     SG1014.2
263000     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
263100     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
263200     PERFORM FAIL.                                                SG1014.2
263300     GO TO SEG-WRITE-147.                                         SG1014.2
263400 SEG-DELETE-147.                                                  SG1014.2
263500     PERFORM DE-LETE.                                             SG1014.2
263600 SEG-WRITE-147.                                                   SG1014.2
263700     MOVE "SEG-TEST-147" TO PAR-NAME.                             SG1014.2
263800     PERFORM PRINT-DETAIL.                                        SG1014.2
263900 SEG-TEST-148.                                                    SG1014.2
264000     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
264100     PERFORM 03.                                                  SG1014.2
264200     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
264300         PERFORM PASS                                             SG1014.2
264400         GO TO SEG-WRITE-148.                                     SG1014.2
264500     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
264600     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
264700     PERFORM FAIL.                                                SG1014.2
264800     GO TO SEG-WRITE-148.                                         SG1014.2
264900 SEG-DELETE-148.                                                  SG1014.2
265000     PERFORM DE-LETE.                                             SG1014.2
265100 SEG-WRITE-148.                                                   SG1014.2
265200     MOVE "SEG-TEST-148" TO PAR-NAME.                             SG1014.2
265300     PERFORM PRINT-DETAIL.                                        SG1014.2
265400 SEG-TEST-149.                                                    SG1014.2
265500     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
265600     PERFORM 02.                                                  SG1014.2
265700     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
265800         PERFORM PASS                                             SG1014.2
265900         GO TO SEG-WRITE-149.                                     SG1014.2
266000     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
266100     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
266200     PERFORM FAIL.                                                SG1014.2
266300     GO TO SEG-WRITE-149.                                         SG1014.2
266400 SEG-DELETE-149.                                                  SG1014.2
266500     PERFORM DE-LETE.                                             SG1014.2
266600 SEG-WRITE-149.                                                   SG1014.2
266700     MOVE "SEG-TEST-149" TO PAR-NAME.                             SG1014.2
266800     PERFORM PRINT-DETAIL.                                        SG1014.2
266900 SEG-TEST-150.                                                    SG1014.2
267000     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
267100     PERFORM 01.                                                  SG1014.2
267200     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
267300         PERFORM PASS                                             SG1014.2
267400         GO TO SEG-WRITE-150.                                     SG1014.2
267500     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
267600     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
267700     PERFORM FAIL.                                                SG1014.2
267800     GO TO SEG-WRITE-150.                                         SG1014.2
267900 SEG-DELETE-150.                                                  SG1014.2
268000     PERFORM DE-LETE.                                             SG1014.2
268100 SEG-WRITE-150.                                                   SG1014.2
268200     MOVE "SEG-TEST-150" TO PAR-NAME.                             SG1014.2
268300     PERFORM PRINT-DETAIL.                                        SG1014.2
268400 SEG-TEST-151.                                                    SG1014.2
268500     MOVE SPACE TO TEST-CHECK.                                    SG1014.2
268600     PERFORM 00.                                                  SG1014.2
268700     IF TEST-CHECK EQUAL TO "GOOD"                                SG1014.2
268800         PERFORM PASS                                             SG1014.2
268900         GO TO SEG-WRITE-151.                                     SG1014.2
269000     MOVE SPACE TO COMPUTED-A.                                    SG1014.2
269100     MOVE "GOOD" TO CORRECT-A.                                    SG1014.2
269200     PERFORM FAIL.                                                SG1014.2
269300     GO TO SEG-WRITE-151.                                         SG1014.2
269400 SEG-DELETE-151.                                                  SG1014.2
269500     PERFORM DE-LETE.                                             SG1014.2
269600 SEG-WRITE-151.                                                   SG1014.2
269700     MOVE "SEG-TEST-151" TO PAR-NAME.                             SG1014.2
269800     PERFORM PRINT-DETAIL.                                        SG1014.2
269900     GO TO     CLOSE-FILES.                                       SG1014.2
270000 50  SECTION 50.                                                  SG1014.2
270100 PARA-50.                                                         SG1014.2
270200     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
270300 51  SECTION 51.                                                  SG1014.2
270400 PARA-51.                                                         SG1014.2
270500     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
270600 52  SECTION 52.                                                  SG1014.2
270700 PARA-52.                                                         SG1014.2
270800     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
270900 53  SECTION 53.                                                  SG1014.2
271000 PARA-53.                                                         SG1014.2
271100     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
271200 54  SECTION 54.                                                  SG1014.2
271300 PARA-54.                                                         SG1014.2
271400     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
271500 55  SECTION 55.                                                  SG1014.2
271600 PARA-55.                                                         SG1014.2
271700     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
271800 56  SECTION 56.                                                  SG1014.2
271900 PARA-56.                                                         SG1014.2
272000     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
272100 57  SECTION 57.                                                  SG1014.2
272200 PARA-57.                                                         SG1014.2
272300     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
272400 58  SECTION 58.                                                  SG1014.2
272500 PARA-58.                                                         SG1014.2
272600     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
272700 59  SECTION 59.                                                  SG1014.2
272800 PARA-59.                                                         SG1014.2
272900     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
273000 60  SECTION 60.                                                  SG1014.2
273100 PARA-60.                                                         SG1014.2
273200     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
273300 61  SECTION 61.                                                  SG1014.2
273400 PARA-61.                                                         SG1014.2
273500     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
273600 62  SECTION 62.                                                  SG1014.2
273700 PARA-62.                                                         SG1014.2
273800     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
273900 63  SECTION 63.                                                  SG1014.2
274000 PARA-63.                                                         SG1014.2
274100     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
274200 64  SECTION 64.                                                  SG1014.2
274300 PARA-64.                                                         SG1014.2
274400     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
274500 65  SECTION 65.                                                  SG1014.2
274600 PARA-65.                                                         SG1014.2
274700     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
274800 66  SECTION 66.                                                  SG1014.2
274900 PARA-66.                                                         SG1014.2
275000     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
275100 67  SECTION 67.                                                  SG1014.2
275200 PARA-67.                                                         SG1014.2
275300     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
275400 68  SECTION 68.                                                  SG1014.2
275500 PARA-68.                                                         SG1014.2
275600     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
275700 69  SECTION 69.                                                  SG1014.2
275800 PARA-69.                                                         SG1014.2
275900     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
276000 70  SECTION 70.                                                  SG1014.2
276100 PARA-70.                                                         SG1014.2
276200     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
276300 71  SECTION 71.                                                  SG1014.2
276400 PARA-71.                                                         SG1014.2
276500     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
276600 72  SECTION 72.                                                  SG1014.2
276700 PARA-72.                                                         SG1014.2
276800     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
276900 73  SECTION 73.                                                  SG1014.2
277000 PARA-73.                                                         SG1014.2
277100     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
277200 74  SECTION 74.                                                  SG1014.2
277300 PARA-74.                                                         SG1014.2
277400     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
277500 75  SECTION 75.                                                  SG1014.2
277600 PARA-75.                                                         SG1014.2
277700     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
277800 76  SECTION 76.                                                  SG1014.2
277900 PARA-76.                                                         SG1014.2
278000     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
278100 77  SECTION 77.                                                  SG1014.2
278200 PARA-77.                                                         SG1014.2
278300     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
278400 78  SECTION 78.                                                  SG1014.2
278500 PARA-78.                                                         SG1014.2
278600     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
278700 79  SECTION 79.                                                  SG1014.2
278800 PARA-79.                                                         SG1014.2
278900     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
279000 80  SECTION 80.                                                  SG1014.2
279100 PARA-80.                                                         SG1014.2
279200     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
279300 81  SECTION 81.                                                  SG1014.2
279400 PARA-81.                                                         SG1014.2
279500     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
279600 82  SECTION 82.                                                  SG1014.2
279700 PARA-82.                                                         SG1014.2
279800     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
279900 83  SECTION 83.                                                  SG1014.2
280000 PARA-83.                                                         SG1014.2
280100     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
280200 84  SECTION 84.                                                  SG1014.2
280300 PARA-84.                                                         SG1014.2
280400     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
280500 85  SECTION 85.                                                  SG1014.2
280600 PARA-85.                                                         SG1014.2
280700     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
280800 86  SECTION 86.                                                  SG1014.2
280900 PARA-86.                                                         SG1014.2
281000     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
281100 87  SECTION 87.                                                  SG1014.2
281200 PARA-87.                                                         SG1014.2
281300     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
281400 88  SECTION 88.                                                  SG1014.2
281500 PARA-88.                                                         SG1014.2
281600     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
281700 89  SECTION 89.                                                  SG1014.2
281800 PARA-89.                                                         SG1014.2
281900     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
282000 90  SECTION 90.                                                  SG1014.2
282100 PARA-90.                                                         SG1014.2
282200     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
282300 91  SECTION 91.                                                  SG1014.2
282400 PARA-91.                                                         SG1014.2
282500     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
282600 92  SECTION 92.                                                  SG1014.2
282700 PARA-92.                                                         SG1014.2
282800     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
282900 93  SECTION 93.                                                  SG1014.2
283000 PARA-93.                                                         SG1014.2
283100     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
283200 94  SECTION 94.                                                  SG1014.2
283300 PARA-94.                                                         SG1014.2
283400     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
283500 95  SECTION 95.                                                  SG1014.2
283600 PARA-95.                                                         SG1014.2
283700     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
283800 96  SECTION 96.                                                  SG1014.2
283900 PARA-96.                                                         SG1014.2
284000     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
284100 97  SECTION 97.                                                  SG1014.2
284200 PARA-97.                                                         SG1014.2
284300     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
284400 98  SECTION 98.                                                  SG1014.2
284500 PARA-98.                                                         SG1014.2
284600     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
284700 99  SECTION 99.                                                  SG1014.2
284800 PARA-99.                                                         SG1014.2
284900     MOVE "GOOD" TO TEST-CHECK.                                   SG1014.2
      *END-OF,SG101A                                                                  
