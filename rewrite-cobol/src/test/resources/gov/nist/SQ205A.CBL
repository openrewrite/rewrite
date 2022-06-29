      *HEADER,COBOL,SQ205A                                                            
000100 IDENTIFICATION DIVISION.                                         SQ2054.2
000200 PROGRAM-ID.                                                      SQ2054.2
000300     SQ205A.                                                      SQ2054.2
000400****************************************************************  SQ2054.2
000500*                                                              *  SQ2054.2
000600*    VALIDATION FOR:-                                          *  SQ2054.2
000700*    " HIGH       ".                                              SQ2054.2
000800*                                                              *  SQ2054.2
000900*    CREATION DATE     /     VALIDATION DATE                   *  SQ2054.2
001000*    "4.2 ".                                                      SQ2054.2
001100*                                                              *  SQ2054.2
001200* THIS ROUTINE (OLD: SQ210)  TESTS THE USE STATEMENT WITH XFILE-NASQ2054.2
001300*    SERIES.  A MASS STORAGE AND TAPE FILE ARE CREATED AND THEN   SQ2054.2
001400*    READ.  AN AT END CONDITION IS USED TO CAUSE THE USE PROCEDURESQ2054.2
001500*    TO BE EXECUTED.  BOTH FILES HAVE A FILE STATUS CLAUSE IN THE SQ2054.2
001600*    SELECT CLAUSE IN THE FILE-CONTROL PARAGRAPH.                 SQ2054.2
001700 ENVIRONMENT DIVISION.                                            SQ2054.2
001800 CONFIGURATION SECTION.                                           SQ2054.2
001900 SOURCE-COMPUTER.                                                 SQ2054.2
002000     XXXXX082.                                                    SQ2054.2
002100 OBJECT-COMPUTER.                                                 SQ2054.2
002200     XXXXX083.                                                    SQ2054.2
002300 INPUT-OUTPUT SECTION.                                            SQ2054.2
002400 FILE-CONTROL.                                                    SQ2054.2
002500*    SELECT RAW-DATA   ASSIGN TO                                  SQ2054.2
002600*    XXXXX062                                                     SQ2054.2
002700*           ORGANIZATION IS INDEXED                               SQ2054.2
002800*           ACCESS MODE IS RANDOM                                 SQ2054.2
002900*           RECORD KEY IS RAW-DATA-KEY.                           SQ2054.2
003000     SELECT PRINT-FILE ASSIGN TO                                  SQ2054.2
003100     XXXXX055.                                                    SQ2054.2
003200     SELECT SQ-FS1 ASSIGN TO                                      SQ2054.2
003300     XXXXX001                                                     SQ2054.2
003400     ORGANIZATION SEQUENTIAL                                      SQ2054.2
003500     ACCESS SEQUENTIAL                                            SQ2054.2
003600     STATUS GRP-STATUS-KEY-1.                                     SQ2054.2
003700     SELECT SQ-FS2 ASSIGN TO                                      SQ2054.2
003800     XXXXX014                                                     SQ2054.2
003900     ORGANIZATION IS SEQUENTIAL                                   SQ2054.2
004000     FILE STATUS GRP-STATUS-KEY-2.                                SQ2054.2
004100 DATA DIVISION.                                                   SQ2054.2
004200 FILE SECTION.                                                    SQ2054.2
004300*                                                                 SQ2054.2
004400*FD  RAW-DATA.                                                    SQ2054.2
004500*                                                                 SQ2054.2
004600*01  RAW-DATA-SATZ.                                               SQ2054.2
004700*    05  RAW-DATA-KEY        PIC X(6).                            SQ2054.2
004800*    05  C-DATE              PIC 9(6).                            SQ2054.2
004900*    05  C-TIME              PIC 9(8).                            SQ2054.2
005000*    05  C-NO-OF-TESTS       PIC 99.                              SQ2054.2
005100*    05  C-OK                PIC 999.                             SQ2054.2
005200*    05  C-ALL               PIC 999.                             SQ2054.2
005300*    05  C-FAIL              PIC 999.                             SQ2054.2
005400*    05  C-DELETED           PIC 999.                             SQ2054.2
005500*    05  C-INSPECT           PIC 999.                             SQ2054.2
005600*    05  C-NOTE              PIC X(13).                           SQ2054.2
005700*    05  C-INDENT            PIC X.                               SQ2054.2
005800*    05  C-ABORT             PIC X(8).                            SQ2054.2
005900 FD  PRINT-FILE                                                   SQ2054.2
006000C    LABEL RECORDS                                                SQ2054.2
006100C    XXXXX084                                                     SQ2054.2
006200C    DATA RECORD IS PRINT-REC DUMMY-RECORD                        SQ2054.2
006300               .                                                  SQ2054.2
006400 01  PRINT-REC PICTURE X(120).                                    SQ2054.2
006500 01  DUMMY-RECORD PICTURE X(120).                                 SQ2054.2
006600 FD  SQ-FS1                                                       SQ2054.2
006700C    LABEL RECORD IS STANDARD                                     SQ2054.2
006800     BLOCK CONTAINS 5 RECORDS.                                    SQ2054.2
006900 01  SQ-FS1R1-F-G-120 PIC X(120).                                 SQ2054.2
007000 FD  SQ-FS2                                                       SQ2054.2
007100C    LABEL RECORD IS STANDARD                                     SQ2054.2
007200     BLOCK CONTAINS 5 RECORDS.                                    SQ2054.2
007300 01  SQ-FS2R1-F-G-120 PIC X(120).                                 SQ2054.2
007400 WORKING-STORAGE SECTION.                                         SQ2054.2
007500 77  SQ-FS1-ERRORS      PIC 999      VALUE ZERO.                  SQ2054.2
007600 77  SQ-FS2-ERRORS      PIC 999      VALUE ZERO.                  SQ2054.2
007700 77  SQ-FS1-EOF-STATUS  PIC 9        VALUE ZERO.                  SQ2054.2
007800 77  SQ-FS2-EOF-STATUS  PIC 9        VALUE ZERO.                  SQ2054.2
007900 77  WRK-RECORD-COUNT   PIC 999      VALUE ZERO.                  SQ2054.2
008000 77  RECORDS-IN-ERROR   PIC 999      VALUE ZERO.                  SQ2054.2
008100 01  COUNT-OF-RECS PIC 9999  VALUE 0.                             SQ2054.2
008200 01  GRP-STATUS-KEY-1.                                            SQ2054.2
008300     02 WRK-XN-00001-KEY-1   PIC XX.                              SQ2054.2
008400*    02 FILLER               PIC X.                               SQ2054.2
008500 01  GRP-STATUS-KEY-2.                                            SQ2054.2
008600     02 WRK-XN-00001-KEY-2   PIC XX.                              SQ2054.2
008700*    02 FILLER               PIC X.                               SQ2054.2
008800 01  FILE-RECORD-INFORMATION-REC.                                 SQ2054.2
008900     03 FILE-RECORD-INFO-SKELETON.                                SQ2054.2
009000        05 FILLER                 PICTURE X(48)       VALUE       SQ2054.2
009100             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  SQ2054.2
009200        05 FILLER                 PICTURE X(46)       VALUE       SQ2054.2
009300             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    SQ2054.2
009400        05 FILLER                 PICTURE X(26)       VALUE       SQ2054.2
009500             ",LFIL=000000,ORG=  ,LBLR= ".                        SQ2054.2
009600        05 FILLER                 PICTURE X(37)       VALUE       SQ2054.2
009700             ",RECKEY=                             ".             SQ2054.2
009800        05 FILLER                 PICTURE X(38)       VALUE       SQ2054.2
009900             ",ALTKEY1=                             ".            SQ2054.2
010000        05 FILLER                 PICTURE X(38)       VALUE       SQ2054.2
010100             ",ALTKEY2=                             ".            SQ2054.2
010200        05 FILLER                 PICTURE X(7)        VALUE SPACE.SQ2054.2
010300     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              SQ2054.2
010400        05 FILE-RECORD-INFO-P1-120.                               SQ2054.2
010500           07 FILLER              PIC X(5).                       SQ2054.2
010600           07 XFILE-NAME           PIC X(6).                      SQ2054.2
010700           07 FILLER              PIC X(8).                       SQ2054.2
010800           07 XRECORD-NAME         PIC X(6).                      SQ2054.2
010900           07 FILLER              PIC X(1).                       SQ2054.2
011000           07 REELUNIT-NUMBER     PIC 9(1).                       SQ2054.2
011100           07 FILLER              PIC X(7).                       SQ2054.2
011200           07 XRECORD-NUMBER       PIC 9(6).                      SQ2054.2
011300           07 FILLER              PIC X(6).                       SQ2054.2
011400           07 UPDATE-NUMBER       PIC 9(2).                       SQ2054.2
011500           07 FILLER              PIC X(5).                       SQ2054.2
011600           07 ODO-NUMBER          PIC 9(4).                       SQ2054.2
011700           07 FILLER              PIC X(5).                       SQ2054.2
011800           07 XPROGRAM-NAME        PIC X(5).                      SQ2054.2
011900           07 FILLER              PIC X(7).                       SQ2054.2
012000           07 XRECORD-LENGTH       PIC 9(6).                      SQ2054.2
012100           07 FILLER              PIC X(7).                       SQ2054.2
012200           07 CHARS-OR-RECORDS    PIC X(2).                       SQ2054.2
012300           07 FILLER              PIC X(1).                       SQ2054.2
012400           07 XBLOCK-SIZE          PIC 9(4).                      SQ2054.2
012500           07 FILLER              PIC X(6).                       SQ2054.2
012600           07 RECORDS-IN-FILE     PIC 9(6).                       SQ2054.2
012700           07 FILLER              PIC X(5).                       SQ2054.2
012800           07 XFILE-ORGANIZATION   PIC X(2).                      SQ2054.2
012900           07 FILLER              PIC X(6).                       SQ2054.2
013000           07 XLABEL-TYPE          PIC X(1).                      SQ2054.2
013100        05 FILE-RECORD-INFO-P121-240.                             SQ2054.2
013200           07 FILLER              PIC X(8).                       SQ2054.2
013300           07 XRECORD-KEY          PIC X(29).                     SQ2054.2
013400           07 FILLER              PIC X(9).                       SQ2054.2
013500           07 ALTERNATE-KEY1      PIC X(29).                      SQ2054.2
013600           07 FILLER              PIC X(9).                       SQ2054.2
013700           07 ALTERNATE-KEY2      PIC X(29).                      SQ2054.2
013800           07 FILLER              PIC X(7).                       SQ2054.2
013900 01  TEST-RESULTS.                                                SQ2054.2
014000     02 FILLER                    PICTURE X VALUE SPACE.          SQ2054.2
014100     02 FEATURE                   PICTURE X(20) VALUE SPACE.      SQ2054.2
014200     02 FILLER                    PICTURE X VALUE SPACE.          SQ2054.2
014300     02 P-OR-F                    PICTURE X(5) VALUE SPACE.       SQ2054.2
014400     02 FILLER                    PICTURE X  VALUE SPACE.         SQ2054.2
014500     02  PAR-NAME.                                                SQ2054.2
014600       03 FILLER PICTURE X(12) VALUE SPACE.                       SQ2054.2
014700       03  PARDOT-X PICTURE X  VALUE SPACE.                       SQ2054.2
014800       03 DOTVALUE PICTURE 99  VALUE ZERO.                        SQ2054.2
014900       03 FILLER PIC X(5) VALUE SPACE.                            SQ2054.2
015000     02 FILLER PIC X(10) VALUE SPACE.                             SQ2054.2
015100     02 RE-MARK PIC X(61).                                        SQ2054.2
015200 01  TEST-COMPUTED.                                               SQ2054.2
015300     02 FILLER PIC X(30) VALUE SPACE.                             SQ2054.2
015400     02 FILLER PIC X(17) VALUE "       COMPUTED=".                SQ2054.2
015500     02 COMPUTED-X.                                               SQ2054.2
015600     03 COMPUTED-A                PICTURE X(20) VALUE SPACE.      SQ2054.2
015700     03 COMPUTED-N REDEFINES COMPUTED-A PICTURE -9(9).9(9).       SQ2054.2
015800     03 COMPUTED-0V18 REDEFINES COMPUTED-A  PICTURE -.9(18).      SQ2054.2
015900     03 COMPUTED-4V14 REDEFINES COMPUTED-A  PICTURE -9(4).9(14).  SQ2054.2
016000     03 COMPUTED-14V4 REDEFINES COMPUTED-A  PICTURE -9(14).9(4).  SQ2054.2
016100     03       CM-18V0 REDEFINES COMPUTED-A.                       SQ2054.2
016200         04 COMPUTED-18V0                   PICTURE -9(18).       SQ2054.2
016300         04 FILLER                          PICTURE X.            SQ2054.2
016400     03 FILLER PIC X(50) VALUE SPACE.                             SQ2054.2
016500 01  TEST-CORRECT.                                                SQ2054.2
016600     02 FILLER PIC X(30) VALUE SPACE.                             SQ2054.2
016700     02 FILLER PIC X(17) VALUE "       CORRECT =".                SQ2054.2
016800     02 CORRECT-X.                                                SQ2054.2
016900     03 CORRECT-A                 PICTURE X(20) VALUE SPACE.      SQ2054.2
017000     03 CORRECT-N REDEFINES CORRECT-A PICTURE -9(9).9(9).         SQ2054.2
017100     03 CORRECT-0V18 REDEFINES CORRECT-A    PICTURE -.9(18).      SQ2054.2
017200     03 CORRECT-4V14 REDEFINES CORRECT-A    PICTURE -9(4).9(14).  SQ2054.2
017300     03 CORRECT-14V4 REDEFINES CORRECT-A    PICTURE -9(14).9(4).  SQ2054.2
017400     03      CR-18V0 REDEFINES CORRECT-A.                         SQ2054.2
017500         04 CORRECT-18V0                    PICTURE -9(18).       SQ2054.2
017600         04 FILLER                          PICTURE X.            SQ2054.2
017700     03 FILLER PIC X(50) VALUE SPACE.                             SQ2054.2
017800 01  CCVS-C-1.                                                    SQ2054.2
017900     02 FILLER PICTURE IS X(99) VALUE IS " FEATURE              PASQ2054.2
018000-    "SS  PARAGRAPH-NAME                                          SQ2054.2
018100-    "        REMARKS".                                           SQ2054.2
018200     02 FILLER PICTURE IS X(20) VALUE IS SPACE.                   SQ2054.2
018300 01  CCVS-C-2.                                                    SQ2054.2
018400     02 FILLER PICTURE IS X VALUE IS SPACE.                       SQ2054.2
018500     02 FILLER PICTURE IS X(6) VALUE IS "TESTED".                 SQ2054.2
018600     02 FILLER PICTURE IS X(15) VALUE IS SPACE.                   SQ2054.2
018700     02 FILLER PICTURE IS X(4) VALUE IS "FAIL".                   SQ2054.2
018800     02 FILLER PICTURE IS X(94) VALUE IS SPACE.                   SQ2054.2
018900 01  REC-SKL-SUB PICTURE 9(2) VALUE ZERO.                         SQ2054.2
019000 01  REC-CT PICTURE 99 VALUE ZERO.                                SQ2054.2
019100 01  DELETE-CNT                   PICTURE 999  VALUE ZERO.        SQ2054.2
019200 01  ERROR-COUNTER PICTURE IS 999 VALUE IS ZERO.                  SQ2054.2
019300 01  INSPECT-COUNTER PIC 999 VALUE ZERO.                          SQ2054.2
019400 01  PASS-COUNTER PIC 999 VALUE ZERO.                             SQ2054.2
019500 01  TOTAL-ERROR PIC 999 VALUE ZERO.                              SQ2054.2
019600 01  ERROR-HOLD PIC 999 VALUE ZERO.                               SQ2054.2
019700 01  DUMMY-HOLD PIC X(120) VALUE SPACE.                           SQ2054.2
019800 01  RECORD-COUNT PIC 9(5) VALUE ZERO.                            SQ2054.2
019900 01  CCVS-H-1.                                                    SQ2054.2
020000     02  FILLER   PICTURE X(27)  VALUE SPACE.                     SQ2054.2
020100     02 FILLER PICTURE X(67) VALUE                                SQ2054.2
020200     " FEDERAL SOFTWARE TESTING CENTER COBOL COMPILER VALIDATION  SQ2054.2
020300-    " SYSTEM".                                                   SQ2054.2
020400     02  FILLER     PICTURE X(26)  VALUE SPACE.                   SQ2054.2
020500 01  CCVS-H-2.                                                    SQ2054.2
020600     02 FILLER PICTURE X(52) VALUE IS                             SQ2054.2
020700     "CCVS85 FSTC COPY, NOT FOR DISTRIBUTION.".                   SQ2054.2
020800     02 FILLER PICTURE IS X(19) VALUE IS "TEST RESULTS SET-  ".   SQ2054.2
020900     02 TEST-ID PICTURE IS X(9).                                  SQ2054.2
021000     02 FILLER PICTURE IS X(40) VALUE IS SPACE.                   SQ2054.2
021100 01  CCVS-H-3.                                                    SQ2054.2
021200     02  FILLER PICTURE X(34) VALUE                               SQ2054.2
021300     " FOR OFFICIAL USE ONLY    ".                                SQ2054.2
021400     02  FILLER PICTURE X(58) VALUE                               SQ2054.2
021500     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ2054.2
021600     02  FILLER PICTURE X(28) VALUE                               SQ2054.2
021700     "  COPYRIGHT   1985 ".                                       SQ2054.2
021800 01  CCVS-E-1.                                                    SQ2054.2
021900     02 FILLER PICTURE IS X(52) VALUE IS SPACE.                   SQ2054.2
022000     02 FILLER PICTURE IS X(14) VALUE IS "END OF TEST-  ".        SQ2054.2
022100     02 ID-AGAIN PICTURE IS X(9).                                 SQ2054.2
022200     02 FILLER PICTURE X(45) VALUE IS                             SQ2054.2
022300     " NTIS DISTRIBUTION COBOL 85".                               SQ2054.2
022400 01  CCVS-E-2.                                                    SQ2054.2
022500     02  FILLER                   PICTURE X(31)  VALUE            SQ2054.2
022600     SPACE.                                                       SQ2054.2
022700     02  FILLER                   PICTURE X(21)  VALUE SPACE.     SQ2054.2
022800     02 CCVS-E-2-2.                                               SQ2054.2
022900         03 ERROR-TOTAL PICTURE IS XXX VALUE IS SPACE.            SQ2054.2
023000         03 FILLER PICTURE IS X VALUE IS SPACE.                   SQ2054.2
023100         03 ENDER-DESC PIC X(46) VALUE "ERRORS ENCOUNTERED".      SQ2054.2
023200 01  CCVS-E-3.                                                    SQ2054.2
023300     02  FILLER PICTURE X(22) VALUE                               SQ2054.2
023400     " FOR OFFICIAL USE ONLY".                                    SQ2054.2
023500     02  FILLER PICTURE X(12) VALUE SPACE.                        SQ2054.2
023600     02  FILLER PICTURE X(58) VALUE                               SQ2054.2
023700     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ2054.2
023800     02  FILLER PICTURE X(13) VALUE SPACE.                        SQ2054.2
023900     02 FILLER PIC X(15) VALUE " COPYRIGHT 1985".                 SQ2054.2
024000 01  CCVS-E-4.                                                    SQ2054.2
024100     02 CCVS-E-4-1 PIC XXX VALUE SPACE.                           SQ2054.2
024200     02 FILLER PIC XXXX VALUE " OF ".                             SQ2054.2
024300     02 CCVS-E-4-2 PIC XXX VALUE SPACE.                           SQ2054.2
024400     02 FILLER PIC X(40) VALUE                                    SQ2054.2
024500      "  TESTS WERE EXECUTED SUCCESSFULLY".                       SQ2054.2
024600 01  XXINFO.                                                      SQ2054.2
024700     02 FILLER PIC X(30) VALUE "        *** INFORMATION  ***".    SQ2054.2
024800     02 INFO-TEXT.                                                SQ2054.2
024900     04 FILLER PIC X(20) VALUE SPACE.                             SQ2054.2
025000     04 XXCOMPUTED PIC X(20).                                     SQ2054.2
025100     04 FILLER PIC X(5) VALUE SPACE.                              SQ2054.2
025200     04 XXCORRECT PIC X(20).                                      SQ2054.2
025300 01  HYPHEN-LINE.                                                 SQ2054.2
025400     02 FILLER PICTURE IS X VALUE IS SPACE.                       SQ2054.2
025500     02 FILLER PICTURE IS X(65) VALUE IS "************************SQ2054.2
025600-    "*****************************************".                 SQ2054.2
025700     02 FILLER PICTURE IS X(54) VALUE IS "************************SQ2054.2
025800-    "******************************".                            SQ2054.2
025900 01  CCVS-PGM-ID PIC X(6) VALUE                                   SQ2054.2
026000     "SQ205A".                                                    SQ2054.2
026100 PROCEDURE DIVISION.                                              SQ2054.2
026200 DECLARATIVES.                                                    SQ2054.2
026300 SEQ-USE SECTION.                                                 SQ2054.2
026400     USE AFTER EXCEPTION PROCEDURE ON SQ-FS1, SQ-FS2.             SQ2054.2
026500 SEQ-USE-001.                                                     SQ2054.2
026600     IF XFILE-NAME (1) NOT EQUAL TO "SQ-FS1" GO TO SEQ-USE-002.   SQ2054.2
026700     IF WRK-XN-00001-KEY-1 EQUAL TO "10"                          SQ2054.2
026800              MOVE 1 TO SQ-FS1-EOF-STATUS                         SQ2054.2
026900              GO TO SEQ-USE-EXIT.                                 SQ2054.2
027000     ADD 1 TO SQ-FS1-ERRORS.                                      SQ2054.2
027100     GO TO SEQ-USE-EXIT.                                          SQ2054.2
027200 SEQ-USE-002.                                                     SQ2054.2
027300     IF XFILE-NAME (1) NOT EQUAL TO "SQ-FS2" GO TO SEQ-USE-EXIT.  SQ2054.2
027400     IF WRK-XN-00001-KEY-2 EQUAL TO "10"                          SQ2054.2
027500              MOVE 1 TO SQ-FS2-EOF-STATUS                         SQ2054.2
027600              GO TO SEQ-USE-EXIT.                                 SQ2054.2
027700     ADD 1 TO SQ-FS2-ERRORS.                                      SQ2054.2
027800 SEQ-USE-EXIT.                                                    SQ2054.2
027900     EXIT.                                                        SQ2054.2
028000 END DECLARATIVES.                                                SQ2054.2
028100 CCVS1 SECTION.                                                   SQ2054.2
028200 OPEN-FILES.                                                      SQ2054.2
028300*    OPEN I-O RAW-DATA.                                           SQ2054.2
028400*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            SQ2054.2
028500*    READ RAW-DATA INVALID KEY GO TO END-E-1.                     SQ2054.2
028600*    MOVE "ABORTED " TO C-ABORT.                                  SQ2054.2
028700*    ADD 1 TO C-NO-OF-TESTS.                                      SQ2054.2
028800*    ACCEPT C-DATE  FROM DATE.                                    SQ2054.2
028900*    ACCEPT C-TIME  FROM TIME.                                    SQ2054.2
029000*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-1.             SQ2054.2
029100*END-E-1.                                                         SQ2054.2
029200*    CLOSE RAW-DATA.                                              SQ2054.2
029300     OPEN     OUTPUT PRINT-FILE.                                  SQ2054.2
029400     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   SQ2054.2
029500     MOVE    SPACE TO TEST-RESULTS.                               SQ2054.2
029600     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             SQ2054.2
029700     MOVE ZERO TO REC-SKL-SUB.                                    SQ2054.2
029800     PERFORM CCVS-INIT-FILE 9 TIMES.                              SQ2054.2
029900 CCVS-INIT-FILE.                                                  SQ2054.2
030000     ADD 1 TO REC-SKL-SUB.                                        SQ2054.2
030100     MOVE FILE-RECORD-INFO-SKELETON TO                            SQ2054.2
030200                  FILE-RECORD-INFO (REC-SKL-SUB).                 SQ2054.2
030300 CCVS-INIT-EXIT.                                                  SQ2054.2
030400     GO TO CCVS1-EXIT.                                            SQ2054.2
030500 CLOSE-FILES.                                                     SQ2054.2
030600     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   SQ2054.2
030700*    OPEN I-O RAW-DATA.                                           SQ2054.2
030800*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            SQ2054.2
030900*    READ RAW-DATA INVALID KEY GO TO END-E-2.                     SQ2054.2
031000*    MOVE "OK.     " TO C-ABORT.                                  SQ2054.2
031100*    MOVE PASS-COUNTER TO C-OK.                                   SQ2054.2
031200*    MOVE ERROR-HOLD   TO C-ALL.                                  SQ2054.2
031300*    MOVE ERROR-COUNTER TO C-FAIL.                                SQ2054.2
031400*    MOVE DELETE-CNT TO C-DELETED.                                SQ2054.2
031500*    MOVE INSPECT-COUNTER TO C-INSPECT.                           SQ2054.2
031600*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-2.             SQ2054.2
031700*END-E-2.                                                         SQ2054.2
031800*    CLOSE RAW-DATA.                                              SQ2054.2
031900 TERMINATE-CCVS.                                                  SQ2054.2
032000*    EXIT PROGRAM.                                                SQ2054.2
032100*TERMINATE-CALL.                                                  SQ2054.2
032200     STOP     RUN.                                                SQ2054.2
032300 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         SQ2054.2
032400 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           SQ2054.2
032500 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          SQ2054.2
032600 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-CNT.          SQ2054.2
032700     MOVE "****TEST DELETED****" TO RE-MARK.                      SQ2054.2
032800 PRINT-DETAIL.                                                    SQ2054.2
032900     IF REC-CT NOT EQUAL TO ZERO                                  SQ2054.2
033000             MOVE "." TO PARDOT-X                                 SQ2054.2
033100             MOVE REC-CT TO DOTVALUE.                             SQ2054.2
033200     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      SQ2054.2
033300     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               SQ2054.2
033400        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 SQ2054.2
033500          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 SQ2054.2
033600     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              SQ2054.2
033700     MOVE SPACE TO CORRECT-X.                                     SQ2054.2
033800     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         SQ2054.2
033900     MOVE     SPACE TO RE-MARK.                                   SQ2054.2
034000 HEAD-ROUTINE.                                                    SQ2054.2
034100     MOVE CCVS-H-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ2054.2
034200     MOVE CCVS-H-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.   SQ2054.2
034300     MOVE CCVS-H-3 TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.   SQ2054.2
034400 COLUMN-NAMES-ROUTINE.                                            SQ2054.2
034500     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ2054.2
034600     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ2054.2
034700     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ2054.2
034800 END-ROUTINE.                                                     SQ2054.2
034900     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.SQ2054.2
035000 END-RTN-EXIT.                                                    SQ2054.2
035100     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ2054.2
035200 END-ROUTINE-1.                                                   SQ2054.2
035300      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      SQ2054.2
035400      ERROR-HOLD. ADD DELETE-CNT TO ERROR-HOLD.                   SQ2054.2
035500      ADD PASS-COUNTER TO ERROR-HOLD.                             SQ2054.2
035600*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   SQ2054.2
035700      MOVE PASS-COUNTER TO CCVS-E-4-1.                            SQ2054.2
035800      MOVE ERROR-HOLD TO CCVS-E-4-2.                              SQ2054.2
035900      MOVE CCVS-E-4 TO CCVS-E-2-2.                                SQ2054.2
036000      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           SQ2054.2
036100  END-ROUTINE-12.                                                 SQ2054.2
036200      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        SQ2054.2
036300     IF       ERROR-COUNTER IS EQUAL TO ZERO                      SQ2054.2
036400         MOVE "NO " TO ERROR-TOTAL                                SQ2054.2
036500         ELSE                                                     SQ2054.2
036600         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       SQ2054.2
036700     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           SQ2054.2
036800     PERFORM WRITE-LINE.                                          SQ2054.2
036900 END-ROUTINE-13.                                                  SQ2054.2
037000     IF DELETE-CNT IS EQUAL TO ZERO                               SQ2054.2
037100         MOVE "NO " TO ERROR-TOTAL  ELSE                          SQ2054.2
037200         MOVE DELETE-CNT TO ERROR-TOTAL.                          SQ2054.2
037300     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   SQ2054.2
037400     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ2054.2
037500      IF   INSPECT-COUNTER EQUAL TO ZERO                          SQ2054.2
037600          MOVE "NO " TO ERROR-TOTAL                               SQ2054.2
037700      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   SQ2054.2
037800      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            SQ2054.2
037900      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          SQ2054.2
038000     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ2054.2
038100 WRITE-LINE.                                                      SQ2054.2
038200     ADD 1 TO RECORD-COUNT.                                       SQ2054.2
038300*    IF RECORD-COUNT GREATER 50                                   SQ2054.2
038400*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          SQ2054.2
038500*        MOVE SPACE TO DUMMY-RECORD                               SQ2054.2
038600*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  SQ2054.2
038700*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             SQ2054.2
038800*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     SQ2054.2
038900*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          SQ2054.2
039000*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          SQ2054.2
039100*        MOVE ZERO TO RECORD-COUNT.                               SQ2054.2
039200     PERFORM WRT-LN.                                              SQ2054.2
039300 WRT-LN.                                                          SQ2054.2
039400     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               SQ2054.2
039500     MOVE SPACE TO DUMMY-RECORD.                                  SQ2054.2
039600 BLANK-LINE-PRINT.                                                SQ2054.2
039700     PERFORM WRT-LN.                                              SQ2054.2
039800 FAIL-ROUTINE.                                                    SQ2054.2
039900     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.   SQ2054.2
040000     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.    SQ2054.2
040100     MOVE "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.    SQ2054.2
040200     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     SQ2054.2
040300     GO TO FAIL-ROUTINE-EX.                                       SQ2054.2
040400 FAIL-ROUTINE-WRITE.                                              SQ2054.2
040500     MOVE TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE           SQ2054.2
040600     MOVE TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES.   SQ2054.2
040700 FAIL-ROUTINE-EX. EXIT.                                           SQ2054.2
040800 BAIL-OUT.                                                        SQ2054.2
040900     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.       SQ2054.2
041000     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.               SQ2054.2
041100 BAIL-OUT-WRITE.                                                  SQ2054.2
041200     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  SQ2054.2
041300     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     SQ2054.2
041400 BAIL-OUT-EX. EXIT.                                               SQ2054.2
041500 CCVS1-EXIT.                                                      SQ2054.2
041600     EXIT.                                                        SQ2054.2
041700 SECT-SQ205A-0001 SECTION.                                        SQ2054.2
041800 WRITE-INIT-GF-01.                                                SQ2054.2
041900     MOVE "R1-F-G" TO XRECORD-NAME (1).                           SQ2054.2
042000     MOVE "SQ205"  TO XPROGRAM-NAME (1).                          SQ2054.2
042100     MOVE 0120     TO XRECORD-LENGTH (1).                         SQ2054.2
042200     MOVE "RC"     TO CHARS-OR-RECORDS (1).                       SQ2054.2
042300     MOVE 5        TO XBLOCK-SIZE (1).                            SQ2054.2
042400     MOVE 500      TO RECORDS-IN-FILE (1).                        SQ2054.2
042500     MOVE "SQ"     TO XFILE-ORGANIZATION (1).                     SQ2054.2
042600     MOVE "S"      TO XLABEL-TYPE (1).                            SQ2054.2
042700     MOVE 1        TO XRECORD-NUMBER (1).                         SQ2054.2
042800     OPEN OUTPUT SQ-FS1 , SQ-FS2.                                 SQ2054.2
042900 WRITE-TEST-GF-01-01.                                             SQ2054.2
043000     MOVE "SQ-FS1" TO XFILE-NAME (1).                             SQ2054.2
043100     MOVE FILE-RECORD-INFO-P1-120 (1) TO SQ-FS1R1-F-G-120.        SQ2054.2
043200     WRITE SQ-FS1R1-F-G-120.                                      SQ2054.2
043300     MOVE "SQ-FS2" TO XFILE-NAME (1).                             SQ2054.2
043400     MOVE FILE-RECORD-INFO-P1-120 (1) TO SQ-FS2R1-F-G-120.        SQ2054.2
043500     WRITE SQ-FS2R1-F-G-120.                                      SQ2054.2
043600     IF XRECORD-NUMBER (1) EQUAL TO 500                           SQ2054.2
043700              GO TO WRITE-TEST-GF-01-02.                          SQ2054.2
043800     ADD 1 TO XRECORD-NUMBER (1).                                 SQ2054.2
043900     GO TO WRITE-TEST-GF-01-01.                                   SQ2054.2
044000 WRITE-TEST-GF-01-02.                                             SQ2054.2
044100     MOVE "CREATE FILE SQ-FS1,2" TO FEATURE.                      SQ2054.2
044200     MOVE "WRITE-TEST-GF-01" TO PAR-NAME.                         SQ2054.2
044300     MOVE "FILE CREATED, RECS =" TO COMPUTED-A.                   SQ2054.2
044400     MOVE XRECORD-NUMBER (1) TO CORRECT-18V0.                     SQ2054.2
044500     PERFORM PRINT-DETAIL.                                        SQ2054.2
044600     CLOSE SQ-FS1 , SQ-FS2.                                       SQ2054.2
044700 READ-INIT-GF-01.                                                 SQ2054.2
044800     OPEN INPUT SQ-FS1.                                           SQ2054.2
044900 READ-TEST-GF-01-01.                                              SQ2054.2
045000     READ SQ-FS1.                                                 SQ2054.2
045100     IF SQ-FS1-EOF-STATUS EQUAL TO 1                              SQ2054.2
045200              GO TO READ-TEST-GF-01-02.                           SQ2054.2
045300     MOVE SQ-FS1R1-F-G-120 TO FILE-RECORD-INFO-P1-120 (1).        SQ2054.2
045400     ADD 1 TO WRK-RECORD-COUNT.                                   SQ2054.2
045500     IF WRK-RECORD-COUNT GREATER THAN 500                         SQ2054.2
045600              MOVE "MORE THAN 500 RECORDS" TO RE-MARK             SQ2054.2
045700              MOVE "RECORDS READ=" TO COMPUTED-A                  SQ2054.2
045800              MOVE WRK-RECORD-COUNT TO CORRECT-18V0               SQ2054.2
045900              GO TO READ-FAIL-GF-01.                              SQ2054.2
046000     IF WRK-RECORD-COUNT NOT EQUAL TO XRECORD-NUMBER (1)          SQ2054.2
046100              ADD 1 TO RECORDS-IN-ERROR                           SQ2054.2
046200              GO TO READ-TEST-GF-01-01.                           SQ2054.2
046300     IF XFILE-NAME (1) NOT EQUAL TO "SQ-FS1"                      SQ2054.2
046400              ADD 1 TO RECORDS-IN-ERROR                           SQ2054.2
046500              GO TO READ-TEST-GF-01-01.                           SQ2054.2
046600     IF XLABEL-TYPE (1) NOT EQUAL TO "S"                          SQ2054.2
046700              ADD 1 TO RECORDS-IN-ERROR.                          SQ2054.2
046800     GO TO READ-TEST-GF-01-01.                                    SQ2054.2
046900 READ-TEST-GF-01-02.                                              SQ2054.2
047000     IF WRK-RECORD-COUNT LESS THAN 500                            SQ2054.2
047100              MOVE "LESS THAN 500 RECORDS;VII-52 OR VII-44"       SQ2054.2
047200                  TO RE-MARK                                      SQ2054.2
047300              MOVE "RECORDS READ=" TO COMPUTED-A                  SQ2054.2
047400              MOVE WRK-RECORD-COUNT TO CORRECT-18V0               SQ2054.2
047500              GO TO READ-FAIL-GF-01.                              SQ2054.2
047600     IF SQ-FS1-ERRORS NOT EQUAL TO ZERO                           SQ2054.2
047700              MOVE "PERM/IMPL ERRORS ENCOUNTERED;VII-44 OR VII-52"SQ2054.2
047800                  TO RE-MARK                                      SQ2054.2
047900              MOVE "RECORDS IN ERROR=" TO COMPUTED-A              SQ2054.2
048000              MOVE SQ-FS1-ERRORS TO CORRECT-18V0                  SQ2054.2
048100              GO TO READ-FAIL-GF-01.                              SQ2054.2
048200     IF RECORDS-IN-ERROR NOT EQUAL TO ZERO                        SQ2054.2
048300              MOVE "ERRORS IN READING SQ-FS1; VII-44 OR VII-52"   SQ2054.2
048400                  TO RE-MARK                                      SQ2054.2
048500              MOVE "RECORDS IN ERROR=" TO COMPUTED-A              SQ2054.2
048600              MOVE RECORDS-IN-ERROR TO CORRECT-18V0               SQ2054.2
048700              GO TO READ-FAIL-GF-01.                              SQ2054.2
048800     GO TO READ-PASS-GF-01.                                       SQ2054.2
048900 READ-DELETE-GF-01.                                               SQ2054.2
049000     PERFORM DE-LETE.                                             SQ2054.2
049100     GO TO READ-WRITE-GF-01.                                      SQ2054.2
049200 READ-FAIL-GF-01.                                                 SQ2054.2
049300     PERFORM FAIL.                                                SQ2054.2
049400     GO TO READ-WRITE-GF-01.                                      SQ2054.2
049500 READ-PASS-GF-01.                                                 SQ2054.2
049600     PERFORM PASS.                                                SQ2054.2
049700     MOVE "FILE VERIFIED RECS =" TO COMPUTED-A.                   SQ2054.2
049800     MOVE WRK-RECORD-COUNT TO CORRECT-18V0.                       SQ2054.2
049900 READ-WRITE-GF-01.                                                SQ2054.2
050000     MOVE "READ-TEST-GF-01" TO PAR-NAME.                          SQ2054.2
050100     MOVE "VERIFY SQ-FS1;F-S:10" TO FEATURE.                      SQ2054.2
050200     PERFORM PRINT-DETAIL.                                        SQ2054.2
050300 READ-CLOSE-GF-01.                                                SQ2054.2
050400     CLOSE SQ-FS1.                                                SQ2054.2
050500 READ-INIT-GF-02.                                                 SQ2054.2
050600     MOVE ZERO TO WRK-RECORD-COUNT.                               SQ2054.2
050700     OPEN INPUT SQ-FS2.                                           SQ2054.2
050800 READ-TEST-GF-02-01.                                              SQ2054.2
050900     READ SQ-FS2 RECORD.                                          SQ2054.2
051000     IF SQ-FS2-EOF-STATUS EQUAL 1                                 SQ2054.2
051100              GO TO READ-TEST-GF-02-02.                           SQ2054.2
051200     MOVE SQ-FS2R1-F-G-120 TO FILE-RECORD-INFO-P1-120 (1).        SQ2054.2
051300     ADD 1 TO WRK-RECORD-COUNT.                                   SQ2054.2
051400     IF WRK-RECORD-COUNT GREATER THAN 500                         SQ2054.2
051500              MOVE "MORE THAN 500 RECORDS" TO RE-MARK             SQ2054.2
051600              MOVE "RECORDS READ =" TO COMPUTED-A                 SQ2054.2
051700              MOVE WRK-RECORD-COUNT TO CORRECT-18V0               SQ2054.2
051800              GO TO READ-FAIL-GF-02.                              SQ2054.2
051900     IF WRK-RECORD-COUNT NOT EQUAL TO XRECORD-NUMBER (1)          SQ2054.2
052000              ADD 1 TO RECORDS-IN-ERROR                           SQ2054.2
052100              GO TO READ-TEST-GF-02-01.                           SQ2054.2
052200     IF XFILE-NAME (1) NOT EQUAL TO "SQ-FS2"                      SQ2054.2
052300              ADD 1 TO RECORDS-IN-ERROR                           SQ2054.2
052400              GO TO READ-TEST-GF-02-01.                           SQ2054.2
052500     IF XLABEL-TYPE (1) NOT EQUAL TO "S"                          SQ2054.2
052600              ADD 1 TO RECORDS-IN-ERROR.                          SQ2054.2
052700     GO TO READ-TEST-GF-02-01.                                    SQ2054.2
052800 READ-TEST-GF-02-02.                                              SQ2054.2
052900     IF WRK-RECORD-COUNT LESS THAN 500                            SQ2054.2
053000              MOVE "LESS THAN 500 RECORDS; VII-44 OR VII-52"      SQ2054.2
053100                  TO RE-MARK                                      SQ2054.2
053200              MOVE "RECORDS READ =" TO COMPUTED-A                 SQ2054.2
053300              MOVE WRK-RECORD-COUNT TO CORRECT-18V0               SQ2054.2
053400              GO TO READ-FAIL-GF-02.                              SQ2054.2
053500     IF SQ-FS2-ERRORS NOT EQUAL TO ZERO                           SQ2054.2
053600              MOVE "PERM/IMPL ERRORS ENCOUNTERED;VII-44 OR -52"   SQ2054.2
053700                  TO RE-MARK                                      SQ2054.2
053800              MOVE "RECORDS IN ERROR =" TO COMPUTED-A             SQ2054.2
053900              MOVE SQ-FS2-ERRORS TO CORRECT-18V0                  SQ2054.2
054000              GO TO READ-FAIL-GF-02.                              SQ2054.2
054100     IF RECORDS-IN-ERROR NOT EQUAL TO ZERO                        SQ2054.2
054200              MOVE "ERRORS IN READING SQ-FS2; VII-44 OR VII-52"   SQ2054.2
054300                  TO RE-MARK                                      SQ2054.2
054400              MOVE "RECORDS IN ERROR =" TO COMPUTED-A             SQ2054.2
054500              MOVE RECORDS-IN-ERROR TO CORRECT-18V0               SQ2054.2
054600              GO TO READ-FAIL-GF-02.                              SQ2054.2
054700     GO TO READ-PASS-GF-02.                                       SQ2054.2
054800 READ-DELETE-GF-02.                                               SQ2054.2
054900     PERFORM DE-LETE.                                             SQ2054.2
055000     GO TO READ-WRITE-GF-02.                                      SQ2054.2
055100 READ-FAIL-GF-02.                                                 SQ2054.2
055200     PERFORM FAIL.                                                SQ2054.2
055300     GO TO READ-WRITE-GF-02.                                      SQ2054.2
055400 READ-PASS-GF-02.                                                 SQ2054.2
055500     PERFORM PASS.                                                SQ2054.2
055600     MOVE "FILE VERIFIED RECS =" TO COMPUTED-A.                   SQ2054.2
055700     MOVE WRK-RECORD-COUNT TO CORRECT-18V0.                       SQ2054.2
055800 READ-WRITE-GF-02.                                                SQ2054.2
055900     MOVE "READ-TEST-GF-02" TO PAR-NAME.                          SQ2054.2
056000     MOVE "VERIFY SQ-FS2;F-S:10" TO FEATURE.                      SQ2054.2
056100     PERFORM PRINT-DETAIL.                                        SQ2054.2
056200 READ-CLOSE-GF-02.                                                SQ2054.2
056300     CLOSE SQ-FS2.                                                SQ2054.2
056400 SQ205A-END-ROUTINE.                                              SQ2054.2
056500     MOVE "END OF SQ205A VALIDATION TESTS" TO PRINT-REC.          SQ2054.2
056600     WRITE PRINT-REC AFTER ADVANCING 1 LINE.                      SQ2054.2
056700 TERMINATE-SQ205A.                                                SQ2054.2
056800     EXIT.                                                        SQ2054.2
056900 CCVS-EXIT SECTION.                                               SQ2054.2
057000 CCVS-999999.                                                     SQ2054.2
057100     GO TO CLOSE-FILES.                                           SQ2054.2
      *END-OF,SQ205A                                                                  
