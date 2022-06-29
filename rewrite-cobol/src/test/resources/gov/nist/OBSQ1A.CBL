      *HEADER,COBOL,OBSQ1A                                                            
000100 IDENTIFICATION DIVISION.                                         OBSQ14.2
000200 PROGRAM-ID.                                                      OBSQ14.2
000300     OBSQ1A.                                                      OBSQ14.2
000400****************************************************************  OBSQ14.2
000500*                                                              *  OBSQ14.2
000600*    VALIDATION FOR:-                                          *  OBSQ14.2
000700*    " HIGH       ".                                              OBSQ14.2
000800*                                                              *  OBSQ14.2
000900*    CREATION DATE     /     VALIDATION DATE                   *  OBSQ14.2
001000*    "4.2 ".                                                      OBSQ14.2
001100*                                                              *  OBSQ14.2
001200*        THE ROUTINE OBSQ1A CREATES A TAPE FILE WHICH HAS FIXED   OBSQ14.2
001300*    LENGTH RECORDS.  THE FILE IS THEN CLOSED AND OPENED AS AN    OBSQ14.2
001400*    INPUT FILE.  THE FILE IS READ AND FIELDS IN THE INPUT RECORDSOBSQ14.2
001500*    ARE COMPARED TO THE VALUES WRITTEN TO ENSURE THAT THE RECORDSOBSQ14.2
001600*    WERE PROCESSED CORRECTLY.  THE FILE IS CLOSED AND OPENED     OBSQ14.2
001700*    AGAIN AS AN INPUT FILE.  FOUR READ FORMAT OPTIONS ARE USED   OBSQ14.2
001800*    TO READ THE FILE AND FIELDS IN THE RECORDS ARE VERIFIED.     OBSQ14.2
001900*    THE OPEN, CLOSE, READ, AND WRITE STATEMENTS ARE TESTED FOR   OBSQ14.2
002000*    LEVEL ONE FEATURES.                                          OBSQ14.2
002100*                                                                 OBSQ14.2
002200*    THIS ROUTINE TESTS THE OBSOLETE LANGUAGE FEATURE "VALUE OF". OBSQ14.2
002300*    IT IS IDENTICAL WITH THE OLD (74) TEST PROGRAM SQ102.        OBSQ14.2
002400*                                                                 OBSQ14.2
002500*    USED X-CARDS:                                                OBSQ14.2
002600*         XXXXX001                                                OBSQ14.2
002700*         XXXXX055                                                OBSQ14.2
002800*     P   XXXXX062                                                OBSQ14.2
002900*         XXXXX082                                                OBSQ14.2
003000*         XXXXX083                                                OBSQ14.2
003100*     C   XXXXX084                                                OBSQ14.2
003200*                                                                 OBSQ14.2
003300*                                                                 OBSQ14.2
003400*     OBSOLETE FEATURES WHICH ARE TESTED:                         OBSQ14.2
003500*                                                                 OBSQ14.2
003600*         VALUE OF                                                OBSQ14.2
003700*         XXXXX074                                                OBSQ14.2
003800*         IS                                                      OBSQ14.2
003900*         XXXXX075                                                OBSQ14.2
004000*         XXXXX069                                                OBSQ14.2
004100*                                                                 OBSQ14.2
004200*         DATA RECORDS ARE  ...           DATA RECORD  ...        OBSQ14.2
004300*         LABEL RECORDS ARE ...           LABEL RECORD ...        OBSQ14.2
004400 ENVIRONMENT DIVISION.                                            OBSQ14.2
004500 CONFIGURATION SECTION.                                           OBSQ14.2
004600 SOURCE-COMPUTER.                                                 OBSQ14.2
004700     XXXXX082.                                                    OBSQ14.2
004800 OBJECT-COMPUTER.                                                 OBSQ14.2
004900     XXXXX083.                                                    OBSQ14.2
005000 INPUT-OUTPUT SECTION.                                            OBSQ14.2
005100 FILE-CONTROL.                                                    OBSQ14.2
005200*    SELECT RAW-DATA   ASSIGN TO                                  OBSQ14.2
005300*    XXXXX062                                                     OBSQ14.2
005400*           ORGANIZATION IS INDEXED                               OBSQ14.2
005500*           ACCESS MODE IS RANDOM                                 OBSQ14.2
005600*           RECORD KEY IS RAW-DATA-KEY.                           OBSQ14.2
005700     SELECT PRINT-FILE ASSIGN TO                                  OBSQ14.2
005800     XXXXX055.                                                    OBSQ14.2
005900     SELECT SQ-FS1 ASSIGN TO                                      OBSQ14.2
006000     XXXXX001                                                     OBSQ14.2
006100     ORGANIZATION IS SEQUENTIAL                                   OBSQ14.2
006200     ACCESS MODE IS SEQUENTIAL.                                   OBSQ14.2
006300 DATA DIVISION.                                                   OBSQ14.2
006400 FILE SECTION.                                                    OBSQ14.2
006500*                                                                 OBSQ14.2
006600*FD  RAW-DATA                                                     OBSQ14.2
006700*       DATA RECORD IS RAW-DATA-SATZ                              OBSQ14.2
006800*       RECORD CONTAINS      50 CHARACTERS                        OBSQ14.2
006900*       LABEL RECORDS ARE STANDARD.                               OBSQ14.2
007000*                                                                 OBSQ14.2
007100*01  RAW-DATA-SATZ.                                               OBSQ14.2
007200*    05  RAW-DATA-KEY.                                            OBSQ14.2
007300*        10  C-2             PIC XX.                              OBSQ14.2
007400*        10  C-POS3          PIC X.                               OBSQ14.2
007500*        10  FILLER          PIC XX.                              OBSQ14.2
007600*    05  C-DATUM.                                                 OBSQ14.2
007700*        10  C-D-JJ          PIC XX.                              OBSQ14.2
007800*        10  C-D-MM          PIC XX.                              OBSQ14.2
007900*        10  C-D-DD          PIC XX.                              OBSQ14.2
008000*    05 C-DATE REDEFINES C-DATUM   PIC 9(6).                      OBSQ14.2
008100*    05 C-ZEIT.                                                   OBSQ14.2
008200*        10  C-T-HH          PIC XX.                              OBSQ14.2
008300*        10  C-T-MM          PIC XX.                              OBSQ14.2
008400*        10  C-T-SS          PIC XX.                              OBSQ14.2
008500*        10  C-T-HS          PIC XX.                              OBSQ14.2
008600*    05 C-TIME REDEFINES C-ZEIT  PIC 9(8).                        OBSQ14.2
008700*    05  C-NO-OF-TESTS       PIC 99.                              OBSQ14.2
008800*    05  C-OK                PIC 999.                             OBSQ14.2
008900*    05  C-ALL               PIC 999.                             OBSQ14.2
009000*    05  C-FAIL              PIC 999.                             OBSQ14.2
009100*    05  C-DELETED           PIC 999.                             OBSQ14.2
009200*    05  C-INSPECT           PIC 999.                             OBSQ14.2
009300*    05  C-NOTE              PIC X(13).                           OBSQ14.2
009400*    05  C-INDENT            PIC X.                               OBSQ14.2
009500 FD  PRINT-FILE.                                                  OBSQ14.2
009600 01  PRINT-REC PICTURE X(120).                                    OBSQ14.2
009700 01  DUMMY-RECORD PICTURE X(120).                                 OBSQ14.2
009800 FD  SQ-FS1                                                       OBSQ14.2
009900C    VALUE OF                                                     OBSQ14.2
010000C    XXXXX074                                                     OBSQ14.2
010100C    IS                                                           OBSQ14.2
010200C    XXXXX075                                                     OBSQ14.2
010300*    XXXXX069                                                     OBSQ14.2
010400     DATA RECORD    SQ-FS1R1-F-G-120                              OBSQ14.2
010500     LABEL RECORD STANDARD.                                       OBSQ14.2
010600 01  SQ-FS1R1-F-G-120.                                            OBSQ14.2
010700     02  FILLER PIC X(120).                                       OBSQ14.2
010800 WORKING-STORAGE SECTION.                                         OBSQ14.2
010900 01  WRK-CS-09V00 PIC S9(9) USAGE COMP VALUE ZERO.                OBSQ14.2
011000 01  RECORDS-IN-ERROR PIC S9(5) USAGE COMP VALUE ZERO.            OBSQ14.2
011100 01  ERROR-FLAG PIC 9 VALUE ZERO.                                 OBSQ14.2
011200 01  EOF-FLAG   PICTURE 9 VALUE ZERO.                             OBSQ14.2
011300 01  FILE-RECORD-INFORMATION-REC.                                 OBSQ14.2
011400     03 FILE-RECORD-INFO-SKELETON.                                OBSQ14.2
011500        05 FILLER                 PICTURE X(48)       VALUE       OBSQ14.2
011600             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  OBSQ14.2
011700        05 FILLER                 PICTURE X(46)       VALUE       OBSQ14.2
011800             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    OBSQ14.2
011900        05 FILLER                 PICTURE X(26)       VALUE       OBSQ14.2
012000             ",LFIL=000000,ORG=  ,LBLR= ".                        OBSQ14.2
012100        05 FILLER                 PICTURE X(37)       VALUE       OBSQ14.2
012200             ",RECKEY=                             ".             OBSQ14.2
012300        05 FILLER                 PICTURE X(38)       VALUE       OBSQ14.2
012400             ",ALTKEY1=                             ".            OBSQ14.2
012500        05 FILLER                 PICTURE X(38)       VALUE       OBSQ14.2
012600             ",ALTKEY2=                             ".            OBSQ14.2
012700        05 FILLER                 PICTURE X(7)        VALUE SPACE.OBSQ14.2
012800     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              OBSQ14.2
012900        05 FILE-RECORD-INFO-P1-120.                               OBSQ14.2
013000           07 FILLER              PIC X(5).                       OBSQ14.2
013100           07 XFILE-NAME           PIC X(6).                      OBSQ14.2
013200           07 FILLER              PIC X(8).                       OBSQ14.2
013300           07 XRECORD-NAME         PIC X(6).                      OBSQ14.2
013400           07 FILLER              PIC X(1).                       OBSQ14.2
013500           07 REELUNIT-NUMBER     PIC 9(1).                       OBSQ14.2
013600           07 FILLER              PIC X(7).                       OBSQ14.2
013700           07 XRECORD-NUMBER       PIC 9(6).                      OBSQ14.2
013800           07 FILLER              PIC X(6).                       OBSQ14.2
013900           07 UPDATE-NUMBER       PIC 9(2).                       OBSQ14.2
014000           07 FILLER              PIC X(5).                       OBSQ14.2
014100           07 ODO-NUMBER          PIC 9(4).                       OBSQ14.2
014200           07 FILLER              PIC X(5).                       OBSQ14.2
014300           07 XPROGRAM-NAME        PIC X(5).                      OBSQ14.2
014400           07 FILLER              PIC X(7).                       OBSQ14.2
014500           07 XRECORD-LENGTH       PIC 9(6).                      OBSQ14.2
014600           07 FILLER              PIC X(7).                       OBSQ14.2
014700           07 CHARS-OR-RECORDS    PIC X(2).                       OBSQ14.2
014800           07 FILLER              PIC X(1).                       OBSQ14.2
014900           07 XBLOCK-SIZE          PIC 9(4).                      OBSQ14.2
015000           07 FILLER              PIC X(6).                       OBSQ14.2
015100           07 RECORDS-IN-FILE     PIC 9(6).                       OBSQ14.2
015200           07 FILLER              PIC X(5).                       OBSQ14.2
015300           07 XFILE-ORGANIZATION   PIC X(2).                      OBSQ14.2
015400           07 FILLER              PIC X(6).                       OBSQ14.2
015500           07 XLABEL-TYPE          PIC X(1).                      OBSQ14.2
015600        05 FILE-RECORD-INFO-P121-240.                             OBSQ14.2
015700           07 FILLER              PIC X(8).                       OBSQ14.2
015800           07 XRECORD-KEY          PIC X(29).                     OBSQ14.2
015900           07 FILLER              PIC X(9).                       OBSQ14.2
016000           07 ALTERNATE-KEY1      PIC X(29).                      OBSQ14.2
016100           07 FILLER              PIC X(9).                       OBSQ14.2
016200           07 ALTERNATE-KEY2      PIC X(29).                      OBSQ14.2
016300           07 FILLER              PIC X(7).                       OBSQ14.2
016400 01  TEST-RESULTS.                                                OBSQ14.2
016500     02 FILLER                    PICTURE X VALUE SPACE.          OBSQ14.2
016600     02 FEATURE                   PICTURE X(20) VALUE SPACE.      OBSQ14.2
016700     02 FILLER                    PICTURE X VALUE SPACE.          OBSQ14.2
016800     02 P-OR-F                    PICTURE X(5) VALUE SPACE.       OBSQ14.2
016900     02 FILLER                    PICTURE X  VALUE SPACE.         OBSQ14.2
017000     02  PAR-NAME.                                                OBSQ14.2
017100       03 FILLER PICTURE X(12) VALUE SPACE.                       OBSQ14.2
017200       03  PARDOT-X PICTURE X  VALUE SPACE.                       OBSQ14.2
017300       03 DOTVALUE PICTURE 99  VALUE ZERO.                        OBSQ14.2
017400       03 FILLER PIC X(5) VALUE SPACE.                            OBSQ14.2
017500     02 FILLER PIC X(10) VALUE SPACE.                             OBSQ14.2
017600     02 RE-MARK PIC X(61).                                        OBSQ14.2
017700 01  TEST-COMPUTED.                                               OBSQ14.2
017800     02 FILLER PIC X(30) VALUE SPACE.                             OBSQ14.2
017900     02 FILLER PIC X(17) VALUE "       COMPUTED=".                OBSQ14.2
018000     02 COMPUTED-X.                                               OBSQ14.2
018100     03 COMPUTED-A                PICTURE X(20) VALUE SPACE.      OBSQ14.2
018200     03 COMPUTED-N REDEFINES COMPUTED-A PICTURE -9(9).9(9).       OBSQ14.2
018300     03 COMPUTED-0V18 REDEFINES COMPUTED-A  PICTURE -.9(18).      OBSQ14.2
018400     03 COMPUTED-4V14 REDEFINES COMPUTED-A  PICTURE -9(4).9(14).  OBSQ14.2
018500     03 COMPUTED-14V4 REDEFINES COMPUTED-A  PICTURE -9(14).9(4).  OBSQ14.2
018600     03       CM-18V0 REDEFINES COMPUTED-A.                       OBSQ14.2
018700         04 COMPUTED-18V0                   PICTURE -9(18).       OBSQ14.2
018800         04 FILLER                          PICTURE X.            OBSQ14.2
018900     03 FILLER PIC X(50) VALUE SPACE.                             OBSQ14.2
019000 01  TEST-CORRECT.                                                OBSQ14.2
019100     02 FILLER PIC X(30) VALUE SPACE.                             OBSQ14.2
019200     02 FILLER PIC X(17) VALUE "       CORRECT =".                OBSQ14.2
019300     02 CORRECT-X.                                                OBSQ14.2
019400     03 CORRECT-A                 PICTURE X(20) VALUE SPACE.      OBSQ14.2
019500     03 CORRECT-N REDEFINES CORRECT-A PICTURE -9(9).9(9).         OBSQ14.2
019600     03 CORRECT-0V18 REDEFINES CORRECT-A    PICTURE -.9(18).      OBSQ14.2
019700     03 CORRECT-4V14 REDEFINES CORRECT-A    PICTURE -9(4).9(14).  OBSQ14.2
019800     03 CORRECT-14V4 REDEFINES CORRECT-A    PICTURE -9(14).9(4).  OBSQ14.2
019900     03      CR-18V0 REDEFINES CORRECT-A.                         OBSQ14.2
020000         04 CORRECT-18V0                    PICTURE -9(18).       OBSQ14.2
020100         04 FILLER                          PICTURE X.            OBSQ14.2
020200     03 FILLER PIC X(50) VALUE SPACE.                             OBSQ14.2
020300 01  CCVS-C-1.                                                    OBSQ14.2
020400     02 FILLER PICTURE IS X(99) VALUE IS " FEATURE              PAOBSQ14.2
020500-    "SS  PARAGRAPH-NAME                                          OBSQ14.2
020600-    "        REMARKS".                                           OBSQ14.2
020700     02 FILLER PICTURE IS X(20) VALUE IS SPACE.                   OBSQ14.2
020800 01  CCVS-C-2.                                                    OBSQ14.2
020900     02 FILLER PICTURE IS X VALUE IS SPACE.                       OBSQ14.2
021000     02 FILLER PICTURE IS X(6) VALUE IS "TESTED".                 OBSQ14.2
021100     02 FILLER PICTURE IS X(15) VALUE IS SPACE.                   OBSQ14.2
021200     02 FILLER PICTURE IS X(4) VALUE IS "FAIL".                   OBSQ14.2
021300     02 FILLER PICTURE IS X(94) VALUE IS SPACE.                   OBSQ14.2
021400 01  REC-SKL-SUB PICTURE 9(2) VALUE ZERO.                         OBSQ14.2
021500 01  REC-CT PICTURE 99 VALUE ZERO.                                OBSQ14.2
021600 01  DELETE-CNT                   PICTURE 999  VALUE ZERO.        OBSQ14.2
021700 01  ERROR-COUNTER PICTURE IS 999 VALUE IS ZERO.                  OBSQ14.2
021800 01  INSPECT-COUNTER PIC 999 VALUE ZERO.                          OBSQ14.2
021900 01  PASS-COUNTER PIC 999 VALUE ZERO.                             OBSQ14.2
022000 01  TOTAL-ERROR PIC 999 VALUE ZERO.                              OBSQ14.2
022100 01  ERROR-HOLD PIC 999 VALUE ZERO.                               OBSQ14.2
022200 01  DUMMY-HOLD PIC X(120) VALUE SPACE.                           OBSQ14.2
022300 01  RECORD-COUNT PIC 9(5) VALUE ZERO.                            OBSQ14.2
022400 01  CCVS-H-1.                                                    OBSQ14.2
022500     02  FILLER   PICTURE X(27)  VALUE SPACE.                     OBSQ14.2
022600     02 FILLER PICTURE X(67) VALUE                                OBSQ14.2
022700     " FEDERAL SOFTWARE TESTING CENTER COBOL COMPILER VALIDATION  OBSQ14.2
022800-    " SYSTEM".                                                   OBSQ14.2
022900     02  FILLER     PICTURE X(26)  VALUE SPACE.                   OBSQ14.2
023000 01  CCVS-H-2.                                                    OBSQ14.2
023100     02 FILLER PICTURE X(52) VALUE IS                             OBSQ14.2
023200     "CCVS74 FSTC COPY, NOT FOR DISTRIBUTION.".                   OBSQ14.2
023300     02 FILLER PICTURE IS X(19) VALUE IS "TEST RESULTS SET-  ".   OBSQ14.2
023400     02 TEST-ID PICTURE IS X(9).                                  OBSQ14.2
023500     02 FILLER PICTURE IS X(40) VALUE IS SPACE.                   OBSQ14.2
023600 01  CCVS-H-3.                                                    OBSQ14.2
023700     02  FILLER PICTURE X(34) VALUE                               OBSQ14.2
023800     " FOR OFFICIAL USE ONLY    ".                                OBSQ14.2
023900     02  FILLER PICTURE X(58) VALUE                               OBSQ14.2
024000     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".OBSQ14.2
024100     02  FILLER PICTURE X(28) VALUE                               OBSQ14.2
024200     "  COPYRIGHT   1974 ".                                       OBSQ14.2
024300 01  CCVS-E-1.                                                    OBSQ14.2
024400     02 FILLER PICTURE IS X(52) VALUE IS SPACE.                   OBSQ14.2
024500     02 FILLER PICTURE IS X(14) VALUE IS "END OF TEST-  ".        OBSQ14.2
024600     02 ID-AGAIN PICTURE IS X(9).                                 OBSQ14.2
024700     02 FILLER PICTURE X(45) VALUE IS                             OBSQ14.2
024800     " NTIS DISTRIBUTION COBOL 74".                               OBSQ14.2
024900 01  CCVS-E-2.                                                    OBSQ14.2
025000     02  FILLER                   PICTURE X(31)  VALUE            OBSQ14.2
025100     SPACE.                                                       OBSQ14.2
025200     02  FILLER                   PICTURE X(21)  VALUE SPACE.     OBSQ14.2
025300     02 CCVS-E-2-2.                                               OBSQ14.2
025400         03 ERROR-TOTAL PICTURE IS XXX VALUE IS SPACE.            OBSQ14.2
025500         03 FILLER PICTURE IS X VALUE IS SPACE.                   OBSQ14.2
025600         03 ENDER-DESC PIC X(46) VALUE "ERRORS ENCOUNTERED".      OBSQ14.2
025700 01  CCVS-E-3.                                                    OBSQ14.2
025800     02  FILLER PICTURE X(22) VALUE                               OBSQ14.2
025900     " FOR OFFICIAL USE ONLY".                                    OBSQ14.2
026000     02  FILLER PICTURE X(12) VALUE SPACE.                        OBSQ14.2
026100     02  FILLER PICTURE X(58) VALUE                               OBSQ14.2
026200     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".OBSQ14.2
026300     02  FILLER PICTURE X(13) VALUE SPACE.                        OBSQ14.2
026400     02 FILLER PIC X(15) VALUE " COPYRIGHT 1974".                 OBSQ14.2
026500 01  CCVS-E-4.                                                    OBSQ14.2
026600     02 CCVS-E-4-1 PIC XXX VALUE SPACE.                           OBSQ14.2
026700     02 FILLER PIC XXXX VALUE " OF ".                             OBSQ14.2
026800     02 CCVS-E-4-2 PIC XXX VALUE SPACE.                           OBSQ14.2
026900     02 FILLER PIC X(40) VALUE                                    OBSQ14.2
027000      "  TESTS WERE EXECUTED SUCCESSFULLY".                       OBSQ14.2
027100 01  XXINFO.                                                      OBSQ14.2
027200     02 FILLER PIC X(30) VALUE "        *** INFORMATION  ***".    OBSQ14.2
027300     02 INFO-TEXT.                                                OBSQ14.2
027400     04 FILLER PIC X(20) VALUE SPACE.                             OBSQ14.2
027500     04 XXCOMPUTED PIC X(20).                                     OBSQ14.2
027600     04 FILLER PIC X(5) VALUE SPACE.                              OBSQ14.2
027700     04 XXCORRECT PIC X(20).                                      OBSQ14.2
027800 01  HYPHEN-LINE.                                                 OBSQ14.2
027900     02 FILLER PICTURE IS X VALUE IS SPACE.                       OBSQ14.2
028000     02 FILLER PICTURE IS X(65) VALUE IS "************************OBSQ14.2
028100-    "*****************************************".                 OBSQ14.2
028200     02 FILLER PICTURE IS X(54) VALUE IS "************************OBSQ14.2
028300-    "******************************".                            OBSQ14.2
028400 01  CCVS-PGM-ID PIC X(6) VALUE                                   OBSQ14.2
028500     "OBSQ1A".                                                    OBSQ14.2
028600 PROCEDURE DIVISION.                                              OBSQ14.2
028700 CCVS1 SECTION.                                                   OBSQ14.2
028800 OPEN-FILES.                                                      OBSQ14.2
028900*    OPEN I-O RAW-DATA.                                           OBSQ14.2
029000*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            OBSQ14.2
029100*    READ RAW-DATA INVALID KEY GO TO END-E-1.                     OBSQ14.2
029200*    ADD 1 TO C-NO-OF-TESTS.                                      OBSQ14.2
029300*    ACCEPT C-DATE  FROM DATE.                                    OBSQ14.2
029400*    ACCEPT C-TIME  FROM TIME.                                    OBSQ14.2
029500*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-1.             OBSQ14.2
029600*END-E-1.                                                         OBSQ14.2
029700*    CLOSE RAW-DATA.                                              OBSQ14.2
029800     OPEN     OUTPUT PRINT-FILE.                                  OBSQ14.2
029900     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   OBSQ14.2
030000     MOVE    SPACE TO TEST-RESULTS.                               OBSQ14.2
030100     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             OBSQ14.2
030200     MOVE ZERO TO REC-SKL-SUB.                                    OBSQ14.2
030300     PERFORM CCVS-INIT-FILE 9 TIMES.                              OBSQ14.2
030400 CCVS-INIT-FILE.                                                  OBSQ14.2
030500     ADD 1 TO REC-SKL-SUB.                                        OBSQ14.2
030600     MOVE FILE-RECORD-INFO-SKELETON TO                            OBSQ14.2
030700                  FILE-RECORD-INFO (REC-SKL-SUB).                 OBSQ14.2
030800 CCVS-INIT-EXIT.                                                  OBSQ14.2
030900     GO TO CCVS1-EXIT.                                            OBSQ14.2
031000 CLOSE-FILES.                                                     OBSQ14.2
031100     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   OBSQ14.2
031200*    OPEN I-O RAW-DATA.                                           OBSQ14.2
031300*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            OBSQ14.2
031400*    READ RAW-DATA INVALID KEY GO TO END-E-2.                     OBSQ14.2
031500*    MOVE PASS-COUNTER TO C-OK.                                   OBSQ14.2
031600*    MOVE ERROR-HOLD   TO C-ALL.                                  OBSQ14.2
031700*    MOVE ERROR-COUNTER TO C-FAIL.                                OBSQ14.2
031800*    MOVE DELETE-CNT TO C-DELETED.                                OBSQ14.2
031900*    MOVE INSPECT-COUNTER TO C-INSPECT.                           OBSQ14.2
032000*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-2.             OBSQ14.2
032100*END-E-2.                                                         OBSQ14.2
032200*    CLOSE RAW-DATA.                                              OBSQ14.2
032300 TERMINATE-CCVS.                                                  OBSQ14.2
032400*    EXIT PROGRAM.                                                OBSQ14.2
032500*TERMINATE-CALL.                                                  OBSQ14.2
032600     STOP     RUN.                                                OBSQ14.2
032700 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         OBSQ14.2
032800 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           OBSQ14.2
032900 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          OBSQ14.2
033000 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-CNT.          OBSQ14.2
033100     MOVE "****TEST DELETED****" TO RE-MARK.                      OBSQ14.2
033200 PRINT-DETAIL.                                                    OBSQ14.2
033300     IF REC-CT NOT EQUAL TO ZERO                                  OBSQ14.2
033400             MOVE "." TO PARDOT-X                                 OBSQ14.2
033500             MOVE REC-CT TO DOTVALUE.                             OBSQ14.2
033600     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      OBSQ14.2
033700     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               OBSQ14.2
033800        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 OBSQ14.2
033900          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 OBSQ14.2
034000     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              OBSQ14.2
034100     MOVE SPACE TO CORRECT-X.                                     OBSQ14.2
034200     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         OBSQ14.2
034300     MOVE     SPACE TO RE-MARK.                                   OBSQ14.2
034400 HEAD-ROUTINE.                                                    OBSQ14.2
034500     MOVE CCVS-H-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   OBSQ14.2
034600     MOVE CCVS-H-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.   OBSQ14.2
034700     MOVE CCVS-H-3 TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.   OBSQ14.2
034800 COLUMN-NAMES-ROUTINE.                                            OBSQ14.2
034900     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           OBSQ14.2
035000     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   OBSQ14.2
035100     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        OBSQ14.2
035200 END-ROUTINE.                                                     OBSQ14.2
035300     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.OBSQ14.2
035400 END-RTN-EXIT.                                                    OBSQ14.2
035500     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   OBSQ14.2
035600 END-ROUTINE-1.                                                   OBSQ14.2
035700      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      OBSQ14.2
035800      ERROR-HOLD. ADD DELETE-CNT TO ERROR-HOLD.                   OBSQ14.2
035900      ADD PASS-COUNTER TO ERROR-HOLD.                             OBSQ14.2
036000*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   OBSQ14.2
036100      MOVE PASS-COUNTER TO CCVS-E-4-1.                            OBSQ14.2
036200      MOVE ERROR-HOLD TO CCVS-E-4-2.                              OBSQ14.2
036300      MOVE CCVS-E-4 TO CCVS-E-2-2.                                OBSQ14.2
036400      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           OBSQ14.2
036500  END-ROUTINE-12.                                                 OBSQ14.2
036600      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        OBSQ14.2
036700     IF       ERROR-COUNTER IS EQUAL TO ZERO                      OBSQ14.2
036800         MOVE "NO " TO ERROR-TOTAL                                OBSQ14.2
036900         ELSE                                                     OBSQ14.2
037000         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       OBSQ14.2
037100     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           OBSQ14.2
037200     PERFORM WRITE-LINE.                                          OBSQ14.2
037300 END-ROUTINE-13.                                                  OBSQ14.2
037400     IF DELETE-CNT IS EQUAL TO ZERO                               OBSQ14.2
037500         MOVE "NO " TO ERROR-TOTAL  ELSE                          OBSQ14.2
037600         MOVE DELETE-CNT TO ERROR-TOTAL.                          OBSQ14.2
037700     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   OBSQ14.2
037800     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           OBSQ14.2
037900      IF   INSPECT-COUNTER EQUAL TO ZERO                          OBSQ14.2
038000          MOVE "NO " TO ERROR-TOTAL                               OBSQ14.2
038100      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   OBSQ14.2
038200      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            OBSQ14.2
038300      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          OBSQ14.2
038400     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           OBSQ14.2
038500 WRITE-LINE.                                                      OBSQ14.2
038600     ADD 1 TO RECORD-COUNT.                                       OBSQ14.2
038700*    IF RECORD-COUNT GREATER 50                                   OBSQ14.2
038800*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          OBSQ14.2
038900*        MOVE SPACE TO DUMMY-RECORD                               OBSQ14.2
039000*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  OBSQ14.2
039100*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             OBSQ14.2
039200*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     OBSQ14.2
039300*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          OBSQ14.2
039400*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          OBSQ14.2
039500*        MOVE ZERO TO RECORD-COUNT.                               OBSQ14.2
039600     PERFORM WRT-LN.                                              OBSQ14.2
039700 WRT-LN.                                                          OBSQ14.2
039800     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               OBSQ14.2
039900     MOVE SPACE TO DUMMY-RECORD.                                  OBSQ14.2
040000 BLANK-LINE-PRINT.                                                OBSQ14.2
040100     PERFORM WRT-LN.                                              OBSQ14.2
040200 FAIL-ROUTINE.                                                    OBSQ14.2
040300     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.   OBSQ14.2
040400     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.    OBSQ14.2
040500     MOVE "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.    OBSQ14.2
040600     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     OBSQ14.2
040700     GO TO FAIL-ROUTINE-EX.                                       OBSQ14.2
040800 FAIL-ROUTINE-WRITE.                                              OBSQ14.2
040900     MOVE TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE           OBSQ14.2
041000     MOVE TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES.   OBSQ14.2
041100 FAIL-ROUTINE-EX. EXIT.                                           OBSQ14.2
041200 BAIL-OUT.                                                        OBSQ14.2
041300     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.       OBSQ14.2
041400     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.               OBSQ14.2
041500 BAIL-OUT-WRITE.                                                  OBSQ14.2
041600     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  OBSQ14.2
041700     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     OBSQ14.2
041800 BAIL-OUT-EX. EXIT.                                               OBSQ14.2
041900 CCVS1-EXIT.                                                      OBSQ14.2
042000     EXIT.                                                        OBSQ14.2
042100 SECT-OBSQ1A-0001 SECTION.                                        OBSQ14.2
042200 SEQ-INIT-001.                                                    OBSQ14.2
042300     MOVE "SQ-FS1" TO XFILE-NAME (1).                             OBSQ14.2
042400     MOVE "R1-F-G" TO XRECORD-NAME (1).                           OBSQ14.2
042500     MOVE CCVS-PGM-ID TO XPROGRAM-NAME (1).                       OBSQ14.2
042600     MOVE 000120 TO XRECORD-LENGTH (1).                           OBSQ14.2
042700     MOVE "RC"   TO CHARS-OR-RECORDS (1).                         OBSQ14.2
042800     MOVE 0001   TO XBLOCK-SIZE (1).                              OBSQ14.2
042900     MOVE 000750 TO RECORDS-IN-FILE (1).                          OBSQ14.2
043000     MOVE "SQ"   TO XFILE-ORGANIZATION (1).                       OBSQ14.2
043100     MOVE "S" TO XLABEL-TYPE (1).                                 OBSQ14.2
043200     MOVE 000001 TO XRECORD-NUMBER (1).                           OBSQ14.2
043300     OPEN OUTPUT SQ-FS1.                                          OBSQ14.2
043400 SEQ-TEST-001.                                                    OBSQ14.2
043500     MOVE FILE-RECORD-INFO-P1-120 (1)  TO SQ-FS1R1-F-G-120.       OBSQ14.2
043600     WRITE SQ-FS1R1-F-G-120.                                      OBSQ14.2
043700     IF XRECORD-NUMBER (1) EQUAL TO 750                           OBSQ14.2
043800         GO TO SEQ-WRITE-001.                                     OBSQ14.2
043900     ADD 1 TO XRECORD-NUMBER (1).                                 OBSQ14.2
044000     GO TO SEQ-TEST-001.                                          OBSQ14.2
044100 SEQ-WRITE-001.                                                   OBSQ14.2
044200     MOVE "CREATE FILE SQ-FS1" TO FEATURE.                        OBSQ14.2
044300     MOVE "SEQ-TEST-001" TO PAR-NAME.                             OBSQ14.2
044400     MOVE "FILE CREATED, RECS =" TO COMPUTED-A.                   OBSQ14.2
044500     MOVE  XRECORD-NUMBER (1) TO CORRECT-18V0.                    OBSQ14.2
044600     PERFORM PRINT-DETAIL.                                        OBSQ14.2
044700     CLOSE SQ-FS1.                                                OBSQ14.2
044800*        A SEQUENTIAL TAPE FILE WITH 120 CHARACTER RECORDS        OBSQ14.2
044900*    HAS BEEN CREATED. THE FILE CONTAINS 750 RECORDS.             OBSQ14.2
045000 SEQ-INIT-002.                                                    OBSQ14.2
045100     MOVE ZERO TO WRK-CS-09V00.                                   OBSQ14.2
045200*        THIS TEST READS AND CHECKS THE FILE CREATED IN           OBSQ14.2
045300*    SEQ-TEST-001.                                                OBSQ14.2
045400     OPEN INPUT SQ-FS1.                                           OBSQ14.2
045500 SEQ-TEST-002.                                                    OBSQ14.2
045600     READ SQ-FS1                                                  OBSQ14.2
045700          AT END GO TO SEQ-TEST-002-1.                            OBSQ14.2
045800     MOVE   SQ-FS1R1-F-G-120 TO FILE-RECORD-INFO-P1-120 (1).      OBSQ14.2
045900     ADD 1 TO WRK-CS-09V00.                                       OBSQ14.2
046000     IF WRK-CS-09V00 GREATER THAN 750                             OBSQ14.2
046100        MOVE "MORE THAN 750 RECORDS" TO RE-MARK                   OBSQ14.2
046200        GO TO SEQ-FAIL-002.                                       OBSQ14.2
046300     IF WRK-CS-09V00 NOT EQUAL TO XRECORD-NUMBER (1)              OBSQ14.2
046400         ADD 1 TO RECORDS-IN-ERROR                                OBSQ14.2
046500         GO TO SEQ-TEST-002.                                      OBSQ14.2
046600     IF XFILE-NAME (1) NOT EQUAL TO "SQ-FS1"                      OBSQ14.2
046700        ADD 1 TO RECORDS-IN-ERROR                                 OBSQ14.2
046800        GO TO SEQ-TEST-002.                                       OBSQ14.2
046900     IF XLABEL-TYPE (1) NOT EQUAL TO "S"                          OBSQ14.2
047000        ADD 1 TO RECORDS-IN-ERROR.                                OBSQ14.2
047100     GO TO SEQ-TEST-002.                                          OBSQ14.2
047200 SEQ-TEST-002-1.                                                  OBSQ14.2
047300     IF RECORDS-IN-ERROR EQUAL TO ZERO                            OBSQ14.2
047400         GO TO SEQ-PASS-002.                                      OBSQ14.2
047500     MOVE "ERRORS IN READING SQ-FS1" TO RE-MARK.                  OBSQ14.2
047600 SEQ-FAIL-002.                                                    OBSQ14.2
047700     MOVE "RECORDS IN ERROR =" TO COMPUTED-A.                     OBSQ14.2
047800     MOVE RECORDS-IN-ERROR TO CORRECT-18V0.                       OBSQ14.2
047900     PERFORM FAIL.                                                OBSQ14.2
048000     GO TO SEQ-WRITE-002.                                         OBSQ14.2
048100 SEQ-PASS-002.                                                    OBSQ14.2
048200     PERFORM PASS.                                                OBSQ14.2
048300     MOVE "FILE VERIFIED RECS =" TO COMPUTED-A.                   OBSQ14.2
048400     MOVE WRK-CS-09V00 TO CORRECT-18V0.                           OBSQ14.2
048500 SEQ-WRITE-002.                                                   OBSQ14.2
048600     MOVE "SEQ-TEST-002" TO PAR-NAME.                             OBSQ14.2
048700     MOVE "VERIFY FILE SQ-FS1" TO FEATURE.                        OBSQ14.2
048800     PERFORM PRINT-DETAIL.                                        OBSQ14.2
048900 SEQ-CLOSE-002.                                                   OBSQ14.2
049000     CLOSE SQ-FS1.                                                OBSQ14.2
049100 READ-INIT-01.                                                    OBSQ14.2
049200     MOVE ZERO TO WRK-CS-09V00.                                   OBSQ14.2
049300     MOVE ZERO TO RECORDS-IN-ERROR.                               OBSQ14.2
049400     OPEN INPUT   SQ-FS1.                                         OBSQ14.2
049500*            FOUR OPTIONS FOR THE READ STATEMENT ARE CHECKED      OBSQ14.2
049600*    IN THIS SERIES OF TESTS.                                     OBSQ14.2
049700     MOVE "LEV 1 READ STATEMENT" TO FEATURE.                      OBSQ14.2
049800     MOVE "READ...RECORD AT END ..." TO RE-MARK.                  OBSQ14.2
049900     MOVE "READ-TEST-01" TO PAR-NAME.                             OBSQ14.2
050000     MOVE ZERO TO ERROR-FLAG.                                     OBSQ14.2
050100 READ-TEST-01.                                                    OBSQ14.2
050200     READ SQ-FS1 RECORD AT END                                    OBSQ14.2
050300              MOVE "UNEXPECTED EOF" TO COMPUTED-A                 OBSQ14.2
050400              MOVE 1 TO EOF-FLAG                                  OBSQ14.2
050500              GO TO READ-FAIL-01.                                 OBSQ14.2
050600     PERFORM RECORD-CHECK.                                        OBSQ14.2
050700     IF WRK-CS-09V00 EQUAL TO 200                                 OBSQ14.2
050800              GO TO READ-TEST-01-1.                               OBSQ14.2
050900             GO TO READ-TEST-01.                                  OBSQ14.2
051000 RECORD-CHECK.                                                    OBSQ14.2
051100     MOVE SQ-FS1R1-F-G-120 TO FILE-RECORD-INFO-P1-120 (1).        OBSQ14.2
051200     ADD 1 TO WRK-CS-09V00.                                       OBSQ14.2
051300     IF WRK-CS-09V00 NOT EQUAL TO XRECORD-NUMBER (1)              OBSQ14.2
051400         ADD 1 TO RECORDS-IN-ERROR                                OBSQ14.2
051500         MOVE 1 TO ERROR-FLAG.                                    OBSQ14.2
051600 READ-TEST-01-1.                                                  OBSQ14.2
051700     IF ERROR-FLAG EQUAL TO ZERO                                  OBSQ14.2
051800         GO TO READ-PASS-01.                                      OBSQ14.2
051900     MOVE "ERROR IN RECORD(S)" TO COMPUTED-A.                     OBSQ14.2
052000 READ-FAIL-01.                                                    OBSQ14.2
052100     PERFORM FAIL.                                                OBSQ14.2
052200     GO TO READ-WRITE-01.                                         OBSQ14.2
052300 READ-PASS-01.                                                    OBSQ14.2
052400     PERFORM PASS.                                                OBSQ14.2
052500 READ-WRITE-01.                                                   OBSQ14.2
052600     PERFORM PRINT-DETAIL.                                        OBSQ14.2
052700 READ-INIT-02.                                                    OBSQ14.2
052800     IF EOF-FLAG EQUAL TO 1                                       OBSQ14.2
052900        GO TO SEQ-EOF-003.                                        OBSQ14.2
053000     MOVE ZERO TO ERROR-FLAG.                                     OBSQ14.2
053100     MOVE "READ...AT END..." TO RE-MARK.                          OBSQ14.2
053200     MOVE "READ-TEST-02" TO PAR-NAME.                             OBSQ14.2
053300 READ-TEST-02.                                                    OBSQ14.2
053400     READ SQ-FS1 AT END                                           OBSQ14.2
053500         MOVE "UNEXPECTED EOF" TO COMPUTED-A                      OBSQ14.2
053600         MOVE 1 TO EOF-FLAG                                       OBSQ14.2
053700         GO TO READ-FAIL-02.                                      OBSQ14.2
053800     PERFORM RECORD-CHECK.                                        OBSQ14.2
053900     IF WRK-CS-09V00 EQUAL TO 400                                 OBSQ14.2
054000         GO TO READ-TEST-02-1.                                    OBSQ14.2
054100     GO TO READ-TEST-02.                                          OBSQ14.2
054200 READ-TEST-02-1.                                                  OBSQ14.2
054300     IF ERROR-FLAG EQUAL TO ZERO                                  OBSQ14.2
054400         GO TO READ-PASS-02.                                      OBSQ14.2
054500     MOVE "ERROR IN RECORD(S)" TO COMPUTED-A.                     OBSQ14.2
054600 READ-FAIL-02.                                                    OBSQ14.2
054700     PERFORM FAIL.                                                OBSQ14.2
054800     GO TO READ-WRITE-02.                                         OBSQ14.2
054900 READ-PASS-02.                                                    OBSQ14.2
055000     PERFORM PASS.                                                OBSQ14.2
055100 READ-WRITE-02.                                                   OBSQ14.2
055200     PERFORM PRINT-DETAIL.                                        OBSQ14.2
055300 READ-INIT-03.                                                    OBSQ14.2
055400     IF EOF-FLAG EQUAL TO 1                                       OBSQ14.2
055500        GO TO SEQ-EOF-003.                                        OBSQ14.2
055600     MOVE ZERO TO ERROR-FLAG.                                     OBSQ14.2
055700     MOVE "READ...RECORD END..." TO RE-MARK.                      OBSQ14.2
055800     MOVE "READ-TEST-03" TO PAR-NAME.                             OBSQ14.2
055900 READ-TEST-03.                                                    OBSQ14.2
056000     READ SQ-FS1 RECORD END                                       OBSQ14.2
056100          MOVE "UNEXPECTED EOF" TO COMPUTED-A                     OBSQ14.2
056200          MOVE 1 TO EOF-FLAG                                      OBSQ14.2
056300          GO TO READ-FAIL-03.                                     OBSQ14.2
056400     PERFORM RECORD-CHECK.                                        OBSQ14.2
056500     IF WRK-CS-09V00 EQUAL TO 600                                 OBSQ14.2
056600         GO TO READ-TEST-03-1.                                    OBSQ14.2
056700     GO TO READ-TEST-03.                                          OBSQ14.2
056800 READ-TEST-03-1.                                                  OBSQ14.2
056900     IF ERROR-FLAG EQUAL TO ZERO                                  OBSQ14.2
057000         GO TO READ-PASS-03.                                      OBSQ14.2
057100     MOVE "ERROR IN RECORD(S)" TO COMPUTED-A.                     OBSQ14.2
057200 READ-FAIL-03.                                                    OBSQ14.2
057300     PERFORM FAIL.                                                OBSQ14.2
057400     GO TO READ-WRITE-03.                                         OBSQ14.2
057500 READ-PASS-03.                                                    OBSQ14.2
057600     PERFORM PASS.                                                OBSQ14.2
057700 READ-WRITE-03.                                                   OBSQ14.2
057800     PERFORM PRINT-DETAIL.                                        OBSQ14.2
057900 READ-INIT-04.                                                    OBSQ14.2
058000     IF EOF-FLAG EQUAL TO 1                                       OBSQ14.2
058100         GO TO SEQ-EOF-003.                                       OBSQ14.2
058200     MOVE ZERO TO ERROR-FLAG.                                     OBSQ14.2
058300     MOVE "READ...END..." TO RE-MARK.                             OBSQ14.2
058400     MOVE  "READ-TEST-04" TO PAR-NAME.                            OBSQ14.2
058500 READ-TEST-04.                                                    OBSQ14.2
058600     READ SQ-FS1 END GO TO READ-TEST-04-1.                        OBSQ14.2
058700     PERFORM RECORD-CHECK.                                        OBSQ14.2
058800     IF WRK-CS-09V00 GREATER THAN 750                             OBSQ14.2
058900          GO TO READ-TEST-04-1.                                   OBSQ14.2
059000     GO TO READ-TEST-04.                                          OBSQ14.2
059100 READ-TEST-04-1.                                                  OBSQ14.2
059200     IF ERROR-FLAG EQUAL TO ZERO                                  OBSQ14.2
059300          GO TO READ-PASS-04.                                     OBSQ14.2
059400 READ-FAIL-04.                                                    OBSQ14.2
059500     MOVE "ERROR IN RECORD(S)" TO COMPUTED-A.                     OBSQ14.2
059600     PERFORM FAIL.                                                OBSQ14.2
059700     GO TO READ-WRITE-04.                                         OBSQ14.2
059800 READ-PASS-04.                                                    OBSQ14.2
059900     PERFORM PASS.                                                OBSQ14.2
060000 READ-WRITE-04.                                                   OBSQ14.2
060100     PERFORM PRINT-DETAIL.                                        OBSQ14.2
060200 SEQ-TEST-003.                                                    OBSQ14.2
060300     IF RECORDS-IN-ERROR NOT EQUAL TO ZERO                        OBSQ14.2
060400          MOVE "RECORDS IN ERROR =" TO COMPUTED-A                 OBSQ14.2
060500          MOVE RECORDS-IN-ERROR TO CORRECT-18V0                   OBSQ14.2
060600          GO TO SEQ-FAIL-003.                                     OBSQ14.2
060700     IF WRK-CS-09V00 GREATER THAN 750                             OBSQ14.2
060800          MOVE "MORE THAN 750 RECORDS" TO RE-MARK                 OBSQ14.2
060900          GO TO SEQ-FAIL-003.                                     OBSQ14.2
061000 SEQ-PASS-003.                                                    OBSQ14.2
061100     PERFORM PASS.                                                OBSQ14.2
061200     GO TO SEQ-WRITE-003.                                         OBSQ14.2
061300 SEQ-EOF-003.                                                     OBSQ14.2
061400     MOVE "LESS THAN 750 RECORDS" TO RE-MARK.                     OBSQ14.2
061500     MOVE "RECORDS READ =" TO COMPUTED-A.                         OBSQ14.2
061600     MOVE WRK-CS-09V00 TO CORRECT-18V0.                           OBSQ14.2
061700 SEQ-FAIL-003.                                                    OBSQ14.2
061800     PERFORM FAIL.                                                OBSQ14.2
061900 SEQ-WRITE-003.                                                   OBSQ14.2
062000     MOVE "SEQ-TEST-003" TO PAR-NAME.                             OBSQ14.2
062100     MOVE "READ FILE SQ-FS1" TO FEATURE.                          OBSQ14.2
062200     PERFORM PRINT-DETAIL.                                        OBSQ14.2
062300 SEQ-CLOSE-003.                                                   OBSQ14.2
062400     CLOSE SQ-FS1.                                                OBSQ14.2
062500 TERMINATE-ROUTINE.                                               OBSQ14.2
062600     EXIT.                                                        OBSQ14.2
062700 CCVS-EXIT SECTION.                                               OBSQ14.2
062800 CCVS-999999.                                                     OBSQ14.2
062900     GO TO CLOSE-FILES.                                           OBSQ14.2
      *END-OF,OBSQ1A                                                                  
