      *HEADER,COBOL,SQ116A                                                            
000100 IDENTIFICATION DIVISION.                                         SQ1164.2
000200 PROGRAM-ID.                                                      SQ1164.2
000300     SQ116A.                                                      SQ1164.2
000400****************************************************************  SQ1164.2
000500*                                                              *  SQ1164.2
000600*    VALIDATION FOR:-                                          *  SQ1164.2
000700*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1164.2
000800*                                                              *  SQ1164.2
000900*    CREATION DATE     /     VALIDATION DATE                   *  SQ1164.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ1164.2
001100*                                                              *  SQ1164.2
001200****************************************************************  SQ1164.2
001300                                                                  SQ1164.2
001400*        THIS PROGRAM CREATES A SEQUENTIAL MASS STORAGE FILE      SQ1164.2
001500*    OF 550 RECORDS.  THE FILE IS THEN OPENED IN THE I-O MODE AND SQ1164.2
001600*    RECORDS ARE UPDATED USING REWRITE...FROM STATEMENTS.  THE    SQ1164.2
001700*    FILE IS THEN READ AGAIN CHECKING EACH RECORD TO ENSURE       SQ1164.2
001800*    THE REWRITES WERE EXECUTED CORRECTLY.                        SQ1164.2
001900*                                                                 SQ1164.2
002000*    USED X-CARDS:                                                SQ1164.2
002100*         XXXXX014                                                SQ1164.2
002200*         XXXXX055                                                SQ1164.2
002300*     P   XXXXX062                                                SQ1164.2
002400*         XXXXX082                                                SQ1164.2
002500*         XXXXX083                                                SQ1164.2
002600*     C   XXXXX084                                                SQ1164.2
002700*                                                                 SQ1164.2
002800*                                                                 SQ1164.2
002900 ENVIRONMENT DIVISION.                                            SQ1164.2
003000 CONFIGURATION SECTION.                                           SQ1164.2
003100 SOURCE-COMPUTER.                                                 SQ1164.2
003200     XXXXX082.                                                    SQ1164.2
003300 OBJECT-COMPUTER.                                                 SQ1164.2
003400     XXXXX083.                                                    SQ1164.2
003500 INPUT-OUTPUT SECTION.                                            SQ1164.2
003600 FILE-CONTROL.                                                    SQ1164.2
003700*    SELECT RAW-DATA   ASSIGN TO                                  SQ1164.2
003800*    XXXXX062                                                     SQ1164.2
003900*           ORGANIZATION IS INDEXED                               SQ1164.2
004000*           ACCESS MODE IS RANDOM                                 SQ1164.2
004100*           RECORD KEY IS RAW-DATA-KEY.                           SQ1164.2
004200     SELECT PRINT-FILE ASSIGN TO                                  SQ1164.2
004300     XXXXX055.                                                    SQ1164.2
004400     SELECT SQ-FS6 ASSIGN                                         SQ1164.2
004500     XXXXX014                                                     SQ1164.2
004600     ORGANIZATION SEQUENTIAL                                      SQ1164.2
004700     ACCESS MODE SEQUENTIAL.                                      SQ1164.2
004800 DATA DIVISION.                                                   SQ1164.2
004900 FILE SECTION.                                                    SQ1164.2
005000*                                                                 SQ1164.2
005100*FD  RAW-DATA.                                                    SQ1164.2
005200*                                                                 SQ1164.2
005300*01  RAW-DATA-SATZ.                                               SQ1164.2
005400*    05  RAW-DATA-KEY        PIC X(6).                            SQ1164.2
005500*    05  C-DATE              PIC 9(6).                            SQ1164.2
005600*    05  C-TIME              PIC 9(8).                            SQ1164.2
005700*    05  C-NO-OF-TESTS       PIC 99.                              SQ1164.2
005800*    05  C-OK                PIC 999.                             SQ1164.2
005900*    05  C-ALL               PIC 999.                             SQ1164.2
006000*    05  C-FAIL              PIC 999.                             SQ1164.2
006100*    05  C-DELETED           PIC 999.                             SQ1164.2
006200*    05  C-INSPECT           PIC 999.                             SQ1164.2
006300*    05  C-NOTE              PIC X(13).                           SQ1164.2
006400*    05  C-INDENT            PIC X.                               SQ1164.2
006500*    05  C-ABORT             PIC X(8).                            SQ1164.2
006600 FD  PRINT-FILE                                                   SQ1164.2
006700C    LABEL RECORDS                                                SQ1164.2
006800C    XXXXX084                                                     SQ1164.2
006900C    DATA RECORD IS PRINT-REC DUMMY-RECORD                        SQ1164.2
007000               .                                                  SQ1164.2
007100 01  PRINT-REC PICTURE X(120).                                    SQ1164.2
007200 01  DUMMY-RECORD PICTURE X(120).                                 SQ1164.2
007300 FD  SQ-FS6                                                       SQ1164.2
007400C    LABEL RECORD STANDARD                                        SQ1164.2
007500               .                                                  SQ1164.2
007600 01  SQ-FS6R1-F-G-130.                                            SQ1164.2
007700     02  SQ-FS6R1-PART1  PICTURE X(120).                          SQ1164.2
007800     02  SQ-FS6R1-PART2  PICTURE X(10).                           SQ1164.2
007900 WORKING-STORAGE SECTION.                                         SQ1164.2
008000 01  COUNT-OF-RECS PICTURE S9(5) COMPUTATIONAL.                   SQ1164.2
008100 01  RECORDS-IN-ERROR PIC S9(5) COMP VALUE ZERO.                  SQ1164.2
008200 01  ERROR-FLAG PIC 9.                                            SQ1164.2
008300 01  EOF-FLAG PIC 9.                                              SQ1164.2
008400 01  END-OF-RECORD-AREA.                                          SQ1164.2
008500     02  UPDATE-AREA-ONLY  PIC X(6).                              SQ1164.2
008600     02  NUMBER-AREA   PIC 9999.                                  SQ1164.2
008700 01  REWRT-FROM-AREA1.                                            SQ1164.2
008800     02  AREA1-1 PICTURE X(120).                                  SQ1164.2
008900     02  AREA1-2.                                                 SQ1164.2
009000         03  AREA1-21 PIC X(6).                                   SQ1164.2
009100         03  AREA1-22 PIC 9999.                                   SQ1164.2
009200 01  REWRT-FROM-AREA2.                                            SQ1164.2
009300     02  AREA2-1.                                                 SQ1164.2
009400         03  AREA2-11 PIC X(120).                                 SQ1164.2
009500         03  AREA2-12 PIC X(6).                                   SQ1164.2
009600         03  AREA2-13 PIC 9999.                                   SQ1164.2
009700     02  AREA2-2  PIC X(9).                                       SQ1164.2
009800 01  RWRT-FROM-AREA3.                                             SQ1164.2
009900     02  AREA3-1  PICTURE X(87).                                  SQ1164.2
010000 01  FOLLOWS-AREA3 PICTURE X(9).                                  SQ1164.2
010100 01  FILE-RECORD-INFORMATION-REC.                                 SQ1164.2
010200     03 FILE-RECORD-INFO-SKELETON.                                SQ1164.2
010300        05 FILLER                 PICTURE X(48)       VALUE       SQ1164.2
010400             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  SQ1164.2
010500        05 FILLER                 PICTURE X(46)       VALUE       SQ1164.2
010600             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    SQ1164.2
010700        05 FILLER                 PICTURE X(26)       VALUE       SQ1164.2
010800             ",LFIL=000000,ORG=  ,LBLR= ".                        SQ1164.2
010900        05 FILLER                 PICTURE X(37)       VALUE       SQ1164.2
011000             ",RECKEY=                             ".             SQ1164.2
011100        05 FILLER                 PICTURE X(38)       VALUE       SQ1164.2
011200             ",ALTKEY1=                             ".            SQ1164.2
011300        05 FILLER                 PICTURE X(38)       VALUE       SQ1164.2
011400             ",ALTKEY2=                             ".            SQ1164.2
011500        05 FILLER                 PICTURE X(7)        VALUE SPACE.SQ1164.2
011600     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              SQ1164.2
011700        05 FILE-RECORD-INFO-P1-120.                               SQ1164.2
011800           07 FILLER              PIC X(5).                       SQ1164.2
011900           07 XFILE-NAME           PIC X(6).                      SQ1164.2
012000           07 FILLER              PIC X(8).                       SQ1164.2
012100           07 XRECORD-NAME         PIC X(6).                      SQ1164.2
012200           07 FILLER              PIC X(1).                       SQ1164.2
012300           07 REELUNIT-NUMBER     PIC 9(1).                       SQ1164.2
012400           07 FILLER              PIC X(7).                       SQ1164.2
012500           07 XRECORD-NUMBER       PIC 9(6).                      SQ1164.2
012600           07 FILLER              PIC X(6).                       SQ1164.2
012700           07 UPDATE-NUMBER       PIC 9(2).                       SQ1164.2
012800           07 FILLER              PIC X(5).                       SQ1164.2
012900           07 ODO-NUMBER          PIC 9(4).                       SQ1164.2
013000           07 FILLER              PIC X(5).                       SQ1164.2
013100           07 XPROGRAM-NAME        PIC X(5).                      SQ1164.2
013200           07 FILLER              PIC X(7).                       SQ1164.2
013300           07 XRECORD-LENGTH       PIC 9(6).                      SQ1164.2
013400           07 FILLER              PIC X(7).                       SQ1164.2
013500           07 CHARS-OR-RECORDS    PIC X(2).                       SQ1164.2
013600           07 FILLER              PIC X(1).                       SQ1164.2
013700           07 XBLOCK-SIZE          PIC 9(4).                      SQ1164.2
013800           07 FILLER              PIC X(6).                       SQ1164.2
013900           07 RECORDS-IN-FILE     PIC 9(6).                       SQ1164.2
014000           07 FILLER              PIC X(5).                       SQ1164.2
014100           07 XFILE-ORGANIZATION   PIC X(2).                      SQ1164.2
014200           07 FILLER              PIC X(6).                       SQ1164.2
014300           07 XLABEL-TYPE          PIC X(1).                      SQ1164.2
014400        05 FILE-RECORD-INFO-P121-240.                             SQ1164.2
014500           07 FILLER              PIC X(8).                       SQ1164.2
014600           07 XRECORD-KEY          PIC X(29).                     SQ1164.2
014700           07 FILLER              PIC X(9).                       SQ1164.2
014800           07 ALTERNATE-KEY1      PIC X(29).                      SQ1164.2
014900           07 FILLER              PIC X(9).                       SQ1164.2
015000           07 ALTERNATE-KEY2      PIC X(29).                      SQ1164.2
015100           07 FILLER              PIC X(7).                       SQ1164.2
015200 01  TEST-RESULTS.                                                SQ1164.2
015300     02 FILLER                    PICTURE X VALUE SPACE.          SQ1164.2
015400     02 FEATURE                   PICTURE X(20) VALUE SPACE.      SQ1164.2
015500     02 FILLER                    PICTURE X VALUE SPACE.          SQ1164.2
015600     02 P-OR-F                    PICTURE X(5) VALUE SPACE.       SQ1164.2
015700     02 FILLER                    PICTURE X  VALUE SPACE.         SQ1164.2
015800     02  PAR-NAME.                                                SQ1164.2
015900       03 FILLER PICTURE X(12) VALUE SPACE.                       SQ1164.2
016000       03  PARDOT-X PICTURE X  VALUE SPACE.                       SQ1164.2
016100       03 DOTVALUE PICTURE 99  VALUE ZERO.                        SQ1164.2
016200       03 FILLER PIC X(5) VALUE SPACE.                            SQ1164.2
016300     02 FILLER PIC X(10) VALUE SPACE.                             SQ1164.2
016400     02 RE-MARK PIC X(61).                                        SQ1164.2
016500 01  TEST-COMPUTED.                                               SQ1164.2
016600     02 FILLER PIC X(30) VALUE SPACE.                             SQ1164.2
016700     02 FILLER PIC X(17) VALUE "       COMPUTED=".                SQ1164.2
016800     02 COMPUTED-X.                                               SQ1164.2
016900     03 COMPUTED-A                PICTURE X(20) VALUE SPACE.      SQ1164.2
017000     03 COMPUTED-N REDEFINES COMPUTED-A PICTURE -9(9).9(9).       SQ1164.2
017100     03 COMPUTED-0V18 REDEFINES COMPUTED-A  PICTURE -.9(18).      SQ1164.2
017200     03 COMPUTED-4V14 REDEFINES COMPUTED-A  PICTURE -9(4).9(14).  SQ1164.2
017300     03 COMPUTED-14V4 REDEFINES COMPUTED-A  PICTURE -9(14).9(4).  SQ1164.2
017400     03       CM-18V0 REDEFINES COMPUTED-A.                       SQ1164.2
017500         04 COMPUTED-18V0                   PICTURE -9(18).       SQ1164.2
017600         04 FILLER                          PICTURE X.            SQ1164.2
017700     03 FILLER PIC X(50) VALUE SPACE.                             SQ1164.2
017800 01  TEST-CORRECT.                                                SQ1164.2
017900     02 FILLER PIC X(30) VALUE SPACE.                             SQ1164.2
018000     02 FILLER PIC X(17) VALUE "       CORRECT =".                SQ1164.2
018100     02 CORRECT-X.                                                SQ1164.2
018200     03 CORRECT-A                 PICTURE X(20) VALUE SPACE.      SQ1164.2
018300     03 CORRECT-N REDEFINES CORRECT-A PICTURE -9(9).9(9).         SQ1164.2
018400     03 CORRECT-0V18 REDEFINES CORRECT-A    PICTURE -.9(18).      SQ1164.2
018500     03 CORRECT-4V14 REDEFINES CORRECT-A    PICTURE -9(4).9(14).  SQ1164.2
018600     03 CORRECT-14V4 REDEFINES CORRECT-A    PICTURE -9(14).9(4).  SQ1164.2
018700     03      CR-18V0 REDEFINES CORRECT-A.                         SQ1164.2
018800         04 CORRECT-18V0                    PICTURE -9(18).       SQ1164.2
018900         04 FILLER                          PICTURE X.            SQ1164.2
019000     03 FILLER PIC X(50) VALUE SPACE.                             SQ1164.2
019100 01  CCVS-C-1.                                                    SQ1164.2
019200     02 FILLER PICTURE IS X(99) VALUE IS " FEATURE              PASQ1164.2
019300-    "SS  PARAGRAPH-NAME                                          SQ1164.2
019400-    "        REMARKS".                                           SQ1164.2
019500     02 FILLER PICTURE IS X(20) VALUE IS SPACE.                   SQ1164.2
019600 01  CCVS-C-2.                                                    SQ1164.2
019700     02 FILLER PICTURE IS X VALUE IS SPACE.                       SQ1164.2
019800     02 FILLER PICTURE IS X(6) VALUE IS "TESTED".                 SQ1164.2
019900     02 FILLER PICTURE IS X(15) VALUE IS SPACE.                   SQ1164.2
020000     02 FILLER PICTURE IS X(4) VALUE IS "FAIL".                   SQ1164.2
020100     02 FILLER PICTURE IS X(94) VALUE IS SPACE.                   SQ1164.2
020200 01  REC-SKL-SUB PICTURE 9(2) VALUE ZERO.                         SQ1164.2
020300 01  REC-CT PICTURE 99 VALUE ZERO.                                SQ1164.2
020400 01  DELETE-CNT                   PICTURE 999  VALUE ZERO.        SQ1164.2
020500 01  ERROR-COUNTER PICTURE IS 999 VALUE IS ZERO.                  SQ1164.2
020600 01  INSPECT-COUNTER PIC 999 VALUE ZERO.                          SQ1164.2
020700 01  PASS-COUNTER PIC 999 VALUE ZERO.                             SQ1164.2
020800 01  TOTAL-ERROR PIC 999 VALUE ZERO.                              SQ1164.2
020900 01  ERROR-HOLD PIC 999 VALUE ZERO.                               SQ1164.2
021000 01  DUMMY-HOLD PIC X(120) VALUE SPACE.                           SQ1164.2
021100 01  RECORD-COUNT PIC 9(5) VALUE ZERO.                            SQ1164.2
021200 01  CCVS-H-1.                                                    SQ1164.2
021300     02  FILLER   PICTURE X(27)  VALUE SPACE.                     SQ1164.2
021400     02 FILLER PICTURE X(67) VALUE                                SQ1164.2
021500     " FEDERAL SOFTWARE TESTING CENTER COBOL COMPILER VALIDATION  SQ1164.2
021600-    " SYSTEM".                                                   SQ1164.2
021700     02  FILLER     PICTURE X(26)  VALUE SPACE.                   SQ1164.2
021800 01  CCVS-H-2.                                                    SQ1164.2
021900     02 FILLER PICTURE X(52) VALUE IS                             SQ1164.2
022000     "CCVS85 FSTC COPY, NOT FOR DISTRIBUTION.".                   SQ1164.2
022100     02 FILLER PICTURE IS X(19) VALUE IS "TEST RESULTS SET-  ".   SQ1164.2
022200     02 TEST-ID PICTURE IS X(9).                                  SQ1164.2
022300     02 FILLER PICTURE IS X(40) VALUE IS SPACE.                   SQ1164.2
022400 01  CCVS-H-3.                                                    SQ1164.2
022500     02  FILLER PICTURE X(34) VALUE                               SQ1164.2
022600     " FOR OFFICIAL USE ONLY    ".                                SQ1164.2
022700     02  FILLER PICTURE X(58) VALUE                               SQ1164.2
022800     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ1164.2
022900     02  FILLER PICTURE X(28) VALUE                               SQ1164.2
023000     "  COPYRIGHT   1985 ".                                       SQ1164.2
023100 01  CCVS-E-1.                                                    SQ1164.2
023200     02 FILLER PICTURE IS X(52) VALUE IS SPACE.                   SQ1164.2
023300     02 FILLER PICTURE IS X(14) VALUE IS "END OF TEST-  ".        SQ1164.2
023400     02 ID-AGAIN PICTURE IS X(9).                                 SQ1164.2
023500     02 FILLER PICTURE X(45) VALUE IS                             SQ1164.2
023600     " NTIS DISTRIBUTION COBOL 85".                               SQ1164.2
023700 01  CCVS-E-2.                                                    SQ1164.2
023800     02  FILLER                   PICTURE X(31)  VALUE            SQ1164.2
023900     SPACE.                                                       SQ1164.2
024000     02  FILLER                   PICTURE X(21)  VALUE SPACE.     SQ1164.2
024100     02 CCVS-E-2-2.                                               SQ1164.2
024200         03 ERROR-TOTAL PICTURE IS XXX VALUE IS SPACE.            SQ1164.2
024300         03 FILLER PICTURE IS X VALUE IS SPACE.                   SQ1164.2
024400         03 ENDER-DESC PIC X(46) VALUE "ERRORS ENCOUNTERED".      SQ1164.2
024500 01  CCVS-E-3.                                                    SQ1164.2
024600     02  FILLER PICTURE X(22) VALUE                               SQ1164.2
024700     " FOR OFFICIAL USE ONLY".                                    SQ1164.2
024800     02  FILLER PICTURE X(12) VALUE SPACE.                        SQ1164.2
024900     02  FILLER PICTURE X(58) VALUE                               SQ1164.2
025000     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1164.2
025100     02  FILLER PICTURE X(13) VALUE SPACE.                        SQ1164.2
025200     02 FILLER PIC X(15) VALUE " COPYRIGHT 1985".                 SQ1164.2
025300 01  CCVS-E-4.                                                    SQ1164.2
025400     02 CCVS-E-4-1 PIC XXX VALUE SPACE.                           SQ1164.2
025500     02 FILLER PIC XXXX VALUE " OF ".                             SQ1164.2
025600     02 CCVS-E-4-2 PIC XXX VALUE SPACE.                           SQ1164.2
025700     02 FILLER PIC X(40) VALUE                                    SQ1164.2
025800      "  TESTS WERE EXECUTED SUCCESSFULLY".                       SQ1164.2
025900 01  XXINFO.                                                      SQ1164.2
026000     02 FILLER PIC X(30) VALUE "        *** INFORMATION  ***".    SQ1164.2
026100     02 INFO-TEXT.                                                SQ1164.2
026200     04 FILLER PIC X(20) VALUE SPACE.                             SQ1164.2
026300     04 XXCOMPUTED PIC X(20).                                     SQ1164.2
026400     04 FILLER PIC X(5) VALUE SPACE.                              SQ1164.2
026500     04 XXCORRECT PIC X(20).                                      SQ1164.2
026600 01  HYPHEN-LINE.                                                 SQ1164.2
026700     02 FILLER PICTURE IS X VALUE IS SPACE.                       SQ1164.2
026800     02 FILLER PICTURE IS X(65) VALUE IS "************************SQ1164.2
026900-    "*****************************************".                 SQ1164.2
027000     02 FILLER PICTURE IS X(54) VALUE IS "************************SQ1164.2
027100-    "******************************".                            SQ1164.2
027200 01  CCVS-PGM-ID PIC X(6) VALUE                                   SQ1164.2
027300     "SQ116A".                                                    SQ1164.2
027400 PROCEDURE DIVISION.                                              SQ1164.2
027500 CCVS1 SECTION.                                                   SQ1164.2
027600 OPEN-FILES.                                                      SQ1164.2
027700*    OPEN I-O RAW-DATA.                                           SQ1164.2
027800*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            SQ1164.2
027900*    READ RAW-DATA INVALID KEY GO TO END-E-1.                     SQ1164.2
028000*    MOVE "ABORTED " TO C-ABORT.                                  SQ1164.2
028100*    ADD 1 TO C-NO-OF-TESTS.                                      SQ1164.2
028200*    ACCEPT C-DATE  FROM DATE.                                    SQ1164.2
028300*    ACCEPT C-TIME  FROM TIME.                                    SQ1164.2
028400*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-1.             SQ1164.2
028500*END-E-1.                                                         SQ1164.2
028600*    CLOSE RAW-DATA.                                              SQ1164.2
028700     OPEN     OUTPUT PRINT-FILE.                                  SQ1164.2
028800     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   SQ1164.2
028900     MOVE    SPACE TO TEST-RESULTS.                               SQ1164.2
029000     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             SQ1164.2
029100     MOVE ZERO TO REC-SKL-SUB.                                    SQ1164.2
029200     PERFORM CCVS-INIT-FILE 9 TIMES.                              SQ1164.2
029300 CCVS-INIT-FILE.                                                  SQ1164.2
029400     ADD 1 TO REC-SKL-SUB.                                        SQ1164.2
029500     MOVE FILE-RECORD-INFO-SKELETON TO                            SQ1164.2
029600                  FILE-RECORD-INFO (REC-SKL-SUB).                 SQ1164.2
029700 CCVS-INIT-EXIT.                                                  SQ1164.2
029800     GO TO CCVS1-EXIT.                                            SQ1164.2
029900 CLOSE-FILES.                                                     SQ1164.2
030000     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   SQ1164.2
030100*    OPEN I-O RAW-DATA.                                           SQ1164.2
030200*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            SQ1164.2
030300*    READ RAW-DATA INVALID KEY GO TO END-E-2.                     SQ1164.2
030400*    MOVE "OK.     " TO C-ABORT.                                  SQ1164.2
030500*    MOVE PASS-COUNTER TO C-OK.                                   SQ1164.2
030600*    MOVE ERROR-HOLD   TO C-ALL.                                  SQ1164.2
030700*    MOVE ERROR-COUNTER TO C-FAIL.                                SQ1164.2
030800*    MOVE DELETE-CNT TO C-DELETED.                                SQ1164.2
030900*    MOVE INSPECT-COUNTER TO C-INSPECT.                           SQ1164.2
031000*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-2.             SQ1164.2
031100*END-E-2.                                                         SQ1164.2
031200*    CLOSE RAW-DATA.                                              SQ1164.2
031300 TERMINATE-CCVS.                                                  SQ1164.2
031400*    EXIT PROGRAM.                                                SQ1164.2
031500*TERMINATE-CALL.                                                  SQ1164.2
031600     STOP     RUN.                                                SQ1164.2
031700 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         SQ1164.2
031800 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           SQ1164.2
031900 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          SQ1164.2
032000 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-CNT.          SQ1164.2
032100     MOVE "****TEST DELETED****" TO RE-MARK.                      SQ1164.2
032200 PRINT-DETAIL.                                                    SQ1164.2
032300     IF REC-CT NOT EQUAL TO ZERO                                  SQ1164.2
032400             MOVE "." TO PARDOT-X                                 SQ1164.2
032500             MOVE REC-CT TO DOTVALUE.                             SQ1164.2
032600     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      SQ1164.2
032700     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               SQ1164.2
032800        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 SQ1164.2
032900          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 SQ1164.2
033000     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              SQ1164.2
033100     MOVE SPACE TO CORRECT-X.                                     SQ1164.2
033200     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         SQ1164.2
033300     MOVE     SPACE TO RE-MARK.                                   SQ1164.2
033400 HEAD-ROUTINE.                                                    SQ1164.2
033500     MOVE CCVS-H-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ1164.2
033600     MOVE CCVS-H-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.   SQ1164.2
033700     MOVE CCVS-H-3 TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.   SQ1164.2
033800 COLUMN-NAMES-ROUTINE.                                            SQ1164.2
033900     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ1164.2
034000     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ1164.2
034100     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ1164.2
034200 END-ROUTINE.                                                     SQ1164.2
034300     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.SQ1164.2
034400 END-RTN-EXIT.                                                    SQ1164.2
034500     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ1164.2
034600 END-ROUTINE-1.                                                   SQ1164.2
034700      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      SQ1164.2
034800      ERROR-HOLD. ADD DELETE-CNT TO ERROR-HOLD.                   SQ1164.2
034900      ADD PASS-COUNTER TO ERROR-HOLD.                             SQ1164.2
035000*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   SQ1164.2
035100      MOVE PASS-COUNTER TO CCVS-E-4-1.                            SQ1164.2
035200      MOVE ERROR-HOLD TO CCVS-E-4-2.                              SQ1164.2
035300      MOVE CCVS-E-4 TO CCVS-E-2-2.                                SQ1164.2
035400      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           SQ1164.2
035500  END-ROUTINE-12.                                                 SQ1164.2
035600      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        SQ1164.2
035700     IF       ERROR-COUNTER IS EQUAL TO ZERO                      SQ1164.2
035800         MOVE "NO " TO ERROR-TOTAL                                SQ1164.2
035900         ELSE                                                     SQ1164.2
036000         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       SQ1164.2
036100     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           SQ1164.2
036200     PERFORM WRITE-LINE.                                          SQ1164.2
036300 END-ROUTINE-13.                                                  SQ1164.2
036400     IF DELETE-CNT IS EQUAL TO ZERO                               SQ1164.2
036500         MOVE "NO " TO ERROR-TOTAL  ELSE                          SQ1164.2
036600         MOVE DELETE-CNT TO ERROR-TOTAL.                          SQ1164.2
036700     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   SQ1164.2
036800     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ1164.2
036900      IF   INSPECT-COUNTER EQUAL TO ZERO                          SQ1164.2
037000          MOVE "NO " TO ERROR-TOTAL                               SQ1164.2
037100      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   SQ1164.2
037200      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            SQ1164.2
037300      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          SQ1164.2
037400     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ1164.2
037500 WRITE-LINE.                                                      SQ1164.2
037600     ADD 1 TO RECORD-COUNT.                                       SQ1164.2
037700*    IF RECORD-COUNT GREATER 50                                   SQ1164.2
037800*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          SQ1164.2
037900*        MOVE SPACE TO DUMMY-RECORD                               SQ1164.2
038000*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  SQ1164.2
038100*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             SQ1164.2
038200*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     SQ1164.2
038300*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          SQ1164.2
038400*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          SQ1164.2
038500*        MOVE ZERO TO RECORD-COUNT.                               SQ1164.2
038600     PERFORM WRT-LN.                                              SQ1164.2
038700 WRT-LN.                                                          SQ1164.2
038800     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               SQ1164.2
038900     MOVE SPACE TO DUMMY-RECORD.                                  SQ1164.2
039000 BLANK-LINE-PRINT.                                                SQ1164.2
039100     PERFORM WRT-LN.                                              SQ1164.2
039200 FAIL-ROUTINE.                                                    SQ1164.2
039300     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.   SQ1164.2
039400     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.    SQ1164.2
039500     MOVE "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.    SQ1164.2
039600     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     SQ1164.2
039700     GO TO FAIL-ROUTINE-EX.                                       SQ1164.2
039800 FAIL-ROUTINE-WRITE.                                              SQ1164.2
039900     MOVE TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE           SQ1164.2
040000     MOVE TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES.   SQ1164.2
040100 FAIL-ROUTINE-EX. EXIT.                                           SQ1164.2
040200 BAIL-OUT.                                                        SQ1164.2
040300     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.       SQ1164.2
040400     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.               SQ1164.2
040500 BAIL-OUT-WRITE.                                                  SQ1164.2
040600     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  SQ1164.2
040700     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     SQ1164.2
040800 BAIL-OUT-EX. EXIT.                                               SQ1164.2
040900 CCVS1-EXIT.                                                      SQ1164.2
041000     EXIT.                                                        SQ1164.2
041100 SECT-SQ116A-0001 SECTION.                                        SQ1164.2
041200 SEQ-INIT-023.                                                    SQ1164.2
041300     MOVE "SQ-FS6" TO XFILE-NAME (1).                             SQ1164.2
041400     MOVE "R1-F-G" TO XRECORD-NAME (1).                           SQ1164.2
041500     MOVE CCVS-PGM-ID TO XPROGRAM-NAME (1).                       SQ1164.2
041600     MOVE 130 TO XRECORD-LENGTH (1).                              SQ1164.2
041700     MOVE "RC" TO CHARS-OR-RECORDS (1).                           SQ1164.2
041800     MOVE 0001 TO XBLOCK-SIZE (1).                                SQ1164.2
041900     MOVE 000550 TO RECORDS-IN-FILE (1).                          SQ1164.2
042000     MOVE "SQ" TO XFILE-ORGANIZATION (1).                         SQ1164.2
042100     MOVE "O" TO XLABEL-TYPE (1).                                 SQ1164.2
042200     MOVE "FIRST " TO UPDATE-AREA-ONLY.                           SQ1164.2
042300     MOVE ZERO TO COUNT-OF-RECS.                                  SQ1164.2
042400     OPEN OUTPUT SQ-FS6.                                          SQ1164.2
042500 SEQ-TEST-023.                                                    SQ1164.2
042600     ADD 1 TO COUNT-OF-RECS.                                      SQ1164.2
042700     MOVE COUNT-OF-RECS TO XRECORD-NUMBER (1).                    SQ1164.2
042800     MOVE COUNT-OF-RECS TO NUMBER-AREA.                           SQ1164.2
042900     MOVE FILE-RECORD-INFO-P1-120 (1) TO SQ-FS6R1-PART1.          SQ1164.2
043000     MOVE END-OF-RECORD-AREA TO SQ-FS6R1-PART2.                   SQ1164.2
043100     WRITE SQ-FS6R1-F-G-130.                                      SQ1164.2
043200     IF COUNT-OF-RECS EQUAL TO 550                                SQ1164.2
043300          GO TO SEQ-WRITE-023.                                    SQ1164.2
043400     GO TO SEQ-TEST-023.                                          SQ1164.2
043500 SEQ-WRITE-023.                                                   SQ1164.2
043600     MOVE "CREATE FILE SQ-FS6" TO FEATURE.                        SQ1164.2
043700     MOVE "SEQ-WRITE-023" TO PAR-NAME.                            SQ1164.2
043800     MOVE "FILE CREATED, RECS =" TO COMPUTED-A.                   SQ1164.2
043900     MOVE COUNT-OF-RECS TO CORRECT-18V0.                          SQ1164.2
044000     PERFORM PRINT-DETAIL.                                        SQ1164.2
044100     CLOSE SQ-FS6.                                                SQ1164.2
044200*        A SEQUENTIAL MASS STORAGE FILE WITH 130 CHARACTER        SQ1164.2
044300*    RECORDS HAS BEEN CREATED.  THE FILE CONTAINS 550 RECORDS.    SQ1164.2
044400 SEQ-INIT-024.                                                    SQ1164.2
044500     MOVE ZERO TO COUNT-OF-RECS.                                  SQ1164.2
044600*        THIS TEST VERIFIES THE FILE CREATED IN SEQ-TEST-023.     SQ1164.2
044700     OPEN INPUT SQ-FS6.                                           SQ1164.2
044800 SEQ-TEST-024.                                                    SQ1164.2
044900     READ SQ-FS6 AT END GO TO SEQ-TEST-024-1.                     SQ1164.2
045000     ADD 1 TO COUNT-OF-RECS.                                      SQ1164.2
045100     IF COUNT-OF-RECS GREATER THAN 550                            SQ1164.2
045200         MOVE "MORE THAN 550 RECORDS" TO RE-MARK                  SQ1164.2
045300         GO TO SEQ-FAIL-024.                                      SQ1164.2
045400     MOVE SQ-FS6R1-PART1 TO FILE-RECORD-INFO-P1-120 (1).          SQ1164.2
045500     MOVE SQ-FS6R1-PART2 TO END-OF-RECORD-AREA.                   SQ1164.2
045600     IF UPDATE-AREA-ONLY NOT EQUAL TO "FIRST "                    SQ1164.2
045700        GO TO SEQ-FAIL-024-1.                                     SQ1164.2
045800     IF NUMBER-AREA NOT EQUAL TO COUNT-OF-RECS                    SQ1164.2
045900        GO TO SEQ-FAIL-024-1.                                     SQ1164.2
046000     IF XRECORD-NUMBER (1) NOT EQUAL TO COUNT-OF-RECS             SQ1164.2
046100        GO TO SEQ-FAIL-024-1.                                     SQ1164.2
046200     IF XFILE-NAME (1) NOT EQUAL TO "SQ-FS6"                      SQ1164.2
046300        GO TO SEQ-FAIL-024-1.                                     SQ1164.2
046400     IF UPDATE-NUMBER (1) NOT EQUAL TO ZERO                       SQ1164.2
046500        GO TO SEQ-FAIL-024-1.                                     SQ1164.2
046600     IF XLABEL-TYPE (1) NOT EQUAL TO "O"                          SQ1164.2
046700        GO TO SEQ-FAIL-024-1.                                     SQ1164.2
046800     GO TO SEQ-TEST-024.                                          SQ1164.2
046900 SEQ-FAIL-024-1.                                                  SQ1164.2
047000     ADD 1 TO RECORDS-IN-ERROR.                                   SQ1164.2
047100     GO TO SEQ-TEST-024.                                          SQ1164.2
047200 SEQ-TEST-024-1.                                                  SQ1164.2
047300     IF RECORDS-IN-ERROR EQUAL TO 0                               SQ1164.2
047400          GO TO SEQ-PASS-024.                                     SQ1164.2
047500     MOVE "ERRORS IN READING SQ-FS6" TO RE-MARK.                  SQ1164.2
047600 SEQ-FAIL-024.                                                    SQ1164.2
047700     MOVE "RECORDS IN ERROR =" TO COMPUTED-A.                     SQ1164.2
047800     MOVE RECORDS-IN-ERROR TO CORRECT-18V0.                       SQ1164.2
047900     PERFORM FAIL.                                                SQ1164.2
048000     GO TO SEQ-WRITE-024.                                         SQ1164.2
048100 SEQ-PASS-024.                                                    SQ1164.2
048200     PERFORM PASS.                                                SQ1164.2
048300     MOVE "FILE VERIFIED RECS =" TO COMPUTED-A.                   SQ1164.2
048400     MOVE COUNT-OF-RECS TO CORRECT-18V0.                          SQ1164.2
048500 SEQ-WRITE-024.                                                   SQ1164.2
048600     MOVE "SEQ-TEST-024" TO PAR-NAME.                             SQ1164.2
048700     MOVE "VERIFY FILE SQ-FS6" TO FEATURE.                        SQ1164.2
048800     PERFORM PRINT-DETAIL.                                        SQ1164.2
048900 SEQ-CLOSE-024.                                                   SQ1164.2
049000     CLOSE SQ-FS6.                                                SQ1164.2
049100 REWRITE-INIT-GF-01.                                              SQ1164.2
049200     OPEN I-O SQ-FS6.                                             SQ1164.2
049300     MOVE 0 TO COUNT-OF-RECS.                                     SQ1164.2
049400     MOVE 0 TO EOF-FLAG.                                          SQ1164.2
049500     MOVE 0 TO ERROR-FLAG.                                        SQ1164.2
049600*       SKIP THE FIRST 30 RECORDS.                                SQ1164.2
049700     PERFORM READ-SQ-FS6 THRU READ-SQ-FS6-EXIT 30 TIMES.          SQ1164.2
049800     IF EOF-FLAG EQUAL TO 1                                       SQ1164.2
049900         GO TO CANT-TEST.                                         SQ1164.2
050000     GO TO REWRITE-TEST-GF-01.                                    SQ1164.2
050100 READ-SQ-FS6.                                                     SQ1164.2
050200     IF EOF-FLAG EQUAL TO 1                                       SQ1164.2
050300         GO TO READ-SQ-FS6-EXIT.                                  SQ1164.2
050400     READ SQ-FS6 AT END                                           SQ1164.2
050500          MOVE 1 TO EOF-FLAG.                                     SQ1164.2
050600     ADD 1 TO COUNT-OF-RECS.                                      SQ1164.2
050700 READ-SQ-FS6-EXIT.                                                SQ1164.2
050800     EXIT.                                                        SQ1164.2
050900 REWRITE-TEST-GF-01.                                              SQ1164.2
051000*        THIS TEST REWRITES RECORDS FROM A WORKING-STORAGE AREA   SQ1164.2
051100*    THE SAME SIZE AS THE FD 01 RECORD AREA.  A CHECK IS MADE TO  SQ1164.2
051200*    ENSURE THAT THE FROM AREA WAS NOT DESTROYED BY THE REWRITE...SQ1164.2
051300*    FROM STATEMENT.                                              SQ1164.2
051400     IF COUNT-OF-RECS EQUAL TO 80                                 SQ1164.2
051500         GO TO REWRITE-TEST-GF-01-1.                              SQ1164.2
051600     READ SQ-FS6 RECORD                                           SQ1164.2
051700           AT END GO TO CANT-TEST.                                SQ1164.2
051800     ADD 1 TO COUNT-OF-RECS.                                      SQ1164.2
051900     MOVE SQ-FS6R1-PART1 TO FILE-RECORD-INFO-P1-120 (1).          SQ1164.2
052000     ADD 1 TO UPDATE-NUMBER (1).                                  SQ1164.2
052100     MOVE FILE-RECORD-INFO-P1-120 (1) TO AREA1-1.                 SQ1164.2
052200     MOVE SQ-FS6R1-PART2 TO AREA1-2.                              SQ1164.2
052300     MOVE "SECOND" TO AREA1-21.                                   SQ1164.2
052400     REWRITE SQ-FS6R1-F-G-130 FROM REWRT-FROM-AREA1.              SQ1164.2
052500     IF AREA1-1 NOT EQUAL TO FILE-RECORD-INFO-P1-120 (1)          SQ1164.2
052600         GO TO REWRITE-FAIL-GF-01-1.                              SQ1164.2
052700     IF AREA1-21 NOT EQUAL TO "SECOND"                            SQ1164.2
052800         GO TO REWRITE-FAIL-GF-01-1.                              SQ1164.2
052900     IF AREA1-22 EQUAL TO COUNT-OF-RECS                           SQ1164.2
053000         GO TO REWRITE-TEST-GF-01.                                SQ1164.2
053100 REWRITE-FAIL-GF-01-1.                                            SQ1164.2
053200     MOVE 1 TO ERROR-FLAG.                                        SQ1164.2
053300     GO TO REWRITE-TEST-GF-01.                                    SQ1164.2
053400 REWRITE-TEST-GF-01-1.                                            SQ1164.2
053500     IF ERROR-FLAG EQUAL TO ZERO                                  SQ1164.2
053600         GO TO REWRITE-PASS-GF-01.                                SQ1164.2
053700 REWRITE-FAIL-GF-01.                                              SQ1164.2
053800     MOVE "FROM AREA CLOBBERED" TO COMPUTED-A.                    SQ1164.2
053900     MOVE "VII-48; 4.5.2                            " TO  RE-MARK.SQ1164.2
054000     PERFORM FAIL.                                                SQ1164.2
054100     GO TO REWRITE-WRITE-GF-01.                                   SQ1164.2
054200 REWRITE-PASS-GF-01.                                              SQ1164.2
054300     PERFORM PASS.                                                SQ1164.2
054400 REWRITE-WRITE-GF-01.                                             SQ1164.2
054500     MOVE "RWRT-TEST-GF-01" TO PAR-NAME.                          SQ1164.2
054600     MOVE "REWRITE...FROM 01 L" TO FEATURE.                       SQ1164.2
054700     PERFORM PRINT-DETAIL.                                        SQ1164.2
054800 REWRITE-INIT-GF-02-A.                                            SQ1164.2
054900*        THIS TEST REWRITES A RECORD FROM A WORKING-STORAGE AREA  SQ1164.2
055000*    LARGER THAN THE FD 01 RECORD AREA.  TRUNCATION SHOULD        SQ1164.2
055100*    OCCUR ON THE RIGHTMOST CHARACTERS.                           SQ1164.2
055200     READ SQ-FS6 RECORD                                           SQ1164.2
055300         AT END GO TO CANT-TEST.                                  SQ1164.2
055400     ADD 1 TO COUNT-OF-RECS.                                      SQ1164.2
055500     MOVE SQ-FS6R1-PART1 TO FILE-RECORD-INFO-P1-120 (1).          SQ1164.2
055600     ADD 1 TO UPDATE-NUMBER (1).                                  SQ1164.2
055700     MOVE FILE-RECORD-INFO-P1-120 (1) TO AREA2-11.                SQ1164.2
055800     MOVE "SECOND" TO AREA2-12.                                   SQ1164.2
055900     MOVE COUNT-OF-RECS TO AREA2-13.                              SQ1164.2
056000     MOVE "JUNK-AREA" TO AREA2-2.                                 SQ1164.2
056100     REWRITE SQ-FS6R1-F-G-130 FROM REWRT-FROM-AREA2.              SQ1164.2
056200     IF COUNT-OF-RECS EQUAL TO 120                                SQ1164.2
056300         GO TO REWRITE-INIT-GF-03-A.                              SQ1164.2
056400     GO TO REWRITE-INIT-GF-02-A.                                  SQ1164.2
056500 REWRITE-INIT-GF-03-A.                                            SQ1164.2
056600*        THIS TEST REWRITES A RECORD FROM AN 87 CHARACTER         SQ1164.2
056700*    WORKING-STORAGE ITEM.  THE REST OF THE 130 CHARACTERS        SQ1164.2
056800*    SHOULD BE SPACE FILLED DURING THE REWRITE STATEMENT.         SQ1164.2
056900     READ SQ-FS6 RECORD                                           SQ1164.2
057000         AT END GO TO CANT-TEST.                                  SQ1164.2
057100     ADD 1 TO COUNT-OF-RECS.                                      SQ1164.2
057200     MOVE SQ-FS6R1-PART1 TO FILE-RECORD-INFO-P1-120 (1).          SQ1164.2
057300     ADD 1 TO UPDATE-NUMBER (1).                                  SQ1164.2
057400     MOVE FILE-RECORD-INFO-P1-120 (1) TO AREA3-1.                 SQ1164.2
057500     MOVE "JUNK-AREA" TO FOLLOWS-AREA3.                           SQ1164.2
057600     REWRITE SQ-FS6R1-F-G-130 FROM RWRT-FROM-AREA3.               SQ1164.2
057700     IF COUNT-OF-RECS EQUAL TO 160                                SQ1164.2
057800         GO TO REWRITE-INIT-GF-04-A.                              SQ1164.2
057900     GO TO REWRITE-INIT-GF-03-A.                                  SQ1164.2
058000 REWRITE-INIT-GF-04-A.                                            SQ1164.2
058100*        THIS TEST REWRITES A RECORD FROM AN 02 LEVEL DATA ITEM.  SQ1164.2
058200     READ SQ-FS6 RECORD                                           SQ1164.2
058300          AT END GO TO CANT-TEST.                                 SQ1164.2
058400     ADD 1 TO COUNT-OF-RECS.                                      SQ1164.2
058500     MOVE SQ-FS6R1-PART1 TO FILE-RECORD-INFO-P1-120 (1).          SQ1164.2
058600     ADD 1 TO UPDATE-NUMBER (1).                                  SQ1164.2
058700     MOVE FILE-RECORD-INFO-P1-120 (1) TO AREA2-11.                SQ1164.2
058800     MOVE "SECOND" TO AREA2-12.                                   SQ1164.2
058900     MOVE COUNT-OF-RECS TO AREA2-13.                              SQ1164.2
059000     MOVE "JUNK-AREA" TO AREA2-2.                                 SQ1164.2
059100     REWRITE SQ-FS6R1-F-G-130 FROM AREA2-1.                       SQ1164.2
059200     IF COUNT-OF-RECS EQUAL TO 200                                SQ1164.2
059300         GO TO REWRITE-INIT-GF-05-A.                              SQ1164.2
059400     GO TO REWRITE-INIT-GF-04-A.                                  SQ1164.2
059500 REWRITE-INIT-GF-05-A.                                            SQ1164.2
059600*        THIS TEST REWRITES A RECORD FROM A SUBSCRIPTED DATA      SQ1164.2
059700*    ITEM OF 120 CHARACTERS.  THE DATA ITEM IS LEVEL 05.          SQ1164.2
059800     READ SQ-FS6 RECORD                                           SQ1164.2
059900         AT END GO TO CANT-TEST.                                  SQ1164.2
060000     ADD 1 TO COUNT-OF-RECS.                                      SQ1164.2
060100     MOVE SQ-FS6R1-PART1 TO FILE-RECORD-INFO-P1-120 (2).          SQ1164.2
060200     ADD 1 TO UPDATE-NUMBER (2).                                  SQ1164.2
060300     MOVE SPACE TO SQ-FS6R1-PART2.                                SQ1164.2
060400     REWRITE SQ-FS6R1-F-G-130 FROM FILE-RECORD-INFO-P1-120 (2).   SQ1164.2
060500     IF COUNT-OF-RECS EQUAL TO 240                                SQ1164.2
060600         GO TO REWRITE-CLOSE-SQ-FS6.                              SQ1164.2
060700     GO TO REWRITE-INIT-GF-05-A.                                  SQ1164.2
060800 REWRITE-CLOSE-SQ-FS6.                                            SQ1164.2
060900     CLOSE SQ-FS6.                                                SQ1164.2
061000     GO TO REWRITE-READ-INIT-GF-02.                               SQ1164.2
061100 CANT-TEST.                                                       SQ1164.2
061200*        THIS PARAGRAPH IS EXECUTED ONLY WHEN AN AT END           SQ1164.2
061300*    CONDITION OCCURRED WHEN TRYING TO READ AND REWRITE           SQ1164.2
061400*    THE FILE SQ-FS6.                                             SQ1164.2
061500     MOVE "UNEXPECTED EOF" TO COMPUTED-A.                         SQ1164.2
061600     MOVE "UNABLE TO UPDATE FILE" TO RE-MARK.                     SQ1164.2
061700     PERFORM PRINT-DETAIL.                                        SQ1164.2
061800     PERFORM BLANK-LINE-PRINT 5 TIMES.                            SQ1164.2
061900     MOVE "**** REWRITE TESTS DELETED ****" TO DUMMY-RECORD.      SQ1164.2
062000     PERFORM WRITE-LINE.                                          SQ1164.2
062100     GO TO SEQ-CLOSE-025.                                         SQ1164.2
062200 REWRITE-READ-INIT-GF-02.                                         SQ1164.2
062300     MOVE 0 TO COUNT-OF-RECS.                                     SQ1164.2
062400     MOVE 0 TO EOF-FLAG.                                          SQ1164.2
062500     MOVE 0 TO ERROR-FLAG.                                        SQ1164.2
062600     MOVE 0 TO RECORDS-IN-ERROR.                                  SQ1164.2
062700     OPEN INPUT SQ-FS6.                                           SQ1164.2
062800 REWRITE-TEST-GF-02.                                              SQ1164.2
062900*        CHECK THE FIRST 30 RECORDS OF THE FILE.                  SQ1164.2
063000*    THESE RECORDS WERE NOT REWRITTEN.                            SQ1164.2
063100     IF COUNT-OF-RECS EQUAL TO 30                                 SQ1164.2
063200         GO TO REWRITE-TEST-GF-02-1.                              SQ1164.2
063300     READ SQ-FS6 RECORD                                           SQ1164.2
063400         AT END MOVE "UNEXPECTED EOF" TO COMPUTED-A               SQ1164.2
063500                MOVE 1 TO EOF-FLAG                                SQ1164.2
063600                GO TO REWRITE-FAIL-GF-02.                         SQ1164.2
063700     ADD 1 TO COUNT-OF-RECS.                                      SQ1164.2
063800     PERFORM CHECK-RECORD THRU CHECK-RECORD-EXIT.                 SQ1164.2
063900     IF ERROR-FLAG EQUAL TO 1                                     SQ1164.2
064000         GO TO REWRITE-TEST-GF-02.                                SQ1164.2
064100     IF UPDATE-NUMBER (1) NOT EQUAL TO 0                          SQ1164.2
064200         PERFORM CHECK-RECORD-FAIL                                SQ1164.2
064300         GO TO REWRITE-TEST-GF-02.                                SQ1164.2
064400     IF UPDATE-AREA-ONLY NOT EQUAL TO "FIRST "                    SQ1164.2
064500         PERFORM CHECK-RECORD-FAIL.                               SQ1164.2
064600     GO TO REWRITE-TEST-GF-02.                                    SQ1164.2
064700 REWRITE-TEST-GF-02-1.                                            SQ1164.2
064800     IF ERROR-FLAG EQUAL TO 0                                     SQ1164.2
064900         GO TO REWRITE-PASS-GF-02.                                SQ1164.2
065000     MOVE "ERRORS IN RECORD(S)" TO COMPUTED-A.                    SQ1164.2
065100 REWRITE-FAIL-GF-02.                                              SQ1164.2
065200     MOVE "VII-48; 4.5.2 RWRT LARGER RECORDS: TRUNC." TO  RE-MARK.SQ1164.2
065300     PERFORM FAIL.                                                SQ1164.2
065400     MOVE "CHECK RECORDS NOT REWRITTEN" TO RE-MARK.               SQ1164.2
065500     GO TO REWRITE-WRITE-GF-02.                                   SQ1164.2
065600 REWRITE-PASS-GF-02.                                              SQ1164.2
065700     PERFORM PASS.                                                SQ1164.2
065800 REWRITE-WRITE-GF-02.                                             SQ1164.2
065900     MOVE "RWRT; LARGER RECORDS  "  TO  FEATURE.                  SQ1164.2
066000     MOVE "RWRT-TEST-GF-02" TO PAR-NAME.                          SQ1164.2
066100     PERFORM PRINT-DETAIL.                                        SQ1164.2
066200     GO TO REWRITE-INIT-GF-03.                                    SQ1164.2
066300 CHECK-RECORD.                                                    SQ1164.2
066400     MOVE SQ-FS6R1-PART1 TO FILE-RECORD-INFO-P1-120 (1).          SQ1164.2
066500     MOVE SQ-FS6R1-PART2 TO END-OF-RECORD-AREA.                   SQ1164.2
066600     IF XFILE-NAME (1) NOT EQUAL TO "SQ-FS6"                      SQ1164.2
066700         GO TO CHECK-RECORD-FAIL.                                 SQ1164.2
066800     IF COUNT-OF-RECS NOT EQUAL TO XRECORD-NUMBER (1)             SQ1164.2
066900         GO TO CHECK-RECORD-FAIL.                                 SQ1164.2
067000     IF XLABEL-TYPE (1) NOT EQUAL TO "O"                          SQ1164.2
067100         GO TO CHECK-RECORD-FAIL.                                 SQ1164.2
067200     IF NUMBER-AREA EQUAL TO COUNT-OF-RECS                        SQ1164.2
067300         GO TO CHECK-RECORD-EXIT.                                 SQ1164.2
067400 CHECK-RECORD-FAIL.                                               SQ1164.2
067500     ADD 1 TO RECORDS-IN-ERROR.                                   SQ1164.2
067600     MOVE 1 TO ERROR-FLAG.                                        SQ1164.2
067700 CHECK-RECORD-EXIT.                                               SQ1164.2
067800     EXIT.                                                        SQ1164.2
067900 REWRITE-INIT-GF-03.                                              SQ1164.2
068000     MOVE 0 TO ERROR-FLAG.                                        SQ1164.2
068100     IF EOF-FLAG EQUAL TO 1                                       SQ1164.2
068200         GO TO SEQ-EOF-025.                                       SQ1164.2
068300*    THIS TEST CHECKS RECORDS 31 THRU 80 WHICH WERE REWRITTEN.    SQ1164.2
068400 REWRITE-TEST-GF-03.                                              SQ1164.2
068500     IF COUNT-OF-RECS EQUAL TO 80                                 SQ1164.2
068600         GO TO REWRITE-TEST-GF-03-1.                              SQ1164.2
068700     READ SQ-FS6 RECORD                                           SQ1164.2
068800         AT END MOVE "UNEXPECTED EOF" TO COMPUTED-A               SQ1164.2
068900                MOVE 1 TO EOF-FLAG                                SQ1164.2
069000                GO TO REWRITE-FAIL-GF-03.                         SQ1164.2
069100     ADD 1 TO COUNT-OF-RECS.                                      SQ1164.2
069200     PERFORM CHECK-RECORD THRU CHECK-RECORD-EXIT.                 SQ1164.2
069300     IF ERROR-FLAG EQUAL TO 1                                     SQ1164.2
069400         GO TO REWRITE-TEST-GF-03.                                SQ1164.2
069500     IF UPDATE-NUMBER (1) NOT EQUAL TO 1                          SQ1164.2
069600         PERFORM CHECK-RECORD-FAIL                                SQ1164.2
069700         GO TO REWRITE-TEST-GF-03.                                SQ1164.2
069800     IF UPDATE-AREA-ONLY NOT EQUAL TO "SECOND"                    SQ1164.2
069900         PERFORM CHECK-RECORD-FAIL.                               SQ1164.2
070000     GO TO REWRITE-TEST-GF-03.                                    SQ1164.2
070100 REWRITE-TEST-GF-03-1.                                            SQ1164.2
070200     IF ERROR-FLAG EQUAL TO 0                                     SQ1164.2
070300         GO TO REWRITE-PASS-GF-03.                                SQ1164.2
070400     MOVE "ERRORS IN RECORD(S)" TO COMPUTED-A.                    SQ1164.2
070500 REWRITE-FAIL-GF-03.                                              SQ1164.2
070600     MOVE "VII-48; 4.5.2  REWRITE OF 130 CHAR RECS  " TO  RE-MARK.SQ1164.2
070700     PERFORM FAIL.                                                SQ1164.2
070800     GO TO REWRITE-WRITE-GF-03.                                   SQ1164.2
070900 REWRITE-PASS-GF-03.                                              SQ1164.2
071000     PERFORM PASS.                                                SQ1164.2
071100 REWRITE-WRITE-GF-03.                                             SQ1164.2
071200     MOVE "RWRT; SHORTER RECORDS  "  TO FEATURE.                  SQ1164.2
071300     MOVE "RWRT-TEST-GF-03"     TO PAR-NAME.                      SQ1164.2
071400     PERFORM PRINT-DETAIL.                                        SQ1164.2
071500 REWRITE-INIT-GF-04.                                              SQ1164.2
071600     MOVE 0 TO REC-CT.                                            SQ1164.2
071700     MOVE 0 TO ERROR-FLAG.                                        SQ1164.2
071800     IF EOF-FLAG EQUAL TO 1                                       SQ1164.2
071900         GO TO SEQ-EOF-025.                                       SQ1164.2
072000*    THIS TEST CHECKS THE RECORDS WHICH WERE REWRITTEN            SQ1164.2
072100*    FROM AN 139 CHARACTER RECORD.                                SQ1164.2
072200 REWRITE-TEST-GF-04.                                              SQ1164.2
072300     IF COUNT-OF-RECS EQUAL TO 120                                SQ1164.2
072400         GO TO REWRITE-TEST-GF-04-1.                              SQ1164.2
072500     READ SQ-FS6 RECORD                                           SQ1164.2
072600         AT END MOVE "UNEXPECTED EOF" TO COMPUTED-A               SQ1164.2
072700                MOVE 1 TO EOF-FLAG                                SQ1164.2
072800                GO TO REWRITE-FAIL-GF-04.                         SQ1164.2
072900     ADD 1 TO COUNT-OF-RECS.                                      SQ1164.2
073000     PERFORM CHECK-RECORD THRU CHECK-RECORD-EXIT.                 SQ1164.2
073100     IF ERROR-FLAG EQUAL TO 1                                     SQ1164.2
073200         GO TO REWRITE-TEST-GF-04.                                SQ1164.2
073300     IF UPDATE-NUMBER (1) NOT EQUAL TO 1                          SQ1164.2
073400         PERFORM CHECK-RECORD-FAIL                                SQ1164.2
073500         GO TO REWRITE-TEST-GF-04.                                SQ1164.2
073600     IF UPDATE-AREA-ONLY NOT EQUAL TO "SECOND"                    SQ1164.2
073700         PERFORM CHECK-RECORD-FAIL                                SQ1164.2
073800         GO TO REWRITE-TEST-GF-04.                                SQ1164.2
073900     MOVE SPACE TO AREA2-2.                                       SQ1164.2
074000     MOVE SQ-FS6R1-F-G-130 TO REWRT-FROM-AREA2.                   SQ1164.2
074100     IF AREA2-2 NOT EQUAL TO SPACE                                SQ1164.2
074200         MOVE "NO RECORD TRUNCATION" TO RE-MARK                   SQ1164.2
074300         PERFORM CHECK-RECORD-FAIL.                               SQ1164.2
074400     GO TO REWRITE-TEST-GF-04.                                    SQ1164.2
074500 REWRITE-TEST-GF-04-1.                                            SQ1164.2
074600     IF ERROR-FLAG EQUAL TO 0                                     SQ1164.2
074700         GO TO REWRITE-PASS-GF-04.                                SQ1164.2
074800     MOVE "ERRORS IN RECORD(S)" TO COMPUTED-A.                    SQ1164.2
074900 REWRITE-FAIL-GF-04.                                              SQ1164.2
075000     MOVE "VII-48; 4.5.2  RWRT FROM 139 CHAR REC    " TO  RE-MARK.SQ1164.2
075100     PERFORM FAIL.                                                SQ1164.2
075200     GO TO REWRITE-WRITE-GF-04.                                   SQ1164.2
075300 REWRITE-PASS-GF-04.                                              SQ1164.2
075400     PERFORM PASS.                                                SQ1164.2
075500 REWRITE-WRITE-GF-04.                                             SQ1164.2
075600     MOVE "RWRT FROM 139"              TO FEATURE.                SQ1164.2
075700     MOVE "RWRT-TEST-GF-04" TO PAR-NAME.                          SQ1164.2
075800     PERFORM PRINT-DETAIL.                                        SQ1164.2
075900 REWRITE-INIT-GF-05.                                              SQ1164.2
076000     IF EOF-FLAG EQUAL TO 1                                       SQ1164.2
076100         GO TO SEQ-EOF-025.                                       SQ1164.2
076200     MOVE 0 TO ERROR-FLAG.                                        SQ1164.2
076300*        THIS TEST CHECKS THE 87 CHARACTER RECORDS WHICH          SQ1164.2
076400*    WERE REWRITTEN.  CHARACTERS 88 THRU 130 SHOULD BE SPACES.    SQ1164.2
076500 REWRITE-TEST-GF-05.                                              SQ1164.2
076600     IF COUNT-OF-RECS EQUAL TO 160                                SQ1164.2
076700         GO TO REWRITE-TEST-GF-05-1.                              SQ1164.2
076800     READ SQ-FS6 RECORD                                           SQ1164.2
076900         AT END MOVE "UNEXPECTED EOF" TO COMPUTED-A               SQ1164.2
077000                MOVE 1 TO EOF-FLAG                                SQ1164.2
077100                GO TO REWRITE-FAIL-GF-05.                         SQ1164.2
077200     ADD 1 TO COUNT-OF-RECS.                                      SQ1164.2
077300     MOVE SQ-FS6R1-PART1 TO FILE-RECORD-INFO-P1-120 (1).          SQ1164.2
077400     IF XFILE-NAME (1) NOT EQUAL TO "SQ-FS6"                      SQ1164.2
077500         GO TO REWRITE-FAIL-GF-05-1.                              SQ1164.2
077600     IF COUNT-OF-RECS NOT EQUAL TO XRECORD-NUMBER (1)             SQ1164.2
077700         GO TO REWRITE-FAIL-GF-05-1.                              SQ1164.2
077800     IF UPDATE-NUMBER (1) NOT EQUAL TO 1                          SQ1164.2
077900         GO TO REWRITE-FAIL-GF-05-1.                              SQ1164.2
078000     IF CHARS-OR-RECORDS (1) NOT EQUAL TO SPACE                   SQ1164.2
078100         MOVE "NO SPACE FILL" TO RE-MARK                          SQ1164.2
078200         GO TO REWRITE-FAIL-GF-05-1.                              SQ1164.2
078300     IF SQ-FS6R1-PART2 NOT EQUAL TO SPACE                         SQ1164.2
078400         MOVE "NO SPACE FILL" TO RE-MARK                          SQ1164.2
078500         GO TO REWRITE-FAIL-GF-05-1.                              SQ1164.2
078600     GO TO REWRITE-TEST-GF-05.                                    SQ1164.2
078700 REWRITE-FAIL-GF-05-1.                                            SQ1164.2
078800     PERFORM CHECK-RECORD-FAIL.                                   SQ1164.2
078900     GO TO REWRITE-TEST-GF-05.                                    SQ1164.2
079000 REWRITE-TEST-GF-05-1.                                            SQ1164.2
079100     IF ERROR-FLAG EQUAL TO 0                                     SQ1164.2
079200         GO TO REWRITE-PASS-GF-05.                                SQ1164.2
079300     MOVE "ERRORS IN RECORD(S)" TO COMPUTED-A.                    SQ1164.2
079400 REWRITE-FAIL-GF-05.                                              SQ1164.2
079500     MOVE "VII-48; 4.5.2  CHARS 88 THRU 139: SPACE} " TO  RE-MARK.SQ1164.2
079600     PERFORM FAIL.                                                SQ1164.2
079700     GO TO REWRITE-WRITE-GF-05.                                   SQ1164.2
079800 REWRITE-PASS-GF-05.                                              SQ1164.2
079900     PERFORM PASS.                                                SQ1164.2
080000 REWRITE-WRITE-GF-05.                                             SQ1164.2
080100     MOVE "RWRT SHORTER RECORDS" TO FEATURE.                      SQ1164.2
080200     MOVE "RWRT-TEST-GF-05" TO PAR-NAME.                          SQ1164.2
080300     PERFORM PRINT-DETAIL.                                        SQ1164.2
080400 REWRITE-INIT-GF-06.                                              SQ1164.2
080500     IF EOF-FLAG EQUAL TO 1                                       SQ1164.2
080600         GO TO SEQ-EOF-025.                                       SQ1164.2
080700     MOVE 0 TO ERROR-FLAG.                                        SQ1164.2
080800*        THIS TEST CHECKS THE RECORDS REWRITTEN FROM AN 02        SQ1164.2
080900*    LEVEL ITEM OF 130 CHARACTERS.                                SQ1164.2
081000 REWRITE-TEST-GF-06.                                              SQ1164.2
081100     IF COUNT-OF-RECS EQUAL TO 200                                SQ1164.2
081200         GO TO REWRITE-TEST-GF-06-1.                              SQ1164.2
081300     READ SQ-FS6 RECORD                                           SQ1164.2
081400         AT END MOVE "UNEXPECTED EOF" TO COMPUTED-A               SQ1164.2
081500                MOVE 1 TO EOF-FLAG                                SQ1164.2
081600                GO TO REWRITE-FAIL-GF-06.                         SQ1164.2
081700     ADD 1 TO COUNT-OF-RECS.                                      SQ1164.2
081800     PERFORM CHECK-RECORD THRU CHECK-RECORD-EXIT.                 SQ1164.2
081900     IF ERROR-FLAG EQUAL TO 1                                     SQ1164.2
082000         GO TO REWRITE-TEST-GF-06.                                SQ1164.2
082100     IF UPDATE-NUMBER (1) NOT EQUAL TO 1                          SQ1164.2
082200         PERFORM CHECK-RECORD-FAIL                                SQ1164.2
082300         GO TO REWRITE-TEST-GF-06.                                SQ1164.2
082400     IF UPDATE-AREA-ONLY NOT EQUAL TO "SECOND"                    SQ1164.2
082500         PERFORM CHECK-RECORD-FAIL.                               SQ1164.2
082600     GO TO REWRITE-TEST-GF-06.                                    SQ1164.2
082700 REWRITE-TEST-GF-06-1.                                            SQ1164.2
082800     IF ERROR-FLAG EQUAL TO ZERO                                  SQ1164.2
082900         GO TO REWRITE-PASS-GF-06.                                SQ1164.2
083000     MOVE "ERRORS IN RECORD(S)" TO COMPUTED-A.                    SQ1164.2
083100 REWRITE-FAIL-GF-06.                                              SQ1164.2
083200     MOVE "VII-48; 4.5.2                            " TO  RE-MARK.SQ1164.2
083300     PERFORM FAIL.                                                SQ1164.2
083400     GO TO REWRITE-WRITE-GF-06.                                   SQ1164.2
083500 REWRITE-PASS-GF-06.                                              SQ1164.2
083600     PERFORM PASS.                                                SQ1164.2
083700 REWRITE-WRITE-GF-06.                                             SQ1164.2
083800     MOVE "RWRT FROM 02 LEVEL" TO FEATURE.                        SQ1164.2
083900     MOVE "RWRT-TEST-GF-06" TO PAR-NAME.                          SQ1164.2
084000     PERFORM PRINT-DETAIL.                                        SQ1164.2
084100 REWRITE-INIT-GF-07.                                              SQ1164.2
084200     IF EOF-FLAG EQUAL TO 1                                       SQ1164.2
084300         GO TO SEQ-EOF-025.                                       SQ1164.2
084400     MOVE 0 TO ERROR-FLAG.                                        SQ1164.2
084500*    THIS TEST CHECKS THE RECORDS REWRITTEN FROM AN 05 LEVEL      SQ1164.2
084600*    SUBSCRIPTED ITEM.                                            SQ1164.2
084700 REWRITE-TEST-GF-07.                                              SQ1164.2
084800     IF COUNT-OF-RECS EQUAL TO 240                                SQ1164.2
084900         GO TO REWRITE-TEST-GF-07-1.                              SQ1164.2
085000     READ SQ-FS6 RECORD                                           SQ1164.2
085100         AT END MOVE "UNEXPECTED EOF" TO COMPUTED-A               SQ1164.2
085200         MOVE 1 TO EOF-FLAG                                       SQ1164.2
085300         GO TO REWRITE-FAIL-GF-07.                                SQ1164.2
085400     ADD 1 TO COUNT-OF-RECS.                                      SQ1164.2
085500     MOVE SQ-FS6R1-PART1 TO FILE-RECORD-INFO-P1-120 (1).          SQ1164.2
085600     IF XFILE-NAME (1) NOT EQUAL TO "SQ-FS6"                      SQ1164.2
085700         GO TO REWRITE-FAIL-GF-07-1.                              SQ1164.2
085800     IF XRECORD-NUMBER (1) NOT EQUAL TO COUNT-OF-RECS             SQ1164.2
085900         GO TO REWRITE-FAIL-GF-07-1.                              SQ1164.2
086000     IF UPDATE-NUMBER (1) NOT EQUAL TO 1                          SQ1164.2
086100         GO TO REWRITE-FAIL-GF-07-1.                              SQ1164.2
086200     IF XLABEL-TYPE (1) NOT EQUAL TO "O"                          SQ1164.2
086300         GO TO REWRITE-FAIL-GF-07-1.                              SQ1164.2
086400     IF SQ-FS6R1-PART2 NOT EQUAL TO SPACE                         SQ1164.2
086500         MOVE "NO SPACE FILL" TO RE-MARK                          SQ1164.2
086600         GO TO REWRITE-FAIL-GF-07-1.                              SQ1164.2
086700     GO TO REWRITE-TEST-GF-07.                                    SQ1164.2
086800 REWRITE-FAIL-GF-07-1.                                            SQ1164.2
086900     PERFORM CHECK-RECORD-FAIL.                                   SQ1164.2
087000     GO TO REWRITE-TEST-GF-07.                                    SQ1164.2
087100 REWRITE-TEST-GF-07-1.                                            SQ1164.2
087200     IF ERROR-FLAG EQUAL TO ZERO                                  SQ1164.2
087300         GO TO REWRITE-PASS-GF-07.                                SQ1164.2
087400     MOVE "ERRORS IN RECORD(S)" TO COMPUTED-A.                    SQ1164.2
087500 REWRITE-FAIL-GF-07.                                              SQ1164.2
087600     MOVE "VII-48; 4.5.2                            " TO  RE-MARK.SQ1164.2
087700     PERFORM FAIL.                                                SQ1164.2
087800     GO TO REWRITE-WRITE-GF-07.                                   SQ1164.2
087900 REWRITE-PASS-GF-07.                                              SQ1164.2
088000     PERFORM PASS.                                                SQ1164.2
088100 REWRITE-WRITE-GF-07.                                             SQ1164.2
088200     MOVE "RWRT FROM 05 LEVEL" TO FEATURE.                        SQ1164.2
088300     MOVE "RWRT-TEST-GF-07" TO PAR-NAME.                          SQ1164.2
088400     PERFORM PRINT-DETAIL.                                        SQ1164.2
088500 REWRITE-INIT-GF-08.                                              SQ1164.2
088600     IF EOF-FLAG EQUAL TO 1                                       SQ1164.2
088700         GO TO SEQ-EOF-025.                                       SQ1164.2
088800     MOVE 0 TO ERROR-FLAG.                                        SQ1164.2
088900*    THIS TEST CHECKS RECORDS 241 THRU 550 WHICH WERE NOT         SQ1164.2
089000*    REWRITTEN.                                                   SQ1164.2
089100 REWRITE-TEST-GF-08.                                              SQ1164.2
089200     IF COUNT-OF-RECS EQUAL TO 550                                SQ1164.2
089300         GO TO REWRITE-TEST-GF-08-1.                              SQ1164.2
089400     READ SQ-FS6 RECORD                                           SQ1164.2
089500         AT END MOVE "UNEXPECTED EOF" TO COMPUTED-A               SQ1164.2
089600                MOVE 1 TO EOF-FLAG                                SQ1164.2
089700                GO TO REWRITE-FAIL-GF-08.                         SQ1164.2
089800     ADD 1 TO COUNT-OF-RECS.                                      SQ1164.2
089900     PERFORM CHECK-RECORD THRU CHECK-RECORD-EXIT.                 SQ1164.2
090000     IF ERROR-FLAG EQUAL TO 1                                     SQ1164.2
090100         GO TO REWRITE-TEST-GF-08.                                SQ1164.2
090200     IF UPDATE-NUMBER (1) NOT EQUAL TO 0                          SQ1164.2
090300         PERFORM CHECK-RECORD-FAIL                                SQ1164.2
090400         GO TO REWRITE-TEST-GF-08.                                SQ1164.2
090500     IF UPDATE-AREA-ONLY NOT EQUAL TO "FIRST "                    SQ1164.2
090600         PERFORM CHECK-RECORD-FAIL.                               SQ1164.2
090700     GO TO REWRITE-TEST-GF-08.                                    SQ1164.2
090800 REWRITE-TEST-GF-08-1.                                            SQ1164.2
090900     IF ERROR-FLAG EQUAL TO 0                                     SQ1164.2
091000         GO TO REWRITE-PASS-GF-08.                                SQ1164.2
091100     MOVE "ERRORS IN RECORD(S)" TO COMPUTED-A.                    SQ1164.2
091200 REWRITE-FAIL-GF-08.                                              SQ1164.2
091300     MOVE "VII-48; 4.5.2                            " TO  RE-MARK.SQ1164.2
091400     PERFORM FAIL.                                                SQ1164.2
091500     GO TO REWRITE-WRITE-GF-08.                                   SQ1164.2
091600 REWRITE-PASS-GF-08.                                              SQ1164.2
091700     PERFORM PASS.                                                SQ1164.2
091800 REWRITE-WRITE-GF-08.                                             SQ1164.2
091900     MOVE "RWRT-TEST-GF-08" TO PAR-NAME.                          SQ1164.2
092000     MOVE "RECORD NOT REWRITTEN" TO FEATURE.                      SQ1164.2
092100     PERFORM PRINT-DETAIL.                                        SQ1164.2
092200 SEQ-INIT-025.                                                    SQ1164.2
092300*        THIS TEST CHECKS IF THERE WERE ANY ERRORS IN THE         SQ1164.2
092400*    UPDATED FILE AND READS THE FILE ONCE MORE EXPECTING          SQ1164.2
092500*    THE AT END CONDITION TO OCCUR.                               SQ1164.2
092600     IF EOF-FLAG EQUAL TO 1                                       SQ1164.2
092700         GO TO SEQ-EOF-025.                                       SQ1164.2
092800 SEQ-TEST-025.                                                    SQ1164.2
092900     READ SQ-FS6 RECORD                                           SQ1164.2
093000         AT END GO TO SEQ-TEST-25-1.                              SQ1164.2
093100     MOVE "MORE THAN 550 RECORDS" TO RE-MARK.                     SQ1164.2
093200     GO TO SEQ-FAIL-025.                                          SQ1164.2
093300 SEQ-TEST-25-1.                                                   SQ1164.2
093400     IF RECORDS-IN-ERROR NOT EQUAL TO 0                           SQ1164.2
093500         MOVE "RECORDS IN ERROR =" TO COMPUTED-A                  SQ1164.2
093600         MOVE RECORDS-IN-ERROR TO CORRECT-18V0                    SQ1164.2
093700         GO TO SEQ-FAIL-025.                                      SQ1164.2
093800 SEQ-PASS-025.                                                    SQ1164.2
093900     PERFORM PASS.                                                SQ1164.2
094000     GO TO SEQ-WRITE-025.                                         SQ1164.2
094100 SEQ-EOF-025.                                                     SQ1164.2
094200     MOVE "LESS THAN 550 RECORDS" TO RE-MARK.                     SQ1164.2
094300     MOVE "RECORDS READ =" TO COMPUTED-A.                         SQ1164.2
094400     MOVE COUNT-OF-RECS TO CORRECT-18V0.                          SQ1164.2
094500 SEQ-FAIL-025.                                                    SQ1164.2
094600     PERFORM FAIL.                                                SQ1164.2
094700 SEQ-WRITE-025.                                                   SQ1164.2
094800     MOVE "SEQ-TEST-025" TO PAR-NAME.                             SQ1164.2
094900     MOVE "READ LAST RECORD" TO FEATURE.                          SQ1164.2
095000     PERFORM PRINT-DETAIL.                                        SQ1164.2
095100 SEQ-CLOSE-025.                                                   SQ1164.2
095200     CLOSE SQ-FS6.                                                SQ1164.2
095300 TERMINATE-ROUTINE.                                               SQ1164.2
095400     EXIT.                                                        SQ1164.2
095500 CCVS-EXIT SECTION.                                               SQ1164.2
095600 CCVS-999999.                                                     SQ1164.2
095700     GO TO CLOSE-FILES.                                           SQ1164.2
      *END-OF,SQ116A                                                                  
