      *HEADER,COBOL,SQ206A                                                            
000100 IDENTIFICATION DIVISION.                                         SQ2064.2
000200 PROGRAM-ID.                                                      SQ2064.2
000300     SQ206A.                                                      SQ2064.2
000400****************************************************************  SQ2064.2
000500*                                                              *  SQ2064.2
000600*    VALIDATION FOR:-                                          *  SQ2064.2
000700*    " HIGH       ".                                              SQ2064.2
000800*                                                              *  SQ2064.2
000900*    CREATION DATE     /     VALIDATION DATE                   *  SQ2064.2
001000*    "4.2 ".                                                      SQ2064.2
001100*                                                              *  SQ2064.2
001200*         THE ROUTINE SQ206A TESTS THE USE OF THE CLAUSES SAME    SQ2064.2
001300*    RECORD AREA AND SAME AREA OF THE I-O-CONTROL PARAGRAPH.      SQ2064.2
001400*    TAPE FILES AND MASS-STORAGE FILES ARE CREATED WHICH          SQ2064.2
001500*    REFERENCE THE SAME RECORD AREA OR ARE NAMED IN A SAME AREA   SQ2064.2
001600*    CLAUSE.  THE FILES ARE PROCESSED AND THE CONTENTS OF THE     SQ2064.2
001700*    RECORDS VERIFIED AGAINST THE EXPECTED RESULTS.               SQ2064.2
001800 ENVIRONMENT DIVISION.                                            SQ2064.2
001900 CONFIGURATION SECTION.                                           SQ2064.2
002000 SOURCE-COMPUTER.                                                 SQ2064.2
002100     XXXXX082.                                                    SQ2064.2
002200 OBJECT-COMPUTER.                                                 SQ2064.2
002300     XXXXX083.                                                    SQ2064.2
002400 INPUT-OUTPUT SECTION.                                            SQ2064.2
002500 FILE-CONTROL.                                                    SQ2064.2
002600*    SELECT RAW-DATA   ASSIGN TO                                  SQ2064.2
002700*    XXXXX062                                                     SQ2064.2
002800*           ORGANIZATION IS INDEXED                               SQ2064.2
002900*           ACCESS MODE IS RANDOM                                 SQ2064.2
003000*           RECORD KEY IS RAW-DATA-KEY.                           SQ2064.2
003100     SELECT PRINT-FILE ASSIGN TO                                  SQ2064.2
003200     XXXXX055.                                                    SQ2064.2
003300     SELECT SQ-FS1 ASSIGN TO                                      SQ2064.2
003400     XXXXX001                                                     SQ2064.2
003500     ORGANIZATION SEQUENTIAL.                                     SQ2064.2
003600     SELECT SQ-FS2 ASSIGN                                         SQ2064.2
003700     XXXXX014                                                     SQ2064.2
003800     ACCESS IS SEQUENTIAL.                                        SQ2064.2
003900     SELECT SQ-FS3 ASSIGN TO                                      SQ2064.2
004000     XXXXX015                                                     SQ2064.2
004100     ORGANIZATION IS SEQUENTIAL                                   SQ2064.2
004200     ACCESS MODE SEQUENTIAL.                                      SQ2064.2
004300     SELECT SQ-FS4 ASSIGN                                         SQ2064.2
004400     XXXXX002                                                     SQ2064.2
004500     ORGANIZATION SEQUENTIAL                                      SQ2064.2
004600     ACCESS SEQUENTIAL.                                           SQ2064.2
004700 I-O-CONTROL.                                                     SQ2064.2
004800     SAME SQ-FS1, SQ-FS2                                          SQ2064.2
004900     SAME RECORD AREA FOR SQ-FS1, SQ-FS2, SQ-FS3, SQ-FS4.         SQ2064.2
005000 DATA DIVISION.                                                   SQ2064.2
005100 FILE SECTION.                                                    SQ2064.2
005200*                                                                 SQ2064.2
005300*FD  RAW-DATA.                                                    SQ2064.2
005400*                                                                 SQ2064.2
005500*01  RAW-DATA-SATZ.                                               SQ2064.2
005600*    05  RAW-DATA-KEY        PIC X(6).                            SQ2064.2
005700*    05  C-DATE              PIC 9(6).                            SQ2064.2
005800*    05  C-TIME              PIC 9(8).                            SQ2064.2
005900*    05  C-NO-OF-TESTS       PIC 99.                              SQ2064.2
006000*    05  C-OK                PIC 999.                             SQ2064.2
006100*    05  C-ALL               PIC 999.                             SQ2064.2
006200*    05  C-FAIL              PIC 999.                             SQ2064.2
006300*    05  C-DELETED           PIC 999.                             SQ2064.2
006400*    05  C-INSPECT           PIC 999.                             SQ2064.2
006500*    05  C-NOTE              PIC X(13).                           SQ2064.2
006600*    05  C-INDENT            PIC X.                               SQ2064.2
006700*    05  C-ABORT             PIC X(8).                            SQ2064.2
006800 FD  PRINT-FILE                                                   SQ2064.2
006900C    LABEL RECORDS                                                SQ2064.2
007000C    XXXXX084                                                     SQ2064.2
007100C    DATA RECORD IS PRINT-REC DUMMY-RECORD                        SQ2064.2
007200               .                                                  SQ2064.2
007300 01  PRINT-REC PICTURE X(120).                                    SQ2064.2
007400 01  DUMMY-RECORD PICTURE X(120).                                 SQ2064.2
007500 FD  SQ-FS1                                                       SQ2064.2
007600C    LABEL RECORDS ARE STANDARD                                   SQ2064.2
007700     BLOCK CONTAINS 120 CHARACTERS.                               SQ2064.2
007800 01  SQ-FS1R1-F-G-120   PIC X(120).                               SQ2064.2
007900 FD  SQ-FS2                                                       SQ2064.2
008000C    LABEL RECORD IS STANDARD                                     SQ2064.2
008100                .                                                 SQ2064.2
008200 01  SQ-FS2R1-F-G-120 PIC X(120).                                 SQ2064.2
008300 FD  SQ-FS3                                                       SQ2064.2
008400C    LABEL RECORD STANDARD                                        SQ2064.2
008500                .                                                 SQ2064.2
008600 01  SQ-FS3R1-F-G-120   PIC X(120).                               SQ2064.2
008700 FD  SQ-FS4                                                       SQ2064.2
008800C    LABEL RECORDS STANDARD                                       SQ2064.2
008900                .                                                 SQ2064.2
009000 01  SQ-FS4R1-F-G-120 PIC X(120).                                 SQ2064.2
009100 WORKING-STORAGE SECTION.                                         SQ2064.2
009200 77  WRK-RECORD-COUNT      PIC 9(4) VALUE 0.                      SQ2064.2
009300 77  RECORDS-IN-ERROR   PIC 9(4) VALUE 0.                         SQ2064.2
009400 01  COUNT-OF-RECS PIC 9999.                                      SQ2064.2
009500 01  FILE-RECORD-INFORMATION-REC.                                 SQ2064.2
009600     03 FILE-RECORD-INFO-SKELETON.                                SQ2064.2
009700        05 FILLER                 PICTURE X(48)       VALUE       SQ2064.2
009800             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  SQ2064.2
009900        05 FILLER                 PICTURE X(46)       VALUE       SQ2064.2
010000             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    SQ2064.2
010100        05 FILLER                 PICTURE X(26)       VALUE       SQ2064.2
010200             ",LFIL=000000,ORG=  ,LBLR= ".                        SQ2064.2
010300        05 FILLER                 PICTURE X(37)       VALUE       SQ2064.2
010400             ",RECKEY=                             ".             SQ2064.2
010500        05 FILLER                 PICTURE X(38)       VALUE       SQ2064.2
010600             ",ALTKEY1=                             ".            SQ2064.2
010700        05 FILLER                 PICTURE X(38)       VALUE       SQ2064.2
010800             ",ALTKEY2=                             ".            SQ2064.2
010900        05 FILLER                 PICTURE X(7)        VALUE SPACE.SQ2064.2
011000     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              SQ2064.2
011100        05 FILE-RECORD-INFO-P1-120.                               SQ2064.2
011200           07 FILLER              PIC X(5).                       SQ2064.2
011300           07 XFILE-NAME           PIC X(6).                      SQ2064.2
011400           07 FILLER              PIC X(8).                       SQ2064.2
011500           07 XRECORD-NAME         PIC X(6).                      SQ2064.2
011600           07 FILLER              PIC X(1).                       SQ2064.2
011700           07 REELUNIT-NUMBER     PIC 9(1).                       SQ2064.2
011800           07 FILLER              PIC X(7).                       SQ2064.2
011900           07 XRECORD-NUMBER       PIC 9(6).                      SQ2064.2
012000           07 FILLER              PIC X(6).                       SQ2064.2
012100           07 UPDATE-NUMBER       PIC 9(2).                       SQ2064.2
012200           07 FILLER              PIC X(5).                       SQ2064.2
012300           07 ODO-NUMBER          PIC 9(4).                       SQ2064.2
012400           07 FILLER              PIC X(5).                       SQ2064.2
012500           07 XPROGRAM-NAME        PIC X(5).                      SQ2064.2
012600           07 FILLER              PIC X(7).                       SQ2064.2
012700           07 XRECORD-LENGTH       PIC 9(6).                      SQ2064.2
012800           07 FILLER              PIC X(7).                       SQ2064.2
012900           07 CHARS-OR-RECORDS    PIC X(2).                       SQ2064.2
013000           07 FILLER              PIC X(1).                       SQ2064.2
013100           07 XBLOCK-SIZE          PIC 9(4).                      SQ2064.2
013200           07 FILLER              PIC X(6).                       SQ2064.2
013300           07 RECORDS-IN-FILE     PIC 9(6).                       SQ2064.2
013400           07 FILLER              PIC X(5).                       SQ2064.2
013500           07 XFILE-ORGANIZATION   PIC X(2).                      SQ2064.2
013600           07 FILLER              PIC X(6).                       SQ2064.2
013700           07 XLABEL-TYPE          PIC X(1).                      SQ2064.2
013800        05 FILE-RECORD-INFO-P121-240.                             SQ2064.2
013900           07 FILLER              PIC X(8).                       SQ2064.2
014000           07 XRECORD-KEY          PIC X(29).                     SQ2064.2
014100           07 FILLER              PIC X(9).                       SQ2064.2
014200           07 ALTERNATE-KEY1      PIC X(29).                      SQ2064.2
014300           07 FILLER              PIC X(9).                       SQ2064.2
014400           07 ALTERNATE-KEY2      PIC X(29).                      SQ2064.2
014500           07 FILLER              PIC X(7).                       SQ2064.2
014600 01  TEST-RESULTS.                                                SQ2064.2
014700     02 FILLER                    PICTURE X VALUE SPACE.          SQ2064.2
014800     02 FEATURE                   PICTURE X(20) VALUE SPACE.      SQ2064.2
014900     02 FILLER                    PICTURE X VALUE SPACE.          SQ2064.2
015000     02 P-OR-F                    PICTURE X(5) VALUE SPACE.       SQ2064.2
015100     02 FILLER                    PICTURE X  VALUE SPACE.         SQ2064.2
015200     02  PAR-NAME.                                                SQ2064.2
015300       03 FILLER PICTURE X(12) VALUE SPACE.                       SQ2064.2
015400       03  PARDOT-X PICTURE X  VALUE SPACE.                       SQ2064.2
015500       03 DOTVALUE PICTURE 99  VALUE ZERO.                        SQ2064.2
015600       03 FILLER PIC X(5) VALUE SPACE.                            SQ2064.2
015700     02 FILLER PIC X(10) VALUE SPACE.                             SQ2064.2
015800     02 RE-MARK PIC X(61).                                        SQ2064.2
015900 01  TEST-COMPUTED.                                               SQ2064.2
016000     02 FILLER PIC X(30) VALUE SPACE.                             SQ2064.2
016100     02 FILLER PIC X(17) VALUE "       COMPUTED=".                SQ2064.2
016200     02 COMPUTED-X.                                               SQ2064.2
016300     03 COMPUTED-A                PICTURE X(20) VALUE SPACE.      SQ2064.2
016400     03 COMPUTED-N REDEFINES COMPUTED-A PICTURE -9(9).9(9).       SQ2064.2
016500     03 COMPUTED-0V18 REDEFINES COMPUTED-A  PICTURE -.9(18).      SQ2064.2
016600     03 COMPUTED-4V14 REDEFINES COMPUTED-A  PICTURE -9(4).9(14).  SQ2064.2
016700     03 COMPUTED-14V4 REDEFINES COMPUTED-A  PICTURE -9(14).9(4).  SQ2064.2
016800     03       CM-18V0 REDEFINES COMPUTED-A.                       SQ2064.2
016900         04 COMPUTED-18V0                   PICTURE -9(18).       SQ2064.2
017000         04 FILLER                          PICTURE X.            SQ2064.2
017100     03 FILLER PIC X(50) VALUE SPACE.                             SQ2064.2
017200 01  TEST-CORRECT.                                                SQ2064.2
017300     02 FILLER PIC X(30) VALUE SPACE.                             SQ2064.2
017400     02 FILLER PIC X(17) VALUE "       CORRECT =".                SQ2064.2
017500     02 CORRECT-X.                                                SQ2064.2
017600     03 CORRECT-A                 PICTURE X(20) VALUE SPACE.      SQ2064.2
017700     03 CORRECT-N REDEFINES CORRECT-A PICTURE -9(9).9(9).         SQ2064.2
017800     03 CORRECT-0V18 REDEFINES CORRECT-A    PICTURE -.9(18).      SQ2064.2
017900     03 CORRECT-4V14 REDEFINES CORRECT-A    PICTURE -9(4).9(14).  SQ2064.2
018000     03 CORRECT-14V4 REDEFINES CORRECT-A    PICTURE -9(14).9(4).  SQ2064.2
018100     03      CR-18V0 REDEFINES CORRECT-A.                         SQ2064.2
018200         04 CORRECT-18V0                    PICTURE -9(18).       SQ2064.2
018300         04 FILLER                          PICTURE X.            SQ2064.2
018400     03 FILLER PIC X(50) VALUE SPACE.                             SQ2064.2
018500 01  CCVS-C-1.                                                    SQ2064.2
018600     02 FILLER PICTURE IS X(99) VALUE IS " FEATURE              PASQ2064.2
018700-    "SS  PARAGRAPH-NAME                                          SQ2064.2
018800-    "        REMARKS".                                           SQ2064.2
018900     02 FILLER PICTURE IS X(20) VALUE IS SPACE.                   SQ2064.2
019000 01  CCVS-C-2.                                                    SQ2064.2
019100     02 FILLER PICTURE IS X VALUE IS SPACE.                       SQ2064.2
019200     02 FILLER PICTURE IS X(6) VALUE IS "TESTED".                 SQ2064.2
019300     02 FILLER PICTURE IS X(15) VALUE IS SPACE.                   SQ2064.2
019400     02 FILLER PICTURE IS X(4) VALUE IS "FAIL".                   SQ2064.2
019500     02 FILLER PICTURE IS X(94) VALUE IS SPACE.                   SQ2064.2
019600 01  REC-SKL-SUB PICTURE 9(2) VALUE ZERO.                         SQ2064.2
019700 01  REC-CT PICTURE 99 VALUE ZERO.                                SQ2064.2
019800 01  DELETE-CNT                   PICTURE 999  VALUE ZERO.        SQ2064.2
019900 01  ERROR-COUNTER PICTURE IS 999 VALUE IS ZERO.                  SQ2064.2
020000 01  INSPECT-COUNTER PIC 999 VALUE ZERO.                          SQ2064.2
020100 01  PASS-COUNTER PIC 999 VALUE ZERO.                             SQ2064.2
020200 01  TOTAL-ERROR PIC 999 VALUE ZERO.                              SQ2064.2
020300 01  ERROR-HOLD PIC 999 VALUE ZERO.                               SQ2064.2
020400 01  DUMMY-HOLD PIC X(120) VALUE SPACE.                           SQ2064.2
020500 01  RECORD-COUNT PIC 9(5) VALUE ZERO.                            SQ2064.2
020600 01  CCVS-H-1.                                                    SQ2064.2
020700     02  FILLER   PICTURE X(27)  VALUE SPACE.                     SQ2064.2
020800     02 FILLER PICTURE X(67) VALUE                                SQ2064.2
020900     " FEDERAL SOFTWARE TESTING CENTER COBOL COMPILER VALIDATION  SQ2064.2
021000-    " SYSTEM".                                                   SQ2064.2
021100     02  FILLER     PICTURE X(26)  VALUE SPACE.                   SQ2064.2
021200 01  CCVS-H-2.                                                    SQ2064.2
021300     02 FILLER PICTURE X(52) VALUE IS                             SQ2064.2
021400     "CCVS85 FSTC COPY, NOT FOR DISTRIBUTION.".                   SQ2064.2
021500     02 FILLER PICTURE IS X(19) VALUE IS "TEST RESULTS SET-  ".   SQ2064.2
021600     02 TEST-ID PICTURE IS X(9).                                  SQ2064.2
021700     02 FILLER PICTURE IS X(40) VALUE IS SPACE.                   SQ2064.2
021800 01  CCVS-H-3.                                                    SQ2064.2
021900     02  FILLER PICTURE X(34) VALUE                               SQ2064.2
022000     " FOR OFFICIAL USE ONLY    ".                                SQ2064.2
022100     02  FILLER PICTURE X(58) VALUE                               SQ2064.2
022200     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ2064.2
022300     02  FILLER PICTURE X(28) VALUE                               SQ2064.2
022400     "  COPYRIGHT   1985 ".                                       SQ2064.2
022500 01  CCVS-E-1.                                                    SQ2064.2
022600     02 FILLER PICTURE IS X(52) VALUE IS SPACE.                   SQ2064.2
022700     02 FILLER PICTURE IS X(14) VALUE IS "END OF TEST-  ".        SQ2064.2
022800     02 ID-AGAIN PICTURE IS X(9).                                 SQ2064.2
022900     02 FILLER PICTURE X(45) VALUE IS                             SQ2064.2
023000     " NTIS DISTRIBUTION COBOL 85".                               SQ2064.2
023100 01  CCVS-E-2.                                                    SQ2064.2
023200     02  FILLER                   PICTURE X(31)  VALUE            SQ2064.2
023300     SPACE.                                                       SQ2064.2
023400     02  FILLER                   PICTURE X(21)  VALUE SPACE.     SQ2064.2
023500     02 CCVS-E-2-2.                                               SQ2064.2
023600         03 ERROR-TOTAL PICTURE IS XXX VALUE IS SPACE.            SQ2064.2
023700         03 FILLER PICTURE IS X VALUE IS SPACE.                   SQ2064.2
023800         03 ENDER-DESC PIC X(46) VALUE "ERRORS ENCOUNTERED".      SQ2064.2
023900 01  CCVS-E-3.                                                    SQ2064.2
024000     02  FILLER PICTURE X(22) VALUE                               SQ2064.2
024100     " FOR OFFICIAL USE ONLY".                                    SQ2064.2
024200     02  FILLER PICTURE X(12) VALUE SPACE.                        SQ2064.2
024300     02  FILLER PICTURE X(58) VALUE                               SQ2064.2
024400     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ2064.2
024500     02  FILLER PICTURE X(13) VALUE SPACE.                        SQ2064.2
024600     02 FILLER PIC X(15) VALUE " COPYRIGHT 1985".                 SQ2064.2
024700 01  CCVS-E-4.                                                    SQ2064.2
024800     02 CCVS-E-4-1 PIC XXX VALUE SPACE.                           SQ2064.2
024900     02 FILLER PIC XXXX VALUE " OF ".                             SQ2064.2
025000     02 CCVS-E-4-2 PIC XXX VALUE SPACE.                           SQ2064.2
025100     02 FILLER PIC X(40) VALUE                                    SQ2064.2
025200      "  TESTS WERE EXECUTED SUCCESSFULLY".                       SQ2064.2
025300 01  XXINFO.                                                      SQ2064.2
025400     02 FILLER PIC X(30) VALUE "        *** INFORMATION  ***".    SQ2064.2
025500     02 INFO-TEXT.                                                SQ2064.2
025600     04 FILLER PIC X(20) VALUE SPACE.                             SQ2064.2
025700     04 XXCOMPUTED PIC X(20).                                     SQ2064.2
025800     04 FILLER PIC X(5) VALUE SPACE.                              SQ2064.2
025900     04 XXCORRECT PIC X(20).                                      SQ2064.2
026000 01  HYPHEN-LINE.                                                 SQ2064.2
026100     02 FILLER PICTURE IS X VALUE IS SPACE.                       SQ2064.2
026200     02 FILLER PICTURE IS X(65) VALUE IS "************************SQ2064.2
026300-    "*****************************************".                 SQ2064.2
026400     02 FILLER PICTURE IS X(54) VALUE IS "************************SQ2064.2
026500-    "******************************".                            SQ2064.2
026600 01  CCVS-PGM-ID PIC X(6) VALUE                                   SQ2064.2
026700     "SQ206A".                                                    SQ2064.2
026800 PROCEDURE DIVISION.                                              SQ2064.2
026900 CCVS1 SECTION.                                                   SQ2064.2
027000 OPEN-FILES.                                                      SQ2064.2
027100*    OPEN I-O RAW-DATA.                                           SQ2064.2
027200*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            SQ2064.2
027300*    READ RAW-DATA INVALID KEY GO TO END-E-1.                     SQ2064.2
027400*    MOVE "ABORTED " TO C-ABORT.                                  SQ2064.2
027500*    ADD 1 TO C-NO-OF-TESTS.                                      SQ2064.2
027600*    ACCEPT C-DATE  FROM DATE.                                    SQ2064.2
027700*    ACCEPT C-TIME  FROM TIME.                                    SQ2064.2
027800*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-1.             SQ2064.2
027900*END-E-1.                                                         SQ2064.2
028000*    CLOSE RAW-DATA.                                              SQ2064.2
028100     OPEN     OUTPUT PRINT-FILE.                                  SQ2064.2
028200     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   SQ2064.2
028300     MOVE    SPACE TO TEST-RESULTS.                               SQ2064.2
028400     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             SQ2064.2
028500     MOVE ZERO TO REC-SKL-SUB.                                    SQ2064.2
028600     PERFORM CCVS-INIT-FILE 9 TIMES.                              SQ2064.2
028700 CCVS-INIT-FILE.                                                  SQ2064.2
028800     ADD 1 TO REC-SKL-SUB.                                        SQ2064.2
028900     MOVE FILE-RECORD-INFO-SKELETON TO                            SQ2064.2
029000                  FILE-RECORD-INFO (REC-SKL-SUB).                 SQ2064.2
029100 CCVS-INIT-EXIT.                                                  SQ2064.2
029200     GO TO CCVS1-EXIT.                                            SQ2064.2
029300 CLOSE-FILES.                                                     SQ2064.2
029400     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   SQ2064.2
029500*    OPEN I-O RAW-DATA.                                           SQ2064.2
029600*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            SQ2064.2
029700*    READ RAW-DATA INVALID KEY GO TO END-E-2.                     SQ2064.2
029800*    MOVE "OK.     " TO C-ABORT.                                  SQ2064.2
029900*    MOVE PASS-COUNTER TO C-OK.                                   SQ2064.2
030000*    MOVE ERROR-HOLD   TO C-ALL.                                  SQ2064.2
030100*    MOVE ERROR-COUNTER TO C-FAIL.                                SQ2064.2
030200*    MOVE DELETE-CNT TO C-DELETED.                                SQ2064.2
030300*    MOVE INSPECT-COUNTER TO C-INSPECT.                           SQ2064.2
030400*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-2.             SQ2064.2
030500*END-E-2.                                                         SQ2064.2
030600*    CLOSE RAW-DATA.                                              SQ2064.2
030700 TERMINATE-CCVS.                                                  SQ2064.2
030800*    EXIT PROGRAM.                                                SQ2064.2
030900*TERMINATE-CALL.                                                  SQ2064.2
031000     STOP     RUN.                                                SQ2064.2
031100 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         SQ2064.2
031200 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           SQ2064.2
031300 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          SQ2064.2
031400 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-CNT.          SQ2064.2
031500     MOVE "****TEST DELETED****" TO RE-MARK.                      SQ2064.2
031600 PRINT-DETAIL.                                                    SQ2064.2
031700     IF REC-CT NOT EQUAL TO ZERO                                  SQ2064.2
031800             MOVE "." TO PARDOT-X                                 SQ2064.2
031900             MOVE REC-CT TO DOTVALUE.                             SQ2064.2
032000     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      SQ2064.2
032100     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               SQ2064.2
032200        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 SQ2064.2
032300          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 SQ2064.2
032400     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              SQ2064.2
032500     MOVE SPACE TO CORRECT-X.                                     SQ2064.2
032600     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         SQ2064.2
032700     MOVE     SPACE TO RE-MARK.                                   SQ2064.2
032800 HEAD-ROUTINE.                                                    SQ2064.2
032900     MOVE CCVS-H-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ2064.2
033000     MOVE CCVS-H-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.   SQ2064.2
033100     MOVE CCVS-H-3 TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.   SQ2064.2
033200 COLUMN-NAMES-ROUTINE.                                            SQ2064.2
033300     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ2064.2
033400     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ2064.2
033500     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ2064.2
033600 END-ROUTINE.                                                     SQ2064.2
033700     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.SQ2064.2
033800 END-RTN-EXIT.                                                    SQ2064.2
033900     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ2064.2
034000 END-ROUTINE-1.                                                   SQ2064.2
034100      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      SQ2064.2
034200      ERROR-HOLD. ADD DELETE-CNT TO ERROR-HOLD.                   SQ2064.2
034300      ADD PASS-COUNTER TO ERROR-HOLD.                             SQ2064.2
034400*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   SQ2064.2
034500      MOVE PASS-COUNTER TO CCVS-E-4-1.                            SQ2064.2
034600      MOVE ERROR-HOLD TO CCVS-E-4-2.                              SQ2064.2
034700      MOVE CCVS-E-4 TO CCVS-E-2-2.                                SQ2064.2
034800      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           SQ2064.2
034900  END-ROUTINE-12.                                                 SQ2064.2
035000      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        SQ2064.2
035100     IF       ERROR-COUNTER IS EQUAL TO ZERO                      SQ2064.2
035200         MOVE "NO " TO ERROR-TOTAL                                SQ2064.2
035300         ELSE                                                     SQ2064.2
035400         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       SQ2064.2
035500     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           SQ2064.2
035600     PERFORM WRITE-LINE.                                          SQ2064.2
035700 END-ROUTINE-13.                                                  SQ2064.2
035800     IF DELETE-CNT IS EQUAL TO ZERO                               SQ2064.2
035900         MOVE "NO " TO ERROR-TOTAL  ELSE                          SQ2064.2
036000         MOVE DELETE-CNT TO ERROR-TOTAL.                          SQ2064.2
036100     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   SQ2064.2
036200     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ2064.2
036300      IF   INSPECT-COUNTER EQUAL TO ZERO                          SQ2064.2
036400          MOVE "NO " TO ERROR-TOTAL                               SQ2064.2
036500      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   SQ2064.2
036600      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            SQ2064.2
036700      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          SQ2064.2
036800     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ2064.2
036900 WRITE-LINE.                                                      SQ2064.2
037000     ADD 1 TO RECORD-COUNT.                                       SQ2064.2
037100*    IF RECORD-COUNT GREATER 50                                   SQ2064.2
037200*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          SQ2064.2
037300*        MOVE SPACE TO DUMMY-RECORD                               SQ2064.2
037400*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  SQ2064.2
037500*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             SQ2064.2
037600*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     SQ2064.2
037700*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          SQ2064.2
037800*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          SQ2064.2
037900*        MOVE ZERO TO RECORD-COUNT.                               SQ2064.2
038000     PERFORM WRT-LN.                                              SQ2064.2
038100 WRT-LN.                                                          SQ2064.2
038200     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               SQ2064.2
038300     MOVE SPACE TO DUMMY-RECORD.                                  SQ2064.2
038400 BLANK-LINE-PRINT.                                                SQ2064.2
038500     PERFORM WRT-LN.                                              SQ2064.2
038600 FAIL-ROUTINE.                                                    SQ2064.2
038700     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.   SQ2064.2
038800     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.    SQ2064.2
038900     MOVE "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.    SQ2064.2
039000     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     SQ2064.2
039100     GO TO FAIL-ROUTINE-EX.                                       SQ2064.2
039200 FAIL-ROUTINE-WRITE.                                              SQ2064.2
039300     MOVE TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE           SQ2064.2
039400     MOVE TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES.   SQ2064.2
039500 FAIL-ROUTINE-EX. EXIT.                                           SQ2064.2
039600 BAIL-OUT.                                                        SQ2064.2
039700     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.       SQ2064.2
039800     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.               SQ2064.2
039900 BAIL-OUT-WRITE.                                                  SQ2064.2
040000     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  SQ2064.2
040100     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     SQ2064.2
040200 BAIL-OUT-EX. EXIT.                                               SQ2064.2
040300 CCVS1-EXIT.                                                      SQ2064.2
040400     EXIT.                                                        SQ2064.2
040500 SECT-SQ206A-0001 SECTION.                                        SQ2064.2
040600 WRITE-INIT-GF-01.                                                SQ2064.2
040700*             IN THIS TEST TWO FILES ARE CREATED USING THE SAME   SQ2064.2
040800*             RECORD AREA.  THE LOGICAL RECORD WRITTEN ON SQ-FS1  SQ2064.2
040900*             REMAINS IN THE RECORD AREA TO BE WRITTEN ON SQ-FS3. SQ2064.2
041000*             ONLY THE FILE NAMES CHANGE.                         SQ2064.2
041100     MOVE "R1-F-G" TO XRECORD-NAME (1).                           SQ2064.2
041200     MOVE "SQ206"  TO XPROGRAM-NAME (1).                          SQ2064.2
041300     MOVE 120      TO XRECORD-LENGTH (1).                         SQ2064.2
041400     MOVE "RC"     TO CHARS-OR-RECORDS (1).                       SQ2064.2
041500     MOVE 1        TO XBLOCK-SIZE (1).                            SQ2064.2
041600     MOVE 750      TO RECORDS-IN-FILE (1).                        SQ2064.2
041700     MOVE "SQ"     TO XFILE-ORGANIZATION (1).                     SQ2064.2
041800     MOVE "S"      TO XLABEL-TYPE (1).                            SQ2064.2
041900     MOVE 1        TO XRECORD-NUMBER (1).                         SQ2064.2
042000     OPEN OUTPUT SQ-FS1, SQ-FS3.                                  SQ2064.2
042100 WRITE-TEST-GF-01.                                                SQ2064.2
042200     MOVE "SQ-FS1" TO XFILE-NAME (1).                             SQ2064.2
042300     MOVE FILE-RECORD-INFO-P1-120 (1) TO SQ-FS1R1-F-G-120.        SQ2064.2
042400     WRITE SQ-FS1R1-F-G-120.                                      SQ2064.2
042500     MOVE "SQ-FS3" TO XFILE-NAME (1).                             SQ2064.2
042600     MOVE FILE-RECORD-INFO-P1-120 (1) TO SQ-FS3R1-F-G-120.        SQ2064.2
042700     WRITE SQ-FS3R1-F-G-120.                                      SQ2064.2
042800     IF XRECORD-NUMBER (1) EQUAL TO 750                           SQ2064.2
042900              GO TO WRITE-WRITE-GF-01.                            SQ2064.2
043000     ADD 1 TO XRECORD-NUMBER (1).                                 SQ2064.2
043100     GO TO WRITE-TEST-GF-01.                                      SQ2064.2
043200 WRITE-DELETE-GF-01.                                              SQ2064.2
043300     PERFORM DE-LETE.                                             SQ2064.2
043400 WRITE-WRITE-GF-01.                                               SQ2064.2
043500     MOVE "CREATE SQ-FS1,SQ-FS3" TO FEATURE.                      SQ2064.2
043600     MOVE "WRITE-TEST-GF-01" TO PAR-NAME.                         SQ2064.2
043700     MOVE "FILES CREATED RECS =" TO COMPUTED-A.                   SQ2064.2
043800     MOVE XRECORD-NUMBER (1) TO CORRECT-18V0.                     SQ2064.2
043900     PERFORM PRINT-DETAIL.                                        SQ2064.2
044000     CLOSE SQ-FS1, SQ-FS3.                                        SQ2064.2
044100 WRITE-INIT-GF-02.                                                SQ2064.2
044200     MOVE "SQ-FS4" TO XFILE-NAME (2).                             SQ2064.2
044300     MOVE "R1-F-G" TO XRECORD-NAME (2).                           SQ2064.2
044400     MOVE "SQ206"  TO XPROGRAM-NAME (2).                          SQ2064.2
044500     MOVE 120      TO XRECORD-LENGTH (2).                         SQ2064.2
044600     MOVE "RC"     TO CHARS-OR-RECORDS (2).                       SQ2064.2
044700     MOVE 1        TO XBLOCK-SIZE (2).                            SQ2064.2
044800     MOVE 750      TO RECORDS-IN-FILE (2).                        SQ2064.2
044900     MOVE "SQ"     TO XFILE-ORGANIZATION (2).                     SQ2064.2
045000     MOVE "S"      TO XLABEL-TYPE (2).                            SQ2064.2
045100     MOVE 1        TO XRECORD-NUMBER (2).                         SQ2064.2
045200     OPEN INPUT SQ-FS1, SQ-FS3                                    SQ2064.2
045300          OUTPUT SQ-FS4.                                          SQ2064.2
045400 WRITE-TEST-GF-02.                                                SQ2064.2
045500     MOVE FILE-RECORD-INFO-P1-120 (2) TO SQ-FS4R1-F-G-120.        SQ2064.2
045600     WRITE SQ-FS4R1-F-G-120.                                      SQ2064.2
045700     IF XRECORD-NUMBER (2) EQUAL TO 750                           SQ2064.2
045800              GO TO WRITE-WRITE-GF-02.                            SQ2064.2
045900     ADD 1 TO XRECORD-NUMBER (2).                                 SQ2064.2
046000     GO TO WRITE-TEST-GF-02.                                      SQ2064.2
046100 WRITE-DELETE-GF-02.                                              SQ2064.2
046200     PERFORM DE-LETE.                                             SQ2064.2
046300 WRITE-WRITE-GF-02.                                               SQ2064.2
046400     MOVE "CREATE FILE SQ-FS4" TO FEATURE.                        SQ2064.2
046500     MOVE "WRITE-TEST-GF-02" TO PAR-NAME.                         SQ2064.2
046600     MOVE "FILE CREATED, RECS =" TO COMPUTED-A.                   SQ2064.2
046700     MOVE XRECORD-NUMBER (2) TO CORRECT-18V0.                     SQ2064.2
046800     PERFORM PRINT-DETAIL.                                        SQ2064.2
046900     CLOSE SQ-FS4.                                                SQ2064.2
047000 READ-INIT-GF-01.                                                 SQ2064.2
047100*             THIS TEST READS AND VALIDATES SQ-FS1 WHICH WAS      SQ2064.2
047200*             CREATED IN WRITE-TEST-GF-01. SQ-FS1 IS OPENED FOR   SQ2064.2
047300*             INPUT IN WRITE-INIT-GF-02.                          SQ2064.2
047400     MOVE 0 TO WRK-RECORD-COUNT, RECORDS-IN-ERROR.                SQ2064.2
047500 READ-TEST-GF-01.                                                 SQ2064.2
047600     READ SQ-FS1                                                  SQ2064.2
047700              AT END GO TO READ-TEST-GF-01-01.                    SQ2064.2
047800     MOVE SQ-FS1R1-F-G-120 TO FILE-RECORD-INFO-P1-120 (1).        SQ2064.2
047900     ADD 1 TO WRK-RECORD-COUNT.                                   SQ2064.2
048000     IF WRK-RECORD-COUNT GREATER THAN 750                         SQ2064.2
048100              MOVE "MORE THAN 750 RECORDS" TO RE-MARK             SQ2064.2
048200              GO TO READ-FAIL-GF-01.                              SQ2064.2
048300     IF WRK-RECORD-COUNT NOT EQUAL TO XRECORD-NUMBER (1)          SQ2064.2
048400              ADD 1 TO RECORDS-IN-ERROR                           SQ2064.2
048500              GO TO READ-TEST-GF-01.                              SQ2064.2
048600     IF XFILE-NAME (1) NOT EQUAL TO "SQ-FS1"                      SQ2064.2
048700              ADD 1 TO RECORDS-IN-ERROR                           SQ2064.2
048800              GO TO READ-TEST-GF-01.                              SQ2064.2
048900     IF XLABEL-TYPE (1) NOT EQUAL TO "S"                          SQ2064.2
049000              ADD 1 TO RECORDS-IN-ERROR.                          SQ2064.2
049100     GO TO READ-TEST-GF-01.                                       SQ2064.2
049200 READ-TEST-GF-01-01.                                              SQ2064.2
049300     IF RECORDS-IN-ERROR EQUAL TO ZERO                            SQ2064.2
049400              GO TO READ-PASS-GF-01.                              SQ2064.2
049500     MOVE "ERRORS IN READING-SQ-FS1" TO RE-MARK.                  SQ2064.2
049600     GO TO READ-FAIL-GF-01.                                       SQ2064.2
049700 READ-DELETE-GF-01.                                               SQ2064.2
049800     PERFORM DE-LETE.                                             SQ2064.2
049900     GO TO READ-WRITE-GF-01.                                      SQ2064.2
050000 READ-FAIL-GF-01.                                                 SQ2064.2
050100     MOVE "RECORDS IN ERROR =" TO COMPUTED-A.                     SQ2064.2
050200     MOVE RECORDS-IN-ERROR TO CORRECT-18V0.                       SQ2064.2
050300     PERFORM FAIL.                                                SQ2064.2
050400     GO TO READ-WRITE-GF-01.                                      SQ2064.2
050500 READ-PASS-GF-01.                                                 SQ2064.2
050600     PERFORM PASS.                                                SQ2064.2
050700     MOVE "FILE VERIFIED RECS =" TO COMPUTED-A.                   SQ2064.2
050800     MOVE WRK-RECORD-COUNT TO CORRECT-18V0.                       SQ2064.2
050900 READ-WRITE-GF-01.                                                SQ2064.2
051000     MOVE "READ-TEST-GF-01" TO PAR-NAME.                          SQ2064.2
051100     MOVE "VERIFY FILE SQ-FS1" TO FEATURE.                        SQ2064.2
051200     PERFORM PRINT-DETAIL.                                        SQ2064.2
051300 READ-CLOSE-GF-01.                                                SQ2064.2
051400     CLOSE SQ-FS1.                                                SQ2064.2
051500 READ-INIT-GF-02.                                                 SQ2064.2
051600*             THIS TEST READS AND VALIDATES SQ-FS3 WHICH WAS      SQ2064.2
051700*             CREATED IN WRITE-TEST-GF-01. SQ-FS3 IS OPENED FOR   SQ2064.2
051800*             INPUT IN WRITE-INIT-GF-02.                          SQ2064.2
051900     MOVE 0 TO WRK-RECORD-COUNT, RECORDS-IN-ERROR.                SQ2064.2
052000 READ-TEST-GF-02.                                                 SQ2064.2
052100     READ SQ-FS3                                                  SQ2064.2
052200              AT END GO TO READ-TEST-GF-02-01.                    SQ2064.2
052300     MOVE SQ-FS3R1-F-G-120 TO FILE-RECORD-INFO-P1-120 (3).        SQ2064.2
052400     ADD 1 TO WRK-RECORD-COUNT.                                   SQ2064.2
052500     IF WRK-RECORD-COUNT GREATER THAN 750                         SQ2064.2
052600              MOVE "MORE THAN 750 RECORDS" TO RE-MARK             SQ2064.2
052700              GO TO READ-FAIL-GF-02.                              SQ2064.2
052800     IF WRK-RECORD-COUNT NOT EQUAL TO XRECORD-NUMBER (3)          SQ2064.2
052900              ADD 1 TO RECORDS-IN-ERROR                           SQ2064.2
053000              GO TO READ-TEST-GF-02.                              SQ2064.2
053100     IF XFILE-NAME (3) NOT EQUAL TO "SQ-FS3"                      SQ2064.2
053200              ADD 1 TO RECORDS-IN-ERROR                           SQ2064.2
053300              GO TO READ-TEST-GF-02.                              SQ2064.2
053400     IF XLABEL-TYPE (3) NOT EQUAL TO "S"                          SQ2064.2
053500              ADD 1 TO RECORDS-IN-ERROR.                          SQ2064.2
053600     GO TO READ-TEST-GF-02.                                       SQ2064.2
053700 READ-TEST-GF-02-01.                                              SQ2064.2
053800     IF RECORDS-IN-ERROR EQUAL TO ZERO                            SQ2064.2
053900              GO TO READ-PASS-GF-02.                              SQ2064.2
054000     MOVE "ERRORS IN READING SQ-FS3" TO RE-MARK.                  SQ2064.2
054100     GO TO READ-FAIL-GF-02.                                       SQ2064.2
054200 READ-DELETE-GF-02.                                               SQ2064.2
054300     PERFORM DE-LETE.                                             SQ2064.2
054400     GO TO READ-WRITE-GF-02.                                      SQ2064.2
054500 READ-FAIL-GF-02.                                                 SQ2064.2
054600     MOVE "RECORDS IN ERROR =" TO COMPUTED-A.                     SQ2064.2
054700     MOVE RECORDS-IN-ERROR TO CORRECT-18V0.                       SQ2064.2
054800     PERFORM FAIL.                                                SQ2064.2
054900     GO TO READ-WRITE-GF-02.                                      SQ2064.2
055000 READ-PASS-GF-02.                                                 SQ2064.2
055100     PERFORM PASS.                                                SQ2064.2
055200     MOVE "FILE VERIFIED RECS =" TO COMPUTED-A.                   SQ2064.2
055300     MOVE WRK-RECORD-COUNT TO CORRECT-18V0.                       SQ2064.2
055400 READ-WRITE-GF-02.                                                SQ2064.2
055500     MOVE "READ-TEST-GF-02" TO PAR-NAME.                          SQ2064.2
055600     MOVE "VERIFY FILE SQ-FS3" TO FEATURE.                        SQ2064.2
055700     PERFORM PRINT-DETAIL.                                        SQ2064.2
055800 READ-CLOSE-GF-02.                                                SQ2064.2
055900     CLOSE SQ-FS3.                                                SQ2064.2
056000 READ-INIT-GF-03.                                                 SQ2064.2
056100*             IN THIS TEST SQ-FS2 IS CREATED AND SQ-FS4 IS READ   SQ2064.2
056200*             AND VALIDATED USING SAME RECORD AREA.  SQ-FS4 WAS   SQ2064.2
056300*             CREATED IN WRITE-TEST-GF-02.                        SQ2064.2
056400     MOVE "SQ-FS2" TO XFILE-NAME (2).                             SQ2064.2
056500     MOVE "R1-F-G" TO XRECORD-NAME (2).                           SQ2064.2
056600     MOVE "SQ206"  TO XPROGRAM-NAME (2).                          SQ2064.2
056700     MOVE 120      TO XRECORD-LENGTH (2).                         SQ2064.2
056800     MOVE "RC"     TO CHARS-OR-RECORDS (2).                       SQ2064.2
056900     MOVE 1        TO XBLOCK-SIZE (2).                            SQ2064.2
057000     MOVE 750      TO RECORDS-IN-FILE (2).                        SQ2064.2
057100     MOVE "SQ"     TO XFILE-ORGANIZATION (2).                     SQ2064.2
057200     MOVE "S"      TO XLABEL-TYPE (2).                            SQ2064.2
057300     MOVE 1        TO XRECORD-NUMBER (2).                         SQ2064.2
057400     OPEN INPUT SQ-FS4                                            SQ2064.2
057500          OUTPUT SQ-FS2.                                          SQ2064.2
057600 READ-TEST-GF-03.                                                 SQ2064.2
057700     MOVE FILE-RECORD-INFO-P1-120 (2) TO SQ-FS2R1-F-G-120.        SQ2064.2
057800     WRITE SQ-FS2R1-F-G-120.                                      SQ2064.2
057900     IF XRECORD-NUMBER (2) EQUAL TO 750                           SQ2064.2
058000              GO TO READ-WRITE-GF-03.                             SQ2064.2
058100     ADD 1 TO XRECORD-NUMBER (2).                                 SQ2064.2
058200     GO TO READ-TEST-GF-03.                                       SQ2064.2
058300 READ-DELETE-GF-03.                                               SQ2064.2
058400     PERFORM DE-LETE.                                             SQ2064.2
058500 READ-WRITE-GF-03.                                                SQ2064.2
058600     MOVE "CREATE FILE SQ-FS2" TO FEATURE.                        SQ2064.2
058700     MOVE "READ-TEST-GF-03" TO PAR-NAME.                          SQ2064.2
058800     MOVE "FILE CREATED, RECS =" TO COMPUTED-A.                   SQ2064.2
058900     MOVE XRECORD-NUMBER (2) TO CORRECT-18V0.                     SQ2064.2
059000     PERFORM PRINT-DETAIL.                                        SQ2064.2
059100     CLOSE SQ-FS2.                                                SQ2064.2
059200 READ-INIT-GF-04.                                                 SQ2064.2
059300*             THIS TEST READS AND VALIDATES SQ-FS4 WHICH WAS      SQ2064.2
059400*             CREATED IN WRITE-TEST-GF-02. SQ-FS4 IS OPENED FOR   SQ2064.2
059500*             INPUT IN WRITE-INIT-GF-03.                          SQ2064.2
059600     MOVE 0 TO WRK-RECORD-COUNT, RECORDS-IN-ERROR.                SQ2064.2
059700 READ-TEST-GF-04.                                                 SQ2064.2
059800     READ SQ-FS4                                                  SQ2064.2
059900              AT END GO TO READ-TEST-GF-04-01.                    SQ2064.2
060000     MOVE SQ-FS4R1-F-G-120 TO FILE-RECORD-INFO-P1-120 (4).        SQ2064.2
060100     ADD 1 TO WRK-RECORD-COUNT.                                   SQ2064.2
060200     IF WRK-RECORD-COUNT GREATER THAN 750                         SQ2064.2
060300              MOVE "MORE THAN 750 RECORDS" TO RE-MARK             SQ2064.2
060400              GO TO READ-FAIL-GF-04.                              SQ2064.2
060500     IF WRK-RECORD-COUNT NOT EQUAL TO XRECORD-NUMBER (4)          SQ2064.2
060600              ADD 1 TO RECORDS-IN-ERROR                           SQ2064.2
060700              GO TO READ-TEST-GF-04.                              SQ2064.2
060800     IF XFILE-NAME (4) NOT EQUAL TO "SQ-FS4"                      SQ2064.2
060900              ADD 1 TO RECORDS-IN-ERROR                           SQ2064.2
061000              GO TO READ-TEST-GF-04.                              SQ2064.2
061100     IF XLABEL-TYPE (4) NOT EQUAL TO "S"                          SQ2064.2
061200              ADD 1 TO RECORDS-IN-ERROR.                          SQ2064.2
061300     GO TO READ-TEST-GF-04.                                       SQ2064.2
061400 READ-TEST-GF-04-01.                                              SQ2064.2
061500     IF RECORDS-IN-ERROR EQUAL TO ZERO                            SQ2064.2
061600              GO TO READ-PASS-GF-04.                              SQ2064.2
061700     MOVE "ERRORS IN READING SQ-FS4" TO RE-MARK.                  SQ2064.2
061800     GO TO READ-FAIL-GF-04.                                       SQ2064.2
061900 READ-DELETE-GF-04.                                               SQ2064.2
062000     PERFORM DE-LETE.                                             SQ2064.2
062100     GO TO READ-WRITE-GF-04.                                      SQ2064.2
062200 READ-FAIL-GF-04.                                                 SQ2064.2
062300     MOVE "RECORDS IN ERROR =" TO COMPUTED-A.                     SQ2064.2
062400     MOVE RECORDS-IN-ERROR TO CORRECT-18V0.                       SQ2064.2
062500     PERFORM FAIL.                                                SQ2064.2
062600     GO TO READ-WRITE-GF-04.                                      SQ2064.2
062700 READ-PASS-GF-04.                                                 SQ2064.2
062800     PERFORM PASS.                                                SQ2064.2
062900     MOVE "FILE VERIFIED RECS =" TO COMPUTED-A.                   SQ2064.2
063000     MOVE WRK-RECORD-COUNT TO CORRECT-18V0.                       SQ2064.2
063100 READ-WRITE-GF-04.                                                SQ2064.2
063200     MOVE "READ-TEST-GF-04" TO PAR-NAME.                          SQ2064.2
063300     MOVE "VERIFY FILE SQ-FS4" TO FEATURE.                        SQ2064.2
063400     PERFORM PRINT-DETAIL.                                        SQ2064.2
063500 READ-CLOSE-GF-04.                                                SQ2064.2
063600     CLOSE SQ-FS4.                                                SQ2064.2
063700 READ-INIT-GF-05.                                                 SQ2064.2
063800*             THIS TEST READS AND VALIDATE SQ-FS2 WHICH WAS       SQ2064.2
063900*             CREATED IN WRITE-TEST-GF-02.                        SQ2064.2
064000     MOVE 0 TO WRK-RECORD-COUNT, RECORDS-IN-ERROR.                SQ2064.2
064100     OPEN INPUT SQ-FS2.                                           SQ2064.2
064200 READ-TEST-GF-05.                                                 SQ2064.2
064300     READ SQ-FS2                                                  SQ2064.2
064400              AT END GO TO READ-TEST-GF-05-01.                    SQ2064.2
064500     MOVE SQ-FS2R1-F-G-120 TO FILE-RECORD-INFO-P1-120 (2).        SQ2064.2
064600     ADD 1 TO WRK-RECORD-COUNT.                                   SQ2064.2
064700     IF WRK-RECORD-COUNT GREATER THAN 750                         SQ2064.2
064800              MOVE "MORE THAN 750 RECORDS" TO RE-MARK             SQ2064.2
064900              GO TO READ-FAIL-GF-05.                              SQ2064.2
065000     IF WRK-RECORD-COUNT NOT EQUAL TO XRECORD-NUMBER (2)          SQ2064.2
065100              ADD 1 TO RECORDS-IN-ERROR                           SQ2064.2
065200              GO TO READ-TEST-GF-05.                              SQ2064.2
065300     IF XFILE-NAME (2) NOT EQUAL TO "SQ-FS2"                      SQ2064.2
065400              ADD 1 TO RECORDS-IN-ERROR                           SQ2064.2
065500              GO TO READ-TEST-GF-05.                              SQ2064.2
065600     IF XLABEL-TYPE (2) NOT EQUAL TO "S"                          SQ2064.2
065700              ADD 1 TO RECORDS-IN-ERROR.                          SQ2064.2
065800     GO TO READ-TEST-GF-05.                                       SQ2064.2
065900 READ-TEST-GF-05-01.                                              SQ2064.2
066000     IF RECORDS-IN-ERROR EQUAL TO ZERO                            SQ2064.2
066100              GO TO READ-PASS-GF-05.                              SQ2064.2
066200     MOVE "ERRORS IN READING SQ-FS2" TO RE-MARK.                  SQ2064.2
066300     GO TO READ-FAIL-GF-05.                                       SQ2064.2
066400 READ-DELETE-GF-05.                                               SQ2064.2
066500     PERFORM DE-LETE.                                             SQ2064.2
066600     GO TO READ-WRITE-GF-05.                                      SQ2064.2
066700 READ-FAIL-GF-05.                                                 SQ2064.2
066800     MOVE "RECORDS IN ERROR =" TO COMPUTED-A.                     SQ2064.2
066900     MOVE RECORDS-IN-ERROR TO CORRECT-18V0.                       SQ2064.2
067000     PERFORM FAIL.                                                SQ2064.2
067100     GO TO READ-WRITE-GF-05.                                      SQ2064.2
067200 READ-PASS-GF-05.                                                 SQ2064.2
067300     PERFORM PASS.                                                SQ2064.2
067400     MOVE "FILE VERIFIED RECS =" TO COMPUTED-A.                   SQ2064.2
067500     MOVE WRK-RECORD-COUNT TO CORRECT-18V0.                       SQ2064.2
067600 READ-WRITE-GF-05.                                                SQ2064.2
067700     MOVE "READ-TEST-GF-05" TO PAR-NAME.                          SQ2064.2
067800     MOVE "VERIFY FILE SQ-FS2" TO FEATURE.                        SQ2064.2
067900     PERFORM PRINT-DETAIL.                                        SQ2064.2
068000 READ-CLOSE-GF-05.                                                SQ2064.2
068100     CLOSE SQ-FS2.                                                SQ2064.2
068200 SQ206A-END-ROUTINE.                                              SQ2064.2
068300     MOVE "END OF SQ206A VALIDATION TESTS" TO PRINT-REC.          SQ2064.2
068400     WRITE PRINT-REC AFTER ADVANCING 1 LINE.                      SQ2064.2
068500 TERMINATE-SQ206A.                                                SQ2064.2
068600     EXIT.                                                        SQ2064.2
068700 CCVS-EXIT SECTION.                                               SQ2064.2
068800 CCVS-999999.                                                     SQ2064.2
068900     GO TO CLOSE-FILES.                                           SQ2064.2
      *END-OF,SQ206A                                                                  
