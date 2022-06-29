      *HEADER,COBOL,SQ204A                                                            
000100 IDENTIFICATION DIVISION.                                         SQ2044.2
000200 PROGRAM-ID.                                                      SQ2044.2
000300     SQ204A.                                                      SQ2044.2
000400****************************************************************  SQ2044.2
000500*                                                              *  SQ2044.2
000600*    VALIDATION FOR:-                                          *  SQ2044.2
000700*    " HIGH       ".                                              SQ2044.2
000800*                                                              *  SQ2044.2
000900*    CREATION DATE     /     VALIDATION DATE                   *  SQ2044.2
001000*    "4.2 ".                                                      SQ2044.2
001100*                                                              *  SQ2044.2
001200*        THIS ROUTINE       TESTS THE USE OF THE OPEN EXTEND      SQ2044.2
001300*    STATEMENT FOR BOTH MAGNETIC TAPE AND MASS STORAGE.  FILES    SQ2044.2
001400*    ARE FIRST CREATED USING THE NORMAL OPEN OUTPUT STATEMENT     SQ2044.2
001500*                                                                 SQ2044.2
001600 ENVIRONMENT DIVISION.                                            SQ2044.2
001700 CONFIGURATION SECTION.                                           SQ2044.2
001800 SOURCE-COMPUTER.                                                 SQ2044.2
001900     XXXXX082.                                                    SQ2044.2
002000 OBJECT-COMPUTER.                                                 SQ2044.2
002100     XXXXX083.                                                    SQ2044.2
002200 INPUT-OUTPUT SECTION.                                            SQ2044.2
002300 FILE-CONTROL.                                                    SQ2044.2
002400*    SELECT RAW-DATA   ASSIGN TO                                  SQ2044.2
002500*    XXXXX062                                                     SQ2044.2
002600*           ORGANIZATION IS INDEXED                               SQ2044.2
002700*           ACCESS MODE IS RANDOM                                 SQ2044.2
002800*           RECORD KEY IS RAW-DATA-KEY.                           SQ2044.2
002900     SELECT PRINT-FILE ASSIGN TO                                  SQ2044.2
003000     XXXXX055.                                                    SQ2044.2
003100     SELECT SQ-FS1 ASSIGN TO                                      SQ2044.2
003200     XXXXX001                                                     SQ2044.2
003300     ORGANIZATION IS SEQUENTIAL                                   SQ2044.2
003400     ACCESS MODE IS SEQUENTIAL.                                   SQ2044.2
003500     SELECT SQ-FS2 ASSIGN TO                                      SQ2044.2
003600     XXXXX014                                                     SQ2044.2
003700     ORGANIZATION IS SEQUENTIAL                                   SQ2044.2
003800     ACCESS MODE IS SEQUENTIAL.                                   SQ2044.2
003900 DATA DIVISION.                                                   SQ2044.2
004000 FILE SECTION.                                                    SQ2044.2
004100*                                                                 SQ2044.2
004200*FD  RAW-DATA.                                                    SQ2044.2
004300*                                                                 SQ2044.2
004400*01  RAW-DATA-SATZ.                                               SQ2044.2
004500*    05  RAW-DATA-KEY        PIC X(6).                            SQ2044.2
004600*    05  C-DATE              PIC 9(6).                            SQ2044.2
004700*    05  C-TIME              PIC 9(8).                            SQ2044.2
004800*    05  C-NO-OF-TESTS       PIC 99.                              SQ2044.2
004900*    05  C-OK                PIC 999.                             SQ2044.2
005000*    05  C-ALL               PIC 999.                             SQ2044.2
005100*    05  C-FAIL              PIC 999.                             SQ2044.2
005200*    05  C-DELETED           PIC 999.                             SQ2044.2
005300*    05  C-INSPECT           PIC 999.                             SQ2044.2
005400*    05  C-NOTE              PIC X(13).                           SQ2044.2
005500*    05  C-INDENT            PIC X.                               SQ2044.2
005600*    05  C-ABORT             PIC X(8).                            SQ2044.2
005700 FD  PRINT-FILE                                                   SQ2044.2
005800C    LABEL RECORDS                                                SQ2044.2
005900C    XXXXX084                                                     SQ2044.2
006000C    DATA RECORD IS PRINT-REC DUMMY-RECORD                        SQ2044.2
006100               .                                                  SQ2044.2
006200 01  PRINT-REC PICTURE X(120).                                    SQ2044.2
006300 01  DUMMY-RECORD PICTURE X(120).                                 SQ2044.2
006400 FD  SQ-FS1                                                       SQ2044.2
006500C    LABEL RECORDS ARE STANDARD                                   SQ2044.2
006600     BLOCK CONTAINS 126 CHARACTERS.                               SQ2044.2
006700 01  SQ-FS1R1-F-G-126.                                            SQ2044.2
006800     02 SQ-FS1R1-F-G-120 PIC X(120).                              SQ2044.2
006900     02 SQ-FS1R1-F-G-006 PIC X(6).                                SQ2044.2
007000 FD  SQ-FS2                                                       SQ2044.2
007100C    LABEL RECORDS ARE STANDARD                                   SQ2044.2
007200     BLOCK CONTAINS 126 CHARACTERS.                               SQ2044.2
007300 01  SQ-FS2R1-F-G-126.                                            SQ2044.2
007400     02 SQ-FS2R1-F-G-120 PIC X(120).                              SQ2044.2
007500     02 SQ-FS2R1-F-G-006 PIC X(6).                                SQ2044.2
007600 WORKING-STORAGE SECTION.                                         SQ2044.2
007700 77  RECORDS-IN-ERROR   PIC  9(4) VALUE 0.                        SQ2044.2
007800 77  WRK-RECORD-COUNT   PIC  9(4) VALUE 0.                        SQ2044.2
007900 01  COUNT-OF-RECS PIC 9999.                                      SQ2044.2
008000 01  FILE-RECORD-INFORMATION-REC.                                 SQ2044.2
008100     03 FILE-RECORD-INFO-SKELETON.                                SQ2044.2
008200        05 FILLER                 PICTURE X(48)       VALUE       SQ2044.2
008300             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  SQ2044.2
008400        05 FILLER                 PICTURE X(46)       VALUE       SQ2044.2
008500             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    SQ2044.2
008600        05 FILLER                 PICTURE X(26)       VALUE       SQ2044.2
008700             ",LFIL=000000,ORG=  ,LBLR= ".                        SQ2044.2
008800        05 FILLER                 PICTURE X(37)       VALUE       SQ2044.2
008900             ",RECKEY=                             ".             SQ2044.2
009000        05 FILLER                 PICTURE X(38)       VALUE       SQ2044.2
009100             ",ALTKEY1=                             ".            SQ2044.2
009200        05 FILLER                 PICTURE X(38)       VALUE       SQ2044.2
009300             ",ALTKEY2=                             ".            SQ2044.2
009400        05 FILLER                 PICTURE X(7)        VALUE SPACE.SQ2044.2
009500     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              SQ2044.2
009600        05 FILE-RECORD-INFO-P1-120.                               SQ2044.2
009700           07 FILLER              PIC X(5).                       SQ2044.2
009800           07 XFILE-NAME           PIC X(6).                      SQ2044.2
009900           07 FILLER              PIC X(8).                       SQ2044.2
010000           07 XRECORD-NAME         PIC X(6).                      SQ2044.2
010100           07 FILLER              PIC X(1).                       SQ2044.2
010200           07 REELUNIT-NUMBER     PIC 9(1).                       SQ2044.2
010300           07 FILLER              PIC X(7).                       SQ2044.2
010400           07 XRECORD-NUMBER       PIC 9(6).                      SQ2044.2
010500           07 FILLER              PIC X(6).                       SQ2044.2
010600           07 UPDATE-NUMBER       PIC 9(2).                       SQ2044.2
010700           07 FILLER              PIC X(5).                       SQ2044.2
010800           07 ODO-NUMBER          PIC 9(4).                       SQ2044.2
010900           07 FILLER              PIC X(5).                       SQ2044.2
011000           07 XPROGRAM-NAME        PIC X(5).                      SQ2044.2
011100           07 FILLER              PIC X(7).                       SQ2044.2
011200           07 XRECORD-LENGTH       PIC 9(6).                      SQ2044.2
011300           07 FILLER              PIC X(7).                       SQ2044.2
011400           07 CHARS-OR-RECORDS    PIC X(2).                       SQ2044.2
011500           07 FILLER              PIC X(1).                       SQ2044.2
011600           07 XBLOCK-SIZE          PIC 9(4).                      SQ2044.2
011700           07 FILLER              PIC X(6).                       SQ2044.2
011800           07 RECORDS-IN-FILE     PIC 9(6).                       SQ2044.2
011900           07 FILLER              PIC X(5).                       SQ2044.2
012000           07 XFILE-ORGANIZATION   PIC X(2).                      SQ2044.2
012100           07 FILLER              PIC X(6).                       SQ2044.2
012200           07 XLABEL-TYPE          PIC X(1).                      SQ2044.2
012300        05 FILE-RECORD-INFO-P121-240.                             SQ2044.2
012400           07 FILLER              PIC X(8).                       SQ2044.2
012500           07 XRECORD-KEY          PIC X(29).                     SQ2044.2
012600           07 FILLER              PIC X(9).                       SQ2044.2
012700           07 ALTERNATE-KEY1      PIC X(29).                      SQ2044.2
012800           07 FILLER              PIC X(9).                       SQ2044.2
012900           07 ALTERNATE-KEY2      PIC X(29).                      SQ2044.2
013000           07 FILLER              PIC X(7).                       SQ2044.2
013100 01  TEST-RESULTS.                                                SQ2044.2
013200     02 FILLER                    PICTURE X VALUE SPACE.          SQ2044.2
013300     02 FEATURE                   PICTURE X(20) VALUE SPACE.      SQ2044.2
013400     02 FILLER                    PICTURE X VALUE SPACE.          SQ2044.2
013500     02 P-OR-F                    PICTURE X(5) VALUE SPACE.       SQ2044.2
013600     02 FILLER                    PICTURE X  VALUE SPACE.         SQ2044.2
013700     02  PAR-NAME.                                                SQ2044.2
013800       03 FILLER PICTURE X(12) VALUE SPACE.                       SQ2044.2
013900       03  PARDOT-X PICTURE X  VALUE SPACE.                       SQ2044.2
014000       03 DOTVALUE PICTURE 99  VALUE ZERO.                        SQ2044.2
014100       03 FILLER PIC X(5) VALUE SPACE.                            SQ2044.2
014200     02 FILLER PIC X(10) VALUE SPACE.                             SQ2044.2
014300     02 RE-MARK PIC X(61).                                        SQ2044.2
014400 01  TEST-COMPUTED.                                               SQ2044.2
014500     02 FILLER PIC X(30) VALUE SPACE.                             SQ2044.2
014600     02 FILLER PIC X(17) VALUE "       COMPUTED=".                SQ2044.2
014700     02 COMPUTED-X.                                               SQ2044.2
014800     03 COMPUTED-A                PICTURE X(20) VALUE SPACE.      SQ2044.2
014900     03 COMPUTED-N REDEFINES COMPUTED-A PICTURE -9(9).9(9).       SQ2044.2
015000     03 COMPUTED-0V18 REDEFINES COMPUTED-A  PICTURE -.9(18).      SQ2044.2
015100     03 COMPUTED-4V14 REDEFINES COMPUTED-A  PICTURE -9(4).9(14).  SQ2044.2
015200     03 COMPUTED-14V4 REDEFINES COMPUTED-A  PICTURE -9(14).9(4).  SQ2044.2
015300     03       CM-18V0 REDEFINES COMPUTED-A.                       SQ2044.2
015400         04 COMPUTED-18V0                   PICTURE -9(18).       SQ2044.2
015500         04 FILLER                          PICTURE X.            SQ2044.2
015600     03 FILLER PIC X(50) VALUE SPACE.                             SQ2044.2
015700 01  TEST-CORRECT.                                                SQ2044.2
015800     02 FILLER PIC X(30) VALUE SPACE.                             SQ2044.2
015900     02 FILLER PIC X(17) VALUE "       CORRECT =".                SQ2044.2
016000     02 CORRECT-X.                                                SQ2044.2
016100     03 CORRECT-A                 PICTURE X(20) VALUE SPACE.      SQ2044.2
016200     03 CORRECT-N REDEFINES CORRECT-A PICTURE -9(9).9(9).         SQ2044.2
016300     03 CORRECT-0V18 REDEFINES CORRECT-A    PICTURE -.9(18).      SQ2044.2
016400     03 CORRECT-4V14 REDEFINES CORRECT-A    PICTURE -9(4).9(14).  SQ2044.2
016500     03 CORRECT-14V4 REDEFINES CORRECT-A    PICTURE -9(14).9(4).  SQ2044.2
016600     03      CR-18V0 REDEFINES CORRECT-A.                         SQ2044.2
016700         04 CORRECT-18V0                    PICTURE -9(18).       SQ2044.2
016800         04 FILLER                          PICTURE X.            SQ2044.2
016900     03 FILLER PIC X(50) VALUE SPACE.                             SQ2044.2
017000 01  CCVS-C-1.                                                    SQ2044.2
017100     02 FILLER PICTURE IS X(99) VALUE IS " FEATURE              PASQ2044.2
017200-    "SS  PARAGRAPH-NAME                                          SQ2044.2
017300-    "        REMARKS".                                           SQ2044.2
017400     02 FILLER PICTURE IS X(20) VALUE IS SPACE.                   SQ2044.2
017500 01  CCVS-C-2.                                                    SQ2044.2
017600     02 FILLER PICTURE IS X VALUE IS SPACE.                       SQ2044.2
017700     02 FILLER PICTURE IS X(6) VALUE IS "TESTED".                 SQ2044.2
017800     02 FILLER PICTURE IS X(15) VALUE IS SPACE.                   SQ2044.2
017900     02 FILLER PICTURE IS X(4) VALUE IS "FAIL".                   SQ2044.2
018000     02 FILLER PICTURE IS X(94) VALUE IS SPACE.                   SQ2044.2
018100 01  REC-SKL-SUB PICTURE 9(2) VALUE ZERO.                         SQ2044.2
018200 01  REC-CT PICTURE 99 VALUE ZERO.                                SQ2044.2
018300 01  DELETE-CNT                   PICTURE 999  VALUE ZERO.        SQ2044.2
018400 01  ERROR-COUNTER PICTURE IS 999 VALUE IS ZERO.                  SQ2044.2
018500 01  INSPECT-COUNTER PIC 999 VALUE ZERO.                          SQ2044.2
018600 01  PASS-COUNTER PIC 999 VALUE ZERO.                             SQ2044.2
018700 01  TOTAL-ERROR PIC 999 VALUE ZERO.                              SQ2044.2
018800 01  ERROR-HOLD PIC 999 VALUE ZERO.                               SQ2044.2
018900 01  DUMMY-HOLD PIC X(120) VALUE SPACE.                           SQ2044.2
019000 01  RECORD-COUNT PIC 9(5) VALUE ZERO.                            SQ2044.2
019100 01  CCVS-H-1.                                                    SQ2044.2
019200     02  FILLER   PICTURE X(27)  VALUE SPACE.                     SQ2044.2
019300     02 FILLER PICTURE X(67) VALUE                                SQ2044.2
019400     " FEDERAL SOFTWARE TESTING CENTER COBOL COMPILER VALIDATION  SQ2044.2
019500-    " SYSTEM".                                                   SQ2044.2
019600     02  FILLER     PICTURE X(26)  VALUE SPACE.                   SQ2044.2
019700 01  CCVS-H-2.                                                    SQ2044.2
019800     02 FILLER PICTURE X(52) VALUE IS                             SQ2044.2
019900     "CCVS85 FSTC COPY, NOT FOR DISTRIBUTION.".                   SQ2044.2
020000     02 FILLER PICTURE IS X(19) VALUE IS "TEST RESULTS SET-  ".   SQ2044.2
020100     02 TEST-ID PICTURE IS X(9).                                  SQ2044.2
020200     02 FILLER PICTURE IS X(40) VALUE IS SPACE.                   SQ2044.2
020300 01  CCVS-H-3.                                                    SQ2044.2
020400     02  FILLER PICTURE X(34) VALUE                               SQ2044.2
020500     " FOR OFFICIAL USE ONLY    ".                                SQ2044.2
020600     02  FILLER PICTURE X(58) VALUE                               SQ2044.2
020700     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ2044.2
020800     02  FILLER PICTURE X(28) VALUE                               SQ2044.2
020900     "  COPYRIGHT   1985 ".                                       SQ2044.2
021000 01  CCVS-E-1.                                                    SQ2044.2
021100     02 FILLER PICTURE IS X(52) VALUE IS SPACE.                   SQ2044.2
021200     02 FILLER PICTURE IS X(14) VALUE IS "END OF TEST-  ".        SQ2044.2
021300     02 ID-AGAIN PICTURE IS X(9).                                 SQ2044.2
021400     02 FILLER PICTURE X(45) VALUE IS                             SQ2044.2
021500     " NTIS DISTRIBUTION COBOL 85".                               SQ2044.2
021600 01  CCVS-E-2.                                                    SQ2044.2
021700     02  FILLER                   PICTURE X(31)  VALUE            SQ2044.2
021800     SPACE.                                                       SQ2044.2
021900     02  FILLER                   PICTURE X(21)  VALUE SPACE.     SQ2044.2
022000     02 CCVS-E-2-2.                                               SQ2044.2
022100         03 ERROR-TOTAL PICTURE IS XXX VALUE IS SPACE.            SQ2044.2
022200         03 FILLER PICTURE IS X VALUE IS SPACE.                   SQ2044.2
022300         03 ENDER-DESC PIC X(46) VALUE "ERRORS ENCOUNTERED".      SQ2044.2
022400 01  CCVS-E-3.                                                    SQ2044.2
022500     02  FILLER PICTURE X(22) VALUE                               SQ2044.2
022600     " FOR OFFICIAL USE ONLY".                                    SQ2044.2
022700     02  FILLER PICTURE X(12) VALUE SPACE.                        SQ2044.2
022800     02  FILLER PICTURE X(58) VALUE                               SQ2044.2
022900     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ2044.2
023000     02  FILLER PICTURE X(13) VALUE SPACE.                        SQ2044.2
023100     02 FILLER PIC X(15) VALUE " COPYRIGHT 1985".                 SQ2044.2
023200 01  CCVS-E-4.                                                    SQ2044.2
023300     02 CCVS-E-4-1 PIC XXX VALUE SPACE.                           SQ2044.2
023400     02 FILLER PIC XXXX VALUE " OF ".                             SQ2044.2
023500     02 CCVS-E-4-2 PIC XXX VALUE SPACE.                           SQ2044.2
023600     02 FILLER PIC X(40) VALUE                                    SQ2044.2
023700      "  TESTS WERE EXECUTED SUCCESSFULLY".                       SQ2044.2
023800 01  XXINFO.                                                      SQ2044.2
023900     02 FILLER PIC X(30) VALUE "        *** INFORMATION  ***".    SQ2044.2
024000     02 INFO-TEXT.                                                SQ2044.2
024100     04 FILLER PIC X(20) VALUE SPACE.                             SQ2044.2
024200     04 XXCOMPUTED PIC X(20).                                     SQ2044.2
024300     04 FILLER PIC X(5) VALUE SPACE.                              SQ2044.2
024400     04 XXCORRECT PIC X(20).                                      SQ2044.2
024500 01  HYPHEN-LINE.                                                 SQ2044.2
024600     02 FILLER PICTURE IS X VALUE IS SPACE.                       SQ2044.2
024700     02 FILLER PICTURE IS X(65) VALUE IS "************************SQ2044.2
024800-    "*****************************************".                 SQ2044.2
024900     02 FILLER PICTURE IS X(54) VALUE IS "************************SQ2044.2
025000-    "******************************".                            SQ2044.2
025100 01  CCVS-PGM-ID PIC X(6) VALUE                                   SQ2044.2
025200     "SQ204A".                                                    SQ2044.2
025300 PROCEDURE DIVISION.                                              SQ2044.2
025400 CCVS1 SECTION.                                                   SQ2044.2
025500 OPEN-FILES.                                                      SQ2044.2
025600*    OPEN I-O RAW-DATA.                                           SQ2044.2
025700*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            SQ2044.2
025800*    READ RAW-DATA INVALID KEY GO TO END-E-1.                     SQ2044.2
025900*    MOVE "ABORTED " TO C-ABORT.                                  SQ2044.2
026000*    ADD 1 TO C-NO-OF-TESTS.                                      SQ2044.2
026100*    ACCEPT C-DATE  FROM DATE.                                    SQ2044.2
026200*    ACCEPT C-TIME  FROM TIME.                                    SQ2044.2
026300*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-1.             SQ2044.2
026400*END-E-1.                                                         SQ2044.2
026500*    CLOSE RAW-DATA.                                              SQ2044.2
026600     OPEN     OUTPUT PRINT-FILE.                                  SQ2044.2
026700     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   SQ2044.2
026800     MOVE    SPACE TO TEST-RESULTS.                               SQ2044.2
026900     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             SQ2044.2
027000     MOVE ZERO TO REC-SKL-SUB.                                    SQ2044.2
027100     PERFORM CCVS-INIT-FILE 9 TIMES.                              SQ2044.2
027200 CCVS-INIT-FILE.                                                  SQ2044.2
027300     ADD 1 TO REC-SKL-SUB.                                        SQ2044.2
027400     MOVE FILE-RECORD-INFO-SKELETON TO                            SQ2044.2
027500                  FILE-RECORD-INFO (REC-SKL-SUB).                 SQ2044.2
027600 CCVS-INIT-EXIT.                                                  SQ2044.2
027700     GO TO CCVS1-EXIT.                                            SQ2044.2
027800 CLOSE-FILES.                                                     SQ2044.2
027900     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   SQ2044.2
028000*    OPEN I-O RAW-DATA.                                           SQ2044.2
028100*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            SQ2044.2
028200*    READ RAW-DATA INVALID KEY GO TO END-E-2.                     SQ2044.2
028300*    MOVE "OK.     " TO C-ABORT.                                  SQ2044.2
028400*    MOVE PASS-COUNTER TO C-OK.                                   SQ2044.2
028500*    MOVE ERROR-HOLD   TO C-ALL.                                  SQ2044.2
028600*    MOVE ERROR-COUNTER TO C-FAIL.                                SQ2044.2
028700*    MOVE DELETE-CNT TO C-DELETED.                                SQ2044.2
028800*    MOVE INSPECT-COUNTER TO C-INSPECT.                           SQ2044.2
028900*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-2.             SQ2044.2
029000*END-E-2.                                                         SQ2044.2
029100*    CLOSE RAW-DATA.                                              SQ2044.2
029200 TERMINATE-CCVS.                                                  SQ2044.2
029300*    EXIT PROGRAM.                                                SQ2044.2
029400*TERMINATE-CALL.                                                  SQ2044.2
029500     STOP     RUN.                                                SQ2044.2
029600 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         SQ2044.2
029700 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           SQ2044.2
029800 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          SQ2044.2
029900 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-CNT.          SQ2044.2
030000     MOVE "****TEST DELETED****" TO RE-MARK.                      SQ2044.2
030100 PRINT-DETAIL.                                                    SQ2044.2
030200     IF REC-CT NOT EQUAL TO ZERO                                  SQ2044.2
030300             MOVE "." TO PARDOT-X                                 SQ2044.2
030400             MOVE REC-CT TO DOTVALUE.                             SQ2044.2
030500     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      SQ2044.2
030600     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               SQ2044.2
030700        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 SQ2044.2
030800          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 SQ2044.2
030900     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              SQ2044.2
031000     MOVE SPACE TO CORRECT-X.                                     SQ2044.2
031100     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         SQ2044.2
031200     MOVE     SPACE TO RE-MARK.                                   SQ2044.2
031300 HEAD-ROUTINE.                                                    SQ2044.2
031400     MOVE CCVS-H-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ2044.2
031500     MOVE CCVS-H-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.   SQ2044.2
031600     MOVE CCVS-H-3 TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.   SQ2044.2
031700 COLUMN-NAMES-ROUTINE.                                            SQ2044.2
031800     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ2044.2
031900     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ2044.2
032000     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ2044.2
032100 END-ROUTINE.                                                     SQ2044.2
032200     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.SQ2044.2
032300 END-RTN-EXIT.                                                    SQ2044.2
032400     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ2044.2
032500 END-ROUTINE-1.                                                   SQ2044.2
032600      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      SQ2044.2
032700      ERROR-HOLD. ADD DELETE-CNT TO ERROR-HOLD.                   SQ2044.2
032800      ADD PASS-COUNTER TO ERROR-HOLD.                             SQ2044.2
032900*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   SQ2044.2
033000      MOVE PASS-COUNTER TO CCVS-E-4-1.                            SQ2044.2
033100      MOVE ERROR-HOLD TO CCVS-E-4-2.                              SQ2044.2
033200      MOVE CCVS-E-4 TO CCVS-E-2-2.                                SQ2044.2
033300      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           SQ2044.2
033400  END-ROUTINE-12.                                                 SQ2044.2
033500      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        SQ2044.2
033600     IF       ERROR-COUNTER IS EQUAL TO ZERO                      SQ2044.2
033700         MOVE "NO " TO ERROR-TOTAL                                SQ2044.2
033800         ELSE                                                     SQ2044.2
033900         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       SQ2044.2
034000     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           SQ2044.2
034100     PERFORM WRITE-LINE.                                          SQ2044.2
034200 END-ROUTINE-13.                                                  SQ2044.2
034300     IF DELETE-CNT IS EQUAL TO ZERO                               SQ2044.2
034400         MOVE "NO " TO ERROR-TOTAL  ELSE                          SQ2044.2
034500         MOVE DELETE-CNT TO ERROR-TOTAL.                          SQ2044.2
034600     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   SQ2044.2
034700     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ2044.2
034800      IF   INSPECT-COUNTER EQUAL TO ZERO                          SQ2044.2
034900          MOVE "NO " TO ERROR-TOTAL                               SQ2044.2
035000      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   SQ2044.2
035100      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            SQ2044.2
035200      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          SQ2044.2
035300     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ2044.2
035400 WRITE-LINE.                                                      SQ2044.2
035500     ADD 1 TO RECORD-COUNT.                                       SQ2044.2
035600*    IF RECORD-COUNT GREATER 50                                   SQ2044.2
035700*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          SQ2044.2
035800*        MOVE SPACE TO DUMMY-RECORD                               SQ2044.2
035900*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  SQ2044.2
036000*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             SQ2044.2
036100*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     SQ2044.2
036200*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          SQ2044.2
036300*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          SQ2044.2
036400*        MOVE ZERO TO RECORD-COUNT.                               SQ2044.2
036500     PERFORM WRT-LN.                                              SQ2044.2
036600 WRT-LN.                                                          SQ2044.2
036700     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               SQ2044.2
036800     MOVE SPACE TO DUMMY-RECORD.                                  SQ2044.2
036900 BLANK-LINE-PRINT.                                                SQ2044.2
037000     PERFORM WRT-LN.                                              SQ2044.2
037100 FAIL-ROUTINE.                                                    SQ2044.2
037200     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.   SQ2044.2
037300     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.    SQ2044.2
037400     MOVE "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.    SQ2044.2
037500     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     SQ2044.2
037600     GO TO FAIL-ROUTINE-EX.                                       SQ2044.2
037700 FAIL-ROUTINE-WRITE.                                              SQ2044.2
037800     MOVE TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE           SQ2044.2
037900     MOVE TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES.   SQ2044.2
038000 FAIL-ROUTINE-EX. EXIT.                                           SQ2044.2
038100 BAIL-OUT.                                                        SQ2044.2
038200     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.       SQ2044.2
038300     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.               SQ2044.2
038400 BAIL-OUT-WRITE.                                                  SQ2044.2
038500     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  SQ2044.2
038600     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     SQ2044.2
038700 BAIL-OUT-EX. EXIT.                                               SQ2044.2
038800 CCVS1-EXIT.                                                      SQ2044.2
038900     EXIT.                                                        SQ2044.2
039000 SECT-SQ204A-0001 SECTION.                                        SQ2044.2
039100 WRITE-INIT-GF-01.                                                SQ2044.2
039200*             THIS IS A TEST FOR OPEN EXTEND FOR MAGNETIC TAPE.   SQ2044.2
039300*             A FILE OF 750 RECORDS IS CREATED THEN RE-OPENED     SQ2044.2
039400*             WITH EXTEND.  250 RECORDS ARE ADDED TO THE FILE.    SQ2044.2
039500*             THE FILE IS THEN READ AND VALIDATED.                SQ2044.2
039600     MOVE "SQ-FS1" TO XFILE-NAME (1).                             SQ2044.2
039700     MOVE "R1-F-G" TO XRECORD-NAME (1).                           SQ2044.2
039800     MOVE "SQ204A" TO XPROGRAM-NAME (1).                          SQ2044.2
039900     MOVE 000126  TO XRECORD-LENGTH (1).                          SQ2044.2
040000     MOVE "RC"    TO CHARS-OR-RECORDS (1).                        SQ2044.2
040100     MOVE 0001    TO XBLOCK-SIZE (1).                             SQ2044.2
040200     MOVE 001000  TO RECORDS-IN-FILE (1).                         SQ2044.2
040300     MOVE "SQ"    TO XFILE-ORGANIZATION (1).                      SQ2044.2
040400     MOVE "S"     TO XLABEL-TYPE (1).                             SQ2044.2
040500     MOVE 000001  TO XRECORD-NUMBER (1).                          SQ2044.2
040600     OPEN OUTPUT SQ-FS1.                                          SQ2044.2
040700 WRITE-TEST-001-01.                                               SQ2044.2
040800     MOVE FILE-RECORD-INFO-P1-120 (1) TO SQ-FS1R1-F-G-120.        SQ2044.2
040900     MOVE SPACES TO SQ-FS1R1-F-G-006.                             SQ2044.2
041000     WRITE SQ-FS1R1-F-G-126.                                      SQ2044.2
041100     IF XRECORD-NUMBER (1) EQUAL TO 750                           SQ2044.2
041200              GO TO WRITE-TEST-GF-01-1.                           SQ2044.2
041300     ADD 1 TO XRECORD-NUMBER (1).                                 SQ2044.2
041400     GO TO WRITE-TEST-001-01.                                     SQ2044.2
041500 WRITE-TEST-GF-01-1.                                              SQ2044.2
041600     MOVE "CREATE FILE SQ-FS1" TO FEATURE.                        SQ2044.2
041700     MOVE "WRITE-TEST-GF-01-1" TO PAR-NAME.                       SQ2044.2
041800     MOVE "FILE CREATED, RECS =" TO COMPUTED-A.                   SQ2044.2
041900     MOVE XRECORD-NUMBER (1) TO CORRECT-18V0.                     SQ2044.2
042000     PERFORM PRINT-DETAIL.                                        SQ2044.2
042100     CLOSE SQ-FS1.                                                SQ2044.2
042200 WRITE-TEST-001-03.                                               SQ2044.2
042300     OPEN EXTEND SQ-FS1.                                          SQ2044.2
042400     ADD 1 TO XRECORD-NUMBER (1).                                 SQ2044.2
042500 WRITE-TEST-001-04.                                               SQ2044.2
042600     MOVE FILE-RECORD-INFO-P1-120 (1) TO SQ-FS1R1-F-G-120.        SQ2044.2
042700     MOVE "EXTEND" TO SQ-FS1R1-F-G-006.                           SQ2044.2
042800     WRITE SQ-FS1R1-F-G-126.                                      SQ2044.2
042900     IF XRECORD-NUMBER (1) EQUAL 1000                             SQ2044.2
043000              GO TO WRITE-TEST-GF-01-2.                           SQ2044.2
043100     ADD 1 TO XRECORD-NUMBER (1).                                 SQ2044.2
043200     GO TO WRITE-TEST-001-04.                                     SQ2044.2
043300 WRITE-TEST-GF-01-2.                                              SQ2044.2
043400     MOVE "CREATE FILE SQ-FS1" TO FEATURE.                        SQ2044.2
043500     MOVE "WRITE-TEST-GF-01-2" TO PAR-NAME.                       SQ2044.2
043600     MOVE "FILE EXTENDED, RECS =" TO COMPUTED-A.                  SQ2044.2
043700     MOVE XRECORD-NUMBER (1) TO CORRECT-18V0.                     SQ2044.2
043800     PERFORM PRINT-DETAIL.                                        SQ2044.2
043900     CLOSE SQ-FS1.                                                SQ2044.2
044000 READ-TEST-001-06.                                                SQ2044.2
044100     OPEN INPUT SQ-FS1.                                           SQ2044.2
044200     MOVE ZERO TO WRK-RECORD-COUNT.                               SQ2044.2
044300 READ-TEST-GF-01-07.                                              SQ2044.2
044400     READ SQ-FS1                                                  SQ2044.2
044500              ; AT END  MOVE "PREMATURE EOF" TO RE-MARK           SQ2044.2
044600                        PERFORM FAIL                              SQ2044.2
044700                        GO TO READ-WRITE-GF-01.                   SQ2044.2
044800     MOVE SQ-FS1R1-F-G-120 TO FILE-RECORD-INFO-P1-120 (1).        SQ2044.2
044900     ADD 1 TO WRK-RECORD-COUNT.                                   SQ2044.2
045000     IF XFILE-NAME (1) NOT EQUAL TO "SQ-FS1"                      SQ2044.2
045100              ADD 1 TO RECORDS-IN-ERROR                           SQ2044.2
045200              GO TO READ-TEST-GF-01-08.                           SQ2044.2
045300     IF WRK-RECORD-COUNT NOT EQUAL TO XRECORD-NUMBER (1)          SQ2044.2
045400              ADD 1 TO RECORDS-IN-ERROR                           SQ2044.2
045500              GO TO READ-TEST-GF-01-08.                           SQ2044.2
045600     IF SQ-FS1R1-F-G-006 NOT EQUAL TO SPACES                      SQ2044.2
045700              ADD 1 TO RECORDS-IN-ERROR.                          SQ2044.2
045800 READ-TEST-GF-01-08.                                              SQ2044.2
045900     IF WRK-RECORD-COUNT NOT EQUAL TO 750                         SQ2044.2
046000              GO TO READ-TEST-GF-01-07.                           SQ2044.2
046100 READ-TEST-GF-01-09.                                              SQ2044.2
046200     READ SQ-FS1 RECORD                                           SQ2044.2
046300              ; END GO TO READ-TEST-GF-01.                        SQ2044.2
046400     MOVE SQ-FS1R1-F-G-120 TO FILE-RECORD-INFO-P1-120 (1).        SQ2044.2
046500     ADD 1 TO WRK-RECORD-COUNT.                                   SQ2044.2
046600     IF XFILE-NAME (1) NOT EQUAL TO "SQ-FS1"                      SQ2044.2
046700              ADD 1 TO RECORDS-IN-ERROR                           SQ2044.2
046800              GO TO READ-TEST-GF-01-09.                           SQ2044.2
046900     IF WRK-RECORD-COUNT NOT EQUAL TO XRECORD-NUMBER (1)          SQ2044.2
047000              ADD 1 TO RECORDS-IN-ERROR                           SQ2044.2
047100              GO TO READ-TEST-GF-01-09.                           SQ2044.2
047200     IF SQ-FS1R1-F-G-006 NOT EQUAL TO "EXTEND"                    SQ2044.2
047300              ADD 1 TO RECORDS-IN-ERROR.                          SQ2044.2
047400     GO TO READ-TEST-GF-01-09.                                    SQ2044.2
047500 READ-TEST-GF-01.                                                 SQ2044.2
047600     IF RECORDS-IN-ERROR EQUAL ZERO                               SQ2044.2
047700              GO TO READ-PASS-GF-01.                              SQ2044.2
047800     MOVE "ERRORS IN READING SQ-FS1" TO RE-MARK.                  SQ2044.2
047900     GO TO READ-FAIL-GF-01.                                       SQ2044.2
048000 READ-DELETE-GF-01.                                               SQ2044.2
048100     PERFORM DE-LETE.                                             SQ2044.2
048200     GO TO READ-WRITE-GF-01.                                      SQ2044.2
048300 READ-FAIL-GF-01.                                                 SQ2044.2
048400     MOVE "VII-44 READ OR VII-52 WRITE INCORRECTLY EXECUTED"      SQ2044.2
048500        TO RE-MARK.                                               SQ2044.2
048600     MOVE "RECORDS IN ERROR =" TO COMPUTED-A.                     SQ2044.2
048700     MOVE RECORDS-IN-ERROR TO CORRECT-18V0.                       SQ2044.2
048800     PERFORM FAIL.                                                SQ2044.2
048900     GO TO READ-WRITE-GF-01.                                      SQ2044.2
049000 READ-PASS-GF-01.                                                 SQ2044.2
049100     PERFORM PASS.                                                SQ2044.2
049200     MOVE "FILE VERIFIED RECS =" TO COMPUTED-A.                   SQ2044.2
049300     MOVE WRK-RECORD-COUNT TO CORRECT-18V0.                       SQ2044.2
049400 READ-WRITE-GF-01.                                                SQ2044.2
049500     MOVE "READ-TEST-GF-01" TO PAR-NAME.                          SQ2044.2
049600     MOVE "VERIFY FILE SQ-FS1" TO FEATURE.                        SQ2044.2
049700     PERFORM PRINT-DETAIL.                                        SQ2044.2
049800 READ-CLOSE-GF-01.                                                SQ2044.2
049900     CLOSE SQ-FS1.                                                SQ2044.2
050000 WRITE-INIT-GF-02.                                                SQ2044.2
050100*             THIS IS A TEST FOR OPEN EXTEND FOR MASS STORAGE.    SQ2044.2
050200*             A FILE OF 750 RECORDS IS CREATED THEN RE-OPENED     SQ2044.2
050300*             WITH EXTEND.  250 RECORDS ARE ADDED TO THE FILE.    SQ2044.2
050400*             THE FILE IS THEN READ AND VALIDATED.                SQ2044.2
050500     MOVE "SQ-FS2" TO XFILE-NAME (2).                             SQ2044.2
050600     MOVE "R1-F-G" TO XRECORD-NAME (2).                           SQ2044.2
050700     MOVE "SQ204A" TO XPROGRAM-NAME (2).                          SQ2044.2
050800     MOVE 000126  TO XRECORD-LENGTH (2).                          SQ2044.2
050900     MOVE "RC"    TO CHARS-OR-RECORDS (2).                        SQ2044.2
051000     MOVE 0001    TO XBLOCK-SIZE (2).                             SQ2044.2
051100     MOVE 001000  TO RECORDS-IN-FILE (2).                         SQ2044.2
051200     MOVE "SQ"    TO XFILE-ORGANIZATION (2).                      SQ2044.2
051300     MOVE "S"     TO XLABEL-TYPE (2).                             SQ2044.2
051400     MOVE 000001  TO XRECORD-NUMBER (2).                          SQ2044.2
051500     OPEN OUTPUT SQ-FS2.                                          SQ2044.2
051600 WRITE-TEST-GF-02-01.                                             SQ2044.2
051700     MOVE FILE-RECORD-INFO-P1-120 (2) TO SQ-FS2R1-F-G-120.        SQ2044.2
051800     MOVE SPACES TO SQ-FS2R1-F-G-006.                             SQ2044.2
051900     WRITE SQ-FS2R1-F-G-126.                                      SQ2044.2
052000     IF XRECORD-NUMBER (2) EQUAL TO 750                           SQ2044.2
052100              GO TO WRITE-TEST-GF-02-1.                           SQ2044.2
052200     ADD 1 TO XRECORD-NUMBER (2).                                 SQ2044.2
052300     GO TO WRITE-TEST-GF-02-01.                                   SQ2044.2
052400 WRITE-TEST-GF-02-1.                                              SQ2044.2
052500     MOVE "CREATE FILE SQ-FS2" TO FEATURE.                        SQ2044.2
052600     MOVE "WRITE-TEST-GF-02-1" TO PAR-NAME.                       SQ2044.2
052700     MOVE "FILE CREATED, RECS =" TO COMPUTED-A.                   SQ2044.2
052800     MOVE XRECORD-NUMBER (2) TO CORRECT-18V0.                     SQ2044.2
052900     PERFORM PRINT-DETAIL.                                        SQ2044.2
053000     CLOSE SQ-FS2.                                                SQ2044.2
053100 WRITE-TEST-GF-02-03.                                             SQ2044.2
053200     OPEN EXTEND SQ-FS2.                                          SQ2044.2
053300     ADD 1 TO XRECORD-NUMBER (2).                                 SQ2044.2
053400 WRITE-TEST-GF-02-04.                                             SQ2044.2
053500     MOVE FILE-RECORD-INFO-P1-120 (2) TO SQ-FS2R1-F-G-120.        SQ2044.2
053600     MOVE "EXTEND" TO SQ-FS2R1-F-G-006.                           SQ2044.2
053700     WRITE SQ-FS2R1-F-G-126.                                      SQ2044.2
053800     IF XRECORD-NUMBER (2) EQUAL 1000                             SQ2044.2
053900              GO TO WRITE-TEST-GF-02-2.                           SQ2044.2
054000     ADD 1 TO XRECORD-NUMBER (2).                                 SQ2044.2
054100     GO TO WRITE-TEST-GF-02-04.                                   SQ2044.2
054200 WRITE-TEST-GF-02-2.                                              SQ2044.2
054300     MOVE "CREATE FILE SQ-FS2" TO FEATURE.                        SQ2044.2
054400     MOVE "WRITE-TEST-GF-02-2" TO PAR-NAME.                       SQ2044.2
054500     MOVE "FILE EXTENDED, RECS =" TO COMPUTED-A.                  SQ2044.2
054600     MOVE XRECORD-NUMBER (2) TO CORRECT-18V0.                     SQ2044.2
054700     PERFORM PRINT-DETAIL.                                        SQ2044.2
054800     CLOSE SQ-FS2.                                                SQ2044.2
054900 READ-TEST-GF-02-06.                                              SQ2044.2
055000     OPEN INPUT SQ-FS2.                                           SQ2044.2
055100     MOVE ZERO TO WRK-RECORD-COUNT.                               SQ2044.2
055200     MOVE ZERO TO RECORDS-IN-ERROR.                               SQ2044.2
055300 READ-TEST-GF-02-07.                                              SQ2044.2
055400     READ SQ-FS2                                                  SQ2044.2
055500              AT END MOVE "PREMATURE EOF" TO RE-MARK              SQ2044.2
055600                     PERFORM FAIL                                 SQ2044.2
055700                     GO TO READ-WRITE-GF-02.                      SQ2044.2
055800     MOVE SQ-FS2R1-F-G-120 TO FILE-RECORD-INFO-P1-120 (2).        SQ2044.2
055900     ADD 1 TO WRK-RECORD-COUNT.                                   SQ2044.2
056000     IF XFILE-NAME (2) NOT EQUAL TO "SQ-FS2"                      SQ2044.2
056100              ADD 1 TO RECORDS-IN-ERROR                           SQ2044.2
056200              GO TO READ-TEST-GF-02-08.                           SQ2044.2
056300     IF WRK-RECORD-COUNT NOT EQUAL TO XRECORD-NUMBER (2)          SQ2044.2
056400              ADD 1 TO RECORDS-IN-ERROR                           SQ2044.2
056500              GO TO READ-TEST-GF-02-08.                           SQ2044.2
056600     IF SQ-FS2R1-F-G-006 NOT EQUAL TO SPACES                      SQ2044.2
056700              ADD 1 TO RECORDS-IN-ERROR.                          SQ2044.2
056800 READ-TEST-GF-02-08.                                              SQ2044.2
056900     IF WRK-RECORD-COUNT NOT EQUAL TO 750                         SQ2044.2
057000              GO TO READ-TEST-GF-02-07.                           SQ2044.2
057100 READ-TEST-GF-02-09.                                              SQ2044.2
057200     READ SQ-FS2 RECORD                                           SQ2044.2
057300              AT END GO TO READ-TEST-GF-02.                       SQ2044.2
057400     MOVE SQ-FS2R1-F-G-120 TO FILE-RECORD-INFO-P1-120 (2)         SQ2044.2
057500     ADD 1 TO WRK-RECORD-COUNT.                                   SQ2044.2
057600     IF XFILE-NAME (2) NOT EQUAL TO "SQ-FS2"                      SQ2044.2
057700              ADD 1 TO RECORDS-IN-ERROR                           SQ2044.2
057800              GO TO READ-TEST-GF-02-09.                           SQ2044.2
057900     IF WRK-RECORD-COUNT NOT EQUAL TO XRECORD-NUMBER (2)          SQ2044.2
058000              ADD 1 TO RECORDS-IN-ERROR                           SQ2044.2
058100              GO TO READ-TEST-GF-02-09.                           SQ2044.2
058200     IF SQ-FS2R1-F-G-006 NOT EQUAL TO "EXTEND"                    SQ2044.2
058300              ADD 1 TO RECORDS-IN-ERROR.                          SQ2044.2
058400     GO TO READ-TEST-GF-02-09.                                    SQ2044.2
058500 READ-TEST-GF-02.                                                 SQ2044.2
058600     IF RECORDS-IN-ERROR EQUAL ZERO                               SQ2044.2
058700              GO TO READ-PASS-GF-02.                              SQ2044.2
058800     MOVE "ERRORS IN READING SQ-FS2" TO RE-MARK.                  SQ2044.2
058900     GO TO READ-FAIL-GF-02.                                       SQ2044.2
059000 READ-DELETE-GF-02.                                               SQ2044.2
059100     PERFORM DE-LETE.                                             SQ2044.2
059200     GO TO READ-WRITE-GF-02.                                      SQ2044.2
059300 READ-FAIL-GF-02.                                                 SQ2044.2
059400     MOVE "VII-44 READ OR VII-52 WRITE INCORRECTLY EXECUTED"      SQ2044.2
059500        TO RE-MARK.                                               SQ2044.2
059600     MOVE "RECORDS IN ERROR =" TO COMPUTED-A.                     SQ2044.2
059700     MOVE RECORDS-IN-ERROR TO CORRECT-18V0.                       SQ2044.2
059800     PERFORM FAIL.                                                SQ2044.2
059900     GO TO READ-WRITE-GF-02.                                      SQ2044.2
060000 READ-PASS-GF-02.                                                 SQ2044.2
060100     PERFORM PASS.                                                SQ2044.2
060200     MOVE "FILE VERIFIED RECS =" TO COMPUTED-A.                   SQ2044.2
060300     MOVE WRK-RECORD-COUNT TO CORRECT-18V0.                       SQ2044.2
060400 READ-WRITE-GF-02.                                                SQ2044.2
060500     MOVE "READ-TEST-GF-02" TO PAR-NAME.                          SQ2044.2
060600     MOVE "VERIFY FILE SQ-FS2" TO FEATURE.                        SQ2044.2
060700     PERFORM PRINT-DETAIL.                                        SQ2044.2
060800 READ-CLOSE-GF-02.                                                SQ2044.2
060900     CLOSE SQ-FS2.                                                SQ2044.2
061000 SQ204A-END-ROUTINE.                                              SQ2044.2
061100     MOVE "END OF SQ204A VALIDATION TESTS" TO PRINT-REC.          SQ2044.2
061200     WRITE PRINT-REC AFTER ADVANCING 1 LINE.                      SQ2044.2
061300 TERMINATE-SQ204A.                                                SQ2044.2
061400     EXIT.                                                        SQ2044.2
061500 CCVS-EXIT SECTION.                                               SQ2044.2
061600 CCVS-999999.                                                     SQ2044.2
061700     GO TO CLOSE-FILES.                                           SQ2044.2
      *END-OF,SQ204A                                                                  
