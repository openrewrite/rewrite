      *HEADER,COBOL,SQ214A                                                            
000100 IDENTIFICATION DIVISION.                                         SQ2144.2
000200 PROGRAM-ID.                                                      SQ2144.2
000300     SQ214A.                                                      SQ2144.2
000400****************************************************************  SQ2144.2
000500*                                                              *  SQ2144.2
000600*    VALIDATION FOR:-                                          *  SQ2144.2
000700*    " HIGH       ".                                              SQ2144.2
000800*                                                              *  SQ2144.2
000900*    CREATION DATE     /     VALIDATION DATE                   *  SQ2144.2
001000*    "4.2 ".                                                      SQ2144.2
001100*                                                              *  SQ2144.2
001200*                                                                 SQ2144.2
001300*                                                                 SQ2144.2
001400*    SQ214A TESTS OPERATIONS INVOLVING FORMAT 2 OCCURS CLAUSES,   SQ2144.2
001500*    I.E.  ...OCCURS INTEGER-1 TO INTEGER-2 TIMES DEPENDING ON    SQ2144.2
001600*          DATA-NAME-1 ....                                       SQ2144.2
001700*    X3.23-1976, PAGE III-4, 2.1.4(3) STATES, IN PART, THAT       SQ2144.2
001800*    INTEGER-2 REPRESENTS THE MAXIMUM NUMBER OF OCCURRENCES AND   SQ2144.2
001900*    THAT ONLY THE NUMBER OF OCCURRENCES, AND NOT THE ITEM LENGTH,SQ2144.2
002000*    IS VARIABLE.  WHENEVER THE PARENT GROUP ITEM IS REFERENCED,  SQ2144.2
002100*    ONLY THE PORTION OF THE TABLE SPECIFIED BY THE CURRENT VALUE SQ2144.2
002200*    OF DATA-NAME-1 WILL BE USED IN THE OPERATION.                SQ2144.2
002300*                                                                 SQ2144.2
002400*    THE FOLLOWING VERBS ARE EXERCIZED,                           SQ2144.2
002500*        READ                                                     SQ2144.2
002600*        WRITE                                                    SQ2144.2
002700*                                                                 SQ2144.2
002800*                                                                 SQ2144.2
002900 ENVIRONMENT DIVISION.                                            SQ2144.2
003000 CONFIGURATION SECTION.                                           SQ2144.2
003100 SOURCE-COMPUTER.                                                 SQ2144.2
003200     XXXXX082.                                                    SQ2144.2
003300 OBJECT-COMPUTER.                                                 SQ2144.2
003400     XXXXX083.                                                    SQ2144.2
003500 INPUT-OUTPUT SECTION.                                            SQ2144.2
003600 FILE-CONTROL.                                                    SQ2144.2
003700*    SELECT RAW-DATA   ASSIGN TO                                  SQ2144.2
003800*    XXXXX062                                                     SQ2144.2
003900*           ORGANIZATION IS INDEXED                               SQ2144.2
004000*           ACCESS MODE IS RANDOM                                 SQ2144.2
004100*           RECORD KEY IS RAW-DATA-KEY.                           SQ2144.2
004200     SELECT PRINT-FILE ASSIGN TO                                  SQ2144.2
004300     XXXXX055.                                                    SQ2144.2
004400     SELECT  SQ-FS1  ASSIGN TO                                    SQ2144.2
004500     XXXXX014.                                                    SQ2144.2
004600 DATA DIVISION.                                                   SQ2144.2
004700 FILE SECTION.                                                    SQ2144.2
004800*                                                                 SQ2144.2
004900*FD  RAW-DATA.                                                    SQ2144.2
005000*                                                                 SQ2144.2
005100*01  RAW-DATA-SATZ.                                               SQ2144.2
005200*    05  RAW-DATA-KEY        PIC X(6).                            SQ2144.2
005300*    05  C-DATE              PIC 9(6).                            SQ2144.2
005400*    05  C-TIME              PIC 9(8).                            SQ2144.2
005500*    05  C-NO-OF-TESTS       PIC 99.                              SQ2144.2
005600*    05  C-OK                PIC 999.                             SQ2144.2
005700*    05  C-ALL               PIC 999.                             SQ2144.2
005800*    05  C-FAIL              PIC 999.                             SQ2144.2
005900*    05  C-DELETED           PIC 999.                             SQ2144.2
006000*    05  C-INSPECT           PIC 999.                             SQ2144.2
006100*    05  C-NOTE              PIC X(13).                           SQ2144.2
006200*    05  C-INDENT            PIC X.                               SQ2144.2
006300*    05  C-ABORT             PIC X(8).                            SQ2144.2
006400 FD  PRINT-FILE                                                   SQ2144.2
006500C    LABEL RECORDS                                                SQ2144.2
006600C    XXXXX084                                                     SQ2144.2
006700C    DATA RECORD IS PRINT-REC DUMMY-RECORD                        SQ2144.2
006800               .                                                  SQ2144.2
006900 01  PRINT-REC PICTURE X(120).                                    SQ2144.2
007000 01  DUMMY-RECORD PICTURE X(120).                                 SQ2144.2
007100 FD  SQ-FS1                                                       SQ2144.2
007200C    LABEL RECORD IS STANDARD                                     SQ2144.2
007300                .                                                 SQ2144.2
007400 01  SQ-FS1R1-F-G-140.                                            SQ2144.2
007500     02 FS1R1-XN-120 PIC X(120).                                  SQ2144.2
007600     02  FS1R1-XN-20.                                             SQ2144.2
007700         03  FS1R1-XN-13  PIC X(13).                              SQ2144.2
007800         03  FS1R1-XN-6   PIC X(6).                               SQ2144.2
007900         03  FILLER       PIC X.                                  SQ2144.2
008000 WORKING-STORAGE SECTION.                                         SQ2144.2
008100 01  ODO-RECORD.                                                  SQ2144.2
008200     02  FILLER PIC X(120).                                       SQ2144.2
008300     02  GRP-ODO.                                                 SQ2144.2
008400         03  DOI-DU-01V00 PIC 9.                                  SQ2144.2
008500         03  ODO-XN-00009  PIC X(9).                              SQ2144.2
008600         03  ODO-GRP-00009.                                       SQ2144.2
008700         04  ODO-XN-00001-O009D OCCURS 1 TO 9 TIMES DEPENDING ON  SQ2144.2
008800                 DOI-DU-01V00 ASCENDING KEY ODO-XN-00001-O009D    SQ2144.2
008900                 INDEXED BY ODO-IX PIC X.                         SQ2144.2
009000 01  STATIC-VALUE.                                                SQ2144.2
009100     02  FILLER PIC 9 VALUE 9.                                    SQ2144.2
009200     02  FILLER PIC X(18) VALUE " ACTIVE: 123456789".             SQ2144.2
009300 01  WRK-GRP-00019.                                               SQ2144.2
009400     02  WRK-DU-01V00 PIC 9.                                      SQ2144.2
009500     02  WRK-XN-00009-1 PIC X(9).                                 SQ2144.2
009600     02  WRK-XN-00009-2 PIC X(9).                                 SQ2144.2
009700 01  WRK-GRP-00009.                                               SQ2144.2
009800     02  ODO-XN-00007  PIC X(7).                                  SQ2144.2
009900     02  ODO-XN-00002  PIC XX.                                    SQ2144.2
010000 01  WRK-GRP-00009A  REDEFINES  WRK-GRP-00009.                    SQ2144.2
010100     02  ODO-XN-00005  PIC X(5).                                  SQ2144.2
010200     02  ODO-XN-00004  PIC X(4).                                  SQ2144.2
010300 01  WRK-DU-05V00 PIC 9(5).                                       SQ2144.2
010400 01  WRK-XN-00020 PIC X(20).                                      SQ2144.2
010500 01  WRK-XN-00010 PIC X(10).                                      SQ2144.2
010600 01  FILE-RECORD-INFORMATION-REC.                                 SQ2144.2
010700     03 FILE-RECORD-INFO-SKELETON.                                SQ2144.2
010800        05 FILLER                 PICTURE X(48)       VALUE       SQ2144.2
010900             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  SQ2144.2
011000        05 FILLER                 PICTURE X(46)       VALUE       SQ2144.2
011100             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    SQ2144.2
011200        05 FILLER                 PICTURE X(26)       VALUE       SQ2144.2
011300             ",LFIL=000000,ORG=  ,LBLR= ".                        SQ2144.2
011400        05 FILLER                 PICTURE X(37)       VALUE       SQ2144.2
011500             ",RECKEY=                             ".             SQ2144.2
011600        05 FILLER                 PICTURE X(38)       VALUE       SQ2144.2
011700             ",ALTKEY1=                             ".            SQ2144.2
011800        05 FILLER                 PICTURE X(38)       VALUE       SQ2144.2
011900             ",ALTKEY2=                             ".            SQ2144.2
012000        05 FILLER                 PICTURE X(7)        VALUE SPACE.SQ2144.2
012100     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              SQ2144.2
012200        05 FILE-RECORD-INFO-P1-120.                               SQ2144.2
012300           07 FILLER              PIC X(5).                       SQ2144.2
012400           07 XFILE-NAME           PIC X(6).                      SQ2144.2
012500           07 FILLER              PIC X(8).                       SQ2144.2
012600           07 XRECORD-NAME         PIC X(6).                      SQ2144.2
012700           07 FILLER              PIC X(1).                       SQ2144.2
012800           07 REELUNIT-NUMBER     PIC 9(1).                       SQ2144.2
012900           07 FILLER              PIC X(7).                       SQ2144.2
013000           07 XRECORD-NUMBER       PIC 9(6).                      SQ2144.2
013100           07 FILLER              PIC X(6).                       SQ2144.2
013200           07 UPDATE-NUMBER       PIC 9(2).                       SQ2144.2
013300           07 FILLER              PIC X(5).                       SQ2144.2
013400           07 ODO-NUMBER          PIC 9(4).                       SQ2144.2
013500           07 FILLER              PIC X(5).                       SQ2144.2
013600           07 XPROGRAM-NAME        PIC X(5).                      SQ2144.2
013700           07 FILLER              PIC X(7).                       SQ2144.2
013800           07 XRECORD-LENGTH       PIC 9(6).                      SQ2144.2
013900           07 FILLER              PIC X(7).                       SQ2144.2
014000           07 CHARS-OR-RECORDS    PIC X(2).                       SQ2144.2
014100           07 FILLER              PIC X(1).                       SQ2144.2
014200           07 XBLOCK-SIZE          PIC 9(4).                      SQ2144.2
014300           07 FILLER              PIC X(6).                       SQ2144.2
014400           07 RECORDS-IN-FILE     PIC 9(6).                       SQ2144.2
014500           07 FILLER              PIC X(5).                       SQ2144.2
014600           07 XFILE-ORGANIZATION   PIC X(2).                      SQ2144.2
014700           07 FILLER              PIC X(6).                       SQ2144.2
014800           07 XLABEL-TYPE          PIC X(1).                      SQ2144.2
014900        05 FILE-RECORD-INFO-P121-240.                             SQ2144.2
015000           07 FILLER              PIC X(8).                       SQ2144.2
015100           07 XRECORD-KEY          PIC X(29).                     SQ2144.2
015200           07 FILLER              PIC X(9).                       SQ2144.2
015300           07 ALTERNATE-KEY1      PIC X(29).                      SQ2144.2
015400           07 FILLER              PIC X(9).                       SQ2144.2
015500           07 ALTERNATE-KEY2      PIC X(29).                      SQ2144.2
015600           07 FILLER              PIC X(7).                       SQ2144.2
015700 01  TEST-RESULTS.                                                SQ2144.2
015800     02 FILLER                    PICTURE X VALUE SPACE.          SQ2144.2
015900     02 FEATURE                   PICTURE X(20) VALUE SPACE.      SQ2144.2
016000     02 FILLER                    PICTURE X VALUE SPACE.          SQ2144.2
016100     02 P-OR-F                    PICTURE X(5) VALUE SPACE.       SQ2144.2
016200     02 FILLER                    PICTURE X  VALUE SPACE.         SQ2144.2
016300     02  PAR-NAME.                                                SQ2144.2
016400       03 FILLER PICTURE X(12) VALUE SPACE.                       SQ2144.2
016500       03  PARDOT-X PICTURE X  VALUE SPACE.                       SQ2144.2
016600       03 DOTVALUE PICTURE 99  VALUE ZERO.                        SQ2144.2
016700       03 FILLER PIC X(5) VALUE SPACE.                            SQ2144.2
016800     02 FILLER PIC X(10) VALUE SPACE.                             SQ2144.2
016900     02 RE-MARK PIC X(61).                                        SQ2144.2
017000 01  TEST-COMPUTED.                                               SQ2144.2
017100     02 FILLER PIC X(30) VALUE SPACE.                             SQ2144.2
017200     02 FILLER PIC X(17) VALUE "       COMPUTED=".                SQ2144.2
017300     02 COMPUTED-X.                                               SQ2144.2
017400     03 COMPUTED-A                PICTURE X(20) VALUE SPACE.      SQ2144.2
017500     03 COMPUTED-N REDEFINES COMPUTED-A PICTURE -9(9).9(9).       SQ2144.2
017600     03 COMPUTED-0V18 REDEFINES COMPUTED-A  PICTURE -.9(18).      SQ2144.2
017700     03 COMPUTED-4V14 REDEFINES COMPUTED-A  PICTURE -9(4).9(14).  SQ2144.2
017800     03 COMPUTED-14V4 REDEFINES COMPUTED-A  PICTURE -9(14).9(4).  SQ2144.2
017900     03       CM-18V0 REDEFINES COMPUTED-A.                       SQ2144.2
018000         04 COMPUTED-18V0                   PICTURE -9(18).       SQ2144.2
018100         04 FILLER                          PICTURE X.            SQ2144.2
018200     03 FILLER PIC X(50) VALUE SPACE.                             SQ2144.2
018300 01  TEST-CORRECT.                                                SQ2144.2
018400     02 FILLER PIC X(30) VALUE SPACE.                             SQ2144.2
018500     02 FILLER PIC X(17) VALUE "       CORRECT =".                SQ2144.2
018600     02 CORRECT-X.                                                SQ2144.2
018700     03 CORRECT-A                 PICTURE X(20) VALUE SPACE.      SQ2144.2
018800     03 CORRECT-N REDEFINES CORRECT-A PICTURE -9(9).9(9).         SQ2144.2
018900     03 CORRECT-0V18 REDEFINES CORRECT-A    PICTURE -.9(18).      SQ2144.2
019000     03 CORRECT-4V14 REDEFINES CORRECT-A    PICTURE -9(4).9(14).  SQ2144.2
019100     03 CORRECT-14V4 REDEFINES CORRECT-A    PICTURE -9(14).9(4).  SQ2144.2
019200     03      CR-18V0 REDEFINES CORRECT-A.                         SQ2144.2
019300         04 CORRECT-18V0                    PICTURE -9(18).       SQ2144.2
019400         04 FILLER                          PICTURE X.            SQ2144.2
019500     03 FILLER PIC X(50) VALUE SPACE.                             SQ2144.2
019600 01  CCVS-C-1.                                                    SQ2144.2
019700     02 FILLER PICTURE IS X(99) VALUE IS " FEATURE              PASQ2144.2
019800-    "SS  PARAGRAPH-NAME                                          SQ2144.2
019900-    "        REMARKS".                                           SQ2144.2
020000     02 FILLER PICTURE IS X(20) VALUE IS SPACE.                   SQ2144.2
020100 01  CCVS-C-2.                                                    SQ2144.2
020200     02 FILLER PICTURE IS X VALUE IS SPACE.                       SQ2144.2
020300     02 FILLER PICTURE IS X(6) VALUE IS "TESTED".                 SQ2144.2
020400     02 FILLER PICTURE IS X(15) VALUE IS SPACE.                   SQ2144.2
020500     02 FILLER PICTURE IS X(4) VALUE IS "FAIL".                   SQ2144.2
020600     02 FILLER PICTURE IS X(94) VALUE IS SPACE.                   SQ2144.2
020700 01  REC-SKL-SUB PICTURE 9(2) VALUE ZERO.                         SQ2144.2
020800 01  REC-CT PICTURE 99 VALUE ZERO.                                SQ2144.2
020900 01  DELETE-CNT                   PICTURE 999  VALUE ZERO.        SQ2144.2
021000 01  ERROR-COUNTER PICTURE IS 999 VALUE IS ZERO.                  SQ2144.2
021100 01  INSPECT-COUNTER PIC 999 VALUE ZERO.                          SQ2144.2
021200 01  PASS-COUNTER PIC 999 VALUE ZERO.                             SQ2144.2
021300 01  TOTAL-ERROR PIC 999 VALUE ZERO.                              SQ2144.2
021400 01  ERROR-HOLD PIC 999 VALUE ZERO.                               SQ2144.2
021500 01  DUMMY-HOLD PIC X(120) VALUE SPACE.                           SQ2144.2
021600 01  RECORD-COUNT PIC 9(5) VALUE ZERO.                            SQ2144.2
021700 01  CCVS-H-1.                                                    SQ2144.2
021800     02  FILLER   PICTURE X(27)  VALUE SPACE.                     SQ2144.2
021900     02 FILLER PICTURE X(67) VALUE                                SQ2144.2
022000     " FEDERAL SOFTWARE TESTING CENTER COBOL COMPILER VALIDATION  SQ2144.2
022100-    " SYSTEM".                                                   SQ2144.2
022200     02  FILLER     PICTURE X(26)  VALUE SPACE.                   SQ2144.2
022300 01  CCVS-H-2.                                                    SQ2144.2
022400     02 FILLER PICTURE X(52) VALUE IS                             SQ2144.2
022500     "CCVS85 FSTC COPY, NOT FOR DISTRIBUTION.".                   SQ2144.2
022600     02 FILLER PICTURE IS X(19) VALUE IS "TEST RESULTS SET-  ".   SQ2144.2
022700     02 TEST-ID PICTURE IS X(9).                                  SQ2144.2
022800     02 FILLER PICTURE IS X(40) VALUE IS SPACE.                   SQ2144.2
022900 01  CCVS-H-3.                                                    SQ2144.2
023000     02  FILLER PICTURE X(34) VALUE                               SQ2144.2
023100     " FOR OFFICIAL USE ONLY    ".                                SQ2144.2
023200     02  FILLER PICTURE X(58) VALUE                               SQ2144.2
023300     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ2144.2
023400     02  FILLER PICTURE X(28) VALUE                               SQ2144.2
023500     "  COPYRIGHT   1985 ".                                       SQ2144.2
023600 01  CCVS-E-1.                                                    SQ2144.2
023700     02 FILLER PICTURE IS X(52) VALUE IS SPACE.                   SQ2144.2
023800     02 FILLER PICTURE IS X(14) VALUE IS "END OF TEST-  ".        SQ2144.2
023900     02 ID-AGAIN PICTURE IS X(9).                                 SQ2144.2
024000     02 FILLER PICTURE X(45) VALUE IS                             SQ2144.2
024100     " NTIS DISTRIBUTION COBOL 85".                               SQ2144.2
024200 01  CCVS-E-2.                                                    SQ2144.2
024300     02  FILLER                   PICTURE X(31)  VALUE            SQ2144.2
024400     SPACE.                                                       SQ2144.2
024500     02  FILLER                   PICTURE X(21)  VALUE SPACE.     SQ2144.2
024600     02 CCVS-E-2-2.                                               SQ2144.2
024700         03 ERROR-TOTAL PICTURE IS XXX VALUE IS SPACE.            SQ2144.2
024800         03 FILLER PICTURE IS X VALUE IS SPACE.                   SQ2144.2
024900         03 ENDER-DESC PIC X(46) VALUE "ERRORS ENCOUNTERED".      SQ2144.2
025000 01  CCVS-E-3.                                                    SQ2144.2
025100     02  FILLER PICTURE X(22) VALUE                               SQ2144.2
025200     " FOR OFFICIAL USE ONLY".                                    SQ2144.2
025300     02  FILLER PICTURE X(12) VALUE SPACE.                        SQ2144.2
025400     02  FILLER PICTURE X(58) VALUE                               SQ2144.2
025500     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ2144.2
025600     02  FILLER PICTURE X(13) VALUE SPACE.                        SQ2144.2
025700     02 FILLER PIC X(15) VALUE " COPYRIGHT 1985".                 SQ2144.2
025800 01  CCVS-E-4.                                                    SQ2144.2
025900     02 CCVS-E-4-1 PIC XXX VALUE SPACE.                           SQ2144.2
026000     02 FILLER PIC XXXX VALUE " OF ".                             SQ2144.2
026100     02 CCVS-E-4-2 PIC XXX VALUE SPACE.                           SQ2144.2
026200     02 FILLER PIC X(40) VALUE                                    SQ2144.2
026300      "  TESTS WERE EXECUTED SUCCESSFULLY".                       SQ2144.2
026400 01  XXINFO.                                                      SQ2144.2
026500     02 FILLER PIC X(30) VALUE "        *** INFORMATION  ***".    SQ2144.2
026600     02 INFO-TEXT.                                                SQ2144.2
026700     04 FILLER PIC X(20) VALUE SPACE.                             SQ2144.2
026800     04 XXCOMPUTED PIC X(20).                                     SQ2144.2
026900     04 FILLER PIC X(5) VALUE SPACE.                              SQ2144.2
027000     04 XXCORRECT PIC X(20).                                      SQ2144.2
027100 01  HYPHEN-LINE.                                                 SQ2144.2
027200     02 FILLER PICTURE IS X VALUE IS SPACE.                       SQ2144.2
027300     02 FILLER PICTURE IS X(65) VALUE IS "************************SQ2144.2
027400-    "*****************************************".                 SQ2144.2
027500     02 FILLER PICTURE IS X(54) VALUE IS "************************SQ2144.2
027600-    "******************************".                            SQ2144.2
027700 01  CCVS-PGM-ID PIC X(6) VALUE                                   SQ2144.2
027800     "SQ214A".                                                    SQ2144.2
027900 PROCEDURE DIVISION.                                              SQ2144.2
028000 CCVS1 SECTION.                                                   SQ2144.2
028100 OPEN-FILES.                                                      SQ2144.2
028200*    OPEN I-O RAW-DATA.                                           SQ2144.2
028300*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            SQ2144.2
028400*    READ RAW-DATA INVALID KEY GO TO END-E-1.                     SQ2144.2
028500*    MOVE "ABORTED " TO C-ABORT.                                  SQ2144.2
028600*    ADD 1 TO C-NO-OF-TESTS.                                      SQ2144.2
028700*    ACCEPT C-DATE  FROM DATE.                                    SQ2144.2
028800*    ACCEPT C-TIME  FROM TIME.                                    SQ2144.2
028900*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-1.             SQ2144.2
029000*END-E-1.                                                         SQ2144.2
029100*    CLOSE RAW-DATA.                                              SQ2144.2
029200     OPEN     OUTPUT PRINT-FILE.                                  SQ2144.2
029300     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   SQ2144.2
029400     MOVE    SPACE TO TEST-RESULTS.                               SQ2144.2
029500     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             SQ2144.2
029600     MOVE ZERO TO REC-SKL-SUB.                                    SQ2144.2
029700     PERFORM CCVS-INIT-FILE 9 TIMES.                              SQ2144.2
029800 CCVS-INIT-FILE.                                                  SQ2144.2
029900     ADD 1 TO REC-SKL-SUB.                                        SQ2144.2
030000     MOVE FILE-RECORD-INFO-SKELETON TO                            SQ2144.2
030100                  FILE-RECORD-INFO (REC-SKL-SUB).                 SQ2144.2
030200 CCVS-INIT-EXIT.                                                  SQ2144.2
030300     GO TO CCVS1-EXIT.                                            SQ2144.2
030400 CLOSE-FILES.                                                     SQ2144.2
030500     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   SQ2144.2
030600*    OPEN I-O RAW-DATA.                                           SQ2144.2
030700*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            SQ2144.2
030800*    READ RAW-DATA INVALID KEY GO TO END-E-2.                     SQ2144.2
030900*    MOVE "OK.     " TO C-ABORT.                                  SQ2144.2
031000*    MOVE PASS-COUNTER TO C-OK.                                   SQ2144.2
031100*    MOVE ERROR-HOLD   TO C-ALL.                                  SQ2144.2
031200*    MOVE ERROR-COUNTER TO C-FAIL.                                SQ2144.2
031300*    MOVE DELETE-CNT TO C-DELETED.                                SQ2144.2
031400*    MOVE INSPECT-COUNTER TO C-INSPECT.                           SQ2144.2
031500*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-2.             SQ2144.2
031600*END-E-2.                                                         SQ2144.2
031700*    CLOSE RAW-DATA.                                              SQ2144.2
031800 TERMINATE-CCVS.                                                  SQ2144.2
031900*    EXIT PROGRAM.                                                SQ2144.2
032000*TERMINATE-CALL.                                                  SQ2144.2
032100     STOP     RUN.                                                SQ2144.2
032200 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         SQ2144.2
032300 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           SQ2144.2
032400 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          SQ2144.2
032500 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-CNT.          SQ2144.2
032600     MOVE "****TEST DELETED****" TO RE-MARK.                      SQ2144.2
032700 PRINT-DETAIL.                                                    SQ2144.2
032800     IF REC-CT NOT EQUAL TO ZERO                                  SQ2144.2
032900             MOVE "." TO PARDOT-X                                 SQ2144.2
033000             MOVE REC-CT TO DOTVALUE.                             SQ2144.2
033100     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      SQ2144.2
033200     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               SQ2144.2
033300        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 SQ2144.2
033400          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 SQ2144.2
033500     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              SQ2144.2
033600     MOVE SPACE TO CORRECT-X.                                     SQ2144.2
033700     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         SQ2144.2
033800     MOVE     SPACE TO RE-MARK.                                   SQ2144.2
033900 HEAD-ROUTINE.                                                    SQ2144.2
034000     MOVE CCVS-H-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ2144.2
034100     MOVE CCVS-H-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.   SQ2144.2
034200     MOVE CCVS-H-3 TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.   SQ2144.2
034300 COLUMN-NAMES-ROUTINE.                                            SQ2144.2
034400     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ2144.2
034500     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ2144.2
034600     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ2144.2
034700 END-ROUTINE.                                                     SQ2144.2
034800     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.SQ2144.2
034900 END-RTN-EXIT.                                                    SQ2144.2
035000     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ2144.2
035100 END-ROUTINE-1.                                                   SQ2144.2
035200      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      SQ2144.2
035300      ERROR-HOLD. ADD DELETE-CNT TO ERROR-HOLD.                   SQ2144.2
035400      ADD PASS-COUNTER TO ERROR-HOLD.                             SQ2144.2
035500*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   SQ2144.2
035600      MOVE PASS-COUNTER TO CCVS-E-4-1.                            SQ2144.2
035700      MOVE ERROR-HOLD TO CCVS-E-4-2.                              SQ2144.2
035800      MOVE CCVS-E-4 TO CCVS-E-2-2.                                SQ2144.2
035900      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           SQ2144.2
036000  END-ROUTINE-12.                                                 SQ2144.2
036100      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        SQ2144.2
036200     IF       ERROR-COUNTER IS EQUAL TO ZERO                      SQ2144.2
036300         MOVE "NO " TO ERROR-TOTAL                                SQ2144.2
036400         ELSE                                                     SQ2144.2
036500         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       SQ2144.2
036600     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           SQ2144.2
036700     PERFORM WRITE-LINE.                                          SQ2144.2
036800 END-ROUTINE-13.                                                  SQ2144.2
036900     IF DELETE-CNT IS EQUAL TO ZERO                               SQ2144.2
037000         MOVE "NO " TO ERROR-TOTAL  ELSE                          SQ2144.2
037100         MOVE DELETE-CNT TO ERROR-TOTAL.                          SQ2144.2
037200     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   SQ2144.2
037300     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ2144.2
037400      IF   INSPECT-COUNTER EQUAL TO ZERO                          SQ2144.2
037500          MOVE "NO " TO ERROR-TOTAL                               SQ2144.2
037600      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   SQ2144.2
037700      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            SQ2144.2
037800      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          SQ2144.2
037900     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ2144.2
038000 WRITE-LINE.                                                      SQ2144.2
038100     ADD 1 TO RECORD-COUNT.                                       SQ2144.2
038200*    IF RECORD-COUNT GREATER 50                                   SQ2144.2
038300*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          SQ2144.2
038400*        MOVE SPACE TO DUMMY-RECORD                               SQ2144.2
038500*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  SQ2144.2
038600*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             SQ2144.2
038700*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     SQ2144.2
038800*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          SQ2144.2
038900*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          SQ2144.2
039000*        MOVE ZERO TO RECORD-COUNT.                               SQ2144.2
039100     PERFORM WRT-LN.                                              SQ2144.2
039200 WRT-LN.                                                          SQ2144.2
039300     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               SQ2144.2
039400     MOVE SPACE TO DUMMY-RECORD.                                  SQ2144.2
039500 BLANK-LINE-PRINT.                                                SQ2144.2
039600     PERFORM WRT-LN.                                              SQ2144.2
039700 FAIL-ROUTINE.                                                    SQ2144.2
039800     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.   SQ2144.2
039900     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.    SQ2144.2
040000     MOVE "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.    SQ2144.2
040100     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     SQ2144.2
040200     GO TO FAIL-ROUTINE-EX.                                       SQ2144.2
040300 FAIL-ROUTINE-WRITE.                                              SQ2144.2
040400     MOVE TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE           SQ2144.2
040500     MOVE TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES.   SQ2144.2
040600 FAIL-ROUTINE-EX. EXIT.                                           SQ2144.2
040700 BAIL-OUT.                                                        SQ2144.2
040800     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.       SQ2144.2
040900     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.               SQ2144.2
041000 BAIL-OUT-WRITE.                                                  SQ2144.2
041100     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  SQ2144.2
041200     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     SQ2144.2
041300 BAIL-OUT-EX. EXIT.                                               SQ2144.2
041400 CCVS1-EXIT.                                                      SQ2144.2
041500     EXIT.                                                        SQ2144.2
041600 BEGIN-SQ214A-TESTS SECTION.                                      SQ2144.2
041700 WRITE-INIT-GF-01.                                                SQ2144.2
041800     MOVE STATIC-VALUE TO WRK-GRP-00019.                          SQ2144.2
041900     MOVE 9 TO DOI-DU-01V00.                                      SQ2144.2
042000     MOVE " ACTIVE: " TO ODO-XN-00009.                            SQ2144.2
042100     MOVE "1" TO ODO-XN-00001-O009D (1).                          SQ2144.2
042200     MOVE "2" TO ODO-XN-00001-O009D (2).                          SQ2144.2
042300     MOVE "3" TO ODO-XN-00001-O009D (3).                          SQ2144.2
042400     MOVE "4" TO ODO-XN-00001-O009D (4).                          SQ2144.2
042500     MOVE "5" TO ODO-XN-00001-O009D (5).                          SQ2144.2
042600     MOVE "6" TO ODO-XN-00001-O009D (6).                          SQ2144.2
042700     MOVE "7" TO ODO-XN-00001-O009D (7).                          SQ2144.2
042800     MOVE "8" TO ODO-XN-00001-O009D (8).                          SQ2144.2
042900     MOVE "9" TO ODO-XN-00001-O009D (9).                          SQ2144.2
043000 WRITE-SQ-FS1 SECTION.                                            SQ2144.2
043100 WRITE-SQ-FS1-PARA1.                                              SQ2144.2
043200     OPEN OUTPUT SQ-FS1.                                          SQ2144.2
043300     MOVE "SQ-FS1" TO XFILE-NAME (1).                             SQ2144.2
043400     MOVE "FS1R1 " TO XRECORD-NAME (1).                           SQ2144.2
043500     MOVE "SQ214"  TO XPROGRAM-NAME (1).                          SQ2144.2
043600     MOVE 140 TO XRECORD-LENGTH (1).                              SQ2144.2
043700     MOVE "1R" TO CHARS-OR-RECORDS (1).                           SQ2144.2
043800     MOVE 4000 TO RECORDS-IN-FILE (1).                            SQ2144.2
043900     MOVE "SQ" TO XFILE-ORGANIZATION (1).                         SQ2144.2
044000     MOVE "S" TO XLABEL-TYPE (1).                                 SQ2144.2
044100     MOVE 1 TO XRECORD-NUMBER (1).                                SQ2144.2
044200     MOVE 3 TO ODO-NUMBER (1).                                    SQ2144.2
044300     MOVE FILE-RECORD-INFO-P1-120 (1) TO ODO-RECORD.              SQ2144.2
044400     PERFORM WRITE-INIT-GF-01.                                    SQ2144.2
044500     MOVE 3 TO DOI-DU-01V00.                                      SQ2144.2
044600     WRITE SQ-FS1R1-F-G-140 FROM ODO-RECORD.                      SQ2144.2
044700     MOVE 2 TO XRECORD-NUMBER (1).                                SQ2144.2
044800     MOVE 7 TO ODO-NUMBER (1).                                    SQ2144.2
044900     MOVE FILE-RECORD-INFO-P1-120 (1) TO ODO-RECORD.              SQ2144.2
045000     PERFORM WRITE-INIT-GF-01.                                    SQ2144.2
045100     MOVE 7 TO DOI-DU-01V00.                                      SQ2144.2
045200     WRITE SQ-FS1R1-F-G-140 FROM ODO-RECORD.                      SQ2144.2
045300     PERFORM WRITE-SQ-FS1-PARA2 VARYING ODO-IX FROM 3 BY 1        SQ2144.2
045400         UNTIL ODO-IX IS GREATER THAN 4000.                       SQ2144.2
045500     GO TO WRITE-SQ-FS1-PARA3.                                    SQ2144.2
045600 WRITE-SQ-FS1-PARA2.                                              SQ2144.2
045700     SET XRECORD-NUMBER (1) TO ODO-IX.                            SQ2144.2
045800     MOVE 9 TO ODO-NUMBER (1).                                    SQ2144.2
045900     MOVE FILE-RECORD-INFO-P1-120 (1) TO ODO-RECORD.              SQ2144.2
046000     PERFORM WRITE-INIT-GF-01.                                    SQ2144.2
046100     WRITE SQ-FS1R1-F-G-140 FROM ODO-RECORD.                      SQ2144.2
046200 WRITE-SQ-FS1-PARA3.                                              SQ2144.2
046300     CLOSE SQ-FS1.                                                SQ2144.2
046400     OPEN INPUT SQ-FS1.                                           SQ2144.2
046500     MOVE "OCCURS DEPENDING ON" TO FEATURE.                       SQ2144.2
046600 END-OF-WRITE-SQ-FS1 SECTION.                                     SQ2144.2
046700 WRITE-TEST-GF-01.                                                SQ2144.2
046800     MOVE SPACES TO SQ-FS1R1-F-G-140.                             SQ2144.2
046900     READ SQ-FS1 AT END GO TO WRITE-DELETE-GF-01.                 SQ2144.2
047000     IF FS1R1-XN-13     IS EQUAL TO "3 ACTIVE: 123" AND           SQ2144.2
047100        FS1R1-XN-6  IS NOT EQUAL TO "456789"                      SQ2144.2
047200         PERFORM PASS                                             SQ2144.2
047300         ELSE                                                     SQ2144.2
047400         PERFORM FAIL                                             SQ2144.2
047500       MOVE "VI-26 OCCURS & VII-44 READ / VII-52 WRITE" TO RE-MARKSQ2144.2
047600         MOVE "3 ACTIVE: 123" TO CORRECT-A                        SQ2144.2
047700         MOVE FS1R1-XN-20 TO COMPUTED-A.                          SQ2144.2
047800     GO TO WRITE-WRITE-GF-01.                                     SQ2144.2
047900 WRITE-DELETE-GF-01.                                              SQ2144.2
048000     PERFORM DE-LETE.                                             SQ2144.2
048100 WRITE-WRITE-GF-01.                                               SQ2144.2
048200     MOVE "WRITE-TEST-GF-01" TO PAR-NAME.                         SQ2144.2
048300     MOVE "WRITE FROM PARTIAL ODO" TO RE-MARK.                    SQ2144.2
048400     PERFORM PRINT-DETAIL.                                        SQ2144.2
048500 READ-TEST-GF-01.                                                 SQ2144.2
048600     MOVE SPACES TO SQ-FS1R1-F-G-140.                             SQ2144.2
048700     MOVE 9 TO DOI-DU-01V00.                                      SQ2144.2
048800     MOVE SPACES TO ODO-RECORD.                                   SQ2144.2
048900     MOVE 9 TO DOI-DU-01V00.                                      SQ2144.2
049000     READ SQ-FS1 INTO ODO-RECORD AT END GO TO READ-DELETE-GF-01.  SQ2144.2
049100     MOVE 9 TO DOI-DU-01V00.                                      SQ2144.2
049200     MOVE ODO-GRP-00009 TO WRK-GRP-00009.                         SQ2144.2
049300     IF ODO-XN-00007 IS EQUAL TO "1234567"  AND                   SQ2144.2
049400        ODO-XN-00002 IS NOT EQUAL TO "89"                         SQ2144.2
049500         PERFORM PASS                                             SQ2144.2
049600         ELSE                                                     SQ2144.2
049700       MOVE "VI-26 OCCURS & VII-44 READ / VII-52 WRITE" TO RE-MARKSQ2144.2
049800         PERFORM FAIL                                             SQ2144.2
049900         MOVE "1234567" TO CORRECT-A                              SQ2144.2
050000         MOVE ODO-GRP-00009 TO COMPUTED-A.                        SQ2144.2
050100     GO TO READ-WRITE-GF-01.                                      SQ2144.2
050200 READ-DELETE-GF-01.                                               SQ2144.2
050300     PERFORM DE-LETE.                                             SQ2144.2
050400 READ-WRITE-GF-01.                                                SQ2144.2
050500     MOVE "READ-TEST-GF-01" TO PAR-NAME.                          SQ2144.2
050600     MOVE "READ PARTIAL ODO INTO FULL ODO" TO RE-MARK.            SQ2144.2
050700     PERFORM PRINT-DETAIL.                                        SQ2144.2
050800 WRITE-TEST-GF-02.                                                SQ2144.2
050900     MOVE SPACES TO SQ-FS1R1-F-G-140.                             SQ2144.2
051000     READ SQ-FS1 AT END GO TO WRITE-DELETE-GF-02.                 SQ2144.2
051100     IF FS1R1-XN-20     IS EQUAL TO "9 ACTIVE: 123456789"         SQ2144.2
051200         PERFORM PASS                                             SQ2144.2
051300         ELSE                                                     SQ2144.2
051400       MOVE "VI-26 OCCURS & VII-44 READ / VII-52 WRITE" TO RE-MARKSQ2144.2
051500         PERFORM FAIL                                             SQ2144.2
051600         MOVE "9 ACTIVE: 123456789" TO CORRECT-A                  SQ2144.2
051700         MOVE FS1R1-XN-20     TO COMPUTED-A.                      SQ2144.2
051800     GO TO WRITE-WRITE-GF-02.                                     SQ2144.2
051900 WRITE-DELETE-GF-02.                                              SQ2144.2
052000     PERFORM DE-LETE.                                             SQ2144.2
052100 WRITE-WRITE-GF-02.                                               SQ2144.2
052200     MOVE "WRITE-TEST-GF-02" TO PAR-NAME.                         SQ2144.2
052300     MOVE "WRITE FROM FULL ODO" TO RE-MARK.                       SQ2144.2
052400     PERFORM PRINT-DETAIL.                                        SQ2144.2
052500 READ-TEST-GF-02.                                                 SQ2144.2
052600     MOVE 9 TO DOI-DU-01V00.                                      SQ2144.2
052700     MOVE SPACES TO SQ-FS1R1-F-G-140   ODO-RECORD.                SQ2144.2
052800     MOVE 9 TO DOI-DU-01V00.                                      SQ2144.2
052900     READ SQ-FS1 INTO ODO-RECORD AT END GO TO READ-DELETE-GF-02.  SQ2144.2
053000     IF GRP-ODO IS EQUAL TO "9 ACTIVE: 123456789"                 SQ2144.2
053100         PERFORM PASS                                             SQ2144.2
053200         ELSE                                                     SQ2144.2
053300       MOVE "VI-26 OCCURS & VII-44 READ / VII-52 WRITE" TO RE-MARKSQ2144.2
053400         PERFORM FAIL                                             SQ2144.2
053500         MOVE "9 ACTIVE: 123456789" TO CORRECT-A                  SQ2144.2
053600         MOVE GRP-ODO TO COMPUTED-A.                              SQ2144.2
053700     GO TO READ-WRITE-GF-02.                                      SQ2144.2
053800 READ-DELETE-GF-02.                                               SQ2144.2
053900     PERFORM DE-LETE.                                             SQ2144.2
054000 READ-WRITE-GF-02.                                                SQ2144.2
054100     MOVE "READ-TEST-GF-02" TO PAR-NAME.                          SQ2144.2
054200     MOVE "READ FULL ODO INTO FULL ODO" TO RE-MARK.               SQ2144.2
054300     PERFORM PRINT-DETAIL.                                        SQ2144.2
054400 READ-TEST-GF-03.                                                 SQ2144.2
054500     MOVE 9 TO DOI-DU-01V00.                                      SQ2144.2
054600     MOVE SPACES TO SQ-FS1R1-F-G-140   ODO-RECORD.                SQ2144.2
054700     MOVE 5 TO DOI-DU-01V00.                                      SQ2144.2
054800     READ SQ-FS1 INTO ODO-RECORD AT END GO TO READ-DELETE-GF-03.  SQ2144.2
054900     MOVE 9 TO DOI-DU-01V00.                                      SQ2144.2
055000     MOVE ODO-GRP-00009 TO WRK-GRP-00009.                         SQ2144.2
055100     IF ODO-XN-00005 IS EQUAL TO "12345"  AND                     SQ2144.2
055200        ODO-XN-00004 IS EQUAL TO "6789"                           SQ2144.2
055300         PERFORM PASS                                             SQ2144.2
055400         ELSE                                                     SQ2144.2
055500       MOVE "VI-28 OCCURS & VII-44 READ / VII-52 WRITE" TO RE-MARKSQ2144.2
055600         PERFORM FAIL                                             SQ2144.2
055700         MOVE "123456789" TO CORRECT-A                            SQ2144.2
055800         MOVE ODO-GRP-00009 TO COMPUTED-A.                        SQ2144.2
055900     GO TO READ-WRITE-GF-03.                                      SQ2144.2
056000 READ-DELETE-GF-03.                                               SQ2144.2
056100     PERFORM DE-LETE.                                             SQ2144.2
056200 READ-WRITE-GF-03.                                                SQ2144.2
056300     MOVE "READ-TEST-GF-03" TO PAR-NAME.                          SQ2144.2
056400     MOVE "READ FULL ODO INTO PARTIAL ODO" TO RE-MARK.            SQ2144.2
056500     PERFORM PRINT-DETAIL.                                        SQ2144.2
056600 END-OF-SQ214A-TESTS.                                             SQ2144.2
056700     CLOSE SQ-FS1.                                                SQ2144.2
056800 CCVS-EXIT SECTION.                                               SQ2144.2
056900 CCVS-999999.                                                     SQ2144.2
057000     GO TO CLOSE-FILES.                                           SQ2144.2
      *END-OF,SQ214A                                                                  
