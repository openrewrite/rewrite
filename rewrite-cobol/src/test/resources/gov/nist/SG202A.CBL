      *HEADER,COBOL,SG202A                                                            
000100 IDENTIFICATION DIVISION.                                         SG2024.2
000200 PROGRAM-ID.                                                      SG2024.2
000300     SG202A.                                                      SG2024.2
000400 AUTHOR.                                                          SG2024.2
000500     FEDERAL COMPILER TESTING CENTER.                             SG2024.2
000600 INSTALLATION.                                                    SG2024.2
000700     GENERAL SERVICES ADMINISTRATION                              SG2024.2
000800*     AUTOMATED DATA AND TELECOMMUNICATION SERVICE.                SG2024.2
000900     SOFTWARE DEVELOPMENT OFFICE.                                 SG2024.2
001000     5203 LEESBURG PIKE  SUITE 1100                               SG2024.2
001100     FALLS CHURCH VIRGINIA 22041.                                 SG2024.2
001200                                                                  SG2024.2
001300     PHONE   (703) 756-6153                                       SG2024.2
001400                                                                  SG2024.2
001500     " HIGH       ".                                              SG2024.2
001600 DATE-WRITTEN.                                                    SG2024.2
001700     CCVS-74 VERSION 4.0 - 1980 JULY 1.                           SG2024.2
001800     CREATION DATE     /    VALIDATION DATE                       SG2024.2
001900     "4.2 ".                                                      SG2024.2
002000 SECURITY.                                                        SG2024.2
002100     NONE.                                                        SG2024.2
002200       THE FOLLOWING FEATURES ARE TESTED BY THIS PROGRAM ---      SG2024.2
002300         BASED ON A SEGMENT-LIMIT OF 25 THESE TESTS ARE           SG2024.2
002400         DESIGNED TO ALTER SEGMENTS THAT HAVE NOT YET BEEN        SG2024.2
002500         CALLED FOR EXECUTION, FALL THRU TO INDEPENDENT           SG2024.2
002600         SEGMENTS, AND PERFORM FIXED OVERLAYABLE SEGMENTS.        SG2024.2
002700                                                                  SG2024.2
002800*                                                                 SG2024.2
002900 ENVIRONMENT DIVISION.                                            SG2024.2
003000 CONFIGURATION SECTION.                                           SG2024.2
003100 SOURCE-COMPUTER.                                                 SG2024.2
003200     XXXXX082.                                                    SG2024.2
003300 OBJECT-COMPUTER.                                                 SG2024.2
003400     XXXXX083                                                     SG2024.2
003500     SEGMENT-LIMIT IS 25.                                         SG2024.2
003600 INPUT-OUTPUT SECTION.                                            SG2024.2
003700 FILE-CONTROL.                                                    SG2024.2
003800     SELECT PRINT-FILE ASSIGN TO                                  SG2024.2
003900     XXXXX055.                                                    SG2024.2
004000 DATA DIVISION.                                                   SG2024.2
004100 FILE SECTION.                                                    SG2024.2
004200 FD  PRINT-FILE                                                   SG2024.2
004300     LABEL RECORDS                                                SG2024.2
004400     XXXXX084                                                     SG2024.2
004500     DATA RECORD IS PRINT-REC DUMMY-RECORD.                       SG2024.2
004600 01  PRINT-REC PICTURE X(120).                                    SG2024.2
004700 01  DUMMY-RECORD PICTURE X(120).                                 SG2024.2
004800 WORKING-STORAGE SECTION.                                         SG2024.2
004900 01  LAST-STATE-A.                                                SG2024.2
005000     02 LAST-STATE-B    PICTURE 9         VALUE 0.                SG2024.2
005100     02 LAST-STATE-C    PICTURE 9         VALUE 0.                SG2024.2
005200 01  ALTER-NOT-CALL     PICTURE X.                                SG2024.2
005300 01  PERF-OVER-RES.                                               SG2024.2
005400     02 PERF-OVER-RES-A PICTURE X.                                SG2024.2
005500     02 PERF-OVER-RES-B PICTURE X.                                SG2024.2
005600 01  PERF-RES-OVER.                                               SG2024.2
005700     02 PERF-RES-OVER-A PICTURE X.                                SG2024.2
005800     02 PERF-RES-OVER-B PICTURE X.                                SG2024.2
005900 01  FALL-RSLT.                                                   SG2024.2
006000     02 FALL-RSLT-1     PICTURE X           VALUE " ".            SG2024.2
006100     02 FALL-RSLT-2     PICTURE X           VALUE " ".            SG2024.2
006200 01  TEST-RESULTS.                                                SG2024.2
006300     02 FILLER                    PICTURE X VALUE SPACE.          SG2024.2
006400     02 FEATURE                   PICTURE X(20) VALUE SPACE.      SG2024.2
006500     02 FILLER                    PICTURE X VALUE SPACE.          SG2024.2
006600     02 P-OR-F                    PICTURE X(5) VALUE SPACE.       SG2024.2
006700     02 FILLER                    PICTURE X  VALUE SPACE.         SG2024.2
006800     02  PAR-NAME.                                                SG2024.2
006900       03 FILLER PICTURE X(12) VALUE SPACE.                       SG2024.2
007000       03  PARDOT-X PICTURE X  VALUE SPACE.                       SG2024.2
007100       03 DOTVALUE PICTURE 99  VALUE ZERO.                        SG2024.2
007200       03 FILLER PIC X(5) VALUE SPACE.                            SG2024.2
007300     02 FILLER PIC X(10) VALUE SPACE.                             SG2024.2
007400     02 RE-MARK PIC X(61).                                        SG2024.2
007500 01  TEST-COMPUTED.                                               SG2024.2
007600     02 FILLER PIC X(30) VALUE SPACE.                             SG2024.2
007700     02 FILLER PIC X(17) VALUE "       COMPUTED=".                SG2024.2
007800     02 COMPUTED-X.                                               SG2024.2
007900     03 COMPUTED-A                PICTURE X(20) VALUE SPACE.      SG2024.2
008000     03 COMPUTED-N REDEFINES COMPUTED-A PICTURE -9(9).9(9).       SG2024.2
008100     03 COMPUTED-0V18 REDEFINES COMPUTED-A  PICTURE -.9(18).      SG2024.2
008200     03 COMPUTED-4V14 REDEFINES COMPUTED-A  PICTURE -9(4).9(14).  SG2024.2
008300     03 COMPUTED-14V4 REDEFINES COMPUTED-A  PICTURE -9(14).9(4).  SG2024.2
008400     03       CM-18V0 REDEFINES COMPUTED-A.                       SG2024.2
008500         04 COMPUTED-18V0                   PICTURE -9(18).       SG2024.2
008600         04 FILLER                          PICTURE X.            SG2024.2
008700     03 FILLER PIC X(50) VALUE SPACE.                             SG2024.2
008800 01  TEST-CORRECT.                                                SG2024.2
008900     02 FILLER PIC X(30) VALUE SPACE.                             SG2024.2
009000     02 FILLER PIC X(17) VALUE "       CORRECT =".                SG2024.2
009100     02 CORRECT-X.                                                SG2024.2
009200     03 CORRECT-A                 PICTURE X(20) VALUE SPACE.      SG2024.2
009300     03 CORRECT-N REDEFINES CORRECT-A PICTURE -9(9).9(9).         SG2024.2
009400     03 CORRECT-0V18 REDEFINES CORRECT-A    PICTURE -.9(18).      SG2024.2
009500     03 CORRECT-4V14 REDEFINES CORRECT-A    PICTURE -9(4).9(14).  SG2024.2
009600     03 CORRECT-14V4 REDEFINES CORRECT-A    PICTURE -9(14).9(4).  SG2024.2
009700     03      CR-18V0 REDEFINES CORRECT-A.                         SG2024.2
009800         04 CORRECT-18V0                    PICTURE -9(18).       SG2024.2
009900         04 FILLER                          PICTURE X.            SG2024.2
010000     03 FILLER PIC X(50) VALUE SPACE.                             SG2024.2
010100 01  CCVS-C-1.                                                    SG2024.2
010200     02 FILLER PICTURE IS X(99) VALUE IS " FEATURE              PASG2024.2
010300-    "SS  PARAGRAPH-NAME                                          SG2024.2
010400-    "        REMARKS".                                           SG2024.2
010500     02 FILLER PICTURE IS X(20) VALUE IS SPACE.                   SG2024.2
010600 01  CCVS-C-2.                                                    SG2024.2
010700     02 FILLER PICTURE IS X VALUE IS SPACE.                       SG2024.2
010800     02 FILLER PICTURE IS X(6) VALUE IS "TESTED".                 SG2024.2
010900     02 FILLER PICTURE IS X(15) VALUE IS SPACE.                   SG2024.2
011000     02 FILLER PICTURE IS X(4) VALUE IS "FAIL".                   SG2024.2
011100     02 FILLER PICTURE IS X(94) VALUE IS SPACE.                   SG2024.2
011200 01  REC-SKL-SUB PICTURE 9(2) VALUE ZERO.                         SG2024.2
011300 01  REC-CT PICTURE 99 VALUE ZERO.                                SG2024.2
011400 01  DELETE-CNT                   PICTURE 999  VALUE ZERO.        SG2024.2
011500 01  ERROR-COUNTER PICTURE IS 999 VALUE IS ZERO.                  SG2024.2
011600 01  INSPECT-COUNTER PIC 999 VALUE ZERO.                          SG2024.2
011700 01  PASS-COUNTER PIC 999 VALUE ZERO.                             SG2024.2
011800 01  TOTAL-ERROR PIC 999 VALUE ZERO.                              SG2024.2
011900 01  ERROR-HOLD PIC 999 VALUE ZERO.                               SG2024.2
012000 01  DUMMY-HOLD PIC X(120) VALUE SPACE.                           SG2024.2
012100 01  RECORD-COUNT PIC 9(5) VALUE ZERO.                            SG2024.2
012200 01  CCVS-H-1.                                                    SG2024.2
012300     02  FILLER   PICTURE X(27)  VALUE SPACE.                     SG2024.2
012400     02 FILLER PICTURE X(67) VALUE                                SG2024.2
012500     " FEDERAL COMPILER TESTING CENTER COBOL COMPILER VALIDATION  SG2024.2
012600-    " SYSTEM".                                                   SG2024.2
012700     02  FILLER     PICTURE X(26)  VALUE SPACE.                   SG2024.2
012800 01  CCVS-H-2.                                                    SG2024.2
012900     02 FILLER PICTURE X(52) VALUE IS                             SG2024.2
013000     "CCVS74 NCC  COPY, NOT FOR DISTRIBUTION.".                   SG2024.2
013100     02 FILLER PICTURE IS X(19) VALUE IS "TEST RESULTS SET-  ".   SG2024.2
013200     02 TEST-ID PICTURE IS X(9).                                  SG2024.2
013300     02 FILLER PICTURE IS X(40) VALUE IS SPACE.                   SG2024.2
013400 01  CCVS-H-3.                                                    SG2024.2
013500     02  FILLER PICTURE X(34) VALUE                               SG2024.2
013600     " FOR OFFICIAL USE ONLY    ".                                SG2024.2
013700     02  FILLER PICTURE X(58) VALUE                               SG2024.2
013800     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SG2024.2
013900     02  FILLER PICTURE X(28) VALUE                               SG2024.2
014000     "  COPYRIGHT   1974 ".                                       SG2024.2
014100 01  CCVS-E-1.                                                    SG2024.2
014200     02 FILLER PICTURE IS X(52) VALUE IS SPACE.                   SG2024.2
014300     02 FILLER PICTURE IS X(14) VALUE IS "END OF TEST-  ".        SG2024.2
014400     02 ID-AGAIN PICTURE IS X(9).                                 SG2024.2
014500     02 FILLER PICTURE X(45) VALUE IS                             SG2024.2
014600     " NTIS DISTRIBUTION COBOL 74".                               SG2024.2
014700 01  CCVS-E-2.                                                    SG2024.2
014800     02  FILLER                   PICTURE X(31)  VALUE            SG2024.2
014900     SPACE.                                                       SG2024.2
015000     02  FILLER                   PICTURE X(21)  VALUE SPACE.     SG2024.2
015100     02 CCVS-E-2-2.                                               SG2024.2
015200         03 ERROR-TOTAL PICTURE IS XXX VALUE IS SPACE.            SG2024.2
015300         03 FILLER PICTURE IS X VALUE IS SPACE.                   SG2024.2
015400         03 ENDER-DESC PIC X(44) VALUE "ERRORS ENCOUNTERED".      SG2024.2
015500 01  CCVS-E-3.                                                    SG2024.2
015600     02  FILLER PICTURE X(22) VALUE                               SG2024.2
015700     " FOR OFFICIAL USE ONLY".                                    SG2024.2
015800     02  FILLER PICTURE X(12) VALUE SPACE.                        SG2024.2
015900     02  FILLER PICTURE X(58) VALUE                               SG2024.2
016000     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SG2024.2
016100     02  FILLER PICTURE X(13) VALUE SPACE.                        SG2024.2
016200     02 FILLER PIC X(15) VALUE " COPYRIGHT 1974".                 SG2024.2
016300 01  CCVS-E-4.                                                    SG2024.2
016400     02 CCVS-E-4-1 PIC XXX VALUE SPACE.                           SG2024.2
016500     02 FILLER PIC XXXX VALUE " OF ".                             SG2024.2
016600     02 CCVS-E-4-2 PIC XXX VALUE SPACE.                           SG2024.2
016700     02 FILLER PIC X(40) VALUE                                    SG2024.2
016800      "  TESTS WERE EXECUTED SUCCESSFULLY".                       SG2024.2
016900 01  XXINFO.                                                      SG2024.2
017000     02 FILLER PIC X(30) VALUE "        *** INFORMATION  ***".    SG2024.2
017100     02 INFO-TEXT.                                                SG2024.2
017200     04 FILLER PIC X(20) VALUE SPACE.                             SG2024.2
017300     04 XXCOMPUTED PIC X(20).                                     SG2024.2
017400     04 FILLER PIC X(5) VALUE SPACE.                              SG2024.2
017500     04 XXCORRECT PIC X(20).                                      SG2024.2
017600 01  HYPHEN-LINE.                                                 SG2024.2
017700     02 FILLER PICTURE IS X VALUE IS SPACE.                       SG2024.2
017800     02 FILLER PICTURE IS X(65) VALUE IS "************************SG2024.2
017900-    "*****************************************".                 SG2024.2
018000     02 FILLER PICTURE IS X(54) VALUE IS "************************SG2024.2
018100-    "******************************".                            SG2024.2
018200 01  CCVS-PGM-ID PIC X(6) VALUE                                   SG2024.2
018300     "SG202A".                                                    SG2024.2
018400 PROCEDURE DIVISION.                                              SG2024.2
018500 SEC00 SECTION.                                                   SG2024.2
018600 PARAGRAPH-NAME-1.                                                SG2024.2
018700     GO TO P0010.                                                 SG2024.2
018800 CCVS1 SECTION.                                                   SG2024.2
018900 OPEN-FILES.                                                      SG2024.2
019000     OPEN     OUTPUT PRINT-FILE.                                  SG2024.2
019100     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   SG2024.2
019200     MOVE    SPACE TO TEST-RESULTS.                               SG2024.2
019300     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             SG2024.2
019400     GO TO CCVS1-EXIT.                                            SG2024.2
019500 CLOSE-FILES.                                                     SG2024.2
019600     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   SG2024.2
019700 TERMINATE-CCVS.                                                  SG2024.2
019800*    EXIT PROGRAM.                                                SG2024.2
019900*TERMINATE-CALL.                                                  SG2024.2
020000     STOP     RUN.                                                SG2024.2
020100 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         SG2024.2
020200 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           SG2024.2
020300 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          SG2024.2
020400 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-CNT.          SG2024.2
020500     MOVE "****TEST DELETED****" TO RE-MARK.                      SG2024.2
020600 PRINT-DETAIL.                                                    SG2024.2
020700     IF REC-CT NOT EQUAL TO ZERO                                  SG2024.2
020800             MOVE "." TO PARDOT-X                                 SG2024.2
020900             MOVE REC-CT TO DOTVALUE.                             SG2024.2
021000     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      SG2024.2
021100     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               SG2024.2
021200        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 SG2024.2
021300          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 SG2024.2
021400     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              SG2024.2
021500     MOVE SPACE TO CORRECT-X.                                     SG2024.2
021600     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         SG2024.2
021700     MOVE     SPACE TO RE-MARK.                                   SG2024.2
021800 HEAD-ROUTINE.                                                    SG2024.2
021900     MOVE CCVS-H-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SG2024.2
022000     MOVE CCVS-H-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.   SG2024.2
022100     MOVE CCVS-H-3 TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.   SG2024.2
022200 COLUMN-NAMES-ROUTINE.                                            SG2024.2
022300     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SG2024.2
022400     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SG2024.2
022500     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        SG2024.2
022600 END-ROUTINE.                                                     SG2024.2
022700     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.SG2024.2
022800 END-RTN-EXIT.                                                    SG2024.2
022900     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SG2024.2
023000 END-ROUTINE-1.                                                   SG2024.2
023100      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      SG2024.2
023200      ERROR-HOLD. ADD DELETE-CNT TO ERROR-HOLD.                   SG2024.2
023300      ADD PASS-COUNTER TO ERROR-HOLD.                             SG2024.2
023400*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   SG2024.2
023500      MOVE PASS-COUNTER TO CCVS-E-4-1.                            SG2024.2
023600      MOVE ERROR-HOLD TO CCVS-E-4-2.                              SG2024.2
023700      MOVE CCVS-E-4 TO CCVS-E-2-2.                                SG2024.2
023800      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           SG2024.2
023900  END-ROUTINE-12.                                                 SG2024.2
024000      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        SG2024.2
024100     IF       ERROR-COUNTER IS EQUAL TO ZERO                      SG2024.2
024200         MOVE "NO " TO ERROR-TOTAL                                SG2024.2
024300         ELSE                                                     SG2024.2
024400         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       SG2024.2
024500     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           SG2024.2
024600     PERFORM WRITE-LINE.                                          SG2024.2
024700 END-ROUTINE-13.                                                  SG2024.2
024800     IF DELETE-CNT IS EQUAL TO ZERO                               SG2024.2
024900         MOVE "NO " TO ERROR-TOTAL  ELSE                          SG2024.2
025000         MOVE DELETE-CNT TO ERROR-TOTAL.                          SG2024.2
025100     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   SG2024.2
025200     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SG2024.2
025300      IF   INSPECT-COUNTER EQUAL TO ZERO                          SG2024.2
025400          MOVE "NO " TO ERROR-TOTAL                               SG2024.2
025500      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   SG2024.2
025600      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            SG2024.2
025700      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          SG2024.2
025800     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SG2024.2
025900 WRITE-LINE.                                                      SG2024.2
026000     ADD 1 TO RECORD-COUNT.                                       SG2024.2
026100*    IF RECORD-COUNT GREATER 50                                   SG2024.2
026200*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          SG2024.2
026300*        MOVE SPACE TO DUMMY-RECORD                               SG2024.2
026400*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  SG2024.2
026500*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             SG2024.2
026600*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     SG2024.2
026700*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          SG2024.2
026800*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          SG2024.2
026900*        MOVE ZERO TO RECORD-COUNT.                               SG2024.2
027000     PERFORM WRT-LN.                                              SG2024.2
027100 WRT-LN.                                                          SG2024.2
027200     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               SG2024.2
027300     MOVE SPACE TO DUMMY-RECORD.                                  SG2024.2
027400 BLANK-LINE-PRINT.                                                SG2024.2
027500     PERFORM WRT-LN.                                              SG2024.2
027600 FAIL-ROUTINE.                                                    SG2024.2
027700     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.   SG2024.2
027800     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.    SG2024.2
027900     MOVE "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.    SG2024.2
028000     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     SG2024.2
028100     GO TO FAIL-ROUTINE-EX.                                       SG2024.2
028200 FAIL-ROUTINE-WRITE.                                              SG2024.2
028300     MOVE TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE           SG2024.2
028400     MOVE TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES.   SG2024.2
028500 FAIL-ROUTINE-EX. EXIT.                                           SG2024.2
028600 BAIL-OUT.                                                        SG2024.2
028700     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.       SG2024.2
028800     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.               SG2024.2
028900 BAIL-OUT-WRITE.                                                  SG2024.2
029000     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  SG2024.2
029100     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     SG2024.2
029200 BAIL-OUT-EX. EXIT.                                               SG2024.2
029300 CCVS1-EXIT.                                                      SG2024.2
029400     EXIT.                                                        SG2024.2
029500 SECT-SG-02-001 SECTION 00 .                                      SG2024.2
029600 SG-02-001.                                                       SG2024.2
029700 P0003.                                                           SG2024.2
029800     MOVE "B" TO PERF-OVER-RES-B.                                 SG2024.2
029900 P0004.                                                           SG2024.2
030000     MOVE "X" TO PERF-RES-OVER-A.                                 SG2024.2
030100     GO TO P4801.                                                 SG2024.2
030200 P0010.                                                           SG2024.2
030300     PERFORM   CCVS1.                                             SG2024.2
030400 TEST-1.                                                          SG2024.2
030500     MOVE SPACE TO CORRECT-A.                                     SG2024.2
030600*    NOTE THAT A TEST WILL BE MADE TO ENSURE THAT A ROUTINE       SG2024.2
030700*             PERFORMED IN THE OVERLAYABLE PART OF THE PERMANENT  SG2024.2
030800*             SEGMENT WILL BE LEFT IN ITS LAST USED STATE --- AN  SG2024.2
030900*             ALTER STATEMENT WILL BE USED FOR THIS TEST.         SG2024.2
031000     PERFORM SEC39.                                               SG2024.2
031100     PERFORM P3901 THRU P3904.                                    SG2024.2
031200     PERFORM SEC39.                                               SG2024.2
031300     IF LAST-STATE-A EQUAL TO "23" PERFORM PASS                   SG2024.2
031400         ELSE MOVE LAST-STATE-A TO COMPUTED-A                     SG2024.2
031500         MOVE "23" TO CORRECT-A                                   SG2024.2
031600              PERFORM FAIL.                                       SG2024.2
031700     GO TO TEST-1-WRITE.                                          SG2024.2
031800 TEST-1-DELETE.                                                   SG2024.2
031900     PERFORM DE-LETE.                                             SG2024.2
032000 TEST-1-WRITE.                                                    SG2024.2
032100     MOVE "TEST-1" TO PAR-NAME.                                   SG2024.2
032200     MOVE "LAST USED STATE" TO FEATURE.                           SG2024.2
032300     PERFORM PRINT-DETAIL.                                        SG2024.2
032400 TEST-2.                                                          SG2024.2
032500     MOVE SPACE TO CORRECT-A.                                     SG2024.2
032600*    NOTE THAT A TEST WILL BE MADE TO ENSURE THAT A STATEMENT     SG2024.2
032700*             IN THE OVERLAYABLE PART OF THE FIXED PORTION CAN BE SG2024.2
032800*             ALTERED FROM THE PERMANENT SEGMENT EVEN THOUGH THE  SG2024.2
032900*             ALTER REFERS TO A SEGMENT NOT YET CALLED FOR        SG2024.2
033000*             EXECUTION.                                          SG2024.2
033100     ALTER P4001 TO PROCEED TO P4003.                             SG2024.2
033200     PERFORM SEC40.                                               SG2024.2
033300     IF ALTER-NOT-CALL EQUAL TO "B" PERFORM PASS                  SG2024.2
033400         ELSE MOVE ALTER-NOT-CALL TO COMPUTED-A                   SG2024.2
033500         MOVE "B" TO CORRECT-A                                    SG2024.2
033600              PERFORM FAIL.                                       SG2024.2
033700     GO TO TEST-2-WRITE.                                          SG2024.2
033800 TEST-2-DELETE.                                                   SG2024.2
033900     PERFORM DE-LETE.                                             SG2024.2
034000 TEST-2-WRITE.                                                    SG2024.2
034100     MOVE "TEST-2" TO PAR-NAME.                                   SG2024.2
034200     MOVE "ALTER NOT CALLD" TO FEATURE.                           SG2024.2
034300     PERFORM PRINT-DETAIL.                                        SG2024.2
034400 TEST-3.                                                          SG2024.2
034500     MOVE SPACE TO CORRECT-A.                                     SG2024.2
034600*    NOTE      THIS TEST WILL ENSURE THAT A PERFORM STATEMENT     SG2024.2
034700*             REFERENCING A OVERLAYABLE FOLLOWED BY A  PERMANENT  SG2024.2
034800*             SEGMENT  OF THE FIXED PORTION WILL BE EXECUTED OK.  SG2024.2
034900     PERFORM P4501 THRU P0003.                                    SG2024.2
035000     IF PERF-OVER-RES IS EQUAL TO "AB" PERFORM PASS               SG2024.2
035100         ELSE MOVE PERF-OVER-RES TO COMPUTED-A                    SG2024.2
035200         MOVE "AB" TO CORRECT-A                                   SG2024.2
035300              PERFORM FAIL.                                       SG2024.2
035400     GO TO TEST-3-WRITE.                                          SG2024.2
035500 TEST-3-DELETE.                                                   SG2024.2
035600     PERFORM DE-LETE.                                             SG2024.2
035700 TEST-3-WRITE.                                                    SG2024.2
035800     MOVE "TEST-3" TO PAR-NAME.                                   SG2024.2
035900     MOVE "PERFORM OVER/FIX" TO FEATURE.                          SG2024.2
036000     PERFORM PRINT-DETAIL.                                        SG2024.2
036100 TEST-4.                                                          SG2024.2
036200     MOVE SPACE TO CORRECT-A.                                     SG2024.2
036300*    NOTE     THIS TEST WILL ENSURE THAT A PERFORM STATEMENT      SG2024.2
036400*             REFERENCING A PERMANENT SEGMENT FOLLOWED BY AN      SG2024.2
036500*             OVERLAYABLE SEGMENT OF THE FIXED PORTION WILL       SG2024.2
036600*             BE EXECUTED OK.                                     SG2024.2
036700     PERFORM P0004 THRU P4802.                                    SG2024.2
036800     IF PERF-RES-OVER EQUAL TO "XY" PERFORM PASS                  SG2024.2
036900         ELSE MOVE PERF-RES-OVER TO COMPUTED-A                    SG2024.2
037000         MOVE "XY" TO CORRECT-A                                   SG2024.2
037100              PERFORM FAIL.                                       SG2024.2
037200     GO TO TEST-4-WRITE.                                          SG2024.2
037300 TEST-4-DELETE.                                                   SG2024.2
037400     PERFORM DE-LETE.                                             SG2024.2
037500 TEST-4-WRITE.                                                    SG2024.2
037600     MOVE "TEST-4" TO PAR-NAME.                                   SG2024.2
037700     MOVE "PERFORM FIX/OVER" TO FEATURE.                          SG2024.2
037800     PERFORM PRINT-DETAIL.                                        SG2024.2
037900 TEST-5.                                                          SG2024.2
038000     MOVE SPACE TO CORRECT-A.                                     SG2024.2
038100*    NOTE     THIS TEST WILL ENSURE THAT THE LOGICAL PATH OF A    SG2024.2
038200*             PROGRAM CAN PROCEED FROM THE PERMANENT SEGMENT OF   SG2024.2
038300*             OF THE FIXED PORTION (IE  IMPLIED FALL-THRU).       SG2024.2
038400     MOVE "A" TO FALL-RSLT-1.                                     SG2024.2
038500 SEC28 SECTION 28.                                                SG2024.2
038600 P2801.                                                           SG2024.2
038700     MOVE "B" TO FALL-RSLT-2.                                     SG2024.2
038800     IF FALL-RSLT EQUAL TO "AB" PERFORM PASS                      SG2024.2
038900         ELSE MOVE FALL-RSLT TO COMPUTED-A                        SG2024.2
039000         MOVE "AB" TO CORRECT-A                                   SG2024.2
039100              PERFORM FAIL.                                       SG2024.2
039200     GO TO TEST-5-WRITE.                                          SG2024.2
039300 TEST-5-DELETE.                                                   SG2024.2
039400     PERFORM DE-LETE.                                             SG2024.2
039500 TEST-5-WRITE.                                                    SG2024.2
039600     MOVE "TEST-5" TO PAR-NAME.                                   SG2024.2
039700     MOVE "FALL THRU IMPLIED" TO FEATURE.                         SG2024.2
039800     PERFORM PRINT-DETAIL.                                        SG2024.2
039900 CLOSE-ROUTINE.                                                   SG2024.2
040000     GO TO CLOSE-FILES.                                           SG2024.2
040100 SEC39 SECTION 39.                                                SG2024.2
040200 P3901.                                                           SG2024.2
040300     GO TO P3902.                                                 SG2024.2
040400 P3902.                                                           SG2024.2
040500     ALTER P3901 TO PROCEED TO P3903.                             SG2024.2
040600     ADD 1 TO LAST-STATE-B.                                       SG2024.2
040700     GO TO P3904.                                                 SG2024.2
040800 P3903.                                                           SG2024.2
040900     ALTER P3901 TO PROCEED TO P3902.                             SG2024.2
041000     ADD 3 TO LAST-STATE-C.                                       SG2024.2
041100 P3904.                                                           SG2024.2
041200     EXIT.                                                        SG2024.2
041300 SEC40 SECTION 40.                                                SG2024.2
041400 P4001.                                                           SG2024.2
041500     GO TO P4002.                                                 SG2024.2
041600 P4002.                                                           SG2024.2
041700     MOVE "A" TO ALTER-NOT-CALL.                                  SG2024.2
041800     GO TO P4004.                                                 SG2024.2
041900 P4003.                                                           SG2024.2
042000     MOVE "B" TO ALTER-NOT-CALL.                                  SG2024.2
042100 P4004.                                                           SG2024.2
042200     EXIT.                                                        SG2024.2
042300 SEC45 SECTION 45.                                                SG2024.2
042400 P4501.                                                           SG2024.2
042500     MOVE "A" TO PERF-OVER-RES-A.                                 SG2024.2
042600 P4502.                                                           SG2024.2
042700     GO TO P0003.                                                 SG2024.2
042800 SEC48 SECTION 48.                                                SG2024.2
042900 P4801.                                                           SG2024.2
043000     MOVE "Y" TO PERF-RES-OVER-B.                                 SG2024.2
043100 P4802.                                                           SG2024.2
043200     EXIT.                                                        SG2024.2
      *END-OF,SG202A                                                                  
