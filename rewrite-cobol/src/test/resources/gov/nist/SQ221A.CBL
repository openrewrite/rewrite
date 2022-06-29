      *HEADER,COBOL,SQ221A                                                            
000100 IDENTIFICATION DIVISION.                                         SQ2214.2
000200 PROGRAM-ID.                                                      SQ2214.2
000300     SQ221A.                                                      SQ2214.2
000400****************************************************************  SQ2214.2
000500*                                                              *  SQ2214.2
000600*    VALIDATION FOR:-                                          *  SQ2214.2
000700*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ2214.2
000800*                                                              *  SQ2214.2
000900*    CREATION DATE     /     VALIDATION DATE                   *  SQ2214.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ2214.2
001100*                                                              *  SQ2214.2
001200*        THIS ROUTINE CHECKS:                                     SQ2214.2
001300*                                                                 SQ2214.2
001400*           RECORD    VARYING    DEPENDING RECORD-LENGTH          SQ2214.2
001500*                                                                 SQ2214.2
001600*                                                                 SQ2214.2
001700*        THIS ROUTINE BUILDS A SEQUENTIAL MASS STORAGE FILE       SQ2214.2
001800*    WHICH CONTAINS BOTH 120 CHARACTER AND 151 CHARACTER          SQ2214.2
001900*    RECORDS.  THE MASS STORAGE FILE CONSISTS OF 1 SHORT,         SQ2214.2
002000*    1 LONG, 10 SHORT, 100 LONG, AND 338 SHORT RECORDS FOR        SQ2214.2
002100*    A TOTAL OF 450 RECORDS IN THE FILE.  THE MASS STORAGE        SQ2214.2
002200*    FILE IS READ AND FIELDS IN THE RECORDS ARE CHECKED           SQ2214.2
002300*    AGAINST THE EXPECTED VALUES.                                 SQ2214.2
002400*                                                                 SQ2214.2
002500*        AN INFORMATION SECTION AT THE END OF THE ROUTINE         SQ2214.2
002600*    CHECKS THE FIELD WHICH CONTAINS THE XRECORD-NUMBER.          SQ2214.2
002700*    THIS FIELD IS PART OF A LONG RECORD ONLY.  IF THE            SQ2214.2
002800*    XRECORD-NUMBER IS THERE FOR A SHORT RECORD, IT MEANS         SQ2214.2
002900*    THE MAXIMUM SIZE RECORD IS ALWAYS WRITTEN.                   SQ2214.2
003000 ENVIRONMENT DIVISION.                                            SQ2214.2
003100 CONFIGURATION SECTION.                                           SQ2214.2
003200 SOURCE-COMPUTER.                                                 SQ2214.2
003300     XXXXX082.                                                    SQ2214.2
003400 OBJECT-COMPUTER.                                                 SQ2214.2
003500     XXXXX083.                                                    SQ2214.2
003600 INPUT-OUTPUT SECTION.                                            SQ2214.2
003700 FILE-CONTROL.                                                    SQ2214.2
003800*    SELECT RAW-DATA   ASSIGN TO                                  SQ2214.2
003900*    XXXXX062                                                     SQ2214.2
004000*           ORGANIZATION IS INDEXED                               SQ2214.2
004100*           ACCESS MODE IS RANDOM                                 SQ2214.2
004200*           RECORD KEY IS RAW-DATA-KEY.                           SQ2214.2
004300     SELECT PRINT-FILE ASSIGN TO                                  SQ2214.2
004400     XXXXX055.                                                    SQ2214.2
004500     SELECT SQ-VS7 ASSIGN TO                                      SQ2214.2
004600     XXXXX014                                                     SQ2214.2
004700     ORGANIZATION SEQUENTIAL                                      SQ2214.2
004800     ACCESS SEQUENTIAL.                                           SQ2214.2
004900 DATA DIVISION.                                                   SQ2214.2
005000 FILE SECTION.                                                    SQ2214.2
005100*                                                                 SQ2214.2
005200*FD  RAW-DATA.                                                    SQ2214.2
005300*                                                                 SQ2214.2
005400*01  RAW-DATA-SATZ.                                               SQ2214.2
005500*    05  RAW-DATA-KEY        PIC X(6).                            SQ2214.2
005600*    05  C-DATE              PIC 9(6).                            SQ2214.2
005700*    05  C-TIME              PIC 9(8).                            SQ2214.2
005800*    05  C-NO-OF-TESTS       PIC 99.                              SQ2214.2
005900*    05  C-OK                PIC 999.                             SQ2214.2
006000*    05  C-ALL               PIC 999.                             SQ2214.2
006100*    05  C-FAIL              PIC 999.                             SQ2214.2
006200*    05  C-DELETED           PIC 999.                             SQ2214.2
006300*    05  C-INSPECT           PIC 999.                             SQ2214.2
006400*    05  C-NOTE              PIC X(13).                           SQ2214.2
006500*    05  C-INDENT            PIC X.                               SQ2214.2
006600*    05  C-ABORT             PIC X(8).                            SQ2214.2
006700 FD  PRINT-FILE                                                   SQ2214.2
006800C    LABEL RECORDS                                                SQ2214.2
006900C    XXXXX084                                                     SQ2214.2
007000C    DATA RECORD IS PRINT-REC DUMMY-RECORD                        SQ2214.2
007100               .                                                  SQ2214.2
007200 01  PRINT-REC PICTURE X(120).                                    SQ2214.2
007300 01  DUMMY-RECORD PICTURE X(120).                                 SQ2214.2
007400 FD  SQ-VS7                                                       SQ2214.2
007500C    LABEL RECORDS ARE STANDARD                                   SQ2214.2
007600     RECORD    VARYING    DEPENDING RECORD-LENGTH.                SQ2214.2
007700 01  SQ-VS7R1-M-G-120.                                            SQ2214.2
007800     02  SQ-VS7R1-FIRST PICTURE X(120).                           SQ2214.2
007900 01  SQ-VS7R2-M-G-151.                                            SQ2214.2
008000     02  SQ-VS7R2-FIRST PICTURE X(120).                           SQ2214.2
008100     02  LONG-OR-SHORT  PICTURE X(5).                             SQ2214.2
008200     02  SQ-VS7-RECNO  PICTURE X(5).                              SQ2214.2
008300 02  SQ-VS7-FILLER  PICTURE X(21).                                SQ2214.2
008400 WORKING-STORAGE SECTION.                                         SQ2214.2
008500 01  RECORD-LENGTH       PICTURE 999  VALUE ZERO.                 SQ2214.2
008600 01  SAVE-COUNT-OF-RECS  PICTURE X(5) VALUE SPACE.                SQ2214.2
008700 01  COUNT-OF-RECS  PICTURE S9(5) COMPUTATIONAL.                  SQ2214.2
008800 01  RECORDS-IN-ERROR  PICTURE S9(5) COMPUTATIONAL.               SQ2214.2
008900 01  ERROR-FLAG PICTURE 9.                                        SQ2214.2
009000 01  EOF-FLAG  PICTURE 9.                                         SQ2214.2
009100 01  DUMP-AREA.                                                   SQ2214.2
009200     02  TYPE-OF-REC PICTURE X(5).                                SQ2214.2
009300     02  RECNO  PICTURE 9(5).                                     SQ2214.2
009400     02  REC-FILLER PICTURE X(21).                                SQ2214.2
009500 01  FILE-RECORD-INFORMATION-REC.                                 SQ2214.2
009600     03 FILE-RECORD-INFO-SKELETON.                                SQ2214.2
009700        05 FILLER                 PICTURE X(48)       VALUE       SQ2214.2
009800             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  SQ2214.2
009900        05 FILLER                 PICTURE X(46)       VALUE       SQ2214.2
010000             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    SQ2214.2
010100        05 FILLER                 PICTURE X(26)       VALUE       SQ2214.2
010200             ",LFIL=000000,ORG=  ,LBLR= ".                        SQ2214.2
010300        05 FILLER                 PICTURE X(37)       VALUE       SQ2214.2
010400             ",RECKEY=                             ".             SQ2214.2
010500        05 FILLER                 PICTURE X(38)       VALUE       SQ2214.2
010600             ",ALTKEY1=                             ".            SQ2214.2
010700        05 FILLER                 PICTURE X(38)       VALUE       SQ2214.2
010800             ",ALTKEY2=                             ".            SQ2214.2
010900        05 FILLER                 PICTURE X(7)        VALUE SPACE.SQ2214.2
011000     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              SQ2214.2
011100        05 FILE-RECORD-INFO-P1-120.                               SQ2214.2
011200           07 FILLER              PIC X(5).                       SQ2214.2
011300           07 XFILE-NAME           PIC X(6).                      SQ2214.2
011400           07 FILLER              PIC X(8).                       SQ2214.2
011500           07 XRECORD-NAME         PIC X(6).                      SQ2214.2
011600           07 FILLER              PIC X(1).                       SQ2214.2
011700           07 REELUNIT-NUMBER     PIC 9(1).                       SQ2214.2
011800           07 FILLER              PIC X(7).                       SQ2214.2
011900           07 XRECORD-NUMBER       PIC 9(6).                      SQ2214.2
012000           07 FILLER              PIC X(6).                       SQ2214.2
012100           07 UPDATE-NUMBER       PIC 9(2).                       SQ2214.2
012200           07 FILLER              PIC X(5).                       SQ2214.2
012300           07 ODO-NUMBER          PIC 9(4).                       SQ2214.2
012400           07 FILLER              PIC X(5).                       SQ2214.2
012500           07 XPROGRAM-NAME        PIC X(5).                      SQ2214.2
012600           07 FILLER              PIC X(7).                       SQ2214.2
012700           07 XRECORD-LENGTH       PIC 9(6).                      SQ2214.2
012800           07 FILLER              PIC X(7).                       SQ2214.2
012900           07 CHARS-OR-RECORDS    PIC X(2).                       SQ2214.2
013000           07 FILLER              PIC X(1).                       SQ2214.2
013100           07 XBLOCK-SIZE          PIC 9(4).                      SQ2214.2
013200           07 FILLER              PIC X(6).                       SQ2214.2
013300           07 RECORDS-IN-FILE     PIC 9(6).                       SQ2214.2
013400           07 FILLER              PIC X(5).                       SQ2214.2
013500           07 XFILE-ORGANIZATION   PIC X(2).                      SQ2214.2
013600           07 FILLER              PIC X(6).                       SQ2214.2
013700           07 XLABEL-TYPE          PIC X(1).                      SQ2214.2
013800        05 FILE-RECORD-INFO-P121-240.                             SQ2214.2
013900           07 FILLER              PIC X(8).                       SQ2214.2
014000           07 XRECORD-KEY          PIC X(29).                     SQ2214.2
014100           07 FILLER              PIC X(9).                       SQ2214.2
014200           07 ALTERNATE-KEY1      PIC X(29).                      SQ2214.2
014300           07 FILLER              PIC X(9).                       SQ2214.2
014400           07 ALTERNATE-KEY2      PIC X(29).                      SQ2214.2
014500           07 FILLER              PIC X(7).                       SQ2214.2
014600 01  TEST-RESULTS.                                                SQ2214.2
014700     02 FILLER                    PICTURE X VALUE SPACE.          SQ2214.2
014800     02 FEATURE                   PICTURE X(20) VALUE SPACE.      SQ2214.2
014900     02 FILLER                    PICTURE X VALUE SPACE.          SQ2214.2
015000     02 P-OR-F                    PICTURE X(5) VALUE SPACE.       SQ2214.2
015100     02 FILLER                    PICTURE X  VALUE SPACE.         SQ2214.2
015200     02  PAR-NAME.                                                SQ2214.2
015300       03 FILLER PICTURE X(12) VALUE SPACE.                       SQ2214.2
015400       03  PARDOT-X PICTURE X  VALUE SPACE.                       SQ2214.2
015500       03 DOTVALUE PICTURE 99  VALUE ZERO.                        SQ2214.2
015600       03 FILLER PIC X(5) VALUE SPACE.                            SQ2214.2
015700     02 FILLER PIC X(10) VALUE SPACE.                             SQ2214.2
015800     02 RE-MARK PIC X(61).                                        SQ2214.2
015900 01  TEST-COMPUTED.                                               SQ2214.2
016000     02 FILLER PIC X(30) VALUE SPACE.                             SQ2214.2
016100     02 FILLER PIC X(17) VALUE "       COMPUTED=".                SQ2214.2
016200     02 COMPUTED-X.                                               SQ2214.2
016300     03 COMPUTED-A                PICTURE X(20) VALUE SPACE.      SQ2214.2
016400     03 COMPUTED-N REDEFINES COMPUTED-A PICTURE -9(9).9(9).       SQ2214.2
016500     03 COMPUTED-0V18 REDEFINES COMPUTED-A  PICTURE -.9(18).      SQ2214.2
016600     03 COMPUTED-4V14 REDEFINES COMPUTED-A  PICTURE -9(4).9(14).  SQ2214.2
016700     03 COMPUTED-14V4 REDEFINES COMPUTED-A  PICTURE -9(14).9(4).  SQ2214.2
016800     03       CM-18V0 REDEFINES COMPUTED-A.                       SQ2214.2
016900         04 COMPUTED-18V0                   PICTURE -9(18).       SQ2214.2
017000         04 FILLER                          PICTURE X.            SQ2214.2
017100     03 FILLER PIC X(50) VALUE SPACE.                             SQ2214.2
017200 01  TEST-CORRECT.                                                SQ2214.2
017300     02 FILLER PIC X(30) VALUE SPACE.                             SQ2214.2
017400     02 FILLER PIC X(17) VALUE "       CORRECT =".                SQ2214.2
017500     02 CORRECT-X.                                                SQ2214.2
017600     03 CORRECT-A                 PICTURE X(20) VALUE SPACE.      SQ2214.2
017700     03 CORRECT-N REDEFINES CORRECT-A PICTURE -9(9).9(9).         SQ2214.2
017800     03 CORRECT-0V18 REDEFINES CORRECT-A    PICTURE -.9(18).      SQ2214.2
017900     03 CORRECT-4V14 REDEFINES CORRECT-A    PICTURE -9(4).9(14).  SQ2214.2
018000     03 CORRECT-14V4 REDEFINES CORRECT-A    PICTURE -9(14).9(4).  SQ2214.2
018100     03      CR-18V0 REDEFINES CORRECT-A.                         SQ2214.2
018200         04 CORRECT-18V0                    PICTURE -9(18).       SQ2214.2
018300         04 FILLER                          PICTURE X.            SQ2214.2
018400     03 FILLER PIC X(50) VALUE SPACE.                             SQ2214.2
018500 01  CCVS-C-1.                                                    SQ2214.2
018600     02 FILLER PICTURE IS X(99) VALUE IS " FEATURE              PASQ2214.2
018700-    "SS  PARAGRAPH-NAME                                          SQ2214.2
018800-    "        REMARKS".                                           SQ2214.2
018900     02 FILLER PICTURE IS X(20) VALUE IS SPACE.                   SQ2214.2
019000 01  CCVS-C-2.                                                    SQ2214.2
019100     02 FILLER PICTURE IS X VALUE IS SPACE.                       SQ2214.2
019200     02 FILLER PICTURE IS X(6) VALUE IS "TESTED".                 SQ2214.2
019300     02 FILLER PICTURE IS X(15) VALUE IS SPACE.                   SQ2214.2
019400     02 FILLER PICTURE IS X(4) VALUE IS "FAIL".                   SQ2214.2
019500     02 FILLER PICTURE IS X(94) VALUE IS SPACE.                   SQ2214.2
019600 01  REC-SKL-SUB PICTURE 9(2) VALUE ZERO.                         SQ2214.2
019700 01  REC-CT PICTURE 99 VALUE ZERO.                                SQ2214.2
019800 01  DELETE-CNT                   PICTURE 999  VALUE ZERO.        SQ2214.2
019900 01  ERROR-COUNTER PICTURE IS 999 VALUE IS ZERO.                  SQ2214.2
020000 01  INSPECT-COUNTER PIC 999 VALUE ZERO.                          SQ2214.2
020100 01  PASS-COUNTER PIC 999 VALUE ZERO.                             SQ2214.2
020200 01  TOTAL-ERROR PIC 999 VALUE ZERO.                              SQ2214.2
020300 01  ERROR-HOLD PIC 999 VALUE ZERO.                               SQ2214.2
020400 01  DUMMY-HOLD PIC X(120) VALUE SPACE.                           SQ2214.2
020500 01  RECORD-COUNT PIC 9(5) VALUE ZERO.                            SQ2214.2
020600 01  CCVS-H-1.                                                    SQ2214.2
020700     02  FILLER   PICTURE X(27)  VALUE SPACE.                     SQ2214.2
020800     02 FILLER PICTURE X(67) VALUE                                SQ2214.2
020900     " FEDERAL SOFTWARE TESTING CENTER COBOL COMPILER VALIDATION  SQ2214.2
021000-    " SYSTEM".                                                   SQ2214.2
021100     02  FILLER     PICTURE X(26)  VALUE SPACE.                   SQ2214.2
021200 01  CCVS-H-2.                                                    SQ2214.2
021300     02 FILLER PICTURE X(52) VALUE IS                             SQ2214.2
021400     "CCVS85 FSTC COPY, NOT FOR DISTRIBUTION.".                   SQ2214.2
021500     02 FILLER PICTURE IS X(19) VALUE IS "TEST RESULTS SET-  ".   SQ2214.2
021600     02 TEST-ID PICTURE IS X(9).                                  SQ2214.2
021700     02 FILLER PICTURE IS X(40) VALUE IS SPACE.                   SQ2214.2
021800 01  CCVS-H-3.                                                    SQ2214.2
021900     02  FILLER PICTURE X(34) VALUE                               SQ2214.2
022000     " FOR OFFICIAL USE ONLY    ".                                SQ2214.2
022100     02  FILLER PICTURE X(58) VALUE                               SQ2214.2
022200     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ2214.2
022300     02  FILLER PICTURE X(28) VALUE                               SQ2214.2
022400     "  COPYRIGHT   1985 ".                                       SQ2214.2
022500 01  CCVS-E-1.                                                    SQ2214.2
022600     02 FILLER PICTURE IS X(52) VALUE IS SPACE.                   SQ2214.2
022700     02 FILLER PICTURE IS X(14) VALUE IS "END OF TEST-  ".        SQ2214.2
022800     02 ID-AGAIN PICTURE IS X(9).                                 SQ2214.2
022900     02 FILLER PICTURE X(45) VALUE IS                             SQ2214.2
023000     " NTIS DISTRIBUTION COBOL 85".                               SQ2214.2
023100 01  CCVS-E-2.                                                    SQ2214.2
023200     02  FILLER                   PICTURE X(31)  VALUE            SQ2214.2
023300     SPACE.                                                       SQ2214.2
023400     02  FILLER                   PICTURE X(21)  VALUE SPACE.     SQ2214.2
023500     02 CCVS-E-2-2.                                               SQ2214.2
023600         03 ERROR-TOTAL PICTURE IS XXX VALUE IS SPACE.            SQ2214.2
023700         03 FILLER PICTURE IS X VALUE IS SPACE.                   SQ2214.2
023800         03 ENDER-DESC PIC X(46) VALUE "ERRORS ENCOUNTERED".      SQ2214.2
023900 01  CCVS-E-3.                                                    SQ2214.2
024000     02  FILLER PICTURE X(22) VALUE                               SQ2214.2
024100     " FOR OFFICIAL USE ONLY".                                    SQ2214.2
024200     02  FILLER PICTURE X(12) VALUE SPACE.                        SQ2214.2
024300     02  FILLER PICTURE X(58) VALUE                               SQ2214.2
024400     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ2214.2
024500     02  FILLER PICTURE X(13) VALUE SPACE.                        SQ2214.2
024600     02 FILLER PIC X(15) VALUE " COPYRIGHT 1985".                 SQ2214.2
024700 01  CCVS-E-4.                                                    SQ2214.2
024800     02 CCVS-E-4-1 PIC XXX VALUE SPACE.                           SQ2214.2
024900     02 FILLER PIC XXXX VALUE " OF ".                             SQ2214.2
025000     02 CCVS-E-4-2 PIC XXX VALUE SPACE.                           SQ2214.2
025100     02 FILLER PIC X(40) VALUE                                    SQ2214.2
025200      "  TESTS WERE EXECUTED SUCCESSFULLY".                       SQ2214.2
025300 01  XXINFO.                                                      SQ2214.2
025400     02 FILLER PIC X(30) VALUE "        *** INFORMATION  ***".    SQ2214.2
025500     02 INFO-TEXT.                                                SQ2214.2
025600     04 FILLER PIC X(20) VALUE SPACE.                             SQ2214.2
025700     04 XXCOMPUTED PIC X(20).                                     SQ2214.2
025800     04 FILLER PIC X(5) VALUE SPACE.                              SQ2214.2
025900     04 XXCORRECT PIC X(20).                                      SQ2214.2
026000 01  HYPHEN-LINE.                                                 SQ2214.2
026100     02 FILLER PICTURE IS X VALUE IS SPACE.                       SQ2214.2
026200     02 FILLER PICTURE IS X(65) VALUE IS "************************SQ2214.2
026300-    "*****************************************".                 SQ2214.2
026400     02 FILLER PICTURE IS X(54) VALUE IS "************************SQ2214.2
026500-    "******************************".                            SQ2214.2
026600 01  CCVS-PGM-ID PIC X(6) VALUE                                   SQ2214.2
026700     "SQ221A".                                                    SQ2214.2
026800 PROCEDURE DIVISION.                                              SQ2214.2
026900 CCVS1 SECTION.                                                   SQ2214.2
027000 OPEN-FILES.                                                      SQ2214.2
027100*    OPEN I-O RAW-DATA.                                           SQ2214.2
027200*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            SQ2214.2
027300*    READ RAW-DATA INVALID KEY GO TO END-E-1.                     SQ2214.2
027400*    MOVE "ABORTED " TO C-ABORT.                                  SQ2214.2
027500*    ADD 1 TO C-NO-OF-TESTS.                                      SQ2214.2
027600*    ACCEPT C-DATE  FROM DATE.                                    SQ2214.2
027700*    ACCEPT C-TIME  FROM TIME.                                    SQ2214.2
027800*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-1.             SQ2214.2
027900*END-E-1.                                                         SQ2214.2
028000*    CLOSE RAW-DATA.                                              SQ2214.2
028100     OPEN     OUTPUT PRINT-FILE.                                  SQ2214.2
028200     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   SQ2214.2
028300     MOVE    SPACE TO TEST-RESULTS.                               SQ2214.2
028400     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             SQ2214.2
028500     MOVE ZERO TO REC-SKL-SUB.                                    SQ2214.2
028600     PERFORM CCVS-INIT-FILE 9 TIMES.                              SQ2214.2
028700 CCVS-INIT-FILE.                                                  SQ2214.2
028800     ADD 1 TO REC-SKL-SUB.                                        SQ2214.2
028900     MOVE FILE-RECORD-INFO-SKELETON TO                            SQ2214.2
029000                  FILE-RECORD-INFO (REC-SKL-SUB).                 SQ2214.2
029100 CCVS-INIT-EXIT.                                                  SQ2214.2
029200     GO TO CCVS1-EXIT.                                            SQ2214.2
029300 CLOSE-FILES.                                                     SQ2214.2
029400     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   SQ2214.2
029500*    OPEN I-O RAW-DATA.                                           SQ2214.2
029600*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            SQ2214.2
029700*    READ RAW-DATA INVALID KEY GO TO END-E-2.                     SQ2214.2
029800*    MOVE "OK.     " TO C-ABORT.                                  SQ2214.2
029900*    MOVE PASS-COUNTER TO C-OK.                                   SQ2214.2
030000*    MOVE ERROR-HOLD   TO C-ALL.                                  SQ2214.2
030100*    MOVE ERROR-COUNTER TO C-FAIL.                                SQ2214.2
030200*    MOVE DELETE-CNT TO C-DELETED.                                SQ2214.2
030300*    MOVE INSPECT-COUNTER TO C-INSPECT.                           SQ2214.2
030400*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-2.             SQ2214.2
030500*END-E-2.                                                         SQ2214.2
030600*    CLOSE RAW-DATA.                                              SQ2214.2
030700 TERMINATE-CCVS.                                                  SQ2214.2
030800*    EXIT PROGRAM.                                                SQ2214.2
030900*TERMINATE-CALL.                                                  SQ2214.2
031000     STOP     RUN.                                                SQ2214.2
031100 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         SQ2214.2
031200 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           SQ2214.2
031300 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          SQ2214.2
031400 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-CNT.          SQ2214.2
031500     MOVE "****TEST DELETED****" TO RE-MARK.                      SQ2214.2
031600 PRINT-DETAIL.                                                    SQ2214.2
031700     IF REC-CT NOT EQUAL TO ZERO                                  SQ2214.2
031800             MOVE "." TO PARDOT-X                                 SQ2214.2
031900             MOVE REC-CT TO DOTVALUE.                             SQ2214.2
032000     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      SQ2214.2
032100     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               SQ2214.2
032200        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 SQ2214.2
032300          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 SQ2214.2
032400     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              SQ2214.2
032500     MOVE SPACE TO CORRECT-X.                                     SQ2214.2
032600     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         SQ2214.2
032700     MOVE     SPACE TO RE-MARK.                                   SQ2214.2
032800 HEAD-ROUTINE.                                                    SQ2214.2
032900     MOVE CCVS-H-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ2214.2
033000     MOVE CCVS-H-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.   SQ2214.2
033100     MOVE CCVS-H-3 TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.   SQ2214.2
033200 COLUMN-NAMES-ROUTINE.                                            SQ2214.2
033300     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ2214.2
033400     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ2214.2
033500     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ2214.2
033600 END-ROUTINE.                                                     SQ2214.2
033700     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.SQ2214.2
033800 END-RTN-EXIT.                                                    SQ2214.2
033900     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ2214.2
034000 END-ROUTINE-1.                                                   SQ2214.2
034100      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      SQ2214.2
034200      ERROR-HOLD. ADD DELETE-CNT TO ERROR-HOLD.                   SQ2214.2
034300      ADD PASS-COUNTER TO ERROR-HOLD.                             SQ2214.2
034400*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   SQ2214.2
034500      MOVE PASS-COUNTER TO CCVS-E-4-1.                            SQ2214.2
034600      MOVE ERROR-HOLD TO CCVS-E-4-2.                              SQ2214.2
034700      MOVE CCVS-E-4 TO CCVS-E-2-2.                                SQ2214.2
034800      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           SQ2214.2
034900  END-ROUTINE-12.                                                 SQ2214.2
035000      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        SQ2214.2
035100     IF       ERROR-COUNTER IS EQUAL TO ZERO                      SQ2214.2
035200         MOVE "NO " TO ERROR-TOTAL                                SQ2214.2
035300         ELSE                                                     SQ2214.2
035400         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       SQ2214.2
035500     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           SQ2214.2
035600     PERFORM WRITE-LINE.                                          SQ2214.2
035700 END-ROUTINE-13.                                                  SQ2214.2
035800     IF DELETE-CNT IS EQUAL TO ZERO                               SQ2214.2
035900         MOVE "NO " TO ERROR-TOTAL  ELSE                          SQ2214.2
036000         MOVE DELETE-CNT TO ERROR-TOTAL.                          SQ2214.2
036100     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   SQ2214.2
036200     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ2214.2
036300      IF   INSPECT-COUNTER EQUAL TO ZERO                          SQ2214.2
036400          MOVE "NO " TO ERROR-TOTAL                               SQ2214.2
036500      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   SQ2214.2
036600      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            SQ2214.2
036700      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          SQ2214.2
036800     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ2214.2
036900 WRITE-LINE.                                                      SQ2214.2
037000     ADD 1 TO RECORD-COUNT.                                       SQ2214.2
037100*    IF RECORD-COUNT GREATER 50                                   SQ2214.2
037200*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          SQ2214.2
037300*        MOVE SPACE TO DUMMY-RECORD                               SQ2214.2
037400*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  SQ2214.2
037500*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             SQ2214.2
037600*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     SQ2214.2
037700*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          SQ2214.2
037800*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          SQ2214.2
037900*        MOVE ZERO TO RECORD-COUNT.                               SQ2214.2
038000     PERFORM WRT-LN.                                              SQ2214.2
038100 WRT-LN.                                                          SQ2214.2
038200     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               SQ2214.2
038300     MOVE SPACE TO DUMMY-RECORD.                                  SQ2214.2
038400 BLANK-LINE-PRINT.                                                SQ2214.2
038500     PERFORM WRT-LN.                                              SQ2214.2
038600 FAIL-ROUTINE.                                                    SQ2214.2
038700     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.   SQ2214.2
038800     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.    SQ2214.2
038900     MOVE "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.    SQ2214.2
039000     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     SQ2214.2
039100     GO TO FAIL-ROUTINE-EX.                                       SQ2214.2
039200 FAIL-ROUTINE-WRITE.                                              SQ2214.2
039300     MOVE TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE           SQ2214.2
039400     MOVE TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES.   SQ2214.2
039500 FAIL-ROUTINE-EX. EXIT.                                           SQ2214.2
039600 BAIL-OUT.                                                        SQ2214.2
039700     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.       SQ2214.2
039800     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.               SQ2214.2
039900 BAIL-OUT-WRITE.                                                  SQ2214.2
040000     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  SQ2214.2
040100     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     SQ2214.2
040200 BAIL-OUT-EX. EXIT.                                               SQ2214.2
040300 CCVS1-EXIT.                                                      SQ2214.2
040400     EXIT.                                                        SQ2214.2
040500 SECT-SQ221A-0001 SECTION.                                        SQ2214.2
040600 WRITE-INIT-GF-01.                                                SQ2214.2
040700     MOVE "SQ-VS7" TO XFILE-NAME (1).                             SQ2214.2
040800     MOVE CCVS-PGM-ID TO XPROGRAM-NAME (1).                       SQ2214.2
040900     MOVE "RC" TO CHARS-OR-RECORDS (1).                           SQ2214.2
041000     MOVE 0001 TO XBLOCK-SIZE (1).                                SQ2214.2
041100     MOVE 000450 TO RECORDS-IN-FILE (1).                          SQ2214.2
041200     MOVE "SQ" TO XFILE-ORGANIZATION (1).                         SQ2214.2
041300     MOVE "S" TO XLABEL-TYPE (1).                                 SQ2214.2
041400     MOVE 000000 TO XRECORD-NUMBER (1).                           SQ2214.2
041500     MOVE ZERO TO COUNT-OF-RECS.                                  SQ2214.2
041600     OPEN OUTPUT SQ-VS7.                                          SQ2214.2
041700     MOVE "MULTIPLE LENGTH RECS" TO SQ-VS7-FILLER.                SQ2214.2
041800 WRITE-TEST-GF-01.                                                SQ2214.2
041900     PERFORM WRITE-SHORT-REC.                                     SQ2214.2
042000     PERFORM WRITE-LONG-REC.                                      SQ2214.2
042100     PERFORM WRITE-SHORT-REC 10 TIMES.                            SQ2214.2
042200     PERFORM WRITE-LONG-REC 100 TIMES.                            SQ2214.2
042300     PERFORM WRITE-SHORT-REC 338 TIMES.                           SQ2214.2
042400 WRITE-WRITE-GF-01.                                               SQ2214.2
042500     MOVE "CREATE FILE SQ-VS7" TO FEATURE.                        SQ2214.2
042600     MOVE "WRITE-TEST-GF-01" TO PAR-NAME.                         SQ2214.2
042700     MOVE "FILE CREATED, RECS =" TO COMPUTED-A.                   SQ2214.2
042800     MOVE COUNT-OF-RECS TO CORRECT-18V0.                          SQ2214.2
042900     MOVE "FILE HAS 120 AND 151 CHAR RECS" TO RE-MARK.            SQ2214.2
043000     PERFORM PRINT-DETAIL.                                        SQ2214.2
043100*        A SEQUENTIAL MASS STORAGE FILE CONTAINING 450            SQ2214.2
043200*    RECORDS HAS BEEN CREATED.  THE FILE CONTAINS RECORDS         SQ2214.2
043300*    OF 120 CHARACTERS AND RECORDS OF 151 CHARACTERS.  THE        SQ2214.2
043400*    SEQUENCE IN WHICH THE RECORDS WERE WRITTEN IS S-L-10S-       SQ2214.2
043500*    100L-338S.                                                   SQ2214.2
043600 WRITE-CLOSE-GF-01.                                               SQ2214.2
043700     CLOSE SQ-VS7.                                                SQ2214.2
043800     GO TO READ-INIT-F1-01.                                       SQ2214.2
043900 WRITE-SHORT-REC.                                                 SQ2214.2
044000     MOVE "R1-M-G" TO XRECORD-NAME (1).                           SQ2214.2
044100     MOVE 000120 TO XRECORD-LENGTH (1).                           SQ2214.2
044200     ADD 1 TO COUNT-OF-RECS.                                      SQ2214.2
044300     MOVE COUNT-OF-RECS TO XRECORD-NUMBER (1).                    SQ2214.2
044400     MOVE "SHORT" TO LONG-OR-SHORT.                               SQ2214.2
044500     MOVE COUNT-OF-RECS TO SQ-VS7-RECNO.                          SQ2214.2
044600     MOVE FILE-RECORD-INFO-P1-120 (1) TO SQ-VS7R1-FIRST.          SQ2214.2
044700     MOVE 120 TO RECORD-LENGTH.                                   SQ2214.2
044800     WRITE SQ-VS7R1-M-G-120.                                      SQ2214.2
044900 WRITE-LONG-REC.                                                  SQ2214.2
045000     MOVE "R2-M-G" TO XRECORD-NAME (1).                           SQ2214.2
045100     MOVE 000151 TO XRECORD-LENGTH (1).                           SQ2214.2
045200     ADD 1 TO COUNT-OF-RECS.                                      SQ2214.2
045300     MOVE COUNT-OF-RECS TO XRECORD-NUMBER (1).                    SQ2214.2
045400     MOVE "LONG" TO LONG-OR-SHORT.                                SQ2214.2
045500     MOVE COUNT-OF-RECS TO SQ-VS7-RECNO.                          SQ2214.2
045600     MOVE FILE-RECORD-INFO-P1-120 (1) TO SQ-VS7R2-FIRST.          SQ2214.2
045700     MOVE 151 TO RECORD-LENGTH.                                   SQ2214.2
045800     WRITE SQ-VS7R2-M-G-151.                                      SQ2214.2
045900 READ-INIT-F1-01.                                                 SQ2214.2
046000     MOVE ZERO TO RECORD-LENGTH.                                  SQ2214.2
046100     MOVE ZERO TO COUNT-OF-RECS.                                  SQ2214.2
046200     MOVE ZERO TO EOF-FLAG.                                       SQ2214.2
046300     MOVE ZERO TO RECORDS-IN-ERROR.                               SQ2214.2
046400     MOVE ZERO TO ERROR-FLAG.                                     SQ2214.2
046500     OPEN INPUT SQ-VS7.                                           SQ2214.2
046600 READ-TEST-F1-01.                                                 SQ2214.2
046700     PERFORM READ-SHORT-REC THRU READ-SHORT-REC-EXIT.             SQ2214.2
046800     IF EOF-FLAG EQUAL TO 1                                       SQ2214.2
046900         MOVE "EOF ON FIRST READ" TO RE-MARK                      SQ2214.2
047000         GO TO READ-EOF-F1-06.                                    SQ2214.2
047100     IF ERROR-FLAG EQUAL TO 1                                     SQ2214.2
047200         GO TO READ-FAIL-F1-01.                                   SQ2214.2
047300 READ-PASS-F1-01.                                                 SQ2214.2
047400     PERFORM PASS.                                                SQ2214.2
047500     GO TO READ-WRITE-F1-01.                                      SQ2214.2
047600 READ-FAIL-F1-01.                                                 SQ2214.2
047700     MOVE " FILE NOT OK. SEE PROGRAM & VII-52 OR -44" TO RE-MARK. SQ2214.2
047800     PERFORM FAIL.                                                SQ2214.2
047900 READ-WRITE-F1-01.                                                SQ2214.2
048000     MOVE "READ SHORT RECORD" TO FEATURE.                         SQ2214.2
048100     MOVE "READ-TEST-F1-01" TO PAR-NAME.                          SQ2214.2
048200     MOVE "EXPECTED RECORD LENGTH: 120" TO RE-MARK.               SQ2214.2
048300     MOVE RECORD-LENGTH TO COMPUTED-N.                            SQ2214.2
048400     MOVE 120 TO CORRECT-N.                                       SQ2214.2
048500     PERFORM PRINT-DETAIL.                                        SQ2214.2
048600     GO TO READ-INIT-F1-02.                                       SQ2214.2
048700 READ-SHORT-REC.                                                  SQ2214.2
048800*   READ  <FILE>     NEXT RECORD     AT END                      *SQ2214.2
048900******************************************************************SQ2214.2
049000     IF EOF-FLAG EQUAL TO 1                                       SQ2214.2
049100         GO TO READ-SHORT-REC-EXIT.                               SQ2214.2
049200     READ SQ-VS7 NEXT RECORD AT END                               SQ2214.2
049300         MOVE 1 TO EOF-FLAG                                       SQ2214.2
049400         GO TO READ-SHORT-REC-EXIT.                               SQ2214.2
049500     ADD 1 TO COUNT-OF-RECS.                                      SQ2214.2
049600     MOVE SQ-VS7R1-FIRST TO FILE-RECORD-INFO-P1-120 (1).          SQ2214.2
049700     IF XRECORD-NAME (1) NOT EQUAL TO "R1-M-G"                    SQ2214.2
049800         GO TO READ-SHORT-REC-ERROR.                              SQ2214.2
049900     IF  RECORD-LENGTH     NOT EQUAL TO 120                       SQ2214.2
050000         GO TO READ-SHORT-REC-ERROR.                              SQ2214.2
050100     IF COUNT-OF-RECS NOT EQUAL TO XRECORD-NUMBER (1)             SQ2214.2
050200         GO TO READ-SHORT-REC-ERROR.                              SQ2214.2
050300     IF XLABEL-TYPE (1) EQUAL TO "S"                              SQ2214.2
050400         GO TO READ-SHORT-REC-EXIT.                               SQ2214.2
050500 READ-SHORT-REC-ERROR.                                            SQ2214.2
050600     ADD 1 TO RECORDS-IN-ERROR.                                   SQ2214.2
050700     MOVE 1 TO ERROR-FLAG.                                        SQ2214.2
050800 READ-SHORT-REC-EXIT.                                             SQ2214.2
050900     EXIT.                                                        SQ2214.2
051000 READ-INIT-F1-02.                                                 SQ2214.2
051100     MOVE ZERO TO ERROR-FLAG.                                     SQ2214.2
051200 READ-TEST-F1-02.                                                 SQ2214.2
051300     PERFORM READ-LONG-REC THRU READ-LONG-REC-EXIT.               SQ2214.2
051400     IF EOF-FLAG EQUAL TO 1                                       SQ2214.2
051500         MOVE "EOF ON SECOND READ" TO RE-MARK                     SQ2214.2
051600         GO TO READ-EOF-F1-06.                                    SQ2214.2
051700     IF ERROR-FLAG EQUAL TO 1                                     SQ2214.2
051800         GO TO READ-FAIL-F1-02.                                   SQ2214.2
051900 READ-PASS-F1-02.                                                 SQ2214.2
052000     PERFORM PASS.                                                SQ2214.2
052100     GO TO READ-WRITE-F1-02.                                      SQ2214.2
052200 READ-FAIL-F1-02.                                                 SQ2214.2
052300     MOVE "SEE VII-52 WRITE LONG RECORD OR VII-44 READ" TO RE-MARKSQ2214.2
052400     MOVE RECORD-LENGTH TO COMPUTED-N.                            SQ2214.2
052500     MOVE 151 TO CORRECT-N.                                       SQ2214.2
052600     PERFORM FAIL.                                                SQ2214.2
052700 READ-WRITE-F1-02.                                                SQ2214.2
052800     MOVE "READ LONG RECORD" TO FEATURE.                          SQ2214.2
052900     MOVE "READ-TEST-F1-02" TO PAR-NAME.                          SQ2214.2
053000     MOVE "EXPECTED RECORD LENGTH: 151" TO RE-MARK.               SQ2214.2
053100     PERFORM PRINT-DETAIL.                                        SQ2214.2
053200     GO TO READ-INIT-F1-03.                                       SQ2214.2
053300 READ-LONG-REC.                                                   SQ2214.2
053400     IF EOF-FLAG EQUAL TO 1                                       SQ2214.2
053500         GO TO READ-LONG-REC-EXIT.                                SQ2214.2
053600     READ SQ-VS7 END                                              SQ2214.2
053700         MOVE 1 TO EOF-FLAG                                       SQ2214.2
053800         GO TO READ-LONG-REC-EXIT.                                SQ2214.2
053900     ADD 1 TO COUNT-OF-RECS.                                      SQ2214.2
054000     MOVE SQ-VS7R2-FIRST TO FILE-RECORD-INFO-P1-120 (1).          SQ2214.2
054100     IF XRECORD-NAME (1) NOT EQUAL TO "R2-M-G"                    SQ2214.2
054200         GO TO READ-LONG-REC-ERROR.                               SQ2214.2
054300     IF  RECORD-LENGTH     NOT EQUAL TO 151                       SQ2214.2
054400         GO TO READ-LONG-REC-ERROR.                               SQ2214.2
054500     MOVE COUNT-OF-RECS TO SAVE-COUNT-OF-RECS.                    SQ2214.2
054600     IF SAVE-COUNT-OF-RECS NOT EQUAL TO SQ-VS7-RECNO              SQ2214.2
054700         GO TO READ-LONG-REC-ERROR.                               SQ2214.2
054800     IF LONG-OR-SHORT EQUAL TO "LONG "                            SQ2214.2
054900         GO TO READ-LONG-REC-EXIT.                                SQ2214.2
055000 READ-LONG-REC-ERROR.                                             SQ2214.2
055100     ADD 1 TO RECORDS-IN-ERROR.                                   SQ2214.2
055200     MOVE 1 TO ERROR-FLAG.                                        SQ2214.2
055300 READ-LONG-REC-EXIT.                                              SQ2214.2
055400     EXIT.                                                        SQ2214.2
055500 READ-INIT-F1-03.                                                 SQ2214.2
055600     MOVE ZERO TO ERROR-FLAG.                                     SQ2214.2
055700 READ-TEST-F1-03.                                                 SQ2214.2
055800     PERFORM READ-SHORT-REC THRU READ-SHORT-REC-EXIT 10 TIMES.    SQ2214.2
055900     IF EOF-FLAG EQUAL TO 1                                       SQ2214.2
056000         MOVE "UNEXPECTED EOF" TO RE-MARK                         SQ2214.2
056100         GO TO READ-EOF-F1-06.                                    SQ2214.2
056200     IF ERROR-FLAG EQUAL TO 1                                     SQ2214.2
056300         GO TO READ-FAIL-F1-03.                                   SQ2214.2
056400 READ-PASS-F1-03.                                                 SQ2214.2
056500     PERFORM PASS.                                                SQ2214.2
056600     GO TO READ-WRITE-F1-03.                                      SQ2214.2
056700 READ-FAIL-F1-03.                                                 SQ2214.2
056800     MOVE "SEE VII-52 WRITE SHORT REC   OR VII-44 READ" TO RE-MARKSQ2214.2
056900     MOVE RECORD-LENGTH TO COMPUTED-N.                            SQ2214.2
057000     MOVE 120 TO CORRECT-N.                                       SQ2214.2
057100     PERFORM FAIL.                                                SQ2214.2
057200 READ-WRITE-F1-03.                                                SQ2214.2
057300     MOVE "READ SHORT RECORDS" TO FEATURE.                        SQ2214.2
057400     MOVE "READ-TEST-F1-03" TO PAR-NAME.                          SQ2214.2
057500     MOVE "EXPECTED RECORD LENGTH: 120" TO RE-MARK.               SQ2214.2
057600     PERFORM PRINT-DETAIL.                                        SQ2214.2
057700 READ-INIT-F1-04.                                                 SQ2214.2
057800     MOVE ZERO TO ERROR-FLAG.                                     SQ2214.2
057900 READ-TEST-F1-04.                                                 SQ2214.2
058000     PERFORM READ-LONG-REC THRU READ-LONG-REC-EXIT 100 TIMES.     SQ2214.2
058100     IF EOF-FLAG EQUAL TO 1                                       SQ2214.2
058200         MOVE "UNEXPECTED EOF" TO RE-MARK                         SQ2214.2
058300         GO TO READ-EOF-F1-06.                                    SQ2214.2
058400     IF ERROR-FLAG EQUAL TO 1                                     SQ2214.2
058500         GO TO READ-FAIL-F1-04.                                   SQ2214.2
058600 READ-PASS-F1-04.                                                 SQ2214.2
058700     PERFORM PASS.                                                SQ2214.2
058800     GO TO READ-WRITE-F1-04.                                      SQ2214.2
058900 READ-FAIL-F1-04.                                                 SQ2214.2
059000     MOVE RECORD-LENGTH TO COMPUTED-N.                            SQ2214.2
059100     MOVE 151 TO CORRECT-N.                                       SQ2214.2
059200     PERFORM FAIL.                                                SQ2214.2
059300 READ-WRITE-F1-04.                                                SQ2214.2
059400     MOVE "READ LONG RECORDS" TO FEATURE.                         SQ2214.2
059500     MOVE "READ-TEST-F1-04" TO PAR-NAME.                          SQ2214.2
059600     MOVE "EXPECTED RECORD LENGTH: 151" TO RE-MARK.               SQ2214.2
059700     PERFORM PRINT-DETAIL.                                        SQ2214.2
059800 READ-INIT-F1-05.                                                 SQ2214.2
059900     MOVE ZERO TO ERROR-FLAG.                                     SQ2214.2
060000 READ-TEST-F1-05.                                                 SQ2214.2
060100     PERFORM READ-SHORT-REC THRU READ-SHORT-REC-EXIT 338 TIMES.   SQ2214.2
060200     IF EOF-FLAG EQUAL TO 1                                       SQ2214.2
060300         MOVE "UNEXPECTED EOF" TO RE-MARK                         SQ2214.2
060400         GO TO READ-EOF-F1-06.                                    SQ2214.2
060500     IF ERROR-FLAG EQUAL TO 1                                     SQ2214.2
060600         GO TO READ-FAIL-F1-05.                                   SQ2214.2
060700 READ-PASS-F1-05.                                                 SQ2214.2
060800     PERFORM PASS.                                                SQ2214.2
060900     GO TO READ-WRITE-F1-05.                                      SQ2214.2
061000 READ-FAIL-F1-05.                                                 SQ2214.2
061100     MOVE "SEE VII-52 WRITE LONG RECORD OR VII-44 READ" TO RE-MARKSQ2214.2
061200     MOVE RECORD-LENGTH TO COMPUTED-N.                            SQ2214.2
061300     MOVE 120 TO CORRECT-N.                                       SQ2214.2
061400     PERFORM FAIL.                                                SQ2214.2
061500 READ-WRITE-F1-05.                                                SQ2214.2
061600     MOVE "READ SHORT RECORDS" TO FEATURE.                        SQ2214.2
061700     MOVE "READ-TEST-F1-05" TO PAR-NAME.                          SQ2214.2
061800     MOVE "EXPECTED RECORD LENGTH: 120" TO RE-MARK.               SQ2214.2
061900     PERFORM PRINT-DETAIL.                                        SQ2214.2
062000 READ-INIT-F1-06.                                                 SQ2214.2
062100     READ SQ-VS7 RECORD END                                       SQ2214.2
062200         GO TO READ-TEST-F1-06.                                   SQ2214.2
062300     MOVE "MORE THAN 450 RECORDS" TO RE-MARK.                     SQ2214.2
062400     GO TO READ-FAIL-F1-06.                                       SQ2214.2
062500 READ-EOF-F1-06.                                                  SQ2214.2
062600     MOVE "RECORDS READ =" TO COMPUTED-A.                         SQ2214.2
062700     MOVE COUNT-OF-RECS TO CORRECT-18V0.                          SQ2214.2
062800     GO TO READ-FAIL-F1-06.                                       SQ2214.2
062900 READ-TEST-F1-06.                                                 SQ2214.2
063000     IF RECORDS-IN-ERROR NOT EQUAL TO ZERO                        SQ2214.2
063100         MOVE "RECORDS IN ERROR =" TO COMPUTED-A                  SQ2214.2
063200     MOVE RECORDS-IN-ERROR TO CORRECT-18V0                        SQ2214.2
063300         GO TO READ-FAIL-F1-06.                                   SQ2214.2
063400 READ-PASS-F1-06.                                                 SQ2214.2
063500     PERFORM PASS.                                                SQ2214.2
063600     GO TO READ-WRITE-F1-06.                                      SQ2214.2
063700 READ-FAIL-F1-06.                                                 SQ2214.2
063800     PERFORM FAIL.                                                SQ2214.2
063900 READ-WRITE-F1-06.                                                SQ2214.2
064000     MOVE "READ-TEST-F1-06" TO PAR-NAME.                          SQ2214.2
064100     MOVE "VERIFY FILE SQ-VS7" TO FEATURE.                        SQ2214.2
064200     PERFORM PRINT-DETAIL.                                        SQ2214.2
064300 READ-CLOSE-F1-06.                                                SQ2214.2
064400     CLOSE SQ-VS7.                                                SQ2214.2
064500 SECT-SQ221A-0002 SECTION.                                        SQ2214.2
064600*        THIS SECTION CHECKS IF THE ENTIRE RECORD AREA IS         SQ2214.2
064700*    WRITTEN ON THE MASS STORAGE DEVICE WHEN A SHORT RECORD       SQ2214.2
064800*    IS WRITTEN.  THE RECORD NUMBER IN CHARACTERS 126 THROUGH     SQ2214.2
064900*    130 IS UNIQUE FOR EACH RECORD.                               SQ2214.2
065000 INFO-INIT-01.                                                    SQ2214.2
065100     OPEN INPUT SQ-VS7.                                           SQ2214.2
065200     MOVE ZERO TO COUNT-OF-RECS.                                  SQ2214.2
065300 INFO-TEST-01.                                                    SQ2214.2
065400     READ SQ-VS7 AT END                                           SQ2214.2
065500         GO TO INFO-END.                                          SQ2214.2
065600     ADD 1 TO COUNT-OF-RECS.                                      SQ2214.2
065700     IF SQ-VS7-RECNO NOT EQUAL TO "00001"                         SQ2214.2
065800         GO TO NO-INFO-01.                                        SQ2214.2
065900     MOVE "MAXIMUM RECORD SIZE WRITTEN" TO RE-MARK.               SQ2214.2
066000     MOVE "RECORD READ =" TO COMPUTED-A.                          SQ2214.2
066100     MOVE 0001 TO CORRECT-18V0.                                   SQ2214.2
066200     GO TO INFO-WRITE-01.                                         SQ2214.2
066300 NO-INFO-01.                                                      SQ2214.2
066400     MOVE "NO DEFINITE CONCLUSION POSSIBLE" TO RE-MARK.           SQ2214.2
066500 INFO-WRITE-01.                                                   SQ2214.2
066600     MOVE "READ SHORT RECORD" TO FEATURE.                         SQ2214.2
066700     MOVE "INFO-TEST-01" TO PAR-NAME.                             SQ2214.2
066800     PERFORM PRINT-DETAIL.                                        SQ2214.2
066900 INFO-INIT-02.                                                    SQ2214.2
067000     READ SQ-VS7 RECORD AT END                                    SQ2214.2
067100         GO TO INFO-END.                                          SQ2214.2
067200     READ SQ-VS7 END                                              SQ2214.2
067300         GO TO INFO-END.                                          SQ2214.2
067400 INFO-TEST-02.                                                    SQ2214.2
067500     READ SQ-VS7 AT END                                           SQ2214.2
067600         GO TO INFO-END.                                          SQ2214.2
067700     IF SQ-VS7-RECNO NOT EQUAL TO "00004"                         SQ2214.2
067800         GO TO NO-INFO-02.                                        SQ2214.2
067900     MOVE "MAXIMUM RECORD SIZE WRITTEN" TO RE-MARK.               SQ2214.2
068000     MOVE "RECORD READ =" TO COMPUTED-A.                          SQ2214.2
068100     MOVE 0004 TO CORRECT-18V0.                                   SQ2214.2
068200     GO TO INFO-WRITE-02.                                         SQ2214.2
068300 NO-INFO-02.                                                      SQ2214.2
068400     MOVE "NO DEFINITE CONCLUSION POSSIBLE" TO RE-MARK.           SQ2214.2
068500 INFO-WRITE-02.                                                   SQ2214.2
068600     MOVE "READ SHORT RECORD" TO FEATURE.                         SQ2214.2
068700     MOVE "INFO-TEST-02" TO PAR-NAME.                             SQ2214.2
068800     PERFORM PRINT-DETAIL.                                        SQ2214.2
068900 INFO-INIT-03.                                                    SQ2214.2
069000     ADD 3 TO COUNT-OF-RECS.                                      SQ2214.2
069100 INFO-INIT-03-1.                                                  SQ2214.2
069200     READ SQ-VS7 RECORD                                           SQ2214.2
069300         END GO TO INFO-END.                                      SQ2214.2
069400     ADD 1 TO COUNT-OF-RECS.                                      SQ2214.2
069500     IF COUNT-OF-RECS EQUAL TO 450                                SQ2214.2
069600         GO TO INFO-TEST-03.                                      SQ2214.2
069700     GO TO INFO-INIT-03-1.                                        SQ2214.2
069800 INFO-TEST-03.                                                    SQ2214.2
069900     IF SQ-VS7-RECNO NOT EQUAL TO "00450"                         SQ2214.2
070000         GO TO NO-INFO-03.                                        SQ2214.2
070100     MOVE "MAXIMUM RECORD SIZE WRITTEN" TO RE-MARK.               SQ2214.2
070200     MOVE "RECORD READ =" TO COMPUTED-A.                          SQ2214.2
070300     MOVE 0450 TO CORRECT-18V0.                                   SQ2214.2
070400     GO TO INFO-WRITE-03.                                         SQ2214.2
070500 NO-INFO-03.                                                      SQ2214.2
070600     MOVE "NO DEFINITE CONCLUSION POSSIBLE" TO RE-MARK.           SQ2214.2
070700 INFO-WRITE-03.                                                   SQ2214.2
070800     MOVE "READ SHORT RECORD" TO FEATURE.                         SQ2214.2
070900     MOVE "INFO-TEST-03" TO PAR-NAME.                             SQ2214.2
071000     PERFORM PRINT-DETAIL.                                        SQ2214.2
071100 INFO-END.                                                        SQ2214.2
071200     CLOSE SQ-VS7.                                                SQ2214.2
071300 TERMINATE-ROUTINE.                                               SQ2214.2
071400     EXIT.                                                        SQ2214.2
071500 CCVS-EXIT SECTION.                                               SQ2214.2
071600 CCVS-999999.                                                     SQ2214.2
071700     GO TO CLOSE-FILES.                                           SQ2214.2
      *END-OF,SQ221A                                                                  
