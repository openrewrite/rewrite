      *HEADER,COBOL,SQ219A                                                            
000100 IDENTIFICATION DIVISION.                                         SQ2194.2
000200 PROGRAM-ID.                                                      SQ2194.2
000300     SQ219A.                                                      SQ2194.2
000400****************************************************************  SQ2194.2
000500*                                                              *  SQ2194.2
000600*    VALIDATION FOR:-                                          *  SQ2194.2
000700*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ2194.2
000800*    USING CCVS85 VERSION 1.0 ISSUED IN JANUARY 1986.          *  SQ2194.2
000900*                                                              *  SQ2194.2
001000*    CREATION DATE     /     VALIDATION DATE                   *  SQ2194.2
001100*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ2194.2
001200*                                                              *  SQ2194.2
001300*       THIS ROUTINE CHECKS  THE                                  SQ2194.2
001400*               RECORD DELIMITER IS IMPLEMENTOR-NAME  LAUSE.      SQ2194.2
001500*                                                                 SQ2194.2
001600*            SEE  VII-13.                                         SQ2194.2
001700*                                                                 SQ2194.2
001800*                                                                 SQ2194.2
001900*       THIS ROUTINE BUILDS A SEQUENTIAL TAPE FILE WHICH CONTAINS SQ2194.2
002000*    BOTH 120 CHARACTER AND 151 CHARACTER RECORDS.  THE TAPE      SQ2194.2
002100*    CONSISTS OF 1 SHORT, 1 LONG, 10 SHORT, 100 LONG, AND 338     SQ2194.2
002200*    SHORT RECORDS FOR A TOTAL OF 450 RECORDS IN THE FILE.        SQ2194.2
002300*    THE TAPE IS READ AND FIELDS IN THE RECORDS ARE CHECKED       SQ2194.2
002400*    AGAINST THE EXPECTED VALUES.                                 SQ2194.2
002500*                                                                 SQ2194.2
002600*       AN INFORMATION SECTION AT THE END OF THE ROUTINE CHECKS   SQ2194.2
002700*    THE FIELD WHICH CONTAINS THE XRECORD-NUMBER.  THIS FIELD IS  SQ2194.2
002800*    PART OF A LONG RECORD ONLY.  IF THE XRECORD-NUMBER IS THERE  SQ2194.2
002900*    FOR A SHORT RECORD, IT MEANS THE MAXIMUM SIZE RECORD IS      SQ2194.2
003000*    ALWAYS WRITTEN.                                              SQ2194.2
003100 ENVIRONMENT DIVISION.                                            SQ2194.2
003200 CONFIGURATION SECTION.                                           SQ2194.2
003300 SOURCE-COMPUTER.                                                 SQ2194.2
003400     XXXXX082.                                                    SQ2194.2
003500 OBJECT-COMPUTER.                                                 SQ2194.2
003600     XXXXX083.                                                    SQ2194.2
003700 INPUT-OUTPUT SECTION.                                            SQ2194.2
003800 FILE-CONTROL.                                                    SQ2194.2
003900*    SELECT RAW-DATA   ASSIGN TO                                  SQ2194.2
004000*    XXXXX062                                                     SQ2194.2
004100*           ORGANIZATION IS INDEXED                               SQ2194.2
004200*           ACCESS MODE IS RANDOM                                 SQ2194.2
004300*           RECORD KEY IS RAW-DATA-KEY.                           SQ2194.2
004400     SELECT PRINT-FILE ASSIGN TO                                  SQ2194.2
004500     XXXXX055.                                                    SQ2194.2
004600     SELECT SQ-VS6 ASSIGN                                         SQ2194.2
004700     XXXXX001                                                     SQ2194.2
004800     RECORD DELIMITER                                             SQ2194.2
004900     XXXXX070                                                     SQ2194.2
005000     ORGANIZATION IS SEQUENTIAL.                                  SQ2194.2
005100 DATA DIVISION.                                                   SQ2194.2
005200 FILE SECTION.                                                    SQ2194.2
005300*                                                                 SQ2194.2
005400*FD  RAW-DATA.                                                    SQ2194.2
005500*                                                                 SQ2194.2
005600*01  RAW-DATA-SATZ.                                               SQ2194.2
005700*    05  RAW-DATA-KEY        PIC X(6).                            SQ2194.2
005800*    05  C-DATE              PIC 9(6).                            SQ2194.2
005900*    05  C-TIME              PIC 9(8).                            SQ2194.2
006000*    05  C-NO-OF-TESTS       PIC 99.                              SQ2194.2
006100*    05  C-OK                PIC 999.                             SQ2194.2
006200*    05  C-ALL               PIC 999.                             SQ2194.2
006300*    05  C-FAIL              PIC 999.                             SQ2194.2
006400*    05  C-DELETED           PIC 999.                             SQ2194.2
006500*    05  C-INSPECT           PIC 999.                             SQ2194.2
006600*    05  C-NOTE              PIC X(13).                           SQ2194.2
006700*    05  C-INDENT            PIC X.                               SQ2194.2
006800*    05  C-ABORT             PIC X(8).                            SQ2194.2
006900 FD  PRINT-FILE                                                   SQ2194.2
007000C    LABEL RECORDS                                                SQ2194.2
007100C    XXXXX084                                                     SQ2194.2
007200C    DATA RECORD IS PRINT-REC DUMMY-RECORD                        SQ2194.2
007300               .                                                  SQ2194.2
007400 01  PRINT-REC PICTURE X(120).                                    SQ2194.2
007500 01  DUMMY-RECORD PICTURE X(120).                                 SQ2194.2
007600                                                                  SQ2194.2
007700 FD  SQ-VS6                                                       SQ2194.2
007800     RECORD CONTAINS 120 TO 151 CHARACTERS                        SQ2194.2
007900C    LABEL RECORDS ARE STANDARD                                   SQ2194.2
008000C    DATA RECORDS ARE SQ-VS6R1-M-G-120  SQ-VS6R2-M-G-151          SQ2194.2
008100               .                                                  SQ2194.2
008200 01  SQ-VS6R1-M-G-120.                                            SQ2194.2
008300     02  SQ-VS6R1-FIRST PIC X(120).                               SQ2194.2
008400 01  SQ-VS6R2-M-G-151.                                            SQ2194.2
008500     02  SQ-VS6R2-FIRST  PIC X(120).                              SQ2194.2
008600     02  LONG-OR-SHORT  PIC X(5).                                 SQ2194.2
008700     02  SQ-VS6-RECNO  PIC X(5).                                  SQ2194.2
008800     02  SQ-VS6-FILLER  PIC X(21).                                SQ2194.2
008900 WORKING-STORAGE SECTION.                                         SQ2194.2
009000 01  SAVE-COUNT-OF-RECS  PIC X(5).                                SQ2194.2
009100 01  COUNT-OF-RECS  PIC S9(5) COMP.                               SQ2194.2
009200 01  RECORDS-IN-ERROR  PIC S9(5) COMP.                            SQ2194.2
009300 01  ERROR-FLAG PIC 9.                                            SQ2194.2
009400 01  EOF-FLAG  PIC 9.                                             SQ2194.2
009500 01  DUMP-AREA.                                                   SQ2194.2
009600     02  TYPE-OF-REC PICTURE X(5).                                SQ2194.2
009700     02  RECNO  PIC 9(5).                                         SQ2194.2
009800     02  REC-FILLER  PIC X(21).                                   SQ2194.2
009900 01  FILE-RECORD-INFORMATION-REC.                                 SQ2194.2
010000     03 FILE-RECORD-INFO-SKELETON.                                SQ2194.2
010100        05 FILLER                 PICTURE X(48)       VALUE       SQ2194.2
010200             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  SQ2194.2
010300        05 FILLER                 PICTURE X(46)       VALUE       SQ2194.2
010400             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    SQ2194.2
010500        05 FILLER                 PICTURE X(26)       VALUE       SQ2194.2
010600             ",LFIL=000000,ORG=  ,LBLR= ".                        SQ2194.2
010700        05 FILLER                 PICTURE X(37)       VALUE       SQ2194.2
010800             ",RECKEY=                             ".             SQ2194.2
010900        05 FILLER                 PICTURE X(38)       VALUE       SQ2194.2
011000             ",ALTKEY1=                             ".            SQ2194.2
011100        05 FILLER                 PICTURE X(38)       VALUE       SQ2194.2
011200             ",ALTKEY2=                             ".            SQ2194.2
011300        05 FILLER                 PICTURE X(7)        VALUE SPACE.SQ2194.2
011400     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              SQ2194.2
011500        05 FILE-RECORD-INFO-P1-120.                               SQ2194.2
011600           07 FILLER              PIC X(5).                       SQ2194.2
011700           07 XFILE-NAME           PIC X(6).                      SQ2194.2
011800           07 FILLER              PIC X(8).                       SQ2194.2
011900           07 XRECORD-NAME         PIC X(6).                      SQ2194.2
012000           07 FILLER              PIC X(1).                       SQ2194.2
012100           07 REELUNIT-NUMBER     PIC 9(1).                       SQ2194.2
012200           07 FILLER              PIC X(7).                       SQ2194.2
012300           07 XRECORD-NUMBER       PIC 9(6).                      SQ2194.2
012400           07 FILLER              PIC X(6).                       SQ2194.2
012500           07 UPDATE-NUMBER       PIC 9(2).                       SQ2194.2
012600           07 FILLER              PIC X(5).                       SQ2194.2
012700           07 ODO-NUMBER          PIC 9(4).                       SQ2194.2
012800           07 FILLER              PIC X(5).                       SQ2194.2
012900           07 XPROGRAM-NAME        PIC X(5).                      SQ2194.2
013000           07 FILLER              PIC X(7).                       SQ2194.2
013100           07 XRECORD-LENGTH       PIC 9(6).                      SQ2194.2
013200           07 FILLER              PIC X(7).                       SQ2194.2
013300           07 CHARS-OR-RECORDS    PIC X(2).                       SQ2194.2
013400           07 FILLER              PIC X(1).                       SQ2194.2
013500           07 XBLOCK-SIZE          PIC 9(4).                      SQ2194.2
013600           07 FILLER              PIC X(6).                       SQ2194.2
013700           07 RECORDS-IN-FILE     PIC 9(6).                       SQ2194.2
013800           07 FILLER              PIC X(5).                       SQ2194.2
013900           07 XFILE-ORGANIZATION   PIC X(2).                      SQ2194.2
014000           07 FILLER              PIC X(6).                       SQ2194.2
014100           07 XLABEL-TYPE          PIC X(1).                      SQ2194.2
014200        05 FILE-RECORD-INFO-P121-240.                             SQ2194.2
014300           07 FILLER              PIC X(8).                       SQ2194.2
014400           07 XRECORD-KEY          PIC X(29).                     SQ2194.2
014500           07 FILLER              PIC X(9).                       SQ2194.2
014600           07 ALTERNATE-KEY1      PIC X(29).                      SQ2194.2
014700           07 FILLER              PIC X(9).                       SQ2194.2
014800           07 ALTERNATE-KEY2      PIC X(29).                      SQ2194.2
014900           07 FILLER              PIC X(7).                       SQ2194.2
015000 01  TEST-RESULTS.                                                SQ2194.2
015100     02 FILLER                    PICTURE X VALUE SPACE.          SQ2194.2
015200     02 FEATURE                   PICTURE X(20) VALUE SPACE.      SQ2194.2
015300     02 FILLER                    PICTURE X VALUE SPACE.          SQ2194.2
015400     02 P-OR-F                    PICTURE X(5) VALUE SPACE.       SQ2194.2
015500     02 FILLER                    PICTURE X  VALUE SPACE.         SQ2194.2
015600     02  PAR-NAME.                                                SQ2194.2
015700       03 FILLER PICTURE X(12) VALUE SPACE.                       SQ2194.2
015800       03  PARDOT-X PICTURE X  VALUE SPACE.                       SQ2194.2
015900       03 DOTVALUE PICTURE 99  VALUE ZERO.                        SQ2194.2
016000       03 FILLER PIC X(5) VALUE SPACE.                            SQ2194.2
016100     02 FILLER PIC X(10) VALUE SPACE.                             SQ2194.2
016200     02 RE-MARK PIC X(61).                                        SQ2194.2
016300 01  TEST-COMPUTED.                                               SQ2194.2
016400     02 FILLER PIC X(30) VALUE SPACE.                             SQ2194.2
016500     02 FILLER PIC X(17) VALUE "       COMPUTED=".                SQ2194.2
016600     02 COMPUTED-X.                                               SQ2194.2
016700     03 COMPUTED-A                PICTURE X(20) VALUE SPACE.      SQ2194.2
016800     03 COMPUTED-N REDEFINES COMPUTED-A PICTURE -9(9).9(9).       SQ2194.2
016900     03 COMPUTED-0V18 REDEFINES COMPUTED-A  PICTURE -.9(18).      SQ2194.2
017000     03 COMPUTED-4V14 REDEFINES COMPUTED-A  PICTURE -9(4).9(14).  SQ2194.2
017100     03 COMPUTED-14V4 REDEFINES COMPUTED-A  PICTURE -9(14).9(4).  SQ2194.2
017200     03       CM-18V0 REDEFINES COMPUTED-A.                       SQ2194.2
017300         04 COMPUTED-18V0                   PICTURE -9(18).       SQ2194.2
017400         04 FILLER                          PICTURE X.            SQ2194.2
017500     03 FILLER PIC X(50) VALUE SPACE.                             SQ2194.2
017600 01  TEST-CORRECT.                                                SQ2194.2
017700     02 FILLER PIC X(30) VALUE SPACE.                             SQ2194.2
017800     02 FILLER PIC X(17) VALUE "       CORRECT =".                SQ2194.2
017900     02 CORRECT-X.                                                SQ2194.2
018000     03 CORRECT-A                 PICTURE X(20) VALUE SPACE.      SQ2194.2
018100     03 CORRECT-N REDEFINES CORRECT-A PICTURE -9(9).9(9).         SQ2194.2
018200     03 CORRECT-0V18 REDEFINES CORRECT-A    PICTURE -.9(18).      SQ2194.2
018300     03 CORRECT-4V14 REDEFINES CORRECT-A    PICTURE -9(4).9(14).  SQ2194.2
018400     03 CORRECT-14V4 REDEFINES CORRECT-A    PICTURE -9(14).9(4).  SQ2194.2
018500     03      CR-18V0 REDEFINES CORRECT-A.                         SQ2194.2
018600         04 CORRECT-18V0                    PICTURE -9(18).       SQ2194.2
018700         04 FILLER                          PICTURE X.            SQ2194.2
018800     03 FILLER PIC X(50) VALUE SPACE.                             SQ2194.2
018900 01  CCVS-C-1.                                                    SQ2194.2
019000     02 FILLER PICTURE IS X(99) VALUE IS " FEATURE              PASQ2194.2
019100-    "SS  PARAGRAPH-NAME                                          SQ2194.2
019200-    "        REMARKS".                                           SQ2194.2
019300     02 FILLER PICTURE IS X(20) VALUE IS SPACE.                   SQ2194.2
019400 01  CCVS-C-2.                                                    SQ2194.2
019500     02 FILLER PICTURE IS X VALUE IS SPACE.                       SQ2194.2
019600     02 FILLER PICTURE IS X(6) VALUE IS "TESTED".                 SQ2194.2
019700     02 FILLER PICTURE IS X(15) VALUE IS SPACE.                   SQ2194.2
019800     02 FILLER PICTURE IS X(4) VALUE IS "FAIL".                   SQ2194.2
019900     02 FILLER PICTURE IS X(94) VALUE IS SPACE.                   SQ2194.2
020000 01  REC-SKL-SUB PICTURE 9(2) VALUE ZERO.                         SQ2194.2
020100 01  REC-CT PICTURE 99 VALUE ZERO.                                SQ2194.2
020200 01  DELETE-CNT                   PICTURE 999  VALUE ZERO.        SQ2194.2
020300 01  ERROR-COUNTER PICTURE IS 999 VALUE IS ZERO.                  SQ2194.2
020400 01  INSPECT-COUNTER PIC 999 VALUE ZERO.                          SQ2194.2
020500 01  PASS-COUNTER PIC 999 VALUE ZERO.                             SQ2194.2
020600 01  TOTAL-ERROR PIC 999 VALUE ZERO.                              SQ2194.2
020700 01  ERROR-HOLD PIC 999 VALUE ZERO.                               SQ2194.2
020800 01  DUMMY-HOLD PIC X(120) VALUE SPACE.                           SQ2194.2
020900 01  RECORD-COUNT PIC 9(5) VALUE ZERO.                            SQ2194.2
021000 01  CCVS-H-1.                                                    SQ2194.2
021100     02  FILLER   PICTURE X(27)  VALUE SPACE.                     SQ2194.2
021200     02 FILLER PICTURE X(67) VALUE                                SQ2194.2
021300     " FEDERAL SOFTWARE TESTING CENTER COBOL COMPILER VALIDATION  SQ2194.2
021400-    " SYSTEM".                                                   SQ2194.2
021500     02  FILLER     PICTURE X(26)  VALUE SPACE.                   SQ2194.2
021600 01  CCVS-H-2.                                                    SQ2194.2
021700     02 FILLER PICTURE X(52) VALUE IS                             SQ2194.2
021800     "CCVS85 FSTC COPY, NOT FOR DISTRIBUTION.".                   SQ2194.2
021900     02 FILLER PICTURE IS X(19) VALUE IS "TEST RESULTS SET-  ".   SQ2194.2
022000     02 TEST-ID PICTURE IS X(9).                                  SQ2194.2
022100     02 FILLER PICTURE IS X(40) VALUE IS SPACE.                   SQ2194.2
022200 01  CCVS-H-3.                                                    SQ2194.2
022300     02  FILLER PICTURE X(34) VALUE                               SQ2194.2
022400     " FOR OFFICIAL USE ONLY    ".                                SQ2194.2
022500     02  FILLER PICTURE X(58) VALUE                               SQ2194.2
022600     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ2194.2
022700     02  FILLER PICTURE X(28) VALUE                               SQ2194.2
022800     "  COPYRIGHT   1985 ".                                       SQ2194.2
022900 01  CCVS-E-1.                                                    SQ2194.2
023000     02 FILLER PICTURE IS X(52) VALUE IS SPACE.                   SQ2194.2
023100     02 FILLER PICTURE IS X(14) VALUE IS "END OF TEST-  ".        SQ2194.2
023200     02 ID-AGAIN PICTURE IS X(9).                                 SQ2194.2
023300     02 FILLER PICTURE X(45) VALUE IS                             SQ2194.2
023400     " NTIS DISTRIBUTION COBOL 85".                               SQ2194.2
023500 01  CCVS-E-2.                                                    SQ2194.2
023600     02  FILLER                   PICTURE X(31)  VALUE            SQ2194.2
023700     SPACE.                                                       SQ2194.2
023800     02  FILLER                   PICTURE X(21)  VALUE SPACE.     SQ2194.2
023900     02 CCVS-E-2-2.                                               SQ2194.2
024000         03 ERROR-TOTAL PICTURE IS XXX VALUE IS SPACE.            SQ2194.2
024100         03 FILLER PICTURE IS X VALUE IS SPACE.                   SQ2194.2
024200         03 ENDER-DESC PIC X(46) VALUE "ERRORS ENCOUNTERED".      SQ2194.2
024300 01  CCVS-E-3.                                                    SQ2194.2
024400     02  FILLER PICTURE X(22) VALUE                               SQ2194.2
024500     " FOR OFFICIAL USE ONLY".                                    SQ2194.2
024600     02  FILLER PICTURE X(12) VALUE SPACE.                        SQ2194.2
024700     02  FILLER PICTURE X(58) VALUE                               SQ2194.2
024800     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ2194.2
024900     02  FILLER PICTURE X(13) VALUE SPACE.                        SQ2194.2
025000     02 FILLER PIC X(15) VALUE " COPYRIGHT 1985".                 SQ2194.2
025100 01  CCVS-E-4.                                                    SQ2194.2
025200     02 CCVS-E-4-1 PIC XXX VALUE SPACE.                           SQ2194.2
025300     02 FILLER PIC XXXX VALUE " OF ".                             SQ2194.2
025400     02 CCVS-E-4-2 PIC XXX VALUE SPACE.                           SQ2194.2
025500     02 FILLER PIC X(40) VALUE                                    SQ2194.2
025600      "  TESTS WERE EXECUTED SUCCESSFULLY".                       SQ2194.2
025700 01  XXINFO.                                                      SQ2194.2
025800     02 FILLER PIC X(30) VALUE "        *** INFORMATION  ***".    SQ2194.2
025900     02 INFO-TEXT.                                                SQ2194.2
026000     04 FILLER PIC X(20) VALUE SPACE.                             SQ2194.2
026100     04 XXCOMPUTED PIC X(20).                                     SQ2194.2
026200     04 FILLER PIC X(5) VALUE SPACE.                              SQ2194.2
026300     04 XXCORRECT PIC X(20).                                      SQ2194.2
026400 01  HYPHEN-LINE.                                                 SQ2194.2
026500     02 FILLER PICTURE IS X VALUE IS SPACE.                       SQ2194.2
026600     02 FILLER PICTURE IS X(65) VALUE IS "************************SQ2194.2
026700-    "*****************************************".                 SQ2194.2
026800     02 FILLER PICTURE IS X(54) VALUE IS "************************SQ2194.2
026900-    "******************************".                            SQ2194.2
027000 01  CCVS-PGM-ID PIC X(6) VALUE                                   SQ2194.2
027100     "SQ219A".                                                    SQ2194.2
027200 PROCEDURE DIVISION.                                              SQ2194.2
027300 CCVS1 SECTION.                                                   SQ2194.2
027400 OPEN-FILES.                                                      SQ2194.2
027500*    OPEN I-O RAW-DATA.                                           SQ2194.2
027600*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            SQ2194.2
027700*    READ RAW-DATA INVALID KEY GO TO END-E-1.                     SQ2194.2
027800*    MOVE "ABORTED " TO C-ABORT.                                  SQ2194.2
027900*    ADD 1 TO C-NO-OF-TESTS.                                      SQ2194.2
028000*    ACCEPT C-DATE  FROM DATE.                                    SQ2194.2
028100*    ACCEPT C-TIME  FROM TIME.                                    SQ2194.2
028200*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-1.             SQ2194.2
028300*END-E-1.                                                         SQ2194.2
028400*    CLOSE RAW-DATA.                                              SQ2194.2
028500     OPEN     OUTPUT PRINT-FILE.                                  SQ2194.2
028600     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   SQ2194.2
028700     MOVE    SPACE TO TEST-RESULTS.                               SQ2194.2
028800     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             SQ2194.2
028900     MOVE ZERO TO REC-SKL-SUB.                                    SQ2194.2
029000     PERFORM CCVS-INIT-FILE 9 TIMES.                              SQ2194.2
029100 CCVS-INIT-FILE.                                                  SQ2194.2
029200     ADD 1 TO REC-SKL-SUB.                                        SQ2194.2
029300     MOVE FILE-RECORD-INFO-SKELETON TO                            SQ2194.2
029400                  FILE-RECORD-INFO (REC-SKL-SUB).                 SQ2194.2
029500 CCVS-INIT-EXIT.                                                  SQ2194.2
029600     GO TO CCVS1-EXIT.                                            SQ2194.2
029700 CLOSE-FILES.                                                     SQ2194.2
029800     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   SQ2194.2
029900*    OPEN I-O RAW-DATA.                                           SQ2194.2
030000*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            SQ2194.2
030100*    READ RAW-DATA INVALID KEY GO TO END-E-2.                     SQ2194.2
030200*    MOVE "OK.     " TO C-ABORT.                                  SQ2194.2
030300*    MOVE PASS-COUNTER TO C-OK.                                   SQ2194.2
030400*    MOVE ERROR-HOLD   TO C-ALL.                                  SQ2194.2
030500*    MOVE ERROR-COUNTER TO C-FAIL.                                SQ2194.2
030600*    MOVE DELETE-CNT TO C-DELETED.                                SQ2194.2
030700*    MOVE INSPECT-COUNTER TO C-INSPECT.                           SQ2194.2
030800*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-2.             SQ2194.2
030900*END-E-2.                                                         SQ2194.2
031000*    CLOSE RAW-DATA.                                              SQ2194.2
031100 TERMINATE-CCVS.                                                  SQ2194.2
031200*    EXIT PROGRAM.                                                SQ2194.2
031300*TERMINATE-CALL.                                                  SQ2194.2
031400     STOP     RUN.                                                SQ2194.2
031500 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         SQ2194.2
031600 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           SQ2194.2
031700 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          SQ2194.2
031800 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-CNT.          SQ2194.2
031900     MOVE "****TEST DELETED****" TO RE-MARK.                      SQ2194.2
032000 PRINT-DETAIL.                                                    SQ2194.2
032100     IF REC-CT NOT EQUAL TO ZERO                                  SQ2194.2
032200             MOVE "." TO PARDOT-X                                 SQ2194.2
032300             MOVE REC-CT TO DOTVALUE.                             SQ2194.2
032400     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      SQ2194.2
032500     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               SQ2194.2
032600        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 SQ2194.2
032700          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 SQ2194.2
032800     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              SQ2194.2
032900     MOVE SPACE TO CORRECT-X.                                     SQ2194.2
033000     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         SQ2194.2
033100     MOVE     SPACE TO RE-MARK.                                   SQ2194.2
033200 HEAD-ROUTINE.                                                    SQ2194.2
033300     MOVE CCVS-H-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ2194.2
033400     MOVE CCVS-H-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.   SQ2194.2
033500     MOVE CCVS-H-3 TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.   SQ2194.2
033600 COLUMN-NAMES-ROUTINE.                                            SQ2194.2
033700     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ2194.2
033800     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ2194.2
033900     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ2194.2
034000 END-ROUTINE.                                                     SQ2194.2
034100     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.SQ2194.2
034200 END-RTN-EXIT.                                                    SQ2194.2
034300     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ2194.2
034400 END-ROUTINE-1.                                                   SQ2194.2
034500      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      SQ2194.2
034600      ERROR-HOLD. ADD DELETE-CNT TO ERROR-HOLD.                   SQ2194.2
034700      ADD PASS-COUNTER TO ERROR-HOLD.                             SQ2194.2
034800*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   SQ2194.2
034900      MOVE PASS-COUNTER TO CCVS-E-4-1.                            SQ2194.2
035000      MOVE ERROR-HOLD TO CCVS-E-4-2.                              SQ2194.2
035100      MOVE CCVS-E-4 TO CCVS-E-2-2.                                SQ2194.2
035200      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           SQ2194.2
035300  END-ROUTINE-12.                                                 SQ2194.2
035400      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        SQ2194.2
035500     IF       ERROR-COUNTER IS EQUAL TO ZERO                      SQ2194.2
035600         MOVE "NO " TO ERROR-TOTAL                                SQ2194.2
035700         ELSE                                                     SQ2194.2
035800         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       SQ2194.2
035900     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           SQ2194.2
036000     PERFORM WRITE-LINE.                                          SQ2194.2
036100 END-ROUTINE-13.                                                  SQ2194.2
036200     IF DELETE-CNT IS EQUAL TO ZERO                               SQ2194.2
036300         MOVE "NO " TO ERROR-TOTAL  ELSE                          SQ2194.2
036400         MOVE DELETE-CNT TO ERROR-TOTAL.                          SQ2194.2
036500     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   SQ2194.2
036600     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ2194.2
036700      IF   INSPECT-COUNTER EQUAL TO ZERO                          SQ2194.2
036800          MOVE "NO " TO ERROR-TOTAL                               SQ2194.2
036900      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   SQ2194.2
037000      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            SQ2194.2
037100      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          SQ2194.2
037200     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ2194.2
037300 WRITE-LINE.                                                      SQ2194.2
037400     ADD 1 TO RECORD-COUNT.                                       SQ2194.2
037500*    IF RECORD-COUNT GREATER 50                                   SQ2194.2
037600*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          SQ2194.2
037700*        MOVE SPACE TO DUMMY-RECORD                               SQ2194.2
037800*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  SQ2194.2
037900*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             SQ2194.2
038000*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     SQ2194.2
038100*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          SQ2194.2
038200*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          SQ2194.2
038300*        MOVE ZERO TO RECORD-COUNT.                               SQ2194.2
038400     PERFORM WRT-LN.                                              SQ2194.2
038500 WRT-LN.                                                          SQ2194.2
038600     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               SQ2194.2
038700     MOVE SPACE TO DUMMY-RECORD.                                  SQ2194.2
038800 BLANK-LINE-PRINT.                                                SQ2194.2
038900     PERFORM WRT-LN.                                              SQ2194.2
039000 FAIL-ROUTINE.                                                    SQ2194.2
039100     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.   SQ2194.2
039200     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.    SQ2194.2
039300     MOVE "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.    SQ2194.2
039400     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     SQ2194.2
039500     GO TO FAIL-ROUTINE-EX.                                       SQ2194.2
039600 FAIL-ROUTINE-WRITE.                                              SQ2194.2
039700     MOVE TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE           SQ2194.2
039800     MOVE TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES.   SQ2194.2
039900 FAIL-ROUTINE-EX. EXIT.                                           SQ2194.2
040000 BAIL-OUT.                                                        SQ2194.2
040100     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.       SQ2194.2
040200     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.               SQ2194.2
040300 BAIL-OUT-WRITE.                                                  SQ2194.2
040400     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  SQ2194.2
040500     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     SQ2194.2
040600 BAIL-OUT-EX. EXIT.                                               SQ2194.2
040700 CCVS1-EXIT.                                                      SQ2194.2
040800     EXIT.                                                        SQ2194.2
040900 SECT-SQ219A-0001 SECTION.                                        SQ2194.2
041000 WRITE-INIT-GF-01.                                                SQ2194.2
041100     MOVE "SQ-VS6" TO XFILE-NAME (1).                             SQ2194.2
041200     MOVE CCVS-PGM-ID TO XPROGRAM-NAME (1).                       SQ2194.2
041300     MOVE "RC" TO CHARS-OR-RECORDS (1).                           SQ2194.2
041400     MOVE 0001 TO XBLOCK-SIZE (1).                                SQ2194.2
041500     MOVE 000450 TO RECORDS-IN-FILE (1).                          SQ2194.2
041600     MOVE "SQ" TO XFILE-ORGANIZATION (1).                         SQ2194.2
041700     MOVE "S" TO XLABEL-TYPE (1).                                 SQ2194.2
041800     MOVE 000000 TO XRECORD-NUMBER (1).                           SQ2194.2
041900     MOVE ZERO TO COUNT-OF-RECS.                                  SQ2194.2
042000     OPEN OUTPUT SQ-VS6.                                          SQ2194.2
042100     MOVE "MULTIPLE LENGTH RECS " TO SQ-VS6-FILLER.               SQ2194.2
042200 WRITE-TEST-GF-01.                                                SQ2194.2
042300     PERFORM WRITE-SHORT-REC.                                     SQ2194.2
042400     PERFORM WRITE-LONG-REC.                                      SQ2194.2
042500     PERFORM WRITE-SHORT-REC 10 TIMES.                            SQ2194.2
042600     PERFORM WRITE-LONG-REC 100 TIMES.                            SQ2194.2
042700     PERFORM WRITE-SHORT-REC 338 TIMES.                           SQ2194.2
042800 WRITE-WRITE-GF-01.                                               SQ2194.2
042900     MOVE "CREATE FILE SQ-VS6" TO FEATURE.                        SQ2194.2
043000     MOVE "WRITE-TEST-GF-01" TO PAR-NAME.                         SQ2194.2
043100     MOVE "FILE CREATED, RECS =" TO COMPUTED-A.                   SQ2194.2
043200     MOVE COUNT-OF-RECS TO CORRECT-18V0.                          SQ2194.2
043300     MOVE "FILE HAS 120 AND 151 CHAR RECS" TO RE-MARK.            SQ2194.2
043400     PERFORM PRINT-DETAIL.                                        SQ2194.2
043500*         A SEQUENTIAL TAPE FILE CONTAINING 450 RECORDS HAS       SQ2194.2
043600*    BEEN CREATED.  THE FILE CONTAINS RECORDS OF 120 CHARACTERS   SQ2194.2
043700*    AND RECORDS OF 151 CHARACTERS.  THE SEQUENCE IN WHICH THE    SQ2194.2
043800*    RECORDS WERE WRITTEN IS S-L-10S-100L-338S.                   SQ2194.2
043900 WRITE-CLOSE-GF-01.                                               SQ2194.2
044000     CLOSE SQ-VS6.                                                SQ2194.2
044100     GO TO READ-INIT-F1-01.                                       SQ2194.2
044200 WRITE-SHORT-REC.                                                 SQ2194.2
044300     MOVE "R1-M-G" TO XRECORD-NAME (1).                           SQ2194.2
044400     MOVE 000120 TO XRECORD-LENGTH (1).                           SQ2194.2
044500     ADD 1 TO COUNT-OF-RECS.                                      SQ2194.2
044600     MOVE COUNT-OF-RECS TO XRECORD-NUMBER (1).                    SQ2194.2
044700     MOVE "SHORT" TO LONG-OR-SHORT.                               SQ2194.2
044800     MOVE COUNT-OF-RECS TO SQ-VS6-RECNO.                          SQ2194.2
044900     MOVE FILE-RECORD-INFO-P1-120 (1) TO SQ-VS6R1-FIRST.          SQ2194.2
045000     WRITE SQ-VS6R1-M-G-120.                                      SQ2194.2
045100 WRITE-LONG-REC.                                                  SQ2194.2
045200     MOVE "R2-M-G" TO XRECORD-NAME (1).                           SQ2194.2
045300     MOVE 000151 TO XRECORD-LENGTH (1).                           SQ2194.2
045400     ADD 1 TO COUNT-OF-RECS.                                      SQ2194.2
045500     MOVE COUNT-OF-RECS TO XRECORD-NUMBER (1).                    SQ2194.2
045600     MOVE "LONG" TO LONG-OR-SHORT.                                SQ2194.2
045700     MOVE COUNT-OF-RECS TO SQ-VS6-RECNO.                          SQ2194.2
045800     MOVE FILE-RECORD-INFO-P1-120 (1) TO SQ-VS6R2-FIRST.          SQ2194.2
045900     WRITE SQ-VS6R2-M-G-151.                                      SQ2194.2
046000 READ-INIT-F1-01.                                                 SQ2194.2
046100     MOVE ZERO TO COUNT-OF-RECS.                                  SQ2194.2
046200     MOVE ZERO TO EOF-FLAG.                                       SQ2194.2
046300     MOVE ZERO TO RECORDS-IN-ERROR.                               SQ2194.2
046400     MOVE ZERO TO ERROR-FLAG.                                     SQ2194.2
046500     OPEN INPUT SQ-VS6.                                           SQ2194.2
046600 READ-TEST-F1-01.                                                 SQ2194.2
046700     PERFORM READ-SHORT-REC THRU READ-SHORT-REC-EXIT.             SQ2194.2
046800     IF EOF-FLAG EQUAL TO 1                                       SQ2194.2
046900         MOVE "EOF ON FIRST READ" TO RE-MARK                      SQ2194.2
047000         GO TO READ-EOF-F1-06.                                    SQ2194.2
047100     IF ERROR-FLAG EQUAL TO 1                                     SQ2194.2
047200         GO TO READ-FAIL-F1-01.                                   SQ2194.2
047300 READ-PASS-F1-01.                                                 SQ2194.2
047400     PERFORM PASS.                                                SQ2194.2
047500     GO TO READ-WRITE-F1-01.                                      SQ2194.2
047600 READ-FAIL-F1-01.                                                 SQ2194.2
047700     MOVE "ERROR ON FIRST READ;VII-13 GR (3)          " TO RE-MARKSQ2194.2
047800     PERFORM FAIL.                                                SQ2194.2
047900 READ-WRITE-F1-01.                                                SQ2194.2
048000     MOVE "READ SHORT RECORD" TO FEATURE.                         SQ2194.2
048100     MOVE "READ-TEST-F1-01" TO PAR-NAME.                          SQ2194.2
048200     PERFORM PRINT-DETAIL.                                        SQ2194.2
048300     GO TO READ-INIT-F1-02.                                       SQ2194.2
048400 READ-SHORT-REC.                                                  SQ2194.2
048500     IF EOF-FLAG EQUAL TO 1                                       SQ2194.2
048600          GO TO READ-SHORT-REC-EXIT.                              SQ2194.2
048700     READ SQ-VS6 AT END                                           SQ2194.2
048800          MOVE 1 TO EOF-FLAG                                      SQ2194.2
048900          GO TO READ-SHORT-REC-EXIT.                              SQ2194.2
049000     ADD 1 TO COUNT-OF-RECS.                                      SQ2194.2
049100     MOVE SQ-VS6R1-FIRST TO FILE-RECORD-INFO-P1-120 (1).          SQ2194.2
049200     IF XRECORD-NAME (1) NOT EQUAL TO "R1-M-G"                    SQ2194.2
049300          GO TO READ-SHORT-REC-ERROR.                             SQ2194.2
049400     IF XRECORD-LENGTH (1) NOT EQUAL TO 120                       SQ2194.2
049500          GO TO READ-SHORT-REC-ERROR.                             SQ2194.2
049600     IF COUNT-OF-RECS NOT EQUAL TO XRECORD-NUMBER (1)             SQ2194.2
049700          GO TO READ-SHORT-REC-ERROR.                             SQ2194.2
049800     IF XLABEL-TYPE (1) EQUAL TO "S"                              SQ2194.2
049900          GO TO READ-SHORT-REC-EXIT.                              SQ2194.2
050000 READ-SHORT-REC-ERROR.                                            SQ2194.2
050100     ADD 1 TO RECORDS-IN-ERROR.                                   SQ2194.2
050200     MOVE 1 TO ERROR-FLAG.                                        SQ2194.2
050300 READ-SHORT-REC-EXIT.                                             SQ2194.2
050400     EXIT.                                                        SQ2194.2
050500 READ-INIT-F1-02.                                                 SQ2194.2
050600     MOVE ZERO TO ERROR-FLAG.                                     SQ2194.2
050700 READ-TEST-F1-02.                                                 SQ2194.2
050800     PERFORM READ-LONG-REC THRU READ-LONG-REC-EXIT.               SQ2194.2
050900     IF EOF-FLAG EQUAL TO 1                                       SQ2194.2
051000          MOVE "EOF ON SECOND READ" TO RE-MARK                    SQ2194.2
051100          GO TO READ-EOF-F1-06.                                   SQ2194.2
051200     IF ERROR-FLAG EQUAL TO 1                                     SQ2194.2
051300          GO TO READ-FAIL-F1-02.                                  SQ2194.2
051400 READ-PASS-F1-02.                                                 SQ2194.2
051500     PERFORM PASS.                                                SQ2194.2
051600     GO TO READ-WRITE-F1-02.                                      SQ2194.2
051700 READ-FAIL-F1-02.                                                 SQ2194.2
051800     MOVE "ERROR ON SEC READ; VII-13 GR (3           " TO RE-MARK SQ2194.2
051900     PERFORM FAIL.                                                SQ2194.2
052000 READ-WRITE-F1-02.                                                SQ2194.2
052100     MOVE "READ LONG RECORD" TO FEATURE.                          SQ2194.2
052200     MOVE "READ-TEST-F1-02" TO PAR-NAME.                          SQ2194.2
052300     PERFORM PRINT-DETAIL.                                        SQ2194.2
052400     GO TO READ-INIT-F1-03.                                       SQ2194.2
052500 READ-LONG-REC.                                                   SQ2194.2
052600     IF EOF-FLAG EQUAL TO 1                                       SQ2194.2
052700         GO TO READ-LONG-REC-EXIT.                                SQ2194.2
052800     READ SQ-VS6 END                                              SQ2194.2
052900         MOVE 1 TO EOF-FLAG                                       SQ2194.2
053000         GO TO READ-LONG-REC-EXIT.                                SQ2194.2
053100     ADD 1 TO COUNT-OF-RECS.                                      SQ2194.2
053200     MOVE SQ-VS6R2-FIRST TO FILE-RECORD-INFO-P1-120 (1).          SQ2194.2
053300     IF XRECORD-NAME (1) NOT EQUAL TO "R2-M-G"                    SQ2194.2
053400         GO TO READ-LONG-REC-ERROR.                               SQ2194.2
053500     IF XRECORD-LENGTH (1) NOT EQUAL TO 151                       SQ2194.2
053600         GO TO READ-LONG-REC-ERROR.                               SQ2194.2
053700     MOVE COUNT-OF-RECS TO SAVE-COUNT-OF-RECS.                    SQ2194.2
053800     IF SAVE-COUNT-OF-RECS NOT EQUAL TO SQ-VS6-RECNO              SQ2194.2
053900         GO TO READ-LONG-REC-ERROR.                               SQ2194.2
054000     IF LONG-OR-SHORT EQUAL TO "LONG "                            SQ2194.2
054100         GO TO READ-LONG-REC-EXIT.                                SQ2194.2
054200 READ-LONG-REC-ERROR.                                             SQ2194.2
054300     ADD 1 TO RECORDS-IN-ERROR.                                   SQ2194.2
054400     MOVE 1 TO ERROR-FLAG.                                        SQ2194.2
054500 READ-LONG-REC-EXIT.                                              SQ2194.2
054600     EXIT.                                                        SQ2194.2
054700 READ-INIT-F1-03.                                                 SQ2194.2
054800     MOVE ZERO TO ERROR-FLAG.                                     SQ2194.2
054900 READ-TEST-F1-03.                                                 SQ2194.2
055000     PERFORM READ-SHORT-REC THRU READ-SHORT-REC-EXIT 10 TIMES.    SQ2194.2
055100     IF EOF-FLAG EQUAL TO 1                                       SQ2194.2
055200          MOVE "UNEXPECTED EOF" TO RE-MARK                        SQ2194.2
055300          GO TO READ-EOF-F1-06.                                   SQ2194.2
055400     IF ERROR-FLAG EQUAL TO 1                                     SQ2194.2
055500          GO TO READ-FAIL-F1-03.                                  SQ2194.2
055600 READ-PASS-F1-03.                                                 SQ2194.2
055700     PERFORM PASS.                                                SQ2194.2
055800     GO TO READ-WRITE-F1-03.                                      SQ2194.2
055900 READ-FAIL-F1-03.                                                 SQ2194.2
056000     MOVE "ERROR REA SHORT REC; VII-13 SR (3)         " TO RE-MARKSQ2194.2
056100     PERFORM FAIL.                                                SQ2194.2
056200 READ-WRITE-F1-03.                                                SQ2194.2
056300     MOVE "READ SHORT RECORDS" TO FEATURE.                        SQ2194.2
056400     MOVE "READ-TEST-F1-03" TO PAR-NAME.                          SQ2194.2
056500     PERFORM PRINT-DETAIL.                                        SQ2194.2
056600 READ-INIT-F1-04.                                                 SQ2194.2
056700     MOVE ZERO TO ERROR-FLAG.                                     SQ2194.2
056800 READ-TEST-F1-04.                                                 SQ2194.2
056900     PERFORM READ-LONG-REC THRU READ-LONG-REC-EXIT 100 TIMES.     SQ2194.2
057000     IF EOF-FLAG EQUAL TO 1                                       SQ2194.2
057100         MOVE "UNEXPECTED EOF" TO RE-MARK                         SQ2194.2
057200         GO TO READ-EOF-F1-06.                                    SQ2194.2
057300     IF ERROR-FLAG EQUAL TO 1                                     SQ2194.2
057400         GO TO READ-FAIL-F1-04.                                   SQ2194.2
057500 READ-PASS-F1-04.                                                 SQ2194.2
057600     PERFORM PASS.                                                SQ2194.2
057700     GO TO READ-WRITE-F1-04.                                      SQ2194.2
057800 READ-FAIL-F1-04.                                                 SQ2194.2
057900     PERFORM FAIL.                                                SQ2194.2
058000     MOVE "ERROR READING LONG RECORD" TO RE-MARK.                 SQ2194.2
058100 READ-WRITE-F1-04.                                                SQ2194.2
058200     MOVE "READ LONG RECORDS" TO FEATURE.                         SQ2194.2
058300     MOVE "READ-TEST-F1-04" TO PAR-NAME.                          SQ2194.2
058400     PERFORM PRINT-DETAIL.                                        SQ2194.2
058500 READ-INIT-F1-05.                                                 SQ2194.2
058600     MOVE ZERO TO ERROR-FLAG.                                     SQ2194.2
058700 READ-TEST-F1-05.                                                 SQ2194.2
058800     PERFORM READ-SHORT-REC THRU READ-SHORT-REC-EXIT 338 TIMES.   SQ2194.2
058900     IF EOF-FLAG EQUAL TO 1                                       SQ2194.2
059000         MOVE "UNEXPECTED EOF" TO RE-MARK                         SQ2194.2
059100         GO TO READ-EOF-F1-06.                                    SQ2194.2
059200     IF ERROR-FLAG EQUAL TO 1                                     SQ2194.2
059300         GO TO READ-FAIL-F1-05.                                   SQ2194.2
059400 READ-PASS-F1-05.                                                 SQ2194.2
059500     PERFORM PASS.                                                SQ2194.2
059600     GO TO READ-WRITE-F1-05.                                      SQ2194.2
059700 READ-FAIL-F1-05.                                                 SQ2194.2
059800     MOVE "ERROR READING SHORT;VII-13 GR (3)          " TO RE-MARKSQ2194.2
059900     PERFORM FAIL.                                                SQ2194.2
060000 READ-WRITE-F1-05.                                                SQ2194.2
060100     MOVE "READ SHORT RECORDS" TO FEATURE.                        SQ2194.2
060200     MOVE "READ-TEST-F1-05" TO PAR-NAME.                          SQ2194.2
060300     PERFORM PRINT-DETAIL.                                        SQ2194.2
060400 READ-INIT-F1-06.                                                 SQ2194.2
060500     READ SQ-VS6 RECORD END                                       SQ2194.2
060600         GO TO READ-TEST-F1-06.                                   SQ2194.2
060700     MOVE "MORE THAN 450 RECORDS" TO RE-MARK.                     SQ2194.2
060800     GO TO READ-FAIL-F1-06.                                       SQ2194.2
060900 READ-EOF-F1-06.                                                  SQ2194.2
061000     MOVE "RECORDS READ =" TO COMPUTED-A.                         SQ2194.2
061100     MOVE COUNT-OF-RECS TO CORRECT-18V0.                          SQ2194.2
061200     GO TO READ-FAIL-F1-06.                                       SQ2194.2
061300 READ-TEST-F1-06.                                                 SQ2194.2
061400     IF RECORDS-IN-ERROR NOT EQUAL TO ZERO                        SQ2194.2
061500         MOVE "RECORDS IN ERROR =" TO COMPUTED-A                  SQ2194.2
061600         MOVE RECORDS-IN-ERROR TO CORRECT-18V0                    SQ2194.2
061700         GO TO READ-FAIL-F1-06.                                   SQ2194.2
061800 READ-PASS-F1-06.                                                 SQ2194.2
061900     PERFORM PASS.                                                SQ2194.2
062000     GO TO READ-WRITE-F1-06.                                      SQ2194.2
062100 READ-FAIL-F1-06.                                                 SQ2194.2
062200     MOVE "VII-13 GR (3)          " TO RE-MARK.                   SQ2194.2
062300     PERFORM FAIL.                                                SQ2194.2
062400 READ-WRITE-F1-06.                                                SQ2194.2
062500     MOVE "READ-TEST-F1-06" TO PAR-NAME.                          SQ2194.2
062600     MOVE "VERIFY FILE SQ-VS6" TO FEATURE.                        SQ2194.2
062700     PERFORM PRINT-DETAIL.                                        SQ2194.2
062800 READ-CLOSE-F1-06.                                                SQ2194.2
062900     CLOSE SQ-VS6.                                                SQ2194.2
063000 SECT-SQ219A-0002 SECTION.                                        SQ2194.2
063100*        THIS SECTION CHECKS IF THE ENTIRE RECORD AREA IS WRITTEN SQ2194.2
063200*    ON THE OUTPUT DEVICE WHEN A SHORT RECORD IS WRITTEN.  THE    SQ2194.2
063300*    RECORD NUMBER IN CHARACTERS 126 THROUGH 130 IS UNIQUE        SQ2194.2
063400*    FOR EACH RECORD.                                             SQ2194.2
063500 INFO-INIT-001.                                                   SQ2194.2
063600     OPEN INPUT SQ-VS6.                                           SQ2194.2
063700     MOVE ZERO TO COUNT-OF-RECS.                                  SQ2194.2
063800 INFO-TEST-001.                                                   SQ2194.2
063900     READ SQ-VS6 AT END                                           SQ2194.2
064000          GO TO INFO-END.                                         SQ2194.2
064100     ADD 1 TO COUNT-OF-RECS.                                      SQ2194.2
064200     IF SQ-VS6-RECNO NOT EQUAL TO "00001"                         SQ2194.2
064300          GO TO NO-INFO-001.                                      SQ2194.2
064400     MOVE "MAXIMUM RECORD SIZE WRITTEN" TO RE-MARK.               SQ2194.2
064500     MOVE "RECORD READ =" TO COMPUTED-A.                          SQ2194.2
064600     MOVE 0001 TO CORRECT-18V0.                                   SQ2194.2
064700     GO TO INFO-WRITE-001.                                        SQ2194.2
064800 NO-INFO-001.                                                     SQ2194.2
064900     MOVE "NO DEFINITE CONCLUSION POSSIBLE" TO RE-MARK.           SQ2194.2
065000 INFO-WRITE-001.                                                  SQ2194.2
065100     MOVE "READ SHORT RECORD" TO FEATURE.                         SQ2194.2
065200     MOVE "INFO-TEST-001" TO PAR-NAME.                            SQ2194.2
065300     PERFORM PRINT-DETAIL.                                        SQ2194.2
065400 INFO-INIT-002.                                                   SQ2194.2
065500     READ SQ-VS6 RECORD AT END                                    SQ2194.2
065600          GO TO INFO-END.                                         SQ2194.2
065700     READ SQ-VS6 END                                              SQ2194.2
065800          GO TO INFO-END.                                         SQ2194.2
065900 INFO-TEST-002.                                                   SQ2194.2
066000     READ SQ-VS6 AT END                                           SQ2194.2
066100          GO TO INFO-END.                                         SQ2194.2
066200     IF SQ-VS6-RECNO NOT EQUAL TO "00004"                         SQ2194.2
066300          GO TO NO-INFO-002.                                      SQ2194.2
066400     MOVE "MAXIMUM RECORD SIZE WRITTEN" TO RE-MARK.               SQ2194.2
066500     MOVE "RECORD READ =" TO COMPUTED-A.                          SQ2194.2
066600     MOVE 0004 TO CORRECT-18V0.                                   SQ2194.2
066700     GO TO INFO-WRITE-002.                                        SQ2194.2
066800 NO-INFO-002.                                                     SQ2194.2
066900     MOVE "NO DEFINITE CONCLUSION POSSIBLE" TO RE-MARK.           SQ2194.2
067000 INFO-WRITE-002.                                                  SQ2194.2
067100     MOVE "READ SHORT RECORD" TO FEATURE.                         SQ2194.2
067200     MOVE "INFO-TEST-002" TO PAR-NAME.                            SQ2194.2
067300     PERFORM PRINT-DETAIL.                                        SQ2194.2
067400 INFO-INIT-003.                                                   SQ2194.2
067500     ADD 3 TO COUNT-OF-RECS.                                      SQ2194.2
067600 INFO-INIT-003-1.                                                 SQ2194.2
067700     READ SQ-VS6 RECORD                                           SQ2194.2
067800          END GO TO INFO-END.                                     SQ2194.2
067900     ADD 1 TO COUNT-OF-RECS.                                      SQ2194.2
068000     IF COUNT-OF-RECS EQUAL TO 450                                SQ2194.2
068100         GO TO INFO-TEST-003.                                     SQ2194.2
068200     GO TO INFO-INIT-003-1.                                       SQ2194.2
068300 INFO-TEST-003.                                                   SQ2194.2
068400     IF SQ-VS6-RECNO NOT EQUAL TO "00450"                         SQ2194.2
068500          GO TO NO-INFO-003.                                      SQ2194.2
068600     MOVE "MAXIMUM RECORD SIZE WRITTEN" TO RE-MARK.               SQ2194.2
068700     MOVE "RECORD READ =" TO COMPUTED-A.                          SQ2194.2
068800     MOVE 0450 TO CORRECT-18V0.                                   SQ2194.2
068900     GO TO INFO-WRITE-003.                                        SQ2194.2
069000 NO-INFO-003.                                                     SQ2194.2
069100     MOVE "NO DEFINITE CONCLUSION POSSIBLE" TO RE-MARK.           SQ2194.2
069200 INFO-WRITE-003.                                                  SQ2194.2
069300     MOVE "READ SHORT RECORD" TO FEATURE.                         SQ2194.2
069400     MOVE "INFO-TEST-003" TO PAR-NAME.                            SQ2194.2
069500     PERFORM PRINT-DETAIL.                                        SQ2194.2
069600 INFO-END.                                                        SQ2194.2
069700     CLOSE SQ-VS6.                                                SQ2194.2
069800 TERMINATE-ROUTINE.                                               SQ2194.2
069900     EXIT.                                                        SQ2194.2
070000 CCVS-EXIT SECTION.                                               SQ2194.2
070100 CCVS-999999.                                                     SQ2194.2
070200     GO TO CLOSE-FILES.                                           SQ2194.2
      *END-OF,SQ219A                                                                  
