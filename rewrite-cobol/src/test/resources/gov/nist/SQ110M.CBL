      *HEADER,COBOL,SQ110M                                                            
000100 IDENTIFICATION DIVISION.                                         SQ1104.2
000200 PROGRAM-ID.                                                      SQ1104.2
000300     SQ110M.                                                      SQ1104.2
000400****************************************************************  SQ1104.2
000500*                                                              *  SQ1104.2
000600*    VALIDATION FOR:-                                          *  SQ1104.2
000700*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1104.2
000800*                                                              *  SQ1104.2
000900*    CREATION DATE     /     VALIDATION DATE                   *  SQ1104.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ1104.2
001100*                                                              *  SQ1104.2
001200****************************************************************  SQ1104.2
001300                                                                  SQ1104.2
001400*         THIS ROUTINE CREATES A 2 UNIT MASS-STORAGE              SQ1104.2
001500*    FILE WHICH HAS FIXED LENGTH RECORDS.  THE FILE IS THEN       SQ1104.2
001600*    CLOSED AND OPENED AS AN INPUT FILE.  THE FILE IS READ AND    SQ1104.2
001700*    FIELDS IN THE INPUT RECORDS ARE COMPARED TO THE VALUES       SQ1104.2
001800*    WRITTEN TO ENSURE THAT THE RECORDS WERE PROCESSED CORRECTLY. SQ1104.2
001900*                                                                 SQ1104.2
002000*    THE FILE IS CLOSED AND OPENED AGAIN AS AN INPUT FILE.  FOUR  SQ1104.2
002100*    READ FORMAT OPTIONS ARE USED TO READ THE FILE AND FIELDS IN  SQ1104.2
002200*    THE RECORDS ARE VERIFIED.  THE OPEN, CLOSE, READ, AND WRITE  SQ1104.2
002300*    STATEMENTS ARE TESTED FOR LEVEL ONE FEATURES.                SQ1104.2
002400*                                                                 SQ1104.2
002500*    USED X-CARDS:                                                SQ1104.2
002600*         XXXXX019                                                SQ1104.2
002700*         XXXXX055                                                SQ1104.2
002800*     P   XXXXX062                                                SQ1104.2
002900*         XXXXX082                                                SQ1104.2
003000*         XXXXX083                                                SQ1104.2
003100*     C   XXXXX084                                                SQ1104.2
003200*                                                                 SQ1104.2
003300*                                                                 SQ1104.2
003400 ENVIRONMENT DIVISION.                                            SQ1104.2
003500 CONFIGURATION SECTION.                                           SQ1104.2
003600 SOURCE-COMPUTER.                                                 SQ1104.2
003700     XXXXX082.                                                    SQ1104.2
003800 OBJECT-COMPUTER.                                                 SQ1104.2
003900     XXXXX083.                                                    SQ1104.2
004000 INPUT-OUTPUT SECTION.                                            SQ1104.2
004100 FILE-CONTROL.                                                    SQ1104.2
004200*    SELECT RAW-DATA   ASSIGN TO                                  SQ1104.2
004300*    XXXXX062                                                     SQ1104.2
004400*           ORGANIZATION IS INDEXED                               SQ1104.2
004500*           ACCESS MODE IS RANDOM                                 SQ1104.2
004600*           RECORD KEY IS RAW-DATA-KEY.                           SQ1104.2
004700     SELECT PRINT-FILE ASSIGN TO                                  SQ1104.2
004800     XXXXX055.                                                    SQ1104.2
004900     SELECT SQ-FS3 ASSIGN TO                                      SQ1104.2
005000     XXXXX019                                                     SQ1104.2
005100     ORGANIZATION IS SEQUENTIAL                                   SQ1104.2
005200     ACCESS MODE IS SEQUENTIAL.                                   SQ1104.2
005300 DATA DIVISION.                                                   SQ1104.2
005400 FILE SECTION.                                                    SQ1104.2
005500*                                                                 SQ1104.2
005600*FD  RAW-DATA.                                                    SQ1104.2
005700*                                                                 SQ1104.2
005800*01  RAW-DATA-SATZ.                                               SQ1104.2
005900*    05  RAW-DATA-KEY        PIC X(6).                            SQ1104.2
006000*    05  C-DATE              PIC 9(6).                            SQ1104.2
006100*    05  C-TIME              PIC 9(8).                            SQ1104.2
006200*    05  C-NO-OF-TESTS       PIC 99.                              SQ1104.2
006300*    05  C-OK                PIC 999.                             SQ1104.2
006400*    05  C-ALL               PIC 999.                             SQ1104.2
006500*    05  C-FAIL              PIC 999.                             SQ1104.2
006600*    05  C-DELETED           PIC 999.                             SQ1104.2
006700*    05  C-INSPECT           PIC 999.                             SQ1104.2
006800*    05  C-NOTE              PIC X(13).                           SQ1104.2
006900*    05  C-INDENT            PIC X.                               SQ1104.2
007000*    05  C-ABORT             PIC X(8).                            SQ1104.2
007100 FD  PRINT-FILE                                                   SQ1104.2
007200C    LABEL RECORDS                                                SQ1104.2
007300C    XXXXX084                                                     SQ1104.2
007400C    DATA RECORD IS PRINT-REC DUMMY-RECORD                        SQ1104.2
007500               .                                                  SQ1104.2
007600 01  PRINT-REC PICTURE X(120).                                    SQ1104.2
007700 01  DUMMY-RECORD PICTURE X(120).                                 SQ1104.2
007800 FD  SQ-FS3                                                       SQ1104.2
007900C    LABEL RECORDS ARE STANDARD                                   SQ1104.2
008000C    DATA RECORD SQ-FS3R1-F-G-120                                 SQ1104.2
008100     BLOCK CONTAINS 120 CHARACTERS                                SQ1104.2
008200     RECORD CONTAINS 120 CHARACTERS.                              SQ1104.2
008300 01  SQ-FS3R1-F-G-120.                                            SQ1104.2
008400     02  FILLER PIC X(120).                                       SQ1104.2
008500 WORKING-STORAGE SECTION.                                         SQ1104.2
008600 01  WRK-CS-09V00 PICTURE S9(9) USAGE COMP VALUE ZERO.            SQ1104.2
008700 01  RECORDS-IN-ERROR  PIC S9(5) USAGE COMP VALUE 0.              SQ1104.2
008800 01  ERROR-FLAG PICTURE 9 VALUE 0.                                SQ1104.2
008900 01  EOF-FLAG PICTURE 9 VALUE 0.                                  SQ1104.2
009000 01  FILE-RECORD-INFORMATION-REC.                                 SQ1104.2
009100     03 FILE-RECORD-INFO-SKELETON.                                SQ1104.2
009200        05 FILLER                 PICTURE X(48)       VALUE       SQ1104.2
009300             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  SQ1104.2
009400        05 FILLER                 PICTURE X(46)       VALUE       SQ1104.2
009500             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    SQ1104.2
009600        05 FILLER                 PICTURE X(26)       VALUE       SQ1104.2
009700             ",LFIL=000000,ORG=  ,LBLR= ".                        SQ1104.2
009800        05 FILLER                 PICTURE X(37)       VALUE       SQ1104.2
009900             ",RECKEY=                             ".             SQ1104.2
010000        05 FILLER                 PICTURE X(38)       VALUE       SQ1104.2
010100             ",ALTKEY1=                             ".            SQ1104.2
010200        05 FILLER                 PICTURE X(38)       VALUE       SQ1104.2
010300             ",ALTKEY2=                             ".            SQ1104.2
010400        05 FILLER                 PICTURE X(7)        VALUE SPACE.SQ1104.2
010500     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              SQ1104.2
010600        05 FILE-RECORD-INFO-P1-120.                               SQ1104.2
010700           07 FILLER              PIC X(5).                       SQ1104.2
010800           07 XFILE-NAME           PIC X(6).                      SQ1104.2
010900           07 FILLER              PIC X(8).                       SQ1104.2
011000           07 XRECORD-NAME         PIC X(6).                      SQ1104.2
011100           07 FILLER              PIC X(1).                       SQ1104.2
011200           07 REELUNIT-NUMBER     PIC 9(1).                       SQ1104.2
011300           07 FILLER              PIC X(7).                       SQ1104.2
011400           07 XRECORD-NUMBER       PIC 9(6).                      SQ1104.2
011500           07 FILLER              PIC X(6).                       SQ1104.2
011600           07 UPDATE-NUMBER       PIC 9(2).                       SQ1104.2
011700           07 FILLER              PIC X(5).                       SQ1104.2
011800           07 ODO-NUMBER          PIC 9(4).                       SQ1104.2
011900           07 FILLER              PIC X(5).                       SQ1104.2
012000           07 XPROGRAM-NAME        PIC X(5).                      SQ1104.2
012100           07 FILLER              PIC X(7).                       SQ1104.2
012200           07 XRECORD-LENGTH       PIC 9(6).                      SQ1104.2
012300           07 FILLER              PIC X(7).                       SQ1104.2
012400           07 CHARS-OR-RECORDS    PIC X(2).                       SQ1104.2
012500           07 FILLER              PIC X(1).                       SQ1104.2
012600           07 XBLOCK-SIZE          PIC 9(4).                      SQ1104.2
012700           07 FILLER              PIC X(6).                       SQ1104.2
012800           07 RECORDS-IN-FILE     PIC 9(6).                       SQ1104.2
012900           07 FILLER              PIC X(5).                       SQ1104.2
013000           07 XFILE-ORGANIZATION   PIC X(2).                      SQ1104.2
013100           07 FILLER              PIC X(6).                       SQ1104.2
013200           07 XLABEL-TYPE          PIC X(1).                      SQ1104.2
013300        05 FILE-RECORD-INFO-P121-240.                             SQ1104.2
013400           07 FILLER              PIC X(8).                       SQ1104.2
013500           07 XRECORD-KEY          PIC X(29).                     SQ1104.2
013600           07 FILLER              PIC X(9).                       SQ1104.2
013700           07 ALTERNATE-KEY1      PIC X(29).                      SQ1104.2
013800           07 FILLER              PIC X(9).                       SQ1104.2
013900           07 ALTERNATE-KEY2      PIC X(29).                      SQ1104.2
014000           07 FILLER              PIC X(7).                       SQ1104.2
014100 01  TEST-RESULTS.                                                SQ1104.2
014200     02 FILLER                    PICTURE X VALUE SPACE.          SQ1104.2
014300     02 FEATURE                   PICTURE X(20) VALUE SPACE.      SQ1104.2
014400     02 FILLER                    PICTURE X VALUE SPACE.          SQ1104.2
014500     02 P-OR-F                    PICTURE X(5) VALUE SPACE.       SQ1104.2
014600     02 FILLER                    PICTURE X  VALUE SPACE.         SQ1104.2
014700     02  PAR-NAME.                                                SQ1104.2
014800       03 FILLER PICTURE X(12) VALUE SPACE.                       SQ1104.2
014900       03  PARDOT-X PICTURE X  VALUE SPACE.                       SQ1104.2
015000       03 DOTVALUE PICTURE 99  VALUE ZERO.                        SQ1104.2
015100       03 FILLER PIC X(5) VALUE SPACE.                            SQ1104.2
015200     02 FILLER PIC X(10) VALUE SPACE.                             SQ1104.2
015300     02 RE-MARK PIC X(61).                                        SQ1104.2
015400 01  TEST-COMPUTED.                                               SQ1104.2
015500     02 FILLER PIC X(30) VALUE SPACE.                             SQ1104.2
015600     02 FILLER PIC X(17) VALUE "       COMPUTED=".                SQ1104.2
015700     02 COMPUTED-X.                                               SQ1104.2
015800     03 COMPUTED-A                PICTURE X(20) VALUE SPACE.      SQ1104.2
015900     03 COMPUTED-N REDEFINES COMPUTED-A PICTURE -9(9).9(9).       SQ1104.2
016000     03 COMPUTED-0V18 REDEFINES COMPUTED-A  PICTURE -.9(18).      SQ1104.2
016100     03 COMPUTED-4V14 REDEFINES COMPUTED-A  PICTURE -9(4).9(14).  SQ1104.2
016200     03 COMPUTED-14V4 REDEFINES COMPUTED-A  PICTURE -9(14).9(4).  SQ1104.2
016300     03       CM-18V0 REDEFINES COMPUTED-A.                       SQ1104.2
016400         04 COMPUTED-18V0                   PICTURE -9(18).       SQ1104.2
016500         04 FILLER                          PICTURE X.            SQ1104.2
016600     03 FILLER PIC X(50) VALUE SPACE.                             SQ1104.2
016700 01  TEST-CORRECT.                                                SQ1104.2
016800     02 FILLER PIC X(30) VALUE SPACE.                             SQ1104.2
016900     02 FILLER PIC X(17) VALUE "       CORRECT =".                SQ1104.2
017000     02 CORRECT-X.                                                SQ1104.2
017100     03 CORRECT-A                 PICTURE X(20) VALUE SPACE.      SQ1104.2
017200     03 CORRECT-N REDEFINES CORRECT-A PICTURE -9(9).9(9).         SQ1104.2
017300     03 CORRECT-0V18 REDEFINES CORRECT-A    PICTURE -.9(18).      SQ1104.2
017400     03 CORRECT-4V14 REDEFINES CORRECT-A    PICTURE -9(4).9(14).  SQ1104.2
017500     03 CORRECT-14V4 REDEFINES CORRECT-A    PICTURE -9(14).9(4).  SQ1104.2
017600     03      CR-18V0 REDEFINES CORRECT-A.                         SQ1104.2
017700         04 CORRECT-18V0                    PICTURE -9(18).       SQ1104.2
017800         04 FILLER                          PICTURE X.            SQ1104.2
017900     03 FILLER PIC X(50) VALUE SPACE.                             SQ1104.2
018000 01  CCVS-C-1.                                                    SQ1104.2
018100     02 FILLER PICTURE IS X(99) VALUE IS " FEATURE              PASQ1104.2
018200-    "SS  PARAGRAPH-NAME                                          SQ1104.2
018300-    "        REMARKS".                                           SQ1104.2
018400     02 FILLER PICTURE IS X(20) VALUE IS SPACE.                   SQ1104.2
018500 01  CCVS-C-2.                                                    SQ1104.2
018600     02 FILLER PICTURE IS X VALUE IS SPACE.                       SQ1104.2
018700     02 FILLER PICTURE IS X(6) VALUE IS "TESTED".                 SQ1104.2
018800     02 FILLER PICTURE IS X(15) VALUE IS SPACE.                   SQ1104.2
018900     02 FILLER PICTURE IS X(4) VALUE IS "FAIL".                   SQ1104.2
019000     02 FILLER PICTURE IS X(94) VALUE IS SPACE.                   SQ1104.2
019100 01  REC-SKL-SUB PICTURE 9(2) VALUE ZERO.                         SQ1104.2
019200 01  REC-CT PICTURE 99 VALUE ZERO.                                SQ1104.2
019300 01  DELETE-CNT                   PICTURE 999  VALUE ZERO.        SQ1104.2
019400 01  ERROR-COUNTER PICTURE IS 999 VALUE IS ZERO.                  SQ1104.2
019500 01  INSPECT-COUNTER PIC 999 VALUE ZERO.                          SQ1104.2
019600 01  PASS-COUNTER PIC 999 VALUE ZERO.                             SQ1104.2
019700 01  TOTAL-ERROR PIC 999 VALUE ZERO.                              SQ1104.2
019800 01  ERROR-HOLD PIC 999 VALUE ZERO.                               SQ1104.2
019900 01  DUMMY-HOLD PIC X(120) VALUE SPACE.                           SQ1104.2
020000 01  RECORD-COUNT PIC 9(5) VALUE ZERO.                            SQ1104.2
020100 01  CCVS-H-1.                                                    SQ1104.2
020200     02  FILLER   PICTURE X(27)  VALUE SPACE.                     SQ1104.2
020300     02 FILLER PICTURE X(67) VALUE                                SQ1104.2
020400     " FEDERAL SOFTWARE TESTING CENTER COBOL COMPILER VALIDATION  SQ1104.2
020500-    " SYSTEM".                                                   SQ1104.2
020600     02  FILLER     PICTURE X(26)  VALUE SPACE.                   SQ1104.2
020700 01  CCVS-H-2.                                                    SQ1104.2
020800     02 FILLER PICTURE X(52) VALUE IS                             SQ1104.2
020900     "CCVS85 FSTC COPY, NOT FOR DISTRIBUTION.".                   SQ1104.2
021000     02 FILLER PICTURE IS X(19) VALUE IS "TEST RESULTS SET-  ".   SQ1104.2
021100     02 TEST-ID PICTURE IS X(9).                                  SQ1104.2
021200     02 FILLER PICTURE IS X(40) VALUE IS SPACE.                   SQ1104.2
021300 01  CCVS-H-3.                                                    SQ1104.2
021400     02  FILLER PICTURE X(34) VALUE                               SQ1104.2
021500     " FOR OFFICIAL USE ONLY    ".                                SQ1104.2
021600     02  FILLER PICTURE X(58) VALUE                               SQ1104.2
021700     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ1104.2
021800     02  FILLER PICTURE X(28) VALUE                               SQ1104.2
021900     "  COPYRIGHT   1985 ".                                       SQ1104.2
022000 01  CCVS-E-1.                                                    SQ1104.2
022100     02 FILLER PICTURE IS X(52) VALUE IS SPACE.                   SQ1104.2
022200     02 FILLER PICTURE IS X(14) VALUE IS "END OF TEST-  ".        SQ1104.2
022300     02 ID-AGAIN PICTURE IS X(9).                                 SQ1104.2
022400     02 FILLER PICTURE X(45) VALUE IS                             SQ1104.2
022500     " NTIS DISTRIBUTION COBOL 85".                               SQ1104.2
022600 01  CCVS-E-2.                                                    SQ1104.2
022700     02  FILLER                   PICTURE X(31)  VALUE            SQ1104.2
022800     SPACE.                                                       SQ1104.2
022900     02  FILLER                   PICTURE X(21)  VALUE SPACE.     SQ1104.2
023000     02 CCVS-E-2-2.                                               SQ1104.2
023100         03 ERROR-TOTAL PICTURE IS XXX VALUE IS SPACE.            SQ1104.2
023200         03 FILLER PICTURE IS X VALUE IS SPACE.                   SQ1104.2
023300         03 ENDER-DESC PIC X(46) VALUE "ERRORS ENCOUNTERED".      SQ1104.2
023400 01  CCVS-E-3.                                                    SQ1104.2
023500     02  FILLER PICTURE X(22) VALUE                               SQ1104.2
023600     " FOR OFFICIAL USE ONLY".                                    SQ1104.2
023700     02  FILLER PICTURE X(12) VALUE SPACE.                        SQ1104.2
023800     02  FILLER PICTURE X(58) VALUE                               SQ1104.2
023900     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1104.2
024000     02  FILLER PICTURE X(13) VALUE SPACE.                        SQ1104.2
024100     02 FILLER PIC X(15) VALUE " COPYRIGHT 1985".                 SQ1104.2
024200 01  CCVS-E-4.                                                    SQ1104.2
024300     02 CCVS-E-4-1 PIC XXX VALUE SPACE.                           SQ1104.2
024400     02 FILLER PIC XXXX VALUE " OF ".                             SQ1104.2
024500     02 CCVS-E-4-2 PIC XXX VALUE SPACE.                           SQ1104.2
024600     02 FILLER PIC X(40) VALUE                                    SQ1104.2
024700      "  TESTS WERE EXECUTED SUCCESSFULLY".                       SQ1104.2
024800 01  XXINFO.                                                      SQ1104.2
024900     02 FILLER PIC X(30) VALUE "        *** INFORMATION  ***".    SQ1104.2
025000     02 INFO-TEXT.                                                SQ1104.2
025100     04 FILLER PIC X(20) VALUE SPACE.                             SQ1104.2
025200     04 XXCOMPUTED PIC X(20).                                     SQ1104.2
025300     04 FILLER PIC X(5) VALUE SPACE.                              SQ1104.2
025400     04 XXCORRECT PIC X(20).                                      SQ1104.2
025500 01  HYPHEN-LINE.                                                 SQ1104.2
025600     02 FILLER PICTURE IS X VALUE IS SPACE.                       SQ1104.2
025700     02 FILLER PICTURE IS X(65) VALUE IS "************************SQ1104.2
025800-    "*****************************************".                 SQ1104.2
025900     02 FILLER PICTURE IS X(54) VALUE IS "************************SQ1104.2
026000-    "******************************".                            SQ1104.2
026100 01  CCVS-PGM-ID PIC X(6) VALUE                                   SQ1104.2
026200     "SQ110M".                                                    SQ1104.2
026300 PROCEDURE DIVISION.                                              SQ1104.2
026400 CCVS1 SECTION.                                                   SQ1104.2
026500 OPEN-FILES.                                                      SQ1104.2
026600*    OPEN I-O RAW-DATA.                                           SQ1104.2
026700*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            SQ1104.2
026800*    READ RAW-DATA INVALID KEY GO TO END-E-1.                     SQ1104.2
026900*    MOVE "ABORTED " TO C-ABORT.                                  SQ1104.2
027000*    ADD 1 TO C-NO-OF-TESTS.                                      SQ1104.2
027100*    ACCEPT C-DATE  FROM DATE.                                    SQ1104.2
027200*    ACCEPT C-TIME  FROM TIME.                                    SQ1104.2
027300*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-1.             SQ1104.2
027400*END-E-1.                                                         SQ1104.2
027500*    CLOSE RAW-DATA.                                              SQ1104.2
027600     OPEN     OUTPUT PRINT-FILE.                                  SQ1104.2
027700     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   SQ1104.2
027800     MOVE    SPACE TO TEST-RESULTS.                               SQ1104.2
027900     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             SQ1104.2
028000     MOVE ZERO TO REC-SKL-SUB.                                    SQ1104.2
028100     PERFORM CCVS-INIT-FILE 9 TIMES.                              SQ1104.2
028200 CCVS-INIT-FILE.                                                  SQ1104.2
028300     ADD 1 TO REC-SKL-SUB.                                        SQ1104.2
028400     MOVE FILE-RECORD-INFO-SKELETON TO                            SQ1104.2
028500                  FILE-RECORD-INFO (REC-SKL-SUB).                 SQ1104.2
028600 CCVS-INIT-EXIT.                                                  SQ1104.2
028700     GO TO CCVS1-EXIT.                                            SQ1104.2
028800 CLOSE-FILES.                                                     SQ1104.2
028900     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   SQ1104.2
029000*    OPEN I-O RAW-DATA.                                           SQ1104.2
029100*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            SQ1104.2
029200*    READ RAW-DATA INVALID KEY GO TO END-E-2.                     SQ1104.2
029300*    MOVE "OK.     " TO C-ABORT.                                  SQ1104.2
029400*    MOVE PASS-COUNTER TO C-OK.                                   SQ1104.2
029500*    MOVE ERROR-HOLD   TO C-ALL.                                  SQ1104.2
029600*    MOVE ERROR-COUNTER TO C-FAIL.                                SQ1104.2
029700*    MOVE DELETE-CNT TO C-DELETED.                                SQ1104.2
029800*    MOVE INSPECT-COUNTER TO C-INSPECT.                           SQ1104.2
029900*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-2.             SQ1104.2
030000*END-E-2.                                                         SQ1104.2
030100*    CLOSE RAW-DATA.                                              SQ1104.2
030200 TERMINATE-CCVS.                                                  SQ1104.2
030300*    EXIT PROGRAM.                                                SQ1104.2
030400*TERMINATE-CALL.                                                  SQ1104.2
030500     STOP     RUN.                                                SQ1104.2
030600 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         SQ1104.2
030700 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           SQ1104.2
030800 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          SQ1104.2
030900 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-CNT.          SQ1104.2
031000     MOVE "****TEST DELETED****" TO RE-MARK.                      SQ1104.2
031100 PRINT-DETAIL.                                                    SQ1104.2
031200     IF REC-CT NOT EQUAL TO ZERO                                  SQ1104.2
031300             MOVE "." TO PARDOT-X                                 SQ1104.2
031400             MOVE REC-CT TO DOTVALUE.                             SQ1104.2
031500     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      SQ1104.2
031600     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               SQ1104.2
031700        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 SQ1104.2
031800          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 SQ1104.2
031900     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              SQ1104.2
032000     MOVE SPACE TO CORRECT-X.                                     SQ1104.2
032100     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         SQ1104.2
032200     MOVE     SPACE TO RE-MARK.                                   SQ1104.2
032300 HEAD-ROUTINE.                                                    SQ1104.2
032400     MOVE CCVS-H-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ1104.2
032500     MOVE CCVS-H-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.   SQ1104.2
032600     MOVE CCVS-H-3 TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.   SQ1104.2
032700 COLUMN-NAMES-ROUTINE.                                            SQ1104.2
032800     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ1104.2
032900     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ1104.2
033000     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ1104.2
033100 END-ROUTINE.                                                     SQ1104.2
033200     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.SQ1104.2
033300 END-RTN-EXIT.                                                    SQ1104.2
033400     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ1104.2
033500 END-ROUTINE-1.                                                   SQ1104.2
033600      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      SQ1104.2
033700      ERROR-HOLD. ADD DELETE-CNT TO ERROR-HOLD.                   SQ1104.2
033800      ADD PASS-COUNTER TO ERROR-HOLD.                             SQ1104.2
033900*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   SQ1104.2
034000      MOVE PASS-COUNTER TO CCVS-E-4-1.                            SQ1104.2
034100      MOVE ERROR-HOLD TO CCVS-E-4-2.                              SQ1104.2
034200      MOVE CCVS-E-4 TO CCVS-E-2-2.                                SQ1104.2
034300      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           SQ1104.2
034400  END-ROUTINE-12.                                                 SQ1104.2
034500      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        SQ1104.2
034600     IF       ERROR-COUNTER IS EQUAL TO ZERO                      SQ1104.2
034700         MOVE "NO " TO ERROR-TOTAL                                SQ1104.2
034800         ELSE                                                     SQ1104.2
034900         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       SQ1104.2
035000     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           SQ1104.2
035100     PERFORM WRITE-LINE.                                          SQ1104.2
035200 END-ROUTINE-13.                                                  SQ1104.2
035300     IF DELETE-CNT IS EQUAL TO ZERO                               SQ1104.2
035400         MOVE "NO " TO ERROR-TOTAL  ELSE                          SQ1104.2
035500         MOVE DELETE-CNT TO ERROR-TOTAL.                          SQ1104.2
035600     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   SQ1104.2
035700     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ1104.2
035800      IF   INSPECT-COUNTER EQUAL TO ZERO                          SQ1104.2
035900          MOVE "NO " TO ERROR-TOTAL                               SQ1104.2
036000      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   SQ1104.2
036100      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            SQ1104.2
036200      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          SQ1104.2
036300     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ1104.2
036400 WRITE-LINE.                                                      SQ1104.2
036500     ADD 1 TO RECORD-COUNT.                                       SQ1104.2
036600*    IF RECORD-COUNT GREATER 50                                   SQ1104.2
036700*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          SQ1104.2
036800*        MOVE SPACE TO DUMMY-RECORD                               SQ1104.2
036900*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  SQ1104.2
037000*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             SQ1104.2
037100*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     SQ1104.2
037200*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          SQ1104.2
037300*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          SQ1104.2
037400*        MOVE ZERO TO RECORD-COUNT.                               SQ1104.2
037500     PERFORM WRT-LN.                                              SQ1104.2
037600 WRT-LN.                                                          SQ1104.2
037700     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               SQ1104.2
037800     MOVE SPACE TO DUMMY-RECORD.                                  SQ1104.2
037900 BLANK-LINE-PRINT.                                                SQ1104.2
038000     PERFORM WRT-LN.                                              SQ1104.2
038100 FAIL-ROUTINE.                                                    SQ1104.2
038200     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.   SQ1104.2
038300     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.    SQ1104.2
038400     MOVE "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.    SQ1104.2
038500     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     SQ1104.2
038600     GO TO FAIL-ROUTINE-EX.                                       SQ1104.2
038700 FAIL-ROUTINE-WRITE.                                              SQ1104.2
038800     MOVE TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE           SQ1104.2
038900     MOVE TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES.   SQ1104.2
039000 FAIL-ROUTINE-EX. EXIT.                                           SQ1104.2
039100 BAIL-OUT.                                                        SQ1104.2
039200     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.       SQ1104.2
039300     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.               SQ1104.2
039400 BAIL-OUT-WRITE.                                                  SQ1104.2
039500     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  SQ1104.2
039600     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     SQ1104.2
039700 BAIL-OUT-EX. EXIT.                                               SQ1104.2
039800 CCVS1-EXIT.                                                      SQ1104.2
039900     EXIT.                                                        SQ1104.2
040000 SECT-SQ110M-0001 SECTION.                                        SQ1104.2
040100 SEQ-INIT-007.                                                    SQ1104.2
040200     MOVE "SQ-FS3" TO XFILE-NAME (1).                             SQ1104.2
040300     MOVE "R1-F-G" TO XRECORD-NAME (1).                           SQ1104.2
040400     MOVE CCVS-PGM-ID TO XPROGRAM-NAME (1).                       SQ1104.2
040500     MOVE 120 TO XRECORD-LENGTH (1).                              SQ1104.2
040600     MOVE "CH" TO CHARS-OR-RECORDS (1).                           SQ1104.2
040700     MOVE 120 TO XBLOCK-SIZE (1).                                 SQ1104.2
040800     MOVE 000649 TO RECORDS-IN-FILE (1).                          SQ1104.2
040900     MOVE "SQ" TO XFILE-ORGANIZATION (1).                         SQ1104.2
041000     MOVE "S" TO XLABEL-TYPE (1).                                 SQ1104.2
041100     MOVE 000001 TO XRECORD-NUMBER (1).                           SQ1104.2
041200     OPEN OUTPUT SQ-FS3.                                          SQ1104.2
041300 SEQ-TEST-007.                                                    SQ1104.2
041400     MOVE FILE-RECORD-INFO-P1-120 (1) TO SQ-FS3R1-F-G-120.        SQ1104.2
041500     WRITE SQ-FS3R1-F-G-120.                                      SQ1104.2
041600     IF      XRECORD-NUMBER (1) EQUAL TO 196                      SQ1104.2
041700*             ADD 1 TO REELUNIT-NUMBER (1)                        SQ1104.2
041800*             CLOSE SQ-FS3 UNIT.                                  SQ1104.2
041900*    MOVE "CLOSE UNIT DELETED" TO RE-MARK.                        SQ1104.2
042000     IF XRECORD-NUMBER (1) EQUAL TO 649                           SQ1104.2
042100        GO TO SEQ-WRITE-007.                                      SQ1104.2
042200     ADD 1 TO XRECORD-NUMBER (1).                                 SQ1104.2
042300     GO TO SEQ-TEST-007.                                          SQ1104.2
042400 SEQ-WRITE-007.                                                   SQ1104.2
042500     MOVE "CREATE FILE SQ-FS3" TO FEATURE.                        SQ1104.2
042600     MOVE "SEQ-TEST-007" TO PAR-NAME.                             SQ1104.2
042700     MOVE "FILE CREATED, RECS =" TO COMPUTED-A.                   SQ1104.2
042800     MOVE XRECORD-NUMBER (1) TO CORRECT-18V0.                     SQ1104.2
042900     PERFORM PRINT-DETAIL.                                        SQ1104.2
043000     CLOSE SQ-FS3.                                                SQ1104.2
043100*        A MASS STORAGE SEQUENTIAL FILE WITH 120 CHARACTER        SQ1104.2
043200*    RECORDS HAS BEEN CREATED.  THE FILE CONTAINS 649 RECORDS.    SQ1104.2
043300 SEQ-INIT-008.                                                    SQ1104.2
043400     MOVE ZERO TO WRK-CS-09V00.                                   SQ1104.2
043500*        THIS TEST READS AND CHECKS THE FILE CREATED IN           SQ1104.2
043600*    SEQ-TEST-007.                                                SQ1104.2
043700     OPEN INPUT SQ-FS3.                                           SQ1104.2
043800 SEQ-TEST-008.                                                    SQ1104.2
043900     READ SQ-FS3 RECORD                                           SQ1104.2
044000         AT END GO TO SEQ-TEST-008-1.                             SQ1104.2
044100     MOVE SQ-FS3R1-F-G-120 TO FILE-RECORD-INFO-P1-120 (1).        SQ1104.2
044200     ADD 1 TO WRK-CS-09V00.                                       SQ1104.2
044300     IF WRK-CS-09V00 GREATER THAN 649                             SQ1104.2
044400         MOVE "MORE THAN 649 RECORDS" TO RE-MARK                  SQ1104.2
044500         GO TO SEQ-FAIL-008.                                      SQ1104.2
044600     IF WRK-CS-09V00 NOT EQUAL TO XRECORD-NUMBER (1)              SQ1104.2
044700         ADD 1 TO RECORDS-IN-ERROR                                SQ1104.2
044800         GO TO SEQ-TEST-008.                                      SQ1104.2
044900     IF XFILE-NAME (1) NOT EQUAL TO "SQ-FS3"                      SQ1104.2
045000         ADD 1 TO RECORDS-IN-ERROR                                SQ1104.2
045100         GO TO SEQ-TEST-008.                                      SQ1104.2
045200     IF XLABEL-TYPE (1) NOT EQUAL TO "S"                          SQ1104.2
045300     ADD 1 TO RECORDS-IN-ERROR.                                   SQ1104.2
045400     GO TO SEQ-TEST-008.                                          SQ1104.2
045500 SEQ-TEST-008-1.                                                  SQ1104.2
045600     IF RECORDS-IN-ERROR EQUAL TO ZERO                            SQ1104.2
045700          GO TO SEQ-PASS-008.                                     SQ1104.2
045800     MOVE "ERRORS IN READING SQ-FS3" TO RE-MARK.                  SQ1104.2
045900 SEQ-FAIL-008.                                                    SQ1104.2
046000     MOVE RECORDS-IN-ERROR TO CORRECT-18V0.                       SQ1104.2
046100     PERFORM FAIL.                                                SQ1104.2
046200     GO TO SEQ-WRITE-008.                                         SQ1104.2
046300 SEQ-PASS-008.                                                    SQ1104.2
046400     PERFORM PASS.                                                SQ1104.2
046500     MOVE "FILE VERIFIED RECS =" TO COMPUTED-A.                   SQ1104.2
046600     MOVE WRK-CS-09V00 TO CORRECT-18V0.                           SQ1104.2
046700 SEQ-WRITE-008.                                                   SQ1104.2
046800     MOVE "SEQ-TEST-008" TO PAR-NAME.                             SQ1104.2
046900     MOVE "VERIFY FILE SQ-FS3" TO FEATURE.                        SQ1104.2
047000     PERFORM PRINT-DETAIL.                                        SQ1104.2
047100 SEQ-CLOSE-008.                                                   SQ1104.2
047200     CLOSE SQ-FS3.                                                SQ1104.2
047300 READ-INIT-GF-01.                                                 SQ1104.2
047400     MOVE ZERO TO WRK-CS-09V00.                                   SQ1104.2
047500     MOVE ZERO TO RECORDS-IN-ERROR.                               SQ1104.2
047600     OPEN INPUT SQ-FS3.                                           SQ1104.2
047700*        FOUR OPTIONS FOR THE READ STATEMENT ARE CHECKED          SQ1104.2
047800*    IN THIS SERIES OF TESTS.                                     SQ1104.2
047900     MOVE "READ...RECORD AT END ..." TO FEATURE.                  SQ1104.2
048000     MOVE "READ-TEST-GF-01" TO PAR-NAME.                          SQ1104.2
048100     MOVE ZERO TO ERROR-FLAG.                                     SQ1104.2
048200 READ-TEST-GF-01.                                                 SQ1104.2
048300     READ SQ-FS3 RECORD                                           SQ1104.2
048400          AT END MOVE "UNEXPECTED EOF" TO COMPUTED-A              SQ1104.2
048500          MOVE 1 TO EOF-FLAG                                      SQ1104.2
048600          GO TO READ-FAIL-GF-01.                                  SQ1104.2
048700     PERFORM RECORD-CHECK.                                        SQ1104.2
048800     IF WRK-CS-09V00 EQUAL TO 50                                  SQ1104.2
048900           GO TO READ-TEST-GF-01-1.                               SQ1104.2
049000     GO TO READ-TEST-GF-01.                                       SQ1104.2
049100 RECORD-CHECK.                                                    SQ1104.2
049200     MOVE SQ-FS3R1-F-G-120 TO FILE-RECORD-INFO-P1-120 (1).        SQ1104.2
049300     ADD 1 TO WRK-CS-09V00.                                       SQ1104.2
049400     IF WRK-CS-09V00 NOT EQUAL TO XRECORD-NUMBER (1)              SQ1104.2
049500         ADD 1 TO RECORDS-IN-ERROR                                SQ1104.2
049600         MOVE 1 TO ERROR-FLAG.                                    SQ1104.2
049700 READ-TEST-GF-01-1.                                               SQ1104.2
049800     IF ERROR-FLAG EQUAL TO ZERO                                  SQ1104.2
049900         GO TO READ-PASS-GF-01.                                   SQ1104.2
050000     MOVE "ERROR IN RECORD(S)" TO COMPUTED-A.                     SQ1104.2
050100 READ-FAIL-GF-01.                                                 SQ1104.2
050200     MOVE "VII-44; 4.4.2                            " TO  RE-MARK.SQ1104.2
050300     PERFORM FAIL.                                                SQ1104.2
050400     GO TO READ-WRITE-GF-01.                                      SQ1104.2
050500 READ-PASS-GF-01.                                                 SQ1104.2
050600     PERFORM PASS.                                                SQ1104.2
050700 READ-WRITE-GF-01.                                                SQ1104.2
050800     PERFORM PRINT-DETAIL.                                        SQ1104.2
050900 READ-INIT-GF-02.                                                 SQ1104.2
051000     IF EOF-FLAG EQUAL TO 1                                       SQ1104.2
051100         GO TO SEQ-EOF-009.                                       SQ1104.2
051200     MOVE ZERO TO ERROR-FLAG.                                     SQ1104.2
051300     MOVE "READ...AT END..." TO FEATURE.                          SQ1104.2
051400     MOVE "READ-TEST-GF-02" TO PAR-NAME.                          SQ1104.2
051500 READ-TEST-GF-02.                                                 SQ1104.2
051600     READ SQ-FS3  AT END                                          SQ1104.2
051700          MOVE "UNEXPECTED EOF" TO COMPUTED-A                     SQ1104.2
051800          MOVE 1 TO EOF-FLAG                                      SQ1104.2
051900          GO TO READ-FAIL-GF-02.                                  SQ1104.2
052000     PERFORM RECORD-CHECK.                                        SQ1104.2
052100     IF WRK-CS-09V00 EQUAL TO 200                                 SQ1104.2
052200         GO TO READ-TEST-GF-02-1.                                 SQ1104.2
052300     GO TO READ-TEST-GF-02.                                       SQ1104.2
052400 READ-TEST-GF-02-1.                                               SQ1104.2
052500     IF ERROR-FLAG EQUAL TO ZERO                                  SQ1104.2
052600          GO TO READ-PASS-GF-02.                                  SQ1104.2
052700     MOVE "ERROR IN RECORD(S)" TO COMPUTED-A.                     SQ1104.2
052800 READ-FAIL-GF-02.                                                 SQ1104.2
052900     MOVE "VII-44; 4.4.2                            " TO  RE-MARK.SQ1104.2
053000     PERFORM FAIL.                                                SQ1104.2
053100     GO TO READ-WRITE-GF-02.                                      SQ1104.2
053200 READ-PASS-GF-02.                                                 SQ1104.2
053300     PERFORM PASS.                                                SQ1104.2
053400 READ-WRITE-GF-02.                                                SQ1104.2
053500     PERFORM PRINT-DETAIL.                                        SQ1104.2
053600 READ-INIT-GF-03.                                                 SQ1104.2
053700     IF EOF-FLAG EQUAL TO 1                                       SQ1104.2
053800         GO TO SEQ-EOF-009.                                       SQ1104.2
053900     MOVE ZERO TO ERROR-FLAG.                                     SQ1104.2
054000     MOVE "READ...RECORD END..." TO FEATURE.                      SQ1104.2
054100     MOVE "READ-TEST-GF-03" TO PAR-NAME.                          SQ1104.2
054200 READ-TEST-GF-03.                                                 SQ1104.2
054300     READ SQ-FS3 RECORD END                                       SQ1104.2
054400          MOVE "UNEXPECTED EOF" TO COMPUTED-A                     SQ1104.2
054500          MOVE 1 TO EOF-FLAG                                      SQ1104.2
054600          GO TO READ-FAIL-GF-03.                                  SQ1104.2
054700     PERFORM RECORD-CHECK.                                        SQ1104.2
054800     IF WRK-CS-09V00 EQUAL TO 499                                 SQ1104.2
054900          GO TO READ-TEST-GF-03-1.                                SQ1104.2
055000     GO TO READ-TEST-GF-03.                                       SQ1104.2
055100 READ-TEST-GF-03-1.                                               SQ1104.2
055200     IF ERROR-FLAG EQUAL TO ZERO                                  SQ1104.2
055300           GO TO READ-PASS-GF-03.                                 SQ1104.2
055400     MOVE "ERROR IN RECORD(S)" TO COMPUTED-A.                     SQ1104.2
055500 READ-FAIL-GF-03.                                                 SQ1104.2
055600     MOVE "VII-44; 4.4.2                            " TO  RE-MARK.SQ1104.2
055700     PERFORM FAIL.                                                SQ1104.2
055800     GO TO READ-WRITE-GF-03.                                      SQ1104.2
055900 READ-PASS-GF-03.                                                 SQ1104.2
056000     PERFORM PASS.                                                SQ1104.2
056100 READ-WRITE-GF-03.                                                SQ1104.2
056200     PERFORM PRINT-DETAIL.                                        SQ1104.2
056300 READ-INIT-GF-04.                                                 SQ1104.2
056400     IF EOF-FLAG EQUAL TO 1                                       SQ1104.2
056500         GO TO SEQ-EOF-009.                                       SQ1104.2
056600     MOVE ZERO TO ERROR-FLAG.                                     SQ1104.2
056700     MOVE "READ...END..." TO FEATURE.                             SQ1104.2
056800     MOVE "READ-TEST-GF-04" TO PAR-NAME.                          SQ1104.2
056900 READ-TEST-GF-04.                                                 SQ1104.2
057000     READ SQ-FS3 END                                              SQ1104.2
057100          GO TO READ-TEST-GF-04-1.                                SQ1104.2
057200     PERFORM RECORD-CHECK.                                        SQ1104.2
057300     IF WRK-CS-09V00 GREATER THAN 649                             SQ1104.2
057400          GO TO READ-TEST-GF-04-1.                                SQ1104.2
057500     GO TO READ-TEST-GF-04.                                       SQ1104.2
057600 READ-TEST-GF-04-1.                                               SQ1104.2
057700     IF ERROR-FLAG EQUAL TO ZERO                                  SQ1104.2
057800         GO TO READ-PASS-GF-04.                                   SQ1104.2
057900 READ-FAIL-GF-04.                                                 SQ1104.2
058000     MOVE "ERROR IN RECORD(S)" TO COMPUTED-A.                     SQ1104.2
058100     MOVE "VII-44; 4.4.2                            " TO  RE-MARK.SQ1104.2
058200     PERFORM FAIL.                                                SQ1104.2
058300     GO TO READ-WRITE-GF-04.                                      SQ1104.2
058400 READ-PASS-GF-04.                                                 SQ1104.2
058500     PERFORM PASS.                                                SQ1104.2
058600 READ-WRITE-GF-04.                                                SQ1104.2
058700     PERFORM PRINT-DETAIL.                                        SQ1104.2
058800 SEQ-TEST-009.                                                    SQ1104.2
058900     IF RECORDS-IN-ERROR NOT EQUAL TO ZERO                        SQ1104.2
059000          MOVE "RECORDS IN ERROR =" TO COMPUTED-A                 SQ1104.2
059100          MOVE RECORDS-IN-ERROR TO CORRECT-18V0                   SQ1104.2
059200          GO TO SEQ-FAIL-009.                                     SQ1104.2
059300     IF WRK-CS-09V00 GREATER THAN 649                             SQ1104.2
059400          MOVE "MORE THAN 649 RECORDS" TO RE-MARK                 SQ1104.2
059500          GO TO SEQ-FAIL-009.                                     SQ1104.2
059600 SEQ-PASS-009.                                                    SQ1104.2
059700     PERFORM PASS                                                 SQ1104.2
059800     GO TO SEQ-WRITE-009.                                         SQ1104.2
059900 SEQ-EOF-009.                                                     SQ1104.2
060000     MOVE "LESS THAN 649 RECORDS" TO RE-MARK.                     SQ1104.2
060100     MOVE "RECORDS READ =" TO COMPUTED-A.                         SQ1104.2
060200     MOVE WRK-CS-09V00 TO CORRECT-18V0.                           SQ1104.2
060300 SEQ-FAIL-009.                                                    SQ1104.2
060400     PERFORM FAIL.                                                SQ1104.2
060500 SEQ-WRITE-009.                                                   SQ1104.2
060600     MOVE "SEQ-TEST-009" TO PAR-NAME.                             SQ1104.2
060700     MOVE "READ FILE SQ-FS3" TO FEATURE.                          SQ1104.2
060800     PERFORM PRINT-DETAIL.                                        SQ1104.2
060900 SEQ-CLOSE-009.                                                   SQ1104.2
061000     CLOSE SQ-FS3.                                                SQ1104.2
061100 TERMINATE-ROUTINE.                                               SQ1104.2
061200     EXIT.                                                        SQ1104.2
061300 CCVS-EXIT SECTION.                                               SQ1104.2
061400 CCVS-999999.                                                     SQ1104.2
061500     GO TO CLOSE-FILES.                                           SQ1104.2
      *END-OF,SQ110M                                                                  
