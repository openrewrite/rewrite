      *HEADER,COBOL,SQ224A                                                            
000100 IDENTIFICATION DIVISION.                                         SQ2244.2
000200 PROGRAM-ID.                                                      SQ2244.2
000300     SQ224A.                                                      SQ2244.2
000400****************************************************************  SQ2244.2
000500*                                                              *  SQ2244.2
000600*    VALIDATION FOR:-                                          *  SQ2244.2
000700*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ2244.2
000800*                                                              *  SQ2244.2
000900*    CREATION DATE     /     VALIDATION DATE                   *  SQ2244.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ2244.2
001100*                                                              *  SQ2244.2
001200*        THIS ROUTINE CHECKS:                                     SQ2244.2
001300*                                                                 SQ2244.2
001400*           RECORD IS VARYING IN SIZE FROM 18 TO 2048 CHARACTERS  SQ2244.2
001500*             DEPENDING ON DATA-NAME-1.                           SQ2244.2
001600*                                                                 SQ2244.2
001700*    THE WRITE STATEMENT IS USED WITH AND WITHOUT THE INTO CLAUSE.SQ2244.2
001800*                                                                 SQ2244.2
001900*    THE READ STATEMENT IS USED WITH AND WITHOUT THE INTO CLAUSE. SQ2244.2
002000*                                                                 SQ2244.2
002100*    THIS  ROUTINE  BUILDS  A  SEQUENTIAL MASS STORAGE FILE WHICH SQ2244.2
002200*    CONTAINS  2031 RECORDS OF A LENGTH OF 18 TO 2048 CHARACTERS. SQ2244.2
002300*    THE  MASS STORAGE FILE IS READ AND FIELDS IN THE RECORDS ARE SQ2244.2
002400*    CHECKED AGAINST THE EXPECTED VALUES.                         SQ2244.2
002500*                                                                 SQ2244.2
002600 ENVIRONMENT DIVISION.                                            SQ2244.2
002700 CONFIGURATION SECTION.                                           SQ2244.2
002800 SOURCE-COMPUTER.                                                 SQ2244.2
002900     XXXXX082.                                                    SQ2244.2
003000 OBJECT-COMPUTER.                                                 SQ2244.2
003100     XXXXX083.                                                    SQ2244.2
003200 INPUT-OUTPUT SECTION.                                            SQ2244.2
003300 FILE-CONTROL.                                                    SQ2244.2
003400*    SELECT RAW-DATA   ASSIGN TO                                  SQ2244.2
003500*    XXXXX062                                                     SQ2244.2
003600*           ORGANIZATION IS INDEXED                               SQ2244.2
003700*           ACCESS MODE IS RANDOM                                 SQ2244.2
003800*           RECORD KEY IS RAW-DATA-KEY.                           SQ2244.2
003900     SELECT PRINT-FILE ASSIGN TO                                  SQ2244.2
004000     XXXXX055.                                                    SQ2244.2
004100     SELECT SQ-VS7 ASSIGN TO                                      SQ2244.2
004200     XXXXX014                                                     SQ2244.2
004300     ORGANIZATION SEQUENTIAL                                      SQ2244.2
004400     ACCESS SEQUENTIAL.                                           SQ2244.2
004500 DATA DIVISION.                                                   SQ2244.2
004600 FILE SECTION.                                                    SQ2244.2
004700*FD  RAW-DATA.                                                    SQ2244.2
004800*                                                                 SQ2244.2
004900*01  RAW-DATA-SATZ.                                               SQ2244.2
005000*    05  RAW-DATA-KEY        PIC X(6).                            SQ2244.2
005100*    05  C-DATE              PIC 9(6).                            SQ2244.2
005200*    05  C-TIME              PIC 9(8).                            SQ2244.2
005300*    05  C-NO-OF-TESTS       PIC 99.                              SQ2244.2
005400*    05  C-OK                PIC 999.                             SQ2244.2
005500*    05  C-ALL               PIC 999.                             SQ2244.2
005600*    05  C-FAIL              PIC 999.                             SQ2244.2
005700*    05  C-DELETED           PIC 999.                             SQ2244.2
005800*    05  C-INSPECT           PIC 999.                             SQ2244.2
005900*    05  C-NOTE              PIC X(13).                           SQ2244.2
006000*    05  C-INDENT            PIC X.                               SQ2244.2
006100*    05  C-ABORT             PIC X(8).                            SQ2244.2
006200 FD  PRINT-FILE                                                   SQ2244.2
006300C    LABEL RECORDS                                                SQ2244.2
006400C    XXXXX084                                                     SQ2244.2
006500C    DATA RECORD IS PRINT-REC DUMMY-RECORD                        SQ2244.2
006600               .                                                  SQ2244.2
006700 01  PRINT-REC PICTURE X(120).                                    SQ2244.2
006800 01  DUMMY-RECORD PICTURE X(120).                                 SQ2244.2
006900 FD  SQ-VS7                                                       SQ2244.2
007000C    LABEL RECORDS ARE STANDARD                                   SQ2244.2
007100     RECORD IS VARYING IN SIZE FROM 18 TO 2048 CHARACTERS         SQ2244.2
007200       DEPENDING ON RECORD-LENGTH.                                SQ2244.2
007300 01  SQ-VSR7R1-M-G-2048.                                          SQ2244.2
007400     02  SQ-VS7R1-FIRST PICTURE X(2048).                          SQ2244.2
007500 WORKING-STORAGE SECTION.                                         SQ2244.2
007600 01  RECORD-LENGTH       PICTURE 9999 VALUE ZERO.                 SQ2244.2
007700 01  SAVE-COUNT-OF-RECS  PICTURE X(5) VALUE SPACE.                SQ2244.2
007800 01  COUNT-OF-RECS  PICTURE S9(4) COMPUTATIONAL.                  SQ2244.2
007900 01  RECORDS-IN-ERROR  PICTURE S9(4) COMPUTATIONAL.               SQ2244.2
008000 01  ERROR-FLAG PICTURE 9.                                        SQ2244.2
008100 01  EOF-FLAG  PICTURE 9.                                         SQ2244.2
008200 01  DUMP-AREA.                                                   SQ2244.2
008300     02  TYPE-OF-REC PICTURE X(5).                                SQ2244.2
008400     02  RECNO  PICTURE 9(5).                                     SQ2244.2
008500     02  REC-FILLER PICTURE X(21).                                SQ2244.2
008600 01  VAR-RECORD-18-2048.                                          SQ2244.2
008700     05  FILLER PIC X(13) VALUE "SQ-VS7LENGTH=".                  SQ2244.2
008800     05  RECORD-NUMBER PIC 9999 VALUE ZERO.                       SQ2244.2
008900     05  FILLER      PIC X(100) VALUE                             SQ2244.2
009000     "........10........20........30........40........50........60SQ2244.2
009100-    "........70........80........90.......100".                  SQ2244.2
009200     05  FILLER      PIC X(100) VALUE                             SQ2244.2
009300     ".......110.......120.......130.......140.......150.......160SQ2244.2
009400-    ".......170.......180.......190.......200".                  SQ2244.2
009500     05  FILLER      PIC X(100) VALUE                             SQ2244.2
009600     ".......210.......220.......230.......240.......250.......260SQ2244.2
009700-    ".......270.......280.......290.......300".                  SQ2244.2
009800     05  FILLER      PIC X(100) VALUE                             SQ2244.2
009900     ".......310.......320.......330.......340.......350.......360SQ2244.2
010000-    ".......370.......380.......390.......400".                  SQ2244.2
010100     05  FILLER      PIC X(100) VALUE                             SQ2244.2
010200     ".......410.......420.......430.......440.......450.......460SQ2244.2
010300-    ".......470.......480.......490.......500".                  SQ2244.2
010400     05  FILLER      PIC X(100) VALUE                             SQ2244.2
010500     ".......510.......520.......530.......540.......550.......560SQ2244.2
010600-    ".......570.......580.......590.......600".                  SQ2244.2
010700     05  FILLER      PIC X(100) VALUE                             SQ2244.2
010800     ".......610.......620.......630.......640.......650.......660SQ2244.2
010900-    ".......670.......680.......690.......700".                  SQ2244.2
011000     05  FILLER      PIC X(100) VALUE                             SQ2244.2
011100     ".......710.......720.......730.......740.......750.......760SQ2244.2
011200-    ".......770.......780.......790.......800".                  SQ2244.2
011300     05  FILLER      PIC X(100) VALUE                             SQ2244.2
011400     ".......810.......820.......830.......840.......850.......860SQ2244.2
011500-    ".......870.......880.......890.......900".                  SQ2244.2
011600     05  FILLER      PIC X(100) VALUE                             SQ2244.2
011700     ".......910.......920.......930.......940.......950.......960SQ2244.2
011800-    ".......970.......980.......990......1000".                  SQ2244.2
011900     05  FILLER      PIC X(100) VALUE                             SQ2244.2
012000     "......1010......1020......1030......1040......1050......1060SQ2244.2
012100-    "......1070......1080......1090......1100".                  SQ2244.2
012200     05  FILLER      PIC X(100) VALUE                             SQ2244.2
012300     "......1110......1120......1130......1140......1150......1160SQ2244.2
012400-    "......1170......1180......1190......1200".                  SQ2244.2
012500     05  FILLER      PIC X(100) VALUE                             SQ2244.2
012600     "......1210......1220......1230......1240......1250......1260SQ2244.2
012700-    ".......270.......280.......290.......300".                  SQ2244.2
012800     05  FILLER      PIC X(100) VALUE                             SQ2244.2
012900     "......1310......1320......1330......1340......1350......1360SQ2244.2
013000-    "......1370......1380......1390......1400".                  SQ2244.2
013100     05  FILLER      PIC X(100) VALUE                             SQ2244.2
013200     "......1410......1420......1430......1440......1450......1460SQ2244.2
013300-    "......1470......1480......1490......1500".                  SQ2244.2
013400     05  FILLER      PIC X(100) VALUE                             SQ2244.2
013500     "......1510......1520......1530......1540......1550......1560SQ2244.2
013600-    "......1570......1580......1590......1600".                  SQ2244.2
013700     05  FILLER      PIC X(100) VALUE                             SQ2244.2
013800     "......1610......1620......1630......1640......1650......1660SQ2244.2
013900-    "......1670......1680......1690......1700".                  SQ2244.2
014000     05  FILLER      PIC X(100) VALUE                             SQ2244.2
014100     "......1710......1720......1730......1740......1750......1760SQ2244.2
014200-    "......1770......1780......1790......1800".                  SQ2244.2
014300     05  FILLER      PIC X(100) VALUE                             SQ2244.2
014400     "......1810......1820......1830......1840......1850......1860SQ2244.2
014500-    "......1870......1880......1890......1900".                  SQ2244.2
014600     05  FILLER      PIC X(100) VALUE                             SQ2244.2
014700     "......1910......1920......1930......1940......1950......1960SQ2244.2
014800-    "......1970......1980......1990......2000".                  SQ2244.2
014900     05  FILLER      PIC X(048) VALUE                             SQ2244.2
015000     "......2010......2020......2030......2040....,...".          SQ2244.2
015100 01  TEST-RESULTS.                                                SQ2244.2
015200     02 FILLER                    PICTURE X VALUE SPACE.          SQ2244.2
015300     02 FEATURE                   PICTURE X(20) VALUE SPACE.      SQ2244.2
015400     02 FILLER                    PICTURE X VALUE SPACE.          SQ2244.2
015500     02 P-OR-F                    PICTURE X(5) VALUE SPACE.       SQ2244.2
015600     02 FILLER                    PICTURE X  VALUE SPACE.         SQ2244.2
015700     02  PAR-NAME.                                                SQ2244.2
015800       03 FILLER PICTURE X(12) VALUE SPACE.                       SQ2244.2
015900       03  PARDOT-X PICTURE X  VALUE SPACE.                       SQ2244.2
016000       03 DOTVALUE PICTURE 99  VALUE ZERO.                        SQ2244.2
016100       03 FILLER PIC X(5) VALUE SPACE.                            SQ2244.2
016200     02 FILLER PIC X(10) VALUE SPACE.                             SQ2244.2
016300     02 RE-MARK PIC X(61).                                        SQ2244.2
016400 01  TEST-COMPUTED.                                               SQ2244.2
016500     02 FILLER PIC X(30) VALUE SPACE.                             SQ2244.2
016600     02 FILLER PIC X(17) VALUE "       COMPUTED=".                SQ2244.2
016700     02 COMPUTED-X.                                               SQ2244.2
016800     03 COMPUTED-A                PICTURE X(20) VALUE SPACE.      SQ2244.2
016900     03 COMPUTED-N REDEFINES COMPUTED-A PICTURE -9(9).9(9).       SQ2244.2
017000     03 COMPUTED-0V18 REDEFINES COMPUTED-A  PICTURE -.9(18).      SQ2244.2
017100     03 COMPUTED-4V14 REDEFINES COMPUTED-A  PICTURE -9(4).9(14).  SQ2244.2
017200     03 COMPUTED-14V4 REDEFINES COMPUTED-A  PICTURE -9(14).9(4).  SQ2244.2
017300     03       CM-18V0 REDEFINES COMPUTED-A.                       SQ2244.2
017400         04 COMPUTED-18V0                   PICTURE -9(18).       SQ2244.2
017500         04 FILLER                          PICTURE X.            SQ2244.2
017600     03 FILLER PIC X(50) VALUE SPACE.                             SQ2244.2
017700 01  TEST-CORRECT.                                                SQ2244.2
017800     02 FILLER PIC X(30) VALUE SPACE.                             SQ2244.2
017900     02 FILLER PIC X(17) VALUE "       CORRECT =".                SQ2244.2
018000     02 CORRECT-X.                                                SQ2244.2
018100     03 CORRECT-A                 PICTURE X(20) VALUE SPACE.      SQ2244.2
018200     03 CORRECT-N REDEFINES CORRECT-A PICTURE -9(9).9(9).         SQ2244.2
018300     03 CORRECT-0V18 REDEFINES CORRECT-A    PICTURE -.9(18).      SQ2244.2
018400     03 CORRECT-4V14 REDEFINES CORRECT-A    PICTURE -9(4).9(14).  SQ2244.2
018500     03 CORRECT-14V4 REDEFINES CORRECT-A    PICTURE -9(14).9(4).  SQ2244.2
018600     03      CR-18V0 REDEFINES CORRECT-A.                         SQ2244.2
018700         04 CORRECT-18V0                    PICTURE -9(18).       SQ2244.2
018800         04 FILLER                          PICTURE X.            SQ2244.2
018900     03 FILLER PIC X(50) VALUE SPACE.                             SQ2244.2
019000 01  CCVS-C-1.                                                    SQ2244.2
019100     02 FILLER PICTURE IS X(99) VALUE IS " FEATURE              PASQ2244.2
019200-    "SS  PARAGRAPH-NAME                                          SQ2244.2
019300-    "        REMARKS".                                           SQ2244.2
019400     02 FILLER PICTURE IS X(20) VALUE IS SPACE.                   SQ2244.2
019500 01  CCVS-C-2.                                                    SQ2244.2
019600     02 FILLER PICTURE IS X VALUE IS SPACE.                       SQ2244.2
019700     02 FILLER PICTURE IS X(6) VALUE IS "TESTED".                 SQ2244.2
019800     02 FILLER PICTURE IS X(15) VALUE IS SPACE.                   SQ2244.2
019900     02 FILLER PICTURE IS X(4) VALUE IS "FAIL".                   SQ2244.2
020000     02 FILLER PICTURE IS X(94) VALUE IS SPACE.                   SQ2244.2
020100 01  REC-SKL-SUB PICTURE 9(2) VALUE ZERO.                         SQ2244.2
020200 01  REC-CT PICTURE 99 VALUE ZERO.                                SQ2244.2
020300 01  DELETE-CNT                   PICTURE 999  VALUE ZERO.        SQ2244.2
020400 01  ERROR-COUNTER PICTURE IS 999 VALUE IS ZERO.                  SQ2244.2
020500 01  INSPECT-COUNTER PIC 999 VALUE ZERO.                          SQ2244.2
020600 01  PASS-COUNTER PIC 999 VALUE ZERO.                             SQ2244.2
020700 01  TOTAL-ERROR PIC 999 VALUE ZERO.                              SQ2244.2
020800 01  ERROR-HOLD PIC 999 VALUE ZERO.                               SQ2244.2
020900 01  DUMMY-HOLD PIC X(120) VALUE SPACE.                           SQ2244.2
021000 01  RECORD-COUNT PIC 9(5) VALUE ZERO.                            SQ2244.2
021100 01  CCVS-H-1.                                                    SQ2244.2
021200     02  FILLER   PICTURE X(27)  VALUE SPACE.                     SQ2244.2
021300     02 FILLER PICTURE X(67) VALUE                                SQ2244.2
021400     " FEDERAL SOFTWARE TESTING CENTER COBOL COMPILER VALIDATION  SQ2244.2
021500-    " SYSTEM".                                                   SQ2244.2
021600     02  FILLER     PICTURE X(26)  VALUE SPACE.                   SQ2244.2
021700 01  CCVS-H-2.                                                    SQ2244.2
021800     02 FILLER PICTURE X(52) VALUE IS                             SQ2244.2
021900     "CCVS85 FSTC COPY, NOT FOR DISTRIBUTION.".                   SQ2244.2
022000     02 FILLER PICTURE IS X(19) VALUE IS "TEST RESULTS SET-  ".   SQ2244.2
022100     02 TEST-ID PICTURE IS X(9).                                  SQ2244.2
022200     02 FILLER PICTURE IS X(40) VALUE IS SPACE.                   SQ2244.2
022300 01  CCVS-H-3.                                                    SQ2244.2
022400     02  FILLER PICTURE X(34) VALUE                               SQ2244.2
022500     " FOR OFFICIAL USE ONLY    ".                                SQ2244.2
022600     02  FILLER PICTURE X(58) VALUE                               SQ2244.2
022700     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ2244.2
022800     02  FILLER PICTURE X(28) VALUE                               SQ2244.2
022900     "  COPYRIGHT   1985 ".                                       SQ2244.2
023000 01  CCVS-E-1.                                                    SQ2244.2
023100     02 FILLER PICTURE IS X(52) VALUE IS SPACE.                   SQ2244.2
023200     02 FILLER PICTURE IS X(14) VALUE IS "END OF TEST-  ".        SQ2244.2
023300     02 ID-AGAIN PICTURE IS X(9).                                 SQ2244.2
023400     02 FILLER PICTURE X(45) VALUE IS                             SQ2244.2
023500     " NTIS DISTRIBUTION COBOL 85".                               SQ2244.2
023600 01  CCVS-E-2.                                                    SQ2244.2
023700     02  FILLER                   PICTURE X(31)  VALUE            SQ2244.2
023800     SPACE.                                                       SQ2244.2
023900     02  FILLER                   PICTURE X(21)  VALUE SPACE.     SQ2244.2
024000     02 CCVS-E-2-2.                                               SQ2244.2
024100         03 ERROR-TOTAL PICTURE IS XXX VALUE IS SPACE.            SQ2244.2
024200         03 FILLER PICTURE IS X VALUE IS SPACE.                   SQ2244.2
024300         03 ENDER-DESC PIC X(46) VALUE "ERRORS ENCOUNTERED".      SQ2244.2
024400 01  CCVS-E-3.                                                    SQ2244.2
024500     02  FILLER PICTURE X(22) VALUE                               SQ2244.2
024600     " FOR OFFICIAL USE ONLY".                                    SQ2244.2
024700     02  FILLER PICTURE X(12) VALUE SPACE.                        SQ2244.2
024800     02  FILLER PICTURE X(58) VALUE                               SQ2244.2
024900     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ2244.2
025000     02  FILLER PICTURE X(13) VALUE SPACE.                        SQ2244.2
025100     02 FILLER PIC X(15) VALUE " COPYRIGHT 1985".                 SQ2244.2
025200 01  CCVS-E-4.                                                    SQ2244.2
025300     02 CCVS-E-4-1 PIC XXX VALUE SPACE.                           SQ2244.2
025400     02 FILLER PIC XXXX VALUE " OF ".                             SQ2244.2
025500     02 CCVS-E-4-2 PIC XXX VALUE SPACE.                           SQ2244.2
025600     02 FILLER PIC X(40) VALUE                                    SQ2244.2
025700      "  TESTS WERE EXECUTED SUCCESSFULLY".                       SQ2244.2
025800 01  XXINFO.                                                      SQ2244.2
025900     02 FILLER PIC X(30) VALUE "        *** INFORMATION  ***".    SQ2244.2
026000     02 INFO-TEXT.                                                SQ2244.2
026100     04 FILLER PIC X(20) VALUE SPACE.                             SQ2244.2
026200     04 XXCOMPUTED PIC X(20).                                     SQ2244.2
026300     04 FILLER PIC X(5) VALUE SPACE.                              SQ2244.2
026400     04 XXCORRECT PIC X(20).                                      SQ2244.2
026500 01  HYPHEN-LINE.                                                 SQ2244.2
026600     02 FILLER PICTURE IS X VALUE IS SPACE.                       SQ2244.2
026700     02 FILLER PICTURE IS X(65) VALUE IS "************************SQ2244.2
026800-    "*****************************************".                 SQ2244.2
026900     02 FILLER PICTURE IS X(54) VALUE IS "************************SQ2244.2
027000-    "******************************".                            SQ2244.2
027100 01  CCVS-PGM-ID PIC X(6) VALUE                                   SQ2244.2
027200     "SQ224A".                                                    SQ2244.2
027300 PROCEDURE DIVISION.                                              SQ2244.2
027400 CCVS1 SECTION.                                                   SQ2244.2
027500 OPEN-FILES.                                                      SQ2244.2
027600*    OPEN I-O RAW-DATA.                                           SQ2244.2
027700*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            SQ2244.2
027800*    READ RAW-DATA INVALID KEY GO TO END-E-1.                     SQ2244.2
027900*    MOVE "ABORTED " TO C-ABORT.                                  SQ2244.2
028000*    ADD 1 TO C-NO-OF-TESTS.                                      SQ2244.2
028100*    ACCEPT C-DATE  FROM DATE.                                    SQ2244.2
028200*    ACCEPT C-TIME  FROM TIME.                                    SQ2244.2
028300*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-1.             SQ2244.2
028400*END-E-1.                                                         SQ2244.2
028500*    CLOSE RAW-DATA.                                              SQ2244.2
028600     OPEN     OUTPUT PRINT-FILE.                                  SQ2244.2
028700     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   SQ2244.2
028800     MOVE    SPACE TO TEST-RESULTS.                               SQ2244.2
028900     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             SQ2244.2
029000     MOVE ZERO TO REC-SKL-SUB.                                    SQ2244.2
029100 CCVS-INIT-EXIT.                                                  SQ2244.2
029200     GO TO CCVS1-EXIT.                                            SQ2244.2
029300 CLOSE-FILES.                                                     SQ2244.2
029400     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   SQ2244.2
029500*    OPEN I-O RAW-DATA.                                           SQ2244.2
029600*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            SQ2244.2
029700*    READ RAW-DATA INVALID KEY GO TO END-E-2.                     SQ2244.2
029800*    MOVE "OK.     " TO C-ABORT.                                  SQ2244.2
029900*    MOVE PASS-COUNTER TO C-OK.                                   SQ2244.2
030000*    MOVE ERROR-HOLD   TO C-ALL.                                  SQ2244.2
030100*    MOVE ERROR-COUNTER TO C-FAIL.                                SQ2244.2
030200*    MOVE DELETE-CNT TO C-DELETED.                                SQ2244.2
030300*    MOVE INSPECT-COUNTER TO C-INSPECT.                           SQ2244.2
030400*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-2.             SQ2244.2
030500*END-E-2.                                                         SQ2244.2
030600*    CLOSE RAW-DATA.                                              SQ2244.2
030700 TERMINATE-CCVS.                                                  SQ2244.2
030800*    EXIT PROGRAM.                                                SQ2244.2
030900*TERMINATE-CALL.                                                  SQ2244.2
031000     STOP     RUN.                                                SQ2244.2
031100 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         SQ2244.2
031200 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           SQ2244.2
031300 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          SQ2244.2
031400 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-CNT.          SQ2244.2
031500     MOVE "****TEST DELETED****" TO RE-MARK.                      SQ2244.2
031600 PRINT-DETAIL.                                                    SQ2244.2
031700     IF REC-CT NOT EQUAL TO ZERO                                  SQ2244.2
031800             MOVE "." TO PARDOT-X                                 SQ2244.2
031900             MOVE REC-CT TO DOTVALUE.                             SQ2244.2
032000     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      SQ2244.2
032100     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               SQ2244.2
032200        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 SQ2244.2
032300          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 SQ2244.2
032400     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              SQ2244.2
032500     MOVE SPACE TO CORRECT-X.                                     SQ2244.2
032600     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         SQ2244.2
032700     MOVE     SPACE TO RE-MARK.                                   SQ2244.2
032800 HEAD-ROUTINE.                                                    SQ2244.2
032900     MOVE CCVS-H-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ2244.2
033000     MOVE CCVS-H-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.   SQ2244.2
033100     MOVE CCVS-H-3 TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.   SQ2244.2
033200 COLUMN-NAMES-ROUTINE.                                            SQ2244.2
033300     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ2244.2
033400     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ2244.2
033500     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ2244.2
033600 END-ROUTINE.                                                     SQ2244.2
033700     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.SQ2244.2
033800 END-RTN-EXIT.                                                    SQ2244.2
033900     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ2244.2
034000 END-ROUTINE-1.                                                   SQ2244.2
034100      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      SQ2244.2
034200      ERROR-HOLD. ADD DELETE-CNT TO ERROR-HOLD.                   SQ2244.2
034300      ADD PASS-COUNTER TO ERROR-HOLD.                             SQ2244.2
034400*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   SQ2244.2
034500      MOVE PASS-COUNTER TO CCVS-E-4-1.                            SQ2244.2
034600      MOVE ERROR-HOLD TO CCVS-E-4-2.                              SQ2244.2
034700      MOVE CCVS-E-4 TO CCVS-E-2-2.                                SQ2244.2
034800      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           SQ2244.2
034900  END-ROUTINE-12.                                                 SQ2244.2
035000      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        SQ2244.2
035100     IF       ERROR-COUNTER IS EQUAL TO ZERO                      SQ2244.2
035200         MOVE "NO " TO ERROR-TOTAL                                SQ2244.2
035300         ELSE                                                     SQ2244.2
035400         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       SQ2244.2
035500     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           SQ2244.2
035600     PERFORM WRITE-LINE.                                          SQ2244.2
035700 END-ROUTINE-13.                                                  SQ2244.2
035800     IF DELETE-CNT IS EQUAL TO ZERO                               SQ2244.2
035900         MOVE "NO " TO ERROR-TOTAL  ELSE                          SQ2244.2
036000         MOVE DELETE-CNT TO ERROR-TOTAL.                          SQ2244.2
036100     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   SQ2244.2
036200     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ2244.2
036300      IF   INSPECT-COUNTER EQUAL TO ZERO                          SQ2244.2
036400          MOVE "NO " TO ERROR-TOTAL                               SQ2244.2
036500      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   SQ2244.2
036600      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            SQ2244.2
036700      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          SQ2244.2
036800     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ2244.2
036900 WRITE-LINE.                                                      SQ2244.2
037000     ADD 1 TO RECORD-COUNT.                                       SQ2244.2
037100*    IF RECORD-COUNT GREATER 50                                   SQ2244.2
037200*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          SQ2244.2
037300*        MOVE SPACE TO DUMMY-RECORD                               SQ2244.2
037400*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  SQ2244.2
037500*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             SQ2244.2
037600*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     SQ2244.2
037700*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          SQ2244.2
037800*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          SQ2244.2
037900*        MOVE ZERO TO RECORD-COUNT.                               SQ2244.2
038000     PERFORM WRT-LN.                                              SQ2244.2
038100 WRT-LN.                                                          SQ2244.2
038200     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               SQ2244.2
038300     MOVE SPACE TO DUMMY-RECORD.                                  SQ2244.2
038400 BLANK-LINE-PRINT.                                                SQ2244.2
038500     PERFORM WRT-LN.                                              SQ2244.2
038600 FAIL-ROUTINE.                                                    SQ2244.2
038700     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.   SQ2244.2
038800     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.    SQ2244.2
038900     MOVE "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.    SQ2244.2
039000     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     SQ2244.2
039100     GO TO FAIL-ROUTINE-EX.                                       SQ2244.2
039200 FAIL-ROUTINE-WRITE.                                              SQ2244.2
039300     MOVE TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE           SQ2244.2
039400     MOVE TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES.   SQ2244.2
039500 FAIL-ROUTINE-EX. EXIT.                                           SQ2244.2
039600 BAIL-OUT.                                                        SQ2244.2
039700     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.       SQ2244.2
039800     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.               SQ2244.2
039900 BAIL-OUT-WRITE.                                                  SQ2244.2
040000     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  SQ2244.2
040100     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     SQ2244.2
040200 BAIL-OUT-EX. EXIT.                                               SQ2244.2
040300 CCVS1-EXIT.                                                      SQ2244.2
040400     EXIT.                                                        SQ2244.2
040500 SECT-SQ224A-0001 SECTION.                                        SQ2244.2
040600 WRITE-INIT-GF-01.                                                SQ2244.2
040700     MOVE ZERO TO COUNT-OF-RECS.                                  SQ2244.2
040800     MOVE 17   TO RECORD-LENGTH.                                  SQ2244.2
040900     OPEN OUTPUT SQ-VS7.                                          SQ2244.2
041000 WRITE-TEST-GF-01.                                                SQ2244.2
041100     PERFORM WRITE-RECORDS-1 1030 TIMES.                          SQ2244.2
041200     PERFORM WRITE-RECORDS-2 1001 TIMES.                          SQ2244.2
041300 WRITE-WRITE-GF-01.                                               SQ2244.2
041400     MOVE "CREATE FILE SQ-VS7" TO FEATURE.                        SQ2244.2
041500     MOVE "WRITE-TEST-GF-01" TO PAR-NAME.                         SQ2244.2
041600     MOVE "FILE CREATED, RECS =" TO COMPUTED-A.                   SQ2244.2
041700     MOVE COUNT-OF-RECS TO CORRECT-18V0.                          SQ2244.2
041800     MOVE "FILE HAS 18 THRU 2048 CHAR RECS" TO RE-MARK.           SQ2244.2
041900     PERFORM PRINT-DETAIL.                                        SQ2244.2
042000*        A SEQUENTIAL MASS STORAGE FILE CONTAINING 2031           SQ2244.2
042100*    RECORDS HAS BEEN CREATED.  THE FILE CONTAINS RECORDS         SQ2244.2
042200*    OF 18  THROUGH 2048 CHARACTERS BEGINNING WITH THE 18 CHAR RECSQ2244.2
042300*    AND ENDING WITH THE 2048 CHAR REC.                           SQ2244.2
042400*                                                                 SQ2244.2
042500 WRITE-CLOSE-GF-01.                                               SQ2244.2
042600     CLOSE SQ-VS7.                                                SQ2244.2
042700     GO TO READ-INIT-F1-01.                                       SQ2244.2
042800 WRITE-RECORDS-1.                                                 SQ2244.2
042900******************************************************************SQ2244.2
043000*   MOVE  ...  TO  OUTPUT-RECORD          1030 RECORDS           *SQ2244.2
043100*   WRITE   OUTPUT-RECORD.                                       *SQ2244.2
043200******************************************************************SQ2244.2
043300     ADD 1 TO COUNT-OF-RECS.                                      SQ2244.2
043400     ADD 1 TO RECORD-LENGTH.                                      SQ2244.2
043500     MOVE COUNT-OF-RECS TO RECORD-NUMBER.                         SQ2244.2
043600     MOVE VAR-RECORD-18-2048 TO SQ-VS7R1-FIRST.                   SQ2244.2
043700     WRITE SQ-VSR7R1-M-G-2048.                                    SQ2244.2
043800 WRITE-RECORDS-2.                                                 SQ2244.2
043900******************************************************************SQ2244.2
044000*WRITE ...  FROM  ....  .                 1001 RECORDS           *SQ2244.2
044100******************************************************************SQ2244.2
044200     ADD 1 TO COUNT-OF-RECS.                                      SQ2244.2
044300     ADD 1 TO RECORD-LENGTH.                                      SQ2244.2
044400     MOVE COUNT-OF-RECS TO RECORD-NUMBER.                         SQ2244.2
044500     WRITE SQ-VSR7R1-M-G-2048 FROM VAR-RECORD-18-2048.            SQ2244.2
044600 READ-INIT-F1-01.                                                 SQ2244.2
044700     MOVE 17   TO RECORD-LENGTH.                                  SQ2244.2
044800     MOVE ZERO TO COUNT-OF-RECS.                                  SQ2244.2
044900     MOVE ZERO TO EOF-FLAG.                                       SQ2244.2
045000     MOVE ZERO TO RECORDS-IN-ERROR.                               SQ2244.2
045100     MOVE ZERO TO ERROR-FLAG.                                     SQ2244.2
045200     OPEN INPUT SQ-VS7.                                           SQ2244.2
045300 READ-TEST-F1-01.                                                 SQ2244.2
045400     PERFORM READ-REC-1 THRU READ-REC-1-EXIT 1030 TIMES.          SQ2244.2
045500     IF EOF-FLAG EQUAL TO 1                                       SQ2244.2
045600         MOVE "EOF ON FIRST READ" TO RE-MARK                      SQ2244.2
045700         GO TO READ-EOF-F1-03.                                    SQ2244.2
045800     IF ERROR-FLAG EQUAL TO 1                                     SQ2244.2
045900         GO TO READ-FAIL-F1-01.                                   SQ2244.2
046000 READ-PASS-F1-01.                                                 SQ2244.2
046100     PERFORM PASS.                                                SQ2244.2
046200     GO TO READ-WRITE-F1-01.                                      SQ2244.2
046300 READ-FAIL-F1-01.                                                 SQ2244.2
046400     MOVE "ERROR:SEE VII-52 WRITE  OR VII-44 READ; VII-30 FORMAT  SQ2244.2
046500-         "RECORD VARYING . DEPENDING "           TO RE-MARK.     SQ2244.2
046600     PERFORM FAIL.                                                SQ2244.2
046700 READ-WRITE-F1-01.                                                SQ2244.2
046800     MOVE "READ 1030 RECORDS" TO FEATURE.                         SQ2244.2
046900     MOVE "READ-TEST-F1-01" TO PAR-NAME.                          SQ2244.2
047000     MOVE "EXPECTED RECORD LENGTH: 18 TO 1047" TO RE-MARK.        SQ2244.2
047100     MOVE RECORD-LENGTH TO COMPUTED-N.                            SQ2244.2
047200     ADD 17 TO COUNT-OF-RECS.                                     SQ2244.2
047300     MOVE COUNT-OF-RECS  TO CORRECT-N.                            SQ2244.2
047400     SUBTRACT 17 FROM COUNT-OF-RECS.                              SQ2244.2
047500     PERFORM PRINT-DETAIL.                                        SQ2244.2
047600     GO TO READ-INIT-F1-02.                                       SQ2244.2
047700 READ-REC-1.                                                      SQ2244.2
047800******************************************************************SQ2244.2
047900*   READ      <FILE>     AT END ...                              *SQ2244.2
048000******************************************************************SQ2244.2
048100     IF EOF-FLAG EQUAL TO 1                                       SQ2244.2
048200         GO TO READ-REC-1-EXIT.                                   SQ2244.2
048300     READ SQ-VS7 AT END                                           SQ2244.2
048400         MOVE 1 TO EOF-FLAG                                       SQ2244.2
048500         GO TO READ-REC-1-EXIT.                                   SQ2244.2
048600     ADD 1 TO COUNT-OF-RECS.                                      SQ2244.2
048700     MOVE SQ-VS7R1-FIRST TO  VAR-RECORD-18-2048.                  SQ2244.2
048800     ADD 17 TO COUNT-OF-RECS.                                     SQ2244.2
048900     IF  RECORD-LENGTH     NOT EQUAL TO COUNT-OF-RECS             SQ2244.2
049000         GO TO READ-REC-1-ERROR.                                  SQ2244.2
049100     SUBTRACT 17 FROM COUNT-OF-RECS.                              SQ2244.2
049200     GO TO READ-REC-1-EXIT.                                       SQ2244.2
049300 READ-REC-1-ERROR.                                                SQ2244.2
049400     SUBTRACT 17 FROM COUNT-OF-RECS.                              SQ2244.2
049500     ADD 1 TO RECORDS-IN-ERROR.                                   SQ2244.2
049600     MOVE 1 TO ERROR-FLAG.                                        SQ2244.2
049700 READ-REC-1-EXIT.                                                 SQ2244.2
049800     EXIT.                                                        SQ2244.2
049900 READ-REC-2.                                                      SQ2244.2
050000******************************************************************SQ2244.2
050100*   READ  <FILE> INTO ....  AT END                               *SQ2244.2
050200******************************************************************SQ2244.2
050300     READ SQ-VS7 INTO  VAR-RECORD-18-2048 AT END                  SQ2244.2
050400         MOVE 1 TO EOF-FLAG                                       SQ2244.2
050500         GO TO READ-REC-2-EXIT.                                   SQ2244.2
050600     ADD 1 TO COUNT-OF-RECS.                                      SQ2244.2
050700     ADD 17 TO COUNT-OF-RECS.                                     SQ2244.2
050800     IF  RECORD-LENGTH     NOT EQUAL TO COUNT-OF-RECS             SQ2244.2
050900         GO TO READ-REC-2-ERROR.                                  SQ2244.2
051000     SUBTRACT 17 FROM COUNT-OF-RECS.                              SQ2244.2
051100     GO TO READ-REC-2-EXIT.                                       SQ2244.2
051200 READ-REC-2-ERROR.                                                SQ2244.2
051300     SUBTRACT 17 FROM COUNT-OF-RECS.                              SQ2244.2
051400     MOVE 1 TO ERROR-FLAG.                                        SQ2244.2
051500 READ-REC-2-EXIT.                                                 SQ2244.2
051600     EXIT.                                                        SQ2244.2
051700 READ-INIT-F1-02.                                                 SQ2244.2
051800     MOVE ZERO TO ERROR-FLAG.                                     SQ2244.2
051900 READ-TEST-F1-02.                                                 SQ2244.2
052000     PERFORM READ-REC-2 THRU READ-REC-2-EXIT 1001 TIMES.          SQ2244.2
052100     IF EOF-FLAG EQUAL TO 1                                       SQ2244.2
052200         MOVE "EOF ON SECOND READ" TO RE-MARK                     SQ2244.2
052300         GO TO READ-EOF-F1-03.                                    SQ2244.2
052400     IF ERROR-FLAG EQUAL TO 1                                     SQ2244.2
052500         GO TO READ-FAIL-F1-02.                                   SQ2244.2
052600 READ-PASS-F1-02.                                                 SQ2244.2
052700     PERFORM PASS.                                                SQ2244.2
052800     GO TO READ-WRITE-F1-02.                                      SQ2244.2
052900 READ-FAIL-F1-02.                                                 SQ2244.2
053000     MOVE "ERROR:SEE VII-52 WRITE  OR VII-44 READ; VII-30 FORMAT  SQ2244.2
053100-         "RECORD VARYING . DEPENDING "           TO RE-MARK.     SQ2244.2
053200     MOVE RECORD-LENGTH TO COMPUTED-N.                            SQ2244.2
053300     ADD 17 TO COUNT-OF-RECS.                                     SQ2244.2
053400     MOVE COUNT-OF-RECS TO CORRECT-N.                             SQ2244.2
053500     SUBTRACT 17 FROM COUNT-OF-RECS.                              SQ2244.2
053600     PERFORM FAIL.                                                SQ2244.2
053700 READ-WRITE-F1-02.                                                SQ2244.2
053800     MOVE "READ 1000 RECORD" TO FEATURE.                          SQ2244.2
053900     MOVE "READ-TEST-F1-02" TO PAR-NAME.                          SQ2244.2
054000     MOVE "EXPECTED RECORD LENGTH: 1049 TO 2048" TO RE-MARK.      SQ2244.2
054100     PERFORM PRINT-DETAIL.                                        SQ2244.2
054200 READ-INIT-F1-03.                                                 SQ2244.2
054300     READ SQ-VS7 RECORD END                                       SQ2244.2
054400         GO TO READ-TEST-F1-03.                                   SQ2244.2
054500     MOVE "MORE THAN 2031 RECORDS" TO RE-MARK.                    SQ2244.2
054600     GO TO READ-FAIL-F1-03.                                       SQ2244.2
054700 READ-EOF-F1-03.                                                  SQ2244.2
054800     MOVE "RECORDS READ =" TO COMPUTED-A.                         SQ2244.2
054900     MOVE COUNT-OF-RECS TO CORRECT-18V0.                          SQ2244.2
055000     GO TO READ-FAIL-F1-03.                                       SQ2244.2
055100 READ-TEST-F1-03.                                                 SQ2244.2
055200     IF RECORDS-IN-ERROR NOT EQUAL TO ZERO                        SQ2244.2
055300         MOVE "RECORDS IN ERROR =" TO COMPUTED-A                  SQ2244.2
055400     MOVE RECORDS-IN-ERROR TO CORRECT-18V0                        SQ2244.2
055500         GO TO READ-FAIL-F1-03.                                   SQ2244.2
055600 READ-PASS-F1-03.                                                 SQ2244.2
055700     PERFORM PASS.                                                SQ2244.2
055800     GO TO READ-WRITE-F1-03.                                      SQ2244.2
055900 READ-FAIL-F1-03.                                                 SQ2244.2
056000     PERFORM FAIL.                                                SQ2244.2
056100 READ-WRITE-F1-03.                                                SQ2244.2
056200     MOVE "READ-TEST-F1-03" TO PAR-NAME.                          SQ2244.2
056300     MOVE "VERIFY FILE SQ-VS7" TO FEATURE.                        SQ2244.2
056400     PERFORM PRINT-DETAIL.                                        SQ2244.2
056500 READ-CLOSE-F1-03.                                                SQ2244.2
056600     CLOSE SQ-VS7.                                                SQ2244.2
056700 TERMINATE-ROUTINE.                                               SQ2244.2
056800     EXIT.                                                        SQ2244.2
056900 CCVS-EXIT SECTION.                                               SQ2244.2
057000 CCVS-999999.                                                     SQ2244.2
057100     GO TO CLOSE-FILES.                                           SQ2244.2
      *END-OF,SQ224A                                                                  
