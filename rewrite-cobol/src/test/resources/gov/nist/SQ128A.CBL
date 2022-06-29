      *HEADER,COBOL,SQ128A                                                            
000100 IDENTIFICATION DIVISION.                                         SQ1284.2
000200 PROGRAM-ID.                                                      SQ1284.2
000300     SQ128A.                                                      SQ1284.2
000400****************************************************************  SQ1284.2
000500*                                                              *  SQ1284.2
000600*    VALIDATION FOR:-                                          *  SQ1284.2
000700*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1284.2
000800*                                                              *  SQ1284.2
000900*    CREATION DATE     /     VALIDATION DATE                   *  SQ1284.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ1284.2
001100*                                                              *  SQ1284.2
001200*         THE ROUTINE SQ128A TESTS THE USE OF THE LEVEL 1 OPEN    SQ1284.2
001300*    SERIES AND CLOSE SERIES STATEMENTS.  INPUT AND OUTPUT CLAUSESSQ1284.2
001400*    ARE USED IN SERIES TOGETHER AND SEPARATELY.  SEVERAL FILES   SQ1284.2
001500*    ARE CREATED AND PROCESSED ON BOTH TAPE AND MASS STORAGE.     SQ1284.2
001600 ENVIRONMENT DIVISION.                                            SQ1284.2
001700 CONFIGURATION SECTION.                                           SQ1284.2
001800 SOURCE-COMPUTER.                                                 SQ1284.2
001900     XXXXX082.                                                    SQ1284.2
002000 OBJECT-COMPUTER.                                                 SQ1284.2
002100     XXXXX083.                                                    SQ1284.2
002200 INPUT-OUTPUT SECTION.                                            SQ1284.2
002300 FILE-CONTROL.                                                    SQ1284.2
002400*    SELECT RAW-DATA   ASSIGN TO                                  SQ1284.2
002500*    XXXXX062                                                     SQ1284.2
002600*           ORGANIZATION IS INDEXED                               SQ1284.2
002700*           ACCESS MODE IS RANDOM                                 SQ1284.2
002800*           RECORD KEY IS RAW-DATA-KEY.                           SQ1284.2
002900     SELECT PRINT-FILE ASSIGN TO                                  SQ1284.2
003000     XXXXX055.                                                    SQ1284.2
003100     SELECT SQ-FS1 ASSIGN TO                                      SQ1284.2
003200     XXXXX001.                                                    SQ1284.2
003300     SELECT SQ-FS2 ASSIGN TO                                      SQ1284.2
003400     XXXXX014.                                                    SQ1284.2
003500     SELECT SQ-FS3 ASSIGN TO                                      SQ1284.2
003600     XXXXX015.                                                    SQ1284.2
003700 DATA DIVISION.                                                   SQ1284.2
003800 FILE SECTION.                                                    SQ1284.2
003900*                                                                 SQ1284.2
004000*FD  RAW-DATA.                                                    SQ1284.2
004100*                                                                 SQ1284.2
004200*01  RAW-DATA-SATZ.                                               SQ1284.2
004300*    05  RAW-DATA-KEY        PIC X(6).                            SQ1284.2
004400*    05  C-DATE              PIC 9(6).                            SQ1284.2
004500*    05  C-TIME              PIC 9(8).                            SQ1284.2
004600*    05  C-NO-OF-TESTS       PIC 99.                              SQ1284.2
004700*    05  C-OK                PIC 999.                             SQ1284.2
004800*    05  C-ALL               PIC 999.                             SQ1284.2
004900*    05  C-FAIL              PIC 999.                             SQ1284.2
005000*    05  C-DELETED           PIC 999.                             SQ1284.2
005100*    05  C-INSPECT           PIC 999.                             SQ1284.2
005200*    05  C-NOTE              PIC X(13).                           SQ1284.2
005300*    05  C-INDENT            PIC X.                               SQ1284.2
005400*    05  C-ABORT             PIC X(8).                            SQ1284.2
005500 FD  PRINT-FILE                                                   SQ1284.2
005600C    LABEL RECORDS                                                SQ1284.2
005700C    XXXXX084                                                     SQ1284.2
005800C    DATA RECORD IS PRINT-REC DUMMY-RECORD                        SQ1284.2
005900               .                                                  SQ1284.2
006000 01  PRINT-REC PICTURE X(120).                                    SQ1284.2
006100 01  DUMMY-RECORD PICTURE X(120).                                 SQ1284.2
006200 FD  SQ-FS1                                                       SQ1284.2
006300C    LABEL RECORD STANDARD                                        SQ1284.2
006400     DATA RECORD IS SQ-FS1R1-F-G-120.                             SQ1284.2
006500 01  SQ-FS1R1-F-G-120 PIC X(120).                                 SQ1284.2
006600 FD  SQ-FS2                                                       SQ1284.2
006700C    LABEL RECORD STANDARD                                        SQ1284.2
006800     BLOCK CONTAINS 10 RECORDS                                    SQ1284.2
006900     DATA RECORD IS SQ-FS2R1-F-G-120.                             SQ1284.2
007000 01  SQ-FS2R1-F-G-120 PIC X(120).                                 SQ1284.2
007100 FD  SQ-FS3                                                       SQ1284.2
007200     LABEL RECORD STANDARD                                        SQ1284.2
007300     BLOCK 120 CHARACTERS                                         SQ1284.2
007400     DATA RECORD IS SQ-FS3R1-F-G-120.                             SQ1284.2
007500 01  SQ-FS3R1-F-G-120 PIC X(120).                                 SQ1284.2
007600 WORKING-STORAGE SECTION.                                         SQ1284.2
007700 01  COUNT-OF-RECS PIC 9999.                                      SQ1284.2
007800 01  FILE-RECORD-INFORMATION-REC.                                 SQ1284.2
007900     03 FILE-RECORD-INFO-SKELETON.                                SQ1284.2
008000        05 FILLER                 PICTURE X(48)       VALUE       SQ1284.2
008100             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  SQ1284.2
008200        05 FILLER                 PICTURE X(46)       VALUE       SQ1284.2
008300             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    SQ1284.2
008400        05 FILLER                 PICTURE X(26)       VALUE       SQ1284.2
008500             ",LFIL=000000,ORG=  ,LBLR= ".                        SQ1284.2
008600        05 FILLER                 PICTURE X(37)       VALUE       SQ1284.2
008700             ",RECKEY=                             ".             SQ1284.2
008800        05 FILLER                 PICTURE X(38)       VALUE       SQ1284.2
008900             ",ALTKEY1=                             ".            SQ1284.2
009000        05 FILLER                 PICTURE X(38)       VALUE       SQ1284.2
009100             ",ALTKEY2=                             ".            SQ1284.2
009200        05 FILLER                 PICTURE X(7)        VALUE SPACE.SQ1284.2
009300     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              SQ1284.2
009400        05 FILE-RECORD-INFO-P1-120.                               SQ1284.2
009500           07 FILLER              PIC X(5).                       SQ1284.2
009600           07 XFILE-NAME           PIC X(6).                      SQ1284.2
009700           07 FILLER              PIC X(8).                       SQ1284.2
009800           07 XRECORD-NAME         PIC X(6).                      SQ1284.2
009900           07 FILLER              PIC X(1).                       SQ1284.2
010000           07 REELUNIT-NUMBER     PIC 9(1).                       SQ1284.2
010100           07 FILLER              PIC X(7).                       SQ1284.2
010200           07 XRECORD-NUMBER       PIC 9(6).                      SQ1284.2
010300           07 FILLER              PIC X(6).                       SQ1284.2
010400           07 UPDATE-NUMBER       PIC 9(2).                       SQ1284.2
010500           07 FILLER              PIC X(5).                       SQ1284.2
010600           07 ODO-NUMBER          PIC 9(4).                       SQ1284.2
010700           07 FILLER              PIC X(5).                       SQ1284.2
010800           07 XPROGRAM-NAME        PIC X(5).                      SQ1284.2
010900           07 FILLER              PIC X(7).                       SQ1284.2
011000           07 XRECORD-LENGTH       PIC 9(6).                      SQ1284.2
011100           07 FILLER              PIC X(7).                       SQ1284.2
011200           07 CHARS-OR-RECORDS    PIC X(2).                       SQ1284.2
011300           07 FILLER              PIC X(1).                       SQ1284.2
011400           07 XBLOCK-SIZE          PIC 9(4).                      SQ1284.2
011500           07 FILLER              PIC X(6).                       SQ1284.2
011600           07 RECORDS-IN-FILE     PIC 9(6).                       SQ1284.2
011700           07 FILLER              PIC X(5).                       SQ1284.2
011800           07 XFILE-ORGANIZATION   PIC X(2).                      SQ1284.2
011900           07 FILLER              PIC X(6).                       SQ1284.2
012000           07 XLABEL-TYPE          PIC X(1).                      SQ1284.2
012100        05 FILE-RECORD-INFO-P121-240.                             SQ1284.2
012200           07 FILLER              PIC X(8).                       SQ1284.2
012300           07 XRECORD-KEY          PIC X(29).                     SQ1284.2
012400           07 FILLER              PIC X(9).                       SQ1284.2
012500           07 ALTERNATE-KEY1      PIC X(29).                      SQ1284.2
012600           07 FILLER              PIC X(9).                       SQ1284.2
012700           07 ALTERNATE-KEY2      PIC X(29).                      SQ1284.2
012800           07 FILLER              PIC X(7).                       SQ1284.2
012900 01  TEST-RESULTS.                                                SQ1284.2
013000     02 FILLER                    PICTURE X VALUE SPACE.          SQ1284.2
013100     02 FEATURE                   PICTURE X(20) VALUE SPACE.      SQ1284.2
013200     02 FILLER                    PICTURE X VALUE SPACE.          SQ1284.2
013300     02 P-OR-F                    PICTURE X(5) VALUE SPACE.       SQ1284.2
013400     02 FILLER                    PICTURE X  VALUE SPACE.         SQ1284.2
013500     02  PAR-NAME.                                                SQ1284.2
013600       03 FILLER PICTURE X(12) VALUE SPACE.                       SQ1284.2
013700       03  PARDOT-X PICTURE X  VALUE SPACE.                       SQ1284.2
013800       03 DOTVALUE PICTURE 99  VALUE ZERO.                        SQ1284.2
013900       03 FILLER PIC X(5) VALUE SPACE.                            SQ1284.2
014000     02 FILLER PIC X(10) VALUE SPACE.                             SQ1284.2
014100     02 RE-MARK PIC X(61).                                        SQ1284.2
014200 01  TEST-COMPUTED.                                               SQ1284.2
014300     02 FILLER PIC X(30) VALUE SPACE.                             SQ1284.2
014400     02 FILLER PIC X(17) VALUE "       COMPUTED=".                SQ1284.2
014500     02 COMPUTED-X.                                               SQ1284.2
014600     03 COMPUTED-A                PICTURE X(20) VALUE SPACE.      SQ1284.2
014700     03 COMPUTED-N REDEFINES COMPUTED-A PICTURE -9(9).9(9).       SQ1284.2
014800     03 COMPUTED-0V18 REDEFINES COMPUTED-A  PICTURE -.9(18).      SQ1284.2
014900     03 COMPUTED-4V14 REDEFINES COMPUTED-A  PICTURE -9(4).9(14).  SQ1284.2
015000     03 COMPUTED-14V4 REDEFINES COMPUTED-A  PICTURE -9(14).9(4).  SQ1284.2
015100     03       CM-18V0 REDEFINES COMPUTED-A.                       SQ1284.2
015200         04 COMPUTED-18V0                   PICTURE -9(18).       SQ1284.2
015300         04 FILLER                          PICTURE X.            SQ1284.2
015400     03 FILLER PIC X(50) VALUE SPACE.                             SQ1284.2
015500 01  TEST-CORRECT.                                                SQ1284.2
015600     02 FILLER PIC X(30) VALUE SPACE.                             SQ1284.2
015700     02 FILLER PIC X(17) VALUE "       CORRECT =".                SQ1284.2
015800     02 CORRECT-X.                                                SQ1284.2
015900     03 CORRECT-A                 PICTURE X(20) VALUE SPACE.      SQ1284.2
016000     03 CORRECT-N REDEFINES CORRECT-A PICTURE -9(9).9(9).         SQ1284.2
016100     03 CORRECT-0V18 REDEFINES CORRECT-A    PICTURE -.9(18).      SQ1284.2
016200     03 CORRECT-4V14 REDEFINES CORRECT-A    PICTURE -9(4).9(14).  SQ1284.2
016300     03 CORRECT-14V4 REDEFINES CORRECT-A    PICTURE -9(14).9(4).  SQ1284.2
016400     03      CR-18V0 REDEFINES CORRECT-A.                         SQ1284.2
016500         04 CORRECT-18V0                    PICTURE -9(18).       SQ1284.2
016600         04 FILLER                          PICTURE X.            SQ1284.2
016700     03 FILLER PIC X(50) VALUE SPACE.                             SQ1284.2
016800 01  CCVS-C-1.                                                    SQ1284.2
016900     02 FILLER PICTURE IS X(99) VALUE IS " FEATURE              PASQ1284.2
017000-    "SS  PARAGRAPH-NAME                                          SQ1284.2
017100-    "        REMARKS".                                           SQ1284.2
017200     02 FILLER PICTURE IS X(20) VALUE IS SPACE.                   SQ1284.2
017300 01  CCVS-C-2.                                                    SQ1284.2
017400     02 FILLER PICTURE IS X VALUE IS SPACE.                       SQ1284.2
017500     02 FILLER PICTURE IS X(6) VALUE IS "TESTED".                 SQ1284.2
017600     02 FILLER PICTURE IS X(15) VALUE IS SPACE.                   SQ1284.2
017700     02 FILLER PICTURE IS X(4) VALUE IS "FAIL".                   SQ1284.2
017800     02 FILLER PICTURE IS X(94) VALUE IS SPACE.                   SQ1284.2
017900 01  REC-SKL-SUB PICTURE 9(2) VALUE ZERO.                         SQ1284.2
018000 01  REC-CT PICTURE 99 VALUE ZERO.                                SQ1284.2
018100 01  DELETE-CNT                   PICTURE 999  VALUE ZERO.        SQ1284.2
018200 01  ERROR-COUNTER PICTURE IS 999 VALUE IS ZERO.                  SQ1284.2
018300 01  INSPECT-COUNTER PIC 999 VALUE ZERO.                          SQ1284.2
018400 01  PASS-COUNTER PIC 999 VALUE ZERO.                             SQ1284.2
018500 01  TOTAL-ERROR PIC 999 VALUE ZERO.                              SQ1284.2
018600 01  ERROR-HOLD PIC 999 VALUE ZERO.                               SQ1284.2
018700 01  DUMMY-HOLD PIC X(120) VALUE SPACE.                           SQ1284.2
018800 01  RECORD-COUNT PIC 9(5) VALUE ZERO.                            SQ1284.2
018900 01  CCVS-H-1.                                                    SQ1284.2
019000     02  FILLER   PICTURE X(27)  VALUE SPACE.                     SQ1284.2
019100     02 FILLER PICTURE X(67) VALUE                                SQ1284.2
019200     " FEDERAL SOFTWARE TESTING CENTER COBOL COMPILER VALIDATION  SQ1284.2
019300-    " SYSTEM".                                                   SQ1284.2
019400     02  FILLER     PICTURE X(26)  VALUE SPACE.                   SQ1284.2
019500 01  CCVS-H-2.                                                    SQ1284.2
019600     02 FILLER PICTURE X(52) VALUE IS                             SQ1284.2
019700     "CCVS85 FSTC COPY, NOT FOR DISTRIBUTION.".                   SQ1284.2
019800     02 FILLER PICTURE IS X(19) VALUE IS "TEST RESULTS SET-  ".   SQ1284.2
019900     02 TEST-ID PICTURE IS X(9).                                  SQ1284.2
020000     02 FILLER PICTURE IS X(40) VALUE IS SPACE.                   SQ1284.2
020100 01  CCVS-H-3.                                                    SQ1284.2
020200     02  FILLER PICTURE X(34) VALUE                               SQ1284.2
020300     " FOR OFFICIAL USE ONLY    ".                                SQ1284.2
020400     02  FILLER PICTURE X(58) VALUE                               SQ1284.2
020500     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ1284.2
020600     02  FILLER PICTURE X(28) VALUE                               SQ1284.2
020700     "  COPYRIGHT   1985 ".                                       SQ1284.2
020800 01  CCVS-E-1.                                                    SQ1284.2
020900     02 FILLER PICTURE IS X(52) VALUE IS SPACE.                   SQ1284.2
021000     02 FILLER PICTURE IS X(14) VALUE IS "END OF TEST-  ".        SQ1284.2
021100     02 ID-AGAIN PICTURE IS X(9).                                 SQ1284.2
021200     02 FILLER PICTURE X(45) VALUE IS                             SQ1284.2
021300     " NTIS DISTRIBUTION COBOL 85".                               SQ1284.2
021400 01  CCVS-E-2.                                                    SQ1284.2
021500     02  FILLER                   PICTURE X(31)  VALUE            SQ1284.2
021600     SPACE.                                                       SQ1284.2
021700     02  FILLER                   PICTURE X(21)  VALUE SPACE.     SQ1284.2
021800     02 CCVS-E-2-2.                                               SQ1284.2
021900         03 ERROR-TOTAL PICTURE IS XXX VALUE IS SPACE.            SQ1284.2
022000         03 FILLER PICTURE IS X VALUE IS SPACE.                   SQ1284.2
022100         03 ENDER-DESC PIC X(46) VALUE "ERRORS ENCOUNTERED".      SQ1284.2
022200 01  CCVS-E-3.                                                    SQ1284.2
022300     02  FILLER PICTURE X(22) VALUE                               SQ1284.2
022400     " FOR OFFICIAL USE ONLY".                                    SQ1284.2
022500     02  FILLER PICTURE X(12) VALUE SPACE.                        SQ1284.2
022600     02  FILLER PICTURE X(58) VALUE                               SQ1284.2
022700     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1284.2
022800     02  FILLER PICTURE X(13) VALUE SPACE.                        SQ1284.2
022900     02 FILLER PIC X(15) VALUE " COPYRIGHT 1985".                 SQ1284.2
023000 01  CCVS-E-4.                                                    SQ1284.2
023100     02 CCVS-E-4-1 PIC XXX VALUE SPACE.                           SQ1284.2
023200     02 FILLER PIC XXXX VALUE " OF ".                             SQ1284.2
023300     02 CCVS-E-4-2 PIC XXX VALUE SPACE.                           SQ1284.2
023400     02 FILLER PIC X(40) VALUE                                    SQ1284.2
023500      "  TESTS WERE EXECUTED SUCCESSFULLY".                       SQ1284.2
023600 01  XXINFO.                                                      SQ1284.2
023700     02 FILLER PIC X(30) VALUE "        *** INFORMATION  ***".    SQ1284.2
023800     02 INFO-TEXT.                                                SQ1284.2
023900     04 FILLER PIC X(20) VALUE SPACE.                             SQ1284.2
024000     04 XXCOMPUTED PIC X(20).                                     SQ1284.2
024100     04 FILLER PIC X(5) VALUE SPACE.                              SQ1284.2
024200     04 XXCORRECT PIC X(20).                                      SQ1284.2
024300 01  HYPHEN-LINE.                                                 SQ1284.2
024400     02 FILLER PICTURE IS X VALUE IS SPACE.                       SQ1284.2
024500     02 FILLER PICTURE IS X(65) VALUE IS "************************SQ1284.2
024600-    "*****************************************".                 SQ1284.2
024700     02 FILLER PICTURE IS X(54) VALUE IS "************************SQ1284.2
024800-    "******************************".                            SQ1284.2
024900 01  CCVS-PGM-ID PIC X(6) VALUE                                   SQ1284.2
025000     "SQ128A".                                                    SQ1284.2
025100 PROCEDURE DIVISION.                                              SQ1284.2
025200 CCVS1 SECTION.                                                   SQ1284.2
025300 OPEN-FILES.                                                      SQ1284.2
025400*    OPEN I-O RAW-DATA.                                           SQ1284.2
025500*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            SQ1284.2
025600*    READ RAW-DATA INVALID KEY GO TO END-E-1.                     SQ1284.2
025700*    MOVE "ABORTED " TO C-ABORT.                                  SQ1284.2
025800*    ADD 1 TO C-NO-OF-TESTS.                                      SQ1284.2
025900*    ACCEPT C-DATE  FROM DATE.                                    SQ1284.2
026000*    ACCEPT C-TIME  FROM TIME.                                    SQ1284.2
026100*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-1.             SQ1284.2
026200*END-E-1.                                                         SQ1284.2
026300*    CLOSE RAW-DATA.                                              SQ1284.2
026400     OPEN     OUTPUT PRINT-FILE.                                  SQ1284.2
026500     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   SQ1284.2
026600     MOVE    SPACE TO TEST-RESULTS.                               SQ1284.2
026700     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             SQ1284.2
026800     MOVE ZERO TO REC-SKL-SUB.                                    SQ1284.2
026900     PERFORM CCVS-INIT-FILE 9 TIMES.                              SQ1284.2
027000 CCVS-INIT-FILE.                                                  SQ1284.2
027100     ADD 1 TO REC-SKL-SUB.                                        SQ1284.2
027200     MOVE FILE-RECORD-INFO-SKELETON TO                            SQ1284.2
027300                  FILE-RECORD-INFO (REC-SKL-SUB).                 SQ1284.2
027400 CCVS-INIT-EXIT.                                                  SQ1284.2
027500     GO TO CCVS1-EXIT.                                            SQ1284.2
027600 CLOSE-FILES.                                                     SQ1284.2
027700     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   SQ1284.2
027800*    OPEN I-O RAW-DATA.                                           SQ1284.2
027900*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            SQ1284.2
028000*    READ RAW-DATA INVALID KEY GO TO END-E-2.                     SQ1284.2
028100*    MOVE "OK.     " TO C-ABORT.                                  SQ1284.2
028200*    MOVE PASS-COUNTER TO C-OK.                                   SQ1284.2
028300*    MOVE ERROR-HOLD   TO C-ALL.                                  SQ1284.2
028400*    MOVE ERROR-COUNTER TO C-FAIL.                                SQ1284.2
028500*    MOVE DELETE-CNT TO C-DELETED.                                SQ1284.2
028600*    MOVE INSPECT-COUNTER TO C-INSPECT.                           SQ1284.2
028700*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-2.             SQ1284.2
028800*END-E-2.                                                         SQ1284.2
028900*    CLOSE RAW-DATA.                                              SQ1284.2
029000 TERMINATE-CCVS.                                                  SQ1284.2
029100*    EXIT PROGRAM.                                                SQ1284.2
029200*TERMINATE-CALL.                                                  SQ1284.2
029300     STOP     RUN.                                                SQ1284.2
029400 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         SQ1284.2
029500 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           SQ1284.2
029600 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          SQ1284.2
029700 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-CNT.          SQ1284.2
029800     MOVE "****TEST DELETED****" TO RE-MARK.                      SQ1284.2
029900 PRINT-DETAIL.                                                    SQ1284.2
030000     IF REC-CT NOT EQUAL TO ZERO                                  SQ1284.2
030100             MOVE "." TO PARDOT-X                                 SQ1284.2
030200             MOVE REC-CT TO DOTVALUE.                             SQ1284.2
030300     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      SQ1284.2
030400     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               SQ1284.2
030500        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 SQ1284.2
030600          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 SQ1284.2
030700     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              SQ1284.2
030800     MOVE SPACE TO CORRECT-X.                                     SQ1284.2
030900     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         SQ1284.2
031000     MOVE     SPACE TO RE-MARK.                                   SQ1284.2
031100 HEAD-ROUTINE.                                                    SQ1284.2
031200     MOVE CCVS-H-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ1284.2
031300     MOVE CCVS-H-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.   SQ1284.2
031400     MOVE CCVS-H-3 TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.   SQ1284.2
031500 COLUMN-NAMES-ROUTINE.                                            SQ1284.2
031600     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ1284.2
031700     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ1284.2
031800     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ1284.2
031900 END-ROUTINE.                                                     SQ1284.2
032000     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.SQ1284.2
032100 END-RTN-EXIT.                                                    SQ1284.2
032200     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ1284.2
032300 END-ROUTINE-1.                                                   SQ1284.2
032400      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      SQ1284.2
032500      ERROR-HOLD. ADD DELETE-CNT TO ERROR-HOLD.                   SQ1284.2
032600      ADD PASS-COUNTER TO ERROR-HOLD.                             SQ1284.2
032700*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   SQ1284.2
032800      MOVE PASS-COUNTER TO CCVS-E-4-1.                            SQ1284.2
032900      MOVE ERROR-HOLD TO CCVS-E-4-2.                              SQ1284.2
033000      MOVE CCVS-E-4 TO CCVS-E-2-2.                                SQ1284.2
033100      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           SQ1284.2
033200  END-ROUTINE-12.                                                 SQ1284.2
033300      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        SQ1284.2
033400     IF       ERROR-COUNTER IS EQUAL TO ZERO                      SQ1284.2
033500         MOVE "NO " TO ERROR-TOTAL                                SQ1284.2
033600         ELSE                                                     SQ1284.2
033700         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       SQ1284.2
033800     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           SQ1284.2
033900     PERFORM WRITE-LINE.                                          SQ1284.2
034000 END-ROUTINE-13.                                                  SQ1284.2
034100     IF DELETE-CNT IS EQUAL TO ZERO                               SQ1284.2
034200         MOVE "NO " TO ERROR-TOTAL  ELSE                          SQ1284.2
034300         MOVE DELETE-CNT TO ERROR-TOTAL.                          SQ1284.2
034400     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   SQ1284.2
034500     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ1284.2
034600      IF   INSPECT-COUNTER EQUAL TO ZERO                          SQ1284.2
034700          MOVE "NO " TO ERROR-TOTAL                               SQ1284.2
034800      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   SQ1284.2
034900      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            SQ1284.2
035000      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          SQ1284.2
035100     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ1284.2
035200 WRITE-LINE.                                                      SQ1284.2
035300     ADD 1 TO RECORD-COUNT.                                       SQ1284.2
035400*    IF RECORD-COUNT GREATER 50                                   SQ1284.2
035500*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          SQ1284.2
035600*        MOVE SPACE TO DUMMY-RECORD                               SQ1284.2
035700*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  SQ1284.2
035800*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             SQ1284.2
035900*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     SQ1284.2
036000*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          SQ1284.2
036100*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          SQ1284.2
036200*        MOVE ZERO TO RECORD-COUNT.                               SQ1284.2
036300     PERFORM WRT-LN.                                              SQ1284.2
036400 WRT-LN.                                                          SQ1284.2
036500     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               SQ1284.2
036600     MOVE SPACE TO DUMMY-RECORD.                                  SQ1284.2
036700 BLANK-LINE-PRINT.                                                SQ1284.2
036800     PERFORM WRT-LN.                                              SQ1284.2
036900 FAIL-ROUTINE.                                                    SQ1284.2
037000     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.   SQ1284.2
037100     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.    SQ1284.2
037200     MOVE "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.    SQ1284.2
037300     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     SQ1284.2
037400     GO TO FAIL-ROUTINE-EX.                                       SQ1284.2
037500 FAIL-ROUTINE-WRITE.                                              SQ1284.2
037600     MOVE TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE           SQ1284.2
037700     MOVE TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES.   SQ1284.2
037800 FAIL-ROUTINE-EX. EXIT.                                           SQ1284.2
037900 BAIL-OUT.                                                        SQ1284.2
038000     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.       SQ1284.2
038100     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.               SQ1284.2
038200 BAIL-OUT-WRITE.                                                  SQ1284.2
038300     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  SQ1284.2
038400     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     SQ1284.2
038500 BAIL-OUT-EX. EXIT.                                               SQ1284.2
038600 CCVS1-EXIT.                                                      SQ1284.2
038700     EXIT.                                                        SQ1284.2
038800 SECT-SQ128A-0001 SECTION.                                        SQ1284.2
038900 OPEN-INIT-GF-01.                                                 SQ1284.2
039000     MOVE "SQ-FS1" TO XFILE-NAME (1).                             SQ1284.2
039100     MOVE "SQ-FS2" TO XFILE-NAME (2).                             SQ1284.2
039200     MOVE "SQ-FS3" TO XFILE-NAME (3).                             SQ1284.2
039300     MOVE "R1-F-G" TO XRECORD-NAME (1)                            SQ1284.2
039400                      XRECORD-NAME (2)                            SQ1284.2
039500                      XRECORD-NAME (3).                           SQ1284.2
039600     MOVE "SQ128A" TO XPROGRAM-NAME (1)                           SQ1284.2
039700                     XPROGRAM-NAME (2)                            SQ1284.2
039800                     XPROGRAM-NAME (3).                           SQ1284.2
039900     MOVE 000120 TO XRECORD-LENGTH (1)                            SQ1284.2
040000                    XRECORD-LENGTH (2)                            SQ1284.2
040100                    XRECORD-LENGTH (3).                           SQ1284.2
040200     MOVE "RC" TO CHARS-OR-RECORDS (1)                            SQ1284.2
040300                  CHARS-OR-RECORDS (2).                           SQ1284.2
040400     MOVE "CH" TO CHARS-OR-RECORDS (3).                           SQ1284.2
040500     MOVE 0001 TO XBLOCK-SIZE (1).                                SQ1284.2
040600     MOVE 0010 TO XBLOCK-SIZE (2).                                SQ1284.2
040700     MOVE 0120 TO XBLOCK-SIZE (3).                                SQ1284.2
040800     MOVE 0750 TO RECORDS-IN-FILE (1)                             SQ1284.2
040900                  RECORDS-IN-FILE (2)                             SQ1284.2
041000                  RECORDS-IN-FILE (3).                            SQ1284.2
041100     MOVE "SQ" TO XFILE-ORGANIZATION (1)                          SQ1284.2
041200                  XFILE-ORGANIZATION (2)                          SQ1284.2
041300                  XFILE-ORGANIZATION (3).                         SQ1284.2
041400     MOVE "S"  TO XLABEL-TYPE (1)                                 SQ1284.2
041500                  XLABEL-TYPE (2)                                 SQ1284.2
041600                  XLABEL-TYPE (3).                                SQ1284.2
041700 OPN-TEST-GF-01.                                                  SQ1284.2
041800     OPEN OUTPUT SQ-FS1                                           SQ1284.2
041900                 SQ-FS2.                                          SQ1284.2
042000     MOVE 00001 TO XRECORD-NUMBER (1)                             SQ1284.2
042100                   XRECORD-NUMBER (2).                            SQ1284.2
042200 OPN-TEST-GF-01-1.                                                SQ1284.2
042300     MOVE FILE-RECORD-INFO-P1-120 (1) TO SQ-FS1R1-F-G-120.        SQ1284.2
042400     MOVE FILE-RECORD-INFO-P1-120 (2) TO SQ-FS2R1-F-G-120.        SQ1284.2
042500     WRITE SQ-FS1R1-F-G-120.                                      SQ1284.2
042600     WRITE SQ-FS2R1-F-G-120.                                      SQ1284.2
042700     IF XRECORD-NUMBER (1) EQUAL TO 750                           SQ1284.2
042800              GO TO OPN-WRITE-GF-01.                              SQ1284.2
042900     ADD 1 TO XRECORD-NUMBER (1).                                 SQ1284.2
043000     ADD 1 TO XRECORD-NUMBER (2).                                 SQ1284.2
043100     GO TO OPN-TEST-GF-01-1.                                      SQ1284.2
043200 OPN-WRITE-GF-01.                                                 SQ1284.2
043300     MOVE "OPEN OUT  1 & 2 " TO FEATURE.                          SQ1284.2
043400     MOVE "OPN-TEST-GF-01" TO PAR-NAME.                           SQ1284.2
043500     MOVE "FILE CREATED, RECS =" TO COMPUTED-A.                   SQ1284.2
043600     MOVE XRECORD-NUMBER (1) TO CORRECT-18V0.                     SQ1284.2
043700     PERFORM PRINT-DETAIL.                                        SQ1284.2
043800     PERFORM PASS.                                                SQ1284.2
043900     MOVE "OPN-TEST-GF-02" TO PAR-NAME.                           SQ1284.2
044000     MOVE "FILE CREATED, RECS =" TO COMPUTED-A.                   SQ1284.2
044100     MOVE XRECORD-NUMBER (2) TO CORRECT-18V0.                     SQ1284.2
044200     PERFORM PRINT-DETAIL.                                        SQ1284.2
044300 CLOSE-INIT-GF-01.                                                SQ1284.2
044400*             THIS TEST CLOSES THE TWO OUTPUT FILES FROM          SQ1284.2
044500*             SEQ-TEST-001 WITH ONE CLOSE STATEMENT.              SQ1284.2
044600     CLOSE SQ-FS1,                                                SQ1284.2
044700           SQ-FS2.                                                SQ1284.2
044800 CLOSE-WRITE-GF-01.                                               SQ1284.2
044900     MOVE "CLOSE FILE 1 & 2 " TO FEATURE.                         SQ1284.2
045000     MOVE "CLOSE-TEST-GF-01" TO PAR-NAME.                         SQ1284.2
045100     MOVE SPACES TO CORRECT-A.                                    SQ1284.2
045200     PERFORM PASS.                                                SQ1284.2
045300     PERFORM PRINT-DETAIL.                                        SQ1284.2
045400 OPEN-TEST-GF-02.                                                 SQ1284.2
045500*             THIS TEST OPENS FOR INPUT THE TWO FILES CREATED IN  SQ1284.2
045600*             SEQ-TEST-001.                                       SQ1284.2
045700     OPEN INPUT SQ-FS1,                                           SQ1284.2
045800                SQ-FS2.                                           SQ1284.2
045900     MOVE "OPEN INPUT 1 & 2" TO FEATURE.                          SQ1284.2
046000     MOVE "OPEN-TEST-GF-02" TO PAR-NAME.                          SQ1284.2
046100     PERFORM PASS.                                                SQ1284.2
046200     PERFORM PRINT-DETAIL.                                        SQ1284.2
046300 READ-TEST-F1-01.                                                 SQ1284.2
046400*             THIS PART OF THE TEST READS AND VALIDATES ONE       SQ1284.2
046500*             RECORD FROM FILES SQ-FS1 AND SQ-FS2.                SQ1284.2
046600     READ SQ-FS1 AT END GO TO READ-FAIL-F1-01.                    SQ1284.2
046700     MOVE SQ-FS1R1-F-G-120 TO FILE-RECORD-INFO-P1-120 (1).        SQ1284.2
046800     IF XFILE-NAME (1) NOT EQUAL TO "SQ-FS1"                      SQ1284.2
046900              GO TO READ-FAIL-F1-01.                              SQ1284.2
047000     IF XRECORD-NUMBER (1) NOT EQUAL TO 1                         SQ1284.2
047100              GO TO READ-FAIL-F1-01.                              SQ1284.2
047200     GO TO READ-PASS-F1-01.                                       SQ1284.2
047300 READ-FAIL-F1-01.                                                 SQ1284.2
047400     MOVE "ERRORS IN READING SQ-FS1; VII-44, 4.4.2   " TO RE-MARK.SQ1284.2
047500     PERFORM FAIL.                                                SQ1284.2
047600     GO TO READ-WRITE-F1-01.                                      SQ1284.2
047700 READ-PASS-F1-01.                                                 SQ1284.2
047800     PERFORM PASS.                                                SQ1284.2
047900     MOVE "FIRST RECORD IS VALID" TO RE-MARK.                     SQ1284.2
048000 READ-WRITE-F1-01.                                                SQ1284.2
048100     MOVE "READ-TEST-F1-01" TO PAR-NAME.                          SQ1284.2
048200     MOVE "VERIFY FILE SQ-FS1" TO FEATURE.                        SQ1284.2
048300     PERFORM PRINT-DETAIL.                                        SQ1284.2
048400 READ-TEST-F1-02.                                                 SQ1284.2
048500     READ SQ-FS2 AT END GO TO READ-FAIL-F1-02.                    SQ1284.2
048600     MOVE SQ-FS2R1-F-G-120 TO FILE-RECORD-INFO-P1-120 (2).        SQ1284.2
048700     IF XFILE-NAME (2) NOT EQUAL TO "SQ-FS2"                      SQ1284.2
048800              GO TO READ-FAIL-F1-02.                              SQ1284.2
048900     IF XRECORD-NUMBER (2) NOT EQUAL TO 1                         SQ1284.2
049000              GO TO READ-FAIL-F1-02.                              SQ1284.2
049100     GO TO READ-PASS-F1-02.                                       SQ1284.2
049200 READ-FAIL-F1-02.                                                 SQ1284.2
049300     MOVE "ERRORS IN READING SQ-FS2; VII-44, 4.4.2   " TO RE-MARK.SQ1284.2
049400     PERFORM FAIL.                                                SQ1284.2
049500     GO TO READ-WRITE-F1-02.                                      SQ1284.2
049600 READ-PASS-F1-02.                                                 SQ1284.2
049700     PERFORM PASS.                                                SQ1284.2
049800     MOVE "FIRST RECORD IS VALID" TO RE-MARK.                     SQ1284.2
049900 READ-WRITE-F1-02.                                                SQ1284.2
050000     MOVE "READ-TEST-F1"  TO PAR-NAME.                            SQ1284.2
050100     MOVE "VERIFY FILE SQ-FS2" TO FEATURE.                        SQ1284.2
050200     PERFORM PRINT-DETAIL.                                        SQ1284.2
050300 OPEN-INIT-03.                                                    SQ1284.2
050400     CLOSE SQ-FS1.                                                SQ1284.2
050500 OPEN-TEST-GF-03.                                                 SQ1284.2
050600*             THIS TEST OPENS A FILE FOR INPUT AND A FILE FOR     SQ1284.2
050700*             OUTPUT WITH THE SAME OPEN STATEMENT.                SQ1284.2
050800     OPEN INPUT SQ-FS1                                            SQ1284.2
050900          OUTPUT SQ-FS3.                                          SQ1284.2
051000     MOVE 00001 TO XRECORD-NUMBER (3).                            SQ1284.2
051100 OPEN-TEST-GF-03-1.                                               SQ1284.2
051200     MOVE FILE-RECORD-INFO-P1-120 (3) TO SQ-FS3R1-F-G-120.        SQ1284.2
051300     WRITE SQ-FS3R1-F-G-120.                                      SQ1284.2
051400     IF XRECORD-NUMBER (3) EQUAL TO 750                           SQ1284.2
051500              GO TO OPEN-WRITE-GF-03.                             SQ1284.2
051600     ADD 1 TO XRECORD-NUMBER (3).                                 SQ1284.2
051700     GO TO OPEN-TEST-GF-03-1.                                     SQ1284.2
051800 OPEN-WRITE-GF-03.                                                SQ1284.2
051900     MOVE "OPEN FILE SQ-FS3" TO FEATURE.                          SQ1284.2
052000     MOVE "OPEN-TEST-GF-03" TO PAR-NAME.                          SQ1284.2
052100     MOVE "FILE CREATED, RECS =" TO COMPUTED-A.                   SQ1284.2
052200     MOVE XRECORD-NUMBER (3) TO CORRECT-18V0.                     SQ1284.2
052300     PERFORM PASS.                                                SQ1284.2
052400     PERFORM PRINT-DETAIL.                                        SQ1284.2
052500 CLOSE-TEST-02.                                                   SQ1284.2
052600*             THIS TEST CLOSES ONE OUTPUT FILE AND TWO INPUT FILESSQ1284.2
052700*             WITH ONE CLOSE STATEMENT.                           SQ1284.2
052800     CLOSE SQ-FS1,                                                SQ1284.2
052900           SQ-FS2,                                                SQ1284.2
053000           SQ-FS3.                                                SQ1284.2
053100 CLOSE-WRITE-02.                                                  SQ1284.2
053200     MOVE "CLOSE FILE SQ-FS1" TO FEATURE.                         SQ1284.2
053300     MOVE "CLOSE-TEST-02 " TO PAR-NAME.                           SQ1284.2
053400     MOVE SPACES TO CORRECT-A.                                    SQ1284.2
053500     PERFORM PASS.                                                SQ1284.2
053600     PERFORM PRINT-DETAIL.                                        SQ1284.2
053700     MOVE "CLOSE FILE SQ-FS2" TO FEATURE.                         SQ1284.2
053800     MOVE "CLOSE-TEST-02 " TO PAR-NAME.                           SQ1284.2
053900     PERFORM PASS.                                                SQ1284.2
054000     PERFORM PRINT-DETAIL.                                        SQ1284.2
054100     MOVE "CLOSE FILE SQ-FS3" TO FEATURE.                         SQ1284.2
054200     MOVE "CLOSE-TEST-02 " TO PAR-NAME.                           SQ1284.2
054300     PERFORM PASS.                                                SQ1284.2
054400     PERFORM PRINT-DETAIL.                                        SQ1284.2
054500 SQ128A-END-ROUTINE.                                              SQ1284.2
054600     MOVE " END OF SQ128A VALIDATION TESTS" TO PRINT-REC.         SQ1284.2
054700     WRITE PRINT-REC AFTER ADVANCING 1 LINE.                      SQ1284.2
054800 TERMINATE-SQ128A.                                                SQ1284.2
054900     EXIT.                                                        SQ1284.2
055000 CCVS-EXIT SECTION.                                               SQ1284.2
055100 CCVS-999999.                                                     SQ1284.2
055200     GO TO CLOSE-FILES.                                           SQ1284.2
      *END-OF,SQ128A                                                                  
