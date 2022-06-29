      *HEADER,COBOL,SQ107A                                                            
000100 IDENTIFICATION DIVISION.                                         SQ1074.2
000200 PROGRAM-ID.                                                      SQ1074.2
000300     SQ107A.                                                      SQ1074.2
000400****************************************************************  SQ1074.2
000500*                                                              *  SQ1074.2
000600*    VALIDATION FOR:-                                          *  SQ1074.2
000700*    " HIGH       ".                                              SQ1074.2
000800*                                                              *  SQ1074.2
000900*    CREATION DATE     /     VALIDATION DATE                   *  SQ1074.2
001000*    "4.2 ".                                                      SQ1074.2
001100*                                                              *  SQ1074.2
001200****************************************************************  SQ1074.2
001300                                                                  SQ1074.2
001400*        THIS ROUTINE BUILDS A SEQUENTIAL MASS STORAGE FILE       SQ1074.2
001500*    WHICH CONTAINS BOTH 120 CHARACTER AND 151 CHARACTER          SQ1074.2
001600*    RECORDS.  THE MASS STORAGE FILE CONSISTS OF 1 SHORT,         SQ1074.2
001700*    1 LONG, 10 SHORT, 100 LONG, AND 338 SHORT RECORDS FOR        SQ1074.2
001800*    A TOTAL OF 450 RECORDS IN THE FILE.  THE MASS STORAGE        SQ1074.2
001900*    FILE IS READ AND FIELDS IN THE RECORDS ARE CHECKED           SQ1074.2
002000*    AGAINST THE EXPECTED VALUES.                                 SQ1074.2
002100*                                                                 SQ1074.2
002200*        AN INFORMATION SECTION AT THE END OF THE ROUTINE         SQ1074.2
002300*    CHECKS THE FIELD WHICH CONTAINS THE XRECORD-NUMBER.          SQ1074.2
002400*    THIS FIELD IS PART OF A LONG RECORD ONLY.  IF THE            SQ1074.2
002500*    XRECORD-NUMBER IS THERE FOR A SHORT RECORD, IT MEANS         SQ1074.2
002600*    THE MAXIMUM SIZE RECORD IS ALWAYS WRITTEN.                   SQ1074.2
002700*                                                                 SQ1074.2
002800*    NEW FEATURE: THE LOGICAL RECORD EXTEND ACROSS THE PHYSICAL   SQ1074.2
002900*                 RECORD.  (VII-23; 3.3.3 (2) A)                  SQ1074.2
003000*                                                                 SQ1074.2
003100*                                                                 SQ1074.2
003200*    USED X-CARDS:                                                SQ1074.2
003300*         XXXXX014                                                SQ1074.2
003400*         XXXXX055                                                SQ1074.2
003500*     P   XXXXX062                                                SQ1074.2
003600*         XXXXX082                                                SQ1074.2
003700*         XXXXX083                                                SQ1074.2
003800*     C   XXXXX084                                                SQ1074.2
003900*                                                                 SQ1074.2
004000*                                                                 SQ1074.2
004100 ENVIRONMENT DIVISION.                                            SQ1074.2
004200 CONFIGURATION SECTION.                                           SQ1074.2
004300 SOURCE-COMPUTER.                                                 SQ1074.2
004400     XXXXX082.                                                    SQ1074.2
004500 OBJECT-COMPUTER.                                                 SQ1074.2
004600     XXXXX083.                                                    SQ1074.2
004700 INPUT-OUTPUT SECTION.                                            SQ1074.2
004800 FILE-CONTROL.                                                    SQ1074.2
004900*    SELECT RAW-DATA   ASSIGN TO                                  SQ1074.2
005000*    XXXXX062                                                     SQ1074.2
005100*           ORGANIZATION IS INDEXED                               SQ1074.2
005200*           ACCESS MODE IS RANDOM                                 SQ1074.2
005300*           RECORD KEY IS RAW-DATA-KEY.                           SQ1074.2
005400     SELECT PRINT-FILE ASSIGN TO                                  SQ1074.2
005500     XXXXX055.                                                    SQ1074.2
005600     SELECT SQ-VS7 ASSIGN TO                                      SQ1074.2
005700     XXXXX014                                                     SQ1074.2
005800     ORGANIZATION SEQUENTIAL                                      SQ1074.2
005900     ACCESS SEQUENTIAL.                                           SQ1074.2
006000 DATA DIVISION.                                                   SQ1074.2
006100 FILE SECTION.                                                    SQ1074.2
006200*                                                                 SQ1074.2
006300*FD  RAW-DATA.                                                    SQ1074.2
006400*                                                                 SQ1074.2
006500*01  RAW-DATA-SATZ.                                               SQ1074.2
006600*    05  RAW-DATA-KEY        PIC X(6).                            SQ1074.2
006700*    05  C-DATE              PIC 9(6).                            SQ1074.2
006800*    05  C-TIME              PIC 9(8).                            SQ1074.2
006900*    05  C-NO-OF-TESTS       PIC 99.                              SQ1074.2
007000*    05  C-OK                PIC 999.                             SQ1074.2
007100*    05  C-ALL               PIC 999.                             SQ1074.2
007200*    05  C-FAIL              PIC 999.                             SQ1074.2
007300*    05  C-DELETED           PIC 999.                             SQ1074.2
007400*    05  C-INSPECT           PIC 999.                             SQ1074.2
007500*    05  C-NOTE              PIC X(13).                           SQ1074.2
007600*    05  C-INDENT            PIC X.                               SQ1074.2
007700*    05  C-ABORT             PIC X(8).                            SQ1074.2
007800 FD  PRINT-FILE                                                   SQ1074.2
007900C    LABEL RECORDS                                                SQ1074.2
008000C    XXXXX084                                                     SQ1074.2
008100C    DATA RECORD IS PRINT-REC DUMMY-RECORD                        SQ1074.2
008200               .                                                  SQ1074.2
008300 01  PRINT-REC PICTURE X(120).                                    SQ1074.2
008400 01  DUMMY-RECORD PICTURE X(120).                                 SQ1074.2
008500 FD  SQ-VS7                                                       SQ1074.2
008600C    LABEL RECORDS ARE STANDARD                                   SQ1074.2
008700     BLOCK CONTAINS 100 CHARACTERS.                               SQ1074.2
008800 01  SQ-VS7R1-M-G-120.                                            SQ1074.2
008900     02  SQ-VS7R1-FIRST PICTURE X(120).                           SQ1074.2
009000 01  SQ-VS7R2-M-G-151.                                            SQ1074.2
009100     02  SQ-VS7R2-FIRST PICTURE X(120).                           SQ1074.2
009200     02  LONG-OR-SHORT  PICTURE X(5).                             SQ1074.2
009300     02  SQ-VS7-RECNO  PICTURE X(5).                              SQ1074.2
009400 02  SQ-VS7-FILLER  PICTURE X(21).                                SQ1074.2
009500 WORKING-STORAGE SECTION.                                         SQ1074.2
009600 01  SAVE-COUNT-OF-RECS  PICTURE X(5) VALUE SPACE.                SQ1074.2
009700 01  COUNT-OF-RECS  PICTURE S9(5) COMPUTATIONAL.                  SQ1074.2
009800 01  RECORDS-IN-ERROR  PICTURE S9(5) COMPUTATIONAL.               SQ1074.2
009900 01  ERROR-FLAG PICTURE 9.                                        SQ1074.2
010000 01  EOF-FLAG  PICTURE 9.                                         SQ1074.2
010100 01  DUMP-AREA.                                                   SQ1074.2
010200     02  TYPE-OF-REC PICTURE X(5).                                SQ1074.2
010300     02  RECNO  PICTURE 9(5).                                     SQ1074.2
010400     02  REC-FILLER PICTURE X(21).                                SQ1074.2
010500 01  FILE-RECORD-INFORMATION-REC.                                 SQ1074.2
010600     03 FILE-RECORD-INFO-SKELETON.                                SQ1074.2
010700        05 FILLER                 PICTURE X(48)       VALUE       SQ1074.2
010800             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  SQ1074.2
010900        05 FILLER                 PICTURE X(46)       VALUE       SQ1074.2
011000             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    SQ1074.2
011100        05 FILLER                 PICTURE X(26)       VALUE       SQ1074.2
011200             ",LFIL=000000,ORG=  ,LBLR= ".                        SQ1074.2
011300        05 FILLER                 PICTURE X(37)       VALUE       SQ1074.2
011400             ",RECKEY=                             ".             SQ1074.2
011500        05 FILLER                 PICTURE X(38)       VALUE       SQ1074.2
011600             ",ALTKEY1=                             ".            SQ1074.2
011700        05 FILLER                 PICTURE X(38)       VALUE       SQ1074.2
011800             ",ALTKEY2=                             ".            SQ1074.2
011900        05 FILLER                 PICTURE X(7)        VALUE SPACE.SQ1074.2
012000     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              SQ1074.2
012100        05 FILE-RECORD-INFO-P1-120.                               SQ1074.2
012200           07 FILLER              PIC X(5).                       SQ1074.2
012300           07 XFILE-NAME           PIC X(6).                      SQ1074.2
012400           07 FILLER              PIC X(8).                       SQ1074.2
012500           07 XRECORD-NAME         PIC X(6).                      SQ1074.2
012600           07 FILLER              PIC X(1).                       SQ1074.2
012700           07 REELUNIT-NUMBER     PIC 9(1).                       SQ1074.2
012800           07 FILLER              PIC X(7).                       SQ1074.2
012900           07 XRECORD-NUMBER       PIC 9(6).                      SQ1074.2
013000           07 FILLER              PIC X(6).                       SQ1074.2
013100           07 UPDATE-NUMBER       PIC 9(2).                       SQ1074.2
013200           07 FILLER              PIC X(5).                       SQ1074.2
013300           07 ODO-NUMBER          PIC 9(4).                       SQ1074.2
013400           07 FILLER              PIC X(5).                       SQ1074.2
013500           07 XPROGRAM-NAME        PIC X(5).                      SQ1074.2
013600           07 FILLER              PIC X(7).                       SQ1074.2
013700           07 XRECORD-LENGTH       PIC 9(6).                      SQ1074.2
013800           07 FILLER              PIC X(7).                       SQ1074.2
013900           07 CHARS-OR-RECORDS    PIC X(2).                       SQ1074.2
014000           07 FILLER              PIC X(1).                       SQ1074.2
014100           07 XBLOCK-SIZE          PIC 9(4).                      SQ1074.2
014200           07 FILLER              PIC X(6).                       SQ1074.2
014300           07 RECORDS-IN-FILE     PIC 9(6).                       SQ1074.2
014400           07 FILLER              PIC X(5).                       SQ1074.2
014500           07 XFILE-ORGANIZATION   PIC X(2).                      SQ1074.2
014600           07 FILLER              PIC X(6).                       SQ1074.2
014700           07 XLABEL-TYPE          PIC X(1).                      SQ1074.2
014800        05 FILE-RECORD-INFO-P121-240.                             SQ1074.2
014900           07 FILLER              PIC X(8).                       SQ1074.2
015000           07 XRECORD-KEY          PIC X(29).                     SQ1074.2
015100           07 FILLER              PIC X(9).                       SQ1074.2
015200           07 ALTERNATE-KEY1      PIC X(29).                      SQ1074.2
015300           07 FILLER              PIC X(9).                       SQ1074.2
015400           07 ALTERNATE-KEY2      PIC X(29).                      SQ1074.2
015500           07 FILLER              PIC X(7).                       SQ1074.2
015600 01  TEST-RESULTS.                                                SQ1074.2
015700     02 FILLER                    PICTURE X VALUE SPACE.          SQ1074.2
015800     02 FEATURE                   PICTURE X(20) VALUE SPACE.      SQ1074.2
015900     02 FILLER                    PICTURE X VALUE SPACE.          SQ1074.2
016000     02 P-OR-F                    PICTURE X(5) VALUE SPACE.       SQ1074.2
016100     02 FILLER                    PICTURE X  VALUE SPACE.         SQ1074.2
016200     02  PAR-NAME.                                                SQ1074.2
016300       03 FILLER PICTURE X(12) VALUE SPACE.                       SQ1074.2
016400       03  PARDOT-X PICTURE X  VALUE SPACE.                       SQ1074.2
016500       03 DOTVALUE PICTURE 99  VALUE ZERO.                        SQ1074.2
016600       03 FILLER PIC X(5) VALUE SPACE.                            SQ1074.2
016700     02 FILLER PIC X(10) VALUE SPACE.                             SQ1074.2
016800     02 RE-MARK PIC X(61).                                        SQ1074.2
016900 01  TEST-COMPUTED.                                               SQ1074.2
017000     02 FILLER PIC X(30) VALUE SPACE.                             SQ1074.2
017100     02 FILLER PIC X(17) VALUE "       COMPUTED=".                SQ1074.2
017200     02 COMPUTED-X.                                               SQ1074.2
017300     03 COMPUTED-A                PICTURE X(20) VALUE SPACE.      SQ1074.2
017400     03 COMPUTED-N REDEFINES COMPUTED-A PICTURE -9(9).9(9).       SQ1074.2
017500     03 COMPUTED-0V18 REDEFINES COMPUTED-A  PICTURE -.9(18).      SQ1074.2
017600     03 COMPUTED-4V14 REDEFINES COMPUTED-A  PICTURE -9(4).9(14).  SQ1074.2
017700     03 COMPUTED-14V4 REDEFINES COMPUTED-A  PICTURE -9(14).9(4).  SQ1074.2
017800     03       CM-18V0 REDEFINES COMPUTED-A.                       SQ1074.2
017900         04 COMPUTED-18V0                   PICTURE -9(18).       SQ1074.2
018000         04 FILLER                          PICTURE X.            SQ1074.2
018100     03 FILLER PIC X(50) VALUE SPACE.                             SQ1074.2
018200 01  TEST-CORRECT.                                                SQ1074.2
018300     02 FILLER PIC X(30) VALUE SPACE.                             SQ1074.2
018400     02 FILLER PIC X(17) VALUE "       CORRECT =".                SQ1074.2
018500     02 CORRECT-X.                                                SQ1074.2
018600     03 CORRECT-A                 PICTURE X(20) VALUE SPACE.      SQ1074.2
018700     03 CORRECT-N REDEFINES CORRECT-A PICTURE -9(9).9(9).         SQ1074.2
018800     03 CORRECT-0V18 REDEFINES CORRECT-A    PICTURE -.9(18).      SQ1074.2
018900     03 CORRECT-4V14 REDEFINES CORRECT-A    PICTURE -9(4).9(14).  SQ1074.2
019000     03 CORRECT-14V4 REDEFINES CORRECT-A    PICTURE -9(14).9(4).  SQ1074.2
019100     03      CR-18V0 REDEFINES CORRECT-A.                         SQ1074.2
019200         04 CORRECT-18V0                    PICTURE -9(18).       SQ1074.2
019300         04 FILLER                          PICTURE X.            SQ1074.2
019400     03 FILLER PIC X(50) VALUE SPACE.                             SQ1074.2
019500 01  CCVS-C-1.                                                    SQ1074.2
019600     02 FILLER PICTURE IS X(99) VALUE IS " FEATURE              PASQ1074.2
019700-    "SS  PARAGRAPH-NAME                                          SQ1074.2
019800-    "        REMARKS".                                           SQ1074.2
019900     02 FILLER PICTURE IS X(20) VALUE IS SPACE.                   SQ1074.2
020000 01  CCVS-C-2.                                                    SQ1074.2
020100     02 FILLER PICTURE IS X VALUE IS SPACE.                       SQ1074.2
020200     02 FILLER PICTURE IS X(6) VALUE IS "TESTED".                 SQ1074.2
020300     02 FILLER PICTURE IS X(15) VALUE IS SPACE.                   SQ1074.2
020400     02 FILLER PICTURE IS X(4) VALUE IS "FAIL".                   SQ1074.2
020500     02 FILLER PICTURE IS X(94) VALUE IS SPACE.                   SQ1074.2
020600 01  REC-SKL-SUB PICTURE 9(2) VALUE ZERO.                         SQ1074.2
020700 01  REC-CT PICTURE 99 VALUE ZERO.                                SQ1074.2
020800 01  DELETE-CNT                   PICTURE 999  VALUE ZERO.        SQ1074.2
020900 01  ERROR-COUNTER PICTURE IS 999 VALUE IS ZERO.                  SQ1074.2
021000 01  INSPECT-COUNTER PIC 999 VALUE ZERO.                          SQ1074.2
021100 01  PASS-COUNTER PIC 999 VALUE ZERO.                             SQ1074.2
021200 01  TOTAL-ERROR PIC 999 VALUE ZERO.                              SQ1074.2
021300 01  ERROR-HOLD PIC 999 VALUE ZERO.                               SQ1074.2
021400 01  DUMMY-HOLD PIC X(120) VALUE SPACE.                           SQ1074.2
021500 01  RECORD-COUNT PIC 9(5) VALUE ZERO.                            SQ1074.2
021600 01  CCVS-H-1.                                                    SQ1074.2
021700     02  FILLER   PICTURE X(27)  VALUE SPACE.                     SQ1074.2
021800     02 FILLER PICTURE X(67) VALUE                                SQ1074.2
021900     " FEDERAL SOFTWARE TESTING CENTER COBOL COMPILER VALIDATION  SQ1074.2
022000-    " SYSTEM".                                                   SQ1074.2
022100     02  FILLER     PICTURE X(26)  VALUE SPACE.                   SQ1074.2
022200 01  CCVS-H-2.                                                    SQ1074.2
022300     02 FILLER PICTURE X(52) VALUE IS                             SQ1074.2
022400     "CCVS85 FSTC COPY, NOT FOR DISTRIBUTION.".                   SQ1074.2
022500     02 FILLER PICTURE IS X(19) VALUE IS "TEST RESULTS SET-  ".   SQ1074.2
022600     02 TEST-ID PICTURE IS X(9).                                  SQ1074.2
022700     02 FILLER PICTURE IS X(40) VALUE IS SPACE.                   SQ1074.2
022800 01  CCVS-H-3.                                                    SQ1074.2
022900     02  FILLER PICTURE X(34) VALUE                               SQ1074.2
023000     " FOR OFFICIAL USE ONLY    ".                                SQ1074.2
023100     02  FILLER PICTURE X(58) VALUE                               SQ1074.2
023200     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ1074.2
023300     02  FILLER PICTURE X(28) VALUE                               SQ1074.2
023400     "  COPYRIGHT   1985 ".                                       SQ1074.2
023500 01  CCVS-E-1.                                                    SQ1074.2
023600     02 FILLER PICTURE IS X(52) VALUE IS SPACE.                   SQ1074.2
023700     02 FILLER PICTURE IS X(14) VALUE IS "END OF TEST-  ".        SQ1074.2
023800     02 ID-AGAIN PICTURE IS X(9).                                 SQ1074.2
023900     02 FILLER PICTURE X(45) VALUE IS                             SQ1074.2
024000     " NTIS DISTRIBUTION COBOL 85".                               SQ1074.2
024100 01  CCVS-E-2.                                                    SQ1074.2
024200     02  FILLER                   PICTURE X(31)  VALUE            SQ1074.2
024300     SPACE.                                                       SQ1074.2
024400     02  FILLER                   PICTURE X(21)  VALUE SPACE.     SQ1074.2
024500     02 CCVS-E-2-2.                                               SQ1074.2
024600         03 ERROR-TOTAL PICTURE IS XXX VALUE IS SPACE.            SQ1074.2
024700         03 FILLER PICTURE IS X VALUE IS SPACE.                   SQ1074.2
024800         03 ENDER-DESC PIC X(46) VALUE "ERRORS ENCOUNTERED".      SQ1074.2
024900 01  CCVS-E-3.                                                    SQ1074.2
025000     02  FILLER PICTURE X(22) VALUE                               SQ1074.2
025100     " FOR OFFICIAL USE ONLY".                                    SQ1074.2
025200     02  FILLER PICTURE X(12) VALUE SPACE.                        SQ1074.2
025300     02  FILLER PICTURE X(58) VALUE                               SQ1074.2
025400     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1074.2
025500     02  FILLER PICTURE X(13) VALUE SPACE.                        SQ1074.2
025600     02 FILLER PIC X(15) VALUE " COPYRIGHT 1985".                 SQ1074.2
025700 01  CCVS-E-4.                                                    SQ1074.2
025800     02 CCVS-E-4-1 PIC XXX VALUE SPACE.                           SQ1074.2
025900     02 FILLER PIC XXXX VALUE " OF ".                             SQ1074.2
026000     02 CCVS-E-4-2 PIC XXX VALUE SPACE.                           SQ1074.2
026100     02 FILLER PIC X(40) VALUE                                    SQ1074.2
026200      "  TESTS WERE EXECUTED SUCCESSFULLY".                       SQ1074.2
026300 01  XXINFO.                                                      SQ1074.2
026400     02 FILLER PIC X(30) VALUE "        *** INFORMATION  ***".    SQ1074.2
026500     02 INFO-TEXT.                                                SQ1074.2
026600     04 FILLER PIC X(20) VALUE SPACE.                             SQ1074.2
026700     04 XXCOMPUTED PIC X(20).                                     SQ1074.2
026800     04 FILLER PIC X(5) VALUE SPACE.                              SQ1074.2
026900     04 XXCORRECT PIC X(20).                                      SQ1074.2
027000 01  HYPHEN-LINE.                                                 SQ1074.2
027100     02 FILLER PICTURE IS X VALUE IS SPACE.                       SQ1074.2
027200     02 FILLER PICTURE IS X(65) VALUE IS "************************SQ1074.2
027300-    "*****************************************".                 SQ1074.2
027400     02 FILLER PICTURE IS X(54) VALUE IS "************************SQ1074.2
027500-    "******************************".                            SQ1074.2
027600 01  CCVS-PGM-ID PIC X(6) VALUE                                   SQ1074.2
027700     "SQ107A".                                                    SQ1074.2
027800 PROCEDURE DIVISION.                                              SQ1074.2
027900 CCVS1 SECTION.                                                   SQ1074.2
028000 OPEN-FILES.                                                      SQ1074.2
028100*    OPEN I-O RAW-DATA.                                           SQ1074.2
028200*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            SQ1074.2
028300*    READ RAW-DATA INVALID KEY GO TO END-E-1.                     SQ1074.2
028400*    MOVE "ABORTED " TO C-ABORT.                                  SQ1074.2
028500*    ADD 1 TO C-NO-OF-TESTS.                                      SQ1074.2
028600*    ACCEPT C-DATE  FROM DATE.                                    SQ1074.2
028700*    ACCEPT C-TIME  FROM TIME.                                    SQ1074.2
028800*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-1.             SQ1074.2
028900*END-E-1.                                                         SQ1074.2
029000*    CLOSE RAW-DATA.                                              SQ1074.2
029100     OPEN     OUTPUT PRINT-FILE.                                  SQ1074.2
029200     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   SQ1074.2
029300     MOVE    SPACE TO TEST-RESULTS.                               SQ1074.2
029400     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             SQ1074.2
029500     MOVE ZERO TO REC-SKL-SUB.                                    SQ1074.2
029600     PERFORM CCVS-INIT-FILE 9 TIMES.                              SQ1074.2
029700 CCVS-INIT-FILE.                                                  SQ1074.2
029800     ADD 1 TO REC-SKL-SUB.                                        SQ1074.2
029900     MOVE FILE-RECORD-INFO-SKELETON TO                            SQ1074.2
030000                  FILE-RECORD-INFO (REC-SKL-SUB).                 SQ1074.2
030100 CCVS-INIT-EXIT.                                                  SQ1074.2
030200     GO TO CCVS1-EXIT.                                            SQ1074.2
030300 CLOSE-FILES.                                                     SQ1074.2
030400     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   SQ1074.2
030500*    OPEN I-O RAW-DATA.                                           SQ1074.2
030600*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            SQ1074.2
030700*    READ RAW-DATA INVALID KEY GO TO END-E-2.                     SQ1074.2
030800*    MOVE "OK.     " TO C-ABORT.                                  SQ1074.2
030900*    MOVE PASS-COUNTER TO C-OK.                                   SQ1074.2
031000*    MOVE ERROR-HOLD   TO C-ALL.                                  SQ1074.2
031100*    MOVE ERROR-COUNTER TO C-FAIL.                                SQ1074.2
031200*    MOVE DELETE-CNT TO C-DELETED.                                SQ1074.2
031300*    MOVE INSPECT-COUNTER TO C-INSPECT.                           SQ1074.2
031400*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-2.             SQ1074.2
031500*END-E-2.                                                         SQ1074.2
031600*    CLOSE RAW-DATA.                                              SQ1074.2
031700 TERMINATE-CCVS.                                                  SQ1074.2
031800*    EXIT PROGRAM.                                                SQ1074.2
031900*TERMINATE-CALL.                                                  SQ1074.2
032000     STOP     RUN.                                                SQ1074.2
032100 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         SQ1074.2
032200 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           SQ1074.2
032300 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          SQ1074.2
032400 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-CNT.          SQ1074.2
032500     MOVE "****TEST DELETED****" TO RE-MARK.                      SQ1074.2
032600 PRINT-DETAIL.                                                    SQ1074.2
032700     IF REC-CT NOT EQUAL TO ZERO                                  SQ1074.2
032800             MOVE "." TO PARDOT-X                                 SQ1074.2
032900             MOVE REC-CT TO DOTVALUE.                             SQ1074.2
033000     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      SQ1074.2
033100     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               SQ1074.2
033200        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 SQ1074.2
033300          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 SQ1074.2
033400     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              SQ1074.2
033500     MOVE SPACE TO CORRECT-X.                                     SQ1074.2
033600     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         SQ1074.2
033700     MOVE     SPACE TO RE-MARK.                                   SQ1074.2
033800 HEAD-ROUTINE.                                                    SQ1074.2
033900     MOVE CCVS-H-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ1074.2
034000     MOVE CCVS-H-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.   SQ1074.2
034100     MOVE CCVS-H-3 TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.   SQ1074.2
034200 COLUMN-NAMES-ROUTINE.                                            SQ1074.2
034300     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ1074.2
034400     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ1074.2
034500     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ1074.2
034600 END-ROUTINE.                                                     SQ1074.2
034700     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.SQ1074.2
034800 END-RTN-EXIT.                                                    SQ1074.2
034900     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ1074.2
035000 END-ROUTINE-1.                                                   SQ1074.2
035100      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      SQ1074.2
035200      ERROR-HOLD. ADD DELETE-CNT TO ERROR-HOLD.                   SQ1074.2
035300      ADD PASS-COUNTER TO ERROR-HOLD.                             SQ1074.2
035400*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   SQ1074.2
035500      MOVE PASS-COUNTER TO CCVS-E-4-1.                            SQ1074.2
035600      MOVE ERROR-HOLD TO CCVS-E-4-2.                              SQ1074.2
035700      MOVE CCVS-E-4 TO CCVS-E-2-2.                                SQ1074.2
035800      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           SQ1074.2
035900  END-ROUTINE-12.                                                 SQ1074.2
036000      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        SQ1074.2
036100     IF       ERROR-COUNTER IS EQUAL TO ZERO                      SQ1074.2
036200         MOVE "NO " TO ERROR-TOTAL                                SQ1074.2
036300         ELSE                                                     SQ1074.2
036400         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       SQ1074.2
036500     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           SQ1074.2
036600     PERFORM WRITE-LINE.                                          SQ1074.2
036700 END-ROUTINE-13.                                                  SQ1074.2
036800     IF DELETE-CNT IS EQUAL TO ZERO                               SQ1074.2
036900         MOVE "NO " TO ERROR-TOTAL  ELSE                          SQ1074.2
037000         MOVE DELETE-CNT TO ERROR-TOTAL.                          SQ1074.2
037100     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   SQ1074.2
037200     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ1074.2
037300      IF   INSPECT-COUNTER EQUAL TO ZERO                          SQ1074.2
037400          MOVE "NO " TO ERROR-TOTAL                               SQ1074.2
037500      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   SQ1074.2
037600      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            SQ1074.2
037700      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          SQ1074.2
037800     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ1074.2
037900 WRITE-LINE.                                                      SQ1074.2
038000     ADD 1 TO RECORD-COUNT.                                       SQ1074.2
038100*    IF RECORD-COUNT GREATER 50                                   SQ1074.2
038200*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          SQ1074.2
038300*        MOVE SPACE TO DUMMY-RECORD                               SQ1074.2
038400*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  SQ1074.2
038500*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             SQ1074.2
038600*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     SQ1074.2
038700*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          SQ1074.2
038800*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          SQ1074.2
038900*        MOVE ZERO TO RECORD-COUNT.                               SQ1074.2
039000     PERFORM WRT-LN.                                              SQ1074.2
039100 WRT-LN.                                                          SQ1074.2
039200     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               SQ1074.2
039300     MOVE SPACE TO DUMMY-RECORD.                                  SQ1074.2
039400 BLANK-LINE-PRINT.                                                SQ1074.2
039500     PERFORM WRT-LN.                                              SQ1074.2
039600 FAIL-ROUTINE.                                                    SQ1074.2
039700     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.   SQ1074.2
039800     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.    SQ1074.2
039900     MOVE "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.    SQ1074.2
040000     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     SQ1074.2
040100     GO TO FAIL-ROUTINE-EX.                                       SQ1074.2
040200 FAIL-ROUTINE-WRITE.                                              SQ1074.2
040300     MOVE TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE           SQ1074.2
040400     MOVE TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES.   SQ1074.2
040500 FAIL-ROUTINE-EX. EXIT.                                           SQ1074.2
040600 BAIL-OUT.                                                        SQ1074.2
040700     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.       SQ1074.2
040800     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.               SQ1074.2
040900 BAIL-OUT-WRITE.                                                  SQ1074.2
041000     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  SQ1074.2
041100     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     SQ1074.2
041200 BAIL-OUT-EX. EXIT.                                               SQ1074.2
041300 CCVS1-EXIT.                                                      SQ1074.2
041400     EXIT.                                                        SQ1074.2
041500 SECT-SQ107A-0001 SECTION.                                        SQ1074.2
041600 SEQ-INIT-017.                                                    SQ1074.2
041700     MOVE "SQ-VS7" TO XFILE-NAME (1).                             SQ1074.2
041800     MOVE CCVS-PGM-ID TO XPROGRAM-NAME (1).                       SQ1074.2
041900     MOVE "RC" TO CHARS-OR-RECORDS (1).                           SQ1074.2
042000     MOVE 0001 TO XBLOCK-SIZE (1).                                SQ1074.2
042100     MOVE 000450 TO RECORDS-IN-FILE (1).                          SQ1074.2
042200     MOVE "SQ" TO XFILE-ORGANIZATION (1).                         SQ1074.2
042300     MOVE "S" TO XLABEL-TYPE (1).                                 SQ1074.2
042400     MOVE 000000 TO XRECORD-NUMBER (1).                           SQ1074.2
042500     MOVE ZERO TO COUNT-OF-RECS.                                  SQ1074.2
042600     OPEN OUTPUT SQ-VS7.                                          SQ1074.2
042700     MOVE "MULTIPLE LENGTH RECS" TO SQ-VS7-FILLER.                SQ1074.2
042800 SEQ-TEST-017.                                                    SQ1074.2
042900     PERFORM WRITE-SHORT-REC.                                     SQ1074.2
043000     PERFORM WRITE-LONG-REC.                                      SQ1074.2
043100     PERFORM WRITE-SHORT-REC 10 TIMES.                            SQ1074.2
043200     PERFORM WRITE-LONG-REC 100 TIMES.                            SQ1074.2
043300     PERFORM WRITE-SHORT-REC 338 TIMES.                           SQ1074.2
043400 SEQ-WRITE-017.                                                   SQ1074.2
043500     MOVE "CREATE FILE SQ-VS7" TO FEATURE.                        SQ1074.2
043600     MOVE "SEQ-TEST-017" TO PAR-NAME.                             SQ1074.2
043700     MOVE "FILE CREATED, RECS =" TO COMPUTED-A.                   SQ1074.2
043800     MOVE COUNT-OF-RECS TO CORRECT-18V0.                          SQ1074.2
043900     MOVE "FILE HAS 120 AND 151 CHAR RECS" TO RE-MARK.            SQ1074.2
044000     PERFORM PRINT-DETAIL.                                        SQ1074.2
044100*        A SEQUENTIAL MASS STORAGE FILE CONTAINING 450            SQ1074.2
044200*    RECORDS HAS BEEN CREATED.  THE FILE CONTAINS RECORDS         SQ1074.2
044300*    OF 120 CHARACTERS AND RECORDS OF 151 CHARACTERS.  THE        SQ1074.2
044400*    SEQUENCE IN WHICH THE RECORDS WERE WRITTEN IS S-L-10S-       SQ1074.2
044500*    100L-338S.                                                   SQ1074.2
044600 SEQ-CLOSE-017.                                                   SQ1074.2
044700     CLOSE SQ-VS7.                                                SQ1074.2
044800     GO TO READ-INIT-GF-01.                                       SQ1074.2
044900 WRITE-SHORT-REC.                                                 SQ1074.2
045000     MOVE "R1-M-G" TO XRECORD-NAME (1).                           SQ1074.2
045100     MOVE 000120 TO XRECORD-LENGTH (1).                           SQ1074.2
045200     ADD 1 TO COUNT-OF-RECS.                                      SQ1074.2
045300     MOVE COUNT-OF-RECS TO XRECORD-NUMBER (1).                    SQ1074.2
045400     MOVE "SHORT" TO LONG-OR-SHORT.                               SQ1074.2
045500     MOVE COUNT-OF-RECS TO SQ-VS7-RECNO.                          SQ1074.2
045600     MOVE FILE-RECORD-INFO-P1-120 (1) TO SQ-VS7R1-FIRST.          SQ1074.2
045700     WRITE SQ-VS7R1-M-G-120.                                      SQ1074.2
045800 WRITE-LONG-REC.                                                  SQ1074.2
045900     MOVE "R2-M-G" TO XRECORD-NAME (1).                           SQ1074.2
046000     MOVE 000151 TO XRECORD-LENGTH (1).                           SQ1074.2
046100     ADD 1 TO COUNT-OF-RECS.                                      SQ1074.2
046200     MOVE COUNT-OF-RECS TO XRECORD-NUMBER (1).                    SQ1074.2
046300     MOVE "LONG" TO LONG-OR-SHORT.                                SQ1074.2
046400     MOVE COUNT-OF-RECS TO SQ-VS7-RECNO.                          SQ1074.2
046500     MOVE FILE-RECORD-INFO-P1-120 (1) TO SQ-VS7R2-FIRST.          SQ1074.2
046600     WRITE SQ-VS7R2-M-G-151.                                      SQ1074.2
046700 READ-INIT-GF-01.                                                 SQ1074.2
046800     MOVE ZERO TO COUNT-OF-RECS.                                  SQ1074.2
046900     MOVE ZERO TO EOF-FLAG.                                       SQ1074.2
047000     MOVE ZERO TO RECORDS-IN-ERROR.                               SQ1074.2
047100     MOVE ZERO TO ERROR-FLAG.                                     SQ1074.2
047200     OPEN INPUT SQ-VS7.                                           SQ1074.2
047300 READ-TEST-GF-01.                                                 SQ1074.2
047400     PERFORM READ-SHORT-REC THRU READ-SHORT-REC-EXIT.             SQ1074.2
047500     IF EOF-FLAG EQUAL TO 1                                       SQ1074.2
047600         MOVE "EOF ON FIRST READ" TO RE-MARK                      SQ1074.2
047700         GO TO SEQ-EOF-018.                                       SQ1074.2
047800     IF ERROR-FLAG EQUAL TO 1                                     SQ1074.2
047900         GO TO READ-FAIL-GF-01.                                   SQ1074.2
048000 READ-PASS-GF-01.                                                 SQ1074.2
048100     PERFORM PASS.                                                SQ1074.2
048200     GO TO READ-WRITE-GF-01.                                      SQ1074.2
048300 READ-FAIL-GF-01.                                                 SQ1074.2
048400     PERFORM FAIL.                                                SQ1074.2
048500     MOVE "ERROR ON FIRST READ" TO RE-MARK.                       SQ1074.2
048600 READ-WRITE-GF-01.                                                SQ1074.2
048700     MOVE "READ SHORT RECORD" TO FEATURE.                         SQ1074.2
048800     MOVE "READ-TEST-GF-01" TO PAR-NAME.                          SQ1074.2
048900     PERFORM PRINT-DETAIL.                                        SQ1074.2
049000     GO TO READ-INIT-GF-02.                                       SQ1074.2
049100 READ-SHORT-REC.                                                  SQ1074.2
049200     IF EOF-FLAG EQUAL TO 1                                       SQ1074.2
049300         GO TO READ-SHORT-REC-EXIT.                               SQ1074.2
049400     READ SQ-VS7 AT END                                           SQ1074.2
049500         MOVE 1 TO EOF-FLAG                                       SQ1074.2
049600         GO TO READ-SHORT-REC-EXIT.                               SQ1074.2
049700     ADD 1 TO COUNT-OF-RECS.                                      SQ1074.2
049800     MOVE SQ-VS7R1-FIRST TO FILE-RECORD-INFO-P1-120 (1).          SQ1074.2
049900     IF XRECORD-NAME (1) NOT EQUAL TO "R1-M-G"                    SQ1074.2
050000         GO TO READ-SHORT-REC-ERROR.                              SQ1074.2
050100     IF XRECORD-LENGTH (1) NOT EQUAL TO 120                       SQ1074.2
050200         GO TO READ-SHORT-REC-ERROR.                              SQ1074.2
050300     IF COUNT-OF-RECS NOT EQUAL TO XRECORD-NUMBER (1)             SQ1074.2
050400         GO TO READ-SHORT-REC-ERROR.                              SQ1074.2
050500     IF XLABEL-TYPE (1) EQUAL TO "S"                              SQ1074.2
050600         GO TO READ-SHORT-REC-EXIT.                               SQ1074.2
050700 READ-SHORT-REC-ERROR.                                            SQ1074.2
050800     ADD 1 TO RECORDS-IN-ERROR.                                   SQ1074.2
050900     MOVE 1 TO ERROR-FLAG.                                        SQ1074.2
051000 READ-SHORT-REC-EXIT.                                             SQ1074.2
051100     EXIT.                                                        SQ1074.2
051200 READ-INIT-GF-02.                                                 SQ1074.2
051300     MOVE ZERO TO ERROR-FLAG.                                     SQ1074.2
051400 READ-TEST-GF-02.                                                 SQ1074.2
051500     PERFORM READ-LONG-REC THRU READ-LONG-REC-EXIT.               SQ1074.2
051600     IF EOF-FLAG EQUAL TO 1                                       SQ1074.2
051700         MOVE "EOF ON SECOND READ" TO RE-MARK                     SQ1074.2
051800         GO TO SEQ-EOF-018.                                       SQ1074.2
051900     IF ERROR-FLAG EQUAL TO 1                                     SQ1074.2
052000         GO TO READ-FAIL-GF-02.                                   SQ1074.2
052100 READ-PASS-GF-02.                                                 SQ1074.2
052200     PERFORM PASS.                                                SQ1074.2
052300     GO TO READ-WRITE-GF-02.                                      SQ1074.2
052400 READ-FAIL-GF-02.                                                 SQ1074.2
052500     PERFORM FAIL.                                                SQ1074.2
052600     MOVE "VII-23; ERROR ON SECOND READ" TO RE-MARK.              SQ1074.2
052700 READ-WRITE-GF-02.                                                SQ1074.2
052800     MOVE "READ LONG RECORD" TO FEATURE.                          SQ1074.2
052900     MOVE "READ-TEST-GF-02" TO PAR-NAME.                          SQ1074.2
053000     PERFORM PRINT-DETAIL.                                        SQ1074.2
053100     GO TO READ-INIT-GF-03.                                       SQ1074.2
053200 READ-LONG-REC.                                                   SQ1074.2
053300     IF EOF-FLAG EQUAL TO 1                                       SQ1074.2
053400         GO TO READ-LONG-REC-EXIT.                                SQ1074.2
053500     READ SQ-VS7 END                                              SQ1074.2
053600         MOVE 1 TO EOF-FLAG                                       SQ1074.2
053700         GO TO READ-LONG-REC-EXIT.                                SQ1074.2
053800     ADD 1 TO COUNT-OF-RECS.                                      SQ1074.2
053900     MOVE SQ-VS7R2-FIRST TO FILE-RECORD-INFO-P1-120 (1).          SQ1074.2
054000     IF XRECORD-NAME (1) NOT EQUAL TO "R2-M-G"                    SQ1074.2
054100         GO TO READ-LONG-REC-ERROR.                               SQ1074.2
054200     IF XRECORD-LENGTH (1) NOT EQUAL TO 151                       SQ1074.2
054300         GO TO READ-LONG-REC-ERROR.                               SQ1074.2
054400     MOVE COUNT-OF-RECS TO SAVE-COUNT-OF-RECS.                    SQ1074.2
054500     IF SAVE-COUNT-OF-RECS NOT EQUAL TO SQ-VS7-RECNO              SQ1074.2
054600         GO TO READ-LONG-REC-ERROR.                               SQ1074.2
054700     IF LONG-OR-SHORT EQUAL TO "LONG "                            SQ1074.2
054800         GO TO READ-LONG-REC-EXIT.                                SQ1074.2
054900 READ-LONG-REC-ERROR.                                             SQ1074.2
055000     ADD 1 TO RECORDS-IN-ERROR.                                   SQ1074.2
055100     MOVE 1 TO ERROR-FLAG.                                        SQ1074.2
055200 READ-LONG-REC-EXIT.                                              SQ1074.2
055300     EXIT.                                                        SQ1074.2
055400 READ-INIT-GF-03.                                                 SQ1074.2
055500     MOVE ZERO TO ERROR-FLAG.                                     SQ1074.2
055600 READ-TEST-GF-03.                                                 SQ1074.2
055700     PERFORM READ-SHORT-REC THRU READ-SHORT-REC-EXIT 10 TIMES.    SQ1074.2
055800     IF EOF-FLAG EQUAL TO 1                                       SQ1074.2
055900         MOVE "UNEXPECTED EOF" TO RE-MARK                         SQ1074.2
056000         GO TO SEQ-EOF-018.                                       SQ1074.2
056100     IF ERROR-FLAG EQUAL TO 1                                     SQ1074.2
056200         GO TO READ-FAIL-GF-03.                                   SQ1074.2
056300 READ-PASS-GF-03.                                                 SQ1074.2
056400     PERFORM PASS.                                                SQ1074.2
056500     GO TO READ-WRITE-GF-03.                                      SQ1074.2
056600 READ-FAIL-GF-03.                                                 SQ1074.2
056700     MOVE "VII-23; ERROR READING SHORT RECORD" TO RE-MARK.        SQ1074.2
056800     PERFORM FAIL.                                                SQ1074.2
056900 READ-WRITE-GF-03.                                                SQ1074.2
057000     MOVE "READ SHORT RECORDS" TO FEATURE.                        SQ1074.2
057100     MOVE "READ-TEST-GF-03" TO PAR-NAME.                          SQ1074.2
057200     PERFORM PRINT-DETAIL.                                        SQ1074.2
057300 READ-INIT-GF-04.                                                 SQ1074.2
057400     MOVE ZERO TO ERROR-FLAG.                                     SQ1074.2
057500 READ-TEST-GF-04.                                                 SQ1074.2
057600     PERFORM READ-LONG-REC THRU READ-LONG-REC-EXIT 100 TIMES.     SQ1074.2
057700     IF EOF-FLAG EQUAL TO 1                                       SQ1074.2
057800         MOVE "UNEXPECTED EOF" TO RE-MARK                         SQ1074.2
057900         GO TO SEQ-EOF-018.                                       SQ1074.2
058000     IF ERROR-FLAG EQUAL TO 1                                     SQ1074.2
058100         GO TO READ-FAIL-GF-04.                                   SQ1074.2
058200 READ-PASS-GF-04.                                                 SQ1074.2
058300     PERFORM PASS.                                                SQ1074.2
058400     GO TO READ-WRITE-GF-04.                                      SQ1074.2
058500 READ-FAIL-GF-04.                                                 SQ1074.2
058600     PERFORM FAIL.                                                SQ1074.2
058700     MOVE "VII-23; ERROR READING LONG RECORD" TO RE-MARK.         SQ1074.2
058800 READ-WRITE-GF-04.                                                SQ1074.2
058900     MOVE "READ LONG RECORDS" TO FEATURE.                         SQ1074.2
059000     MOVE "READ-TEST-GF-04" TO PAR-NAME.                          SQ1074.2
059100     PERFORM PRINT-DETAIL.                                        SQ1074.2
059200 READ-INIT-GF-06.                                                 SQ1074.2
059300     MOVE ZERO TO ERROR-FLAG.                                     SQ1074.2
059400 READ-TEST-GF-05.                                                 SQ1074.2
059500     PERFORM READ-SHORT-REC THRU READ-SHORT-REC-EXIT 338 TIMES.   SQ1074.2
059600     IF EOF-FLAG EQUAL TO 1                                       SQ1074.2
059700         MOVE "UNEXPECTED EOF" TO RE-MARK                         SQ1074.2
059800         GO TO SEQ-EOF-018.                                       SQ1074.2
059900     IF ERROR-FLAG EQUAL TO 1                                     SQ1074.2
060000         GO TO READ-FAIL-GF-05.                                   SQ1074.2
060100 READ-PASS-GF-05.                                                 SQ1074.2
060200     PERFORM PASS.                                                SQ1074.2
060300     GO TO READ-WRITE-GF-05.                                      SQ1074.2
060400 READ-FAIL-GF-05.                                                 SQ1074.2
060500     PERFORM FAIL.                                                SQ1074.2
060600     MOVE "VII-23; ERROR READING SHORT RECORD" TO RE-MARK.        SQ1074.2
060700 READ-WRITE-GF-05.                                                SQ1074.2
060800     MOVE "READ SHORT RECORDS" TO FEATURE.                        SQ1074.2
060900     MOVE "READ-TEST-GF-05" TO PAR-NAME.                          SQ1074.2
061000     PERFORM PRINT-DETAIL.                                        SQ1074.2
061100 SEQ-INIT-018.                                                    SQ1074.2
061200     READ SQ-VS7 RECORD END                                       SQ1074.2
061300         GO TO SEQ-TEST-018.                                      SQ1074.2
061400     MOVE "MORE THAN 450 RECORDS" TO RE-MARK.                     SQ1074.2
061500     GO TO SEQ-FAIL-018.                                          SQ1074.2
061600 SEQ-EOF-018.                                                     SQ1074.2
061700     MOVE "RECORDS READ =" TO COMPUTED-A.                         SQ1074.2
061800     MOVE COUNT-OF-RECS TO CORRECT-18V0.                          SQ1074.2
061900     GO TO SEQ-FAIL-018.                                          SQ1074.2
062000 SEQ-TEST-018.                                                    SQ1074.2
062100     IF RECORDS-IN-ERROR NOT EQUAL TO ZERO                        SQ1074.2
062200         MOVE "RECORDS IN ERROR =" TO COMPUTED-A                  SQ1074.2
062300     MOVE RECORDS-IN-ERROR TO CORRECT-18V0                        SQ1074.2
062400         GO TO SEQ-FAIL-018.                                      SQ1074.2
062500 SEQ-PASS-018.                                                    SQ1074.2
062600     PERFORM PASS.                                                SQ1074.2
062700     GO TO SEQ-WRITE-018.                                         SQ1074.2
062800 SEQ-FAIL-018.                                                    SQ1074.2
062900     PERFORM FAIL.                                                SQ1074.2
063000 SEQ-WRITE-018.                                                   SQ1074.2
063100     MOVE "SEQ-TEST-018" TO PAR-NAME.                             SQ1074.2
063200     MOVE "VERIFY FILE SQ-VS7" TO FEATURE.                        SQ1074.2
063300     PERFORM PRINT-DETAIL.                                        SQ1074.2
063400 SEQ-CLOSE-018.                                                   SQ1074.2
063500     CLOSE SQ-VS7.                                                SQ1074.2
063600 SECT-SQ107A-0002 SECTION.                                        SQ1074.2
063700*        THIS SECTION CHECKS IF THE ENTIRE RECORD AREA IS         SQ1074.2
063800*    WRITTEN ON THE MASS STORAGE DEVICE WHEN A SHORT RECORD       SQ1074.2
063900*    IS WRITTEN.  THE RECORD NUMBER IN CHARACTERS 126 THROUGH     SQ1074.2
064000*    130 IS UNIQUE FOR EACH RECORD.                               SQ1074.2
064100 INFO-INIT-004.                                                   SQ1074.2
064200     OPEN INPUT SQ-VS7.                                           SQ1074.2
064300     MOVE ZERO TO COUNT-OF-RECS.                                  SQ1074.2
064400 INFO-TEST-004.                                                   SQ1074.2
064500     READ SQ-VS7 AT END                                           SQ1074.2
064600         GO TO INFO-END.                                          SQ1074.2
064700     ADD 1 TO COUNT-OF-RECS.                                      SQ1074.2
064800     IF SQ-VS7-RECNO NOT EQUAL TO "00001"                         SQ1074.2
064900         GO TO NO-INFO-004.                                       SQ1074.2
065000     MOVE "MAXIMUM RECORD SIZE WRITTEN" TO RE-MARK.               SQ1074.2
065100     MOVE "RECORD READ =" TO COMPUTED-A.                          SQ1074.2
065200     MOVE 0001 TO CORRECT-18V0.                                   SQ1074.2
065300     GO TO INFO-WRITE-004.                                        SQ1074.2
065400 NO-INFO-004.                                                     SQ1074.2
065500     MOVE "NO DEFINITE CONCLUSION POSSIBLE" TO RE-MARK.           SQ1074.2
065600 INFO-WRITE-004.                                                  SQ1074.2
065700     MOVE "READ SHORT RECORD" TO FEATURE.                         SQ1074.2
065800     MOVE "SEQ-INFO-004" TO PAR-NAME.                             SQ1074.2
065900     PERFORM PRINT-DETAIL.                                        SQ1074.2
066000 INFO-INIT-005.                                                   SQ1074.2
066100     READ SQ-VS7 RECORD AT END                                    SQ1074.2
066200         GO TO INFO-END.                                          SQ1074.2
066300     READ SQ-VS7 END                                              SQ1074.2
066400         GO TO INFO-END.                                          SQ1074.2
066500 INFO-TEST-005.                                                   SQ1074.2
066600     READ SQ-VS7 AT END                                           SQ1074.2
066700         GO TO INFO-END.                                          SQ1074.2
066800     IF SQ-VS7-RECNO NOT EQUAL TO "00004"                         SQ1074.2
066900         GO TO NO-INFO-005.                                       SQ1074.2
067000     MOVE "MAXIMUM RECORD SIZE WRITTEN" TO RE-MARK.               SQ1074.2
067100     MOVE "RECORD READ =" TO COMPUTED-A.                          SQ1074.2
067200     MOVE 0004 TO CORRECT-18V0.                                   SQ1074.2
067300     GO TO INFO-WRITE-005.                                        SQ1074.2
067400 NO-INFO-005.                                                     SQ1074.2
067500     MOVE "NO DEFINITE CONCLUSION POSSIBLE" TO RE-MARK.           SQ1074.2
067600 INFO-WRITE-005.                                                  SQ1074.2
067700     MOVE "READ SHORT RECORD" TO FEATURE.                         SQ1074.2
067800     MOVE "SEQ-INFO-005" TO PAR-NAME.                             SQ1074.2
067900     PERFORM PRINT-DETAIL.                                        SQ1074.2
068000 INFO-INIT-006.                                                   SQ1074.2
068100     ADD 3 TO COUNT-OF-RECS.                                      SQ1074.2
068200 INFO-INIT-006-1.                                                 SQ1074.2
068300     READ SQ-VS7 RECORD                                           SQ1074.2
068400         END GO TO INFO-END.                                      SQ1074.2
068500     ADD 1 TO COUNT-OF-RECS.                                      SQ1074.2
068600     IF COUNT-OF-RECS EQUAL TO 450                                SQ1074.2
068700         GO TO INFO-TEST-006.                                     SQ1074.2
068800     GO TO INFO-INIT-006-1.                                       SQ1074.2
068900 INFO-TEST-006.                                                   SQ1074.2
069000     IF SQ-VS7-RECNO NOT EQUAL TO "00450"                         SQ1074.2
069100         GO TO NO-INFO-006.                                       SQ1074.2
069200     MOVE "MAXIMUM RECORD SIZE WRITTEN" TO RE-MARK.               SQ1074.2
069300     MOVE "RECORD READ =" TO COMPUTED-A.                          SQ1074.2
069400     MOVE 0450 TO CORRECT-18V0.                                   SQ1074.2
069500     GO TO INFO-WRITE-006.                                        SQ1074.2
069600 NO-INFO-006.                                                     SQ1074.2
069700     MOVE "NO DEFINITE CONCLUSION POSSIBLE" TO RE-MARK.           SQ1074.2
069800 INFO-WRITE-006.                                                  SQ1074.2
069900     MOVE "READ SHORT RECORD" TO FEATURE.                         SQ1074.2
070000     MOVE "SEQ-INFO-006" TO PAR-NAME.                             SQ1074.2
070100     PERFORM PRINT-DETAIL.                                        SQ1074.2
070200 INFO-END.                                                        SQ1074.2
070300     CLOSE SQ-VS7.                                                SQ1074.2
070400 TERMINATE-ROUTINE.                                               SQ1074.2
070500     EXIT.                                                        SQ1074.2
070600 CCVS-EXIT SECTION.                                               SQ1074.2
070700 CCVS-999999.                                                     SQ1074.2
070800     GO TO CLOSE-FILES.                                           SQ1074.2
      *END-OF,SQ107A                                                                  
