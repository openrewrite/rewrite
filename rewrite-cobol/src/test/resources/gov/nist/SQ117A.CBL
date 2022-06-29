      *HEADER,COBOL,SQ117A                                                            
000100 IDENTIFICATION DIVISION.                                         SQ1174.2
000200 PROGRAM-ID.                                                      SQ1174.2
000300     SQ117A.                                                      SQ1174.2
000400****************************************************************  SQ1174.2
000500*                                                              *  SQ1174.2
000600*    VALIDATION FOR:-                                          *  SQ1174.2
000700*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1174.2
000800*    USING CCVS85 VERSION 1.0 ISSUED IN JANUARY 1986.          *  SQ1174.2
000900*                                                              *  SQ1174.2
001000*    CREATION DATE     /     VALIDATION DATE                   *  SQ1174.2
001100*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ1174.2
001200*                                                              *  SQ1174.2
001300****************************************************************  SQ1174.2
001400                                                                  SQ1174.2
001500*        THIS ROUTINE CREATES A SEQUENTIAL MASS STORAGE FILE      SQ1174.2
001600*    USING WRITE...FROM STATEMENTS.  THE FILE IS READ AND FIELDS  SQ1174.2
001700*    IN THE RECORDS ARE CHECKED TO ENSURE THAT TRUNCATION AND     SQ1174.2
001800*    BLANK FILLING OF THE RECORD OCCURS WHEN REQUIRED.            SQ1174.2
001900*                                                                 SQ1174.2
002000*    USED X-CARDS:                                                SQ1174.2
002100*         XXXXX014                                                SQ1174.2
002200*         XXXXX055                                                SQ1174.2
002300*     P   XXXXX062                                                SQ1174.2
002400*         XXXXX082                                                SQ1174.2
002500*         XXXXX083                                                SQ1174.2
002600*     C   XXXXX084                                                SQ1174.2
002700*                                                                 SQ1174.2
002800*                                                                 SQ1174.2
002900 ENVIRONMENT DIVISION.                                            SQ1174.2
003000 CONFIGURATION SECTION.                                           SQ1174.2
003100 SOURCE-COMPUTER.                                                 SQ1174.2
003200     XXXXX082.                                                    SQ1174.2
003300 OBJECT-COMPUTER.                                                 SQ1174.2
003400     XXXXX083.                                                    SQ1174.2
003500 INPUT-OUTPUT SECTION.                                            SQ1174.2
003600 FILE-CONTROL.                                                    SQ1174.2
003700*    SELECT RAW-DATA   ASSIGN TO                                  SQ1174.2
003800*    XXXXX062                                                     SQ1174.2
003900*           ORGANIZATION IS INDEXED                               SQ1174.2
004000*           ACCESS MODE IS RANDOM                                 SQ1174.2
004100*           RECORD KEY IS RAW-DATA-KEY.                           SQ1174.2
004200     SELECT PRINT-FILE ASSIGN TO                                  SQ1174.2
004300     XXXXX055.                                                    SQ1174.2
004400     SELECT SQ-FS9 ASSIGN TO                                      SQ1174.2
004500     XXXXX014                                                     SQ1174.2
004600     ORGANIZATION IS SEQUENTIAL                                   SQ1174.2
004700     ACCESS MODE IS SEQUENTIAL.                                   SQ1174.2
004800 DATA DIVISION.                                                   SQ1174.2
004900 FILE SECTION.                                                    SQ1174.2
005000*                                                                 SQ1174.2
005100*FD  RAW-DATA.                                                    SQ1174.2
005200*                                                                 SQ1174.2
005300*01  RAW-DATA-SATZ.                                               SQ1174.2
005400*    05  RAW-DATA-KEY        PIC X(6).                            SQ1174.2
005500*    05  C-DATE              PIC 9(6).                            SQ1174.2
005600*    05  C-TIME              PIC 9(8).                            SQ1174.2
005700*    05  C-NO-OF-TESTS       PIC 99.                              SQ1174.2
005800*    05  C-OK                PIC 999.                             SQ1174.2
005900*    05  C-ALL               PIC 999.                             SQ1174.2
006000*    05  C-FAIL              PIC 999.                             SQ1174.2
006100*    05  C-DELETED           PIC 999.                             SQ1174.2
006200*    05  C-INSPECT           PIC 999.                             SQ1174.2
006300*    05  C-NOTE              PIC X(13).                           SQ1174.2
006400*    05  C-INDENT            PIC X.                               SQ1174.2
006500*    05  C-ABORT             PIC X(8).                            SQ1174.2
006600 FD  PRINT-FILE                                                   SQ1174.2
006700C    LABEL RECORDS                                                SQ1174.2
006800C    XXXXX084                                                     SQ1174.2
006900C    DATA RECORD IS PRINT-REC DUMMY-RECORD                        SQ1174.2
007000               .                                                  SQ1174.2
007100 01  PRINT-REC PICTURE X(120).                                    SQ1174.2
007200 01  DUMMY-RECORD PICTURE X(120).                                 SQ1174.2
007300 FD  SQ-FS9                                                       SQ1174.2
007400C    LABEL RECORD STANDARD                                        SQ1174.2
007500     BLOCK CONTAINS 1 RECORDS.                                    SQ1174.2
007600 01  SQ-FS9R1-F-G-141.                                            SQ1174.2
007700     02  SQ-FS9R1-PART1  PICTURE X(120).                          SQ1174.2
007800     02  SQ-FS9R1-PART2  PICTURE X(21).                           SQ1174.2
007900 WORKING-STORAGE SECTION.                                         SQ1174.2
008000 01  COUNT-OF-RECS PICTURE 9(5)  VALUE 0.                         SQ1174.2
008100 01  RECORDS-IN-ERROR PIC S9(5) USAGE COMP VALUE 0.               SQ1174.2
008200 01  ERROR-FLAG PICTURE 9 VALUE 0.                                SQ1174.2
008300 01  EOF-FLAG PIC 9 VALUE 0.                                      SQ1174.2
008400 01  WRITE-FROM-AREA1.                                            SQ1174.2
008500     02  AREA1-1  PIC X(87).                                      SQ1174.2
008600 01  FOLLOWS-AREA1  PIC  X(10).                                   SQ1174.2
008700 01  WRITE-FROM-AREA2.                                            SQ1174.2
008800     02  AREA2-1  PIC  X(120).                                    SQ1174.2
008900 01  WRITE-FROM-AREA3.                                            SQ1174.2
009000     02  AREA3-1  PIC X(141).                                     SQ1174.2
009100     02  AREA3-2  PIC X(7).                                       SQ1174.2
009200 01  WRITE-FROM-AREA4.                                            SQ1174.2
009300     02  AREA4-1  PIC X(120).                                     SQ1174.2
009400     02  AREA4-2  PIC X(21).                                      SQ1174.2
009500 01  END-OF-RECORD-AREA.                                          SQ1174.2
009600     02  ALPHA-AREA  PIC X(17).                                   SQ1174.2
009700     02  NUMBER-AREA PIC 9999.                                    SQ1174.2
009800 01  FILE-RECORD-INFORMATION-REC.                                 SQ1174.2
009900     03 FILE-RECORD-INFO-SKELETON.                                SQ1174.2
010000        05 FILLER                 PICTURE X(48)       VALUE       SQ1174.2
010100             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  SQ1174.2
010200        05 FILLER                 PICTURE X(46)       VALUE       SQ1174.2
010300             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    SQ1174.2
010400        05 FILLER                 PICTURE X(26)       VALUE       SQ1174.2
010500             ",LFIL=000000,ORG=  ,LBLR= ".                        SQ1174.2
010600        05 FILLER                 PICTURE X(37)       VALUE       SQ1174.2
010700             ",RECKEY=                             ".             SQ1174.2
010800        05 FILLER                 PICTURE X(38)       VALUE       SQ1174.2
010900             ",ALTKEY1=                             ".            SQ1174.2
011000        05 FILLER                 PICTURE X(38)       VALUE       SQ1174.2
011100             ",ALTKEY2=                             ".            SQ1174.2
011200        05 FILLER                 PICTURE X(7)        VALUE SPACE.SQ1174.2
011300     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              SQ1174.2
011400        05 FILE-RECORD-INFO-P1-120.                               SQ1174.2
011500           07 FILLER              PIC X(5).                       SQ1174.2
011600           07 XFILE-NAME           PIC X(6).                      SQ1174.2
011700           07 FILLER              PIC X(8).                       SQ1174.2
011800           07 XRECORD-NAME         PIC X(6).                      SQ1174.2
011900           07 FILLER              PIC X(1).                       SQ1174.2
012000           07 REELUNIT-NUMBER     PIC 9(1).                       SQ1174.2
012100           07 FILLER              PIC X(7).                       SQ1174.2
012200           07 XRECORD-NUMBER       PIC 9(6).                      SQ1174.2
012300           07 FILLER              PIC X(6).                       SQ1174.2
012400           07 UPDATE-NUMBER       PIC 9(2).                       SQ1174.2
012500           07 FILLER              PIC X(5).                       SQ1174.2
012600           07 ODO-NUMBER          PIC 9(4).                       SQ1174.2
012700           07 FILLER              PIC X(5).                       SQ1174.2
012800           07 XPROGRAM-NAME        PIC X(5).                      SQ1174.2
012900           07 FILLER              PIC X(7).                       SQ1174.2
013000           07 XRECORD-LENGTH       PIC 9(6).                      SQ1174.2
013100           07 FILLER              PIC X(7).                       SQ1174.2
013200           07 CHARS-OR-RECORDS    PIC X(2).                       SQ1174.2
013300           07 FILLER              PIC X(1).                       SQ1174.2
013400           07 XBLOCK-SIZE          PIC 9(4).                      SQ1174.2
013500           07 FILLER              PIC X(6).                       SQ1174.2
013600           07 RECORDS-IN-FILE     PIC 9(6).                       SQ1174.2
013700           07 FILLER              PIC X(5).                       SQ1174.2
013800           07 XFILE-ORGANIZATION   PIC X(2).                      SQ1174.2
013900           07 FILLER              PIC X(6).                       SQ1174.2
014000           07 XLABEL-TYPE          PIC X(1).                      SQ1174.2
014100        05 FILE-RECORD-INFO-P121-240.                             SQ1174.2
014200           07 FILLER              PIC X(8).                       SQ1174.2
014300           07 XRECORD-KEY          PIC X(29).                     SQ1174.2
014400           07 FILLER              PIC X(9).                       SQ1174.2
014500           07 ALTERNATE-KEY1      PIC X(29).                      SQ1174.2
014600           07 FILLER              PIC X(9).                       SQ1174.2
014700           07 ALTERNATE-KEY2      PIC X(29).                      SQ1174.2
014800           07 FILLER              PIC X(7).                       SQ1174.2
014900 01  TEST-RESULTS.                                                SQ1174.2
015000     02 FILLER                    PICTURE X VALUE SPACE.          SQ1174.2
015100     02 FEATURE                   PICTURE X(20) VALUE SPACE.      SQ1174.2
015200     02 FILLER                    PICTURE X VALUE SPACE.          SQ1174.2
015300     02 P-OR-F                    PICTURE X(5) VALUE SPACE.       SQ1174.2
015400     02 FILLER                    PICTURE X  VALUE SPACE.         SQ1174.2
015500     02  PAR-NAME.                                                SQ1174.2
015600       03 FILLER PICTURE X(12) VALUE SPACE.                       SQ1174.2
015700       03  PARDOT-X PICTURE X  VALUE SPACE.                       SQ1174.2
015800       03 DOTVALUE PICTURE 99  VALUE ZERO.                        SQ1174.2
015900       03 FILLER PIC X(5) VALUE SPACE.                            SQ1174.2
016000     02 FILLER PIC X(10) VALUE SPACE.                             SQ1174.2
016100     02 RE-MARK PIC X(61).                                        SQ1174.2
016200 01  TEST-COMPUTED.                                               SQ1174.2
016300     02 FILLER PIC X(30) VALUE SPACE.                             SQ1174.2
016400     02 FILLER PIC X(17) VALUE "       COMPUTED=".                SQ1174.2
016500     02 COMPUTED-X.                                               SQ1174.2
016600     03 COMPUTED-A                PICTURE X(20) VALUE SPACE.      SQ1174.2
016700     03 COMPUTED-N REDEFINES COMPUTED-A PICTURE -9(9).9(9).       SQ1174.2
016800     03 COMPUTED-0V18 REDEFINES COMPUTED-A  PICTURE -.9(18).      SQ1174.2
016900     03 COMPUTED-4V14 REDEFINES COMPUTED-A  PICTURE -9(4).9(14).  SQ1174.2
017000     03 COMPUTED-14V4 REDEFINES COMPUTED-A  PICTURE -9(14).9(4).  SQ1174.2
017100     03       CM-18V0 REDEFINES COMPUTED-A.                       SQ1174.2
017200         04 COMPUTED-18V0                   PICTURE -9(18).       SQ1174.2
017300         04 FILLER                          PICTURE X.            SQ1174.2
017400     03 FILLER PIC X(50) VALUE SPACE.                             SQ1174.2
017500 01  TEST-CORRECT.                                                SQ1174.2
017600     02 FILLER PIC X(30) VALUE SPACE.                             SQ1174.2
017700     02 FILLER PIC X(17) VALUE "       CORRECT =".                SQ1174.2
017800     02 CORRECT-X.                                                SQ1174.2
017900     03 CORRECT-A                 PICTURE X(20) VALUE SPACE.      SQ1174.2
018000     03 CORRECT-N REDEFINES CORRECT-A PICTURE -9(9).9(9).         SQ1174.2
018100     03 CORRECT-0V18 REDEFINES CORRECT-A    PICTURE -.9(18).      SQ1174.2
018200     03 CORRECT-4V14 REDEFINES CORRECT-A    PICTURE -9(4).9(14).  SQ1174.2
018300     03 CORRECT-14V4 REDEFINES CORRECT-A    PICTURE -9(14).9(4).  SQ1174.2
018400     03      CR-18V0 REDEFINES CORRECT-A.                         SQ1174.2
018500         04 CORRECT-18V0                    PICTURE -9(18).       SQ1174.2
018600         04 FILLER                          PICTURE X.            SQ1174.2
018700     03 FILLER PIC X(50) VALUE SPACE.                             SQ1174.2
018800 01  CCVS-C-1.                                                    SQ1174.2
018900     02 FILLER PICTURE IS X(99) VALUE IS " FEATURE              PASQ1174.2
019000-    "SS  PARAGRAPH-NAME                                          SQ1174.2
019100-    "        REMARKS".                                           SQ1174.2
019200     02 FILLER PICTURE IS X(20) VALUE IS SPACE.                   SQ1174.2
019300 01  CCVS-C-2.                                                    SQ1174.2
019400     02 FILLER PICTURE IS X VALUE IS SPACE.                       SQ1174.2
019500     02 FILLER PICTURE IS X(6) VALUE IS "TESTED".                 SQ1174.2
019600     02 FILLER PICTURE IS X(15) VALUE IS SPACE.                   SQ1174.2
019700     02 FILLER PICTURE IS X(4) VALUE IS "FAIL".                   SQ1174.2
019800     02 FILLER PICTURE IS X(94) VALUE IS SPACE.                   SQ1174.2
019900 01  REC-SKL-SUB PICTURE 9(2) VALUE ZERO.                         SQ1174.2
020000 01  REC-CT PICTURE 99 VALUE ZERO.                                SQ1174.2
020100 01  DELETE-CNT                   PICTURE 999  VALUE ZERO.        SQ1174.2
020200 01  ERROR-COUNTER PICTURE IS 999 VALUE IS ZERO.                  SQ1174.2
020300 01  INSPECT-COUNTER PIC 999 VALUE ZERO.                          SQ1174.2
020400 01  PASS-COUNTER PIC 999 VALUE ZERO.                             SQ1174.2
020500 01  TOTAL-ERROR PIC 999 VALUE ZERO.                              SQ1174.2
020600 01  ERROR-HOLD PIC 999 VALUE ZERO.                               SQ1174.2
020700 01  DUMMY-HOLD PIC X(120) VALUE SPACE.                           SQ1174.2
020800 01  RECORD-COUNT PIC 9(5) VALUE ZERO.                            SQ1174.2
020900 01  CCVS-H-1.                                                    SQ1174.2
021000     02  FILLER   PICTURE X(27)  VALUE SPACE.                     SQ1174.2
021100     02 FILLER PICTURE X(67) VALUE                                SQ1174.2
021200     " FEDERAL SOFTWARE TESTING CENTER COBOL COMPILER VALIDATION  SQ1174.2
021300-    " SYSTEM".                                                   SQ1174.2
021400     02  FILLER     PICTURE X(26)  VALUE SPACE.                   SQ1174.2
021500 01  CCVS-H-2.                                                    SQ1174.2
021600     02 FILLER PICTURE X(52) VALUE IS                             SQ1174.2
021700     "CCVS85 FSTC COPY, NOT FOR DISTRIBUTION.".                   SQ1174.2
021800     02 FILLER PICTURE IS X(19) VALUE IS "TEST RESULTS SET-  ".   SQ1174.2
021900     02 TEST-ID PICTURE IS X(9).                                  SQ1174.2
022000     02 FILLER PICTURE IS X(40) VALUE IS SPACE.                   SQ1174.2
022100 01  CCVS-H-3.                                                    SQ1174.2
022200     02  FILLER PICTURE X(34) VALUE                               SQ1174.2
022300     " FOR OFFICIAL USE ONLY    ".                                SQ1174.2
022400     02  FILLER PICTURE X(58) VALUE                               SQ1174.2
022500     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ1174.2
022600     02  FILLER PICTURE X(28) VALUE                               SQ1174.2
022700     "  COPYRIGHT   1985 ".                                       SQ1174.2
022800 01  CCVS-E-1.                                                    SQ1174.2
022900     02 FILLER PICTURE IS X(52) VALUE IS SPACE.                   SQ1174.2
023000     02 FILLER PICTURE IS X(14) VALUE IS "END OF TEST-  ".        SQ1174.2
023100     02 ID-AGAIN PICTURE IS X(9).                                 SQ1174.2
023200     02 FILLER PICTURE X(45) VALUE IS                             SQ1174.2
023300     " NTIS DISTRIBUTION COBOL 85".                               SQ1174.2
023400 01  CCVS-E-2.                                                    SQ1174.2
023500     02  FILLER                   PICTURE X(31)  VALUE            SQ1174.2
023600     SPACE.                                                       SQ1174.2
023700     02  FILLER                   PICTURE X(21)  VALUE SPACE.     SQ1174.2
023800     02 CCVS-E-2-2.                                               SQ1174.2
023900         03 ERROR-TOTAL PICTURE IS XXX VALUE IS SPACE.            SQ1174.2
024000         03 FILLER PICTURE IS X VALUE IS SPACE.                   SQ1174.2
024100         03 ENDER-DESC PIC X(46) VALUE "ERRORS ENCOUNTERED".      SQ1174.2
024200 01  CCVS-E-3.                                                    SQ1174.2
024300     02  FILLER PICTURE X(22) VALUE                               SQ1174.2
024400     " FOR OFFICIAL USE ONLY".                                    SQ1174.2
024500     02  FILLER PICTURE X(12) VALUE SPACE.                        SQ1174.2
024600     02  FILLER PICTURE X(58) VALUE                               SQ1174.2
024700     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1174.2
024800     02  FILLER PICTURE X(13) VALUE SPACE.                        SQ1174.2
024900     02 FILLER PIC X(15) VALUE " COPYRIGHT 1985".                 SQ1174.2
025000 01  CCVS-E-4.                                                    SQ1174.2
025100     02 CCVS-E-4-1 PIC XXX VALUE SPACE.                           SQ1174.2
025200     02 FILLER PIC XXXX VALUE " OF ".                             SQ1174.2
025300     02 CCVS-E-4-2 PIC XXX VALUE SPACE.                           SQ1174.2
025400     02 FILLER PIC X(40) VALUE                                    SQ1174.2
025500      "  TESTS WERE EXECUTED SUCCESSFULLY".                       SQ1174.2
025600 01  XXINFO.                                                      SQ1174.2
025700     02 FILLER PIC X(30) VALUE "        *** INFORMATION  ***".    SQ1174.2
025800     02 INFO-TEXT.                                                SQ1174.2
025900     04 FILLER PIC X(20) VALUE SPACE.                             SQ1174.2
026000     04 XXCOMPUTED PIC X(20).                                     SQ1174.2
026100     04 FILLER PIC X(5) VALUE SPACE.                              SQ1174.2
026200     04 XXCORRECT PIC X(20).                                      SQ1174.2
026300 01  HYPHEN-LINE.                                                 SQ1174.2
026400     02 FILLER PICTURE IS X VALUE IS SPACE.                       SQ1174.2
026500     02 FILLER PICTURE IS X(65) VALUE IS "************************SQ1174.2
026600-    "*****************************************".                 SQ1174.2
026700     02 FILLER PICTURE IS X(54) VALUE IS "************************SQ1174.2
026800-    "******************************".                            SQ1174.2
026900 01  CCVS-PGM-ID PIC X(6) VALUE                                   SQ1174.2
027000     "SQ117A".                                                    SQ1174.2
027100 PROCEDURE DIVISION.                                              SQ1174.2
027200 CCVS1 SECTION.                                                   SQ1174.2
027300 OPEN-FILES.                                                      SQ1174.2
027400*    OPEN I-O RAW-DATA.                                           SQ1174.2
027500*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            SQ1174.2
027600*    READ RAW-DATA INVALID KEY GO TO END-E-1.                     SQ1174.2
027700*    MOVE "ABORTED " TO C-ABORT.                                  SQ1174.2
027800*    ADD 1 TO C-NO-OF-TESTS.                                      SQ1174.2
027900*    ACCEPT C-DATE  FROM DATE.                                    SQ1174.2
028000*    ACCEPT C-TIME  FROM TIME.                                    SQ1174.2
028100*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-1.             SQ1174.2
028200*END-E-1.                                                         SQ1174.2
028300*    CLOSE RAW-DATA.                                              SQ1174.2
028400     OPEN     OUTPUT PRINT-FILE.                                  SQ1174.2
028500     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   SQ1174.2
028600     MOVE    SPACE TO TEST-RESULTS.                               SQ1174.2
028700     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             SQ1174.2
028800     MOVE ZERO TO REC-SKL-SUB.                                    SQ1174.2
028900     PERFORM CCVS-INIT-FILE 9 TIMES.                              SQ1174.2
029000 CCVS-INIT-FILE.                                                  SQ1174.2
029100     ADD 1 TO REC-SKL-SUB.                                        SQ1174.2
029200     MOVE FILE-RECORD-INFO-SKELETON TO                            SQ1174.2
029300                  FILE-RECORD-INFO (REC-SKL-SUB).                 SQ1174.2
029400 CCVS-INIT-EXIT.                                                  SQ1174.2
029500     GO TO CCVS1-EXIT.                                            SQ1174.2
029600 CLOSE-FILES.                                                     SQ1174.2
029700     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   SQ1174.2
029800*    OPEN I-O RAW-DATA.                                           SQ1174.2
029900*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            SQ1174.2
030000*    READ RAW-DATA INVALID KEY GO TO END-E-2.                     SQ1174.2
030100*    MOVE "OK.     " TO C-ABORT.                                  SQ1174.2
030200*    MOVE PASS-COUNTER TO C-OK.                                   SQ1174.2
030300*    MOVE ERROR-HOLD   TO C-ALL.                                  SQ1174.2
030400*    MOVE ERROR-COUNTER TO C-FAIL.                                SQ1174.2
030500*    MOVE DELETE-CNT TO C-DELETED.                                SQ1174.2
030600*    MOVE INSPECT-COUNTER TO C-INSPECT.                           SQ1174.2
030700*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-2.             SQ1174.2
030800*END-E-2.                                                         SQ1174.2
030900*    CLOSE RAW-DATA.                                              SQ1174.2
031000 TERMINATE-CCVS.                                                  SQ1174.2
031100*    EXIT PROGRAM.                                                SQ1174.2
031200*TERMINATE-CALL.                                                  SQ1174.2
031300     STOP     RUN.                                                SQ1174.2
031400 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         SQ1174.2
031500 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           SQ1174.2
031600 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          SQ1174.2
031700 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-CNT.          SQ1174.2
031800     MOVE "****TEST DELETED****" TO RE-MARK.                      SQ1174.2
031900 PRINT-DETAIL.                                                    SQ1174.2
032000     IF REC-CT NOT EQUAL TO ZERO                                  SQ1174.2
032100             MOVE "." TO PARDOT-X                                 SQ1174.2
032200             MOVE REC-CT TO DOTVALUE.                             SQ1174.2
032300     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      SQ1174.2
032400     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               SQ1174.2
032500        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 SQ1174.2
032600          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 SQ1174.2
032700     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              SQ1174.2
032800     MOVE SPACE TO CORRECT-X.                                     SQ1174.2
032900     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         SQ1174.2
033000     MOVE     SPACE TO RE-MARK.                                   SQ1174.2
033100 HEAD-ROUTINE.                                                    SQ1174.2
033200     MOVE CCVS-H-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ1174.2
033300     MOVE CCVS-H-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.   SQ1174.2
033400     MOVE CCVS-H-3 TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.   SQ1174.2
033500 COLUMN-NAMES-ROUTINE.                                            SQ1174.2
033600     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ1174.2
033700     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ1174.2
033800     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ1174.2
033900 END-ROUTINE.                                                     SQ1174.2
034000     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.SQ1174.2
034100 END-RTN-EXIT.                                                    SQ1174.2
034200     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ1174.2
034300 END-ROUTINE-1.                                                   SQ1174.2
034400      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      SQ1174.2
034500      ERROR-HOLD. ADD DELETE-CNT TO ERROR-HOLD.                   SQ1174.2
034600      ADD PASS-COUNTER TO ERROR-HOLD.                             SQ1174.2
034700*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   SQ1174.2
034800      MOVE PASS-COUNTER TO CCVS-E-4-1.                            SQ1174.2
034900      MOVE ERROR-HOLD TO CCVS-E-4-2.                              SQ1174.2
035000      MOVE CCVS-E-4 TO CCVS-E-2-2.                                SQ1174.2
035100      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           SQ1174.2
035200  END-ROUTINE-12.                                                 SQ1174.2
035300      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        SQ1174.2
035400     IF       ERROR-COUNTER IS EQUAL TO ZERO                      SQ1174.2
035500         MOVE "NO " TO ERROR-TOTAL                                SQ1174.2
035600         ELSE                                                     SQ1174.2
035700         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       SQ1174.2
035800     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           SQ1174.2
035900     PERFORM WRITE-LINE.                                          SQ1174.2
036000 END-ROUTINE-13.                                                  SQ1174.2
036100     IF DELETE-CNT IS EQUAL TO ZERO                               SQ1174.2
036200         MOVE "NO " TO ERROR-TOTAL  ELSE                          SQ1174.2
036300         MOVE DELETE-CNT TO ERROR-TOTAL.                          SQ1174.2
036400     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   SQ1174.2
036500     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ1174.2
036600      IF   INSPECT-COUNTER EQUAL TO ZERO                          SQ1174.2
036700          MOVE "NO " TO ERROR-TOTAL                               SQ1174.2
036800      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   SQ1174.2
036900      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            SQ1174.2
037000      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          SQ1174.2
037100     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ1174.2
037200 WRITE-LINE.                                                      SQ1174.2
037300     ADD 1 TO RECORD-COUNT.                                       SQ1174.2
037400*    IF RECORD-COUNT GREATER 50                                   SQ1174.2
037500*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          SQ1174.2
037600*        MOVE SPACE TO DUMMY-RECORD                               SQ1174.2
037700*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  SQ1174.2
037800*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             SQ1174.2
037900*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     SQ1174.2
038000*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          SQ1174.2
038100*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          SQ1174.2
038200*        MOVE ZERO TO RECORD-COUNT.                               SQ1174.2
038300     PERFORM WRT-LN.                                              SQ1174.2
038400 WRT-LN.                                                          SQ1174.2
038500     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               SQ1174.2
038600     MOVE SPACE TO DUMMY-RECORD.                                  SQ1174.2
038700 BLANK-LINE-PRINT.                                                SQ1174.2
038800     PERFORM WRT-LN.                                              SQ1174.2
038900 FAIL-ROUTINE.                                                    SQ1174.2
039000     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.   SQ1174.2
039100     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.    SQ1174.2
039200     MOVE "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.    SQ1174.2
039300     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     SQ1174.2
039400     GO TO FAIL-ROUTINE-EX.                                       SQ1174.2
039500 FAIL-ROUTINE-WRITE.                                              SQ1174.2
039600     MOVE TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE           SQ1174.2
039700     MOVE TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES.   SQ1174.2
039800 FAIL-ROUTINE-EX. EXIT.                                           SQ1174.2
039900 BAIL-OUT.                                                        SQ1174.2
040000     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.       SQ1174.2
040100     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.               SQ1174.2
040200 BAIL-OUT-WRITE.                                                  SQ1174.2
040300     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  SQ1174.2
040400     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     SQ1174.2
040500 BAIL-OUT-EX. EXIT.                                               SQ1174.2
040600 CCVS1-EXIT.                                                      SQ1174.2
040700     EXIT.                                                        SQ1174.2
040800 SECT-SQ117A-0001 SECTION.                                        SQ1174.2
040900 WRITE-INIT-GF-01.                                                SQ1174.2
041000     MOVE "SQ-FS9" TO XFILE-NAME (1).                             SQ1174.2
041100     MOVE "R1-F-G" TO XRECORD-NAME (1).                           SQ1174.2
041200     MOVE CCVS-PGM-ID TO XPROGRAM-NAME (1).                       SQ1174.2
041300     MOVE 141 TO XRECORD-LENGTH (1).                              SQ1174.2
041400     MOVE "RC" TO CHARS-OR-RECORDS (1).                           SQ1174.2
041500     MOVE 1 TO XBLOCK-SIZE (1).                                   SQ1174.2
041600     MOVE 493 TO RECORDS-IN-FILE (1).                             SQ1174.2
041700     MOVE "SQ" TO XFILE-ORGANIZATION (1).                         SQ1174.2
041800     MOVE "O" TO XLABEL-TYPE (1).                                 SQ1174.2
041900     OPEN OUTPUT SQ-FS9.                                          SQ1174.2
042000     MOVE "WRITE...FROM FILE" TO ALPHA-AREA.                      SQ1174.2
042100 WRITE-TEST-GF-01.                                                SQ1174.2
042200     ADD 1 TO COUNT-OF-RECS.                                      SQ1174.2
042300     MOVE COUNT-OF-RECS TO XRECORD-NUMBER (1).                    SQ1174.2
042400     MOVE COUNT-OF-RECS TO NUMBER-AREA.                           SQ1174.2
042500     MOVE FILE-RECORD-INFO-P1-120 (1) TO AREA4-1.                 SQ1174.2
042600     MOVE END-OF-RECORD-AREA TO AREA4-2.                          SQ1174.2
042700     WRITE SQ-FS9R1-F-G-141 FROM WRITE-FROM-AREA4.                SQ1174.2
042800*        THIS TEST CONTAINS A WRITE RECORD FROM IDENTIFIER        SQ1174.2
042900*    STATEMENT WITH THE SIZE OF THE IDENTIFIER EQUAL TO THE SIZE  SQ1174.2
043000*    OF FILE RECORD.  THE IDENTIFIER AREA IS CHECKED AFTER THE    SQ1174.2
043100*    WRITE TO ENSURE THIS AREA WAS LEFT INTACT BY THE WRITE       SQ1174.2
043200*    STATEMENT.                                                   SQ1174.2
043300     IF FILE-RECORD-INFO-P1-120 (1) NOT EQUAL TO AREA4-1          SQ1174.2
043400             MOVE  1 TO ERROR-FLAG.                               SQ1174.2
043500     IF END-OF-RECORD-AREA NOT EQUAL TO AREA4-2                   SQ1174.2
043600          MOVE 1 TO ERROR-FLAG.                                   SQ1174.2
043700     IF COUNT-OF-RECS EQUAL TO 50                                 SQ1174.2
043800         GO TO WRITE-TEST-GF-01-1.                                SQ1174.2
043900     GO TO WRITE-TEST-GF-01.                                      SQ1174.2
044000 WRITE-TEST-GF-01-1.                                              SQ1174.2
044100     IF ERROR-FLAG EQUAL TO ZERO                                  SQ1174.2
044200          GO TO WRITE-PASS-GF-01.                                 SQ1174.2
044300 WRITE-FAIL-GF-01.                                                SQ1174.2
044400     MOVE "VII-53; 4.7.3 (4); FROM AREA DESTROYED BY WRITE"       SQ1174.2
044500            TO RE-MARK.                                           SQ1174.2
044600     GO TO WRITE-WRITE-GF-01.                                     SQ1174.2
044700 WRITE-PASS-GF-01.                                                SQ1174.2
044800     PERFORM PASS.                                                SQ1174.2
044900 WRITE-WRITE-GF-01.                                               SQ1174.2
045000     MOVE "WRTE-TEST-GF-01" TO PAR-NAME.                          SQ1174.2
045100     MOVE "WRITE...FROM EQUAL" TO FEATURE.                        SQ1174.2
045200     PERFORM PRINT-DETAIL.                                        SQ1174.2
045300 WRITE-INIT-GF-02-A.                                              SQ1174.2
045400*        THIS TEST WRITES A RECORD FROM AN IDENTIFIER WHICH IS    SQ1174.2
045500*    LARGER THAN THE SIZE OF THE FILE RECORD.  THE RIGHTMOST 7    SQ1174.2
045600*    CHARACTERS SHOULD BE TRUNCATED IN THE OUTPUT RECORD.         SQ1174.2
045700     ADD 1 TO COUNT-OF-RECS.                                      SQ1174.2
045800     MOVE COUNT-OF-RECS TO XRECORD-NUMBER (1).                    SQ1174.2
045900     MOVE COUNT-OF-RECS TO NUMBER-AREA.                           SQ1174.2
046000     MOVE FILE-RECORD-INFO-P1-120 (1) TO AREA4-1.                 SQ1174.2
046100     MOVE END-OF-RECORD-AREA TO AREA4-2.                          SQ1174.2
046200     MOVE WRITE-FROM-AREA4 TO AREA3-1.                            SQ1174.2
046300     MOVE "ABCDEFG" TO AREA3-2.                                   SQ1174.2
046400     WRITE SQ-FS9R1-F-G-141 FROM WRITE-FROM-AREA3.                SQ1174.2
046500     IF COUNT-OF-RECS EQUAL TO 100                                SQ1174.2
046600         GO TO WRITE-INIT-GF-03-A.                                SQ1174.2
046700     GO TO WRITE-INIT-GF-02-A.                                    SQ1174.2
046800 WRITE-INIT-GF-03-A.                                              SQ1174.2
046900*        THIS TEST WRITES A RECORD FROM AN IDENTIFIER OF 87       SQ1174.2
047000*    CHARACTERS LENGTH.  IN THE OUTPUT RECORD CHARACTERS 88       SQ1174.2
047100*    THROUGH 141 SHOULD BE BLANK.  ONLY THE NUMBER OF CHARACTERS  SQ1174.2
047200*    IN THE FROM IDENTIFIER SHOULD BE MOVED TO THE OUTPUT RECORD. SQ1174.2
047300*    THE CHARACTERS IN THE AREA FOLLOWING IDENTIFIER              SQ1174.2
047400*    ARE NOT MOVED INTO THE OUTPUT AREA.                          SQ1174.2
047500     ADD 1 TO COUNT-OF-RECS.                                      SQ1174.2
047600     MOVE COUNT-OF-RECS TO XRECORD-NUMBER (1).                    SQ1174.2
047700     MOVE FILE-RECORD-INFO-P1-120 (1) TO AREA1-1.                 SQ1174.2
047800     MOVE "ZXYUVST" TO FOLLOWS-AREA1.                             SQ1174.2
047900     WRITE SQ-FS9R1-F-G-141 FROM WRITE-FROM-AREA1.                SQ1174.2
048000     IF COUNT-OF-RECS EQUAL TO 150                                SQ1174.2
048100         GO TO WRITE-INIT-GF-04-A.                                SQ1174.2
048200     GO TO WRITE-INIT-GF-03-A.                                    SQ1174.2
048300 WRITE-INIT-GF-04-A.                                              SQ1174.2
048400*        THIS TEST WRITES A RECORD FROM AN IDENTIFIER OF 120      SQ1174.2
048500*    CHARACTERS.  THE LAST 21 CHARACTERS IN THE FD RECORD AREA    SQ1174.2
048600*    ARE SET TO JUNK WHICH SHOULD BE REPLACED WITH BLANKS DURING  SQ1174.2
048700*    THE WRITE...FROM STATEMENT.  THE IDENTIFIER IS LEVEL 02.     SQ1174.2
048800     ADD 1 TO COUNT-OF-RECS.                                      SQ1174.2
048900     MOVE COUNT-OF-RECS TO XRECORD-NUMBER (1).                    SQ1174.2
049000     MOVE FILE-RECORD-INFO-P1-120 (1) TO AREA2-1.                 SQ1174.2
049100     MOVE "AREA SHOULD BE BLANK" TO SQ-FS9R1-PART2.               SQ1174.2
049200     WRITE SQ-FS9R1-F-G-141 FROM AREA2-1.                         SQ1174.2
049300     IF COUNT-OF-RECS EQUAL TO 200                                SQ1174.2
049400         GO TO WRITE-INIT-GF-05-A.                                SQ1174.2
049500     GO TO WRITE-INIT-GF-04-A.                                    SQ1174.2
049600 WRITE-INIT-GF-05-A.                                              SQ1174.2
049700*        THIS TEST WRITES A RECORD OF 121 CHARACTERS FROM A       SQ1174.2
049800*    SUBSCRIPTED DATA ITEM. THE LAST 21 CHARACTERS IN THE FD      SQ1174.2
049900*    RECORD AREA ARE SET TO JUNK WHICH SHOULD BE REPLACED WITH    SQ1174.2
050000*    BLANKS DURING THE WRITE...FROM STATEMENT. IDENT IS LEVEL 05. SQ1174.2
050100     ADD 1 TO COUNT-OF-RECS.                                      SQ1174.2
050200     MOVE COUNT-OF-RECS TO XRECORD-NUMBER (1).                    SQ1174.2
050300     MOVE "AREA SHOULD BE BLANK" TO SQ-FS9R1-PART2.               SQ1174.2
050400     WRITE SQ-FS9R1-F-G-141 FROM FILE-RECORD-INFO-P1-120 (1).     SQ1174.2
050500     IF COUNT-OF-RECS EQUAL TO 250                                SQ1174.2
050600         GO TO WRITE-INIT-GF-06-A.                                SQ1174.2
050700     GO TO WRITE-INIT-GF-05-A.                                    SQ1174.2
050800 WRITE-INIT-GF-06-A.                                              SQ1174.2
050900*        THIS TEST WRITES RECORDS FROM AN IDENTIFIER THE SAME     SQ1174.2
051000*    SIZE AS THE OUTPUT RECORD AREA.                              SQ1174.2
051100     ADD 1 TO COUNT-OF-RECS.                                      SQ1174.2
051200     MOVE COUNT-OF-RECS TO XRECORD-NUMBER (1).                    SQ1174.2
051300     MOVE COUNT-OF-RECS TO NUMBER-AREA.                           SQ1174.2
051400     MOVE FILE-RECORD-INFO-P1-120 (1) TO AREA4-1.                 SQ1174.2
051500     MOVE END-OF-RECORD-AREA TO AREA4-2.                          SQ1174.2
051600     WRITE SQ-FS9R1-F-G-141 FROM WRITE-FROM-AREA4.                SQ1174.2
051700     IF COUNT-OF-RECS EQUAL TO 493                                SQ1174.2
051800        GO TO WRITE-FROM-CLOSE.                                   SQ1174.2
051900     GO TO WRITE-INIT-GF-06-A.                                    SQ1174.2
052000 WRITE-FROM-CLOSE.                                                SQ1174.2
052100     CLOSE SQ-FS9.                                                SQ1174.2
052200     MOVE 0 TO ERROR-FLAG.                                        SQ1174.2
052300     MOVE 0 TO COUNT-OF-RECS.                                     SQ1174.2
052400 WRITE-INIT-GF-02.                                                SQ1174.2
052500     OPEN INPUT SQ-FS9.                                           SQ1174.2
052600 WRITE-TEST-GF-02.                                                SQ1174.2
052700     IF COUNT-OF-RECS EQUAL TO 50                                 SQ1174.2
052800         GO TO WRITE-TEST-GF-02-1.                                SQ1174.2
052900     READ SQ-FS9 RECORD                                           SQ1174.2
053000          AT END MOVE "UNEXPECTED EOF" TO COMPUTED-A              SQ1174.2
053100                 MOVE 1 TO EOF-FLAG                               SQ1174.2
053200                 GO TO WRITE-FAIL-GF-02.                          SQ1174.2
053300     ADD 1 TO COUNT-OF-RECS.                                      SQ1174.2
053400     MOVE SQ-FS9R1-PART1 TO FILE-RECORD-INFO-P1-120 (1).          SQ1174.2
053500     MOVE SQ-FS9R1-PART2 TO END-OF-RECORD-AREA.                   SQ1174.2
053600     IF ALPHA-AREA NOT EQUAL TO "WRITE...FROM FILE"               SQ1174.2
053700          GO TO WRITE-FAIL-GF-02-1.                               SQ1174.2
053800     IF NUMBER-AREA NOT EQUAL TO COUNT-OF-RECS                    SQ1174.2
053900          GO TO WRITE-FAIL-GF-02-1.                               SQ1174.2
054000     IF XRECORD-NUMBER (1) NOT EQUAL TO COUNT-OF-RECS             SQ1174.2
054100          GO TO WRITE-FAIL-GF-02-1.                               SQ1174.2
054200     IF XFILE-NAME (1) NOT EQUAL TO "SQ-FS9"                      SQ1174.2
054300          GO TO WRITE-FAIL-GF-02-1.                               SQ1174.2
054400     IF XLABEL-TYPE (1) NOT EQUAL TO "O"                          SQ1174.2
054500          GO TO WRITE-FAIL-GF-02-1.                               SQ1174.2
054600     GO TO WRITE-TEST-GF-02.                                      SQ1174.2
054700 WRITE-FAIL-GF-02-1.                                              SQ1174.2
054800     ADD 1 TO RECORDS-IN-ERROR.                                   SQ1174.2
054900     MOVE 1 TO ERROR-FLAG.                                        SQ1174.2
055000     GO TO WRITE-TEST-GF-02.                                      SQ1174.2
055100 WRITE-TEST-GF-02-1.                                              SQ1174.2
055200     IF ERROR-FLAG EQUAL TO ZERO                                  SQ1174.2
055300        GO TO WRITE-PASS-GF-02.                                   SQ1174.2
055400     MOVE "ERROR IN RECORD(S)" TO COMPUTED-A.                     SQ1174.2
055500 WRITE-FAIL-GF-02.                                                SQ1174.2
055600     MOVE "VII-53; 4.7.3 (3) LARGER RECORDS:TRUNCATED            "SQ1174.2
055700           TO RE-MARK.                                            SQ1174.2
055800     PERFORM FAIL.                                                SQ1174.2
055900     GO TO WRITE-WRITE-GF-02.                                     SQ1174.2
056000 WRITE-PASS-GF-02.                                                SQ1174.2
056100     PERFORM PASS.                                                SQ1174.2
056200 WRITE-WRITE-GF-02.                                               SQ1174.2
056300     MOVE "WRITE .. FROM LARGER" TO FEATURE.                      SQ1174.2
056400     MOVE "WRTE-TEST-GF-02" TO PAR-NAME.                          SQ1174.2
056500     PERFORM PRINT-DETAIL.                                        SQ1174.2
056600 WRITE-INIT-GF-03.                                                SQ1174.2
056700     MOVE 0 TO ERROR-FLAG.                                        SQ1174.2
056800     IF EOF-FLAG EQUAL TO 1                                       SQ1174.2
056900          GO TO SEQ-EOF-22.                                       SQ1174.2
057000     MOVE "WRTE-TEST-GF-03" TO PAR-NAME.                          SQ1174.2
057100     MOVE "WRITE ... FROP SHORTER" TO FEATURE.                    SQ1174.2
057200 WRITE-TEST-GF-03.                                                SQ1174.2
057300     IF COUNT-OF-RECS EQUAL TO 100                                SQ1174.2
057400          GO TO WRITE-TEST-GF-03-1.                               SQ1174.2
057500     READ SQ-FS9 RECORD                                           SQ1174.2
057600          AT END MOVE "UNEXPECTED EOF" TO COMPUTED-A              SQ1174.2
057700                 MOVE 1 TO EOF-FLAG                               SQ1174.2
057800                 GO TO WRITE-FAIL-GF-03.                          SQ1174.2
057900     ADD 1 TO COUNT-OF-RECS.                                      SQ1174.2
058000     MOVE SPACE TO AREA3-2.                                       SQ1174.2
058100     MOVE SQ-FS9R1-F-G-141 TO WRITE-FROM-AREA3.                   SQ1174.2
058200     IF AREA3-2 NOT EQUAL TO SPACE                                SQ1174.2
058300          MOVE "NO TRUNCATION" TO RE-MARK                         SQ1174.2
058400          GO TO WRITE-FAIL-GF-03-1.                               SQ1174.2
058500     MOVE SQ-FS9R1-PART1 TO FILE-RECORD-INFO-P1-120 (1).          SQ1174.2
058600     MOVE SQ-FS9R1-PART2 TO END-OF-RECORD-AREA.                   SQ1174.2
058700     IF ALPHA-AREA NOT EQUAL TO "WRITE...FROM FILE"               SQ1174.2
058800          GO TO WRITE-FAIL-GF-03-1.                               SQ1174.2
058900     IF NUMBER-AREA NOT EQUAL TO COUNT-OF-RECS                    SQ1174.2
059000          GO TO WRITE-FAIL-GF-03-1.                               SQ1174.2
059100     IF XRECORD-NUMBER (1) NOT EQUAL TO COUNT-OF-RECS             SQ1174.2
059200          GO TO WRITE-FAIL-GF-03-1.                               SQ1174.2
059300     IF XFILE-NAME (1) NOT EQUAL TO "SQ-FS9"                      SQ1174.2
059400          GO TO WRITE-FAIL-GF-03-1.                               SQ1174.2
059500     IF XLABEL-TYPE (1) NOT EQUAL TO "O"                          SQ1174.2
059600          GO TO WRITE-FAIL-GF-03-1.                               SQ1174.2
059700     GO TO WRITE-TEST-GF-03.                                      SQ1174.2
059800 WRITE-FAIL-GF-03-1.                                              SQ1174.2
059900     ADD 1 TO RECORDS-IN-ERROR.                                   SQ1174.2
060000     MOVE 1 TO ERROR-FLAG.                                        SQ1174.2
060100     GO TO WRITE-TEST-GF-03.                                      SQ1174.2
060200 WRITE-TEST-GF-03-1.                                              SQ1174.2
060300     IF ERROR-FLAG EQUAL TO ZERO                                  SQ1174.2
060400         GO TO WRITE-PASS-GF-03.                                  SQ1174.2
060500     MOVE "ERROR IN RECORD(S)" TO COMPUTED-A.                     SQ1174.2
060600 WRITE-FAIL-GF-03.                                                SQ1174.2
060700     MOVE "VII-53; 4.7.3 (3) SHORTER  RECORDS: NOT SPACE FILLED  "SQ1174.2
060800           TO RE-MARK.                                            SQ1174.2
060900     PERFORM FAIL.                                                SQ1174.2
061000     GO TO WRITE-WRITE-GF-03.                                     SQ1174.2
061100 WRITE-PASS-GF-03.                                                SQ1174.2
061200     PERFORM PASS.                                                SQ1174.2
061300 WRITE-WRITE-GF-03.                                               SQ1174.2
061400     PERFORM PRINT-DETAIL.                                        SQ1174.2
061500 WRITE-INIT-GF-04.                                                SQ1174.2
061600     MOVE 0 TO ERROR-FLAG.                                        SQ1174.2
061700     IF EOF-FLAG EQUAL TO 1                                       SQ1174.2
061800         GO TO SEQ-EOF-22.                                        SQ1174.2
061900     MOVE "WRTE-TEST-GF-04" TO PAR-NAME.                          SQ1174.2
062000 WRITE-TEST-GF-04.                                                SQ1174.2
062100     IF COUNT-OF-RECS EQUAL TO 150                                SQ1174.2
062200         GO TO WRITE-TEST-GF-04-1.                                SQ1174.2
062300     READ SQ-FS9 RECORD                                           SQ1174.2
062400          AT END MOVE "UNEXPECTED EOF" TO COMPUTED-A              SQ1174.2
062500                 MOVE 1 TO EOF-FLAG                               SQ1174.2
062600                 GO TO WRITE-FAIL-GF-04.                          SQ1174.2
062700     ADD 1 TO COUNT-OF-RECS.                                      SQ1174.2
062800     IF SQ-FS9R1-PART2 NOT EQUAL TO SPACE                         SQ1174.2
062900         MOVE "NO SPACE FILLING" TO RE-MARK                       SQ1174.2
063000         GO TO WRITE-FAIL-GF-04-1.                                SQ1174.2
063100     MOVE SQ-FS9R1-PART1 TO FILE-RECORD-INFO-P1-120 (1).          SQ1174.2
063200     IF CHARS-OR-RECORDS (1) NOT EQUAL TO SPACE                   SQ1174.2
063300         MOVE "NO SPACE FILLING" TO RE-MARK                       SQ1174.2
063400         GO TO WRITE-FAIL-GF-04-1.                                SQ1174.2
063500     IF XLABEL-TYPE (1) NOT EQUAL TO SPACE                        SQ1174.2
063600         MOVE "NO SPACE FILLING" TO RE-MARK                       SQ1174.2
063700         GO TO WRITE-FAIL-GF-04-1.                                SQ1174.2
063800     IF XFILE-NAME (1) NOT EQUAL "SQ-FS9"                         SQ1174.2
063900         GO TO WRITE-FAIL-GF-04-1.                                SQ1174.2
064000     IF XRECORD-NUMBER (1) NOT EQUAL TO COUNT-OF-RECS             SQ1174.2
064100         GO TO WRITE-FAIL-GF-04-1.                                SQ1174.2
064200     GO TO WRITE-TEST-GF-04.                                      SQ1174.2
064300 WRITE-FAIL-GF-04-1.                                              SQ1174.2
064400     ADD 1 TO RECORDS-IN-ERROR.                                   SQ1174.2
064500     MOVE 1 TO ERROR-FLAG.                                        SQ1174.2
064600     GO TO WRITE-TEST-GF-04.                                      SQ1174.2
064700 WRITE-TEST-GF-04-1.                                              SQ1174.2
064800     IF ERROR-FLAG EQUAL TO ZERO                                  SQ1174.2
064900         GO TO WRITE-PASS-GF-04.                                  SQ1174.2
065000     MOVE "ERROR IN RECORD(S)" TO COMPUTED-A.                     SQ1174.2
065100 WRITE-FAIL-GF-04.                                                SQ1174.2
065200     MOVE "VII-53; 4.7.3 (3) SHORTER RECORDS: NOT SPACE FILLED   "SQ1174.2
065300           TO RE-MARK.                                            SQ1174.2
065400     PERFORM FAIL.                                                SQ1174.2
065500     GO TO WRITE-WRITE-GF-04.                                     SQ1174.2
065600 WRITE-PASS-GF-04.                                                SQ1174.2
065700     PERFORM PASS.                                                SQ1174.2
065800 WRITE-WRITE-GF-04.                                               SQ1174.2
065900     MOVE "WRITE ... FROM 02 SHORT RECS" TO FEATURE.              SQ1174.2
066000     PERFORM PRINT-DETAIL.                                        SQ1174.2
066100 WRITE-INIT-GF-05.                                                SQ1174.2
066200     MOVE 0 TO ERROR-FLAG.                                        SQ1174.2
066300     IF EOF-FLAG EQUAL TO 1                                       SQ1174.2
066400         GO TO SEQ-EOF-22.                                        SQ1174.2
066500     MOVE "WRTE-TEST-GF-05" TO PAR-NAME.                          SQ1174.2
066600 WRITE-TEST-GF-05.                                                SQ1174.2
066700     IF COUNT-OF-RECS EQUAL TO 200                                SQ1174.2
066800          GO TO WRITE-TEST-GF-05-1.                               SQ1174.2
066900     READ SQ-FS9 RECORD                                           SQ1174.2
067000         AT END MOVE "UNEXPECTED EOF" TO COMPUTED-A               SQ1174.2
067100         MOVE 1 TO EOF-FLAG                                       SQ1174.2
067200         GO TO WRITE-FAIL-GF-05.                                  SQ1174.2
067300     ADD 1 TO COUNT-OF-RECS.                                      SQ1174.2
067400     IF SQ-FS9R1-PART2 NOT EQUAL TO SPACE                         SQ1174.2
067500         MOVE "NOT BLANK FILLED" TO RE-MARK                       SQ1174.2
067600         GO TO WRITE-FAIL-GF-05-1.                                SQ1174.2
067700     MOVE SQ-FS9R1-PART1 TO FILE-RECORD-INFO-P1-120 (1).          SQ1174.2
067800     IF XFILE-NAME (1) NOT EQUAL TO "SQ-FS9"                      SQ1174.2
067900         GO TO WRITE-FAIL-GF-05-1.                                SQ1174.2
068000     IF XRECORD-NUMBER (1) NOT EQUAL TO COUNT-OF-RECS             SQ1174.2
068100         GO TO WRITE-FAIL-GF-05-1.                                SQ1174.2
068200     IF XLABEL-TYPE (1) NOT EQUAL TO "O"                          SQ1174.2
068300         GO TO WRITE-FAIL-GF-05-1.                                SQ1174.2
068400     GO TO WRITE-TEST-GF-05.                                      SQ1174.2
068500 WRITE-FAIL-GF-05-1.                                              SQ1174.2
068600     ADD 1 TO RECORDS-IN-ERROR.                                   SQ1174.2
068700     MOVE 1 TO ERROR-FLAG.                                        SQ1174.2
068800     GO TO WRITE-TEST-GF-05.                                      SQ1174.2
068900 WRITE-TEST-GF-05-1.                                              SQ1174.2
069000     IF ERROR-FLAG EQUAL TO 0                                     SQ1174.2
069100         GO TO WRITE-PASS-GF-05.                                  SQ1174.2
069200     MOVE "ERROR IN RECORD(S)" TO COMPUTED-A.                     SQ1174.2
069300 WRITE-FAIL-GF-05.                                                SQ1174.2
069400     MOVE "VII-53; 4.7.3 (3) SHORTER RECORDS: NOT SPACE FILLED   "SQ1174.2
069500           TO RE-MARK.                                            SQ1174.2
069600     PERFORM FAIL.                                                SQ1174.2
069700     GO TO WRITE-WRITE-GF-05.                                     SQ1174.2
069800 WRITE-PASS-GF-05.                                                SQ1174.2
069900     PERFORM PASS.                                                SQ1174.2
070000 WRITE-WRITE-GF-05.                                               SQ1174.2
070100     MOVE "WRITE .. FROM SHORT SUBSC 02" TO FEATURE.              SQ1174.2
070200     PERFORM PRINT-DETAIL.                                        SQ1174.2
070300 WRITE-INIT-GF-06.                                                SQ1174.2
070400     MOVE 0 TO ERROR-FLAG.                                        SQ1174.2
070500     IF EOF-FLAG EQUAL TO 1                                       SQ1174.2
070600         GO TO SEQ-EOF-22.                                        SQ1174.2
070700     MOVE "WRTE-TEST-GF-06" TO PAR-NAME.                          SQ1174.2
070800 WRITE-TEST-GF-06.                                                SQ1174.2
070900     IF COUNT-OF-RECS EQUAL TO 250                                SQ1174.2
071000          GO TO WRITE-TEST-GF-06-1.                               SQ1174.2
071100     READ SQ-FS9 RECORD                                           SQ1174.2
071200          AT END MOVE "UNEXPECTED EOF" TO COMPUTED-A              SQ1174.2
071300                 MOVE 1 TO EOF-FLAG                               SQ1174.2
071400                 GO TO WRITE-FAIL-GF-06.                          SQ1174.2
071500     ADD 1 TO COUNT-OF-RECS.                                      SQ1174.2
071600     IF SQ-FS9R1-PART2 NOT EQUAL TO SPACE                         SQ1174.2
071700          MOVE "NOT BLANK FILLED" TO RE-MARK                      SQ1174.2
071800          GO TO WRITE-FAIL-GF-06-1.                               SQ1174.2
071900     MOVE SQ-FS9R1-PART1 TO FILE-RECORD-INFO-P1-120 (1).          SQ1174.2
072000     IF XFILE-NAME (1) NOT EQUAL TO "SQ-FS9"                      SQ1174.2
072100          GO TO WRITE-FAIL-GF-06-1.                               SQ1174.2
072200     IF XRECORD-NUMBER (1) NOT EQUAL TO COUNT-OF-RECS             SQ1174.2
072300          GO TO WRITE-FAIL-GF-06-1.                               SQ1174.2
072400     IF XLABEL-TYPE (1) NOT EQUAL TO "O"                          SQ1174.2
072500          GO TO WRITE-FAIL-GF-06-1.                               SQ1174.2
072600     GO TO WRITE-TEST-GF-06.                                      SQ1174.2
072700 WRITE-FAIL-GF-06-1.                                              SQ1174.2
072800     ADD 1 TO RECORDS-IN-ERROR.                                   SQ1174.2
072900     MOVE 1 TO ERROR-FLAG.                                        SQ1174.2
073000     GO TO WRITE-TEST-GF-06.                                      SQ1174.2
073100 WRITE-TEST-GF-06-1.                                              SQ1174.2
073200     IF ERROR-FLAG EQUAL TO 0                                     SQ1174.2
073300           GO TO WRITE-PASS-GF-06.                                SQ1174.2
073400     MOVE "ERROR IN RECORD(S)" TO COMPUTED-A.                     SQ1174.2
073500 WRITE-FAIL-GF-06.                                                SQ1174.2
073600     MOVE "VII-53; 4.7.3 (3) SHORTER RECORDS: NOT SPACE FILLED   "SQ1174.2
073700           TO RE-MARK.                                            SQ1174.2
073800     PERFORM FAIL.                                                SQ1174.2
073900     GO TO WRITE-WRITE-GF-06.                                     SQ1174.2
074000 WRITE-PASS-GF-06.                                                SQ1174.2
074100     PERFORM PASS.                                                SQ1174.2
074200 WRITE-WRITE-GF-06.                                               SQ1174.2
074300     MOVE "WRITE .. FROM SHORT SUBSC 05 " TO FEATURE.             SQ1174.2
074400     PERFORM PRINT-DETAIL.                                        SQ1174.2
074500 WRITE-INIT-GF-07.                                                SQ1174.2
074600     MOVE 0 TO ERROR-FLAG.                                        SQ1174.2
074700     IF EOF-FLAG EQUAL TO 1                                       SQ1174.2
074800         GO TO SEQ-EOF-22.                                        SQ1174.2
074900     MOVE "WRTE-TEST-GF-07" TO PAR-NAME.                          SQ1174.2
075000 WRITE-TEST-GF-07.                                                SQ1174.2
075100     IF COUNT-OF-RECS EQUAL TO 493                                SQ1174.2
075200          GO TO WRITE-TEST-GF-07-1.                               SQ1174.2
075300     READ SQ-FS9 RECORD                                           SQ1174.2
075400          AT END MOVE "UNEXPECTED EOF" TO COMPUTED-A              SQ1174.2
075500          MOVE 1 TO EOF-FLAG                                      SQ1174.2
075600          GO TO WRITE-FAIL-GF-07.                                 SQ1174.2
075700     ADD 1 TO COUNT-OF-RECS.                                      SQ1174.2
075800     MOVE SQ-FS9R1-PART1 TO FILE-RECORD-INFO-P1-120 (1).          SQ1174.2
075900     MOVE SQ-FS9R1-PART2 TO END-OF-RECORD-AREA.                   SQ1174.2
076000     IF ALPHA-AREA NOT EQUAL TO "WRITE...FROM FILE"               SQ1174.2
076100          GO TO WRITE-FAIL-GF-07-1.                               SQ1174.2
076200     IF NUMBER-AREA NOT EQUAL TO COUNT-OF-RECS                    SQ1174.2
076300          GO TO WRITE-FAIL-GF-07-1.                               SQ1174.2
076400     IF XFILE-NAME (1) NOT EQUAL TO "SQ-FS9"                      SQ1174.2
076500          GO TO WRITE-FAIL-GF-07-1.                               SQ1174.2
076600     IF XRECORD-NUMBER (1) NOT EQUAL TO COUNT-OF-RECS             SQ1174.2
076700          GO TO WRITE-FAIL-GF-07-1.                               SQ1174.2
076800     IF XLABEL-TYPE (1) NOT EQUAL TO "O"                          SQ1174.2
076900          GO TO WRITE-FAIL-GF-07-1.                               SQ1174.2
077000     GO TO WRITE-TEST-GF-07.                                      SQ1174.2
077100 WRITE-FAIL-GF-07-1.                                              SQ1174.2
077200     ADD 1 TO RECORDS-IN-ERROR.                                   SQ1174.2
077300     MOVE 1 TO ERROR-FLAG.                                        SQ1174.2
077400     GO TO WRITE-TEST-GF-07.                                      SQ1174.2
077500 WRITE-TEST-GF-07-1.                                              SQ1174.2
077600     IF ERROR-FLAG EQUAL TO ZERO                                  SQ1174.2
077700         GO TO WRITE-PASS-GF-07.                                  SQ1174.2
077800     MOVE "ERROR IN RECORD(S)" TO COMPUTED-A.                     SQ1174.2
077900 WRITE-FAIL-GF-07.                                                SQ1174.2
078000     MOVE "VII-53; 4.7.3 (3) SAME SIZE" TO RE-MARK.               SQ1174.2
078100     PERFORM FAIL.                                                SQ1174.2
078200     GO TO WRITE-WRITE-GF-07.                                     SQ1174.2
078300 WRITE-PASS-GF-07.                                                SQ1174.2
078400     PERFORM PASS.                                                SQ1174.2
078500 WRITE-WRITE-GF-07.                                               SQ1174.2
078600     MOVE "WRITE .. FROM SAME SIZE" TO FEATURE.                   SQ1174.2
078700     PERFORM PRINT-DETAIL.                                        SQ1174.2
078800 SEQ-TEST-022.                                                    SQ1174.2
078900     IF EOF-FLAG EQUAL TO 1                                       SQ1174.2
079000          GO TO SEQ-EOF-22.                                       SQ1174.2
079100     IF RECORDS-IN-ERROR NOT EQUAL TO ZERO                        SQ1174.2
079200          MOVE "RECORDS IN ERROR =" TO COMPUTED-A                 SQ1174.2
079300          MOVE RECORDS-IN-ERROR TO CORRECT-18V0                   SQ1174.2
079400          GO TO SEQ-FAIL-22.                                      SQ1174.2
079500     READ SQ-FS9 RECORD                                           SQ1174.2
079600          AT END PERFORM PASS                                     SQ1174.2
079700                 GO TO SEQ-WRITE-22.                              SQ1174.2
079800     MOVE "MORE THAN 493 RECORDS" TO RE-MARK.                     SQ1174.2
079900     GO TO SEQ-FAIL-22.                                           SQ1174.2
080000 SEQ-EOF-22.                                                      SQ1174.2
080100     MOVE "LESS THAN 493 RECORDS" TO RE-MARK.                     SQ1174.2
080200     MOVE "RECORDS READ =" TO COMPUTED-A.                         SQ1174.2
080300     MOVE COUNT-OF-RECS TO CORRECT-18V0.                          SQ1174.2
080400 SEQ-FAIL-22.                                                     SQ1174.2
080500     MOVE "VII-52; 4.7.2 (3)                         " TO RE-MARK.SQ1174.2
080600     PERFORM FAIL.                                                SQ1174.2
080700 SEQ-WRITE-22.                                                    SQ1174.2
080800     MOVE "READ FILE SQ-FS9" TO FEATURE.                          SQ1174.2
080900     MOVE "SEQ-TEST-022" TO PAR-NAME.                             SQ1174.2
081000     PERFORM PRINT-DETAIL.                                        SQ1174.2
081100 SEQ-CLOSE-22.                                                    SQ1174.2
081200     CLOSE SQ-FS9.                                                SQ1174.2
081300 TERMINATE-ROUTINE.                                               SQ1174.2
081400     EXIT.                                                        SQ1174.2
081500 CCVS-EXIT SECTION.                                               SQ1174.2
081600 CCVS-999999.                                                     SQ1174.2
081700     GO TO CLOSE-FILES.                                           SQ1174.2
      *END-OF,SQ117A                                                                  
