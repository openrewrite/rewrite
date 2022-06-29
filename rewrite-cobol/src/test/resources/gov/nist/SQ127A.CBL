      *HEADER,COBOL,SQ127A                                                            
000100 IDENTIFICATION DIVISION.                                         SQ1274.2
000200 PROGRAM-ID.                                                      SQ1274.2
000300     SQ127A.                                                      SQ1274.2
000400****************************************************************  SQ1274.2
000500*                                                              *  SQ1274.2
000600*    VALIDATION FOR:-                                          *  SQ1274.2
000700*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1274.2
000800*                                                              *  SQ1274.2
000900*    CREATION DATE     /     VALIDATION DATE                   *  SQ1274.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ1274.2
001100*                                                              *  SQ1274.2
001200****************************************************************  SQ1274.2
001300                                                                  SQ1274.2
001400*     THIS ROUTINE CHECKS THE SAME AS SQ104 IN COMBINATION        SQ1274.2
001500*     WITH                                                        SQ1274.2
001600*                                                                 SQ1274.2
001700*         SELECT ... ASSIGN TO   "LITERAL-1"                      SQ1274.2
001800*                                -----------                      SQ1274.2
001900*             (X-CARD X-60 IS UESD FOR LITERAL-1)                 SQ1274.2
002000*                                                                 SQ1274.2
002100*                                                                 SQ1274.2
002200*        THE ROUTINE SQ127A CREATES A SEQUENTIAL MASS STORAGE     SQ1274.2
002300*    FILE WHICH HAS FIXED LENGTH RECORDS.  THE FILE IS THEN       SQ1274.2
002400*    CLOSED AND OPENED AS AN INPUT FILE.  THE FILE IS READ AND    SQ1274.2
002500*    FIELDS IN THE INPUT RECORDS ARE COMPARED TO THE VALUES       SQ1274.2
002600*    WRITTEN TO ENSURE THAT THE RECORDS WERE PROCESSED CORRECTLY. SQ1274.2
002700*    THE FILE IS CLOSED AND OPENED AGAIN AS AN INPUT FILE.  FOUR  SQ1274.2
002800*    READ FORMAT OPTIONS ARE USED TO READ THE FILE AND FIELDS IN  SQ1274.2
002900*    THE RECORDS ARE VERIFIED.  THE OPEN, CLOSE, READ, AND WRITE  SQ1274.2
003000*    STATEMENTS ARE TESTED FOR LEVEL ONE FEATURES.                SQ1274.2
003100*                                                                 SQ1274.2
003200*    USED X-CARDS:                                                SQ1274.2
003300*         XXXXX055                                                SQ1274.2
003400*         XXXXX060        FOR "SQ-FS3"                            SQ1274.2
003500*     P   XXXXX062                                                SQ1274.2
003600*         XXXXX082                                                SQ1274.2
003700*         XXXXX083                                                SQ1274.2
003800*     C   XXXXX084                                                SQ1274.2
003900*                                                                 SQ1274.2
004000*                                                                 SQ1274.2
004100 ENVIRONMENT DIVISION.                                            SQ1274.2
004200 CONFIGURATION SECTION.                                           SQ1274.2
004300 SOURCE-COMPUTER.                                                 SQ1274.2
004400     XXXXX082.                                                    SQ1274.2
004500 OBJECT-COMPUTER.                                                 SQ1274.2
004600     XXXXX083.                                                    SQ1274.2
004700 INPUT-OUTPUT SECTION.                                            SQ1274.2
004800 FILE-CONTROL.                                                    SQ1274.2
004900*    SELECT RAW-DATA   ASSIGN TO                                  SQ1274.2
005000*    XXXXX062                                                     SQ1274.2
005100*           ORGANIZATION IS INDEXED                               SQ1274.2
005200*           ACCESS MODE IS RANDOM                                 SQ1274.2
005300*           RECORD KEY IS RAW-DATA-KEY.                           SQ1274.2
005400     SELECT PRINT-FILE ASSIGN TO                                  SQ1274.2
005500     XXXXX055.                                                    SQ1274.2
005600     SELECT SQ-FS3 ASSIGN TO                                      SQ1274.2
005700     XXXXX060                                                     SQ1274.2
005800     ORGANIZATION IS SEQUENTIAL                                   SQ1274.2
005900     ACCESS MODE IS SEQUENTIAL.                                   SQ1274.2
006000 DATA DIVISION.                                                   SQ1274.2
006100 FILE SECTION.                                                    SQ1274.2
006200*                                                                 SQ1274.2
006300*FD  RAW-DATA.                                                    SQ1274.2
006400*                                                                 SQ1274.2
006500*01  RAW-DATA-SATZ.                                               SQ1274.2
006600*    05  RAW-DATA-KEY        PIC X(6).                            SQ1274.2
006700*    05  C-DATE              PIC 9(6).                            SQ1274.2
006800*    05  C-TIME              PIC 9(8).                            SQ1274.2
006900*    05  C-NO-OF-TESTS       PIC 99.                              SQ1274.2
007000*    05  C-OK                PIC 999.                             SQ1274.2
007100*    05  C-ALL               PIC 999.                             SQ1274.2
007200*    05  C-FAIL              PIC 999.                             SQ1274.2
007300*    05  C-DELETED           PIC 999.                             SQ1274.2
007400*    05  C-INSPECT           PIC 999.                             SQ1274.2
007500*    05  C-NOTE              PIC X(13).                           SQ1274.2
007600*    05  C-INDENT            PIC X.                               SQ1274.2
007700*    05  C-ABORT             PIC X(8).                            SQ1274.2
007800 FD  PRINT-FILE                                                   SQ1274.2
007900C    LABEL RECORDS                                                SQ1274.2
008000C    XXXXX084                                                     SQ1274.2
008100C    DATA RECORD IS PRINT-REC DUMMY-RECORD                        SQ1274.2
008200               .                                                  SQ1274.2
008300 01  PRINT-REC PICTURE X(120).                                    SQ1274.2
008400 01  DUMMY-RECORD PICTURE X(120).                                 SQ1274.2
008500 FD  SQ-FS3                                                       SQ1274.2
008600C    LABEL RECORDS ARE STANDARD                                   SQ1274.2
008700C    DATA RECORD SQ-FS3R1-F-G-120                                 SQ1274.2
008800     BLOCK CONTAINS 120 CHARACTERS                                SQ1274.2
008900     RECORD CONTAINS 120 CHARACTERS.                              SQ1274.2
009000 01  SQ-FS3R1-F-G-120.                                            SQ1274.2
009100     02  FILLER PIC X(120).                                       SQ1274.2
009200 WORKING-STORAGE SECTION.                                         SQ1274.2
009300 01  WRK-CS-09V00 PICTURE S9(9) USAGE COMP VALUE ZERO.            SQ1274.2
009400 01  RECORDS-IN-ERROR  PIC S9(5) USAGE COMP VALUE 0.              SQ1274.2
009500 01  ERROR-FLAG PICTURE 9 VALUE 0.                                SQ1274.2
009600 01  EOF-FLAG PICTURE 9 VALUE 0.                                  SQ1274.2
009700 01  FILE-RECORD-INFORMATION-REC.                                 SQ1274.2
009800     03 FILE-RECORD-INFO-SKELETON.                                SQ1274.2
009900        05 FILLER                 PICTURE X(48)       VALUE       SQ1274.2
010000             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  SQ1274.2
010100        05 FILLER                 PICTURE X(46)       VALUE       SQ1274.2
010200             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    SQ1274.2
010300        05 FILLER                 PICTURE X(26)       VALUE       SQ1274.2
010400             ",LFIL=000000,ORG=  ,LBLR= ".                        SQ1274.2
010500        05 FILLER                 PICTURE X(37)       VALUE       SQ1274.2
010600             ",RECKEY=                             ".             SQ1274.2
010700        05 FILLER                 PICTURE X(38)       VALUE       SQ1274.2
010800             ",ALTKEY1=                             ".            SQ1274.2
010900        05 FILLER                 PICTURE X(38)       VALUE       SQ1274.2
011000             ",ALTKEY2=                             ".            SQ1274.2
011100        05 FILLER                 PICTURE X(7)        VALUE SPACE.SQ1274.2
011200     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              SQ1274.2
011300        05 FILE-RECORD-INFO-P1-120.                               SQ1274.2
011400           07 FILLER              PIC X(5).                       SQ1274.2
011500           07 XFILE-NAME           PIC X(6).                      SQ1274.2
011600           07 FILLER              PIC X(8).                       SQ1274.2
011700           07 XRECORD-NAME         PIC X(6).                      SQ1274.2
011800           07 FILLER              PIC X(1).                       SQ1274.2
011900           07 REELUNIT-NUMBER     PIC 9(1).                       SQ1274.2
012000           07 FILLER              PIC X(7).                       SQ1274.2
012100           07 XRECORD-NUMBER       PIC 9(6).                      SQ1274.2
012200           07 FILLER              PIC X(6).                       SQ1274.2
012300           07 UPDATE-NUMBER       PIC 9(2).                       SQ1274.2
012400           07 FILLER              PIC X(5).                       SQ1274.2
012500           07 ODO-NUMBER          PIC 9(4).                       SQ1274.2
012600           07 FILLER              PIC X(5).                       SQ1274.2
012700           07 XPROGRAM-NAME        PIC X(5).                      SQ1274.2
012800           07 FILLER              PIC X(7).                       SQ1274.2
012900           07 XRECORD-LENGTH       PIC 9(6).                      SQ1274.2
013000           07 FILLER              PIC X(7).                       SQ1274.2
013100           07 CHARS-OR-RECORDS    PIC X(2).                       SQ1274.2
013200           07 FILLER              PIC X(1).                       SQ1274.2
013300           07 XBLOCK-SIZE          PIC 9(4).                      SQ1274.2
013400           07 FILLER              PIC X(6).                       SQ1274.2
013500           07 RECORDS-IN-FILE     PIC 9(6).                       SQ1274.2
013600           07 FILLER              PIC X(5).                       SQ1274.2
013700           07 XFILE-ORGANIZATION   PIC X(2).                      SQ1274.2
013800           07 FILLER              PIC X(6).                       SQ1274.2
013900           07 XLABEL-TYPE          PIC X(1).                      SQ1274.2
014000        05 FILE-RECORD-INFO-P121-240.                             SQ1274.2
014100           07 FILLER              PIC X(8).                       SQ1274.2
014200           07 XRECORD-KEY          PIC X(29).                     SQ1274.2
014300           07 FILLER              PIC X(9).                       SQ1274.2
014400           07 ALTERNATE-KEY1      PIC X(29).                      SQ1274.2
014500           07 FILLER              PIC X(9).                       SQ1274.2
014600           07 ALTERNATE-KEY2      PIC X(29).                      SQ1274.2
014700           07 FILLER              PIC X(7).                       SQ1274.2
014800 01  TEST-RESULTS.                                                SQ1274.2
014900     02 FILLER                    PICTURE X VALUE SPACE.          SQ1274.2
015000     02 FEATURE                   PICTURE X(20) VALUE SPACE.      SQ1274.2
015100     02 FILLER                    PICTURE X VALUE SPACE.          SQ1274.2
015200     02 P-OR-F                    PICTURE X(5) VALUE SPACE.       SQ1274.2
015300     02 FILLER                    PICTURE X  VALUE SPACE.         SQ1274.2
015400     02  PAR-NAME.                                                SQ1274.2
015500       03 FILLER PICTURE X(12) VALUE SPACE.                       SQ1274.2
015600       03  PARDOT-X PICTURE X  VALUE SPACE.                       SQ1274.2
015700       03 DOTVALUE PICTURE 99  VALUE ZERO.                        SQ1274.2
015800       03 FILLER PIC X(5) VALUE SPACE.                            SQ1274.2
015900     02 FILLER PIC X(10) VALUE SPACE.                             SQ1274.2
016000     02 RE-MARK PIC X(61).                                        SQ1274.2
016100 01  TEST-COMPUTED.                                               SQ1274.2
016200     02 FILLER PIC X(30) VALUE SPACE.                             SQ1274.2
016300     02 FILLER PIC X(17) VALUE "       COMPUTED=".                SQ1274.2
016400     02 COMPUTED-X.                                               SQ1274.2
016500     03 COMPUTED-A                PICTURE X(20) VALUE SPACE.      SQ1274.2
016600     03 COMPUTED-N REDEFINES COMPUTED-A PICTURE -9(9).9(9).       SQ1274.2
016700     03 COMPUTED-0V18 REDEFINES COMPUTED-A  PICTURE -.9(18).      SQ1274.2
016800     03 COMPUTED-4V14 REDEFINES COMPUTED-A  PICTURE -9(4).9(14).  SQ1274.2
016900     03 COMPUTED-14V4 REDEFINES COMPUTED-A  PICTURE -9(14).9(4).  SQ1274.2
017000     03       CM-18V0 REDEFINES COMPUTED-A.                       SQ1274.2
017100         04 COMPUTED-18V0                   PICTURE -9(18).       SQ1274.2
017200         04 FILLER                          PICTURE X.            SQ1274.2
017300     03 FILLER PIC X(50) VALUE SPACE.                             SQ1274.2
017400 01  TEST-CORRECT.                                                SQ1274.2
017500     02 FILLER PIC X(30) VALUE SPACE.                             SQ1274.2
017600     02 FILLER PIC X(17) VALUE "       CORRECT =".                SQ1274.2
017700     02 CORRECT-X.                                                SQ1274.2
017800     03 CORRECT-A                 PICTURE X(20) VALUE SPACE.      SQ1274.2
017900     03 CORRECT-N REDEFINES CORRECT-A PICTURE -9(9).9(9).         SQ1274.2
018000     03 CORRECT-0V18 REDEFINES CORRECT-A    PICTURE -.9(18).      SQ1274.2
018100     03 CORRECT-4V14 REDEFINES CORRECT-A    PICTURE -9(4).9(14).  SQ1274.2
018200     03 CORRECT-14V4 REDEFINES CORRECT-A    PICTURE -9(14).9(4).  SQ1274.2
018300     03      CR-18V0 REDEFINES CORRECT-A.                         SQ1274.2
018400         04 CORRECT-18V0                    PICTURE -9(18).       SQ1274.2
018500         04 FILLER                          PICTURE X.            SQ1274.2
018600     03 FILLER PIC X(50) VALUE SPACE.                             SQ1274.2
018700 01  CCVS-C-1.                                                    SQ1274.2
018800     02 FILLER PICTURE IS X(99) VALUE IS " FEATURE              PASQ1274.2
018900-    "SS  PARAGRAPH-NAME                                          SQ1274.2
019000-    "        REMARKS".                                           SQ1274.2
019100     02 FILLER PICTURE IS X(20) VALUE IS SPACE.                   SQ1274.2
019200 01  CCVS-C-2.                                                    SQ1274.2
019300     02 FILLER PICTURE IS X VALUE IS SPACE.                       SQ1274.2
019400     02 FILLER PICTURE IS X(6) VALUE IS "TESTED".                 SQ1274.2
019500     02 FILLER PICTURE IS X(15) VALUE IS SPACE.                   SQ1274.2
019600     02 FILLER PICTURE IS X(4) VALUE IS "FAIL".                   SQ1274.2
019700     02 FILLER PICTURE IS X(94) VALUE IS SPACE.                   SQ1274.2
019800 01  REC-SKL-SUB PICTURE 9(2) VALUE ZERO.                         SQ1274.2
019900 01  REC-CT PICTURE 99 VALUE ZERO.                                SQ1274.2
020000 01  DELETE-CNT                   PICTURE 999  VALUE ZERO.        SQ1274.2
020100 01  ERROR-COUNTER PICTURE IS 999 VALUE IS ZERO.                  SQ1274.2
020200 01  INSPECT-COUNTER PIC 999 VALUE ZERO.                          SQ1274.2
020300 01  PASS-COUNTER PIC 999 VALUE ZERO.                             SQ1274.2
020400 01  TOTAL-ERROR PIC 999 VALUE ZERO.                              SQ1274.2
020500 01  ERROR-HOLD PIC 999 VALUE ZERO.                               SQ1274.2
020600 01  DUMMY-HOLD PIC X(120) VALUE SPACE.                           SQ1274.2
020700 01  RECORD-COUNT PIC 9(5) VALUE ZERO.                            SQ1274.2
020800 01  CCVS-H-1.                                                    SQ1274.2
020900     02  FILLER   PICTURE X(27)  VALUE SPACE.                     SQ1274.2
021000     02 FILLER PICTURE X(67) VALUE                                SQ1274.2
021100     " FEDERAL SOFTWARE TESTING CENTER COBOL COMPILER VALIDATION  SQ1274.2
021200-    " SYSTEM".                                                   SQ1274.2
021300     02  FILLER     PICTURE X(26)  VALUE SPACE.                   SQ1274.2
021400 01  CCVS-H-2.                                                    SQ1274.2
021500     02 FILLER PICTURE X(52) VALUE IS                             SQ1274.2
021600     "CCVS85 FSTC COPY, NOT FOR DISTRIBUTION.".                   SQ1274.2
021700     02 FILLER PICTURE IS X(19) VALUE IS "TEST RESULTS SET-  ".   SQ1274.2
021800     02 TEST-ID PICTURE IS X(9).                                  SQ1274.2
021900     02 FILLER PICTURE IS X(40) VALUE IS SPACE.                   SQ1274.2
022000 01  CCVS-H-3.                                                    SQ1274.2
022100     02  FILLER PICTURE X(34) VALUE                               SQ1274.2
022200     " FOR OFFICIAL USE ONLY    ".                                SQ1274.2
022300     02  FILLER PICTURE X(58) VALUE                               SQ1274.2
022400     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ1274.2
022500     02  FILLER PICTURE X(28) VALUE                               SQ1274.2
022600     "  COPYRIGHT   1985 ".                                       SQ1274.2
022700 01  CCVS-E-1.                                                    SQ1274.2
022800     02 FILLER PICTURE IS X(52) VALUE IS SPACE.                   SQ1274.2
022900     02 FILLER PICTURE IS X(14) VALUE IS "END OF TEST-  ".        SQ1274.2
023000     02 ID-AGAIN PICTURE IS X(9).                                 SQ1274.2
023100     02 FILLER PICTURE X(45) VALUE IS                             SQ1274.2
023200     " NTIS DISTRIBUTION COBOL 85".                               SQ1274.2
023300 01  CCVS-E-2.                                                    SQ1274.2
023400     02  FILLER                   PICTURE X(31)  VALUE            SQ1274.2
023500     SPACE.                                                       SQ1274.2
023600     02  FILLER                   PICTURE X(21)  VALUE SPACE.     SQ1274.2
023700     02 CCVS-E-2-2.                                               SQ1274.2
023800         03 ERROR-TOTAL PICTURE IS XXX VALUE IS SPACE.            SQ1274.2
023900         03 FILLER PICTURE IS X VALUE IS SPACE.                   SQ1274.2
024000         03 ENDER-DESC PIC X(46) VALUE "ERRORS ENCOUNTERED".      SQ1274.2
024100 01  CCVS-E-3.                                                    SQ1274.2
024200     02  FILLER PICTURE X(22) VALUE                               SQ1274.2
024300     " FOR OFFICIAL USE ONLY".                                    SQ1274.2
024400     02  FILLER PICTURE X(12) VALUE SPACE.                        SQ1274.2
024500     02  FILLER PICTURE X(58) VALUE                               SQ1274.2
024600     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1274.2
024700     02  FILLER PICTURE X(13) VALUE SPACE.                        SQ1274.2
024800     02 FILLER PIC X(15) VALUE " COPYRIGHT 1985".                 SQ1274.2
024900 01  CCVS-E-4.                                                    SQ1274.2
025000     02 CCVS-E-4-1 PIC XXX VALUE SPACE.                           SQ1274.2
025100     02 FILLER PIC XXXX VALUE " OF ".                             SQ1274.2
025200     02 CCVS-E-4-2 PIC XXX VALUE SPACE.                           SQ1274.2
025300     02 FILLER PIC X(40) VALUE                                    SQ1274.2
025400      "  TESTS WERE EXECUTED SUCCESSFULLY".                       SQ1274.2
025500 01  XXINFO.                                                      SQ1274.2
025600     02 FILLER PIC X(30) VALUE "        *** INFORMATION  ***".    SQ1274.2
025700     02 INFO-TEXT.                                                SQ1274.2
025800     04 FILLER PIC X(20) VALUE SPACE.                             SQ1274.2
025900     04 XXCOMPUTED PIC X(20).                                     SQ1274.2
026000     04 FILLER PIC X(5) VALUE SPACE.                              SQ1274.2
026100     04 XXCORRECT PIC X(20).                                      SQ1274.2
026200 01  HYPHEN-LINE.                                                 SQ1274.2
026300     02 FILLER PICTURE IS X VALUE IS SPACE.                       SQ1274.2
026400     02 FILLER PICTURE IS X(65) VALUE IS "************************SQ1274.2
026500-    "*****************************************".                 SQ1274.2
026600     02 FILLER PICTURE IS X(54) VALUE IS "************************SQ1274.2
026700-    "******************************".                            SQ1274.2
026800 01  CCVS-PGM-ID PIC X(6) VALUE                                   SQ1274.2
026900     "SQ127A".                                                    SQ1274.2
027000 PROCEDURE DIVISION.                                              SQ1274.2
027100 CCVS1 SECTION.                                                   SQ1274.2
027200 OPEN-FILES.                                                      SQ1274.2
027300*    OPEN I-O RAW-DATA.                                           SQ1274.2
027400*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            SQ1274.2
027500*    READ RAW-DATA INVALID KEY GO TO END-E-1.                     SQ1274.2
027600*    MOVE "ABORTED " TO C-ABORT.                                  SQ1274.2
027700*    ADD 1 TO C-NO-OF-TESTS.                                      SQ1274.2
027800*    ACCEPT C-DATE  FROM DATE.                                    SQ1274.2
027900*    ACCEPT C-TIME  FROM TIME.                                    SQ1274.2
028000*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-1.             SQ1274.2
028100*END-E-1.                                                         SQ1274.2
028200*    CLOSE RAW-DATA.                                              SQ1274.2
028300     OPEN     OUTPUT PRINT-FILE.                                  SQ1274.2
028400     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   SQ1274.2
028500     MOVE    SPACE TO TEST-RESULTS.                               SQ1274.2
028600     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             SQ1274.2
028700     MOVE ZERO TO REC-SKL-SUB.                                    SQ1274.2
028800     PERFORM CCVS-INIT-FILE 9 TIMES.                              SQ1274.2
028900 CCVS-INIT-FILE.                                                  SQ1274.2
029000     ADD 1 TO REC-SKL-SUB.                                        SQ1274.2
029100     MOVE FILE-RECORD-INFO-SKELETON TO                            SQ1274.2
029200                  FILE-RECORD-INFO (REC-SKL-SUB).                 SQ1274.2
029300 CCVS-INIT-EXIT.                                                  SQ1274.2
029400     GO TO CCVS1-EXIT.                                            SQ1274.2
029500 CLOSE-FILES.                                                     SQ1274.2
029600     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   SQ1274.2
029700*    OPEN I-O RAW-DATA.                                           SQ1274.2
029800*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            SQ1274.2
029900*    READ RAW-DATA INVALID KEY GO TO END-E-2.                     SQ1274.2
030000*    MOVE "OK.     " TO C-ABORT.                                  SQ1274.2
030100*    MOVE PASS-COUNTER TO C-OK.                                   SQ1274.2
030200*    MOVE ERROR-HOLD   TO C-ALL.                                  SQ1274.2
030300*    MOVE ERROR-COUNTER TO C-FAIL.                                SQ1274.2
030400*    MOVE DELETE-CNT TO C-DELETED.                                SQ1274.2
030500*    MOVE INSPECT-COUNTER TO C-INSPECT.                           SQ1274.2
030600*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-2.             SQ1274.2
030700*END-E-2.                                                         SQ1274.2
030800*    CLOSE RAW-DATA.                                              SQ1274.2
030900 TERMINATE-CCVS.                                                  SQ1274.2
031000*    EXIT PROGRAM.                                                SQ1274.2
031100*TERMINATE-CALL.                                                  SQ1274.2
031200     STOP     RUN.                                                SQ1274.2
031300 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         SQ1274.2
031400 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           SQ1274.2
031500 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          SQ1274.2
031600 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-CNT.          SQ1274.2
031700     MOVE "****TEST DELETED****" TO RE-MARK.                      SQ1274.2
031800 PRINT-DETAIL.                                                    SQ1274.2
031900     IF REC-CT NOT EQUAL TO ZERO                                  SQ1274.2
032000             MOVE "." TO PARDOT-X                                 SQ1274.2
032100             MOVE REC-CT TO DOTVALUE.                             SQ1274.2
032200     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      SQ1274.2
032300     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               SQ1274.2
032400        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 SQ1274.2
032500          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 SQ1274.2
032600     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              SQ1274.2
032700     MOVE SPACE TO CORRECT-X.                                     SQ1274.2
032800     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         SQ1274.2
032900     MOVE     SPACE TO RE-MARK.                                   SQ1274.2
033000 HEAD-ROUTINE.                                                    SQ1274.2
033100     MOVE CCVS-H-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ1274.2
033200     MOVE CCVS-H-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.   SQ1274.2
033300     MOVE CCVS-H-3 TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.   SQ1274.2
033400 COLUMN-NAMES-ROUTINE.                                            SQ1274.2
033500     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ1274.2
033600     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ1274.2
033700     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ1274.2
033800 END-ROUTINE.                                                     SQ1274.2
033900     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.SQ1274.2
034000 END-RTN-EXIT.                                                    SQ1274.2
034100     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ1274.2
034200 END-ROUTINE-1.                                                   SQ1274.2
034300      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      SQ1274.2
034400      ERROR-HOLD. ADD DELETE-CNT TO ERROR-HOLD.                   SQ1274.2
034500      ADD PASS-COUNTER TO ERROR-HOLD.                             SQ1274.2
034600*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   SQ1274.2
034700      MOVE PASS-COUNTER TO CCVS-E-4-1.                            SQ1274.2
034800      MOVE ERROR-HOLD TO CCVS-E-4-2.                              SQ1274.2
034900      MOVE CCVS-E-4 TO CCVS-E-2-2.                                SQ1274.2
035000      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           SQ1274.2
035100  END-ROUTINE-12.                                                 SQ1274.2
035200      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        SQ1274.2
035300     IF       ERROR-COUNTER IS EQUAL TO ZERO                      SQ1274.2
035400         MOVE "NO " TO ERROR-TOTAL                                SQ1274.2
035500         ELSE                                                     SQ1274.2
035600         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       SQ1274.2
035700     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           SQ1274.2
035800     PERFORM WRITE-LINE.                                          SQ1274.2
035900 END-ROUTINE-13.                                                  SQ1274.2
036000     IF DELETE-CNT IS EQUAL TO ZERO                               SQ1274.2
036100         MOVE "NO " TO ERROR-TOTAL  ELSE                          SQ1274.2
036200         MOVE DELETE-CNT TO ERROR-TOTAL.                          SQ1274.2
036300     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   SQ1274.2
036400     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ1274.2
036500      IF   INSPECT-COUNTER EQUAL TO ZERO                          SQ1274.2
036600          MOVE "NO " TO ERROR-TOTAL                               SQ1274.2
036700      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   SQ1274.2
036800      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            SQ1274.2
036900      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          SQ1274.2
037000     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ1274.2
037100 WRITE-LINE.                                                      SQ1274.2
037200     ADD 1 TO RECORD-COUNT.                                       SQ1274.2
037300*    IF RECORD-COUNT GREATER 50                                   SQ1274.2
037400*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          SQ1274.2
037500*        MOVE SPACE TO DUMMY-RECORD                               SQ1274.2
037600*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  SQ1274.2
037700*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             SQ1274.2
037800*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     SQ1274.2
037900*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          SQ1274.2
038000*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          SQ1274.2
038100*        MOVE ZERO TO RECORD-COUNT.                               SQ1274.2
038200     PERFORM WRT-LN.                                              SQ1274.2
038300 WRT-LN.                                                          SQ1274.2
038400     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               SQ1274.2
038500     MOVE SPACE TO DUMMY-RECORD.                                  SQ1274.2
038600 BLANK-LINE-PRINT.                                                SQ1274.2
038700     PERFORM WRT-LN.                                              SQ1274.2
038800 FAIL-ROUTINE.                                                    SQ1274.2
038900     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.   SQ1274.2
039000     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.    SQ1274.2
039100     MOVE "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.    SQ1274.2
039200     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     SQ1274.2
039300     GO TO FAIL-ROUTINE-EX.                                       SQ1274.2
039400 FAIL-ROUTINE-WRITE.                                              SQ1274.2
039500     MOVE TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE           SQ1274.2
039600     MOVE TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES.   SQ1274.2
039700 FAIL-ROUTINE-EX. EXIT.                                           SQ1274.2
039800 BAIL-OUT.                                                        SQ1274.2
039900     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.       SQ1274.2
040000     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.               SQ1274.2
040100 BAIL-OUT-WRITE.                                                  SQ1274.2
040200     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  SQ1274.2
040300     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     SQ1274.2
040400 BAIL-OUT-EX. EXIT.                                               SQ1274.2
040500 CCVS1-EXIT.                                                      SQ1274.2
040600     EXIT.                                                        SQ1274.2
040700 SECT-SQ127A-0001 SECTION.                                        SQ1274.2
040800 SEQ-INIT-007.                                                    SQ1274.2
040900     MOVE "SQ-FS3" TO XFILE-NAME (1).                             SQ1274.2
041000     MOVE "R1-F-G" TO XRECORD-NAME (1).                           SQ1274.2
041100     MOVE CCVS-PGM-ID TO XPROGRAM-NAME (1).                       SQ1274.2
041200     MOVE 120 TO XRECORD-LENGTH (1).                              SQ1274.2
041300     MOVE "CH" TO CHARS-OR-RECORDS (1).                           SQ1274.2
041400     MOVE 120 TO XBLOCK-SIZE (1).                                 SQ1274.2
041500     MOVE 000649 TO RECORDS-IN-FILE (1).                          SQ1274.2
041600     MOVE "SQ" TO XFILE-ORGANIZATION (1).                         SQ1274.2
041700     MOVE "S" TO XLABEL-TYPE (1).                                 SQ1274.2
041800     MOVE 000001 TO XRECORD-NUMBER (1).                           SQ1274.2
041900     OPEN OUTPUT SQ-FS3.                                          SQ1274.2
042000 SEQ-TEST-007.                                                    SQ1274.2
042100     MOVE FILE-RECORD-INFO-P1-120 (1) TO SQ-FS3R1-F-G-120.        SQ1274.2
042200     WRITE SQ-FS3R1-F-G-120.                                      SQ1274.2
042300     IF XRECORD-NUMBER (1) EQUAL TO 649                           SQ1274.2
042400        GO TO SEQ-WRITE-007.                                      SQ1274.2
042500     ADD 1 TO XRECORD-NUMBER (1).                                 SQ1274.2
042600     GO TO SEQ-TEST-007.                                          SQ1274.2
042700 SEQ-WRITE-007.                                                   SQ1274.2
042800     MOVE "CREATE FILE SQ-FS3" TO FEATURE.                        SQ1274.2
042900     MOVE "SEQ-TEST-007" TO PAR-NAME.                             SQ1274.2
043000     MOVE "FILE CREATED, RECS =" TO COMPUTED-A.                   SQ1274.2
043100     MOVE XRECORD-NUMBER (1) TO CORRECT-18V0.                     SQ1274.2
043200     PERFORM PRINT-DETAIL.                                        SQ1274.2
043300     CLOSE SQ-FS3.                                                SQ1274.2
043400*        A MASS STORAGE SEQUENTIAL FILE WITH 120 CHARACTER        SQ1274.2
043500*    RECORDS HAS BEEN CREATED.  THE FILE CONTAINS 649 RECORDS.    SQ1274.2
043600 READ-INIT-GF-01.                                                 SQ1274.2
043700     MOVE ZERO TO WRK-CS-09V00.                                   SQ1274.2
043800*        THIS TEST READS AND CHECKS THE FILE CREATED IN           SQ1274.2
043900*    SEQ-TEST-007.                                                SQ1274.2
044000     OPEN INPUT SQ-FS3.                                           SQ1274.2
044100 READ-TEST-GF-01.                                                 SQ1274.2
044200     READ SQ-FS3 RECORD                                           SQ1274.2
044300         AT END GO TO READ-TEST-GF-01-1.                          SQ1274.2
044400     MOVE SQ-FS3R1-F-G-120 TO FILE-RECORD-INFO-P1-120 (1).        SQ1274.2
044500     ADD 1 TO WRK-CS-09V00.                                       SQ1274.2
044600     IF WRK-CS-09V00 GREATER THAN 649                             SQ1274.2
044700         MOVE "MORE THAN 649 RECORDS" TO RE-MARK                  SQ1274.2
044800         GO TO READ-FAIL-GF-01.                                   SQ1274.2
044900     IF WRK-CS-09V00 NOT EQUAL TO XRECORD-NUMBER (1)              SQ1274.2
045000         ADD 1 TO RECORDS-IN-ERROR                                SQ1274.2
045100         GO TO READ-TEST-GF-01.                                   SQ1274.2
045200     IF XFILE-NAME (1) NOT EQUAL TO "SQ-FS3"                      SQ1274.2
045300         ADD 1 TO RECORDS-IN-ERROR                                SQ1274.2
045400         GO TO READ-TEST-GF-01.                                   SQ1274.2
045500     IF XLABEL-TYPE (1) NOT EQUAL TO "S"                          SQ1274.2
045600     ADD 1 TO RECORDS-IN-ERROR.                                   SQ1274.2
045700     GO TO READ-TEST-GF-01.                                       SQ1274.2
045800 READ-TEST-GF-01-1.                                               SQ1274.2
045900     IF RECORDS-IN-ERROR EQUAL TO ZERO                            SQ1274.2
046000          GO TO READ-PASS-GF-01.                                  SQ1274.2
046100     MOVE "ERRORS IN READING SQ-FS3" TO RE-MARK.                  SQ1274.2
046200 READ-FAIL-GF-01.                                                 SQ1274.2
046300     MOVE RECORDS-IN-ERROR TO CORRECT-18V0.                       SQ1274.2
046400     MOVE "VII-44 4.4.2,                      " TO RE-MARK.       SQ1274.2
046500     PERFORM FAIL.                                                SQ1274.2
046600     GO TO READ-WRITE-GF-01.                                      SQ1274.2
046700 READ-PASS-GF-01.                                                 SQ1274.2
046800     PERFORM PASS.                                                SQ1274.2
046900     MOVE "FILE VERIFIED RECS =" TO COMPUTED-A.                   SQ1274.2
047000     MOVE WRK-CS-09V00 TO CORRECT-18V0.                           SQ1274.2
047100 READ-WRITE-GF-01.                                                SQ1274.2
047200     MOVE "READ-TEST-GF-01" TO PAR-NAME.                          SQ1274.2
047300     MOVE "VERIFY FILE SQ-FS3" TO FEATURE.                        SQ1274.2
047400     PERFORM PRINT-DETAIL.                                        SQ1274.2
047500 SEQ-CLOSE-008.                                                   SQ1274.2
047600     CLOSE SQ-FS3.                                                SQ1274.2
047700 READ-INIT-GF-02.                                                 SQ1274.2
047800     MOVE ZERO TO WRK-CS-09V00.                                   SQ1274.2
047900     MOVE ZERO TO RECORDS-IN-ERROR.                               SQ1274.2
048000     OPEN INPUT SQ-FS3.                                           SQ1274.2
048100*        FOUR OPTIONS FOR THE READ STATEMENT ARE CHECKED          SQ1274.2
048200*    IN THIS SERIES OF TESTS.                                     SQ1274.2
048300     MOVE "READ...RECORD AT END ..." TO FEATURE.                  SQ1274.2
048400     MOVE "READ-TEST-GF-02" TO PAR-NAME.                          SQ1274.2
048500     MOVE ZERO TO ERROR-FLAG.                                     SQ1274.2
048600 READ-TEST-GF-02.                                                 SQ1274.2
048700     READ SQ-FS3 RECORD                                           SQ1274.2
048800          AT END MOVE "UNEXPECTED EOF" TO COMPUTED-A              SQ1274.2
048900          MOVE 1 TO EOF-FLAG                                      SQ1274.2
049000          GO TO READ-FAIL-GF-02.                                  SQ1274.2
049100     PERFORM RECORD-CHECK.                                        SQ1274.2
049200     IF WRK-CS-09V00 EQUAL TO 50                                  SQ1274.2
049300           GO TO READ-TEST-GF-02-1.                               SQ1274.2
049400     GO TO READ-TEST-GF-02.                                       SQ1274.2
049500 RECORD-CHECK.                                                    SQ1274.2
049600     MOVE SQ-FS3R1-F-G-120 TO FILE-RECORD-INFO-P1-120 (1).        SQ1274.2
049700     ADD 1 TO WRK-CS-09V00.                                       SQ1274.2
049800     IF WRK-CS-09V00 NOT EQUAL TO XRECORD-NUMBER (1)              SQ1274.2
049900         ADD 1 TO RECORDS-IN-ERROR                                SQ1274.2
050000         MOVE 1 TO ERROR-FLAG.                                    SQ1274.2
050100 READ-TEST-GF-02-1.                                               SQ1274.2
050200     IF ERROR-FLAG EQUAL TO ZERO                                  SQ1274.2
050300         GO TO READ-PASS-GF-02.                                   SQ1274.2
050400     MOVE "ERROR IN RECORD(S)" TO COMPUTED-A.                     SQ1274.2
050500 READ-FAIL-GF-02.                                                 SQ1274.2
050600     MOVE "VII-44 4.4.2,                      " TO RE-MARK.       SQ1274.2
050700     PERFORM FAIL.                                                SQ1274.2
050800     GO TO READ-WRITE-GF-02.                                      SQ1274.2
050900 READ-PASS-GF-02.                                                 SQ1274.2
051000     PERFORM PASS.                                                SQ1274.2
051100 READ-WRITE-GF-02.                                                SQ1274.2
051200     PERFORM PRINT-DETAIL.                                        SQ1274.2
051300 READ-INIT-GF-03.                                                 SQ1274.2
051400     IF EOF-FLAG EQUAL TO 1                                       SQ1274.2
051500         GO TO READ-EOF-GF-06.                                    SQ1274.2
051600     MOVE ZERO TO ERROR-FLAG.                                     SQ1274.2
051700     MOVE "READ...AT END..." TO FEATURE.                          SQ1274.2
051800     MOVE "READ-TEST-GF-03" TO PAR-NAME.                          SQ1274.2
051900 READ-TEST-GF-03.                                                 SQ1274.2
052000     READ SQ-FS3  AT END                                          SQ1274.2
052100          MOVE "UNEXPECTED EOF" TO COMPUTED-A                     SQ1274.2
052200          MOVE 1 TO EOF-FLAG                                      SQ1274.2
052300          GO TO READ-FAIL-GF-03.                                  SQ1274.2
052400     PERFORM RECORD-CHECK.                                        SQ1274.2
052500     IF WRK-CS-09V00 EQUAL TO 200                                 SQ1274.2
052600         GO TO READ-TEST-GF-03-1.                                 SQ1274.2
052700     GO TO READ-TEST-GF-03.                                       SQ1274.2
052800 READ-TEST-GF-03-1.                                               SQ1274.2
052900     IF ERROR-FLAG EQUAL TO ZERO                                  SQ1274.2
053000          GO TO READ-PASS-GF-03.                                  SQ1274.2
053100     MOVE "ERROR IN RECORD(S)" TO COMPUTED-A.                     SQ1274.2
053200 READ-FAIL-GF-03.                                                 SQ1274.2
053300     MOVE "VII-44 4.4.2,                      " TO RE-MARK.       SQ1274.2
053400     PERFORM FAIL.                                                SQ1274.2
053500     GO TO READ-WRITE-GF-03.                                      SQ1274.2
053600 READ-PASS-GF-03.                                                 SQ1274.2
053700     PERFORM PASS.                                                SQ1274.2
053800 READ-WRITE-GF-03.                                                SQ1274.2
053900     PERFORM PRINT-DETAIL.                                        SQ1274.2
054000 READ-INIT-GF-04.                                                 SQ1274.2
054100     IF EOF-FLAG EQUAL TO 1                                       SQ1274.2
054200         GO TO READ-EOF-GF-06.                                    SQ1274.2
054300     MOVE ZERO TO ERROR-FLAG.                                     SQ1274.2
054400     MOVE "READ...RECORD END..." TO FEATURE.                      SQ1274.2
054500     MOVE "READ-TEST-GF-04" TO PAR-NAME.                          SQ1274.2
054600 READ-TEST-GF-04.                                                 SQ1274.2
054700     READ SQ-FS3 RECORD END                                       SQ1274.2
054800          MOVE "UNEXPECTED EOF" TO COMPUTED-A                     SQ1274.2
054900          MOVE 1 TO EOF-FLAG                                      SQ1274.2
055000          GO TO READ-FAIL-GF-04.                                  SQ1274.2
055100     PERFORM RECORD-CHECK.                                        SQ1274.2
055200     IF WRK-CS-09V00 EQUAL TO 499                                 SQ1274.2
055300          GO TO READ-TEST-GF-04-1.                                SQ1274.2
055400     GO TO READ-TEST-GF-04.                                       SQ1274.2
055500 READ-TEST-GF-04-1.                                               SQ1274.2
055600     IF ERROR-FLAG EQUAL TO ZERO                                  SQ1274.2
055700           GO TO READ-PASS-GF-04.                                 SQ1274.2
055800     MOVE "ERROR IN RECORD(S)" TO COMPUTED-A.                     SQ1274.2
055900 READ-FAIL-GF-04.                                                 SQ1274.2
056000     MOVE "VII-44 4.4.2,                      " TO RE-MARK.       SQ1274.2
056100     PERFORM FAIL.                                                SQ1274.2
056200     GO TO READ-WRITE-GF-04.                                      SQ1274.2
056300 READ-PASS-GF-04.                                                 SQ1274.2
056400     PERFORM PASS.                                                SQ1274.2
056500 READ-WRITE-GF-04.                                                SQ1274.2
056600     PERFORM PRINT-DETAIL.                                        SQ1274.2
056700 READ-INIT-GF-05.                                                 SQ1274.2
056800     IF EOF-FLAG EQUAL TO 1                                       SQ1274.2
056900         GO TO READ-EOF-GF-06.                                    SQ1274.2
057000     MOVE ZERO TO ERROR-FLAG.                                     SQ1274.2
057100     MOVE "READ...END..." TO FEATURE.                             SQ1274.2
057200     MOVE "READ-TEST-GF-05" TO PAR-NAME.                          SQ1274.2
057300 READ-TEST-GF-05.                                                 SQ1274.2
057400     READ SQ-FS3 END                                              SQ1274.2
057500          GO TO READ-TEST-GF-05-1.                                SQ1274.2
057600     PERFORM RECORD-CHECK.                                        SQ1274.2
057700     IF WRK-CS-09V00 GREATER THAN 649                             SQ1274.2
057800          GO TO READ-TEST-GF-05-1.                                SQ1274.2
057900     GO TO READ-TEST-GF-05.                                       SQ1274.2
058000 READ-TEST-GF-05-1.                                               SQ1274.2
058100     IF ERROR-FLAG EQUAL TO ZERO                                  SQ1274.2
058200         GO TO READ-PASS-GF-05.                                   SQ1274.2
058300 READ-FAIL-GF-05.                                                 SQ1274.2
058400     MOVE "ERROR IN RECORD(S)" TO COMPUTED-A.                     SQ1274.2
058500     MOVE "VII-44 4.4.2,                      " TO RE-MARK.       SQ1274.2
058600     PERFORM FAIL.                                                SQ1274.2
058700     GO TO READ-WRITE-GF-05.                                      SQ1274.2
058800 READ-PASS-GF-05.                                                 SQ1274.2
058900     PERFORM PASS.                                                SQ1274.2
059000 READ-WRITE-GF-05.                                                SQ1274.2
059100     PERFORM PRINT-DETAIL.                                        SQ1274.2
059200 READ-TEST-GF-06.                                                 SQ1274.2
059300     IF RECORDS-IN-ERROR NOT EQUAL TO ZERO                        SQ1274.2
059400          MOVE "RECORDS IN ERROR =" TO COMPUTED-A                 SQ1274.2
059500          MOVE RECORDS-IN-ERROR TO CORRECT-18V0                   SQ1274.2
059600          GO TO READ-FAIL-GF-06.                                  SQ1274.2
059700     IF WRK-CS-09V00 GREATER THAN 649                             SQ1274.2
059800          MOVE "MORE THAN 649 RECORDS" TO RE-MARK                 SQ1274.2
059900          GO TO READ-FAIL-GF-06.                                  SQ1274.2
060000 READ-PASS-GF-06.                                                 SQ1274.2
060100     PERFORM PASS                                                 SQ1274.2
060200     GO TO READ-WRITE-GF-06.                                      SQ1274.2
060300 READ-EOF-GF-06.                                                  SQ1274.2
060400     MOVE "LESS THAN 649 RECORDS" TO RE-MARK.                     SQ1274.2
060500     MOVE "RECORDS READ =" TO COMPUTED-A.                         SQ1274.2
060600     MOVE WRK-CS-09V00 TO CORRECT-18V0.                           SQ1274.2
060700 READ-FAIL-GF-06.                                                 SQ1274.2
060800     PERFORM FAIL.                                                SQ1274.2
060900 READ-WRITE-GF-06.                                                SQ1274.2
061000     MOVE "READ-TEST-GF-06" TO PAR-NAME.                          SQ1274.2
061100     MOVE "READ FILE SQ-FS3" TO FEATURE.                          SQ1274.2
061200     PERFORM PRINT-DETAIL.                                        SQ1274.2
061300 READ-CLOSE-GF-06.                                                SQ1274.2
061400     CLOSE SQ-FS3.                                                SQ1274.2
061500 TERMINATE-ROUTINE.                                               SQ1274.2
061600     EXIT.                                                        SQ1274.2
061700 CCVS-EXIT SECTION.                                               SQ1274.2
061800 CCVS-999999.                                                     SQ1274.2
061900     GO TO CLOSE-FILES.                                           SQ1274.2
      *END-OF,SQ127A                                                                  
