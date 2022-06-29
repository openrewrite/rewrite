      *HEADER,COBOL,SQ201M                                                            
000100 IDENTIFICATION DIVISION.                                         SQ2014.2
000200 PROGRAM-ID.                                                      SQ2014.2
000300     SQ201M.                                                      SQ2014.2
000400****************************************************************  SQ2014.2
000500*                                                              *  SQ2014.2
000600*    VALIDATION FOR:-                                          *  SQ2014.2
000700*    " HIGH       ".                                              SQ2014.2
000800*                                                              *  SQ2014.2
000900*    CREATION DATE     /     VALIDATION DATE                   *  SQ2014.2
001000*    "4.2 ".                                                      SQ2014.2
001100*                                                              *  SQ2014.2
001200*    THIS ROUTINE TESTS THE WRITE ... ADVANCIN  STATEMENT IN      SQ2014.2
001300*    COMBINATION WITH THE                                         SQ2014.2
001400*                NOT AT END-OF-PAGE                               SQ2014.2
001500*    AND THE                                                      SQ2014.2
001600*                END-WRITE        CLAUSES.                        SQ2014.2
001700*                                                                 SQ2014.2
001800*        THE ROUTINE SQ201M TESTS THE USE OF THE LEVEL 2 WRITE    SQ2014.2
001900*    STATEMENT AND THE LINAGE CLAUSE FOR A FILE DESIGNATED AS     SQ2014.2
002000*    PRINTER OUTPUT.  THESE STATEMENTS CONTROL THE VERTICAL       SQ2014.2
002100*    POSITIONING OF EACH LINE ON A PRINTED PAGE.  THE LINAGE      SQ2014.2
002200*    CLAUSE SPECIFICALLY CONTROLS THE VERTICAL FORMAT OF A        SQ2014.2
002300*    LOGICAL PRINT PAGE.  SQ201M TESTS (1) THE ACCURACY OF THE    SQ2014.2
002400*    LINAGE-COUNTER, (2) THE WRITE ADVANCING PAGE, AND (3) THE    SQ2014.2
002500*    FOUR COMBINATIONS OF THE END-OF-PAGE PHRASE.  IT IS ASSUMED  SQ2014.2
002600*    THAT ALL LEVEL 2 NUCLEUS OPTIONS ARE AVAILABLE IN TESTING    SQ2014.2
002700*    SQ201M. A LINAGE CLAUSE WITH COMPLETE FOOTING, TOP, AND      SQ2014.2
002800*    BOTTOM SECTIONS AND UTILIZING INTEGER ITEMS IS USED WITH     SQ2014.2
002900*    THIS TEST.                                                   SQ2014.2
003000 ENVIRONMENT DIVISION.                                            SQ2014.2
003100 CONFIGURATION SECTION.                                           SQ2014.2
003200 SOURCE-COMPUTER.                                                 SQ2014.2
003300     XXXXX082.                                                    SQ2014.2
003400 OBJECT-COMPUTER.                                                 SQ2014.2
003500     XXXXX083.                                                    SQ2014.2
003600 INPUT-OUTPUT SECTION.                                            SQ2014.2
003700 FILE-CONTROL.                                                    SQ2014.2
003800*    SELECT RAW-DATA   ASSIGN TO                                  SQ2014.2
003900*    XXXXX062                                                     SQ2014.2
004000*           ORGANIZATION IS INDEXED                               SQ2014.2
004100*           ACCESS MODE IS RANDOM                                 SQ2014.2
004200*           RECORD KEY IS RAW-DATA-KEY.                           SQ2014.2
004300     SELECT PRINT-FILE ASSIGN TO                                  SQ2014.2
004400     XXXXX055.                                                    SQ2014.2
004500 DATA DIVISION.                                                   SQ2014.2
004600 FILE SECTION.                                                    SQ2014.2
004700*                                                                 SQ2014.2
004800*FD  RAW-DATA.                                                    SQ2014.2
004900*                                                                 SQ2014.2
005000*01  RAW-DATA-SATZ.                                               SQ2014.2
005100*    05  RAW-DATA-KEY        PIC X(6).                            SQ2014.2
005200*    05  C-DATE              PIC 9(6).                            SQ2014.2
005300*    05  C-TIME              PIC 9(8).                            SQ2014.2
005400*    05  C-NO-OF-TESTS       PIC 99.                              SQ2014.2
005500*    05  C-OK                PIC 999.                             SQ2014.2
005600*    05  C-ALL               PIC 999.                             SQ2014.2
005700*    05  C-FAIL              PIC 999.                             SQ2014.2
005800*    05  C-DELETED           PIC 999.                             SQ2014.2
005900*    05  C-INSPECT           PIC 999.                             SQ2014.2
006000*    05  C-NOTE              PIC X(13).                           SQ2014.2
006100*    05  C-INDENT            PIC X.                               SQ2014.2
006200*    05  C-ABORT             PIC X(8).                            SQ2014.2
006300 FD  PRINT-FILE                                                   SQ2014.2
006400C    LABEL RECORDS                                                SQ2014.2
006500C    XXXXX084                                                     SQ2014.2
006600C    DATA RECORD IS PRINT-REC DUMMY-RECORD                        SQ2014.2
006700     LINAGE IS 50 LINES                                           SQ2014.2
006800              WITH FOOTING AT 45                                  SQ2014.2
006900              LINES AT TOP 10                                     SQ2014.2
007000              LINES AT BOTTOM 6.                                  SQ2014.2
007100 01  PRINT-REC PICTURE X(120).                                    SQ2014.2
007200 01  DUMMY-RECORD PICTURE X(120).                                 SQ2014.2
007300 WORKING-STORAGE SECTION.                                         SQ2014.2
007400 01  WRITE-SWITCH       PIC 9  VALUE 0.                           SQ2014.2
007500 01  END-WRITE-SWITCH   PIC 9  VALUE 1.                           SQ2014.2
007600 01  LC-HOLD            PIC 99.                                   SQ2014.2
007700 01  IDENTIFIER-2       PIC 99.                                   SQ2014.2
007800 01  TOP-LINE           PIC X(120)   VALUE "THIS LINE WAS WRITTEN SQ2014.2
007900-    "BY A WRITE ADVANCING PAGE OPERATION.  IT SHOULD APPEAR AS THSQ2014.2
008000-    "E FIRST LINE OF A NEW LOGICAL PAGE.".                       SQ2014.2
008100 01  DETAIL-LINE.                                                 SQ2014.2
008200     02 FILLER          PIC X(20)    VALUE SPACE.                 SQ2014.2
008300     02 FILLER          PIC X(13)    VALUE "THIS IS LINE ".       SQ2014.2
008400     02 DETAIL-LINE-NO  PIC 999.                                  SQ2014.2
008500     02 FILLER          PIC X(52)   VALUE " OF 132 DETAIL LINES.".SQ2014.2
008600     02 FILLER          PIC X(18)    VALUE "LINAGE-COUNTER IS ".  SQ2014.2
008700     02 DETAIL-LC       PIC 99.                                   SQ2014.2
008800     02 FILLER          PIC X(12)    VALUE ".".                   SQ2014.2
008900 01  FOOT-LINE.                                                   SQ2014.2
009000     02 FILLER          PIC X(20)    VALUE SPACE.                 SQ2014.2
009100     02 FILLER          PIC X(13)    VALUE "THIS IS LINE ".       SQ2014.2
009200     02 FOOT-COUNT      PIC 999.                                  SQ2014.2
009300     02 FILLER          PIC X(47)    VALUE " OF 6 FOOTING LINES.  SQ2014.2
009400-    "LINAGE-COUNTER SHOULD BE ".                                 SQ2014.2
009500     02 FOOT-LINE-NO    PIC 99.                                   SQ2014.2
009600     02 FILLER          PIC X(21)   VALUE ".  LINAGE-COUNTER IS ".SQ2014.2
009700     02 FOOT-LC         PIC 99.                                   SQ2014.2
009800     02 FILLER          PIC X(12)    VALUE ".".                   SQ2014.2
009900 01  EOP-MESSAGE-1      PIC X(120)   VALUE "          THIS IS A TESQ2014.2
010000-    "ST FOR THE EOP PHRASE.  50 LINES SHOULD PRINT IN THE PAGE BOSQ2014.2
010100-    "DY INCLUDING 44 DETAIL LINES AND".                          SQ2014.2
010200 01  EOP-MESSAGE-2      PIC X(120)   VALUE "          6 FOOTING LISQ2014.2
010300-    "NES.  THESE LINES SHOULD BE CONSECUTIVE ON ONE LOGICAL PAGE SQ2014.2
010400-    "AND BE FOLLOWED BY 16 TOP AND BOTTOM".                      SQ2014.2
010500 01  EOP-MESSAGE-3      PIC X(120)   VALUE "          BLANK LINES.SQ2014.2
010600-    "  THE CORRECT AND COMPUTED LINAGE-COUNTER VALUES IN THE FOOTSQ2014.2
010700-    "ING LINES SHOULD BE EQUAL.".                                SQ2014.2
010800 01  TEST-RESULTS.                                                SQ2014.2
010900     02 FILLER                    PICTURE X VALUE SPACE.          SQ2014.2
011000     02 FEATURE                   PICTURE X(20) VALUE SPACE.      SQ2014.2
011100     02 FILLER                    PICTURE X VALUE SPACE.          SQ2014.2
011200     02 P-OR-F                    PICTURE X(5) VALUE SPACE.       SQ2014.2
011300     02 FILLER                    PICTURE X  VALUE SPACE.         SQ2014.2
011400     02  PAR-NAME.                                                SQ2014.2
011500       03 FILLER PICTURE X(12) VALUE SPACE.                       SQ2014.2
011600       03  PARDOT-X PICTURE X  VALUE SPACE.                       SQ2014.2
011700       03 DOTVALUE PICTURE 99  VALUE ZERO.                        SQ2014.2
011800       03 FILLER PIC X(5) VALUE SPACE.                            SQ2014.2
011900     02 FILLER PIC X(10) VALUE SPACE.                             SQ2014.2
012000     02 RE-MARK PIC X(61).                                        SQ2014.2
012100 01  TEST-COMPUTED.                                               SQ2014.2
012200     02 FILLER PIC X(30) VALUE SPACE.                             SQ2014.2
012300     02 FILLER PIC X(17) VALUE "       COMPUTED=".                SQ2014.2
012400     02 COMPUTED-X.                                               SQ2014.2
012500     03 COMPUTED-A                PICTURE X(20) VALUE SPACE.      SQ2014.2
012600     03 COMPUTED-N REDEFINES COMPUTED-A PICTURE -9(9).9(9).       SQ2014.2
012700     03 COMPUTED-0V18 REDEFINES COMPUTED-A  PICTURE -.9(18).      SQ2014.2
012800     03 COMPUTED-4V14 REDEFINES COMPUTED-A  PICTURE -9(4).9(14).  SQ2014.2
012900     03 COMPUTED-14V4 REDEFINES COMPUTED-A  PICTURE -9(14).9(4).  SQ2014.2
013000     03       CM-18V0 REDEFINES COMPUTED-A.                       SQ2014.2
013100         04 COMPUTED-18V0                   PICTURE -9(18).       SQ2014.2
013200         04 FILLER                          PICTURE X.            SQ2014.2
013300     03 FILLER PIC X(50) VALUE SPACE.                             SQ2014.2
013400 01  TEST-CORRECT.                                                SQ2014.2
013500     02 FILLER PIC X(30) VALUE SPACE.                             SQ2014.2
013600     02 FILLER PIC X(17) VALUE "       CORRECT =".                SQ2014.2
013700     02 CORRECT-X.                                                SQ2014.2
013800     03 CORRECT-A                 PICTURE X(20) VALUE SPACE.      SQ2014.2
013900     03 CORRECT-N REDEFINES CORRECT-A PICTURE -9(9).9(9).         SQ2014.2
014000     03 CORRECT-0V18 REDEFINES CORRECT-A    PICTURE -.9(18).      SQ2014.2
014100     03 CORRECT-4V14 REDEFINES CORRECT-A    PICTURE -9(4).9(14).  SQ2014.2
014200     03 CORRECT-14V4 REDEFINES CORRECT-A    PICTURE -9(14).9(4).  SQ2014.2
014300     03      CR-18V0 REDEFINES CORRECT-A.                         SQ2014.2
014400         04 CORRECT-18V0                    PICTURE -9(18).       SQ2014.2
014500         04 FILLER                          PICTURE X.            SQ2014.2
014600     03 FILLER PIC X(50) VALUE SPACE.                             SQ2014.2
014700 01  CCVS-C-1.                                                    SQ2014.2
014800     02 FILLER PICTURE IS X(99) VALUE IS " FEATURE              PASQ2014.2
014900-    "SS  PARAGRAPH-NAME                                          SQ2014.2
015000-    "        REMARKS".                                           SQ2014.2
015100     02 FILLER PICTURE IS X(20) VALUE IS SPACE.                   SQ2014.2
015200 01  CCVS-C-2.                                                    SQ2014.2
015300     02 FILLER PICTURE IS X VALUE IS SPACE.                       SQ2014.2
015400     02 FILLER PICTURE IS X(6) VALUE IS "TESTED".                 SQ2014.2
015500     02 FILLER PICTURE IS X(15) VALUE IS SPACE.                   SQ2014.2
015600     02 FILLER PICTURE IS X(4) VALUE IS "FAIL".                   SQ2014.2
015700     02 FILLER PICTURE IS X(94) VALUE IS SPACE.                   SQ2014.2
015800 01  REC-SKL-SUB PICTURE 9(2) VALUE ZERO.                         SQ2014.2
015900 01  REC-CT PICTURE 99 VALUE ZERO.                                SQ2014.2
016000 01  DELETE-CNT                   PICTURE 999  VALUE ZERO.        SQ2014.2
016100 01  ERROR-COUNTER PICTURE IS 999 VALUE IS ZERO.                  SQ2014.2
016200 01  INSPECT-COUNTER PIC 999 VALUE 11.                            SQ2014.2
016300 01  PASS-COUNTER PIC 999 VALUE ZERO.                             SQ2014.2
016400 01  TOTAL-ERROR PIC 999 VALUE ZERO.                              SQ2014.2
016500 01  ERROR-HOLD PIC 999 VALUE ZERO.                               SQ2014.2
016600 01  DUMMY-HOLD PIC X(120) VALUE SPACE.                           SQ2014.2
016700 01  RECORD-COUNT PIC 9(5) VALUE ZERO.                            SQ2014.2
016800 01  CCVS-H-1.                                                    SQ2014.2
016900     02  FILLER   PICTURE X(27)  VALUE SPACE.                     SQ2014.2
017000     02 FILLER PICTURE X(67) VALUE                                SQ2014.2
017100     " FEDERAL SOFTWARE TESTING CENTER COBOL COMPILER VALIDATION  SQ2014.2
017200-    " SYSTEM".                                                   SQ2014.2
017300     02  FILLER     PICTURE X(26)  VALUE SPACE.                   SQ2014.2
017400 01  CCVS-H-2.                                                    SQ2014.2
017500     02 FILLER PICTURE X(52) VALUE IS                             SQ2014.2
017600     "CCVS85 FSTC COPY, NOT FOR DISTRIBUTION.".                   SQ2014.2
017700     02 FILLER PICTURE IS X(19) VALUE IS "TEST RESULTS SET-  ".   SQ2014.2
017800     02 TEST-ID PICTURE IS X(9).                                  SQ2014.2
017900     02 FILLER PICTURE IS X(40) VALUE IS SPACE.                   SQ2014.2
018000 01  CCVS-H-3.                                                    SQ2014.2
018100     02  FILLER PICTURE X(34) VALUE                               SQ2014.2
018200     " FOR OFFICIAL USE ONLY    ".                                SQ2014.2
018300     02  FILLER PICTURE X(58) VALUE                               SQ2014.2
018400     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ2014.2
018500     02  FILLER PICTURE X(28) VALUE                               SQ2014.2
018600     "  COPYRIGHT   1985 ".                                       SQ2014.2
018700 01  CCVS-E-1.                                                    SQ2014.2
018800     02 FILLER PICTURE IS X(52) VALUE IS SPACE.                   SQ2014.2
018900     02 FILLER PICTURE IS X(14) VALUE IS "END OF TEST-  ".        SQ2014.2
019000     02 ID-AGAIN PICTURE IS X(9).                                 SQ2014.2
019100     02 FILLER PICTURE X(45) VALUE IS                             SQ2014.2
019200     " NTIS DISTRIBUTION COBOL 85".                               SQ2014.2
019300 01  CCVS-E-2.                                                    SQ2014.2
019400     02  FILLER                   PICTURE X(31)  VALUE            SQ2014.2
019500     SPACE.                                                       SQ2014.2
019600     02  FILLER                   PICTURE X(21)  VALUE SPACE.     SQ2014.2
019700     02 CCVS-E-2-2.                                               SQ2014.2
019800         03 ERROR-TOTAL PICTURE IS XXX VALUE IS SPACE.            SQ2014.2
019900         03 FILLER PICTURE IS X VALUE IS SPACE.                   SQ2014.2
020000         03 ENDER-DESC PIC X(46) VALUE "ERRORS ENCOUNTERED".      SQ2014.2
020100 01  CCVS-E-3.                                                    SQ2014.2
020200     02  FILLER PICTURE X(22) VALUE                               SQ2014.2
020300     " FOR OFFICIAL USE ONLY".                                    SQ2014.2
020400     02  FILLER PICTURE X(12) VALUE SPACE.                        SQ2014.2
020500     02  FILLER PICTURE X(58) VALUE                               SQ2014.2
020600     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ2014.2
020700     02  FILLER PICTURE X(13) VALUE SPACE.                        SQ2014.2
020800     02 FILLER PIC X(15) VALUE " COPYRIGHT 1985".                 SQ2014.2
020900 01  CCVS-E-4.                                                    SQ2014.2
021000     02 CCVS-E-4-1 PIC XXX VALUE SPACE.                           SQ2014.2
021100     02 FILLER PIC XXXX VALUE " OF ".                             SQ2014.2
021200     02 CCVS-E-4-2 PIC XXX VALUE SPACE.                           SQ2014.2
021300     02 FILLER PIC X(40) VALUE                                    SQ2014.2
021400      "  TESTS WERE EXECUTED SUCCESSFULLY".                       SQ2014.2
021500 01  XXINFO.                                                      SQ2014.2
021600     02 FILLER PIC X(30) VALUE "        *** INFORMATION  ***".    SQ2014.2
021700     02 INFO-TEXT.                                                SQ2014.2
021800     04 FILLER PIC X(20) VALUE SPACE.                             SQ2014.2
021900     04 XXCOMPUTED PIC X(20).                                     SQ2014.2
022000     04 FILLER PIC X(5) VALUE SPACE.                              SQ2014.2
022100     04 XXCORRECT PIC X(20).                                      SQ2014.2
022200 01  HYPHEN-LINE.                                                 SQ2014.2
022300     02 FILLER PICTURE IS X VALUE IS SPACE.                       SQ2014.2
022400     02 FILLER PICTURE IS X(65) VALUE IS "************************SQ2014.2
022500-    "*****************************************".                 SQ2014.2
022600     02 FILLER PICTURE IS X(54) VALUE IS "************************SQ2014.2
022700-    "******************************".                            SQ2014.2
022800 01  CCVS-PGM-ID PIC X(6) VALUE                                   SQ2014.2
022900     "SQ201M".                                                    SQ2014.2
023000 PROCEDURE DIVISION.                                              SQ2014.2
023100 CCVS1 SECTION.                                                   SQ2014.2
023200 OPEN-FILES.                                                      SQ2014.2
023300*    OPEN I-O RAW-DATA.                                           SQ2014.2
023400*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            SQ2014.2
023500*    MOVE "ABORTED " TO C-ABORT.                                  SQ2014.2
023600*    READ RAW-DATA INVALID KEY GO TO END-E-1.                     SQ2014.2
023700*    MOVE "ABORTED " TO C-ABORT.                                  SQ2014.2
023800*    ADD 1 TO C-NO-OF-TESTS.                                      SQ2014.2
023900*    ACCEPT C-DATE  FROM DATE.                                    SQ2014.2
024000*    ACCEPT C-TIME  FROM TIME.                                    SQ2014.2
024100*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-1.             SQ2014.2
024200*END-E-1.                                                         SQ2014.2
024300*    CLOSE RAW-DATA.                                              SQ2014.2
024400     OPEN     OUTPUT PRINT-FILE.                                  SQ2014.2
024500     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   SQ2014.2
024600     MOVE    SPACE TO TEST-RESULTS.                               SQ2014.2
024700     GO TO CCVS1-EXIT.                                            SQ2014.2
024800 CLOSE-FILES.                                                     SQ2014.2
024900     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   SQ2014.2
025000*    OPEN I-O RAW-DATA.                                           SQ2014.2
025100*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            SQ2014.2
025200*    READ RAW-DATA INVALID KEY GO TO END-E-2.                     SQ2014.2
025300*    MOVE "OK.     " TO C-ABORT.                                  SQ2014.2
025400*    MOVE PASS-COUNTER TO C-OK.                                   SQ2014.2
025500*    MOVE ERROR-HOLD   TO C-ALL.                                  SQ2014.2
025600*    MOVE ERROR-COUNTER TO C-FAIL.                                SQ2014.2
025700*    MOVE DELETE-CNT TO C-DELETED.                                SQ2014.2
025800*    MOVE INSPECT-COUNTER TO C-INSPECT.                           SQ2014.2
025900*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-2.             SQ2014.2
026000*END-E-2.                                                         SQ2014.2
026100*    CLOSE RAW-DATA.                                              SQ2014.2
026200 TERMINATE-CCVS.                                                  SQ2014.2
026300*    EXIT PROGRAM.                                                SQ2014.2
026400*TERMINATE-CALL.                                                  SQ2014.2
026500     STOP     RUN.                                                SQ2014.2
026600 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         SQ2014.2
026700 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           SQ2014.2
026800 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          SQ2014.2
026900 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-CNT.          SQ2014.2
027000     MOVE "****TEST DELETED****" TO RE-MARK.                      SQ2014.2
027100 PRINT-DETAIL.                                                    SQ2014.2
027200     IF REC-CT NOT EQUAL TO ZERO                                  SQ2014.2
027300             MOVE "." TO PARDOT-X                                 SQ2014.2
027400             MOVE REC-CT TO DOTVALUE.                             SQ2014.2
027500     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      SQ2014.2
027600     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               SQ2014.2
027700        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 SQ2014.2
027800          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 SQ2014.2
027900     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              SQ2014.2
028000     MOVE SPACE TO CORRECT-X.                                     SQ2014.2
028100     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         SQ2014.2
028200     MOVE     SPACE TO RE-MARK.                                   SQ2014.2
028300 HEAD-ROUTINE.                                                    SQ2014.2
028400     MOVE CCVS-H-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ2014.2
028500     MOVE CCVS-H-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.   SQ2014.2
028600     MOVE CCVS-H-3 TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.   SQ2014.2
028700 COLUMN-NAMES-ROUTINE.                                            SQ2014.2
028800     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ2014.2
028900     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ2014.2
029000     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ2014.2
029100 END-ROUTINE.                                                     SQ2014.2
029200     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.SQ2014.2
029300 END-RTN-EXIT.                                                    SQ2014.2
029400     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ2014.2
029500 END-ROUTINE-1.                                                   SQ2014.2
029600      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      SQ2014.2
029700      ERROR-HOLD. ADD DELETE-CNT TO ERROR-HOLD.                   SQ2014.2
029800      ADD PASS-COUNTER TO ERROR-HOLD.                             SQ2014.2
029900*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   SQ2014.2
030000      MOVE PASS-COUNTER TO CCVS-E-4-1.                            SQ2014.2
030100      MOVE ERROR-HOLD TO CCVS-E-4-2.                              SQ2014.2
030200      MOVE CCVS-E-4 TO CCVS-E-2-2.                                SQ2014.2
030300      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           SQ2014.2
030400  END-ROUTINE-12.                                                 SQ2014.2
030500      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        SQ2014.2
030600     IF       ERROR-COUNTER IS EQUAL TO ZERO                      SQ2014.2
030700         MOVE "NO " TO ERROR-TOTAL                                SQ2014.2
030800         ELSE                                                     SQ2014.2
030900         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       SQ2014.2
031000     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           SQ2014.2
031100     PERFORM WRITE-LINE.                                          SQ2014.2
031200 END-ROUTINE-13.                                                  SQ2014.2
031300     IF DELETE-CNT IS EQUAL TO ZERO                               SQ2014.2
031400         MOVE "NO " TO ERROR-TOTAL  ELSE                          SQ2014.2
031500         MOVE DELETE-CNT TO ERROR-TOTAL.                          SQ2014.2
031600     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   SQ2014.2
031700     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ2014.2
031800      IF   INSPECT-COUNTER EQUAL TO ZERO                          SQ2014.2
031900          MOVE "NO " TO ERROR-TOTAL                               SQ2014.2
032000      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   SQ2014.2
032100      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            SQ2014.2
032200      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          SQ2014.2
032300     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ2014.2
032400 WRITE-LINE.                                                      SQ2014.2
032500     ADD 1 TO RECORD-COUNT.                                       SQ2014.2
032600*    IF RECORD-COUNT GREATER 50                                   SQ2014.2
032700*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          SQ2014.2
032800*        MOVE SPACE TO DUMMY-RECORD                               SQ2014.2
032900*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  SQ2014.2
033000*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             SQ2014.2
033100*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     SQ2014.2
033200*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          SQ2014.2
033300*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          SQ2014.2
033400*        MOVE ZERO TO RECORD-COUNT.                               SQ2014.2
033500     PERFORM WRT-LN.                                              SQ2014.2
033600 WRT-LN.                                                          SQ2014.2
033700     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               SQ2014.2
033800     MOVE SPACE TO DUMMY-RECORD.                                  SQ2014.2
033900 BLANK-LINE-PRINT.                                                SQ2014.2
034000     PERFORM WRT-LN.                                              SQ2014.2
034100 FAIL-ROUTINE.                                                    SQ2014.2
034200     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.   SQ2014.2
034300     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.    SQ2014.2
034400     MOVE "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.    SQ2014.2
034500     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     SQ2014.2
034600     GO TO FAIL-ROUTINE-EX.                                       SQ2014.2
034700 FAIL-ROUTINE-WRITE.                                              SQ2014.2
034800     MOVE TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE           SQ2014.2
034900     MOVE TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES.   SQ2014.2
035000 FAIL-ROUTINE-EX. EXIT.                                           SQ2014.2
035100 BAIL-OUT.                                                        SQ2014.2
035200     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.       SQ2014.2
035300     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.               SQ2014.2
035400 BAIL-OUT-WRITE.                                                  SQ2014.2
035500     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  SQ2014.2
035600     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     SQ2014.2
035700 BAIL-OUT-EX. EXIT.                                               SQ2014.2
035800 CCVS1-EXIT.                                                      SQ2014.2
035900     EXIT.                                                        SQ2014.2
036000 SECT-SQ201M-0001 SECTION.                                        SQ2014.2
036100 WRT-TEST-001.                                                    SQ2014.2
036200*             THIS TEST CHECKS THE LINAGE-COUNTER UPON COMPLETION SQ2014.2
036300*             OF AN OPEN COMMAND.  IT SHOULD BE EQUAL TO 1.       SQ2014.2
036400     CLOSE PRINT-FILE.                                            SQ2014.2
036500     OPEN OUTPUT PRINT-FILE.                                      SQ2014.2
036600     IF LINAGE-COUNTER EQUAL TO 1                                 SQ2014.2
036700              PERFORM PASS                                        SQ2014.2
036800              GO TO WRT-WRITE-001.                                SQ2014.2
036900     GO TO WRT-FAIL-001.                                          SQ2014.2
037000 WRT-DELETE-001.                                                  SQ2014.2
037100     PERFORM DE-LETE.                                             SQ2014.2
037200     GO TO WRT-WRITE-001.                                         SQ2014.2
037300 WRT-FAIL-001.                                                    SQ2014.2
037400     MOVE "VII-5 1.3.8; VII-29 D." TO RE-MARK.                    SQ2014.2
037500     PERFORM FAIL.                                                SQ2014.2
037600     MOVE LINAGE-COUNTER TO COMPUTED-18V0.                        SQ2014.2
037700     MOVE 1 TO CORRECT-18V0.                                      SQ2014.2
037800 WRT-WRITE-001.                                                   SQ2014.2
037900     MOVE "LINAGE-CT AFTER OPEN" TO FEATURE.                      SQ2014.2
038000     MOVE "WRT-TEST-01" TO PAR-NAME.                              SQ2014.2
038100     MOVE "FILE IS CLOSED, THEN OPENED" TO RE-MARK.               SQ2014.2
038200     PERFORM PRINT-DETAIL.                                        SQ2014.2
038300     MOVE SPACE TO DUMMY-RECORD.                                  SQ2014.2
038400     WRITE PRINT-REC AFTER ADVANCING 4 LINES.                     SQ2014.2
038500 WRT-INIT-GF-001.                                                 SQ2014.2
038600     PERFORM HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.              SQ2014.2
038700     MOVE "TEST WRT-TEST-01 MUST BE PRINTED BEFORE THE HEADER OF TSQ2014.2
038800-    "HIS LIST"  TO PRINT-REC.                                    SQ2014.2
038900     WRITE PRINT-REC AFTER ADVANCING 4 LINES.                     SQ2014.2
039000     MOVE "THIS PROGRAM TESTS THE STATEMENT:" TO PRINT-REC.       SQ2014.2
039100     WRITE PRINT-REC AFTER ADVANCING 4 LINES.                     SQ2014.2
039200     MOVE "   WRITE ...   ADVANCING ... " TO PRINT-REC.           SQ2014.2
039300     WRITE PRINT-REC AFTER ADVANCING 2 LINE.                      SQ2014.2
039400     MOVE "THE RULES ARE DESCRIBED ON PAGE VII-52 THROUGH VII-56."SQ2014.2
039500          TO PRINT-REC.                                           SQ2014.2
039600     WRITE PRINT-REC AFTER ADVANCING 2 LINE.                      SQ2014.2
039700     MOVE "THE LOGICAL PAGE SIZE IS EQUAL TO       66"            SQ2014.2
039800          TO PRINT-REC.                                           SQ2014.2
039900     WRITE PRINT-REC AFTER ADVANCING 4 LINE.                      SQ2014.2
040000 WRT-TEST-002.                                                    SQ2014.2
040100*             THIS TEST CHECKS THE LINAGE-COUNTER UPON COMPLETION SQ2014.2
040200*             OF A WRITE ADVANCING PAGE OPERATION.                SQ2014.2
040300*             IT SHOULD BE EQUAL TO 1.                            SQ2014.2
040400     MOVE SPACE TO DUMMY-RECORD.                                  SQ2014.2
040500     WRITE DUMMY-RECORD AFTER ADVANCING PAGE.                     SQ2014.2
040600     IF LINAGE-COUNTER EQUAL TO 1                                 SQ2014.2
040700              PERFORM PASS                                        SQ2014.2
040800              GO TO WRT-WRITE-002.                                SQ2014.2
040900     GO TO WRT-FAIL-002.                                          SQ2014.2
041000 WRT-DELETE-002.                                                  SQ2014.2
041100     PERFORM DE-LETE.                                             SQ2014.2
041200     GO TO WRT-WRITE-002.                                         SQ2014.2
041300 WRT-FAIL-002.                                                    SQ2014.2
041400     MOVE "VII-5 1.3.8; VII-28 C (1)" TO RE-MARK.                 SQ2014.2
041500     PERFORM FAIL.                                                SQ2014.2
041600     MOVE LINAGE-COUNTER TO COMPUTED-18V0.                        SQ2014.2
041700     MOVE 1 TO CORRECT-18V0.                                      SQ2014.2
041800 WRT-WRITE-002.                                                   SQ2014.2
041900     MOVE "L-C AFTER WRITE PAGE" TO FEATURE.                      SQ2014.2
042000     MOVE "WRT-TEST-02" TO PAR-NAME.                              SQ2014.2
042100     PERFORM PRINT-DETAIL.                                        SQ2014.2
042200 WRT-TEST-003.                                                    SQ2014.2
042300*             THIS TEST CHECKS THE LINAGE-COUNTER UPON COMPLETION SQ2014.2
042400*             OF A WRITE AFTER ADVANCING 1 LINE OPERATION ON WHICHSQ2014.2
042500*             LOGICAL PAGE OVERFLOW OCCURS.  IT SHOULD EQUAL 1.   SQ2014.2
042600     MOVE SPACE TO DUMMY-RECORD.                                  SQ2014.2
042700     WRITE DUMMY-RECORD AFTER ADVANCING PAGE.                     SQ2014.2
042800     PERFORM BLANK-LINE-PRINT 50 TIMES.                           SQ2014.2
042900     IF LINAGE-COUNTER EQUAL TO 1                                 SQ2014.2
043000              PERFORM PASS                                        SQ2014.2
043100              GO TO WRT-WRITE-003.                                SQ2014.2
043200     GO TO WRT-FAIL-003.                                          SQ2014.2
043300 WRT-DELETE-003.                                                  SQ2014.2
043400     PERFORM DE-LETE.                                             SQ2014.2
043500     GO TO WRT-WRITE-003.                                         SQ2014.2
043600 WRT-FAIL-003.                                                    SQ2014.2
043700     MOVE "VII-5 1.3.8; VII-29 C  4)" TO RE-MARK.                 SQ2014.2
043800     PERFORM FAIL.                                                SQ2014.2
043900     MOVE LINAGE-COUNTER TO COMPUTED-18V0.                        SQ2014.2
044000     MOVE 1 TO CORRECT-18V0.                                      SQ2014.2
044100 WRT-WRITE-003.                                                   SQ2014.2
044200     MOVE "L-C AFT PAGE OVERFLW" TO FEATURE.                      SQ2014.2
044300     MOVE "WRT-TEST-03" TO PAR-NAME.                              SQ2014.2
044400     PERFORM PRINT-DETAIL.                                        SQ2014.2
044500 WRT-TEST-004.                                                    SQ2014.2
044600*             THIS TEST CHECKS THE LINAGE-COUNTER UPON COMPLETION SQ2014.2
044700*             OF A WRITE OPERATION WITHOUT AN ADVANCING PHRASE.   SQ2014.2
044800*             IT SHOULD BE INCREMENTED BY 1.                      SQ2014.2
044900     MOVE LINAGE-COUNTER TO LC-HOLD.                              SQ2014.2
045000     ADD 1 TO LC-HOLD.                                            SQ2014.2
045100     MOVE SPACE TO DUMMY-RECORD.                                  SQ2014.2
045200     WRITE DUMMY-RECORD.                                          SQ2014.2
045300     IF LC-HOLD EQUAL TO LINAGE-COUNTER                           SQ2014.2
045400              PERFORM PASS                                        SQ2014.2
045500              GO TO WRT-WRITE-004.                                SQ2014.2
045600     GO TO WRT-FAIL-004.                                          SQ2014.2
045700 WRT-DELETE-004.                                                  SQ2014.2
045800     PERFORM DE-LETE.                                             SQ2014.2
045900     GO TO WRT-WRITE-004.                                         SQ2014.2
046000 WRT-FAIL-004.                                                    SQ2014.2
046100     MOVE "VII-5 1.3.8; VII-29 C  3)" TO RE-MARK.                 SQ2014.2
046200     PERFORM FAIL.                                                SQ2014.2
046300     MOVE LINAGE-COUNTER TO COMPUTED-18V0.                        SQ2014.2
046400     MOVE LC-HOLD TO CORRECT-18V0.                                SQ2014.2
046500 WRT-WRITE-004.                                                   SQ2014.2
046600     MOVE "L-C AFT WRT W/O ADV" TO FEATURE.                       SQ2014.2
046700     MOVE "WRT-TEST-04" TO PAR-NAME.                              SQ2014.2
046800     PERFORM PRINT-DETAIL.                                        SQ2014.2
046900 WRT-TEST-005.                                                    SQ2014.2
047000*             THIS TEST CHECKS THE LINAGE-COUNTER UPON COMPLETION SQ2014.2
047100*             OF A WRITE ADVANCING INTEGER LINE OPERATION.        SQ2014.2
047200     MOVE LINAGE-COUNTER TO LC-HOLD.                              SQ2014.2
047300     ADD 5 TO LC-HOLD.                                            SQ2014.2
047400     MOVE SPACE TO DUMMY-RECORD.                                  SQ2014.2
047500     WRITE DUMMY-RECORD BEFORE ADVANCING 5 LINE.                  SQ2014.2
047600     IF LINAGE-COUNTER EQUAL TO LC-HOLD                           SQ2014.2
047700              PERFORM PASS                                        SQ2014.2
047800              GO TO WRT-WRITE-005.                                SQ2014.2
047900     GO TO WRT-FAIL-005.                                          SQ2014.2
048000 WRT-DELETE-005.                                                  SQ2014.2
048100     PERFORM DE-LETE.                                             SQ2014.2
048200     GO TO WRT-WRITE-005.                                         SQ2014.2
048300 WRT-FAIL-005.                                                    SQ2014.2
048400     MOVE "VII-5 1.3.8; VII-28 (9) C  1) INTEGER" TO RE-MARK.     SQ2014.2
048500     PERFORM FAIL.                                                SQ2014.2
048600     MOVE LINAGE-COUNTER TO COMPUTED-18V0.                        SQ2014.2
048700     MOVE LC-HOLD TO CORRECT-18V0.                                SQ2014.2
048800 WRT-WRITE-005.                                                   SQ2014.2
048900     MOVE "L-C AFT WRT ADV INT" TO FEATURE.                       SQ2014.2
049000     MOVE "WRT-TEST-05" TO PAR-NAME.                              SQ2014.2
049100     PERFORM PRINT-DETAIL.                                        SQ2014.2
049200 WRT-TEST-006.                                                    SQ2014.2
049300*             THIS TEST CHECKS THE LINAGE-COUNTER UPON COMPLETION SQ2014.2
049400*             OF A WRITE IDENTIFIER-2 LINES OPERATION.            SQ2014.2
049500     MOVE 4 TO IDENTIFIER-2.                                      SQ2014.2
049600     MOVE LINAGE-COUNTER TO LC-HOLD.                              SQ2014.2
049700     ADD 4 TO LC-HOLD.                                            SQ2014.2
049800     MOVE SPACE TO DUMMY-RECORD.                                  SQ2014.2
049900     WRITE DUMMY-RECORD BEFORE ADVANCING IDENTIFIER-2 LINES.      SQ2014.2
050000     IF LINAGE-COUNTER EQUAL TO LC-HOLD                           SQ2014.2
050100              PERFORM PASS                                        SQ2014.2
050200              GO TO WRT-WRITE-006.                                SQ2014.2
050300     GO TO WRT-FAIL-006.                                          SQ2014.2
050400 WRT-DELETE-006.                                                  SQ2014.2
050500     PERFORM DE-LETE.                                             SQ2014.2
050600     GO TO WRT-WRITE-006.                                         SQ2014.2
050700 WRT-FAIL-006.                                                    SQ2014.2
050800     MOVE "VII-5 1.3.8; VII-29 (9) C  2) IDENTIFIER-2" TO RE-MARK.SQ2014.2
050900     PERFORM FAIL.                                                SQ2014.2
051000     MOVE LINAGE-COUNTER TO COMPUTED-18V0.                        SQ2014.2
051100     MOVE LC-HOLD TO CORRECT-18V0.                                SQ2014.2
051200 WRT-WRITE-006.                                                   SQ2014.2
051300     MOVE "L-C AFT WRT ADV ID-2" TO FEATURE.                      SQ2014.2
051400     MOVE "WRT-TEST-06" TO PAR-NAME.                              SQ2014.2
051500     PERFORM PRINT-DETAIL.                                        SQ2014.2
051600 WRT-INIT-007.                                                    SQ2014.2
051700     PERFORM BLANK-LINE-PRINT 10 TIMES.                           SQ2014.2
051800     MOVE "THE FOLLOWING SQ201M TESTS CANNOT BE TESTED USING THE NSQ2014.2
051900-    "ORMAL PASS/FAIL METHODS.  A VISUAL CHECK WILL HAVE TO TO BE SQ2014.2
052000-    "MADE" TO PRINT-REC.                                         SQ2014.2
052100     WRITE PRINT-REC AFTER ADVANCING 1 LINE.                      SQ2014.2
052200     MOVE "TO DETERMINE THE ACCURACY OF EACH TEST" TO PRINT-REC.  SQ2014.2
052300     WRITE PRINT-REC AFTER ADVANCING 1 LINE.                      SQ2014.2
052400     MOVE SPACE TO DUMMY-RECORD.                                  SQ2014.2
052500     PERFORM BLANK-LINE-PRINT 10 TIMES.                           SQ2014.2
052600 WRT-TEST-007.                                                    SQ2014.2
052700*             THIS IS A TEST FOR WRITE AFTER ADVANCING PAGE.      SQ2014.2
052800*             THE RECORD SHOULD PRINT ON THE FIRST LINE OF THE    SQ2014.2
052900*             NEXT LOGICAL PAGE.                                  SQ2014.2
053000     MOVE "WRT AFT ADV PAGE" TO FEATURE.                          SQ2014.2
053100     MOVE "WRT-TEST-07" TO PAR-NAME.                              SQ2014.2
053200     PERFORM PRINT-DETAIL.                                        SQ2014.2
053300     MOVE TOP-LINE TO PRINT-REC.                                  SQ2014.2
053400     WRITE PRINT-REC AFTER ADVANCING PAGE.                        SQ2014.2
053500     MOVE SPACE TO DUMMY-RECORD.                                  SQ2014.2
053600 WRT-TEST-008.                                                    SQ2014.2
053700******************************************************************SQ2014.2
053800*                                                                *SQ2014.2
053900*             THIS IS A TEST FOR WRITE ... NOT AT END-OF-PAGE... *SQ2014.2
054000*                                          ---                   *SQ2014.2
054100******************************************************************SQ2014.2
054200     MOVE 0 TO WRITE-SWITCH.                                      SQ2014.2
054300     PERFORM WRITE-EOP-MESSAGE.                                   SQ2014.2
054400     MOVE SPACE TO DUMMY-RECORD.                                  SQ2014.2
054500     PERFORM BLANK-LINE-PRINT UNTIL LINAGE-COUNTER EQUAL 1.       SQ2014.2
054600     MOVE 1 TO DETAIL-LINE-NO.                                    SQ2014.2
054700     PERFORM EOP-TEST-1 132 TIMES.                                SQ2014.2
054800 VAR-TEST-008.                                                    SQ2014.2
054900     IF WRITE-SWITCH NOT = 1                                      SQ2014.2
055000         GO TO VAR-FAIL-008.                                      SQ2014.2
055100 VAR-PASS-008.                                                    SQ2014.2
055200     PERFORM PASS.                                                SQ2014.2
055300     GO TO VAR-WRITE-008.                                         SQ2014.2
055400 VAR-FAIL-008.                                                    SQ2014.2
055500     MOVE "<NOT AT END-OF-PAGE> NOT ENCOUNTERED; VII-53 GR (9)"   SQ2014.2
055600                TO RE-MARK.                                       SQ2014.2
055700     PERFORM FAIL.                                                SQ2014.2
055800 VAR-WRITE-008.                                                   SQ2014.2
055900     MOVE "NOT END-OF-PAGE" TO FEATURE.                           SQ2014.2
056000     MOVE "VAR-TEST-008" TO PAR-NAME.                             SQ2014.2
056100     PERFORM PRINT-DETAIL.                                        SQ2014.2
056200     MOVE SPACE TO DUMMY-RECORD.                                  SQ2014.2
056300     PERFORM BLANK-LINE-PRINT 5 TIMES.                            SQ2014.2
056400 WRT-TEST-009.                                                    SQ2014.2
056500*             THIS IS A TEST FOR WRITE ...; AT EOP ...            SQ2014.2
056600******************************************************************SQ2014.2
056700*                                                                *SQ2014.2
056800*             THIS IS A TEST FOR WRITE ... NOT AT EOP        ... *SQ2014.2
056900*                                          ---                   *SQ2014.2
057000******************************************************************SQ2014.2
057100     MOVE 0 TO WRITE-SWITCH.                                      SQ2014.2
057200     PERFORM WRITE-EOP-MESSAGE.                                   SQ2014.2
057300     MOVE SPACE TO DUMMY-RECORD.                                  SQ2014.2
057400     PERFORM BLANK-LINE-PRINT UNTIL LINAGE-COUNTER EQUAL 1.       SQ2014.2
057500     MOVE 1 TO DETAIL-LINE-NO.                                    SQ2014.2
057600     PERFORM EOP-TEST-2 132 TIMES.                                SQ2014.2
057700 VAR-TEST-009.                                                    SQ2014.2
057800     IF WRITE-SWITCH NOT = 1                                      SQ2014.2
057900         GO TO VAR-FAIL-009.                                      SQ2014.2
058000 VAR-PASS-009.                                                    SQ2014.2
058100     PERFORM PASS.                                                SQ2014.2
058200     GO TO VAR-WRITE-009.                                         SQ2014.2
058300 VAR-FAIL-009.                                                    SQ2014.2
058400     MOVE "<NOT AT EOP> NOT ENCOUNTERED; VII-53 GR (9)" TO RE-MARKSQ2014.2
058500     PERFORM FAIL.                                                SQ2014.2
058600 VAR-WRITE-009.                                                   SQ2014.2
058700     MOVE "NOT AT EOP"         TO FEATURE.                        SQ2014.2
058800     MOVE "VAR-TEST-009" TO PAR-NAME.                             SQ2014.2
058900     PERFORM PRINT-DETAIL.                                        SQ2014.2
059000     MOVE 0 TO WRITE-SWITCH.                                      SQ2014.2
059100     MOVE SPACE TO DUMMY-RECORD.                                  SQ2014.2
059200     PERFORM BLANK-LINE-PRINT 5 TIMES.                            SQ2014.2
059300 WRT-TEST-010.                                                    SQ2014.2
059400*             THIS IS A TEST FOR WRITE... ; END-OF-PAGE ...       SQ2014.2
059500******************************************************************SQ2014.2
059600*                                                                *SQ2014.2
059700*             THIS IS A TEST FOR WRITE ... NOT END-OF-PAGE   ... *SQ2014.2
059800*                                          ---                   *SQ2014.2
059900*         AND    END-WRITE                                       *SQ2014.2
060000******************************************************************SQ2014.2
060100     MOVE 0 TO WRITE-SWITCH.                                      SQ2014.2
060200     MOVE 0 TO END-WRITE-SWITCH.                                  SQ2014.2
060300     PERFORM WRITE-EOP-MESSAGE.                                   SQ2014.2
060400     MOVE SPACE TO DUMMY-RECORD.                                  SQ2014.2
060500     PERFORM BLANK-LINE-PRINT UNTIL LINAGE-COUNTER EQUAL 1.       SQ2014.2
060600     MOVE 1 TO DETAIL-LINE-NO.                                    SQ2014.2
060700     PERFORM EOP-TEST-3 132 TIMES.                                SQ2014.2
060800 VAR-TEST-010.                                                    SQ2014.2
060900     IF WRITE-SWITCH NOT = 1                                      SQ2014.2
061000         GO TO VAR-FAIL-010.                                      SQ2014.2
061100 VAR-PASS-010.                                                    SQ2014.2
061200     PERFORM PASS.                                                SQ2014.2
061300     GO TO VAR-WRITE-010.                                         SQ2014.2
061400 VAR-FAIL-010.                                                    SQ2014.2
061500     MOVE "<NOT END-OF-PAGE> NOT ENCOUNTERED; VII-53 GR (9)"      SQ2014.2
061600          TO RE-MARK.                                             SQ2014.2
061700     PERFORM FAIL.                                                SQ2014.2
061800 VAR-WRITE-010.                                                   SQ2014.2
061900     MOVE "NOT END-OF-PAGE" TO FEATURE.                           SQ2014.2
062000     MOVE "VAR-TEST-010" TO PAR-NAME.                             SQ2014.2
062100     PERFORM PRINT-DETAIL.                                        SQ2014.2
062200     MOVE 0 TO WRITE-SWITCH.                                      SQ2014.2
062300     MOVE SPACE TO DUMMY-RECORD.                                  SQ2014.2
062400     PERFORM BLANK-LINE-PRINT 5 TIMES.                            SQ2014.2
062500 VAR-TEST-010-1.                                                  SQ2014.2
062600     IF END-WRITE-SWITCH NOT EQUAL TO 1                           SQ2014.2
062700         GO TO VAR-FAIL-010-1.                                    SQ2014.2
062800 VAR-PASS-010-1.                                                  SQ2014.2
062900     PERFORM PASS.                                                SQ2014.2
063000     GO TO VAR-WRITE-010-1.                                       SQ2014.2
063100 VAR-FAIL-010-1.                                                  SQ2014.2
063200     MOVE "<END-WRITE> NOT CORRECT; IV-27 4.4.4" TO RE-MARK.      SQ2014.2
063300     PERFORM FAIL.                                                SQ2014.2
063400 VAR-WRITE-010-1.                                                 SQ2014.2
063500     MOVE "END-WRITE;NOT END-OF" TO FEATURE.                      SQ2014.2
063600     MOVE "VAR-TEST-010-1" TO PAR-NAME.                           SQ2014.2
063700     PERFORM PRINT-DETAIL.                                        SQ2014.2
063800     MOVE SPACE TO DUMMY-RECORD.                                  SQ2014.2
063900     PERFORM BLANK-LINE-PRINT 5 TIMES.                            SQ2014.2
064000 WRT-TEST-011.                                                    SQ2014.2
064100*             THIS IS A TEST FOR WRITE ...EOP...                  SQ2014.2
064200******************************************************************SQ2014.2
064300*                                                                *SQ2014.2
064400*             THIS IS A TEST FOR WRITE ... NOT EOP           ... *SQ2014.2
064500*                                          ---                   *SQ2014.2
064600******************************************************************SQ2014.2
064700     MOVE 0 TO WRITE-SWITCH.                                      SQ2014.2
064800     MOVE 0 TO END-WRITE-SWITCH.                                  SQ2014.2
064900*    MOVE "EOP" TO FEATURE.                                       SQ2014.2
065000*    MOVE "WRT-TEST-11" TO PAR-NAME.                              SQ2014.2
065100*    PERFORM PRINT-DETAIL.                                        SQ2014.2
065200     PERFORM WRITE-EOP-MESSAGE.                                   SQ2014.2
065300     MOVE SPACE TO DUMMY-RECORD.                                  SQ2014.2
065400     PERFORM BLANK-LINE-PRINT UNTIL LINAGE-COUNTER EQUAL 1.       SQ2014.2
065500     MOVE 1 TO DETAIL-LINE-NO.                                    SQ2014.2
065600     PERFORM EOP-TEST-4 132 TIMES.                                SQ2014.2
065700 VAR-TEST-011.                                                    SQ2014.2
065800     IF WRITE-SWITCH NOT = 1                                      SQ2014.2
065900         GO TO VAR-FAIL-011.                                      SQ2014.2
066000 VAR-PASS-011.                                                    SQ2014.2
066100     PERFORM PASS.                                                SQ2014.2
066200     GO TO VAR-WRITE-011.                                         SQ2014.2
066300 VAR-FAIL-011.                                                    SQ2014.2
066400     MOVE "<NOT EOP> NOT ENCOUNTERED; VII-53 GR (9)" TO RE-MARK.  SQ2014.2
066500     PERFORM FAIL.                                                SQ2014.2
066600 VAR-WRITE-011.                                                   SQ2014.2
066700     MOVE "NOT EOP" TO FEATURE.                                   SQ2014.2
066800     MOVE "VAR-TEST-011" TO PAR-NAME.                             SQ2014.2
066900     PERFORM PRINT-DETAIL.                                        SQ2014.2
067000     MOVE 0 TO WRITE-SWITCH.                                      SQ2014.2
067100     MOVE SPACE TO DUMMY-RECORD.                                  SQ2014.2
067200     PERFORM BLANK-LINE-PRINT 5 TIMES.                            SQ2014.2
067300 VAR-TEST-011-1.                                                  SQ2014.2
067400     IF END-WRITE-SWITCH NOT EQUAL TO 1                           SQ2014.2
067500         GO TO VAR-FAIL-011-1.                                    SQ2014.2
067600 VAR-PASS-011-1.                                                  SQ2014.2
067700     PERFORM PASS.                                                SQ2014.2
067800     GO TO VAR-WRITE-011-1.                                       SQ2014.2
067900 VAR-FAIL-011-1.                                                  SQ2014.2
068000     MOVE "<END-WRITE> NOT CORRECT; IV-27 4.4.4"   TO RE-MARK.    SQ2014.2
068100     PERFORM FAIL.                                                SQ2014.2
068200 VAR-WRITE-011-1.                                                 SQ2014.2
068300     MOVE "END-WRITE; NOT EOP" TO FEATURE.                        SQ2014.2
068400     MOVE "VAR-TEST-011-1" TO PAR-NAME.                           SQ2014.2
068500     PERFORM PRINT-DETAIL.                                        SQ2014.2
068600     MOVE 0 TO WRITE-SWITCH.                                      SQ2014.2
068700     MOVE 0 TO END-WRITE-SWITCH.                                  SQ2014.2
068800     MOVE SPACE TO DUMMY-RECORD.                                  SQ2014.2
068900     PERFORM BLANK-LINE-PRINT 5 TIMES.                            SQ2014.2
069000 SQ201M-END-ROUTINE.                                              SQ2014.2
069100     MOVE "END OF SQ201M VALIDATION TESTS" TO PRINT-REC.          SQ2014.2
069200     WRITE PRINT-REC AFTER ADVANCING 1 LINE.                      SQ2014.2
069300     GO TO CCVS-EXIT.                                             SQ2014.2
069400 EOP-TEST-1.                                                      SQ2014.2
069500     MOVE LINAGE-COUNTER TO DETAIL-LC.                            SQ2014.2
069600     MOVE DETAIL-LINE TO PRINT-REC.                               SQ2014.2
069700     WRITE PRINT-REC BEFORE ADVANCING 1 LINE AT END-OF-PAGE       SQ2014.2
069800              MOVE 1 TO FOOT-COUNT                                SQ2014.2
069900              MOVE 45 TO FOOT-LINE-NO                             SQ2014.2
070000              PERFORM PRINT-FOOTING 6 TIMES                       SQ2014.2
070100******************************************************************SQ2014.2
070200*                                                                *SQ2014.2
070300*         NEW: NOT AT END-OF-PAGE                                *SQ2014.2
070400*              ---                                               *SQ2014.2
070500******************************************************************SQ2014.2
070600        NOT AT END-OF-PAGE                                        SQ2014.2
070700             MOVE 1 TO WRITE-SWITCH.                              SQ2014.2
070800     ADD 1 TO DETAIL-LINE-NO.                                     SQ2014.2
070900 EOP-TEST-2.                                                      SQ2014.2
071000     MOVE LINAGE-COUNTER TO DETAIL-LC.                            SQ2014.2
071100     MOVE DETAIL-LINE TO PRINT-REC.                               SQ2014.2
071200     WRITE PRINT-REC BEFORE ADVANCING 1 LINE ; AT EOP             SQ2014.2
071300              MOVE 1 TO FOOT-COUNT                                SQ2014.2
071400              MOVE 45 TO FOOT-LINE-NO                             SQ2014.2
071500              PERFORM PRINT-FOOTING 6 TIMES                       SQ2014.2
071600******************************************************************SQ2014.2
071700*                                                                *SQ2014.2
071800*         NEW: NOT AT EOP                                        *SQ2014.2
071900*                                                                *SQ2014.2
072000******************************************************************SQ2014.2
072100        NOT AT EOP                                                SQ2014.2
072200             MOVE 1 TO WRITE-SWITCH.                              SQ2014.2
072300     ADD 1 TO DETAIL-LINE-NO.                                     SQ2014.2
072400 EOP-TEST-3.                                                      SQ2014.2
072500     MOVE LINAGE-COUNTER TO DETAIL-LC.                            SQ2014.2
072600     MOVE DETAIL-LINE TO PRINT-REC.                               SQ2014.2
072700     IF END-WRITE-SWITCH EQUAL TO 1 OR END-WRITE-SWITCH EQUAL TO 0SQ2014.2
072800         WRITE PRINT-REC BEFORE ADVANCING 1 LINE ; END-OF-PAGE    SQ2014.2
072900              MOVE 1 TO FOOT-COUNT                                SQ2014.2
073000              MOVE 45 TO FOOT-LINE-NO                             SQ2014.2
073100              PERFORM PRINT-FOOTING 6 TIMES                       SQ2014.2
073200******************************************************************SQ2014.2
073300*                                                                *SQ2014.2
073400*         NEW: NOT END-OF-PAGE                                   *SQ2014.2
073500*              ---                                               *SQ2014.2
073600******************************************************************SQ2014.2
073700           NOT END-OF-PAGE                                        SQ2014.2
073800             MOVE 1 TO WRITE-SWITCH                               SQ2014.2
073900          END-WRITE                                               SQ2014.2
074000        MOVE 1 TO END-WRITE-SWITCH.                               SQ2014.2
074100     ADD 1 TO DETAIL-LINE-NO.                                     SQ2014.2
074200 EOP-TEST-4.                                                      SQ2014.2
074300     MOVE LINAGE-COUNTER TO DETAIL-LC.                            SQ2014.2
074400     MOVE DETAIL-LINE TO PRINT-REC.                               SQ2014.2
074500     IF END-WRITE-SWITCH EQUAL TO 1 OR END-WRITE-SWITCH EQUAL TO 0SQ2014.2
074600         WRITE PRINT-REC BEFORE ADVANCING 1 LINE EOP              SQ2014.2
074700              MOVE 1 TO FOOT-COUNT                                SQ2014.2
074800              MOVE 45 TO FOOT-LINE-NO                             SQ2014.2
074900              PERFORM PRINT-FOOTING 6 TIMES                       SQ2014.2
075000******************************************************************SQ2014.2
075100*                                                                *SQ2014.2
075200*         NEW: NOT EOP                                           *SQ2014.2
075300*              ---                                               *SQ2014.2
075400******************************************************************SQ2014.2
075500            NOT EOP                                               SQ2014.2
075600             MOVE 1 TO WRITE-SWITCH                               SQ2014.2
075700          END-WRITE                                               SQ2014.2
075800        MOVE 1 TO END-WRITE-SWITCH.                               SQ2014.2
075900     ADD 1 TO DETAIL-LINE-NO.                                     SQ2014.2
076000 PRINT-FOOTING.                                                   SQ2014.2
076100     MOVE LINAGE-COUNTER TO FOOT-LC.                              SQ2014.2
076200     MOVE FOOT-LINE TO PRINT-REC.                                 SQ2014.2
076300     WRITE PRINT-REC BEFORE ADVANCING 1 LINE.                     SQ2014.2
076400     ADD 1 TO FOOT-COUNT.                                         SQ2014.2
076500     ADD 1 TO FOOT-LINE-NO.                                       SQ2014.2
076600 WRITE-EOP-MESSAGE.                                               SQ2014.2
076700     PERFORM BLANK-LINE-PRINT 5 TIMES.                            SQ2014.2
076800     MOVE EOP-MESSAGE-1 TO PRINT-REC.                             SQ2014.2
076900     WRITE PRINT-REC BEFORE ADVANCING 1 LINE.                     SQ2014.2
077000     MOVE EOP-MESSAGE-2 TO PRINT-REC.                             SQ2014.2
077100     WRITE PRINT-REC BEFORE ADVANCING 1 LINE.                     SQ2014.2
077200     MOVE EOP-MESSAGE-3 TO PRINT-REC.                             SQ2014.2
077300     WRITE PRINT-REC BEFORE ADVANCING 1 LINE.                     SQ2014.2
077400     MOVE SPACE TO DUMMY-RECORD.                                  SQ2014.2
077500     PERFORM BLANK-LINE-PRINT 5 TIMES.                            SQ2014.2
077600 CCVS-EXIT SECTION.                                               SQ2014.2
077700 CCVS-999999.                                                     SQ2014.2
077800     GO TO CLOSE-FILES.                                           SQ2014.2
      *END-OF,SQ201M                                                                  
