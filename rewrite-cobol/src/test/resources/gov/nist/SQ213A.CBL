      *HEADER,COBOL,SQ213A                                                            
000100 IDENTIFICATION DIVISION.                                         SQ2134.2
000200 PROGRAM-ID.                                                      SQ2134.2
000300     SQ213A.                                                      SQ2134.2
000400****************************************************************  SQ2134.2
000500*                                                              *  SQ2134.2
000600*    VALIDATION FOR:-                                          *  SQ2134.2
000700*    " HIGH       ".                                              SQ2134.2
000800*                                                              *  SQ2134.2
000900*    CREATION DATE     /     VALIDATION DATE                   *  SQ2134.2
001000*    "4.2 ".                                                      SQ2134.2
001100*                                                              *  SQ2134.2
001200*         THE ROUTINE SQ213A TESTS THE USE OF THE USE AFTER ERROR SQ2134.2
001300*    PROCEDURE FOR EXTEND AND FILE-NAME SERIES.  SQ213A IS        SQ2134.2
001400*    BASICALLY A REWRITE OF SQ205 WITH THE ADDITION OF THE USE    SQ2134.2
001500*    PROCEDURES.  MAGNETIC TAPE FILE SQ-FS1 IS FIRST CREATED WITH SQ2134.2
001600*    750 RECORDS.  THEN IT IS REOPENED WITH EXTEND AND AN         SQ2134.2
001700*    ADDITIONAL 250 RECORDS ARE WRITTEN.  FINALLY IT IS READ AND  SQ2134.2
001800*    VALIDATED FOR CORRECTNESS.  MASS-STORAGE FILE SQ-FS2 IS      SQ2134.2
001900*    CREATED AS A SINGLE OUTPUT FILE WITH 1000 RECORDS, AFTERWHICHSQ2134.2
002000*    IT IS READ AND VALIDATED FOR CORRECTNESS.  THE TEST FOR THE  SQ2134.2
002100*    USE PROCEDURE MERELY INDICATES WHETHER OR NOT THE USE        SQ2134.2
002200*    PROCEDURES WERE REFERENCED.                                  SQ2134.2
002300 ENVIRONMENT DIVISION.                                            SQ2134.2
002400 CONFIGURATION SECTION.                                           SQ2134.2
002500 SOURCE-COMPUTER.                                                 SQ2134.2
002600     XXXXX082.                                                    SQ2134.2
002700 OBJECT-COMPUTER.                                                 SQ2134.2
002800     XXXXX083.                                                    SQ2134.2
002900 INPUT-OUTPUT SECTION.                                            SQ2134.2
003000 FILE-CONTROL.                                                    SQ2134.2
003100*    SELECT RAW-DATA   ASSIGN TO                                  SQ2134.2
003200*    XXXXX062                                                     SQ2134.2
003300*           ORGANIZATION IS INDEXED                               SQ2134.2
003400*           ACCESS MODE IS RANDOM                                 SQ2134.2
003500*           RECORD KEY IS RAW-DATA-KEY.                           SQ2134.2
003600     SELECT PRINT-FILE ASSIGN TO                                  SQ2134.2
003700     XXXXX055.                                                    SQ2134.2
003800     SELECT SQ-FS1 ASSIGN TO                                      SQ2134.2
003900     XXXXX001                                                     SQ2134.2
004000     ORGANIZATION IS SEQUENTIAL                                   SQ2134.2
004100     ACCESS MODE IS SEQUENTIAL.                                   SQ2134.2
004200     SELECT SQ-FS2 ASSIGN TO                                      SQ2134.2
004300     XXXXX014                                                     SQ2134.2
004400     ORGANIZATION IS SEQUENTIAL                                   SQ2134.2
004500     ACCESS MODE IS SEQUENTIAL.                                   SQ2134.2
004600 DATA DIVISION.                                                   SQ2134.2
004700 FILE SECTION.                                                    SQ2134.2
004800*                                                                 SQ2134.2
004900*FD  RAW-DATA.                                                    SQ2134.2
005000*                                                                 SQ2134.2
005100*01  RAW-DATA-SATZ.                                               SQ2134.2
005200*    05  RAW-DATA-KEY        PIC X(6).                            SQ2134.2
005300*    05  C-DATE              PIC 9(6).                            SQ2134.2
005400*    05  C-TIME              PIC 9(8).                            SQ2134.2
005500*    05  C-NO-OF-TESTS       PIC 99.                              SQ2134.2
005600*    05  C-OK                PIC 999.                             SQ2134.2
005700*    05  C-ALL               PIC 999.                             SQ2134.2
005800*    05  C-FAIL              PIC 999.                             SQ2134.2
005900*    05  C-DELETED           PIC 999.                             SQ2134.2
006000*    05  C-INSPECT           PIC 999.                             SQ2134.2
006100*    05  C-NOTE              PIC X(13).                           SQ2134.2
006200*    05  C-INDENT            PIC X.                               SQ2134.2
006300*    05  C-ABORT             PIC X(8).                            SQ2134.2
006400 FD  PRINT-FILE                                                   SQ2134.2
006500C    LABEL RECORDS                                                SQ2134.2
006600C    XXXXX084                                                     SQ2134.2
006700C    DATA RECORD IS PRINT-REC DUMMY-RECORD                        SQ2134.2
006800               .                                                  SQ2134.2
006900 01  PRINT-REC PICTURE X(120).                                    SQ2134.2
007000 01  DUMMY-RECORD PICTURE X(120).                                 SQ2134.2
007100 FD  SQ-FS1                                                       SQ2134.2
007200C    LABEL RECORDS ARE STANDARD                                   SQ2134.2
007300     RECORD CONTAINS 126 CHARACTERS                               SQ2134.2
007400     BLOCK CONTAINS 126 CHARACTERS.                               SQ2134.2
007500 01  SQ-FS1R1-F-G-126.                                            SQ2134.2
007600     02 SQ-FS1R1-F-G-120 PIC X(120).                              SQ2134.2
007700     02 SQ-FS1R1-F-G-006 PIC X(6).                                SQ2134.2
007800 FD  SQ-FS2                                                       SQ2134.2
007900C    LABEL RECORDS ARE STANDARD                                   SQ2134.2
008000     RECORD 126                                                   SQ2134.2
008100     BLOCK CONTAINS 126 CHARACTERS.                               SQ2134.2
008200 01  SQ-FS2R1-F-G-126.                                            SQ2134.2
008300     02 SQ-FS2R1-F-G-120 PIC X(120).                              SQ2134.2
008400     02 SQ-FS2R1-F-G-006 PIC X(6).                                SQ2134.2
008500 WORKING-STORAGE SECTION.                                         SQ2134.2
008600 77  RECORDS-IN-ERROR   PIC  9(4) VALUE 0.                        SQ2134.2
008700 77  WRK-RECORD-COUNT   PIC  9(4) VALUE 0.                        SQ2134.2
008800 01  COUNT-OF-RECS PIC 9999.                                      SQ2134.2
008900 01  EXTEND-ERROR       PIC 9999  VALUE 0.                        SQ2134.2
009000 01  FN-SERIES-ERROR    PIC 9999  VALUE 0.                        SQ2134.2
009100 01  FILE-RECORD-INFORMATION-REC.                                 SQ2134.2
009200     03 FILE-RECORD-INFO-SKELETON.                                SQ2134.2
009300        05 FILLER                 PICTURE X(48)       VALUE       SQ2134.2
009400             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  SQ2134.2
009500        05 FILLER                 PICTURE X(46)       VALUE       SQ2134.2
009600             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    SQ2134.2
009700        05 FILLER                 PICTURE X(26)       VALUE       SQ2134.2
009800             ",LFIL=000000,ORG=  ,LBLR= ".                        SQ2134.2
009900        05 FILLER                 PICTURE X(37)       VALUE       SQ2134.2
010000             ",RECKEY=                             ".             SQ2134.2
010100        05 FILLER                 PICTURE X(38)       VALUE       SQ2134.2
010200             ",ALTKEY1=                             ".            SQ2134.2
010300        05 FILLER                 PICTURE X(38)       VALUE       SQ2134.2
010400             ",ALTKEY2=                             ".            SQ2134.2
010500        05 FILLER                 PICTURE X(7)        VALUE SPACE.SQ2134.2
010600     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              SQ2134.2
010700        05 FILE-RECORD-INFO-P1-120.                               SQ2134.2
010800           07 FILLER              PIC X(5).                       SQ2134.2
010900           07 XFILE-NAME           PIC X(6).                      SQ2134.2
011000           07 FILLER              PIC X(8).                       SQ2134.2
011100           07 XRECORD-NAME         PIC X(6).                      SQ2134.2
011200           07 FILLER              PIC X(1).                       SQ2134.2
011300           07 REELUNIT-NUMBER     PIC 9(1).                       SQ2134.2
011400           07 FILLER              PIC X(7).                       SQ2134.2
011500           07 XRECORD-NUMBER       PIC 9(6).                      SQ2134.2
011600           07 FILLER              PIC X(6).                       SQ2134.2
011700           07 UPDATE-NUMBER       PIC 9(2).                       SQ2134.2
011800           07 FILLER              PIC X(5).                       SQ2134.2
011900           07 ODO-NUMBER          PIC 9(4).                       SQ2134.2
012000           07 FILLER              PIC X(5).                       SQ2134.2
012100           07 XPROGRAM-NAME        PIC X(5).                      SQ2134.2
012200           07 FILLER              PIC X(7).                       SQ2134.2
012300           07 XRECORD-LENGTH       PIC 9(6).                      SQ2134.2
012400           07 FILLER              PIC X(7).                       SQ2134.2
012500           07 CHARS-OR-RECORDS    PIC X(2).                       SQ2134.2
012600           07 FILLER              PIC X(1).                       SQ2134.2
012700           07 XBLOCK-SIZE          PIC 9(4).                      SQ2134.2
012800           07 FILLER              PIC X(6).                       SQ2134.2
012900           07 RECORDS-IN-FILE     PIC 9(6).                       SQ2134.2
013000           07 FILLER              PIC X(5).                       SQ2134.2
013100           07 XFILE-ORGANIZATION   PIC X(2).                      SQ2134.2
013200           07 FILLER              PIC X(6).                       SQ2134.2
013300           07 XLABEL-TYPE          PIC X(1).                      SQ2134.2
013400        05 FILE-RECORD-INFO-P121-240.                             SQ2134.2
013500           07 FILLER              PIC X(8).                       SQ2134.2
013600           07 XRECORD-KEY          PIC X(29).                     SQ2134.2
013700           07 FILLER              PIC X(9).                       SQ2134.2
013800           07 ALTERNATE-KEY1      PIC X(29).                      SQ2134.2
013900           07 FILLER              PIC X(9).                       SQ2134.2
014000           07 ALTERNATE-KEY2      PIC X(29).                      SQ2134.2
014100           07 FILLER              PIC X(7).                       SQ2134.2
014200 01  TEST-RESULTS.                                                SQ2134.2
014300     02 FILLER                    PICTURE X VALUE SPACE.          SQ2134.2
014400     02 FEATURE                   PICTURE X(20) VALUE SPACE.      SQ2134.2
014500     02 FILLER                    PICTURE X VALUE SPACE.          SQ2134.2
014600     02 P-OR-F                    PICTURE X(5) VALUE SPACE.       SQ2134.2
014700     02 FILLER                    PICTURE X  VALUE SPACE.         SQ2134.2
014800     02  PAR-NAME.                                                SQ2134.2
014900       03 FILLER PICTURE X(12) VALUE SPACE.                       SQ2134.2
015000       03  PARDOT-X PICTURE X  VALUE SPACE.                       SQ2134.2
015100       03 DOTVALUE PICTURE 99  VALUE ZERO.                        SQ2134.2
015200       03 FILLER PIC X(5) VALUE SPACE.                            SQ2134.2
015300     02 FILLER PIC X(10) VALUE SPACE.                             SQ2134.2
015400     02 RE-MARK PIC X(61).                                        SQ2134.2
015500 01  TEST-COMPUTED.                                               SQ2134.2
015600     02 FILLER PIC X(30) VALUE SPACE.                             SQ2134.2
015700     02 FILLER PIC X(17) VALUE "       COMPUTED=".                SQ2134.2
015800     02 COMPUTED-X.                                               SQ2134.2
015900     03 COMPUTED-A                PICTURE X(20) VALUE SPACE.      SQ2134.2
016000     03 COMPUTED-N REDEFINES COMPUTED-A PICTURE -9(9).9(9).       SQ2134.2
016100     03 COMPUTED-0V18 REDEFINES COMPUTED-A  PICTURE -.9(18).      SQ2134.2
016200     03 COMPUTED-4V14 REDEFINES COMPUTED-A  PICTURE -9(4).9(14).  SQ2134.2
016300     03 COMPUTED-14V4 REDEFINES COMPUTED-A  PICTURE -9(14).9(4).  SQ2134.2
016400     03       CM-18V0 REDEFINES COMPUTED-A.                       SQ2134.2
016500         04 COMPUTED-18V0                   PICTURE -9(18).       SQ2134.2
016600         04 FILLER                          PICTURE X.            SQ2134.2
016700     03 FILLER PIC X(50) VALUE SPACE.                             SQ2134.2
016800 01  TEST-CORRECT.                                                SQ2134.2
016900     02 FILLER PIC X(30) VALUE SPACE.                             SQ2134.2
017000     02 FILLER PIC X(17) VALUE "       CORRECT =".                SQ2134.2
017100     02 CORRECT-X.                                                SQ2134.2
017200     03 CORRECT-A                 PICTURE X(20) VALUE SPACE.      SQ2134.2
017300     03 CORRECT-N REDEFINES CORRECT-A PICTURE -9(9).9(9).         SQ2134.2
017400     03 CORRECT-0V18 REDEFINES CORRECT-A    PICTURE -.9(18).      SQ2134.2
017500     03 CORRECT-4V14 REDEFINES CORRECT-A    PICTURE -9(4).9(14).  SQ2134.2
017600     03 CORRECT-14V4 REDEFINES CORRECT-A    PICTURE -9(14).9(4).  SQ2134.2
017700     03      CR-18V0 REDEFINES CORRECT-A.                         SQ2134.2
017800         04 CORRECT-18V0                    PICTURE -9(18).       SQ2134.2
017900         04 FILLER                          PICTURE X.            SQ2134.2
018000     03 FILLER PIC X(50) VALUE SPACE.                             SQ2134.2
018100 01  CCVS-C-1.                                                    SQ2134.2
018200     02 FILLER PICTURE IS X(99) VALUE IS " FEATURE              PASQ2134.2
018300-    "SS  PARAGRAPH-NAME                                          SQ2134.2
018400-    "        REMARKS".                                           SQ2134.2
018500     02 FILLER PICTURE IS X(20) VALUE IS SPACE.                   SQ2134.2
018600 01  CCVS-C-2.                                                    SQ2134.2
018700     02 FILLER PICTURE IS X VALUE IS SPACE.                       SQ2134.2
018800     02 FILLER PICTURE IS X(6) VALUE IS "TESTED".                 SQ2134.2
018900     02 FILLER PICTURE IS X(15) VALUE IS SPACE.                   SQ2134.2
019000     02 FILLER PICTURE IS X(4) VALUE IS "FAIL".                   SQ2134.2
019100     02 FILLER PICTURE IS X(94) VALUE IS SPACE.                   SQ2134.2
019200 01  REC-SKL-SUB PICTURE 9(2) VALUE ZERO.                         SQ2134.2
019300 01  REC-CT PICTURE 99 VALUE ZERO.                                SQ2134.2
019400 01  DELETE-CNT                   PICTURE 999  VALUE ZERO.        SQ2134.2
019500 01  ERROR-COUNTER PICTURE IS 999 VALUE IS ZERO.                  SQ2134.2
019600 01  INSPECT-COUNTER PIC 999 VALUE ZERO.                          SQ2134.2
019700 01  PASS-COUNTER PIC 999 VALUE ZERO.                             SQ2134.2
019800 01  TOTAL-ERROR PIC 999 VALUE ZERO.                              SQ2134.2
019900 01  ERROR-HOLD PIC 999 VALUE ZERO.                               SQ2134.2
020000 01  DUMMY-HOLD PIC X(120) VALUE SPACE.                           SQ2134.2
020100 01  RECORD-COUNT PIC 9(5) VALUE ZERO.                            SQ2134.2
020200 01  CCVS-H-1.                                                    SQ2134.2
020300     02  FILLER   PICTURE X(27)  VALUE SPACE.                     SQ2134.2
020400     02 FILLER PICTURE X(67) VALUE                                SQ2134.2
020500     " FEDERAL SOFTWARE TESTING CENTER COBOL COMPILER VALIDATION  SQ2134.2
020600-    " SYSTEM".                                                   SQ2134.2
020700     02  FILLER     PICTURE X(26)  VALUE SPACE.                   SQ2134.2
020800 01  CCVS-H-2.                                                    SQ2134.2
020900     02 FILLER PICTURE X(52) VALUE IS                             SQ2134.2
021000     "CCVS85 FSTC COPY, NOT FOR DISTRIBUTION.".                   SQ2134.2
021100     02 FILLER PICTURE IS X(19) VALUE IS "TEST RESULTS SET-  ".   SQ2134.2
021200     02 TEST-ID PICTURE IS X(9).                                  SQ2134.2
021300     02 FILLER PICTURE IS X(40) VALUE IS SPACE.                   SQ2134.2
021400 01  CCVS-H-3.                                                    SQ2134.2
021500     02  FILLER PICTURE X(34) VALUE                               SQ2134.2
021600     " FOR OFFICIAL USE ONLY    ".                                SQ2134.2
021700     02  FILLER PICTURE X(58) VALUE                               SQ2134.2
021800     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ2134.2
021900     02  FILLER PICTURE X(28) VALUE                               SQ2134.2
022000     "  COPYRIGHT   1985 ".                                       SQ2134.2
022100 01  CCVS-E-1.                                                    SQ2134.2
022200     02 FILLER PICTURE IS X(52) VALUE IS SPACE.                   SQ2134.2
022300     02 FILLER PICTURE IS X(14) VALUE IS "END OF TEST-  ".        SQ2134.2
022400     02 ID-AGAIN PICTURE IS X(9).                                 SQ2134.2
022500     02 FILLER PICTURE X(45) VALUE IS                             SQ2134.2
022600     " NTIS DISTRIBUTION COBOL 85".                               SQ2134.2
022700 01  CCVS-E-2.                                                    SQ2134.2
022800     02  FILLER                   PICTURE X(31)  VALUE            SQ2134.2
022900     SPACE.                                                       SQ2134.2
023000     02  FILLER                   PICTURE X(21)  VALUE SPACE.     SQ2134.2
023100     02 CCVS-E-2-2.                                               SQ2134.2
023200         03 ERROR-TOTAL PICTURE IS XXX VALUE IS SPACE.            SQ2134.2
023300         03 FILLER PICTURE IS X VALUE IS SPACE.                   SQ2134.2
023400         03 ENDER-DESC PIC X(46) VALUE "ERRORS ENCOUNTERED".      SQ2134.2
023500 01  CCVS-E-3.                                                    SQ2134.2
023600     02  FILLER PICTURE X(22) VALUE                               SQ2134.2
023700     " FOR OFFICIAL USE ONLY".                                    SQ2134.2
023800     02  FILLER PICTURE X(12) VALUE SPACE.                        SQ2134.2
023900     02  FILLER PICTURE X(58) VALUE                               SQ2134.2
024000     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ2134.2
024100     02  FILLER PICTURE X(13) VALUE SPACE.                        SQ2134.2
024200     02 FILLER PIC X(15) VALUE " COPYRIGHT 1985".                 SQ2134.2
024300 01  CCVS-E-4.                                                    SQ2134.2
024400     02 CCVS-E-4-1 PIC XXX VALUE SPACE.                           SQ2134.2
024500     02 FILLER PIC XXXX VALUE " OF ".                             SQ2134.2
024600     02 CCVS-E-4-2 PIC XXX VALUE SPACE.                           SQ2134.2
024700     02 FILLER PIC X(40) VALUE                                    SQ2134.2
024800      "  TESTS WERE EXECUTED SUCCESSFULLY".                       SQ2134.2
024900 01  XXINFO.                                                      SQ2134.2
025000     02 FILLER PIC X(30) VALUE "        *** INFORMATION  ***".    SQ2134.2
025100     02 INFO-TEXT.                                                SQ2134.2
025200     04 FILLER PIC X(20) VALUE SPACE.                             SQ2134.2
025300     04 XXCOMPUTED PIC X(20).                                     SQ2134.2
025400     04 FILLER PIC X(5) VALUE SPACE.                              SQ2134.2
025500     04 XXCORRECT PIC X(20).                                      SQ2134.2
025600 01  HYPHEN-LINE.                                                 SQ2134.2
025700     02 FILLER PICTURE IS X VALUE IS SPACE.                       SQ2134.2
025800     02 FILLER PICTURE IS X(65) VALUE IS "************************SQ2134.2
025900-    "*****************************************".                 SQ2134.2
026000     02 FILLER PICTURE IS X(54) VALUE IS "************************SQ2134.2
026100-    "******************************".                            SQ2134.2
026200 01  CCVS-PGM-ID PIC X(6) VALUE                                   SQ2134.2
026300     "SQ213A".                                                    SQ2134.2
026400 PROCEDURE DIVISION.                                              SQ2134.2
026500 DECLARATIVES.                                                    SQ2134.2
026600 SECT-SQ213A-0001 SECTION.                                        SQ2134.2
026700     USE AFTER ERROR PROCEDURE EXTEND.                            SQ2134.2
026800 EXTEND-ERROR-PROCESS.                                            SQ2134.2
026900     MOVE 1 TO EXTEND-ERROR.                                      SQ2134.2
027000 SECT-SQ213A-0002 SECTION.                                        SQ2134.2
027100     USE AFTER EXCEPTION PROCEDURE ON SQ-FS2, PRINT-FILE.         SQ2134.2
027200 FN-SERIES-ERROR-PROCESS.                                         SQ2134.2
027300     MOVE 1 TO FN-SERIES-ERROR.                                   SQ2134.2
027400 END DECLARATIVES.                                                SQ2134.2
027500 CCVS1 SECTION.                                                   SQ2134.2
027600 OPEN-FILES.                                                      SQ2134.2
027700*    OPEN I-O RAW-DATA.                                           SQ2134.2
027800*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            SQ2134.2
027900*    READ RAW-DATA INVALID KEY GO TO END-E-1.                     SQ2134.2
028000*    MOVE "ABORTED " TO C-ABORT.                                  SQ2134.2
028100*    ADD 1 TO C-NO-OF-TESTS.                                      SQ2134.2
028200*    ACCEPT C-DATE  FROM DATE.                                    SQ2134.2
028300*    ACCEPT C-TIME  FROM TIME.                                    SQ2134.2
028400*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-1.             SQ2134.2
028500*END-E-1.                                                         SQ2134.2
028600*    CLOSE RAW-DATA.                                              SQ2134.2
028700     OPEN     OUTPUT PRINT-FILE.                                  SQ2134.2
028800     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   SQ2134.2
028900     MOVE    SPACE TO TEST-RESULTS.                               SQ2134.2
029000     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             SQ2134.2
029100     MOVE ZERO TO REC-SKL-SUB.                                    SQ2134.2
029200     PERFORM CCVS-INIT-FILE 9 TIMES.                              SQ2134.2
029300 CCVS-INIT-FILE.                                                  SQ2134.2
029400     ADD 1 TO REC-SKL-SUB.                                        SQ2134.2
029500     MOVE FILE-RECORD-INFO-SKELETON TO                            SQ2134.2
029600                  FILE-RECORD-INFO (REC-SKL-SUB).                 SQ2134.2
029700 CCVS-INIT-EXIT.                                                  SQ2134.2
029800     GO TO CCVS1-EXIT.                                            SQ2134.2
029900 CLOSE-FILES.                                                     SQ2134.2
030000     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   SQ2134.2
030100*    OPEN I-O RAW-DATA.                                           SQ2134.2
030200*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            SQ2134.2
030300*    READ RAW-DATA INVALID KEY GO TO END-E-2.                     SQ2134.2
030400*    MOVE "OK.     " TO C-ABORT.                                  SQ2134.2
030500*    MOVE PASS-COUNTER TO C-OK.                                   SQ2134.2
030600*    MOVE ERROR-HOLD   TO C-ALL.                                  SQ2134.2
030700*    MOVE ERROR-COUNTER TO C-FAIL.                                SQ2134.2
030800*    MOVE DELETE-CNT TO C-DELETED.                                SQ2134.2
030900*    MOVE INSPECT-COUNTER TO C-INSPECT.                           SQ2134.2
031000*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-2.             SQ2134.2
031100*END-E-2.                                                         SQ2134.2
031200*    CLOSE RAW-DATA.                                              SQ2134.2
031300 TERMINATE-CCVS.                                                  SQ2134.2
031400*    EXIT PROGRAM.                                                SQ2134.2
031500*TERMINATE-CALL.                                                  SQ2134.2
031600     STOP     RUN.                                                SQ2134.2
031700 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         SQ2134.2
031800 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           SQ2134.2
031900 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          SQ2134.2
032000 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-CNT.          SQ2134.2
032100     MOVE "****TEST DELETED****" TO RE-MARK.                      SQ2134.2
032200 PRINT-DETAIL.                                                    SQ2134.2
032300     IF REC-CT NOT EQUAL TO ZERO                                  SQ2134.2
032400             MOVE "." TO PARDOT-X                                 SQ2134.2
032500             MOVE REC-CT TO DOTVALUE.                             SQ2134.2
032600     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      SQ2134.2
032700     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               SQ2134.2
032800        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 SQ2134.2
032900          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 SQ2134.2
033000     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              SQ2134.2
033100     MOVE SPACE TO CORRECT-X.                                     SQ2134.2
033200     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         SQ2134.2
033300     MOVE     SPACE TO RE-MARK.                                   SQ2134.2
033400 HEAD-ROUTINE.                                                    SQ2134.2
033500     MOVE CCVS-H-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ2134.2
033600     MOVE CCVS-H-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.   SQ2134.2
033700     MOVE CCVS-H-3 TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.   SQ2134.2
033800 COLUMN-NAMES-ROUTINE.                                            SQ2134.2
033900     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ2134.2
034000     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ2134.2
034100     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ2134.2
034200 END-ROUTINE.                                                     SQ2134.2
034300     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.SQ2134.2
034400 END-RTN-EXIT.                                                    SQ2134.2
034500     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ2134.2
034600 END-ROUTINE-1.                                                   SQ2134.2
034700      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      SQ2134.2
034800      ERROR-HOLD. ADD DELETE-CNT TO ERROR-HOLD.                   SQ2134.2
034900      ADD PASS-COUNTER TO ERROR-HOLD.                             SQ2134.2
035000*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   SQ2134.2
035100      MOVE PASS-COUNTER TO CCVS-E-4-1.                            SQ2134.2
035200      MOVE ERROR-HOLD TO CCVS-E-4-2.                              SQ2134.2
035300      MOVE CCVS-E-4 TO CCVS-E-2-2.                                SQ2134.2
035400      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           SQ2134.2
035500  END-ROUTINE-12.                                                 SQ2134.2
035600      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        SQ2134.2
035700     IF       ERROR-COUNTER IS EQUAL TO ZERO                      SQ2134.2
035800         MOVE "NO " TO ERROR-TOTAL                                SQ2134.2
035900         ELSE                                                     SQ2134.2
036000         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       SQ2134.2
036100     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           SQ2134.2
036200     PERFORM WRITE-LINE.                                          SQ2134.2
036300 END-ROUTINE-13.                                                  SQ2134.2
036400     IF DELETE-CNT IS EQUAL TO ZERO                               SQ2134.2
036500         MOVE "NO " TO ERROR-TOTAL  ELSE                          SQ2134.2
036600         MOVE DELETE-CNT TO ERROR-TOTAL.                          SQ2134.2
036700     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   SQ2134.2
036800     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ2134.2
036900      IF   INSPECT-COUNTER EQUAL TO ZERO                          SQ2134.2
037000          MOVE "NO " TO ERROR-TOTAL                               SQ2134.2
037100      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   SQ2134.2
037200      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            SQ2134.2
037300      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          SQ2134.2
037400     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ2134.2
037500 WRITE-LINE.                                                      SQ2134.2
037600     ADD 1 TO RECORD-COUNT.                                       SQ2134.2
037700*    IF RECORD-COUNT GREATER 50                                   SQ2134.2
037800*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          SQ2134.2
037900*        MOVE SPACE TO DUMMY-RECORD                               SQ2134.2
038000*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  SQ2134.2
038100*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             SQ2134.2
038200*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     SQ2134.2
038300*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          SQ2134.2
038400*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          SQ2134.2
038500*        MOVE ZERO TO RECORD-COUNT.                               SQ2134.2
038600     PERFORM WRT-LN.                                              SQ2134.2
038700 WRT-LN.                                                          SQ2134.2
038800     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               SQ2134.2
038900     MOVE SPACE TO DUMMY-RECORD.                                  SQ2134.2
039000 BLANK-LINE-PRINT.                                                SQ2134.2
039100     PERFORM WRT-LN.                                              SQ2134.2
039200 FAIL-ROUTINE.                                                    SQ2134.2
039300     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.   SQ2134.2
039400     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.    SQ2134.2
039500     MOVE "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.    SQ2134.2
039600     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     SQ2134.2
039700     GO TO FAIL-ROUTINE-EX.                                       SQ2134.2
039800 FAIL-ROUTINE-WRITE.                                              SQ2134.2
039900     MOVE TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE           SQ2134.2
040000     MOVE TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES.   SQ2134.2
040100 FAIL-ROUTINE-EX. EXIT.                                           SQ2134.2
040200 BAIL-OUT.                                                        SQ2134.2
040300     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.       SQ2134.2
040400     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.               SQ2134.2
040500 BAIL-OUT-WRITE.                                                  SQ2134.2
040600     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  SQ2134.2
040700     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     SQ2134.2
040800 BAIL-OUT-EX. EXIT.                                               SQ2134.2
040900 CCVS1-EXIT.                                                      SQ2134.2
041000     EXIT.                                                        SQ2134.2
041100 SECT-SQ213A-0003 SECTION.                                        SQ2134.2
041200 OPEN-INIT-GF-01.                                                 SQ2134.2
041300*             THIS IS A TEST FOR OPEN EXTEND FOR MAGNETIC TAPE.   SQ2134.2
041400*             A FILE OF 750 RECORDS IS CREATED THEN RE-OPENED     SQ2134.2
041500*             WITH EXTEND.  250 RECORDS ARE ADDED TO THE FILE.    SQ2134.2
041600*             THE FILE IS THEN READ AND VALIDATED.                SQ2134.2
041700     MOVE "SQ-FS1" TO XFILE-NAME (1).                             SQ2134.2
041800     MOVE "R1-F-G" TO XRECORD-NAME (1).                           SQ2134.2
041900     MOVE "SQ213"  TO XPROGRAM-NAME (1).                          SQ2134.2
042000     MOVE 000126  TO XRECORD-LENGTH (1).                          SQ2134.2
042100     MOVE "RC"    TO CHARS-OR-RECORDS (1).                        SQ2134.2
042200     MOVE 0001    TO XBLOCK-SIZE (1).                             SQ2134.2
042300     MOVE 001000  TO RECORDS-IN-FILE (1).                         SQ2134.2
042400     MOVE "SQ"    TO XFILE-ORGANIZATION (1).                      SQ2134.2
042500     MOVE "S"     TO XLABEL-TYPE (1).                             SQ2134.2
042600     MOVE 000001  TO XRECORD-NUMBER (1).                          SQ2134.2
042700     OPEN OUTPUT SQ-FS1.                                          SQ2134.2
042800 OPEN-TEST-01-01.                                                 SQ2134.2
042900     MOVE FILE-RECORD-INFO-P1-120 (1) TO SQ-FS1R1-F-G-120.        SQ2134.2
043000     MOVE SPACES TO SQ-FS1R1-F-G-006.                             SQ2134.2
043100     WRITE SQ-FS1R1-F-G-126.                                      SQ2134.2
043200     IF XRECORD-NUMBER (1) EQUAL TO 750                           SQ2134.2
043300              GO TO OPEN-TEST-01-02.                              SQ2134.2
043400     ADD 1 TO XRECORD-NUMBER (1).                                 SQ2134.2
043500     GO TO OPEN-TEST-01-01.                                       SQ2134.2
043600 OPEN-TEST-01-02.                                                 SQ2134.2
043700     MOVE "CREATE FILE SQ-FS1" TO FEATURE.                        SQ2134.2
043800     MOVE "OPEN-TEST-GF-01-02"  TO PAR-NAME.                      SQ2134.2
043900     MOVE "FILE CREATED, RECS =" TO COMPUTED-A.                   SQ2134.2
044000     MOVE XRECORD-NUMBER (1) TO CORRECT-18V0.                     SQ2134.2
044100     PERFORM PASS.                                                SQ2134.2
044200     PERFORM PRINT-DETAIL.                                        SQ2134.2
044300     CLOSE SQ-FS1.                                                SQ2134.2
044400 OPEN-TEST-01-03.                                                 SQ2134.2
044500     OPEN EXTEND SQ-FS1.                                          SQ2134.2
044600     ADD 1 TO XRECORD-NUMBER (1).                                 SQ2134.2
044700 OPEN-TEST-01-04.                                                 SQ2134.2
044800     MOVE FILE-RECORD-INFO-P1-120 (1) TO SQ-FS1R1-F-G-120.        SQ2134.2
044900     MOVE "EXTEND" TO SQ-FS1R1-F-G-006.                           SQ2134.2
045000     WRITE SQ-FS1R1-F-G-126.                                      SQ2134.2
045100     IF XRECORD-NUMBER (1) EQUAL 1000                             SQ2134.2
045200              GO TO OPEN-TEST-GF-01-05.                           SQ2134.2
045300     ADD 1 TO XRECORD-NUMBER (1).                                 SQ2134.2
045400     GO TO OPEN-TEST-01-04.                                       SQ2134.2
045500 OPEN-TEST-GF-01-05.                                              SQ2134.2
045600     MOVE "OPEN O SQ-FS1 EXTEND" TO FEATURE.                      SQ2134.2
045700     MOVE "OPEN-TEST-GF-01-03" TO PAR-NAME.                       SQ2134.2
045800     MOVE "FILE EXTENDED RECS=" TO COMPUTED-A.                    SQ2134.2
045900     MOVE XRECORD-NUMBER (1) TO CORRECT-18V0.                     SQ2134.2
046000     PERFORM PASS.                                                SQ2134.2
046100     PERFORM PRINT-DETAIL.                                        SQ2134.2
046200     CLOSE SQ-FS1.                                                SQ2134.2
046300 READ-TEST-F1-01.                                                 SQ2134.2
046400     OPEN INPUT SQ-FS1.                                           SQ2134.2
046500     MOVE ZERO TO WRK-RECORD-COUNT.                               SQ2134.2
046600 READ-TEST-F1-01-07.                                              SQ2134.2
046700     READ SQ-FS1                                                  SQ2134.2
046800              ; AT END  MOVE "PREMATURE EOF" TO RE-MARK           SQ2134.2
046900                        PERFORM FAIL                              SQ2134.2
047000                        GO TO READ-WRITE-F1-01.                   SQ2134.2
047100     MOVE SQ-FS1R1-F-G-120 TO FILE-RECORD-INFO-P1-120 (1).        SQ2134.2
047200     ADD 1 TO WRK-RECORD-COUNT.                                   SQ2134.2
047300     IF XFILE-NAME (1) NOT EQUAL TO "SQ-FS1"                      SQ2134.2
047400              ADD 1 TO RECORDS-IN-ERROR                           SQ2134.2
047500              GO TO READ-TEST-F1-01-08.                           SQ2134.2
047600     IF WRK-RECORD-COUNT NOT EQUAL TO XRECORD-NUMBER (1)          SQ2134.2
047700              ADD 1 TO RECORDS-IN-ERROR                           SQ2134.2
047800              GO TO READ-TEST-F1-01-08.                           SQ2134.2
047900     IF SQ-FS1R1-F-G-006 NOT EQUAL TO SPACES                      SQ2134.2
048000              ADD 1 TO RECORDS-IN-ERROR.                          SQ2134.2
048100 READ-TEST-F1-01-08.                                              SQ2134.2
048200     IF WRK-RECORD-COUNT NOT EQUAL TO 750                         SQ2134.2
048300              GO TO READ-TEST-F1-01-07.                           SQ2134.2
048400 READ-TEST-F1-01-09.                                              SQ2134.2
048500     READ SQ-FS1 RECORD                                           SQ2134.2
048600              ; END GO TO READ-TEST-F1-01-10.                     SQ2134.2
048700     MOVE SQ-FS1R1-F-G-120 TO FILE-RECORD-INFO-P1-120 (1).        SQ2134.2
048800     ADD 1 TO WRK-RECORD-COUNT.                                   SQ2134.2
048900     IF XFILE-NAME (1) NOT EQUAL TO "SQ-FS1"                      SQ2134.2
049000              ADD 1 TO RECORDS-IN-ERROR                           SQ2134.2
049100              GO TO READ-TEST-F1-01-09.                           SQ2134.2
049200     IF WRK-RECORD-COUNT NOT EQUAL TO XRECORD-NUMBER (1)          SQ2134.2
049300              ADD 1 TO RECORDS-IN-ERROR                           SQ2134.2
049400              GO TO READ-TEST-F1-01-09.                           SQ2134.2
049500     IF SQ-FS1R1-F-G-006 NOT EQUAL TO "EXTEND"                    SQ2134.2
049600              ADD 1 TO RECORDS-IN-ERROR.                          SQ2134.2
049700     GO TO READ-TEST-F1-01-09.                                    SQ2134.2
049800 READ-TEST-F1-01-10.                                              SQ2134.2
049900     IF RECORDS-IN-ERROR EQUAL ZERO                               SQ2134.2
050000              GO TO READ-PASS-F1-01.                              SQ2134.2
050100     GO TO READ-FAIL-F1-01.                                       SQ2134.2
050200 READ-DELETE-F1-01.                                               SQ2134.2
050300     PERFORM DE-LETE.                                             SQ2134.2
050400     GO TO READ-WRITE-F1-01.                                      SQ2134.2
050500 READ-FAIL-F1-01.                                                 SQ2134.2
050600     MOVE "ERRORS IN READING SQ-FS1; VII-39; OPEN .. EXTEND"      SQ2134.2
050700          TO RE-MARK.                                             SQ2134.2
050800     MOVE "RECORDS IN ERROR =" TO COMPUTED-A.                     SQ2134.2
050900     MOVE RECORDS-IN-ERROR TO CORRECT-18V0.                       SQ2134.2
051000     PERFORM FAIL.                                                SQ2134.2
051100     GO TO READ-WRITE-F1-01.                                      SQ2134.2
051200 READ-PASS-F1-01.                                                 SQ2134.2
051300     PERFORM PASS.                                                SQ2134.2
051400     MOVE "FILE VERIFIED RECS =" TO COMPUTED-A.                   SQ2134.2
051500     MOVE WRK-RECORD-COUNT TO CORRECT-18V0.                       SQ2134.2
051600 READ-WRITE-F1-01.                                                SQ2134.2
051700     MOVE "READ-TEST-F1-01" TO PAR-NAME.                          SQ2134.2
051800     MOVE "VERIFY FILE SQ-FS1" TO FEATURE.                        SQ2134.2
051900     PERFORM PRINT-DETAIL.                                        SQ2134.2
052000 READ-CLOSE-F1-01.                                                SQ2134.2
052100     CLOSE SQ-FS1.                                                SQ2134.2
052200 WRITE-INIT-002.                                                  SQ2134.2
052300*         THIS TEST CREATES A MASS-STORAGE FILE OF 1000 RECORDS.  SQ2134.2
052400*         THEN IT IS READ AND VALIDATED FOR CORRECTNESS.          SQ2134.2
052500     MOVE "SQ-FS2" TO XFILE-NAME (2).                             SQ2134.2
052600     MOVE "R1-F-G" TO XRECORD-NAME (2).                           SQ2134.2
052700     MOVE "SQ213"  TO XPROGRAM-NAME (2).                          SQ2134.2
052800     MOVE 000126  TO XRECORD-LENGTH (2).                          SQ2134.2
052900     MOVE "RC"    TO CHARS-OR-RECORDS (2).                        SQ2134.2
053000     MOVE 0001    TO XBLOCK-SIZE (2).                             SQ2134.2
053100     MOVE 001000  TO RECORDS-IN-FILE (2).                         SQ2134.2
053200     MOVE "SQ"    TO XFILE-ORGANIZATION (2).                      SQ2134.2
053300     MOVE "S"     TO XLABEL-TYPE (2).                             SQ2134.2
053400     MOVE 000001  TO XRECORD-NUMBER (2).                          SQ2134.2
053500     OPEN OUTPUT SQ-FS2.                                          SQ2134.2
053600 WRITE-TEST-GF-01-1.                                              SQ2134.2
053700     MOVE FILE-RECORD-INFO-P1-120 (2) TO SQ-FS2R1-F-G-120.        SQ2134.2
053800     MOVE SPACES TO SQ-FS2R1-F-G-006.                             SQ2134.2
053900     WRITE SQ-FS2R1-F-G-126.                                      SQ2134.2
054000     IF XRECORD-NUMBER (2) EQUAL TO 1000                          SQ2134.2
054100              GO TO WRITE-TEST-GF-01-2.                           SQ2134.2
054200     ADD 1 TO XRECORD-NUMBER (2).                                 SQ2134.2
054300     GO TO WRITE-TEST-GF-01-1.                                    SQ2134.2
054400 WRITE-TEST-GF-01-2.                                              SQ2134.2
054500     MOVE "CREATE FILE SQ-FS2" TO FEATURE.                        SQ2134.2
054600     MOVE "WRITE-TEST-GF-01" TO PAR-NAME.                         SQ2134.2
054700     MOVE "FILE CREATED, RECS =" TO COMPUTED-A.                   SQ2134.2
054800     MOVE XRECORD-NUMBER (2) TO CORRECT-18V0.                     SQ2134.2
054900     PERFORM PASS. PERFORM PRINT-DETAIL.                          SQ2134.2
055000     CLOSE SQ-FS2.                                                SQ2134.2
055100 READ-INIT-F1-02.                                                 SQ2134.2
055200     OPEN INPUT SQ-FS2.                                           SQ2134.2
055300     MOVE ZERO TO WRK-RECORD-COUNT.                               SQ2134.2
055400     MOVE ZERO TO RECORDS-IN-ERROR.                               SQ2134.2
055500 READ-TEST-F1-02-09.                                              SQ2134.2
055600     READ SQ-FS2 RECORD                                           SQ2134.2
055700              AT END GO TO READ-TEST-F1-02-10.                    SQ2134.2
055800     MOVE SQ-FS2R1-F-G-120 TO FILE-RECORD-INFO-P1-120 (2)         SQ2134.2
055900     ADD 1 TO WRK-RECORD-COUNT.                                   SQ2134.2
056000     IF XFILE-NAME (2) NOT EQUAL TO "SQ-FS2"                      SQ2134.2
056100              ADD 1 TO RECORDS-IN-ERROR                           SQ2134.2
056200              GO TO READ-TEST-F1-02-09.                           SQ2134.2
056300     IF WRK-RECORD-COUNT NOT EQUAL TO XRECORD-NUMBER (2)          SQ2134.2
056400              ADD 1 TO RECORDS-IN-ERROR                           SQ2134.2
056500              GO TO READ-TEST-F1-02-09.                           SQ2134.2
056600     GO TO READ-TEST-F1-02-09.                                    SQ2134.2
056700 READ-TEST-F1-02-10.                                              SQ2134.2
056800     IF RECORDS-IN-ERROR EQUAL ZERO                               SQ2134.2
056900              GO TO READ-PASS-F1-02.                              SQ2134.2
057000     GO TO READ-FAIL-F1-02.                                       SQ2134.2
057100 READ-DELETE-F1-02.                                               SQ2134.2
057200     PERFORM DE-LETE.                                             SQ2134.2
057300     GO TO READ-WRITE-F1-02.                                      SQ2134.2
057400 READ-FAIL-F1-02.                                                 SQ2134.2
057500     MOVE "ERRORS IN READING SQ-FS2; VII-39, -52, -44" TO RE-MARK.SQ2134.2
057600     MOVE "RECORDS IN ERROR =" TO COMPUTED-A.                     SQ2134.2
057700     MOVE RECORDS-IN-ERROR TO CORRECT-18V0.                       SQ2134.2
057800     PERFORM FAIL.                                                SQ2134.2
057900     GO TO READ-WRITE-F1-02.                                      SQ2134.2
058000 READ-PASS-F1-02.                                                 SQ2134.2
058100     PERFORM PASS.                                                SQ2134.2
058200     MOVE "FILE VERIFIED RECS =" TO COMPUTED-A.                   SQ2134.2
058300     MOVE WRK-RECORD-COUNT TO CORRECT-18V0.                       SQ2134.2
058400 READ-WRITE-F1-02.                                                SQ2134.2
058500     MOVE "READ-TEST-F1-02" TO PAR-NAME.                          SQ2134.2
058600     MOVE "VERIFY FILE SQ-FS2" TO FEATURE.                        SQ2134.2
058700     PERFORM PRINT-DETAIL.                                        SQ2134.2
058800 READ-CLOSE-F1-02.                                                SQ2134.2
058900     CLOSE SQ-FS2.                                                SQ2134.2
059000 USE-INIT-GF-01.                                                  SQ2134.2
059100     MOVE "USE PROCEDURE TESTS" TO FEATURE.                       SQ2134.2
059200     MOVE "USE-TEST-GF-01" TO PAR-NAME.                           SQ2134.2
059300 USE-TEST-GF-01-01.                                               SQ2134.2
059400     IF EXTEND-ERROR EQUAL ZERO                                   SQ2134.2
059500         GO TO USE-PASS-GF-01.                                    SQ2134.2
059600     GO TO USE-FAIL-GF-01.                                        SQ2134.2
059700 USE-DELETE-GF-01.                                                SQ2134.2
059800     PERFORM DE-LETE.                                             SQ2134.2
059900     GO TO USE-WRITE-GF-01.                                       SQ2134.2
060000 USE-FAIL-GF-01.                                                  SQ2134.2
060100     MOVE "VII-50 -51; UNEXSPECTED USE PERFORMED" TO RE-MARK.     SQ2134.2
060200     MOVE "RECORDS IN ERROR =" TO COMPUTED-A.                     SQ2134.2
060300     MOVE RECORDS-IN-ERROR TO CORRECT-18V0.                       SQ2134.2
060400     PERFORM FAIL.                                                SQ2134.2
060500     GO TO USE-WRITE-GF-01.                                       SQ2134.2
060600 USE-PASS-GF-01.                                                  SQ2134.2
060700     PERFORM PASS.                                                SQ2134.2
060800     MOVE "FILE VERIFIED RECS =" TO COMPUTED-A.                   SQ2134.2
060900     MOVE WRK-RECORD-COUNT TO CORRECT-18V0.                       SQ2134.2
061000 USE-WRITE-GF-01.                                                 SQ2134.2
061100     MOVE "USE-TEST-GF-01"  TO PAR-NAME.                          SQ2134.2
061200     PERFORM PRINT-DETAIL.                                        SQ2134.2
061300 USE-TEST-GF-02.                                                  SQ2134.2
061400     IF FN-SERIES-ERROR EQUAL ZERO                                SQ2134.2
061500         GO TO USE-PASS-GF-02.                                    SQ2134.2
061600     GO TO USE-FAIL-GF-02.                                        SQ2134.2
061700 USE-DELETE-GF-02.                                                SQ2134.2
061800     PERFORM DE-LETE.                                             SQ2134.2
061900     GO TO USE-WRITE-GF-02.                                       SQ2134.2
062000 USE-FAIL-GF-02.                                                  SQ2134.2
062100     MOVE "VII-50 -51; UNEXSPECTED USE PERFORMED" TO RE-MARK.     SQ2134.2
062200     MOVE "RECORDS IN ERROR =" TO COMPUTED-A.                     SQ2134.2
062300     MOVE RECORDS-IN-ERROR TO CORRECT-18V0.                       SQ2134.2
062400     PERFORM FAIL.                                                SQ2134.2
062500     GO TO USE-WRITE-GF-02.                                       SQ2134.2
062600 USE-PASS-GF-02.                                                  SQ2134.2
062700     PERFORM PASS.                                                SQ2134.2
062800     MOVE "FILE VERIFIED RECS =" TO COMPUTED-A.                   SQ2134.2
062900     MOVE WRK-RECORD-COUNT TO CORRECT-18V0.                       SQ2134.2
063000 USE-WRITE-GF-02.                                                 SQ2134.2
063100     MOVE "USE-TEST-GF-02"  TO PAR-NAME.                          SQ2134.2
063200     PERFORM PRINT-DETAIL.                                        SQ2134.2
063300 SQ213A-END-ROUTINE.                                              SQ2134.2
063400     MOVE "END OF SQ213A VALIDATION TESTS" TO PRINT-REC.          SQ2134.2
063500     WRITE PRINT-REC AFTER ADVANCING 1 LINE.                      SQ2134.2
063600 TERMINATE-SQ213A.                                                SQ2134.2
063700     EXIT.                                                        SQ2134.2
063800 CCVS-EXIT SECTION.                                               SQ2134.2
063900 CCVS-999999.                                                     SQ2134.2
064000     GO TO CLOSE-FILES.                                           SQ2134.2
      *END-OF,SQ213A                                                                  
