      *HEADER,COBOL,SQ112A                                                            
000100 IDENTIFICATION DIVISION.                                         SQ1124.2
000200 PROGRAM-ID.                                                      SQ1124.2
000300     SQ112A.                                                      SQ1124.2
000400****************************************************************  SQ1124.2
000500*                                                              *  SQ1124.2
000600*    VALIDATION FOR:-                                          *  SQ1124.2
000700*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1124.2
000800*                                                              *  SQ1124.2
000900*    CREATION DATE     /     VALIDATION DATE                   *  SQ1124.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ1124.2
001100*                                                              *  SQ1124.2
001200****************************************************************  SQ1124.2
001300                                                                  SQ1124.2
001400*        THE ROUTINE SQ112A CREATES A FILE WHICH HAS FIXED LENGTH SQ1124.2
001500*    RECORDS.  THE FILE IS THEN CLOSED AND OPENED AS AN INPUT FILESQ1124.2
001600*    AND THE FILE IS READ AND FIELDS IN THE INPUT RECORDS ARE     SQ1124.2
001700*    COMPARED TO THE VALUES WRITTEN TO ENSURE THAT THE RECORDS    SQ1124.2
001800*    WERE PROCESSED CORRECTLY.  THE FILE IS CLOSED AND OPENED     SQ1124.2
001900*    AGAIN FOR OUTPUT.  THE DATA WRITTEN TO THE FILE PREVIOUSLY   SQ1124.2
002000*    SHOULD BE ELIMINATED.  ADDITIONAL  RECORDS ARE WRITTEN TO    SQ1124.2
002100*    THE FILE.  THE FILE IS CLOSED AND OPENED AS AN INPUT FILE.   SQ1124.2
002200*    THE CONTENT OF THE FILE IS VERIFIED TO ASCERTAIN THAT ONLY   SQ1124.2
002300*    DATA WRITTEN AFTER THE FILE HAD BEEN OPENED OUTPUT THE       SQ1124.2
002400*                                                                 SQ1124.2
002500*    SECOND TIME IS PRESENT.                                      SQ1124.2
002600*    THE OPEN, CLOSE, READ, AND WRITE STATEMENTS ARE TESTED FOR   SQ1124.2
002700*    LEVEL ONE FEATURES.                                          SQ1124.2
002800*                                                                 SQ1124.2
002900*    USED X-CARDS:                                                SQ1124.2
003000*         XXXXX001                                                SQ1124.2
003100*         XXXXX055                                                SQ1124.2
003200*     P   XXXXX062                                                SQ1124.2
003300*         XXXXX082                                                SQ1124.2
003400*         XXXXX083                                                SQ1124.2
003500*     C   XXXXX084                                                SQ1124.2
003600*                                                                 SQ1124.2
003700*                                                                 SQ1124.2
003800 ENVIRONMENT DIVISION.                                            SQ1124.2
003900 CONFIGURATION SECTION.                                           SQ1124.2
004000 SOURCE-COMPUTER.                                                 SQ1124.2
004100     XXXXX082.                                                    SQ1124.2
004200 OBJECT-COMPUTER.                                                 SQ1124.2
004300     XXXXX083.                                                    SQ1124.2
004400 INPUT-OUTPUT SECTION.                                            SQ1124.2
004500 FILE-CONTROL.                                                    SQ1124.2
004600*    SELECT RAW-DATA   ASSIGN TO                                  SQ1124.2
004700*    XXXXX062                                                     SQ1124.2
004800*           ORGANIZATION IS INDEXED                               SQ1124.2
004900*           ACCESS MODE IS RANDOM                                 SQ1124.2
005000*           RECORD KEY IS RAW-DATA-KEY.                           SQ1124.2
005100     SELECT PRINT-FILE ASSIGN TO                                  SQ1124.2
005200     XXXXX055.                                                    SQ1124.2
005300     SELECT SQ-FS1 ASSIGN TO                                      SQ1124.2
005400     XXXXX001                                                     SQ1124.2
005500     ORGANIZATION IS SEQUENTIAL                                   SQ1124.2
005600     ACCESS MODE IS SEQUENTIAL.                                   SQ1124.2
005700 DATA DIVISION.                                                   SQ1124.2
005800 FILE SECTION.                                                    SQ1124.2
005900*                                                                 SQ1124.2
006000*FD  RAW-DATA.                                                    SQ1124.2
006100*                                                                 SQ1124.2
006200*01  RAW-DATA-SATZ.                                               SQ1124.2
006300*    05  RAW-DATA-KEY        PIC X(6).                            SQ1124.2
006400*    05  C-DATE              PIC 9(6).                            SQ1124.2
006500*    05  C-TIME              PIC 9(8).                            SQ1124.2
006600*    05  C-NO-OF-TESTS       PIC 99.                              SQ1124.2
006700*    05  C-OK                PIC 999.                             SQ1124.2
006800*    05  C-ALL               PIC 999.                             SQ1124.2
006900*    05  C-FAIL              PIC 999.                             SQ1124.2
007000*    05  C-DELETED           PIC 999.                             SQ1124.2
007100*    05  C-INSPECT           PIC 999.                             SQ1124.2
007200*    05  C-NOTE              PIC X(13).                           SQ1124.2
007300*    05  C-INDENT            PIC X.                               SQ1124.2
007400*    05  C-ABORT             PIC X(8).                            SQ1124.2
007500 FD  PRINT-FILE                                                   SQ1124.2
007600C    LABEL RECORDS                                                SQ1124.2
007700C    XXXXX084                                                     SQ1124.2
007800C    DATA RECORD IS PRINT-REC DUMMY-RECORD                        SQ1124.2
007900               .                                                  SQ1124.2
008000 01  PRINT-REC PICTURE X(120).                                    SQ1124.2
008100 01  DUMMY-RECORD PICTURE X(120).                                 SQ1124.2
008200 FD  SQ-FS1                                                       SQ1124.2
008300C    LABEL RECORD STANDARD                                        SQ1124.2
008400                   .                                              SQ1124.2
008500 01  SQ-FS1R1-F-G-120.                                            SQ1124.2
008600     02  FILLER PIC X(120).                                       SQ1124.2
008700 WORKING-STORAGE SECTION.                                         SQ1124.2
008800 01  WRK-CS-09V00 PIC S9(9) USAGE COMP VALUE ZERO.                SQ1124.2
008900 01  RECORDS-IN-ERROR PIC S9(5) USAGE COMP VALUE ZERO.            SQ1124.2
009000 01  ERROR-FLAG PIC 9 VALUE ZERO.                                 SQ1124.2
009100 01  EOF-FLAG   PICTURE 9 VALUE ZERO.                             SQ1124.2
009200 01  FILE-RECORD-INFORMATION-REC.                                 SQ1124.2
009300     03 FILE-RECORD-INFO-SKELETON.                                SQ1124.2
009400        05 FILLER                 PICTURE X(48)       VALUE       SQ1124.2
009500             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  SQ1124.2
009600        05 FILLER                 PICTURE X(46)       VALUE       SQ1124.2
009700             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    SQ1124.2
009800        05 FILLER                 PICTURE X(26)       VALUE       SQ1124.2
009900             ",LFIL=000000,ORG=  ,LBLR= ".                        SQ1124.2
010000        05 FILLER                 PICTURE X(37)       VALUE       SQ1124.2
010100             ",RECKEY=                             ".             SQ1124.2
010200        05 FILLER                 PICTURE X(38)       VALUE       SQ1124.2
010300             ",ALTKEY1=                             ".            SQ1124.2
010400        05 FILLER                 PICTURE X(38)       VALUE       SQ1124.2
010500             ",ALTKEY2=                             ".            SQ1124.2
010600        05 FILLER                 PICTURE X(7)        VALUE SPACE.SQ1124.2
010700     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              SQ1124.2
010800        05 FILE-RECORD-INFO-P1-120.                               SQ1124.2
010900           07 FILLER              PIC X(5).                       SQ1124.2
011000           07 XFILE-NAME           PIC X(6).                      SQ1124.2
011100           07 FILLER              PIC X(8).                       SQ1124.2
011200           07 XRECORD-NAME         PIC X(6).                      SQ1124.2
011300           07 FILLER              PIC X(1).                       SQ1124.2
011400           07 REELUNIT-NUMBER     PIC 9(1).                       SQ1124.2
011500           07 FILLER              PIC X(7).                       SQ1124.2
011600           07 XRECORD-NUMBER       PIC 9(6).                      SQ1124.2
011700           07 FILLER              PIC X(6).                       SQ1124.2
011800           07 UPDATE-NUMBER       PIC 9(2).                       SQ1124.2
011900           07 FILLER              PIC X(5).                       SQ1124.2
012000           07 ODO-NUMBER          PIC 9(4).                       SQ1124.2
012100           07 FILLER              PIC X(5).                       SQ1124.2
012200           07 XPROGRAM-NAME        PIC X(5).                      SQ1124.2
012300           07 FILLER              PIC X(7).                       SQ1124.2
012400           07 XRECORD-LENGTH       PIC 9(6).                      SQ1124.2
012500           07 FILLER              PIC X(7).                       SQ1124.2
012600           07 CHARS-OR-RECORDS    PIC X(2).                       SQ1124.2
012700           07 FILLER              PIC X(1).                       SQ1124.2
012800           07 XBLOCK-SIZE          PIC 9(4).                      SQ1124.2
012900           07 FILLER              PIC X(6).                       SQ1124.2
013000           07 RECORDS-IN-FILE     PIC 9(6).                       SQ1124.2
013100           07 FILLER              PIC X(5).                       SQ1124.2
013200           07 XFILE-ORGANIZATION   PIC X(2).                      SQ1124.2
013300           07 FILLER              PIC X(6).                       SQ1124.2
013400           07 XLABEL-TYPE          PIC X(1).                      SQ1124.2
013500        05 FILE-RECORD-INFO-P121-240.                             SQ1124.2
013600           07 FILLER              PIC X(8).                       SQ1124.2
013700           07 XRECORD-KEY          PIC X(29).                     SQ1124.2
013800           07 FILLER              PIC X(9).                       SQ1124.2
013900           07 ALTERNATE-KEY1      PIC X(29).                      SQ1124.2
014000           07 FILLER              PIC X(9).                       SQ1124.2
014100           07 ALTERNATE-KEY2      PIC X(29).                      SQ1124.2
014200           07 FILLER              PIC X(7).                       SQ1124.2
014300 01  TEST-RESULTS.                                                SQ1124.2
014400     02 FILLER                    PICTURE X VALUE SPACE.          SQ1124.2
014500     02 FEATURE                   PICTURE X(20) VALUE SPACE.      SQ1124.2
014600     02 FILLER                    PICTURE X VALUE SPACE.          SQ1124.2
014700     02 P-OR-F                    PICTURE X(5) VALUE SPACE.       SQ1124.2
014800     02 FILLER                    PICTURE X  VALUE SPACE.         SQ1124.2
014900     02  PAR-NAME.                                                SQ1124.2
015000       03 FILLER PICTURE X(12) VALUE SPACE.                       SQ1124.2
015100       03  PARDOT-X PICTURE X  VALUE SPACE.                       SQ1124.2
015200       03 DOTVALUE PICTURE 99  VALUE ZERO.                        SQ1124.2
015300       03 FILLER PIC X(5) VALUE SPACE.                            SQ1124.2
015400     02 FILLER PIC X(10) VALUE SPACE.                             SQ1124.2
015500     02 RE-MARK PIC X(61).                                        SQ1124.2
015600 01  TEST-COMPUTED.                                               SQ1124.2
015700     02 FILLER PIC X(30) VALUE SPACE.                             SQ1124.2
015800     02 FILLER PIC X(17) VALUE "       COMPUTED=".                SQ1124.2
015900     02 COMPUTED-X.                                               SQ1124.2
016000     03 COMPUTED-A                PICTURE X(20) VALUE SPACE.      SQ1124.2
016100     03 COMPUTED-N REDEFINES COMPUTED-A PICTURE -9(9).9(9).       SQ1124.2
016200     03 COMPUTED-0V18 REDEFINES COMPUTED-A  PICTURE -.9(18).      SQ1124.2
016300     03 COMPUTED-4V14 REDEFINES COMPUTED-A  PICTURE -9(4).9(14).  SQ1124.2
016400     03 COMPUTED-14V4 REDEFINES COMPUTED-A  PICTURE -9(14).9(4).  SQ1124.2
016500     03       CM-18V0 REDEFINES COMPUTED-A.                       SQ1124.2
016600         04 COMPUTED-18V0                   PICTURE -9(18).       SQ1124.2
016700         04 FILLER                          PICTURE X.            SQ1124.2
016800     03 FILLER PIC X(50) VALUE SPACE.                             SQ1124.2
016900 01  TEST-CORRECT.                                                SQ1124.2
017000     02 FILLER PIC X(30) VALUE SPACE.                             SQ1124.2
017100     02 FILLER PIC X(17) VALUE "       CORRECT =".                SQ1124.2
017200     02 CORRECT-X.                                                SQ1124.2
017300     03 CORRECT-A                 PICTURE X(20) VALUE SPACE.      SQ1124.2
017400     03 CORRECT-N REDEFINES CORRECT-A PICTURE -9(9).9(9).         SQ1124.2
017500     03 CORRECT-0V18 REDEFINES CORRECT-A    PICTURE -.9(18).      SQ1124.2
017600     03 CORRECT-4V14 REDEFINES CORRECT-A    PICTURE -9(4).9(14).  SQ1124.2
017700     03 CORRECT-14V4 REDEFINES CORRECT-A    PICTURE -9(14).9(4).  SQ1124.2
017800     03      CR-18V0 REDEFINES CORRECT-A.                         SQ1124.2
017900         04 CORRECT-18V0                    PICTURE -9(18).       SQ1124.2
018000         04 FILLER                          PICTURE X.            SQ1124.2
018100     03 FILLER PIC X(50) VALUE SPACE.                             SQ1124.2
018200 01  CCVS-C-1.                                                    SQ1124.2
018300     02 FILLER PICTURE IS X(99) VALUE IS " FEATURE              PASQ1124.2
018400-    "SS  PARAGRAPH-NAME                                          SQ1124.2
018500-    "        REMARKS".                                           SQ1124.2
018600     02 FILLER PICTURE IS X(20) VALUE IS SPACE.                   SQ1124.2
018700 01  CCVS-C-2.                                                    SQ1124.2
018800     02 FILLER PICTURE IS X VALUE IS SPACE.                       SQ1124.2
018900     02 FILLER PICTURE IS X(6) VALUE IS "TESTED".                 SQ1124.2
019000     02 FILLER PICTURE IS X(15) VALUE IS SPACE.                   SQ1124.2
019100     02 FILLER PICTURE IS X(4) VALUE IS "FAIL".                   SQ1124.2
019200     02 FILLER PICTURE IS X(94) VALUE IS SPACE.                   SQ1124.2
019300 01  REC-SKL-SUB PICTURE 9(2) VALUE ZERO.                         SQ1124.2
019400 01  REC-CT PICTURE 99 VALUE ZERO.                                SQ1124.2
019500 01  DELETE-CNT                   PICTURE 999  VALUE ZERO.        SQ1124.2
019600 01  ERROR-COUNTER PICTURE IS 999 VALUE IS ZERO.                  SQ1124.2
019700 01  INSPECT-COUNTER PIC 999 VALUE ZERO.                          SQ1124.2
019800 01  PASS-COUNTER PIC 999 VALUE ZERO.                             SQ1124.2
019900 01  TOTAL-ERROR PIC 999 VALUE ZERO.                              SQ1124.2
020000 01  ERROR-HOLD PIC 999 VALUE ZERO.                               SQ1124.2
020100 01  DUMMY-HOLD PIC X(120) VALUE SPACE.                           SQ1124.2
020200 01  RECORD-COUNT PIC 9(5) VALUE ZERO.                            SQ1124.2
020300 01  CCVS-H-1.                                                    SQ1124.2
020400     02  FILLER   PICTURE X(27)  VALUE SPACE.                     SQ1124.2
020500     02 FILLER PICTURE X(67) VALUE                                SQ1124.2
020600     " FEDERAL SOFTWARE TESTING CENTER COBOL COMPILER VALIDATION  SQ1124.2
020700-    " SYSTEM".                                                   SQ1124.2
020800     02  FILLER     PICTURE X(26)  VALUE SPACE.                   SQ1124.2
020900 01  CCVS-H-2.                                                    SQ1124.2
021000     02 FILLER PICTURE X(52) VALUE IS                             SQ1124.2
021100     "CCVS85 FSTC COPY, NOT FOR DISTRIBUTION.".                   SQ1124.2
021200     02 FILLER PICTURE IS X(19) VALUE IS "TEST RESULTS SET-  ".   SQ1124.2
021300     02 TEST-ID PICTURE IS X(9).                                  SQ1124.2
021400     02 FILLER PICTURE IS X(40) VALUE IS SPACE.                   SQ1124.2
021500 01  CCVS-H-3.                                                    SQ1124.2
021600     02  FILLER PICTURE X(34) VALUE                               SQ1124.2
021700     " FOR OFFICIAL USE ONLY    ".                                SQ1124.2
021800     02  FILLER PICTURE X(58) VALUE                               SQ1124.2
021900     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ1124.2
022000     02  FILLER PICTURE X(28) VALUE                               SQ1124.2
022100     "  COPYRIGHT   1985 ".                                       SQ1124.2
022200 01  CCVS-E-1.                                                    SQ1124.2
022300     02 FILLER PICTURE IS X(52) VALUE IS SPACE.                   SQ1124.2
022400     02 FILLER PICTURE IS X(14) VALUE IS "END OF TEST-  ".        SQ1124.2
022500     02 ID-AGAIN PICTURE IS X(9).                                 SQ1124.2
022600     02 FILLER PICTURE X(45) VALUE IS                             SQ1124.2
022700     " NTIS DISTRIBUTION COBOL 85".                               SQ1124.2
022800 01  CCVS-E-2.                                                    SQ1124.2
022900     02  FILLER                   PICTURE X(31)  VALUE            SQ1124.2
023000     SPACE.                                                       SQ1124.2
023100     02  FILLER                   PICTURE X(21)  VALUE SPACE.     SQ1124.2
023200     02 CCVS-E-2-2.                                               SQ1124.2
023300         03 ERROR-TOTAL PICTURE IS XXX VALUE IS SPACE.            SQ1124.2
023400         03 FILLER PICTURE IS X VALUE IS SPACE.                   SQ1124.2
023500         03 ENDER-DESC PIC X(46) VALUE "ERRORS ENCOUNTERED".      SQ1124.2
023600 01  CCVS-E-3.                                                    SQ1124.2
023700     02  FILLER PICTURE X(22) VALUE                               SQ1124.2
023800     " FOR OFFICIAL USE ONLY".                                    SQ1124.2
023900     02  FILLER PICTURE X(12) VALUE SPACE.                        SQ1124.2
024000     02  FILLER PICTURE X(58) VALUE                               SQ1124.2
024100     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1124.2
024200     02  FILLER PICTURE X(13) VALUE SPACE.                        SQ1124.2
024300     02 FILLER PIC X(15) VALUE " COPYRIGHT 1985".                 SQ1124.2
024400 01  CCVS-E-4.                                                    SQ1124.2
024500     02 CCVS-E-4-1 PIC XXX VALUE SPACE.                           SQ1124.2
024600     02 FILLER PIC XXXX VALUE " OF ".                             SQ1124.2
024700     02 CCVS-E-4-2 PIC XXX VALUE SPACE.                           SQ1124.2
024800     02 FILLER PIC X(40) VALUE                                    SQ1124.2
024900      "  TESTS WERE EXECUTED SUCCESSFULLY".                       SQ1124.2
025000 01  XXINFO.                                                      SQ1124.2
025100     02 FILLER PIC X(30) VALUE "        *** INFORMATION  ***".    SQ1124.2
025200     02 INFO-TEXT.                                                SQ1124.2
025300     04 FILLER PIC X(20) VALUE SPACE.                             SQ1124.2
025400     04 XXCOMPUTED PIC X(20).                                     SQ1124.2
025500     04 FILLER PIC X(5) VALUE SPACE.                              SQ1124.2
025600     04 XXCORRECT PIC X(20).                                      SQ1124.2
025700 01  HYPHEN-LINE.                                                 SQ1124.2
025800     02 FILLER PICTURE IS X VALUE IS SPACE.                       SQ1124.2
025900     02 FILLER PICTURE IS X(65) VALUE IS "************************SQ1124.2
026000-    "*****************************************".                 SQ1124.2
026100     02 FILLER PICTURE IS X(54) VALUE IS "************************SQ1124.2
026200-    "******************************".                            SQ1124.2
026300 01  CCVS-PGM-ID PIC X(6) VALUE                                   SQ1124.2
026400     "SQ112A".                                                    SQ1124.2
026500 PROCEDURE DIVISION.                                              SQ1124.2
026600 CCVS1 SECTION.                                                   SQ1124.2
026700 OPEN-FILES.                                                      SQ1124.2
026800*    OPEN I-O RAW-DATA.                                           SQ1124.2
026900*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            SQ1124.2
027000*    READ RAW-DATA INVALID KEY GO TO END-E-1.                     SQ1124.2
027100*    MOVE "ABORTED " TO C-ABORT.                                  SQ1124.2
027200*    ADD 1 TO C-NO-OF-TESTS.                                      SQ1124.2
027300*    ACCEPT C-DATE  FROM DATE.                                    SQ1124.2
027400*    ACCEPT C-TIME  FROM TIME.                                    SQ1124.2
027500*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-1.             SQ1124.2
027600*END-E-1.                                                         SQ1124.2
027700*    CLOSE RAW-DATA.                                              SQ1124.2
027800     OPEN     OUTPUT PRINT-FILE.                                  SQ1124.2
027900     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   SQ1124.2
028000     MOVE    SPACE TO TEST-RESULTS.                               SQ1124.2
028100     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             SQ1124.2
028200     MOVE ZERO TO REC-SKL-SUB.                                    SQ1124.2
028300     PERFORM CCVS-INIT-FILE 9 TIMES.                              SQ1124.2
028400 CCVS-INIT-FILE.                                                  SQ1124.2
028500     ADD 1 TO REC-SKL-SUB.                                        SQ1124.2
028600     MOVE FILE-RECORD-INFO-SKELETON TO                            SQ1124.2
028700                  FILE-RECORD-INFO (REC-SKL-SUB).                 SQ1124.2
028800 CCVS-INIT-EXIT.                                                  SQ1124.2
028900     GO TO CCVS1-EXIT.                                            SQ1124.2
029000 CLOSE-FILES.                                                     SQ1124.2
029100     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   SQ1124.2
029200*    OPEN I-O RAW-DATA.                                           SQ1124.2
029300*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            SQ1124.2
029400*    READ RAW-DATA INVALID KEY GO TO END-E-2.                     SQ1124.2
029500*    MOVE "OK.     " TO C-ABORT.                                  SQ1124.2
029600*    MOVE PASS-COUNTER TO C-OK.                                   SQ1124.2
029700*    MOVE ERROR-HOLD   TO C-ALL.                                  SQ1124.2
029800*    MOVE ERROR-COUNTER TO C-FAIL.                                SQ1124.2
029900*    MOVE DELETE-CNT TO C-DELETED.                                SQ1124.2
030000*    MOVE INSPECT-COUNTER TO C-INSPECT.                           SQ1124.2
030100*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-2.             SQ1124.2
030200*END-E-2.                                                         SQ1124.2
030300*    CLOSE RAW-DATA.                                              SQ1124.2
030400 TERMINATE-CCVS.                                                  SQ1124.2
030500*    EXIT PROGRAM.                                                SQ1124.2
030600*TERMINATE-CALL.                                                  SQ1124.2
030700     STOP     RUN.                                                SQ1124.2
030800 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         SQ1124.2
030900 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           SQ1124.2
031000 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          SQ1124.2
031100 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-CNT.          SQ1124.2
031200     MOVE "****TEST DELETED****" TO RE-MARK.                      SQ1124.2
031300 PRINT-DETAIL.                                                    SQ1124.2
031400     IF REC-CT NOT EQUAL TO ZERO                                  SQ1124.2
031500             MOVE "." TO PARDOT-X                                 SQ1124.2
031600             MOVE REC-CT TO DOTVALUE.                             SQ1124.2
031700     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      SQ1124.2
031800     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               SQ1124.2
031900        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 SQ1124.2
032000          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 SQ1124.2
032100     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              SQ1124.2
032200     MOVE SPACE TO CORRECT-X.                                     SQ1124.2
032300     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         SQ1124.2
032400     MOVE     SPACE TO RE-MARK.                                   SQ1124.2
032500 HEAD-ROUTINE.                                                    SQ1124.2
032600     MOVE CCVS-H-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ1124.2
032700     MOVE CCVS-H-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.   SQ1124.2
032800     MOVE CCVS-H-3 TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.   SQ1124.2
032900 COLUMN-NAMES-ROUTINE.                                            SQ1124.2
033000     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ1124.2
033100     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ1124.2
033200     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ1124.2
033300 END-ROUTINE.                                                     SQ1124.2
033400     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.SQ1124.2
033500 END-RTN-EXIT.                                                    SQ1124.2
033600     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ1124.2
033700 END-ROUTINE-1.                                                   SQ1124.2
033800      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      SQ1124.2
033900      ERROR-HOLD. ADD DELETE-CNT TO ERROR-HOLD.                   SQ1124.2
034000      ADD PASS-COUNTER TO ERROR-HOLD.                             SQ1124.2
034100*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   SQ1124.2
034200      MOVE PASS-COUNTER TO CCVS-E-4-1.                            SQ1124.2
034300      MOVE ERROR-HOLD TO CCVS-E-4-2.                              SQ1124.2
034400      MOVE CCVS-E-4 TO CCVS-E-2-2.                                SQ1124.2
034500      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           SQ1124.2
034600  END-ROUTINE-12.                                                 SQ1124.2
034700      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        SQ1124.2
034800     IF       ERROR-COUNTER IS EQUAL TO ZERO                      SQ1124.2
034900         MOVE "NO " TO ERROR-TOTAL                                SQ1124.2
035000         ELSE                                                     SQ1124.2
035100         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       SQ1124.2
035200     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           SQ1124.2
035300     PERFORM WRITE-LINE.                                          SQ1124.2
035400 END-ROUTINE-13.                                                  SQ1124.2
035500     IF DELETE-CNT IS EQUAL TO ZERO                               SQ1124.2
035600         MOVE "NO " TO ERROR-TOTAL  ELSE                          SQ1124.2
035700         MOVE DELETE-CNT TO ERROR-TOTAL.                          SQ1124.2
035800     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   SQ1124.2
035900     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ1124.2
036000      IF   INSPECT-COUNTER EQUAL TO ZERO                          SQ1124.2
036100          MOVE "NO " TO ERROR-TOTAL                               SQ1124.2
036200      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   SQ1124.2
036300      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            SQ1124.2
036400      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          SQ1124.2
036500     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ1124.2
036600 WRITE-LINE.                                                      SQ1124.2
036700     ADD 1 TO RECORD-COUNT.                                       SQ1124.2
036800*    IF RECORD-COUNT GREATER 50                                   SQ1124.2
036900*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          SQ1124.2
037000*        MOVE SPACE TO DUMMY-RECORD                               SQ1124.2
037100*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  SQ1124.2
037200*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             SQ1124.2
037300*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     SQ1124.2
037400*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          SQ1124.2
037500*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          SQ1124.2
037600*        MOVE ZERO TO RECORD-COUNT.                               SQ1124.2
037700     PERFORM WRT-LN.                                              SQ1124.2
037800 WRT-LN.                                                          SQ1124.2
037900     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               SQ1124.2
038000     MOVE SPACE TO DUMMY-RECORD.                                  SQ1124.2
038100 BLANK-LINE-PRINT.                                                SQ1124.2
038200     PERFORM WRT-LN.                                              SQ1124.2
038300 FAIL-ROUTINE.                                                    SQ1124.2
038400     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.   SQ1124.2
038500     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.    SQ1124.2
038600     MOVE "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.    SQ1124.2
038700     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     SQ1124.2
038800     GO TO FAIL-ROUTINE-EX.                                       SQ1124.2
038900 FAIL-ROUTINE-WRITE.                                              SQ1124.2
039000     MOVE TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE           SQ1124.2
039100     MOVE TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES.   SQ1124.2
039200 FAIL-ROUTINE-EX. EXIT.                                           SQ1124.2
039300 BAIL-OUT.                                                        SQ1124.2
039400     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.       SQ1124.2
039500     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.               SQ1124.2
039600 BAIL-OUT-WRITE.                                                  SQ1124.2
039700     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  SQ1124.2
039800     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     SQ1124.2
039900 BAIL-OUT-EX. EXIT.                                               SQ1124.2
040000 CCVS1-EXIT.                                                      SQ1124.2
040100     EXIT.                                                        SQ1124.2
040200 SECT-SQ112A-0001 SECTION.                                        SQ1124.2
040300 WRITE-INIT-GF-01.                                                SQ1124.2
040400     MOVE "SQ112X" TO XFILE-NAME (1).                             SQ1124.2
040500     MOVE "OUTPUT" TO XRECORD-NAME (1).                           SQ1124.2
040600     MOVE CCVS-PGM-ID TO XPROGRAM-NAME (1).                       SQ1124.2
040700     MOVE 000120 TO XRECORD-LENGTH (1).                           SQ1124.2
040800     MOVE "RC"   TO CHARS-OR-RECORDS (1).                         SQ1124.2
040900     MOVE 0001   TO XBLOCK-SIZE (1).                              SQ1124.2
041000     MOVE 000150 TO RECORDS-IN-FILE (1).                          SQ1124.2
041100     MOVE "SQ"   TO XFILE-ORGANIZATION (1).                       SQ1124.2
041200     MOVE "S" TO XLABEL-TYPE (1).                                 SQ1124.2
041300     MOVE 000001 TO XRECORD-NUMBER (1).                           SQ1124.2
041400     OPEN OUTPUT SQ-FS1.                                          SQ1124.2
041500 WRITE-TEST-GF-01.                                                SQ1124.2
041600     MOVE FILE-RECORD-INFO-P1-120 (1)  TO SQ-FS1R1-F-G-120.       SQ1124.2
041700     WRITE SQ-FS1R1-F-G-120.                                      SQ1124.2
041800     IF XRECORD-NUMBER (1) EQUAL TO 150                           SQ1124.2
041900         GO TO WRITE-WRITE-GF-01.                                 SQ1124.2
042000     ADD 1 TO XRECORD-NUMBER (1).                                 SQ1124.2
042100     GO TO WRITE-TEST-GF-01.                                      SQ1124.2
042200 WRITE-WRITE-GF-01.                                               SQ1124.2
042300     MOVE "WRITE 150 RECORDS " TO FEATURE.                        SQ1124.2
042400     MOVE "WRITE-TEST-GF-01" TO PAR-NAME.                         SQ1124.2
042500     MOVE "FILE CREATED, RECS =" TO COMPUTED-A.                   SQ1124.2
042600     MOVE  XRECORD-NUMBER (1) TO CORRECT-18V0.                    SQ1124.2
042700     PERFORM PRINT-DETAIL.                                        SQ1124.2
042800     CLOSE SQ-FS1.                                                SQ1124.2
042900*        A SEQUENTIAL TAPE FILE WITH 120 CHARACTER RECORDS        SQ1124.2
043000*    HAS BEEN CREATED. THE FILE CONTAINS 150 RECORDS.             SQ1124.2
043100 READ-INIT-GF-01.                                                 SQ1124.2
043200     MOVE ZERO TO WRK-CS-09V00.                                   SQ1124.2
043300*        THIS TEST READS AND CHECKS THE FILE CREATED IN           SQ1124.2
043400*    READ-TEST-001.                                               SQ1124.2
043500     OPEN INPUT SQ-FS1.                                           SQ1124.2
043600 READ-TEST-GF-01.                                                 SQ1124.2
043700     READ SQ-FS1                                                  SQ1124.2
043800          AT END GO TO READ-TEST-GF-01-1.                         SQ1124.2
043900     MOVE   SQ-FS1R1-F-G-120 TO FILE-RECORD-INFO-P1-120 (1).      SQ1124.2
044000     ADD 1 TO WRK-CS-09V00.                                       SQ1124.2
044100     IF WRK-CS-09V00 GREATER THAN 150                             SQ1124.2
044200        MOVE "MORE THAN 150 RECORDS" TO RE-MARK                   SQ1124.2
044300        GO TO READ-FAIL-GF-01.                                    SQ1124.2
044400     IF WRK-CS-09V00 NOT EQUAL TO XRECORD-NUMBER (1)              SQ1124.2
044500         ADD 1 TO RECORDS-IN-ERROR                                SQ1124.2
044600         GO TO READ-TEST-GF-01.                                   SQ1124.2
044700     IF XFILE-NAME (1) NOT EQUAL TO "SQ112X"                      SQ1124.2
044800        ADD 1 TO RECORDS-IN-ERROR                                 SQ1124.2
044900        GO TO READ-TEST-GF-01.                                    SQ1124.2
045000     IF XLABEL-TYPE (1) NOT EQUAL TO "S"                          SQ1124.2
045100        ADD 1 TO RECORDS-IN-ERROR.                                SQ1124.2
045200     GO TO READ-TEST-GF-01.                                       SQ1124.2
045300 READ-TEST-GF-01-1.                                               SQ1124.2
045400     IF RECORDS-IN-ERROR EQUAL TO ZERO                            SQ1124.2
045500         GO TO READ-PASS-GF-01.                                   SQ1124.2
045600     MOVE "ERRORS IN READING SQ-FS1" TO RE-MARK.                  SQ1124.2
045700 READ-FAIL-GF-01.                                                 SQ1124.2
045800     MOVE "RECORDS IN ERROR =" TO COMPUTED-A.                     SQ1124.2
045900     MOVE RECORDS-IN-ERROR TO CORRECT-18V0.                       SQ1124.2
046000     PERFORM FAIL.                                                SQ1124.2
046100     GO TO READ-READ-GF-01.                                       SQ1124.2
046200 READ-PASS-GF-01.                                                 SQ1124.2
046300     PERFORM PASS.                                                SQ1124.2
046400     MOVE "FILE VERIFIED RECS =" TO COMPUTED-A.                   SQ1124.2
046500     MOVE WRK-CS-09V00 TO CORRECT-18V0.                           SQ1124.2
046600 READ-READ-GF-01.                                                 SQ1124.2
046700     MOVE "READ-TEST-GF-01"  TO PAR-NAME.                         SQ1124.2
046800     MOVE "VERIFY FILE SQ-FS1" TO FEATURE.                        SQ1124.2
046900     PERFORM PRINT-DETAIL.                                        SQ1124.2
047000 READ-CLOSE-GF-01.                                                SQ1124.2
047100     CLOSE SQ-FS1.                                                SQ1124.2
047200 SECT-SQ112A-0002 SECTION.                                        SQ1124.2
047300 WRITE-INIT-GF-02.                                                SQ1124.2
047400     MOVE "SQ-FS1" TO XFILE-NAME (1).                             SQ1124.2
047500     MOVE "R1-F-G" TO XRECORD-NAME (1).                           SQ1124.2
047600     MOVE CCVS-PGM-ID TO XPROGRAM-NAME (1).                       SQ1124.2
047700     MOVE 000120 TO XRECORD-LENGTH (1).                           SQ1124.2
047800     MOVE "RC"   TO CHARS-OR-RECORDS (1).                         SQ1124.2
047900     MOVE 0001   TO XBLOCK-SIZE (1).                              SQ1124.2
048000     MOVE 000150 TO RECORDS-IN-FILE (1).                          SQ1124.2
048100     MOVE "SQ"   TO XFILE-ORGANIZATION (1).                       SQ1124.2
048200     MOVE "S" TO XLABEL-TYPE (1).                                 SQ1124.2
048300     MOVE 000001 TO XRECORD-NUMBER (1).                           SQ1124.2
048400     OPEN OUTPUT SQ-FS1.                                          SQ1124.2
048500 WRITE-TEST-GF-02.                                                SQ1124.2
048600     MOVE FILE-RECORD-INFO-P1-120 (1)  TO SQ-FS1R1-F-G-120.       SQ1124.2
048700     WRITE SQ-FS1R1-F-G-120.                                      SQ1124.2
048800     IF XRECORD-NUMBER (1) EQUAL TO 150                           SQ1124.2
048900         GO TO WRITE-WRITE-GF-02.                                 SQ1124.2
049000     ADD 1 TO XRECORD-NUMBER (1).                                 SQ1124.2
049100     GO TO WRITE-TEST-GF-02.                                      SQ1124.2
049200 WRITE-WRITE-GF-02.                                               SQ1124.2
049300     MOVE "WRITE 150 RECS 2ND" TO FEATURE.                        SQ1124.2
049400     MOVE "WRITE-TEST-GF-02" TO PAR-NAME.                         SQ1124.2
049500     MOVE "FILE CREATED, RECS =" TO COMPUTED-A.                   SQ1124.2
049600     MOVE  XRECORD-NUMBER (1) TO CORRECT-18V0.                    SQ1124.2
049700     PERFORM PRINT-DETAIL.                                        SQ1124.2
049800     CLOSE SQ-FS1.                                                SQ1124.2
049900*        A SEQUENTIAL TAPE FILE WITH 120 CHARACTER RECORDS        SQ1124.2
050000*    HAS BEEN CREATED. THE FILE CONTAINS 150 RECORDS.             SQ1124.2
050100 READ-INIT-GF-02.                                                 SQ1124.2
050200     MOVE ZERO TO WRK-CS-09V00.                                   SQ1124.2
050300*        THIS TEST READS AND CHECKS THE FILE CREATED IN           SQ1124.2
050400*    READ-TEST-001.                                               SQ1124.2
050500     OPEN INPUT SQ-FS1.                                           SQ1124.2
050600 READ-TEST-GF-02.                                                 SQ1124.2
050700     READ SQ-FS1                                                  SQ1124.2
050800          AT END GO TO READ-TEST-GF-02-1.                         SQ1124.2
050900     MOVE   SQ-FS1R1-F-G-120 TO FILE-RECORD-INFO-P1-120 (1).      SQ1124.2
051000     ADD 1 TO WRK-CS-09V00.                                       SQ1124.2
051100*    IF WRK-CS-09V00 GREATER THAN 150                             SQ1124.2
051200*       MOVE "MORE THAN 150 RECORDS" TO RE-MARK                   SQ1124.2
051300*       GO TO READ-FAIL-GF-02.                                    SQ1124.2
051400     IF WRK-CS-09V00 NOT EQUAL TO XRECORD-NUMBER (1)              SQ1124.2
051500         ADD 1 TO RECORDS-IN-ERROR                                SQ1124.2
051600         GO TO READ-TEST-GF-02.                                   SQ1124.2
051700     IF XFILE-NAME (1) NOT EQUAL TO "SQ-FS1"                      SQ1124.2
051800        ADD 1 TO RECORDS-IN-ERROR                                 SQ1124.2
051900        GO TO READ-TEST-GF-02.                                    SQ1124.2
052000     IF XLABEL-TYPE (1) NOT EQUAL TO "S"                          SQ1124.2
052100        ADD 1 TO RECORDS-IN-ERROR.                                SQ1124.2
052200     GO TO READ-TEST-GF-02.                                       SQ1124.2
052300 READ-TEST-GF-02-1.                                               SQ1124.2
052400     IF RECORDS-IN-ERROR EQUAL TO ZERO                            SQ1124.2
052500         GO TO READ-PASS-GF-02.                                   SQ1124.2
052600     MOVE "ERRORS IN READING SQ-FS1" TO RE-MARK.                  SQ1124.2
052700 READ-FAIL-GF-02.                                                 SQ1124.2
052800     MOVE "RECORDS IN ERROR =" TO COMPUTED-A.                     SQ1124.2
052900     MOVE RECORDS-IN-ERROR TO CORRECT-18V0.                       SQ1124.2
053000     MOVE "VII-43;4.3.4 (21)                        " TO  RE-MARK.SQ1124.2
053100     PERFORM FAIL.                                                SQ1124.2
053200     GO TO READ-WRITE-GF-02.                                      SQ1124.2
053300 READ-PASS-GF-02.                                                 SQ1124.2
053400     PERFORM PASS.                                                SQ1124.2
053500     MOVE "FILE VERIFIED RECS =" TO COMPUTED-A.                   SQ1124.2
053600     MOVE WRK-CS-09V00 TO CORRECT-18V0.                           SQ1124.2
053700 READ-WRITE-GF-02.                                                SQ1124.2
053800     MOVE "READ-TEST-GF-02" TO PAR-NAME.                          SQ1124.2
053900     MOVE "VERIFY FILE SQ-FS1" TO FEATURE.                        SQ1124.2
054000     PERFORM PRINT-DETAIL.                                        SQ1124.2
054100 READ-CLOSE-GF-02.                                                SQ1124.2
054200     CLOSE SQ-FS1.                                                SQ1124.2
054300 SECT-SQ112A-0003 SECTION.                                        SQ1124.2
054400 READ-INIT-GF-03.                                                 SQ1124.2
054500     MOVE ZERO TO WRK-CS-09V00.                                   SQ1124.2
054600     MOVE ZERO TO RECORDS-IN-ERROR.                               SQ1124.2
054700     OPEN INPUT   SQ-FS1.                                         SQ1124.2
054800*            FOUR OPTIONS FOR THE READ STATEMENT ARE CHECKED      SQ1124.2
054900*    IN THIS SERIES OF TESTS.                                     SQ1124.2
055000     MOVE "READ...RECORD AT END ..." TO FEATURE.                  SQ1124.2
055100     MOVE "READ-TEST-GF-03" TO PAR-NAME.                          SQ1124.2
055200     MOVE ZERO TO ERROR-FLAG.                                     SQ1124.2
055300 READ-TEST-GF-03.                                                 SQ1124.2
055400     READ SQ-FS1 RECORD AT END                                    SQ1124.2
055500              MOVE "UNEXPECTED EOF" TO COMPUTED-A                 SQ1124.2
055600              MOVE 1 TO EOF-FLAG                                  SQ1124.2
055700              GO TO READ-FAIL-GF-03.                              SQ1124.2
055800     PERFORM RECORD-CHECK.                                        SQ1124.2
055900     IF WRK-CS-09V00 EQUAL TO 40                                  SQ1124.2
056000              GO TO READ-TEST-GF-03-1.                            SQ1124.2
056100             GO TO READ-TEST-GF-03.                               SQ1124.2
056200 RECORD-CHECK.                                                    SQ1124.2
056300     MOVE SQ-FS1R1-F-G-120 TO FILE-RECORD-INFO-P1-120 (1).        SQ1124.2
056400     ADD 1 TO WRK-CS-09V00.                                       SQ1124.2
056500     IF WRK-CS-09V00 NOT EQUAL TO XRECORD-NUMBER (1)              SQ1124.2
056600         ADD 1 TO RECORDS-IN-ERROR                                SQ1124.2
056700         MOVE 1 TO ERROR-FLAG.                                    SQ1124.2
056800 READ-TEST-GF-03-1.                                               SQ1124.2
056900     IF ERROR-FLAG EQUAL TO ZERO                                  SQ1124.2
057000         GO TO READ-PASS-GF-03.                                   SQ1124.2
057100     MOVE "ERROR IN RECORD(S)" TO COMPUTED-A.                     SQ1124.2
057200 READ-FAIL-GF-03.                                                 SQ1124.2
057300     MOVE "VII-44; 4.4.2                            " TO  RE-MARK.SQ1124.2
057400     PERFORM FAIL.                                                SQ1124.2
057500     GO TO READ-WRITE-GF-03.                                      SQ1124.2
057600 READ-PASS-GF-03.                                                 SQ1124.2
057700     PERFORM PASS.                                                SQ1124.2
057800 READ-WRITE-GF-03.                                                SQ1124.2
057900     PERFORM PRINT-DETAIL.                                        SQ1124.2
058000 READ-INIT-GF-04.                                                 SQ1124.2
058100     IF EOF-FLAG EQUAL TO 1                                       SQ1124.2
058200        GO TO SEQ-EOF-005.                                        SQ1124.2
058300     MOVE ZERO TO ERROR-FLAG.                                     SQ1124.2
058400     MOVE "READ...AT END..." TO FEATURE.                          SQ1124.2
058500     MOVE "READ-TEST-GF-04" TO PAR-NAME.                          SQ1124.2
058600 READ-TEST-GF-04.                                                 SQ1124.2
058700     READ SQ-FS1 AT END                                           SQ1124.2
058800         MOVE "UNEXPECTED EOF" TO COMPUTED-A                      SQ1124.2
058900         MOVE 1 TO EOF-FLAG                                       SQ1124.2
059000         GO TO READ-FAIL-GF-04.                                   SQ1124.2
059100     PERFORM RECORD-CHECK.                                        SQ1124.2
059200     IF WRK-CS-09V00 EQUAL TO 80                                  SQ1124.2
059300         GO TO READ-TEST-GF-04-1.                                 SQ1124.2
059400     GO TO READ-TEST-GF-04.                                       SQ1124.2
059500 READ-TEST-GF-04-1.                                               SQ1124.2
059600     IF ERROR-FLAG EQUAL TO ZERO                                  SQ1124.2
059700         GO TO READ-PASS-GF-04.                                   SQ1124.2
059800     MOVE "ERROR IN RECORD(S)" TO COMPUTED-A.                     SQ1124.2
059900 READ-FAIL-GF-04.                                                 SQ1124.2
060000     MOVE "VII-44; 4.4.2                            " TO  RE-MARK.SQ1124.2
060100     PERFORM FAIL.                                                SQ1124.2
060200     GO TO READ-WRITE-GF-04.                                      SQ1124.2
060300 READ-PASS-GF-04.                                                 SQ1124.2
060400     PERFORM PASS.                                                SQ1124.2
060500 READ-WRITE-GF-04.                                                SQ1124.2
060600     PERFORM PRINT-DETAIL.                                        SQ1124.2
060700 READ-INIT-GF-05.                                                 SQ1124.2
060800     IF EOF-FLAG EQUAL TO 1                                       SQ1124.2
060900        GO TO SEQ-EOF-005.                                        SQ1124.2
061000     MOVE ZERO TO ERROR-FLAG.                                     SQ1124.2
061100     MOVE "READ...RECORD END..." TO FEATURE.                      SQ1124.2
061200     MOVE "READ-TEST-GF-05" TO PAR-NAME.                          SQ1124.2
061300 READ-TEST-GF-05.                                                 SQ1124.2
061400     READ SQ-FS1 RECORD END                                       SQ1124.2
061500          MOVE "UNEXPECTED EOF" TO COMPUTED-A                     SQ1124.2
061600          MOVE 1 TO EOF-FLAG                                      SQ1124.2
061700          GO TO READ-FAIL-GF-05.                                  SQ1124.2
061800     PERFORM RECORD-CHECK.                                        SQ1124.2
061900     IF WRK-CS-09V00 EQUAL TO 120                                 SQ1124.2
062000         GO TO READ-TEST-GF-05-1.                                 SQ1124.2
062100     GO TO READ-TEST-GF-05.                                       SQ1124.2
062200 READ-TEST-GF-05-1.                                               SQ1124.2
062300     IF ERROR-FLAG EQUAL TO ZERO                                  SQ1124.2
062400         GO TO READ-PASS-GF-05.                                   SQ1124.2
062500     MOVE "ERROR IN RECORD(S)" TO COMPUTED-A.                     SQ1124.2
062600 READ-FAIL-GF-05.                                                 SQ1124.2
062700     MOVE "VII-44; 4.4.2                            " TO  RE-MARK.SQ1124.2
062800     PERFORM FAIL.                                                SQ1124.2
062900     GO TO READ-WRITE-GF-05.                                      SQ1124.2
063000 READ-PASS-GF-05.                                                 SQ1124.2
063100     PERFORM PASS.                                                SQ1124.2
063200 READ-WRITE-GF-05.                                                SQ1124.2
063300     PERFORM PRINT-DETAIL.                                        SQ1124.2
063400 READ-INIT-GF-06.                                                 SQ1124.2
063500     IF EOF-FLAG EQUAL TO 1                                       SQ1124.2
063600         GO TO SEQ-EOF-005.                                       SQ1124.2
063700     MOVE ZERO TO ERROR-FLAG.                                     SQ1124.2
063800     MOVE "READ...END..." TO FEATURE.                             SQ1124.2
063900     MOVE  "READ-TEST-GF-06" TO PAR-NAME.                         SQ1124.2
064000 READ-TEST-GF-06.                                                 SQ1124.2
064100     READ SQ-FS1 END GO TO READ-TEST-GF-06-1.                     SQ1124.2
064200     PERFORM RECORD-CHECK.                                        SQ1124.2
064300     IF WRK-CS-09V00 GREATER THAN 150                             SQ1124.2
064400          GO TO READ-TEST-GF-06-1.                                SQ1124.2
064500     GO TO READ-TEST-GF-06.                                       SQ1124.2
064600 READ-TEST-GF-06-1.                                               SQ1124.2
064700     IF ERROR-FLAG EQUAL TO ZERO                                  SQ1124.2
064800          GO TO READ-PASS-GF-06.                                  SQ1124.2
064900 READ-FAIL-GF-06.                                                 SQ1124.2
065000     MOVE "ERROR IN RECORD(S)" TO COMPUTED-A.                     SQ1124.2
065100     MOVE "VII-44; 4.4.2                            " TO  RE-MARK.SQ1124.2
065200     PERFORM FAIL.                                                SQ1124.2
065300     GO TO READ-WRITE-GF-06.                                      SQ1124.2
065400 READ-PASS-GF-06.                                                 SQ1124.2
065500     PERFORM PASS.                                                SQ1124.2
065600 READ-WRITE-GF-06.                                                SQ1124.2
065700     PERFORM PRINT-DETAIL.                                        SQ1124.2
065800 SEQ-TEST-005.                                                    SQ1124.2
065900     IF RECORDS-IN-ERROR NOT EQUAL TO ZERO                        SQ1124.2
066000          MOVE "RECORDS IN ERROR =" TO COMPUTED-A                 SQ1124.2
066100          MOVE RECORDS-IN-ERROR TO CORRECT-18V0                   SQ1124.2
066200          GO TO SEQ-FAIL-005.                                     SQ1124.2
066300     IF WRK-CS-09V00 GREATER THAN 150                             SQ1124.2
066400          MOVE "MORE THAN 150 RECORDS" TO RE-MARK                 SQ1124.2
066500          GO TO SEQ-FAIL-005.                                     SQ1124.2
066600 SEQ-PASS-005.                                                    SQ1124.2
066700     PERFORM PASS.                                                SQ1124.2
066800     GO TO SEQ-WRITE-005.                                         SQ1124.2
066900 SEQ-EOF-005.                                                     SQ1124.2
067000     MOVE "LESS THAN 150 RECORDS" TO RE-MARK.                     SQ1124.2
067100     MOVE "RECORDS READ =" TO COMPUTED-A.                         SQ1124.2
067200     MOVE WRK-CS-09V00 TO CORRECT-18V0.                           SQ1124.2
067300 SEQ-FAIL-005.                                                    SQ1124.2
067400     MOVE "VII-43;4.3.4 (21)                        " TO  RE-MARK.SQ1124.2
067500     PERFORM FAIL.                                                SQ1124.2
067600 SEQ-WRITE-005.                                                   SQ1124.2
067700     MOVE "SEQ-TEST-005" TO PAR-NAME.                             SQ1124.2
067800     MOVE "READ FILE SQ-FS1" TO FEATURE.                          SQ1124.2
067900     PERFORM PRINT-DETAIL.                                        SQ1124.2
068000 SEQ-CLOSE-005.                                                   SQ1124.2
068100     CLOSE SQ-FS1.                                                SQ1124.2
068200 TERMINATE-ROUTINE.                                               SQ1124.2
068300     EXIT.                                                        SQ1124.2
068400 CCVS-EXIT SECTION.                                               SQ1124.2
068500 CCVS-999999.                                                     SQ1124.2
068600     GO TO CLOSE-FILES.                                           SQ1124.2
      *END-OF,SQ112A                                                                  
