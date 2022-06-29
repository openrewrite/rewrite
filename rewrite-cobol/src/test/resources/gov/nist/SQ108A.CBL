      *HEADER,COBOL,SQ108A                                                            
000100 IDENTIFICATION DIVISION.                                         SQ1084.2
000200 PROGRAM-ID.                                                      SQ1084.2
000300     SQ108A.                                                      SQ1084.2
000400****************************************************************  SQ1084.2
000500*                                                              *  SQ1084.2
000600*    VALIDATION FOR:-                                          *  SQ1084.2
000700*    " HIGH       ".                                              SQ1084.2
000800*                                                              *  SQ1084.2
000900*    CREATION DATE     /     VALIDATION DATE                   *  SQ1084.2
001000*    "4.2 ".                                                      SQ1084.2
001100*                                                              *  SQ1084.2
001200****************************************************************  SQ1084.2
001300                                                                  SQ1084.2
001400*        THE ROUTINE SQ108A CREATES A FIXED LENGTH MASS STORAGE   SQ1084.2
001500*    FILE.  THE FILE IS CREATED USING WRITE STATEMENTS, VERIFIED  SQ1084.2
001600*    IN SEQ-TEST-20 AND THEN READ USING READ...INTO STATEMENTS.   SQ1084.2
001700*    THE READ...INTO TESTS CHECK FOR TRUNCATION AND BLANK FILL    SQ1084.2
001800*    OF THE IDENTIFIER AREA.                                      SQ1084.2
001900*                                                                 SQ1084.2
002000*    USED X-CARDS:                                                SQ1084.2
002100*         XXXXX014                                                SQ1084.2
002200*         XXXXX055                                                SQ1084.2
002300*     P   XXXXX062                                                SQ1084.2
002400*         XXXXX082                                                SQ1084.2
002500*         XXXXX083                                                SQ1084.2
002600*     C   XXXXX084                                                SQ1084.2
002700*                                                                 SQ1084.2
002800*                                                                 SQ1084.2
002900 ENVIRONMENT DIVISION.                                            SQ1084.2
003000 CONFIGURATION SECTION.                                           SQ1084.2
003100 SOURCE-COMPUTER.                                                 SQ1084.2
003200     XXXXX082.                                                    SQ1084.2
003300 OBJECT-COMPUTER.                                                 SQ1084.2
003400     XXXXX083.                                                    SQ1084.2
003500 INPUT-OUTPUT SECTION.                                            SQ1084.2
003600 FILE-CONTROL.                                                    SQ1084.2
003700*    SELECT RAW-DATA   ASSIGN TO                                  SQ1084.2
003800*    XXXXX062                                                     SQ1084.2
003900*           ORGANIZATION IS INDEXED                               SQ1084.2
004000*           ACCESS MODE IS RANDOM                                 SQ1084.2
004100*           RECORD KEY IS RAW-DATA-KEY.                           SQ1084.2
004200     SELECT PRINT-FILE ASSIGN TO                                  SQ1084.2
004300     XXXXX055.                                                    SQ1084.2
004400     SELECT SQ-FS8 ASSIGN TO                                      SQ1084.2
004500     XXXXX014                                                     SQ1084.2
004600     ORGANIZATION IS SEQUENTIAL                                   SQ1084.2
004700     ACCESS MODE IS SEQUENTIAL.                                   SQ1084.2
004800 DATA DIVISION.                                                   SQ1084.2
004900 FILE SECTION.                                                    SQ1084.2
005000*                                                                 SQ1084.2
005100*FD  RAW-DATA.                                                    SQ1084.2
005200*                                                                 SQ1084.2
005300*01  RAW-DATA-SATZ.                                               SQ1084.2
005400*    05  RAW-DATA-KEY        PIC X(6).                            SQ1084.2
005500*    05  C-DATE              PIC 9(6).                            SQ1084.2
005600*    05  C-TIME              PIC 9(8).                            SQ1084.2
005700*    05  C-NO-OF-TESTS       PIC 99.                              SQ1084.2
005800*    05  C-OK                PIC 999.                             SQ1084.2
005900*    05  C-ALL               PIC 999.                             SQ1084.2
006000*    05  C-FAIL              PIC 999.                             SQ1084.2
006100*    05  C-DELETED           PIC 999.                             SQ1084.2
006200*    05  C-INSPECT           PIC 999.                             SQ1084.2
006300*    05  C-NOTE              PIC X(13).                           SQ1084.2
006400*    05  C-INDENT            PIC X.                               SQ1084.2
006500*    05  C-ABORT             PIC X(8).                            SQ1084.2
006600 FD  PRINT-FILE                                                   SQ1084.2
006700C    LABEL RECORDS                                                SQ1084.2
006800C    XXXXX084                                                     SQ1084.2
006900C    DATA RECORD IS PRINT-REC DUMMY-RECORD                        SQ1084.2
007000               .                                                  SQ1084.2
007100 01  PRINT-REC PICTURE X(120).                                    SQ1084.2
007200 01  DUMMY-RECORD PICTURE X(120).                                 SQ1084.2
007300 FD  SQ-FS8                                                       SQ1084.2
007400C    LABEL RECORD STANDARD                                        SQ1084.2
007500     BLOCK CONTAINS 1 RECORDS.                                    SQ1084.2
007600 01  SQ-FS8R1-F-G-141.                                            SQ1084.2
007700     02  SQ-FS8R1-PART1   PICTURE X(120).                         SQ1084.2
007800     02  SQ-FS8R1-PART2   PICTURE X(21).                          SQ1084.2
007900 WORKING-STORAGE SECTION.                                         SQ1084.2
008000 01  END-OF-RECORD-AREA.                                          SQ1084.2
008100     02  ALPHA-AREA PIC X(17).                                    SQ1084.2
008200     02  NUMBER-AREA  PIC 9999.                                   SQ1084.2
008300 01  COUNT-OF-RECS PIC 9999.                                      SQ1084.2
008400 01  RECORDS-IN-ERROR  PIC S9(5) USAGE COMP VALUE 0.              SQ1084.2
008500 01  ERROR-FLAG PICTURE 9 VALUE 0.                                SQ1084.2
008600 01  EOF-FLAG PICTURE 9 VALUE 0.                                  SQ1084.2
008700 01  READ-INTO-AREA1.                                             SQ1084.2
008800     02  AREA1-1  PIC X(87).                                      SQ1084.2
008900 01  FOLLOWS-AREA1 PIC X(10).                                     SQ1084.2
009000 01  READ-INTO-AREA2.                                             SQ1084.2
009100     02  AREA2-1 PIC X(120).                                      SQ1084.2
009200 01  FOLLOWS-AREA2 PIC X(10).                                     SQ1084.2
009300 01  READ-INTO-AREA3.                                             SQ1084.2
009400     02  AREA3-1   PIC  X(141).                                   SQ1084.2
009500     02  AREA3-2   PIC  X(7).                                     SQ1084.2
009600 01  READ-INTO-AREA4.                                             SQ1084.2
009700     02  AREA4-1   PICTURE X(120).                                SQ1084.2
009800     02  AREA4-2   PICTURE X(21).                                 SQ1084.2
009900 01  FILE-RECORD-INFORMATION-REC.                                 SQ1084.2
010000     03 FILE-RECORD-INFO-SKELETON.                                SQ1084.2
010100        05 FILLER                 PICTURE X(48)       VALUE       SQ1084.2
010200             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  SQ1084.2
010300        05 FILLER                 PICTURE X(46)       VALUE       SQ1084.2
010400             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    SQ1084.2
010500        05 FILLER                 PICTURE X(26)       VALUE       SQ1084.2
010600             ",LFIL=000000,ORG=  ,LBLR= ".                        SQ1084.2
010700        05 FILLER                 PICTURE X(37)       VALUE       SQ1084.2
010800             ",RECKEY=                             ".             SQ1084.2
010900        05 FILLER                 PICTURE X(38)       VALUE       SQ1084.2
011000             ",ALTKEY1=                             ".            SQ1084.2
011100        05 FILLER                 PICTURE X(38)       VALUE       SQ1084.2
011200             ",ALTKEY2=                             ".            SQ1084.2
011300        05 FILLER                 PICTURE X(7)        VALUE SPACE.SQ1084.2
011400     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              SQ1084.2
011500        05 FILE-RECORD-INFO-P1-120.                               SQ1084.2
011600           07 FILLER              PIC X(5).                       SQ1084.2
011700           07 XFILE-NAME           PIC X(6).                      SQ1084.2
011800           07 FILLER              PIC X(8).                       SQ1084.2
011900           07 XRECORD-NAME         PIC X(6).                      SQ1084.2
012000           07 FILLER              PIC X(1).                       SQ1084.2
012100           07 REELUNIT-NUMBER     PIC 9(1).                       SQ1084.2
012200           07 FILLER              PIC X(7).                       SQ1084.2
012300           07 XRECORD-NUMBER       PIC 9(6).                      SQ1084.2
012400           07 FILLER              PIC X(6).                       SQ1084.2
012500           07 UPDATE-NUMBER       PIC 9(2).                       SQ1084.2
012600           07 FILLER              PIC X(5).                       SQ1084.2
012700           07 ODO-NUMBER          PIC 9(4).                       SQ1084.2
012800           07 FILLER              PIC X(5).                       SQ1084.2
012900           07 XPROGRAM-NAME        PIC X(5).                      SQ1084.2
013000           07 FILLER              PIC X(7).                       SQ1084.2
013100           07 XRECORD-LENGTH       PIC 9(6).                      SQ1084.2
013200           07 FILLER              PIC X(7).                       SQ1084.2
013300           07 CHARS-OR-RECORDS    PIC X(2).                       SQ1084.2
013400           07 FILLER              PIC X(1).                       SQ1084.2
013500           07 XBLOCK-SIZE          PIC 9(4).                      SQ1084.2
013600           07 FILLER              PIC X(6).                       SQ1084.2
013700           07 RECORDS-IN-FILE     PIC 9(6).                       SQ1084.2
013800           07 FILLER              PIC X(5).                       SQ1084.2
013900           07 XFILE-ORGANIZATION   PIC X(2).                      SQ1084.2
014000           07 FILLER              PIC X(6).                       SQ1084.2
014100           07 XLABEL-TYPE          PIC X(1).                      SQ1084.2
014200        05 FILE-RECORD-INFO-P121-240.                             SQ1084.2
014300           07 FILLER              PIC X(8).                       SQ1084.2
014400           07 XRECORD-KEY          PIC X(29).                     SQ1084.2
014500           07 FILLER              PIC X(9).                       SQ1084.2
014600           07 ALTERNATE-KEY1      PIC X(29).                      SQ1084.2
014700           07 FILLER              PIC X(9).                       SQ1084.2
014800           07 ALTERNATE-KEY2      PIC X(29).                      SQ1084.2
014900           07 FILLER              PIC X(7).                       SQ1084.2
015000 01  TEST-RESULTS.                                                SQ1084.2
015100     02 FILLER                    PICTURE X VALUE SPACE.          SQ1084.2
015200     02 FEATURE                   PICTURE X(20) VALUE SPACE.      SQ1084.2
015300     02 FILLER                    PICTURE X VALUE SPACE.          SQ1084.2
015400     02 P-OR-F                    PICTURE X(5) VALUE SPACE.       SQ1084.2
015500     02 FILLER                    PICTURE X  VALUE SPACE.         SQ1084.2
015600     02  PAR-NAME.                                                SQ1084.2
015700       03 FILLER PICTURE X(12) VALUE SPACE.                       SQ1084.2
015800       03  PARDOT-X PICTURE X  VALUE SPACE.                       SQ1084.2
015900       03 DOTVALUE PICTURE 99  VALUE ZERO.                        SQ1084.2
016000       03 FILLER PIC X(5) VALUE SPACE.                            SQ1084.2
016100     02 FILLER PIC X(10) VALUE SPACE.                             SQ1084.2
016200     02 RE-MARK PIC X(61).                                        SQ1084.2
016300 01  TEST-COMPUTED.                                               SQ1084.2
016400     02 FILLER PIC X(30) VALUE SPACE.                             SQ1084.2
016500     02 FILLER PIC X(17) VALUE "       COMPUTED=".                SQ1084.2
016600     02 COMPUTED-X.                                               SQ1084.2
016700     03 COMPUTED-A                PICTURE X(20) VALUE SPACE.      SQ1084.2
016800     03 COMPUTED-N REDEFINES COMPUTED-A PICTURE -9(9).9(9).       SQ1084.2
016900     03 COMPUTED-0V18 REDEFINES COMPUTED-A  PICTURE -.9(18).      SQ1084.2
017000     03 COMPUTED-4V14 REDEFINES COMPUTED-A  PICTURE -9(4).9(14).  SQ1084.2
017100     03 COMPUTED-14V4 REDEFINES COMPUTED-A  PICTURE -9(14).9(4).  SQ1084.2
017200     03       CM-18V0 REDEFINES COMPUTED-A.                       SQ1084.2
017300         04 COMPUTED-18V0                   PICTURE -9(18).       SQ1084.2
017400         04 FILLER                          PICTURE X.            SQ1084.2
017500     03 FILLER PIC X(50) VALUE SPACE.                             SQ1084.2
017600 01  TEST-CORRECT.                                                SQ1084.2
017700     02 FILLER PIC X(30) VALUE SPACE.                             SQ1084.2
017800     02 FILLER PIC X(17) VALUE "       CORRECT =".                SQ1084.2
017900     02 CORRECT-X.                                                SQ1084.2
018000     03 CORRECT-A                 PICTURE X(20) VALUE SPACE.      SQ1084.2
018100     03 CORRECT-N REDEFINES CORRECT-A PICTURE -9(9).9(9).         SQ1084.2
018200     03 CORRECT-0V18 REDEFINES CORRECT-A    PICTURE -.9(18).      SQ1084.2
018300     03 CORRECT-4V14 REDEFINES CORRECT-A    PICTURE -9(4).9(14).  SQ1084.2
018400     03 CORRECT-14V4 REDEFINES CORRECT-A    PICTURE -9(14).9(4).  SQ1084.2
018500     03      CR-18V0 REDEFINES CORRECT-A.                         SQ1084.2
018600         04 CORRECT-18V0                    PICTURE -9(18).       SQ1084.2
018700         04 FILLER                          PICTURE X.            SQ1084.2
018800     03 FILLER PIC X(50) VALUE SPACE.                             SQ1084.2
018900 01  CCVS-C-1.                                                    SQ1084.2
019000     02 FILLER PICTURE IS X(99) VALUE IS " FEATURE              PASQ1084.2
019100-    "SS  PARAGRAPH-NAME                                          SQ1084.2
019200-    "        REMARKS".                                           SQ1084.2
019300     02 FILLER PICTURE IS X(20) VALUE IS SPACE.                   SQ1084.2
019400 01  CCVS-C-2.                                                    SQ1084.2
019500     02 FILLER PICTURE IS X VALUE IS SPACE.                       SQ1084.2
019600     02 FILLER PICTURE IS X(6) VALUE IS "TESTED".                 SQ1084.2
019700     02 FILLER PICTURE IS X(15) VALUE IS SPACE.                   SQ1084.2
019800     02 FILLER PICTURE IS X(4) VALUE IS "FAIL".                   SQ1084.2
019900     02 FILLER PICTURE IS X(94) VALUE IS SPACE.                   SQ1084.2
020000 01  REC-SKL-SUB PICTURE 9(2) VALUE ZERO.                         SQ1084.2
020100 01  REC-CT PICTURE 99 VALUE ZERO.                                SQ1084.2
020200 01  DELETE-CNT                   PICTURE 999  VALUE ZERO.        SQ1084.2
020300 01  ERROR-COUNTER PICTURE IS 999 VALUE IS ZERO.                  SQ1084.2
020400 01  INSPECT-COUNTER PIC 999 VALUE ZERO.                          SQ1084.2
020500 01  PASS-COUNTER PIC 999 VALUE ZERO.                             SQ1084.2
020600 01  TOTAL-ERROR PIC 999 VALUE ZERO.                              SQ1084.2
020700 01  ERROR-HOLD PIC 999 VALUE ZERO.                               SQ1084.2
020800 01  DUMMY-HOLD PIC X(120) VALUE SPACE.                           SQ1084.2
020900 01  RECORD-COUNT PIC 9(5) VALUE ZERO.                            SQ1084.2
021000 01  CCVS-H-1.                                                    SQ1084.2
021100     02  FILLER   PICTURE X(27)  VALUE SPACE.                     SQ1084.2
021200     02 FILLER PICTURE X(67) VALUE                                SQ1084.2
021300     " FEDERAL SOFTWARE TESTING CENTER COBOL COMPILER VALIDATION  SQ1084.2
021400-    " SYSTEM".                                                   SQ1084.2
021500     02  FILLER     PICTURE X(26)  VALUE SPACE.                   SQ1084.2
021600 01  CCVS-H-2.                                                    SQ1084.2
021700     02 FILLER PICTURE X(52) VALUE IS                             SQ1084.2
021800     "CCVS85 FSTC COPY, NOT FOR DISTRIBUTION.".                   SQ1084.2
021900     02 FILLER PICTURE IS X(19) VALUE IS "TEST RESULTS SET-  ".   SQ1084.2
022000     02 TEST-ID PICTURE IS X(9).                                  SQ1084.2
022100     02 FILLER PICTURE IS X(40) VALUE IS SPACE.                   SQ1084.2
022200 01  CCVS-H-3.                                                    SQ1084.2
022300     02  FILLER PICTURE X(34) VALUE                               SQ1084.2
022400     " FOR OFFICIAL USE ONLY    ".                                SQ1084.2
022500     02  FILLER PICTURE X(58) VALUE                               SQ1084.2
022600     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ1084.2
022700     02  FILLER PICTURE X(28) VALUE                               SQ1084.2
022800     "  COPYRIGHT   1985 ".                                       SQ1084.2
022900 01  CCVS-E-1.                                                    SQ1084.2
023000     02 FILLER PICTURE IS X(52) VALUE IS SPACE.                   SQ1084.2
023100     02 FILLER PICTURE IS X(14) VALUE IS "END OF TEST-  ".        SQ1084.2
023200     02 ID-AGAIN PICTURE IS X(9).                                 SQ1084.2
023300     02 FILLER PICTURE X(45) VALUE IS                             SQ1084.2
023400     " NTIS DISTRIBUTION COBOL 85".                               SQ1084.2
023500 01  CCVS-E-2.                                                    SQ1084.2
023600     02  FILLER                   PICTURE X(31)  VALUE            SQ1084.2
023700     SPACE.                                                       SQ1084.2
023800     02  FILLER                   PICTURE X(21)  VALUE SPACE.     SQ1084.2
023900     02 CCVS-E-2-2.                                               SQ1084.2
024000         03 ERROR-TOTAL PICTURE IS XXX VALUE IS SPACE.            SQ1084.2
024100         03 FILLER PICTURE IS X VALUE IS SPACE.                   SQ1084.2
024200         03 ENDER-DESC PIC X(46) VALUE "ERRORS ENCOUNTERED".      SQ1084.2
024300 01  CCVS-E-3.                                                    SQ1084.2
024400     02  FILLER PICTURE X(22) VALUE                               SQ1084.2
024500     " FOR OFFICIAL USE ONLY".                                    SQ1084.2
024600     02  FILLER PICTURE X(12) VALUE SPACE.                        SQ1084.2
024700     02  FILLER PICTURE X(58) VALUE                               SQ1084.2
024800     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1084.2
024900     02  FILLER PICTURE X(13) VALUE SPACE.                        SQ1084.2
025000     02 FILLER PIC X(15) VALUE " COPYRIGHT 1985".                 SQ1084.2
025100 01  CCVS-E-4.                                                    SQ1084.2
025200     02 CCVS-E-4-1 PIC XXX VALUE SPACE.                           SQ1084.2
025300     02 FILLER PIC XXXX VALUE " OF ".                             SQ1084.2
025400     02 CCVS-E-4-2 PIC XXX VALUE SPACE.                           SQ1084.2
025500     02 FILLER PIC X(40) VALUE                                    SQ1084.2
025600      "  TESTS WERE EXECUTED SUCCESSFULLY".                       SQ1084.2
025700 01  XXINFO.                                                      SQ1084.2
025800     02 FILLER PIC X(30) VALUE "        *** INFORMATION  ***".    SQ1084.2
025900     02 INFO-TEXT.                                                SQ1084.2
026000     04 FILLER PIC X(20) VALUE SPACE.                             SQ1084.2
026100     04 XXCOMPUTED PIC X(20).                                     SQ1084.2
026200     04 FILLER PIC X(5) VALUE SPACE.                              SQ1084.2
026300     04 XXCORRECT PIC X(20).                                      SQ1084.2
026400 01  HYPHEN-LINE.                                                 SQ1084.2
026500     02 FILLER PICTURE IS X VALUE IS SPACE.                       SQ1084.2
026600     02 FILLER PICTURE IS X(65) VALUE IS "************************SQ1084.2
026700-    "*****************************************".                 SQ1084.2
026800     02 FILLER PICTURE IS X(54) VALUE IS "************************SQ1084.2
026900-    "******************************".                            SQ1084.2
027000 01  CCVS-PGM-ID PIC X(6) VALUE                                   SQ1084.2
027100     "SQ108A".                                                    SQ1084.2
027200 PROCEDURE DIVISION.                                              SQ1084.2
027300 CCVS1 SECTION.                                                   SQ1084.2
027400 OPEN-FILES.                                                      SQ1084.2
027500*    OPEN I-O RAW-DATA.                                           SQ1084.2
027600*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            SQ1084.2
027700*    READ RAW-DATA INVALID KEY GO TO END-E-1.                     SQ1084.2
027800*    MOVE "ABORTED " TO C-ABORT.                                  SQ1084.2
027900*    ADD 1 TO C-NO-OF-TESTS.                                      SQ1084.2
028000*    ACCEPT C-DATE  FROM DATE.                                    SQ1084.2
028100*    ACCEPT C-TIME  FROM TIME.                                    SQ1084.2
028200*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-1.             SQ1084.2
028300*END-E-1.                                                         SQ1084.2
028400*    CLOSE RAW-DATA.                                              SQ1084.2
028500     OPEN     OUTPUT PRINT-FILE.                                  SQ1084.2
028600     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   SQ1084.2
028700     MOVE    SPACE TO TEST-RESULTS.                               SQ1084.2
028800     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             SQ1084.2
028900     MOVE ZERO TO REC-SKL-SUB.                                    SQ1084.2
029000     PERFORM CCVS-INIT-FILE 9 TIMES.                              SQ1084.2
029100 CCVS-INIT-FILE.                                                  SQ1084.2
029200     ADD 1 TO REC-SKL-SUB.                                        SQ1084.2
029300     MOVE FILE-RECORD-INFO-SKELETON TO                            SQ1084.2
029400                  FILE-RECORD-INFO (REC-SKL-SUB).                 SQ1084.2
029500 CCVS-INIT-EXIT.                                                  SQ1084.2
029600     GO TO CCVS1-EXIT.                                            SQ1084.2
029700 CLOSE-FILES.                                                     SQ1084.2
029800     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   SQ1084.2
029900*    OPEN I-O RAW-DATA.                                           SQ1084.2
030000*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            SQ1084.2
030100*    READ RAW-DATA INVALID KEY GO TO END-E-2.                     SQ1084.2
030200*    MOVE "OK.     " TO C-ABORT.                                  SQ1084.2
030300*    MOVE PASS-COUNTER TO C-OK.                                   SQ1084.2
030400*    MOVE ERROR-HOLD   TO C-ALL.                                  SQ1084.2
030500*    MOVE ERROR-COUNTER TO C-FAIL.                                SQ1084.2
030600*    MOVE DELETE-CNT TO C-DELETED.                                SQ1084.2
030700*    MOVE INSPECT-COUNTER TO C-INSPECT.                           SQ1084.2
030800*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-2.             SQ1084.2
030900*END-E-2.                                                         SQ1084.2
031000*    CLOSE RAW-DATA.                                              SQ1084.2
031100 TERMINATE-CCVS.                                                  SQ1084.2
031200*    EXIT PROGRAM.                                                SQ1084.2
031300*TERMINATE-CALL.                                                  SQ1084.2
031400     STOP     RUN.                                                SQ1084.2
031500 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         SQ1084.2
031600 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           SQ1084.2
031700 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          SQ1084.2
031800 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-CNT.          SQ1084.2
031900     MOVE "****TEST DELETED****" TO RE-MARK.                      SQ1084.2
032000 PRINT-DETAIL.                                                    SQ1084.2
032100     IF REC-CT NOT EQUAL TO ZERO                                  SQ1084.2
032200             MOVE "." TO PARDOT-X                                 SQ1084.2
032300             MOVE REC-CT TO DOTVALUE.                             SQ1084.2
032400     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      SQ1084.2
032500     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               SQ1084.2
032600        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 SQ1084.2
032700          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 SQ1084.2
032800     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              SQ1084.2
032900     MOVE SPACE TO CORRECT-X.                                     SQ1084.2
033000     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         SQ1084.2
033100     MOVE     SPACE TO RE-MARK.                                   SQ1084.2
033200 HEAD-ROUTINE.                                                    SQ1084.2
033300     MOVE CCVS-H-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ1084.2
033400     MOVE CCVS-H-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.   SQ1084.2
033500     MOVE CCVS-H-3 TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.   SQ1084.2
033600 COLUMN-NAMES-ROUTINE.                                            SQ1084.2
033700     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ1084.2
033800     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ1084.2
033900     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ1084.2
034000 END-ROUTINE.                                                     SQ1084.2
034100     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.SQ1084.2
034200 END-RTN-EXIT.                                                    SQ1084.2
034300     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ1084.2
034400 END-ROUTINE-1.                                                   SQ1084.2
034500      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      SQ1084.2
034600      ERROR-HOLD. ADD DELETE-CNT TO ERROR-HOLD.                   SQ1084.2
034700      ADD PASS-COUNTER TO ERROR-HOLD.                             SQ1084.2
034800*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   SQ1084.2
034900      MOVE PASS-COUNTER TO CCVS-E-4-1.                            SQ1084.2
035000      MOVE ERROR-HOLD TO CCVS-E-4-2.                              SQ1084.2
035100      MOVE CCVS-E-4 TO CCVS-E-2-2.                                SQ1084.2
035200      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           SQ1084.2
035300  END-ROUTINE-12.                                                 SQ1084.2
035400      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        SQ1084.2
035500     IF       ERROR-COUNTER IS EQUAL TO ZERO                      SQ1084.2
035600         MOVE "NO " TO ERROR-TOTAL                                SQ1084.2
035700         ELSE                                                     SQ1084.2
035800         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       SQ1084.2
035900     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           SQ1084.2
036000     PERFORM WRITE-LINE.                                          SQ1084.2
036100 END-ROUTINE-13.                                                  SQ1084.2
036200     IF DELETE-CNT IS EQUAL TO ZERO                               SQ1084.2
036300         MOVE "NO " TO ERROR-TOTAL  ELSE                          SQ1084.2
036400         MOVE DELETE-CNT TO ERROR-TOTAL.                          SQ1084.2
036500     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   SQ1084.2
036600     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ1084.2
036700      IF   INSPECT-COUNTER EQUAL TO ZERO                          SQ1084.2
036800          MOVE "NO " TO ERROR-TOTAL                               SQ1084.2
036900      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   SQ1084.2
037000      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            SQ1084.2
037100      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          SQ1084.2
037200     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ1084.2
037300 WRITE-LINE.                                                      SQ1084.2
037400     ADD 1 TO RECORD-COUNT.                                       SQ1084.2
037500*    IF RECORD-COUNT GREATER 50                                   SQ1084.2
037600*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          SQ1084.2
037700*        MOVE SPACE TO DUMMY-RECORD                               SQ1084.2
037800*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  SQ1084.2
037900*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             SQ1084.2
038000*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     SQ1084.2
038100*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          SQ1084.2
038200*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          SQ1084.2
038300*        MOVE ZERO TO RECORD-COUNT.                               SQ1084.2
038400     PERFORM WRT-LN.                                              SQ1084.2
038500 WRT-LN.                                                          SQ1084.2
038600     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               SQ1084.2
038700     MOVE SPACE TO DUMMY-RECORD.                                  SQ1084.2
038800 BLANK-LINE-PRINT.                                                SQ1084.2
038900     PERFORM WRT-LN.                                              SQ1084.2
039000 FAIL-ROUTINE.                                                    SQ1084.2
039100     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.   SQ1084.2
039200     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.    SQ1084.2
039300     MOVE "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.    SQ1084.2
039400     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     SQ1084.2
039500     GO TO FAIL-ROUTINE-EX.                                       SQ1084.2
039600 FAIL-ROUTINE-WRITE.                                              SQ1084.2
039700     MOVE TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE           SQ1084.2
039800     MOVE TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES.   SQ1084.2
039900 FAIL-ROUTINE-EX. EXIT.                                           SQ1084.2
040000 BAIL-OUT.                                                        SQ1084.2
040100     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.       SQ1084.2
040200     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.               SQ1084.2
040300 BAIL-OUT-WRITE.                                                  SQ1084.2
040400     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  SQ1084.2
040500     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     SQ1084.2
040600 BAIL-OUT-EX. EXIT.                                               SQ1084.2
040700 CCVS1-EXIT.                                                      SQ1084.2
040800     EXIT.                                                        SQ1084.2
040900 SECT-SQ-108-0001 SECTION.                                        SQ1084.2
041000 SEQ-INIT-019.                                                    SQ1084.2
041100     MOVE "SQ-FS8" TO XFILE-NAME (1).                             SQ1084.2
041200     MOVE "R1-F-G" TO XRECORD-NAME (1).                           SQ1084.2
041300     MOVE CCVS-PGM-ID TO XPROGRAM-NAME (1).                       SQ1084.2
041400     MOVE 141 TO XRECORD-LENGTH (1).                              SQ1084.2
041500     MOVE "RC" TO CHARS-OR-RECORDS (1).                           SQ1084.2
041600     MOVE 1 TO XBLOCK-SIZE (1).                                   SQ1084.2
041700     MOVE 710 TO RECORDS-IN-FILE (1).                             SQ1084.2
041800     MOVE "SQ" TO XFILE-ORGANIZATION (1).                         SQ1084.2
041900     MOVE "O" TO XLABEL-TYPE (1).                                 SQ1084.2
042000     MOVE  0  TO NUMBER-AREA.                                     SQ1084.2
042100     MOVE "READ...INTO FILE " TO ALPHA-AREA.                      SQ1084.2
042200     OPEN OUTPUT SQ-FS8.                                          SQ1084.2
042300 SEQ-TEST-019.                                                    SQ1084.2
042400     ADD 1 TO NUMBER-AREA.                                        SQ1084.2
042500     MOVE NUMBER-AREA TO XRECORD-NUMBER (1).                      SQ1084.2
042600     MOVE FILE-RECORD-INFO-P1-120 (1) TO SQ-FS8R1-PART1.          SQ1084.2
042700     MOVE END-OF-RECORD-AREA TO SQ-FS8R1-PART2.                   SQ1084.2
042800     WRITE SQ-FS8R1-F-G-141.                                      SQ1084.2
042900     IF NUMBER-AREA EQUAL TO 710                                  SQ1084.2
043000         GO TO SEQ-WRITE-019.                                     SQ1084.2
043100     GO TO SEQ-TEST-019.                                          SQ1084.2
043200 SEQ-WRITE-019.                                                   SQ1084.2
043300     MOVE "CREATE FILE SQ-FS8" TO FEATURE.                        SQ1084.2
043400     MOVE "SEQ-TEST-019" TO PAR-NAME.                             SQ1084.2
043500     MOVE "FILE CREATED, RECS =" TO COMPUTED-A.                   SQ1084.2
043600     MOVE NUMBER-AREA TO CORRECT-18V0.                            SQ1084.2
043700     PERFORM PRINT-DETAIL.                                        SQ1084.2
043800     CLOSE SQ-FS8.                                                SQ1084.2
043900*        A MASS STORAGE SEQUENTIAL FILE WITH 141 CHARACTER        SQ1084.2
044000*    RECORDS HAS BEEN CREATED.  THE FILE CONTAINS 710 RECORDS.    SQ1084.2
044100 RERAD-INIT-020.                                                  SQ1084.2
044200     MOVE ZERO TO COUNT-OF-RECS.                                  SQ1084.2
044300*        THIS TEST READS AND CHECKS THE FILE CREATED              SQ1084.2
044400*    IN RERAD-TEST-019.                                           SQ1084.2
044500     OPEN INPUT SQ-FS8.                                           SQ1084.2
044600 SEQ-TEST-020.                                                    SQ1084.2
044700     READ SQ-FS8 RECORD                                           SQ1084.2
044800         AT END GO TO SEQ-TEST-020-1.                             SQ1084.2
044900     ADD 1 TO COUNT-OF-RECS.                                      SQ1084.2
045000     IF COUNT-OF-RECS GREATER THAN 710                            SQ1084.2
045100           MOVE "MORE THAN 710 RECORDS" TO RE-MARK                SQ1084.2
045200           GO TO SEQ-FAIL-020.                                    SQ1084.2
045300     MOVE SQ-FS8R1-PART1 TO FILE-RECORD-INFO-P1-120 (1).          SQ1084.2
045400     MOVE SQ-FS8R1-PART2 TO END-OF-RECORD-AREA.                   SQ1084.2
045500     IF COUNT-OF-RECS NOT EQUAL TO NUMBER-AREA                    SQ1084.2
045600           GO TO SEQ-TEST-020-2.                                  SQ1084.2
045700     IF XFILE-NAME (1) NOT EQUAL TO "SQ-FS8"                      SQ1084.2
045800           GO TO SEQ-TEST-020-2.                                  SQ1084.2
045900     IF XLABEL-TYPE (1) NOT EQUAL TO "O"                          SQ1084.2
046000           GO TO SEQ-TEST-020-2.                                  SQ1084.2
046100     IF COUNT-OF-RECS NOT EQUAL TO XRECORD-NUMBER (1)             SQ1084.2
046200           GO TO SEQ-TEST-020-2.                                  SQ1084.2
046300     IF ALPHA-AREA     EQUAL TO "READ...INTO FILE "               SQ1084.2
046400           GO TO SEQ-TEST-020.                                    SQ1084.2
046500 SEQ-TEST-020-2.                                                  SQ1084.2
046600     ADD 1 TO RECORDS-IN-ERROR.                                   SQ1084.2
046700     GO TO SEQ-TEST-020.                                          SQ1084.2
046800 SEQ-TEST-020-1.                                                  SQ1084.2
046900     IF RECORDS-IN-ERROR EQUAL TO ZERO                            SQ1084.2
047000         GO TO SEQ-PASS-020.                                      SQ1084.2
047100     MOVE "ERRORS IN READING SQ-FS8" TO RE-MARK.                  SQ1084.2
047200 SEQ-FAIL-020.                                                    SQ1084.2
047300     MOVE "RECORDS IN ERROR =" TO COMPUTED-A.                     SQ1084.2
047400     MOVE RECORDS-IN-ERROR TO CORRECT-18V0.                       SQ1084.2
047500     PERFORM FAIL.                                                SQ1084.2
047600     GO TO SEQ-WRITE-020.                                         SQ1084.2
047700 SEQ-PASS-020.                                                    SQ1084.2
047800     PERFORM PASS.                                                SQ1084.2
047900     MOVE "FILE VERIFIED RECS =" TO COMPUTED-A.                   SQ1084.2
048000     MOVE COUNT-OF-RECS TO CORRECT-18V0.                          SQ1084.2
048100 SEQ-WRITE-020.                                                   SQ1084.2
048200     MOVE "SEQ-TEST-020"   TO PAR-NAME.                           SQ1084.2
048300     MOVE "VERIFY FILE SQ-FS8" TO FEATURE.                        SQ1084.2
048400     PERFORM PRINT-DETAIL.                                        SQ1084.2
048500 SEQ-CLOSE-020.                                                   SQ1084.2
048600     CLOSE SQ-FS8.                                                SQ1084.2
048700 READ-INIT-GF-01.                                                 SQ1084.2
048800     MOVE ZERO TO COUNT-OF-RECS.                                  SQ1084.2
048900     MOVE ZERO TO RECORDS-IN-ERROR.                               SQ1084.2
049000     MOVE ZERO TO ERROR-FLAG.                                     SQ1084.2
049100     MOVE ZERO TO EOF-FLAG.                                       SQ1084.2
049200     MOVE "READ 141 INTO 87    " TO FEATURE.                      SQ1084.2
049300     MOVE "READ...RECORD INTO...AT END  01 LEVEL" TO RE-MARK.     SQ1084.2
049400     MOVE "READ-TEST-GF-01" TO PAR-NAME.                          SQ1084.2
049500*        THIS TEST READS RECORDS OF 141 CHARACTERS INTO A         SQ1084.2
049600*    WORKING-STORAGE AREA OF 87 CHARACTERS AND CHECKS THE AREA    SQ1084.2
049700*    FOLLOWING TO ENSURE TRUNCATION TOOK PLACE.  OTHER FIELDS     SQ1084.2
049800*    IN THE RECORD AREA ARE ALSO CHECKED.                         SQ1084.2
049900     OPEN INPUT SQ-FS8.                                           SQ1084.2
050000 READ-TEST-GF-01.                                                 SQ1084.2
050100     MOVE SPACE TO FOLLOWS-AREA1.                                 SQ1084.2
050200     READ SQ-FS8 RECORD INTO READ-INTO-AREA1                      SQ1084.2
050300          AT END MOVE "UNEXPECTED EOF" TO COMPUTED-A              SQ1084.2
050400          MOVE 1 TO EOF-FLAG                                      SQ1084.2
050500          GO TO READ-FAIL-GF-01.                                  SQ1084.2
050600     ADD 1 TO COUNT-OF-RECS.                                      SQ1084.2
050700     IF COUNT-OF-RECS EQUAL TO 125                                SQ1084.2
050800          GO TO READ-TEST-GF-01-1.                                SQ1084.2
050900     IF FOLLOWS-AREA1 NOT EQUAL TO SPACE                          SQ1084.2
051000        MOVE "WORKING-STORAGE CLOBBERED" TO RE-MARK               SQ1084.2
051100        MOVE FOLLOWS-AREA1 TO COMPUTED-A                          SQ1084.2
051200        GO TO READ-FAIL-GF-01.                                    SQ1084.2
051300     MOVE SPACE TO CHARS-OR-RECORDS (1).                          SQ1084.2
051400     MOVE AREA1-1 TO FILE-RECORD-INFO-P1-120 (1).                 SQ1084.2
051500     IF CHARS-OR-RECORDS (1) EQUAL TO "RC"                        SQ1084.2
051600         MOVE "NO TRUNC ON READ" TO COMPUTED-A                    SQ1084.2
051700         GO TO READ-FAIL-GF-01.                                   SQ1084.2
051800     IF XFILE-NAME (1) NOT EQUAL TO "SQ-FS8"                      SQ1084.2
051900         ADD 1 TO RECORDS-IN-ERROR                                SQ1084.2
052000         GO TO READ-TEST-GF-01.                                   SQ1084.2
052100     IF XRECORD-NUMBER (1) NOT EQUAL TO COUNT-OF-RECS             SQ1084.2
052200         ADD 1 TO RECORDS-IN-ERROR.                               SQ1084.2
052300     GO TO READ-TEST-GF-01.                                       SQ1084.2
052400 READ-TEST-GF-01-1.                                               SQ1084.2
052500     IF RECORDS-IN-ERROR EQUAL TO 0                               SQ1084.2
052600         GO TO READ-PASS-GF-01.                                   SQ1084.2
052700 READ-FAIL-GF-01.                                                 SQ1084.2
052800     MOVE "ERRORS IN RECORD(S)" TO COMPUTED-A.                    SQ1084.2
052900     MOVE "VII-45; 4.4.3 (7), (8)                   " TO  RE-MARK.SQ1084.2
053000     PERFORM FAIL.                                                SQ1084.2
053100     GO TO READ-WRITE-GF-01.                                      SQ1084.2
053200 READ-PASS-GF-01.                                                 SQ1084.2
053300     PERFORM PASS.                                                SQ1084.2
053400 READ-WRITE-GF-01.                                                SQ1084.2
053500     PERFORM PRINT-DETAIL.                                        SQ1084.2
053600 READ-INIT-GF-02.                                                 SQ1084.2
053700     IF EOF-FLAG EQUAL TO 1                                       SQ1084.2
053800           GO TO SEQ-EOF-21.                                      SQ1084.2
053900     MOVE 0 TO ERROR-FLAG.                                        SQ1084.2
054000*        THIS TEST READS RECORDS OF 141 CHARACTERS INTO AN 02     SQ1084.2
054100*    LEVEL IDENTIFIER WITH PIC X(120).                            SQ1084.2
054200     MOVE "READ 141 INTO 120    " TO FEATURE.                     SQ1084.2
054300     MOVE "READ-TEST-GF-02" TO PAR-NAME.                          SQ1084.2
054400     MOVE "READ...INTO...AT END 02 LEVEL" TO RE-MARK.             SQ1084.2
054500 READ-TEST-GF-02.                                                 SQ1084.2
054600     MOVE SPACE TO FOLLOWS-AREA2.                                 SQ1084.2
054700     READ SQ-FS8 INTO AREA2-1                                     SQ1084.2
054800         AT END MOVE "UNEXPECTED EOF" TO COMPUTED-A               SQ1084.2
054900         MOVE 1 TO EOF-FLAG                                       SQ1084.2
055000         GO TO READ-FAIL-GF-02.                                   SQ1084.2
055100     ADD 1 TO COUNT-OF-RECS.                                      SQ1084.2
055200     IF COUNT-OF-RECS EQUAL TO 250                                SQ1084.2
055300         GO TO READ-TEST-GF-02-1.                                 SQ1084.2
055400     IF FOLLOWS-AREA2 NOT EQUAL TO SPACE                          SQ1084.2
055500         MOVE "WORKING-STORAGE CLOBBERED" TO RE-MARK              SQ1084.2
055600         MOVE FOLLOWS-AREA2 TO COMPUTED-A                         SQ1084.2
055700         GO TO READ-FAIL-GF-02.                                   SQ1084.2
055800     MOVE AREA2-1 TO FILE-RECORD-INFO-P1-120 (1).                 SQ1084.2
055900     IF XFILE-NAME (1) NOT EQUAL TO "SQ-FS8"                      SQ1084.2
056000         ADD 1 TO RECORDS-IN-ERROR                                SQ1084.2
056100         MOVE 1 TO ERROR-FLAG                                     SQ1084.2
056200         GO TO READ-TEST-GF-02.                                   SQ1084.2
056300     IF XRECORD-NUMBER (1) NOT EQUAL TO COUNT-OF-RECS             SQ1084.2
056400         ADD 1 TO RECORDS-IN-ERROR                                SQ1084.2
056500         MOVE 1 TO ERROR-FLAG.                                    SQ1084.2
056600     GO TO READ-TEST-GF-02.                                       SQ1084.2
056700 READ-TEST-GF-02-1.                                               SQ1084.2
056800     IF ERROR-FLAG EQUAL TO 0                                     SQ1084.2
056900         GO TO READ-PASS-GF-02.                                   SQ1084.2
057000 READ-FAIL-GF-02.                                                 SQ1084.2
057100     MOVE "VII-45; 4.4.3 (7) & (8)                  " TO  RE-MARK.SQ1084.2
057200     MOVE "ERRORS IN RECORD(S)" TO COMPUTED-A.                    SQ1084.2
057300     PERFORM FAIL.                                                SQ1084.2
057400     GO TO READ-WRITE-GF-02.                                      SQ1084.2
057500 READ-PASS-GF-02.                                                 SQ1084.2
057600     PERFORM PASS.                                                SQ1084.2
057700 READ-WRITE-GF-02.                                                SQ1084.2
057800     PERFORM PRINT-DETAIL.                                        SQ1084.2
057900 READ-INIT-GF-03.                                                 SQ1084.2
058000     IF EOF-FLAG EQUAL TO 1                                       SQ1084.2
058100         GO TO SEQ-EOF-21.                                        SQ1084.2
058200     MOVE 0 TO ERROR-FLAG.                                        SQ1084.2
058300     MOVE "READ 141 INTO 148 " TO FEATURE.                        SQ1084.2
058400     MOVE "READ-TEST-GF-03" TO PAR-NAME.                          SQ1084.2
058500     MOVE "READ...RECORD INTO...END 01 LEVEL" TO RE-MARK.         SQ1084.2
058600*        THIS TEST READS RECORDS OF 141 CHARACTERS INTO A WORKING-SQ1084.2
058700*    STORAGE RECORD OF 148 CHARACTERS.  THE LAST 7 CHARACTERS ARE SQ1084.2
058800*    TESTED TO ENSURE THAT SPACE FILLING ON THE RIGHT OCCURRED.   SQ1084.2
058900 READ-TEST-GF-03.                                                 SQ1084.2
059000     MOVE "ABCDEFG" TO AREA3-2.                                   SQ1084.2
059100     READ SQ-FS8 RECORD INTO READ-INTO-AREA3                      SQ1084.2
059200           END MOVE "UNEXPECTED EOF" TO COMPUTED-A                SQ1084.2
059300           MOVE 1 TO EOF-FLAG                                     SQ1084.2
059400           GO TO READ-FAIL-GF-03.                                 SQ1084.2
059500     ADD 1 TO COUNT-OF-RECS.                                      SQ1084.2
059600     IF COUNT-OF-RECS EQUAL TO 350                                SQ1084.2
059700           GO TO READ-TEST-GF-03-1.                               SQ1084.2
059800     IF AREA3-2 NOT EQUAL TO SPACE                                SQ1084.2
059900         MOVE "NO SPACE FILL" TO RE-MARK                          SQ1084.2
060000         MOVE AREA3-2 TO COMPUTED-A                               SQ1084.2
060100         GO TO READ-FAIL-GF-03.                                   SQ1084.2
060200     MOVE AREA3-1 TO FILE-RECORD-INFO-P1-120 (1).                 SQ1084.2
060300     IF XFILE-NAME (1) NOT EQUAL TO "SQ-FS8"                      SQ1084.2
060400         ADD 1 TO RECORDS-IN-ERROR                                SQ1084.2
060500         MOVE 1 TO ERROR-FLAG                                     SQ1084.2
060600         GO TO READ-TEST-GF-03.                                   SQ1084.2
060700     IF XRECORD-NUMBER (1) NOT EQUAL TO COUNT-OF-RECS             SQ1084.2
060800         ADD 1 TO RECORDS-IN-ERROR                                SQ1084.2
060900         MOVE 1 TO ERROR-FLAG                                     SQ1084.2
061000         GO TO READ-TEST-GF-03.                                   SQ1084.2
061100     IF XLABEL-TYPE (1) NOT EQUAL TO "O"                          SQ1084.2
061200         ADD 1 TO RECORDS-IN-ERROR                                SQ1084.2
061300         MOVE 1 TO ERROR-FLAG.                                    SQ1084.2
061400     GO TO READ-TEST-GF-03.                                       SQ1084.2
061500 READ-TEST-GF-03-1.                                               SQ1084.2
061600     IF ERROR-FLAG EQUAL TO 0                                     SQ1084.2
061700         GO TO READ-PASS-GF-03.                                   SQ1084.2
061800 READ-FAIL-GF-03.                                                 SQ1084.2
061900     MOVE "VII-45; 4.4.3 (7) & (8)                  " TO  RE-MARK.SQ1084.2
062000     MOVE "ERRORS IN RECORD(S)" TO COMPUTED-A.                    SQ1084.2
062100     PERFORM FAIL.                                                SQ1084.2
062200     GO TO READ-WRITE-GF-03.                                      SQ1084.2
062300 READ-PASS-GF-03.                                                 SQ1084.2
062400     PERFORM PASS.                                                SQ1084.2
062500 READ-WRITE-GF-03.                                                SQ1084.2
062600     PERFORM PRINT-DETAIL.                                        SQ1084.2
062700 READ-INIT-GF-04.                                                 SQ1084.2
062800     IF EOF-FLAG EQUAL TO 1                                       SQ1084.2
062900          GO TO SEQ-EOF-21.                                       SQ1084.2
063000     MOVE 0 TO ERROR-FLAG.                                        SQ1084.2
063100     MOVE "READ 141 INTO 141" TO FEATURE.                         SQ1084.2
063200     MOVE "READ-TEST-GF-04" TO PAR-NAME.                          SQ1084.2
063300     MOVE "READ...INTO...END  01 LEVEL" TO RE-MARK.               SQ1084.2
063400*        THIS TEST READS RECORDS OF 141 CHARACTERS INTO A         SQ1084.2
063500*    WORKING-STORAGE RECORD OF 141 CHARACTERS.                    SQ1084.2
063600 READ-TEST-GF-04.                                                 SQ1084.2
063700     READ SQ-FS8 INTO READ-INTO-AREA4                             SQ1084.2
063800          END MOVE "UNEXPECTED EOF" TO COMPUTED-A                 SQ1084.2
063900          MOVE 1 TO EOF-FLAG                                      SQ1084.2
064000          GO TO READ-FAIL-GF-04.                                  SQ1084.2
064100     ADD 1 TO COUNT-OF-RECS.                                      SQ1084.2
064200     IF COUNT-OF-RECS EQUAL TO 400                                SQ1084.2
064300         GO TO READ-TEST-GF-04-1.                                 SQ1084.2
064400     MOVE AREA4-2 TO END-OF-RECORD-AREA.                          SQ1084.2
064500     IF ALPHA-AREA NOT EQUAL TO "READ...INTO FILE "               SQ1084.2
064600           GO TO READ-FAIL-GF-04-1.                               SQ1084.2
064700     IF NUMBER-AREA NOT EQUAL TO COUNT-OF-RECS                    SQ1084.2
064800           GO TO READ-FAIL-GF-04-1.                               SQ1084.2
064900     MOVE AREA4-1 TO FILE-RECORD-INFO-P1-120 (1).                 SQ1084.2
065000     IF XFILE-NAME (1) NOT EQUAL TO "SQ-FS8"                      SQ1084.2
065100         GO TO READ-FAIL-GF-04-1.                                 SQ1084.2
065200     IF XRECORD-NUMBER (1) NOT EQUAL TO COUNT-OF-RECS             SQ1084.2
065300         GO TO READ-FAIL-GF-04-1.                                 SQ1084.2
065400     IF XLABEL-TYPE (1) NOT EQUAL TO "O"                          SQ1084.2
065500         GO TO READ-FAIL-GF-04-1.                                 SQ1084.2
065600     GO TO READ-TEST-GF-04.                                       SQ1084.2
065700 READ-FAIL-GF-04-1.                                               SQ1084.2
065800     ADD 1 TO RECORDS-IN-ERROR.                                   SQ1084.2
065900     MOVE 1 TO ERROR-FLAG.                                        SQ1084.2
066000     GO TO READ-TEST-GF-04.                                       SQ1084.2
066100 READ-TEST-GF-04-1.                                               SQ1084.2
066200     IF ERROR-FLAG EQUAL TO 0                                     SQ1084.2
066300         GO TO READ-PASS-GF-04.                                   SQ1084.2
066400 READ-FAIL-GF-04.                                                 SQ1084.2
066500     MOVE "ERRORS IN RECORD(S)" TO COMPUTED-A.                    SQ1084.2
066600     MOVE "VII-45; 4.4.3 (7) & (8)                  " TO  RE-MARK.SQ1084.2
066700     PERFORM FAIL.                                                SQ1084.2
066800     GO TO READ-WRITE-GF-04.                                      SQ1084.2
066900 READ-PASS-GF-04.                                                 SQ1084.2
067000     PERFORM PASS.                                                SQ1084.2
067100 READ-WRITE-GF-04.                                                SQ1084.2
067200     PERFORM PRINT-DETAIL.                                        SQ1084.2
067300 READ-INIT-GF-05.                                                 SQ1084.2
067400     IF EOF-FLAG EQUAL TO 1                                       SQ1084.2
067500         GO TO SEQ-EOF-21.                                        SQ1084.2
067600     MOVE 0 TO ERROR-FLAG.                                        SQ1084.2
067700     MOVE "READ 141 INTO 120" TO FEATURE.                         SQ1084.2
067800     MOVE "READ-TEST-GF-05" TO PAR-NAME.                          SQ1084.2
067900     MOVE "READ INTO SUBSCRIPTED DATA ITEM 05 LEVEL" TO RE-MARK.  SQ1084.2
068000*         THIS TEST READS A RECORD OF 141 CHARACTERS INTO A       SQ1084.2
068100*    SUBSCRIPTED DATA ITEM OF 120 CHARACTERS.                     SQ1084.2
068200 READ-TEST-GF-05.                                                 SQ1084.2
068300     READ SQ-FS8 RECORD INTO FILE-RECORD-INFO-P1-120 (1)          SQ1084.2
068400          AT END MOVE "UNEXPECTED EOF" TO COMPUTED-A              SQ1084.2
068500          MOVE 1 TO EOF-FLAG                                      SQ1084.2
068600          GO TO READ-FAIL-GF-05.                                  SQ1084.2
068700     ADD 1 TO COUNT-OF-RECS.                                      SQ1084.2
068800     IF COUNT-OF-RECS EQUAL TO 425                                SQ1084.2
068900          GO TO READ-TEST-GF-05-1.                                SQ1084.2
069000     IF XFILE-NAME (1) NOT EQUAL TO "SQ-FS8"                      SQ1084.2
069100          GO TO READ-FAIL-GF-05-1.                                SQ1084.2
069200     IF XRECORD-NUMBER (1) NOT EQUAL TO COUNT-OF-RECS             SQ1084.2
069300          GO TO READ-FAIL-GF-05-1.                                SQ1084.2
069400     IF XLABEL-TYPE (1) NOT EQUAL TO "O"                          SQ1084.2
069500          GO TO READ-FAIL-GF-05-1.                                SQ1084.2
069600     GO TO READ-TEST-GF-05.                                       SQ1084.2
069700 READ-FAIL-GF-05-1.                                               SQ1084.2
069800     ADD 1 TO RECORDS-IN-ERROR.                                   SQ1084.2
069900     MOVE 1 TO ERROR-FLAG.                                        SQ1084.2
070000     GO TO READ-TEST-GF-05.                                       SQ1084.2
070100 READ-TEST-GF-05-1.                                               SQ1084.2
070200     IF ERROR-FLAG EQUAL TO 0                                     SQ1084.2
070300         GO TO READ-PASS-GF-05.                                   SQ1084.2
070400     MOVE "ERRORS IN RECORD(S)" TO COMPUTED-A.                    SQ1084.2
070500 READ-FAIL-GF-05.                                                 SQ1084.2
070600     MOVE "VII-45; 4.4.3 (7) & (8)                  " TO  RE-MARK.SQ1084.2
070700     PERFORM FAIL.                                                SQ1084.2
070800     GO TO READ-WRITE-GF-05.                                      SQ1084.2
070900 READ-PASS-GF-05.                                                 SQ1084.2
071000     PERFORM PASS.                                                SQ1084.2
071100 READ-WRITE-GF-05.                                                SQ1084.2
071200     PERFORM PRINT-DETAIL.                                        SQ1084.2
071300 READ-INIT-GF-06.                                                 SQ1084.2
071400     IF EOF-FLAG EQUAL TO 1                                       SQ1084.2
071500         GO TO SEQ-EOF-21.                                        SQ1084.2
071600     MOVE 0 TO ERROR-FLAG.                                        SQ1084.2
071700     MOVE "READ 141 INTO 141" TO FEATURE.                         SQ1084.2
071800     MOVE "READ-TEST-GF-06" TO PAR-NAME.                          SQ1084.2
071900     MOVE "CHECK OF FD RECORD ON RD INTO 01 LEVEL" TO RE-MARK.    SQ1084.2
072000*        THIS TEST READS A RECORD INTO A WORKING-STORAGE AREA     SQ1084.2
072100*    AND CHECKS THE CONTENTS OF THE FD RECORD AREA TO ENSURE      SQ1084.2
072200*    THAT IT IS NOT AFFECTED BY THE INTO PHRASE.                  SQ1084.2
072300 READ-TEST-GF-06.                                                 SQ1084.2
072400     READ SQ-FS8 RECORD INTO READ-INTO-AREA4                      SQ1084.2
072500          AT END MOVE "UNEXPECTED EOF" TO COMPUTED-A              SQ1084.2
072600          MOVE 1 TO EOF-FLAG                                      SQ1084.2
072700          GO TO READ-FAIL-GF-06.                                  SQ1084.2
072800     ADD 1 TO COUNT-OF-RECS.                                      SQ1084.2
072900     IF COUNT-OF-RECS EQUAL TO 710                                SQ1084.2
073000         GO TO READ-TEST-GF-06-1.                                 SQ1084.2
073100     MOVE SQ-FS8R1-PART2 TO END-OF-RECORD-AREA.                   SQ1084.2
073200     IF ALPHA-AREA NOT EQUAL TO "READ...INTO FILE "               SQ1084.2
073300         GO TO READ-FAIL-GF-06-1.                                 SQ1084.2
073400     IF NUMBER-AREA NOT EQUAL TO COUNT-OF-RECS                    SQ1084.2
073500         GO TO READ-FAIL-GF-06-1.                                 SQ1084.2
073600     MOVE SQ-FS8R1-PART1 TO FILE-RECORD-INFO-P1-120 (1).          SQ1084.2
073700     IF XFILE-NAME (1) NOT EQUAL TO "SQ-FS8"                      SQ1084.2
073800         GO TO READ-FAIL-GF-06-1.                                 SQ1084.2
073900     IF XRECORD-NUMBER (1) NOT EQUAL TO COUNT-OF-RECS             SQ1084.2
074000         GO TO READ-FAIL-GF-06-1.                                 SQ1084.2
074100     IF XLABEL-TYPE (1) NOT EQUAL TO "O"                          SQ1084.2
074200         GO TO READ-FAIL-GF-06-1.                                 SQ1084.2
074300     GO TO READ-TEST-GF-06.                                       SQ1084.2
074400 READ-FAIL-GF-06-1.                                               SQ1084.2
074500     ADD 1 TO RECORDS-IN-ERROR.                                   SQ1084.2
074600     MOVE 1 TO ERROR-FLAG.                                        SQ1084.2
074700     GO TO READ-TEST-GF-06.                                       SQ1084.2
074800 READ-TEST-GF-06-1.                                               SQ1084.2
074900     IF ERROR-FLAG EQUAL TO 0                                     SQ1084.2
075000         GO TO READ-TEST-GF-06-2.                                 SQ1084.2
075100     MOVE "ERRORS IN RECORD(S)" TO COMPUTED-A.                    SQ1084.2
075200     GO TO READ-FAIL-GF-06.                                       SQ1084.2
075300 READ-TEST-GF-06-2.                                               SQ1084.2
075400     IF READ-INTO-AREA4 EQUAL TO SQ-FS8R1-F-G-141                 SQ1084.2
075500         GO TO READ-PASS-GF-06.                                   SQ1084.2
075600 READ-FAIL-GF-06.                                                 SQ1084.2
075700     MOVE "VII-45; 4.4.3 (7) & (8)                  " TO  RE-MARK.SQ1084.2
075800     PERFORM FAIL.                                                SQ1084.2
075900     GO TO READ-WRITE-GF-06.                                      SQ1084.2
076000 READ-PASS-GF-06.                                                 SQ1084.2
076100     PERFORM PASS.                                                SQ1084.2
076200 READ-WRITE-GF-06.                                                SQ1084.2
076300     PERFORM PRINT-DETAIL.                                        SQ1084.2
076400 SEQ-INIT-21.                                                     SQ1084.2
076500*        THIS TEST CHECKS IF ANY ERRORS WERE ENCOUNTERED ON THE   SQ1084.2
076600*    PRECEDING READS, AND READS THE FILE ONCE MORE EXPECTING      SQ1084.2
076700*    THE END CONDITION TO OCCUR.                                  SQ1084.2
076800     IF EOF-FLAG EQUAL TO 1                                       SQ1084.2
076900         GO TO SEQ-EOF-21.                                        SQ1084.2
077000 SEQ-TEST-21.                                                     SQ1084.2
077100     READ SQ-FS8 RECORD INTO READ-INTO-AREA4                      SQ1084.2
077200          AT END GO TO SEQ-TEST-21-1.                             SQ1084.2
077300     MOVE "MORE THAN 710 RECORDS" TO RE-MARK.                     SQ1084.2
077400     GO TO SEQ-FAIL-21.                                           SQ1084.2
077500 SEQ-TEST-21-1.                                                   SQ1084.2
077600     IF RECORDS-IN-ERROR NOT EQUAL TO 0                           SQ1084.2
077700          MOVE "RECORDS IN ERROR =" TO COMPUTED-A                 SQ1084.2
077800          MOVE RECORDS-IN-ERROR TO CORRECT-18V0                   SQ1084.2
077900          GO TO SEQ-FAIL-21.                                      SQ1084.2
078000 SEQ-PASS-21.                                                     SQ1084.2
078100     PERFORM PASS.                                                SQ1084.2
078200     GO TO SEQ-WRITE-21.                                          SQ1084.2
078300 SEQ-EOF-21.                                                      SQ1084.2
078400     MOVE "LESS THAN 710 RECORDS" TO RE-MARK.                     SQ1084.2
078500     MOVE "RECORDS READ =" TO COMPUTED-A.                         SQ1084.2
078600     MOVE COUNT-OF-RECS TO CORRECT-18V0.                          SQ1084.2
078700 SEQ-FAIL-21.                                                     SQ1084.2
078800     PERFORM FAIL.                                                SQ1084.2
078900 SEQ-WRITE-21.                                                    SQ1084.2
079000     MOVE "SEQ-TEST-21" TO PAR-NAME.                              SQ1084.2
079100     MOVE "READ SQ-FS8 INTO END" TO FEATURE.                      SQ1084.2
079200     PERFORM PRINT-DETAIL.                                        SQ1084.2
079300 SEQ-CLOSE-021.                                                   SQ1084.2
079400     CLOSE SQ-FS8.                                                SQ1084.2
079500 TERMINATE-ROUTINE.                                               SQ1084.2
079600     EXIT.                                                        SQ1084.2
079700 CCVS-EXIT SECTION.                                               SQ1084.2
079800 CCVS-999999.                                                     SQ1084.2
079900     GO TO CLOSE-FILES.                                           SQ1084.2
      *END-OF,SQ108A                                                                  
