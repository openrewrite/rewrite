      *HEADER,COBOL,SG103A                                                            
000100 IDENTIFICATION DIVISION.                                         SG1034.2
000200 PROGRAM-ID.                                                      SG1034.2
000300     SG103A.                                                      SG1034.2
000400 AUTHOR.                                                          SG1034.2
000500     FEDERAL COMPILER TESTING CENTER.                             SG1034.2
000600 INSTALLATION.                                                    SG1034.2
000700     GENERAL SERVICES ADMINISTRATION                              SG1034.2
000800*     AUTOMATED DATA AND TELECOMMUNICATION SERVICE.                SG1034.2
000900     SOFTWARE DEVELOPMENT OFFICE.                                 SG1034.2
001000     5203 LEESBURG PIKE  SUITE 1100                               SG1034.2
001100     FALLS CHURCH VIRGINIA 22041.                                 SG1034.2
001200                                                                  SG1034.2
001300     PHONE   (703) 756-6153                                       SG1034.2
001400                                                                  SG1034.2
001500     " HIGH       ".                                              SG1034.2
001600 DATE-WRITTEN.                                                    SG1034.2
001700     CCVS-74 VERSION 4.0 - 1980 JULY 1.                           SG1034.2
001800     CREATION DATE     /    VALIDATION DATE                       SG1034.2
001900     "4.2 ".                                                      SG1034.2
002000 SECURITY.                                                        SG1034.2
002100     NONE.                                                        SG1034.2
002200       THE FOLLOWING FEATURES ARE TESTED BY THIS PROGRAM ---      SG1034.2
002300         THE ALTER, PERFORM, AND GO TO STATEMENTS ARE USED        SG1034.2
002400         TO CHECK INITIAL AND LAST-USED STATES.                   SG1034.2
002500                                                                  SG1034.2
002600 ENVIRONMENT DIVISION.                                            SG1034.2
002700 CONFIGURATION SECTION.                                           SG1034.2
002800 SOURCE-COMPUTER.                                                 SG1034.2
002900     XXXXX082.                                                    SG1034.2
003000 OBJECT-COMPUTER.                                                 SG1034.2
003100     XXXXX083.                                                    SG1034.2
003200 INPUT-OUTPUT SECTION.                                            SG1034.2
003300 FILE-CONTROL.                                                    SG1034.2
003400     SELECT PRINT-FILE ASSIGN TO                                  SG1034.2
003500     XXXXX055.                                                    SG1034.2
003600 DATA DIVISION.                                                   SG1034.2
003700 FILE SECTION.                                                    SG1034.2
003800 FD  PRINT-FILE                                                   SG1034.2
003900     LABEL RECORDS                                                SG1034.2
004000     XXXXX084                                                     SG1034.2
004100     DATA RECORD IS PRINT-REC DUMMY-RECORD.                       SG1034.2
004200 01  PRINT-REC PICTURE X(120).                                    SG1034.2
004300 01  DUMMY-RECORD PICTURE X(120).                                 SG1034.2
004400 WORKING-STORAGE SECTION.                                         SG1034.2
004500 77  ENT-COUNTER      PIC 9 VALUE ZERO.                           SG1034.2
004600 01  INITIAL-STATE-A PICTURE 9 VALUE 0.                           SG1034.2
004700 01  GO-TO-IND          PICTURE X           VALUE " ".            SG1034.2
004800 01  PERFORM-RSLT.                                                SG1034.2
004900     02 PERFORM-RSLT-1  PICTURE X           VALUE " ".            SG1034.2
005000     02 PERFORM-RSLT-2  PICTURE X           VALUE " ".            SG1034.2
005100     02 PERFORM-RSLT-3  PICTURE X           VALUE " ".            SG1034.2
005200     02 PERFORM-RSLT-4  PICTURE X           VALUE " ".            SG1034.2
005300 01  ALTER-RSLT.                                                  SG1034.2
005400     02 ALTER-RSLT-1    PICTURE X           VALUE " ".            SG1034.2
005500     02 ALTER-RSLT-2    PICTURE X           VALUE " ".            SG1034.2
005600     02 ALTER-RSLT-3    PICTURE X           VALUE " ".            SG1034.2
005700 01  FALL-RSLT.                                                   SG1034.2
005800     02 FALL-RSLT-1     PICTURE X           VALUE " ".            SG1034.2
005900     02 FALL-RSLT-2     PICTURE X           VALUE " ".            SG1034.2
006000 01  TEST-RESULTS.                                                SG1034.2
006100     02 FILLER                    PICTURE X VALUE SPACE.          SG1034.2
006200     02 FEATURE                   PICTURE X(20) VALUE SPACE.      SG1034.2
006300     02 FILLER                    PICTURE X VALUE SPACE.          SG1034.2
006400     02 P-OR-F                    PICTURE X(5) VALUE SPACE.       SG1034.2
006500     02 FILLER                    PICTURE X  VALUE SPACE.         SG1034.2
006600     02  PAR-NAME.                                                SG1034.2
006700       03 FILLER PICTURE X(12) VALUE SPACE.                       SG1034.2
006800       03  PARDOT-X PICTURE X  VALUE SPACE.                       SG1034.2
006900       03 DOTVALUE PICTURE 99  VALUE ZERO.                        SG1034.2
007000       03 FILLER PIC X(5) VALUE SPACE.                            SG1034.2
007100     02 FILLER PIC X(10) VALUE SPACE.                             SG1034.2
007200     02 RE-MARK PIC X(61).                                        SG1034.2
007300 01  TEST-COMPUTED.                                               SG1034.2
007400     02 FILLER PIC X(30) VALUE SPACE.                             SG1034.2
007500     02 FILLER PIC X(17) VALUE "       COMPUTED=".                SG1034.2
007600     02 COMPUTED-X.                                               SG1034.2
007700     03 COMPUTED-A                PICTURE X(20) VALUE SPACE.      SG1034.2
007800     03 COMPUTED-N REDEFINES COMPUTED-A PICTURE -9(9).9(9).       SG1034.2
007900     03 COMPUTED-0V18 REDEFINES COMPUTED-A  PICTURE -.9(18).      SG1034.2
008000     03 COMPUTED-4V14 REDEFINES COMPUTED-A  PICTURE -9(4).9(14).  SG1034.2
008100     03 COMPUTED-14V4 REDEFINES COMPUTED-A  PICTURE -9(14).9(4).  SG1034.2
008200     03       CM-18V0 REDEFINES COMPUTED-A.                       SG1034.2
008300         04 COMPUTED-18V0                   PICTURE -9(18).       SG1034.2
008400         04 FILLER                          PICTURE X.            SG1034.2
008500     03 FILLER PIC X(50) VALUE SPACE.                             SG1034.2
008600 01  TEST-CORRECT.                                                SG1034.2
008700     02 FILLER PIC X(30) VALUE SPACE.                             SG1034.2
008800     02 FILLER PIC X(17) VALUE "       CORRECT =".                SG1034.2
008900     02 CORRECT-X.                                                SG1034.2
009000     03 CORRECT-A                 PICTURE X(20) VALUE SPACE.      SG1034.2
009100     03 CORRECT-N REDEFINES CORRECT-A PICTURE -9(9).9(9).         SG1034.2
009200     03 CORRECT-0V18 REDEFINES CORRECT-A    PICTURE -.9(18).      SG1034.2
009300     03 CORRECT-4V14 REDEFINES CORRECT-A    PICTURE -9(4).9(14).  SG1034.2
009400     03 CORRECT-14V4 REDEFINES CORRECT-A    PICTURE -9(14).9(4).  SG1034.2
009500     03      CR-18V0 REDEFINES CORRECT-A.                         SG1034.2
009600         04 CORRECT-18V0                    PICTURE -9(18).       SG1034.2
009700         04 FILLER                          PICTURE X.            SG1034.2
009800     03 FILLER PIC X(50) VALUE SPACE.                             SG1034.2
009900 01  CCVS-C-1.                                                    SG1034.2
010000     02 FILLER PICTURE IS X(99) VALUE IS " FEATURE              PASG1034.2
010100-    "SS  PARAGRAPH-NAME                                          SG1034.2
010200-    "        REMARKS".                                           SG1034.2
010300     02 FILLER PICTURE IS X(20) VALUE IS SPACE.                   SG1034.2
010400 01  CCVS-C-2.                                                    SG1034.2
010500     02 FILLER PICTURE IS X VALUE IS SPACE.                       SG1034.2
010600     02 FILLER PICTURE IS X(6) VALUE IS "TESTED".                 SG1034.2
010700     02 FILLER PICTURE IS X(15) VALUE IS SPACE.                   SG1034.2
010800     02 FILLER PICTURE IS X(4) VALUE IS "FAIL".                   SG1034.2
010900     02 FILLER PICTURE IS X(94) VALUE IS SPACE.                   SG1034.2
011000 01  REC-SKL-SUB PICTURE 9(2) VALUE ZERO.                         SG1034.2
011100 01  REC-CT PICTURE 99 VALUE ZERO.                                SG1034.2
011200 01  DELETE-CNT                   PICTURE 999  VALUE ZERO.        SG1034.2
011300 01  ERROR-COUNTER PICTURE IS 999 VALUE IS ZERO.                  SG1034.2
011400 01  INSPECT-COUNTER PIC 999 VALUE ZERO.                          SG1034.2
011500 01  PASS-COUNTER PIC 999 VALUE ZERO.                             SG1034.2
011600 01  TOTAL-ERROR PIC 999 VALUE ZERO.                              SG1034.2
011700 01  ERROR-HOLD PIC 999 VALUE ZERO.                               SG1034.2
011800 01  DUMMY-HOLD PIC X(120) VALUE SPACE.                           SG1034.2
011900 01  RECORD-COUNT PIC 9(5) VALUE ZERO.                            SG1034.2
012000 01  CCVS-H-1.                                                    SG1034.2
012100     02  FILLER   PICTURE X(27)  VALUE SPACE.                     SG1034.2
012200     02 FILLER PICTURE X(67) VALUE                                SG1034.2
012300     " FEDERAL COMPILER TESTING CENTER COBOL COMPILER VALIDATION  SG1034.2
012400-    " SYSTEM".                                                   SG1034.2
012500     02  FILLER     PICTURE X(26)  VALUE SPACE.                   SG1034.2
012600 01  CCVS-H-2.                                                    SG1034.2
012700     02 FILLER PICTURE X(52) VALUE IS                             SG1034.2
012800     "CCVS74 NCC  COPY, NOT FOR DISTRIBUTION.".                   SG1034.2
012900     02 FILLER PICTURE IS X(19) VALUE IS "TEST RESULTS SET-  ".   SG1034.2
013000     02 TEST-ID PICTURE IS X(9).                                  SG1034.2
013100     02 FILLER PICTURE IS X(40) VALUE IS SPACE.                   SG1034.2
013200 01  CCVS-H-3.                                                    SG1034.2
013300     02  FILLER PICTURE X(34) VALUE                               SG1034.2
013400     " FOR OFFICIAL USE ONLY    ".                                SG1034.2
013500     02  FILLER PICTURE X(58) VALUE                               SG1034.2
013600     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SG1034.2
013700     02  FILLER PICTURE X(28) VALUE                               SG1034.2
013800     "  COPYRIGHT   1974 ".                                       SG1034.2
013900 01  CCVS-E-1.                                                    SG1034.2
014000     02 FILLER PICTURE IS X(52) VALUE IS SPACE.                   SG1034.2
014100     02 FILLER PICTURE IS X(14) VALUE IS "END OF TEST-  ".        SG1034.2
014200     02 ID-AGAIN PICTURE IS X(9).                                 SG1034.2
014300     02 FILLER PICTURE X(45) VALUE IS                             SG1034.2
014400     " NTIS DISTRIBUTION COBOL 74".                               SG1034.2
014500 01  CCVS-E-2.                                                    SG1034.2
014600     02  FILLER                   PICTURE X(31)  VALUE            SG1034.2
014700     SPACE.                                                       SG1034.2
014800     02  FILLER                   PICTURE X(21)  VALUE SPACE.     SG1034.2
014900     02 CCVS-E-2-2.                                               SG1034.2
015000         03 ERROR-TOTAL PICTURE IS XXX VALUE IS SPACE.            SG1034.2
015100         03 FILLER PICTURE IS X VALUE IS SPACE.                   SG1034.2
015200         03 ENDER-DESC PIC X(44) VALUE "ERRORS ENCOUNTERED".      SG1034.2
015300 01  CCVS-E-3.                                                    SG1034.2
015400     02  FILLER PICTURE X(22) VALUE                               SG1034.2
015500     " FOR OFFICIAL USE ONLY".                                    SG1034.2
015600     02  FILLER PICTURE X(12) VALUE SPACE.                        SG1034.2
015700     02  FILLER PICTURE X(58) VALUE                               SG1034.2
015800     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SG1034.2
015900     02  FILLER PICTURE X(13) VALUE SPACE.                        SG1034.2
016000     02 FILLER PIC X(15) VALUE " COPYRIGHT 1974".                 SG1034.2
016100 01  CCVS-E-4.                                                    SG1034.2
016200     02 CCVS-E-4-1 PIC XXX VALUE SPACE.                           SG1034.2
016300     02 FILLER PIC XXXX VALUE " OF ".                             SG1034.2
016400     02 CCVS-E-4-2 PIC XXX VALUE SPACE.                           SG1034.2
016500     02 FILLER PIC X(40) VALUE                                    SG1034.2
016600      "  TESTS WERE EXECUTED SUCCESSFULLY".                       SG1034.2
016700 01  XXINFO.                                                      SG1034.2
016800     02 FILLER PIC X(30) VALUE "        *** INFORMATION  ***".    SG1034.2
016900     02 INFO-TEXT.                                                SG1034.2
017000     04 FILLER PIC X(20) VALUE SPACE.                             SG1034.2
017100     04 XXCOMPUTED PIC X(20).                                     SG1034.2
017200     04 FILLER PIC X(5) VALUE SPACE.                              SG1034.2
017300     04 XXCORRECT PIC X(20).                                      SG1034.2
017400 01  HYPHEN-LINE.                                                 SG1034.2
017500     02 FILLER PICTURE IS X VALUE IS SPACE.                       SG1034.2
017600     02 FILLER PICTURE IS X(65) VALUE IS "************************SG1034.2
017700-    "*****************************************".                 SG1034.2
017800     02 FILLER PICTURE IS X(54) VALUE IS "************************SG1034.2
017900-    "******************************".                            SG1034.2
018000 01  CCVS-PGM-ID PIC X(6) VALUE                                   SG1034.2
018100     "SG103A".                                                    SG1034.2
018200 PROCEDURE DIVISION.                                              SG1034.2
018300 SEC00 SECTION.                                                   SG1034.2
018400 P0001.                                                           SG1034.2
018500     GO TO P0003.                                                 SG1034.2
018600 CCVS1 SECTION.                                                   SG1034.2
018700 OPEN-FILES.                                                      SG1034.2
018800     OPEN     OUTPUT PRINT-FILE.                                  SG1034.2
018900     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   SG1034.2
019000     MOVE    SPACE TO TEST-RESULTS.                               SG1034.2
019100     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             SG1034.2
019200     GO TO CCVS1-EXIT.                                            SG1034.2
019300 CLOSE-FILES.                                                     SG1034.2
019400     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   SG1034.2
019500 TERMINATE-CCVS.                                                  SG1034.2
019600*    EXIT PROGRAM.                                                SG1034.2
019700*TERMINATE-CALL.                                                  SG1034.2
019800     STOP     RUN.                                                SG1034.2
019900 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         SG1034.2
020000 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           SG1034.2
020100 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          SG1034.2
020200 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-CNT.          SG1034.2
020300     MOVE "****TEST DELETED****" TO RE-MARK.                      SG1034.2
020400 PRINT-DETAIL.                                                    SG1034.2
020500     IF REC-CT NOT EQUAL TO ZERO                                  SG1034.2
020600             MOVE "." TO PARDOT-X                                 SG1034.2
020700             MOVE REC-CT TO DOTVALUE.                             SG1034.2
020800     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      SG1034.2
020900     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               SG1034.2
021000        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 SG1034.2
021100          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 SG1034.2
021200     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              SG1034.2
021300     MOVE SPACE TO CORRECT-X.                                     SG1034.2
021400     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         SG1034.2
021500     MOVE     SPACE TO RE-MARK.                                   SG1034.2
021600 HEAD-ROUTINE.                                                    SG1034.2
021700     MOVE CCVS-H-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SG1034.2
021800     MOVE CCVS-H-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.   SG1034.2
021900     MOVE CCVS-H-3 TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.   SG1034.2
022000 COLUMN-NAMES-ROUTINE.                                            SG1034.2
022100     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SG1034.2
022200     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SG1034.2
022300     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        SG1034.2
022400 END-ROUTINE.                                                     SG1034.2
022500     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.SG1034.2
022600 END-RTN-EXIT.                                                    SG1034.2
022700     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SG1034.2
022800 END-ROUTINE-1.                                                   SG1034.2
022900      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      SG1034.2
023000      ERROR-HOLD. ADD DELETE-CNT TO ERROR-HOLD.                   SG1034.2
023100      ADD PASS-COUNTER TO ERROR-HOLD.                             SG1034.2
023200*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   SG1034.2
023300      MOVE PASS-COUNTER TO CCVS-E-4-1.                            SG1034.2
023400      MOVE ERROR-HOLD TO CCVS-E-4-2.                              SG1034.2
023500      MOVE CCVS-E-4 TO CCVS-E-2-2.                                SG1034.2
023600      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           SG1034.2
023700  END-ROUTINE-12.                                                 SG1034.2
023800      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        SG1034.2
023900     IF       ERROR-COUNTER IS EQUAL TO ZERO                      SG1034.2
024000         MOVE "NO " TO ERROR-TOTAL                                SG1034.2
024100         ELSE                                                     SG1034.2
024200         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       SG1034.2
024300     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           SG1034.2
024400     PERFORM WRITE-LINE.                                          SG1034.2
024500 END-ROUTINE-13.                                                  SG1034.2
024600     IF DELETE-CNT IS EQUAL TO ZERO                               SG1034.2
024700         MOVE "NO " TO ERROR-TOTAL  ELSE                          SG1034.2
024800         MOVE DELETE-CNT TO ERROR-TOTAL.                          SG1034.2
024900     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   SG1034.2
025000     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SG1034.2
025100      IF   INSPECT-COUNTER EQUAL TO ZERO                          SG1034.2
025200          MOVE "NO " TO ERROR-TOTAL                               SG1034.2
025300      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   SG1034.2
025400      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            SG1034.2
025500      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          SG1034.2
025600     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SG1034.2
025700 WRITE-LINE.                                                      SG1034.2
025800     ADD 1 TO RECORD-COUNT.                                       SG1034.2
025900*    IF RECORD-COUNT GREATER 50                                   SG1034.2
026000*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          SG1034.2
026100*        MOVE SPACE TO DUMMY-RECORD                               SG1034.2
026200*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  SG1034.2
026300*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             SG1034.2
026400*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     SG1034.2
026500*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          SG1034.2
026600*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          SG1034.2
026700*        MOVE ZERO TO RECORD-COUNT.                               SG1034.2
026800     PERFORM WRT-LN.                                              SG1034.2
026900 WRT-LN.                                                          SG1034.2
027000     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               SG1034.2
027100     MOVE SPACE TO DUMMY-RECORD.                                  SG1034.2
027200 BLANK-LINE-PRINT.                                                SG1034.2
027300     PERFORM WRT-LN.                                              SG1034.2
027400 FAIL-ROUTINE.                                                    SG1034.2
027500     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.   SG1034.2
027600     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.    SG1034.2
027700     MOVE "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.    SG1034.2
027800     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     SG1034.2
027900     GO TO FAIL-ROUTINE-EX.                                       SG1034.2
028000 FAIL-ROUTINE-WRITE.                                              SG1034.2
028100     MOVE TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE           SG1034.2
028200     MOVE TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES.   SG1034.2
028300 FAIL-ROUTINE-EX. EXIT.                                           SG1034.2
028400 BAIL-OUT.                                                        SG1034.2
028500     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.       SG1034.2
028600     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.               SG1034.2
028700 BAIL-OUT-WRITE.                                                  SG1034.2
028800     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  SG1034.2
028900     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     SG1034.2
029000 BAIL-OUT-EX. EXIT.                                               SG1034.2
029100 CCVS1-EXIT.                                                      SG1034.2
029200     EXIT.                                                        SG1034.2
029300 SECT-SG-03-001 SECTION  00.                                      SG1034.2
029400 P0002.                                                           SG1034.2
029500     MOVE "D" TO PERFORM-RSLT-4.                                  SG1034.2
029600 P0003.                                                           SG1034.2
029700     PERFORM  CCVS1.                                              SG1034.2
029800 SEC20 SECTION 20.                                                SG1034.2
029900 TEST-1.                                                          SG1034.2
030000     PERFORM SEC80.                                               SG1034.2
030100*    NOTE THAT AN INDEPENDENT SEGMENT SHOULD BE MADE AVAILABLE TO SG1034.2
030200*         THE PROGRAM IN ITS INITIAL STATE EACH TIME IT IS        SG1034.2
030300*         REFERENCED, AN ALTER STATEMENT WILL BE USED TO TEST THISSG1034.2
030400*         FEATURE.                                                SG1034.2
030500     PERFORM SEC80.                                               SG1034.2
030600     IF INITIAL-STATE-A EQUAL TO 2 PERFORM PASS                   SG1034.2
030700         ELSE MOVE INITIAL-STATE-A TO COMPUTED-A                  SG1034.2
030800         MOVE "2" TO CORRECT-A                                    SG1034.2
030900              PERFORM FAIL.                                       SG1034.2
031000     GO TO TEST-1-WRITE.                                          SG1034.2
031100 TEST-1-DELETE.                                                   SG1034.2
031200     PERFORM DE-LETE.                                             SG1034.2
031300 TEST-1-WRITE.                                                    SG1034.2
031400     MOVE "INITIAL STATE" TO FEATURE.                             SG1034.2
031500     MOVE "TEST-1" TO PAR-NAME.                                   SG1034.2
031600     PERFORM PRINT-DETAIL.                                        SG1034.2
031700 TEST-2.                                                          SG1034.2
031800     MOVE SPACE TO CORRECT-A.                                     SG1034.2
031900*    NOTE THAT A "GO TO"   A NON-RESIDENT ROUTINE WILL BE TESTED. SG1034.2
032000     GO TO P6001.                                                 SG1034.2
032100 GO-TO-RETURN.                                                    SG1034.2
032200     IF GO-TO-IND EQUAL TO "G" PERFORM PASS                       SG1034.2
032300         ELSE MOVE GO-TO-IND TO COMPUTED-A                        SG1034.2
032400         MOVE "G" TO CORRECT-A                                    SG1034.2
032500              PERFORM FAIL.                                       SG1034.2
032600     GO TO TEST-2-WRITE.                                          SG1034.2
032700 TEST-2-DELETE.                                                   SG1034.2
032800     PERFORM DE-LETE.                                             SG1034.2
032900 TEST-2-WRITE.                                                    SG1034.2
033000     MOVE "GO TO INDEP SEG" TO FEATURE.                           SG1034.2
033100     MOVE "TEST-2" TO PAR-NAME.                                   SG1034.2
033200     PERFORM PRINT-DETAIL.                                        SG1034.2
033300 TEST-3.                                                          SG1034.2
033400     MOVE SPACE TO CORRECT-A.                                     SG1034.2
033500*    NOTE THAT THIS TEST PERFORMS A ROUTINE LOCATED IN AN         SG1034.2
033600*             INDEPENDENT SEGMENT.                                SG1034.2
033700     PERFORM P9301 THRU P9303.                                    SG1034.2
033800     IF PERFORM-RSLT EQUAL TO "ABCD" PERFORM PASS                 SG1034.2
033900         ELSE MOVE PERFORM-RSLT TO COMPUTED-A                     SG1034.2
034000         MOVE "ABCD" TO CORRECT-A                                 SG1034.2
034100              PERFORM FAIL.                                       SG1034.2
034200     GO TO TEST-3-WRITE.                                          SG1034.2
034300 TEST-3-DELETE.                                                   SG1034.2
034400     PERFORM DE-LETE.                                             SG1034.2
034500 TEST-3-WRITE.                                                    SG1034.2
034600     MOVE "PERFORM IND SEG" TO FEATURE.                           SG1034.2
034700     MOVE "TEST-3" TO PAR-NAME.                                   SG1034.2
034800     PERFORM PRINT-DETAIL.                                        SG1034.2
034900 TEST-4.                                                          SG1034.2
035000     MOVE SPACE TO CORRECT-A.                                     SG1034.2
035100*    NOTE THAT THIS TEST CAUSES AN INDEPENDENT SEGMENT TO ALTER   SG1034.2
035200*             A STATEMENT IN THE FIXED PORTION AND THEN CHECKS TO SG1034.2
035300*             SEE THAT THE ALTER IS IN EFFECT.                    SG1034.2
035400     PERFORM SEC95.                                               SG1034.2
035500 ALTER-RES.                                                       SG1034.2
035600     GO TO ALTER-RES1.                                            SG1034.2
035700 ALTER-RES1.                                                      SG1034.2
035800     MOVE "A" TO ALTER-RSLT-2.                                    SG1034.2
035900     GO TO ALTER-RES3.                                            SG1034.2
036000 ALTER-RES2.                                                      SG1034.2
036100     MOVE "B" TO ALTER-RSLT-3.                                    SG1034.2
036200 ALTER-RES3.                                                      SG1034.2
036300     IF ALTER-RSLT EQUAL TO "E B" PERFORM PASS                    SG1034.2
036400         ELSE MOVE ALTER-RSLT TO COMPUTED-A                       SG1034.2
036500         MOVE "E B" TO CORRECT-A                                  SG1034.2
036600         PERFORM FAIL.                                            SG1034.2
036700     GO TO TEST-4-WRITE.                                          SG1034.2
036800 TEST-4-DELETE.                                                   SG1034.2
036900     PERFORM DE-LETE.                                             SG1034.2
037000 TEST-4-WRITE.                                                    SG1034.2
037100     MOVE "ALT RES FRM IND" TO FEATURE.                           SG1034.2
037200     MOVE "TEST-4" TO PAR-NAME.                                   SG1034.2
037300     PERFORM PRINT-DETAIL.                                        SG1034.2
037400 TEST-5.                                                          SG1034.2
037500     IF PAR-NAME EQUAL TO "TEST-6         "  GO TO P2006.         SG1034.2
037600     MOVE "TEST-5" TO PAR-NAME.                                   SG1034.2
037700*    NOTE THAT THIS TEST REQUIRES THE LOGICAL PATH OF THE PROGRAM SG1034.2
037800*             TO PROCEED FROM THE FIXED PORTION THROUGH AN        SG1034.2
037900*             INDEPENDENT SEGMENT.                                SG1034.2
038000 P2005.                                                           SG1034.2
038100     MOVE "A" TO FALL-RSLT-1.                                     SG1034.2
038200 P2006.  EXIT.                                                    SG1034.2
038300 SEC51 SECTION 51.                                                SG1034.2
038400 P5101.                                                           SG1034.2
038500     IF PAR-NAME EQUAL TO "TEST-6         "  GO TO P5102.         SG1034.2
038600     MOVE "B" TO FALL-RSLT-2.                                     SG1034.2
038700     IF FALL-RSLT EQUAL TO "AB" PERFORM PASS                      SG1034.2
038800         ELSE MOVE FALL-RSLT TO COMPUTED-A                        SG1034.2
038900         MOVE "AB" TO CORRECT-A                                   SG1034.2
039000              PERFORM FAIL.                                       SG1034.2
039100     GO TO TEST-5-WRITE.                                          SG1034.2
039200 TEST-5-DELETE.                                                   SG1034.2
039300     PERFORM DE-LETE.                                             SG1034.2
039400 TEST-5-WRITE.                                                    SG1034.2
039500     MOVE "FALL THRU TEST" TO FEATURE.                            SG1034.2
039600     PERFORM PRINT-DETAIL.                                        SG1034.2
039700     MOVE "TEST-6" TO PAR-NAME.                                   SG1034.2
039800     GO TO TEST-5.                                                SG1034.2
039900 P5102.   GO TO P5103.                                            SG1034.2
040000 P5103.                                                           SG1034.2
040100     ALTER P5102 TO PROCEED TO P5104.                             SG1034.2
040200     MOVE SPACE TO FALL-RSLT.                                     SG1034.2
040300     GO TO P5105.                                                 SG1034.2
040400 P5104.   MOVE "XX" TO FALL-RSLT.                                 SG1034.2
040500 P5105.   EXIT.                                                   SG1034.2
040600 P5106.                                                           SG1034.2
040700     ADD 1 TO ENT-COUNTER.                                        SG1034.2
040800     IF ENT-COUNTER EQUAL TO 2                                    SG1034.2
040900         GO TO TEST-6.                                            SG1034.2
041000     GO TO TEST-5.                                                SG1034.2
041100 TEST-6.                                                          SG1034.2
041200     IF FALL-RSLT EQUAL TO SPACE                                  SG1034.2
041300         PERFORM PASS                                             SG1034.2
041400         GO TO TEST-6-WRITE.                                      SG1034.2
041500     MOVE "SPACE" TO CORRECT-A.                                   SG1034.2
041600     MOVE FALL-RSLT TO COMPUTED-A.                                SG1034.2
041700     PERFORM FAIL.                                                SG1034.2
041800     GO TO TEST-6-WRITE.                                          SG1034.2
041900 TEST-6-DELETE.                                                   SG1034.2
042000     PERFORM DE-LETE.                                             SG1034.2
042100 TEST-6-WRITE.                                                    SG1034.2
042200     PERFORM PRINT-DETAIL.                                        SG1034.2
042300     MOVE ZERO TO ENT-COUNTER.                                    SG1034.2
042400     MOVE SPACE TO GO-TO-IND.                                     SG1034.2
042500 TEST-7.                                                          SG1034.2
042600     GO TO P9901.                                                 SG1034.2
042700 PARA-7A.                                                         SG1034.2
042800     GO TO P9901.                                                 SG1034.2
042900 PARA-7B.                                                         SG1034.2
043000     IF GO-TO-IND EQUAL TO SPACE                                  SG1034.2
043100         PERFORM PASS                                             SG1034.2
043200         GO TO TEST-7-WRITE.                                      SG1034.2
043300     MOVE "SPACE" TO CORRECT-A.                                   SG1034.2
043400     MOVE GO-TO-IND TO COMPUTED-A.                                SG1034.2
043500     PERFORM FAIL.                                                SG1034.2
043600     GO TO TEST-7-WRITE.                                          SG1034.2
043700 TEST-7-DELETE.                                                   SG1034.2
043800     PERFORM DE-LETE.                                             SG1034.2
043900 TEST-7-WRITE.                                                    SG1034.2
044000     MOVE "TEST-7" TO PAR-NAME.                                   SG1034.2
044100     MOVE "GO TO ALTER IND" TO FEATURE.                           SG1034.2
044200     PERFORM PRINT-DETAIL.                                        SG1034.2
044300 WRAP-UP.                                                         SG1034.2
044400     GO TO   CLOSE-FILES.                                         SG1034.2
044500 SEC60 SECTION 60.                                                SG1034.2
044600 P6001.                                                           SG1034.2
044700     MOVE "G" TO GO-TO-IND.                                       SG1034.2
044800     GO TO GO-TO-RETURN.                                          SG1034.2
044900 SEC80 SECTION 80.                                                SG1034.2
045000 P8001.                                                           SG1034.2
045100     GO TO P8002.                                                 SG1034.2
045200 P8002.                                                           SG1034.2
045300     ALTER P8001 TO PROCEED TO P8003.                             SG1034.2
045400     ADD 1 TO INITIAL-STATE-A.                                    SG1034.2
045500     GO TO P8004.                                                 SG1034.2
045600 P8003.                                                           SG1034.2
045700     MOVE 9 TO INITIAL-STATE-A.                                   SG1034.2
045800 P8004.                                                           SG1034.2
045900     EXIT.                                                        SG1034.2
046000 SEC93 SECTION 93.                                                SG1034.2
046100 P9301.                                                           SG1034.2
046200     MOVE "A" TO PERFORM-RSLT-1.                                  SG1034.2
046300     PERFORM P9302.                                               SG1034.2
046400     MOVE "C" TO PERFORM-RSLT-3.                                  SG1034.2
046500     GO TO P9303.                                                 SG1034.2
046600 P9302.                                                           SG1034.2
046700     MOVE "B" TO PERFORM-RSLT-2.                                  SG1034.2
046800 P9303.                                                           SG1034.2
046900     PERFORM P0002.                                               SG1034.2
047000 SEC95 SECTION 95.                                                SG1034.2
047100 P9501.                                                           SG1034.2
047200     MOVE "E" TO ALTER-RSLT-1.                                    SG1034.2
047300     ALTER ALTER-RES TO PROCEED TO ALTER-RES2.                    SG1034.2
047400 SEC99 SECTION 99.                                                SG1034.2
047500 P9901.                                                           SG1034.2
047600     GO TO P9902.                                                 SG1034.2
047700 P9902.                                                           SG1034.2
047800     ALTER P9901 TO P9903.                                        SG1034.2
047900     IF ENT-COUNTER EQUAL TO ZERO                                 SG1034.2
048000         ADD 1 TO ENT-COUNTER                                     SG1034.2
048100         GO TO PARA-7A.                                           SG1034.2
048200     GO TO PARA-7B.                                               SG1034.2
048300 P9903.                                                           SG1034.2
048400     MOVE "X" TO GO-TO-IND.                                       SG1034.2
048500     GO TO PARA-7B.                                               SG1034.2
      *END-OF,SG103A                                                                  
