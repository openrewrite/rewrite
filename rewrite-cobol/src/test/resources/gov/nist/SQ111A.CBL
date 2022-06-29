      *HEADER,COBOL,SQ111A                                                            
000100 IDENTIFICATION DIVISION.                                         SQ1114.2
000200 PROGRAM-ID.                                                      SQ1114.2
000300     SQ111A.                                                      SQ1114.2
000400****************************************************************  SQ1114.2
000500*                                                              *  SQ1114.2
000600*    VALIDATION FOR:-                                          *  SQ1114.2
000700*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1114.2
000800*                                                              *  SQ1114.2
000900*    CREATION DATE     /     VALIDATION DATE                   *  SQ1114.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ1114.2
001100*                                                              *  SQ1114.2
001200****************************************************************  SQ1114.2
001300                                                                  SQ1114.2
001400*        THIS ROUTINE CREATES A SEQUENTIAL TAPE FILE CONTAINING   SQ1114.2
001500*    595 RECORDS, EACH RECORD CONTAINING 155 CHARACTERS.  THE     SQ1114.2
001600*    CODE-SET CLAUSE IS INCLUDED IN THE FILE DESCRIPTION ENTRY    SQ1114.2
001700*    FOR THE FILE.  THE RECORD DESCRIPTION FOR THE FILE CONTAINS  SQ1114.2
001800*    AN ITEM WITH THE SIGN IS SEPARATE CHARACTER CLAUSE.          SQ1114.2
001900*        A SEQUENTIAL TAPE FILE WITH 595 RECORDS HAS BEEN         SQ1114.2
002000*    CREATED.  THE FD FOR THE FILE CONTAINS A CODE-SET CLAUSE.    SQ1114.2
002100*    THERE ARE 155 CHARACTERS PER RECORD INCLUDING A NUMERIC      SQ1114.2
002200*    ITEM WITH THE SIGN IS SEPARATE CLAUSE.                       SQ1114.2
002300*                                                                 SQ1114.2
002400*    USED X-CARDS:                                                SQ1114.2
002500*         XXXXX001                                                SQ1114.2
002600*         XXXXX055                                                SQ1114.2
002700*     P   XXXXX062                                                SQ1114.2
002800*         XXXXX082                                                SQ1114.2
002900*         XXXXX083                                                SQ1114.2
003000*     C   XXXXX084                                                SQ1114.2
003100*                                                                 SQ1114.2
003200*                                                                 SQ1114.2
003300 ENVIRONMENT DIVISION.                                            SQ1114.2
003400 CONFIGURATION SECTION.                                           SQ1114.2
003500 SOURCE-COMPUTER.                                                 SQ1114.2
003600     XXXXX082.                                                    SQ1114.2
003700 OBJECT-COMPUTER.                                                 SQ1114.2
003800     XXXXX083.                                                    SQ1114.2
003900 SPECIAL-NAMES.                                                   SQ1114.2
004000     ALPHABET TAPE-CHARACTER-SET IS STANDARD-1.                   SQ1114.2
004100 INPUT-OUTPUT SECTION.                                            SQ1114.2
004200 FILE-CONTROL.                                                    SQ1114.2
004300*    SELECT RAW-DATA   ASSIGN TO                                  SQ1114.2
004400*    XXXXX062                                                     SQ1114.2
004500*           ORGANIZATION IS INDEXED                               SQ1114.2
004600*           ACCESS MODE IS RANDOM                                 SQ1114.2
004700*           RECORD KEY IS RAW-DATA-KEY.                           SQ1114.2
004800     SELECT PRINT-FILE ASSIGN TO                                  SQ1114.2
004900     XXXXX055.                                                    SQ1114.2
005000     SELECT SQ-FS1 ASSIGN TO                                      SQ1114.2
005100     XXXXX001                                                     SQ1114.2
005200     ORGANIZATION IS SEQUENTIAL.                                  SQ1114.2
005300 DATA DIVISION.                                                   SQ1114.2
005400 FILE SECTION.                                                    SQ1114.2
005500*                                                                 SQ1114.2
005600*FD  RAW-DATA.                                                    SQ1114.2
005700*                                                                 SQ1114.2
005800*01  RAW-DATA-SATZ.                                               SQ1114.2
005900*    05  RAW-DATA-KEY        PIC X(6).                            SQ1114.2
006000*    05  C-DATE              PIC 9(6).                            SQ1114.2
006100*    05  C-TIME              PIC 9(8).                            SQ1114.2
006200*    05  C-NO-OF-TESTS       PIC 99.                              SQ1114.2
006300*    05  C-OK                PIC 999.                             SQ1114.2
006400*    05  C-ALL               PIC 999.                             SQ1114.2
006500*    05  C-FAIL              PIC 999.                             SQ1114.2
006600*    05  C-DELETED           PIC 999.                             SQ1114.2
006700*    05  C-INSPECT           PIC 999.                             SQ1114.2
006800*    05  C-NOTE              PIC X(13).                           SQ1114.2
006900*    05  C-INDENT            PIC X.                               SQ1114.2
007000*    05  C-ABORT             PIC X(8).                            SQ1114.2
007100 FD  PRINT-FILE                                                   SQ1114.2
007200C    LABEL RECORDS                                                SQ1114.2
007300C    XXXXX084                                                     SQ1114.2
007400C    DATA RECORD IS PRINT-REC DUMMY-RECORD                        SQ1114.2
007500               .                                                  SQ1114.2
007600 01  PRINT-REC PICTURE X(120).                                    SQ1114.2
007700 01  DUMMY-RECORD PICTURE X(120).                                 SQ1114.2
007800 FD  SQ-FS1                                                       SQ1114.2
007900C    LABEL RECORD STANDARD                                        SQ1114.2
008000     CODE-SET IS TAPE-CHARACTER-SET .                             SQ1114.2
008100 01  SQ-FS1R1-F-G-155.                                            SQ1114.2
008200     02  SQ-FS1-FIRST  PICTURE X(120).                            SQ1114.2
008300     02  SQ-FS1-RECNO  PIC S9(5)  SIGN IS LEADING                 SQ1114.2
008400         SEPARATE CHARACTER.                                      SQ1114.2
008500     02  SQ-FS1-FILLER PICTURE X(30).                             SQ1114.2
008600 WORKING-STORAGE SECTION.                                         SQ1114.2
008700 01  COUNT-OF-RECS   PIC S9(5) VALUE ZERO.                        SQ1114.2
008800 01  ERROR-FLAG   PIC 9 VALUE ZERO.                               SQ1114.2
008900 01  RECORDS-IN-ERROR  PIC S9(5) USAGE COMP VALUE ZERO.           SQ1114.2
009000 01  EOF-FLAG PIC 9 VALUE ZERO.                                   SQ1114.2
009100 01  COMPARE-ITEM.                                                SQ1114.2
009200     02  FILLER PICTURE X.                                        SQ1114.2
009300     02  COMPARE-REC-NO PICTURE 9(5).                             SQ1114.2
009400 01  TEMP-STORE-FOR-PRINT.                                        SQ1114.2
009500     02 TEMP-FIRST  PIC X(120).                                   SQ1114.2
009600     02 TEMP-SECOND.                                              SQ1114.2
009700        03  TEMP-RECNO PIC X(6).                                  SQ1114.2
009800        03  TEMP-FILLER PIC X(30).                                SQ1114.2
009900 01  FILE-RECORD-INFORMATION-REC.                                 SQ1114.2
010000     03 FILE-RECORD-INFO-SKELETON.                                SQ1114.2
010100        05 FILLER                 PICTURE X(48)       VALUE       SQ1114.2
010200             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  SQ1114.2
010300        05 FILLER                 PICTURE X(46)       VALUE       SQ1114.2
010400             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    SQ1114.2
010500        05 FILLER                 PICTURE X(26)       VALUE       SQ1114.2
010600             ",LFIL=000000,ORG=  ,LBLR= ".                        SQ1114.2
010700        05 FILLER                 PICTURE X(37)       VALUE       SQ1114.2
010800             ",RECKEY=                             ".             SQ1114.2
010900        05 FILLER                 PICTURE X(38)       VALUE       SQ1114.2
011000             ",ALTKEY1=                             ".            SQ1114.2
011100        05 FILLER                 PICTURE X(38)       VALUE       SQ1114.2
011200             ",ALTKEY2=                             ".            SQ1114.2
011300        05 FILLER                 PICTURE X(7)        VALUE SPACE.SQ1114.2
011400     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              SQ1114.2
011500        05 FILE-RECORD-INFO-P1-120.                               SQ1114.2
011600           07 FILLER              PIC X(5).                       SQ1114.2
011700           07 XFILE-NAME           PIC X(6).                      SQ1114.2
011800           07 FILLER              PIC X(8).                       SQ1114.2
011900           07 XRECORD-NAME         PIC X(6).                      SQ1114.2
012000           07 FILLER              PIC X(1).                       SQ1114.2
012100           07 REELUNIT-NUMBER     PIC 9(1).                       SQ1114.2
012200           07 FILLER              PIC X(7).                       SQ1114.2
012300           07 XRECORD-NUMBER       PIC 9(6).                      SQ1114.2
012400           07 FILLER              PIC X(6).                       SQ1114.2
012500           07 UPDATE-NUMBER       PIC 9(2).                       SQ1114.2
012600           07 FILLER              PIC X(5).                       SQ1114.2
012700           07 ODO-NUMBER          PIC 9(4).                       SQ1114.2
012800           07 FILLER              PIC X(5).                       SQ1114.2
012900           07 XPROGRAM-NAME        PIC X(5).                      SQ1114.2
013000           07 FILLER              PIC X(7).                       SQ1114.2
013100           07 XRECORD-LENGTH       PIC 9(6).                      SQ1114.2
013200           07 FILLER              PIC X(7).                       SQ1114.2
013300           07 CHARS-OR-RECORDS    PIC X(2).                       SQ1114.2
013400           07 FILLER              PIC X(1).                       SQ1114.2
013500           07 XBLOCK-SIZE          PIC 9(4).                      SQ1114.2
013600           07 FILLER              PIC X(6).                       SQ1114.2
013700           07 RECORDS-IN-FILE     PIC 9(6).                       SQ1114.2
013800           07 FILLER              PIC X(5).                       SQ1114.2
013900           07 XFILE-ORGANIZATION   PIC X(2).                      SQ1114.2
014000           07 FILLER              PIC X(6).                       SQ1114.2
014100           07 XLABEL-TYPE          PIC X(1).                      SQ1114.2
014200        05 FILE-RECORD-INFO-P121-240.                             SQ1114.2
014300           07 FILLER              PIC X(8).                       SQ1114.2
014400           07 XRECORD-KEY          PIC X(29).                     SQ1114.2
014500           07 FILLER              PIC X(9).                       SQ1114.2
014600           07 ALTERNATE-KEY1      PIC X(29).                      SQ1114.2
014700           07 FILLER              PIC X(9).                       SQ1114.2
014800           07 ALTERNATE-KEY2      PIC X(29).                      SQ1114.2
014900           07 FILLER              PIC X(7).                       SQ1114.2
015000 01  TEST-RESULTS.                                                SQ1114.2
015100     02 FILLER                    PICTURE X VALUE SPACE.          SQ1114.2
015200     02 FEATURE                   PICTURE X(20) VALUE SPACE.      SQ1114.2
015300     02 FILLER                    PICTURE X VALUE SPACE.          SQ1114.2
015400     02 P-OR-F                    PICTURE X(5) VALUE SPACE.       SQ1114.2
015500     02 FILLER                    PICTURE X  VALUE SPACE.         SQ1114.2
015600     02  PAR-NAME.                                                SQ1114.2
015700       03 FILLER PICTURE X(12) VALUE SPACE.                       SQ1114.2
015800       03  PARDOT-X PICTURE X  VALUE SPACE.                       SQ1114.2
015900       03 DOTVALUE PICTURE 99  VALUE ZERO.                        SQ1114.2
016000       03 FILLER PIC X(5) VALUE SPACE.                            SQ1114.2
016100     02 FILLER PIC X(10) VALUE SPACE.                             SQ1114.2
016200     02 RE-MARK PIC X(61).                                        SQ1114.2
016300 01  TEST-COMPUTED.                                               SQ1114.2
016400     02 FILLER PIC X(30) VALUE SPACE.                             SQ1114.2
016500     02 FILLER PIC X(17) VALUE "       COMPUTED=".                SQ1114.2
016600     02 COMPUTED-X.                                               SQ1114.2
016700     03 COMPUTED-A                PICTURE X(20) VALUE SPACE.      SQ1114.2
016800     03 COMPUTED-N REDEFINES COMPUTED-A PICTURE -9(9).9(9).       SQ1114.2
016900     03 COMPUTED-0V18 REDEFINES COMPUTED-A  PICTURE -.9(18).      SQ1114.2
017000     03 COMPUTED-4V14 REDEFINES COMPUTED-A  PICTURE -9(4).9(14).  SQ1114.2
017100     03 COMPUTED-14V4 REDEFINES COMPUTED-A  PICTURE -9(14).9(4).  SQ1114.2
017200     03       CM-18V0 REDEFINES COMPUTED-A.                       SQ1114.2
017300         04 COMPUTED-18V0                   PICTURE -9(18).       SQ1114.2
017400         04 FILLER                          PICTURE X.            SQ1114.2
017500     03 FILLER PIC X(50) VALUE SPACE.                             SQ1114.2
017600 01  TEST-CORRECT.                                                SQ1114.2
017700     02 FILLER PIC X(30) VALUE SPACE.                             SQ1114.2
017800     02 FILLER PIC X(17) VALUE "       CORRECT =".                SQ1114.2
017900     02 CORRECT-X.                                                SQ1114.2
018000     03 CORRECT-A                 PICTURE X(20) VALUE SPACE.      SQ1114.2
018100     03 CORRECT-N REDEFINES CORRECT-A PICTURE -9(9).9(9).         SQ1114.2
018200     03 CORRECT-0V18 REDEFINES CORRECT-A    PICTURE -.9(18).      SQ1114.2
018300     03 CORRECT-4V14 REDEFINES CORRECT-A    PICTURE -9(4).9(14).  SQ1114.2
018400     03 CORRECT-14V4 REDEFINES CORRECT-A    PICTURE -9(14).9(4).  SQ1114.2
018500     03      CR-18V0 REDEFINES CORRECT-A.                         SQ1114.2
018600         04 CORRECT-18V0                    PICTURE -9(18).       SQ1114.2
018700         04 FILLER                          PICTURE X.            SQ1114.2
018800     03 FILLER PIC X(50) VALUE SPACE.                             SQ1114.2
018900 01  CCVS-C-1.                                                    SQ1114.2
019000     02 FILLER PICTURE IS X(99) VALUE IS " FEATURE              PASQ1114.2
019100-    "SS  PARAGRAPH-NAME                                          SQ1114.2
019200-    "        REMARKS".                                           SQ1114.2
019300     02 FILLER PICTURE IS X(20) VALUE IS SPACE.                   SQ1114.2
019400 01  CCVS-C-2.                                                    SQ1114.2
019500     02 FILLER PICTURE IS X VALUE IS SPACE.                       SQ1114.2
019600     02 FILLER PICTURE IS X(6) VALUE IS "TESTED".                 SQ1114.2
019700     02 FILLER PICTURE IS X(15) VALUE IS SPACE.                   SQ1114.2
019800     02 FILLER PICTURE IS X(4) VALUE IS "FAIL".                   SQ1114.2
019900     02 FILLER PICTURE IS X(94) VALUE IS SPACE.                   SQ1114.2
020000 01  REC-SKL-SUB PICTURE 9(2) VALUE ZERO.                         SQ1114.2
020100 01  REC-CT PICTURE 99 VALUE ZERO.                                SQ1114.2
020200 01  DELETE-CNT                   PICTURE 999  VALUE ZERO.        SQ1114.2
020300 01  ERROR-COUNTER PICTURE IS 999 VALUE IS ZERO.                  SQ1114.2
020400 01  INSPECT-COUNTER PIC 999 VALUE ZERO.                          SQ1114.2
020500 01  PASS-COUNTER PIC 999 VALUE ZERO.                             SQ1114.2
020600 01  TOTAL-ERROR PIC 999 VALUE ZERO.                              SQ1114.2
020700 01  ERROR-HOLD PIC 999 VALUE ZERO.                               SQ1114.2
020800 01  DUMMY-HOLD PIC X(120) VALUE SPACE.                           SQ1114.2
020900 01  RECORD-COUNT PIC 9(5) VALUE ZERO.                            SQ1114.2
021000 01  CCVS-H-1.                                                    SQ1114.2
021100     02  FILLER   PICTURE X(27)  VALUE SPACE.                     SQ1114.2
021200     02 FILLER PICTURE X(67) VALUE                                SQ1114.2
021300     " FEDERAL SOFTWARE TESTING CENTER COBOL COMPILER VALIDATION  SQ1114.2
021400-    " SYSTEM".                                                   SQ1114.2
021500     02  FILLER     PICTURE X(26)  VALUE SPACE.                   SQ1114.2
021600 01  CCVS-H-2.                                                    SQ1114.2
021700     02 FILLER PICTURE X(52) VALUE IS                             SQ1114.2
021800     "CCVS85 FSTC COPY, NOT FOR DISTRIBUTION.".                   SQ1114.2
021900     02 FILLER PICTURE IS X(19) VALUE IS "TEST RESULTS SET-  ".   SQ1114.2
022000     02 TEST-ID PICTURE IS X(9).                                  SQ1114.2
022100     02 FILLER PICTURE IS X(40) VALUE IS SPACE.                   SQ1114.2
022200 01  CCVS-H-3.                                                    SQ1114.2
022300     02  FILLER PICTURE X(34) VALUE                               SQ1114.2
022400     " FOR OFFICIAL USE ONLY    ".                                SQ1114.2
022500     02  FILLER PICTURE X(58) VALUE                               SQ1114.2
022600     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ1114.2
022700     02  FILLER PICTURE X(28) VALUE                               SQ1114.2
022800     "  COPYRIGHT   1985 ".                                       SQ1114.2
022900 01  CCVS-E-1.                                                    SQ1114.2
023000     02 FILLER PICTURE IS X(52) VALUE IS SPACE.                   SQ1114.2
023100     02 FILLER PICTURE IS X(14) VALUE IS "END OF TEST-  ".        SQ1114.2
023200     02 ID-AGAIN PICTURE IS X(9).                                 SQ1114.2
023300     02 FILLER PICTURE X(45) VALUE IS                             SQ1114.2
023400     " NTIS DISTRIBUTION COBOL 85".                               SQ1114.2
023500 01  CCVS-E-2.                                                    SQ1114.2
023600     02  FILLER                   PICTURE X(31)  VALUE            SQ1114.2
023700     SPACE.                                                       SQ1114.2
023800     02  FILLER                   PICTURE X(21)  VALUE SPACE.     SQ1114.2
023900     02 CCVS-E-2-2.                                               SQ1114.2
024000         03 ERROR-TOTAL PICTURE IS XXX VALUE IS SPACE.            SQ1114.2
024100         03 FILLER PICTURE IS X VALUE IS SPACE.                   SQ1114.2
024200         03 ENDER-DESC PIC X(46) VALUE "ERRORS ENCOUNTERED".      SQ1114.2
024300 01  CCVS-E-3.                                                    SQ1114.2
024400     02  FILLER PICTURE X(22) VALUE                               SQ1114.2
024500     " FOR OFFICIAL USE ONLY".                                    SQ1114.2
024600     02  FILLER PICTURE X(12) VALUE SPACE.                        SQ1114.2
024700     02  FILLER PICTURE X(58) VALUE                               SQ1114.2
024800     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1114.2
024900     02  FILLER PICTURE X(13) VALUE SPACE.                        SQ1114.2
025000     02 FILLER PIC X(15) VALUE " COPYRIGHT 1985".                 SQ1114.2
025100 01  CCVS-E-4.                                                    SQ1114.2
025200     02 CCVS-E-4-1 PIC XXX VALUE SPACE.                           SQ1114.2
025300     02 FILLER PIC XXXX VALUE " OF ".                             SQ1114.2
025400     02 CCVS-E-4-2 PIC XXX VALUE SPACE.                           SQ1114.2
025500     02 FILLER PIC X(40) VALUE                                    SQ1114.2
025600      "  TESTS WERE EXECUTED SUCCESSFULLY".                       SQ1114.2
025700 01  XXINFO.                                                      SQ1114.2
025800     02 FILLER PIC X(30) VALUE "        *** INFORMATION  ***".    SQ1114.2
025900     02 INFO-TEXT.                                                SQ1114.2
026000     04 FILLER PIC X(20) VALUE SPACE.                             SQ1114.2
026100     04 XXCOMPUTED PIC X(20).                                     SQ1114.2
026200     04 FILLER PIC X(5) VALUE SPACE.                              SQ1114.2
026300     04 XXCORRECT PIC X(20).                                      SQ1114.2
026400 01  HYPHEN-LINE.                                                 SQ1114.2
026500     02 FILLER PICTURE IS X VALUE IS SPACE.                       SQ1114.2
026600     02 FILLER PICTURE IS X(65) VALUE IS "************************SQ1114.2
026700-    "*****************************************".                 SQ1114.2
026800     02 FILLER PICTURE IS X(54) VALUE IS "************************SQ1114.2
026900-    "******************************".                            SQ1114.2
027000 01  CCVS-PGM-ID PIC X(6) VALUE                                   SQ1114.2
027100     "SQ111A".                                                    SQ1114.2
027200 PROCEDURE DIVISION.                                              SQ1114.2
027300 CCVS1 SECTION.                                                   SQ1114.2
027400 OPEN-FILES.                                                      SQ1114.2
027500*    OPEN I-O RAW-DATA.                                           SQ1114.2
027600*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            SQ1114.2
027700*    READ RAW-DATA INVALID KEY GO TO END-E-1.                     SQ1114.2
027800*    MOVE "ABORTED " TO C-ABORT.                                  SQ1114.2
027900*    ADD 1 TO C-NO-OF-TESTS.                                      SQ1114.2
028000*    ACCEPT C-DATE  FROM DATE.                                    SQ1114.2
028100*    ACCEPT C-TIME  FROM TIME.                                    SQ1114.2
028200*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-1.             SQ1114.2
028300*END-E-1.                                                         SQ1114.2
028400*    CLOSE RAW-DATA.                                              SQ1114.2
028500     OPEN     OUTPUT PRINT-FILE.                                  SQ1114.2
028600     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   SQ1114.2
028700     MOVE    SPACE TO TEST-RESULTS.                               SQ1114.2
028800     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             SQ1114.2
028900     MOVE ZERO TO REC-SKL-SUB.                                    SQ1114.2
029000     PERFORM CCVS-INIT-FILE 9 TIMES.                              SQ1114.2
029100 CCVS-INIT-FILE.                                                  SQ1114.2
029200     ADD 1 TO REC-SKL-SUB.                                        SQ1114.2
029300     MOVE FILE-RECORD-INFO-SKELETON TO                            SQ1114.2
029400                  FILE-RECORD-INFO (REC-SKL-SUB).                 SQ1114.2
029500 CCVS-INIT-EXIT.                                                  SQ1114.2
029600     GO TO CCVS1-EXIT.                                            SQ1114.2
029700 CLOSE-FILES.                                                     SQ1114.2
029800     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   SQ1114.2
029900*    OPEN I-O RAW-DATA.                                           SQ1114.2
030000*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            SQ1114.2
030100*    READ RAW-DATA INVALID KEY GO TO END-E-2.                     SQ1114.2
030200*    MOVE "OK.     " TO C-ABORT.                                  SQ1114.2
030300*    MOVE PASS-COUNTER TO C-OK.                                   SQ1114.2
030400*    MOVE ERROR-HOLD   TO C-ALL.                                  SQ1114.2
030500*    MOVE ERROR-COUNTER TO C-FAIL.                                SQ1114.2
030600*    MOVE DELETE-CNT TO C-DELETED.                                SQ1114.2
030700*    MOVE INSPECT-COUNTER TO C-INSPECT.                           SQ1114.2
030800*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-2.             SQ1114.2
030900*END-E-2.                                                         SQ1114.2
031000*    CLOSE RAW-DATA.                                              SQ1114.2
031100 TERMINATE-CCVS.                                                  SQ1114.2
031200*    EXIT PROGRAM.                                                SQ1114.2
031300*TERMINATE-CALL.                                                  SQ1114.2
031400     STOP     RUN.                                                SQ1114.2
031500 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         SQ1114.2
031600 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           SQ1114.2
031700 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          SQ1114.2
031800 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-CNT.          SQ1114.2
031900     MOVE "****TEST DELETED****" TO RE-MARK.                      SQ1114.2
032000 PRINT-DETAIL.                                                    SQ1114.2
032100     IF REC-CT NOT EQUAL TO ZERO                                  SQ1114.2
032200             MOVE "." TO PARDOT-X                                 SQ1114.2
032300             MOVE REC-CT TO DOTVALUE.                             SQ1114.2
032400     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      SQ1114.2
032500     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               SQ1114.2
032600        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 SQ1114.2
032700          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 SQ1114.2
032800     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              SQ1114.2
032900     MOVE SPACE TO CORRECT-X.                                     SQ1114.2
033000     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         SQ1114.2
033100     MOVE     SPACE TO RE-MARK.                                   SQ1114.2
033200 HEAD-ROUTINE.                                                    SQ1114.2
033300     MOVE CCVS-H-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ1114.2
033400     MOVE CCVS-H-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.   SQ1114.2
033500     MOVE CCVS-H-3 TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.   SQ1114.2
033600 COLUMN-NAMES-ROUTINE.                                            SQ1114.2
033700     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ1114.2
033800     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ1114.2
033900     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ1114.2
034000 END-ROUTINE.                                                     SQ1114.2
034100     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.SQ1114.2
034200 END-RTN-EXIT.                                                    SQ1114.2
034300     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ1114.2
034400 END-ROUTINE-1.                                                   SQ1114.2
034500      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      SQ1114.2
034600      ERROR-HOLD. ADD DELETE-CNT TO ERROR-HOLD.                   SQ1114.2
034700      ADD PASS-COUNTER TO ERROR-HOLD.                             SQ1114.2
034800*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   SQ1114.2
034900      MOVE PASS-COUNTER TO CCVS-E-4-1.                            SQ1114.2
035000      MOVE ERROR-HOLD TO CCVS-E-4-2.                              SQ1114.2
035100      MOVE CCVS-E-4 TO CCVS-E-2-2.                                SQ1114.2
035200      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           SQ1114.2
035300  END-ROUTINE-12.                                                 SQ1114.2
035400      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        SQ1114.2
035500     IF       ERROR-COUNTER IS EQUAL TO ZERO                      SQ1114.2
035600         MOVE "NO " TO ERROR-TOTAL                                SQ1114.2
035700         ELSE                                                     SQ1114.2
035800         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       SQ1114.2
035900     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           SQ1114.2
036000     PERFORM WRITE-LINE.                                          SQ1114.2
036100 END-ROUTINE-13.                                                  SQ1114.2
036200     IF DELETE-CNT IS EQUAL TO ZERO                               SQ1114.2
036300         MOVE "NO " TO ERROR-TOTAL  ELSE                          SQ1114.2
036400         MOVE DELETE-CNT TO ERROR-TOTAL.                          SQ1114.2
036500     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   SQ1114.2
036600     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ1114.2
036700      IF   INSPECT-COUNTER EQUAL TO ZERO                          SQ1114.2
036800          MOVE "NO " TO ERROR-TOTAL                               SQ1114.2
036900      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   SQ1114.2
037000      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            SQ1114.2
037100      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          SQ1114.2
037200     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ1114.2
037300 WRITE-LINE.                                                      SQ1114.2
037400     ADD 1 TO RECORD-COUNT.                                       SQ1114.2
037500*    IF RECORD-COUNT GREATER 50                                   SQ1114.2
037600*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          SQ1114.2
037700*        MOVE SPACE TO DUMMY-RECORD                               SQ1114.2
037800*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  SQ1114.2
037900*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             SQ1114.2
038000*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     SQ1114.2
038100*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          SQ1114.2
038200*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          SQ1114.2
038300*        MOVE ZERO TO RECORD-COUNT.                               SQ1114.2
038400     PERFORM WRT-LN.                                              SQ1114.2
038500 WRT-LN.                                                          SQ1114.2
038600     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               SQ1114.2
038700     MOVE SPACE TO DUMMY-RECORD.                                  SQ1114.2
038800 BLANK-LINE-PRINT.                                                SQ1114.2
038900     PERFORM WRT-LN.                                              SQ1114.2
039000 FAIL-ROUTINE.                                                    SQ1114.2
039100     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.   SQ1114.2
039200     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.    SQ1114.2
039300     MOVE "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.    SQ1114.2
039400     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     SQ1114.2
039500     GO TO FAIL-ROUTINE-EX.                                       SQ1114.2
039600 FAIL-ROUTINE-WRITE.                                              SQ1114.2
039700     MOVE TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE           SQ1114.2
039800     MOVE TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES.   SQ1114.2
039900 FAIL-ROUTINE-EX. EXIT.                                           SQ1114.2
040000 BAIL-OUT.                                                        SQ1114.2
040100     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.       SQ1114.2
040200     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.               SQ1114.2
040300 BAIL-OUT-WRITE.                                                  SQ1114.2
040400     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  SQ1114.2
040500     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     SQ1114.2
040600 BAIL-OUT-EX. EXIT.                                               SQ1114.2
040700 CCVS1-EXIT.                                                      SQ1114.2
040800     EXIT.                                                        SQ1114.2
040900 SECT-SQ111A-0001 SECTION.                                        SQ1114.2
041000 WRITE-INIT-GF-01.                                                SQ1114.2
041100     MOVE "SQ-FS1" TO XFILE-NAME (1).                             SQ1114.2
041200     MOVE "R1-F-G" TO XRECORD-NAME (1).                           SQ1114.2
041300     MOVE CCVS-PGM-ID TO XPROGRAM-NAME (1).                       SQ1114.2
041400     MOVE 000155 TO XRECORD-LENGTH (1).                           SQ1114.2
041500     MOVE "RC" TO CHARS-OR-RECORDS (1).                           SQ1114.2
041600     MOVE 0001 TO XBLOCK-SIZE (1).                                SQ1114.2
041700     MOVE 000595 TO RECORDS-IN-FILE (1).                          SQ1114.2
041800     MOVE "SQ" TO XFILE-ORGANIZATION (1).                         SQ1114.2
041900     MOVE "S" TO XLABEL-TYPE (1).                                 SQ1114.2
042000     MOVE 000001 TO XRECORD-NUMBER (1).                           SQ1114.2
042100     OPEN OUTPUT SQ-FS1.                                          SQ1114.2
042200 WRITE-TEST-GF-01.                                                SQ1114.2
042300     ADD 1 TO COUNT-OF-RECS.                                      SQ1114.2
042400     MOVE COUNT-OF-RECS TO XRECORD-NUMBER (1).                    SQ1114.2
042500     MOVE FILE-RECORD-INFO-P1-120 (1) TO SQ-FS1-FIRST.            SQ1114.2
042600     MOVE COUNT-OF-RECS TO SQ-FS1-RECNO.                          SQ1114.2
042700     MOVE "WRITE-SET USED IN CREATING FILE" TO SQ-FS1-FILLER.     SQ1114.2
042800     WRITE SQ-FS1R1-F-G-155.                                      SQ1114.2
042900     IF COUNT-OF-RECS EQUAL TO 595                                SQ1114.2
043000          GO TO WRITE-WRITE-GF-01.                                SQ1114.2
043100     GO TO WRITE-TEST-GF-01.                                      SQ1114.2
043200 WRITE-WRITE-GF-01.                                               SQ1114.2
043300     MOVE "WRITE  FILE SQ-FS1" TO FEATURE.                        SQ1114.2
043400     MOVE "WRITE-TEST-GF-01" TO PAR-NAME.                         SQ1114.2
043500     MOVE "FILE CREATED, RECS =" TO COMPUTED-A.                   SQ1114.2
043600     MOVE COUNT-OF-RECS TO CORRECT-18V0.                          SQ1114.2
043700     MOVE "CODE-SET CLAUSE IN FD" TO RE-MARK.                     SQ1114.2
043800     PERFORM PRINT-DETAIL.                                        SQ1114.2
043900     CLOSE SQ-FS1.                                                SQ1114.2
044000 READ-INIT-GF-01.                                                 SQ1114.2
044100     MOVE ZERO TO COUNT-OF-RECS.                                  SQ1114.2
044200*       THIS TEST READS AND CHECKS THE FILE CREATED IN            SQ1114.2
044300*    READ-TEST-GF-01.                                             SQ1114.2
044400     OPEN INPUT SQ-FS1.                                           SQ1114.2
044500 READ-TEST-GF-01.                                                 SQ1114.2
044600     READ SQ-FS1 RECORD                                           SQ1114.2
044700        AT END GO TO READ-TEST-GF-01-1.                           SQ1114.2
044800     ADD 1 TO COUNT-OF-RECS.                                      SQ1114.2
044900     IF COUNT-OF-RECS EQUAL TO 596                                SQ1114.2
045000         MOVE "MORE THAN 595 RECORDS" TO RE-MARK                  SQ1114.2
045100         GO TO READ-FAIL-GF-01-1.                                 SQ1114.2
045200     MOVE SQ-FS1-FIRST TO FILE-RECORD-INFO-P1-120 (1).            SQ1114.2
045300     IF COUNT-OF-RECS NOT EQUAL TO XRECORD-NUMBER (1)             SQ1114.2
045400         ADD 1 TO RECORDS-IN-ERROR                                SQ1114.2
045500         GO TO READ-TEST-GF-01.                                   SQ1114.2
045600     IF XFILE-NAME (1) NOT EQUAL TO "SQ-FS1"                      SQ1114.2
045700         ADD 1 TO RECORDS-IN-ERROR                                SQ1114.2
045800         GO TO READ-TEST-GF-01.                                   SQ1114.2
045900     MOVE SQ-FS1-RECNO TO COMPARE-ITEM.                           SQ1114.2
046000     IF COMPARE-REC-NO EQUAL TO COUNT-OF-RECS                     SQ1114.2
046100         GO TO READ-TEST-GF-01.                                   SQ1114.2
046200     ADD 1 TO RECORDS-IN-ERROR.                                   SQ1114.2
046300     GO TO READ-TEST-GF-01.                                       SQ1114.2
046400 READ-TEST-GF-01-1.                                               SQ1114.2
046500     IF COUNT-OF-RECS NOT EQUAL TO 595                            SQ1114.2
046600         MOVE "UNEXPECTED EOF" TO RE-MARK                         SQ1114.2
046700         MOVE "RECORDS READ =" TO COMPUTED-A                      SQ1114.2
046800         MOVE COUNT-OF-RECS TO CORRECT-18V0                       SQ1114.2
046900         GO TO READ-FAIL-GF-01.                                   SQ1114.2
047000     IF RECORDS-IN-ERROR EQUAL TO ZERO                            SQ1114.2
047100         GO TO READ-PASS-GF-01.                                   SQ1114.2
047200     MOVE "VII-44; 4.4.2; ERRORS IN READING SQ-FS1" TO RE-MARK.   SQ1114.2
047300 READ-FAIL-GF-01-1.                                               SQ1114.2
047400     MOVE "RECORDS IN ERROR =" TO COMPUTED-A.                     SQ1114.2
047500     MOVE RECORDS-IN-ERROR TO CORRECT-18V0.                       SQ1114.2
047600 READ-FAIL-GF-01.                                                 SQ1114.2
047700     PERFORM FAIL.                                                SQ1114.2
047800     GO TO READ-WRITE-GF-01.                                      SQ1114.2
047900 READ-PASS-GF-01.                                                 SQ1114.2
048000     PERFORM PASS.                                                SQ1114.2
048100     MOVE "FILE VERIFIED RECS =" TO COMPUTED-A.                   SQ1114.2
048200     MOVE COUNT-OF-RECS TO CORRECT-18V0.                          SQ1114.2
048300 READ-WRITE-GF-01.                                                SQ1114.2
048400     MOVE "READ-TEST-GF-01" TO PAR-NAME.                          SQ1114.2
048500     MOVE "READ TO VERIFY    " TO FEATURE.                        SQ1114.2
048600     PERFORM PRINT-DETAIL.                                        SQ1114.2
048700 READ-CLOSE-GF-01.                                                SQ1114.2
048800     CLOSE SQ-FS1.                                                SQ1114.2
048900 CCVS-EXIT SECTION.                                               SQ1114.2
049000 CCVS-999999.                                                     SQ1114.2
049100     GO TO CLOSE-FILES.                                           SQ1114.2
      *END-OF,SQ111A                                                                  
