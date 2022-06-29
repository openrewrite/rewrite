      *HEADER,COBOL,SQ223A                                                            
000100 IDENTIFICATION DIVISION.                                         SQ2234.2
000200 PROGRAM-ID.                                                      SQ2234.2
000300     SQ223A.                                                      SQ2234.2
000400****************************************************************  SQ2234.2
000500*                                                              *  SQ2234.2
000600*    VALIDATION FOR:-                                          *  SQ2234.2
000700*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ2234.2
000800*                                                              *  SQ2234.2
000900*    CREATION DATE     /     VALIDATION DATE                   *  SQ2234.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ2234.2
001100*                                                              *  SQ2234.2
001200*        THIS ROUTINE CHECKS:                                     SQ2234.2
001300*                                                                 SQ2234.2
001400*           RECORD IS VARYING IN SIZE FROM 120 TO 151 CHARACTERS. SQ2234.2
001500*                                                                 SQ2234.2
001600*        THIS ROUTINE BUILDS A SEQUENTIAL MASS STORAGE FILE       SQ2234.2
001700*    WHICH CONTAINS BOTH 120 CHARACTER AND 151 CHARACTER          SQ2234.2
001800*    RECORDS.  THE MASS STORAGE FILE CONSISTS OF 1 SHORT,         SQ2234.2
001900*    1 LONG, 10 SHORT, 100 LONG, AND 338 SHORT RECORDS FOR        SQ2234.2
002000*    A TOTAL OF 450 RECORDS IN THE FILE.  THE MASS STORAGE        SQ2234.2
002100*    FILE IS READ AND FIELDS IN THE RECORDS ARE CHECKED           SQ2234.2
002200*    AGAINST THE EXPECTED VALUES.                                 SQ2234.2
002300*                                                                 SQ2234.2
002400*        AN INFORMATION SECTION AT THE END OF THE ROUTINE         SQ2234.2
002500*    CHECKS THE FIELD WHICH CONTAINS THE XRECORD-NUMBER.          SQ2234.2
002600*    THIS FIELD IS PART OF A LONG RECORD ONLY.  IF THE            SQ2234.2
002700*    XRECORD-NUMBER IS THERE FOR A SHORT RECORD, IT MEANS         SQ2234.2
002800*    THE MAXIMUM SIZE RECORD IS ALWAYS WRITTEN.                   SQ2234.2
002900 ENVIRONMENT DIVISION.                                            SQ2234.2
003000 CONFIGURATION SECTION.                                           SQ2234.2
003100 SOURCE-COMPUTER.                                                 SQ2234.2
003200     XXXXX082.                                                    SQ2234.2
003300 OBJECT-COMPUTER.                                                 SQ2234.2
003400     XXXXX083.                                                    SQ2234.2
003500 INPUT-OUTPUT SECTION.                                            SQ2234.2
003600 FILE-CONTROL.                                                    SQ2234.2
003700*    SELECT RAW-DATA   ASSIGN TO                                  SQ2234.2
003800*    XXXXX062                                                     SQ2234.2
003900*           ORGANIZATION IS INDEXED                               SQ2234.2
004000*           ACCESS MODE IS RANDOM                                 SQ2234.2
004100*           RECORD KEY IS RAW-DATA-KEY.                           SQ2234.2
004200     SELECT PRINT-FILE ASSIGN TO                                  SQ2234.2
004300     XXXXX055.                                                    SQ2234.2
004400     SELECT SQ-VS7 ASSIGN TO                                      SQ2234.2
004500     XXXXX014                                                     SQ2234.2
004600     ORGANIZATION SEQUENTIAL                                      SQ2234.2
004700     ACCESS SEQUENTIAL.                                           SQ2234.2
004800 DATA DIVISION.                                                   SQ2234.2
004900 FILE SECTION.                                                    SQ2234.2
005000*                                                                 SQ2234.2
005100*FD  RAW-DATA.                                                    SQ2234.2
005200*                                                                 SQ2234.2
005300*01  RAW-DATA-SATZ.                                               SQ2234.2
005400*    05  RAW-DATA-KEY        PIC X(6).                            SQ2234.2
005500*    05  C-DATE              PIC 9(6).                            SQ2234.2
005600*    05  C-TIME              PIC 9(8).                            SQ2234.2
005700*    05  C-NO-OF-TESTS       PIC 99.                              SQ2234.2
005800*    05  C-OK                PIC 999.                             SQ2234.2
005900*    05  C-ALL               PIC 999.                             SQ2234.2
006000*    05  C-FAIL              PIC 999.                             SQ2234.2
006100*    05  C-DELETED           PIC 999.                             SQ2234.2
006200*    05  C-INSPECT           PIC 999.                             SQ2234.2
006300*    05  C-NOTE              PIC X(13).                           SQ2234.2
006400*    05  C-INDENT            PIC X.                               SQ2234.2
006500*    05  C-ABORT             PIC X(8).                            SQ2234.2
006600 FD  PRINT-FILE                                                   SQ2234.2
006700C    LABEL RECORDS                                                SQ2234.2
006800C    XXXXX084                                                     SQ2234.2
006900C    DATA RECORD IS PRINT-REC DUMMY-RECORD                        SQ2234.2
007000               .                                                  SQ2234.2
007100 01  PRINT-REC PICTURE X(120).                                    SQ2234.2
007200 01  DUMMY-RECORD PICTURE X(120).                                 SQ2234.2
007300 FD  SQ-VS7                                                       SQ2234.2
007400C    LABEL RECORDS ARE STANDARD                                   SQ2234.2
007500     RECORD IS VARYING IN SIZE FROM 120 TO 151 CHARACTERS.        SQ2234.2
007600 01  SQ-VS7R1-M-G-120.                                            SQ2234.2
007700     02  SQ-VS7R1-FIRST PICTURE X(120).                           SQ2234.2
007800 01  SQ-VS7R2-M-G-151.                                            SQ2234.2
007900     02  SQ-VS7R2-FIRST PICTURE X(120).                           SQ2234.2
008000     02  LONG-OR-SHORT  PICTURE X(5).                             SQ2234.2
008100     02  SQ-VS7-RECNO  PICTURE X(5).                              SQ2234.2
008200 02  SQ-VS7-FILLER  PICTURE X(21).                                SQ2234.2
008300 WORKING-STORAGE SECTION.                                         SQ2234.2
008400 01  SAVE-COUNT-OF-RECS  PICTURE X(5) VALUE SPACE.                SQ2234.2
008500 01  COUNT-OF-RECS  PICTURE S9(5) COMPUTATIONAL.                  SQ2234.2
008600 01  RECORDS-IN-ERROR  PICTURE S9(5) COMPUTATIONAL.               SQ2234.2
008700 01  ERROR-FLAG PICTURE 9.                                        SQ2234.2
008800 01  EOF-FLAG  PICTURE 9.                                         SQ2234.2
008900 01  DUMP-AREA.                                                   SQ2234.2
009000     02  TYPE-OF-REC PICTURE X(5).                                SQ2234.2
009100     02  RECNO  PICTURE 9(5).                                     SQ2234.2
009200     02  REC-FILLER PICTURE X(21).                                SQ2234.2
009300 01  FILE-RECORD-INFORMATION-REC.                                 SQ2234.2
009400     03 FILE-RECORD-INFO-SKELETON.                                SQ2234.2
009500        05 FILLER                 PICTURE X(48)       VALUE       SQ2234.2
009600             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  SQ2234.2
009700        05 FILLER                 PICTURE X(46)       VALUE       SQ2234.2
009800             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    SQ2234.2
009900        05 FILLER                 PICTURE X(26)       VALUE       SQ2234.2
010000             ",LFIL=000000,ORG=  ,LBLR= ".                        SQ2234.2
010100        05 FILLER                 PICTURE X(37)       VALUE       SQ2234.2
010200             ",RECKEY=                             ".             SQ2234.2
010300        05 FILLER                 PICTURE X(38)       VALUE       SQ2234.2
010400             ",ALTKEY1=                             ".            SQ2234.2
010500        05 FILLER                 PICTURE X(38)       VALUE       SQ2234.2
010600             ",ALTKEY2=                             ".            SQ2234.2
010700        05 FILLER                 PICTURE X(7)        VALUE SPACE.SQ2234.2
010800     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              SQ2234.2
010900        05 FILE-RECORD-INFO-P1-120.                               SQ2234.2
011000           07 FILLER              PIC X(5).                       SQ2234.2
011100           07 XFILE-NAME           PIC X(6).                      SQ2234.2
011200           07 FILLER              PIC X(8).                       SQ2234.2
011300           07 XRECORD-NAME         PIC X(6).                      SQ2234.2
011400           07 FILLER              PIC X(1).                       SQ2234.2
011500           07 REELUNIT-NUMBER     PIC 9(1).                       SQ2234.2
011600           07 FILLER              PIC X(7).                       SQ2234.2
011700           07 XRECORD-NUMBER       PIC 9(6).                      SQ2234.2
011800           07 FILLER              PIC X(6).                       SQ2234.2
011900           07 UPDATE-NUMBER       PIC 9(2).                       SQ2234.2
012000           07 FILLER              PIC X(5).                       SQ2234.2
012100           07 ODO-NUMBER          PIC 9(4).                       SQ2234.2
012200           07 FILLER              PIC X(5).                       SQ2234.2
012300           07 XPROGRAM-NAME        PIC X(5).                      SQ2234.2
012400           07 FILLER              PIC X(7).                       SQ2234.2
012500           07 XRECORD-LENGTH       PIC 9(6).                      SQ2234.2
012600           07 FILLER              PIC X(7).                       SQ2234.2
012700           07 CHARS-OR-RECORDS    PIC X(2).                       SQ2234.2
012800           07 FILLER              PIC X(1).                       SQ2234.2
012900           07 XBLOCK-SIZE          PIC 9(4).                      SQ2234.2
013000           07 FILLER              PIC X(6).                       SQ2234.2
013100           07 RECORDS-IN-FILE     PIC 9(6).                       SQ2234.2
013200           07 FILLER              PIC X(5).                       SQ2234.2
013300           07 XFILE-ORGANIZATION   PIC X(2).                      SQ2234.2
013400           07 FILLER              PIC X(6).                       SQ2234.2
013500           07 XLABEL-TYPE          PIC X(1).                      SQ2234.2
013600        05 FILE-RECORD-INFO-P121-240.                             SQ2234.2
013700           07 FILLER              PIC X(8).                       SQ2234.2
013800           07 XRECORD-KEY          PIC X(29).                     SQ2234.2
013900           07 FILLER              PIC X(9).                       SQ2234.2
014000           07 ALTERNATE-KEY1      PIC X(29).                      SQ2234.2
014100           07 FILLER              PIC X(9).                       SQ2234.2
014200           07 ALTERNATE-KEY2      PIC X(29).                      SQ2234.2
014300           07 FILLER              PIC X(7).                       SQ2234.2
014400 01  TEST-RESULTS.                                                SQ2234.2
014500     02 FILLER                    PICTURE X VALUE SPACE.          SQ2234.2
014600     02 FEATURE                   PICTURE X(20) VALUE SPACE.      SQ2234.2
014700     02 FILLER                    PICTURE X VALUE SPACE.          SQ2234.2
014800     02 P-OR-F                    PICTURE X(5) VALUE SPACE.       SQ2234.2
014900     02 FILLER                    PICTURE X  VALUE SPACE.         SQ2234.2
015000     02  PAR-NAME.                                                SQ2234.2
015100       03 FILLER PICTURE X(12) VALUE SPACE.                       SQ2234.2
015200       03  PARDOT-X PICTURE X  VALUE SPACE.                       SQ2234.2
015300       03 DOTVALUE PICTURE 99  VALUE ZERO.                        SQ2234.2
015400       03 FILLER PIC X(5) VALUE SPACE.                            SQ2234.2
015500     02 FILLER PIC X(10) VALUE SPACE.                             SQ2234.2
015600     02 RE-MARK PIC X(61).                                        SQ2234.2
015700 01  TEST-COMPUTED.                                               SQ2234.2
015800     02 FILLER PIC X(30) VALUE SPACE.                             SQ2234.2
015900     02 FILLER PIC X(17) VALUE "       COMPUTED=".                SQ2234.2
016000     02 COMPUTED-X.                                               SQ2234.2
016100     03 COMPUTED-A                PICTURE X(20) VALUE SPACE.      SQ2234.2
016200     03 COMPUTED-N REDEFINES COMPUTED-A PICTURE -9(9).9(9).       SQ2234.2
016300     03 COMPUTED-0V18 REDEFINES COMPUTED-A  PICTURE -.9(18).      SQ2234.2
016400     03 COMPUTED-4V14 REDEFINES COMPUTED-A  PICTURE -9(4).9(14).  SQ2234.2
016500     03 COMPUTED-14V4 REDEFINES COMPUTED-A  PICTURE -9(14).9(4).  SQ2234.2
016600     03       CM-18V0 REDEFINES COMPUTED-A.                       SQ2234.2
016700         04 COMPUTED-18V0                   PICTURE -9(18).       SQ2234.2
016800         04 FILLER                          PICTURE X.            SQ2234.2
016900     03 FILLER PIC X(50) VALUE SPACE.                             SQ2234.2
017000 01  TEST-CORRECT.                                                SQ2234.2
017100     02 FILLER PIC X(30) VALUE SPACE.                             SQ2234.2
017200     02 FILLER PIC X(17) VALUE "       CORRECT =".                SQ2234.2
017300     02 CORRECT-X.                                                SQ2234.2
017400     03 CORRECT-A                 PICTURE X(20) VALUE SPACE.      SQ2234.2
017500     03 CORRECT-N REDEFINES CORRECT-A PICTURE -9(9).9(9).         SQ2234.2
017600     03 CORRECT-0V18 REDEFINES CORRECT-A    PICTURE -.9(18).      SQ2234.2
017700     03 CORRECT-4V14 REDEFINES CORRECT-A    PICTURE -9(4).9(14).  SQ2234.2
017800     03 CORRECT-14V4 REDEFINES CORRECT-A    PICTURE -9(14).9(4).  SQ2234.2
017900     03      CR-18V0 REDEFINES CORRECT-A.                         SQ2234.2
018000         04 CORRECT-18V0                    PICTURE -9(18).       SQ2234.2
018100         04 FILLER                          PICTURE X.            SQ2234.2
018200     03 FILLER PIC X(50) VALUE SPACE.                             SQ2234.2
018300 01  CCVS-C-1.                                                    SQ2234.2
018400     02 FILLER PICTURE IS X(99) VALUE IS " FEATURE              PASQ2234.2
018500-    "SS  PARAGRAPH-NAME                                          SQ2234.2
018600-    "        REMARKS".                                           SQ2234.2
018700     02 FILLER PICTURE IS X(20) VALUE IS SPACE.                   SQ2234.2
018800 01  CCVS-C-2.                                                    SQ2234.2
018900     02 FILLER PICTURE IS X VALUE IS SPACE.                       SQ2234.2
019000     02 FILLER PICTURE IS X(6) VALUE IS "TESTED".                 SQ2234.2
019100     02 FILLER PICTURE IS X(15) VALUE IS SPACE.                   SQ2234.2
019200     02 FILLER PICTURE IS X(4) VALUE IS "FAIL".                   SQ2234.2
019300     02 FILLER PICTURE IS X(94) VALUE IS SPACE.                   SQ2234.2
019400 01  REC-SKL-SUB PICTURE 9(2) VALUE ZERO.                         SQ2234.2
019500 01  REC-CT PICTURE 99 VALUE ZERO.                                SQ2234.2
019600 01  DELETE-CNT                   PICTURE 999  VALUE ZERO.        SQ2234.2
019700 01  ERROR-COUNTER PICTURE IS 999 VALUE IS ZERO.                  SQ2234.2
019800 01  INSPECT-COUNTER PIC 999 VALUE ZERO.                          SQ2234.2
019900 01  PASS-COUNTER PIC 999 VALUE ZERO.                             SQ2234.2
020000 01  TOTAL-ERROR PIC 999 VALUE ZERO.                              SQ2234.2
020100 01  ERROR-HOLD PIC 999 VALUE ZERO.                               SQ2234.2
020200 01  DUMMY-HOLD PIC X(120) VALUE SPACE.                           SQ2234.2
020300 01  RECORD-COUNT PIC 9(5) VALUE ZERO.                            SQ2234.2
020400 01  CCVS-H-1.                                                    SQ2234.2
020500     02  FILLER   PICTURE X(27)  VALUE SPACE.                     SQ2234.2
020600     02 FILLER PICTURE X(67) VALUE                                SQ2234.2
020700     " FEDERAL SOFTWARE TESTING CENTER COBOL COMPILER VALIDATION  SQ2234.2
020800-    " SYSTEM".                                                   SQ2234.2
020900     02  FILLER     PICTURE X(26)  VALUE SPACE.                   SQ2234.2
021000 01  CCVS-H-2.                                                    SQ2234.2
021100     02 FILLER PICTURE X(52) VALUE IS                             SQ2234.2
021200     "CCVS85 FSTC COPY, NOT FOR DISTRIBUTION.".                   SQ2234.2
021300     02 FILLER PICTURE IS X(19) VALUE IS "TEST RESULTS SET-  ".   SQ2234.2
021400     02 TEST-ID PICTURE IS X(9).                                  SQ2234.2
021500     02 FILLER PICTURE IS X(40) VALUE IS SPACE.                   SQ2234.2
021600 01  CCVS-H-3.                                                    SQ2234.2
021700     02  FILLER PICTURE X(34) VALUE                               SQ2234.2
021800     " FOR OFFICIAL USE ONLY    ".                                SQ2234.2
021900     02  FILLER PICTURE X(58) VALUE                               SQ2234.2
022000     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ2234.2
022100     02  FILLER PICTURE X(28) VALUE                               SQ2234.2
022200     "  COPYRIGHT   1985 ".                                       SQ2234.2
022300 01  CCVS-E-1.                                                    SQ2234.2
022400     02 FILLER PICTURE IS X(52) VALUE IS SPACE.                   SQ2234.2
022500     02 FILLER PICTURE IS X(14) VALUE IS "END OF TEST-  ".        SQ2234.2
022600     02 ID-AGAIN PICTURE IS X(9).                                 SQ2234.2
022700     02 FILLER PICTURE X(45) VALUE IS                             SQ2234.2
022800     " NTIS DISTRIBUTION COBOL 85".                               SQ2234.2
022900 01  CCVS-E-2.                                                    SQ2234.2
023000     02  FILLER                   PICTURE X(31)  VALUE            SQ2234.2
023100     SPACE.                                                       SQ2234.2
023200     02  FILLER                   PICTURE X(21)  VALUE SPACE.     SQ2234.2
023300     02 CCVS-E-2-2.                                               SQ2234.2
023400         03 ERROR-TOTAL PICTURE IS XXX VALUE IS SPACE.            SQ2234.2
023500         03 FILLER PICTURE IS X VALUE IS SPACE.                   SQ2234.2
023600         03 ENDER-DESC PIC X(46) VALUE "ERRORS ENCOUNTERED".      SQ2234.2
023700 01  CCVS-E-3.                                                    SQ2234.2
023800     02  FILLER PICTURE X(22) VALUE                               SQ2234.2
023900     " FOR OFFICIAL USE ONLY".                                    SQ2234.2
024000     02  FILLER PICTURE X(12) VALUE SPACE.                        SQ2234.2
024100     02  FILLER PICTURE X(58) VALUE                               SQ2234.2
024200     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ2234.2
024300     02  FILLER PICTURE X(13) VALUE SPACE.                        SQ2234.2
024400     02 FILLER PIC X(15) VALUE " COPYRIGHT 1985".                 SQ2234.2
024500 01  CCVS-E-4.                                                    SQ2234.2
024600     02 CCVS-E-4-1 PIC XXX VALUE SPACE.                           SQ2234.2
024700     02 FILLER PIC XXXX VALUE " OF ".                             SQ2234.2
024800     02 CCVS-E-4-2 PIC XXX VALUE SPACE.                           SQ2234.2
024900     02 FILLER PIC X(40) VALUE                                    SQ2234.2
025000      "  TESTS WERE EXECUTED SUCCESSFULLY".                       SQ2234.2
025100 01  XXINFO.                                                      SQ2234.2
025200     02 FILLER PIC X(30) VALUE "        *** INFORMATION  ***".    SQ2234.2
025300     02 INFO-TEXT.                                                SQ2234.2
025400     04 FILLER PIC X(20) VALUE SPACE.                             SQ2234.2
025500     04 XXCOMPUTED PIC X(20).                                     SQ2234.2
025600     04 FILLER PIC X(5) VALUE SPACE.                              SQ2234.2
025700     04 XXCORRECT PIC X(20).                                      SQ2234.2
025800 01  HYPHEN-LINE.                                                 SQ2234.2
025900     02 FILLER PICTURE IS X VALUE IS SPACE.                       SQ2234.2
026000     02 FILLER PICTURE IS X(65) VALUE IS "************************SQ2234.2
026100-    "*****************************************".                 SQ2234.2
026200     02 FILLER PICTURE IS X(54) VALUE IS "************************SQ2234.2
026300-    "******************************".                            SQ2234.2
026400 01  CCVS-PGM-ID PIC X(6) VALUE                                   SQ2234.2
026500     "SQ223A".                                                    SQ2234.2
026600 PROCEDURE DIVISION.                                              SQ2234.2
026700 CCVS1 SECTION.                                                   SQ2234.2
026800 OPEN-FILES.                                                      SQ2234.2
026900*    OPEN I-O RAW-DATA.                                           SQ2234.2
027000*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            SQ2234.2
027100*    READ RAW-DATA INVALID KEY GO TO END-E-1.                     SQ2234.2
027200*    MOVE "ABORTED " TO C-ABORT.                                  SQ2234.2
027300*    ADD 1 TO C-NO-OF-TESTS.                                      SQ2234.2
027400*    ACCEPT C-DATE  FROM DATE.                                    SQ2234.2
027500*    ACCEPT C-TIME  FROM TIME.                                    SQ2234.2
027600*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-1.             SQ2234.2
027700*END-E-1.                                                         SQ2234.2
027800*    CLOSE RAW-DATA.                                              SQ2234.2
027900     OPEN     OUTPUT PRINT-FILE.                                  SQ2234.2
028000     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   SQ2234.2
028100     MOVE    SPACE TO TEST-RESULTS.                               SQ2234.2
028200     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             SQ2234.2
028300     MOVE ZERO TO REC-SKL-SUB.                                    SQ2234.2
028400     PERFORM CCVS-INIT-FILE 9 TIMES.                              SQ2234.2
028500 CCVS-INIT-FILE.                                                  SQ2234.2
028600     ADD 1 TO REC-SKL-SUB.                                        SQ2234.2
028700     MOVE FILE-RECORD-INFO-SKELETON TO                            SQ2234.2
028800                  FILE-RECORD-INFO (REC-SKL-SUB).                 SQ2234.2
028900 CCVS-INIT-EXIT.                                                  SQ2234.2
029000     GO TO CCVS1-EXIT.                                            SQ2234.2
029100 CLOSE-FILES.                                                     SQ2234.2
029200     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   SQ2234.2
029300*    OPEN I-O RAW-DATA.                                           SQ2234.2
029400*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            SQ2234.2
029500*    READ RAW-DATA INVALID KEY GO TO END-E-2.                     SQ2234.2
029600*    MOVE "OK.     " TO C-ABORT.                                  SQ2234.2
029700*    MOVE PASS-COUNTER TO C-OK.                                   SQ2234.2
029800*    MOVE ERROR-HOLD   TO C-ALL.                                  SQ2234.2
029900*    MOVE ERROR-COUNTER TO C-FAIL.                                SQ2234.2
030000*    MOVE DELETE-CNT TO C-DELETED.                                SQ2234.2
030100*    MOVE INSPECT-COUNTER TO C-INSPECT.                           SQ2234.2
030200*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-2.             SQ2234.2
030300*END-E-2.                                                         SQ2234.2
030400*    CLOSE RAW-DATA.                                              SQ2234.2
030500 TERMINATE-CCVS.                                                  SQ2234.2
030600*    EXIT PROGRAM.                                                SQ2234.2
030700*TERMINATE-CALL.                                                  SQ2234.2
030800     STOP     RUN.                                                SQ2234.2
030900 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         SQ2234.2
031000 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           SQ2234.2
031100 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          SQ2234.2
031200 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-CNT.          SQ2234.2
031300     MOVE "****TEST DELETED****" TO RE-MARK.                      SQ2234.2
031400 PRINT-DETAIL.                                                    SQ2234.2
031500     IF REC-CT NOT EQUAL TO ZERO                                  SQ2234.2
031600             MOVE "." TO PARDOT-X                                 SQ2234.2
031700             MOVE REC-CT TO DOTVALUE.                             SQ2234.2
031800     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      SQ2234.2
031900     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               SQ2234.2
032000        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 SQ2234.2
032100          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 SQ2234.2
032200     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              SQ2234.2
032300     MOVE SPACE TO CORRECT-X.                                     SQ2234.2
032400     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         SQ2234.2
032500     MOVE     SPACE TO RE-MARK.                                   SQ2234.2
032600 HEAD-ROUTINE.                                                    SQ2234.2
032700     MOVE CCVS-H-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ2234.2
032800     MOVE CCVS-H-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.   SQ2234.2
032900     MOVE CCVS-H-3 TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.   SQ2234.2
033000 COLUMN-NAMES-ROUTINE.                                            SQ2234.2
033100     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ2234.2
033200     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ2234.2
033300     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ2234.2
033400 END-ROUTINE.                                                     SQ2234.2
033500     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.SQ2234.2
033600 END-RTN-EXIT.                                                    SQ2234.2
033700     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ2234.2
033800 END-ROUTINE-1.                                                   SQ2234.2
033900      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      SQ2234.2
034000      ERROR-HOLD. ADD DELETE-CNT TO ERROR-HOLD.                   SQ2234.2
034100      ADD PASS-COUNTER TO ERROR-HOLD.                             SQ2234.2
034200*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   SQ2234.2
034300      MOVE PASS-COUNTER TO CCVS-E-4-1.                            SQ2234.2
034400      MOVE ERROR-HOLD TO CCVS-E-4-2.                              SQ2234.2
034500      MOVE CCVS-E-4 TO CCVS-E-2-2.                                SQ2234.2
034600      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           SQ2234.2
034700  END-ROUTINE-12.                                                 SQ2234.2
034800      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        SQ2234.2
034900     IF       ERROR-COUNTER IS EQUAL TO ZERO                      SQ2234.2
035000         MOVE "NO " TO ERROR-TOTAL                                SQ2234.2
035100         ELSE                                                     SQ2234.2
035200         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       SQ2234.2
035300     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           SQ2234.2
035400     PERFORM WRITE-LINE.                                          SQ2234.2
035500 END-ROUTINE-13.                                                  SQ2234.2
035600     IF DELETE-CNT IS EQUAL TO ZERO                               SQ2234.2
035700         MOVE "NO " TO ERROR-TOTAL  ELSE                          SQ2234.2
035800         MOVE DELETE-CNT TO ERROR-TOTAL.                          SQ2234.2
035900     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   SQ2234.2
036000     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ2234.2
036100      IF   INSPECT-COUNTER EQUAL TO ZERO                          SQ2234.2
036200          MOVE "NO " TO ERROR-TOTAL                               SQ2234.2
036300      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   SQ2234.2
036400      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            SQ2234.2
036500      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          SQ2234.2
036600     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ2234.2
036700 WRITE-LINE.                                                      SQ2234.2
036800     ADD 1 TO RECORD-COUNT.                                       SQ2234.2
036900*    IF RECORD-COUNT GREATER 50                                   SQ2234.2
037000*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          SQ2234.2
037100*        MOVE SPACE TO DUMMY-RECORD                               SQ2234.2
037200*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  SQ2234.2
037300*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             SQ2234.2
037400*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     SQ2234.2
037500*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          SQ2234.2
037600*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          SQ2234.2
037700*        MOVE ZERO TO RECORD-COUNT.                               SQ2234.2
037800     PERFORM WRT-LN.                                              SQ2234.2
037900 WRT-LN.                                                          SQ2234.2
038000     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               SQ2234.2
038100     MOVE SPACE TO DUMMY-RECORD.                                  SQ2234.2
038200 BLANK-LINE-PRINT.                                                SQ2234.2
038300     PERFORM WRT-LN.                                              SQ2234.2
038400 FAIL-ROUTINE.                                                    SQ2234.2
038500     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.   SQ2234.2
038600     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.    SQ2234.2
038700     MOVE "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.    SQ2234.2
038800     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     SQ2234.2
038900     GO TO FAIL-ROUTINE-EX.                                       SQ2234.2
039000 FAIL-ROUTINE-WRITE.                                              SQ2234.2
039100     MOVE TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE           SQ2234.2
039200     MOVE TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES.   SQ2234.2
039300 FAIL-ROUTINE-EX. EXIT.                                           SQ2234.2
039400 BAIL-OUT.                                                        SQ2234.2
039500     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.       SQ2234.2
039600     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.               SQ2234.2
039700 BAIL-OUT-WRITE.                                                  SQ2234.2
039800     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  SQ2234.2
039900     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     SQ2234.2
040000 BAIL-OUT-EX. EXIT.                                               SQ2234.2
040100 CCVS1-EXIT.                                                      SQ2234.2
040200     EXIT.                                                        SQ2234.2
040300 SECT-SQ223A-0001 SECTION.                                        SQ2234.2
040400 WRITE-INIT-GF-01.                                                SQ2234.2
040500     MOVE "SQ-VS7" TO XFILE-NAME (1).                             SQ2234.2
040600     MOVE CCVS-PGM-ID TO XPROGRAM-NAME (1).                       SQ2234.2
040700     MOVE "RC" TO CHARS-OR-RECORDS (1).                           SQ2234.2
040800     MOVE 0001 TO XBLOCK-SIZE (1).                                SQ2234.2
040900     MOVE 000450 TO RECORDS-IN-FILE (1).                          SQ2234.2
041000     MOVE "SQ" TO XFILE-ORGANIZATION (1).                         SQ2234.2
041100     MOVE "S" TO XLABEL-TYPE (1).                                 SQ2234.2
041200     MOVE 000000 TO XRECORD-NUMBER (1).                           SQ2234.2
041300     MOVE ZERO TO COUNT-OF-RECS.                                  SQ2234.2
041400     OPEN OUTPUT SQ-VS7.                                          SQ2234.2
041500     MOVE "MULTIPLE LENGTH RECS" TO SQ-VS7-FILLER.                SQ2234.2
041600 WRITE-TEST-GF-01.                                                SQ2234.2
041700     PERFORM WRITE-SHORT-REC.                                     SQ2234.2
041800     PERFORM WRITE-LONG-REC.                                      SQ2234.2
041900     PERFORM WRITE-SHORT-REC 10 TIMES.                            SQ2234.2
042000     PERFORM WRITE-LONG-REC 100 TIMES.                            SQ2234.2
042100     PERFORM WRITE-SHORT-REC 338 TIMES.                           SQ2234.2
042200 WRITE-WRITE-GF-01.                                               SQ2234.2
042300     MOVE "CREATE FILE SQ-VS7" TO FEATURE.                        SQ2234.2
042400     MOVE "WRITE-TEST-GF-01" TO PAR-NAME.                         SQ2234.2
042500     MOVE "FILE CREATED, RECS =" TO COMPUTED-A.                   SQ2234.2
042600     MOVE COUNT-OF-RECS TO CORRECT-18V0.                          SQ2234.2
042700     MOVE "FILE HAS 120 AND 151 CHAR RECS" TO RE-MARK.            SQ2234.2
042800     PERFORM PRINT-DETAIL.                                        SQ2234.2
042900*        A SEQUENTIAL MASS STORAGE FILE CONTAINING 450            SQ2234.2
043000*    RECORDS HAS BEEN CREATED.  THE FILE CONTAINS RECORDS         SQ2234.2
043100*    OF 120 CHARACTERS AND RECORDS OF 151 CHARACTERS.  THE        SQ2234.2
043200*    SEQUENCE IN WHICH THE RECORDS WERE WRITTEN IS S-L-10S-       SQ2234.2
043300*    100L-338S.                                                   SQ2234.2
043400 WRITE-CLOSE-GF-01.                                               SQ2234.2
043500     CLOSE SQ-VS7.                                                SQ2234.2
043600     GO TO READ-INIT-F1-01.                                       SQ2234.2
043700 WRITE-SHORT-REC.                                                 SQ2234.2
043800     MOVE "R1-M-G" TO XRECORD-NAME (1).                           SQ2234.2
043900     MOVE 000120 TO XRECORD-LENGTH (1).                           SQ2234.2
044000     ADD 1 TO COUNT-OF-RECS.                                      SQ2234.2
044100     MOVE COUNT-OF-RECS TO XRECORD-NUMBER (1).                    SQ2234.2
044200     MOVE "SHORT" TO LONG-OR-SHORT.                               SQ2234.2
044300     MOVE COUNT-OF-RECS TO SQ-VS7-RECNO.                          SQ2234.2
044400     MOVE FILE-RECORD-INFO-P1-120 (1) TO SQ-VS7R1-FIRST.          SQ2234.2
044500     WRITE SQ-VS7R1-M-G-120.                                      SQ2234.2
044600 WRITE-LONG-REC.                                                  SQ2234.2
044700     MOVE "R2-M-G" TO XRECORD-NAME (1).                           SQ2234.2
044800     MOVE 000151 TO XRECORD-LENGTH (1).                           SQ2234.2
044900     ADD 1 TO COUNT-OF-RECS.                                      SQ2234.2
045000     MOVE COUNT-OF-RECS TO XRECORD-NUMBER (1).                    SQ2234.2
045100     MOVE "LONG" TO LONG-OR-SHORT.                                SQ2234.2
045200     MOVE COUNT-OF-RECS TO SQ-VS7-RECNO.                          SQ2234.2
045300     MOVE FILE-RECORD-INFO-P1-120 (1) TO SQ-VS7R2-FIRST.          SQ2234.2
045400     WRITE SQ-VS7R2-M-G-151.                                      SQ2234.2
045500 READ-INIT-F1-01.                                                 SQ2234.2
045600     MOVE ZERO TO COUNT-OF-RECS.                                  SQ2234.2
045700     MOVE ZERO TO EOF-FLAG.                                       SQ2234.2
045800     MOVE ZERO TO RECORDS-IN-ERROR.                               SQ2234.2
045900     MOVE ZERO TO ERROR-FLAG.                                     SQ2234.2
046000     OPEN INPUT SQ-VS7.                                           SQ2234.2
046100 READ-TEST-F1-01.                                                 SQ2234.2
046200     PERFORM READ-SHORT-REC THRU READ-SHORT-REC-EXIT.             SQ2234.2
046300     IF EOF-FLAG EQUAL TO 1                                       SQ2234.2
046400         MOVE "EOF ON FIRST READ" TO RE-MARK                      SQ2234.2
046500         GO TO READ-EOF-F1-06.                                    SQ2234.2
046600     IF ERROR-FLAG EQUAL TO 1                                     SQ2234.2
046700         GO TO READ-FAIL-F1-01.                                   SQ2234.2
046800 READ-PASS-F1-01.                                                 SQ2234.2
046900     PERFORM PASS.                                                SQ2234.2
047000     GO TO READ-WRITE-F1-01.                                      SQ2234.2
047100 READ-FAIL-F1-01.                                                 SQ2234.2
047200     MOVE "ERROR:SEE VII-52 WRITE  OR VII-44 READ; VII-30 FORMAT  SQ2234.2
047300-         "RECORD IS VARYING IN SIZE ..."              TO RE-MARK.SQ2234.2
047400     PERFORM FAIL.                                                SQ2234.2
047500 READ-WRITE-F1-01.                                                SQ2234.2
047600     MOVE "READ SHORT RECORD" TO FEATURE.                         SQ2234.2
047700     MOVE "READ-TEST-F1-01" TO PAR-NAME.                          SQ2234.2
047800     MOVE "EXPECTED RECORD LENGTH: 120" TO RE-MARK.               SQ2234.2
047900     MOVE 120 TO CORRECT-N.                                       SQ2234.2
048000     PERFORM PRINT-DETAIL.                                        SQ2234.2
048100     GO TO READ-INIT-F1-02.                                       SQ2234.2
048200 READ-SHORT-REC.                                                  SQ2234.2
048300     IF EOF-FLAG EQUAL TO 1                                       SQ2234.2
048400         GO TO READ-SHORT-REC-EXIT.                               SQ2234.2
048500     READ SQ-VS7 AT END                                           SQ2234.2
048600         MOVE 1 TO EOF-FLAG                                       SQ2234.2
048700         GO TO READ-SHORT-REC-EXIT.                               SQ2234.2
048800     ADD 1 TO COUNT-OF-RECS.                                      SQ2234.2
048900     MOVE SQ-VS7R1-FIRST TO FILE-RECORD-INFO-P1-120 (1).          SQ2234.2
049000     IF XRECORD-NAME (1) NOT EQUAL TO "R1-M-G"                    SQ2234.2
049100         GO TO READ-SHORT-REC-ERROR.                              SQ2234.2
049200     IF XRECORD-LENGTH (1) NOT EQUAL TO 120                       SQ2234.2
049300         GO TO READ-SHORT-REC-ERROR.                              SQ2234.2
049400     IF COUNT-OF-RECS NOT EQUAL TO XRECORD-NUMBER (1)             SQ2234.2
049500         GO TO READ-SHORT-REC-ERROR.                              SQ2234.2
049600     IF XLABEL-TYPE (1) EQUAL TO "S"                              SQ2234.2
049700         GO TO READ-SHORT-REC-EXIT.                               SQ2234.2
049800 READ-SHORT-REC-ERROR.                                            SQ2234.2
049900     ADD 1 TO RECORDS-IN-ERROR.                                   SQ2234.2
050000     MOVE 1 TO ERROR-FLAG.                                        SQ2234.2
050100 READ-SHORT-REC-EXIT.                                             SQ2234.2
050200     EXIT.                                                        SQ2234.2
050300 READ-INIT-F1-02.                                                 SQ2234.2
050400     MOVE ZERO TO ERROR-FLAG.                                     SQ2234.2
050500 READ-TEST-F1-02.                                                 SQ2234.2
050600     PERFORM READ-LONG-REC THRU READ-LONG-REC-EXIT.               SQ2234.2
050700     IF EOF-FLAG EQUAL TO 1                                       SQ2234.2
050800         MOVE "EOF ON SECOND READ" TO RE-MARK                     SQ2234.2
050900         GO TO READ-EOF-F1-06.                                    SQ2234.2
051000     IF ERROR-FLAG EQUAL TO 1                                     SQ2234.2
051100         GO TO READ-FAIL-F1-02.                                   SQ2234.2
051200 READ-PASS-F1-02.                                                 SQ2234.2
051300     PERFORM PASS.                                                SQ2234.2
051400     GO TO READ-WRITE-F1-02.                                      SQ2234.2
051500 READ-FAIL-F1-02.                                                 SQ2234.2
051600     MOVE "ERROR:SEE VII-52 WRITE  OR VII-44 READ; VII-30 FORMAT  SQ2234.2
051700-         "RECORD IS VARYING IN SIZE ..."              TO RE-MARK.SQ2234.2
051800     MOVE 151 TO CORRECT-N.                                       SQ2234.2
051900     PERFORM FAIL.                                                SQ2234.2
052000 READ-WRITE-F1-02.                                                SQ2234.2
052100     MOVE "READ LONG RECORD" TO FEATURE.                          SQ2234.2
052200     MOVE "READ-TEST-F1-02" TO PAR-NAME.                          SQ2234.2
052300     MOVE "EXPECTED RECORD LENGTH: 151" TO RE-MARK.               SQ2234.2
052400     PERFORM PRINT-DETAIL.                                        SQ2234.2
052500     GO TO READ-INIT-F1-03.                                       SQ2234.2
052600 READ-LONG-REC.                                                   SQ2234.2
052700     IF EOF-FLAG EQUAL TO 1                                       SQ2234.2
052800         GO TO READ-LONG-REC-EXIT.                                SQ2234.2
052900     READ SQ-VS7 END                                              SQ2234.2
053000         MOVE 1 TO EOF-FLAG                                       SQ2234.2
053100         GO TO READ-LONG-REC-EXIT.                                SQ2234.2
053200     ADD 1 TO COUNT-OF-RECS.                                      SQ2234.2
053300     MOVE SQ-VS7R2-FIRST TO FILE-RECORD-INFO-P1-120 (1).          SQ2234.2
053400     IF XRECORD-NAME (1) NOT EQUAL TO "R2-M-G"                    SQ2234.2
053500         GO TO READ-LONG-REC-ERROR.                               SQ2234.2
053600     IF XRECORD-LENGTH (1) NOT EQUAL TO 151                       SQ2234.2
053700         GO TO READ-LONG-REC-ERROR.                               SQ2234.2
053800     MOVE COUNT-OF-RECS TO SAVE-COUNT-OF-RECS.                    SQ2234.2
053900     IF SAVE-COUNT-OF-RECS NOT EQUAL TO SQ-VS7-RECNO              SQ2234.2
054000         GO TO READ-LONG-REC-ERROR.                               SQ2234.2
054100     IF LONG-OR-SHORT EQUAL TO "LONG "                            SQ2234.2
054200         GO TO READ-LONG-REC-EXIT.                                SQ2234.2
054300 READ-LONG-REC-ERROR.                                             SQ2234.2
054400     ADD 1 TO RECORDS-IN-ERROR.                                   SQ2234.2
054500     MOVE 1 TO ERROR-FLAG.                                        SQ2234.2
054600 READ-LONG-REC-EXIT.                                              SQ2234.2
054700     EXIT.                                                        SQ2234.2
054800 READ-INIT-F1-03.                                                 SQ2234.2
054900     MOVE ZERO TO ERROR-FLAG.                                     SQ2234.2
055000 READ-TEST-F1-03.                                                 SQ2234.2
055100     PERFORM READ-SHORT-REC THRU READ-SHORT-REC-EXIT 10 TIMES.    SQ2234.2
055200     IF EOF-FLAG EQUAL TO 1                                       SQ2234.2
055300         MOVE "UNEXPECTED EOF" TO RE-MARK                         SQ2234.2
055400         GO TO READ-EOF-F1-06.                                    SQ2234.2
055500     IF ERROR-FLAG EQUAL TO 1                                     SQ2234.2
055600         GO TO READ-FAIL-F1-03.                                   SQ2234.2
055700 READ-PASS-F1-03.                                                 SQ2234.2
055800     PERFORM PASS.                                                SQ2234.2
055900     GO TO READ-WRITE-F1-03.                                      SQ2234.2
056000 READ-FAIL-F1-03.                                                 SQ2234.2
056100     MOVE "ERROR:SEE VII-52 WRITE  OR VII-44 READ; VII-30 FORMAT  SQ2234.2
056200-         "RECORD IS VARYING IN SIZE ..."              TO RE-MARK.SQ2234.2
056300     MOVE 120 TO CORRECT-N.                                       SQ2234.2
056400     PERFORM FAIL.                                                SQ2234.2
056500 READ-WRITE-F1-03.                                                SQ2234.2
056600     MOVE "READ SHORT RECORDS" TO FEATURE.                        SQ2234.2
056700     MOVE "READ-TEST-F1-03" TO PAR-NAME.                          SQ2234.2
056800     MOVE "EXPECTED RECORD LENGTH: 120" TO RE-MARK.               SQ2234.2
056900     PERFORM PRINT-DETAIL.                                        SQ2234.2
057000 READ-INIT-F1-04.                                                 SQ2234.2
057100     MOVE ZERO TO ERROR-FLAG.                                     SQ2234.2
057200 READ-TEST-F1-04.                                                 SQ2234.2
057300     PERFORM READ-LONG-REC THRU READ-LONG-REC-EXIT 100 TIMES.     SQ2234.2
057400     IF EOF-FLAG EQUAL TO 1                                       SQ2234.2
057500         MOVE "UNEXPECTED EOF" TO RE-MARK                         SQ2234.2
057600         GO TO READ-EOF-F1-06.                                    SQ2234.2
057700     IF ERROR-FLAG EQUAL TO 1                                     SQ2234.2
057800         GO TO READ-FAIL-F1-04.                                   SQ2234.2
057900 READ-PASS-F1-04.                                                 SQ2234.2
058000     PERFORM PASS.                                                SQ2234.2
058100     GO TO READ-WRITE-F1-04.                                      SQ2234.2
058200 READ-FAIL-F1-04.                                                 SQ2234.2
058300     MOVE "ERROR:SEE VII-52 WRITE  OR VII-44 READ; VII-30 FORMAT  SQ2234.2
058400-         "RECORD IS VARYING IN SIZE ..."              TO RE-MARK.SQ2234.2
058500     MOVE 151 TO CORRECT-N.                                       SQ2234.2
058600     PERFORM FAIL.                                                SQ2234.2
058700 READ-WRITE-F1-04.                                                SQ2234.2
058800     MOVE "READ LONG RECORDS" TO FEATURE.                         SQ2234.2
058900     MOVE "READ-TEST-F1-04" TO PAR-NAME.                          SQ2234.2
059000     MOVE "EXPECTED RECORD LENGTH: 151" TO RE-MARK.               SQ2234.2
059100     PERFORM PRINT-DETAIL.                                        SQ2234.2
059200 READ-INIT-F1-05.                                                 SQ2234.2
059300     MOVE ZERO TO ERROR-FLAG.                                     SQ2234.2
059400 READ-TEST-F1-05.                                                 SQ2234.2
059500     PERFORM READ-SHORT-REC THRU READ-SHORT-REC-EXIT 338 TIMES.   SQ2234.2
059600     IF EOF-FLAG EQUAL TO 1                                       SQ2234.2
059700         MOVE "UNEXPECTED EOF" TO RE-MARK                         SQ2234.2
059800         GO TO READ-EOF-F1-06.                                    SQ2234.2
059900     IF ERROR-FLAG EQUAL TO 1                                     SQ2234.2
060000         GO TO READ-FAIL-F1-05.                                   SQ2234.2
060100 READ-PASS-F1-05.                                                 SQ2234.2
060200     MOVE "ERROR:SEE VII-52 WRITE  OR VII-44 READ; VII-30 FORMAT  SQ2234.2
060300-         "RECORD IS VARYING IN SIZE ..."              TO RE-MARK.SQ2234.2
060400     PERFORM PASS.                                                SQ2234.2
060500     GO TO READ-WRITE-F1-05.                                      SQ2234.2
060600 READ-FAIL-F1-05.                                                 SQ2234.2
060700     MOVE 120 TO CORRECT-N.                                       SQ2234.2
060800     PERFORM FAIL.                                                SQ2234.2
060900 READ-WRITE-F1-05.                                                SQ2234.2
061000     MOVE "READ SHORT RECORDS" TO FEATURE.                        SQ2234.2
061100     MOVE "READ-TEST-F1-05" TO PAR-NAME.                          SQ2234.2
061200     MOVE "EXPECTED RECORD LENGTH: 120" TO RE-MARK.               SQ2234.2
061300     PERFORM PRINT-DETAIL.                                        SQ2234.2
061400 READ-INIT-F1-06.                                                 SQ2234.2
061500     READ SQ-VS7 RECORD END                                       SQ2234.2
061600         GO TO READ-TEST-F1-06.                                   SQ2234.2
061700     MOVE "MORE THAN 450 RECORDS" TO RE-MARK.                     SQ2234.2
061800     GO TO READ-FAIL-F1-06.                                       SQ2234.2
061900 READ-EOF-F1-06.                                                  SQ2234.2
062000     MOVE "RECORDS READ =" TO COMPUTED-A.                         SQ2234.2
062100     MOVE COUNT-OF-RECS TO CORRECT-18V0.                          SQ2234.2
062200     GO TO READ-FAIL-F1-06.                                       SQ2234.2
062300 READ-TEST-F1-06.                                                 SQ2234.2
062400     IF RECORDS-IN-ERROR NOT EQUAL TO ZERO                        SQ2234.2
062500         MOVE "RECORDS IN ERROR =" TO COMPUTED-A                  SQ2234.2
062600     MOVE RECORDS-IN-ERROR TO CORRECT-18V0                        SQ2234.2
062700         GO TO READ-FAIL-F1-06.                                   SQ2234.2
062800 READ-PASS-F1-06.                                                 SQ2234.2
062900     PERFORM PASS.                                                SQ2234.2
063000     GO TO READ-WRITE-F1-06.                                      SQ2234.2
063100 READ-FAIL-F1-06.                                                 SQ2234.2
063200     PERFORM FAIL.                                                SQ2234.2
063300 READ-WRITE-F1-06.                                                SQ2234.2
063400     MOVE "READ-TEST-F1-06" TO PAR-NAME.                          SQ2234.2
063500     MOVE "VERIFY FILE SQ-VS7" TO FEATURE.                        SQ2234.2
063600     PERFORM PRINT-DETAIL.                                        SQ2234.2
063700 READ-CLOSE-F1-06.                                                SQ2234.2
063800     CLOSE SQ-VS7.                                                SQ2234.2
063900 SECT-SQ223A-0002 SECTION.                                        SQ2234.2
064000*        THIS SECTION CHECKS IF THE ENTIRE RECORD AREA IS         SQ2234.2
064100*    WRITTEN ON THE MASS STORAGE DEVICE WHEN A SHORT RECORD       SQ2234.2
064200*    IS WRITTEN.  THE RECORD NUMBER IN CHARACTERS 126 THROUGH     SQ2234.2
064300*    130 IS UNIQUE FOR EACH RECORD.                               SQ2234.2
064400 INFO-INIT-01.                                                    SQ2234.2
064500     OPEN INPUT SQ-VS7.                                           SQ2234.2
064600     MOVE ZERO TO COUNT-OF-RECS.                                  SQ2234.2
064700 INFO-TEST-01.                                                    SQ2234.2
064800     READ SQ-VS7 AT END                                           SQ2234.2
064900         GO TO INFO-END.                                          SQ2234.2
065000     ADD 1 TO COUNT-OF-RECS.                                      SQ2234.2
065100     IF SQ-VS7-RECNO NOT EQUAL TO "00001"                         SQ2234.2
065200         GO TO NO-INFO-01.                                        SQ2234.2
065300     MOVE "MAXIMUM RECORD SIZE WRITTEN" TO RE-MARK.               SQ2234.2
065400     MOVE "RECORD READ =" TO COMPUTED-A.                          SQ2234.2
065500     MOVE 0001 TO CORRECT-18V0.                                   SQ2234.2
065600     GO TO INFO-WRITE-01.                                         SQ2234.2
065700 NO-INFO-01.                                                      SQ2234.2
065800     MOVE "NO DEFINITE CONCLUSION POSSIBLE" TO RE-MARK.           SQ2234.2
065900 INFO-WRITE-01.                                                   SQ2234.2
066000     MOVE "READ SHORT RECORD" TO FEATURE.                         SQ2234.2
066100     MOVE "SEQ-INFO-01 " TO PAR-NAME.                             SQ2234.2
066200     PERFORM PRINT-DETAIL.                                        SQ2234.2
066300 INFO-INIT-02.                                                    SQ2234.2
066400     READ SQ-VS7 RECORD AT END                                    SQ2234.2
066500         GO TO INFO-END.                                          SQ2234.2
066600     READ SQ-VS7 END                                              SQ2234.2
066700         GO TO INFO-END.                                          SQ2234.2
066800 INFO-TEST-02.                                                    SQ2234.2
066900     READ SQ-VS7 AT END                                           SQ2234.2
067000         GO TO INFO-END.                                          SQ2234.2
067100     IF SQ-VS7-RECNO NOT EQUAL TO "00004"                         SQ2234.2
067200         GO TO NO-INFO-02.                                        SQ2234.2
067300     MOVE "MAXIMUM RECORD SIZE WRITTEN" TO RE-MARK.               SQ2234.2
067400     MOVE "RECORD READ =" TO COMPUTED-A.                          SQ2234.2
067500     MOVE 0004 TO CORRECT-18V0.                                   SQ2234.2
067600     GO TO INFO-WRITE-02.                                         SQ2234.2
067700 NO-INFO-02.                                                      SQ2234.2
067800     MOVE "NO DEFINITE CONCLUSION POSSIBLE" TO RE-MARK.           SQ2234.2
067900 INFO-WRITE-02.                                                   SQ2234.2
068000     MOVE "READ SHORT RECORD" TO FEATURE.                         SQ2234.2
068100     MOVE "SEQ-INFO-02 " TO PAR-NAME.                             SQ2234.2
068200     PERFORM PRINT-DETAIL.                                        SQ2234.2
068300 INFO-INIT-03.                                                    SQ2234.2
068400     ADD 3 TO COUNT-OF-RECS.                                      SQ2234.2
068500 INFO-INIT-03-1.                                                  SQ2234.2
068600     READ SQ-VS7 RECORD                                           SQ2234.2
068700         END GO TO INFO-END.                                      SQ2234.2
068800     ADD 1 TO COUNT-OF-RECS.                                      SQ2234.2
068900     IF COUNT-OF-RECS EQUAL TO 450                                SQ2234.2
069000         GO TO INFO-TEST-03.                                      SQ2234.2
069100     GO TO INFO-INIT-03-1.                                        SQ2234.2
069200 INFO-TEST-03.                                                    SQ2234.2
069300     IF SQ-VS7-RECNO NOT EQUAL TO "00450"                         SQ2234.2
069400         GO TO NO-INFO-03.                                        SQ2234.2
069500     MOVE "MAXIMUM RECORD SIZE WRITTEN" TO RE-MARK.               SQ2234.2
069600     MOVE "RECORD READ =" TO COMPUTED-A.                          SQ2234.2
069700     MOVE 0450 TO CORRECT-18V0.                                   SQ2234.2
069800     GO TO INFO-WRITE-03.                                         SQ2234.2
069900 NO-INFO-03.                                                      SQ2234.2
070000     MOVE "NO DEFINITE CONCLUSION POSSIBLE" TO RE-MARK.           SQ2234.2
070100 INFO-WRITE-03.                                                   SQ2234.2
070200     MOVE "READ SHORT RECORD" TO FEATURE.                         SQ2234.2
070300     MOVE "SEQ-INFO-03 " TO PAR-NAME.                             SQ2234.2
070400     PERFORM PRINT-DETAIL.                                        SQ2234.2
070500 INFO-END.                                                        SQ2234.2
070600     CLOSE SQ-VS7.                                                SQ2234.2
070700 TERMINATE-ROUTINE.                                               SQ2234.2
070800     EXIT.                                                        SQ2234.2
070900 CCVS-EXIT SECTION.                                               SQ2234.2
071000 CCVS-999999.                                                     SQ2234.2
071100     GO TO CLOSE-FILES.                                           SQ2234.2
      *END-OF,SQ223A                                                                  
