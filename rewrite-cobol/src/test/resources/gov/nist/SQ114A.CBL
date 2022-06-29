      *HEADER,COBOL,SQ114A                                                            
000100 IDENTIFICATION DIVISION.                                         SQ1144.2
000200 PROGRAM-ID.                                                      SQ1144.2
000300     SQ114A.                                                      SQ1144.2
000400****************************************************************  SQ1144.2
000500*                                                              *  SQ1144.2
000600*    VALIDATION FOR:-                                          *  SQ1144.2
000700*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1144.2
000800*                                                              *  SQ1144.2
000900*    CREATION DATE     /     VALIDATION DATE                   *  SQ1144.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ1144.2
001100*                                                              *  SQ1144.2
001200****************************************************************  SQ1144.2
001300                                                                  SQ1144.2
001400*                                                                 SQ1144.2
001500*                                                                 SQ1144.2
001600*         NEW TEST:                                               SQ1144.2
001700*                         OPEN OUTPUT  FILE-1, FILE-2.            SQ1144.2
001800*                         OPEN  INPUT  FILE-1, FILE-2.            SQ1144.2
001900*                         CLOSE FILE-1, FILE-2.                   SQ1144.2
002000*                                                                 SQ1144.2
002100*        THE ROUTINE SQ114A TESTS THE USE OF THE SAME AREA        SQ1144.2
002200*    CLAUSE FOR TWO FILES, ONE A TAPE FILE AND THE OTHER A        SQ1144.2
002300*    MASS STORAGE FILE.  THIS ROUTINE IS A COMBINATION OF THE     SQ1144.2
002400*    ROUTINES SQ102 AND SQ104.                                    SQ1144.2
002500*                                                                 SQ1144.2
002600*    USED X-CARDS:                                                SQ1144.2
002700*         XXXXX001                                                SQ1144.2
002800*         XXXXX014                                                SQ1144.2
002900*         XXXXX055                                                SQ1144.2
003000*     P   XXXXX062                                                SQ1144.2
003100*         XXXXX082                                                SQ1144.2
003200*         XXXXX083                                                SQ1144.2
003300*     C   XXXXX084                                                SQ1144.2
003400*                                                                 SQ1144.2
003500*                                                                 SQ1144.2
003600 ENVIRONMENT DIVISION.                                            SQ1144.2
003700 CONFIGURATION SECTION.                                           SQ1144.2
003800 SOURCE-COMPUTER.                                                 SQ1144.2
003900     XXXXX082.                                                    SQ1144.2
004000 OBJECT-COMPUTER.                                                 SQ1144.2
004100     XXXXX083.                                                    SQ1144.2
004200 INPUT-OUTPUT SECTION.                                            SQ1144.2
004300 FILE-CONTROL.                                                    SQ1144.2
004400*    SELECT RAW-DATA   ASSIGN TO                                  SQ1144.2
004500*    XXXXX062                                                     SQ1144.2
004600*           ORGANIZATION IS INDEXED                               SQ1144.2
004700*           ACCESS MODE IS RANDOM                                 SQ1144.2
004800*           RECORD KEY IS RAW-DATA-KEY.                           SQ1144.2
004900     SELECT PRINT-FILE ASSIGN TO                                  SQ1144.2
005000     XXXXX055.                                                    SQ1144.2
005100     SELECT SQ-FS1 ASSIGN TO                                      SQ1144.2
005200     XXXXX001                                                     SQ1144.2
005300     ORGANIZATION IS SEQUENTIAL                                   SQ1144.2
005400     ACCESS MODE IS SEQUENTIAL                                    SQ1144.2
005500     FILE STATUS IS FILE-STATUS-SQ-FS1.                           SQ1144.2
005600     SELECT SQ-FS2 ASSIGN TO                                      SQ1144.2
005700     XXXXX014                                                     SQ1144.2
005800     ORGANIZATION IS SEQUENTIAL                                   SQ1144.2
005900     ACCESS MODE IS SEQUENTIAL                                    SQ1144.2
006000     FILE STATUS IS FILE-STATUS-SQ-FS2.                           SQ1144.2
006100     SELECT SQ-FS3 ASSIGN TO                                      SQ1144.2
006200     XXXXX014                                                     SQ1144.2
006300     ORGANIZATION IS SEQUENTIAL                                   SQ1144.2
006400     ACCESS MODE IS SEQUENTIAL                                    SQ1144.2
006500     FILE STATUS IS FILE-STATUS-SQ-FS3.                           SQ1144.2
006600 I-O-CONTROL.                                                     SQ1144.2
006700     SAME AREA SQ-FS1 SQ-FS3.                                     SQ1144.2
006800 DATA DIVISION.                                                   SQ1144.2
006900 FILE SECTION.                                                    SQ1144.2
007000*                                                                 SQ1144.2
007100*FD  RAW-DATA.                                                    SQ1144.2
007200*                                                                 SQ1144.2
007300*01  RAW-DATA-SATZ.                                               SQ1144.2
007400*    05  RAW-DATA-KEY        PIC X(6).                            SQ1144.2
007500*    05  C-DATE              PIC 9(6).                            SQ1144.2
007600*    05  C-TIME              PIC 9(8).                            SQ1144.2
007700*    05  C-NO-OF-TESTS       PIC 99.                              SQ1144.2
007800*    05  C-OK                PIC 999.                             SQ1144.2
007900*    05  C-ALL               PIC 999.                             SQ1144.2
008000*    05  C-FAIL              PIC 999.                             SQ1144.2
008100*    05  C-DELETED           PIC 999.                             SQ1144.2
008200*    05  C-INSPECT           PIC 999.                             SQ1144.2
008300*    05  C-NOTE              PIC X(13).                           SQ1144.2
008400*    05  C-INDENT            PIC X.                               SQ1144.2
008500*    05  C-ABORT             PIC X(8).                            SQ1144.2
008600 FD  PRINT-FILE                                                   SQ1144.2
008700C    LABEL RECORDS                                                SQ1144.2
008800C    XXXXX084                                                     SQ1144.2
008900C    DATA RECORD IS PRINT-REC DUMMY-RECORD                        SQ1144.2
009000               .                                                  SQ1144.2
009100 01  PRINT-REC PICTURE X(120).                                    SQ1144.2
009200 01  DUMMY-RECORD PICTURE X(120).                                 SQ1144.2
009300 FD  SQ-FS1                                                       SQ1144.2
009400C    LABEL RECORD STANDARD                                        SQ1144.2
009500               .                                                  SQ1144.2
009600 01  SQ-FS1R1-F-G-120.                                            SQ1144.2
009700     02  FILLER PIC X(120).                                       SQ1144.2
009800 FD  SQ-FS2                                                       SQ1144.2
009900C    LABEL RECORDS ARE STANDARD                                   SQ1144.2
010000C    DATA RECORD SQ-FS2R1-F-G-120                                 SQ1144.2
010100     BLOCK CONTAINS 120 CHARACTERS                                SQ1144.2
010200     RECORD CONTAINS 120 CHARACTERS.                              SQ1144.2
010300 01  SQ-FS2R1-F-G-120.                                            SQ1144.2
010400     02  FILLER PIC X(120).                                       SQ1144.2
010500 FD  SQ-FS3                                                       SQ1144.2
010600C    LABEL RECORDS ARE STANDARD                                   SQ1144.2
010700C    DATA RECORD SQ-FS3R1-F-G-120                                 SQ1144.2
010800     BLOCK CONTAINS 120 CHARACTERS                                SQ1144.2
010900     RECORD CONTAINS 120 CHARACTERS.                              SQ1144.2
011000 01  SQ-FS3R1-F-G-120.                                            SQ1144.2
011100     02  FILLER PIC X(120).                                       SQ1144.2
011200 WORKING-STORAGE SECTION.                                         SQ1144.2
011300 01  FILE-STATUS-SQ-FS1    PIC XX VALUE SPACE.                    SQ1144.2
011400 01  FILE-STATUS-SQ-FS3    PIC XX VALUE SPACE.                    SQ1144.2
011500 01  FILE-STATUS-SQ-FS2    PIC XX VALUE SPACE.                    SQ1144.2
011600 01  WRK-CS-09V00 PIC S9(9) USAGE COMP VALUE ZERO.                SQ1144.2
011700 01  RECORDS-IN-ERROR PIC S9(5) USAGE COMP VALUE ZERO.            SQ1144.2
011800 01  ERROR-FLAG PIC 9 VALUE ZERO.                                 SQ1144.2
011900 01  EOF-FLAG   PICTURE 9 VALUE ZERO.                             SQ1144.2
012000 01  FILE-RECORD-INFORMATION-REC.                                 SQ1144.2
012100     03 FILE-RECORD-INFO-SKELETON.                                SQ1144.2
012200        05 FILLER                 PICTURE X(48)       VALUE       SQ1144.2
012300             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  SQ1144.2
012400        05 FILLER                 PICTURE X(46)       VALUE       SQ1144.2
012500             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    SQ1144.2
012600        05 FILLER                 PICTURE X(26)       VALUE       SQ1144.2
012700             ",LFIL=000000,ORG=  ,LBLR= ".                        SQ1144.2
012800        05 FILLER                 PICTURE X(37)       VALUE       SQ1144.2
012900             ",RECKEY=                             ".             SQ1144.2
013000        05 FILLER                 PICTURE X(38)       VALUE       SQ1144.2
013100             ",ALTKEY1=                             ".            SQ1144.2
013200        05 FILLER                 PICTURE X(38)       VALUE       SQ1144.2
013300             ",ALTKEY2=                             ".            SQ1144.2
013400        05 FILLER                 PICTURE X(7)        VALUE SPACE.SQ1144.2
013500     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              SQ1144.2
013600        05 FILE-RECORD-INFO-P1-120.                               SQ1144.2
013700           07 FILLER              PIC X(5).                       SQ1144.2
013800           07 XFILE-NAME           PIC X(6).                      SQ1144.2
013900           07 FILLER              PIC X(8).                       SQ1144.2
014000           07 XRECORD-NAME         PIC X(6).                      SQ1144.2
014100           07 FILLER              PIC X(1).                       SQ1144.2
014200           07 REELUNIT-NUMBER     PIC 9(1).                       SQ1144.2
014300           07 FILLER              PIC X(7).                       SQ1144.2
014400           07 XRECORD-NUMBER       PIC 9(6).                      SQ1144.2
014500           07 FILLER              PIC X(6).                       SQ1144.2
014600           07 UPDATE-NUMBER       PIC 9(2).                       SQ1144.2
014700           07 FILLER              PIC X(5).                       SQ1144.2
014800           07 ODO-NUMBER          PIC 9(4).                       SQ1144.2
014900           07 FILLER              PIC X(5).                       SQ1144.2
015000           07 XPROGRAM-NAME        PIC X(5).                      SQ1144.2
015100           07 FILLER              PIC X(7).                       SQ1144.2
015200           07 XRECORD-LENGTH       PIC 9(6).                      SQ1144.2
015300           07 FILLER              PIC X(7).                       SQ1144.2
015400           07 CHARS-OR-RECORDS    PIC X(2).                       SQ1144.2
015500           07 FILLER              PIC X(1).                       SQ1144.2
015600           07 XBLOCK-SIZE          PIC 9(4).                      SQ1144.2
015700           07 FILLER              PIC X(6).                       SQ1144.2
015800           07 RECORDS-IN-FILE     PIC 9(6).                       SQ1144.2
015900           07 FILLER              PIC X(5).                       SQ1144.2
016000           07 XFILE-ORGANIZATION   PIC X(2).                      SQ1144.2
016100           07 FILLER              PIC X(6).                       SQ1144.2
016200           07 XLABEL-TYPE          PIC X(1).                      SQ1144.2
016300        05 FILE-RECORD-INFO-P121-240.                             SQ1144.2
016400           07 FILLER              PIC X(8).                       SQ1144.2
016500           07 XRECORD-KEY          PIC X(29).                     SQ1144.2
016600           07 FILLER              PIC X(9).                       SQ1144.2
016700           07 ALTERNATE-KEY1      PIC X(29).                      SQ1144.2
016800           07 FILLER              PIC X(9).                       SQ1144.2
016900           07 ALTERNATE-KEY2      PIC X(29).                      SQ1144.2
017000           07 FILLER              PIC X(7).                       SQ1144.2
017100 01  TEST-RESULTS.                                                SQ1144.2
017200     02 FILLER                    PICTURE X VALUE SPACE.          SQ1144.2
017300     02 FEATURE                   PICTURE X(20) VALUE SPACE.      SQ1144.2
017400     02 FILLER                    PICTURE X VALUE SPACE.          SQ1144.2
017500     02 P-OR-F                    PICTURE X(5) VALUE SPACE.       SQ1144.2
017600     02 FILLER                    PICTURE X  VALUE SPACE.         SQ1144.2
017700     02  PAR-NAME.                                                SQ1144.2
017800       03 FILLER PICTURE X(12) VALUE SPACE.                       SQ1144.2
017900       03  PARDOT-X PICTURE X  VALUE SPACE.                       SQ1144.2
018000       03 DOTVALUE PICTURE 99  VALUE ZERO.                        SQ1144.2
018100       03 FILLER PIC X(5) VALUE SPACE.                            SQ1144.2
018200     02 FILLER PIC X(10) VALUE SPACE.                             SQ1144.2
018300     02 RE-MARK PIC X(61).                                        SQ1144.2
018400 01  TEST-COMPUTED.                                               SQ1144.2
018500     02 FILLER PIC X(30) VALUE SPACE.                             SQ1144.2
018600     02 FILLER PIC X(17) VALUE "       COMPUTED=".                SQ1144.2
018700     02 COMPUTED-X.                                               SQ1144.2
018800     03 COMPUTED-A                PICTURE X(20) VALUE SPACE.      SQ1144.2
018900     03 COMPUTED-N REDEFINES COMPUTED-A PICTURE -9(9).9(9).       SQ1144.2
019000     03 COMPUTED-0V18 REDEFINES COMPUTED-A  PICTURE -.9(18).      SQ1144.2
019100     03 COMPUTED-4V14 REDEFINES COMPUTED-A  PICTURE -9(4).9(14).  SQ1144.2
019200     03 COMPUTED-14V4 REDEFINES COMPUTED-A  PICTURE -9(14).9(4).  SQ1144.2
019300     03       CM-18V0 REDEFINES COMPUTED-A.                       SQ1144.2
019400         04 COMPUTED-18V0                   PICTURE -9(18).       SQ1144.2
019500         04 FILLER                          PICTURE X.            SQ1144.2
019600     03 FILLER PIC X(50) VALUE SPACE.                             SQ1144.2
019700 01  TEST-CORRECT.                                                SQ1144.2
019800     02 FILLER PIC X(30) VALUE SPACE.                             SQ1144.2
019900     02 FILLER PIC X(17) VALUE "       CORRECT =".                SQ1144.2
020000     02 CORRECT-X.                                                SQ1144.2
020100     03 CORRECT-A                 PICTURE X(20) VALUE SPACE.      SQ1144.2
020200     03 CORRECT-N REDEFINES CORRECT-A PICTURE -9(9).9(9).         SQ1144.2
020300     03 CORRECT-0V18 REDEFINES CORRECT-A    PICTURE -.9(18).      SQ1144.2
020400     03 CORRECT-4V14 REDEFINES CORRECT-A    PICTURE -9(4).9(14).  SQ1144.2
020500     03 CORRECT-14V4 REDEFINES CORRECT-A    PICTURE -9(14).9(4).  SQ1144.2
020600     03      CR-18V0 REDEFINES CORRECT-A.                         SQ1144.2
020700         04 CORRECT-18V0                    PICTURE -9(18).       SQ1144.2
020800         04 FILLER                          PICTURE X.            SQ1144.2
020900     03 FILLER PIC X(50) VALUE SPACE.                             SQ1144.2
021000 01  CCVS-C-1.                                                    SQ1144.2
021100     02 FILLER PICTURE IS X(99) VALUE IS " FEATURE              PASQ1144.2
021200-    "SS  PARAGRAPH-NAME                                          SQ1144.2
021300-    "        REMARKS".                                           SQ1144.2
021400     02 FILLER PICTURE IS X(20) VALUE IS SPACE.                   SQ1144.2
021500 01  CCVS-C-2.                                                    SQ1144.2
021600     02 FILLER PICTURE IS X VALUE IS SPACE.                       SQ1144.2
021700     02 FILLER PICTURE IS X(6) VALUE IS "TESTED".                 SQ1144.2
021800     02 FILLER PICTURE IS X(15) VALUE IS SPACE.                   SQ1144.2
021900     02 FILLER PICTURE IS X(4) VALUE IS "FAIL".                   SQ1144.2
022000     02 FILLER PICTURE IS X(94) VALUE IS SPACE.                   SQ1144.2
022100 01  REC-SKL-SUB PICTURE 9(2) VALUE ZERO.                         SQ1144.2
022200 01  REC-CT PICTURE 99 VALUE ZERO.                                SQ1144.2
022300 01  DELETE-CNT                   PICTURE 999  VALUE ZERO.        SQ1144.2
022400 01  ERROR-COUNTER PICTURE IS 999 VALUE IS ZERO.                  SQ1144.2
022500 01  INSPECT-COUNTER PIC 999 VALUE ZERO.                          SQ1144.2
022600 01  PASS-COUNTER PIC 999 VALUE ZERO.                             SQ1144.2
022700 01  TOTAL-ERROR PIC 999 VALUE ZERO.                              SQ1144.2
022800 01  ERROR-HOLD PIC 999 VALUE ZERO.                               SQ1144.2
022900 01  DUMMY-HOLD PIC X(120) VALUE SPACE.                           SQ1144.2
023000 01  RECORD-COUNT PIC 9(5) VALUE ZERO.                            SQ1144.2
023100 01  CCVS-H-1.                                                    SQ1144.2
023200     02  FILLER   PICTURE X(27)  VALUE SPACE.                     SQ1144.2
023300     02 FILLER PICTURE X(67) VALUE                                SQ1144.2
023400     " FEDERAL SOFTWARE TESTING CENTER COBOL COMPILER VALIDATION  SQ1144.2
023500-    " SYSTEM".                                                   SQ1144.2
023600     02  FILLER     PICTURE X(26)  VALUE SPACE.                   SQ1144.2
023700 01  CCVS-H-2.                                                    SQ1144.2
023800     02 FILLER PICTURE X(52) VALUE IS                             SQ1144.2
023900     "CCVS85 FSTC COPY, NOT FOR DISTRIBUTION.".                   SQ1144.2
024000     02 FILLER PICTURE IS X(19) VALUE IS "TEST RESULTS SET-  ".   SQ1144.2
024100     02 TEST-ID PICTURE IS X(9).                                  SQ1144.2
024200     02 FILLER PICTURE IS X(40) VALUE IS SPACE.                   SQ1144.2
024300 01  CCVS-H-3.                                                    SQ1144.2
024400     02  FILLER PICTURE X(34) VALUE                               SQ1144.2
024500     " FOR OFFICIAL USE ONLY    ".                                SQ1144.2
024600     02  FILLER PICTURE X(58) VALUE                               SQ1144.2
024700     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ1144.2
024800     02  FILLER PICTURE X(28) VALUE                               SQ1144.2
024900     "  COPYRIGHT   1985 ".                                       SQ1144.2
025000 01  CCVS-E-1.                                                    SQ1144.2
025100     02 FILLER PICTURE IS X(52) VALUE IS SPACE.                   SQ1144.2
025200     02 FILLER PICTURE IS X(14) VALUE IS "END OF TEST-  ".        SQ1144.2
025300     02 ID-AGAIN PICTURE IS X(9).                                 SQ1144.2
025400     02 FILLER PICTURE X(45) VALUE IS                             SQ1144.2
025500     " NTIS DISTRIBUTION COBOL 85".                               SQ1144.2
025600 01  CCVS-E-2.                                                    SQ1144.2
025700     02  FILLER                   PICTURE X(31)  VALUE            SQ1144.2
025800     SPACE.                                                       SQ1144.2
025900     02  FILLER                   PICTURE X(21)  VALUE SPACE.     SQ1144.2
026000     02 CCVS-E-2-2.                                               SQ1144.2
026100         03 ERROR-TOTAL PICTURE IS XXX VALUE IS SPACE.            SQ1144.2
026200         03 FILLER PICTURE IS X VALUE IS SPACE.                   SQ1144.2
026300         03 ENDER-DESC PIC X(46) VALUE "ERRORS ENCOUNTERED".      SQ1144.2
026400 01  CCVS-E-3.                                                    SQ1144.2
026500     02  FILLER PICTURE X(22) VALUE                               SQ1144.2
026600     " FOR OFFICIAL USE ONLY".                                    SQ1144.2
026700     02  FILLER PICTURE X(12) VALUE SPACE.                        SQ1144.2
026800     02  FILLER PICTURE X(58) VALUE                               SQ1144.2
026900     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1144.2
027000     02  FILLER PICTURE X(13) VALUE SPACE.                        SQ1144.2
027100     02 FILLER PIC X(15) VALUE " COPYRIGHT 1985".                 SQ1144.2
027200 01  CCVS-E-4.                                                    SQ1144.2
027300     02 CCVS-E-4-1 PIC XXX VALUE SPACE.                           SQ1144.2
027400     02 FILLER PIC XXXX VALUE " OF ".                             SQ1144.2
027500     02 CCVS-E-4-2 PIC XXX VALUE SPACE.                           SQ1144.2
027600     02 FILLER PIC X(40) VALUE                                    SQ1144.2
027700      "  TESTS WERE EXECUTED SUCCESSFULLY".                       SQ1144.2
027800 01  XXINFO.                                                      SQ1144.2
027900     02 FILLER PIC X(30) VALUE "        *** INFORMATION  ***".    SQ1144.2
028000     02 INFO-TEXT.                                                SQ1144.2
028100     04 FILLER PIC X(20) VALUE SPACE.                             SQ1144.2
028200     04 XXCOMPUTED PIC X(20).                                     SQ1144.2
028300     04 FILLER PIC X(5) VALUE SPACE.                              SQ1144.2
028400     04 XXCORRECT PIC X(20).                                      SQ1144.2
028500 01  HYPHEN-LINE.                                                 SQ1144.2
028600     02 FILLER PICTURE IS X VALUE IS SPACE.                       SQ1144.2
028700     02 FILLER PICTURE IS X(65) VALUE IS "************************SQ1144.2
028800-    "*****************************************".                 SQ1144.2
028900     02 FILLER PICTURE IS X(54) VALUE IS "************************SQ1144.2
029000-    "******************************".                            SQ1144.2
029100 01  CCVS-PGM-ID PIC X(6) VALUE                                   SQ1144.2
029200     "SQ114A".                                                    SQ1144.2
029300 PROCEDURE DIVISION.                                              SQ1144.2
029400 CCVS1 SECTION.                                                   SQ1144.2
029500 OPEN-FILES.                                                      SQ1144.2
029600*    OPEN I-O RAW-DATA.                                           SQ1144.2
029700*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            SQ1144.2
029800*    READ RAW-DATA INVALID KEY GO TO END-E-1.                     SQ1144.2
029900*    MOVE "ABORTED " TO C-ABORT.                                  SQ1144.2
030000*    ADD 1 TO C-NO-OF-TESTS.                                      SQ1144.2
030100*    ACCEPT C-DATE  FROM DATE.                                    SQ1144.2
030200*    ACCEPT C-TIME  FROM TIME.                                    SQ1144.2
030300*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-1.             SQ1144.2
030400*END-E-1.                                                         SQ1144.2
030500*    CLOSE RAW-DATA.                                              SQ1144.2
030600     OPEN     OUTPUT PRINT-FILE.                                  SQ1144.2
030700     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   SQ1144.2
030800     MOVE    SPACE TO TEST-RESULTS.                               SQ1144.2
030900     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             SQ1144.2
031000     MOVE ZERO TO REC-SKL-SUB.                                    SQ1144.2
031100     PERFORM CCVS-INIT-FILE 9 TIMES.                              SQ1144.2
031200 CCVS-INIT-FILE.                                                  SQ1144.2
031300     ADD 1 TO REC-SKL-SUB.                                        SQ1144.2
031400     MOVE FILE-RECORD-INFO-SKELETON TO                            SQ1144.2
031500                  FILE-RECORD-INFO (REC-SKL-SUB).                 SQ1144.2
031600 CCVS-INIT-EXIT.                                                  SQ1144.2
031700     GO TO CCVS1-EXIT.                                            SQ1144.2
031800 CLOSE-FILES.                                                     SQ1144.2
031900     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   SQ1144.2
032000*    OPEN I-O RAW-DATA.                                           SQ1144.2
032100*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            SQ1144.2
032200*    READ RAW-DATA INVALID KEY GO TO END-E-2.                     SQ1144.2
032300*    MOVE "OK.     " TO C-ABORT.                                  SQ1144.2
032400*    MOVE PASS-COUNTER TO C-OK.                                   SQ1144.2
032500*    MOVE ERROR-HOLD   TO C-ALL.                                  SQ1144.2
032600*    MOVE ERROR-COUNTER TO C-FAIL.                                SQ1144.2
032700*    MOVE DELETE-CNT TO C-DELETED.                                SQ1144.2
032800*    MOVE INSPECT-COUNTER TO C-INSPECT.                           SQ1144.2
032900*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-2.             SQ1144.2
033000*END-E-2.                                                         SQ1144.2
033100*    CLOSE RAW-DATA.                                              SQ1144.2
033200 TERMINATE-CCVS.                                                  SQ1144.2
033300*    EXIT PROGRAM.                                                SQ1144.2
033400*TERMINATE-CALL.                                                  SQ1144.2
033500     STOP     RUN.                                                SQ1144.2
033600 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         SQ1144.2
033700 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           SQ1144.2
033800 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          SQ1144.2
033900 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-CNT.          SQ1144.2
034000     MOVE "****TEST DELETED****" TO RE-MARK.                      SQ1144.2
034100 PRINT-DETAIL.                                                    SQ1144.2
034200     IF REC-CT NOT EQUAL TO ZERO                                  SQ1144.2
034300             MOVE "." TO PARDOT-X                                 SQ1144.2
034400             MOVE REC-CT TO DOTVALUE.                             SQ1144.2
034500     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      SQ1144.2
034600     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               SQ1144.2
034700        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 SQ1144.2
034800          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 SQ1144.2
034900     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              SQ1144.2
035000     MOVE SPACE TO CORRECT-X.                                     SQ1144.2
035100     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         SQ1144.2
035200     MOVE     SPACE TO RE-MARK.                                   SQ1144.2
035300 HEAD-ROUTINE.                                                    SQ1144.2
035400     MOVE CCVS-H-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ1144.2
035500     MOVE CCVS-H-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.   SQ1144.2
035600     MOVE CCVS-H-3 TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.   SQ1144.2
035700 COLUMN-NAMES-ROUTINE.                                            SQ1144.2
035800     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ1144.2
035900     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ1144.2
036000     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ1144.2
036100 END-ROUTINE.                                                     SQ1144.2
036200     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.SQ1144.2
036300 END-RTN-EXIT.                                                    SQ1144.2
036400     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ1144.2
036500 END-ROUTINE-1.                                                   SQ1144.2
036600      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      SQ1144.2
036700      ERROR-HOLD. ADD DELETE-CNT TO ERROR-HOLD.                   SQ1144.2
036800      ADD PASS-COUNTER TO ERROR-HOLD.                             SQ1144.2
036900*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   SQ1144.2
037000      MOVE PASS-COUNTER TO CCVS-E-4-1.                            SQ1144.2
037100      MOVE ERROR-HOLD TO CCVS-E-4-2.                              SQ1144.2
037200      MOVE CCVS-E-4 TO CCVS-E-2-2.                                SQ1144.2
037300      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           SQ1144.2
037400  END-ROUTINE-12.                                                 SQ1144.2
037500      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        SQ1144.2
037600     IF       ERROR-COUNTER IS EQUAL TO ZERO                      SQ1144.2
037700         MOVE "NO " TO ERROR-TOTAL                                SQ1144.2
037800         ELSE                                                     SQ1144.2
037900         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       SQ1144.2
038000     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           SQ1144.2
038100     PERFORM WRITE-LINE.                                          SQ1144.2
038200 END-ROUTINE-13.                                                  SQ1144.2
038300     IF DELETE-CNT IS EQUAL TO ZERO                               SQ1144.2
038400         MOVE "NO " TO ERROR-TOTAL  ELSE                          SQ1144.2
038500         MOVE DELETE-CNT TO ERROR-TOTAL.                          SQ1144.2
038600     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   SQ1144.2
038700     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ1144.2
038800      IF   INSPECT-COUNTER EQUAL TO ZERO                          SQ1144.2
038900          MOVE "NO " TO ERROR-TOTAL                               SQ1144.2
039000      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   SQ1144.2
039100      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            SQ1144.2
039200      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          SQ1144.2
039300     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ1144.2
039400 WRITE-LINE.                                                      SQ1144.2
039500     ADD 1 TO RECORD-COUNT.                                       SQ1144.2
039600*    IF RECORD-COUNT GREATER 50                                   SQ1144.2
039700*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          SQ1144.2
039800*        MOVE SPACE TO DUMMY-RECORD                               SQ1144.2
039900*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  SQ1144.2
040000*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             SQ1144.2
040100*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     SQ1144.2
040200*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          SQ1144.2
040300*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          SQ1144.2
040400*        MOVE ZERO TO RECORD-COUNT.                               SQ1144.2
040500     PERFORM WRT-LN.                                              SQ1144.2
040600 WRT-LN.                                                          SQ1144.2
040700     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               SQ1144.2
040800     MOVE SPACE TO DUMMY-RECORD.                                  SQ1144.2
040900 BLANK-LINE-PRINT.                                                SQ1144.2
041000     PERFORM WRT-LN.                                              SQ1144.2
041100 FAIL-ROUTINE.                                                    SQ1144.2
041200     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.   SQ1144.2
041300     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.    SQ1144.2
041400     MOVE "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.    SQ1144.2
041500     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     SQ1144.2
041600     GO TO FAIL-ROUTINE-EX.                                       SQ1144.2
041700 FAIL-ROUTINE-WRITE.                                              SQ1144.2
041800     MOVE TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE           SQ1144.2
041900     MOVE TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES.   SQ1144.2
042000 FAIL-ROUTINE-EX. EXIT.                                           SQ1144.2
042100 BAIL-OUT.                                                        SQ1144.2
042200     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.       SQ1144.2
042300     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.               SQ1144.2
042400 BAIL-OUT-WRITE.                                                  SQ1144.2
042500     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  SQ1144.2
042600     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     SQ1144.2
042700 BAIL-OUT-EX. EXIT.                                               SQ1144.2
042800 CCVS1-EXIT.                                                      SQ1144.2
042900     EXIT.                                                        SQ1144.2
043000 SECT-SQ102-0001 SECTION.                                         SQ1144.2
043100 SEQ-INIT-001.                                                    SQ1144.2
043200     MOVE "SQ-FS1" TO XFILE-NAME (1).                             SQ1144.2
043300     MOVE "R1-F-G" TO XRECORD-NAME (1).                           SQ1144.2
043400     MOVE CCVS-PGM-ID TO XPROGRAM-NAME (1).                       SQ1144.2
043500     MOVE 000120 TO XRECORD-LENGTH (1).                           SQ1144.2
043600     MOVE "RC"   TO CHARS-OR-RECORDS (1).                         SQ1144.2
043700     MOVE 0001   TO XBLOCK-SIZE (1).                              SQ1144.2
043800     MOVE 000750 TO RECORDS-IN-FILE (1).                          SQ1144.2
043900     MOVE "SQ"   TO XFILE-ORGANIZATION (1).                       SQ1144.2
044000     MOVE "S" TO XLABEL-TYPE (1).                                 SQ1144.2
044100     MOVE 000001 TO XRECORD-NUMBER (1).                           SQ1144.2
044200 OPEN-TEST-GF-01.                                                 SQ1144.2
044300**************************************************************    SQ1144.2
044400*  OPEN OUTPUT FILE-1, FILE-2.   WILL BE TESTED IN THIS TEST.*    SQ1144.2
044500*  VII; 4.3.2 (PAGE VII-39)                                  *    SQ1144.2
044600**************************************************************    SQ1144.2
044700                                                                  SQ1144.2
044800     MOVE SPACE TO FILE-STATUS-SQ-FS1.                            SQ1144.2
044900     MOVE SPACE TO FILE-STATUS-SQ-FS2.                            SQ1144.2
045000     OPEN OUTPUT SQ-FS1 SQ-FS2.                                   SQ1144.2
045100     IF FILE-STATUS-SQ-FS1 NOT = "00"                             SQ1144.2
045200         OR FILE-STATUS-SQ-FS2 NOT = "00"                         SQ1144.2
045300             GO TO  OPEN-FAIL-GF-01.                              SQ1144.2
045400 OPEN-PASS-GF-01.                                                 SQ1144.2
045500     PERFORM PASS                                                 SQ1144.2
045600     GO TO OPEN-WRITE-GF-01.                                      SQ1144.2
045700 OPEN-FAIL-GF-01.                                                 SQ1144.2
045800     IF FILE-STATUS-SQ-FS1 NOT = "00"                             SQ1144.2
045900         MOVE "STATUS 1: 00" TO CORRECT-A                         SQ1144.2
046000         MOVE FILE-STATUS-SQ-FS1 TO COMPUTED-A                    SQ1144.2
046100         MOVE "VII-39; 4.3.2        " TO  RE-MARK                 SQ1144.2
046200         PERFORM FAIL                                             SQ1144.2
046300         PERFORM OPEN-WRITE-GF-01.                                SQ1144.2
046400     IF FILE-STATUS-SQ-FS2 NOT = "00"                             SQ1144.2
046500         MOVE "STATUS 2: 00" TO CORRECT-A                         SQ1144.2
046600         MOVE FILE-STATUS-SQ-FS2 TO COMPUTED-A                    SQ1144.2
046700         MOVE "VII-39; 4.3.2        " TO  RE-MARK                 SQ1144.2
046800         PERFORM FAIL.                                            SQ1144.2
046900 OPEN-WRITE-GF-01.                                                SQ1144.2
047000     MOVE "OPEN-TEST-GF-01" TO PAR-NAME.                          SQ1144.2
047100     MOVE "OPEN OUTPUT FIL1 FIL2" TO FEATURE.                     SQ1144.2
047200     PERFORM PRINT-DETAIL.                                        SQ1144.2
047300 WRITE-TEST-GF-01.                                                SQ1144.2
047400     MOVE FILE-RECORD-INFO-P1-120 (1)  TO SQ-FS1R1-F-G-120.       SQ1144.2
047500     WRITE SQ-FS1R1-F-G-120.                                      SQ1144.2
047600     IF XRECORD-NUMBER (1) EQUAL TO 750                           SQ1144.2
047700         GO TO WRITE-WRITE-GF-01.                                 SQ1144.2
047800     ADD 1 TO XRECORD-NUMBER (1).                                 SQ1144.2
047900     GO TO WRITE-TEST-GF-01.                                      SQ1144.2
048000 WRITE-WRITE-GF-01.                                               SQ1144.2
048100     MOVE "WRITE  FILE SQ-FS1" TO FEATURE.                        SQ1144.2
048200     MOVE "WRITE-TEST-GF-01" TO PAR-NAME.                         SQ1144.2
048300     MOVE "FILE CREATED, RECS =" TO COMPUTED-A.                   SQ1144.2
048400     MOVE  XRECORD-NUMBER (1) TO CORRECT-18V0.                    SQ1144.2
048500     PERFORM PRINT-DETAIL.                                        SQ1144.2
048600     CLOSE SQ-FS1.                                                SQ1144.2
048700*        A SEQUENTIAL TAPE FILE WITH 120 CHARACTER RECORDS        SQ1144.2
048800*    HAS BEEN CREATED. THE FILE CONTAINS 750 RECORDS.             SQ1144.2
048900 SEQ-INIT-002.                                                    SQ1144.2
049000     MOVE ZERO TO WRK-CS-09V00.                                   SQ1144.2
049100*        THIS TEST READS AND CHECKS THE FILE CREATED IN           SQ1144.2
049200*    SEQ-TEST-001.                                                SQ1144.2
049300     OPEN INPUT SQ-FS1.                                           SQ1144.2
049400 SEQ-TEST-002.                                                    SQ1144.2
049500     READ SQ-FS1                                                  SQ1144.2
049600          AT END GO TO SEQ-TEST-002-1.                            SQ1144.2
049700     MOVE   SQ-FS1R1-F-G-120 TO FILE-RECORD-INFO-P1-120 (1).      SQ1144.2
049800     ADD 1 TO WRK-CS-09V00.                                       SQ1144.2
049900     IF WRK-CS-09V00 GREATER THAN 750                             SQ1144.2
050000        MOVE "MORE THAN 750 RECORDS" TO RE-MARK                   SQ1144.2
050100        GO TO SEQ-FAIL-002.                                       SQ1144.2
050200     IF WRK-CS-09V00 NOT EQUAL TO XRECORD-NUMBER (1)              SQ1144.2
050300         ADD 1 TO RECORDS-IN-ERROR                                SQ1144.2
050400         GO TO SEQ-TEST-002.                                      SQ1144.2
050500     IF XFILE-NAME (1) NOT EQUAL TO "SQ-FS1"                      SQ1144.2
050600        ADD 1 TO RECORDS-IN-ERROR                                 SQ1144.2
050700        GO TO SEQ-TEST-002.                                       SQ1144.2
050800     IF XLABEL-TYPE (1) NOT EQUAL TO "S"                          SQ1144.2
050900        ADD 1 TO RECORDS-IN-ERROR.                                SQ1144.2
051000     GO TO SEQ-TEST-002.                                          SQ1144.2
051100 SEQ-TEST-002-1.                                                  SQ1144.2
051200     IF RECORDS-IN-ERROR EQUAL TO ZERO                            SQ1144.2
051300         GO TO SEQ-PASS-002.                                      SQ1144.2
051400     MOVE "ERRORS IN READING SQ-FS1" TO RE-MARK.                  SQ1144.2
051500 SEQ-FAIL-002.                                                    SQ1144.2
051600     MOVE "RECORDS IN ERROR =" TO COMPUTED-A.                     SQ1144.2
051700     MOVE RECORDS-IN-ERROR TO CORRECT-18V0.                       SQ1144.2
051800     PERFORM FAIL.                                                SQ1144.2
051900     GO TO SEQ-WRITE-002.                                         SQ1144.2
052000 SEQ-PASS-002.                                                    SQ1144.2
052100     PERFORM PASS.                                                SQ1144.2
052200     MOVE "FILE VERIFIED RECS =" TO COMPUTED-A.                   SQ1144.2
052300     MOVE WRK-CS-09V00 TO CORRECT-18V0.                           SQ1144.2
052400 SEQ-WRITE-002.                                                   SQ1144.2
052500     MOVE "SEQ-TEST-002" TO PAR-NAME.                             SQ1144.2
052600     MOVE "VERIFY FILE SQ-FS1" TO FEATURE.                        SQ1144.2
052700     PERFORM PRINT-DETAIL.                                        SQ1144.2
052800 SEQ-CLOSE-002.                                                   SQ1144.2
052900     CLOSE SQ-FS1.                                                SQ1144.2
053000 READ-INIT-GF-01.                                                 SQ1144.2
053100     MOVE ZERO TO WRK-CS-09V00.                                   SQ1144.2
053200     MOVE ZERO TO RECORDS-IN-ERROR.                               SQ1144.2
053300     OPEN INPUT   SQ-FS1.                                         SQ1144.2
053400*            FOUR OPTIONS FOR THE READ STATEMENT ARE CHECKED      SQ1144.2
053500*    IN THIS SERIES OF TESTS.                                     SQ1144.2
053600     MOVE "READ...RECORD AT END ..." TO FEATURE.                  SQ1144.2
053700     MOVE "READ-TEST-GF-01" TO PAR-NAME.                          SQ1144.2
053800     MOVE ZERO TO ERROR-FLAG.                                     SQ1144.2
053900 READ-TEST-GF-01.                                                 SQ1144.2
054000     READ SQ-FS1 RECORD AT END                                    SQ1144.2
054100              MOVE "UNEXPECTED EOF" TO COMPUTED-A                 SQ1144.2
054200              MOVE 1 TO EOF-FLAG                                  SQ1144.2
054300              GO TO READ-FAIL-GF-01.                              SQ1144.2
054400     PERFORM RECORD-CHECK.                                        SQ1144.2
054500     IF WRK-CS-09V00 EQUAL TO 200                                 SQ1144.2
054600              GO TO READ-TEST-GF-01-1.                            SQ1144.2
054700             GO TO READ-TEST-GF-01.                               SQ1144.2
054800 RECORD-CHECK.                                                    SQ1144.2
054900     MOVE SQ-FS1R1-F-G-120 TO FILE-RECORD-INFO-P1-120 (1).        SQ1144.2
055000     ADD 1 TO WRK-CS-09V00.                                       SQ1144.2
055100     IF WRK-CS-09V00 NOT EQUAL TO XRECORD-NUMBER (1)              SQ1144.2
055200         ADD 1 TO RECORDS-IN-ERROR                                SQ1144.2
055300         MOVE 1 TO ERROR-FLAG.                                    SQ1144.2
055400 READ-TEST-GF-01-1.                                               SQ1144.2
055500     IF ERROR-FLAG EQUAL TO ZERO                                  SQ1144.2
055600         GO TO READ-PASS-GF-01.                                   SQ1144.2
055700     MOVE "ERROR IN RECORD(S)" TO COMPUTED-A.                     SQ1144.2
055800 READ-FAIL-GF-01.                                                 SQ1144.2
055900     MOVE "VII-15; 2.10.2; VII-44; 4.4.2            " TO  RE-MARK.SQ1144.2
056000     PERFORM FAIL.                                                SQ1144.2
056100     GO TO READ-WRITE-GF-01.                                      SQ1144.2
056200 READ-PASS-GF-01.                                                 SQ1144.2
056300     PERFORM PASS.                                                SQ1144.2
056400 READ-WRITE-GF-01.                                                SQ1144.2
056500     PERFORM PRINT-DETAIL.                                        SQ1144.2
056600 READ-INIT-GF-02.                                                 SQ1144.2
056700     IF EOF-FLAG EQUAL TO 1                                       SQ1144.2
056800        GO TO READ-EOF-GF-05.                                     SQ1144.2
056900     MOVE ZERO TO ERROR-FLAG.                                     SQ1144.2
057000     MOVE "READ...AT END..." TO FEATURE.                          SQ1144.2
057100     MOVE "READ-TEST-GF-02" TO PAR-NAME.                          SQ1144.2
057200 READ-TEST-GF-02.                                                 SQ1144.2
057300     READ SQ-FS1 AT END                                           SQ1144.2
057400         MOVE "UNEXPECTED EOF" TO COMPUTED-A                      SQ1144.2
057500         MOVE 1 TO EOF-FLAG                                       SQ1144.2
057600         GO TO READ-FAIL-GF-02.                                   SQ1144.2
057700     PERFORM RECORD-CHECK.                                        SQ1144.2
057800     IF WRK-CS-09V00 EQUAL TO 400                                 SQ1144.2
057900         GO TO READ-TEST-GF-02-1.                                 SQ1144.2
058000     GO TO READ-TEST-GF-02.                                       SQ1144.2
058100 READ-TEST-GF-02-1.                                               SQ1144.2
058200     IF ERROR-FLAG EQUAL TO ZERO                                  SQ1144.2
058300         GO TO READ-PASS-GF-02.                                   SQ1144.2
058400     MOVE "ERROR IN RECORD(S)" TO COMPUTED-A.                     SQ1144.2
058500 READ-FAIL-GF-02.                                                 SQ1144.2
058600     MOVE "VII-15; 2.10.2; VII-44; 4.4.2            " TO  RE-MARK.SQ1144.2
058700     PERFORM FAIL.                                                SQ1144.2
058800     GO TO READ-WRITE-GF-02.                                      SQ1144.2
058900 READ-PASS-GF-02.                                                 SQ1144.2
059000     PERFORM PASS.                                                SQ1144.2
059100 READ-WRITE-GF-02.                                                SQ1144.2
059200     PERFORM PRINT-DETAIL.                                        SQ1144.2
059300 READ-INIT-GF-03.                                                 SQ1144.2
059400     IF EOF-FLAG EQUAL TO 1                                       SQ1144.2
059500        GO TO READ-EOF-GF-05.                                     SQ1144.2
059600     MOVE ZERO TO ERROR-FLAG.                                     SQ1144.2
059700     MOVE "READ...RECORD END..." TO RE-MARK.                      SQ1144.2
059800     MOVE "READ-TEST-GF-03" TO PAR-NAME.                          SQ1144.2
059900 READ-TEST-GF-03.                                                 SQ1144.2
060000     READ SQ-FS1 RECORD END                                       SQ1144.2
060100          MOVE "UNEXPECTED EOF" TO COMPUTED-A                     SQ1144.2
060200          MOVE 1 TO EOF-FLAG                                      SQ1144.2
060300          GO TO READ-FAIL-GF-03.                                  SQ1144.2
060400     PERFORM RECORD-CHECK.                                        SQ1144.2
060500     IF WRK-CS-09V00 EQUAL TO 600                                 SQ1144.2
060600         GO TO READ-TEST-GF-03-1.                                 SQ1144.2
060700     GO TO READ-TEST-GF-03.                                       SQ1144.2
060800 READ-TEST-GF-03-1.                                               SQ1144.2
060900     IF ERROR-FLAG EQUAL TO ZERO                                  SQ1144.2
061000         GO TO READ-PASS-GF-03.                                   SQ1144.2
061100     MOVE "ERROR IN RECORD(S)" TO COMPUTED-A.                     SQ1144.2
061200 READ-FAIL-GF-03.                                                 SQ1144.2
061300     MOVE "VII-15; 2.10.2; VII-44; 4.4.2            " TO  RE-MARK.SQ1144.2
061400     PERFORM FAIL.                                                SQ1144.2
061500     GO TO READ-WRITE-GF-03.                                      SQ1144.2
061600 READ-PASS-GF-03.                                                 SQ1144.2
061700     PERFORM PASS.                                                SQ1144.2
061800 READ-WRITE-GF-03.                                                SQ1144.2
061900     PERFORM PRINT-DETAIL.                                        SQ1144.2
062000 READ-INIT-GF-04.                                                 SQ1144.2
062100     IF EOF-FLAG EQUAL TO 1                                       SQ1144.2
062200        GO TO READ-EOF-GF-05.                                     SQ1144.2
062300     MOVE ZERO TO ERROR-FLAG.                                     SQ1144.2
062400     MOVE "READ...END..." TO FEATURE.                             SQ1144.2
062500     MOVE  "READ-TEST-GF-04" TO PAR-NAME.                         SQ1144.2
062600 READ-TEST-GF-04.                                                 SQ1144.2
062700     READ SQ-FS1 END GO TO READ-TEST-GF-04-1.                     SQ1144.2
062800     PERFORM RECORD-CHECK.                                        SQ1144.2
062900     IF WRK-CS-09V00 GREATER THAN 750                             SQ1144.2
063000          GO TO READ-TEST-GF-04-1.                                SQ1144.2
063100     GO TO READ-TEST-GF-04.                                       SQ1144.2
063200 READ-TEST-GF-04-1.                                               SQ1144.2
063300     IF ERROR-FLAG EQUAL TO ZERO                                  SQ1144.2
063400          GO TO READ-PASS-GF-04.                                  SQ1144.2
063500 READ-FAIL-GF-04.                                                 SQ1144.2
063600     MOVE "ERROR IN RECORD(S)" TO COMPUTED-A.                     SQ1144.2
063700     MOVE "VII-15; 2.10.2; VII-44; 4.4.2            " TO  RE-MARK.SQ1144.2
063800     PERFORM FAIL.                                                SQ1144.2
063900     GO TO READ-WRITE-GF-04.                                      SQ1144.2
064000 READ-PASS-GF-04.                                                 SQ1144.2
064100     PERFORM PASS.                                                SQ1144.2
064200 READ-WRITE-GF-04.                                                SQ1144.2
064300     PERFORM PRINT-DETAIL.                                        SQ1144.2
064400 READ-TEST-GF-05.                                                 SQ1144.2
064500     IF RECORDS-IN-ERROR NOT EQUAL TO ZERO                        SQ1144.2
064600          MOVE "RECORDS IN ERROR =" TO COMPUTED-A                 SQ1144.2
064700          MOVE RECORDS-IN-ERROR TO CORRECT-18V0                   SQ1144.2
064800          GO TO READ-FAIL-GF-05.                                  SQ1144.2
064900     IF WRK-CS-09V00 GREATER THAN 750                             SQ1144.2
065000          MOVE "MORE THAN 750 RECORDS" TO RE-MARK                 SQ1144.2
065100          GO TO READ-FAIL-GF-05.                                  SQ1144.2
065200 READ-PASS-GF-05.                                                 SQ1144.2
065300     PERFORM PASS.                                                SQ1144.2
065400     GO TO READ-WRITE-GF-05.                                      SQ1144.2
065500 READ-EOF-GF-05.                                                  SQ1144.2
065600     MOVE "LESS THAN 750 RECORDS" TO RE-MARK.                     SQ1144.2
065700     MOVE "RECORDS READ =" TO COMPUTED-A.                         SQ1144.2
065800     MOVE WRK-CS-09V00 TO CORRECT-18V0.                           SQ1144.2
065900 READ-FAIL-GF-05.                                                 SQ1144.2
066000     MOVE "VII-15; 2.10.2; VII-44; 4.4.2            " TO  RE-MARK.SQ1144.2
066100     PERFORM FAIL.                                                SQ1144.2
066200 READ-WRITE-GF-05.                                                SQ1144.2
066300     MOVE "READ-TEST-GF-05" TO PAR-NAME.                          SQ1144.2
066400     MOVE "READ FILE SQ-FS1" TO FEATURE.                          SQ1144.2
066500     PERFORM PRINT-DETAIL.                                        SQ1144.2
066600 READ-CLOSE-GF-05.                                                SQ1144.2
066700     CLOSE SQ-FS1.                                                SQ1144.2
066800     CLOSE SQ-FS2.                                                SQ1144.2
066900 SECT-SQ104-0001 SECTION.                                         SQ1144.2
067000 OPEN-TEST-GF-02.                                                 SQ1144.2
067100     OPEN OUTPUT SQ-FS3.                                          SQ1144.2
067200     IF   FILE-STATUS-SQ-FS3 NOT = "00"                           SQ1144.2
067300          GO TO OPEN-FAIL-GF-03.                                  SQ1144.2
067400 OPEN-PASS-GF-01.                                                 SQ1144.2
067500     PERFORM PASS                                                 SQ1144.2
067600     GO TO OPEN-WRITE-GF-02.                                      SQ1144.2
067700 OPEN-FAIL-GF-02.                                                 SQ1144.2
067800     IF FILE-STATUS-SQ-FS3 NOT = "00"                             SQ1144.2
067900         MOVE "STATUS 3: 00" TO CORRECT-A                         SQ1144.2
068000         MOVE FILE-STATUS-SQ-FS3 TO COMPUTED-A                    SQ1144.2
068100         MOVE "VII-39; 4.3.2        " TO  RE-MARK                 SQ1144.2
068200         PERFORM FAIL.                                            SQ1144.2
068300 OPEN-WRITE-GF-02.                                                SQ1144.2
068400     MOVE "OPEN-TEST-GF-02" TO PAR-NAME.                          SQ1144.2
068500     MOVE "OPEN OUTPUT FIL3" TO FEATURE.                          SQ1144.2
068600     PERFORM PRINT-DETAIL.                                        SQ1144.2
068700 WRITE-INIT-GF-02.                                                SQ1144.2
068800     MOVE "SQ-FS3" TO XFILE-NAME (2).                             SQ1144.2
068900     MOVE "R1-F-G" TO XRECORD-NAME (2).                           SQ1144.2
069000     MOVE CCVS-PGM-ID TO XPROGRAM-NAME (2).                       SQ1144.2
069100     MOVE 120 TO XRECORD-LENGTH (2).                              SQ1144.2
069200     MOVE "CH" TO CHARS-OR-RECORDS (2).                           SQ1144.2
069300     MOVE 120 TO XBLOCK-SIZE (2).                                 SQ1144.2
069400     MOVE 000649 TO RECORDS-IN-FILE (2).                          SQ1144.2
069500     MOVE "SQ" TO XFILE-ORGANIZATION (2).                         SQ1144.2
069600     MOVE "S" TO XLABEL-TYPE (2).                                 SQ1144.2
069700     MOVE 000001 TO XRECORD-NUMBER (2).                           SQ1144.2
069800 WRITE-TEST-GF-02.                                                SQ1144.2
069900     MOVE FILE-RECORD-INFO-P1-120 (2) TO SQ-FS3R1-F-G-120.        SQ1144.2
070000     WRITE SQ-FS3R1-F-G-120.                                      SQ1144.2
070100     IF XRECORD-NUMBER (2) EQUAL TO 649                           SQ1144.2
070200        GO TO WRITE-WRITE-GF-02.                                  SQ1144.2
070300     ADD 1 TO XRECORD-NUMBER (2).                                 SQ1144.2
070400     GO TO WRITE-TEST-GF-02.                                      SQ1144.2
070500 WRITE-WRITE-GF-02.                                               SQ1144.2
070600     MOVE "WRITE SQ-FS3 649RE" TO FEATURE.                        SQ1144.2
070700     MOVE "WRITE-TEST-GF-02" TO PAR-NAME.                         SQ1144.2
070800     MOVE "FILE CREATED, RECS =" TO COMPUTED-A.                   SQ1144.2
070900     MOVE XRECORD-NUMBER (2) TO CORRECT-18V0.                     SQ1144.2
071000     PERFORM PRINT-DETAIL.                                        SQ1144.2
071100     CLOSE SQ-FS3.                                                SQ1144.2
071200*        A MASS STORAGE SEQUENTIAL FILE WITH 120 CHARACTER        SQ1144.2
071300*    RECORDS HAS BEEN CREATED.  THE FILE CONTAINS 649 RECORDS.    SQ1144.2
071400 READ-INIT-GF-06.                                                 SQ1144.2
071500     MOVE ZERO TO WRK-CS-09V00.                                   SQ1144.2
071600     MOVE    ZERO TO RECORDS-IN-ERROR.                            SQ1144.2
071700*        THIS TEST READS AND CHECKS THE FILE CREATED IN           SQ1144.2
071800*    READ-TEST-007.                                               SQ1144.2
071900     OPEN INPUT SQ-FS3.                                           SQ1144.2
072000 READ-TEST-GF-06.                                                 SQ1144.2
072100     READ SQ-FS3 RECORD                                           SQ1144.2
072200         AT END GO TO READ-TEST-GF-06-1.                          SQ1144.2
072300     MOVE SQ-FS3R1-F-G-120 TO FILE-RECORD-INFO-P1-120 (2).        SQ1144.2
072400     ADD 1 TO WRK-CS-09V00.                                       SQ1144.2
072500     IF WRK-CS-09V00 GREATER THAN 649                             SQ1144.2
072600         MOVE "MORE THAN 649 RECORDS" TO RE-MARK                  SQ1144.2
072700         GO TO READ-FAIL-GF-06.                                   SQ1144.2
072800     IF WRK-CS-09V00 NOT EQUAL TO XRECORD-NUMBER (2)              SQ1144.2
072900         ADD 1 TO RECORDS-IN-ERROR                                SQ1144.2
073000         GO TO READ-TEST-GF-06.                                   SQ1144.2
073100     IF XFILE-NAME (2) NOT EQUAL TO "SQ-FS3"                      SQ1144.2
073200         ADD 1 TO RECORDS-IN-ERROR                                SQ1144.2
073300         GO TO READ-TEST-GF-06.                                   SQ1144.2
073400     IF XLABEL-TYPE (2) NOT EQUAL TO "S"                          SQ1144.2
073500     ADD 1 TO RECORDS-IN-ERROR.                                   SQ1144.2
073600     GO TO READ-TEST-GF-06.                                       SQ1144.2
073700 READ-TEST-GF-06-1.                                               SQ1144.2
073800     IF RECORDS-IN-ERROR EQUAL TO ZERO                            SQ1144.2
073900          GO TO READ-PASS-GF-06.                                  SQ1144.2
074000     MOVE "ERRORS IN READING SQ-FS3" TO RE-MARK.                  SQ1144.2
074100 READ-FAIL-GF-06.                                                 SQ1144.2
074200     MOVE RECORDS-IN-ERROR TO CORRECT-18V0.                       SQ1144.2
074300     PERFORM FAIL.                                                SQ1144.2
074400     GO TO READ-WRITE-GF-06.                                      SQ1144.2
074500 READ-PASS-GF-06.                                                 SQ1144.2
074600     PERFORM PASS.                                                SQ1144.2
074700     MOVE "FILE VERIFIED RECS =" TO COMPUTED-A.                   SQ1144.2
074800     MOVE WRK-CS-09V00 TO CORRECT-18V0.                           SQ1144.2
074900 READ-WRITE-GF-06.                                                SQ1144.2
075000     MOVE "READ-TEST-GF-06" TO PAR-NAME.                          SQ1144.2
075100     MOVE "VERIFY FILE SQ-FS3" TO FEATURE.                        SQ1144.2
075200     PERFORM PRINT-DETAIL.                                        SQ1144.2
075300 READ-CLOSE-GF-06.                                                SQ1144.2
075400     CLOSE SQ-FS3.                                                SQ1144.2
075500 READ-INIT-GF-07.                                                 SQ1144.2
075600     MOVE ZERO TO WRK-CS-09V00.                                   SQ1144.2
075700     MOVE ZERO TO RECORDS-IN-ERROR.                               SQ1144.2
075800     OPEN INPUT SQ-FS3.                                           SQ1144.2
075900*        FOUR OPTIONS FOR THE READ STATEMENT ARE CHECKED          SQ1144.2
076000*    IN THIS SERIES OF TESTS.                                     SQ1144.2
076100     MOVE "LEV 1 READ STATEMENT" TO FEATURE.                      SQ1144.2
076200     MOVE     ZERO TO EOF-FLAG.                                   SQ1144.2
076300     MOVE "READ...RECORD AT END ..." TO FEATURE.                  SQ1144.2
076400     MOVE "READ-TEST-GF-07" TO PAR-NAME.                          SQ1144.2
076500     MOVE ZERO TO ERROR-FLAG.                                     SQ1144.2
076600 READ-TEST-GF-07.                                                 SQ1144.2
076700     READ SQ-FS3 RECORD                                           SQ1144.2
076800          AT END MOVE "UNEXPECTED EOF" TO COMPUTED-A              SQ1144.2
076900          MOVE 1 TO EOF-FLAG                                      SQ1144.2
077000          GO TO READ-FAIL-GF-07.                                  SQ1144.2
077100     PERFORM RECORD-CHECK-1.                                      SQ1144.2
077200     IF WRK-CS-09V00 EQUAL TO 50                                  SQ1144.2
077300           GO TO READ-TEST-GF-07-1.                               SQ1144.2
077400     GO TO READ-TEST-GF-07.                                       SQ1144.2
077500 RECORD-CHECK-1.                                                  SQ1144.2
077600     MOVE SQ-FS3R1-F-G-120 TO FILE-RECORD-INFO-P1-120 (2).        SQ1144.2
077700     ADD 1 TO WRK-CS-09V00.                                       SQ1144.2
077800     IF WRK-CS-09V00 NOT EQUAL TO XRECORD-NUMBER (2)              SQ1144.2
077900         ADD 1 TO RECORDS-IN-ERROR                                SQ1144.2
078000         MOVE 1 TO ERROR-FLAG.                                    SQ1144.2
078100 READ-TEST-GF-07-1.                                               SQ1144.2
078200     IF ERROR-FLAG EQUAL TO ZERO                                  SQ1144.2
078300         GO TO READ-PASS-GF-07.                                   SQ1144.2
078400     MOVE "ERROR IN RECORD(S)" TO COMPUTED-A.                     SQ1144.2
078500 READ-FAIL-GF-07.                                                 SQ1144.2
078600     MOVE "VII-15; 2.10.2; VII-44; 4.4.2            " TO  RE-MARK.SQ1144.2
078700     PERFORM FAIL.                                                SQ1144.2
078800     GO TO READ-WRITE-GF-07.                                      SQ1144.2
078900 READ-PASS-GF-07.                                                 SQ1144.2
079000     PERFORM PASS.                                                SQ1144.2
079100 READ-WRITE-GF-07.                                                SQ1144.2
079200     PERFORM PRINT-DETAIL.                                        SQ1144.2
079300 READ-INIT-GF-08.                                                 SQ1144.2
079400     IF EOF-FLAG EQUAL TO 1                                       SQ1144.2
079500         GO TO READ-EOF-GF-11.                                    SQ1144.2
079600     MOVE ZERO TO ERROR-FLAG.                                     SQ1144.2
079700     MOVE "READ...AT END..." TO FEATURE.                          SQ1144.2
079800     MOVE "READ-TEST-GF-08" TO PAR-NAME.                          SQ1144.2
079900 READ-TEST-GF-08.                                                 SQ1144.2
080000     READ SQ-FS3  AT END                                          SQ1144.2
080100          MOVE "UNEXPECTED EOF" TO COMPUTED-A                     SQ1144.2
080200          MOVE 1 TO EOF-FLAG                                      SQ1144.2
080300          GO TO READ-FAIL-GF-08.                                  SQ1144.2
080400     PERFORM RECORD-CHECK-1.                                      SQ1144.2
080500     IF WRK-CS-09V00 EQUAL TO 200                                 SQ1144.2
080600         GO TO READ-TEST-GF-08-1.                                 SQ1144.2
080700     GO TO READ-TEST-GF-08.                                       SQ1144.2
080800 READ-TEST-GF-08-1.                                               SQ1144.2
080900     IF ERROR-FLAG EQUAL TO ZERO                                  SQ1144.2
081000          GO TO READ-PASS-GF-08.                                  SQ1144.2
081100     MOVE "ERROR IN RECORD(S)" TO COMPUTED-A.                     SQ1144.2
081200 READ-FAIL-GF-08.                                                 SQ1144.2
081300     MOVE "VII-15; 2.10.2; VII-44; 4.4.2            " TO  RE-MARK.SQ1144.2
081400     PERFORM FAIL.                                                SQ1144.2
081500     GO TO READ-WRITE-GF-08.                                      SQ1144.2
081600 READ-PASS-GF-08.                                                 SQ1144.2
081700     PERFORM PASS.                                                SQ1144.2
081800 READ-WRITE-GF-08.                                                SQ1144.2
081900     PERFORM PRINT-DETAIL.                                        SQ1144.2
082000 READ-INIT-GF-09.                                                 SQ1144.2
082100     IF EOF-FLAG EQUAL TO 1                                       SQ1144.2
082200         GO TO READ-EOF-GF-11.                                    SQ1144.2
082300     MOVE ZERO TO ERROR-FLAG.                                     SQ1144.2
082400     MOVE "READ...RECORD END..." TO FEATURE.                      SQ1144.2
082500     MOVE "READ-TEST-GF-09" TO PAR-NAME.                          SQ1144.2
082600 READ-TEST-GF-09.                                                 SQ1144.2
082700     READ SQ-FS3 RECORD END                                       SQ1144.2
082800          MOVE "UNEXPECTED EOF" TO COMPUTED-A                     SQ1144.2
082900          MOVE 1 TO EOF-FLAG                                      SQ1144.2
083000          GO TO READ-FAIL-GF-09.                                  SQ1144.2
083100     PERFORM RECORD-CHECK-1.                                      SQ1144.2
083200     IF WRK-CS-09V00 EQUAL TO 499                                 SQ1144.2
083300          GO TO READ-TEST-GF-09-1.                                SQ1144.2
083400     GO TO READ-TEST-GF-09.                                       SQ1144.2
083500 READ-TEST-GF-09-1.                                               SQ1144.2
083600     IF ERROR-FLAG EQUAL TO ZERO                                  SQ1144.2
083700           GO TO READ-PASS-GF-09.                                 SQ1144.2
083800     MOVE "ERROR IN RECORD(S)" TO COMPUTED-A.                     SQ1144.2
083900 READ-FAIL-GF-09.                                                 SQ1144.2
084000     MOVE "VII-15; 2.10.2; VII-44; 4.4.2            " TO  RE-MARK.SQ1144.2
084100     PERFORM FAIL.                                                SQ1144.2
084200     GO TO READ-WRITE-GF-09.                                      SQ1144.2
084300 READ-PASS-GF-09.                                                 SQ1144.2
084400     PERFORM PASS.                                                SQ1144.2
084500 READ-WRITE-GF-09.                                                SQ1144.2
084600     PERFORM PRINT-DETAIL.                                        SQ1144.2
084700 READ-INIT-GF-10.                                                 SQ1144.2
084800     IF EOF-FLAG EQUAL TO 1                                       SQ1144.2
084900         GO TO READ-EOF-GF-11.                                    SQ1144.2
085000     MOVE ZERO TO ERROR-FLAG.                                     SQ1144.2
085100     MOVE "READ...END..." TO FEATURE.                             SQ1144.2
085200     MOVE "READ-TEST-GF-10" TO PAR-NAME.                          SQ1144.2
085300 READ-TEST-GF-10.                                                 SQ1144.2
085400     READ SQ-FS3 END                                              SQ1144.2
085500          GO TO READ-TEST-GF-10-1.                                SQ1144.2
085600     PERFORM RECORD-CHECK-1.                                      SQ1144.2
085700     IF WRK-CS-09V00 GREATER THAN 649                             SQ1144.2
085800          GO TO READ-TEST-GF-10-1.                                SQ1144.2
085900     GO TO READ-TEST-GF-10.                                       SQ1144.2
086000 READ-TEST-GF-10-1.                                               SQ1144.2
086100     IF ERROR-FLAG EQUAL TO ZERO                                  SQ1144.2
086200         GO TO READ-PASS-GF-10.                                   SQ1144.2
086300 READ-FAIL-GF-10.                                                 SQ1144.2
086400     MOVE "ERROR IN RECORD(S)" TO COMPUTED-A.                     SQ1144.2
086500     MOVE "VII-15; 2.10.2; VII-44; 4.4.2            " TO  RE-MARK.SQ1144.2
086600     PERFORM FAIL.                                                SQ1144.2
086700     GO TO READ-WRITE-GF-10.                                      SQ1144.2
086800 READ-PASS-GF-10.                                                 SQ1144.2
086900     PERFORM PASS.                                                SQ1144.2
087000 READ-WRITE-GF-10.                                                SQ1144.2
087100     PERFORM PRINT-DETAIL.                                        SQ1144.2
087200 READ-TEST-GF-11.                                                 SQ1144.2
087300     IF RECORDS-IN-ERROR NOT EQUAL TO ZERO                        SQ1144.2
087400          MOVE "RECORDS IN ERROR =" TO COMPUTED-A                 SQ1144.2
087500          MOVE RECORDS-IN-ERROR TO CORRECT-18V0                   SQ1144.2
087600          GO TO READ-FAIL-GF-11.                                  SQ1144.2
087700     IF WRK-CS-09V00 GREATER THAN 649                             SQ1144.2
087800          MOVE "MORE THAN 649 RECORDS" TO RE-MARK                 SQ1144.2
087900          GO TO READ-FAIL-GF-11.                                  SQ1144.2
088000 READ-PASS-GF-11.                                                 SQ1144.2
088100     PERFORM PASS                                                 SQ1144.2
088200     GO TO READ-WRITE-GF-11.                                      SQ1144.2
088300 READ-EOF-GF-11.                                                  SQ1144.2
088400     MOVE "LESS THAN 649 RECORDS" TO RE-MARK.                     SQ1144.2
088500     MOVE "RECORDS READ =" TO COMPUTED-A.                         SQ1144.2
088600     MOVE WRK-CS-09V00 TO CORRECT-18V0.                           SQ1144.2
088700 READ-FAIL-GF-11.                                                 SQ1144.2
088800     PERFORM FAIL.                                                SQ1144.2
088900 READ-WRITE-GF-11.                                                SQ1144.2
089000     MOVE "READ-TEST-GF-11" TO PAR-NAME.                          SQ1144.2
089100     MOVE "READ FILE SQ-FS3" TO FEATURE.                          SQ1144.2
089200     PERFORM PRINT-DETAIL.                                        SQ1144.2
089300 READ-CLOSE-GF-11.                                                SQ1144.2
089400     CLOSE SQ-FS3.                                                SQ1144.2
089500 OPEN-TEST-GF-03.                                                 SQ1144.2
089600**************************************************************    SQ1144.2
089700*  OPEN OUTPUT FILE-1, FILE-2.   WILL BE TESTED IN THIS TEST.*    SQ1144.2
089800*  VII; 4.3.2 (PAGE VII-39)                                  *    SQ1144.2
089900**************************************************************    SQ1144.2
090000                                                                  SQ1144.2
090100     MOVE SPACE TO FILE-STATUS-SQ-FS1.                            SQ1144.2
090200     MOVE SPACE TO FILE-STATUS-SQ-FS2.                            SQ1144.2
090300     OPEN INPUT SQ-FS1 SQ-FS2.                                    SQ1144.2
090400     IF FILE-STATUS-SQ-FS1 NOT = "00"                             SQ1144.2
090500         OR FILE-STATUS-SQ-FS2 NOT = "00"                         SQ1144.2
090600             GO TO  OPEN-FAIL-GF-02.                              SQ1144.2
090700 OPEN-PASS-GF-03.                                                 SQ1144.2
090800     PERFORM PASS                                                 SQ1144.2
090900     GO TO OPEN-WRITE-GF-03.                                      SQ1144.2
091000 OPEN-FAIL-GF-03.                                                 SQ1144.2
091100     IF FILE-STATUS-SQ-FS1 NOT = "00"                             SQ1144.2
091200         MOVE "STATUS 1: 00" TO CORRECT-A                         SQ1144.2
091300         MOVE FILE-STATUS-SQ-FS1 TO COMPUTED-A                    SQ1144.2
091400         MOVE "VII-15; 2.10.2; VII-44; 4.4.2   " TO  RE-MARK      SQ1144.2
091500         PERFORM FAIL                                             SQ1144.2
091600         PERFORM OPEN-WRITE-GF-03.                                SQ1144.2
091700     IF FILE-STATUS-SQ-FS2 NOT = "00"                             SQ1144.2
091800         MOVE "STATUS 2: 00" TO CORRECT-A                         SQ1144.2
091900         MOVE FILE-STATUS-SQ-FS2 TO COMPUTED-A                    SQ1144.2
092000         MOVE "VII-15; 2.10.2; VII-44; 4.4.2   " TO  RE-MARK      SQ1144.2
092100         PERFORM FAIL.                                            SQ1144.2
092200 OPEN-WRITE-GF-03.                                                SQ1144.2
092300     MOVE "OPEN-TEST-GF-03" TO PAR-NAME.                          SQ1144.2
092400     MOVE "OPEN INPUT FILE1, FILE2" TO FEATURE.                   SQ1144.2
092500     PERFORM PRINT-DETAIL.                                        SQ1144.2
092600                                                                  SQ1144.2
092700*CLOSE-TEST-GF-01.                                                SQ1144.2
092800******************************************************************SQ1144.2
092900*  CLOSE FILE-1, FILE-2 WITH LOCK.  WILL BE TESTED IN THIS TEST. *SQ1144.2
093000*  VII; 4.2 (PAGE VII-35)                                        *SQ1144.2
093100******************************************************************SQ1144.2
093200                                                                  SQ1144.2
093300*    MOVE SPACE TO FILE-STATUS-SQ-FS1.                            SQ1144.2
093400*    MOVE SPACE TO FILE-STATUS-SQ-FS2.                            SQ1144.2
093500*    CLOSE SQ-FS1 WITH LOCK, SQ-FS2 WITH LOCK.                    SQ1144.2
093600*    IF FILE-STATUS-SQ-FS1 NOT = "00"                             SQ1144.2
093700*        OR FILE-STATUS-SQ-FS2 NOT = "00"                         SQ1144.2
093800*            GO TO CLOSE-FAIL-GF-01.                              SQ1144.2
093900*CLOSE-PASS-GF-01.                                                SQ1144.2
094000*    PERFORM PASS                                                 SQ1144.2
094100*    GO TO CLOSE-WRITE-GF-01.                                     SQ1144.2
094200*CLOSE-FAIL-GF-01.                                                SQ1144.2
094300*    IF FILE-STATUS-SQ-FS1 NOT = "00"                             SQ1144.2
094400*        MOVE "STATUS 1: 00" TO CORRECT-A                         SQ1144.2
094500*        MOVE FILE-STATUS-SQ-FS1 TO COMPUTED-A                    SQ1144.2
094600*        MOVE "VII-35; 4.2.2      " TO  RE-MARK                   SQ1144.2
094700*        PERFORM FAIL                                             SQ1144.2
094800*        PERFORM CLOSE-WRITE-GF-01.                               SQ1144.2
094900*    IF FILE-STATUS-SQ-FS2 NOT = "00"                             SQ1144.2
095000*        MOVE "STATUS 2: 00" TO CORRECT-A                         SQ1144.2
095100*        MOVE FILE-STATUS-SQ-FS2 TO COMPUTED-A                    SQ1144.2
095200*        MOVE "VII-35; 4.2.2      " TO  RE-MARK                   SQ1144.2
095300*        PERFORM FAIL.                                            SQ1144.2
095400*CLOSE-WRITE-GF-01.                                               SQ1144.2
095500*    MOVE "CLOSE-TEST-GF-01" TO PAR-NAME.                         SQ1144.2
095600*    MOVE "CLOSE FILE1, FILE2" TO FEATURE.                        SQ1144.2
095700*    PERFORM PRINT-DETAIL.                                        SQ1144.2
095800 TERMINATE-ROUTINE.                                               SQ1144.2
095900     EXIT.                                                        SQ1144.2
096000 CCVS-EXIT SECTION.                                               SQ1144.2
096100 CCVS-999999.                                                     SQ1144.2
096200     GO TO CLOSE-FILES.                                           SQ1144.2
      *END-OF,SQ114A                                                                  
