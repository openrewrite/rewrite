      *HEADER,COBOL,NC126A                                                            
000100 IDENTIFICATION DIVISION.                                         NC1264.2
000200 PROGRAM-ID.                                                      NC1264.2
000300     NC126A.                                                      NC1264.2
000400****************************************************************  NC1264.2
000500*                                                              *  NC1264.2
000600*    VALIDATION FOR:-                                          *  NC1264.2
000700*                                                              *  NC1264.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1264.2
000900*                                                              *  NC1264.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC1264.2
001100*                                                              *  NC1264.2
001200****************************************************************  NC1264.2
001300*                                                              *  NC1264.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  NC1264.2
001500*                                                              *  NC1264.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  NC1264.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  NC1264.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  NC1264.2
001900*                                                              *  NC1264.2
002000****************************************************************  NC1264.2
002100*                                                                 NC1264.2
002200*    PROGRAM NC126A TESTS THE USE OF LEVEL NUMBERS 01 THROUGH 49  NC1264.2
002300*    INCLUSIVE WITH A VARIETY OF PICTURE CLAUSES AND GROUP AND    NC1264.2
002400*    ELEMENTARY COMPARISONS.                                      NC1264.2
002500*                                                                 NC1264.2
002600 ENVIRONMENT DIVISION.                                            NC1264.2
002700 CONFIGURATION SECTION.                                           NC1264.2
002800 SOURCE-COMPUTER.                                                 NC1264.2
002900     XXXXX082.                                                    NC1264.2
003000 OBJECT-COMPUTER.                                                 NC1264.2
003100     XXXXX083.                                                    NC1264.2
003200 INPUT-OUTPUT SECTION.                                            NC1264.2
003300 FILE-CONTROL.                                                    NC1264.2
003400     SELECT PRINT-FILE ASSIGN TO                                  NC1264.2
003500     XXXXX055.                                                    NC1264.2
003600 DATA DIVISION.                                                   NC1264.2
003700 FILE SECTION.                                                    NC1264.2
003800 FD  PRINT-FILE.                                                  NC1264.2
003900 01  PRINT-REC PICTURE X(120).                                    NC1264.2
004000 01  DUMMY-RECORD PICTURE X(120).                                 NC1264.2
004100 WORKING-STORAGE SECTION.                                         NC1264.2
004200 01  TEST-GROUP-1.                                                NC1264.2
004300     02 GP-1  PICTURE ZZZ999.                                     NC1264.2
004400     02 GPLEVEL-1.                                                NC1264.2
004500      03 GP-2  PICTURE 9(9).99.                                   NC1264.2
004600      03 GPLEVEL-2.                                               NC1264.2
004700       04 GP-3 PICTURE  0009(9).                                  NC1264.2
004800       04 GPLEVEL-3.                                              NC1264.2
004900        05 GP-4 PICTURE  ZBZBZ9.                                  NC1264.2
005000        05 GPLEVEL-4.                                             NC1264.2
005100           06  GP-5  PICTURE  $$$,$99.99.                         NC1264.2
005200         06 GPLEVEL-5.                                            NC1264.2
005300          07 GP-6 PICTURE  ******99.                              NC1264.2
005400          07 GPLEVEL-6.                                           NC1264.2
005500           08 GP-7 PICTURE +999,999.                              NC1264.2
005600           08 GPLEVEL-7.                                          NC1264.2
005700            09 GP-8 PICTURE X(14).                                NC1264.2
005800            09 GPLEVEL-8.                                         NC1264.2
005900             10 GP-9 PICTURE XBXBXBX.                             NC1264.2
006000             10 GPLEVEL-9.                                        NC1264.2
006100              11 GP-10 PICTURE 9090900.                           NC1264.2
006200              11 GPLEVEL-10.                                      NC1264.2
006300               12 GP-11  PICTURE $999,999.00.                     NC1264.2
006400               12 GPLEVEL-11.                                     NC1264.2
006500                13 GP-12 PICTURE ZZZ.9.                           NC1264.2
006600                13 GPLEVEL-12.                                    NC1264.2
006700                 14 GP-13 PICTURE  ZZ9B900.                       NC1264.2
006800                 14 GPLEVEL-13.                                   NC1264.2
006900                  15 GP-14  PICTURE  XXXX.                        NC1264.2
007000                  15 GPLEVEL-14.                                  NC1264.2
007100     16 GP-15 PICTURE  9(10).                                     NC1264.2
007200     16 GPLEVEL-15.                                               NC1264.2
007300      17 GP-16 PICTURE  Z(11).                                    NC1264.2
007400      17 GPLEVEL-16.                                              NC1264.2
007500       18 GP-17  PICTURE  99BB909.                                NC1264.2
007600       18 GPLEVEL-17.                                             NC1264.2
007700        19 GP-18  PICTURE -*B*99.                                 NC1264.2
007800        19 GPLEVEL-18.                                            NC1264.2
007900         20 GP-19 PICTURE  0009999.                               NC1264.2
008000         20 GPLEVEL-19.                                           NC1264.2
008100          21 GP-20 PICTURE  999DB.                                NC1264.2
008200          21 GPLEVEL-20.                                          NC1264.2
008300           22 GP-21 PICTURE  ABABABA.                             NC1264.2
008400           22 GPLEVEL-21.                                         NC1264.2
008500            23 GP-22 PICTURE *999999.                             NC1264.2
008600            23 GPLEVEL-22.                                        NC1264.2
008700             24 GP-23 PICTURE XXXXXA.                             NC1264.2
008800             24 GPLEVEL-23.                                       NC1264.2
008900              25 GP-24  PICTURE  $$$,$$$.99.                      NC1264.2
009000              25 GPLEVEL-24.                                      NC1264.2
009100               26 GP-25  PICTURE  9BB9BBB9BBB.                    NC1264.2
009200               26 GPLEVEL-25.                                     NC1264.2
009300                27 GP-26 PICTURE  9990009.                        NC1264.2
009400                27 GPLEVEL-26.                                    NC1264.2
009500                 28 GP-27 PICTURE  9,999,999.                     NC1264.2
009600                 28 GPLEVEL-27.                                   NC1264.2
009700                  29 GP-28 PICTURE   9(7),9.                      NC1264.2
009800                  29 GPLEVEL-28.                                  NC1264.2
009900                   30 GP-29  PICTURE  $***.99.                    NC1264.2
010000                   30 GPLEVEL-29.                                 NC1264.2
010100     31 GP-30  PICTURE  X(15).                                    NC1264.2
010200     31 GPLEVEL-30.                                               NC1264.2
010300      32 GP-31  PICTURE  9(10).                                   NC1264.2
010400      32 GPLEVEL-31.                                              NC1264.2
010500       33 GP-32 PICTURE  *99.                                     NC1264.2
010600       33 GPLEVEL-32.                                             NC1264.2
010700        34 GP-33 PICTURE  ZZZ9.                                   NC1264.2
010800        34 GPLEVEL-33.                                            NC1264.2
010900         35 GP-34  PICTURE BB9BB9.                                NC1264.2
011000         35 GPLEVEL-34.                                           NC1264.2
011100          36 GP-35 PICTURE  $99,999.99.                           NC1264.2
011200          36 GPLEVEL-35.                                          NC1264.2
011300           37 GP-36  PICTURE  090909.                             NC1264.2
011400           37 GPLEVEL-36.                                         NC1264.2
011500            38 GP-37  PICTURE  ZZZZ.                              NC1264.2
011600            38 GPLEVEL-37.                                        NC1264.2
011700             39 GP-38  PICTURE  +99.                              NC1264.2
011800             39 GPLEVEL-38.                                       NC1264.2
011900              40 GP-39  PICTURE -99.                              NC1264.2
012000              40 GPLEVEL-39.                                      NC1264.2
012100               41 GP-40  PICTURE  99CR.                           NC1264.2
012200               41 GPLEVEL-40.                                     NC1264.2
012300                42 GP-41  PICTURE  99DB.                          NC1264.2
012400                42 GPLEVEL-41.                                    NC1264.2
012500                 43 GP-42  PICTURE  ****.                         NC1264.2
012600                 43 GPLEVEL-42.                                   NC1264.2
012700                  44 GP-43  PICTURE  AAA.                         NC1264.2
012800                  44 GPLEVEL-43.                                  NC1264.2
012900                   45 GP-44  PICTURE XXX.                         NC1264.2
013000                   45 GPLEVEL-44.                                 NC1264.2
013100     46 GP-45  PICTURE  *9999.                                    NC1264.2
013200     46 GPLEVEL-45.                                               NC1264.2
013300      47 GP-46  PICTURE  9(10).99.                                NC1264.2
013400      47 GPLEVEL-46.                                              NC1264.2
013500       48 GP-47           OCCURS 2 TIMES  PICTURE 9.              NC1264.2
013600       48 GPLEVEL-47.                                             NC1264.2
013700        49 GP-48          OCCURS 2 TIMES  PICTURE X.              NC1264.2
013800 01  TABLE-GROUP.                                                 NC1264.2
013900     02  TB-1  PICTURE XX.                                        NC1264.2
014000     02  TBGRP-1.                                                 NC1264.2
014100      03 TB-2  PICTURE XX.                                        NC1264.2
014200      03 TBGRP-2.                                                 NC1264.2
014300       04 TB-3 PICTURE XX.                                        NC1264.2
014400       04 TBGRP-3.                                                NC1264.2
014500        05 TB-4  PICTURE XX.                                      NC1264.2
014600        05 TBGRP-4.                                               NC1264.2
014700         06 TB-5  PICTURE XX.                                     NC1264.2
014800         06 TBGRP-5.                                              NC1264.2
014900          07 TB-6  PICTURE XX.                                    NC1264.2
015000          07 TBGRP-6.                                             NC1264.2
015100           08 TB-7  PICTURE XX.                                   NC1264.2
015200           08 TBGRP-7.                                            NC1264.2
015300            09 TB-8  PICTURE XX.                                  NC1264.2
015400            09 TBGRP-8.                                           NC1264.2
015500             10 TB-9 PICTURE XX.                                  NC1264.2
015600             10 TBGRP-9.                                          NC1264.2
015700              11 TB-10 PICTURE XX.                                NC1264.2
015800              11 TBGRP-10.                                        NC1264.2
015900               12 TB-11 PICTURE XX.                               NC1264.2
016000               12 TBGRP-11.                                       NC1264.2
016100                13 TB-12  PICTURE XX.                             NC1264.2
016200                13 TBGRP-12.                                      NC1264.2
016300                14 TB-13  PICTURE XX.                             NC1264.2
016400                14 TBGRP-13.                                      NC1264.2
016500                 15 TB-14  PICTURE XX.                            NC1264.2
016600                 15 TBGRP-14.                                     NC1264.2
016700     16 TB-15 PICTURE XX.                                         NC1264.2
016800     16 TBGRP-15.                                                 NC1264.2
016900      17 TB-16 PICTURE XX.                                        NC1264.2
017000      17 TBGRP-16.                                                NC1264.2
017100       18 TB-17 PICTURE XX.                                       NC1264.2
017200       18 TBGRP-17.                                               NC1264.2
017300        19 TB-18 PICTURE XX.                                      NC1264.2
017400        19 TBGRP-18.                                              NC1264.2
017500         20 TB-19 PICTURE XX.                                     NC1264.2
017600         20 TBGRP-19.                                             NC1264.2
017700          21 TB-20 PICTURE XX.                                    NC1264.2
017800          21 TBGRP-20.                                            NC1264.2
017900           22 TB-21 PICTURE XX.                                   NC1264.2
018000           22 TBGRP-21.                                           NC1264.2
018100            23 TB-22 PICTURE XX.                                  NC1264.2
018200            23 TBGRP-22.                                          NC1264.2
018300             24 TB-23  PICTURE XX.                                NC1264.2
018400             24 TBGRP-23.                                         NC1264.2
018500              25 TB-24 PICTURE XX.                                NC1264.2
018600              25 TBGRP-24.                                        NC1264.2
018700               26 TB-25 PICTURE XX.                               NC1264.2
018800               26 TBGRP-25.                                       NC1264.2
018900                27 TB-26 PICTURE XX.                              NC1264.2
019000                27 TBGRP-26.                                      NC1264.2
019100                 28 TB-27 PICTURE XX.                             NC1264.2
019200                 28 TBGRP-27.                                     NC1264.2
019300                  29 TB-28 PICTURE XX.                            NC1264.2
019400                  29 TBGRP-28.                                    NC1264.2
019500                   30 TB-29 PICTURE XX.                           NC1264.2
019600                   30 TBGRP-29.                                   NC1264.2
019700     31 TB-30  PICTURE XX.                                        NC1264.2
019800     31 TBGRP-30.                                                 NC1264.2
019900      32 TB-31  PICTURE XX.                                       NC1264.2
020000      32 TBGRP-31.                                                NC1264.2
020100       33 TB-32  PICTURE XX.                                      NC1264.2
020200       33 TBGRP-32.                                               NC1264.2
020300        34 TB-33  PICTURE XX.                                     NC1264.2
020400        34 TBGRP-33.                                              NC1264.2
020500         35 TB-34  PICTURE XX.                                    NC1264.2
020600         35 TBGRP-34.                                             NC1264.2
020700          36 TB-35 PICTURE XX.                                    NC1264.2
020800          36 TBGRP-35.                                            NC1264.2
020900           37 TB-36  PICTURE XX.                                  NC1264.2
021000           37 TBGRP-36.                                           NC1264.2
021100            38 TB-37  PICTURE XX.                                 NC1264.2
021200            38 TBGRP-37.                                          NC1264.2
021300             39 TB-38  PICTURE XX.                                NC1264.2
021400             39 TBGRP-38.                                         NC1264.2
021500              40 TB-39  PICTURE XX.                               NC1264.2
021600              40 TBGRP-39.                                        NC1264.2
021700               41 TB-40  PICTURE XX.                              NC1264.2
021800               41 TBGRP-40.                                       NC1264.2
021900                42 TB-41  PICTURE XX.                             NC1264.2
022000                42 TBGRP-41.                                      NC1264.2
022100                 43 TB-42  PICTURE XX.                            NC1264.2
022200                 43 TBGRP-42.                                     NC1264.2
022300                  44 TB-43  PICTURE XX.                           NC1264.2
022400                  44 TBGRP-43.                                    NC1264.2
022500                   45 TB-44  PICTURE XX.                          NC1264.2
022600                   45 TBGRP-44.                                   NC1264.2
022700     46 TB-45  PICTURE XX.                                        NC1264.2
022800     46 TBGRP-45.                                                 NC1264.2
022900      47 TB-46  PICTURE XX.                                       NC1264.2
023000      47 TBGRP-46.                                                NC1264.2
023100       48 TB-47  PICTURE XX.                                      NC1264.2
023200       48 TBGRP-47.                                               NC1264.2
023300        49 TB-48  PICTURE XX.                                     NC1264.2
023400 01  LITERAL-98.                                                  NC1264.2
023500     02 A-PART-98  PICTURE X(20) VALUE "ABCDEFGHIJKLMNOPQRST".    NC1264.2
023600     02 B-PART-98  PICTURE X(20) VALUE "01234567899876543210".    NC1264.2
023700     02 C-PART-98  PICTURE X(20) VALUE "SUPERCALIFRAGILISTIC".    NC1264.2
023800     02 D-PART-98  PICTURE X(20) VALUE "THAT LITERAL WAS BAD".    NC1264.2
023900     02 E-PART-98  PICTURE X(16) VALUE "UP ON THE ROOFS".         NC1264.2
024000 01  BREAKDOWN-RECORD.                                            NC1264.2
024100     02 LENGTH-COUNTER  PICTURE 999 VALUE ZERO.                   NC1264.2
024200     02 COMPUTED-BREAKDOWN.                                       NC1264.2
024300         03 CM-20       PICTURE X(20).                            NC1264.2
024400         03 CM-40       PICTURE X(20).                            NC1264.2
024500         03 CM-60       PICTURE X(20).                            NC1264.2
024600         03 CM-80       PICTURE X(20).                            NC1264.2
024700         03 CM-100      PICTURE X(20).                            NC1264.2
024800         03 CM-120      PICTURE X(20).                            NC1264.2
024900         03 CM-140      PICTURE X(20).                            NC1264.2
025000         03 CM-160      PICTURE X(20).                            NC1264.2
025100         03 CM-180      PICTURE X(20).                            NC1264.2
025200         03 CM-200      PICTURE X(20).                            NC1264.2
025300     02 CORRECT-BREAKDOWN.                                        NC1264.2
025400         03 CR-20       PICTURE X(20).                            NC1264.2
025500         03 CR-40       PICTURE X(20).                            NC1264.2
025600         03 CR-60       PICTURE X(20).                            NC1264.2
025700         03 CR-80       PICTURE X(20).                            NC1264.2
025800         03 CR-100      PICTURE X(20).                            NC1264.2
025900         03 CR-120      PICTURE X(20).                            NC1264.2
026000         03 CR-140      PICTURE X(20).                            NC1264.2
026100         03 CR-160      PICTURE X(20).                            NC1264.2
026200         03 CR-180      PICTURE X(20).                            NC1264.2
026300         03 CR-200      PICTURE X(20).                            NC1264.2
026400 01  TEST-RESULTS.                                                NC1264.2
026500     02 FILLER                   PIC X      VALUE SPACE.          NC1264.2
026600     02 FEATURE                  PIC X(20)  VALUE SPACE.          NC1264.2
026700     02 FILLER                   PIC X      VALUE SPACE.          NC1264.2
026800     02 P-OR-F                   PIC X(5)   VALUE SPACE.          NC1264.2
026900     02 FILLER                   PIC X      VALUE SPACE.          NC1264.2
027000     02  PAR-NAME.                                                NC1264.2
027100       03 FILLER                 PIC X(19)  VALUE SPACE.          NC1264.2
027200       03  PARDOT-X              PIC X      VALUE SPACE.          NC1264.2
027300       03 DOTVALUE               PIC 99     VALUE ZERO.           NC1264.2
027400     02 FILLER                   PIC X(8)   VALUE SPACE.          NC1264.2
027500     02 RE-MARK                  PIC X(61).                       NC1264.2
027600 01  TEST-COMPUTED.                                               NC1264.2
027700     02 FILLER                   PIC X(30)  VALUE SPACE.          NC1264.2
027800     02 FILLER                   PIC X(17)  VALUE                 NC1264.2
027900            "       COMPUTED=".                                   NC1264.2
028000     02 COMPUTED-X.                                               NC1264.2
028100     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          NC1264.2
028200     03 COMPUTED-N               REDEFINES COMPUTED-A             NC1264.2
028300                                 PIC -9(9).9(9).                  NC1264.2
028400     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         NC1264.2
028500     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     NC1264.2
028600     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     NC1264.2
028700     03       CM-18V0 REDEFINES COMPUTED-A.                       NC1264.2
028800         04 COMPUTED-18V0                    PIC -9(18).          NC1264.2
028900         04 FILLER                           PIC X.               NC1264.2
029000     03 FILLER PIC X(50) VALUE SPACE.                             NC1264.2
029100 01  TEST-CORRECT.                                                NC1264.2
029200     02 FILLER PIC X(30) VALUE SPACE.                             NC1264.2
029300     02 FILLER PIC X(17) VALUE "       CORRECT =".                NC1264.2
029400     02 CORRECT-X.                                                NC1264.2
029500     03 CORRECT-A                  PIC X(20) VALUE SPACE.         NC1264.2
029600     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      NC1264.2
029700     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         NC1264.2
029800     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     NC1264.2
029900     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     NC1264.2
030000     03      CR-18V0 REDEFINES CORRECT-A.                         NC1264.2
030100         04 CORRECT-18V0                     PIC -9(18).          NC1264.2
030200         04 FILLER                           PIC X.               NC1264.2
030300     03 FILLER PIC X(2) VALUE SPACE.                              NC1264.2
030400     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     NC1264.2
030500 01  CCVS-C-1.                                                    NC1264.2
030600     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PANC1264.2
030700-    "SS  PARAGRAPH-NAME                                          NC1264.2
030800-    "       REMARKS".                                            NC1264.2
030900     02 FILLER                     PIC X(20)    VALUE SPACE.      NC1264.2
031000 01  CCVS-C-2.                                                    NC1264.2
031100     02 FILLER                     PIC X        VALUE SPACE.      NC1264.2
031200     02 FILLER                     PIC X(6)     VALUE "TESTED".   NC1264.2
031300     02 FILLER                     PIC X(15)    VALUE SPACE.      NC1264.2
031400     02 FILLER                     PIC X(4)     VALUE "FAIL".     NC1264.2
031500     02 FILLER                     PIC X(94)    VALUE SPACE.      NC1264.2
031600 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       NC1264.2
031700 01  REC-CT                        PIC 99       VALUE ZERO.       NC1264.2
031800 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       NC1264.2
031900 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       NC1264.2
032000 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       NC1264.2
032100 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       NC1264.2
032200 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       NC1264.2
032300 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       NC1264.2
032400 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      NC1264.2
032500 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       NC1264.2
032600 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     NC1264.2
032700 01  CCVS-H-1.                                                    NC1264.2
032800     02  FILLER                    PIC X(39)    VALUE SPACES.     NC1264.2
032900     02  FILLER                    PIC X(42)    VALUE             NC1264.2
033000     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 NC1264.2
033100     02  FILLER                    PIC X(39)    VALUE SPACES.     NC1264.2
033200 01  CCVS-H-2A.                                                   NC1264.2
033300   02  FILLER                        PIC X(40)  VALUE SPACE.      NC1264.2
033400   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  NC1264.2
033500   02  FILLER                        PIC XXXX   VALUE             NC1264.2
033600     "4.2 ".                                                      NC1264.2
033700   02  FILLER                        PIC X(28)  VALUE             NC1264.2
033800            " COPY - NOT FOR DISTRIBUTION".                       NC1264.2
033900   02  FILLER                        PIC X(41)  VALUE SPACE.      NC1264.2
034000                                                                  NC1264.2
034100 01  CCVS-H-2B.                                                   NC1264.2
034200   02  FILLER                        PIC X(15)  VALUE             NC1264.2
034300            "TEST RESULT OF ".                                    NC1264.2
034400   02  TEST-ID                       PIC X(9).                    NC1264.2
034500   02  FILLER                        PIC X(4)   VALUE             NC1264.2
034600            " IN ".                                               NC1264.2
034700   02  FILLER                        PIC X(12)  VALUE             NC1264.2
034800     " HIGH       ".                                              NC1264.2
034900   02  FILLER                        PIC X(22)  VALUE             NC1264.2
035000            " LEVEL VALIDATION FOR ".                             NC1264.2
035100   02  FILLER                        PIC X(58)  VALUE             NC1264.2
035200     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1264.2
035300 01  CCVS-H-3.                                                    NC1264.2
035400     02  FILLER                      PIC X(34)  VALUE             NC1264.2
035500            " FOR OFFICIAL USE ONLY    ".                         NC1264.2
035600     02  FILLER                      PIC X(58)  VALUE             NC1264.2
035700     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC1264.2
035800     02  FILLER                      PIC X(28)  VALUE             NC1264.2
035900            "  COPYRIGHT   1985 ".                                NC1264.2
036000 01  CCVS-E-1.                                                    NC1264.2
036100     02 FILLER                       PIC X(52)  VALUE SPACE.      NC1264.2
036200     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              NC1264.2
036300     02 ID-AGAIN                     PIC X(9).                    NC1264.2
036400     02 FILLER                       PIC X(45)  VALUE SPACES.     NC1264.2
036500 01  CCVS-E-2.                                                    NC1264.2
036600     02  FILLER                      PIC X(31)  VALUE SPACE.      NC1264.2
036700     02  FILLER                      PIC X(21)  VALUE SPACE.      NC1264.2
036800     02 CCVS-E-2-2.                                               NC1264.2
036900         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      NC1264.2
037000         03 FILLER                   PIC X      VALUE SPACE.      NC1264.2
037100         03 ENDER-DESC               PIC X(44)  VALUE             NC1264.2
037200            "ERRORS ENCOUNTERED".                                 NC1264.2
037300 01  CCVS-E-3.                                                    NC1264.2
037400     02  FILLER                      PIC X(22)  VALUE             NC1264.2
037500            " FOR OFFICIAL USE ONLY".                             NC1264.2
037600     02  FILLER                      PIC X(12)  VALUE SPACE.      NC1264.2
037700     02  FILLER                      PIC X(58)  VALUE             NC1264.2
037800     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1264.2
037900     02  FILLER                      PIC X(13)  VALUE SPACE.      NC1264.2
038000     02 FILLER                       PIC X(15)  VALUE             NC1264.2
038100             " COPYRIGHT 1985".                                   NC1264.2
038200 01  CCVS-E-4.                                                    NC1264.2
038300     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      NC1264.2
038400     02 FILLER                       PIC X(4)   VALUE " OF ".     NC1264.2
038500     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      NC1264.2
038600     02 FILLER                       PIC X(40)  VALUE             NC1264.2
038700      "  TESTS WERE EXECUTED SUCCESSFULLY".                       NC1264.2
038800 01  XXINFO.                                                      NC1264.2
038900     02 FILLER                       PIC X(19)  VALUE             NC1264.2
039000            "*** INFORMATION ***".                                NC1264.2
039100     02 INFO-TEXT.                                                NC1264.2
039200       04 FILLER                     PIC X(8)   VALUE SPACE.      NC1264.2
039300       04 XXCOMPUTED                 PIC X(20).                   NC1264.2
039400       04 FILLER                     PIC X(5)   VALUE SPACE.      NC1264.2
039500       04 XXCORRECT                  PIC X(20).                   NC1264.2
039600     02 INF-ANSI-REFERENCE           PIC X(48).                   NC1264.2
039700 01  HYPHEN-LINE.                                                 NC1264.2
039800     02 FILLER  PIC IS X VALUE IS SPACE.                          NC1264.2
039900     02 FILLER  PIC IS X(65)    VALUE IS "************************NC1264.2
040000-    "*****************************************".                 NC1264.2
040100     02 FILLER  PIC IS X(54)    VALUE IS "************************NC1264.2
040200-    "******************************".                            NC1264.2
040300 01  CCVS-PGM-ID                     PIC X(9)   VALUE             NC1264.2
040400     "NC126A".                                                    NC1264.2
040500 PROCEDURE DIVISION.                                              NC1264.2
040600 CCVS1 SECTION.                                                   NC1264.2
040700 OPEN-FILES.                                                      NC1264.2
040800     OPEN     OUTPUT PRINT-FILE.                                  NC1264.2
040900     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   NC1264.2
041000     MOVE    SPACE TO TEST-RESULTS.                               NC1264.2
041100     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             NC1264.2
041200     GO TO CCVS1-EXIT.                                            NC1264.2
041300 CLOSE-FILES.                                                     NC1264.2
041400     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   NC1264.2
041500 TERMINATE-CCVS.                                                  NC1264.2
041600*    EXIT PROGRAM.                                                NC1264.2
041700*TERMINATE-CALL.                                                  NC1264.2
041800     STOP     RUN.                                                NC1264.2
041900 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         NC1264.2
042000 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           NC1264.2
042100 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          NC1264.2
042200 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      NC1264.2
042300     MOVE "****TEST DELETED****" TO RE-MARK.                      NC1264.2
042400 PRINT-DETAIL.                                                    NC1264.2
042500     IF REC-CT NOT EQUAL TO ZERO                                  NC1264.2
042600             MOVE "." TO PARDOT-X                                 NC1264.2
042700             MOVE REC-CT TO DOTVALUE.                             NC1264.2
042800     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      NC1264.2
042900     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               NC1264.2
043000        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 NC1264.2
043100          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 NC1264.2
043200     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              NC1264.2
043300     MOVE SPACE TO CORRECT-X.                                     NC1264.2
043400     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         NC1264.2
043500     MOVE     SPACE TO RE-MARK.                                   NC1264.2
043600 HEAD-ROUTINE.                                                    NC1264.2
043700     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC1264.2
043800     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC1264.2
043900     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC1264.2
044000     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC1264.2
044100 COLUMN-NAMES-ROUTINE.                                            NC1264.2
044200     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1264.2
044300     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1264.2
044400     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        NC1264.2
044500 END-ROUTINE.                                                     NC1264.2
044600     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.NC1264.2
044700 END-RTN-EXIT.                                                    NC1264.2
044800     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1264.2
044900 END-ROUTINE-1.                                                   NC1264.2
045000      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      NC1264.2
045100      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               NC1264.2
045200      ADD PASS-COUNTER TO ERROR-HOLD.                             NC1264.2
045300*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   NC1264.2
045400      MOVE PASS-COUNTER TO CCVS-E-4-1.                            NC1264.2
045500      MOVE ERROR-HOLD TO CCVS-E-4-2.                              NC1264.2
045600      MOVE CCVS-E-4 TO CCVS-E-2-2.                                NC1264.2
045700      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           NC1264.2
045800  END-ROUTINE-12.                                                 NC1264.2
045900      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        NC1264.2
046000     IF       ERROR-COUNTER IS EQUAL TO ZERO                      NC1264.2
046100         MOVE "NO " TO ERROR-TOTAL                                NC1264.2
046200         ELSE                                                     NC1264.2
046300         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       NC1264.2
046400     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           NC1264.2
046500     PERFORM WRITE-LINE.                                          NC1264.2
046600 END-ROUTINE-13.                                                  NC1264.2
046700     IF DELETE-COUNTER IS EQUAL TO ZERO                           NC1264.2
046800         MOVE "NO " TO ERROR-TOTAL  ELSE                          NC1264.2
046900         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      NC1264.2
047000     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   NC1264.2
047100     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1264.2
047200      IF   INSPECT-COUNTER EQUAL TO ZERO                          NC1264.2
047300          MOVE "NO " TO ERROR-TOTAL                               NC1264.2
047400      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   NC1264.2
047500      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            NC1264.2
047600      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          NC1264.2
047700     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1264.2
047800 WRITE-LINE.                                                      NC1264.2
047900     ADD 1 TO RECORD-COUNT.                                       NC1264.2
048000*    IF RECORD-COUNT GREATER 42                                   NC1264.2
048100*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          NC1264.2
048200*        MOVE SPACE TO DUMMY-RECORD                               NC1264.2
048300*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  NC1264.2
048400*        MOVE CCVS-H-1  TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   NC1264.2
048500*        MOVE CCVS-H-2A TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   NC1264.2
048600*        MOVE CCVS-H-2B TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   NC1264.2
048700*        MOVE CCVS-H-3  TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   NC1264.2
048800*        MOVE CCVS-C-1  TO DUMMY-RECORD  PERFORM WRT-LN           NC1264.2
048900*        MOVE CCVS-C-2  TO DUMMY-RECORD  PERFORM WRT-LN           NC1264.2
049000*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          NC1264.2
049100*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          NC1264.2
049200*        MOVE ZERO TO RECORD-COUNT.                               NC1264.2
049300     PERFORM WRT-LN.                                              NC1264.2
049400 WRT-LN.                                                          NC1264.2
049500     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               NC1264.2
049600     MOVE SPACE TO DUMMY-RECORD.                                  NC1264.2
049700 BLANK-LINE-PRINT.                                                NC1264.2
049800     PERFORM WRT-LN.                                              NC1264.2
049900 FAIL-ROUTINE.                                                    NC1264.2
050000     IF     COMPUTED-X NOT EQUAL TO SPACE                         NC1264.2
050100            GO TO FAIL-ROUTINE-WRITE.                             NC1264.2
050200     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.NC1264.2
050300     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC1264.2
050400     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   NC1264.2
050500     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1264.2
050600     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC1264.2
050700     GO TO  FAIL-ROUTINE-EX.                                      NC1264.2
050800 FAIL-ROUTINE-WRITE.                                              NC1264.2
050900     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         NC1264.2
051000     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 NC1264.2
051100     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. NC1264.2
051200     MOVE   SPACES TO COR-ANSI-REFERENCE.                         NC1264.2
051300 FAIL-ROUTINE-EX. EXIT.                                           NC1264.2
051400 BAIL-OUT.                                                        NC1264.2
051500     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   NC1264.2
051600     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           NC1264.2
051700 BAIL-OUT-WRITE.                                                  NC1264.2
051800     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  NC1264.2
051900     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC1264.2
052000     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1264.2
052100     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC1264.2
052200 BAIL-OUT-EX. EXIT.                                               NC1264.2
052300 CCVS1-EXIT.                                                      NC1264.2
052400     EXIT.                                                        NC1264.2
052500 SECT-NC126A-001 SECTION.                                         NC1264.2
052600 LEV-INIT-GF-1-1.                                                 NC1264.2
052700     MOVE "VI-21 5.3.3 SR1" TO ANSI-REFERENCE.                    NC1264.2
052800     MOVE "SPACE MOVED TO GRP"  TO FEATURE.                       NC1264.2
052900     PERFORM PRINT-DETAIL.                                        NC1264.2
053000     MOVE "GROUP ITEM CHECK  " TO FEATURE.                        NC1264.2
053100 LEV-TEST-GF-1-0.                                                 NC1264.2
053200     MOVE SPACE  TO TABLE-GROUP.                                  NC1264.2
053300 LEV-TEST-GF-1-1.                                                 NC1264.2
053400     IF TBGRP-1 EQUAL TO  SPACE                                   NC1264.2
053500              PERFORM PASS                                        NC1264.2
053600              GO  TO   LEV-WRITE-GF-1-1.                          NC1264.2
053700     MOVE    TABLE-GROUP           TO COMPUTED-BREAKDOWN.         NC1264.2
053800     MOVE    SPACE                 TO CORRECT-BREAKDOWN.          NC1264.2
053900     MOVE  94     TO LENGTH-COUNTER.                              NC1264.2
054000     GO   TO    LEV-WRITE-GF-1-1.                                 NC1264.2
054100 LEV-DELETE-GF-1-1.                                               NC1264.2
054200     PERFORM DE-LETE.                                             NC1264.2
054300 LEV-WRITE-GF-1-1.                                                NC1264.2
054400     MOVE  "LEV-TEST-GF-1-1"  TO PAR-NAME.                        NC1264.2
054500     IF       LENGTH-COUNTER GREATER THAN ZERO                    NC1264.2
054600              PERFORM BREAKDOWN-PARA THRU BREAKDOWN-EXIT.         NC1264.2
054700     PERFORM PRINT-DETAIL.                                        NC1264.2
054800 LEV-TEST-GF-1-2.                                                 NC1264.2
054900     IF TBGRP-2 EQUAL TO  SPACE                                   NC1264.2
055000              PERFORM PASS                                        NC1264.2
055100              GO  TO   LEV-WRITE-GF-1-2.                          NC1264.2
055200     MOVE    TABLE-GROUP           TO COMPUTED-BREAKDOWN.         NC1264.2
055300     MOVE    SPACE                 TO CORRECT-BREAKDOWN.          NC1264.2
055400     MOVE  92     TO LENGTH-COUNTER.                              NC1264.2
055500     GO   TO    LEV-WRITE-GF-1-2.                                 NC1264.2
055600 LEV-DELETE-GF-1-2.                                               NC1264.2
055700     PERFORM DE-LETE.                                             NC1264.2
055800 LEV-WRITE-GF-1-2.                                                NC1264.2
055900     MOVE  "LEV-TEST-GF-1-2"  TO PAR-NAME.                        NC1264.2
056000     IF       LENGTH-COUNTER GREATER THAN ZERO                    NC1264.2
056100              PERFORM BREAKDOWN-PARA THRU BREAKDOWN-EXIT.         NC1264.2
056200     PERFORM PRINT-DETAIL.                                        NC1264.2
056300 LEV-TEST-GF-1-3.                                                 NC1264.2
056400     IF TBGRP-3   EQUAL TO SPACE                                  NC1264.2
056500              PERFORM PASS                                        NC1264.2
056600              GO  TO   LEV-WRITE-GF-1-3.                          NC1264.2
056700     MOVE    TABLE-GROUP           TO COMPUTED-BREAKDOWN.         NC1264.2
056800     MOVE    SPACE                 TO CORRECT-BREAKDOWN.          NC1264.2
056900     MOVE  90     TO LENGTH-COUNTER.                              NC1264.2
057000     GO   TO    LEV-WRITE-GF-1-3.                                 NC1264.2
057100 LEV-DELETE-GF-1-3.                                               NC1264.2
057200     PERFORM DE-LETE.                                             NC1264.2
057300 LEV-WRITE-GF-1-3.                                                NC1264.2
057400     MOVE  "LEV-TEST-GF-1-3"  TO PAR-NAME.                        NC1264.2
057500     IF       LENGTH-COUNTER GREATER THAN ZERO                    NC1264.2
057600              PERFORM BREAKDOWN-PARA THRU BREAKDOWN-EXIT.         NC1264.2
057700     PERFORM PRINT-DETAIL.                                        NC1264.2
057800 LEV-TEST-GF-1-4.                                                 NC1264.2
057900     IF TBGRP-4   EQUAL TO SPACE                                  NC1264.2
058000              PERFORM PASS                                        NC1264.2
058100              GO  TO   LEV-WRITE-GF-1-4.                          NC1264.2
058200     MOVE    TABLE-GROUP           TO COMPUTED-BREAKDOWN.         NC1264.2
058300     MOVE    SPACE                 TO CORRECT-BREAKDOWN.          NC1264.2
058400     MOVE  88     TO LENGTH-COUNTER.                              NC1264.2
058500     GO   TO    LEV-WRITE-GF-1-4.                                 NC1264.2
058600 LEV-DELETE-GF-1-4.                                               NC1264.2
058700     PERFORM DE-LETE.                                             NC1264.2
058800 LEV-WRITE-GF-1-4.                                                NC1264.2
058900     MOVE  "LEV-TEST-GF-1-4"  TO PAR-NAME.                        NC1264.2
059000     IF       LENGTH-COUNTER GREATER THAN ZERO                    NC1264.2
059100              PERFORM BREAKDOWN-PARA THRU BREAKDOWN-EXIT.         NC1264.2
059200     PERFORM PRINT-DETAIL.                                        NC1264.2
059300 LEV-TEST-GF-1-5.                                                 NC1264.2
059400     IF TBGRP-5   EQUAL TO   SPACE                                NC1264.2
059500              PERFORM PASS                                        NC1264.2
059600              GO  TO   LEV-WRITE-GF-1-5.                          NC1264.2
059700     MOVE    TABLE-GROUP           TO COMPUTED-BREAKDOWN.         NC1264.2
059800     MOVE    SPACE                 TO CORRECT-BREAKDOWN.          NC1264.2
059900     MOVE  86     TO LENGTH-COUNTER.                              NC1264.2
060000     GO   TO    LEV-WRITE-GF-1-5.                                 NC1264.2
060100 LEV-DELETE-GF-1-5.                                               NC1264.2
060200     PERFORM DE-LETE.                                             NC1264.2
060300 LEV-WRITE-GF-1-5.                                                NC1264.2
060400     MOVE  "LEV-TEST-GF-1-5"  TO PAR-NAME.                        NC1264.2
060500     IF       LENGTH-COUNTER GREATER THAN ZERO                    NC1264.2
060600              PERFORM BREAKDOWN-PARA THRU BREAKDOWN-EXIT.         NC1264.2
060700     PERFORM PRINT-DETAIL.                                        NC1264.2
060800 LEV-TEST-GF-1-6.                                                 NC1264.2
060900     IF TBGRP-6   EQUAL TO   SPACE                                NC1264.2
061000              PERFORM PASS                                        NC1264.2
061100              GO  TO   LEV-WRITE-GF-1-6.                          NC1264.2
061200     MOVE    TABLE-GROUP           TO COMPUTED-BREAKDOWN.         NC1264.2
061300     MOVE    SPACE                 TO CORRECT-BREAKDOWN.          NC1264.2
061400     MOVE  84     TO LENGTH-COUNTER.                              NC1264.2
061500     GO   TO    LEV-WRITE-GF-1-6.                                 NC1264.2
061600 LEV-DELETE-GF-1-6.                                               NC1264.2
061700     PERFORM DE-LETE.                                             NC1264.2
061800 LEV-WRITE-GF-1-6.                                                NC1264.2
061900     MOVE  "LEV-TEST-GF-1-6"  TO PAR-NAME.                        NC1264.2
062000     IF       LENGTH-COUNTER GREATER THAN ZERO                    NC1264.2
062100              PERFORM BREAKDOWN-PARA THRU BREAKDOWN-EXIT.         NC1264.2
062200     PERFORM PRINT-DETAIL.                                        NC1264.2
062300 LEV-TEST-GF-1-7.                                                 NC1264.2
062400     IF TBGRP-7   EQUAL TO   SPACE                                NC1264.2
062500              PERFORM PASS                                        NC1264.2
062600              GO  TO   LEV-WRITE-GF-1-7.                          NC1264.2
062700     MOVE    TABLE-GROUP           TO COMPUTED-BREAKDOWN.         NC1264.2
062800     MOVE    SPACE                 TO CORRECT-BREAKDOWN.          NC1264.2
062900     MOVE  82     TO LENGTH-COUNTER.                              NC1264.2
063000     GO   TO    LEV-WRITE-GF-1-7.                                 NC1264.2
063100 LEV-DELETE-GF-1-7.                                               NC1264.2
063200     PERFORM DE-LETE.                                             NC1264.2
063300 LEV-WRITE-GF-1-7.                                                NC1264.2
063400     MOVE  "LEV-TEST-GF-1-7"  TO PAR-NAME.                        NC1264.2
063500     IF       LENGTH-COUNTER GREATER THAN ZERO                    NC1264.2
063600              PERFORM BREAKDOWN-PARA THRU BREAKDOWN-EXIT.         NC1264.2
063700     PERFORM PRINT-DETAIL.                                        NC1264.2
063800 LEV-TEST-GF-1-8.                                                 NC1264.2
063900     IF TBGRP-8   EQUAL TO   SPACE                                NC1264.2
064000              PERFORM PASS                                        NC1264.2
064100              GO  TO   LEV-WRITE-GF-1-8.                          NC1264.2
064200     MOVE    TABLE-GROUP           TO COMPUTED-BREAKDOWN.         NC1264.2
064300     MOVE    SPACE                 TO CORRECT-BREAKDOWN.          NC1264.2
064400     MOVE  80     TO LENGTH-COUNTER.                              NC1264.2
064500     GO   TO    LEV-WRITE-GF-1-8.                                 NC1264.2
064600 LEV-DELETE-GF-1-8.                                               NC1264.2
064700     PERFORM DE-LETE.                                             NC1264.2
064800 LEV-WRITE-GF-1-8.                                                NC1264.2
064900     MOVE  "LEV-TEST-GF-1-8"  TO PAR-NAME.                        NC1264.2
065000     IF       LENGTH-COUNTER GREATER THAN ZERO                    NC1264.2
065100              PERFORM BREAKDOWN-PARA THRU BREAKDOWN-EXIT.         NC1264.2
065200     PERFORM PRINT-DETAIL.                                        NC1264.2
065300 LEV-TEST-GF-1-9.                                                 NC1264.2
065400     IF TBGRP-9   EQUAL TO   SPACE                                NC1264.2
065500              PERFORM PASS                                        NC1264.2
065600              GO  TO   LEV-WRITE-GF-1-9.                          NC1264.2
065700     MOVE    TABLE-GROUP           TO COMPUTED-BREAKDOWN.         NC1264.2
065800     MOVE    SPACE                 TO CORRECT-BREAKDOWN.          NC1264.2
065900     MOVE  78     TO LENGTH-COUNTER.                              NC1264.2
066000     GO   TO    LEV-WRITE-GF-1-9.                                 NC1264.2
066100 LEV-DELETE-GF-1-9.                                               NC1264.2
066200     PERFORM DE-LETE.                                             NC1264.2
066300 LEV-WRITE-GF-1-9.                                                NC1264.2
066400     MOVE  "LEV-TEST-GF-1-9"  TO PAR-NAME.                        NC1264.2
066500     IF       LENGTH-COUNTER GREATER THAN ZERO                    NC1264.2
066600              PERFORM BREAKDOWN-PARA THRU BREAKDOWN-EXIT.         NC1264.2
066700     PERFORM PRINT-DETAIL.                                        NC1264.2
066800 LEV-TEST-GF-10.                                                  NC1264.2
066900     IF TBGRP-10 EQUAL TO    SPACE                                NC1264.2
067000              PERFORM PASS                                        NC1264.2
067100              GO  TO   LEV-WRITE-GF-1-10.                         NC1264.2
067200     MOVE    TABLE-GROUP           TO COMPUTED-BREAKDOWN.         NC1264.2
067300     MOVE    SPACE                 TO CORRECT-BREAKDOWN.          NC1264.2
067400     MOVE  76     TO LENGTH-COUNTER.                              NC1264.2
067500     GO   TO    LEV-WRITE-GF-1-10.                                NC1264.2
067600 LEV-DELETE-GF-1-10.                                              NC1264.2
067700     PERFORM DE-LETE.                                             NC1264.2
067800 LEV-WRITE-GF-1-10.                                               NC1264.2
067900     MOVE  "LEV-TEST-GF-1-10"  TO PAR-NAME.                       NC1264.2
068000     IF       LENGTH-COUNTER GREATER THAN ZERO                    NC1264.2
068100              PERFORM BREAKDOWN-PARA THRU BREAKDOWN-EXIT.         NC1264.2
068200     PERFORM PRINT-DETAIL.                                        NC1264.2
068300 LEV-TEST-GF-1-11.                                                NC1264.2
068400     IF TBGRP-11 EQUAL TO    SPACE                                NC1264.2
068500              PERFORM PASS                                        NC1264.2
068600              GO  TO   LEV-WRITE-GF-1-11.                         NC1264.2
068700     MOVE    TABLE-GROUP           TO COMPUTED-BREAKDOWN.         NC1264.2
068800     MOVE    SPACE                 TO CORRECT-BREAKDOWN.          NC1264.2
068900     MOVE  74     TO LENGTH-COUNTER.                              NC1264.2
069000     GO   TO    LEV-WRITE-GF-1-11.                                NC1264.2
069100 LEV-DELETE-GF-1-11.                                              NC1264.2
069200     PERFORM DE-LETE.                                             NC1264.2
069300 LEV-WRITE-GF-1-11.                                               NC1264.2
069400     MOVE  "LEV-TEST-GF-1-11"  TO PAR-NAME.                       NC1264.2
069500     IF       LENGTH-COUNTER GREATER THAN ZERO                    NC1264.2
069600              PERFORM BREAKDOWN-PARA THRU BREAKDOWN-EXIT.         NC1264.2
069700     PERFORM PRINT-DETAIL.                                        NC1264.2
069800 LEV-TEST-GF-1-12.                                                NC1264.2
069900     IF TBGRP-12 EQUAL TO    SPACE                                NC1264.2
070000              PERFORM PASS                                        NC1264.2
070100              GO  TO   LEV-WRITE-GF-1-12.                         NC1264.2
070200     MOVE    TABLE-GROUP           TO COMPUTED-BREAKDOWN.         NC1264.2
070300     MOVE    SPACE                 TO CORRECT-BREAKDOWN.          NC1264.2
070400     MOVE  72     TO LENGTH-COUNTER.                              NC1264.2
070500     GO   TO    LEV-WRITE-GF-1-12.                                NC1264.2
070600 LEV-DELETE-GF-1-12.                                              NC1264.2
070700     PERFORM DE-LETE.                                             NC1264.2
070800 LEV-WRITE-GF-1-12.                                               NC1264.2
070900     MOVE  "LEV-TEST-GF-1-12"  TO PAR-NAME.                       NC1264.2
071000     IF       LENGTH-COUNTER GREATER THAN ZERO                    NC1264.2
071100              PERFORM BREAKDOWN-PARA THRU BREAKDOWN-EXIT.         NC1264.2
071200     PERFORM PRINT-DETAIL.                                        NC1264.2
071300 LEV-TEST-GF-13.                                                  NC1264.2
071400     IF TBGRP-13 EQUAL TO    SPACE                                NC1264.2
071500              PERFORM PASS                                        NC1264.2
071600              GO  TO   LEV-WRITE-GF-1-13.                         NC1264.2
071700     MOVE    TABLE-GROUP           TO COMPUTED-BREAKDOWN.         NC1264.2
071800     MOVE    SPACE                 TO CORRECT-BREAKDOWN.          NC1264.2
071900     MOVE  70     TO LENGTH-COUNTER.                              NC1264.2
072000     GO   TO    LEV-WRITE-GF-1-13.                                NC1264.2
072100 LEV-DELETE-GF-1-13.                                              NC1264.2
072200     PERFORM DE-LETE.                                             NC1264.2
072300 LEV-WRITE-GF-1-13.                                               NC1264.2
072400     MOVE  "LEV-TEST-GF-1-13"  TO PAR-NAME.                       NC1264.2
072500     IF       LENGTH-COUNTER GREATER THAN ZERO                    NC1264.2
072600              PERFORM BREAKDOWN-PARA THRU BREAKDOWN-EXIT.         NC1264.2
072700     PERFORM PRINT-DETAIL.                                        NC1264.2
072800 LEV-TEST-GF-1-14.                                                NC1264.2
072900     IF TBGRP-14 EQUAL TO    SPACE                                NC1264.2
073000              PERFORM PASS                                        NC1264.2
073100              GO  TO   LEV-WRITE-GF-1-14.                         NC1264.2
073200     MOVE    TABLE-GROUP           TO COMPUTED-BREAKDOWN.         NC1264.2
073300     MOVE    SPACE                 TO CORRECT-BREAKDOWN.          NC1264.2
073400     MOVE  68     TO LENGTH-COUNTER.                              NC1264.2
073500     GO   TO    LEV-WRITE-GF-1-14.                                NC1264.2
073600 LEV-DELETE-GF-1-14.                                              NC1264.2
073700     PERFORM DE-LETE.                                             NC1264.2
073800 LEV-WRITE-GF-1-14.                                               NC1264.2
073900     MOVE  "LEV-TEST-GF-1-14"  TO PAR-NAME.                       NC1264.2
074000     IF       LENGTH-COUNTER GREATER THAN ZERO                    NC1264.2
074100              PERFORM BREAKDOWN-PARA THRU BREAKDOWN-EXIT.         NC1264.2
074200     PERFORM PRINT-DETAIL.                                        NC1264.2
074300 LEV-TEST-GF-1-15.                                                NC1264.2
074400     IF TBGRP-15 EQUAL TO    SPACE                                NC1264.2
074500              PERFORM PASS                                        NC1264.2
074600              GO  TO   LEV-WRITE-GF-1-15.                         NC1264.2
074700     MOVE    TABLE-GROUP           TO COMPUTED-BREAKDOWN.         NC1264.2
074800     MOVE    SPACE                 TO CORRECT-BREAKDOWN.          NC1264.2
074900     MOVE  66     TO LENGTH-COUNTER.                              NC1264.2
075000     GO   TO    LEV-WRITE-GF-1-15.                                NC1264.2
075100 LEV-DELETE-GF-1-15.                                              NC1264.2
075200     PERFORM DE-LETE.                                             NC1264.2
075300 LEV-WRITE-GF-1-15.                                               NC1264.2
075400     MOVE  "LEV-TEST-GF-1-15"  TO PAR-NAME.                       NC1264.2
075500     IF       LENGTH-COUNTER GREATER THAN ZERO                    NC1264.2
075600              PERFORM BREAKDOWN-PARA THRU BREAKDOWN-EXIT.         NC1264.2
075700     PERFORM PRINT-DETAIL.                                        NC1264.2
075800 LEV-TEST-GF-1-16.                                                NC1264.2
075900     IF TBGRP-16 EQUAL TO    SPACE                                NC1264.2
076000              PERFORM PASS                                        NC1264.2
076100              GO  TO   LEV-WRITE-GF-1-16.                         NC1264.2
076200     MOVE    TABLE-GROUP           TO COMPUTED-BREAKDOWN.         NC1264.2
076300     MOVE    SPACE                 TO CORRECT-BREAKDOWN.          NC1264.2
076400     MOVE  64     TO LENGTH-COUNTER.                              NC1264.2
076500     GO   TO    LEV-WRITE-GF-1-16.                                NC1264.2
076600 LEV-DELETE-GF-1-16.                                              NC1264.2
076700     PERFORM DE-LETE.                                             NC1264.2
076800 LEV-WRITE-GF-1-16.                                               NC1264.2
076900     MOVE  "LEV-TEST-GF-1-16"  TO PAR-NAME.                       NC1264.2
077000     IF       LENGTH-COUNTER GREATER THAN ZERO                    NC1264.2
077100              PERFORM BREAKDOWN-PARA THRU BREAKDOWN-EXIT.         NC1264.2
077200     PERFORM PRINT-DETAIL.                                        NC1264.2
077300 LEV-TEST-GF-1-17.                                                NC1264.2
077400     IF TBGRP-17 EQUAL TO    SPACE                                NC1264.2
077500              PERFORM PASS                                        NC1264.2
077600              GO  TO   LEV-WRITE-GF-1-17.                         NC1264.2
077700     MOVE    TABLE-GROUP           TO COMPUTED-BREAKDOWN.         NC1264.2
077800     MOVE    SPACE                 TO CORRECT-BREAKDOWN.          NC1264.2
077900     MOVE  62     TO LENGTH-COUNTER.                              NC1264.2
078000     GO   TO    LEV-WRITE-GF-1-17.                                NC1264.2
078100 LEV-DELETE-GF-1-17.                                              NC1264.2
078200     PERFORM DE-LETE.                                             NC1264.2
078300 LEV-WRITE-GF-1-17.                                               NC1264.2
078400     MOVE  "LEV-TEST-GF-1-17"  TO PAR-NAME.                       NC1264.2
078500     IF       LENGTH-COUNTER GREATER THAN ZERO                    NC1264.2
078600              PERFORM BREAKDOWN-PARA THRU BREAKDOWN-EXIT.         NC1264.2
078700     PERFORM PRINT-DETAIL.                                        NC1264.2
078800 LEV-TEST-GF-1-18.                                                NC1264.2
078900     IF TBGRP-18 EQUAL TO    SPACE                                NC1264.2
079000              PERFORM PASS                                        NC1264.2
079100              GO  TO   LEV-WRITE-GF-1-18.                         NC1264.2
079200     MOVE    TABLE-GROUP           TO COMPUTED-BREAKDOWN.         NC1264.2
079300     MOVE    SPACE                 TO CORRECT-BREAKDOWN.          NC1264.2
079400     MOVE  60     TO LENGTH-COUNTER.                              NC1264.2
079500     GO   TO    LEV-WRITE-GF-1-18.                                NC1264.2
079600 LEV-DELETE-GF-1-18.                                              NC1264.2
079700     PERFORM DE-LETE.                                             NC1264.2
079800 LEV-WRITE-GF-1-18.                                               NC1264.2
079900     MOVE  "LEV-TEST-GF-1-18"  TO PAR-NAME.                       NC1264.2
080000     IF       LENGTH-COUNTER GREATER THAN ZERO                    NC1264.2
080100              PERFORM BREAKDOWN-PARA THRU BREAKDOWN-EXIT.         NC1264.2
080200     PERFORM PRINT-DETAIL.                                        NC1264.2
080300 LEV-TEST-GF-1-19.                                                NC1264.2
080400     IF TBGRP-19 EQUAL TO    SPACE                                NC1264.2
080500              PERFORM PASS                                        NC1264.2
080600              GO  TO   LEV-WRITE-GF-1-19.                         NC1264.2
080700     MOVE    TABLE-GROUP           TO COMPUTED-BREAKDOWN.         NC1264.2
080800     MOVE    SPACE                 TO CORRECT-BREAKDOWN.          NC1264.2
080900     MOVE  58     TO LENGTH-COUNTER.                              NC1264.2
081000     GO   TO    LEV-WRITE-GF-1-19.                                NC1264.2
081100 LEV-DELETE-GF-1-19.                                              NC1264.2
081200     PERFORM DE-LETE.                                             NC1264.2
081300 LEV-WRITE-GF-1-19.                                               NC1264.2
081400     MOVE  "LEV-TEST-GF-1-19"  TO PAR-NAME.                       NC1264.2
081500     IF       LENGTH-COUNTER GREATER THAN ZERO                    NC1264.2
081600              PERFORM BREAKDOWN-PARA THRU BREAKDOWN-EXIT.         NC1264.2
081700     PERFORM PRINT-DETAIL.                                        NC1264.2
081800 LEV-TEST-GF-1-20.                                                NC1264.2
081900     IF TBGRP-20 EQUAL TO    SPACE                                NC1264.2
082000              PERFORM PASS                                        NC1264.2
082100              GO  TO   LEV-WRITE-GF-1-20.                         NC1264.2
082200     MOVE    TABLE-GROUP           TO COMPUTED-BREAKDOWN.         NC1264.2
082300     MOVE    SPACE                 TO CORRECT-BREAKDOWN.          NC1264.2
082400     MOVE  56     TO LENGTH-COUNTER.                              NC1264.2
082500     GO   TO    LEV-WRITE-GF-1-20.                                NC1264.2
082600 LEV-DELETE-GF-1-20.                                              NC1264.2
082700     PERFORM DE-LETE.                                             NC1264.2
082800 LEV-WRITE-GF-1-20.                                               NC1264.2
082900     MOVE  "LEV-TEST-GF-1-20"  TO PAR-NAME.                       NC1264.2
083000     IF       LENGTH-COUNTER GREATER THAN ZERO                    NC1264.2
083100              PERFORM BREAKDOWN-PARA THRU BREAKDOWN-EXIT.         NC1264.2
083200     PERFORM PRINT-DETAIL.                                        NC1264.2
083300 LEV-TEST-GF-1-21.                                                NC1264.2
083400     IF TBGRP-21 EQUAL TO    SPACE                                NC1264.2
083500              PERFORM PASS                                        NC1264.2
083600              GO  TO   LEV-WRITE-GF-1-21.                         NC1264.2
083700     MOVE    TABLE-GROUP           TO COMPUTED-BREAKDOWN.         NC1264.2
083800     MOVE    SPACE                 TO CORRECT-BREAKDOWN.          NC1264.2
083900     MOVE  54     TO LENGTH-COUNTER.                              NC1264.2
084000     GO   TO    LEV-WRITE-GF-1-21.                                NC1264.2
084100 LEV-DELETE-GF-1-21.                                              NC1264.2
084200     PERFORM DE-LETE.                                             NC1264.2
084300 LEV-WRITE-GF-1-21.                                               NC1264.2
084400     MOVE  "LEV-TEST-GF-1-21"  TO PAR-NAME.                       NC1264.2
084500     IF       LENGTH-COUNTER GREATER THAN ZERO                    NC1264.2
084600              PERFORM BREAKDOWN-PARA THRU BREAKDOWN-EXIT.         NC1264.2
084700     PERFORM PRINT-DETAIL.                                        NC1264.2
084800 LEV-TEST-GF-1-22.                                                NC1264.2
084900     IF TBGRP-22 EQUAL TO    SPACE                                NC1264.2
085000              PERFORM PASS                                        NC1264.2
085100              GO  TO   LEV-WRITE-GF-1-22.                         NC1264.2
085200     MOVE    TABLE-GROUP           TO COMPUTED-BREAKDOWN.         NC1264.2
085300     MOVE    SPACE                 TO CORRECT-BREAKDOWN.          NC1264.2
085400     MOVE  52     TO LENGTH-COUNTER.                              NC1264.2
085500     GO   TO    LEV-WRITE-GF-1-22.                                NC1264.2
085600 LEV-DELETE-GF-1-22.                                              NC1264.2
085700     PERFORM DE-LETE.                                             NC1264.2
085800 LEV-WRITE-GF-1-22.                                               NC1264.2
085900     MOVE  "LEV-TEST-GF-1-22"  TO PAR-NAME.                       NC1264.2
086000     IF       LENGTH-COUNTER GREATER THAN ZERO                    NC1264.2
086100              PERFORM BREAKDOWN-PARA THRU BREAKDOWN-EXIT.         NC1264.2
086200     PERFORM PRINT-DETAIL.                                        NC1264.2
086300 LEV-TEST-GF-1-23.                                                NC1264.2
086400     IF TBGRP-23 EQUAL TO    SPACE                                NC1264.2
086500              PERFORM PASS                                        NC1264.2
086600              GO  TO   LEV-WRITE-GF-1-23.                         NC1264.2
086700     MOVE    TABLE-GROUP           TO COMPUTED-BREAKDOWN.         NC1264.2
086800     MOVE    SPACE                 TO CORRECT-BREAKDOWN.          NC1264.2
086900     MOVE  50     TO LENGTH-COUNTER.                              NC1264.2
087000     GO   TO    LEV-WRITE-GF-1-23.                                NC1264.2
087100 LEV-DELETE-GF-1-23.                                              NC1264.2
087200     PERFORM DE-LETE.                                             NC1264.2
087300 LEV-WRITE-GF-1-23.                                               NC1264.2
087400     MOVE  "LEV-TEST-GF-1-23"  TO PAR-NAME.                       NC1264.2
087500     IF       LENGTH-COUNTER GREATER THAN ZERO                    NC1264.2
087600              PERFORM BREAKDOWN-PARA THRU BREAKDOWN-EXIT.         NC1264.2
087700     PERFORM PRINT-DETAIL.                                        NC1264.2
087800 LEV-TEST-GF-1-24.                                                NC1264.2
087900     IF TBGRP-24 EQUAL TO    SPACE                                NC1264.2
088000              PERFORM PASS                                        NC1264.2
088100              GO  TO   LEV-WRITE-GF-1-24.                         NC1264.2
088200     MOVE    TABLE-GROUP           TO COMPUTED-BREAKDOWN.         NC1264.2
088300     MOVE    SPACE                 TO CORRECT-BREAKDOWN.          NC1264.2
088400     MOVE  48     TO LENGTH-COUNTER.                              NC1264.2
088500     GO   TO    LEV-WRITE-GF-1-24.                                NC1264.2
088600 LEV-DELETE-GF-1-24.                                              NC1264.2
088700     PERFORM DE-LETE.                                             NC1264.2
088800 LEV-WRITE-GF-1-24.                                               NC1264.2
088900     MOVE  "LEV-TEST-GF-1-24"  TO PAR-NAME.                       NC1264.2
089000     IF       LENGTH-COUNTER GREATER THAN ZERO                    NC1264.2
089100              PERFORM BREAKDOWN-PARA THRU BREAKDOWN-EXIT.         NC1264.2
089200     PERFORM PRINT-DETAIL.                                        NC1264.2
089300 LEV-TEST-GF-1-25.                                                NC1264.2
089400     IF TBGRP-25 EQUAL TO    SPACE                                NC1264.2
089500              PERFORM PASS                                        NC1264.2
089600              GO  TO   LEV-WRITE-GF-1-25.                         NC1264.2
089700     MOVE    TABLE-GROUP           TO COMPUTED-BREAKDOWN.         NC1264.2
089800     MOVE    SPACE                 TO CORRECT-BREAKDOWN.          NC1264.2
089900     MOVE  46     TO LENGTH-COUNTER.                              NC1264.2
090000     GO   TO    LEV-WRITE-GF-1-25.                                NC1264.2
090100 LEV-DELETE-GF-1-25.                                              NC1264.2
090200     PERFORM DE-LETE.                                             NC1264.2
090300 LEV-WRITE-GF-1-25.                                               NC1264.2
090400     MOVE  "LEV-TEST-GF-1-25"  TO PAR-NAME.                       NC1264.2
090500     IF       LENGTH-COUNTER GREATER THAN ZERO                    NC1264.2
090600              PERFORM BREAKDOWN-PARA THRU BREAKDOWN-EXIT.         NC1264.2
090700     PERFORM PRINT-DETAIL.                                        NC1264.2
090800 LEV-TEST-GF-1-26.                                                NC1264.2
090900     IF TBGRP-26 EQUAL TO    SPACE                                NC1264.2
091000              PERFORM PASS                                        NC1264.2
091100              GO  TO   LEV-WRITE-GF-1-26.                         NC1264.2
091200     MOVE    TABLE-GROUP           TO COMPUTED-BREAKDOWN.         NC1264.2
091300     MOVE    SPACE                 TO CORRECT-BREAKDOWN.          NC1264.2
091400     MOVE  44     TO LENGTH-COUNTER.                              NC1264.2
091500     GO   TO    LEV-WRITE-GF-1-26.                                NC1264.2
091600 LEV-DELETE-GF-1-26.                                              NC1264.2
091700     PERFORM DE-LETE.                                             NC1264.2
091800 LEV-WRITE-GF-1-26.                                               NC1264.2
091900     MOVE  "LEV-TEST-GF-1-26"  TO PAR-NAME.                       NC1264.2
092000     IF       LENGTH-COUNTER GREATER THAN ZERO                    NC1264.2
092100              PERFORM BREAKDOWN-PARA THRU BREAKDOWN-EXIT.         NC1264.2
092200     PERFORM PRINT-DETAIL.                                        NC1264.2
092300 LEV-TEST-GF-1-27.                                                NC1264.2
092400     IF TBGRP-27 EQUAL TO    SPACE                                NC1264.2
092500              PERFORM PASS                                        NC1264.2
092600              GO  TO   LEV-WRITE-GF-1-27.                         NC1264.2
092700     MOVE    TABLE-GROUP           TO COMPUTED-BREAKDOWN.         NC1264.2
092800     MOVE    SPACE                 TO CORRECT-BREAKDOWN.          NC1264.2
092900     MOVE  42     TO LENGTH-COUNTER.                              NC1264.2
093000     GO   TO    LEV-WRITE-GF-1-27.                                NC1264.2
093100 LEV-DELETE-GF-1-27.                                              NC1264.2
093200     PERFORM DE-LETE.                                             NC1264.2
093300 LEV-WRITE-GF-1-27.                                               NC1264.2
093400     MOVE  "LEV-TEST-GF-1-27"  TO PAR-NAME.                       NC1264.2
093500     IF       LENGTH-COUNTER GREATER THAN ZERO                    NC1264.2
093600              PERFORM BREAKDOWN-PARA THRU BREAKDOWN-EXIT.         NC1264.2
093700     PERFORM PRINT-DETAIL.                                        NC1264.2
093800 LEV-TEST-GF-1-28.                                                NC1264.2
093900     IF TBGRP-28 EQUAL TO    SPACE                                NC1264.2
094000              PERFORM PASS                                        NC1264.2
094100              GO  TO   LEV-WRITE-GF-1-28.                         NC1264.2
094200     MOVE    TABLE-GROUP           TO COMPUTED-BREAKDOWN.         NC1264.2
094300     MOVE    SPACE                 TO CORRECT-BREAKDOWN.          NC1264.2
094400     MOVE  40     TO LENGTH-COUNTER.                              NC1264.2
094500     GO   TO    LEV-WRITE-GF-1-28.                                NC1264.2
094600 LEV-DELETE-GF-1-28.                                              NC1264.2
094700     PERFORM DE-LETE.                                             NC1264.2
094800 LEV-WRITE-GF-1-28.                                               NC1264.2
094900     MOVE  "LEV-TEST-GF-1-28"  TO PAR-NAME.                       NC1264.2
095000     IF       LENGTH-COUNTER GREATER THAN ZERO                    NC1264.2
095100              PERFORM BREAKDOWN-PARA THRU BREAKDOWN-EXIT.         NC1264.2
095200     PERFORM PRINT-DETAIL.                                        NC1264.2
095300 LEV-TEST-GF-1-29.                                                NC1264.2
095400     IF TBGRP-29 EQUAL TO    SPACE                                NC1264.2
095500              PERFORM PASS                                        NC1264.2
095600              GO  TO   LEV-WRITE-GF-1-29.                         NC1264.2
095700     MOVE    TABLE-GROUP           TO COMPUTED-BREAKDOWN.         NC1264.2
095800     MOVE    SPACE                 TO CORRECT-BREAKDOWN.          NC1264.2
095900     MOVE  38     TO LENGTH-COUNTER.                              NC1264.2
096000     GO   TO    LEV-WRITE-GF-1-29.                                NC1264.2
096100 LEV-DELETE-GF-1-29.                                              NC1264.2
096200     PERFORM DE-LETE.                                             NC1264.2
096300 LEV-WRITE-GF-1-29.                                               NC1264.2
096400     MOVE  "LEV-TEST-GF-1-29"  TO PAR-NAME.                       NC1264.2
096500     IF       LENGTH-COUNTER GREATER THAN ZERO                    NC1264.2
096600              PERFORM BREAKDOWN-PARA THRU BREAKDOWN-EXIT.         NC1264.2
096700     PERFORM PRINT-DETAIL.                                        NC1264.2
096800 LEV-TEST-GF-1-30.                                                NC1264.2
096900     IF TBGRP-30 EQUAL TO    SPACE                                NC1264.2
097000              PERFORM PASS                                        NC1264.2
097100              GO  TO   LEV-WRITE-GF-1-30.                         NC1264.2
097200     MOVE    TABLE-GROUP           TO COMPUTED-BREAKDOWN.         NC1264.2
097300     MOVE    SPACE                 TO CORRECT-BREAKDOWN.          NC1264.2
097400     MOVE  36     TO LENGTH-COUNTER.                              NC1264.2
097500     GO   TO    LEV-WRITE-GF-1-30.                                NC1264.2
097600 LEV-DELETE-GF-1-30.                                              NC1264.2
097700     PERFORM DE-LETE.                                             NC1264.2
097800 LEV-WRITE-GF-1-30.                                               NC1264.2
097900     MOVE  "LEV-TEST-GF-1-30"  TO PAR-NAME.                       NC1264.2
098000     IF       LENGTH-COUNTER GREATER THAN ZERO                    NC1264.2
098100              PERFORM BREAKDOWN-PARA THRU BREAKDOWN-EXIT.         NC1264.2
098200     PERFORM PRINT-DETAIL.                                        NC1264.2
098300 LEV-TEST-GF-1-31.                                                NC1264.2
098400     IF TBGRP-31 EQUAL TO    SPACE                                NC1264.2
098500              PERFORM PASS                                        NC1264.2
098600              GO  TO   LEV-WRITE-GF-1-31.                         NC1264.2
098700     MOVE    TABLE-GROUP           TO COMPUTED-BREAKDOWN.         NC1264.2
098800     MOVE    SPACE                 TO CORRECT-BREAKDOWN.          NC1264.2
098900     MOVE  34     TO LENGTH-COUNTER.                              NC1264.2
099000     GO   TO    LEV-WRITE-GF-1-31.                                NC1264.2
099100 LEV-DELETE-GF-1-31.                                              NC1264.2
099200     PERFORM DE-LETE.                                             NC1264.2
099300 LEV-WRITE-GF-1-31.                                               NC1264.2
099400     MOVE  "LEV-TEST-GF-1-31"  TO PAR-NAME.                       NC1264.2
099500     IF       LENGTH-COUNTER GREATER THAN ZERO                    NC1264.2
099600              PERFORM BREAKDOWN-PARA THRU BREAKDOWN-EXIT.         NC1264.2
099700     PERFORM PRINT-DETAIL.                                        NC1264.2
099800 LEV-TEST-GF-1-32.                                                NC1264.2
099900     IF TBGRP-32 EQUAL TO    SPACE                                NC1264.2
100000              PERFORM PASS                                        NC1264.2
100100              GO  TO   LEV-WRITE-GF-1-32.                         NC1264.2
100200     MOVE    TABLE-GROUP           TO COMPUTED-BREAKDOWN.         NC1264.2
100300     MOVE    SPACE                 TO CORRECT-BREAKDOWN.          NC1264.2
100400     MOVE  32     TO LENGTH-COUNTER.                              NC1264.2
100500     GO   TO    LEV-WRITE-GF-1-32.                                NC1264.2
100600 LEV-DELETE-GF-1-32.                                              NC1264.2
100700     PERFORM DE-LETE.                                             NC1264.2
100800 LEV-WRITE-GF-1-32.                                               NC1264.2
100900     MOVE  "LEV-TEST-GF-1-32"  TO PAR-NAME.                       NC1264.2
101000     IF       LENGTH-COUNTER GREATER THAN ZERO                    NC1264.2
101100              PERFORM BREAKDOWN-PARA THRU BREAKDOWN-EXIT.         NC1264.2
101200     PERFORM PRINT-DETAIL.                                        NC1264.2
101300 LEV-TEST-GF-1-33.                                                NC1264.2
101400     IF TBGRP-33 EQUAL TO    SPACE                                NC1264.2
101500              PERFORM PASS                                        NC1264.2
101600              GO  TO   LEV-WRITE-GF-1-33.                         NC1264.2
101700     MOVE    TABLE-GROUP           TO COMPUTED-BREAKDOWN.         NC1264.2
101800     MOVE    SPACE                 TO CORRECT-BREAKDOWN.          NC1264.2
101900     MOVE  30     TO LENGTH-COUNTER.                              NC1264.2
102000     GO   TO    LEV-WRITE-GF-1-33.                                NC1264.2
102100 LEV-DELETE-GF-1-33.                                              NC1264.2
102200     PERFORM DE-LETE.                                             NC1264.2
102300 LEV-WRITE-GF-1-33.                                               NC1264.2
102400     MOVE  "LEV-TEST-GF-1-33"  TO PAR-NAME.                       NC1264.2
102500     IF       LENGTH-COUNTER GREATER THAN ZERO                    NC1264.2
102600              PERFORM BREAKDOWN-PARA THRU BREAKDOWN-EXIT.         NC1264.2
102700     PERFORM PRINT-DETAIL.                                        NC1264.2
102800 LEV-TEST-GF-1-34.                                                NC1264.2
102900     IF TBGRP-34 EQUAL TO    SPACE                                NC1264.2
103000              PERFORM PASS                                        NC1264.2
103100              GO  TO   LEV-WRITE-GF-1-34.                         NC1264.2
103200     MOVE    TABLE-GROUP           TO COMPUTED-BREAKDOWN.         NC1264.2
103300     MOVE    SPACE                 TO CORRECT-BREAKDOWN.          NC1264.2
103400     MOVE  28     TO LENGTH-COUNTER.                              NC1264.2
103500     GO   TO    LEV-WRITE-GF-1-34.                                NC1264.2
103600 LEV-DELETE-GF-1-34.                                              NC1264.2
103700     PERFORM DE-LETE.                                             NC1264.2
103800 LEV-WRITE-GF-1-34.                                               NC1264.2
103900     MOVE  "LEV-TEST-GF-1-34"  TO PAR-NAME.                       NC1264.2
104000     IF       LENGTH-COUNTER GREATER THAN ZERO                    NC1264.2
104100              PERFORM BREAKDOWN-PARA THRU BREAKDOWN-EXIT.         NC1264.2
104200     PERFORM PRINT-DETAIL.                                        NC1264.2
104300 LEV-TEST-GF-1-35.                                                NC1264.2
104400     IF TBGRP-35 EQUAL TO    SPACE                                NC1264.2
104500              PERFORM PASS                                        NC1264.2
104600              GO  TO   LEV-WRITE-GF-1-35.                         NC1264.2
104700     MOVE    TABLE-GROUP           TO COMPUTED-BREAKDOWN.         NC1264.2
104800     MOVE    SPACE                 TO CORRECT-BREAKDOWN.          NC1264.2
104900     MOVE  26     TO LENGTH-COUNTER.                              NC1264.2
105000     GO   TO    LEV-WRITE-GF-1-35.                                NC1264.2
105100 LEV-DELETE-GF-1-35.                                              NC1264.2
105200     PERFORM DE-LETE.                                             NC1264.2
105300 LEV-WRITE-GF-1-35.                                               NC1264.2
105400     MOVE  "LEV-TEST-GF-1-35"  TO PAR-NAME.                       NC1264.2
105500     IF       LENGTH-COUNTER GREATER THAN ZERO                    NC1264.2
105600              PERFORM BREAKDOWN-PARA THRU BREAKDOWN-EXIT.         NC1264.2
105700     PERFORM PRINT-DETAIL.                                        NC1264.2
105800 LEV-TEST-GF-1-36.                                                NC1264.2
105900     IF TBGRP-36 EQUAL TO    SPACE                                NC1264.2
106000              PERFORM PASS                                        NC1264.2
106100              GO  TO   LEV-WRITE-GF-1-36.                         NC1264.2
106200     MOVE    TABLE-GROUP           TO COMPUTED-BREAKDOWN.         NC1264.2
106300     MOVE    SPACE                 TO CORRECT-BREAKDOWN.          NC1264.2
106400     MOVE  24     TO LENGTH-COUNTER.                              NC1264.2
106500     GO   TO    LEV-WRITE-GF-1-36.                                NC1264.2
106600 LEV-DELETE-GF-1-36.                                              NC1264.2
106700     PERFORM DE-LETE.                                             NC1264.2
106800 LEV-WRITE-GF-1-36.                                               NC1264.2
106900     MOVE  "LEV-TEST-GF-1-36"  TO PAR-NAME.                       NC1264.2
107000     IF       LENGTH-COUNTER GREATER THAN ZERO                    NC1264.2
107100              PERFORM BREAKDOWN-PARA THRU BREAKDOWN-EXIT.         NC1264.2
107200     PERFORM PRINT-DETAIL.                                        NC1264.2
107300 LEV-TEST-GF-1-37.                                                NC1264.2
107400     IF TBGRP-37 EQUAL TO    SPACE                                NC1264.2
107500              PERFORM PASS                                        NC1264.2
107600              GO  TO   LEV-WRITE-GF-1-37.                         NC1264.2
107700     MOVE    TABLE-GROUP           TO COMPUTED-BREAKDOWN.         NC1264.2
107800     MOVE    SPACE                 TO CORRECT-BREAKDOWN.          NC1264.2
107900     MOVE  22     TO LENGTH-COUNTER.                              NC1264.2
108000     GO   TO    LEV-WRITE-GF-1-37.                                NC1264.2
108100 LEV-DELETE-GF-1-37.                                              NC1264.2
108200     PERFORM DE-LETE.                                             NC1264.2
108300 LEV-WRITE-GF-1-37.                                               NC1264.2
108400     MOVE  "LEV-TEST-GF-1-37"  TO PAR-NAME.                       NC1264.2
108500     IF       LENGTH-COUNTER GREATER THAN ZERO                    NC1264.2
108600              PERFORM BREAKDOWN-PARA THRU BREAKDOWN-EXIT.         NC1264.2
108700     PERFORM PRINT-DETAIL.                                        NC1264.2
108800 LEV-TEST-GF-1-38.                                                NC1264.2
108900     IF TBGRP-38 EQUAL TO    SPACE                                NC1264.2
109000              PERFORM PASS                                        NC1264.2
109100              GO  TO   LEV-WRITE-GF-1-38.                         NC1264.2
109200     MOVE    TABLE-GROUP           TO COMPUTED-BREAKDOWN.         NC1264.2
109300     MOVE    SPACE                 TO CORRECT-BREAKDOWN.          NC1264.2
109400     MOVE  20     TO LENGTH-COUNTER.                              NC1264.2
109500     GO   TO    LEV-WRITE-GF-1-38.                                NC1264.2
109600 LEV-DELETE-GF-1-38.                                              NC1264.2
109700     PERFORM DE-LETE.                                             NC1264.2
109800 LEV-WRITE-GF-1-38.                                               NC1264.2
109900     MOVE  "LEV-TEST-GF-1-38"  TO PAR-NAME.                       NC1264.2
110000     IF       LENGTH-COUNTER GREATER THAN ZERO                    NC1264.2
110100              PERFORM BREAKDOWN-PARA THRU BREAKDOWN-EXIT.         NC1264.2
110200     PERFORM PRINT-DETAIL.                                        NC1264.2
110300 LEV-TEST-GF-1-39.                                                NC1264.2
110400     IF TBGRP-39 EQUAL TO    SPACE                                NC1264.2
110500              PERFORM PASS                                        NC1264.2
110600              GO  TO   LEV-WRITE-GF-1-39.                         NC1264.2
110700     MOVE    TABLE-GROUP           TO COMPUTED-BREAKDOWN.         NC1264.2
110800     MOVE    SPACE                 TO CORRECT-BREAKDOWN.          NC1264.2
110900     MOVE  18     TO LENGTH-COUNTER.                              NC1264.2
111000     GO   TO    LEV-WRITE-GF-1-39.                                NC1264.2
111100 LEV-DELETE-GF-1-39.                                              NC1264.2
111200     PERFORM DE-LETE.                                             NC1264.2
111300 LEV-WRITE-GF-1-39.                                               NC1264.2
111400     MOVE  "LEV-TEST-GF-1-39"  TO PAR-NAME.                       NC1264.2
111500     IF       LENGTH-COUNTER GREATER THAN ZERO                    NC1264.2
111600              PERFORM BREAKDOWN-PARA THRU BREAKDOWN-EXIT.         NC1264.2
111700     PERFORM PRINT-DETAIL.                                        NC1264.2
111800 LEV-TEST-GF-1-40.                                                NC1264.2
111900     IF TBGRP-40 EQUAL TO    SPACE                                NC1264.2
112000              PERFORM PASS                                        NC1264.2
112100              GO  TO   LEV-WRITE-GF-1-40.                         NC1264.2
112200     MOVE    TABLE-GROUP           TO COMPUTED-BREAKDOWN.         NC1264.2
112300     MOVE    SPACE                 TO CORRECT-BREAKDOWN.          NC1264.2
112400     MOVE  16     TO LENGTH-COUNTER.                              NC1264.2
112500     GO   TO    LEV-WRITE-GF-1-40.                                NC1264.2
112600 LEV-DELETE-GF-1-40.                                              NC1264.2
112700     PERFORM DE-LETE.                                             NC1264.2
112800 LEV-WRITE-GF-1-40.                                               NC1264.2
112900     MOVE  "LEV-TEST-GF-1-40"  TO PAR-NAME.                       NC1264.2
113000     IF       LENGTH-COUNTER GREATER THAN ZERO                    NC1264.2
113100              PERFORM BREAKDOWN-PARA THRU BREAKDOWN-EXIT.         NC1264.2
113200     PERFORM PRINT-DETAIL.                                        NC1264.2
113300 LEV-TEST-GF-1-41.                                                NC1264.2
113400     IF TBGRP-41 EQUAL TO    SPACE                                NC1264.2
113500              PERFORM PASS                                        NC1264.2
113600              GO  TO   LEV-WRITE-GF-1-41.                         NC1264.2
113700     MOVE    TABLE-GROUP           TO COMPUTED-BREAKDOWN.         NC1264.2
113800     MOVE    SPACE                 TO CORRECT-BREAKDOWN.          NC1264.2
113900     MOVE  14     TO LENGTH-COUNTER.                              NC1264.2
114000     GO   TO    LEV-WRITE-GF-1-41.                                NC1264.2
114100 LEV-DELETE-GF-1-41.                                              NC1264.2
114200     PERFORM DE-LETE.                                             NC1264.2
114300 LEV-WRITE-GF-1-41.                                               NC1264.2
114400     MOVE  "LEV-TEST-GF-1-41"  TO PAR-NAME.                       NC1264.2
114500     IF       LENGTH-COUNTER GREATER THAN ZERO                    NC1264.2
114600              PERFORM BREAKDOWN-PARA THRU BREAKDOWN-EXIT.         NC1264.2
114700     PERFORM PRINT-DETAIL.                                        NC1264.2
114800 LEV-TEST-GF-1-42.                                                NC1264.2
114900     IF TBGRP-42 EQUAL TO    SPACE                                NC1264.2
115000              PERFORM PASS                                        NC1264.2
115100              GO  TO   LEV-WRITE-GF-1-42.                         NC1264.2
115200     MOVE    TABLE-GROUP           TO COMPUTED-BREAKDOWN.         NC1264.2
115300     MOVE    SPACE                 TO CORRECT-BREAKDOWN.          NC1264.2
115400     MOVE  12     TO LENGTH-COUNTER.                              NC1264.2
115500     GO   TO    LEV-WRITE-GF-1-42.                                NC1264.2
115600 LEV-DELETE-GF-1-42.                                              NC1264.2
115700     PERFORM DE-LETE.                                             NC1264.2
115800 LEV-WRITE-GF-1-42.                                               NC1264.2
115900     MOVE  "LEV-TEST-GF-1-42"  TO PAR-NAME.                       NC1264.2
116000     IF       LENGTH-COUNTER GREATER THAN ZERO                    NC1264.2
116100              PERFORM BREAKDOWN-PARA THRU BREAKDOWN-EXIT.         NC1264.2
116200     PERFORM PRINT-DETAIL.                                        NC1264.2
116300 LEV-TEST-GF-1-43.                                                NC1264.2
116400     IF TBGRP-43 EQUAL TO    SPACE                                NC1264.2
116500              PERFORM PASS                                        NC1264.2
116600              GO  TO   LEV-WRITE-GF-1-43.                         NC1264.2
116700     MOVE    TABLE-GROUP           TO COMPUTED-BREAKDOWN.         NC1264.2
116800     MOVE    SPACE                 TO CORRECT-BREAKDOWN.          NC1264.2
116900     MOVE  10     TO LENGTH-COUNTER.                              NC1264.2
117000     GO   TO    LEV-WRITE-GF-1-43.                                NC1264.2
117100 LEV-DELETE-GF-1-43.                                              NC1264.2
117200     PERFORM DE-LETE.                                             NC1264.2
117300 LEV-WRITE-GF-1-43.                                               NC1264.2
117400     MOVE  "LEV-TEST-GF-1-43"  TO PAR-NAME.                       NC1264.2
117500     IF       LENGTH-COUNTER GREATER THAN ZERO                    NC1264.2
117600              PERFORM BREAKDOWN-PARA THRU BREAKDOWN-EXIT.         NC1264.2
117700     PERFORM PRINT-DETAIL.                                        NC1264.2
117800 LEV-TEST-GF-1-44.                                                NC1264.2
117900     IF TBGRP-44 EQUAL TO    SPACE                                NC1264.2
118000              PERFORM PASS                                        NC1264.2
118100              GO  TO   LEV-WRITE-GF-1-44.                         NC1264.2
118200     MOVE    TABLE-GROUP           TO COMPUTED-BREAKDOWN.         NC1264.2
118300     MOVE    SPACE                 TO CORRECT-BREAKDOWN.          NC1264.2
118400     MOVE   8     TO LENGTH-COUNTER.                              NC1264.2
118500     GO   TO    LEV-WRITE-GF-1-44.                                NC1264.2
118600 LEV-DELETE-GF-1-44.                                              NC1264.2
118700     PERFORM DE-LETE.                                             NC1264.2
118800 LEV-WRITE-GF-1-44.                                               NC1264.2
118900     MOVE  "LEV-TEST-GF-1-44"  TO PAR-NAME.                       NC1264.2
119000     IF       LENGTH-COUNTER GREATER THAN ZERO                    NC1264.2
119100              PERFORM BREAKDOWN-PARA THRU BREAKDOWN-EXIT.         NC1264.2
119200     PERFORM PRINT-DETAIL.                                        NC1264.2
119300 LEV-TEST-GF-1-45.                                                NC1264.2
119400     IF TBGRP-45 EQUAL TO    SPACE                                NC1264.2
119500              PERFORM PASS                                        NC1264.2
119600              GO  TO   LEV-WRITE-GF-1-45.                         NC1264.2
119700     MOVE    TABLE-GROUP           TO COMPUTED-BREAKDOWN.         NC1264.2
119800     MOVE    SPACE                 TO CORRECT-BREAKDOWN.          NC1264.2
119900     MOVE   6     TO LENGTH-COUNTER.                              NC1264.2
120000     GO   TO    LEV-WRITE-GF-1-45.                                NC1264.2
120100 LEV-DELETE-GF-1-45.                                              NC1264.2
120200     PERFORM DE-LETE.                                             NC1264.2
120300 LEV-WRITE-GF-1-45.                                               NC1264.2
120400     MOVE  "LEV-TEST-GF-1-45"  TO PAR-NAME.                       NC1264.2
120500     IF       LENGTH-COUNTER GREATER THAN ZERO                    NC1264.2
120600              PERFORM BREAKDOWN-PARA THRU BREAKDOWN-EXIT.         NC1264.2
120700     PERFORM PRINT-DETAIL.                                        NC1264.2
120800 LEV-TEST-GF-1-46.                                                NC1264.2
120900     IF TBGRP-46 EQUAL TO    SPACE                                NC1264.2
121000              PERFORM PASS                                        NC1264.2
121100              GO  TO   LEV-WRITE-GF-1-46.                         NC1264.2
121200     MOVE    TABLE-GROUP           TO COMPUTED-BREAKDOWN.         NC1264.2
121300     MOVE    SPACE                 TO CORRECT-BREAKDOWN.          NC1264.2
121400     MOVE   4     TO LENGTH-COUNTER.                              NC1264.2
121500     GO   TO    LEV-WRITE-GF-1-46.                                NC1264.2
121600 LEV-DELETE-GF-1-46.                                              NC1264.2
121700     PERFORM DE-LETE.                                             NC1264.2
121800 LEV-WRITE-GF-1-46.                                               NC1264.2
121900     MOVE  "LEV-TEST-GF-1-46"  TO PAR-NAME.                       NC1264.2
122000     IF       LENGTH-COUNTER GREATER THAN ZERO                    NC1264.2
122100              PERFORM BREAKDOWN-PARA THRU BREAKDOWN-EXIT.         NC1264.2
122200     PERFORM PRINT-DETAIL.                                        NC1264.2
122300 LEV-TEST-GF-1-47.                                                NC1264.2
122400     IF TBGRP-47 EQUAL TO    SPACE                                NC1264.2
122500              PERFORM PASS                                        NC1264.2
122600              GO  TO   LEV-WRITE-GF-1-47.                         NC1264.2
122700     MOVE    TABLE-GROUP           TO COMPUTED-BREAKDOWN.         NC1264.2
122800     MOVE    SPACE                 TO CORRECT-BREAKDOWN.          NC1264.2
122900     MOVE   2     TO LENGTH-COUNTER.                              NC1264.2
123000     GO   TO    LEV-WRITE-GF-1-47.                                NC1264.2
123100 LEV-DELETE-GF-1-47.                                              NC1264.2
123200     PERFORM DE-LETE.                                             NC1264.2
123300 LEV-WRITE-GF-1-47.                                               NC1264.2
123400     MOVE  "LEV-TEST-GF-1-47"  TO PAR-NAME.                       NC1264.2
123500     IF       LENGTH-COUNTER GREATER THAN ZERO                    NC1264.2
123600              PERFORM BREAKDOWN-PARA THRU BREAKDOWN-EXIT.         NC1264.2
123700     PERFORM PRINT-DETAIL.                                        NC1264.2
123800 LEV-INIT-GF-2-1.                                                 NC1264.2
123900     MOVE "VI-21 5.3.3 SR1" TO ANSI-REFERENCE.                    NC1264.2
124000     PERFORM END-ROUTINE.                                         NC1264.2
124100     MOVE SPACE TO PRINT-REC.                                     NC1264.2
124200     MOVE "ALPHA MOVED TO GROUP"  TO FEATURE.                     NC1264.2
124300     PERFORM PRINT-DETAIL.                                        NC1264.2
124400     MOVE "ELEMENTRY ITEM CHK"  TO FEATURE.                       NC1264.2
124500     MOVE "ABCDEFGHIJKLMNOPQRST" TO A-PART-98.                    NC1264.2
124600     MOVE "01234567899876543210" TO B-PART-98.                    NC1264.2
124700     MOVE "SUPERCALIFRAGILISTIC" TO C-PART-98.                    NC1264.2
124800     MOVE "THAT LITERAL WAS BAD" TO D-PART-98.                    NC1264.2
124900     MOVE "UP ON THE ROOFS"      TO E-PART-98.                    NC1264.2
125000     MOVE LITERAL-98 TO TABLE-GROUP.                              NC1264.2
125100 LEV-TEST-GF-2-1.                                                 NC1264.2
125200     IF TB-1 EQUAL TO "AB"                                        NC1264.2
125300              PERFORM PASS                                        NC1264.2
125400              GO  TO   LEV-WRITE-GF-2-1.                          NC1264.2
125500     PERFORM FAIL.                                                NC1264.2
125600     MOVE TB-1 TO COMPUTED-A.                                     NC1264.2
125700     MOVE "AB" TO CORRECT-A.                                      NC1264.2
125800     GO   TO    LEV-WRITE-GF-2-1.                                 NC1264.2
125900 LEV-DELETE-GF-2-1.                                               NC1264.2
126000     PERFORM DE-LETE.                                             NC1264.2
126100 LEV-WRITE-GF-2-1.                                                NC1264.2
126200     MOVE  "LEV-TEST-GF-2-1"  TO PAR-NAME.                        NC1264.2
126300     PERFORM PRINT-DETAIL.                                        NC1264.2
126400 LEV-TEST-GF-2-2.                                                 NC1264.2
126500     IF TB-2 EQUAL TO "CD"                                        NC1264.2
126600              PERFORM PASS                                        NC1264.2
126700              GO  TO   LEV-WRITE-GF-2-2.                          NC1264.2
126800     PERFORM FAIL.                                                NC1264.2
126900     MOVE TB-2 TO COMPUTED-A.                                     NC1264.2
127000     MOVE "CD" TO  CORRECT-A.                                     NC1264.2
127100     GO   TO    LEV-WRITE-GF-2-2.                                 NC1264.2
127200 LEV-DELETE-GF-2-2.                                               NC1264.2
127300     PERFORM DE-LETE.                                             NC1264.2
127400 LEV-WRITE-GF-2-2.                                                NC1264.2
127500     MOVE  "LEV-TEST-GF-2-2"  TO PAR-NAME.                        NC1264.2
127600     PERFORM PRINT-DETAIL.                                        NC1264.2
127700 LEV-TEST-GF-2-3.                                                 NC1264.2
127800     IF TB-3 EQUAL TO "EF"                                        NC1264.2
127900              PERFORM PASS                                        NC1264.2
128000              GO  TO   LEV-WRITE-GF-2-3.                          NC1264.2
128100     PERFORM FAIL.                                                NC1264.2
128200     MOVE TB-3 TO COMPUTED-A.                                     NC1264.2
128300     MOVE "EF" TO CORRECT-A.                                      NC1264.2
128400     GO   TO    LEV-WRITE-GF-2-3.                                 NC1264.2
128500 LEV-DELETE-GF-2-3.                                               NC1264.2
128600     PERFORM DE-LETE.                                             NC1264.2
128700 LEV-WRITE-GF-2-3.                                                NC1264.2
128800     MOVE  "LEV-TEST-GF-2-3"  TO PAR-NAME.                        NC1264.2
128900     PERFORM PRINT-DETAIL.                                        NC1264.2
129000 LEV-TEST-GF-2-4.                                                 NC1264.2
129100     IF TB-4 EQUAL TO "GH"                                        NC1264.2
129200              PERFORM PASS                                        NC1264.2
129300              GO  TO   LEV-WRITE-GF-2-4.                          NC1264.2
129400     PERFORM FAIL.                                                NC1264.2
129500     MOVE TB-4 TO COMPUTED-A.                                     NC1264.2
129600     MOVE "GH" TO CORRECT-A.                                      NC1264.2
129700     GO   TO    LEV-WRITE-GF-2-4.                                 NC1264.2
129800 LEV-DELETE-GF-2-4.                                               NC1264.2
129900     PERFORM DE-LETE.                                             NC1264.2
130000 LEV-WRITE-GF-2-4.                                                NC1264.2
130100     MOVE  "LEV-TEST-GF-2-4"  TO PAR-NAME.                        NC1264.2
130200     PERFORM PRINT-DETAIL.                                        NC1264.2
130300 LEV-TEST-GF-2-5.                                                 NC1264.2
130400     IF TB-5 EQUAL TO "IJ"                                        NC1264.2
130500              PERFORM PASS                                        NC1264.2
130600              GO  TO   LEV-WRITE-GF-2-5.                          NC1264.2
130700     PERFORM FAIL.                                                NC1264.2
130800     MOVE TB-5 TO COMPUTED-A.                                     NC1264.2
130900     MOVE "IJ" TO CORRECT-A.                                      NC1264.2
131000     GO   TO    LEV-WRITE-GF-2-5.                                 NC1264.2
131100 LEV-DELETE-GF-2-5.                                               NC1264.2
131200     PERFORM DE-LETE.                                             NC1264.2
131300 LEV-WRITE-GF-2-5.                                                NC1264.2
131400     MOVE  "LEV-TEST-GF-2-5"  TO PAR-NAME.                        NC1264.2
131500     PERFORM PRINT-DETAIL.                                        NC1264.2
131600 LEV-TEST-GF-2-6.                                                 NC1264.2
131700     IF TB-6 EQUAL TO "KL"                                        NC1264.2
131800              PERFORM PASS                                        NC1264.2
131900              GO  TO   LEV-WRITE-GF-2-6.                          NC1264.2
132000     PERFORM FAIL.                                                NC1264.2
132100     MOVE TB-6 TO COMPUTED-A.                                     NC1264.2
132200     MOVE "KL" TO CORRECT-A.                                      NC1264.2
132300     GO   TO    LEV-WRITE-GF-2-6.                                 NC1264.2
132400 LEV-DELETE-GF-2-6.                                               NC1264.2
132500     PERFORM DE-LETE.                                             NC1264.2
132600 LEV-WRITE-GF-2-6.                                                NC1264.2
132700     MOVE  "LEV-TEST-GF-2-6"  TO PAR-NAME.                        NC1264.2
132800     PERFORM PRINT-DETAIL.                                        NC1264.2
132900 LEV-TEST-GF-2-7.                                                 NC1264.2
133000     IF TB-7 EQUAL TO "MN"                                        NC1264.2
133100              PERFORM PASS                                        NC1264.2
133200              GO  TO   LEV-WRITE-GF-2-7.                          NC1264.2
133300     PERFORM FAIL.                                                NC1264.2
133400     MOVE TB-7 TO COMPUTED-A.                                     NC1264.2
133500     MOVE "MN" TO CORRECT-A.                                      NC1264.2
133600     GO   TO    LEV-WRITE-GF-2-7.                                 NC1264.2
133700 LEV-DELETE-GF-2-7.                                               NC1264.2
133800     PERFORM DE-LETE.                                             NC1264.2
133900 LEV-WRITE-GF-2-7.                                                NC1264.2
134000     MOVE  "LEV-TEST-GF-2-7"  TO PAR-NAME.                        NC1264.2
134100     PERFORM PRINT-DETAIL.                                        NC1264.2
134200 LEV-TEST-GF-2-8.                                                 NC1264.2
134300     IF TB-8 EQUAL TO "OP"                                        NC1264.2
134400              PERFORM PASS                                        NC1264.2
134500              GO  TO   LEV-WRITE-GF-2-8.                          NC1264.2
134600     PERFORM FAIL.                                                NC1264.2
134700     MOVE TB-8 TO COMPUTED-A.                                     NC1264.2
134800     MOVE "OP" TO CORRECT-A.                                      NC1264.2
134900     GO   TO    LEV-WRITE-GF-2-8.                                 NC1264.2
135000 LEV-DELETE-GF-2-8.                                               NC1264.2
135100     PERFORM DE-LETE.                                             NC1264.2
135200 LEV-WRITE-GF-2-8.                                                NC1264.2
135300     MOVE  "LEV-TEST-GF-2-8"  TO PAR-NAME.                        NC1264.2
135400     PERFORM PRINT-DETAIL.                                        NC1264.2
135500 LEV-TEST-GF-2-9.                                                 NC1264.2
135600     IF TB-9 EQUAL TO "QR"                                        NC1264.2
135700              PERFORM PASS                                        NC1264.2
135800              GO  TO   LEV-WRITE-GF-2-9.                          NC1264.2
135900     PERFORM FAIL.                                                NC1264.2
136000     MOVE TB-9 TO COMPUTED-A.                                     NC1264.2
136100     MOVE "QR" TO CORRECT-A.                                      NC1264.2
136200     GO   TO    LEV-WRITE-GF-2-9.                                 NC1264.2
136300 LEV-DELETE-GF-2-9.                                               NC1264.2
136400     PERFORM DE-LETE.                                             NC1264.2
136500 LEV-WRITE-GF-2-9.                                                NC1264.2
136600     MOVE  "LEV-TEST-GF-2-9"  TO PAR-NAME.                        NC1264.2
136700     PERFORM PRINT-DETAIL.                                        NC1264.2
136800 LEV-TEST-GF-2-10.                                                NC1264.2
136900     IF TB-10 EQUAL TO "ST"                                       NC1264.2
137000              PERFORM  PASS                                       NC1264.2
137100              GO  TO   LEV-WRITE-GF-2-10.                         NC1264.2
137200     PERFORM FAIL.                                                NC1264.2
137300     MOVE TB-10 TO COMPUTED-A.                                    NC1264.2
137400     MOVE "ST" TO CORRECT-A.                                      NC1264.2
137500     GO   TO    LEV-WRITE-GF-2-10.                                NC1264.2
137600 LEV-DELETE-GF-2-10.                                              NC1264.2
137700     PERFORM DE-LETE.                                             NC1264.2
137800 LEV-WRITE-GF-2-10.                                               NC1264.2
137900     MOVE  "LEV-TEST-GF-2-10"  TO PAR-NAME.                       NC1264.2
138000     PERFORM PRINT-DETAIL.                                        NC1264.2
138100 LEV-TEST-GF-2-11.                                                NC1264.2
138200     IF TB-11 EQUAL TO "01"                                       NC1264.2
138300              PERFORM PASS                                        NC1264.2
138400              GO  TO   LEV-WRITE-GF-2-11.                         NC1264.2
138500     PERFORM FAIL.                                                NC1264.2
138600     MOVE TB-11 TO COMPUTED-A.                                    NC1264.2
138700     MOVE "01" TO CORRECT-A.                                      NC1264.2
138800     GO   TO    LEV-WRITE-GF-2-11.                                NC1264.2
138900 LEV-DELETE-GF-2-11.                                              NC1264.2
139000     PERFORM DE-LETE.                                             NC1264.2
139100 LEV-WRITE-GF-2-11.                                               NC1264.2
139200     MOVE  "LEV-TEST-GF-2-11"  TO PAR-NAME.                       NC1264.2
139300     PERFORM PRINT-DETAIL.                                        NC1264.2
139400 LEV-TEST-GF-2-12.                                                NC1264.2
139500     IF TB-12 EQUAL TO "23"                                       NC1264.2
139600              PERFORM PASS                                        NC1264.2
139700              GO  TO   LEV-WRITE-GF-2-12.                         NC1264.2
139800     PERFORM FAIL.                                                NC1264.2
139900     MOVE TB-12 TO COMPUTED-A.                                    NC1264.2
140000     MOVE "23" TO CORRECT-A.                                      NC1264.2
140100     GO   TO    LEV-WRITE-GF-2-12.                                NC1264.2
140200 LEV-DELETE-GF-2-12.                                              NC1264.2
140300     PERFORM DE-LETE.                                             NC1264.2
140400 LEV-WRITE-GF-2-12.                                               NC1264.2
140500     MOVE  "LEV-TEST-GF-2-12"  TO PAR-NAME.                       NC1264.2
140600     PERFORM PRINT-DETAIL.                                        NC1264.2
140700 LEV-TEST-GF-2-13.                                                NC1264.2
140800     IF TB-13 EQUAL TO "45"                                       NC1264.2
140900              PERFORM PASS                                        NC1264.2
141000              GO  TO   LEV-WRITE-GF-2-13.                         NC1264.2
141100     PERFORM FAIL.                                                NC1264.2
141200     MOVE TB-13 TO COMPUTED-A.                                    NC1264.2
141300     MOVE "45" TO CORRECT-A.                                      NC1264.2
141400     GO   TO    LEV-WRITE-GF-2-13.                                NC1264.2
141500 LEV-DELETE-GF-2-13.                                              NC1264.2
141600     PERFORM DE-LETE.                                             NC1264.2
141700 LEV-WRITE-GF-2-13.                                               NC1264.2
141800     MOVE  "LEV-TEST-GF-2-13"  TO PAR-NAME.                       NC1264.2
141900     PERFORM PRINT-DETAIL.                                        NC1264.2
142000 LEV-TEST-GF-2-14.                                                NC1264.2
142100     IF TB-14 EQUAL TO "67"                                       NC1264.2
142200              PERFORM PASS                                        NC1264.2
142300              GO  TO   LEV-WRITE-GF-2-14.                         NC1264.2
142400     PERFORM FAIL.                                                NC1264.2
142500     MOVE TB-14 TO COMPUTED-A.                                    NC1264.2
142600     MOVE "67" TO CORRECT-A.                                      NC1264.2
142700     GO   TO    LEV-WRITE-GF-2-14.                                NC1264.2
142800 LEV-DELETE-GF-2-14.                                              NC1264.2
142900     PERFORM DE-LETE.                                             NC1264.2
143000 LEV-WRITE-GF-2-14.                                               NC1264.2
143100     MOVE  "LEV-TEST-GF-2-14"  TO PAR-NAME.                       NC1264.2
143200     PERFORM PRINT-DETAIL.                                        NC1264.2
143300 LEV-TEST-GF-2-15.                                                NC1264.2
143400     IF TB-15 EQUAL TO "89"                                       NC1264.2
143500              PERFORM PASS                                        NC1264.2
143600              GO  TO   LEV-WRITE-GF-2-15.                         NC1264.2
143700     PERFORM FAIL.                                                NC1264.2
143800     MOVE TB-15 TO COMPUTED-A.                                    NC1264.2
143900     MOVE "89" TO CORRECT-A.                                      NC1264.2
144000     GO   TO    LEV-WRITE-GF-2-15.                                NC1264.2
144100 LEV-DELETE-GF-2-15.                                              NC1264.2
144200     PERFORM DE-LETE.                                             NC1264.2
144300 LEV-WRITE-GF-2-15.                                               NC1264.2
144400     MOVE "LEV-TEST-GF-2-15" TO PAR-NAME.                         NC1264.2
144500     PERFORM PRINT-DETAIL.                                        NC1264.2
144600 LEV-TEST-GF-2-16.                                                NC1264.2
144700     IF TB-16 EQUAL TO "98"                                       NC1264.2
144800              PERFORM PASS                                        NC1264.2
144900              GO  TO   LEV-WRITE-GF-2-16.                         NC1264.2
145000     PERFORM FAIL.                                                NC1264.2
145100     MOVE TB-16 TO COMPUTED-A.                                    NC1264.2
145200     MOVE "98" TO CORRECT-A.                                      NC1264.2
145300     GO   TO    LEV-WRITE-GF-2-16.                                NC1264.2
145400 LEV-DELETE-GF-2-16.                                              NC1264.2
145500     PERFORM DE-LETE.                                             NC1264.2
145600 LEV-WRITE-GF-2-16.                                               NC1264.2
145700     MOVE  "LEV-TEST-GF-2-16"  TO PAR-NAME.                       NC1264.2
145800     PERFORM PRINT-DETAIL.                                        NC1264.2
145900 LEV-TEST-GF-2-17.                                                NC1264.2
146000     IF TB-17 EQUAL TO "76"                                       NC1264.2
146100              PERFORM PASS                                        NC1264.2
146200              GO  TO   LEV-WRITE-GF-2-17.                         NC1264.2
146300     PERFORM FAIL.                                                NC1264.2
146400     MOVE TB-17 TO COMPUTED-A.                                    NC1264.2
146500     MOVE "76" TO CORRECT-A.                                      NC1264.2
146600     GO   TO    LEV-WRITE-GF-2-17.                                NC1264.2
146700 LEV-DELETE-GF-2-17.                                              NC1264.2
146800     PERFORM DE-LETE.                                             NC1264.2
146900 LEV-WRITE-GF-2-17.                                               NC1264.2
147000     MOVE  "LEV-TEST-GF-2-17"  TO PAR-NAME.                       NC1264.2
147100     PERFORM PRINT-DETAIL.                                        NC1264.2
147200 LEV-TEST-GF-2-18.                                                NC1264.2
147300     IF TB-18 EQUAL TO "54"                                       NC1264.2
147400              PERFORM PASS                                        NC1264.2
147500              GO  TO   LEV-WRITE-GF-2-18.                         NC1264.2
147600     PERFORM FAIL.                                                NC1264.2
147700     MOVE TB-18 TO COMPUTED-A.                                    NC1264.2
147800     MOVE "54" TO CORRECT-A.                                      NC1264.2
147900     GO   TO    LEV-WRITE-GF-2-18.                                NC1264.2
148000 LEV-DELETE-GF-2-18.                                              NC1264.2
148100     PERFORM DE-LETE.                                             NC1264.2
148200 LEV-WRITE-GF-2-18.                                               NC1264.2
148300     MOVE  "LEV-TEST-GF-2-18"  TO PAR-NAME.                       NC1264.2
148400     PERFORM PRINT-DETAIL.                                        NC1264.2
148500 LEV-TEST-GF-2-19.                                                NC1264.2
148600     IF TB-19 EQUAL TO "32"                                       NC1264.2
148700              PERFORM PASS                                        NC1264.2
148800              GO  TO   LEV-WRITE-GF-2-19.                         NC1264.2
148900     PERFORM FAIL.                                                NC1264.2
149000     MOVE TB-19 TO COMPUTED-A.                                    NC1264.2
149100     MOVE "32" TO CORRECT-A.                                      NC1264.2
149200     GO   TO    LEV-WRITE-GF-2-19.                                NC1264.2
149300 LEV-DELETE-GF-2-19.                                              NC1264.2
149400     PERFORM DE-LETE.                                             NC1264.2
149500 LEV-WRITE-GF-2-19.                                               NC1264.2
149600     MOVE  "LEV-TEST-GF-2-19"  TO PAR-NAME.                       NC1264.2
149700     PERFORM PRINT-DETAIL.                                        NC1264.2
149800 LEV-TEST-GF-2-20.                                                NC1264.2
149900     IF TB-20 EQUAL TO "10"                                       NC1264.2
150000              PERFORM PASS                                        NC1264.2
150100              GO  TO   LEV-WRITE-GF-2-20.                         NC1264.2
150200     PERFORM FAIL.                                                NC1264.2
150300     MOVE TB-20 TO COMPUTED-A.                                    NC1264.2
150400     MOVE "10" TO CORRECT-A.                                      NC1264.2
150500     GO   TO    LEV-WRITE-GF-2-20.                                NC1264.2
150600 LEV-DELETE-GF-2-20.                                              NC1264.2
150700     PERFORM DE-LETE.                                             NC1264.2
150800 LEV-WRITE-GF-2-20.                                               NC1264.2
150900     MOVE  "LEV-TEST-GF-2-20"  TO PAR-NAME.                       NC1264.2
151000     PERFORM PRINT-DETAIL.                                        NC1264.2
151100 LEV-TEST-GF-2-21.                                                NC1264.2
151200     IF TB-21 EQUAL TO "SU"                                       NC1264.2
151300              PERFORM PASS                                        NC1264.2
151400              GO  TO   LEV-WRITE-GF-2-21.                         NC1264.2
151500     PERFORM FAIL.                                                NC1264.2
151600     MOVE TB-21 TO COMPUTED-A.                                    NC1264.2
151700     MOVE "SU" TO CORRECT-A.                                      NC1264.2
151800     GO   TO    LEV-WRITE-GF-2-21.                                NC1264.2
151900 LEV-DELETE-GF-2-21.                                              NC1264.2
152000     PERFORM DE-LETE.                                             NC1264.2
152100 LEV-WRITE-GF-2-21.                                               NC1264.2
152200     MOVE  "LEV-TEST-GF-2-21"  TO PAR-NAME.                       NC1264.2
152300     PERFORM PRINT-DETAIL.                                        NC1264.2
152400 LEV-TEST-GF-2-22.                                                NC1264.2
152500     IF TB-22 EQUAL TO "PE"                                       NC1264.2
152600              PERFORM PASS                                        NC1264.2
152700              GO  TO   LEV-WRITE-GF-2-22.                         NC1264.2
152800     PERFORM FAIL.                                                NC1264.2
152900     MOVE TB-22 TO COMPUTED-A.                                    NC1264.2
153000     MOVE "PE" TO CORRECT-A.                                      NC1264.2
153100     GO   TO    LEV-WRITE-GF-2-22.                                NC1264.2
153200 LEV-DELETE-GF-2-22.                                              NC1264.2
153300     PERFORM DE-LETE.                                             NC1264.2
153400 LEV-WRITE-GF-2-22.                                               NC1264.2
153500     MOVE  "LEV-TEST-GF-2-22"  TO PAR-NAME.                       NC1264.2
153600     PERFORM PRINT-DETAIL.                                        NC1264.2
153700 LEV-TEST-GF-2-23.                                                NC1264.2
153800     IF TB-23 EQUAL TO "RC"                                       NC1264.2
153900              PERFORM PASS                                        NC1264.2
154000              GO  TO   LEV-WRITE-GF-2-23.                         NC1264.2
154100     PERFORM FAIL.                                                NC1264.2
154200     MOVE TB-23 TO COMPUTED-A.                                    NC1264.2
154300     MOVE "RC" TO CORRECT-A.                                      NC1264.2
154400     GO   TO    LEV-WRITE-GF-2-23.                                NC1264.2
154500 LEV-DELETE-GF-2-23.                                              NC1264.2
154600     PERFORM DE-LETE.                                             NC1264.2
154700 LEV-WRITE-GF-2-23.                                               NC1264.2
154800     MOVE  "LEV-TEST-GF-2-23"  TO PAR-NAME.                       NC1264.2
154900     PERFORM PRINT-DETAIL.                                        NC1264.2
155000 LEV-TEST-GF-2-24.                                                NC1264.2
155100     IF TB-24 EQUAL TO "AL"                                       NC1264.2
155200              PERFORM PASS                                        NC1264.2
155300              GO  TO   LEV-WRITE-GF-2-24.                         NC1264.2
155400     PERFORM FAIL.                                                NC1264.2
155500     MOVE TB-24 TO COMPUTED-A.                                    NC1264.2
155600     MOVE "AL" TO CORRECT-A.                                      NC1264.2
155700     GO   TO    LEV-WRITE-GF-2-24.                                NC1264.2
155800 LEV-DELETE-GF-2-24.                                              NC1264.2
155900     PERFORM DE-LETE.                                             NC1264.2
156000 LEV-WRITE-GF-2-24.                                               NC1264.2
156100     MOVE  "LEV-TEST-GF-2-24"  TO PAR-NAME.                       NC1264.2
156200     PERFORM PRINT-DETAIL.                                        NC1264.2
156300 LEV-TEST-GF-2-25.                                                NC1264.2
156400     IF TB-25 EQUAL TO "IF"                                       NC1264.2
156500              PERFORM PASS                                        NC1264.2
156600              GO  TO   LEV-WRITE-GF-2-25.                         NC1264.2
156700     PERFORM FAIL.                                                NC1264.2
156800     MOVE TB-25 TO COMPUTED-A.                                    NC1264.2
156900     MOVE "IF" TO CORRECT-A.                                      NC1264.2
157000     GO   TO    LEV-WRITE-GF-2-25.                                NC1264.2
157100 LEV-DELETE-GF-2-25.                                              NC1264.2
157200     PERFORM DE-LETE.                                             NC1264.2
157300 LEV-WRITE-GF-2-25.                                               NC1264.2
157400     MOVE  "LEV-TEST-GF-2-25"  TO PAR-NAME.                       NC1264.2
157500     PERFORM PRINT-DETAIL.                                        NC1264.2
157600 LEV-TEST-GF-2-26.                                                NC1264.2
157700     IF TB-26 EQUAL TO "RA"                                       NC1264.2
157800              PERFORM PASS                                        NC1264.2
157900              GO  TO   LEV-WRITE-GF-2-26.                         NC1264.2
158000     PERFORM FAIL.                                                NC1264.2
158100     MOVE TB-26 TO COMPUTED-A.                                    NC1264.2
158200     MOVE "RA" TO CORRECT-A.                                      NC1264.2
158300     GO   TO    LEV-WRITE-GF-2-26.                                NC1264.2
158400 LEV-DELETE-GF-2-26.                                              NC1264.2
158500     PERFORM DE-LETE.                                             NC1264.2
158600 LEV-WRITE-GF-2-26.                                               NC1264.2
158700     MOVE  "LEV-TEST-GF-2-26"  TO PAR-NAME.                       NC1264.2
158800     PERFORM PRINT-DETAIL.                                        NC1264.2
158900 LEV-TEST-GF-2-27.                                                NC1264.2
159000     IF TB-27 EQUAL TO "GI"                                       NC1264.2
159100              PERFORM PASS                                        NC1264.2
159200              GO  TO   LEV-WRITE-GF-2-27.                         NC1264.2
159300     PERFORM FAIL.                                                NC1264.2
159400     MOVE TB-27 TO COMPUTED-A.                                    NC1264.2
159500     MOVE "GI" TO CORRECT-A.                                      NC1264.2
159600     GO   TO    LEV-WRITE-GF-2-27.                                NC1264.2
159700 LEV-DELETE-GF-2-27.                                              NC1264.2
159800     PERFORM DE-LETE.                                             NC1264.2
159900 LEV-WRITE-GF-2-27.                                               NC1264.2
160000     MOVE  "LEV-TEST-GF-2-27"  TO PAR-NAME.                       NC1264.2
160100     PERFORM PRINT-DETAIL.                                        NC1264.2
160200 LEV-TEST-GF-2-28.                                                NC1264.2
160300     IF TB-28 EQUAL TO "LI"                                       NC1264.2
160400             PERFORM PASS                                         NC1264.2
160500              GO  TO   LEV-WRITE-GF-2-28.                         NC1264.2
160600     PERFORM FAIL.                                                NC1264.2
160700     MOVE TB-29 TO COMPUTED-A.                                    NC1264.2
160800     MOVE "LI" TO CORRECT-A.                                      NC1264.2
160900     GO   TO    LEV-WRITE-GF-2-28.                                NC1264.2
161000 LEV-DELETE-GF-2-28.                                              NC1264.2
161100     PERFORM DE-LETE.                                             NC1264.2
161200 LEV-WRITE-GF-2-28.                                               NC1264.2
161300     MOVE  "LEV-TEST-GF-2-28"  TO PAR-NAME.                       NC1264.2
161400     PERFORM PRINT-DETAIL.                                        NC1264.2
161500 LEV-TEST-GF-2-29.                                                NC1264.2
161600     IF TB-29 EQUAL TO "ST"                                       NC1264.2
161700              PERFORM PASS                                        NC1264.2
161800              GO  TO   LEV-WRITE-GF-2-29.                         NC1264.2
161900     PERFORM FAIL.                                                NC1264.2
162000     MOVE TB-29 TO COMPUTED-A.                                    NC1264.2
162100     MOVE "ST" TO CORRECT-A.                                      NC1264.2
162200     GO   TO    LEV-WRITE-GF-2-29.                                NC1264.2
162300 LEV-DELETE-GF-2-29.                                              NC1264.2
162400     PERFORM DE-LETE.                                             NC1264.2
162500 LEV-WRITE-GF-2-29.                                               NC1264.2
162600     MOVE  "LEV-TEST-GF-2-29"  TO PAR-NAME.                       NC1264.2
162700     PERFORM PRINT-DETAIL.                                        NC1264.2
162800 LEV-TEST-GF-2-30.                                                NC1264.2
162900     IF TB-30 EQUAL TO "IC"                                       NC1264.2
163000              PERFORM PASS                                        NC1264.2
163100              GO  TO   LEV-WRITE-GF-2-30.                         NC1264.2
163200     PERFORM FAIL.                                                NC1264.2
163300     MOVE TB-30 TO COMPUTED-A.                                    NC1264.2
163400     MOVE "IC" TO CORRECT-A.                                      NC1264.2
163500     GO   TO    LEV-WRITE-GF-2-30.                                NC1264.2
163600 LEV-DELETE-GF-2-30.                                              NC1264.2
163700     PERFORM DE-LETE.                                             NC1264.2
163800 LEV-WRITE-GF-2-30.                                               NC1264.2
163900     MOVE  "LEV-TEST-GF-2-30"  TO PAR-NAME.                       NC1264.2
164000     PERFORM PRINT-DETAIL.                                        NC1264.2
164100 LEV-TEST-GF-2-31.                                                NC1264.2
164200     IF TB-31 EQUAL TO "TH"                                       NC1264.2
164300              PERFORM PASS                                        NC1264.2
164400              GO  TO   LEV-WRITE-GF-2-31.                         NC1264.2
164500     PERFORM FAIL.                                                NC1264.2
164600     MOVE TB-31 TO COMPUTED-A.                                    NC1264.2
164700     MOVE "TH" TO CORRECT-A.                                      NC1264.2
164800     GO   TO    LEV-WRITE-GF-2-31.                                NC1264.2
164900 LEV-DELETE-GF-2-31.                                              NC1264.2
165000     PERFORM DE-LETE.                                             NC1264.2
165100 LEV-WRITE-GF-2-31.                                               NC1264.2
165200     MOVE  "LEV-TEST-GF-2-31"  TO PAR-NAME.                       NC1264.2
165300     PERFORM PRINT-DETAIL.                                        NC1264.2
165400 LEV-TEST-GF-2-32.                                                NC1264.2
165500     IF TB-32 EQUAL TO "AT"                                       NC1264.2
165600              PERFORM PASS                                        NC1264.2
165700              GO  TO   LEV-WRITE-GF-2-32.                         NC1264.2
165800     PERFORM FAIL.                                                NC1264.2
165900     MOVE TB-32 TO COMPUTED-A.                                    NC1264.2
166000     MOVE "AT" TO CORRECT-A.                                      NC1264.2
166100     GO   TO    LEV-WRITE-GF-2-32.                                NC1264.2
166200 LEV-DELETE-GF-2-32.                                              NC1264.2
166300     PERFORM DE-LETE.                                             NC1264.2
166400 LEV-WRITE-GF-2-32.                                               NC1264.2
166500     MOVE  "LEV-TEST-GF-2-32"  TO PAR-NAME.                       NC1264.2
166600     PERFORM PRINT-DETAIL.                                        NC1264.2
166700 LEV-TEST-GF-2-33.                                                NC1264.2
166800     IF TB-33 EQUAL TO " L"                                       NC1264.2
166900              PERFORM PASS                                        NC1264.2
167000              GO  TO   LEV-WRITE-GF-2-33.                         NC1264.2
167100     PERFORM FAIL.                                                NC1264.2
167200     MOVE TB-33 TO COMPUTED-A.                                    NC1264.2
167300     MOVE " L" TO CORRECT-A.                                      NC1264.2
167400     GO   TO    LEV-WRITE-GF-2-33.                                NC1264.2
167500 LEV-DELETE-GF-2-33.                                              NC1264.2
167600     PERFORM DE-LETE.                                             NC1264.2
167700 LEV-WRITE-GF-2-33.                                               NC1264.2
167800     MOVE  "LEV-TEST-GF-2-33"  TO PAR-NAME.                       NC1264.2
167900     PERFORM PRINT-DETAIL.                                        NC1264.2
168000 LEV-TEST-GF-2-34.                                                NC1264.2
168100     IF TB-34 EQUAL TO "IT"                                       NC1264.2
168200              PERFORM PASS                                        NC1264.2
168300              GO  TO   LEV-WRITE-GF-2-34.                         NC1264.2
168400     PERFORM FAIL.                                                NC1264.2
168500     MOVE TB-34 TO COMPUTED-A.                                    NC1264.2
168600     MOVE "IT" TO CORRECT-A.                                      NC1264.2
168700     GO   TO    LEV-WRITE-GF-2-34.                                NC1264.2
168800 LEV-DELETE-GF-2-34.                                              NC1264.2
168900     PERFORM DE-LETE.                                             NC1264.2
169000 LEV-WRITE-GF-2-34.                                               NC1264.2
169100     MOVE  "LEV-TEST-GF-2-34"  TO PAR-NAME.                       NC1264.2
169200     PERFORM PRINT-DETAIL.                                        NC1264.2
169300 LEV-TEST-GF-2-35.                                                NC1264.2
169400     IF TB-35 EQUAL TO "ER"                                       NC1264.2
169500              PERFORM PASS                                        NC1264.2
169600              GO  TO   LEV-WRITE-GF-2-35.                         NC1264.2
169700     PERFORM FAIL.                                                NC1264.2
169800     MOVE TB-35 TO COMPUTED-A.                                    NC1264.2
169900     MOVE "ER" TO CORRECT-A.                                      NC1264.2
170000     GO   TO    LEV-WRITE-GF-2-35.                                NC1264.2
170100 LEV-DELETE-GF-2-35.                                              NC1264.2
170200     PERFORM DE-LETE.                                             NC1264.2
170300 LEV-WRITE-GF-2-35.                                               NC1264.2
170400     MOVE  "LEV-TEST-GF-2-35"  TO PAR-NAME.                       NC1264.2
170500     PERFORM PRINT-DETAIL.                                        NC1264.2
170600 LEV-TEST-GF-2-36.                                                NC1264.2
170700     IF TB-36 EQUAL TO "AL"                                       NC1264.2
170800              PERFORM PASS                                        NC1264.2
170900              GO  TO   LEV-WRITE-GF-2-36.                         NC1264.2
171000     PERFORM FAIL.                                                NC1264.2
171100     MOVE TB-36 TO COMPUTED-A.                                    NC1264.2
171200     MOVE "AL" TO CORRECT-A.                                      NC1264.2
171300     GO   TO    LEV-WRITE-GF-2-36.                                NC1264.2
171400 LEV-DELETE-GF-2-36.                                              NC1264.2
171500     PERFORM DE-LETE.                                             NC1264.2
171600 LEV-WRITE-GF-2-36.                                               NC1264.2
171700     MOVE  "LEV-TEST-GF-2-36"  TO PAR-NAME.                       NC1264.2
171800     PERFORM PRINT-DETAIL.                                        NC1264.2
171900 LEV-TEST-GF-2-37.                                                NC1264.2
172000     IF TB-37 EQUAL TO " W"                                       NC1264.2
172100              PERFORM PASS                                        NC1264.2
172200              GO  TO   LEV-WRITE-GF-2-37.                         NC1264.2
172300     PERFORM FAIL.                                                NC1264.2
172400     MOVE TB-37 TO COMPUTED-A.                                    NC1264.2
172500     MOVE " W" TO CORRECT-A.                                      NC1264.2
172600     GO   TO    LEV-WRITE-GF-2-37.                                NC1264.2
172700 LEV-DELETE-GF-2-37.                                              NC1264.2
172800     PERFORM DE-LETE.                                             NC1264.2
172900 LEV-WRITE-GF-2-37.                                               NC1264.2
173000     MOVE  "LEV-TEST-GF-2-37"  TO PAR-NAME.                       NC1264.2
173100     PERFORM PRINT-DETAIL.                                        NC1264.2
173200 LEV-TEST-GF-2-38.                                                NC1264.2
173300     IF TB-38 EQUAL TO "AS"                                       NC1264.2
173400              PERFORM PASS                                        NC1264.2
173500              GO  TO   LEV-WRITE-GF-2-38.                         NC1264.2
173600     PERFORM FAIL.                                                NC1264.2
173700     MOVE TB-38 TO COMPUTED-A.                                    NC1264.2
173800     MOVE "AS" TO CORRECT-A.                                      NC1264.2
173900     GO   TO    LEV-WRITE-GF-2-38.                                NC1264.2
174000 LEV-DELETE-GF-2-38.                                              NC1264.2
174100     PERFORM DE-LETE.                                             NC1264.2
174200 LEV-WRITE-GF-2-38.                                               NC1264.2
174300     MOVE  "LEV-TEST-GF-2-38"  TO PAR-NAME.                       NC1264.2
174400     PERFORM PRINT-DETAIL.                                        NC1264.2
174500 LEV-TEST-GF-2-39.                                                NC1264.2
174600     IF TB-39 EQUAL TO " B"                                       NC1264.2
174700              PERFORM PASS                                        NC1264.2
174800              GO  TO   LEV-WRITE-GF-2-39.                         NC1264.2
174900     PERFORM FAIL.                                                NC1264.2
175000     MOVE TB-39 TO COMPUTED-A.                                    NC1264.2
175100     MOVE " B" TO CORRECT-A.                                      NC1264.2
175200     GO   TO    LEV-WRITE-GF-2-39.                                NC1264.2
175300 LEV-DELETE-GF-2-39.                                              NC1264.2
175400     PERFORM DE-LETE.                                             NC1264.2
175500 LEV-WRITE-GF-2-39.                                               NC1264.2
175600     MOVE  "LEV-TEST-GF-2-39"  TO PAR-NAME.                       NC1264.2
175700     PERFORM PRINT-DETAIL.                                        NC1264.2
175800 LEV-TEST-GF-2-40.                                                NC1264.2
175900     IF TB-40 EQUAL TO "AD"                                       NC1264.2
176000              PERFORM PASS                                        NC1264.2
176100              GO  TO   LEV-WRITE-GF-2-40.                         NC1264.2
176200     PERFORM FAIL.                                                NC1264.2
176300     MOVE TB-40 TO COMPUTED-A.                                    NC1264.2
176400     MOVE "AD" TO CORRECT-A.                                      NC1264.2
176500     GO   TO    LEV-WRITE-GF-2-40.                                NC1264.2
176600 LEV-DELETE-GF-2-40.                                              NC1264.2
176700     PERFORM DE-LETE.                                             NC1264.2
176800 LEV-WRITE-GF-2-40.                                               NC1264.2
176900     MOVE  "LEV-TEST-GF-2-40"  TO PAR-NAME.                       NC1264.2
177000     PERFORM PRINT-DETAIL.                                        NC1264.2
177100 LEV-TEST-GF-2-41.                                                NC1264.2
177200     IF TB-41 EQUAL TO "UP"                                       NC1264.2
177300              PERFORM PASS                                        NC1264.2
177400              GO  TO   LEV-WRITE-GF-2-41.                         NC1264.2
177500     PERFORM FAIL.                                                NC1264.2
177600     MOVE TB-41 TO COMPUTED-A.                                    NC1264.2
177700     MOVE "UP" TO CORRECT-A.                                      NC1264.2
177800     GO   TO    LEV-WRITE-GF-2-41.                                NC1264.2
177900 LEV-DELETE-GF-2-41.                                              NC1264.2
178000     PERFORM DE-LETE.                                             NC1264.2
178100 LEV-WRITE-GF-2-41.                                               NC1264.2
178200     MOVE  "LEV-TEST-GF-2-41"  TO PAR-NAME.                       NC1264.2
178300     PERFORM PRINT-DETAIL.                                        NC1264.2
178400 LEV-TEST-GF-2-42.                                                NC1264.2
178500     IF TB-42  EQUAL TO " O"                                      NC1264.2
178600              PERFORM PASS                                        NC1264.2
178700              GO  TO   LEV-WRITE-GF-2-42.                         NC1264.2
178800     PERFORM FAIL.                                                NC1264.2
178900     MOVE TB-42 TO COMPUTED-A.                                    NC1264.2
179000     MOVE " O" TO CORRECT-A.                                      NC1264.2
179100     GO   TO    LEV-WRITE-GF-2-42.                                NC1264.2
179200 LEV-DELETE-GF-2-42.                                              NC1264.2
179300     PERFORM DE-LETE.                                             NC1264.2
179400 LEV-WRITE-GF-2-42.                                               NC1264.2
179500     MOVE  "LEV-TEST-GF-2-42"  TO PAR-NAME.                       NC1264.2
179600     PERFORM PRINT-DETAIL.                                        NC1264.2
179700 LEV-TEST-GF-2-43.                                                NC1264.2
179800     IF TB-43 EQUAL TO "N "                                       NC1264.2
179900              PERFORM PASS                                        NC1264.2
180000              GO  TO   LEV-WRITE-GF-2-43.                         NC1264.2
180100     PERFORM FAIL.                                                NC1264.2
180200     MOVE TB-43 TO COMPUTED-A.                                    NC1264.2
180300     MOVE "N " TO CORRECT-A.                                      NC1264.2
180400     GO   TO    LEV-WRITE-GF-2-43.                                NC1264.2
180500 LEV-DELETE-GF-2-43.                                              NC1264.2
180600     PERFORM DE-LETE.                                             NC1264.2
180700 LEV-WRITE-GF-2-43.                                               NC1264.2
180800     MOVE  "LEV-TEST-GF-2-43"  TO PAR-NAME.                       NC1264.2
180900     PERFORM PRINT-DETAIL.                                        NC1264.2
181000 LEV-TEST-GF-2-44.                                                NC1264.2
181100     IF TB-44 EQUAL TO "TH"                                       NC1264.2
181200              PERFORM PASS                                        NC1264.2
181300              GO  TO   LEV-WRITE-GF-2-44.                         NC1264.2
181400     PERFORM FAIL.                                                NC1264.2
181500     MOVE TB-44 TO COMPUTED-A.                                    NC1264.2
181600     MOVE "TH" TO CORRECT-A.                                      NC1264.2
181700     GO   TO    LEV-WRITE-GF-2-44.                                NC1264.2
181800 LEV-DELETE-GF-2-44.                                              NC1264.2
181900     PERFORM DE-LETE.                                             NC1264.2
182000 LEV-WRITE-GF-2-44.                                               NC1264.2
182100     MOVE  "LEV-TEST-GF-2-44"  TO PAR-NAME.                       NC1264.2
182200     PERFORM PRINT-DETAIL.                                        NC1264.2
182300 LEV-TEST-GF-2-45.                                                NC1264.2
182400     IF TB-45 EQUAL TO "E "                                       NC1264.2
182500              PERFORM PASS                                        NC1264.2
182600              GO  TO   LEV-WRITE-GF-2-45.                         NC1264.2
182700     PERFORM FAIL.                                                NC1264.2
182800     MOVE TB-45 TO COMPUTED-A.                                    NC1264.2
182900     MOVE "E " TO CORRECT-A.                                      NC1264.2
183000     GO   TO    LEV-WRITE-GF-2-45.                                NC1264.2
183100 LEV-DELETE-GF-2-45.                                              NC1264.2
183200     PERFORM DE-LETE.                                             NC1264.2
183300 LEV-WRITE-GF-2-45.                                               NC1264.2
183400     MOVE  "LEV-TEST-GF-2-45"  TO PAR-NAME.                       NC1264.2
183500     PERFORM PRINT-DETAIL.                                        NC1264.2
183600 LEV-TEST-GF-2-46.                                                NC1264.2
183700     IF TB-46 EQUAL TO "RO"                                       NC1264.2
183800              PERFORM PASS                                        NC1264.2
183900              GO  TO   LEV-WRITE-GF-2-46.                         NC1264.2
184000     PERFORM FAIL.                                                NC1264.2
184100     MOVE TB-46 TO COMPUTED-A.                                    NC1264.2
184200     MOVE "RO" TO CORRECT-A.                                      NC1264.2
184300     GO   TO    LEV-WRITE-GF-2-46.                                NC1264.2
184400 LEV-DELETE-GF-2-46.                                              NC1264.2
184500     PERFORM DE-LETE.                                             NC1264.2
184600 LEV-WRITE-GF-2-46.                                               NC1264.2
184700     MOVE  "LEV-TEST-GF-2-46"  TO PAR-NAME.                       NC1264.2
184800     PERFORM PRINT-DETAIL.                                        NC1264.2
184900 LEV-TEST-GF-2-47.                                                NC1264.2
185000     IF TB-47 EQUAL TO "OF"                                       NC1264.2
185100              PERFORM PASS                                        NC1264.2
185200              GO TO LEV-WRITE-GF-2-47.                            NC1264.2
185300     PERFORM FAIL.                                                NC1264.2
185400     MOVE TB-47  TO COMPUTED-A.                                   NC1264.2
185500     MOVE "OF" TO CORRECT-A.                                      NC1264.2
185600     GO   TO    LEV-WRITE-GF-2-47.                                NC1264.2
185700 LEV-DELETE-GF-2-47.                                              NC1264.2
185800     PERFORM DE-LETE.                                             NC1264.2
185900 LEV-WRITE-GF-2-47.                                               NC1264.2
186000     MOVE  "LEV-TEST-GF-2-47"  TO PAR-NAME.                       NC1264.2
186100     PERFORM PRINT-DETAIL.                                        NC1264.2
186200 LEV-TEST-GF-2-48.                                                NC1264.2
186300     IF TB-48 EQUAL TO "S "                                       NC1264.2
186400              PERFORM PASS                                        NC1264.2
186500              GO  TO   LEV-WRITE-GF-2-48.                         NC1264.2
186600     PERFORM FAIL.                                                NC1264.2
186700     MOVE TB-48 TO COMPUTED-A.                                    NC1264.2
186800     MOVE "S " TO CORRECT-A.                                      NC1264.2
186900     GO   TO    LEV-WRITE-GF-2-48.                                NC1264.2
187000 LEV-DELETE-GF-2-48.                                              NC1264.2
187100     PERFORM DE-LETE.                                             NC1264.2
187200 LEV-WRITE-GF-2-48.                                               NC1264.2
187300     MOVE  "LEV-TEST-GF-2-48"  TO PAR-NAME.                       NC1264.2
187400     PERFORM PRINT-DETAIL.                                        NC1264.2
187500 LEV-INIT-GF-3-1.                                                 NC1264.2
187600     MOVE "VI-21 5.3.3 SR1" TO ANSI-REFERENCE.                    NC1264.2
187700     PERFORM END-ROUTINE.                                         NC1264.2
187800     MOVE SPACE TO PRINT-REC.                                     NC1264.2
187900     MOVE "NUMERIC MOVED TO GRP"     TO FEATURE.                  NC1264.2
188000     PERFORM PRINT-DETAIL.                                        NC1264.2
188100     MOVE "ELEMENTRY ITEM CHK"  TO FEATURE.                       NC1264.2
188200     MOVE  000046                 TO GP-1.                        NC1264.2
188300     MOVE  12345678902            TO GP-2.                        NC1264.2
188400     MOVE  121619492              TO GP-3.                        NC1264.2
188500     MOVE  0109                   TO GP-4.                        NC1264.2
188600     MOVE  6645143                TO GP-5.                        NC1264.2
188700     MOVE  000096                 TO GP-6.                        NC1264.2
188800     MOVE  -4361                  TO GP-7.                        NC1264.2
188900     MOVE  "PROGRAM DIVISI"       TO GP-8.                        NC1264.2
189000     MOVE  "NPLD"                 TO GP-9.                        NC1264.2
189100     MOVE  770                    TO GP-10.                       NC1264.2
189200     MOVE  5604                   TO GP-11.                       NC1264.2
189300     MOVE  ZERO                   TO GP-12.                       NC1264.2
189400     MOVE  0004                   TO GP-13.                       NC1264.2
189500     MOVE  "KLOP"                 TO GP-14.                       NC1264.2
189600     MOVE  12345678902           TO GP-15.                        NC1264.2
189700     MOVE  ZERO                   TO GP-16.                       NC1264.2
189800     MOVE  1972                   TO GP-17.                       NC1264.2
189900     MOVE  -0042                  TO GP-18.                       NC1264.2
190000     MOVE  ZERO                   TO GP-19.                       NC1264.2
190100     MOVE  492                    TO GP-20.                       NC1264.2
190200     MOVE  SPACE                  TO GP-21.                       NC1264.2
190300     MOVE  040290                 TO GP-22.                       NC1264.2
190400     MOVE "9A8B7C"                TO GP-23.                       NC1264.2
190500     MOVE        040290           TO GP-24.                       NC1264.2
190600     MOVE  289                    TO GP-25.                       NC1264.2
190700     MOVE  2251                   TO GP-26.                       NC1264.2
190800     MOVE  1692                   TO GP-27.                       NC1264.2
190900     MOVE  00000041               TO GP-28.                       NC1264.2
191000     MOVE  00001                  TO GP-29.                       NC1264.2
191100     MOVE              ZERO       TO GP-30.                       NC1264.2
191200     MOVE  ZERO            TO GP-31.                              NC1264.2
191300     MOVE  000                    TO GP-32.                       NC1264.2
191400     MOVE  ZERO                   TO GP-33.                       NC1264.2
191500     MOVE  21                     TO GP-34.                       NC1264.2
191600     MOVE  36                     TO GP-35.                       NC1264.2
191700     MOVE  918                    TO GP-36.                       NC1264.2
191800     MOVE  ZERO TO         GP-37.                                 NC1264.2
191900     MOVE  -36                    TO GP-38.                       NC1264.2
192000     MOVE  24                     TO GP-39.                       NC1264.2
192100     MOVE  36                     TO GP-40.                       NC1264.2
192200     MOVE  -1                     TO GP-41.                       NC1264.2
192300     MOVE  ZERO                   TO GP-42.                       NC1264.2
192400     MOVE  "AIR"                  TO GP-43.                       NC1264.2
192500     MOVE "9ZX"                   TO GP-44.                       NC1264.2
192600     MOVE  01000                  TO GP-45.                       NC1264.2
192700     MOVE  93                     TO GP-46.                       NC1264.2
192800     MOVE   5      TO GP-47 (1).                                  NC1264.2
192900     MOVE    ZERO TO   GP-47 (2).                                 NC1264.2
193000     MOVE  "Y"   TO GP-48 (1).                                    NC1264.2
193100     MOVE   SPACE TO   GP-48 (2).                                 NC1264.2
193200 LEV-TEST-GF-3-1.                                                 NC1264.2
193300     IF GP-1 EQUAL TO       "   046"                              NC1264.2
193400              PERFORM PASS                                        NC1264.2
193500              GO  TO   LEV-WRITE-GF-3-1.                          NC1264.2
193600     PERFORM FAIL.                                                NC1264.2
193700     MOVE GP-1 TO COMPUTED-A.                                     NC1264.2
193800     MOVE  "   046"               TO CORRECT-A.                   NC1264.2
193900     GO   TO    LEV-WRITE-GF-3-1.                                 NC1264.2
194000 LEV-DELETE-GF-3-1.                                               NC1264.2
194100     PERFORM DE-LETE.                                             NC1264.2
194200 LEV-WRITE-GF-3-1.                                                NC1264.2
194300     MOVE  "LEV-TEST-GF-3-1"  TO PAR-NAME.                        NC1264.2
194400     PERFORM PRINT-DETAIL.                                        NC1264.2
194500 LEV-TEST-GF-3-2.                                                 NC1264.2
194600     IF GP-2 EQUAL TO "345678902.00"                              NC1264.2
194700              PERFORM PASS                                        NC1264.2
194800              GO  TO   LEV-WRITE-GF-3-2.                          NC1264.2
194900     PERFORM FAIL.                                                NC1264.2
195000     MOVE GP-2 TO COMPUTED-A                                      NC1264.2
195100     MOVE  "345678902.00"         TO CORRECT-A.                   NC1264.2
195200     GO   TO    LEV-WRITE-GF-3-2.                                 NC1264.2
195300 LEV-DELETE-GF-3-2.                                               NC1264.2
195400     PERFORM DE-LETE.                                             NC1264.2
195500 LEV-WRITE-GF-3-2.                                                NC1264.2
195600     MOVE  "LEV-TEST-GF-3-2"  TO PAR-NAME.                        NC1264.2
195700     PERFORM PRINT-DETAIL.                                        NC1264.2
195800 LEV-TEST-GF-3-3.                                                 NC1264.2
195900     IF GP-3 EQUAL TO       "000121619492"                        NC1264.2
196000              PERFORM PASS                                        NC1264.2
196100              GO  TO   LEV-WRITE-GF-3-3.                          NC1264.2
196200     PERFORM FAIL.                                                NC1264.2
196300     MOVE GP-3 TO COMPUTED-A                                      NC1264.2
196400     MOVE  "000121619492"         TO CORRECT-A.                   NC1264.2
196500     GO   TO    LEV-WRITE-GF-3-3.                                 NC1264.2
196600 LEV-DELETE-GF-3-3.                                               NC1264.2
196700     PERFORM DE-LETE.                                             NC1264.2
196800 LEV-WRITE-GF-3-3.                                                NC1264.2
196900     MOVE  "LEV-TEST-GF-3-3"  TO PAR-NAME.                        NC1264.2
197000     PERFORM PRINT-DETAIL.                                        NC1264.2
197100 LEV-TEST-GF-3-4.                                                 NC1264.2
197200     IF GP-4 EQUAL TO       "  1 09"                              NC1264.2
197300              PERFORM PASS                                        NC1264.2
197400              GO  TO   LEV-WRITE-GF-3-4.                          NC1264.2
197500     PERFORM FAIL.                                                NC1264.2
197600     MOVE GP-4 TO COMPUTED-A                                      NC1264.2
197700     MOVE  "  1 09"               TO CORRECT-A.                   NC1264.2
197800     GO   TO    LEV-WRITE-GF-3-4.                                 NC1264.2
197900 LEV-DELETE-GF-3-4.                                               NC1264.2
198000     PERFORM DE-LETE.                                             NC1264.2
198100 LEV-WRITE-GF-3-4.                                                NC1264.2
198200     MOVE  "LEV-TEST-GF-3-4"  TO PAR-NAME.                        NC1264.2
198300     PERFORM PRINT-DETAIL.                                        NC1264.2
198400 LEV-TEST-GF-3-5.                                                 NC1264.2
198500     IF GP-5 EQUAL TO       "$45,143.00"                          NC1264.2
198600              PERFORM PASS                                        NC1264.2
198700              GO  TO   LEV-WRITE-GF-3-5.                          NC1264.2
198800     PERFORM FAIL.                                                NC1264.2
198900     MOVE GP-5 TO COMPUTED-A                                      NC1264.2
199000     MOVE  "$45,143.00"           TO CORRECT-A.                   NC1264.2
199100     GO   TO    LEV-WRITE-GF-3-5.                                 NC1264.2
199200 LEV-DELETE-GF-3-5.                                               NC1264.2
199300     PERFORM DE-LETE.                                             NC1264.2
199400 LEV-WRITE-GF-3-5.                                                NC1264.2
199500     MOVE  "LEV-TEST-GF-3-5" TO PAR-NAME.                         NC1264.2
199600     PERFORM PRINT-DETAIL.                                        NC1264.2
199700 LEV-TEST-GF-3-6.                                                 NC1264.2
199800     IF GP-6 EQUAL TO       "******96"                            NC1264.2
199900              PERFORM PASS                                        NC1264.2
200000              GO  TO   LEV-WRITE-GF-3-6.                          NC1264.2
200100     PERFORM FAIL.                                                NC1264.2
200200     MOVE GP-6 TO COMPUTED-A                                      NC1264.2
200300     MOVE  "******96"             TO CORRECT-A.                   NC1264.2
200400     GO   TO    LEV-WRITE-GF-3-6.                                 NC1264.2
200500 LEV-DELETE-GF-3-6.                                               NC1264.2
200600     PERFORM DE-LETE.                                             NC1264.2
200700 LEV-WRITE-GF-3-6.                                                NC1264.2
200800     MOVE  "LEV-TEST-GF-3-6" TO PAR-NAME.                         NC1264.2
200900     PERFORM PRINT-DETAIL.                                        NC1264.2
201000 LEV-TEST-GF-3-7.                                                 NC1264.2
201100     IF GP-7 EQUAL TO       "-004,361"                            NC1264.2
201200              PERFORM PASS                                        NC1264.2
201300              GO  TO   LEV-WRITE-GF-3-7.                          NC1264.2
201400     PERFORM FAIL.                                                NC1264.2
201500     MOVE GP-7 TO COMPUTED-A                                      NC1264.2
201600     MOVE  "-004,361"             TO CORRECT-A.                   NC1264.2
201700     GO   TO    LEV-WRITE-GF-3-7.                                 NC1264.2
201800 LEV-DELETE-GF-3-7.                                               NC1264.2
201900     PERFORM DE-LETE.                                             NC1264.2
202000 LEV-WRITE-GF-3-7.                                                NC1264.2
202100     MOVE  "LEV-TEST-GF-3-7" TO PAR-NAME.                         NC1264.2
202200     PERFORM PRINT-DETAIL.                                        NC1264.2
202300 LEV-TEST-GF-3-8.                                                 NC1264.2
202400     IF GP-8 EQUAL TO       "PROGRAM DIVISI"                      NC1264.2
202500              PERFORM PASS                                        NC1264.2
202600              GO  TO   LEV-WRITE-GF-3-8.                          NC1264.2
202700     PERFORM FAIL.                                                NC1264.2
202800     MOVE  "PROGRAM DIVISI"       TO CORRECT-A.                   NC1264.2
202900     GO   TO    LEV-WRITE-GF-3-8.                                 NC1264.2
203000 LEV-DELETE-GF-3-8.                                               NC1264.2
203100     PERFORM DE-LETE.                                             NC1264.2
203200 LEV-WRITE-GF-3-8.                                                NC1264.2
203300     MOVE  "LEV-TEST-GF-3-8" TO PAR-NAME.                         NC1264.2
203400     PERFORM PRINT-DETAIL.                                        NC1264.2
203500 LEV-TEST-GF-3-9.                                                 NC1264.2
203600     IF GP-9 EQUAL TO       "N P L D"                             NC1264.2
203700              PERFORM PASS                                        NC1264.2
203800              GO  TO   LEV-WRITE-GF-3-9.                          NC1264.2
203900     PERFORM FAIL.                                                NC1264.2
204000     MOVE GP-9 TO COMPUTED-A                                      NC1264.2
204100     MOVE  "N P L D"              TO CORRECT-A.                   NC1264.2
204200     GO   TO    LEV-WRITE-GF-3-9.                                 NC1264.2
204300 LEV-DELETE-GF-3-9.                                               NC1264.2
204400     PERFORM DE-LETE.                                             NC1264.2
204500 LEV-WRITE-GF-3-9.                                                NC1264.2
204600     MOVE  "LEV-TEST-GF-3-9" TO PAR-NAME.                         NC1264.2
204700     PERFORM PRINT-DETAIL.                                        NC1264.2
204800 LEV-TEST-GF-3-10.                                                NC1264.2
204900     IF GP-10 EQUAL TO      "7070000"                             NC1264.2
205000              PERFORM PASS                                        NC1264.2
205100              GO  TO   LEV-WRITE-GF-3-10.                         NC1264.2
205200     PERFORM FAIL.                                                NC1264.2
205300     MOVE GP-10 TO COMPUTED-A                                     NC1264.2
205400     MOVE  "7070000"              TO CORRECT-A.                   NC1264.2
205500     GO   TO    LEV-WRITE-GF-3-10.                                NC1264.2
205600 LEV-DELETE-GF-3-10.                                              NC1264.2
205700     PERFORM DE-LETE.                                             NC1264.2
205800 LEV-WRITE-GF-3-10.                                               NC1264.2
205900     MOVE  "LEV-TEST-GF-3-10" TO PAR-NAME.                        NC1264.2
206000     PERFORM PRINT-DETAIL.                                        NC1264.2
206100 LEV-TEST-GF-3-11.                                                NC1264.2
206200     IF GP-11 EQUAL TO     "$005,604.00"                          NC1264.2
206300              PERFORM PASS                                        NC1264.2
206400              GO  TO   LEV-WRITE-GF-3-11.                         NC1264.2
206500     PERFORM FAIL.                                                NC1264.2
206600     MOVE GP-11 TO COMPUTED-A.                                    NC1264.2
206700     MOVE "$005,604.00"           TO CORRECT-A.                   NC1264.2
206800     GO   TO    LEV-WRITE-GF-3-11.                                NC1264.2
206900 LEV-DELETE-GF-3-11.                                              NC1264.2
207000     PERFORM DE-LETE.                                             NC1264.2
207100 LEV-WRITE-GF-3-11.                                               NC1264.2
207200     MOVE  "LEV-TEST-GF-3-11" TO PAR-NAME.                        NC1264.2
207300     PERFORM PRINT-DETAIL.                                        NC1264.2
207400 LEV-TEST-GF-3-12.                                                NC1264.2
207500     IF GP-12 EQUAL TO      "   .0"                               NC1264.2
207600              PERFORM PASS                                        NC1264.2
207700              GO  TO   LEV-WRITE-GF-3-12.                         NC1264.2
207800     PERFORM FAIL.                                                NC1264.2
207900     MOVE GP-12 TO COMPUTED-A                                     NC1264.2
208000     MOVE  "   .0"                TO CORRECT-A.                   NC1264.2
208100     GO   TO    LEV-WRITE-GF-3-12.                                NC1264.2
208200 LEV-DELETE-GF-3-12.                                              NC1264.2
208300     PERFORM DE-LETE.                                             NC1264.2
208400 LEV-WRITE-GF-3-12.                                               NC1264.2
208500     MOVE  "LEV-TEST-GF-3-12" TO PAR-NAME.                        NC1264.2
208600     PERFORM PRINT-DETAIL.                                        NC1264.2
208700 LEV-TEST-GF-3-13.                                                NC1264.2
208800     IF GP-13 EQUAL TO      "  0 400"                             NC1264.2
208900              PERFORM PASS                                        NC1264.2
209000              GO  TO   LEV-WRITE-GF-3-13.                         NC1264.2
209100     PERFORM FAIL.                                                NC1264.2
209200     MOVE GP-13 TO COMPUTED-A.                                    NC1264.2
209300     MOVE  "  0 400"              TO CORRECT-A.                   NC1264.2
209400     GO   TO    LEV-WRITE-GF-3-13.                                NC1264.2
209500 LEV-DELETE-GF-3-13.                                              NC1264.2
209600     PERFORM DE-LETE.                                             NC1264.2
209700 LEV-WRITE-GF-3-13.                                               NC1264.2
209800     MOVE  "LEV-TEST-GF-3-13" TO PAR-NAME.                        NC1264.2
209900     PERFORM PRINT-DETAIL.                                        NC1264.2
210000 LEV-TEST-GF-3-14.                                                NC1264.2
210100     IF GP-14 EQUAL TO      "KLOP"                                NC1264.2
210200              PERFORM PASS                                        NC1264.2
210300              GO  TO   LEV-WRITE-GF-3-14.                         NC1264.2
210400     PERFORM FAIL.                                                NC1264.2
210500     MOVE GP-14 TO COMPUTED-A                                     NC1264.2
210600     MOVE  "KLOP"                 TO CORRECT-A.                   NC1264.2
210700     GO   TO    LEV-WRITE-GF-3-14.                                NC1264.2
210800 LEV-DELETE-GF-3-14.                                              NC1264.2
210900     PERFORM DE-LETE.                                             NC1264.2
211000 LEV-WRITE-GF-3-14.                                               NC1264.2
211100     MOVE  "LEV-TEST-GF-3-14" TO PAR-NAME.                        NC1264.2
211200     PERFORM PRINT-DETAIL.                                        NC1264.2
211300 LEV-TEST-GF-3-15.                                                NC1264.2
211400     IF GP-15  EQUAL TO "2345678902"                              NC1264.2
211500              PERFORM PASS                                        NC1264.2
211600              GO  TO   LEV-WRITE-GF-3-15.                         NC1264.2
211700     PERFORM FAIL.                                                NC1264.2
211800     MOVE GP-15 TO COMPUTED-A.                                    NC1264.2
211900     MOVE  "2345678902"           TO CORRECT-A.                   NC1264.2
212000     GO   TO    LEV-WRITE-GF-3-15.                                NC1264.2
212100 LEV-DELETE-GF-3-15.                                              NC1264.2
212200     PERFORM DE-LETE.                                             NC1264.2
212300 LEV-WRITE-GF-3-15.                                               NC1264.2
212400     MOVE  "LEV-TEST-GF-3-15" TO PAR-NAME.                        NC1264.2
212500     PERFORM PRINT-DETAIL.                                        NC1264.2
212600 LEV-TEST-GF-3-16.                                                NC1264.2
212700     IF GP-16 EQUAL TO  SPACE                                     NC1264.2
212800              PERFORM PASS                                        NC1264.2
212900              GO  TO   LEV-WRITE-GF-3-16.                         NC1264.2
213000     PERFORM FAIL.                                                NC1264.2
213100     MOVE GP-16 TO COMPUTED-A                                     NC1264.2
213200     MOVE  "SPACE"                TO CORRECT-A.                   NC1264.2
213300     GO   TO    LEV-WRITE-GF-3-16.                                NC1264.2
213400 LEV-DELETE-GF-3-16.                                              NC1264.2
213500     PERFORM DE-LETE.                                             NC1264.2
213600 LEV-WRITE-GF-3-16.                                               NC1264.2
213700     MOVE  "LEV-TEST-GF-3-16" TO PAR-NAME.                        NC1264.2
213800     PERFORM PRINT-DETAIL.                                        NC1264.2
213900 LEV-TEST-GF-3-17.                                                NC1264.2
214000     IF GP-17 EQUAL TO  "19  702"                                 NC1264.2
214100              PERFORM PASS                                        NC1264.2
214200              GO  TO   LEV-WRITE-GF-3-17.                         NC1264.2
214300     PERFORM FAIL.                                                NC1264.2
214400     MOVE GP-17 TO COMPUTED-A.                                    NC1264.2
214500     MOVE  "19  702"              TO CORRECT-A.                   NC1264.2
214600     GO   TO    LEV-WRITE-GF-3-17.                                NC1264.2
214700 LEV-DELETE-GF-3-17.                                              NC1264.2
214800     PERFORM DE-LETE.                                             NC1264.2
214900 LEV-WRITE-GF-3-17.                                               NC1264.2
215000     MOVE  "LEV-TEST-GF-3-17" TO PAR-NAME.                        NC1264.2
215100     PERFORM PRINT-DETAIL.                                        NC1264.2
215200 LEV-TEST-GF-3-18.                                                NC1264.2
215300     IF GP-18 EQUAL TO "-***42"                                   NC1264.2
215400              PERFORM PASS                                        NC1264.2
215500              GO  TO   LEV-WRITE-GF-3-18.                         NC1264.2
215600     PERFORM FAIL.                                                NC1264.2
215700     MOVE GP-18 TO COMPUTED-A                                     NC1264.2
215800     MOVE  "-***42" TO CORRECT-A.                                 NC1264.2
215900     GO   TO    LEV-WRITE-GF-3-18.                                NC1264.2
216000 LEV-DELETE-GF-3-18.                                              NC1264.2
216100     PERFORM DE-LETE.                                             NC1264.2
216200 LEV-WRITE-GF-3-18.                                               NC1264.2
216300     MOVE  "LEV-TEST-GF-3-18" TO PAR-NAME.                        NC1264.2
216400     PERFORM PRINT-DETAIL.                                        NC1264.2
216500 LEV-TEST-GF-3-19.                                                NC1264.2
216600     IF GP-19 EQUAL TO       ZERO                                 NC1264.2
216700              PERFORM PASS                                        NC1264.2
216800              GO  TO   LEV-WRITE-GF-3-19.                         NC1264.2
216900     PERFORM FAIL.                                                NC1264.2
217000     MOVE GP-19 TO COMPUTED-A.                                    NC1264.2
217100     MOVE  "0000000"   TO CORRECT-A.                              NC1264.2
217200     GO   TO    LEV-WRITE-GF-3-19.                                NC1264.2
217300 LEV-DELETE-GF-3-19.                                              NC1264.2
217400     PERFORM DE-LETE.                                             NC1264.2
217500 LEV-WRITE-GF-3-19.                                               NC1264.2
217600     MOVE  "LEV-TEST-GF-3-19" TO PAR-NAME.                        NC1264.2
217700     PERFORM PRINT-DETAIL.                                        NC1264.2
217800 LEV-TEST-GF-3-20.                                                NC1264.2
217900     IF GP-20 EQUAL TO      "492  "                               NC1264.2
218000              PERFORM PASS                                        NC1264.2
218100              GO  TO   LEV-WRITE-GF-3-20.                         NC1264.2
218200     PERFORM FAIL.                                                NC1264.2
218300     MOVE GP-20 TO COMPUTED-A                                     NC1264.2
218400     MOVE  "492  "                TO CORRECT-A.                   NC1264.2
218500     GO   TO    LEV-WRITE-GF-3-20.                                NC1264.2
218600 LEV-DELETE-GF-3-20.                                              NC1264.2
218700     PERFORM DE-LETE.                                             NC1264.2
218800 LEV-WRITE-GF-3-20.                                               NC1264.2
218900     MOVE  "LEV-TEST-GF-3-20" TO PAR-NAME.                        NC1264.2
219000     PERFORM PRINT-DETAIL.                                        NC1264.2
219100 LEV-TEST-GF-3-21.                                                NC1264.2
219200     IF GP-21 EQUAL TO       SPACE                                NC1264.2
219300              PERFORM PASS                                        NC1264.2
219400              GO  TO   LEV-WRITE-GF-3-21.                         NC1264.2
219500     PERFORM FAIL.                                                NC1264.2
219600     MOVE GP-20 TO COMPUTED-A                                     NC1264.2
219700     MOVE  "SPACE"                TO CORRECT-A.                   NC1264.2
219800     GO   TO    LEV-WRITE-GF-3-21.                                NC1264.2
219900 LEV-DELETE-GF-3-21.                                              NC1264.2
220000     PERFORM DE-LETE.                                             NC1264.2
220100 LEV-WRITE-GF-3-21.                                               NC1264.2
220200     MOVE  "LEV-TEST-GF-3-21" TO PAR-NAME.                        NC1264.2
220300     PERFORM PRINT-DETAIL.                                        NC1264.2
220400 LEV-TEST-GF-3-22.                                                NC1264.2
220500     IF GP-22 EQUAL TO   "*040290"                                NC1264.2
220600              PERFORM PASS                                        NC1264.2
220700              GO  TO   LEV-WRITE-GF-3-22.                         NC1264.2
220800     PERFORM FAIL                                                 NC1264.2
220900     MOVE GP-22 TO COMPUTED-A                                     NC1264.2
221000     MOVE "*040290"               TO CORRECT-A.                   NC1264.2
221100     GO   TO    LEV-WRITE-GF-3-22.                                NC1264.2
221200 LEV-DELETE-GF-3-22.                                              NC1264.2
221300     PERFORM DE-LETE.                                             NC1264.2
221400 LEV-WRITE-GF-3-22.                                               NC1264.2
221500     MOVE  "LEV-TEST-GF-3-22" TO PAR-NAME.                        NC1264.2
221600     PERFORM PRINT-DETAIL.                                        NC1264.2
221700 LEV-TEST-GF-3-23.                                                NC1264.2
221800     IF GP-23 EQUAL TO      "9A8B7C"                              NC1264.2
221900              PERFORM PASS                                        NC1264.2
222000              GO  TO   LEV-WRITE-GF-3-23.                         NC1264.2
222100     PERFORM FAIL.                                                NC1264.2
222200     MOVE GP-23 TO COMPUTED-A                                     NC1264.2
222300     MOVE  "9A8B7C"               TO CORRECT-A.                   NC1264.2
222400     GO   TO    LEV-WRITE-GF-3-23.                                NC1264.2
222500 LEV-DELETE-GF-3-23.                                              NC1264.2
222600     PERFORM DE-LETE.                                             NC1264.2
222700 LEV-WRITE-GF-3-23.                                               NC1264.2
222800     MOVE  "LEV-TEST-GF-3-23" TO PAR-NAME.                        NC1264.2
222900     PERFORM PRINT-DETAIL.                                        NC1264.2
223000 LEV-TEST-GF-3-24.                                                NC1264.2
223100     IF GP-24 EQUAL TO      "$40,290.00"                          NC1264.2
223200              PERFORM PASS                                        NC1264.2
223300              GO  TO   LEV-WRITE-GF-3-24.                         NC1264.2
223400     PERFORM FAIL.                                                NC1264.2
223500     MOVE GP-24 TO COMPUTED-A                                     NC1264.2
223600     MOVE  "$40,290.00"           TO CORRECT-A.                   NC1264.2
223700     GO   TO    LEV-WRITE-GF-3-24.                                NC1264.2
223800 LEV-DELETE-GF-3-24.                                              NC1264.2
223900     PERFORM DE-LETE.                                             NC1264.2
224000 LEV-WRITE-GF-3-24.                                               NC1264.2
224100     MOVE  "LEV-TEST-GF-3-24" TO PAR-NAME.                        NC1264.2
224200     PERFORM PRINT-DETAIL.                                        NC1264.2
224300 LEV-TEST-GF-3-25.                                                NC1264.2
224400     IF GP-25 EQUAL TO      "2  8   9   "                         NC1264.2
224500              PERFORM PASS                                        NC1264.2
224600              GO  TO   LEV-WRITE-GF-3-25.                         NC1264.2
224700     PERFORM FAIL.                                                NC1264.2
224800     MOVE GP-25 TO COMPUTED-A                                     NC1264.2
224900     MOVE  "2  8   9   "          TO CORRECT-A.                   NC1264.2
225000     GO   TO    LEV-WRITE-GF-3-25.                                NC1264.2
225100 LEV-DELETE-GF-3-25.                                              NC1264.2
225200     PERFORM DE-LETE.                                             NC1264.2
225300 LEV-WRITE-GF-3-25.                                               NC1264.2
225400     MOVE  "LEV-TEST-GF-3-25" TO PAR-NAME.                        NC1264.2
225500     PERFORM PRINT-DETAIL.                                        NC1264.2
225600 LEV-TEST-GF-3-26.                                                NC1264.2
225700     IF GP-26 EQUAL TO      "2250001"                             NC1264.2
225800              PERFORM PASS                                        NC1264.2
225900              GO  TO   LEV-WRITE-GF-3-26.                         NC1264.2
226000     PERFORM FAIL.                                                NC1264.2
226100     MOVE GP-26 TO COMPUTED-A                                     NC1264.2
226200     MOVE  "2250001"              TO CORRECT-A.                   NC1264.2
226300     GO   TO    LEV-WRITE-GF-3-26.                                NC1264.2
226400 LEV-DELETE-GF-3-26.                                              NC1264.2
226500     PERFORM DE-LETE.                                             NC1264.2
226600 LEV-WRITE-GF-3-26.                                               NC1264.2
226700     MOVE  "LEV-TEST-GF-3-26" TO PAR-NAME.                        NC1264.2
226800     PERFORM PRINT-DETAIL.                                        NC1264.2
226900 LEV-TEST-GF-3-27.                                                NC1264.2
227000     IF GP-27 EQUAL TO      "0,001,692"                           NC1264.2
227100              PERFORM PASS                                        NC1264.2
227200              GO  TO   LEV-WRITE-GF-3-27.                         NC1264.2
227300     PERFORM FAIL.                                                NC1264.2
227400     MOVE GP-27 TO COMPUTED-A                                     NC1264.2
227500     MOVE  "0,001,692"            TO CORRECT-A.                   NC1264.2
227600     GO   TO    LEV-WRITE-GF-3-27.                                NC1264.2
227700 LEV-DELETE-GF-3-27.                                              NC1264.2
227800     PERFORM DE-LETE.                                             NC1264.2
227900 LEV-WRITE-GF-3-27.                                               NC1264.2
228000     MOVE  "LEV-TEST-GF-3-27" TO PAR-NAME.                        NC1264.2
228100     PERFORM PRINT-DETAIL.                                        NC1264.2
228200 LEV-TEST-GF-3-28.                                                NC1264.2
228300     IF GP-28 EQUAL TO      "0000004,1"                           NC1264.2
228400              PERFORM PASS                                        NC1264.2
228500              GO  TO   LEV-WRITE-GF-3-28.                         NC1264.2
228600     PERFORM FAIL.                                                NC1264.2
228700     MOVE GP-28 TO COMPUTED-A.                                    NC1264.2
228800     MOVE  "0000004,1"            TO CORRECT-A.                   NC1264.2
228900     GO   TO    LEV-WRITE-GF-3-28.                                NC1264.2
229000 LEV-DELETE-GF-3-28.                                              NC1264.2
229100     PERFORM DE-LETE.                                             NC1264.2
229200 LEV-WRITE-GF-3-28.                                               NC1264.2
229300     MOVE  "LEV-TEST-GF-3-28" TO PAR-NAME.                        NC1264.2
229400     PERFORM PRINT-DETAIL.                                        NC1264.2
229500 LEV-TEST-GF-3-29.                                                NC1264.2
229600     IF GP-29 EQUAL TO      "$**1.00"                             NC1264.2
229700              PERFORM PASS                                        NC1264.2
229800              GO  TO   LEV-WRITE-GF-3-29.                         NC1264.2
229900     PERFORM FAIL.                                                NC1264.2
230000     MOVE GP-29 TO COMPUTED-A                                     NC1264.2
230100     MOVE  "$**1.00"              TO CORRECT-A.                   NC1264.2
230200     GO   TO    LEV-WRITE-GF-3-29.                                NC1264.2
230300 LEV-DELETE-GF-3-29.                                              NC1264.2
230400     PERFORM DE-LETE.                                             NC1264.2
230500 LEV-WRITE-GF-3-29.                                               NC1264.2
230600     MOVE  "LEV-TEST-GF-3-29" TO PAR-NAME.                        NC1264.2
230700     PERFORM PRINT-DETAIL.                                        NC1264.2
230800 LEV-TEST-GF-3-30.                                                NC1264.2
230900     IF GP-30 EQUAL TO      ZERO                                  NC1264.2
231000              PERFORM PASS                                        NC1264.2
231100              GO  TO   LEV-WRITE-GF-3-30.                         NC1264.2
231200     PERFORM FAIL.                                                NC1264.2
231300     MOVE GP-30 TO COMPUTED-A                                     NC1264.2
231400     MOVE  "000000000000000"      TO CORRECT-A.                   NC1264.2
231500     GO   TO    LEV-WRITE-GF-3-30.                                NC1264.2
231600 LEV-DELETE-GF-3-30.                                              NC1264.2
231700     PERFORM DE-LETE.                                             NC1264.2
231800 LEV-WRITE-GF-3-30.                                               NC1264.2
231900     MOVE  "LEV-TEST-GF-3-30" TO PAR-NAME.                        NC1264.2
232000     PERFORM PRINT-DETAIL.                                        NC1264.2
232100 LEV-TEST-GF-3-31.                                                NC1264.2
232200     IF GP-31  EQUAL TO ZERO                                      NC1264.2
232300              PERFORM PASS                                        NC1264.2
232400              GO  TO   LEV-WRITE-GF-3-31.                         NC1264.2
232500     PERFORM FAIL.                                                NC1264.2
232600     MOVE GP-31 TO COMPUTED-A                                     NC1264.2
232700     MOVE  "SPACE"                TO CORRECT-A.                   NC1264.2
232800     GO   TO    LEV-WRITE-GF-3-31.                                NC1264.2
232900 LEV-DELETE-GF-3-31.                                              NC1264.2
233000     PERFORM DE-LETE.                                             NC1264.2
233100 LEV-WRITE-GF-3-31.                                               NC1264.2
233200     MOVE  "LEV-TEST-GF-3-31" TO PAR-NAME.                        NC1264.2
233300     PERFORM PRINT-DETAIL.                                        NC1264.2
233400 LEV-TEST-GF-3-32.                                                NC1264.2
233500     IF GP-32 EQUAL TO      "*00"                                 NC1264.2
233600              PERFORM PASS                                        NC1264.2
233700              GO  TO   LEV-WRITE-GF-3-32.                         NC1264.2
233800     PERFORM FAIL.                                                NC1264.2
233900     MOVE GP-32 TO COMPUTED-A                                     NC1264.2
234000     MOVE  "*00"                  TO CORRECT-A.                   NC1264.2
234100     GO   TO    LEV-WRITE-GF-3-32.                                NC1264.2
234200 LEV-DELETE-GF-3-32.                                              NC1264.2
234300     PERFORM DE-LETE.                                             NC1264.2
234400 LEV-WRITE-GF-3-32.                                               NC1264.2
234500     MOVE  "LEV-TEST-GF-3-32" TO PAR-NAME.                        NC1264.2
234600     PERFORM PRINT-DETAIL.                                        NC1264.2
234700 LEV-TEST-GF-3-33.                                                NC1264.2
234800     IF GP-33 EQUAL TO      "   0"                                NC1264.2
234900              PERFORM PASS                                        NC1264.2
235000              GO  TO   LEV-WRITE-GF-3-33.                         NC1264.2
235100     PERFORM FAIL.                                                NC1264.2
235200     MOVE GP-33 TO COMPUTED-A                                     NC1264.2
235300     MOVE  "   0"                 TO CORRECT-A.                   NC1264.2
235400     GO   TO    LEV-WRITE-GF-3-33.                                NC1264.2
235500 LEV-DELETE-GF-3-33.                                              NC1264.2
235600     PERFORM DE-LETE.                                             NC1264.2
235700 LEV-WRITE-GF-3-33.                                               NC1264.2
235800     MOVE  "LEV-TEST-GF-3-33" TO PAR-NAME.                        NC1264.2
235900     PERFORM PRINT-DETAIL.                                        NC1264.2
236000 LEV-TEST-GF-3-34.                                                NC1264.2
236100     IF GP-34 EQUAL TO      "  2  1"                              NC1264.2
236200              PERFORM PASS                                        NC1264.2
236300              GO  TO   LEV-WRITE-GF-3-34.                         NC1264.2
236400     PERFORM FAIL.                                                NC1264.2
236500     MOVE GP-34 TO COMPUTED-A.                                    NC1264.2
236600     MOVE  "  2  1"               TO CORRECT-A.                   NC1264.2
236700     GO   TO    LEV-WRITE-GF-3-34.                                NC1264.2
236800 LEV-DELETE-GF-3-34.                                              NC1264.2
236900     PERFORM DE-LETE.                                             NC1264.2
237000 LEV-WRITE-GF-3-34.                                               NC1264.2
237100     MOVE  "LEV-TEST-GF-3-34" TO PAR-NAME.                        NC1264.2
237200     PERFORM PRINT-DETAIL.                                        NC1264.2
237300 LEV-TEST-GF-3-35.                                                NC1264.2
237400     IF GP-35 EQUAL TO      "$00,036.00"                          NC1264.2
237500              PERFORM PASS                                        NC1264.2
237600              GO  TO   LEV-WRITE-GF-3-35.                         NC1264.2
237700     PERFORM FAIL.                                                NC1264.2
237800     MOVE GP-35 TO COMPUTED-A.                                    NC1264.2
237900     MOVE  "$00,036.00"           TO CORRECT-A.                   NC1264.2
238000     GO   TO    LEV-WRITE-GF-3-35.                                NC1264.2
238100 LEV-DELETE-GF-3-35.                                              NC1264.2
238200     PERFORM DE-LETE.                                             NC1264.2
238300 LEV-WRITE-GF-3-35.                                               NC1264.2
238400     MOVE  "LEV-TEST-GF-3-35" TO PAR-NAME.                        NC1264.2
238500     PERFORM PRINT-DETAIL.                                        NC1264.2
238600 LEV-TEST-GF-3-36.                                                NC1264.2
238700     IF GP-36 EQUAL TO      "090108"                              NC1264.2
238800              PERFORM PASS                                        NC1264.2
238900              GO  TO   LEV-WRITE-GF-3-36.                         NC1264.2
239000     PERFORM FAIL.                                                NC1264.2
239100     MOVE GP-36 TO COMPUTED-A                                     NC1264.2
239200     MOVE  "090108"               TO CORRECT-A.                   NC1264.2
239300     GO   TO    LEV-WRITE-GF-3-36.                                NC1264.2
239400 LEV-DELETE-GF-3-36.                                              NC1264.2
239500     PERFORM DE-LETE.                                             NC1264.2
239600 LEV-WRITE-GF-3-36.                                               NC1264.2
239700     MOVE  "LEV-TEST-GF-3-36" TO PAR-NAME.                        NC1264.2
239800     PERFORM PRINT-DETAIL.                                        NC1264.2
239900 LEV-TEST-GF-3-37.                                                NC1264.2
240000     IF GP-37 EQUAL TO       SPACE                                NC1264.2
240100              PERFORM PASS                                        NC1264.2
240200              GO  TO   LEV-WRITE-GF-3-37.                         NC1264.2
240300     PERFORM FAIL.                                                NC1264.2
240400     MOVE GP-37 TO COMPUTED-A                                     NC1264.2
240500     MOVE  "SPACE"                TO CORRECT-A.                   NC1264.2
240600     GO   TO    LEV-WRITE-GF-3-37.                                NC1264.2
240700 LEV-DELETE-GF-3-37.                                              NC1264.2
240800     PERFORM DE-LETE.                                             NC1264.2
240900 LEV-WRITE-GF-3-37.                                               NC1264.2
241000     MOVE  "LEV-TEST-GF-3-37" TO PAR-NAME.                        NC1264.2
241100     PERFORM PRINT-DETAIL.                                        NC1264.2
241200 LEV-TEST-GF-3-38.                                                NC1264.2
241300     IF GP-38 EQUAL TO      "-36"                                 NC1264.2
241400              PERFORM PASS                                        NC1264.2
241500              GO  TO   LEV-WRITE-GF-3-38.                         NC1264.2
241600     PERFORM FAIL.                                                NC1264.2
241700     MOVE GP-38 TO COMPUTED-A                                     NC1264.2
241800     MOVE  "-36"                  TO CORRECT-A.                   NC1264.2
241900     GO   TO    LEV-WRITE-GF-3-38.                                NC1264.2
242000 LEV-DELETE-GF-3-38.                                              NC1264.2
242100     PERFORM DE-LETE.                                             NC1264.2
242200 LEV-WRITE-GF-3-38.                                               NC1264.2
242300     MOVE  "LEV-TEST-GF-3-38" TO PAR-NAME.                        NC1264.2
242400     PERFORM PRINT-DETAIL.                                        NC1264.2
242500 LEV-TEST-GF-3-39.                                                NC1264.2
242600     IF GP-39 EQUAL TO      " 24"                                 NC1264.2
242700              PERFORM PASS                                        NC1264.2
242800              GO  TO   LEV-WRITE-GF-3-39.                         NC1264.2
242900              PERFORM FAIL.                                       NC1264.2
243000     MOVE GP-39 TO COMPUTED-A                                     NC1264.2
243100     MOVE  " 24"                  TO CORRECT-A.                   NC1264.2
243200     GO   TO    LEV-WRITE-GF-3-39.                                NC1264.2
243300 LEV-DELETE-GF-3-39.                                              NC1264.2
243400     PERFORM DE-LETE.                                             NC1264.2
243500 LEV-WRITE-GF-3-39.                                               NC1264.2
243600     MOVE  "LEV-TEST-GF-3-39" TO PAR-NAME.                        NC1264.2
243700     PERFORM PRINT-DETAIL.                                        NC1264.2
243800 LEV-TEST-GF-3-40.                                                NC1264.2
243900     IF GP-40 EQUAL TO      "36  "                                NC1264.2
244000              PERFORM PASS                                        NC1264.2
244100              GO  TO   LEV-WRITE-GF-3-40.                         NC1264.2
244200     PERFORM FAIL.                                                NC1264.2
244300     MOVE GP-40 TO COMPUTED-A                                     NC1264.2
244400     MOVE  "36  "                 TO CORRECT-A.                   NC1264.2
244500     GO   TO    LEV-WRITE-GF-3-40.                                NC1264.2
244600 LEV-DELETE-GF-3-40.                                              NC1264.2
244700     PERFORM DE-LETE.                                             NC1264.2
244800 LEV-WRITE-GF-3-40.                                               NC1264.2
244900     MOVE  "LEV-TEST-GF-3-40" TO PAR-NAME.                        NC1264.2
245000     PERFORM PRINT-DETAIL.                                        NC1264.2
245100 LEV-TEST-GF-3-41.                                                NC1264.2
245200     IF GP-41 EQUAL TO      "01DB"                                NC1264.2
245300              PERFORM PASS                                        NC1264.2
245400              GO  TO   LEV-WRITE-GF-3-41.                         NC1264.2
245500     PERFORM FAIL.                                                NC1264.2
245600     MOVE GP-41 TO COMPUTED-A                                     NC1264.2
245700     MOVE  "01DB"                 TO CORRECT-A.                   NC1264.2
245800     GO   TO    LEV-WRITE-GF-3-41.                                NC1264.2
245900 LEV-DELETE-GF-3-41.                                              NC1264.2
246000     PERFORM DE-LETE.                                             NC1264.2
246100 LEV-WRITE-GF-3-41.                                               NC1264.2
246200     MOVE  "LEV-TEST-GF-3-41" TO PAR-NAME.                        NC1264.2
246300     PERFORM PRINT-DETAIL.                                        NC1264.2
246400 LEV-TEST-GF-3-42.                                                NC1264.2
246500     IF GP-42 EQUAL TO "****"                                     NC1264.2
246600              PERFORM PASS                                        NC1264.2
246700              GO  TO   LEV-WRITE-GF-3-42.                         NC1264.2
246800     PERFORM FAIL.                                                NC1264.2
246900     MOVE GP-42       TO COMPUTED-A.                              NC1264.2
247000     MOVE "****"  TO CORRECT-A.                                   NC1264.2
247100     GO   TO    LEV-WRITE-GF-3-42.                                NC1264.2
247200 LEV-DELETE-GF-3-42.                                              NC1264.2
247300     PERFORM DE-LETE.                                             NC1264.2
247400 LEV-WRITE-GF-3-42.                                               NC1264.2
247500     MOVE  "LEV-TEST-GF-3-42" TO PAR-NAME.                        NC1264.2
247600     PERFORM PRINT-DETAIL.                                        NC1264.2
247700 LEV-TEST-GF-3-43.                                                NC1264.2
247800     IF GP-43 EQUAL TO      "AIR"                                 NC1264.2
247900              PERFORM PASS                                        NC1264.2
248000              GO  TO   LEV-WRITE-GF-3-43.                         NC1264.2
248100     PERFORM FAIL.                                                NC1264.2
248200     MOVE GP-43 TO COMPUTED-A                                     NC1264.2
248300     MOVE  "AIR"                  TO CORRECT-A.                   NC1264.2
248400     GO   TO    LEV-WRITE-GF-3-43.                                NC1264.2
248500 LEV-DELETE-GF-3-43.                                              NC1264.2
248600     PERFORM DE-LETE.                                             NC1264.2
248700 LEV-WRITE-GF-3-43.                                               NC1264.2
248800     MOVE  "LEV-TEST-GF-3-43" TO PAR-NAME.                        NC1264.2
248900     PERFORM PRINT-DETAIL.                                        NC1264.2
249000 LEV-TEST-GF-3-44.                                                NC1264.2
249100     IF GP-44 EQUAL TO      "9ZX"                                 NC1264.2
249200              PERFORM PASS                                        NC1264.2
249300              GO  TO   LEV-WRITE-GF-3-44.                         NC1264.2
249400     PERFORM FAIL.                                                NC1264.2
249500     MOVE GP-44 TO COMPUTED-A                                     NC1264.2
249600     MOVE  "9ZX"                  TO CORRECT-A.                   NC1264.2
249700     GO   TO    LEV-WRITE-GF-3-44.                                NC1264.2
249800 LEV-DELETE-GF-3-44.                                              NC1264.2
249900     PERFORM DE-LETE.                                             NC1264.2
250000 LEV-WRITE-GF-3-44.                                               NC1264.2
250100     MOVE  "LEV-TEST-GF-3-44" TO PAR-NAME.                        NC1264.2
250200     PERFORM PRINT-DETAIL.                                        NC1264.2
250300 LEV-TEST-GF-3-45.                                                NC1264.2
250400     IF GP-45 EQUAL TO       "*1000"                              NC1264.2
250500              PERFORM PASS                                        NC1264.2
250600              GO  TO   LEV-WRITE-GF-3-45.                         NC1264.2
250700     PERFORM FAIL.                                                NC1264.2
250800     MOVE GP-45 TO COMPUTED-A                                     NC1264.2
250900     MOVE  "*1000"                TO CORRECT-A.                   NC1264.2
251000     GO   TO    LEV-WRITE-GF-3-45.                                NC1264.2
251100 LEV-DELETE-GF-3-45.                                              NC1264.2
251200     PERFORM DE-LETE.                                             NC1264.2
251300 LEV-WRITE-GF-3-45.                                               NC1264.2
251400     MOVE  "LEV-TEST-GF-3-45" TO PAR-NAME.                        NC1264.2
251500     PERFORM PRINT-DETAIL.                                        NC1264.2
251600 LEV-TEST-GF-3-46.                                                NC1264.2
251700     IF GP-46 EQUAL TO      "0000000093.00"                       NC1264.2
251800              PERFORM PASS                                        NC1264.2
251900              GO  TO   LEV-WRITE-GF-3-46.                         NC1264.2
252000     PERFORM FAIL.                                                NC1264.2
252100     MOVE GP-46 TO COMPUTED-A                                     NC1264.2
252200     MOVE  "0000000093.00"        TO CORRECT-A.                   NC1264.2
252300     GO   TO    LEV-WRITE-GF-3-46.                                NC1264.2
252400 LEV-DELETE-GF-3-46.                                              NC1264.2
252500     PERFORM DE-LETE.                                             NC1264.2
252600 LEV-WRITE-GF-3-46.                                               NC1264.2
252700     MOVE  "LEV-TEST-GF-3-46" TO PAR-NAME.                        NC1264.2
252800     PERFORM PRINT-DETAIL.                                        NC1264.2
252900 LEV-TEST-GF-3-47.                                                NC1264.2
253000     IF GP-47 (1) EQUAL TO "5"                                    NC1264.2
253100              PERFORM PASS                                        NC1264.2
253200              GO  TO   LEV-WRITE-GF-3-47.                         NC1264.2
253300     PERFORM FAIL.                                                NC1264.2
253400     MOVE GP-47 (1)   TO COMPUTED-A.                              NC1264.2
253500     MOVE  "5"                    TO CORRECT-A.                   NC1264.2
253600     GO   TO    LEV-WRITE-GF-3-47.                                NC1264.2
253700 LEV-DELETE-GF-3-47.                                              NC1264.2
253800     PERFORM DE-LETE.                                             NC1264.2
253900 LEV-WRITE-GF-3-47.                                               NC1264.2
254000     MOVE  "LEV-TEST-GF-3-47" TO PAR-NAME.                        NC1264.2
254100     PERFORM PRINT-DETAIL.                                        NC1264.2
254200 LEV-TEST-GF-3-48.                                                NC1264.2
254300     IF GP-47 (2) EQUAL TO ZERO                                   NC1264.2
254400              PERFORM PASS                                        NC1264.2
254500              GO  TO   LEV-WRITE-GF-3-48.                         NC1264.2
254600     PERFORM FAIL.                                                NC1264.2
254700     MOVE GP-47 (2) TO COMPUTED-A.                                NC1264.2
254800     MOVE   ZERO TO  CORRECT-A.                                   NC1264.2
254900     GO   TO    LEV-WRITE-GF-3-48.                                NC1264.2
255000 LEV-DELETE-GF-3-48.                                              NC1264.2
255100     PERFORM DE-LETE.                                             NC1264.2
255200 LEV-WRITE-GF-3-48.                                               NC1264.2
255300     MOVE  "LEV-TEST-GF-3-48" TO PAR-NAME.                        NC1264.2
255400     PERFORM PRINT-DETAIL.                                        NC1264.2
255500 LEV-TEST-GF-3-49.                                                NC1264.2
255600     IF GP-48 (1) EQUAL TO "Y"                                    NC1264.2
255700              PERFORM PASS                                        NC1264.2
255800              GO TO LEV-WRITE-GF-3-49.                            NC1264.2
255900     PERFORM FAIL.                                                NC1264.2
256000     MOVE GP-48 (1)   TO COMPUTED-A.                              NC1264.2
256100     MOVE  "Y"                    TO CORRECT-A.                   NC1264.2
256200     GO TO LEV-WRITE-GF-3-49.                                     NC1264.2
256300 LEV-DELETE-GF-3-49.                                              NC1264.2
256400     PERFORM DE-LETE.                                             NC1264.2
256500 LEV-WRITE-GF-3-49.                                               NC1264.2
256600     MOVE "LEV-TEST-GF-3-49"  TO PAR-NAME.                        NC1264.2
256700     PERFORM PRINT-DETAIL.                                        NC1264.2
256800 LEV-TEST-GF-3-50.                                                NC1264.2
256900     IF GP-48 (2) EQUAL TO " "                                    NC1264.2
257000              PERFORM PASS                                        NC1264.2
257100              GO TO LEV-WRITE-GF-3-50.                            NC1264.2
257200     PERFORM FAIL.                                                NC1264.2
257300     MOVE GP-48 (2) TO COMPUTED-A.                                NC1264.2
257400     MOVE " "    TO CORRECT-A.                                    NC1264.2
257500     GO TO LEV-WRITE-GF-3-50.                                     NC1264.2
257600 LEV-DELETE-GF-3-50.                                              NC1264.2
257700     PERFORM DE-LETE.                                             NC1264.2
257800 LEV-WRITE-GF-3-50.                                               NC1264.2
257900     MOVE "LEV-TEST-GF-3-50"  TO PAR-NAME.                        NC1264.2
258000     PERFORM PRINT-DETAIL.                                        NC1264.2
258100     GO TO    CCVS-EXIT.                                          NC1264.2
258200 BREAKDOWN-PARA.                                                  NC1264.2
258300     PERFORM  FAIL.                                               NC1264.2
258400     MOVE     CM-20  TO COMPUTED-A.                               NC1264.2
258500     MOVE     CR-20  TO CORRECT-A.                                NC1264.2
258600     MOVE     " 1ST 20 POSITIONS OF RESULTS" TO RE-MARK.          NC1264.2
258700     IF       LENGTH-COUNTER LESS THAN  21 GO TO BREAKDOWN-EXIT.  NC1264.2
258800     PERFORM  PRINT-DETAIL.                                       NC1264.2
258900     MOVE     CM-40  TO COMPUTED-A.                               NC1264.2
259000     MOVE     CR-40  TO CORRECT-A.                                NC1264.2
259100     MOVE     " 2ND 20 POSITIONS OF RESULTS" TO RE-MARK.          NC1264.2
259200     IF       LENGTH-COUNTER LESS THAN  41 GO TO BREAKDOWN-EXIT.  NC1264.2
259300     PERFORM  PRINT-DETAIL.                                       NC1264.2
259400     MOVE     CM-60  TO COMPUTED-A.                               NC1264.2
259500     MOVE     CR-60  TO CORRECT-A.                                NC1264.2
259600     MOVE     " 3RD 20 POSITIONS OF RESULTS" TO RE-MARK.          NC1264.2
259700     IF       LENGTH-COUNTER LESS THAN  61 GO TO BREAKDOWN-EXIT.  NC1264.2
259800     PERFORM  PRINT-DETAIL.                                       NC1264.2
259900     MOVE     CM-80  TO COMPUTED-A.                               NC1264.2
260000     MOVE     CR-80  TO CORRECT-A.                                NC1264.2
260100     MOVE     " 4TH 20 POSITIONS OF RESULTS" TO RE-MARK.          NC1264.2
260200     IF       LENGTH-COUNTER LESS THAN  81 GO TO BREAKDOWN-EXIT.  NC1264.2
260300     PERFORM  PRINT-DETAIL.                                       NC1264.2
260400     MOVE     CM-100 TO COMPUTED-A.                               NC1264.2
260500     MOVE     CR-100 TO CORRECT-A.                                NC1264.2
260600     MOVE     " 5TH 20 POSITIONS OF RESULTS" TO RE-MARK.          NC1264.2
260700     IF       LENGTH-COUNTER LESS THAN 101 GO TO BREAKDOWN-EXIT.  NC1264.2
260800     PERFORM  PRINT-DETAIL.                                       NC1264.2
260900     MOVE     CM-120 TO COMPUTED-A.                               NC1264.2
261000     MOVE     CR-120 TO CORRECT-A.                                NC1264.2
261100     MOVE     " 6TH 20 POSITIONS OF RESULTS" TO RE-MARK.          NC1264.2
261200     IF       LENGTH-COUNTER LESS THAN 121 GO TO BREAKDOWN-EXIT.  NC1264.2
261300     PERFORM  PRINT-DETAIL.                                       NC1264.2
261400     MOVE     CM-140 TO COMPUTED-A.                               NC1264.2
261500     MOVE     CR-140 TO CORRECT-A.                                NC1264.2
261600     MOVE     " 7TH 20 POSITIONS OF RESULTS" TO RE-MARK.          NC1264.2
261700     IF       LENGTH-COUNTER LESS THAN 141 GO TO BREAKDOWN-EXIT.  NC1264.2
261800     PERFORM  PRINT-DETAIL.                                       NC1264.2
261900     MOVE     CM-160 TO COMPUTED-A.                               NC1264.2
262000     MOVE     CR-160 TO CORRECT-A.                                NC1264.2
262100     MOVE     " 8TH 20 POSITIONS OF RESULTS" TO RE-MARK.          NC1264.2
262200     IF       LENGTH-COUNTER LESS THAN 161 GO TO BREAKDOWN-EXIT.  NC1264.2
262300     PERFORM  PRINT-DETAIL.                                       NC1264.2
262400     MOVE     CM-180 TO COMPUTED-A.                               NC1264.2
262500     MOVE     CR-180 TO CORRECT-A.                                NC1264.2
262600     MOVE     " 9TH 20 POSITIONS OF RESULTS" TO RE-MARK.          NC1264.2
262700     IF       LENGTH-COUNTER LESS THAN 181 GO TO BREAKDOWN-EXIT.  NC1264.2
262800     PERFORM  PRINT-DETAIL.                                       NC1264.2
262900     MOVE     CM-200 TO COMPUTED-A.                               NC1264.2
263000     MOVE     CR-200 TO CORRECT-A.                                NC1264.2
263100     MOVE     "10TH 20 POSITIONS OF RESULTS" TO RE-MARK.          NC1264.2
263200 BREAKDOWN-EXIT.                                                  NC1264.2
263300     MOVE     ZERO TO LENGTH-COUNTER.                             NC1264.2
263400 CCVS-EXIT SECTION.                                               NC1264.2
263500 CCVS-999999.                                                     NC1264.2
263600     GO TO CLOSE-FILES.                                           NC1264.2
      *END-OF,NC126A                                                                  
