      *HEADER,COBOL,IX121A                                                            
000100 IDENTIFICATION DIVISION.                                         IX1214.2
000200 PROGRAM-ID.                                                      IX1214.2
000300     IX121A.                                                      IX1214.2
000400****************************************************************  IX1214.2
000500*                                                              *  IX1214.2
000600*    VALIDATION FOR:-                                          *  IX1214.2
000700*                                                              *  IX1214.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IX1214.2
000900*                                                              *  IX1214.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IX1214.2
001100*                                                              *  IX1214.2
001200****************************************************************  IX1214.2
001300*                                                                 IX1214.2
001400*    1.  THE  ROUTINE  CREATES  THE  MASS  STORAGE  FILE  IX-VS2  IX1214.2
001500*        CONTAINING  50  RECORDS. EACH BLOCK CONTAINS 2 RECORDS,  IX1214.2
001600*        EACH  RECORD  CONTAINS  240 CHARACTERS, ORGANIZATION IS  IX1214.2
001700*        INDEXED,  ACCESS  IS SEQUENTIAL. THEN THE FILE IS OPENED IX1214.2
001800*        AS I-O AND IT IS ATTEMTED TO REWRITE A RECORD WITH A     IX1214.2
001900*        WRONG RECORD LENGTH.  THE RECORD REWRITTEN IS LONGER     IX1214.2
002000*        THAN THE RECORD READ.                                    IX1214.2
002100*                                                                 IX1214.2
002200*    NOTE: THIS PROGRAM IS A SUPPLEMENT TO PROGRAM IX112A, WHICH  IX1214.2
002300*        REWRITES A RECORD SHORTER THAN THE RECORD READ.          IX1214.2
002400*                                                                 IX1214.2
002500*    2. THE ROUTINE READS THE CREATED FILE, VERIFIES IT AND       IX1214.2
002600*       CHECKS THE FILE STATUS CODE:                              IX1214.2
002700*           44  -  AFTER REWRITE   (WITH WRONG RECORD LENGTH)     IX1214.2
002800*                                                                 IX1214.2
002900*    3. X-CARDS USED IN THIS PROGRAM:                             IX1214.2
003000*                                                                 IX1214.2
003100*                 XXXXX024                                        IX1214.2
003200*                 XXXXX055.                                       IX1214.2
003300*                 XXXXX062.                                       IX1214.2
003400*                 XXXXX082.                                       IX1214.2
003500*                 XXXXX083.                                       IX1214.2
003600*                 XXXXX084                                        IX1214.2
003700*                                                                 IX1214.2
003800*                                                                 IX1214.2
003900 ENVIRONMENT DIVISION.                                            IX1214.2
004000 CONFIGURATION SECTION.                                           IX1214.2
004100 SOURCE-COMPUTER.                                                 IX1214.2
004200     XXXXX082.                                                    IX1214.2
004300 OBJECT-COMPUTER.                                                 IX1214.2
004400     XXXXX083.                                                    IX1214.2
004500 INPUT-OUTPUT SECTION.                                            IX1214.2
004600 FILE-CONTROL.                                                    IX1214.2
004700*    SELECT RAW-DATA   ASSIGN TO                                  IX1214.2
004800*    XXXXX062                                                     IX1214.2
004900*           ORGANIZATION IS INDEXED                               IX1214.2
005000*           ACCESS MODE IS RANDOM                                 IX1214.2
005100*           RECORD KEY IS RAW-DATA-KEY.                           IX1214.2
005200*                                                                 IX1214.2
005300     SELECT PRINT-FILE ASSIGN TO                                  IX1214.2
005400     XXXXX055.                                                    IX1214.2
005500*                                                                 IX1214.2
005600     SELECT IX-VS2 ASSIGN                                         IX1214.2
005700     XXXXX024                                                     IX1214.2
005800     ORGANIZATION IS INDEXED                                      IX1214.2
005900     ACCESS MODE IS SEQUENTIAL                                    IX1214.2
006000     RECORD KEY IS IX-VS2-KEY                                     IX1214.2
006100     FILE STATUS IS IX-VS2-STATUS.                                IX1214.2
006200                                                                  IX1214.2
006300 DATA DIVISION.                                                   IX1214.2
006400                                                                  IX1214.2
006500 FILE SECTION.                                                    IX1214.2
006600*                                                                 IX1214.2
006700*FD  RAW-DATA.                                                    IX1214.2
006800*                                                                 IX1214.2
006900*01  RAW-DATA-SATZ.                                               IX1214.2
007000*    05  RAW-DATA-KEY        PIC X(6).                            IX1214.2
007100*    05  C-DATE              PIC 9(6).                            IX1214.2
007200*    05  C-TIME              PIC 9(8).                            IX1214.2
007300*    05  C-NO-OF-TESTS       PIC 99.                              IX1214.2
007400*    05  C-OK                PIC 999.                             IX1214.2
007500*    05  C-ALL               PIC 999.                             IX1214.2
007600*    05  C-FAIL              PIC 999.                             IX1214.2
007700*    05  C-DELETED           PIC 999.                             IX1214.2
007800*    05  C-INSPECT           PIC 999.                             IX1214.2
007900*    05  C-NOTE              PIC X(13).                           IX1214.2
008000*    05  C-INDENT            PIC X.                               IX1214.2
008100*    05  C-ABORT             PIC X(8).                            IX1214.2
008200                                                                  IX1214.2
008300 FD  PRINT-FILE.                                                  IX1214.2
008400                                                                  IX1214.2
008500 01  PRINT-REC               PIC X(120).                          IX1214.2
008600                                                                  IX1214.2
008700 01  DUMMY-RECORD            PIC X(120).                          IX1214.2
008800                                                                  IX1214.2
008900 FD  IX-VS2                                                       IX1214.2
009000C       DATA RECORDS IX-VS2R1-F-G-240 IX-VS2R1-F-G-200            IX1214.2
009100C                                     IX-VS2R1-F-G-280            IX1214.2
009200C       LABEL RECORD STANDARD                                     IX1214.2
009300        RECORD 200 TO 280                                         IX1214.2
009400        BLOCK CONTAINS 2 RECORDS.                                 IX1214.2
009500                                                                  IX1214.2
009600 01  IX-VS2R1-F-G-240.                                            IX1214.2
009700     05  IX-VS2-REC-120      PIC X(120).                          IX1214.2
009800     05  IX-VS2-REC-120-240.                                      IX1214.2
009900         10  FILLER          PIC X(8).                            IX1214.2
010000         10  IX-VS2-KEY      PIC X(29).                           IX1214.2
010100         10  FILLER          PIC X(9).                            IX1214.2
010200         10  IX-VS2-ALTER-KEY  PIC X(29).                         IX1214.2
010300         10  FILLER            PIC X(45).                         IX1214.2
010400                                                                  IX1214.2
010500 01  IX-VS2R1-F-G-200.                                            IX1214.2
010600     05  IX-VS2-REC-SHORT    PIC X(120).                          IX1214.2
010700     05  IX-VS2-REC-120-200  PIC X(80).                           IX1214.2
010800                                                                  IX1214.2
010900 01  IX-VS2R1-F-G-280.                                            IX1214.2
011000     05  IX-VS2-REC-LONG     PIC X(120).                          IX1214.2
011100     05  IX-VS2-REC-120-239  PIC X(120).                          IX1214.2
011200     05  IX-VS2-REC-240-280  PIC X(40).                           IX1214.2
011300                                                                  IX1214.2
011400 WORKING-STORAGE SECTION.                                         IX1214.2
011500                                                                  IX1214.2
011600 01  GRP-0101.                                                    IX1214.2
011700     05  FILLER              PIC X(10) VALUE "RECORD-KEY".        IX1214.2
011800     05  GRP-0101-KEY        PIC 9(9)  VALUE ZERO.                IX1214.2
011900     05  FILLER              PIC X(10) VALUE "END-OF-KEY".        IX1214.2
012000                                                                  IX1214.2
012100 01  GRP-0102.                                                    IX1214.2
012200     05  FILLER              PIC X(10) VALUE "ALTERN-KEY".        IX1214.2
012300     05  GRP-0102-KEY        PIC 9(9)  VALUE ZERO.                IX1214.2
012400     05  FILLER              PIC X(10) VALUE "END-AL-KEY".        IX1214.2
012500                                                                  IX1214.2
012600 01  WRK-CS-09V00            PIC S9(9) COMP VALUE ZERO.           IX1214.2
012700                                                                  IX1214.2
012800 01  EOF-FLAG                PIC 9 VALUE ZERO.                    IX1214.2
012900                                                                  IX1214.2
013000 01  RECORDS-IN-ERROR        PIC S9(5) COMP VALUE ZERO.           IX1214.2
013100                                                                  IX1214.2
013200 01  ERROR-FLAG              PIC 9 VALUE ZERO.                    IX1214.2
013300                                                                  IX1214.2
013400 01  PERM-ERRORS             PIC S9(5) COMP VALUE ZERO.           IX1214.2
013500                                                                  IX1214.2
013600 01  STATUS-TEST-00          PIC 9 VALUE ZERO.                    IX1214.2
013700                                                                  IX1214.2
013800 01  STATUS-TEST-10          PIC 9 VALUE ZERO.                    IX1214.2
013900 01  STATUS-TEST-READ        PIC 9 VALUE ZERO.                    IX1214.2
014000                                                                  IX1214.2
014100 01  IX-VS2-STATUS.                                               IX1214.2
014200     05  IX-VS2-STAT1        PIC X.                               IX1214.2
014300     05  IX-VS2-STAT2        PIC X.                               IX1214.2
014400                                                                  IX1214.2
014500 01  COUNT-OF-RECS           PIC 9(5).                            IX1214.2
014600                                                                  IX1214.2
014700 01  COUNT-OF-RECORDS REDEFINES COUNT-OF-RECS  PIC 9(5).          IX1214.2
014800                                                                  IX1214.2
014900 01  FILE-RECORD-INFORMATION-REC.                                 IX1214.2
015000     05  FILE-RECORD-INFO-SKELETON.                               IX1214.2
015100         10  FILLER          PIC X(48) VALUE                      IX1214.2
015200              "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00". IX1214.2
015300         10  FILLER          PIC X(46) VALUE                      IX1214.2
015400                ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000". IX1214.2
015500         10  FILLER          PIC X(26) VALUE                      IX1214.2
015600                                    ",LFIL=000000,ORG=  ,LBLR= ". IX1214.2
015700         10  FILLER          PIC X(37) VALUE                      IX1214.2
015800                         ",RECKEY=                             ". IX1214.2
015900         10  FILLER          PIC X(38) VALUE                      IX1214.2
016000                        ",ALTKEY1=                             ". IX1214.2
016100         10  FILLER          PIC X(38) VALUE                      IX1214.2
016200                        ",ALTKEY2=                             ". IX1214.2
016300         10  FILLER          PIC X(7) VALUE SPACE.                IX1214.2
016400     05  FILE-RECORD-INFO             OCCURS 10.                  IX1214.2
016500         10  FILE-RECORD-INFO-P1-120.                             IX1214.2
016600             15  FILLER      PIC X(5).                            IX1214.2
016700             15  XFILE-NAME  PIC X(6).                            IX1214.2
016800             15  FILLER      PIC X(8).                            IX1214.2
016900             15  XRECORD-NAME  PIC X(6).                          IX1214.2
017000             15  FILLER        PIC X(1).                          IX1214.2
017100             15  REELUNIT-NUMBER  PIC 9(1).                       IX1214.2
017200             15  FILLER           PIC X(7).                       IX1214.2
017300             15  XRECORD-NUMBER   PIC 9(6).                       IX1214.2
017400             15  FILLER           PIC X(6).                       IX1214.2
017500             15  UPDATE-NUMBER    PIC 9(2).                       IX1214.2
017600             15  FILLER           PIC X(5).                       IX1214.2
017700             15  ODO-NUMBER       PIC 9(4).                       IX1214.2
017800             15  FILLER           PIC X(5).                       IX1214.2
017900             15  XPROGRAM-NAME    PIC X(5).                       IX1214.2
018000             15  FILLER           PIC X(7).                       IX1214.2
018100             15  XRECORD-LENGTH   PIC 9(6).                       IX1214.2
018200             15  FILLER           PIC X(7).                       IX1214.2
018300             15  CHARS-OR-RECORDS  PIC X(2).                      IX1214.2
018400             15  FILLER            PIC X(1).                      IX1214.2
018500             15  XBLOCK-SIZE       PIC 9(4).                      IX1214.2
018600             15  FILLER            PIC X(6).                      IX1214.2
018700             15  RECORDS-IN-FILE   PIC 9(6).                      IX1214.2
018800             15  FILLER            PIC X(5).                      IX1214.2
018900             15  XFILE-ORGANIZATION  PIC X(2).                    IX1214.2
019000             15  FILLER              PIC X(6).                    IX1214.2
019100             15  XLABEL-TYPE         PIC X(1).                    IX1214.2
019200         10  FILE-RECORD-INFO-P121-240.                           IX1214.2
019300             15  FILLER              PIC X(8).                    IX1214.2
019400             15  XRECORD-KEY         PIC X(29).                   IX1214.2
019500             15  FILLER              PIC X(9).                    IX1214.2
019600             15  ALTERNATE-KEY1      PIC X(29).                   IX1214.2
019700             15  FILLER              PIC X(9).                    IX1214.2
019800             15  ALTERNATE-KEY2      PIC X(29).                   IX1214.2
019900             15  FILLER              PIC X(7).                    IX1214.2
020000                                                                  IX1214.2
020100 01  TEST-RESULTS.                                                IX1214.2
020200     02 FILLER                   PIC X      VALUE SPACE.          IX1214.2
020300     02 FEATURE                  PIC X(20)  VALUE SPACE.          IX1214.2
020400     02 FILLER                   PIC X      VALUE SPACE.          IX1214.2
020500     02 P-OR-F                   PIC X(5)   VALUE SPACE.          IX1214.2
020600     02 FILLER                   PIC X      VALUE SPACE.          IX1214.2
020700     02  PAR-NAME.                                                IX1214.2
020800       03 FILLER                 PIC X(19)  VALUE SPACE.          IX1214.2
020900       03  PARDOT-X              PIC X      VALUE SPACE.          IX1214.2
021000       03 DOTVALUE               PIC 99     VALUE ZERO.           IX1214.2
021100     02 FILLER                   PIC X(8)   VALUE SPACE.          IX1214.2
021200     02 RE-MARK                  PIC X(61).                       IX1214.2
021300 01  TEST-COMPUTED.                                               IX1214.2
021400     02 FILLER                   PIC X(30)  VALUE SPACE.          IX1214.2
021500     02 FILLER                   PIC X(17)  VALUE                 IX1214.2
021600            "       COMPUTED=".                                   IX1214.2
021700     02 COMPUTED-X.                                               IX1214.2
021800     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          IX1214.2
021900     03 COMPUTED-N               REDEFINES COMPUTED-A             IX1214.2
022000                                 PIC -9(9).9(9).                  IX1214.2
022100     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         IX1214.2
022200     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     IX1214.2
022300     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     IX1214.2
022400     03       CM-18V0 REDEFINES COMPUTED-A.                       IX1214.2
022500         04 COMPUTED-18V0                    PIC -9(18).          IX1214.2
022600         04 FILLER                           PIC X.               IX1214.2
022700     03 FILLER PIC X(50) VALUE SPACE.                             IX1214.2
022800 01  TEST-CORRECT.                                                IX1214.2
022900     02 FILLER PIC X(30) VALUE SPACE.                             IX1214.2
023000     02 FILLER PIC X(17) VALUE "       CORRECT =".                IX1214.2
023100     02 CORRECT-X.                                                IX1214.2
023200     03 CORRECT-A                  PIC X(20) VALUE SPACE.         IX1214.2
023300     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      IX1214.2
023400     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         IX1214.2
023500     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     IX1214.2
023600     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     IX1214.2
023700     03      CR-18V0 REDEFINES CORRECT-A.                         IX1214.2
023800         04 CORRECT-18V0                     PIC -9(18).          IX1214.2
023900         04 FILLER                           PIC X.               IX1214.2
024000     03 FILLER PIC X(2) VALUE SPACE.                              IX1214.2
024100     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     IX1214.2
024200 01  CCVS-C-1.                                                    IX1214.2
024300     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAIX1214.2
024400-    "SS  PARAGRAPH-NAME                                          IX1214.2
024500-    "       REMARKS".                                            IX1214.2
024600     02 FILLER                     PIC X(20)    VALUE SPACE.      IX1214.2
024700 01  CCVS-C-2.                                                    IX1214.2
024800     02 FILLER                     PIC X        VALUE SPACE.      IX1214.2
024900     02 FILLER                     PIC X(6)     VALUE "TESTED".   IX1214.2
025000     02 FILLER                     PIC X(15)    VALUE SPACE.      IX1214.2
025100     02 FILLER                     PIC X(4)     VALUE "FAIL".     IX1214.2
025200     02 FILLER                     PIC X(94)    VALUE SPACE.      IX1214.2
025300 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       IX1214.2
025400 01  REC-CT                        PIC 99       VALUE ZERO.       IX1214.2
025500 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       IX1214.2
025600 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       IX1214.2
025700 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       IX1214.2
025800 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       IX1214.2
025900 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       IX1214.2
026000 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       IX1214.2
026100 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      IX1214.2
026200 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       IX1214.2
026300 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     IX1214.2
026400 01  CCVS-H-1.                                                    IX1214.2
026500     02  FILLER                    PIC X(39)    VALUE SPACES.     IX1214.2
026600     02  FILLER                    PIC X(42)    VALUE             IX1214.2
026700     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 IX1214.2
026800     02  FILLER                    PIC X(39)    VALUE SPACES.     IX1214.2
026900 01  CCVS-H-2A.                                                   IX1214.2
027000   02  FILLER                        PIC X(40)  VALUE SPACE.      IX1214.2
027100   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  IX1214.2
027200   02  FILLER                        PIC XXXX   VALUE             IX1214.2
027300     "4.2 ".                                                      IX1214.2
027400   02  FILLER                        PIC X(28)  VALUE             IX1214.2
027500            " COPY - NOT FOR DISTRIBUTION".                       IX1214.2
027600   02  FILLER                        PIC X(41)  VALUE SPACE.      IX1214.2
027700                                                                  IX1214.2
027800 01  CCVS-H-2B.                                                   IX1214.2
027900   02  FILLER                        PIC X(15)  VALUE             IX1214.2
028000            "TEST RESULT OF ".                                    IX1214.2
028100   02  TEST-ID                       PIC X(9).                    IX1214.2
028200   02  FILLER                        PIC X(4)   VALUE             IX1214.2
028300            " IN ".                                               IX1214.2
028400   02  FILLER                        PIC X(12)  VALUE             IX1214.2
028500     " HIGH       ".                                              IX1214.2
028600   02  FILLER                        PIC X(22)  VALUE             IX1214.2
028700            " LEVEL VALIDATION FOR ".                             IX1214.2
028800   02  FILLER                        PIC X(58)  VALUE             IX1214.2
028900     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IX1214.2
029000 01  CCVS-H-3.                                                    IX1214.2
029100     02  FILLER                      PIC X(34)  VALUE             IX1214.2
029200            " FOR OFFICIAL USE ONLY    ".                         IX1214.2
029300     02  FILLER                      PIC X(58)  VALUE             IX1214.2
029400     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IX1214.2
029500     02  FILLER                      PIC X(28)  VALUE             IX1214.2
029600            "  COPYRIGHT   1985 ".                                IX1214.2
029700 01  CCVS-E-1.                                                    IX1214.2
029800     02 FILLER                       PIC X(52)  VALUE SPACE.      IX1214.2
029900     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              IX1214.2
030000     02 ID-AGAIN                     PIC X(9).                    IX1214.2
030100     02 FILLER                       PIC X(45)  VALUE SPACES.     IX1214.2
030200 01  CCVS-E-2.                                                    IX1214.2
030300     02  FILLER                      PIC X(31)  VALUE SPACE.      IX1214.2
030400     02  FILLER                      PIC X(21)  VALUE SPACE.      IX1214.2
030500     02 CCVS-E-2-2.                                               IX1214.2
030600         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      IX1214.2
030700         03 FILLER                   PIC X      VALUE SPACE.      IX1214.2
030800         03 ENDER-DESC               PIC X(44)  VALUE             IX1214.2
030900            "ERRORS ENCOUNTERED".                                 IX1214.2
031000 01  CCVS-E-3.                                                    IX1214.2
031100     02  FILLER                      PIC X(22)  VALUE             IX1214.2
031200            " FOR OFFICIAL USE ONLY".                             IX1214.2
031300     02  FILLER                      PIC X(12)  VALUE SPACE.      IX1214.2
031400     02  FILLER                      PIC X(58)  VALUE             IX1214.2
031500     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IX1214.2
031600     02  FILLER                      PIC X(13)  VALUE SPACE.      IX1214.2
031700     02 FILLER                       PIC X(15)  VALUE             IX1214.2
031800             " COPYRIGHT 1985".                                   IX1214.2
031900 01  CCVS-E-4.                                                    IX1214.2
032000     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      IX1214.2
032100     02 FILLER                       PIC X(4)   VALUE " OF ".     IX1214.2
032200     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      IX1214.2
032300     02 FILLER                       PIC X(40)  VALUE             IX1214.2
032400      "  TESTS WERE EXECUTED SUCCESSFULLY".                       IX1214.2
032500 01  XXINFO.                                                      IX1214.2
032600     02 FILLER                       PIC X(19)  VALUE             IX1214.2
032700            "*** INFORMATION ***".                                IX1214.2
032800     02 INFO-TEXT.                                                IX1214.2
032900       04 FILLER                     PIC X(8)   VALUE SPACE.      IX1214.2
033000       04 XXCOMPUTED                 PIC X(20).                   IX1214.2
033100       04 FILLER                     PIC X(5)   VALUE SPACE.      IX1214.2
033200       04 XXCORRECT                  PIC X(20).                   IX1214.2
033300     02 INF-ANSI-REFERENCE           PIC X(48).                   IX1214.2
033400 01  HYPHEN-LINE.                                                 IX1214.2
033500     02 FILLER  PIC IS X VALUE IS SPACE.                          IX1214.2
033600     02 FILLER  PIC IS X(65)    VALUE IS "************************IX1214.2
033700-    "*****************************************".                 IX1214.2
033800     02 FILLER  PIC IS X(54)    VALUE IS "************************IX1214.2
033900-    "******************************".                            IX1214.2
034000 01  CCVS-PGM-ID                     PIC X(9)   VALUE             IX1214.2
034100     "IX121A".                                                    IX1214.2
034200 01 TEST-NUMBER                      PIC 9 VALUE ZERO.            IX1214.2
034300                                                                  IX1214.2
034400 PROCEDURE DIVISION.                                              IX1214.2
034500 DECLARATIVES.                                                    IX1214.2
034600                                                                  IX1214.2
034700 SECT-IX105-0002 SECTION.                                         IX1214.2
034800     USE AFTER EXCEPTION PROCEDURE ON IX-VS2.                     IX1214.2
034900 INPUT-PROCESS.                                                   IX1214.2
035000         MOVE 1 TO PERM-ERRORS.                                   IX1214.2
035100         IF TEST-NUMBER NOT = 3 GO TO END-DECL.                   IX1214.2
035200 D-RWR-TEST-GF-01-1.                                              IX1214.2
035300     IF IX-VS2-STATUS  = "00"                                     IX1214.2
035400         GO TO D-RWR-PASS-GF-01-0.                                IX1214.2
035500     IF IX-VS2-STATUS  = "44"                                     IX1214.2
035600         GO TO D-RWR-PASS-GF-01-0.                                IX1214.2
035700 D-RWR-FAIL-GF-01-0.                                              IX1214.2
035800     MOVE "IX-5, 1.3.4, (5) d 1 & 2;  SHORT RECORD" TO RE-MARK.   IX1214.2
035900     PERFORM D-FAIL.                                              IX1214.2
036000     MOVE  IX-VS2-STATUS      TO COMPUTED-A.                      IX1214.2
036100     MOVE "00 OR 44" TO CORRECT-X.                                IX1214.2
036200     GO TO D-RWR-WRITE-GF-01-0.                                   IX1214.2
036300 D-RWR-PASS-GF-01-0.                                              IX1214.2
036400     PERFORM D-PASS.                                              IX1214.2
036500 D-RWR-WRITE-GF-01-0.                                             IX1214.2
036600     PERFORM D-PRINT-DETAIL.                                      IX1214.2
036700 D-CLOSE-FILES.                                                   IX1214.2
036800*    OPEN I-O RAW-DATA.                                           IX1214.2
036900*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            IX1214.2
037000*    READ RAW-DATA INVALID KEY GO TO D-END-E-2.                   IX1214.2
037100*    MOVE "OK.     " TO C-ABORT.                                  IX1214.2
037200*    MOVE PASS-COUNTER TO C-OK.                                   IX1214.2
037300*    MOVE ERROR-HOLD   TO C-ALL.                                  IX1214.2
037400*    MOVE ERROR-COUNTER TO C-FAIL.                                IX1214.2
037500*    MOVE DELETE-COUNTER TO C-DELETED.                            IX1214.2
037600*    MOVE INSPECT-COUNTER TO C-INSPECT.                           IX1214.2
037700*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO D-END-E-2.           IX1214.2
037800*D-END-E-2.                                                       IX1214.2
037900*    CLOSE RAW-DATA.                                              IX1214.2
038000     PERFORM D-END-ROUTINE THRU D-END-ROUTINE-13.                 IX1214.2
038100     CLOSE PRINT-FILE.                                            IX1214.2
038200 D-TERMINATE-CCVS.                                                IX1214.2
038300*    EXIT PROGRAM.                                                IX1214.2
038400*D-TERMINATE-CALL.                                                IX1214.2
038500     STOP     RUN.                                                IX1214.2
038600 D-PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.         IX1214.2
038700 D-FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.        IX1214.2
038800 D-PRINT-DETAIL.                                                  IX1214.2
038900     IF REC-CT NOT EQUAL TO ZERO                                  IX1214.2
039000             MOVE "." TO PARDOT-X                                 IX1214.2
039100             MOVE REC-CT TO DOTVALUE.                             IX1214.2
039200     MOVE TEST-RESULTS TO PRINT-REC. PERFORM D-WRITE-LINE.        IX1214.2
039300     IF P-OR-F EQUAL TO "FAIL*"  PERFORM D-WRITE-LINE             IX1214.2
039400        PERFORM D-FAIL-ROUTINE THRU D-FAIL-ROUTINE-EX             IX1214.2
039500     ELSE PERFORM D-BAIL-OUT THRU D-BAIL-OUT-EX.                  IX1214.2
039600     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              IX1214.2
039700     MOVE SPACE TO CORRECT-X.                                     IX1214.2
039800     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         IX1214.2
039900     MOVE     SPACE TO RE-MARK.                                   IX1214.2
040000 D-END-ROUTINE.                                                   IX1214.2
040100     MOVE HYPHEN-LINE TO DUMMY-RECORD.                            IX1214.2
040200     PERFORM D-WRITE-LINE 5 TIMES.                                IX1214.2
040300 D-END-RTN-EXIT.                                                  IX1214.2
040400     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM D-WRITE-LINE 2 TIMES. IX1214.2
040500 D-END-ROUTINE-1.                                                 IX1214.2
040600      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      IX1214.2
040700      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               IX1214.2
040800      ADD PASS-COUNTER TO ERROR-HOLD.                             IX1214.2
040900      MOVE PASS-COUNTER TO CCVS-E-4-1.                            IX1214.2
041000      MOVE ERROR-HOLD TO CCVS-E-4-2.                              IX1214.2
041100      MOVE CCVS-E-4 TO CCVS-E-2-2.                                IX1214.2
041200      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM D-WRITE-LINE.         IX1214.2
041300 D-END-ROUTINE-12.                                                IX1214.2
041400      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        IX1214.2
041500     IF       ERROR-COUNTER IS EQUAL TO ZERO                      IX1214.2
041600         MOVE "NO " TO ERROR-TOTAL                                IX1214.2
041700         ELSE                                                     IX1214.2
041800         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       IX1214.2
041900     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           IX1214.2
042000     PERFORM D-WRITE-LINE.                                        IX1214.2
042100 D-END-ROUTINE-13.                                                IX1214.2
042200     IF DELETE-COUNTER IS EQUAL TO ZERO                           IX1214.2
042300         MOVE "NO " TO ERROR-TOTAL  ELSE                          IX1214.2
042400         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      IX1214.2
042500     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   IX1214.2
042600     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM D-WRITE-LINE.         IX1214.2
042700      IF   INSPECT-COUNTER EQUAL TO ZERO                          IX1214.2
042800          MOVE "NO " TO ERROR-TOTAL                               IX1214.2
042900      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   IX1214.2
043000      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            IX1214.2
043100     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM D-WRITE-LINE.         IX1214.2
043200     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM D-WRITE-LINE.         IX1214.2
043300 D-WRITE-LINE.                                                    IX1214.2
043400     ADD 1 TO RECORD-COUNT.                                       IX1214.2
043500*    IF RECORD-COUNT GREATER 42                                   IX1214.2
043600*      MOVE DUMMY-RECORD TO DUMMY-HOLD                            IX1214.2
043700*      MOVE SPACE TO DUMMY-RECORD                                 IX1214.2
043800*      WRITE DUMMY-RECORD AFTER ADVANCING PAGE                    IX1214.2
043900*      MOVE CCVS-H-1  TO DUMMY-RECORD PERFORM D-WRT-LN 2 TIMES    IX1214.2
044000*      MOVE CCVS-H-2A TO DUMMY-RECORD PERFORM D-WRT-LN 2 TIMES    IX1214.2
044100*      MOVE CCVS-H-2B TO DUMMY-RECORD PERFORM D-WRT-LN 3 TIMES    IX1214.2
044200*      MOVE CCVS-H-3  TO DUMMY-RECORD PERFORM D-WRT-LN 3 TIMES    IX1214.2
044300*      MOVE CCVS-C-1  TO DUMMY-RECORD PERFORM D-WRT-LN            IX1214.2
044400*      MOVE CCVS-C-2  TO DUMMY-RECORD PERFORM D-WRT-LN            IX1214.2
044500*      MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM D-WRT-LN          IX1214.2
044600*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          IX1214.2
044700*        MOVE ZERO TO RECORD-COUNT.                               IX1214.2
044800     PERFORM D-WRT-LN.                                            IX1214.2
044900 D-WRT-LN.                                                        IX1214.2
045000     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               IX1214.2
045100     MOVE SPACE TO DUMMY-RECORD.                                  IX1214.2
045200 D-BLANK-LINE-PRINT.                                              IX1214.2
045300     PERFORM D-WRT-LN.                                            IX1214.2
045400 D-FAIL-ROUTINE.                                                  IX1214.2
045500     IF     COMPUTED-X NOT EQUAL TO SPACE                         IX1214.2
045600            GO TO  D-FAIL-ROUTINE-WRITE.                          IX1214.2
045700     IF  CORRECT-X NOT EQUAL TO SPACE GO TO D-FAIL-ROUTINE-WRITE. IX1214.2
045800     MOVE ANSI-REFERENCE TO INF-ANSI-REFERENCE.                   IX1214.2
045900     MOVE "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.    IX1214.2
046000     MOVE  XXINFO TO DUMMY-RECORD. PERFORM D-WRITE-LINE 2 TIMES.  IX1214.2
046100     MOVE  SPACES TO INF-ANSI-REFERENCE.                          IX1214.2
046200     GO TO D-FAIL-ROUTINE-EX.                                     IX1214.2
046300 D-FAIL-ROUTINE-WRITE.                                            IX1214.2
046400     MOVE  TEST-COMPUTED TO PRINT-REC PERFORM D-WRITE-LINE        IX1214.2
046500     MOVE  ANSI-REFERENCE TO COR-ANSI-REFERENCE.                  IX1214.2
046600     MOVE TEST-CORRECT TO PRINT-REC PERFORM D-WRITE-LINE 2 TIMES. IX1214.2
046700     MOVE  SPACES TO COR-ANSI-REFERENCE.                          IX1214.2
046800 D-FAIL-ROUTINE-EX. EXIT.                                         IX1214.2
046900 D-BAIL-OUT.                                                      IX1214.2
047000     IF  COMPUTED-A NOT EQUAL TO SPACE GO TO D-BAIL-OUT-WRITE.    IX1214.2
047100     IF  CORRECT-A EQUAL TO SPACE GO TO D-BAIL-OUT-EX.            IX1214.2
047200 D-BAIL-OUT-WRITE.                                                IX1214.2
047300     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  IX1214.2
047400     MOVE  ANSI-REFERENCE TO INF-ANSI-REFERENCE.                  IX1214.2
047500     MOVE  XXINFO TO DUMMY-RECORD. PERFORM D-WRITE-LINE 2 TIMES.  IX1214.2
047600     MOVE  SPACES TO INF-ANSI-REFERENCE.                          IX1214.2
047700 D-BAIL-OUT-EX. EXIT.                                             IX1214.2
047800                                                                  IX1214.2
047900 END-DECL.                                                        IX1214.2
048000 END DECLARATIVES.                                                IX1214.2
048100                                                                  IX1214.2
048200                                                                  IX1214.2
048300 CCVS1 SECTION.                                                   IX1214.2
048400 OPEN-FILES.                                                      IX1214.2
048500*    OPEN I-O RAW-DATA.                                           IX1214.2
048600*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            IX1214.2
048700*    READ RAW-DATA INVALID KEY GO TO END-E-1.                     IX1214.2
048800*    MOVE "ABORTED " TO C-ABORT.                                  IX1214.2
048900*    ADD 1 TO C-NO-OF-TESTS.                                      IX1214.2
049000*    ACCEPT C-DATE  FROM DATE.                                    IX1214.2
049100*    ACCEPT C-TIME  FROM TIME.                                    IX1214.2
049200*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-1.             IX1214.2
049300*END-E-1.                                                         IX1214.2
049400*    CLOSE RAW-DATA.                                              IX1214.2
049500     OPEN    OUTPUT PRINT-FILE.                                   IX1214.2
049600     MOVE  CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.  IX1214.2
049700     MOVE    SPACE TO TEST-RESULTS.                               IX1214.2
049800     PERFORM HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.              IX1214.2
049900     MOVE    ZERO TO REC-SKL-SUB.                                 IX1214.2
050000     PERFORM CCVS-INIT-FILE 9 TIMES.                              IX1214.2
050100 CCVS-INIT-FILE.                                                  IX1214.2
050200     ADD     1 TO REC-SKL-SUB.                                    IX1214.2
050300     MOVE    FILE-RECORD-INFO-SKELETON                            IX1214.2
050400          TO FILE-RECORD-INFO (REC-SKL-SUB).                      IX1214.2
050500 CCVS-INIT-EXIT.                                                  IX1214.2
050600     GO TO CCVS1-EXIT.                                            IX1214.2
050700 CLOSE-FILES.                                                     IX1214.2
050800*    OPEN I-O RAW-DATA.                                           IX1214.2
050900*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            IX1214.2
051000*    READ RAW-DATA INVALID KEY GO TO END-E-2.                     IX1214.2
051100*    MOVE "OK.     " TO C-ABORT.                                  IX1214.2
051200*    MOVE PASS-COUNTER TO C-OK.                                   IX1214.2
051300*    MOVE ERROR-HOLD   TO C-ALL.                                  IX1214.2
051400*    MOVE ERROR-COUNTER TO C-FAIL.                                IX1214.2
051500*    MOVE DELETE-COUNTER TO C-DELETED.                            IX1214.2
051600*    MOVE INSPECT-COUNTER TO C-INSPECT.                           IX1214.2
051700*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-2.             IX1214.2
051800*END-E-2.                                                         IX1214.2
051900*    CLOSE RAW-DATA.                                              IX1214.2
052000     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   IX1214.2
052100 TERMINATE-CCVS.                                                  IX1214.2
052200*    EXIT PROGRAM.                                                IX1214.2
052300*TERMINATE-CALL.                                                  IX1214.2
052400     STOP     RUN.                                                IX1214.2
052500 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         IX1214.2
052600 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           IX1214.2
052700 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          IX1214.2
052800 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      IX1214.2
052900     MOVE "****TEST DELETED****" TO RE-MARK.                      IX1214.2
053000 PRINT-DETAIL.                                                    IX1214.2
053100     IF REC-CT NOT EQUAL TO ZERO                                  IX1214.2
053200             MOVE "." TO PARDOT-X                                 IX1214.2
053300             MOVE REC-CT TO DOTVALUE.                             IX1214.2
053400     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      IX1214.2
053500     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               IX1214.2
053600        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 IX1214.2
053700          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 IX1214.2
053800     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              IX1214.2
053900     MOVE SPACE TO CORRECT-X.                                     IX1214.2
054000     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         IX1214.2
054100     MOVE     SPACE TO RE-MARK.                                   IX1214.2
054200 HEAD-ROUTINE.                                                    IX1214.2
054300     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IX1214.2
054400     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IX1214.2
054500     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IX1214.2
054600     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IX1214.2
054700 COLUMN-NAMES-ROUTINE.                                            IX1214.2
054800     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IX1214.2
054900     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX1214.2
055000     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        IX1214.2
055100 END-ROUTINE.                                                     IX1214.2
055200     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.IX1214.2
055300 END-RTN-EXIT.                                                    IX1214.2
055400     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX1214.2
055500 END-ROUTINE-1.                                                   IX1214.2
055600      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      IX1214.2
055700      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               IX1214.2
055800      ADD PASS-COUNTER TO ERROR-HOLD.                             IX1214.2
055900*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   IX1214.2
056000      MOVE PASS-COUNTER TO CCVS-E-4-1.                            IX1214.2
056100      MOVE ERROR-HOLD TO CCVS-E-4-2.                              IX1214.2
056200      MOVE CCVS-E-4 TO CCVS-E-2-2.                                IX1214.2
056300      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           IX1214.2
056400  END-ROUTINE-12.                                                 IX1214.2
056500      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        IX1214.2
056600     IF       ERROR-COUNTER IS EQUAL TO ZERO                      IX1214.2
056700         MOVE "NO " TO ERROR-TOTAL                                IX1214.2
056800         ELSE                                                     IX1214.2
056900         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       IX1214.2
057000     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           IX1214.2
057100     PERFORM WRITE-LINE.                                          IX1214.2
057200 END-ROUTINE-13.                                                  IX1214.2
057300     IF DELETE-COUNTER IS EQUAL TO ZERO                           IX1214.2
057400         MOVE "NO " TO ERROR-TOTAL  ELSE                          IX1214.2
057500         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      IX1214.2
057600     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   IX1214.2
057700     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IX1214.2
057800      IF   INSPECT-COUNTER EQUAL TO ZERO                          IX1214.2
057900          MOVE "NO " TO ERROR-TOTAL                               IX1214.2
058000      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   IX1214.2
058100      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            IX1214.2
058200      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          IX1214.2
058300     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IX1214.2
058400 WRITE-LINE.                                                      IX1214.2
058500     ADD 1 TO RECORD-COUNT.                                       IX1214.2
058600*    IF RECORD-COUNT GREATER 42                                   IX1214.2
058700*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          IX1214.2
058800*        MOVE SPACE TO DUMMY-RECORD                               IX1214.2
058900*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  IX1214.2
059000*        MOVE CCVS-H-1  TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    IX1214.2
059100*        MOVE CCVS-H-2A TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    IX1214.2
059200*        MOVE CCVS-H-2B TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    IX1214.2
059300*        MOVE CCVS-H-3  TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    IX1214.2
059400*        MOVE CCVS-C-1  TO DUMMY-RECORD PERFORM WRT-LN            IX1214.2
059500*        MOVE CCVS-C-2  TO DUMMY-RECORD PERFORM WRT-LN            IX1214.2
059600*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          IX1214.2
059700*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          IX1214.2
059800*        MOVE ZERO TO RECORD-COUNT.                               IX1214.2
059900     PERFORM WRT-LN.                                              IX1214.2
060000 WRT-LN.                                                          IX1214.2
060100     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               IX1214.2
060200     MOVE SPACE TO DUMMY-RECORD.                                  IX1214.2
060300 BLANK-LINE-PRINT.                                                IX1214.2
060400     PERFORM WRT-LN.                                              IX1214.2
060500 FAIL-ROUTINE.                                                    IX1214.2
060600     IF     COMPUTED-X NOT EQUAL TO SPACE                         IX1214.2
060700            GO TO   FAIL-ROUTINE-WRITE.                           IX1214.2
060800     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.IX1214.2
060900     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IX1214.2
061000     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   IX1214.2
061100     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX1214.2
061200     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IX1214.2
061300     GO TO  FAIL-ROUTINE-EX.                                      IX1214.2
061400 FAIL-ROUTINE-WRITE.                                              IX1214.2
061500     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         IX1214.2
061600     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 IX1214.2
061700     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. IX1214.2
061800     MOVE   SPACES TO COR-ANSI-REFERENCE.                         IX1214.2
061900 FAIL-ROUTINE-EX. EXIT.                                           IX1214.2
062000 BAIL-OUT.                                                        IX1214.2
062100     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   IX1214.2
062200     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           IX1214.2
062300 BAIL-OUT-WRITE.                                                  IX1214.2
062400     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  IX1214.2
062500     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IX1214.2
062600     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX1214.2
062700     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IX1214.2
062800 BAIL-OUT-EX. EXIT.                                               IX1214.2
062900 CCVS1-EXIT.                                                      IX1214.2
063000     EXIT.                                                        IX1214.2
063100                                                                  IX1214.2
063200 SECT-IX121A-0003 SECTION.                                        IX1214.2
063300 SEQ-INIT-010.                                                    IX1214.2
063400     MOVE "IX-VS2" TO XFILE-NAME (1).                             IX1214.2
063500     MOVE "R1-F-G" TO XRECORD-NAME (1).                           IX1214.2
063600     MOVE CCVS-PGM-ID TO XPROGRAM-NAME (1).                       IX1214.2
063700     MOVE 000240 TO XRECORD-LENGTH (1).                           IX1214.2
063800     MOVE "RC" TO CHARS-OR-RECORDS (1).                           IX1214.2
063900     MOVE 0002 TO XBLOCK-SIZE (1).                                IX1214.2
064000     MOVE 000050 TO RECORDS-IN-FILE (1).                          IX1214.2
064100     MOVE "IX" TO XFILE-ORGANIZATION (1).                         IX1214.2
064200     MOVE "S" TO XLABEL-TYPE (1).                                 IX1214.2
064300     MOVE 000001 TO XRECORD-NUMBER (1).                           IX1214.2
064400     MOVE 0 TO COUNT-OF-RECS.                                     IX1214.2
064500                                                                  IX1214.2
064600******************************************************************IX1214.2
064700*   TEST  1                                                      *IX1214.2
064800*         OPEN OUTPUT ...                 00 EXPECTED            *IX1214.2
064900*         IX-3, 1.3.4 (1) a                                      *IX1214.2
065000*    STATUS 00 CHECK ON OUTPUT FILE IX-VS2                       *IX1214.2
065100*    THE OUTPUT STATEMENT IS SUCCESSFULLY EXECUTED               *IX1214.2
065200******************************************************************IX1214.2
065300 OPN-INIT-GF-01-0.                                                IX1214.2
065400     ADD  1 TO TEST-NUMBER.                                       IX1214.2
065500     MOVE 1 TO STATUS-TEST-00.                                    IX1214.2
065600     MOVE SPACES TO IX-VS2-STATUS.                                IX1214.2
065700     MOVE "OPEN OUTPUT: 00 EXP." TO FEATURE.                      IX1214.2
065800     MOVE "OPN-TEST-GF-01-0" TO PAR-NAME.                         IX1214.2
065900     OPEN                                                         IX1214.2
066000        OUTPUT IX-VS2.                                            IX1214.2
066100     IF IX-VS2-STATUS EQUAL TO "00"                               IX1214.2
066200         GO TO OPN-PASS-GF-01-0.                                  IX1214.2
066300 OPN-FAIL-GF-01-0.                                                IX1214.2
066400     MOVE "IX-3, 1.3.4, (1) a. " TO RE-MARK.                      IX1214.2
066500     PERFORM FAIL.                                                IX1214.2
066600     MOVE IX-VS2-STATUS TO COMPUTED-A.                            IX1214.2
066700     MOVE "00" TO CORRECT-X.                                      IX1214.2
066800     GO TO OPN-WRITE-GF-01-0.                                     IX1214.2
066900 OPN-PASS-GF-01-0.                                                IX1214.2
067000     PERFORM PASS.                                                IX1214.2
067100 OPN-WRITE-GF-01-0.                                               IX1214.2
067200     PERFORM PRINT-DETAIL.                                        IX1214.2
067300******************************************************************IX1214.2
067400*   TEST  2                                                      *IX1214.2
067500*         WRITE                           00 EXPECTED            *IX1214.2
067600*         IX-3, 1.3.4 (1) a                                      *IX1214.2
067700*    CREATING A INDEXED FILE WITH 50 RECORDS                     *IX1214.2
067800*    KEY:  FROM  000000001 TO 000000050                          *IX1214.2
067900******************************************************************IX1214.2
068000 WRI-INIT-GF-01-0.                                                IX1214.2
068100     ADD  1 TO TEST-NUMBER.                                       IX1214.2
068200     MOVE SPACES TO IX-VS2-STATUS.                                IX1214.2
068300     MOVE 0 TO STATUS-TEST-00.                                    IX1214.2
068400     MOVE "WRITE: 00 EXPECTED" TO FEATURE.                        IX1214.2
068500     MOVE "WRI-TEST-GF-01-0" TO PAR-NAME.                         IX1214.2
068600 WRI-TEST-GF-01-0.                                                IX1214.2
068700     MOVE XRECORD-NUMBER (1) TO GRP-0101-KEY, COUNT-OF-RECS.      IX1214.2
068800     MOVE GRP-0101 TO XRECORD-KEY (1).                            IX1214.2
068900     MOVE GRP-0102 TO ALTERNATE-KEY1 (1).                         IX1214.2
069000*    THE VALUE OF THE ALTERNATE KEY IS 50 TIMES UNCHANGED        *IX1214.2
069100     MOVE FILE-RECORD-INFO (1) TO IX-VS2R1-F-G-240.               IX1214.2
069200     WRITE IX-VS2R1-F-G-240.                                      IX1214.2
069300     IF IX-VS2-STATUS  NOT = "00"                                 IX1214.2
069400         MOVE 1 TO STATUS-TEST-00                                 IX1214.2
069500         GO TO WRI-FAIL-GF-01-0.                                  IX1214.2
069600     IF XRECORD-NUMBER (1) EQUAL TO 50                            IX1214.2
069700         GO TO WRI-TEST-GF-01-1.                                  IX1214.2
069800     ADD 1 TO XRECORD-NUMBER (1).                                 IX1214.2
069900     GO TO WRI-TEST-GF-01-0.                                      IX1214.2
070000 WRI-TEST-GF-01-1.                                                IX1214.2
070100     IF RECORDS-IN-ERROR EQUAL TO ZERO                            IX1214.2
070200         GO TO WRI-PASS-GF-01-0.                                  IX1214.2
070300     MOVE "ERROR IN CREATING FILE" TO RE-MARK.                    IX1214.2
070400 WRI-FAIL-GF-01-0.                                                IX1214.2
070500     MOVE "IX-3, 1.3.4, (1) a. " TO RE-MARK.                      IX1214.2
070600     PERFORM FAIL.                                                IX1214.2
070700     MOVE "RECORDS WRITTEN =" TO COMPUTED-A.                      IX1214.2
070800     GO TO WRI-WRITE-GF-01-0.                                     IX1214.2
070900 WRI-PASS-GF-01-0.                                                IX1214.2
071000     PERFORM PASS.                                                IX1214.2
071100 WRI-WRITE-GF-01-0.                                               IX1214.2
071200     PERFORM PRINT-DETAIL.                                        IX1214.2
071300     MOVE "FILE CREATED, RECS =" TO COMPUTED-A.                   IX1214.2
071400     MOVE "CREATE FILE IX-VS2" TO FEATURE.                        IX1214.2
071500     MOVE "WRI-TEST-GF-01-1" TO PAR-NAME.                         IX1214.2
071600     MOVE COUNT-OF-RECS TO CORRECT-18V0.                          IX1214.2
071700     PERFORM PRINT-DETAIL.                                        IX1214.2
071800******************************************************************IX1214.2
071900*   TEST  3                                                      *IX1214.2
072000*         REWRITE   (WITH WRONG RECORD LENGTH (SHORTER))         *IX1214.2
072100*         IX-5, 1.3.4 (5) d 1 & 2                                *IX1214.2
072200*         FILE STATUS  00   OR  44   EXPECTED                    *IX1214.2
072300*    KEY:        000000005                                       *IX1214.2
072400******************************************************************IX1214.2
072500 RWR-INIT-GF-01-0.                                                IX1214.2
072600     ADD  1 TO TEST-NUMBER.                                       IX1214.2
072700     CLOSE IX-VS2.                                                IX1214.2
072800     OPEN I-O IX-VS2.                                             IX1214.2
072900     MOVE SPACES TO IX-VS2-STATUS.                                IX1214.2
073000     MOVE 0 TO STATUS-TEST-00.                                    IX1214.2
073100     MOVE "RWRTE LG. 00/44 EXP." TO FEATURE.                      IX1214.2
073200     MOVE "RWR-TEST-GF-01-0" TO PAR-NAME.                         IX1214.2
073300     READ IX-VS2 AT END       GO TO RWR-TEST-GF-01-0.             IX1214.2
073400     READ IX-VS2 AT END       GO TO RWR-TEST-GF-01-0.             IX1214.2
073500     READ IX-VS2 AT END       GO TO RWR-TEST-GF-01-0.             IX1214.2
073600     READ IX-VS2 AT END       GO TO RWR-TEST-GF-01-0.             IX1214.2
073700     READ IX-VS2 AT END       GO TO RWR-TEST-GF-01-0.             IX1214.2
073800 RWR-TEST-GF-01-0.                                                IX1214.2
073900     MOVE "WRONG RECORD LENGTH ( LONGER )" TO IX-VS2-REC-LONG.    IX1214.2
074000     REWRITE IX-VS2R1-F-G-280.                                    IX1214.2
074100 RWR-TEST-GF-01-1.                                                IX1214.2
074200     IF IX-VS2-STATUS  = "00"                                     IX1214.2
074300         GO TO RWR-PASS-GF-01-0.                                  IX1214.2
074400     IF IX-VS2-STATUS  = "44"                                     IX1214.2
074500         GO TO RWR-PASS-GF-01-0.                                  IX1214.2
074600 RWR-FAIL-GF-01-0.                                                IX1214.2
074700     MOVE "IX-5, 1.3.4, (5) D 1 & 2;   LONG RECORD" TO RE-MARK.   IX1214.2
074800     PERFORM FAIL.                                                IX1214.2
074900     MOVE  IX-VS2-STATUS      TO COMPUTED-A.                      IX1214.2
075000     MOVE "00 OR 44" TO CORRECT-X.                                IX1214.2
075100     GO TO RWR-WRITE-GF-01-0.                                     IX1214.2
075200 RWR-PASS-GF-01-0.                                                IX1214.2
075300     PERFORM PASS.                                                IX1214.2
075400 RWR-WRITE-GF-01-0.                                               IX1214.2
075500     PERFORM PRINT-DETAIL.                                        IX1214.2
075600                                                                  IX1214.2
075700 TERMINATE-ROUTINE.                                               IX1214.2
075800     EXIT.                                                        IX1214.2
075900                                                                  IX1214.2
076000 CCVS-EXIT SECTION.                                               IX1214.2
076100 CCVS-999999.                                                     IX1214.2
076200     GO TO CLOSE-FILES.                                           IX1214.2
      *END-OF,IX121A                                                                  
