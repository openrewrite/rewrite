      *HEADER,COBOL,SQ115A                                                            
000100 IDENTIFICATION DIVISION.                                         SQ1154.2
000200 PROGRAM-ID.                                                      SQ1154.2
000300     SQ115A.                                                      SQ1154.2
000400****************************************************************  SQ1154.2
000500*                                                              *  SQ1154.2
000600*    VALIDATION FOR:-                                          *  SQ1154.2
000700*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1154.2
000800*                                                              *  SQ1154.2
000900*    CREATION DATE     /     VALIDATION DATE                   *  SQ1154.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ1154.2
001100*                                                              *  SQ1154.2
001200****************************************************************  SQ1154.2
001300                                                                  SQ1154.2
001400*        THIS ROUTINE CREATES A MASS STORAGE FILE CONTAINING      SQ1154.2
001500*    550 RECORDS.  EACH RECORD CONTAINS 126 CHARACTERS. THE       SQ1154.2
001600*    FILE IS CLOSED AND OPENED AS AN INPUT-OUTPUT FILE. EVERY     SQ1154.2
001700*    TENTH RECORD IS REWRITTEN.  THE FILE IS CLOSED AND OPENED    SQ1154.2
001800*    AGAIN AS AN INPUT FILE.  FIELDS IN EACH RECORD ARE CHECKED   SQ1154.2
001900*    TO ENSURE THAT THE RECORDS REWRITTEN ARE CORRECT AND THAT    SQ1154.2
002000*    THE RECORDS WHICH WERE NOT UPDATED WERE NOT CHANGED.         SQ1154.2
002100*                                                                 SQ1154.2
002200*    USED X-CARDS:                                                SQ1154.2
002300*         XXXXX014                                                SQ1154.2
002400*         XXXXX055                                                SQ1154.2
002500*     P   XXXXX062                                                SQ1154.2
002600*         XXXXX082                                                SQ1154.2
002700*         XXXXX083                                                SQ1154.2
002800*     C   XXXXX084                                                SQ1154.2
002900*                                                                 SQ1154.2
003000*                                                                 SQ1154.2
003100 ENVIRONMENT DIVISION.                                            SQ1154.2
003200 CONFIGURATION SECTION.                                           SQ1154.2
003300 SOURCE-COMPUTER.                                                 SQ1154.2
003400     XXXXX082.                                                    SQ1154.2
003500 OBJECT-COMPUTER.                                                 SQ1154.2
003600     XXXXX083.                                                    SQ1154.2
003700 INPUT-OUTPUT SECTION.                                            SQ1154.2
003800 FILE-CONTROL.                                                    SQ1154.2
003900*    SELECT RAW-DATA   ASSIGN TO                                  SQ1154.2
004000*    XXXXX062                                                     SQ1154.2
004100*           ORGANIZATION IS INDEXED                               SQ1154.2
004200*           ACCESS MODE IS RANDOM                                 SQ1154.2
004300*           RECORD KEY IS RAW-DATA-KEY.                           SQ1154.2
004400     SELECT PRINT-FILE ASSIGN TO                                  SQ1154.2
004500     XXXXX055.                                                    SQ1154.2
004600     SELECT SQ-FS5 ASSIGN                                         SQ1154.2
004700     XXXXX014                                                     SQ1154.2
004800     ORGANIZATION SEQUENTIAL                                      SQ1154.2
004900     ACCESS MODE SEQUENTIAL.                                      SQ1154.2
005000 DATA DIVISION.                                                   SQ1154.2
005100 FILE SECTION.                                                    SQ1154.2
005200*                                                                 SQ1154.2
005300*FD  RAW-DATA.                                                    SQ1154.2
005400*                                                                 SQ1154.2
005500*01  RAW-DATA-SATZ.                                               SQ1154.2
005600*    05  RAW-DATA-KEY        PIC X(6).                            SQ1154.2
005700*    05  C-DATE              PIC 9(6).                            SQ1154.2
005800*    05  C-TIME              PIC 9(8).                            SQ1154.2
005900*    05  C-NO-OF-TESTS       PIC 99.                              SQ1154.2
006000*    05  C-OK                PIC 999.                             SQ1154.2
006100*    05  C-ALL               PIC 999.                             SQ1154.2
006200*    05  C-FAIL              PIC 999.                             SQ1154.2
006300*    05  C-DELETED           PIC 999.                             SQ1154.2
006400*    05  C-INSPECT           PIC 999.                             SQ1154.2
006500*    05  C-NOTE              PIC X(13).                           SQ1154.2
006600*    05  C-INDENT            PIC X.                               SQ1154.2
006700*    05  C-ABORT             PIC X(8).                            SQ1154.2
006800 FD  PRINT-FILE                                                   SQ1154.2
006900C    LABEL RECORDS                                                SQ1154.2
007000C    XXXXX084                                                     SQ1154.2
007100C    DATA RECORD IS PRINT-REC DUMMY-RECORD                        SQ1154.2
007200               .                                                  SQ1154.2
007300 01  PRINT-REC PICTURE X(120).                                    SQ1154.2
007400 01  DUMMY-RECORD PICTURE X(120).                                 SQ1154.2
007500 FD  SQ-FS5                                                       SQ1154.2
007600C    LABEL RECORD STANDARD                                        SQ1154.2
007700                    .                                             SQ1154.2
007800 01  SQ-FS5R1-F-G-126.                                            SQ1154.2
007900     02  SQ-FS5-120  PICTURE X(120).                              SQ1154.2
008000     02  SQ-FS5-UPDATE PICTURE X(6).                              SQ1154.2
008100 WORKING-STORAGE SECTION.                                         SQ1154.2
008200 01  COUNT-OF-RECORDS PIC S9(5) COMPUTATIONAL.                    SQ1154.2
008300 01  RECORDS-IN-ERROR PIC S9(5) COMP VALUE ZERO.                  SQ1154.2
008400 01  ERROR-FLAG PIC 9.                                            SQ1154.2
008500 01  EOF-FLAG PIC 9.                                              SQ1154.2
008600 01  LOOP-COUNT PIC 99.                                           SQ1154.2
008700 01  FILE-RECORD-INFORMATION-REC.                                 SQ1154.2
008800     03 FILE-RECORD-INFO-SKELETON.                                SQ1154.2
008900        05 FILLER                 PICTURE X(48)       VALUE       SQ1154.2
009000             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  SQ1154.2
009100        05 FILLER                 PICTURE X(46)       VALUE       SQ1154.2
009200             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    SQ1154.2
009300        05 FILLER                 PICTURE X(26)       VALUE       SQ1154.2
009400             ",LFIL=000000,ORG=  ,LBLR= ".                        SQ1154.2
009500        05 FILLER                 PICTURE X(37)       VALUE       SQ1154.2
009600             ",RECKEY=                             ".             SQ1154.2
009700        05 FILLER                 PICTURE X(38)       VALUE       SQ1154.2
009800             ",ALTKEY1=                             ".            SQ1154.2
009900        05 FILLER                 PICTURE X(38)       VALUE       SQ1154.2
010000             ",ALTKEY2=                             ".            SQ1154.2
010100        05 FILLER                 PICTURE X(7)        VALUE SPACE.SQ1154.2
010200     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              SQ1154.2
010300        05 FILE-RECORD-INFO-P1-120.                               SQ1154.2
010400           07 FILLER              PIC X(5).                       SQ1154.2
010500           07 XFILE-NAME           PIC X(6).                      SQ1154.2
010600           07 FILLER              PIC X(8).                       SQ1154.2
010700           07 XRECORD-NAME         PIC X(6).                      SQ1154.2
010800           07 FILLER              PIC X(1).                       SQ1154.2
010900           07 REELUNIT-NUMBER     PIC 9(1).                       SQ1154.2
011000           07 FILLER              PIC X(7).                       SQ1154.2
011100           07 XRECORD-NUMBER       PIC 9(6).                      SQ1154.2
011200           07 FILLER              PIC X(6).                       SQ1154.2
011300           07 UPDATE-NUMBER       PIC 9(2).                       SQ1154.2
011400           07 FILLER              PIC X(5).                       SQ1154.2
011500           07 ODO-NUMBER          PIC 9(4).                       SQ1154.2
011600           07 FILLER              PIC X(5).                       SQ1154.2
011700           07 XPROGRAM-NAME        PIC X(5).                      SQ1154.2
011800           07 FILLER              PIC X(7).                       SQ1154.2
011900           07 XRECORD-LENGTH       PIC 9(6).                      SQ1154.2
012000           07 FILLER              PIC X(7).                       SQ1154.2
012100           07 CHARS-OR-RECORDS    PIC X(2).                       SQ1154.2
012200           07 FILLER              PIC X(1).                       SQ1154.2
012300           07 XBLOCK-SIZE          PIC 9(4).                      SQ1154.2
012400           07 FILLER              PIC X(6).                       SQ1154.2
012500           07 RECORDS-IN-FILE     PIC 9(6).                       SQ1154.2
012600           07 FILLER              PIC X(5).                       SQ1154.2
012700           07 XFILE-ORGANIZATION   PIC X(2).                      SQ1154.2
012800           07 FILLER              PIC X(6).                       SQ1154.2
012900           07 XLABEL-TYPE          PIC X(1).                      SQ1154.2
013000        05 FILE-RECORD-INFO-P121-240.                             SQ1154.2
013100           07 FILLER              PIC X(8).                       SQ1154.2
013200           07 XRECORD-KEY          PIC X(29).                     SQ1154.2
013300           07 FILLER              PIC X(9).                       SQ1154.2
013400           07 ALTERNATE-KEY1      PIC X(29).                      SQ1154.2
013500           07 FILLER              PIC X(9).                       SQ1154.2
013600           07 ALTERNATE-KEY2      PIC X(29).                      SQ1154.2
013700           07 FILLER              PIC X(7).                       SQ1154.2
013800 01  TEST-RESULTS.                                                SQ1154.2
013900     02 FILLER                    PICTURE X VALUE SPACE.          SQ1154.2
014000     02 FEATURE                   PICTURE X(20) VALUE SPACE.      SQ1154.2
014100     02 FILLER                    PICTURE X VALUE SPACE.          SQ1154.2
014200     02 P-OR-F                    PICTURE X(5) VALUE SPACE.       SQ1154.2
014300     02 FILLER                    PICTURE X  VALUE SPACE.         SQ1154.2
014400     02  PAR-NAME.                                                SQ1154.2
014500       03 FILLER PICTURE X(12) VALUE SPACE.                       SQ1154.2
014600       03  PARDOT-X PICTURE X  VALUE SPACE.                       SQ1154.2
014700       03 DOTVALUE PICTURE 99  VALUE ZERO.                        SQ1154.2
014800       03 FILLER PIC X(5) VALUE SPACE.                            SQ1154.2
014900     02 FILLER PIC X(10) VALUE SPACE.                             SQ1154.2
015000     02 RE-MARK PIC X(61).                                        SQ1154.2
015100 01  TEST-COMPUTED.                                               SQ1154.2
015200     02 FILLER PIC X(30) VALUE SPACE.                             SQ1154.2
015300     02 FILLER PIC X(17) VALUE "       COMPUTED=".                SQ1154.2
015400     02 COMPUTED-X.                                               SQ1154.2
015500     03 COMPUTED-A                PICTURE X(20) VALUE SPACE.      SQ1154.2
015600     03 COMPUTED-N REDEFINES COMPUTED-A PICTURE -9(9).9(9).       SQ1154.2
015700     03 COMPUTED-0V18 REDEFINES COMPUTED-A  PICTURE -.9(18).      SQ1154.2
015800     03 COMPUTED-4V14 REDEFINES COMPUTED-A  PICTURE -9(4).9(14).  SQ1154.2
015900     03 COMPUTED-14V4 REDEFINES COMPUTED-A  PICTURE -9(14).9(4).  SQ1154.2
016000     03       CM-18V0 REDEFINES COMPUTED-A.                       SQ1154.2
016100         04 COMPUTED-18V0                   PICTURE -9(18).       SQ1154.2
016200         04 FILLER                          PICTURE X.            SQ1154.2
016300     03 FILLER PIC X(50) VALUE SPACE.                             SQ1154.2
016400 01  TEST-CORRECT.                                                SQ1154.2
016500     02 FILLER PIC X(30) VALUE SPACE.                             SQ1154.2
016600     02 FILLER PIC X(17) VALUE "       CORRECT =".                SQ1154.2
016700     02 CORRECT-X.                                                SQ1154.2
016800     03 CORRECT-A                 PICTURE X(20) VALUE SPACE.      SQ1154.2
016900     03 CORRECT-N REDEFINES CORRECT-A PICTURE -9(9).9(9).         SQ1154.2
017000     03 CORRECT-0V18 REDEFINES CORRECT-A    PICTURE -.9(18).      SQ1154.2
017100     03 CORRECT-4V14 REDEFINES CORRECT-A    PICTURE -9(4).9(14).  SQ1154.2
017200     03 CORRECT-14V4 REDEFINES CORRECT-A    PICTURE -9(14).9(4).  SQ1154.2
017300     03      CR-18V0 REDEFINES CORRECT-A.                         SQ1154.2
017400         04 CORRECT-18V0                    PICTURE -9(18).       SQ1154.2
017500         04 FILLER                          PICTURE X.            SQ1154.2
017600     03 FILLER PIC X(50) VALUE SPACE.                             SQ1154.2
017700 01  CCVS-C-1.                                                    SQ1154.2
017800     02 FILLER PICTURE IS X(99) VALUE IS " FEATURE              PASQ1154.2
017900-    "SS  PARAGRAPH-NAME                                          SQ1154.2
018000-    "        REMARKS".                                           SQ1154.2
018100     02 FILLER PICTURE IS X(20) VALUE IS SPACE.                   SQ1154.2
018200 01  CCVS-C-2.                                                    SQ1154.2
018300     02 FILLER PICTURE IS X VALUE IS SPACE.                       SQ1154.2
018400     02 FILLER PICTURE IS X(6) VALUE IS "TESTED".                 SQ1154.2
018500     02 FILLER PICTURE IS X(15) VALUE IS SPACE.                   SQ1154.2
018600     02 FILLER PICTURE IS X(4) VALUE IS "FAIL".                   SQ1154.2
018700     02 FILLER PICTURE IS X(94) VALUE IS SPACE.                   SQ1154.2
018800 01  REC-SKL-SUB PICTURE 9(2) VALUE ZERO.                         SQ1154.2
018900 01  REC-CT PICTURE 99 VALUE ZERO.                                SQ1154.2
019000 01  DELETE-CNT                   PICTURE 999  VALUE ZERO.        SQ1154.2
019100 01  ERROR-COUNTER PICTURE IS 999 VALUE IS ZERO.                  SQ1154.2
019200 01  INSPECT-COUNTER PIC 999 VALUE ZERO.                          SQ1154.2
019300 01  PASS-COUNTER PIC 999 VALUE ZERO.                             SQ1154.2
019400 01  TOTAL-ERROR PIC 999 VALUE ZERO.                              SQ1154.2
019500 01  ERROR-HOLD PIC 999 VALUE ZERO.                               SQ1154.2
019600 01  DUMMY-HOLD PIC X(120) VALUE SPACE.                           SQ1154.2
019700 01  RECORD-COUNT PIC 9(5) VALUE ZERO.                            SQ1154.2
019800 01  CCVS-H-1.                                                    SQ1154.2
019900     02  FILLER   PICTURE X(27)  VALUE SPACE.                     SQ1154.2
020000     02 FILLER PICTURE X(67) VALUE                                SQ1154.2
020100     " FEDERAL SOFTWARE TESTING CENTER COBOL COMPILER VALIDATION  SQ1154.2
020200-    " SYSTEM".                                                   SQ1154.2
020300     02  FILLER     PICTURE X(26)  VALUE SPACE.                   SQ1154.2
020400 01  CCVS-H-2.                                                    SQ1154.2
020500     02 FILLER PICTURE X(52) VALUE IS                             SQ1154.2
020600     "CCVS85 FSTC COPY, NOT FOR DISTRIBUTION.".                   SQ1154.2
020700     02 FILLER PICTURE IS X(19) VALUE IS "TEST RESULTS SET-  ".   SQ1154.2
020800     02 TEST-ID PICTURE IS X(9).                                  SQ1154.2
020900     02 FILLER PICTURE IS X(40) VALUE IS SPACE.                   SQ1154.2
021000 01  CCVS-H-3.                                                    SQ1154.2
021100     02  FILLER PICTURE X(34) VALUE                               SQ1154.2
021200     " FOR OFFICIAL USE ONLY    ".                                SQ1154.2
021300     02  FILLER PICTURE X(58) VALUE                               SQ1154.2
021400     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ1154.2
021500     02  FILLER PICTURE X(28) VALUE                               SQ1154.2
021600     "  COPYRIGHT   1985 ".                                       SQ1154.2
021700 01  CCVS-E-1.                                                    SQ1154.2
021800     02 FILLER PICTURE IS X(52) VALUE IS SPACE.                   SQ1154.2
021900     02 FILLER PICTURE IS X(14) VALUE IS "END OF TEST-  ".        SQ1154.2
022000     02 ID-AGAIN PICTURE IS X(9).                                 SQ1154.2
022100     02 FILLER PICTURE X(45) VALUE IS                             SQ1154.2
022200     " NTIS DISTRIBUTION COBOL 85".                               SQ1154.2
022300 01  CCVS-E-2.                                                    SQ1154.2
022400     02  FILLER                   PICTURE X(31)  VALUE            SQ1154.2
022500     SPACE.                                                       SQ1154.2
022600     02  FILLER                   PICTURE X(21)  VALUE SPACE.     SQ1154.2
022700     02 CCVS-E-2-2.                                               SQ1154.2
022800         03 ERROR-TOTAL PICTURE IS XXX VALUE IS SPACE.            SQ1154.2
022900         03 FILLER PICTURE IS X VALUE IS SPACE.                   SQ1154.2
023000         03 ENDER-DESC PIC X(46) VALUE "ERRORS ENCOUNTERED".      SQ1154.2
023100 01  CCVS-E-3.                                                    SQ1154.2
023200     02  FILLER PICTURE X(22) VALUE                               SQ1154.2
023300     " FOR OFFICIAL USE ONLY".                                    SQ1154.2
023400     02  FILLER PICTURE X(12) VALUE SPACE.                        SQ1154.2
023500     02  FILLER PICTURE X(58) VALUE                               SQ1154.2
023600     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1154.2
023700     02  FILLER PICTURE X(13) VALUE SPACE.                        SQ1154.2
023800     02 FILLER PIC X(15) VALUE " COPYRIGHT 1985".                 SQ1154.2
023900 01  CCVS-E-4.                                                    SQ1154.2
024000     02 CCVS-E-4-1 PIC XXX VALUE SPACE.                           SQ1154.2
024100     02 FILLER PIC XXXX VALUE " OF ".                             SQ1154.2
024200     02 CCVS-E-4-2 PIC XXX VALUE SPACE.                           SQ1154.2
024300     02 FILLER PIC X(40) VALUE                                    SQ1154.2
024400      "  TESTS WERE EXECUTED SUCCESSFULLY".                       SQ1154.2
024500 01  XXINFO.                                                      SQ1154.2
024600     02 FILLER PIC X(30) VALUE "        *** INFORMATION  ***".    SQ1154.2
024700     02 INFO-TEXT.                                                SQ1154.2
024800     04 FILLER PIC X(20) VALUE SPACE.                             SQ1154.2
024900     04 XXCOMPUTED PIC X(20).                                     SQ1154.2
025000     04 FILLER PIC X(5) VALUE SPACE.                              SQ1154.2
025100     04 XXCORRECT PIC X(20).                                      SQ1154.2
025200 01  HYPHEN-LINE.                                                 SQ1154.2
025300     02 FILLER PICTURE IS X VALUE IS SPACE.                       SQ1154.2
025400     02 FILLER PICTURE IS X(65) VALUE IS "************************SQ1154.2
025500-    "*****************************************".                 SQ1154.2
025600     02 FILLER PICTURE IS X(54) VALUE IS "************************SQ1154.2
025700-    "******************************".                            SQ1154.2
025800 01  CCVS-PGM-ID PIC X(6) VALUE                                   SQ1154.2
025900     "SQ115A".                                                    SQ1154.2
026000 PROCEDURE DIVISION.                                              SQ1154.2
026100 CCVS1 SECTION.                                                   SQ1154.2
026200 OPEN-FILES.                                                      SQ1154.2
026300*    OPEN I-O RAW-DATA.                                           SQ1154.2
026400*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            SQ1154.2
026500*    READ RAW-DATA INVALID KEY GO TO END-E-1.                     SQ1154.2
026600*    MOVE "ABORTED " TO C-ABORT.                                  SQ1154.2
026700*    ADD 1 TO C-NO-OF-TESTS.                                      SQ1154.2
026800*    ACCEPT C-DATE  FROM DATE.                                    SQ1154.2
026900*    ACCEPT C-TIME  FROM TIME.                                    SQ1154.2
027000*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-1.             SQ1154.2
027100*END-E-1.                                                         SQ1154.2
027200*    CLOSE RAW-DATA.                                              SQ1154.2
027300     OPEN     OUTPUT PRINT-FILE.                                  SQ1154.2
027400     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   SQ1154.2
027500     MOVE    SPACE TO TEST-RESULTS.                               SQ1154.2
027600     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             SQ1154.2
027700     MOVE ZERO TO REC-SKL-SUB.                                    SQ1154.2
027800     PERFORM CCVS-INIT-FILE 9 TIMES.                              SQ1154.2
027900 CCVS-INIT-FILE.                                                  SQ1154.2
028000     ADD 1 TO REC-SKL-SUB.                                        SQ1154.2
028100     MOVE FILE-RECORD-INFO-SKELETON TO                            SQ1154.2
028200                  FILE-RECORD-INFO (REC-SKL-SUB).                 SQ1154.2
028300 CCVS-INIT-EXIT.                                                  SQ1154.2
028400     GO TO CCVS1-EXIT.                                            SQ1154.2
028500 CLOSE-FILES.                                                     SQ1154.2
028600     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   SQ1154.2
028700*    OPEN I-O RAW-DATA.                                           SQ1154.2
028800*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            SQ1154.2
028900*    READ RAW-DATA INVALID KEY GO TO END-E-2.                     SQ1154.2
029000*    MOVE "OK.     " TO C-ABORT.                                  SQ1154.2
029100*    MOVE PASS-COUNTER TO C-OK.                                   SQ1154.2
029200*    MOVE ERROR-HOLD   TO C-ALL.                                  SQ1154.2
029300*    MOVE ERROR-COUNTER TO C-FAIL.                                SQ1154.2
029400*    MOVE DELETE-CNT TO C-DELETED.                                SQ1154.2
029500*    MOVE INSPECT-COUNTER TO C-INSPECT.                           SQ1154.2
029600*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-2.             SQ1154.2
029700*END-E-2.                                                         SQ1154.2
029800*    CLOSE RAW-DATA.                                              SQ1154.2
029900 TERMINATE-CCVS.                                                  SQ1154.2
030000*    EXIT PROGRAM.                                                SQ1154.2
030100*TERMINATE-CALL.                                                  SQ1154.2
030200     STOP     RUN.                                                SQ1154.2
030300 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         SQ1154.2
030400 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           SQ1154.2
030500 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          SQ1154.2
030600 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-CNT.          SQ1154.2
030700     MOVE "****TEST DELETED****" TO RE-MARK.                      SQ1154.2
030800 PRINT-DETAIL.                                                    SQ1154.2
030900     IF REC-CT NOT EQUAL TO ZERO                                  SQ1154.2
031000             MOVE "." TO PARDOT-X                                 SQ1154.2
031100             MOVE REC-CT TO DOTVALUE.                             SQ1154.2
031200     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      SQ1154.2
031300     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               SQ1154.2
031400        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 SQ1154.2
031500          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 SQ1154.2
031600     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              SQ1154.2
031700     MOVE SPACE TO CORRECT-X.                                     SQ1154.2
031800     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         SQ1154.2
031900     MOVE     SPACE TO RE-MARK.                                   SQ1154.2
032000 HEAD-ROUTINE.                                                    SQ1154.2
032100     MOVE CCVS-H-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ1154.2
032200     MOVE CCVS-H-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.   SQ1154.2
032300     MOVE CCVS-H-3 TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.   SQ1154.2
032400 COLUMN-NAMES-ROUTINE.                                            SQ1154.2
032500     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ1154.2
032600     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ1154.2
032700     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ1154.2
032800 END-ROUTINE.                                                     SQ1154.2
032900     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.SQ1154.2
033000 END-RTN-EXIT.                                                    SQ1154.2
033100     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ1154.2
033200 END-ROUTINE-1.                                                   SQ1154.2
033300      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      SQ1154.2
033400      ERROR-HOLD. ADD DELETE-CNT TO ERROR-HOLD.                   SQ1154.2
033500      ADD PASS-COUNTER TO ERROR-HOLD.                             SQ1154.2
033600*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   SQ1154.2
033700      MOVE PASS-COUNTER TO CCVS-E-4-1.                            SQ1154.2
033800      MOVE ERROR-HOLD TO CCVS-E-4-2.                              SQ1154.2
033900      MOVE CCVS-E-4 TO CCVS-E-2-2.                                SQ1154.2
034000      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           SQ1154.2
034100  END-ROUTINE-12.                                                 SQ1154.2
034200      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        SQ1154.2
034300     IF       ERROR-COUNTER IS EQUAL TO ZERO                      SQ1154.2
034400         MOVE "NO " TO ERROR-TOTAL                                SQ1154.2
034500         ELSE                                                     SQ1154.2
034600         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       SQ1154.2
034700     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           SQ1154.2
034800     PERFORM WRITE-LINE.                                          SQ1154.2
034900 END-ROUTINE-13.                                                  SQ1154.2
035000     IF DELETE-CNT IS EQUAL TO ZERO                               SQ1154.2
035100         MOVE "NO " TO ERROR-TOTAL  ELSE                          SQ1154.2
035200         MOVE DELETE-CNT TO ERROR-TOTAL.                          SQ1154.2
035300     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   SQ1154.2
035400     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ1154.2
035500      IF   INSPECT-COUNTER EQUAL TO ZERO                          SQ1154.2
035600          MOVE "NO " TO ERROR-TOTAL                               SQ1154.2
035700      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   SQ1154.2
035800      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            SQ1154.2
035900      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          SQ1154.2
036000     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ1154.2
036100 WRITE-LINE.                                                      SQ1154.2
036200     ADD 1 TO RECORD-COUNT.                                       SQ1154.2
036300*    IF RECORD-COUNT GREATER 50                                   SQ1154.2
036400*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          SQ1154.2
036500*        MOVE SPACE TO DUMMY-RECORD                               SQ1154.2
036600*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  SQ1154.2
036700*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             SQ1154.2
036800*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     SQ1154.2
036900*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          SQ1154.2
037000*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          SQ1154.2
037100*        MOVE ZERO TO RECORD-COUNT.                               SQ1154.2
037200     PERFORM WRT-LN.                                              SQ1154.2
037300 WRT-LN.                                                          SQ1154.2
037400     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               SQ1154.2
037500     MOVE SPACE TO DUMMY-RECORD.                                  SQ1154.2
037600 BLANK-LINE-PRINT.                                                SQ1154.2
037700     PERFORM WRT-LN.                                              SQ1154.2
037800 FAIL-ROUTINE.                                                    SQ1154.2
037900     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.   SQ1154.2
038000     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.    SQ1154.2
038100     MOVE "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.    SQ1154.2
038200     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     SQ1154.2
038300     GO TO FAIL-ROUTINE-EX.                                       SQ1154.2
038400 FAIL-ROUTINE-WRITE.                                              SQ1154.2
038500     MOVE TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE           SQ1154.2
038600     MOVE TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES.   SQ1154.2
038700 FAIL-ROUTINE-EX. EXIT.                                           SQ1154.2
038800 BAIL-OUT.                                                        SQ1154.2
038900     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.       SQ1154.2
039000     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.               SQ1154.2
039100 BAIL-OUT-WRITE.                                                  SQ1154.2
039200     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  SQ1154.2
039300     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     SQ1154.2
039400 BAIL-OUT-EX. EXIT.                                               SQ1154.2
039500 CCVS1-EXIT.                                                      SQ1154.2
039600     EXIT.                                                        SQ1154.2
039700 SECT-SQ-115-0001 SECTION.                                        SQ1154.2
039800 SEQ-INIT-013.                                                    SQ1154.2
039900     MOVE "SQ-FS5" TO XFILE-NAME (1).                             SQ1154.2
040000     MOVE "R1-F-G" TO XRECORD-NAME (1).                           SQ1154.2
040100     MOVE CCVS-PGM-ID TO XPROGRAM-NAME (1).                       SQ1154.2
040200     MOVE 000126 TO XRECORD-LENGTH (1).                           SQ1154.2
040300     MOVE "RC" TO CHARS-OR-RECORDS (1).                           SQ1154.2
040400     MOVE 0001 TO XBLOCK-SIZE (1).                                SQ1154.2
040500     MOVE 000550 TO RECORDS-IN-FILE (1).                          SQ1154.2
040600     MOVE "SQ" TO XFILE-ORGANIZATION (1).                         SQ1154.2
040700     MOVE "S" TO XLABEL-TYPE (1).                                 SQ1154.2
040800     MOVE 000001 TO XRECORD-NUMBER (1).                           SQ1154.2
040900     OPEN OUTPUT SQ-FS5.                                          SQ1154.2
041000     MOVE ZERO TO COUNT-OF-RECORDS.                               SQ1154.2
041100 SEQ-TEST-013.                                                    SQ1154.2
041200     MOVE FILE-RECORD-INFO-P1-120 (1) TO SQ-FS5-120.              SQ1154.2
041300     MOVE "FIRST " TO SQ-FS5-UPDATE.                              SQ1154.2
041400     WRITE SQ-FS5R1-F-G-126.                                      SQ1154.2
041500     ADD 1 TO COUNT-OF-RECORDS.                                   SQ1154.2
041600     IF COUNT-OF-RECORDS EQUAL TO 550                             SQ1154.2
041700          GO TO SEQ-WRITE-013.                                    SQ1154.2
041800     ADD 1 TO XRECORD-NUMBER (1).                                 SQ1154.2
041900     GO TO SEQ-TEST-013.                                          SQ1154.2
042000 SEQ-WRITE-013.                                                   SQ1154.2
042100     MOVE "CREATE SQ-FS5 550R" TO FEATURE.                        SQ1154.2
042200     MOVE "SEQ-TEST-013" TO PAR-NAME.                             SQ1154.2
042300     MOVE "FILE CREATED, RECS =" TO COMPUTED-A.                   SQ1154.2
042400     MOVE COUNT-OF-RECORDS TO CORRECT-18V0.                       SQ1154.2
042500     PERFORM PRINT-DETAIL.                                        SQ1154.2
042600     CLOSE SQ-FS5.                                                SQ1154.2
042700*         A SEQUENTIAL MASS STORAGE FILE WITH 126 CHARACTER       SQ1154.2
042800*    RECORDS HAS BEEN CREATED.  THE FILE CONTAINS 550 RECORDS.    SQ1154.2
042900 SEQ-INIT-014.                                                    SQ1154.2
043000     MOVE ZERO TO COUNT-OF-RECORDS.                               SQ1154.2
043100*        THIS TEST READS AND CHECKS THE FILE CREATED              SQ1154.2
043200*    IN SEQ-TEST-013.                                             SQ1154.2
043300     OPEN INPUT SQ-FS5.                                           SQ1154.2
043400 SEQ-TEST-014.                                                    SQ1154.2
043500     READ SQ-FS5 AT END                                           SQ1154.2
043600          GO TO SEQ-TEST-014-1.                                   SQ1154.2
043700     ADD 1 TO COUNT-OF-RECORDS.                                   SQ1154.2
043800     MOVE SQ-FS5-120 TO FILE-RECORD-INFO-P1-120 (1).              SQ1154.2
043900     IF COUNT-OF-RECORDS GREATER THAN 550                         SQ1154.2
044000         MOVE "MORE THAN 550 RECORDS" TO RE-MARK                  SQ1154.2
044100         GO TO SEQ-FAIL-014.                                      SQ1154.2
044200     IF COUNT-OF-RECORDS NOT EQUAL TO XRECORD-NUMBER (1)          SQ1154.2
044300         ADD 1 TO RECORDS-IN-ERROR                                SQ1154.2
044400         GO TO SEQ-TEST-014.                                      SQ1154.2
044500     IF XFILE-NAME (1) NOT EQUAL TO "SQ-FS5"                      SQ1154.2
044600         ADD 1 TO RECORDS-IN-ERROR                                SQ1154.2
044700         GO TO SEQ-TEST-014.                                      SQ1154.2
044800     IF UPDATE-NUMBER (1) NOT EQUAL TO ZERO                       SQ1154.2
044900         ADD 1 TO RECORDS-IN-ERROR                                SQ1154.2
045000         GO TO SEQ-TEST-014.                                      SQ1154.2
045100     IF SQ-FS5-UPDATE EQUAL TO "FIRST "                           SQ1154.2
045200         GO TO SEQ-TEST-014.                                      SQ1154.2
045300     ADD 1 TO RECORDS-IN-ERROR.                                   SQ1154.2
045400     GO TO SEQ-TEST-014.                                          SQ1154.2
045500 SEQ-TEST-014-1.                                                  SQ1154.2
045600     IF RECORDS-IN-ERROR EQUAL TO ZERO                            SQ1154.2
045700         GO TO SEQ-PASS-014.                                      SQ1154.2
045800     MOVE "ERRORS IN READING SQ-FS5" TO RE-MARK.                  SQ1154.2
045900 SEQ-FAIL-014.                                                    SQ1154.2
046000     MOVE "RECORDS IN ERROR =" TO COMPUTED-A.                     SQ1154.2
046100     MOVE RECORDS-IN-ERROR TO CORRECT-18V0.                       SQ1154.2
046200     MOVE "VII-48; 4.5.2                            " TO  RE-MARK.SQ1154.2
046300     PERFORM FAIL.                                                SQ1154.2
046400     GO TO SEQ-WRITE-014.                                         SQ1154.2
046500 SEQ-PASS-014.                                                    SQ1154.2
046600     PERFORM PASS.                                                SQ1154.2
046700     MOVE "FILE VERIFIED RECS =" TO COMPUTED-A.                   SQ1154.2
046800     MOVE COUNT-OF-RECORDS TO CORRECT-18V0.                       SQ1154.2
046900 SEQ-WRITE-014.                                                   SQ1154.2
047000     MOVE "SEQ-TEST-014" TO PAR-NAME.                             SQ1154.2
047100     MOVE "VERIFY FILE SQ-FS5" TO FEATURE.                        SQ1154.2
047200     PERFORM PRINT-DETAIL.                                        SQ1154.2
047300 SEQ-CLOSE-014.                                                   SQ1154.2
047400     CLOSE SQ-FS5.                                                SQ1154.2
047500 REWRITE-INIT-GF-01.                                              SQ1154.2
047600     OPEN I-O SQ-FS5.                                             SQ1154.2
047700     MOVE ZERO TO COUNT-OF-RECORDS.                               SQ1154.2
047800     MOVE ZERO TO EOF-FLAG.                                       SQ1154.2
047900*        THIS TEST REWRITES EVERY TENTH RECORD                    SQ1154.2
048000*    OF THE FILE SQ-FS5.                                          SQ1154.2
048100 REWRITE-TEST-GF-01.                                              SQ1154.2
048200     PERFORM READ-SQ-FS5 THRU READ-SQ-FS5-EXIT 10 TIMES.          SQ1154.2
048300     IF EOF-FLAG EQUAL TO 1                                       SQ1154.2
048400          GO TO REWRITE-TEST-GF-01-1.                             SQ1154.2
048500     MOVE SQ-FS5-120 TO FILE-RECORD-INFO-P1-120 (1).              SQ1154.2
048600     ADD 1 TO UPDATE-NUMBER (1).                                  SQ1154.2
048700     MOVE FILE-RECORD-INFO-P1-120 (1) TO SQ-FS5-120.              SQ1154.2
048800     MOVE "SECOND" TO SQ-FS5-UPDATE.                              SQ1154.2
048900     REWRITE SQ-FS5R1-F-G-126.                                    SQ1154.2
049000     GO TO REWRITE-TEST-GF-01.                                    SQ1154.2
049100 READ-SQ-FS5.                                                     SQ1154.2
049200     IF EOF-FLAG EQUAL TO 1                                       SQ1154.2
049300          GO TO READ-SQ-FS5-EXIT.                                 SQ1154.2
049400     READ SQ-FS5 RECORD                                           SQ1154.2
049500          AT END  MOVE 1 TO EOF-FLAG                              SQ1154.2
049600          GO TO READ-SQ-FS5-EXIT.                                 SQ1154.2
049700     ADD 1 TO COUNT-OF-RECORDS.                                   SQ1154.2
049800 READ-SQ-FS5-EXIT.                                                SQ1154.2
049900     EXIT.                                                        SQ1154.2
050000 REWRITE-TEST-GF-01-1.                                            SQ1154.2
050100     IF COUNT-OF-RECORDS EQUAL TO 550                             SQ1154.2
050200         GO TO REWRITE-PASS-GF-01.                                SQ1154.2
050300 REWRITE-FAIL-GF-01.                                              SQ1154.2
050400     MOVE "VII-48; 4.5.2                            " TO  RE-MARK.SQ1154.2
050500     PERFORM FAIL.                                                SQ1154.2
050600     MOVE "550 RECORDS SHOULD BE READ" TO RE-MARK.                SQ1154.2
050700     MOVE "RECORDS READ =" TO COMPUTED-A.                         SQ1154.2
050800     MOVE COUNT-OF-RECORDS TO CORRECT-18V0.                       SQ1154.2
050900     GO TO REWRITE-WRITE-GF-01.                                   SQ1154.2
051000 REWRITE-PASS-GF-01.                                              SQ1154.2
051100     PERFORM PASS.                                                SQ1154.2
051200 REWRITE-WRITE-GF-01.                                             SQ1154.2
051300     MOVE "RWRT-TEST-GF-01" TO PAR-NAME.                          SQ1154.2
051400     MOVE "REWRITE FILE SQ-FS5" TO FEATURE.                       SQ1154.2
051500     PERFORM PRINT-DETAIL.                                        SQ1154.2
051600 REWRITE-CLOSE-GF-01.                                             SQ1154.2
051700     CLOSE SQ-FS5.                                                SQ1154.2
051800 REWRITE-INIT-GF-02.                                              SQ1154.2
051900     MOVE ZERO TO COUNT-OF-RECORDS.                               SQ1154.2
052000     MOVE ZERO TO EOF-FLAG.                                       SQ1154.2
052100     OPEN INPUT SQ-FS5.                                           SQ1154.2
052200*        THIS TEST READS AND CHECKS THE FILE WHICH WAS            SQ1154.2
052300*    REWRITTEN IN REWRITE-TEST-01.                                SQ1154.2
052400     MOVE ZERO TO RECORDS-IN-ERROR.                               SQ1154.2
052500     MOVE ZERO TO LOOP-COUNT.                                     SQ1154.2
052600 REWRITE-TEST-GF-02.                                              SQ1154.2
052700     READ SQ-FS5  END                                             SQ1154.2
052800          MOVE 1 TO EOF-FLAG                                      SQ1154.2
052900          GO TO REWRITE-TEST-GF-02-2.                             SQ1154.2
053000     ADD 1 TO COUNT-OF-RECORDS.                                   SQ1154.2
053100     IF COUNT-OF-RECORDS GREATER THAN 550                         SQ1154.2
053200          MOVE "MORE THAN 550 RECORDS" TO RE-MARK                 SQ1154.2
053300          GO TO REWRITE-FAIL-GF-02.                               SQ1154.2
053400     ADD 1 TO LOOP-COUNT.                                         SQ1154.2
053500     MOVE SQ-FS5-120 TO FILE-RECORD-INFO-P1-120 (1).              SQ1154.2
053600     IF XFILE-NAME (1) NOT EQUAL TO "SQ-FS5"                      SQ1154.2
053700          ADD 1 TO RECORDS-IN-ERROR                               SQ1154.2
053800          GO TO REWRITE-TEST-GF-02.                               SQ1154.2
053900     IF LOOP-COUNT EQUAL TO 10                                    SQ1154.2
054000          MOVE ZERO TO LOOP-COUNT                                 SQ1154.2
054100          GO TO REWRITE-TEST-GF-02-1.                             SQ1154.2
054200     IF UPDATE-NUMBER (1) NOT EQUAL TO ZERO                       SQ1154.2
054300          ADD 1 TO RECORDS-IN-ERROR                               SQ1154.2
054400          GO TO REWRITE-TEST-GF-02.                               SQ1154.2
054500     IF SQ-FS5-UPDATE EQUAL TO "FIRST "                           SQ1154.2
054600          GO TO REWRITE-TEST-GF-02.                               SQ1154.2
054700     ADD 1 TO RECORDS-IN-ERROR.                                   SQ1154.2
054800     GO TO REWRITE-TEST-GF-02.                                    SQ1154.2
054900 REWRITE-TEST-GF-02-1.                                            SQ1154.2
055000     IF UPDATE-NUMBER (1) NOT EQUAL TO 1                          SQ1154.2
055100          ADD 1 TO RECORDS-IN-ERROR                               SQ1154.2
055200          GO TO REWRITE-TEST-GF-02.                               SQ1154.2
055300     IF SQ-FS5-UPDATE EQUAL TO "SECOND"                           SQ1154.2
055400          GO TO REWRITE-TEST-GF-02.                               SQ1154.2
055500     ADD 1 TO RECORDS-IN-ERROR.                                   SQ1154.2
055600     GO TO REWRITE-TEST-GF-02.                                    SQ1154.2
055700 REWRITE-TEST-GF-02-2.                                            SQ1154.2
055800     IF COUNT-OF-RECORDS NOT EQUAL TO 550                         SQ1154.2
055900         MOVE "LESS THAN 550 RECORDS" TO RE-MARK                  SQ1154.2
056000         MOVE "RECORDS READ =" TO COMPUTED-A                      SQ1154.2
056100         MOVE COUNT-OF-RECORDS TO CORRECT-18V0                    SQ1154.2
056200         GO TO REWRITE-FAIL-GF-02.                                SQ1154.2
056300     IF RECORDS-IN-ERROR NOT EQUAL TO ZERO                        SQ1154.2
056400         MOVE "ERRORS IN READING SQ-FS5" TO RE-MARK               SQ1154.2
056500         MOVE "RECORDS IN ERROR =" TO COMPUTED-A                  SQ1154.2
056600         MOVE RECORDS-IN-ERROR TO CORRECT-18V0                    SQ1154.2
056700         GO TO REWRITE-FAIL-GF-02.                                SQ1154.2
056800 REWRITE-PASS-GF-02.                                              SQ1154.2
056900     PERFORM PASS.                                                SQ1154.2
057000     GO TO REWRITE-WRITE-GF-02.                                   SQ1154.2
057100 REWRITE-FAIL-GF-02.                                              SQ1154.2
057200     MOVE "VII-48; 4.5.2                            " TO  RE-MARK.SQ1154.2
057300     PERFORM FAIL.                                                SQ1154.2
057400 REWRITE-WRITE-GF-02.                                             SQ1154.2
057500     MOVE "RWRT-TEST-GF-02" TO PAR-NAME.                          SQ1154.2
057600     MOVE "VERIFY FILE SQ-FS5" TO FEATURE.                        SQ1154.2
057700     PERFORM PRINT-DETAIL.                                        SQ1154.2
057800 REWRITE-CLOSE-GF-02.                                             SQ1154.2
057900     CLOSE SQ-FS5.                                                SQ1154.2
058000 TERMINATE-ROUTINE.                                               SQ1154.2
058100     EXIT.                                                        SQ1154.2
058200 CCVS-EXIT SECTION.                                               SQ1154.2
058300 CCVS-999999.                                                     SQ1154.2
058400     GO TO CLOSE-FILES.                                           SQ1154.2
      *END-OF,SQ115A                                                                  
