      *HEADER,COBOL,SQ126A                                                            
000100 IDENTIFICATION DIVISION.                                         SQ1264.2
000200 PROGRAM-ID.                                                      SQ1264.2
000300     SQ126A.                                                      SQ1264.2
000400****************************************************************  SQ1264.2
000500*                                                              *  SQ1264.2
000600*    VALIDATION FOR:-                                          *  SQ1264.2
000700*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1264.2
000800*                                                              *  SQ1264.2
000900*    CREATION DATE     /     VALIDATION DATE                   *  SQ1264.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ1264.2
001100*                                                              *  SQ1264.2
001200****************************************************************  SQ1264.2
001300                                                                  SQ1264.2
001400*                                                                 SQ1264.2
001500*                                                                 SQ1264.2
001600******************************************************************SQ1264.2
001700*                                                                *SQ1264.2
001800*    NEW TESTS:                                                  *SQ1264.2
001900*                                                                *SQ1264.2
002000*    READ ... AT END  ... NOT AT END ...                         *SQ1264.2
002100*                                                                *SQ1264.2
002200*    READ ... RECORD AT END ... NOT END                          *SQ1264.2
002300*                                                                *SQ1264.2
002400*    IF ... READ ... AT END ... NOT AT END ... END-READ          *SQ1264.2
002500*                                                                *SQ1264.2
002600*    IF ... READ ... RECORD END ... NOT END ... END-READ ...     *SQ1264.2
002700*                                                                *SQ1264.2
002800******************************************************************SQ1264.2
002900*                                                                 SQ1264.2
003000*        THE ROUTINE SQ126A TESTS THE USE OF THE NOT AT END       SQ1264.2
003100*    PHRASE FOR THE READ STATEMENT AND ALSO THE END-READ PHRASE.  SQ1264.2
003200*                                                                 SQ1264.2
003300*                                                                 SQ1264.2
003400*    USED X-CARDS:                                                SQ1264.2
003500*         XXXXX001                                                SQ1264.2
003600*         XXXXX055                                                SQ1264.2
003700*     P   XXXXX062                                                SQ1264.2
003800*         XXXXX082                                                SQ1264.2
003900*         XXXXX083                                                SQ1264.2
004000*     C   XXXXX084                                                SQ1264.2
004100*                                                                 SQ1264.2
004200*                                                                 SQ1264.2
004300 ENVIRONMENT DIVISION.                                            SQ1264.2
004400 CONFIGURATION SECTION.                                           SQ1264.2
004500 SOURCE-COMPUTER.                                                 SQ1264.2
004600     XXXXX082.                                                    SQ1264.2
004700 OBJECT-COMPUTER.                                                 SQ1264.2
004800     XXXXX083.                                                    SQ1264.2
004900 INPUT-OUTPUT SECTION.                                            SQ1264.2
005000 FILE-CONTROL.                                                    SQ1264.2
005100*    SELECT RAW-DATA   ASSIGN TO                                  SQ1264.2
005200*    XXXXX062                                                     SQ1264.2
005300*           ORGANIZATION IS INDEXED                               SQ1264.2
005400*           ACCESS MODE IS RANDOM                                 SQ1264.2
005500*           RECORD KEY IS RAW-DATA-KEY.                           SQ1264.2
005600     SELECT PRINT-FILE ASSIGN TO                                  SQ1264.2
005700     XXXXX055.                                                    SQ1264.2
005800     SELECT SQ-FS1 ASSIGN TO                                      SQ1264.2
005900     XXXXX001                                                     SQ1264.2
006000     ORGANIZATION IS SEQUENTIAL                                   SQ1264.2
006100     ACCESS MODE IS SEQUENTIAL.                                   SQ1264.2
006200                                                                  SQ1264.2
006300 DATA DIVISION.                                                   SQ1264.2
006400                                                                  SQ1264.2
006500 FILE SECTION.                                                    SQ1264.2
006600*                                                                 SQ1264.2
006700*FD  RAW-DATA.                                                    SQ1264.2
006800*                                                                 SQ1264.2
006900*01  RAW-DATA-SATZ.                                               SQ1264.2
007000*    05  RAW-DATA-KEY        PIC X(6).                            SQ1264.2
007100*    05  C-DATE              PIC 9(6).                            SQ1264.2
007200*    05  C-TIME              PIC 9(8).                            SQ1264.2
007300*    05  C-NO-OF-TESTS       PIC 99.                              SQ1264.2
007400*    05  C-OK                PIC 999.                             SQ1264.2
007500*    05  C-ALL               PIC 999.                             SQ1264.2
007600*    05  C-FAIL              PIC 999.                             SQ1264.2
007700*    05  C-DELETED           PIC 999.                             SQ1264.2
007800*    05  C-INSPECT           PIC 999.                             SQ1264.2
007900*    05  C-NOTE              PIC X(13).                           SQ1264.2
008000*    05  C-INDENT            PIC X.                               SQ1264.2
008100*    05  C-ABORT             PIC X(8).                            SQ1264.2
008200                                                                  SQ1264.2
008300 FD  PRINT-FILE                                                   SQ1264.2
008400C    LABEL RECORDS                                                SQ1264.2
008500C    XXXXX084                                                     SQ1264.2
008600C    DATA RECORD IS PRINT-REC DUMMY-RECORD                        SQ1264.2
008700               .                                                  SQ1264.2
008800                                                                  SQ1264.2
008900 01  PRINT-REC               PIC X(120).                          SQ1264.2
009000                                                                  SQ1264.2
009100 01  DUMMY-RECORD            PIC X(120).                          SQ1264.2
009200                                                                  SQ1264.2
009300 FD  SQ-FS1                                                       SQ1264.2
009400C       LABEL RECORD STANDARD                                     SQ1264.2
009500               .                                                  SQ1264.2
009600                                                                  SQ1264.2
009700 01  SQ-FS1R1-F-G-120.                                            SQ1264.2
009800     05  FILLER              PIC X(120).                          SQ1264.2
009900                                                                  SQ1264.2
010000 WORKING-STORAGE SECTION.                                         SQ1264.2
010100                                                                  SQ1264.2
010200 01  SWITCH-READ1            PIC 9 VALUE ZERO.                    SQ1264.2
010300 01  SWITCH-READ2            PIC 9 VALUE ZERO.                    SQ1264.2
010400 01  SWITCH-READ3            PIC 9 VALUE ZERO.                    SQ1264.2
010500 01  FILE-STATUS-SQ-FS1      PIC XX VALUE SPACE.                  SQ1264.2
010600 01  WRK-CS-09V00            PIC S9(9) COMP VALUE ZERO.           SQ1264.2
010700 01  RECORDS-IN-ERROR        PIC S9(5) COMP VALUE ZERO.           SQ1264.2
010800 01  ERROR-FLAG              PIC 9 VALUE ZERO.                    SQ1264.2
010900 01  EOF-FLAG                PIC 9 VALUE ZERO.                    SQ1264.2
011000                                                                  SQ1264.2
011100 01  FILE-RECORD-INFORMATION-REC.                                 SQ1264.2
011200     05  FILE-RECORD-INFO-SKELETON.                               SQ1264.2
011300         10  FILLER          PIC X(48) VALUE                      SQ1264.2
011400              "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00". SQ1264.2
011500         10  FILLER          PIC X(46) VALUE                      SQ1264.2
011600                ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000". SQ1264.2
011700         10  FILLER          PIC X(26) VALUE                      SQ1264.2
011800                                    ",LFIL=000000,ORG=  ,LBLR= ". SQ1264.2
011900         10  FILLER          PIC X(37) VALUE                      SQ1264.2
012000                         ",RECKEY=                             ". SQ1264.2
012100         10  FILLER          PIC X(38) VALUE                      SQ1264.2
012200                        ",ALTKEY1=                             ". SQ1264.2
012300         10  FILLER          PIC X(38) VALUE                      SQ1264.2
012400                        ",ALTKEY2=                             ". SQ1264.2
012500         10  FILLER          PIC X(7) VALUE SPACE.                SQ1264.2
012600     05  FILE-RECORD-INFO             OCCURS 10.                  SQ1264.2
012700         10  FILE-RECORD-INFO-P1-120.                             SQ1264.2
012800             15  FILLER      PIC X(5).                            SQ1264.2
012900             15  XFILE-NAME  PIC X(6).                            SQ1264.2
013000             15  FILLER      PIC X(8).                            SQ1264.2
013100             15  XRECORD-NAME  PIC X(6).                          SQ1264.2
013200             15  FILLER        PIC X(1).                          SQ1264.2
013300             15  REELUNIT-NUMBER  PIC 9(1).                       SQ1264.2
013400             15  FILLER           PIC X(7).                       SQ1264.2
013500             15  XRECORD-NUMBER   PIC 9(6).                       SQ1264.2
013600             15  FILLER           PIC X(6).                       SQ1264.2
013700             15  UPDATE-NUMBER    PIC 9(2).                       SQ1264.2
013800             15  FILLER           PIC X(5).                       SQ1264.2
013900             15  ODO-NUMBER       PIC 9(4).                       SQ1264.2
014000             15  FILLER           PIC X(5).                       SQ1264.2
014100             15  XPROGRAM-NAME    PIC X(5).                       SQ1264.2
014200             15  FILLER           PIC X(7).                       SQ1264.2
014300             15  XRECORD-LENGTH   PIC 9(6).                       SQ1264.2
014400             15  FILLER           PIC X(7).                       SQ1264.2
014500             15  CHARS-OR-RECORDS  PIC X(2).                      SQ1264.2
014600             15  FILLER            PIC X(1).                      SQ1264.2
014700             15  XBLOCK-SIZE       PIC 9(4).                      SQ1264.2
014800             15  FILLER            PIC X(6).                      SQ1264.2
014900             15  RECORDS-IN-FILE   PIC 9(6).                      SQ1264.2
015000             15  FILLER            PIC X(5).                      SQ1264.2
015100             15  XFILE-ORGANIZATION  PIC X(2).                    SQ1264.2
015200             15  FILLER              PIC X(6).                    SQ1264.2
015300             15  XLABEL-TYPE         PIC X(1).                    SQ1264.2
015400         10  FILE-RECORD-INFO-P121-240.                           SQ1264.2
015500             15  FILLER              PIC X(8).                    SQ1264.2
015600             15  XRECORD-KEY         PIC X(29).                   SQ1264.2
015700             15  FILLER              PIC X(9).                    SQ1264.2
015800             15  ALTERNATE-KEY1      PIC X(29).                   SQ1264.2
015900             15  FILLER              PIC X(9).                    SQ1264.2
016000             15  ALTERNATE-KEY2      PIC X(29).                   SQ1264.2
016100             15  FILLER              PIC X(7).                    SQ1264.2
016200                                                                  SQ1264.2
016300 01  TEST-RESULTS.                                                SQ1264.2
016400     05  FILLER              PIC X VALUE SPACE.                   SQ1264.2
016500     05  FEATURE             PIC X(20) VALUE SPACE.               SQ1264.2
016600     05  FILLER              PIC X     VALUE SPACE.               SQ1264.2
016700     05  P-OR-F              PIC X(5)  VALUE SPACE.               SQ1264.2
016800     05  FILLER              PIC X     VALUE SPACE.               SQ1264.2
016900     05  PAR-NAME.                                                SQ1264.2
017000         10  FILLER          PIC X(12) VALUE SPACE.               SQ1264.2
017100         10  PARDOT-X        PIC X     VALUE SPACE.               SQ1264.2
017200         10  DOTVALUE        PIC 99    VALUE ZERO.                SQ1264.2
017300         10  FILLER          PIC X(5)  VALUE SPACE.               SQ1264.2
017400     05  FILLER              PIC X(10) VALUE SPACE.               SQ1264.2
017500     05  RE-MARK             PIC X(61).                           SQ1264.2
017600                                                                  SQ1264.2
017700 01  TEST-COMPUTED.                                               SQ1264.2
017800     05  FILLER              PIC X(30) VALUE SPACE.               SQ1264.2
017900     05  FILLER              PIC X(17) VALUE "       COMPUTED=".  SQ1264.2
018000     05  COMPUTED-X.                                              SQ1264.2
018100         10  COMPUTED-A      PIC X(20) VALUE SPACE.               SQ1264.2
018200         10  COMPUTED-N REDEFINES COMPUTED-A  PIC -9(9).9(9).     SQ1264.2
018300         10  COMPUTED-0V18 REDEFINES COMPUTED-A  PIC -.9(18).     SQ1264.2
018400         10  COMPUTED-4V14 REDEFINES COMPUTED-A  PIC -9(4).9(14). SQ1264.2
018500         10  COMPUTED-14V4 REDEFINES COMPUTED-A  PIC -9(14).9(4). SQ1264.2
018600         10  CM-18V0 REDEFINES COMPUTED-A.                        SQ1264.2
018700             15  COMPUTED-18V0                   PIC -9(18).      SQ1264.2
018800             15  FILLER                          PIC X.           SQ1264.2
018900         10  FILLER                              PIC X(50)        SQ1264.2
019000                                       VALUE SPACE.               SQ1264.2
019100                                                                  SQ1264.2
019200 01  TEST-CORRECT.                                                SQ1264.2
019300     05  FILLER              PIC X(30) VALUE SPACE.               SQ1264.2
019400     05  FILLER              PIC X(17) VALUE "       CORRECT =".  SQ1264.2
019500     05  CORRECT-X.                                               SQ1264.2
019600         10  CORRECT-A       PIC X(20) VALUE SPACE.               SQ1264.2
019700         10  CORRECT-N REDEFINES CORRECT-A  PIC -9(9).9(9).       SQ1264.2
019800         10  CORRECT-0V18 REDEFINES CORRECT-A  PIC -.9(18).       SQ1264.2
019900         10  CORRECT-4V14 REDEFINES CORRECT-A  PIC -9(4).9(14).   SQ1264.2
020000         10  CORRECT-14V4 REDEFINES CORRECT-A  PIC -9(14).9(4).   SQ1264.2
020100         10  CR-18V0 REDEFINES CORRECT-A.                         SQ1264.2
020200             15  CORRECT-18V0                  PIC -9(18).        SQ1264.2
020300             15  FILLER                        PIC X.             SQ1264.2
020400         10  FILLER                            PIC X(50)          SQ1264.2
020500                                       VALUE SPACE.               SQ1264.2
020600                                                                  SQ1264.2
020700 01  CCVS-C-1.                                                    SQ1264.2
020800     05  FILLER              PIC X(99) VALUE                      SQ1264.2
020900     " FEATURE              PASS  PARAGRAPH-NAME                  SQ1264.2
021000-    "                                REMARKS".                   SQ1264.2
021100     05  FILLER              PIC X(20) VALUE SPACE.               SQ1264.2
021200                                                                  SQ1264.2
021300 01  CCVS-C-2.                                                    SQ1264.2
021400     05  FILLER              PIC X VALUE SPACE.                   SQ1264.2
021500     05  FILLER              PIC X(6) VALUE "TESTED".             SQ1264.2
021600     05  FILLER              PIC X(15) VALUE SPACE.               SQ1264.2
021700     05  FILLER              PIC X(4)  VALUE "FAIL".              SQ1264.2
021800     05  FILLER              PIC X(94) VALUE SPACE.               SQ1264.2
021900                                                                  SQ1264.2
022000 01  REC-SKL-SUB             PIC 9(2) VALUE ZERO.                 SQ1264.2
022100 01  REC-CT                  PIC 99 VALUE ZERO.                   SQ1264.2
022200 01  DELETE-CNT              PIC 999 VALUE ZERO.                  SQ1264.2
022300 01  ERROR-COUNTER           PIC 999 VALUE ZERO.                  SQ1264.2
022400 01  INSPECT-COUNTER         PIC 999 VALUE ZERO.                  SQ1264.2
022500 01  PASS-COUNTER            PIC 999 VALUE ZERO.                  SQ1264.2
022600 01  TOTAL-ERROR             PIC 999 VALUE ZERO.                  SQ1264.2
022700 01  ERROR-HOLD              PIC 999 VALUE ZERO.                  SQ1264.2
022800 01  DUMMY-HOLD              PIC X(120) VALUE SPACE.              SQ1264.2
022900 01  RECORD-COUNT            PIC 9(5) VALUE ZERO.                 SQ1264.2
023000                                                                  SQ1264.2
023100 01  CCVS-H-1.                                                    SQ1264.2
023200     05  FILLER              PIC X(27) VALUE SPACE.               SQ1264.2
023300     05  FILLER              PIC X(67) VALUE                      SQ1264.2
023400     " FEDERAL SOFTWARE TESTING CENTER COBOL COMPILER VALIDATION  SQ1264.2
023500-    " SYSTEM".                                                   SQ1264.2
023600     05  FILLER              PIC X(26) VALUE SPACE.               SQ1264.2
023700                                                                  SQ1264.2
023800 01  CCVS-H-2.                                                    SQ1264.2
023900     05  FILLER              PIC X(52) VALUE                      SQ1264.2
024000                       "CCVS85 FSTC COPY, NOT FOR DISTRIBUTION.". SQ1264.2
024100     05  FILLER              PIC X(19) VALUE                      SQ1264.2
024200                                           "TEST RESULTS SET-  ". SQ1264.2
024300     05  TEST-ID             PIC X(9).                            SQ1264.2
024400     05  FILLER              PIC X(40) VALUE SPACE.               SQ1264.2
024500                                                                  SQ1264.2
024600 01  CCVS-H-3.                                                    SQ1264.2
024700     05  FILLER              PIC X(34) VALUE                      SQ1264.2
024800                                    " FOR OFFICIAL USE ONLY    ". SQ1264.2
024900     05  FILLER              PIC X(58) VALUE                      SQ1264.2
025000     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ1264.2
025100                                                                  SQ1264.2
025200     05  FILLER              PIC X(28) VALUE                      SQ1264.2
025300                                           "  COPYRIGHT   1985 ". SQ1264.2
025400                                                                  SQ1264.2
025500 01  CCVS-E-1.                                                    SQ1264.2
025600     05  FILLER              PIC X(52) VALUE SPACE.               SQ1264.2
025700     05  FILLER              PIC X(14) VALUE "END OF TEST-  ".    SQ1264.2
025800     05  ID-AGAIN            PIC X(9).                            SQ1264.2
025900     05  FILLER              PIC X(45) VALUE                      SQ1264.2
026000                                   " NTIS DISTRIBUTION COBOL 85". SQ1264.2
026100                                                                  SQ1264.2
026200 01  CCVS-E-2.                                                    SQ1264.2
026300     05  FILLER              PIC X(31) VALUE SPACE.               SQ1264.2
026400     05  FILLER              PIC X(21) VALUE SPACE.               SQ1264.2
026500     05  CCVS-E-2-2.                                              SQ1264.2
026600         10  ERROR-TOTAL     PIC XXX   VALUE SPACE.               SQ1264.2
026700         10  FILLER          PIC X     VALUE SPACE.               SQ1264.2
026800         10  ENDER-DESC      PIC X(44) VALUE                      SQ1264.2
026900                                            "ERRORS ENCOUNTERED". SQ1264.2
027000                                                                  SQ1264.2
027100 01  CCVS-E-3.                                                    SQ1264.2
027200     05  FILLER              PIC X(22) VALUE                      SQ1264.2
027300                                        " FOR OFFICIAL USE ONLY". SQ1264.2
027400     05  FILLER              PIC X(12) VALUE SPACE.               SQ1264.2
027500     05  FILLER              PIC X(58) VALUE                      SQ1264.2
027600     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1264.2
027700                                                                  SQ1264.2
027800     05  FILLER              PIC X(13) VALUE SPACE.               SQ1264.2
027900     05  FILLER              PIC X(15) VALUE " COPYRIGHT 1985".   SQ1264.2
028000                                                                  SQ1264.2
028100 01  CCVS-E-4.                                                    SQ1264.2
028200     05  CCVS-E-4-1          PIC XXX VALUE SPACE.                 SQ1264.2
028300     05  FILLER              PIC X(4) VALUE " OF ".               SQ1264.2
028400     05  CCVS-E-4-2          PIC XXX VALUE SPACE.                 SQ1264.2
028500     05  FILLER              PIC X(40) VALUE                      SQ1264.2
028600                            "  TESTS WERE EXECUTED SUCCESSFULLY". SQ1264.2
028700                                                                  SQ1264.2
028800 01  XXINFO.                                                      SQ1264.2
028900     05  FILLER              PIC X(30) VALUE                      SQ1264.2
029000                                  "        *** INFORMATION  ***". SQ1264.2
029100     05  INFO-TEXT.                                               SQ1264.2
029200         10  FILLER          PIC X(20) VALUE SPACE.               SQ1264.2
029300         10  XXCOMPUTED      PIC X(20).                           SQ1264.2
029400         10  FILLER          PIC X(5)  VALUE SPACE.               SQ1264.2
029500         10  XXCORRECT       PIC X(20).                           SQ1264.2
029600                                                                  SQ1264.2
029700 01  HYPHEN-LINE.                                                 SQ1264.2
029800     05  FILLER              PIC X VALUE SPACE.                   SQ1264.2
029900     05  FILLER              PIC X(65) VALUE                      SQ1264.2
030000     "************************************************************SQ1264.2
030100-    "*****".                                                     SQ1264.2
030200     05  FILLER              PIC X(54) VALUE                      SQ1264.2
030300        "******************************************************". SQ1264.2
030400                                                                  SQ1264.2
030500 01  CCVS-PGM-ID             PIC X(6) VALUE "SQ126A".             SQ1264.2
030600                                                                  SQ1264.2
030700 PROCEDURE DIVISION.                                              SQ1264.2
030800                                                                  SQ1264.2
030900 CCVS1 SECTION.                                                   SQ1264.2
031000 OPEN-FILES.                                                      SQ1264.2
031100*    OPEN I-O RAW-DATA.                                           SQ1264.2
031200*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            SQ1264.2
031300*    READ RAW-DATA INVALID KEY GO TO END-E-1.                     SQ1264.2
031400*    MOVE "ABORTED " TO C-ABORT.                                  SQ1264.2
031500*    ADD 1 TO C-NO-OF-TESTS.                                      SQ1264.2
031600*    ACCEPT C-DATE  FROM DATE.                                    SQ1264.2
031700*    ACCEPT C-TIME  FROM TIME.                                    SQ1264.2
031800*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-1.             SQ1264.2
031900*END-E-1.                                                         SQ1264.2
032000*    CLOSE RAW-DATA.                                              SQ1264.2
032100     OPEN                                                         SQ1264.2
032200        OUTPUT PRINT-FILE.                                        SQ1264.2
032300     MOVE CCVS-PGM-ID TO TEST-ID.                                 SQ1264.2
032400     MOVE CCVS-PGM-ID TO ID-AGAIN.                                SQ1264.2
032500     MOVE SPACE TO TEST-RESULTS.                                  SQ1264.2
032600     PERFORM HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.              SQ1264.2
032700     MOVE ZERO TO REC-SKL-SUB.                                    SQ1264.2
032800     PERFORM CCVS-INIT-FILE 9 TIMES.                              SQ1264.2
032900 CCVS-INIT-FILE.                                                  SQ1264.2
033000     ADD 1 TO REC-SKL-SUB.                                        SQ1264.2
033100     MOVE FILE-RECORD-INFO-SKELETON TO FILE-RECORD-INFO           SQ1264.2
033200          (REC-SKL-SUB).                                          SQ1264.2
033300 CCVS-INIT-EXIT.                                                  SQ1264.2
033400     GO TO CCVS1-EXIT.                                            SQ1264.2
033500 CLOSE-FILES.                                                     SQ1264.2
033600     PERFORM END-ROUTINE THRU END-ROUTINE-13.                     SQ1264.2
033700     CLOSE PRINT-FILE.                                            SQ1264.2
033800*    OPEN I-O RAW-DATA.                                           SQ1264.2
033900*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            SQ1264.2
034000*    READ RAW-DATA INVALID KEY GO TO END-E-2.                     SQ1264.2
034100*    MOVE "OK.     " TO C-ABORT.                                  SQ1264.2
034200*    MOVE PASS-COUNTER TO C-OK.                                   SQ1264.2
034300*    MOVE ERROR-HOLD   TO C-ALL.                                  SQ1264.2
034400*    MOVE ERROR-COUNTER TO C-FAIL.                                SQ1264.2
034500*    MOVE DELETE-CNT TO C-DELETED.                                SQ1264.2
034600*    MOVE INSPECT-COUNTER TO C-INSPECT.                           SQ1264.2
034700*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-2.             SQ1264.2
034800*END-E-2.                                                         SQ1264.2
034900*    CLOSE RAW-DATA.                                              SQ1264.2
035000 TERMINATE-CCVS.                                                  SQ1264.2
035100*    EXIT PROGRAM.                                                SQ1264.2
035200*TERMINATE-CALL.                                                  SQ1264.2
035300     STOP RUN.                                                    SQ1264.2
035400 INSPT.                                                           SQ1264.2
035500     MOVE "INSPT" TO P-OR-F.                                      SQ1264.2
035600     ADD 1 TO INSPECT-COUNTER.                                    SQ1264.2
035700 PASS.                                                            SQ1264.2
035800     MOVE "PASS " TO P-OR-F.                                      SQ1264.2
035900     ADD 1 TO PASS-COUNTER.                                       SQ1264.2
036000 FAIL.                                                            SQ1264.2
036100     MOVE "FAIL*" TO P-OR-F.                                      SQ1264.2
036200     ADD 1 TO ERROR-COUNTER.                                      SQ1264.2
036300 DE-LETE.                                                         SQ1264.2
036400     MOVE "*****" TO P-OR-F.                                      SQ1264.2
036500     ADD 1 TO DELETE-CNT.                                         SQ1264.2
036600     MOVE "****TEST DELETED****" TO RE-MARK.                      SQ1264.2
036700 PRINT-DETAIL.                                                    SQ1264.2
036800     IF REC-CT NOT EQUAL TO ZERO                                  SQ1264.2
036900         MOVE "." TO PARDOT-X                                     SQ1264.2
037000         MOVE REC-CT TO DOTVALUE.                                 SQ1264.2
037100     MOVE TEST-RESULTS TO PRINT-REC.                              SQ1264.2
037200     PERFORM WRITE-LINE.                                          SQ1264.2
037300     IF P-OR-F EQUAL TO "FAIL*"                                   SQ1264.2
037400         PERFORM WRITE-LINE                                       SQ1264.2
037500         PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                SQ1264.2
037600     ELSE                                                         SQ1264.2
037700         PERFORM BAIL-OUT THRU BAIL-OUT-EX.                       SQ1264.2
037800     MOVE SPACE TO P-OR-F.                                        SQ1264.2
037900     MOVE SPACE TO COMPUTED-X.                                    SQ1264.2
038000     MOVE SPACE TO CORRECT-X.                                     SQ1264.2
038100     IF REC-CT EQUAL TO ZERO                                      SQ1264.2
038200         MOVE SPACE TO PAR-NAME.                                  SQ1264.2
038300     MOVE SPACE TO RE-MARK.                                       SQ1264.2
038400 HEAD-ROUTINE.                                                    SQ1264.2
038500     MOVE CCVS-H-1 TO DUMMY-RECORD.                               SQ1264.2
038600     PERFORM WRITE-LINE 2 TIMES.                                  SQ1264.2
038700     MOVE CCVS-H-2 TO DUMMY-RECORD.                               SQ1264.2
038800     PERFORM WRITE-LINE 5 TIMES.                                  SQ1264.2
038900     MOVE CCVS-H-3 TO DUMMY-RECORD.                               SQ1264.2
039000     PERFORM WRITE-LINE 3 TIMES.                                  SQ1264.2
039100 COLUMN-NAMES-ROUTINE.                                            SQ1264.2
039200     MOVE CCVS-C-1 TO DUMMY-RECORD.                               SQ1264.2
039300     PERFORM WRITE-LINE.                                          SQ1264.2
039400     MOVE CCVS-C-2 TO DUMMY-RECORD.                               SQ1264.2
039500     PERFORM WRITE-LINE 2 TIMES.                                  SQ1264.2
039600     MOVE HYPHEN-LINE TO DUMMY-RECORD.                            SQ1264.2
039700     PERFORM WRITE-LINE.                                          SQ1264.2
039800 END-ROUTINE.                                                     SQ1264.2
039900     MOVE HYPHEN-LINE TO DUMMY-RECORD.                            SQ1264.2
040000     PERFORM WRITE-LINE 5 TIMES.                                  SQ1264.2
040100 END-RTN-EXIT.                                                    SQ1264.2
040200     MOVE CCVS-E-1 TO DUMMY-RECORD.                               SQ1264.2
040300     PERFORM WRITE-LINE 2 TIMES.                                  SQ1264.2
040400 END-ROUTINE-1.                                                   SQ1264.2
040500     ADD ERROR-COUNTER TO ERROR-HOLD                              SQ1264.2
040600     ADD INSPECT-COUNTER TO ERROR-HOLD.                           SQ1264.2
040700     ADD DELETE-CNT TO ERROR-HOLD.                                SQ1264.2
040800     ADD PASS-COUNTER TO ERROR-HOLD.                              SQ1264.2
040900*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   SQ1264.2
041000     MOVE PASS-COUNTER TO CCVS-E-4-1.                             SQ1264.2
041100     MOVE ERROR-HOLD TO CCVS-E-4-2.                               SQ1264.2
041200     MOVE CCVS-E-4 TO CCVS-E-2-2.                                 SQ1264.2
041300     MOVE CCVS-E-2 TO DUMMY-RECORD                                SQ1264.2
041400     PERFORM WRITE-LINE.                                          SQ1264.2
041500 END-ROUTINE-12.                                                  SQ1264.2
041600     MOVE "TEST(S) FAILED" TO ENDER-DESC.                         SQ1264.2
041700     IF ERROR-COUNTER IS EQUAL TO ZERO                            SQ1264.2
041800         MOVE "NO " TO ERROR-TOTAL                                SQ1264.2
041900     ELSE                                                         SQ1264.2
042000         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       SQ1264.2
042100     MOVE CCVS-E-2 TO DUMMY-RECORD.                               SQ1264.2
042200     PERFORM WRITE-LINE.                                          SQ1264.2
042300 END-ROUTINE-13.                                                  SQ1264.2
042400     IF DELETE-CNT IS EQUAL TO ZERO                               SQ1264.2
042500         MOVE "NO " TO ERROR-TOTAL                                SQ1264.2
042600     ELSE                                                         SQ1264.2
042700         MOVE DELETE-CNT TO ERROR-TOTAL.                          SQ1264.2
042800     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   SQ1264.2
042900     MOVE CCVS-E-2 TO DUMMY-RECORD.                               SQ1264.2
043000     PERFORM WRITE-LINE.                                          SQ1264.2
043100     IF INSPECT-COUNTER EQUAL TO ZERO                             SQ1264.2
043200         MOVE "NO " TO ERROR-TOTAL                                SQ1264.2
043300     ELSE                                                         SQ1264.2
043400         MOVE INSPECT-COUNTER TO ERROR-TOTAL.                     SQ1264.2
043500     MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.             SQ1264.2
043600     MOVE CCVS-E-2 TO DUMMY-RECORD.                               SQ1264.2
043700     PERFORM WRITE-LINE.                                          SQ1264.2
043800     MOVE CCVS-E-3 TO DUMMY-RECORD.                               SQ1264.2
043900     PERFORM WRITE-LINE.                                          SQ1264.2
044000 WRITE-LINE.                                                      SQ1264.2
044100     ADD 1 TO RECORD-COUNT.                                       SQ1264.2
044200*    IF RECORD-COUNT GREATER 50                                   SQ1264.2
044300*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          SQ1264.2
044400*        MOVE SPACE TO DUMMY-RECORD                               SQ1264.2
044500*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  SQ1264.2
044600*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             SQ1264.2
044700*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     SQ1264.2
044800*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          SQ1264.2
044900*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          SQ1264.2
045000*        MOVE ZERO TO RECORD-COUNT.                               SQ1264.2
045100     PERFORM WRT-LN.                                              SQ1264.2
045200 WRT-LN.                                                          SQ1264.2
045300     WRITE DUMMY-RECORD AFTER ADVANCING 1 LINES.                  SQ1264.2
045400     MOVE SPACE TO DUMMY-RECORD.                                  SQ1264.2
045500 BLANK-LINE-PRINT.                                                SQ1264.2
045600     PERFORM WRT-LN.                                              SQ1264.2
045700 FAIL-ROUTINE.                                                    SQ1264.2
045800     IF COMPUTED-X NOT EQUAL TO SPACE                             SQ1264.2
045900         GO TO FAIL-ROUTINE-WRITE.                                SQ1264.2
046000     IF CORRECT-X NOT EQUAL TO SPACE                              SQ1264.2
046100         GO TO FAIL-ROUTINE-WRITE.                                SQ1264.2
046200     MOVE "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.    SQ1264.2
046300     MOVE XXINFO TO DUMMY-RECORD.                                 SQ1264.2
046400     PERFORM WRITE-LINE 2 TIMES.                                  SQ1264.2
046500     GO TO FAIL-ROUTINE-EX.                                       SQ1264.2
046600 FAIL-ROUTINE-WRITE.                                              SQ1264.2
046700     MOVE TEST-COMPUTED TO PRINT-REC                              SQ1264.2
046800     PERFORM WRITE-LINE                                           SQ1264.2
046900     MOVE TEST-CORRECT TO PRINT-REC                               SQ1264.2
047000     PERFORM WRITE-LINE 2 TIMES.                                  SQ1264.2
047100 FAIL-ROUTINE-EX.                                                 SQ1264.2
047200     EXIT.                                                        SQ1264.2
047300 BAIL-OUT.                                                        SQ1264.2
047400     IF COMPUTED-A NOT EQUAL TO SPACE                             SQ1264.2
047500         GO TO BAIL-OUT-WRITE.                                    SQ1264.2
047600     IF CORRECT-A EQUAL TO SPACE                                  SQ1264.2
047700         GO TO BAIL-OUT-EX.                                       SQ1264.2
047800 BAIL-OUT-WRITE.                                                  SQ1264.2
047900     MOVE CORRECT-A TO XXCORRECT.                                 SQ1264.2
048000     MOVE COMPUTED-A TO XXCOMPUTED.                               SQ1264.2
048100     MOVE XXINFO TO DUMMY-RECORD.                                 SQ1264.2
048200     PERFORM WRITE-LINE 2 TIMES.                                  SQ1264.2
048300 BAIL-OUT-EX.                                                     SQ1264.2
048400     EXIT.                                                        SQ1264.2
048500 CCVS1-EXIT.                                                      SQ1264.2
048600     EXIT.                                                        SQ1264.2
048700                                                                  SQ1264.2
048800 SECT-SQ126-0001 SECTION.                                         SQ1264.2
048900 SEQ-INIT-001.                                                    SQ1264.2
049000     MOVE "SQ-FS1" TO XFILE-NAME (1).                             SQ1264.2
049100     MOVE "R1-F-G" TO XRECORD-NAME (1).                           SQ1264.2
049200     MOVE CCVS-PGM-ID TO XPROGRAM-NAME (1).                       SQ1264.2
049300     MOVE 000120 TO XRECORD-LENGTH (1).                           SQ1264.2
049400     MOVE "RC" TO CHARS-OR-RECORDS (1).                           SQ1264.2
049500     MOVE 0001 TO XBLOCK-SIZE (1).                                SQ1264.2
049600     MOVE 000750 TO RECORDS-IN-FILE (1).                          SQ1264.2
049700     MOVE "SQ" TO XFILE-ORGANIZATION (1).                         SQ1264.2
049800     MOVE "S" TO XLABEL-TYPE (1).                                 SQ1264.2
049900     MOVE 000001 TO XRECORD-NUMBER (1).                           SQ1264.2
050000     OPEN                                                         SQ1264.2
050100        OUTPUT SQ-FS1.                                            SQ1264.2
050200 SEQ-TEST-001.                                                    SQ1264.2
050300     MOVE FILE-RECORD-INFO-P1-120 (1) TO SQ-FS1R1-F-G-120.        SQ1264.2
050400     WRITE SQ-FS1R1-F-G-120.                                      SQ1264.2
050500     IF XRECORD-NUMBER (1) EQUAL TO 750                           SQ1264.2
050600         GO TO SEQ-WRITE-001.                                     SQ1264.2
050700     ADD 1 TO XRECORD-NUMBER (1).                                 SQ1264.2
050800     GO TO SEQ-TEST-001.                                          SQ1264.2
050900 SEQ-WRITE-001.                                                   SQ1264.2
051000     MOVE "CREATE FILE SQ-FS1" TO FEATURE.                        SQ1264.2
051100     MOVE "SEQ-TEST-001" TO PAR-NAME.                             SQ1264.2
051200     MOVE "FILE CREATED, RECS =" TO COMPUTED-A.                   SQ1264.2
051300     MOVE XRECORD-NUMBER (1) TO CORRECT-18V0.                     SQ1264.2
051400     PERFORM PRINT-DETAIL.                                        SQ1264.2
051500     CLOSE SQ-FS1.                                                SQ1264.2
051600*        A SEQUENTIAL TAPE FILE WITH 120 CHARACTER RECORDS        SQ1264.2
051700*    HAS BEEN CREATED. THE FILE CONTAINS 750 RECORDS.             SQ1264.2
051800 READ-INIT-GF-01.                                                 SQ1264.2
051900     MOVE ZERO TO WRK-CS-09V00.                                   SQ1264.2
052000     MOVE ZERO TO SWITCH-READ1.                                   SQ1264.2
052100*        THIS TEST READS AND CHECKS THE FILE CREATED IN           SQ1264.2
052200*    READ-TEST-001 AND CHECKS THE NOT AT END CONDITION.           SQ1264.2
052300     OPEN                                                         SQ1264.2
052400        INPUT SQ-FS1.                                             SQ1264.2
052500 READ-TEST-GF-01.                                                 SQ1264.2
052600******************************************************************SQ1264.2
052700*                                                                *SQ1264.2
052800*    READ ... AT END  --- NOT AT END ...                         *SQ1264.2
052900*                                                                *SQ1264.2
053000******************************************************************SQ1264.2
053100     READ SQ-FS1 AT END                                           SQ1264.2
053200         GO TO READ-TEST-GF-01-1                                  SQ1264.2
053300         NOT AT END                                               SQ1264.2
053400             MOVE 1 TO SWITCH-READ1.                              SQ1264.2
053500     MOVE SQ-FS1R1-F-G-120 TO FILE-RECORD-INFO-P1-120 (1).        SQ1264.2
053600     ADD 1 TO WRK-CS-09V00.                                       SQ1264.2
053700     IF WRK-CS-09V00 GREATER THAN 750                             SQ1264.2
053800         MOVE "MORE THAN 750 RECORDS" TO RE-MARK                  SQ1264.2
053900         GO TO READ-FAIL-GF-01.                                   SQ1264.2
054000     IF WRK-CS-09V00 NOT EQUAL TO XRECORD-NUMBER (1)              SQ1264.2
054100         ADD 1 TO RECORDS-IN-ERROR                                SQ1264.2
054200         GO TO READ-TEST-GF-01.                                   SQ1264.2
054300     IF XFILE-NAME (1) NOT EQUAL TO "SQ-FS1"                      SQ1264.2
054400         ADD 1 TO RECORDS-IN-ERROR                                SQ1264.2
054500         GO TO READ-TEST-GF-01.                                   SQ1264.2
054600     IF XLABEL-TYPE (1) NOT EQUAL TO "S"                          SQ1264.2
054700         ADD 1 TO RECORDS-IN-ERROR.                               SQ1264.2
054800     GO TO READ-TEST-GF-01.                                       SQ1264.2
054900 READ-TEST-GF-01-1.                                               SQ1264.2
055000     IF RECORDS-IN-ERROR EQUAL TO ZERO                            SQ1264.2
055100         GO TO READ-PASS-GF-01.                                   SQ1264.2
055200     MOVE "ERRORS IN READING SQ-FS1" TO RE-MARK.                  SQ1264.2
055300 READ-FAIL-GF-01.                                                 SQ1264.2
055400     MOVE "RECORDS IN ERROR =" TO COMPUTED-A.                     SQ1264.2
055500     MOVE RECORDS-IN-ERROR TO CORRECT-18V0.                       SQ1264.2
055600     PERFORM FAIL.                                                SQ1264.2
055700     GO TO READ-WRITE-GF-01.                                      SQ1264.2
055800 READ-PASS-GF-01.                                                 SQ1264.2
055900     PERFORM PASS.                                                SQ1264.2
056000     MOVE "FILE VERIFIED RECS =" TO COMPUTED-A.                   SQ1264.2
056100     MOVE WRK-CS-09V00 TO CORRECT-18V0.                           SQ1264.2
056200 READ-WRITE-GF-01.                                                SQ1264.2
056300     MOVE "READ-TEST-GF-01" TO PAR-NAME.                          SQ1264.2
056400     MOVE "VERIFY FILE SQ-FS1" TO FEATURE.                        SQ1264.2
056500     PERFORM PRINT-DETAIL.                                        SQ1264.2
056600 READ-TEST-GF-01-2.                                               SQ1264.2
056700     MOVE "READ...AT END...NOT AT END" TO RE-MARK.                SQ1264.2
056800     MOVE "NOT AT END" TO FEATURE.                                SQ1264.2
056900     IF SWITCH-READ1 = 1                                          SQ1264.2
057000         GO TO READ-PASS-GF-01-2.                                 SQ1264.2
057100 READ-FAIL-GF-01-2.                                               SQ1264.2
057200     MOVE "VII-44 4.4.2, VII-46 4.4.4 (11) C  " TO RE-MARK.       SQ1264.2
057300     PERFORM FAIL.                                                SQ1264.2
057400     GO TO READ-WRITE-GF-01-2.                                    SQ1264.2
057500 READ-PASS-GF-01-2.                                               SQ1264.2
057600     PERFORM PASS.                                                SQ1264.2
057700 READ-WRITE-GF-01-2.                                              SQ1264.2
057800     MOVE "READ-TEST-GF-01-2" TO PAR-NAME.                        SQ1264.2
057900     PERFORM PRINT-DETAIL.                                        SQ1264.2
058000 SEQ-CLOSE-GF-01.                                                 SQ1264.2
058100     CLOSE SQ-FS1.                                                SQ1264.2
058200 READ-INIT-GF-02.                                                 SQ1264.2
058300     MOVE ZERO TO SWITCH-READ1.                                   SQ1264.2
058400     MOVE ZERO TO WRK-CS-09V00.                                   SQ1264.2
058500     MOVE ZERO TO RECORDS-IN-ERROR.                               SQ1264.2
058600     OPEN                                                         SQ1264.2
058700        INPUT SQ-FS1.                                             SQ1264.2
058800     MOVE "NOT END             " TO FEATURE.                      SQ1264.2
058900     MOVE "READ...RECORD AT END ... NOT END " TO RE-MARK.         SQ1264.2
059000     MOVE "READ-TEST-GF-02" TO PAR-NAME.                          SQ1264.2
059100     MOVE ZERO TO ERROR-FLAG.                                     SQ1264.2
059200 READ-TEST-GF-02.                                                 SQ1264.2
059300******************************************************************SQ1264.2
059400*                                                                *SQ1264.2
059500*   READ ... RECORD AT END ... NOT END                           *SQ1264.2
059600*                                                                *SQ1264.2
059700******************************************************************SQ1264.2
059800     READ SQ-FS1 RECORD AT END                                    SQ1264.2
059900         MOVE "UNEXPECTED EOF" TO COMPUTED-A                      SQ1264.2
060000         MOVE 1 TO EOF-FLAG                                       SQ1264.2
060100         GO TO READ-FAIL-GF-02                                    SQ1264.2
060200      NOT END                                                     SQ1264.2
060300             MOVE 1 TO SWITCH-READ1.                              SQ1264.2
060400     PERFORM RECORD-CHECK.                                        SQ1264.2
060500     IF WRK-CS-09V00 EQUAL TO 200                                 SQ1264.2
060600         GO TO READ-TEST-GF-02-1.                                 SQ1264.2
060700     GO TO READ-TEST-GF-02.                                       SQ1264.2
060800 RECORD-CHECK.                                                    SQ1264.2
060900     MOVE SQ-FS1R1-F-G-120 TO FILE-RECORD-INFO-P1-120 (1).        SQ1264.2
061000     ADD 1 TO WRK-CS-09V00.                                       SQ1264.2
061100     IF WRK-CS-09V00 NOT EQUAL TO XRECORD-NUMBER (1)              SQ1264.2
061200         ADD 1 TO RECORDS-IN-ERROR                                SQ1264.2
061300         MOVE 1 TO ERROR-FLAG.                                    SQ1264.2
061400 READ-TEST-GF-02-1.                                               SQ1264.2
061500     IF SWITCH-READ1 = 1                                          SQ1264.2
061600         GO TO READ-PASS-GF-02.                                   SQ1264.2
061700     MOVE "NOT PASSED"               TO COMPUTED-A.               SQ1264.2
061800 READ-FAIL-GF-02.                                                 SQ1264.2
061900     MOVE "VII-44 4.4.2, VII-46 4.4.4 (11) C  " TO RE-MARK.       SQ1264.2
062000     PERFORM FAIL.                                                SQ1264.2
062100     GO TO READ-WRITE-GF-02.                                      SQ1264.2
062200 READ-PASS-GF-02.                                                 SQ1264.2
062300     PERFORM PASS.                                                SQ1264.2
062400 READ-WRITE-GF-02.                                                SQ1264.2
062500     PERFORM PRINT-DETAIL.                                        SQ1264.2
062600     PERFORM PRINT-DETAIL.                                        SQ1264.2
062700 READ-INIT-GF-03.                                                 SQ1264.2
062800     MOVE ZERO TO ERROR-FLAG.                                     SQ1264.2
062900     MOVE ZERO TO SWITCH-READ1.                                   SQ1264.2
063000     MOVE 1 TO SWITCH-READ2.                                      SQ1264.2
063100     MOVE ZERO TO SWITCH-READ3.                                   SQ1264.2
063200     MOVE "IF...READ...AT END...NOT AT END..." TO RE-MARK.        SQ1264.2
063300     MOVE "READ-TEST-GF-03-1" TO PAR-NAME.                        SQ1264.2
063400     MOVE "NOT AT END;END-READ" TO FEATURE.                       SQ1264.2
063500 READ-TEST-GF-03.                                                 SQ1264.2
063600******************************************************************SQ1264.2
063700*                                                                *SQ1264.2
063800*    IF ... READ ... AT END ... NOT AT END ... END-READ          *SQ1264.2
063900*                                                                *SQ1264.2
064000******************************************************************SQ1264.2
064100     IF SWITCH-READ2 = 1                                          SQ1264.2
064200         READ SQ-FS1 AT END                                       SQ1264.2
064300             MOVE "UNEXPECTED EOF" TO COMPUTED-A                  SQ1264.2
064400             MOVE 1 TO EOF-FLAG                                   SQ1264.2
064500             GO TO READ-FAIL-GF-03                                SQ1264.2
064600             NOT AT END                                           SQ1264.2
064700                 MOVE 1 TO SWITCH-READ1                           SQ1264.2
064800             END-READ                                             SQ1264.2
064900         MOVE 1 TO SWITCH-READ3.                                  SQ1264.2
065000     PERFORM RECORD-CHECK.                                        SQ1264.2
065100     IF WRK-CS-09V00 EQUAL TO 400                                 SQ1264.2
065200         GO TO READ-TEST-GF-03-1.                                 SQ1264.2
065300     GO TO READ-TEST-GF-03.                                       SQ1264.2
065400 READ-TEST-GF-03-1.                                               SQ1264.2
065500     IF SWITCH-READ1 = 1                                          SQ1264.2
065600         GO TO READ-PASS-GF-03.                                   SQ1264.2
065700 READ-FAIL-GF-03.                                                 SQ1264.2
065800     MOVE "VII-44 4.4.2, VII-46 4.4.4 (11) C  " TO RE-MARK.       SQ1264.2
065900     PERFORM FAIL.                                                SQ1264.2
066000     GO TO READ-WRITE-GF-03.                                      SQ1264.2
066100 READ-PASS-GF-03.                                                 SQ1264.2
066200     PERFORM PASS.                                                SQ1264.2
066300 READ-WRITE-GF-03.                                                SQ1264.2
066400     PERFORM PRINT-DETAIL.                                        SQ1264.2
066500 READ-TEST-GF-03-2.                                               SQ1264.2
066600     IF SWITCH-READ3 = 1                                          SQ1264.2
066700         GO TO READ-PASS-GF-03-2.                                 SQ1264.2
066800 READ-FAIL-GF-03-2.                                               SQ1264.2
066900     MOVE "VII-47 4.4.4 (14)  " TO RE-MARK.                       SQ1264.2
067000     PERFORM FAIL.                                                SQ1264.2
067100     GO TO READ-WRITE-GF-03-2.                                    SQ1264.2
067200 READ-PASS-GF-03-2.                                               SQ1264.2
067300     PERFORM PASS.                                                SQ1264.2
067400 READ-WRITE-GF-03-2.                                              SQ1264.2
067500     MOVE "READ-TEST-GF-03-2" TO PAR-NAME.                        SQ1264.2
067600     PERFORM PRINT-DETAIL.                                        SQ1264.2
067700 READ-INIT-GF-04.                                                 SQ1264.2
067800     MOVE ZERO TO ERROR-FLAG.                                     SQ1264.2
067900     MOVE ZERO TO SWITCH-READ1.                                   SQ1264.2
068000     MOVE ZERO TO SWITCH-READ2.                                   SQ1264.2
068100     MOVE 1 TO SWITCH-READ3.                                      SQ1264.2
068200     MOVE "READ...RECORD END...NOT END;END-READ" TO RE-MARK.      SQ1264.2
068300     MOVE "READ-TEST-GF-04-1" TO PAR-NAME.                        SQ1264.2
068400 READ-TEST-GF-04.                                                 SQ1264.2
068500******************************************************************SQ1264.2
068600*                                                                *SQ1264.2
068700*   IF ... READ ... RECORD END ... NOT END ... END-READ ...      *SQ1264.2
068800*                                                                *SQ1264.2
068900******************************************************************SQ1264.2
069000     IF SWITCH-READ3 = 1                                          SQ1264.2
069100         READ SQ-FS1 RECORD END                                   SQ1264.2
069200             MOVE "UNEXPECTED EOF" TO COMPUTED-A                  SQ1264.2
069300             MOVE 1 TO EOF-FLAG                                   SQ1264.2
069400             GO TO READ-FAIL-GF-04-1                              SQ1264.2
069500             NOT END                                              SQ1264.2
069600                 MOVE 1 TO SWITCH-READ1                           SQ1264.2
069700             END-READ                                             SQ1264.2
069800         MOVE 1 TO SWITCH-READ2.                                  SQ1264.2
069900     PERFORM RECORD-CHECK.                                        SQ1264.2
070000     IF WRK-CS-09V00 EQUAL TO 600                                 SQ1264.2
070100         GO TO READ-TEST-GF-04-1.                                 SQ1264.2
070200     GO TO READ-TEST-GF-04.                                       SQ1264.2
070300 READ-TEST-GF-04-1.                                               SQ1264.2
070400     IF SWITCH-READ1 EQUAL TO 1                                   SQ1264.2
070500         GO TO READ-PASS-GF-04-1.                                 SQ1264.2
070600     MOVE "NOT PASSED"               TO COMPUTED-A.               SQ1264.2
070700 READ-FAIL-GF-04-1.                                               SQ1264.2
070800     MOVE "VII-44 4.4.2, VII-46 4.4.4 (11) C  " TO RE-MARK.       SQ1264.2
070900     PERFORM FAIL.                                                SQ1264.2
071000     GO TO READ-WRITE-GF-04-1.                                    SQ1264.2
071100 READ-PASS-GF-04-1.                                               SQ1264.2
071200     PERFORM PASS.                                                SQ1264.2
071300 READ-WRITE-GF-04-1.                                              SQ1264.2
071400     PERFORM PRINT-DETAIL.                                        SQ1264.2
071500 READ-TEST-GF-04-2.                                               SQ1264.2
071600     IF SWITCH-READ2 EQUAL TO 1                                   SQ1264.2
071700         GO TO READ-PASS-GF-04-2.                                 SQ1264.2
071800     MOVE "END-READ: NOT PASSED"      TO COMPUTED-A.              SQ1264.2
071900     MOVE "READ-TEST-GF-04-2" TO PAR-NAME.                        SQ1264.2
072000 READ-FAIL-GF-04-2.                                               SQ1264.2
072100     MOVE "VII-47 4.4.4 (14)  " TO RE-MARK.                       SQ1264.2
072200     PERFORM FAIL.                                                SQ1264.2
072300     GO TO READ-WRITE-GF-04-2.                                    SQ1264.2
072400 READ-PASS-GF-04-2.                                               SQ1264.2
072500     PERFORM PASS.                                                SQ1264.2
072600 READ-WRITE-GF-04-2.                                              SQ1264.2
072700     PERFORM PRINT-DETAIL.                                        SQ1264.2
072800 SEQ-CLOSE-003.                                                   SQ1264.2
072900     CLOSE SQ-FS1.                                                SQ1264.2
073000 TERMINATE-ROUTINE.                                               SQ1264.2
073100     EXIT.                                                        SQ1264.2
073200                                                                  SQ1264.2
073300 CCVS-EXIT SECTION.                                               SQ1264.2
073400 CCVS-999999.                                                     SQ1264.2
073500     GO TO CLOSE-FILES.                                           SQ1264.2
      *END-OF,SQ126A                                                                  
