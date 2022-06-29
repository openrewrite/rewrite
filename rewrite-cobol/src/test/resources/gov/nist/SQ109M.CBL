      *HEADER,COBOL,SQ109M                                                            
000100 IDENTIFICATION DIVISION.                                         SQ1094.2
000200 PROGRAM-ID.                                                      SQ1094.2
000300     SQ109M.                                                      SQ1094.2
000400****************************************************************  SQ1094.2
000500*                                                              *  SQ1094.2
000600*    VALIDATION FOR:-                                          *  SQ1094.2
000700*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1094.2
000800*                                                              *  SQ1094.2
000900*    CREATION DATE     /     VALIDATION DATE                   *  SQ1094.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ1094.2
001100*                                                              *  SQ1094.2
001200****************************************************************  SQ1094.2
001300                                                                  SQ1094.2
001400*         THIS ROUTINE CREATES A 2 REEL TAPE FILE OF FIXED        SQ1094.2
001500*    LENGTH RECORDS.  THE FILE IS THEN CLOSED AND OPENED AS AN    SQ1094.2
001600*    INPUT FILE.  THE FILE IS READ AND FIELDS IN THE INPUT RECORDSSQ1094.2
001700*    ARE COMPARED TO THE VALUES WRITTEN TO ENSURE THAT THE RECORDSSQ1094.2
001800*    WERE PROCESSED CORRECTLY.  THE FILE IS CLOSED AND OPENED     SQ1094.2
001900*    AGAIN AS AN INPUT FILE.  FOUR READ FORMAT OPTIONS ARE USED   SQ1094.2
002000*    TO READ THE FILE AND FIELDS IN THE RECORDS ARE VERIFIED.     SQ1094.2
002100*    THE OPEN, CLOSE, READ, AND WRITE STATEMENTS ARE TESTED FOR   SQ1094.2
002200*    LEVEL ONE FEATURES.                                          SQ1094.2
002300*                                                                 SQ1094.2
002400*    USED X-CARDS:                                                SQ1094.2
002500*         XXXXX006                                                SQ1094.2
002600*         XXXXX055                                                SQ1094.2
002700*     P   XXXXX062                                                SQ1094.2
002800*         XXXXX082                                                SQ1094.2
002900*         XXXXX083                                                SQ1094.2
003000*     C   XXXXX084                                                SQ1094.2
003100*                                                                 SQ1094.2
003200*                                                                 SQ1094.2
003300 ENVIRONMENT DIVISION.                                            SQ1094.2
003400 CONFIGURATION SECTION.                                           SQ1094.2
003500 SOURCE-COMPUTER.                                                 SQ1094.2
003600     XXXXX082.                                                    SQ1094.2
003700 OBJECT-COMPUTER.                                                 SQ1094.2
003800     XXXXX083.                                                    SQ1094.2
003900 INPUT-OUTPUT SECTION.                                            SQ1094.2
004000 FILE-CONTROL.                                                    SQ1094.2
004100*    SELECT RAW-DATA   ASSIGN TO                                  SQ1094.2
004200*    XXXXX062                                                     SQ1094.2
004300*           ORGANIZATION IS INDEXED                               SQ1094.2
004400*           ACCESS MODE IS RANDOM                                 SQ1094.2
004500*           RECORD KEY IS RAW-DATA-KEY.                           SQ1094.2
004600     SELECT PRINT-FILE ASSIGN TO                                  SQ1094.2
004700     XXXXX055.                                                    SQ1094.2
004800     SELECT SQ-FS1 ASSIGN TO                                      SQ1094.2
004900     XXXXX006                                                     SQ1094.2
005000     ORGANIZATION IS SEQUENTIAL                                   SQ1094.2
005100     ACCESS MODE IS SEQUENTIAL.                                   SQ1094.2
005200 DATA DIVISION.                                                   SQ1094.2
005300 FILE SECTION.                                                    SQ1094.2
005400*                                                                 SQ1094.2
005500*FD  RAW-DATA.                                                    SQ1094.2
005600*                                                                 SQ1094.2
005700*01  RAW-DATA-SATZ.                                               SQ1094.2
005800*    05  RAW-DATA-KEY        PIC X(6).                            SQ1094.2
005900*    05  C-DATE              PIC 9(6).                            SQ1094.2
006000*    05  C-TIME              PIC 9(8).                            SQ1094.2
006100*    05  C-NO-OF-TESTS       PIC 99.                              SQ1094.2
006200*    05  C-OK                PIC 999.                             SQ1094.2
006300*    05  C-ALL               PIC 999.                             SQ1094.2
006400*    05  C-FAIL              PIC 999.                             SQ1094.2
006500*    05  C-DELETED           PIC 999.                             SQ1094.2
006600*    05  C-INSPECT           PIC 999.                             SQ1094.2
006700*    05  C-NOTE              PIC X(13).                           SQ1094.2
006800*    05  C-INDENT            PIC X.                               SQ1094.2
006900*    05  C-ABORT             PIC X(8).                            SQ1094.2
007000 FD  PRINT-FILE                                                   SQ1094.2
007100C    LABEL RECORDS                                                SQ1094.2
007200C    XXXXX084                                                     SQ1094.2
007300C    DATA RECORD IS PRINT-REC DUMMY-RECORD                        SQ1094.2
007400               .                                                  SQ1094.2
007500 01  PRINT-REC PICTURE X(120).                                    SQ1094.2
007600 01  DUMMY-RECORD PICTURE X(120).                                 SQ1094.2
007700 FD  SQ-FS1                                                       SQ1094.2
007800C    LABEL RECORD STANDARD                                        SQ1094.2
007900               .                                                  SQ1094.2
008000 01  SQ-FS1R1-F-G-120.                                            SQ1094.2
008100     02  FILLER PIC X(120).                                       SQ1094.2
008200 WORKING-STORAGE SECTION.                                         SQ1094.2
008300 01  WRK-CS-09V00 PIC S9(9) USAGE COMP VALUE ZERO.                SQ1094.2
008400 01  RECORDS-IN-ERROR PIC S9(5) USAGE COMP VALUE ZERO.            SQ1094.2
008500 01  ERROR-FLAG PIC 9 VALUE ZERO.                                 SQ1094.2
008600 01  EOF-FLAG   PICTURE 9 VALUE ZERO.                             SQ1094.2
008700 01  FILE-RECORD-INFORMATION-REC.                                 SQ1094.2
008800     03 FILE-RECORD-INFO-SKELETON.                                SQ1094.2
008900        05 FILLER                 PICTURE X(48)       VALUE       SQ1094.2
009000             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  SQ1094.2
009100        05 FILLER                 PICTURE X(46)       VALUE       SQ1094.2
009200             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    SQ1094.2
009300        05 FILLER                 PICTURE X(26)       VALUE       SQ1094.2
009400             ",LFIL=000000,ORG=  ,LBLR= ".                        SQ1094.2
009500        05 FILLER                 PICTURE X(37)       VALUE       SQ1094.2
009600             ",RECKEY=                             ".             SQ1094.2
009700        05 FILLER                 PICTURE X(38)       VALUE       SQ1094.2
009800             ",ALTKEY1=                             ".            SQ1094.2
009900        05 FILLER                 PICTURE X(38)       VALUE       SQ1094.2
010000             ",ALTKEY2=                             ".            SQ1094.2
010100        05 FILLER                 PICTURE X(7)        VALUE SPACE.SQ1094.2
010200     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              SQ1094.2
010300        05 FILE-RECORD-INFO-P1-120.                               SQ1094.2
010400           07 FILLER              PIC X(5).                       SQ1094.2
010500           07 XFILE-NAME           PIC X(6).                      SQ1094.2
010600           07 FILLER              PIC X(8).                       SQ1094.2
010700           07 XRECORD-NAME         PIC X(6).                      SQ1094.2
010800           07 FILLER              PIC X(1).                       SQ1094.2
010900           07 REELUNIT-NUMBER     PIC 9(1).                       SQ1094.2
011000           07 FILLER              PIC X(7).                       SQ1094.2
011100           07 XRECORD-NUMBER       PIC 9(6).                      SQ1094.2
011200           07 FILLER              PIC X(6).                       SQ1094.2
011300           07 UPDATE-NUMBER       PIC 9(2).                       SQ1094.2
011400           07 FILLER              PIC X(5).                       SQ1094.2
011500           07 ODO-NUMBER          PIC 9(4).                       SQ1094.2
011600           07 FILLER              PIC X(5).                       SQ1094.2
011700           07 XPROGRAM-NAME        PIC X(5).                      SQ1094.2
011800           07 FILLER              PIC X(7).                       SQ1094.2
011900           07 XRECORD-LENGTH       PIC 9(6).                      SQ1094.2
012000           07 FILLER              PIC X(7).                       SQ1094.2
012100           07 CHARS-OR-RECORDS    PIC X(2).                       SQ1094.2
012200           07 FILLER              PIC X(1).                       SQ1094.2
012300           07 XBLOCK-SIZE          PIC 9(4).                      SQ1094.2
012400           07 FILLER              PIC X(6).                       SQ1094.2
012500           07 RECORDS-IN-FILE     PIC 9(6).                       SQ1094.2
012600           07 FILLER              PIC X(5).                       SQ1094.2
012700           07 XFILE-ORGANIZATION   PIC X(2).                      SQ1094.2
012800           07 FILLER              PIC X(6).                       SQ1094.2
012900           07 XLABEL-TYPE          PIC X(1).                      SQ1094.2
013000        05 FILE-RECORD-INFO-P121-240.                             SQ1094.2
013100           07 FILLER              PIC X(8).                       SQ1094.2
013200           07 XRECORD-KEY          PIC X(29).                     SQ1094.2
013300           07 FILLER              PIC X(9).                       SQ1094.2
013400           07 ALTERNATE-KEY1      PIC X(29).                      SQ1094.2
013500           07 FILLER              PIC X(9).                       SQ1094.2
013600           07 ALTERNATE-KEY2      PIC X(29).                      SQ1094.2
013700           07 FILLER              PIC X(7).                       SQ1094.2
013800 01  TEST-RESULTS.                                                SQ1094.2
013900     02 FILLER                    PICTURE X VALUE SPACE.          SQ1094.2
014000     02 FEATURE                   PICTURE X(20) VALUE SPACE.      SQ1094.2
014100     02 FILLER                    PICTURE X VALUE SPACE.          SQ1094.2
014200     02 P-OR-F                    PICTURE X(5) VALUE SPACE.       SQ1094.2
014300     02 FILLER                    PICTURE X  VALUE SPACE.         SQ1094.2
014400     02  PAR-NAME.                                                SQ1094.2
014500       03 FILLER PICTURE X(12) VALUE SPACE.                       SQ1094.2
014600       03  PARDOT-X PICTURE X  VALUE SPACE.                       SQ1094.2
014700       03 DOTVALUE PICTURE 99  VALUE ZERO.                        SQ1094.2
014800       03 FILLER PIC X(5) VALUE SPACE.                            SQ1094.2
014900     02 FILLER PIC X(10) VALUE SPACE.                             SQ1094.2
015000     02 RE-MARK PIC X(61).                                        SQ1094.2
015100 01  TEST-COMPUTED.                                               SQ1094.2
015200     02 FILLER PIC X(30) VALUE SPACE.                             SQ1094.2
015300     02 FILLER PIC X(17) VALUE "       COMPUTED=".                SQ1094.2
015400     02 COMPUTED-X.                                               SQ1094.2
015500     03 COMPUTED-A                PICTURE X(20) VALUE SPACE.      SQ1094.2
015600     03 COMPUTED-N REDEFINES COMPUTED-A PICTURE -9(9).9(9).       SQ1094.2
015700     03 COMPUTED-0V18 REDEFINES COMPUTED-A  PICTURE -.9(18).      SQ1094.2
015800     03 COMPUTED-4V14 REDEFINES COMPUTED-A  PICTURE -9(4).9(14).  SQ1094.2
015900     03 COMPUTED-14V4 REDEFINES COMPUTED-A  PICTURE -9(14).9(4).  SQ1094.2
016000     03       CM-18V0 REDEFINES COMPUTED-A.                       SQ1094.2
016100         04 COMPUTED-18V0                   PICTURE -9(18).       SQ1094.2
016200         04 FILLER                          PICTURE X.            SQ1094.2
016300     03 FILLER PIC X(50) VALUE SPACE.                             SQ1094.2
016400 01  TEST-CORRECT.                                                SQ1094.2
016500     02 FILLER PIC X(30) VALUE SPACE.                             SQ1094.2
016600     02 FILLER PIC X(17) VALUE "       CORRECT =".                SQ1094.2
016700     02 CORRECT-X.                                                SQ1094.2
016800     03 CORRECT-A                 PICTURE X(20) VALUE SPACE.      SQ1094.2
016900     03 CORRECT-N REDEFINES CORRECT-A PICTURE -9(9).9(9).         SQ1094.2
017000     03 CORRECT-0V18 REDEFINES CORRECT-A    PICTURE -.9(18).      SQ1094.2
017100     03 CORRECT-4V14 REDEFINES CORRECT-A    PICTURE -9(4).9(14).  SQ1094.2
017200     03 CORRECT-14V4 REDEFINES CORRECT-A    PICTURE -9(14).9(4).  SQ1094.2
017300     03      CR-18V0 REDEFINES CORRECT-A.                         SQ1094.2
017400         04 CORRECT-18V0                    PICTURE -9(18).       SQ1094.2
017500         04 FILLER                          PICTURE X.            SQ1094.2
017600     03 FILLER PIC X(50) VALUE SPACE.                             SQ1094.2
017700 01  CCVS-C-1.                                                    SQ1094.2
017800     02 FILLER PICTURE IS X(99) VALUE IS " FEATURE              PASQ1094.2
017900-    "SS  PARAGRAPH-NAME                                          SQ1094.2
018000-    "        REMARKS".                                           SQ1094.2
018100     02 FILLER PICTURE IS X(20) VALUE IS SPACE.                   SQ1094.2
018200 01  CCVS-C-2.                                                    SQ1094.2
018300     02 FILLER PICTURE IS X VALUE IS SPACE.                       SQ1094.2
018400     02 FILLER PICTURE IS X(6) VALUE IS "TESTED".                 SQ1094.2
018500     02 FILLER PICTURE IS X(15) VALUE IS SPACE.                   SQ1094.2
018600     02 FILLER PICTURE IS X(4) VALUE IS "FAIL".                   SQ1094.2
018700     02 FILLER PICTURE IS X(94) VALUE IS SPACE.                   SQ1094.2
018800 01  REC-SKL-SUB PICTURE 9(2) VALUE ZERO.                         SQ1094.2
018900 01  REC-CT PICTURE 99 VALUE ZERO.                                SQ1094.2
019000 01  DELETE-CNT                   PICTURE 999  VALUE ZERO.        SQ1094.2
019100 01  ERROR-COUNTER PICTURE IS 999 VALUE IS ZERO.                  SQ1094.2
019200 01  INSPECT-COUNTER PIC 999 VALUE ZERO.                          SQ1094.2
019300 01  PASS-COUNTER PIC 999 VALUE ZERO.                             SQ1094.2
019400 01  TOTAL-ERROR PIC 999 VALUE ZERO.                              SQ1094.2
019500 01  ERROR-HOLD PIC 999 VALUE ZERO.                               SQ1094.2
019600 01  DUMMY-HOLD PIC X(120) VALUE SPACE.                           SQ1094.2
019700 01  RECORD-COUNT PIC 9(5) VALUE ZERO.                            SQ1094.2
019800 01  CCVS-H-1.                                                    SQ1094.2
019900     02  FILLER   PICTURE X(27)  VALUE SPACE.                     SQ1094.2
020000     02 FILLER PICTURE X(67) VALUE                                SQ1094.2
020100     " FEDERAL SOFTWARE TESTING CENTER COBOL COMPILER VALIDATION  SQ1094.2
020200-    " SYSTEM".                                                   SQ1094.2
020300     02  FILLER     PICTURE X(26)  VALUE SPACE.                   SQ1094.2
020400 01  CCVS-H-2.                                                    SQ1094.2
020500     02 FILLER PICTURE X(52) VALUE IS                             SQ1094.2
020600     "CCVS85 FSTC COPY, NOT FOR DISTRIBUTION.".                   SQ1094.2
020700     02 FILLER PICTURE IS X(19) VALUE IS "TEST RESULTS SET-  ".   SQ1094.2
020800     02 TEST-ID PICTURE IS X(9).                                  SQ1094.2
020900     02 FILLER PICTURE IS X(40) VALUE IS SPACE.                   SQ1094.2
021000 01  CCVS-H-3.                                                    SQ1094.2
021100     02  FILLER PICTURE X(34) VALUE                               SQ1094.2
021200     " FOR OFFICIAL USE ONLY    ".                                SQ1094.2
021300     02  FILLER PICTURE X(58) VALUE                               SQ1094.2
021400     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ1094.2
021500     02  FILLER PICTURE X(28) VALUE                               SQ1094.2
021600     "  COPYRIGHT   1985 ".                                       SQ1094.2
021700 01  CCVS-E-1.                                                    SQ1094.2
021800     02 FILLER PICTURE IS X(52) VALUE IS SPACE.                   SQ1094.2
021900     02 FILLER PICTURE IS X(14) VALUE IS "END OF TEST-  ".        SQ1094.2
022000     02 ID-AGAIN PICTURE IS X(9).                                 SQ1094.2
022100     02 FILLER PICTURE X(45) VALUE IS                             SQ1094.2
022200     " NTIS DISTRIBUTION COBOL 85".                               SQ1094.2
022300 01  CCVS-E-2.                                                    SQ1094.2
022400     02  FILLER                   PICTURE X(31)  VALUE            SQ1094.2
022500     SPACE.                                                       SQ1094.2
022600     02  FILLER                   PICTURE X(21)  VALUE SPACE.     SQ1094.2
022700     02 CCVS-E-2-2.                                               SQ1094.2
022800         03 ERROR-TOTAL PICTURE IS XXX VALUE IS SPACE.            SQ1094.2
022900         03 FILLER PICTURE IS X VALUE IS SPACE.                   SQ1094.2
023000         03 ENDER-DESC PIC X(46) VALUE "ERRORS ENCOUNTERED".      SQ1094.2
023100 01  CCVS-E-3.                                                    SQ1094.2
023200     02  FILLER PICTURE X(22) VALUE                               SQ1094.2
023300     " FOR OFFICIAL USE ONLY".                                    SQ1094.2
023400     02  FILLER PICTURE X(12) VALUE SPACE.                        SQ1094.2
023500     02  FILLER PICTURE X(58) VALUE                               SQ1094.2
023600     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1094.2
023700     02  FILLER PICTURE X(13) VALUE SPACE.                        SQ1094.2
023800     02 FILLER PIC X(15) VALUE " COPYRIGHT 1985".                 SQ1094.2
023900 01  CCVS-E-4.                                                    SQ1094.2
024000     02 CCVS-E-4-1 PIC XXX VALUE SPACE.                           SQ1094.2
024100     02 FILLER PIC XXXX VALUE " OF ".                             SQ1094.2
024200     02 CCVS-E-4-2 PIC XXX VALUE SPACE.                           SQ1094.2
024300     02 FILLER PIC X(40) VALUE                                    SQ1094.2
024400      "  TESTS WERE EXECUTED SUCCESSFULLY".                       SQ1094.2
024500 01  XXINFO.                                                      SQ1094.2
024600     02 FILLER PIC X(30) VALUE "        *** INFORMATION  ***".    SQ1094.2
024700     02 INFO-TEXT.                                                SQ1094.2
024800     04 FILLER PIC X(20) VALUE SPACE.                             SQ1094.2
024900     04 XXCOMPUTED PIC X(20).                                     SQ1094.2
025000     04 FILLER PIC X(5) VALUE SPACE.                              SQ1094.2
025100     04 XXCORRECT PIC X(20).                                      SQ1094.2
025200 01  HYPHEN-LINE.                                                 SQ1094.2
025300     02 FILLER PICTURE IS X VALUE IS SPACE.                       SQ1094.2
025400     02 FILLER PICTURE IS X(65) VALUE IS "************************SQ1094.2
025500-    "*****************************************".                 SQ1094.2
025600     02 FILLER PICTURE IS X(54) VALUE IS "************************SQ1094.2
025700-    "******************************".                            SQ1094.2
025800 01  CCVS-PGM-ID PIC X(6) VALUE                                   SQ1094.2
025900     "SQ109M".                                                    SQ1094.2
026000 PROCEDURE DIVISION.                                              SQ1094.2
026100 CCVS1 SECTION.                                                   SQ1094.2
026200 OPEN-FILES.                                                      SQ1094.2
026300*    OPEN I-O RAW-DATA.                                           SQ1094.2
026400*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            SQ1094.2
026500*    READ RAW-DATA INVALID KEY GO TO END-E-1.                     SQ1094.2
026600*    MOVE "ABORTED " TO C-ABORT.                                  SQ1094.2
026700*    ADD 1 TO C-NO-OF-TESTS.                                      SQ1094.2
026800*    ACCEPT C-DATE  FROM DATE.                                    SQ1094.2
026900*    ACCEPT C-TIME  FROM TIME.                                    SQ1094.2
027000*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-1.             SQ1094.2
027100*END-E-1.                                                         SQ1094.2
027200*    CLOSE RAW-DATA.                                              SQ1094.2
027300     OPEN     OUTPUT PRINT-FILE.                                  SQ1094.2
027400     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   SQ1094.2
027500     MOVE    SPACE TO TEST-RESULTS.                               SQ1094.2
027600     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             SQ1094.2
027700     MOVE ZERO TO REC-SKL-SUB.                                    SQ1094.2
027800     PERFORM CCVS-INIT-FILE 9 TIMES.                              SQ1094.2
027900 CCVS-INIT-FILE.                                                  SQ1094.2
028000     ADD 1 TO REC-SKL-SUB.                                        SQ1094.2
028100     MOVE FILE-RECORD-INFO-SKELETON TO                            SQ1094.2
028200                  FILE-RECORD-INFO (REC-SKL-SUB).                 SQ1094.2
028300 CCVS-INIT-EXIT.                                                  SQ1094.2
028400     GO TO CCVS1-EXIT.                                            SQ1094.2
028500 CLOSE-FILES.                                                     SQ1094.2
028600     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   SQ1094.2
028700*    OPEN I-O RAW-DATA.                                           SQ1094.2
028800*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            SQ1094.2
028900*    READ RAW-DATA INVALID KEY GO TO END-E-2.                     SQ1094.2
029000*    MOVE "OK.     " TO C-ABORT.                                  SQ1094.2
029100*    MOVE PASS-COUNTER TO C-OK.                                   SQ1094.2
029200*    MOVE ERROR-HOLD   TO C-ALL.                                  SQ1094.2
029300*    MOVE ERROR-COUNTER TO C-FAIL.                                SQ1094.2
029400*    MOVE DELETE-CNT TO C-DELETED.                                SQ1094.2
029500*    MOVE INSPECT-COUNTER TO C-INSPECT.                           SQ1094.2
029600*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-2.             SQ1094.2
029700*END-E-2.                                                         SQ1094.2
029800*    CLOSE RAW-DATA.                                              SQ1094.2
029900 TERMINATE-CCVS.                                                  SQ1094.2
030000*    EXIT PROGRAM.                                                SQ1094.2
030100*TERMINATE-CALL.                                                  SQ1094.2
030200     STOP     RUN.                                                SQ1094.2
030300 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         SQ1094.2
030400 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           SQ1094.2
030500 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          SQ1094.2
030600 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-CNT.          SQ1094.2
030700     MOVE "****TEST DELETED****" TO RE-MARK.                      SQ1094.2
030800 PRINT-DETAIL.                                                    SQ1094.2
030900     IF REC-CT NOT EQUAL TO ZERO                                  SQ1094.2
031000             MOVE "." TO PARDOT-X                                 SQ1094.2
031100             MOVE REC-CT TO DOTVALUE.                             SQ1094.2
031200     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      SQ1094.2
031300     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               SQ1094.2
031400        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 SQ1094.2
031500          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 SQ1094.2
031600     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              SQ1094.2
031700     MOVE SPACE TO CORRECT-X.                                     SQ1094.2
031800     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         SQ1094.2
031900     MOVE     SPACE TO RE-MARK.                                   SQ1094.2
032000 HEAD-ROUTINE.                                                    SQ1094.2
032100     MOVE CCVS-H-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ1094.2
032200     MOVE CCVS-H-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.   SQ1094.2
032300     MOVE CCVS-H-3 TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.   SQ1094.2
032400 COLUMN-NAMES-ROUTINE.                                            SQ1094.2
032500     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ1094.2
032600     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ1094.2
032700     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ1094.2
032800 END-ROUTINE.                                                     SQ1094.2
032900     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.SQ1094.2
033000 END-RTN-EXIT.                                                    SQ1094.2
033100     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ1094.2
033200 END-ROUTINE-1.                                                   SQ1094.2
033300      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      SQ1094.2
033400      ERROR-HOLD. ADD DELETE-CNT TO ERROR-HOLD.                   SQ1094.2
033500      ADD PASS-COUNTER TO ERROR-HOLD.                             SQ1094.2
033600*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   SQ1094.2
033700      MOVE PASS-COUNTER TO CCVS-E-4-1.                            SQ1094.2
033800      MOVE ERROR-HOLD TO CCVS-E-4-2.                              SQ1094.2
033900      MOVE CCVS-E-4 TO CCVS-E-2-2.                                SQ1094.2
034000      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           SQ1094.2
034100  END-ROUTINE-12.                                                 SQ1094.2
034200      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        SQ1094.2
034300     IF       ERROR-COUNTER IS EQUAL TO ZERO                      SQ1094.2
034400         MOVE "NO " TO ERROR-TOTAL                                SQ1094.2
034500         ELSE                                                     SQ1094.2
034600         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       SQ1094.2
034700     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           SQ1094.2
034800     PERFORM WRITE-LINE.                                          SQ1094.2
034900 END-ROUTINE-13.                                                  SQ1094.2
035000     IF DELETE-CNT IS EQUAL TO ZERO                               SQ1094.2
035100         MOVE "NO " TO ERROR-TOTAL  ELSE                          SQ1094.2
035200         MOVE DELETE-CNT TO ERROR-TOTAL.                          SQ1094.2
035300     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   SQ1094.2
035400     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ1094.2
035500      IF   INSPECT-COUNTER EQUAL TO ZERO                          SQ1094.2
035600          MOVE "NO " TO ERROR-TOTAL                               SQ1094.2
035700      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   SQ1094.2
035800      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            SQ1094.2
035900      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          SQ1094.2
036000     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ1094.2
036100 WRITE-LINE.                                                      SQ1094.2
036200     ADD 1 TO RECORD-COUNT.                                       SQ1094.2
036300*    IF RECORD-COUNT GREATER 50                                   SQ1094.2
036400*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          SQ1094.2
036500*        MOVE SPACE TO DUMMY-RECORD                               SQ1094.2
036600*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  SQ1094.2
036700*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             SQ1094.2
036800*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     SQ1094.2
036900*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          SQ1094.2
037000*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          SQ1094.2
037100*        MOVE ZERO TO RECORD-COUNT.                               SQ1094.2
037200     PERFORM WRT-LN.                                              SQ1094.2
037300 WRT-LN.                                                          SQ1094.2
037400     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               SQ1094.2
037500     MOVE SPACE TO DUMMY-RECORD.                                  SQ1094.2
037600 BLANK-LINE-PRINT.                                                SQ1094.2
037700     PERFORM WRT-LN.                                              SQ1094.2
037800 FAIL-ROUTINE.                                                    SQ1094.2
037900     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.   SQ1094.2
038000     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.    SQ1094.2
038100     MOVE "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.    SQ1094.2
038200     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     SQ1094.2
038300     GO TO FAIL-ROUTINE-EX.                                       SQ1094.2
038400 FAIL-ROUTINE-WRITE.                                              SQ1094.2
038500     MOVE TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE           SQ1094.2
038600     MOVE TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES.   SQ1094.2
038700 FAIL-ROUTINE-EX. EXIT.                                           SQ1094.2
038800 BAIL-OUT.                                                        SQ1094.2
038900     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.       SQ1094.2
039000     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.               SQ1094.2
039100 BAIL-OUT-WRITE.                                                  SQ1094.2
039200     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  SQ1094.2
039300     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     SQ1094.2
039400 BAIL-OUT-EX. EXIT.                                               SQ1094.2
039500 CCVS1-EXIT.                                                      SQ1094.2
039600     EXIT.                                                        SQ1094.2
039700 SECT-SQ109-0001 SECTION.                                         SQ1094.2
039800 SEQ-INIT-001.                                                    SQ1094.2
039900     MOVE "SQ-FS1" TO XFILE-NAME (1).                             SQ1094.2
040000     MOVE "R1-F-G" TO XRECORD-NAME (1).                           SQ1094.2
040100     MOVE CCVS-PGM-ID TO XPROGRAM-NAME (1).                       SQ1094.2
040200     MOVE 000120 TO XRECORD-LENGTH (1).                           SQ1094.2
040300     MOVE "RC"   TO CHARS-OR-RECORDS (1).                         SQ1094.2
040400     MOVE 0001   TO XBLOCK-SIZE (1).                              SQ1094.2
040500     MOVE 000750 TO RECORDS-IN-FILE (1).                          SQ1094.2
040600     MOVE "SQ"   TO XFILE-ORGANIZATION (1).                       SQ1094.2
040700     MOVE "S" TO XLABEL-TYPE (1).                                 SQ1094.2
040800     MOVE 000001 TO XRECORD-NUMBER (1).                           SQ1094.2
040900     OPEN OUTPUT SQ-FS1.                                          SQ1094.2
041000 SEQ-TEST-001.                                                    SQ1094.2
041100     MOVE FILE-RECORD-INFO-P1-120 (1)  TO SQ-FS1R1-F-G-120.       SQ1094.2
041200     WRITE SQ-FS1R1-F-G-120.                                      SQ1094.2
041300     IF      XRECORD-NUMBER (1) EQUAL TO 325                      SQ1094.2
041400*             ADD 1 TO REELUNIT-NUMBER (1)                        SQ1094.2
041500*             CLOSE SQ-FS1 REEL.                                  SQ1094.2
041600*    MOVE "CLOSE REEL DELETED" TO RE-MARK.                        SQ1094.2
041700     IF XRECORD-NUMBER (1) EQUAL TO 750                           SQ1094.2
041800         GO TO SEQ-WRITE-001.                                     SQ1094.2
041900     ADD 1 TO XRECORD-NUMBER (1).                                 SQ1094.2
042000     GO TO SEQ-TEST-001.                                          SQ1094.2
042100 SEQ-WRITE-001.                                                   SQ1094.2
042200     MOVE "CREATE FILE SQ-FS1" TO FEATURE.                        SQ1094.2
042300     MOVE "SEQ-TEST-001" TO PAR-NAME.                             SQ1094.2
042400     MOVE "FILE CREATED, RECS =" TO COMPUTED-A.                   SQ1094.2
042500     MOVE  XRECORD-NUMBER (1) TO CORRECT-18V0.                    SQ1094.2
042600     PERFORM PRINT-DETAIL.                                        SQ1094.2
042700     CLOSE SQ-FS1.                                                SQ1094.2
042800*        A SEQUENTIAL TAPE FILE WITH 120 CHARACTER RECORDS        SQ1094.2
042900*    HAS BEEN CREATED. THE FILE CONTAINS 750 RECORDS.             SQ1094.2
043000 SEQ-INIT-002.                                                    SQ1094.2
043100     MOVE ZERO TO WRK-CS-09V00.                                   SQ1094.2
043200*        THIS TEST READS AND CHECKS THE FILE CREATED IN           SQ1094.2
043300*    SEQ-TEST-001.                                                SQ1094.2
043400     OPEN INPUT SQ-FS1.                                           SQ1094.2
043500 SEQ-TEST-002.                                                    SQ1094.2
043600     READ SQ-FS1                                                  SQ1094.2
043700          AT END GO TO SEQ-TEST-002-1.                            SQ1094.2
043800     MOVE   SQ-FS1R1-F-G-120 TO FILE-RECORD-INFO-P1-120 (1).      SQ1094.2
043900     ADD 1 TO WRK-CS-09V00.                                       SQ1094.2
044000     IF WRK-CS-09V00 GREATER THAN 750                             SQ1094.2
044100        MOVE "MORE THAN 750 RECORDS" TO RE-MARK                   SQ1094.2
044200        GO TO SEQ-FAIL-002.                                       SQ1094.2
044300     IF WRK-CS-09V00 NOT EQUAL TO XRECORD-NUMBER (1)              SQ1094.2
044400         ADD 1 TO RECORDS-IN-ERROR                                SQ1094.2
044500         GO TO SEQ-TEST-002.                                      SQ1094.2
044600     IF XFILE-NAME (1) NOT EQUAL TO "SQ-FS1"                      SQ1094.2
044700        ADD 1 TO RECORDS-IN-ERROR                                 SQ1094.2
044800        GO TO SEQ-TEST-002.                                       SQ1094.2
044900     IF XLABEL-TYPE (1) NOT EQUAL TO "S"                          SQ1094.2
045000        ADD 1 TO RECORDS-IN-ERROR.                                SQ1094.2
045100     GO TO SEQ-TEST-002.                                          SQ1094.2
045200 SEQ-TEST-002-1.                                                  SQ1094.2
045300     IF WRK-CS-09V00 EQUAL TO ZERO                                SQ1094.2
045400         MOVE "AT END ON FIRST READ" TO RE-MARK                   SQ1094.2
045500         GO TO SEQ-FAIL-002.                                      SQ1094.2
045600     IF RECORDS-IN-ERROR EQUAL TO ZERO                            SQ1094.2
045700         GO TO SEQ-PASS-002.                                      SQ1094.2
045800     MOVE "ERRORS IN READING SQ-FS1" TO RE-MARK.                  SQ1094.2
045900 SEQ-FAIL-002.                                                    SQ1094.2
046000     MOVE "RECORDS IN ERROR =" TO COMPUTED-A.                     SQ1094.2
046100     MOVE RECORDS-IN-ERROR TO CORRECT-18V0.                       SQ1094.2
046200     PERFORM FAIL.                                                SQ1094.2
046300     GO TO SEQ-WRITE-002.                                         SQ1094.2
046400 SEQ-PASS-002.                                                    SQ1094.2
046500     PERFORM PASS.                                                SQ1094.2
046600     MOVE "FILE VERIFIED RECS =" TO COMPUTED-A.                   SQ1094.2
046700     MOVE WRK-CS-09V00 TO CORRECT-18V0.                           SQ1094.2
046800 SEQ-WRITE-002.                                                   SQ1094.2
046900     MOVE "SEQ-TEST-002" TO PAR-NAME.                             SQ1094.2
047000     MOVE "VERIFY FILE SQ-FS1" TO FEATURE.                        SQ1094.2
047100     PERFORM PRINT-DETAIL.                                        SQ1094.2
047200 SEQ-CLOSE-002.                                                   SQ1094.2
047300     CLOSE SQ-FS1.                                                SQ1094.2
047400 READ-INIT-GF-01.                                                 SQ1094.2
047500     MOVE ZERO TO WRK-CS-09V00.                                   SQ1094.2
047600     MOVE ZERO TO RECORDS-IN-ERROR.                               SQ1094.2
047700     OPEN INPUT   SQ-FS1.                                         SQ1094.2
047800*            FOUR OPTIONS FOR THE READ STATEMENT ARE CHECKED      SQ1094.2
047900*    IN THIS SERIES OF TESTS.                                     SQ1094.2
048000     MOVE "READ...RECORD AT END ..." TO FEATURE.                  SQ1094.2
048100     MOVE "READ-TEST-GF-01" TO PAR-NAME.                          SQ1094.2
048200     MOVE ZERO TO ERROR-FLAG.                                     SQ1094.2
048300 READ-TEST-GF-01.                                                 SQ1094.2
048400     READ SQ-FS1 RECORD AT END                                    SQ1094.2
048500              MOVE "UNEXPECTED EOF" TO COMPUTED-A                 SQ1094.2
048600              MOVE 1 TO EOF-FLAG                                  SQ1094.2
048700              GO TO READ-FAIL-GF-01.                              SQ1094.2
048800     PERFORM RECORD-CHECK.                                        SQ1094.2
048900     IF WRK-CS-09V00 EQUAL TO 200                                 SQ1094.2
049000              GO TO READ-TEST-GF-01-1.                            SQ1094.2
049100             GO TO READ-TEST-GF-01.                               SQ1094.2
049200 RECORD-CHECK.                                                    SQ1094.2
049300     MOVE SQ-FS1R1-F-G-120 TO FILE-RECORD-INFO-P1-120 (1).        SQ1094.2
049400     ADD 1 TO WRK-CS-09V00.                                       SQ1094.2
049500     IF WRK-CS-09V00 NOT EQUAL TO XRECORD-NUMBER (1)              SQ1094.2
049600         ADD 1 TO RECORDS-IN-ERROR                                SQ1094.2
049700         MOVE 1 TO ERROR-FLAG.                                    SQ1094.2
049800 READ-TEST-GF-01-1.                                               SQ1094.2
049900     IF ERROR-FLAG EQUAL TO ZERO                                  SQ1094.2
050000         GO TO READ-PASS-GF-01.                                   SQ1094.2
050100     MOVE "ERROR IN RECORD(S)" TO COMPUTED-A.                     SQ1094.2
050200 READ-FAIL-GF-01.                                                 SQ1094.2
050300     MOVE "VII-44; 4.4.2                            " TO  RE-MARK.SQ1094.2
050400     PERFORM FAIL.                                                SQ1094.2
050500     GO TO READ-WRITE-GF-01.                                      SQ1094.2
050600 READ-PASS-GF-01.                                                 SQ1094.2
050700     PERFORM PASS.                                                SQ1094.2
050800 READ-WRITE-GF-01.                                                SQ1094.2
050900     PERFORM PRINT-DETAIL.                                        SQ1094.2
051000 READ-INIT-GF-02.                                                 SQ1094.2
051100     IF EOF-FLAG EQUAL TO 1                                       SQ1094.2
051200        GO TO SEQ-EOF-003.                                        SQ1094.2
051300     MOVE ZERO TO ERROR-FLAG.                                     SQ1094.2
051400     MOVE "READ...AT END..." TO FEATURE.                          SQ1094.2
051500     MOVE "READ-TEST-GF-02" TO PAR-NAME.                          SQ1094.2
051600 READ-TEST-GF-02.                                                 SQ1094.2
051700     READ SQ-FS1 AT END                                           SQ1094.2
051800         MOVE "UNEXPECTED EOF" TO COMPUTED-A                      SQ1094.2
051900         MOVE 1 TO EOF-FLAG                                       SQ1094.2
052000         GO TO READ-FAIL-GF-02.                                   SQ1094.2
052100     PERFORM RECORD-CHECK.                                        SQ1094.2
052200     IF WRK-CS-09V00 EQUAL TO 400                                 SQ1094.2
052300         GO TO READ-TEST-GF-02-1.                                 SQ1094.2
052400     GO TO READ-TEST-GF-02.                                       SQ1094.2
052500 READ-TEST-GF-02-1.                                               SQ1094.2
052600     IF ERROR-FLAG EQUAL TO ZERO                                  SQ1094.2
052700         GO TO READ-PASS-GF-02.                                   SQ1094.2
052800     MOVE "ERROR IN RECORD(S)" TO COMPUTED-A.                     SQ1094.2
052900 READ-FAIL-GF-02.                                                 SQ1094.2
053000     MOVE "VII-44; 4.4.2                            " TO  RE-MARK.SQ1094.2
053100     PERFORM FAIL.                                                SQ1094.2
053200     GO TO READ-WRITE-GF-02.                                      SQ1094.2
053300 READ-PASS-GF-02.                                                 SQ1094.2
053400     PERFORM PASS.                                                SQ1094.2
053500 READ-WRITE-GF-02.                                                SQ1094.2
053600     PERFORM PRINT-DETAIL.                                        SQ1094.2
053700 READ-INIT-GF-03.                                                 SQ1094.2
053800     IF EOF-FLAG EQUAL TO 1                                       SQ1094.2
053900        GO TO SEQ-EOF-003.                                        SQ1094.2
054000     MOVE ZERO TO ERROR-FLAG.                                     SQ1094.2
054100     MOVE "READ...RECORD END..." TO FEATURE.                      SQ1094.2
054200     MOVE "READ-TEST-GF-03" TO PAR-NAME.                          SQ1094.2
054300 READ-TEST-GF-03.                                                 SQ1094.2
054400     READ SQ-FS1 RECORD END                                       SQ1094.2
054500          MOVE "UNEXPECTED EOF" TO COMPUTED-A                     SQ1094.2
054600          MOVE 1 TO EOF-FLAG                                      SQ1094.2
054700          GO TO READ-FAIL-GF-03.                                  SQ1094.2
054800     PERFORM RECORD-CHECK.                                        SQ1094.2
054900     IF WRK-CS-09V00 EQUAL TO 600                                 SQ1094.2
055000         GO TO READ-TEST-GF-03-1.                                 SQ1094.2
055100     GO TO READ-TEST-GF-03.                                       SQ1094.2
055200 READ-TEST-GF-03-1.                                               SQ1094.2
055300     IF ERROR-FLAG EQUAL TO ZERO                                  SQ1094.2
055400         GO TO READ-PASS-GF-03.                                   SQ1094.2
055500     MOVE "ERROR IN RECORD(S)" TO COMPUTED-A.                     SQ1094.2
055600 READ-FAIL-GF-03.                                                 SQ1094.2
055700     MOVE "VII-44; 4.4.2                            " TO  RE-MARK.SQ1094.2
055800     PERFORM FAIL.                                                SQ1094.2
055900     GO TO READ-WRITE-GF-03.                                      SQ1094.2
056000 READ-PASS-GF-03.                                                 SQ1094.2
056100     PERFORM PASS.                                                SQ1094.2
056200 READ-WRITE-GF-03.                                                SQ1094.2
056300     PERFORM PRINT-DETAIL.                                        SQ1094.2
056400 READ-INIT-GF-04.                                                 SQ1094.2
056500     IF EOF-FLAG EQUAL TO 1                                       SQ1094.2
056600         GO TO SEQ-EOF-003.                                       SQ1094.2
056700     MOVE ZERO TO ERROR-FLAG.                                     SQ1094.2
056800     MOVE "READ...END..." TO FEATURE.                             SQ1094.2
056900     MOVE  "READ-TEST-GF-04" TO PAR-NAME.                         SQ1094.2
057000 READ-TEST-GF-04.                                                 SQ1094.2
057100     READ SQ-FS1 END GO TO READ-TEST-GF-04-1.                     SQ1094.2
057200     PERFORM RECORD-CHECK.                                        SQ1094.2
057300     IF WRK-CS-09V00 GREATER THAN 750                             SQ1094.2
057400          GO TO READ-TEST-GF-04-1.                                SQ1094.2
057500     GO TO READ-TEST-GF-04.                                       SQ1094.2
057600 READ-TEST-GF-04-1.                                               SQ1094.2
057700     IF ERROR-FLAG EQUAL TO ZERO                                  SQ1094.2
057800          GO TO READ-PASS-GF-04.                                  SQ1094.2
057900 READ-FAIL-GF-04.                                                 SQ1094.2
058000     MOVE "VII-44; 4.4.2                            " TO  RE-MARK.SQ1094.2
058100     MOVE "ERROR IN RECORD(S)" TO COMPUTED-A.                     SQ1094.2
058200     PERFORM FAIL.                                                SQ1094.2
058300     GO TO READ-WRITE-GF-04.                                      SQ1094.2
058400 READ-PASS-GF-04.                                                 SQ1094.2
058500     PERFORM PASS.                                                SQ1094.2
058600 READ-WRITE-GF-04.                                                SQ1094.2
058700     PERFORM PRINT-DETAIL.                                        SQ1094.2
058800 SEQ-TEST-003.                                                    SQ1094.2
058900     IF RECORDS-IN-ERROR NOT EQUAL TO ZERO                        SQ1094.2
059000          MOVE "RECORDS IN ERROR =" TO COMPUTED-A                 SQ1094.2
059100          MOVE RECORDS-IN-ERROR TO CORRECT-18V0                   SQ1094.2
059200          GO TO SEQ-FAIL-003.                                     SQ1094.2
059300     IF WRK-CS-09V00 GREATER THAN 750                             SQ1094.2
059400          MOVE "MORE THAN 750 RECORDS" TO RE-MARK                 SQ1094.2
059500          GO TO SEQ-FAIL-003.                                     SQ1094.2
059600 SEQ-PASS-003.                                                    SQ1094.2
059700     PERFORM PASS.                                                SQ1094.2
059800     GO TO SEQ-WRITE-003.                                         SQ1094.2
059900 SEQ-EOF-003.                                                     SQ1094.2
060000     MOVE "LESS THAN 750 RECORDS" TO RE-MARK.                     SQ1094.2
060100     MOVE "RECORDS READ =" TO COMPUTED-A.                         SQ1094.2
060200     MOVE WRK-CS-09V00 TO CORRECT-18V0.                           SQ1094.2
060300 SEQ-FAIL-003.                                                    SQ1094.2
060400     PERFORM FAIL.                                                SQ1094.2
060500 SEQ-WRITE-003.                                                   SQ1094.2
060600     MOVE "SEQ-TEST-003" TO PAR-NAME.                             SQ1094.2
060700     MOVE "READ FILE SQ-FS1" TO FEATURE.                          SQ1094.2
060800     PERFORM PRINT-DETAIL.                                        SQ1094.2
060900 SEQ-CLOSE-003.                                                   SQ1094.2
061000     CLOSE SQ-FS1.                                                SQ1094.2
061100 TERMINATE-ROUTINE.                                               SQ1094.2
061200     EXIT.                                                        SQ1094.2
061300 CCVS-EXIT SECTION.                                               SQ1094.2
061400 CCVS-999999.                                                     SQ1094.2
061500     GO TO CLOSE-FILES.                                           SQ1094.2
      *END-OF,SQ109M                                                                  
