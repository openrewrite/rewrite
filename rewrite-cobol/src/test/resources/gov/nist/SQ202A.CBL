      *HEADER,COBOL,SQ202A                                                            
000100 IDENTIFICATION DIVISION.                                         SQ2024.2
000200 PROGRAM-ID.                                                      SQ2024.2
000300     SQ202A.                                                      SQ2024.2
000400****************************************************************  SQ2024.2
000500*                                                              *  SQ2024.2
000600*    VALIDATION FOR:-                                          *  SQ2024.2
000700*    " HIGH       ".                                              SQ2024.2
000800*                                                              *  SQ2024.2
000900*    CREATION DATE     /     VALIDATION DATE                   *  SQ2024.2
001000*    "4.2 ".                                                      SQ2024.2
001100*                                                              *  SQ2024.2
001200*  THE ROUTINE SQ202A (OLD SQ203) CREATES A MAGNETIC TAPE FILE ANDSQ2024.2
001300*    PASSES IT ON TO SQ203A TO BE OPENED AS INPUT UNDER A SELECT  SQ2024.2
001400*    OPTIONAL CLAUSE.                                             SQ2024.2
001500 ENVIRONMENT DIVISION.                                            SQ2024.2
001600 CONFIGURATION SECTION.                                           SQ2024.2
001700 SOURCE-COMPUTER.                                                 SQ2024.2
001800     XXXXX082.                                                    SQ2024.2
001900 OBJECT-COMPUTER.                                                 SQ2024.2
002000     XXXXX083.                                                    SQ2024.2
002100 INPUT-OUTPUT SECTION.                                            SQ2024.2
002200 FILE-CONTROL.                                                    SQ2024.2
002300*    SELECT RAW-DATA   ASSIGN TO                                  SQ2024.2
002400*    XXXXX062                                                     SQ2024.2
002500*           ORGANIZATION IS INDEXED                               SQ2024.2
002600*           ACCESS MODE IS RANDOM                                 SQ2024.2
002700*           RECORD KEY IS RAW-DATA-KEY.                           SQ2024.2
002800     SELECT PRINT-FILE ASSIGN TO                                  SQ2024.2
002900     XXXXX055.                                                    SQ2024.2
003000     SELECT SQ-FS1 ASSIGN TO                                      SQ2024.2
003100     XXXXP001.                                                    SQ2024.2
003200 DATA DIVISION.                                                   SQ2024.2
003300 FILE SECTION.                                                    SQ2024.2
003400*                                                                 SQ2024.2
003500*FD  RAW-DATA.                                                    SQ2024.2
003600*                                                                 SQ2024.2
003700*01  RAW-DATA-SATZ.                                               SQ2024.2
003800*    05  RAW-DATA-KEY        PIC X(6).                            SQ2024.2
003900*    05  C-DATE              PIC 9(6).                            SQ2024.2
004000*    05  C-TIME              PIC 9(8).                            SQ2024.2
004100*    05  C-NO-OF-TESTS       PIC 99.                              SQ2024.2
004200*    05  C-OK                PIC 999.                             SQ2024.2
004300*    05  C-ALL               PIC 999.                             SQ2024.2
004400*    05  C-FAIL              PIC 999.                             SQ2024.2
004500*    05  C-DELETED           PIC 999.                             SQ2024.2
004600*    05  C-INSPECT           PIC 999.                             SQ2024.2
004700*    05  C-NOTE              PIC X(13).                           SQ2024.2
004800*    05  C-INDENT            PIC X.                               SQ2024.2
004900*    05  C-ABORT             PIC X(8).                            SQ2024.2
005000 FD  PRINT-FILE                                                   SQ2024.2
005100C    LABEL RECORDS                                                SQ2024.2
005200C    XXXXX084                                                     SQ2024.2
005300C    DATA RECORD IS PRINT-REC DUMMY-RECORD                        SQ2024.2
005400               .                                                  SQ2024.2
005500 01  PRINT-REC PICTURE X(120).                                    SQ2024.2
005600 01  DUMMY-RECORD PICTURE X(120).                                 SQ2024.2
005700 FD  SQ-FS1                                                       SQ2024.2
005800C    LABEL RECORD STANDARD                                        SQ2024.2
005900     BLOCK CONTAINS 120 CHARACTERS.                               SQ2024.2
006000 01  SQ-FS1R1-F-G-120 PIC X(120).                                 SQ2024.2
006100 WORKING-STORAGE SECTION.                                         SQ2024.2
006200 77  RECORD-OUT-COUNT   PIC 999.                                  SQ2024.2
006300 77  RECORDS-IN-ERROR   PIC 999.                                  SQ2024.2
006400 01  COUNT-OF-RECS PIC 9999.                                      SQ2024.2
006500 01  FILE-RECORD-INFORMATION-REC.                                 SQ2024.2
006600     03 FILE-RECORD-INFO-SKELETON.                                SQ2024.2
006700        05 FILLER                 PICTURE X(48)       VALUE       SQ2024.2
006800             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  SQ2024.2
006900        05 FILLER                 PICTURE X(46)       VALUE       SQ2024.2
007000             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    SQ2024.2
007100        05 FILLER                 PICTURE X(26)       VALUE       SQ2024.2
007200             ",LFIL=000000,ORG=  ,LBLR= ".                        SQ2024.2
007300        05 FILLER                 PICTURE X(37)       VALUE       SQ2024.2
007400             ",RECKEY=                             ".             SQ2024.2
007500        05 FILLER                 PICTURE X(38)       VALUE       SQ2024.2
007600             ",ALTKEY1=                             ".            SQ2024.2
007700        05 FILLER                 PICTURE X(38)       VALUE       SQ2024.2
007800             ",ALTKEY2=                             ".            SQ2024.2
007900        05 FILLER                 PICTURE X(7)        VALUE SPACE.SQ2024.2
008000     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              SQ2024.2
008100        05 FILE-RECORD-INFO-P1-120.                               SQ2024.2
008200           07 FILLER              PIC X(5).                       SQ2024.2
008300           07 XFILE-NAME           PIC X(6).                      SQ2024.2
008400           07 FILLER              PIC X(8).                       SQ2024.2
008500           07 XRECORD-NAME         PIC X(6).                      SQ2024.2
008600           07 FILLER              PIC X(1).                       SQ2024.2
008700           07 REELUNIT-NUMBER     PIC 9(1).                       SQ2024.2
008800           07 FILLER              PIC X(7).                       SQ2024.2
008900           07 XRECORD-NUMBER       PIC 9(6).                      SQ2024.2
009000           07 FILLER              PIC X(6).                       SQ2024.2
009100           07 UPDATE-NUMBER       PIC 9(2).                       SQ2024.2
009200           07 FILLER              PIC X(5).                       SQ2024.2
009300           07 ODO-NUMBER          PIC 9(4).                       SQ2024.2
009400           07 FILLER              PIC X(5).                       SQ2024.2
009500           07 XPROGRAM-NAME        PIC X(5).                      SQ2024.2
009600           07 FILLER              PIC X(7).                       SQ2024.2
009700           07 XRECORD-LENGTH       PIC 9(6).                      SQ2024.2
009800           07 FILLER              PIC X(7).                       SQ2024.2
009900           07 CHARS-OR-RECORDS    PIC X(2).                       SQ2024.2
010000           07 FILLER              PIC X(1).                       SQ2024.2
010100           07 XBLOCK-SIZE          PIC 9(4).                      SQ2024.2
010200           07 FILLER              PIC X(6).                       SQ2024.2
010300           07 RECORDS-IN-FILE     PIC 9(6).                       SQ2024.2
010400           07 FILLER              PIC X(5).                       SQ2024.2
010500           07 XFILE-ORGANIZATION   PIC X(2).                      SQ2024.2
010600           07 FILLER              PIC X(6).                       SQ2024.2
010700           07 XLABEL-TYPE          PIC X(1).                      SQ2024.2
010800        05 FILE-RECORD-INFO-P121-240.                             SQ2024.2
010900           07 FILLER              PIC X(8).                       SQ2024.2
011000           07 XRECORD-KEY          PIC X(29).                     SQ2024.2
011100           07 FILLER              PIC X(9).                       SQ2024.2
011200           07 ALTERNATE-KEY1      PIC X(29).                      SQ2024.2
011300           07 FILLER              PIC X(9).                       SQ2024.2
011400           07 ALTERNATE-KEY2      PIC X(29).                      SQ2024.2
011500           07 FILLER              PIC X(7).                       SQ2024.2
011600 01  TEST-RESULTS.                                                SQ2024.2
011700     02 FILLER                    PICTURE X VALUE SPACE.          SQ2024.2
011800     02 FEATURE                   PICTURE X(20) VALUE SPACE.      SQ2024.2
011900     02 FILLER                    PICTURE X VALUE SPACE.          SQ2024.2
012000     02 P-OR-F                    PICTURE X(5) VALUE SPACE.       SQ2024.2
012100     02 FILLER                    PICTURE X  VALUE SPACE.         SQ2024.2
012200     02  PAR-NAME.                                                SQ2024.2
012300       03 FILLER PICTURE X(12) VALUE SPACE.                       SQ2024.2
012400       03  PARDOT-X PICTURE X  VALUE SPACE.                       SQ2024.2
012500       03 DOTVALUE PICTURE 99  VALUE ZERO.                        SQ2024.2
012600       03 FILLER PIC X(5) VALUE SPACE.                            SQ2024.2
012700     02 FILLER PIC X(10) VALUE SPACE.                             SQ2024.2
012800     02 RE-MARK PIC X(61).                                        SQ2024.2
012900 01  TEST-COMPUTED.                                               SQ2024.2
013000     02 FILLER PIC X(30) VALUE SPACE.                             SQ2024.2
013100     02 FILLER PIC X(17) VALUE "       COMPUTED=".                SQ2024.2
013200     02 COMPUTED-X.                                               SQ2024.2
013300     03 COMPUTED-A                PICTURE X(20) VALUE SPACE.      SQ2024.2
013400     03 COMPUTED-N REDEFINES COMPUTED-A PICTURE -9(9).9(9).       SQ2024.2
013500     03 COMPUTED-0V18 REDEFINES COMPUTED-A  PICTURE -.9(18).      SQ2024.2
013600     03 COMPUTED-4V14 REDEFINES COMPUTED-A  PICTURE -9(4).9(14).  SQ2024.2
013700     03 COMPUTED-14V4 REDEFINES COMPUTED-A  PICTURE -9(14).9(4).  SQ2024.2
013800     03       CM-18V0 REDEFINES COMPUTED-A.                       SQ2024.2
013900         04 COMPUTED-18V0                   PICTURE -9(18).       SQ2024.2
014000         04 FILLER                          PICTURE X.            SQ2024.2
014100     03 FILLER PIC X(50) VALUE SPACE.                             SQ2024.2
014200 01  TEST-CORRECT.                                                SQ2024.2
014300     02 FILLER PIC X(30) VALUE SPACE.                             SQ2024.2
014400     02 FILLER PIC X(17) VALUE "       CORRECT =".                SQ2024.2
014500     02 CORRECT-X.                                                SQ2024.2
014600     03 CORRECT-A                 PICTURE X(20) VALUE SPACE.      SQ2024.2
014700     03 CORRECT-N REDEFINES CORRECT-A PICTURE -9(9).9(9).         SQ2024.2
014800     03 CORRECT-0V18 REDEFINES CORRECT-A    PICTURE -.9(18).      SQ2024.2
014900     03 CORRECT-4V14 REDEFINES CORRECT-A    PICTURE -9(4).9(14).  SQ2024.2
015000     03 CORRECT-14V4 REDEFINES CORRECT-A    PICTURE -9(14).9(4).  SQ2024.2
015100     03      CR-18V0 REDEFINES CORRECT-A.                         SQ2024.2
015200         04 CORRECT-18V0                    PICTURE -9(18).       SQ2024.2
015300         04 FILLER                          PICTURE X.            SQ2024.2
015400     03 FILLER PIC X(50) VALUE SPACE.                             SQ2024.2
015500 01  CCVS-C-1.                                                    SQ2024.2
015600     02 FILLER PICTURE IS X(99) VALUE IS " FEATURE              PASQ2024.2
015700-    "SS  PARAGRAPH-NAME                                          SQ2024.2
015800-    "        REMARKS".                                           SQ2024.2
015900     02 FILLER PICTURE IS X(20) VALUE IS SPACE.                   SQ2024.2
016000 01  CCVS-C-2.                                                    SQ2024.2
016100     02 FILLER PICTURE IS X VALUE IS SPACE.                       SQ2024.2
016200     02 FILLER PICTURE IS X(6) VALUE IS "TESTED".                 SQ2024.2
016300     02 FILLER PICTURE IS X(15) VALUE IS SPACE.                   SQ2024.2
016400     02 FILLER PICTURE IS X(4) VALUE IS "FAIL".                   SQ2024.2
016500     02 FILLER PICTURE IS X(94) VALUE IS SPACE.                   SQ2024.2
016600 01  REC-SKL-SUB PICTURE 9(2) VALUE ZERO.                         SQ2024.2
016700 01  REC-CT PICTURE 99 VALUE ZERO.                                SQ2024.2
016800 01  DELETE-CNT                   PICTURE 999  VALUE ZERO.        SQ2024.2
016900 01  ERROR-COUNTER PICTURE IS 999 VALUE IS ZERO.                  SQ2024.2
017000 01  INSPECT-COUNTER PIC 999 VALUE ZERO.                          SQ2024.2
017100 01  PASS-COUNTER PIC 999 VALUE ZERO.                             SQ2024.2
017200 01  TOTAL-ERROR PIC 999 VALUE ZERO.                              SQ2024.2
017300 01  ERROR-HOLD PIC 999 VALUE ZERO.                               SQ2024.2
017400 01  DUMMY-HOLD PIC X(120) VALUE SPACE.                           SQ2024.2
017500 01  RECORD-COUNT PIC 9(5) VALUE ZERO.                            SQ2024.2
017600 01  CCVS-H-1.                                                    SQ2024.2
017700     02  FILLER   PICTURE X(27)  VALUE SPACE.                     SQ2024.2
017800     02 FILLER PICTURE X(67) VALUE                                SQ2024.2
017900     " FEDERAL SOFTWARE TESTING CENTER COBOL COMPILER VALIDATION  SQ2024.2
018000-    " SYSTEM".                                                   SQ2024.2
018100     02  FILLER     PICTURE X(26)  VALUE SPACE.                   SQ2024.2
018200 01  CCVS-H-2.                                                    SQ2024.2
018300     02 FILLER PICTURE X(52) VALUE IS                             SQ2024.2
018400     "CCVS85 FSTC COPY, NOT FOR DISTRIBUTION.".                   SQ2024.2
018500     02 FILLER PICTURE IS X(19) VALUE IS "TEST RESULTS SET-  ".   SQ2024.2
018600     02 TEST-ID PICTURE IS X(9).                                  SQ2024.2
018700     02 FILLER PICTURE IS X(40) VALUE IS SPACE.                   SQ2024.2
018800 01  CCVS-H-3.                                                    SQ2024.2
018900     02  FILLER PICTURE X(34) VALUE                               SQ2024.2
019000     " FOR OFFICIAL USE ONLY    ".                                SQ2024.2
019100     02  FILLER PICTURE X(58) VALUE                               SQ2024.2
019200     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ2024.2
019300     02  FILLER PICTURE X(28) VALUE                               SQ2024.2
019400     "  COPYRIGHT   1985 ".                                       SQ2024.2
019500 01  CCVS-E-1.                                                    SQ2024.2
019600     02 FILLER PICTURE IS X(52) VALUE IS SPACE.                   SQ2024.2
019700     02 FILLER PICTURE IS X(14) VALUE IS "END OF TEST-  ".        SQ2024.2
019800     02 ID-AGAIN PICTURE IS X(9).                                 SQ2024.2
019900     02 FILLER PICTURE X(45) VALUE IS                             SQ2024.2
020000     " NTIS DISTRIBUTION COBOL 85".                               SQ2024.2
020100 01  CCVS-E-2.                                                    SQ2024.2
020200     02  FILLER                   PICTURE X(31)  VALUE            SQ2024.2
020300     SPACE.                                                       SQ2024.2
020400     02  FILLER                   PICTURE X(21)  VALUE SPACE.     SQ2024.2
020500     02 CCVS-E-2-2.                                               SQ2024.2
020600         03 ERROR-TOTAL PICTURE IS XXX VALUE IS SPACE.            SQ2024.2
020700         03 FILLER PICTURE IS X VALUE IS SPACE.                   SQ2024.2
020800         03 ENDER-DESC PIC X(46) VALUE "ERRORS ENCOUNTERED".      SQ2024.2
020900 01  CCVS-E-3.                                                    SQ2024.2
021000     02  FILLER PICTURE X(22) VALUE                               SQ2024.2
021100     " FOR OFFICIAL USE ONLY".                                    SQ2024.2
021200     02  FILLER PICTURE X(12) VALUE SPACE.                        SQ2024.2
021300     02  FILLER PICTURE X(58) VALUE                               SQ2024.2
021400     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ2024.2
021500     02  FILLER PICTURE X(13) VALUE SPACE.                        SQ2024.2
021600     02 FILLER PIC X(15) VALUE " COPYRIGHT 1985".                 SQ2024.2
021700 01  CCVS-E-4.                                                    SQ2024.2
021800     02 CCVS-E-4-1 PIC XXX VALUE SPACE.                           SQ2024.2
021900     02 FILLER PIC XXXX VALUE " OF ".                             SQ2024.2
022000     02 CCVS-E-4-2 PIC XXX VALUE SPACE.                           SQ2024.2
022100     02 FILLER PIC X(40) VALUE                                    SQ2024.2
022200      "  TESTS WERE EXECUTED SUCCESSFULLY".                       SQ2024.2
022300 01  XXINFO.                                                      SQ2024.2
022400     02 FILLER PIC X(30) VALUE "        *** INFORMATION  ***".    SQ2024.2
022500     02 INFO-TEXT.                                                SQ2024.2
022600     04 FILLER PIC X(20) VALUE SPACE.                             SQ2024.2
022700     04 XXCOMPUTED PIC X(20).                                     SQ2024.2
022800     04 FILLER PIC X(5) VALUE SPACE.                              SQ2024.2
022900     04 XXCORRECT PIC X(20).                                      SQ2024.2
023000 01  HYPHEN-LINE.                                                 SQ2024.2
023100     02 FILLER PICTURE IS X VALUE IS SPACE.                       SQ2024.2
023200     02 FILLER PICTURE IS X(65) VALUE IS "************************SQ2024.2
023300-    "*****************************************".                 SQ2024.2
023400     02 FILLER PICTURE IS X(54) VALUE IS "************************SQ2024.2
023500-    "******************************".                            SQ2024.2
023600 01  CCVS-PGM-ID PIC X(6) VALUE                                   SQ2024.2
023700     "SQ202A".                                                    SQ2024.2
023800 PROCEDURE DIVISION.                                              SQ2024.2
023900 CCVS1 SECTION.                                                   SQ2024.2
024000 OPEN-FILES.                                                      SQ2024.2
024100*    OPEN I-O RAW-DATA.                                           SQ2024.2
024200*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            SQ2024.2
024300*    READ RAW-DATA INVALID KEY GO TO END-E-1.                     SQ2024.2
024400*    MOVE "ABORTED " TO C-ABORT.                                  SQ2024.2
024500*    ADD 1 TO C-NO-OF-TESTS.                                      SQ2024.2
024600*    ACCEPT C-DATE  FROM DATE.                                    SQ2024.2
024700*    ACCEPT C-TIME  FROM TIME.                                    SQ2024.2
024800*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-1.             SQ2024.2
024900*END-E-1.                                                         SQ2024.2
025000*    CLOSE RAW-DATA.                                              SQ2024.2
025100     OPEN     OUTPUT PRINT-FILE.                                  SQ2024.2
025200     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   SQ2024.2
025300     MOVE    SPACE TO TEST-RESULTS.                               SQ2024.2
025400     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             SQ2024.2
025500     MOVE ZERO TO REC-SKL-SUB.                                    SQ2024.2
025600     PERFORM CCVS-INIT-FILE 9 TIMES.                              SQ2024.2
025700 CCVS-INIT-FILE.                                                  SQ2024.2
025800     ADD 1 TO REC-SKL-SUB.                                        SQ2024.2
025900     MOVE FILE-RECORD-INFO-SKELETON TO                            SQ2024.2
026000                  FILE-RECORD-INFO (REC-SKL-SUB).                 SQ2024.2
026100 CCVS-INIT-EXIT.                                                  SQ2024.2
026200     GO TO CCVS1-EXIT.                                            SQ2024.2
026300 CLOSE-FILES.                                                     SQ2024.2
026400     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   SQ2024.2
026500*    OPEN I-O RAW-DATA.                                           SQ2024.2
026600*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            SQ2024.2
026700*    READ RAW-DATA INVALID KEY GO TO END-E-2.                     SQ2024.2
026800*    MOVE "OK.     " TO C-ABORT.                                  SQ2024.2
026900*    MOVE PASS-COUNTER TO C-OK.                                   SQ2024.2
027000*    MOVE ERROR-HOLD   TO C-ALL.                                  SQ2024.2
027100*    MOVE ERROR-COUNTER TO C-FAIL.                                SQ2024.2
027200*    MOVE DELETE-CNT TO C-DELETED.                                SQ2024.2
027300*    MOVE INSPECT-COUNTER TO C-INSPECT.                           SQ2024.2
027400*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-2.             SQ2024.2
027500*END-E-2.                                                         SQ2024.2
027600*    CLOSE RAW-DATA.                                              SQ2024.2
027700 TERMINATE-CCVS.                                                  SQ2024.2
027800*    EXIT PROGRAM.                                                SQ2024.2
027900*TERMINATE-CALL.                                                  SQ2024.2
028000     STOP     RUN.                                                SQ2024.2
028100 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         SQ2024.2
028200 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           SQ2024.2
028300 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          SQ2024.2
028400 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-CNT.          SQ2024.2
028500     MOVE "****TEST DELETED****" TO RE-MARK.                      SQ2024.2
028600 PRINT-DETAIL.                                                    SQ2024.2
028700     IF REC-CT NOT EQUAL TO ZERO                                  SQ2024.2
028800             MOVE "." TO PARDOT-X                                 SQ2024.2
028900             MOVE REC-CT TO DOTVALUE.                             SQ2024.2
029000     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      SQ2024.2
029100     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               SQ2024.2
029200        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 SQ2024.2
029300          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 SQ2024.2
029400     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              SQ2024.2
029500     MOVE SPACE TO CORRECT-X.                                     SQ2024.2
029600     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         SQ2024.2
029700     MOVE     SPACE TO RE-MARK.                                   SQ2024.2
029800 HEAD-ROUTINE.                                                    SQ2024.2
029900     MOVE CCVS-H-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ2024.2
030000     MOVE CCVS-H-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.   SQ2024.2
030100     MOVE CCVS-H-3 TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.   SQ2024.2
030200 COLUMN-NAMES-ROUTINE.                                            SQ2024.2
030300     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ2024.2
030400     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ2024.2
030500     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ2024.2
030600 END-ROUTINE.                                                     SQ2024.2
030700     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.SQ2024.2
030800 END-RTN-EXIT.                                                    SQ2024.2
030900     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ2024.2
031000 END-ROUTINE-1.                                                   SQ2024.2
031100      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      SQ2024.2
031200      ERROR-HOLD. ADD DELETE-CNT TO ERROR-HOLD.                   SQ2024.2
031300      ADD PASS-COUNTER TO ERROR-HOLD.                             SQ2024.2
031400*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   SQ2024.2
031500      MOVE PASS-COUNTER TO CCVS-E-4-1.                            SQ2024.2
031600      MOVE ERROR-HOLD TO CCVS-E-4-2.                              SQ2024.2
031700      MOVE CCVS-E-4 TO CCVS-E-2-2.                                SQ2024.2
031800      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           SQ2024.2
031900  END-ROUTINE-12.                                                 SQ2024.2
032000      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        SQ2024.2
032100     IF       ERROR-COUNTER IS EQUAL TO ZERO                      SQ2024.2
032200         MOVE "NO " TO ERROR-TOTAL                                SQ2024.2
032300         ELSE                                                     SQ2024.2
032400         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       SQ2024.2
032500     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           SQ2024.2
032600     PERFORM WRITE-LINE.                                          SQ2024.2
032700 END-ROUTINE-13.                                                  SQ2024.2
032800     IF DELETE-CNT IS EQUAL TO ZERO                               SQ2024.2
032900         MOVE "NO " TO ERROR-TOTAL  ELSE                          SQ2024.2
033000         MOVE DELETE-CNT TO ERROR-TOTAL.                          SQ2024.2
033100     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   SQ2024.2
033200     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ2024.2
033300      IF   INSPECT-COUNTER EQUAL TO ZERO                          SQ2024.2
033400          MOVE "NO " TO ERROR-TOTAL                               SQ2024.2
033500      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   SQ2024.2
033600      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            SQ2024.2
033700      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          SQ2024.2
033800     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ2024.2
033900 WRITE-LINE.                                                      SQ2024.2
034000     ADD 1 TO RECORD-COUNT.                                       SQ2024.2
034100*    IF RECORD-COUNT GREATER 50                                   SQ2024.2
034200*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          SQ2024.2
034300*        MOVE SPACE TO DUMMY-RECORD                               SQ2024.2
034400*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  SQ2024.2
034500*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             SQ2024.2
034600*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     SQ2024.2
034700*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          SQ2024.2
034800*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          SQ2024.2
034900*        MOVE ZERO TO RECORD-COUNT.                               SQ2024.2
035000     PERFORM WRT-LN.                                              SQ2024.2
035100 WRT-LN.                                                          SQ2024.2
035200     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               SQ2024.2
035300     MOVE SPACE TO DUMMY-RECORD.                                  SQ2024.2
035400 BLANK-LINE-PRINT.                                                SQ2024.2
035500     PERFORM WRT-LN.                                              SQ2024.2
035600 FAIL-ROUTINE.                                                    SQ2024.2
035700     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.   SQ2024.2
035800     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.    SQ2024.2
035900     MOVE "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.    SQ2024.2
036000     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     SQ2024.2
036100     GO TO FAIL-ROUTINE-EX.                                       SQ2024.2
036200 FAIL-ROUTINE-WRITE.                                              SQ2024.2
036300     MOVE TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE           SQ2024.2
036400     MOVE TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES.   SQ2024.2
036500 FAIL-ROUTINE-EX. EXIT.                                           SQ2024.2
036600 BAIL-OUT.                                                        SQ2024.2
036700     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.       SQ2024.2
036800     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.               SQ2024.2
036900 BAIL-OUT-WRITE.                                                  SQ2024.2
037000     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  SQ2024.2
037100     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     SQ2024.2
037200 BAIL-OUT-EX. EXIT.                                               SQ2024.2
037300 CCVS1-EXIT.                                                      SQ2024.2
037400     EXIT.                                                        SQ2024.2
037500 SECTION-SQ202A-0001 SECTION.                                     SQ2024.2
037600 WRI-INIT-001.                                                    SQ2024.2
037700     MOVE "SQ-FS1" TO XFILE-NAME (1).                             SQ2024.2
037800     MOVE "R1-F-G" TO XRECORD-NAME (1).                           SQ2024.2
037900     MOVE "SQ202A" TO XPROGRAM-NAME (1).                          SQ2024.2
038000     MOVE 120      TO XRECORD-LENGTH (1).                         SQ2024.2
038100     MOVE "RC"     TO CHARS-OR-RECORDS (1).                       SQ2024.2
038200     MOVE 1        TO XBLOCK-SIZE (1).                            SQ2024.2
038300     MOVE 750      TO RECORDS-IN-FILE (1).                        SQ2024.2
038400     MOVE "SQ"     TO XFILE-ORGANIZATION (1).                     SQ2024.2
038500     MOVE "S"      TO XLABEL-TYPE (1).                            SQ2024.2
038600     MOVE 1 TO XRECORD-NUMBER (1).                                SQ2024.2
038700     OPEN OUTPUT SQ-FS1.                                          SQ2024.2
038800 WRI-TEST-001.                                                    SQ2024.2
038900     MOVE FILE-RECORD-INFO-P1-120 (1) TO SQ-FS1R1-F-G-120.        SQ2024.2
039000     WRITE SQ-FS1R1-F-G-120.                                      SQ2024.2
039100     IF XRECORD-NUMBER (1) EQUAL TO 750                           SQ2024.2
039200              GO TO WRI-WRITE-001.                                SQ2024.2
039300     ADD 1 TO XRECORD-NUMBER (1).                                 SQ2024.2
039400     GO TO WRI-TEST-001.                                          SQ2024.2
039500 WRI-WRITE-001.                                                   SQ2024.2
039600     MOVE "CREATE FILE SQ-FS1" TO FEATURE.                        SQ2024.2
039700     MOVE "WRI-TEST-001" TO PAR-NAME.                             SQ2024.2
039800     MOVE "FILE CREATED, RECS =" TO COMPUTED-A.                   SQ2024.2
039900     MOVE XRECORD-NUMBER (1) TO CORRECT-18V0.                     SQ2024.2
040000     PERFORM PRINT-DETAIL.                                        SQ2024.2
040100 WRI-CLOSE-001.                                                   SQ2024.2
040200     CLOSE SQ-FS1.                                                SQ2024.2
040300 READ-INIT-001.                                                   SQ2024.2
040400     MOVE 0 TO RECORD-OUT-COUNT, RECORDS-IN-ERROR.                SQ2024.2
040500     OPEN INPUT SQ-FS1.                                           SQ2024.2
040600 READ-TEST-001.                                                   SQ2024.2
040700     READ SQ-FS1 AT END GO TO READ-TEST-001-01.                   SQ2024.2
040800     MOVE SQ-FS1R1-F-G-120 TO FILE-RECORD-INFO-P1-120 (1).        SQ2024.2
040900     ADD 1 TO RECORD-OUT-COUNT                                    SQ2024.2
041000     IF RECORD-OUT-COUNT GREATER THAN 750                         SQ2024.2
041100              MOVE "MORE THAN 750 RECORDS" TO RE-MARK             SQ2024.2
041200              GO TO READ-FAIL-001.                                SQ2024.2
041300     IF RECORD-OUT-COUNT NOT EQUAL TO XRECORD-NUMBER (1)          SQ2024.2
041400              ADD 1 TO RECORDS-IN-ERROR                           SQ2024.2
041500              GO TO READ-TEST-001.                                SQ2024.2
041600     IF XFILE-NAME (1) NOT EQUAL TO "SQ-FS1"                      SQ2024.2
041700              ADD 1 TO RECORDS-IN-ERROR                           SQ2024.2
041800              GO TO READ-TEST-001.                                SQ2024.2
041900     IF XLABEL-TYPE (1) NOT EQUAL TO "S"                          SQ2024.2
042000              ADD 1 TO RECORDS-IN-ERROR.                          SQ2024.2
042100     GO TO READ-TEST-001.                                         SQ2024.2
042200 READ-TEST-001-01.                                                SQ2024.2
042300     IF RECORDS-IN-ERROR EQUAL TO ZERO                            SQ2024.2
042400              GO TO READ-PASS-001.                                SQ2024.2
042500     MOVE "ERRORS IN READING SQ-FS1" TO RE-MARK.                  SQ2024.2
042600 READ-FAIL-001.                                                   SQ2024.2
042700     MOVE "RECORDS IN ERROR" TO COMPUTED-A.                       SQ2024.2
042800     MOVE RECORDS-IN-ERROR TO CORRECT-18V0.                       SQ2024.2
042900     PERFORM FAIL.                                                SQ2024.2
043000     GO TO READ-WRITE-001.                                        SQ2024.2
043100 READ-PASS-001.                                                   SQ2024.2
043200     PERFORM PASS.                                                SQ2024.2
043300     MOVE "FILE VERIFIED RECS =" TO COMPUTED-A.                   SQ2024.2
043400     MOVE RECORD-OUT-COUNT TO CORRECT-18V0.                       SQ2024.2
043500 READ-WRITE-001.                                                  SQ2024.2
043600     MOVE "READ-TEST-001" TO PAR-NAME.                            SQ2024.2
043700     MOVE "VERIFY FILE SQ-FS1" TO FEATURE.                        SQ2024.2
043800     PERFORM PRINT-DETAIL.                                        SQ2024.2
043900 READ-CLOSE-001.                                                  SQ2024.2
044000     CLOSE SQ-FS1.                                                SQ2024.2
044100 SQ202A-END-ROUTINE.                                              SQ2024.2
044200     MOVE "END OF SQ202A VALIDATION TESTS" TO PRINT-REC.          SQ2024.2
044300     WRITE PRINT-REC AFTER ADVANCING 1 LINE.                      SQ2024.2
044400 TERMINATE-SQ202A.                                                SQ2024.2
044500     EXIT.                                                        SQ2024.2
044600 CCVS-EXIT SECTION.                                               SQ2024.2
044700 CCVS-999999.                                                     SQ2024.2
044800     GO TO CLOSE-FILES.                                           SQ2024.2
      *END-OF,SQ202A                                                                  
