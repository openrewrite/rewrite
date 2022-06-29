      *HEADER,COBOL,SQ210M                                                            
000100 IDENTIFICATION DIVISION.                                         SQ2104.2
000200 PROGRAM-ID.                                                      SQ2104.2
000300     SQ210M.                                                      SQ2104.2
000400****************************************************************  SQ2104.2
000500*                                                              *  SQ2104.2
000600*    VALIDATION FOR:-                                          *  SQ2104.2
000700*    " HIGH       ".                                              SQ2104.2
000800*                                                              *  SQ2104.2
000900*    CREATION DATE     /     VALIDATION DATE                   *  SQ2104.2
001000*    "4.2 ".                                                      SQ2104.2
001100*                                                              *  SQ2104.2
001200*        THE ROUTINE SQ210M TESTS THE USE OF THE LEVEL 2 WRITE    SQ2104.2
001300*    STATEMENT AND THE LINAGE CLAUSE FOR A FILE DESIGNATED AS     SQ2104.2
001400*    PRINTER OUTPUT.  THESE STATEMENTS CONTROL THE VERTICAL       SQ2104.2
001500*    POSITIONING OF EACH LINE ON A PRINTED PAGE.  THE LINAGE      SQ2104.2
001600*    CLAUSE SPECIFICALLY CONTROLS THE VERTICAL FORMAT OF LOGICAL  SQ2104.2
001700*    PRINT PAGE.  SQ210M TESTS THE USE OF A MIXTURE OF INTEGER ANDSQ2104.2
001800*    DATA-NAME ITEMS IN THE LINAGE CLAUSE.  VALUES OF DATA-NAMES  SQ2104.2
001900*    ARE CHANGED IN ORDER TO CHECK REDEFINITION OF LOGICAL PAGE   SQ2104.2
002000*    FORMATS.  IT IS ASSUMED THAT ALL LEVEL 2 NUCLEUS OPTIONS     SQ2104.2
002100*    ARE AVAILABLE IN TESTING SQ210M.                             SQ2104.2
002200 ENVIRONMENT DIVISION.                                            SQ2104.2
002300 CONFIGURATION SECTION.                                           SQ2104.2
002400 SOURCE-COMPUTER.                                                 SQ2104.2
002500     XXXXX082.                                                    SQ2104.2
002600 OBJECT-COMPUTER.                                                 SQ2104.2
002700     XXXXX083.                                                    SQ2104.2
002800 INPUT-OUTPUT SECTION.                                            SQ2104.2
002900 FILE-CONTROL.                                                    SQ2104.2
003000*    SELECT RAW-DATA   ASSIGN TO                                  SQ2104.2
003100*    XXXXX062                                                     SQ2104.2
003200*           ORGANIZATION IS INDEXED                               SQ2104.2
003300*           ACCESS MODE IS RANDOM                                 SQ2104.2
003400*           RECORD KEY IS RAW-DATA-KEY.                           SQ2104.2
003500     SELECT PRINT-FILE ASSIGN TO                                  SQ2104.2
003600     XXXXX055.                                                    SQ2104.2
003700 DATA DIVISION.                                                   SQ2104.2
003800 FILE SECTION.                                                    SQ2104.2
003900*                                                                 SQ2104.2
004000*FD  RAW-DATA.                                                    SQ2104.2
004100*                                                                 SQ2104.2
004200*01  RAW-DATA-SATZ.                                               SQ2104.2
004300*    05  RAW-DATA-KEY        PIC X(6).                            SQ2104.2
004400*    05  C-DATE              PIC 9(6).                            SQ2104.2
004500*    05  C-TIME              PIC 9(8).                            SQ2104.2
004600*    05  C-NO-OF-TESTS       PIC 99.                              SQ2104.2
004700*    05  C-OK                PIC 999.                             SQ2104.2
004800*    05  C-ALL               PIC 999.                             SQ2104.2
004900*    05  C-FAIL              PIC 999.                             SQ2104.2
005000*    05  C-DELETED           PIC 999.                             SQ2104.2
005100*    05  C-INSPECT           PIC 999.                             SQ2104.2
005200*    05  C-NOTE              PIC X(13).                           SQ2104.2
005300*    05  C-INDENT            PIC X.                               SQ2104.2
005400*    05  C-ABORT             PIC X(8).                            SQ2104.2
005500 FD  PRINT-FILE                                                   SQ2104.2
005600C    LABEL RECORDS                                                SQ2104.2
005700C    XXXXX084                                                     SQ2104.2
005800C    DATA RECORD IS PRINT-REC DUMMY-RECORD                        SQ2104.2
005900     LINAGE IS LINAGE-CTR LINES                                   SQ2104.2
006000              TOP 5.                                              SQ2104.2
006100 01  PRINT-REC PICTURE X(120).                                    SQ2104.2
006200 01  DUMMY-RECORD PICTURE X(120).                                 SQ2104.2
006300 WORKING-STORAGE SECTION.                                         SQ2104.2
006400 77  LINAGE-CTR         PIC 99       VALUE 66.                    SQ2104.2
006500 01  DETAIL-LINE-1.                                               SQ2104.2
006600     02 FILLER          PIC X(20)    VALUE SPACE.                 SQ2104.2
006700     02 FILLER          PIC X(13)    VALUE "THIS IS LINE ".       SQ2104.2
006800     02 DL1-LINE-NO     PIC 99.                                   SQ2104.2
006900     02 FILLER          PIC X(40)    VALUE " OF 80 DETAIL LINES.  SQ2104.2
007000-    "LINAGE-COUNTER IS ".                                        SQ2104.2
007100     02 DL1-LC          PIC 99.                                   SQ2104.2
007200     02 FILLER          PIC X(43)    VALUE ".".                   SQ2104.2
007300 01  COMMENT-LINE-1     PIC X(120)   VALUE "BECAUSE OF THE NATURE SQ2104.2
007400-    "OF THESE TESTS A PASS OR FAIL CANNOT BE DETERMINED WITHIN THSQ2104.2
007500-    "E PROGRAM.  THE USER MUST VISUALLY".                        SQ2104.2
007600 01  COMMENT-LINE-2     PIC X(120)   VALUE "CHECK THE POSITION OF SQ2104.2
007700-    "EACH LINE TO DETERMINE THE ACCURACY OF THE VARIOUS WRITE OPTSQ2104.2
007800-    "IONS. VII-27 TO 29".                                        SQ2104.2
007900 01  COMMENT-LINE-3     PIC X(120)   VALUE "IN THIS TEST THE FIRSTSQ2104.2
008000-    " LOGICAL PAGE SHOULD CONTAIN 20 DETAIL LINES.  ALL SUCCEEDINSQ2104.2
008100-    "G LOGICAL PAGES SHOULD CONTAIN 30".                         SQ2104.2
008200 01  COMMENT-LINE-4     PIC X(120)   VALUE "DETAIL LINES.  ALL LOGSQ2104.2
008300-    "ICAL PAGES SHOULD BE SEPARATED BY 5 BLANK LINES.".          SQ2104.2
008400 01  LAST-LINE          PIC X(120)   VALUE "THIS IS THE LAST LINE SQ2104.2
008500-    "IN THE PAGE BODY OF THIS LOGICAL PAGE.  USE IT AS A REFERENCSQ2104.2
008600-    "E POINT FOR THE FOLLOWING TEST PAGES.".                     SQ2104.2
008700 01  TEST-RESULTS.                                                SQ2104.2
008800     02 FILLER                    PICTURE X VALUE SPACE.          SQ2104.2
008900     02 FEATURE                   PICTURE X(20) VALUE SPACE.      SQ2104.2
009000     02 FILLER                    PICTURE X VALUE SPACE.          SQ2104.2
009100     02 P-OR-F                    PICTURE X(5) VALUE SPACE.       SQ2104.2
009200     02 FILLER                    PICTURE X  VALUE SPACE.         SQ2104.2
009300     02  PAR-NAME.                                                SQ2104.2
009400       03 FILLER PICTURE X(12) VALUE SPACE.                       SQ2104.2
009500       03  PARDOT-X PICTURE X  VALUE SPACE.                       SQ2104.2
009600       03 DOTVALUE PICTURE 99  VALUE ZERO.                        SQ2104.2
009700       03 FILLER PIC X(5) VALUE SPACE.                            SQ2104.2
009800     02 FILLER PIC X(10) VALUE SPACE.                             SQ2104.2
009900     02 RE-MARK PIC X(61).                                        SQ2104.2
010000 01  TEST-COMPUTED.                                               SQ2104.2
010100     02 FILLER PIC X(30) VALUE SPACE.                             SQ2104.2
010200     02 FILLER PIC X(17) VALUE "       COMPUTED=".                SQ2104.2
010300     02 COMPUTED-X.                                               SQ2104.2
010400     03 COMPUTED-A                PICTURE X(20) VALUE SPACE.      SQ2104.2
010500     03 COMPUTED-N REDEFINES COMPUTED-A PICTURE -9(9).9(9).       SQ2104.2
010600     03 COMPUTED-0V18 REDEFINES COMPUTED-A  PICTURE -.9(18).      SQ2104.2
010700     03 COMPUTED-4V14 REDEFINES COMPUTED-A  PICTURE -9(4).9(14).  SQ2104.2
010800     03 COMPUTED-14V4 REDEFINES COMPUTED-A  PICTURE -9(14).9(4).  SQ2104.2
010900     03       CM-18V0 REDEFINES COMPUTED-A.                       SQ2104.2
011000         04 COMPUTED-18V0                   PICTURE -9(18).       SQ2104.2
011100         04 FILLER                          PICTURE X.            SQ2104.2
011200     03 FILLER PIC X(50) VALUE SPACE.                             SQ2104.2
011300 01  TEST-CORRECT.                                                SQ2104.2
011400     02 FILLER PIC X(30) VALUE SPACE.                             SQ2104.2
011500     02 FILLER PIC X(17) VALUE "       CORRECT =".                SQ2104.2
011600     02 CORRECT-X.                                                SQ2104.2
011700     03 CORRECT-A                 PICTURE X(20) VALUE SPACE.      SQ2104.2
011800     03 CORRECT-N REDEFINES CORRECT-A PICTURE -9(9).9(9).         SQ2104.2
011900     03 CORRECT-0V18 REDEFINES CORRECT-A    PICTURE -.9(18).      SQ2104.2
012000     03 CORRECT-4V14 REDEFINES CORRECT-A    PICTURE -9(4).9(14).  SQ2104.2
012100     03 CORRECT-14V4 REDEFINES CORRECT-A    PICTURE -9(14).9(4).  SQ2104.2
012200     03      CR-18V0 REDEFINES CORRECT-A.                         SQ2104.2
012300         04 CORRECT-18V0                    PICTURE -9(18).       SQ2104.2
012400         04 FILLER                          PICTURE X.            SQ2104.2
012500     03 FILLER PIC X(50) VALUE SPACE.                             SQ2104.2
012600 01  CCVS-C-1.                                                    SQ2104.2
012700     02 FILLER PICTURE IS X(99) VALUE IS " FEATURE              PASQ2104.2
012800-    "SS  PARAGRAPH-NAME                                          SQ2104.2
012900-    "        REMARKS".                                           SQ2104.2
013000     02 FILLER PICTURE IS X(20) VALUE IS SPACE.                   SQ2104.2
013100 01  CCVS-C-2.                                                    SQ2104.2
013200     02 FILLER PICTURE IS X VALUE IS SPACE.                       SQ2104.2
013300     02 FILLER PICTURE IS X(6) VALUE IS "TESTED".                 SQ2104.2
013400     02 FILLER PICTURE IS X(15) VALUE IS SPACE.                   SQ2104.2
013500     02 FILLER PICTURE IS X(4) VALUE IS "FAIL".                   SQ2104.2
013600     02 FILLER PICTURE IS X(94) VALUE IS SPACE.                   SQ2104.2
013700 01  REC-SKL-SUB PICTURE 9(2) VALUE ZERO.                         SQ2104.2
013800 01  REC-CT PICTURE 99 VALUE ZERO.                                SQ2104.2
013900 01  DELETE-CNT                   PICTURE 999  VALUE ZERO.        SQ2104.2
014000 01  ERROR-COUNTER PICTURE IS 999 VALUE IS ZERO.                  SQ2104.2
014100 01  INSPECT-COUNTER PIC 999 VALUE 3.                             SQ2104.2
014200 01  PASS-COUNTER PIC 999 VALUE ZERO.                             SQ2104.2
014300 01  TOTAL-ERROR PIC 999 VALUE ZERO.                              SQ2104.2
014400 01  ERROR-HOLD PIC 999 VALUE ZERO.                               SQ2104.2
014500 01  DUMMY-HOLD PIC X(120) VALUE SPACE.                           SQ2104.2
014600 01  RECORD-COUNT PIC 9(5) VALUE ZERO.                            SQ2104.2
014700 01  CCVS-H-1.                                                    SQ2104.2
014800     02  FILLER   PICTURE X(27)  VALUE SPACE.                     SQ2104.2
014900     02 FILLER PICTURE X(67) VALUE                                SQ2104.2
015000     " FEDERAL SOFTWARE TESTING CENTER COBOL COMPILER VALIDATION  SQ2104.2
015100-    " SYSTEM".                                                   SQ2104.2
015200     02  FILLER     PICTURE X(26)  VALUE SPACE.                   SQ2104.2
015300 01  CCVS-H-2.                                                    SQ2104.2
015400     02 FILLER PICTURE X(52) VALUE IS                             SQ2104.2
015500     "CCVS85 FSTC COPY, NOT FOR DISTRIBUTION.".                   SQ2104.2
015600     02 FILLER PICTURE IS X(19) VALUE IS "TEST RESULTS SET-  ".   SQ2104.2
015700     02 TEST-ID PICTURE IS X(9).                                  SQ2104.2
015800     02 FILLER PICTURE IS X(40) VALUE IS SPACE.                   SQ2104.2
015900 01  CCVS-H-3.                                                    SQ2104.2
016000     02  FILLER PICTURE X(34) VALUE                               SQ2104.2
016100     " FOR OFFICIAL USE ONLY    ".                                SQ2104.2
016200     02  FILLER PICTURE X(58) VALUE                               SQ2104.2
016300     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ2104.2
016400     02  FILLER PICTURE X(28) VALUE                               SQ2104.2
016500     "  COPYRIGHT   1985 ".                                       SQ2104.2
016600 01  CCVS-E-1.                                                    SQ2104.2
016700     02 FILLER PICTURE IS X(52) VALUE IS SPACE.                   SQ2104.2
016800     02 FILLER PICTURE IS X(14) VALUE IS "END OF TEST-  ".        SQ2104.2
016900     02 ID-AGAIN PICTURE IS X(9).                                 SQ2104.2
017000     02 FILLER PICTURE X(45) VALUE IS                             SQ2104.2
017100     " NTIS DISTRIBUTION COBOL 85".                               SQ2104.2
017200 01  CCVS-E-2.                                                    SQ2104.2
017300     02  FILLER                   PICTURE X(31)  VALUE            SQ2104.2
017400     SPACE.                                                       SQ2104.2
017500     02  FILLER                   PICTURE X(21)  VALUE SPACE.     SQ2104.2
017600     02 CCVS-E-2-2.                                               SQ2104.2
017700         03 ERROR-TOTAL PICTURE IS XXX VALUE IS SPACE.            SQ2104.2
017800         03 FILLER PICTURE IS X VALUE IS SPACE.                   SQ2104.2
017900         03 ENDER-DESC PIC X(46) VALUE "ERRORS ENCOUNTERED".      SQ2104.2
018000 01  CCVS-E-3.                                                    SQ2104.2
018100     02  FILLER PICTURE X(22) VALUE                               SQ2104.2
018200     " FOR OFFICIAL USE ONLY".                                    SQ2104.2
018300     02  FILLER PICTURE X(12) VALUE SPACE.                        SQ2104.2
018400     02  FILLER PICTURE X(58) VALUE                               SQ2104.2
018500     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ2104.2
018600     02  FILLER PICTURE X(13) VALUE SPACE.                        SQ2104.2
018700     02 FILLER PIC X(15) VALUE " COPYRIGHT 1985".                 SQ2104.2
018800 01  CCVS-E-4.                                                    SQ2104.2
018900     02 CCVS-E-4-1 PIC XXX VALUE SPACE.                           SQ2104.2
019000     02 FILLER PIC XXXX VALUE " OF ".                             SQ2104.2
019100     02 CCVS-E-4-2 PIC XXX VALUE SPACE.                           SQ2104.2
019200     02 FILLER PIC X(40) VALUE                                    SQ2104.2
019300      "  TESTS WERE EXECUTED SUCCESSFULLY".                       SQ2104.2
019400 01  XXINFO.                                                      SQ2104.2
019500     02 FILLER PIC X(30) VALUE "        *** INFORMATION  ***".    SQ2104.2
019600     02 INFO-TEXT.                                                SQ2104.2
019700     04 FILLER PIC X(20) VALUE SPACE.                             SQ2104.2
019800     04 XXCOMPUTED PIC X(20).                                     SQ2104.2
019900     04 FILLER PIC X(5) VALUE SPACE.                              SQ2104.2
020000     04 XXCORRECT PIC X(20).                                      SQ2104.2
020100 01  HYPHEN-LINE.                                                 SQ2104.2
020200     02 FILLER PICTURE IS X VALUE IS SPACE.                       SQ2104.2
020300     02 FILLER PICTURE IS X(65) VALUE IS "************************SQ2104.2
020400-    "*****************************************".                 SQ2104.2
020500     02 FILLER PICTURE IS X(54) VALUE IS "************************SQ2104.2
020600-    "******************************".                            SQ2104.2
020700 01  CCVS-PGM-ID PIC X(6) VALUE                                   SQ2104.2
020800     "SQ210M".                                                    SQ2104.2
020900 PROCEDURE DIVISION.                                              SQ2104.2
021000 CCVS1 SECTION.                                                   SQ2104.2
021100 OPEN-FILES.                                                      SQ2104.2
021200*    OPEN I-O RAW-DATA.                                           SQ2104.2
021300*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            SQ2104.2
021400*    READ RAW-DATA INVALID KEY GO TO END-E-1.                     SQ2104.2
021500*    MOVE "ABORTED " TO C-ABORT.                                  SQ2104.2
021600*    ADD 1 TO C-NO-OF-TESTS.                                      SQ2104.2
021700*    ACCEPT C-DATE  FROM DATE.                                    SQ2104.2
021800*    ACCEPT C-TIME  FROM TIME.                                    SQ2104.2
021900*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-1.             SQ2104.2
022000*END-E-1.                                                         SQ2104.2
022100*    CLOSE RAW-DATA.                                              SQ2104.2
022200     OPEN     OUTPUT PRINT-FILE.                                  SQ2104.2
022300     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   SQ2104.2
022400     MOVE    SPACE TO TEST-RESULTS.                               SQ2104.2
022500     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             SQ2104.2
022600     GO TO CCVS1-EXIT.                                            SQ2104.2
022700 CLOSE-FILES.                                                     SQ2104.2
022800     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   SQ2104.2
022900*    OPEN I-O RAW-DATA.                                           SQ2104.2
023000*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            SQ2104.2
023100*    READ RAW-DATA INVALID KEY GO TO END-E-2.                     SQ2104.2
023200*    MOVE "OK.     " TO C-ABORT.                                  SQ2104.2
023300*    MOVE PASS-COUNTER TO C-OK.                                   SQ2104.2
023400*    MOVE ERROR-HOLD   TO C-ALL.                                  SQ2104.2
023500*    MOVE ERROR-COUNTER TO C-FAIL.                                SQ2104.2
023600*    MOVE DELETE-CNT TO C-DELETED.                                SQ2104.2
023700*    MOVE INSPECT-COUNTER TO C-INSPECT.                           SQ2104.2
023800*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-2.             SQ2104.2
023900*END-E-2.                                                         SQ2104.2
024000*    CLOSE RAW-DATA.                                              SQ2104.2
024100 TERMINATE-CCVS.                                                  SQ2104.2
024200*    EXIT PROGRAM.                                                SQ2104.2
024300*TERMINATE-CALL.                                                  SQ2104.2
024400     STOP     RUN.                                                SQ2104.2
024500 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         SQ2104.2
024600 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           SQ2104.2
024700 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          SQ2104.2
024800 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-CNT.          SQ2104.2
024900     MOVE "****TEST DELETED****" TO RE-MARK.                      SQ2104.2
025000 PRINT-DETAIL.                                                    SQ2104.2
025100     IF REC-CT NOT EQUAL TO ZERO                                  SQ2104.2
025200             MOVE "." TO PARDOT-X                                 SQ2104.2
025300             MOVE REC-CT TO DOTVALUE.                             SQ2104.2
025400     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      SQ2104.2
025500     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               SQ2104.2
025600        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 SQ2104.2
025700          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 SQ2104.2
025800     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              SQ2104.2
025900     MOVE SPACE TO CORRECT-X.                                     SQ2104.2
026000     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         SQ2104.2
026100     MOVE     SPACE TO RE-MARK.                                   SQ2104.2
026200 HEAD-ROUTINE.                                                    SQ2104.2
026300     MOVE CCVS-H-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ2104.2
026400     MOVE CCVS-H-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.   SQ2104.2
026500     MOVE CCVS-H-3 TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.   SQ2104.2
026600 COLUMN-NAMES-ROUTINE.                                            SQ2104.2
026700     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ2104.2
026800     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ2104.2
026900     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ2104.2
027000 END-ROUTINE.                                                     SQ2104.2
027100     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.SQ2104.2
027200 END-RTN-EXIT.                                                    SQ2104.2
027300     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ2104.2
027400 END-ROUTINE-1.                                                   SQ2104.2
027500      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      SQ2104.2
027600      ERROR-HOLD. ADD DELETE-CNT TO ERROR-HOLD.                   SQ2104.2
027700      ADD PASS-COUNTER TO ERROR-HOLD.                             SQ2104.2
027800*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   SQ2104.2
027900      MOVE PASS-COUNTER TO CCVS-E-4-1.                            SQ2104.2
028000      MOVE ERROR-HOLD TO CCVS-E-4-2.                              SQ2104.2
028100      MOVE CCVS-E-4 TO CCVS-E-2-2.                                SQ2104.2
028200      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           SQ2104.2
028300  END-ROUTINE-12.                                                 SQ2104.2
028400      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        SQ2104.2
028500     IF       ERROR-COUNTER IS EQUAL TO ZERO                      SQ2104.2
028600         MOVE "NO " TO ERROR-TOTAL                                SQ2104.2
028700         ELSE                                                     SQ2104.2
028800         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       SQ2104.2
028900     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           SQ2104.2
029000     PERFORM WRITE-LINE.                                          SQ2104.2
029100 END-ROUTINE-13.                                                  SQ2104.2
029200     IF DELETE-CNT IS EQUAL TO ZERO                               SQ2104.2
029300         MOVE "NO " TO ERROR-TOTAL  ELSE                          SQ2104.2
029400         MOVE DELETE-CNT TO ERROR-TOTAL.                          SQ2104.2
029500     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   SQ2104.2
029600     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ2104.2
029700      IF   INSPECT-COUNTER EQUAL TO ZERO                          SQ2104.2
029800          MOVE "NO " TO ERROR-TOTAL                               SQ2104.2
029900      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   SQ2104.2
030000      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            SQ2104.2
030100      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          SQ2104.2
030200     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ2104.2
030300 WRITE-LINE.                                                      SQ2104.2
030400     ADD 1 TO RECORD-COUNT.                                       SQ2104.2
030500*    IF RECORD-COUNT GREATER 50                                   SQ2104.2
030600*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          SQ2104.2
030700*        MOVE SPACE TO DUMMY-RECORD                               SQ2104.2
030800*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  SQ2104.2
030900*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             SQ2104.2
031000*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     SQ2104.2
031100*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          SQ2104.2
031200*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          SQ2104.2
031300*        MOVE ZERO TO RECORD-COUNT.                               SQ2104.2
031400     PERFORM WRT-LN.                                              SQ2104.2
031500 WRT-LN.                                                          SQ2104.2
031600     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               SQ2104.2
031700     MOVE SPACE TO DUMMY-RECORD.                                  SQ2104.2
031800 BLANK-LINE-PRINT.                                                SQ2104.2
031900     PERFORM WRT-LN.                                              SQ2104.2
032000 FAIL-ROUTINE.                                                    SQ2104.2
032100     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.   SQ2104.2
032200     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.    SQ2104.2
032300     MOVE "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.    SQ2104.2
032400     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     SQ2104.2
032500     GO TO FAIL-ROUTINE-EX.                                       SQ2104.2
032600 FAIL-ROUTINE-WRITE.                                              SQ2104.2
032700     MOVE TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE           SQ2104.2
032800     MOVE TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES.   SQ2104.2
032900 FAIL-ROUTINE-EX. EXIT.                                           SQ2104.2
033000 BAIL-OUT.                                                        SQ2104.2
033100     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.       SQ2104.2
033200     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.               SQ2104.2
033300 BAIL-OUT-WRITE.                                                  SQ2104.2
033400     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  SQ2104.2
033500     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     SQ2104.2
033600 BAIL-OUT-EX. EXIT.                                               SQ2104.2
033700 CCVS1-EXIT.                                                      SQ2104.2
033800     EXIT.                                                        SQ2104.2
033900 SECT-SQ210M-0001 SECTION.                                        SQ2104.2
034000 WRITE-INIT-GF-01.                                                SQ2104.2
034100     MOVE COMMENT-LINE-1 TO PRINT-REC.                            SQ2104.2
034200     WRITE PRINT-REC AFTER ADVANCING 5 LINES.                     SQ2104.2
034300     MOVE COMMENT-LINE-2 TO PRINT-REC.                            SQ2104.2
034400     WRITE PRINT-REC AFTER ADVANCING 1 LINE.                      SQ2104.2
034500 WRITE-TEST-GF-01.                                                SQ2104.2
034600*             THIS TEST CHECKS A LINAGE CLAUSE WHICH CONTAINS     SQ2104.2
034700*             PHRASES WITH BOTH INTEGER AND DATA NAME ITEMS.      SQ2104.2
034800     MOVE "LINAGE INT / D-N MIX" TO FEATURE.                      SQ2104.2
034900     MOVE "WRITE-TEST-GF-01" TO PAR-NAME.                         SQ2104.2
035000     PERFORM PRINT-DETAIL.                                        SQ2104.2
035100     MOVE COMMENT-LINE-3 TO PRINT-REC.                            SQ2104.2
035200     WRITE PRINT-REC AFTER ADVANCING 5 LINES.                     SQ2104.2
035300     MOVE COMMENT-LINE-4 TO PRINT-REC.                            SQ2104.2
035400     WRITE PRINT-REC AFTER ADVANCING 1 LINE.                      SQ2104.2
035500     MOVE SPACE TO DUMMY-RECORD.                                  SQ2104.2
035600     PERFORM BLANK-LINE-PRINT UNTIL LINAGE-COUNTER EQUAL 66.      SQ2104.2
035700     MOVE 20 TO LINAGE-CTR.                                       SQ2104.2
035800     MOVE LAST-LINE TO PRINT-REC.                                 SQ2104.2
035900     WRITE PRINT-REC BEFORE ADVANCING 1 LINE.                     SQ2104.2
036000     MOVE 30 TO LINAGE-CTR.                                       SQ2104.2
036100     MOVE 1 TO DL1-LINE-NO.                                       SQ2104.2
036200     PERFORM PRINT-DETAIL-1 80 TIMES.                             SQ2104.2
036300 SQ210M-END-ROUTINE.                                              SQ2104.2
036400     MOVE "END OF SQ210M VALIDATION TESTS" TO PRINT-REC.          SQ2104.2
036500     WRITE PRINT-REC AFTER ADVANCING 1 LINE.                      SQ2104.2
036600     GO TO CCVS-EXIT.                                             SQ2104.2
036700 PRINT-DETAIL-1.                                                  SQ2104.2
036800     MOVE LINAGE-COUNTER TO DL1-LC.                               SQ2104.2
036900     MOVE DETAIL-LINE-1 TO PRINT-REC.                             SQ2104.2
037000     WRITE PRINT-REC BEFORE ADVANCING 1 LINE.                     SQ2104.2
037100     ADD 1 TO DL1-LINE-NO.                                        SQ2104.2
037200 CCVS-EXIT SECTION.                                               SQ2104.2
037300 CCVS-999999.                                                     SQ2104.2
037400     GO TO CLOSE-FILES.                                           SQ2104.2
      *END-OF,SQ210M                                                                  
