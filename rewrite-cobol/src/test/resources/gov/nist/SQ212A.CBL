      *HEADER,COBOL,SQ212A                                                            
000100 IDENTIFICATION DIVISION.                                         SQ2124.2
000200 PROGRAM-ID.                                                      SQ2124.2
000300     SQ212A.                                                      SQ2124.2
000400****************************************************************  SQ2124.2
000500*                                                              *  SQ2124.2
000600*    VALIDATION FOR:-                                          *  SQ2124.2
000700*    " HIGH       ".                                              SQ2124.2
000800*                                                              *  SQ2124.2
000900*    CREATION DATE     /     VALIDATION DATE                   *  SQ2124.2
001000*    "4.2 ".                                                      SQ2124.2
001100*                                                              *  SQ2124.2
001200*        THIS ROUTINE CHECKS THE                                  SQ2124.2
001300*                   FILE STATUS VALUE  44 (BOUNDARY VIOLATION)    SQ2124.2
001400*                                                                 SQ2124.2
001500*         FOR:    WRITE SMALLER OR LAGER RECORDS                  SQ2124.2
001600*         AND:    REWRITE SMALLER OR LAGER RECORDS                SQ2124.2
001700*                                                                 SQ2124.2
001800*         FOR A FILE WITH VARIABLE LENGTH RECORDS FOLLOWING:      SQ2124.2
001900*                                                                 SQ2124.2
002000*           RECORD IS VARYING IN SIZE FROM 18 TO 2048 CHARACTERS  SQ2124.2
002100*             DEPENDING ON DATA-NAME-1.                           SQ2124.2
002200*                                                                 SQ2124.2
002300*    AN  ATTEMPT  IS  MADE  TO WRITE 3 SMALLER RECORDS. THEN 2031 SQ2124.2
002400*    RECORDS  SHOULD  BE WRITTEN AND THEN FUTHER 9 LARGER RECORDS SQ2124.2
002500*    SHOULD  CAUSE  A  BOUNDARY  VIOLATION  WITH  STATUS CODE 44. SQ2124.2
002600*    THEN  THE  FILE  IS  CLOSED  AND  OPENED  AGAIN  FOR  INPUT. SQ2124.2
002700*    2031  RECORDS WILL BE READ. THEN THE RECORD NO 2031 IS TRIED SQ2124.2
002800*    BE   READ.  THIS  READ  STATEMENT  MUST  CAUSE  THE  AT  END SQ2124.2
002900*    CONDITION.  IF  THERE  IS  ANOTHER RECORD, IT DOES MEAN THAT SQ2124.2
003000*    EITHER A SMALLER OR A LARGER RECORD HAVE BEEN WRITTEN.       SQ2124.2
003100*    (SEE VII-5; 1.3.7 AND VII-54; GR (13) ).                     SQ2124.2
003200*                                                                 SQ2124.2
003300*                                                                 SQ2124.2
003400*    THIS  ROUTINE  BUILDS  A  SEQUENTIAL MASS STORAGE FILE WHICH SQ2124.2
003500*    CONTAINS  2031 RECORDS OF A LENGTH OF 18 TO 2048 CHARACTERS. SQ2124.2
003600*    THE  MASS STORAGE FILE IS READ AND FIELDS IN THE RECORDS ARE SQ2124.2
003700*    CHECKED AGAINST THE EXPECTED VALUES.                         SQ2124.2
003800*                                                                 SQ2124.2
003900 ENVIRONMENT DIVISION.                                            SQ2124.2
004000 CONFIGURATION SECTION.                                           SQ2124.2
004100 SOURCE-COMPUTER.                                                 SQ2124.2
004200     XXXXX082.                                                    SQ2124.2
004300 OBJECT-COMPUTER.                                                 SQ2124.2
004400     XXXXX083.                                                    SQ2124.2
004500 INPUT-OUTPUT SECTION.                                            SQ2124.2
004600 FILE-CONTROL.                                                    SQ2124.2
004700*    SELECT RAW-DATA   ASSIGN TO                                  SQ2124.2
004800*    XXXXX062                                                     SQ2124.2
004900*           ORGANIZATION IS INDEXED                               SQ2124.2
005000*           ACCESS MODE IS RANDOM                                 SQ2124.2
005100*           RECORD KEY IS RAW-DATA-KEY.                           SQ2124.2
005200     SELECT PRINT-FILE ASSIGN TO                                  SQ2124.2
005300     XXXXX055.                                                    SQ2124.2
005400     SELECT SQ-VS7 ASSIGN TO                                      SQ2124.2
005500     XXXXX014                                                     SQ2124.2
005600     ORGANIZATION SEQUENTIAL                                      SQ2124.2
005700     ACCESS SEQUENTIAL                                            SQ2124.2
005800     STATUS IS SQ-VS7-STATUS.                                     SQ2124.2
005900 DATA DIVISION.                                                   SQ2124.2
006000 FILE SECTION.                                                    SQ2124.2
006100*                                                                 SQ2124.2
006200*FD  RAW-DATA.                                                    SQ2124.2
006300*                                                                 SQ2124.2
006400*01  RAW-DATA-SATZ.                                               SQ2124.2
006500*    05  RAW-DATA-KEY        PIC X(6).                            SQ2124.2
006600*    05  C-DATE              PIC 9(6).                            SQ2124.2
006700*    05  C-TIME              PIC 9(8).                            SQ2124.2
006800*    05  C-NO-OF-TESTS       PIC 99.                              SQ2124.2
006900*    05  C-OK                PIC 999.                             SQ2124.2
007000*    05  C-ALL               PIC 999.                             SQ2124.2
007100*    05  C-FAIL              PIC 999.                             SQ2124.2
007200*    05  C-DELETED           PIC 999.                             SQ2124.2
007300*    05  C-INSPECT           PIC 999.                             SQ2124.2
007400*    05  C-NOTE              PIC X(13).                           SQ2124.2
007500*    05  C-INDENT            PIC X.                               SQ2124.2
007600*    05  C-ABORT             PIC X(8).                            SQ2124.2
007700 FD  PRINT-FILE                                                   SQ2124.2
007800C    LABEL RECORDS                                                SQ2124.2
007900C    XXXXX084                                                     SQ2124.2
008000C    DATA RECORD IS PRINT-REC DUMMY-RECORD                        SQ2124.2
008100               .                                                  SQ2124.2
008200 01  PRINT-REC PICTURE X(120).                                    SQ2124.2
008300 01  DUMMY-RECORD PICTURE X(120).                                 SQ2124.2
008400 FD  SQ-VS7                                                       SQ2124.2
008500C    LABEL RECORDS ARE STANDARD                                   SQ2124.2
008600     RECORD IS VARYING IN SIZE FROM 18 TO 2048 CHARACTERS         SQ2124.2
008700       DEPENDING ON RECORD-LENGTH.                                SQ2124.2
008800 01  SQ-VSR7R1-M-G-2048.                                          SQ2124.2
008900     02  SQ-VS7R1-FIRST PICTURE X(2048).                          SQ2124.2
009000 WORKING-STORAGE SECTION.                                         SQ2124.2
009100 01  SWITCH-WRITE-REWRITE PICTURE 9   VALUE ZERO.                 SQ2124.2
009200 01  RECORD-LENGTH       PICTURE 9999 VALUE ZERO.                 SQ2124.2
009300 01  SQ-VS7-STATUS       PICTURE XX   VALUE SPACES.               SQ2124.2
009400 01  SAVE-COUNT-OF-RECS  PICTURE X(5) VALUE SPACE.                SQ2124.2
009500 01  COUNT-OF-RECS  PICTURE S9(5) COMPUTATIONAL.                  SQ2124.2
009600 01  RECORDS-IN-ERROR  PICTURE S9(5) COMPUTATIONAL                SQ2124.2
009700                              VALUE ZERO.                         SQ2124.2
009800 01  ERROR-FLAG PICTURE 9.                                        SQ2124.2
009900 01  EOF-FLAG  PICTURE 9.                                         SQ2124.2
010000 01  DUMP-AREA.                                                   SQ2124.2
010100     02  TYPE-OF-REC PICTURE X(5).                                SQ2124.2
010200     02  RECNO  PICTURE 9(5).                                     SQ2124.2
010300     02  REC-FILLER PICTURE X(21).                                SQ2124.2
010400     02  REC-FILLER PICTURE X(21).                                SQ2124.2
010500 01  VAR-RECORD-18-2048.                                          SQ2124.2
010600     05  FILLER PIC X(13) VALUE "SQ-VS7LENGTH=".                  SQ2124.2
010700     05  RECORD-NUMBER PIC 9999 VALUE ZERO.                       SQ2124.2
010800     05  FILLER      PIC X(100) VALUE                             SQ2124.2
010900     "........10........20........30........40........50........60SQ2124.2
011000-    "........70........80........90.......100".                  SQ2124.2
011100     05  FILLER      PIC X(100) VALUE                             SQ2124.2
011200     ".......110.......120.......130.......140.......150.......160SQ2124.2
011300-    ".......170.......180.......190.......200".                  SQ2124.2
011400     05  FILLER      PIC X(100) VALUE                             SQ2124.2
011500     ".......210.......220.......230.......240.......250.......260SQ2124.2
011600-    ".......270.......280.......290.......300".                  SQ2124.2
011700     05  FILLER      PIC X(100) VALUE                             SQ2124.2
011800     ".......310.......320.......330.......340.......350.......360SQ2124.2
011900-    ".......370.......380.......390.......400".                  SQ2124.2
012000     05  FILLER      PIC X(100) VALUE                             SQ2124.2
012100     ".......410.......420.......430.......440.......450.......460SQ2124.2
012200-    ".......470.......480.......490.......500".                  SQ2124.2
012300     05  FILLER      PIC X(100) VALUE                             SQ2124.2
012400     ".......510.......520.......530.......540.......550.......560SQ2124.2
012500-    ".......570.......580.......590.......600".                  SQ2124.2
012600     05  FILLER      PIC X(100) VALUE                             SQ2124.2
012700     ".......610.......620.......630.......640.......650.......660SQ2124.2
012800-    ".......670.......680.......690.......700".                  SQ2124.2
012900     05  FILLER      PIC X(100) VALUE                             SQ2124.2
013000     ".......710.......720.......730.......740.......750.......760SQ2124.2
013100-    ".......770.......780.......790.......800".                  SQ2124.2
013200     05  FILLER      PIC X(100) VALUE                             SQ2124.2
013300     ".......810.......820.......830.......840.......850.......860SQ2124.2
013400-    ".......870.......880.......890.......900".                  SQ2124.2
013500     05  FILLER      PIC X(100) VALUE                             SQ2124.2
013600     ".......910.......920.......930.......940.......950.......960SQ2124.2
013700-    ".......970.......980.......990......1000".                  SQ2124.2
013800     05  FILLER      PIC X(100) VALUE                             SQ2124.2
013900     "......1010......1020......1030......1040......1050......1060SQ2124.2
014000-    "......1070......1080......1090......1100".                  SQ2124.2
014100     05  FILLER      PIC X(100) VALUE                             SQ2124.2
014200     "......1110......1120......1130......1140......1150......1160SQ2124.2
014300-    "......1170......1180......1190......1200".                  SQ2124.2
014400     05  FILLER      PIC X(100) VALUE                             SQ2124.2
014500     "......1210......1220......1230......1240......1250......1260SQ2124.2
014600-    ".......270.......280.......290.......300".                  SQ2124.2
014700     05  FILLER      PIC X(100) VALUE                             SQ2124.2
014800     "......1310......1320......1330......1340......1350......1360SQ2124.2
014900-    "......1370......1380......1390......1400".                  SQ2124.2
015000     05  FILLER      PIC X(100) VALUE                             SQ2124.2
015100     "......1410......1420......1430......1440......1450......1460SQ2124.2
015200-    "......1470......1480......1490......1500".                  SQ2124.2
015300     05  FILLER      PIC X(100) VALUE                             SQ2124.2
015400     "......1510......1520......1530......1540......1550......1560SQ2124.2
015500-    "......1570......1580......1590......1600".                  SQ2124.2
015600     05  FILLER      PIC X(100) VALUE                             SQ2124.2
015700     "......1610......1620......1630......1640......1650......1660SQ2124.2
015800-    "......1670......1680......1690......1700".                  SQ2124.2
015900     05  FILLER      PIC X(100) VALUE                             SQ2124.2
016000     "......1710......1720......1730......1740......1750......1760SQ2124.2
016100-    "......1770......1780......1790......1800".                  SQ2124.2
016200     05  FILLER      PIC X(100) VALUE                             SQ2124.2
016300     "......1810......1820......1830......1840......1850......1860SQ2124.2
016400-    "......1870......1880......1890......1900".                  SQ2124.2
016500     05  FILLER      PIC X(100) VALUE                             SQ2124.2
016600     "......1910......1920......1930......1940......1950......1960SQ2124.2
016700-    "......1970......1980......1990......2000".                  SQ2124.2
016800     05  FILLER      PIC X(100) VALUE                             SQ2124.2
016900     "......2010......2020......2030......2040....,...".          SQ2124.2
017000 01  TEST-RESULTS.                                                SQ2124.2
017100     02 FILLER                    PICTURE X VALUE SPACE.          SQ2124.2
017200     02 FEATURE                   PICTURE X(20) VALUE SPACE.      SQ2124.2
017300     02 FILLER                    PICTURE X VALUE SPACE.          SQ2124.2
017400     02 P-OR-F                    PICTURE X(5) VALUE SPACE.       SQ2124.2
017500     02 FILLER                    PICTURE X  VALUE SPACE.         SQ2124.2
017600     02  PAR-NAME.                                                SQ2124.2
017700       03 FILLER PICTURE X(12) VALUE SPACE.                       SQ2124.2
017800       03  PARDOT-X PICTURE X  VALUE SPACE.                       SQ2124.2
017900       03 DOTVALUE PICTURE 99  VALUE ZERO.                        SQ2124.2
018000       03 FILLER PIC X(5) VALUE SPACE.                            SQ2124.2
018100     02 FILLER PIC X(10) VALUE SPACE.                             SQ2124.2
018200     02 RE-MARK PIC X(61).                                        SQ2124.2
018300 01  TEST-COMPUTED.                                               SQ2124.2
018400     02 FILLER PIC X(30) VALUE SPACE.                             SQ2124.2
018500     02 FILLER PIC X(17) VALUE "       COMPUTED=".                SQ2124.2
018600     02 COMPUTED-X.                                               SQ2124.2
018700     03 COMPUTED-A                PICTURE X(20) VALUE SPACE.      SQ2124.2
018800     03 COMPUTED-N REDEFINES COMPUTED-A PICTURE -9(9).9(9).       SQ2124.2
018900     03 COMPUTED-0V18 REDEFINES COMPUTED-A  PICTURE -.9(18).      SQ2124.2
019000     03 COMPUTED-4V14 REDEFINES COMPUTED-A  PICTURE -9(4).9(14).  SQ2124.2
019100     03 COMPUTED-14V4 REDEFINES COMPUTED-A  PICTURE -9(14).9(4).  SQ2124.2
019200     03       CM-18V0 REDEFINES COMPUTED-A.                       SQ2124.2
019300         04 COMPUTED-18V0                   PICTURE -9(18).       SQ2124.2
019400         04 FILLER                          PICTURE X.            SQ2124.2
019500     03 FILLER PIC X(50) VALUE SPACE.                             SQ2124.2
019600 01  TEST-CORRECT.                                                SQ2124.2
019700     02 FILLER PIC X(30) VALUE SPACE.                             SQ2124.2
019800     02 FILLER PIC X(17) VALUE "       CORRECT =".                SQ2124.2
019900     02 CORRECT-X.                                                SQ2124.2
020000     03 CORRECT-A                 PICTURE X(20) VALUE SPACE.      SQ2124.2
020100     03 CORRECT-N REDEFINES CORRECT-A PICTURE -9(9).9(9).         SQ2124.2
020200     03 CORRECT-0V18 REDEFINES CORRECT-A    PICTURE -.9(18).      SQ2124.2
020300     03 CORRECT-4V14 REDEFINES CORRECT-A    PICTURE -9(4).9(14).  SQ2124.2
020400     03 CORRECT-14V4 REDEFINES CORRECT-A    PICTURE -9(14).9(4).  SQ2124.2
020500     03      CR-18V0 REDEFINES CORRECT-A.                         SQ2124.2
020600         04 CORRECT-18V0                    PICTURE -9(18).       SQ2124.2
020700         04 FILLER                          PICTURE X.            SQ2124.2
020800     03 FILLER PIC X(50) VALUE SPACE.                             SQ2124.2
020900 01  CCVS-C-1.                                                    SQ2124.2
021000     02 FILLER PICTURE IS X(99) VALUE IS " FEATURE              PASQ2124.2
021100-    "SS  PARAGRAPH-NAME                                          SQ2124.2
021200-    "        REMARKS".                                           SQ2124.2
021300     02 FILLER PICTURE IS X(20) VALUE IS SPACE.                   SQ2124.2
021400 01  CCVS-C-2.                                                    SQ2124.2
021500     02 FILLER PICTURE IS X VALUE IS SPACE.                       SQ2124.2
021600     02 FILLER PICTURE IS X(6) VALUE IS "TESTED".                 SQ2124.2
021700     02 FILLER PICTURE IS X(15) VALUE IS SPACE.                   SQ2124.2
021800     02 FILLER PICTURE IS X(4) VALUE IS "FAIL".                   SQ2124.2
021900     02 FILLER PICTURE IS X(94) VALUE IS SPACE.                   SQ2124.2
022000 01  REC-SKL-SUB PICTURE 9(2) VALUE ZERO.                         SQ2124.2
022100 01  REC-CT PICTURE 99 VALUE ZERO.                                SQ2124.2
022200 01  DELETE-CNT                   PICTURE 999  VALUE ZERO.        SQ2124.2
022300 01  ERROR-COUNTER PICTURE IS 999 VALUE IS ZERO.                  SQ2124.2
022400 01  INSPECT-COUNTER PIC 999 VALUE ZERO.                          SQ2124.2
022500 01  PASS-COUNTER PIC 999 VALUE ZERO.                             SQ2124.2
022600 01  TOTAL-ERROR PIC 999 VALUE ZERO.                              SQ2124.2
022700 01  ERROR-HOLD PIC 999 VALUE ZERO.                               SQ2124.2
022800 01  DUMMY-HOLD PIC X(120) VALUE SPACE.                           SQ2124.2
022900 01  RECORD-COUNT PIC 9(5) VALUE ZERO.                            SQ2124.2
023000 01  CCVS-H-1.                                                    SQ2124.2
023100     02  FILLER   PICTURE X(27)  VALUE SPACE.                     SQ2124.2
023200     02 FILLER PICTURE X(67) VALUE                                SQ2124.2
023300     " FEDERAL SOFTWARE TESTING CENTER COBOL COMPILER VALIDATION  SQ2124.2
023400-    " SYSTEM".                                                   SQ2124.2
023500     02  FILLER     PICTURE X(26)  VALUE SPACE.                   SQ2124.2
023600 01  CCVS-H-2.                                                    SQ2124.2
023700     02 FILLER PICTURE X(52) VALUE IS                             SQ2124.2
023800     "CCVS85 FSTC COPY, NOT FOR DISTRIBUTION.".                   SQ2124.2
023900     02 FILLER PICTURE IS X(19) VALUE IS "TEST RESULTS SET-  ".   SQ2124.2
024000     02 TEST-ID PICTURE IS X(9).                                  SQ2124.2
024100     02 FILLER PICTURE IS X(40) VALUE IS SPACE.                   SQ2124.2
024200 01  CCVS-H-3.                                                    SQ2124.2
024300     02  FILLER PICTURE X(34) VALUE                               SQ2124.2
024400     " FOR OFFICIAL USE ONLY    ".                                SQ2124.2
024500     02  FILLER PICTURE X(58) VALUE                               SQ2124.2
024600     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ2124.2
024700     02  FILLER PICTURE X(28) VALUE                               SQ2124.2
024800     "  COPYRIGHT   1985 ".                                       SQ2124.2
024900 01  CCVS-E-1.                                                    SQ2124.2
025000     02 FILLER PICTURE IS X(52) VALUE IS SPACE.                   SQ2124.2
025100     02 FILLER PICTURE IS X(14) VALUE IS "END OF TEST-  ".        SQ2124.2
025200     02 ID-AGAIN PICTURE IS X(9).                                 SQ2124.2
025300     02 FILLER PICTURE X(45) VALUE IS                             SQ2124.2
025400     " NTIS DISTRIBUTION COBOL 85".                               SQ2124.2
025500 01  CCVS-E-2.                                                    SQ2124.2
025600     02  FILLER                   PICTURE X(31)  VALUE            SQ2124.2
025700     SPACE.                                                       SQ2124.2
025800     02  FILLER                   PICTURE X(21)  VALUE SPACE.     SQ2124.2
025900     02 CCVS-E-2-2.                                               SQ2124.2
026000         03 ERROR-TOTAL PICTURE IS XXX VALUE IS SPACE.            SQ2124.2
026100         03 FILLER PICTURE IS X VALUE IS SPACE.                   SQ2124.2
026200         03 ENDER-DESC PIC X(46) VALUE "ERRORS ENCOUNTERED".      SQ2124.2
026300 01  CCVS-E-3.                                                    SQ2124.2
026400     02  FILLER PICTURE X(22) VALUE                               SQ2124.2
026500     " FOR OFFICIAL USE ONLY".                                    SQ2124.2
026600     02  FILLER PICTURE X(12) VALUE SPACE.                        SQ2124.2
026700     02  FILLER PICTURE X(58) VALUE                               SQ2124.2
026800     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ2124.2
026900     02  FILLER PICTURE X(13) VALUE SPACE.                        SQ2124.2
027000     02 FILLER PIC X(15) VALUE " COPYRIGHT 1985".                 SQ2124.2
027100 01  CCVS-E-4.                                                    SQ2124.2
027200     02 CCVS-E-4-1 PIC XXX VALUE SPACE.                           SQ2124.2
027300     02 FILLER PIC XXXX VALUE " OF ".                             SQ2124.2
027400     02 CCVS-E-4-2 PIC XXX VALUE SPACE.                           SQ2124.2
027500     02 FILLER PIC X(40) VALUE                                    SQ2124.2
027600      "  TESTS WERE EXECUTED SUCCESSFULLY".                       SQ2124.2
027700 01  XXINFO.                                                      SQ2124.2
027800     02 FILLER PIC X(30) VALUE "        *** INFORMATION  ***".    SQ2124.2
027900     02 INFO-TEXT.                                                SQ2124.2
028000     04 FILLER PIC X(20) VALUE SPACE.                             SQ2124.2
028100     04 XXCOMPUTED PIC X(20).                                     SQ2124.2
028200     04 FILLER PIC X(5) VALUE SPACE.                              SQ2124.2
028300     04 XXCORRECT PIC X(20).                                      SQ2124.2
028400 01  HYPHEN-LINE.                                                 SQ2124.2
028500     02 FILLER PICTURE IS X VALUE IS SPACE.                       SQ2124.2
028600     02 FILLER PICTURE IS X(65) VALUE IS "************************SQ2124.2
028700-    "*****************************************".                 SQ2124.2
028800     02 FILLER PICTURE IS X(54) VALUE IS "************************SQ2124.2
028900-    "******************************".                            SQ2124.2
029000 01  CCVS-PGM-ID PIC X(6) VALUE                                   SQ2124.2
029100     "SQ212A".                                                    SQ2124.2
029200 PROCEDURE DIVISION.                                              SQ2124.2
029300 DECLARATIVES.                                                    SQ2124.2
029400 SECT-SQ212A-0001 SECTION.                                        SQ2124.2
029500     USE AFTER STANDARD EXCEPTION PROCEDURE ON SQ-VS7.            SQ2124.2
029600 TEST-STATUS-44-00.                                               SQ2124.2
029700     IF SWITCH-WRITE-REWRITE = 1                                  SQ2124.2
029800         GO TO TEST-STATUS-44-0.                                  SQ2124.2
029900     GO TO TEST-STATUS-44-1-0.                                    SQ2124.2
030000 TEST-STATUS-44-0.                                                SQ2124.2
030100     IF SQ-VS7-STATUS  = "44"                                     SQ2124.2
030200         GO TO TEST-STATUS-44-PASS.                               SQ2124.2
030300 TEST-STATUS-44-FAIL.                                             SQ2124.2
030400     MOVE "VII-4 (3) D. 1)" TO RE-MARK.                           SQ2124.2
030500     PERFORM FAIL1.                                               SQ2124.2
030600     MOVE SQ-VS7-STATUS TO COMPUTED-A.                            SQ2124.2
030700     MOVE "44" TO CORRECT-A.                                      SQ2124.2
030800     GO TO TEST-STATUS-44-WRITE.                                  SQ2124.2
030900 TEST-STATUS-44-PASS.                                             SQ2124.2
031000     PERFORM PASS1.                                               SQ2124.2
031100 TEST-STATUS-44-WRITE.                                            SQ2124.2
031200     MOVE "DECL-STATUS-44-0" TO PAR-NAME.                         SQ2124.2
031300     PERFORM PRINT-DETAIL1.                                       SQ2124.2
031400     ADD 1 TO RECORDS-IN-ERROR.                                   SQ2124.2
031500     GO TO EXIT-PARA.                                             SQ2124.2
031600 TEST-STATUS-44-1-0.                                              SQ2124.2
031700     IF SQ-VS7-STATUS  = "44"                                     SQ2124.2
031800         GO TO TEST-STATUS-44-1-PASS.                             SQ2124.2
031900 TEST-STATUS-44-1-FAIL.                                           SQ2124.2
032000     MOVE "VII-4 (3) D. 1)" TO RE-MARK.                           SQ2124.2
032100     PERFORM FAIL1.                                               SQ2124.2
032200     MOVE SQ-VS7-STATUS TO COMPUTED-A.                            SQ2124.2
032300     MOVE "44" TO CORRECT-A.                                      SQ2124.2
032400     GO TO TEST-STATUS-44-1-WRITE.                                SQ2124.2
032500 TEST-STATUS-44-1-PASS.                                           SQ2124.2
032600     PERFORM PASS1.                                               SQ2124.2
032700 TEST-STATUS-44-1-WRITE.                                          SQ2124.2
032800*          RWRT SHORTER & LONGER                                  SQ2124.2
032900     PERFORM PRINT-DETAIL1.                                       SQ2124.2
033000     ADD 1 TO RECORDS-IN-ERROR.                                   SQ2124.2
033100     GO TO EXIT-PARA.                                             SQ2124.2
033200 PASS1.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.          SQ2124.2
033300 FAIL1.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.         SQ2124.2
033400 PRINT-DETAIL1.                                                   SQ2124.2
033500     IF REC-CT NOT EQUAL TO ZERO                                  SQ2124.2
033600             MOVE "." TO PARDOT-X                                 SQ2124.2
033700             MOVE REC-CT TO DOTVALUE.                             SQ2124.2
033800     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE1.     SQ2124.2
033900     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE1              SQ2124.2
034000        PERFORM FAIL-ROUTINE1 THRU FAIL-ROUTINE-EX1               SQ2124.2
034100          ELSE PERFORM BAIL-OUT1 THRU BAIL-OUT-EX1.               SQ2124.2
034200     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              SQ2124.2
034300     MOVE SPACE TO CORRECT-X.                                     SQ2124.2
034400     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         SQ2124.2
034500     MOVE     SPACE TO RE-MARK.                                   SQ2124.2
034600 END-ROUTINE1.                                                    SQ2124.2
034700     MOVE HYPHEN-LINE TO DUMMY-RECORD.                            SQ2124.2
034800     PERFORM WRITE-LINE1 5 TIMES.                                 SQ2124.2
034900 END-RTN1-EXIT.                                                   SQ2124.2
035000     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE1 2 TIMES.  SQ2124.2
035100 END-ROUTINE1-1.                                                  SQ2124.2
035200      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      SQ2124.2
035300      ERROR-HOLD. ADD DELETE-CNT TO ERROR-HOLD.                   SQ2124.2
035400      ADD PASS-COUNTER TO ERROR-HOLD.                             SQ2124.2
035500*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE1-12.  SQ2124.2
035600      MOVE PASS-COUNTER TO CCVS-E-4-1.                            SQ2124.2
035700      MOVE ERROR-HOLD TO CCVS-E-4-2.                              SQ2124.2
035800      MOVE CCVS-E-4 TO CCVS-E-2-2.                                SQ2124.2
035900      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE1.          SQ2124.2
036000  END-ROUTINE1-12.                                                SQ2124.2
036100      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        SQ2124.2
036200     IF       ERROR-COUNTER IS EQUAL TO ZERO                      SQ2124.2
036300         MOVE "NO " TO ERROR-TOTAL                                SQ2124.2
036400         ELSE                                                     SQ2124.2
036500         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       SQ2124.2
036600     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           SQ2124.2
036700     PERFORM WRITE-LINE1.                                         SQ2124.2
036800 END-ROUTINE1-13.                                                 SQ2124.2
036900     IF DELETE-CNT IS EQUAL TO ZERO                               SQ2124.2
037000         MOVE "NO " TO ERROR-TOTAL  ELSE                          SQ2124.2
037100         MOVE DELETE-CNT TO ERROR-TOTAL.                          SQ2124.2
037200     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   SQ2124.2
037300     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE1.          SQ2124.2
037400      IF   INSPECT-COUNTER EQUAL TO ZERO                          SQ2124.2
037500          MOVE "NO " TO ERROR-TOTAL                               SQ2124.2
037600      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   SQ2124.2
037700      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            SQ2124.2
037800      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE1.         SQ2124.2
037900     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE1.          SQ2124.2
038000 WRITE-LINE1.                                                     SQ2124.2
038100     ADD 1 TO RECORD-COUNT.                                       SQ2124.2
038200*    IF RECORD-COUNT GREATER 50                                   SQ2124.2
038300*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          SQ2124.2
038400*        MOVE SPACE TO DUMMY-RECORD                               SQ2124.2
038500*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  SQ2124.2
038600*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN1            SQ2124.2
038700*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN1 2 TIMES    SQ2124.2
038800*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN1         SQ2124.2
038900*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          SQ2124.2
039000*        MOVE ZERO TO RECORD-COUNT.                               SQ2124.2
039100     PERFORM WRT-LN1.                                             SQ2124.2
039200 WRT-LN1.                                                         SQ2124.2
039300     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               SQ2124.2
039400     MOVE SPACE TO DUMMY-RECORD.                                  SQ2124.2
039500 FAIL-ROUTINE1.                                                   SQ2124.2
039600     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE1.  SQ2124.2
039700     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE1.   SQ2124.2
039800     MOVE "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.    SQ2124.2
039900     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE1 2 TIMES.    SQ2124.2
040000     GO TO FAIL-ROUTINE-EX1.                                      SQ2124.2
040100 FAIL-ROUTINE-WRITE1.                                             SQ2124.2
040200     MOVE TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE1          SQ2124.2
040300     MOVE TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE1 2 TIMES.  SQ2124.2
040400 FAIL-ROUTINE-EX1. EXIT.                                          SQ2124.2
040500 BAIL-OUT1.                                                       SQ2124.2
040600     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE1.      SQ2124.2
040700     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX1.              SQ2124.2
040800 BAIL-OUT-WRITE1.                                                 SQ2124.2
040900     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  SQ2124.2
041000     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE1 2 TIMES.    SQ2124.2
041100 BAIL-OUT-EX1. EXIT.                                              SQ2124.2
041200 EXIT-PARA.                                                       SQ2124.2
041300     EXIT.                                                        SQ2124.2
041400 CLOSE-FILES1.                                                    SQ2124.2
041500     PERFORM END-ROUTINE1 THRU END-ROUTINE1-13. CLOSE PRINT-FILE. SQ2124.2
041600*    OPEN I-O RAW-DATA.                                           SQ2124.2
041700*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            SQ2124.2
041800*    READ RAW-DATA INVALID KEY GO TO END1-E-2.                    SQ2124.2
041900*    MOVE "OK.     " TO C-ABORT.                                  SQ2124.2
042000*    MOVE PASS-COUNTER TO C-OK.                                   SQ2124.2
042100*    MOVE ERROR-HOLD   TO C-ALL.                                  SQ2124.2
042200*    MOVE ERROR-COUNTER TO C-FAIL.                                SQ2124.2
042300*    MOVE DELETE-CNT TO C-DELETED.                                SQ2124.2
042400*    MOVE INSPECT-COUNTER TO C-INSPECT.                           SQ2124.2
042500*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END1-E-2.            SQ2124.2
042600*END1-E-2.                                                        SQ2124.2
042700*    CLOSE RAW-DATA.                                              SQ2124.2
042800 TERMINATE1-CCVS.                                                 SQ2124.2
042900*    EXIT PROGRAM.                                                SQ2124.2
043000*TERMINATE1-CALL.                                                 SQ2124.2
043100     STOP     RUN.                                                SQ2124.2
043200 END DECLARATIVES.                                                SQ2124.2
043300 CCVS1 SECTION.                                                   SQ2124.2
043400 OPEN-FILES.                                                      SQ2124.2
043500*    OPEN I-O RAW-DATA.                                           SQ2124.2
043600*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            SQ2124.2
043700*    READ RAW-DATA INVALID KEY GO TO END-E-1.                     SQ2124.2
043800*    MOVE "ABORTED " TO C-ABORT.                                  SQ2124.2
043900*    ADD 1 TO C-NO-OF-TESTS.                                      SQ2124.2
044000*    ACCEPT C-DATE  FROM DATE.                                    SQ2124.2
044100*    ACCEPT C-TIME  FROM TIME.                                    SQ2124.2
044200*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-1.             SQ2124.2
044300*END-E-1.                                                         SQ2124.2
044400*    CLOSE RAW-DATA.                                              SQ2124.2
044500     OPEN     OUTPUT PRINT-FILE.                                  SQ2124.2
044600     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   SQ2124.2
044700     MOVE    SPACE TO TEST-RESULTS.                               SQ2124.2
044800     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             SQ2124.2
044900     MOVE ZERO TO REC-SKL-SUB.                                    SQ2124.2
045000 CCVS-INIT-EXIT.                                                  SQ2124.2
045100     GO TO CCVS1-EXIT.                                            SQ2124.2
045200 CLOSE-FILES.                                                     SQ2124.2
045300     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   SQ2124.2
045400*    OPEN I-O RAW-DATA.                                           SQ2124.2
045500*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            SQ2124.2
045600*    READ RAW-DATA INVALID KEY GO TO END-E-2.                     SQ2124.2
045700*    MOVE "OK.     " TO C-ABORT.                                  SQ2124.2
045800*    MOVE PASS-COUNTER TO C-OK.                                   SQ2124.2
045900*    MOVE ERROR-HOLD   TO C-ALL.                                  SQ2124.2
046000*    MOVE ERROR-COUNTER TO C-FAIL.                                SQ2124.2
046100*    MOVE DELETE-CNT TO C-DELETED.                                SQ2124.2
046200*    MOVE INSPECT-COUNTER TO C-INSPECT.                           SQ2124.2
046300*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-2.             SQ2124.2
046400*END-E-2.                                                         SQ2124.2
046500*    CLOSE RAW-DATA.                                              SQ2124.2
046600 TERMINATE-CCVS.                                                  SQ2124.2
046700*    EXIT PROGRAM.                                                SQ2124.2
046800*TERMINATE-CALL.                                                  SQ2124.2
046900     STOP     RUN.                                                SQ2124.2
047000 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         SQ2124.2
047100 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           SQ2124.2
047200 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          SQ2124.2
047300 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-CNT.          SQ2124.2
047400     MOVE "****TEST DELETED****" TO RE-MARK.                      SQ2124.2
047500 PRINT-DETAIL.                                                    SQ2124.2
047600     IF REC-CT NOT EQUAL TO ZERO                                  SQ2124.2
047700             MOVE "." TO PARDOT-X                                 SQ2124.2
047800             MOVE REC-CT TO DOTVALUE.                             SQ2124.2
047900     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      SQ2124.2
048000     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               SQ2124.2
048100        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 SQ2124.2
048200          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 SQ2124.2
048300     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              SQ2124.2
048400     MOVE SPACE TO CORRECT-X.                                     SQ2124.2
048500     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         SQ2124.2
048600     MOVE     SPACE TO RE-MARK.                                   SQ2124.2
048700 HEAD-ROUTINE.                                                    SQ2124.2
048800     MOVE CCVS-H-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ2124.2
048900     MOVE CCVS-H-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.   SQ2124.2
049000     MOVE CCVS-H-3 TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.   SQ2124.2
049100 COLUMN-NAMES-ROUTINE.                                            SQ2124.2
049200     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ2124.2
049300     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ2124.2
049400     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ2124.2
049500 END-ROUTINE.                                                     SQ2124.2
049600     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.SQ2124.2
049700 END-RTN-EXIT.                                                    SQ2124.2
049800     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ2124.2
049900 END-ROUTINE-1.                                                   SQ2124.2
050000      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      SQ2124.2
050100      ERROR-HOLD. ADD DELETE-CNT TO ERROR-HOLD.                   SQ2124.2
050200      ADD PASS-COUNTER TO ERROR-HOLD.                             SQ2124.2
050300*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   SQ2124.2
050400      MOVE PASS-COUNTER TO CCVS-E-4-1.                            SQ2124.2
050500      MOVE ERROR-HOLD TO CCVS-E-4-2.                              SQ2124.2
050600      MOVE CCVS-E-4 TO CCVS-E-2-2.                                SQ2124.2
050700      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           SQ2124.2
050800  END-ROUTINE-12.                                                 SQ2124.2
050900      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        SQ2124.2
051000     IF       ERROR-COUNTER IS EQUAL TO ZERO                      SQ2124.2
051100         MOVE "NO " TO ERROR-TOTAL                                SQ2124.2
051200         ELSE                                                     SQ2124.2
051300         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       SQ2124.2
051400     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           SQ2124.2
051500     PERFORM WRITE-LINE.                                          SQ2124.2
051600 END-ROUTINE-13.                                                  SQ2124.2
051700     IF DELETE-CNT IS EQUAL TO ZERO                               SQ2124.2
051800         MOVE "NO " TO ERROR-TOTAL  ELSE                          SQ2124.2
051900         MOVE DELETE-CNT TO ERROR-TOTAL.                          SQ2124.2
052000     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   SQ2124.2
052100     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ2124.2
052200      IF   INSPECT-COUNTER EQUAL TO ZERO                          SQ2124.2
052300          MOVE "NO " TO ERROR-TOTAL                               SQ2124.2
052400      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   SQ2124.2
052500      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            SQ2124.2
052600      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          SQ2124.2
052700     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ2124.2
052800 WRITE-LINE.                                                      SQ2124.2
052900     ADD 1 TO RECORD-COUNT.                                       SQ2124.2
053000*    IF RECORD-COUNT GREATER 50                                   SQ2124.2
053100*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          SQ2124.2
053200*        MOVE SPACE TO DUMMY-RECORD                               SQ2124.2
053300*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  SQ2124.2
053400*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             SQ2124.2
053500*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     SQ2124.2
053600*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          SQ2124.2
053700*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          SQ2124.2
053800*        MOVE ZERO TO RECORD-COUNT.                               SQ2124.2
053900     PERFORM WRT-LN.                                              SQ2124.2
054000 WRT-LN.                                                          SQ2124.2
054100     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               SQ2124.2
054200     MOVE SPACE TO DUMMY-RECORD.                                  SQ2124.2
054300 BLANK-LINE-PRINT.                                                SQ2124.2
054400     PERFORM WRT-LN.                                              SQ2124.2
054500 FAIL-ROUTINE.                                                    SQ2124.2
054600     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.   SQ2124.2
054700     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.    SQ2124.2
054800     MOVE "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.    SQ2124.2
054900     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     SQ2124.2
055000     GO TO FAIL-ROUTINE-EX.                                       SQ2124.2
055100 FAIL-ROUTINE-WRITE.                                              SQ2124.2
055200     MOVE TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE           SQ2124.2
055300     MOVE TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES.   SQ2124.2
055400 FAIL-ROUTINE-EX. EXIT.                                           SQ2124.2
055500 BAIL-OUT.                                                        SQ2124.2
055600     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.       SQ2124.2
055700     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.               SQ2124.2
055800 BAIL-OUT-WRITE.                                                  SQ2124.2
055900     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  SQ2124.2
056000     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     SQ2124.2
056100 BAIL-OUT-EX. EXIT.                                               SQ2124.2
056200 CCVS1-EXIT.                                                      SQ2124.2
056300     EXIT.                                                        SQ2124.2
056400 SECT-SQ212A-0002 SECTION.                                        SQ2124.2
056500 WRITE-INIT-GF-01.                                                SQ2124.2
056600     MOVE ZERO TO COUNT-OF-RECS.                                  SQ2124.2
056700******************************************************************SQ2124.2
056800*                                                                *SQ2124.2
056900*    ATTEMPT IS MADE TO WRITE 3 SHORTER RECORDS.                 *SQ2124.2
057000*                                                                *SQ2124.2
057100******************************************************************SQ2124.2
057200     MOVE "3 SHORTER RECORDS" TO RE-MARK.                         SQ2124.2
057300     MOVE 14   TO RECORD-LENGTH.                                  SQ2124.2
057400     MOVE 1 TO SWITCH-WRITE-REWRITE.                              SQ2124.2
057500     OPEN OUTPUT SQ-VS7.                                          SQ2124.2
057600     MOVE "WRITE SHORTER RECORDS" TO FEATURE.                     SQ2124.2
057700     PERFORM WRITE-RECORDS-1   3  TIMES.                          SQ2124.2
057800     MOVE 0 TO COUNT-OF-RECS.                                     SQ2124.2
057900 WRITE-TEST-GF-01.                                                SQ2124.2
058000     PERFORM WRITE-RECORDS-1 1030 TIMES.                          SQ2124.2
058100     PERFORM WRITE-RECORDS-2 1001 TIMES.                          SQ2124.2
058200 WRITE-WRITE-GF-01.                                               SQ2124.2
058300     MOVE "CREATE FILE SQ-VS7" TO FEATURE.                        SQ2124.2
058400     MOVE "WRITE-TEST-GF-01" TO PAR-NAME.                         SQ2124.2
058500     MOVE "FILE CREATED, RECS =" TO COMPUTED-A.                   SQ2124.2
058600     MOVE COUNT-OF-RECS TO CORRECT-18V0.                          SQ2124.2
058700     MOVE "FILE CONTAINS 18 THRU 2048 CHAR RECS" TO RE-MARK.      SQ2124.2
058800     PERFORM PRINT-DETAIL.                                        SQ2124.2
058900*        A SEQUENTIAL MASS STORAGE FILE CONTAINING 2031           SQ2124.2
059000*    RECORDS HAS BEEN CREATED.  THE FILE CONTAINS RECORDS         SQ2124.2
059100*    OF 18  THROUGH 2048 CHARACTERS BEGINNING WITH THE 18 CHAR RECSQ2124.2
059200*    AND ENDING WITH THE 2048 CHAR REC.                           SQ2124.2
059300*                                                                 SQ2124.2
059400******************************************************************SQ2124.2
059500*                                                                *SQ2124.2
059600*   RECORD NO.  2031 TO 2080  SHOULD NOT BE WRITTEN              *SQ2124.2
059700*                                                                *SQ2124.2
059800******************************************************************SQ2124.2
059900 TEST-STATUS-44.                                                  SQ2124.2
060000     MOVE "9 LONGER RECORDS" TO RE-MARK.                          SQ2124.2
060100     MOVE "WRITE LONGER RECORDS" TO FEATURE.                      SQ2124.2
060200     PERFORM WRITE-RECORDS-1  9   TIMES.                          SQ2124.2
060300 WRITE-CLOSE-GF-01.                                               SQ2124.2
060400     CLOSE SQ-VS7.                                                SQ2124.2
060500     GO TO READ-INIT-F1-01.                                       SQ2124.2
060600 WRITE-RECORDS-1.                                                 SQ2124.2
060700******************************************************************SQ2124.2
060800*   MOVE  ...  TO  OUTPUT-RECORD          1030 RECORDS           *SQ2124.2
060900*   WRITE   OUTPUT-RECORD.                                       *SQ2124.2
061000******************************************************************SQ2124.2
061100     ADD 1 TO COUNT-OF-RECS.                                      SQ2124.2
061200     ADD 1 TO RECORD-LENGTH.                                      SQ2124.2
061300     MOVE COUNT-OF-RECS TO RECORD-NUMBER.                         SQ2124.2
061400     MOVE VAR-RECORD-18-2048 TO SQ-VS7R1-FIRST.                   SQ2124.2
061500     MOVE SPACE TO SQ-VS7-STATUS.                                 SQ2124.2
061600     WRITE SQ-VSR7R1-M-G-2048.                                    SQ2124.2
061700 WRITE-RECORDS-2.                                                 SQ2124.2
061800******************************************************************SQ2124.2
061900*WRITE ...  FROM  ....  .                 1000 RECORDS           *SQ2124.2
062000******************************************************************SQ2124.2
062100     ADD 1 TO COUNT-OF-RECS.                                      SQ2124.2
062200     ADD 1 TO RECORD-LENGTH.                                      SQ2124.2
062300     MOVE COUNT-OF-RECS TO RECORD-NUMBER.                         SQ2124.2
062400     WRITE SQ-VSR7R1-M-G-2048 FROM VAR-RECORD-18-2048.            SQ2124.2
062500 READ-INIT-F1-01.                                                 SQ2124.2
062600     MOVE 17   TO RECORD-LENGTH.                                  SQ2124.2
062700     MOVE ZERO TO COUNT-OF-RECS.                                  SQ2124.2
062800     MOVE ZERO TO EOF-FLAG.                                       SQ2124.2
062900     MOVE ZERO TO RECORDS-IN-ERROR.                               SQ2124.2
063000     MOVE ZERO TO ERROR-FLAG.                                     SQ2124.2
063100     OPEN INPUT SQ-VS7.                                           SQ2124.2
063200 READ-TEST-F1-01.                                                 SQ2124.2
063300     PERFORM READ-REC-1 THRU READ-REC-1-EXIT 1030 TIMES.          SQ2124.2
063400     IF EOF-FLAG EQUAL TO 1                                       SQ2124.2
063500         MOVE "EOF ON FIRST READ" TO RE-MARK                      SQ2124.2
063600         GO TO READ-EOF-F1-03.                                    SQ2124.2
063700     IF ERROR-FLAG EQUAL TO 1                                     SQ2124.2
063800         GO TO READ-FAIL-F1-01.                                   SQ2124.2
063900 READ-PASS-F1-01.                                                 SQ2124.2
064000     PERFORM PASS.                                                SQ2124.2
064100     GO TO READ-WRITE-F1-01.                                      SQ2124.2
064200 READ-FAIL-F1-01.                                                 SQ2124.2
064300     MOVE "VII-30 FORMAT 2" TO RE-MARK.                           SQ2124.2
064400     PERFORM FAIL.                                                SQ2124.2
064500 READ-WRITE-F1-01.                                                SQ2124.2
064600     MOVE "READ 1030 RECORDS" TO FEATURE.                         SQ2124.2
064700     MOVE "READ-TEST-F1-01" TO PAR-NAME.                          SQ2124.2
064800     PERFORM PRINT-DETAIL.                                        SQ2124.2
064900     GO TO READ-INIT-F1-02.                                       SQ2124.2
065000 READ-REC-1.                                                      SQ2124.2
065100******************************************************************SQ2124.2
065200*   READ      <FILE>     AT END ...                              *SQ2124.2
065300******************************************************************SQ2124.2
065400     IF EOF-FLAG EQUAL TO 1                                       SQ2124.2
065500         GO TO READ-REC-1-EXIT.                                   SQ2124.2
065600     READ SQ-VS7 AT END                                           SQ2124.2
065700         MOVE 1 TO EOF-FLAG                                       SQ2124.2
065800         GO TO READ-REC-1-EXIT.                                   SQ2124.2
065900     ADD 1 TO COUNT-OF-RECS.                                      SQ2124.2
066000     MOVE SQ-VS7R1-FIRST TO VAR-RECORD-18-2048.                   SQ2124.2
066100     ADD 17 TO COUNT-OF-RECS.                                     SQ2124.2
066200     IF  RECORD-LENGTH     NOT EQUAL TO COUNT-OF-RECS             SQ2124.2
066300         GO TO READ-REC-1-ERROR.                                  SQ2124.2
066400     SUBTRACT 17 FROM COUNT-OF-RECS.                              SQ2124.2
066500     GO TO READ-REC-1-EXIT.                                       SQ2124.2
066600 READ-REC-1-ERROR.                                                SQ2124.2
066700     SUBTRACT 17 FROM COUNT-OF-RECS.                              SQ2124.2
066800     ADD 1 TO RECORDS-IN-ERROR.                                   SQ2124.2
066900     MOVE 1 TO ERROR-FLAG.                                        SQ2124.2
067000 READ-REC-1-EXIT.                                                 SQ2124.2
067100     EXIT.                                                        SQ2124.2
067200 READ-REC-2.                                                      SQ2124.2
067300******************************************************************SQ2124.2
067400*   READ  <FILE> INTO ....  AT END                               *SQ2124.2
067500******************************************************************SQ2124.2
067600     READ SQ-VS7 INTO VAR-RECORD-18-2048 AT END                   SQ2124.2
067700         MOVE 1 TO EOF-FLAG                                       SQ2124.2
067800         GO TO READ-REC-2-EXIT.                                   SQ2124.2
067900     ADD 1 TO COUNT-OF-RECS.                                      SQ2124.2
068000     ADD 17 TO COUNT-OF-RECS.                                     SQ2124.2
068100     IF  RECORD-LENGTH     NOT EQUAL TO COUNT-OF-RECS             SQ2124.2
068200         GO TO READ-REC-2-ERROR.                                  SQ2124.2
068300     SUBTRACT 17 FROM COUNT-OF-RECS.                              SQ2124.2
068400     GO TO READ-REC-2-EXIT.                                       SQ2124.2
068500 READ-REC-2-ERROR.                                                SQ2124.2
068600     SUBTRACT 17 FROM COUNT-OF-RECS.                              SQ2124.2
068700     MOVE 1 TO ERROR-FLAG.                                        SQ2124.2
068800 READ-REC-2-EXIT.                                                 SQ2124.2
068900     EXIT.                                                        SQ2124.2
069000 READ-INIT-F1-02.                                                 SQ2124.2
069100     MOVE ZERO TO ERROR-FLAG.                                     SQ2124.2
069200 READ-TEST-F1-02.                                                 SQ2124.2
069300     PERFORM READ-REC-2 THRU READ-REC-2-EXIT 1001 TIMES.          SQ2124.2
069400     IF EOF-FLAG EQUAL TO 1                                       SQ2124.2
069500         MOVE "EOF ON SECOND READ" TO RE-MARK                     SQ2124.2
069600         GO TO READ-EOF-F1-03.                                    SQ2124.2
069700     IF ERROR-FLAG EQUAL TO 1                                     SQ2124.2
069800         GO TO READ-FAIL-F1-02.                                   SQ2124.2
069900 READ-PASS-F1-02.                                                 SQ2124.2
070000     PERFORM PASS.                                                SQ2124.2
070100     GO TO READ-WRITE-F1-02.                                      SQ2124.2
070200 READ-FAIL-F1-02.                                                 SQ2124.2
070300     MOVE "VII-30 FORMAT 2" TO RE-MARK.                           SQ2124.2
070400     MOVE RECORD-LENGTH TO COMPUTED-N.                            SQ2124.2
070500     ADD 17 TO COUNT-OF-RECS.                                     SQ2124.2
070600     MOVE COUNT-OF-RECS TO CORRECT-N.                             SQ2124.2
070700     SUBTRACT 17 FROM COUNT-OF-RECS.                              SQ2124.2
070800     PERFORM FAIL.                                                SQ2124.2
070900 READ-WRITE-F1-02.                                                SQ2124.2
071000     MOVE "READ 1001 RECORDS" TO FEATURE.                         SQ2124.2
071100     MOVE "READ-TEST-F1-02" TO PAR-NAME.                          SQ2124.2
071200     PERFORM PRINT-DETAIL.                                        SQ2124.2
071300 READ-INIT-F1-03.                                                 SQ2124.2
071400     READ SQ-VS7 RECORD END                                       SQ2124.2
071500         GO TO READ-TEST-F1-03.                                   SQ2124.2
071600     MOVE "MORE THAN 2031 RECORDS" TO RE-MARK.                    SQ2124.2
071700 READ-EOF-F1-03.                                                  SQ2124.2
071800     MOVE "RECORDS READ =" TO COMPUTED-A.                         SQ2124.2
071900     MOVE COUNT-OF-RECS TO CORRECT-18V0.                          SQ2124.2
072000     GO TO READ-FAIL-F1-03.                                       SQ2124.2
072100 READ-TEST-F1-03.                                                 SQ2124.2
072200     IF RECORDS-IN-ERROR NOT EQUAL TO ZERO                        SQ2124.2
072300         MOVE "RECORDS IN ERROR =" TO COMPUTED-A                  SQ2124.2
072400     MOVE RECORDS-IN-ERROR TO CORRECT-18V0                        SQ2124.2
072500         GO TO READ-FAIL-F1-03.                                   SQ2124.2
072600 READ-PASS-F1-03.                                                 SQ2124.2
072700     PERFORM PASS.                                                SQ2124.2
072800     GO TO READ-WRITE-F1-03.                                      SQ2124.2
072900 READ-FAIL-F1-03.                                                 SQ2124.2
073000     MOVE "VII-30 FORMAT 2; TOO MUCH RECORDS" TO RE-MARK.         SQ2124.2
073100     PERFORM FAIL.                                                SQ2124.2
073200 READ-WRITE-F1-03.                                                SQ2124.2
073300     MOVE "READ-TEST-F1-03" TO PAR-NAME.                          SQ2124.2
073400     MOVE "AT END            " TO FEATURE.                        SQ2124.2
073500     PERFORM PRINT-DETAIL.                                        SQ2124.2
073600 READ-CLOSE-F1-03.                                                SQ2124.2
073700     CLOSE SQ-VS7.                                                SQ2124.2
073800 REWRITE-44-1.                                                    SQ2124.2
073900     OPEN I-O SQ-VS7.                                             SQ2124.2
074000******************************************************************SQ2124.2
074100*                                                                *SQ2124.2
074200*         READ   1ST  RECORD; REWRITE SMALLER RECORD.            *SQ2124.2
074300*                                                                *SQ2124.2
074400******************************************************************SQ2124.2
074500     READ SQ-VS7                                                  SQ2124.2
074600     MOVE 15 TO RECORD-LENGTH.                                    SQ2124.2
074700     MOVE 2 TO SWITCH-WRITE-REWRITE.                              SQ2124.2
074800     MOVE "REWRITE-44-1" TO PAR-NAME.                             SQ2124.2
074900     MOVE "RWRT SMALLER RECORD" TO FEATURE.                       SQ2124.2
075000     REWRITE SQ-VSR7R1-M-G-2048.                                  SQ2124.2
075100 REWRITE-44-2.                                                    SQ2124.2
075200******************************************************************SQ2124.2
075300*                                                                *SQ2124.2
075400*         READ   2ND  RECORD; REWRITE LARGER  RECORD.            *SQ2124.2
075500*                                                                *SQ2124.2
075600******************************************************************SQ2124.2
075700     READ SQ-VS7.                                                 SQ2124.2
075800     MOVE 2500 TO RECORD-LENGTH.                                  SQ2124.2
075900     MOVE "REWRITE-44-2" TO PAR-NAME.                             SQ2124.2
076000     MOVE "RWRT LARGER RECORD" TO FEATURE.                        SQ2124.2
076100     REWRITE SQ-VSR7R1-M-G-2048.                                  SQ2124.2
076200     CLOSE SQ-VS7.                                                SQ2124.2
076300 TERMINATE-ROUTINE.                                               SQ2124.2
076400     EXIT.                                                        SQ2124.2
076500 CCVS-EXIT SECTION.                                               SQ2124.2
076600 CCVS-999999.                                                     SQ2124.2
076700     GO TO CLOSE-FILES.                                           SQ2124.2
      *END-OF,SQ212A                                                                  
